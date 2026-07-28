(ns app.shared.matter-room
  "matter-room P1/P2/P3/P4 — deterministic addresses, resident composition, and
   pure act/briefing parameters for
   facet-master rooms.

   P1: the JVM derives room ids. CLJS consumes the served `:portal/room`
   mapping and must never mint an address independently (matter-room R2-7).

   P2: a master's ROOM is a real conversation container whose machine
   residents are ORDINARY DURABLE BLOCKS (L3). This namespace is the PURE
   half of that — it derives each resident's identity, its content, and the
   act the driver must perform (birth / refresh / nothing). It performs no
   I/O, holds no clock, and builds no request: the driver
   (`server_jetty/open-matter-room!`) rides the EXISTING episode import path
   for birth (G5 — no new import family, no bespoke import composer) and the
   EXISTING `:object/edit` lane for refresh (T1 — no second write artery).

   P3: this remains a PURE seam. It normalizes parameters for the named matter
   act lane and derives the master-anchored room briefing from the
   server-authoritative reverse table. It does NOT build an ActionRequest,
   append anything, or expose preview over HTTP. Jetty hands the normalized
   parameters to the existing P6 functions; preview remains the client
   membrane.

   P4: the gauge is another ordinary room resident. Standard room opening
   births its stable identity with an unmeasured placeholder but never
   overwrites a later report. The explicitly on-demand face composes the SAME
   identity with the verbatim last computed report and marks it refreshable;
   the existing P2 `:object/edit` lane is the only driver that may land it.

   THE LIFECYCLE, pinned (PLAN §P2, F5):
   - BIRTH ONCE per resident, at an identity-only turn-id
     `mr:<master-id>:<section>[:<revision-id>]` — NEVER a content hash in the
     id. A replayed birth (same id, same payload) converges; changed content
     must NEVER re-import, because the kernel's import fingerprint is strict
     (`import-material-fingerprint-conflict-error`).
   - REFRESH through the edit lane, so changed content lands as a revision on
     the SAME unit instead of a duplicate resident.
   - APPEND-ONLY TRAIL: one resident per pointer revision; a new revision
     births exactly ONE new resident and edits nothing — historically honest.

   TIME (T10 + PLAN F5): no clock enters a resident. `:resident/time-ms` is
   the COMPOSITION ORDINAL — the room's order-key encodes composition order
   (head · bindings · gauge · trail oldest→newest), not a moment. This is a ruling,
   not an oversight: the birth payload is FINGERPRINTED, so a serve-time
   clock would make an honest replay a durable
   `import-material-fingerprint-conflict-error`, and the anchor projection
   carries no claimed birth time for a composed resident. Claimed times from
   durable truth ride the resident TEXT, where they are labelled as claimed."
  (:require [app.shared.activation-event :as activation-event]
            [app.shared.facet-masters :as facet-masters]
            [clojure.string :as str])
  #?(:clj
     (:import [java.nio.charset StandardCharsets]
              [java.util UUID])))

(def registered-master-ids
  "The finite registry input from which both directions are derived."
  facet-masters/master-ids)

(defn room-id
  "A UUID-shaped, byte-stable conversation id derived from exactly the UTF-8
   bytes of `master-id` with JDK nameUUIDFromBytes (UUID v3 / MD5)."
  [master-id]
  #?(:clj
     (str (UUID/nameUUIDFromBytes
           (.getBytes (str master-id) StandardCharsets/UTF_8)))
     :cljs
     (throw
      (ex-info "Room ids are server-derived; read :portal/room"
               {:master-id master-id
                :type :matter-room/client-derivation-forbidden}))))

(def room-id-by-master
  "The finite registered master -> room table. Unknown anchors are still total
   and serve their directly-derived mapping, but are not added to this registry."
  #?(:clj
     (into (sorted-map)
           (map (fn [master-id] [master-id (room-id master-id)]))
           registered-master-ids)
     :cljs {}))

(def master-id-by-room
  "The server-authoritative reverse table used by the later room briefing lane."
  #?(:clj
     (into (sorted-map)
           (map (fn [[master-id derived-room-id]]
                  [derived-room-id master-id]))
           room-id-by-master)
     :cljs {}))

(defn master-id-for-room
  [room-id]
  (get master-id-by-room (str room-id)))

;; ===========================================================================
;; P3 · the room's hands and mouth — pure authority + act parameter builders
;; ===========================================================================

(def matter-actor
  "The default actor for an act invoked from Sid's local matter-room console.
   Jetty supplies it only when the caller omitted an actor; tests and future
   authenticated callers can pass another valid declared actor."
  {:actor/id "sid" :actor/type :human})

