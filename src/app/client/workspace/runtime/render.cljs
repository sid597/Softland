(ns app.client.workspace.runtime.render
  "Render consumer: world snapshot, store-slot text reconciliation, GPU
   upload diffing, draw on RAF. Post ground-boot the frame is the land's:
   the world camera, the scene store's per-slot contribution, the frame
   registry, and Region3D. The dev workspace's editor/sidebar/flow/chat
   feeds died with that surface (dead-path census, Sid's ruling 2026-08-15);
   the <face-main flow survives here — its consumer is the ground's
   per-block reconcile (ground/on-face-bundle!)."
  (:require [missionary.core :as m]
            [app.client.substrate.frame-inputs :as frame-inputs]
            [app.client.substrate.frame-scheduler :as frame-scheduler]
            [app.client.substrate.webgpu.renderer :as editor]
            [app.client.workspace.scene-runtime :as scene-rt] ;; scene-substrate P3a/P3b
            [app.client.substrate.webgpu.buffer-pool :as pool]
            [app.client.substrate.webgpu.gpu-budget :as gpu-budget]
            [app.client.workspace.ground :as ground]
            [app.client.workspace.frame-runtime :as frame-runtime]
            [app.client.workspace.region3d-runtime :as region3d-runtime]))

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
  "Missionary consumer: build world snapshot, diff-upload to GPU, draw on RAF."
  [{:keys [!viewport !settings !active-font
           !font-manifest !font-assets !text-geo !gpu-budget
           !cmd-rect-sys !settings-rect-sys
           !sidebar-pool !editor-pool !editor-shadow-pool !sidebar-shadow-pool
           !face-context]
    :as atoms}
   _layout
   {:keys [device ctx geometry]}
   _deps
   >raf]
  (let [;; ── first-light P1: the main-face bundle flow. On the ground the
        ;; consumer is ground/on-face-bundle! — it reads ONLY :face-context
        ;; (the outline-face tree never renders; the ground IS per-block
        ;; slots). The dev assembly-face renderer (build-main-face!) died
        ;; with the workspace.
        <face-main (m/latest (fn [fc] {:face-context fc})
                             (m/watch !face-context))

        ;; ── scene-substrate P3a: the store's GPU contribution + composed
        ;; container transforms. TWO independent single-source flows (store /
        ;; registry) combined ONCE in the world snapshot — no diamond (T3).
        <store-frame (scene-rt/<store-frame)
        <effective   (scene-rt/<effective)
        <frame-registry (scene-rt/<frame-registry)

        <world-snapshot (m/latest
                          (fn [viewport settings active-font
                               store-frame effective frame-registry
                               region3d-session]
                            {:store-frame store-frame
                             :effective   effective
                             :frame-registry frame-registry
                             :region3d-session region3d-session
                             :viewport viewport
                             :font-size (:font-size settings)
                             :px-range (:px-range settings)
                             :line-height (:line-height settings)
                             :sharpness (:sharpness settings)
                             :snap-to-pixel? (:snap-to-pixel? settings)
                             :show-diagnostics? (:show-diagnostics? settings)
                             :char-width (:char-width active-font)})
                          (m/watch !viewport)
                          (m/watch !settings)
                          (m/watch !active-font)
                          <store-frame
                          <effective
                          <frame-registry
                          (m/watch region3d-runtime/!session))]

    ;; Two joined consumers: the main-face slot edge + the RAF render pulse.
    (m/join vector
      ;; ── first-light P1: the main-face slot consumer ──
      ;; A DEDICATED edge (T4: store mutations at edges only), deliberately NOT
      ;; the RAF edge — and the reconcile runs in a COALESCING MICROTASK, not
      ;; synchronously in the propagation (measured, G1 drill). Bursts within
      ;; one turn coalesce to one reconcile (last bundle wins).
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
                    (when (ground/ground-active?)
                      (ground/on-face-bundle! b))))))
            nil)
          nil <face-main))

      ;; Render pulse: sample world on each RAF tick
      (m/reduce
      (fn [prev-state [world frame-time]]
        ;; first-light P2b, quarantined by SEAM-STEP1: camera is a sink-local
        ;; mosaic input. It is dereferenced per RAF and never enters derivation.
        (let [ground-camera @ground/!camera
              world-changed? (not (identical? world (:prev-world prev-state)))
              camera-moved? (not= ground-camera (:prev-ground-camera prev-state))
              logical-time (frame-scheduler/clock-time frame-time)
              _ (frame-runtime/sync-pulse-deadline! logical-time)
              causes (frame-scheduler/derive-causes
                      {:world-changed? world-changed?
                       :camera-moved? camera-moved?
                       :dirty-rect-pending? false})
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
                {:keys [viewport font-size px-range line-height sharpness
                        char-width snap-to-pixel? show-diagnostics?
                        region3d-session]} world

                ;; ── scene-substrate P3b: store contribution ──
                store-frame          (:store-frame world)
                effective            (:effective world)
                frame-registry       (:frame-registry world)
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
                ;; On the ground the store ALWAYS composites (per-block slots
                ;; are the surface). The dev-workspace face-mode gate died
                ;; with the editor it protected.
                store-text-by-vi     (:text-by-vi store-frame)
                store-rects          (:rects store-frame)
                store-shadows        (:shadows store-frame)
                containers-buffer    (:containers-buffer (:pipelines geometry))
                ;; Upload composed container transforms only when they changed
                ;; (drag/spawn); instance buffers untouched (identical? skip).
                _ (when-not (identical? effective (:prev-effective prev-state))
                    (editor/write-containers! device containers-buffer effective))

                dpr (:dpr viewport)
                snap? (not (false? snap-to-pixel?))
                line-h (let [v (* font-size line-height)]
                         (if snap? (/ (Math/round (* v dpr)) dpr) v))
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

                ;; Common rendering settings check
                settings-same? (and (= font-size (:prev-font-size prev-state))
                                    (= px-range (:prev-px-range prev-state))
                                    (= line-h (:prev-line-height prev-state))
                                    (= sharpness (:prev-sharpness prev-state))
                                    (= char-width (:prev-char-width prev-state))
                                    (= snap-step (:prev-snap-step prev-state))
                                    (not font-changed?))

                ;; The content geo is the slot clone-parent only — the ground
                ;; never reshapes it with monolithic ops.
                new-content-geo updated-content-geo

                ;; ── P3b Rung 2: per-slot isolated text geos (G8) ──
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

                ;; ── Chrome text: the diagnostics overlay only ──
                diagnostics-line (when show-diagnostics?
                                   (let [diag-size (max 10 (- font-size 2))
                                         backend-name (name (:backend font-assets))
                                         atlas-size (get-in font-assets [:atlas :atlas :size])
                                         font-name (:name @!active-font)
                                         gpu-line (gpu-budget/summary-line gpu-tracker)
                                         diag-text (str "font: " (or font-name (:id font-assets)) "\n"
                                                        "backend: " backend-name "\n"
                                                        "dpr: " dpr "  snap: " (if snap? "on" "off") "\n"
                                                        "pxRange: " px-range "  sharp: " sharpness "\n"
                                                        "atlas: " atlas-size
                                                        "  charW: " char-width
                                                        (when gpu-line
                                                          (str "\n" gpu-line)))]
                                     [{:text diag-text :type :comment
                                       :from 0 :to (count diag-text)
                                       :x 16 :y 20 :size diag-size
                                       :r 0.7 :g 0.7 :b 0.7 :a 1.0}]))
                diagnostics-line-index (when diagnostics-line 0)
                chrome-same? (and (= show-diagnostics? (:prev-show-diagnostics prev-state))
                                  settings-same?)
                new-chrome-geo (if chrome-same?
                                 updated-chrome-geo
                                 (editor/update-text-data device updated-chrome-geo
                                                          (vec (or diagnostics-line []))
                                                          font-assets font-size
                                                          :px-range px-range
                                                          :line-height line-h
                                                          :char-width char-width
                                                          :snap-step snap-step
                                                          :sharpness sharpness
                                                          :surface :combined-text-ops))

                raf-t2 (js/performance.now)
                new-text-geo new-content-geo

                ;; Store slots' rects ride the editor pool, each stamped with
                ;; its :container-idx (pack-rect honors it).
                _editor-pool-diff (when store-frame-changed?
                                    (pool/ordered-diff-update-pool! !editor-pool
                                                                    (vec (or store-rects []))))
                _editor-shadow-diff
                (when store-frame-changed?
                  (pool/batch-update-pool! !editor-shadow-pool
                                           (vec (or store-shadows []))))]

            (reset! !text-geo new-text-geo)

            (let [first-frame? (nil? (:prev-world prev-state))
                  frame-log? (or (<= frame-idx 8)
                                 font-changed?
                                 backend-changed?
                                 (not chrome-same?)
                                 (not settings-same?))
                  _ (when frame-log?
                      (render-debug! "[RENDER/FRAME]"
                                     {:frame frame-idx
                                      :first-frame? first-frame?
                                      :font-id (:id font-assets)
                                      :backend (:backend font-assets)
                                      :viewport viewport
                                      :store-rects (count (or store-rects []))
                                      :live-slots (count (or store-text-by-vi {}))
                                      :snap? snap?}))
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
                                           :draw-count (count (or store-rects [])))
                                    @!cmd-rect-sys
                                    (:camera-floats (:pipelines geometry))
                                    (:pass-descriptor (:pipelines geometry))
                                    ;; first-light P2b: the ground drives the
                                    ;; REAL world camera (pan+zoom, Laws 2/3).
                                    (:x gcam)
                                    (:y gcam)
                                    (:width viewport) (:height viewport)
                                    :zoom (:zoom gcam)
                                    :frame-idx frame-idx
                                    :cmd-panel-visible false
                                    :cmd-panel-h 0
                                    :chrome-text-sys new-chrome-geo
                                    :chrome-base-line-count 0
                                    :settings-line-count 0
                                    :settings-visible false
                                    :settings-rect-sys @!settings-rect-sys
                                    :diagnostics-visible show-diagnostics?
                                    :diagnostics-line-index diagnostics-line-index
                                    :agent-visible false
                                    :editor-shadow-pool-info
                                    (assoc (pool/pool-draw-info !editor-shadow-pool)
                                           :draw-count (count (or store-shadows [])))
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
                                    :editor-rect-count (count (or store-rects []))
                                    :editor-shadow-count (count (or store-shadows []))
                                    :dirty-rect nil
                                    :render-target nil
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
                                            :store-rects (count (or store-rects []))}))
                (throw err)))
              (let [raf-t4 (js/performance.now)]
                (when (> (- raf-t4 raf-t0) 5)
                  (js/console.log "[RAF] prep:" (.toFixed (- raf-t2 raf-t0) 1) "ms | rects-gpu:" (.toFixed (- raf-t3 raf-t2) 1) "ms | draw:" (.toFixed (- raf-t4 raf-t3) 1) "ms | TOTAL:" (.toFixed (- raf-t4 raf-t0) 1) "ms"
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
             :prev-world world
             :prev-ground-camera ground-camera
             :scheduler-state (:state scheduler-step)
             :last-plan-hash (aget js/globalThis "__softlandFrameLastPlanHash")
             :prev-show-diagnostics show-diagnostics?
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
             ;; scene-substrate P3b Rung 2: per-slot text geos {vi {:geo :text}}
             :slot-text-geos slot-text-geos
             :extra-text-geos extra-text-geos
             :frame-idx frame-idx}))))

      (let [tracker @!gpu-budget
            chrome-text-geo (editor/clone-text-system device (:text geometry) 256)]
        (render-debug! "[RENDER/INIT]"
                       {:viewport @!viewport
                        :font-id (:id @!font-assets)
                        :backend (:backend @!font-assets)
                        :persistent-render-target? use-persistent-render-target?})
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
       :prev-world nil
       ;; Seed the sink-local camera identity at renderer construction.  A nil
       ;; sentinel made the first cold frame look like a camera gesture even
       ;; when the camera stayed at its boot value.
       :prev-ground-camera @ground/!camera
       :prev-show-diagnostics nil
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
       ;; scene-substrate P3b Rung 2
       :slot-text-geos {}
       :extra-text-geos nil
       :frame-idx 0})

      (m/sample vector <world-snapshot >raf))))
