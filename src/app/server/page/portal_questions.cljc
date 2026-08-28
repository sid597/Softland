(ns app.server.page.portal-questions
  "editable-material P7 — the portal, pure half.

   The portal is a DETERMINISTIC, TOTAL, BATCHED PROJECTION of one pick. This
   namespace holds everything about it that needs no runtime: the question list
   (as DATA, so `answered` is checkable instead of claimed), recipe resolution
   from observed composition, why-this-pixel, the truncation roll-up, the
   resident briefing, and the CODE FLOOR rendering.

   Three laws are structural here, not aspirational:

   1. **No LLM in the projection path, ever.** Nothing in this namespace or its
      server half calls a model. The briefing a summoned resident receives is
      `briefing`, a pure function of the projection — the resident reads exactly
      what the human reads, byte for byte (G7).

   2. **Describe, never gate** (DIRECTION §Paradigm rulings, THE GATE TEST). No
      function here refuses an entity. An untyped, unknown, absent or malformed
      subject projects — with named absences instead of an error. `recipe` is
      the sharp end: it is exhaust, it describes what has recurred, and no
      rendering, dispatch or write consults it.

   3. **The floor is code.** `render-model` derives its card set and card ORDER
      from `questions` — a compile-time value — so no data revision can change
      how many cards the portal has or which order they come in. A garbage
      projection yields the same cards carrying named errors (G9's drill). This
      is why this namespace may not require the server: the floor cannot be
      allowed to depend on anything a revision can reach."
  (:require [clojure.string :as str]))

(def portal-version 0)

;; ===========================================================================
;; matter-room P1 — pure master-anchor laws
;; ===========================================================================

(def not-applicable-at-anchor
  "The explicit answer for values whose question presupposes an entity pick.

   This is deliberately truthy. Renderers must test real booleans with `true?`
   rather than treating this sentinel as one (matter-room T-R2-3)."
  :not-applicable-at-anchor)

(def master-here-keys
  "Every master value whose meaning presupposes an entity at `here`."
  [:master/pinned-here?
   :master/tier-here
   :master/worn-here-revision-id
   :master/floored-here?
   :master/holds-here])

(def deviation-here-keys
  "Every deviation value whose meaning presupposes an entity at `here`."
  [:deviations/here
   :deviations/here-count
   :deviations/pins-here])

(def wearer-here-keys
  "Every wearer value whose meaning presupposes an entity at `here`."
  [:wearers/here])

(defn master-anchor?
  "A master anchor is selected only when `:master-id` is present and the entity
   address is absent. Supplying both preserves the established entity mode."
  [{:keys [entity-id master-id]}]
  (and (some? master-id) (nil? entity-id)))

(defn master-anchor-identity
  "The identity section for one facet-master address.

   `spec` and `floor-master-id` are passed in so this pure/client-safe namespace
   does not acquire a server or registry dependency. Registry presence, not an
   ObjectContainer lookup, decides `found?`."
  [master-id spec floor-master-id]
  {:entity/id master-id
   :entity/found? (some? spec)
   :entity/kind :facet-master
   :entity/facet (:facet-master/facet spec)
   :entity/master-id master-id
   :entity/floor-master-id floor-master-id
   :entity/document-container-id nil
   :entity/source-id (:facet-master/source-ref spec)
   :entity/target-kind :facet-master
   :entity/target-id master-id
   :entity/addressable? (boolean (and (string? master-id)
                                      (seq master-id)))})

(defn- sentinel-values
  [m ks]
  (reduce #(assoc %1 %2 not-applicable-at-anchor) (or m {}) ks))

(defn masters-at-anchor
  "Replace every entity-relative `here` fact in each per-master map."
  [masters]
  (into (sorted-map)
        (map (fn [[master-id m]]
               [master-id (sentinel-values m master-here-keys)]))
        masters))

(defn deviations-at-anchor
  "Replace every entity-relative deviation fact without erasing world totals."
  [deviations]
  (sentinel-values deviations deviation-here-keys))

(defn wearers-at-anchor
  "Declare both the absent `here` and the absent wearer-snapshot basis."
  [wearers]
  (-> (sentinel-values wearers wearer-here-keys)
      (assoc :wearers/basis :no-wearer-snapshot-at-anchor)))

(defn blast-at-anchor
  "Remove confident-zero claims from an anchor opened without wearer evidence.

   The override is repeated on every per-master value because that is the map
   the rendered card reads; a section-only basis would leave those rows lying."
  [blast]
  (-> (or blast {})
      (assoc :blast/basis :no-wearer-snapshot-at-anchor
             :blast/counted-over nil)
      (update :blast/by-master
              (fn [by-master]
                (into (sorted-map)
                      (map (fn [[master-id m]]
                             [master-id
                              (assoc m
                                     :blast/basis
                                     :no-wearer-snapshot-at-anchor
                                     :blast/counted-over nil)]))
                      (or by-master {}))))))

;; ===========================================================================
;; The question list — DIRECTION's portal questions, as data
;;
;; DIRECTION never numbered these; it stated them as capabilities across
;; §The layer, §The architecture (render provenance), §Aliveness laws
;; (announcement scales, case reports, standable history) and §Records. P7's
;; prompt enumerates the same set as deliverables. Making the list a value is
;; what turns "the portal answers these" into a gate: `unanswered` walks every
;; `:question/answers-at` path against a real projection, and G1 fails on any
;; question the portal only claims to answer.
;;
;; `:question/call-jvm` and `:question/call-console` are REPLAYABLE — paste-able
;; verbatim, one per question, which is P7's required output form.
;; ===========================================================================

(def ^:private jvm-open
  "(portal/open rt {:entity-id ENTITY :wearers WEARERS})")

(def questions
  "Every question the portal answers, its answer path, and the exact calls that
   replay it. Order is the reading order of the portal itself and the card order
   of the floor rendering."
  [{:question/id :identity
    :question/ask "What is this?"
    :question/answers-at [:portal/identity]
    :question/title "Identity"
    :question/direction "§The layer — durable entity: the block itself"
    :question/call-jvm jvm-open
    :question/call-console "await __portal.open()"}

   {:question/id :recipe
    :question/ask "What is it made of — has this composition recurred?"
    :question/answers-at [:portal/recipe]
    :question/title "Recipe"
    :question/direction "§The layer — recipe: the recurring composition, emerges by recurrence"
    :question/call-jvm jvm-open
    :question/call-console "(await __portal.open())['portal/recipe']"}

   {:question/id :attachments
    :question/ask "Which facts are attached to it, and on whose authority?"
    :question/answers-at [:portal/attachments]
    :question/title "Facet attachments"
    :question/direction "§The layer — facet attachment: this subject wears this facet, these values, this authority"
    :question/call-jvm jvm-open
    :question/call-console "(await __portal.open())['portal/attachments']"}

   {:question/id :masters
    :question/ask "Which material decides each fact — and is latest the same as active?"
    :question/answers-at [:portal/masters]
    :question/title "Masters + revisions"
    :question/direction "§The architecture — latest ≠ candidate ≠ previewed ≠ active ≠ pinned ≠ previous"
    :question/call-jvm jvm-open
    :question/call-console "(await __portal.open())['portal/masters']"}

   {:question/id :bindings
    :question/ask "What does touching it do, and which revision decided that?"
    :question/answers-at [:portal/bindings]
    :question/title "Bindings + verbs"
    :question/direction "§The architecture — four stations of input; verbs carry effect classes"
    :question/call-jvm jvm-open
    :question/call-console "(await __portal.open())['portal/bindings']"}

   {:question/id :wearers
    :question/ask "Who else wears this material, and where does it appear?"
    :question/answers-at [:portal/wearers]
    :question/title "Wearers + appearances"
    :question/direction "§The layer — appearance: one rendering (ground, portal copy)"
    :question/call-jvm jvm-open
    :question/call-console "(await __portal.open())['portal/wearers']"}

   {:question/id :deviations
    :question/ask "Where does this instance differ from what it inherits?"
    :question/answers-at [:portal/deviations]
    :question/title "Deviations + pins"
    :question/direction "§Aliveness laws — deviation cheap, scoped, visible, queryable"
    :question/call-jvm jvm-open
    :question/call-console "(await __portal.open())['portal/deviations']"}

   {:question/id :activation-history
    :question/ask "How did the worn material come to be worn, by whom, on what grounds?"
    :question/answers-at [:portal/activation-history]
    :question/title "Activation history"
    :question/direction "§Aliveness laws — case reports are derived, never authored"
    :question/call-jvm jvm-open
    :question/call-console "(await __portal.open())['portal/activation-history']"}

   {:question/id :experience
    :question/ask "What has been experienced around this, and where did each item come from?"
    :question/answers-at [:portal/experience]
    :question/title "Experience"
    :question/direction "§Records — receipt/silver/gold; edges travel both ways"
    :question/call-jvm jvm-open
    :question/call-console "(await __portal.open())['portal/experience']"}

   {:question/id :lint
    :question/ask "Where is the material colliding or lying?"
    :question/answers-at [:portal/lint]
    :question/title "Conflicts + lint"
    :question/direction "§The architecture — same-slot conflicts render as lint; silent winner never"
    :question/call-jvm jvm-open
    :question/call-console "(await __portal.open())['portal/lint']"}

   {:question/id :truncation
    :question/ask "What am I NOT being shown?"
    :question/answers-at [:portal/truncation]
    :question/title "Truncation"
    :question/direction "§Aliveness laws — gauges expose window, denominator, coverage, truncation"
    :question/call-jvm jvm-open
    :question/call-console "(await __portal.open())['portal/truncation']"}

   {:question/id :why
    :question/ask "Why THIS pixel?"
    :question/answers-at [:portal/why]
    :question/title "Why this pixel"
    :question/direction "§The architecture — render provenance grows from src-path to causal trace"
    :question/call-jvm
    "(portal/open rt {:entity-id ENTITY :wearers WEARERS :why STAMP})"
    :question/call-console
    "await __portal.why('{:material/master \"fm:attention\" :material/site :block/user-hit-area}')"}

   {:question/id :blast
    :question/ask "What would this activation be felt by, before I do it?"
    :question/answers-at [:portal/blast]
    :question/title "Blast radius"
    :question/direction "§The architecture — propagation by reference, never fan-out writes"
    :question/call-jvm
    "(portal/open rt {:entity-id ENTITY :wearers WEARERS :scope [:scope/all-unpinned]})"
    :question/call-console "await __portal.blast('[:scope/all-unpinned]')"}

   {:question/id :history
    :question/ask "What did this world look like before?"
    :question/answers-at [:portal/history]
    :question/title "Historical cut"
    :question/direction "§Aliveness laws — history is standable"
    :question/call-jvm
    "(portal/open rt {:entity-id ENTITY :wearers WEARERS :cut {MASTER-ID POINTER-REVISION-ID}})"
    :question/call-console
    "await __portal.at('{\"fm:text-body\" \"<pointer-revision-id>\"}')"}

   {:question/id :recovery
    :question/ask "If the worn revision is bad, what do I go back to and how?"
    :question/answers-at [:portal/recovery]
    :question/title "Previous-revision recovery"
    :question/direction "§The layer — previous revision always rewearable (the dull floor)"
    :question/call-jvm jvm-open
    :question/call-console "(await __portal.open())['portal/recovery']"}

   {:question/id :chrome
    :question/ask "Which material decides the portal's own look — the strange loop?"
    :question/answers-at [:portal/chrome]
    :question/title "Portal chrome"
    :question/direction "§The layer — the portal is itself an instance of Softland space"
    :question/call-jvm jvm-open
    :question/call-console "(await __portal.open())['portal/chrome']"}

   {:question/id :briefing
    :question/ask "What does a resident summoned in here know?"
    :question/answers-at [:portal/briefing-of]
    :question/title "Resident briefing"
    :question/direction "§The layer — the resident works INSIDE a layer, briefed by the same projection the human sees"
    :question/call-jvm "(portal/briefing (portal/open rt {...}))"
    :question/call-console "await __portal.briefing()"}])

(def question-ids
  (mapv :question/id questions))

(def question-by-id
  (into {} (map (juxt :question/id identity)) questions))

(defn- safe-get-in
  "`get-in` that cannot throw. The floor's card set is derived from probes like
   this one, so a value with a hostile lookup must degrade to `not present`
   rather than take the rendering down (G9's drill)."
  [m path]
  (try
    (get-in m path ::missing)
    (catch #?(:clj Throwable :cljs :default) _ ::missing)))

(defn unanswered
  "Every question whose answer path is absent from `result`. THE gate for
   \"answered one by one\": a portal that grew a section but forgot to wire it,
   or that dropped a section on an error path, shows up here as a value.

   Absent means the key is missing. A section that is present and says
   `not-applicable` or carries an error card IS an answer — the portal's job is
   to never be silent, not to always have good news."
  [result]
  (->> questions
       (remove (fn [q]
                 (not= ::missing (safe-get-in result (:question/answers-at q)))))
       (mapv :question/id)))

(defn answered?
  [result]
  (empty? (unanswered result)))

;; ===========================================================================
;; Canonical bytes
;; ===========================================================================

(defn- compare-edn
  [a b]
  (compare (pr-str a) (pr-str b)))

(defn canonicalize
  "Remove hash iteration order from an EDN value: maps become pr-str-key-sorted
   maps, sets become sorted vectors, other collections become vectors."
  [x]
  (cond
    (map? x)
    (into (sorted-map-by compare-edn)
          (map (fn [[k v]] [k (canonicalize v)]))
          x)

    (set? x)
    (->> x (map canonicalize) (sort-by pr-str) vec)

    (coll? x)
    (mapv canonicalize x)

    :else x))

(defn canonical-edn
  "Canonical bytes for one projection.

   `*print-namespace-maps*` is pinned FALSE (P6's determinism trap, verbatim):
   the var is TRUE at a REPL and FALSE in a plain program, so an all-one-
   namespace map prints as `#:portal{…}` from a REPL and `{:portal/… }` from the
   server — two byte strings for one value. The portal's bytes are compared for
   equality (G7 briefing identity, G2 determinism), so the flag cannot be left
   to the caller's environment."
  [x]
  #?(:clj (binding [*print-namespace-maps* false]
            (pr-str (canonicalize x)))
     :cljs (pr-str (canonicalize x))))

;; ===========================================================================
;; Composition + recipe — exhaust, described, never gating
;; ===========================================================================

(defn composition
  "The facet composition ONE wearer's rendered contributions stamped.

   Evidence, not declaration: a stamp names the PARENT master even when the
   instance tier won (`wear-for-subject` sets `:facet-master/id` to the parent
   in every branch), so a deviating block belongs to the same composition as a
   conforming one — which is the whole point. A deviation is a variation of a
   type, not a different type."
  [wearer master->facet]
  (->> (:wearer/facets wearer)
       (keep #(get master->facet (:wearer/master-id %)))
       distinct
       (sort-by pr-str)
       vec))

(defn composition-id
  "A readable, deterministic id for one composition. Readable on purpose: a
   recipe is exhaust and its id is meant to be recognised in a log, not hashed
   into opacity."
  [facets]
  (if (seq facets)
    (str "recipe:" (str/join "+" (map name facets)))
    "recipe:none"))

(def ^:private worn-block-composition
  "The named block recipe follows the real wear census. P2 explicitly widens
   P7's historical five with the visible invocation master. `:provenance`
   remains the probe master rather than block type."
  #{:attention :foldable :invocation
    :positioned :threaded :text-body})

(defn recipe
  "Resolve the recipe from OBSERVED composition, and name it iff it recurred.

   P7: `NAME the block recipe here iff the worn-five composition has recurred —
   recipes are exhaust; if named, it describes and never gates.`

   Recurrence is measured as DISTRIBUTION, never count (DIRECTION §Aliveness:
   `three retries by one machine are not three precedents`): distinct entities
   carrying the composition is the number that matters, and it is reported
   beside the histogram of every composition in the basis.

   `:recipe/recurrence-class` is the honest half, and it exists because the
   naive reading of this data is wrong. Facet attachment is DERIVED today
   (`:attachment/authority :derived`, basis: rendered contribution stamps), so
   every rendered block necessarily wears the same facets. That makes
   recurrence trivially true and tells you about the CODE, not about anything
   the land chose:

     :derived-uniform — one composition across the whole basis. Recurred, named,
                        but this is code attaching uniformly. It is NOT
                        DIRECTION's deep proof of life (`the first unplanned
                        material type, condensed from recurrent lived
                        deviations, ratified by care`).
     :lived           — two or more compositions exist and this one recurs
                        across distinct entities. The land has started to
                        differentiate.
     :single          — one entity only. Nothing has recurred; no name.

   `:recipe/name-authority` is `:sid` because DIRECTION §Only-Sid holds naming:
   `names finalize by recurrence, Sid names`. The name minted here is working
   scaffolding — enough for the portal to say what this is, never a finalized
   term.

   `:recipe/gates?` is false, permanently, and is stated rather than assumed:
   THE GATE TEST asks whether a description ever refuses an instance. Nothing
   reads this map to decide whether to render, dispatch, or write."
  [{:keys [entity-id wearers master->facet]}]
  (let [pairs (mapv (fn [w]
                      [(:wearer/entity-id w) (composition w master->facet)])
                    wearers)
        ;; Masters stamped on the pick that this build cannot map to a facet.
        ;; `composition` drops them — correctly, since a composition is a set of
        ;; FACETS and an unknown master names none — but dropping them silently
        ;; would let two genuinely different compositions read as one. Named here
        ;; instead, the same way `:portal/masters` names them.
        unknown (->> wearers
                     (filter #(= entity-id (:wearer/entity-id %)))
                     (mapcat :wearer/facets)
                     (map :wearer/master-id)
                     (remove #(contains? master->facet %))
                     distinct
                     sort
                     vec)
        by-composition (reduce (fn [m [eid facets]]
                                 (update m facets (fnil conj #{}) eid))
                               {}
                               pairs)
        mine (some (fn [[eid facets]] (when (= entity-id eid) facets)) pairs)
        mine (or mine [])
        holders (get by-composition mine #{})
        distinct-entities (count holders)
        distinct-compositions (count by-composition)
        ;; an EMPTY composition shared by two entities is not a recurrence —
        ;; `nothing` recurring is still nothing, and calling it recurred would
        ;; put `recurred? true` next to `:recipe/facets []`
        recurred? (boolean (and (seq mine) (>= distinct-entities 2)))
        class (cond
                (not recurred?) :single
                (<= distinct-compositions 1) :derived-uniform
                :else :lived)
        ;; The wire keys retain their P7 names for projection compatibility;
        ;; their ruled meaning is now the widened worn block composition above.
        covers-worn-five? (every? (set mine) worn-block-composition)]
    {:recipe/id (composition-id mine)
     :recipe/facets mine
     :recipe/covers-worn-five? covers-worn-five?
     :recipe/missing-from-worn-five
     (vec (sort-by pr-str
                   (remove (set mine) worn-block-composition)))
     :recipe/recurred? recurred?
     :recipe/recurrence-class class
     ;; NAMED iff the worn-five composition has recurred — P7's condition,
     ;; both halves enforced. A partial composition that recurred is described
     ;; by its id and deliberately left unnamed: naming it `block` would claim a
     ;; type the evidence does not carry, and inventing a second name for a
     ;; fragment would put two names on one thing.
     :recipe/name (when (and recurred? covers-worn-five?) "block")
     :recipe/name-status (cond
                           (not recurred?) :unnamed-nothing-recurred
                           (not covers-worn-five?) :unnamed-partial-composition
                           :else :working-scaffolding)
     :recipe/name-authority :sid
     :recipe/gates? false
     :recipe/unknown-masters unknown
     :recipe/distribution
     {:distribution/distinct-entities distinct-entities
      :distribution/distinct-compositions distinct-compositions
      :distribution/basis-entities (count pairs)
      :distribution/entities (vec (sort holders))
      :distribution/histogram
      (->> by-composition
           (map (fn [[facets eids]]
                  {:composition/id (composition-id facets)
                   :composition/facets facets
                   :composition/entities (count eids)}))
           (sort-by :composition/id)
           vec)}
     ;; the basis is said out loud: current-scene evidence is not a durable
     ;; attachment table, and a recipe derived from it is only as wide as the
     ;; page that was open
     :recipe/basis :rendered-contribution-stamps}))

;; ===========================================================================
;; Why this pixel
;; ===========================================================================

(defn why-this-pixel
  "Answer `why this pixel` from ONE rendered contribution stamp.

   DIRECTION names the exact chain this must produce: `entity, placement,
   attachment, master revision, recipe revision (when one exists), override/pin
   that won, source path`. Every one of those is a key below, and each is
   present-or-explicitly-nil — a chain with a silent gap is not a causal trace.

   `:why/stale?` is the field this function exists for. The stamp records the
   revision that was rendered; the projection carries the revision currently
   worn. When they differ, the pixel on screen is older than the material — a
   fact no other query in the layer can state, because only the stamp remembers
   what was actually drawn."
  [{:keys [stamp spec-source-path wear diff pin recipe-id bindings placement]}]
  (let [stamp-revision (:material/revision stamp)
        worn-revision (:facet-master/revision-id wear)
        ;; The attachment is DERIVED (`[:derived facet subject]` —
        ;; `facet-material/contribution-stamp`), so a caller who asks about a
        ;; pixel without carrying the attachment along still gets the chain
        ;; whole. A nil link in a causal trace is a broken trace, and the caller
        ;; should not have to reconstruct a value the layer can compute.
        attachment (or (:material/attachment stamp)
                       (when (and (:facet-master/facet wear)
                                  (:material/subject stamp))
                         [:derived (:facet-master/facet wear)
                          (:material/subject stamp)]))]
    {:why/found? (boolean (and stamp wear))
     :why/stamp stamp
     ;; entity
     :why/entity (:material/subject stamp)
     ;; placement — the block in ONE world
     :why/placement placement
     ;; attachment
     :why/attachment attachment
     :why/attachment-derived? (nil? (:material/attachment stamp))
     :why/facet (:facet-master/facet wear)
     ;; master revision
     :why/master-id (:facet-master/id wear)
     :why/master-revision-id worn-revision
     :why/rendered-revision-id stamp-revision
     :why/stale? (boolean (and stamp-revision worn-revision
                               (not= stamp-revision worn-revision)))
     ;; recipe revision — nil until a recipe is a revisioned object, and saying
     ;; nil is the honest answer rather than borrowing the master's
     :why/recipe-id recipe-id
     :why/recipe-revision-id nil
     ;; the override or pin that won
     :why/tier (:facet-master/tier wear)
     :why/won-by (cond
                   (:facet-master/pin-unresolved? wear) :pin-unresolved-floor
                   (true? (:facet-master/pinned? wear)) :pin
                   (= :instance (:facet-master/tier wear)) :instance-deviation
                   (= :floor (:facet-master/tier wear)) :code-floor
                   :else :shared-active)
     :why/pin pin
     :why/deviation diff
     :why/floor? (true? (:facet-master/floor? wear))
     ;; source path
     :why/source-path spec-source-path
     ;; and what this pixel would DO if touched — the fourth station, from the
     ;; same served table, never a second read
     :why/site (:material/site stamp)
     :why/role (:material/role stamp)
     :why/slot (:material/slot stamp)
     :why/bindings-at-site (vec bindings)}))

;; ===========================================================================
;; Truncation roll-up
;; ===========================================================================

(defn truncation
  "One place that answers `what am I not being shown`.

   Sections declare their own limits; this collects every declaration into one
   list so a reader never has to know which section hides a cap. `:complete?`
   is the conjunction — false the moment ANY section is partial."
  [entries]
  (let [entries (->> entries
                     (remove nil?)
                     (sort-by (comp pr-str :truncation/section))
                     vec)
        partial? (boolean (some :truncation/truncated? entries))]
    {:truncation/complete? (not partial?)
     :truncation/sections entries}))

(defn truncation-entry
  [section {:keys [truncated? limit returned total note]}]
  {:truncation/section section
   :truncation/truncated? (boolean truncated?)
   :truncation/limit limit
   :truncation/returned returned
   :truncation/total total
   :truncation/note note})

;; ===========================================================================
;; The CODE FLOOR rendering
;;
;; `render-model` is the dull floor P7 requires: `a code-floor rendering no data
;; revision can break — prove with a drill`. Two properties make that literal:
;;
;;   * the card SET and card ORDER come from `questions`, a compile-time value.
;;     No revision, no absence, no malformation can add, drop or reorder a card.
;;   * every card body is built inside `safe-rows`, so a section that is
;;     garbage, nil, or of an unexpected shape yields a card carrying a named
;;     error instead of a throw or a hole.
;;
;; G9 drills exactly this: feed nonsense as the projection and assert the same
;; card ids in the same order, every one rendered, every failure named.
;; ===========================================================================

(defn- render-value
  "A value rendered for reading, total over anything. Bounded so one enormous
   durable value cannot make a card unreadable — and the bound is declared in
   the row rather than silently applied."
  [v]
  (let [s (cond
            (nil? v) "—"
            (string? v) v
            (keyword? v) (str v)
            (boolean? v) (str v)
            (number? v) (str v)
            :else (pr-str (canonicalize v)))]
    (if (> (count s) 400)
      (str (subs s 0 400) " …[" (count s) " chars]")
      s)))

(defn- row
  [label v]
  {:row/label label :row/value (render-value v)})

(defn- safe-rows
  [f]
  (try
    (vec (remove nil? (f)))
    (catch #?(:clj Throwable :cljs :default) t
      [{:row/label "render error"
        :row/value (str #?(:clj (.getMessage t) :cljs (.-message t)))
        :row/error? true}])))

(defn- card-rows
  "The floor's rows for one question id. Every branch is defensive by
   construction: `get`/`get-in` over an arbitrary value, never destructuring
   that assumes a shape."
  [id result]
  (case id
    :identity
    (let [m (get result :portal/identity)]
      [(row "entity" (get m :entity/id))
       (row "found?" (get m :entity/found?))
       (row "kind" (get m :entity/kind))
       (row "container" (get m :entity/document-container-id))])

    :recipe
    (let [m (get result :portal/recipe)]
      [(row "recipe" (get m :recipe/id))
       (row "name" (get m :recipe/name))
       (row "recurred?" (get m :recipe/recurred?))
       (row "recurrence" (get m :recipe/recurrence-class))
       (row "distinct wearers"
            (get-in m [:recipe/distribution :distribution/distinct-entities]))
       (row "gates?" (get m :recipe/gates?))])

    :attachments
    (let [xs (get result :portal/attachments)]
      (into [(row "attachments" (count xs))]
            (map (fn [a]
                   (row (str (get a :attachment/facet))
                        (str (get a :attachment/master-id)
                             " · authority " (get a :attachment/authority)
                             " · present? " (get a :attachment/present?)))))
            xs))

    :masters
    (let [m (get result :portal/masters)]
      (into [(row "masters" (count m))]
            (map (fn [[mid v]]
                   (row (str mid)
                        (str "active " (get v :master/active-revision-id)
                             " · latest " (get v :master/latest-revision-id)
                             (when (get v :master/candidate?) " · CANDIDATE")
                             (when (true? (get v :master/pinned-here?))
                               " · PINNED")))))
            m))

    :bindings
    (let [m (get result :portal/bindings)]
      [(row "rows at this entity's sites" (count (get m :bindings/rows)))
       (row "sites" (get m :bindings/sites))
       (row "verbs" (count (get m :bindings/verbs)))
       (row "conflicts" (count (get m :bindings/conflicts)))])

    :wearers
    (let [m (get result :portal/wearers)]
      [(row "wearers in basis" (get m :wearers/count))
       (row "appearances" (get m :wearers/appearance-count))
       (row "basis" (get m :wearers/basis))])

    :deviations
    (let [m (get result :portal/deviations)]
      [(row "this entity deviates" (get m :deviations/here))
       (row "pins here" (get m :deviations/pins-here))
       (row "deviations in world" (get m :deviations/count))])

    :activation-history
    (let [m (get result :portal/activation-history)]
      [(row "activations" (get m :activation-history/count))
       (row "clock regressions" (get m :activation-history/regressions))
       (row "complete?" (get m :activation-history/complete?))])

    :experience
    (let [m (get result :portal/experience)]
      [(row "items" (get m :experience/count))
       (row "receipt/silver/gold" (get m :experience/composition))
       (row "origin links" (get m :experience/origin-link-count))])

    :lint
    (let [m (get result :portal/lint)]
      [(row "binding conflicts" (count (get m :lint/binding-conflicts)))
       (row "composition conflicts" (count (get m :lint/composition-conflicts)))
       (row "invalid material" (count (get m :lint/invalid-material)))])

    :truncation
    (let [m (get result :portal/truncation)]
      (into [(row "complete?" (get m :truncation/complete?))]
            (keep (fn [e]
                    (when (get e :truncation/truncated?)
                      (row (str (get e :truncation/section)) "TRUNCATED"))))
            (get m :truncation/sections)))

    :why
    (let [m (get result :portal/why)]
      [(row "asked?" (get m :why/found?))
       (row "won by" (get m :why/won-by))
       (row "master revision" (get m :why/master-revision-id))
       (row "stale pixel?" (get m :why/stale?))
       (row "source path" (get m :why/source-path))])

    :blast
    (let [m (get result :portal/blast)]
      (into (cond-> [(row "scope" (get m :blast/scope))]
              (contains? m :blast/basis)
              (conj (row "basis" (get m :blast/basis))))
            (map (fn [[mid v]]
                   (row (str mid)
                        (str (count (get v :blast/will-move)) " will move · "
                             (count (get v :blast/pinned)) " pinned · "
                             (count (get v :blast/deviating)) " deviating"
                             (when (get v :blast/includes-future-wearers?)
                               " · + future wearers")))))
            (get m :blast/by-master)))

    :history
    (let [m (get result :portal/history)]
      [(row "cut" (get m :history/cut))
       (row "masters at cut" (count (get m :history/masters)))
       (row "standable?" (get m :history/standable?))])

    :recovery
    (let [m (get result :portal/recovery)]
      (into [(row "offers" (count (get m :recovery/offers)))]
            (map (fn [o]
                   (row (str (get o :recovery/master-id))
                        (str "→ " (get o :recovery/to-revision-id)))))
            (get m :recovery/offers)))

    :chrome
    (let [m (get result :portal/chrome)]
      (into [(row "self-editing" (get m :chrome/self-editing?))
             (row "floor?" (get m :chrome/floor?))]
            (map (fn [[facet v]]
                   (row (str facet)
                        (str (get v :chrome/master-id)
                             " @ " (get v :chrome/revision-id)
                             " · tier " (get v :chrome/tier)))))
            (get m :chrome/wears)))

    :briefing
    (let [m (get result :portal/briefing-of)]
      [(row "briefing is" (get m :briefing/is))
       (row "llm in path?" (get m :briefing/llm-in-path?))
       (row "questions carried" (get m :briefing/question-count))
       (row "self-editing" (get m :briefing/self-editing?))])

    [(row "unknown card" id)]))

(defn render-model
  "The portal's floor rendering: one card per question, in question order,
   always. Total over ANY value — including nil, a string, or a map full of
   garbage.

   This is a MODEL, not pixels: the client draws it. That split is deliberate —
   the floor has to be provable in the JVM (G9 runs headless), and a rendering
   whose floor can only be checked in a browser is not a floor."
  [result]
  {:render/version 0
   :render/floor? true
   :render/title
   (str "portal · "
        (let [v (safe-get-in result [:portal/identity :entity/id])]
          (if (string? v) v "(no entity)")))
   :render/card-ids question-ids
   :render/cards
   (mapv (fn [q]
           (let [id (:question/id q)]
             {:card/id id
              :card/title (:question/title q)
              :card/ask (:question/ask q)
              :card/rows (safe-rows #(card-rows id result))
              ;; the answered? probe is itself a read of an arbitrary value, so
              ;; it needs the same boundary the rows have. The G9 drill found
              ;; this hole with a hostile ILookup: a `get-in` that throws would
              ;; have taken the whole floor down from OUTSIDE `safe-rows` —
              ;; which is precisely the failure the floor exists to survive.
              :card/answered?
              (not= ::missing
                    (safe-get-in result (:question/answers-at q)))}))
         questions)})

;; ===========================================================================
;; The resident briefing
;;
;; P7, verbatim: `The resident summoned inside the portal receives EXACTLY this
;; projection as briefing — no LLM in the projection path itself, ever.`
;;
;; Both halves of that are structural. EXACTLY: the `<projection>` payload is
;; `canonical-edn` of the same result the human's portal renders, so G7 asserts
;; byte equality rather than trusting a summary. NO LLM: this is a pure function
;; over a value, in a namespace that requires nothing but clojure.string.
;;
;; Modelled on `face-projection/compose-episode-seed` (the successor-episode
;; seed): deterministic prose from durable material, wrapped for a fresh
;; session's first message.
;; ===========================================================================

(defn briefing
  "The briefing text a resident summoned inside the portal receives."
  [result]
  (let [e (safe-get-in result [:portal/identity :entity/id])
        entity (if (string? e) e "(no entity)")
        edn (canonical-edn result)]
    (str "<portal-briefing>\n"
         "You are working INSIDE the material portal for " entity ".\n"
         "This briefing is a deterministic projection of durable material. No"
         " model wrote it, summarised it, or chose what it contains — it is the"
         " same projection, byte for byte, that the human sees on the other side"
         " of this portal.\n\n"
         "The questions this projection answers, in order:\n"
         (str/join "\n"
                   (map-indexed
                    (fn [i q]
                      (str "  " (inc i) ". " (:question/ask q)
                           "  → " (pr-str (:question/answers-at q))))
                    questions))
         "\n\nRules that hold in here:\n"
         "  · Nothing in this projection is a permission. A description never"
         " refuses an instance.\n"
         "  · latest ≠ candidate ≠ previewed ≠ active ≠ pinned ≠ previous."
         " Read the key, do not infer.\n"
         "  · Truncation is declared. Check :portal/truncation before claiming"
         " coverage.\n"
         "  · Matter acts are named and effect-classed:"
         " :matter/deviate, :matter/activate, :matter/rollback, and"
         " :matter/say are"
         " :durable-via-request through the existing P6 act artery;"
         " :matter/preview is :pure-projection on the client and writes"
         " nothing.\n"
         "\n<projection>"
         edn
         "</projection>\n"
         "</portal-briefing>\n")))

(defn briefing-of
  "The self-description the projection carries about its own briefing — so the
   portal can answer `what does a resident in here know?` without embedding the
   briefing inside the value it is a briefing OF.

   No byte count here on purpose: a value that reports its own serialized size
   has no fixpoint. The size is a TRANSPORT fact and rides the envelope."
  []
  {:briefing/is :the-canonical-projection-verbatim
   :briefing/llm-in-path? false
   :briefing/question-count (count questions)
   :briefing/self-editing? false})

(defn question-rows
  "The question list annotated against one projection: what was asked, where the
   answer is, whether it is there, and the exact calls that replay it.

   This is P7's required output form as a VALUE — the portal carries its own
   question list, so `answered one by one with replayable calls` is something a
   reader can execute rather than something a report claims."
  [result]
  (mapv (fn [q]
          (assoc q :question/answered?
                 (not= ::missing
                       (safe-get-in result (:question/answers-at q)))))
        questions))

(defn master-question-rows
  "The same seventeen questions with replay calls addressed to a facet-master.

   The global `questions` value remains entity-addressed so established entity
   projections retain byte-identical question rows."
  [result master-id]
  (let [jvm-call (str "(portal/open rt {:master-id " (pr-str master-id) "})")
        escaped (str/replace (str master-id) "'" "\\'")
        console-call (str "await __portal.openMaster('" escaped "')")]
    (mapv (fn [q]
            (assoc q
                   :question/answered?
                   (not= ::missing
                         (safe-get-in result (:question/answers-at q)))
                   :question/call-jvm jvm-call
                   :question/call-console console-call))
          questions)))