(defn narrowed-portal-open
  "The ONLY master-anchored portal-open shape a room turn may use.

   The conversation id is the authority. Any caller-supplied master, entity,
   wearer, or master set is deliberately ignored; a registered room resolves
   through the server's finite reverse table and can name exactly one master.
   Unknown/non-room conversations return nil so the existing P8 block-narrowing
   path remains in force."
  [{:keys [conversation-id]}]
  (when-let [master-id (master-id-for-room conversation-id)]
    {:master-id master-id
     :conversation-id (str conversation-id)
     :narrowed? true}))

(defn- nonblank-string?
  [x]
  (and (string? x) (not (str/blank? x))))

(defn- act-error
  ([verb error]
   (act-error verb error []))
  ([verb error errors]
   {:act/verb verb
    :act/valid? false
    :act/error error
    :act/errors (vec errors)}))

(defn- common-act-error
  [verb {:keys [master-id request-id time-ms actor]}]
  (cond
    (not (nonblank-string? master-id))
    (act-error verb :matter/master-id-required)

    (not (nonblank-string? request-id))
    (act-error verb :matter/request-id-required)

    (not (integer? time-ms))
    (act-error verb :matter/time-ms-required)

    (not (activation-event/valid-actor? actor))
    (act-error verb :matter/actor-invalid)

    :else nil))

(defn- act-options
  [{:keys [request-id time-ms actor conversation-id scope grounds]}]
  (cond-> {:request/id request-id
           :time-ms (long time-ms)
           :actor actor}
    (some? conversation-id) (assoc :conversation-id conversation-id)
    (some? scope) (assoc :activation/scope scope)
    (some? grounds) (assoc :activation/grounds grounds)))

(defn deviate-request
  "Normalize one `:matter/deviate` invocation without performing it.

   Exactly one existing P6 branch is selected:
   - `subject-uid` + an overrides map → `material-truth/deviate!`
   - source bytes, with no subject      → `facet-master/import-candidate!`

   Supplying both is refused rather than guessing which durable truth the
   inhabitant intended to move."
  [{:keys [master-id subject-uid overrides source] :as request}]
  (or
   (common-act-error :matter/deviate request)
   (cond
     (and (nonblank-string? subject-uid) (nonblank-string? source))
     (act-error :matter/deviate :matter/deviation-branch-ambiguous)

     (nonblank-string? subject-uid)
     (if (map? overrides)
       {:act/verb :matter/deviate
        :act/valid? true
        :act/branch :instance
        :act/master-id master-id
        :act/subject-uid subject-uid
        :act/overrides overrides
        :act/options (act-options request)}
       (act-error :matter/deviate :matter/overrides-map-required))

     (nonblank-string? source)
     {:act/verb :matter/deviate
      :act/valid? true
      :act/branch :master-candidate
      :act/master-id master-id
      :act/source source
      :act/options (act-options request)}

     :else
     (act-error :matter/deviate :matter/deviation-content-required))))

(defn activation-request
  "Normalize and grammar-check an activate/rollback invocation.

   The returned `:act/event` is the exact closed activation form the existing
   `facet-master/activate!` call must reproduce from `:act/options`. Invalid
   scope/actor/grounds become a total error card before any pointer write."
  [kind {:keys [master-id revision-id actor time-ms scope grounds] :as request}]
  (let [verb (case kind
               :activate :matter/activate
               :rollback :matter/rollback
               nil)]
    (cond
      (nil? verb)
      (act-error :matter/activate :matter/activation-kind-invalid)

      :else
      (or
       (common-act-error verb request)
       (when-not (nonblank-string? revision-id)
         (act-error verb :matter/revision-id-required))
       (let [event (activation-event/event
                    {:revision-id revision-id
                     :kind kind
                     :scope scope
                     :actor actor
                     :time-ms time-ms
                     :grounds grounds})
             errors (activation-event/event-errors event)]
         (if (seq errors)
           (act-error verb :matter/activation-event-invalid errors)
           {:act/verb verb
            :act/valid? true
            :act/master-id master-id
            :act/revision-id revision-id
            :act/event event
            :act/options
            (assoc (act-options request)
                   :activation/kind kind
                   :activation/scope (:activation/scope event)
                   :activation/grounds (:activation/grounds event))}))))))

(defn recovery-offer
  "Find the exact currently-served rollback offer the act lane may execute."
  [portal-result master-id to-revision-id]
  (first
   (filter
    #(and (= master-id (:recovery/master-id %))
          (= to-revision-id (:recovery/to-revision-id %))
          (= :rollback (:recovery/kind %)))
    (get-in portal-result [:portal/recovery :recovery/offers]))))

