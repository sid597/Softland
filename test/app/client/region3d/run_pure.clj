;; Shared engine/path records plus the sphere/coating records.
;; clj -M:test -i test/app/client/region3d/run_pure.clj
(require '[clojure.test :as t])
(def namespaces
  '[app.client.engine.executor-test app.client.engine.executor-pending-test app.client.engine.surface-test
    app.client.path.pickup-test app.client.path.value-test app.client.path.source-test app.client.path.stroke-test
    app.client.path.nib-test app.client.path.pack-test app.client.path.construction-test app.client.path.component-test
    app.client.path.frame-test app.client.region3d.path-placement-test
    app.client.region3d.surface-region-test app.client.region3d.coating-test app.client.region3d.brush-test])
(doseq [n namespaces] (require n))
(let [summary (apply t/run-tests namespaces)]
  (println "SUMMARY" summary)
  (System/exit (if (zero? (+ (:fail summary) (:error summary))) 0 1)))
