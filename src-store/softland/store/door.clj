(ns softland.store.door
  "The door: the runtime's one entry for acts. It takes the parts of an act
  from a caller, puts the act in its envelope, seals its values, appends it to
  the acts depot for the class of its layer, and gives back the answer of the
  gate that decided it. It also finds a recorded answer from a name. One entry
  serves stream layers and microbatch layers. No caller names a depot, a gate
  or a class.

  Inputs and results, by call:

    connect        a cluster and the module's name -> a door
    make           a door and the parts from a caller -> the act as said  (steps 1 to 4)
    send           a door and an act -> an answer, or a throw             (steps 5 to 10)
    make-and-send  a door and the parts -> an answer, or a throw
    answer         a door and a name -> the recorded answer, or :no-answer

  An answer is {:name :answer :reason :stamp}, with :batch when the
  microbatch gate gave one. A refusal with no stamp was not recorded under
  the name, whoever gave it.

  State. A door is a map of seven Rama handles, borrowed from the cluster,
  and one atom of process memory. The memory holds only work in progress:
  leased locks that the door did not use yet, and the sealed form of each act
  that has no answer yet. It is lost when the process stops. Durable state is
  the module's: the record, the lease rows and each answer. A door can stop
  at any moment and the store loses nothing.

  Effects. `make` reads the settings of the layer. `send` appends to an acts
  depot; for an act with values it can first send a lease act of its own and
  query for the leased locks. `answer` only reads.

  Approach: the steps, in order.

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
   10. Give the answer.

  Waits and repeats. For a stream act `send` appends with a wait for the
  gate's event, and the ack carries the gate's decision. For a microbatch
  act it waits for the append only, then asks by name until the batch that
  decided the act shows. After an error, or a wait that ran out, it sends
  the same sealed bytes again, and the gate answers a repeat from its
  record. The door takes nothing from a sending that failed, and never
  takes a record that it read by name as the answer to its own sending.

  Words. \"Key\" is the key of a fact. \"Lock\" is an encryption key. The
  class words :by-layer and :by-entity are the words of the ruling on
  placement. On the wire the rig's word \"offer\" stays in two places until
  the gates and the depots are taken: the scheme word :offer in a name, and
  the depot names *offers and *micro-offers.

  The act's code is not here. It is the rig's envelope, required below as
  `act`: one copy, which the gates and the door both use. Until the module
  moves out of the rig, the door needs these four namespaces of the rig.
  The lease road, the two waits and the choice of gate follow the rig's two
  doors, rig.store.client and rig.store.micro-client; a function that
  differs from them says how. See README.md."
  (:refer-clojure :exclude [send])
  (:require [com.rpl.rama :as r]
            [com.rpl.rama.path :as p]
            [rig.store.envelope :as act]   ; the act: its parts, names, parse and sealed view
            [rig.store.locks :as locks]    ; the seal, and the lock ids of a lease
            [rig.store.gate :as gate]      ; the check for a misplaced setting
            [rig.store.micro :as micro]))  ; the microbatch gate's parse, digest and fingerprint

(def lease-size
  "How many locks one lease act asks for in a stream layer when the pool
  holds too few. An act that needs more gets as many as it needs from one
  lease, up to the 256 of one lease. The rig's number."
  64)

(def stream-tries
  "How many times `send` sends one act to the stream gate before it throws
  \"no answer\". The rig's bound."
  60)

(def microbatch-wait-ms
  "How long `send` waits for the microbatch gate's answer to one act before
  it throws \"no answer\". The rig's bound."
  60000)

;; ------------------------------------------------------------ the door

(defn- fresh-memory
  "Gives an empty memory for a door. Process state only, lost when the
  process stops. For each [layer session]: the leased locks that the door did
  not use (`:pool`) and the leases whose locks it took into that pool
  (`:known`, so that no lock goes in two times). The grain of each layer at
  its last lease (`:grain`). The sealed form of each act that has no answer
  yet, by the act as said (`:sealed`). The rig's `fresh-door`, without its
  note of the people that the door made."
  []
  {:pool {} :known {} :grain {} :sealed {}})

