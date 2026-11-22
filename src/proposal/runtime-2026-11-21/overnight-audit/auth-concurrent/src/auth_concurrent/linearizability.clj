(ns auth-concurrent.linearizability
  "Real-time partial-order (Wing & Gong / Lowe) linearizability and post-batch
  read checker for any implementation of the 18 calls of `softland.auth.client`,
  held against `test-auth/softland/auth/reference.clj`.

  Rules enforced:
    1. Real-time precedence: if call A returned (`:returned-ns`) strictly before
       call B was invoked (`:invoked-ns`), A must appear before B in the total
       order.
    2. Write semantics: each write `[kind arg]` stepped via `reference/step`
       must return `(:result op)`.
    3. `:could-not` on a write (`SPEC.md:140-141` and Sid's Step 3 rule):
       A `{:could-not ...}` from a write says nothing of whether it was taken.
       The search branches on BOTH:
         (a) not taken (leaves state unchanged), and
         (b) taken at some point at or after `(:invoked-ns op)` (note: if a
             client timed out, the write may even take effect after the client
             returned `{:could-not :reach-the-module}`, so when taken its upper
             time bound is unconstrained while predecessors that returned before
             `(:invoked-ns op)` still precede it).
       Exception: `{:could-not :read-the-event}` on an unfit input has identical
       taken/not-taken state (`reference/step` leaves state unchanged); if an
       implementation returns `{:could-not :read-the-event}` on a *fit* write on
       a valid client, that is rejected as a contract violation.
    4. Concurrent reads (`:who-is`, `:ask-of`, `:under`) must match the
       reference's pure read at their point in the total order (unless
       `{:could-not :reach-the-module}` / closed).
    5. Post-batch read sweep: after all concurrent threads return, reads of
       `who-is`, `ask-of`, `under`, and `watch-root` (`live-under`) must match
       `final-ref-state`."
  (:require [softland.auth.reference :as ref]))

(def write-kinds
  #{:invite :enter :own-layer-made :begin-sign-in :replace-secret :end-sign-in
    :make-actor :end-actor :open-session :close-session :ask :answer-ask})

