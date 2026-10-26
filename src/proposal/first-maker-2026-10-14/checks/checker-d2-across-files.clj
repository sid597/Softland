;; The checker's own run, 15 October 2026. Loads runs/decision-2-maps.clj
;; (silenced) for its reader, as runs/decision-2-fixes.clj does, repeats that
;; script's `defs-by-id` and `across` word for word, and prints how many files
;; each repeated :<ns>/<name> id sits in. Prints only. Run from the repository root:
;;   clojure -M:store src/proposal/first-maker-2026-10-14/checks/checker-d2-across-files.clj
(alter-var-root (var clojure.core/shutdown-agents) (constantly (fn [])))
(binding [*out* (java.io.StringWriter.)]
  (load-file "src/proposal/first-maker-2026-10-14/runs/decision-2-maps.clj"))
(def defs-by-id
  (reduce (fn [acc path]
            (let [forms (top-forms (git-show path))
                  ns-form (first (filter #(= 'ns (first (:form %))) forms))]
              (if-not ns-form
                acc
                (let [ns-name (str (second (:form ns-form)))]
                  (reduce (fn [acc {:keys [form]}]
                            (if (and (not= 'ns (first form)) (symbol? (second form)))
                              (update acc (keyword ns-name (name (second form))) (fnil conj #{}) path)
                              acc))
                          acc forms)))))
          {} (filter #(re-find #"\.cljc?$" %) tracked)))
(def across (filter #(> (count (val %)) 1) defs-by-id))
(println "repeated ids:" (count across))
(println "files per repeated id -> how many ids:" (into (sorted-map) (frequencies (map (comp count val) across))))
(flush)
(System/exit 0)
