(ns app.client.workspace.combined-text
  "Combined text ops: joins editor, sidebar, cmd-panel, agent trail text for GPU."
  (:require [clojure.string :as str]
            [missionary.core :as m]
            [app.client.workspace.events :refer [maybe-snap]]
            [app.client.workspace.rect-tree :refer [rt-node wrap-line tree->rects tree->text-ops tree->shadows resolve-layout]]
            [app.client.workspace.runtime.workspace-actions :as ws]
            [app.client.workspace.ui-primitives :as ui :refer [dt]]
            [app.client.workspace.sidebar :as sidebar :refer [sidebar-w]]
            [app.client.workspace.trail :as trail :refer [trail->display-lines trail->chat-nodes agent-wrapped-line-count compute-agent-panel-h]]
            [app.client.workspace.shell :refer [build-file-layout]]
            [app.client.workspace.cmd-panel :refer [cmd-prompt-text cmd-text-start-x]]
            [app.client.workspace.themes :as themes]))

(defn <combined-text-ops
  "Derived flow: combined text render ops (editor + command panel + status bar).
   Split into scoped sub-flows: flow-canvas text isolated from editor text,
   so hover/drag/collapse changes don't recompute editor tokenization and vice versa."
  [!editor-doc !cmd-panel !ai-provider !agent-output !agent-scroll-y !scroll-y !viewport !settings !active-font
   !current-file !effective-local-world
   tokenize-fn layout-fn
   <fold-data
   !flow-state !collapsed-groups !hovered-row-idx !drag-state
   !sidebar-visible !sidebar-scene !trail-face-scene !face-scene !extract-preview
   !shimmer-phase !trail-collapsed !active-pane !scroll-x !chat-scroll-y !chat-input !focus !run-scroll-y !detail-scroll-y
   compute-ticket-list-text-ops* compute-run-text-ops* offset-text-ops*
   layout-x layout-y cmd-panel-h status-bar-h]
  (let [;; ── Shared layout context ──
        <layout
        (m/latest
          (fn [viewport settings active-font sidebar-visible? local-world]
            (let [dpr (:dpr viewport)
                  snap? (:snap-to-pixel? settings)
                  font-size (:font-size settings)
                  char-width (:char-width active-font)
                  char-advance (maybe-snap (* font-size char-width) dpr snap?)
                  ;; trail-room R-2 / W-1 (R-1 debt 1): the LAYOUT consumes
                  ;; the derived world's sidebar judgment, never the raw
                  ;; atom - in a trail face NOTHING else is ambient, so
                  ;; sb-w is 0 there (matches editor_compute's <layout>;
                  ;; the raw atom stays untouched, so leaving the face
                  ;; restores the sidebar exactly as it was).
                  ;; the assembly-hosted face inherits the same ground rule
                  trail-face? (or (ws/local-world-trail-face? local-world)
                                  (ws/local-world-face-assembly? local-world))
                  sb-vis? (boolean (and sidebar-visible? (not trail-face?)))]
              {:viewport viewport :settings settings :dpr dpr :snap? snap?
               :font-size font-size :char-width char-width :char-advance char-advance
               :line-h (maybe-snap (* font-size (:line-height settings)) dpr snap?)
               :sb-vis? sb-vis? :sb-w (if sb-vis? sidebar-w 0)}))
          (m/watch !viewport) (m/watch !settings) (m/watch !active-font) (m/watch !sidebar-visible)
          (m/watch !effective-local-world))
        ;; 5 fn args, 5 flows

        ;; ── Flow canvas text ops (intake + run) ──
        ;; ── Intake text ops (ticket list) ──
        ;; NOT watching: !shimmer-phase, !agent-output, !trail-collapsed, !run-scroll-y
        <intake-text
        (m/latest
          (fn [layout local-world flow-state scroll-y detail-scroll-y
               hovered-row-idx collapsed-groups drag-state]
            (if-not (ws/local-world-intake? local-world)
              []
              (let [{:keys [viewport font-size char-advance sb-w]} layout
                    content-vw (- (:width viewport) sb-w)]
                (compute-ticket-list-text-ops* flow-state content-vw (:height viewport)
                                                font-size char-advance scroll-y
                                                detail-scroll-y
                                                hovered-row-idx collapsed-groups drag-state))))
          <layout
          (m/watch !effective-local-world) (m/watch !flow-state) (m/watch !scroll-y) (m/watch !detail-scroll-y)
          (m/watch !hovered-row-idx) (m/watch !collapsed-groups) (m/watch !drag-state))
        ;; 8 fn args, 8 flows

        ;; ── Run text ops (agent execution view) ──
        ;; NOT watching: !hovered-row-idx, !collapsed-groups, !drag-state, !detail-scroll-y
        <run-text
        (m/latest
          (fn [layout local-world flow-state scroll-y agent-output shimmer-phase
               trail-collapsed run-scroll-y]
            (if-not (ws/local-world-run? local-world)
              []
              (let [{:keys [viewport font-size char-advance sb-w]} layout
                    content-vw (- (:width viewport) sb-w)]
                (compute-run-text-ops* flow-state content-vw (:height viewport)
                                        scroll-y agent-output font-size char-advance
                                        shimmer-phase trail-collapsed run-scroll-y))))
          <layout
          (m/watch !effective-local-world) (m/watch !flow-state) (m/watch !scroll-y)
          (m/watch !agent-output) (m/watch !shimmer-phase)
          (m/watch !trail-collapsed) (m/watch !run-scroll-y))
        ;; 8 fn args, 8 flows

        ;; ── Chrome text (cmd panel + agent status + status bar) ──
        ;; Separate flow: only fires when chrome-relevant inputs change.
        ;; Does NOT watch: fold-data, sidebar-scene, extract-preview, scroll-x, etc.
        <chrome-text
        (m/latest
          (fn [layout panel provider agent-output agent-scroll-y scroll-y
               doc current-file local-world flow-state trail-face-scene]
            (let [{:keys [viewport dpr snap? font-size char-width char-advance sb-w]} layout
                  file-workspace? (ws/local-world-file-workspace? local-world)
                  flow-mode? (ws/local-world-flow? local-world)
                  ;; trail-room item 3: in a trail face the status strip BECOMES
                  ;; the rim (scope · delta · address · palette). The four slots
                  ;; are pre-computed in the CACHED scene (scene/rim-slots, in
                  ;; :data) so this glue only positions + colors them.
                  trail-mode? (ws/local-world-trail-face? local-world)
                  rim-slots (when trail-mode?
                              (get-in trail-face-scene [:data :trail-face/rim-slots]))
                  panel-visible? (or (:visible panel) file-workspace? flow-mode?)
                  cmd-ops (when panel-visible?
                            (let [cmd-panel-y (maybe-snap (+ scroll-y (- (:height viewport) cmd-panel-h status-bar-h)) dpr snap?)
                                  cmd-text-y (maybe-snap (+ cmd-panel-y 12 font-size) dpr snap?)
                                  prompt-text (cmd-prompt-text provider)
                                  prompt-x (maybe-snap (+ 24 sb-w) dpr snap?)
                                  text-x (+ (cmd-text-start-x provider font-size char-width dpr snap?) sb-w)]
                              [(when (seq (:text panel))
                                 [{:text (:text panel) :type :text :from 0 :to (count (:text panel))
                                   :x text-x :y cmd-text-y :size font-size
                                   :r 0.9 :g 0.9 :b 0.9 :a 1.0}])
                               [{:text prompt-text :type :macro :from 0 :to (count prompt-text)
                                 :x prompt-x :y cmd-text-y :size font-size
                                 :r 0.3 :g 0.6 :b 1.0 :a 1.0}]
                               (when (empty? (:text panel))
                                 [{:text "Ask AI..." :type :comment :from 0 :to 10
                                   :x text-x :y cmd-text-y :size font-size
                                   :r 0.5 :g 0.5 :b 0.5 :a 0.7}])]))
                  cmd-lines (if panel-visible? (vec (filter some? cmd-ops)) [])
                  status (:status agent-output)
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
                  available-w (- (:width viewport) agent-x-px 24)
                  max-chars (if (pos? char-advance) (max 1 (int (/ available-w char-advance))) 80)
                  trail (:trail agent-output)
                  display-entries (if (seq trail) (trail->display-lines trail) nil)
                  raw-lines (if display-entries
                              (cond-> []
                                header-text (conj {:text header-text :color status-color})
                                (and (= status :running) (empty? display-entries)) (conj {:text "..." :color status-color})
                                (seq display-entries) (into display-entries))
                              (let [flat-lines (cond-> []
                                                header-text (conj header-text)
                                                (and (= status :running) (empty? output-lines)) (conj "...")
                                                (seq output-lines) (into output-lines))]
                                (mapv (fn [l] {:text l :color status-color}) flat-lines)))
                  all-lines (into []
                              (mapcat (fn [entry]
                                (let [nl-lines (str/split-lines (or (:text entry) ""))
                                      wrapped (mapcat #(wrap-line % max-chars) nl-lines)]
                                  (mapv (fn [wl] {:text wl :color (:color entry)}) wrapped))))
                              raw-lines)
                  agent-panel-h (if file-workspace? 0
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
                                               y (+ agent-y0 8 font-size (* idx line-step) (- agent-scroll-y))
                                               c (:color entry)]
                                           (when (and (>= y (+ panel-top 8)) (< y panel-bottom))
                                             [{:text (:text entry) :type :comment
                                               :from 0 :to (count (:text entry))
                                               :x agent-x :y y :size font-size
                                               :r (:r c) :g (:g c) :b (:b c) :a (:a c)}]))))
                                  (filter some?))
                                (range (count all-lines)))
                  status-y (maybe-snap (+ scroll-y (- (:height viewport) status-bar-h) 4 font-size) dpr snap?)
                  status-left-text (if flow-mode?
                                     (let [node-name (some-> (:node flow-state) name str/upper-case)
                                           n-tickets (count (:tickets flow-state))
                                           n-selected (count (:selected flow-state))]
                                       (str node-name " | " n-tickets " tickets | " n-selected " selected"))
                                     (let [sb-cursor (:cursor doc)]
                                       (str "Ln " (inc (:line sb-cursor)) ", Col " (inc (:col sb-cursor)))))
                  file-name (or (:name current-file)
                                (get-in local-world [:selected-artifact :name])
                                "untitled")
                  provider-upper (some-> provider name str/upper-case)
                  status-right-text (if provider-upper (str file-name "  |  " provider-upper) file-name)
                  status-right-w (* (count status-right-text) char-advance)
                  status-right-x (maybe-snap (- (:width viewport) status-right-w 16) dpr snap?)]
              (vec (concat cmd-lines
                           ;; no agent overlay over the assembly-hosted face
                           ;; (G16 falsification fix)
                           (when-not (or file-workspace?
                                         (ws/local-world-face-assembly? local-world))
                             agent-lines)
                           (if (and trail-mode? (seq rim-slots))
                             ;; the RIM: four slots left-to-right in kraft-neutral
                             ;; chrome ink (item 3 / G2). Editor mode never reaches
                             ;; here (trail-mode? false -> the else branch below).
                             (first
                              (reduce
                               (fn [[ops x] slot]
                                 (let [t (:text slot)]
                                   [(conj ops {:text t :type :comment
                                               :from 0 :to (count t)
                                               :x (maybe-snap x dpr snap?) :y status-y
                                               :size font-size
                                               :r 0.72 :g 0.66 :b 0.52 :a 0.95})
                                    (+ x (* (count t) char-advance) (* 2 char-advance))]))
                               [[] (+ 16 sb-w)]
                               rim-slots))
                             ;; editor / flow: the existing strip, UNCHANGED
                             (concat
                              [{:text status-left-text :type :comment
                                :from 0 :to (count status-left-text)
                                :x (maybe-snap (+ 16 sb-w) dpr snap?) :y status-y :size font-size
                                :r 0.65 :g 0.65 :b 0.65 :a 0.9}]
                              [{:text status-right-text :type :comment
                                :from 0 :to (count status-right-text)
                                :x status-right-x :y status-y :size font-size
                                :r 0.65 :g 0.65 :b 0.65 :a 0.9}]))))))
          <layout
          (m/watch !cmd-panel) (m/watch !ai-provider)
          (m/watch !agent-output) (m/watch !agent-scroll-y) (m/watch !scroll-y)
          (m/watch !editor-doc) (m/watch !current-file)
          (m/watch !effective-local-world) (m/watch !flow-state)
          (m/watch !trail-face-scene))
        ;; 11 fn args, 11 flows
        ]

    ;; ── Content text (editor + sidebar — the bulk) ──
    ;; Separate flow: does NOT watch cmd-panel, ai-provider, agent-scroll-y, flow-state.
    ;; Cursor-only moves don't trigger content re-upload.
    (let [<content-text
    (m/latest
      (fn [intake-text run-text layout
           lines fold-state scroll-y
           current-file local-world sidebar-scene trail-face-scene face-scene extract-preview agent-output
           shimmer-phase trail-collapsed active-pane scroll-x chat-scroll-y chat-input focus]
        (let [{:keys [viewport settings dpr snap? font-size char-width char-advance line-h
                       sb-vis? sb-w]} layout
              layout-x (maybe-snap layout-x dpr snap?)
              editor-lx (maybe-snap (- layout-x (or scroll-x 0)) dpr snap?)
              layout-y (maybe-snap layout-y dpr snap?)
              theme-id (or (:theme-id settings) :gruvbox-dark)
              content-vw (- (:width viewport) sb-w)
              mode (ws/local-world-mode local-world)
              file-workspace? (ws/local-world-file-workspace? local-world)
              flow-mode? (ws/local-world-flow? local-world)
              trail-mode? (ws/local-world-trail-face? local-world)
              face-mode? (ws/local-world-face-assembly? local-world)

              ;; Sidebar text ops — reads from shared scene (cached by render flow)
              sidebar-text-ops
              (when sb-vis?
                (when-let [tree sidebar-scene]
                  (tree->text-ops tree)))

              ;; MODE-SWITCH: use pre-computed flow text, or compute editor text
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
                   (vec (range (count lines)))
                   []])
                (if (or trail-mode? face-mode?)
                  ;; Trail face (WP-B2) / assembly-hosted face (framework
                  ;; W1-INT): flatten from the CACHED scene, threaded in as
                  ;; a flow like sidebar-scene - never a non-reactive deref
                  ;; here (gate 13 / advisory A3; trap T9 - mouse hit-tests
                  ;; the SAME object)
                  [(let [scene (if face-mode? face-scene trail-face-scene)]
                     (if scene (tree->text-ops scene) []))
                   (vec (range (count lines)))
                   []]

                (if flow-mode?
                  ;; Flow canvas: use pre-computed ops from intake or run
                  [(if (ws/local-world-intake? local-world) intake-text run-text)
                   (vec (range (count lines)))
                   []]

                ;; Normal editor mode
                (let [folded (or (:folded fold-state) #{})
                      regions (or (:regions fold-state) [])
                      total-line-count (count lines)
                      large-file? (and (> total-line-count 500) (empty? folded))
                      raw-start (max 0 (- (int (/ scroll-y line-h)) 5))
                      raw-end (+ (int (/ (+ scroll-y (:height viewport)) line-h)) 5)
                      visible-start (min total-line-count raw-start)
                      visible-end (min total-line-count (max visible-start raw-end))
                      visible-lines (subvec lines visible-start visible-end)
                      tokenized-visible (mapv tokenize-fn visible-lines)]
                  (if large-file?
                    (let [adjusted-y (+ layout-y (* visible-start line-h))
                          result (layout-fn tokenized-visible editor-lx adjusted-y font-size
                                            [] #{} char-advance line-h theme-id)
                          full-mapping (vec (range total-line-count))
                          nums (mapv (fn [i]
                                       (let [logical (+ visible-start i)
                                             num-str (str (inc logical))
                                             num-w (* (count num-str) char-advance)
                                             x (maybe-snap (- layout-x 8 num-w) dpr snap?)
                                             y (+ adjusted-y font-size (* i line-h))]
                                         [{:text num-str :type :line-number
                                           :from 0 :to (count num-str)
                                           :x x :y y :size font-size
                                           :r 0.45 :g 0.45 :b 0.45 :a 0.4}]))
                                     (range (count visible-lines)))]
                      [(:render-ops result) full-mapping nums])

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
                                             y (+ layout-y font-size (* visual-idx line-h))]
                                         [{:text num-str :type :line-number
                                           :from 0 :to (count num-str)
                                           :x x :y y :size font-size
                                           :r 0.45
                                           :g 0.45 :b 0.45 :a 0.4}]))
                                     (range (count mapping)))]
                      [(filterv seq (:render-ops result)) mapping nums])))))))

              ;; When file is open, clip editor text to left 40% and add right-tree text ops
              [editor-ops final-line-mapping line-num-ops]
              (if (and file-workspace? (not (:rt-node extract-preview)))
                (let [code-w (int (* content-vw (ws/pane-width-pct local-world :main 0.4)))
                      clip-left layout-x
                      header-h 36
                      clip-top (+ scroll-y header-h)
                      clip-sub (fn [sub]
                                 (let [x (or (:x sub) 0)
                                       y (or (:y sub) 0)
                                       fs (or (:size sub) font-size)
                                       cw (if (== fs font-size)
                                            char-advance
                                            (maybe-snap (* fs char-width) dpr snap?))
                                       txt (or (:text sub) "")
                                       text-end (+ x (* (count txt) cw))]
                                   (when (and (< x code-w) (> text-end clip-left) (>= y clip-top))
                                     (let [skip (if (< x clip-left) (min (count txt) (int (Math/ceil (/ (- clip-left x) cw)))) 0)
                                           adj-x (+ x (* skip cw))
                                           adj-txt (if (pos? skip) (subs txt skip) txt)
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
                                                      :local-world local-world
                                                      :active-pane active-pane :char-advance char-advance
                                                      :chat-scroll-y (or chat-scroll-y 0)
                                                      :chat-input chat-input :focus focus))
                      right-text-ops (tree->text-ops right-tree)
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

              ;; Bottom clip
              bottom-clip-y (+ scroll-y (- (:height viewport) cmd-panel-h status-bar-h))
              clip-bottom (fn [ops]
                            (into []
                              (keep (fn [op]
                                      (if (vector? op)
                                        (let [clipped (filterv #(< (or (:y %) 0) bottom-clip-y) op)]
                                          (when (seq clipped) clipped))
                                        (when (< (or (:y op) 0) bottom-clip-y) op))))
                              ops))

              offset-editor-ops (offset-text-ops* (clip-bottom editor-ops) sb-w)
              offset-line-num-ops (offset-text-ops* (clip-bottom line-num-ops) sb-w)]
          ;; Content-only return (chrome is in separate <chrome-text flow)
          {:ops (vec (concat (or sidebar-text-ops [])
                             offset-line-num-ops offset-editor-ops))
           :line-mapping final-line-mapping
           :editor-line-count (+ (count line-num-ops) (count editor-ops))}))
      <intake-text <run-text <layout
      (m/eduction (map :lines) (dedupe) (m/watch !editor-doc)) <fold-data (m/watch !scroll-y)
      (m/watch !current-file) (m/watch !effective-local-world) (m/watch !sidebar-scene)
      (m/watch !trail-face-scene) (m/watch !face-scene)
      (m/watch !extract-preview) (m/watch !agent-output)
      (m/watch !shimmer-phase) (m/watch !trail-collapsed) (m/watch !active-pane)
      (m/watch !scroll-x) (m/watch !chat-scroll-y) (m/watch !chat-input) (m/watch !focus))]
      ;; ── Combining flow: preserves identity of unchanged region ──
      (m/latest
        (fn [content chrome]
          {:content-ops (:ops content)
           :chrome-ops chrome
           :line-mapping (:line-mapping content)
           :editor-line-count (:editor-line-count content)})
        <content-text <chrome-text))))
