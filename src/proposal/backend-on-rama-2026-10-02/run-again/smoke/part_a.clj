(ns run-again.smoke.part-a
  "A smoke run of part A of the build \"run again\", on the store itself:
  the door writes the time on the act, the streaming gate makes its stamp
  of that time and of the layer's last stamp, a thing with no usable time
  is refused, the word of an act for the base on the streaming depot comes
  from the act alone, a second send reads the first's answer, and the
  streaming gate has no depot of answers.

  It is no proof. It starts `Store` in Rama's in-process cluster with 4
  tasks, sends a few things, and prints one line that starts with SMOKE for
  each thing that it shows. It checks nothing and always exits 0 unless
  something throws: read the lines.

  Run it behind the shared lock, from the repository root, with the output
  in a file, and read the file only by grep for ^SMOKE and for Exception:

    flock src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock \\
      clojure -M:store-module -e '(load-file \"src/proposal/backend-on-rama-2026-10-02/run-again/smoke/part_a.clj\")' \\
      > src/proposal/backend-on-rama-2026-10-02/run-again/smoke/raw-part-a.txt 2>&1

  State that it changes: none outside its own in-process cluster, which it
  closes."
  (:require [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [com.rpl.rama.test :as rtest]
            [softland.store.door :as door]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.gate.stamp :as stamp]
            [softland.store.module :as module])
  (:import [java.util UUID]))

