(ns softland.store.module-proof
  "The proof of the store's module on Rama's in-process cluster: the
  streaming gate, its record, and the three reads. One command, from the
  repository root:

    clojure -M:store-module -m softland.store.module-proof

  It prints each thing that it showed on a line that starts with SHOWN,
  prints those lines again together at the end, and exits with a status that
  is not zero when a check fails.

  The door is not built. The proof plays it by hand, with a few functions of
  this namespace, in the order of one act: it reads the class of the layer;
  makes the act-id, with a random UUID; asks for locks by an act and reads
  them with one query; seals each value, the UTF-8 of the one text of the
  value, to its place under a lock that it has not used; appends the thing
  with the ack level :ack; reads the answer by act-id; and compares the
  answer's digest with the digest of what it sent. An error from the append
  is not an answer. It keeps each thing that it sent, so that a repeat is
  the same thing byte for byte.

  State. The proof starts one in-process cluster with four tasks and two
  workers, launches the module `Store` in it, and closes it at the end; the
  cluster's files are temporary and are Rama's. It owns five atoms: the
  lines that it showed; the things that it sent; the first answer of each
  act-id, in the order sent; the locks that it holds and has not used; and
  the one act, with its lock, that later checks come back to. It reads the
  clock to make a carried stamp.

  Approach. The checks run in one order on one cluster, and later checks
  build on the layers, the facts and the permissions that earlier ones
  made. Each check says what must be read back, and reads it back through
  the module's own queries. Where it shows a row, it selects the row from
  the gate's PState with the layer as the partitioning key. Near the end it
  starts the module again in the cluster, by Rama's module update with the
  unchanged module, and reads the record once more; Rama then prints some
  lines of its own log, marked ERROR, that are no failed checks.

  What it does not reach is in the folder's README."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]
            [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [com.rpl.rama.test :as rtest]
            [softland.store.act :as act]
            [softland.store.declaration :as declaration]
            [softland.store.digest :as digest]
            [softland.store.first-act :as first-act]
            [softland.store.module :as module]
            [softland.store.record :as record]
            [softland.store.seal :as seal]
            [softland.store.stamp :as stamp]
            [softland.store.text :as text])
  (:import [java.util UUID]))

(defonce ^:private shown-lines (atom []))

(defn- shown
  "Prints one line of what the proof observed, and keeps it for the recap
  that `-main` prints at the end."
  [& xs]
  (let [line (apply print-str "SHOWN" xs)]
    (swap! shown-lines conj line)
    (println line)
    (flush)))

(defn- check
  "Checks that what was observed is what must be. Gives what was observed."
  [what expected observed]
  (is (= expected observed) what)
  observed)

;; ------------------------------------------------------------------ the hand

(def ^:private tasks "The number of tasks of the proof's cluster." 4)

(def ^:dynamic ^:private *hand*
  "What the proof's hand holds of the cluster: the cluster itself, the depot,
  the three queries, and the gate's four PStates. `-main` binds it for the
  run."
  nil)

(defonce ^:private things-sent (atom []))
(defonce ^:private first-answers (atom []))
(defonce ^:private locks-in-hand (atom {}))
(defonce ^:private kept (atom {}))

(defn- open-hand
  "Takes the cluster. Gives the handles that the hand works with."
  [ipc]
  (let [m (rama/get-module-name module/Store)]
    {:ipc        ipc
     :depot      (rama/foreign-depot ipc m "*streaming-depot")
     :answer     (rama/foreign-query ipc m "answer-by-act-id")
     :locks      (rama/foreign-query ipc m "locks-of-request")
     :class      (rama/foreign-query ipc m "class-of-layer")
     :answers-ps (rama/foreign-pstate ipc m "$$streaming-answers")
     :facts-ps   (rama/foreign-pstate ipc m "$$streaming-facts")
     :locks-ps   (rama/foreign-pstate ipc m "$$streaming-locks")
     :task-ps    (rama/foreign-pstate ipc m "$$streaming-task")}))

(defn- class-of-layer
  "Reads the class of a layer, or nil for a layer that is not made. The hand
  tests what it passes before it calls, and gives nil itself for a thing
  that is no id."
  [layer]
  (when (record/class-read layer)
    (rama/foreign-invoke-query (:class *hand*) layer)))

(defn- answer-of
  "Reads the answer under an act-id, or nil for nothing yet. The hand passes
  the act-id's first slot as the layer, and tests the act-id before it
  calls."
  [act-id]
  (let [layer (when (vector? act-id) (nth act-id 0 nil))]
    (when (record/answer-read layer act-id)
      (rama/foreign-invoke-query (:answer *hand*) layer act-id))))

(defn- locks-of
  "Reads the locks of a request, of the indexes from `from` to `to`: a map
  from index to {:bytes :way}."
  [request-act-id from to]
  (let [layer (when (vector? request-act-id) (nth request-act-id 0 nil))]
    (if (record/lock-range-read layer request-act-id from to)
      (rama/foreign-invoke-query (:locks *hand*) layer request-act-id from to)
      {})))

(defn- new-act-id
  "Makes an act-id for a layer: the layer, its class as the module gives it,
  or the class that the layer starts with when it is not made, and a random
  UUID."
  [layer]
  [layer (or (class-of-layer layer) (first-act/class-at-making layer)) (UUID/randomUUID)])

(defn- thing
  "An act as the door sends it: the maker's parts, and the door's act-id and
  class. `more` is merged in last, for the optional parts and for faults."
  ([layer who permission facts] (thing layer who permission facts {}))
  ([layer who permission facts more]
   (let [act-id (new-act-id layer)]
     (merge {:act-id act-id :class (nth act-id 1)
             :version 1 :who who :layer layer :permission permission :session :proof-session
             :facts (vec facts)}
            more))))

(defn- send!
  "Appends a thing to the streaming depot with the ack level :ack, and keeps
  it. Gives :acked, or what the append threw, which is not an answer."
  [x]
  (swap! things-sent conj x)
  (try (rama/foreign-append! (:depot *hand*) x :ack)
       :acked
       (catch Throwable t [:append-failed (.getSimpleName (class t))])))

