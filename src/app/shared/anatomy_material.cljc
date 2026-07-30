(ns app.shared.anatomy-material
  "The revisioned, material anatomy of a block.

   Rows are deliberately flatter and more restrictive than assembly grammar:
   material names registered primitives by keyword, closed presence conditions,
   closed view keys, and either literal, wear, or view props. `assembly-for`
   compiles those rows into the existing grammar-0 assembly data; it is not a
   second renderer or interpreter."
  (:require #?(:clj [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [app.shared.facet-material :as facet-material]))

(def master-id "fm:anatomy")
(def grammar-version 0)
(def code-floor-revision-id "code-floor:fm:anatomy:v0")

(def presence-vocabulary
  "Closed APPLY-time conditions. `:focused-or-hover` is the attention box's
   actual condition; the original fourteen-key contract census could not
   express `(or focused? hover?)` without changing legacy render semantics."
  #{:always
    :machine
    :user
    :header
    :focused
    :hover
    :focused-or-hover
    :has-selection
    :has-refusal
    :has-notice
    :boundary
    :has-group-sel
    :has-gold-marks
    :has-silver-marks
    :has-conflicts})

(def view-key-vocabulary
  "The only instance-derived values material may bind. Per-item keys are the
   closed fields merged into :view for selection, fold-header, and conflict
   expansions."
  #{:text
    :wrap-col
    :headers
    :header-count
    :machine?
    :user?
    :hover?
    :focused?
    :caret-line
    :caret-col
    :notice
    :refusal
    :boundary?
    :gsel?
    :sel-spans
    :fold-headers
    :gold-mark-text
    :gold-mark-count
    :silver-mark-text
    :silver-mark-count
    :line-count
    :max-len
    :block-w
    :block-h
    :font-size
    :char-advance
    :line-h
    :placement-derived?
    ;; closed per-item fields
    :line
    :col-start
    :col-len
    :i
    :section
    :fold-key
    :conflict})

(def part-prim-vocabulary
  "Closed material keyword -> code-address vocabulary. `:sub-anatomy` is a
   compile-time entry that is expanded before the assembly reaches the normal
   registry; the registry still carries a total backstop for it."
  #{:panel
    :panel-header
    :panel-content
    :panel-footer
    :panel-group
    :list-item
    :card
    :badge
    :divider
    :empty-state
    :scrollbar
    :omissions
    :hole-card
    :stack
    :text-run
    :indent-rail
    :box
    :header-band
    :text-clip
    :sliver
    :block-root
    :block-selection-wash
    :block-group-selection
    :block-provenance-rail
    :block-attention-box
    :block-caret
    :block-refusal
    :block-notice
    :block-boundary
    :block-conflict-lint
    :block-gold-mark
    :block-silver-mark
    :block-fold-header-hit
    :thread-edge-rail
    :thread-indent
    :sub-anatomy})

(def ^:private part-keys
  #{:part/id
    :part/prim
    :part/when
    :part/props
    :part/order
    :part/stamp
    :part/def})

