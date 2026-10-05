(ns softland.store.door
  "The door: the part of the runtime that puts each act in its envelope and
  appends the act to an acts depot. There is one door, with one entry for
  stream layers and for microbatch layers. No caller names a depot, a gate
  or a class.

  The calls:

    connect        the cluster and the module's name -> a door
    make           the parts from a caller -> the act as said    (steps 1 to 4)
    send           an act -> an answer, or a throw               (steps 5 to 10)
    make-and-send  the two together
    answer         a name -> the answer recorded under it, or :no-answer

  The steps, in order:

    1. Take the parts from the caller: who, session, permission, layer,
       facts, stood-on, because-of, subjects; and a name and a claimed-when
       if the caller has them. The door adds no fact and changes no fact.
       It only seals values.
    2. Read the class of the layer, one time for each act.
    3. Make the name, if the caller gave none.
    4. Put on the envelope: version, class, claimed-when, empty defaults.
       The result is the act as said. Nothing changes it after this, so a
       resend is never an act that was built again.
    5. Examine the act, before anything is leased, sealed or appended. An
       act that fails comes back as a refusal with no stamp, and nothing
       goes to a depot.
    6. Seal each value one time, and keep the sealed form until the act
       has its answer. Each sending is that form, byte for byte.
    7. Append the act to the acts depot of the class in its name.
    8. Wait for the answer to this sending.
    9. If there is no answer, send the same sealed form again under the
       same name. After the bound, throw \"no answer\" with the name.
   10. Give the answer: :name, :answer (:yes or :no), :reason and :stamp,
       and :batch when the microbatch gate gave one.

  A refusal with no stamp was not recorded under the name, whoever gave it.

  The door remembers only its work in progress: the leased locks that it
  did not use yet, and the sealed form of each act that has no answer yet.
  The store holds the state. A door can stop at any moment and the store
  loses nothing.

  Words. \"Key\" is the key of a fact. \"Lock\" is an encryption key. On the
  wire three of the rig's words stay until the gates and the depots are
  taken: the scheme word :offer in a name, the depot names *offers and
  *micro-offers, and the class words :by-layer and :by-entity.

  The act's code is not here. It is the rig's envelope, required below as
  `act`: one copy, which the gates and the door both use. Until the module
  moves out of the rig, the door needs these four namespaces of the rig."
  (:refer-clojure :exclude [send])
  (:require [com.rpl.rama :as r]
            [com.rpl.rama.path :as p]
            [rig.store.envelope :as act]   ; the act: its parts, names, parse and sealed view
            [rig.store.locks :as locks]    ; the seal, and the lock ids of a lease
            [rig.store.gate :as gate]      ; the check for a misplaced setting
            [rig.store.micro :as micro]))  ; the microbatch gate's parse, digest and fingerprint

(def lease-size
  "How many locks the door leases at one time for a stream layer when its
  pool holds too few. One lease gives 256 at most, and an act that needs
  more than 64 gets up to that many from one lease."
  64)

(def stream-tries
  "How many times the door sends one act to the stream gate before it
  throws \"no answer\"."
  60)

(def microbatch-wait-ms
  "How long the door waits for the microbatch gate's answer to one act
  before it throws \"no answer\"."
  60000)

;; ------------------------------------------------------------ the door

(defn- fresh-memory
  "What a door remembers, and nothing more: for each [layer session] the
  leased locks that it did not use (`:pool`) and the leases whose locks it
  took into that pool (`:known`, so that no lock goes in two times); the
  grain of each layer at its last lease (`:grain`); and the sealed form of
  each act that has no answer yet (`:sealed`)."
  []
  {:pool {} :known {} :grain {} :sealed {}})

(defn connect
  "A door on the store: the handles that it uses, taken one time, and an
  empty memory. `module-name` is the module's name as a string, because a
  client knows a module by its name, so the door does not change when the
  module moves. A second `connect` is a door that started again: it holds
  no lock and no sealed form, and it takes its unused locks again by
  query."
  [cluster module-name]
  {:stream-acts       (r/foreign-depot cluster module-name "*offers")
   :microbatch-acts   (r/foreign-depot cluster module-name "*micro-offers")
   :stream-record     (r/foreign-pstate cluster module-name "$$layers")
   :microbatch-record (r/foreign-pstate cluster module-name "$$micro")
   :stream-leases     (r/foreign-query cluster module-name "lease-locks")
   :microbatch-lease  (r/foreign-query cluster module-name "micro-lease")
   :microbatch-answer (r/foreign-query cluster module-name "micro-lookup")
   :memory            (atom (fresh-memory))
   ;; one thread leases at a time; the others wait and take from the filled pool
   :lease-mutex       (Object.)})

