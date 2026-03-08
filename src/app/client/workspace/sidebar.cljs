(ns app.client.workspace.sidebar
  "File explorer sidebar: constants, tree flattening, and rect-tree builder."
  (:require [clojure.string :as str]
            [app.client.workspace.rect-tree :refer [rt-node]]
            [app.client.workspace.ui-primitives :as ui
             :refer [dt typo-title typo-subtitle typo-body typo-caption]]))

;; ============================================================================
;; SIDEBAR CONSTANTS (WebGPU-native sidebar)
;; ============================================================================

(def sidebar-w 256)
(def sidebar-tab-h 36)
(def sidebar-row-h 32)
(def sidebar-back-h 48)
(def cmd-panel-h 40)
(def status-bar-h 24)
(def sidebar-breadcrumb-h 24)
(def sidebar-indent-px 14)
(def sidebar-padding-x 16)
(def sidebar-item-inset 8)
(def sidebar-font-size 13)

(defn split-filename
  "Split a filename into [stem extension] at the last dot.
   Handles dotfiles (.gitignore → ['.gitignore' nil]), no-ext (Makefile → ['Makefile' nil]),
   and truncated names ending in '..' (returned as-is, no split)."
  [name]
  (if (str/ends-with? name "..")
    [name nil]  ;; Truncated — don't split the '..' marker
    (let [dot-idx (str/last-index-of name ".")]
      (if (and dot-idx (pos? dot-idx))
        [(subs name 0 dot-idx) (subs name dot-idx)]
        [name nil]))))

(defn flatten-file-tree
  "Recursively walk dir-cache tree and return a flat vector of row descriptors.
   Each row: {:entry {:name :path :type} :depth N :expanded? bool :active? bool}
   Dirs listed before files at each level, both sorted alphabetically."
  [entries expanded-dirs current-file cache depth]
  (let [sorted (sort-by (fn [e] [(if (= (:type e) :dir) 0 1)
                                  (str/lower-case (or (:name e) ""))])
                         entries)]
    (into []
      (mapcat
        (fn [entry]
          (let [is-dir? (= (:type entry) :dir)
                is-exp? (and is-dir? (contains? expanded-dirs (:path entry)))
                is-active? (and (not is-dir?) current-file
                                (= (:path entry) (:path current-file)))
                row {:entry entry :depth depth :expanded? is-exp? :active? is-active?}
                children (when (and is-dir? is-exp?)
                           (when-let [child-entries (get cache (:path entry))]
                             (flatten-file-tree child-entries expanded-dirs current-file
                                                cache (inc depth))))]
            (if children
              (into [row] children)
              [row]))))
      sorted)))

(defn compute-sidebar-content-height
  "Total content height in px for sidebar scroll clamping."
  [sidebar-state current-file]
  (let [{:keys [project expanded-dirs dir-cache home-dirs]} sidebar-state]
    (if (nil? project)
      ;; Home dirs list
      (* (count (or home-dirs [])) sidebar-row-h)
      ;; File tree
      (let [root-entries (get dir-cache (:path project) [])
            flat (flatten-file-tree root-entries expanded-dirs current-file dir-cache 0)]
        (* (count flat) sidebar-row-h)))))

;; ============================================================================
;; SIDEBAR TREE BUILDER (rect tree for file sidebar)
;; ============================================================================

