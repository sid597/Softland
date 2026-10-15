(ns softland.runtime.system-steps
  "The runtime's other work as the system, as steps that are safe to run
  again: a name changed; an agent or a program made; a token's session made
  and ended; an agent's run recorded; and the facts of a blocked attempt.
  Pure code. It needs no cluster.

  The start and a sign-in are steps of their own (`softland.runtime.start`,
  `softland.runtime.sign-in`). What is here is the rest of what the runtime
  writes into the system's layers and the record layers, which only the
  system writes (PROGRESS.md, \"The system's layers\"; BUILD.md, default
  18). Each step function has the form of `softland.runtime.steps`: it takes
  an input and what is known, and gives reads to make, one act of the system
  to send and wait for, done, or a refusal. Each reads first and does only
  what is missing, so a run that a crash cut is finished by the next run
  with the same input. A caller drives them with a door and a read path
  (`softland.runtime.service`).

  Who may ask for each is the caller's to check where it reads nothing (the
  kind of the actor); where it needs a fact, the step reads it: that an
  agent's maker is the one who asks for its token, that a session is
  ended by its identity or the identity's maker.

  A name. A freed name waits before another identity can take it (BUILD.md,
  default 2): a place under `:name/of` whose nearest fact is a retract is
  taken again only once the rule's wait (`:rule/name-wait-ms`, on `:names`
  in the rules layer) has passed since the time on the act that wrote the
  retract. That time is the door's label, the clock of the server that ran
  the door (`softland.store.door/send-act`); it is the one time the record
  holds of the retract. The wait holds for every identity, the one that
  freed the name too. A place whose retract carries no usable time is free.
  Both are a session's choices.

  Inputs and results, by call:

    rename-step          an input, what is known -> the next thing to do
    make-identity-step   an input, what is known -> the next thing to do
    session-step         an input, what is known -> the next thing to do
    end-session-step     an input, what is known -> the next thing to do
    run-step             an input, what is known -> the next thing to do
    blocked-facts        a sender, the count, the rule, an attempt -> facts
    attempt-value        a thing stopped, why, the time -> the value kept

  Each step function's input and its done are in its docstring. Each input
  holds `:system-session`, the id of this start of the runtime, which every
  act of the system names as its session.

  The words of a refusal, beside the steps' own (`:read-failed`,
  `:step-refused`, `:act-not-answered`): `words`.

  State and effects. None. Each function is pure: the time and the fresh
  ids come in the input.

  Callers. `softland.runtime.service`, which drives these with its door and
  read path."
  (:require [softland.runtime.ids :as ids]
            [softland.runtime.records :as records]
            [softland.runtime.steps :as steps]
            [softland.runtime.token :as token]
            [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.door.text :as text]))

(def words
  "The words with which a step of this namespace stops, beside the steps'
  own:

    :input-not-whole            the input misses a part, or one is not of its
                                form
    :identity-not-found         the identity has no kind in `identities`
    :name-not-allowed           the name does not keep the rule
                                (`softland.runtime.ids/name?`)
    :name-taken                 another identity holds the name
    :name-waits                 the name was freed, and its wait has not
                                passed; the refusal says until when
    :maker-not-a-person         who makes an agent or a program is no person
    :identity-not-the-makers    the fresh id is held by an identity of
                                another kind or another maker
    :record-layer-not-the-systems  a record layer is made, and its top is
                                not the system
    :not-the-maker              the token is asked for an identity that the
                                asker did not make, or of no agent or program
    :may-not-end-this-session   the one who ends a session is neither its
                                identity nor the identity's maker
    :session-not-found          the session is not a session of that
                                identity
    :session-ended              the session was ended
    :only-an-agent-runs         a run is asked for an identity that is no
                                agent

  The words are a session's, Sid's to change."
  [:input-not-whole :identity-not-found :name-not-allowed :name-taken :name-waits
   :maker-not-a-person :identity-not-the-makers :record-layer-not-the-systems
   :not-the-maker :may-not-end-this-session :session-not-found :session-ended
   :only-an-agent-runs])

