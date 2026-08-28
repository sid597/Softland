(ns app.server.worn.binding-material
  "editable-material P5 — binding rows as material, and the ONE dispatch law.

   DIRECTION §Four stations of input: gesture (kernel) → one pick over the
   containment path (kernel) → innermost matching material claim (binding rows,
   material) → named verb (code registry). This namespace owns stations three
   and four as a PURE function: `resolve-binding` reads data and returns a
   decision. It never touches an atom, never renders, never effects. The single
   side-effecting site is the client applier, which is why `no side effects in
   reactive queries` holds structurally rather than by inspection.

   THE ROW GRAMMAR IS CLOSED — exactly five keys, every value from an
   enumerated set or the verb registry:

     {:binding/gesture   :pointer/press | :pointer/tap | :pointer/meta |
                         :wheel | :key/eval
      :binding/phase     :begin | :threshold | :complete
      :binding/modifiers #{:shift} | :any
      :binding/verb      {:verb/name … :verb/version …}
      :binding/priority  <integer>}

   Rows are filed by SITE — the same contribution-site vocabulary P3 already
   stamps on contributions. A site is a map key, not a sixth row field: the
   claim the kernel builds at a rendered node names the site, so a row for
   `:block/fold-header` can never fire on a body pixel.

   LOCALITY TIERS, innermost precedence first:
     :instance — rows attached to ONE subject (locality: this block only)
     :master   — the served facet-master's shared rows (locality: every wearer)
     :floor    — that facet's CODE FLOOR rows

   The floor tier is derived from the facet SPEC, never from served data, and it
   is always present. So a valid revision may REBIND a gesture (that is the
   whole point of P5) but can never LOSE one: drop the row and the floor row
   underneath it fires. A malformed revision resolves to the code floor whole
   (P3 totality), which is the same answer by a shorter road.

   CONTAINMENT beats every tier: the law walks the pick path innermost →
   outermost and stops at the first depth that matches. A gesture the innermost
   claim does not name falls OUTWARD — that fallthrough is what lets a fold
   header claim the tap while its block still claims the drag.

   SAME-DEPTH, SAME-TIER, SAME-PRIORITY ties are conflicts: the winner is
   deterministic (`row-order` below) and the tie is REPORTED so the land can
   render it as lint. Never silent."
  (:require [app.server.page.verb-registry :as verb-registry]))

;; ===========================================================================
;; The closed grammar
;; ===========================================================================

