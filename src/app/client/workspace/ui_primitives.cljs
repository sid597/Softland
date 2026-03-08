(ns app.client.workspace.ui-primitives
  "Design tokens, typography, and reusable UI component builders."
  (:require [clojure.string :as str]
            [app.client.workspace.rect-tree :as rt :refer [rt-node wrap-line]]
            [components.design-tokens :as design-tokens]))

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

(def priority-colors
  "Priority level -> RGBA color used by generic list/ticket UI."
  {1 {:r 0.95 :g 0.30 :b 0.30 :a 1.0}
   2 {:r 0.95 :g 0.60 :b 0.25 :a 1.0}
   3 {:r 0.90 :g 0.80 :b 0.30 :a 1.0}
   4 {:r 0.45 :g 0.85 :b 0.45 :a 1.0}
   0 {:r 0.55 :g 0.55 :b 0.60 :a 0.8}})

;; ============================================================================
;; DESIGN TOKENS (Linear/shadcn-inspired dark theme)
;; ============================================================================

(def dt
  "Design tokens — imported from shared components.design-tokens."
  design-tokens/dt)

;; --- Typography hierarchy ----------------------------------------------------
;; Consistent type scale: reference these instead of ad-hoc font sizes/alphas.

(def typo-title    {:size (:xl (:font-sizes dt)) :a 1.0})
(def typo-subtitle {:size (:lg (:font-sizes dt)) :a 0.9})
(def typo-body     {:size (:md (:font-sizes dt)) :a 0.85})
(def typo-caption  {:size (:sm (:font-sizes dt)) :a 0.6})

;; ============================================================================
;; COMPONENT LIBRARY (pure fns → rt-node trees)
;; ============================================================================

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

(defn ui-button
  "Button component: solid/outline/ghost variants. Returns an rt-node."
  [id bounds label & {:keys [variant on-click font-size]
                      :or {variant :solid font-size (:sm (:font-sizes dt))}}]
  (let [accent (:accent (:colors dt))
        styles (case variant
                 :solid   {:bg accent :radius (:md (:radii dt))}
                 :outline {:bg [0 0 0 0] :radius (:md (:radii dt))
                           :border-width 1 :border-color accent}
                 :ghost   {:bg [0 0 0 0] :radius (:md (:radii dt))}
                 {:bg accent :radius (:md (:radii dt))})
        text-c (case variant
                 :solid [1 1 1 1]
                 :outline accent
                 :ghost (:fg-muted (:colors dt))
                 [1 1 1 1])]
    (rt-node id :button bounds
             :style styles
             :actions (if on-click {:click on-click} {})
             :text [{:text label :type :text
                     :from 0 :to (count label)
                     :x (:sm (:spacing dt)) :y (- (:h bounds) 5)
                     :size font-size
                     :r (nth text-c 0) :g (nth text-c 1)
                     :b (nth text-c 2) :a (nth text-c 3)}])))

(defn ui-divider
  "Thin horizontal separator line. Returns an rt-node."
  [id bounds & {:keys [color] :or {color (:border-subtle (:colors dt))}}]
  (rt-node id :divider bounds :style {:bg color}))

(defn ui-progress
  "Progress bar (track + fill). Returns an rt-node with child fill rect."
  [id bounds progress & {:keys [color track-color]
                         :or {color (:accent (:colors dt))
                              track-color (:bg-muted (:colors dt))}}]
  (let [fill-w (* (:w bounds) (min 1.0 (max 0.0 progress)))]
    (rt-node id :progress bounds
             :style {:bg track-color :radius (:sm (:radii dt))}
             :children [(rt-node (keyword (str (name id) "-fill")) :progress-fill
                          {:x 0 :y 0 :w fill-w :h (:h bounds)}
                          :style {:bg color :radius (:sm (:radii dt))})])))

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

