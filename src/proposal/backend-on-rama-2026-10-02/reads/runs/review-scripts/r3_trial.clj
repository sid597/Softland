;; Review helper for R3: try to make a query send Rama a path on a whole table.
;; Every thing below is handed to the check of a query; for each that is taken, every source that the
;; read could want in any layer of it is made, and every path that `path-of` builds for it is held to
;; this: a key, or a range whose two ends are texts that begin with the text of one layer and a space
;; or `!`, so the range cannot leave that layer.
(require '[softland.store.read-path :as rp]
         '[softland.store.read-path.query :as query]
         '[softland.store.read-path.sources :as sources]
         '[softland.store.door.declaration :as declaration]
         '[softland.store.record.rows :as rows])
(def numbers (query/numbers nil))
(def odd-ids [:a (keyword "") (keyword " ") (keyword "a b") (keyword "a" "b c") (keyword "!") (keyword "a!") (keyword "~") nil "" " " "a"
              (java.util.UUID. 0 0) 1 [] {} :softland.store/depot (keyword (apply str (repeat 300 "x")))
              (keyword "\u0000") (keyword "a\u0000") (keyword "￿") (keyword (String. (Character/toChars 0x1F600)))
              'sym :first :last (keyword "a\nb") (keyword "a\tb")])
(def asks (concat
           (for [e odd-ids k odd-ids] [:place e k])
           (for [e odd-ids] [:entity e])
           (for [a (concat odd-ids (map vector odd-ids) [[:a :k] :first :last [] [:a :b :c]])
                 b [:last :first [:z] [:a] [:a :k] nil [(keyword "")] [(keyword " ")]]]
             [:run a b])
           (for [k odd-ids v [:x nil "" (apply str (repeat 70000 "y"))]] [:points-at k v])
           [[:whole-table] [:run] [:run :first] [:table "$$streaming-facts"] nil [] [:place] :place
            [:run :first :last] [:run :first :last :more]]))
(def queries (for [a asks
                   layers [{:layers [:l]} {:layers [(keyword "")]} {:layers [(keyword " ")]} {:from-layer :l}
                           {:layers [:base]} {:layers [(keyword "!")]} {:layers []} {:layers [nil]}]]
               (merge {:asks a :asker :ann} layers)))
(def task-ids ["t0" "t1" "t2" "t3"])
(defn within-one-layer? [layer ^String text]
  (let [l (declaration/id-text layer)]
    (or (.startsWith text (str l " ")) (.startsWith text (str l "!")))))
(let [taken (keep (fn [q] (:ok (query/check q))) queries)
      all-sources (distinct
                   (for [q taken
                         layer (or (:layers q) [(:from-layer q)])
                         :let [marked {(rows/place-key layer layer :softland.store/marked-keys)
                                       {:level-since (pr-str {(nth (:asks q) 1) 7})}}]
                         source (concat [(sources/own-rows-source layer) (sources/permission-source layer (:asker q))]
                                        ((get sources/of-word (first (:asks q))) q layer marked task-ids))]
                     source))
      paths (for [s all-sources step [:whole :first :count :size :no-such-step]] [s step (#'rp/path-of s numbers step)])
      kinds (frequencies (map (fn [s] (first (:path s))) all-sources))
      bad (remove (fn [s] (let [[kind a b] (:path s)]
                            (and (#{:row :own-rows :run} kind)
                                 (string? a) (within-one-layer? (:layer s) a)
                                 (or (= :row kind) (and (string? b) (within-one-layer? (:layer s) b) (rows/before? a b))))))
                  all-sources)]
  (println "R3 things handed to the check:" (count queries) "| taken as a query:" (count taken) "| turned away:" (- (count queries) (count taken)))
  (println "R3 sources that those queries could want:" (count all-sources) "by kind" (pr-str kinds))
  (println "R3 sources whose path is not a key or a range inside one layer:" (count bad) (pr-str (take 3 bad)))
  (println "R3 paths built:" (count (remove #(nil? (nth % 2)) paths)) "| for a step that is not known:" (count (remove #(nil? (nth % 2)) (filter #(= :no-such-step (second %)) paths)))
           "| for a row or the own rows, any step but :whole:" (count (remove #(nil? (nth % 2)) (filter (fn [[s step]] (and (not= :run (first (:path s))) (not= :whole step))) paths))))
  (println "R3 a source of a kind that is not known:" (pr-str (mapv #(#'rp/path-of {:path %} numbers :whole) [[:whole-table] [:table "x"] [:run nil nil] [:run "a" nil] [:row nil] nil [:run]]))))
(shutdown-agents)
