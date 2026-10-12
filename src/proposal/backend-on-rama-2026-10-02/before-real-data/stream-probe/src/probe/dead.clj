(ns probe.dead
  "The run for the skill's warning on `:ack` in `depot-partition-append!`:
  \"DO NOT use :ack if depot is consumed by same stream topology as is
  appending it, since that will cause a deadlock\" (`depot-design.md:66`).

  In the other runs of this probe nothing consumes the second depot, so the
  warning does not touch them. This run gives the second depot a consumer,
  in the two ways there are, and appends to it from an event with each ack
  level:

    *side   consumed by another stream topology of the same module
    *own    consumed by the stream topology that appends to it

  The module sets `topology.stream.timeout.seconds` to 5, so that an event
  that can never end is failed by Rama and the run goes on. A thing asks for
  its ack level on its first attempt only; a later attempt appends with
  :append-ack.

  Run:  clojure -M:dead   (from this folder; the log is logs/dead.log)"
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest]
            [com.rpl.rama.ops :as ops]
            [probe.util :as u]))

(defonce ^{:doc "id → how many times the event of that thing began."}
  attempts (atom {}))

(defonce ^{:doc "Lines kept for the end: [ms what id attempt]."}
  marks (atom []))

(defn dead-begin!
  "The appending event calls it first. Takes the thing. Counts the attempt, says it, gives its number."
  [thing]
  (let [id (:id thing)
        n (get (swap! attempts update id (fnil inc 0)) id)]
    (swap! marks conj [(u/ms) :begin id n])
    (u/say "INSIDE " id "attempt" n "begins; it will append to" (name (:to thing)) "with"
           (pr-str (if (= 1 n) (:ack thing) :append-ack)))
    n))

(defn ack-now
  "Takes the thing and the attempt. Gives the ack level of this attempt's append."
  [thing attempt]
  (if (= 1 attempt) (:ack thing) :append-ack))

(defn dead-after!
  "The appending event calls it behind its append. Takes the thing and the attempt."
  [thing attempt]
  (swap! marks conj [(u/ms) :after (:id thing) attempt])
  (u/say "INSIDE " (:id thing) "attempt" attempt "is behind its append")
  nil)

(defn dead-consumed!
  "The consuming event calls it. Takes the depot's word, the id and the attempt of the record."
  [depot id attempt]
  (swap! marks conj [(u/ms) :consumed id attempt])
  (u/say "INSIDE  the consumer of" depot "got the record of" id "attempt" attempt)
  nil)

(defmodule DeadProbe [setup topologies]
  (declare-depot setup *in (hash-by :k))
  (declare-depot setup *own :disallow)
  (declare-depot setup *side :disallow)
  (set-launch-topology-dynamic-option! setup "gate" "topology.stream.timeout.seconds" 5)
  (let [s (stream-topology topologies "gate")]
    (declare-pstate s $$rows {String Object})
    (<<sources s
      (source> *in {:start-from :beginning :retry-mode :all-after} :> *thing)
      (identity *thing :> {:keys [*id *to]})
      (dead-begin! *thing :> *attempt)
      (ack-now *thing *attempt :> *ack)
      (hash-map :id *id :attempt *attempt :> *record)
      (<<if (= :own *to)
        (depot-partition-append! *own *record *ack)
       (else>)
        (depot-partition-append! *side *record *ack))
      (dead-after! *thing *attempt)
      (local-transform> [(keypath *id) (termval *attempt)] $$rows)

      (source> *own {:start-from :beginning} :> *own-record)
      (identity *own-record :> {*own-id :id *own-attempt :attempt})
      (dead-consumed! "*own" *own-id *own-attempt)
      (local-transform> [(keypath (str "own/" *own-id)) (termval *own-attempt)] $$rows)))
  (let [o (stream-topology topologies "other")]
    (declare-pstate o $$side-rows {String Object})
    (<<sources o
      (source> *side {:start-from :beginning} :> *side-record)
      (identity *side-record :> {*side-id :id *side-attempt :attempt})
      (dead-consumed! "*side" *side-id *side-attempt)
      (local-transform> [(keypath *side-id) (termval *side-attempt)] $$side-rows))))

(defn -main [& _]
  (u/open-log! "logs/dead.log")
  (try
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc DeadProbe {:tasks 4 :threads 4})
      (let [m (get-module-name DeadProbe)
            in (foreign-depot ipc m "*in")
            own (foreign-depot ipc m "*own")
            side (foreign-depot ipc m "*side")
            k (second (rtest/gen-hashing-index-keys "key-" 4))]
        (u/say "RUN     Rama 1.6.0, in-process, 4 tasks, 4 threads, topology.stream.timeout.seconds 5 · every thing goes to task 1")
        (doseq [[to ack] [[:side :append-ack] [:side nil] [:side :ack]
                          [:own :append-ack] [:own nil] [:own :ack]]]
          (let [id (str (name to) "-" (if ack (name ack) "no-ack"))
                t (u/ms)]
            (u/say "RUN     ---- an append to" (if (= :own to) "*own, which the appending topology consumes," "*side, which another stream topology consumes,")
                   "with" (pr-str ack) "----")
            (let [ret (u/within 60000 #(foreign-append! in {:k k :id id :to to :ack ack} :ack))]
              (u/say "CLIENT  the :ack append of" id "gave" (if (vector? ret) (second ret) (pr-str ret))
                     "after" (format "%.1f" (- (u/ms) t)) "ms · events of" id "begun:" (get @attempts id)))
            (Thread/sleep 1500)))
        (Thread/sleep 6000)
        (u/say "RESULT  events begun, by thing:" (pr-str (into (sorted-map) @attempts)))
        (u/say "RESULT  *side, partition 1:" (pr-str (foreign-depot-read side 1 0 (:end-offset (foreign-depot-partition-info side 1)))))
        (u/say "RESULT  *own, partition 1:" (pr-str (foreign-depot-read own 1 0 (:end-offset (foreign-depot-partition-info own 1)))))
        (u/say "RUN     END")))
    (catch Throwable t
      (u/say "RUN     THREW" (u/root-message t))
      (.printStackTrace t)))
  (shutdown-agents)
  (System/exit 0))
