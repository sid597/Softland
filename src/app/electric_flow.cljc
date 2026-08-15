(ns app.electric-flow
  (:require [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            [app.file-viewer :as fv]
            ;; block-write Lane A · server-only edit entry point deps (the write
            ;; layer, never on the client). face_projection.clj stays READ-ONLY
            ;; (its g12 grep forbids the append form), so the ONE :object/edit
            ;; entry point lives HERE in the artery, beside RecordFaceWear.
            #?@(:clj [[app.server.rama.object-container :as oc]
                      [app.server.rama.object-container.runtime :as ocr]
                      [app.server.rama.util-fns :as util-fns]])
            #?@(:cljs [[app.client.substrate.webgpu.renderer :as editor]
                       [app.client.substrate.webgpu.gpu-budget :as gpu-budget]
                       [app.client.workspace.chrome-runtime :as chrome-runtime]
                       [app.client.workspace.frame-runtime :as frame-runtime]
                       [app.client.workspace.live-atoms :as live-atoms]
                       [app.client.workspace.live-edges :as live-edges]
                       [app.client.workspace.scene-runtime :as scene-runtime]
                       [app.client.workspace.runtime.fonts :as runtime-fonts]
                       [app.client.workspace.runtime :as loop]
                       [global-flow :refer [await-promise]]])))

#?(:clj
   (defn submit-block-edit!
     "block-write Lane A · the ONE server edit entry point (CONTRACT §3 envelope →
      :object/edit on the EXISTING object-container stream path; no new module /
      depot / topology — BW-T1). Callable from Electric server context (the artery
      wiring in Main below; Lane B's reader-face outbox connects at INT).

      `env` carries client-minted fields: :request-id, :idempotency-key (deterministic
      f(request-id), object-key-scoped — nil lets the kernel builder derive it that
      way), :edit-client-id, :edit-seq, :actor (sid's map WITH capability :object/edit),
      :time-ms, :target {:target/kind :target/id}, and :payload {:document-container-id
      :object-key :content-text}. The SERVER stamps :content-hash itself via the kernel's
      own `oc/source-hash` (one hash rule, HERE — no cljs crypto port, no drift; any
      client-supplied hash is IGNORED). Appends with :ack (ack ⇒ event tree complete ⇒
      materialized, CONTRACT §3), reads the DURABLE decision (accepted OR rejected — the
      refusal reason SURFACES, G5), and on an ACCEPT bumps the ingest epoch so the generic
      FacePull re-reads truth. The bump lives in THIS ack continuation ONLY — never a
      render / m/latest path (BW-T9). Returns a plain, wire-safe map (no raw record)."
     ([oc-rt env] (submit-block-edit! oc-rt env util-fns/!ingest-epoch-atom))
     ([oc-rt env !epoch]
      (let [{:keys [request-id idempotency-key edit-client-id edit-seq actor time-ms
                    target payload]} env
            {:keys [document-container-id object-key content-text]} payload
            content-text (str content-text)
            request (oc/object-edit-request
                     (:target/kind target)
                     (:target/id target)
                     content-text
                     {:object-key            object-key
                      :document-container-id document-container-id
                      ;; server stamps the hash (CONTRACT §4) — client hash ignored
                      :content-hash          (oc/source-hash content-text)
                      :request-id            request-id
                      :idempotency-key       idempotency-key
                      :edit-client-id        edit-client-id
                      :edit-seq              edit-seq
                      :actor                 actor
                      :time-ms               time-ms})
            ;; BW-T5 replay detection: a same-request-id re-append hits the audit
            ;; short-circuit (object_container.clj:1818), which ack-returns the PRIOR
            ;; decision VERBATIM — no replay marker on the row. So the honest signal is
            ;; "was this request-id already decided BEFORE this append?" (one cheap point
            ;; read). A same-idempotency-key / different-request-id replay instead carries
            ;; :replayed-from-decision-id; we honour both.
            already-decided? (some? (ocr/read-decision oc-rt request))]
        ;; :ack IS the barrier on this stream topology (materialized on return —
        ;; the deterministic barrier, never a poll).
        (ocr/append-block-edit-request-durably! oc-rt request :ack)
        (let [decision  (ocr/read-decision oc-rt request)
              accepted? (oc/decision-accepted? decision)
              replay?   (or already-decided?
                            (some? (:replayed-from-decision-id decision)))]
          ;; BW-T9: epoch bump ONLY here, after the ack barrier, and ONLY on a real
          ;; truth change (a fresh accept — not a replay, not a rejection). A rejection
          ;; changes no state; the caller reverts the block from the returned reason.
          (when (and accepted? (not replay?)) (swap! !epoch inc))
          {:accepted?  accepted?
           :replay?    replay?
           :reason     (:reason decision)   ;; G5: refusal reason surfaced
           :errors     (:errors decision)
           :status     (:status decision)
           :request-id request-id
           :object-key object-key
           :target-id  (:target/id target)})))))

