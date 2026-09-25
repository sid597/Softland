(ns rig.store.clock-test
  "Pure tests of the hybrid stamp (rig.store.clock; PLAN-stream-store.md,
  'Hybrid clock (26 September)'): the packing, ruling 4's two promises under
  arbitrary walls and stood-on stamps, the counter carrying past 65,535, and
  the stamps staying at the wall's millisecond at a high rate, where the
  millisecond unit ran ahead. No cluster; the gate's use of it is
  rig.store.stream-gate-test's."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [rig.store.clock :as hlc]
            [rig.store.envelope :as env]))

(defn- check!
  "Run a property; on failure report the smallest failing input."
  [label n p]
  (let [r (tc/quick-check n p {:max-size 50})]
    (is (true? (:pass? r))
        (str label " failed: "
             (pr-str (select-keys r [:seed :num-tests :fail]))
             "\nsmallest: " (pr-str (get-in r [:shrunk :smallest]))))
    r))

(def ^:private now-ms 1790000000000) ; a wall in 2026, in milliseconds

(defn- stamps-over
  "The stamps a unit gives for decisions at the walls `walls`, from `last`,
  standing on nothing."
  [walls last]
  (next (reductions (fn [s ms] (hlc/next-stamp ms s [])) last walls)))

(defn- ms-unit-stamps
  "What the millisecond unit gave over the same walls: max(wall, last + 1)."
  [walls last]
  (next (reductions (fn [s ms] (max ms (inc s))) last walls)))

(deftest encoding
  (testing "a stamp is ms × 65536 + counter"
    (is (= 65536 hlc/counter-span))
    (is (= 65535 hlc/max-counter))
    (is (= 0 (hlc/pack 0 0)))
    (is (= 65536 (hlc/pack 1 0)))
    (is (= 131071 (hlc/pack 1 65535)))
    (is (= (+ (* now-ms 65536) 7) (hlc/pack now-ms 7)))
    (is (instance? Long (hlc/pack now-ms 7))))
  (testing "the parts read back, and a long orders stamps by millisecond, then counter"
    (check! "pack round trip and order" 2000
            (prop/for-all [ms1 (gen/choose 0 (bit-shift-left 1 46)) c1 (gen/choose 0 65535)
                           ms2 (gen/choose 0 (bit-shift-left 1 46)) c2 (gen/choose 0 65535)]
              (let [s1 (hlc/pack ms1 c1) s2 (hlc/pack ms2 c2)]
                (and (= ms1 (hlc/ms-of s1)) (= c1 (hlc/counter-of s1))
                     (= (compare [ms1 c1] [ms2 c2]) (compare s1 s2)))))))
  (testing "every long is one (ms, counter) pair, negative ones included"
    (check! "unpack round trip" 2000
            (prop/for-all [s (gen/choose (- (bit-shift-left 1 62)) (bit-shift-left 1 62))]
              (and (= s (hlc/pack (hlc/ms-of s) (hlc/counter-of s)))
                   (<= 0 (hlc/counter-of s) 65535))))))

(def ^:private gen-decisions
  "A run of decisions on one unit: each a wall step (the wall may stand
  still or step back, as a real clock can) and what the fact stood on,
  some of it ahead of the wall, some behind."
  (gen/vector (gen/tuple (gen/frequency [[6 (gen/choose 0 3)] [1 (gen/choose -5 -1)] [1 (gen/choose 4 5000)]])
                         (gen/vector (gen/tuple (gen/choose -2000 2000) (gen/choose 0 65535)) 0 3))
              1 60))

