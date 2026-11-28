(ns softland.auth.schema
  "Typed records, key encoders, projections, and pure single-task transition
  functions for the Rama module `Auth`. Pure code.

  Every Rama depot record and PState value implements a closed marker interface
  (`IRootDatum`, `ILiveEntry`, `IAccountState`, `IClaimState`,
  `IInviteOpState`, `IAskState`, `IEnterOpState`, `IDrivenBy`) so no PState
  schema uses `Object` and no live watched record holds a byte array. The
  keys whose order a read uses are strictly 7-bit ASCII, so RocksDB UTF-8
  byte order matches JVM UTF-16 order identically: the composite keys of
  `$$root-data` (`\"<P>\\0...\"`), and the inner keys of a root's map in
  `$$live` (`\"g:<G>\"` for a sign-in, `\"s:<S>\"` for a session,
  `\"ends\"` for the count of ends). A person's accounts (`k-account`) end
  in an account's key, which holds the provider's subject as given: their
  range is bounded by an ASCII prefix, and `project-accounts` orders them
  itself.

  Two things are kept apart for a sign-in and a session. That someone ended
  it is a fact, written on its record (`ended?`, `closed?`) by `end-sign-in`,
  `close-session` and `end-actor` alone, and never taken back. That its time
  has passed is no fact of Auth's, which reads no clock: the record keeps
  `until`, each write that needs the thing live holds `until` against its
  own `:at` and keeps nothing of that, and each reader holds it against its
  own clock. `$$live` is what a root's watchers are pushed. A thing is in it
  from a write that gave it an `until` after that write's `:at`, until a
  write under its root whose `:at` that `until` is at or before, or until
  someone ends it. So it holds nothing that someone ended and nothing whose
  time had passed at the root's latest write; a thing that left it has not
  ended, and is not put back until a write gives it a time again.

  The pure transition functions for `enter` and `answer-ask` were first
  written from the step specification that was model-checked in
  `src/proposal/runtime-2026-11-21/spine/auth/agreement/`, step for step. They
  are not that code, and no model check has run them; since 28 November the
  steps of `enter`, `invite`, `take-back` and `unlink` are not even that
  specification (`softland.auth.module` says which checks hold them)."
  (:import [java.util UUID]))

;; ------------------------------------------------------- marker interfaces

(definterface IDrivenBy)
(definterface IRootDatum)
(definterface ILiveEntry)
(definterface IAccountState)
(definterface IClaimState)
(definterface IAskState)
(definterface IEnterOpState)
(definterface IRootEvent)
(definterface IAskEvent)
(definterface IInviteEvent)
(definterface IEnterEvent)
(definterface IInviteOpState)

;; ------------------------------------------------------------- driven-by

(defrecord DrivenByPerson [] IDrivenBy)
(defrecord DrivenByModel [model] IDrivenBy)

(defn to-driven-by
  [m]
  (when m
    (case (:kind m)
      :person (->DrivenByPerson)
      :model (->DrivenByModel (:model m)))))

(defn from-driven-by
  [d]
  (cond
    (nil? d) nil
    (instance? DrivenByPerson d) {:kind :person}
    (instance? DrivenByModel d) {:kind :model :model (:model d)}))

;; ----------------------------------------------------------- depot events

;; *invites (partitioned by :claim-key String). `claim-key` is `e:<hash>` or
;; `c:<hash>`: Auth is given the hash of an email, never the address.
(defrecord InviteEvent [op-id person claim-key invited-by-kind invited-by-id ends at] IInviteEvent)
(defrecord TakeBackEvent [claim-key person invite-op at] IInviteEvent)

;; *enters (partitioned by :account-key String)
(defrecord EnterEvent [op-id account-key provider subject says email-hash code-hash
                       own-lane making-uuid making-session at]
  IEnterEvent)
(defrecord UnlinkEvent [account-key link-op person at] IEnterEvent)

;; *asks (partitioned by :code String)
(defrecord AskEvent [code hash asking-hash asks ends at] IAskEvent)
(defrecord AnswerAskEvent [op-id code yes? root actor new-agent-label sign-in until at] IAskEvent)

;; *root-events (partitioned by :root-id UUID)
(defrecord OwnLaneMadeEvent [root-id at] IRootEvent)
(defrecord BeginSignInEvent [root-id actor sign-in kind hash until label at] IRootEvent)
(defrecord ReplaceSecretEvent [root-id sign-in old-hash new-hash until previous-until at] IRootEvent)
(defrecord EndSignInEvent [root-id sign-in at] IRootEvent)
(defrecord MakeActorEvent [root-id actor made-by label at] IRootEvent)
(defrecord EndActorEvent [root-id actor at] IRootEvent)
(defrecord OpenSessionEvent [root-id sign-in session until driven-by at] IRootEvent)
(defrecord CloseSessionEvent [root-id session at] IRootEvent)

;; ----------------------------------------------------------- PState records

;; $$root-data {String -> IRootDatum} (partitioned by |hash root-UUID)
;; A person's record appears at her first enter: a person who waits is a
;; claim that names her and nothing else. It holds who first invited her and
;; when, and no claim and no account: each account of hers is a
;; `PersonAccount` of its own, under `k-account`.
(defrecord RootEntered [invited-by-kind invited-by-id invited-at own-lane making-uuid making-session
                        own-lane-made? next-actor-seq next-sign-in-seq]
  IRootDatum)
;; `link-op` is the op of the enter that made the link: the link's id.
(defrecord PersonAccount [provider subject linked-at link-op]
  IRootDatum)
(defrecord ActorRec [actor made-by ends-with depth label ended? seq]
  IRootDatum)
(defrecord ActorOrderRec [actor made-by ends-with depth label ended? seq]
  IRootDatum)
(defrecord SignInRec [sign-in actor kind initial-hash hash until prev-hash prev-until
                      ended? begun label seq own-lane own-lane-made-at-begin?]
  IRootDatum)
(defrecord SessionRec [session sign-in until closed? driven-by]
  IRootDatum)
(defrecord ExpiryEntry [kind id parent-id until]
  IRootDatum)
(defrecord AskOutcomeYes [root actor sign-in]
  IRootDatum)
(defrecord AskOutcomeNo [code]
  IRootDatum)
(defrecord AskOutcomeRefused [reason]
  IRootDatum)

;; $$live {String -> ILiveEntry} (partitioned by |hash root-UUID)
(defrecord LiveSignIn [sign-in actor kind initial-hash hash until prev-hash prev-until
                       begun label seq own-lane own-lane-made-at-begin?]
  ILiveEntry)
;; `actor` is the actor of the session's sign-in, which never changes: by it
;; `end-actor` finds the watched sessions of an agent whose sign-in is not
;; watched.
(defrecord LiveSession [session sign-in actor until driven-by]
  ILiveEntry)
;; How many writes under the root ended something. A watcher can hold a
;; thing that is not in the root's map, so an end of such a thing would
;; change nothing that is pushed: this makes every end a push.
(defrecord LiveEnds [n]
  ILiveEntry)

;; $$accounts {String -> IAccountState} (partitioned by |hash account-key)
;; `round` is raised at each reset to `:probing`; a step of a chain that
;; left in an earlier round changes no status.
(defrecord AccountLinking [op-id account-key provider subject code-claim-key email-claim-key
                           own-lane making-uuid making-session says at status chosen round]
  IAccountState)
;; `claim-key` is the claim that the link was made by proving, `link-op` the
;; op of the enter that made it.
(defrecord AccountLinked [person own-lane making-uuid making-session says says-at claim-key link-op]
  IAccountState)
;; The mark of an unlink that is under way. It holds no `:says`.
(defrecord AccountUnlinking [person claim-key link-op]
  IAccountState)

;; $$claims {String -> IClaimState} (partitioned by |hash claim-key)
;; One record for a claim: whom it names, the invitation that waits under it
;; (one or none), and the links that were made by proving it, a map from the
;; op of the linking enter to `[invited-by-kind invited-by-id invited-at]`,
;; the invitation that the link was made by. The record is removed when
;; nothing waits and no link stands.
(defrecord Invitation [invite-op invited-by-kind invited-by-id invited-at ends])
(defrecord Claim [person waiting links]
  IClaimState)

;; $$invite-ops {UUID -> IInviteOpState} (partitioned by |hash claim-key):
;; read and written only at the claim's task, in the event of its own
;; invite, and never looked up by op alone. It names nobody; `at` is there so
;; that a later sweep can drop old ones.
(defrecord InviteOpDone [answer at]
  IInviteOpState)

;; $$asks {String -> IAskState} (partitioned by |hash code)
;; `hash` is of the secret that becomes the program's credential at a yes.
;; `asking-hash` is of the secret with which the program asks how its ask
;; stands; it is kept in every state, since the program asks after the answer
;; too, and it never becomes a sign-in's hash.
(defrecord AskWaiting [hash asking-hash asks ends born-over?]
  IAskState)
(defrecord AskAnswering [hash asking-hash asks ends born-over? op-id yes? root actor new-agent-label
                         sign-in until at]
  IAskState)
(defrecord AskYes [hash asking-hash asks ends root actor sign-in]
  IAskState)
(defrecord AskNo [hash asking-hash asks ends]
  IAskState)

;; $$enter-ops {UUID -> IEnterOpState} (partitioned by |hash op-id)
(defrecord EnterOpFailed []
  IEnterOpState)

