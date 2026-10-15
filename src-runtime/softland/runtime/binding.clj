(ns softland.runtime.binding
  "The binding: who is acting, made true on each act and read before the
  store's door and read path. Pure code. It needs no cluster.

  The door takes `:who`, `:session` and `:permission` as the maker gives
  them, and the read path asks as \"what the caller says\"
  (`softland.store.door`, `softland.store.read-path.query`). Sign-in stands
  in front of both and changes neither (BUILD.md, \"What sign-in is here\"):
  the runtime knows who is acting from her sitting or her token, and this
  namespace makes the act and the query say so.

  An actor is {:id :kind :session}, and an agent's run: `:kind` is
  `:person`, `:agent` or `:program`; `:session` is her sitting, the agent's
  conversation or the program's run, as its token named it; `:run` is the
  agent's model call, which the runtime found to be one of its session's
  before it made the actor. The system is no actor here: only the runtime's
  own steps act as the system (BUILD.md, default 17), and they make their
  acts whole (`softland.runtime.steps`).

  For an act (`bind-act`): a `:who` or a `:session` that is not the actor's
  is refused, as a client that claims another's name; otherwise both are
  set, `:session` to the sitting for a person and the run for an agent
  (BUILD.md, default 10). An act under a permission whose holder is not the
  actor is refused, and so is an agent's or a program's act that would make
  a layer: one that holds a fact under `:softland.store/depot` (BUILD.md,
  default 8: an agent is never at the top of a chain of rights); and one
  with a fact under another store key that its right's key list does not
  name (default 8: an agent gives no right and changes no setting unless
  its person allows it). An act
  that passes goes to the door, whose gate decides whether its permission
  stands; an act from someone without a right is logged and refused there
  (12 October 2026, entry 7). So is an act that is not in the act's one
  form: the binding puts her name on it and gives it back, and refuses
  only a thing that is no map, which has nothing to put a name on.

  For a query (`bind-query`): the asker is set to the actor, or the query
  says `:not-signed-in true` when no one is signed in. A query that names
  another asker is refused. A signed-in actor may ask as not signed in: she
  then sees less, and claims no one's name.

  Blocks and limits. A block is {:keys :layers :per-minute}, each part
  optional, the same list as a no (BUILD.md, default 12; PROGRESS.md,
  `:rule/block`). The pure functions here say whether a block stops an act
  or a read, and whether a count is over a limit. The counting, the keeping
  of the first few attempts whole, and which blocks and limits apply to
  which sender, are the service's (`softland.runtime.service`).

  Inputs and results, by call:

    bind-act           an actor, a maker's act -> {:ok act} or {:refuse word}
    bind-query         an actor or nil, a query -> {:ok query} or {:refuse word}
    block-stops-act?   a block, an act, the count -> true or false
    block-stops-read?  a block, a query -> true or false
    over-limit?        a limit per minute, the count -> true or false

  The words: `act-words`, `query-words`.

  State and effects. None. Each function is pure and never throws.

  Callers. `softland.runtime.service`, for each act and read of an actor,
  and for the blocks and limits that it reads from the rules layer and
  counts; and the pure proof."
  (:require [softland.store.act :as act]))

;; ------------------------------------------------------------------ actors

(def actor-shapes
  "The shape of an actor, in the act's shape language: an id, a kind, the
  session its token named, and for an agent the run it acts in."
  (assoc act/shapes
         :actor [:parts
                 [:id :uuid]
                 [:kind [:enum :person :agent :program]]
                 [:session :uuid]
                 [:run :uuid {:optional true :left-out true}]]))

(defn- actor-of
  "Takes anything. Gives the actor in its one form, or nil for a thing that
  is no actor."
  [x]
  (try (let [[verdict got] (act/as-shape actor-shapes :actor x)]
         (when (= :ok verdict) got))
       (catch Throwable _ nil)))

(defn acting-session
  "Takes an actor in its one form. Gives the id that its acts name as
  `:session`: the run for an agent, the session for a person or a program.
  Nil for an agent with no run."
  [actor]
  (if (= :agent (:kind actor)) (:run actor) (:session actor)))

;; -------------------------------------------------------------------- acts

(def act-words
  "The words of `bind-act`, in the order of its checks:

    :actor-not-known            the actor is no actor: no UUID id, a kind
                                that is not :person, :agent or :program, no
                                session
    :agent-act-without-a-run    an agent's act, and the actor has no run
    :not-an-act                 no plain map: nothing to put a name on
    :who-not-the-actor          the act names a `:who` that is not the actor
    :session-not-the-actors     the act names a `:session` that is not the
                                actor's sitting, or the agent's run
    :permission-not-the-actors  the holder of the act's permission, where it
                                can be read, is not the actor
    :may-not-make-a-layer       an agent's or a program's act holds a fact
                                under `:softland.store/depot`, where its
                                facts can be read
    :store-key-not-allowed-for-an-agent
                                an agent's or a program's act holds a fact
                                under a store key (a right given or ended, a
                                setting of a layer) that its permission's
                                key list does not name

  The words are a session's, Sid's to change; the last is the build
  session's, 15 October 2026."
  [:actor-not-known :agent-act-without-a-run :not-an-act :who-not-the-actor
   :session-not-the-actors :permission-not-the-actors :may-not-make-a-layer
   :store-key-not-allowed-for-an-agent])

(def ^:private max-facts-read
  "How many facts of an act that does not parse the binding looks at for a
  fact under `:softland.store/depot`: the door's bound on the facts of one
  act (`softland.store.door.declaration/max-facts`). An act with more is
  refused by the gate."
  256)

(defn- holder-of
  "Takes an act, parsed or as given. Gives the holder of its permission, the
  first slot, when its `:permission` is a vector with a slot; nil when it
  cannot be told."
  [the-act]
  (let [p (:permission the-act)]
    (when (and (vector? p) (seq p)) (first p))))

(defn- store-key?
  "True for a key of the store's own namespace."
  [k]
  (and (keyword? k) (= act/store-namespace (namespace k))))

(defn- store-key-not-named?
  "Takes an act, parsed or as given. Gives true when one of its facts, as
  far as they can be read (`max-facts-read`), is under a store key that what
  its permission allows does not name: allows must be a set of key
  patterns (`softland.store.act/allows`, the permission's fifth slot) that
  holds that key or `:softland.store/*`. `:see-and-act`, and a permission
  with no fifth slot, name no store key here, though the gate takes them
  as allowing every key."
  [the-act]
  (let [allows (act/allows (:permission the-act))
        named? (fn [k] (and (set? allows) (or (contains? allows k) (contains? allows :softland.store/*))))
        facts (:facts the-act)]
    (boolean (and (sequential? facts)
                  (some #(and (map? %) (store-key? (:k %)) (not (named? (:k %))))
                        (take max-facts-read facts))))))

(defn- makes-a-layer?
  "Takes an act, parsed or as given. Gives true when one of its facts, as
  far as they can be read, is a map under `:softland.store/depot`. Looks at
  `max-facts-read` facts at most of a sequence, and at none of anything
  else."
  [the-act]
  (let [facts (:facts the-act)]
    (boolean (and (sequential? facts)
                  (some #(and (map? %) (= :softland.store/depot (:k %))) (take max-facts-read facts))))))

(defn bind-act
  "Takes an actor and a maker's act. Gives {:ok act}, with `:who` the actor
  and `:session` her acting session (`acting-session`), or {:refuse word}
  with one of `act-words`.

  A thing that is no plain map is refused `:not-an-act`: there is nothing
  to put her name on. A `:who` or `:session` that the act names must be the
  actor's; one that it leaves out is set. Then the holder of the permission
  must be the actor, and an agent's or a program's act must hold no fact
  under `:softland.store/depot`: a making act is the one act with such a
  fact (`softland.store.gate.first-act`).

  An act that does not parse (`softland.store.act/parse`) is not refused
  here. It is given back as it came, with her `:who` and `:session` set, so
  that it goes to the door and the gate logs it and refuses it with its own
  word: an act is stored and then refused, not dropped (12 October 2026,
  entry 7, \"why don't we store both?\"; the build session's reading of it,
  15 October 2026). For such an act the holder and the depot facts are
  checked only where they can be read from the map (`holder-of`,
  `makes-a-layer?`); where they cannot, the gate's own check of standing
  refuses it, since it holds `:who` against the permission's holder. An act
  that parses is given back in its one form.

  An agent's or a program's fact under any other store key (a right given
  or ended, a setting of a layer) is refused `:store-key-not-allowed-for-an-agent`
  unless its permission's key list names that key, or
  `:softland.store/*`: an agent gives no right and changes no setting
  unless its person allows it (BUILD.md, default 8, deny by default; the
  build session's ruling of 15 October 2026). A right with no key list, or
  `:see-and-act`, allows the agent no store key here, though the gate
  would take it.

  Limits. It reads no record: whether the permission stands, what it
  allows beyond the store keys of an agent, and whether the act is blocked
  or over a limit are asked elsewhere. An act whose `:facts` has no end
  does not come back from the parse."
  [actor the-act]
  (let [actor (actor-of actor)
        session (when actor (acting-session actor))]
    (cond
      (nil? actor) {:refuse :actor-not-known}
      (nil? session) {:refuse :agent-act-without-a-run}
      (not (and (map? the-act) (not (record? the-act)))) {:refuse :not-an-act}
      (and (some? (:who the-act)) (not= (:id actor) (:who the-act))) {:refuse :who-not-the-actor}
      (and (some? (:session the-act)) (not= session (:session the-act))) {:refuse :session-not-the-actors}
      :else
      (let [named (assoc the-act :who (:id actor) :session session)
            bound (or (:ok (act/parse named)) named)
            holder (holder-of bound)]
        (cond
          (and (some? holder) (not= (:id actor) holder))
          {:refuse :permission-not-the-actors}

          (and (contains? #{:agent :program} (:kind actor)) (makes-a-layer? bound))
          {:refuse :may-not-make-a-layer}

          (and (contains? #{:agent :program} (:kind actor)) (store-key-not-named? bound))
          {:refuse :store-key-not-allowed-for-an-agent}

          :else {:ok bound})))))

;; ----------------------------------------------------------------- queries

(def query-words
  "The words of `bind-query`:

    :actor-not-known      a thing that is given as the actor is no actor
    :not-a-query          the query is no plain map
    :asker-not-the-actor  the query names an asker that is not the actor,
                          or names one when no one is signed in"
  [:actor-not-known :not-a-query :asker-not-the-actor])

(defn bind-query
  "Takes an actor, or nil when no one is signed in, and a query
  (`softland.store.read-path.query`). Gives {:ok query} with the asker set,
  or {:refuse word} with one of `query-words`.

  With no actor: an `:asker` that the query names is refused; else the
  query says `:not-signed-in true`. With an actor: an `:asker` that is not
  hers is refused; a query that says `:not-signed-in true` is kept so, and
  reads as not signed in; else `:asker` is set to her id, for an agent the
  agent's own id. The rest of the query is left as given: the read path
  checks its form."
  [actor query]
  (cond
    (not (and (map? query) (not (record? query)))) {:refuse :not-a-query}

    (nil? actor)
    (if (some? (:asker query))
      {:refuse :asker-not-the-actor}
      {:ok (-> query (dissoc :asker) (assoc :not-signed-in true))})

    :else
    (let [actor (actor-of actor)]
      (cond
        (nil? actor) {:refuse :actor-not-known}
        (and (some? (:asker query)) (not= (:id actor) (:asker query))) {:refuse :asker-not-the-actor}
        (true? (:not-signed-in query)) {:ok (dissoc query :asker)}
        :else {:ok (-> query (dissoc :not-signed-in) (assoc :asker (:id actor)))}))))

;; ------------------------------------------------------- blocks and limits

(defn- in-layers?
  "Takes a block and a set of layers. Gives true when the block names no
  layers, or names one of these."
  [block layers]
  (or (not (contains? block :layers))
      (boolean (some #(contains? (:layers block) %) layers))))

(defn block-stops-act?
  "Takes a block in its one form ({:keys :layers :per-minute}, each part
  optional; `softland.runtime.records`, the record `:block`), an act, and
  the count of the sender's acts within the block's reach in the minute
  before this one. Gives true when the block stops the act.

  The block reaches the act when the act's layer is one of its `:layers`,
  or it names none; and a fact of the act has a key that its `:keys` names,
  or it names none. Its `:keys` are the patterns of a right
  (`softland.store.act/allows-key?`): a key, `:ns/*`, or `:*` for every
  plain key, never a store key. A block that reaches the act stops it when
  it names no `:per-minute`, or the count is at that number or over it.
  So a block of `:layers` alone stops every act in them, and a block with
  no part stops every act. A count that is no whole number counts as none."
  [block the-act count-this-minute]
  (let [reaches? (and (in-layers? block #{(:layer the-act)})
                      (or (not (contains? block :keys))
                          (boolean (some #(act/allows-key? (:keys block) (:k %)) (:facts the-act)))))
        n (if (int? count-this-minute) count-this-minute 0)]
    (boolean (and reaches?
                  (or (not (contains? block :per-minute))
                      (>= n (:per-minute block)))))))

(defn- query-layers
  "Takes a query. Gives the set of layers that it names: its `:layers`, or
  its `:from-layer`."
  [query]
  (set (or (:layers query) (when (:from-layer query) [(:from-layer query)]))))

(defn block-stops-read?
  "Takes a block in its one form and a query. Gives true when the block
  stops the read: it names no `:keys` and no `:per-minute`, and it names no
  `:layers` or one of the layers that the query names. \"See\" is reading,
  so a block of keys stops acts and not reads; a rate stops acts only.

  Limit. A query that names `:from-layer` is judged by that layer alone:
  the layers it builds on are known only once the read path has read it.
  So a read from a layer that is not blocked, which builds on one that is,
  is not stopped here."
  [block query]
  (boolean (and (not (contains? block :keys))
                (not (contains? block :per-minute))
                (in-layers? block (query-layers query)))))

(defn over-limit?
  "Takes the limit of a kind of actor, acts per minute
  (`:rule/acts-per-minute`), or nil for none; and the count of the sender's
  acts in the minute before this one. Gives true when this act would be
  over the limit: the count is at the limit or over it. False with no
  limit, or a count that is no whole number."
  [per-minute count-this-minute]
  (boolean (and (int? per-minute) (int? count-this-minute)
                (>= count-this-minute per-minute))))
