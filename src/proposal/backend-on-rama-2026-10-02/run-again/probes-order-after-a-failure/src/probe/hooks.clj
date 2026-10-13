(ns probe.hooks
  "Rama's own test hooks, used to make the END of a streaming batch slow with
  the task's thread free. They are not a public API of Rama.

  Why. The stand-in gate has no wait inside an event, so nothing in it can
  make a streaming batch stall with the thread free. In a real cluster a
  batch waits for its followers at its end, and that wait can be slow. The
  in-process cluster has one copy of everything, so its batches wait for
  nothing. Rama 1.6.0's jar holds functions named `hook:...` that take no
  argument and do nothing; Rama's code calls them at set points. This
  namespace puts a function in the place of some of them:

    it counts each call, by hook and by the name of the calling thread, and
    it sleeps on the calling thread when `slow` holds that hook's name, once
    for each number that `slow` holds.

  `install!` must be called before a cluster is made. Whether Rama's compiled
  code calls a replaced function is not known beforehand: the counts tell.

  A probe, not a design."
  (:require [clojure.string :as str]))

(def hook-names
  "The hooks that are replaced: namespace and name."
  '[[rpl.rama.distributed.topology.streaming hook:starting-streaming-batch]
    [rpl.rama.distributed.topology.streaming hook:completed-streaming-batch]
    [rpl.rama.distributed.topology.streaming hook:failed-streaming-batch]
    [rpl.rama.distributed.topology.streaming hook:triggering-reactivity]
    [rpl.rama.distributed.topology.streaming hook:filtering-because-invalid]
    [rpl.rama.distributed.daemon.worker.replication.leader.replog-applies hook:apply-promise:test-hook]
    [rpl.rama.distributed.daemon.worker.replication.leader.replog-applies hook:running-leader-apply-agent]
    [rpl.rama.distributed.daemon.worker.replication.leader.impl hook:replog-append-all]])

(defonce ^{:doc "[hook name, kind of thread] → how many calls."}
  calls (atom {}))

(defonce ^{:doc "The calls in order while `tracing?` holds true: [ms-nanoTime hook thread]."}
  trace (atom []))

(defonce ^{:doc "When true, each call is also kept in `trace`."}
  tracing? (atom false))

(defonce ^{:doc "hook name → a vector of milliseconds; the next call of that hook from a thread whose
  name holds `slow-thread` takes the first number off and sleeps that long on its own thread."}
  slow (atom {}))

(defonce ^{:doc "A part of a thread's name; only calls from such a thread sleep. nil for any thread."}
  slow-thread (atom nil))

(defonce ^{:doc "What `install!` found: hook name → :replaced, or the reason why not."}
  installed (atom {}))

(defn- thread-kind
  "Takes a thread's name. Gives it with its digits taken off, so that the calls can be counted by kind of thread."
  [n]
  (-> n (str/replace #"[0-9a-f]{8}" "x") (str/replace #"\d+" "n")))

(defn- hook-fn
  "Takes a hook's name. Gives the function that takes its place."
  [hook]
  (fn [& _]
    (let [t (.getName (Thread/currentThread))]
      (swap! calls update [hook (thread-kind t)] (fnil inc 0))
      (when @tracing? (swap! trace conj [(System/nanoTime) hook t]))
      (when (and (seq (get @slow hook))
                 (or (nil? @slow-thread) (str/includes? t @slow-thread)))
        (let [[old _] (swap-vals! slow update hook #(vec (rest %)))]
          (when-let [ms (first (get old hook))]
            (when @tracing? (swap! trace conj [(System/nanoTime) (str hook " SLEEPS " ms " ms") t]))
            (Thread/sleep (long ms))
            (when @tracing? (swap! trace conj [(System/nanoTime) (str hook " WAKES") t]))))))
    nil))

(defn install!
  "Puts the counting function in the place of each hook of `hook-names` that exists. Gives what was done for each."
  []
  (doseq [[n s] hook-names]
    (let [k (str s)]
      (try
        (require n)
        (if-let [v (ns-resolve n s)]
          (do (alter-var-root v (constantly (hook-fn k)))
              (swap! installed assoc k :replaced))
          (swap! installed assoc k "no such name"))
        (catch Throwable t
          (swap! installed assoc k (str "threw " (.getSimpleName (class t))))))))
  @installed)

(defn reset-counts!
  "Empties the counts, the trace and what is to be slow."
  []
  (reset! calls {})
  (reset! trace [])
  (reset! slow {})
  (reset! slow-thread nil)
  (reset! tracing? false))
