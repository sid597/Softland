(ns auth-concurrent.plan-steps
  "Revised single-task cross-key steps for `Auth` (`PLAN.md` Revisions 1, 2, 3,
  4, 5), encoded in the exact shape of `overnight-audit/auth-concurrent`'s
  `plan_steps.clj` so carbon's `model_check.clj`, `spec_oracle.clj`, and
  `step1_run.clj` scenarios run over it unchanged.

  Key protocol changes from the failed Phase-1 plan:
  - `invite` (Revision 1):
    * Before returning `{:refused :person-in-use}` from H(P) (either on
      RootInvited/RootEntered or after helping a competing RootReserving C0 to
      RootInvited), hop `|hash C` to re-check `$$claims[C]`. If `C` now names
      another person, return `{:refused :claim-in-use}` (enforcing SPEC.md's
      refusal order across concurrent causal chains — Witness 9).
    * On Step 1 `:claim-in-use` (`$$claims[C].person != P`), hop `|hash P` to
      clear any `RootReserving(C)` before returning `{:refused :claim-in-use}`
      (closing orphaned `RootReserving` / email PII leak — Witness 6).
  - `enter` (Revisions 1 & 5):
    * Uniform choose-at-account protocol for BOTH 1-claim and 2-claim enters
      (no uncoordinated 1-claim fast path; never writes `ClaimClaiming` onto
      `$$claims`).
    * Step 2 probes claims READ-ONLY (code first, then email; if code was nil
      and email was ClaimUsed, re-checks code once to ensure a single-instant
      linearization point for `:not-invited`). Any pre-seeded `ClaimClaiming`
      (e.g. scenarios 3C-06 and 2C-15) is resolved via strict CAS against
      `$$enter-ops[other-op]` into `ClaimUsed` or `ClaimWaiting` before
      evaluating usability.
    * Step 2b commits `:chosen C` on `$$accounts[Acc]` (`AccountLinking`) before
      Step 3 writes `ClaimUsed(P, op)` on `$$claims[C]`. A `:chosen` claim is
      cleared via strict CAS ONLY after observing permanent `ClaimUsed(*, other-op)`.
    * Returning and finalizing enters update `:says` and `:says-at` ONLY when
      `(> at says-at)` (Sid's Call 2 — Witnesses 7, 8, 2C-16).
  - `answer-ask` (Revision 4):
    * Step 2 checks `ok:<op>` BEFORE running `take-out(P, ans.at)` so replays
      never re-execute stale take-outs.
    * Step 3 helper (`op0 != self-op`) ALWAYS hops `|hash (:root self-input)` to
      run take-out at `(:at self-input)` on `AskOutcomeYes` / `AskOutcomeNo`
      before returning (closing `P == P0` missed take-out — Witness 10).
  - `who-is` (Revision 3) and `end-actor` / `take-out` (Revision 2):
    * `who-is` reads `g:<G>` first and skips `\"<P>\\0r\"` whenever
      `:own-layer-made-at-begin?` is true.
    * `ch:`, `ag:`, and `sg:` indexes are deleted; `end-actor` uses the 2-scan
      (`ao:` forward scan + `$$live` scan) cascade.")

;; ---------------------------------------------------------- PState empty state

(def empty-pstates
  "Partitioned PStates of revised `PLAN.md`.
   `:enter-ops` records ONLY PII-free `{:state :EnterOpFailed}` on refused
   `enter` attempts so Rama `:all-after` stream replays of a refused depot
   record can never re-execute against later invites or overwrite later `:says`.
   (Pre-seeded legacy `:EnterOpWon` / `:EnterOpTryEmail` entries in carbon's
   `3C-06` / `2C-15` scenarios are also resolved via strict CAS.)"
  {:root-data {}
   :live {}
   :accounts {}
   :claims {}
   :asks {}
   :enter-ops {}})

(defn task-for
  "Maps a partition key to its task identifier."
  [kind k]
  [kind k])

(defn depot-info
  "Returns `{:depot keyword :partition-key any :task task-id}` for a write call."
  [[op input]]
  (case op
    :invite {:depot :*invites
             :partition-key (:claim input)
             :task (task-for :claim (:claim input))}
    :enter {:depot :*enters
            :partition-key (:account input)
            :task (task-for :account (:account input))}
    :answer-ask {:depot :*asks
                 :partition-key (:code input)
                 :task (task-for :code (:code input))}
    :ask {:depot :*asks
          :partition-key (:code input)
          :task (task-for :code (:code input))}
    :own-layer-made {:depot :*root-events
                     :partition-key (:person input)
                     :task (task-for :root (:person input))}
    (:begin-sign-in :replace-secret :end-sign-in
     :make-actor :end-actor :open-session :close-session)
    {:depot :*root-events
     :partition-key (:root input)
     :task (task-for :root (:root input))}))

;; ------------------------------------------------- Root helpers & take-out (§2)

(defn- close-session-on-root
  "Closes session S on root P in $$root-data and removes S from $$live and exp:."
  [pstates P S]
  (let [s (get-in pstates [:root-data P :sessions S])]
    (if (or (nil? s) (:closed? s))
      pstates
      (let [u (:until s)]
        (-> pstates
            (assoc-in [:root-data P :sessions S :closed?] true)
            (update-in [:live P :sessions] dissoc S)
            (update-in [:root-data P :expiries] dissoc [u :session S]))))))

(defn- end-sign-in-on-root
  "Ends sign-in G on root P in $$root-data, removes G from $$live and exp:,
  and cascades to open sessions of G found via $$live (zero sg: index)."
  [pstates P G]
  (let [g (get-in pstates [:root-data P :sign-ins G])]
    (if (or (nil? g) (:ended? g))
      pstates
      (let [u (:until g)
            open-ss (keep (fn [[S s]] (when (= (:sign-in s) G) S))
                          (get-in pstates [:live P :sessions] {}))
            ps1 (-> pstates
                    (assoc-in [:root-data P :sign-ins G :ended?] true)
                    (update-in [:live P :sign-ins] dissoc G)
                    (update-in [:root-data P :expiries] dissoc [u :sign-in G]))]
        (reduce (fn [ps S] (close-session-on-root ps P S)) ps1 open-ss)))))

(defn run-take-out-on-root
  "Scans exp: entries with `until <= at` and ends/closes them atomically on H(P)."
  [pstates P at]
  (let [exps (get-in pstates [:root-data P :expiries] {})
        expired (->> (keys exps)
                     (filter (fn [[until _ _]] (<= until at)))
                     (sort))]
    (reduce (fn [ps [_ kind id :as k]]
              (if-not (contains? (get-in ps [:root-data P :expiries]) k)
                ps
                (case kind
                  :session (close-session-on-root ps P id)
                  :sign-in (end-sign-in-on-root ps P id))))
            pstates
            expired)))

;; --------------------------------------------------------- Four reads (PLAN.md)

(defn plan-who-is
  "Executes revised `who-is` on H(P) (Revision 3: 2-seek fast path reading
  g:<G> first and skipping `<P>\\0r` when `:own-layer-made-at-begin?` is true)."
  [pstates {:keys [root sign-in session]}]
  (let [g (get-in pstates [:root-data root :sign-ins sign-in])
        fast? (and (some? g) (boolean (:own-layer-made-at-begin? g)))
        r (when-not fast? (get-in pstates [:root-data root :r]))
        entered? (or fast? (= :RootEntered (:state r)))]
    (if-not entered?
      {:sign-in nil :session nil :own-layer nil :own-layer-made? false}
      (let [s (when session (get-in pstates [:root-data root :sessions session]))
            actor-ended? (if (and g (:ended? g) (not= (:actor g) root))
                           (boolean (get-in pstates [:root-data root :actors (:actor g) :ended?]))
                           false)
            own-layer (if g (:own-layer g) (:own-layer r))
            made? (if fast? true (boolean (:own-layer-made? r)))]
        {:sign-in (when g
                    {:actor (:actor g)
                     :kind (:kind g)
                     :hash (:hash g)
                     :until (:until g)
                     :previous (when (:prev-hash g)
                                 {:hash (:prev-hash g) :until (:prev-until g)})
                     :ended? (boolean (:ended? g))
                     :actor-ended? actor-ended?})
         :session (when s
                    {:sign-in (:sign-in s)
                     :until (:until s)
                     :closed? (boolean (:closed? s))
                     :driven-by (:driven-by s)})
         :own-layer own-layer
         :own-layer-made? made?}))))

(defn plan-ask-of
  "Executes `ask-of` on H(U). AskWaiting and AskAnswering both project to `:waiting`."
  [pstates code]
  (when-let [k (get-in pstates [:asks code])]
    (case (:state k)
      (:AskWaiting :AskAnswering)
      {:hash (:hash k) :asks (:asks k) :ends (:ends k) :state :waiting :answer nil}
      :AskYes
      {:hash (:hash k) :asks (:asks k) :ends (:ends k) :state :yes
       :answer {:root (:root k) :actor (:actor k) :sign-in (:sign-in k)}}
      :AskNo
      {:hash (:hash k) :asks (:asks k) :ends (:ends k) :state :no :answer nil})))

