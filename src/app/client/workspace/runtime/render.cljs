(ns app.client.workspace.runtime.render
  "Render consumer: derived flow assembly, world snapshot, GPU upload diffing, draw."
  (:require [missionary.core :as m]
            [app.client.substrate.frame-inputs :as frame-inputs]
            [app.client.substrate.frame-scheduler :as frame-scheduler]
            [app.client.substrate.webgpu.renderer :as editor]
            [app.client.workspace.scene-runtime :as scene-rt] ;; scene-substrate P3a/P3b
            [app.client.substrate.webgpu.buffer-pool :as pool]
            [app.client.substrate.webgpu.gpu-budget :as gpu-budget]
            [app.client.workspace.events :refer [maybe-snap]]
            [app.client.workspace.ground :as ground]
            [app.client.workspace.frame-runtime :as frame-runtime]
            [app.client.workspace.region3d-runtime :as region3d-runtime]
            [app.client.workspace.runtime.workspace-actions :as ws]
            [app.client.workspace.sidebar :refer [cmd-panel-h status-bar-h]]
            [app.client.workspace.editor-compute :refer [<fold-state <bracket-match <editor-rects+sidebar build-main-face!]]
            [app.client.workspace.combined-text :refer [<combined-text-ops]]
            [app.client.workspace.cmd-panel :refer [<cmd-panel-rects]]
            [app.client.workspace.settings-view :refer [<settings-panel-rects <settings-panel-text]]
            [app.client.workflows.dg-flow :as dg]))

(def ^:private use-persistent-render-target? false)
(defonce ^:private !due-deadline-consumer (atom nil))
(defonce ^:private !session-layout-provider (atom nil))

(defn install-due-deadline-consumer!
  "Install T2's nil-default sink hook. It observes the ids returned by the
   scheduler after decide and before this frame's encode branch."
  [consumer]
  (reset! !due-deadline-consumer consumer)
  true)

(defn install-session-layout-provider! [provider]
  (reset! !session-layout-provider provider)
  true)

(defn- render-debug! [label data]
  (let [payload (clj->js data)]
    (js/console.log label payload)
    (js/console.log (str label " JSON " (js/JSON.stringify payload)))))

(defn- selection-active? []
  (pos? (or (some-> (aget js/globalThis "__softlandChromeReceipt")
                     (aget "selection-census")
                     (aget "selected"))
            0)))

(defn- publish-scheduler-receipt! [state]
  (aset js/globalThis "__softlandFrameSchedulerReceipt"
        (clj->js (frame-scheduler/receipt state))))

(defn- text-op-seq [texts]
  (mapcat #(if (vector? %) % [%]) (or texts [])))

(defn- slot-layout-token [texts]
  (mapv (fn [op]
          [(get-in op [:layout-result :layout/id])
           (:layout-line-id op)
           (:paint-source-range op)])
        (text-op-seq texts)))

(defn- slot-paint-token [texts]
  ;; Fingerprint only values consumed by the text instance packers. Material
  ;; provenance, bindings, and attention stamps may rebuild the semantic tree
  ;; without changing pixels; keying on the entire op map turned those
  ;; non-paint revisions into fake GPU work (G4 j/k).
  (mapv #(select-keys % [:text :from :to :x :y :size
                         :r :g :b :a :container-idx])
        (text-op-seq texts)))

(defn- slot-paint-cause [texts fallback]
  (or (some :paint/cause (text-op-seq texts)) fallback))

