(ns app.client.workspace.face-primitives
  "Framework Wave 1 — lane W1-B: the primitive VOCABULARY (CONTRACT §6).

   A `face` is pure-EDN arrangement (an assembly) walked by lane A's interpreter
   over a REGISTRY of primitive builders. This ns is that registry, assembled as
   a plain value (`registry`, at the bottom) — no global mutable state; tests
   pass their own map (§6).

   Three kinds of entry live here:

   1. HARVESTED BUILDERS — extraction, not invention (§6 / the
      copy-then-harmonize ruling, CONTRACT §2). The `ui-*` builders and the
      `list-*`/`typo-*`/`dt` support defs came from the old workspace's
      ui_primitives.cljs; `omission-line`/`omissions-block`/
      `hole-endpoint-card` came from the retired trail-face drawing island.
      Those origins are gone, so THIS file is the canonical home of all of
      them. Gate G7 (`face_primitives_test.clj`) pins their continued presence.

   2. §6 BUILDER-FN wrappers — the fixed interface `(fn [ctx props children]
      -> rt-node)`. The harvested builders keep their original bounds-passing
      signatures; the `*-prim` wrappers adapt the §6 interface to them, MEASURING
      each node's bounds bottom-up from its already-built children before
      returning (the measure rule, §6 / trap T7 — measure in the primitive,
      arrange in the engine). These `*-prim` fns are the registry values.

   3. The two genuinely-NEW primitives (`:text-run`, `:indent-rail`) plus the
      bare `:stack` layout node, written directly in the §6 interface.

   PURITY (G9): every fn here is pure `.cljc` returning `rect_tree` rt-nodes;
   no `js/`, no reader conditionals — the whole vocabulary loads and runs JVM-
   side. Persisted form is keyword + code-address, never fn values (trap T5, §6);
   the runtime map below binds keyword->fn at load."
  (:require [clojure.string :as str]
            [app.client.workspace.rect-tree :as rt :refer [rt-node wrap-line]]
            [app.client.workspace.text-layout :as tl]
            [components.design-tokens :as design-tokens]))

;; ===========================================================================
;; Support defs (extracted from the old ui_primitives.cljs; canonical here)
;; ===========================================================================

(def list-row-h 36)
(def list-group-header-h 28)
(def list-left-pane-pct 0.40)
(def list-padding-x 16)
(def list-item-inset 6)       ;; horizontal inset for rounded hover bg
(def list-padding-top 44)
(def list-checkbox-size 16)
(def list-divider-w 1)
(def list-group-gap 12)       ;; vertical space between groups
(def list-footer-h 40)        ;; footer height

(def dt
  "Design tokens — imported from shared components.design-tokens."
  design-tokens/dt)

(def typo-title    {:size (:xl (:font-sizes dt)) :a 1.0})
(def typo-subtitle {:size (:lg (:font-sizes dt)) :a 0.9})
(def typo-body     {:size (:md (:font-sizes dt)) :a 0.85})
(def typo-caption  {:size (:sm (:font-sizes dt)) :a 0.6})

;; ===========================================================================
;; The named ui-* builders (extracted from the old ui_primitives.cljs; this
;; file is their canonical home — the G7 diff against that origin retired).
;; ===========================================================================

(defn ui-card
  "Card component: rounded rect with border, optional shadow.
   Returns an rt-node with :shadow and :radius in style."
  [id bounds & {:keys [children text shadow variant]
                :or {shadow :md variant :default}}]
  (let [bg     (case variant
                 :elevated (:bg-elevated (:colors dt))
                 :muted    (:bg-muted (:colors dt))
                 (:bg-subtle (:colors dt)))
        border (:border (:colors dt))
        radius (:lg (:radii dt))
        shadow-spec (get (:shadows dt) shadow)]
    (rt-node id :card bounds
             :style (cond-> {:bg bg :radius radius
                             :border-width 1 :border-color border}
                      shadow-spec (assoc :shadow shadow-spec))
             :children (vec (or children []))
             :text (or text []))))

(defn ui-badge
  "Small pill badge with tinted background. Returns an rt-node."
  [id bounds label & {:keys [color text-color font-size]
                      :or {color (:accent-muted (:colors dt))
                           text-color (:accent (:colors dt))
                           font-size (:xs (:font-sizes dt))}}]
  (rt-node id :badge bounds
           :style {:bg color :radius (:full (:radii dt))}
           :text [{:text label :type :keyword
                   :from 0 :to (count label)
                   :x 6 :y (- (:h bounds) 4)
                   :size font-size
                   :r (nth text-color 0) :g (nth text-color 1)
                   :b (nth text-color 2) :a (nth text-color 3)}]))

(defn ui-divider
  "Thin horizontal separator line. Returns an rt-node."
  [id bounds & {:keys [color] :or {color (:border-subtle (:colors dt))}}]
  (rt-node id :divider bounds :style {:bg color}))

(defn ui-scrollbar
  "Vertical scrollbar (track + thumb). Returns an rt-node."
  [id bounds thumb-pct thumb-offset & {:keys [track-color thumb-color]
                                       :or {track-color [0 0 0 0]
                                            thumb-color [0.35 0.35 0.40 0.5]}}]
  (let [track-h (:h bounds)
        thumb-h (max 20 (* track-h thumb-pct))
        thumb-y (* (- track-h thumb-h) (min 1.0 (max 0.0 thumb-offset)))]
    (rt-node id :scrollbar bounds
             :style {:bg track-color}
             :children [(rt-node (keyword (str (name id) "-thumb")) :scrollbar-thumb
                          {:x 1 :y thumb-y :w (- (:w bounds) 2) :h thumb-h}
                          :style {:bg thumb-color :radius (:full (:radii dt))})])))

