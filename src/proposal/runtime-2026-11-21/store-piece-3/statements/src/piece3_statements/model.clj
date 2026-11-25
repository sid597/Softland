(ns piece3-statements.model
  "Small, self-contained in-memory model of the store's two gates, door, and
  Piece 3's read by stamp, built from `store-piece-3/SPEC.md` (which governs),
  `IMPLICIT_SPEC.md`, and Sid's 32 MiB / 256 KiB call alone.

  Exposes `(make-target)` (and `(make-target hooks)` for `broken_copies.clj`),
  implementing the exact runner interface that `runner.clj` takes for any
  store door + read-by-stamp implementation."
  (:require [piece3-statements.statements :as st])
  (:import [java.util UUID]))

(def store-keys
  {:softland.store/depot             {:setting true  :written-again false}
   :softland.store/permission        {:setting false :written-again false}
   :softland.store/run-permission    {:setting false :written-again false}
   :softland.store/act-permission    {:setting false :written-again false}
   :softland.store/who-may-see       {:setting true  :written-again true}
   :softland.store/builds-on         {:setting true  :written-again true}
   :softland.store/marked-keys       {:setting true  :written-again true}
   :softland.store/who-may-read-acts {:setting true  :written-again true}})

(def permission-keys
  #{:softland.store/permission
    :softland.store/run-permission
    :softland.store/act-permission})

(def max-value-bytes 65536)
(def max-act-bytes 18874368)
(def max-facts 256)

;; ---------------------------------------------- Stray & Declaration checks