(defn- decide!
  "Sends a thing and reads its answer by act-id. Gives the thing, the ack,
  the answer, whether the answer's digest is the digest of what was sent,
  and one word for the outcome: :accepted, the reason of a refusal, or
  :nothing-yet. The first answer of each act-id is kept, in the order sent,
  for the check of the stamps."
  [x]
  (let [ack (send! x)
        answer (answer-of (:act-id x))
        mine? (digest/is-digest-of? (:digest answer) x)]
    (when (and answer mine? (not-any? #(= (:act-id x) (:act-id %)) @first-answers))
      (swap! first-answers conj {:act-id (:act-id x) :layer (:layer x) :stamp (:stamp answer)}))
    {:thing x :ack ack :answer answer :mine? mine?
     :word (cond (nil? answer) :nothing-yet
                 (= :accepted (:verdict answer)) :accepted
                 :else (:reason answer))}))

(defn- ask-for-locks!
  "Asks for n locks by an act of `who` under `permission`: one fact under
  :lock-request on the uuid of that act's own act-id. Reads the answer, then
  the locks with one query. Gives the result of the act with :locks, a map
  from index to lock, and :request, the request's act-id. The locks are put
  in the hand for the layer."
  [layer who permission n]
  (let [base (thing layer who permission [])
        request (:act-id base)
        result (decide! (assoc base :facts [{:e (nth request 2) :k :lock-request :v n}]))
        locks (locks-of request 0 255)]
    (swap! locks-in-hand update layer (fnil into [])
           (map (fn [[index lock]] {:lock-id [request index] :lock lock}) (sort-by key locks)))
    (assoc result :request request :locks locks)))

(defn- take-lock!
  "Takes one lock that the hand holds for a layer and has not used. Gives
  {:lock-id :lock}. Throws when the hand holds none: the proof then asked
  for too few."
  [layer]
  (let [taken (first (get @locks-in-hand layer))]
    (when-not taken (throw (ex-info "the hand holds no lock for the layer" {:layer layer})))
    (swap! locks-in-hand update layer (comp vec rest))
    taken))

(defn- plain-bytes
  "The plain bytes that the hand seals for a value: the UTF-8 of the one text
  of the value in its normal form."
  [v]
  (text/utf8 (act/normal-value v)))

(defn- seal-with
  "Seals a value to the place [layer e k] under a lock in hand,
  {:lock-id :lock}. Gives the fact as it arrives: {:e :k :sealed :lock-id}."
  [{:keys [lock-id lock]} layer e k v]
  {:e e :k k :sealed (seal/seal lock [layer e k] (plain-bytes v)) :lock-id lock-id})

(defn- sealed
  "A value fact as it arrives, sealed under a lock that the hand has not
  used."
  [layer e k v]
  (seal-with (take-lock! layer) layer e k v))

(defn- row
  "Selects one row of a PState of the gate, by its key, on the task of the
  layer."
  [pstate layer k]
  (rama/foreign-select-one (path/keypath k) (get *hand* pstate) {:pkey layer}))

(defn- fact-row
  "The row of a place of a layer, or nil for an empty place."
  [layer e k]
  (row :facts-ps layer (record/place-key layer e k)))

(defn- lock-row
  "The row of a lock, by its id."
  [[[layer _ request-uuid] index]]
  (row :locks-ps layer (record/lock-key layer request-uuid index)))

(defn- last-stamp
  "The last stamp that the gate gave on the task of a layer."
  [layer]
  (rama/foreign-select-one (path/keypath :last-stamp) (:task-ps *hand*) {:pkey layer}))

(defn- strays
  "The number of different strays under a word, over every task: one select
  for each task, with a key that hashes to it."
  [word]
  (reduce + (map (fn [k] (rama/foreign-select-one [(path/keypath :strays word) (path/view count)]
                                                  (:task-ps *hand*) {:pkey k}))
                 (rtest/gen-hashing-index-keys tasks))))

(defn- stray-digests
  "The digests of the strays under a word, over every task."
  [word]
  (into #{} (mapcat (fn [k] (rama/foreign-select [(path/keypath :strays word) path/ALL]
                                                 (:task-ps *hand*) {:pkey k}))
                    (rtest/gen-hashing-index-keys tasks))))

(defn- stored
  "Each record of the streaming depot's log, read back: [partition record]."
  []
  (let [depot (:depot *hand*)]
    (for [p (range (:num-partitions (rama/foreign-object-info depot)))
          :let [{:keys [start-offset end-offset]} (rama/foreign-depot-partition-info depot p)]
          r (rama/foreign-depot-read depot p start-offset end-offset)]
      [p r])))

(defn- short-id
  "The first eight characters of the uuid of an act-id, for a line."
  [act-id]
  (subs (str (nth act-id 2)) 0 8))

(defn- told
  "A result as text for a line: the short act-id, the outcome, the stamp."
  [{:keys [thing answer word]}]
  (str (short-id (:act-id thing)) " " word " stamp " (:stamp answer)))

;; ------------------------------------------------------------- the people

(def ^:private alice-own "Alice's permission in her personal layer." [:alice :alice :alice])
(def ^:private alice-in-team "Alice's permission in the group's layer that she makes." [:alice :team :team])
(def ^:private bobs "Bob's permission in Alice's layer, beneath hers." [:bob :alice :alice alice-own])
(def ^:private carols "Carol's permission in Alice's layer, beneath Bob's." [:carol :alice :alice bobs])
(def ^:private daves "Dave's permission in Alice's layer, beneath Carol's: four deep." [:dave :alice :alice carols])

(defn- making
  "The making act of a layer: by `who`, naming the first permission of
  `holder`, with the class that the layer starts with."
  [layer who holder]
  (thing layer who [holder layer layer]
         [{:e layer :k :class :v (first-act/class-at-making layer)}
          {:e holder :k :permission :v [holder layer layer]}]))

(defn- alice
  "Alice's act into her personal layer, with these facts."
  ([facts] (alice facts {}))
  ([facts more] (thing :alice :alice alice-own facts more)))

;; ---------------------------------------------------------------- the checks

(deftest what-rama-carries
  (let [kinds {:vector [1 2 3] :list (list 1 2 3) :map {:a {:b 1}} :keyword :tool/class
               :uuid (UUID/randomUUID) :big-integer 100000000000000000000N :decimal 1.50M
               :bytes (byte-array [1 2 -3]) :set #{1 2} :narrow-integer (int 5) :minus-zero -0.0
               :string "a \"text\"" :nothing nil :subvector (subvec [0 1 2] 1)}
        x {:act-id (new-act-id :carrier) :layer :carrier :kinds kinds}
        sent-digest (digest/hex-of x)
        result (decide! x)
        [partition back] (first (filter (fn [[_ r]] (and (map? r) (= (:act-id x) (:act-id r)))) (stored)))
        back-kinds (:kinds back)]
    (check "the thing was acked" :acked (:ack result))
    (check "the gate's digest of the thing as it arrived is the digest that the door took before the append"
           sent-digest (:digest (:answer result)))
    (check "a thing that is no act, with a usable act-id, gets a recorded refusal" :unknown-part (:word result))
    (check "the record read back from the depot's log is another object than the one sent: it was serialised"
           false (identical? x back))
    (check "the digest of what came back from the depot's log is the digest of what was sent"
           sent-digest (digest/hex-of back))
    (check "a vector comes back a vector, a list a list in its order, a plain map a plain map, bytes the same bytes"
           [true true [1 2 3] true [1 2 -3]]
           [(vector? (:vector back-kinds))
            (and (sequential? (:list back-kinds)) (not (vector? (:list back-kinds))))
            (vec (:list back-kinds))
            (and (map? (:map back-kinds)) (not (record? (:map back-kinds))))
            (vec (:bytes back-kinds))])
    (check "a keyword with its namespace, a UUID, a big integer, a decimal and a set come back equal"
           (select-keys kinds [:keyword :uuid :big-integer :decimal :set])
           (select-keys back-kinds [:keyword :uuid :big-integer :decimal :set]))
    (shown "each kind of value comes back from the depot's log with the digest it was sent with:" sent-digest
           "| partition" partition "| the classes that came back:"
           (pr-str (into (sorted-map) (map (fn [[k v]] [k (some-> v class .getSimpleName)])) back-kinds)))
    (shown "the gate took the same digest of the thing as it arrived, and recorded it with its refusal:" (told result))))

