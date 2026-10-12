;; Review helper: every top-level def of the store's source and proofs, and whether it has a docstring.
(require '[clojure.java.io :as io] '[clojure.string :as str])
(import '[java.io PushbackReader])
(defn forms [f]
  (with-open [r (PushbackReader. (io/reader f))]
    (binding [*read-eval* false *default-data-reader-fn* (fn [_ v] v)]
      (let [eof (Object.)]
        (loop [acc []]
          (let [x (try (read {:eof eof :read-cond :allow} r) (catch Throwable t [:unreadable (.getMessage t)]))]
            (cond (identical? eof x) acc
                  (and (vector? x) (= :unreadable (first x))) (conj acc x)
                  :else (recur (conj acc x)))))))))
(defn doc? [form]
  (let [[head nm & more] form]
    (cond
      (#{'ns} head) (string? (first more))
      (#{'defn 'defn- 'defmacro} head) (or (string? (first more)) (:doc (meta nm)))
      (#{'def 'defonce} head) (or (and (string? (first more)) (seq (rest more))) (:doc (meta nm)))
      (#{'deftest} head) true
      :else true)))
(doseq [root ["src-store" "test-store"]
        f (sort (filter #(str/ends-with? (.getName %) ".clj") (file-seq (io/file root))))]
  (let [fs (forms f)
        defs (filter #(and (seq? %) (#{'ns 'defn 'defn- 'defmacro 'def 'defonce} (first %))) fs)
        missing (remove doc? defs)]
    (println (format "%-62s defs %3d  without a docstring %d %s" (str f) (count defs) (count missing)
                     (if (seq missing) (pr-str (mapv second missing)) "")))
    (doseq [u (filter #(and (vector? %) (= :unreadable (first %))) fs)] (println "   UNREADABLE" u))))
