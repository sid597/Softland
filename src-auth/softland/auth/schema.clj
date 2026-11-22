(ns softland.auth.schema
  "Typed records, key encoders, projections, and pure single-task transition
  functions for the Rama module `Auth`.

  Every Rama depot record and PState value implements a closed marker interface
  (`IRootDatum`, `ILiveEntry`, `IAccountState`, `IClaimState`, `IAskState`,
  `IEnterOpState`, `IDrivenBy`) so no PState schema uses `Object` and no live
  watched record holds a byte array. All range-scanned composite keys in
  `$$root-data` and `$$live` are strictly 7-bit ASCII so RocksDB UTF-8 byte
  order matches JVM UTF-16 order identically.

  The pure transition functions for `invite`, `enter`, and `answer-ask`
  mirror the model-checked step specification in
  `src/proposal/runtime-2026-11-21/spine/auth/agreement/` one-for-one.

  State and effects: none. Pure records and functions only."
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
  "Converts a contract `:driven-by` map (`{:kind :person}`,
  `{:kind :model :model s}`, or `nil`) into its typed `IDrivenBy` record or `nil`."
  [m]
  (when m
    (case (:kind m)
      :person (->DrivenByPerson)
      :model (->DrivenByModel (:model m)))))

(defn from-driven-by
  "Converts a stored `IDrivenBy` record (or `nil`) back into its contract map."
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
                       own-layer making-uuid making-session at])

;; *asks (partitioned by :code String)
(defrecord AskEvent [code hash asks ends at] IAskEvent)
(defrecord AnswerAskEvent [op-id code yes? root actor new-agent-label sign-in until at] IAskEvent)

;; *root-events (partitioned by :root-id UUID)
(defrecord OwnLayerMadeEvent [root-id at] IRootEvent)
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
                        linked-provider linked-subject own-layer making-uuid making-session
                        own-layer-made? next-actor-seq next-sign-in-seq]
  IRootDatum)
(defrecord ActorRec [actor made-by depth label ended? seq]
  IRootDatum)
(defrecord ActorOrderRec [actor made-by depth label ended? seq]
  IRootDatum)
(defrecord SignInRec [sign-in actor kind initial-hash hash until prev-hash prev-until
                      ended? begun label seq own-layer own-layer-made-at-begin?]
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
                       begun label seq own-layer own-layer-made-at-begin?]
  ILiveEntry)
(defrecord LiveSession [session sign-in until driven-by]
  ILiveEntry)

;; $$accounts {String -> IAccountState} (partitioned by |hash account-key)
(defrecord AccountLinking [op-id account-key provider subject code-claim-key email-claim-key
                           own-layer making-uuid making-session says at status chosen]
  IAccountState)
(defrecord AccountLinked [person own-layer making-uuid making-session says says-at]
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
  "Bijective string key for `[provider subject]`. Length-prefixes the full
  provider keyword text so arbitrary subjects (including `:` or NUL) never
  collide."
  [provider ^String subject]
  (let [p (subs (str provider) 1)]
    (str (.length p) ":" p ":" subject)))

(defn claim-key-from-parts
  "Bijective string key for a claim given its kind (`:email` or `:code`) and
  value (lower-case email or 64-hex hash)."
  [kind ^String val]
  (case kind
    :email (str "e:" val)
    :code  (str "c:" val)))

(defn claim-key-from-map
  "Bijective string key for a contract `:claim` map."
  [claim]
  (case (:by claim)
    :email (str "e:" (:email claim))
    :code  (str "c:" (:hash claim))))

(defn claim-val-from-map
  "Extracts the email or hash string from a contract `:claim` map."
  [claim]
  (case (:by claim)
    :email (:email claim)
    :code  (:hash claim)))

(defn hex16
  "Formats non-negative sequence `n` as 16 lower-case ASCII hex digits."
  ^String [^long n]
  (format "%016x" n))