(def read-kinds
  #{:who-is :ask-of :under})

(defn api-from-ns
  "Builds the 18-call client API map from any namespace symbol (e.g.,
  `'softland.auth.reference` or `'softland.auth.client`) that defines the 18
  functions of Auth's client contract."
  [ns-sym]
  (require ns-sym)
  (let [req (fn [sym]
              (or (ns-resolve ns-sym sym)
                  (throw (ex-info (str "Missing Auth client function " ns-sym "/" sym)
                                  {:ns ns-sym :fn sym}))))]
    {:open           @(req 'open)
     :close!         @(req 'close!)
     :invite         @(req 'invite)
     :enter          @(req 'enter)
     :own-layer-made @(req 'own-layer-made)
     :begin-sign-in  @(req 'begin-sign-in)
     :replace-secret @(req 'replace-secret)
     :end-sign-in    @(req 'end-sign-in)
     :make-actor     @(req 'make-actor)
     :end-actor      @(req 'end-actor)
     :open-session   @(req 'open-session)
     :close-session  @(req 'close-session)
     :ask            @(req 'ask)
     :answer-ask     @(req 'answer-ask)
     :who-is         @(req 'who-is)
     :ask-of         @(req 'ask-of)
     :under          @(req 'under)
     :watch-root     @(req 'watch-root)}))

(defn eval-ref-read [state kind arg]
  (case kind
    :who-is (ref/who-is state arg)
    :ask-of (ref/ask-of state arg)
    :under  (ref/under state arg)))

(defn post-reads-match?
  "Checks whether all post-batch reads captured from the implementation after
  all worker threads returned agree with `final-state` in `reference.clj`."
  [final-state post-reads]
  (every?
   (fn [{:keys [kind arg result initial-live]}]
     (if (and (map? result) (contains? result :could-not))
       ;; Post-run verification reads on an open healthy client should succeed;
       ;; only :reach-the-module is tolerated on transient transport errors.
       (= :reach-the-module (:could-not result))
       (case kind
         :who-is (= result (ref/who-is final-state arg))
         :ask-of (= result (ref/ask-of final-state arg))
         :under  (= result (ref/under final-state arg))
         :watch-root-last
         (let [expected-live (ref/live-under final-state arg)]
           ;; watch-root owes no callback if live-under never changed, or if
           ;; transient changes settled back; if callbacks fired, the latest
           ;; delivered value (unless :softland.auth.client/not-reached) must
           ;; equal expected-live when expected-live != initial-live.
           (cond
             (= result :softland.auth.client/not-reached) true
             (= result ::no-callback) (= expected-live initial-live)
             :else (= result expected-live))))))
   post-reads))

(defn post-read-diff
  "Returns the first post-read mismatch against `final-state` for diagnostics."
  [final-state post-reads]
  (some
   (fn [{:keys [kind arg result initial-live]}]
     (let [expected (case kind
                      :who-is (ref/who-is final-state arg)
                      :ask-of (ref/ask-of final-state arg)
                      :under  (ref/under final-state arg)
                      :watch-root-last (ref/live-under final-state arg))]
       (when-not (post-reads-match? final-state [{:kind kind :arg arg :result result :initial-live initial-live}])
         {:kind kind :arg arg :actual result :expected expected})))
   post-reads))

(defn linearizable?
  "Searches whether there exists at least one total order of `history`
  preserving real-time `returned-ns < invoked-ns` precedence whose results and
  `post-reads` match `reference.clj` starting from `init-state`.

  Each entry in `history` is:
    `{:id int :thread int :kind kw :arg any :result any
      :invoked-ns long :returned-ns long :closed-client? bool}`

  Returns `{:ok? true :witness-order [...] :final-state ...}` or
  `{:ok? false :reason ... :history history :post-reads post-reads}`."
  [init-state history post-reads]
  (let [ops-vec (vec history)
        n (count ops-vec)
        all-mask (if (= n 64) -1 (dec (bit-shift-left 1 n)))
        ;; For each op i, compute bitmask of ops j that strictly finished before i started:
        ;; j.returned-ns < i.invoked-ns
        preds (long-array n)
        _ (dotimes [i n]
            (let [inv-i (:invoked-ns (nth ops-vec i))]
              (loop [j 0 m 0]
                (if (= j n)
                  (aset preds i m)
                  (let [op-j (nth ops-vec j)
                        ;; Note: if op-j is a write that returned {:could-not ...} and
                        ;; we take its "taken after timeout" branch, op-j's effective
                        ;; completion might be later than op-j.returned-ns; however,
                        ;; even then op-j only constrains ops invoked AFTER op-j returned
                        ;; if op-j completed by op-j.returned-ns. Relaxing pred when op-j
                        ;; returned :could-not is strictly more permissive (allows both).
                        must-precede? (and (not= i j)
                                           (< (:returned-ns op-j) inv-i)
                                           (not (and (contains? write-kinds (:kind op-j))
                                                     (map? (:result op-j))
                                                     (contains? (:result op-j) :could-not))))]
                    (recur (inc j) (if must-precede? (bit-set m j) m)))))))
        visited (java.util.HashSet.)
        sample-terminal-states (atom [])]
    (letfn [(dfs [^long done-mask state order]
              (if (= done-mask all-mask)
                (do
                  (when (< (count @sample-terminal-states) 3)
                    (swap! sample-terminal-states conj state))
                  (when (post-reads-match? state post-reads)
                    {:ok? true :witness-order order :final-state state}))
                (let [memo-key [done-mask state]]
                  (when-not (.contains visited memo-key)
                    (.add visited memo-key)
                    (loop [i 0]
                      (when (< i n)
                        (if (not (zero? (bit-and done-mask (bit-shift-left 1 i))))
                          (recur (inc i))
                          (let [req (aget preds i)]
                            (if (not= req (bit-and done-mask req))
                              (recur (inc i))
                              (let [op (nth ops-vec i)
                                    {:keys [id kind arg result closed-client?]} op
                                    next-mask (bit-or done-mask (bit-shift-left 1 i))
                                    hit
                                    (cond
                                      ;; Lifecycle ops (:open / :close!) checked structurally, no Auth state change
                                      (or (= kind :open) (= kind :close!) (= kind :watch-root))
                                      (dfs next-mask state (conj order [id kind :lifecycle]))

                                      ;; Write returning {:could-not ...}
                                      (and (contains? write-kinds kind)
                                           (map? result)
                                           (contains? result :could-not))
                                      (let [cn (:could-not result)
                                            [st-taken res-taken] (ref/step state [kind arg])]
                                        (if (= cn :read-the-event)
                                          ;; :read-the-event is only valid if input is unfit (or non-client)
                                          (when (= res-taken {:could-not :read-the-event})
                                            (dfs next-mask state (conj order [id kind :unfit])))
                                          ;; Other :could-not (:reach-the-module, :closed):
                                          ;; allow BOTH not-taken and taken!
                                          (or (dfs next-mask state (conj order [id kind :could-not-dropped]))
                                              (when (not= res-taken {:could-not :read-the-event})
                                                (dfs next-mask st-taken (conj order [id kind :could-not-taken]))))))

                                      ;; Normal write
                                      (contains? write-kinds kind)
                                      (let [[st' expected] (ref/step state [kind arg])]
                                        (when (= result expected)
                                          (dfs next-mask st' (conj order [id kind result]))))

                                      ;; Read
                                      (contains? read-kinds kind)
                                      (if (and (map? result) (contains? result :could-not))
                                        (when (or closed-client?
                                                  (= :reach-the-module (:could-not result))
                                                  (= result (eval-ref-read state kind arg)))
                                          (dfs next-mask state (conj order [id kind result])))
                                        (when (= result (eval-ref-read state kind arg))
                                          (dfs next-mask state (conj order [id kind result])))))]
                                (or hit (recur (inc i)))))))))))))]
      (or (dfs 0 init-state [])
          {:ok? false
           :reason (if (seq @sample-terminal-states)
                     {:type :post-read-mismatch-on-all-valid-orders
                      :first-mismatch (post-read-diff (first @sample-terminal-states) post-reads)}
                     {:type :no-causal-order-matches-call-results})
           :history ops-vec
           :post-reads post-reads}))))
