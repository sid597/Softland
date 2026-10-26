;; The checker's own run, 15 October 2026. Of the tracked .clj/.cljc/.md files
;; at HEAD (read from git, not the working tree), how many exceed 65,536 bytes
;; raw, and how many exceed it as the store measures a value: the UTF-8 bytes
;; of softland.store.door.text/text of the file's whole text as one string.
;; Prints only. Run from the repository root:
;;   clojure -M:store src/proposal/first-maker-2026-10-14/checks/checker-d1-sizes-as-text.clj
(require '[softland.store.door.text :as t] '[clojure.java.shell :refer [sh]] '[clojure.string :as str])
(def files (->> (str/split-lines (:out (sh "git" "ls-files" "*.clj" "*.cljc" "*.md"))) (remove str/blank?)))
(def rows (for [f files
                :let [{:keys [exit out]} (sh "git" "show" (str "HEAD:" f))]
                :when (zero? exit)]
            [f (count (.getBytes ^String out "UTF-8")) (count (.getBytes ^String (t/text out) "UTF-8"))]))
(println "files at HEAD:" (count rows))
(println "over 65536 raw:" (count (filter #(> (nth % 1) 65536) rows)))
(println "over 65536 as the value's text:" (count (filter #(> (nth % 2) 65536) rows)))
(println "README as text:" (some #(when (= "src-store/README.md" (first %)) (nth % 2)) rows))