(defn hex-ts
  "Formats signed 64-bit timestamp `t` into a strictly order-preserving 16-char
  lower-case ASCII hex string across `[Long/MIN_VALUE, Long/MAX_VALUE]`."
  ^String [^long t]
  (format "%016x" (bit-xor t Long/MIN_VALUE)))

(defn k-root
  "Key of root `P`'s root record (`\"<P>\\0r\"`)."
  ^String [^UUID p]
  (str p "\u0000r"))

(defn k-actor
  "Key of agent `A` under root `P` (`\"<P>\\0a:<A>\"`)."
  ^String [^UUID p ^UUID a]
  (str p "\u0000a:" a))

(defn k-ao
  "Key of agent order record at `seq-n` under root `P`."
  ^String [^UUID p ^long seq-n]
  (str p "\u0000ao:" (hex16 seq-n)))

(defn k-ao-lo
  "Inclusive lower bound of root `P`'s `ao:` range."
  ^String [^UUID p]
  (str p "\u0000ao:"))

(defn k-ao-hi
  "Exclusive upper bound of root `P`'s `ao:` range (`;` is ASCII 0x3b > `:` 0x3a)."
  ^String [^UUID p]
  (str p "\u0000ao;"))

(defn k-signin
  "Key of sign-in `G` under root `P` (used in both `$$root-data` and `$$live`)."
  ^String [^UUID p ^UUID g]
  (str p "\u0000g:" g))

(defn k-signin-lo
  "Inclusive lower bound of root `P`'s `g:` range in `$$live`."
  ^String [^UUID p]
  (str p "\u0000g:"))

(defn k-signin-hi
  "Exclusive upper bound of root `P`'s `g:` range in `$$live`."
  ^String [^UUID p]
  (str p "\u0000g;"))

(defn k-session
  "Key of session `S` under root `P` (used in both `$$root-data` and `$$live`)."
  ^String [^UUID p ^UUID s]
  (str p "\u0000s:" s))

(defn k-session-lo
  "Inclusive lower bound of root `P`'s `s:` range in `$$live`."
  ^String [^UUID p]
  (str p "\u0000s:"))

(defn k-session-hi
  "Exclusive upper bound of root `P`'s `s:` range in `$$live`."
  ^String [^UUID p]
  (str p "\u0000s;"))

(defn k-exp
  "Key of expiry index entry for sign-in (`\"g\"`) or session (`\"s\"`) at `until`."
  ^String [^UUID p ^long until ^String kind-ch ^UUID id]
  (str p "\u0000exp:" (hex-ts until) ":" kind-ch ":" id))

(defn k-exp-lo
  "Inclusive lower bound of root `P`'s `exp:` range."
  ^String [^UUID p]
  (str p "\u0000exp:"))

(defn k-exp-hi
  "Exclusive upper bound for all `exp:` entries with `until <= at`."
  ^String [^UUID p ^long at]
  (str p "\u0000exp:" (hex-ts at) ";"))

(defn k-ok
  "Key of `answer-ask` outcome record for `op-id` under root `P`."
  ^String [^UUID p ^UUID op-id]
  (str p "\u0000ok:" op-id))

(defn k-live-lo
  "Inclusive lower bound of root `P`'s full slice in `$$live`."
  ^String [^UUID p]
  (str p "\u0000"))

(defn k-live-hi
  "Exclusive upper bound of root `P`'s full slice in `$$live`."
  ^String [^UUID p]
  (str p "\u0001"))

;; ------------------------------------------------------- action accessors
;; Named Clojure functions so Rama dataflow never puts keywords in op position.

(defn act-done?
  "Whether step action `act` terminates the loop with a final result."
  [act]
  (boolean (:done? act)))

(defn act-result
  "Returns the terminal result map of a completed step action."
  [act]
  (:result act))

(defn act-write-k
  "Returns the PState key to write in `act`, or `nil`."
  [act]
  (:write-k act))