(defn connect
  "Takes a cluster and the module's name as a string. Gives a door: seven
  foreign handles on that module, taken one time, and an empty memory. It
  does not deploy, launch or check the module, and it gives no way to close
  the handles. The name is a string because a client knows a module by its
  name, so the door does not change when the module moves. A second
  `connect` is a door that started again: it holds no lock and no sealed
  form, and it takes its unused locks again by query (`refresh!`)."
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
  "Takes a function that reads, and a time limit (60 s). Gives what the
  function gives. Calls it again each 100 ms while it throws: a read that
  throws is \"not yet\" while a worker starts again. Throws the last error
  when the time limit passes. The rig's `retrying`."
  ([f] (retrying f 60000))
  ([f timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop []
       (let [got (try [:ok (f)] (catch Exception e [:error e]))]
         (cond (= :ok (first got)) (second got)
               (> (System/currentTimeMillis) deadline) (throw (second got))
               :else (do (Thread/sleep 100) (recur))))))))

(defn- stream-settings
  "Takes a door and a layer. Gives the layer's settings in the stream record,
  or nil for a layer that the stream gate did not make. One read of a PState,
  not done again on an error."
  [door layer]
  (r/foreign-select-one [(p/keypath layer :settings)] (:stream-record door)))

(defn- settings-versions
  "Takes a door and a layer. Gives each version of the layer's settings that
  the microbatch record keeps, by batch; empty for a layer that the
  microbatch gate did not make. Done again while it throws (`retrying`)."
  [door layer]
  (retrying #(into (sorted-map) (r/foreign-select [(p/keypath layer :settings) p/ALL] (:microbatch-record door)))))

(defn- settings-of
  "Takes a door and a layer. Gives the settings of the layer in force: the
  last version in the microbatch record, else the settings in the stream
  record, else nil. Done again while it throws (`retrying`)."
  [door layer]
  (retrying #(or (last (r/foreign-select [(p/keypath layer :settings) (p/sorted-map-range-to-end 1) p/MAP-VALS]
                                         (:microbatch-record door)))
                 (stream-settings door layer))))

