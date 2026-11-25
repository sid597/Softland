(ns softland.auth.schema
  "Typed records, key encoders, projections, and pure single-task transition
  functions for the Rama module `Auth`. Pure code.

  Every Rama depot record and PState value implements a closed marker interface
  (`IRootDatum`, `ILiveEntry`, `IAccountState`, `IClaimState`, `IAskState`,
  `IEnterOpState`, `IDrivenBy`) so no PState schema uses `Object` and no live
  watched record holds a byte array. All range-scanned keys are strictly 7-bit
  ASCII so RocksDB UTF-8 byte order matches JVM UTF-16 order identically: the
  composite keys of `$$root-data` (`\"<P>\\0...\"`), and the inner keys of a
  root's map in `$$live` (`\"g:<G>\"` for a sign-in, `\"s:<S>\"` for a
  session).

  The pure transition functions for `invite`, `enter`, and `answer-ask` are
  written from the step specification that was model-checked in
  `src/proposal/runtime-2026-11-21/spine/auth/agreement/`, step for step, but
  they are not that code, and no model check has run them: what checks them
  is `softland.auth.module-check`, through the module, against the
  contract's reference (`softland.auth.module` says how far that reaches)."
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

;; *invites (partitioned by :claim-key String)
(defrecord InviteEvent [person claim-key claim-kind claim-val invited-by-kind invited-by-id at])

;; *enters (partitioned by :account-key String)
(defrecord EnterEvent [op-id account-key provider subject says email code-hash
                       own-lane making-uuid making-session at])

;; *asks (partitioned by :code String)
(defrecord AskEvent [code hash asks ends at] IAskEvent)
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
(defrecord RootReserving [claim-key claim-kind claim-val invited-by-kind invited-by-id invited-at]
  IRootDatum)
(defrecord RootInvited [claim-key claim-kind claim-val invited-by-kind invited-by-id invited-at]
  IRootDatum)
(defrecord RootEntered [claim-key claim-kind claim-val invited-by-kind invited-by-id invited-at
                        linked-provider linked-subject own-lane making-uuid making-session
                        own-lane-made? next-actor-seq next-sign-in-seq]
  IRootDatum)
(defrecord ActorRec [actor made-by depth label ended? seq]
  IRootDatum)