(defn act-write-v
  "Returns the PState value to write in `act`."
  [act]
  (:write-v act))

(defn act-delete?
  "Whether `act` deletes `:write-k` via `NONE>`."
  [act]
  (boolean (:delete? act)))

(defn act-hop-key
  "Returns the partition key to `|hash` to next, or `nil` for a same-task step."
  [act]
  (:hop-key act))

(defn act-next-step
  "Returns the next step keyword of `act`."
  [act]
  (:next-step act))

(defn act-next-st
  "Returns the next carried loop state map of `act`."
  [act]
  (:next-st act))

;; ------------------------------------------------------- Slice a: invite

(defn invite-step-1
  "Pure Step 1 of `invite` on H(C) (Revision 1).
  Checks `$$claims[C]`: repeat hops to Step 4; `:claim-in-use` hops to H(P)
  to clean up any orphaned `RootReserving(C)` (Witness 6); `nil` hops to Step 2."
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
  "Pure Step 2 of `invite` on H(P) (Revision 1).
  Before refusing `:person-in-use`, hops to H(C) to re-check `$$claims[C]`
  so `:claim-in-use` always precedes `:person-in-use` (Witness 9)."
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
  "Pure Step 2-help on H(C0): helps a competing `RootReserving(C0)` commit
  or observe loss on `$$claims[C0]`."
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
  "Pure Step 2-help-finish on H(P): promotes won `C0` (then rechecks `C` for
  Witness 9) or evicts lost `C0`."
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
  "Pure Step 2-recheck on H(C): verifies `:claim-in-use` vs `:person-in-use`."
  [^InviteEvent ev c]
  (let [P (:person ev)]
    (cond
      (nil? c)          {:done? true :result {:refused :person-in-use}}
      (= (:person c) P) {:hop-key P :next-step :invite/step-4 :next-st {}}
      :else             {:done? true :result {:refused :claim-in-use}})))

(defn invite-step-3
  "Pure Step 3 of `invite` on H(C): commits `ClaimWaiting(P)` or hops to H(P)
  to clear `RootReserving(C)` on loss."
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
  "Pure Step 3-clear on H(P): deletes `RootReserving(C)` if still `C` and
  returns `{:refused :claim-in-use}`."
  [^InviteEvent ev r]
  (let [P (:person ev)
        clear? (and (instance? RootReserving r) (= (:claim-key r) (:claim-key ev)))]
    (cond-> {:done? true :result {:refused :claim-in-use}}
      clear? (assoc :write-k (k-root P) :delete? true))))

(defn invite-step-4
  "Pure Step 4 of `invite` on H(P): promotes `nil`/`RootReserving(C)` to
  `RootInvited(C)` and returns `{:invited P}`."
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
  "Extracts the plain target map carried across hops from an `AccountLinking` record."
  [^AccountLinking acc]
  {:op-id (:op-id acc)
   :account-key (:account-key acc)
   :provider (:provider acc)
   :subject (:subject acc)
   :code-claim-key (:code-claim-key acc)
   :email-claim-key (:email-claim-key acc)
   :own-layer (:own-layer acc)
   :making-uuid (:making-uuid acc)
   :making-session (:making-session acc)
   :says (:says acc)
   :at (:at acc)
   :status (:status acc)
   :chosen (:chosen acc)})

(defn- enter-route-linking
  "Routes an `AccountLinking` state according to its `:status` (`:failing`,
  `:chosen`, or `:probing`)."
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
  "Pure Step E1 of `enter` on H(Acc) (Revisions 1 & 5)."
  [^EnterEvent ev acc]
  (let [ak (:account-key ev)
        self-op (:op-id ev)
        cc (when-let [h (:code-hash ev)] (claim-key-from-parts :code h))
        ec (when-let [e (:email ev)] (claim-key-from-parts :email e))]
    (cond
      (instance? AccountLinked acc)
      (let [prev-at (long (:says-at acc))
            link-info {:linked-person (:person acc)
                       :linked-layer (:own-layer acc)
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
                                        cc ec (:own-layer ev) (:making-uuid ev)
                                        (:making-session ev) (:says ev) (:at ev)
                                        :probing nil)]
          (assoc (enter-route-linking linking)
                 :write-k ak
                 :write-v linking)))

      (instance? AccountLinking acc)
      (enter-route-linking acc))))

