(ns app.shared.matter-room
  "matter-room P1/P2 — deterministic addresses and resident composition for
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
   (head · bindings · trail oldest→newest), not a moment. This is a ruling,
   not an oversight: the birth payload is FINGERPRINTED, so a serve-time
   clock would make an honest replay a durable
   `import-material-fingerprint-conflict-error`, and the anchor projection
   carries no claimed birth time for a composed resident. Claimed times from
   durable truth ride the resident TEXT, where they are labelled as claimed."
  (:require [app.shared.facet-masters :as facet-masters]
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
            entries (trail-entry-by-pointer portal-result master-id)
            stable [{:resident/section :head
                     :resident/turn-id (resident-turn-id master-id :head)
                     :resident/text (head-text master-id identity* master
                                               room-address)
                     :resident/refreshable? true}
                    {:resident/section :bindings
                     :resident/turn-id (resident-turn-id master-id :bindings)
                     :resident/text (bindings-text master-id bindings)
                     :resident/refreshable? true}]
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
                (assoc r
                       :resident/index i
                       ;; the ordinal IS the order key input (ns docstring)
                       :resident/time-ms (long i)
                       :resident/position
                       {:x resident-column-x
                        :y (* (double i) resident-row-height)}))
              (into stable trail)))))))
