;; Review helper: each declared word of a class, and how many times each proof names it.
(require '[clojure.string :as str]
         '[softland.store.act :as act]
         '[softland.store.door.declaration :as declaration]
         '[softland.store.read-path.words :as words]
         '[softland.store.read-path.query :as query])
(def proofs {:pure (slurp "test-store/softland/store/pure_proof.clj")
             :act (slurp "test-store/softland/store/act_proof.clj")
             :module (slurp "test-store/softland/store/module_proof.clj")
             :door (slurp "test-store/softland/store/door_proof.clj")
             :rp-pure (slurp "test-store/softland/store/read_path_pure_proof.clj")
             :rp (slurp "test-store/softland/store/read_path_proof.clj")})
(defn times [text k] (count (re-seq (re-pattern (str (java.util.regex.Pattern/quote (str k)) "(?![a-zA-Z0-9?!*+'-])")) text)))
(defn report [what ks where]
  (let [rows (for [k (sort ks)] [k (mapv #(times (get proofs %) k) where)])
        none (filter (fn [[_ ns]] (every? zero? ns)) rows)
        not-on-the-cluster (filter (fn [[_ ns]] (zero? (last ns))) rows)]
    (println (format "%-58s %3d words | named by none of %s: %s | not named by the last of them: %s"
                     what (count rows) (pr-str where) (pr-str (mapv first none)) (pr-str (mapv first not-on-the-cluster))))))
(report "the read path's own words" (keys words/words) [:rp-pure :rp])
(report "the reasons for which a live read ends" (keys words/because) [:rp-pure :rp])
(report "the words for a layer that is left out" (keys words/left-out-words) [:rp-pure :rp])
(report "the words with which the check of a query turns away" query/reasons [:rp-pure :rp])
(report "the numbers of the read path" (keys query/default-numbers) [:rp-pure :rp])
(report "the act's reasons" act/reasons [:act :pure :module])
(report "the declaration's reasons" declaration/reasons [:pure :module])
(report "the words of a stray" declaration/stray-reasons [:pure :module])
