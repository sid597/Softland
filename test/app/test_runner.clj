(ns app.test-runner
  "Repository test lanes.

   `full` discovers every *_test.clj namespace and preserves its existing
   runtime lifecycle. Registered flake namespaces run in fresh JVMs according
   to their existing standalone protocol before the remaining unchanged
   namespaces run in the default six concurrent shards. `:shards 1` serializes
   the non-flake sweep while retaining that standalone protocol.

   `fast` runs every pure namespace plus the explicitly proven shared-IPC tier.
   The shared tier gets one physical InProcessCluster for the JVM. Runtime
   constructors still choose their usual module and launch options; this
   harness only memoizes identical module launches and owns the final close.

   Classification is fail-closed: adding or removing a test namespace requires
   an explicit tier decision here, so the fast lane can never silently de-scope
   coverage."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :as t]
            [com.rpl.rama :as rama]
            [com.rpl.rama.test :as rtest]
            [rpl.rama.distributed.simulate.sim-common :as sim-common])
  (:import [clojure.lang LineNumberingPushbackReader]
           [java.io File]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def pure-namespaces
  '[app.client.substrate.image-material-test
    app.client.substrate.path-material-test
    app.client.substrate.path-tessellation-test
    app.client.substrate.region3d-evaluation-test
    app.client.substrate.region3d-material-test
    app.client.substrate.region3d-placement-test
    app.client.substrate.region3d-scene-test
    app.client.substrate.region-rungs-test
    app.client.workspace.containers-test
    app.client.workspace.shaping-correction-test
    app.client.workspace.text-layout-planes-test
    app.client.workspace.text-layout-test
    app.binding-dispatch-test
    app.missionary-claims-test
    app.reply-to-block-test
    app.space-material-test
    app.server.parser-test
    app.server.rama.core-guards-test
    app.server.rama.probe-harness-test])

(def shared-cluster-namespaces
  "Every IPC write in these namespaces uses disjoint or idempotent identity.
   All runtime constructors request the same 4-task/2-thread launch for any
   module they share. The launch-once guard below enforces that proof at run
   time."
  '[app.face-gate-fixes-test
    app.machine-cut-serve-test
    app.material-inspector-test
    app.server.rama.dogfood-transcript-probe-test
    app.server.rama.dogfood-transcript-test])

(def isolation-exceptions
  "Namespaces whose semantics still require the historical private-cluster
   lifecycle. These remain in `full`; only the inner-loop `fast` lane omits
   them."
  '{app.cascade-table-test
    "Boots fresh OC, relation, and LLM runtimes for the behavior-identity cut."

    app.face-arsenal-test
    "Mutates the process-global ingest epoch and opens a second cluster for WAL boot replay."

    app.face-projection-test
    "The guarded 6.9MB real-corpus receipt asserts exact clean-cluster durable counts."

    app.machine-cut-test
    "Mutates the process-global ingest epoch and opens a fresh relation cluster for WAL replay."

    app.material-circulation-test
    "Cross-module activation and circulation mutate fixed facet-master and relation identities."

    app.material-truth-test
    "P6 gates mint instance masters, move shared activation pointers, upsert the episode registry index, and assert the process-global ingest epoch."

    app.material-portal-test
    "P7 gates deviate, pin and activate fixed facet-master identities and run the malformed drill; the recovery gate rolls a shared activation pointer back."

    app.provenance-material-test
    "Activation, malformed-candidate, and rollback gates mutate fixed facet-master pointers."

    app.server.episode-test
    "Restart-adoption and durable episode-cell assertions require a clean object-container history."

    app.server.ingest-watchers-test
    "File-watcher recovery mutates the process-global ingest epoch; it is also a registered port-conflict flake."

    app.server.rama.code-atoms-test
    "Git-history receipts use randomized task counts and independently launched composite runtimes."

    app.server.rama.dogfood-llm-probe-test
    "Registered suite-order flake; fixed thread ids overlap the space probe, so shared-state safety is unproven."

    app.server.rama.dogfood-llm-test
    "Executor-death and stale-approval recovery mutate shared executor indexes; registered flake semantics stay unchanged."

    app.server.rama.dogfood.transcript-ingest-test
    "Watch resume, file-offset restart, and terminal-state tests repeatedly assume a clean ingest runtime."

    app.server.rama.git-spine-gate-test
    "The gate compares two intentionally fresh relation runtimes across a replay boundary."

    app.server.rama.git-spine-test
    "WAL replay and restart gates open multiple fresh randomized-task runtimes."

    app.server.rama.object-container-test
    "Physical negative reads and exact empty-state assertions use direct with-open clusters."

    app.server.rama.object-container.block-distiller-test
    "Exact corpus counts and the intentional OC/RK two-cluster boundary span several clean launches."

    app.server.rama.object-container.block-write-test
    "The WAL survival gate requires a real runtime close followed by a fresh cluster."

    app.server.rama.object-container.clojure-adapter-test
    "Pinned whole-corpus enumeration and physical reads share one directly owned clean fixture cluster."

    app.server.rama.relation-kernel-test
    "Randomized partition sweeps plus pause/resume deliberately control the whole microbatch topology."

    app.server.rama.trail-view-test
    "Randomized task-count and no-source module launches are distinct topology-shape proofs."

    app.server.relation-assert-route-test
    "Two-task WAL route tests own scratch logs and require independently clean relation runtimes."})

