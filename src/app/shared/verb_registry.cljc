(ns app.shared.verb-registry
  "editable-material P5 — the named-verb registry (DIRECTION §The architecture,
   station four).

   Binding material names a verb; it never names code. This registry is the
   ONLY vocabulary a binding row may reach for: a row naming anything absent
   here fails its grammar and the facet falls to its code floor. That is the
   structural form of `material never names arbitrary code` — not a convention
   the kernel remembers to check.

   Every entry declares its EFFECT CLASS, so the effect a gesture will have on
   the world is legible from the row alone, before any code runs:

   - `:pure-projection`            — moves appearance/attention only. Nothing
                                     durable, nothing leaves the client.
   - `:durable-via-request`        — ends in an acked request→decision→event
                                     (the settle or named matter-act lane).
                                     Durable truth may move.
   - `:external-via-derived-worker` — reaches outside the land through a
                                     derived worker. No verb wears this class
                                     yet; the class exists because the closed
                                     grammar must be able to SAY it before a
                                     verb needs it.

   `:verb/extracted-from` is an honesty field, not documentation: a strangler
   migration names the exact pre-registry branch it was lifted out of. P8 adds
   the first post-P5 extraction: Ctrl+Enter's already-durable reply path.

   `:verb/floor-reserved?` marks the two verbs the code floor keeps forever
   (space pan and pointer-anchored zoom). `bindable?` refuses them, so no data
   revision — valid or malformed — can bind, rebind, or shadow the camera."
  (:require [clojure.string :as str]))

(def effect-classes
  #{:pure-projection
    :durable-via-request
    :external-via-derived-worker})

