(ns softland.auth.reference
  "Auth's contract written a second time, as plain Clojure, from its spec
  alone: `src/proposal/runtime-2026-11-21/spine/auth/SPEC.md`. No Rama, no
  cluster, no module. It is what the module, and the runtime that calls it,
  can be held against.

  What it gives:

  - `step`, the twelve writes as a pure state machine: `(step state call)`
    gives `[state' result]`, where a call is `[:invite {...}]`,
    `[:enter {...}]` and so on. `empty-state` is where it starts.
  - The pure reads of a state: `who-is`, `ask-of`, `under`, and
    `live-under`, which is what is live under a root: the value that
    `watch-root` hands to its function.
  - `cluster`, a thing that stands for a cluster in which Auth is launched
    under the names that it is given; and the eighteen functions of the
    contract under their own names, `open` among them, which takes such a
    thing and a module's name, so that a program can stand on it. The reads
    take a client or a state.

  The state is one map:

    :persons  person -> {:claim :invited-by :invited-at, and from her first
                         enter :account :own-layer :making :own-layer-made?
                         :entered-at}
    :claims   claim -> the person whom it names, for good
    :links    account -> {:person :says :said-at}
    :roots    person -> {:agents {agent {:made-by :label :made-at :depth
                                         :ended?}}
                         :agent-order [agent ...]
                         :sign-ins {sign-in {:actor :kind :hash :first-hash
                                             :until :previous :label :begun
                                             :ended?}}
                         :sign-in-order [sign-in ...]
                         :sessions {session {:sign-in :until :driven-by
                                             :opened-at :closed?}}}
    :asks     code -> {:hash :asks :ends :asked-at :over-when-made? :state
                       :answer}

  A person is in `:roots` from her first enter on. She is then an actor of
  her root, at depth 0, and is not one of its `:agents`. Nothing is ever
  taken out of the map: an end is a flag. The reference keeps no log of
  events, and a refused enter leaves nothing. A result comes from what
  stands, never from a memory of calls (Sid's calls of 22 November): a
  link keeps one `:says`, an ask keeps the answer that stands, and a
  sign-in keeps its present hash and the one before it, with no history of
  its replacements. A sign-in keeps, as `:first-hash`, the hash that its
  begin bound, one for each sign-in, by which a repeat of that begin is
  known (QUESTIONS.md, 8).

  Approach. `step` reads the form of the call first: an input that does
  not fit gives `{:could-not :read-the-event}` and the state as it was. A
  write that names a `:root` then takes out of what is live under that root
  whatever has passed at its `:at` (`take-out`), however it is judged after.
  Then the write is judged: a repeat is recognised first, and an answer to
  an ask that has an answer is told what stands; then come the refusals in
  the order in which the contract lists them, and only then is the write
  taken. A refusal leaves the state as the take-out left it. Nothing reads a
  clock: every time is the `:at` of a call. Standing is worked out as the
  spec words it (`actor-live?`, `sign-in-live?`); what a read costs is not
  modelled.

  Where the spec does not say what happens, or says two things, the
  reference takes the reading that refuses more or changes less.
  `QUESTIONS.md`, beside this file, names each place and the reading, and
  says which of them Sid's calls of 22 November settled.

  Broken copies. `step` takes a third argument, a key of `breaks`, which
  breaks the one rule that `breaks` says; nil breaks none; and
  `broken-cluster` launches Auth with one rule so broken. They are for the
  proofs, whose generated histories must catch each one.

  State. `step` and the reads are pure. A cluster holds one atom for each
  name that it was made with: one Auth. Every client opened under that
  name shares the atom, as copies of the runtime share one Auth: the atom
  holds the state, the watches, the calls that the watches are owed, which
  clients are closed, and the broken rule, if any. A lock on the atom puts
  the writes, and the calls of the watches that follow each write, in one
  order."
  (:import [java.util Locale]))

;; ------------------------------------------------------------------ the bounds

(def ^:private longest-text
  "The most characters of a text, a `char` of the JVM being one."
  256)

(def ^:private longest-said
  "The most characters of a value in a `:says` map."
  2048)

(def ^:private most-said
  "The most entries of a `:says` map."
  32)

(def ^:private deepest
  "How many makers below its root an actor may be at most: the store's own
  bound on how deep a permission can be. The person is 0 below her root."
  32)

;; ------------------------------------------------------------------- the forms

(defn- time?
  "Whether `x` is a time: a fixed-width integer, milliseconds since 1970."
  [x]
  (int? x))

(defn- id?
  "Whether `x` is the id of a person, an actor, a sign-in or a session: a UUID."
  [x]
  (uuid? x))

(defn- hash?
  "Whether `x` is a hash: 64 lower-case hex characters."
  [x]
  (and (string? x) (= 64 (count x)) (some? (re-matches #"[0-9a-f]{64}" x))))

(defn- carried?
  "Whether UTF-8 can carry the string `s`: it holds no surrogate character
  without its pair. A string that holds one does not fit wherever the
  contract takes a string (QUESTIONS.md, 29: the build's default of 23
  November, for Sid to turn)."
  [^String s]
  (let [n (count s)]
    (loop [i 0]
      (cond
        (>= i n) true
        (Character/isHighSurrogate (.charAt s i))
        (and (< (inc i) n) (Character/isLowSurrogate (.charAt s (inc i))) (recur (+ i 2)))
        (Character/isLowSurrogate (.charAt s i)) false
        :else (recur (inc i))))))

(defn- code?
  "Whether `x` is the code of an ask: a string of 8 characters that UTF-8
  can carry."
  [x]
  (and (string? x) (= 8 (count x)) (carried? x)))

(defn- text?
  "Whether `x` is a text: a string of at most 256 characters that UTF-8 can
  carry."
  [x]
  (and (string? x) (<= (count x) longest-text) (carried? x)))

(defn- email?
  "Whether `x` is an email: a text in lower case. The spec says that the
  runtime gives emails so, and Auth holds it as form (QUESTIONS.md, 3)."
  [x]
  (and (text? x) (= x (.toLowerCase ^String x Locale/ROOT))))

(defn- provider?
  "Whether `x` is a provider: a keyword whose name, with its namespace when
  it has one, is at most 256 characters (Sid's call of 22 November), and can
  be carried by UTF-8."
  [x]
  (and (keyword? x)
       (let [full (subs (str x) 1)]
         (and (<= (count full) longest-text) (carried? full)))))

(defn- account?
  "Whether `x` is an account: a vector of a provider and the provider's
  subject, a text. `provider-ok?` is the test of the provider."
  ([x] (account? x provider?))
  ([x provider-ok?]
   (and (vector? x) (= 2 (count x)) (provider-ok? (nth x 0)) (text? (nth x 1)))))

(defn- says?
  "Whether `x` is what a provider said: a map of at most 32 entries, each
  from a text to a string of at most 2,048 characters that UTF-8 can carry."
  [x]
  (and (map? x)
       (<= (count x) most-said)
       (every? (fn [[k v]] (and (text? k) (string? v) (<= (count v) longest-said) (carried? v))) x)))

(defn- shaped?
  "Whether `m` is a map that holds every key of `parts`, each with a value
  that the key's test passes; that may hold a key of `optional`, whose value
  its test must pass; and that holds no other key."
  [m parts optional]
  (and (map? m)
       (every? (fn [[k ok?]] (and (contains? m k) (ok? (get m k)))) parts)
       (every? (fn [k] (or (contains? parts k) (contains? optional k))) (keys m))
       (every? (fn [[k ok?]] (or (not (contains? m k)) (ok? (get m k)))) optional)))

(defn- or-nil
  "A test that passes nil and what `ok?` passes."
  [ok?]
  (fn [x] (or (nil? x) (ok? x))))

(defn- is
  "A test that passes the keyword `k` alone."
  [k]
  (fn [x] (= k x)))

(defn- claim?
  "Whether `x` is a claim: `{:by :email :email e}` or `{:by :code :hash h}`.
  `email-ok?` is the test of the email."
  ([x] (claim? x email?))
  ([x email-ok?]
   (or (shaped? x {:by (is :email) :email email-ok?} {})
       (shaped? x {:by (is :code) :hash hash?} {}))))

(defn- kind?
  "Whether `x` is the kind of a sign-in: `:browser` or `:program`."
  [x]
  (or (= :browser x) (= :program x)))

(defn- making?
  "Whether `x` is a `:making`: the UUID of the act that makes an own layer,
  and the id that the act names as its session, a UUID too (QUESTIONS.md,
  4)."
  [x]
  (shaped? x {:uuid id? :session id?} {}))

(defn- new-agent?
  "Whether `x` is the `:new-agent` of an answer: nil, or `{:label text}`."
  [x]
  (or (nil? x) (shaped? x {:label text?} {})))

(defn- driven-by?
  "Whether `x` is a `:driven-by`: nil, `{:kind :person}`, or
  `{:kind :model :model text}`."
  [x]
  (or (nil? x)
      (shaped? x {:kind (is :person)} {})
      (shaped? x {:kind (is :model) :model text?} {})))

(defn- maker-or-system?
  "Whether `x` is who invited: an actor's id, or `:system`."
  [x]
  (or (id? x) (= :system x)))

(def ^:private forms
  "The form of each write's map: every part, with its test. A part that the
  contract writes `-or-nil` must be there, and may be nil; a part that the
  contract does not name does not fit; `:own-layer` is a UUID (Sid's call
  of 22 November)."
  {:invite {:person id? :claim claim? :invited-by maker-or-system? :at time?}
   :enter {:account account? :says says? :email (or-nil email?) :code-hash (or-nil hash?)
           :own-layer id? :making making? :at time?}
   :own-layer-made {:person id? :at time?}
   :begin-sign-in {:root id? :actor id? :sign-in id? :kind kind? :hash hash? :until time?
                   :label (or-nil text?) :at time?}
   :replace-secret {:root id? :sign-in id? :old-hash hash? :new-hash hash? :until time?
                    :previous-until time? :at time?}
   :end-sign-in {:root id? :sign-in id? :at time?}
   :make-actor {:root id? :actor id? :made-by id? :label text? :at time?}
   :end-actor {:root id? :actor id? :at time?}
   :open-session {:root id? :sign-in id? :session id? :until time? :at time?}
   :close-session {:root id? :session id? :at time?}
   :ask {:code code? :hash hash? :asks text? :ends time? :at time?}
   :answer-ask {:code code? :yes? boolean? :root id? :actor id? :new-agent new-agent?
                :sign-in id? :until time? :at time?}})

(def ^:private optional-parts
  "The parts that a write's map may leave out: the `:driven-by` of
  `open-session`, which, absent, is not said."
  {:open-session {:driven-by driven-by?}})

(def ^:private broken-forms
  "The forms of the broken copies that break a form, each under its key of
  `breaks`."
  {:long-label-fits (-> forms
                        (assoc-in [:begin-sign-in :label] (or-nil string?))
                        (assoc-in [:make-actor :label] string?))
   :provider-and-layer-unchecked (-> forms
                                     (assoc-in [:enter :account] #(account? % keyword?))
                                     (assoc-in [:enter :own-layer] any?))
   :capital-email-fits (-> forms
                           (assoc-in [:invite :claim] #(claim? % text?))
                           (assoc-in [:enter :email] (or-nil text?)))})

(defn- form-of
  "The form by which the input of the write `kind` is judged in the copy
  `broken` of the rules; nil for a keyword that is no write's. In the
  broken copy `:no-takes-any-parts`, the parts of a no that it does not
  judge may be anything."
  [kind input broken]
  (let [form (get (get broken-forms broken forms) kind)]
    (if (and form (= broken :no-takes-any-parts) (= kind :answer-ask) (map? input) (false? (get input :yes?)))
      (merge form {:actor any? :sign-in any? :new-agent any? :until any?})
      form)))

(defn- loosened
  "The broken copy `:loose-form`: the input with each part of `form` that it
  leaves out read as nil, and each part that neither `form` nor `optional`
  names dropped. Gives nil for an input that is no map."
  [input form optional]
  (when (map? input)
    (merge (zipmap (keys form) (repeat nil))
           (select-keys input (concat (keys form) (keys optional))))))

(defn- fitting
  "Gives the call when it is a vector of a write's keyword and a map of that
  write's form, as `[kind input]` with the input as it is judged, and nil
  when it does not fit. A test that throws, as the comparator of a sorted
  map can, counts as no fit."
  [call broken]
  (try
    (when (and (vector? call) (= 2 (count call)) (keyword? (nth call 0)))
      (let [[kind input] call
            form (form-of kind input broken)
            optional (get optional-parts kind {})
            input (if (and form (= broken :loose-form)) (loosened input form optional) input)]
        (when (and form (shaped? input form optional))
          [kind input])))
    (catch Throwable _ nil)))

;; ------------------------------------------------------------------- the state

(def empty-state
  "The state of an Auth into which nothing has been written."
  {:persons {} :claims {} :links {} :roots {} :asks {}})

(def ^:private empty-root
  "What the root of a person holds when she has just entered."
  {:agents {} :agent-order [] :sign-ins {} :sign-in-order [] :sessions {}})

(defn- state?
  "Whether `x` has the keys of a state. A map whose comparator throws has
  not."
  [x]
  (try
    (and (map? x) (every? #(contains? x %) (keys empty-state)))
    (catch Throwable _ false)))

(defn- entered?
  "Whether the person `P` has entered: an account is linked to her."
  [state P]
  (some? (get-in state [:persons P :account])))

(defn- actor-of
  "The actor `A` of the root `P`: `{:person? true :depth 0}` for the person
  herself once she has entered, the record of her agent `A`, or nil."
  [state P A]
  (if (= A P)
    (when (entered? state P) {:person? true :depth 0})
    (get-in state [:roots P :agents A])))

(defn- actor-live?
  "Whether the actor `A` of the root `P` is there, has not ended, and has no
  actor above it that has ended. Walks up who made whom, which is 32 deep
  at most."
  [state P A]
  (loop [A A
         steps 0]
    (let [a (actor-of state P A)]
      (cond (nil? a) false
            (:person? a) true
            (:ended? a) false
            (> steps deepest) false
            :else (recur (:made-by a) (inc steps))))))

(defn- passed?
  "Whether a thing whose end is `end` has passed at the time `at`: a thing
  is live while the time that it is held against is before its end."
  [end at broken]
  (if (= broken :edge-is-after-the-end) (< end at) (<= end at)))

(defn- sign-in-time
  "The time of the sign-in `g`: its present secret's `:until` alone."
  [g broken]
  (if (= broken :time-of-the-older-secret)
    (max (:until g) (or (get-in g [:previous :until]) Long/MIN_VALUE))
    (:until g)))

(defn- sign-in-live?
  "Whether the sign-in `g` of the root `P` is live at the time `at`: it is
  there, it has not ended, its time has not passed, and its actor is live."
  [state P g at broken]
  (boolean (and g
                (not (:ended? g))
                (not (passed? (sign-in-time g broken) at broken))
                (actor-live? state P (:actor g)))))

(defn- end-sign-ins
  "Ends the sign-ins `Gs` (a set) of the root `P`, which must be there, as
  `end-sign-in` does, and closes every session under them unless `close?`
  is false."
  [state P Gs close?]
  (update-in state [:roots P]
             (fn [r]
               (cond-> (update r :sign-ins (fn [gs] (reduce (fn [gs G] (assoc-in gs [G :ended?] true)) gs Gs)))
                 close? (update :sessions
                                (fn [ss]
                                  (into {} (map (fn [[S s]] [S (if (contains? Gs (:sign-in s)) (assoc s :closed? true) s)]))
                                        ss)))))))

(defn take-out
  "Takes out of what is live under the root `P` every sign-in and every
  session whose time is at or before `at`. A sign-in so taken out is ended,
  as by `end-sign-in`, with its sessions; a session so taken out is closed,
  as by `close-session`. Neither is taken back. A root that is not there is
  left as it is. `step` does this first for each write that names a root,
  whether the write is then taken, is a repeat, or is refused; the proof
  uses it to say what a repeat or a refusal may change. The arity with
  `broken` is for the broken copies."
  ([state P at] (take-out state P at nil))
  ([state P at broken]
   (if-let [r (get-in state [:roots P])]
     (let [Gs (into #{} (keep (fn [[G g]] (when (and (not (:ended? g)) (passed? (sign-in-time g broken) at broken)) G)))
                    (:sign-ins r))
           Ss (into #{} (keep (fn [[S s]] (when (and (not (:closed? s)) (passed? (:until s) at broken)) S)))
                    (:sessions r))]
       (-> state
           (end-sign-ins P Gs true)
           (update-in [:roots P :sessions] (fn [ss] (reduce (fn [ss S] (assoc-in ss [S :closed?] true)) ss Ss)))))
     state)))

;; ------------------------------------------------------------------ the writes

(defn- refuse
  "Gives the state as it is and a refusal with `word`."
  [state word]
  [state {:refused word}])

(defn- write-invite
  "invite: makes the person wait under the claim. A claim that names this
  person already is a repeat, also after she has entered; a claim that
  names another person is `:claim-in-use`; a person under another claim is
  `:person-in-use`. `:invited-by` is kept as given."
  [state {:keys [person claim invited-by at]} broken]
  (let [named (get-in state [:claims claim])
        has (get-in state [:persons person :claim])]
    (cond
      (= named person) [state {:invited person}]
      (and (some? named) (not (and (= broken :used-claim-names-another) (entered? state named))))
      (refuse state :claim-in-use)
      (and (some? has) (not= broken :second-claim-taken)) (refuse state :person-in-use)
      :else [(-> state
                 (assoc-in [:claims claim] person)
                 (update-in [:persons person] assoc :claim claim :invited-by invited-by :invited-at at))
             {:invited person}])))

(defn- entered
  "What an enter of an account that is linked to the person `P` gives: her
  id, the own layer and the `:making` that her first enter fixed, and
  whether her layer is marked as made, as it stands at that enter (Sid's
  call of 22 November)."
  [state P]
  (let [p (get-in state [:persons P])]
    {:person P :own-layer (:own-layer p) :making (:making p) :own-layer-made? (boolean (:own-layer-made? p))}))

(defn- write-enter
  "enter: an account that has a link gives its person, whatever claim came
  with it, with the own layer and the `:making` that were fixed, and
  `:own-layer-made?` as it stands. One that has none takes the person of
  the claim that it proves, the code's when `:code-hash` names an
  invitation that waits, else the email's: the link is written, the claim
  is used up, and the person becomes an actor, her own root, with the
  offered own layer and `:making` fixed. One that proves no claim is
  `:not-invited`, and nothing of it is kept. The link keeps the `:says` of
  the enter with the latest `:at`, with that `:at`: an enter whose `:at`
  is not later leaves it. Nothing remembers what an enter gave (Sid's call
  of 22 November). The broken copy `:enter-repeats-what-it-gave` keeps
  that memory, under `:decided`, as the reading that the call set aside
  did."
  [state {:keys [account says email code-hash own-layer making at] :as input} broken]
  (let [link (get-in state [:links account])
        waits (fn [claim]
                (when-let [P (get-in state [:claims claim])]
                  (when-not (entered? state P) P)))
        proven (or (when code-hash (waits {:by :code :hash code-hash}))
                   (when email (waits {:by :email :email email})))
        remembering? (= broken :enter-repeats-what-it-gave)]
    (cond
      (and link (not (and proven (= broken :link-moves))))
      (let [state' (if (or (> at (:said-at link)) (= broken :says-of-every-enter))
                     (update-in state [:links account] assoc :says says :said-at at)
                     state)
            result (or (when remembering? (get-in state' [:decided account input]))
                       (entered state' (:person link)))]
        [(cond-> state' remembering? (assoc-in [:decided account input] result)) result])
      proven
      (let [state' (-> state
                       (assoc-in [:links account] {:person proven :says says :said-at at})
                       (update-in [:persons proven] assoc :account account :own-layer own-layer
                                  :making making :own-layer-made? false :entered-at at)
                       (update-in [:roots proven] #(or % empty-root)))
            result (entered state' proven)]
        [(cond-> state' remembering? (assoc-in [:decided account input] result)) result])
      :else (refuse state :not-invited))))

(defn- write-own-layer-made
  "own-layer-made: marks the own layer of a person who has entered as made.
  A person who waits, or no person, is `:no-such-person`."
  [state {:keys [person]} _]
  (if (entered? state person)
    [(assoc-in state [:persons person :own-layer-made?] true) {:made person}]
    (refuse state :no-such-person)))

(defn- begin
  "Begins the sign-in `G` of the root `P` from `g`, which holds its actor,
  kind, hash, until and label, at the time `at`, last in the order in which
  the root's sign-ins began. A sign-in whose until is at or before `at` is
  taken and is never live: it begins ended."
  [state P G g at broken]
  (-> state
      (assoc-in [:roots P :sign-ins G]
                (assoc g :first-hash (:hash g) :previous nil :begun at
                       :ended? (and (passed? (:until g) at broken) (not= broken :born-live))))
      (update-in [:roots P :sign-in-order] conj G)))

(defn- write-begin-sign-in
  "begin-sign-in: binds the sign-in id, once, to the actor, the kind and the
  hash. The same id with the same actor, kind and hash (the hash it began
  with) is a repeat, whose other parts are dropped. Else an actor that is
  not of this root is `:no-such-actor`, one that is not live is `:ended`,
  and an id that is another sign-in already is `:in-use`."
  [state {:keys [root actor sign-in kind hash until label at]} broken]
  (let [g (get-in state [:roots root :sign-ins sign-in])]
    (cond
      (and g (= actor (:actor g)) (= kind (:kind g)) (= hash (:first-hash g)))
      (case broken
        :ended-sign-in-begins-again
        [(if (:ended? g) (update-in state [:roots root :sign-ins sign-in] assoc :ended? false :until until) state)
         {:begun sign-in}]
        :repeat-keeps-its-parts
        [(update-in state [:roots root :sign-ins sign-in] assoc :until until :label label) {:begun sign-in}]
        [state {:begun sign-in}])
      (nil? (actor-of state root actor)) (refuse state :no-such-actor)
      (not (actor-live? state root actor)) (refuse state :ended)
      (some? g) (refuse state :in-use)
      :else [(begin state root sign-in {:actor actor :kind kind :hash hash :until until :label label} at broken)
             {:begun sign-in}])))

(defn- write-replace-secret
  "replace-secret: the sign-in's secret becomes the new one, with a new
  until, and the old one stands until `:previous-until`, in place of any
  secret before it. The same replacement again (the new hash is the hash
  now and the old one is the one before it) is a repeat. Else a sign-in
  that is not live is `:not-live`, and an old hash that is not the hash now
  is `:wrong-secret`. A replacement is judged by the present hash and the
  one before it alone (Sid's call of 22 November): one that arrives after
  a newer one is refused, and one whose hash has come back is taken when
  its old hash is the hash now. No session changes. A new until at or
  before `:at` takes the sign-in out at once, with its sessions
  (QUESTIONS.md, 10). The broken copy `:replacements-remembered` keeps, in
  `:replaced`, each replacement that the sign-in took, as the reading that
  the call set aside did."
  [state {:keys [root sign-in old-hash new-hash until previous-until at]} broken]
  (let [g (get-in state [:roots root :sign-ins sign-in])
        remembering? (= broken :replacements-remembered)]
    (cond
      (and g
           (or (and (= new-hash (:hash g)) (= old-hash (get-in g [:previous :hash])))
               (and (= broken :late-replacement-is-a-repeat) (= new-hash (get-in g [:previous :hash])))))
      [state {:replaced sign-in}]
      (not (sign-in-live? state root g at broken)) (refuse state :not-live)
      (or (not= old-hash (:hash g)) (and remembering? (contains? (:replaced g) [old-hash new-hash])))
      (refuse state :wrong-secret)
      :else
      (let [g' (cond-> (assoc g :hash new-hash :until until :previous {:hash old-hash :until previous-until})
                 remembering? (update :replaced (fnil conj #{}) [old-hash new-hash]))
            state' (assoc-in state [:roots root :sign-ins sign-in] g')]
        [(if (passed? (sign-in-time g' broken) at broken) (end-sign-ins state' root #{sign-in} true) state')
         {:replaced sign-in}]))))

(defn- write-end-sign-in
  "end-sign-in: the sign-in ends, and every session under it is closed.
  Ending it again changes nothing. A sign-in that is not of this root is
  `:no-such-sign-in`."
  [state {:keys [root sign-in]} broken]
  (let [g (get-in state [:roots root :sign-ins sign-in])]
    (cond
      (nil? g) (refuse state :no-such-sign-in)
      (:ended? g) [state {:ended sign-in}]
      :else [(end-sign-ins state root #{sign-in} (not= broken :end-leaves-sessions-open)) {:ended sign-in}])))

(defn- make-agent
  "Makes the agent `A` of the root `P`, made by `B` with `label` at the time
  `at`, `depth` makers below the root, last in the order of the root's
  agents."
  [state P A B label at depth]
  (-> state
      (assoc-in [:roots P :agents A] {:made-by B :label label :made-at at :depth depth :ended? false})
      (update-in [:roots P :agent-order] conj A)))

(defn- write-make-actor
  "make-actor: makes the agent under the root, made by its live actor. The
  same id with the same maker is a repeat, whose label is dropped. Else a
  maker that is not of this root is `:no-such-maker`, one that is not live
  is `:ended`, an id that is an actor of the root already (the person's
  own among them) is `:in-use`, and a maker 32 below its root is
  `:too-deep`."
  [state {:keys [root actor made-by label at]} broken]
  (let [a (get-in state [:roots root :agents actor])
        maker (actor-of state root made-by)]
    (cond
      (and a (= made-by (:made-by a))) [state {:made actor}]
      (nil? maker) (refuse state :no-such-maker)
      (not (actor-live? state root made-by)) (refuse state :ended)
      (some? (actor-of state root actor)) (refuse state :in-use)
      (>= (:depth maker) deepest) (refuse state :too-deep)
      :else [(make-agent state root actor made-by label at (inc (:depth maker))) {:made actor}])))

(defn- beneath
  "The agent `A` of the root record `r` and every agent that it made, in any
  depth; in the broken copy `:end-reaches-one-down`, only those that it
  made itself."
  [r A broken]
  (if (= broken :end-reaches-one-down)
    (into #{A} (keep (fn [[B b]] (when (= A (:made-by b)) B))) (:agents r))
    (loop [found #{A}]
      (let [more (into found (keep (fn [[B b]] (when (contains? found (:made-by b)) B))) (:agents r))]
        (if (= more found) found (recur more))))))

(defn- write-end-actor
  "end-actor: the agent ends, with every actor that it made, in any depth,
  and every sign-in and session of each. An agent that has ended already,
  by this call or with one above it, gives the same and nothing changes
  (QUESTIONS.md, 16). An actor that is not of this root is `:no-such-actor`;
  the person is `:is-a-person`."
  [state {:keys [root actor]} broken]
  (let [a (actor-of state root actor)]
    (cond
      (and a (:ended? a)) [state {:ended actor}]
      (nil? a) (refuse state :no-such-actor)
      (:person? a) (refuse state :is-a-person)
      :else
      (let [r (get-in state [:roots root])
            As (beneath r actor broken)
            Gs (into #{} (keep (fn [[G g]] (when (contains? As (:actor g)) G))) (:sign-ins r))]
        [(-> state
             (update-in [:roots root :agents] (fn [as] (reduce (fn [as B] (assoc-in as [B :ended?] true)) as As)))
             (end-sign-ins root Gs (not= broken :end-actor-leaves-sessions-open)))
         {:ended actor}]))))

(defn- write-open-session
  "open-session: opens the session under a live sign-in, with its until and
  its `:driven-by`, nil when not said. The same id under the same sign-in is
  a repeat: while the session is open it moves its until to the one that
  came, when that is later, and changes nothing else; a session that is
  closed is not opened again. Else a sign-in that is not live is
  `:not-live`, and an id that is a session of another sign-in, open or
  not, is `:in-use`. A session whose until is at or before `:at` is taken
  and is never live: it opens closed."
  [state {:keys [root sign-in session until at] :as input} broken]
  (let [s (get-in state [:roots root :sessions session])
        g (get-in state [:roots root :sign-ins sign-in])]
    (cond
      (and s (= sign-in (:sign-in s)))
      [(cond
         (and (:closed? s) (= broken :closed-session-opens-again))
         (update-in state [:roots root :sessions session] assoc :closed? false :until (max (:until s) until))
         (:closed? s) state
         :else (update-in state [:roots root :sessions session]
                          (fn [s] (cond-> (assoc s :until (max (:until s) until))
                                    (= broken :repeat-sets-driven-by) (assoc :driven-by (:driven-by input))))))
       {:open session}]
      (not (sign-in-live? state root g at broken)) (refuse state :not-live)
      (some? s) (refuse state :in-use)
      :else [(assoc-in state [:roots root :sessions session]
                       {:sign-in sign-in :until until :driven-by (:driven-by input) :opened-at at
                        :closed? (and (passed? until at broken) (not= broken :born-live))})
             {:open session}])))

(defn- write-close-session
  "close-session: the session is closed, once; closing it again changes
  nothing. A session that is not of this root is `:no-such-session`."
  [state {:keys [root session]} _]
  (let [s (get-in state [:roots root :sessions session])]
    (cond (nil? s) (refuse state :no-such-session)
          (:closed? s) [state {:closed session}]
          :else [(assoc-in state [:roots root :sessions session :closed?] true) {:closed session}])))

(defn- write-ask
  "ask: binds the code, for good, to the hash of the program's secret. The
  same code with the same hash is a repeat, whose other parts are dropped;
  with another hash it is `:in-use`, also when the ask is over. An ask whose
  end is at or before `:at` is taken and is never live: it is over from
  its making."
  [state {:keys [code hash asks ends at]} broken]
  (let [k (get-in state [:asks code])]
    (cond
      (and k (= hash (:hash k))) [state {:asked code}]
      (and k (not= broken :bound-code-asks-again)) (refuse state :in-use)
      :else [(assoc-in state [:asks code] {:hash hash :asks asks :ends ends :asked-at at
                                           :over-when-made? (passed? ends at broken)
                                           :state :waiting :answer nil})
             {:asked code}])))

(defn- over?
  "Whether an answer at the time `at` comes too late for the ask `k`: at or
  after its `:ends`, or the ask was over when it was made."
  [k at broken]
  (or (:over-when-made? k)
      (if (= broken :answer-at-the-end-taken) (> at (:ends k)) (>= at (:ends k)))))

(defn- answered?
  "Whether the ask `k` has an answer: a yes or a no stands on it."
  [k]
  (not= :waiting (:state k)))

(defn- what-stands
  "The result of the answer that stands on the ask `k` under the code `U`."
  [k U]
  (if (= :yes (:state k)) {:yes (:answer k)} {:no U}))

(defn- as-it-would-stand
  "What the answer `input` would give under the code `U`, were it the one
  that stands. The broken copy `:later-answer-stands` gives what stands to
  an answer that would give the same, and takes any other."
  [{:keys [yes? root actor sign-in]} U]
  (if yes? {:yes {:root root :actor actor :sign-in sign-in}} {:no U}))

(defn- same-answer?
  "The broken copy `:answered-ask-judged-first`, which keeps under
  `:answered` the parts of the answer that stands, as the reading that Sid's
  call of 22 November set aside did: whether `input` is that answer, by the
  same yes or no, the same root, and for a yes the same actor and sign-in,
  with a new agent or with none as that one."
  [k {:keys [yes? root actor new-agent sign-in]}]
  (let [given (:answered k)]
    (boolean (and given
                  (= yes? (:yes? given))
                  (= root (:root given))
                  (or (not yes?)
                      (and (= actor (:actor given))
                           (= sign-in (:sign-in given))
                           (= (some? new-agent) (:new-agent? given))))))))

(defn- answer-refusal
  "The refusal that applies to the answer `input` after `:no-such-ask` and
  `:over`, or nil. A no needs a person who has entered, and judges no other
  part. A yes needs its actor, or the person who makes the new agent, of
  the root and live; and a new agent's id and the sign-in's id that are no
  other's of the root."
  [state {:keys [yes? root actor new-agent sign-in]}]
  (let [sign-in-taken? (some? (get-in state [:roots root :sign-ins sign-in]))]
    (cond
      (not yes?) (when-not (entered? state root) :no-such-actor)
      (some? new-agent) (cond (not (entered? state root)) :no-such-actor
                              (some? (actor-of state root actor)) :in-use
                              sign-in-taken? :in-use)
      :else (cond (nil? (actor-of state root actor)) :no-such-actor
                  (not (actor-live? state root actor)) :ended
                  sign-in-taken? :in-use))))

(defn- write-answer-ask
  "answer-ask: a person answers an ask once. An ask that is not there is
  `:no-such-ask`. Once an ask has an answer, every answer gives what
  stands, before any refusal, and changes nothing (Sid's call of 22
  November). An answer to an ask that waits is `:over` at or after the
  ask's `:ends`, or when the ask was over when it was made, and then come
  the refusals of `answer-refusal`. A yes makes the new agent when there is
  one, made by the person, and begins a sign-in of kind `:program` for the
  actor, with the ask's hash and the ask's `:asks` as its label; a no
  refuses the ask for good."
  [state {:keys [code yes? root actor new-agent sign-in until at] :as input} broken]
  (let [k (get-in state [:asks code])
        judged-first? (= broken :answered-ask-judged-first)]
    (cond
      (nil? k) (refuse state :no-such-ask)
      (and (answered? k)
           (case broken
             :later-answer-stands (= (what-stands k code) (as-it-would-stand input code))
             :answered-ask-judged-first (same-answer? k input)
             true))
      [state (what-stands k code)]
      (over? k at broken) (refuse state :over)
      :else
      (if-let [word (answer-refusal state input)]
        (refuse state word)
        (cond
          (and (answered? k) judged-first?) [state (what-stands k code)]
          yes? (let [answer {:root root :actor actor :sign-in sign-in}]
                 [(-> (if (some? new-agent) (make-agent state root actor root (:label new-agent) at 1) state)
                      (begin root sign-in {:actor actor :kind :program :hash (:hash k) :until until :label (:asks k)}
                             at broken)
                      (assoc-in [:asks code]
                                (cond-> (assoc k :state :yes :answer answer)
                                  judged-first? (assoc :answered (assoc answer :yes? true :new-agent? (some? new-agent))))))
                  {:yes answer}])
          :else [(assoc-in state [:asks code]
                           (cond-> (assoc k :state :no :answer nil)
                             judged-first? (assoc :answered {:yes? false :root root})))
                 {:no code}])))))

(def ^:private writes
  "Each write's keyword, with the function that judges it."
  {:invite write-invite
   :enter write-enter
   :own-layer-made write-own-layer-made
   :begin-sign-in write-begin-sign-in
   :replace-secret write-replace-secret
   :end-sign-in write-end-sign-in
   :make-actor write-make-actor
   :end-actor write-end-actor
   :open-session write-open-session
   :close-session write-close-session
   :ask write-ask
   :answer-ask write-answer-ask})

(def names-a-root
  "The writes that name a `:root`, and so take out what has passed under it.
  `invite`, `enter`, `own-layer-made` and `ask` name none."
  #{:begin-sign-in :replace-secret :end-sign-in :make-actor :end-actor
    :open-session :close-session :answer-ask})

(def breaks
  "The broken copies of the rules. Each key, given to `step`, breaks one
  rule, and its value says how. The last eight take, each, a reading that
  Sid's calls of 22 November set aside or let stand against another; a
  module built on that reading is one of them."
  {:link-moves
   "an enter of an account that has a link, which proves another invitation that waits, moves the link to that invitation's person"
   :used-claim-names-another
   "a claim that an account has used up is bound to another person by a new invite"
   :second-claim-taken
   "a person who has a claim is invited under another, which takes the place of the first"
   :refusal-takes-nothing-out
   "a write that is refused takes nothing out of what is live"
   :edge-is-after-the-end
   "a thing whose end is the time of a write is still live at that write"
   :time-of-the-older-secret
   "a sign-in's time is the later of its present secret's and the secret's before it"
   :born-live
   "a sign-in or a session whose end is at or before the time of the event that makes it is live"
   :ended-sign-in-begins-again
   "a repeat of begin-sign-in makes an ended sign-in live again"
   :closed-session-opens-again
   "a repeat of open-session opens a closed session again"
   :repeat-keeps-its-parts
   "a repeat of begin-sign-in takes its own until and label in place of the first ones"
   :repeat-sets-driven-by
   "a repeat of open-session takes its own driven-by"
   :late-replacement-is-a-repeat
   "a replacement that arrives after a newer one is taken as a repeat"
   :end-leaves-sessions-open
   "end-sign-in ends the sign-in and leaves its sessions open"
   :end-reaches-one-down
   "end-actor ends the agent and the agents that it made itself, and none further down"
   :end-actor-leaves-sessions-open
   "end-actor ends the actors and their sign-ins, and leaves the sessions of those open"
   :later-answer-stands
   "a later answer to an ask takes the place of the answer that stands"
   :answer-at-the-end-taken
   "an answer whose time is the ask's end is taken"
   :bound-code-asks-again
   "an ask under a code that is bound to another hash takes the code"
   :unfit-input-takes-out
   "an input that does not fit, but names a root and a time, takes out what has passed under that root"
   :long-label-fits
   "a label of more than 256 characters fits"
   :loose-form
   "a part that the contract names may be left out, and reads as nil, and a part that it does not name is dropped"
   :provider-and-layer-unchecked
   "a provider's name of more than 256 characters, and an :own-layer that is no UUID, fit"
   :capital-email-fits
   "an email with a capital letter fits, and is compared as given"
   :no-takes-any-parts
   "the parts of a no that it does not judge may be anything"
   :enter-repeats-what-it-gave
   "an enter that is a copy of one that its link decided gives what that one gave, its :own-layer-made? too"
   :says-of-every-enter
   "every enter of an account that has a link keeps its own :says, whatever its :at"
   :answered-ask-judged-first
   "an answer to an ask that has an answer, unless it is the same answer, meets the refusals first, :over among them, and only then is told what stands"
   :replacements-remembered
   "a sign-in keeps each replacement that it took, and refuses one that it took before, also when that one's old hash has come back and is the hash now"})

(defn- take-out-for-unfit
  "The broken copy `:unfit-input-takes-out`: an input that does not fit, but
  names a root and a time, takes out what has passed under that root."
  [state call]
  (try
    (let [input (when (and (vector? call) (= 2 (count call))) (nth call 1))]
      (if (and (map? input) (id? (get input :root)) (time? (get input :at)))
        (take-out state (:root input) (:at input))
        state))
    (catch Throwable _ state)))

(defn step
  "Takes a state and a call, `[write-keyword input-map]`, and gives
  `[state' result]`: the state after the write, and what the write gives.
  An input that does not fit gives `{:could-not :read-the-event}` and the
  state as it was. A write that names a root takes out, under that root,
  what has passed at its `:at` before it is judged (`take-out`); then a
  repeat is recognised first, and the refusals come in the order in which
  the contract lists them. It reads no clock and throws for no input.
  `broken`, a key of `breaks` or nil, breaks one rule."
  ([state call] (step state call nil))
  ([state call broken]
   (if-let [[kind input] (fitting call broken)]
     (let [swept (if (contains? names-a-root kind) (take-out state (:root input) (:at input) broken) state)
           [state' result] ((get writes kind) swept input broken)]
       [(if (and (contains? result :refused) (= broken :refusal-takes-nothing-out)) state state') result])
     [(if (= broken :unfit-input-takes-out) (take-out-for-unfit state call) state)
      {:could-not :read-the-event}])))

;; ------------------------------------------------------------------- the reads

(defn- who-is-in
  "who-is of a state: the sign-in of the root, with its standing, the
  session asked for, which can be of another sign-in, with its
  `:driven-by`, and the root person's own layer. Ended and closed things
  are given too."
  [state query]
  (if-not (try (shaped? query {:root id? :sign-in id? :session (or-nil id?)} {})
               (catch Throwable _ false))
    {:could-not :read-the-event}
    (let [{:keys [root sign-in session]} query
          g (get-in state [:roots root :sign-ins sign-in])
          s (when session (get-in state [:roots root :sessions session]))
          p (when (entered? state root) (get-in state [:persons root]))]
      {:sign-in (when g
                  {:actor (:actor g) :kind (:kind g) :hash (:hash g) :until (:until g)
                   :previous (:previous g) :ended? (boolean (:ended? g))
                   :actor-ended? (not (actor-live? state root (:actor g)))})
       :session (when s
                  {:sign-in (:sign-in s) :until (:until s) :closed? (boolean (:closed? s))
                   :driven-by (:driven-by s)})
       :own-layer (:own-layer p)
       :own-layer-made? (boolean (:own-layer-made? p))})))

(defn- ask-of-in
  "ask-of of a state: the ask under the code, or nil. It judges no time."
  [state code]
  (if-not (code? code)
    {:could-not :read-the-event}
    (when-let [k (get-in state [:asks code])]
      {:hash (:hash k) :asks (:asks k) :ends (:ends k) :state (:state k) :answer (:answer k)})))

(defn- live-sign-ins
  "The sign-ins of the root `P` that are live as the state holds them: not
  ended, and their actor live. Gives `[id record]` pairs in the order in
  which they began."
  [state P]
  (let [r (get-in state [:roots P])]
    (keep (fn [G] (let [g (get-in r [:sign-ins G])]
                    (when (and (not (:ended? g)) (actor-live? state P (:actor g))) [G g])))
          (:sign-in-order r))))

(defn- under-in
  "under of a state: the agents of the root, ended ones too, in the order in
  which they were made, and its live sign-ins, in the order in which they
  began."
  [state P]
  (if-not (id? P)
    {:could-not :read-the-event}
    (let [r (get-in state [:roots P])]
      {:actors (mapv (fn [A] (let [a (get-in r [:agents A])]
                               {:actor A :made-by (:made-by a) :label (:label a) :ended? (boolean (:ended? a))}))
                     (:agent-order r))
       :sign-ins (mapv (fn [[G g]] {:sign-in G :actor (:actor g) :kind (:kind g) :label (:label g)
                                    :begun (:begun g) :until (:until g)})
                       (live-sign-ins state P))})))

(defn- live-under-in
  "What is live under the root `P` in a state: its live sign-ins and its
  open sessions, with no `:driven-by`."
  [state P]
  (if-not (id? P)
    {:could-not :read-the-event}
    {:sign-ins (into {} (map (fn [[G g]] [G (select-keys g [:actor :kind :hash :until :previous])]))
                     (live-sign-ins state P))
     :sessions (into {} (keep (fn [[S s]] (when-not (:closed? s) [S {:sign-in (:sign-in s) :until (:until s)}])))
                     (get-in state [:roots P :sessions]))}))

;; ------------------------------------------------------------------ the client

(deftype Cluster [modules])

(deftype Client [place id])

(defn- new-place
  "The atom of one Auth with nothing written in it, whose writes are judged
  by the copy `broken` of the rules, nil for the rules as they are."
  [broken]
  (atom {:state empty-state :watches {} :queue [] :closed #{} :next-id 1 :broken broken}))

(defn cluster
  "Gives a thing that stands for a cluster in which Auth is launched under
  each of `names`, each a string and each an Auth of its own with nothing
  written in it. It is what a program that stands on the reference gives
  `open` in the place of a Rama cluster."
  [& names]
  (Cluster. (into {} (map (fn [n] [n (new-place nil)])) (filter string? names))))

(defn broken-cluster
  "As `cluster`, but each Auth judges its writes by the copy of the rules in
  which the rule `broken`, a key of `breaks`, is broken: for the proofs
  that must catch it."
  [broken & names]
  (Cluster. (into {} (map (fn [n] [n (new-place broken)])) (filter string? names))))

(defn- client?
  "Whether `x` is a client of the reference."
  [x]
  (instance? Client x))

(defn- closed?
  "Whether the client `c` is closed in the place's value `w`."
  [w ^Client c]
  (contains? (:closed w) (.-id c)))

(defn- read-with
  "Runs `f` on the state that `x` stands for: a client's present state, or
  `x` itself when it is a state. A closed client gives
  `{:could-not :closed}`, and a thing that is neither gives
  `{:could-not :read-the-event}`. A throw, which would be a fault of the
  reference, gives `{:could-not :reach-the-module :threw t}`."
  [x f]
  (try
    (cond (client? x) (let [w @(.-place ^Client x)]
                        (if (closed? w x) {:could-not :closed} (f (:state w))))
          (state? x) (f x)
          :else {:could-not :read-the-event})
    (catch Throwable t {:could-not :reach-the-module :threw t})))

(defn- watch-calls
  "The calls that the watches in the place's value `w` are owed when the
  state went from `before` to `after`: for each watched root whose live
  value changed, the id of each of its watches with the new value, in the
  order in which the watches were registered."
  [w before after]
  (let [roots (into #{} (map :root) (vals (:watches w)))
        changed (into {} (keep (fn [P] (let [v (live-under-in after P)]
                                         (when (not= v (live-under-in before P)) [P v]))))
                      roots)]
    (vec (for [[watch-id {:keys [root]}] (sort-by key (:watches w))
               :let [v (get changed root)]
               :when v]
           {:watch watch-id :value v}))))

(def ^:private ^:dynamic *delivering*
  "The places whose owed calls this thread is handing out now."
  #{})

(defn- deliver!
  "Hands out the calls that the place's queue holds, first in first out,
  dropping what a watching function throws. A watch is looked up just
  before its call, so that one stopped, or of a client closed, by an
  earlier call is not called. A write that a watching function makes puts
  its calls at the queue's end and returns: the calls of each change come
  in the order of the changes. Must run under the place's lock."
  [place]
  (when-not (contains? *delivering* place)
    (binding [*delivering* (conj *delivering* place)]
      (loop []
        (let [w @place]
          (when-let [{:keys [watch value]} (first (:queue w))]
            (reset! place (update w :queue (comp vec rest)))
            (when-let [f (get-in w [:watches watch :f])]
              (try (f value) (catch Throwable _ nil)))
            (recur)))))))

(defn- write!
  "Makes the write `kind` with `input` through the client: under the
  place's lock, steps the state, keeps it with the calls that the watches
  are owed, and hands those out before it returns. A closed client gives
  `{:could-not :closed}`; a thing that is no client gives
  `{:could-not :read-the-event}`."
  [client kind input]
  (if-not (client? client)
    {:could-not :read-the-event}
    (let [place (.-place ^Client client)]
      (try
        (locking place
          (let [w @place]
            (if (closed? w client)
              {:could-not :closed}
              (let [[state' result] (step (:state w) [kind input] (:broken w))]
                (reset! place (-> w
                                  (assoc :state state')
                                  (update :queue into (watch-calls w (:state w) state'))))
                (deliver! place)
                result))))
        (catch Throwable t {:could-not :reach-the-module :threw t})))))

(defn open
  "Takes what stands for the cluster, a thing that `cluster` gave, and a
  module's name, a string. Gives a client of the Auth that the cluster
  holds under that name: every client opened under one name shares that
  Auth, as copies of the runtime do. A name under which the cluster holds
  no module, and anything in the place of the cluster that is not one,
  give `{:could-not :find-the-module}`."
  [cluster module-name]
  (try
    (if-let [place (and (instance? Cluster cluster) (string? module-name)
                        (get (.-modules ^Cluster cluster) module-name))]
      (locking place
        (let [id (:next-id @place)]
          (swap! place update :next-id inc)
          (Client. place id)))
      {:could-not :find-the-module})
    (catch Throwable t {:could-not :reach-the-module :threw t})))

(defn close!
  "Closes the client: every later call on it gives `{:could-not :closed}`,
  and no function that it watched with is called again. Other clients over
  the same atom go on. Gives nil, for any argument."
  [client]
  (when (client? client)
    (let [place (.-place ^Client client)
          id (.-id ^Client client)]
      (locking place
        (swap! place (fn [w] (-> w
                                 (update :closed conj id)
                                 (update :watches (fn [ws] (into {} (remove (fn [[_ v]] (= id (:client v)))) ws)))))))))
  nil)

(defn invite
  "invite through the client: `{:person P :claim C :invited-by B :at t}`."
  [client input]
  (write! client :invite input))

(defn enter
  "enter through the client: `{:account :says :email :code-hash :own-layer
  :making :at}`."
  [client input]
  (write! client :enter input))

(defn own-layer-made
  "own-layer-made through the client: `{:person P :at t}`."
  [client input]
  (write! client :own-layer-made input))

(defn begin-sign-in
  "begin-sign-in through the client: `{:root :actor :sign-in :kind :hash
  :until :label :at}`."
  [client input]
  (write! client :begin-sign-in input))

(defn replace-secret
  "replace-secret through the client: `{:root :sign-in :old-hash :new-hash
  :until :previous-until :at}`."
  [client input]
  (write! client :replace-secret input))

(defn end-sign-in
  "end-sign-in through the client: `{:root P :sign-in G :at t}`."
  [client input]
  (write! client :end-sign-in input))

(defn make-actor
  "make-actor through the client: `{:root :actor :made-by :label :at}`."
  [client input]
  (write! client :make-actor input))

(defn end-actor
  "end-actor through the client: `{:root P :actor A :at t}`."
  [client input]
  (write! client :end-actor input))

(defn open-session
  "open-session through the client: `{:root :sign-in :session :until :at}`,
  and an optional `:driven-by`."
  [client input]
  (write! client :open-session input))

(defn close-session
  "close-session through the client: `{:root P :session S :at t}`."
  [client input]
  (write! client :close-session input))

(defn ask
  "ask through the client: `{:code :hash :asks :ends :at}`."
  [client input]
  (write! client :ask input))

(defn answer-ask
  "answer-ask through the client: `{:code :yes? :root :actor :new-agent
  :sign-in :until :at}`."
  [client input]
  (write! client :answer-ask input))

(defn who-is
  "Takes a client or a state, and `{:root P :sign-in G :session S-or-nil}`.
  Gives `{:sign-in {:actor :kind :hash :until :previous :ended?
  :actor-ended?}-or-nil :session {:sign-in :until :closed? :driven-by}-or-nil
  :own-layer :own-layer-made?}`. It judges no time: the caller holds each
  time against its clock."
  [client-or-state query]
  (read-with client-or-state #(who-is-in % query)))

(defn ask-of
  "Takes a client or a state, and a code. Gives `{:hash :asks :ends :state
  :answer}`, where `:state` is `:waiting`, `:yes` or `:no` and `:answer` is
  `{:root :actor :sign-in}` for a yes and nil otherwise; or nil when no ask
  has the code. It judges no time."
  [client-or-state code]
  (read-with client-or-state #(ask-of-in % code)))

(defn under
  "Takes a client or a state, and a root. Gives `{:actors [{:actor :made-by
  :label :ended?} ...] :sign-ins [{:sign-in :actor :kind :label :begun
  :until} ...]}`: the agents of the root, ended ones too, in the order in
  which they were made, and its live sign-ins, in the order in which they
  began, as they stood at the root's latest event."
  [client-or-state P]
  (read-with client-or-state #(under-in % P)))

(defn live-under
  "Takes a client or a state, and a root. Gives what is live under the root,
  the value that `watch-root` hands to its function: `{:sign-ins {G {:actor
  :kind :hash :until :previous}} :sessions {S {:sign-in :until}}}`, the live
  sign-ins and the open sessions as they stood at the root's latest event."
  [client-or-state P]
  (read-with client-or-state #(live-under-in % P)))

(defn watch-root
  "Takes a client, a root and a function of one argument. From now on,
  each time what is live under the root changes, through this client or
  any other opened under the same name, calls the function with the new value of
  `live-under`, on the thread that made the write, before the write
  returns, in the order of the changes. No call is made at the
  registration, and this reference never hands
  `:softland.auth.client/not-reached`. Gives a function of no argument
  that stops the watching; on a closed client, `{:could-not :closed}` in
  its place. The function must be a function (`fn?`) (QUESTIONS.md, 27)."
  [client P f]
  (if-not (client? client)
    {:could-not :read-the-event}
    (let [place (.-place ^Client client)]
      (try
        (locking place
          (let [w @place]
            (cond
              (closed? w client) {:could-not :closed}
              (not (and (id? P) (fn? f))) {:could-not :read-the-event}
              :else (let [watch-id (:next-id w)]
                      (reset! place (-> w
                                        (update :next-id inc)
                                        (assoc-in [:watches watch-id] {:root P :f f :client (.-id ^Client client)})))
                      (fn stop []
                        (locking place (swap! place update :watches dissoc watch-id))
                        nil)))))
        (catch Throwable t {:could-not :reach-the-module :threw t})))))
