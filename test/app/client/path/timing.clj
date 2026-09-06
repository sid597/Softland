;; The per-edit cost of the path kind's pure layer on the JVM: the definer's
;; records through their constructions, a pack for the GPU, a CPU answer.
;; Warm, 200 runs each, microseconds per run. A measurement, not a test.
;;
;;   clj -M:test -i test/app/client/path/timing.clj
;;
;; The browser's numbers for the same code at scene scale come from the
;; harness's trace: `node test/render_engine/dump_result.mjs <out>` (add
;; RENDER_VERIFIER_HARDWARE=1 for the real adapter), then the trace block of
;; <out>/path-step.json. docs/below-the-waist/path-kind/measurements-client-claude.md
;; reads both.
(require '[app.client.path.component :as c] '[app.client.path.records :as r] '[app.client.path.pack :as pack])

(defn- time-of [label f n]
  (dotimes [_ 50] (f))
  (let [t0 (System/nanoTime)]
    (dotimes [_ n] (f))
    (println (format "%-48s %8.1f us  (%d runs)" label (/ (- (System/nanoTime) t0) 1000.0 n) n))))

(time-of "run: Z as one skin (3 pieces, 26 curves)" #(c/run r/harness-z {}) 200)
(time-of "run: Z as 24 dabs" #(c/run r/z-as-dabs {}) 200)
(time-of "run: draw tool (70 samples, spline, envelope)" #(c/run r/draw-tool {}) 200)
(time-of "run: pen tool (4 cubics, fill + miter stroke)" #(c/run r/pen-tool {}) 200)
(time-of "run: border (rect, snap, fill, inside stroke)" #(c/run r/border {:scale 2.0 :pan [1.0 0.0]}) 200)
(time-of "run: holed concave (2 rings, even-odd)" #(c/run r/holed-concave {}) 200)
(let [skin (first (:regions (c/run r/harness-z {})))
      draw (first (:regions (c/run r/draw-tool {})))]
  (time-of "pack: Z skin at bucket 0" #(pack/pack-region (:path skin) (pack/bucket-tolerance 0) {}) 200)
  (time-of "pack: draw tool skin at bucket 0" #(pack/pack-region (:path draw) (pack/bucket-tolerance 0) {}) 200)
  (println "  draw tool skin curves, bucket 0:" (:count (:pack (pack/pack-region (:path draw) (pack/bucket-tolerance 0) {})))
           " bucket 3:" (:count (:pack (pack/pack-region (:path draw) (pack/bucket-tolerance 3) {})))))
(time-of "classify: Z at the crossing (runs, packs, winds)" #(c/classify r/harness-z [64.0 64.0]) 100)
(time-of "classify: draw tool at a point" #(c/classify r/draw-tool [64.0 64.0]) 100)
(System/exit 0)
