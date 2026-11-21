(ns auth-blind.model
  "Pure in-memory model of Auth built solely from `spine/auth/SPEC.md` (plus the
  `:driven-by` addition on `open-session`). Used only to verify that the blind
  property suite itself is satisfiable and to construct single-rule broken
  variants (`auth-blind.mutants`).

  The suite itself (`auth-blind.properties`) takes any implementation as a map
  of the 18 contract functions and shares no state with this namespace."
  (:import [java.util UUID]))

;; ---------------------------------------------------------------------------
;; Input validation ("Rules for every call" + "What the runtime does")
;; ---------------------------------------------------------------------------

(defn plain-map? [x]
  (and (map? x) (not (record? x))))

(defn valid-time? [x]
  (int? x))

(defn valid-uuid? [x]
  (instance? UUID x))

(defn valid-hash? [x]
  (and (string? x)
       (= 64 (.length ^String x))
       (boolean (re-matches #"[0-9a-f]{64}" x))))

(defn valid-code? [x]
  (and (string? x) (= 8 (.length ^String x))))

(defn valid-text? [x]
  (and (string? x) (<= (.length ^String x) 256)))

(defn valid-opt-text? [x]
  (or (nil? x) (valid-text? x)))

(defn valid-provider? [x]
  (and (keyword? x)
       (let [s (subs (str x) 1)]
         (and (pos? (.length s)) (<= (.length s) 256)))))

(defn valid-account? [x]
  (and (vector? x)
       (= 2 (count x))
       (valid-provider? (nth x 0))
       (valid-text? (nth x 1))))

(defn valid-says? [x]
  (and (plain-map? x)
       (<= (count x) 32)
       (every? (fn [[k v]]
                 (and (valid-text? k)
                      (string? v)
                      (<= (.length ^String v) 2048)))
               x)))

(defn valid-claim? [x]
  (and (plain-map? x)
       (case (:by x)
         :email (and (= #{:by :email} (set (keys x)))
                     (valid-text? (:email x)))
         :code  (and (= #{:by :hash} (set (keys x)))
                     (valid-hash? (:hash x)))
         false)))

(defn valid-making? [x]
  (and (plain-map? x)
       (= #{:uuid :session} (set (keys x)))
       (valid-uuid? (:uuid x))
       (valid-uuid? (:session x))))

(defn valid-driven-by? [x]
  (or (nil? x)
      (and (plain-map? x)
           (case (:kind x)
             :person (= #{:kind} (set (keys x)))
             :model  (and (= #{:kind :model} (set (keys x)))
                          (valid-text? (:model x)))
             false))))

(defn valid-new-agent? [x]
  (or (nil? x)
      (and (plain-map? x)
           (= #{:label} (set (keys x)))
           (valid-text? (:label x)))))

(defn only-keys? [m allowed]
  (every? #(contains? allowed %) (keys m)))

(defn valid-Time-and-yes-parts? [arg]
  (and (valid-time? (:at arg))
       (if (:yes? arg)
         (and (valid-uuid? (:actor arg))
              (valid-new-agent? (:new-agent arg))
              (valid-uuid? (:sign-in arg))
              (valid-time? (:until arg)))
         (and (or (nil? (:actor arg)) (valid-uuid? (:actor arg)))
              (valid-new-agent? (:new-agent arg))
              (or (nil? (:sign-in arg)) (valid-uuid? (:sign-in arg)))
              (or (nil? (:until arg)) (valid-time? (:until arg)))))))

(defn valid-call-input? [op arg]
  (case op
    :invite
    (and (plain-map? arg)
         (only-keys? arg #{:person :claim :invited-by :at})
         (valid-uuid? (:person arg))
         (valid-claim? (:claim arg))
         (or (= :system (:invited-by arg)) (valid-uuid? (:invited-by arg)))
         (valid-time? (:at arg)))

    :enter
    (and (plain-map? arg)
         (only-keys? arg #{:account :says :email :code-hash :own-layer :making :at})
         (valid-account? (:account arg))
         (valid-says? (:says arg))
         (valid-opt-text? (:email arg))
         (or (nil? (:code-hash arg)) (valid-hash? (:code-hash arg)))
         (valid-uuid? (:own-layer arg))
         (valid-making? (:making arg))
         (valid-time? (:at arg)))

    :own-layer-made
    (and (plain-map? arg)
         (only-keys? arg #{:person :at})
         (valid-uuid? (:person arg))
         (valid-time? (:at arg)))

    :begin-sign-in
    (and (plain-map? arg)
         (only-keys? arg #{:root :actor :sign-in :kind :hash :until :label :at})
         (valid-uuid? (:root arg))
         (valid-uuid? (:actor arg))
         (valid-uuid? (:sign-in arg))
         (contains? #{:browser :program} (:kind arg))
         (valid-hash? (:hash arg))
         (valid-time? (:until arg))
         (valid-opt-text? (:label arg))
         (valid-time? (:at arg)))

    :replace-secret
    (and (plain-map? arg)
         (only-keys? arg #{:root :sign-in :old-hash :new-hash :until :previous-until :at})
         (valid-uuid? (:root arg))
         (valid-uuid? (:sign-in arg))
         (valid-hash? (:old-hash arg))
         (valid-hash? (:new-hash arg))
         (valid-time? (:until arg))
         (valid-time? (:previous-until arg))
         (valid-time? (:at arg)))

    :end-sign-in
    (and (plain-map? arg)
         (only-keys? arg #{:root :sign-in :at})
         (valid-uuid? (:root arg))
         (valid-uuid? (:sign-in arg))
         (valid-time? (:at arg)))

    :make-actor
    (and (plain-map? arg)
         (only-keys? arg #{:root :actor :made-by :label :at})
         (valid-uuid? (:root arg))
         (valid-uuid? (:actor arg))
         (valid-uuid? (:made-by arg))
         (valid-text? (:label arg))
         (valid-time? (:at arg)))

    :end-actor
    (and (plain-map? arg)
         (only-keys? arg #{:root :actor :at})
         (valid-uuid? (:root arg))
         (valid-uuid? (:actor arg))
         (valid-time? (:at arg)))

    :open-session
    (and (plain-map? arg)
         (only-keys? arg #{:root :sign-in :session :until :at :driven-by})
         (valid-uuid? (:root arg))
         (valid-uuid? (:sign-in arg))
         (valid-uuid? (:session arg))
         (valid-time? (:until arg))
         (valid-time? (:at arg))
         (valid-driven-by? (:driven-by arg)))

    :close-session
    (and (plain-map? arg)
         (only-keys? arg #{:root :session :at})
         (valid-uuid? (:root arg))
         (valid-uuid? (:session arg))
         (valid-time? (:at arg)))

    :ask
    (and (plain-map? arg)
         (only-keys? arg #{:code :hash :asks :ends :at})
         (valid-code? (:code arg))
         (valid-hash? (:hash arg))
         (valid-text? (:asks arg))
         (valid-time? (:ends arg))
         (valid-time? (:at arg)))

    :answer-ask
    (and (plain-map? arg)
         (only-keys? arg #{:code :yes? :root :actor :new-agent :sign-in :until :at})
         (valid-code? (:code arg))
         (boolean? (:yes? arg))
         (valid-uuid? (:root arg))
         (valid-Time-and-yes-parts? arg))

    :who-is
    (and (plain-map? arg)
         (only-keys? arg #{:root :sign-in :session})
         (valid-uuid? (:root arg))
         (valid-uuid? (:sign-in arg))
         (or (nil? (:session arg)) (valid-uuid? (:session arg))))

    :ask-of
    (valid-code? arg)

    :under
    (valid-uuid? arg)

    :watch-root
    (valid-uuid? arg)))

;; ---------------------------------------------------------------------------
;; Pure state helpers
;; ---------------------------------------------------------------------------

(defn empty-state []
  {:invitations-by-person {} ; P -> {:claim C :invited-by B :used? bool}
   :person-by-claim       {} ; C -> P
   :links                 {} ; [provider subject] -> {:person P :says map :at long}
   :roots                 {} ; P -> {:own-layer L :making M :own-layer-made? bool
                             ;       :actors {A {:actor A :made-by B-or-nil :label text-or-nil :ended? bool :depth long}}
                             ;       :actor-order [A ...] ; agents only, in order made
                             ;       :sign-ins {G {:sign-in G :actor A :kind K :hash H :initial-hash H0 :via kw
                             ;                     :label text-or-nil :begun t :until t1 :previous map-or-nil
                             ;                     :ended? bool :actor-ended? bool}}
                             ;       :sign-in-order [G ...]
                             ;       :sessions {S {:session S :sign-in G :until t3 :closed? bool :driven-by D-or-nil}}}
   :asks                  {}}) ; U -> {:hash H :asks text :ends t1 :state :waiting|:yes|:no :answer map-or-nil
                               ;       :first-reply map-or-nil}

(defn live-watch-view [root-data]
  (if (nil? root-data)
    {:sign-ins {} :sessions {}}
    {:sign-ins (into {}
                     (keep (fn [[g si]]
                             (when-not (:ended? si)
                               [g {:actor    (:actor si)
                                   :kind     (:kind si)
                                   :hash     (:hash si)
                                   :until    (:until si)
                                   :previous (:previous si)}])))
                     (:sign-ins root-data))
     :sessions (into {}
                     (keep (fn [[s sess]]
                             (when-not (:closed? sess)
                               [s {:sign-in (:sign-in sess)
                                   :until   (:until sess)}])))
                     (:sessions root-data))}))

(defn close-sessions-of-sign-in [root-data g]
  (update root-data :sessions
          (fn [sessions]
            (into {}
                  (map (fn [[s sess]]
                         [s (if (= g (:sign-in sess))
                              (assoc sess :closed? true)
                              sess)]))
                  sessions))))

(defn end-sign-in-in-root [root-data g actor-ended?]
  (if-not (contains? (:sign-ins root-data) g)
    root-data
    (-> root-data
        (update-in [:sign-ins g]
                   (fn [si]
                     (cond-> (assoc si :ended? true)
                       actor-ended? (assoc :actor-ended? true))))
        (close-sessions-of-sign-in g))))

(defn sweep-root-at [state root-id at-time {:keys [expiry-strict-lt?]}]
  (if-not (contains? (:roots state) root-id)
    state
    (let [expired? (fn [until]
                     (if expiry-strict-lt?
                       (< (long until) (long at-time))
                       (<= (long until) (long at-time))))]
      (update-in state [:roots root-id]
                 (fn [root-data]
                   (let [expired-sign-ins (keep (fn [[g si]]
                                                  (when (and (not (:ended? si))
                                                             (expired? (:until si)))
                                                    g))
                                                (:sign-ins root-data))
                         root-data (reduce #(end-sign-in-in-root %1 %2 false)
                                           root-data
                                           expired-sign-ins)]
                     (update root-data :sessions
                             (fn [sessions]
                               (into {}
                                     (map (fn [[s sess]]
                                            [s (if (and (not (:closed? sess))
                                                        (expired? (:until sess)))
                                                 (assoc sess :closed? true)
                                                 sess)]))
                                     sessions)))))))))

(defn descendants-of [actors target-a {:keys [shallow-end-actor?]}]
  (if shallow-end-actor?
    #{target-a}
    (loop [frontier #{target-a}
           all #{target-a}]
      (let [children (into #{}
                           (keep (fn [[a info]]
                                   (when (and (not (contains? all a))
                                              (contains? frontier (:made-by info)))
                                     a)))
                           actors)]
        (if (empty? children)
          all
          (recur children (into all children)))))))

;; ---------------------------------------------------------------------------
;; Configurable model factory (flags = {} is the spec-compliant reference;
;; `auth-blind.mutants` flips one flag at a time)
;; ---------------------------------------------------------------------------

(defn make-world
  "Creates an isolated world handle for `(open world \"auth\")`."
  ([] (make-world "auth"))
  ([module-name]
   {:modules {module-name {:state (atom (empty-state))
                           :watchers (atom {})}}}))

(defn make-auth
  "Returns a map of the 18 Auth client contract functions. `flags` is `{}` for
  the spec-compliant model; broken variants pass a single fault flag."
  ([] (make-auth {}))
  ([flags]
   (let [guard-client
         (fn [client op arg valid? body]
           (let [run-body
                 (fn []
                   (cond
                     (not (and (plain-map? client) (contains? client :closed?)))
                     {:could-not :read-the-event}

                     (and @(:closed? client) (not (:allow-closed-client? flags)))
                     {:could-not :closed}

                     (not valid?)
                     (if (:throw-on-bad-input? flags)
                       (throw (IllegalArgumentException. "bad input"))
                       {:could-not :read-the-event})

                     :else (body)))]
             (if (:throw-on-bad-input? flags)
               (run-body)
               (try (run-body)
                    (catch Throwable t
                      {:could-not :reach-the-module :threw (.getName (class t))})))))

         notify-watchers!
         (fn [mod-entry root-id before-state after-state]
           (let [v-before (live-watch-view (get-in before-state [:roots root-id]))
                 v-after  (live-watch-view (get-in after-state [:roots root-id]))
                 v-send   (if (:leak-driven-by-in-watch? flags)
                            (update v-after :sessions
                                    (fn [m]
                                      (into {}
                                            (map (fn [[s sm]]
                                                   [s (assoc sm :driven-by
                                                             (get-in after-state [:roots root-id :sessions s :driven-by]))]))
                                            m)))
                            v-after)]
             (when (not= v-before v-after)
               (doseq [[_ {:keys [root closed? stopped? f]}] @(:watchers mod-entry)
                       :when (and (= root root-id)
                                  (not @stopped?)
                                  (or (:call-watch-after-close? flags) (not @closed?)))]
                 (try (f v-send) (catch Throwable _ nil))))))

         transact!
         (fn [client root-id step-fn]
           (let [mod-entry (:mod client)
                 state-atom (:state mod-entry)
                 out (atom nil)
                 [before after]
                 (swap-vals! state-atom
                             (fn [st]
                               (let [[st' res] (step-fn st)]
                                 (reset! out res)
                                 st')))]
             (when root-id
               (notify-watchers! mod-entry root-id before after))
             @out))

         expired-at-make?
         (fn [until at]
           (if (:expiry-strict-lt? flags)
             (< (long until) (long at))
             (<= (long until) (long at))))]

     {:open
      (fn [cluster module-name]
        (try
          (if-let [mod-entry (and (plain-map? cluster)
                                  (string? module-name)
                                  (get-in cluster [:modules module-name]))]
            {:mod mod-entry
             :client-id (UUID/randomUUID)
             :closed? (atom false)}
            {:could-not :find-the-module :module module-name})
          (catch Throwable t
            {:could-not :find-the-module :threw (.getName (class t))})))

      :close!
      (fn [client]
        (when (and (plain-map? client) (contains? client :closed?))
          (reset! (:closed? client) true))
        nil)

      :invite
      (fn [client m]
        (guard-client client :invite m (valid-call-input? :invite m)
          (fn []
            (let [{:keys [person claim invited-by]} m]
              (transact! client nil
                (fn [st]
                  (let [existing-p (get-in st [:invitations-by-person person])
                        claim-owner (get-in st [:person-by-claim claim])]
                    (cond
                      ;; Repeat first
                      (and existing-p (= claim (:claim existing-p)))
                      [st {:invited person}]

                      ;; Wrong refusal order mutant
                      (and (:flip-invite-refusal-order? flags)
                           existing-p (not= claim (:claim existing-p)))
                      [st {:refused :person-in-use}]

                      ;; Refusal 1: claim-in-use (unless mutant lets proven claim be reused)
                      (and claim-owner
                           (not= person claim-owner)
                           (not (and (:reuse-used-claim? flags)
                                     (get-in st [:invitations-by-person claim-owner :used?]))))
                      [st {:refused :claim-in-use}]

                      ;; Refusal 2: person-in-use
                      (some? existing-p)
                      [st {:refused :person-in-use}]

                      :else
                      [(-> st
                           (assoc-in [:invitations-by-person person]
                                     {:claim claim :invited-by invited-by :used? false})
                           (assoc-in [:person-by-claim claim] person))
                       {:invited person}]))))))))

      :enter
      (fn [client m]
        (guard-client client :enter m (valid-call-input? :enter m)
          (fn []
            (let [{:keys [account says email code-hash own-layer making at]} m]
              (transact! client nil
                (fn [st]
                  (if-let [link (get-in st [:links account])]
                    (let [p (:person link)
                          st' (if (:overwrite-own-layer-on-reenter? flags)
                                (-> st
                                    (assoc-in [:links account] {:person p :says says :at (long at)})
                                    (assoc-in [:roots p :own-layer] own-layer)
                                    (assoc-in [:roots p :making] making))
                                (assoc-in st [:links account] {:person p :says says :at (long at)}))
                          r (get-in st' [:roots p])]
                      [st' {:person p
                            :own-layer (:own-layer r)
                            :making (:making r)
                            :own-layer-made? (:own-layer-made? r)}])
                    (let [code-claim (when code-hash {:by :code :hash code-hash})
                          email-claim (when email {:by :email :email email})
                          waiting-person (fn [c]
                                           (when c
                                             (let [p (get-in st [:person-by-claim c])]
                                               (when (and p (not (get-in st [:invitations-by-person p :used?])))
                                                 [p c]))))
                          [p matched-claim] (if (:email-before-code-on-enter? flags)
                                              (or (waiting-person email-claim) (waiting-person code-claim))
                                              (or (waiting-person code-claim) (waiting-person email-claim)))]
                      (if-not p
                        [st {:refused :not-invited}]
                        (let [st' (-> st
                                      (assoc-in [:invitations-by-person p :used?] true)
                                      (assoc-in [:links account] {:person p :says says :at (long at)})
                                      (assoc-in [:roots p]
                                                {:own-layer own-layer
                                                 :making making
                                                 :own-layer-made? false
                                                 :actors {p {:actor p :made-by nil :label nil :ended? false :depth 0}}
                                                 :actor-order []
                                                 :sign-ins {}
                                                 :sign-in-order []
                                                 :sessions {}}))]
                          [st' {:person p
                                :own-layer own-layer
                                :making making
                                :own-layer-made? false}]))))))))))

      :own-layer-made
      (fn [client m]
        (guard-client client :own-layer-made m (valid-call-input? :own-layer-made m)
          (fn []
            (let [{:keys [person at]} m]
              (transact! client (when (:sweep-on-non-root-write? flags) person)
                (fn [st]
                  (let [st (if (:sweep-on-non-root-write? flags)
                             (sweep-root-at st person at flags)
                             st)]
                    (if-not (contains? (:roots st) person)
                      [st {:refused :no-such-person}]
                      [(assoc-in st [:roots person :own-layer-made?] true)
                       {:made person}]))))))))

      :begin-sign-in
      (fn [client m]
        (guard-client client :begin-sign-in m (valid-call-input? :begin-sign-in m)
          (fn []
            (let [{:keys [root actor sign-in kind hash until label at]} m]
              (transact! client root
                (fn [st0]
                  (let [st (sweep-root-at st0 root at flags)
                        r (get-in st [:roots root])
                        existing-si (get-in r [:sign-ins sign-in])
                        act-info (get-in r [:actors actor])]
                    (cond
                      ;; Repeat first
                      (and existing-si
                           (= :begin-sign-in (:via existing-si))
                           (= actor (:actor existing-si))
                           (= kind (:kind existing-si))
                           (= hash (:initial-hash existing-si))
                           (not (:refuse-repeat-after-end? flags)))
                      [st {:begun sign-in}]

                      (nil? act-info)
                      [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :no-such-actor}]

                      (:ended? act-info)
                      [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :ended}]

                      (some? existing-si)
                      [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :in-use}]

                      :else
                      (let [never-live? (and (not (:allow-expired-at-make? flags))
                                             (expired-at-make? until at))
                            si {:sign-in sign-in
                                :actor actor
                                :kind kind
                                :hash hash
                                :initial-hash hash
                                :via :begin-sign-in
                                :label label
                                :begun (long at)
                                :until (long until)
                                :previous nil
                                :ended? never-live?
                                :actor-ended? false}]
                        [(-> st
                             (assoc-in [:roots root :sign-ins sign-in] si)
                             (update-in [:roots root :sign-in-order] conj sign-in))
                         {:begun sign-in}])))))))))

      :replace-secret
      (fn [client m]
        (guard-client client :replace-secret m (valid-call-input? :replace-secret m)
          (fn []
            (let [{:keys [root sign-in old-hash new-hash until previous-until at]} m]
              (transact! client root
                (fn [st0]
                  (let [st (sweep-root-at st0 root at flags)
                        si (get-in st [:roots root :sign-ins sign-in])]
                    (cond
                      ;; Repeat first: H' is sign-in's hash now and H is the one before it
                      (and si
                           (:previous si)
                           (= new-hash (:hash si))
                           (= old-hash (get-in si [:previous :hash])))
                      [st {:replaced sign-in}]

                      (or (nil? si) (:ended? si))
                      [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :not-live}]

                      (and (:accept-previous-secret-for-replace? flags)
                           (:previous si)
                           (= old-hash (get-in si [:previous :hash])))
                      [(assoc-in st [:roots root :sign-ins sign-in]
                                 (assoc si
                                        :hash new-hash
                                        :until (long until)
                                        :previous {:hash old-hash :until (long previous-until)}))
                       {:replaced sign-in}]

                      (not= old-hash (:hash si))
                      [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :wrong-secret}]

                      :else
                      (let [never-live? (expired-at-make? until at)
                            si' (-> si
                                    (assoc :hash new-hash
                                           :until (long until)
                                           :previous (if (:drop-previous-secret? flags)
                                                       nil
                                                       {:hash old-hash :until (long previous-until)}))
                                    (cond-> never-live? (assoc :ended? true)))
                            st' (assoc-in st [:roots root :sign-ins sign-in] si')
                            st' (if never-live?
                                  (update-in st' [:roots root] close-sessions-of-sign-in sign-in)
                                  st')]
                        [st' {:replaced sign-in}])))))))))

      :end-sign-in
      (fn [client m]
        (guard-client client :end-sign-in m (valid-call-input? :end-sign-in m)
          (fn []
            (let [{:keys [root sign-in at]} m]
              (transact! client root
                (fn [st0]
                  (let [st (sweep-root-at st0 root at flags)
                        si (get-in st [:roots root :sign-ins sign-in])]
                    (if (nil? si)
                      [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :no-such-sign-in}]
                      [(if (:leave-sessions-open-on-end-sign-in? flags)
                         (assoc-in st [:roots root :sign-ins sign-in :ended?] true)
                         (update-in st [:roots root] end-sign-in-in-root sign-in false))
                       {:ended sign-in}]))))))))

      :make-actor
      (fn [client m]
        (guard-client client :make-actor m (valid-call-input? :make-actor m)
          (fn []
            (let [{:keys [root actor made-by label at]} m]
              (transact! client root
                (fn [st0]
                  (let [st (sweep-root-at st0 root at flags)
                        r (get-in st [:roots root])
                        existing-a (get-in r [:actors actor])
                        maker-info (get-in r [:actors made-by])]
                    (cond
                      ;; Repeat first: root and maker match (and actor is an agent, not the root person)
                      (and existing-a
                           (some? (:made-by existing-a))
                           (= made-by (:made-by existing-a)))
                      [st {:made actor}]

                      (nil? maker-info)
                      [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :no-such-maker}]

                      (:ended? maker-info)
                      [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :ended}]

                      (some? existing-a)
                      [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :in-use}]

                      (and (not (:no-depth-bound? flags))
                           (>= (:depth maker-info) 32))
                      [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :too-deep}]

                      :else
                      [(-> st
                           (assoc-in [:roots root :actors actor]
                                     {:actor actor
                                      :made-by made-by
                                      :label label
                                      :ended? false
                                      :depth (inc (:depth maker-info))})
                           (update-in [:roots root :actor-order] conj actor))
                       {:made actor}]))))))))

      :end-actor
      (fn [client m]
        (guard-client client :end-actor m (valid-call-input? :end-actor m)
          (fn []
            (let [{:keys [root actor at]} m]
              (transact! client root
                (fn [st0]
                  (let [st (sweep-root-at st0 root at flags)
                        r (get-in st [:roots root])
                        act-info (get-in r [:actors actor])]
                    (cond
                      (nil? act-info)
                      [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :no-such-actor}]

                      (and (= root actor) (not (:allow-ending-person? flags)))
                      [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :is-a-person}]

                      :else
                      (let [doomed (descendants-of (:actors r) actor flags)
                            st' (update-in st [:roots root]
                                           (fn [r]
                                             (let [r (update r :actors
                                                             (fn [actors]
                                                               (into {}
                                                                     (map (fn [[a info]]
                                                                            [a (if (contains? doomed a)
                                                                                 (assoc info :ended? true)
                                                                                 info)]))
                                                                     actors)))
                                                   doomed-sis (keep (fn [[g si]]
                                                                      (when (contains? doomed (:actor si)) g))
                                                                    (:sign-ins r))]
                                               (reduce #(end-sign-in-in-root %1 %2 true) r doomed-sis))))]
                        [st' {:ended actor}])))))))))

      :open-session
      (fn [client m]
        (guard-client client :open-session m (valid-call-input? :open-session m)
          (fn []
            (let [{:keys [root sign-in session until at driven-by]} m]
              (transact! client root
                (fn [st0]
                  (let [st (sweep-root-at st0 root at flags)
                        r (get-in st [:roots root])
                        si (get-in r [:sign-ins sign-in])
                        existing-s (get-in r [:sessions session])]
                    (cond
                      ;; Repeat: same root, sign-in, and session
                      (and existing-s (= sign-in (:sign-in existing-s)))
                      (if (:closed? existing-s)
                        (if (:reopen-closed-session? flags)
                          [(assoc-in st [:roots root :sessions session]
                                     (assoc existing-s :closed? false :until (long until)))
                           {:open session}]
                          [st {:open session}])
                        (if (or (nil? si) (:ended? si))
                          [st {:open session}]
                          (let [new-until (if (:shrink-session-until-on-repeat? flags)
                                            (long until)
                                            (max (long (:until existing-s)) (long until)))
                                new-db (if (:overwrite-driven-by-on-repeat? flags)
                                         driven-by
                                         (:driven-by existing-s))]
                            [(assoc-in st [:roots root :sessions session]
                                       (assoc existing-s :until new-until :driven-by new-db))
                             {:open session}])))

                      (or (nil? si) (:ended? si))
                      [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :not-live}]

                      (some? existing-s)
                      [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :in-use}]

                      :else
                      (let [never-live? (and (not (:allow-expired-at-make? flags))
                                             (expired-at-make? until at))]
                        [(assoc-in st [:roots root :sessions session]
                                   {:session session
                                    :sign-in sign-in
                                    :until (long until)
                                    :closed? never-live?
                                    :driven-by driven-by})
                         {:open session}])))))))))

      :close-session
      (fn [client m]
        (guard-client client :close-session m (valid-call-input? :close-session m)
          (fn []
            (let [{:keys [root session at]} m]
              (transact! client root
                (fn [st0]
                  (let [st (sweep-root-at st0 root at flags)
                        sess (get-in st [:roots root :sessions session])]
                    (if (nil? sess)
                      [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :no-such-session}]
                      [(assoc-in st [:roots root :sessions session :closed?] true)
                       {:closed session}]))))))))

      :ask
      (fn [client m]
        (guard-client client :ask m (valid-call-input? :ask m)
          (fn []
            (let [{:keys [code hash asks ends]} m]
              (transact! client nil
                (fn [st]
                  (if-let [existing (get-in st [:asks code])]
                    (if (= hash (:hash existing))
                      [st {:asked code}]
                      [st {:refused :in-use}])
                    [(assoc-in st [:asks code]
                               {:hash hash
                                :asks asks
                                :ends (long ends)
                                :state :waiting
                                :answer nil
                                :first-reply nil})
                     {:asked code}])))))))

      :answer-ask
      (fn [client m]
        (guard-client client :answer-ask m (valid-call-input? :answer-ask m)
          (fn []
            (let [{:keys [code yes? root actor new-agent sign-in until at]} m]
              (transact! client root
                (fn [st0]
                  (let [st (sweep-root-at st0 root at flags)
                        ask-entry (get-in st [:asks code])
                        r (get-in st [:roots root])]
                    (cond
                      (nil? ask-entry)
                      [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :no-such-ask}]

                      (not= :waiting (:state ask-entry))
                      (if (:refuse-over-on-already-answered-ask? flags)
                        [st {:refused :over}]
                        [st (:first-reply ask-entry)])

                      (>= (long at) (long (:ends ask-entry)))
                      [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :over}]

                      (not yes?)
                      (cond
                        (nil? r)
                        [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :no-such-actor}]

                        (and (:check-sign-in-on-no-answer? flags)
                             (contains? (:sign-ins r) sign-in))
                        [st {:refused :in-use}]

                        :else
                        (let [reply {:no code}]
                          [(assoc-in st [:asks code]
                                     (assoc ask-entry :state :no :answer nil :first-reply reply))
                           reply]))

                      ;; yes? is true:
                      (nil? r)
                      [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :no-such-actor}]

                      (some? new-agent)
                      (cond
                        (or (contains? (:actors r) actor)
                            (contains? (:sign-ins r) sign-in))
                        [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :in-use}]

                        :else
                        (let [never-live? (expired-at-make? until at)
                              reply {:yes {:root root :actor actor :sign-in sign-in}}
                              si {:sign-in sign-in
                                  :actor actor
                                  :kind :program
                                  :hash (:hash ask-entry)
                                  :initial-hash (:hash ask-entry)
                                  :via :answer-ask
                                  :label (:asks ask-entry)
                                  :begun (long at)
                                  :until (long until)
                                  :previous nil
                                  :ended? never-live?
                                  :actor-ended? false}]
                          [(-> st
                               (assoc-in [:roots root :actors actor]
                                         {:actor actor :made-by root :label (:label new-agent)
                                          :ended? false :depth 1})
                               (update-in [:roots root :actor-order] conj actor)
                               (assoc-in [:roots root :sign-ins sign-in] si)
                               (update-in [:roots root :sign-in-order] conj sign-in)
                               (assoc-in [:asks code]
                                         (assoc ask-entry
                                                :state :yes
                                                :answer {:root root :actor actor :sign-in sign-in}
                                                :first-reply reply)))
                           reply]))

                      :else
                      (let [act-info (get-in r [:actors actor])]
                        (cond
                          (nil? act-info)
                          [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :no-such-actor}]

                          (:ended? act-info)
                          [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :ended}]

                          (contains? (:sign-ins r) sign-in)
                          [(if (:skip-sweep-on-refusal? flags) st0 st) {:refused :in-use}]

                          :else
                          (let [never-live? (expired-at-make? until at)
                                reply {:yes {:root root :actor actor :sign-in sign-in}}
                                si {:sign-in sign-in
                                    :actor actor
                                    :kind :program
                                    :hash (:hash ask-entry)
                                    :initial-hash (:hash ask-entry)
                                    :via :answer-ask
                                    :label (:asks ask-entry)
                                    :begun (long at)
                                    :until (long until)
                                    :previous nil
                                    :ended? never-live?
                                    :actor-ended? false}]
                            [(-> st
                                 (assoc-in [:roots root :sign-ins sign-in] si)
                                 (update-in [:roots root :sign-in-order] conj sign-in)
                                 (assoc-in [:asks code]
                                           (assoc ask-entry
                                                  :state :yes
                                                  :answer {:root root :actor actor :sign-in sign-in}
                                                  :first-reply reply)))
                             reply])))))))))))

      :who-is
      (fn [client m]
        (guard-client client :who-is m (valid-call-input? :who-is m)
          (fn []
            (let [{:keys [root sign-in session]} m
                  st @(:state (:mod client))
                  r (get-in st [:roots root])
                  si (get-in r [:sign-ins sign-in])
                  sess (when session (get-in r [:sessions session]))]
              {:sign-in (when (and si (not (and (:hide-ended-in-reads? flags) (:ended? si))))
                          {:actor        (:actor si)
                           :kind         (:kind si)
                           :hash         (:hash si)
                           :until        (:until si)
                           :previous     (:previous si)
                           :ended?       (:ended? si)
                           :actor-ended? (:actor-ended? si)})
               :session (when (and sess (not (and (:hide-ended-in-reads? flags) (:closed? sess))))
                          {:sign-in   (:sign-in sess)
                           :until     (:until sess)
                           :closed?   (:closed? sess)
                           :driven-by (:driven-by sess)})
               :own-layer (:own-layer r)
               :own-layer-made? (boolean (:own-layer-made? r))}))))

      :ask-of
      (fn [client code]
        (guard-client client :ask-of code (valid-call-input? :ask-of code)
          (fn []
            (when-let [a (get-in @(:state (:mod client)) [:asks code])]
              {:hash   (:hash a)
               :asks   (:asks a)
               :ends   (:ends a)
               :state  (:state a)
               :answer (:answer a)}))))

      :under
      (fn [client root]
        (guard-client client :under root (valid-call-input? :under root)
          (fn []
            (let [r (get-in @(:state (:mod client)) [:roots root])]
              {:actors   (into []
                               (keep (fn [a]
                                       (let [info (get-in r [:actors a])]
                                         (when (and info (not (and (:drop-ended-actors-in-under? flags)
                                                                   (:ended? info))))
                                           {:actor   (:actor info)
                                            :made-by (:made-by info)
                                            :label   (:label info)
                                            :ended?  (:ended? info)}))))
                               (:actor-order r))
               :sign-ins (into []
                               (keep (fn [g]
                                       (let [si (get-in r [:sign-ins g])]
                                         (when (and si (or (:include-ended-sign-ins-in-under? flags)
                                                           (not (:ended? si))))
                                           {:sign-in (:sign-in si)
                                            :actor   (:actor si)
                                            :kind    (:kind si)
                                            :label   (:label si)
                                            :begun   (:begun si)
                                            :until   (:until si)}))))
                               (:sign-in-order r))}))))

      :watch-root
      (fn [client root f]
        (guard-client client :watch-root root (and (valid-call-input? :watch-root root) (ifn? f))
          (fn []
            (let [wid (UUID/randomUUID)
                  stopped? (atom false)
                  mod-entry (:mod client)]
              (swap! (:watchers mod-entry) assoc wid
                     {:root root :closed? (:closed? client) :stopped? stopped? :f f})
              (when (:call-watch-on-register? flags)
                (f (live-watch-view (get-in @(:state mod-entry) [:roots root]))))
              (fn []
                (reset! stopped? true)
                (swap! (:watchers mod-entry) dissoc wid)
                nil)))))})))
