(ns app.client.substrate.webgpu.container-probe
  "scene-substrate P2 probe (UNCOMMITTED — delete this file + the tagged
   mount lines in runtime/render.cljs to remove).

   Gates G4/G6 (build/scene-substrate/CONTRACT.md): N containers, ≥10k
   glyphs packed ONCE through the real shape/pack path; per frame ONLY
   container transforms (16 B each) and the probe camera are written —
   instance buffers are never touched after start (trap T5's claim made
   observable). One container orbits; zoom is drivable through [0.1, 10].

   Window API (installed at boot):
     ctProbe.start(n)   — spawn n containers (default 16), begin the soak
     ctProbe.zoom(z)    — drive the probe camera zoom (G6)
     ctProbe.set(cid, x, y, scale) — place a container by hand
     ctProbe.stats()    — writes/frames counters (G4 receipt)
     ctProbe.stop()     — freeze (systems stay; start() reuses them)"
  (:require [app.client.substrate.webgpu.renderer :as renderer]))

(defonce !state (atom nil)) ;; nil = never started; {:active? bool ...}

(defn driving? []
  (boolean (:active? @!state)))

;; --- scene ------------------------------------------------------------------

(def ^:private lines-per-container 20)
(def ^:private line-chars 34)

(defn- probe-text-ops
  "Container-LOCAL text: identical content in every container — placement on
   screen comes only from the transform (that's the point)."
  [n]
  (vec
   (for [cid (range 1 (inc n))
         line (range lines-per-container)]
     [{:text (str "c" cid " line " line " "
                  (apply str (repeat (- line-chars 12) (char (+ 33 (mod (+ cid line) 90))))))
       :x 8.0
       :y (+ 24.0 (* line 16.0))
       :size 13
       :r 0.75 :g 0.82 :b 0.9 :a 1.0
       :container-idx cid}])))

(defn- probe-rects [n]
  (vec
   (for [cid (range 1 (inc n))]
     {:x 0.0 :y 0.0 :w 320.0 :h (+ 40.0 (* lines-per-container 16.0))
      :r 0.12 :g 0.14 :b 0.2 :a 0.92
      :radius 8.0
      :border-width 1.0
      :border-color [0.4 0.55 0.8 0.9]
      :container-idx cid})))

(defn- grid-transforms
  "Initial layout: containers tile a grid in world space, slight scale ramp."
  [n]
  (into {}
        (for [cid (range 1 (inc n))]
          (let [col (mod (dec cid) 4)
                row (quot (dec cid) 4)]
            [cid {:x (+ 40.0 (* col 360.0))
                  :y (+ 40.0 (* row 400.0))
                  :scale (+ 0.55 (* 0.05 (mod cid 5)))}]))))

;; --- lifecycle ----------------------------------------------------------------

(defn- ensure-systems!
  "Create the probe's own rect+text systems ONCE, sharing the app's
   containers buffer (from geometry :pipelines) but a PRIVATE camera so
   probe zoom never disturbs the app's chrome."
  [^js device geometry font-assets n]
  (let [st @!state]
    (if (:text-sys st)
      st
      (let [pipelines (:pipelines geometry)
            containers-buffer (:containers-buffer pipelines)
            format (:format pipelines)
            tracker (:gpu-tracker (:text-sys pipelines))
            camera-buffer (renderer/create-camera-buffer device tracker)
            text-sys (renderer/init-text-system device format camera-buffer font-assets
                                                :initial-capacity (* n lines-per-container line-chars)
                                                :tracker tracker
                                                :label "ct-probe/text"
                                                :containers-buffer containers-buffer)
            rect-sys (renderer/init-rect-system device format camera-buffer
                                                :initial-capacity (+ n 8)
                                                :tracker tracker
                                                :label "ct-probe/rects"
                                                :containers-buffer containers-buffer)
            st' (merge st {:camera-buffer camera-buffer
                           :camera-floats (js/Float32Array. 6)
                           :containers-buffer containers-buffer
                           :text-sys text-sys
                           :rect-sys rect-sys})]
        (reset! !state st')
        st'))))

(defn start!
  "Pack the probe scene ONCE and begin the soak. Safe to call again — reuses
   systems, repacks only when n changes."
  [^js device geometry font-assets n]
  (let [n (or n 16)
        st (ensure-systems! device geometry font-assets n)
        text-sys (renderer/update-text-data device (:text-sys st)
                                            (probe-text-ops n) font-assets 13
                                            :char-width 0.56)
        rect-sys (renderer/update-rects device (:rect-sys st) (probe-rects n))]
    (swap! !state merge
           {:active? true
            :n n
            :text-sys text-sys
            :rect-sys rect-sys
            :transforms (grid-transforms n)
            :zoom 1.0
            :t0 (js/performance.now)
            :frames 0
            :instance-packs (inc (or (:instance-packs @!state) 0))
            :container-writes 0})
    (js/console.log "[CT-PROBE] start" #js {:containers n
                                            :glyphs (:num-instances text-sys)
                                            :rects n})))

(defn- write-frame-transforms!
  "The per-frame gesture: container 1 orbits, everything else static.
   This is the ONLY buffer write of the frame (16 B × containers-in-range)."
  [^js device st now]
  (let [t (/ (- now (:t0 st)) 1000.0)
        transforms (assoc-in (:transforms st) [1]
                             {:x (+ 300.0 (* 240.0 (Math/cos t)))
                              :y (+ 220.0 (* 160.0 (Math/sin (* 1.3 t))))
                              :scale (+ 0.9 (* 0.35 (Math/sin (* 0.7 t))))})]
    (renderer/write-containers! device (:containers-buffer st) transforms)
    (swap! !state #(-> %
                       (assoc :transforms transforms)
                       ;; frame-delta ring (last ~600 RAF deltas) → p50/p95/max
                       ;; receipts, so "feels like 40fps" becomes a number (G4)
                       (assoc :last-now now)
                       (update :deltas (fn [ds]
                                         (let [ds (or ds [])
                                               ds (if-let [ln (:last-now %)]
                                                    (conj ds (- now ln)) ds)]
                                           (if (> (count ds) 600)
                                             (subvec ds (- (count ds) 600)) ds))))
                       (update :container-writes inc)
                       (update :frames inc)))))

(defn- delta-stats
  "p50/p95/max over the recorded frame deltas (ms, 1 decimal)."
  [deltas]
  (when (seq deltas)
    (let [sorted (vec (sort deltas))
          n (count sorted)
          q (fn [p] (nth sorted (min (dec n) (int (* p n)))))
          r (fn [v] (/ (js/Math.round (* 10 v)) 10))]
      {:p50 (r (q 0.5)) :p95 (r (q 0.95)) :max (r (peek sorted)) :n n})))

(defn- anatomy-stats
  "Frame-anatomy p50s (diagnosis 2026-07-13): sample = rAF fire → reduce-body
   entry (the m/latest sampling side, invisible to [RAF]) · body = uploads +
   draw encode + island step · probe = this file's step! · wait = derived
   (raf-delta p50 − the rest ≈ GPU/compositor backpressure + scheduling).
   Segments stay nil until render.cljs passes raf-t0/frame-time through
   (hard refresh after a hot swap)."
  [anat delta-p50]
  (let [sample (:p50 (delta-stats (keep :sample anat)))
        body   (:p50 (delta-stats (keep :body anat)))
        probe  (:p50 (delta-stats (keep :probe anat)))]
    {:sample sample :body body :probe probe
     :wait (when (and delta-p50 sample body probe)
             (/ (js/Math.round (* 10 (- delta-p50 sample body probe))) 10))}))

(defn step!
  "Called from the runtime frame (after the main draw): write transforms +
   probe camera, then draw the probe systems in an additive pass. Runs
   OUTSIDE the frame's try/catch — any probe error deactivates the probe
   instead of tearing down the render consumer (falsification finding #2).
   raf-t0 = reduce-body entry, frame-time = the rAF callback stamp — both
   optional (3-arity keeps stale hot-reload callers alive); when present the
   receipts split each frame into sample/body/probe/wait (anatomy-stats)."
  ([^js device ^js ctx viewport] (step! device ctx viewport nil nil))
  ([^js device ^js ctx viewport raf-t0 frame-time]
  (when-let [st @!state]
    (when (:active? st)
      (try
      (let [now (js/performance.now)
            {:keys [width height]} viewport]
        (write-frame-transforms! device st now)
        (renderer/update-camera device (:camera-buffer st) (:camera-floats st)
                                0 0 (:zoom st) width height)
        (let [st @!state
              encoder (.createCommandEncoder device)
              view (.createView (.getCurrentTexture ctx))
              pass (.beginRenderPass encoder
                     (clj->js {:colorAttachments [{:view view
                                                   :loadOp "load"
                                                   :storeOp "store"}]}))
              rect-sys (:rect-sys st)
              text-sys (:text-sys st)]
          (when (pos? (:num-instances rect-sys))
            (.setPipeline pass (:pipeline rect-sys))
            (.setBindGroup pass 0 (:bind-group rect-sys))
            (.setVertexBuffer pass 0 (:instance-buffer rect-sys))
            (.draw pass 6 (:num-instances rect-sys) 0 0))
          (when (pos? (:num-instances text-sys))
            (.setPipeline pass (:pipeline text-sys))
            (.setBindGroup pass 0 (:bind-group text-sys))
            (.setVertexBuffer pass 0 (:instance-buffer text-sys))
            (.draw pass 6 (:num-instances text-sys) 0 0))
          (.end pass)
          (.submit (.-queue device) #js [(.finish encoder)]))
        ;; frame-anatomy ring (diagnosis 2026-07-13): where does the RAF
        ;; delta actually go — sampling / body JS / probe / GPU wait?
        (let [t-end (js/performance.now)]
          (swap! !state update :anat
                 (fn [xs]
                   (let [xs (conj (or xs [])
                                  {:sample (when (and raf-t0 frame-time)
                                             (- raf-t0 frame-time))
                                   :body   (when raf-t0 (- now raf-t0))
                                   :probe  (- t-end now)})]
                     (if (> (count xs) 600)
                       (subvec xs (- (count xs) 600)) xs)))))
        (when (zero? (mod (:frames @!state) 300))
          (let [st @!state
                secs (/ (- now (:t0 st)) 1000.0)
                ds (delta-stats (:deltas st))
                an (anatomy-stats (:anat st) (:p50 ds))]
            (js/console.log "[CT-PROBE]"
                            #js {:frames (:frames st)
                                 :fps (js/Math.round (/ (:frames st) (max secs 0.001)))
                                 :frameMsP50 (:p50 ds)
                                 :frameMsP95 (:p95 ds)
                                 :frameMsMax (:max ds)
                                 :sampleMsP50 (:sample an)
                                 :bodyMsP50 (:body an)
                                 :probeMsP50 (:probe an)
                                 :waitMsP50 (:wait an)
                                 :containerWrites (:container-writes st)
                                 :instancePacks (:instance-packs st)
                                 :glyphs (:num-instances (:text-sys st))
                                 :zoom (:zoom st)}))))
      (catch :default e
        (swap! !state assoc :active? false)
        (js/console.error "[CT-PROBE] step failed — probe stopped" e)))))))

(defn stats []
  (let [st @!state
        ds (delta-stats (:deltas st))
        an (anatomy-stats (:anat st) (:p50 ds))]
    #js {:frames (or (:frames st) 0)
         :frameMsP50 (:p50 ds)
         :frameMsP95 (:p95 ds)
         :frameMsMax (:max ds)
         :sampleMsP50 (:sample an)
         :bodyMsP50 (:body an)
         :probeMsP50 (:probe an)
         :waitMsP50 (:wait an)
         :recentFps (when (:p50 ds) (js/Math.round (/ 1000 (max (:p50 ds) 0.1))))
         :containerWrites (or (:container-writes st) 0)
         :instancePacks (or (:instance-packs st) 0)
         :active (boolean (:active? st))}))

(defn install-window-api!
  "window.ctProbe — start/stop/zoom/set/stats from the console.
   font-assets-fn is a getter (deref'd at start! time) so a font-backend
   swap mid-session probes the CURRENT backend."
  [^js device geometry font-assets-fn]
  (set! (.-ctProbe js/window)
        #js {:start (fn [n] (start! device geometry (font-assets-fn) (or n 16)))
             :stop (fn [] (swap! !state assoc :active? false)
                     (js/console.log "[CT-PROBE] stopped" (stats)))
             :zoom (fn [z] (swap! !state assoc :zoom (max 0.1 (min 10.0 z)))
                     (js/console.log "[CT-PROBE] zoom" z))
             :set (fn [cid x y scale]
                    (swap! !state update :transforms assoc (int cid)
                           {:x x :y y :scale (or scale 1.0)}))
             :stats (fn [] (stats))})
  (js/console.log "[CT-PROBE] window.ctProbe installed — ctProbe.start(16)")
  ;; Headless-receipt hook: ?ct-probe=N auto-starts the soak after boot
  ;; (used by the G4 headless run; inert without the query param).
  (when-let [m (re-find #"ct-probe=(\d+)" (str (.-search (.-location js/window))))]
    (js/setTimeout #(start! device geometry (font-assets-fn) (js/parseInt (second m) 10))
                   3000)))
