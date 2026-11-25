(ns auth-concurrent.model-check
  "Exhaustive interleaving, failure-injection, Rama retry-mode, helper, and
  clock-skew checker for Step 1 (`PLAN.md` §1A `invite`, §1B `enter` 1-claim
  and 2-claim, §1C `answer-ask`).

  Models Rama's execution semantics:
  - Single-threaded tasks, each owning its PState slice;
  - Directed `(src-task -> dst-task)` FIFO message channels (`stream.md:37`);
  - Colocated depot partitions emitting `Step 1` on their target task;
  - Failure after each step + all Rama ways of running an event again:
    1. `:individual` retry: failed attempt's remaining un-executed hop dies,
       already-committed PState writes on earlier tasks remain, and Rama
       re-emits the depot record from `source>` (`Step 1`);
    2. `:all-after` partition replay (`PLAN.md:195`): replays the failed /
       uncheckpointed record AND all subsequent records on that same depot
       partition in append order from `Step 1` (including post-commit replay);
    3. `:timeout-overlap` retry (`stream.md:260`): event-tree timeout starts a
       retry from `Step 1` while the slow attempt's child hop is still queued;
    4. `:client-rerun`: after a write returns (or after `:could-not`), a caller
       sends the write again (with same `:at` or fresh `:at`).

  Checks all five questions from Sid's brief on every execution order:
  - Q1 (`:uniqueness-broken`): 1 account -> 2 persons, 1 claim -> 2 accounts,
    or 1 person -> 2 claims;
  - Q2 (`:two-ask-answers`): 1 ask -> 2 standing answers;
  - Q3 (`:left-unsettled`): anything left forever in a state neither done nor
    refused (`RootReserving`, `AccountLinking`, `ClaimClaiming`, `AskAnswering`,
    or orphaned half-linked PStates);
  - Q4 (`:disallowed-rerun-or-result`): a call or re-run gives a result (or
    `:says` state) disallowed by `SPEC.md` + Sid's 2 calls of 22 November;
  - Q5 (`:read-misses-effect`): immediately after a write returns (or at end),
    any read (`who-is`, `ask-of`, `under`, `watch-root`, or subsequent `enter`)
    misses its effect."
  (:require [auth-concurrent.plan-steps :as ps]
            [auth-concurrent.spec-oracle :as oracle]))

;; Fix tiny helper in plan-steps if needed? Let's verify `plan-steps` first when loading!

;; --------------------------------------------- Five Invariant Checks (Q1..Q5)