(defn build-empty-state
  "Centered empty state with icon, headline, description.
   Returns an rt-node positioned at center of w x h bounds.
   icon: 2-char text symbol (e.g. \"--\", \"[]\", \"<>\")
   headline: main message text
   description: secondary guidance text"
  [id w h {:keys [icon headline description]}]
  (let [surfaces (:surfaces dt)
        colors (:colors dt)
        text-sec (or (:text-secondary surfaces) (:fg-muted colors))
        text-mut (or (:text-muted surfaces) (:fg-subtle colors))
        cx (/ w 2)
        ;; Center vertically, offset upward slightly for visual balance
        cy (- (/ h 2) 40)
        icon-size (:size typo-title)
        head-size (:size typo-subtitle)
        desc-size (:size typo-body)
        icon-w (* (count (or icon "")) icon-size 0.56)
        head-w (* (count (or headline "")) head-size 0.56)
        desc-lines (when description
                     (let [max-chars (max 20 (int (/ (* w 0.6) (* desc-size 0.56))))]
                       (wrap-line description max-chars)))]
    (rt-node id :empty-state
      {:x 0 :y 0 :w w :h h}
      :text
      (cond-> []
        ;; Icon
        icon
        (conj {:text icon :type :comment
               :from 0 :to (count icon)
               :x (- cx (/ icon-w 2)) :y cy
               :size icon-size
               :r (nth text-mut 0) :g (nth text-mut 1)
               :b (nth text-mut 2) :a (nth text-mut 3 0.5)})
        ;; Headline
        headline
        (conj {:text headline :type :text
               :from 0 :to (count headline)
               :x (- cx (/ head-w 2)) :y (+ cy 32)
               :size head-size
               :r (nth text-sec 0) :g (nth text-sec 1)
               :b (nth text-sec 2) :a (nth text-sec 3 0.7)})
        ;; Description lines (centered)
        desc-lines
        (into (map-indexed
                (fn [i line]
                  (let [line-w (* (count line) desc-size 0.56)]
                    {:text line :type :comment
                     :from 0 :to (count line)
                     :x (- cx (/ line-w 2)) :y (+ cy 56 (* i 22))
                     :size desc-size
                     :r (nth text-mut 0) :g (nth text-mut 1)
                     :b (nth text-mut 2) :a 0.7}))
                desc-lines))))))

(defn ui-panel
  "Container panel — the outermost sidebar/panel frame.
   Vertical column layout: stacks header/content/footer automatically.
   variant: :default (bg), :elevated (bg-elevated), :subtle (bg-subtle)"
  [id bounds & {:keys [children variant style]
                :or {variant :default}}]
  (let [bg (case variant
             :elevated (:bg-elevated (:colors dt))
             :subtle   (:bg-subtle (:colors dt))
             (:bg (:colors dt)))]
    (rt-node id :panel bounds
             :style (merge {:bg bg} style)
             :layout {:direction :column}
             :children (vec (or children [])))))

(defn ui-panel-header
  "Fixed-height header slot with integrated bottom border.
   text: vector of text-op maps (with :x/:y in local coords)."
  [id w h & {:keys [children text style]}]
  (rt-node id :panel-header
    {:x 0 :y 0 :w w :h h}
    :style (merge {:bg (:bg-elevated (:colors dt))
                   :border-widths [0 0 1 0]
                   :border-color (:border-subtle (:colors dt))}
                  style)
    :children (vec (or children []))
    :text (or text [])))

(defn ui-panel-content
  "Scrollable content area — fills remaining height, clips children.
   layout-opts override defaults: {:direction :column :gap list-group-gap}."
  [id w h & {:keys [children layout-opts]}]
  (rt-node id :panel-content
    {:x 0 :y 0 :w w :h h}
    :clip? true
    :layout (merge {:direction :column :gap list-group-gap :padding [4 0 0 0]} layout-opts)
    :children (vec (or children []))))

(defn ui-panel-footer
  "Fixed-height footer slot with top border separator."
  [id w h & {:keys [children text style]}]
  (rt-node id :panel-footer
    {:x 0 :y 0 :w w :h h}
    :style (merge {:bg (:bg-elevated (:colors dt))
                   :border-widths [1 0 0 0]
                   :border-color (:border-subtle (:colors dt))} style)
    :children (vec (or children []))
    :text (or text [])))

(defn ui-panel-group
  "Collapsible labeled section — shadcn-style uppercase muted label + items.
   items: vector of rt-nodes (typically ui-list-item results).
   status: raw status string (for hit-test/collapse). label: display string.
   Height is auto-computed: header-h + (collapsed? 0 : items)."
  [id w & {:keys [label status icon icon-color collapsed? items font-size first-group?]
           :or {font-size (:xs (:font-sizes dt))
                collapsed? false
                first-group? false}}]
  (let [header-h list-group-header-h
        sc (:fg-section (:colors dt))
        ind (if collapsed? ">" "v")
        upper-label (str/upper-case (or label ""))
        label-str (str ind " " upper-label)
        ;; Subtle top separator between groups (skip first)
        sep-node (when-not first-group?
                   (rt-node (keyword (str (name id) "-sep")) :divider
                     {:x list-padding-x :y 0 :w (- w (* 2 list-padding-x)) :h 1}
                     :style {:bg (:border-subtle (:colors dt))}))
        group-header (rt-node (keyword (str (name id) "-hdr")) :group-header
                       {:x 0 :y 0 :w w :h header-h}
                       :data {:status (or status label)}
                       :text [{:text label-str :type :comment
                               :from 0 :to (count label-str)
                               :x list-padding-x :y 19
                               :size font-size
                               :r (nth sc 0) :g (nth sc 1) :b (nth sc 2) :a (nth sc 3)}])
        visible-items (if collapsed? [] (vec items))
        all-children (cond-> []
                       sep-node  (conj sep-node)
                       true      (conj group-header)
                       true      (into visible-items))
        sep-h (if sep-node 1 0)
        total-h (+ sep-h header-h (if collapsed? 0 (* (count (or items [])) list-row-h)))]
    (rt-node id :panel-group
      {:x 0 :y 0 :w w :h total-h}
      :layout {:direction :column}
      :children all-children)))

(defn ui-list-item
  "Row with leading/title/trailing slots — shadcn SidebarMenuItem style.
   Rounded inset hover/selected background with left accent bar on selected.
   leading/trailing are child rt-nodes.
   title: string — rendered as text between leading and trailing."
  [id w & {:keys [title leading trailing selected? hovered? ghost? data
                  font-size title-x-offset]
           :or {font-size (:sm (:font-sizes dt))
                title-x-offset (+ list-padding-x list-checkbox-size 16)}}]
  (let [;; Outer row is transparent — inner child gets the rounded bg
        ga (if ghost? 0.3 1.0)
        ;; Inner rounded highlight rect (inset from edges)
        inner-bg (cond
                   (and selected? ghost?) (assoc (:bg-selected (:colors dt)) 3 0.15)
                   selected?              (:bg-selected (:colors dt))
                   hovered?               (:bg-hover (:colors dt))
                   :else                  nil)
        inset list-item-inset
        inner-h (- list-row-h 4)  ;; 2px top + 2px bottom breathing room
        inner-w (- w (* 2 inset))
        highlight-node (when inner-bg
                         (rt-node (keyword (str (name id) "-hl")) :highlight
                           {:x inset :y 2 :w inner-w :h inner-h}
                           :style {:bg inner-bg
                                   :radius (:lg (:radii dt))}))
        ;; Left accent bar on selected items (shadcn active indicator)
        accent-bar (when (and selected? (not ghost?))
                     (rt-node (keyword (str (name id) "-acc")) :accent-bar
                       {:x (+ inset 1) :y 6 :w 3 :h (- list-row-h 12)}
                       :style {:bg (:accent (:colors dt))
                               :radius (:sm (:radii dt))}))
        ;; Text truncation
        trunc-max (if (pos? font-size)
                    (max 8 (int (/ (- w title-x-offset 36)
                                   (* font-size 0.56))))
                    30)
        trunc (when title
                (if (> (count title) trunc-max)
                  (str (subs title 0 (- trunc-max 2)) "..")
                  title))
        ;; Text vertically centered in row
        text-y (+ (/ list-row-h 2) (/ font-size 2.5))
        children (cond-> []
                   highlight-node (conj highlight-node)
                   accent-bar     (conj accent-bar)
                   (and leading (not ghost?)) (conj leading)
                   (and trailing (not ghost?)) (conj trailing))
        ;; Selected text gets slightly brighter
        fg (if selected?
             [0.95 0.95 0.98 1.0]
             (:fg (:colors dt)))
        text-ops (cond-> []
                   trunc
                   (conj {:text trunc :type :text
                          :from 0 :to (count trunc)
                          :x title-x-offset :y text-y
                          :size font-size
                          :r (nth fg 0) :g (nth fg 1) :b (nth fg 2)
                          :a (* (nth fg 3) ga)}))]
    (rt-node id :ticket-row
      {:x 0 :y 0 :w w :h list-row-h}
      :data (merge {:selected? selected? :hovered? hovered? :ghost? ghost?}
                   data)
      :children children
      :text text-ops)))

;; ===========================================================================
;; HARVESTED BUILDERS — canonical here since the trail-face drawing island
;; retired.
;; The Outline face's D-005 affordances (holes/omissions "every lack the face
;; exposes"). feed-entry-card is NOT harvested: it pulls a heavy private-helper
;; chain (band-line-ops / two-clock stamps / kind-glyph …) and its band-specific
;; measure is trail-view, not outline-generic — a named extension point, not a
;; void.
;; ===========================================================================

(defn omission-line
  "One omission entry -> one VISIBLE line with its count; resumable caps
   render the cursor affordance (E-15). Total over the three live shapes
   (gate-16 verified): bundle caps {:layer :dropped :cap :cursor},
   unrecognized targets {:id :reason <kw>}, and feed standing gaps
   {:omission/kind :feed/uncovered :feed/gap <kw> :reason <string>}."
  [{:keys [layer dropped cap cursor reason note] :as omission}]
  (let [uncovered? (or (= :feed/uncovered (:omission/kind omission))
                       (= :feed/uncovered reason))]
    {:style :omission
     :omission omission
     :text (cond
             uncovered?
             (str "uncovered"
                  (when-let [g (:feed/gap omission)] (str " (" (name g) ")"))
                  ": " (or note (when (string? reason) reason) "declared gap"))

             (keyword? reason)
             ;; full keyword incl. namespace - :target/unrecognized carries
             ;; meaning in the namespace half
             (str "omitted (" (subs (str reason) 1) "): "
                  (or (:id omission) "") (when dropped (str " x" dropped)))

             :else
             (str "omitted " (or dropped "?") " of " (some-> layer name)
                  (when cap (str " (cap " cap ")"))
                  (when cursor " … more")))}))

(defn omissions-block
  "Omission entries -> rt-node block, one visible line each. An empty
   input renders NO block (nil), never an empty frame."
  [omissions {:keys [line-height card-w] :as _geom}]
  (when (seq omissions)
    (let [lines (mapv omission-line omissions)]
      (rt/rt-node [:trail-face/omissions (count lines)] :omissions
                  {:x 0 :y 0 :w (or card-w 300)
                   :h (* (count lines) (or line-height 18))}
                  :style {:bg [0.25 0.20 0.20 0.6]}
                  :data {:trail-face/omission-count (count lines)}
                  :text (vec (map-indexed
                              (fn [i l]
                                {:text (:text l) :x 4 :y (* i (or line-height 18))
                                 :size 12 :style :omission})
                              lines))))))

(defn hole-endpoint-card
  "A hole-shaped row renders with the EXPLICIT hole style (U+25CC dotted
   circle) - no exception, no silent skip (WP1 s9.3 door kept open)."
  [row {:keys [card-w line-height] :as _geom}]
  (rt/rt-node [:trail-face/hole (or (:relation-id row) (hash row))] :hole
              {:x 0 :y 0 :w (or card-w 300) :h (or line-height 18)}
              :style {:bg [0.30 0.25 0.35 0.6] :border-width 1
                      :border-color [0.6 0.5 0.8 0.8]}
              :data {:trail-face/hole? true}
              :text [{:text (str "◌ hole "
                                 (when-let [k (:kind row)] (str "(" (name k) ") "))
                                 "- endpoint unresolved")
                      :x 4 :y 0 :size 12 :style :hole}]))