(e/defn SubmitBlockEdit [env]
  ;; block-write Lane A · the edit write e/defn (CONTRACT §3). Same indirection as
  ;; RecordFaceWear (fv): server-only body, oc-rt resolved from face-ctx, plain-map
  ;; result crosses back. The accept-side epoch bump inside submit-block-edit!
  ;; (BW-T9) drives the existing WatchIngestEpoch → FacePull re-read.
  (e/server (submit-block-edit! (:oc-rt (fv/face-ctx)) env)))









#?(:cljs
   (do
     (defonce !window-debug-hooks-installed? (atom false))

     (defn- install-window-debug-hooks! []
       (when-not @!window-debug-hooks-installed?
         (reset! !window-debug-hooks-installed? true)
         (.addEventListener js/window "error"
           (fn [event]
             (js/console.error "[CLIENT/ERROR]"
                               {:message (.-message event)
                                :filename (.-filename event)
                                :lineno (.-lineno event)
                                :colno (.-colno event)
                                :error (.-error event)})))
         (.addEventListener js/window "unhandledrejection"
           (fn [event]
             (js/console.error "[CLIENT/UNHANDLED-REJECTION]" (.-reason event))))
         (js/console.log "[CLIENT] Installed global window debug hooks")))

     (defn- install-webgpu-debug-hooks! [^js device]
       (when (and device (not (true? (.-__softlandDebugHooksInstalled device))))
         (set! (.-__softlandDebugHooksInstalled device) true)
         (.addEventListener device "uncapturederror"
           (fn [event]
             (js/console.error "[WEBGPU/UNCAUGHT-ERROR]" (.-error event))))
         (-> (.-lost device)
             (.then (fn [info]
                      (js/console.error "[WEBGPU/DEVICE-LOST]"
                                        {:message (.-message info)
                                         :reason (.-reason info)})))
             (.catch (fn [err]
                       (js/console.error "[WEBGPU/DEVICE-LOST-HOOK-FAILED]" err))))
         (js/console.log "[WEBGPU] Installed device debug hooks")))))

(e/defn LoadWebGPU []
  (e/client
    (let [_ (install-window-debug-hooks!)
          gpu js/navigator.gpu
          _ (when-not gpu
              (js/console.error "[BOOT] navigator.gpu unavailable"))
          adapter (e/Task (await-promise (.requestAdapter ^js gpu)))
          device (e/Task (await-promise (.requestDevice ^js adapter)))
          initial-font-data (e/Task (await-promise (runtime-fonts/load-default-font-data-async)))]
      (when (and adapter device initial-font-data)
        (let [format (.getPreferredCanvasFormat ^js gpu)
              adapter-limits (gpu-budget/snapshot-adapter-limits adapter)
              tracker (gpu-budget/create-tracker adapter-limits)]
          (install-webgpu-debug-hooks! device)
          (js/console.log "[BOOT] WebGPU ready"
                          {:format format
                           :font-id (get-in initial-font-data [:font-config :id])
                           :font-backend (get-in initial-font-data [:font-assets :backend])
                           :adapter-limits adapter-limits})
          (merge initial-font-data
                 {:device device
                  :format format
                  :adapter-limits adapter-limits
                  :gpu-budget tracker}))))))

(e/defn Prepare-Geometry [device pipelines render-ops font-assets font-config]
  (e/client
    (let [font-defaults (:defaults font-config)
          font-size (or (:fontSize font-defaults) 19)
          char-width (or (:charWidth font-config) 0.56)
          px-range (or (:pxRange font-defaults) 8)
          sharpness (or (:sharpness font-defaults) 0.0)
          line-height-factor (or (:lineHeight font-defaults) 1.2)
          dpr (or (.-devicePixelRatio js/window) 1)
          snap-step (/ 1 dpr)
          snap (fn [v] (* (Math/round (/ v snap-step)) snap-step))
          line-h (snap (* font-size line-height-factor))]
      (js/console.log "[BOOT] Prepare geometry"
                      {:font-id (:id font-config)
                       :font-backend (:backend font-assets)
                       :render-line-count (count render-ops)
                       :font-size font-size
                       :char-width char-width
                       :px-range px-range
                       :line-height line-h
                       :dpr dpr})
      {:text (editor/update-text-data device (:text-sys pipelines) render-ops font-assets font-size
                                      :px-range px-range
                                      :line-height line-h
                                      :char-width char-width
                                      :snap-step snap-step
                                      :sharpness sharpness)
       :rect (editor/update-rects device (:rect-sys pipelines) [])
       ;; Shadow pools now own runtime shadow uploads; bootstrap only needs the pipeline state.
       :shadow (:shadow-sys pipelines)
       :pipelines pipelines})))