(defn build-sidebar-tree
  "Build the file sidebar scene graph. Pure function, same pattern as build-intake-tree.
   Returns a single rt-node tree. Walk with tree->rects for GPU rects, tree->text-ops for text.
   The sidebar root is pinned to the viewport via scroll-y offset."
  [sidebar-state current-file sidebar-visible?
   viewport-h scroll-y font-size char-advance]
  (when sidebar-visible?
    (let [{:keys [project expanded-dirs dir-cache home-dirs
                  hovered-id]} sidebar-state
          sidebar-scroll-y (or (:scroll-y sidebar-state) 0)
          sb-w sidebar-w
          sb-font font-size
          sb-char-advance (* sb-font 0.56)
          max-chars (max 8 (int (/ (- sb-w (* 2 sidebar-padding-x)) sb-char-advance)))
          colors (:colors dt)

          ;; Right border line (1px separator between sidebar and content)
          border-node (rt-node :sidebar-border :chrome
                        {:x (dec sb-w) :y 0 :w 1 :h viewport-h}
                        :style {:bg (:border colors)})

          ;; Content fills full height (no tab bar)
          content-top 0
          content-h viewport-h

          content-children
          (cond
            ;; No project selected — show home dirs
            (nil? project)
            (let [;; Header
                  explorer-label "EXPLORER"
                  ;; Overline typography for category labels
                  text-muted-c (or (:text-muted (:surfaces dt)) [0.36 0.42 0.50 1.0])
                  header-node (rt-node :explorer-hdr :header
                                {:x 0 :y 0 :w sb-w :h sidebar-back-h}
                                :style {:bg (:bg-elevated colors)
                                        :border-widths [0 0 1 0]
                                        :border-color (:border-subtle colors)}
                                :text [{:text explorer-label :type :comment
                                        :from 0 :to (count explorer-label)
                                        :x sidebar-padding-x :y 28
                                        :size 11
                                        :r (nth text-muted-c 0) :g (nth text-muted-c 1)
                                        :b (nth text-muted-c 2) :a (nth text-muted-c 3)}])
                  ;; Dir list items
                  dir-items
                  (if (nil? home-dirs)
                    ;; Loading state
                    [(rt-node :home-loading :text-block
                       {:x 0 :y 0 :w sb-w :h sidebar-row-h}
                       :text [{:text "Loading..." :type :comment
                               :from 0 :to 10
                               :x sidebar-padding-x :y 20
                               :size sb-font
                               :r 0.40 :g 0.40 :b 0.45 :a 0.6}])]
                    ;; Render each home dir
                    (mapv
                      (fn [i d]
                        (let [id-kw (keyword (str "home-" i))
                              name-str (str "▸ " (:name d))
                              hovered? (= id-kw hovered-id)
                              text-y (+ (/ sidebar-row-h 2) (/ sb-font 2.5))]
                          (rt-node id-kw :sidebar-entry
                            {:x 0 :y 0 :w sb-w :h sidebar-row-h}
                            :data {:entry-type :home-dir :entry d :idx i}
                            :style (when hovered?
                                     {:bg (:bg-hover colors)
                                      :radius (:sm (:radii dt))})
                            :children
                            (if hovered?
                              [(rt-node (keyword (str "home-" i "-hl")) :highlight
                                 {:x sidebar-item-inset :y 2
                                  :w (- sb-w (* 2 sidebar-item-inset)) :h (- sidebar-row-h 4)}
                                 :style {:bg (:bg-hover colors)
                                         :radius (:sm (:radii dt))})]
                              [])
                            :text [{:text name-str :type :text
                                    :from 0 :to (count name-str)
                                    :x sidebar-padding-x :y text-y
                                    :size sb-font
                                    :r 0.65 :g 0.65 :b 0.70 :a 1.0}])))
                      (range) home-dirs))]
              (into [header-node] dir-items))

            ;; Files mode, project selected — file tree
            :else
            (let [;; Back button
                  back-label (str "← " (:name project))
                  subtitle "Project Workspace"
                  back-node (rt-node :back-btn :sidebar-entry
                              {:x 0 :y 0 :w sb-w :h sidebar-back-h}
                              :data {:entry-type :back-btn}
                              :style {:bg (:bg-elevated colors)
                                      :border-widths [0 0 1 0]
                                      :border-color (:border-subtle colors)}
                              :text [{:text back-label :type :text
                                      :from 0 :to (count back-label)
                                      :x sidebar-padding-x :y 26
                                      :size (:size typo-subtitle)
                                      :r 0.90 :g 0.90 :b 0.92 :a (:a typo-subtitle)}
                                     {:text subtitle :type :comment
                                      :from 0 :to (count subtitle)
                                      :x (+ sidebar-padding-x 16) :y 40
                                      :size (:size typo-body)
                                      :r 0.55 :g 0.55 :b 0.60 :a 0.7}])
                  ;; Breadcrumb (when file is open)
                  breadcrumb-node
                  (when current-file
                    (let [fname (:name current-file)]
                      (rt-node :breadcrumb :text-block
                        {:x 0 :y 0 :w sb-w :h sidebar-breadcrumb-h}
                        :style {:bg [0.05 0.05 0.06 1.0]
                                :border-widths [0 0 1 0]
                                :border-color (:border-subtle colors)}
                        :text [{:text fname :type :comment
                                :from 0 :to (count fname)
                                :x sidebar-padding-x :y 17
                                :size (:size typo-body)
                                :r 0.55 :g 0.55 :b 0.60 :a 0.8}])))
                  ;; Flatten the file tree
                  root-entries (get dir-cache (:path project) [])
                  flat-rows (flatten-file-tree root-entries expanded-dirs current-file dir-cache 0)
                  ;; Build file entry nodes
                  file-nodes
                  (mapv
                    (fn [i {:keys [entry depth expanded? active?]}]
                      (let [is-dir? (= (:type entry) :dir)
                            id-kw (keyword (str "entry-" i))
                            hovered? (= id-kw hovered-id)
                            indent (* depth sidebar-indent-px)
                            chevron (cond
                                      (not is-dir?) "  "
                                      expanded?     "▾ "
                                      :else         "▸ ")
                            label (str chevron (:name entry))
                            avail-chars (max 5 (- max-chars (int (/ indent sb-char-advance))))
                            trunc (if (> (count label) avail-chars)
                                    (str (subs label 0 (- avail-chars 2)) "..")
                                    label)
                            fg-color (cond
                                       active?  [0.95 0.95 0.98 1.0]
                                       :else    (:fg colors))
                            ;; Highlight background
                            inner-bg (cond
                                       active?  (:bg-selected colors)
                                       hovered? (:bg-hover colors)
                                       :else    nil)
                            highlight (when inner-bg
                                        (rt-node (keyword (str "entry-" i "-hl")) :highlight
                                          {:x sidebar-item-inset :y 2
                                           :w (- sb-w (* 2 sidebar-item-inset)) :h (- sidebar-row-h 4)}
                                          :style {:bg inner-bg
                                                  :radius (:sm (:radii dt))}))
                            ;; Active accent bar (2px, inner-left edge)
                            accent-bar (when active?
                                         (rt-node (keyword (str "entry-" i "-acc")) :accent-bar
                                           {:x (+ sidebar-item-inset 1) :y 6
                                            :w 2 :h (- sidebar-row-h 12)}
                                           :style {:bg (:accent colors)
                                                   :radius (:sm (:radii dt))}))
                            ;; Indent guide lines (1px vertical per depth level)
                            guide-color (:border colors)
                            indent-guides
                            (when (pos? depth)
                              (mapv (fn [d]
                                      (let [gx (+ sidebar-padding-x (* d sidebar-indent-px) (/ sidebar-indent-px 2))]
                                        (rt-node (keyword (str "entry-" i "-g" d)) :indent-guide
                                          {:x gx :y 0 :w 1 :h sidebar-row-h}
                                          :style {:bg [(nth guide-color 0) (nth guide-color 1)
                                                       (nth guide-color 2) 0.10]})))
                                    (range depth)))
                            text-y (+ (/ sidebar-row-h 2) (/ sb-font 2.5))]
                        (rt-node id-kw :sidebar-entry
                          {:x 0 :y 0 :w sb-w :h sidebar-row-h}
                          :data {:entry-type (if is-dir? :dir :file) :entry entry :idx i
                                 :expanded? expanded? :active? active? :depth depth}
                          :children (cond-> []
                                      highlight     (conj highlight)
                                      accent-bar    (conj accent-bar)
                                      indent-guides (into indent-guides))
                          :text [{:text trunc :type :text
                                  :from 0 :to (count trunc)
                                  :x (+ sidebar-padding-x indent) :y text-y
                                  :size sb-font
                                  :r (nth fg-color 0) :g (nth fg-color 1)
                                  :b (nth fg-color 2) :a (nth fg-color 3)}])))
                    (range) flat-rows)]
              (cond-> [back-node]
                breadcrumb-node (conj breadcrumb-node)
                true (into file-nodes))))

          ;; Scrollable content area (clips children)
          ;; Inner scroll container: offset by -scroll-y, layout positions children,
          ;; outer clip-node hides overflow
          scroll-inner (rt-node :sidebar-scroll-inner :container
                         {:x 0 :y (- sidebar-scroll-y) :w sb-w :h 99999}
                         :layout {:direction :column}
                         :children (vec content-children))
          content-node (rt-node :sidebar-content :panel-content
                         {:x 0 :y content-top :w sb-w :h content-h}
                         :clip? true
                         :children [scroll-inner])]

      ;; Root node: pinned to viewport via scroll-y
      (rt-node :sidebar-root :panel
        {:x 0 :y scroll-y :w sb-w :h viewport-h}
        :style {:bg (or (:base (:surfaces dt)) (:bg colors))}
        :children [border-node content-node]))))