(def ^:private stamp-keys
  #{:stamp/facet :stamp/site :stamp/role :stamp/slot})

(defn- data-literal?
  "The assembly V4 guard, applied before rows become assembly data."
  [x]
  (cond
    (or (nil? x)
        (boolean? x)
        (string? x)
        (keyword? x)
        (number? x)) true
    (vector? x) (every? data-literal? x)
    (map? x) (and (every? data-literal? (keys x))
                  (every? data-literal? (vals x)))
    :else false))

(defn- wear-ref?
  [x]
  (and (vector? x)
       (= 3 (count x))
       (= :wear (nth x 0))
       (keyword? (nth x 1))
       (keyword? (nth x 2))))

(defn- view-ref?
  [x]
  (and (vector? x)
       (= 2 (count x))
       (= :view (nth x 0))
       (contains? view-key-vocabulary (nth x 1))))

(defn- prop-value?
  [x]
  (cond
    (and (vector? x) (= :wear (first x))) (wear-ref? x)
    (and (vector? x) (= :view (first x))) (view-ref? x)
    :else (data-literal? x)))

(defn- stamp-row?
  [row]
  (and (map? row)
       (= stamp-keys (set (keys row)))
       (every? keyword? (vals row))))

(defn- stamp-form?
  [x]
  (or (nil? x)
      (stamp-row? x)
      (and (map? x)
           (seq x)
           (every? keyword? (keys x))
           (every? stamp-row? (vals x)))))

(defn- teach
  [type path actual legal]
  {:type type
   :path path
   :actual actual
   :legal legal})

(defn- part-errors
  [path row in-def?]
  (if-not (map? row)
    [(teach :anatomy/part-not-a-map path row
            "a map with :part/id, :part/prim, :part/when, :part/props, and :part/order")]
    (let [unknown (seq (sort-by pr-str (remove part-keys (keys row))))
          prim (:part/prim row)
          sub? (= :sub-anatomy prim)
          props (:part/props row)]
      (cond-> []
        unknown
        (conj (teach :anatomy/part-unknown-keys path unknown part-keys))

        (not (keyword? (:part/id row)))
        (conj (teach :anatomy/part-id-invalid (conj path :part/id)
                     (:part/id row) :keyword))

        (not (contains? part-prim-vocabulary prim))
        (conj (teach :anatomy/part-prim-invalid (conj path :part/prim)
                     prim part-prim-vocabulary))

        (not (contains? presence-vocabulary (:part/when row)))
        (conj (teach :anatomy/part-presence-invalid (conj path :part/when)
                     (:part/when row) presence-vocabulary))

        (not (integer? (:part/order row)))
        (conj (teach :anatomy/part-order-invalid (conj path :part/order)
                     (:part/order row) :integer))

        (not (map? props))
        (conj (teach :anatomy/part-props-invalid (conj path :part/props)
                     props "a keyword-keyed map"))

        (and (map? props) (not-every? keyword? (keys props)))
        (conj (teach :anatomy/part-prop-key-invalid (conj path :part/props)
                     (vec (remove keyword? (keys props))) :keyword))

        (and (map? props) (not-every? prop-value? (vals props)))
        (conj (teach :anatomy/part-prop-value-invalid (conj path :part/props)
                     (into {} (remove (comp prop-value? val)) props)
                     "literal | [:wear facet key] | [:view key]"))

        (not (stamp-form? (:part/stamp row)))
        (conj (teach :anatomy/part-stamp-invalid (conj path :part/stamp)
                     (:part/stamp row) stamp-keys))

        (and sub? in-def?)
        (conj (teach :anatomy/sub-anatomy-depth-invalid path row
                     "defs may not contain :sub-anatomy"))

        (and sub? (not (keyword? (:part/def row))))
        (conj (teach :anatomy/sub-anatomy-def-invalid (conj path :part/def)
                     (:part/def row) :keyword))

        (and (not sub?) (contains? row :part/def))
        (conj (teach :anatomy/part-def-unexpected (conj path :part/def)
                     (:part/def row) "only :sub-anatomy rows carry :part/def"))))))

(defn validation-errors
  "Accumulated, teaching refusals for the material grammar."
  [parts defs]
  (let [parts (if (vector? parts) parts [])
        defs (if (map? defs) defs {})
        row-errors
        (into []
              (mapcat (fn [[i row]]
                        (part-errors [:anatomy/parts i] row false)))
              (map-indexed vector parts))
        def-errors
        (into []
              (mapcat
               (fn [[def-name rows]]
                 (cond
                   (not (keyword? def-name))
                   [(teach :anatomy/def-name-invalid
                           [:anatomy/defs def-name] def-name :keyword)]

                   (not (vector? rows))
                   [(teach :anatomy/def-rows-invalid
                           [:anatomy/defs def-name] rows :vector)]

                   :else
                   (into []
                         (mapcat
                          (fn [[i row]]
                            (part-errors [:anatomy/defs def-name i] row true)))
                         (map-indexed vector rows)))))
              defs)
        ids (keep :part/id parts)
        orders (keep :part/order parts)
        roots (filter #(= :block-root (:part/prim %)) parts)
        ordered (sort-by (juxt :part/order (comp pr-str :part/id)) parts)
        refs (keep #(when (= :sub-anatomy (:part/prim %))
                      (:part/def %))
                   parts)]
    (cond-> (into row-errors def-errors)
      (not (vector? parts))
      (conj (teach :anatomy/parts-invalid [:anatomy/parts] parts :vector))

      (not (map? defs))
      (conj (teach :anatomy/defs-invalid [:anatomy/defs] defs :map))

      (not= (count ids) (count (distinct ids)))
      (conj (teach :anatomy/part-id-duplicate [:anatomy/parts]
                   (vec ids) "unique :part/id values"))

      (not= (count orders) (count (distinct orders)))
      (conj (teach :anatomy/part-order-duplicate [:anatomy/parts]
                   (vec orders) "unique :part/order values"))

      (not= 1 (count roots))
      (conj (teach :anatomy/root-count-invalid [:anatomy/parts]
                   (mapv :part/id roots) "exactly one :block-root"))

      (and (= 1 (count roots))
           (not= (:part/id (first ordered)) (:part/id (first roots))))
      (conj (teach :anatomy/root-order-invalid [:anatomy/parts]
                   (:part/id (first ordered))
                   "the :block-root row must have the lowest :part/order"))

      (and (= 1 (count roots))
           (not= :always (:part/when (first roots))))
      (conj (teach :anatomy/root-presence-invalid [:anatomy/parts]
                   (:part/when (first roots)) :always))

      (seq (remove #(contains? defs %) refs))
      (conj (teach :anatomy/sub-anatomy-missing-def [:anatomy/defs]
                   (vec (remove #(contains? defs %) refs))
                   (set (keys defs)))))))

(def seed-parts
  [{:part/id :root
    :part/prim :block-root
    :part/when :always
    :part/order 0
    :part/props
    {:text [:view :text]
     :wrap-col [:view :wrap-col]
     :headers [:view :headers]
     :machine? [:view :machine?]
     :placement-derived? [:view :placement-derived?]
     :font-size [:view :font-size]
     :char-advance [:view :char-advance]
     :line-h [:view :line-h]
     :tint [:wear :provenance :provenance/tint]
     :pad [:wear :attention :attention/hit-padding]}
    :part/stamp
    {:header-provenance
     {:stamp/facet :provenance
      :stamp/site :fold-header
      :stamp/role :text-color
      :stamp/slot :block/content}
     :noise-header
     {:stamp/facet :foldable
      :stamp/site :noise-header
      :stamp/role :header-copy
      :stamp/slot :block/fold-header-text}
     :prose-header
     {:stamp/facet :foldable
      :stamp/site :prose-header
      :stamp/role :header-copy
      :stamp/slot :block/fold-header-text}
     :body
     {:stamp/facet :text-body
      :stamp/site :wrapped-body
      :stamp/role :wrap-policy
      :stamp/slot :block/content-flow}
     :hit
     {:stamp/facet :attention
      :stamp/site :hit-box
      :stamp/role :hit-target
      :stamp/slot :block/hit-area}
     :positioned
     {:stamp/facet :positioned
      :stamp/site :derived-placement
      :stamp/role :placement-default
      :stamp/slot :block/placement}
     :threaded
     {:stamp/facet :threaded
      :stamp/site :send-adoption
      :stamp/role :column-adoption-policy
      :stamp/slot :block/thread-adoption}}}

   {:part/id :selection
    :part/prim :block-selection-wash
    :part/when :has-selection
    :part/order 10
    :part/props
    {:line [:view :line]
     :col-start [:view :col-start]
     :col-len [:view :col-len]
     :char-advance [:view :char-advance]
     :line-h [:view :line-h]}}

   {:part/id :group-selection
    :part/prim :block-group-selection
    :part/when :has-group-sel
    :part/order 20
    :part/props
    {:w [:view :block-w]
     :h [:view :block-h]
     :pad [:wear :attention :attention/hit-padding]}}

   {:part/id :provenance-rail
    :part/prim :block-provenance-rail
    :part/when :machine
    :part/order 40
    :part/props
    {:h [:view :block-h]
     :pad [:wear :attention :attention/hit-padding]
     :tint [:wear :provenance :provenance/tint]}
    :part/stamp
    {:stamp/facet :provenance
     :stamp/site :machine-rail
     :stamp/role :provenance-marker
     :stamp/slot :block/decorations}}

   {:part/id :attention-box
    :part/prim :block-attention-box
    :part/when :focused-or-hover
    :part/order 30
    :part/props
    {:w [:view :block-w]
     :h [:view :block-h]
     :pad [:wear :attention :attention/hit-padding]
     :border-width [:wear :attention :attention/border-width]
     :border-color [:wear :attention :attention/border-color]
     :bg [:wear :attention :attention/background]}
    :part/stamp
    {:stamp/facet :attention
     :stamp/site :attention-box
     :stamp/role :attention-border
     :stamp/slot :block/decorations}}

   {:part/id :caret
    :part/prim :block-caret
    :part/when :focused
    :part/order 50
    :part/props
    {:line [:view :caret-line]
     :col [:view :caret-col]
     :char-advance [:view :char-advance]
     :line-h [:view :line-h]}}

   {:part/id :refusal
    :part/prim :block-refusal
    :part/when :has-refusal
    :part/order 60
    :part/props
    {:refusal [:view :refusal]
     :line-count [:view :line-count]
     :w [:view :block-w]
     :line-h [:view :line-h]
     :font-size [:view :font-size]}}

   {:part/id :notice
    :part/prim :block-notice
    :part/when :has-notice
    :part/order 70
    :part/props
    {:notice [:view :notice]
     :refusal [:view :refusal]
     :line-count [:view :line-count]
     :w [:view :block-w]
     :line-h [:view :line-h]
     :font-size [:view :font-size]}}

   {:part/id :boundary
    :part/prim :block-boundary
    :part/when :boundary
    :part/order 80
    :part/props
    {:w [:view :block-w]
     :line-h [:view :line-h]
     :font-size [:view :font-size]
     :tint [:wear :provenance :provenance/tint]}
    :part/stamp
    {:stamp/facet :provenance
     :stamp/site :episode-boundary
     :stamp/role :boundary-label
     :stamp/slot :block/prelude}}

   {:part/id :conflict-lint
    :part/prim :block-conflict-lint
    :part/when :has-conflicts
    :part/order 90
    :part/props
    {:i [:view :i]
     :conflict [:view :conflict]
     :w [:view :block-w]
     :h [:view :block-h]
     :line-h [:view :line-h]
     :font-size [:view :font-size]}}

   {:part/id :gold-mark
    :part/prim :block-gold-mark
    :part/when :has-gold-marks
    :part/order 100
    :part/props
    {:text [:view :gold-mark-text]
     :count [:view :gold-mark-count]
     :boundary? [:view :boundary?]
     :w [:view :block-w]
     :char-advance [:view :char-advance]
     :line-h [:view :line-h]
     :font-size [:view :font-size]}}

   {:part/id :silver-mark
    :part/prim :block-silver-mark
    :part/when :has-silver-marks
    :part/order 110
    :part/props
    {:text [:view :silver-mark-text]
     :count [:view :silver-mark-count]
     :boundary? [:view :boundary?]
     :gold? [:view :gold-mark-count]
     :w [:view :block-w]
     :char-advance [:view :char-advance]
     :line-h [:view :line-h]
     :font-size [:view :font-size]}}

   {:part/id :fold-header-hit
    :part/prim :block-fold-header-hit
    :part/when :header
    :part/order 120
    :part/props
    {:i [:view :i]
     :section [:view :section]
     :fold-key [:view :fold-key]
     :w [:view :block-w]
     :line-h [:view :line-h]}
    :part/stamp
    {:stamp/facet :foldable
     :stamp/site :fold-header-hit
     :stamp/role :fold-toggle
     :stamp/slot :block/hit-area}}])

(def default-form
  {:facet-master/id master-id
   :facet-master/grammar grammar-version
   :facet-master/facet :anatomy
   :anatomy/parts seed-parts
   :anatomy/defs {}})

(def default-source (pr-str default-form))

(defn- valid-parts?
  [parts]
  (and (vector? parts)
       ;; The generic envelope validates keys independently. Cross-key
       ;; references are therefore deferred to `compile-form`, where the real
       ;; :anatomy/defs value is available.
       (empty?
        (remove #(= :anatomy/sub-anatomy-missing-def (:type %))
                (validation-errors parts {})))))

(defn- valid-defs?
  [defs]
  (and (map? defs)
       (empty? (validation-errors seed-parts defs))))

(def spec
  {:facet-master/id master-id
   :facet-master/facet :anatomy
   :facet-master/source-ref "softland://facet-master/anatomy"
   :facet-master/default-form default-form
   :facet-master/floor-form default-form
   :facet-master/code-floor-revision-id code-floor-revision-id
   :facet-master/grammars
   {grammar-version
    {:material-keys #{:anatomy/parts :anatomy/defs}
     :validators
     {:anatomy/parts
      {:valid? valid-parts?
       :error-type :anatomy/parts-invalid}
      :anatomy/defs
      {:valid? valid-defs?
       :error-type :anatomy/defs-invalid}}}}})

(defn compile-form
  "Compile with the generic facet envelope and the anatomy grammar's richer,
   accumulated teaching errors."
  [form]
  (let [generic (facet-material/compile-form spec form)
        errors (when (map? form)
                 (validation-errors (:anatomy/parts form)
                                    (:anatomy/defs form)))]
    (if (seq errors)
      {:valid? false
       :errors (vec errors)
       :grammar nil
       :material nil}
      generic)))

(defn compile-source
  [source]
  (try
    (compile-form (edn/read-string (str source)))
    (catch #?(:clj Throwable :cljs :default) t
      {:valid? false
       :errors [{:type :anatomy/parse-error
                 :path []
                 :actual (str source)
                 :legal "EDN anatomy form"
                 :message #?(:clj (.getMessage t)
                             :cljs (.-message t))}]
       :grammar nil
       :material nil})))

(def code-floor
  (facet-material/code-floor spec))

(defn resolved-wear
  [served]
  (facet-material/resolved-wear spec served))

(defn compile-cache-key
  "The slow-changing material identity used by the caller's compiled-assembly
   cache. Instance/view state must never enter this key (T11)."
  [anatomy-wear]
  [(:facet-master/id anatomy-wear)
   (:facet-master/revision-id anatomy-wear)])

(defn contribution-stamp
  [wear subject site role slot]
  (facet-material/contribution-stamp wear subject site role slot))

(defn- qualified-part-id
  [parent-id child-id]
  (keyword (str (name parent-id) "/" (name child-id))))

(defn expand-parts
  "Inline the grammar's one legal sub-anatomy level. Expansion is material
   order preserving; ids are qualified by the referencing part."
  [parts defs]
  (into []
        (mapcat
         (fn [part]
           (if (= :sub-anatomy (:part/prim part))
             (map
              (fn [child]
                (-> child
                    (assoc :part/id
                           (qualified-part-id
                            (:part/id part)
                            (:part/id child)))
                    (update :part/order
                            (fn [child-order]
                              (+ (* 1000 (:part/order part))
                                 child-order)))))
              (sort-by (juxt :part/order (comp pr-str :part/id))
                       (get defs (:part/def part))))
             [part])))
        (sort-by (juxt :part/order (comp pr-str :part/id)) parts)))

(defn- compile-prop
  [value]
  (cond
    (wear-ref? value)
    {:bind value}

    (view-ref? value)
    {:bind [:view (second value)]}

    :else value))

(defn- compiled-props
  [part]
  (cond-> (into {}
                (map (fn [[k value]] [k (compile-prop value)]))
                (:part/props part))
    (stamp-row? (:part/stamp part))
    (assoc :stamp {:bind [:stamp]})

    (and (map? (:part/stamp part))
         (not (stamp-row? (:part/stamp part))))
    (assoc :stamps {:bind [:stamps]})))

(defn assembly-for
  "Compile valid material rows into the existing grammar-0 assembly data.
   T2: this function never dispatches on a component identity."
  [anatomy]
  (let [parts (expand-parts (:anatomy/parts anatomy)
                            (:anatomy/defs anatomy))
        root (first parts)
        children
        (mapv
         (fn [part]
           {:each [:parts (:part/id part)]
            :template
            {:prim (:part/prim part)
             :props (compiled-props part)}})
         (rest parts))]
    {:assembly/name "block"
     :assembly/grammar 0
     :root
     {:prim (:part/prim root)
      :props (compiled-props root)
      :children children}}))

(defn- mint-stamp
  [wears address stamp-row]
  (let [wear (get wears (:stamp/facet stamp-row))]
    (facet-material/contribution-stamp
     wear
     address
     (:stamp/site stamp-row)
     (:stamp/role stamp-row)
     (:stamp/slot stamp-row))))

(defn- minted-stamps
  [wears address stamp-form]
  (cond
    (stamp-row? stamp-form)
    {:stamp (mint-stamp wears address stamp-form)}

    (map? stamp-form)
    {:stamps
     (into {}
           (map (fn [[name row]]
                  [name (mint-stamp wears address row)]))
           stamp-form)}

    :else {}))

(defn- presence?
  [condition view conflicts]
  (case condition
    :always true
    :machine (true? (:machine? view))
    :user (true? (:user? view))
    :header (pos? (:header-count view 0))
    :focused (and (true? (:focused? view))
                  (number? (:caret-line view))
                  (number? (:caret-col view)))
    :hover (true? (:hover? view))
    :focused-or-hover (or (true? (:focused? view))
                          (true? (:hover? view)))
    :has-selection (seq (:sel-spans view))
    :has-refusal (some? (:refusal view))
    :has-notice (some? (:notice view))
    :boundary (true? (:boundary? view))
    :has-group-sel (true? (:gsel? view))
    :has-gold-marks (pos? (:gold-mark-count view 0))
    :has-silver-marks (pos? (:silver-mark-count view 0))
    :has-conflicts (seq conflicts)
    false))

(defn- contribution-descriptors
  [parts wears view address]
  (into []
        (mapcat
         (fn [part]
           (when (presence? (:part/when part) view [])
             (let [form (:part/stamp part)
                   rows (cond
                          (stamp-row? form) [form]
                          (map? form) (vals form)
                          :else [])]
               (keep
                (fn [row]
                  (when (= :block/decorations (:stamp/slot row))
                    (let [wear (get wears (:stamp/facet row))]
                      {:wear wear
                       :stamp (mint-stamp wears address row)
                       :value (:part/id part)})))
                rows)))))
        parts))

(defn- item-source
  [condition view conflicts]
  (case condition
    :has-selection (:sel-spans view)
    :header (:fold-headers view)
    :has-conflicts
    (mapv (fn [i conflict]
            {:id i :i i :conflict conflict})
          (range)
          conflicts)
    [{}]))

(defn apply-data
  "Build the data-context consumed by `face-assembly/apply-assembly`.

   Presence becomes a 0/N item vector for grammar-0 `:each`. Stamps are minted
   from the worn facet revisions at this subject, and same-slot contributions
   are passed through the standing composition law before conflict-lint items
   are exposed."
  [rows wears view address]
  (let [parts (vec rows)
        composition
        (facet-material/compose
         (contribution-descriptors parts wears view address))
        conflicts (:conflicts composition)
        composed-part-ids (mapv :value (:contributions composition))
        composed-part-id-set (set composed-part-ids)
        composed-orders
        (->> parts
             (filter #(contains? composed-part-id-set (:part/id %)))
             (map :part/order)
             sort
             vec)
        ;; Same-slot parts keep occupying the material-order slots, while the
        ;; live composition law decides which contribution occupies each slot.
        ;; Both ordinary priority order and deterministic conflict order remain
        ;; exact without adding a block-specific post-composer.
        sibling-ranks (zipmap composed-part-ids composed-orders)
        root (first parts)
        part-items
        (into {}
              (map
               (fn [part]
                 (let [present? (presence? (:part/when part) view conflicts)
                       source (when present?
                                (item-source
                                 (:part/when part) view conflicts))
                       stamps (minted-stamps
                               wears address (:part/stamp part))]
                   [(:part/id part)
                    (mapv
                     (fn [item]
                       (cond->
                        (merge
                         {:id (or (:id item) (:part/id part))
                          :wear wears
                          :view (merge view (dissoc item :id))}
                         stamps)
                         (contains? sibling-ranks (:part/id part))
                         (assoc :assembly/sibling-rank
                                (get sibling-ranks (:part/id part)))))
                     source)])))
              (rest parts))]
    (merge
     {:wear wears
      :view view
      :parts part-items}
     (minted-stamps wears address (:part/stamp root)))))
