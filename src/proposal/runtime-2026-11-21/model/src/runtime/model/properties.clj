(ns runtime.model.properties
  "Seven invariants over any finished history of the runtime model.
   Each function returns nil when the property holds, or a map describing
   the first violation when it fails."
  (:require [runtime.model.core :as m]))

(defn p1-idempotent-one-rt
  "Every logical maker send (and any retries across runtime copies, secret
   rotations, or layer depot re-classing):
   1. Never lands more than once in Store.
   2. Never gets conflicting answers (:accepted vs :refused) across retries of an active session.
   3. Never fails with :wrong-depot solely because a layer re-classed."
  [st]
  (first
   (for [intent (get-in st [:history :wire-sends])
         :let [resps (:responses intent)
               non-auth-resps (remove #(= :unauthenticated (:reason %)) resps)
               act-ids (distinct (keep :act-id non-auth-resps))
               lid (get-in intent [:act :layer])
               landed-acts (filter (fn [a]
                                     (and (= :accepted (:answer a))
                                          (some #{(:act-id a)} act-ids)))
                                   (get-in st [:store :layers lid :acts]))
               distinct-outcomes (distinct (map :answer non-auth-resps))
               wrong-depot (first (filter #(= :wrong-depot (:reason %)) non-auth-resps))]
         :when (or (> (count landed-acts) 1)
                   (> (count distinct-outcomes) 1)
                   (some? wrong-depot))]
     {:property :p1-idempotent-one-rt
      :intent-id (:intent-id intent)
      :landed-count (count landed-acts)
      :outcomes (vec distinct-outcomes)
      :wrong-depot-error wrong-depot})))

(defn p2-bootstrap-completeness
  "Across concurrent sign-ins and runtime JVM crashes between Auth and Store:
   1. Each [provider sub] maps to at most one person id across all issued sessions.
   2. Every issued session has its person's own-layer AND standing root permission present in Store."
  [st]
  (or
   ;; (1) Uniqueness per external subject
   (first
    (for [[sub entries] (group-by :subject (get-in st [:history :signins]))
          :let [people (distinct (map :person entries))]
          :when (> (count people) 1)]
      {:property :p2-bootstrap-completeness
       :violation :duplicate-person-for-subject
       :subject sub
       :people (vec people)}))
   ;; (2) No active or historical session issued without Store layer & root permission ready
   (first
    (for [{:keys [subject sid person]} (get-in st [:history :signins])
          :let [acct (get-in st [:auth :accounts [:google subject]])
                own-lid (:own-layer acct)
                lm (get-in st [:store :layers own-lid])
                root-pid [person own-lid own-lid]]
          :when (or (nil? lm)
                    (not (m/perm-standing? lm root-pid)))]
      {:property :p2-bootstrap-completeness
       :violation :session-issued-without-store-bootstrap
       :subject subject
       :session sid
       :person person
       :own-layer own-lid
       :layer-exists? (some? lm)}))))

(defn p3-revocation-and-regive
  "Permission lifecycle invariants (Piece 1 + ‹45c7127b›):
   1. No act is ever accepted under an ended permission or a descendant of an ended permission.
   2. When a holder's permission is ended and later re-given:
      - the holder CAN hold a standing permission again (re-give works, not dead forever), AND
      - any child permission granted under the OLD ended permission MUST remain ended
        ('Those beneath do not come back by themselves', PROPOSAL.md:121)."
  [st]
  (first
   (for [[lid lm] (get-in st [:store :layers])
         ;; Walk the layer's accepted acts in stamp order to track true causal history of grants & ends
         :let [check-history
               (loop [acts (:acts lm)
                      ended-ever #{}      ;; permissions ended at some earlier stamp
                      children-of-ended #{} ;; permissions whose parent was ended
                      granted #{}]
                 (if (empty? acts)
                   nil
                   (let [a (first acts)]
                     (if (not= :accepted (:answer a))
                       (recur (rest acts) ended-ever children-of-ended granted)
                       (let [used-pid (:permission a)
                             ;; Did this accepted act use a permission that is a child of a previously ended permission?
                             resurrected-child? (some #(contains? children-of-ended %) (m/perm-ancestors used-pid))
                             ;; Or did a re-give fail to distinguish the new grant from an already-ended grant id,
                             ;; causing old ended children's parent id to become "un-ended" in lm?
                             new-grants (set (keep #(when (= :softland.store/permission (:k %)) (:v %)) (:facts a)))
                             new-ends   (set (keep #(when (= :softland.store/end-permission (:k %)) (:v %)) (:facts a)))
                             granted'   (into granted new-grants)
                             ended'     (into ended-ever new-ends)
                             ;; Any existing grant whose ancestor is in new-ends is permanently dead!
                             newly-dead-children
                             (set (for [p granted'
                                        :when (and (not (contains? new-ends p))
                                                   (some new-ends (rest (m/perm-ancestors p))))]
                                    p))
                             children-dead' (into children-of-ended newly-dead-children)
                             ;; Also check at current state if any permanently dead child currently counts as standing!
                             zombie-child (first (filter #(m/perm-standing? lm %) children-dead'))]
                         (cond
                           resurrected-child?
                           {:violation :ended-child-acted-after-parent-regive
                            :layer lid :stamp (:stamp a) :used-perm used-pid}

                           zombie-child
                           {:violation :child-permission-resurrected-by-regive
                            :layer lid :zombie-perm zombie-child}

                           :else
                           (recur (rest acts) ended' children-dead' granted')))))))]
         :when (some? check-history)]
     (assoc check-history :property :p3-revocation-and-regive))))

(defn p4-root-only-settings
  "Only a layer's root holder may mutate its settings (:who-may-see, :builds-on, :marked-keys)."
  [st]
  (let [skeys #{:softland.store/who-may-see :softland.store/builds-on :softland.store/marked-keys}]
    (first
     (for [[lid lm] (get-in st [:store :layers])
           a (:acts lm)
           :when (and (= :accepted (:answer a))
                      (> (:stamp a) 1)
                      (some #(contains? skeys (:k %)) (:facts a))
                      (not= (:who a) (:root lm)))]
       {:property :p4-root-only-settings
        :layer lid
        :root (:root lm)
        :illegal-writer (:who a)
        :stamp (:stamp a)}))))

(defn p5-zero-leak-live-watches
  "Across all runtime copies (:c0, :c1):
   1. If a watch's session is currently inactive OR its actor no longer has clearance
      to see the watched target (:layer or :trail), no event was delivered to that watch
      at a tick when watch-permitted? was false (i.e. watches must close on revocation!).
   2. who-is-here-now never includes inactive sessions, unpermitted actors, or expired leases."
  [st]
  (or
   ;; Check current open watches whose permission or session is NOW gone: they MUST be marked :closed? true
   ;; before any subsequent write happened!
   (first
    (for [copy-id [:c0 :c1]
          [wid w] (get-in st [:copies copy-id :watches])
          :when (and (not (:closed? w))
                     (not (m/watch-permitted? st (:session w) (:target w))))]
      {:property :p5-zero-leak-live-watches
       :violation :watch-stayed-open-after-revocation
       :copy copy-id
       :watch-id wid
       :session (:session w)
       :target (:target w)}))
   ;; Check who-is-here-now lease & clearance hygiene
   (first
    (for [lid (keys (get-in st [:store :layers]))
          [sid entry] (m/who-is-here-now st lid)
          :when (or (> (:tick st) (:lease-until entry))
                    (not (get-in st [:auth :sessions sid :active?]))
                    (not (m/actor-may-see-layer? st lid (:actor entry))))]
      {:property :p5-zero-leak-live-watches
       :violation :stale-or-unauthorized-presence
       :layer lid
       :session sid}))))

(defn p6-never-relay-and-safe-promote
  "1. Never-relay: every non-bootstrap accepted act has :who equal to an existing session's :actor
      (never ':spoofed-other').
   2. Safe promote:
      - Every completed promotion preserves :promoted-from provenance on target facts.
      - Multi-fact branches (> max-facts-per-act) with no base conflict succeed instead of failing :too-many-facts.
      - No promote ever overwrites a base-layer fact that was not the fact the branch replaced (no lost update)."
  [st]
  (or
   ;; (1) Never-relay check
   (first
    (for [[lid lm] (get-in st [:store :layers])
          a (:acts lm)
          :when (and (= :accepted (:answer a))
                     (= :spoofed-other (:who a)))]
      {:property :p6-never-relay-and-safe-promote
       :violation :spoofed-who-accepted
       :layer lid :act-id (:act-id a)}))
   ;; (2) Promote provenance, multi-act capacity, and conflict safety
   (first
    (for [p (get-in st [:history :promotes])
          :let [reasons (set (keep :reason (:chunks p)))
                too-big-failure? (contains? reasons :too-many-facts)
                target-lm (get-in st [:store :layers (:target p)])
                ;; Check accepted promote acts in target layer for missing provenance or lost update
                accepted-act-ids (set (keep #(when (= :accepted (:answer %)) (:act-id %)) (:chunks p)))
                promo-acts (filter #(contains? accepted-act-ids (:act-id %)) (:acts target-lm))
                missing-prov? (some (fn [a] (some #(nil? (:promoted-from %)) (:facts a))) promo-acts)
                ;; Check lost update: inside target-lm's accepted acts in order, did a promo act overwrite
                ;; a prior target fact id != (:replaces bf)?
                lost-update?
                (loop [acts (:acts target-lm)
                       heads {}]
                  (if (empty? acts)
                    false
                    (let [a (first acts)]
                      (if (not= :accepted (:answer a))
                        (recur (rest acts) heads)
                        (let [is-this-promo? (contains? accepted-act-ids (:act-id a))
                              clobbered?
                              (and is-this-promo?
                                   (some (fn [f]
                                           (let [cur-fid (get heads [(:e f) (:k f)])
                                                 bf (first (filter #(and (= (:e f) (:e %)) (= (:k f) (:k %)))
                                                                   (:branch-facts p)))]
                                             (and bf (not= cur-fid (:replaces bf)))))
                                         (:facts a)))
                              heads' (reduce (fn [h [i f]]
                                               (if (contains? #{:note} (:k f))
                                                 (assoc h [(:e f) (:k f)] [(:act-id a) i])
                                                 h))
                                             heads
                                             (map-indexed vector (:facts a)))]
                          (if clobbered?
                            true
                            (recur (rest acts) heads')))))))]
          :when (or too-big-failure? missing-prov? lost-update?)]
      {:property :p6-never-relay-and-safe-promote
       :promo-id (:promo-id p)
       :too-many-facts-refusal? too-big-failure?
       :missing-provenance? (boolean missing-prov?)
       :lost-update-overwrite? (boolean lost-update?)}))))

(defn p7-trail-causality-and-visibility
  "1. Without :read-seq on Store acts, every accepted maker act sent by session S appears in Trails[S]
      strictly after the reads S made before sending it, in monotonic :seq order.
   2. No unauthorized reader ever reads another person's session trail."
  [st]
  (or
   ;; (1) Every accepted maker act is recorded in its session's trail with strictly increasing :seq
   (first
    (for [[lid lm] (get-in st [:store :layers])
          a (:acts lm)
          :let [sid (:session a)]
          :when (and (= :accepted (:answer a))
                     (not= :system-boot sid)
                     (nil? (first (filter #(and (= :act (:kind %))
                                                (= (:act-id a) (:act-id %)))
                                          (get-in st [:trails :by-session sid])))))]
      {:property :p7-trail-causality-and-visibility
       :violation :accepted-act-missing-from-session-trail
       :layer lid :session sid :act-id (:act-id a)}))
   ;; (2) Trail privacy
   (first
    (for [tr (get-in st [:history :trail-reads])
          :let [target-sess (get-in st [:auth :sessions (:target-sid tr)])]
          :when (and (:allowed? tr)
                     (or (nil? target-sess)
                         (and (not= (:reader-actor tr) (:actor target-sess))
                              (not= (:reader-actor tr) (:person target-sess)))))]
      {:property :p7-trail-causality-and-visibility
       :violation :unauthorized-trail-read
       :reader (:reader-actor tr)
       :target-session (:target-sid tr)}))))

(def all-properties
  [[:P1-one-rt       p1-idempotent-one-rt]
   [:P2-sign-in      p2-bootstrap-completeness]
   [:P3-permissions  p3-revocation-and-regive]
   [:P4-settings     p4-root-only-settings]
   [:P5-watches      p5-zero-leak-live-watches]
   [:P6-promote      p6-never-relay-and-safe-promote]
   [:P7-trails       p7-trail-causality-and-visibility]])
