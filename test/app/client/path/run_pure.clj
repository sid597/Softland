;; The path kind's pure layer on the JVM, without the repository test runner
;; (which loads Rama). Runs the namespaces that carry the definer's
;; records as tests and exits with their status:
;;
;;   clj -M:test -i test/app/client/path/run_pure.clj
;;
;; For a REPL over the same code: `clj -M:test`, then
;;   (require '[app.client.path.component :as c] '[app.client.path.construction :as build] '[app.client.path.records :as r])
;;   (map :kind (:regions (c/regions (build/construct r/z-as-dabs) {})))   ; 24 dabs
;;   (c/classify (build/construct r/harness-z) [64.0 64.0])             ; :inside, once
(require '[clojure.test :as t])

(def namespaces
  '[app.client.engine.executor-test
    app.client.engine.surface-test
    app.client.path.pickup-test
    app.client.path.value-test
    app.client.path.source-test
    app.client.path.stroke-test
    app.client.path.nib-test
    app.client.path.pack-test
    app.client.path.construction-test
    app.client.path.component-test
    app.client.path.frame-test
    app.client.region3d.path-placement-test])

(doseq [n namespaces] (require n))

(let [summary (apply t/run-tests namespaces)]
  (println "SUMMARY" summary)
  (System/exit (if (and (zero? (:fail summary)) (zero? (:error summary))) 0 1)))
