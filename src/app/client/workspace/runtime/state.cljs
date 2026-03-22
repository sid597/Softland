(ns app.client.workspace.runtime.state
  "Runtime state: atom creation, layout constants, font defaults.
   Returns the rt context map — the single shared contract for all runtime modules."
  (:require [app.client.substrate.webgpu.renderer :as editor]
            [app.client.substrate.webgpu.buffer-pool :as pool]
            [app.client.workspace.settings-view :refer [manifest-defaults->settings font-defaults->settings]]
            [app.client.workflows.dg-flow :refer [initial-flow-state]]))

(def default-font-manifest
  {:fonts [{:name "DejaVu Sans Mono"
            :id "dejavu-sans-mono"
            :charWidth 0.56
            :default true
            :defaults {:fontSize 19
                       :lineHeight 1.2
                       :pxRange 8
                       :sharpness 0.0
                       :snapToPixel true
                       :showDiagnostics false}}]
   :settings {:fontSize {:default 19}
              :lineHeight {:default 1.2}
              :pxRange {:default 8}
              :sharpness {:default 0.0}
              :snapToPixel {:default true}
              :showDiagnostics {:default false}}})

(defn make-runtime-state
  "Create all runtime atoms and layout constants. Returns the rt context map.
   External atoms (!sidebar-visible, !file-load-request, !preview-el) are threaded through."
  [{:keys [node device ctx geometry atlas initial-lines font-manifest
           !sidebar-visible !file-load-request !preview-el]}]
  (let [;; Layout constants
        gutter-w 40
        layout-x (+ 50 gutter-w)
        layout-y 100

        ;; Font manifest processing
        manifest (or font-manifest default-font-manifest)
        fonts (or (:fonts manifest) [])
        available-fonts (filterv #(not (false? (:available %))) fonts)
        default-font (or (first (filter :default available-fonts))
                         (first available-fonts)
                         {:id "dejavu-sans-mono" :name "DejaVu Sans Mono" :charWidth 0.56})
        default-font-idx (or (first (keep-indexed (fn [idx font]
                                                    (when (= (:id font) (:id default-font)) idx))
                                                  available-fonts))
                             0)
        manifest-settings (manifest-defaults->settings (:settings manifest))
        base-settings {:visible false
                       :font-id (:id default-font)
                       :selected-index default-font-idx
                       :slider-index 0
                       :focus-section :fonts}
        initial-settings (merge base-settings
                                manifest-settings
                                (font-defaults->settings default-font))]

    {:layout {:layout-x layout-x :layout-y layout-y :gutter-w gutter-w}

     :atoms
     {;; Editor core
      :!editor-doc    (atom {:lines initial-lines
                             :cursor {:line 0 :col 0}
                             :selection nil
                             :desired-col 0})
      :!cmd-panel     (atom {:text "" :cursor 0 :visible true})
      :!focus         (atom :command-panel)
      :!scroll-y      (atom 0)
      :!scroll-x      (atom 0)
      :!run-scroll-y  (atom 0)
      :!detail-scroll-y (atom 0)
      :!viewport      (atom {:width  (.-clientWidth node)
                              :height (.-clientHeight node)
                              :dpr    (or (.-devicePixelRatio js/window) 1)})
      :!folded-lines  (atom #{})
      :!caret-visible (atom true)
      :!clipboard     (atom nil)
      :!undo-stack    (atom [])
      :!redo-stack    (atom [])
      :!eval-result   (atom nil)
      :!dragging?     (atom false)
      :!active-pane   (atom :editor)
      :!drag-start    (atom nil)

      ;; Font / settings
      :!settings      (atom initial-settings)
      :!font-manifest (atom manifest)
      :!active-font   (atom {:id (:id default-font)
                              :char-width (or (:charWidth default-font) 0.56)
                              :name (:name default-font)})
      :!font-assets   (atom {:atlas atlas :bitmap nil :id "dejavu-sans-mono"})

      ;; GPU state (terminals update these)
      :!text-geo       (atom (:text geometry))
      :!editor-rect-sys (atom (:rect geometry))
      :!cmd-rect-sys   (atom (let [capacity 16
                                    ib (.createBuffer device
                                         (clj->js {:size (* capacity editor/rect-stride)
                                                    :usage (bit-or js/GPUBufferUsage.VERTEX
                                                                   js/GPUBufferUsage.COPY_DST)}))]
                                {:pipeline (:pipeline (:rect geometry))
                                 :bind-group (:bind-group (:rect geometry))
                                 :instance-buffer ib
                                 :num-instances 0}))
      :!settings-rect-sys (atom (let [capacity 32
                                       ib (.createBuffer device
                                            (clj->js {:size (* capacity editor/rect-stride)
                                                      :usage (bit-or js/GPUBufferUsage.VERTEX
                                                                     js/GPUBufferUsage.COPY_DST)}))]
                                   {:pipeline (:pipeline (:rect geometry))
                                    :bind-group (:bind-group (:rect geometry))
                                    :instance-buffer ib
                                    :num-instances 0}))
      :!shadow-sys    (atom (:shadow geometry))

      ;; Sidebar buffer pool (differential rendering — separate from editor-rect-sys)
      :!sidebar-pool  (pool/create-pool device 256
                        (:pipeline (:rect geometry))
                        (:bind-group (:rect geometry)))

      ;; Sidebar / file state
      :!current-file  (atom {:path "/home/sid/projects/discourse-graph/apps/roam/src/index.ts"
                              :name "index.ts"})
      :!sidebar-state (atom {:project nil
                              :expanded-dirs #{}
                              :dir-cache {}
                              :home-dirs nil
                              :scroll-y 0
                              :hovered-id nil
                              :loading? false})

      ;; Agent / AI
      :!ai-provider     (atom :claude)
      :!agent-output    (atom nil)
      :!agent-scroll-y  (atom 0)
      :!chat-scroll-y   (atom 0)
      :!chat-input      (atom {:text "" :cursor 0})

      ;; Mouse tracking
      :!mouse-x (atom 0)
      :!mouse-y (atom 0)

      ;; Workflow
      :!flow-state       (atom (initial-flow-state))
      :!collapsed-groups (atom #{})
      :!hovered-row-idx  (atom nil)
      :!drag-state       (atom {:phase :idle})
      :!extract-preview  (atom nil)
      :!trail-collapsed  (atom #{})
      :!shimmer-phase    (atom false)

      ;; External atoms (passed through from caller)
      :!sidebar-visible    !sidebar-visible
      :!file-load-request  !file-load-request
      :!preview-el         !preview-el}

     :gpu {:device device :ctx ctx :geometry geometry :atlas atlas}
     :node node}))

(defn save-undo!
  "Push current state to undo stack (max 100), clear redo stack."
  [{:keys [!undo-stack !redo-stack]} lines cursor]
  (swap! !undo-stack conj {:lines lines :cursor cursor})
  (when (> (count @!undo-stack) 100)
    (swap! !undo-stack #(vec (drop 1 %))))
  (reset! !redo-stack []))