(deftest layers-are-made
  (let [before (class-of-layer :alice)
        not-made (decide! (alice [{:e :note-1 :k :text}]))
        request-early (ask-for-locks! :alice :alice alice-own 4)
        by-stranger (decide! (assoc (making :alice :system :alice) :who :mallory))
        base-here (decide! (thing :base :system [:system :base :base]
                                  [{:e :base :k :class :v :microbatch}
                                   {:e :system :k :permission :v [:system :base :base]}]
                                  {:act-id [:base :streaming (UUID/randomUUID)] :class :streaming}))
        personal (decide! (making :alice :system :alice))
        group (decide! (making :team :alice :alice))
        own-uuid (UUID/randomUUID)
        own-layer (UUID/randomUUID)
        by-uuid (decide! (making own-layer own-uuid own-uuid))
        again-by-system (decide! (making :alice :system :alice))
        again-by-holder (decide! (making :team :alice :alice))
        system-again (decide! (thing :alice :system alice-own [{:e :note-1 :k :text}]))]
    (check "before its making, the class of a layer is nothing" nil before)
    (check "an act into a layer that is not made is refused" :layer-not-made (:word not-made))
    (check "a request for locks into a layer that is not made is refused, and makes no lock"
           [:layer-not-made {}] [(:word request-early) (:locks request-early)])
    (check "a making act by a who that is neither the holder nor the system is refused" :layer-not-made (:word by-stranger))
    (check "a making act of the base at the streaming gate is refused" :layer-not-made (:word base-here))
    (check "a person's layer made by the system for that person: accepted, and its class is streaming"
           [:accepted true :streaming] [(:word personal) (:mine? personal) (class-of-layer :alice)])
    (check "the record holds the class as a fact on the layer's own entity, and the person's permission on its holder"
           [:streaming alice-own]
           [(record/plain-value-of (fact-row :alice :alice :class)) (record/plain-value-of (fact-row :alice :alice :permission))])
    (check "the class, the permission and the answer hold one stamp"
           [(:stamp (:answer personal)) (:stamp (:answer personal))]
           [(:stamp (fact-row :alice :alice :class)) (:stamp (fact-row :alice :alice :permission))])
    (check "a group's layer made by the person who creates it: accepted, and its class is streaming"
           [:accepted :streaming] [(:word group) (class-of-layer :team)])
    (check "a layer whose id is a UUID, made by a person whose id is a UUID" [:accepted :streaming]
           [(:word by-uuid) (class-of-layer own-layer)])
    (check "a making act for a layer that is made, by the system under the person's permission, is refused"
           :who-not-the-permissions-holder (:word again-by-system))
    (check "a making act for a layer that is made, by its holder, is refused: its permission is not beneath its own"
           :permission-fact-not-beneath-the-acts-own (:word again-by-holder))
    (check "the system holds no permission in the personal layer that it made: its second act there is refused"
           :who-not-the-permissions-holder (:word system-again))
    (check "the first permission of the group's layer is the one that the first making act wrote"
           alice-in-team (record/plain-value-of (fact-row :team :alice :permission)))
    (shown "an act into a layer that is not made refused:" (told not-made))
    (shown "a person's layer made by the system for that person, with the class streaming:" (told personal)
           "| class-of-layer :alice =" (class-of-layer :alice))
    (shown "a group's layer made by the person who creates it:" (told group) "| class-of-layer :team =" (class-of-layer :team))
    (shown "a making act for a layer that is made is refused: by the system" (:word again-by-system)
           "| by its holder" (:word again-by-holder) "| a second act of the system in a personal layer" (:word system-again))))