(defn enter-step-1a-check-op-returning
  "Pure Step E1a on H(self-op): blocks `:all-after` replay of a previously
  refused enter from overwriting `:says` on an already-linked account."
  [^EnterEvent ev st eo]
  (if (instance? EnterOpFailed eo)
    {:done? true :result {:refused :not-invited}}
    {:hop-key (:account-key ev)
     :next-step :enter/step-1c-update-says
     :next-st st}))

(defn enter-step-1c-update-says
  "Pure Step E1c on H(Acc): updates `:says` and `:says-at` iff `(> at says-at)`
  (Sid's Call 2), then hops to H(P) for `:own-layer-made?`."
  [^EnterEvent ev st acc]
  (let [P (:linked-person st)
        upd? (and (instance? AccountLinked acc)
                  (> (long (:at ev)) (long (:says-at acc))))]
    (cond-> {:hop-key P :next-step :enter/step-1b-read-flag :next-st st}
      upd? (assoc :write-k (:account-key ev)
                  :write-v (assoc acc :says (:says ev) :says-at (:at ev))))))

(defn enter-step-1b-read-flag
  "Pure Step E1b on H(P): reads present `:own-layer-made?` and finishes."
  [st r]
  {:done? true
   :result {:person (:linked-person st)
            :own-layer (:linked-layer st)
            :making (:linked-making st)
            :own-layer-made? (boolean (when (instance? RootEntered r) (:own-layer-made? r)))}})

(defn enter-step-2-probe
  "Pure Step E2 on H(curr-claim): strictly read-only probe of `$$claims[C]`
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
  "Pure Step E2' on H(Acc): atomically transitions `AccountLinking` from
  `:probing` to `:chosen(C)` or `:failing`."
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
  "Pure Step E2c-confirm on H(op): refuses to enter E3 if `$$enter-ops[op]`
  holds `EnterOpFailed`, otherwise hops to H(curr-claim) for Step E3."
  [^EnterEvent ev st eo]
  (if (instance? EnterOpFailed eo)
    {:hop-key (:account-key ev)
     :next-step :enter/step-2d-finish-failed
     :next-st {:target (:target st)}}
    {:hop-key (:curr-claim st)
     :next-step :enter/step-3-use
     :next-st st}))

(defn enter-step-2c-mark-failed
  "Pure Step E2c-fail on H(op): writes `EnterOpFailed` before H(Acc) clears
  `AccountLinking`."
  [^EnterEvent ev st eo]
  (let [op (:op-id (:target st))]
    (cond-> {:hop-key (:account-key ev)
             :next-step :enter/step-2d-finish-failed
             :next-st {:target (:target st)}}
      (not (instance? EnterOpFailed eo))
      (assoc :write-k op :write-v (->EnterOpFailed)))))

(defn enter-step-2c-mark-failed-direct
  "Pure Step E2c-direct on H(self-op): marks no-claim refusal as `EnterOpFailed`
  and returns `{:refused :not-invited}`."
  [^EnterEvent ev eo]
  (cond-> {:done? true :result {:refused :not-invited}}
    (not (instance? EnterOpFailed eo))
    (assoc :write-k (:op-id ev) :write-v (->EnterOpFailed))))

(defn enter-step-2d-finish-failed
  "Pure Step E2d on H(Acc): deletes `AccountLinking(op)` via `NONE>`, then
  either returns `{:refused :not-invited}` (if own op) or re-runs Step E1
  locally (if helper)."
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
  "Pure Step E3 on H(curr-claim): claims `ClaimWaiting(P)` directly as
  `ClaimUsed(P, op)` or hops back to H(Acc) to CAS-clear `:chosen` on loss."
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
  "Pure Step E3b on H(Acc): strictly CAS-resets `:chosen` from `failed-claim`
  back to `:probing` and loops locally to Step E1."
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
  "Pure Step E4 on H(P): ensures `RootEntered` fixing `own-layer` and `making`,
  reads fixed values and present `:own-layer-made?`, hops to Step E5 on H(Acc)."
  [^EnterEvent ev st r]
  (let [target (:target st)
        P (:won-person target)
        entered-already? (instance? RootEntered r)
        r' (if entered-already?
             r
             (->RootEntered (:claim-key r) (:claim-kind r) (:claim-val r)
                            (:invited-by-kind r) (:invited-by-id r) (:invited-at r)
                            (:provider target) (:subject target)
                            (:own-layer target) (:making-uuid target) (:making-session target)
                            false 0 0))]
    (cond-> {:hop-key (:account-key ev)
             :next-step :enter/step-5-link
             :next-st {:target (assoc target
                                      :fixed-layer (:own-layer r')
                                      :fixed-making-uuid (:making-uuid r')
                                      :fixed-making-session (:making-session r')
                                      :present-flag (boolean (:own-layer-made? r')))}}
      (not entered-already?)
      (assoc :write-k (k-root P) :write-v r'))))

(defn enter-step-5-link
  "Pure Step E5 on H(Acc): transitions `AccountLinking(op)` to `AccountLinked`
  using `target`'s `:says`/`:at`; if a helper has a strictly later `:at`, hops
  to H(self-op) (`E1a`) to verify not refused before updating `:says`."
  [^EnterEvent ev st acc]
  (let [target (:target st)
        op (:op-id target)
        self-op (:op-id ev)
        ak (:account-key ev)
        P (:won-person target)
        fl (:fixed-layer target)
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
                         :linked-layer fl
                         :linked-making {:uuid fmu :session fms}}}
        linking-this? (assoc :write-k ak :write-v linked-base))

      (and (not helping?) need-self-says?)
      {:write-k ak
       :write-v (assoc linked-base :says (:says ev) :says-at (:at ev))
       :done? true
       :result {:person P :own-layer fl :making {:uuid fmu :session fms} :own-layer-made? flag}}

      :else
      (cond-> {:done? true
               :result {:person P :own-layer fl :making {:uuid fmu :session fms} :own-layer-made? flag}}
        linking-this? (assoc :write-k ak :write-v linked-base)))))

;; --------------------------------------------------- Slice a: own-layer-made

(defn own-layer-made-step
  "Pure single-task decision for `own-layer-made` on H(P)."
  [^OwnLayerMadeEvent ev r]
  (let [P (:root-id ev)]
    (if (instance? RootEntered r)
      (cond-> {:done? true :result {:made P}}
        (not (:own-layer-made? r))
        (assoc :write-k (k-root P) :write-v (assoc r :own-layer-made? true)))
      {:done? true :result {:refused :no-such-person}})))

;; ------------------------------------------- Slice b: root plans & writes

(def ^:private max-actor-depth
  "Maximum makers below root (Revision 7 closed for this build: 32, matching
  `SPEC.md` and `reference.clj`)."
  32)

(defn plan-rd-upserts
  "Returns the vector of `[key val]` pairs to upsert into `$$root-data`."
  [plan]
  (or (:rd-upserts plan) []))

(defn plan-rd-deletes
  "Returns the vector of keys to delete (`NONE>`) from `$$root-data`."
  [plan]
  (or (:rd-deletes plan) []))

(defn plan-live-upserts
  "Returns the vector of `[key val]` pairs to upsert into `$$live`."
  [plan]
  (or (:live-upserts plan) []))

(defn plan-live-deletes
  "Returns the vector of keys to delete (`NONE>`) from `$$live`."
  [plan]
  (or (:live-deletes plan) []))

(defn plan-result
  "Returns the contract result map produced by a single-root write plan."
  [plan]
  (:result plan))

(defn signin-rec->live
  "Builds a `LiveSignIn` entry from a live `SignInRec`."
  [^SignInRec g]
  (->LiveSignIn (:sign-in g) (:actor g) (:kind g) (:initial-hash g) (:hash g)
                (:until g) (:prev-hash g) (:prev-until g) (:begun g) (:label g)
                (:seq g) (:own-layer g) (:own-layer-made-at-begin? g)))

(defn live-signin->ended-rec
  "Builds an ended `SignInRec` directly from a `LiveSignIn` (0 point seeks on `g:`)."
  [^LiveSignIn g]
  (->SignInRec (:sign-in g) (:actor g) (:kind g) (:initial-hash g) (:hash g)
               (:until g) (:prev-hash g) (:prev-until g) true (:begun g)
               (:label g) (:seq g) (:own-layer g) (:own-layer-made-at-begin? g)))

(defn live-session->closed-rec
  "Builds a closed `SessionRec` directly from a `LiveSession`, preserving `:driven-by`
  with 0 point seeks on `s:`."
  [^LiveSession s]
  (->SessionRec (:session s) (:sign-in s) (:until s) true (:driven-by s)))

(defn plan-take-out
  "Pure planner for take-out under `root`: given non-empty `exp-map` (`exp:`
  entries with `until <= at`) and `live-map` (`$$live` range for `root`),
  computes all `rd-upserts`, `rd-deletes`, and `live-deletes` in memory,
  cascading expired sign-ins to their open sessions even when session `:until > at`."
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
  "Pure planner for `begin-sign-in` on H(P) after take-out."
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
            own-layer (:own-layer r)
            made-at-begin? (boolean (:own-layer-made? r))
            rec (->SignInRec G A kind h h until nil nil born-ended? at lbl seq-n own-layer made-at-begin?)
            r' (assoc r :next-sign-in-seq (inc seq-n))]
        {:rd-upserts (cond-> [[(k-root P) r']
                              [(k-signin P G) rec]]
                       (not born-ended?)
                       (conj [(k-exp P until "g" G) (->ExpiryEntry :sign-in G A until)]))
         :live-upserts (if born-ended?
                         []
                         [[(k-signin P G) (signin-rec->live rec)]])
         :result {:begun G}}))))

(defn replace-secret-born-ended?
  "Whether an accepted `replace-secret` event will immediately end `g` because
  `(:until ev) <= (:at ev)` (requiring a `$$live` `s:` scan to close open sessions)."
  [^ReplaceSecretEvent ev g]
  (boolean (and (instance? SignInRec g)
                (not (and (= (:hash g) (:new-hash ev))
                          (= (:prev-hash g) (:old-hash ev))))
                (not (:ended? g))
                (= (:hash g) (:old-hash ev))
                (<= (long (:until ev)) (long (:at ev))))))

(defn plan-replace-secret
  "Pure planner for `replace-secret` on H(P) after take-out. `live-s-map` is
  needed only when `replace-secret-born-ended?` is true."
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
                live-deletes (transient [(k-signin P G)])]
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
           :live-upserts [[(k-signin P G) (signin-rec->live g')]]
           :result {:replaced G}})))))

(defn signin-needs-end?
  "Whether `g` is a live `SignInRec` that `end-sign-in` must end and cascade."
  [g]
  (boolean (and (instance? SignInRec g) (not (:ended? g)))))

(defn plan-end-sign-in
  "Pure planner for `end-sign-in` on H(P) after take-out."
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
            live-deletes (transient [(k-signin P G)])]
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
  "Pure planner for `make-actor` on H(P) after take-out."
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
  "Whether `end-actor` must run the 2-scan cascade (`r` is entered, `A != P`,
  and `a` is a live `ActorRec`)."
  [root actor r a]
  (boolean (and (instance? RootEntered r)
                (not= actor root)
                (instance? ActorRec a)
                (not (:ended? a)))))

