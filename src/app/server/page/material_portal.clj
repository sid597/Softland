(ns app.server.page.material-portal
  "A batched material-world join around one picked entity.
   Takes: a page-serving function, runtime context, entity ids, wearer stamps, and optional history cuts.
   Gives: one portal result with identity, materials, bindings, wearers, history, recovery, and briefing data.
   Holds nothing."
  (:require [app.server.episode.episode :as episode]
            [app.server.worn.material-truth :as material-truth]
            [app.server.worn.facet-master :as facet-master]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.worn.activation-event :as activation-event]
            [app.server.worn.binding-material :as binding-material]
            [app.server.worn.facet-material :as facet-material]
            [app.server.worn.facet-masters :as facet-masters]
            [app.server.page.matter-room :as matter-room]
            [app.server.page.material-inspector :as material-inspector]
            [app.server.page.portal-questions :as portal]
            [app.server.page.verb-registry :as verb-registry]))

;; ===========================================================================
;; Section boundaries — the error-card floor (T-P7-2)
;; ===========================================================================

(defn- without-clock
  "Strip the serve-time clock from an embedded projection (T-P7-1)."
  [m]
  (if (map? m) (dissoc m :face/rendered-at-ms) m))

(defn- error-of
  [id ^Throwable t]
  (merge {:error/section id
          :error/type :portal/section-read-failed
          :error/message (.getMessage t)}
         (when-let [e (:sub-error (ex-data t))]
           {:error/type :portal/sub-projection-failed
            :error/sub-error e})))

(defn- sub
  "One sub-projection, read through the module's OWN serve.

   `serve` is total by contract: it catches everything and returns an error
   data-context rather than throwing. That totality is right for the render loop
   and wrong here — a portal section that silently receives
   `{:conversation/error …}` looks empty rather than broken. Converting it to a
   throw routes it into the section boundary, which names it in the card AND in
   `:portal/errors`."
  [serve-fn request]
  (let [v (serve-fn request)]
    (when-let [e (and (map? v) (:conversation/error v))]
      (throw (ex-info "sub-projection failed" {:sub-error e})))
    (without-clock v)))

(defn- section
  "Compute one section, or return `fallback` carrying a named error.

   The section is always PRESENT (T-P7-2). `errors` is a local atom collecting
   every failure so `:portal/errors` can list them in one place — the portal
   says what broke in the section AND at the top, because a reader scanning for
   trouble should not have to open seventeen cards."
  [errors id fallback f]
  (try
    (f)
    (catch Throwable t
      (let [e (error-of id t)]
        (swap! errors conj e)
        (assoc fallback :section/error e)))))

;; ===========================================================================
;; Identity + placement
;; ===========================================================================

(defn- identity-of
  "What this is. Describe-never-gate: an absent or unknown id projects with
   `:entity/found? false` and named nils — never a refusal, never a throw."
  [oc-rt entity-id]
  (let [result (when (and oc-rt (string? entity-id))
                 (ocr/read-unit oc-rt entity-id))
        unit (:unit result)]
    {:entity/id entity-id
     :entity/found? (some? result)
     :entity/kind (:unit-kind unit)
     :entity/document-container-id (:document-container-id unit)
     :entity/source-id (:source-id unit)
     :entity/target-kind (:target-kind result)
     :entity/target-id (:target-id result)
     :entity/addressable? (boolean (and (string? entity-id)
                                        (seq entity-id)))}))

(def ^:private geometry-page-limit
  "`episode/read-geometry-cells` reads the settled-cell page at the inherited
   100k cap. That is the boot-restore read's own bound, reused rather than
   re-invented; the portal declares it as truncation metadata instead of
   pretending the page is the world."
  100000)