(deftest an-act-lands
  (let [request (ask-for-locks! :alice :alice alice-own 16)
        in-hand (first (get @locks-in-hand :alice))
        fact (sealed :alice :note-1 :text "the first text")
        landed (decide! (alice [fact]))
        act-uuid (nth (:act-id (:thing landed)) 2)
        the-row (fact-row :alice :note-1 :text)
        the-lock (lock-row (:lock-id in-hand))
        listed (decide! (alice [] {:facts (list (sealed :alice :note-2 :text {:n 1}) (sealed :alice :note-3 :text #{:a}))}))
        list-uuid (nth (:act-id (:thing listed)) 2)
        never-sent (answer-of (new-act-id :alice))]
    (swap! kept assoc :landed landed :landed-lock in-hand)
    (check "a request for 16 locks is accepted" :accepted (:word request))
    (check "one query gives its 16 locks, each 32 bytes of way 1, no two the same"
           [16 #{32} #{1} 16]
           [(count (:locks request)) (set (map #(alength ^bytes (:bytes %)) (vals (:locks request))))
            (set (map :way (vals (:locks request)))) (count (set (map #(vec (:bytes %)) (vals (:locks request)))))])
    (check "the request's own fact is a row: its count, as a plain value on the uuid of its act"
           16 (record/plain-value-of (fact-row :alice (nth (:request request) 2) :lock-request)))
    (check "one act lands through the streaming gate and is read back by act-id: accepted, with the digest of what was sent"
           [:acked :accepted nil true] [(:ack landed) (:verdict (:answer landed)) (:reason (:answer landed)) (:mine? landed)])
    (check "the place holds the sealed bytes as sent, the fact that wrote it, its lock, and the stamp of the answer"
           [(vec (:sealed fact)) act-uuid 0 (nth (first (:lock-id fact)) 2) (second (:lock-id fact)) (:stamp (:answer landed))]
           [(vec (:value the-row)) (:by-act the-row) (:by-index the-row) (:lock-act the-row) (:lock-index the-row) (:stamp the-row)])
    (check "the lock's row has the bytes that the door read, and now names the fact that it seals"
           [(vec (:bytes (:lock in-hand))) 1 act-uuid 0]
           [(vec (:bytes the-lock)) (:way the-lock) (:seals-act the-lock) (:seals-index the-lock)])
    (check "the lock opens the place's bytes at its place: the value as it was sealed"
           (vec (plain-bytes "the first text"))
           (vec (seal/open (record/lock-of the-lock) [:alice :note-1 :text] (:value the-row))))
    (check "an act whose facts came as a list is accepted, and each fact has its index in the order sent"
           [:accepted [list-uuid 0] [list-uuid 1]]
           [(:word listed)
            ((juxt :by-act :by-index) (fact-row :alice :note-2 :text))
            ((juxt :by-act :by-index) (fact-row :alice :note-3 :text))])
    (check "an act-id that was never sent has no answer: nothing yet" nil never-sent)
    (shown "a request for 16 locks, read with one query:" (told request) "| locks" (count (:locks request)))
    (shown "one act landing through the streaming gate and read back by act-id:" (pr-str (:answer landed)))
    (shown "its place in the record: written by" (str (:by-act the-row)) "index" (:by-index the-row)
           "stamp" (:stamp the-row) "sealed bytes" (alength ^bytes (:value the-row))
           "| its lock sits beside it and names the fact:" (str (:seals-act the-lock)) (:seals-index the-lock))))

(deftest refusals-and-repeats
  (let [accepted-thing (:thing (:landed @kept))
        first-fact-id [(:act-id accepted-thing) 0]
        unsealed (decide! (alice [{:e :note-9 :k :text :v "a plain value"}]))
        answer-before (answer-of (:act-id accepted-thing))
        stamp-before (last-stamp :alice)
        repeat-ack (send! accepted-thing)
        answer-after (answer-of (:act-id accepted-thing))
        refused-again (do (send! (:thing unsealed)) (answer-of (:act-id (:thing unsealed))))
        stamp-after (last-stamp :alice)
        other-under-used (decide! (assoc (alice [{:e :note-8 :k :text}]) :act-id (:act-id accepted-thing)))
        junk (decide! {:act-id (new-act-id :alice) :layer :alice :junk 1})
        good-under-junk (decide! (assoc (alice [{:e :note-8 :k :text}]) :act-id (:act-id (:thing junk))))
        stale (decide! (alice [(assoc (sealed :alice :note-1 :text "stale") :replaces [(new-act-id :alice) 0])]))
        empty-place (decide! (alice [(assoc (sealed :alice :note-7 :text "x") :replaces first-fact-id)]))
        replaced (decide! (alice [(assoc (sealed :alice :note-1 :text "the second text") :replaces first-fact-id)]))
        second-fact-id [(:act-id (:thing replaced)) 0]
        stale-now (decide! (alice [(assoc (sealed :alice :note-1 :text "late") :replaces first-fact-id)]))
        repeat-of-replace (do (send! (:thing replaced)) (answer-of (:act-id (:thing replaced))))
        over (decide! (alice [(sealed :alice :note-1 :text "the third text")]))
        over-row (fact-row :alice :note-1 :text)
        retract (decide! (alice [{:e :note-1 :k :text}]))
        retract-row (fact-row :alice :note-1 :text)
        over-retract (decide! (alice [(assoc (sealed :alice :note-1 :text "after the retract")
                                             :replaces [(:act-id (:thing retract)) 0])]))
        retract-of-retract (decide! (alice [{:e :note-6 :k :text}]))
        retract-again (decide! (alice [{:e :note-6 :k :text}]))
        onto-retract (decide! (alice [(sealed :alice :note-6 :text "onto a retract, naming nothing")]))
        on-the-layer (decide! (alice [(sealed :alice :alice :title "a fact of the layer's own entity, under a plain key")]))
        twice (decide! (alice [{:e :note-10 :k :text} {:e :note-10 :k :text}]))
        own-base (alice [])
        own-fact (decide! (assoc own-base :facts [{:e :note-10 :k :text}
                                                  {:e :note-11 :k :text :replaces [(:act-id own-base) 0]}]))
        one-lock (take-lock! :alice)
        lock-twice (decide! (alice [(seal-with one-lock :alice :note-10 :text "a") (seal-with one-lock :alice :note-11 :text "b")]))
        lock-then-used (decide! (alice [(seal-with one-lock :alice :note-10 :text "a")]))]
    (check "an unsealed value is refused, and the refusal is recorded with its reason, a stamp and the digest"
           [:refused :not-sealed true true]
           [(:verdict (:answer unsealed)) (:reason (:answer unsealed)) (int? (:stamp (:answer unsealed))) (:mine? unsealed)])
    (check "a refusal writes nothing at the place" nil (fact-row :alice :note-9 :text))
    (check "a repeat is acked and answered from the record: the same answer, with the stamp of the first decision"
           [:acked answer-before] [repeat-ack answer-after])
    (check "a repeat of a refused act reads the recorded refusal" (:answer unsealed) refused-again)
    (check "not decided twice: the task's last stamp is the same after two repeats" stamp-before stamp-after)
    (check "a second, different act under a used act-id is told so: the answer it reads is not of its own digest"
           [answer-before false]
           [(:answer other-under-used) (:mine? other-under-used)])
    (check "and nothing of the second act is written" nil (fact-row :alice :note-8 :text))
    (check "a thing that is no act uses its act-id: a good act sent under it reads the first thing's refusal"
           [:unknown-part true :unknown-part false]
           [(:word junk) (:mine? junk) (:word good-under-junk) (:mine? good-under-junk)])
    (check "a stale replaces is refused: a fact that is not the current fact of the place" :replaced-fact-not-current (:word stale))
    (check "a fact that names something to replace on an empty place is stale" :replaced-fact-not-current (:word empty-place))
    (check "a fact that names the current fact of its place is accepted" :accepted (:word replaced))
    (check "a second act that names the fact that was replaced is stale" :replaced-fact-not-current (:word stale-now))
    (check "a repeat of the accepted replace is still accepted, although its replaces would now be stale"
           (:answer replaced) repeat-of-replace)
    (check "a fact that names nothing to replace is never stale: it becomes the current fact of a place that holds one"
           [:accepted (nth (:act-id (:thing over)) 2)] [(:word over) (:by-act over-row)])
    (check "a retract is recorded as the current fact of its place: a row with no value and no lock"
           [:accepted (nth (:act-id (:thing retract)) 2) false false]
           [(:word retract) (:by-act retract-row) (contains? retract-row :value) (contains? retract-row :lock-act)])
    (check "a later fact can replace the retract" :accepted (:word over-retract))
    (check "a retract on an empty place, and a retract of a retract, are accepted" [:accepted :accepted]
           [(:word retract-of-retract) (:word retract-again)])
    (check "a value that names nothing, on a place that holds a retract, is accepted" :accepted (:word onto-retract))
    (check "a value fact on the layer's own entity, under a plain key, is a fact like any other" :accepted (:word on-the-layer))
    (check "an act names a place once: two facts on one place, and a fact that replaces a fact of its own act, are refused"
           [:place-named-twice :replaces-own-fact nil] [(:word twice) (:word own-fact) (fact-row :alice :note-11 :text)])
    (check "two facts of one act under one lock are refused, and the lock is free for a later act"
           [:lock-named-twice :accepted] [(:word lock-twice) (:word lock-then-used)])
    (shown "a refusal recorded with its reason:" (pr-str (:answer unsealed)))
    (shown "an unsealed value refused:" (told unsealed))
    (shown "a repeat (same act-id sent twice) answered from the record, not decided twice: stamp before" (:stamp answer-before)
           "after" (:stamp answer-after) "| the task's last stamp before" stamp-before "after" stamp-after)
    (shown "a second, different act under a used act-id told so: the digest of the answer" (subs (:digest (:answer other-under-used)) 0 16)
           "is not the digest of what was sent" (subs (digest/hex-of (:thing other-under-used)) 0 16))
    (shown "a stale replaces refused:" (told stale) "| the current fact named:" (told replaced) "| the replaced fact named again:" (told stale-now))
    (shown "a place named twice:" (:word twice) "| a fact that replaces a fact of its own act:" (:word own-fact)
           "| two facts under one lock:" (:word lock-twice))
    (shown "a fact that names nothing is never stale:" (told over) "| a retract is the current fact of its place:" (told retract)
           "| a later fact replaces the retract:" (told over-retract) "(second fact id" (short-id (first second-fact-id)) ")")))

(deftest settings-and-permissions
  (let [setting (decide! (alice [{:e :team :k :class :v :streaming}]))
        other-layer (decide! (thing :alice :alice alice-in-team [{:e :note-5 :k :text}]))
        not-yet (decide! (thing :alice :bob bobs [{:e :note-5 :k :text}]))
        give (decide! (alice [{:e :bob :k :permission :v bobs}]))
        not-yet-again (do (send! (:thing not-yet)) (answer-of (:act-id (:thing not-yet))))
        bob-asks (ask-for-locks! :alice :bob bobs 2)
        bob-writes (decide! (thing :alice :bob bobs [(sealed :alice :bobs-note :text "bob's text")]))
        beside (decide! (thing :alice :bob bobs [{:e :carol :k :permission :v [:carol :alice :alice alice-own]}]))
        by-no-holder (decide! (thing :alice :carol bobs [{:e :dave :k :permission :v [:dave :alice :alice bobs]}]))
        carol-not-given (decide! (thing :alice :carol [:carol :alice :alice alice-own] [{:e :note-5 :k :text}]))
        give-carol (decide! (thing :alice :bob bobs [{:e :carol :k :permission :v carols}]))
        give-dave (decide! (thing :alice :carol carols [{:e :dave :k :permission :v daves}]))
        dave-acts (decide! (thing :alice :dave daves [{:e :daves-note :k :text}]))
        five-deep (decide! (thing :alice :dave daves [{:e :erin :k :permission :v [:erin :alice :alice daves]}]))
        granted-elsewhere (decide! (alice [{:e :erin :k :permission :v [:erin :alice :team alice-own]}]))
        for-elsewhere (decide! (alice [{:e :erin :k :permission :v [:erin :team :alice alice-own]}]))
        at-the-top (decide! (alice [{:e :erin :k :permission :v [:erin :alice :alice]}]))
        give-again (decide! (alice [{:e :bob :k :permission :v bobs}]))
        end-it (decide! (alice [{:e :bob :k :permission}]))
        class-again (decide! (alice [{:e :alice :k :class :v :streaming}]))
        class-other (decide! (alice [{:e :alice :k :class :v :microbatch}]))
        class-retract (decide! (alice [{:e :alice :k :class}]))
        bob-still (decide! (thing :alice :bob bobs [{:e :bobs-note-2 :k :text}]))
        own-give (decide! (thing :alice :frank [:frank :alice :alice alice-own]
                                 [{:e :frank :k :permission :v [:frank :alice :alice alice-own]}]))
        with-stale (decide! (alice [{:e :erin :k :permission :v [:erin :alice :alice alice-own]}
                                    (assoc (sealed :alice :note-1 :text "stale") :replaces [(new-act-id :alice) 0])]))
        erin-acts (decide! (thing :alice :erin [:erin :alice :alice alice-own] [{:e :note-5 :k :text}]))]
    (check "a setting key on another layer is refused, and that layer's class is as it was"
           [:setting-not-on-the-layer :streaming] [(:word setting) (class-of-layer :team)])
    (check "an act with a permission for another layer is refused by the form" :permission-not-in-layer (:word other-layer))
    (check "an act under a permission that does not stand is refused" :permission-does-not-stand (:word not-yet))
    (check "a permission given beneath another by its holder is accepted, and sits on its holder as a fact"
           [:accepted bobs] [(:word give) (record/plain-value-of (fact-row :alice :bob :permission))])
    (check "a repeat of the refused act reads its recorded refusal, although the act would now pass"
           (:answer not-yet) not-yet-again)
    (check "an act under it is accepted: a request for locks, and a value sealed under one of them"
           [:accepted :accepted] [(:word bob-asks) (:word bob-writes)])
    (check "a permission given by one who does not hold the one above is refused: it is not beneath the giver's own"
           :permission-fact-not-beneath-the-acts-own (:word beside))
    (check "an act under a permission by a who that is not its holder is refused" :who-not-the-permissions-holder (:word by-no-holder))
    (check "the refused give gave nothing: an act under that permission is refused" :permission-does-not-stand (:word carol-not-given))
    (check "a chain of four: each holder gives one beneath its own, and the last acts under it"
           [:accepted :accepted :accepted] [(:word give-carol) (:word give-dave) (:word dave-acts)])
    (check "a permission five deep is no permission: the holder of one four deep cannot give"
           :permission-fact-not-a-permission (:word five-deep))
    (check "a granted-in that is not the act's layer is refused" :permission-fact-granted-in-another-layer (:word granted-elsewhere))
    (check "a permission fact for another layer is refused" :permission-fact-for-another-layer (:word for-elsewhere))
    (check "a permission with nothing above it, in a layer that is made, is refused" :permission-fact-not-beneath-the-acts-own (:word at-the-top))
    (check "a permission that stands is not given again: a store key takes one write for a place" :store-key-takes-one-write (:word give-again))
    (check "a retract of a permission is refused with the word that says it is not decided" :ending-a-permission-not-decided (:word end-it))
    (check "a class fact after the making: the same class, the other class, a retract"
           [:store-key-takes-one-write :changing-a-class-not-decided :store-key-retract-not-built]
           [(:word class-again) (:word class-other) (:word class-retract)])
    (check "after each of those refusals the permission stands and the class is as it was"
           [:accepted :streaming] [(:word bob-still) (class-of-layer :alice)])
    (check "an act that gives a permission and names that permission as its own is refused: it does not stand before the act"
           :permission-does-not-stand (:word own-give))
    (check "whole or nothing: an act that gives a permission and carries a stale fact is refused, and the permission is not given"
           [:replaced-fact-not-current :permission-does-not-stand nil]
           [(:word with-stale) (:word erin-acts) (fact-row :alice :erin :permission)])
    (shown "a setting key on another layer refused:" (told setting))
    (shown "an act with a permission for another layer refused:" (told other-layer))
    (shown "an act under a permission that does not stand refused:" (told not-yet))
    (shown "a permission given beneath another by its holder:" (told give) "| and an act under it accepted:" (told bob-writes))
    (shown "a permission given by one who does not hold the one above refused:" (told beside)
           "| by a who that is not the holder of the permission it names:" (told by-no-holder))
    (shown "a chain four deep stands, and a fifth is refused:" (:word give-carol) (:word give-dave) (:word dave-acts) (:word five-deep))
    (shown "granted-in must be the act's layer:" (:word granted-elsewhere) "| a store key takes one write:" (:word give-again)
           "| ending a permission:" (:word end-it) "| a class after the making:" (:word class-again) (:word class-other) (:word class-retract))
    (shown "whole or nothing: a give with a stale fact" (:word with-stale) "| an act under the permission that was not given" (:word erin-acts))))

(deftest the-path-of-a-lock
  (let [used-lock (:landed-lock @kept)
        second-value (decide! (alice [(seal-with used-lock :alice :note-20 :text "a second value")]))
        free (take-lock! :alice)
        refused-first (decide! (alice [(assoc (seal-with free :alice :note-21 :text "v") :replaces [(new-act-id :alice) 0])]))
        free-after (lock-row (:lock-id free))
        copied-bytes (seal-with free :alice :note-21 :text "v")
        copied (decide! (alice [(assoc copied-bytes :e :note-22)]))
        any-bytes (decide! (alice [(assoc copied-bytes :sealed (byte-array 40))]))
        then-used (decide! (alice [copied-bytes]))
        unknown-lock (decide! (alice [(seal-with {:lock-id [(new-act-id :alice) 0] :lock (:lock free)} :alice :note-23 :text "v")]))
        team-request (ask-for-locks! :team :alice alice-in-team 2)
        team-lock (take-lock! :team)
        in-other-layer (decide! (alice [(seal-with team-lock :alice :note-24 :text "v")]))
        renamed (decide! (alice [(seal-with (assoc team-lock :lock-id [[:alice :streaming (nth (first (:lock-id team-lock)) 2)] 0])
                                            :alice :note-24 :text "v")]))
        team-uses (decide! (thing :team :alice alice-in-team [(seal-with team-lock :team :plan :text "the team's text")]))
        by-stranger (ask-for-locks! :alice :mallory alice-own 3)
        too-many (ask-for-locks! :alice :alice alice-own 257)
        request (ask-for-locks! :alice :alice alice-own 16)
        bytes-before (into {} (map (fn [[i lock]] [i (vec (:bytes lock))])) (:locks request))
        repeat-ack (send! (:thing request))
        bytes-after (into {} (map (fn [[i lock]] [i (vec (:bytes lock))])) (locks-of (:request request) 0 255))
        bobs-lock (take-lock! :alice)
        by-another-who (decide! (thing :alice :bob bobs [(seal-with bobs-lock :alice :bobs-note-3 :text "sealed under a lock that Alice asked for")]))
        both-base (alice [])
        both (decide! (assoc both-base :facts [{:e (nth (:act-id both-base) 2) :k :lock-request :v 2}
                                               (sealed :alice :note-25 :text "with a request")]))]
    (check "a second value under a lock that already seals one is refused" :lock-already-seals-a-value (:word second-value))
    (check "a lock that a refused act named is free: its row names no fact"
           [:replaced-fact-not-current false] [(:word refused-first) (record/seals-a-fact? free-after)])
    (check "sealed bytes copied to another place do not open" :lock-does-not-open-the-value (:word copied))
    (check "any bytes under a real lock id do not open" :lock-does-not-open-the-value (:word any-bytes))
    (check "after those three refusals the lock seals a value of a later act" :accepted (:word then-used))
    (check "a sealed value under a lock id that the store did not make is refused" :lock-not-made-for-this-layer (:word unknown-lock))
    (check "a sealed value under a lock that the store made for another layer is refused, under its own id and under this layer's"
           [:accepted :lock-not-made-for-this-layer :lock-not-made-for-this-layer]
           [(:word team-request) (:word in-other-layer) (:word renamed)])
    (check "that lock is still free in its own layer" :accepted (:word team-uses))
    (check "a refused request makes no lock: by a who that is not the holder, and for 257 locks"
           [:who-not-the-permissions-holder {} :too-many-locks-requested {}]
           [(:word by-stranger) (:locks by-stranger) (:word too-many) (:locks too-many)])
    (check "a request sent twice makes no lock again: each lock has the bytes that the first run made"
           [:acked bytes-before] [repeat-ack bytes-after])
    (check "a read of a part of a request's locks: one index, the last two, a range cut to 0 to 255, an empty range"
           [[10] [14 15] 16 {}]
           [(vec (keys (locks-of (:request request) 10 10))) (vec (sort (keys (locks-of (:request request) 14 99))))
            (count (locks-of (:request request) -5 999)) (locks-of (:request request) 9 3)])
    (check "a lock in each state gives the same bytes: one that seals a value reads as the door first read it"
           (vec (:bytes (:lock free))) (vec (:bytes (get (locks-of (first (:lock-id free)) 0 255) (second (:lock-id free))))))
    (check "a lock is bound to its layer, not to who asked for it: another holder of the layer seals under it" :accepted (:word by-another-who))
    (check "one act asks for locks and carries a sealed value: accepted, and its two locks are made"
           [:accepted 2] [(:word both) (count (locks-of (:act-id (:thing both)) 0 255))])
    (shown "the path of a lock: asked for and read (" (count (:locks request)) "locks ), used, and then a second value under it refused:" (told second-value))
    (shown "the lock of a refused act is still free:" (told refused-first) "then" (told then-used))
    (shown "sealed bytes copied to another place do not open:" (told copied) "| any bytes under a real lock:" (:word any-bytes))
    (shown "a lock that the store did not make for this layer:" (:word unknown-lock) "| a lock of another layer:" (:word in-other-layer)
           "| a refused request makes no lock:" (:word by-stranger) (pr-str (:locks by-stranger)))
    (shown "a request sent twice gives the same" (count bytes-after) "locks, byte for byte")))

(deftest strays-and-limits
  (let [counts (fn [] (into {} (map (fn [w] [w (strays w)])) declaration/stray-reasons))
        before (counts)
        long (keyword (apply str (repeat 300 "a")))
        no-act-id 5
        malformed {:act-id [:alice :streaming] :layer :alice :marker (UUID/randomUUID)}
        other-gate (assoc (alice [{:e :note-30 :k :text}]) :act-id [:alice :microbatch (UUID/randomUUID)] :class :microbatch)
        other-layer (assoc (alice [{:e :note-30 :k :text}]) :layer :team)
        too-long {:act-id [long :streaming (UUID/randomUUID)] :layer long}
        acks (mapv send! [no-act-id malformed other-gate other-layer too-long other-gate other-gate])
        after (counts)
        second-of-a-word (do (send! (assoc malformed :marker (UUID/randomUUID))) (counts))
        many (fn [n] (alice (map (fn [i] {:e (keyword (str "many-" i)) :k :text}) (range n))))
        facts-256 (decide! (many 256))
        facts-257 (decide! (many 257))
        based-on (fn [n] (into {} (map (fn [i] [[[:alice :streaming (UUID. 0 1)] i] 1])) (range n)))
        based-1024 (decide! (alice [{:e :note-31 :k :text}] {:based-on (based-on 1024)}))
        based-1025 (decide! (alice [{:e :note-31 :k :text}] {:based-on (based-on 1025)}))
        big (take-lock! :alice)
        sealed-over (decide! (alice [(assoc (seal-with big :alice :note-32 :text "v") :sealed (byte-array 65537))]))
        sealed-at (decide! (alice [{:e :note-32 :k :text :lock-id (:lock-id big)
                                    :sealed (seal/seal (:lock big) [:alice :note-32 :text] (byte-array (- 65536 28)))}]))
        long-entity (decide! (alice [{:e long :k :text}]))
        now (System/currentTimeMillis)
        fact-id [[:alice :streaming (UUID. 0 1)] 0]
        far-ahead (decide! (alice [{:e :note-33 :k :text}] {:based-on {fact-id (stamp/pack (+ now 60000) 0)}}))
        claim (decide! (alice [{:e :note-33 :k :text}] {:claimed-when Long/MAX_VALUE}))
        carried (stamp/pack (+ (System/currentTimeMillis) 5000) 7)
        near-ahead (decide! (alice [{:e :note-34 :k :text}] {:based-on {fact-id carried} :because-of (new-act-id :alice)}))
        next-one (decide! (alice [{:e :note-35 :k :text}]))]
    (check "each stray was acked" (vec (repeat 7 :acked)) acks)
    (check "a stray is kept under its word, once however often it comes: five different strays, one of them sent three times"
           {:no-act-id 1 :act-id-malformed 1 :id-too-long 1 :act-id-of-the-other-gate 1 :act-id-of-another-layer 1}
           (merge-with - after before))
    (check "what is kept is the digest of the stray thing"
           [true true] [(contains? (stray-digests :act-id-of-the-other-gate) (digest/hex-of other-gate))
                        (contains? (stray-digests :no-act-id) (digest/hex-of no-act-id))])
    (check "the count of strays under a word is the number of different ones: a second, different one makes it two more than before"
           2 (- (:act-id-malformed second-of-a-word) (:act-id-malformed before)))
    (check "a stray gets no answer: an act-id of the other gate, and an act-id whose layer is not the thing's layer"
           [nil nil] [(answer-of (:act-id other-gate)) (answer-of (:act-id other-layer))])
    (check "a stray writes no fact" nil (fact-row :alice :note-30 :text))
    (check "an act of 256 facts is accepted, and each fact is a row; one of 257 is refused"
           [:accepted 255 :too-many-facts] [(:word facts-256) (:by-index (fact-row :alice :many-255 :text)) (:word facts-257)])
    (check "1,024 entries of based-on are taken; 1,025 are refused" [:accepted :too-many-based-on] [(:word based-1024) (:word based-1025)])
    (check "a sealed value of 65,537 bytes is refused; one of 65,536 is accepted, under the same lock"
           [:sealed-value-too-large :accepted 65536] [(:word sealed-over) (:word sealed-at) (alength ^bytes (:value (fact-row :alice :note-32 :text)))])
    (check "an id that prints to more than 256 characters is refused, and the refusal is recorded under the act-id" :id-too-long (:word long-entity))
    (check "an act that carries a stamp more than 10 seconds ahead of the gate's clock is refused"
           :carried-stamp-too-far-ahead (:word far-ahead))
    (check "its answer holds an ordinary stamp, not one that follows the carried stamp"
           true (< (stamp/time-of (:stamp (:answer far-ahead))) (+ now 30000)))
    (check "claimed-when far ahead is a claim: the act is accepted, and its stamp does not follow it"
           [:accepted true] [(:word claim) (< (stamp/time-of (:stamp (:answer claim))) (+ now 30000))])
    (check "an act that carries a stamp 5 seconds ahead, based on a fact and because of an act that the record does not hold, is accepted, with a stamp later than the one it carries"
           [:accepted true] [(:word near-ahead) (> (:stamp (:answer near-ahead)) carried)])
    (check "and the next stamp of the task is later still" true (> (:stamp (:answer next-one)) (:stamp (:answer near-ahead))))
    (shown "a stray is kept under its word and gets no answer; strays seen, by word:" (pr-str (merge-with - after before))
           "| the same stray sent three times counts once | answer of a stray:" (pr-str (answer-of (:act-id other-gate))))
    (shown "a thing over each limit is refused:" (:word facts-257) (:word based-1025) (:word sealed-over) (:word long-entity)
           "| at each limit accepted:" (:word facts-256) (:word based-1024) (:word sealed-at))
    (shown "a carried stamp too far ahead:" (told far-ahead) "| 5 seconds ahead:" (told near-ahead) "carried" carried)))

(deftest the-reads-take-anything
  (let [u (UUID/randomUUID)
        long (keyword (apply str (repeat 300 "a")))
        accepted (:act-id (:thing (:landed @kept)))
        ask (fn [query & args] (apply rama/foreign-invoke-query (get *hand* query) args))]
    (check "the hand gives nothing itself for what is no id or no act-id, and calls no query"
           [nil nil nil nil {} {}]
           [(class-of-layer nil) (class-of-layer "alice") (answer-of nil) (answer-of [:alice :streaming]) (locks-of nil 0 1)
            (locks-of accepted "0" 1)])
    (check "the read by act-id, called with what is no act-id of the layer given, gives nothing"
           [nil nil nil nil nil true]
           [(ask :answer :alice [:team :streaming u]) (ask :answer :alice 5) (ask :answer :alice [:alice :streaming])
            (ask :answer "alice" ["alice" :streaming u]) (ask :answer long [long :streaming u])
            (some? (ask :answer :alice accepted))])
    (check "an act-id of the class microbatch has no answer in this build" nil (ask :answer :alice [:alice :microbatch (nth accepted 2)]))
    (check "the read of a class, called with what is no id, or with a layer that is not made, gives nothing"
           [nil nil nil] [(ask :class "alice") (ask :class long) (ask :class :nobody)])
    (check "the read of locks, called with an act-id that made no lock, indexes that are no integers, or the other class, gives an empty map"
           [{} {} {} {}]
           [(ask :locks :alice accepted 0 255) (ask :locks :alice accepted "0" 3) (ask :locks :alice [:alice :microbatch u] 0 3)
            (ask :locks :alice [:team :streaming u] 0 3)])
    (check "the same uuid in two layers is two answers: an answer is found by its layer and its uuid"
           [nil nil] [(ask :answer :team [:team :streaming (nth accepted 2)]) (answer-of [:team :streaming (nth accepted 2)])])
    (shown "the three reads give nothing for what is no id, no act-id, another layer's act-id or the other class, and do not fail")))

(deftest after-the-module-is-started-again
  (let [landed (:landed @kept)
        accepted (:act-id (:thing landed))
        request (ask-for-locks! :alice :alice alice-own 2)
        bytes-before (into {} (map (fn [[i lock]] [i (vec (:bytes lock))])) (:locks request))
        stamp-before (last-stamp :alice)
        strays-before (strays :act-id-malformed)
        _ (rtest/update-module! (:ipc *hand*) module/Store)
        answer-after (answer-of accepted)
        repeat-ack (send! (:thing landed))
        stamp-after-repeat (last-stamp :alice)
        bytes-after (into {} (map (fn [[i lock]] [i (vec (:bytes lock))])) (locks-of (:request request) 0 255))
        with-old-lock (decide! (alice [(sealed :alice :note-40 :text "sealed under a lock from before")]))
        by-bob (decide! (thing :alice :bob bobs [{:e :bobs-note-4 :k :text}]))]
    (check "the answer that was read before is the answer after" (:answer landed) answer-after)
    (check "a repeat after it is answered from the record: the task's last stamp is the one from before"
           [:acked stamp-before] [repeat-ack stamp-after-repeat])
    (check "the class, the strays and the locks are as they were: each lock has the same bytes"
           [:streaming strays-before bytes-before] [(class-of-layer :alice) (strays :act-id-malformed) bytes-after])
    (check "a lock that was asked for before seals a value after" :accepted (:word with-old-lock))
    (check "the first stamp after is later than the last stamp before" true (> (:stamp (:answer with-old-lock)) stamp-before))
    (check "a permission that was given before stands after" :accepted (:word by-bob))
    (shown "after the module is started again in the cluster, the record is as it was: the same answer, the same locks;"
           "the last stamp before" stamp-before "the first stamp after" (:stamp (:answer with-old-lock)))))

(deftest the-stamps-and-the-depot
  (let [records (vec (stored))
        partition-of (into {} (keep (fn [[p r]] (when (and (map? r) (contains? r :layer)) [(:layer r) p]))) records)
        made [:alice :team :carrier]
        by-partition (group-by #(get partition-of (:layer %)) @first-answers)
        lock-texts (into #{} (comp (filter #(and (map? %) (vector? (:facts %))))
                                   (map :act-id)
                                   (mapcat (fn [act-id] (vals (locks-of act-id 0 255))))
                                   (map #(text/hex (:bytes %))))
                         @things-sent)
        depot-text (apply str (map (fn [[_ r]] (text/text r)) records))]
    (check "the stamps of the answers increase strictly on each task, in the order in which the acts were sent"
           true (every? (fn [[_ answers]] (apply < (map :stamp answers))) by-partition))
    (check "each stamp is below 2^62 and holds a clock time of today"
           true (every? #(and (< (:stamp %) act/max-carried-stamp)
                              (< (- (System/currentTimeMillis) 3600000) (stamp/time-of (:stamp %)) (+ (System/currentTimeMillis) 60000)))
                        @first-answers))
    (check "the depot holds each thing that was sent: accepted, refused, repeated and stray" (count @things-sent) (count records))
    (check "the layers of the proof are on more than one task" true (< 1 (count (set (vals partition-of)))))
    (check "no lock's bytes are on the depot: the hex of each lock that the door read is in no stored record"
           [true false] [(< 30 (count lock-texts)) (boolean (some #(str/includes? depot-text %) lock-texts))])
    (shown "the stamps of" (count @first-answers) "answers increase strictly on each task; answers by partition:"
           (pr-str (into (sorted-map) (map (fn [[p answers]] [p (count answers)])) by-partition)))
    (shown "layers by depot partition:" (pr-str (select-keys partition-of made)) "| the depot holds all" (count records)
           "things sent | none of" (count lock-texts) "locks' bytes is on the depot")))

(defn test-ns-hook
  "Runs the checks in the order in which the proof tells them. Later checks
  build on what earlier ones made."
  []
  (what-rama-carries)
  (layers-are-made)
  (an-act-lands)
  (refusals-and-repeats)
  (settings-and-permissions)
  (the-path-of-a-lock)
  (strays-and-limits)
  (the-reads-take-anything)
  (after-the-module-is-started-again)
  (the-stamps-and-the-depot))

(defn -main
  "Starts the cluster, launches the module, runs the proof, closes the
  cluster and exits: status 0 when each check holds. It takes no argument."
  [& _]
  (let [result (with-open [ipc (rtest/create-ipc)]
                 (rtest/launch-module! ipc module/Store {:tasks tasks :threads 2 :workers 2})
                 (binding [*hand* (open-hand ipc)]
                   (run-tests 'softland.store.module-proof)))
        {:keys [fail error]} result]
    (println)
    (println "THE PROOF, IN ONE PLACE")
    (run! println @shown-lines)
    (println (select-keys result [:test :pass :fail :error]))
    (shutdown-agents)
    (System/exit (if (zero? (+ fail error)) 0 1))))