;; ============================================================================
;; SIDEBAR CONSTANTS (used by imperative DOM in loop.cljs)
;; ============================================================================

;; Sidebar constants removed — sidebar now rendered via WebGPU rect tree in loop.cljs

(e/defn main [ring-request]
  (e/client
    (binding [dom/node js/document.body]
      (dom/style {:margin "0" :padding "0"
                  :width "100vw" :height "100vh"
                  :overflow "hidden" :background "#111"
                  :user-select "none"})

      (let [resources (LoadWebGPU)
            ;; CONNECTOR ATOM: the boot-static R1 response. It remains nil
            ;; unless the shared live-atoms flag and real canvas addresses
            ;; both exist.
            !live-edges-data (atom nil)
            ;; Trail face (view-mvp WP-B2): request set by the runtime
            ;; wiring, pull result + epoch pushed back to it.
            !trail-request (atom nil)
            !trail-data (atom nil)
            !ingest-epoch-remote (atom 0)
            ;; Faces-as-assemblies · the ONE generic face artery (CONTRACT §7).
            ;; Request set by face-wiring, data-context pulled back whole.
            !face-request (atom nil)
            !face-data (atom nil)
            !assembly-request (atom nil)
            !assembly-data (atom nil)
            !face-list-request (atom nil)
            !face-list-data (atom nil)
            ;; editable-material P3: one constant, batched facet projection
            ;; through the existing FacePull + ingest-epoch artery.
            !facet-materials-request (atom nil)
            !facet-materials-data (atom nil)
            ;; editable-material P2: console-triggered, read-only inspector
            ;; through the SAME server projection registry + FacePull.
            !material-inspector-request (atom nil)
            !material-inspector-data (atom nil)
            ;; editable-material P5: the interaction table (gesture × facet
            ;; → verb) read through the SAME registry + FacePull.
            !interaction-table-request (atom nil)
            !interaction-table-data (atom nil)
            ;; editable-material P7: the portal — the whole material world
            ;; around ONE pick, joined server-side, ONE data-context.
            !material-portal-request (atom nil)
            !material-portal-data (atom nil)
            !face-wear-outbox (atom nil)
            !face-wear-result (atom nil)
            ;; block-write Lane A · the edit write seam (CONTRACT §3/§5).
            !block-edit-outbox (atom nil)
            !block-edit-result (atom nil)
            ;; block-write INT · the §5 narrowing echo.
            !block-truth-request (atom nil)
            !block-truth-data (atom nil)]
        (reset! !ingest-epoch-remote (fv/WatchIngestEpoch))
        (let [addresses
              (when (live-edges/live-edges-enabled?)
                (live-edges/capture-boot-addresses!
                 (e/watch scene-runtime/!scene-store)))]
          (when (seq addresses)
            (reset! !live-edges-data (fv/LiveEdges addresses))))
        ;; Trail face pull: re-runs when the request changes (face entry,
        ;; expansion clicks, debounced epoch bumps re-stamp the request).
        (let [treq (e/watch !trail-request)]
          (when treq
            (reset! !trail-data
                    (case (:face treq)
                      :text
                      (let [params (second (:address treq))]
                        {:text (fv/TrailText (vec (:targets params))
                                             (dissoc params :targets))})

                      :timeline
                      {:feed (fv/TrailFeed (:window treq)
                                           {:order (:order treq)})
                       :bundles (if (seq (:expanded treq))
                                  (let [b (fv/TrailBundle (vec (:expanded treq)) {})]
                                    (zipmap (:expanded treq) (repeat b)))
                                  {})}
                      nil))))
        ;; Faces-as-assemblies · the ONE generic face pull (CONTRACT §7, trap T8).
        (let [freq (e/watch !face-request)]
          (when freq
            (reset! !face-data (fv/FacePull freq))))
        (let [areq (e/watch !assembly-request)]
          (when areq
            (reset! !assembly-data (fv/FacePull areq))))
        (let [lreq (e/watch !face-list-request)]
          (when lreq
            (reset! !face-list-data (fv/FacePull lreq))))
        (let [mreq (e/watch !facet-materials-request)]
          (when mreq
            (reset! !facet-materials-data (fv/FacePull mreq))))
        (let [ireq (e/watch !material-inspector-request)]
          (when ireq
            (reset! !material-inspector-data (fv/FacePull ireq))))
        (let [breq (e/watch !interaction-table-request)]
          (when breq
            (reset! !interaction-table-data (fv/FacePull breq))))
        ;; P7: ONE pull, every portal answer.
        (let [preq (e/watch !material-portal-request)]
          (when preq
            (reset! !material-portal-data (fv/FacePull preq))))
        ;; W2: the wear write path — outbox value in, result mirrored back.
        (let [wear (e/watch !face-wear-outbox)]
          (when wear
            (reset! !face-wear-result (fv/RecordFaceWear wear))))
        ;; block-write Lane A · the edit write path (CONTRACT §3).
        (let [edit (e/watch !block-edit-outbox)]
          (when edit
            (reset! !block-edit-result (SubmitBlockEdit edit))))
        ;; block-write INT · the single-unit truth pull (§5 narrowing).
        (let [breq (e/watch !block-truth-request)]
          (when breq
            (reset! !block-truth-data (fv/FacePull breq))))
        (when resources
          (let [device (get resources :device)
                format (get resources :format)
                font-manifest (get resources :font-manifest)
                font-config (get resources :font-config)
                font-assets (get resources :font-assets)
                pipelines (e/Task (await-promise (live-atoms/augment-pipelines! resources (editor/create-editor-state resources))))
                _ (frame-runtime/mount!)
                _ (when-let [response (e/watch !live-edges-data)]
                    (live-edges/mount-live-edges! response))
                _ (chrome-runtime/frame-edge!)]
            (js/console.log "[BOOT] Client resources ready"
                            {:font-id (:id font-config)
                             :font-backend (:backend font-assets)
                             :font-manifest-count (count (:fonts font-manifest))
                             :format format})

            (let [geometry (Prepare-Geometry device pipelines [] font-assets font-config)]
              ;; Full-width canvas — the open ground (first-light T9)
              (dom/canvas
                (dom/props {:id "webgpu-canvas"
                            :style {:width "100vw"
                                    :height "100vh"
                                    :display "block"}})
                (let [ctx (.getContext dom/node "webgpu" (clj->js {:alpha true}))]
                  (let [boot-w (max 1 (.-clientWidth dom/node))
                        boot-h (max 1 (.-clientHeight dom/node))
                        boot-dpr (or (.-devicePixelRatio js/window) 1)]
                    (set! (.-width dom/node) (Math/floor (* boot-w boot-dpr)))
                    (set! (.-height dom/node) (Math/floor (* boot-h boot-dpr)))
                    (js/console.log "[BOOT] Configuring WebGPU canvas"
                                    (str "{\"clientWidth\":" (.-clientWidth dom/node)
                                         ",\"clientHeight\":" (.-clientHeight dom/node)
                                         ",\"devicePixelRatio\":" (or (.-devicePixelRatio js/window) 1)
                                         ",\"format\":\"" format "\""
                                         ",\"copyDst\":true}")))
                  (.configure ^js ctx
                    (clj->js {:device device
                              :format format
                              :usage (bit-or (.-RENDER_ATTACHMENT js/GPUTextureUsage)
                                             (.-COPY_DST js/GPUTextureUsage))
                              :alphaMode "premultiplied"}))
                  (js/console.log "[BOOT] Starting runtime loop"
                                  {:font-id (:id font-config)
                                   :font-backend (:backend font-assets)})
                  (e/Task (loop/start-loop! dom/node device ctx geometry font-assets
                                            :font-manifest font-manifest
                                            :gpu-budget (:gpu-budget resources)
                                            :!trail-request !trail-request
                                            :!trail-data !trail-data
                                            :!face-request !face-request
                                            :!face-data !face-data
                                            :!assembly-request !assembly-request
                                            :!assembly-data !assembly-data
                                            :!face-list-request !face-list-request
                                            :!face-list-data !face-list-data
                                            :!facet-materials-request !facet-materials-request
                                            :!facet-materials-data !facet-materials-data
                                            :!material-inspector-request !material-inspector-request
                                            :!material-inspector-data !material-inspector-data
                                            :!interaction-table-request !interaction-table-request
                                            :!interaction-table-data !interaction-table-data
                                            :!material-portal-request !material-portal-request
                                            :!material-portal-data !material-portal-data
                                            :!face-wear-outbox !face-wear-outbox
                                            :!face-wear-result !face-wear-result
                                            :!block-edit-outbox !block-edit-outbox
                                            :!block-edit-result !block-edit-result
                                            :!block-truth-request !block-truth-request
                                            :!block-truth-data !block-truth-data
                                            :!ingest-epoch-remote !ingest-epoch-remote)))))))))))
