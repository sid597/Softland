(ns runtime.model.core
  "Pure executable state-machine model of the Softland runtime seam across
   two stateless runtime JVM copies (:c0, :c1) and three Rama modules
   (Store, Auth, Trails).

   No Rama, no network, no disk. Encodes the open seams of
   src/proposal/runtime-2026-11-21/PROPOSAL.md as explicit configuration
   readings so test.check can falsify underspecified designs and shrink
   violations to minimal traces.")

(def max-facts-per-act
  "Small cap in the toy so multi-act branch promotions happen constantly."
  2)

(def presence-lease-ticks
  "Ticks before an un-refreshed presence entry expires from who-is-here-now."
  3)

(def baseline-config
  {;; R1: Permission identity when ending and giving again (Piece 1)
   ;; :content-tuple -> [who layer granted-in parent allows] (collides on re-give)
   ;; :grant-epoch   -> [who layer granted-in parent {:allows ... :grant-id g :scope :layer :span :until-ended}]
   :perm-id :grant-epoch

   ;; R1b: Who can write layer settings (:who-may-see, :builds-on, :marked-keys)
   ;; :any-actor   -> any holder with :see-and-act
   ;; :root-only   -> only the layer's root holder
   :settings-guard :root-only

   ;; R2: Sign-in & agent bootstrap across Auth + Store under crashes/races (Piece 4)
   ;; :uncoordinated           -> Auth link written first; Store layer created with fresh ids or skipped if link exists
   ;; :deterministic-resumable -> Auth atomically records {:person :own-layer :boot-uuid :ready? false};
   ;;                             any copy replays deterministic Store bootstrap before issuing active session
   :two-module-bootstrap :deterministic-resumable

   ;; R3: Maker act-id on the wire across retries & depot re-classing (Entry 7)
   ;; :maker-pins-act-id         -> maker pins [layer known-depot uuid] when creating the intent and re-sends that exact act-id on retry
   ;; :client-uuid-runtime-depot -> maker sends bare :act-uuid and runtime fills current depot on each try (SUBTLE BUG on reclass + retry!)
   ;; :runtime-mints-random      -> runtime mints a new UUID on each HTTP/WS call (20 Nov bug)
   :act-wire-id :maker-pins-act-id

   ;; R4: Live watch revocation across Store + Auth + Trails on multiple copies (Pieces 1, 4, 5)
   ;; :check-at-open-only         -> standing & permission checked only when watch opens
   ;; :reactive-standing-and-perm -> watch joins Auth standing + target clearance; closes before post-end pushes
   :watch-standing :reactive-standing-and-perm

   ;; R5: Branch promote under never-relay & max-facts-per-act (Piece 8)
   ;; :naive-overwrite   -> single-act cap, no :replaces guard, or relays agent :who
   ;; :chunked-replaces  -> promoter is :who, carries :promoted-from provenance, sets :replaces against target base head,
   ;;                       and chunks deterministically [promo-id idx] with a commit marker when |facts| > max-facts-per-act
   :promote-form :chunked-replaces})

(def naive-config
  {:perm-id               :content-tuple
   :settings-guard        :any-actor
   :two-module-bootstrap  :uncoordinated
   :act-wire-id           :runtime-mints-random
   :watch-standing        :check-at-open-only
   :promote-form          :naive-overwrite})

(defn initial-state [config]
  {:config config
   :tick 0
   :next-id 1
   ;; Two stateless runtime JVM copies; all they hold in-memory are open watch handles
   :copies {:c0 {:watches {}}
            :c1 {:watches {}}}
   ;; Module 1: Auth
   :auth {:accounts {}          ;; [provider sub] -> {:person :own-layer :boot-uuid :ready? ...}
          :pending-signins []   ;; half-completed sign-ins interrupted before Store bootstrap
          :sessions {}}         ;; sid -> {:sid :person :actor :secret-ver :active? :created-at}
   ;; Module 2: Store
   :store {:layers {}}          ;; lid -> {:lid :depot :root :settings :stamp :answers :acts :perms :ended-perms :heads}
   ;; Module 3: Trails
   :trails {:by-session {}      ;; sid -> [{:seq :tick :kind ...}]
            :presence {}}       ;; lid -> {sid {:actor :tick :lease-until}}
   ;; History log for property checking (the ruler, never read by gates)
   :history {:signins []
             :wire-sends []     ;; logical maker intents {:intent-id :act-uuid :act :Session ... :responses [...]}
             :promotes []
             :trail-reads []}})

(defn- alloc-id [st prefix]
  (let [n (:next-id st)]
    [(update st :next-id inc) (keyword (str (name prefix) "-" n))]))

(defn- bump-tick [st]
  (update st :tick inc))

;; --------------------------------------------------------- permissions & store

(defn perm-parent [pid]
  (when (and (vector? pid) (>= (count pid) 4))
    (nth pid 3)))

(defn perm-holder [pid]
  (nth pid 0))

(defn perm-layer [pid]
  (nth pid 1))

(defn perm-allows [pid]
  (if (>= (count pid) 5)
    (nth pid 4)
    :see-and-act))

(defn make-perm-id
  "Uses Comparable vector tuples so permission IDs never throw ClassCastException
   in sorted sets or Rama sorted PState subindexes."
  [cfg who layer granted-in parent allows grant-id]
  (if (nil? parent)
    [who layer granted-in]
    (case (:perm-id cfg)
      :content-tuple [who layer granted-in parent allows]
      :grant-epoch   [who layer granted-in parent allows grant-id :layer :until-ended])))

(defn perm-ancestors
  "Returns [pid parent grandparent ... root]."
  [pid]
  (loop [cur pid acc []]
    (if (nil? cur)
      acc
      (recur (perm-parent cur) (conj acc cur)))))

(defn perm-standing?
  "A permission stands in a layer iff it was granted in that layer and neither
   it nor any ancestor in its chain is in :ended-perms."
  [layer-map pid]
  (and (= (:lid layer-map) (perm-layer pid))
       (contains? (:perms layer-map) pid)
       (not-any? #(contains? (:ended-perms layer-map) %) (perm-ancestors pid))))

(defn actor-may-see-layer? [st lid actor]
  (when-let [lm (get-in st [:store :layers lid])]
    (or (= :anyone (get-in lm [:settings :who-may-see]))
        (some (fn [pid]
                (and (= actor (perm-holder pid))
                     (perm-standing? lm pid)))
              (keys (:perms lm))))))

(defn actor-may-see-trail? [st target-sid actor]
  (when-let [sess (get-in st [:auth :sessions target-sid])]
    ;; A session's trail may be read by its own actor or the root person who owns that session/agent
    (or (= actor (:actor sess))
        (= actor (:person sess)))))

(defn watch-permitted? [st session-id target]
  (let [sess (get-in st [:auth :sessions session-id])]
    (and sess
         (:active? sess)
         (let [[kind id] target
               actor (:actor sess)]
           (case kind
             :layer (boolean (actor-may-see-layer? st id actor))
             :trail (boolean (actor-may-see-trail? st id actor))
             false)))))

(defn- sync-reactive-watches
  "Under :reactive-standing-and-perm, any watch whose session ended or whose
   target clearance ended closes immediately on all runtime copies before new
   events are pushed."
  [st]
  (if (not= :reactive-standing-and-perm (get-in st [:config :watch-standing]))
    st
    (reduce (fn [s copy-id]
              (update-in s [:copies copy-id :watches]
                         (fn [ws]
                           (into {}
                                 (map (fn [[wid w]]
                                        [wid (if (or (:closed? w)
                                                     (not (watch-permitted? s (:session w) (:target w))))
                                               (assoc w :closed? true)
                                               w)]))
                                 ws))))
            st
            [:c0 :c1])))

(defn- notify-watches [st target event]
  (let [st (sync-reactive-watches st)]
    (reduce (fn [s copy-id]
              (update-in s [:copies copy-id :watches]
                         (fn [ws]
                           (into {}
                                 (map (fn [[wid w]]
                                        [wid (if (and (not (:closed? w))
                                                      (= target (:target w)))
                                               (update w :delivered conj event)
                                               w)]))
                                 ws))))
            st
            [:c0 :c1])))

(defn- append-trail [st sid entry]
  (let [st (bump-tick st)
        seq-no (inc (count (get-in st [:trails :by-session sid] [])))
        item (assoc entry :seq seq-no :tick (:tick st))]
    (-> st
        (update-in [:trails :by-session sid] (fnil conj []) item)
        (notify-watches [:trail sid] item))))

;; --------------------------------------------------------- store gate logic

(def ^:private setting-keys
  #{:softland.store/who-may-see
    :softland.store/builds-on
    :softland.store/marked-keys})

(defn- store-gate-apply
  "Applies a door-appended act [act-id digest act] to the Store module."
  [st act-id digest act]
  (let [[lid depot _uuid] act-id
        st (bump-tick st)
        tick (:tick st)
        existing-lm (get-in st [:store :layers lid])]
    (cond
      ;; Duplicate delivery of an already-decided act-id
      (and existing-lm (contains? (:answers existing-lm) act-id))
      (let [prev (get-in existing-lm [:answers act-id])]
        (if (= digest (:digest prev))
          [st (dissoc prev :digest)]
          [st {:answer :not-mine}]))

      ;; First act making a new layer
      (nil? existing-lm)
      (let [depot-fact (first (filter #(= :softland.store/depot (:k %)) (:facts act)))
            perm-fact  (first (filter #(= :softland.store/permission (:k %)) (:facts act)))
            declared-depot (or (:v depot-fact) :streaming)]
        (if (or (not= depot declared-depot) (nil? perm-fact))
          [st {:answer :refused :reason :invalid-first-act :stamp 1}]
          (let [root-pid (:v perm-fact)
                root-holder (perm-holder root-pid)
                builds-fact (first (filter #(= :softland.store/builds-on (:k %)) (:facts act)))
                ans {:answer :accepted :stamp 1 :digest digest :tick tick}
                stored-act (assoc act :act-id act-id :stamp 1 :answer :accepted :digest digest)
                lm {:lid lid
                    :depot declared-depot
                    :root root-holder
                    :settings {:who-may-see :holders
                               :builds-on (:v builds-fact)
                               :marked-keys #{}}
                    :stamp 1
                    :answers {act-id ans}
                    :acts [stored-act]
                    :perms {root-pid {:granted-at 1 :by (:who act)}}
                    :ended-perms #{}
                    :heads {}}]
            [(-> st
                 (assoc-in [:store :layers lid] lm)
                 (notify-watches [:layer lid] {:stamp 1 :tick tick :act-id act-id}))
             (dissoc ans :digest)])))

      ;; Existing layer: check depot match first
      (not= depot (:depot existing-lm))
      (let [stamp (inc (:stamp existing-lm))
            ans {:answer :refused :reason :wrong-depot :stamp stamp :digest digest :tick tick}
            stored-act (assoc act :act-id act-id :stamp stamp :answer :refused :reason :wrong-depot :digest digest)]
        [(-> st
             (assoc-in [:store :layers lid :stamp] stamp)
             (assoc-in [:store :layers lid :answers act-id] ans)
             (update-in [:store :layers lid :acts] conj stored-act))
         (dissoc ans :digest)])

      ;; Existing layer: check permission, settings guard, end-permission rules, and :replaces
      :else
      (let [stamp (inc (:stamp existing-lm))
            pid (:permission act)
            who (:who act)
            facts (:facts act)
            touches-settings? (some #(contains? setting-keys (:k %)) facts)
            settings-fault? (and touches-settings?
                                 (= :root-only (get-in st [:config :settings-guard]))
                                 (not= who (:root existing-lm)))
            perm-ok? (and (= who (perm-holder pid))
                          (perm-standing? existing-lm pid)
                          (= :see-and-act (perm-allows pid)))
            ;; Check :end-permission facts: giver or ancestor up to root may end
            end-facts (filter #(= :softland.store/end-permission (:k %)) facts)
            end-fault? (some (fn [ef]
                               (let [target-pid (:v ef)]
                                 (or (not (contains? (:perms existing-lm) target-pid))
                                     (not (or (= who (:root existing-lm))
                                              (some #(= who (perm-holder %)) (rest (perm-ancestors target-pid))))))))
                             end-facts)
            ;; Check :replaces conflicts against current heads (with overlay fallback to builds-on base head)
            base-lid (get-in existing-lm [:settings :builds-on])
            base-heads (when base-lid (get-in st [:store :layers base-lid :heads]))
            replaces-fault? (some (fn [f]
                                    (when (contains? f :expected-head)
                                      (let [cur-head (or (get-in existing-lm [:heads [(:e f) (:k f)]])
                                                         (when base-heads (get base-heads [(:e f) (:k f)])))
                                            cur-id (:fact-id cur-head)]
                                        (not= (:replaces f) cur-id))))
                                  facts)
            size-fault? (> (count facts) max-facts-per-act)
            reason (cond
                     (not perm-ok?)    :Ended-or-invalid-permission
                     settings-fault?   :root-only-settings
                     end-fault?        :cannot-end-permission
                     size-fault?       :too-many-facts
                     replaces-fault?   :stale-replaces
                     :else             nil)]
        (if reason
          (let [ans {:answer :refused :reason reason :stamp stamp :digest digest :tick tick}
                stored-act (assoc act :act-id act-id :stamp stamp :answer :refused :reason reason :digest digest)]
            [(-> st
                 (assoc-in [:store :layers lid :stamp] stamp)
                 (assoc-in [:store :layers lid :answers act-id] ans)
                 (update-in [:store :layers lid :acts] conj stored-act))
             (dissoc ans :digest)])
          ;; Accepted! Apply permission grants, permission ends, settings, and normal facts
          (let [ans {:answer :accepted :stamp stamp :digest digest :tick tick}
                stored-act (assoc act :act-id act-id :stamp stamp :answer :accepted :digest digest)
                lm' (reduce
                     (fn [m [idx f]]
                       (let [fid [act-id idx]]
                         (case (:k f)
                           :softland.store/permission
                           (let [new-pid (:v f)]
                             (if (= :content-tuple (get-in st [:config :perm-id]))
                               ;; Under naive :content-tuple, re-giving tries to clear :ended-perms for new-pid
                               (-> m
                                   (assoc-in [:perms new-pid] {:granted-at stamp :by who})
                                   (update :ended-perms disj new-pid))
                               (assoc-in m [:perms new-pid] {:granted-at stamp :by who})))

                           :softland.store/end-permission
                           (update m :ended-perms conj (:v f))

                           :softland.store/who-may-see
                           (assoc-in m [:settings :who-may-see] (:v f))

                           :softland.store/builds-on
                           (assoc-in m [:settings :builds-on] (:v f))

                           :softland.store/marked-keys
                           (assoc-in m [:settings :marked-keys] (:v f))

                           ;; Ordinary or provenance fact
                           (assoc-in m [:heads [(:e f) (:k f)]]
                                     {:fact-id fid :e (:e f) :k (:k f) :v (:v f)
                                      :who who :stamp stamp :act-id act-id
                                      :replaces (:replaces f)
                                      :promoted-from (:promoted-from f)}))))
                     (-> existing-lm
                         (assoc :stamp stamp)
                         (assoc-in [:answers act-id] ans)
                         (update :acts conj stored-act))
                     (map-indexed vector facts))
                st' (-> st
                        (assoc-in [:store :layers lid] lm')
                        (sync-reactive-watches)
                        (notify-watches [:layer lid] {:stamp stamp :tick tick :act-id act-id}))]
            [st' (dissoc ans :digest)]))))))

;; --------------------------------------------------------- runtime operations

(defn op-crash-copy
  "Simulates kill -9 of a runtime JVM copy: drops all in-process memory on that copy."
  [st copy-id]
  (assoc-in st [:copies copy-id] {:watches {}}))

(defn- complete-store-bootstrap [st acct]
  (let [{:keys [person own-layer boot-uuid]} acct
        root-pid [person own-layer own-layer]
        boot-act {:version 1
                  :who :system
                  :layer own-layer
                  :permission root-pid
                  :session :system-boot
                  :facts [{:e own-layer :k :softland.store/depot :v :streaming}
                          {:e person :k :softland.store/permission :v root-pid}]}
        act-id [own-layer :streaming boot-uuid]
        digest [:boot own-layer boot-uuid]
        [st' ans] (store-gate-apply st act-id digest boot-act)]
    [st' ans]))

(defn op-sign-in
  "Simulates a first or repeat sign-in on runtime `copy-id` for `[provider sub]`.
   If `crash-between?` is true, the runtime JVM crashes right after writing Auth
   and before talking to Store!"
  [st copy-id subject crash-between?]
  (let [cfg (:config st)
        acct-key [:google subject]]
    (case (:two-module-bootstrap cfg)
      :uncoordinated
      (let [existing (get-in st [:auth :accounts acct-key])]
        (if existing
          ;; Bug in uncoordinated two-module sign-in: Auth row already exists from crashed attempt,
          ;; so retry assumes Store layer already exists and immediately mints an active session!
          (let [[st sid] (alloc-id st :sess)
                st (bump-tick st)
                sess {:sid sid :person (:person existing) :actor (:person existing)
                      :secret-ver 1 :active? true :created-at (:tick st)}]
            (-> st
                (assoc-in [:auth :sessions sid] sess)
                (update-in [:history :signins] conj {:subject subject :copy copy-id :sid sid :person (:person existing)})))
          (let [[st person] (alloc-id st :person)
                [st own-layer] (alloc-id st :layer)
                [st boot-uuid] (alloc-id st :boot)
                acct {:person person :own-layer own-layer :boot-uuid boot-uuid :ready? true}
                st (assoc-in st [:auth :accounts acct-key] acct)]
            (if crash-between?
              (op-crash-copy st copy-id)
              (let [[st _] (complete-store-bootstrap st acct)
                    [st sid] (alloc-id st :sess)
                    st (bump-tick st)
                    sess {:sid sid :person person :actor person :secret-ver 1 :active? true :created-at (:tick st)}]
                (-> st
                    (assoc-in [:auth :sessions sid] sess)
                    (update-in [:history :signins] conj {:subject subject :copy copy-id :sid sid :person person})))))))

      :deterministic-resumable
      (let [[st existing-or-new]
            (if-let [acct (get-in st [:auth :accounts acct-key])]
              [st acct]
              (let [[st person] (alloc-id st :person)
                    [st own-layer] (alloc-id st :layer)
                    [st boot-uuid] (alloc-id st :boot)
                    acct {:person person :own-layer own-layer :boot-uuid boot-uuid :ready? false}]
                [(assoc-in st [:auth :accounts acct-key] acct) acct]))]
        (if crash-between?
          ;; Copy crashes before Store bootstrap completes — notice no active session is issued!
          (op-crash-copy st copy-id)
          (let [[st _ans] (if (:ready? existing-or-new)
                            [st {:answer :accepted}]
                            (complete-store-bootstrap st existing-or-new))
                st (assoc-in st [:auth :accounts acct-key :ready?] true)
                [st sid] (alloc-id st :sess)
                st (bump-tick st)
                person (:person existing-or-new)
                sess {:sid sid :person person :actor person :secret-ver 1 :active? true :created-at (:tick st)}]
            (-> st
                (assoc-in [:auth :sessions sid] sess)
                (update-in [:history :signins] conj {:subject subject :copy copy-id :sid sid :person person}))))))))

(defn op-rotate-secret
  "Rotates short-lived credential secret beneath stable :session id (Entry 7)."
  [st sid]
  (if (get-in st [:auth :sessions sid])
    (update-in st [:auth :sessions sid :secret-ver] inc)
    st))

(defn op-end-session [st sid]
  (if (get-in st [:auth :sessions sid])
    (-> st
        (assoc-in [:auth :sessions sid :active?] false)
        (sync-reactive-watches))
    st))

(defn- send-maker-act-via-copy
  "Runtime seam check + door append + trail append.
   `pinned-act-id` is [layer depot uuid] made when the maker created the intent."
  [st _copy-id sid pinned-act-id act-uuid maker-act]
  (let [sess (get-in st [:auth :sessions sid])]
    (cond
      (or (nil? sess) (not (:active? sess)))
      [st {:answer :refused :reason :unauthenticated}]

      ;; Entry 2: Never-relay! Runtime refuses any act whose :who != caller's actor or :session != sid
      (or (not= (:who maker-act) (:actor sess))
          (not= (:session maker-act) sid))
      [st {:answer :refused :reason :who-or-session-mismatch}]

      :else
      (let [cfg (:config st)
            lid (:layer maker-act)
            actual-depot (get-in st [:store :layers lid :depot] :streaming)
            [st wire-act-id]
            (case (:act-wire-id cfg)
              :maker-pins-act-id
              [st pinned-act-id]

              :client-uuid-runtime-depot
              [st [lid actual-depot act-uuid]]

              :runtime-mints-random
              (let [[st2 fresh-u] (alloc-id st :rand-u)]
                [st2 [lid actual-depot fresh-u]]))
            ;; Digest depends strictly on maker-act + wire-act-id (leaves door clock out, per door.clj:579)
            digest [wire-act-id (dissoc maker-act :door-time)]
            [st ans] (store-gate-apply st wire-act-id digest maker-act)
            st (append-trail st sid {:kind :act :act-id wire-act-id :answer (:answer ans)})]
        [st (assoc ans :act-id wire-act-id)]))))

(defn- send-internal-maker-act
  "Helper for single-shot maker acts inside attach/grant/end/promote."
  [st copy-id sid act-u act]
  (let [lid (:layer act)
        depot (get-in st [:store :layers lid :depot] :streaming)]
    (send-maker-act-via-copy st copy-id sid [lid depot act-u] act-u act)))

(defn- visible-head-fact-id [st lid e k]
  (let [lm (get-in st [:store :layers lid])
        base-lid (get-in lm [:settings :builds-on])]
    (or (get-in lm [:heads [e k] :fact-id])
        (when base-lid (get-in st [:store :layers base-lid :heads [e k] :fact-id])))))

(defn op-send-note
  "Maker creates an act-id `[layer known-depot act-uuid]` and sends a note fact in 1-RT,
   recording logical intent so op-retry-intent can replay it across copies/reclasses."
  [st copy-id sid layer pid entity value spoof-who?]
  (if-let [sess (get-in st [:auth :sessions sid])]
    (let [[st intent-id] (alloc-id st :intent)
          [st act-uuid]  (alloc-id st :act-u)
          known-depot    (get-in st [:store :layers layer :depot] :streaming)
          pinned-act-id  [layer known-depot act-uuid]
          who (if spoof-who? :spoofed-other (:actor sess))
          head-fid (visible-head-fact-id st layer entity :note)
          act {:version 1
               :who who
               :layer layer
               :permission pid
               :session sid
               :facts [(cond-> {:e entity :k :note :v value}
                         head-fid (assoc :replaces head-fid))]}
          [st resp] (send-maker-act-via-copy st copy-id sid pinned-act-id act-uuid act)
          intent {:intent-id intent-id :sid sid :pinned-act-id pinned-act-id :act-uuid act-uuid
                  :act act :spoof? spoof-who? :responses [resp]}]
      (update-in st [:history :wire-sends] conj intent))
    st))

(defn op-retry-intent
  "Retries a previously sent maker intent (e.g. after network timeout or secret rotation) on `copy-id`."
  [st copy-id intent-idx]
  (let [sends (get-in st [:history :wire-sends])]
    (if (empty? sends)
      st
      (let [idx (mod intent-idx (count sends))
            intent (nth sends idx)
            [st resp] (send-maker-act-via-copy st copy-id (:sid intent) (:pinned-act-id intent) (:act-uuid intent) (:act intent))]
        (assoc-in st [:history :wire-sends idx :responses] (conj (:responses intent) resp))))))

(defn op-reclass-layer
  "Re-classes a layer from :streaming to :microbatch."
  [st lid]
  (if (get-in st [:store :layers lid])
    (assoc-in st [:store :layers lid :depot] :microbatch)
    st))

(defn op-attach-agent
  "Person attaches an agent: creates agent identity + session + branch layer building on `base-lid`,
   and optionally grants agent permission in `base-lid` or branch layer."
  [st copy-id owner-sid base-lid in-shared-layer?]
  (let [owner-sess (get-in st [:auth :sessions owner-sid])
        base-lm (get-in st [:store :layers base-lid])]
    (if (or (nil? owner-sess) (not (:active? owner-sess)) (nil? base-lm))
      st
      (let [owner (:actor owner-sess)
            owner-pid (first (filter #(and (= owner (perm-holder %))
                                           (perm-standing? base-lm %)
                                           (= :see-and-act (perm-allows %)))
                                     (keys (:perms base-lm))))]
        (if (nil? owner-pid)
          st
          (let [[st agent-id] (alloc-id st :agent)
                [st agent-sid] (alloc-id st :asess)
                [st grant-id] (alloc-id st :grant)
                st (bump-tick st)
                sess {:sid agent-sid :person (:person owner-sess) :actor agent-id
                      :secret-ver 1 :active? true :created-at (:tick st)}
                st (assoc-in st [:auth :sessions agent-sid] sess)]
            (if in-shared-layer?
              ;; Grant agent directly inside base-lid under owner-pid
              (let [agent-pid (make-perm-id (:config st) agent-id base-lid base-lid owner-pid :see-and-act grant-id)
                    [st act-u] (alloc-id st :act-u)
                    act {:version 1 :who owner :layer base-lid :permission owner-pid :session owner-sid
                         :facts [{:e agent-id :k :softland.store/permission :v agent-pid}]}
                    [st _] (send-internal-maker-act st copy-id owner-sid act-u act)]
                st)
              ;; Default (Entry 8 = 4A): make branch layer building on base-lid, root = owner, grant agent on branch
              ;; Must go through send-maker-act-via-copy so the runtime seam records the maker's act in Trails!
              (let [[st branch-lid] (alloc-id st :branch)
                    [st boot-u] (alloc-id st :boot-b)
                    root-b-pid [owner branch-lid branch-lid]
                    boot-act {:version 1 :who owner :layer branch-lid :permission root-b-pid :session owner-sid
                              :facts [{:e branch-lid :k :softland.store/depot :v :streaming}
                                      {:e owner :k :softland.store/permission :v root-b-pid}]}
                    [st _] (send-internal-maker-act st copy-id owner-sid boot-u boot-act)
                    ;; Set builds-on base-lid + grant agent-id on branch-lid
                    agent-pid (make-perm-id (:config st) agent-id branch-lid branch-lid root-b-pid :see-and-act grant-id)
                    [st act-u] (alloc-id st :act-u)
                    setup-act {:version 1 :who owner :layer branch-lid :permission root-b-pid :session owner-sid
                               :facts [{:e branch-lid :k :softland.store/builds-on :v base-lid}
                                       {:e agent-id :k :softland.store/permission :v agent-pid}]}
                    [st _] (send-internal-maker-act st copy-id owner-sid act-u setup-act)]
                st))))))))

(defn op-subgrant-permission
  "An existing holder (e.g. agent) delegates a child permission to a helper sub-agent."
  [st copy-id holder-sid lid]
  (let [sess (get-in st [:auth :sessions holder-sid])
        lm (get-in st [:store :layers lid])]
    (if (or (nil? sess) (not (:active? sess)) (nil? lm))
      st
      (let [holder (:actor sess)
            parent-pid (first (filter #(and (= holder (perm-holder %))
                                            (perm-standing? lm %)
                                            (= :see-and-act (perm-allows %)))
                                      (keys (:perms lm))))]
        (if (nil? parent-pid)
          st
          (let [[st sub-id] (alloc-id st :subagent)
                [st sub-sid] (alloc-id st :subsess)
                [st grant-id] (alloc-id st :grant)
                st (bump-tick st)
                sub-sess {:sid sub-sid :person (:person sess) :actor sub-id
                          :secret-ver 1 :active? true :created-at (:tick st)}
                sub-pid (make-perm-id (:config st) sub-id lid lid parent-pid :see-and-act grant-id)
                [st act-u] (alloc-id st :act-u)
                act {:version 1 :who holder :layer lid :permission parent-pid :session holder-sid
                     :facts [{:e sub-id :k :softland.store/permission :v sub-pid}]}
                st (assoc-in st [:auth :sessions sub-sid] sub-sess)
                [st _] (send-internal-maker-act st copy-id holder-sid act-u act)]
            st))))))

(defn op-end-permission
  "Root or ancestor ends a granted non-root permission in `lid`."
  [st copy-id ender-sid lid target-pid]
  (let [sess (get-in st [:auth :sessions ender-sid])
        lm (get-in st [:store :layers lid])]
    (if (or (nil? sess) (not (:active? sess)) (nil? lm) (nil? (perm-parent target-pid)))
      st
      (let [ender (:actor sess)
            ender-pid (first (filter #(and (= ender (perm-holder %))
                                           (perm-standing? lm %)
                                           (= :see-and-act (perm-allows %)))
                                     (keys (:perms lm))))]
        (if (nil? ender-pid)
          st
          (let [[st act-u] (alloc-id st :act-u)
                act {:version 1 :who ender :layer lid :permission ender-pid :session ender-sid
                     :facts [{:e (perm-holder target-pid) :k :softland.store/end-permission :v target-pid}]}
                [st _] (send-internal-maker-act st copy-id ender-sid act-u act)]
            st))))))

(defn op-regive-permission
  "After ending a holder's permission, root re-gives permission to the SAME holder in `lid`."
  [st copy-id root-sid lid holder]
  (let [sess (get-in st [:auth :sessions root-sid])
        lm (get-in st [:store :layers lid])]
    (if (or (nil? sess) (not (:active? sess)) (nil? lm))
      st
      (let [root (:actor sess)
            root-pid [root lid lid]]
        (if-not (perm-standing? lm root-pid)
          st
          (let [[st grant-id] (alloc-id st :grant)
                new-pid (make-perm-id (:config st) holder lid lid root-pid :see-and-act grant-id)
                [st act-u] (alloc-id st :act-u)
                act {:version 1 :who root :layer lid :permission root-pid :session root-sid
                     :facts [{:e holder :k :softland.store/permission :v new-pid}]}
                [st _] (send-internal-maker-act st copy-id root-sid act-u act)]
            st))))))

(defn op-write-settings
  "A session attempts to change :who-may-see on `lid`."
  [st copy-id sid lid visibility]
  (let [sess (get-in st [:auth :sessions sid])
        lm (get-in st [:store :layers lid])]
    (if (or (nil? sess) (not (:active? sess)) (nil? lm))
      st
      (let [actor (:actor sess)
            pid (first (filter #(and (= actor (perm-holder %))
                                     (perm-standing? lm %)
                                     (= :see-and-act (perm-allows %)))
                               (keys (:perms lm))))]
        (if (nil? pid)
          st
          (let [[st act-u] (alloc-id st :act-u)
                act {:version 1 :who actor :layer lid :permission pid :session sid
                     :facts [{:e lid :k :softland.store/who-may-see :v visibility}]}
                [st _] (send-internal-maker-act st copy-id sid act-u act)]
            st))))))

(defn op-read-layer
  "Session reads a layer through the runtime -> appends :read entry to session's Trail."
  [st sid lid]
  (if (and (get-in st [:auth :sessions sid :active?])
           (actor-may-see-layer? st lid (get-in st [:auth :sessions sid :actor])))
    (let [heads (get-in st [:store :layers lid :heads] {})]
      (append-trail st sid {:kind :read :target [:layer lid] :seen-keys (vec (keys heads))}))
    st))

(defn op-read-trail
  "Session attempts to read `target-sid`'s trail through the unified `read` word."
  [st reader-sid target-sid]
  (let [sess (get-in st [:auth :sessions reader-sid])
        allowed? (and sess (:active? sess)
                      (actor-may-see-trail? st target-sid (:actor sess)))
        items (when allowed? (get-in st [:trails :by-session target-sid] []))]
    (update-in st [:history :trail-reads] conj
               {:reader-sid reader-sid
                :reader-actor (:actor sess)
                :target-sid target-sid
                :allowed? (boolean allowed?)
                :items items})))

(defn op-record-movement-chunk
  "Client sends a batched movement chunk -> stored on session's Trail + updates leased presence on layer."
  [st sid lid points]
  (let [sess (get-in st [:auth :sessions sid])]
    (if (and sess (:active? sess) (actor-may-see-layer? st lid (:actor sess)))
      (let [st (append-trail st sid {:kind :move-chunk :layer lid :points points})
            now (:tick st)]
        (assoc-in st [:trails :presence lid sid]
                  {:actor (:actor sess) :tick now :lease-until (+ now presence-lease-ticks)}))
      st)))

(defn who-is-here-now
  "Reads active presence on `lid` as of current `:tick`."
  [st lid]
  (let [now (:tick st)]
    (into {}
          (filter (fn [[sid entry]]
                    (and (<= now (:lease-until entry))
                         (get-in st [:auth :sessions sid :active?])
                         (actor-may-see-layer? st lid (:actor entry)))))
          (get-in st [:trails :presence lid] {}))))

(defn op-open-watch
  "Opens a live watch on `copy-id` for `target` (`[:layer lid]` or `[:trail sid]`)."
  [st copy-id sid target]
  (if-not (watch-permitted? st sid target)
    st
    (let [[st wid] (alloc-id st :watch)
          w {:wid wid :copy copy-id :session sid :actor (get-in st [:auth :sessions sid :actor])
             :target target :opened-tick (:tick st) :closed? false :delivered []}]
      (assoc-in st [:copies copy-id :watches wid] w))))

(defn op-promote-branch
  "Promoter promotes facts from `branch-lid` into its `:builds-on` target `base-lid`.
   Tests `never-relay`, `:replaces` conflict safety, provenance preservation, and multi-act chunking."
  [st copy-id promoter-sid branch-lid crash-mid-promote?]
  (let [sess (get-in st [:auth :sessions promoter-sid])
        branch-lm (get-in st [:store :layers branch-lid])
        base-lid (get-in branch-lm [:settings :builds-on])
        base-lm (when base-lid (get-in st [:store :layers base-lid]))]
    (if (or (nil? sess) (not (:active? sess)) (nil? branch-lm) (nil? base-lm))
      st
      (let [promoter (:actor sess)
            promoter-pid (first (filter #(and (= promoter (perm-holder %))
                                              (perm-standing? base-lm %)
                                              (= :see-and-act (perm-allows %)))
                                        (keys (:perms base-lm))))
            branch-facts (vec (vals (:heads branch-lm)))]
        (if (or (nil? promoter-pid) (empty? branch-facts))
          st
          (let [cfg (:config st)
                [st promo-id] (alloc-id st :promo)]
            (case (:promote-form cfg)
              :naive-overwrite
              ;; Naive promote: tries to squeeze all branch facts into 1 act, omits :expected-head/:replaces check,
              ;; and loses :promoted-from provenance!
              (let [[st act-u] (alloc-id st :promo-u)
                    facts (mapv (fn [bf] {:e (:e bf) :k (:k bf) :v (:v bf)}) branch-facts)
                    act {:version 1 :who promoter :layer base-lid :permission promoter-pid :session promoter-sid
                         :facts facts}
                    [st resp] (send-internal-maker-act st copy-id promoter-sid act-u act)]
                (update-in st [:history :promotes] conj
                           {:promo-id promo-id :branch branch-lid :target base-lid
                            :branch-facts branch-facts :chunks [resp] :completed? (= :accepted (:answer resp))}))

              :chunked-replaces
              ;; Safe promote: carries :replaces + :expected-head (what branch fact replaced in base),
              ;; keeps :promoted-from provenance on each fact, chunks by max-facts-per-act, and is crash-resumable!
              (let [known-depot (get-in st [:store :layers base-lid :depot] :streaming)
                    prepared (mapv (fn [bf]
                                     {:e (:e bf) :k (:k bf) :v (:v bf)
                                      :replaces (:replaces bf)
                                      :expected-head true
                                      :promoted-from (:fact-id bf)})
                                   branch-facts)
                    chunks (vec (partition-all max-facts-per-act prepared))
                    run-chunks
                    (fn [s0 stop-after-first?]
                      (loop [s s0 idx 0 resps []]
                        (if (= idx (count chunks))
                          [s resps true]
                          (if (and stop-after-first? (= idx 1))
                            [(op-crash-copy s copy-id) resps false]
                            (let [chunk-u (keyword (str (name promo-id) "-c" idx))
                                  pinned-id [base-lid known-depot chunk-u]
                                  act {:version 1 :who promoter :layer base-lid
                                       :permission promoter-pid :session promoter-sid
                                       :facts (vec (nth chunks idx))}
                                  [s' r] (send-maker-act-via-copy s copy-id promoter-sid pinned-id chunk-u act)]
                              (if (= :accepted (:answer r))
                                (recur s' (inc idx) (conj resps r))
                                [s' (conj resps r) false]))))))
                    [st resps done?] (run-chunks st crash-mid-promote?)
                    ;; If copy crashed mid-promote, second copy resumes with the SAME deterministic chunk UUIDs!
                    [st final-resps final-done?]
                    (if (and crash-mid-promote? (not done?))
                      (run-chunks st false)
                      [st resps done?])]
                (update-in st [:history :promotes] conj
                           {:promo-id promo-id :branch branch-lid :target base-lid
                            :branch-facts branch-facts :chunks final-resps :completed? final-done?})))))))))