(defn- class-of-layer
  "Step 2. Takes a door, a layer and the facts of one act. Gives the class
  of the layer for that act: the class fact of the layer, read from the
  stream record, else from the microbatch record. The act that makes a layer
  finds no class fact yet. The class is then that act's own class fact about
  the layer, as a gate reads it, and :by-layer if the act has none. It does
  not check the value: the gate's parse refuses a class that is not one.

  This is the one function that knows where the class fact is. The door
  reads the class for each act and keeps it for no other act, because the
  name holds the class and a change of class can come at any time. The
  class is never worked out from the kind of the layer or from the number
  of its owners. The rig's `layer-class`: there each caller calls it and
  hands the class in, and the making act's own class fact is not read."
  [door layer facts]
  (or (when (act/readable-keyword? layer)
        (or (:class (stream-settings door layer))
            (:class (settings-of door layer))))
      (when (sequential? facts)
        (some #(when (and (map? %) (= layer (:e %)) (= :class (:k %))) (:v %)) facts))
      :by-layer))

;; ------------------------------------------------- make: steps 1 to 4

(defn- envelope
  "Steps 3 and 4. Takes the parts from a caller, and a class. Gives the act
  as said: one map with the twelve parts. It fills the version, the class,
  the empty defaults, a claimed-when from the door's clock if the caller
  gave none (a claim, not a stamp), and a new name if the caller gave none.
  It leaves the session as given: the door makes up no session, because a
  session starts at a sign-in. It leaves the facts as given.

  Limits. It ignores a key that is not one of the parts, a :class too. It
  throws if :facts is something that `vec` cannot take, such as a number.
  The rig's `build`, without its made-up session."
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
  "Steps 1 to 4. Takes a door and the parts from a caller. Gives the act as
  said, with its name. `parts` holds :who, :layer, :permission, :session,
  :facts, :stood-on, :because-of and :subjects, and :name and :claimed-when
  if the caller has them. The caller gives no class: the door reads it.
  Effect: reads the settings of the layer. It appends nothing.

  A name that the caller gives is kept as it is; `send` refuses the act if
  the name was made for another layer or class. To make an act and to send
  it are two calls, so a series can have all its names before its first act
  goes. The map that comes back is what is sent and what is sent again; the
  values in it are still plain. Limits: as `envelope`."
  [door parts]
  (envelope parts (class-of-layer door (:layer parts) (:facts parts))))

;; --------------------------------------------- the gate, and step 5

(defn- gate-for
  "Takes a door and an act whose name is well formed. Gives :stream or
  :microbatch: the gate that decides the act, read off its name. The stream
  gate for a :by-layer name, the microbatch gate for a :by-entity name. A
  name with no class is on an act that the store places itself. It goes to
  the microbatch gate if the microbatch record holds settings for its layer,
  or if it forgets a value that the microbatch gate accepted; else to the
  stream gate. Only a name with no class costs a read. Facts that are not
  well formed do not make it throw. The rig's `gate-for`."
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
  "Step 5. Takes an act and its gate; the gate is nil when the name is not
  well formed. Gives the reason why the door does not send the act, or nil.
  It reads and writes nothing. Three checks, and the first that fails gives
  the reason:

  - the gate's own parse, on the sealed view of the act (the act with each
    plain value replaced by a mark, as the gate will get it);
  - a setting key about an entity that is not the act's layer
    (:malformed-control, the gates' own word for it);
  - a value fact and no session (:no-session, the door's one own reason).

  Only the sender can keep a bad act out of a depot: a depot keeps what it
  gets, and a client can read it. With no gate the stream gate's parse gives
  the reason. The rig throws for the misplaced setting (`refuse-misplaced!`)
  and sends each other bad act for the gate to refuse."
  [a gate]
  (let [view (act/sealed-view a)
        parsed (if (= :microbatch gate) (micro/parse-micro view) (act/parse view))
        facts (:facts a)]
    (cond
      (contains? parsed :refuse) (:refuse parsed)
      (some #(gate/misplaced-setting? a %) facts) :malformed-control
      (and (nil? (:session a)) (some act/value-fact? facts)) :no-session)))

(defn- refused
  "Takes an act and a reason. Gives a refusal of the door itself: a no with
  no stamp, under the act's name as it was given, well formed or not."
  [a reason]
  {:name (:name a) :answer :no :reason reason :stamp nil})

(defn- answer-from
  "Step 10. Takes a name, and what a gate or its record gave. Gives the
  answer in its one shape: the name, :yes or :no, the reason, the stamp; and
  the batch when the microbatch gate gave one. Who, permission, session and
  subjects are parts of the act, and to read them is a read, not an answer."
  [nm given]
  (cond-> {:name nm :answer (:answer given) :reason (:reason given) :stamp (:stamp given)}
    (contains? given :batch) (assoc :batch (:batch given))))

;; ------------------------------------------------ the locks for step 6

(declare send)

(defn- value-indices
  "Takes an act that passed step 5. Gives the positions of the facts that
  the door seals."
  [a]
  (into [] (keep-indexed (fn [i f] (when (act/value-fact? f) i))) (:facts a)))

(defn- lease-act
  "Takes an act, a number of locks and a permission. Gives the lease act
  for it: an act like each other, by the same writer, in the same layer and
  session, that asks the gate for that many locks. It takes the class of the
  act that it serves, and the door does not read the class again for it: the
  lease must reach the gate that decides the act, or the act cannot find its
  lock. The same act that the rig's `lease-for!` and `lease-offer` build."
  [a n permission]
  (envelope {:who (:who a) :layer (:layer a) :permission permission :session (:session a)
             :facts [{:e (:session a) :k :lease :v {:count n}}]}
            (:class a)))

(defn- lease!
  "Takes a door, an act and its lease act. Gives the gate's answer to the
  lease. Effect: sends the lease act through `send`, the same entry as each
  other act. If the lease gets no answer, the throw that the caller sees
  carries the name of the act that the caller sent, with the lease's name
  under :lease."
  [door a lease]
  (try
    (send door lease)
    (catch clojure.lang.ExceptionInfo e
      (if (= :no-answer (:door (ex-data e)))
        (throw (ex-info "no answer" {:door :no-answer :name (:name a) :lease (:name lease)} e))
        (throw e)))))

(defn- held-for
  "Takes the positions of an act's value facts, the locks picked for it as
  [[lock-id lock] ...], and the grain. Gives the locks that the act is
  sealed under: for each position the id of its lock (`:ids`), and for each
  id its lock (`:locks`). With the grain :per-act each value fact cites the
  first lock; else each cites its own. The rig's `held-for` and
  `assign-by-grain`."
  [idx picked grain]
  {:ids (if (= :per-act grain)
          (zipmap idx (repeat (first (first picked))))
          (zipmap idx (map first picked)))
   :locks (into {} picked)})

;; A stream layer: a pool of leased locks for each layer and session.

(defn- refresh!
  "Takes a door, a layer and a session. Gives the grain in force; nil for a
  layer that nobody made. Effects: one query for the session's unused locks,
  in plain form, and a change of the door's memory. Only the locks of leases
  that the door did not take before go into the pool: the locks of a lease
  go in one time, so a query result that is older than another thread's use
  of a lock cannot put the lock back. A door that started again knows no
  lease, and so takes each unused lock of the session.

  Limit: two doors that write in one session at one time take the same
  unused locks. The rig's `refresh!`."
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
  "Takes a door, a layer, a session and a number. Gives that many locks as
  [[lock-id lock] ...], taken out of the pool of [layer session]; nil, and
  the pool as it was, when the pool holds fewer. The rig's `take!`."
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
  "Takes a door, a stream act and the positions of its value facts. Gives
  the locks to seal it under (see `held-for`), or {:lease-refused answer}
  when the gate refused the lease; the door then has no lock for the act.
  Effects: takes locks out of the pool. While the pool holds too few it
  sends a lease act, for 64 locks or for as many as the act still needs, up
  to 256, and then queries for its locks. One thread leases at a time.
  Throws if the pool still holds too few after more than 100 rounds.

  The rig's `assign!`. After a refused lease the rig seals the act under
  locks that it throws away and sends it; here the refusal comes back."
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
  "Takes a door, the name of a lease in a microbatch layer, and a time limit
  (30 s). Gives the plain locks of that lease as {lock-id lock}. Asks by
  query each 50 ms until the query gives a lock. Gives an empty map when the
  time limit passes with no lock to give: the batch of the lease did not
  show, or the lease has no unused lock. `seal` then throws. A query that
  throws is \"not yet\". The rig's `take-locks`."
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
  "Takes a door, a microbatch act and the positions of its value facts.
  Gives the locks to seal it under (see `held-for`), or {:lease-refused
  answer} when the gate refused the lease. Effects: reads the grain of the
  layer, sends one lease act for this act (for one lock, or for one for each
  value, by the grain), and then queries for its locks. The lock ids are a
  function of the lease's name, so the door needs nothing from the lease's
  answer but its yes.

  The lease act leaves out the permission of the operator, as the rig's
  does. The rig's `write!` (its lease half) and `lease!`, without their
  options: a lease by another actor or permission, a forced grain."
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
  "Takes an act, the positions of its value facts, and its locks from
  `held-for`. Gives the act with each value fact sealed under its lock and
  citing the lock's id, and no plain value in it. A control fact and a
  retract stay as they are. Each sealing uses a fresh nonce, so two sealings
  of one act are different bytes. The door never seals under no lock: if it
  holds no lock for an id it throws {:door :no-lock} and builds nothing. The
  rig's `seal-with` and `seal`."
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
  "Step 6. Takes a door, an act that passed step 5, and its gate. Gives the
  act as the door sends it, or {:lease-refused answer} when the gate refused
  the lease for it; nothing is then sealed. An act with no value fact goes
  as it is. Else the door takes locks for it, seals it one time, and keeps
  that sealed form in its memory until `answered!` drops it. So each sending
  is the same bytes, and one act has one fingerprint in a door's life. No
  plain value and no lock goes into an acts depot.

  Limits. A fact that the caller hands in already sealed has no plain value,
  so it goes as it is. The sealed form of an act that never gets an answer
  stays in the memory. Two threads that send the same act at one time can
  each seal it. The rig's `sealed`, which keeps the locks and seals again
  for each sending."
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
  "Steps 7 to 9 for a stream act. Takes a door and a sealed act. Gives what
  the stream gate's ack gave: a map with :answer, :reason, :stamp and :name.
  Effect: appends the act to the stream acts depot and waits for the gate's
  event. After an error, or an ack with no answer in it, it waits 250 ms and
  appends the same map again. Throws \"no answer\", with the name in its
  data, after `stream-tries` sendings.

  The door takes nothing from a sending that failed, and it does not read
  the record to answer its own sending: on a repeat the gate's own look at
  its record is the one check that sees other value content under one name.
  The rig's `offer!` and `offer-until-answered!`; after an error the rig
  reads the record by name and compares a digest that covers no value."
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
  "Takes what the microbatch record gave by name, and the fingerprint of one
  sending. Gives true when it answers that sending itself: a refusal on its
  face, or a note that the gate answered it from the record, under this
  fingerprint; or the record that the gate decided for this fingerprint; or
  the name taken by other parts. A record that the gate decided for another
  sealed form is not yet this sending's answer: the digest does not see what
  a sealed value holds, so only the gate's own check of this sending can
  answer it. The rig's `own-answer?`."
  [got fingerprint]
  (boolean (and got (not= :no-answer (:answer got))
                (or (:face got) (:recorded got) (= fingerprint (:decided-fp got))
                    (and (= :name-taken (:reason got)) (not (contains? got :stamp)))))))

(defn- await-answer
  "Takes a door, a sealed microbatch act and a time limit. Gives this
  sending's own answer from the microbatch record, or nil when the time
  limit passes first. Asks by query each 50 ms, with the digest and the
  fingerprint of this sending. A query that throws is \"not yet\".

  An answer that is not this sending's own is no answer. The rig's
  `await-answer` gives its last result when its time limit passes, its own
  or not; so a record that the gate decided for another sealed form came
  back as the answer."
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
  "Steps 7 to 9 for a microbatch act. Takes a door and a sealed act. Gives
  this sending's own answer from the microbatch record. Effect: appends the
  act to the microbatch acts depot and waits for the append only, because a
  microbatch depot has no ack from the gate; then asks by name until the
  batch shows (`await-answer`). After an append that failed it waits up to
  2 s for an answer and appends the same map again. After a good append it
  waits the whole time and does not send again. Throws \"no answer\", with
  the name in its data, after `microbatch-wait-ms`. The rig's `send!` and
  `offer!`."
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
  "Takes a door, an act and a gate's answer to it. Effect on the door's
  memory: the sealed form of the act goes. After a yes to a change of the
  lock grain, the door takes the grain of that layer again at its next
  lease. After a yes to a :session-closed act, the door drops the pooled
  locks of that session, which the gate deleted.

  Limit: the locks that an act was sealed under are never put back in the
  pool. If the gate did not consume them, as with an answer that has no
  stamp, only a door that starts again finds them. The rig's `answered!`,
  which its stream door alone runs; here it runs for the answers of both
  gates."
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
  "Steps 5 to 10. Takes a door and an act as `make` gave it. Gives its
  answer: {:name :answer :reason :stamp}, and :batch from the microbatch
  gate. The answer is one of:

  - a gate's decision: a yes, or a no with its reason, and the stamp;
  - a refusal with no stamp. The door gives one when the act fails the
    examination of step 5, and when the gate refuses the lease for the
    act's locks: the reason is then the lease's, under the act's name. A
    gate gives one for an act that it refuses on its face. Nothing was
    recorded under the name.

  Effects: for an act with values, a lease act and a query for its locks
  when the door holds too few; one or more appends of the same sealed bytes
  to one acts depot; a change of the door's memory. An act that the door
  refuses causes no append.

  Throws \"no answer\", with the name in its data, when no answer came
  within the bound. It never gives \"sent\". The door keeps the sealed form
  of an act with no answer, so the caller can give the act again and the
  same bytes go. Something that is not an act comes back as a refusal, not
  a throw.

  The door never makes a second name for an act. A new name after
  :class-mismatch is the caller's: the caller makes the act again. The
  rig's `offer-into!` and `write!`."
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
  "Takes a door and the parts from a caller. Makes the act, then sends it.
  Gives the answer, or throws, as `send` does."
  [door parts]
  (send door (make door parts)))

;; --------------------------------------------------- the answer by name

(defn- stream-answer
  "Takes a door and a well-formed name. Gives the answer that the stream
  gate recorded under it, or nil. One read of a PState. The rig's `record`."
  [door nm]
  (when-let [record (r/foreign-select-one [(p/keypath (nth nm 0) :answers nm)] (:stream-record door))]
    (answer-from nm record)))

(defn- microbatch-answer
  "Takes a door and a well-formed name. Gives the answer that the microbatch
  gate recorded under it and that shows now, or nil. One query, done again
  while it throws. With no digest and no fingerprint the query gives the
  record as data, and no refusal that the gate gave on an act's face. The
  rig's `lookup`."
  [door nm]
  (let [got (retrying #(r/foreign-invoke-query (:microbatch-answer door) nm nil nil nil))]
    (when (and (map? got) (not= :no-answer (:answer got)))
      (answer-from nm got))))

(defn answer
  "The answer by name. Takes a door and a name, and nothing more. Gives the
  answer that a gate recorded under the name, in the shape that `send`
  gives, or :no-answer. It only reads. The name tells the layer and the
  class, and so the gate that holds the answer. For a name with no class
  the door looks in the stream record of the layer first, then in the
  microbatch record. Something that is not a well-formed name gives
  :no-answer.

  This is the one function that knows where answers are kept. A door that
  started again, a bell or a tool holds only the name, and learns the
  outcome here."
  [door nm]
  (or (when (act/valid-name? nm)
        (case (nth nm 1)
          :by-layer (stream-answer door nm)
          :by-entity (microbatch-answer door nm)
          (or (stream-answer door nm) (microbatch-answer door nm))))
      :no-answer))
