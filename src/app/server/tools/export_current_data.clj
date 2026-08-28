(ns app.server.tools.export-current-data
  "A read-only archive writer for all 44 durable PStates.
   Takes: an output directory and foreign handles to the five pinned Rama modules.
   Gives: EDNL PState files, manifest.edn, verification.edn, and SHA256SUMS.
   Holds nothing."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            ;; Nippy needs these existing record classes loaded before a
            ;; subindexed value can be thawed. Requiring the definitions is
            ;; local JVM setup only; it does not launch or mutate a module.
            [app.server.rama.object-container]
            [app.server.rama.relation-kernel]
            [app.server.rama.face-arsenal])
  (:import [java.io File]
           [java.nio.charset StandardCharsets]
           [java.nio.file Files LinkOption Path Paths]
           [java.security MessageDigest]
           [java.time Instant]))

(def task-count 8)
(def root-page-size 128)

(def ^:private object-container-module
  "app.server.rama.object-container/object-container-module")
(def ^:private transcript-ops-module
  "app.server.rama.object-container/object-container-transcript-ops-module")
(def ^:private relation-kernel-module
  "app.server.rama.relation-kernel/relation-kernel-module")
(def ^:private trail-view-module
  "app.server.rama.trail-view/trail-view-module")
(def ^:private face-arsenal-module
  "app.server.rama.face-arsenal/face-arsenal-module")

(defn- pstate
  [name shape value-type]
  (cond-> {:name name :shape shape :value-type value-type}
    (= :nested shape) (assoc :inner-storage :subindexed)))

(def module-specs
  [{:module object-container-module
    :role :truth-owner
    :pstates
    [(pstate "$$requests-by-audit-id" :flat "ObjectContainerRequestRow")
     (pstate "$$decisions-by-audit-id" :flat "ObjectContainerDecisionRow")
     (pstate "$$decisions-by-idempotency" :nested "ObjectContainerDecisionRow")
     (pstate "$$events-by-id" :flat "ObjectContainerEventRow")
     (pstate "$$import-completions-by-key" :flat "ImportCompletionRow")
     (pstate "$$source-artifacts-by-id" :flat "SourceArtifactRow")
     (pstate "$$source-versions-by-ref" :nested "SourceVersionRow")
     (pstate "$$source-latest-by-ref" :flat "SourceVersionRow")
     (pstate "$$source-ingest-completions-by-ref" :nested "SourceIngestCompletionRow")
     (pstate "$$containers-by-id" :flat "ObjectContainerRow")
     (pstate "$$revisions-by-id" :flat "RevisionRow")
     (pstate "$$revision-history-by-container" :nested "RevisionRow")
     (pstate "$$derived-units-by-id" :flat "DerivedUnitRow")
     (pstate "$$unit-graduations-by-id" :flat "UnitGraduationRow")
     (pstate "$$source-anchors-by-target" :nested "SourceAnchorRow")
     (pstate "$$composition-children-by-parent" :nested "CompositionEdgeRow")
     (pstate "$$composition-parent-by-child" :nested "CompositionEdgeRow")
     (pstate "$$source-containers-by-source" :nested "SourceMaterialRefRow")
     (pstate "$$source-derived-units-by-source" :nested "SourceMaterialRefRow")
     (pstate "$$source-anchors-by-source" :nested "SourceMaterialRefRow")
     (pstate "$$source-edges-by-source" :nested "SourceMaterialRefRow")
     (pstate "$$native-identity-claims-by-container" :flat "NativeIdentityClaimRow")
     (pstate "$$outline-by-document" :nested "OutlineNodeRow")
     (pstate "$$transcript-conversation-projection" :nested "TranscriptConversationProjectionRow")
     (pstate "$$transcript-tool-calls-by-name" :nested "TranscriptToolCallIndexRow")
     (pstate "$$transcript-audit-by-request" :nested "TranscriptAuditEntryRow")
     (pstate "$$transcript-last-message-by-conversation" :flat "TranscriptLastMessageRow")
     (pstate "$$transcript-source-lines-by-file" :nested "TranscriptSourceLineStatusRow")
     (pstate "$$edit-order-by-target" :nested "EditOrderRow")]}
   {:module transcript-ops-module
    :role :truth-owner
    :pstates
    [(pstate "$$transcript-runs" :flat "TranscriptRunRow")
     (pstate "$$transcript-file-offsets" :flat "TranscriptFileOffsetRow")
     (pstate "$$transcript-file-source-lines-by-file" :nested "TranscriptSourceLineStatusRow")]}
   {:module relation-kernel-module
    :role :truth-owner
    :pstates
    [(pstate "$$relation-decisions-by-idempotency" :nested "RelationDecisionRow")
     (pstate "$$relation-decisions-by-id" :flat "RelationDecisionRow")
     (pstate "$$relation-events-by-id" :flat "RelationEventRow")
     (pstate "$$relations-by-id" :flat "RelationEdgeRow")
     (pstate "$$relation-status-log-by-relation" :nested "RelationStatusLogRow")
     (pstate "$$relations-by-target" :nested "RelationEdgeRow")
     (assoc (pstate "$$relation-target-descriptors" :nested "RelationTargetDescriptorRow")
            :inner-storage :plain-map)
     (pstate "$$relation-activity-by-bucket" :nested "RelationActivityRow")]}
   {:module trail-view-module
    :role :mirror-only
    :pstates []}
   {:module face-arsenal-module
    :role :truth-owner
    :pstates
    [(pstate "$$faces-by-name" :nested "FaceRegistryRow")
     (pstate "$$wear-events-by-face" :nested "WearEventRow")
     (pstate "$$wear-counts-by-face" :flat "WearCountRow")
     (pstate "$$wear-journal-by-face" :nested "String")]}])