(def flake-registry-namespaces
  "The existing registered environment/order-sensitive namespaces. The harness
   does not modify their tests or retry protocol, and keeps all three isolated."
  '#{app.server.ingest-watchers-test
     app.server.rama.dogfood-llm-probe-test
     app.server.rama.dogfood-llm-test})

(def full-receipt-floor
  "The receipt floor after the conductor cut (scene tape, frame graph, delta
   spine) retired 30 tests / 172 assertions.
   Full runs may grow but may not drop below it."
  {:test 326 :assertions 4379})

(def ^:private shared-close-vars
  '[app.server.rama.face-arsenal/close-face-arsenal-runtime!
    app.server.rama.relation-kernel/close-relation-runtime!
    app.server.rama.object-container.runtime/close-object-container-runtime!
    app.server.rama.dogfood.transcript/close-transcript-runtime!])

(def ^:private slow-namespace-cost
  "Extra sharding weights for work whose cost is not represented by source
   length or runtime-constructor count. The face-projection namespace performs
   one shared harvest/distill over the guarded 6.9MB corpus; dogfood-space
   launches a clean runtime for each integration test. Keep both minutes-long
   receipts on their own shards."
  '{app.face-projection-test 24000
    app.server.rama.object-container.block-distiller-test 1600
    app.server.rama.dogfood.transcript-ingest-test 900})

(def ^:private minimum-shard-port-span
  "One Rama IPC reserves a 1,000-port supervisor range plus conductor/client
   ports. Keep a safety margin while dividing Rama's built-in test range."
  1500)

(defn- assert-shard-count!
  [shard-count]
  (let [available (- sim-common/END-PORT sim-common/START-PORT)
        port-span (quot available shard-count)]
    (when (< port-span minimum-shard-port-span)
      (throw
       (ex-info "Too many shards for Rama's in-process port range"
                {:shards shard-count
                 :available-ports available
                 :minimum-port-span minimum-shard-port-span}))))
  shard-count)

