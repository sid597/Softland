(ns probe.runs
  "A second probe for the plan of the read path, 12 October 2026: how a read
  of a run can be bounded in the path itself. It uses the module and the
  helpers of `probe.tasks`. No code of the store.

  `probe.tasks` showed (T8) that a run walked whole and then cut to its first
  places costs the whole walk, and throws for a cut of 20,001. This asks the
  other order: the first n places from the run's start, and then the run's
  own end.

    U1  a window and then the run's end, as a select: on a small run, on a
        run of 200,000, with a window of 257 and of 20,001; and counted
    U2  the same inside a multi-path, beside a place
    U3  the same as a push: what it sends for a change inside the run, for a
        new place in the run, and for a place that is inside the window and
        after the run's end
    U4  a run whose two ends are the same text; a run with its end included
    U6  how large a run is, asked with a bounded walk: the count of a window
        and the key of its last place, in one call
    U5  last, since it may fail on the task: a run whose ends are in the
        wrong order

  Run, from this folder:  clojure -M:runs"
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest]
            [probe.tasks :as t])
  (:import [com.rpl.rama Helpers]))

(defn- within [f ms] (#'t/within f ms))
(defn- root-msg [x] (#'t/root-msg x))
(defn- ms [a b] (#'t/ms a b))

(defn- timed [label n thunk show]
  (dotimes [i n]
    (let [a (System/nanoTime)
          v (try (thunk) (catch Throwable e [:threw (root-msg e)]))
          b (System/nanoTime)]
      (t/say label "try" (inc i) ":" (ms a b) "ms ·" (if (and (vector? v) (= :threw (first v))) v (show v))))))

(defn- ks-of [m] (if (sorted? m) (vec (keys m)) m))
(defn- n-of [m] (if (map? m) (str (count m) " places, first " (first (keys m)) ", last " (last (keys m))) m))

(defn -main [& _]
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc t/TaskProbe {:tasks 4 :threads 2})
    (let [m      (get-module-name t/TaskProbe)
          sdepot (foreign-depot ipc m "*s-writes")
          srows  (foreign-pstate ipc m "$$s-rows")
          ks     (vec (Helpers/genHashingIndexKeys (int 4)))
          s!     (fn [route changes] (foreign-append! sdepot {:op :many :route route :changes changes} :ack))
          pk     {:pkey (ks 3)}]
      (t/say "START")
      ;; task 3: a small run "L aaa ", then a run of 200,000 "L big ", then a small run "L n "
      (s! (ks 3) [{:k "L aaa k1" :v "1" :st 1} {:k "L aaa k2" :v "2" :st 1} {:k "L aaa k3" :v "3" :st 1}
                  {:k "L n x" :v "x" :st 1}])
      (doseq [i (range 10)]
        (foreign-append! sdepot {:op :bulk :route (ks 3) :prefix "L big k" :from (* i 20000) :to (* (inc i) 20000)} :ack))
      (t/say "seeded: 3 places of L aaa, 200,000 of L big, 1 of L n, all on one task")

      ;; U1
      (timed "U1 small run, exact range:" 3
             #(foreign-select-one [(sorted-map-range "L aaa " "L aaa!")] srows pk) ks-of)
      (timed "U1 small run, window of 257 then the run's end:" 3
             #(foreign-select-one [(sorted-map-range-from "L aaa " {:max-amt 257}) (sorted-map-range "L aaa " "L aaa!")] srows pk) ks-of)
      (timed "U1 small run, window of 20,001 then the run's end:" 3
             #(foreign-select-one [(sorted-map-range-from "L aaa " {:max-amt 20001}) (sorted-map-range "L aaa " "L aaa!")] srows pk) ks-of)
      (timed "U1 run of 200,000, window of 257 then the run's end:" 3
             #(foreign-select-one [(sorted-map-range-from "L big " {:max-amt 257}) (sorted-map-range "L big " "L big!")] srows pk) n-of)
      (timed "U1 run of 200,000, window of 20,001 then the run's end:" 3
             #(foreign-select-one [(sorted-map-range-from "L big " {:max-amt 20001}) (sorted-map-range "L big " "L big!")] srows pk) n-of)
      (timed "U1 run of 200,000, window of 257 then the run's end, counted:" 3
             #(foreign-select-one [(sorted-map-range-from "L big " {:max-amt 257}) (sorted-map-range "L big " "L big!") (view count)] srows pk) str)
      (timed "U1 run of 200,000, window of 20,001 then the run's end, counted:" 3
             #(foreign-select-one [(sorted-map-range-from "L big " {:max-amt 20001}) (sorted-map-range "L big " "L big!") (view count)] srows pk) str)
      (timed "U1 small run, window of 20,001 then the run's end, counted:" 3
             #(foreign-select-one [(sorted-map-range-from "L aaa " {:max-amt 20001}) (sorted-map-range "L aaa " "L aaa!") (view count)] srows pk) str)
      (timed "U1 small run, window of 20,001 with no end, counted:" 3
             #(foreign-select-one [(sorted-map-range-from "L aaa " {:max-amt 20001}) (view count)] srows pk) str)

      ;; U2
      (timed "U2 multi-path: a place, a place with no row, a window of 257 then the run's end:" 2
             #(foreign-select [(multi-path (keypath "L aaa k2")
                                           (keypath "L aaa none")
                                           [(sorted-map-range-from "L aaa " {:max-amt 257}) (sorted-map-range "L aaa " "L aaa!")])]
                              srows pk)
             #(mapv ks-of %))

      ;; U4
      (timed "U4 a run whose two ends are the same text:" 1
             #(foreign-select-one [(sorted-map-range "L aaa k2" "L aaa k2")] srows pk) ks-of)
      (timed "U4 a run with its end included, by the option:" 1
             #(foreign-select-one [(sorted-map-range "L aaa k1" "L aaa k2" {:inclusive-end? true})] srows pk) ks-of)
      (timed "U4 a run with its end included, by a space after the end's text:" 1
             #(foreign-select-one [(sorted-map-range "L aaa k1" "L aaa k2 ")] srows pk) ks-of)

      ;; U3: a push on a window of 6 then the end of the run L aaa; the window
      ;; reaches past the run's end into L big
      (let [[seen cb] (#'t/recorder)
            p (within (foreign-proxy-async [(sorted-map-range-from "L aaa " {:max-amt 6}) (sorted-map-range "L aaa " "L aaa!")] srows
                                           (assoc pk :callback-fn cb)) 6000)]
        (if (vector? p)
          (t/say "U3 push on a window of 6 then the run's end:" p)
          (do (t/say "U3 push on a window of 6 then the run's end: opens · value now" (ks-of @p))
              (Thread/sleep 300) (reset! seen [])
              (s! (ks 3) [{:k "L aaa k2" :v "2b" :st 2}])
              (Thread/sleep 300)
              (#'t/show-calls "U3 a place of the run changed:" seen)
              (reset! seen [])
              (s! (ks 3) [{:k "L aaa k4" :v "4" :st 2}])
              (Thread/sleep 300)
              (#'t/show-calls "U3 a new place in the run:" seen)
              (reset! seen [])
              (s! (ks 3) [{:k "L big k000000" :v "changed" :st 2}])
              (Thread/sleep 300)
              (#'t/show-calls "U3 a place inside the window and after the run's end changed:" seen)
              (t/say "U3 value now" (ks-of @p))
              (close! p))))

      ;; U6: how large a run is, asked with a bounded walk: how many places
      ;; the window holds, and the key of its last place
      (timed "U6 run of 200,000, window of 20,001: its count and its last key, one call:" 3
             #(foreign-select [(sorted-map-range-from "L big " {:max-amt 20001}) (multi-path (view count) [LAST FIRST])] srows pk) vec)
      (timed "U6 small run, window of 20,001: its count and its last key, one call:" 3
             #(foreign-select [(sorted-map-range-from "L aaa " {:max-amt 20001}) (multi-path (view count) [LAST FIRST])] srows pk) vec)
      (timed "U6 the last run of the task, window of 20,001: its count and its last key:" 2
             #(foreign-select [(sorted-map-range-from "L n " {:max-amt 20001}) (multi-path (view count) [LAST FIRST])] srows pk) vec)
      (timed "U6 a window that holds nothing: its count and its last key:" 2
             #(foreign-select [(sorted-map-range-from "L zzz " {:max-amt 20001}) (multi-path (view count) [LAST FIRST])] srows pk) vec)
      (timed "U6 the last place before a run's end, a window of 1 backward:" 2
             #(foreign-select-one [(sorted-map-range-to "L aaa!" {:max-amt 1})] srows pk) ks-of)
      (timed "U6 a push on a run of 200,000, exact range, opened and closed:" 1
             #(let [p (within (foreign-proxy-async [(sorted-map-range "L big " "L big!")] srows pk) 20000)]
                (if (vector? p) p (let [n (count @p)] (close! p) n)))
             str)

      ;; U5, last
      (t/say "U5 a run whose ends are in the wrong order, as a select →"
             (let [v (within (foreign-select-one-async [(sorted-map-range "L aaa k3" "L aaa k1")] srows pk) 8000)]
               (ks-of v)))
      (t/say "U5 a select after it, on the same task →"
             (let [v (within (foreign-select-one-async [(sorted-map-range "L aaa " "L aaa!")] srows pk) 8000)]
               (ks-of v)))
      (t/say "END"))))
