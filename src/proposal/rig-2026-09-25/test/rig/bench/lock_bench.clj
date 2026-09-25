(ns rig.bench.lock-bench
  "Phase 7, the second number: lock store growth under a hand layer, bytes
  per value and the curve over 100,000 values (SPEC.md, phase 7).

  Run from the rig folder:

    clojure -M:bench rig.bench.lock-bench run <variant,...|all> [values] [every]
    clojure -M:bench rig.bench.lock-bench report

  Each variant gets a fresh in-process cluster with rig.bench.LockSlice on
  one task, one thread, one worker, and writes `values` values (default
  100,000) into the hand layer :alice-hand (owner :alice), one value per act,
  the same values in the same order in every variant with the same size
  (seeded). Every `every` values (default 10,000) the appends are drained
  (every ack returned) and four things are recorded:

  - logical: the lock rows read back from `[layer :locks]` page by page,
    each sized in Rama 1.6.0's own serializers (the key by the RocksDB key
    encoder, the record by the PState value serializer);
  - live: the file bytes of the PState's RocksDB directory as it stands
    (write-ahead log, SST files, manifest and options; RocksDB's info LOG
    excluded; apparent length, not the WAL's preallocated blocks);
  - compacted: the same directory copied, opened with the options Rama
    wrote into it, flushed and fully compacted: the SST bytes the data
    settles to, and a scan of every key and value in it (count and bytes);
  - replog: the task's replication-log segment bytes, for reference.

  Results append to runs/phase7-lock-growth.edn, one map per variant;
  `report` prints the tables and ratios from that file."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [rig.bench.lock-slice :as slice]
            [rig.store.envelope :as env]
            [rpl.rama.api.durable.rocksdb.key-encoding :as rkey]
            [rpl.rama.util.nippy-serialization :as rser])
  (:import [java.io File]
           [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets]
           [java.nio.file CopyOption Files StandardCopyOption]
           [java.util ArrayList Random]
           [java.util.concurrent Semaphore]
           [java.util.function BiConsumer]
           [org.rocksdb ColumnFamilyHandle CompactRangeOptions CompactRangeOptions$BottommostLevelCompaction
            ConfigOptions DBOptions FlushOptions OptionsUtil RocksDB]))

(def results-file
  "Where results append; LOCK_BENCH_OUT overrides it (smoke runs)."
  (or (System/getenv "LOCK_BENCH_OUT") "runs/phase7-lock-growth.edn"))

(def layer :alice-hand)
(def owner :alice)

(def variants
  "size: canonical EDN bytes of each value; encoding: slot form; lock-row:
  whether the lock row is written; people: 1 = a :note by the owner, 2 = a
  :mention of :bob marked :die-with-any, so the wrap requires both."
  (array-map
   "small-b64-lock" {:size 40  :encoding :base64 :row-lock true  :people 1}
   "small-b64-none" {:size 40  :encoding :base64 :row-lock false :people 1}
   "small-raw-lock" {:size 40  :encoding :raw    :row-lock true  :people 1}
   "small-raw-none" {:size 40  :encoding :raw    :row-lock false :people 1}
   "large-b64-lock" {:size 200 :encoding :base64 :row-lock true  :people 1}
   "large-b64-none" {:size 200 :encoding :base64 :row-lock false :people 1}
   "large-raw-lock" {:size 200 :encoding :raw    :row-lock true  :people 1}
   "large-raw-none" {:size 200 :encoding :raw    :row-lock false :people 1}
   "two-b64-lock"   {:size 40  :encoding :base64 :row-lock true  :people 2}
   "two-b64-none"   {:size 40  :encoding :base64 :row-lock false :people 2}))

;; ------------------------------------------------------------ the values

(def ^:private letters "abcdefghijklmnopqrstuvwxyz ")

(defn- text [^Random r n]
  (let [sb (StringBuilder.)]
    (dotimes [_ n] (.append sb (.charAt ^String letters (.nextInt r (count letters)))))
    (str sb)))

