(ns softland.runtime.records
  "The runtime's records: what sign-in writes into the store, declared as
  data in the act's shape language, with the check of a record and the
  facts that a record becomes. Pure code. It needs no cluster.

  A record is the facts of one entity in one layer, given as a plain map
  from each fact's key to its value: a person in the system's identities
  layer is {:identity/kind :person :identity/name \"…\" …}, on the person's
  id. So a record's parts are the keys of its facts, and `facts-of` gives
  one fact for each part that is there. The records, where each sits, and
  on what entity: `records`. The keys are the runtime's plain keys, a
  session's words (PROGRESS.md, \"Keys\").

  Read `records` first. Each entry names the layer that holds the record,
  the entity it sits on, and its parts, each a key, a shape and options.
  `shapes` is the act's map of shapes (`softland.store.act/shapes`) with one
  `:parts` shape for each record and the shapes of the values inside them,
  read by the act's engine (`softland.store.act/as-shape`). The leaves of
  that language are the act's five; a string's rule, a name's rule and the
  form of a hash are no leaf, so they are `rules`, examined after the shape,
  as the act's `rules` are after its shape.

  A part marked `:one-write` takes one write in its layer: a fact that names
  nothing to replace is refused when its place holds a value
  (`:softland.store/one-write-keys`, a store change of this build). The
  keys that each system layer marks so are read from these marks
  (`one-write-keys`), so a key is marked in one place.

  Inputs and results, by call:

    check              a record's name and a thing -> {:ok record} or
                       {:refuse word :at part}
    facts-of           a record's name, an entity and a thing -> {:ok facts}
                       or {:refuse word :at part}
    part-facts         a record's name, an entity and some of its parts ->
                       {:ok facts} or {:refuse word :at part}
    one-write-keys     a system layer -> the set of its one-write keys
    session-end-facts  a session id and the time -> the fact that ends it

  State and effects. None. Each function is pure and never throws.

  Callers. `softland.runtime.start`, `softland.runtime.sign-in` and
  `softland.runtime.system-steps`, which make their acts' facts here;
  `softland.runtime.service`, which checks an agent's description and a
  block with `check` and writes a block's facts; and the pure proof.

  Not here. The acts that carry the facts, with their `:who`, layer,
  permission and session: the steps make the system's
  (`softland.runtime.steps`), and the binding sets them on an actor's
  (`softland.runtime.binding`)."
  (:require [softland.runtime.ids :as ids]
            [softland.runtime.token :as token]
            [softland.store.act :as act]))

;; ------------------------------------------------------------- the records

(def records
  "Each record the runtime writes: the layer that holds it, the entity it
  sits on, and its parts, in the order of the checks. A part is
  [key shape options]; the options are `:optional`, for a part that can be
  absent, and `:one-write`, for a key that its layer marks as taking one
  write. A shape is a shape of `shapes`. `:layer` is a system layer's id,
  or a word for a layer of an identity's: `:record-layer`, the identity's
  record layer (BUILD.md, default 18), or `:own-layer`, a person's own.

    :person           a person: kind, name, record layer, own layer, and the
                      invitation she came in by. The system makes her at her
                      first sign-in, and no other right is kept here
    :agent            an agent: a lasting id with a changeable name, made by
                      a person, maybe copied from another agent (BUILD.md,
                      default 7). Whether it keeps an own layer is open
    :program          a program: as an agent, its kind apart
    :system-identity  the system's own identity, made at the runtime's start
    :agent-description  an agent's description, which its person writes in
                      her own layer, on the agent: instructions, tools and
                      the engine it prefers
    :link             the link from an account to a person, in `ways-in`: the
                      person, the account as given (its kind and number), and
                      the invitation that brought the account in, when one did
    :way-in           the account as Google gave it, in the identity's record
                      layer: kind, number, and the email and name when given
    :name             a name, in `names`, on `(keyword \"name\" n)`: the
                      identity it points at
    :session          a session, in the identity's record layer: whose, which
                      kind, its token's hash, who made it, the account that
                      opened a sitting, when a sitting ends, the request that
                      a person approved when the token was asked for in her
                      browser, and when it was ended
    :run              one model call of an agent's session, in the agent's
                      record layer (BUILD.md, default 9). The description's
                      fact id is optional: an agent whose person wrote no
                      description runs without one
    :invitation       an invitation, in `invitations`: its code's hash, the
                      rights it gives, who made it, who used it
    :block            a block of a sender by the system, in `rules`, on the
                      sender: not these keys, not in these layers, not more
                      than this many a minute (BUILD.md, default 12)
    :limit            a limit of acts a minute, in `rules`, on a kind of actor
                      (`:person`, `:agent`, `:program`) or on one identity's
                      id, which the runtime reads before its kind's (BUILD.md,
                      default 15; the rule on an id is the build session's)
    :byte-limit       a limit of bytes a minute, on a kind or an id, read as
                      `:limit` is (the build session's, 15 October 2026)
    :sitting          how long a person's sitting lasts, in `rules`, on
                      `:person` (the build session's)
    :name-wait        how long a freed name waits, in `rules`, on `:names`
    :keep-first       how many blocked attempts are kept whole, in `rules`,
                      on `:blocked`
    :blocked-count    an identity's count of blocked attempts, in its record
                      layer, on the identity
    :blocked-attempt  one blocked attempt kept whole, in the sender's record
                      layer, on an id of the attempt

  A token is in the store only as its hash, the session's
  `:session/token-hash`; its parts, once read from its text, have the shape
  `:token` of `shapes` (`softland.runtime.token/parse-token`).

  Additions of this builder to the plan's keys, each keeping more:
  `:way-in/invitation` on a link, which a later sign-in reads to finish a
  first one that stopped before the identity was written; `:session/way-in`,
  the account that opened a sitting; and `:email-verified` in a way-in."
  {:person
   {:layer ids/identities :on :the-person
    :parts [[:identity/kind         [:enum :person] {:one-write true}]
            [:identity/name         :value]
            [:identity/record-layer :uuid           {:one-write true}]
            [:identity/own-layer    :uuid           {:one-write true}]
            [:identity/invited-by   :uuid           {:one-write true :optional true}]]}

   :agent
   {:layer ids/identities :on :the-agent
    :parts [[:identity/kind         [:enum :agent] {:one-write true}]
            [:identity/name         :value]
            [:identity/record-layer :uuid          {:one-write true}]
            [:identity/own-layer    :uuid          {:one-write true :optional true}]
            [:identity/made-by      :uuid          {:one-write true}]
            [:identity/copied-from  :uuid          {:one-write true :optional true}]]}

   :program
   {:layer ids/identities :on :the-program
    :parts [[:identity/kind         [:enum :program] {:one-write true}]
            [:identity/name         :value]
            [:identity/record-layer :uuid            {:one-write true}]
            [:identity/made-by      :uuid            {:one-write true}]
            [:identity/copied-from  :uuid            {:one-write true :optional true}]]}

   :system-identity
   {:layer ids/identities :on :system
    :parts [[:identity/kind         [:enum :system] {:one-write true}]
            [:identity/name         :value]
            [:identity/record-layer :id             {:one-write true}]]}

   :agent-description
   {:layer :own-layer :on :the-agent
    :parts [[:agent/description :description]]}

   :link
   {:layer ids/ways-in :on :the-account
    :parts [[:way-in/person     :uuid            {:one-write true}]
            [:way-in/account    :account-as-given]
            [:way-in/invitation :uuid            {:one-write true :optional true}]]}

   :way-in
   {:layer :record-layer :on :the-account
    :parts [[:way-in/account :account-as-google-gave-it]]}

   :name
   {:layer ids/names :on :the-name
    :parts [[:name/of :id {:one-write true}]]}

   :session
   {:layer :record-layer :on :the-session
    :parts [[:session/of         :id]
            [:session/kind       [:enum :sitting :conversation :program-run]]
            [:session/token-hash :value          {:optional true}]
            [:session/made-by    :id]
            [:session/way-in     :uuid           {:optional true}]
            [:session/ends-at    [:int {:min 0}] {:optional true}]
            [:session/asked      :asked          {:optional true}]
            [:session/ended      [:int {:min 0}] {:optional true}]]}

   :run
   {:layer :record-layer :on :the-run
    :parts [[:run/session        :uuid]
            [:run/description    :fact-id        {:optional true}]
            [:run/engine         :engine]
            [:run/context        :value]
            [:run/human-in-loop  [:enum true false]]
            [:run/cause          :value]]}

   :invitation
   {:layer ids/invitations :on :the-invitation
    :parts [[:invitation/code-hash :value]
            [:invitation/gives     [:seq-of :give]]
            [:invitation/made-by   :id]
            [:invitation/used      :use {:one-write true :optional true}]]}

   :block
   {:layer ids/rules :on :the-sender
    :parts [[:rule/block :block-list]]}

   :limit
   {:layer ids/rules :on :the-kind-of-actor-or-an-id
    :parts [[:rule/acts-per-minute [:int {:min 0}]]]}

   :byte-limit
   {:layer ids/rules :on :the-kind-of-actor-or-an-id
    :parts [[:rule/bytes-per-minute [:int {:min 0}]]]}

   :sitting
   {:layer ids/rules :on :person
    :parts [[:rule/sitting-ms [:int {:min 0}]]]}

   :name-wait
   {:layer ids/rules :on :names
    :parts [[:rule/name-wait-ms [:int {:min 0}]]]}

   :keep-first
   {:layer ids/rules :on :blocked
    :parts [[:rule/keep-first [:int {:min 0}]]]}

   :blocked-count
   {:layer :record-layer :on :the-identity
    :parts [[:blocked/count [:int {:min 0}]]]}

   :blocked-attempt
   {:layer :record-layer :on :the-attempt
    :parts [[:blocked/attempt :value]]}})

;; -------------------------------------------------------------- the shapes

(def max-gives
  "How many rights one invitation can give. A number of the build."
  64)

(def max-block-entries
  "How many keys, and how many layers, one block can name: the act's bound
  on key patterns (`softland.store.act/max-key-patterns`)."
  act/max-key-patterns)

(defn- part-entry
  "Takes a part of `records`. Gives its entry in a `:parts` shape: the key
  and the shape, and options that make the key the reason when the part
  does not fit or is absent, and that leave an absent optional part out of
  the one form, so that `facts-of` makes no fact for it."
  [[k shape opts]]
  [k shape (cond-> {:reason k :absent k}
             (:optional opts) (assoc :optional true :left-out true))])

(def shapes
  "The runtime's map of shapes: the act's map
  (`softland.store.act/shapes`), one `:parts` shape for each record of
  `records`, under the record's name, and the shapes of the values inside
  them:

    :account-as-given           {:kind :google :sub text}
    :account-as-google-gave-it  the same, and :email, :email-verified and
                                :name when Google gave them
    :description                {:instructions text :tools any :engine engine},
                                the last two optional
    :engine                     {:provider text :model text :effort any}, the
                                effort optional
    :give                       {:layer id :allows what}: a right that an
                                invitation gives, beneath the system's first
                                permission in that layer; what it allows is
                                the act's `:allows`
    :use                        {:person uuid :account uuid}: who used an
                                invitation
    :block-list                 {:keys #{key pattern} :layers #{id}
                                 :per-minute n}, each part optional: what a
                                block names
    :token                      {:identity uuid :session uuid :secret text}:
                                the parts of a token's text
    :asked                      {:request uuid :code text :asked-at time}: a
                                token request approved in the browser

  An optional part inside a value is left out of the one form when absent."
  (merge act/shapes
         (into {} (map (fn [[record-name {:keys [parts]}]]
                         [record-name (into [:parts] (map part-entry) parts)]))
               records)
         {:account-as-given          [:parts [:kind [:enum :google]] [:sub :value]]
          :account-as-google-gave-it [:parts [:kind [:enum :google]] [:sub :value]
                                      [:email :value {:optional true :left-out true}]
                                      [:email-verified [:enum true false] {:optional true :left-out true}]
                                      [:name :value {:optional true :left-out true}]]
          :description               [:parts [:instructions :value]
                                      [:tools :value {:optional true :left-out true}]
                                      [:engine :engine {:optional true :left-out true}]]
          :engine                    [:parts [:provider :value] [:model :value]
                                      [:effort :value {:optional true :left-out true}]]
          :give                      [:parts [:layer :id] [:allows :allows]]
          :use                       [:parts [:person :uuid] [:account :uuid]]
          :block-list                [:parts
                                      [:keys [:set-of :key {:max max-block-entries}] {:optional true :left-out true}]
                                      [:layers [:set-of :id {:max max-block-entries}] {:optional true :left-out true}]
                                      [:per-minute [:int {:min 0}] {:optional true :left-out true}]]
          :token                     [:parts [:identity :uuid] [:session :uuid] [:secret :value]]
          :asked                     [:parts [:request :uuid] [:code :value] [:asked-at [:int {:min 0}]]]}))

;; --------------------------------------------------------------- the rules

(defn- text?
  "True for a string of 1 to 255 characters: the bound of an account's
  number, an email, a display name, a provider and a model."
  [x]
  (and (string? x) (<= 1 (count x) 255)))

(def rule-checks
  "The code of each check that a rule names. Each takes the value at the
  rule's path, which is not nil, and gives true when it holds.

    :name        a name that keeps the rule (`softland.runtime.ids/name?`)
    :text        a string of 1 to 255 characters
    :hash        a SHA-256 hex text (`softland.runtime.token/hash?`)
    :few-gives   a list of `max-gives` rights at most"
  {:name      ids/name?
   :text      text?
   :hash      token/hash?
   :few-gives #(<= (count %) max-gives)})

(def rule-words
  "The refusal word of each check of `rule-checks`."
  {:name      :name-not-allowed
   :text      :not-a-text
   :hash      :not-a-hash
   :few-gives :too-many-gives})

(def rules
  "What a record that has its shape must also keep: for each record, pairs
  of a path into the record and a check of `rule-checks`, in the order in
  which they are examined. A path whose value is absent is passed over: an
  optional part that is not there keeps every rule."
  {:person          [[[:identity/name] :name]]
   :agent           [[[:identity/name] :name]]
   :program         [[[:identity/name] :name]]
   :system-identity [[[:identity/name] :name]]
   :agent-description [[[:agent/description :instructions] :text]
                       [[:agent/description :engine :provider] :text]
                       [[:agent/description :engine :model] :text]]
   :link            [[[:way-in/account :sub] :text]]
   :way-in          [[[:way-in/account :sub] :text]
                     [[:way-in/account :email] :text]
                     [[:way-in/account :name] :text]]
   :session         [[[:session/token-hash] :hash]]
   :run             [[[:run/engine :provider] :text]
                     [[:run/engine :model] :text]]
   :invitation      [[[:invitation/code-hash] :hash]
                     [[:invitation/gives] :few-gives]]})

(defn- broken-rule
  "Takes a record's name and the record in its one form. Gives
  {:refuse word :at path} for the first rule that it breaks, or nil."
  [record-name record]
  (some (fn [[path check]]
          (let [v (get-in record path)]
            (when (and (some? v) (not ((get rule-checks check) v)))
              {:refuse (get rule-words check) :at path})))
        (get rules record-name)))

;; --------------------------------------------------------------- the check

(def words
  "The words of `check`: `:record-not-known` for a name that `records` does
  not hold, `:record-not-in-shape` for a thing that does not fit the shape,
  and the words of `rule-words`."
  (into #{:record-not-known :record-not-in-shape} (vals rule-words)))

(defn- part-at-fault
  "Takes a record's name and a thing that its shape refused. Gives where
  the check stopped: `:malformed` for a thing that is no plain map; else
  the first key of the thing that the record does not name; else the first
  part, in the declared order, that does not fit by itself."
  [record-name x]
  (let [entries (rest (get shapes record-name))
        declared (into #{} (map first) entries)]
    (cond
      (not (and (map? x) (not (record? x)))) :malformed
      (some (complement declared) (keys x)) (first (remove declared (keys x)))
      :else (or (some (fn [[part :as entry]]
                        (when (= :no (first (act/as-shape shapes [:parts entry] (select-keys x [part]))))
                          part))
                      entries)
                :malformed))))

(defn check
  "The check of a record. Takes the name of a record of `records`, and
  anything. Gives {:ok record}, the record in its one form, where an absent
  optional part is absent; or {:refuse word :at where}: `:record-not-known`;
  `:record-not-in-shape` at the part that does not fit, at a key that the
  record does not name, or at `:malformed` for a thing that is no plain
  map; or the word of the first rule broken, at its path. It never throws."
  [record-name x]
  (try
    (if-not (contains? records record-name)
      {:refuse :record-not-known :at record-name}
      (let [[verdict got] (act/as-shape shapes record-name x)]
        (if (= :no verdict)
          {:refuse :record-not-in-shape :at (part-at-fault record-name x)}
          (or (broken-rule record-name got)
              {:ok got}))))
    (catch Throwable _ {:refuse :record-not-in-shape :at :malformed})))

(defn facts-of
  "Takes the name of a record, the entity it sits on, and the record. Gives
  {:ok facts}: one fact {:e entity :k key :v value} for each part of the
  record in its one form, in the order of `records`; or what `check` refused
  with. No fact is a retract: a part that is absent gives no fact. What must
  hold: the entity is an id of the store; the act that carries the facts
  is the caller's."
  [record-name entity x]
  (let [checked (check record-name x)]
    (if-let [record (:ok checked)]
      {:ok (into []
                 (keep (fn [[k]] (when (contains? record k) {:e entity :k k :v (get record k)})))
                 (get-in records [record-name :parts]))}
      checked)))

(defn- shape-of-some-parts
  "Takes a record's name. Gives a `:parts` shape of its parts in which every
  part is optional: the shape of a write of some parts of the record."
  [record-name]
  (into [:parts]
        (map (fn [[k shape opts]] (part-entry [k shape (assoc opts :optional true)])))
        (get-in records [record-name :parts])))

(defn part-facts
  "Takes the name of a record, the entity it sits on, and some of its parts,
  as a map. Gives {:ok facts}, one fact for each part given, each part held
  to its shape and its rules; or {:refuse word :at where}, as `check`. No
  part must be there: a claim of one one-write key, such as
  `:invitation/used`, writes one part of its record. Gives
  `:record-not-in-shape` at `:malformed` for an empty map. It never throws."
  [record-name entity x]
  (try
    (cond
      (not (contains? records record-name)) {:refuse :record-not-known :at record-name}
      (not (and (map? x) (seq x))) {:refuse :record-not-in-shape :at :malformed}
      :else
      (let [declared (assoc shapes ::some-parts (shape-of-some-parts record-name))
            [verdict got] (act/as-shape declared ::some-parts x)]
        (if (= :no verdict)
          {:refuse :record-not-in-shape :at (or got :malformed)}
          (or (broken-rule record-name got)
              {:ok (into []
                         (keep (fn [[k]] (when (contains? got k) {:e entity :k k :v (get got k)})))
                         (get-in records [record-name :parts]))}))))
    (catch Throwable _ {:refuse :record-not-in-shape :at :malformed})))

(defn one-write-keys
  "Takes a system layer's id. Gives the set of keys that the layer marks as
  taking one write: each part marked `:one-write` of a record that the
  layer holds. The empty set for a layer that holds none."
  [layer]
  (into #{}
        (for [[_ {record-layer :layer parts :parts}] records
              :when (= layer record-layer)
              [k _ opts] parts
              :when (:one-write opts)]
          k)))

(defn session-end-facts
  "Takes a session's id and the time, in milliseconds, that the caller read.
  Gives the one fact that ends it: `:session/ended` with the time. The hash
  of its token is kept, so that the runtime checks a token's secret before
  it says that its session ended, and only the holder of the right secret
  learns that (`softland.runtime.service/actor-of`; the build session's
  ruling of 15 October 2026, after the review). Gives nil for a time that
  is no whole number from zero."
  [session now-ms]
  (when (and (int? now-ms) (<= 0 now-ms))
    [{:e session :k :session/ended :v (long now-ms)}]))