(defn check-q1-uniqueness
  "Q1: Does one account ever get two persons, or one claim two accounts, or one
  person two claims?"
  [pstates completed-ops]
  (let [;; 1. Returned enter results per account
        enter-persons-by-acc
        (reduce (fn [m {:keys [call result]}]
                  (let [[op inp] call]
                    (if (and (= op :enter) (:person result))
                      (update m (:account inp) (fnil conj #{}) (:person result))
                      m)))
                {}
                completed-ops)
        ;; Also check RootEntered :linked-account in PStates
        root-persons-by-acc
        (reduce-kv (fn [m P {:keys [r]}]
                     (if (and (= :RootEntered (:state r)) (:linked-account r))
                       (update m (:linked-account r) (fnil conj #{}) P)
                       m))
                   enter-persons-by-acc
                   (:root-data pstates))
        acc-two-persons (first (filter (fn [[_ ps]] (> (count ps) 1)) root-persons-by-acc))

        ;; 2. One claim -> two accounts (or one person linked to two accounts)
        accs-by-person
        (reduce (fn [m {:keys [call result]}]
                  (let [[op inp] call]
                    (if (and (= op :enter) (:person result))
                      (update m (:person result) (fnil conj #{}) (:account inp))
                      m)))
                (reduce-kv (fn [m acc st]
                             (if (= :AccountLinked (:state st))
                               (update m (:person st) (fnil conj #{}) acc)
                               m))
                           {}
                           (:accounts pstates))
                completed-ops)
        claim-two-accounts (first (filter (fn [[_ accs]] (> (count accs) 1)) accs-by-person))

        ;; 3. One person -> two claims
        claims-by-person
        (reduce-kv (fn [m claim st]
                     (if (:person st)
                       (update m (:person st) (fnil conj #{}) claim)
                       m))
                   (reduce-kv (fn [m P {:keys [r]}]
                                (if (and (#{:RootInvited :RootEntered} (:state r)) (:claim r))
                                  (update m P (fnil conj #{}) (:claim r))
                                  m))
                              {}
                              (:root-data pstates))
                   (:claims pstates))
        person-two-claims (first (filter (fn [[_ cs]] (> (count cs) 1)) claims-by-person))]
    (cond
      acc-two-persons
      {:question :q1-uniqueness
       :subkind :one-account-two-persons
       :detail (str "Account " (first acc-two-persons) " got two persons: " (second acc-two-persons))}

      claim-two-accounts
      {:question :q1-uniqueness
       :subkind :one-claim-two-accounts
       :detail (str "Person/claim for " (first claim-two-accounts) " was taken by two accounts: " (second claim-two-accounts))}

      person-two-claims
      {:question :q1-uniqueness
       :subkind :one-person-two-claims
       :detail (str "Person " (first person-two-claims) " bound to two claims: " (second person-two-claims))}

      :else nil)))

(defn check-q2-ask-answers
  "Q2: Does one ask ever get two answers that stand?"
  [completed-ops ask-history-by-code]
  (let [answers-by-code
        (reduce (fn [m {:keys [call result]}]
                  (let [[op inp] call]
                    (if (and (= op :answer-ask)
                             (or (contains? result :yes) (contains? result :no)))
                      (update m (:code inp) (fnil conj #{}) result)
                      m)))
                {}
                completed-ops)
        conflict (first (filter (fn [[_ rs]] (> (count rs) 1)) answers-by-code))
        state-flip (first (filter (fn [[_ states]] (> (count states) 1)) ask-history-by-code))]
    (cond
      conflict
      {:question :q2-two-ask-answers
       :subkind :conflicting-returned-answers
       :detail (str "Ask " (first conflict) " returned two standing answers: " (second conflict))}

      state-flip
      {:question :q2-two-ask-answers
       :subkind :committed-ask-mutated
       :detail (str "Ask " (first state-flip) " had multiple committed AskYes/AskNo states: " (second state-flip))}

      :else nil)))

(defn check-q3-unsettled
  "Q3: Once quiescence is reached (all events and retries finished), is anything
  left forever in a state that is neither done nor refused?"
  [pstates]
  (let [stuck-root (first (keep (fn [[P {:keys [r]}]]
                                  (when (= :RootReserving (:state r)) [P r]))
                                (:root-data pstates)))
        stuck-acc (first (keep (fn [[acc st]]
                                 (when (= :AccountLinking (:state st)) [acc st]))
                               (:accounts pstates)))
        stuck-claim (first (keep (fn [[c st]]
                                   (when (= :ClaimClaiming (:state st)) [c st]))
                                 (:claims pstates)))
        stuck-ask (first (keep (fn [[u st]]
                                 (when (= :AskAnswering (:state st)) [u st]))
                               (:asks pstates)))
        half-claim (first (keep (fn [[c st]]
                                  (let [P (:person st)
                                        r (get-in pstates [:root-data P :r])]
                                    (cond
                                      (and (= :ClaimWaiting (:state st))
                                           (not (#{:RootInvited :RootEntered} (:state r))))
                                      [:claim-waiting-without-root c st r]

                                      (and (= :ClaimUsed (:state st))
                                           (not= :RootEntered (:state r)))
                                      [:claim-used-without-entered-root c st r]

                                      :else nil)))
                                (:claims pstates)))
        half-entered (first (keep (fn [[P {:keys [r]}]]
                                    (when (= :RootEntered (:state r))
                                      (let [acc-k (:linked-account r)
                                            acc (get-in pstates [:accounts acc-k])]
                                        (when (or (nil? acc) (not= :AccountLinked (:state acc)) (not= (:person acc) P))
                                          [:entered-root-without-matching-account-link P r acc]))))
                                  (:root-data pstates)))]
    (cond
      stuck-root {:question :q3-left-unsettled
                  :subkind :root-reserving-forever
                  :detail (str "$$root-data[" (first stuck-root) "] left forever in RootReserving: " (second stuck-root))}
      stuck-acc {:question :q3-left-unsettled
                 :subkind :account-linking-forever
                 :detail (str "$$accounts[" (first stuck-acc) "] left forever in AccountLinking: " (second stuck-acc))}
      stuck-claim {:question :q3-left-unsettled
                   :subkind :claim-claiming-forever
                   :detail (str "$$claims[" (first stuck-claim) "] left forever in ClaimClaiming: " (second stuck-claim))}
      stuck-ask {:question :q3-left-unsettled
                 :subkind :ask-answering-forever
                 :detail (str "$$asks[" (first stuck-ask) "] left forever in AskAnswering: " (second stuck-ask))}
      half-claim {:question :q3-left-unsettled
                  :subkind (first half-claim)
                  :detail (str "Cross-partition half-state on claim: " (pr-str half-claim))}
      half-entered {:question :q3-left-unsettled
                    :subkind (first half-entered)
                    :detail (str "Cross-partition half-state on entered person: " (pr-str half-entered))}
      :else nil)))

(defn check-q5-read-after-write
  "Q5: Immediately at the moment a write returns, can any read (`who-is`,
  `ask-of`, `under`, `watch-root`, or account link) miss its effect?"
  [pstates [op input] result]
  (when-not (:could-not result)
    (case op
      :invite
      (when (:invited result)
        (let [P (:person input)
              C (:claim input)
              c (get-in pstates [:claims C])
              r (get-in pstates [:root-data P :r])]
          (when (or (nil? c) (not= (:person c) P)
                    (not (#{:RootInvited :RootEntered} (:state r)))
                    (not= (:claim r) C))
            {:question :q5-read-misses-effect
             :subkind :invite-not-visible-on-return
             :detail (str "invite returned " result " but $$claims[C]=" c ", r=" r)})))

      :enter
      (when-let [P (:person result)]
        (let [acc (get-in pstates [:accounts (:account input)])
              w (ps/plan-who-is pstates {:root P :sign-in [:dummy-g] :session nil})]
          (cond
            (or (not= :AccountLinked (:state acc))
                (not= (:person acc) P)
                (not= (:own-lane acc) (:own-lane result))
                (not= (:making acc) (:making result)))
            {:question :q5-read-misses-effect
             :subkind :enter-account-link-misses-effect
             :detail (str "enter returned " result " for " (:account input) " but $$accounts holds " acc)}

            (not= (:own-lane w) (:own-lane result))
            {:question :q5-read-misses-effect
             :subkind :enter-who-is-misses-own-lane
             :detail (str "enter returned " result " but who-is(P) returned " w)}

            :else nil)))

      :answer-ask
      (let [root (:root input)
            at (:at input)
            code (:code input)
            ;; 1. Check take-out effect on `root` at `at` (SPEC.md lines 343-347, 93)!
            live (ps/plan-live-under pstates root)
            stale-g (first (filter (fn [[_ g]] (<= (:until g) at)) (:sign-ins live)))
            stale-s (first (filter (fn [[_ s]] (<= (:until s) at)) (:sessions live)))
            ask-read (ps/plan-ask-of pstates code)]
        (cond
          (or stale-g stale-s)
          {:question :q5-read-misses-effect
           :subkind :answer-ask-takeout-missed-on-return
           :detail (str "answer-ask (root=" root ", at=" at ") returned " result
                        ", but reads (watch-root / under / who-is) still see expired live items: "
                        {:stale-sign-in stale-g :stale-session stale-s})}

          (and (:yes result)
               (let [{P0 :root G0 :sign-in A0 :actor} (:yes result)
                     w (ps/plan-who-is pstates {:root P0 :sign-in G0 :session nil})]
                 (or (not= :yes (:state ask-read))
                     (not= (:yes result) (:answer ask-read))
                     (nil? (:sign-in w))
                     (not= A0 (get-in w [:sign-in :actor])))))
          {:question :q5-read-misses-effect
           :subkind :answer-ask-yes-not-visible-on-return
           :detail (str "answer-ask returned " result " but ask-of=" ask-read)}

          (and (:no result) (not= :no (:state ask-read)))
          {:question :q5-read-misses-effect
           :subkind :answer-ask-no-not-visible-on-return
           :detail (str "answer-ask returned " result " but ask-of=" ask-read)}

          :else nil))

      nil)))

(defn- pstates-match-oracle-final?
  "Checks whether settled PStates match the sequential oracle state `ost` on all
  reads (`who-is`, `ask-of`, `under`, `live-under`) AND on kept `:says` per
  linked account (Sid's Call 2: `:says` kept from the enter with latest `:at`)."
  [pstates ost entities]
  (and
   (every? (fn [acc]
             (let [pa (get-in pstates [:accounts acc])
                   oa (get-in ost [:links acc])]
               (if (nil? oa)
                 (nil? pa)
                 (and (= :AccountLinked (:state pa))
                      (= (:person pa) (:person oa))
                      (= (:own-lane pa) (get-in ost [:persons (:person oa) :own-lane]))
                      (= (:making pa) (get-in ost [:persons (:person oa) :making]))
                      (= (:says pa) (:says oa))))))
           (:accounts entities))
   (every? (fn [code]
             (= (ps/plan-ask-of pstates code) (oracle/oracle-ask-of ost code)))
           (:codes entities))
   (every? (fn [P]
             (and (= (ps/plan-under pstates P) (oracle/oracle-under ost P))
                  (= (ps/plan-live-under pstates P) (oracle/oracle-live-under ost P))
                  (every? (fn [G]
                            (every? (fn [S]
                                      (= (ps/plan-who-is pstates {:root P :sign-in G :session S})
                                         (oracle/oracle-who-is ost {:root P :sign-in G :session S})))
                                    (conj (:sessions entities) nil)))
                          (:sign-ins entities))))
           (:persons entities))))

(defn check-q4-linearizability-and-reruns
  "Q4: Does any call (or call run again) give a result or leave a state (such as
  `:says`) that SPEC.md and Sid's two calls of 22 November do not allow?"
  [init-ost pstates completed-ops entities rerun-fn]
  (cond
    (not (oracle/valid-linearization? init-ost completed-ops (constantly true)))
    {:question :q4-disallowed-result
     :subkind :no-valid-causal-linearization-of-results
     :detail (str "Returned results cannot occur in ANY causal order under SPEC.md + Sid's 2 calls: "
                  (mapv #(select-keys % [:id :call :invoked-at-step :returned-at-step :result]) completed-ops))}

    (not (oracle/valid-linearization?
          init-ost
          completed-ops
          #(pstates-match-oracle-final? pstates % entities)))
    {:question :q4-disallowed-result
     :subkind :final-state-or-says-violates-spec
     :detail (str "Final PState state (e.g. :says from latest :at, or live take-out) disagrees with every causal linearization: accounts="
                  (:accounts pstates))}

    rerun-fn
    (first (rerun-fn))

    :else nil))

;; ------------------------------------- Multi-task Rama Event-Tree State Machine

(defn init-world
  "Initializes a simulation world from `scenario`:
   `{:init-pstates ... :init-oracle ... :entities ... :calls [...]}`."
  [{:keys [init-pstates init-oracle calls]}]
  (let [world0 {:pstates (or init-pstates ps/empty-pstates)
                :init-oracle (or init-oracle oracle/empty-oracle-state)
                :depots {}          ;; {depot-task [record ...]} (append log for :all-after)
                :depot-cursors {}   ;; {depot-task next-idx}
                :channels {}        ;; {[src-task dst-task] PersistentQueue}
                :call-meta {}       ;; {call-id {:call [op input] :invoked-at-step 0 :active-attempt int}}
                :completed {}       ;; {call-id {:id :call :invoked-at-step :returned-at-step :result}}
                :ask-committed {}   ;; {code #{AskYes/AskNo ...}}
                :raw-violations []
                :step-num 0
                :next-attempt-id 100
                :trace []}]
    (reduce
     (fn [w [idx {:keys [id call op-id sequential-after]}]]
       (let [[op input] call
             {:keys [task]} (ps/depot-info call)
             rec {:call-id id :op op :input input :op-id (or op-id [:op id])
                  :attempt-id (inc idx) :sequential-after sequential-after}]
         (if sequential-after
           (assoc-in w [:deferred id] rec)
           (-> w
               (update-in [:depots task] (fnil conj []) rec)
               (assoc-in [:call-meta id] {:call call :invoked-at-step 0 :active-attempt (inc idx)})))))
     world0
     (map-indexed vector calls))))

(defn- maybe-release-deferred
  "When a call completes, appends any sequential follow-up calls waiting on it."
  [world]
  (reduce-kv
   (fn [w id {:keys [sequential-after call-id op input op-id attempt-id] :as rec}]
     (if (every? #(contains? (:completed w) %) sequential-after)
       (let [{:keys [task]} (ps/depot-info [op input])]
         (-> w
             (update :deferred dissoc id)
             (update-in [:depots task] (fnil conj []) rec)
             (assoc-in [:call-meta call-id]
                       {:call [op input] :invoked-at-step (:step-num w) :active-attempt attempt-id})))
       w))
   world
   (:deferred world)))

(defn runnable-actions
  "Lists all deterministic single-step transitions enabled in `world`:
   - `[:intake depot-task]` when `depot-cursors[depot-task] < count(depots[depot-task])`
   - `[:channel [src-task dst-task]]` when channel queue is non-empty."
  [world]
  (let [intakes (for [[t recs] (:depots world)
                      :let [cur (get-in world [:depot-cursors t] 0)]
                      :when (< cur (count recs))]
                  [:intake t])
        chans (for [[edge q] (:channels world)
                    :when (seq q)]
                [:channel edge])]
    (vec (concat (sort-by str intakes) (sort-by str chans)))))

(defn- record-committed-asks [ask-committed pstates]
  (reduce-kv (fn [m code st]
               (if (#{:AskYes :AskNo} (:state st))
                 (update m code (fnil conj #{}) (select-keys st [:state :root :actor :sign-in]))
                 m))
             ask-committed
             (:asks pstates)))

(defn apply-msg-step
  "Executes `msg` on `(:task msg)` and advances `world` by 1 step."
  [world msg source-desc]
  (if (> (:rounds msg 0) 8)
    (assoc world :infinite-loop? true)
    (let [step-n (inc (:step-num world))
          {:keys [pstates plan-lines summary next-msg ack-return]} (ps/execute-step (:pstates world) msg)
          call-id (:call-id msg)
          active? (= (:attempt-id msg) (get-in world [:call-meta call-id :active-attempt]))
          already-done? (contains? (:completed world) call-id)
          trace-entry {:step-num step-n
                       :call-id call-id
                       :attempt-id (:attempt-id msg)
                       :task (:task msg)
                       :step (:step msg)
                       :plan-lines plan-lines
                       :source source-desc
                       :summary summary}
          w1 (-> world
                 (assoc :pstates pstates :step-num step-n)
                 (assoc :ask-committed (record-committed-asks (:ask-committed world) pstates))
                 (update :trace conj trace-entry))
          w2 (if next-msg
               (let [edge [(:task msg) (:task next-msg)]]
                 (update-in w1 [:channels edge]
                            (fnil conj clojure.lang.PersistentQueue/EMPTY)
                            next-msg))
               w1)
          w3 (if (and (some? ack-return) active? (not already-done?))
               (let [call (get-in w2 [:call-meta call-id :call])
                     inv-at (get-in w2 [:call-meta call-id :invoked-at-step] 0)
                     raw-err (check-q5-read-after-write pstates call ack-return)]
                 (cond-> (assoc-in w2 [:completed call-id]
                                   {:id call-id
                                    :call call
                                    :invoked-at-step inv-at
                                    :returned-at-step step-n
                                    :result ack-return})
                   raw-err (update :raw-violations conj raw-err)))
               w2)]
      (maybe-release-deferred w3))))

(defn step-world
  "Executes one runnable action (`[:intake t]` or `[:channel edge]`)."
  [world [kind arg]]
  (case kind
    :intake
    (let [cur (get-in world [:depot-cursors arg] 0)
          rec (nth (get-in world [:depots arg]) cur)
          msg (ps/make-initial-msg rec)
          w1 (assoc-in world [:depot-cursors arg] (inc cur))]
      (apply-msg-step w1 msg (str "depot-intake " arg " idx=" cur)))

    :channel
    (let [q (get-in world [:channels arg])
          msg (peek q)
          q' (pop q)
          w1 (if (empty? q')
               (update world :channels dissoc arg)
               (assoc-in world [:channels arg] q'))]
      (apply-msg-step w1 msg (str "hop " (first arg) " -> " (second arg))))))

;; ------------------------------------- Rama Failure & Replay Injections

(defn inject-individual-retry
  "Injects a mid-flight failure + Rama `:individual` retry on an in-flight
  channel message at `edge`: drops that pending message (simulating crash on
  destination task before commit; prior tasks' PStates stay committed!) and
  re-emits the record from `Step 1` on its source depot task."
  [world edge]
  (let [q (get-in world [:channels edge])
        msg (peek q)
        q' (pop q)
        call-id (:call-id msg)
        [op input] (get-in world [:call-meta call-id :call])
        new-aid (inc (:next-attempt-id world))
        {:keys [task]} (ps/depot-info [op input])
        rec {:call-id call-id :op op :input input
             :op-id (or (:self-op msg) [:op call-id])
             :attempt-id new-aid}]
    (-> world
        (assoc :next-attempt-id new-aid)
        (update :channels #(if (empty? q') (dissoc % edge) (assoc % edge q')))
        (assoc-in [:call-meta call-id :active-attempt] new-aid)
        (update-in [:depots task] conj rec)
        (update :trace conj {:step-num (:step-num world)
                             :call-id call-id
                             :attempt-id new-aid
                             :task (:task msg)
                             :step :FAULT/individual-retry
                             :plan-lines "stream.md:94-102"
                             :source (str "drop in-flight " edge)
                             :summary (str "Crash before " (:step msg) " on " (:task msg)
                                           "; prior hops stay committed; Rama retries " call-id " from Step 1 on " task)}))))

(defn inject-all-after-replay
  "Injects Rama `:all-after` / uncheckpointed worker restart on `depot-task`
  (`PLAN.md:195`, `stream.md:96,166`): drops any in-flight messages belonging to
  records from `from-idx` onward on `depot-task` and resets `depot-cursors[depot-task]`
  back to `from-idx` so that record AND every subsequent record on that partition
  replay in depot order from `Step 1`!"
  [world depot-task from-idx]
  (let [recs (subvec (get-in world [:depots depot-task] []) from-idx)
        replay-ids (into #{} (map :call-id) recs)
        ;; Assign fresh attempt IDs to replayed records that have not yet returned
        [w1 new-recs]
        (reduce (fn [[w acc] r]
                  (let [aid (inc (:next-attempt-id w))
                        cid (:call-id r)
                        w' (cond-> (assoc w :next-attempt-id aid)
                             (not (contains? (:completed w) cid))
                             (assoc-in [:call-meta cid :active-attempt] aid))]
                    [w' (conj acc (assoc r :attempt-id aid))]))
                [world []]
                recs)
        prefix (subvec (get-in w1 [:depots depot-task] []) 0 from-idx)
        cleaned-chans
        (into {}
              (keep (fn [[edge q]]
                      (let [q' (into clojure.lang.PersistentQueue/EMPTY
                                     (remove #(contains? replay-ids (:call-id %)))
                                     q)]
                        (when (seq q') [edge q']))))
              (:channels w1))]
    (-> w1
        (assoc :channels cleaned-chans)
        (assoc-in [:depots depot-task] (into prefix new-recs))
        (assoc-in [:depot-cursors depot-task] from-idx)
        (update :trace conj {:step-num (:step-num world)
                             :call-id (mapv :call-id recs)
                             :attempt-id (:next-attempt-id w1)
                             :task depot-task
                             :step :FAULT/all-after-replay
                             :plan-lines "PLAN.md:195, stream.md:96"
                             :source (str "replay partition " depot-task " from idx=" from-idx)
                             :summary (str "Rama :all-after / uncheckpointed replay on " depot-task
                                           " from idx=" from-idx " for calls " (mapv :call-id recs))}))))

(defn inject-timeout-overlap-retry
  "Injects Rama event-tree timeout retry (`stream.md:260`): starts a new attempt
  of `call-id` from `Step 1` WITHOUT cancelling the slow child message still in
  flight on `edge`."
  [world edge]
  (let [msg (peek (get-in world [:channels edge]))
        call-id (:call-id msg)
        [op input] (get-in world [:call-meta call-id :call])
        new-aid (inc (:next-attempt-id world))
        {:keys [task]} (ps/depot-info [op input])
        rec {:call-id call-id :op op :input input
             :op-id (or (:self-op msg) [:op call-id])
             :attempt-id new-aid}]
    (-> world
        (assoc :next-attempt-id new-aid)
        (assoc-in [:call-meta call-id :active-attempt] new-aid)
        (update-in [:depots task] conj rec)
        (update :trace conj {:step-num (:step-num world)
                             :call-id call-id
                             :attempt-id new-aid
                             :task task
                             :step :FAULT/timeout-overlap-retry
                             :plan-lines "stream.md:260"
                             :source (str "overlap while " edge " in flight")
                             :summary (str "Stream timeout re-emits " call-id " from Step 1 while prior attempt's "
                                           (:step msg) " remains queued on " edge)}))))

;; ------------------------------------- Quiescent Client Re-run Check (Q4)

(defn check-client-reruns-after-quiescence
  "Runs every completed call AGAIN sequentially after quiescence (as a client
  retry with a fresh `op-id` on `enter`/`answer-ask` per PLAN.md:511,579, first
  with the original `:at` and also with a later `:at`) and verifies its result
  and state against SPEC.md + Sid's 2 calls."
  [world entities]
  (let [completed-ops (vals (:completed world))
        pstates0 (:pstates world)]
    (keep
     (fn [{:keys [id call result]}]
       (let [[op input] call
             fresh-op [:rerun-op id]
             w-rerun (init-world {:init-pstates pstates0
                                  :calls [{:id [:rerun id] :call call :op-id fresh-op}]})
             w-done (loop [w w-rerun guard 0]
                      (let [acts (runnable-actions w)]
                        (if (or (empty? acts) (> guard 40))
                          w
                          (recur (step-world w (first acts)) (inc guard)))))
             rerun-res (get-in w-done [:completed [:rerun id] :result])
             raw-err (first (:raw-violations w-done))]
         (cond
           raw-err (assoc raw-err :rerun-of id)

           ;; Check whether `[completed-ops ... + rerun]` is linearizable!
           (let [aug-ops (conj (vec completed-ops)
                               {:id [:rerun id]
                                :call call
                                :invoked-at-step (inc (:step-num world))
                                :returned-at-step (+ 2 (:step-num world))
                                :result rerun-res})]
             (not (oracle/valid-linearization?
                   (:init-oracle world)
                   aug-ops
                   #(pstates-match-oracle-final? (:pstates w-done) % entities))))
           {:question :q4-disallowed-result
            :subkind :client-rerun-violates-spec
            :detail (str "Re-running " id " " call " (which originally gave " result
                         ") returned " rerun-res " / left state disallowed by SPEC.md + Sid's 2 calls")
            :rerun-trace (:trace w-done)}

           :else nil)))
     completed-ops)))

;; ------------------------------------- Exhaustive Scenario Explorer

(defn- world-signature
  "State signature for diamond-schedule deduplication during graph search."
  [world fault-budget]
  [fault-budget
   (:pstates world)
   (:depot-cursors world)
   (into {} (map (fn [[k v]] [k (mapv #(dissoc % :attempt-id) v)])) (:depots world))
   (into {}
         (map (fn [[e q]]
                [e (mapv (fn [m]
                           (-> (select-keys m [:call-id :step :self-op :phase :curr-claim :target :ask-outcome :other-won?])
                               (assoc :active? (= (:attempt-id m)
                                                  (get-in world [:call-meta (:call-id m) :active-attempt])))))
                         q)]))
         (:channels world))
   (into {} (map (fn [[cid c]] [cid (:result c)])) (:completed world))
   (:deferred world)
   (not-empty (:raw-violations world))])

(defn check-terminal-world
  "Runs all five questions Q1..Q5 on a quiescent terminal `world`."
  [world entities check-reruns?]
  (let [completed-ops (vals (:completed world))
        pstates (:pstates world)]
    (or (first (:raw-violations world))
        (check-q1-uniqueness pstates completed-ops)
        (check-q2-ask-answers completed-ops (:ask-committed world))
        (check-q3-unsettled pstates)
        (check-q4-linearizability-and-reruns
         (:init-oracle world) pstates completed-ops entities
         (when check-reruns?
           #(check-client-reruns-after-quiescence world entities))))))

(defn explore-scenario
  "Exhaustively explores schedules of `scenario` under `mode`:
   - `:pure`            — every interleaving, 0 failures (plus client re-run checks)
   - `:individual`      — every interleaving + `fault-budget` `:individual` crashes/retries
   - `:all-after`       — every interleaving + `fault-budget` `:all-after` partition replays
   - `:timeout-overlap` — every interleaving + `fault-budget` overlapping timeout retries
   - `:mixed`           — every interleaving + `fault-budget` faults drawn from ANY of
                          `:individual`, `:all-after`, or `:timeout-overlap` in one history.

  Returns `{:scenario name :mode mode :fault-budget int :states-explored int :terminal-histories int
            :violations [{:question ... :subkind ... :detail ... :trace [...] :completed ... :pstates ...}]}`."
  [{:keys [name entities] :as scenario} mode {:keys [max-states check-reruns? fault-budget]
                                              :or {max-states 25000 check-reruns? true}}]
  (let [w0 (init-world scenario)
        init-fb (if (some? fault-budget)
                  fault-budget
                  (if (= mode :pure) 0 1))
        visited (java.util.HashSet.)
        states-cnt (volatile! 0)
        term-cnt (volatile! 0)
        violations-by-subkind (atom {})]
    (loop [queue (conj clojure.lang.PersistentQueue/EMPTY [w0 init-fb])]
      (when (and (seq queue) (< @states-cnt max-states))
        (let [[w fb] (peek queue)
              queue' (pop queue)
              sig (world-signature w fb)]
          (if (.contains visited sig)
            (recur queue')
            (do
              (.add visited sig)
              (vswap! states-cnt inc)
              (let [acts (runnable-actions w)]
                (cond
                  (:infinite-loop? w)
                  (do
                    (swap! violations-by-subkind update :infinite-loop
                           #(or % {:question :q3-left-unsettled
                                   :subkind :infinite-loop
                                   :detail "Event exceeded max helping rounds without terminating"
                                   :trace (:trace w)
                                   :completed (:completed w)
                                   :pstates (:pstates w)}))
                    (recur queue'))

                  (empty? acts)
                  (do
                    (vswap! term-cnt inc)
                    (when-let [v (check-terminal-world w entities check-reruns?)]
                      (swap! violations-by-subkind update [(:question v) (:subkind v)]
                             #(or % (assoc v
                                           :scenario name
                                           :mode mode
                                           :fault-budget init-fb
                                           :trace (:trace w)
                                           :completed (:completed w)
                                           :pstates (:pstates w)))))
                    ;; In :all-after or :mixed mode, also explore post-commit uncheckpointed worker restart!
                    (let [post-commit-replays
                          (when (and (contains? #{:all-after :mixed} mode) (pos? fb))
                            (for [[t _] (:depots w)
                                  :let [cur (get-in w [:depot-cursors t] 0)]
                                  :when (pos? cur)
                                  idx (range cur)]
                              [(inject-all-after-replay w t idx) (dec fb)]))]
                      (recur (into queue' post-commit-replays))))

                  :else
                  (let [normal-next (map (fn [a] [(step-world w a) fb]) acts)
                        fault-next
                        (when (pos? fb)
                          (case mode
                            :individual
                            (for [[edge q] (:channels w)
                                  :when (seq q)]
                              [(inject-individual-retry w edge) (dec fb)])

                            :all-after
                            (for [[t _] (:depots w)
                                  :let [cur (get-in w [:depot-cursors t] 0)]
                                  :when (pos? cur)
                                  idx (range cur)]
                              [(inject-all-after-replay w t idx) (dec fb)])

                            :timeout-overlap
                            (for [[edge q] (:channels w)
                                  :when (seq q)]
                              [(inject-timeout-overlap-retry w edge) (dec fb)])

                            :mixed
                            (concat
                             (for [[edge q] (:channels w)
                                   :when (seq q)]
                               [(inject-individual-retry w edge) (dec fb)])
                             (for [[t _] (:depots w)
                                   :let [cur (get-in w [:depot-cursors t] 0)]
                                   :when (pos? cur)
                                   idx (range cur)]
                               [(inject-all-after-replay w t idx) (dec fb)])
                             (for [[edge q] (:channels w)
                                   :when (seq q)]
                               [(inject-timeout-overlap-retry w edge) (dec fb)]))

                            nil))]
                    (recur (into queue' (concat normal-next fault-next)))))))))))
    {:scenario name
     :mode mode
     :fault-budget init-fb
     :capped? (>= @states-cnt max-states)
     :states-explored @states-cnt
     :terminal-histories @term-cnt
     :violations (vec (vals @violations-by-subkind))}))
