;; A throwaway run of the decision-2 agent, 15 October 2026, at HEAD c90c7bcc,
;; for the corrections after review (checks/review-d2-report.md). Pure: no
;; cluster. It loads runs/decision-2-maps.clj for its scan and its helpers,
;; then:
;;   1. d2-3 split in two acts, the base's mark first and the file's facts
;;      after: parse and the door's check on each, and the gate's own
;;      since-by-place (gate/decision.clj), which says which places of an act
;;      go into the order by value, with the marks as they were before it.
;;      The same for d2-3 as one act.
;;   2. Names repeated across files: :<ns>/<name> keywords defined in two
;;      files or more, by Clojure's reader (the scan of decision-2-maps.clj
;;      counted repeats inside one file only).
;;   3. A relation held as a set: the value digest of #{"296a2a7b"} and of
;;      "296a2a7b" (record/rows.clj value-digest-of), the text that the order
;;      by value is keyed on.
;; Run from the repository root:
;;   clojure -M:store src/proposal/first-maker-2026-10-14/runs/decision-2-fixes.clj

(alter-var-root (var clojure.core/shutdown-agents) (constantly (fn [])))
(binding [*out* (java.io.StringWriter.)]
  (load-file "src/proposal/first-maker-2026-10-14/runs/decision-2-maps.clj"))
(require '[softland.store.gate.decision :as decision]
         '[softland.store.record.rows :as rows])

(def marks #{:file/path :code/in})
(def mark-fact {:e :base :k :softland.store/marked-keys :v marks})
(def file-facts [{:e file-uuid :k :file/path :v stamp-path}
                 {:e file-uuid :k :file/blob :v blob-after}])

(def one-act (assoc sys :facts (into [mark-fact] file-facts)))
(def act-a (assoc sys :facts [mark-fact]))
(def act-b (assoc sys :facts file-facts))

(defn arrived-n [m n]
  (assoc m :act-id [:base :microbatch (java.util.UUID/fromString (format "d2000000-0000-4000-8000-%012d" n))]
         :depot :microbatch declaration/time-part 1760500000000))
(defn checked [m n] (:ok (declaration/check (arrived-n m n))))

;; The row at the base's marks place after act-a, as a gate keeps it: each
;; marked key with the stamp since which it is level (rows/marked-keys-of reads
;; :level-since). Instance: act-a decided at stamp 1.
(def marks-row-after-a {:level-since (pr-str (into {} (map (fn [k] [k 1])) marks))})

(println "SPLIT d2-3: the mark first, the file after")
(doseq [[n label m] [["d2-3"  "one act: the mark and the file" one-act]
                     ["d2-3a" "act 1 of 2: the mark alone" act-a]
                     ["d2-3b" "act 2 of 2: the file alone" act-b]]]
  (println (format "  %-6s parse %-6s door-check %-6s %s"
                   n (pr-str (word (act/parse m))) (pr-str (word (declaration/check (arrived-n m 3)))) label)))
(println "  since-by-place, d2-3 as one act, marks before it none:"
         (pr-str (decision/since-by-place (checked one-act 3) nil)))
(println "  since-by-place, d2-3b after d2-3a:"
         (pr-str (decision/since-by-place (checked act-b 4) marks-row-after-a)))
(println "  marked-keys-of that row:" (pr-str (rows/marked-keys-of marks-row-after-a)))

;; ---------------------------------------- names repeated across files

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
(println "ACROSS FILES: :<ns>/<name> ids defined in two files or more:" (count across)
         "; in three or more:" (count (filter #(> (count (val %)) 2) across)))
(println "  their namespaces:" (pr-str (sort (set (map (comp namespace key) across)))))
(println "  e.g." (pr-str (first (sort-by (comp str key) across))))

;; ---------------------------------------- a relation held as a set

(println "SET VALUE: digest of #{\"296a2a7b\"}" (subs (rows/value-digest-of #{"296a2a7b"}) 0 16)
         "| digest of \"296a2a7b\"" (subs (rows/value-digest-of "296a2a7b") 0 16)
         "| equal:" (= (rows/value-digest-of #{"296a2a7b"}) (rows/value-digest-of "296a2a7b")))
(flush)
(System/exit 0)