(def gesture-kinds
  "Every gesture the kernel normalizes. `:pointer/press` and `:pointer/tap` are
   distinct gestures, not phases of one: a press that crosses the threshold is
   never also a tap."
  #{:pointer/press :pointer/tap :pointer/meta :wheel :key/eval})

(def gesture-phases
  #{:begin :threshold :complete})

(def legal-gestures
  "The enumerated (kind, phase) pairs. A row declaring a pair the kernel cannot
   produce is refused rather than accepted and never fired."
  ;; GRAMMAR VERSION NOTE · Halo P1 · 2026-07-29: `:pointer/meta :complete`
  ;; is the one additive kernel widening. Every pre-existing pair and its v1/v2
  ;; material meaning is byte-frozen; universal reservation below means old
  ;; material rejects the newly-known pair just as it did while unknown.
  #{[:pointer/press :begin]
    [:pointer/press :threshold]
    [:pointer/tap :complete]
    [:pointer/meta :complete]
    [:wheel :complete]
    [:key/eval :complete]})

(def modifier-keys
  "Only what the kernel actually observes today: `events/>mouse` and
   `events/>wheel` carry `:shift?` and nothing else. A row asking for a
   modifier the kernel never normalizes could never fire, so the grammar
   refuses it loudly instead of leaving a dead row in the table. Widening this
   set is a kernel change plus a grammar version — never a silent one."
  #{:shift})

(def sites
  "The claim sites P5 opened with. P1 makes `:space/ground` the outermost rung
   of every runtime claim chain; its rows still live only on the code floor."
  #{:block/user-hit-area
    :block/machine-hit-area
    :block/fold-header
    :space/ground})

(def site-arg-keys
  "editable-material P6 · T10 — the ONE kernel place where each site declares
   which claim-arg keys it can feed a verb.

   A claim is built at render time from what the rendered node IS, so what it
   can supply is a property of the SITE, not of the row that lands on it. The
   fold header's node knows its own section; a block's hit area knows only the
   block. Pairing this with `verb-registry/required-args` is what makes the P5
   gate's arg-starved rebind a GRAMMAR error instead of a silent no-op.

   Widening a site's supply is a kernel change (the claim builder must actually
   carry the new key) plus a grammar version — never a silent one."
  {:block/user-hit-area #{}
   :block/machine-hit-area #{}
   :block/fold-header #{:section :fold-key}
   :space/ground #{}})

(def instance-legal-sites
  "editable-material P6 · G10 — the BLOCK sites where an INSTANCE-tier row may
   be filed for any owner.

   The var remains this enumeration because refusal cards and the legacy
   one-arity predicate describe the owner-independent block surface.
   `:space/ground` is legal only through `instance-site-legal?`'s owner-aware
   arity, and only for the space itself."
  #{:block/user-hit-area
    :block/machine-hit-area
    :block/fold-header})

(defn instance-site-legal?
  "G10 legality, with the owner named at every lifted write/read lane.

   One arity deliberately retains the old block-only meaning so an unswept
   caller fails closed. Two arities make `:space/ground` legal only for the
   space facet/claim owner; the shared `:space` name is intentional."
  ([site]
   (contains? instance-legal-sites site))
  ([site owner]
   ;; T-R3 — a wholesale site lift would let non-space facets mint dead rows.
   (or (contains? instance-legal-sites site)
       (and (= :space/ground site)
            (= :space owner)))))

(defn camera-gesture-reserved?
  "T3/T4 — true when a MATERIAL row attempts to capture a camera gesture.

   This single predicate var is the reservation set's one home. Master and
   instance material both call it; code-floor rows deliberately do not."
  [site row]
  (and (= :space/ground site)
       (contains?
        #{[:pointer/press :threshold #{}]
          [:pointer/press :threshold :any]
          [:wheel :complete #{}]
          [:wheel :complete :any]}
        [(:binding/gesture row)
         (:binding/phase row)
         (:binding/modifiers row)])))

(defn meta-gesture-reserved?
  "Halo P1 · H2 — true when a MATERIAL row attempts to capture the
   context-menu/meta gesture.

   Unlike camera reservation this is universal: every site and every material
   tier is barred. The site argument is retained so every reservation lane has
   the same call shape and callers cannot accidentally omit where the row was
   filed. Code-floor validation passes `bindable?` false and remains the only
   place this gesture can be declared."
  [_site row]
  (= [:pointer/meta :complete]
     [(:binding/gesture row) (:binding/phase row)]))

(def tier-order
  "Precedence within one containment depth."
  [:instance :master :floor])

(def row-keys
  #{:binding/gesture
    :binding/phase
    :binding/modifiers
    :binding/verb
    :binding/priority})

;; ===========================================================================
;; Row validation — usable directly as a facet-master grammar validator
;; ===========================================================================