;; ------------------------------------------- the settings of a layer

(defn- retrying
  "A read, done again while a worker starts again after a failure: a read
  that throws is \"not yet\". After `timeout-ms` the last error is thrown."
  ([f] (retrying f 60000))
  ([f timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop []
       (let [got (try [:ok (f)] (catch Exception e [:error e]))]
         (cond (= :ok (first got)) (second got)
               (> (System/currentTimeMillis) deadline) (throw (second got))
               :else (do (Thread/sleep 100) (recur))))))))

(defn- stream-settings
  "The settings of a layer in the stream record, or nil."
  [door layer]
  (r/foreign-select-one [(p/keypath layer :settings)] (:stream-record door)))

(defn- settings-versions
  "Each version of a layer's settings that the microbatch record keeps, by
  batch. A layer that the microbatch gate made has one or more."
  [door layer]
  (retrying #(into (sorted-map) (r/foreign-select [(p/keypath layer :settings) p/ALL] (:microbatch-record door)))))

(defn- settings-of
  "The settings of a layer in force: the last version in the microbatch
  record, else the settings in the stream record."
  [door layer]
  (retrying #(or (last (r/foreign-select [(p/keypath layer :settings) (p/sorted-map-range-to-end 1) p/MAP-VALS]
                                         (:microbatch-record door)))
                 (stream-settings door layer))))