;; ------------------------------------------------------------- key encoders

(defn account-key
  "The provider's full keyword text is length-prefixed, so that arbitrary
  subjects (including `:` or NUL) never collide."
  [provider ^String subject]
  (let [p (subs (str provider) 1)]
    (str (.length p) ":" p ":" subject)))

(defn claim-key-from-parts
  [kind ^String h]
  (case kind
    :email (str "e:" h)
    :code  (str "c:" h)))

(defn claim-key-from-map
  "A claim is `{:by :email :hash H}` or `{:by :code :hash H}`: two kinds of
  one form."
  [claim]
  (claim-key-from-parts (:by claim) (:hash claim)))

(defn hex16
  "`n` must not be negative."
  ^String [^long n]
  (format "%016x" n))

(defn hex-ts
  "The xor with `Long/MIN_VALUE` makes the text strictly order-preserving
  across `[Long/MIN_VALUE, Long/MAX_VALUE]`."
  ^String [^long t]
  (format "%016x" (bit-xor t Long/MIN_VALUE)))

(defn k-root
  ^String [^UUID p]
  (str p "\u0000r"))

(defn k-actor
  ^String [^UUID p ^UUID a]
  (str p "\u0000a:" a))

(defn k-ao
  "An agent's order record stays here while the agent is live, and moves to
  `k-ae` when it ends: so `end-actor`, which scans the live agents made
  after the one it ends for those that end with it, reads no agent that
  ended before."
  ^String [^UUID p ^long seq-n]
  (str p "\u0000ao:" (hex16 seq-n)))

(defn k-ao-lo
  ^String [^UUID p]
  (str p "\u0000ao:"))

(defn k-ao-hi
  "Exclusive: `;` is ASCII 0x3b, just above `:` 0x3a."
  ^String [^UUID p]
  (str p "\u0000ao;"))

(defn k-ae
  ^String [^UUID p ^long seq-n]
  (str p "\u0000ae:" (hex16 seq-n)))

(defn k-ae-lo
  ^String [^UUID p]
  (str p "\u0000ae:"))

(defn k-ae-hi
  ^String [^UUID p]
  (str p "\u0000ae;"))

(defn k-signin
  ^String [^UUID p ^UUID g]
  (str p "\u0000g:" g))

(defn k-session
  ^String [^UUID p ^UUID s]
  (str p "\u0000s:" s))

(defn k-exp
  "`kind-ch` is `\"g\"` for a sign-in or `\"s\"` for a session."
  ^String [^UUID p ^long until ^String kind-ch ^UUID id]
  (str p "\u0000exp:" (hex-ts until) ":" kind-ch ":" id))

(defn k-exp-lo
  ^String [^UUID p]
  (str p "\u0000exp:"))

(defn k-exp-hi
  "Exclusive, and above every `exp:` entry with `until <= at`."
  ^String [^UUID p ^long at]
  (str p "\u0000exp:" (hex-ts at) ";"))

(defn k-ok
  "The outcome of an `answer-ask`, by its `op-id`."
  ^String [^UUID p ^UUID op-id]
  (str p "\u0000ok:" op-id))

(defn k-account
  "One account of the person `p`, by its account key."
  ^String [^UUID p ^String account-key]
  (str p "\u0000na:" account-key))

(defn k-account-lo
  ^String [^UUID p]
  (str p "\u0000na:"))

(defn k-account-hi
  ^String [^UUID p]
  (str p "\u0000na;"))

;; ------------------------------------------- inner keys of a root in $$live

(defn lk-signin
  ^String [^UUID g]
  (str "g:" g))

(defn lk-session
  ^String [^UUID s]
  (str "s:" s))

(def lk-ends
  "Below `g:`, so that no scan of sign-ins or sessions reads it."
  "ends")

(def live-lo
  "")

(def live-hi
  "Exclusive. DEL (0x7f): above every printable 7-bit ASCII character."
  "\u007f")

(def live-things-lo
  "With `live-things-hi`: the sign-ins and the sessions, without `lk-ends`."
  "g:")

(def live-things-hi
  "s;")

(def live-signin-lo
  "g:")

(def live-signin-hi
  "g;")

(def live-session-lo
  "s:")

(def live-session-hi
  "s;")

;; ------------------------------------------------ the form of a depot record
;; The client (`softland.auth.client`) checks each input (`softland.auth.fit`)
;; before it builds a record. A record that some other appender wrote is
;; checked here, in the topology, before any of its fields is used: a field
;; of another type would throw in a step, and under `:all-after` the record
;; would be tried again for ever and hold its partition.

(def does-not-fit
  {:could-not :read-the-event})

(defn- long-val?
  [x]
  (instance? Long x))

(defn- text-or-nil?
  [x]
  (or (nil? x) (string? x)))

(defn- said?
  [x]
  (and (map? x) (every? (fn [[k v]] (and (string? k) (string? v))) x)))

(defn- driven-by-val?
  [x]
  (or (nil? x)
      (instance? DrivenByPerson x)
      (and (instance? DrivenByModel x) (string? (:model x)))))

