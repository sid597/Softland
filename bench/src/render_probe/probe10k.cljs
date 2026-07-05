(ns render-probe.probe10k
  "Browser half of the NORTH.md §9 probe: synthetic incseq diff stream at
   10^2 / 10^3 / 10^4 elements -> scene-store apply path -> [RAF]-shaped
   instrumentation. One diff batch per RAF frame (an upper bound on real
   trail-view rates — stress, declared).

   Per frame it records: mint ms (client-side ->seq-differ, EXCLUDED from
   the consumer verdict — minting is server-side in the real system),
   apply ms (the measured leg), RAF timestamp (pacing), diff op counts,
   JS heap. Results POST to the runner as JSON per cell.

   Mirrors the as-built [RAF] instrument's shape (runtime/render.cljs:484):
   phase timings via performance.now, 5ms reporting threshold applied at
   analysis time, all frames recorded."
  (:require [render-probe.mixes :as mixes]
            [render-probe.consumers :as c]
            [app.client.substrate.webgpu.buffer-pool :as pool]
            [hyperfiddle.incseq :as i]))

(def warmup-frames 20)
(def measure-frames 150)
(def cell-cap-ms 15000)

(def cells
  (vec
    (concat
      (for [n [100 1000 10000]
            mix [:cold-load :change-1 :change-10 :permute-rot
                 :churn-10 :churn-100 :grow-append]
            :when (not (and (= mix :churn-100) (= n 100)))]
        {:consumer :direct :n n :mix mix})
      (for [n [100 1000 10000]
            mix [:cold-load :change-1 :change-10 :permute-rot :churn-10]]
        {:consumer :recollect :n n :mix mix})
      (for [n [100 1000 10000]
            mix [:change-1 :change-10 :grow-append]]
        {:consumer :mount :n n :mix mix})
      ;; deliberate demonstration of the C1 permutation corruption
      [{:consumer :mount :n 100 :mix :permute-rot :expect :corrupt}]
      ;; pre-minted diffs: at 10^4 live minting saturates the frame, hiding
      ;; the consumer's own cost. Mint ONE diff at setup, re-apply it every
      ;; frame (a rotation/churn diff composes legally with itself), so the
      ;; apply column is observable at scale. Verification: slot count only.
      (for [mix [:permute-rot :churn-10 :churn-100]]
        {:consumer :direct :n 10000 :mix mix :premint true}))))

(defn- now [] (js/performance.now))

(defn- heap-bytes []
  (when-let [m (.-memory js/performance)]
    (.-usedJSHeapSize m)))

(defn post! [path obj]
  (js/fetch path
            #js {:method "POST"
                 :headers #js {"Content-Type" "application/json"}
                 :body (js/JSON.stringify (clj->js obj))}))

(defn- log! [& xs]
  (let [line (apply str (interpose " " (map str xs)))]
    (js/console.log line)
    (when-let [el (.getElementById js/document "log")]
      (set! (.-textContent el) (str (.-textContent el) line "\n")))))

(defonce !device-mode (atom nil))
(defonce !device (atom nil))
(defonce !adapter-info (atom nil))
(defonce !gpu-latencies (atom []))

(defn- sample-gpu-done!
  "Every 30th frame on a real device: measure apply-end -> queue-work-done.
   Async — never stalls the frame; resolves later and records."
  [frame t-apply-end]
  (when (and (= @!device-mode :webgpu) (zero? (mod frame 30)))
    (-> (.onSubmittedWorkDone (.-queue ^js @!device))
        (.then (fn [_]
                 (swap! !gpu-latencies conj
                        {:frame frame :ms (- (now) t-apply-end)}))))))

