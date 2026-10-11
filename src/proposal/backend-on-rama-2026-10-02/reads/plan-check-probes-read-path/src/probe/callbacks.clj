(ns probe.callbacks
  "A probe for the check of the plan of the read path, 12 October 2026. No
  code of the store, and no form, name or table shape for it: one table that
  a stream topology writes, a top-level map from a String to a row.

  What it asks of Rama 1.6.0's client, in-process, 4 tasks, 2 threads:
    C1  when a push's callback runs, does that push already hold the new
        value (a deref inside the callback), or still the old one?
    C2  one event changes a row that two pushes watch (a place, and a run
        that holds it): inside each callback, what does the other push hold?
    C3  a push opened with the call that does not block: is its first
        callback a ResyncDiff, and does it come before the future is given?
        On which thread does a function chained to the future run, for a
        push and for a select?
    C4  a callback that does not return for 2.5 s: are the callbacks of
        other pushes held back? A select from another thread? The opening of
        a new push?
    C5  inside a callback: close! of the push whose callback it is; close!
        of another push; a select with a bound; the opening of a push with a
        bound
    C6  close! from another thread while the push's callback is running
    C7  the order in which a table keeps String keys with letters outside
        ASCII and outside the first plane
    C8  what `foreign-object-info` costs a client

  Run, from this folder:  clojure -M:callbacks"
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest]
            [com.rpl.rama.ops :as ops]
            [clojure.string :as str])
  (:import [com.rpl.rama Helpers]
           [java.util.concurrent CompletableFuture TimeUnit CountDownLatch]))

(defmodule CallbackProbe [setup topologies]
  (declare-depot setup *writes (hash-by :route))
  (let [s (stream-topology topologies "s")]
    (declare-pstate s $$rows {String (fixed-keys-schema {:value String :stamp Long})})
    (<<sources s
      (source> *writes :> {:keys [*changes]})
      (ops/explode *changes :> {:keys [*k *v *st]})
      (<<if (nil? *v)
        (local-transform> [(keypath *k) NONE>] $$rows)
       (else>)
        (hash-map :value *v :stamp *st :> *row)
        (local-transform> [(keypath *k) (termval *row)] $$rows)))))

(def t0 (System/nanoTime))
(defn- now-ms [] (/ (double (- (System/nanoTime) t0)) 1e6))