(defn valid-act-id? [x]
  (and (vector? x)
       (= 3 (count x))
       (st/usable-id? (nth x 0))
       (contains? #{:streaming :microbatch} (nth x 1))
       (uuid? (nth x 2))))

(defn stray-reason [gate-depot thing]
  (try
    (let [act-id (when (st/plain-map? thing) (get thing :act-id))]
      (cond
        (nil? act-id) :no-act-id
        (not (and (vector? act-id)
                  (= 3 (count act-id))
                  (or (st/readable-keyword? (nth act-id 0)) (uuid? (nth act-id 0)))
                  (contains? #{:streaming :microbatch} (nth act-id 1))
                  (uuid? (nth act-id 2))))
        :act-id-malformed
        (not (<= (count (str (nth act-id 0))) st/max-id-text)) :id-too-long
        (not= gate-depot (nth act-id 1)) :act-id-of-the-other-gate
        (not= (nth act-id 0) (get thing :lane)) :act-id-of-another-lane
        :else nil))
    (catch Throwable _ :no-act-id)))

(defn valid-perm-id?
  ([x] (valid-perm-id? x 1))
  ([x depth]
   (and (<= depth 32)
        (vector? x)
        (case (count x)
          3 (every? st/usable-id? x)
          4 (and (every? st/usable-id? (take 3 x))
                 (valid-perm-id? (nth x 3) (inc depth)))
          5 (and (every? st/usable-id? (take 3 x))
                 (valid-perm-id? (nth x 3) (inc depth))
                 (contains? #{:see :see-and-act} (nth x 4)))
          6 (and (every? st/usable-id? (take 3 x))
                 (valid-perm-id? (nth x 3) (inc depth))
                 (let [allows (nth x 4)
                       cov (nth x 5)]
                   (and (vector? cov)
                        (case (first cov)
                          :run (and (contains? #{:see :see-and-act} allows)
                                    (= 2 (count cov))
                                    (st/usable-id? (second cov)))
                          :act (and (= :see-and-act allows)
                                    (= 3 (count cov))
                                    (uuid? (nth cov 1))
                                    (string? (nth cov 2))
                                    (= 64 (count (nth cov 2))))
                          false))))
          false))))

(defn perm-allows [p]
  (if (>= (count p) 5) (nth p 4) :see-and-act))

(defn perm-covers [p]
  (when (= 6 (count p)) (nth p 5)))

(defn valid-fact-id? [x]
  (and (vector? x)
       (= 2 (count x))
       (valid-act-id? (nth x 0))
       (int? (nth x 1))
       (<= 0 (long (nth x 1)))))

(def allowed-act-keys
  #{:act-id :depot :door-time :version :who :lane :permission :session
    :based-on :because-of :claimed-when :subjects :facts})

(def allowed-fact-keys
  #{:e :k :v :replaces})

(defn check-declaration
  "Checks `thing` (which passed `stray-reason`). Returns `{:ok act}` or
  `{:refuse reason}`."
  [thing]
  (try
    (cond
      (not (st/plain-map? thing)) {:refuse :malformed}
      (some #(not (contains? allowed-act-keys %)) (keys thing)) {:refuse :unknown-part}
      (not (contains? #{:streaming :microbatch} (:depot thing))) {:refuse :depot-not-the-act-ids-depot}
      (not (and (int? (:door-time thing)) (<= 0 (long (:door-time thing))))) {:refuse :no-door-time}
      (not= 1 (:version thing)) {:refuse :unknown-version}
      (not (st/usable-id? (:who thing))) {:refuse :malformed}
      (not (st/usable-id? (:lane thing))) {:refuse :malformed}
      (nil? (:permission thing)) {:refuse :no-permission}
      (not (valid-perm-id? (:permission thing))) {:refuse :malformed}
      (nil? (:session thing)) {:refuse :no-session}
      (not (st/usable-id? (:session thing))) {:refuse :malformed}
      (and (some? (:based-on thing))
           (not (and (st/plain-map? (:based-on thing))
                     (every? (fn [[fid s]] (and (valid-fact-id? fid) (int? s) (<= 0 (long s))))
                             (:based-on thing)))))
      {:refuse :malformed}
      (and (some? (:because-of thing)) (not (valid-act-id? (:because-of thing)))) {:refuse :malformed}
      (and (some? (:claimed-when thing)) (not (int? (:claimed-when thing)))) {:refuse :malformed}
      (and (some? (:subjects thing))
           (not (and (set? (:subjects thing))
                     (<= (count (:subjects thing)) 256)
                     (every? st/usable-id? (:subjects thing)))))
      {:refuse :malformed}
      (not (sequential? (:facts thing))) {:refuse :malformed}
      (empty? (:facts thing)) {:refuse :empty-act}
      (not (every? (fn [f]
                     (and (st/plain-map? f)
                          (every? #(contains? allowed-fact-keys %) (keys f))
                          (st/usable-id? (:e f))
                          (st/readable-keyword? (:k f))
                          (<= (count (str (:k f))) st/max-id-text)
                          (or (nil? (:replaces f)) (valid-fact-id? (:replaces f)))))
                   (:facts thing)))
      {:refuse :malformed}
      ;; Rules
      (not= (:lane thing) (nth (:permission thing) 1)) {:refuse :permission-not-in-lane}
      (not= (:depot thing) (nth (:act-id thing) 1)) {:refuse :depot-not-the-act-ids-depot}
      (> (count (:facts thing)) max-facts) {:refuse :too-many-facts}
      (> (count (or (:based-on thing) {})) 1024) {:refuse :too-many-based-on}
      (some (fn [f] (and (= "softland.store" (namespace (:k f)))
                         (not (contains? store-keys (:k f)))))
            (:facts thing))
      {:refuse :store-key-not-known}
      (some (fn [f] (and (= :softland.store/depot (:k f)) (some? (:v f))
                         (not (contains? #{:streaming :microbatch} (:v f)))))
            (:facts thing))
      {:refuse :depot-fact-not-a-depot-word}
      (some (fn [f] (and (contains? permission-keys (:k f)) (some? (:v f))
                         (not (valid-perm-id? (:v f)))))
            (:facts thing))
      {:refuse :permission-fact-not-a-permission}
      (some (fn [f] (and (contains? permission-keys (:k f)) (some? (:v f))
                         (not= (:k f) (case (first (perm-covers (:v f)))
                                        :run :softland.store/run-permission
                                        :act :softland.store/act-permission
                                        :softland.store/permission))))
            (:facts thing))
      {:refuse :permission-fact-under-another-key}
      (some (fn [f] (and (= :softland.store/permission (:k f)) (some? (:v f))
                         (not= (:e f) (nth (:v f) 0))))
            (:facts thing))
      {:refuse :permission-fact-not-on-its-holder}
      (some (fn [f] (and (contains? #{:softland.store/run-permission :softland.store/act-permission} (:k f))
                         (some? (:v f))
                         (not= (:e f) (second (perm-covers (:v f))))))
            (:facts thing))
      {:refuse :permission-fact-not-on-what-it-covers}
      (some (fn [f] (and (= :softland.store/who-may-see (:k f)) (some? (:v f))
                         (not (contains? #{:members :everyone} (:v f)))))
            (:facts thing))
      {:refuse :who-may-see-fact-not-members-or-everyone}
      (some (fn [f] (and (= :softland.store/who-may-read-acts (:k f)) (some? (:v f))
                         (not (contains? #{:members :everyone} (:v f)))))
            (:facts thing))
      {:refuse :who-may-read-acts-fact-not-members-or-everyone}
      (some (fn [f] (and (= :softland.store/marked-keys (:k f)) (some? (:v f))
                         (not (and (set? (:v f)) (every? st/readable-keyword? (:v f))))))
            (:facts thing))
      {:refuse :marked-keys-fact-not-a-set-of-keys}
      (some (fn [f] (and (= :softland.store/marked-keys (:k f)) (some? (:v f))
                         (> (count (:v f)) 32)))
            (:facts thing))
      {:refuse :too-many-marked-keys}
      (some (fn [f] (and (= :softland.store/marked-keys (:k f)) (some? (:v f))
                         (some #(= "softland.store" (namespace %)) (:v f))))
            (:facts thing))
      {:refuse :marked-key-in-the-stores-namespace}
      (not (apply distinct? (map (juxt :e :k) (:facts thing))))
      {:refuse :place-named-twice}
      (some (fn [f] (= (:act-id thing) (first (:replaces f)))) (:facts thing))
      {:refuse :replaces-own-fact}
      :else
      ;; Value byte limits (`with-texts`)
      (let [who-perm-bytes (+ (st/thing-utf8-bytes (:who thing))
                              (st/thing-utf8-bytes (:permission thing)))
            res (reduce (fn [sum f]
                          (if (nil? (:v f))
                            sum
                            (let [sz (st/thing-utf8-bytes (:v f))
                                  plain? (not (contains? store-keys (:k f)))]
                              (if (and plain? (> sz max-value-bytes))
                                (reduced :value-too-large)
                                (+ sum sz)))))
                        0
                        (:facts thing))]
        (cond
          (= :value-too-large res) {:refuse :value-too-large}
          (> (+ (long res) (* (count (:facts thing)) who-perm-bytes)) max-act-bytes)
          {:refuse :act-too-large}
          :else {:ok thing})))
    (catch Throwable _ {:refuse :malformed})))

;; ---------------------------------------------- Gate standing & places

(defn making-act? [gate-depot act]
  (let [lane (:lane act)
        who (:who act)
        perm [who lane lane]
        facts (vec (:facts act))]
    (and (= gate-depot (st/depot-at-making lane))
         (= perm (:permission act))
         (= 2 (count facts))
         (every? #(nil? (:replaces %)) facts)
         (= {:e lane :k :softland.store/depot :v gate-depot}
            (select-keys (nth facts 0) [:e :k :v]))
         (= {:e who :k :softland.store/permission :v perm}
            (select-keys (nth facts 1) [:e :k :v]))
         (or (not= :base lane) (= :system who)))))

(defn perm-place-id [perm]
  (let [cov (perm-covers perm)]
    (case (first cov)
      :run [:run (second cov)]
      :act [:act (second cov)]
      [:holder (nth perm 0)])))

(defn check-standing [gate-depot lane-state act raw-thing]
  (let [perm (:permission act)
        [ckind cwhat chex] (perm-covers perm)
        p-id (perm-place-id perm)
        cur (get-in lane-state [:perm-places p-id])
        ended-set (get lane-state :ended-perm-places #{})
        made? (:made? lane-state)]
    (cond
      (and cur (= perm (:perm cur)))
      (cond
        (not= (:who act) (nth perm 0)) :who-not-the-permissions-holder
        (and (= :run ckind) (not= cwhat (:session act))) :permission-for-another-run
        (and (= :act ckind) (not= cwhat (nth (:act-id act) 2))) :permission-for-another-act
        (and (= :act ckind)
             (not= chex (st/digest-hex (dissoc raw-thing :permission))))
        :act-not-the-one-permitted
        (not (st/permission-stands? (:perm-places lane-state) perm)) :permission-above-ended
        (= :see (perm-allows perm)) :permission-does-not-allow-acting
        :else nil)

      (or cur (contains? ended-set p-id) made?)
      (cond
        (not= (:who act) (nth perm 0)) :who-not-the-permissions-holder
        (and (nil? cur) (contains? ended-set p-id)) :permission-ended
        :else :permission-does-not-stand)

      (not= gate-depot (st/depot-at-making (:lane act)))
      :lane-belongs-to-the-other-gate

      (making-act? gate-depot act)
      nil

      :else
      :lane-not-made)))

(defn above-in-chain? [upper lower]
  (loop [p (when (>= (count lower) 4) (nth lower 3))]
    (cond
      (nil? p) false
      (= p upper) true
      :else (recur (when (>= (count p) 4) (nth p 3))))))

(defn check-places [gate-depot lane-state act makes-lane?]
  (let [lane (:lane act)
        facts (vec (:facts act))
        facts-map (:facts lane-state {})
        perm-places (:perm-places lane-state {})]
    (or ;; (e) replaces
        (some (fn [f]
                (when-let [rep (:replaces f)]
                  (let [cur (get facts-map [(:e f) (:k f)])]
                    (when-not (and cur (= rep (:fact-id cur)))
                      :replaced-fact-not-current))))
              facts)
        ;; (f) setting on lane entity
        (some (fn [f]
                (when (and (get-in store-keys [(:k f) :setting])
                           (not= (:e f) lane))
                  :setting-not-on-the-lane))
              facts)
        (when-not makes-lane?
          (or ;; (g) permission give shape
              (some (fn [f]
                      (let [given (:v f)]
                        (when (and (contains? permission-keys (:k f)) (some? given))
                          (cond
                            (some? (perm-covers (:permission act))) :only-a-permission-for-the-lane-gives
                            (not= lane (nth given 1)) :permission-fact-for-another-lane
                            (not= lane (nth given 2)) :permission-fact-granted-in-another-lane
                            (not= (:permission act) (nth given 3 nil)) :permission-fact-not-beneath-the-acts-own))))
                    facts)
              ;; who-may-write-fault
              (some (fn [f]
                      (when (and (get-in store-keys [(:k f) :written-again])
                                 (not= 3 (count (:permission act))))
                        :only-the-first-holder-writes-a-setting))
                    facts)
              ;; store-key-fault
              (some (fn [f]
                      (let [k (:k f)
                            retract? (nil? (:v f))]
                        (cond
                          (and (contains? permission-keys k) retract?)
                          (let [p-id (case k
                                       :softland.store/run-permission [:run (:e f)]
                                       :softland.store/act-permission [:act (:e f)]
                                       [:holder (:e f)])
                                held (:perm (get perm-places p-id))
                                own (:permission act)]
                            (cond
                              (nil? held) :no-permission-to-end
                              (= 3 (count held)) :first-permission-does-not-end
                              (or (= held own) (above-in-chain? own held)) nil
                              (and (nil? (perm-covers own)) (= (:who act) (nth held 0))) nil
                              :else :not-above-the-permission))

                          (and (contains? store-keys k) retract?)
                          :store-key-retract-not-built

                          (and (= :softland.store/depot k) (not= gate-depot (:v f)))
                          :changing-a-depot-not-decided

                          (= :softland.store/depot k)
                          :store-key-takes-one-write

                          (contains? permission-keys k)
                          (let [p-id (perm-place-id (:v f))
                                held (:perm (get perm-places p-id))]
                            (when (and held (st/permission-stands? perm-places held))
                              :holder-has-a-permission))

                          :else nil)))
                    facts))))))

(defn empty-lane-state [lane]
  {:lane lane
   :made? false
   :who-may-see (if (= :base lane) :everyone :members)
   :who-may-read-acts :members
   :perm-places {}
   :ended-perm-places #{}
   :facts {}
   :answers {}
   :decisions []})

(defn- apply-accepted-facts [lane-state act stamp]
  (let [lane (:lane act)
        act-id (:act-id act)]
    (reduce (fn [ls [idx f]]
              (let [e (:e f)
                    k (:k f)
                    v (:v f)
                    ls1 (assoc-in ls [:facts [e k]] {:v v :fact-id [act-id idx] :stamp stamp :who (:who act)})]
                (cond
                  (= k :softland.store/depot)
                  (assoc ls1 :made? true)

                  (= k :softland.store/who-may-see)
                  (assoc ls1 :who-may-see v)

                  (= k :softland.store/who-may-read-acts)
                  (assoc ls1 :who-may-read-acts v)

                  (contains? permission-keys k)
                  (let [p-id (case k
                               :softland.store/run-permission [:run e]
                               :softland.store/act-permission [:act e]
                               [:holder e])]
                    (if (nil? v)
                      (-> ls1
                          (update :perm-places dissoc p-id)
                          (update :ended-perm-places conj p-id))
                      (-> ls1
                          (assoc-in [:perm-places p-id] {:perm v :stamp stamp})
                          (update :ended-perm-places disj p-id))))

                  :else ls1)))
            lane-state
            (map-indexed vector (:facts act)))))

;; ---------------------------------------------- Model instance & hooks

(defn make-target
  "Creates a fresh in-memory store + Piece-3 target. Optional `hooks` map is
  used by `broken_copies.clj` to introduce targeted faults without changing
  `statements.clj`."
  ([] (make-target {}))
  ([hooks]
   (let [default-acts-setting (get hooks :default-acts-setting-fn (constantly :members))
         state (atom {:gates {:streaming {} :microbatch {}}
                      :strays {:streaming #{} :microbatch #{}}
                      :clock 1700000000000})
         ensure-lane (fn [st gate lane]
                        (if (get-in st [:gates gate lane])
                          st
                          (assoc-in st [:gates gate lane]
                                    (assoc (empty-lane-state lane)
                                           :who-may-read-acts (default-acts-setting lane)))))
         append-to-gate!
         (fn [gate-depot raw-thing]
           (swap! state
                  (fn [st]
                    (let [stray (stray-reason gate-depot raw-thing)]
                      (if (and stray (not (:stray-gets-stamp? hooks)))
                        (update-in st [:strays gate-depot] conj (st/digest-hex raw-thing))
                        (let [act-id (if stray
                                       [(or (and (st/plain-map? raw-thing) (:lane raw-thing)) :stray-lane)
                                        gate-depot
                                        (UUID/randomUUID)]
                                       (:act-id raw-thing))
                              [lane _ uuid] act-id
                              st1 (ensure-lane st gate-depot lane)
                              ls (get-in st1 [:gates gate-depot lane])]
                          (if-let [prev-ans (get-in ls [:answers uuid])]
                            (if (:resend-overwrites-thing? hooks)
                              (let [s (:stamp prev-ans)]
                                (assoc-in st1 [:gates gate-depot lane :decisions s :thing] raw-thing))
                              st1)
                            (let [stamp (count (:decisions ls))
                                  t (st/usable-time raw-thing)
                                  dig (st/digest-hex raw-thing)
                                  t-bytes (st/thing-utf8-bytes raw-thing)
                                  raw-who (st/raw-usable-who raw-thing)
                                  decl (if stray {:refuse stray} (check-declaration raw-thing))
                                  [verdict reason stage ls-after]
                                  (if (:refuse decl)
                                    [:refused (:refuse decl) :declaration ls]
                                    (let [act (:ok decl)
                                          ;; Hook: allow non-first holder to write acts setting
                                          ls-for-check ls
                                          s-fault (check-standing gate-depot ls-for-check act raw-thing)]
                                      (if s-fault
                                        [:refused s-fault :standing ls]
                                        (let [makes? (and (not (:made? ls)) (making-act? gate-depot act))
                                              p-fault (let [f (check-places gate-depot ls-for-check act makes?)]
                                                        (if (and (= f :only-the-first-holder-writes-a-setting)
                                                                 (:given-holder-writes-acts-setting? hooks)
                                                                 (every? #(or (not (get-in store-keys [(:k %) :written-again]))
                                                                              (= :softland.store/who-may-read-acts (:k %)))
                                                                         (:facts act)))
                                                          nil
                                                          f))]
                                          (if p-fault
                                            [:refused p-fault :places ls]
                                            [:accepted nil nil
                                             (if (:acts-setting-non-retroactive? hooks)
                                               (apply-accepted-facts ls act stamp)
                                               (apply-accepted-facts ls act stamp))])))))
                                  kept? (if-let [keep-fn (:kept-whole-fn hooks)]
                                          (keep-fn {:verdict verdict :refused-stage stage :thing-bytes t-bytes})
                                          (st/kept-whole-by-gate? {:verdict verdict :refused-stage stage :thing-bytes t-bytes}))
                                  ans {:verdict verdict
                                       :reason reason
                                       :stamp stamp
                                       :time t
                                       :digest dig}
                                  decision {:stamp stamp
                                            :act-id act-id
                                            :answer ans
                                            :thing (when kept? raw-thing)
                                            :thing-bytes t-bytes
                                            :raw-who raw-who
                                            :refused-stage stage
                                            :kept-whole? kept?}
                                  ls-final (-> ls-after
                                               (assoc-in [:answers uuid] ans)
                                               (update :decisions conj decision))]
                              (assoc-in st1 [:gates gate-depot lane] ls-final)))))))))
         next-time! (fn [] (:clock (swap! state update :clock + 10)))]
     {:state state

      :act-id-for
      (fn [lane]
        (if-not (st/usable-id? lane)
          {:could-not :make-an-act-id :at :lane}
          [lane (st/depot-at-making lane) (UUID/randomUUID)]))

      :append-raw!
      (fn [depot raw-thing]
        (append-to-gate! depot raw-thing)
        {:ok true})

      :send-act!
      (fn [maker-act act-id]
        (cond
          (not (st/plain-map? maker-act)) {:could-not :read-the-act}
          (some #(contains? maker-act %) [:act-id :depot :door-time]) {:could-not :add-its-parts}
          (not (st/usable-id? (:lane maker-act))) {:could-not :make-an-act-id :at :lane}
          :else
          (let [aid (or act-id [(:lane maker-act) (st/depot-at-making (:lane maker-act)) (UUID/randomUUID)])
                depot (nth aid 1)
                thing (assoc maker-act :act-id aid :depot depot :door-time (next-time!))
                dig (st/digest-hex thing)]
            (append-to-gate! depot thing)
            {:act-id aid :digest dig :ack (if (= :microbatch depot) :depot-holds :gate-ran)})))

      :answer-of
      (fn [act-id digest]
        (if-not (valid-act-id? act-id)
          {:answer :nothing-yet}
          (let [[lane depot uuid] act-id
                ans (get-in @state [:gates depot lane :answers uuid])]
            (cond
              (nil? ans) {:answer :nothing-yet}
              (not= digest (:digest ans)) {:answer :not-mine}
              (= :accepted (:verdict ans))
              (cond-> {:answer :accepted :stamp (:stamp ans)}
                (some? (:time ans)) (assoc :time (:time ans)))
              :else
              (cond-> {:answer :refused :reason (:reason ans) :stamp (:stamp ans)}
                (some? (:time ans)) (assoc :time (:time ans)))))))

      :read-line
      (fn [{:keys [lane start max-count max-bytes asker not-signed-in session] :as req}]
        (if-not (st/valid-read-request? req)
          {:could-not :read-the-query}
          (let [own-gate (st/depot-at-making lane)
                other-gate (if (= :microbatch own-gate) :streaming :microbatch)
                ls0 (or (get-in @state [:gates own-gate lane])
                        (assoc (empty-lane-state lane)
                               :who-may-read-acts (default-acts-setting lane)))
                ls (if (:other-gate-mixed-in? hooks)
                     (let [other-decs (get-in @state [:gates other-gate lane :decisions] [])]
                       (update ls0 :decisions
                               (fn [ds]
                                 (vec (map-indexed (fn [i d]
                                                     (-> d
                                                         (assoc :stamp i)
                                                         (assoc-in [:answer :stamp] i)))
                                                   (concat ds other-decs))))))
                     ls0)
                role (cond
                       (not (:made? ls)) :not-made

                       ;; Hook: act-permission wrongly grants holder reading
                       (and (:act-permission-grants-read? hooks)
                            (some? asker)
                            (some (fn [[[kind _] {:keys [perm]}]]
                                    (and (= :act kind) (= asker (nth perm 0))))
                                  (:perm-places ls)))
                       :all-whole

                       ;; Hook: open lane skips reading asker's permission
                       (and (:open-lane-skips-permission? hooks)
                            (= :everyone (:who-may-see ls)))
                       (if (= :everyone (:who-may-read-acts ls)) :all-whole :visitor-own)

                       ;; Hook: check only own row without checking chain above
                       (and (:ended-chain-still-reads-whole? hooks)
                            (some? asker)
                            (contains? (:perm-places ls) [:holder asker]))
                       :all-whole

                       :else (st/asker-role ls req))
                eff-max-bytes (long (or max-bytes (* 64 1024 1024)))
                decisions (:decisions ls)
                true-last (when (seq decisions) (dec (count decisions)))]
            (if (and (contains? #{:not-made :may-not-see} role)
                     (:closed-lane-hides-own-things? hooks))
              {:left-out role :last-stamp nil :next-stamp nil :entries []}
              (let [candidates
                    (->> decisions
                         (drop start)
                         (keep (fn [d]
                                 (cond
                                   ;; Hook: visitor reads taken acts whole even under :members
                                   (and (= :visitor-own role)
                                        (:visitor-reads-taken-act-whole? hooks)
                                        (= :accepted (get-in d [:answer :verdict])))
                                   (st/expected-entry-for :all-whole asker eff-max-bytes d)

                                   ;; Hook: closed lane leaks withheld stamps of others
                                   (and (= :may-not-see role)
                                        (:closed-lane-leaks-stamps? hooks))
                                   (or (st/expected-entry-for role asker eff-max-bytes d)
                                       {:stamp (:stamp d) :status :withheld})

                                   :else
                                   (let [e (st/expected-entry-for role asker eff-max-bytes d)]
                                     (cond
                                       (and (= :withheld (:status e)) (:withheld-leaks-answer? hooks))
                                       (assoc e :act-id (:act-id d) :answer (:answer d))

                                       (and (= :too-large (:status e)) (:too-large-marked-withheld? hooks))
                                       {:stamp (:stamp d) :status :withheld}

                                       :else e))))))
                    ;; Accumulate up to `max-count` and `eff-max-bytes` across multiple whole entries
                    [page-entries _]
                    (reduce (fn [[acc used-bytes] e]
                              (if (>= (count acc) max-count)
                                (reduced [acc used-bytes])
                                (if (and (= :too-large (:status e))
                                         (:too-large-stops-line? hooks))
                                  (reduced [acc used-bytes])
                                  (let [eb (if (= :whole (:status e))
                                             (:thing-bytes (nth decisions (:stamp e)))
                                             0)]
                                    (if (and (seq acc) (> (+ used-bytes eb) eff-max-bytes))
                                      (reduced [acc used-bytes])
                                      [(conj acc e) (+ used-bytes eb)])))))
                            [[] 0]
                            candidates)
                    next-s (cond
                             (seq page-entries) (inc (:stamp (last page-entries)))
                             (contains? #{:all-whole :visitor-own} role) (long start)
                             :else nil)]
                {:left-out (when (contains? #{:not-made :may-not-see} role) role)
                 :last-stamp (if (contains? #{:not-made :may-not-see} role)
                               (when (:closed-lane-leaks-stamps? hooks) true-last)
                               true-last)
                 :next-stamp next-s
                 :entries page-entries})))))})))
