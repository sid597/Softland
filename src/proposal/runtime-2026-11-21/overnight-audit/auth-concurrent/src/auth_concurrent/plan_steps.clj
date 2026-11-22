(ns auth-concurrent.plan-steps
  "Plain-Clojure encoding of PLAN.md's single-task steps across partitions for
  `invite` (PLAN.md §1A, lines 464-507), `enter` with 1 claim and with 2 claims
  (PLAN.md §1B, lines 508-575), `answer-ask` (PLAN.md §1C, lines 576-631),
  take-out on `:root` writes (PLAN.md §2, lines 639-653), plus single-task
  `own-layer-made`, `end-actor`, and the four reads (`who-is`, `ask-of`,
  `under`, `watch-root` / `$$live`).

  Every step function executes atomically on ONE task (`(:task msg)`), mutates
  only PState partitions colocated on that task, and emits zero or one outgoing
  hop message (to a target task) or an `ack-return>` value for its event tree.
  Every step carries its exact `PLAN.md` line range so that any witness history
  can be held line-by-line against `PLAN.md`.")

;; ---------------------------------------------------------- PState empty state

(def empty-pstates
  "The six partitioned PStates of PLAN.md lines 53-183:
   - :root-data  {root -> {:r RootDatum, :actors {A ActorRec}, :actor-order [ActorOrderRec],
                           :children {B #{A}}, :sign-ins {G SignInRec},
                           :actor-sign-ins {A #{G}}, :sessions {S SessionRec},
                           :sign-in-sessions {G #{S}}, :expiries {[until kind id] ExpiryEntry},
                           :ask-outcomes {op-id AskOutcome}}}
   - :live       {root -> {:sign-ins {G LiveSignIn}, :sessions {S LiveSession}}}
   - :accounts   {account -> AccountState}
   - :claims     {claim -> ClaimState}
   - :asks       {code -> AskState}
   - :enter-ops  {op-id -> EnterOpState}"
  {:root-data {}
   :live {}
   :accounts {}
   :claims {}
   :asks {}
   :enter-ops {}})

(defn task-for
  "Maps a partition key to its task identifier. In Rama (`|hash`), distinct
  partition keys land on independent single-threaded tasks unless they hash-collide.
  Using `[:claim C]`, `[:root P]`, `[:account Acc]`, `[:op op-id]`, and `[:code U]`
  gives each partition key its own single-threaded task while preserving strict
  FIFO ordering on every directed `(src-task -> dst-task)` channel."
  [kind k]
  [kind k])

(defn depot-info
  "Returns `{:depot keyword :partition-key any :task task-id}` for a write call,
  matching PLAN.md lines 198-204."
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
  "Closes session S on root P in $$root-data and removes S from $$live, sg:, exp:
  (PLAN.md lines 645-647)."
  [pstates P S]
  (let [s (get-in pstates [:root-data P :sessions S])]
    (if (or (nil? s) (:closed? s))
      pstates
      (let [G (:sign-in s)
            u (:until s)]
        (-> pstates
            (assoc-in [:root-data P :sessions S :closed?] true)
            (update-in [:live P :sessions] dissoc S)
            (update-in [:root-data P :sign-in-sessions G] disj S)
            (update-in [:root-data P :expiries] dissoc [u :session S]))))))

(defn- end-sign-in-on-root
  "Ends sign-in G on root P in $$root-data, removes G from $$live, ag:, exp:,
  and cascades to all open sessions in sg:<G>: (PLAN.md lines 646, 661-666)."
  [pstates P G]
  (let [g (get-in pstates [:root-data P :sign-ins G])]
    (if (or (nil? g) (:ended? g))
      pstates
      (let [A (:actor g)
            u (:until g)
            open-ss (get-in pstates [:root-data P :sign-in-sessions G] #{})
            ps1 (-> pstates
                    (assoc-in [:root-data P :sign-ins G :ended?] true)
                    (update-in [:live P :sign-ins] dissoc G)
                    (update-in [:root-data P :actor-sign-ins A] disj G)
                    (update-in [:root-data P :expiries] dissoc [u :sign-in G]))]
        (reduce (fn [ps S] (close-session-on-root ps P S)) ps1 open-ss)))))

(defn run-take-out-on-root
  "Executes PLAN.md lines 639-651 on H(P): scans exp: entries with `until <= at`
  and ends/closes them atomically on H(P)."
  [pstates P at]
  (let [exps (get-in pstates [:root-data P :expiries] {})
        expired (->> (keys exps)
                     (filter (fn [[until _ _]] (<= until at)))
                     (sort))]
    (reduce (fn [ps [until kind id :as k]]
              (if-not (contains? (get-in ps [:root-data P :expiries]) k)
                ps
                (case kind
                  :session (close-session-on-root ps P id)
                  :sign-in (end-sign-in-on-root ps P id))))
            pstates
            expired)))

;; --------------------------------------------------------- Four reads (PLAN.md)

(defn plan-who-is
  "Executes PLAN.md lines 10, 263-278, 683-698 (`who-is` on H(P))."
  [pstates {:keys [root sign-in session]}]
  (let [r (get-in pstates [:root-data root :r])]
    (if (or (nil? r) (not= :RootEntered (:state r)))
      {:sign-in nil :session nil :own-layer nil :own-layer-made? false}
      (let [g (get-in pstates [:root-data root :sign-ins sign-in])
            s (when session (get-in pstates [:root-data root :sessions session]))
            actor-ended? (if (and g (:ended? g) (not= (:actor g) root))
                           (boolean (get-in pstates [:root-data root :actors (:actor g) :ended?]))
                           false)]
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
         :own-layer (:own-layer r)
         :own-layer-made? (boolean (:own-layer-made? r))}))))

(defn plan-ask-of
  "Executes PLAN.md lines 11, 20 (`ask-of` on H(U)).
  AskWaiting and AskAnswering both project to `:state :waiting :answer nil`."
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
  "Executes PLAN.md lines 12, 21, 280-294 (`under` on H(P))."
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
  "Executes PLAN.md lines 13, 22 (`watch-root` projected `$$live` slice on H(P))."
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
  "Synchronously applies `ask` on H(U) (PLAN.md line 47, 812)."
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
  "Synchronously applies `make-actor` on H(P) (PLAN.md lines 43, 808)."
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
                       {:seq seq-n :actor actor :made-by made-by :label label :ended? false})
             (update-in [:root-data root :children made-by] (fnil conj #{}) actor))
         {:made actor}]))))

(defn seed-begin-sign-in
  "Synchronously applies `begin-sign-in` on H(P) (PLAN.md lines 40, 805)."
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
            ps1 (-> ps
                    (assoc-in [:root-data root :r :next-sign-in-seq] (inc seq-n))
                    (assoc-in [:root-data root :sign-ins sign-in]
                              {:sign-in sign-in :actor actor :kind kind
                               :initial-hash hash :hash hash :until until
                               :prev-hash nil :prev-until nil
                               :ended? born-ended? :begun at :label label :seq seq-n}))]
        [(if born-ended?
           ps1
           (-> ps1
               (assoc-in [:live root :sign-ins sign-in]
                         {:sign-in sign-in :actor actor :kind kind :hash hash
                          :until until :prev-hash nil :prev-until nil
                          :begun at :label label :seq seq-n})
               (update-in [:root-data root :actor-sign-ins actor] (fnil conj #{}) sign-in)
               (assoc-in [:root-data root :expiries [until :sign-in sign-in]]
                         {:kind :sign-in :id sign-in :parent-id actor :until until})))
         {:begun sign-in}]))))