(defn- record-fits?
  [ev]
  (boolean
   (cond
     (instance? InviteEvent ev)
     (and (uuid? (:op-id ev)) (uuid? (:person ev)) (string? (:claim-key ev))
          (contains? #{:system :actor} (:invited-by-kind ev))
          (or (nil? (:invited-by-id ev)) (uuid? (:invited-by-id ev))) (long-val? (:ends ev)) (long-val? (:at ev)))

     (instance? TakeBackEvent ev)
     (and (string? (:claim-key ev)) (uuid? (:person ev)) (uuid? (:invite-op ev)) (long-val? (:at ev)))

     (instance? EnterEvent ev)
     (and (uuid? (:op-id ev)) (string? (:account-key ev)) (keyword? (:provider ev)) (string? (:subject ev))
          (said? (:says ev)) (text-or-nil? (:email-hash ev)) (text-or-nil? (:code-hash ev))
          (uuid? (:own-lane ev)) (uuid? (:making-uuid ev)) (uuid? (:making-session ev)) (long-val? (:at ev)))

     (instance? UnlinkEvent ev)
     (and (string? (:account-key ev)) (uuid? (:link-op ev)) (uuid? (:person ev)) (long-val? (:at ev)))

     (instance? AskEvent ev)
     (and (string? (:code ev)) (string? (:hash ev)) (string? (:asking-hash ev)) (string? (:asks ev))
          (long-val? (:ends ev)) (long-val? (:at ev)))

     (instance? AnswerAskEvent ev)
     (and (uuid? (:op-id ev)) (string? (:code ev)) (boolean? (:yes? ev)) (uuid? (:root ev)) (uuid? (:actor ev))
          (text-or-nil? (:new-agent-label ev)) (uuid? (:sign-in ev)) (long-val? (:until ev)) (long-val? (:at ev)))

     (instance? OwnLaneMadeEvent ev)
     (and (uuid? (:root-id ev)) (long-val? (:at ev)))

     (instance? BeginSignInEvent ev)
     (and (uuid? (:root-id ev)) (uuid? (:actor ev)) (uuid? (:sign-in ev)) (contains? #{:browser :program} (:kind ev))
          (string? (:hash ev)) (long-val? (:until ev)) (text-or-nil? (:label ev)) (long-val? (:at ev)))

     (instance? ReplaceSecretEvent ev)
     (and (uuid? (:root-id ev)) (uuid? (:sign-in ev)) (string? (:old-hash ev)) (string? (:new-hash ev))
          (long-val? (:until ev)) (long-val? (:previous-until ev)) (long-val? (:at ev)))

     (instance? EndSignInEvent ev)
     (and (uuid? (:root-id ev)) (uuid? (:sign-in ev)) (long-val? (:at ev)))

     (instance? MakeActorEvent ev)
     (and (uuid? (:root-id ev)) (uuid? (:actor ev)) (uuid? (:made-by ev)) (string? (:label ev)) (long-val? (:at ev)))

     (instance? EndActorEvent ev)
     (and (uuid? (:root-id ev)) (uuid? (:actor ev)) (long-val? (:at ev)))

     (instance? OpenSessionEvent ev)
     (and (uuid? (:root-id ev)) (uuid? (:sign-in ev)) (uuid? (:session ev)) (long-val? (:until ev))
          (driven-by-val? (:driven-by ev)) (long-val? (:at ev)))

     (instance? CloseSessionEvent ev)
     (and (uuid? (:root-id ev)) (uuid? (:session ev)) (long-val? (:at ev)))

     :else false)))

(defn invite-record-fits?
  [ev]
  (and (instance? IInviteEvent ev) (record-fits? ev)))

(defn enter-record-fits?
  [ev]
  (and (instance? IEnterEvent ev) (record-fits? ev)))

(defn ask-record-fits?
  [ev]
  (and (or (instance? AskEvent ev) (instance? AnswerAskEvent ev)) (record-fits? ev)))

(defn root-record-fits?
  [ev]
  (and (instance? IRootEvent ev) (record-fits? ev)))

;; ------------------------------------------------------- action accessors
;; Named Clojure functions so Rama dataflow never puts keywords in op position.

(defn act-done?
  [act]
  (boolean (:done? act)))

(defn act-result
  [act]
  (:result act))

(defn act-write-k
  [act]
  (:write-k act))

(defn act-write-v
  [act]
  (:write-v act))

(defn act-delete?
  [act]
  (boolean (:delete? act)))

(defn act-writes
  "Each `[k v]` that the act writes to the one table of its step, `v` nil to
  remove `k`."
  [act]
  (cond
    (:writes act) (:writes act)
    (some? (:write-k act)) [[(:write-k act) (when-not (:delete? act) (:write-v act))]]
    :else []))

(defn act-op-v
  "What an invite writes under its own op, at the claim's task."
  [act]
  (:op-v act))

(defn act-hop-key
  "Nil for a step on the same task."
  [act]
  (:hop-key act))

(defn act-next-step
  [act]
  (:next-step act))

(defn act-next-st
  [act]
  (:next-st act))

;; ------------------------------------------------- invite and take-back

(defn invitation-lets-in?
  "Whether the invitation `w` lets in an enter at `at`: it was made at or
  before `at`, and `at` is before its end. Both ends are held against the
  `:at` of the op that the claim is used for."
  [w at]
  (boolean (and (instance? Invitation w)
                (<= (long (:invited-at w)) (long at))
                (< (long at) (long (:ends w))))))

(defn- not-over?
  "Whether the invitation `w` is not over at `at`."
  [w at]
  (boolean (and (instance? Invitation w) (< (long at) (long (:ends w))))))

(defn claim-named
  "The person whom the claim `c` names at `at`: its person while a link
  stands under it or an invitation waits that is not over. Nil when the
  claim is free."
  [c at]
  (when (and (instance? Claim c) (or (seq (:links c)) (not-over? (:waiting c) at)))
    (:person c)))

(defn- answer-of
  "The result of an invite of `P` that was answered `answer`."
  [P answer]
  (if (= :invited answer) {:invited P} {:refused :claim-in-use}))

(defn invite-step
  "On H(C), one step, no hop. `c` is the claim's record and `done` the
  record of this invite's op. An op that has a record changes nothing and
  gives the answer it gave. Every other case writes the op's record in the
  same event."
  [^InviteEvent ev c done]
  (let [P (:person ev)
        ck (:claim-key ev)
        at (long (:at ev))
        named (claim-named c at)]
    (cond
      (instance? InviteOpDone done)
      {:result (answer-of P (:answer done))}

      (and (some? named) (not= named P))
      {:result {:refused :claim-in-use} :op-v (->InviteOpDone :claim-in-use at)}

      (and (= named P) (not-over? (:waiting c) at))
      {:result {:invited P} :op-v (->InviteOpDone :invited at)}

      :else
      ;; The links, when there are any, are P's: another's would name another.
      {:result {:invited P}
       :write-k ck
       :write-v (->Claim P
                         (->Invitation (:op-id ev) (:invited-by-kind ev) (:invited-by-id ev) at (:ends ev))
                         (if (instance? Claim c) (:links c) {}))
       :op-v (->InviteOpDone :invited at)})))

(defn take-back-step
  "On H(C), one step, no hop. Takes back the invitation that the op
  `invite-op` made, when it waits; touches no link and no other invitation."
  [^TakeBackEvent ev c]
  (let [ck (:claim-key ev)
        res {:result {:taken-back (:person ev)}}]
    (if (and (instance? Claim c) (= (:invite-op ev) (:invite-op (:waiting c))))
      (if (empty? (:links c))
        (assoc res :write-k ck :delete? true)
        (assoc res :write-k ck :write-v (assoc c :waiting nil)))
      res)))

;; ------------------------------------- what an unlink and a release remove

(defn release-claim
  "At H(C): the claim `c` of key `ck` without the link of op `L`. The record
  goes when nothing waits under it and no link is left. Nothing when the
  link is not there."
  [ck c L]
  (cond
    (not (and (instance? Claim c) (contains? (:links c) L))) {}
    (and (nil? (:waiting c)) (= 1 (count (:links c)))) {:write-k ck :delete? true}
    :else {:write-k ck :write-v (update c :links dissoc L)}))

(defn release-entry
  "At H(P): removes the person's entry of the account when it is of the link
  `L`; a link made anew meanwhile is not touched."
  [P ak e L]
  (if (and (instance? PersonAccount e) (= L (:link-op e)))
    {:write-k (k-account P ak) :delete? true}
    {}))

(defn release-mark
  "At H(Acc): removes the mark of the unlink of the link `L`."
  [ak acc L]
  (if (and (instance? AccountUnlinking acc) (= L (:link-op acc)))
    {:write-k ak :delete? true}
    {}))

(defn- mark-of
  [^AccountUnlinking acc]
  {:person (:person acc) :claim-key (:claim-key acc) :link-op (:link-op acc)})

;; ------------------------------------------------------------------ enter

(defn claim-provable?
  "Whether the op `op`, whose enter is at `at`, can use the claim `c`: it
  holds a link of `op` already (a record that failed after the use, running
  again, or a second use of one round), or an invitation waits that lets the
  enter in."
  [c op at]
  (boolean (and (instance? Claim c)
                (or (contains? (:links c) op)
                    (invitation-lets-in? (:waiting c) at)))))

(defn- linking->target
  [^AccountLinking acc]
  {:op-id (:op-id acc)
   :account-key (:account-key acc)
   :provider (:provider acc)
   :subject (:subject acc)
   :code-claim-key (:code-claim-key acc)
   :email-claim-key (:email-claim-key acc)
   :own-lane (:own-lane acc)
   :making-uuid (:making-uuid acc)
   :making-session (:making-session acc)
   :says (:says acc)
   :at (:at acc)
   :status (:status acc)
   :chosen (:chosen acc)
   :round (:round acc)})

(defn- enter-route-linking
  [^AccountLinking acc]
  (let [t (linking->target acc)]
    (case (:status acc)
      :failing
      {:hop-key (:op-id acc)
       :next-step :enter/step-2c-mark-failed
       :next-st {:target t}}

      :chosen
      {:hop-key (:op-id acc)
       :next-step :enter/step-2c-confirm-choice
       :next-st {:target t :curr-claim (:chosen acc)}}

      ;; :probing
      (let [cc (:code-claim-key acc)
            ec (:email-claim-key acc)
            [ph ck] (if cc [:code cc] [:email ec])]
        {:hop-key ck
         :next-step :enter/step-2-probe
         :next-st {:target t :phase ph :curr-claim ck :code-was-nil? false}}))))

(defn enter-step-1
  "Step E1, on H(Acc). A mark of an unlink is finished first (`E1u`), and
  then this step runs again."
  [^EnterEvent ev acc]
  (let [ak (:account-key ev)
        self-op (:op-id ev)
        cc (when-let [h (:code-hash ev)] (claim-key-from-parts :code h))
        ec (when-let [h (:email-hash ev)] (claim-key-from-parts :email h))]
    (cond
      (instance? AccountLinked acc)
      (let [prev-at (long (:says-at acc))
            link-info {:linked-person (:person acc)
                       :linked-lane (:own-lane acc)
                       :linked-making {:uuid (:making-uuid acc)
                                       :session (:making-session acc)}}]
        (if (> (long (:at ev)) prev-at)
          {:hop-key self-op
           :next-step :enter/step-1a-check-op-returning
           :next-st link-info}
          {:hop-key (:person acc)
           :next-step :enter/step-1b-read-flag
           :next-st link-info}))

      (instance? AccountUnlinking acc)
      {:hop-key (:claim-key acc)
       :next-step :enter/step-1u-claim
       :next-st {:mark (mark-of acc)}}

      (nil? acc)
      (if (and (nil? cc) (nil? ec))
        {:hop-key self-op
         :next-step :enter/step-2c-mark-failed-direct
         :next-st {}}
        (let [linking (->AccountLinking self-op ak (:provider ev) (:subject ev)
                                        cc ec (:own-lane ev) (:making-uuid ev)
                                        (:making-session ev) (:says ev) (:at ev)
                                        :probing nil 0)]
          (assoc (enter-route-linking linking)
                 :write-k ak
                 :write-v linking)))

      (instance? AccountLinking acc)
      (enter-route-linking acc))))

(defn enter-step-1u-claim
  "Step E1u, on H(C) of the mark: the unlink's U2."
  [^EnterEvent ev st c]
  (let [{:keys [person claim-key link-op]} (:mark st)]
    (assoc (release-claim claim-key c link-op)
           :hop-key person :next-step :enter/step-1u-person :next-st st)))

(defn enter-step-1u-person
  "Step E1u, on H(P) of the mark: the unlink's U3."
  [^EnterEvent ev st e]
  (let [{:keys [person link-op]} (:mark st)]
    (assoc (release-entry person (:account-key ev) e link-op)
           :hop-key (:account-key ev) :next-step :enter/step-1u-account :next-st st)))

(defn enter-step-1u-account
  "Step E1u, on H(Acc): the unlink's U5, then step E1 again."
  [^EnterEvent ev st acc]
  (assoc (release-mark (:account-key ev) acc (:link-op (:mark st)))
         :hop-key nil :next-step :enter/step-1 :next-st {}))

(defn enter-step-1a-check-op-returning
  "Step E1a, on H(self-op): blocks `:all-after` replay of a previously
  refused enter from overwriting `:says` on an already-linked account."
  [^EnterEvent ev st eo]
  (if (instance? EnterOpFailed eo)
    {:done? true :result {:refused :not-invited}}
    {:hop-key (:account-key ev)
     :next-step :enter/step-1c-update-says
     :next-st st}))

(defn enter-step-1c-update-says
  "Step E1c, on H(Acc)."
  [^EnterEvent ev st acc]
  (let [P (:linked-person st)
        upd? (and (instance? AccountLinked acc)
                  (> (long (:at ev)) (long (:says-at acc))))]
    (cond-> {:hop-key P :next-step :enter/step-1b-read-flag :next-st st}
      upd? (assoc :write-k (:account-key ev)
                  :write-v (assoc acc :says (:says ev) :says-at (:at ev))))))

(defn enter-step-1b-read-flag
  "Step E1b, on H(P)."
  [st r]
  {:done? true
   :result {:person (:linked-person st)
            :own-lane (:linked-lane st)
            :making (:linked-making st)
            :own-lane-made? (boolean (when (instance? RootEntered r) (:own-lane-made? r)))}})

(defn enter-step-2-probe
  "Step E2, on H(curr-claim): strictly read-only probe of `$$claims[C]`,
  with `:code-recheck` when `:code` had no record and the email's claim
  has one that this op cannot use."
  [^EnterEvent ev st c]
  (let [target (:target st)
        op (:op-id target)
        phase (:phase st)
        ck (:curr-claim st)
        ak (:account-key ev)]
    (cond
      (claim-provable? c op (:at target))
      {:hop-key ak
       :next-step :enter/step-2b-choose
       :next-st {:target target :probed ck}}

      (and (= :code phase) (some? (:email-claim-key target)))
      {:hop-key (:email-claim-key target)
       :next-step :enter/step-2-probe
       :next-st {:target target
                 :phase :email
                 :curr-claim (:email-claim-key target)
                 :code-was-nil? (nil? c)}}

      (and (= :email phase) (:code-was-nil? st) (some? c))
      {:hop-key (:code-claim-key target)
       :next-step :enter/step-2-probe
       :next-st {:target target
                 :phase :code-recheck
                 :curr-claim (:code-claim-key target)
                 :code-was-nil? false}}

      :else
      {:hop-key ak
       :next-step :enter/step-2b-choose
       :next-st {:target target :probed nil}})))

(defn enter-step-2b-choose
  "Step E2', on H(Acc). It changes the status only of the linking of its
  own target op in its own round; a chain whose round has passed starts
  again at step E1."
  [^EnterEvent ev st acc]
  (let [target (:target st)
        op (:op-id target)
        probed (:probed st)
        ak (:account-key ev)]
    (cond
      (not (and (instance? AccountLinking acc) (= (:op-id acc) op)))
      (enter-step-1 ev acc)

      (not= (:round acc) (:round target))
      (enter-step-1 ev acc)

      (= :failing (:status acc))
      (enter-route-linking acc)

      (= :chosen (:status acc))
      (enter-route-linking acc)

      (some? probed)
      (let [acc' (assoc acc :status :chosen :chosen probed)]
        (assoc (enter-route-linking acc')
               :write-k ak
               :write-v acc'))

      :else
      (let [acc' (assoc acc :status :failing :chosen nil)]
        (assoc (enter-route-linking acc')
               :write-k ak
               :write-v acc')))))

(defn enter-step-2c-confirm-choice
  "Step E2c-confirm, on H(op)."
  [^EnterEvent ev st eo]
  (if (instance? EnterOpFailed eo)
    {:hop-key (:account-key ev)
     :next-step :enter/step-2d-finish-failed
     :next-st {:target (:target st)}}
    {:hop-key (:curr-claim st)
     :next-step :enter/step-3-use
     :next-st st}))

(defn enter-step-2c-mark-failed
  "Step E2c-fail, on H(op): writes `EnterOpFailed` before H(Acc) clears
  `AccountLinking`."
  [^EnterEvent ev st eo]
  (let [op (:op-id (:target st))]
    (cond-> {:hop-key (:account-key ev)
             :next-step :enter/step-2d-finish-failed
             :next-st {:target (:target st)}}
      (not (instance? EnterOpFailed eo))
      (assoc :write-k op :write-v (->EnterOpFailed)))))

(defn enter-step-2c-mark-failed-direct
  "Step E2c-direct, on H(self-op)."
  [^EnterEvent ev eo]
  (cond-> {:done? true :result {:refused :not-invited}}
    (not (instance? EnterOpFailed eo))
    (assoc :write-k (:op-id ev) :write-v (->EnterOpFailed))))

(defn enter-step-2d-finish-failed
  "Step E2d, on H(Acc)."
  [^EnterEvent ev st acc]
  (let [op (:op-id (:target st))
        ak (:account-key ev)
        clear? (and (instance? AccountLinking acc) (= (:op-id acc) op))]
    (if (= op (:op-id ev))
      (cond-> {:done? true :result {:refused :not-invited}}
        clear? (assoc :write-k ak :delete? true))
      ;; A helper: clears the op's linking, then judges its own enter afresh
      ;; at step E1, on this task.
      (cond-> {:hop-key nil :next-step :enter/step-1 :next-st {}}
        clear? (assoc :write-k ak :delete? true)))))

(defn enter-step-3-use
  "Step E3, on H(curr-claim). A link of the target op that the claim holds
  already goes on with what it holds; else an invitation that lets the
  target's enter in is used: it no longer waits, and the target's link
  stands under the claim. Anything else is a lost choice."
  [^EnterEvent ev st c]
  (let [target (:target st)
        op (:op-id target)
        ck (:curr-claim st)
        ak (:account-key ev)
        won (fn [P [by-kind by-id by-at]]
              {:hop-key P
               :next-step :enter/step-4-root
               :next-st {:target (assoc target :won-person P :won-claim-key ck
                                        :won-by-kind by-kind :won-by-id by-id :won-at by-at)}})]
    (cond
      (and (instance? Claim c) (contains? (:links c) op))
      (won (:person c) (get (:links c) op))

      (and (instance? Claim c) (invitation-lets-in? (:waiting c) (:at target)))
      (let [w (:waiting c)
            by [(:invited-by-kind w) (:invited-by-id w) (:invited-at w)]]
        (assoc (won (:person c) by)
               :write-k ck
               :write-v (assoc c :waiting nil :links (assoc (:links c) op by))))

      :else
      {:hop-key ak
       :next-step :enter/step-3b-clear-chosen
       :next-st {:target target :failed-claim ck}})))

(defn enter-step-3b-clear-chosen
  "Step E3b, on H(Acc). Resets the target's linking to `:probing`, in a
  round one higher, only when it is in the round that the chain left in."
  [^EnterEvent ev st acc]
  (let [target (:target st)
        op (:op-id target)
        failed-claim (:failed-claim st)
        ak (:account-key ev)
        reset? (and (instance? AccountLinking acc)
                    (= (:op-id acc) op)
                    (= (:round acc) (:round target))
                    (= :chosen (:status acc))
                    (= (:chosen acc) failed-claim))]
    (cond-> {:hop-key nil :next-step :enter/step-1 :next-st {}}
      reset? (assoc :write-k ak :write-v (assoc acc :status :probing :chosen nil
                                                :round (inc (long (:round acc))))))))

(defn enter-step-4-root
  "Step E4, on H(P). A person with no record gets `RootEntered` from what
  the enter offers, with who invited her from the use; one who has a record
  keeps it. Either way her entry of this account is written, with the
  target op as its link."
  [^EnterEvent ev st r]
  (let [target (:target st)
        P (:won-person target)
        ak (:account-key ev)
        entered-already? (instance? RootEntered r)
        r' (if entered-already?
             r
             (->RootEntered (:won-by-kind target) (:won-by-id target) (:won-at target)
                            (:own-lane target) (:making-uuid target) (:making-session target)
                            false 0 0))
        entry (->PersonAccount (:provider target) (:subject target) (:at target) (:op-id target))]
    {:writes (cond-> [[(k-account P ak) entry]]
               (not entered-already?) (conj [(k-root P) r']))
     :hop-key ak
     :next-step :enter/step-5-link
     :next-st {:target (assoc target
                              :fixed-lane (:own-lane r')
                              :fixed-making-uuid (:making-uuid r')
                              :fixed-making-session (:making-session r')
                              :present-flag (boolean (:own-lane-made? r')))}}))

(defn enter-step-5-link
  "Step E5, on H(Acc). The target's linking, in any round, becomes its link;
  a link of the target op answers its person. Anything else (no record, a
  mark of an unlink, a link or a linking of another op) releases what the
  use and step E4 wrote (`E5r`) and starts again at step E1 for this
  chain's own op. A helper with a strictly later `:at` hops to H(self-op)
  (`E1a`) to verify not refused before updating `:says`."
  [^EnterEvent ev st acc]
  (let [target (:target st)
        op (:op-id target)
        self-op (:op-id ev)
        ak (:account-key ev)
        P (:won-person target)
        fl (:fixed-lane target)
        fmu (:fixed-making-uuid target)
        fms (:fixed-making-session target)
        flag (boolean (:present-flag target))
        helping? (not= op self-op)
        linking-this? (and (instance? AccountLinking acc) (= (:op-id acc) op))
        linked-base (cond
                      linking-this?
                      (->AccountLinked P fl fmu fms (:says target) (:at target) (:won-claim-key target) op)
                      (and (instance? AccountLinked acc) (= (:link-op acc) op))
                      acc
                      :else nil)
        need-self-says? (and (some? linked-base)
                             (> (long (:at ev)) (long (:says-at linked-base))))]
    (cond
      (nil? linked-base)
      {:hop-key (:won-claim-key target)
       :next-step :enter/step-5r-claim
       :next-st {:target target}}

      (and helping? need-self-says?)
      (cond-> {:hop-key self-op
               :next-step :enter/step-1a-check-op-returning
               :next-st {:linked-person P
                         :linked-lane fl
                         :linked-making {:uuid fmu :session fms}}}
        linking-this? (assoc :write-k ak :write-v linked-base))

      (and (not helping?) need-self-says?)
      {:write-k ak
       :write-v (assoc linked-base :says (:says ev) :says-at (:at ev))
       :done? true
       :result {:person P :own-lane fl :making {:uuid fmu :session fms} :own-lane-made? flag}}

      :else
      (cond-> {:done? true
               :result {:person P :own-lane fl :making {:uuid fmu :session fms} :own-lane-made? flag}}
        linking-this? (assoc :write-k ak :write-v linked-base)))))

(defn enter-step-5r-claim
  "Step E5r, on H(C) of the use: takes the target op's link out of the
  claim. The invitation that the use took is spent and is not put back:
  putting it back could bring back one that was taken back meanwhile."
  [^EnterEvent ev st c]
  (let [target (:target st)]
    (assoc (release-claim (:won-claim-key target) c (:op-id target))
           :hop-key (:won-person target) :next-step :enter/step-5r-person :next-st st)))

(defn enter-step-5r-person
  "Step E5r, on H(P) of the use: removes her entry of this account when it
  is of the target op, then step E1 again for this chain's own op."
  [^EnterEvent ev st e]
  (let [target (:target st)]
    (assoc (release-entry (:won-person target) (:account-key ev) e (:op-id target))
           :hop-key (:account-key ev) :next-step :enter/step-1 :next-st {})))

;; ----------------------------------------------------------------- unlink

(defn unlink-step-1
  "Step U1, on H(Acc). The link `L` of `P` is replaced by its mark, which is
  the decision and holds no `:says`. A mark of `L` is finished. A link `L`
  of another person is refused, and so is a link that the enter `L` is
  still making. Anything else means that `L` does not stand on the
  account: U3 removes an entry that a fault left, and the unlink gives
  `{:unlinked P}`."
  [^UnlinkEvent ev acc]
  (let [L (:link-op ev)
        P (:person ev)
        ak (:account-key ev)]
    (cond
      (and (instance? AccountLinked acc) (= L (:link-op acc)))
      (if (= P (:person acc))
        (let [mark (->AccountUnlinking P (:claim-key acc) L)]
          {:write-k ak :write-v mark
           :hop-key (:claim-key acc) :next-step :unlink/step-2 :next-st {:mark (mark-of mark)}})
        {:done? true :result {:refused :another-persons}})

      (and (instance? AccountUnlinking acc) (= L (:link-op acc)))
      {:hop-key (:claim-key acc) :next-step :unlink/step-2 :next-st {:mark (mark-of acc)}}

      (and (instance? AccountLinking acc) (= L (:op-id acc)))
      {:done? true :result {:refused :linking}}

      :else
      {:hop-key P :next-step :unlink/step-3 :next-st {:mark {:person P :link-op L} :last? true}})))

(defn unlink-step-2
  "Step U2, on H(C) of the mark."
  [^UnlinkEvent ev st c]
  (let [{:keys [person claim-key link-op]} (:mark st)]
    (assoc (release-claim claim-key c link-op)
           :hop-key person :next-step :unlink/step-3 :next-st st)))

(defn unlink-step-3
  "Step U3, on H(P) of the mark."
  [^UnlinkEvent ev st e]
  (let [{:keys [person link-op]} (:mark st)
        act (release-entry person (:account-key ev) e link-op)]
    (if (:last? st)
      (assoc act :done? true :result {:unlinked (:person ev)})
      (assoc act :hop-key (:account-key ev) :next-step :unlink/step-5 :next-st st))))

(defn unlink-step-5
  "Step U5, on H(Acc)."
  [^UnlinkEvent ev st acc]
  (assoc (release-mark (:account-key ev) acc (:link-op (:mark st)))
         :done? true :result {:unlinked (:person ev)}))

;; --------------------------------------------------- Slice a: own-lane-made

(defn own-lane-made-step
  "On H(P)."
  [^OwnLaneMadeEvent ev r]
  (let [P (:root-id ev)]
    (if (instance? RootEntered r)
      (cond-> {:done? true :result {:made P}}
        (not (:own-lane-made? r))
        (assoc :write-k (k-root P) :write-v (assoc r :own-lane-made? true)))
      {:done? true :result {:refused :no-such-person}})))

;; ------------------------------------------- Slice b: root plans & writes

(def ^:private max-actor-depth
  "Makers below the root. Matches `SPEC.md` and `reference.clj`."
  32)

(defn plan-rd-upserts
  [plan]
  (or (:rd-upserts plan) []))

(defn plan-rd-deletes
  [plan]
  (or (:rd-deletes plan) []))

(defn plan-live-upserts
  [plan]
  (or (:live-upserts plan) []))

(defn plan-live-deletes
  [plan]
  (or (:live-deletes plan) []))

(defn plan-result
  [plan]
  (:result plan))

(defn plan-ended-something?
  [plan]
  (boolean (:ended-something? plan)))

(defn bump-ends
  "`entry` is the root's `LiveEnds`, or nil before its first end."
  [entry]
  (->LiveEnds (inc (long (if (instance? LiveEnds entry) (:n entry) 0)))))

(defn signin-rec->live
  [^SignInRec g]
  (->LiveSignIn (:sign-in g) (:actor g) (:kind g) (:initial-hash g) (:hash g)
                (:until g) (:prev-hash g) (:prev-until g) (:begun g) (:label g)
                (:seq g) (:own-lane g) (:own-lane-made-at-begin? g)))

(defn live-signin->ended-rec
  "From the live entry alone: 0 point seeks on `g:`."
  [^LiveSignIn g]
  (->SignInRec (:sign-in g) (:actor g) (:kind g) (:initial-hash g) (:hash g)
               (:until g) (:prev-hash g) (:prev-until g) true (:begun g)
               (:label g) (:seq g) (:own-lane g) (:own-lane-made-at-begin? g)))

(defn live-session->closed-rec
  "From the live entry alone, `:driven-by` too: 0 point seeks on `s:`."
  [^LiveSession s]
  (->SessionRec (:session s) (:sign-in s) (:until s) true (:driven-by s)))

(defn plan-thin-watched
  "`exp-map` holds the `exp:` entries with `until <= at`. Each leaves the
  root's map in `$$live` with its `exp:` entry, by its own `until`: a
  session stays while its sign-in leaves, and the other way round. No
  record is written: leaving what is watched ends nothing, and a later
  write that gives the thing a time after its own `:at` puts it back.
  What it bounds is the map that a watcher holds, which would otherwise
  keep every sign-in and session that nobody ended."
  [exp-map]
  {:rd-deletes (vec (keys exp-map))
   :live-deletes (mapv (fn [^ExpiryEntry e]
                         (case (:kind e)
                           :sign-in (lk-signin (:id e))
                           :session (lk-session (:id e))))
                       (vals exp-map))})

(defn signin-agent
  "The agent whose record a judgment of the sign-in `g` needs; nil for a
  sign-in of the person herself, and for what is no sign-in."
  [root g]
  (when (and (instance? SignInRec g) (not= (:actor g) root))
    (:actor g)))

(defn- actor-over?
  "`actor-rec` is the record of `(signin-agent root g)`. An agent's record
  alone says it: `end-actor` marks every agent that ends with the one it
  ends, watched or not, since agents have no time."
  [root g actor-rec]
  (and (not= (:actor g) root)
       (or (not (instance? ActorRec actor-rec)) (boolean (:ended? actor-rec)))))

(defn signin-ended?
  "Whether someone ended the sign-in `g`: itself, or its actor or one above.
  The second is read from the actor's record and not from `ended?` of `g`:
  `end-actor` writes `ended?` on the sign-ins that it finds in `$$live`,
  and a sign-in that had left `$$live` is not among them."
  [root g actor-rec]
  (boolean (and (instance? SignInRec g)
                (or (:ended? g) (actor-over? root g actor-rec)))))

(defn signin-stands-at?
  "The judgment of a write that needs the sign-in live, against that write's
  own `at`. Nothing of it is kept."
  [root g actor-rec at]
  (boolean (and (instance? SignInRec g)
                (not (signin-ended? root g actor-rec))
                (> (long (:until g)) (long at)))))

(defn plan-begin-sign-in
  "On H(P), after the thinning. A sign-in whose `until` is at or before the
  `:at` of its begin is taken, not watched and not ended."
  [^BeginSignInEvent ev r g actor-rec]
  (let [P (:root-id ev)
        A (:actor ev)
        G (:sign-in ev)
        kind (:kind ev)
        h (:hash ev)
        until (long (:until ev))
        lbl (:label ev)
        at (long (:at ev))]
    (cond
      (and (instance? SignInRec g)
           (= (:actor g) A)
           (= (:kind g) kind)
           (= (:initial-hash g) h))
      {:result {:begun G}}

      (or (not (instance? RootEntered r))
          (and (not= A P) (nil? actor-rec)))
      {:result {:refused :no-such-actor}}

      (and (not= A P) (:ended? actor-rec))
      {:result {:refused :ended}}

      (some? g)
      {:result {:refused :in-use}}

      :else
      (let [seq-n (long (:next-sign-in-seq r))
            watched? (> until at)
            own-lane (:own-lane r)
            made-at-begin? (boolean (:own-lane-made? r))
            rec (->SignInRec G A kind h h until nil nil false at lbl seq-n own-lane made-at-begin?)
            r' (assoc r :next-sign-in-seq (inc seq-n))]
        {:rd-upserts (cond-> [[(k-root P) r']
                              [(k-signin P G) rec]]
                       watched?
                       (conj [(k-exp P until "g" G) (->ExpiryEntry :sign-in G A until)]))
         :live-upserts (if watched?
                         [[(lk-signin G) (signin-rec->live rec)]]
                         [])
         :result {:begun G}}))))

(defn plan-replace-secret
  "On H(P), after the thinning. `actor-rec` is the record of
  `(signin-agent P g)`. A replacement whose new `until` is at or before its
  own `:at` is taken: the sign-in then has that `until`, is not watched, and
  is not ended."
  [^ReplaceSecretEvent ev g actor-rec]
  (let [P (:root-id ev)
        G (:sign-in ev)
        old-h (:old-hash ev)
        new-h (:new-hash ev)
        until (long (:until ev))
        prev-until (long (:previous-until ev))
        at (long (:at ev))]
    (cond
      (and (instance? SignInRec g)
           (= (:hash g) new-h)
           (= (:prev-hash g) old-h))
      {:result {:replaced G}}

      (not (signin-stands-at? P g actor-rec at))
      {:result {:refused :not-live}}

      (not= (:hash g) old-h)
      {:result {:refused :wrong-secret}}

      :else
      (let [old-until (long (:until g))
            watched? (> until at)
            g' (assoc g :hash new-h :until until
                      :prev-hash old-h :prev-until prev-until)]
        ;; A delete of an `exp:` or `$$live` entry that is not there, as when
        ;; the sign-in had left what is watched, changes nothing.
        {:rd-upserts (cond-> [[(k-signin P G) g']]
                       watched?
                       (conj [(k-exp P until "g" G) (->ExpiryEntry :sign-in G (:actor g) until)]))
         :rd-deletes (if (and watched? (= until old-until)) [] [(k-exp P old-until "g" G)])
         :live-upserts (if watched? [[(lk-signin G) (signin-rec->live g')]] [])
         :live-deletes (if watched? [] [(lk-signin G)])
         :result {:replaced G}}))))

(defn signin-needs-end?
  [root g actor-rec]
  (boolean (and (instance? SignInRec g) (not (signin-ended? root g actor-rec)))))

(defn plan-end-sign-in
  "On H(P), after the thinning. `live-s-map` holds the watched sessions of
  the root, and is read only when `signin-needs-end?`. A session of the
  sign-in that is not watched is not written here: `who-is` and each write
  read its standing from the sign-in's record."
  [^EndSignInEvent ev g actor-rec live-s-map]
  (let [P (:root-id ev)
        G (:sign-in ev)]
    (cond
      (not (instance? SignInRec g))
      {:result {:refused :no-such-sign-in}}

      (signin-ended? P g actor-rec)
      {:result {:ended G}}

      :else
      (let [rd-upserts (transient [[(k-signin P G) (assoc g :ended? true)]])
            rd-deletes (transient [(k-exp P (:until g) "g" G)])
            live-deletes (transient [(lk-signin G)])]
        (doseq [[lk ^LiveSession s] live-s-map]
          (when (= (:sign-in s) G)
            (conj! live-deletes lk)
            (conj! rd-upserts [(k-session P (:session s)) (live-session->closed-rec s)])
            (conj! rd-deletes (k-exp P (:until s) "s" (:session s)))))
        {:rd-upserts (persistent! rd-upserts)
         :rd-deletes (persistent! rd-deletes)
         :live-deletes (persistent! live-deletes)
         :ended-something? true
         :result {:ended G}}))))

(defn plan-make-actor
  "On H(P), after the thinning."
  [^MakeActorEvent ev r existing-a maker-rec]
  (let [P (:root-id ev)
        A (:actor ev)
        B (:made-by ev)
        lbl (:label ev)]
    (cond
      (and (instance? ActorRec existing-a) (= (:made-by existing-a) B))
      {:result {:made A}}

      (or (not (instance? RootEntered r))
          (and (not= B P) (nil? maker-rec)))
      {:result {:refused :no-such-maker}}

      (and (not= B P) (:ended? maker-rec))
      {:result {:refused :ended}}

      (or (= A P) (some? existing-a))
      {:result {:refused :in-use}}

      (>= (if (= B P) 0 (long (:depth maker-rec))) max-actor-depth)
      {:result {:refused :too-deep}}

      :else
      (let [seq-n (long (:next-actor-seq r))
            depth (inc (if (= B P) 0 (long (:depth maker-rec))))]
        {:rd-upserts [[(k-root P) (assoc r :next-actor-seq (inc seq-n))]
                      ;; `ends-with` starts as the maker: no input sets it.
                      [(k-actor P A) (->ActorRec A B B depth lbl false seq-n)]
                      [(k-ao P seq-n) (->ActorOrderRec A B B depth lbl false seq-n)]]
         :result {:made A}}))))

(defn actor-needs-end?
  [root actor r a]
  (boolean (and (instance? RootEntered r)
                (not= actor root)
                (instance? ActorRec a)
                (not (:ended? a)))))

(defn plan-end-actor-noop
  "For when `actor-needs-end?` is false."
  [^EndActorEvent ev r a]
  (let [P (:root-id ev)
        A (:actor ev)]
    (cond
      (and (instance? ActorRec a) (:ended? a))
      {:result {:ended A}}

      (or (not (instance? RootEntered r))
          (and (not= A P) (nil? a)))
      {:result {:refused :no-such-actor}}

      (= A P)
      {:result {:refused :is-a-person}}

      :else
      {:result {:refused :no-such-actor}})))

(defn plan-end-actor-2scan
  "Revision 2: 3 seeks and 2 scans (`a:<A>`, a forward `ao:` scan from the
  target's `seq`, and a scan of the root's map in `$$live`). `ao:` holds
  live agents alone, so the scan reads the live agents made after the target
  and none that ended before. What ends with the target is followed by
  `ends-with`, in any depth, and `made-by` is read for nothing. It is
  reached through live actors alone: an agent that ended took with it
  everything that ends with it.

  Relied on and not checked: what ends with an agent was made after it, so
  that the scan from the target's `seq`, taken in the order of `seq`, meets
  an agent after the one that it ends with. That holds while `ends-with` is
  the maker, and no write makes it anything else; a write that points an
  agent at another must scan all of `ao:`, to a fixed point.

  `live-map` holds the watched sign-ins and sessions of the root. A sign-in
  of an ended agent that is not watched is not written here: its standing
  is read from the agent's record (`signin-ended?`)."
  [root ^ActorRec target-rec ao-map live-map]
  (let [target-id (:actor target-rec)
        ended-as (volatile! #{target-id})
        ended-gs (volatile! #{})
        rd-upserts (transient [])
        rd-deletes (transient [])
        live-deletes (transient [])]
    ;; Pass 1: single forward scan over ao-map sorted by ascending :seq.
    (doseq [^ActorOrderRec ao (sort-by :seq (vals ao-map))]
      (let [aid (:actor ao)]
        (when (and (not (:ended? ao))
                   (or (= aid target-id)
                       (contains? @ended-as (:ends-with ao))))
          (vswap! ended-as conj aid)
          (conj! rd-deletes (k-ao root (:seq ao)))
          (conj! rd-upserts [(k-ae root (:seq ao)) (assoc ao :ended? true)])
          (conj! rd-upserts [(k-actor root aid)
                             (->ActorRec aid (:made-by ao) (:ends-with ao) (:depth ao) (:label ao) true
                                         (:seq ao))]))))
    ;; Pass 2a: watched sign-ins of any ended actor (0 point seeks on g:).
    (let [as @ended-as]
      (doseq [[lk lv] live-map]
        (when (instance? LiveSignIn lv)
          (when (contains? as (:actor lv))
            (vswap! ended-gs conj (:sign-in lv))
            (conj! live-deletes lk)
            (conj! rd-deletes (k-exp root (:until lv) "g" (:sign-in lv)))
            (conj! rd-upserts [(k-signin root (:sign-in lv)) (live-signin->ended-rec lv)])))))
    ;; Pass 2b: watched sessions of any ended actor, by the actor that the
    ;; live entry holds: the session's sign-in need not be watched
    ;; (0 point seeks on s:, preserves :driven-by).
    (let [as @ended-as]
      (doseq [[lk lv] live-map]
        (when (instance? LiveSession lv)
          (when (contains? as (:actor lv))
            (conj! live-deletes lk)
            (conj! rd-deletes (k-exp root (:until lv) "s" (:session lv)))
            (conj! rd-upserts [(k-session root (:session lv)) (live-session->closed-rec lv)])))))
    {:rd-upserts (persistent! rd-upserts)
     :rd-deletes (persistent! rd-deletes)
     :live-deletes (persistent! live-deletes)
     :ended-something? true
     :result {:ended target-id}}))

(defn plan-open-session
  "On H(P), after the thinning. `g` is the record of the event's sign-in and
  `actor-rec` that of `(signin-agent P g)`. A repeat moves the session's
  time on when nobody closed it, its time has not passed at this `:at`, and
  its sign-in stands at this `:at`; otherwise it changes nothing. A session
  whose `until` is at or before the `:at` of its opening is taken, not
  watched and not closed."
  [^OpenSessionEvent ev s g actor-rec]
  (let [P (:root-id ev)
        G (:sign-in ev)
        S (:session ev)
        until (long (:until ev))
        db (:driven-by ev)
        at (long (:at ev))]
    (cond
      (and (instance? SessionRec s) (= (:sign-in s) G))
      (if (and (not (:closed? s))
               (> (long (:until s)) at)
               (signin-stands-at? P g actor-rec at)
               (> until (long (:until s))))
        (let [old-until (long (:until s))
              s' (assoc s :until until)]
          {:rd-upserts [[(k-session P S) s']
                        [(k-exp P until "s" S) (->ExpiryEntry :session S G until)]]
           :rd-deletes [(k-exp P old-until "s" S)]
           :live-upserts [[(lk-session S) (->LiveSession S G (:actor g) until (:driven-by s))]]
           :result {:open S}})
        {:result {:open S}})

      (not (signin-stands-at? P g actor-rec at))
      {:result {:refused :not-live}}

      (some? s)
      {:result {:refused :in-use}}

      :else
      (let [watched? (> until at)
            rec (->SessionRec S G until false db)]
        {:rd-upserts (cond-> [[(k-session P S) rec]]
                       watched?
                       (conj [(k-exp P until "s" S) (->ExpiryEntry :session S G until)]))
         :live-upserts (if watched?
                         [[(lk-session S) (->LiveSession S G (:actor g) until db)]]
                         [])
         :result {:open S}}))))

(defn open-session-signin
  "The sign-in whose record `close-session` needs: nil when `s` is no
  session, or one that is closed."
  [s]
  (when (and (instance? SessionRec s) (not (:closed? s)))
    (:sign-in s)))

(defn plan-close-session
  "On H(P), after the thinning. `g` is the record of
  `(open-session-signin s)` and `actor-rec` that of `(signin-agent P g)`: a
  session whose sign-in someone ended is closed already, though its own
  record may not say so."
  [^CloseSessionEvent ev s g actor-rec]
  (let [P (:root-id ev)
        S (:session ev)]
    (cond
      (not (instance? SessionRec s))
      {:result {:refused :no-such-session}}

      (or (:closed? s) (signin-ended? P g actor-rec))
      {:result {:closed S}}

      :else
      {:rd-upserts [[(k-session P S) (assoc s :closed? true)]]
       :rd-deletes [(k-exp P (:until s) "s" S)]
       :live-deletes [(lk-session S)]
       :ended-something? true
       :result {:closed S}})))

;; ------------------------------------------------- Slice c: ask & answer-ask

(defn ask-step
  "On H(U)."
  [^AskEvent ev k]
  (let [U (:code ev)
        h (:hash ev)
        ah (:asking-hash ev)
        ends (long (:ends ev))
        at (long (:at ev))]
    (cond
      (and (instance? IAskState k) (= (:hash k) h) (= (:asking-hash k) ah))
      {:done? true :result {:asked U}}

      (some? k)
      {:done? true :result {:refused :in-use}}

      :else
      {:write-k U
       :write-v (->AskWaiting h ah (:asks ev) ends (<= ends at))
       :done? true
       :result {:asked U}})))

(defn- answering->target
  [^AskAnswering k]
  {:hash (:hash k)
   :asks (:asks k)
   :ends (:ends k)
   :born-over? (:born-over? k)
   :op-id (:op-id k)
   :yes? (:yes? k)
   :root (:root k)
   :actor (:actor k)
   :new-agent-label (:new-agent-label k)
   :sign-in (:sign-in k)
   :until (:until k)
   :at (:at k)})

(defn answer-ask-step-1
  "On H(U) (Revision 4)."
  [^AnswerAskEvent ev k]
  (let [U (:code ev)
        P (:root ev)
        at (long (:at ev))]
    (cond
      (nil? k)
      {:hop-key P
       :next-step :answer-ask/step-1b
       :next-st {:takeout-root P :takeout-at at :pending-result {:refused :no-such-ask} :no-such-ask? true}}

      (instance? AskYes k)
      {:hop-key P
       :next-step :answer-ask/step-1b
       :next-st {:takeout-root P :takeout-at at
                 :pending-result {:yes {:root (:root k) :actor (:actor k) :sign-in (:sign-in k)}}
                 :no-such-ask? false}}

      (instance? AskNo k)
      {:hop-key P
       :next-step :answer-ask/step-1b
       :next-st {:takeout-root P :takeout-at at :pending-result {:no U} :no-such-ask? false}}

      (instance? AskWaiting k)
      (if (or (:born-over? k) (>= at (long (:ends k))))
        {:hop-key P
         :next-step :answer-ask/step-1b
         :next-st {:takeout-root P :takeout-at at :pending-result {:refused :over} :no-such-ask? false}}
        (let [ans (->AskAnswering (:hash k) (:asking-hash k) (:asks k) (:ends k) (:born-over? k)
                                  (:op-id ev) (:yes? ev) P (:actor ev) (:new-agent-label ev)
                                  (:sign-in ev) (:until ev) at)]
          {:write-k U
           :write-v ans
           :hop-key P
           :next-step :answer-ask/step-2
           :next-st {:target (answering->target ans)}}))

      (instance? AskAnswering k)
      {:hop-key (:root k)
       :next-step :answer-ask/step-2
       :next-st {:target (answering->target k)}})))

(defn answer-ask-eval-root
  "Step 2, on H(P_target), after the caller confirmed that `ok:<op>` is
  absent and thinned what is watched under P_target at `target.at`. The
  sign-in that a yes begins is taken, not watched and not ended when its
  `until` is at or before the answer's `:at`."
  [code target r actor-rec g]
  (let [{:keys [op-id yes? root actor new-agent-label sign-in until at hash asks]} target
        ok-key (k-ok root op-id)]
    (cond
      (not (instance? RootEntered r))
      (let [out (->AskOutcomeRefused :no-such-actor)]
        {:rd-upserts [[ok-key out]] :outcome out})

      (false? yes?)
      (let [out (->AskOutcomeNo code)]
        {:rd-upserts [[ok-key out]] :outcome out})

      (nil? new-agent-label)
      (let [missing-actor? (and (not= actor root) (nil? actor-rec))
            ended-actor?   (and (not= actor root) (some? actor-rec) (:ended? actor-rec))
            g-taken?       (some? g)]
        (cond
          missing-actor?
          (let [out (->AskOutcomeRefused :no-such-actor)]
            {:rd-upserts [[ok-key out]] :outcome out})

          ended-actor?
          (let [out (->AskOutcomeRefused :ended)]
            {:rd-upserts [[ok-key out]] :outcome out})

          g-taken?
          (let [out (->AskOutcomeRefused :in-use)]
            {:rd-upserts [[ok-key out]] :outcome out})

          :else
          (let [g-seq (long (:next-sign-in-seq r))
                watched? (> (long until) (long at))
                own-lane (:own-lane r)
                made-at-begin? (boolean (:own-lane-made? r))
                srec (->SignInRec sign-in actor :program hash hash until nil nil
                                  false at asks g-seq own-lane made-at-begin?)
                out (->AskOutcomeYes root actor sign-in)]
            {:rd-upserts (cond-> [[(k-root root) (assoc r :next-sign-in-seq (inc g-seq))]
                                  [(k-signin root sign-in) srec]
                                  [ok-key out]]
                           watched?
                           (conj [(k-exp root until "g" sign-in) (->ExpiryEntry :sign-in sign-in actor until)]))
             :live-upserts (if watched? [[(lk-signin sign-in) (signin-rec->live srec)]] [])
             :outcome out})))

      :else
      (let [a-taken? (or (= actor root) (some? actor-rec))
            g-taken? (some? g)]
        (if (or a-taken? g-taken?)
          (let [out (->AskOutcomeRefused :in-use)]
            {:rd-upserts [[ok-key out]] :outcome out})
          (let [a-seq (long (:next-actor-seq r))
                g-seq (long (:next-sign-in-seq r))
                watched? (> (long until) (long at))
                own-lane (:own-lane r)
                made-at-begin? (boolean (:own-lane-made? r))
                arec (->ActorRec actor root root 1 new-agent-label false a-seq)
                aorec (->ActorOrderRec actor root root 1 new-agent-label false a-seq)
                srec (->SignInRec sign-in actor :program hash hash until nil nil
                                  false at asks g-seq own-lane made-at-begin?)
                out (->AskOutcomeYes root actor sign-in)]
            {:rd-upserts (cond-> [[(k-root root) (assoc r :next-actor-seq (inc a-seq) :next-sign-in-seq (inc g-seq))]
                                  [(k-actor root actor) arec]
                                  [(k-ao root a-seq) aorec]
                                  [(k-signin root sign-in) srec]
                                  [ok-key out]]
                           watched?
                           (conj [(k-exp root until "g" sign-in) (->ExpiryEntry :sign-in sign-in actor until)]))
             :live-upserts (if watched? [[(lk-signin sign-in) (signin-rec->live srec)]] [])
             :outcome out}))))))

(defn plan-outcome
  [plan]
  (:outcome plan))

(defn no-such-ask-outcome
  "Recorded for the replay safety of Step 1b."
  []
  (->AskOutcomeRefused :no-such-ask))

(defn answer-ask-step-3
  "On H(U) (Revision 4). Helpers (`op != self-op`) on Yes/No ALWAYS hop to
  H(self-root) for the thinning at `(:at ev)` (Witness 10)."
  [^AnswerAskEvent ev st k]
  (let [U (:code ev)
        self-op (:op-id ev)
        target (:target st)
        op (:op-id target)
        out (:outcome st)
        still-op? (and (instance? AskAnswering k) (= (:op-id k) op))
        helping? (not= op self-op)]
    (cond
      (instance? AskOutcomeYes out)
      (let [res {:yes {:root (:root out) :actor (:actor out) :sign-in (:sign-in out)}}
            base (cond-> {}
                   still-op?
                   (assoc :write-k U
                          :write-v (->AskYes (:hash k) (:asking-hash k) (:asks k) (:ends k)
                                             (:root out) (:actor out) (:sign-in out))))]
        (if helping?
          (assoc base
                 :hop-key (:root ev)
                 :next-step :answer-ask/step-1b
                 :next-st {:takeout-root (:root ev) :takeout-at (:at ev)
                           :pending-result res :no-such-ask? false})
          (assoc base :done? true :result res)))

      (instance? AskOutcomeNo out)
      (let [res {:no U}
            base (cond-> {}
                   still-op?
                   (assoc :write-k U
                          :write-v (->AskNo (:hash k) (:asking-hash k) (:asks k) (:ends k))))]
        (if helping?
          (assoc base
                 :hop-key (:root ev)
                 :next-step :answer-ask/step-1b
                 :next-st {:takeout-root (:root ev) :takeout-at (:at ev)
                           :pending-result res :no-such-ask? false})
          (assoc base :done? true :result res)))

      (instance? AskOutcomeRefused out)
      (let [base (cond-> {}
                   still-op?
                   (assoc :write-k U
                          :write-v (->AskWaiting (:hash k) (:asking-hash k) (:asks k) (:ends k)
                                                 (:born-over? k))))]
        (if helping?
          (assoc base :hop-key nil :next-step :answer-ask/step-1 :next-st {})
          (assoc base :done? true :result {:refused (:reason out)}))))))

;; --------------------------------------------------- Read projections

(defn signin-fast-path?
  "True lets `who-is` skip reading `r`."
  [sig]
  (boolean (and (instance? SignInRec sig) (:own-lane-made-at-begin? sig))))

(defn root-entered?
  [r]
  (instance? RootEntered r))

(defn other-signin-of-session
  "The sign-in of the session `sess` when it is another than `sign-in`, the
  one that `who-is` was asked for, and nobody closed the session: `who-is`
  then reads that sign-in's record too, to say whether the session is
  closed."
  [sess sign-in]
  (when (and (instance? SessionRec sess) (not (:closed? sess)) (not= (:sign-in sess) sign-in))
    (:sign-in sess)))

(defn build-who-is-result
  "`actor-rec` is the record of `(signin-agent root sig)`. `sess-sig` is the
  record of the session's own sign-in, which is `sig` unless
  `other-signin-of-session`, and `sess-actor-rec` that of its agent.
  `:ended?` and `:closed?` say that someone ended the thing, at whichever
  record that is written; neither says that a time has passed."
  [root sig r sess actor-rec sess-sig sess-actor-rec]
  (let [fast? (signin-fast-path? sig)
        entered? (or fast? (instance? RootEntered r))]
    (if-not entered?
      {:sign-in nil :session nil :own-lane nil :own-lane-made? false}
      (let [own-lane (if fast? (:own-lane sig) (:own-lane r))
            made? (if fast? true (boolean (:own-lane-made? r)))]
        {:sign-in (when (instance? SignInRec sig)
                    {:actor (:actor sig)
                     :kind (:kind sig)
                     :hash (:hash sig)
                     :until (:until sig)
                     :previous (when (:prev-hash sig)
                                 {:hash (:prev-hash sig) :until (:prev-until sig)})
                     :ended? (signin-ended? root sig actor-rec)
                     :actor-ended? (boolean (actor-over? root sig actor-rec))})
         :session (when (instance? SessionRec sess)
                    {:sign-in (:sign-in sess)
                     :until (:until sess)
                     :closed? (boolean (or (:closed? sess) (signin-ended? root sess-sig sess-actor-rec)))
                     :driven-by (from-driven-by (:driven-by sess))})
         :own-lane own-lane
         :own-lane-made? made?}))))

(defn root-has-actors?
  [r]
  (boolean (and (instance? RootEntered r) (pos? (long (:next-actor-seq r))))))

(defn root-has-signins?
  [r]
  (boolean (and (instance? RootEntered r) (pos? (long (:next-sign-in-seq r))))))

(defn build-under-result
  "`:actors` holds live and ended agents, in the order in which they were
  made (`:seq`); `:sign-ins` holds the watched sign-ins only."
  [ao-map ae-map live-g-map]
  {:actors (if (or (seq ao-map) (seq ae-map))
             (->> (concat (vals ao-map) (vals ae-map))
                  (sort-by :seq)
                  (mapv (fn [^ActorOrderRec ao]
                          {:actor (:actor ao)
                           :made-by (:made-by ao)
                           :ends-with (:ends-with ao)
                           :label (:label ao)
                           :ended? (boolean (:ended? ao))})))
             [])
   :sign-ins (if (seq live-g-map)
               (->> (vals live-g-map)
                    (sort-by :seq)
                    (mapv (fn [^LiveSignIn g]
                            {:sign-in (:sign-in g)
                             :actor (:actor g)
                             :kind (:kind g)
                             :label (:label g)
                             :begun (:begun g)
                             :until (:until g)})))
               [])})

(defn project-ask
  [k]
  (cond
    (nil? k) nil
    (or (instance? AskWaiting k) (instance? AskAnswering k))
    {:hash (:hash k) :asking-hash (:asking-hash k) :asks (:asks k) :ends (:ends k) :state :waiting :answer nil}
    (instance? AskYes k)
    {:hash (:hash k) :asking-hash (:asking-hash k) :asks (:asks k) :ends (:ends k) :state :yes
     :answer {:root (:root k) :actor (:actor k) :sign-in (:sign-in k)}}
    (instance? AskNo k)
    {:hash (:hash k) :asking-hash (:asking-hash k) :asks (:asks k) :ends (:ends k) :state :no :answer nil}))

(defn- invited-by-of
  [kind id]
  (if (= :system kind) :system id))

(defn project-invitation
  "What `invitation-of` gives of the claim's record `c`: nil when there is
  none. It says nothing of \"over\": the caller holds `:ends` against its own
  time."
  [c]
  (when (instance? Claim c)
    {:person (:person c)
     :invitation (when-let [w (:waiting c)]
                   {:invite (:invite-op w)
                    :invited-by (invited-by-of (:invited-by-kind w) (:invited-by-id w))
                    :invited-at (:invited-at w)
                    :ends (:ends w)})
     :links (count (:links c))}))

(defn account-order
  "The order of a person's accounts: by the `:at` of the enter that linked
  each, then by the provider's name, its namespace counted, then by the
  subject."
  [{[provider subject] :account linked-at :linked-at}]
  [linked-at (subs (str provider) 1) subject])

(defn project-accounts
  "What `accounts-of` gives of the person's entries (`k-account`), a map
  from key to `PersonAccount`."
  [m]
  {:accounts (->> (vals m)
                  (filter #(instance? PersonAccount %))
                  (map (fn [e] {:account [(:provider e) (:subject e)] :link (:link-op e) :linked-at (:linked-at e)}))
                  (sort-by account-order)
                  vec)})

(def no-live
  "What `watch-root` gives of a root under which nothing is watched and
  nothing was ended."
  {:sign-ins {} :sessions {} :ends 0})

(defn live-with
  "`proj` with the entry of a root's map in `$$live` put in."
  [proj entry]
  (cond
    (instance? LiveSignIn entry)
    (assoc-in proj [:sign-ins (:sign-in entry)]
              {:actor (:actor entry)
               :kind (:kind entry)
               :hash (:hash entry)
               :until (:until entry)
               :previous (when (:prev-hash entry)
                           {:hash (:prev-hash entry) :until (:prev-until entry)})})

    (instance? LiveSession entry)
    (assoc-in proj [:sessions (:session entry)]
              {:sign-in (:sign-in entry)
               :until (:until entry)})

    (instance? LiveEnds entry)
    (assoc proj :ends (:n entry))

    :else proj))

(defn live-without
  "`proj` without the sign-in or the session of the inner key `k`. The
  count of ends is never taken out."
  [proj k]
  (if (and (string? k) (> (.length ^String k) 2) (= \: (.charAt ^String k 1)))
    (let [id (try (UUID/fromString (.substring ^String k 2)) (catch Throwable _ nil))
          section (case (.charAt ^String k 0)
                    \g :sign-ins
                    \s :sessions
                    nil)]
      (if (and id section)
        (update proj section dissoc id)
        proj))
    proj))

(defn project-live-map
  [m]
  (reduce-kv (fn [acc _ v] (live-with acc v)) no-live (or m {})))