(defn- class-of-layer
  "Step 2. The class of a layer, for one act: the class fact of the layer,
  from the stream record, else from the microbatch record. The act that
  makes a layer finds no class fact yet. The class is then that act's own
  class fact about the layer, as a gate reads it, and :by-layer if the act
  has none.

  This is the one function that knows where the class fact is. The door
  reads the class for each act and keeps it for no other act, because the
  name holds the class and a change of class can come at any time. The
  class is never worked out from the kind of the layer or from the number
  of its owners."
  [door layer facts]
  (or (when (act/readable-keyword? layer)
        (or (:class (stream-settings door layer))
            (:class (settings-of door layer))))
      (when (sequential? facts)
        (some #(when (and (map? %) (= layer (:e %)) (= :class (:k %))) (:v %)) facts))
      :by-layer))

;; ------------------------------------------------- make: steps 1 to 4

(defn- envelope
  "Steps 3 and 4, with the class known: the name if the caller gave none,
  then the envelope. The version is the door's. The claimed-when is the
  caller's, or the door's clock now: a claim, not a stamp. The session is
  the caller's or nothing: the door makes up no session, because a session
  starts at a sign-in. The facts stay as the caller gave them."
  [{:keys [who layer permission session facts stood-on because-of subjects claimed-when] given-name :name} class]
  (let [a {:version      act/version
           :who          who
           :layer        layer
           :class        class
           :permission   permission
           :session      session
           :stood-on     (or stood-on {})
           :because-of   because-of
           :claimed-when (or claimed-when (System/currentTimeMillis))
           :subjects     (or subjects #{})
           :facts        (vec facts)}]
    (assoc a :name (or given-name (act/name-for a)))))

(defn make
  "Steps 1 to 4: the parts from a caller -> the act as said, with its name.
  `parts` holds :who, :layer, :permission, :session, :facts, :stood-on,
  :because-of and :subjects, and :name and :claimed-when if the caller has
  them. The caller gives no class: the door reads it.

  To make an act and to send it are two calls, so a series can have all
  its names before its first act goes. The map that comes back is what is
  sent and what is sent again; the values in it are still plain."
  [door parts]
  (envelope parts (class-of-layer door (:layer parts) (:facts parts))))

;; --------------------------------------------- the gate, and step 5

(defn- gate-for
  "Which gate decides an act, read off its name: the stream gate for a
  :by-layer name and the microbatch gate for a :by-entity name. A name
  with no class is on an act that the store places itself. It goes to the
  microbatch gate if the microbatch record holds settings for its layer,
  or if it forgets a value that the microbatch gate accepted; else to the
  stream gate. The name must be well formed."
  [door a]
  (let [[layer class] (act/name-tag (:name a))]
    (case class
      :by-layer :stream
      :by-entity :microbatch
      (let [facts (:facts a)
            target (when (sequential? facts)
                     (some #(when (and (map? %) (= :forget (:k %))) (get-in % [:v :target])) facts))]
        (cond
          (seq (settings-versions door layer)) :microbatch
          (and (vector? target) (vector? (nth target 0 nil)) (= :by-entity (nth (nth target 0) 1 nil))) :microbatch
          :else :stream)))))

(defn- refusal-of
  "Step 5. Why the door does not send this act, or nil. Three checks, and
  the first that fails gives the reason:

  - the gate's own parse, on the sealed view of the act (the act with each
    plain value replaced by a mark, as the gate will get it);
  - a setting key about an entity that is not the act's layer
    (:malformed-control, the gates' own word for it);
  - a value fact and no session (:no-session, the door's one own reason).

  Only the sender can keep a bad act out of a depot: a depot keeps what it
  gets, and a client can read it. With no gate (the name is not well
  formed) the stream gate's parse gives the reason."
  [a gate]
  (let [view (act/sealed-view a)
        parsed (if (= :microbatch gate) (micro/parse-micro view) (act/parse view))
        facts (:facts a)]
    (cond
      (contains? parsed :refuse) (:refuse parsed)
      (some #(gate/misplaced-setting? a %) facts) :malformed-control
      (and (nil? (:session a)) (some act/value-fact? facts)) :no-session)))

(defn- refused
  "A refusal that the door gives itself: a no with no stamp."
  [a reason]
  {:name (:name a) :answer :no :reason reason :stamp nil})

(defn- answer-from
  "Step 10. The answer in its one shape, from what a gate or its record
  gave: the name, :yes or :no, the reason, the stamp; and the batch when
  the microbatch gate gave one. Who, permission, session and subjects are
  parts of the act, and to read them is a read, not an answer."
  [nm given]
  (cond-> {:name nm :answer (:answer given) :reason (:reason given) :stamp (:stamp given)}
    (contains? given :batch) (assoc :batch (:batch given))))

;; ------------------------------------------------ the locks for step 6

(declare send)

(defn- value-indices
  "The positions of the facts that the door seals."
  [a]
  (into [] (keep-indexed (fn [i f] (when (act/value-fact? f) i))) (:facts a)))

(defn- lease-act
  "The lease act for act `a`: an act like each other, by the same writer,
  in the same layer and session, that asks the gate for `n` locks. It
  takes the class of the act that it serves, and the door does not read
  the class again for it: the lease must reach the gate that decides the
  act, or the act cannot find its lock."
  [a n permission]
  (envelope {:who (:who a) :layer (:layer a) :permission permission :session (:session a)
             :facts [{:e (:session a) :k :lease :v {:count n}}]}
            (:class a)))

(defn- lease!
  "Send the lease act for act `a` through the same entry as each other
  act, and give its answer. If the lease gets no answer, the throw that
  the caller sees carries the name of the act that the caller sent, with
  the lease's name beside it."
  [door a lease]
  (try
    (send door lease)
    (catch clojure.lang.ExceptionInfo e
      (if (= :no-answer (:door (ex-data e)))
        (throw (ex-info "no answer" {:door :no-answer :name (:name a) :lease (:name lease)} e))
        (throw e)))))

(defn- held-for
  "The locks that an act is sealed under: for each value fact the id of
  its lock, and for each id its lock. `picked` is [[lock-id lock] ...].
  With the grain :per-act each value fact cites the first; else each cites
  its own."
  [idx picked grain]
  {:ids (if (= :per-act grain)
          (zipmap idx (repeat (first (first picked))))
          (zipmap idx (map first picked)))
   :locks (into {} picked)})

;; A stream layer: a pool of leased locks for each layer and session.

(defn- refresh!
  "Take the session's unused locks into the pool, by query, and note the
  grain in force. Only the locks of leases that the door did not take
  before go in: the locks of a lease go into the pool one time, so a query
  result that is older than another thread's use of a lock cannot put the
  lock back. A door that started again knows no lease, and so takes each
  unused lock of the session. Gives the grain."
  [door layer session]
  (let [{:keys [grain] leased :locks} (r/foreign-invoke-query (:stream-leases door) layer session)
        k [layer session]
        memory (:memory door)]
    (locking memory
      (swap! memory (fn [m]
                      (let [known (get-in m [:known k] #{})
                            fresh (into {} (remove (fn [[id _]] (contains? known (locks/lease-name-of id)))) leased)]
                        (-> m
                            (assoc-in [:grain layer] grain)
                            (update-in [:known k] (fnil into #{}) (map (comp locks/lease-name-of first)) leased)
                            (update-in [:pool k] merge fresh))))))
    grain))

(defn- take!
  "Take `n` locks of [layer session] out of the pool, or nil when it holds
  fewer."
  [door layer session n]
  (let [memory (:memory door)
        k [layer session]]
    (locking memory
      (let [pool (get-in @memory [:pool k])]
        (when (<= n (count pool))
          (let [picked (vec (take n (sort-by (comp str first) pool)))]
            (swap! memory update-in [:pool k] #(apply dissoc % (map first picked)))
            picked))))))

(defn- assign!
  "The locks for a stream act's value facts: from the pool, with a lease
  act (and then a query for its locks) while the pool holds too few. One
  lease act asks for 64 locks, or for as many as the act still needs, up
  to 256. If the gate refuses the lease, this gives {:lease-refused
  answer} and the door has no lock for the act."
  [door a idx]
  (let [layer (:layer a)
        session (:session a)
        memory (:memory door)
        grain (or (get-in @memory [:grain layer]) (refresh! door layer session))
        n (if (= :per-act grain) 1 (count idx))]
    (loop [tries 0]
      (if-let [picked (take! door layer session n)]
        (held-for idx picked grain)
        (let [outcome (locking (:lease-mutex door)
                        ;; another thread can have filled the pool while this one waited
                        (let [have (count (get-in @memory [:pool [layer session]]))]
                          (if (<= n have)
                            :refilled
                            (let [leased (lease! door a (lease-act a (min locks/max-lease (max lease-size (- n have)))
                                                                   (:permission a)))]
                              (if (= :yes (:answer leased))
                                (do (refresh! door layer session) :leased)
                                leased)))))]
          (cond
            (map? outcome) {:lease-refused outcome}
            (< 100 tries) (throw (ex-info "the door could not lease enough locks" {:layer layer :n n}))
            :else (recur (inc tries))))))))

;; A microbatch layer: one lease act for each act, and its locks by query.

(defn- take-locks
  "The plain locks of a lease in a microbatch layer, by query, when its
  batch shows: {lock-id lock}. Empty when `timeout-ms` passes first."
  ([door lease-name] (take-locks door lease-name 30000))
  ([door lease-name timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop []
       (let [got (try (r/foreign-invoke-query (:microbatch-lease door) lease-name nil) (catch Exception _ nil))
             leased (:locks got)]
         (if (or (seq leased) (> (System/currentTimeMillis) deadline))
           (into {} (map (fn [[i lock]] [[(into [] lease-name) (long i)] lock])) leased)
           (do (Thread/sleep 50) (recur))))))))

(defn- lease-for-act!
  "The locks for a microbatch act's value facts: one lease act for this
  act, for one lock or for one for each value by the grain of the layer,
  then the locks by query. The lock ids are a function of the lease's
  name, so the door needs nothing from the lease's answer but its yes. If
  the gate refuses the lease, this gives {:lease-refused answer}."
  [door a idx]
  (let [grain (or (:grain (settings-of door (:layer a))) :per-value)
        n (if (= :per-act grain) 1 (count idx))
        lease (lease-act a n (when-not (contains? gate/exempt-actors (:who a)) (:permission a)))
        leased (lease! door a lease)]
    (if (= :yes (:answer leased))
      (let [taken (take-locks door (:name lease))]
        (held-for idx (mapv (fn [id] [id (get taken id)]) (locks/lease-ids (:name lease) n)) grain))
      {:lease-refused leased})))

;; --------------------------------------------------------- step 6

(defn- seal
  "The act with each value fact sealed under its lock and citing the lock's
  id. A control fact and a retract stay as they are. The door never seals
  under no lock: if it holds no lock for an id, it throws and builds
  nothing."
  [a idx {ids :ids held :locks}]
  (assoc a :facts
         (reduce (fn [fs i]
                   (let [f (nth fs i)
                         id (get ids i)
                         sealed (locks/seal (get held id) (locks/canonical-bytes (:v f)))]
                     (when (nil? sealed)
                       (throw (ex-info "the door holds no lock for this id, and never seals under no lock"
                                       {:door :no-lock :name (:name a) :lock-id id})))
                     (assoc fs i (-> f (dissoc :v) (assoc :sealed sealed :lock-id id)))))
                 (vec (:facts a))
                 idx)))

(defn- sealed-form
  "Step 6. The act as the door sends it. An act with no value fact goes as
  it is. Else the door takes locks for it, seals it one time, and keeps
  that sealed form until the act has its answer, so each sending is the
  same bytes and one act has one fingerprint in a door's life. No plain
  value and no lock goes into an acts depot.

  If the gate refuses the lease for the act, this gives {:lease-refused
  answer} and seals nothing."
  [door a gate]
  (let [idx (value-indices a)
        memory (:memory door)]
    (if (empty? idx)
      a
      (or (get-in @memory [:sealed a])
          (let [held (if (= :microbatch gate) (lease-for-act! door a idx) (assign! door a idx))]
            (if (contains? held :lease-refused)
              held
              (let [sealed (seal a idx held)]
                (swap! memory assoc-in [:sealed a] sealed)
                sealed)))))))

;; ---------------------------------------------------- steps 7 to 9

(defn- ask-stream-gate
  "Steps 7 to 9 for a stream act. Append the sealed form to the stream acts
  depot and wait for the gate's event: the ack brings the answer. After an
  error, or an ack with no answer in it, send the same sealed form again.
  The door takes nothing from a sending that failed, and it does not read
  the record to answer its own sending: on a resend the gate's own look at
  its record is the one check that sees other value content under one
  name. After `stream-tries` sendings, throw \"no answer\" with the name."
  [door sealed]
  (loop [n 1]
    (let [got (try (get (r/foreign-append! (:stream-acts door) sealed :ack) "gate")
                   (catch Exception e e))]
      (cond
        (map? got) got
        (>= n stream-tries) (throw (ex-info "no answer" {:door :no-answer :name (:name sealed)}
                                            (when (instance? Throwable got) got)))
        :else (do (Thread/sleep 250) (recur (inc n)))))))

(defn- own-answer?
  "Whether what the microbatch record gave by name answers this sending
  itself: a refusal on its face, or a note that the gate answered it from
  the record, under this sending's fingerprint; or the record that the
  gate decided for this fingerprint; or the name taken by other parts. A
  record that the gate decided for another sealed form is not yet this
  sending's answer: the digest does not see what a sealed value holds, so
  only the gate's own check of this sending can answer it."
  [got fingerprint]
  (boolean (and got (not= :no-answer (:answer got))
                (or (:face got) (:recorded got) (= fingerprint (:decided-fp got))
                    (and (= :name-taken (:reason got)) (not (contains? got :stamp)))))))

(defn- await-answer
  "Ask the microbatch record by name, with the digest and the fingerprint
  of this sending, until it gives this sending's own answer. Nil when
  `timeout-ms` passes first: an answer that is not this sending's own is
  no answer. A read that throws is \"not yet\"."
  [door sealed timeout-ms]
  (let [nm (:name sealed)
        ;; an act that the gate refuses on its face has no digest; a digest
        ;; that no record has keeps a record from answering for it
        digest (or (some-> (micro/parse-micro sealed) :ok micro/parts-digest) "unparsed")
        fingerprint (micro/envelope-fp sealed)
        deadline (+ (System/currentTimeMillis) timeout-ms)]
    (loop []
      (let [got (try (r/foreign-invoke-query (:microbatch-answer door) nm digest fingerprint nil)
                     (catch Exception _ nil))]
        (cond
          (own-answer? got fingerprint) got
          (> (System/currentTimeMillis) deadline) nil
          :else (do (Thread/sleep 50) (recur)))))))

(defn- ask-microbatch-gate
  "Steps 7 to 9 for a microbatch act. Append the sealed form to the
  microbatch acts depot and wait for the append only: a microbatch depot
  has no ack from the gate. Then ask by name until the batch shows. After
  an append that failed, wait a short time and send the same sealed form
  again. After `microbatch-wait-ms`, throw \"no answer\" with the name."
  [door sealed]
  (let [deadline (+ (System/currentTimeMillis) microbatch-wait-ms)]
    (loop []
      (let [appended? (try (r/foreign-append! (:microbatch-acts door) sealed :append-ack) true
                           (catch Exception _ false))
            got (await-answer door sealed (if appended? microbatch-wait-ms 2000))]
        (cond
          (some? got) got
          (> (System/currentTimeMillis) deadline) (throw (ex-info "no answer" {:door :no-answer :name (:name sealed)}))
          :else (recur))))))

(defn- answered!
  "The door's memory after a gate answered an act: the sealed form goes.
  After a yes to a change of the lock grain, the door takes the grain of
  that layer again at its next lease. After a yes to a :session-closed
  act, the door drops the pooled locks of that session."
  [door a answer]
  (let [yes? (= :yes (:answer answer))
        facts (:facts a)
        closed (some #(when (= :session-closed (:k %)) (get-in % [:v :session])) facts)]
    (swap! (:memory door)
           (fn [m]
             (cond-> (update m :sealed dissoc a)
               (and yes? (some #(= :lock-grain (:k %)) facts)) (update :grain dissoc (:layer a))
               (and yes? closed) (update :pool dissoc [(:layer a) closed]))))))

;; -------------------------------------------------- send: steps 5 to 10

(defn send
  "Steps 5 to 10: an act as `make` gave it -> its answer.

  The answer is {:name :answer :reason :stamp}, and :batch from the
  microbatch gate. It is one of:

  - a gate's decision: a yes, or a no with its reason, and the stamp;
  - a refusal with no stamp. The door gives one when the act fails the
    examination of step 5, and when the gate refuses the lease for the
    act's locks: the reason is then the lease's, under the act's name. A
    gate gives one for an act that it refuses on its face. Nothing was
    recorded under the name.

  It throws \"no answer\", with the name in its data, when no answer came
  within the bound. It never gives \"sent\". The door keeps the sealed form
  of an act with no answer, so the caller can give the act again and the
  same bytes go.

  The door never makes a second name for an act. A new name after
  :class-mismatch is the caller's: the caller makes the act again."
  [door a]
  (let [gate (when (act/valid-name? (:name a)) (gate-for door a))]
    (if-let [reason (refusal-of a gate)]
      (refused a reason)
      (let [sealed (sealed-form door a gate)]
        (if-let [lease (:lease-refused sealed)]
          (refused a (:reason lease))
          (let [got (if (= :microbatch gate) (ask-microbatch-gate door sealed) (ask-stream-gate door sealed))
                answer (answer-from (:name a) got)]
            (answered! door a answer)
            answer))))))

(defn make-and-send
  "Make an act from a caller's parts, then send it: the answer."
  [door parts]
  (send door (make door parts)))

;; --------------------------------------------------- the answer by name

(defn- stream-answer
  "The answer that the stream gate recorded under a name, or nil."
  [door nm]
  (when-let [record (r/foreign-select-one [(p/keypath (nth nm 0) :answers nm)] (:stream-record door))]
    (answer-from nm record)))

(defn- microbatch-answer
  "The answer that the microbatch gate recorded under a name and that shows
  now, or nil. With no digest and no fingerprint the query gives the
  record as data."
  [door nm]
  (let [got (retrying #(r/foreign-invoke-query (:microbatch-answer door) nm nil nil nil))]
    (when (and (map? got) (not= :no-answer (:answer got)))
      (answer-from nm got))))

(defn answer
  "The answer by name. From a name and nothing more: the answer that a gate
  recorded under it, in the shape that `send` gives, or :no-answer. The
  name tells the layer and the class, and so the gate that holds the
  answer. For a name with no class the door looks in the stream record of
  the layer first, then in the microbatch record.

  This is the one function that knows where answers are kept. A door that
  started again, a bell or a tool holds only the name, and learns the
  outcome here. Something that is not a well-formed name gives :no-answer."
  [door nm]
  (or (when (act/valid-name? nm)
        (case (nth nm 1)
          :by-layer (stream-answer door nm)
          :by-entity (microbatch-answer door nm)
          (or (stream-answer door nm) (microbatch-answer door nm))))
      :no-answer))