(defn ui-tabs
  "Tab bar with active indicator. Returns an rt-node.
   tabs: [{:id :label}], active-id: keyword."
  [id bounds tabs active-id & {:keys [font-size padding]
                                :or {font-size (:sm (:font-sizes dt)) padding 16}}]
  (let [char-adv (* font-size 0.56)
        {tab-nodes :nodes}
        (reduce
          (fn [{:keys [nodes tx]} tab]
            (let [active? (= (:id tab) active-id)
                  label-str (:label tab)
                  text-w (* (count label-str) char-adv)
                  tab-w (+ text-w (* 2 padding))]
              {:nodes
               (conj nodes
                 (rt-node (:id tab) :tab
                   {:x tx :y 0 :w tab-w :h (:h bounds)}
                   :data {:tab-id (:id tab)}
                   :children (when active?
                               [(rt-node (keyword (str (name (:id tab)) "-ind")) :tab-indicator
                                  {:x padding :y (- (:h bounds) 2) :w text-w :h 2}
                                  :style {:bg [0.9 0.9 0.92 1.0]})])
                   :text [{:text label-str :type (if active? :keyword :text)
                           :from 0 :to (count label-str)
                           :x padding :y (- (:h bounds) 14)
                           :size font-size
                           :r (if active? 0.9 0.55)
                           :g (if active? 0.9 0.55)
                           :b (if active? 0.92 0.60)
                           :a 1.0}]))
               :tx (+ tx tab-w)}))
          {:nodes [] :tx 0}
          tabs)]
    (rt-node id :tab-bar bounds
             :style {:bg (:bg-elevated (:colors dt))
                     :border-widths [0 0 1 0]
                     :border-color (:border (:colors dt))}
             :children tab-nodes)))

(defn ui-tooltip
  "Small floating card with text, positioned at anchor. Returns an rt-node."
  [id bounds label & {:keys [font-size] :or {font-size (:xs (:font-sizes dt))}}]
  (let [fg (:fg (:colors dt))]
    (ui-card id bounds
             :shadow :sm
             :variant :elevated
             :text [{:text label :type :text
                     :from 0 :to (count label)
                     :x (:sm (:spacing dt)) :y (- (:h bounds) 5)
                     :size font-size
                     :r (nth fg 0) :g (nth fg 1) :b (nth fg 2) :a (nth fg 3)}])))

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

;; ============================================================================
;; COMPOSABLE PANEL COMPONENTS (shadcn Sidebar pattern for WebGPU)
;; ============================================================================
;; Pure fns returning rt-nodes with :layout directives.
;; Composition: ui-panel > ui-panel-header > ui-panel-content > ui-panel-group > ui-list-item
;; The layout engine (resolve-layout) handles all child positioning.

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

(defn ui-checkbox
  "Simplified checkbox — single rect, no inner fill child.
   Checked: accent bg + accent border. Unchecked: transparent + subtle border."
  [id & {:keys [checked? size]
         :or {size list-checkbox-size}}]
  (let [cb-x (+ list-padding-x 4)
        cb-y (/ (- list-row-h size) 2)]
    (rt-node id :checkbox
      {:x cb-x :y cb-y :w size :h size}
      :style {:bg (if checked?
                    (:accent (:colors dt))
                    [0.15 0.15 0.18 0.6])
              :radius (:sm (:radii dt))
              :border-width 1.5
              :border-color (if checked?
                              (:accent (:colors dt))
                              [0.30 0.30 0.36 0.5])})))

(defn ui-priority-dot
  "Circular color dot indicating priority level (1-4).
   Uses priority-colors lookup. 8px dot, vertically centered."
  [id priority & {:keys [parent-w] :or {parent-w 0}}]
  (let [prio (or priority 0)
        dot-size 8
        pc (get priority-colors prio {:r 0.55 :g 0.55 :b 0.60 :a 0.8})]
    (rt-node id :priority-dot
      {:x (if (pos? parent-w) (- parent-w 24) 0)
       :y (/ (- list-row-h dot-size) 2)
       :w dot-size :h dot-size}
      :style {:bg [(:r pc) (:g pc) (:b pc) (:a pc)]
              :radius (:full (:radii dt))})))
