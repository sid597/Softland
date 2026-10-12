;; Review helper for R6: every keyword that the read path's code holds, against the lists of words.
(require '[clojure.java.io :as io] '[clojure.string :as str] '[clojure.walk :as walk])
(import '[java.io PushbackReader])
(defn forms [f]
  (with-open [r (PushbackReader. (io/reader f))]
    (binding [*read-eval* false *default-data-reader-fn* (fn [_ v] v)]
      (let [eof (Object.)]
        (loop [acc []] (let [x (read {:eof eof :read-cond :allow} r)] (if (identical? eof x) acc (recur (conj acc x)))))))))
(defn keywords-of [f]
  (let [found (atom #{})]
    (walk/postwalk (fn [x] (when (keyword? x) (swap! found conj x)) x)
                   ;; docstrings are strings, so they are not walked into
                   (forms f))
    @found))
(def lists (str (slurp "src-store/softland/store/read_path/README.md") "\n" (slurp "src-store/softland/store/README.md")
                "\n" (slurp "src-store/softland/store/door/README.md")))
(def clojure-own #{:require :import :as :refer :private :doc :else :keys :or :let :when :while :eof :read-cond :allow :tag :dynamic :all :const})
(doseq [f ["src-store/softland/store/read_path.clj" "src-store/softland/store/read_path/query.clj"
           "src-store/softland/store/read_path/layers.clj" "src-store/softland/store/read_path/sources.clj"
           "src-store/softland/store/read_path/answer.clj" "src-store/softland/store/read_path/words.clj"
           "src-store/softland/store/record/rows.clj" "src-store/softland/store/record/tables.clj" "src-store/softland/store/door/declaration.clj" "src-store/softland/store/gate/decision.clj" "src-store/softland/store/gate/batch.clj" "src-store/softland/store/gate/first_act.clj" "src-store/softland/store/act.clj" "src-store/softland/store/module/batch_clock.clj" "src-store/softland/store/module/microbatch_gate.clj" "src-store/softland/store/module/streaming_gate.clj" "src-store/softland/store/module/door_reads.clj"]]
  (let [ks (remove clojure-own (keywords-of f))
        missing (sort (remove (fn [k] (str/includes? lists (str "`" k "`"))) ks))]
    (println (format "%-52s keywords %3d, not on a list of words: %s" f (count ks) (pr-str missing)))))