(defn plan-end-actor-noop
  "Pure planner for `end-actor` repeat or refusal (when `actor-needs-end?` is false)."
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
  "Pure planner for Revision 2's 3-seek 2-scan `end-actor` cascade (`a:<A>`,
  forward `ao:` scan from `target.seq`, and `$$live` scan for `root`)."
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
          (conj! rd-upserts [(k-ao root (:seq ao)) (assoc ao :ended? true)])
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
  "Pure planner for `open-session` on H(P) after take-out."
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
           :live-upserts [[(k-session P S) (->LiveSession S G until (:driven-by s))]]
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
                         [[(k-session P S) (->LiveSession S G until db)]])
         :result {:open S}}))))

(defn plan-close-session
  "Pure planner for `close-session` on H(P) after take-out."
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
       :live-deletes [(k-session P S)]
       :result {:closed S}})))

;; ------------------------------------------------- Slice c: ask & answer-ask

(defn ask-step
  "Pure single-task decision for `ask` on H(U)."
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
  "Extracts the plain target map carried across hops from an `AskAnswering` record."
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
  "Pure Step 1 of `answer-ask` on H(U) (Revision 4)."
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
  "Pure Step 2 evaluation of `answer-ask` on H(P_target) after confirming
  `ok:<op>` is absent and running `take-out(P_target, target.at)`. Returns a
  root write plan plus `:outcome` (`AskOutcomeYes` / `AskOutcomeNo` /
  `AskOutcomeRefused`)."
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
                own-layer (:own-layer r)
                made-at-begin? (boolean (:own-layer-made? r))
                srec (->SignInRec sign-in actor :program hash hash until nil nil
                                  born-ended? at asks g-seq own-layer made-at-begin?)
                out (->AskOutcomeYes root actor sign-in)]
            {:rd-upserts (cond-> [[(k-root root) (assoc r :next-sign-in-seq (inc g-seq))]
                                  [(k-signin root sign-in) srec]
                                  [ok-key out]]
                           (not born-ended?)
                           (conj [(k-exp root until "g" sign-in) (->ExpiryEntry :sign-in sign-in actor until)]))
             :live-upserts (if born-ended? [] [[(k-signin root sign-in) (signin-rec->live srec)]])
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
                own-layer (:own-layer r)
                made-at-begin? (boolean (:own-layer-made? r))
                arec (->ActorRec actor root 1 new-agent-label false a-seq)
                aorec (->ActorOrderRec actor root 1 new-agent-label false a-seq)
                srec (->SignInRec sign-in actor :program hash hash until nil nil
                                  born-ended? at asks g-seq own-layer made-at-begin?)
                out (->AskOutcomeYes root actor sign-in)]
            {:rd-upserts (cond-> [[(k-root root) (assoc r :next-actor-seq (inc a-seq) :next-sign-in-seq (inc g-seq))]
                                  [(k-actor root actor) arec]
                                  [(k-ao root a-seq) aorec]
                                  [(k-signin root sign-in) srec]
                                  [ok-key out]]
                           (not born-ended?)
                           (conj [(k-exp root until "g" sign-in) (->ExpiryEntry :sign-in sign-in actor until)]))
             :live-upserts (if born-ended? [] [[(k-signin root sign-in) (signin-rec->live srec)]])
             :outcome out}))))))