(defn- read-ns-symbol
  [^File file]
  (with-open [reader (LineNumberingPushbackReader. (io/reader file))]
    (binding [*read-eval* false]
      (loop []
        (let [form (read {:eof ::eof
                          :read-cond :allow
                          :features #{:clj}}
                         reader)]
          (cond
            (= ::eof form)
            (throw (ex-info "No ns form in test file" {:file (.getPath file)}))

            (and (seq? form) (= 'ns (first form)))
            (second form)

            :else
            (recur)))))))

(defn- test-inventory
  []
  (->> (file-seq (io/file "test"))
       (filter #(.isFile ^File %))
       (filter #(str/ends-with? (.getName ^File %) "_test.clj"))
       (map (fn [file] [(read-ns-symbol file) file]))
       (sort-by (comp str first))
       (into (sorted-map))))

(defn- assert-inventory!
  []
  (let [discovered (set (keys (test-inventory)))
        pure (set pure-namespaces)
        shared (set shared-cluster-namespaces)
        isolated (set (keys isolation-exceptions))
        duplicate (set/union (set/intersection pure shared)
                             (set/intersection pure isolated)
                             (set/intersection shared isolated))
        classified (set/union pure shared isolated)]
    (when (seq duplicate)
      (throw (ex-info "Test namespace appears in more than one tier"
                      {:duplicate duplicate})))
    (when-not (= discovered classified)
      (throw (ex-info "Test namespace classification is stale"
                      {:unclassified (set/difference discovered classified)
                       :missing-on-disk (set/difference classified discovered)})))
    (when-not (set/subset? flake-registry-namespaces isolated)
      (throw (ex-info "A registered flake left the isolated tier"
                      {:not-isolated
                       (set/difference flake-registry-namespaces isolated)})))
    {:discovered discovered
     :pure pure
     :shared shared
     :isolated isolated}))

(defn- summary
  [result]
  (let [{:keys [pass fail error] :as counters}
        (select-keys result [:test :pass :fail :error])]
    (assoc counters :assertions (+ pass fail error))))

(defn- merge-summaries
  [& results]
  (reduce (fn [total result]
            (merge-with + total (summary result)))
          {:test 0 :pass 0 :fail 0 :error 0 :assertions 0}
          results))

(defn- elapsed-ms
  [started]
  (/ (double (- (System/nanoTime) started)) 1000000.0))

(defn- require-namespaces!
  [namespaces]
  (doseq [namespace namespaces]
    (require namespace)))

(defn- run-namespaces!
  [lane namespaces]
  (let [started (System/nanoTime)]
    (require-namespaces! namespaces)
    (let [result (apply t/run-tests namespaces)]
      (assoc (summary result)
             :lane lane
             :namespaces (count namespaces)
             :namespace-list (vec namespaces)
             :elapsed-ms (elapsed-ms started)))))

(defn- green?
  [result]
  (zero? (+ (:fail result 0) (:error result 0))))

(defn- assert-full-floor!
  [result]
  (doseq [[k floor] full-receipt-floor]
    (when (< (get result k 0) floor)
      (throw (ex-info "Full-suite receipt dropped below the banked baseline"
                      {:metric k :floor floor :actual (get result k 0)}))))
  result)

(defn- fail-on-red!
  [result]
  (when-not (green? result)
    (throw (ex-info "Test lane failed" {:receipt result})))
  result)

(defn- write-child-receipt!
  [receipt]
  (when-let [path (System/getenv "SOFTLAND_TEST_RECEIPT")]
    (spit path (pr-str receipt)))
  receipt)

(defn- resolve-close-bindings
  []
  (into {}
        (map (fn [symbol]
               (let [v (find-var symbol)]
                 (when-not v
                   (throw (ex-info "Shared-tier close var is not loaded"
                                   {:var symbol})))
                 [v (fn [& _] nil)])))
        shared-close-vars))

(defn- run-shared-cluster!
  []
  (require-namespaces! shared-cluster-namespaces)
  (let [ipc (rtest/create-ipc)
        original-launch rtest/launch-module!
        launched (atom {})
        launch-lock (Object.)
        launch-once!
        (fn [requested-ipc module launch-opts]
          (when-not (identical? ipc requested-ipc)
            (throw (ex-info "Shared-tier runtime selected an unexpected IPC"
                            {:requested requested-ipc :shared ipc})))
          (locking launch-lock
            (let [module-name (rama/get-module-name module)]
              (if-let [{existing-opts :launch-opts
                        result :result} (get @launched module-name)]
                (do
                  (when-not (= existing-opts launch-opts)
                    (throw
                     (ex-info
                      "Shared-tier module requested incompatible launch options"
                      {:module module-name
                       :first existing-opts
                       :requested launch-opts})))
                  result)
                (let [result (original-launch ipc module launch-opts)]
                  (swap! launched assoc module-name
                         {:launch-opts launch-opts :result result})
                  result)))))]
    (try
      (let [bindings (merge
                      {#'rtest/create-ipc (fn [] ipc)
                       #'rtest/launch-module! launch-once!}
                      (resolve-close-bindings))
            result
            (with-redefs-fn
              bindings
              #(run-namespaces! :fast-shared shared-cluster-namespaces))]
        (assoc result
               :cluster-count 1
               :launched-modules
               (into (sorted-map)
                     (map (fn [[module-name {:keys [launch-opts]}]]
                            [module-name launch-opts]))
                     @launched)))
      (finally
        (try
          (.close ^java.lang.AutoCloseable ipc)
          (catch Exception _ nil))))))

(defn fast
  "Run every pure namespace plus the one-cluster shared tier."
  [_]
  (let [inventory (assert-inventory!)
        started (System/nanoTime)
        pure-result (run-namespaces! :fast-pure pure-namespaces)
        shared-result (run-shared-cluster!)
        result (assoc (merge-summaries pure-result shared-result)
                      :lane :fast
                      :namespaces (+ (count pure-namespaces)
                                     (count shared-cluster-namespaces))
                      :pure-namespaces (count pure-namespaces)
                      :shared-cluster-namespaces
                      (count shared-cluster-namespaces)
                      :isolated-namespaces (count (:isolated inventory))
                      :shared-clusters 1
                      :shared-modules (:launched-modules shared-result)
                      :flake-registry-namespaces
                      (vec (sort flake-registry-namespaces))
                      :flake-registry-status :unchanged
                      :elapsed-ms (elapsed-ms started))]
    (println "TEST-RECEIPT" (pr-str result))
    (fail-on-red! result)))

(defn- source-cost
  [[namespace ^File file]]
  (let [source (slurp file)
        lines (count (str/split-lines source))
        runtime-launches
        (count (re-seq #"start-[a-z0-9-]+runtime!|rtest/create-ipc|\(create-ipc\)"
                       source))]
    {:namespace namespace
     :cost (+ lines
              (* 650 runtime-launches)
              (get slow-namespace-cost namespace 0))}))

(defn- shard-plan
  [shard-count]
  (let [weighted (->> (apply dissoc
                             (test-inventory)
                             flake-registry-namespaces)
                      (map source-cost)
                      (sort-by (juxt (comp - :cost) (comp str :namespace))))]
    (->> weighted
         (reduce
          (fn [bins item]
            (let [target (:index (first (sort-by (juxt :cost :index) bins)))]
              (update bins target
                      (fn [bin]
                        (-> bin
                            (update :cost + (:cost item))
                            (update :namespaces conj (:namespace item)))))))
         (mapv (fn [index] {:index index :cost 0 :namespaces []})
                (range shard-count)))
         (mapv #(update % :namespaces
                        (fn [namespaces]
                          (vec (sort-by str namespaces))))))))

(defn- registered-flake-plan
  []
  (->> flake-registry-namespaces
       (sort-by str)
       (map-indexed (fn [index namespace]
                      {:index index
                       :kind :registered-flake
                       :attempt 1
                       :namespaces [namespace]}))
       vec))

(defn shard
  "Internal non-flake full-suite child. `full` starts these concurrently."
  [{:keys [index count]}]
  (assert-inventory!)
  (when-not (and (int? index)
                 (int? count)
                 (pos? count)
                 (<= 0 index)
                 (< index count))
    (throw (ex-info "Invalid shard coordinates"
                    {:index index :count count})))
  (assert-shard-count! count)
  (let [available (- sim-common/END-PORT sim-common/START-PORT)
        port-span (quot available count)]
    (let [port-start (+ sim-common/START-PORT (* index port-span))
          port-end (if (= index (dec count))
                     sim-common/END-PORT
                     (dec (+ port-start port-span)))
          {:keys [cost namespaces]} (nth (shard-plan count) index)
          result
          (with-redefs [sim-common/START-PORT port-start
                        sim-common/END-PORT port-end
                        sim-common/LAST-OPEN-PORT-ATOM (atom port-start)]
            (assoc (run-namespaces! :full-shard namespaces)
                      :shard-index index
                      :shard-count count
                      :estimated-cost cost
                      :rama-port-range [port-start port-end]
                      :flake-registry-status :unchanged))]
      (println "TEST-RECEIPT" (pr-str result))
      (write-child-receipt! result)
      (fail-on-red! result))))

(defn registered-flake
  "Internal full-suite child for one unchanged registered-flake namespace."
  [{:keys [index attempt] :or {attempt 1}}]
  (assert-inventory!)
  (let [plan (registered-flake-plan)]
    (when-not (and (int? index) (<= 0 index) (< index (count plan)))
      (throw (ex-info "Invalid registered-flake index"
                      {:index index :count (count plan)})))
    (let [namespace (first (:namespaces (nth plan index)))
          result (assoc (run-namespaces! :full-registered-flake [namespace])
                        :registered-flake-index index
                        :registered-flake-attempt attempt
                        :flake-registry-status :unchanged)]
      (println "TEST-RECEIPT" (pr-str result))
      (write-child-receipt! result)
      (fail-on-red! result))))

(defn- temp-dir
  []
  (.toFile
   (Files/createTempDirectory
    "softland-test-shards-"
    (make-array FileAttribute 0))))

(defn- start-shard-process!
  [^File dir {:keys [index] :as plan} shard-count]
  (let [receipt-file (io/file dir (str "shard-" index ".edn"))
        log-file (io/file dir (str "shard-" index ".log"))
        command ["clj" "-X:test" "shard"
                 ":index" (str index)
                 ":count" (str shard-count)]
        builder (ProcessBuilder. ^java.util.List command)]
    (.directory builder (io/file (System/getProperty "user.dir")))
    (.redirectErrorStream builder true)
    (.redirectOutput builder log-file)
    (.put (.environment builder)
          "SOFTLAND_TEST_RECEIPT"
          (.getAbsolutePath receipt-file))
    {:plan plan
     :receipt-file receipt-file
     :log-file log-file
     :process (.start builder)}))

(defn- start-registered-flake-process!
  [^File dir {:keys [index attempt] :as plan}]
  (let [receipt-file
        (io/file dir (str "registered-flake-" index "-" attempt ".edn"))
        log-file
        (io/file dir (str "registered-flake-" index "-" attempt ".log"))
        command ["clj" "-X:test" "registered-flake"
                 ":index" (str index)
                 ":attempt" (str attempt)]
        builder (ProcessBuilder. ^java.util.List command)]
    (.directory builder (io/file (System/getProperty "user.dir")))
    (.redirectErrorStream builder true)
    (.redirectOutput builder log-file)
    (.put (.environment builder)
          "SOFTLAND_TEST_RECEIPT"
          (.getAbsolutePath receipt-file))
    {:plan plan
     :receipt-file receipt-file
     :log-file log-file
     :process (.start builder)}))

(defn- finish-shard-process!
  [{:keys [plan receipt-file log-file process]}]
  (let [exit (.waitFor ^Process process)
        output (slurp log-file)
        receipt (when (.isFile ^File receipt-file)
                  (edn/read-string (slurp receipt-file)))]
    (if (= :registered-flake (:kind plan))
      (println (str "\n===== REGISTERED FLAKE "
                    (first (:namespaces plan))
                    " / attempt " (:attempt plan) " ====="))
      (println (str "\n===== FULL SHARD " (:index plan)
                    " / estimated cost " (:cost plan) " =====")))
    (print output)
    {:exit exit
     :plan plan
     :receipt receipt
     :receipt-file receipt-file
     :log-file log-file}))

(defn- run-registered-flake-with-protocol!
  [^File dir plan]
  (let [first-result
        (-> (start-registered-flake-process! dir plan)
            finish-shard-process!)
        retry? (and (:receipt first-result)
                    (not (green? (:receipt first-result))))
        attempts
        (cond-> [first-result]
          retry?
          (conj
           (-> (start-registered-flake-process!
                dir
                (assoc plan :attempt 2))
               finish-shard-process!)))]
    {:canonical (peek attempts)
     :attempts attempts}))

(defn- delete-temp-files!
  [^File dir]
  (doseq [^File file (.listFiles dir)]
    (.delete file))
  (.delete dir))

(defn- run-parallel-full!
  [shard-count]
  (let [started (System/nanoTime)
        dir (temp-dir)]
    (try
      (let [flake-plan (registered-flake-plan)
            flake-runs
            (mapv #(run-registered-flake-with-protocol! dir %) flake-plan)
            flake-results (mapv :canonical flake-runs)
            plan (shard-plan shard-count)
            children (mapv #(start-shard-process! dir % shard-count) plan)
            shard-results (mapv finish-shard-process! children)
            results (into flake-results shard-results)
            missing (filterv (comp nil? :receipt) results)]
        (when (seq missing)
          (throw
           (ex-info "One or more full-suite children produced no receipt"
                    {:missing-receipts (mapv (comp :index :plan) missing)
                     :exit-codes (mapv (juxt (comp :index :plan) :exit)
                                       results)})))
        (let [result
              (assoc
               (apply merge-summaries (map :receipt results))
               :lane :full
               :execution
               (if (= 1 shard-count)
                 :registered-flakes-standalone-plus-serial-jvm
                 :registered-flakes-standalone-plus-parallel-jvms)
               :shards shard-count
               :namespaces
               (reduce + (map (comp count :namespace-list :receipt) results))
               :registered-flake-execution :standalone-sequential-jvms
               :registered-flake-plan
               (mapv :namespaces flake-plan)
               :registered-flake-attempts
               (mapv
                (fn [{:keys [attempts]}]
                  {:namespace (-> attempts first :plan :namespaces first)
                   :attempts (count attempts)
                   :receipts (mapv (comp summary :receipt) attempts)})
                flake-runs)
               :shard-plan
               (mapv #(select-keys % [:index :cost :namespaces]) plan)
               :flake-registry-namespaces
               (vec (sort flake-registry-namespaces))
               :flake-registry-status :unchanged
               :elapsed-ms (elapsed-ms started))]
          (assert-full-floor! result)
          (println "TEST-RECEIPT" (pr-str result))
          (fail-on-red! result)))
      (finally
        (delete-temp-files! dir)))))

(defn full
  "Run every discovered test namespace.

   The three registered-flake namespaces run in standalone JVMs before the
   remaining namespaces. Their existing registry protocol permits one
   recorded standalone rerun; two consecutive failures remain red.

   Default: six concurrent JVM shards for the remaining sweep.
   Serial non-flake sweep: `clj -X:test full :shards 1`.
   Configurable parallelism: `clj -X:test full :shards N`."
  [{:keys [shards] :or {shards 6}}]
  (assert-inventory!)
  (when-not (and (int? shards) (pos? shards))
    (throw (ex-info "shards must be a positive integer" {:shards shards})))
  (assert-shard-count! shards)
  (run-parallel-full! shards))
