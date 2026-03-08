(ns app.client.workspace.combined-text
  "Combined text ops: joins editor, sidebar, cmd-panel, agent trail text for GPU."
  (:require [clojure.string :as str]
            [missionary.core :as m]
            [app.client.workspace.events :refer [maybe-snap]]
            [app.client.workspace.rect-tree :refer [rt-node wrap-line tree->rects tree->text-ops tree->shadows resolve-layout]]
            [app.client.workspace.ui-primitives :as ui :refer [dt]]
            [app.client.workspace.sidebar :as sidebar :refer [sidebar-w build-sidebar-tree]]
            [app.client.workspace.trail :as trail :refer [trail->display-lines trail->chat-nodes agent-wrapped-line-count compute-agent-panel-h]]
            [app.client.workspace.shell :refer [build-file-layout]]
            [app.client.workspace.cmd-panel :refer [cmd-prompt-text cmd-text-start-x]]
            [app.client.workspace.themes :as themes]))

(defn <combined-text-ops
  "Derived flow: combined text render ops (editor + command panel + status bar)
   Uses m/latest instead of m/ap to avoid cancellation propagation.
   REACTIVE: font-size comes from !settings, updates live.
   MODE-SWITCH: when flow canvas is active, returns ticket card text ops instead.
   SIDEBAR: when sidebar visible, sidebar text ops prepended, content offset right."
  [!editor-doc !cmd-panel !ai-provider !agent-output !agent-scroll-y !scroll-y !viewport !settings !active-font
   !current-file
   tokenize-fn layout-fn
   <fold-data
   !flow-state !collapsed-groups !hovered-row-idx !drag-state
   !sidebar-state !sidebar-visible !extract-preview
   !shimmer-phase !trail-collapsed !active-pane !scroll-x !chat-scroll-y !chat-input !focus
   flow-canvas-active?* compute-ticket-list-text-ops* offset-text-ops*
   layout-x layout-y cmd-panel-h status-bar-h]
  (m/latest
    (fn [doc panel provider agent-output agent-scroll-y scroll-y viewport fold-state settings active-font
         current-file flow-state collapsed-groups hovered-row-idx drag-state
         sidebar-state sidebar-visible? extract-preview
         shimmer-phase trail-collapsed active-pane scroll-x chat-scroll-y chat-input focus]
      (let [sb-vis? (boolean sidebar-visible?)
            sb-w (if sb-vis? sidebar-w 0)
            dpr (:dpr viewport)
              snap? (:snap-to-pixel? settings)
              ;; Reactive font settings
              font-size (:font-size settings)
              char-width (:char-width active-font)
              char-advance (maybe-snap (* font-size char-width) dpr snap?)
              line-h (maybe-snap (* font-size (:line-height settings)) dpr snap?)
              layout-x (maybe-snap layout-x dpr snap?)
              ;; Scrolled x for editor text only — gutter/line-nums stay fixed
              editor-lx (maybe-snap (- layout-x (or scroll-x 0)) dpr snap?)
              layout-y (maybe-snap layout-y dpr snap?)
              ;; Theme
              theme-id (or (:theme-id settings) :gruvbox-dark)
              ;; Content viewport width (minus sidebar)
              content-vw (- (:width viewport) sb-w)

              ;; Sidebar text ops
              sidebar-text-ops
              (when sb-vis?
                (let [tree (resolve-layout
                             (build-sidebar-tree sidebar-state current-file true
                                                 (:height viewport) scroll-y font-size char-advance))]
                  (when tree (tree->text-ops tree))))

              ;; MODE-SWITCH: file open (3-pane), flow canvas, or plain editor
              file-open? (some? current-file)
              [editor-ops final-line-mapping line-num-ops]
              (if (:rt-node extract-preview)
                ;; Extract preview on right half
                (let [half-w (/ content-vw 2)
                      preview-tree (when-let [rt (:rt-node extract-preview)]
                                     (resolve-layout
                                       (rt-node :extract-preview-root :rect
                                                {:x half-w :y 0 :w half-w :h (:height viewport)}
                                                :layout {:direction :column :padding [16 16 16 16] :gap 8}
                                                :children [(rt-node :extract-label :text
                                                                    {:x 0 :y 0 :w (- half-w 32) :h 24}
                                                                    :text [{:text "Compiled Preview" :type :keyword
                                                                            :from 0 :to 16 :x 0 :y 16
                                                                            :size 14 :r 0.55 :g 0.55 :b 0.60 :a 1.0}])
                                                           (assoc-in rt [:bounds :w] (- half-w 32))])))]
                  [(if preview-tree (tree->text-ops preview-tree) [])
                   (vec (range (count (:lines doc))))
                   []])
                (if (flow-canvas-active?* flow-state)
                  ;; Flow canvas mode: master-detail list view text
                  [(compute-ticket-list-text-ops* flow-state content-vw (:height viewport)
                                                  font-size char-advance scroll-y
                                                  hovered-row-idx collapsed-groups drag-state)
                   (vec (range (count (:lines doc))))
                   []]

                ;; Normal editor mode — original logic below
                (let [;; Pre-computed fold state from <fold-data
                      folded (or (:folded fold-state) #{})
                      regions (or (:regions fold-state) [])
                      lines (:lines doc)
                      total-line-count (count lines)
                      large-file? (and (> total-line-count 500) (empty? folded))
                      visible-start (max 0 (- (int (/ scroll-y line-h)) 5))
                      visible-end (min total-line-count (+ (int (/ (+ scroll-y (:height viewport)) line-h)) 5))
                      visible-lines (subvec lines visible-start visible-end)
                      tokenized-visible (mapv tokenize-fn visible-lines)
                      cursor-line (:line (:cursor doc))]
                  (if large-file?
                    ;; FAST PATH: skip fold detection entirely
                    (let [adjusted-y (+ layout-y (* visible-start line-h))
                          result (layout-fn tokenized-visible editor-lx adjusted-y font-size
                                            [] #{} char-advance line-h theme-id)
                          full-mapping (vec (range total-line-count))
                          nums (mapv (fn [i]
                                       (let [logical (+ visible-start i)
                                             num-str (str (inc logical))
                                             num-w (* (count num-str) char-advance)
                                             x (maybe-snap (- layout-x 8 num-w) dpr snap?)
                                             y (+ adjusted-y font-size (* i line-h))
                                             current? (= logical cursor-line)]
                                         [{:text num-str :type :line-number
                                           :from 0 :to (count num-str)
                                           :x x :y y :size font-size
                                           :r (if current? 0.85 0.45)
                                           :g (if current? 0.85 0.45)
                                           :b (if current? 0.85 0.45)
                                           :a (if current? 0.9 0.4)}]))
                                     (range (count visible-lines)))]
                      [(:render-ops result) full-mapping nums])

                    ;; NORMAL PATH (<500 lines or folds active): full fold support
                    (do (when (seq folded)
                          (js/console.log "[TEXT-OPS] folded:" (clj->js folded)
                                          "total-lines:" total-line-count
                                          "regions:" (count regions)))
                    (let [tokenized-all (into []
                                          (map-indexed
                                            (fn [idx _]
                                              (if (and (>= idx visible-start) (< idx visible-end))
                                                (nth tokenized-visible (- idx visible-start))
                                                [])))
                                          lines)
                          result (layout-fn tokenized-all editor-lx layout-y font-size
                                            regions folded char-advance line-h theme-id)
                          _ (when (seq folded)
                              (js/console.log "[TEXT-OPS] render-ops:" (count (:render-ops result))
                                              "mapping:" (count (:line-mapping result))
                                              "regions:" (count regions)))
                          mapping (:line-mapping result)
                          nums (mapv (fn [visual-idx]
                                       (let [logical (get mapping visual-idx visual-idx)
                                             num-str (str (inc logical))
                                             num-w (* (count num-str) char-advance)
                                             x (maybe-snap (- layout-x 8 num-w) dpr snap?)
                                             y (+ layout-y font-size (* visual-idx line-h))
                                             current? (= logical cursor-line)]
                                         [{:text num-str :type :line-number
                                           :from 0 :to (count num-str)
                                           :x x :y y :size font-size
                                           :r (if current? 0.85 0.45)
                                           :g (if current? 0.85 0.45)
                                           :b (if current? 0.85 0.45)
                                           :a (if current? 0.9 0.4)}]))
                                     (range (count mapping)))]
                      [(filterv seq (:render-ops result)) mapping nums]))))))

              ;; When file is open, clip editor text to left 40% and add right-tree text ops
              [editor-ops final-line-mapping line-num-ops]
              (if (and file-open? (not (flow-canvas-active?* flow-state)) (not (:rt-node extract-preview)))
                (let [code-w (int (* content-vw 0.4))
                      ;; Clip text ops to editor pane [layout-x, code-w] — left (gutter edge) AND right
                      font-cw (:char-width active-font 0.56)
                      clip-left layout-x ;; left boundary = gutter right edge (text start)
                      header-h 36
                      clip-top (+ scroll-y header-h) ;; viewport-pinned top edge below header
                      clip-sub (fn [sub]
                                 (let [x (or (:x sub) 0)
                                       y (or (:y sub) 0)
                                       fs (or (:size sub) font-size)
                                       cw (* fs font-cw)
                                       txt (or (:text sub) "")
                                       text-end (+ x (* (count txt) cw))]
                                   ;; Drop if outside horizontal [clip-left, code-w] or above header
                                   (when (and (< x code-w) (> text-end clip-left) (>= y clip-top))
                                     ;; Left-trim chars before gutter edge
                                     (let [skip (if (< x clip-left) (min (count txt) (int (Math/ceil (/ (- clip-left x) cw)))) 0)
                                           adj-x (+ x (* skip cw))
                                           adj-txt (if (pos? skip) (subs txt skip) txt)
                                           ;; Right-truncate at code-w
                                           max-chars (if (pos? cw)
                                                       (max 0 (int (/ (- code-w adj-x) cw)))
                                                       1000)
                                           final-txt (if (> (count adj-txt) max-chars)
                                                       (subs adj-txt 0 max-chars)
                                                       adj-txt)]
                                       (when (seq final-txt)
                                         (assoc sub :text final-txt :x adj-x
                                                :from skip :to (+ skip (count final-txt))))))))
                      clip-op (fn [op]
                                (if (vector? op)
                                  (let [clipped (into [] (keep clip-sub) op)]
                                    (when (seq clipped) clipped))
                                  (clip-sub op)))
                      clipped (into [] (keep clip-op) editor-ops)
                      shimmer-alpha (if shimmer-phase 0.9 0.4)
                      file-layout-h (- (:height viewport) cmd-panel-h status-bar-h)
                      right-tree (resolve-layout
                                   (build-file-layout content-vw file-layout-h
                                                      current-file agent-output font-size
                                                      shimmer-alpha trail-collapsed
                                                      :active-pane active-pane :char-advance char-advance
                                                      :chat-scroll-y (or chat-scroll-y 0)
                                                      :chat-input chat-input :focus focus))
                      right-text-ops (tree->text-ops right-tree)
                      ;; Pin to viewport: offset by scroll-y so camera pan doesn't move it
                      pinned-ops (mapv (fn [op]
                                         (if (vector? op)
                                           (mapv #(update % :y + scroll-y) op)
                                           (update op :y + scroll-y)))
                                       right-text-ops)]
                  (let [clip-ln (fn [op]
                                  (if (vector? op)
                                    (let [f (filterv #(>= (or (:y %) 0) clip-top) op)]
                                      (when (seq f) f))
                                    (when (>= (or (:y op) 0) clip-top) op)))
                        clipped-ln (into [] (keep clip-ln) line-num-ops)]
                    [(into (vec clipped) pinned-ops) final-line-mapping clipped-ln]))
                [editor-ops final-line-mapping line-num-ops])

              ;; Bottom clip: filter out text ops that would render inside cmd panel / status bar
              bottom-clip-y (+ scroll-y (- (:height viewport) cmd-panel-h status-bar-h))
              clip-bottom (fn [ops]
                            (into []
                              (keep (fn [op]
                                      (if (vector? op)
                                        (let [clipped (filterv #(< (or (:y %) 0) bottom-clip-y) op)]
                                          (when (seq clipped) clipped))
                                        (when (< (or (:y op) 0) bottom-clip-y) op))))
                              ops))

              ;; Offset editor/flow text ops by sidebar width
              offset-editor-ops (offset-text-ops* (clip-bottom editor-ops) sb-w)
              offset-line-num-ops (offset-text-ops* (clip-bottom line-num-ops) sb-w)

              ;; Command panel ops (if visible or file open) — offset by sb-w
              panel-visible? (or (:visible panel) file-open? (flow-canvas-active?* flow-state))
              cmd-ops (when panel-visible?
                        (let [cmd-panel-y (maybe-snap (+ scroll-y (- (:height viewport) cmd-panel-h status-bar-h)) dpr snap?)
                              cmd-text-y (maybe-snap (+ cmd-panel-y 12 font-size) dpr snap?)
                              prompt-text (cmd-prompt-text provider)
                              prompt-x (maybe-snap (+ 24 sb-w) dpr snap?)
                              text-x (+ (cmd-text-start-x provider font-size (:char-width active-font) dpr snap?) sb-w)]
                          [(when (seq (:text panel))
                             [{:text (:text panel)
                               :type :text
                               :from 0 :to (count (:text panel))
                               :x text-x :y cmd-text-y
                               :size font-size
                               :r 0.9 :g 0.9 :b 0.9 :a 1.0}])
                           [{:text prompt-text
                             :type :macro
                             :from 0 :to (count prompt-text)
                             :x prompt-x :y cmd-text-y
                             :size font-size
                             :r 0.3 :g 0.6 :b 1.0 :a 1.0}]
                           (when (empty? (:text panel))
                             [{:text "Ask AI..."
                               :type :comment
                               :from 0 :to 10
                               :x text-x :y cmd-text-y
                               :size font-size
                               :r 0.5 :g 0.5 :b 0.5 :a 0.7}])]))
              cmd-lines (if panel-visible? (vec (filter some? cmd-ops)) [])]

          (let [status (:status agent-output)
                provider-name (some-> (:provider agent-output) name str/upper-case)
                prompt (:prompt agent-output)
                result-output (or (:output agent-output) "")
                status-color (case status
                               :complete {:r 0.55 :g 0.9 :b 0.55 :a 1.0}
                               :failed {:r 0.95 :g 0.45 :b 0.45 :a 1.0}
                               :timeout {:r 0.95 :g 0.75 :b 0.35 :a 1.0}
                               :running {:r 0.6 :g 0.8 :b 1.0 :a 1.0}
                               :submitting {:r 0.6 :g 0.8 :b 1.0 :a 1.0}
                               {:r 0.75 :g 0.75 :b 0.75 :a 1.0})
                header-text (cond
                              (nil? status) nil
                              (= status :running) (str "[" provider-name "] running: " prompt)
                              (= status :submitting) (str "[" provider-name "] submitting: " prompt)
                              (= status :failed) (str "[" provider-name "] failed: " prompt)
                              (= status :timeout) (str "[" provider-name "] timeout: " prompt)
                              (= status :complete) (str "[" provider-name "] complete: " prompt)
                              :else (str "[" provider-name "] " (name status) ": " prompt))
                output-lines (str/split-lines result-output)
                agent-x-px (+ 24 sb-w)
                right-pad 24
                available-w (- (:width viewport) agent-x-px right-pad)
                max-chars (if (pos? char-advance) (max 1 (int (/ available-w char-advance))) 80)

                ;; Trail-aware line generation: use trail if available, fall back to flat text
                trail (:trail agent-output)
                display-entries (if (seq trail)
                                  (trail->display-lines trail)
                                  nil)
                raw-lines (if display-entries
                            ;; Trail path: header + trail display lines (each carries its own color)
                            (cond-> []
                              header-text (conj {:text header-text :color status-color})
                              (and (= status :running) (empty? display-entries)) (conj {:text "..." :color status-color})
                              (seq display-entries) (into display-entries))
                            ;; Flat text fallback (backward compat)
                            (let [flat-lines (cond-> []
                                              header-text (conj header-text)
                                              (and (= status :running) (empty? output-lines)) (conj "...")
                                              (seq output-lines) (into output-lines))]
                              (mapv (fn [l] {:text l :color status-color}) flat-lines)))

                ;; Wrap all lines (both trail and flat share this path)
                ;; Split by newlines FIRST, then wrap — prevents \n inside text ops
                ;; which causes shape-text to bump Y and overlap with the next text op
                all-lines (into []
                            (mapcat (fn [entry]
                              (let [nl-lines (str/split-lines (or (:text entry) ""))
                                    wrapped (mapcat #(wrap-line % max-chars) nl-lines)]
                                (mapv (fn [wl] {:text wl :color (:color entry)}) wrapped))))
                            raw-lines)

                agent-panel-h (if file-open?
                                0 ;; Chat pane shows trail; suppress bottom panel
                                (compute-agent-panel-h agent-output font-size (:height viewport)
                                                       (:width viewport) char-advance))
                agent-x (maybe-snap (+ 24 sb-w) dpr snap?)
                agent-y0 (maybe-snap (+ scroll-y (- (:height viewport) cmd-panel-h status-bar-h agent-panel-h 12)) dpr snap?)
                line-step (maybe-snap (* font-size 1.2) dpr snap?)
                panel-top agent-y0
                panel-bottom (+ agent-y0 agent-panel-h)
                agent-lines (into []
                              (comp
                                (map (fn [idx]
                                       (let [entry (nth all-lines idx)
                                             y (+ agent-y0 8 font-size
                                                  (* idx line-step)
                                                  (- agent-scroll-y))
                                             c (:color entry)]
                                         (when (and (>= y (+ panel-top 8))
                                                    (< y panel-bottom))
                                           [{:text (:text entry)
                                             :type :comment
                                             :from 0 :to (count (:text entry))
                                             :x agent-x
                                             :y y
                                             :size font-size
                                             :r (:r c)
                                             :g (:g c)
                                             :b (:b c)
                                             :a (:a c)}]))))
                                (filter some?))
                              (range (count all-lines)))

                ;; Status bar text (always visible, pinned to bottom) — offset by sb-w
                status-y (maybe-snap (+ scroll-y (- (:height viewport) status-bar-h) 4 font-size) dpr snap?)
                ;; Flow canvas mode: show flow info instead of cursor position
                status-left-text (if (flow-canvas-active?* flow-state)
                                   (let [node-name (some-> (:node flow-state) name str/upper-case)
                                         n-tickets (count (:tickets flow-state))
                                         n-selected (count (:selected flow-state))]
                                     (str node-name " | " n-tickets " tickets | " n-selected " selected"))
                                   (let [sb-cursor (:cursor doc)]
                                     (str "Ln " (inc (:line sb-cursor)) ", Col " (inc (:col sb-cursor)))))
                file-name (or (:name current-file) "untitled")
                provider-upper (some-> provider name str/upper-case)
                status-right-text (if provider-upper
                                    (str file-name "  |  " provider-upper)
                                    file-name)
                status-right-w (* (count status-right-text) char-advance)
                status-right-x (maybe-snap (- (:width viewport) status-right-w 16) dpr snap?)
                status-lines [;; Left: cursor position
                              [{:text status-left-text
                                :type :comment
                                :from 0 :to (count status-left-text)
                                :x (maybe-snap (+ 16 sb-w) dpr snap?) :y status-y
                                :size font-size
                                :r 0.65 :g 0.65 :b 0.65 :a 0.9}]
                              ;; Right: filename | PROVIDER
                              [{:text status-right-text
                                :type :comment
                                :from 0 :to (count status-right-text)
                                :x status-right-x :y status-y
                                :size font-size
                                :r 0.65 :g 0.65 :b 0.65 :a 0.9}]]]

            {:render-ops (vec (concat (or sidebar-text-ops [])
                                      offset-line-num-ops offset-editor-ops
                                      cmd-lines
                                      ;; Suppress bottom agent output when 3-pane chat shows trail
                                      (when-not file-open? agent-lines)
                                      status-lines))
             :line-mapping final-line-mapping
             :editor-line-count (+ (count line-num-ops) (count editor-ops))
             :cmd-line-count (+ (count cmd-lines) (count status-lines))})))
    (m/watch !editor-doc)
    (m/watch !cmd-panel)
    (m/watch !ai-provider)
    (m/watch !agent-output)
    (m/watch !agent-scroll-y)
    (m/watch !scroll-y)
    (m/watch !viewport)
    <fold-data  ;; pre-computed fold state (regions + folded set), replaces (m/watch !folded-lines)
    (m/watch !settings)
    (m/watch !active-font)
    (m/watch !current-file)
    (m/watch !flow-state)
    (m/watch !collapsed-groups)
    (m/watch !hovered-row-idx)
    (m/watch !drag-state)
    (m/watch !sidebar-state)
    (m/watch !sidebar-visible)
    (m/watch !extract-preview)
    (m/watch !shimmer-phase)
    (m/watch !trail-collapsed)
    (m/watch !active-pane)
    (m/watch !scroll-x)
    (m/watch !chat-scroll-y)
    (m/watch !chat-input)
    (m/watch !focus)))
