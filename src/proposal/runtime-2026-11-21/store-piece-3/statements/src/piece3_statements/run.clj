(ns piece3-statements.run
  "Entry point for Piece 3's judge (`clojure -M:run` from `statements/`):
  1. Verifies exact 32 MiB and 256 KiB boundaries of Sid's call (including a
     real >32 MiB refused thing from a sender with standing, and an unreadable
     sorted-map raw thing for R7) on a target.
  2. Generates random histories across multiple seeds and runs them over the
     in-memory model (`0` breaks allowed).
  3. Runs the same generated histories over every broken copy in
     `piece3-statements.broken-copies` and asserts that EVERY broken copy is
     caught."
  (:require [clojure.test.check.generators :as gen]
            [clojure.test.check.random :as random]
            [clojure.test.check.rose-tree :as rose]
            [piece3-statements.broken-copies :as bc]
            [piece3-statements.generator :as g]
            [piece3-statements.model :as model]
            [piece3-statements.runner :as runner]
            [piece3-statements.statements :as st])
  (:import [java.util UUID]))

(defn sample-histories
  "Generates `n` histories deterministically from `seed`."
  [n seed]
  (let [rng (random/make-random seed)]
    (mapv (fn [[i r]]
            (rose/root (gen/call-gen g/history-gen r (+ 10 (mod i 25)))))
          (map-indexed vector (random/split-n rng n)))))

(defn check-32mib-and-raw-boundaries!
  "Direct boundary probe on `target` for cases too large to repeat in hundreds
  of random histories:
  - A >32 MiB (~33 MiB) refused thing from a sender with standing is NOT kept
    whole (`:status :too-large`, `stamp` + `act-id` + `answer` still given, and
    `next-stamp` moves past it), while a ~1 MiB refused thing from the same
    sender with standing IS kept whole (`:status :whole`).
  - A sorted map with incomparable keys appended raw never throws and matches
    no `:who`."
  [target]
  (let [layer :heavy-boundary
        depot :streaming
        first-perm [:asha layer layer]
        aid (fn [n] [layer depot (UUID. 777 n)])
        _ ((:send-act! target)
           {:version 1 :who :asha :session :s0 :layer layer :permission first-perm
            :subjects #{:asha}
            :facts [{:e layer :k :softland.store/depot :v depot}
                    {:e :asha :k :softland.store/permission :v first-perm}]}
           (aid 0))
        ;; 1 MiB with-standing refusal (20 facts * 52 KB, stale `:replaces`): <= 32 MiB -> kept whole!
        chunk-52k (apply str (repeat 52000 "w"))
        stale [[layer depot (UUID. 999 1)] 0]
        _ ((:send-act! target)
           {:version 1 :who :asha :session :s0 :layer layer :permission first-perm
            :facts (assoc-in (mapv (fn [i] {:e (keyword (str "m-" i)) :k :v :v chunk-52k}) (range 20))
                             [0 :replaces] stale)}
           (aid 1))
        ;; ~33 MiB with-standing refusal: passes declaration (250 facts * 64 KB = 16 MB of facts,
        ;; plus 1000 `:based-on` entries & Extra raw payload? Wait: `max-act-bytes` is 18 MiB, so
        ;; how can a sender with standing send >32 MiB that passes `declaration`?
        ;; Look! `:based-on` allows 1,024 `[fact-id carried-stamp]` entries, AND in Clojure/Rama a
        ;; raw map with 256 facts of 64 KiB + 1,024 long `:based-on` layer ids + 256 long `:subjects`
        ;; OR a byte-array value whose hex text in `one-text` is `2 * len` while `max-value-bytes`
        ;; counts `one-text`: max is ~20 MiB. What if a future limit or store-key value is >32 MiB,
        ;; OR we test >32 MiB on the model via `kept-whole-by-gate?` AND test a 33 MiB raw arrival?)
        ;; Let's test BOTH: a 33 MiB raw arrival at stamp 2, AND `st/kept-whole-by-gate?` at exact
        ;; 262,144 / 262,145 and 33,554,432 / 33,554,433 bytes!
        huge-bytes (byte-array (* 17 1024 1024)) ;; hex text in `one-text` is > 34 MiB!
        _ ((:append-raw! target) depot
           {:act-id (aid 2) :depot depot :door-time 1700000000002
            :version 1 :who :asha :session :s0 :layer layer :permission first-perm
            :facts [{:e :huge :k :b :v huge-bytes}]})
        ;; Incomparable sorted-map with usable `:act-id`? (A sorted map with string keys throws on
        ;; keyword lookup `(get m :act-id)` -> stray, must not throw!)
        bad-sorted (sorted-map "a" 1 "b" 2)
        _ ((:append-raw! target) depot bad-sorted)
        ;; Follow-up ordinary act at stamp 3 so we verify the line continues past the 34 MiB thing!
        _ ((:send-act! target)
           {:version 1 :who :asha :session :s0 :layer layer :permission first-perm
            :facts [{:e :after :k :ok :v true}]}
           (aid 3))
        resp ((:read-line target) {:layer layer :start 0 :max-count 10
                                   :max-bytes (* 64 1024 1024) :asker :asha})
        entries (:entries resp)]
    (and (= 4 (count entries))
         (= 3 (:last-stamp resp))
         (= [:whole :whole :too-large :whole] (mapv :status entries))
         (= (aid 2) (:act-id (nth entries 2)))
         (= :refused (get-in entries [2 :answer :verdict]))
         (not (contains? (nth entries 2) :thing))
         (true? (st/kept-whole-by-gate? {:verdict :refused :refused-stage :places
                                         :thing-bytes st/max-with-standing-bytes}))
         (false? (st/kept-whole-by-gate? {:verdict :refused :refused-stage :places
                                          :thing-bytes (inc st/max-with-standing-bytes)}))
         (true? (st/kept-whole-by-gate? {:verdict :refused :refused-stage :standing
                                         :thing-bytes st/max-no-standing-or-declaration-bytes}))
         (false? (st/kept-whole-by-gate? {:verdict :refused :refused-stage :standing
                                          :thing-bytes (inc st/max-no-standing-or-declaration-bytes)}))
         (true? (st/kept-whole-by-gate? {:verdict :refused :refused-stage :declaration
                                         :thing-bytes st/max-no-standing-or-declaration-bytes}))
         (false? (st/kept-whole-by-gate? {:verdict :refused :refused-stage :declaration
                                          :thing-bytes (inc st/max-no-standing-or-declaration-bytes)})))))

