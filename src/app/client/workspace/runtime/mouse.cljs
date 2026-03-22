(ns app.client.workspace.runtime.mouse
  "Mouse consumer: click routing, drag-select, paste handler, sidebar/flow/editor clicks."
  (:require [clojure.string :as str]
            [missionary.core :as m]
            [app.client.workspace.events :refer [maybe-snap]]
            [app.client.workspace.rect-tree :refer [resolve-layout hit-test]]
            [app.client.workspace.sidebar :refer [sidebar-w cmd-panel-h status-bar-h]]
            [app.client.workspace.shell :refer [build-file-layout]]
            [app.client.workspace.cmd-panel :refer [cmd-panel-apply-event cmd-text-start-x]]
            [app.client.workspace.editor-compute :refer [editor-apply-event]]
            [app.client.workspace.settings-view :refer [slider-specs]]
            [app.client.workspace.ui-primitives :refer [list-left-pane-pct]]
            [app.client.workspace.runtime.state :refer [save-undo!]]
            [app.client.workspace.runtime.sidebar-io :refer [emit-sidebar-action!]]
            [app.client.workflows.dg-flow :refer [flow-canvas-active? build-intake-tree
                                                   drag-distance drag-threshold-px
                                                   set-selection]]))

;; ═══════════════════════════════════════════════════════════════════════
;; Paste + drag-select (raw DOM, not Missionary)
;; ═══════════════════════════════════════════════════════════════════════

(defn install-paste-handler!
  "Wire system clipboard paste to editor/cmd/chat based on focus."
  [{:keys [!clipboard !focus !editor-doc !cmd-panel !chat-input !caret-visible
           !undo-stack !redo-stack]}]
  (let [paste-handler
        (fn [e]
          (let [text (.getData (.-clipboardData e) "text/plain")]
            (when (seq text)
              (.preventDefault e)
              (reset! !clipboard text)
              (case @!focus
                :editor
                (let [doc @!editor-doc
                      lengths (mapv count (:lines doc))]
                  (save-undo! {:!undo-stack !undo-stack :!redo-stack !redo-stack}
                              (:lines doc) (:cursor doc))
                  (let [new-doc (editor-apply-event doc {:type :paste} lengths text)]
                    (reset! !editor-doc new-doc)
                    (reset! !caret-visible true)))
                :command-panel
                (let [panel @!cmd-panel
                      new-panel (cmd-panel-apply-event panel {:type :paste} text)]
                  (reset! !cmd-panel new-panel)
                  (reset! !caret-visible true))
                :chat
                (let [ci @!chat-input
                      ci-text (:text ci)
                      ci-cursor (:cursor ci)
                      new-text (str (subs ci-text 0 ci-cursor) text (subs ci-text ci-cursor))]
                  (reset! !chat-input {:text new-text :cursor (+ ci-cursor (count text))})
                  (reset! !caret-visible true))
                nil))))]
    (.addEventListener js/window "paste" paste-handler)))