(defn- placement-of
  "The block in ONE world: its settled cell plus the world it is settled in.

   DIRECTION's identity table separates `durable entity` from `placement /
   wear` — position, width override, pins. The cell is the position half; the
   override and pin halves come from the positioned/text-body wears and are
   linked here rather than copied, so there is one truth for each."
  [oc-rt entity-id conversation-id]
  (if-not (and oc-rt (string? entity-id))
    {:placement/found? false :placement/basis :no-runtime}
    (let [object-key (episode/episode-object-key
                      (or conversation-id episode/genesis-conversation-id))
          {:keys [cells camera]} (episode/read-geometry-cells oc-rt object-key)
          cell (get cells entity-id)]
      {:placement/found? (some? cell)
       :placement/world object-key
       :placement/cell cell
       :placement/camera camera
       :placement/settled-cells (count cells)
       :placement/basis :settled-geometry-cells})))

;; ===========================================================================
;; The activation trails — read ONCE, per master, for the whole portal
;; ===========================================================================

(defn- trails-of
  "One `activation-trail` read per priced master, for the whole projection.

   This function exists because the first draft of this portal did not have it:
   `masters-of` wanted the causally previous revision, `recovery-of` wanted the
   rollback target, and `history` wanted the nameable cuts — three sections
   asking the same store the same question, three reads per master. At six
   masters that is eighteen trail reads where six will do, and each trail read is
   a `read-current-revision` plus a `read-revision-history` page.

   Rama's cost model makes this the difference between a portal and a stall
   (seek ≈ 0.3–0.5ms, and the trail page iterates); the /rama skill's rule is
   explicit — never trade I/O efficiency for code simplicity."
  [oc-rt master-ids]
  (if-not oc-rt
    {}
    (into {}
          (keep (fn [master-id]
                  (when-let [spec (facet-masters/spec master-id)]
                    [master-id (facet-master/activation-trail oc-rt spec)])))
          master-ids)))

;; ===========================================================================
;; Masters — latest ≠ candidate ≠ active ≠ pinned ≠ previous, all distinct
;; ===========================================================================

(defn- masters-of
  "Every master stamped on the pick, joined to its served state and to what THIS
   subject actually wears.

   The five states P7 requires kept distinct are five separate keys, none
   derived from another by the reader: `:master/latest-revision-id`,
   `:master/candidate-revision-id`, `:master/active-revision-id`,
   `:master/pinned-revision-id`, `:master/previous-revision-id`. A portal that
   collapsed any two of these would be lying in exactly the way the campaign
   spent P6 preventing."
  [served instances subject master-ids trails]
  (into (sorted-map)
        (map
         (fn [master-id]
           (if-let [spec (facet-masters/spec master-id)]
             (let [facet (:facet-master/facet spec)
                   shared (get-in served [:facet-materials/by-id master-id])
                   inst (get-in instances [facet subject])
                   wear (facet-material/wear-for-subject spec shared inst)
                   active-id (:facet-master/active-revision-id shared)
                   latest-id (:facet-master/latest-revision-id shared)
                   candidate? (and (string? latest-id)
                                   (string? active-id)
                                   (not= latest-id active-id))
                   ;; causally previous, from the ONE trail read (trails-of)
                   previous (get-in (get trails master-id)
                                    [:chain 1 :event :activation/revision-id])]
               [master-id
                {:master/id master-id
                 :master/facet facet
                 :master/source-ref (:facet-master/source-ref spec)
                 :master/grammar (:facet-master/grammar shared)
                 :master/found? (true? (:facet-master/found? shared))
                 :master/valid? (true? (:facet-master/valid? shared))
                 :master/errors (vec (:facet-master/errors shared))
                 ;; the five distinct states
                 :master/active-revision-id active-id
                 :master/latest-revision-id latest-id
                 :master/candidate? (boolean candidate?)
                 :master/candidate-revision-id (when candidate? latest-id)
                 :master/pointer-revision-id
                 (:facet-master/pointer-revision-id shared)
                 :master/previous-revision-id previous
                 :master/pinned-here?
                 (true? (:facet-master/pinned? wear))
                 :master/pinned-revision-id
                 (get-in inst [:facet-master/pin :pinned-revision-id])
                 ;; and what the PICK wears, which is not the same question
                 :master/tier-here (:facet-master/tier wear)
                 :master/worn-here-revision-id (:facet-master/revision-id wear)
                 ;; FLOOR HONESTY. `resolved-wear` reports `:floor? false` for a
                 ;; master the serve could not read at all: the serve substitutes
                 ;; the floor material AND the floor revision id into its
                 ;; `unavailable` shape, and that shape then passes
                 ;; `valid-material?` — so the wear looks like ordinary served
                 ;; material. It is not; it is the floor wearing a served map's
                 ;; clothes. The portal's whole job is to not lie about which
                 ;; revision decided a pixel, so the floor verdict is taken from
                 ;; the SERVE's own found?/valid? as well as from the wear, and
                 ;; the reason is named.
                 :master/floored-here?
                 (boolean (or (:facet-master/floor? wear)
                              (not (true? (:facet-master/valid? shared)))))
                 :master/floor-reason
                 (cond
                   (true? (:facet-master/pin-unresolved? wear)) :pin-unresolved
                   (not (true? (:facet-master/found? shared))) :master-unavailable
                   (not (true? (:facet-master/valid? shared)))
                   :invalid-active-material
                   (:facet-master/floor? wear) :wear-floored
                   :else nil)
                 :master/floor-revision-id (facet-material/floor-master-id spec)
                 :master/instance-id (:facet-master/id inst)
                 :master/holds-here (:facet-master/holds inst)}])
             ;; A stamp can name a master this build has never heard of — an
             ;; older render, a renamed spec, a hand-built request. The first
             ;; draft dropped those rows, which is the one thing the portal may
             ;; never do: a silently absent master reads as `no such fact` when
             ;; the truth is `a fact I cannot explain`. Named, the way the P2
             ;; inspector names it.
             [master-id {:master/id master-id
                         :master/error :unknown-master
                         :master/found? false
                         :master/valid? false
                         :master/floored-here? true
                         :master/floor-reason :unknown-master}]))
         master-ids)))

;; ===========================================================================
;; Bindings — what touching this does, narrowed to the pick's own sites
;; ===========================================================================

(defn- bindings-of
  "The interaction grammar, narrowed to the sites the pick actually renders.

   The whole table is a legitimate query (P5 serves it), but `what does touching
   THIS do` is a different question, and answering it with 20 rows about sites
   this entity has none of would be a dump rather than an answer. Sites come
   from the pick's own contribution stamps — evidence, not assumption."
  ([table sites]
   (bindings-of table sites nil))
  ([table sites anchor-master-id]
   (let [sites (set sites)
         source-rows (vec (:interaction-table/rows table))
         master-rows (if anchor-master-id
                       (filterv #(= anchor-master-id (:table/master-id %))
                                source-rows)
                       source-rows)
         rows (if (seq sites)
                (filterv #(contains? sites (:table/site %)) master-rows)
                master-rows)
         verb-names (into (sorted-set) (keep :table/verb) rows)
         conflicts (if anchor-master-id
                     (binding-material/table-conflicts rows)
                     (filterv #(or (empty? sites)
                                   (contains? sites (:binding/site %)))
                              (:interaction-table/conflicts table)))
         releases
         (->> (:interaction-table/releases table)
              (keep
               (fn [release]
                 (let [binding-node
                       (some #(when (= :binding (:release/kind %)) %)
                             (:release/nodes release))
                       b (:release/binding binding-node)
                       row (:row b)]
                   (when (or (nil? anchor-master-id)
                             (= anchor-master-id (:master-id b)))
                     (let [worn-row
                           (some
                            #(when (and (= :master (:table/tier %))
                                        (= (:master-id b)
                                           (:table/master-id %))
                                        (= (:revision-id b)
                                           (:table/revision-id %))
                                        (= (:binding/gesture row)
                                           (:table/gesture %))
                                        (= (:binding/phase row)
                                           (:table/phase %))
                                        (= (get-in row
                                                   [:binding/verb :verb/name])
                                           (:table/verb %))
                                        (= (get-in row
                                                   [:binding/verb :verb/version])
                                           (:table/verb-version %)))
                               %)
                            master-rows)]
                       (assoc release
                              :release/worn? (boolean worn-row)
                              :release/worn-row worn-row
                              :release/query-path
                              [:portal/bindings :bindings/releases]))))))
              vec)]
     {:bindings/sites (vec (sort-by pr-str sites))
      :bindings/rows rows
      :bindings/row-count (count rows)
      :bindings/table-row-count (count master-rows)
      :bindings/conflicts conflicts
      :bindings/releases releases
      :bindings/verbs
      (mapv (fn [n]
              {:verb/name n
               :verb/effect-class (verb-registry/effect-class n)
               :verb/required-args
               (vec (sort-by pr-str (verb-registry/required-args n)))
               :verb/floor-reserved? (verb-registry/floor-reserved? n)})
            verb-names)
      ;; the pick miss is part of the grammar and belongs in the answer: a
      ;; gesture that claims nothing falls through to the space's floor rows
      :bindings/unclaimed-falls-to binding-material/space-floor-master-id})))

;; ===========================================================================
;; Deviations, activation history, recovery
;; ===========================================================================

(defn- deviations-of
  [truth instances subject]
  (let [diffs (:truth/diffs truth)
        here (into (sorted-map)
                   (keep (fn [[facet by-subject]]
                           (when-let [d (get by-subject subject)]
                             [facet d])))
                   diffs)
        world-count (reduce + 0 (map count (vals diffs)))
        pins-here (into (sorted-map)
                        (keep (fn [[facet m]]
                                (when-let [pin (get-in m [subject :facet-master/pin])]
                                  [facet pin])))
                        instances)]
    {:deviations/here here
     :deviations/here-count (count here)
     :deviations/pins-here pins-here
     :deviations/count world-count
     :deviations/by-facet (or diffs {})
     ;; a diff is computed on read from the two forms, never stored — said out
     ;; loud because a stored diff is the thing that drifts
     :deviations/basis :computed-on-read}))

(defn- activation-history-of
  "The activation history for the masters on the pick: the classified trail, the
   three announcement scales, and the derived case report — each from its P6
   owner, none re-derived here."
  [truth inspector master-ids]
  (let [trails (into {}
                     (map (fn [f]
                            [(get-in f [:material-inspector/facet-master
                                        :facet-master/id])
                             {:trail (:material-inspector/revision-trail f)
                              :complete?
                              (true? (:material-inspector/revision-trail-complete? f))}]))
                     (:material-inspector/facets inspector))
        by-master
        (into (sorted-map)
              (map (fn [master-id]
                     (let [ann (get-in truth [:truth/announcements master-id])
                           cas (get-in truth [:truth/case-reports master-id])
                           tr (get trails master-id)]
                       [master-id
                        {:history/trail (vec (:trail tr))
                         ;; COMPLETE unless something was actually cut off. The
                         ;; first draft read a MISSING trail as an incomplete
                         ;; one, so a master with no history at all reported
                         ;; truncation — and `:portal/truncation` then told the
                         ;; reader material was being withheld when there was
                         ;; none. `nothing to show` and `more than I showed` are
                         ;; different answers.
                         :history/trail-complete? (if tr
                                                    (true? (:complete? tr))
                                                    true)
                         :history/announcements (vec (:announcements ann))
                         :history/announcements-complete? (if ann
                                                            (true? (:complete? ann))
                                                            true)
                         :history/regressions (:regressions ann)
                         :history/case-report cas}])))
              master-ids)]
    {:activation-history/by-master by-master
     :activation-history/count
     (reduce + 0 (map #(count (:history/announcements %)) (vals by-master)))
     :activation-history/complete?
     (every? #(and (:history/trail-complete? %)
                   (:history/announcements-complete? %))
             (vals by-master))
     :activation-history/regressions
     (reduce + 0 (keep #(some-> (:history/regressions %) count)
                       (vals by-master)))
     ;; causal, never clock — P4's finding, still load-bearing
     :activation-history/order :causal}))

(defn- recovery-of
  "Previous-revision recovery, per master (T-P7-5).

   The dull floor's second promise is `previous revision always rewearable`. A
   promise a reader cannot execute is decoration, so each offer names the target
   revision AND the call that rewears it. Rollback is another activation event —
   never a delete, never an `undo` — so the offer's shape is an activation."
  [master-ids trails]
  (let [offers
        (vec
         (keep
          (fn [master-id]
            (when-let [spec (facet-masters/spec master-id)]
              (when-let [chain (:chain (get trails master-id))]
                (let [current (first chain)
                      prev (second chain)
                      to (get-in prev [:event :activation/revision-id])]
                  (when to
                    {:recovery/master-id master-id
                     :recovery/facet (:facet-master/facet spec)
                     :recovery/from-revision-id
                     (get-in current [:event :activation/revision-id])
                     :recovery/to-revision-id to
                     :recovery/via-pointer-revision-id
                     (:pointer-revision-id prev)
                     :recovery/kind :rollback
                     :recovery/call
                     (str "(facet-master/activate! rt (facet-masters/spec \""
                          master-id "\") \"" to
                          "\" {:kind :rollback :actor ACTOR :time-ms (now)})")})))))
          master-ids))]
    {:recovery/offers offers
     :recovery/count (count offers)
     ;; the floor beneath the offers: if every revision were unreadable, this is
     ;; what would still render
     :recovery/code-floor
     (into (sorted-map)
           (keep (fn [master-id]
                   (when-let [spec (facet-masters/spec master-id)]
                     [master-id (facet-material/floor-master-id spec)])))
           master-ids)}))

;; ===========================================================================
;; Lint — where the material is colliding or lying
;; ===========================================================================

(defn- lint-of
  "Three collision classes, kept separate because they have different owners:
   binding priority ties (P5's table lint), same-slot contribution collisions on
   this pick (P3's composition protocol), and material that failed its own
   grammar and is therefore being served from the floor.

   A deterministic temporary winner is allowed; a SILENT winner never is
   (DIRECTION §Composition honesty) — so every class lists its winner."
  [bindings masters wearer-facets]
  (let [by-slot (->> wearer-facets
                     (mapcat (fn [f]
                               (map (fn [slot]
                                      {:slot slot
                                       :master (:wearer/master-id f)
                                       :revision (:wearer/revision-id f)})
                                    (:wearer/contribution-slots f))))
                     (group-by :slot))
        composition-conflicts
        (->> by-slot
             (keep (fn [[slot entries]]
                     (let [masters* (into (sorted-set) (map :master) entries)]
                       (when (> (count masters*) 1)
                         {:type :material-composition/same-slot-collision
                          :material/slot slot
                          :material/masters (vec masters*)
                          ;; deterministic, and named — never silent
                          :material/winner (first masters*)}))))
             (sort-by (comp pr-str :material/slot))
             vec)
        invalid
        (->> masters
             (keep (fn [[master-id m]]
                     (when-not (:master/valid? m)
                       {:type :facet-master/invalid-active-material
                        :master-id master-id
                        :errors (:master/errors m)
                        :serving :code-floor})))
             vec)]
    {:lint/binding-conflicts (vec (:bindings/conflicts bindings))
     :lint/composition-conflicts composition-conflicts
     :lint/invalid-material invalid
     :lint/clean? (and (empty? (:bindings/conflicts bindings))
                       (empty? composition-conflicts)
                       (empty? invalid))}))

;; ===========================================================================
;; Chrome — the strange loop, with self-editing OFF
;; ===========================================================================

(def chrome-facets
  "The facets the portal's OWN surface resolves through.

   DIRECTION: `the portal is itself an instance of Softland space with its own
   material in the layer it opens`. That loop is closed here by resolving the
   portal's chrome through `wear-for-subject` — the same law, the same served
   masters, the same code floor beneath — for the three facets a card surface
   genuinely has: its box (attention), its header (foldable), its text wrap
   (text-body).

   `:positioned` and `:threaded` are excluded because the portal's cards are not
   canvas blocks with birth positions or column adoption; `:provenance` is
   excluded because the portal is not a machine utterance. Excluding by naming
   is the point — a silent omission here would read as an oversight."
  [:attention :foldable :text-body])

(defn- chrome-of
  "The portal's own look, resolved through the layer it is opening.

   The portal subject is derived from the pick, so two portals over two blocks
   are two appearances — never a durable identity (`view-instance is NEVER
   durable identity`, and neither is this).

   `:chrome/self-editing? false` is the P7 fence stated in the value, not only
   in a comment: the portal's material can be READ from inside the portal and
   cannot be written from there. That is P8+, after wear."
  [served entity-id]
  (let [subject (str "portal:" (or entity-id "none"))]
    {:chrome/subject subject
     :chrome/self-editing? false
     :chrome/writable-from-inside? false
     :chrome/facets chrome-facets
     :chrome/excluded-facets
     {:positioned :portal-cards-are-not-canvas-blocks
      :threaded :portal-cards-adopt-no-column
      :provenance :portal-is-not-a-machine-utterance}
     :chrome/wears
     (into (sorted-map)
           (keep
            (fn [facet]
              (when-let [spec (facet-masters/spec-for-facet facet)]
                (let [shared (get-in served [:facet-materials/by-id
                                             (:facet-master/id spec)])
                      ;; no instance master: the portal deviates from nothing,
                      ;; which is what makes it a plain wearer of the land
                      wear (facet-material/wear-for-subject spec shared nil)]
                  [facet
                   {:chrome/master-id (:facet-master/id wear)
                    :chrome/revision-id (:facet-master/revision-id wear)
                    :chrome/tier (:facet-master/tier wear)
                    ;; same floor honesty as `masters-of`: an unreadable master
                    ;; is the floor even when the wear map says otherwise
                    :chrome/floor?
                    (boolean (or (:facet-master/floor? wear)
                                 (not (true? (:facet-master/valid? shared)))))
                    :chrome/master-found? (true? (:facet-master/found? shared))
                    :chrome/material
                    (dissoc wear
                            :facet-master/id :facet-master/facet
                            :facet-master/grammar :facet-master/revision-id
                            :facet-master/floor? :facet-master/tier
                            :facet-master/pinned?)}]))))
           chrome-facets)
     ;; whether the portal is currently standing on material or on code — true
     ;; the moment ANY chrome facet is floored, because a portal half on the
     ;; floor is standing on the floor
     :chrome/floor?
     (boolean
      (some (fn [facet]
              (when-let [spec (facet-masters/spec-for-facet facet)]
                (let [shared (get-in served [:facet-materials/by-id
                                             (:facet-master/id spec)])
                      wear (facet-material/wear-for-subject spec shared nil)]
                  (or (:facet-master/floor? wear)
                      (not (true? (:facet-master/valid? shared)))))))
            chrome-facets))}))

;; ===========================================================================
;; Why this pixel
;; ===========================================================================

(defn- why-of
  "Resolve one rendered contribution stamp into a full causal chain, or — when
   no stamp was asked about — say so and list the stamps that CAN be asked
   about. An empty answer that does not tell you how to ask a real one is a
   dead end, and the portal is supposed to be inhabitable."
  [{:keys [stamp masters deviations bindings placement recipe available]}]
  (if-not (map? stamp)
    {:why/found? false
     :why/asked? false
     :why/available-stamps available
     :why/available-count (count available)}
    (let [master-id (:material/master stamp)
          m (get masters master-id)
          facet (:master/facet m)
          spec (facet-masters/spec master-id)
          diff (get-in deviations [:deviations/here facet])
          pin (get-in deviations [:deviations/pins-here facet])
          site (:material/site stamp)
          rows (filterv #(= site (:table/site %)) (:bindings/rows bindings))]
      (assoc
       (portal/why-this-pixel
        {:stamp stamp
         :spec-source-path (:facet-master/source-ref spec)
         ;; nil when the stamp names a master the portal cannot resolve — so
         ;; `:why/found?` says false. The first draft passed a map of nils here,
         ;; which made `found?` true and produced a confident chain of blanks:
         ;; the exact shape of a causal trace that lies.
         :wear (when (and m (nil? (:master/error m)))
                 {:facet-master/id master-id
                  :facet-master/facet facet
                  :facet-master/revision-id (:master/worn-here-revision-id m)
                  :facet-master/tier (:master/tier-here m)
                  :facet-master/floor? (:master/floored-here? m)
                  :facet-master/pinned? (:master/pinned-here? m)})
         :diff diff
         :pin pin
         :recipe-id (:recipe/id recipe)
         :bindings rows
         :placement placement})
       :why/asked? true
       :why/master-known? (boolean (and m (nil? (:master/error m))))))))

;; ===========================================================================
;; The portal
;; ===========================================================================

(def ^:private announcement-limit 20)

(defn open
  "Open the portal on one pick. ONE call, every answer.

   `serve-fn` is `(fn [request] → data-context)` — the module's own registry
   dispatch, injected (see the ns docstring). `params`:

     :entity-id       the pick (a durable unit id). Absent/unknown still
                      projects — describe, never gate.
     :master-id       a facet-master anchor. It selects anchor mode only when
                      :entity-id is absent.
     :wearers         the current appearance snapshot, one payload, sent once.
     :conversation-id the world, for placement + the instance registry.
     :master-ids      restrict the masters priced (default: those on the pick).
     :narrowed?       make an explicitly empty master set stay empty.
     :scope           the scope to price blast radius for.
     :cut             {master-id → pointer-revision-id} — stand in history.
     :why             a contribution stamp to trace.
     :drill?          the `?drill=` lane's totality probe.

   Returns the canonical, clock-free result. The caller adds transport."
  [{:keys [oc-rt] :as _ctx} serve-fn
   {:keys [entity-id master-id wearers conversation-id master-ids scope cut why
           drill? narrowed?] :as params}]
  (let [errors (atom [])
        sect (fn [id fallback f] (section errors id fallback f))
        anchor? (portal/master-anchor? params)
        anchor-spec (when anchor? (facet-masters/spec master-id))
        room-id (when anchor? (matter-room/room-id master-id))
        experience-conversation-address
        (if anchor?
          (episode/episode-object-key room-id)
          (when conversation-id
            (episode/episode-object-key conversation-id)))
        wearer-snapshot? (some? wearers)
        wearers (material-inspector/normalize-wearers wearers)
        wearers
        (if anchor?
          (filterv
           (fn [w]
             (some #(= master-id (:wearer/master-id %))
                   (:wearer/facets w)))
           wearers)
          wearers)
        here (some #(when (= entity-id (:wearer/entity-id %)) %) wearers)
        wearer-facets (vec (:wearer/facets here))
        stamped-masters (into (sorted-set) (keep :wearer/master-id) wearer-facets)
        ;; T-P7-3: price what the pick wears; widen to the whole registry when it
        ;; stamps nothing, so an untyped entity still gets a world description
        priced (if anchor?
                 [master-id]
                 (if narrowed?
                   (vec (or master-ids []))
                   (vec (or (seq master-ids)
                            (seq stamped-masters)
                            facet-masters/master-ids))))
        sites (into (sorted-set)
                    (mapcat :wearer/contribution-sites)
                    wearer-facets)
        ;; BLAST SCOPE. The first draft passed only the picked entity as
        ;; `:subjects`, which made blast radius price a set of ONE — the portal
        ;; then reported `0 will move` for an activation that was about to reach
        ;; every block on screen. A blast radius that undercounts is worse than
        ;; none, so the subject set is every wearer the portal was given. Diffs
        ;; and deviations still key on the PICK; only the radius widens.
        subjects (vec (sort (into #{} (keep :wearer/entity-id) wearers)))
        subjects (if (seq subjects)
                   subjects
                   (vec (remove nil? [entity-id])))
        scope (or scope (activation-event/all-unpinned-scope))

        ;; ---- the five sub-serves, all inside this one call ----
        served (sect :facet-materials
                     {:facet-materials/by-id {} :facet-materials/instances {}}
                     #(sub serve-fn
                           {:face :facet-materials
                           :params {:subjects subjects
                                     :conversation-id conversation-id
                                     :drill? drill?}}))
        instances (:facet-materials/instances served)
        inspector (sect :material-inspector
                        {:material-inspector/facets []}
                        #(:material-inspector/result
                          (sub serve-fn
                               {:face :material-inspector
                                :params {:entity-id entity-id
                                         :wearers wearers}})))
        table (sect :interaction-table
                    {:interaction-table/rows [] :interaction-table/conflicts []
                     :interaction-table/releases []}
                    #(sub serve-fn
                          {:face :interaction-table
                           :params {:drill? drill?}}))
        truth (sect :material-truth
                    {:truth/diffs {} :truth/blast {} :truth/announcements {}
                     :truth/case-reports {} :truth/history {}}
                    #(sub serve-fn
                          {:face :material-truth
                           :params {:subjects subjects
                                    :conversation-id conversation-id
                                    :master-ids priced
                                    :scope scope
                                    :cut cut
                                    :limit announcement-limit}}))
        experience (sect :material-experience
                         ;; the fallback carries the SHAPE, not just the key: a
                         ;; reader of the composition gauge should not have to
                         ;; branch on whether the read failed
                         {:experience/items []
                          :experience/composition
                          {:receipt 0 :silver 0 :gold 0
                           :machine-records 0 :machine-failures 0}}
                         #(sub serve-fn
                               {:face :material-experience
                                :address (if anchor? master-id entity-id)
                                :params {:material-ids
                                         [(if anchor? master-id entity-id)]
                                         :conversation-address
                                         experience-conversation-address}}))
        cascade-link
        (when anchor?
          {:face :escape-gauge
           :params {:master-id master-id}
           :on-demand? true
           :embedded? false})
        cascade-section
        (when anchor?
          (sect :cascade
                {:cascade/version 0
                 :cascade/rows []
                 :cascade/row-count 0
                 :cascade/read-only? true
                 :cascade/labels [:code-owned :in-process]
                 :cascade/ownership :code-owned
                 :cascade/lifetime :in-process
                 :cascade/escape-gauge cascade-link}
                #(assoc
                  (sub serve-fn {:face :cascade-rows :params {}})
                  :cascade/escape-gauge cascade-link)))

        ;; ---- derived sections ----
        identity* (sect :identity {:entity/id entity-id :entity/found? false}
                        #(if anchor?
                           (portal/master-anchor-identity
                            master-id
                            anchor-spec
                            (when anchor-spec
                              (facet-material/floor-master-id anchor-spec)))
                           (identity-of oc-rt entity-id)))
        placement (sect :placement {:placement/found? false}
                        #(if anchor?
                           {:placement/applicable? false
                            :placement/anchor :facet-master}
                           (placement-of oc-rt entity-id conversation-id)))
        recipe (sect :recipe {:recipe/id "recipe:none"}
                     #(cond->
                       (portal/recipe
                        {:entity-id entity-id
                         :wearers wearers
                         :master->facet
                         (into {} (map (juxt :facet-master/id
                                             :facet-master/facet))
                               facet-masters/specs)})
                        anchor?
                        (assoc :recipe/anchor-master-id master-id)))
        attachments (sect :attachments []
                          #(->> (:material-inspector/facets inspector)
                                (mapv :material-inspector/attachment)))
        ;; ONE trail read per priced master, shared by masters/recovery/history
        trails (sect :activation-trails {} #(trails-of oc-rt priced))
        masters (sect :masters {}
                      #(cond->
                        (masters-of served instances entity-id priced trails)
                         anchor? portal/masters-at-anchor))
        bindings (sect :bindings {:bindings/rows [] :bindings/conflicts []}
                       #(if anchor?
                          (bindings-of table #{} master-id)
                          (bindings-of table sites)))
        deviations (sect :deviations {:deviations/here {}}
                         #(cond->
                           (deviations-of truth instances entity-id)
                            anchor? portal/deviations-at-anchor))
        activation-history (sect :activation-history
                                 {:activation-history/by-master {}}
                                 #(activation-history-of truth inspector priced))
        recovery (sect :recovery {:recovery/offers []}
                       #(recovery-of priced trails))
        lint (sect :lint {:lint/clean? true}
                   #(lint-of bindings masters wearer-facets))
        chrome (sect :chrome {:chrome/self-editing? false}
                     #(chrome-of served entity-id))
        blast (sect :blast {:blast/by-master {}}
                    #(cond->
                      (-> {:blast/scope scope
                           :blast/candidate-wearers
                           (mapv :wearer/entity-id wearers)
                           :blast/by-master
                           (into (sorted-map)
                                 (filter (fn [[k _]]
                                           (contains? (set priced) k)))
                                 (:truth/blast truth))}
                          (assoc :blast/pre-activation? true
                                 :blast/writes-nothing? true))
                       (and anchor? (not wearer-snapshot?))
                       portal/blast-at-anchor))
        history (sect :history {:history/masters {}}
                      #(let [h (:truth/history truth)
                             ;; T-P7-4: a cut you cannot name is not standable
                             available
                             (into (sorted-map)
                                   (keep
                                    (fn [master-id]
                                      (when-let [chain (:chain (get trails master-id))]
                                        [master-id
                                         (mapv :pointer-revision-id chain)])))
                                   priced)]
                         {:history/cut (or cut {})
                          ;; `{}` not nil: a section that answers `nothing`
                          ;; should say it in the shape of the thing it answers
                          :history/masters (or (:history/masters h) {})
                          :history/available-cuts available
                          :history/standable?
                          (every? (comp true? :found?)
                                  (vals (select-keys (:history/masters h)
                                                     (keys (or cut {})))))
                          :history/named-by :pointer-revision-id}))
        why* (sect :why {:why/found? false}
                   #(why-of {:stamp why
                             :masters masters
                             :deviations deviations
                             :bindings bindings
                             :placement placement
                             :recipe recipe
                             ;; offered stamps are COMPLETE stamps: a portal that
                             ;; hands back something it then cannot fully answer
                             ;; about is worse than one that hands back nothing
                             :available
                             (vec (mapcat
                                   (fn [f]
                                     (map (fn [site]
                                            {:material/subject entity-id
                                             :material/attachment
                                             (first (:wearer/attachments f))
                                             :material/master (:wearer/master-id f)
                                             :material/revision (:wearer/revision-id f)
                                             :material/site site
                                             :material/role
                                             (first (:wearer/contribution-roles f))
                                             :material/slot
                                             (first (:wearer/contribution-slots f))})
                                          (:wearer/contribution-sites f)))
                                   wearer-facets))}))
        wearers-section
        (cond->
         {:wearers/count (count wearers)
          :wearers/appearance-count
          (reduce + 0 (map #(count (:wearer/view-instances %)) wearers))
          :wearers/here here
          :wearers/entities (mapv :wearer/entity-id wearers)
          :wearers/by-entity wearers
          ;; the honest basis: current-scene evidence, never a durable
          ;; attachment table (P2's ruling, unchanged)
          :wearers/basis :current-client-scene}
          (and anchor? (not wearer-snapshot?)) portal/wearers-at-anchor)
        truncation
        (portal/truncation
         [(portal/truncation-entry
           :revision-trail
           {:truncated? (not (every? #(true? (:material-inspector/revision-trail-complete? %))
                                     (:material-inspector/facets inspector)))
            :note "per-master candidate + pointer history page"})
          (portal/truncation-entry
           :instances
           {:truncated? (true? (:facet-materials/instances-truncated? served))
            :total (:facet-materials/instances-total served)
            :note "served deviation index"})
          (portal/truncation-entry
           :announcements
           {:truncated? (not (:activation-history/complete? activation-history))
            :limit announcement-limit
            :note "activation announcements per master"})
          (portal/truncation-entry
           :placement
           {:truncated? (>= (or (:placement/settled-cells placement) 0)
                            geometry-page-limit)
            :limit geometry-page-limit
            :returned (:placement/settled-cells placement)
            :note "settled geometry cell page"})
          (portal/truncation-entry
           :experience
           {:truncated? (boolean (seq (:experience/source-errors experience)))
            :returned (count (:experience/items experience))
            :note "relation + record reads for this material"})
          (portal/truncation-entry
           :wearers
           {:truncated? false
            :returned (count wearers)
            :note "appearance snapshot is the open page, not the world"})
          (when anchor?
            (portal/truncation-entry
             :cascade
             {:truncated? false
              :returned (:cascade/row-count cascade-section)
              :total (:cascade/row-count cascade-section)
              :note "complete code-owned in-process declaration table"}))
          (when anchor?
            (portal/truncation-entry
             :escape-gauge
             {:truncated? false
              :returned 1
              :total 1
              :note "complete on-demand link; zero git-backed reports embedded"}))])
        experience-section
        (cond->
         (merge (select-keys experience
                             [:experience/items
                              :experience/composition
                              :experience/gold-marks-by-target
                              :experience/silver-marks-by-target
                              :experience/query-plan
                              :experience/source-errors])
                {:experience/count (count (:experience/items experience))
                 :experience/origin-link-count
                 (reduce + 0 (map #(count (:experience/reverse-links %))
                                  (:experience/items experience)))})
          anchor?
          (assoc :experience/material-ids [master-id]
                 :experience/conversation-address
                 experience-conversation-address))
        base
        (cond->
         {:portal/version portal/portal-version
          :portal/entity-id (if anchor? master-id entity-id)
          :portal/identity identity*
          :portal/placement placement
          :portal/recipe recipe
          :portal/attachments attachments
          :portal/masters masters
          :portal/bindings bindings
          :portal/wearers wearers-section
          :portal/deviations deviations
          :portal/activation-history activation-history
          :portal/experience experience-section
          :portal/lint lint
          :portal/truncation truncation
          :portal/why why*
          :portal/blast blast
          :portal/history history
          :portal/recovery recovery
          :portal/chrome chrome
          :portal/briefing-of (portal/briefing-of)
          :portal/errors @errors
          :portal/query-plan
          {:plan/client-roundtrips 1
           :plan/sub-projections
           (cond-> [:facet-materials :material-inspector :interaction-table
                    :material-truth :material-experience]
             anchor? (conj :cascade-rows))
           :plan/masters-priced (count priced)
           :plan/masters-in-registry (count facet-masters/master-ids)
           :plan/joins-server-side? true
           :plan/client-joins 0
           :plan/llm-calls 0
           ;; declared rather than discovered: `:material-truth` re-reads the
           ;; served facet materials internally (P6's own shape). Narrowing to
           ;; the priced masters is what keeps that from multiplying.
           :plan/known-duplicate-reads [:facet-materials-inside-material-truth]
           :plan/object-container-available? (boolean oc-rt)}}
          anchor?
          (assoc :portal/master-id master-id
                 :portal/room (sorted-map room-id master-id)
                 :portal/cascade cascade-section))]
    ;; the question list is annotated LAST, against the assembled projection, so
    ;; `:question/answered?` is a measurement of this very value and not a claim
    ;; copied forward from the code. No question answers at `:portal/questions`,
    ;; so this assoc cannot make itself true.
    (portal/canonicalize
     (assoc base :portal/questions
            (if anchor?
              (portal/master-question-rows base master-id)
              (portal/question-rows base))))))

(defn render
  "The portal's CODE-FLOOR rendering of one result.

   Kept OUT of `:portal/result` deliberately. The result is the projection; this
   is a view of it. Embedding the view would put a rendering of the projection
   inside the projection the briefing carries — doubling the bytes a resident
   reads, for a value it can compute itself."
  [result]
  (portal/render-model result))

(defn briefing
  "The briefing a resident summoned inside this portal receives: the canonical
   projection, verbatim, wrapped in deterministic prose. Pure — no model is
   consulted anywhere on this path (P7's absolute fence)."
  [result]
  (portal/briefing result))