(defn- valid-verb-ref?
  [x bindable?]
  (and (map? x)
       (= #{:verb/name :verb/version} (set (keys x)))
       (keyword? (:verb/name x))
       (integer? (:verb/version x))
       (if bindable?
         (verb-registry/bindable? (:verb/name x) (:verb/version x))
         (verb-registry/known? (:verb/name x) (:verb/version x)))))

(defn site-can-feed?
  "T10 — can a claim at this site supply everything the row's verb requires?

   An unknown site answers false: a row filed somewhere the kernel never builds
   a claim could not fire anyway, and saying so here is cheaper than leaving it
   in the table looking live."
  [site row]
  (let [supplied (get site-arg-keys site)
        required (verb-registry/required-args
                  (get-in row [:binding/verb :verb/name]))]
    (and (some? supplied)
         (every? supplied required))))

(defn valid-row?
  "One row against the closed grammar. `bindable?` true (the material path)
   additionally refuses floor-reserved verbs; the code floor passes false.

   The 3-arity adds the T10 site check. `valid-bindings?` always knows the site
   a row is filed under, so it always uses that arity — the 2-arity remains the
   site-agnostic shape check for callers that genuinely have no site."
  ([row] (valid-row? row true))
  ([row bindable?]
   (and (map? row)
        (= row-keys (set (keys row)))
        (contains? gesture-kinds (:binding/gesture row))
        (contains? gesture-phases (:binding/phase row))
        (contains? legal-gestures
                   [(:binding/gesture row) (:binding/phase row)])
        (let [m (:binding/modifiers row)]
          (or (= :any m)
              (and (set? m) (every? modifier-keys m))))
        (valid-verb-ref? (:binding/verb row) bindable?)
        (or (not bindable?)
            (not (meta-gesture-reserved? nil row)))
        (integer? (:binding/priority row))))
  ([row bindable? site]
   (and (valid-row? row bindable?)
        (site-can-feed? site row))))

(defn- bindings-ok?
  [bindings bindable? site-check?]
  (and (map? bindings)
       (seq bindings)
       (every?
        (fn [[site rows]]
          (and (contains? sites site)
               (vector? rows)
               (seq rows)
               (every? #(if site-check?
                          (valid-row? % bindable? site)
                          (valid-row? % bindable?))
                       rows)
               ;; the same (gesture, phase, modifiers) twice in ONE facet at
               ;; ONE site is an authoring mistake, not a cross-facet
               ;; conflict: refuse it here rather than lint it forever
               (apply distinct?
                      (map (juxt :binding/gesture
                                 :binding/phase
                                 :binding/modifiers)
                           rows))))
        bindings)))

(defn valid-bindings?
  "A facet's whole `:facet-master/bindings` value: site → rows. Total — never
   throws, so a hostile candidate becomes an error card, not an exception.

   GRAMMAR v1 semantics, frozen. P6 does NOT strengthen this function: durable
   v1 revisions are read under the v1 declaration forever, and re-reading them
   through a stricter validator is exactly the reinterpretation P3's per-version
   grammar map exists to make structurally impossible. The strengthening lives
   in `valid-bindings-strict?` under a NEW grammar version."
  ([bindings] (valid-bindings? bindings true))
  ([bindings bindable?] (bindings-ok? bindings bindable? false)))

(defn valid-bindings-strict?
  "GRAMMAR v2 (P6 · T10) = v1 plus the site-can-feed-the-verb check. Additive:
   every row v2 accepts, v1 accepted too. The twenty shipped rows all pass —
   the only rows this refuses are the ones that could never have worked."
  ([bindings] (valid-bindings-strict? bindings true))
  ([bindings bindable?] (bindings-ok? bindings bindable? true)))

(def bindings-validator
  "Drop-in `:validators` entry for a grammar carrying rows — v1 semantics."
  {:valid? valid-bindings?
   :error-type :facet-master/bindings-invalid})

(def strict-bindings-validator
  "Drop-in `:validators` entry for a v2 grammar — v1 plus the T10 site check."
  {:valid? valid-bindings-strict?
   :error-type :facet-master/bindings-invalid})

;; ===========================================================================
;; The normalized gesture
;; ===========================================================================

