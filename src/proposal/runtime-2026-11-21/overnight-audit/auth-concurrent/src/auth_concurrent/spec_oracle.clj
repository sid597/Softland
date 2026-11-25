(ns auth-concurrent.spec-oracle
  "Pure sequential oracle of Auth's contract (`spine/auth/SPEC.md`) together
  with Sid's two calls of 22 November:
    1. Every part that the contract names is there, nil where `-or-nil`, each
       in its stated form, nothing extra; `:driven-by` alone may be left out.
    2. A repeat's result comes from what stands, never from a memory of calls.
       Once an ask has an answer, every answer that fits gives what stands,
       before any refusal. An enter of an account that has a link gives its
       person, the fixed own lane and making, and `:own-lane-made?` as it
       stands then. `:says` is kept from the enter with the latest `:at`.

  Used in Step 1 to verify whether every concurrent / retried / re-run history
  admits at least one causal linearization (respecting real-time return-before-
  invoke precedence) whose return values, read-after-write snapshots, and
  standing state (`:says`, links, claims, asks, roots) are allowed by the spec
  and Sid's two calls.")

(def empty-oracle-state
  {:persons {}
   :claims {}
   :links {}
   :roots {}
   :asks {}})

(def ^:private empty-root
  {:agents {} :agent-order [] :sign-ins {} :sign-in-order [] :sessions {}})

(defn- entered? [st P]
  (some? (get-in st [:persons P :account])))

(defn- actor-of [st P A]
  (if (= A P)
    (when (entered? st P) {:person? true :depth 0 :ended? false})
    (get-in st [:roots P :agents A])))

(defn- actor-live? [st P A]
  (loop [cur A steps 0]
    (let [a (actor-of st P A)]
      (if (nil? a)
        false
        (let [ac (actor-of st P cur)]
          (cond (nil? ac) false
                (:person? ac) true
                (:ended? ac) false
                (> steps 32) false
                :else (recur (:made-by ac) (inc steps))))))))

(defn- end-sign-ins [st P Gs]
  (update-in st [:roots P]
             (fn [r]
               (-> r
                   (update :sign-ins (fn [gs] (reduce #(assoc-in %1 [%2 :ended?] true) gs Gs)))
                   (update :sessions
                           (fn [ss]
                             (into {}
                                   (map (fn [[S s]]
                                          [S (if (contains? Gs (:sign-in s))
                                               (assoc s :closed? true)
                                               s)]))
                                   ss)))))))

(defn take-out [st P at]
  (if-let [r (get-in st [:roots P])]
    (let [Gs (into #{}
                   (keep (fn [[G g]] (when (and (not (:ended? g)) (<= (:until g) at)) G)))
                   (:sign-ins r))
          Ss (into #{}
                   (keep (fn [[S s]] (when (and (not (:closed? s)) (<= (:until s) at)) S)))
                   (:sessions r))]
      (-> st
          (end-sign-ins P Gs)
          (update-in [:roots P :sessions]
                     (fn [ss] (reduce #(assoc-in %1 [%2 :closed?] true) ss Ss)))))
    st))

(defn oracle-step
  "Applies `[op input]` sequentially under SPEC.md + Sid's 2 calls of 22 Nov.
  Returns `[state' result]`."
  [st [op input]]
  (case op
    :invite
    (let [{:keys [person claim invited-by at]} input
          named (get-in st [:claims claim])
          has (get-in st [:persons person :claim])]
      (cond
        (= named person) [st {:invited person}]
        (some? named) [st {:refused :claim-in-use}]
        (some? has) [st {:refused :person-in-use}]
        :else [(-> st
                   (assoc-in [:claims claim] person)
                   (assoc-in [:persons person] {:claim claim :invited-by invited-by :invited-at at}))
               {:invited person}]))

    :enter
    (let [{:keys [account says email code-hash own-lane making at]} input
          link (get-in st [:links account])
          waits (fn [c]
                  (when-let [P (get-in st [:claims c])]
                    (when-not (entered? st P) P)))
          proven (or (when code-hash (waits {:by :code :hash code-hash}))
                     (when email (waits {:by :email :email email})))]
      (cond
        link
        (let [P (:person link)
              p (get-in st [:persons P])
              ;; Sid's call 2 (SPEC.md:282-284): :says is kept with the link from
              ;; the enter with the latest :at; an enter whose :at is not later
              ;; than the kept one's leaves it.
              keep-new-says? (> at (:said-at link Long/MIN_VALUE))
              st' (if keep-new-says?
                    (update-in st [:links account] assoc :says says :said-at at)
                    st)]
          [st' {:person P
                :own-lane (:own-lane p)
                :making (:making p)
                :own-lane-made? (boolean (:own-lane-made? p))}])

        proven
        (let [st' (-> st
                      (assoc-in [:links account] {:person proven :says says :said-at at})
                      (update-in [:persons proven] assoc
                                 :account account :own-lane own-lane
                                 :making making :own-lane-made? false :entered-at at)
                      (update-in [:roots proven] #(or % empty-root)))]
          [st' {:person proven :own-lane own-lane :making making :own-lane-made? false}])

        :else
        [st {:refused :not-invited}]))

    :own-lane-made
    (let [{:keys [person]} input]
      (if (entered? st person)
        [(assoc-in st [:persons person :own-lane-made?] true) {:made person}]
        [st {:refused :no-such-person}]))

    :ask
    (let [{:keys [code hash asks ends at]} input
          k (get-in st [:asks code])]
      (cond
        (and k (= (:hash k) hash)) [st {:asked code}]
        (some? k) [st {:refused :in-use}]
        :else [(assoc-in st [:asks code]
                         {:hash hash :asks asks :ends ends
                          :born-over? (<= ends at) :state :waiting :answer nil})
               {:asked code}]))

    :answer-ask
    (let [{:keys [code yes? root actor new-agent sign-in until at]} input
          st0 (take-out st root at)
          k (get-in st0 [:asks code])]
      (cond
        (nil? k) [st0 {:refused :no-such-ask}]
        ;; Sid's call 2: Once an ask has an answer, every answer that fits gives
        ;; what stands, before any refusal.
        (= :yes (:state k)) [st0 {:yes (:answer k)}]
        (= :no (:state k)) [st0 {:no code}]
        (or (:born-over? k) (>= at (:ends k))) [st0 {:refused :over}]
        (not yes?)
        (if (entered? st0 root)
          [(assoc-in st0 [:asks code :state] :no) {:no code}]
          [st0 {:refused :no-such-actor}])
        ;; yes? == true
        (some? new-agent)
        (cond
          (not (entered? st0 root)) [st0 {:refused :no-such-actor}]
          (or (some? (actor-of st0 root actor))
              (some? (get-in st0 [:roots root :sign-ins sign-in])))
          [st0 {:refused :in-use}]
          :else
          (let [ans {:root root :actor actor :sign-in sign-in}
                born-ended? (<= until at)
                st1 (-> st0
                        (assoc-in [:roots root :agents actor]
                                  {:made-by root :label (:label new-agent) :depth 1 :ended? false})
                        (update-in [:roots root :agent-order] conj actor)
                        (assoc-in [:roots root :sign-ins sign-in]
                                  {:actor actor :kind :program :hash (:hash k) :first-hash (:hash k)
                                   :until until :previous nil :label (:asks k) :begun at :ended? born-ended?})
                        (update-in [:roots root :sign-in-order] conj sign-in)
                        (assoc-in [:asks code] (assoc k :state :yes :answer ans)))]
            [st1 {:yes ans}]))
        :else
        (cond
          (nil? (actor-of st0 root actor)) [st0 {:refused :no-such-actor}]
          (not (actor-live? st0 root actor)) [st0 {:refused :ended}]
          (some? (get-in st0 [:roots root :sign-ins sign-in])) [st0 {:refused :in-use}]
          :else
          (let [ans {:root root :actor actor :sign-in sign-in}
                born-ended? (<= until at)
                st1 (-> st0
                        (assoc-in [:roots root :sign-ins sign-in]
                                  {:actor actor :kind :program :hash (:hash k) :first-hash (:hash k)
                                   :until until :previous nil :label (:asks k) :begun at :ended? born-ended?})
                        (update-in [:roots root :sign-in-order] conj sign-in)
                        (assoc-in [:asks code] (assoc k :state :yes :answer ans)))]
            [st1 {:yes ans}]))))

    :make-actor
    (let [{:keys [root actor made-by label at]} input
          st0 (take-out st root at)
          existing (get-in st0 [:roots root :agents actor])
          maker (actor-of st0 root made-by)]
      (cond
        (and existing (= (:made-by existing) made-by)) [st0 {:made actor}]
        (nil? maker) [st0 {:refused :no-such-maker}]
        (not (actor-live? st0 root made-by)) [st0 {:refused :ended}]
        (some? (actor-of st0 root actor)) [st0 {:refused :in-use}]
        (>= (:depth maker) 32) [st0 {:refused :too-deep}]
        :else [(-> st0
                   (assoc-in [:roots root :agents actor]
                             {:made-by made-by :label label :depth (inc (:depth maker)) :ended? false})
                   (update-in [:roots root :agent-order] conj actor))
               {:made actor}]))

    :begin-sign-in
    (let [{:keys [root actor sign-in kind hash until label at]} input
          st0 (take-out st root at)
          g (get-in st0 [:roots root :sign-ins sign-in])]
      (cond
        (and g (= (:actor g) actor) (= (:kind g) kind) (= (:first-hash g) hash)) [st0 {:begun sign-in}]
        (nil? (actor-of st0 root actor)) [st0 {:refused :no-such-actor}]
        (not (actor-live? st0 root actor)) [st0 {:refused :ended}]
        (some? g) [st0 {:refused :in-use}]
        :else [(-> st0
                   (assoc-in [:roots root :sign-ins sign-in]
                             {:actor actor :kind kind :hash hash :first-hash hash
                              :until until :previous nil :label label :begun at
                              :ended? (<= until at)})
                   (update-in [:roots root :sign-in-order] conj sign-in))
               {:begun sign-in}]))

    :open-session
    (let [{:keys [root sign-in session until driven-by at]} input
          st0 (take-out st root at)
          s (get-in st0 [:roots root :sessions session])
          g (get-in st0 [:roots root :sign-ins sign-in])
          g-live? (and g (not (:ended? g)) (< at (:until g)) (actor-live? st0 root (:actor g)))]
      (cond
        (and s (= (:sign-in s) sign-in))
        [(if (:closed? s)
           st0
           (update-in st0 [:roots root :sessions session :until] max until))
         {:open session}]
        (not g-live?) [st0 {:refused :not-live}]
        (some? s) [st0 {:refused :in-use}]
        :else [(assoc-in st0 [:roots root :sessions session]
                         {:sign-in sign-in :until until :driven-by driven-by :closed? (<= until at)})
               {:open session}]))

    :end-actor
    (let [{:keys [root actor at]} input
          st0 (take-out st root at)
          a (actor-of st0 root actor)]
      (cond
        (and a (:ended? a)) [st0 {:ended actor}]
        (nil? a) [st0 {:refused :no-such-actor}]
        (:person? a) [st0 {:refused :is-a-person}]
        :else
        (let [agents (get-in st0 [:roots root :agents] {})
              sub (loop [found #{actor}]
                    (let [more (into found
                                     (keep (fn [[B b]] (when (contains? found (:made-by b)) B)))
                                     agents)]
                      (if (= more found) found (recur more))))
              Gs (into #{}
                       (keep (fn [[G g]] (when (contains? sub (:actor g)) G)))
                       (get-in st0 [:roots root :sign-ins] {}))]
          [(-> st0
               (update-in [:roots root :agents]
                          (fn [as] (reduce #(assoc-in %1 [%2 :ended?] true) as sub)))
               (end-sign-ins root Gs))
           {:ended actor}])))))

(defn oracle-who-is [st {:keys [root sign-in session]}]
  (let [g (get-in st [:roots root :sign-ins sign-in])
        s (when session (get-in st [:roots root :sessions session]))
        p (when (entered? st root) (get-in st [:persons root]))]
    {:sign-in (when g
                {:actor (:actor g) :kind (:kind g) :hash (:hash g) :until (:until g)
                 :previous (:previous g) :ended? (boolean (:ended? g))
                 :actor-ended? (not (actor-live? st root (:actor g)))})
     :session (when s
                {:sign-in (:sign-in s) :until (:until s) :closed? (boolean (:closed? s))
                 :driven-by (:driven-by s)})
     :own-lane (:own-lane p)
     :own-lane-made? (boolean (:own-lane-made? p))}))

(defn oracle-ask-of [st code]
  (when-let [k (get-in st [:asks code])]
    {:hash (:hash k) :asks (:asks k) :ends (:ends k) :state (:state k) :answer (:answer k)}))

(defn oracle-under [st P]
  (let [r (get-in st [:roots P])]
    {:actors (mapv (fn [A]
                     (let [a (get-in r [:agents A])]
                       {:actor A :made-by (:made-by a) :label (:label a) :ended? (boolean (:ended? a))}))
                   (:agent-order r))
     :sign-ins (->> (:sign-in-order r)
                    (keep (fn [G]
                            (let [g (get-in r [:sign-ins G])]
                              (when (and (not (:ended? g)) (actor-live? st P (:actor g)))
                                {:sign-in G :actor (:actor g) :kind (:kind g) :label (:label g)
                                 :begun (:begun g) :until (:until g)}))))
                    vec)}))

(defn oracle-live-under [st P]
  (let [r (get-in st [:roots P])]
    {:sign-ins (into {}
                     (keep (fn [G]
                             (let [g (get-in r [:sign-ins G])]
                               (when (and (not (:ended? g)) (actor-live? st P (:actor g)))
                                 [G (select-keys g [:actor :kind :hash :until :previous])]))))
                     (:sign-in-order r))
     :sessions (into {}
                     (keep (fn [[S s]]
                             (when-not (:closed? s)
                               [S {:sign-in (:sign-in s) :until (:until s)}])))
                     (:sessions r))}))

(defn valid-linearization?
  "Checks whether a set of completed operations `ops` (each `{:id :call [op input]
  :invoked-at-step int :returned-at-step int :result map}`) and optional final
  state predicate `final-match?` admit at least one causal total order
  (`returned-at-step(A) <= invoked-at-step(B) => A before B`) starting from
  `init-oracle-state`."
  [init-oracle-state ops final-match?]
  (let [ops-vec (vec ops)
        n (count ops-vec)
        must-precede (into {}
                           (map (fn [i]
                                  [i (into #{}
                                           (keep (fn [j]
                                                   (when (and (not= i j)
                                                              (<= (:returned-at-step (nth ops-vec j))
                                                                  (:invoked-at-step (nth ops-vec i))))
                                                     j)))
                                           (range n))]))
                           (range n))]
    (letfn [(search [placed-set st]
              (if (= (count placed-set) n)
                (final-match? st)
                (boolean
                 (some (fn [i]
                         (when (and (not (contains? placed-set i))
                                    (every? placed-set (get must-precede i)))
                           (let [op-entry (nth ops-vec i)
                                 [st' expected] (oracle-step st (:call op-entry))]
                             (when (= expected (:result op-entry))
                               (search (conj placed-set i) st')))))
                       (range n)))))]
      (search #{} init-oracle-state))))