;; ===========================================================================
;; P2 · the room's machine residents — identity, content, act (all pure)
;; ===========================================================================

(def resident-actor-id
  "The room's machine actor. It is NOT \"sid\", which is the whole point: the
   render's machine classification reads the projection hint's role slot and
   asks `(not= speaker \"sid\")` (ground.cljs ← face_projection ←
   block_distiller ← episode's hint)."
  "softland:matter-room")

(def resident-actor
  "The birth/refresh actor.

   `:actor/type` is `:agent`, NOT `:machine`: `core/actor-types` is
   `#{:human :agent :system :bot}` and `:machine` is RK-only — the same trap
   `block-distiller/import-actor` names in its own docstring. `:agent` is
   also not `:system`, so `authorized-request?` genuinely checks the
   capabilities below instead of waving the write through."
  {:actor/id resident-actor-id
   :actor/type :agent
   :actor/capabilities #{:object-container/import-material :object/edit}})

(def resident-actor-role
  "The production-event role beside the actor id. `\"user\"` is Sid's lane."
  "assistant")

(def resident-part-type
  "The WHOLE-BLOCK cut (`free-cut-part :material` → `:material-part`).
   `:text` would fan one resident into N markdown blocks and break the
   one-unit edit lane the refresh path depends on."
  :material)

(def resident-edit-client-id
  "The edit lane's client identity. Room refreshes are keyed
   (lineage-key = unit-id, client-id) by the kernel's staleness rule, so this
   constant is what makes the room's own seq the only seq it competes with."
  resident-actor-id)

(def resident-column-x 0.0)
(def resident-row-height 260.0)
(def gauge-resident-index 2)

(defn resident-turn-id
  "The identity-only birth id. No content hash: content changes ride the edit
   lane, and an id that moved with content would duplicate the resident on
   every change (T2)."
  ([master-id section] (str "mr:" master-id ":" (name section)))
  ([master-id section discriminator]
   (str "mr:" master-id ":" (name section) ":" discriminator)))

(defn- line
  [label value]
  (str label ": " (if (nil? value) "—" (str value))))

(defn- section-text
  [title lines]
  (str/join "\n" (into [title] (remove nil? lines))))

(defn- head-text
  [master-id identity* master room-address]
  (section-text
   (str "# " master-id " — master")
   [(line "facet" (get identity* :entity/facet))
    (line "found?" (get identity* :entity/found?))
    (line "source" (get identity* :entity/source-id))
    (line "floor revision" (get master :master/floor-revision-id))
    (line "active revision" (get master :master/active-revision-id))
    (line "latest revision" (get master :master/latest-revision-id))
    (line "candidate?" (true? (get master :master/candidate?)))
    (line "candidate revision" (get master :master/candidate-revision-id))
    (line "pointer revision" (get master :master/pointer-revision-id))
    (line "previous revision" (get master :master/previous-revision-id))
    (line "grammar" (get master :master/grammar))
    (line "valid?" (get master :master/valid?))
    (line "room" room-address)
    ""
    (str "This resident is a projection of durable master state, refreshed"
         " through the block edit lane. It is not a second truth: every value"
         " above has an owner in the object container.")]))

(defn- bindings-text
  [master-id bindings]
  (let [rows (vec (get bindings :bindings/rows))
        verbs (vec (get bindings :bindings/verbs))]
    (section-text
     (str "# " master-id " — bindings")
     (into [(line "rows" (count rows))
            (line "sites" (get bindings :bindings/sites))
            (line "conflicts" (count (get bindings :bindings/conflicts)))
            (line "unclaimed falls to" (get bindings :bindings/unclaimed-falls-to))
            ""]
           (map (fn [v]
                  (str "- " (get v :verb/name)
                       " · effect " (get v :verb/effect-class)
                       " · required-args " (get v :verb/required-args)
                       (when (true? (get v :verb/floor-reserved?))
                         " · FLOOR-RESERVED"))))
           verbs))))

(defn- gauge-text
  [master-id report]
  (section-text
   (str "# " master-id " — terminal escape gauge")
   [(if (map? report)
      (str "LAST COMPUTED REPORT (verbatim EDN): "
           (pr-str (into (sorted-map) report)))
      "LAST COMPUTED REPORT: not measured — open the linked :escape-gauge face on demand.")
    ""
    (str "This resident is refreshed through the existing P2 block edit lane."
         " Standard portal open links the gauge but never runs git and never"
         " embeds or overwrites a computed report.")]))

(defn- place-resident
  [index resident]
  (assoc resident
         :resident/index index
         ;; the ordinal IS the order key input (ns docstring)
         :resident/time-ms (long index)
         :resident/position
         {:x resident-column-x
          :y (* (double index) resident-row-height)}))

(defn gauge-resident
  "The stable resident identity for one room's LAST on-demand escape report.

   With no report, this is the birth-only placeholder and deliberately NOT
   refreshable: a later standard room open must never downgrade computed bytes
   back to `not measured`. With a report, the on-demand face marks the same
   resident refreshable so the existing P2 edit lane can land the new bytes."
  [master-id report]
  (when (string? master-id)
    (place-resident
     gauge-resident-index
     {:resident/section :gauge
      :resident/turn-id (resident-turn-id master-id :gauge)
      :resident/text (gauge-text master-id report)
      :resident/report report
      :resident/refreshable? (map? report)})))

(defn- trail-text
  "ONLY facts intrinsic to this pointer revision.

   Deliberately NOT here: `current?`, `on-causal-chain?`, `re-wear?` — every
   one of those is a fact about the PRESENT chain, and a present-fact inside
   an append-only resident makes its content change under it (found live in
   the P2 probe: a new activation demoted the previous pointer and the trail
   resident's text moved). Where the pointer stands today is the HEAD
   resident's job, and the head refreshes."
  [master-id pointer-revision-id entry]
  (section-text
   (str "# " master-id " — activation " pointer-revision-id)
   [(line "pointer revision" pointer-revision-id)
    (line "kind" (get entry :trail/kind))
    (line "revision" (get entry :trail/revision-id))
    (line "parent revision" (get entry :trail/parent-revision-id))
    (line "declared time-ms (claimed, from durable truth)"
          (get entry :trail/declared-time-ms))
    ""
    (str "One resident per POINTER REVISION. A new activation births exactly"
         " one new resident here; nothing above is ever edited, so the room's"
         " trail is historically honest.")]))

(defn- pointer-revision-ids
  "The anchored master's pointer revisions, OLDEST FIRST.

   Source: `:history/available-cuts` — the portal's own nameable-cut list,
   which is the ONE trail read the anchor already performs (`trails-of`), in
   causal order, newest first. Reversing is what makes a new activation
   APPEND a resident instead of renumbering the room."
  [portal-result master-id]
  (->> (get-in portal-result [:portal/history
                              :history/available-cuts
                              master-id])
       (filter string?)
       reverse
       vec))

(defn- trail-entry-by-pointer
  "Any richer trail entry the projection happens to carry for a pointer
   revision. Absent at an anchor today; composing THROUGH it keeps the
   resident honest if the section later fills in."
  [portal-result master-id]
  (into {}
        (keep (fn [e]
                (when-let [pid (get e :trail/pointer-revision-id)]
                  [pid e])))
        (filter map?
                (get-in portal-result [:portal/activation-history
                                       :activation-history/by-master
                                       master-id
                                       :history/trail]))))

(defn residents
  "PURE: one master-anchored portal result → the room's machine residents, in
   room order. Total over garbage (every read is `get`-shaped).

   Each resident carries everything the driver needs and nothing it must
   derive: `:resident/turn-id` (birth identity), `:resident/text`,
   `:resident/time-ms` (the composition ordinal — see the ns docstring),
   `:resident/position` (deterministic column), and `:resident/refreshable?`
   — false for trail residents, which are append-only by construction (a new
   activation births a NEW resident and edits nothing)."
  [portal-result]
  (let [master-id (get portal-result :portal/master-id)]
    (if-not (string? master-id)
      []
      (let [identity* (get portal-result :portal/identity)
            master (get-in portal-result [:portal/masters master-id])
            bindings (get portal-result :portal/bindings)
            room (get portal-result :portal/room)
            room-address (first (keys (when (map? room) room)))
            gauge-linked?
            (= :escape-gauge
               (get-in portal-result
                       [:portal/cascade :cascade/escape-gauge :face]))
            entries (trail-entry-by-pointer portal-result master-id)
            stable
            (cond->
             [{:resident/section :head
               :resident/turn-id (resident-turn-id master-id :head)
               :resident/text (head-text master-id identity* master
                                         room-address)
               :resident/refreshable? true}
              {:resident/section :bindings
               :resident/turn-id (resident-turn-id master-id :bindings)
               :resident/text (bindings-text master-id bindings)
               :resident/refreshable? true}]
              gauge-linked?
              (conj (gauge-resident master-id nil)))
            trail (mapv (fn [pid]
                          {:resident/section :trail
                           :resident/turn-id
                           (resident-turn-id master-id :trail pid)
                           :resident/text
                           (trail-text master-id pid (get entries pid))
                           :resident/refreshable? false})
                        (pointer-revision-ids portal-result master-id))]
        (vec (map-indexed
              (fn [i r]
                (place-resident i r))
              (into stable trail)))))))
