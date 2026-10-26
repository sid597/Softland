;; A throwaway run of the decision-2 agent, 15 October 2026, at HEAD c90c7bcc.
;; Not code of the road and not an adapter. It does two things:
;;   1. It scans every tracked .clj, .cljc and .md file at HEAD and counts how
;;      the ends of decision 2 meet the store's bounds: 256 facts in an act,
;;      65,536 bytes in a value, 256 characters in the text of an id, and the
;;      form of an id and a key (softland.store.act).
;;   2. It gives a few more act maps, beside the walk's, to the act's pure
;;      check (softland.store.act/parse) and the door's pure check
;;      (softland.store.door.declaration/check), and one query to the read
;;      path's check.
;; Every id, key and session in here is an instance value. Nothing here is a
;; decision. It never reads src/app/server/env.clj nor any file named raw-*.
;; Run from the repository root:
;;   clojure -M:store src/proposal/first-maker-2026-10-14/runs/decision-2-maps.clj

(require '[softland.store.act :as act]
         '[softland.store.door.declaration :as declaration]
         '[softland.store.read-path.query :as query]
         '[clojure.java.shell :refer [sh]]
         '[clojure.string :as str])

(def rev "c90c7bcc")

(defn git-show [path]
  (let [{:keys [exit out]} (sh "git" "show" (str rev ":" path))]
    (when (zero? exit) out)))

(def tracked
  (->> (str/split-lines (:out (sh "git" "ls-files")))
       (filter #(re-find #"\.(clj|cljc|md)$" %))
       (remove #(str/ends-with? % "src/app/server/env.clj"))
       (remove #(str/starts-with? (last (str/split % #"/")) "raw-"))
       vec))

(defn utf8-bytes [^String s] (count (.getBytes s "UTF-8")))

;; ------------------------------------------------------------ .clj, .cljc

(def resolver
  "Reads ::alias/k and syntax-quote without loading anything."
  (reify clojure.lang.LispReader$Resolver
    (currentNS [_] 'user)
    (resolveClass [_ s] s)
    (resolveAlias [_ s] s)
    (resolveVar [_ s] s)))

(defn top-forms
  "The top-level forms of a text, each with its text from its first line to
  the line before the next form. Nil when the reader cannot read it."
  [text]
  (try
    (let [rdr (clojure.lang.LineNumberingPushbackReader. (java.io.StringReader. text))
          forms (binding [*read-eval* false
                          *reader-resolver* resolver
                          *default-data-reader-fn* (fn [_ v] v)]
                  (loop [acc []]
                    (let [f (read {:eof ::eof :read-cond :allow} rdr)]
                      (if (= ::eof f) acc (recur (conj acc f))))))
          forms (filter #(and (seq? %) (:line (meta %))) forms)
          lines (str/split-lines text)
          starts (mapv #(:line (meta %)) forms)
          ends (conj (mapv dec (rest starts)) (count lines))]
      (mapv (fn [f s e] {:form f :text (str/join "\n" (subvec lines (dec s) (max (dec s) e)))})
            forms starts ends))
    (catch Throwable _ nil)))

(defn clj-file [path]
  (let [text (git-show path)
        forms (top-forms text)]
    (if (nil? forms)
      {:path path :unread true}
      (let [ns-form (first (filter #(= 'ns (first (:form %))) forms))
            ns-name (when ns-form (str (second (:form ns-form))))
            named (for [{:keys [form]} forms
                        :when (and (not= 'ns (first form)) (symbol? (second form)))]
                    (name (second form)))
            unnamed (count (filter #(and (not= 'ns (first (:form %))) (not (symbol? (second (:form %))))) forms))
            ids (for [n named :when ns-name] (keyword ns-name n))]
        {:path path
         :bytes (utf8-bytes text)
         :forms (count forms)
         :named (count named)
         :unnamed unnamed
         :no-ns (nil? ns-name)
         :dup-names (count (filter #(> (val %) 1) (frequencies named)))
         :facts-cut-a (+ 2 (if ns-name 2 0) (* 4 (count named)) (* 4 unnamed))
         :largest-form (apply max 0 (map (comp utf8-bytes :text) forms))
         :ids-not-readable (count (remove act/readable-keyword? ids))
         :ids-too-long (count (remove declaration/id-within-the-limit? ids))}))))

;; ------------------------------------------------------------------- .md

(defn md-file
  "Leaf sections: from one heading to the next heading of any level, fences
  skipped. Paragraphs: runs of non-blank lines outside fences."
  [path]
  (let [text (git-show path)
        lines (str/split-lines text)
        step (fn [{:keys [fence] :as st} l]
               (let [fence-line? (re-find #"^\s{0,3}(```|~~~)" l)]
                 (cond
                   fence-line? (assoc st :fence (not fence) :cur (conj (:cur st) l))
                   (and (not fence) (re-find #"^#{1,6}\s" l))
                   (let [level (count (re-find #"^#+" l))
                         title (str/trim (str/replace l #"^#+\s" ""))
                         stack (conj (vec (take-while #(< (first %) level) (:stack st))) [level title])]
                     (-> st
                         (update :sections conj (:cur st))
                         (assoc :cur [l] :stack stack)
                         (update :titles conj title)
                         (update :paths conj (mapv second stack))))
                   :else (update st :cur conj l))))
        st (reduce step {:fence false :sections [] :cur [] :stack [] :titles [] :paths []} lines)
        sections (map #(str/join "\n" %) (conj (:sections st) (:cur st)))
        paragraphs (->> (str/split text #"\n\s*\n") (remove str/blank?))]
    {:path path
     :bytes (utf8-bytes text)
     :headings (count (:titles st))
     :largest-section (apply max 0 (map utf8-bytes sections))
     :dup-titles (count (filter #(> (val %) 1) (frequencies (:titles st))))
     :dup-paths (count (filter #(> (val %) 1) (frequencies (:paths st))))
     :paragraphs (count paragraphs)
     :largest-paragraph (apply max 0 (map utf8-bytes paragraphs))}))

;; ------------------------------------------------------ the scan, printed

(def clj (mapv clj-file (filter #(re-find #"\.cljc?$" %) tracked)))
(def md (mapv md-file (filter #(str/ends-with? % ".md") tracked)))
(def read-clj (remove :unread clj))
(def bound declaration/max-value-bytes)
(def facts declaration/max-facts)

(println "SCAN at" rev ":" (count clj) "code files," (count md) "markdown files")
(println "CODE  unreadable by the reader here:" (count (filter :unread clj)))
(println "CODE  whole file over" bound "bytes:" (count (filter #(> (:bytes %) bound) read-clj)))
(println "CODE  cut A (file, ns, each top-level form, 4 facts each), one act per file, over" facts "facts:"
         (count (filter #(> (:facts-cut-a %) facts) read-clj))
         "; most:" (apply max (map :facts-cut-a read-clj)))
(println "CODE  one top-level form over" bound "bytes:" (count (filter #(> (:largest-form %) bound) read-clj))
         "; largest form bytes:" (apply max (map :largest-form read-clj)))
(println "CODE  files with no ns form:" (count (filter :no-ns read-clj)))
(println "CODE  top-level forms with no name (comment, do, require ...):" (reduce + (map :unnamed read-clj))
         "in" (count (filter #(pos? (:unnamed %)) read-clj)) "files; named:" (reduce + (map :named read-clj)))
(println "CODE  files where one name is defined twice or more:" (count (filter #(pos? (:dup-names %)) read-clj)))
(println "CODE  ids :<ns>/<name> that are no readable keyword:" (reduce + (map :ids-not-readable read-clj))
         "; over the id text limit" declaration/max-id-text ":" (reduce + (map :ids-too-long read-clj)))
(println "MD    whole file over" bound "bytes:" (count (filter #(> (:bytes %) bound) md)))
(println "MD    a section (heading to next heading) over" bound "bytes:"
         (count (filter #(> (:largest-section %) bound) md))
         "; largest section bytes:" (apply max (map :largest-section md)))
(println "MD    a paragraph over" bound "bytes:" (count (filter #(> (:largest-paragraph %) bound) md))
         "; largest paragraph bytes:" (apply max (map :largest-paragraph md)))
(println "MD    one act per file at sections (4 facts each) over" facts "facts:"
         (count (filter #(> (+ 2 (* 4 (:headings %))) facts) md)))
(println "MD    one act per file at paragraphs (4 facts each) over" facts "facts:"
         (count (filter #(> (+ 2 (* 4 (:paragraphs %))) facts) md)))
(println "MD    files where one heading text is used twice or more:" (count (filter #(pos? (:dup-titles %)) md)))
(println "MD    files where one whole heading path is used twice or more:" (count (filter #(pos? (:dup-paths %)) md)))

;; ------------------------------------------------- paths as keyword ids

(def path-keywords
  (for [p tracked] [p (keyword "file" (str/replace p "/" "."))]))
(println "PATH  as :file/<path with / as .>: not readable"
         (count (remove (comp act/readable-keyword? second) path-keywords))
         "; over the id text limit" (count (remove (comp declaration/id-within-the-limit? second) path-keywords))
         "; two paths on one keyword"
         (count (filter #(> (val %) 1) (frequencies (map second path-keywords)))))

;; ---------------------------------------------------------- act maps

(def sys {:version 1 :who :system :layer :base :permission [:system :base :base]
          :session :decision-2-instance-session})
(defn arrived [m] (assoc m :act-id [:base :microbatch #uuid "d2000000-0000-4000-8000-000000000001"]
                         :depot :microbatch declaration/time-part 1760500000000))
(defn word [r] (if (:ok r) :ok (:refuse r)))

(def stamp-path "src-store/softland/store/gate/stamp.clj")
(def blob-after (str/trim (:out (sh "git" "rev-parse" (str "0e6182b9:" stamp-path)))))
(def blob-before (str/trim (:out (sh "git" "rev-parse" (str "bb6b6cc1:" stamp-path)))))
(def file-uuid #uuid "3f0c9a52-1d1e-4c7b-9a51-6c2f0b1e7d10")

(def maps
  [{:n "d2-1" :note "id = git's blob id, as a keyword: stamp.clj after 0e6182b9"
    :act (assoc sys :facts [{:e (keyword "blob" blob-after) :k :file/path :v stamp-path}])}
   {:n "d2-2" :note "id = git's blob id, as a keyword: stamp.clj before 0e6182b9"
    :act (assoc sys :facts [{:e (keyword "blob" blob-before) :k :file/path :v stamp-path}])}
   {:n "d2-3" :note "id = a random UUID; the path and the blob are facts on it; the base marks :file/path and :code/in"
    :act (assoc sys :facts [{:e file-uuid :k :file/path :v stamp-path}
                            {:e file-uuid :k :file/blob :v blob-after}
                            {:e :base :k :softland.store/marked-keys :v #{:file/path :code/in}}])}
   {:n "d2-4" :note "a key that is already an id: the word and grammar of :code/doc as facts on it"
    :act (assoc sys :facts [{:e :code/doc :k :key/word :v "doc"}
                            {:e :code/doc :k :key/grammar :v :string}
                            {:e :key/word :k :key/word :v "word"}])}
   {:n "d2-5" :note "the bottom of keys as ids, put in the store's namespace"
    :act (assoc sys :facts [{:e :code/doc :k :softland.store/word :v "doc"}])}
   {:n "d2-6" :note "a bare key, no vocabulary named"
    :act (assoc sys :facts [{:e :softland.store.gate.stamp/next-stamp :k :doc :v "The stamp of one decision."}])}])

(println "ACT MAPS")
(doseq [{:keys [n note act]} maps]
  (println (format "  %-5s parse %-12s door-check %-24s %s"
                   n (pr-str (word (act/parse act))) (pr-str (word (declaration/check (arrived act)))) note)))
(println "  blob ids:" (subs blob-before 0 8) (subs blob-after 0 8))


;; ------------------------------------- cut A over two of the store's own files

(defn cut-a
  "The walk's cut A (walk-parse.clj clj-facts) over a file at HEAD: the file,
  the namespace and each named top-level form are entities; ids a UUID for
  the file, :<ns> and :<ns>/<name>; four facts on each form."
  [path]
  (let [forms (top-forms (git-show path))
        ns-form (first (filter #(= 'ns (first (:form %))) forms))
        ns-id (keyword (str (second (:form ns-form))))
        f file-uuid]
    (into [{:e f :k :file/path :v path}
           {:e ns-id :k :code/in-file :v f}]
          (mapcat (fn [{:keys [form text]}]
                    (let [d (keyword (name ns-id) (name (second form)))]
                      [{:e d :k :code/in :v ns-id}
                       {:e d :k :code/kind :v (keyword (name (first form)))}
                       {:e d :k :code/doc :v (let [x (nth form 2 nil)] (when (string? x) x))}
                       {:e d :k :code/text :v text}]))
                  (filter #(and (not= 'ns (first (:form %))) (symbol? (second (:form %)))) forms)))))

(doseq [[n path] [["d2-7" "src-store/softland/store/act.clj"]
                  ["d2-8" "test-store/softland/store/pure_proof.clj"]]]
  (let [a (assoc sys :facts (cut-a path))]
    (println (format "  %-5s parse %-12s door-check %-24s cut A over %s, one act, %d facts"
                     n (pr-str (word (act/parse a))) (pr-str (word (declaration/check (arrived a))))
                     path (count (:facts a))))))

(def q {:asks [:run [:softland.store.gate.stamp] [:softland.store.gate.stamp0]] :asker :system :layers [:base]})
(println "QUERY d2-q1 check" (pr-str (let [r (query/check q)] (if (:ok r) :ok [(:refuse r) (:at r)])))
         ": a run from the namespace's id to the id just after its definitions")
(def q2 {:asks [:points-at :file/path stamp-path] :asker :system :layers [:base]})
(println "QUERY d2-q2 check" (pr-str (let [r (query/check q2)] (if (:ok r) :ok [(:refuse r) (:at r)])))
         ": what points at a path under :file/path")
(shutdown-agents)
