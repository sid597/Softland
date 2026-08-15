(ns app.client.workspace.runtime.state
  "Runtime state: atom creation, layout constants, font defaults.
   Returns the rt context map — the single shared contract for all runtime
   modules. Post ground-boot (first-light T9) the map carries only the land's
   state: fonts/settings, the GPU systems, the trail-face pull lane, and the
   faces lane. The dev workspace's editor/sidebar/flow/chat atoms died with
   that surface (dead-path census, Sid's ruling 2026-08-15)."
  (:require [app.client.substrate.webgpu.renderer :as editor]
            [app.client.substrate.webgpu.buffer-pool :as pool]
            [app.client.substrate.webgpu.gpu-budget :as gpu-budget]
            [app.client.workspace.runtime.fonts :as fonts]))

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
  "Create all runtime atoms and layout constants. Returns the rt context map."
  [{:keys [node device ctx geometry font-assets font-manifest gpu-budget]}]
  (let [manifest (or font-manifest default-font-manifest)
        fonts* (or (:fonts manifest) [])
        available-fonts (filterv #(not (false? (:available %))) fonts*)
        default-font (or (first (filter :default available-fonts))
                         (first available-fonts)
                         {:id "dejavu-sans-mono" :name "DejaVu Sans Mono" :charWidth 0.56})
        default-font-idx (or (first (keep-indexed (fn [idx font]
                                                    (when (= (:id font) (:id default-font)) idx))
                                                  available-fonts))
                             0)
        manifest-settings (fonts/manifest-defaults->settings (:settings manifest))
        base-settings {:visible false
                       :font-id (:id default-font)
                       :selected-index default-font-idx
                       :slider-index 0
                       :focus-section :fonts}
        initial-settings (merge base-settings
                                manifest-settings
                                (fonts/font-defaults->settings default-font))]

    {:layout {}

     :atoms
     {;; Session basics
      :!focus         (atom :ground-input)
      :!scroll-y      (atom 0)
      :!viewport      (atom (initial-viewport node))

      ;; Font / settings
      :!settings      (atom initial-settings)
      :!font-manifest (atom manifest)
      :!active-font   (atom {:id (:id default-font)
                              :char-width (or (:charWidth default-font) 0.56)
                              :name (:name default-font)
                              :layout-provider (:layout-provider font-assets)})
      :!font-assets   (atom (or font-assets {:backend :msdf :atlas nil :bitmap nil :id "dejavu-sans-mono"}))

      ;; GPU state (terminals update these)
      :!text-geo       (atom (:text geometry))
      :!gpu-budget     (atom gpu-budget)
      ;; Generic rect systems threaded through draw-frame!'s stable API
      ;; (the renderer floor keeps its capability surface; these carry zero
      ;; instances on the ground).
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
      :!settings-rect-sys (atom (let [capacity 16
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
      ;; Store-slot pools: face/ground block rects + shadows ride these.
      :!editor-shadow-pool  (pool/create-pool device 16
                              (:pipeline (:shadow geometry))
                              (:bind-group (:shadow geometry))
                              :floats-per-item 21 ;; 20 + container u32 (scene-substrate P2)
                              :pack-fn pool/pack-shadow
                              :tracker gpu-budget
                              :label "pool/editor-shadow")
      :!sidebar-shadow-pool (pool/create-pool device 16
                              (:pipeline (:shadow geometry))
                              (:bind-group (:shadow geometry))
                              :floats-per-item 21
                              :pack-fn pool/pack-shadow
                              :tracker gpu-budget
                              :label "pool/sidebar-shadow")
      :!sidebar-pool  (pool/create-pool device 16
                        (:pipeline (:rect geometry))
                        (:bind-group (:rect geometry))
                        :tracker gpu-budget
                        :label "pool/sidebar")
      :!editor-pool   (pool/create-pool device 64
                        (:pipeline (:rect geometry))
                        (:bind-group (:rect geometry))
                        :tracker gpu-budget
                        :label "pool/editor")

      ;; Trail face (view-mvp WP-B2): entry state + server truth pulls.
      ;; The full-screen dev views died with the workspace; the pull lane and
      ;; entry state stay (kernel-material trail — clause-2 keep).
      :!trail-face-state (atom nil)
      :!trail-text   (atom nil)
      :!trail-feed   (atom nil)
      :!trail-bundles (atom {})
      :!ingest-epoch (atom 0)
      :!trail-coverage (atom nil)

      ;; Faces-as-assemblies (framework CONTRACT §5): wear state, the served
      ;; data-context, the wear-time compiled builder, and the cached scene.
      :!face-state    (atom nil)
      :!face-context  (atom nil)
      :!face-compiled (atom nil)
      :!face-scene    (atom nil)
      :!face-list     (atom nil)
      ;; editable-material P3: all worn facet-masters arrive together as
      ;; confirmed projection data; ground never writes them optimistically.
      :!facet-materials (atom nil)

      ;; Mouse tracking (scene pick + dev receipts)
      :!mouse-x (atom 0)
      :!mouse-y (atom 0)}

     :gpu {:device device :ctx ctx :geometry geometry :font-assets font-assets}
     :node node}))