(defn plan-outcome
  "Extracts `:outcome` from an `answer-ask-eval-root` plan."
  [plan]
  (:outcome plan))

(defn no-such-ask-outcome
  "Constructs the recorded `AskOutcomeRefused(:no-such-ask)` for Step 1b replay safety."
  []
  (->AskOutcomeRefused :no-such-ask))

(defn answer-ask-step-3
  "Pure Step 3 of `answer-ask` on H(U) (Revision 4): finalizes `AskYes`,
  `AskNo`, or restores `AskWaiting`; helpers (`op != self-op`) on Yes/No
  ALWAYS hop to H(self-root) for take-out at `(:at ev)` (Witness 10)."
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
  "Whether `sig` (`SignInRec` or nil) allows `who-is` to skip reading `r`."
  [sig]
  (boolean (and (instance? SignInRec sig) (:own-layer-made-at-begin? sig))))

(defn root-entered?
  "Whether `r` is a `RootEntered` record."
  [r]
  (instance? RootEntered r))

(defn need-actor-seek?
  "Whether `who-is` must read `a:<A>` (only when `sig` is an ended agent sign-in)."
  [root sig]
  (boolean (and (instance? SignInRec sig)
                (:ended? sig)
                (not= (:actor sig) root))))

(defn signin-actor
  "Returns `:actor` of a `SignInRec`."
  [^SignInRec sig]
  (:actor sig))

