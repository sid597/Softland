;; Claude lane: verifies the Codex judge's F1 on the pure layer (a construction edit versus the run cache).
(require '[app.client.path.component :as c] '[app.client.path.records :as r])
(println "== Claude lane: F1, a construction edit versus the run cache ==")
(let [run0 (c/run r/harness-z {:scale 1.0})
      edited (-> r/harness-z (assoc :path/revision 2) (assoc :path/construction (c/default-construction r/z-as-dabs)))
      run1 (c/run edited {:scale 1.0})]
  (println "   original regions:" (count (:regions run0)) " reads:" (sort (keys (:reads run0))))
  (println "   edited record (same source, tool, paint; its own construction = the dabs default): fresh run regions" (count (:regions run1)))
  (println "   component/rerun? edited-record view old-reads →" (c/rerun? edited {:scale 1.0} (:reads run0))
           " (false = the renderer's run cache keeps the union; the construction is not among the reads)"))
(System/exit 0)