(def continuations
  "The moments a verb implementation may serve. `:invoke` is a discrete verb
   (one gesture, one act). A continuous verb serves `:begin` when the gesture
   is claimed, `:move` for each subsequent pointer sample, and `:end` at
   release. Nothing else is dispatchable — the kernel's phase machine has
   exactly these hooks."
  #{:invoke :begin :move :end})

(def verbs
  "verb-name → declaration. Order-independent; `names` is the canonical order."
  {:focus/place-caret
   {:verb/name :focus/place-caret
    :verb/version 0
    :verb/effect-class :pure-projection
    :verb/continuations #{:invoke}
    :verb/required-args #{}
    :verb/extracted-from
    "ground/pointer-up! :pending → non-machine block → ge/focus-block at the
     pressed line/col over truth text"
    :verb/doc
    "Focus the subject block and place its edit caret at the pressed point."}

   :focus/enter-block
   {:verb/name :focus/enter-block
    :verb/version 0
    :verb/effect-class :pure-projection
    :verb/continuations #{:invoke}
    :verb/required-args #{}
    :verb/extracted-from
    "ground/pointer-down! `(when (and text? (not= uid (:focus @!ground-edit)))
     …)` — Task 11: shift on an unfocused user block focuses it in the same
     gesture"
    :verb/doc
    "Focus the subject block ONLY if it is not already focused, seeding the
     caret from the pressed point. An already-focused block keeps its caret."}

   :focus/release
   {:verb/name :focus/release
    :verb/version 0
    :verb/effect-class :pure-projection
    :verb/continuations #{:invoke}
    :verb/required-args #{}
    :verb/extracted-from
    "ground/pointer-up! :pending → machine block, no fold row → ge/escape"
    :verb/doc
    "Blur the focused block (machine bodies are read-only while replies render
     merged), leaving the pressed subject rebuilt."}

   :fold/toggle-section
   {:verb/name :fold/toggle-section
    :verb/version 0
    :verb/effect-class :pure-projection
    :verb/continuations #{:invoke}
    ;; T10 (P5 gate, axis 1): the implementation reads `(:fold-key args)`. A
    ;; VALID revision that bound this verb at `:block/user-hit-area` — a site
    ;; whose claim carries no args — resolved to `:claimed` and then no-opped
    ;; totally, lint-silent. Declaring the requirement is what lets
    ;; `binding-material/valid-row?` refuse that row instead of shipping it.
    :verb/required-args #{:fold-key}
    :verb/extracted-from
    "ground/pointer-up! :pending → machine block → `(when (and run? (<= row 1))
     (if (zero? row) :noise? :prose?))` fold toggle (Task 7)"
    :verb/doc
    "Toggle one run section's fold state. The section arrives as the claim's
     argument — the header node hit is the section, so no row arithmetic
     survives in the kernel."}

   :anchor/place
   {:verb/name :anchor/place
    :verb/version 0
    :verb/effect-class :pure-projection
    :verb/continuations #{:invoke}
    :verb/required-args #{}
    :verb/extracted-from
    "ground/pointer-up! :pending → :ground target → ge/set-anchor (Law 1)"
    :verb/doc
    "Place the ephemeral caret anchor at the pressed world point, blurring any
     focused block."}

   :selection/text-begin
   {:verb/name :selection/text-begin
    :verb/version 0
    :verb/effect-class :pure-projection
    :verb/continuations #{:begin :move :end}
    :verb/required-args #{}
    :verb/extracted-from
    "ground/pointer-move! :pending `(:text? p)` → ge/begin-select, then the
     :selecting phase branch → ge/extend-select"
    :verb/doc
    "Editable text selection inside the pressed block, anchored at the pressed
     caret and extended by the pointer."}

   :selection/machine-begin
   {:verb/name :selection/machine-begin
    :verb/version 0
    :verb/effect-class :pure-projection
    :verb/continuations #{:begin :move :end}
    :verb/required-args #{}
    :verb/extracted-from
    "ground/pointer-move! :pending `(:mtext? p)` → !machine-sel, then the
     :mselecting phase branch (Task 11: copy what you see, read-only)"
    :verb/doc
    "Read-only selection over a machine block's RENDERED lines, anchored at the
     pressed visual line/col."}

   :selection/marquee-begin
   {:verb/name :selection/marquee-begin
    :verb/version 0
    :verb/effect-class :pure-projection
    :verb/continuations #{:begin :move :end}
    :verb/required-args #{}
    :verb/extracted-from
    "ground/pointer-move! :pending `(and (= :ground (:target p)) (:shift? p))`
     → :marquee phase, then pointer-up! :marquee → !group-sel (Task 18)"
    :verb/doc
    "Sweep a group selection over the space; blocks whose AABB intersects the
     sweep join the group at release."}

   :placement/drag-group
   {:verb/name :placement/drag-group
    :verb/version 0
    :verb/effect-class :durable-via-request
    :verb/continuations #{:begin :move :end}
    :verb/required-args #{}
    :verb/extracted-from
    "ground/pointer-move! :pending :else → :dragging phase, the :dragging move
     branch (rigid drag-group, Task 4), and pointer-up! :dragging → arm-settle!
     :cell per member"
    :verb/doc
    "Move the pressed block's rigid drag group. Release ARMS the debounced,
     acked settle write — the position becomes durable truth, so this verb is
     durable-via-request even though every intermediate frame is projection."}

   :camera/pan
   {:verb/name :camera/pan
    :verb/version 0
    :verb/effect-class :durable-via-request
    :verb/continuations #{:begin :move :end}
    :verb/required-args #{}
    :verb/floor-reserved? true
    :verb/extracted-from
    "ground/pointer-move! :panning branch + pointer-up! :panning → arm-settle!
     :camera (Laws 2/3/9)"
    :verb/doc
    "Pan the world camera from the press-time camera. Release arms the camera
     settle. Floor-reserved: the space's own gesture, never material."}

   :camera/zoom-at-pointer
   {:verb/name :camera/zoom-at-pointer
    :verb/version 0
    :verb/effect-class :durable-via-request
    :verb/continuations #{:invoke}
    :verb/required-args #{}
    :verb/floor-reserved? true
    :verb/extracted-from "ground/handle-wheel! (§9.4)"
    :verb/doc
    "Zoom so the world point under the pointer stays under it; arms the camera
     settle at burst end. Floor-reserved."}

   :resident/reply-to-block
   {:verb/name :resident/reply-to-block
    :verb/version 1
    :verb/effect-class :durable-via-request
    :verb/continuations #{:invoke}
    :verb/required-args #{}
    :verb/release-ref
    "softland://verb-release/resident-reply-to-block/v1"
    :verb/extracted-from
    "ground/ground-keys-consumer :eval → submit-turn! — Ctrl+Enter pinned the
     focused block's confirmed revision, recorded it durably, then summoned
     the resident"
    :verb/doc
    "Reply to exactly the addressed user block. The raw turn becomes durable
     before the resident is summoned with that block's narrowed portal
     briefing."}

   ;; matter-room P3 · L5/G7. These are declarations of EXISTING P6 acts, not
   ;; gesture claims and not release births. No site supplies `:master-id` (or
   ;; `:subject-uid`), so strict v2+ material refuses an arg-starved row. The
   ;; named act lane is deliberately separate: three disclosed Jetty endpoints
   ;; plus console verbs. V1 material can accept such a row and then no-op; that
   ;; historical grammar scope is stated in every declaration instead of
   ;; pretending the refusal is absolute.
   :matter/deviate
   {:verb/name :matter/deviate
    :verb/version 0
    :verb/effect-class :durable-via-request
    :verb/continuations #{:invoke}
    :verb/required-args #{:master-id :subject-uid}
    :verb/extracted-from
    "material-truth/deviate! for a subject instance, or
     facet-master/import-candidate! for a master candidate; invoked through
     POST /api/matter-room/deviate and window.__portal.deviate"
    :verb/doc
    "REGISTRY-DECLARED, GRAMMAR-UNBINDABLE at current sites. Deviate through
     the named act lane: subject + overrides uses the existing P6 instance
     deviation; source bytes use the existing facet-master candidate import.
     Strict v2+ material refuses missing master-id/subject-uid; v1 can
     accept-then-no-op."}

   :matter/preview
   {:verb/name :matter/preview
    :verb/version 0
    :verb/effect-class :pure-projection
    :verb/continuations #{:invoke}
    :verb/required-args #{:master-id}
    :verb/extracted-from
    "ground/preview-candidate! and ground/end-preview! through
     window.__bindings.preview/endPreview; invoked by
     window.__portal.preview/endPreview with no server endpoint"
    :verb/doc
    "REGISTRY-DECLARED, GRAMMAR-UNBINDABLE at current sites. Preview is the
     existing client membrane over real served material: pure projection,
     no request, no import, no pointer edit. Strict v2+ material refuses the
     arg-starved row; v1 can accept-then-no-op."}

   :matter/activate
   {:verb/name :matter/activate
    :verb/version 0
    :verb/effect-class :durable-via-request
    :verb/continuations #{:invoke}
    :verb/required-args #{:master-id}
    :verb/extracted-from
    "facet-master/activate! after activation-event grammar validation;
     invoked through POST /api/matter-room/activate and
     window.__portal.activate"
    :verb/doc
    "REGISTRY-DECLARED, GRAMMAR-UNBINDABLE at current sites. Activate one
     retained candidate through the named matter-act lane and the existing
     P6 request-decision-event machinery. Strict v2+ material refuses the
     arg-starved row; v1 can accept-then-no-op."}

   :matter/rollback
   {:verb/name :matter/rollback
    :verb/version 0
    :verb/effect-class :durable-via-request
    :verb/continuations #{:invoke}
    :verb/required-args #{:master-id}
    :verb/extracted-from
    "facet-master/activate! with :activation/kind :rollback at a currently
     served :portal/recovery offer; invoked through
     POST /api/matter-room/rollback and window.__portal.rollback"
    :verb/doc
    "REGISTRY-DECLARED, GRAMMAR-UNBINDABLE at current sites. Rollback
     re-wears a currently offered previous revision through the named act
     lane; it never deletes or rewrites history. Strict v2+ material refuses
     the arg-starved row; v1 can accept-then-no-op."}})

(def names
  "Canonical verb order — every projection and lint listing sorts by it."
  (vec (sort-by str (keys verbs))))

(defn entry
  [verb-name]
  (get verbs verb-name))

(defn known?
  "Is this a registered verb name at this version? Both must match: a row
   pinned to a version the registry no longer serves is refused, never
   silently upgraded."
  [verb-name version]
  (let [e (entry verb-name)]
    (boolean (and e (= version (:verb/version e))))))

(defn floor-reserved?
  [verb-name]
  (true? (:verb/floor-reserved? (entry verb-name))))

(defn bindable?
  "May MATERIAL name this verb? Registered, at the declared version, and not
   floor-reserved. The code floor itself is not held to this — it is where the
   reserved verbs live."
  [verb-name version]
  (and (known? verb-name version)
       (not (floor-reserved? verb-name))))

(defn effect-class
  [verb-name]
  (:verb/effect-class (entry verb-name)))

(defn required-args
  "editable-material P6 · T10 — the claim-arg keys this verb's implementation
   cannot work without. The P5 gate proved the gap this closes: a VALID binding
   revision could bind an arg-requiring verb at a site whose claim carries no
   args, and the result was `:claimed` followed by a total no-op — visible in
   the served table, reversible, and completely silent to lint. Declaring the
   requirement here (and the site's supply in `binding-material/site-arg-keys`)
   turns that from a runtime nothing into a grammar refusal.

   Empty for every verb that reads only the press and its subject."
  [verb-name]
  (set (:verb/required-args (entry verb-name) #{})))

(defn serves?
  "Does this verb declare an implementation for this continuation moment?"
  [verb-name continuation]
  (contains? (:verb/continuations (entry verb-name) #{}) continuation))

(defn continuous?
  [verb-name]
  (serves? verb-name :move))

(defn declaration-rows
  "The registry as flat, deterministic rows — the shape the served interaction
   table and the console listing both consume."
  []
  (mapv
   (fn [verb-name]
     (let [e (entry verb-name)]
       {:verb/name verb-name
        :verb/version (:verb/version e)
        :verb/effect-class (:verb/effect-class e)
        :verb/continuations (vec (sort-by str (:verb/continuations e)))
        :verb/floor-reserved? (true? (:verb/floor-reserved? e))
        :verb/required-args (vec (sort-by str (required-args verb-name)))
        :verb/bindable? (bindable? verb-name (:verb/version e))
        :verb/release-ref (:verb/release-ref e)
        :verb/extracted-from
        (str/replace (str (:verb/extracted-from e)) #"\s+" " ")}))
   names))

(defn well-formed-registry?
  "Every declaration is complete and its classes are in the closed sets. The
   suite asserts this so a malformed entry can never reach a served table."
  []
  (every?
   (fn [verb-name]
     (let [e (entry verb-name)]
       (and (= verb-name (:verb/name e))
            (integer? (:verb/version e))
            (contains? effect-classes (:verb/effect-class e))
            (set? (:verb/continuations e))
            (seq (:verb/continuations e))
            (every? continuations (:verb/continuations e))
            ;; a continuous verb serves all three moments or none of them
            (or (= #{:invoke} (:verb/continuations e))
                (= #{:begin :move :end} (:verb/continuations e)))
            ;; T10: the declaration must EXIST on every entry — an absent
            ;; `:verb/required-args` and an empty one are the same value to
            ;; `required-args`, so only this check keeps a new verb from
            ;; silently re-opening the arg-starved class
            (set? (:verb/required-args e))
            (every? keyword? (:verb/required-args e))
            (string? (:verb/extracted-from e))
            (seq (:verb/extracted-from e))
            (string? (:verb/doc e)))))
   names))
