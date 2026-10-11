(ns probe.tasks
  "A probe for the plan of the read path, 12 October 2026. No code of the
  store, and no form, name or table shape for it: its two tables stand in for
  a table that a stream topology writes and one that a microbatch topology
  writes, each a top-level map from a String to a row.

  What it asks of Rama 1.6.0, in-process, 4 tasks:
    T1  does a client learn the number of tasks from a table's handle?
    T2  does Rama's own class, outside its test namespace, give one key for
        each task?
    T3  does a select with such a key read one task, and each task?
    T4  does a push with such a key watch one task, on a table that a
        microbatch topology writes?
    T5  one event that changes, adds and removes places of one run: what does
        the push send, and does a processor of key diffs name each place?
        On which thread is the callback called?
    T6  what does closing a push do: a last callback, its status, its value?
    T7  the calls that do not block, with a partitioning key
    T8  a run that is capped in the path itself: what a select costs on a run
        of 200,000 places, and what a push on a capped run sends
    T9  several places in one call: multi-path, as a select. And `submap`,
        which run 1 showed to throw on the task's thread and take the worker
        down; it runs only with the argument `submap`, and nothing after it

  Run, from this folder:  clojure -M:tasks         (all but the submap case)
                          clojure -M:tasks submap  (T1 to T7 and the submap case)"
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest]
            [com.rpl.rama.ops :as ops]
            [clojure.string :as str])
  (:import [com.rpl.rama Helpers]
           [com.rpl.rama.diffs Diff Diff$Processor KeyDiff KeyDiff$Processor
            KeyRemoveDiff KeyRemoveDiff$Processor]
           [java.util.concurrent CompletableFuture TimeUnit]))

(defmodule TaskProbe [setup topologies]
  (declare-depot setup *s-writes (hash-by :route))
  (declare-depot setup *m-writes (hash-by :route))
  (let [s (stream-topology topologies "s")]
    (declare-pstate s $$s-rows {String (fixed-keys-schema {:value String :stamp Long})})
    (<<sources s
      (source> *s-writes :> {:keys [*op *changes *prefix *from *to]})
      (<<cond
        ;; each change is {:k key :v value-or-nil :st stamp}; nil removes
        (case> (= *op :many))
        (ops/explode *changes :> {:keys [*k *v *st]})
        (<<if (nil? *v)
          (local-transform> [(keypath *k) NONE>] $$s-rows)
         (else>)
          (hash-map :value *v :stamp *st :> *row)
          (local-transform> [(keypath *k) (termval *row)] $$s-rows))

        (case> (= *op :bulk))
        (ops/range> *from *to :> *i)
        (format "%s%06d" *prefix *i :> *place)
        (hash-map :value "v" :stamp 1 :> *row)
        (local-transform> [(keypath *place) (termval *row)] $$s-rows)

        (default>)
        (println "PROBE unknown op" *op))))
  (let [mb (microbatch-topology topologies "mb")]
    (declare-pstate mb $$m-rows {String (fixed-keys-schema {:value String :stamp Long})})
    (<<sources mb
      (source> *m-writes :> %microbatch)
      (%microbatch :> {:keys [*changes]})
      (ops/explode *changes :> {:keys [*k *v *st]})
      (<<if (nil? *v)
        (local-transform> [(keypath *k) NONE>] $$m-rows)
       (else>)
        (hash-map :value *v :stamp *st :> *row)
        (local-transform> [(keypath *k) (termval *row)] $$m-rows)))))

(def t0 (System/nanoTime))
(defn- now-ms [] (/ (double (- (System/nanoTime) t0)) 1e6))
(defn- ms [a b] (format "%.1f" (/ (double (- b a)) 1e6)))