;; ===========================================================================
;; Shared measure helpers (§6 measure rule — measure in the primitive)
;; ===========================================================================

(def ^:private fallback-char-width
  "Ubuntu Sans Mono glyph advance — THE codebase 0.56 (CLAUDE.md Text/Cursor
   Alignment: this file is on the sync list; if this diverges from the other
   files' 0.56 the face's wrap width drifts from the GPU advance). ONE named
   constant so no second literal creeps in (§6 — G16 falsification fix);
   fallback only — the §5 geom always carries :char-advance in production."
  0.56)

(defn- child-id
  "Deterministic child id: extend a vector-path id by one segment; wrap a
   scalar id into a vector first (tests may pass a keyword id)."
  [id seg]
  (conj (if (vector? id) id [id]) seg))

(defn- named-id
  "The interpreter's node ids are structural VECTOR paths (§5), but three of the
   harvested builders (ui-panel-group / ui-list-item / ui-scrollbar) derive
   their child ids via `(name id)` — they assume a name-able keyword/string id
   (their sidebar origin). The wrappers hand those builders a deterministic
   name-able id folded from the
   vector path (`[:root 1] -> :root.1`); it still travels with the item on
   reorder (trap T6), so keyed-pool stability holds."
  [id]
  (cond
    (keyword? id) id
    (string? id)  id
    (vector? id)  (keyword (str/join "." (map (fn [seg] (if (keyword? seg) (name seg) (str seg))) id)))
    :else         (keyword (str id))))

(defn- content-w
  "Width flows down (§6): default to the geom content-w unless :props :w says
   otherwise. Never introduces a second char-advance constant."
  [props geom]
  (or (:w props) (:content-w geom) (:viewport-w geom) 300))