(defn normalize-gesture
  "Raw input → the normalized gesture the law matches on. Total: an unknown
   kind/phase pair yields nil and the kernel does nothing, rather than
   fabricating a gesture no row could name."
  [kind phase modifiers]
  (let [mods (into #{} (filter modifier-keys) (or modifiers #{}))]
    (when (contains? legal-gestures [kind phase])
      {:gesture/kind kind
       :gesture/phase phase
       :gesture/modifiers mods})))

(defn matches?
  [row gesture]
  (and (= (:binding/gesture row) (:gesture/kind gesture))
       (= (:binding/phase row) (:gesture/phase gesture))
       (let [m (:binding/modifiers row)]
         (or (= :any m)
             (= m (:gesture/modifiers gesture))))))

;; ===========================================================================
;; The one dispatch law
;; ===========================================================================

(defn- specificity
  "0 = the row names its modifiers exactly, 1 = it accepts any. An exact row
   beats an `:any` row at equal priority WITHOUT lint: that is a declared
   refinement, not an ambiguity. Two exact rows at equal priority genuinely
   are ambiguous, and those are the ties that lint."
  [row]
  (if (= :any (:binding/modifiers row)) 1 0))

(defn- row-order
  "The deterministic total order. Priority first (ascending — the same
   convention `facet-material/compose` already uses for contributions), then
   specificity, then facet and verb identity so the winner never depends on map
   iteration order."
  [{:keys [facet row]}]
  [(:binding/priority row)
   (specificity row)
   (str facet)
   (str (get-in row [:binding/verb :verb/name]))
   (get-in row [:binding/verb :verb/version])])

(defn- candidates-at
  "Every matching row visible from ONE claim, tagged with its tier and facet."
  [{:keys [claim gesture facet-rows floor-rows instance-rows]}]
  (let [subject (:claim/subject claim)
        site (:claim/site claim)
        facets (:claim/facets claim)]
    (concat
     (for [row (get instance-rows [subject site])
           :when (matches? row gesture)]
       {:tier :instance :facet nil :row row})
     (for [facet facets
           row (get-in facet-rows [facet site])
           :when (matches? row gesture)]
       {:tier :master :facet facet :row row})
     (for [facet facets
           row (get-in floor-rows [facet site])
           :when (matches? row gesture)]
       {:tier :floor :facet facet :row row}))))

(defn- conflict-at
  [tied {:keys [claim gesture depth tier winner]}]
  {:type :material-binding/priority-tie
   :binding/site (:claim/site claim)
   :binding/subject (:claim/subject claim)
   :binding/gesture [(:gesture/kind gesture)
                     (:gesture/phase gesture)
                     (vec (sort (:gesture/modifiers gesture)))]
   :binding/depth depth
   :binding/tier tier
   :binding/priority (get-in winner [:row :binding/priority])
   :binding/facets (vec (sort-by str (map :facet tied)))
   :binding/verbs
   (vec (sort-by str (map #(get-in % [:row :binding/verb :verb/name]) tied)))
   :binding/winner (get-in winner [:row :binding/verb])})

(defn- decide-at
  "Resolve one containment depth, or nil to keep walking outward."
  [{:keys [claim gesture depth] :as ctx}]
  (let [cands (candidates-at ctx)]
    (when (seq cands)
      (let [by-tier (group-by :tier cands)
            tier (first (filter by-tier tier-order))
            ranked (vec (sort-by row-order (get by-tier tier)))
            winner (first ranked)
            tied (filterv
                  (fn [c]
                    (and (= (get-in c [:row :binding/priority])
                            (get-in winner [:row :binding/priority]))
                         (= (specificity (:row c))
                            (specificity (:row winner)))))
                  ranked)]
        {:decision/outcome :claimed
         :decision/verb (get-in winner [:row :binding/verb])
         :decision/effect-class
         (verb-registry/effect-class
          (get-in winner [:row :binding/verb :verb/name]))
         :decision/subject (:claim/subject claim)
         :decision/site (:claim/site claim)
         :decision/args (or (:claim/args claim) {})
         :decision/depth depth
         :decision/tier tier
         :decision/facet (:facet winner)
         :decision/row (:row winner)
         :decision/conflicts
         (if (> (count tied) 1)
           [(conflict-at tied (assoc ctx :tier tier :winner winner))]
           [])}))))

(defn resolve-binding
  "THE dispatch law. Pure.

   `:gesture`       normalized gesture (nil ⇒ nothing to dispatch)
   `:claims`        the containment chain, INNERMOST FIRST. Each claim:
                    {:claim/subject s :claim/site kw :claim/facets [kw…]
                     :claim/args m}
   `:facet-rows`    facet-kw → {site → [row…]} from the SERVED active material
   `:floor-rows`    facet-kw → {site → [row…]} from the facet's CODE FLOOR
   `:instance-rows` [subject site] → [row…]

   Returns a decision map. `:decision/outcome` is `:claimed` or `:unclaimed`;
   an unclaimed gesture is inert — the kernel does nothing at all with it."
  [{:keys [gesture claims] :as ctx}]
  (if (nil? gesture)
    {:decision/outcome :unclaimed
     :decision/verb nil
     :decision/conflicts []
     :decision/reason :gesture/unnormalized}
    (or (some
         (fn [[depth claim]]
           (decide-at (assoc ctx :claim claim :depth depth)))
         (map-indexed vector claims))
        {:decision/outcome :unclaimed
         :decision/verb nil
         :decision/conflicts []
         :decision/reason :claim/no-matching-row
         :decision/gesture gesture
         :decision/sites (mapv :claim/site claims)})))

;; ===========================================================================
;; The space's code floor (family 4)
;; ===========================================================================

(defn- floor-row
  [gesture phase modifiers verb-name priority]
  {:binding/gesture gesture
   :binding/phase phase
   :binding/modifiers modifiers
   :binding/verb {:verb/name verb-name :verb/version 0}
   :binding/priority priority})

(def halo-floor-bindings
  "Halo P1 · H2 — the complete, exact code-floor ownership of
   `:pointer/meta`. There is one row at each named interaction site and no
   material tier may shadow it."
  {:attention
   {:block/user-hit-area
    [(floor-row :pointer/meta :complete :any :halo/condense 0)]
    :block/machine-hit-area
    [(floor-row :pointer/meta :complete :any :halo/condense 0)]}
   :foldable
   {:block/fold-header
    [(floor-row :pointer/meta :complete :any :halo/condense 0)]}
   :space
   {:space/ground
    [(floor-row :pointer/meta :complete :any :halo/condense 0)]}})

(defn with-halo-floor-bindings
  "Add Halo's four reserved rows to a facet → site → rows floor table.

   This is the one augmenter used by both runtime dispatch and the served
   interaction table. It is idempotent so a caller cannot duplicate a
   reserved row by composing already-augmented tables."
  [floor-bindings]
  (reduce-kv
   (fn [by-facet facet by-site]
     (reduce-kv
      (fn [by-facet* site rows]
        (update-in by-facet* [facet site]
                   (fn [existing]
                     (vec (distinct (concat (or existing []) rows))))))
      by-facet
      by-site))
   (or floor-bindings {})
   halo-floor-bindings))

(def space-facet
  "The facet the outermost space rung's floor rows are filed under. There is no
   served `fm:space` master in rung 1, so the space has a FLOOR tier and no
   material tier."
  :space)

(def space-floor-master-id
  "The space floor rows' deciding revision. `fm:space` now derives the same
   label through its registered spec; the kernel keeps the value beside the
   special camera-inclusive floor table so every pre-spec reader moves with it."
  "code-floor:fm:space:v0")

(def space-floor-bindings
  "The space's rows, code side, forever (per the package: camera and space
   bindings last, always on the code floor). `:camera/pan` and
   `:camera/zoom-at-pointer` are additionally floor-reserved in the verb
   registry, so even a future `fm:space` master could not bind them."
  {:space/ground
   [(floor-row :pointer/tap :complete :any :anchor/place 0)
    (floor-row :pointer/press :threshold #{} :camera/pan 0)
    (floor-row :pointer/press :threshold #{:shift}
               :selection/marquee-begin 0)
    (floor-row :wheel :complete :any :camera/zoom-at-pointer 0)]})

(def space-claim
  "The one-element outermost rung appended to every runtime claim chain."
  [{:claim/subject :space
    :claim/site :space/ground
    :claim/facets [space-facet]
    :claim/args {}}])

;; ===========================================================================
;; The floor drill — one probe set, used by the suite AND the live console
;; ===========================================================================

(def block-claim-facets
  "The facets a rendered block's hit-area claim names. Kept here so the drill
   and the suite describe the same block the renderer builds."
  [:attention :positioned])

(def floor-drill-probes
  "Every gesture the floor must still answer after any data revision. The
   fence names space pan/zoom and click-focus; the rest are here because a
   drill that only checks the three named ones proves less than it looks."
  [{:probe/label "tap a user block → focus"
    :probe/kind :pointer/tap :probe/phase :complete :probe/modifiers #{}
    :probe/site :block/user-hit-area}
   {:probe/label "shift-press a user block → focus in the same gesture"
    :probe/kind :pointer/press :probe/phase :begin :probe/modifiers #{:shift}
    :probe/site :block/user-hit-area}
   {:probe/label "drag a user block"
    :probe/kind :pointer/press :probe/phase :threshold :probe/modifiers #{}
    :probe/site :block/user-hit-area}
   {:probe/label "shift-drag a user block → text selection"
    :probe/kind :pointer/press :probe/phase :threshold
    :probe/modifiers #{:shift}
    :probe/site :block/user-hit-area}
   {:probe/label "tap a machine block → release focus"
    :probe/kind :pointer/tap :probe/phase :complete :probe/modifiers #{}
    :probe/site :block/machine-hit-area}
   {:probe/label "drag a machine block"
    :probe/kind :pointer/press :probe/phase :threshold :probe/modifiers #{}
    :probe/site :block/machine-hit-area}
   {:probe/label "tap a fold header → toggle its section"
    :probe/kind :pointer/tap :probe/phase :complete :probe/modifiers #{}
    :probe/site :block/fold-header}
   {:probe/label "tap empty space → caret anchor"
    :probe/kind :pointer/tap :probe/phase :complete :probe/modifiers #{}
    :probe/site :space/ground}
   {:probe/label "drag empty space → pan the camera"
    :probe/kind :pointer/press :probe/phase :threshold :probe/modifiers #{}
    :probe/site :space/ground}
   {:probe/label "shift-drag empty space → marquee"
    :probe/kind :pointer/press :probe/phase :threshold
    :probe/modifiers #{:shift}
    :probe/site :space/ground}
   {:probe/label "wheel → zoom at the pointer"
    :probe/kind :wheel :probe/phase :complete :probe/modifiers #{}
    :probe/site :space/ground}
   {:probe/label "wheel at a block → zoom through the space rung"
    :probe/kind :wheel :probe/phase :complete :probe/modifiers #{}
    :probe/site :block/user-hit-area}
   {:probe/label "meta a user block → condense"
    :probe/kind :pointer/meta :probe/phase :complete :probe/modifiers #{}
    :probe/site :block/user-hit-area}
   {:probe/label "meta a machine block → condense"
    :probe/kind :pointer/meta :probe/phase :complete :probe/modifiers #{}
    :probe/site :block/machine-hit-area}
   {:probe/label "meta a fold header → condense"
    :probe/kind :pointer/meta :probe/phase :complete :probe/modifiers #{}
    :probe/site :block/fold-header}
   {:probe/label "meta empty space → condense"
    :probe/kind :pointer/meta :probe/phase :complete :probe/modifiers #{}
    :probe/site :space/ground}])

(defn probe-claims
  "The runtime-shaped claim chain a probe resolves against. Every chain ends
   at space; `:block/fold-header` is header → block → space."
  [site subject]
  (let [claims
        (case site
          :space/ground []
          :block/fold-header
          [{:claim/subject subject
            :claim/site :block/fold-header
            :claim/facets [:foldable]
            :claim/args {:section :noise :fold-key :noise?}}
           {:claim/subject subject
            :claim/site :block/machine-hit-area
            :claim/facets block-claim-facets
            :claim/args {}}]
          [{:claim/subject subject
            :claim/site site
            :claim/facets block-claim-facets
            :claim/args {}}])]
    ;; T1 — keep the drill honest about the runtime's into-append shape.
    (into claims space-claim)))

(defn drill-report
  "Resolve every probe against the supplied tiers and report the verb, tier and
   master each one lands on. Pure — the live console drill and the suite call
   this with the same arguments and must get the same answer."
  [{:keys [facet-rows floor-rows instance-rows subject]
    :or {subject "du:drill"}}]
  (mapv
   (fn [probe]
     (let [decision
           (resolve-binding
            {:gesture (normalize-gesture (:probe/kind probe)
                                         (:probe/phase probe)
                                         (:probe/modifiers probe))
             :claims (probe-claims (:probe/site probe) subject)
             :facet-rows facet-rows
             :floor-rows floor-rows
             :instance-rows instance-rows})]
       {:probe/label (:probe/label probe)
        :probe/site (:probe/site probe)
        :probe/decision-site (:decision/site decision)
        :probe/depth (:decision/depth decision)
        :probe/gesture [(:probe/kind probe)
                        (:probe/phase probe)
                        (vec (sort (:probe/modifiers probe)))]
        :probe/verb (get-in decision [:decision/verb :verb/name])
        :probe/tier (:decision/tier decision)
        :probe/facet (:decision/facet decision)
        :probe/outcome (:decision/outcome decision)}))
   floor-drill-probes))

;; ===========================================================================
;; The interaction table (served projection + console listing)
;; ===========================================================================

(defn table-rows
  "Flatten binding tables into the served interaction table's rows: one row per
   (gesture × site × facet → verb), each carrying its master link so a reader
   can walk from a gesture to the revision that decided it.

   `tables` is a vector of
     {:tier … :facet … :master-id … :revision-id … :floor? … :bindings …}
   and the result is sorted by gesture, so the table reads as an interaction
   grammar rather than a dump of per-facet policy."
  [tables]
  (->> tables
       (mapcat
        (fn [{:keys [tier facet master-id revision-id floor? bindings]}]
          (for [[site rows] bindings
                row rows]
            {:table/gesture (:binding/gesture row)
             :table/phase (:binding/phase row)
             :table/modifiers (if (= :any (:binding/modifiers row))
                                :any
                                (vec (sort (:binding/modifiers row))))
             :table/site site
             :table/tier tier
             :table/facet facet
             :table/master-id master-id
             :table/revision-id revision-id
             :table/floor? (true? floor?)
             :table/verb (get-in row [:binding/verb :verb/name])
             :table/verb-version (get-in row [:binding/verb :verb/version])
             :table/effect-class
             (verb-registry/effect-class
              (get-in row [:binding/verb :verb/name]))
             :table/priority (:binding/priority row)})))
       (sort-by
        (juxt (comp str :table/gesture)
              (comp str :table/phase)
              (comp str :table/modifiers)
              (comp str :table/site)
              (comp str :table/tier)
              :table/priority
              (comp str :table/facet)
              (comp str :table/verb)))
       vec))

(defn table-conflicts
  "Every same-site, same-tier, same-priority, same-specificity collision the
   served table contains — the lint the land renders, computed once over the
   whole table instead of only when a gesture happens to fire."
  [rows]
  (->> rows
       (group-by
        (juxt :table/gesture :table/phase :table/modifiers
              :table/site :table/tier :table/priority))
       (keep
        (fn [[[gesture phase modifiers site tier priority] group]]
          (when (> (count group) 1)
            {:type :material-binding/priority-tie
             :binding/gesture [gesture phase modifiers]
             :binding/site site
             :binding/tier tier
             :binding/priority priority
             :binding/facets
             (vec (sort-by str (map :table/facet group)))
             :binding/verbs
             (vec (sort-by str (map :table/verb group)))
             :binding/winner
             (-> (sort-by
                  (juxt (comp str :table/facet) (comp str :table/verb))
                  group)
                 first
                 :table/verb)})))
       (sort-by (juxt (comp str :binding/gesture)
                      (comp str :binding/site)))
       vec))