(defrecord ActorOrderRec [actor made-by depth label ended? seq]
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
(defrecord LiveSession [session sign-in until driven-by]
  ILiveEntry)

;; $$accounts {String -> IAccountState} (partitioned by |hash account-key)
(defrecord AccountLinking [op-id account-key provider subject code-claim-key email-claim-key
                           own-lane making-uuid making-session says at status chosen]
  IAccountState)
(defrecord AccountLinked [person own-lane making-uuid making-session says says-at]
  IAccountState)

;; $$claims {String -> IClaimState} (partitioned by |hash claim-key)
(defrecord ClaimWaiting [person invited-by-kind invited-by-id invited-at]
  IClaimState)
(defrecord ClaimUsed [person invited-by-kind invited-by-id invited-at used-by-op]
  IClaimState)

;; $$asks {String -> IAskState} (partitioned by |hash code)
(defrecord AskWaiting [hash asks ends born-over?]
  IAskState)
(defrecord AskAnswering [hash asks ends born-over? op-id yes? root actor new-agent-label
                         sign-in until at]
  IAskState)
(defrecord AskYes [hash asks ends root actor sign-in]
  IAskState)
(defrecord AskNo [hash asks ends]
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
  [kind ^String val]
  (case kind
    :email (str "e:" val)
    :code  (str "c:" val)))

(defn claim-key-from-map
  [claim]
  (case (:by claim)
    :email (str "e:" (:email claim))
    :code  (str "c:" (:hash claim))))

(defn claim-val-from-map
  [claim]
  (case (:by claim)
    :email (:email claim)
    :code  (:hash claim)))

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
  `k-ae` when it ends: so `end-actor`, which scans for the live agents made
  after the one it ends, reads no agent that ended before."
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

;; ------------------------------------------- inner keys of a root in $$live

(defn lk-signin
  ^String [^UUID g]
  (str "g:" g))

(defn lk-session
  ^String [^UUID s]
  (str "s:" s))

(def live-lo
  "")

(def live-hi
  "Exclusive. DEL (0x7f): above every printable 7-bit ASCII character."
  "\u007f")

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
     (and (uuid? (:person ev)) (string? (:claim-key ev)) (contains? #{:email :code} (:claim-kind ev))
          (string? (:claim-val ev)) (contains? #{:system :actor} (:invited-by-kind ev))
          (or (nil? (:invited-by-id ev)) (uuid? (:invited-by-id ev))) (long-val? (:at ev)))

     (instance? EnterEvent ev)
     (and (uuid? (:op-id ev)) (string? (:account-key ev)) (keyword? (:provider ev)) (string? (:subject ev))
          (said? (:says ev)) (text-or-nil? (:email ev)) (text-or-nil? (:code-hash ev))
          (uuid? (:own-lane ev)) (uuid? (:making-uuid ev)) (uuid? (:making-session ev)) (long-val? (:at ev)))

     (instance? AskEvent ev)
     (and (string? (:code ev)) (string? (:hash ev)) (string? (:asks ev)) (long-val? (:ends ev)) (long-val? (:at ev)))

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
  (and (instance? InviteEvent ev) (record-fits? ev)))

(defn enter-record-fits?
  [ev]
  (and (instance? EnterEvent ev) (record-fits? ev)))

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

;; ------------------------------------------------------- Slice a: invite

(defn invite-step-1
  "On H(C) (Revision 1). `:claim-in-use` hops to H(P) to clean up any
  orphaned `RootReserving(C)` (Witness 6)."
  [^InviteEvent ev c]
  (let [P (:person ev)]
    (cond
      (nil? c)
      {:hop-key P :next-step :invite/step-2 :next-st {}}

      (= (:person c) P)
      {:hop-key P :next-step :invite/step-4 :next-st {}}

      :else
      {:hop-key P :next-step :invite/step-3-clear :next-st {}})))

(defn invite-step-2
  "On H(P) (Revision 1). Before refusing `:person-in-use`, hops to H(C) to
  re-check `$$claims[C]`, so `:claim-in-use` always precedes
  `:person-in-use` (Witness 9)."
  [^InviteEvent ev r]
  (let [P (:person ev)
        C (:claim-key ev)]
    (cond
      (or (instance? RootInvited r) (instance? RootEntered r))
      (if (= (:claim-key r) C)
        {:done? true :result {:invited P}}
        {:hop-key C :next-step :invite/step-2-recheck :next-st {}})

      (nil? r)
      {:write-k (k-root P)
       :write-v (->RootReserving C (:claim-kind ev) (:claim-val ev)
                                 (:invited-by-kind ev) (:invited-by-id ev) (:at ev))
       :hop-key C :next-step :invite/step-3 :next-st {}}

      (instance? RootReserving r)
      (let [c0 (:claim-key r)]
        (if (= c0 C)
          {:hop-key C :next-step :invite/step-3 :next-st {}}
          {:hop-key c0
           :next-step :invite/step-2-help-claim
           :next-st {:help-claim c0
                     :help-kind (:claim-kind r)
                     :help-val (:claim-val r)
                     :help-by-kind (:invited-by-kind r)
                     :help-by-id (:invited-by-id r)
                     :help-at (:invited-at r)}})))))

(defn invite-step-2-help-claim
  "On H(C0): helps a competing `RootReserving(C0)` commit or observe loss on
  `$$claims[C0]`."
  [^InviteEvent ev st c0]
  (let [P (:person ev)
        hc (:help-claim st)]
    (cond
      (nil? c0)
      {:write-k hc
       :write-v (->ClaimWaiting P (:help-by-kind st) (:help-by-id st) (:help-at st))
       :hop-key P
       :next-step :invite/step-2-help-root
       :next-st (assoc st :other-won? true)}

      (= (:person c0) P)
      {:hop-key P
       :next-step :invite/step-2-help-root
       :next-st (assoc st :other-won? true)}

      :else
      {:hop-key P
       :next-step :invite/step-2-help-root
       :next-st (assoc st :other-won? false)})))

(defn invite-step-2-help-root
  "On H(P): promotes won `C0` (then rechecks `C` for Witness 9) or evicts
  lost `C0`."
  [^InviteEvent ev st r]
  (let [P (:person ev)
        C (:claim-key ev)
        hc (:help-claim st)]
    (if (:other-won? st)
      (let [promote? (and (instance? RootReserving r) (= (:claim-key r) hc))]
        (cond-> {:hop-key C :next-step :invite/step-2-recheck :next-st {}}
          promote?
          (assoc :write-k (k-root P)
                 :write-v (->RootInvited hc (:help-kind st) (:help-val st)
                                         (:help-by-kind st) (:help-by-id st) (:help-at st)))))
      (if (and (instance? RootReserving r) (= (:claim-key r) hc))
        {:write-k (k-root P)
         :write-v (->RootReserving C (:claim-kind ev) (:claim-val ev)
                                   (:invited-by-kind ev) (:invited-by-id ev) (:at ev))
         :hop-key C :next-step :invite/step-3 :next-st {}}
        ;; Root changed meanwhile -> re-run Step 2 locally on H(P)
        (invite-step-2 ev r)))))

(defn invite-step-2-recheck
  "On H(C)."
  [^InviteEvent ev c]
  (let [P (:person ev)]
    (cond
      (nil? c)          {:done? true :result {:refused :person-in-use}}
      (= (:person c) P) {:hop-key P :next-step :invite/step-4 :next-st {}}
      :else             {:done? true :result {:refused :claim-in-use}})))

(defn invite-step-3
  "On H(C)."
  [^InviteEvent ev c]
  (let [P (:person ev)
        C (:claim-key ev)]
    (cond
      (nil? c)
      {:write-k C
       :write-v (->ClaimWaiting P (:invited-by-kind ev) (:invited-by-id ev) (:at ev))
       :hop-key P :next-step :invite/step-4 :next-st {}}

      (= (:person c) P)
      {:hop-key P :next-step :invite/step-4 :next-st {}}

      :else
      {:hop-key P :next-step :invite/step-3-clear :next-st {}})))

(defn invite-step-3-clear
  "On H(P)."
  [^InviteEvent ev r]
  (let [P (:person ev)
        clear? (and (instance? RootReserving r) (= (:claim-key r) (:claim-key ev)))]
    (cond-> {:done? true :result {:refused :claim-in-use}}
      clear? (assoc :write-k (k-root P) :delete? true))))

(defn invite-step-4
  "On H(P)."
  [^InviteEvent ev r]
  (let [P (:person ev)
        C (:claim-key ev)
        promote? (or (nil? r) (and (instance? RootReserving r) (= (:claim-key r) C)))]
    (cond-> {:done? true :result {:invited P}}
      promote?
      (assoc :write-k (k-root P)
             :write-v (->RootInvited C (:claim-kind ev) (:claim-val ev)
                                     (:invited-by-kind ev) (:invited-by-id ev) (:at ev))))))

;; ------------------------------------------------------- Slice a: enter

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
   :chosen (:chosen acc)})

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
  "Step E1, on H(Acc) (Revisions 1 & 5)."
  [^EnterEvent ev acc]
  (let [ak (:account-key ev)
        self-op (:op-id ev)
        cc (when-let [h (:code-hash ev)] (claim-key-from-parts :code h))
        ec (when-let [e (:email ev)] (claim-key-from-parts :email e))]
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

      (nil? acc)
      (if (and (nil? cc) (nil? ec))
        {:hop-key self-op
         :next-step :enter/step-2c-mark-failed-direct
         :next-st {}}
        (let [linking (->AccountLinking self-op ak (:provider ev) (:subject ev)
                                        cc ec (:own-lane ev) (:making-uuid ev)
                                        (:making-session ev) (:says ev) (:at ev)
                                        :probing nil)]
          (assoc (enter-route-linking linking)
                 :write-k ak
                 :write-v linking)))

      (instance? AccountLinking acc)
      (enter-route-linking acc))))

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
  {:softland/whose-call
   [{:state :decided
     :what "Step E1c of `enter` updates `:says` and `:says-at` of a linked
            account iff `(> at says-at)`."
     :source "Sid's Call 2"}]}
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
  with `:code-recheck` when `:code` was `nil` and `:email` was `ClaimUsed`."
  [^EnterEvent ev st c]
  (let [target (:target st)
        op (:op-id target)
        phase (:phase st)
        ck (:curr-claim st)
        ak (:account-key ev)]
    (cond
      (or (instance? ClaimWaiting c)
          (and (instance? ClaimUsed c) (= (:used-by-op c) op)))
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

      (and (= :email phase) (:code-was-nil? st) (instance? ClaimUsed c))
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
  "Step E2', on H(Acc)."
  [^EnterEvent ev st acc]
  (let [target (:target st)
        op (:op-id target)
        probed (:probed st)
        ak (:account-key ev)]
    (cond
      (not (and (instance? AccountLinking acc) (= (:op-id acc) op)))
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
      ;; Helper: clear op first, then re-evaluate self at Step E1 on H(Acc)
      ;; Note: if we cleared acc, we must hop/continue locally with acc=nil,
      ;; so we let the topology apply delete and continue> to :enter/step-1!
      (cond-> {:hop-key nil :next-step :enter/step-1 :next-st {}}
        clear? (assoc :write-k ak :delete? true)))))

(defn enter-step-3-use
  "Step E3, on H(curr-claim)."
  [^EnterEvent ev st c]
  (let [target (:target st)
        op (:op-id target)
        ck (:curr-claim st)
        ak (:account-key ev)]
    (cond
      (instance? ClaimWaiting c)
      (let [P (:person c)]
        {:write-k ck
         :write-v (->ClaimUsed P (:invited-by-kind c) (:invited-by-id c) (:invited-at c) op)
         :hop-key P
         :next-step :enter/step-4-root
         :next-st {:target (assoc target :won-person P :won-claim-key ck)}})

      (and (instance? ClaimUsed c) (= (:used-by-op c) op))
      (let [P (:person c)]
        {:hop-key P
         :next-step :enter/step-4-root
         :next-st {:target (assoc target :won-person P :won-claim-key ck)}})

      :else
      {:hop-key ak
       :next-step :enter/step-3b-clear-chosen
       :next-st {:target target :failed-claim ck}})))

(defn enter-step-3b-clear-chosen
  "Step E3b, on H(Acc)."
  [^EnterEvent ev st acc]
  (let [op (:op-id (:target st))
        failed-claim (:failed-claim st)
        ak (:account-key ev)
        reset? (and (instance? AccountLinking acc)
                    (= (:op-id acc) op)
                    (= :chosen (:status acc))
                    (= (:chosen acc) failed-claim))]
    (cond-> {:hop-key nil :next-step :enter/step-1 :next-st {}}
      reset? (assoc :write-k ak :write-v (assoc acc :status :probing :chosen nil)))))

(defn enter-step-4-root
  "Step E4, on H(P)."
  [^EnterEvent ev st r]
  (let [target (:target st)
        P (:won-person target)
        entered-already? (instance? RootEntered r)
        r' (if entered-already?
             r
             (->RootEntered (:claim-key r) (:claim-kind r) (:claim-val r)
                            (:invited-by-kind r) (:invited-by-id r) (:invited-at r)
                            (:provider target) (:subject target)
                            (:own-lane target) (:making-uuid target) (:making-session target)
                            false 0 0))]
    (cond-> {:hop-key (:account-key ev)
             :next-step :enter/step-5-link
             :next-st {:target (assoc target
                                      :fixed-lane (:own-lane r')
                                      :fixed-making-uuid (:making-uuid r')
                                      :fixed-making-session (:making-session r')
                                      :present-flag (boolean (:own-lane-made? r')))}}
      (not entered-already?)
      (assoc :write-k (k-root P) :write-v r'))))

(defn enter-step-5-link
  "Step E5, on H(Acc). A helper with a strictly later `:at` hops to
  H(self-op) (`E1a`) to verify not refused before updating `:says`."
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
                      (->AccountLinked P fl fmu fms (:says target) (:at target))
                      (instance? AccountLinked acc)
                      acc
                      :else nil)
        need-self-says? (and (some? linked-base)
                             (> (long (:at ev)) (long (:says-at linked-base))))]
    (cond
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

(def ^:private
  ^{:softland/whose-call
    [{:state :decided
      :what "The most makers below a root is 32, matching `SPEC.md` and
             `reference.clj`. Revision 7 is closed for this build."
      :source "Revision 7, which names nobody;
               `src/proposal/runtime-2026-11-21/spine/auth/BUILD.md` says
               \"Closed for this build per Sid's instruction\""}]}
  max-actor-depth
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

(defn plan-take-out
  "The caller gives a non-empty `exp-map`, the `exp:` entries with
  `until <= at`, and `live-map`, the root's map in `$$live`. An expired
  sign-in closes its open sessions even when a session's `:until > at`."
  [root exp-map live-map]
  (let [exp-gs (volatile! #{})
        exp-ss (volatile! #{})
        rd-deletes (volatile! (set (keys exp-map)))
        rd-upserts (transient [])
        live-deletes (transient [])]
    (doseq [[_ ^ExpiryEntry e] exp-map]
      (case (:kind e)
        :sign-in (vswap! exp-gs conj (:id e))
        :session (vswap! exp-ss conj (:id e))))
    (let [gs @exp-gs
          ss @exp-ss]
      (doseq [[lk lv] live-map]
        (when (instance? LiveSignIn lv)
          (when (contains? gs (:sign-in lv))
            (conj! live-deletes lk)
            (conj! rd-upserts [(k-signin root (:sign-in lv)) (live-signin->ended-rec lv)]))))
      (doseq [[lk lv] live-map]
        (when (instance? LiveSession lv)
          (let [sid (:session lv)]
            (when (or (contains? ss sid) (contains? gs (:sign-in lv)))
              (conj! live-deletes lk)
              (conj! rd-upserts [(k-session root sid) (live-session->closed-rec lv)])
              (vswap! rd-deletes conj (k-exp root (:until lv) "s" sid)))))))
    {:rd-upserts (persistent! rd-upserts)
     :rd-deletes (vec @rd-deletes)
     :live-upserts []
     :live-deletes (persistent! live-deletes)}))

(defn plan-begin-sign-in
  "On H(P), after take-out."
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
            born-ended? (<= until at)
            own-lane (:own-lane r)
            made-at-begin? (boolean (:own-lane-made? r))
            rec (->SignInRec G A kind h h until nil nil born-ended? at lbl seq-n own-lane made-at-begin?)
            r' (assoc r :next-sign-in-seq (inc seq-n))]
        {:rd-upserts (cond-> [[(k-root P) r']
                              [(k-signin P G) rec]]
                       (not born-ended?)
                       (conj [(k-exp P until "g" G) (->ExpiryEntry :sign-in G A until)]))
         :live-upserts (if born-ended?
                         []
                         [[(lk-signin G) (signin-rec->live rec)]])
         :result {:begun G}}))))

(defn replace-secret-born-ended?
  "True when an accepted `replace-secret` ends `g` at once, because
  `(:until ev) <= (:at ev)`: the caller must then scan `s:` in `$$live` to
  close the open sessions."
  [^ReplaceSecretEvent ev g]
  (boolean (and (instance? SignInRec g)
                (not (and (= (:hash g) (:new-hash ev))
                          (= (:prev-hash g) (:old-hash ev))))
                (not (:ended? g))
                (= (:hash g) (:old-hash ev))
                (<= (long (:until ev)) (long (:at ev))))))

(defn plan-replace-secret
  "On H(P), after take-out. `live-s-map` is needed only when
  `replace-secret-born-ended?` is true."
  [^ReplaceSecretEvent ev g live-s-map]
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

      (or (not (instance? SignInRec g)) (:ended? g))
      {:result {:refused :not-live}}

      (not= (:hash g) old-h)
      {:result {:refused :wrong-secret}}

      :else
      (let [old-until (long (:until g))
            born-ended? (<= until at)
            g' (assoc g :hash new-h :until until
                      :prev-hash old-h :prev-until prev-until
                      :ended? born-ended?)]
        (if born-ended?
          (let [rd-upserts (transient [[(k-signin P G) g']])
                rd-deletes (transient [(k-exp P old-until "g" G)])
                live-deletes (transient [(lk-signin G)])]
            (doseq [[lk ^LiveSession s] live-s-map]
              (when (= (:sign-in s) G)
                (conj! live-deletes lk)
                (conj! rd-upserts [(k-session P (:session s)) (live-session->closed-rec s)])
                (conj! rd-deletes (k-exp P (:until s) "s" (:session s)))))
            {:rd-upserts (persistent! rd-upserts)
             :rd-deletes (persistent! rd-deletes)
             :live-deletes (persistent! live-deletes)
             :result {:replaced G}})
          {:rd-upserts [[(k-signin P G) g']
                        [(k-exp P until "g" G) (->ExpiryEntry :sign-in G (:actor g) until)]]
           :rd-deletes (if (= until old-until) [] [(k-exp P old-until "g" G)])
           :live-upserts [[(lk-signin G) (signin-rec->live g')]]
           :result {:replaced G}})))))

(defn signin-needs-end?
  [g]
  (boolean (and (instance? SignInRec g) (not (:ended? g)))))

(defn plan-end-sign-in
  "On H(P), after take-out."
  [^EndSignInEvent ev g live-s-map]
  (let [P (:root-id ev)
        G (:sign-in ev)]
    (cond
      (not (instance? SignInRec g))
      {:result {:refused :no-such-sign-in}}

      (:ended? g)
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
         :result {:ended G}}))))

(defn plan-make-actor
  "On H(P), after take-out."
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
                      [(k-actor P A) (->ActorRec A B depth lbl false seq-n)]
                      [(k-ao P seq-n) (->ActorOrderRec A B depth lbl false seq-n)]]
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
  and none that ended before. A live descendant of the target is reached
  through live makers alone: an agent that ended took its whole subtree
  with it."
  [root ^ActorRec target-rec ao-map live-map]
  (let [target-id (:actor target-rec)
        ended-as (volatile! #{target-id})
        ended-gs (volatile! #{})
        rd-upserts (transient [])
        rd-deletes (transient [])
        live-deletes (transient [])]
    ;; Pass 1: single forward scan over ao-map sorted by ascending :seq
    ;; (seq(maker) < seq(child) always holds).
    (doseq [^ActorOrderRec ao (sort-by :seq (vals ao-map))]
      (let [aid (:actor ao)]
        (when (and (not (:ended? ao))
                   (or (= aid target-id)
                       (contains? @ended-as (:made-by ao))))
          (vswap! ended-as conj aid)
          (conj! rd-deletes (k-ao root (:seq ao)))
          (conj! rd-upserts [(k-ae root (:seq ao)) (assoc ao :ended? true)])
          (conj! rd-upserts [(k-actor root aid)
                             (->ActorRec aid (:made-by ao) (:depth ao) (:label ao) true (:seq ao))]))))
    ;; Pass 2a: live sign-ins of any ended actor (0 point seeks on g:).
    (let [as @ended-as]
      (doseq [[lk lv] live-map]
        (when (instance? LiveSignIn lv)
          (when (contains? as (:actor lv))
            (vswap! ended-gs conj (:sign-in lv))
            (conj! live-deletes lk)
            (conj! rd-deletes (k-exp root (:until lv) "g" (:sign-in lv)))
            (conj! rd-upserts [(k-signin root (:sign-in lv)) (live-signin->ended-rec lv)])))))
    ;; Pass 2b: open sessions of any ended sign-in (0 point seeks on s:, preserves :driven-by).
    (let [gs @ended-gs]
      (doseq [[lk lv] live-map]
        (when (instance? LiveSession lv)
          (when (contains? gs (:sign-in lv))
            (conj! live-deletes lk)
            (conj! rd-deletes (k-exp root (:until lv) "s" (:session lv)))
            (conj! rd-upserts [(k-session root (:session lv)) (live-session->closed-rec lv)])))))
    {:rd-upserts (persistent! rd-upserts)
     :rd-deletes (persistent! rd-deletes)
     :live-deletes (persistent! live-deletes)
     :result {:ended target-id}}))

(defn plan-open-session
  "On H(P), after take-out."
  [^OpenSessionEvent ev s g]
  (let [P (:root-id ev)
        G (:sign-in ev)
        S (:session ev)
        until (long (:until ev))
        db (:driven-by ev)
        at (long (:at ev))]
    (cond
      (and (instance? SessionRec s) (= (:sign-in s) G))
      (if (and (not (:closed? s)) (> until (long (:until s))))
        (let [old-until (long (:until s))
              s' (assoc s :until until)]
          {:rd-upserts [[(k-session P S) s']
                        [(k-exp P until "s" S) (->ExpiryEntry :session S G until)]]
           :rd-deletes [(k-exp P old-until "s" S)]
           :live-upserts [[(lk-session S) (->LiveSession S G until (:driven-by s))]]
           :result {:open S}})
        {:result {:open S}})

      (or (not (instance? SignInRec g)) (:ended? g))
      {:result {:refused :not-live}}

      (some? s)
      {:result {:refused :in-use}}

      :else
      (let [born-closed? (<= until at)
            rec (->SessionRec S G until born-closed? db)]
        {:rd-upserts (cond-> [[(k-session P S) rec]]
                       (not born-closed?)
                       (conj [(k-exp P until "s" S) (->ExpiryEntry :session S G until)]))
         :live-upserts (if born-closed?
                         []
                         [[(lk-session S) (->LiveSession S G until db)]])
         :result {:open S}}))))

(defn plan-close-session
  "On H(P), after take-out."
  [^CloseSessionEvent ev s]
  (let [P (:root-id ev)
        S (:session ev)]
    (cond
      (not (instance? SessionRec s))
      {:result {:refused :no-such-session}}

      (:closed? s)
      {:result {:closed S}}

      :else
      {:rd-upserts [[(k-session P S) (assoc s :closed? true)]]
       :rd-deletes [(k-exp P (:until s) "s" S)]
       :live-deletes [(lk-session S)]
       :result {:closed S}})))

;; ------------------------------------------------- Slice c: ask & answer-ask

(defn ask-step
  "On H(U)."
  [^AskEvent ev k]
  (let [U (:code ev)
        h (:hash ev)
        ends (long (:ends ev))
        at (long (:at ev))]
    (cond
      (and (instance? IAskState k) (= (:hash k) h))
      {:done? true :result {:asked U}}

      (some? k)
      {:done? true :result {:refused :in-use}}

      :else
      {:write-k U
       :write-v (->AskWaiting h (:asks ev) ends (<= ends at))
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
        (let [ans (->AskAnswering (:hash k) (:asks k) (:ends k) (:born-over? k)
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
  absent and ran `take-out(P_target, target.at)`."
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
                born-ended? (<= (long until) (long at))
                own-lane (:own-lane r)
                made-at-begin? (boolean (:own-lane-made? r))
                srec (->SignInRec sign-in actor :program hash hash until nil nil
                                  born-ended? at asks g-seq own-lane made-at-begin?)
                out (->AskOutcomeYes root actor sign-in)]
            {:rd-upserts (cond-> [[(k-root root) (assoc r :next-sign-in-seq (inc g-seq))]
                                  [(k-signin root sign-in) srec]
                                  [ok-key out]]
                           (not born-ended?)
                           (conj [(k-exp root until "g" sign-in) (->ExpiryEntry :sign-in sign-in actor until)]))
             :live-upserts (if born-ended? [] [[(lk-signin sign-in) (signin-rec->live srec)]])
             :outcome out})))

      :else
      (let [a-taken? (or (= actor root) (some? actor-rec))
            g-taken? (some? g)]
        (if (or a-taken? g-taken?)
          (let [out (->AskOutcomeRefused :in-use)]
            {:rd-upserts [[ok-key out]] :outcome out})
          (let [a-seq (long (:next-actor-seq r))
                g-seq (long (:next-sign-in-seq r))
                born-ended? (<= (long until) (long at))
                own-lane (:own-lane r)
                made-at-begin? (boolean (:own-lane-made? r))
                arec (->ActorRec actor root 1 new-agent-label false a-seq)
                aorec (->ActorOrderRec actor root 1 new-agent-label false a-seq)
                srec (->SignInRec sign-in actor :program hash hash until nil nil
                                  born-ended? at asks g-seq own-lane made-at-begin?)
                out (->AskOutcomeYes root actor sign-in)]
            {:rd-upserts (cond-> [[(k-root root) (assoc r :next-actor-seq (inc a-seq) :next-sign-in-seq (inc g-seq))]
                                  [(k-actor root actor) arec]
                                  [(k-ao root a-seq) aorec]
                                  [(k-signin root sign-in) srec]
                                  [ok-key out]]
                           (not born-ended?)
                           (conj [(k-exp root until "g" sign-in) (->ExpiryEntry :sign-in sign-in actor until)]))
             :live-upserts (if born-ended? [] [[(lk-signin sign-in) (signin-rec->live srec)]])
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
  H(self-root) for take-out at `(:at ev)` (Witness 10)."
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
                          :write-v (->AskYes (:hash k) (:asks k) (:ends k)
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
                          :write-v (->AskNo (:hash k) (:asks k) (:ends k))))]
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
                          :write-v (->AskWaiting (:hash k) (:asks k) (:ends k) (:born-over? k))))]
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

(defn need-actor-seek?
  "True only when `sig` is an ended sign-in of an agent: `who-is` must then
  read `a:<A>`."
  [root sig]
  (boolean (and (instance? SignInRec sig)
                (:ended? sig)
                (not= (:actor sig) root))))

(defn signin-actor
  [^SignInRec sig]
  (:actor sig))

(defn build-who-is-result
  [root sig r sess actor-rec]
  (let [fast? (signin-fast-path? sig)
        entered? (or fast? (instance? RootEntered r))]
    (if-not entered?
      {:sign-in nil :session nil :own-lane nil :own-lane-made? false}
      (let [own-lane (if fast? (:own-lane sig) (:own-lane r))
            made? (if fast? true (boolean (:own-lane-made? r)))
            actor-ended? (if (and (instance? SignInRec sig)
                                  (:ended? sig)
                                  (not= (:actor sig) root))
                           (boolean (when (instance? ActorRec actor-rec) (:ended? actor-rec)))
                           false)]
        {:sign-in (when (instance? SignInRec sig)
                    {:actor (:actor sig)
                     :kind (:kind sig)
                     :hash (:hash sig)
                     :until (:until sig)
                     :previous (when (:prev-hash sig)
                                 {:hash (:prev-hash sig) :until (:prev-until sig)})
                     :ended? (boolean (:ended? sig))
                     :actor-ended? actor-ended?})
         :session (when (instance? SessionRec sess)
                    {:sign-in (:sign-in sess)
                     :until (:until sess)
                     :closed? (boolean (:closed? sess))
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
  made (`:seq`); `:sign-ins` holds live sign-ins only."
  [ao-map ae-map live-g-map]
  {:actors (if (or (seq ao-map) (seq ae-map))
             (->> (concat (vals ao-map) (vals ae-map))
                  (sort-by :seq)
                  (mapv (fn [^ActorOrderRec ao]
                          {:actor (:actor ao)
                           :made-by (:made-by ao)
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
    {:hash (:hash k) :asks (:asks k) :ends (:ends k) :state :waiting :answer nil}
    (instance? AskYes k)
    {:hash (:hash k) :asks (:asks k) :ends (:ends k) :state :yes
     :answer {:root (:root k) :actor (:actor k) :sign-in (:sign-in k)}}
    (instance? AskNo k)
    {:hash (:hash k) :asks (:asks k) :ends (:ends k) :state :no :answer nil}))

(defn project-live-entry
  [entry]
  (cond
    (instance? LiveSignIn entry)
    [:sign-ins (:sign-in entry)
     {:actor (:actor entry)
      :kind (:kind entry)
      :hash (:hash entry)
      :until (:until entry)
      :previous (when (:prev-hash entry)
                  {:hash (:prev-hash entry) :until (:prev-until entry)})}]

    (instance? LiveSession entry)
    [:sessions (:session entry)
     {:sign-in (:sign-in entry)
      :until (:until entry)}]))

(defn parse-live-key
  [k]
  (when (and (string? k) (> (.length ^String k) 2) (= \: (.charAt ^String k 1)))
    (let [id (try (UUID/fromString (.substring ^String k 2)) (catch Throwable _ nil))]
      (when id
        (case (.charAt ^String k 0)
          \g [:sign-ins id]
          \s [:sessions id]
          nil)))))

(defn project-live-map
  [m]
  (reduce-kv
   (fn [acc _ v]
     (if-let [[section id proj] (project-live-entry v)]
       (assoc-in acc [section id] proj)
       acc))
   {:sign-ins {} :sessions {}}
   (or m {})))