(defn seed-open-session
  "Synchronously applies `open-session` on H(P) (PLAN.md lines 45, 810)."
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
                         {:session session :sign-in sign-in :until until})
               (update-in [:root-data root :sign-in-sessions sign-in] (fnil conj #{}) session)
               (assoc-in [:root-data root :expiries [until :session session]]
                         {:kind :session :id session :parent-id sign-in :until until})))
         {:open session}]))))

;; --------------------------------------------- Step execution engine (PLAN.md)
;; Each `execute-step` takes `[pstates msg]` and returns:
;; `{:pstates pstates'
;;   :plan-lines "PLAN.md:..."
;;   :summary "..."
;;   :next-msg optional-hop-msg
;;   :ack-return optional-result-map}`

(defn make-initial-msg
  "Builds the initial Step-1 message on the source depot's task for an event."
  [{:keys [call-id attempt-id op input op-id]}]
  (let [{:keys [task]} (depot-info [op input])]
    (case op
      :invite
      {:call-id call-id :attempt-id attempt-id :op :invite
       :step :invite/step-1-check-claim
       :task task
       :input input}

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
       :input input})))

;; ---- 1A. invite steps (PLAN.md lines 464-487)

(defn- invite-step-2-at-root
  "Evaluates Step 2 of `invite` on H(P) (PLAN.md lines 473-482)."
  [pstates {:keys [input] :as msg}]
  (let [{:keys [person claim invited-by at]} input
        r (get-in pstates [:root-data person :r])]
    (cond
      (and r (#{:RootInvited :RootEntered} (:state r)))
      (if (= (:claim r) claim)
        {:pstates pstates
         :plan-lines "PLAN.md:474"
         :summary (str "H(P=" person ") sees " (:state r) " with same C=" claim " -> ack-return {:invited " person "}")
         :ack-return {:invited person}}
        {:pstates pstates
         :plan-lines "PLAN.md:474"
         :summary (str "H(P=" person ") sees " (:state r) " with other claim " (:claim r) " != " claim " -> ack-return {:refused :person-in-use}")
         :ack-return {:refused :person-in-use}})

      (nil? r)
      {:pstates (assoc-in pstates [:root-data person :r]
                          {:state :RootReserving :claim claim :invited-by invited-by :invited-at at})
       :plan-lines "PLAN.md:475"
       :summary (str "H(P=" person ") sees r=nil -> writes RootReserving(C=" claim "), hops |hash C to Step 3")
       :next-msg (assoc msg :step :invite/step-3-commit-claim :task (task-for :claim claim))}

      (= :RootReserving (:state r))
      (let [{c0 :claim b0 :invited-by t0 :invited-at} r]
        (if (= c0 claim)
          {:pstates pstates
           :plan-lines "PLAN.md:477"
           :summary (str "H(P=" person ") sees RootReserving(same C=" claim ") -> hops |hash C to Step 3")
           :next-msg (assoc msg :step :invite/step-3-commit-claim :task (task-for :claim claim))}
          {:pstates pstates
           :plan-lines "PLAN.md:478"
           :summary (str "H(P=" person ") sees competing RootReserving(C0=" c0 ") != " claim " -> hops |hash C0 to help C0")
           :next-msg (assoc msg
                            :step :invite/step-2-help-check-claim
                            :task (task-for :claim c0)
                            :help-claim c0 :help-by b0 :help-at t0)})))))

(defn- exec-invite-step [pstates {:keys [step input] :as msg}]
  (let [{:keys [person claim invited-by at]} input]
    (case step
      :invite/step-1-check-claim
      (let [c (get-in pstates [:claims claim])]
        (cond
          (nil? c)
          {:pstates pstates
           :plan-lines "PLAN.md:472"
           :summary (str "H(C=" claim ") sees $$claims[C]=nil -> hops |hash P=" person " to Step 2")
           :next-msg (assoc msg :step :invite/step-2-check-or-reserve-root :task (task-for :root person))}

          (= (:person c) person)
          {:pstates pstates
           :plan-lines "PLAN.md:472"
           :summary (str "H(C=" claim ") sees $$claims[C].person == " person " -> hops |hash P to Step 4")
           :next-msg (assoc msg :step :invite/step-4-promote-root :task (task-for :root person))}

          :else
          {:pstates pstates
           :plan-lines "PLAN.md:472"
           :summary (str "H(C=" claim ") sees $$claims[C].person=" (:person c) " != " person " -> immediate 0-hop ack-return {:refused :claim-in-use}")
           :ack-return {:refused :claim-in-use}}))

      :invite/step-2-check-or-reserve-root
      (invite-step-2-at-root pstates msg)

      :invite/step-2-help-check-claim
      (let [{:keys [help-claim help-by help-at]} msg
            c0 (get-in pstates [:claims help-claim])]
        (cond
          (nil? c0)
          {:pstates (assoc-in pstates [:claims help-claim]
                              {:state :ClaimWaiting :person person :invited-by help-by :invited-at help-at})
           :plan-lines "PLAN.md:479"
           :summary (str "H(C0=" help-claim ") helping P=" person ": sees nil -> writes ClaimWaiting(P), hops |hash P")
           :next-msg (assoc msg :step :invite/step-2-help-finish-root :task (task-for :root person) :help-outcome :won)}

          (= (:person c0) person)
          {:pstates pstates
           :plan-lines "PLAN.md:480"
           :summary (str "H(C0=" help-claim ") helping P=" person ": sees person==P -> hops |hash P")
           :next-msg (assoc msg :step :invite/step-2-help-finish-root :task (task-for :root person) :help-outcome :won)}

          :else
          {:pstates pstates
           :plan-lines "PLAN.md:481"
           :summary (str "H(C0=" help-claim ") helping P=" person ": sees person=" (:person c0) " != P (C0 lost) -> hops |hash P")
           :next-msg (assoc msg :step :invite/step-2-help-finish-root :task (task-for :root person) :help-outcome :lost)}))

      :invite/step-2-help-finish-root
      (let [{:keys [help-claim help-by help-at help-outcome]} msg
            r (get-in pstates [:root-data person :r])]
        (if (= :won help-outcome)
          (let [ps1 (if (and (= :RootReserving (:state r)) (= (:claim r) help-claim))
                      (assoc-in pstates [:root-data person :r]
                                {:state :RootInvited :claim help-claim :invited-by help-by :invited-at help-at})
                      pstates)]
            {:pstates ps1
             :plan-lines "PLAN.md:479-480"
             :summary (str "H(P=" person ") promotes helped C0=" help-claim " to RootInvited -> ack-return {:refused :person-in-use}")
             :ack-return {:refused :person-in-use}})
          ;; :lost (PLAN.md:481)
          (if (and (= :RootReserving (:state r)) (= (:claim r) help-claim))
            {:pstates (assoc-in pstates [:root-data person :r]
                                {:state :RootReserving :claim claim :invited-by invited-by :invited-at at})
             :plan-lines "PLAN.md:481"
             :summary (str "H(P=" person ") evicts lost RootReserving(C0=" help-claim ") -> writes RootReserving(C=" claim "), hops |hash C to Step 3")
             :next-msg (-> msg
                           (dissoc :help-claim :help-by :help-at :help-outcome)
                           (assoc :step :invite/step-3-commit-claim :task (task-for :claim claim)))}
            (invite-step-2-at-root pstates (dissoc msg :help-claim :help-by :help-at :help-outcome)))))

      :invite/step-3-commit-claim
      (let [c (get-in pstates [:claims claim])]
        (cond
          (nil? c)
          {:pstates (assoc-in pstates [:claims claim]
                              {:state :ClaimWaiting :person person :invited-by invited-by :invited-at at})
           :plan-lines "PLAN.md:483"
           :summary (str "H(C=" claim ") Step 3 sees nil -> commits ClaimWaiting(P=" person "), hops |hash P to Step 4")
           :next-msg (assoc msg :step :invite/step-4-promote-root :task (task-for :root person))}

          (= (:person c) person)
          {:pstates pstates
           :plan-lines "PLAN.md:484"
           :summary (str "H(C=" claim ") Step 3 sees person==P=" person " -> hops |hash P to Step 4")
           :next-msg (assoc msg :step :invite/step-4-promote-root :task (task-for :root person))}

          :else
          {:pstates pstates
           :plan-lines "PLAN.md:485"
           :summary (str "H(C=" claim ") Step 3 sees person=" (:person c) " != " person " -> hops |hash P to clear RootReserving")
           :next-msg (assoc msg :step :invite/step-3-clear-lost-root :task (task-for :root person))}))

      :invite/step-3-clear-lost-root
      (let [r (get-in pstates [:root-data person :r])
            ps1 (if (and (= :RootReserving (:state r)) (= (:claim r) claim))
                  (update-in pstates [:root-data person] dissoc :r)
                  pstates)]
        {:pstates ps1
         :plan-lines "PLAN.md:485"
         :summary (str "H(P=" person ") clears RootReserving(C=" claim ") if still C -> ack-return {:refused :claim-in-use}")
         :ack-return {:refused :claim-in-use}})

      :invite/step-4-promote-root
      (let [r (get-in pstates [:root-data person :r])
            ps1 (if (or (nil? r) (and (= :RootReserving (:state r)) (= (:claim r) claim)))
                  (assoc-in pstates [:root-data person :r]
                            {:state :RootInvited :claim claim :invited-by invited-by :invited-at at})
                  pstates)]
        {:pstates ps1
         :plan-lines "PLAN.md:486"
         :summary (str "H(P=" person ") Step 4 ensures RootInvited(C=" claim ") -> ack-return {:invited " person "}")
         :ack-return {:invited person}}))))

;; Fix argument order on recursive fallback in `:invite/step-2-help-finish-root`:
;; Wait — let's make sure `(invite-step-2-at-root pstates ...)` has `[pstates msg]`!

;; ---- 1B. enter steps (PLAN.md lines 508-546)

(defn- claims-of-enter [{:keys [code-hash email]}]
  {:code-claim (when code-hash {:by :code :hash code-hash})
   :email-claim (when email {:by :email :email email})})

(defn- start-enter-step-2
  "Routes from Step 1 on H(Acc) into Step 2 (1-claim fast path vs 2-claim path,
  PLAN.md lines 520-542) for `target` (`{:op-id ... :code-claim ... :email-claim ...}`)."
  [msg target]
  (let [{:keys [code-claim email-claim]} target]
    (if (and code-claim email-claim)
      (assoc msg
             :target target
             :phase :code
             :curr-claim code-claim
             :step :enter/step-2-2claim-try-claim
             :task (task-for :claim code-claim))
      (let [c (or code-claim email-claim)]
        (assoc msg
               :target target
               :curr-claim c
               :step :enter/step-2-1claim-acquire
               :task (task-for :claim c))))))

(defn- enter-step-1-at-account
  "Evaluates Step 1 of `enter` on H(Acc) (PLAN.md lines 512-519)."
  [pstates {:keys [self-op self-input] :as msg}]
  (let [{:keys [account says own-layer making at]} self-input
        {:keys [code-claim email-claim]} (claims-of-enter self-input)
        acc (get-in pstates [:accounts account])]
    (cond
      (= :AccountLinked (:state acc))
      (let [ps1 (-> pstates
                    (assoc-in [:accounts account :says] says)
                    (assoc-in [:accounts account :says-at] at))]
        {:pstates ps1
         :plan-lines "PLAN.md:513"
         :summary (str "H(Acc=" account ") sees AccountLinked(P=" (:person acc) ") -> overwrites :says/:says-at (at=" at "), hops |hash P for :own-layer-made?")
         :next-msg (assoc msg
                          :step :enter/step-1b-returning-read-flag
                          :task (task-for :root (:person acc))
                          :linked-person (:person acc)
                          :linked-layer (:own-layer acc)
                          :linked-making (:making acc))})

      (nil? acc)
      (if (and (nil? code-claim) (nil? email-claim))
        {:pstates pstates
         :plan-lines "PLAN.md:515"
         :summary (str "H(Acc=" account ") unlinked & no claims -> 0-hop ack-return {:refused :not-invited}")
         :ack-return {:refused :not-invited}}
        (let [linking {:state :AccountLinking
                       :op-id self-op
                       :account account
                       :code-claim code-claim
                       :email-claim email-claim
                       :own-layer own-layer
                       :making making
                       :says says
                       :at at}]
          {:pstates (assoc-in pstates [:accounts account] linking)
           :plan-lines "PLAN.md:516"
           :summary (str "H(Acc=" account ") unlinked -> writes AccountLinking(op=" self-op "), hops to Step 2")
           :next-msg (start-enter-step-2 msg linking)}))

      (= :AccountLinking (:state acc))
      (if (= (:op-id acc) self-op)
        {:pstates pstates
         :plan-lines "PLAN.md:518"
         :summary (str "H(Acc=" account ") sees own AccountLinking(op=" self-op ") -> continues to Step 2")
         :next-msg (start-enter-step-2 msg acc)}
        {:pstates pstates
         :plan-lines "PLAN.md:519"
         :summary (str "H(Acc=" account ") sees AccountLinking(op0=" (:op-id acc) " != " self-op ") -> helps op0 finish Step 2")
         :next-msg (start-enter-step-2 msg acc)}))))

(defn- other-op-won-phase?
  "Checks PLAN.md line 536 on H(other-op): whether `other-op` wins `other-phase`."
  [eo other-phase]
  (or (nil? eo)
      (and (= :EnterOpWon (:state eo)) (= (:won-phase eo) other-phase))
      (and (= :EnterOpTryEmail (:state eo)) (= :email other-phase))))

(defn- exec-enter-step [pstates {:keys [step self-op self-input target] :as msg}]
  (let [account (:account self-input)
        op (:op-id target)]
    (case step
      :enter/step-1-check-account
      (enter-step-1-at-account pstates msg)

      :enter/step-1b-returning-read-flag
      (let [{:keys [linked-person linked-layer linked-making]} msg
            flag (boolean (get-in pstates [:root-data linked-person :r :own-layer-made?]))]
        {:pstates pstates
         :plan-lines "PLAN.md:513"
         :summary (str "H(P=" linked-person ") reads :own-layer-made?=" flag " -> ack-return {:person " linked-person " ...}")
         :ack-return {:person linked-person
                      :own-layer linked-layer
                      :making linked-making
                      :own-layer-made? flag}})

      ;; ---- 1-claim fast path (PLAN.md:521-526)
      :enter/step-2-1claim-acquire
      (let [C (:curr-claim msg)
            c (get-in pstates [:claims C])]
        (cond
          (= :ClaimWaiting (:state c))
          (let [P (:person c)]
            {:pstates (assoc-in pstates [:claims C]
                                (assoc c :state :ClaimUsed :used-by-op op))
             :plan-lines "PLAN.md:523"
             :summary (str "H(C=" C ") 1-claim fast path sees ClaimWaiting(P=" P ") -> writes ClaimUsed(P, op=" op ") directly, hops |hash P to Step 3")
             :next-msg (assoc msg :won-person P :step :enter/step-3a-enter-root :task (task-for :root P))})

          (= :ClaimUsed (:state c))
          (if (= (:used-by-op c) op)
            (let [P (:person c)]
              {:pstates pstates
               :plan-lines "PLAN.md:524"
               :summary (str "H(C=" C ") 1-claim sees ClaimUsed by same op=" op " -> hops |hash P=" P " to Step 3")
               :next-msg (assoc msg :won-person P :step :enter/step-3a-enter-root :task (task-for :root P))})
            {:pstates pstates
             :plan-lines "PLAN.md:524"
             :summary (str "H(C=" C ") 1-claim sees ClaimUsed by other op=" (:used-by-op c) " -> miss, hops |hash Acc")
             :next-msg (assoc msg :step :enter/step-2-miss-cleanup :task (task-for :account account))})

          (= :ClaimClaiming (:state c))
          {:pstates pstates
           :plan-lines "PLAN.md:525"
           :summary (str "H(C=" C ") 1-claim sees ClaimClaiming(P=" (:person c) ", other-op=" (:op-id c) ") -> hops |hash other-op")
           :next-msg (assoc msg
                            :step :enter/step-2-1claim-help-op
                            :task (task-for :op (:op-id c))
                            :claiming-person (:person c)
                            :other-op (:op-id c)
                            :other-phase (:phase c))}

          (nil? c)
          {:pstates pstates
           :plan-lines "PLAN.md:526"
           :summary (str "H(C=" C ") 1-claim sees nil -> miss, hops |hash Acc")
           :next-msg (assoc msg :step :enter/step-2-miss-cleanup :task (task-for :account account))}))

      :enter/step-2-1claim-help-op
      (let [{:keys [curr-claim claiming-person other-op other-phase]} msg
            eo (get-in pstates [:enter-ops other-op])]
        (if (other-op-won-phase? eo other-phase)
          (let [ps1 (if (= :EnterOpWon (:state eo))
                      pstates
                      (assoc-in pstates [:enter-ops other-op]
                                {:state :EnterOpWon :person claiming-person :won-phase other-phase}))]
            {:pstates ps1
             :plan-lines "PLAN.md:525,536"
             :summary (str "H(other-op=" other-op ") sees " (:state eo) " -> helps other-op commit EnterOpWon, hops |hash C")
             :next-msg (assoc msg :step :enter/step-2-1claim-after-help :task (task-for :claim curr-claim) :other-won? true)})
          {:pstates pstates
           :plan-lines "PLAN.md:525,537"
           :summary (str "H(other-op=" other-op ") sees " (:state eo) " (other-op aborted " other-phase ") -> hops |hash C to claim")
           :next-msg (assoc msg :step :enter/step-2-1claim-after-help :task (task-for :claim curr-claim) :other-won? false)}))

      :enter/step-2-1claim-after-help
      (let [{:keys [curr-claim claiming-person other-op other-won?]} msg
            c (get-in pstates [:claims curr-claim])]
        (if other-won?
          (let [ps1 (if (and c (not= :ClaimUsed (:state c)))
                      (assoc-in pstates [:claims curr-claim]
                                (assoc c :state :ClaimUsed :used-by-op other-op))
                      pstates)]
            {:pstates ps1
             :plan-lines "PLAN.md:525"
             :summary (str "H(C=" curr-claim ") promotes C to ClaimUsed(other-op=" other-op ") -> miss, hops |hash Acc")
             :next-msg (assoc msg :step :enter/step-2-miss-cleanup :task (task-for :account account))})
          ;; Verbatim PLAN.md:525: "if other-op aborted other-phase, overwrite C with ClaimUsed(P, op) and go to Step 3."
          {:pstates (assoc-in pstates [:claims curr-claim]
                              {:state :ClaimUsed :person claiming-person
                               :invited-by (:invited-by c) :invited-at (:invited-at c)
                               :used-by-op op})
           :plan-lines "PLAN.md:525"
           :summary (str "H(C=" curr-claim ") overwrites aborted ClaimClaiming with ClaimUsed(P=" claiming-person ", op=" op "), hops |hash P to Step 3")
           :next-msg (assoc msg :won-person claiming-person :step :enter/step-3a-enter-root :task (task-for :root claiming-person))}))

      ;; ---- 2-claim coordinated path (PLAN.md:527-542)
      :enter/step-2-2claim-try-claim
      (let [{:keys [phase curr-claim]} msg
            c (get-in pstates [:claims curr-claim])]
        (cond
          (= :ClaimWaiting (:state c))
          (let [P (:person c)]
            {:pstates (assoc-in pstates [:claims curr-claim]
                                (assoc c :state :ClaimClaiming :op-id op :phase phase))
             :plan-lines (if (= :code phase) "PLAN.md:530" "PLAN.md:541")
             :summary (str "H(C=" curr-claim ") 2-claim phase=" phase " sees ClaimWaiting(P=" P ") -> writes ClaimClaiming(P, op=" op ", " phase "), hops |hash op")
             :next-msg (assoc msg :claiming-person P :step :enter/step-2-2claim-commit-op :task (task-for :op op))})

          (and (= :ClaimClaiming (:state c)) (= (:op-id c) op) (= (:phase c) phase))
          {:pstates pstates
           :plan-lines (if (= :code phase) "PLAN.md:534" "PLAN.md:541")
           :summary (str "H(C=" curr-claim ") 2-claim sees own ClaimClaiming(op=" op ", " phase ") -> hops |hash op")
           :next-msg (assoc msg :claiming-person (:person c) :step :enter/step-2-2claim-commit-op :task (task-for :op op))}

          (= :ClaimClaiming (:state c))
          {:pstates pstates
           :plan-lines (if (= :code phase) "PLAN.md:535" "PLAN.md:541")
           :summary (str "H(C=" curr-claim ") 2-claim sees ClaimClaiming(other-op=" (:op-id c) ") -> hops |hash other-op")
           :next-msg (assoc msg
                            :claiming-person (:person c)
                            :other-op (:op-id c)
                            :other-phase (:phase c)
                            :step :enter/step-2-2claim-help-op
                            :task (task-for :op (:op-id c)))}

          (and (= :ClaimUsed (:state c)) (= (:used-by-op c) op))
          (let [P (:person c)]
            {:pstates pstates
             :plan-lines (if (= :code phase) "PLAN.md:538" "PLAN.md:541")
             :summary (str "H(C=" curr-claim ") 2-claim sees ClaimUsed by same op=" op " -> hops |hash P=" P " to Step 3")
             :next-msg (assoc msg :won-person P :step :enter/step-3a-enter-root :task (task-for :root P))})

          :else
          {:pstates pstates
           :plan-lines (if (= :code phase) "PLAN.md:539" "PLAN.md:542")
           :summary (str "H(C=" curr-claim ") 2-claim missed " phase " -> hops |hash op=" op)
           :next-msg (assoc msg
                            :step (if (= :code phase)
                                    :enter/step-2-2claim-code-missed
                                    :enter/step-2-2claim-email-missed)
                            :task (task-for :op op))}))

      :enter/step-2-2claim-help-op
      (let [{:keys [curr-claim claiming-person other-op other-phase]} msg
            eo (get-in pstates [:enter-ops other-op])]
        (if (other-op-won-phase? eo other-phase)
          (let [ps1 (if (= :EnterOpWon (:state eo))
                      pstates
                      (assoc-in pstates [:enter-ops other-op]
                                {:state :EnterOpWon :person claiming-person :won-phase other-phase}))]
            {:pstates ps1
             :plan-lines "PLAN.md:536"
             :summary (str "H(other-op=" other-op ") helps other-op commit EnterOpWon -> hops |hash C")
             :next-msg (assoc msg :step :enter/step-2-2claim-after-help :task (task-for :claim curr-claim) :other-won? true)})
          {:pstates pstates
           :plan-lines "PLAN.md:537"
           :summary (str "H(other-op=" other-op ") sees other-op moved past " other-phase " -> hops |hash C to evict")
           :next-msg (assoc msg :step :enter/step-2-2claim-after-help :task (task-for :claim curr-claim) :other-won? false)}))

      :enter/step-2-2claim-after-help
      (let [{:keys [phase curr-claim claiming-person other-op other-won?]} msg
            c (get-in pstates [:claims curr-claim])]
        (if other-won?
          (let [ps1 (if (and c (not= :ClaimUsed (:state c)))
                      (assoc-in pstates [:claims curr-claim]
                                (assoc c :state :ClaimUsed :used-by-op other-op))
                      pstates)]
            {:pstates ps1
             :plan-lines "PLAN.md:536"
             :summary (str "H(C=" curr-claim ") promotes C to ClaimUsed(other-op=" other-op ") -> missed " phase ", hops |hash op")
             :next-msg (assoc msg
                              :step (if (= :code phase)
                                      :enter/step-2-2claim-code-missed
                                      :enter/step-2-2claim-email-missed)
                              :task (task-for :op op))})
          ;; Verbatim PLAN.md:537: "hop |hash C_code, evict stale ClaimClaiming and write ClaimClaiming(P, op, :code), then hop |hash op to commit op!"
          {:pstates (assoc-in pstates [:claims curr-claim]
                              {:state :ClaimClaiming :person claiming-person
                               :invited-by (:invited-by c) :invited-at (:invited-at c)
                               :op-id op :phase phase})
           :plan-lines "PLAN.md:537"
           :summary (str "H(C=" curr-claim ") evicts stale ClaimClaiming & writes ClaimClaiming(P=" claiming-person ", op=" op ", " phase "), hops |hash op")
           :next-msg (assoc msg :step :enter/step-2-2claim-commit-op :task (task-for :op op))}))

      :enter/step-2-2claim-commit-op
      (let [{:keys [phase curr-claim claiming-person]} msg
            eo (get-in pstates [:enter-ops op])
            can-win? (if (= :code phase)
                       (or (nil? eo) (and (= :EnterOpWon (:state eo)) (= :code (:won-phase eo))))
                       (or (= :EnterOpTryEmail (:state eo))
                           (and (= :EnterOpWon (:state eo)) (= :email (:won-phase eo)))))]
        (if can-win?
          (let [ps1 (if (= :EnterOpWon (:state eo))
                      pstates
                      (assoc-in pstates [:enter-ops op]
                                {:state :EnterOpWon :person claiming-person :won-phase phase}))]
            {:pstates ps1
             :plan-lines (if (= :code phase) "PLAN.md:531-532" "PLAN.md:541")
             :summary (str "H(op=" op ") commits EnterOpWon(P=" claiming-person ", " phase ") -> hops |hash C to promote ClaimUsed")
             :next-msg (assoc msg
                              :won-person claiming-person
                              :won-claim curr-claim
                              :step :enter/step-2-2claim-promote-used
                              :task (task-for :claim curr-claim))})
          ;; Abort tentative ClaimClaiming (PLAN.md:533)
          {:pstates pstates
           :plan-lines (if (= :code phase) "PLAN.md:533" "PLAN.md:541")
           :summary (str "H(op=" op ") sees " (:state eo) " -> aborts tentative " phase ", hops |hash C to restore ClaimWaiting")
           :next-msg (assoc msg
                            :observed-eo eo
                            :step :enter/step-2-2claim-abort-restore
                            :task (task-for :claim curr-claim))}))

      :enter/step-2-2claim-promote-used
      (let [{:keys [won-person won-claim]} msg
            c (get-in pstates [:claims won-claim])
            ps1 (if (and c (not= :ClaimUsed (:state c)))
                  (assoc-in pstates [:claims won-claim]
                            (assoc c :state :ClaimUsed :used-by-op op))
                  pstates)]
        {:pstates ps1
         :plan-lines "PLAN.md:531-532"
         :summary (str "H(C=" won-claim ") promotes to ClaimUsed(P=" won-person ", op=" op ") -> hops |hash P to Step 3")
         :next-msg (assoc msg :step :enter/step-3a-enter-root :task (task-for :root won-person))})

      :enter/step-2-2claim-abort-restore
      (let [{:keys [phase curr-claim observed-eo]} msg
            c (get-in pstates [:claims curr-claim])
            ps1 (if (and (= :ClaimClaiming (:state c)) (= (:op-id c) op) (= (:phase c) phase))
                  (-> pstates
                      (assoc-in [:claims curr-claim]
                                (-> c (dissoc :op-id :phase) (assoc :state :ClaimWaiting))))
                  pstates)
            next-m (case (:state observed-eo)
                     :EnterOpTryEmail
                     (assoc msg
                            :phase :email
                            :curr-claim (:email-claim target)
                            :step :enter/step-2-2claim-try-claim
                            :task (task-for :claim (:email-claim target)))
                     :EnterOpWon
                     (let [wc (if (= :code (:won-phase observed-eo))
                                (:code-claim target)
                                (:email-claim target))]
                       (assoc msg
                              :won-person (:person observed-eo)
                              :won-claim wc
                              :step :enter/step-2-2claim-promote-used
                              :task (task-for :claim wc)))
                     :EnterOpFailed
                     (assoc msg :step :enter/step-2-miss-cleanup :task (task-for :account account)))]
        {:pstates ps1
         :plan-lines "PLAN.md:533"
         :summary (str "H(C=" curr-claim ") restores ClaimWaiting (if still op=" op ", " phase ") and follows " (:state observed-eo))
         :next-msg next-m})

      :enter/step-2-2claim-code-missed
      (let [eo (get-in pstates [:enter-ops op])]
        (cond
          (or (nil? eo) (= :EnterOpTryEmail (:state eo)))
          {:pstates (if (nil? eo)
                      (assoc-in pstates [:enter-ops op] {:state :EnterOpTryEmail})
                      pstates)
           :plan-lines "PLAN.md:539"
           :summary (str "H(op=" op ") commits EnterOpTryEmail -> hops |hash C_email")
           :next-msg (assoc msg
                            :phase :email
                            :curr-claim (:email-claim target)
                            :step :enter/step-2-2claim-try-claim
                            :task (task-for :claim (:email-claim target)))}

          (= :EnterOpWon (:state eo))
          (let [wc (if (= :code (:won-phase eo)) (:code-claim target) (:email-claim target))]
            {:pstates pstates
             :plan-lines "PLAN.md:539"
             :summary (str "H(op=" op ") already EnterOpWon(P=" (:person eo) ") -> hops to won claim")
             :next-msg (assoc msg
                              :won-person (:person eo)
                              :won-claim wc
                              :step :enter/step-2-2claim-promote-used
                              :task (task-for :claim wc))})

          (= :EnterOpFailed (:state eo))
          {:pstates pstates
           :plan-lines "PLAN.md:539"
           :summary (str "H(op=" op ") already EnterOpFailed -> hops |hash Acc to cleanup")
           :next-msg (assoc msg :step :enter/step-2-miss-cleanup :task (task-for :account account))}))

      :enter/step-2-2claim-email-missed
      (let [eo (get-in pstates [:enter-ops op])]
        (if (= :EnterOpWon (:state eo))
          (let [wc (if (= :code (:won-phase eo)) (:code-claim target) (:email-claim target))]
            {:pstates pstates
             :plan-lines "PLAN.md:542"
             :summary (str "H(op=" op ") sees EnterOpWon -> hops to won claim")
             :next-msg (assoc msg
                              :won-person (:person eo)
                              :won-claim wc
                              :step :enter/step-2-2claim-promote-used
                              :task (task-for :claim wc))})
          {:pstates (if (= :EnterOpTryEmail (:state eo))
                      (assoc-in pstates [:enter-ops op] {:state :EnterOpFailed})
                      pstates)
           :plan-lines "PLAN.md:542"
           :summary (str "H(op=" op ") writes EnterOpFailed -> hops |hash Acc to cleanup")
           :next-msg (assoc msg :step :enter/step-2-miss-cleanup :task (task-for :account account))}))

      :enter/step-2-miss-cleanup
      (let [acc (get-in pstates [:accounts account])
            ps1 (if (and (= :AccountLinking (:state acc)) (= (:op-id acc) op))
                  (update pstates :accounts dissoc account)
                  pstates)]
        (if (not= op self-op)
          ;; Helper (`self-op`) finished `op` which failed -> loop back to Step 1 on H(Acc) for `self-op`! (PLAN.md:519)
          (let [res (enter-step-1-at-account ps1 (-> msg
                                                     (dissoc :target :phase :curr-claim :won-person :won-claim :observed-eo)
                                                     (update :rounds inc)))]
            (update res :summary #(str "H(Acc=" account ") cleared failed helped op0=" op " and looped to Step 1: " %)))
          {:pstates ps1
           :plan-lines "PLAN.md:524,526,542"
           :summary (str "H(Acc=" account ") clears AccountLinking(op=" op ") if still op -> ack-return {:refused :not-invited}")
           :ack-return {:refused :not-invited}}))

      :enter/step-3a-enter-root
      (let [P (:won-person msg)
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
         :plan-lines "PLAN.md:544"
         :summary (str "H(P=" P ") Step 3a ensures RootEntered (fixed L'=" (:own-layer r') ", made?=" (:own-layer-made? r') ") -> hops |hash Acc")
         :next-msg (assoc msg
                          :fixed-layer (:own-layer r')
                          :fixed-making (:making r')
                          :present-flag (boolean (:own-layer-made? r'))
                          :step :enter/step-3b-link-account
                          :task (task-for :account account))})

      :enter/step-3b-link-account
      (let [{:keys [won-person fixed-layer fixed-making present-flag]} msg
            acc (get-in pstates [:accounts account])
            helping? (not= op self-op)
            ps1 (cond
                  (and (= :AccountLinking (:state acc)) (= (:op-id acc) op))
                  (assoc-in pstates [:accounts account]
                            {:state :AccountLinked
                             :person won-person
                             :own-layer fixed-layer
                             :making fixed-making
                             :says (if helping? (:says self-input) (:says target))
                             :says-at (if helping? (:at self-input) (:at target))})

                  (= :AccountLinked (:state acc))
                  (if helping?
                    (-> pstates
                        (assoc-in [:accounts account :says] (:says self-input))
                        (assoc-in [:accounts account :says-at] (:at self-input)))
                    pstates)

                  ;; AccountLinking was already cleared or replaced by another op!
                  (nil? acc)
                  (assoc-in pstates [:accounts account]
                            {:state :AccountLinked
                             :person won-person
                             :own-layer fixed-layer
                             :making fixed-making
                             :says (:says target)
                             :says-at (:at target)})

                  :else pstates)]
        {:pstates ps1
         :plan-lines "PLAN.md:545-546,519"
         :summary (str "H(Acc=" account ") Step 3b finalizes AccountLinked -> ack-return {:person " won-person " :own-layer " fixed-layer " :own-layer-made? " present-flag "}")
         :ack-return {:person won-person
                      :own-layer fixed-layer
                      :making fixed-making
                      :own-layer-made? present-flag}}))))

;; ---- 1C. answer-ask steps (PLAN.md lines 576-610)

(defn- answer-ask-step-1
  "Evaluates Step 1 of `answer-ask` on H(U) (PLAN.md lines 580-590)."
  [pstates {:keys [self-op self-input] :as msg}]
  (let [{:keys [code yes? root actor new-agent sign-in until at]} self-input
        k (get-in pstates [:asks code])]
    (cond
      (nil? k)
      {:pstates pstates
       :plan-lines "PLAN.md:581-582"
       :summary (str "H(U=" code ") sees nil -> hops |hash P=" root " for take-out then {:refused :no-such-ask}")
       :next-msg (assoc msg
                        :step :answer-ask/step-1b-takeout-and-return
                        :task (task-for :root root)
                        :takeout-root root :takeout-at at
                        :pending-result {:refused :no-such-ask})}

      (= :AskYes (:state k))
      (let [res {:yes {:root (:root k) :actor (:actor k) :sign-in (:sign-in k)}}]
        {:pstates pstates
         :plan-lines "PLAN.md:581-582"
         :summary (str "H(U=" code ") sees standing AskYes -> hops |hash P=" root " for take-out then " res)
         :next-msg (assoc msg
                          :step :answer-ask/step-1b-takeout-and-return
                          :task (task-for :root root)
                          :takeout-root root :takeout-at at
                          :pending-result res)})

      (= :AskNo (:state k))
      {:pstates pstates
       :plan-lines "PLAN.md:581-582"
       :summary (str "H(U=" code ") sees standing AskNo -> hops |hash P=" root " for take-out then {:no " code "}")
       :next-msg (assoc msg
                        :step :answer-ask/step-1b-takeout-and-return
                        :task (task-for :root root)
                        :takeout-root root :takeout-at at
                        :pending-result {:no code})}

      (= :AskWaiting (:state k))
      (if (or (:born-over? k) (>= at (:ends k)))
        {:pstates pstates
         :plan-lines "PLAN.md:584"
         :summary (str "H(U=" code ") sees AskWaiting but born-over? or at=" at " >= ends=" (:ends k) " -> hops |hash P=" root " for take-out then {:refused :over}")
         :next-msg (assoc msg
                          :step :answer-ask/step-1b-takeout-and-return
                          :task (task-for :root root)
                          :takeout-root root :takeout-at at
                          :pending-result {:refused :over})}
        (let [answering {:state :AskAnswering
                         :hash (:hash k) :asks (:asks k) :ends (:ends k) :born-over? (:born-over? k)
                         :op-id self-op :yes? yes? :root root :actor actor
                         :new-agent-label (:label new-agent) :sign-in sign-in
                         :until until :at at}]
          {:pstates (assoc-in pstates [:asks code] answering)
           :plan-lines "PLAN.md:585"
           :summary (str "H(U=" code ") writes AskAnswering(op=" self-op ", P=" root ") -> hops |hash P to Step 2")
           :next-msg (assoc msg
                            :target answering
                            :step :answer-ask/step-2-eval-on-root
                            :task (task-for :root root))}))

      (= :AskAnswering (:state k))
      {:pstates pstates
       :plan-lines (if (= (:op-id k) self-op) "PLAN.md:587" "PLAN.md:588")
       :summary (if (= (:op-id k) self-op)
                  (str "H(U=" code ") sees own AskAnswering(op=" self-op ") -> hops |hash P to Step 2")
                  (str "H(U=" code ") sees competing AskAnswering(op0=" (:op-id k) ", P0=" (:root k) ") -> hops |hash P0 to help op0"))
       :next-msg (assoc msg
                        :target k
                        :step :answer-ask/step-2-eval-on-root
                        :task (task-for :root (:root k)))})))

(defn- eval-answer-on-root
  "Evaluates Step 2 of `answer-ask` on H(P_target) (PLAN.md lines 591-605)."
  [pstates code target]
  (let [{:keys [op-id yes? root actor new-agent-label sign-in until at hash asks]} target
        ps0 (run-take-out-on-root pstates root at)
        existing-ok (get-in ps0 [:root-data root :ask-outcomes op-id])]
    (if existing-ok
      [ps0 existing-ok true]
      (let [r (get-in ps0 [:root-data root :r])]
        (cond
          (not= :RootEntered (:state r))
          (let [out {:outcome :AskOutcomeRefused :reason :no-such-actor}]
            [(assoc-in ps0 [:root-data root :ask-outcomes op-id] out) out false])

          (false? yes?)
          (let [out {:outcome :AskOutcomeNo :code code}]
            [(assoc-in ps0 [:root-data root :ask-outcomes op-id] out) out false])

          ;; yes? == true
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
                    out {:outcome :AskOutcomeYes :root root :actor actor :sign-in sign-in}
                    ps1 (-> ps0
                            (assoc-in [:root-data root :r :next-sign-in-seq] (inc g-seq))
                            (assoc-in [:root-data root :sign-ins sign-in]
                                      {:sign-in sign-in :actor actor :kind :program
                                       :initial-hash hash :hash hash :until until
                                       :prev-hash nil :prev-until nil
                                       :ended? born-ended? :begun at :label asks :seq g-seq})
                            (assoc-in [:root-data root :ask-outcomes op-id] out))
                    ps2 (if born-ended?
                          ps1
                          (-> ps1
                              (assoc-in [:live root :sign-ins sign-in]
                                        {:sign-in sign-in :actor actor :kind :program :hash hash
                                         :until until :prev-hash nil :prev-until nil
                                         :begun at :label asks :seq g-seq})
                              (update-in [:root-data root :actor-sign-ins actor] (fnil conj #{}) sign-in)
                              (assoc-in [:root-data root :expiries [until :sign-in sign-in]]
                                        {:kind :sign-in :id sign-in :parent-id actor :until until})))]
                [ps2 out false])))

          ;; yes? == true with new-agent-label != nil (PLAN.md:599-604)
          :else
          (let [a-exists? (or (= actor root) (some? (get-in ps0 [:root-data root :actors actor])))
                g-exists? (some? (get-in ps0 [:root-data root :sign-ins sign-in]))]
            (if (or a-exists? g-exists?)
              (let [out {:outcome :AskOutcomeRefused :reason :in-use}]
                [(assoc-in ps0 [:root-data root :ask-outcomes op-id] out) out false])
              (let [a-seq (:next-actor-seq r 0)
                    g-seq (:next-sign-in-seq r 0)
                    born-ended? (<= until at)
                    out {:outcome :AskOutcomeYes :root root :actor actor :sign-in sign-in}
                    ps1 (-> ps0
                            (assoc-in [:root-data root :r :next-actor-seq] (inc a-seq))
                            (assoc-in [:root-data root :r :next-sign-in-seq] (inc g-seq))
                            (assoc-in [:root-data root :actors actor]
                                      {:actor actor :made-by root :depth 1 :label new-agent-label :ended? false :seq a-seq})
                            (assoc-in [:root-data root :actor-order a-seq]
                                      {:seq a-seq :actor actor :made-by root :label new-agent-label :ended? false})
                            (update-in [:root-data root :children root] (fnil conj #{}) actor)
                            (assoc-in [:root-data root :sign-ins sign-in]
                                      {:sign-in sign-in :actor actor :kind :program
                                       :initial-hash hash :hash hash :until until
                                       :prev-hash nil :prev-until nil
                                       :ended? born-ended? :begun at :label asks :seq g-seq})
                            (assoc-in [:root-data root :ask-outcomes op-id] out))
                    ps2 (if born-ended?
                          ps1
                          (-> ps1
                              (assoc-in [:live root :sign-ins sign-in]
                                        {:sign-in sign-in :actor actor :kind :program :hash hash
                                         :until until :prev-hash nil :prev-until nil
                                         :begun at :label asks :seq g-seq})
                              (update-in [:root-data root :actor-sign-ins actor] (fnil conj #{}) sign-in)
                              (assoc-in [:root-data root :expiries [until :sign-in sign-in]]
                                        {:kind :sign-in :id sign-in :parent-id actor :until until})))]
                [ps2 out false]))))))))

(defn- exec-answer-ask-step [pstates {:keys [step self-op self-input target] :as msg}]
  (let [code (:code self-input)]
    (case step
      :answer-ask/step-1-check-ask
      (answer-ask-step-1 pstates msg)

      :answer-ask/step-1b-takeout-and-return
      (let [{:keys [takeout-root takeout-at pending-result]} msg
            ps1 (run-take-out-on-root pstates takeout-root takeout-at)]
        {:pstates ps1
         :plan-lines "PLAN.md:581-584,589"
         :summary (str "H(P=" takeout-root ") runs take-out at " takeout-at " -> ack-return " pending-result)
         :ack-return pending-result})

      :answer-ask/step-2-eval-on-root
      (let [[ps1 out cached?] (eval-answer-on-root pstates code target)]
        {:pstates ps1
         :plan-lines (if cached? "PLAN.md:592-593" "PLAN.md:592-604")
         :summary (str "H(P=" (:root target) ") take-out at " (:at target)
                       (if cached? " + replays cached ok:" " + records ok:")
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
            (if (and helping? (not= (:root self-input) (:root target)))
              {:pstates ps1
               :plan-lines "PLAN.md:607,589"
               :summary (str "H(U=" code ") commits AskYes; helper P=" (:root self-input) " != P0=" (:root target) " -> hops |hash P for take-out")
               :next-msg (assoc msg
                                :step :answer-ask/step-1b-takeout-and-return
                                :task (task-for :root (:root self-input))
                                :takeout-root (:root self-input)
                                :takeout-at (:at self-input)
                                :pending-result res)}
              {:pstates ps1
               :plan-lines (if helping? "PLAN.md:607,589" "PLAN.md:607")
               :summary (str "H(U=" code ") commits AskYes"
                             (when helping? (str " (helper P==P0=" (:root self-input) ", skips hop per PLAN.md:589)"))
                             " -> ack-return " res)
               :ack-return res}))

          :AskOutcomeNo
          (let [res {:no code}
                ps1 (if still-op?
                      (assoc-in pstates [:asks code]
                                {:state :AskNo :hash (:hash k) :asks (:asks k) :ends (:ends k)})
                      pstates)]
            (if (and helping? (not= (:root self-input) (:root target)))
              {:pstates ps1
               :plan-lines "PLAN.md:608,589"
               :summary (str "H(U=" code ") commits AskNo; helper P=" (:root self-input) " != P0=" (:root target) " -> hops |hash P for take-out")
               :next-msg (assoc msg
                                :step :answer-ask/step-1b-takeout-and-return
                                :task (task-for :root (:root self-input))
                                :takeout-root (:root self-input)
                                :takeout-at (:at self-input)
                                :pending-result res)}
              {:pstates ps1
               :plan-lines (if helping? "PLAN.md:608,589" "PLAN.md:608")
               :summary (str "H(U=" code ") commits AskNo"
                             (when helping? (str " (helper P==P0=" (:root self-input) ", skips hop per PLAN.md:589)"))
                             " -> ack-return " res)
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
               :plan-lines "PLAN.md:609-610"
               :summary (str "H(U=" code ") restores AskWaiting (if still op) -> ack-return {:refused " (:reason out) "}")
               :ack-return {:refused (:reason out)}})))))))

;; ---- Single-task companion writes (`own-layer-made`, `end-actor`)

(defn- exec-own-layer-made [pstates {:keys [input]}]
  (let [{:keys [person]} input
        r (get-in pstates [:root-data person :r])]
    (if (= :RootEntered (:state r))
      {:pstates (assoc-in pstates [:root-data person :r :own-layer-made?] true)
       :plan-lines "PLAN.md:39,804"
       :summary (str "H(P=" person ") sets :own-layer-made? true -> ack-return {:made " person "}")
       :ack-return {:made person}}
      {:pstates pstates
       :plan-lines "PLAN.md:39,804"
       :summary (str "H(P=" person ") r not RootEntered -> ack-return {:refused :no-such-person}")
       :ack-return {:refused :no-such-person}})))

(defn- exec-end-actor [pstates {:keys [input]}]
  (let [{:keys [root actor at]} input
        ps0 (run-take-out-on-root pstates root at)
        r (get-in ps0 [:root-data root :r])
        a (get-in ps0 [:root-data root :actors actor])]
    (cond
      (and a (:ended? a))
      {:pstates ps0
       :plan-lines "PLAN.md:44,809"
       :summary (str "H(P=" root ") sees actor " actor " already ended -> ack-return {:ended " actor "}")
       :ack-return {:ended actor}}

      (or (not= :RootEntered (:state r)) (and (not= actor root) (nil? a)))
      {:pstates ps0
       :plan-lines "PLAN.md:44,809"
       :summary (str "H(P=" root ") actor " actor " absent -> ack-return {:refused :no-such-actor}")
       :ack-return {:refused :no-such-actor}}

      (= actor root)
      {:pstates ps0
       :plan-lines "PLAN.md:44,809"
       :summary (str "H(P=" root ") actor==P -> ack-return {:refused :is-a-person}")
       :ack-return {:refused :is-a-person}}

      :else
      (let [ps-end
            (loop [q (conj clojure.lang.PersistentQueue/EMPTY actor)
                   ps ps0]
              (if (empty? q)
                ps
                (let [x (peek q)
                      q' (pop q)
                      ax (get-in ps [:root-data root :actors x])]
                  (if (:ended? ax)
                    (recur q' ps)
                    (let [live-gs (get-in ps [:root-data root :actor-sign-ins x] #{})
                          children (get-in ps [:root-data root :children x] #{})
                          ps1 (-> ps
                                  (assoc-in [:root-data root :actors x :ended?] true)
                                  (assoc-in [:root-data root :actor-order (:seq ax) :ended?] true))
                          ps2 (reduce (fn [acc G] (end-sign-in-on-root acc root G)) ps1 live-gs)]
                      (recur (into q' children) ps2))))))]
        {:pstates ps-end
         :plan-lines "PLAN.md:658-668"
         :summary (str "H(P=" root ") BFS end-actor cascade under " actor " -> ack-return {:ended " actor "}")
         :ack-return {:ended actor}}))))

(defn execute-step
  "Executes one atomic single-task step of `PLAN.md`."
  [pstates msg]
  (case (:op msg)
    :invite (exec-invite-step pstates msg)
    :enter (exec-enter-step pstates msg)
    :answer-ask (exec-answer-ask-step pstates msg)
    :own-layer-made (exec-own-layer-made pstates msg)
    :end-actor (exec-end-actor pstates msg)
    :make-actor (let [[ps1 res] (seed-make-actor pstates (:input msg))]
                  {:pstates ps1
                   :plan-lines "PLAN.md:43,808"
                   :summary (str "H(P=" (get-in msg [:input :root]) ") make-actor " (get-in msg [:input :actor]) " -> ack-return " res)
                   :ack-return res})))