(defn- say
  "Prints one line of the smoke run: SMOKE, the letter of the point, and
  the things given, each printed readably."
  [point & things]
  (println (apply str "SMOKE " point (map #(str " " (pr-str %)) things))))

(defn- own
  "The first permission of a personal layer: its person holds it."
  [who]
  [who who who])

(defn- making-of
  "The making act of the personal streaming layer of a who, as a maker
  gives it."
  [who]
  {:version 1 :who :system :layer who :permission (own who) :session :smoke
   :facts [{:e who :k :softland.store/depot :v :streaming}
           {:e who :k :softland.store/permission :v (own who)}]})

(defn- act-of
  "An act of a person into her own layer, as a maker gives it: one value
  under the plain key :text."
  [who text]
  {:version 1 :who who :layer who :permission (own who) :session :smoke
   :facts [{:e who :k :text :v text}]})

(def ^:private base-making
  "The making act of the base, as a maker gives it."
  {:version 1 :who :system :layer :base :permission [:system :base :base] :session :smoke
   :facts [{:e :base :k :softland.store/depot :v :microbatch}
           {:e :system :k :softland.store/permission :v [:system :base :base]}]})

(defn- by-hand
  "A thing as a hand appends it, without the door: the maker's act with an
  act-id of the layer on the streaming depot, the depot, and, when a time
  is given, that time under the declaration's name for it."
  ([act] (by-hand act ::none))
  ([act door-time]
   (cond-> (assoc act :act-id [(:layer act) :streaming (UUID/randomUUID)] :depot :streaming)
     (not= ::none door-time) (assoc declaration/time-part door-time))))

(defn- sent-by-hand
  "Appends a thing to the streaming depot with the ack level :ack, and
  reads its answer back through the door by its act-id and its digest.
  Gives the answer."
  [d depot thing]
  (rama/foreign-append! depot thing :ack)
  (door/answer-of d (:act-id thing) (digest/hex-of thing) 20000))

(defn- log-of
  "Each record of a depot's log, as [partition record], in the order of the
  partitions and of each partition's offsets."
  [depot]
  (vec (for [p (range (:num-partitions (rama/foreign-object-info depot)))
             :let [{:keys [start-offset end-offset]} (rama/foreign-depot-partition-info depot p)]
             r (rama/foreign-depot-read depot p start-offset end-offset)]
         [p r])))

(defn- stamp-parts
  "The two halves of a stamp, for a line: [time counter]."
  [s]
  (when s [(stamp/time-of s) (stamp/counter-of s)]))

(defn- run
  "The smoke run: the points a to f of the brief of part A, in an order in
  which the base is made last."
  [ipc]
  (let [m (rama/get-module-name module/Store)
        d (door/open ipc m)
        streaming (rama/foreign-depot ipc m "*streaming-depot")
        task-table (rama/foreign-pstate ipc m "$$streaming-task")
        last-stamp-of (fn [layer]
                        (rama/foreign-select-one (path/keypath :last-stamp (declaration/id-text layer))
                                                 task-table {:pkey layer}))]

    ;; a. Through the door: a making act of a streaming layer is accepted,
    ;; and the time in its stamp is the time that the door wrote. The thing
    ;; is read back from the depot's partition.
    (let [sent (door/send-act d (making-of :alice))
          answer (door/answer-of d (:act-id sent) (:digest sent) 20000)
          thing (some (fn [[_ r]] (when (= (:act-id sent) (:act-id r)) r)) (log-of streaming))
          door-time (get thing declaration/time-part)]
      (say "a" :answer (:answer answer) :ack (:ack sent)
           :door-time-on-the-depot door-time
           :time-in-the-stamp (stamp/time-of (:stamp answer))
           :counter (stamp/counter-of (:stamp answer))
           :equal (= door-time (stamp/time-of (:stamp answer)))
           :last-stamp-of-the-layer-is-the-stamp (= (:stamp answer) (last-stamp-of :alice))))

    ;; b. Two layers on one task, things sent by hand with one chosen time.
    ;; Eight layers are made, so two of them are on one of the four tasks.
    (let [t0 1700000000000
          layers (mapv #(keyword (str "smoke-" %)) (range 8))
          made (into {} (map (fn [layer]
                               [layer (sent-by-hand d streaming (by-hand (making-of layer) t0))]))
                     layers)
          partition-of (into {} (keep (fn [[p r]] (when (some #{(:layer r)} layers) [(:layer r) p])))
                             (log-of streaming))
          [l1 l2] (some (fn [[_ ls]] (when (<= 2 (count ls)) (vec (take 2 (sort ls)))))
                        (group-by partition-of layers))
          a1 (sent-by-hand d streaming (by-hand (act-of l1 "one") t0))
          a2 (sent-by-hand d streaming (by-hand (act-of l1 "two") t0))
          b1 (sent-by-hand d streaming (by-hand (act-of l2 "one") t0))]
      (say "b" :chosen-time t0 :layers [l1 l2] :partitions [(partition-of l1) (partition-of l2)])
      (say "b" :making-acts-of-the-eight
           (mapv (fn [layer] [(partition-of layer) (:answer (made layer)) (stamp-parts (:stamp (made layer)))])
                 layers))
      (say "b" :layer-1 l1 :making (stamp-parts (:stamp (made l1)))
           :second-act (stamp-parts (:stamp a1)) :third-act (stamp-parts (:stamp a2))
           :answers [(:answer a1) (:answer a2)])
      (say "b" :layer-2 l2 :making (stamp-parts (:stamp (made l2)))
           :second-act-after-layer-1s-three (stamp-parts (:stamp b1)) :answer (:answer b1))
      (say "b" :last-stamps-kept [(stamp-parts (last-stamp-of l1)) (stamp-parts (last-stamp-of l2))]
           :layer-2s-counter-is-its-own (= 1 (stamp/counter-of (:stamp b1))))

      ;; c. By hand: a usable act-id and no time, and a time of 2^46.
      (let [no-time (sent-by-hand d streaming (by-hand (act-of l1 "no time")))
            nil-time (sent-by-hand d streaming (by-hand (act-of l1 "nil time") nil))
            too-great (sent-by-hand d streaming (by-hand (act-of l1 "2^46") (bit-shift-left 1 46)))
            greatest (sent-by-hand d streaming (by-hand (act-of l2 "2^46 less one") (dec (bit-shift-left 1 46))))]
        (say "c" :no-time (select-keys no-time [:answer :reason]) :stamp (stamp-parts (:stamp no-time)))
        (say "c" :nil-time (select-keys nil-time [:answer :reason]) :stamp (stamp-parts (:stamp nil-time)))
        (say "c" :time-2-to-the-46 (select-keys too-great [:answer :reason]) :stamp (stamp-parts (:stamp too-great)))
        (say "c" :time-2-to-the-46-less-one (select-keys greatest [:answer :reason])
             :below-2-to-the-62 (< (:stamp greatest) (bit-shift-left 1 62)))))

    ;; d. By hand, on the streaming depot: an act for the base under a
    ;; streaming act-id, while the base is not made.
    (let [answer (sent-by-hand d streaming (by-hand base-making 1700000000000))]
      (say "d" :base-on-the-streaming-depot (select-keys answer [:answer :reason])
           :depot-of-the-base-read (pr-str (rama/foreign-invoke-query
                                            (rama/foreign-query ipc m "depot-of-layer") :base))))

    ;; e. A second send of one act under one act-id, through the door.
    (let [act (act-of :alice "sent twice")
          act-id (door/act-id-for d :alice)
          first-send (door/send-act d act act-id)
          _ (Thread/sleep 25)
          second-send (door/send-act d act act-id)
          times (mapv (fn [[_ r]] (get r declaration/time-part))
                      (filter (fn [[_ r]] (= act-id (:act-id r))) (log-of streaming)))
          first-answer (door/answer-of d act-id (:digest first-send) 20000)
          second-answer (door/answer-of d act-id (:digest second-send) 20000)]
      (say "e" :same-digest (= (:digest first-send) (:digest second-send))
           :times-on-the-depot times :times-differ (apply not= times))
      (say "e" :first-answer first-answer :second-answer second-answer
           :one-answer (= first-answer second-answer)
           :time-in-the-stamp (stamp/time-of (:stamp first-answer))))

    ;; f. No depot of answers of the streaming gate, and the base through
    ;; the door is still answered.
    (let [fetch (fn [depot-name]
                  (try (let [depot (rama/foreign-depot ipc m depot-name)]
                         [:found (:num-partitions (rama/foreign-object-info depot))])
                       (catch Throwable t [:failed (.getName (class t))])))]
      (say "f" :streaming-answer-depot (fetch "*streaming-answer-depot")
           :streaming-depot (fetch "*streaming-depot")))
    (let [sent (door/send-act d base-making)
          answer (door/answer-of d (:act-id sent) (:digest sent) 120000)]
      (say "f" :base-through-the-door (select-keys sent [:ack]) :answer (select-keys answer [:answer :reason])
           :act-id-depot (nth (:act-id sent) 1)))))

(try
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc module/Store {:tasks 4 :threads 2 :workers 2})
    (run ipc))
  (println "SMOKE done")
  (catch Throwable t
    (println "SMOKE Exception" (.getName (class t)) (.getMessage t))
    (.printStackTrace t)))
(shutdown-agents)
(System/exit 0)