(defn value-spec
  "The i-th value fact of a variant, from a seeded generator: the value's
  canonical EDN is exactly `size` bytes."
  [^Random r i {:keys [size people]}]
  (let [base (if (= 2 people) {:persons #{:bob} :text ""} {:text ""})
        overhead (count (.getBytes ^String (env/canonical base) StandardCharsets/UTF_8))
        v (assoc base :text (text r (- size overhead)))]
    {:e (keyword (format "n%06d" i))
     :k (if (= 2 people) :mention :note)
     :v v
     :mark (if (= 2 people) #{:die-with-any} #{})}))

;; ------------------------------------------------------------- the sizes

(def ^:private ^ByteBuffer buf (ByteBuffer/allocate (* 1024 1024)))

(defn value-bytes
  "Bytes of v as Rama 1.6.0 serializes a PState value."
  [v]
  (.clear buf) (rser/freeze buf v) (.position buf))

(defn key-bytes
  "Bytes of k as Rama 1.6.0 encodes a RocksDB key."
  [k]
  (.clear buf) (rkey/k-ser buf k) (.position buf))

(defn lock-rows-logical
  "Every lock row of the layer, read back page by page from the PState, with
  its serialized key and record bytes."
  [layers-ps page]
  (loop [after nil acc {:rows 0 :key-bytes 0 :record-bytes 0 :row-min Long/MAX_VALUE :row-max 0}]
    (let [path (if after
                 [(keypath layer :locks) (sorted-map-range-from after {:max-amt page :inclusive? false}) ALL]
                 [(keypath layer :locks) (sorted-map-range-from-start page) ALL])
          rows (foreign-select path layers-ps)]
      (if (empty? rows)
        (cond-> acc (zero? (:rows acc)) (assoc :row-min 0))
        (recur (first (peek rows))
               (reduce (fn [a [lid rec]]
                         (let [kb (key-bytes lid) vb (value-bytes rec) t (+ kb vb)]
                           (-> a (update :rows inc) (update :key-bytes + kb) (update :record-bytes + vb)
                               (update :row-min min t) (update :row-max max t))))
                       acc rows))))))

;; ---------------------------------------------------------- the disk side

(defn ipc-root
  "The in-process cluster's own directory (its temp_dir field)."
  ^File [ipc]
  (let [f (.getDeclaredField (class ipc) "temp_dir")]
    (.setAccessible f true)
    (io/file (str (.get f ipc)))))

(defn- ancestors-of [^File f] (take-while some? (iterate #(.getParentFile ^File %) f)))

(defn rocks-dirs
  "The RocksDB directories of one PState (one per task), found by the
  URL-encoded PState name among a CURRENT file's ancestors."
  [^File root pstate-name]
  (let [enc (str "%24%24" (subs pstate-name 2))]
    (->> (file-seq root)
         (filter #(and (.isFile ^File %) (= "CURRENT" (.getName ^File %))))
         (map #(.getParentFile ^File %))
         (filter (fn [d] (some #(= enc (.getName ^File %)) (ancestors-of d))))
         distinct
         vec)))

(defn- info-log? [^String n] (or (= "LOG" n) (str/starts-with? n "LOG.old")))

(defn live-bytes
  "File bytes of the directories as they stand, by kind (apparent length)."
  [dirs]
  (reduce (fn [acc ^File f]
            (let [n (.getName f) len (.length f)]
              (cond
                (= "LOCK" n) acc
                (info-log? n) (update acc :info-log + len)
                (str/ends-with? n ".log") (update acc :wal + len)
                (str/ends-with? n ".sst") (update acc :sst + len)
                :else (update acc :meta + len))))
          {:wal 0 :sst 0 :meta 0 :info-log 0}
          (for [^File d dirs ^File f (.listFiles d) :when (.isFile f)] f)))

(defn- listing [^File d]
  (into (sorted-map) (for [^File f (.listFiles d) :when (.isFile f)] [(.getName f) (.length f)])))

(defn- wait-stable
  "Wait until a directory's file names and lengths hold still for 300 ms."
  [^File d]
  (loop [prev (listing d) tries 0]
    (Thread/sleep 300)
    (let [cur (listing d)]
      (if (or (= prev cur) (> tries 100)) cur (recur cur (inc tries))))))

(defn- rm-rf [^File f]
  (when (.isDirectory f) (doseq [c (.listFiles f)] (rm-rf c)))
  (.delete f))

(defn- scan-kv
  "Count and bytes of every key and value in one column family."
  [^RocksDB db ^ColumnFamilyHandle h]
  (with-open [it (.newIterator db h)]
    (.seekToFirst it)
    (loop [n 0 kb 0 vb 0]
      (if (.isValid it)
        (let [k (alength (.key it)) v (alength (.value it))]
          (.next it)
          (recur (inc n) (+ kb k) (+ vb v)))
        {:kv-count n :kv-key-bytes kb :kv-value-bytes vb}))))

(defn- compact-copy
  "Copy one RocksDB directory, open the copy with the options Rama wrote
  into it, flush and compact it fully, and measure it."
  [^File d ^File scratch]
  (wait-stable d)
  (loop [attempt 1]
    (let [copy (io/file scratch (str "copy-" (System/nanoTime)))
          r (try
              (.mkdirs copy)
              (doseq [^File f (.listFiles d)
                      :when (and (.isFile f) (not= "LOCK" (.getName f)) (not (info-log? (.getName f))))]
                (Files/copy (.toPath f) (.toPath (io/file copy (.getName f)))
                            ^"[Ljava.nio.file.CopyOption;" (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING])))
              (let [dbo (DBOptions.) cfds (ArrayList.) handles (ArrayList.)]
                (OptionsUtil/loadLatestOptions (ConfigOptions.) (.getPath copy) dbo cfds)
                (.setCreateIfMissing dbo false)
                (let [db (RocksDB/open dbo (.getPath copy) cfds handles)]
                  (try
                    (doseq [^ColumnFamilyHandle h handles]
                      (with-open [fo (doto (FlushOptions.) (.setWaitForFlush true))] (.flush db fo h)))
                    ;; the flush output as it stands (sequence numbers intact)
                    (let [flushed (into {} (for [^ColumnFamilyHandle h handles]
                                             [(String. (.getName h) StandardCharsets/UTF_8)
                                              (Long/parseLong (.getProperty db h "rocksdb.total-sst-files-size"))]))]
                      ;; a forced rewrite into the bottom level: without kForce a lone
                      ;; flushed file is moved, not rewritten, and the measure would
                      ;; depend on whether the live store had flushed
                      (with-open [cro (doto (CompactRangeOptions.)
                                        (.setBottommostLevelCompaction CompactRangeOptions$BottommostLevelCompaction/kForce))]
                        (doseq [^ColumnFamilyHandle h handles] (.compactRange db h nil nil cro)))
                      ;; Rama keeps a PState's top-level entries in "default" and
                      ;; every subindexed element in "subindexed"
                      (let [per-cf (into (sorted-map)
                                         (for [^ColumnFamilyHandle h handles
                                               :let [cf (String. (.getName h) StandardCharsets/UTF_8)]]
                                           [cf (assoc (scan-kv db h)
                                                      :sst-flushed (get flushed cf)
                                                      :sst (Long/parseLong (.getProperty db h "rocksdb.total-sst-files-size")))]))]
                        (merge {:sst-files (count (filter #(str/ends-with? (.getName ^File %) ".sst") (.listFiles copy)))
                                :per-cf per-cf}
                               (apply merge-with + (map #(select-keys % [:sst :sst-flushed :kv-count :kv-key-bytes :kv-value-bytes])
                                                        (vals per-cf))))))
                    (finally
                      (doseq [^ColumnFamilyHandle h handles] (.close h))
                      (.close db)))))
              (catch Throwable t {:error (str t)})
              (finally (rm-rf copy)))]
      (if (and (:error r) (< attempt 5))
        (do (Thread/sleep 1000) (wait-stable d) (recur (inc attempt)))
        r))))

(defn compacted-bytes
  "The compacted measure summed over the PState's directories (one per task)."
  [dirs scratch]
  (let [rs (for [d dirs] (let [r (compact-copy d scratch)]
                           (if (:error r) (throw (ex-info "compaction copy failed" r)) r)))]
    (assoc (apply merge-with + {:sst 0 :sst-flushed 0 :sst-files 0 :kv-count 0 :kv-key-bytes 0 :kv-value-bytes 0}
                  (map #(dissoc % :per-cf) rs))
           :per-cf (apply merge-with #(merge-with + %1 %2) (map :per-cf rs)))))

(defn replog-bytes
  "Segment bytes of the task-thread replication logs (index files excluded)."
  [^File root]
  (reduce + (for [^File f (file-seq root)
                  :when (and (.isFile f) (str/ends-with? (.getName f) ".log")
                             (str/includes? (.getPath f) "/replog/"))]
              (.length f))))

;; ------------------------------------------------------------- the checks

(defn- open-value
  "The plaintext EDN text of a stored row, through its lock row and the
  given person locks, or nil."
  [encoding row rec locks]
  (when-let [K (slice/unwrap-lock encoding rec locks)]
    (some-> (slice/unseal K (slice/decode encoding (:v row))) (String. StandardCharsets/UTF_8))))

(defn check-rows
  "Read back sample values: the row's slot is not the plaintext; with a lock
  row, the value opens through it and the person locks, and does not open
  without a required person's lock; without one, no lock row exists."
  [layers-ps cfg sample]
  (let [enc (:encoding cfg)]
    (reduce
     (fn [acc {:keys [name v]}]
       (let [row (foreign-select-one [(keypath layer :log name) (nthpath 0)] layers-ps)
             rec (foreign-select-one [(keypath layer :locks (:lock-id row))] layers-ps)
             plain (env/encode-value v)
             acc (cond-> (update acc :checked inc)
                   (and (some? (:v row)) (not= (:v row) plain)) (update :ciphertext-differs inc)
                   (nil? rec) (update :no-lock-row inc))]
         (if (nil? rec)
           acc
           (cond-> acc
             (= plain (open-value enc row rec slice/person-locks)) (update :opened inc)
             (nil? (open-value enc row rec (dissoc slice/person-locks :alice))) (update :closed-without-owner inc)
             (nil? (open-value enc row rec (dissoc slice/person-locks :bob))) (update :closed-without-bob inc)))))
     {:checked 0 :ciphertext-differs 0 :no-lock-row 0 :opened 0 :closed-without-owner 0 :closed-without-bob 0}
     sample)))

(defn- sample-breakdown
  "Serialized sizes of one stored row and its lock record, part by part."
  [layers-ps name]
  (let [row (foreign-select-one [(keypath layer :log name) (nthpath 0)] layers-ps)
        rec (when (:lock-id row) (foreign-select-one [(keypath layer :locks (:lock-id row))] layers-ps))]
    (cond-> {:row (value-bytes row)
             :row-v-slot (value-bytes (:v row))
             :row-without-lock-id (value-bytes (dissoc row :lock-id))
             :lock-id-key (key-bytes (:lock-id row))
             :lock-id-as-value (value-bytes (:lock-id row))
             :name-key (key-bytes name)}
      rec (merge {:record (value-bytes rec)
                  :record-blob-slot (value-bytes (:blob rec))
                  :record-without-nil-any-blobs (value-bytes (dissoc rec :any-blobs))
                  :record-without-any-blobs-and-empty-any-of (value-bytes (dissoc rec :any-blobs :any-of))
                  :row-with-lock-in-record (value-bytes (assoc row :lock rec))}))))

;; ------------------------------------------------------------------ a run

(defn run-variant
  [vname cfg n every inflight scratch]
  (println (format "== %s %s: %,d values, a point every %,d" vname (pr-str cfg) n every))
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc slice/LockSlice {:tasks 1 :threads 1 :workers 1})
    (let [mn (get-module-name slice/LockSlice)
          depot (foreign-depot ipc mn "*values")
          pname (if (= :raw (:encoding cfg)) "$$layers-raw" "$$layers")
          layers-ps (foreign-pstate ipc mn pname)
          root (ipc-root ipc)
          r (Random. 20260925)
          sem (Semaphore. inflight)
          errors (atom [])
          not-written (atom 0)
          sample-every (max 1 (quot n 100))
          sample (atom [])
          drain! (fn [] (.acquire sem inflight) (.release sem inflight))
          t0 (System/nanoTime)
          points
          (loop [i 0 points []]
            (if (= i n)
              points
              (let [spec (value-spec r i cfg)
                    rec (slice/value-record (merge spec {:layer layer :owner owner
                                                         :encoding (:encoding cfg)
                                                         :row-lock (:row-lock cfg)}))]
                (when (zero? (mod i sample-every)) (swap! sample conj {:name (:name rec) :v (:v rec)}))
                (.acquire sem)
                (.whenComplete ^java.util.concurrent.CompletableFuture (foreign-append-async! depot rec :ack)
                               (reify BiConsumer
                                 (accept [_ res t]
                                   (when t (swap! errors conj (str t)))
                                   (when (and (nil? t) (not= :written (get res "slice"))) (swap! not-written inc))
                                   (.release sem))))
                (let [i (inc i)]
                  (if (zero? (mod i every))
                    (do (drain!)
                        (let [dirs (rocks-dirs root pname)
                              p {:values i
                                 :elapsed-ms (quot (- (System/nanoTime) t0) 1000000)
                                 :rocks-dirs (count dirs)
                                 :logical (if (:row-lock cfg) (lock-rows-logical layers-ps 10000) {:rows 0 :key-bytes 0 :record-bytes 0})
                                 :live (live-bytes dirs)
                                 :compacted (compacted-bytes dirs scratch)
                                 :replog (replog-bytes root)}]
                          (println (format "  %,7d values  lock rows %,7d  logical %,11d  live %,11d  compacted sst %,11d  kv %,11d"
                                           i (-> p :logical :rows)
                                           (+ (-> p :logical :key-bytes) (-> p :logical :record-bytes))
                                           (reduce + (vals (dissoc (:live p) :info-log)))
                                           (-> p :compacted :sst)
                                           (+ (-> p :compacted :kv-key-bytes) (-> p :compacted :kv-value-bytes))))
                          (recur i (conj points p))))
                    (recur i points))))))
          _ (drain!)
          checks (check-rows layers-ps cfg @sample)
          breakdown (sample-breakdown layers-ps (:name (first @sample)))
          result {:variant vname
                  :config cfg
                  :values n
                  :every every
                  :layer layer
                  :launch {:tasks 1 :threads 1 :workers 1}
                  :inflight inflight
                  :pstate pname
                  :errors (count @errors)
                  :first-errors (take 3 @errors)
                  :not-written @not-written
                  :checks checks
                  :breakdown breakdown
                  :points points
                  :finished (str (java.time.LocalDateTime/now))
                  :jvm (str (System/getProperty "java.vm.name") " " (System/getProperty "java.runtime.version"))}]
      (println "  checks" checks)
      (println "  breakdown" breakdown)
      (println "  errors" (count @errors) "not written" @not-written)
      result)))

(defn- scratch-dir []
  (let [d (io/file (System/getProperty "java.io.tmpdir") (str "lock-bench-" (System/currentTimeMillis)))]
    (.mkdirs d)
    d))

(defn run
  "clojure -M:bench rig.bench.lock-bench run <variant,...|all> [values] [every] [inflight]"
  [which & [n every inflight]]
  (RocksDB/loadLibrary)
  (let [n (if n (Long/parseLong n) 100000)
        every (if every (Long/parseLong every) 10000)
        inflight (if inflight (Long/parseLong inflight) 64)
        names (if (= "all" which) (keys variants) (str/split which #","))
        scratch (scratch-dir)]
    (try
      (doseq [vname names]
        (let [cfg (or (get variants vname) (throw (ex-info "no such variant" {:variant vname})))
              res (run-variant vname cfg n every inflight scratch)]
          (spit results-file (str (pr-str res) "\n") :append true)
          (println "  appended to" results-file)))
      (finally (rm-rf scratch)))))

;; ----------------------------------------------------------------- report

(defn- load-results
  "The latest result per variant name and value count."
  []
  (->> (str/split-lines (slurp results-file))
       (remove str/blank?)
       (map edn/read-string)
       (reduce (fn [m r] (assoc m [(:variant r) (:values r)] r)) {})
       vals))

(defn- live-total [p] (+ (-> p :live :wal) (-> p :live :sst) (-> p :live :meta)))
(defn- kv-total [p] (+ (-> p :compacted :kv-key-bytes) (-> p :compacted :kv-value-bytes)))
(defn- logical-total [p] (+ (-> p :logical :key-bytes) (-> p :logical :record-bytes)))

(defn- per [x n] (if (zero? n) 0.0 (/ (double x) n)))

(defn wrap-costs
  "Computed, not measured on disk: serialized lock-record bytes as the wrap
  grows, in the chain form (every person required: a one-owner layer's
  marked value) and the copy form (any one of them: a shared layer, 7b)."
  []
  (let [K (slice/fresh-bytes 32)
        people [:alice :bob :carol]]
    (println)
    (println "What each extra person in a wrap costs (computed with the slice's wrap-lock and Rama's serializer; record bytes, key excluded):")
    (doseq [enc [:base64 :raw] form [:required :any-of]]
      (let [sizes (for [k [1 2 3]]
                    (let [ps (vec (take k people))
                          w (if (= :required form) {:required ps :any-of []} {:required [] :any-of ps})]
                      (value-bytes (slice/wrap-lock enc K w (repeatedly k #(slice/fresh-bytes 12))))))]
        (println (format "  %-6s %-9s 1 person %3d B, 2 persons %3d B, 3 persons %3d B: +%d B, then +%d B per person"
                         (name enc) (if (= :required form) "chain" "copies")
                         (nth sizes 0) (nth sizes 1) (nth sizes 2)
                         (- (nth sizes 1) (nth sizes 0)) (- (nth sizes 2) (nth sizes 1))))))
    (let [nm (env/make-name layer :by-layer)]
      (println (format "  keys: a per-value lock id [:value [name 0]] %d B; a per-act lock id [:act name] %d B; the name alone %d B"
                       (key-bytes [:value [nm 0]]) (key-bytes [:act nm]) (key-bytes nm))))))

(defn- live-shape
  "Where the live directory's bytes were at each point: WAL or SST."
  [r]
  (str/join ", " (for [p (:points r)]
                   (format "%dk %s" (quot (:values p) 1000)
                           (if (pos? (-> p :live :sst))
                             (format "wal %.1fM+sst %.1fM" (/ (-> p :live :wal) 1e6) (/ (-> p :live :sst) 1e6))
                             (format "wal %.1fM" (/ (-> p :live :wal) 1e6)))))))

(defn report
  "Tables and ratios from runs/phase7-lock-growth.edn."
  [& [values]]
  (let [values (if values (Long/parseLong values) 100000)
        rs (into {} (for [r (load-results) :when (= values (:values r))] [(:variant r) r]))
        pairs (for [base ["small-b64" "small-raw" "large-b64" "large-raw" "two-b64"]
                    :let [l (rs (str base "-lock")) z (rs (str base "-none"))]
                    :when (and l z)]
                [base l z])]
    (println (format "Lock store under the hand layer %s, %,d values; bytes per value at each point." layer values))
    (doseq [[base l z] pairs]
      (let [size (-> l :config :size)]
        (println)
        (println (format "%s: values of %d bytes (canonical EDN), %s slots, %d person(s) in the wrap; checks %s; errors %d/%d"
                         base size (name (-> l :config :encoding)) (-> l :config :people)
                         (pr-str (:checks l)) (:errors l) (:errors z)))
        (println "  values | logical lock rows (B/v) | lock-store raw KV (B/v) | lock-store live dir (B/v) | lock-store flushed SST (B/v) | lock-store compacted SST (B/v) | no-lock compacted SST (B/v)")
        (doseq [[pl pz] (map vector (:points l) (:points z))]
          (let [n (:values pl)
                fl (fn [p] (or (-> p :compacted :sst-flushed) 0))]
            (assert (= n (:values pz)))
            (println (format "  %,7d | %,11d (%6.1f) | %,11d (%6.1f) | %,11d (%6.1f)   | %,11d (%6.1f)      | %,11d (%6.1f)        | %,11d (%6.1f)"
                             n
                             (logical-total pl) (per (logical-total pl) n)
                             (- (kv-total pl) (kv-total pz)) (per (- (kv-total pl) (kv-total pz)) n)
                             (- (live-total pl) (live-total pz)) (per (- (live-total pl) (live-total pz)) n)
                             (- (fl pl) (fl pz)) (per (- (fl pl) (fl pz)) n)
                             (- (-> pl :compacted :sst) (-> pz :compacted :sst)) (per (- (-> pl :compacted :sst) (-> pz :compacted :sst)) n)
                             (-> pz :compacted :sst) (per (-> pz :compacted :sst) n)))))
        (let [steps (fn [f] (let [xs (cons 0 (map f (:points l) (:points z)))]
                              (map - (rest xs) xs)))
              lsteps (steps (fn [pl _] (logical-total pl)))
              csteps (steps (fn [pl pz] (- (-> pl :compacted :sst) (-> pz :compacted :sst))))
              last-l (last (:points l)) last-z (last (:points z)) n (:values last-l)
              logical (per (logical-total last-l) n)
              kvd (per (- (kv-total last-l) (kv-total last-z)) n)
              sst (per (- (-> last-l :compacted :sst) (-> last-z :compacted :sst)) n)
              value-sst (per (-> last-z :compacted :sst) n)
              value-kv (per (kv-total last-z) n)]
          (println (format "  steps of 10,000: logical min %,d max %,d; compacted lock-store SST min %,d max %,d"
                           (apply min lsteps) (apply max lsteps) (apply min csteps) (apply max csteps)))
          (println (format "  ratio to the value's plaintext (%d B): logical %.2f, raw KV %.2f, compacted disk %.2f"
                           size (/ logical size) (/ kvd size) (/ sst size)))
          (println (format "  ratio to the value rows as stored without lock rows: raw KV %.2f (%.1f B/v), compacted disk %.2f (%.1f B/v)"
                           (/ kvd value-kv) value-kv (/ sst value-sst) value-sst))
          (println (format "  ratio to the sealed value slot alone (%d B serialized): logical %.2f"
                           (-> l :breakdown :row-v-slot) (/ logical (-> l :breakdown :row-v-slot))))
          (println (format "  entries: lock variant %,d, no-lock variant %,d (difference %,d); per column family at %,d: %s"
                           (-> last-l :compacted :kv-count) (-> last-z :compacted :kv-count)
                           (- (-> last-l :compacted :kv-count) (-> last-z :compacted :kv-count)) n
                           (pr-str (into {} (for [[cf m] (-> last-l :compacted :per-cf)]
                                              [cf [(:kv-count m) (- (:kv-count m) (get-in last-z [:compacted :per-cf cf :kv-count] 0))]])))))
          (println "  live directory, lock variant:" (live-shape l))
          (println "  live directory, no-lock variant:" (live-shape z))
          (println (format "  replication log: lock variant %.1f B/v, no-lock %.1f B/v, difference %.1f B/v"
                           (per (:replog last-l) n) (per (:replog last-z) n) (per (- (:replog last-l) (:replog last-z)) n)))
          (println "  breakdown" (pr-str (:breakdown l))))))
    (wrap-costs)))