(defn- slot-layout-cause [texts fallback]
  (or (some #(get-in % [:layout-result :layout/cause]) (text-op-seq texts))
      fallback))

(defn- reconcile-slot-text-geos!
  "scene-substrate P3b Rung 2 (G8): keep ONE isolated text geo per store slot,
   separate from the monolithic content geo. For each vi in `text-by-vi`:
   - unchanged text (identical? to last frame) AND no reclone → REUSE the geo,
     zero GPU writes (trap T5 skip);
   - changed text → reshape ONLY that slot's geo via update-text-data;
   - new vi, or `reclone?` (font/backend change → the content clone-parent is
     fresh) → clone a new geo off the content system (shared pipeline/bind-group/
     camera/containers-buffer/font — NOT a second text path, T12) and shape it.
   Destroys geos for vanished vis (and the stale ones a reclone replaces).
   Returns [geos-map write-count]; write-count is the G8 receipt (how many slot
   geos reshaped this frame — a CONTENT-geo edit must never bump it)."
  [device content-geo font-assets prev-geos text-by-vi reclone? reclone-cause
   font-size px-range line-h char-width snap-step sharpness]
  (let [shape! (fn [geo texts]
                 (editor/update-text-data device geo (vec texts) font-assets font-size
                                          :px-range px-range :line-height line-h
                                          :char-width char-width :snap-step snap-step
                                          :sharpness sharpness :surface :ground))
        {:keys [geos writes]}
        (reduce-kv
          (fn [acc vi texts]
            (let [prev (get prev-geos vi)]
              (if (and prev (not reclone?) (identical? texts (:text prev)))
                ;; identity fast path: the exact op vector from last frame ⇒
                ;; tokens are unchanged by construction — skip computing them.
                ;; A camera-only frame must cost ~0 here (perf receipt
                ;; 2026-08-08: token recompute over unchanged slots was the
                ;; whole text-gpu bucket, ~26ms/frame on an iPhone).
                (update acc :geos assoc vi prev)
                (let [layout-token (slot-layout-token texts)
                      paint-token (slot-paint-token texts)
                      same-layout? (and prev (= layout-token (:layout-token prev)))
                      same-paint? (and prev (= paint-token (:paint-token prev)))]
                  (if (and prev (not reclone?) same-layout? same-paint?)
                    ;; value fast path: fresh op objects, unchanged content
                    (update acc :geos assoc vi prev)
                (let [base (if (and prev (not reclone?))
                             (:geo prev)                    ; evolve this slot's geo in place
                             (do (when (and prev reclone?)  ; stale clone (old font) → free
                                 (editor/destroy-text-system! (:geo prev)))
                                 (editor/clone-text-system device content-geo 256)))
                      old-buffer (:instance-buffer base)
                      geo  (shape! base texts)
                      capacity-grown? (and prev (not reclone?)
                                            (not (identical? old-buffer
                                                             (:instance-buffer geo))))
                      lifecycle (cond
                                  (nil? prev) :fresh
                                  reclone? :recloned
                                  capacity-grown? :capacity-grown
                                  :else :in-place)
                      cause (cond
                              reclone? reclone-cause
                              (not same-layout?)
                              (slot-layout-cause texts :slot-text-change)
                              :else (slot-paint-cause texts :other))]
                  (ground/record-shaping-counter! [:paint-repacks cause])
                  (ground/record-shaping-counter! [:slot-text-writes])
                  (ground/record-shaping-counter! [:geo lifecycle])
                  (when (and prev reclone?)
                    (ground/record-shaping-counter! [:geo :destroyed]))
                  (when-let [dirty-cause
                             (cond
                               (nil? prev) :entry
                               (not same-layout?) :slot-text
                               reclone? :other
                               ;; Selection/provenance changes may rebuild the
                               ;; semantic tree without changing text paint.
                               ;; Hover is an attention rectangle outside the
                               ;; block glyph paint token; any hover-attributed
                               ;; text pack is therefore a counter-visible
                               ;; violation, never a dirty-text lifecycle.
                               :else nil)]
                    (ground/record-shaping-counter! [:dirty dirty-cause]))
                  (-> acc
                      (update :geos assoc vi {:geo geo :text texts
                                              :layout-token layout-token
                                              :paint-token paint-token})
                      (update :writes inc))))))))
          {:geos {} :writes 0}
          (or text-by-vi {}))]
    (doseq [[vi prev] prev-geos]
      (when-not (contains? geos vi)
        (editor/destroy-text-system! (:geo prev))
        (ground/record-shaping-counter! [:geo :destroyed])
        (ground/record-shaping-counter! [:dirty :exit])
        (ground/evict-layouts-for-slot! vi)))
    [(if (frame-inputs/input-value-same? geos prev-geos) prev-geos geos)
     writes]))

(defn render-consumer
  "Missionary consumer: assemble derived flows, build world snapshot, diff-upload to GPU, draw on RAF."
  [{:keys [!editor-doc !cmd-panel !ai-provider !agent-output !agent-scroll-y !scroll-y
           !viewport !settings !active-font !current-file !effective-local-world !flow-state !collapsed-groups
           !hovered-row-idx !drag-state !sidebar-truth !sidebar-overlay !sidebar-ui !sidebar-visible !sidebar-scene !extract-preview
           !shimmer-phase !trail-collapsed !active-pane !scroll-x !chat-scroll-y !chat-input
           !focus !run-scroll-y !detail-scroll-y !eval-result !caret-visible !folded-lines
           !font-manifest !font-assets !text-geo !gpu-budget !cmd-rect-sys !settings-rect-sys
           !sidebar-pool !editor-pool !editor-shadow-pool !sidebar-shadow-pool
           !trail-face-state !trail-face-scene !trail-text !trail-feed !trail-bundles
           !trail-coverage
           !face-state !face-context !face-compiled !face-scene !face-list]
    :as atoms}
   {:keys [layout-x layout-y gutter-w]}
   {:keys [device ctx geometry]}
   {:keys [tokenize-fn layout-fn detect-folds-fn find-bracket-fn]}
   >raf]
  (let [;; Derived flows
        <fold-data    (<fold-state !editor-doc !folded-lines detect-folds-fn)
        <bracket-data (<bracket-match !editor-doc find-bracket-fn)

        <text-data (<combined-text-ops
                      !editor-doc !cmd-panel !ai-provider !agent-output !agent-scroll-y !scroll-y !viewport !settings !active-font
                      !current-file !effective-local-world
                      tokenize-fn layout-fn
                      <fold-data
                      !flow-state !collapsed-groups !hovered-row-idx !drag-state
                      !sidebar-visible !sidebar-scene !trail-face-scene !extract-preview
                      !shimmer-phase !trail-collapsed !active-pane !scroll-x !chat-scroll-y !chat-input !focus !run-scroll-y !detail-scroll-y
                      dg/compute-ticket-list-text-ops dg/compute-run-text-ops dg/offset-text-ops
                      layout-x layout-y cmd-panel-h status-bar-h)

        ;; Split: editor rects (content only) + sidebar rects (for pool) +
        ;; the main-face tree flow (first-light P1 — feeds the store slot)
        {<editor-rect-flow :<editor-rects
         <sidebar-flow     :<sidebar
         <face-main-flow   :<face-main}
        (<editor-rects+sidebar
          !editor-doc !eval-result !caret-visible !focus
          !settings !active-font !viewport
          <fold-data <bracket-data
          !flow-state !scroll-y !collapsed-groups !hovered-row-idx !drag-state
          !sidebar-truth !sidebar-overlay !sidebar-ui !sidebar-visible !current-file !effective-local-world !sidebar-scene !extract-preview !agent-output !face-list
          !shimmer-phase !trail-collapsed !active-pane !scroll-x !chat-scroll-y !chat-input !run-scroll-y !detail-scroll-y
          !trail-face-state !trail-face-scene !trail-text !trail-feed !trail-bundles !trail-coverage
          !face-state !face-context !face-compiled !face-scene
          dg/compute-ticket-list-rects dg/compute-run-rects dg/offset-rects dg/offset-shadows
          layout-x layout-y gutter-w)

        <editor-rect-data <editor-rect-flow

        <cmd-rect-data (<cmd-panel-rects
                          !cmd-panel !focus !caret-visible !scroll-y !viewport
                          !settings !active-font
                          !ai-provider !agent-output !sidebar-visible
                          !effective-local-world
                          cmd-panel-h status-bar-h)

        <settings-rect-data (<settings-panel-rects !settings !focus !viewport !scroll-y !font-manifest)
        <settings-text-data (<settings-panel-text !settings !viewport !scroll-y !font-manifest)

        ;; ── scene-substrate P3a: the store's GPU contribution + composed
        ;; container transforms. TWO independent single-source flows (store /
        ;; registry) combined ONCE in the world snapshot — no diamond (T3).
        <store-frame (scene-rt/<store-frame)
        <effective   (scene-rt/<effective)
        <frame-registry (scene-rt/<frame-registry)

        ;; World snapshot (now includes sidebar data for pool updates)
        <world-snapshot (m/latest
                          (fn [text-data editor-rect-data sidebar-data
                               cmd-rects settings-rects settings-text
                               viewport scroll-y cmd-panel settings active-font agent-output
                               local-world store-frame effective frame-registry
                               region3d-session]
                            {:text-data text-data
                             :store-frame store-frame ;; scene-substrate P3a
                             :effective   effective   ;; scene-substrate P3a
                             :frame-registry frame-registry
                             :region3d-session region3d-session
                             ;; P3b finding #1: the store composites ONLY in face
                             ;; mode — a defensive gate mirroring the click
                             ;; dispatch (mouse.cljs), so a slot that outlives its
                             ;; face view (mode switch that never nils face-scene)
                             ;; cannot paint over the editor/file workspace.
                             :face-mode?  (ws/local-world-face-assembly? local-world)
                             :editor-rect-data editor-rect-data
                             :sidebar-data sidebar-data
                             :cmd-rects cmd-rects
                             :settings-rects settings-rects
                             :settings-text settings-text
                             :viewport viewport
                             :scroll-y scroll-y
                             :cmd-visible (or (:visible cmd-panel)
                                              (ws/local-world-file-workspace? local-world)
                                              (ws/local-world-flow? local-world))
                             :agent-visible (some? (:status agent-output))
                             :settings-visible (:visible settings)
                             :font-size (:font-size settings)
                             :px-range (:px-range settings)
                             :line-height (:line-height settings)
                             :sharpness (:sharpness settings)
                             :snap-to-pixel? (:snap-to-pixel? settings)
                             :show-diagnostics? (:show-diagnostics? settings)
                             :char-width (:char-width active-font)})
                          <text-data
                          <editor-rect-data
                          <sidebar-flow
                          <cmd-rect-data
                          <settings-rect-data
                          <settings-text-data
                          (m/watch !viewport)
                          (m/watch !scroll-y)
                          (m/watch !cmd-panel)
                          (m/watch !settings)
                          (m/watch !active-font)
                          (m/watch !agent-output)
                          (m/watch !effective-local-world)
                          <store-frame   ;; scene-substrate P3a
                          <effective     ;; scene-substrate P3a
                          <frame-registry
                          (m/watch region3d-runtime/!session))]

    ;; Two joined consumers: the main-face slot edge + the RAF render pulse.
    (m/join vector
      ;; ── first-light P1: the main-face slot consumer ──
      ;; A DEDICATED edge (T4: store mutations at edges only), deliberately NOT
      ;; the RAF edge — and the build runs in a COALESCING MICROTASK, not
      ;; synchronously in the propagation. Measured (G1 drill): the in-combine
      ;; build put ~7-15ms into every keystroke/decision/truth turn and pushed
      ;; narrow echo p50 20→52ms; a microtask drains before the browser
      ;; paints (slot lands the SAME frame — no store-lag frame) while the
      ;; envelope dispatch and echo processing run unblocked. Bursts within
      ;; one turn coalesce to one build (last bundle wins).
      (let [!pending (atom nil)
            !queued? (atom false)]
        (m/reduce
          (fn [_ bundle]
            (reset! !pending bundle)
            (when (compare-and-set! !queued? false true)
              (js/queueMicrotask
                (fn []
                  (reset! !queued? false)
                  (when-let [b @!pending]
                    ;; first-light P2b: on the open ground the outline-face
                    ;; TREE never renders — the served projection reconciles
                    ;; into per-block slots (the tip pattern is DEAD).
                    (if (ground/ground-active?)
                      (ground/on-face-bundle! b)
                      (build-main-face! b))))))
            nil)
          nil <face-main-flow))

      ;; Render pulse: sample world on each RAF tick
      (m/reduce
      (fn [prev-state [world frame-time]]
        ;; first-light P2b, quarantined by SEAM-STEP1: camera is a sink-local
        ;; mosaic input. It is dereferenced per RAF and never enters derivation.
        (let [ground-camera @ground/!camera
              world-changed? (not (identical? world (:prev-world prev-state)))
              camera-moved? (not= ground-camera (:prev-ground-camera prev-state))
              dirty-rect-pending? (some? (:pending-dirty-rect prev-state))
              logical-time (frame-scheduler/clock-time frame-time)
              _ (frame-runtime/sync-pulse-deadline! logical-time)
              causes (frame-scheduler/derive-causes
                      {:world-changed? world-changed?
                       :camera-moved? camera-moved?
                       :dirty-rect-pending? dirty-rect-pending?})
              scheduler-step (frame-scheduler/decide-at!
                              (:scheduler-state prev-state)
                              logical-time causes
                              (:last-plan-hash prev-state))
              _ (when-let [consume @!due-deadline-consumer]
                  (consume (:due-deadlines scheduler-step)))
              _ (publish-scheduler-receipt! (:state scheduler-step))]
        (if-not (:encode? scheduler-step)
          (assoc prev-state :scheduler-state (:state scheduler-step))

          (let [frame-idx (inc (or (:frame-idx prev-state) 0))
                _ (ground/record-shaping-counter! [:raf-frames])
                raf-t0 (js/performance.now)
                {:keys [text-data editor-rect-data sidebar-data cmd-rects settings-rects settings-text
                        viewport scroll-y cmd-visible agent-visible settings-visible
                        font-size px-range line-height sharpness char-width
                        snap-to-pixel? show-diagnostics? region3d-session]} world
                editor-rects   (:rects editor-rect-data)
                editor-shadows (:shadows editor-rect-data)

                ;; ── scene-substrate P3b: store contribution + echo fan-out ──
                        store-frame          (:store-frame world)
                        effective            (:effective world)
                        frame-registry       (:frame-registry world)
                face-mode?           (:face-mode? world)
                ground?              (ground/ground-active?)
                gcam                 (or ground-camera
                                         {:x 0.0 :y 0.0 :zoom 1.0})
                store-frame-changed? (not (identical? store-frame (:prev-store-frame prev-state)))
                _ (when store-frame-changed?
                    (ground/record-shaping-counter! [:store-frame-execs]))
                _ (when camera-moved?
                    (ground/record-shaping-counter! [:dirty :camera]))
                _ (when (and store-frame-changed?
                             (= (set (or (:ordered-vis store-frame) []))
                                (set (or (get-in prev-state [:prev-store-frame :ordered-vis]) [])))
                             (not= (:ordered-vis store-frame)
                                   (get-in prev-state [:prev-store-frame :ordered-vis])))
                    (ground/record-shaping-counter! [:dirty :order]))
                ;; P3b finding #1: gate the store's compositing on face mode. When
                ;; not in a face view the store contributes NOTHING (nil), so an
                ;; orphaned slot can never paint over the editor. store-frame-
                ;; changed? still fires on the clearing transition, so leaving
                ;; face mode re-uploads content WITHOUT the store text (orphan
                ;; removal) exactly once, then settles.
                store-text-by-vi     (when face-mode? (:text-by-vi store-frame))
                store-rects          (when face-mode? (:rects store-frame))
                store-shadows        (when face-mode? (:shadows store-frame))
                containers-buffer    (:containers-buffer (:pipelines geometry))
                ;; Echo fan-out (G7, Rung-1 form): copies rebuild from
                ;; @!face-context, so the refresh keys on FACE-CONTEXT identity —
                ;; not face-scene, whose m/latest struct includes the overlay-
                ;; merged edit state and therefore flips on EVERY keystroke/caret
                ;; move (gate-review F1, 2026-07-13: keying on face-scene made
                ;; every open copy full-repack per keystroke for zero visual
                ;; change). face-context only changes on a real projection change
                ;; (the debounced FacePull), which is the only event that can
                ;; alter copy content until P3c threads the overlay lane into the
                ;; per-vi build. ONE deref (wave-2 finding #2: tree AND addresses
                ;; build from the SAME sampled projection). face-scene nil (worn
                ;; face off) → clear the spawned copies. Edge-only mutation (T4).
                face-scene           @!face-scene
                face-scene-changed?  (not (identical? face-scene (:prev-face-scene prev-state)))
                face-context         @!face-context
                face-context-changed? (not (identical? face-context (:prev-face-context prev-state)))
                _ (cond
                    ;; left face mode by ANY path (mode switch, /face off) → clear
                    ;; every spawned instance (finding #1: no orphan, no leak).
                    ;; first-light P1: RE-CHECK the LIVE mode before the
                    ;; destructive close — the sampled world can lag one frame
                    ;; behind the atoms, and on face-mode ENTRY the main-face
                    ;; slot lands via its own consumer edge before the world
                    ;; catches up; a stale-world close here would blank the
                    ;; face until the next tree rebuild.
                    (and (not face-mode?)
                         (not (ws/local-world-face-assembly? @!effective-local-world))
                         (scene-rt/any-slots?))
                    (scene-rt/close-all-slots!)
                    ;; worn face just turned off → the copies' projection died
                    ;; with it; clear them.
                    (and face-scene-changed? (nil? face-scene) (scene-rt/any-slots?))
                    (scene-rt/close-all-slots!)
                    ;; projection changed while a face is worn → echo fan-out.
                    (and face-scene face-context face-context-changed?
                         (scene-rt/any-slots?))
                    (scene-rt/refresh-all-slots! face-context))
                ;; Upload composed container transforms only when they changed
                ;; (drag/spawn); instance buffers untouched (identical? skip).
                _ (when-not (identical? effective (:prev-effective prev-state))
                    (editor/write-containers! device containers-buffer effective))

                ;; Differential sidebar pool update — keyed by identity (Phase 5)
                sidebar-rects (or (:rects sidebar-data) [])
                _sidebar-diff (when-not (identical? sidebar-data (:prev-sidebar-data prev-state))
                                (let [has-ids? (some :id sidebar-rects)
                                      t0 (js/performance.now)
                                      result (if has-ids?
                                               (pool/keyed-diff-update-pool! !sidebar-pool sidebar-rects)
                                               (do (pool/batch-update-pool! !sidebar-pool sidebar-rects) nil))
                                      t1 (js/performance.now)]
                                  (when (and result (pos? (+ (:added result) (:updated result) (:freed result))))
                                    (js/console.log "[SIDEBAR-POOL] keyed-diff:"
                                                    (.toFixed (- t1 t0) 2) "ms |"
                                                    "added:" (:added result)
                                                    "updated:" (:updated result)
                                                    "freed:" (:freed result)
                                                    "writes:" (:total-writes result)
                                                    "rects:" (count sidebar-rects)))))
                dpr (:dpr viewport)
                snap? (not (false? snap-to-pixel?))
                line-h (maybe-snap (* font-size line-height) dpr snap?)
                snap-step (when snap? (/ 1 (or dpr 1)))

                gpu-tracker @!gpu-budget
                font-assets @!font-assets
                prev-font-id (:prev-font-id prev-state)
                prev-font-backend (:prev-font-backend prev-state)
                font-changed? (or (not= (:id font-assets) prev-font-id)
                                  (not= (:backend font-assets) prev-font-backend))
                backend-changed? (not= (:backend font-assets) prev-font-backend)

                ;; Font asset update — same backend updates resources in-place,
                ;; backend changes rebuild the content system, then re-clone chrome.
                [updated-content-geo updated-chrome-geo]
                (if font-changed?
                  (if backend-changed?
                    (let [old-content-geo (:content-text-geo prev-state)
                          old-chrome-geo (:chrome-text-geo prev-state)
                          chrome-capacity (max 1
                                              (quot (.-size ^js (:instance-buffer old-chrome-geo))
                                                    (:instance-stride old-chrome-geo)))
                          _ (js/console.log "[RENDER] Recreating text systems for backend:" (name (:backend font-assets)))
                          _ (editor/destroy-text-system! old-chrome-geo)
                          new-content-geo (editor/recreate-text-system device
                                                                       (:format (:pipelines geometry))
                                                                       old-content-geo
                                                                       font-assets)]
                      [new-content-geo
                       (editor/clone-text-system device new-content-geo chrome-capacity)])
                    (do (js/console.log "[RENDER] Updating font assets for:" (:id font-assets) "backend=" (name (:backend font-assets)))
                        (let [new-content-geo (editor/update-font-assets device (:content-text-geo prev-state) font-assets)]
                          [new-content-geo
                           (editor/share-font-resources (:chrome-text-geo prev-state) new-content-geo)])))
                  [(:content-text-geo prev-state) (:chrome-text-geo prev-state)])

                ;; Resize render target if viewport changed (Phase 6E)
                ;; Render target is always at physical pixels (CSS * dpr)
                prev-rt (:render-target prev-state)
                phys-w (Math/floor (* (:width viewport) dpr))
                phys-h (Math/floor (* (:height viewport) dpr))
                rt-resized? (and use-persistent-render-target?
                                 (or (not= phys-w (:width prev-rt))
                                     (not= phys-h (:height prev-rt))))
                render-target (when use-persistent-render-target?
                                (if rt-resized?
                                  (editor/create-render-target device
                                    phys-w phys-h
                                    (:format (:pipelines geometry))
                                    :tracker gpu-tracker
                                    :scene-color (:scene-color (:pipelines geometry))
                                    :previous prev-rt)
                                  prev-rt))

                ;; Common rendering settings check
                settings-same? (and (= font-size (:prev-font-size prev-state))
                                    (= px-range (:prev-px-range prev-state))
                                    (= line-h (:prev-line-height prev-state))
                                    (= sharpness (:prev-sharpness prev-state))
                                    (= char-width (:prev-char-width prev-state))
                                    (= snap-step (:prev-snap-step prev-state))
                                    (not font-changed?))

                ;; ── Content text (editor + sidebar — the bulk) ──
                content-ops (:content-ops text-data)
                ;; scene-substrate P3b Rung 2: store slots' text NO LONGER rides
                ;; the content geo — each slot owns an ISOLATED text geo (G8), so
                ;; content-same? drops the store-frame dependency and a face edit
                ;; never reshapes the content geo (the G8 receipt below proves it).
                content-same? (and (identical? content-ops (:prev-content-ops prev-state))
                                   settings-same?)

                raf-t1 (js/performance.now)
                new-content-geo (if content-same?
                                  updated-content-geo
                                  (editor/update-text-data device updated-content-geo
                                                           (vec content-ops)
                                                           font-assets font-size
                                                           :px-range px-range
                                                           :line-height line-h
                                                           :char-width char-width
                                                           :snap-step snap-step
                                                           :sharpness sharpness
                                                           :surface :combined-text-ops))

                ;; ── P3b Rung 2: per-slot isolated text geos (G8) ──
                ;; Reconcile one text geo per store slot off the content system.
                ;; reclone on a font change (the content clone-parent is fresh).
                prev-slot-geos (:slot-text-geos prev-state)
                rec-t0 (js/performance.now)
                [slot-text-geos slot-text-writes]
                (reconcile-slot-text-geos! device new-content-geo font-assets
                                           prev-slot-geos store-text-by-vi
                                           (boolean font-changed?)
                                           (if backend-changed? :other
                                               :provider-change)
                                           font-size px-range line-h char-width snap-step sharpness)
                rec-ms (- (js/performance.now) rec-t0)
                _ (when (pos? slot-text-writes)
                    (js/console.log "[SCENE-FACES/G8] slot text geos reshaped:" slot-text-writes
                                    "| content geo reshaped this frame?:" (not content-same?)
                                    "| live slots:" (count store-text-by-vi)))
                prev-store-frame (:prev-store-frame prev-state)
                prev-extra-text-geos (:extra-text-geos prev-state)
                extra-sources-same?
                (and (identical? slot-text-geos (:slot-text-geos prev-state))
                     (identical? (:ordered-vis store-frame)
                                 (:ordered-vis prev-store-frame))
                     (identical? (:text-clips-by-vi store-frame)
                                 (:text-clips-by-vi prev-store-frame))
                     (identical? (:order-by-vi store-frame)
                                 (:order-by-vi prev-store-frame)))
                extra-text-geos
                (if extra-sources-same?
                  prev-extra-text-geos
                  (let [candidate
                        (into []
                              (keep (fn [vi]
                                      (when-let [slot-geo (get slot-text-geos vi)]
                                        {:vi vi :geo (:geo slot-geo)
                                         :text-clips
                                         (get-in store-frame
                                                 [:text-clips-by-vi vi])
                                         :order (get-in store-frame
                                                        [:order-by-vi vi])})))
                              (:ordered-vis store-frame))]
                    (if (frame-inputs/input-value-same?
                         candidate prev-extra-text-geos)
                      prev-extra-text-geos candidate)))

                ;; ── Chrome text (cmd + agent + status + settings + diagnostics) ──
                chrome-ops (:chrome-ops text-data)
                settings-lines (when settings-visible (when settings-text [settings-text]))
                diagnostics-line (when show-diagnostics?
                                   (let [diag-x (maybe-snap 16 dpr snap?)
                                         diag-y (maybe-snap (+ scroll-y 20) dpr snap?)
                                         diag-size (max 10 (- font-size 2))
                                         backend-name (name (:backend font-assets))
                                         atlas-size (get-in font-assets [:atlas :atlas :size])
                                         curve-tex (get-in font-assets [:slug :meta :curveTexture])
                                         band-tex (get-in font-assets [:slug :meta :bandTexture])
                                         slug-upload-ready? (and (= :slug (:backend font-assets))
                                                                 (:curve-texture updated-content-geo)
                                                                 (:band-texture updated-content-geo))
                                         font-name (:name @!active-font)
                                         gpu-line (gpu-budget/summary-line gpu-tracker)
                                         diag-text (str "font: " (or font-name (:id font-assets)) "\n"
                                                        "backend: " backend-name "\n"
                                                        "dpr: " dpr "  snap: " (if snap? "on" "off") "\n"
                                                        "pxRange: " px-range "  sharp: " sharpness "\n"
                                                        (if (= :slug (:backend font-assets))
                                                          (str "curve: " (:width curve-tex) "x" (:height curve-tex)
                                                               "  band: " (:width band-tex) "x" (:height band-tex)
                                                               "  upload: " (if slug-upload-ready? "ready" "missing"))
                                                          (str "atlas: " atlas-size))
                                                        "  charW: " char-width
                                                        (when gpu-line
                                                          (str "\n" gpu-line)))]
                                     [{:text diag-text :type :comment
                                       :from 0 :to (count diag-text)
                                       :x diag-x :y diag-y :size diag-size
                                       :r 0.7 :g 0.7 :b 0.7 :a 1.0}]))
                chrome-base-count (count (or chrome-ops []))
                full-chrome-ops (vec (concat (or chrome-ops [])
                                            (or settings-lines [])
                                            diagnostics-line))
                chrome-same? (and (identical? chrome-ops (:prev-chrome-ops prev-state))
                                  (identical? settings-text (:prev-settings-text prev-state))
                                  (= settings-visible (:prev-settings-visible prev-state))
                                  (= show-diagnostics? (:prev-show-diagnostics prev-state))
                                  (= scroll-y (:prev-scroll-y prev-state))
                                  settings-same?)

                new-chrome-geo (if chrome-same?
                                 updated-chrome-geo
                                 (editor/update-text-data device updated-chrome-geo
                                                          full-chrome-ops font-assets font-size
                                                          :px-range px-range
                                                          :line-height line-h
                                                          :char-width char-width
                                                          :snap-step snap-step
                                                          :sharpness sharpness
                                                          :surface :combined-text-ops))

                raf-t2 (js/performance.now)
                editor-line-count (:editor-line-count text-data)
                settings-line-count (count (or settings-lines []))
                diagnostics-line-index (when diagnostics-line
                                         (+ chrome-base-count settings-line-count))

                ;; Content geo for hit-testing (mouse uses line-mapping + editor-line-count)
                new-text-geo (assoc new-content-geo
                                    :line-mapping (:line-mapping text-data)
                                    :editor-line-count editor-line-count)

                ;; Differential editor pool update — ordered keyed diff (Phase 6A)
                ;; scene-substrate P3a: store slots' rects ride the SAME pool,
                ;; each stamped with its :container-idx (pack-rect honors it).
                _editor-pool-diff (when (or (not (identical? editor-rects (:prev-editor-rects prev-state)))
                                            store-frame-changed?)
                                    (let [t0 (js/performance.now)
                                          result (pool/ordered-diff-update-pool! !editor-pool
                                                   (cond-> (vec editor-rects)
                                                     (seq store-rects) (into store-rects)))
                                          t1 (js/performance.now)]
                                      (when (pos? (:total-writes result))
                                        (js/console.log "[EDITOR-POOL] ordered-diff:"
                                                        (.toFixed (- t1 t0) 2) "ms |"
                                                        "added:" (:added result)
                                                        "updated:" (:updated result)
                                                        "freed:" (:freed result)
                                                        "writes:" (:total-writes result)
                                                        "rects:" (count editor-rects)))))

                ;; Per-source shadow pools (Phase 6C) — independent diffs, no cross-source shifts
                _editor-shadow-diff
                (when (or (not (identical? editor-shadows (:prev-editor-shadows prev-state)))
                          store-frame-changed?) ;; scene-substrate P3a: store shadows ride this pool
                  (let [shadows (cond-> (vec (or editor-shadows []))
                                  (seq store-shadows) (into store-shadows))
                        t0 (js/performance.now)
                        writes (pool/batch-update-pool! !editor-shadow-pool shadows)
                        t1 (js/performance.now)]
                    (when (pos? writes)
                      (js/console.log "[EDITOR-SHADOW-POOL] batch-diff:"
                                      (.toFixed (- t1 t0) 2) "ms |"
                                      "writes:" writes
                                      "shadows:" (count shadows)))))
                _sidebar-shadow-diff
                (when-not (identical? sidebar-data (:prev-sidebar-data prev-state))
                  (let [shadows (or (:shadows sidebar-data) [])
                        t0 (js/performance.now)
                        writes (pool/batch-update-pool! !sidebar-shadow-pool shadows)
                        t1 (js/performance.now)]
                    (when (pos? writes)
                      (js/console.log "[SIDEBAR-SHADOW-POOL] batch-diff:"
                                      (.toFixed (- t1 t0) 2) "ms |"
                                      "writes:" writes
                                      "shadows:" (count shadows)))))

                new-cmd-sys (if (not (identical? cmd-rects (:prev-cmd-rects prev-state)))
                              (editor/update-rects device
                                                   (or (:cmd-rect-sys prev-state) @!cmd-rect-sys)
                                                   (or cmd-rects []))
                              (:cmd-rect-sys prev-state))

                new-settings-sys (if (not (identical? settings-rects (:prev-settings-rects prev-state)))
                                   (editor/update-rects device
                                                        (or (:settings-rect-sys prev-state) @!settings-rect-sys)
                                                        (or settings-rects []))
                                   (:settings-rect-sys prev-state))]

            (reset! !text-geo new-text-geo)

            ;; ── Dirty-present metadata (Phase 6E) ─────────────────────
            ;; Collect which subsystems changed to compute dirty rect for scissored redraw
            ;; All rects in PHYSICAL pixels (CSS * dpr) for setScissorRect
            (let [sidebar-dirty? (not (identical? sidebar-data (:prev-sidebar-data prev-state)))
                  editor-rects-dirty? (not (identical? editor-rects (:prev-editor-rects prev-state)))
                  editor-shadows-dirty? (not (identical? editor-shadows (:prev-editor-shadows prev-state)))
                  cmd-dirty? (not (identical? cmd-rects (:prev-cmd-rects prev-state)))
                  settings-dirty? (not (identical? settings-rects (:prev-settings-rects prev-state)))
                  first-frame? (nil? (:prev-world prev-state))
                  ;; Physical pixel dimensions
                  pw phys-w
                  ph phys-h
                  sb-w (* dpr (or (:sb-w (:line-mapping text-data)) 0))
                  chrome-h (* dpr (+ cmd-panel-h status-bar-h))
                  ;; Per-subsystem screen regions (physical pixels)
                  ;; Sidebar: left strip
                  ;; Editor: right of sidebar, above chrome
                  ;; Chrome: bottom strip (cmd panel + status bar)
                  ;; Settings: full viewport (overlay, conservative)
                  ;; first-light P2b: a camera move (pan/zoom) shifts EVERY
                  ;; world-camera pixel — partial scissor would smear; force
                  ;; a full clear.
                  dirty-rect
                  (cond
                    ;; First frame, resize, or settings/font changed → full clear, no scissor
                    (or first-frame? rt-resized? (not settings-same?) font-changed?
                        camera-moved?)
                    nil

                    ;; Content or chrome text changed → full viewport (text spans everything)
                    (or (not content-same?) (not chrome-same?))
                    nil

                    ;; Only rects/shadows changed — union per-subsystem regions
                    (or sidebar-dirty? editor-rects-dirty? editor-shadows-dirty?
                        cmd-dirty? settings-dirty?)
                    (let [regions (cond-> []
                                   sidebar-dirty?
                                   (conj {:x 0 :y 0 :w (max 1 sb-w) :h ph})
                                   (or editor-rects-dirty? editor-shadows-dirty?)
                                   (conj {:x sb-w :y 0 :w (- pw sb-w) :h (- ph chrome-h)})
                                   cmd-dirty?
                                   (conj {:x 0 :y (- ph chrome-h) :w pw :h chrome-h})
                                   settings-dirty?
                                   (conj {:x 0 :y 0 :w pw :h ph}))]
                      (when (seq regions)
                        (let [x1 (apply min (map :x regions))
                              y1 (apply min (map :y regions))
                              x2 (apply max (map #(+ (:x %) (:w %)) regions))
                              y2 (apply max (map #(+ (:y %) (:h %)) regions))]
                          {:x x1 :y y1 :w (- x2 x1) :h (- y2 y1)})))

                    ;; Nothing changed (shouldn't happen — world identity check should have caught this)
                    :else nil)

                  frame-log? (or (<= frame-idx 8)
                                 font-changed?
                                 backend-changed?
                                 (not content-same?)
                                 (not chrome-same?)
                                 (not settings-same?)
                                 rt-resized?)
                  _ (when frame-log?
                      (render-debug! "[RENDER/FRAME]"
                                     {:frame frame-idx
                                      :font-id (:id font-assets)
                                      :backend (:backend font-assets)
                                      :viewport viewport
                                      :content-lines (count (or content-ops []))
                                      :chrome-lines (count (or full-chrome-ops []))
                                      :content-instances (:num-instances new-content-geo)
                                      :chrome-instances (:num-instances new-chrome-geo)
                                      :editor-rects (count editor-rects)
                                      :editor-shadows (count (or editor-shadows []))
                                      :sidebar-rects (count sidebar-rects)
                                      :cmd-rects (count (or cmd-rects []))
                                      :settings-rects (count (or settings-rects []))
                                      :cmd-visible cmd-visible
                                      :agent-visible agent-visible
                                      :settings-visible settings-visible
                                      :snap? snap?
                                      :dirty-rect (or dirty-rect :full)
                                      :rt-enabled? use-persistent-render-target?}))
                  _ (when (and (seq content-ops)
                               (zero? (:num-instances new-content-geo)))
                      (js/console.warn "[RENDER/WARN] content ops present but content instance buffer is empty"
                                       {:frame frame-idx
                                        :content-lines (count content-ops)
                                        :font-id (:id font-assets)
                                        :backend (:backend font-assets)}))
                  raf-t3 (js/performance.now)  ;; before draw
                  ;; Sync canvas pixel dimensions atomically with draw.
                  ;; Setting canvas.width/height clears the swap chain, so this
                  ;; MUST happen in the same RAF as the draw, not in the resize consumer.
                  ^js canvas (.-canvas ctx)
                  target-w (Math/floor (* (:width viewport) dpr))
                  target-h (Math/floor (* (:height viewport) dpr))]
            (when (not= (.-width canvas) target-w)
              (set! (.-width canvas) target-w))
            (when (not= (.-height canvas) target-h)
              (set! (.-height canvas) target-h))
            (try
              (let [draw-result
                    (editor/draw-frame! device ctx
                                    new-content-geo
                                    (assoc (pool/pool-draw-info !editor-pool)
                                           :draw-count (count editor-rects))
                                    new-cmd-sys
                                    (:camera-floats (:pipelines geometry))
                                    (:pass-descriptor (:pipelines geometry))
                                    ;; first-light P2b: the ground drives the
                                    ;; REAL world camera (pan+zoom, Laws 2/3);
                                    ;; every other mode keeps the scroll rail.
                                    (if ground? (:x gcam) 0)
                                    (if ground? (:y gcam) (- scroll-y))
                                    (:width viewport) (:height viewport)
                                    :zoom (if ground? (:zoom gcam) 1.0)
                                    :frame-idx frame-idx
                                    :cmd-panel-visible cmd-visible
                                    :cmd-panel-h cmd-panel-h
                                    :chrome-text-sys new-chrome-geo
                                    :chrome-base-line-count chrome-base-count
                                    :settings-line-count settings-line-count
                                    :settings-visible settings-visible
                                    :settings-rect-sys new-settings-sys
                                    :diagnostics-visible show-diagnostics?
                                    :diagnostics-line-index diagnostics-line-index
                                    :agent-visible agent-visible
                                    :editor-shadow-pool-info
                                    (assoc (pool/pool-draw-info !editor-shadow-pool)
                                           :draw-count (count (or editor-shadows [])))
                                    :sidebar-shadow-pool-info (pool/pool-draw-info !sidebar-shadow-pool)
                                    :sidebar-pool-info (pool/pool-draw-info !sidebar-pool)
                                    :store-frame store-frame
                                    :image-system (:image-system (:pipelines geometry))
                                    :path-system (:path-system (:pipelines geometry))
                                    :connector-system (:connector-system
                                                       (:pipelines geometry))
                                    :chrome-system (:chrome-system
                                                    (:pipelines geometry))
                                    :effective-transforms effective
                                    :container-registry frame-registry
                                    :container-delta-snapshot
                                    (scene-rt/frame-container-delta-snapshot)
                                    :container-delta-ack!
                                    scene-rt/ack-frame-container-deltas!
                                    :frame-format (:format (:pipelines geometry))
                                    :region3d-session region3d-session
                                    :session-layout-snapshot
                                    (when-let [provider @!session-layout-provider]
                                      (provider))
                                    :dpr dpr
                                    :pulse-alpha
                                    (frame-scheduler/pulse-alpha
                                     (:time scheduler-step)
                                     (selection-active?))
                                    :font-assets font-assets
                                    :editor-rect-count (count editor-rects)
                                    :editor-shadow-count (count (or editor-shadows []))
                                    :dirty-rect dirty-rect
                                    :render-target render-target
                                    :clear-quad (:clear-quad (:pipelines geometry))
                                    ;; W2-B consumes the store tape's canonical
                                    ;; W2-A stack order; map iteration is never
                                    ;; a second text-family order truth.
                                    :extra-text-geos extra-text-geos)]
                (when-let [plan-hash (:plan-hash draw-result)]
                  (aset js/globalThis "__softlandFrameLastPlanHash" plan-hash)))
              (catch :default err
                (js/console.error "[RENDER/DRAW-FAIL]"
                                  err
                                  (clj->js {:frame frame-idx
                                            :font-id (:id font-assets)
                                            :backend (:backend font-assets)
                                            :viewport viewport
                                            :content-instances (:num-instances new-content-geo)
                                            :chrome-instances (:num-instances new-chrome-geo)
                                            :editor-rects (count editor-rects)
                                            :sidebar-rects (count sidebar-rects)
                                            :dirty-rect (or dirty-rect :full)
                                            :rt-enabled? use-persistent-render-target?}))
                (throw err)))
              (let [raf-t4 (js/performance.now)]
                (when (> (- raf-t4 raf-t0) 5)
                  (js/console.log "[RAF] prep:" (.toFixed (- raf-t1 raf-t0) 1) "ms | text-gpu:" (.toFixed (- raf-t2 raf-t1) 1) "ms | rects-gpu:" (.toFixed (- raf-t3 raf-t2) 1) "ms | draw:" (.toFixed (- raf-t4 raf-t3) 1) "ms | TOTAL:" (.toFixed (- raf-t4 raf-t0) 1) "ms | content-same?:" content-same? "chrome-same?:" chrome-same?
                                  "dirty-rect:" (if dirty-rect "partial" "full")
                                  "| reconcile:" (.toFixed rec-ms 1) "ms")
                  ;; [DRAW] sub-buckets from renderer.cljs mark-draw! stamps —
                  ;; splits the draw bucket into its per-frame phases.
                  (when-let [d (aget js/globalThis "__sfDraw")]
                    (let [g (fn [k] (aget d k))
                          f (fn [a b] (if (and (g a) (g b)) (.toFixed (- (g b) (g a)) 1) "?"))]
                      (js/console.log "[DRAW] img:" (f "t0" "img")
                                      "| path:" (f "img" "path") "| chrome:" (f "path" "chrome")
                                      "| region:" (f "chrome" "region") "| conn:" (f "region" "conn")
                                      "| produce:" (f "conn" "produce")
                                      "| plan:" (f "produce" "plan")
                                      "| cam:" (f "plan" "cam")
                                      "| project:" (f "cam" "arrange")
                                      "| twin:" (f "arrange" "twin")
                                      "| encode:" (if (g "twin") (.toFixed (- raf-t4 (g "twin")) 1) "?")
                                      "| fams:" (or (aget js/globalThis "__sfFams") "")
                                      "| changed:"
                                      (if-let [l (aget js/globalThis "__softlandFrameLedger")]
                                        (str (js/JSON.stringify (aget l "changed-families"))
                                             " plan?" (aget l "plan-maintained?")
                                             " rgn-prep" (aget l "region-prepared"))
                                        "?")))))))

            {:content-text-geo new-content-geo
             :chrome-text-geo new-chrome-geo
             :cmd-rect-sys new-cmd-sys
             :settings-rect-sys new-settings-sys
             :render-target render-target
             :prev-world world
             :prev-ground-camera ground-camera
             :pending-dirty-rect nil
             :scheduler-state (:state scheduler-step)
             :last-plan-hash (aget js/globalThis "__softlandFrameLastPlanHash")
             :prev-content-ops content-ops
             :prev-chrome-ops chrome-ops
             :prev-sidebar-data sidebar-data
             :prev-settings-text settings-text
             :prev-settings-visible settings-visible
             :prev-show-diagnostics show-diagnostics?
             :prev-scroll-y scroll-y
             :prev-editor-rects editor-rects
             :prev-editor-shadows editor-shadows
             :prev-cmd-rects cmd-rects
             :prev-settings-rects settings-rects
             :prev-font-size font-size
             :prev-px-range px-range
             :prev-line-height line-h
             :prev-sharpness sharpness
             :prev-char-width char-width
             :prev-snap-step snap-step
             :prev-font-id (:id font-assets)
             :prev-font-backend (:backend font-assets)
             ;; scene-substrate P3a
             :prev-store-frame store-frame
             :prev-effective effective
             :prev-face-scene face-scene
             :prev-face-context face-context
             ;; scene-substrate P3b Rung 2: per-slot text geos {vi {:geo :text}}
             :slot-text-geos slot-text-geos
             :extra-text-geos extra-text-geos
             :frame-idx frame-idx}))))

      (let [tracker @!gpu-budget
            chrome-text-geo (editor/clone-text-system device (:text geometry) 2000)
            render-target (when use-persistent-render-target?
                            (let [vp @!viewport
                                  d (or (:dpr vp) 1)]
                              (editor/create-render-target device
                                (Math/floor (* (:width vp) d))
                                (Math/floor (* (:height vp) d))
                                (:format (:pipelines geometry))
                                :tracker tracker
                                :scene-color (:scene-color (:pipelines geometry)))))]
        (render-debug! "[RENDER/INIT]"
                       {:viewport @!viewport
                        :font-id (:id @!font-assets)
                        :backend (:backend @!font-assets)
                        :persistent-render-target? use-persistent-render-target?
                        :has-render-target? (boolean render-target)})
        (gpu-budget/log-startup-report! tracker)
        (scene-rt/install-window-api! atoms) ;; scene-substrate P3a dev affordance
        (scene-rt/install-context-window-api! atoms) ;; scene-substrate P4 dev affordance
        (region3d-runtime/boot!
         (.-canvas ctx)
         {:screen->world
          (fn [[screen-x screen-y]]
            (let [{:keys [x y zoom]} @ground/!camera
                  zoom (or zoom 1.0)]
              [(/ (- screen-x (or x 0.0)) zoom)
               (/ (- screen-y (or y 0.0)) zoom)]))
          :viewport-scale
          (fn []
            {:zoom (or (:zoom @ground/!camera) 1.0)
             :dpr (or (:dpr @!viewport) 1.0)})})
        {:content-text-geo (:text geometry)
       :chrome-text-geo chrome-text-geo
       :cmd-rect-sys @!cmd-rect-sys
       :settings-rect-sys @!settings-rect-sys
       :render-target render-target
       :prev-world nil
       ;; Seed the sink-local camera identity at renderer construction.  A nil
       ;; sentinel made the first cold frame look like a camera gesture even
       ;; when the camera stayed at its boot value.
       :prev-ground-camera @ground/!camera
       :prev-content-ops nil
       :prev-chrome-ops nil
       :prev-sidebar-data nil
       :prev-settings-text nil
       :prev-settings-visible nil
       :prev-show-diagnostics nil
       :prev-scroll-y nil
       :prev-editor-rects nil
       :prev-editor-shadows nil
       :prev-cmd-rects nil
       :prev-settings-rects nil
       :prev-font-size nil
       :prev-px-range nil
       :prev-line-height nil
       :prev-sharpness nil
       :prev-char-width nil
       :prev-snap-step nil
       :prev-font-id (:id @!font-assets)
       :prev-font-backend (:backend @!font-assets)
       ;; scene-substrate P3a
       :prev-store-frame nil
       :prev-effective nil
       :prev-face-scene nil
       :prev-face-context nil
       ;; scene-substrate P3b Rung 2
       :slot-text-geos {}
       :extra-text-geos nil
       :frame-idx 0})

      (m/sample vector <world-snapshot >raf)))))