(defn install-drag-select!
  "Install raw DOM drag-select listeners (bypasses Missionary async scheduling)."
  [{:keys [!scroll-y !sidebar-visible !viewport !settings !active-font !scroll-x
           !text-geo !editor-doc !current-file !flow-state !drag-start !dragging?
           !focus !caret-visible]}
   {:keys [layout-x layout-y]}
   node]
  (let [get-coords (fn [e]
                     (let [rect (.getBoundingClientRect node)]
                       {:x (- (.-clientX e) (.-left rect))
                        :y (- (.-clientY e) (.-top rect))}))
        mouse->pos (fn [{:keys [x y]}]
                     (let [scroll-y @!scroll-y
                           sb-w (if (and !sidebar-visible @!sidebar-visible) sidebar-w 0)
                           local-x (- x sb-w)
                           dpr (:dpr @!viewport)
                           snap? (:snap-to-pixel? @!settings)
                           font-size (:font-size @!settings)
                           char-width (:char-width @!active-font)
                           line-h (maybe-snap (* font-size (:line-height @!settings)) dpr snap?)
                           char-w (maybe-snap (* font-size char-width) dpr snap?)
                           adj-y (+ y scroll-y)
                           lx (maybe-snap (- layout-x (or @!scroll-x 0)) dpr snap?)
                           ly (maybe-snap layout-y dpr snap?)
                           text-result @!text-geo
                           line-mapping (or (:line-mapping text-result) [])
                           lengths (mapv count (:lines @!editor-doc))
                           visual-line (max 0 (Math/floor (/ (- adj-y ly) line-h)))
                           logical-line (get line-mapping visual-line
                                             (min visual-line (dec (count lengths))))
                           line-len (get lengths logical-line 0)
                           col (-> (/ (- local-x lx) char-w)
                                   (Math/round)
                                   (max 0)
                                   (min line-len))]
                       {:line logical-line :col col}))]
    (.addEventListener node "mousedown"
      (fn [e]
        (let [coords (get-coords e)
              {:keys [x]} coords
              sb-w (if (and !sidebar-visible @!sidebar-visible) sidebar-w 0)
              local-x (- x sb-w)
              content-w (- (:width @!viewport) sb-w)
              code-w (int (* content-w 0.4))
              in-editor? (and (some? (:path @!current-file)) (< local-x code-w))
              in-normal-editor? (and (nil? (:path @!current-file))
                                     (not (flow-canvas-active? @!flow-state)))]
          (when (or in-editor? in-normal-editor?)
            (let [pos (mouse->pos coords)]
              (reset! !drag-start pos)
              (reset! !dragging? true)
              (swap! !editor-doc assoc
                     :cursor pos :selection nil :desired-col (:col pos))
              (reset! !focus :editor)
              (reset! !caret-visible true))))))
    (.addEventListener js/window "mousemove"
      (fn [e]
        (when @!dragging?
          (let [pos (mouse->pos (get-coords e))
                start @!drag-start]
            (when (and start (not= pos start))
              (swap! !editor-doc assoc
                     :selection {:start start :end pos}))))))
    (.addEventListener js/window "mouseup"
      (fn [_]
        (reset! !dragging? false)
        (reset! !drag-start nil)))))

(def ^:private flow-cwd "/home/sid/projects/discourse-graph")

;; ═══════════════════════════════════════════════════════════════════════
;; Mousedown branch helpers
;; ═══════════════════════════════════════════════════════════════════════