(defn say [& xs]
  (println (str "PROBE " (format "%9.1f" (now-ms)) "  " (str/join " " (map #(if (string? %) % (pr-str %)) xs))))
  (flush))

(defn- root-msg [^Throwable t]
  (let [r (last (take-while some? (iterate #(.getCause ^Throwable %) t)))]
    (str (.getSimpleName (class r)) ": "
         (-> (or (.getMessage ^Throwable r) "") (str/split #"\n") first (#(subs % 0 (min 200 (count %))))))))

(defn- cut [x n] (let [s (str x)] (if (> (count s) n) (str (subs s 0 n) " … (" (count s) " chars)") s)))

(defn keys-of-diff
  "What a processor of KeyDiff and KeyRemoveDiff is handed for one diff."
  [^Diff d]
  (let [acc (atom [])
        p   (reify
              Diff$Processor (unhandled [_] (swap! acc conj [:unhandled]))
              KeyDiff$Processor (processKeyDiff [_ kd] (swap! acc conj [:put (.getKey ^KeyDiff kd)]))
              KeyRemoveDiff$Processor (processKeyRemoveDiff [_ kd] (swap! acc conj [:out (.getKey ^KeyRemoveDiff kd)])))]
    (try (.process d p) (catch Throwable t (swap! acc conj [:threw (root-msg t)])))
    @acc))

(defn- recorder
  "An atom, and a callback that notes each call into it."
  []
  (let [seen (atom [])]
    [seen (fn [newval diff oldval]
            (swap! seen conj {:at     (now-ms)
                              :thread (.getName (Thread/currentThread))
                              :diff   (when diff (.getSimpleName (class diff)))
                              :text   (cut diff 150)
                              :keys   (when diff (keys-of-diff diff))
                              :same?  (= newval oldval)
                              :size   (when (coll? newval) (count newval))}))]))

(defn- within
  "The value of a CompletableFuture within ms, or [:timeout] or [:threw text]."
  [^CompletableFuture f ms]
  (try (.get f (long ms) TimeUnit/MILLISECONDS)
       (catch java.util.concurrent.TimeoutException _ [:timeout])
       (catch Throwable t [:threw (root-msg t)])))

(defn- timed [label n thunk show]
  (dotimes [i n]
    (let [a (System/nanoTime)
          v (try (thunk) (catch Throwable t [:threw (root-msg t)]))
          b (System/nanoTime)]
      (say label "try" (inc i) ":" (ms a b) "ms ·" (show v)))))

(defn- show-calls [label seen]
  (if (empty? @seen)
    (say label "no callback")
    (doseq [c @seen]
      (say label "callback" (:diff c) "· same value as before:" (:same? c) "· size" (:size c)
           "· thread" (:thread c) "· processor got" (:keys c) "·" (:text c)))))

(defn -main [& args]
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc TaskProbe {:tasks 4 :threads 2})
    (let [m      (get-module-name TaskProbe)
          sdepot (foreign-depot ipc m "*s-writes")
          mdepot (foreign-depot ipc m "*m-writes")
          srows  (foreign-pstate ipc m "$$s-rows")
          mrows  (foreign-pstate ipc m "$$m-rows")
          submap? (= "submap" (first args))
          mb-n   (atom 0)
          mb!    (fn [route changes]
                   (foreign-append! mdepot {:route route :changes changes} :append-ack)
                   (rtest/wait-for-microbatch-processed-count ipc m "mb" (swap! mb-n inc)))
          s!     (fn [route changes]
                   (foreign-append! sdepot {:op :many :route route :changes changes} :ack))]
      (say "START")

      ;; T1
      (say "T1 object info of the stream table:" (foreign-object-info srows))
      (say "T1 object info of the microbatch table:" (foreign-object-info mrows))
      (say "T1 object info of a depot:" (foreign-object-info mdepot))

      (let [n   (:num-partitions (foreign-object-info mrows))
            ks  (vec (Helpers/genHashingIndexKeys (int n)))
            ks2 (vec (rtest/gen-hashing-index-keys n))]
        ;; T2
        (say "T2 Helpers/genHashingIndexKeys" n "→" ks "· the same as the test namespace's:" (= ks ks2)
             "· class of one:" (.getName (class (first ks))))

        ;; T3
        (doseq [t (range n)]
          (mb! (ks t) [{:k (str "L e" t " a") :v (str "on-task-" t) :st 1}
                       {:k (str "L e" t " b") :v (str "on-task-" t) :st 1}])
          (s! (ks t) [{:k (str "L e" t " a") :v (str "on-task-" t) :st 1}]))
        (doseq [t (range n)]
          (say "T3 select, microbatch table, key of task" t "→"
               (vec (keys (foreign-select-one [(sorted-map-range "L " "L!")] mrows {:pkey (ks t)})))))
        (doseq [t (range n)]
          (say "T3 select, stream table, key of task" t "→"
               (vec (keys (foreign-select-one [(sorted-map-range "L " "L!")] srows {:pkey (ks t)})))))

        ;; T4
        (let [recs   (vec (repeatedly n recorder))
              pushes (vec (for [t (range n)]
                            (foreign-proxy [(sorted-map-range "L " "L!")] mrows
                                           {:pkey (ks t) :callback-fn (second (recs t))})))]
          (doseq [t (range n)]
            (say "T4 push, microbatch table, key of task" t "· value now" (vec (keys @(pushes t)))))
          (Thread/sleep 600)
          (doseq [t (range n)] (show-calls (str "T4 before any write, push of task " t) (first (recs t))))
          (doseq [t (range n)] (reset! (first (recs t)) []))
          (mb! (ks 2) [{:k "L e2 c" :v "new-on-task-2" :st 2}])
          (Thread/sleep 600)
          (doseq [t (range n)] (show-calls (str "T4 after one write on task 2, push of task " t) (first (recs t))))

          ;; T6, on two of these pushes
          (let [p (pushes 0) [seen _] (recs 0)]
            (reset! seen [])
            (close! p)
            (Thread/sleep 400)
            (show-calls "T6 after close!" seen)
            (say "T6 status after close!:" (proxy-status p) "· deref after close!:" (try (vec (keys @p)) (catch Throwable t [:threw (root-msg t)])))
            (reset! seen [])
            (mb! (ks 0) [{:k "L e0 c" :v "after-close" :st 3}])
            (Thread/sleep 500)
            (show-calls "T6 a write on its task after close!" seen)
            (say "T6 a second close!:" (try (close! p) :no-throw (catch Throwable t [:threw (root-msg t)]))))
          (say "T6 status of an open push:" (proxy-status (pushes 1)))
          (doseq [t [1 2 3]] (close! (pushes t))))

        ;; T5
        (s! (ks 1) [{:k "L m a" :v "a1" :st 1} {:k "L m b" :v "b1" :st 1} {:k "L m c" :v "c1" :st 1}])
        (let [[seen cb] (recorder)
              p (foreign-proxy [(sorted-map-range "L m " "L m!")] srows {:pkey (ks 1) :callback-fn cb})]
          (say "T5 stream table: value now" (vec (keys @p)))
          (Thread/sleep 400) (reset! seen [])
          (s! (ks 1) [{:k "L m a" :v "a2" :st 2} {:k "L m d" :v "d1" :st 2} {:k "L m b" :v nil :st 2}])
          (Thread/sleep 400)
          (show-calls "T5 stream table, one event: change a, add d, remove b:" seen)
          (reset! seen [])
          (s! (ks 1) [{:k "L m a" :v "a2" :st 2}])
          (Thread/sleep 400)
          (show-calls "T5 stream table, the same row written again:" seen)
          (close! p))
        (mb! (ks 1) [{:k "L m a" :v "a1" :st 1} {:k "L m b" :v "b1" :st 1} {:k "L m c" :v "c1" :st 1}])
        (let [[seen cb] (recorder)
              p (foreign-proxy [(sorted-map-range "L m " "L m!")] mrows {:pkey (ks 1) :callback-fn cb})]
          (Thread/sleep 400) (reset! seen [])
          (mb! (ks 1) [{:k "L m a" :v "a2" :st 2} {:k "L m d" :v "d1" :st 2} {:k "L m b" :v nil :st 2}])
          (Thread/sleep 600)
          (show-calls "T5 microbatch table, one batch: change a, add d, remove b:" seen)
          (close! p))
        (let [[seen cb] (recorder)
              p (foreign-proxy [(keypath "L m a")] srows {:pkey (ks 1) :callback-fn cb})]
          (say "T5 one place: value now" @p)
          (Thread/sleep 400) (reset! seen [])
          (s! (ks 1) [{:k "L m a" :v "a3" :st 3}])
          (Thread/sleep 300)
          (show-calls "T5 one place, changed:" seen)
          (reset! seen [])
          (s! (ks 1) [{:k "L m a" :v nil :st 3}])
          (Thread/sleep 300)
          (show-calls "T5 one place, removed:" seen)
          (say "T5 one place: value after the removal" @p)
          (close! p))
        (let [[seen cb] (recorder)
              p (foreign-proxy [(keypath "L m never")] srows {:pkey (ks 1) :callback-fn cb})]
          (say "T5 one place that holds no row: value now" @p)
          (Thread/sleep 300) (reset! seen [])
          (s! (ks 1) [{:k "L m never" :v "first" :st 1}])
          (Thread/sleep 300)
          (show-calls "T5 one place, its first row:" seen)
          (close! p))

        ;; T7
        (say "T7 foreign-select-one-async with :pkey →"
             (let [v (within (foreign-select-one-async [(sorted-map-range "L " "L!")] mrows {:pkey (ks 3)}) 5000)]
               (if (map? v) (vec (keys v)) v)))
        (let [[seen cb] (recorder)
              a (System/nanoTime)
              f (foreign-proxy-async [(sorted-map-range "L " "L!")] mrows {:pkey (ks 3) :callback-fn cb})
              p (within f 5000)]
          (say "T7 foreign-proxy-async with :pkey: a" (.getSimpleName (class f)) "· given in" (ms a (System/nanoTime)) "ms ·"
               (if (vector? p) p (str "status " (proxy-status p) " value now " (vec (keys @p)))))
          (when-not (vector? p) (close! p)))

        ;; T9, on the small runs
        (s! (ks 1) [{:k "L m a" :v "a4" :st 4}])
        (say "T9 select, multi-path of a run, a place, and a place with no row →"
             (try (mapv #(if (sorted? %) (vec (keys %)) %)
                        (foreign-select [(multi-path (sorted-map-range "L m " "L m!") (keypath "L m a") (keypath "L nothing x"))]
                                        srows {:pkey (ks 1)}))
                  (catch Throwable t [:threw (root-msg t)])))
        (when submap?
          ;; Run 1 showed what this does: the select throws on the task's
          ;; thread and the worker goes down. So it runs only when asked for,
          ;; and nothing is run after it.
          (say "T9 select, submap of two keys, one with no row →"
               (try (foreign-select-one [(submap ["L m a" "L m zz"])] srows {:pkey (ks 1)})
                    (catch Throwable t [:threw (root-msg t)])))
          (let [[seen cb] (recorder)
                p (within (foreign-proxy-async [(submap ["L m a" "L m zz"])] srows {:pkey (ks 1) :callback-fn cb}) 6000)]
            (say "T9 push on a submap of two keys:" (if (vector? p) p "opens"))))

        ;; T8: a run of 200,000 places on the stream table, task 3
        (when-not submap?
        (let [a (System/nanoTime)]
          (doseq [i (range 10)]
            (foreign-append! sdepot {:op :bulk :route (ks 3) :prefix "L big k" :from (* i 20000) :to (* (inc i) 20000)} :ack))
          (say "T8 seeding 200,000 places took" (ms a (System/nanoTime)) "ms"))
        (let [from "L big " to "L big!" pk {:pkey (ks 3)}]
          (timed "T8 (a) the run, counted on the task:" 3
                 #(foreign-select-one [(sorted-map-range from to) (view count)] srows pk) str)
          (timed "T8 (b) the run, then its first 3:" 3
                 #(foreign-select-one [(sorted-map-range from to) (sorted-map-range-from-start 3)] srows pk) #(if (map? %) (vec (keys %)) %))
          (timed "T8 (c) the first 3 from the run's start, no end:" 3
                 #(foreign-select-one [(sorted-map-range-from from {:max-amt 3})] srows pk) #(if (map? %) (vec (keys %)) %))
          (timed "T8 (d) the run, then its first 20,001:" 3
                 #(foreign-select-one [(sorted-map-range from to) (sorted-map-range-from-start 20001)] srows pk) #(if (map? %) (str (count %) " places") %))
          (timed "T8 (e) the first 20,001 from the run's start, no end:" 3
                 #(foreign-select-one [(sorted-map-range-from from {:max-amt 20001})] srows pk) #(if (map? %) (str (count %) " places") %))
          (timed "T8 (f) the run whole:" 2
                 #(foreign-select-one [(sorted-map-range from to)] srows pk) #(if (map? %) (str (count %) " places") %))
          (timed "T8 (g) 2,000 places of the run by multi-path, one call:" 2
                 #(foreign-select [(apply multi-path (for [i (range 2000)] (keypath (format "L big k%06d" (* i 97)))))] srows pk)
                 #(if (sequential? %) (str (count (remove nil? %)) " rows of " (count %)) %))
          (timed "T8 (i) 2,000 places of the run, one call each, one after another:" 1
                 #(count (remove nil? (for [i (range 2000)] (foreign-select-one [(keypath (format "L big k%06d" (* i 97)))] srows pk))))
                 str)
          (timed "T8 (j) 2,000 places of the run, one call each, all sent at once:" 2
                 #(let [fs (mapv (fn [i] (foreign-select-one-async [(keypath (format "L big k%06d" (* i 97)))] srows pk)) (range 2000))]
                    (count (remove nil? (map (fn [f] (within f 20000)) fs))))
                 str))
        ;; a push on a capped run, on the small run of task 1: a c d zz
        (let [[seen cb] (recorder)
              p (within (foreign-proxy-async [(sorted-map-range "L m " "L m!") (sorted-map-range-from-start 3)] srows
                                             {:pkey (ks 1) :callback-fn cb}) 6000)]
          (if (vector? p)
            (say "T8 push on a run capped at 3:" p)
            (do (say "T8 push on a run capped at 3: opens · value now" (vec (keys @p)))
                (Thread/sleep 300) (reset! seen [])
                (s! (ks 1) [{:k "L m c" :v "c10" :st 6}])
                (Thread/sleep 300)
                (show-calls "T8 capped push, a place inside the cap changed:" seen)
                (reset! seen [])
                (s! (ks 1) [{:k "L m zz" :v "zz2" :st 6}])
                (Thread/sleep 300)
                (show-calls "T8 capped push, a place of the run beyond the cap changed:" seen)
                (reset! seen [])
                (s! (ks 1) [{:k "L m aa" :v "aa1" :st 6}])
                (Thread/sleep 300)
                (show-calls "T8 capped push, a new place that sorts into the cap:" seen)
                (say "T8 capped push: value now" (vec (keys @p)))
                (close! p)))))
        (say "END")))))
