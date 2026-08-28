(require '[clojure.java.io :as io]
         '[clojure.set :as set]
         '[clojure.string :as str])

(def ^:private server-root "src/app/server")

(def ^:private allowed-edges
  [{:id "E1"
    :from 'app.server.worn.material-truth
    :to 'app.server.episode.episode
    :file "src/app/server/worn/material_truth.clj"}
   {:id "E2"
    :from 'app.server.worn.binding-material
    :to 'app.server.page.verb-registry
    :file "src/app/server/worn/binding_material.cljc"}
   {:id "E3"
    :from 'app.server.episode.cascade
    :to 'app.server.door.server-jetty
    :file "src/app/server/episode/cascade.clj"}
   {:id "E4"
    :from 'app.server.ingest.transcript
    :to 'app.server.episode.llm
    :file "src/app/server/ingest/transcript.clj"}
   {:id "E5"
    :from 'app.server.rama.transcript-ingest
    :to 'app.server.ingest.transcript
    :file "src/app/server/rama/transcript_ingest.clj"}])

(defn- source-file?
  [^java.io.File file]
  (and (.isFile file)
       (or (str/ends-with? (.getName file) ".clj")
           (str/ends-with? (.getName file) ".cljc"))
       (not= "env.clj" (.getName file))))

(defn- server-files
  []
  (->> (file-seq (io/file server-root))
       (filter source-file?)
       (sort-by #(.getPath ^java.io.File %))))

(defn- read-ns
  [^java.io.File file]
  (with-open [reader (clojure.lang.LineNumberingPushbackReader.
                      (io/reader file))]
    (let [form (read {:read-cond :preserve
                      :features #{:clj}}
                     reader)]
      {:namespace (second form)
       :end-line (.getLineNumber reader)})))

(defn- tier-for-file
  [path]
  (let [relative (subs path (inc (count server-root)))]
    (cond
      (or (str/starts-with? relative "door/")
          (str/starts-with? relative "tools/"))
      {:rank 5 :label "3"}

      (or (str/starts-with? relative "page/")
          (= relative "rama/trail_view.clj"))
      {:rank 4 :label "2p"}

      (str/starts-with? relative "episode/")
      {:rank 3 :label "2e"}

      (or (str/starts-with? relative "worn/")
          (= relative "rama/face_arsenal.clj"))
      {:rank 2 :label "2w"}

      (str/starts-with? relative "ingest/")
      {:rank 1 :label "1"}

      (str/starts-with? relative "rama/")
      {:rank 0 :label "0"}

      :else
      (throw (ex-info "Unclassified server source path" {:path path})))))

(def ^:private required-namespace-pattern
  #"(?<![A-Za-z0-9_.-])(app\.server(?:\.[A-Za-z0-9_-]+)+)")

(def ^:private quoted-namespace-pattern
  #"'(app\.server(?:\.[A-Za-z0-9_-]+)+)/[A-Za-z0-9_!?*+<>=.-]+")

(defn- matches-on-line
  [pattern line]
  (map (comp symbol second) (re-seq pattern line)))

(defn- dependency-edges
  [^java.io.File file namespace end-line]
  (let [path (.getPath file)
        lines (str/split-lines (slurp file))
        ns-lines (take end-line lines)
        require-index (first (keep-indexed
                              (fn [index line]
                                (when (str/includes? line ":require") index))
                              ns-lines))
        require-edges
        (when require-index
          (for [index (range require-index (count ns-lines))
                target (matches-on-line required-namespace-pattern
                                        (nth ns-lines index))]
            {:from namespace
             :to target
             :file path
             :line (inc index)}))
        quoted-edges
        (for [index (range end-line (count lines))
              target (matches-on-line quoted-namespace-pattern
                                      (nth lines index))]
          {:from namespace
           :to target
           :file path
           :line (inc index)})]
    (concat require-edges quoted-edges)))

(defn- edge-key
  [{:keys [from to file]}]
  [from to file])

(defn- format-edge
  [{:keys [from to file line from-tier to-tier]}]
  (format "%s → %s  (tier %s → tier %s)  %s:%d"
          from to (:label from-tier) (:label to-tier) file line))

(let [files (server-files)
      source-rows (mapv (fn [file]
                          (let [{:keys [namespace end-line]} (read-ns file)
                                path (.getPath ^java.io.File file)]
                            {:file file
                             :path path
                             :namespace namespace
                             :end-line end-line
                             :tier (tier-for-file path)}))
                        files)
      namespace-tiers (into {'app.server.env {:rank 0 :label "0"}}
                            (map (juxt :namespace :tier) source-rows))
      all-edges (->> source-rows
                     (mapcat (fn [{:keys [file namespace end-line tier]}]
                               (for [edge (dependency-edges file namespace end-line)]
                                 (assoc edge
                                        :from-tier tier
                                        :to-tier (get namespace-tiers (:to edge))))))
                     distinct)
      unknown-edges (filter #(nil? (:to-tier %)) all-edges)
      upward-edges (filter #(and (:to-tier %)
                                 (> (get-in % [:to-tier :rank])
                                    (get-in % [:from-tier :rank])))
                           all-edges)
      upward-by-key (into {} (map (juxt edge-key identity) upward-edges))
      allowed-by-key (into {} (map (juxt edge-key identity) allowed-edges))
      allowed-keys (set (keys allowed-by-key))
      upward-keys (set (keys upward-by-key))
      present-keys (set/intersection allowed-keys upward-keys)
      missing-keys (set/difference allowed-keys upward-keys)
      extra-keys (set/difference upward-keys allowed-keys)
      allowed-id-by-key (into {} (map (juxt edge-key :id) allowed-edges))]
  (println "ALLOWED")
  (doseq [edge allowed-edges
          :let [key (edge-key edge)]
          :when (contains? present-keys key)]
    (println (:id edge) (format-edge (get upward-by-key key))))
  (println "VIOLATION")
  (if (and (empty? missing-keys)
           (empty? extra-keys)
           (empty? unknown-edges))
    (println "none")
    (do
      (doseq [key (sort-by str missing-keys)]
        (println "MISSING" (allowed-id-by-key key) (pr-str key)))
      (doseq [key (sort-by str extra-keys)]
        (println "EXTRA" (format-edge (get upward-by-key key))))
      (doseq [edge (sort-by (juxt :file :line) unknown-edges)]
        (println "UNKNOWN" (:from edge) "→" (:to edge)
                 (str (:file edge) ":" (:line edge))))
      (System/exit 1))))