(defn build-who-is-result
  "Builds the exact contract result map of `who-is` from `(sig, r, sess, actor-rec)`."
  [root sig r sess actor-rec]
  (let [fast? (signin-fast-path? sig)
        entered? (or fast? (instance? RootEntered r))]
    (if-not entered?
      {:sign-in nil :session nil :own-layer nil :own-layer-made? false}
      (let [own-layer (if fast? (:own-layer sig) (:own-layer r))
            made? (if fast? true (boolean (:own-layer-made? r)))
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
         :own-layer own-layer
         :own-layer-made? made?}))))

(defn root-has-actors?
  "Whether `RootEntered` `r` has created at least one agent (`next-actor-seq > 0`)."
  [r]
  (boolean (and (instance? RootEntered r) (pos? (long (:next-actor-seq r))))))

(defn root-has-signins?
  "Whether `RootEntered` `r` has begun at least one sign-in (`next-sign-in-seq > 0`)."
  [r]
  (boolean (and (instance? RootEntered r) (pos? (long (:next-sign-in-seq r))))))

(defn build-under-result
  "Builds the exact contract result map of `under` from `ao-map` (`ao:` sorted
  range) and `live-g-map` (`$$live` `g:` range, live sign-ins only, sorted by `:seq`)."
  [ao-map live-g-map]
  {:actors (if (seq ao-map)
             (->> (vals ao-map)
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
  "Projects a stored `IAskState` record into the contract `ask-of` map (or `nil`)."
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
  "Projects one `ILiveEntry` (`LiveSignIn` or `LiveSession`) into `[section id val]`
  for `watch-root`'s `{:sign-ins {...} :sessions {...}}` map (omitting `:driven-by`,
  `:begun`, `:label`, `:seq`, `:initial-hash`, `:own-layer`, `:own-layer-made-at-begin?`)."
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
  "Parses a `$$live` key (`\"<P>\\0g:<G>\"` or `\"<P>\\0s:<S>\"`) into
  `[:sign-ins G-uuid]` or `[:sessions S-uuid]` for incremental `KeyRemoveDiff`."
  [^String k]
  (let [nul (.indexOf k 0)]
    (when (and (>= nul 0) (> (.length k) (+ nul 3)))
      (let [tag (.charAt k (inc nul))
            id (UUID/fromString (.substring k (+ nul 3)))]
        (case tag
          \g [:sign-ins id]
          \s [:sessions id]
          nil)))))

(defn project-live-map
  "Projects a full `$$live` slice map (`{String -> ILiveEntry}`) into
  `{:sign-ins {G ...} :sessions {S ...}}`."
  [m]
  (reduce-kv
   (fn [acc _ v]
     (if-let [[section id proj] (project-live-entry v)]
       (assoc-in acc [section id] proj)
       acc))
   {:sign-ins {} :sessions {}}
   (or m {})))