(defn run-cell!
  "Runs one cell on RAF; calls (done summary) when finished."
  [{:keys [consumer n mix expect premint] :as cell} done]
  (let [device @!device
        pool-cap (if (= mix :cold-load) 256 (js/Math.ceil (* n 1.5)))
        pool* (pool/create-pool device pool-cap nil nil
                                :label (str (name consumer) "-" n "-" (name mix)))
        cons* (c/make-consumer consumer pool*)
        {:keys [init step]} (mixes/make-mix mix n)
        differ (i/->seq-differ :id)
        !frames (atom [])
        !err (atom nil)
        ;; premint: one representative diff minted at setup, re-applied every
        ;; frame; isolates consumer cost from differ cost at 10^4.
        pre-diff (when premint
                   (let [d (differ init)]
                     (when-not (= consumer :recollect)
                       ((:apply-diff! cons*) d)))
                   (differ (step init 0)))
        ;; clock starts AFTER setup/pre-mint: the cap bounds the RAF loop,
        ;; not the (possibly seconds-long) synchronous setup stall.
        cell-t0 (now)]
    (reset! !gpu-latencies [])
    ;; setup: land the initial collection, unmeasured (steady-state premise);
    ;; :cold-load starts empty so frame 0 IS the measurement.
    (when-not premint
      (try
        (if (= consumer :recollect)
          ((:apply-coll! cons*) init)
          ((:apply-diff! cons*) (differ init)))
        (catch :default e (reset! !err (str "setup: " e)))))
    (letfn [(finish [coll frames-run]
              (let [verify (try ((:verify cons*) (when-not premint coll))
                                (catch :default e {:verify-error (str e)}))
                    summary {:cell (assoc cell :expect (or expect :ok))
                             :device-mode @!device-mode
                             :pool-capacity (:capacity @pool*)
                             :frames-run frames-run
                             :verify verify
                             :error @!err
                             :gpu-done-samples @!gpu-latencies}]
                (log! "CELL DONE" (pr-str (dissoc summary :gpu-done-samples)))
                (-> (post! "/log" {:type :cell
                                   :summary summary
                                   :frames @!frames})
                    (.then #(done summary))
                    (.catch #(done summary)))))
            (tick [frame coll]
              (js/requestAnimationFrame
                (fn [raf-ts]
                  (if (or (= frame (+ warmup-frames measure-frames))
                          (> (- (now) cell-t0) cell-cap-ms)
                          @!err)
                    (finish coll frame)
                    (let [coll' (if premint coll (step coll frame))
                          t0 (now)
                          diff (if premint
                                 pre-diff
                                 (when (not= consumer :recollect)
                                   (differ coll')))
                          t1 (now)]
                      (try
                        (if (= consumer :recollect)
                          ((:apply-coll! cons*) coll')
                          ((:apply-diff! cons*) diff))
                        (catch :default e (reset! !err (str "apply@" frame ": " e))))
                      (let [t2 (now)]
                        (sample-gpu-done! frame t2)
                        ;; record every frame; :w flags warmup so analysis can
                        ;; prefer post-warmup samples but a time-capped cell
                        ;; still reports honest numbers.
                        (swap! !frames conj
                               {:f frame
                                :w (< frame warmup-frames)
                                :raf raf-ts
                                :mint (if premint 0 (- t1 t0))
                                :apply (- t2 t1)
                                :grow (when diff (:grow diff))
                                :shrink (when diff (:shrink diff))
                                :perm (when diff (count (:permutation diff)))
                                :change (when diff (count (:change diff)))
                                :heap (heap-bytes)}))
                      (tick (inc frame) coll'))))))]
      (tick 0 init))))

(defn- active-cells []
  (if (re-find #"premint-only" (str (.-search js/location)))
    (filterv :premint cells)
    cells))

(defn run-all! []
  (let [cells (active-cells)
        t0 (now)]
    (letfn [(next-cell [idx]
              (if (= idx (count cells))
                (-> (post! "/done" {:cells (count cells)
                                    :total-ms (- (now) t0)
                                    :device-mode @!device-mode
                                    :adapter-info @!adapter-info
                                    :ua (.-userAgent js/navigator)})
                    (.then #(log! "ALL DONE")))
                (do (log! "CELL" (inc idx) "/" (count cells)
                          (pr-str (nth cells idx)))
                    (run-cell! (nth cells idx)
                               (fn [_]
                                 ;; brief gap between cells: pool drops out of
                                 ;; scope; lets GC/queue settle (declared).
                                 (js/setTimeout #(next-cell (inc idx)) 200))))))]
      (next-cell 0))))

(defn init! []
  (-> (c/request-real-device)
      (.then (fn [real]
               (if real
                 (do (reset! !device-mode :webgpu)
                     (reset! !device (:device real))
                     (reset! !adapter-info (:adapter-info real)))
                 (do (c/ensure-gpu-usage-shim!)
                     (reset! !device-mode :stub)
                     (reset! !device (c/stub-device))))
               (log! "DEVICE" (name @!device-mode) (pr-str @!adapter-info))
               (run-all!)))
      (.catch (fn [e]
                (log! "FATAL" e)
                (post! "/done" {:fatal (str e)})))))

(defonce _boot (init!))
