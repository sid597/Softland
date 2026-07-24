(ns app.client.workspace.runtime.state
  "Runtime state: atom creation, layout constants, font defaults.
   Returns the rt context map — the single shared contract for all runtime modules."
  (:require [app.client.substrate.webgpu.renderer :as editor]
            [app.client.substrate.webgpu.buffer-pool :as pool]
            [app.client.substrate.webgpu.gpu-budget :as gpu-budget]
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

(defn- initial-viewport [node]
  {:width  (max 1 (or (.-clientWidth node) 0))
   :height (max 1 (or (.-clientHeight node) 0))
   :dpr    (or (.-devicePixelRatio js/window) 1)})

(defn make-runtime-state
  "Create all runtime atoms and layout constants. Returns the rt context map.
   External atoms (!sidebar-visible, !file-load-request, !preview-el) are threaded through."
  [{:keys [node device ctx geometry font-assets initial-lines font-manifest
           !sidebar-visible !file-load-request !preview-el gpu-budget]}]
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
      :!viewport      (atom (initial-viewport node))
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
      :!font-assets   (atom (or font-assets {:backend :msdf :atlas nil :bitmap nil :id "dejavu-sans-mono"}))

      ;; GPU state (terminals update these)
      :!text-geo       (atom (:text geometry))
      :!gpu-budget     (atom gpu-budget)
      :!cmd-rect-sys   (atom (let [capacity 16
                                    size (* capacity editor/rect-stride)
                                    ib (.createBuffer device
                                         (clj->js {:size size
                                                    :usage (bit-or js/GPUBufferUsage.VERTEX
                                                                   js/GPUBufferUsage.COPY_DST)}))
                                    _ (gpu-budget/register-buffer! gpu-budget ib "runtime/cmd" size :active-bytes 0)]
                                {:pipeline (:pipeline (:rect geometry))
                                 :bind-group (:bind-group (:rect geometry))
                                 :instance-buffer ib
                                 :num-instances 0
                                 :gpu-tracker gpu-budget
                                 :gpu-label "runtime/cmd"}))
      :!settings-rect-sys (atom (let [capacity 32
                                       size (* capacity editor/rect-stride)
                                       ib (.createBuffer device
                                            (clj->js {:size size
                                                      :usage (bit-or js/GPUBufferUsage.VERTEX
                                                                     js/GPUBufferUsage.COPY_DST)}))
                                       _ (gpu-budget/register-buffer! gpu-budget ib "runtime/settings" size :active-bytes 0)]
                                   {:pipeline (:pipeline (:rect geometry))
                                    :bind-group (:bind-group (:rect geometry))
                                    :instance-buffer ib
                                    :num-instances 0
                                    :gpu-tracker gpu-budget
                                    :gpu-label "runtime/settings"}))
      ;; Per-source shadow pools (Phase 6C: differential rendering, 20 floats/shadow)
      ;; Separate pools prevent cross-source position shifts from causing full rewrites
      :!editor-shadow-pool  (pool/create-pool device 16
                              (:pipeline (:shadow geometry))
                              (:bind-group (:shadow geometry))
                              :floats-per-item 21 ;; 20 + container u32 (scene-substrate P2)
                              :pack-fn pool/pack-shadow
                              :tracker gpu-budget
                              :label "pool/editor-shadow")
      :!sidebar-shadow-pool (pool/create-pool device 64
                              (:pipeline (:shadow geometry))
                              (:bind-group (:shadow geometry))
                              :floats-per-item 21 ;; 20 + container u32 (scene-substrate P2)
                              :pack-fn pool/pack-shadow
                              :tracker gpu-budget
                              :label "pool/sidebar-shadow")

      ;; Sidebar buffer pool (differential rendering)
      :!sidebar-pool  (pool/create-pool device 256
                        (:pipeline (:rect geometry))
                        (:bind-group (:rect geometry))
                        :tracker gpu-budget
                        :label "pool/sidebar")

      ;; Editor rect pool (Phase 6A: differential rendering with stable identities)
      :!editor-pool   (pool/create-pool device 64
                        (:pipeline (:rect geometry))
                        (:bind-group (:rect geometry))
                        :tracker gpu-budget
                        :label "pool/editor")

      ;; Effective local world — the single semantic root.
      ;; Derived from truth+overlay+ui+artifacts. No independent write path.
      ;; Updated reactively via watches in runtime.cljs.
      :!effective-local-world (atom nil)

      ;; Artifact selection — the semantic intent ("the user chose this")
      ;; :kind = :file | :trail | :workflow (extensible)
      ;; :file → {:kind :file :path "..." :name "..."}
      ;; nil = nothing selected (home screen)
      :!selected-artifact (atom nil)

      ;; Sidebar / file state
      ;; TRANSITIONAL: downstream I/O cache. Updated when selected-artifact
      ;; is a :file and content finishes loading. Readers that check
      ;; "is a file open?" still use this; they'll migrate to
      ;; !selected-artifact in later phases.
      :!current-file  (atom {:path "/home/sid/projects/discourse-graph/apps/roam/src/index.ts"
                              :name "index.ts"})
      :!sidebar-truth (atom {:project nil
                             :expanded-dirs #{}
                             :selected-file nil})
      :!sidebar-overlay (atom {:pending-project nil
                               :pending-expanded-dirs #{}
                               :pending-collapsed-dirs #{}
                               :pending-selected-file nil})
      :!sidebar-ui    (atom {:hover-id nil
                             :scroll-y 0
                             :pointer-state :idle
                             :dir-cache {}
                             :home-dirs nil
                             :loading? false
                             :in-flight-dirs #{}
                             :in-flight-files #{}})
      ;; Shared sidebar scene — resolved tree cached by render flow,
      ;; consumed by hit-testing. Single source, two consumers.
      :!sidebar-scene (atom nil)

      ;; Trail face (view-mvp WP-B2): entry state set by /trail command
      ;; ({:face :text|:timeline :address <edn> :order :arrival|:claimed})
      ;; and the cached scene tree (built once per data/viewport change;
      ;; render flattens + mouse hit-tests the SAME object - gate 13).
      :!trail-face-state (atom nil)
      :!trail-face-scene (atom nil)
      ;; server truth pulls for the faces (set by Electric bridge)
      :!trail-text   (atom nil)
      :!trail-feed   (atom nil)
      :!trail-bundles (atom {})
      :!ingest-epoch (atom 0)
      ;; atlas coverage set (loaded once by trail-face wiring from
      ;; /font_atlas.json; passed as DATA into the pure sanitizer)
      :!trail-coverage (atom nil)

      ;; Faces-as-assemblies (framework CONTRACT §5, W1-INT): entry state set
      ;; by the /face command ({:face <kw> :address <addr|:default>
      ;; :params {:limit <n> :until-ms <ms|nil>}}); the whole §7 data-context
      ;; mirrored from the Electric pull; the wear-time compiled builder
      ;; (compile once per assembly change — trap T3); the cached scene tree
      ;; (built once per data change; combined_text flattens + mouse hit-tests
      ;; the SAME object — trap T9: outputs live here, watched inputs never
      ;; receive per-build writes).
      :!face-state    (atom nil)
      :!face-context  (atom nil)
      :!face-compiled (atom nil)
      :!face-scene    (atom nil)
      ;; W2 (CONTRACT §16): the arsenal roster (:face-list data-context,
      ;; mirrored whole by face-wiring) — the sidebar lists faces FROM RAMA
      ;; (trap T14), never the faces directory.
      :!face-list     (atom nil)
      ;; editable-material P3: all worn facet-masters arrive together as
      ;; confirmed projection data; ground never writes them optimistically.
      :!facet-materials (atom nil)

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

     :gpu {:device device :ctx ctx :geometry geometry :font-assets font-assets}
     :node node}))

(defn save-undo!
  "Push current state to undo stack (max 100), clear redo stack."
  [{:keys [!undo-stack !redo-stack]} lines cursor]
  (swap! !undo-stack conj {:lines lines :cursor cursor})
  (when (> (count @!undo-stack) 100)
    (swap! !undo-stack #(vec (drop 1 %))))
  (reset! !redo-stack []))