(deftest the-two-promises
  (testing "never backward within a unit; never earlier than anything the fact stood on; never before the wall's millisecond (ruling 4)"
    (check! "next-stamp promises" 500
            (prop/for-all [start (gen/choose 0 100000)
                           decisions gen-decisions]
              (loop [ms (+ now-ms start) last 0 [[step stood] & more] decisions]
                (let [ms (+ ms step)
                      stood-on (map (fn [[d c]] (hlc/pack (+ ms d) c)) stood)
                      s (hlc/next-stamp ms last stood-on)
                      ok (and (instance? Long s)
                              (< last s)
                              (every? #(< % s) stood-on)
                              (<= (hlc/pack ms 0) s)
                              ;; at the wall's own millisecond whenever nothing it
                              ;; must follow has reached the wall's last counter
                              (or (<= (hlc/pack ms hlc/max-counter) (apply max last stood-on))
                                  (= ms (hlc/ms-of s))))]
                  (cond (not ok) false
                        (empty? more) true
                        :else (recur ms s more))))))))

(deftest the-counter
  (testing "the wall standing still: the counter counts, the millisecond stays the wall's"
    (let [ss (stamps-over (repeat 5 now-ms) 0)]
      (is (= (map #(hlc/pack now-ms %) (range 5)) ss))))
  (testing "the counter rolling past 65,535 within one millisecond carries into the next millisecond"
    (let [top (hlc/pack now-ms 65535)
          over (hlc/next-stamp now-ms top [])]
      (is (= (hlc/pack (inc now-ms) 0) over))
      (is (= (inc top) over) "still one past the last: nothing skipped")
      (is (= [(inc now-ms) 0] [(hlc/ms-of over) (hlc/counter-of over)]))
      (is (= (hlc/pack (inc now-ms) 1) (hlc/next-stamp now-ms over [])) "the wall still behind: the counter counts on")
      (is (= (hlc/pack (inc now-ms) 1) (hlc/next-stamp (inc now-ms) over [])) "the wall reaching the carried millisecond")
      (is (= (hlc/pack (+ now-ms 2) 0) (hlc/next-stamp (+ now-ms 2) over [])) "the wall past it: the counter starts again")))
  (testing "65,536 decisions in one millisecond fit it; the 65,537th is the next millisecond's first"
    (let [ss (vec (stamps-over (repeat 65537 now-ms) 0))]
      (is (every? #(= now-ms (hlc/ms-of %)) (subvec ss 0 65536)))
      (is (= (hlc/pack now-ms 65535) (ss 65535)))
      (is (= (hlc/pack (inc now-ms) 0) (ss 65536)))
      (is (apply < ss)))))

(deftest a-stood-on-stamp-ahead-of-the-wall
  (let [ahead (hlc/pack (+ now-ms 60000) 5)
        last (hlc/pack now-ms 3)
        s (hlc/next-stamp now-ms last [ahead (hlc/pack (- now-ms 10) 9)])]
    (is (= (inc ahead) s) "the promise wins over the wall")
    (is (= (+ now-ms 60000) (hlc/ms-of s)))
    (is (= (inc s) (hlc/next-stamp (+ now-ms 1) s [])) "the unit keeps the lead until the wall catches up: never backward")
    (is (= (hlc/pack (+ now-ms 60001) 0) (hlc/next-stamp (+ now-ms 60001) s [])) "the wall caught up")))

(deftest stamps-stay-near-the-wall-at-a-high-rate
  (testing "2,250 decisions a second for 18 seconds on one unit: the millisecond part never leads the wall"
    (let [n (* 18 2250)
          walls (map #(+ now-ms (quot (* % 1000) 2250)) (range n))
          ss (stamps-over walls 0)
          lead (apply max (map #(- (hlc/ms-of %2) %1) walls ss))
          ms-lead (apply max (map - (ms-unit-stamps walls 0) walls))]
      (println "OBSERVED hybrid: largest lead over the wall" lead "ms; the millisecond unit's, same walls:" ms-lead "ms")
      (is (apply < ss))
      (is (= 0 lead))
      (is (< 20000 ms-lead) "the millisecond unit over the same decisions ran more than 20 s ahead, as the bench saw")))
  (testing "past 65,536 decisions in a millisecond the lead is one millisecond per 65,536 decisions"
    (let [walls (repeat (* 3 65536) now-ms)
          ss (stamps-over walls 0)]
      (is (= (+ now-ms 2) (hlc/ms-of (last ss))))
      (is (= 65535 (hlc/counter-of (last ss)))))))

(deftest overflow-is-refused-not-wrapped
  (testing "R16's bound: a stood-on stamp just below 2^62 is followed; the maximum long throws, which the gate's guard makes a refusal"
    (is (= env/max-carried-stamp (hlc/next-stamp now-ms 0 [(dec env/max-carried-stamp)])))
    (is (= (inc env/max-carried-stamp) (hlc/next-stamp now-ms env/max-carried-stamp [])))
    (is (thrown? ArithmeticException (hlc/next-stamp now-ms 0 [Long/MAX_VALUE])))
    (is (thrown? ArithmeticException (hlc/next-stamp now-ms Long/MAX_VALUE [])))
    (is (thrown? ArithmeticException (hlc/pack (bit-shift-left 1 48) 0)) "a wall past millisecond 2^47 cannot be packed")))