(defn- handle-settings-click!
  "Route click within the settings panel overlay."
  [{:keys [!settings !font-manifest !active-font]} x y panel-x panel-y settings]
  (let [left-w 220
        header-h 40
        content-y (+ panel-y header-h)
        font-item-h 32
        slider-item-h 58
        slider-count (count (slider-specs settings))
        rel-x (- x panel-x)
        rel-y (- y content-y)]
    (cond
      (< rel-y 0) nil
      (< rel-x left-w)
      (let [font-idx (int (/ rel-y font-item-h))
            fonts (or (:fonts @!font-manifest)
                      [{:name "DejaVu Sans Mono" :id "dejavu-sans-mono"}])
            available-fonts (filterv #(not (false? (:available %))) fonts)]
        (when (< font-idx (count available-fonts))
          (swap! !settings assoc :selected-index font-idx :focus-section :fonts)
          (let [selected-font (nth available-fonts font-idx)]
            (reset! !active-font {:id (:id selected-font)
                                  :char-width (or (:charWidth selected-font) 0.56)
                                  :name (:name selected-font)}))))
      :else
      (let [slider-idx (int (/ rel-y slider-item-h))]
        (when (< slider-idx slider-count)
          (swap! !settings assoc :slider-index slider-idx :focus-section :sliders))))))

(defn- handle-sidebar-click!
  "Route click within the sidebar file explorer.
   Reads from the shared sidebar scene (cached by render flow) — same tree
   that produced the current frame's rects. No redundant tree rebuild.
   Optimistic local updates for instant UI, fire-and-forget POST to Rama.
   Committed truth flows back via Electric subscription."
  [{:keys [!sidebar-truth !sidebar-overlay !sidebar-ui !current-file !sidebar-scene !settings !active-font]}
   {:keys [fetch-dir! fetch-file!]}
   x y viewport scroll-y]
  (let [t0 (js/performance.now)
        tree @!sidebar-scene
        path (when tree (hit-test tree x (+ y scroll-y)))
        t1 (js/performance.now)]
    (js/console.log "[SIDEBAR-CLICK] hit-test:" (.toFixed (- t1 t0) 2) "ms | hit:" (some? path))
    (when path
      (some (fn [node]
              (case (:type node)
                :sidebar-entry
                (let [d (:data node)
                      et (:entry-type d)]
                  (js/console.log "[SIDEBAR-CLICK] matched:" (str et) "| node-id:" (str (:id node)))
                  (case et
                    :back-btn
                    (do
                      (swap! !sidebar-overlay assoc
                             :pending-project {:path nil}
                             :pending-expanded-dirs #{}
                             :pending-collapsed-dirs #{}
                             :pending-selected-file {:path nil})
                      (swap! !sidebar-ui assoc :dir-cache {} :scroll-y 0)
                      (reset! !current-file nil)
                      (emit-sidebar-action! :sidebar/project-back {} nil)
                      (js/console.log "[SIDEBAR-CLICK] back total:" (.toFixed (- (js/performance.now) t0) 2) "ms")
                      true)
                    :home-dir
                    (let [entry (:entry d)]
                      (swap! !sidebar-overlay assoc
                             :pending-project {:name (:name entry) :path (:path entry)}
                             :pending-expanded-dirs #{}
                             :pending-collapsed-dirs #{}
                             :pending-selected-file {:path nil})
                      (swap! !sidebar-ui assoc :dir-cache {} :scroll-y 0)
                      (emit-sidebar-action! :sidebar/project-select
                        {:name (:name entry) :path (:path entry)} nil)
                      (fetch-dir! (:path entry))
                      (js/console.log "[SIDEBAR-CLICK] home-dir total:" (.toFixed (- (js/performance.now) t0) 2) "ms")
                      true)
                    :dir
                    (let [entry (:entry d) dir-path (:path entry)
                          truth-exp (or (:expanded-dirs @!sidebar-truth) #{})
                          overlay @!sidebar-overlay
                          eff-exp (clojure.set/difference 
                                    (clojure.set/union truth-exp (:pending-expanded-dirs overlay)) 
                                    (:pending-collapsed-dirs overlay))
                          expanding? (not (contains? eff-exp dir-path))]
                      (if expanding?
                        (swap! !sidebar-overlay (fn [o] (-> o
                                                            (update :pending-expanded-dirs conj dir-path)
                                                            (update :pending-collapsed-dirs disj dir-path))))
                        (swap! !sidebar-overlay (fn [o] (-> o
                                                            (update :pending-collapsed-dirs conj dir-path)
                                                            (update :pending-expanded-dirs disj dir-path)))))
                      (emit-sidebar-action! :sidebar/dir-toggle {:path dir-path} nil)
                      (when expanding?
                        (fetch-dir! dir-path))
                      (js/console.log "[SIDEBAR-CLICK] dir-toggle total:" (.toFixed (- (js/performance.now) t0) 2) "ms")
                      true)
                    :file
                    (let [entry (:entry d)
                          project (or (:pending-project @!sidebar-overlay) (:project @!sidebar-truth))]
                      (swap! !sidebar-overlay assoc :pending-selected-file {:path (:path entry) :name (:name entry)})
                      (emit-sidebar-action! :sidebar/file-select
                        {:path (:path entry) :name (:name entry)} nil)
                      (fetch-file! (:path entry) (:path project))
                      (js/console.log "[SIDEBAR-CLICK] file-select total:" (.toFixed (- (js/performance.now) t0) 2) "ms")
                      true)
                    nil))
                nil))
            (rseq path)))))

(defn- handle-cmd-click!
  "Place cursor in the command panel."
  [{:keys [!focus !cmd-panel !caret-visible !settings !viewport !active-font] :as atoms}
   x sb-vis? cmd-panel]
  (let [font-size (:font-size @!settings)
        dpr (:dpr @!viewport)
        snap? (:snap-to-pixel? @!settings)
        char-width (:char-width @!active-font)
        char-w (maybe-snap (* font-size char-width) dpr snap?)
        sb-w (if sb-vis? sidebar-w 0)
        text-x (+ (cmd-text-start-x @(get atoms :!ai-provider) font-size char-width dpr snap?) sb-w)
        text (:text cmd-panel)
        col (-> (/ (- x text-x) char-w) (Math/round) (max 0) (min (count text)))]
    (reset! !focus :command-panel)
    (swap! !cmd-panel assoc :cursor col)
    (reset! !caret-visible true)))

(defn- handle-editor-click!
  "Place cursor or toggle fold in the editor gutter/body. Shared by file-layout and normal mode."
  [{:keys [!viewport !settings !active-font !scroll-x !text-geo !editor-doc
           !folded-lines !drag-start !caret-visible !focus]}
   {:keys [detect-folds-fn]}
   layout-x layout-y gutter-w x adj-y local-x]
  (let [dpr (:dpr @!viewport)
        snap? (:snap-to-pixel? @!settings)
        font-size (:font-size @!settings)
        char-width (:char-width @!active-font)
        line-h (maybe-snap (* font-size (:line-height @!settings)) dpr snap?)
        char-w (maybe-snap (* font-size char-width) dpr snap?)
        elx (maybe-snap (- layout-x (or @!scroll-x 0)) dpr snap?)
        ely (maybe-snap layout-y dpr snap?)
        gutter-x (- elx gutter-w)
        gutter-right (+ gutter-x gutter-w)]
    (if (and (>= local-x gutter-x) (< local-x gutter-right))
      ;; Gutter click — toggle fold
      (let [text-result @!text-geo
            line-mapping (or (:line-mapping text-result) [])
            visual-line (max 0 (Math/floor (/ (- adj-y ely) line-h)))
            logical-line (get line-mapping visual-line visual-line)
            regions (detect-folds-fn (:lines @!editor-doc) (mapv count (:lines @!editor-doc)))
            fold-region (first (filter #(= (:start-line %) logical-line) (or regions [])))]
        (when fold-region
          (swap! !folded-lines
                 (fn [folded]
                   (if (contains? folded logical-line)
                     (disj folded logical-line) (conj folded logical-line)))))
        (reset! !focus :editor))
      ;; Body click — place cursor
      (let [text-result @!text-geo
            line-mapping (or (:line-mapping text-result) [])
            lengths (mapv count (:lines @!editor-doc))
            visual-line (max 0 (Math/floor (/ (- adj-y ely) line-h)))
            logical-line (get line-mapping visual-line (min visual-line (dec (count lengths))))
            line-len (get lengths logical-line 0)
            col (-> (/ (- local-x elx) char-w) (Math/round) (max 0) (min line-len))
            pos {:line logical-line :col col}]
        (when-not @!drag-start
          (swap! !editor-doc assoc :cursor pos :selection nil :desired-col col)
          (reset! !caret-visible true)
          (reset! !focus :editor))))))

(defn- handle-chat-click!
  "Route click within the chat pane: nav links and tool collapse toggles."
  [{:keys [!settings !active-font !shimmer-phase !current-file !agent-output
           !trail-collapsed !active-pane !chat-scroll-y !chat-input !focus
           !editor-doc !scroll-y !sidebar-truth]}
   {:keys [fetch-file!]}
   rel-x y content-w viewport scroll-y]
  (let [file-layout-h (- (:height viewport) cmd-panel-h status-bar-h)]
    (when (< y (- file-layout-h 36))
      (let [font-size (:font-size @!settings)
            char-advance (* font-size (:char-width @!active-font))
            shimmer-alpha (if @!shimmer-phase 0.9 0.4)
            tree (resolve-layout
                   (build-file-layout content-w file-layout-h
                                      @!current-file @!agent-output font-size
                                      shimmer-alpha @!trail-collapsed
                                      :active-pane @!active-pane :char-advance char-advance
                                      :chat-scroll-y (or @!chat-scroll-y 0)
                                      :chat-input @!chat-input :focus @!focus))
            path (hit-test tree rel-x y)]
        (when path
          (let [handled-nav?
                (some (fn [node]
                        (when-let [nav (:nav (:data node))]
                          (let [file-path (:file-path nav)
                                target-line (or (:line nav) 0)
                                current-path (:path @!current-file)
                                project (or (:path (:project @!sidebar-truth)) flow-cwd)
                                same-file? (or (= file-path current-path)
                                               (and current-path
                                                    (str/ends-with? current-path file-path)))]
                            (if same-file?
                              (let [safe-line (min target-line
                                                   (max 0 (dec (count (:lines @!editor-doc)))))]
                                (swap! !editor-doc assoc
                                       :cursor {:line safe-line :col 0}
                                       :selection nil :desired-col 0)
                                (let [line-h (* font-size (:line-height @!settings))]
                                  (reset! !scroll-y (max 0 (- (* safe-line line-h) 100)))))
                              (fetch-file! file-path project :target-line target-line))
                            (reset! !active-pane :editor)
                            true)))
                      (rseq path))]
            (when-not handled-nav?
              (some (fn [node]
                      (when (= :tool-header (:type node))
                        (when-let [cid (:collapse-id (:data node))]
                          (swap! !trail-collapsed
                                 (fn [s] (if (contains? s cid) (disj s cid) (conj s cid))))
                          true)))
                    (rseq path)))))))))

(defn- handle-flow-canvas-click!
  "Route click within the DG flow canvas (group headers + ticket rows)."
  [{:keys [!flow-state !collapsed-groups !hovered-row-idx !drag-state !settings !active-font]}
   content-x y content-w viewport-h scroll-y]
  (let [flow @!flow-state
        font-size (:font-size @!settings)
        char-advance (* font-size (:char-width @!active-font))
        tree (resolve-layout
               (build-intake-tree flow content-w viewport-h
                                  scroll-y nil @!hovered-row-idx @!collapsed-groups
                                  font-size char-advance nil))
        path (hit-test tree content-x (+ y scroll-y))]
    (when path
      (some (fn [node]
              (case (:type node)
                :group-header
                (let [status (:status (:data node))]
                (swap! !collapsed-groups
                       (fn [cg] (if (contains? cg status) (disj cg status) (conj cg status))))
                  true)
                :ticket-row
                (do (reset! !drag-state {:phase :pending :origin {:x content-x :y y} :node node})
                    true)
                nil))
            (rseq path)))))

;; ═══════════════════════════════════════════════════════════════════════
;; Top-level case handlers
;; ═══════════════════════════════════════════════════════════════════════

(defn- handle-mousedown!
  "Route mousedown to the appropriate zone handler."
  [{:keys [!viewport !scroll-y !settings !sidebar-visible !current-file !cmd-panel
           !flow-state !focus !caret-visible !active-pane] :as atoms}
   {:keys [layout-x layout-y gutter-w] :as layout}
   deps io x y]
  (let [viewport @!viewport
        scroll-y @!scroll-y
        settings @!settings
        sb-vis? (and !sidebar-visible @!sidebar-visible)
        ;; Settings overlay
        panel-w 600  panel-h 480
        panel-x (/ (- (:width viewport) panel-w) 2)
        panel-y (+ scroll-y (/ (- (:height viewport) panel-h) 2))
        in-settings? (and (:visible settings)
                          (>= x panel-x) (< x (+ panel-x panel-w))
                          (>= y panel-y) (< y (+ panel-y panel-h)))]
    (cond
      in-settings?
      (handle-settings-click! atoms x y panel-x panel-y settings)

      (and sb-vis? (< x sidebar-w))
      (handle-sidebar-click! atoms io x y viewport scroll-y)

      (>= y (- (:height viewport) status-bar-h))
      nil ;; status bar — no-op

      :else
      (let [cmd-panel @!cmd-panel
            cmd-visible? (or (:visible cmd-panel)
                             (some? @!current-file)
                             (flow-canvas-active? @!flow-state))
            cmd-panel-top (if cmd-visible?
                            (- (:height viewport) cmd-panel-h status-bar-h)
                            (:height viewport))
            in-cmd? (and cmd-visible? (>= y cmd-panel-top)
                         (< y (- (:height viewport) status-bar-h)))]
        (if in-cmd?
          (handle-cmd-click! atoms x sb-vis? cmd-panel)
          ;; Editor area
          (do
            (when (:visible @!cmd-panel)
              (swap! !cmd-panel assoc :visible false))
            (cond
              (flow-canvas-active? @!flow-state)
              (let [sb-w (if sb-vis? sidebar-w 0)]
                (handle-flow-canvas-click! atoms
                                           (- x sb-w) y
                                           (- (:width viewport) sb-w)
                                           (:height viewport)
                                           scroll-y))

              (some? @!current-file)
              (let [sb-w (if sb-vis? sidebar-w 0)
                    content-w (- (:width viewport) sb-w)
                    code-w (int (* content-w 0.4))
                    chat-w (int (* content-w 0.55))
                    rel-x (- x sb-w)
                    in-editor? (< rel-x code-w)
                    in-chat? (and (>= rel-x code-w) (< rel-x (+ code-w chat-w)))
                    clicked-pane (cond in-editor? :editor in-chat? :chat :else :preview)]
                (reset! !active-pane clicked-pane)
                (when in-editor?
                  (handle-editor-click! atoms deps layout-x layout-y gutter-w
                                        x (+ y scroll-y) (- x sb-w)))
                (when in-chat?
                  (reset! !focus :chat)
                  (reset! !caret-visible true)
                  (handle-chat-click! atoms io rel-x y content-w viewport scroll-y)))

              :else
              (let [sb-w (if sb-vis? sidebar-w 0)]
                (handle-editor-click! atoms deps layout-x layout-y gutter-w
                                      x (+ y scroll-y) (- x sb-w))))))))))

(defn- handle-mousemove!
  "Route mousemove: sidebar hover, drag state machine, flow canvas hover."
  [{:keys [!mouse-x !mouse-y !sidebar-visible !sidebar-ui !sidebar-scene !settings !active-font
           !current-file !viewport !scroll-y !drag-state !flow-state
           !hovered-row-idx !collapsed-groups]}
   coords]
  (reset! !mouse-x (:x coords))
  (reset! !mouse-y (:y coords))
  ;; Sidebar hover — writes to dedicated atom (:hover-id in !sidebar-ui) to avoid
  ;; triggering expensive <combined-text-ops and <cmd-panel-rects flows
  (let [sb-vis? (and !sidebar-visible @!sidebar-visible)
        in-sidebar? (and sb-vis? (< (:x coords) sidebar-w))]
    (if in-sidebar?
      (let [tree @!sidebar-scene
            path (when tree (hit-test tree (:x coords) (+ (:y coords) @!scroll-y)))
            new-id (when path
                     (some (fn [node]
                             (when (= :sidebar-entry (:type node))
                               (:id node)))
                           (rseq path)))
            old-id (:hover-id @!sidebar-ui)]
        (when (not= new-id old-id)
          (swap! !sidebar-ui assoc :hover-id new-id))
        (when @!hovered-row-idx
          (reset! !hovered-row-idx nil)))
      (when (:hover-id @!sidebar-ui)
        (swap! !sidebar-ui assoc :hover-id nil))))
  ;; Drag state machine
  (let [ds @!drag-state
        mx (:x coords) my (:y coords)]
    (case (:phase ds)
      :pending
      (when (> (drag-distance ds mx my) drag-threshold-px)
        (reset! !drag-state {:phase :dragging :origin (:origin ds)
                             :node (:node ds) :current {:x mx :y my}}))
      :dragging
      (swap! !drag-state assoc :current {:x mx :y my})
      nil))
  ;; Flow canvas hover
  (let [sb-vis? (and !sidebar-visible @!sidebar-visible)
        sb-w (if sb-vis? sidebar-w 0)
        in-sidebar? (and sb-vis? (< (:x coords) sidebar-w))]
    (when (and (flow-canvas-active? @!flow-state)
               (not in-sidebar?)
               (= :idle (:phase @!drag-state)))
      (let [flow @!flow-state
            content-x (- (:x coords) sb-w)
            content-w (- (:width @!viewport) sb-w)
            font-size (:font-size @!settings)
            char-advance (* font-size (:char-width @!active-font))
            tree (resolve-layout
                   (build-intake-tree flow content-w (:height @!viewport)
                                      @!scroll-y nil @!hovered-row-idx @!collapsed-groups
                                      font-size char-advance nil))
            path (hit-test tree content-x (+ (:y coords) @!scroll-y))
            new-idx (when path
                      (some (fn [node]
                              (when (= :ticket-row (:type node))
                                (:idx (:data node))))
                            (rseq path)))]
        (when (not= new-idx @!hovered-row-idx)
          (reset! !hovered-row-idx new-idx))))))

(defn- handle-mouseup!
  "Route mouseup: ticket selection or drag-to-select completion."
  [{:keys [!drag-state !flow-state !viewport !sidebar-visible]}]
  (let [ds @!drag-state]
    (case (:phase ds)
      :pending
      (do (let [node (:node ds)]
            (when (= :ticket-row (:type node))
              (let [idx (:idx (:data node))
                    flow @!flow-state
                    selected (:selected flow)
                    already? (some #{idx} selected)
                    new-sel (if already?
                              (vec (remove #{idx} selected))
                              (conj (vec selected) idx))]
                (swap! !flow-state set-selection new-sel))))
          (reset! !drag-state {:phase :idle}))
      :dragging
      (let [sb-vis? (and !sidebar-visible @!sidebar-visible)
            sb-w (if sb-vis? sidebar-w 0)
            node (:node ds)
            cur (:current ds)
            content-w (- (:width @!viewport) sb-w)
            left-w (int (* content-w list-left-pane-pct))
            cur-x (- (:x cur) sb-w)]
        (when (and node cur (= :ticket-row (:type node)))
          (if (>= cur-x left-w)
            (let [idx (:idx (:data node))
                  flow @!flow-state
                  selected (:selected flow)
                  already? (some #{idx} selected)]
              (when-not already?
                (swap! !flow-state set-selection (conj (vec selected) idx))))
            nil))
        (reset! !drag-state {:phase :idle}))
      nil)))

;; ═══════════════════════════════════════════════════════════════════════
;; Missionary consumer
;; ═══════════════════════════════════════════════════════════════════════

(defn mouse-consumer
  "Missionary consumer: route mouse events to named handlers."
  [atoms layout deps io >mouse-events]
  (->> >mouse-events
       (m/reduce
         (fn [_ [type coords]]
           (case type
             :mousedown (handle-mousedown! atoms layout deps io (:x coords) (:y coords))
             :mousemove (handle-mousemove! atoms coords)
             :mouseup   (handle-mouseup! atoms)
             nil))
         nil)))