(defn plan-under
  "Executes `under` on H(P): agents from `ao:` and live sign-ins from `$$live`
  sorted by `:seq` (never walks ended sign-ins)."
  [pstates P]
  (let [r (get-in pstates [:root-data P :r])]
    (if (or (nil? r) (not= :RootEntered (:state r)))
      {:actors [] :sign-ins []}
      (let [ao (if (pos? (:next-actor-seq r 0))
                 (mapv #(select-keys % [:actor :made-by :label :ended?])
                       (sort-by :seq (vals (get-in pstates [:root-data P :actor-order] {}))))
                 [])
            gs (if (pos? (:next-sign-in-seq r 0))
                 (->> (vals (get-in pstates [:live P :sign-ins] {}))
                      (sort-by :seq)
                      (mapv #(select-keys % [:sign-in :actor :kind :label :begun :until])))
                 [])]
        {:actors ao :sign-ins gs}))))

(defn plan-live-under
  "Executes `watch-root` projected `$$live` slice on H(P) (omits `:driven-by`)."
  [pstates P]
  {:sign-ins (into {}
                   (map (fn [[G g]]
                          [G {:actor (:actor g)
                              :kind (:kind g)
                              :hash (:hash g)
                              :until (:until g)
                              :previous (when (:prev-hash g)
                                          {:hash (:prev-hash g) :until (:prev-until g)})}]))
                   (get-in pstates [:live P :sign-ins] {}))
   :sessions (into {}
                   (map (fn [[S s]]
                          [S {:sign-in (:sign-in s) :until (:until s)}]))
                   (get-in pstates [:live P :sessions] {}))})

;; -------------------------------------- Seeding / single-task operations on P/U

(defn seed-ask
  "Synchronously applies `ask` on H(U)."
  [pstates {:keys [code hash asks ends at]}]
  (let [k (get-in pstates [:asks code])]
    (cond
      (and k (= (:hash k) hash)) [pstates {:asked code}]
      (some? k) [pstates {:refused :in-use}]
      :else [(assoc-in pstates [:asks code]
                       {:state :AskWaiting :hash hash :asks asks :ends ends
                        :born-over? (<= ends at)})
             {:asked code}])))

(defn seed-invite-settled
  "Synchronously seeds a settled invitation `(P, C)` in `$$claims` and `$$root-data`."
  [pstates {:keys [person claim invited-by at]}]
  [(-> pstates
       (assoc-in [:claims claim] {:state :ClaimWaiting :person person :invited-by invited-by :invited-at at})
       (assoc-in [:root-data person :r] {:state :RootInvited :claim claim :invited-by invited-by :invited-at at}))
   {:invited person}])

(defn seed-entered-person
  "Synchronously seeds an entered person `P` linked to `Acc` with claim `C`."
  [pstates {:keys [person claim account own-layer making says at own-layer-made?]}]
  (let [op-id [:seed-op person]]
    [(-> pstates
         (assoc-in [:claims claim] {:state :ClaimUsed :person person :invited-by :system :invited-at 0 :used-by-op op-id})
         (assoc-in [:root-data person :r]
                   {:state :RootEntered :claim claim :invited-by :system :invited-at 0
                    :linked-account account :own-layer own-layer :making making
                    :own-layer-made? (boolean own-layer-made?)
                    :next-actor-seq 0 :next-sign-in-seq 0})
         (assoc-in [:accounts account]
                   {:state :AccountLinked :person person :own-layer own-layer :making making
                    :says (or says {}) :says-at (or at 0)}))
     {:person person :own-layer own-layer :making making :own-layer-made? (boolean own-layer-made?)}]))

(defn seed-make-actor
  "Synchronously applies `make-actor` on H(P) (no ch: index)."
  [pstates {:keys [root actor made-by label at]}]
  (let [ps (run-take-out-on-root pstates root at)
        r (get-in ps [:root-data root :r])
        existing (get-in ps [:root-data root :actors actor])
        maker (if (= made-by root)
                (when (= :RootEntered (:state r)) {:depth 0 :ended? false})
                (get-in ps [:root-data root :actors made-by]))]
    (cond
      (and existing (= (:made-by existing) made-by)) [ps {:made actor}]
      (or (not= :RootEntered (:state r)) (nil? maker)) [ps {:refused :no-such-maker}]
      (:ended? maker) [ps {:refused :ended}]
      (or (= actor root) (some? existing)) [ps {:refused :in-use}]
      (= 32 (:depth maker)) [ps {:refused :too-deep}]
      :else
      (let [seq-n (:next-actor-seq r 0)
            depth (inc (:depth maker))]
        [(-> ps
             (assoc-in [:root-data root :r :next-actor-seq] (inc seq-n))
             (assoc-in [:root-data root :actors actor]
                       {:actor actor :made-by made-by :depth depth :label label :ended? false :seq seq-n})
             (assoc-in [:root-data root :actor-order seq-n]
                       {:seq seq-n :actor actor :made-by made-by :depth depth :label label :ended? false}))
         {:made actor}]))))

(defn seed-begin-sign-in
  "Synchronously applies `begin-sign-in` on H(P) (with `:own-layer` and
  `:own-layer-made-at-begin?` for 2-seek `who-is`, and no ag: index)."
  [pstates {:keys [root actor sign-in kind hash until label at]}]
  (let [ps (run-take-out-on-root pstates root at)
        r (get-in ps [:root-data root :r])
        existing (get-in ps [:root-data root :sign-ins sign-in])
        act-rec (if (= actor root)
                  (when (= :RootEntered (:state r)) {:ended? false})
                  (get-in ps [:root-data root :actors actor]))]
    (cond
      (and existing (= (:actor existing) actor) (= (:kind existing) kind) (= (:initial-hash existing) hash))
      [ps {:begun sign-in}]
      (or (not= :RootEntered (:state r)) (nil? act-rec)) [ps {:refused :no-such-actor}]
      (:ended? act-rec) [ps {:refused :ended}]
      (some? existing) [ps {:refused :in-use}]
      :else
      (let [seq-n (:next-sign-in-seq r 0)
            born-ended? (<= until at)
            own-layer (:own-layer r)
            made-at-begin? (boolean (:own-layer-made? r))
            ps1 (-> ps
                    (assoc-in [:root-data root :r :next-sign-in-seq] (inc seq-n))
                    (assoc-in [:root-data root :sign-ins sign-in]
                              {:sign-in sign-in :actor actor :kind kind
                               :initial-hash hash :hash hash :until until
                               :prev-hash nil :prev-until nil
                               :ended? born-ended? :begun at :label label :seq seq-n
                               :own-layer own-layer :own-layer-made-at-begin? made-at-begin?}))]
        [(if born-ended?
           ps1
           (-> ps1
               (assoc-in [:live root :sign-ins sign-in]
                         {:sign-in sign-in :actor actor :kind kind :initial-hash hash :hash hash
                          :until until :prev-hash nil :prev-until nil
                          :begun at :label label :seq seq-n
                          :own-layer own-layer :own-layer-made-at-begin? made-at-begin?})
               (assoc-in [:root-data root :expiries [until :sign-in sign-in]]
                         {:kind :sign-in :id sign-in :parent-id actor :until until})))
         {:begun sign-in}]))))

(defn seed-open-session
  "Synchronously applies `open-session` on H(P) (no sg: index)."
  [pstates {:keys [root sign-in session until driven-by at]}]
  (let [ps (run-take-out-on-root pstates root at)
        s (get-in ps [:root-data root :sessions session])
        g (get-in ps [:root-data root :sign-ins sign-in])]
    (cond
      (and s (= (:sign-in s) sign-in))
      (if (and (not (:closed? s)) (> until (:until s)))
        (let [old-u (:until s)]
          [(-> ps
               (assoc-in [:root-data root :sessions session :until] until)
               (assoc-in [:live root :sessions session :until] until)
               (update-in [:root-data root :expiries] dissoc [old-u :session session])
               (assoc-in [:root-data root :expiries [until :session session]]
                         {:kind :session :id session :parent-id sign-in :until until}))
           {:open session}])
        [ps {:open session}])
      (or (nil? g) (:ended? g)) [ps {:refused :not-live}]
      (some? s) [ps {:refused :in-use}]
      :else
      (let [born-closed? (<= until at)
            ps1 (assoc-in ps [:root-data root :sessions session]
                          {:session session :sign-in sign-in :until until
                           :closed? born-closed? :driven-by driven-by})]
        [(if born-closed?
           ps1
           (-> ps1
               (assoc-in [:live root :sessions session]
                         {:session session :sign-in sign-in :until until :driven-by driven-by})
               (assoc-in [:root-data root :expiries [until :session session]]
                         {:kind :session :id session :parent-id sign-in :until until})))
         {:open session}]))))

;; --------------------------------------------- Step execution engine

(defn make-initial-msg
  "Builds the initial Step-1 message on the source depot's task for an event."
  [{:keys [call-id attempt-id op input op-id]}]
  (let [{:keys [task]} (depot-info [op input])]
    (case op
      :invite
      {:call-id call-id :attempt-id attempt-id :op :invite
       :step :invite/step-1-check-claim
       :task task
       :input input
       :rounds 0}

      :enter
      {:call-id call-id :attempt-id attempt-id :op :enter
       :step :enter/step-1-check-account
       :task task
       :self-op op-id
       :self-input input
       :rounds 0}

      :answer-ask
      {:call-id call-id :attempt-id attempt-id :op :answer-ask
       :step :answer-ask/step-1-check-ask
       :task task
       :self-op op-id
       :self-input input
       :rounds 0}

      :own-layer-made
      {:call-id call-id :attempt-id attempt-id :op :own-layer-made
       :step :own-layer-made/step-1
       :task task
       :input input}

      :end-actor
      {:call-id call-id :attempt-id attempt-id :op :end-actor
       :step :end-actor/step-1
       :task task
       :input input}

      :make-actor
      {:call-id call-id :attempt-id attempt-id :op :make-actor
       :step :make-actor/step-1
       :task task
       :input input}

      :ask
      {:call-id call-id :attempt-id attempt-id :op :ask
       :step :ask/step-1
       :task task
       :input input})))

;; ---- 1A. Revised invite steps (PLAN.md §1A + Revision 1)

(defn- invite-step-2-at-root
  "Evaluates Step 2 of `invite` on H(P). Before returning `:person-in-use`,
  hops `|hash C` to re-check `$$claims[C]` so `:claim-in-use` always precedes
  `:person-in-use` across concurrent causal chains."
  [pstates {:keys [input] :as msg}]
  (let [{:keys [person claim invited-by at]} input
        r (get-in pstates [:root-data person :r])]
    (cond
      (and r (#{:RootInvited :RootEntered} (:state r)))
      (if (= (:claim r) claim)
        {:pstates pstates
         :plan-lines "PLAN.md:invite-step-2-repeat"
         :summary (str "H(P=" person ") sees " (:state r) " with same C=" claim " -> ack-return {:invited " person "}")
         :ack-return {:invited person}}
        {:pstates pstates
         :plan-lines "PLAN.md:invite-step-2-recheck-claim"
         :summary (str "H(P=" person ") sees " (:state r) " with other claim " (:claim r) " -> hops |hash C=" claim " to verify :claim-in-use vs :person-in-use")
         :next-msg (-> msg
                       (dissoc :target)
                       (assoc :step :invite/step-2-recheck-claim :task (task-for :claim claim)))})

      (nil? r)
      {:pstates (assoc-in pstates [:root-data person :r]
                          {:state :RootReserving :claim claim :invited-by invited-by :invited-at at})
       :plan-lines "PLAN.md:invite-step-2-reserve"
       :summary (str "H(P=" person ") sees r=nil -> writes RootReserving(C=" claim "), hops |hash C to Step 3")
       :next-msg (-> msg
                     (dissoc :target)
                     (assoc :step :invite/step-3-commit-claim :task (task-for :claim claim)))}

      (= :RootReserving (:state r))
      (let [{c0 :claim b0 :invited-by t0 :invited-at} r]
        (if (= c0 claim)
          {:pstates pstates
           :plan-lines "PLAN.md:invite-step-2-own-reserve"
           :summary (str "H(P=" person ") sees RootReserving(same C=" claim ") -> hops |hash C to Step 3")
           :next-msg (-> msg
                         (dissoc :target)
                         (assoc :step :invite/step-3-commit-claim :task (task-for :claim claim)))}
          {:pstates pstates
           :plan-lines "PLAN.md:invite-step-2-help-c0"
           :summary (str "H(P=" person ") sees competing RootReserving(C0=" c0 ") != " claim " -> hops |hash C0 to help C0")
           :next-msg (assoc msg
                            :step :invite/step-2-help-check-claim
                            :task (task-for :claim c0)
                            :curr-claim c0
                            :target {:help-claim c0 :help-by b0 :help-at t0})})))))

(defn- exec-invite-step [pstates {:keys [step input target] :as msg}]
  (let [{:keys [person claim invited-by at]} input]
    (case step
      :invite/step-1-check-claim
      (let [c (get-in pstates [:claims claim])]
        (cond
          (nil? c)
          {:pstates pstates
           :plan-lines "PLAN.md:invite-step-1-free"
           :summary (str "H(C=" claim ") sees $$claims[C]=nil -> hops |hash P=" person " to Step 2")
           :next-msg (assoc msg :step :invite/step-2-check-or-reserve-root :task (task-for :root person))}

          (= (:person c) person)
          {:pstates pstates
           :plan-lines "PLAN.md:invite-step-1-repeat"
           :summary (str "H(C=" claim ") sees $$claims[C].person == " person " -> hops |hash P to Step 4")
           :next-msg (assoc msg :step :invite/step-4-promote-root :task (task-for :root person))}

          :else
          ;; Revision 1 (Witness 6): clean up any orphaned RootReserving(C) on H(P)
          ;; left by a crashed earlier attempt before returning :claim-in-use.
          {:pstates pstates
           :plan-lines "PLAN.md:invite-step-1-claim-in-use-cleanup"
           :summary (str "H(C=" claim ") sees $$claims[C].person=" (:person c) " != " person " -> hops |hash P to clear any RootReserving(C) then :claim-in-use")
           :next-msg (assoc msg :step :invite/step-3-clear-lost-root :task (task-for :root person))}))

      :invite/step-2-check-or-reserve-root
      (invite-step-2-at-root pstates msg)

      :invite/step-2-help-check-claim
      (let [{:keys [help-claim help-by help-at]} target
            c0 (get-in pstates [:claims help-claim])]
        (cond
          (nil? c0)
          {:pstates (assoc-in pstates [:claims help-claim]
                              {:state :ClaimWaiting :person person :invited-by help-by :invited-at help-at})
           :plan-lines "PLAN.md:invite-step-2-help-commit"
           :summary (str "H(C0=" help-claim ") helping P=" person ": sees nil -> writes ClaimWaiting(P), hops |hash P")
           :next-msg (assoc msg :step :invite/step-2-help-finish-root :task (task-for :root person)
                            :other-won? true)}

          (= (:person c0) person)
          {:pstates pstates
           :plan-lines "PLAN.md:invite-step-2-help-already-won"
           :summary (str "H(C0=" help-claim ") helping P=" person ": sees person==P -> hops |hash P")
           :next-msg (assoc msg :step :invite/step-2-help-finish-root :task (task-for :root person)
                            :other-won? true)}

          :else
          {:pstates pstates
           :plan-lines "PLAN.md:invite-step-2-help-lost"
           :summary (str "H(C0=" help-claim ") helping P=" person ": sees person=" (:person c0) " != P (C0 lost) -> hops |hash P")
           :next-msg (assoc msg :step :invite/step-2-help-finish-root :task (task-for :root person)
                            :other-won? false)}))

      :invite/step-2-help-finish-root
      (let [{:keys [help-claim help-by help-at]} target
            won? (:other-won? msg)
            r (get-in pstates [:root-data person :r])]
        (if won?
          (let [ps1 (if (and (= :RootReserving (:state r)) (= (:claim r) help-claim))
                      (assoc-in pstates [:root-data person :r]
                                {:state :RootInvited :claim help-claim :invited-by help-by :invited-at help-at})
                      pstates)]
            ;; Revision 1 (Witness 9): re-check $$claims[C] before returning :person-in-use
            {:pstates ps1
             :plan-lines "PLAN.md:invite-step-2-help-recheck-claim"
             :summary (str "H(P=" person ") promotes helped C0=" help-claim " to RootInvited -> hops |hash C=" claim " to verify :claim-in-use vs :person-in-use")
             :next-msg (-> msg
                           (dissoc :target :curr-claim :other-won?)
                           (assoc :step :invite/step-2-recheck-claim :task (task-for :claim claim)))})
          (if (and (= :RootReserving (:state r)) (= (:claim r) help-claim))
            {:pstates (assoc-in pstates [:root-data person :r]
                                {:state :RootReserving :claim claim :invited-by invited-by :invited-at at})
             :plan-lines "PLAN.md:invite-step-2-evict-lost"
             :summary (str "H(P=" person ") evicts lost RootReserving(C0=" help-claim ") -> writes RootReserving(C=" claim "), hops |hash C to Step 3")
             :next-msg (-> msg
                           (dissoc :target :curr-claim :other-won?)
                           (assoc :step :invite/step-3-commit-claim :task (task-for :claim claim)))}
            (invite-step-2-at-root pstates (-> msg
                                               (dissoc :target :curr-claim :other-won?)
                                               (update :rounds inc))))))

      :invite/step-2-recheck-claim
      (let [c (get-in pstates [:claims claim])]
        (cond
          (nil? c)
          {:pstates pstates
           :plan-lines "PLAN.md:invite-step-2-recheck-free"
           :summary (str "H(C=" claim ") recheck sees nil -> ack-return {:refused :person-in-use}")
           :ack-return {:refused :person-in-use}}

          (= (:person c) person)
          {:pstates pstates
           :plan-lines "PLAN.md:invite-step-2-recheck-self"
           :summary (str "H(C=" claim ") recheck sees person==P -> hops |hash P to Step 4")
           :next-msg (assoc msg :step :invite/step-4-promote-root :task (task-for :root person))}

          :else
          {:pstates pstates
           :plan-lines "PLAN.md:invite-step-2-recheck-claim-in-use"
           :summary (str "H(C=" claim ") recheck sees person=" (:person c) " != " person " -> ack-return {:refused :claim-in-use}")
           :ack-return {:refused :claim-in-use}}))

      :invite/step-3-commit-claim
      (let [c (get-in pstates [:claims claim])]
        (cond
          (nil? c)
          {:pstates (assoc-in pstates [:claims claim]
                              {:state :ClaimWaiting :person person :invited-by invited-by :invited-at at})
           :plan-lines "PLAN.md:invite-step-3-commit"
           :summary (str "H(C=" claim ") Step 3 sees nil -> commits ClaimWaiting(P=" person "), hops |hash P to Step 4")
           :next-msg (assoc msg :step :invite/step-4-promote-root :task (task-for :root person))}

          (= (:person c) person)
          {:pstates pstates
           :plan-lines "PLAN.md:invite-step-3-already-committed"
           :summary (str "H(C=" claim ") Step 3 sees person==P=" person " -> hops |hash P to Step 4")
           :next-msg (assoc msg :step :invite/step-4-promote-root :task (task-for :root person))}

          :else
          {:pstates pstates
           :plan-lines "PLAN.md:invite-step-3-lost"
           :summary (str "H(C=" claim ") Step 3 sees person=" (:person c) " != " person " -> hops |hash P to clear RootReserving")
           :next-msg (assoc msg :step :invite/step-3-clear-lost-root :task (task-for :root person))}))

      :invite/step-3-clear-lost-root
      (let [r (get-in pstates [:root-data person :r])
            ps1 (if (and (= :RootReserving (:state r)) (= (:claim r) claim))
                  (update-in pstates [:root-data person] dissoc :r)
                  pstates)]
        {:pstates ps1
         :plan-lines "PLAN.md:invite-step-3-clear"
         :summary (str "H(P=" person ") clears RootReserving(C=" claim ") if still C -> ack-return {:refused :claim-in-use}")
         :ack-return {:refused :claim-in-use}})

      :invite/step-4-promote-root
      (let [r (get-in pstates [:root-data person :r])
            ps1 (if (or (nil? r) (and (= :RootReserving (:state r)) (= (:claim r) claim)))
                  (assoc-in pstates [:root-data person :r]
                            {:state :RootInvited :claim claim :invited-by invited-by :invited-at at})
                  pstates)]
        {:pstates ps1
         :plan-lines "PLAN.md:invite-step-4-promote"
         :summary (str "H(P=" person ") Step 4 ensures RootInvited(C=" claim ") -> ack-return {:invited " person "}")
         :ack-return {:invited person}}))))

;; ---- 1B. Revised enter steps (uniform choose-at-account, zero ClaimClaiming)

(defn- claims-of-enter [{:keys [code-hash email]}]
  {:code-claim (when code-hash {:by :code :hash code-hash})
   :email-claim (when email {:by :email :email email})})

(defn- update-says-if-later
  "Updates `:says` and `:says-at` on `$$accounts[account]` ONLY when `(> at says-at)`
  (Sid's Call 2: an enter whose `:at` is not later than the kept one's leaves it)."
  [pstates account says at]
  (let [prev-at (get-in pstates [:accounts account :says-at] Long/MIN_VALUE)]
    (if (> at prev-at)
      (-> pstates
          (assoc-in [:accounts account :says] says)
          (assoc-in [:accounts account :says-at] at))
      pstates)))

(defn- start-probe-or-use
  "Routes an active `AccountLinking` decision (`target`) according to its
  `:status` on H(Acc):
  - `:failing` -> H(op) to ensure `EnterOpFailed` before clearing `AccountLinking`
  - `:chosen`  -> H(op) to confirm `op` is not `EnterOpFailed` before E3
  - `:probing` -> H(C) read-only E2 probe."
  [msg target]
  (case (:status target)
    :failing
    (assoc msg
           :target (dissoc target :code-was-nil? :claiming-person :other-op :other-phase :return-to)
           :phase :fail
           :curr-claim nil
           :step :enter/step-2c-mark-failed
           :task (task-for :op (:op-id target)))

    :chosen
    (let [ch (:chosen target)]
      (assoc msg
             :target (dissoc target :code-was-nil? :claiming-person :other-op :other-phase :return-to)
             :phase :use
             :curr-claim ch
             :step :enter/step-2c-confirm-choice
             :task (task-for :op (:op-id target))))

    ;; :probing
    (let [code-c (:code-claim target)
          email-c (:email-claim target)
          [ph c] (if code-c [:code code-c] [:email email-c])]
      (assoc msg
             :target (dissoc target :code-was-nil? :claiming-person :other-op :other-phase :return-to)
             :phase ph
             :curr-claim c
             :step :enter/step-2-probe-claim
             :task (task-for :claim c)))))

(defn- enter-step-1-at-account
  "Step E1 on H(Acc): returning link (verifies self-op not `:EnterOpFailed`
  before upgrading `:says`, then H(P) flag read), unlinked no-claim refusal
  (records `:EnterOpFailed` on H(self-op)), new `AccountLinking` creation, or
  helping existing `AccountLinking`."
  [pstates {:keys [self-op self-input] :as msg}]
  (let [{:keys [account says own-layer making at]} self-input
        {:keys [code-claim email-claim]} (claims-of-enter self-input)
        acc (get-in pstates [:accounts account])]
    (cond
      (= :AccountLinked (:state acc))
      (let [prev-at (:says-at acc Long/MIN_VALUE)
            link-target {:linked-person (:person acc)
                         :linked-layer (:own-layer acc)
                         :linked-making (:making acc)}]
        (if (> at prev-at)
          ;; Guard against `:all-after` replay of a previously-refused high-`:at` enter.
          {:pstates pstates
           :plan-lines "PLAN.md:enter-step-1-returning-check-op"
           :summary (str "H(Acc=" account ") sees AccountLinked(P=" (:person acc) ") with at=" at " > " prev-at " -> hops |hash self-op to verify not refused")
           :next-msg (-> msg
                         (dissoc :curr-claim :phase :other-won?)
                         (assoc :step :enter/step-1a-check-op-returning
                                :task (task-for :op self-op)
                                :target link-target))}
          {:pstates pstates
           :plan-lines "PLAN.md:enter-step-1-returning"
           :summary (str "H(Acc=" account ") sees AccountLinked(P=" (:person acc) ") with at=" at " <= " prev-at " -> hops |hash P for :own-layer-made?")
           :next-msg (-> msg
                         (dissoc :curr-claim :phase :other-won?)
                         (assoc :step :enter/step-1b-returning-read-flag
                                :task (task-for :root (:person acc))
                                :target link-target))}))

      (nil? acc)
      (if (and (nil? code-claim) (nil? email-claim))
        {:pstates pstates
         :plan-lines "PLAN.md:enter-step-1-no-claims"
         :summary (str "H(Acc=" account ") unlinked & no claims -> hops |hash self-op to mark EnterOpFailed then {:refused :not-invited}")
         :next-msg (-> msg
                       (dissoc :curr-claim :phase :other-won? :target)
                       (assoc :step :enter/step-2c-mark-failed-direct
                              :task (task-for :op self-op)))}
        (let [linking {:state :AccountLinking
                       :op-id self-op
                       :account account
                       :code-claim code-claim
                       :email-claim email-claim
                       :own-layer own-layer
                       :making making
                       :says says
                       :at at
                       :status :probing
                       :chosen nil}]
          {:pstates (assoc-in pstates [:accounts account] linking)
           :plan-lines "PLAN.md:enter-step-1-start-linking"
           :summary (str "H(Acc=" account ") unlinked -> writes AccountLinking(op=" self-op ", status=:probing), hops to E2 probe")
           :next-msg (start-probe-or-use msg linking)}))

      (= :AccountLinking (:state acc))
      {:pstates pstates
       :plan-lines (if (= (:op-id acc) self-op) "PLAN.md:enter-step-1-own-linking" "PLAN.md:enter-step-1-help-linking")
       :summary (str "H(Acc=" account ") sees AccountLinking(op=" (:op-id acc) ", status=" (:status acc) ", chosen=" (:chosen acc) ")")
       :next-msg (start-probe-or-use msg acc)})))

(defn- other-op-won-phase?
  "Determines whether a pre-seeded `ClaimClaiming` owner (`other-op`) committed
  `other-phase` in `$$enter-ops` (used solely to resolve pre-seeded legacy state
  in scenarios `3C-06` and `2C-15`)."
  [eo other-phase]
  (or (nil? eo)
      (and (= :EnterOpWon (:state eo)) (= (:won-phase eo) other-phase))
      (and (= :EnterOpTryEmail (:state eo)) (= :email other-phase))))

(defn- eval-probe-on-claim
  "Evaluates read-only Step E2 probe on H(curr-claim) for `target`."
  [pstates {:keys [self-input target phase curr-claim] :as msg}]
  (let [account (:account self-input)
        op (:op-id target)
        c (get-in pstates [:claims curr-claim])]
    (cond
      (= :ClaimClaiming (:state c))
      {:pstates pstates
       :plan-lines "PLAN.md:enter-step-2-resolve-claiming"
       :summary (str "H(C=" curr-claim ") probe sees pre-seeded ClaimClaiming(other-op=" (:op-id c) ") -> hops |hash other-op")
       :next-msg (assoc msg
                        :step :enter/step-2-resolve-claiming
                        :task (task-for :op (:op-id c))
                        :target (assoc target
                                       :claiming-person (:person c)
                                       :other-op (:op-id c)
                                       :other-phase (:phase c)
                                       :return-to :probe))}

      (or (= :ClaimWaiting (:state c))
          (and (= :ClaimUsed (:state c)) (= (:used-by-op c) op)))
      {:pstates pstates
       :plan-lines "PLAN.md:enter-step-2-usable"
       :summary (str "H(C=" curr-claim ") probe phase=" phase " sees usable " (:state c) "(P=" (:person c) ") -> hops |hash Acc to E2' choose")
       :next-msg (assoc msg
                        :step :enter/step-2b-choose
                        :task (task-for :account account)
                        :curr-claim curr-claim)}

      ;; Not usable: nil or ClaimUsed by another op
      (and (= :code phase) (some? (:email-claim target)))
      {:pstates pstates
       :plan-lines "PLAN.md:enter-step-2-try-email"
       :summary (str "H(C=" curr-claim ") probe missed :code (" (:state c) ") -> hops |hash C_email")
       :next-msg (assoc msg
                        :phase :email
                        :curr-claim (:email-claim target)
                        :target (assoc target :code-was-nil? (nil? c))
                        :step :enter/step-2-probe-claim
                        :task (task-for :claim (:email-claim target)))}

      (and (= :email phase) (:code-was-nil? target) (= :ClaimUsed (:state c)))
      ;; Single-instant linearizability check: C_code was nil at t1 and C_email
      ;; is ClaimUsed at t2 -> re-check C_code at t3 (while C_email stays ClaimUsed).
      {:pstates pstates
       :plan-lines "PLAN.md:enter-step-2-recheck-code"
       :summary (str "H(C=" curr-claim ") probe missed :email (ClaimUsed after nil :code) -> re-checks C_code for linearizable :not-invited")
       :next-msg (assoc msg
                        :phase :code-recheck
                        :curr-claim (:code-claim target)
                        :target (dissoc target :code-was-nil?)
                        :step :enter/step-2-probe-claim
                        :task (task-for :claim (:code-claim target)))}

      :else
      {:pstates pstates
       :plan-lines "PLAN.md:enter-step-2-none"
       :summary (str "H(C=" curr-claim ") probe found no usable claim -> hops |hash Acc to E2' with nil")
       :next-msg (assoc msg
                        :step :enter/step-2b-choose
                        :task (task-for :account account)
                        :curr-claim nil)})))

(defn- eval-use-on-claim
  "Evaluates Step E3 on H(curr-claim): claims `curr-claim` exclusively for `op`."
  [pstates {:keys [self-input target curr-claim] :as msg}]
  (let [account (:account self-input)
        op (:op-id target)
        c (get-in pstates [:claims curr-claim])]
    (cond
      (= :ClaimClaiming (:state c))
      {:pstates pstates
       :plan-lines "PLAN.md:enter-step-3-resolve-claiming"
       :summary (str "H(C=" curr-claim ") E3 sees pre-seeded ClaimClaiming(other-op=" (:op-id c) ") -> hops |hash other-op")
       :next-msg (assoc msg
                        :step :enter/step-2-resolve-claiming
                        :task (task-for :op (:op-id c))
                        :target (assoc target
                                       :claiming-person (:person c)
                                       :other-op (:op-id c)
                                       :other-phase (:phase c)
                                       :return-to :use))}

      (= :ClaimWaiting (:state c))
      (let [P (:person c)]
        {:pstates (assoc-in pstates [:claims curr-claim]
                            (assoc c :state :ClaimUsed :used-by-op op))
         :plan-lines "PLAN.md:enter-step-3-use"
         :summary (str "H(C=" curr-claim ") E3 sees ClaimWaiting(P=" P ") -> writes ClaimUsed(P, op=" op "), hops |hash P to E4")
         :next-msg (assoc msg
                          :target (assoc target :won-person P)
                          :step :enter/step-4-enter-root
                          :task (task-for :root P))})

      (and (= :ClaimUsed (:state c)) (= (:used-by-op c) op))
      (let [P (:person c)]
        {:pstates pstates
         :plan-lines "PLAN.md:enter-step-3-already-ours"
         :summary (str "H(C=" curr-claim ") E3 sees own ClaimUsed(P=" P ", op=" op ") -> hops |hash P to E4")
         :next-msg (assoc msg
                          :target (assoc target :won-person P)
                          :step :enter/step-4-enter-root
                          :task (task-for :root P))})

      :else
      ;; Claim was permanently taken by another op -> CAS-clear :chosen on H(Acc).
      {:pstates pstates
       :plan-lines "PLAN.md:enter-step-3-lost"
       :summary (str "H(C=" curr-claim ") E3 lost claim to " (:used-by-op c) " -> hops |hash Acc to CAS-clear :chosen")
       :next-msg (assoc msg
                        :step :enter/step-3b-clear-chosen
                        :task (task-for :account account))})))

(defn- exec-enter-step [pstates {:keys [step self-op self-input target curr-claim] :as msg}]
  (let [account (:account self-input)
        op (:op-id target)]
    (case step
      :enter/step-1-check-account
      (enter-step-1-at-account pstates msg)

      :enter/step-1a-check-op-returning
      (let [eo (get-in pstates [:enter-ops self-op])]
        (if (= :EnterOpFailed (:state eo))
          {:pstates pstates
           :plan-lines "PLAN.md:enter-step-1a-already-failed"
           :summary (str "H(self-op=" self-op ") sees EnterOpFailed (replayed refusal) -> ack-return {:refused :not-invited}")
           :ack-return {:refused :not-invited}}
          {:pstates pstates
           :plan-lines "PLAN.md:enter-step-1a-not-failed"
           :summary (str "H(self-op=" self-op ") not failed -> hops |hash Acc to update :says if later")
           :next-msg (assoc msg
                            :step :enter/step-1c-returning-update-says
                            :task (task-for :account account))}))

      :enter/step-1c-returning-update-says
      (let [{:keys [linked-person]} target
            ps1 (update-says-if-later pstates account (:says self-input) (:at self-input))]
        {:pstates ps1
         :plan-lines "PLAN.md:enter-step-1c-update-says"
         :summary (str "H(Acc=" account ") updates :says iff at=" (:at self-input) " > says-at -> hops |hash P=" linked-person " for :own-layer-made?")
         :next-msg (assoc msg
                          :step :enter/step-1b-returning-read-flag
                          :task (task-for :root linked-person))})

      :enter/step-1b-returning-read-flag
      (let [{:keys [linked-person linked-layer linked-making]} target
            flag (boolean (get-in pstates [:root-data linked-person :r :own-layer-made?]))]
        {:pstates pstates
         :plan-lines "PLAN.md:enter-step-1b-flag"
         :summary (str "H(P=" linked-person ") reads :own-layer-made?=" flag " -> ack-return {:person " linked-person " ...}")
         :ack-return {:person linked-person
                      :own-layer linked-layer
                      :making linked-making
                      :own-layer-made? flag}})

      :enter/step-2-probe-claim
      (eval-probe-on-claim pstates msg)

      :enter/step-2-resolve-claiming
      (let [{:keys [claiming-person other-op other-phase]} target
            eo (get-in pstates [:enter-ops other-op])]
        (if (other-op-won-phase? eo other-phase)
          (let [ps1 (if (= :EnterOpWon (:state eo))
                      pstates
                      (assoc-in pstates [:enter-ops other-op]
                                {:state :EnterOpWon :person claiming-person :won-phase other-phase}))]
            {:pstates ps1
             :plan-lines "PLAN.md:enter-resolve-claiming-won"
             :summary (str "H(other-op=" other-op ") won " other-phase " -> hops |hash C=" curr-claim " to CAS promote ClaimUsed")
             :next-msg (assoc msg :step :enter/step-2-after-resolve :task (task-for :claim curr-claim) :other-won? true)})
          {:pstates pstates
           :plan-lines "PLAN.md:enter-resolve-claiming-aborted"
           :summary (str "H(other-op=" other-op ") aborted " other-phase " -> hops |hash C=" curr-claim " to CAS restore ClaimWaiting")
           :next-msg (assoc msg :step :enter/step-2-after-resolve :task (task-for :claim curr-claim) :other-won? false)}))

      :enter/step-2-after-resolve
      (let [{:keys [other-op other-phase return-to]} target
            other-won? (:other-won? msg)
            c (get-in pstates [:claims curr-claim])
            ;; STRICT CAS: only transition if $$claims[C] is STILL ClaimClaiming(other-op, other-phase)!
            ps1 (if (and (= :ClaimClaiming (:state c))
                         (= (:op-id c) other-op)
                         (= (:phase c) other-phase))
                  (if other-won?
                    (assoc-in pstates [:claims curr-claim]
                              (-> c (dissoc :op-id :phase) (assoc :state :ClaimUsed :used-by-op other-op)))
                    (assoc-in pstates [:claims curr-claim]
                              (-> c (dissoc :op-id :phase) (assoc :state :ClaimWaiting))))
                  pstates)
            clean-msg (-> msg
                          (dissoc :other-won?)
                          (assoc :target (dissoc target :claiming-person :other-op :other-phase :return-to)))]
        (if (= :use return-to)
          (eval-use-on-claim ps1 clean-msg)
          (eval-probe-on-claim ps1 clean-msg)))

      :enter/step-2b-choose
      (let [acc (get-in pstates [:accounts account])
            probed curr-claim]
        (cond
          (not (and (= :AccountLinking (:state acc)) (= (:op-id acc) op)))
          ;; op already ended (linked or cleared) -> re-evaluate self at Step 1
          (enter-step-1-at-account pstates (-> msg
                                               (dissoc :target :curr-claim :phase :other-won?)
                                               (update :rounds inc)))

          (= :failing (:status acc))
          ;; Already decided to fail on H(Acc) -> ensure EnterOpFailed then clear
          {:pstates pstates
           :plan-lines "PLAN.md:enter-step-2b-follow-failing"
           :summary (str "H(Acc=" account ") E2' sees status=:failing on op=" op " -> hops |hash op")
           :next-msg (start-probe-or-use msg acc)}

          (= :chosen (:status acc))
          ;; Another attempt/helper already committed a choice -> confirm at H(op) then E3
          {:pstates pstates
           :plan-lines "PLAN.md:enter-step-2b-follow-chosen"
           :summary (str "H(Acc=" account ") E2' sees already chosen C=" (:chosen acc) " -> hops |hash op to confirm")
           :next-msg (start-probe-or-use msg acc)}

          (some? probed)
          ;; Commit choice on H(Acc) BEFORE confirming at H(op) and mutating any claim!
          (let [acc' (assoc acc :status :chosen :chosen probed)]
            {:pstates (assoc-in pstates [:accounts account] acc')
             :plan-lines "PLAN.md:enter-step-2b-commit-choice"
             :summary (str "H(Acc=" account ") E2' commits status=:chosen C=" probed " on op=" op " -> hops |hash op to confirm")
             :next-msg (start-probe-or-use msg acc')})

          :else
          ;; probed == nil while :status == :probing -> atomically latch :status :failing on H(Acc)
          (let [acc' (assoc acc :status :failing :chosen nil)]
            {:pstates (assoc-in pstates [:accounts account] acc')
             :plan-lines "PLAN.md:enter-step-2b-latch-failing"
             :summary (str "H(Acc=" account ") E2' latches status=:failing on op=" op " -> hops |hash op to record EnterOpFailed")
             :next-msg (start-probe-or-use msg acc')})))

      :enter/step-2c-confirm-choice
      (let [eo (get-in pstates [:enter-ops op])]
        (if (= :EnterOpFailed (:state eo))
          {:pstates pstates
           :plan-lines "PLAN.md:enter-step-2c-was-failed"
           :summary (str "H(op=" op ") sees EnterOpFailed -> aborts claim use, hops |hash Acc to clear AccountLinking")
           :next-msg (assoc msg
                            :step :enter/step-2d-finish-failed
                            :task (task-for :account account))}
          {:pstates pstates
           :plan-lines "PLAN.md:enter-step-2c-confirmed"
           :summary (str "H(op=" op ") confirmed not failed -> hops |hash C=" curr-claim " to E3")
           :next-msg (assoc msg
                            :step :enter/step-3-use-claim
                            :task (task-for :claim curr-claim))}))

      :enter/step-2c-mark-failed
      (let [eo (get-in pstates [:enter-ops op])
            ps1 (if (= :EnterOpFailed (:state eo))
                  pstates
                  (assoc-in pstates [:enter-ops op] {:state :EnterOpFailed}))]
        {:pstates ps1
         :plan-lines "PLAN.md:enter-step-2c-mark-failed"
         :summary (str "H(op=" op ") records EnterOpFailed -> hops |hash Acc to clear AccountLinking")
         :next-msg (assoc msg
                          :step :enter/step-2d-finish-failed
                          :task (task-for :account account))})

      :enter/step-2c-mark-failed-direct
      (let [eo (get-in pstates [:enter-ops self-op])
            ps1 (if (= :EnterOpFailed (:state eo))
                  pstates
                  (assoc-in pstates [:enter-ops self-op] {:state :EnterOpFailed}))]
        {:pstates ps1
         :plan-lines "PLAN.md:enter-step-2c-mark-failed-direct"
         :summary (str "H(self-op=" self-op ") records EnterOpFailed (no-claim refusal) -> ack-return {:refused :not-invited}")
         :ack-return {:refused :not-invited}})

      :enter/step-2d-finish-failed
      (let [acc (get-in pstates [:accounts account])
            ps1 (if (and (= :AccountLinking (:state acc)) (= (:op-id acc) op))
                  (update pstates :accounts dissoc account)
                  pstates)]
        (if (= op self-op)
          {:pstates ps1
           :plan-lines "PLAN.md:enter-step-2d-refuse"
           :summary (str "H(Acc=" account ") E2d clears AccountLinking(op=" op ") -> ack-return {:refused :not-invited}")
           :ack-return {:refused :not-invited}}
          (enter-step-1-at-account ps1 (-> msg
                                           (dissoc :target :curr-claim :phase :other-won?)
                                           (update :rounds inc)))))

      :enter/step-3-use-claim
      (eval-use-on-claim pstates msg)

      :enter/step-3b-clear-chosen
      (let [acc (get-in pstates [:accounts account])
            failed-claim curr-claim
            ;; STRICT CAS: only clear :chosen if still op, :status :chosen, AND still failed-claim!
            ps1 (if (and (= :AccountLinking (:state acc))
                         (= (:op-id acc) op)
                         (= :chosen (:status acc))
                         (= (:chosen acc) failed-claim))
                  (assoc-in pstates [:accounts account] (assoc acc :status :probing :chosen nil))
                  pstates)]
        (enter-step-1-at-account ps1 (-> msg
                                         (dissoc :target :curr-claim :phase :other-won?)
                                         (update :rounds inc))))

      :enter/step-4-enter-root
      (let [P (:won-person target)
            r (get-in pstates [:root-data P :r])
            ps1 (if (= :RootEntered (:state r))
                  pstates
                  (assoc-in pstates [:root-data P :r]
                            {:state :RootEntered
                             :claim (:claim r)
                             :invited-by (:invited-by r)
                             :invited-at (:invited-at r)
                             :linked-account account
                             :own-layer (:own-layer target)
                             :making (:making target)
                             :own-layer-made? false
                             :next-actor-seq 0
                             :next-sign-in-seq 0}))
            r' (get-in ps1 [:root-data P :r])]
        {:pstates ps1
         :plan-lines "PLAN.md:enter-step-4-root"
         :summary (str "H(P=" P ") E4 ensures RootEntered (fixed L'=" (:own-layer r') ", made?=" (:own-layer-made? r') ") -> hops |hash Acc to E5")
         :next-msg (assoc msg
                          :target (assoc target
                                         :fixed-layer (:own-layer r')
                                         :fixed-making (:making r')
                                         :present-flag (boolean (:own-layer-made? r')))
                          :step :enter/step-5-link-account
                          :task (task-for :account account))})

      :enter/step-5-link-account
      (let [{:keys [won-person fixed-layer fixed-making present-flag]} target
            acc (get-in pstates [:accounts account])
            helping? (not= op self-op)
            ps1 (if (and (= :AccountLinking (:state acc)) (= (:op-id acc) op))
                  (assoc-in pstates [:accounts account]
                            {:state :AccountLinked
                             :person won-person
                             :own-layer fixed-layer
                             :making fixed-making
                             :says (:says target)
                             :says-at (:at target)})
                  pstates)
            acc1 (get-in ps1 [:accounts account])
            need-self-says? (and (= :AccountLinked (:state acc1))
                                 (> (:at self-input) (:says-at acc1 Long/MIN_VALUE)))]
        (cond
          ;; Helper with a later `:at` must verify self-op is not `:EnterOpFailed`
          ;; before upgrading `:says` (protects against `:all-after` refused replay).
          (and helping? need-self-says?)
          {:pstates ps1
           :plan-lines "PLAN.md:enter-step-5-helper-check-op"
           :summary (str "H(Acc=" account ") E5 linked P=" won-person "; helper with later at=" (:at self-input) " -> hops |hash self-op")
           :next-msg (-> msg
                         (dissoc :curr-claim :phase :other-won?)
                         (assoc :step :enter/step-1a-check-op-returning
                                :task (task-for :op self-op)
                                :target {:linked-person won-person
                                         :linked-layer fixed-layer
                                         :linked-making fixed-making}))}

          ;; Own op won (`op == self-op`) -> guaranteed never `:EnterOpFailed`.
          (and (not helping?) need-self-says?)
          {:pstates (update-says-if-later ps1 account (:says self-input) (:at self-input))
           :plan-lines "PLAN.md:enter-step-5-link"
           :summary (str "H(Acc=" account ") E5 finalizes AccountLinked(P=" won-person ") -> ack-return")
           :ack-return {:person won-person
                        :own-layer fixed-layer
                        :making fixed-making
                        :own-layer-made? present-flag}}

          :else
          {:pstates ps1
           :plan-lines "PLAN.md:enter-step-5-link"
           :summary (str "H(Acc=" account ") E5 finalizes AccountLinked(P=" won-person ") with monotonic :says -> ack-return")
           :ack-return {:person won-person
                        :own-layer fixed-layer
                        :making fixed-making
                        :own-layer-made? present-flag}})))))

;; ---- 1C. Revised answer-ask steps (PLAN.md §1C + Revision 4)

(defn- answer-ask-step-1
  "Evaluates Step 1 of `answer-ask` on H(U)."
  [pstates {:keys [self-op self-input] :as msg}]
  (let [{:keys [code yes? root actor new-agent sign-in until at]} self-input
        k (get-in pstates [:asks code])]
    (cond
      (nil? k)
      {:pstates pstates
       :plan-lines "PLAN.md:answer-ask-step-1-missing"
       :summary (str "H(U=" code ") sees nil -> hops |hash P=" root " for take-out then {:refused :no-such-ask}")
       :next-msg (assoc msg
                        :step :answer-ask/step-1b-takeout-and-return
                        :task (task-for :root root)
                        :target {:takeout-root root :takeout-at at :pending-result {:refused :no-such-ask}})}

      (= :AskYes (:state k))
      (let [res {:yes {:root (:root k) :actor (:actor k) :sign-in (:sign-in k)}}]
        {:pstates pstates
         :plan-lines "PLAN.md:answer-ask-step-1-standing-yes"
         :summary (str "H(U=" code ") sees standing AskYes -> hops |hash P=" root " for take-out then " res)
         :next-msg (assoc msg
                          :step :answer-ask/step-1b-takeout-and-return
                          :task (task-for :root root)
                          :target {:takeout-root root :takeout-at at :pending-result res})})

      (= :AskNo (:state k))
      {:pstates pstates
       :plan-lines "PLAN.md:answer-ask-step-1-standing-no"
       :summary (str "H(U=" code ") sees standing AskNo -> hops |hash P=" root " for take-out then {:no " code "}")
       :next-msg (assoc msg
                        :step :answer-ask/step-1b-takeout-and-return
                        :task (task-for :root root)
                        :target {:takeout-root root :takeout-at at :pending-result {:no code}})}

      (= :AskWaiting (:state k))
      (if (or (:born-over? k) (>= at (:ends k)))
        {:pstates pstates
         :plan-lines "PLAN.md:answer-ask-step-1-over"
         :summary (str "H(U=" code ") sees AskWaiting but born-over? or at=" at " >= ends=" (:ends k) " -> hops |hash P=" root " for take-out then {:refused :over}")
         :next-msg (assoc msg
                          :step :answer-ask/step-1b-takeout-and-return
                          :task (task-for :root root)
                          :target {:takeout-root root :takeout-at at :pending-result {:refused :over}})}
        (let [answering {:state :AskAnswering
                         :hash (:hash k) :asks (:asks k) :ends (:ends k) :born-over? (:born-over? k)
                         :op-id self-op :yes? yes? :root root :actor actor
                         :new-agent-label (:label new-agent) :sign-in sign-in
                         :until until :at at}]
          {:pstates (assoc-in pstates [:asks code] answering)
           :plan-lines "PLAN.md:answer-ask-step-1-lock"
           :summary (str "H(U=" code ") writes AskAnswering(op=" self-op ", P=" root ") -> hops |hash P to Step 2")
           :next-msg (assoc msg
                            :target answering
                            :step :answer-ask/step-2-eval-on-root
                            :task (task-for :root root))}))

      (= :AskAnswering (:state k))
      {:pstates pstates
       :plan-lines (if (= (:op-id k) self-op) "PLAN.md:answer-ask-step-1-retry" "PLAN.md:answer-ask-step-1-help")
       :summary (if (= (:op-id k) self-op)
                  (str "H(U=" code ") sees own AskAnswering(op=" self-op ") -> hops |hash P to Step 2")
                  (str "H(U=" code ") sees competing AskAnswering(op0=" (:op-id k) ", P0=" (:root k) ") -> hops |hash P0 to help op0"))
       :next-msg (assoc msg
                        :target k
                        :step :answer-ask/step-2-eval-on-root
                        :task (task-for :root (:root k)))})))

(defn- eval-answer-on-root
  "Evaluates Step 2 of `answer-ask` on H(P_target).
  Revision 4: checks `ok:<op>` BEFORE running `take-out(P, ans.at)` so replays
  and helpers never re-execute `ans.at` take-out."
  [pstates code target]
  (let [{:keys [op-id yes? root actor new-agent-label sign-in until at hash asks]} target
        existing-ok (get-in pstates [:root-data root :ask-outcomes op-id])]
    (if existing-ok
      [pstates existing-ok true]
      (let [ps0 (run-take-out-on-root pstates root at)
            r (get-in ps0 [:root-data root :r])]
        (cond
          (not= :RootEntered (:state r))
          (let [out {:outcome :AskOutcomeRefused :reason :no-such-actor}]
            [(assoc-in ps0 [:root-data root :ask-outcomes op-id] out) out false])

          (false? yes?)
          (let [out {:outcome :AskOutcomeNo :code code}]
            [(assoc-in ps0 [:root-data root :ask-outcomes op-id] out) out false])

          ;; yes? == true, existing actor
          (nil? new-agent-label)
          (let [act-rec (if (= actor root)
                          {:ended? false}
                          (get-in ps0 [:root-data root :actors actor]))
                g-exists? (some? (get-in ps0 [:root-data root :sign-ins sign-in]))]
            (cond
              (nil? act-rec)
              (let [out {:outcome :AskOutcomeRefused :reason :no-such-actor}]
                [(assoc-in ps0 [:root-data root :ask-outcomes op-id] out) out false])

              (:ended? act-rec)
              (let [out {:outcome :AskOutcomeRefused :reason :ended}]
                [(assoc-in ps0 [:root-data root :ask-outcomes op-id] out) out false])

              g-exists?
              (let [out {:outcome :AskOutcomeRefused :reason :in-use}]
                [(assoc-in ps0 [:root-data root :ask-outcomes op-id] out) out false])

              :else
              (let [g-seq (:next-sign-in-seq r 0)
                    born-ended? (<= until at)
                    own-layer (:own-layer r)
                    made-at-begin? (boolean (:own-layer-made? r))
                    out {:outcome :AskOutcomeYes :root root :actor actor :sign-in sign-in}
                    ps1 (-> ps0
                            (assoc-in [:root-data root :r :next-sign-in-seq] (inc g-seq))
                            (assoc-in [:root-data root :sign-ins sign-in]
                                      {:sign-in sign-in :actor actor :kind :program
                                       :initial-hash hash :hash hash :until until
                                       :prev-hash nil :prev-until nil
                                       :ended? born-ended? :begun at :label asks :seq g-seq
                                       :own-layer own-layer :own-layer-made-at-begin? made-at-begin?})
                            (assoc-in [:root-data root :ask-outcomes op-id] out))
                    ps2 (if born-ended?
                          ps1
                          (-> ps1
                              (assoc-in [:live root :sign-ins sign-in]
                                        {:sign-in sign-in :actor actor :kind :program :initial-hash hash :hash hash
                                         :until until :prev-hash nil :prev-until nil
                                         :begun at :label asks :seq g-seq
                                         :own-layer own-layer :own-layer-made-at-begin? made-at-begin?})
                              (assoc-in [:root-data root :expiries [until :sign-in sign-in]]
                                        {:kind :sign-in :id sign-in :parent-id actor :until until})))]
                [ps2 out false])))

          ;; yes? == true, new agent
          :else
          (let [a-exists? (or (= actor root) (some? (get-in ps0 [:root-data root :actors actor])))
                g-exists? (some? (get-in ps0 [:root-data root :sign-ins sign-in]))]
            (if (or a-exists? g-exists?)
              (let [out {:outcome :AskOutcomeRefused :reason :in-use}]
                [(assoc-in ps0 [:root-data root :ask-outcomes op-id] out) out false])
              (let [a-seq (:next-actor-seq r 0)
                    g-seq (:next-sign-in-seq r 0)
                    born-ended? (<= until at)
                    own-layer (:own-layer r)
                    made-at-begin? (boolean (:own-layer-made? r))
                    out {:outcome :AskOutcomeYes :root root :actor actor :sign-in sign-in}
                    ps1 (-> ps0
                            (assoc-in [:root-data root :r :next-actor-seq] (inc a-seq))
                            (assoc-in [:root-data root :r :next-sign-in-seq] (inc g-seq))
                            (assoc-in [:root-data root :actors actor]
                                      {:actor actor :made-by root :depth 1 :label new-agent-label :ended? false :seq a-seq})
                            (assoc-in [:root-data root :actor-order a-seq]
                                      {:seq a-seq :actor actor :made-by root :depth 1 :label new-agent-label :ended? false})
                            (assoc-in [:root-data root :sign-ins sign-in]
                                      {:sign-in sign-in :actor actor :kind :program
                                       :initial-hash hash :hash hash :until until
                                       :prev-hash nil :prev-until nil
                                       :ended? born-ended? :begun at :label asks :seq g-seq
                                       :own-layer own-layer :own-layer-made-at-begin? made-at-begin?})
                            (assoc-in [:root-data root :ask-outcomes op-id] out))
                    ps2 (if born-ended?
                          ps1
                          (-> ps1
                              (assoc-in [:live root :sign-ins sign-in]
                                        {:sign-in sign-in :actor actor :kind :program :initial-hash hash :hash hash
                                         :until until :prev-hash nil :prev-until nil
                                         :begun at :label asks :seq g-seq
                                         :own-layer own-layer :own-layer-made-at-begin? made-at-begin?})
                              (assoc-in [:root-data root :expiries [until :sign-in sign-in]]
                                        {:kind :sign-in :id sign-in :parent-id actor :until until})))]
                [ps2 out false]))))))))

(defn- exec-answer-ask-step [pstates {:keys [step self-op self-input target] :as msg}]
  (let [code (:code self-input)]
    (case step
      :answer-ask/step-1-check-ask
      (answer-ask-step-1 pstates msg)

      :answer-ask/step-1b-takeout-and-return
      (let [{:keys [takeout-root takeout-at pending-result]} target
            no-such-ask? (= {:refused :no-such-ask} pending-result)
            existing-ok (when no-such-ask?
                          (get-in pstates [:root-data takeout-root :ask-outcomes self-op]))
            ps1 (cond
                  existing-ok pstates
                  no-such-ask?
                  (-> (run-take-out-on-root pstates takeout-root takeout-at)
                      (assoc-in [:root-data takeout-root :ask-outcomes self-op]
                                {:outcome :AskOutcomeRefused :reason :no-such-ask}))
                  :else
                  (run-take-out-on-root pstates takeout-root takeout-at))]
        {:pstates ps1
         :plan-lines "PLAN.md:answer-ask-step-1b-takeout"
         :summary (str "H(P=" takeout-root ") runs take-out at " takeout-at " -> ack-return " pending-result)
         :ack-return pending-result})

      :answer-ask/step-2-eval-on-root
      (let [[ps1 out cached?] (eval-answer-on-root pstates code target)]
        {:pstates ps1
         :plan-lines (if cached? "PLAN.md:answer-ask-step-2-cached" "PLAN.md:answer-ask-step-2-eval")
         :summary (str "H(P=" (:root target) ") "
                       (if cached? "replays cached ok:" (str "take-out at " (:at target) " + records ok:"))
                       (:op-id target) "=" (:outcome out) " -> hops |hash U=" code " to Step 3")
         :next-msg (assoc msg
                          :ask-outcome out
                          :step :answer-ask/step-3-finalize-ask
                          :task (task-for :code code))})

      :answer-ask/step-3-finalize-ask
      (let [out (:ask-outcome msg)
            k (get-in pstates [:asks code])
            still-op? (and (= :AskAnswering (:state k)) (= (:op-id k) (:op-id target)))
            helping? (not= (:op-id target) self-op)]
        (case (:outcome out)
          :AskOutcomeYes
          (let [res {:yes {:root (:root out) :actor (:actor out) :sign-in (:sign-in out)}}
                ps1 (if still-op?
                      (assoc-in pstates [:asks code]
                                {:state :AskYes :hash (:hash k) :asks (:asks k) :ends (:ends k)
                                 :root (:root out) :actor (:actor out) :sign-in (:sign-in out)})
                      pstates)]
            ;; Revision 4 (Witness 10): helper ALWAYS runs take-out on self-input's root at self-input's :at!
            (if helping?
              {:pstates ps1
               :plan-lines "PLAN.md:answer-ask-step-3-helper-takeout"
               :summary (str "H(U=" code ") commits AskYes; helper hops |hash P=" (:root self-input) " for take-out at " (:at self-input))
               :next-msg (-> msg
                             (dissoc :ask-outcome)
                             (assoc :step :answer-ask/step-1b-takeout-and-return
                                    :task (task-for :root (:root self-input))
                                    :target {:takeout-root (:root self-input)
                                             :takeout-at (:at self-input)
                                             :pending-result res}))}
              {:pstates ps1
               :plan-lines "PLAN.md:answer-ask-step-3-yes"
               :summary (str "H(U=" code ") commits AskYes -> ack-return " res)
               :ack-return res}))

          :AskOutcomeNo
          (let [res {:no code}
                ps1 (if still-op?
                      (assoc-in pstates [:asks code]
                                {:state :AskNo :hash (:hash k) :asks (:asks k) :ends (:ends k)})
                      pstates)]
            ;; Revision 4 (Witness 10): helper ALWAYS runs take-out on self-input's root at self-input's :at!
            (if helping?
              {:pstates ps1
               :plan-lines "PLAN.md:answer-ask-step-3-helper-takeout"
               :summary (str "H(U=" code ") commits AskNo; helper hops |hash P=" (:root self-input) " for take-out at " (:at self-input))
               :next-msg (-> msg
                             (dissoc :ask-outcome)
                             (assoc :step :answer-ask/step-1b-takeout-and-return
                                    :task (task-for :root (:root self-input))
                                    :target {:takeout-root (:root self-input)
                                             :takeout-at (:at self-input)
                                             :pending-result res}))}
              {:pstates ps1
               :plan-lines "PLAN.md:answer-ask-step-3-no"
               :summary (str "H(U=" code ") commits AskNo -> ack-return " res)
               :ack-return res}))

          :AskOutcomeRefused
          (let [ps1 (if still-op?
                      (assoc-in pstates [:asks code]
                                {:state :AskWaiting :hash (:hash k) :asks (:asks k)
                                 :ends (:ends k) :born-over? (:born-over? k)})
                      pstates)]
            (if helping?
              (let [res (answer-ask-step-1 ps1 (-> msg
                                                   (dissoc :target :ask-outcome)
                                                   (update :rounds inc)))]
                (update res :summary #(str "H(U=" code ") reverted refused helped op0=" (:op-id target) " and looped to Step 1: " %)))
              {:pstates ps1
               :plan-lines "PLAN.md:answer-ask-step-3-refused"
               :summary (str "H(U=" code ") restores AskWaiting (if still op) -> ack-return {:refused " (:reason out) "}")
               :ack-return {:refused (:reason out)}})))))))

;; ---- Single-task companion writes (`own-layer-made`, 2-scan `end-actor`)

(defn- exec-own-layer-made [pstates {:keys [input]}]
  (let [{:keys [person]} input
        r (get-in pstates [:root-data person :r])]
    (if (= :RootEntered (:state r))
      {:pstates (assoc-in pstates [:root-data person :r :own-layer-made?] true)
       :plan-lines "PLAN.md:own-layer-made"
       :summary (str "H(P=" person ") sets :own-layer-made? true -> ack-return {:made " person "}")
       :ack-return {:made person}}
      {:pstates pstates
       :plan-lines "PLAN.md:own-layer-made"
       :summary (str "H(P=" person ") r not RootEntered -> ack-return {:refused :no-such-person}")
       :ack-return {:refused :no-such-person}})))

(defn- exec-end-actor
  "Revision 2: 2-scan (`ao:` forward scan from target.seq + `$$live` scan) cascade."
  [pstates {:keys [input]}]
  (let [{:keys [root actor at]} input
        ps0 (run-take-out-on-root pstates root at)
        r (get-in ps0 [:root-data root :r])
        a (get-in ps0 [:root-data root :actors actor])]
    (cond
      (and a (:ended? a))
      {:pstates ps0
       :plan-lines "PLAN.md:end-actor-repeat"
       :summary (str "H(P=" root ") sees actor " actor " already ended -> ack-return {:ended " actor "}")
       :ack-return {:ended actor}}

      (or (not= :RootEntered (:state r)) (and (not= actor root) (nil? a)))
      {:pstates ps0
       :plan-lines "PLAN.md:end-actor-no-such"
       :summary (str "H(P=" root ") actor " actor " absent -> ack-return {:refused :no-such-actor}")
       :ack-return {:refused :no-such-actor}}

      (= actor root)
      {:pstates ps0
       :plan-lines "PLAN.md:end-actor-person"
       :summary (str "H(P=" root ") actor==P -> ack-return {:refused :is-a-person}")
       :ack-return {:refused :is-a-person}}

      :else
      (let [ao-after (->> (get-in ps0 [:root-data root :actor-order] {})
                          (vals)
                          (filter #(>= (:seq %) (:seq a)))
                          (sort-by :seq))
            [ended-as ps1]
            (reduce (fn [[aset ps] ao]
                      (let [aid (:actor ao)]
                        (if (and (not (:ended? ao))
                                 (or (= aid actor) (contains? aset (:made-by ao))))
                          [(conj aset aid)
                           (-> ps
                               (assoc-in [:root-data root :actors aid :ended?] true)
                               (assoc-in [:root-data root :actor-order (:seq ao) :ended?] true))]
                          [aset ps])))
                    [#{actor} ps0]
                    ao-after)
            live-gs (get-in ps1 [:live root :sign-ins] {})
            [ended-gs ps2]
            (reduce-kv (fn [[gset ps] G g]
                         (if (contains? ended-as (:actor g))
                           [(conj gset G)
                            (-> ps
                                (assoc-in [:root-data root :sign-ins G :ended?] true)
                                (update-in [:live root :sign-ins] dissoc G)
                                (update-in [:root-data root :expiries] dissoc [(:until g) :sign-in G]))]
                           [gset ps]))
                       [#{} ps1]
                       live-gs)
            live-ss (get-in ps2 [:live root :sessions] {})
            ps3 (reduce-kv (fn [ps S s]
                             (if (contains? ended-gs (:sign-in s))
                               (-> ps
                                   (assoc-in [:root-data root :sessions S :closed?] true)
                                   (update-in [:live root :sessions] dissoc S)
                                   (update-in [:root-data root :expiries] dissoc [(:until s) :session S]))
                               ps))
                           ps2
                           live-ss)]
        {:pstates ps3
         :plan-lines "PLAN.md:end-actor-2scan"
         :summary (str "H(P=" root ") 2-scan end-actor cascade under " actor " -> ack-return {:ended " actor "}")
         :ack-return {:ended actor}}))))

(defn execute-step
  "Executes one atomic single-task step of the revised `PLAN.md`."
  [pstates msg]
  (case (:op msg)
    :invite (exec-invite-step pstates msg)
    :enter (exec-enter-step pstates msg)
    :answer-ask (exec-answer-ask-step pstates msg)
    :own-layer-made (exec-own-layer-made pstates msg)
    :end-actor (exec-end-actor pstates msg)
    :make-actor (let [[ps1 res] (seed-make-actor pstates (:input msg))]
                  {:pstates ps1
                   :plan-lines "PLAN.md:make-actor"
                   :summary (str "H(P=" (get-in msg [:input :root]) ") make-actor " (get-in msg [:input :actor]) " -> ack-return " res)
                   :ack-return res})
    :ask (let [[ps1 res] (seed-ask pstates (:input msg))]
           {:pstates ps1
            :plan-lines "PLAN.md:ask"
            :summary (str "H(U=" (get-in msg [:input :code]) ") ask -> ack-return " res)
            :ack-return res})))