(defn- sum-children-h [children]
  (reduce + 0 (map #(get-in % [:bounds :h] 0) children)))

(defn- sum-children-w [children]
  (reduce + 0 (map #(get-in % [:bounds :w] 0) children)))

(defn- gap-total [gap n]
  (* gap (max 0 (dec n))))

;; ===========================================================================
;; GENUINELY NEW primitives (§6) — written directly in the builder-fn interface
;; ===========================================================================

(defn stack-prim
  "`:stack` — the bare layout node (§6): rt-node + :layout passthrough
   (direction/gap/padding/align; both :row and :column exist, rect_tree.cljc:81).
   Measures its own bounds bottom-up from already-built children (measure rule,
   trap T7) so the engine's resolve-layout only arranges. :direction :column
   (default) or :row (accepts :dir :h as an alias for :row)."
  [ctx props children]
  (let [geom (:geom ctx)
        dir  (case (:direction props (:dir props)) (:row :h) :row :column)
        gap  (or (:gap props) 0)
        pad  (or (:padding props) 0)
        [pt pr pb pl] (rt/normalize-padding pad)
        gt   (gap-total gap (count children))
        w    (if (= dir :row)
               (+ pl pr gt (sum-children-w children))
               (content-w props geom))
        h    (if (= dir :row)
               (+ pt pb (reduce max 0 (map #(get-in % [:bounds :h] 0) children)))
               (+ pt pb gt (sum-children-h children)))]
    (rt-node (:id ctx) :stack
             {:x 0 :y 0 :w w :h h}
             :layout {:direction dir :gap gap :padding pad
                      :align (or (:align props) :start)}
             :children (vec children))))

(defn text-run-prim
  "`:text-run` — prose block (§6 measure rule; PROBE Finding 1; trap T7).
   Contract T0 computes wrap, measure, and positioned paint ops from one legacy
   layout result. Width defaults to (:content-w geom); the 0.56 ratio remains
   only a provider input when a bare test geom omits :char-advance."
  [ctx props children]
  (let [geom (:geom ctx)
        w    (content-w props geom)
        fs   (or (:size props) (:font-size geom) 14)
        lh   (or (:line-height props) (:line-height geom) 20)
        ca   (or (:char-advance geom) (* fs fallback-char-width))
        pad  (or (:padding props) 0)
        [pt pr pb pl] (rt/normalize-padding pad)
        avail (max 1 (- w pl pr))
        value (str (:value props (:text props "")))
        layout-result (tl/layout {:text value
                                  :provider (or (:layout-provider geom)
                                                (:text-provider geom))
                                  :font-size fs
                                  :char-advance ca
                                  :line-height lh
                                  :origin [pl pt]
                                  :baseline-offset 0
                                  :inline-size avail
                                  :wrap-policy :word
                                  :source-id (:id ctx)})
        color (or (:color props) (get-in dt [:colors :fg]))
        ops   (tl/line-paint-ops
               layout-result
               {:type (or (:text-type props) :text)
                :size fs
                :r (nth color 0) :g (nth color 1)
                :b (nth color 2) :a (nth color 3)})
        h     (+ pt pb (second (get-in (tl/measure-result layout-result)
                                       [:metrics :advance])))]
    (rt-node (:id ctx) :text-run
             {:x 0 :y 0 :w w :h h}
             :text ops
             :children (vec children))))

(defn indent-rail-prim
  "`:indent-rail` — the outline indent guide (§6, genuinely new). A container
   that stacks its children in a column indented by :indent px and paints a thin
   vertical rail in the gutter. Measures its own :h bottom-up from the built
   children (measure rule, trap T7). The rail rect is marked
   :data {:layout-skip? true} so the engine's layout-children (rect_tree.cljc:107)
   leaves it at the gutter while positioning the real children at x = indent."
  [ctx props children]
  (let [geom   (:geom ctx)
        w      (content-w props geom)
        indent (or (:indent props) 16)
        gap    (or (:gap props) 0)
        rail-w (or (:rail-width props) 1)
        rail-x (or (:rail-inset props) (quot indent 2))
        color  (or (:color props) (get-in dt [:colors :border-subtle]))
        content-h (+ (sum-children-h children) (gap-total gap (count children)))
        rail   (rt-node (child-id (:id ctx) :rail) :indent-rail-guide
                        {:x rail-x :y 0 :w rail-w :h content-h}
                        :style {:bg color}
                        :data {:layout-skip? true})]
    (rt-node (:id ctx) :indent-rail
             {:x 0 :y 0 :w w :h content-h}
             :layout {:direction :column :gap gap :padding [0 0 0 indent]}
             :children (into [rail] children))))

;; ===========================================================================
;; smalltalk-ui-vm P1 — block anatomy leaves
;; ===========================================================================

(def block-fg [0.92 0.92 0.94 1.0])
(def block-dim [0.55 0.58 0.62 1.0])
(def block-error [0.95 0.45 0.40 1.0])
(def block-amber [0.92 0.75 0.35 1.0])

(defn text-op
  "The ground renderer's positioned monospace text op. Kept in this shared
   vocabulary so the interpreted block, copy path, and provisional composers
   use one byte-identical helper."
  [text i line-h fs [r g b a] x]
  {:text text :type :text :from 0 :to (count text)
   :x x :y (+ (* i line-h) fs) :size fs
   :r r :g g :b b :a a})

(defn wrap-lines
  "Compatibility name for Contract-T's ground-block wrapper."
  [lines col]
  (tl/block-wrap-lines lines col))

(defn lines-offset
  "Visual {:line :col} over rendered lines -> flat newline-joined offset."
  [lines {:keys [line col]}]
  (let [n (count lines)
        line (max 0 (min (long (or line 0)) (dec (max 1 n))))
        col (max 0 (min (long (or col 0)) (count (nth lines line ""))))]
    (+ (reduce + 0 (map #(inc (count %)) (take line lines))) col)))

(defn block-render-lines
  "The one split/wrap/header operation shared by view derivation and the root
   primitive. Contract T0 keeps this compatibility reader over the one layout
   provider; it owns no wrapping truth."
  [text machine? wrap-col headers]
  (:lines
   (tl/wrap-result
    (tl/layout {:text (or text "")
                :font-size 1
                :char-advance 1
                :line-height 1
                :max-chars (when machine? wrap-col)
                :wrap-policy :block-greedy
                :headers headers}))))

(defn- ground-block-layout
  "Build the one result shared by the interpreted block's root, selection, and
   caret readers. The data context makes their semantic input identical without
   changing the settled persisted anatomy grammar."
  [ctx props required-line required-col]
  (let [view (get-in ctx [:data-context :view])
        actual? (or (contains? props :text) (contains? view :text))
        text (or (:text props) (:text view) "")
        machine? (boolean (or (:machine? props) (:machine? view)))
        wrap-col (or (:wrap-col props) (:wrap-col view))
        headers (or (:headers props) (:headers view) [])
        fs (or (:font-size props) (:font-size view) 14)
        ca (or (:char-advance props) (:char-advance view) 8)
        lh (or (:line-h props) (:line-h view) 20)
        provider (or (:layout-provider props)
                     (:layout-provider view)
                     (get-in ctx [:geom :layout-provider])
                     (get-in ctx [:geom :text-provider]))
        carried-result (or (:layout-result props) (:layout-result view))
        request {:subject-id (:view-instance ctx)
                 :op-role :block-root
                 :occurrence 0
                 :stamp (or (:revision-stamp props) (:revision-stamp view))
                 :body-text text
                 :header-texts headers
                 :provider provider
                 :font-size fs
                 :line-height lh
                 :baseline-offset fs
                 :wrap-policy :block-greedy
                 :wrap-col (when machine? wrap-col)
                 :source-id (:address ctx)}
        acquire (get-in ctx [:geom :layout-acquire])
        fallback-lines
        (when-not actual?
          (let [line-count (max 1 (inc (long (or required-line 0))))
                chars (apply str (repeat (max 0 (long (or required-col 0))) " "))]
            (assoc (vec (repeat line-count "")) (dec line-count) chars)))]
    (cond
      carried-result carried-result
      (and acquire (nil? fallback-lines))
      (acquire request)
      :else
      (tl/layout (cond-> {:text text
                          :provider provider
                          :font-size fs
                          :char-advance ca
                          :line-height lh
                          :baseline-offset fs
                          :wrap-col (when machine? wrap-col)
                          :max-chars (when (and machine? (nil? provider)) wrap-col)
                          :wrap-policy :block-greedy
                          :headers headers
                          :source-id (:address ctx)}
                   fallback-lines (assoc :source-lines fallback-lines
                                         :headers []))))))

(defn block-root-prim
  "The root part is the character grid. T2: all instance variation arrives in
   props; this builder has no component-identity dispatch."
  [ctx {:keys [text machine? wrap-col headers font-size char-advance line-h
               tint pad placement-derived? stamps]
        :or {headers []
             font-size 14
             char-advance 8
             line-h 20
             tint block-dim
             pad 0
             stamps {}}}
   children]
  (let [layout-result (ground-block-layout ctx
                                           {:text text :machine? machine?
                                            :wrap-col wrap-col :headers headers
                                            :font-size font-size
                                            :char-advance char-advance
                                            :line-h line-h}
                                           0 0)
        lines (:lines (tl/wrap-result layout-result))
        layout-lines (:lines layout-result)
        nh (count headers)
        n (count lines)
        logical-w (first (get-in (tl/measure-result layout-result)
                                 [:metrics :advance]))
        logical-h (second (get-in (tl/measure-result layout-result)
                                  [:metrics :advance]))
        w (+ (max char-advance logical-w) (* 2 pad))
        h (+ logical-h (* 2 pad))
        carry-layout? (boolean (get-in ctx [:geom :layout-acquire]))
        base-ops
        (if carry-layout?
          (tl/line-paint-ops
           layout-result
           {:type :text :size font-size :r 1.0 :g 1.0 :b 1.0 :a 1.0})
          ;; The carried-layout payload is a ground renderer seam, not a new
          ;; primitive-tree schema.  Keep non-ground/legacy anatomy byte-for-
          ;; byte compatible while the live ground takes the indexed layout
          ;; result all the way to paint.
          (mapv (fn [line layout-line]
                  (let [[x y] (:baseline layout-line)]
                    {:text line :type :text :from 0
                     :to (tl/code-unit-count line)
                     :x x :y y :size font-size
                     :r 1.0 :g 1.0 :b 1.0 :a 1.0}))
                lines layout-lines))
        ops
        (mapv
          (fn [i line op]
            (let [header? (< i nh)
                  body? (and machine? (not header?))
                  p-stamp (when header? (:header-provenance stamps))
                  f-stamp (when header?
                            (if (zero? i)
                              (:noise-header stamps)
                              (:prose-header stamps)))
                  body-stamp (when body? (:body stamps))]
              (let [[r g b a] (cond
                                header? tint
                                machine? block-dim
                                :else block-fg)]
                (cond->
                    (assoc op :text line :r r :g g :b b :a a)
                carry-layout? (assoc :layout/surface :ground)
                header? (merge p-stamp)
                body? (merge body-stamp)
                header?
                (assoc :material/contributions [p-stamp f-stamp])))))
          (range) lines base-ops)
        address (:address ctx)
        data
        (cond->
            (merge
             {:address address}
             (:hit stamps)
             {:material/claim
              {:claim/subject address
               :claim/site (if machine?
                             :block/machine-hit-area
                             :block/user-hit-area)
               :claim/facets [:attention :positioned]
               :claim/args {}}})
          placement-derived?
          (assoc :material/positioned (:positioned stamps))

          (not machine?)
          (assoc :material/threaded (:threaded stamps)))]
    (rt-node :ground-block :text-run
             {:x 0 :y 0
              :w (max w char-advance)
              :h (max h line-h)}
             :text ops
             :data data
             :children (vec children))))

(defn block-selection-wash-prim
  [ctx {:keys [line col-start col-len char-advance line-h] :as props
         :or {line 0 col-start 0 col-len 0 char-advance 8 line-h 20}}
   _children]
  (let [layout-result (ground-block-layout ctx props line (+ col-start col-len))
        rect (:rect (tl/selection-result layout-result line col-start
                                         (+ col-start col-len)
                                         :min-width 2.0))]
    (rt-node (keyword (str "ground-sel-" line))
             :rect rect
             :style {:bg [0.35 0.5 0.8 0.3]})))

(defn block-group-selection-prim
  [_ctx {:keys [w h pad] :or {w 0 h 0 pad 0}} _children]
  (rt-node :ground-gsel :rect
           {:x (- pad) :y (- pad) :w w :h h}
           :style {:border-width 1.5
                   :border-color [0.55 0.65 0.9 0.8]
                   :bg [0.35 0.5 0.8 0.10]}))

(defn block-provenance-rail-prim
  [_ctx {:keys [h pad tint stamp]
         :or {h 0 pad 0 tint [0 0 0 0]}}
   _children]
  (rt-node :ground-mark :rect
           {:x (- pad) :y (- pad) :w 2.5 :h h}
           :style {:bg tint}
           :data stamp))

(defn block-attention-box-prim
  [_ctx {:keys [w h pad border-width border-color bg stamp]
         :or {w 0
              h 0
              pad 0
              border-width 0
              border-color [0 0 0 0]
              bg [0 0 0 0]}}
   _children]
  (rt-node :ground-box :rect
           {:x (- pad) :y (- pad) :w w :h h}
           :style {:border-width border-width
                   :border-color border-color
                   :bg bg}
           :data stamp))

(defn block-caret-prim
  [ctx {:keys [line col char-advance line-h] :as props
         :or {line 0 col 0 char-advance 8 line-h 20}}
   _children]
  (let [layout-result (ground-block-layout ctx props line col)]
    (rt-node :ground-caret :rect
             (:rect (tl/caret-result layout-result line col))
             :style {:bg [0.95 0.95 0.95 1.0]})))

(defn block-refusal-prim
  [_ctx {:keys [refusal line-count w line-h font-size]
         :or {line-count 0 w 0 line-h 20 font-size 14}}
   _children]
  (rt-node :ground-refusal :text-run
           {:x 0 :y (* line-count line-h) :w w :h line-h}
           :text
           [(text-op
             (str "⟂ edit refused: "
                  (if (keyword? refusal) (name refusal) (str refusal)))
             0 line-h font-size block-error 0)]))

(defn block-notice-prim
  [ctx {:keys [notice refusal line-count w line-h font-size]
         :or {line-count 0 w 0 line-h 20 font-size 14}}
   _children]
  (cond->
      (rt-node :ground-notice :text-run
               {:x 0
                :y (* (+ line-count (if refusal 1 0)) line-h)
                :w w
                :h line-h}
               :text [(text-op (str notice) 0 line-h font-size block-amber 0)])
    (get-in ctx [:geom :layout-acquire])
    (assoc :data {:ground/op-role
                  (tl/ground-op-role {:part-id (last (:id ctx))})})))

(defn block-boundary-prim
  [_ctx {:keys [w line-h font-size tint stamp]
         :or {w 0 line-h 20 font-size 14 tint block-dim}}
   _children]
  (rt-node :ground-episode-boundary :text-run
           {:x 0 :y (- (* 1.6 line-h)) :w w :h line-h}
           :text [(text-op "— fresh session —" 0 line-h font-size tint 0)]
           :data stamp))

(defn block-conflict-lint-prim
  [_ctx {:keys [i conflict w h line-h font-size]
         :or {i 0
              conflict {:type :unknown}
              w 0
              h 0
              line-h 20
              font-size 14}}
   _children]
  (rt-node (keyword (str "ground-material-conflict-" i))
           :error-card
           {:x 0
            :y (+ h (* i line-h))
            :w w
            :h line-h}
           :style {:bg [0.24 0.07 0.08 0.98]
                   :border-width 1.0
                   :border-color [0.9 0.3 0.3 1.0]}
           :text
           [(text-op
             (str "material conflict · " (name (:type conflict)))
             0 line-h font-size block-error 0)]
           :data conflict))

(defn block-gold-mark-prim
  [_ctx {:keys [text count boundary? w char-advance line-h font-size]
         :or {count 0 w 0 char-advance 8 line-h 20 font-size 14}}
   _children]
  (rt-node :ground-wish-mark :text-run
           {:x 0
            :y (- (* (if boundary? 2.8 1.4) line-h))
            :w (max w (* 42 char-advance))
            :h line-h}
           :text
           [(text-op
             (str "⌁ " text
                  (when (> count 1)
                    (str "  +" (dec count))))
             0 line-h font-size block-amber 0)]))

(defn block-silver-mark-prim
  [_ctx {:keys [text count boundary? gold? w char-advance line-h font-size]
         :or {count 0 w 0 char-advance 8 line-h 20 font-size 14}}
   _children]
  (rt-node :ground-silver-mark :text-run
           {:x 0
            :y (- (* (+ (if boundary? 1.4 0.0)
                        (if (pos? (or gold? 0)) 1.4 0.0)
                        1.4)
                     line-h))
            :w (max w (* 48 char-advance))
            :h line-h}
           :text
           [(text-op
             (str "≈ machine guess · " text
                  (when (> count 1)
                    (str "  +" (dec count))))
             0 line-h font-size block-dim 0)]))

(defn block-fold-header-hit-prim
  [ctx {:keys [i section fold-key w line-h stamp]
        :or {i 0 section :unknown w 0 line-h 20}}
   _children]
  (let [address (:address ctx)]
    (rt-node (keyword (str "ground-fold-header-hit-" (name section)))
             :hit-area
             {:x 0 :y (* i line-h) :w w :h line-h}
             :data
             (merge
              stamp
              {:address address
               :material/claim
               {:claim/subject address
                :claim/site :block/fold-header
                :claim/facets [:foldable]
                :claim/args {:section section :fold-key fold-key}}}))))

(defn thread-edge-rail-prim
  [_ctx {:keys [h pad indent width color stamp]
         :or {h 0 pad 0 indent 0 width 0 color [0 0 0 0]}}
   _children]
  (rt-node :ground-thread-edge-rail :rect
           {:x (- indent) :y (- pad) :w width :h h}
           :style {:bg color}
           :data stamp))

(defn thread-indent-prim
  [_ctx {:keys [indent width color stamp]
         :or {indent 0 width 0 color [0 0 0 0]}}
   _children]
  (rt-node :ground-thread-indent :rect
           {:x (- indent) :y 0 :w indent :h width}
           :style {:bg color}
           :data stamp))

(defn sub-anatomy-prim
  "A total backstop for the compile-time-only keyword. Valid anatomy expands
   this before assembly compilation."
  [ctx _props _children]
  (rt-node (:id ctx) :error-card
           {:x 0 :y 0 :w 240 :h 24}
           :style {:bg [0.35 0.10 0.10 1.0]}
           :text [(text-op "unexpanded sub-anatomy" 0 20 14 block-error 4)]))

;; ===========================================================================
;; W2 lane-E GAP-FILL primitives (CONTRACT §16/§19 G22-G23) — minted for the
;; two design-round transcriptions (1e Boxes · 1f Minimap+Reader, Sid's picks
;; 2026-07-11). Extraction-and-gap-fill, not invention (§6): each primitive
;; below cites the design element it exists for (BlockExplorer.dc.html under
;; build/framework/design-round/). The CSS is claude-design's medium; these
;; transcribe STRUCTURE through the land's own tokens, never pixel-port.
;; ===========================================================================

(defn- words-in
  "Word count, computed INSIDE the primitive (measure rule §6; the design-round
   INTENT.md transcription note: 'no per-block word counts unless computed by a
   primitive'). A string counts its non-whitespace tokens; a sequential of
   block maps sums over their :text (the design's per-turn words,
   BlockExplorer.dc.html buildBoxes t.words)."
  [x]
  (cond
    (string? x)     (count (re-seq #"\S+" x))
    (sequential? x) (reduce + 0 (map #(words-in (:text %)) x))
    :else           0))

(def sliver-palette
  "Block-kind -> color for the kind-coded design elements (sliver bars,
   header dots/labels). Hex origins are the design's TYPE map
   (BlockExplorer.dc.html:448-456), converted to the land's [r g b a] floats
   at the design's non-toolish sliver alpha 0.6 (:814). v0 kinds are a small
   open set (fixtures use 'text'/'heading'/'code'; the worn projection emits
   e.g. :human-message) — unknown kinds take the neutral default in the
   consuming primitive. The design's toolish dimming (alpha 0.3 / rowOp 0.62)
   rides a noise-class taxonomy the data does not carry (:kind only) — a G23
   named data lack, never improvised here."
  {"human-message" [0.373 0.690 0.918 0.6]   ;; #5fb0ea user-message
   "user-message"  [0.373 0.690 0.918 0.6]
   "heading"       [0.337 0.749 0.651 0.6]   ;; #56bfa6 structure teal
   "text"          [0.498 0.788 0.561 0.6]   ;; #7fc98f agent prose
   "thinking"      [0.706 0.557 0.871 0.6]   ;; #b48ede agent-thinking
   "code"          [0.851 0.627 0.357 0.6]   ;; #d9a05b tool-call gold
   "tool-result"   [0.612 0.541 0.369 0.6]}) ;; #9c8a5e result khaki

(defn- kind-color
  "Palette lookup for a :kind (keyword or string), nil when unknown."
  [kind]
  (when kind (get sliver-palette (name (keyword kind)))))

(defn box-prim
  "`:box` — W2 lane-E gap-fill: the bordered rounded CONTAINMENT frame of the
   Boxes/Minimap designs — russian-doll turn frames, role frames and block
   cards (BlockExplorer.dc.html:277 turn frame, :285 user frame, :296 chunk
   card, :309 response frame, :319 block card — Boxes; :358 strip group card,
   :385/:412 reader frames — Minimap+Reader). `:card` cannot express it: the
   harvested ui-card carries no :layout, so its children would not stack.
   Column layout with :padding/:gap;
   measures its own :h bottom-up from the already-built children (measure
   rule, trap T7 — measure in the primitive, arrange in the engine)."
  [ctx props children]
  (let [geom  (:geom ctx)
        w     (content-w props geom)
        gap   (or (:gap props) 0)
        pad   (or (:padding props) 0)
        [pt _pr pb _pl] (rt/normalize-padding pad)
        h     (or (:h props)
                  (+ pt pb (sum-children-h children)
                     (gap-total gap (count children))))
        style (cond-> {}
                (:bg props)     (assoc :bg (:bg props))
                (:radius props) (assoc :radius (:radius props))
                (:border-width props)
                (assoc :border-width (:border-width props)
                       :border-color (or (:border-color props)
                                         (get-in dt [:colors :border])))
                (:border-widths props)
                (assoc :border-widths (:border-widths props)
                       :border-color (or (:border-color props)
                                         (get-in dt [:colors :border]))))]
    (rt-node (:id ctx) :box
             {:x 0 :y 0 :w w :h h}
             :style style
             :layout {:direction :column :gap gap :padding pad
                      :align (or (:align props) :start)}
             :children (vec children))))

(defn header-band-prim
  "`:header-band` — W2 lane-E gap-fill: the fixed-height header strip every
   Boxes/Minimap frame opens with — leading role dot + bold label at the left,
   muted meta RIGHT-ALIGNED at the far edge (the design's `flex:1` spacer made
   flesh): BlockExplorer.dc.html:278-283 turn header, :286-293 user-message
   header, :310-316 response header, :320-329 block-card header, :355 strip
   header, :374-383 reader top bar. Emits its own positioned text ops (measure
   rule §6; the `build-empty-state` pattern). Right-alignment measures text
   width as fs x (geom advance ratio), falling back to the ONE
   `fallback-char-width` constant — never a second width literal (G22/F10 law).
   Props: :label (+ :label-prefix — the design's 'T'/'turn ' literals,
   :360/:377), :sub, and ONE meta slot: :meta (explicit) | :words-of (string or
   block-seq -> 'Nw', computed inside the primitive per INTENT.md) | :count-of
   (+ :count-suffix); :kind derives dot+label color from `sliver-palette` (the
   design's per-type b.color, :321-322), :dot-color/:label-color override;
   :h :bg :border-bottom? :size. Interaction verbs on design headers
   (click-to-pin, expand toggles, nav arrows) are :actions-class — v0-out
   (CONTRACT §4 reserved), logged as G23 named lacks."
  [ctx props _children]
  (let [geom   (:geom ctx)
        w      (content-w props geom)
        h      (or (:h props) 24)
        fs     (or (:size props) (:xs (:font-sizes dt)))
        ratio  (let [ga (:char-advance geom) gf (:font-size geom)]
                 (if (and (number? ga) (number? gf) (pos? gf))
                   (/ ga gf)
                   fallback-char-width))
        ca     (* fs ratio)
        kc     (some-> (:kind props) kind-color (assoc 3 1.0))
        dotc   (or (:dot-color props) kc)
        pl0    10
        pl     (if dotc (+ pl0 7 8) pl0)
        ;; double division on purpose: golden snapshots are EDN and
        ;; clojure.edn cannot read ratio literals
        ty     (+ (/ h 2.0) (/ fs 2.5))
        label  (str (:label-prefix props "") (:label props ""))
        sub    (some-> (:sub props) str)
        meta*  (cond
                 (some? (:meta props))     (str (:meta props))
                 (some? (:words-of props)) (str (words-in (:words-of props)) "w")
                 (some? (:count-of props)) (str (count (:count-of props))
                                                (:count-suffix props ""))
                 :else nil)
        lc     (or (:label-color props) kc (get-in dt [:colors :fg]))
        mc     (get-in dt [:colors :fg-muted])
        sub-x  (+ pl (* (count label) ca) 8)
        ops    (cond-> []
                 (seq label)
                 (conj {:text label :type :keyword :from 0 :to (count label)
                        :x pl :y ty :size fs
                        :r (nth lc 0) :g (nth lc 1) :b (nth lc 2) :a (nth lc 3)})
                 sub
                 (conj {:text sub :type :comment :from 0 :to (count sub)
                        :x sub-x :y ty :size fs
                        :r (nth mc 0) :g (nth mc 1) :b (nth mc 2) :a (nth mc 3)})
                 meta*
                 (conj {:text meta* :type :comment :from 0 :to (count meta*)
                        ;; right-aligned: the design's `flex:1` spacer
                        :x (max pl (- w 10 (* (count meta*) ca))) :y ty :size fs
                        :r (nth mc 0) :g (nth mc 1) :b (nth mc 2) :a (nth mc 3)}))
        dot    (when dotc
                 (rt-node (child-id (:id ctx) :dot) :header-dot
                          {:x pl0 :y (/ (- h 7) 2.0) :w 7 :h 7}
                          :style {:bg dotc :radius (:full (:radii dt))}
                          :data {:layout-skip? true}))
        style  (cond-> {}
                 (:bg props) (assoc :bg (:bg props))
                 (:border-bottom? props)
                 (assoc :border-widths [0 0 1 0]
                        :border-color (get-in dt [:colors :border-subtle])))]
    (rt-node (:id ctx) :header-band
             {:x 0 :y 0 :w w :h h}
             :style style
             :text ops
             :children (if dot [dot] []))))

(defn text-clip-prim
  "`:text-clip` — W2 lane-E gap-fill: prose clipped at :max-lines with an
   HONEST visible remainder stub — the Boxes intent line ('long blocks clip at
   ~6 lines') and the design's clip branch + measured '▸ N …' stub
   (BlockExplorer.dc.html:333-338 showClip, :488 b.stub). Wraps ONCE via
   `wrap-line` (G8 discipline; `:text-layout` NOWHERE — the dead hook, PROBE
   Finding 1, trap T7), emits its own positioned ops for the first :max-lines
   wrapped lines plus one muted '▸ N more lines' indicator op when clipped,
   and measures :h = shown-lines(+stub) x line-height + padding. :data carries
   {:text/clipped? :text/lines-total :text/lines-shown} so the tree never lies
   about the cut (map-must-not-lie). The design's expand/collapse TOGGLE is
   :actions-class (v0 read-only, CONTRACT §4) and its bottom fade gradient is
   cosmetic — both logged as G23 named lacks, not improvised."
  [ctx props children]
  (let [geom      (:geom ctx)
        w         (content-w props geom)
        fs        (or (:size props) (:font-size geom) 14)
        lh        (or (:line-height props) (:line-height geom) 20)
        ca        (or (:char-advance geom) (* fs fallback-char-width))
        pad       (or (:padding props) 0)
        [pt pr pb pl] (rt/normalize-padding pad)
        avail     (max 1 (- w pl pr))
        max-chars (max 1 (int (/ avail ca)))
        max-lines (max 1 (or (:max-lines props) 6))
        value     (str (:value props (:text props "")))
        raw       (str/split-lines value)
        lines     (vec (mapcat (fn [ln] (wrap-line ln max-chars)) raw))
        total     (count lines)
        clipped?  (> total max-lines)
        shown     (if clipped? (subvec lines 0 max-lines) lines)
        n         (max 1 (count shown))
        color     (or (:color props) (get-in dt [:colors :fg]))
        mc        (get-in dt [:colors :fg-muted])
        ops       (vec (map-indexed
                        (fn [i line]
                          {:text line :type (or (:text-type props) :text)
                           :from 0 :to (count line)
                           :x pl :y (+ pt (* i lh))
                           :size fs
                           :r (nth color 0) :g (nth color 1)
                           :b (nth color 2) :a (nth color 3)})
                        shown))
        stub      (when clipped?
                    (let [s (str "▸ " (- total max-lines) " more lines")]
                      {:text s :type :comment :from 0 :to (count s)
                       :x pl :y (+ pt (* n lh)) :size fs
                       :r (nth mc 0) :g (nth mc 1) :b (nth mc 2) :a (nth mc 3)}))
        rows      (if clipped? (inc n) n)
        h         (+ pt pb (* rows lh))]
    (rt-node (:id ctx) :text-clip
             {:x 0 :y 0 :w w :h h}
             :text (if stub (conj ops stub) ops)
             :data {:text/clipped?     clipped?
                    :text/lines-total  total
                    :text/lines-shown  (count shown)}
             :children (vec children))))

(defn sliver-prim
  "`:sliver` — W2 lane-E gap-fill: the Minimap strip's per-block DENSITY BAR —
   'navigate by density': height ∝ √words, clamped [2,26] px, exactly the
   design's formula `max(2, min(26, round(sqrt(words)*0.75)))`
   (BlockExplorer.dc.html:795, and the strip's own header names the law:
   'map · h ∝ √words', :355), color keyed by block :kind through
   `sliver-palette` (:814). Words are computed INSIDE the primitive from
   :text (measure rule §6; INTENT.md sanctions primitive-computed counts —
   the projection carries none: a G23 named data lack). :data carries the
   computed {:sliver/words} so tests assert the proportionality law, never a
   magic height."
  [ctx props _children]
  (let [geom  (:geom ctx)
        w     (content-w props geom)
        words (words-in (str (:text props "")))
        h     (max 2 (min 26 (int (+ 0.5 (* 0.75 (Math/sqrt words))))))
        kind  (some-> (:kind props) keyword name)
        color (or (:color props)
                  (kind-color kind)
                  [0.42 0.51 0.60 0.6])]
    (rt-node (:id ctx) :sliver
             {:x 0 :y 0 :w w :h h}
             :style {:bg color :radius 1}
             :data {:sliver/words words :sliver/kind kind})))

;; ===========================================================================
;; §6 BUILDER-FN wrappers — adapt (ctx props children) to the verbatim copies.
;; Each MEASURES the node's bounds from geom + already-built children, then
;; calls the original bounds-passing builder. These are the registry values.
;; ===========================================================================

(defn card-prim
  [ctx props children]
  (let [geom (:geom ctx)
        w    (content-w props geom)
        h    (max (or (:h props) 0) (sum-children-h children) 1)]
    (ui-card (:id ctx) {:x 0 :y 0 :w w :h h}
             :children children
             :text (:text props [])
             :shadow (or (:shadow props) :md)
             :variant (or (:variant props) :default))))

(defn badge-prim
  [ctx props children]
  (let [geom  (:geom ctx)
        label (str (:label props (:text props "")))
        fs    (or (:font-size props) (:xs (:font-sizes dt)))
        ca    (or (:char-advance geom) (* fs fallback-char-width))
        w     (or (:w props) (+ 12 (* (count label) ca)))
        h     (or (:h props) (+ fs 8))]
    (ui-badge (:id ctx) {:x 0 :y 0 :w w :h h} label
              :color (or (:color props) (:accent-muted (:colors dt)))
              :text-color (or (:text-color props) (:accent (:colors dt)))
              :font-size fs)))

(defn divider-prim
  [ctx props _children]
  (ui-divider (:id ctx)
              {:x 0 :y 0 :w (content-w props (:geom ctx)) :h (or (:h props) 1)}
              :color (or (:color props) (:border-subtle (:colors dt)))))

(defn scrollbar-prim
  [ctx props _children]
  (let [geom (:geom ctx)
        w    (or (:w props) 8)
        h    (or (:h props) (:viewport-h geom) 100)]
    (ui-scrollbar (named-id (:id ctx)) {:x 0 :y 0 :w w :h h}
                  (or (:thumb-pct props) 1.0)
                  (or (:thumb-offset props) 0.0))))

(defn empty-state-prim
  [ctx props _children]
  (let [geom (:geom ctx)
        w    (content-w props geom)
        h    (or (:h props) (:viewport-h geom) 200)]
    (build-empty-state (:id ctx) w h
                       {:icon (:icon props)
                        :headline (:headline props)
                        :description (:description props)})))

(defn panel-prim
  [ctx props children]
  (let [geom (:geom ctx)
        w    (content-w props geom)
        h    (or (:h props) (:viewport-h geom) (max 1 (sum-children-h children)))]
    (ui-panel (:id ctx) {:x 0 :y 0 :w w :h h}
              :children children
              :variant (or (:variant props) :default)
              :style (:style props))))

(defn panel-header-prim
  [ctx props children]
  (ui-panel-header (:id ctx)
                   (content-w props (:geom ctx))
                   (or (:h props) list-padding-top)
                   :children children
                   :text (:text props [])
                   :style (:style props)))

(defn panel-content-prim
  [ctx props children]
  (let [geom (:geom ctx)
        w    (content-w props geom)
        h    (or (:h props) (:viewport-h geom)
                 (max 1 (+ (sum-children-h children)
                           (gap-total list-group-gap (count children)))))]
    (ui-panel-content (:id ctx) w h
                      :children children
                      :layout-opts (:layout-opts props))))

(defn panel-footer-prim
  [ctx props children]
  (ui-panel-footer (:id ctx)
                   (content-w props (:geom ctx))
                   (or (:h props) list-footer-h)
                   :children children
                   :text (:text props [])
                   :style (:style props)))

(defn panel-group-prim
  [ctx props children]
  (ui-panel-group (named-id (:id ctx))
                  (content-w props (:geom ctx))
                  :label (:label props)
                  :status (:status props)
                  :collapsed? (boolean (:collapsed? props))
                  :items children
                  :first-group? (boolean (:first-group? props))))

(defn list-item-prim
  [ctx props children]
  (ui-list-item (named-id (:id ctx))
                (content-w props (:geom ctx))
                :title (:title props)
                :leading (first children)
                :trailing (second children)
                :selected? (boolean (:selected? props))
                :hovered? (boolean (:hovered? props))
                :ghost? (boolean (:ghost? props))
                :data (:data props)))

(defn omissions-prim
  "Primitive over the canonical `omissions-block`. The omission
   entries arrive via props (:omissions, a :bind path). omissions-block returns
   nil on empty input; the §6 interface must return an rt-node, so an empty set
   coalesces to a zero-height node (honest gap, never a crash)."
  [ctx props _children]
  (let [geom (:geom ctx)]
    (or (omissions-block (:omissions props)
                         {:line-height (or (:line-height geom) 18)
                          :card-w (content-w props geom)})
        (rt-node (:id ctx) :omissions
                 {:x 0 :y 0 :w (content-w props geom) :h 0}))))

(defn hole-card-prim
  "Primitive over the canonical `hole-endpoint-card`. The hole row
   arrives via props (:row, a :bind path; falls back to the whole props map).
   A row without :relation-id gets the interpreter's STRUCTURAL id injected —
   the original falls back to `(hash row)`, and `hash` is platform-divergent
   (JVM murmur ≠ cljs hash), which would leak a JVM-vs-browser-different node
   id into the tree (G16 falsification fix; the wrapper closes the path)."
  [ctx props _children]
  (let [geom (:geom ctx)
        row0 (or (:row props) props)
        row  (cond-> row0 (nil? (:relation-id row0)) (assoc :relation-id (:id ctx)))]
    (hole-endpoint-card row
                        {:card-w (content-w props geom)
                         :line-height (or (:line-height geom) 18)})))

;; ===========================================================================
;; The registry — a plain map value (§6). No global mutable state; tests pass
;; their own. Persisted form is keyword + code-address (trap T5, §8), never the
;; fn values below — those bind at load.
;; ===========================================================================

(def registry
  "keyword -> §6 builder-fn `(fn [ctx props children] -> rt-node)`."
  {;; the ui-* builders (canonical here; extracted from the old workspace)
   :panel         panel-prim
   :panel-header  panel-header-prim
   :panel-content panel-content-prim
   :panel-footer  panel-footer-prim
   :panel-group   panel-group-prim
   :list-item     list-item-prim
   :card          card-prim
   :badge         badge-prim
   :divider       divider-prim
   :empty-state   empty-state-prim
   :scrollbar     scrollbar-prim
   ;; harvested omission and hole primitives
   :omissions     omissions-prim
   :hole-card     hole-card-prim
   ;; genuinely new (§6)
   :stack         stack-prim
   :text-run      text-run-prim
   :indent-rail   indent-rail-prim
   ;; W2 lane-E gap-fill (design-round transcription, CONTRACT §16/G22-G23)
   :box           box-prim
   :header-band   header-band-prim
   :text-clip     text-clip-prim
   :sliver        sliver-prim
   ;; smalltalk-ui-vm P1 block vocabulary. T3: anatomy material names only
   ;; these keywords; function values never cross the material boundary.
   :block-root              block-root-prim
   :block-selection-wash    block-selection-wash-prim
   :block-group-selection   block-group-selection-prim
   :block-provenance-rail   block-provenance-rail-prim
   :block-attention-box     block-attention-box-prim
   :block-caret             block-caret-prim
   :block-refusal           block-refusal-prim
   :block-notice            block-notice-prim
   :block-boundary          block-boundary-prim
   :block-conflict-lint     block-conflict-lint-prim
   :block-gold-mark         block-gold-mark-prim
   :block-silver-mark       block-silver-mark-prim
   :block-fold-header-hit   block-fold-header-hit-prim
   :thread-edge-rail        thread-edge-rail-prim
   :thread-indent           thread-indent-prim
   :sub-anatomy             sub-anatomy-prim})