;; ----------------------------------------------------------------- helpers

(defn- identity-query
  "The query of an identity's record in `identities`, asked as the system."
  [id]
  (steps/entity-query ids/system ids/identities id false))

(defn- name-query
  "The query of a name's place in `names`, asked as the system, with the
  time on the act that wrote its nearest fact: the time that a freed name's
  wait counts from."
  [n]
  {:asks [:place (ids/name-entity n) :name/of] :asker ids/system :layers [ids/names] :with #{:time}})

(def ^:private wait-query
  "The query of the wait before a freed name is taken again."
  (steps/place-query ids/system ids/rules :names :rule/name-wait-ms))

(defn- name-state
  "Takes the answer of a name's place (`name-query`), the identity that
  would hold it, the wait in milliseconds or nil, and the time. Gives
  `:hers` when it points at that identity, `:free` when no fact is there or
  its retract's wait has passed, [:waits until] while the wait runs, and
  `:taken` when it points at another."
  [answer owner wait-ms now-ms]
  (let [pa (steps/place-of answer)]
    (case (:holds pa)
      :value (if (= owner (:value pa)) :hers :taken)
      :no-fact :free
      :nothing-here (let [t (:time pa)]
                      (if (and (int? wait-ms) (int? t) (< now-ms (+ t wait-ms)))
                        [:waits (+ t wait-ms)]
                        :free))
      :taken)))

(defn- facts
  "Takes a record's name, an entity and a record. Gives the record's facts
  (`softland.runtime.records/facts-of`). What must hold: the record keeps
  its shape; each caller made sure of it from its input."
  [record-name entity record]
  (:ok (records/facts-of record-name entity record)))

(defn- record-layer-made
  "The step of an identity's record layer: made by the system with the
  system as its top, when it is not made."
  [ctx]
  (let [r (:record-layer ctx)
        q (steps/place-query ids/system r ids/system :softland.store/permission)]
    (steps/reading ctx [q]
      (fn [[answer]]
        (cond
          (= [ids/system r r] (steps/value-at answer)) [:go ctx]
          (= :not-made (steps/left-out-why answer r)) (steps/acting ctx (steps/making-act ctx r ids/system) ctx)
          :else [:refuse :record-layer-not-the-systems {:layer r}])))))

(defn- see-in-record-layer
  "The permission that a record layer gives to one who may read it: `:see`,
  beneath the system's first permission there."
  [who r]
  [who r r [ids/system r r] :see])

(defn- record-sees
  "The step that gives `:see` in the record layer to each of `:sees` of the
  context that does not hold a fact there yet, in one act."
  [ctx]
  (let [r (:record-layer ctx)
        whos (:sees ctx)
        queries (mapv #(steps/place-query ids/system r % :softland.store/permission) whos)]
    (steps/reading ctx queries
      (fn [answers]
        (let [needed (keep (fn [[who a]]
                             (when (= :no-fact (steps/holds-at a))
                               {:e who :k :softland.store/permission :v (see-in-record-layer who r)}))
                           (map vector whos answers))]
          (if (empty? needed)
            [:go ctx]
            (steps/acting ctx (steps/system-act ctx r needed) ctx)))))))

(defn- run-list
  "Runs a list of steps on an input, as `softland.runtime.steps/run-steps`
  does, with the context that the steps share."
  [step-list input known ctx finish]
  (steps/run-steps step-list (merge {:in input :known known} ctx) finish))

;; ----------------------------------------------------------- a name changed

(defn- rename-wait
  "Reads the wait before a freed name is taken again."
  [ctx]
  (steps/reading ctx [wait-query]
    (fn [[a]] [:go (assoc ctx :wait-ms (steps/value-at a))])))

(defn- rename-identity
  "Reads the identity's record: its name, and the fact of that name."
  [ctx]
  (let [p (get-in ctx [:in :identity])]
    (steps/reading ctx [(identity-query p)]
      (fn [[a]]
        (let [held (steps/record-of a p)]
          (if (nil? (:identity/kind held))
            [:refuse :identity-not-found]
            [:go (assoc ctx
                        :old-name (:identity/name held)
                        :old-name-fact (steps/fact-at a [p :identity/name]))]))))))

(defn- rename-new-name
  "Reads the new name's place: hers, free, waiting or taken."
  [ctx]
  (let [{:keys [identity name now-ms]} (:in ctx)]
    (steps/reading ctx [(name-query name)]
      (fn [[a]]
        (let [state (name-state a identity (:wait-ms ctx) now-ms)]
          (cond
            (= :hers state) [:go (assoc ctx :claimed? true)]
            (= :free state) [:go (assoc ctx :claimed? false)]
            (vector? state) [:refuse :name-waits {:until (second state)}]
            :else [:refuse :name-taken]))))))

(defn- rename-old-name
  "Reads the old name's place, when there is an old name and it is not the
  new one: the fact that the rename retracts, when it still points at her."
  [ctx]
  (let [{:keys [identity name]} (:in ctx)
        old (:old-name ctx)]
    (if (or (nil? old) (= old name) (not (ids/name? old)))
      [:go ctx]
      (steps/reading ctx [(name-query old)]
        (fn [[a]]
          [:go (assoc ctx :old-fact (when (= identity (steps/value-at a)) (:fact (steps/place-of a))))])))))

(defn- rename-names
  "The act in `names`: the new name claimed, and the old one retracted,
  naming the fact it replaces. One act, so the two land together."
  [ctx]
  (let [{:keys [identity name]} (:in ctx)
        needed (concat (when-not (:claimed? ctx)
                         (facts :name (ids/name-entity name) {:name/of identity}))
                       (when-let [f (:old-fact ctx)]
                         [{:e (ids/name-entity (:old-name ctx)) :k :name/of :replaces f}]))]
    (if (empty? needed)
      [:go ctx]
      (steps/acting ctx (steps/system-act ctx ids/names needed) ctx))))

(defn- rename-identity-name
  "The act in `identities`: the identity's name, replacing the fact of the
  name it held."
  [ctx]
  (let [{:keys [identity name]} (:in ctx)]
    (if (= name (:old-name ctx))
      [:go ctx]
      (steps/acting ctx
                    (steps/system-act ctx ids/identities
                                      [(cond-> {:e identity :k :identity/name :v name}
                                         (:old-name-fact ctx) (assoc :replaces (:old-name-fact ctx)))])
                    ctx))))

(def ^:private rename-steps
  "The steps of a name changed, in order."
  [[:wait rename-wait]
   [:identity rename-identity]
   [:new-name rename-new-name]
   [:old-name rename-old-name]
   [:names rename-names]
   [:identity-name rename-identity-name]])

(defn rename-step
  "A name changed (PROGRESS.md, \"Keys\": the new name claimed and the old
  one retracted, naming the fact it replaces, in one act; then the
  identity's `:identity/name`). Takes the input {:identity id :name text
  :now-ms time :system-session uuid} and what is known. Gives the next thing
  to do, or done: {:do :done :identity id :name text}. A name that the
  identity already holds is done with no act.

  Safe to run again: a run cut after the act in `names` is finished by the
  next, which finds the new name hers and the old one freed, and writes the
  identity's name. Who may change whose name is the caller's to check."
  [input known]
  (let [{:keys [identity name now-ms system-session]} input]
    (cond
      (not (and (some? identity) (int? now-ms) (uuid? system-session))) {:do :refuse :word :input-not-whole}
      (not (ids/name? name)) {:do :refuse :word :name-not-allowed}
      :else (run-list rename-steps input known {}
                 (fn [_] {:do :done :identity identity :name name})))))

;; ---------------------------------------------- an agent or a program made

(defn- maker-is-a-person
  "Reads the maker's identity: a person, with her own layer."
  [ctx]
  (let [m (get-in ctx [:in :maker])]
    (steps/reading ctx [(identity-query m)]
      (fn [[a]]
        (let [held (steps/record-of a m)]
          (if (= :person (:identity/kind held))
            [:go (assoc ctx :maker-own-layer (:identity/own-layer held))]
            [:refuse :maker-not-a-person]))))))

(defn- made-name-free
  "Reads the name: free or the new identity's, else the run stops."
  [ctx]
  (let [{:keys [name now-ms fresh]} (:in ctx)]
    (steps/reading ctx [wait-query (name-query name)]
      (fn [[w a]]
        (let [state (name-state a (:id fresh) (steps/value-at w) now-ms)]
          (cond
            (= :hers state) [:go (assoc ctx :claimed? true)]
            (= :free state) [:go (assoc ctx :claimed? false)]
            (vector? state) [:refuse :name-waits {:until (second state)}]
            :else [:refuse :name-taken]))))))

(defn- made-identity
  "Reads the new identity, and writes it when it is not there."
  [ctx]
  (let [{:keys [kind maker name fresh copied-from]} (:in ctx)
        a (:id fresh)]
    (steps/reading ctx [(identity-query a)]
      (fn [[answer]]
        (let [held (steps/record-of answer a)]
          (cond
            (and (= kind (:identity/kind held)) (= maker (:identity/made-by held)))
            [:go (assoc ctx :record-layer (:identity/record-layer held))]

            (some? (:identity/kind held)) [:refuse :identity-not-the-makers]

            :else
            (steps/acting ctx
                          (steps/system-act ctx ids/identities
                                            (facts kind a (cond-> {:identity/kind kind
                                                                   :identity/name name
                                                                   :identity/record-layer (:record-layer fresh)
                                                                   :identity/made-by maker}
                                                            copied-from (assoc :identity/copied-from copied-from))))
                          (assoc ctx :record-layer (:record-layer fresh)))))))))

(defn- made-name-claim
  "Claims the name for the new identity, when it is not hers yet."
  [ctx]
  (let [{:keys [name fresh]} (:in ctx)]
    (if (:claimed? ctx)
      [:go ctx]
      (steps/acting ctx (steps/system-act ctx ids/names (facts :name (ids/name-entity name) {:name/of (:id fresh)})) ctx))))

(def ^:private make-identity-steps
  "The steps of an agent or a program made, in order."
  [[:maker maker-is-a-person]
   [:name made-name-free]
   [:identity made-identity]
   [:name-claim made-name-claim]
   [:record-layer record-layer-made]
   [:record-sees record-sees]])

(defn make-identity-step
  "An agent or a program made by a person (BUILD.md, defaults 7 and 18): its
  identity in `identities` (kind, name, record layer, maker, and what it was
  copied from), its name claimed in `names`, its record layer made with the
  system as its top, and `:see` given there to it and to its maker. Takes
  the input

    {:kind :agent or :program  :maker person  :name text
     :fresh {:id uuid :record-layer uuid}, made by the caller
     :copied-from id or nil    :now-ms time   :system-session uuid}

  and what is known. Gives the next thing to do, or done:
  {:do :done :id :name :record-layer :maker-own-layer}, the last the
  maker's own layer, where she writes the description.

  The identity is written before its name is claimed. So a claim that
  another wins between the read of the name and the claim stops the run
  with `:name-taken`, and leaves an identity that no name points at and
  that has no record layer: no token can be made for it. Its description
  is the maker's act, not the system's, and is not here."
  [input known]
  (let [{:keys [kind maker name fresh now-ms system-session]} input]
    (cond
      (not (and (contains? #{:agent :program} kind) (uuid? maker) (uuid? (:id fresh))
                (uuid? (:record-layer fresh)) (int? now-ms) (uuid? system-session)))
      {:do :refuse :word :input-not-whole}

      (not (ids/name? name)) {:do :refuse :word :name-not-allowed}

      :else
      (run-list make-identity-steps input known {:sees [(:id fresh) maker]}
           (fn [ctx] {:do :done :id (:id fresh) :name name
                      :record-layer (:record-layer ctx) :maker-own-layer (:maker-own-layer ctx)})))))

;; ------------------------------------------------------ a token's session

(defn- session-identity
  "Reads the identity that the token is for: an agent or a program, made by
  the one who asks."
  [ctx]
  (let [{:keys [identity made-by]} (:in ctx)]
    (steps/reading ctx [(identity-query identity)]
      (fn [[a]]
        (let [held (steps/record-of a identity)]
          (if (and (contains? #{:agent :program} (:identity/kind held))
                   (= made-by (:identity/made-by held))
                   (some? (:identity/record-layer held)))
            [:go (assoc ctx :kind (:identity/kind held) :record-layer (:identity/record-layer held))]
            [:refuse :not-the-maker]))))))

(defn- session-written
  "Writes the session with its token's hash. The session is fresh, so
  there is nothing to read."
  [ctx]
  (let [{:keys [identity made-by fresh asked]} (:in ctx)
        the-token (token/make-token identity (:session fresh) (:secret fresh))
        record (cond-> {:session/of identity
                        :session/kind (if (= :agent (:kind ctx)) :conversation :program-run)
                        :session/token-hash (token/text-hash the-token)
                        :session/made-by made-by}
                 (some? asked) (assoc :session/asked asked))]
    (steps/acting ctx
                  (steps/system-act ctx (:record-layer ctx) (facts :session (:session fresh) record))
                  (assoc ctx :token the-token))))

(defn session-step
  "A token made for an agent or a program, by the person who made it
  (BUILD.md, default 5): a session in the identity's record layer, of kind
  `:conversation` for an agent and `:program-run` for a program, with the
  hash of its token and its maker, and, for a token that the agent asked
  for and its person approved in her browser, the request: its id, its
  code and when it was asked (`:session/asked`), so that the record tells
  an approved token from a pasted one (the build session's, 15 October
  2026). Takes the input {:identity uuid :made-by person :fresh {:session
  uuid :secret 32 bytes} :asked {:request uuid :code text :asked-at time}
  or absent :system-session uuid} and what is known. Gives the next thing to do, or done:
  {:do :done :identity :session :token}, the token's text, which the store
  never holds. Refused `:not-the-maker` when the identity is no agent or
  program of hers."
  [input known]
  (let [{:keys [identity made-by fresh system-session]} input]
    (if-not (and (uuid? identity) (uuid? made-by) (uuid? (:session fresh))
                 (bytes? (:secret fresh)) (= 32 (alength ^bytes (:secret fresh))) (uuid? system-session)
                 (or (nil? (:asked input)) (contains? (records/check :session {:session/of identity
                                                                              :session/kind :conversation
                                                                              :session/made-by made-by
                                                                              :session/asked (:asked input)})
                                                       :ok)))
      {:do :refuse :word :input-not-whole}
      (run-list [[:identity session-identity] [:session session-written]] input known {}
           (fn [ctx] {:do :done :identity identity :session (:session fresh) :token (:token ctx)})))))

;; ------------------------------------------------------ a session ended

(defn- ending-identity
  "Reads the identity whose session ends, and holds who ends it to it or
  its maker."
  [ctx]
  (let [{:keys [identity by]} (:in ctx)]
    (steps/reading ctx [(identity-query identity)]
      (fn [[a]]
        (let [held (steps/record-of a identity)]
          (cond
            (nil? (:identity/record-layer held)) [:refuse :identity-not-found]
            (not (or (= by identity) (= by (:identity/made-by held)))) [:refuse :may-not-end-this-session]
            :else [:go (assoc ctx :record-layer (:identity/record-layer held))]))))))

(defn- session-ended
  "Reads the session, and ends it when it is not ended."
  [ctx]
  (let [{:keys [identity session now-ms]} (:in ctx)
        r (:record-layer ctx)]
    (steps/reading ctx [(steps/entity-query ids/system r session false)]
      (fn [[a]]
        (let [held (steps/record-of a session)]
          (cond
            (not= identity (:session/of held)) [:refuse :session-not-found]
            (some? (:session/ended held)) [:go ctx]
            :else (steps/acting ctx (steps/system-act ctx r (records/session-end-facts session now-ms)) ctx)))))))

(defn end-session-step
  "A session ended: a person's sitting at her sign-out, or an agent's or a
  program's token (BUILD.md, defaults 5 and 10: ending a session is the
  runtime's job). `:session/ended` is written with the time
  (`softland.runtime.records/session-end-facts`) and the hash of its token
  is kept, so the runtime answers the token's holder `:session-ended` only
  once her secret has matched (`softland.runtime.service/actor-of`). Takes the input {:identity id :session uuid :by
  id :now-ms time :system-session uuid} and what is known. Who ends it is
  the identity itself or the identity's maker; any other is refused
  `:may-not-end-this-session`. Gives the next thing to do, or done:
  {:do :done :session uuid :ended-before? bool}; a session that was ended
  before is done with no act. `:ended-before?` is true when no act of this
  run ended it: the read after this run's own end shows the end too, so
  the read alone cannot tell."
  [input known]
  (let [{:keys [identity session by now-ms system-session]} input]
    (if-not (and (some? identity) (uuid? session) (some? by) (int? now-ms) (<= 0 now-ms) (uuid? system-session))
      {:do :refuse :word :input-not-whole}
      (run-list [[:identity ending-identity] [:session session-ended]] input known {}
           (fn [_] {:do :done :session session
                    :ended-before? (not-any? (comp steps/accepted? val) (:outcomes known))})))))

;; ---------------------------------------------------------- an agent's run

(defn- run-agent
  "Reads the agent: its record layer and its maker."
  [ctx]
  (let [a (get-in ctx [:in :agent])]
    (steps/reading ctx [(identity-query a)]
      (fn [[answer]]
        (let [held (steps/record-of answer a)]
          (if (and (= :agent (:identity/kind held)) (some? (:identity/record-layer held)))
            [:go (assoc ctx :record-layer (:identity/record-layer held) :maker (:identity/made-by held))]
            [:refuse :only-an-agent-runs]))))))

(defn- run-maker
  "Reads the maker's own layer, where the agent's description is."
  [ctx]
  (let [m (:maker ctx)]
    (steps/reading ctx [(identity-query m)]
      (fn [[a]] [:go (assoc ctx :maker-own-layer (:identity/own-layer (steps/record-of a m)))]))))

(defn- run-session
  "Reads the session that the run is in: the agent's, and not ended."
  [ctx]
  (let [{:keys [agent session]} (:in ctx)
        r (:record-layer ctx)]
    (steps/reading ctx [(steps/entity-query ids/system r session false)]
      (fn [[a]]
        (let [held (steps/record-of a session)]
          (cond
            (not= agent (:session/of held)) [:refuse :session-not-found]
            (some? (:session/ended held)) [:refuse :session-ended]
            :else [:go ctx]))))))

(defn- run-description
  "Reads the fact of the agent's description in its maker's own layer, as
  its maker: the system keeps no right there (BUILD.md, default 18). No
  fact, or a layer she may not see, gives none."
  [ctx]
  (let [a (get-in ctx [:in :agent])
        o (:maker-own-layer ctx)]
    (if (nil? o)
      [:go ctx]
      (steps/reading ctx [(steps/place-query (:maker ctx) o a :agent/description)]
        (fn [[answer]]
          [:go (assoc ctx :description (when (= :value (steps/holds-at answer))
                                         (:fact (steps/place-of answer))))])))))

(defn- run-written
  "Writes the run in the agent's record layer. The run is fresh, so there
  is nothing to read."
  [ctx]
  (let [{:keys [agent session run fresh]} (:in ctx)
        record (cond-> {:run/session session
                        :run/engine (:engine run)
                        :run/context (:context run)
                        :run/human-in-loop (boolean (:human-in-loop run))
                        :run/cause (:cause run)}
                 (:description ctx) (assoc :run/description (:description ctx)))
        got (records/facts-of :run (:run fresh) record)]
    (if-let [fs (:ok got)]
      (steps/acting ctx (steps/system-act ctx (:record-layer ctx) fs) ctx)
      [:refuse :input-not-whole {:at (:at got) :why (:refuse got)}])))

(def ^:private run-steps
  "The steps of a run, in order."
  [[:agent run-agent]
   [:maker run-maker]
   [:session run-session]
   [:description run-description]
   [:run run-written]])

(defn run-step
  "An agent's run recorded (BUILD.md, default 9): one model call in one of
  its sessions, in its record layer, with the fact id of its description
  at the run, its engine, its context, whether a human was in the loop,
  and what caused it. Takes the input

    {:agent uuid  :session uuid, the agent's session that its token named
     :run {:engine {:provider :model :effort} :context value
           :human-in-loop bool :cause value}
     :fresh {:run uuid}  :system-session uuid}

  and what is known. Gives the next thing to do, or done:
  {:do :done :run uuid :description fact-id-or-nil}.

  The description is read as the agent's maker, in her own layer: the
  runtime reads it for her so that the run names the version it was given.
  That reading as her is a session's choice. An agent with no description
  there has a run without `:run/description`. The context and the cause
  are values of the store, each within its bound on a value."
  [input known]
  (let [{:keys [agent session run fresh system-session]} input]
    (if-not (and (uuid? agent) (uuid? session) (map? run) (uuid? (:run fresh)) (uuid? system-session))
      {:do :refuse :word :input-not-whole}
      (run-list run-steps input known {}
           (fn [ctx] {:do :done :run (:run fresh) :description (:description ctx)})))))

;; --------------------------------------------------------- blocked attempts

(defn attempt-value
  "Takes a thing that the runtime stopped (an act, or a query), the word of
  why, and the time. Gives the value kept of it as `:blocked/attempt`:
  {:thing thing :why word :at time} when the thing is a value of the store
  whose text is within the store's bound on a value
  (`softland.store.door.declaration/max-value-bytes`); else
  {:digest hex :bytes n :why word :at time}, the digest of the thing as the
  door takes one (`softland.store.door.digest/hex-of`) and the bytes of its
  one text. It never throws."
  [thing why now-ms]
  (let [whole {:thing thing :why why :at now-ms}
        value? (try (act/value? whole) (catch Throwable _ false))
        size (try (long (text/utf8-count (text/text (if value? (act/normal-value whole) thing))))
                  (catch Throwable _ -1))]
    (if (and value? (<= 0 size declaration/max-value-bytes))
      whole
      {:digest (digest/hex-of thing) :bytes size :why why :at now-ms})))

(defn- power-of-two?
  "True for 1, 2, 4, 8 and so on."
  [n]
  (and (pos-int? n) (zero? (bit-and n (dec n)))))

(defn blocked-facts
  "Takes the sender, the count of its blocked attempts with this one, the
  rule of how many are kept whole (`:rule/keep-first`, or nil for none), an
  id for this attempt, and the value of the attempt (`attempt-value`).
  Gives the facts to write in the sender's record layer: `:blocked/count`
  on the sender when the count is 1, 2, 4, 8 and so on, so the record's
  count is never more than half behind and a flood writes few acts; and
  `:blocked/attempt` on the attempt's id while the count is within the
  rule. Gives an empty vector when neither holds."
  [sender n keep-first attempt-id attempt]
  (cond-> []
    (power-of-two? n) (conj {:e sender :k :blocked/count :v (long n)})
    (and (int? keep-first) (<= n keep-first)) (conj {:e attempt-id :k :blocked/attempt :v attempt})))