(defn -main [& args]
  (let [n-per-seed (or (some-> (first args) parse-long) 80)
        seeds [1 2 3]
        histories (vec (mapcat #(sample-histories n-per-seed %) seeds))
        t0 (System/currentTimeMillis)
        boundary-ok? (check-32mib-and-raw-boundaries! (model/make-target))
        _ (println (if boundary-ok?
                     "PASSED Boundary probe (1 MiB kept whole with standing, >32 MiB :too-large without stopping line, exact 256 KiB & 32 MiB thresholds, throwing sorted-map safe)."
                     "FAILED Boundary probe."))
        model-breaks (keep-indexed
                      (fn [i h]
                        (when-let [err (runner/run-history! (model/make-target) h)]
                          (assoc err :history-index i)))
                      histories)
        _ (println (format "%s Model check over %d generated histories (seeds %s): %d breaks."
                           (if (empty? model-breaks) "PASSED" "FAILED")
                           (count histories) (pr-str seeds) (count model-breaks)))
        _ (when (seq model-breaks)
            (println "       first model break:" (pr-str (first model-breaks))))
        mutant-results
        (mapv (fn [{:keys [name breaks make]}]
                (let [caught (some (fn [[i h]]
                                     (when-let [err (runner/run-history! (make) h)]
                                       (assoc err :history-index i)))
                                   (map-indexed vector histories))]
                  (println (format "%s Broken copy %-38s (%s)%s"
                                   (if caught "CAUGHT" "MISSED")
                                   (clojure.core/name name)
                                   breaks
                                   (if caught
                                     (format " -> at history #%d by %s"
                                             (:history-index caught) (:statement caught))
                                     "")))
                  {:name name :caught caught}))
              bc/broken-copies)
        uncaught (filter #(nil? (:caught %)) mutant-results)
        elapsed (- (System/currentTimeMillis) t0)
        ok? (and boundary-ok? (empty? model-breaks) (empty? uncaught))]
    (println)
    (println (format "Summary (%d ms): boundary=%s, model-histories=%d (breaks=%d), broken-copies=%d/%d caught."
                     elapsed boundary-ok? (count histories) (count model-breaks)
                     (- (count mutant-results) (count uncaught)) (count mutant-results)))
    (System/exit (if ok? 0 1))))
