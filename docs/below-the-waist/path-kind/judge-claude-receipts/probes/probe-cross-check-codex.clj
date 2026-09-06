;; Codex lane: verifies the Codex judge's F3 on the pure layer (a screen-fixed group's projected scale).
(require '[app.client.engine.transform :as transform] '[app.client.path.pack :as pack])
(println "== Codex lane: F3, a screen-fixed group's projected scale ==")
(let [registry (-> (transform/empty-registry) (transform/add-group 17 {:parent 0 :affine [1.0 0.0 0.0 1.0 0.0 0.0] :camera :screen}))
      wt (transform/world-transforms registry)
      row (get wt 17)]
  (println "   the registry's row for group 17:" (pr-str row))
  (println "   pack/projected-scale row zoom=1 dpr=1 →" (pack/projected-scale row 1.0 1.0)
           "; zoom=2 →" (pack/projected-scale row 2.0 1.0)
           " (a screen group should give 1.0 at both; pack reads (:camera row), the row carries :flags)"))
(System/exit 0)