(defn say [& xs]
  (println (str "PROBE " (format "%9.1f" (now-ms)) "  " (str/join " " (map #(if (string? %) % (pr-str %)) xs))))
  (flush))

(defn- root-msg [^Throwable t]
  (let [r (last (take-while some? (iterate #(.getCause ^Throwable %) t)))]
    (str (.getSimpleName (class r)) ": "
         (-> (or (.getMessage ^Throwable r) "") (str/split #"\n") first (#(subs % 0 (min 160 (count %))))))))

(defn- within
  "The value of a CompletableFuture within ms, or [:timeout] or [:threw text]."
  [^CompletableFuture f ms]
  (try (.get f (long ms) TimeUnit/MILLISECONDS)
       (catch java.util.concurrent.TimeoutException _ [:timeout])
       (catch Throwable t [:threw (root-msg t)])))

(defn- timed
  "Runs thunk. Gives [milliseconds, what it gave or [:threw text]]."
  [thunk]
  (let [a (System/nanoTime)
        v (try (thunk) (catch Throwable t [:threw (root-msg t)]))]
    [(/ (Math/round (/ (double (- (System/nanoTime) a)) 1e4)) 100.0) v]))

(defn- thread-name [] (.getName (Thread/currentThread)))
(defn- kind [diff] (when diff (.getSimpleName (class diff))))

(defn -main [& _]
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc CallbackProbe {:tasks 4 :threads 2})
    (let [m     (get-module-name CallbackProbe)
          depot (foreign-depot ipc m "*writes")
          rows  (foreign-pstate ipc m "$$rows")
          ks    (vec (Helpers/genHashingIndexKeys (int 4)))
          w!    (fn [route changes] (foreign-append! depot {:route route :changes changes} :ack))]
      (say "START · keys of the tasks" ks)

      ;; ---------------------------------------------------------------- C1, C2
      (w! (ks 0) [{:k "L p a" :v "a1" :st 1} {:k "L p b" :v "b1" :st 1}])
      (let [place (atom nil) run (atom nil) seen (atom [])
            cb-place (fn [new diff old]
                       (swap! seen conj
                              {:push :place :diff (kind diff) :thread (thread-name) :at (now-ms)
                               :own-holds (when-let [p @place] (cond (= @p new) :the-new (= @p old) :the-old :else :another))
                               :other-holds (when-let [r @run] (let [v (get @r "L p a")] (cond (= v new) :the-new (= v old) :the-old :else :another)))}))
            cb-run   (fn [new diff old]
                       (swap! seen conj
                              {:push :run :diff (kind diff) :thread (thread-name) :at (now-ms)
                               :own-holds (when-let [r @run] (cond (= @r new) :the-new (= @r old) :the-old :else :another))
                               :other-holds (when-let [p @place] (let [n (get new "L p a") o (get old "L p a")]
                                                                   (cond (= @p n) :the-new (= @p o) :the-old :else :another)))}))]
        (reset! place (foreign-proxy [(keypath "L p a")] rows {:pkey (ks 0) :callback-fn cb-place}))
        (reset! run (foreign-proxy [(sorted-map-range "L p " "L p!")] rows {:pkey (ks 0) :callback-fn cb-run}))
        (Thread/sleep 500)
        (doseq [c @seen] (say "C1 at the opening:" c))
        (reset! seen [])
        (dotimes [i 6]
          (w! (ks 0) [{:k "L p a" :v (str "a" (+ 2 i)) :st (+ 2 i)}])
          (Thread/sleep 150))
        (Thread/sleep 300)
        (say "C1 six writes of one place, one event each. Callbacks:" (count @seen)
             "· the push's own value inside its callback:" (frequencies (map :own-holds @seen)))
        (say "C2 inside a callback, what the other push of the same row holds:"
             (frequencies (map (juxt :push :other-holds) @seen)))
        (say "C2 threads:" (frequencies (map (juxt :push :thread) @seen)))
        ;; many writes with no pause, so that callbacks queue
        (reset! seen [])
        (dotimes [i 40] (foreign-append-async! depot {:route (ks 0) :changes [{:k "L p a" :v (str "q" i) :st (+ 100 i)}]} :ack))
        (Thread/sleep 1500)
        (say "C1 forty writes with no pause. Callbacks:" (count @seen)
             "· own value inside the callback:" (frequencies (map (juxt :push :own-holds) @seen)))
        (say "C2 the same forty, the other push:" (frequencies (map (juxt :push :other-holds) @seen)))
        (close! @place) (close! @run))

      ;; ---------------------------------------------------------------- C3
      (let [seen (atom [])
            cb   (fn [new diff old] (swap! seen conj {:what :callback :diff (kind diff) :old-nil? (nil? old) :thread (thread-name) :at (now-ms)}))
            a    (now-ms)
            ^CompletableFuture f (foreign-proxy-async [(keypath "L p a")] rows {:pkey (ks 0) :callback-fn cb})
            _    (.thenRun f (reify Runnable (run [_] (swap! seen conj {:what :future-chained :thread (thread-name) :at (now-ms)}))))
            p    (within f 5000)]
        (swap! seen conj {:what :future-given-to-the-caller :thread (thread-name) :at (now-ms)})
        (Thread/sleep 400)
        (say "C3 a push opened with the call that does not block, begun at" (format "%.1f" a))
        (doseq [c (sort-by :at @seen)] (say "C3  " c))
        (let [f2 (foreign-select-one-async [(keypath "L p a")] rows {:pkey (ks 0)})
              th (promise)]
          (.thenRun ^CompletableFuture f2 (reify Runnable (run [_] (deliver th (thread-name)))))
          (say "C3 a select with the call that does not block: a function chained to its future ran on" (deref th 3000 :not-run)))
        ;; a place with no row: the first callback
        (reset! seen [])
        (let [p2 (foreign-proxy [(keypath "L p never")] rows {:pkey (ks 0) :callback-fn cb})]
          (Thread/sleep 300)
          (say "C3 a push on a place with no row, its first callbacks:" (mapv #(select-keys % [:diff :old-nil?]) @seen))
          (close! p2))
        (close! p))

      ;; ---------------------------------------------------------------- C4
      ;; eight pushes on eight places of task 0; one callback sleeps
      (w! (ks 0) (vec (for [i (range 8)] {:k (str "L s k" i) :v "0" :st 1})))
      (let [seen   (atom [])
            block? (atom false)
            mk     (fn [i] (fn [new diff old]
                             (let [a (now-ms)]
                               (when (and (zero? i) @block? (= "block" (:value new)))
                                 (Thread/sleep 2500))
                               (swap! seen conj {:push i :diff (kind diff) :thread (thread-name) :begun a :ended (now-ms) :value (:value new)}))))
            pushes (vec (for [i (range 8)] (foreign-proxy [(keypath (str "L s k" i))] rows {:pkey (ks 0) :callback-fn (mk i)})))]
        (Thread/sleep 400)
        (say "C4 eight pushes on task 0, how many on each thread at the first callback:" (frequencies (map :thread @seen)))
        (let [thread-of (into {} (map (juxt :push :thread) @seen))]
          (reset! seen []) (reset! block? true)
          (let [a (now-ms)]
            (w! (ks 0) [{:k "L s k0" :v "block" :st 2}])
            (Thread/sleep 150)
            (w! (ks 0) (vec (for [i (range 1 8)] {:k (str "L s k" i) :v "after" :st 2})))
            ;; while the sleeper sleeps, from four threads at once: 6 of each kind of call
            (let [sel   (vec (for [i (range 6)] (future (first (timed #(foreign-select-one [(keypath (str "L s k" i))] rows {:pkey (ks 0)}))))))
                  asel  (vec (for [i (range 6)] (future (first (timed #(within (foreign-select-one-async [(keypath (str "L s k" i))] rows {:pkey (ks 0)}) 6000))))))
                  opens (vec (for [i (range 6)] (future (timed #(within (foreign-proxy-async [(keypath (str "L s k" i))] rows {:pkey (ks 0) :callback-fn (fn [& _])}) 6000)))))
                  ms    (fn [fs] (vec (sort (map #(Math/round (double (deref % 9000 -1))) fs))))]
              (say "C4 while one callback sleeps 2.5 s, six of each from other threads, ms each, sorted:")
              (say "C4   a select that blocks:" (ms sel))
              (say "C4   a select that does not block, waited for:" (ms asel))
              (say "C4   the opening of a push, waited for:" (vec (sort (map #(Math/round (double (first (deref % 9000 [-1])))) opens))))
              (doseq [o opens] (let [[_ p] (deref o 9000 [nil [:late]])] (when-not (vector? p) (close! p)))))
            (Thread/sleep 3200)
            (say "C4 the callback that slept ran on" (thread-of 0))
            (say "C4 the other seven pushes, ms from the first write to the callback, by thread:"
                 (into (sorted-map)
                       (for [[same? cs] (group-by #(= (thread-of (:push %)) (thread-of 0)) (filter #(pos? (:push %)) @seen))]
                         [(if same? "the sleeper's thread" "the other thread") (vec (sort (map #(Math/round (- (:begun %) a)) cs)))])))))
        (doseq [p pushes] (close! p)))

      ;; ---------------------------------------------------------------- C5
      ;; inside a callback: close! of the push itself, of another push on the
      ;; same thread, of one on the other thread. The callback then returns.
      (w! (ks 0) (vec (for [i (range 6)] {:k (str "L c k" i) :v "0" :st 1})))
      (let [seen (atom []) did (atom {}) ps (atom {})
            mk (fn [i] (fn [new diff old]
                         (swap! seen conj {:push i :diff (kind diff) :value (:value new) :thread (thread-name) :at (now-ms)})
                         (when (and (zero? i) (= "go" (:value new)))
                           (let [th (:threads @did)
                                 same (first (filter #(and (pos? %) (= (th %) (th 0))) (range 6)))
                                 othr (first (filter #(not= (th %) (th 0)) (range 6)))]
                             (swap! did assoc :same same :othr othr)
                             (swap! did assoc :close-own (timed #(do (close! (@ps 0)) :returned)))
                             (swap! did assoc :status-own-inside (proxy-status (@ps 0)))
                             (swap! did assoc :deref-own-inside (try (:value @(@ps 0)) (catch Throwable t [:threw (root-msg t)])))
                             (swap! did assoc :close-same-thread (timed #(do (close! (@ps same)) :returned)))
                             (swap! did assoc :close-other-thread (timed #(do (close! (@ps othr)) :returned)))))))]
        (doseq [i (range 6)]
          (swap! ps assoc i (foreign-proxy [(keypath (str "L c k" i))] rows {:pkey (ks 0) :callback-fn (mk i)})))
        (Thread/sleep 300)
        (swap! did assoc :threads (into {} (map (juxt :push :thread) @seen)))
        (reset! seen [])
        (w! (ks 0) [{:k "L c k0" :v "go" :st 2}])
        (Thread/sleep 800)
        (say "C5 inside the callback of push 0, each as [ms, what it gave]:")
        (doseq [k [:close-own :status-own-inside :deref-own-inside :close-same-thread :close-other-thread]]
          (say "C5  " k (get @did k :not-reached)))
        (say "C5 callbacks seen after, in order:" (mapv (juxt :push :diff :value) @seen))
        (say "C5 status after the callback returned: own" (proxy-status (@ps 0))
             "· the one on the same thread" (proxy-status (@ps (:same @did)))
             "· the one on the other thread" (proxy-status (@ps (:othr @did))))
        (reset! seen [])
        (w! (ks 0) (vec (for [i (range 6)] {:k (str "L c k" i) :v "later" :st 3})))
        (Thread/sleep 500)
        (say "C5 a write to all six after: callbacks of" (vec (sort (map :push @seen))) "· closed were" [0 (:same @did) (:othr @did)])
        (doseq [i (range 6)] (close! (@ps i))))

      ;; inside a callback: calls that do not block, waited for with a bound
      (w! (ks 0) [{:k "L e a" :v "0" :st 1}])
      (let [did (atom {}) done (promise) opened (atom [])
            cb (fn [new diff old]
                 (when (= "go" (:value new))
                   (let [a   (now-ms)
                         sel (vec (for [i (range 6)] (foreign-select-one-async [(keypath (str "L s k" i))] rows {:pkey (ks 0)})))
                         ops (vec (for [i (range 6)] (foreign-proxy-async [(keypath (str "L s k" i))] rows {:pkey (ks 0) :callback-fn (fn [& _])})))]
                     (Thread/sleep 2500)
                     (swap! did assoc
                            :selects-given-inside (count (filter #(.isDone ^CompletableFuture %) sel))
                            :openings-given-inside (count (filter #(.isDone ^CompletableFuture %) ops)))
                     (reset! opened ops)
                     (deliver done [sel ops]))))
            p (foreign-proxy [(keypath "L e a")] rows {:pkey (ks 0) :callback-fn cb})]
        (Thread/sleep 300)
        (w! (ks 0) [{:k "L e a" :v "go" :st 2}])
        (let [[sel ops] (deref done 6000 [[] []])]
          (Thread/sleep 300)
          (say "C5 inside a callback that stays 2.5 s: of 6 selects that do not block," (:selects-given-inside @did)
               "were given inside it; of 6 openings of a push," (:openings-given-inside @did))
          (say "C5 0.3 s after the callback returned: selects given" (count (filter #(.isDone ^CompletableFuture %) sel))
               "· openings given" (count (filter #(.isDone ^CompletableFuture %) ops)))
          (doseq [o ops] (let [x (within o 2000)] (when-not (vector? x) (close! x)))))
        (close! p))

      ;; ---------------------------------------------------------------- C6
      (w! (ks 0) [{:k "L d a" :v "0" :st 1}])
      (let [seen (atom []) in (CountDownLatch. 1)
            cb (fn [new diff old]
                 (when (= "hold" (:value new)) (.countDown in) (Thread/sleep 1500))
                 (swap! seen conj {:diff (kind diff) :value (:value new) :ended (now-ms)}))
            p (foreign-proxy [(keypath "L d a")] rows {:pkey (ks 0) :callback-fn cb})]
        (Thread/sleep 300) (reset! seen [])
        (w! (ks 0) [{:k "L d a" :v "hold" :st 2}])
        (.await in 3 TimeUnit/SECONDS)
        (let [[ms v] (timed #(do (close! p) :returned))]
          (say "C6 close! from another thread while the push's callback runs: took" ms "ms →" v "· status at once" (proxy-status p))
          (w! (ks 0) [{:k "L d a" :v "after-close" :st 3}])
          (Thread/sleep 2200)
          (say "C6 callbacks after it:" (mapv (juxt :diff :value) @seen) "· status" (proxy-status p))))

      ;; ---------------------------------------------------------------- C7
      (let [e000  (str "L x" (char 0xE000))
            ffff  (str "L x" (char 0xFFFF))
            smile (str "L x" (String. (Character/toChars 0x1F600)))
            ae    "L xä" zz "L xz" plain "L x" bang "L x!" space "L x a"]
        (w! (ks 2) (vec (for [k [e000 ffff smile ae zz plain bang space]] {:k k :v "1" :st 1})))
        (let [got (vec (keys (foreign-select-one [(sorted-map-range "L x" "L y")] rows {:pkey (ks 2)})))
              nm  {e000 "U+E000" ffff "U+FFFF" smile "U+1F600" ae "ä" zz "z" plain "(nothing)" bang "!" space "(space)a"}]
          (say "C7 the order in which the table keeps keys that differ after \"L x\":" (mapv nm got))
          (say "C7 by Java's compareTo (UTF-16 units):" (mapv nm (sort got)))
          ;; code point order is the order of the bytes of UTF-8
          (say "C7 by code point:" (mapv nm (sort (fn [^String x ^String y]
                                                     (java.util.Arrays/compareUnsigned (.getBytes x "UTF-8") (.getBytes y "UTF-8")))
                                                   got)))))

      ;; ---------------------------------------------------------------- C8
      (let [[ms _] (timed #(dotimes [_ 200] (foreign-object-info rows)))]
        (say "C8 foreign-object-info, 200 calls:" ms "ms in all →" (foreign-object-info rows)))

      ;; ---------------------------------------------------------------- C9
      ;; last, because it can hold a thread of Rama's client for good: a
      ;; select that BLOCKS, called inside a callback. The JVM is ended by
      ;; hand after it, whatever came back.
      (w! (ks 0) [{:k "L f a" :v "0" :st 1}])
      (let [got (promise)
            cb  (fn [new diff old]
                  (when (= "go" (:value new))
                    (deliver got (timed #(:value (foreign-select-one [(keypath "L f a")] rows {:pkey (ks 0)}))))))
            _p  (foreign-proxy [(keypath "L f a")] rows {:pkey (ks 0) :callback-fn cb})]
        (Thread/sleep 300)
        (w! (ks 0) [{:k "L f a" :v "go" :st 2}])
        (say "C9 a select that blocks, called inside a callback, within 20 s:" (deref got 20000 :did-not-come-back)))
      (say "END")
      (flush)
      (System/exit 0))))