(defn expected-module-names [] (set (map :module module-specs)))
(defn pstate-specs []
  (mapcat (fn [{:keys [module role pstates]}]
            (map #(assoc % :module module :module-role role) pstates))
          module-specs))

(defn plain-edn
  "Convert runtime values to portable EDN without hiding failed deserialization."
  [x]
  (walk/postwalk
   (fn [value]
     (if-let [[_ failure]
              (when (map? value)
                (some (fn [[k :as entry]]
                        (when (= :nippy/unthawable k) entry))
                      value))]
       (throw (ex-info "Rama returned an unthawable stored value; export aborted"
                       {:failure-class (or (:throwable/class failure)
                                           (some-> failure class .getName))
                        :failure-message (or (:throwable/message failure)
                                             (when (instance? Throwable failure)
                                               (.getMessage ^Throwable failure)))}))
       (cond
         (record? value)
         (into {} value)

         (instance? Throwable value)
         {:archive/type :throwable
          :throwable/class (.getName (class value))
          :throwable/message (.getMessage ^Throwable value)
          :throwable/data (ex-data value)
          :throwable/cause-class (some-> ^Throwable value .getCause class .getName)
          :throwable/cause-message (some-> ^Throwable value .getCause .getMessage)
          :throwable/stacktrace (mapv str (.getStackTrace ^Throwable value))}

         :else value)))
   x))

(defn- path-of ^Path [s]
  (.normalize (.toAbsolutePath (Paths/get (str s) (make-array String 0)))))

(defn safe-output-path?
  [repo-root output]
  (let [out (path-of output)
        repo (path-of repo-root)
        rama-root (path-of "/mnt/data/rama")]
    (and (not= out repo)
         (not= out rama-root)
         (not (.startsWith out repo))
         (not (.startsWith out rama-root))
         (not= out (.getRoot out)))))

(defn assert-safe-output!
  [repo-root output]
  (when-not (safe-output-path? repo-root output)
    (throw (ex-info "Archive destination must be outside Git and /mnt/data/rama"
                    {:repo-root (str (path-of repo-root))
                     :output (str (path-of output))})))
  (when (Files/exists (path-of output) (make-array LinkOption 0))
    (throw (ex-info "Archive destination already exists; refusing overwrite"
                    {:output (str (path-of output))})))
  (path-of output))

(defn partition-keys
  "Return one inert String key for every hash partition, ordered by task id."
  [n]
  (loop [candidate 0 found {}]
    (if (= n (count found))
      (mapv found (range n))
      (let [k (str "archive-census-key-" candidate)
            idx (mod (hash k) n)]
        (recur (inc candidate) (if (contains? found idx) found (assoc found idx k)))))))

(defn- canonical-compare [a b]
  (compare (pr-str a) (pr-str b)))

(defn- canonical-map [entries]
  (into (sorted-map-by canonical-compare) entries))

(defn edn-line
  "Serialize and immediately prove that one archive row is readable EDN."
  [row]
  (let [normalized (plain-edn row)
        line (pr-str normalized)
        reread (edn/read-string line)]
    (when-not (= normalized reread)
      (throw (ex-info "EDN row failed exact readback" {:row normalized :reread reread})))
    line))

(defn- sorted-entries [entries]
  (sort-by (comp pr-str first) entries))

(defn- root-range
  [cursor]
  (if (nil? cursor)
    (path/sorted-map-range-from-start root-page-size)
    (path/sorted-map-range-from cursor
                                {:max-amt root-page-size
                                 :inclusive? false})))

(defn- flat-partition-entries
  [handle partition-key]
  (loop [cursor nil acc []]
    (let [page (rama/foreign-select-one [(root-range cursor)]
                                        handle
                                        {:pkey partition-key})
          entries (vec (sorted-entries page))
          acc* (into acc entries)]
      (if (< (count entries) root-page-size)
        acc*
        (recur (ffirst (rseq entries)) acc*)))))

(defn- nested-partition-outer-keys
  [handle partition-key]
  (loop [cursor nil acc []]
    (let [keys (vec (sort-by pr-str
                             (rama/foreign-select [(root-range cursor) path/MAP-KEYS]
                                                  handle
                                                  {:pkey partition-key})))
          acc* (into acc keys)]
      (if (< (count keys) root-page-size)
        acc*
        (recur (peek keys) acc*)))))

(defn- nested-inner-entries
  [handle outer-key partition-key]
  (loop [cursor nil acc []]
    (let [page (rama/foreign-select-one [(path/keypath outer-key)
                                         (root-range cursor)]
                                        handle
                                        {:pkey partition-key})
          entries (vec (sorted-entries page))
          acc* (into acc entries)]
      (if (< (count entries) root-page-size)
        acc*
        (recur (ffirst (rseq entries)) acc*)))))

(defn- nested-plain-inner-entries
  [handle outer-key partition-key]
  (sorted-entries
   (rama/foreign-select-one [(path/keypath outer-key)]
                            handle
                            {:pkey partition-key})))

(defn- scan-flat-partition
  [handle module pstate-name partition-index partition-key]
  (mapv (fn [[k v]]
          {:archive/module module
           :archive/pstate pstate-name
           :archive/shape :flat
           :archive/partition partition-index
           :archive/key (plain-edn k)
           :archive/value-class (some-> v class .getName)
           :archive/value (plain-edn v)})
        (flat-partition-entries handle partition-key)))

(defn- scan-nested-partition
  [handle module pstate-name partition-index partition-key]
  (let [outer-keys (nested-partition-outer-keys handle partition-key)]
    (mapv (fn [outer-key]
            (let [inner-entries (nested-inner-entries handle outer-key partition-key)]
              {:archive/module module
               :archive/pstate pstate-name
               :archive/shape :nested
               :archive/partition partition-index
               :archive/key (plain-edn outer-key)
               :archive/leaf-count (count inner-entries)
               :archive/value
               (canonical-map
                (map (fn [[k v]] [(plain-edn k) (plain-edn v)])
                     (sorted-entries inner-entries)))}))
          outer-keys)))

(defn- scan-nested-plain-partition
  [handle module pstate-name partition-index partition-key]
  (let [outer-keys (nested-partition-outer-keys handle partition-key)]
    (mapv (fn [outer-key]
            (let [inner-entries (nested-plain-inner-entries handle outer-key partition-key)]
              {:archive/module module
               :archive/pstate pstate-name
               :archive/shape :nested
               :archive/partition partition-index
               :archive/key (plain-edn outer-key)
               :archive/leaf-count (count inner-entries)
               :archive/value
               (canonical-map
                (map (fn [[k v]] [(plain-edn k) (plain-edn v)])
                     inner-entries))}))
          outer-keys)))

(defn scan-pstate
  [manager {:keys [module name shape value-type inner-storage]}]
  (let [handle (rama/foreign-pstate manager module name)
        keys (partition-keys task-count)
        scan-partition (case shape
                         :flat scan-flat-partition
                         :nested (case inner-storage
                                   :subindexed scan-nested-partition
                                   :plain-map scan-nested-plain-partition))
        by-partition
        (mapv (fn [partition-index]
                (scan-partition handle module name partition-index (nth keys partition-index)))
              (range task-count))
        rows (vec (mapcat identity by-partition))
        logical-keys (map :archive/key rows)
        duplicates (->> logical-keys frequencies (keep (fn [[k n]] (when (> n 1) k))) vec)]
    (when (seq duplicates)
      (throw (ex-info "A PState key appeared in more than one physical partition"
                      {:module module :pstate name :duplicates duplicates})))
    {:module module
     :pstate name
     :shape shape
     :inner-storage inner-storage
     :value-type value-type
     :rows rows
     :entry-count (count rows)
     :leaf-count (if (= :nested shape)
                   (reduce + 0 (map :archive/leaf-count rows))
                   (count rows))
     :partition-counts (into (sorted-map)
                             (map-indexed (fn [idx xs] [idx (count xs)]) by-partition))}))

(defn- safe-fragment [s]
  (str/replace (str s) #"[^A-Za-z0-9._-]+" "_"))

(defn pstate-relative-file
  [module pstate-name]
  (str "pstates/" (safe-fragment module) "__" (safe-fragment pstate-name) ".ednl"))

(defn- sha256-file
  [file]
  (let [digest (MessageDigest/getInstance "SHA-256")
        buffer (byte-array 65536)]
    (with-open [in (io/input-stream file)]
      (loop []
        (let [n (.read in buffer)]
          (when (pos? n)
            (.update digest buffer 0 n)
            (recur)))))
    (apply str (map #(format "%02x" (bit-and 0xff %)) (.digest digest)))))

(defn- write-ednl!
  [file rows]
  (io/make-parents file)
  (with-open [w (io/writer file :encoding "UTF-8")]
    (doseq [row rows]
      (.write w (edn-line row))
      (.write w "\n"))))

(defn- write-edn!
  [file value]
  (io/make-parents file)
  (with-open [w (io/writer file :encoding "UTF-8")]
    (.write w (pr-str (plain-edn value)))
    (.write w "\n")))

(defn- git-value [repo-root & args]
  (let [{:keys [exit out err]} (apply shell/sh "git" "-C" (str repo-root) args)]
    (when-not (zero? exit)
      (throw (ex-info "Git custody read failed" {:args args :err err})))
    (str/trim out)))

(defn- running-state [status]
  (str (.get_state status)))

(defn- deployment-snapshot [manager]
  (let [deployed (set (rama/deployed-module-names manager))
        expected (expected-module-names)]
    (when-not (= expected deployed)
      (throw (ex-info "Deployed module inventory differs from the frozen export inventory"
                      {:expected expected :deployed deployed})))
    (let [states (into (sorted-map)
                       (map (fn [module]
                              [module (running-state (rama/get-module-status manager module))])
                            deployed))]
      (when-not (every? #(= "RUNNING" %) (vals states))
        (throw (ex-info "Every deployed module must be RUNNING before export"
                        {:states states})))
      {:modules (vec (sort deployed)) :states states})))

(defn read-ednl
  [file]
  (with-open [r (io/reader file :encoding "UTF-8")]
    (->> (line-seq r)
         (remove str/blank?)
         (mapv edn/read-string))))

(defn verify-archive
  [output]
  (let [root (io/file output)
        manifest-file (io/file root "manifest.edn")
        manifest (edn/read-string (slurp manifest-file :encoding "UTF-8"))
        results
        (mapv (fn [{:keys [file sha256 entry-count leaf-count]}]
                (let [f (io/file root file)
                      rows (read-ednl f)
                      reread-leaves (reduce + 0 (map #(or (:archive/leaf-count %) 1) rows))]
                  {:file file
                   :exists? (.isFile f)
                   :sha256-ok? (= sha256 (sha256-file f))
                   :entry-count-ok? (= entry-count (count rows))
                   :leaf-count-ok? (= leaf-count reread-leaves)}))
              (:pstates manifest))
        ok? (every? #(every? true? (vals (dissoc % :file))) results)]
    {:ok? ok?
     :manifest-schema (:archive/schema manifest)
     :pstate-count (count results)
     :entry-count (reduce + 0 (map :entry-count (:pstates manifest)))
     :leaf-count (reduce + 0 (map :leaf-count (:pstates manifest)))
     :files results}))

(defn export!
  [repo-root output]
  (let [output-path (assert-safe-output! repo-root output)
        started-at (str (Instant/now))
        partition-keys* (partition-keys task-count)]
    (with-open [manager (rama/open-cluster-manager
                         {"conductor.host" "localhost"
                          "foreign.pstate.operation.timeout.millis" 60000})]
      (let [deployment (deployment-snapshot manager)]
        (Files/createDirectory output-path (make-array java.nio.file.attribute.FileAttribute 0))
        (Files/createDirectory (.resolve output-path "pstates")
                               (make-array java.nio.file.attribute.FileAttribute 0))
        (let [pstate-results
              (mapv (fn [spec]
                      (println "[EXPORT]" (:module spec) (:name spec))
                      (let [{:keys [rows] :as scanned} (scan-pstate manager spec)
                            relative (pstate-relative-file (:module spec) (:name spec))
                            file (io/file (str output-path) relative)]
                        (write-ednl! file rows)
                        (-> scanned
                            (dissoc :rows)
                            (assoc :file relative :sha256 (sha256-file file)))))
                    (pstate-specs))
              manifest
              {:archive/schema 1
               :archive/format :ednl-pstate-map-snapshot
               :archive/complete? true
               :archive/started-at started-at
               :archive/completed-at (str (Instant/now))
               :archive/consistency :quiescent-multi-query-snapshot
               :archive/record-normalization
               {:records :recursively-converted-to-plain-maps
                :throwables :portable-maps-with-class-message-data-cause-and-stacktrace
                :unthawable-values :fatal-export-error
                :other-edn-values :preserved}
               :source/repository (str (path-of repo-root))
               :source/git-head (git-value repo-root "rev-parse" "HEAD")
               :source/git-branch (git-value repo-root "branch" "--show-current")
               :cluster/conductor "localhost"
               :cluster/task-count task-count
               :cluster/task-count-basis
               {:deploy-source "bin/land DEPLOY_OPTS"
                :running-task-groups "tasks 0-7 observed before export"}
               :cluster/partition-keys
               (mapv (fn [idx k] {:partition idx :pkey k}) (range task-count) partition-keys*)
               :cluster/deployment deployment
               :pstates pstate-results
               :totals {:pstates (count pstate-results)
                        :entries (reduce + 0 (map :entry-count pstate-results))
                        :leaves (reduce + 0 (map :leaf-count pstate-results))}
               :excluded
               [{:module trail-view-module
                 :reason :mirror-only
                 :detail "No own PStates; all thirteen mirrors duplicate exported owner PStates."}]
               :write-surface
               {:foreign-appends 0 :depot-appends 0 :pstate-transforms 0
                :deploys 0 :updates 0 :migrations 0}}
              manifest-file (io/file (str output-path) "manifest.edn")]
          (write-edn! manifest-file manifest)
          (let [hash-files (concat (map :file pstate-results) ["manifest.edn"])
                sums-file (io/file (str output-path) "SHA256SUMS")]
            (with-open [w (io/writer sums-file :encoding "UTF-8")]
              (doseq [relative (sort hash-files)]
                (.write w (str (sha256-file (io/file (str output-path) relative))
                               "  " relative "\n"))))
            (let [verification (verify-archive output-path)]
              (when-not (:ok? verification)
                (throw (ex-info "Archive verification failed" verification)))
              (write-edn! (io/file (str output-path) "verification.edn")
                          (assoc verification
                                 :verified-at (str (Instant/now))
                                 :sha256sums-sha256 (sha256-file sums-file)))
              {:output (str output-path)
               :manifest manifest
               :verification verification})))))))

(defn -main
  [& [output]]
  (when (str/blank? output)
    (throw (ex-info "Usage: clj -M -m app.tools.export-current-data <new-output-dir>" {})))
  (let [repo-root (System/getProperty "user.dir")
        result (export! repo-root output)]
    (println (pr-str {:status :exported
                      :output (:output result)
                      :totals (get-in result [:manifest :totals])
                      :verified? (get-in result [:verification :ok?])}))))
