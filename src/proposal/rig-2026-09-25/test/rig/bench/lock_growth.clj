(ns rig.bench.lock-growth
  "Number 2 (PLAN-numbers.md 6): lock store growth under hand layers, bytes
  per value and the curve over 100,000 values, on the finished store's real
  lock rows.

  From the rig folder, one variant a JVM and a cluster, under the cluster
  lock (8.1):

    clojure -M:bench rig.bench.lock-growth run <variant>

  Each variant: a fresh cluster, {:tasks 1 :threads 1 :workers 1}; the
  store layer, the persons, the hand layer `:alice-hand` ({:kind :hand
  :owner :alice}, per-value grain from its making act) and its root
  permission; for the `:mention` variants the layer's own `:mention` grammar
  written as a fact first (B9); for the per-act variant the grain switch.
  64 writer threads share one door, each offering one act at a time as
  `:who :alice` under the root permission in the door's default session,
  taking the values in order from one shared counter. At every point all
  writers finish their act and wait, and the point is measured (6.4): the
  lock rows read back and sized in Rama's own serializers; the layer store
  copied, force-compacted and measured; the lock rows picked out of that
  copy and re-packed alone (6.5); the live directory; the replication log.

  Every variant's RESULT goes to `runs/phase7-final-lock-growth.edn`."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [rig.bench.lock-bench :as lb]
            [rig.bench.numbers :as nb]
            [rig.store.client :as c]
            [rig.store.envelope :as env]
            [rig.store.locks :as locks]
            [rpl.rama.api.durable.rocksdb.key-encoding :as rkey]
            [rpl.rama.util.nippy-serialization :as rser])
  (:import [java.io File]
           [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets]
           [java.nio.file CopyOption Files StandardCopyOption]
           [java.util ArrayList Arrays Random]
           [java.util.concurrent Callable ExecutorService Executors Future]
           [java.util.concurrent.atomic AtomicLong LongAdder]
           [org.rocksdb ColumnFamilyDescriptor ColumnFamilyHandle CompactRangeOptions
            CompactRangeOptions$BottommostLevelCompaction ConfigOptions DBOptions FlushOptions OptionsUtil
            Range RocksDB RocksIterator SizeApproximationFlag Slice]))

(def number :lock-growth)

(def layer :alice-hand)
(def owner :alice)
(def seed "The slice's seed (lock_bench.clj `run-variant`)." 20260925)
(def writers "Door threads (6.3): 64 in flight, as the slice kept 64 appends in flight." 64)

(def variants
  "6.3's table. `:size` is the value's canonical EDN bytes; `:persons` the
  people a `:mention` names beside the owner (marked `:die-with-any`, so the
  wrap requires them all); `:per-act` the values an act holds under per-act
  grain; `:set` the run set it belongs to (8.2)."
  (array-map
   "h40"       {:values 100000 :every 10000 :size 40  :persons nil                      :grain :per-value :set :minimum}
   "h200"      {:values 100000 :every 10000 :size 200 :persons nil                      :grain :per-value :set :minimum}
   "h40-p2"    {:values 100000 :every 10000 :size 40  :persons #{:bob}                  :grain :per-value :set :minimum}
   "h40-p3"    {:values 10000  :every 2000  :size 64  :persons #{:bob :carol}           :grain :per-value :set :minimum}
   "h40-p5"    {:values 10000  :every 2000  :size 64  :persons #{:bob :carol :dave :erin} :grain :per-value :set :minimum}
   "h40-again" {:values 100000 :every 10000 :size 40  :persons nil                      :grain :per-value :set :full}
   "h40-act4"  {:values 100000 :every 10000 :size 40  :persons nil :per-act 4           :grain :per-act   :set :full}))

(defn- smaller
  "A small run's variant: the same values, far fewer."
  [cfg]
  (if nb/smoke?
    (assoc cfg :values (if (= 10000 (:values cfg)) 1000 2000) :every (if (= 10000 (:values cfg)) 500 1000))
    cfg))

;; =============================================================== the values

(def ^:private letters "abcdefghijklmnopqrstuvwxyz ")

(defn- text
  "The slice's text: `n` characters drawn from `r`."
  [^Random r n]
  (let [sb (StringBuilder.)]
    (dotimes [_ n] (.append sb (.charAt ^String letters (.nextInt r (count letters)))))
    (str sb)))

(defn value-spec
  "The i-th value fact of a variant (6.3): the slice's seeded generator
  (`rig.bench.lock-bench/value-spec`, seed 20260925) extended to a set of
  persons, so `h40`, `h200` and `h40-p2` get the slice's values in the
  slice's order. `{:persons ps :text \"..\"}` under `:mention`, marked
  `:die-with-any`, for a variant that names persons; else `{:text \"..\"}`
  under the primary key, unmarked. The value's canonical EDN is exactly
  `size` bytes; its entity is `:n<i>`, one per value."
  [^Random r i {:keys [size persons]}]
  (let [base (if (seq persons) {:persons persons :text ""} {:text ""})
        overhead (count (.getBytes ^String (env/canonical base) StandardCharsets/UTF_8))
        v (assoc base :text (text r (- size overhead)))]
    {:e (keyword (format "n%06d" i))
     :k (if (seq persons) :mention nb/primary-key)
     :v v
     :mark (if (seq persons) #{:die-with-any} #{})}))

(defn canonical-size
  "The bytes the door seals for value `v` (`locks/canonical-bytes`)."
  [v]
  (alength ^bytes (locks/canonical-bytes v)))

;; ================================================================ the sizes

(def ^:private big-buf
  (ThreadLocal/withInitial (reify java.util.function.Supplier
                             (get [_] (ByteBuffer/allocate (* 4 1024 1024))))))

(defn value-bytes
  "Bytes of `v` as Rama 1.6.0 serializes a PState value (B12; the slice's
  `lock-bench/value-bytes`, here per thread)."
  [v]
  (let [^ByteBuffer bb (.get ^ThreadLocal big-buf)]
    (.clear bb) (rser/freeze bb v) (.position bb)))

(defn key-bytes
  "Bytes of `k` as Rama 1.6.0 encodes a RocksDB key (B12)."
  [k]
  (let [^ByteBuffer bb (.get ^ThreadLocal big-buf)]
    (.clear bb) (rkey/k-ser bb k) (.position bb)))

(defn lock-rows-logical
  "6.4 item 1: every lock row of `[L :locks]`, read back in pages of
  `page` (`sorted-map-range` paging, the slice's `lock-rows-logical` made
  to take any layer), each key sized by Rama's key encoder and each record
  by its PState value serializer: count, total key and record bytes, the
  smallest and largest row. With `sample-every`, every so many rows kept
  as [lock-id record] (T7 reads them again by keypath)."
  ([ps L page] (lock-rows-logical ps L page nil))
  ([ps L page sample-every]
   (loop [after nil
          acc {:rows 0 :key-bytes 0 :record-bytes 0 :row-min Long/MAX_VALUE :row-max 0 :sampled []}]
     (let [path (if after
                  [(keypath L :locks) (sorted-map-range-from after {:max-amt page :inclusive? false}) ALL]
                  [(keypath L :locks) (sorted-map-range-from-start page) ALL])
           rows (foreign-select path ps)]
       (if (empty? rows)
         (cond-> acc (zero? (:rows acc)) (assoc :row-min 0))
         (recur (first (peek rows))
                (reduce (fn [a [lid rec]]
                          (let [kb (key-bytes lid) vb (value-bytes rec) t (+ kb vb)]
                            (cond-> (-> a (update :rows inc) (update :key-bytes + kb) (update :record-bytes + vb)
                                        (update :row-min min t) (update :row-max max t))
                              (and sample-every (zero? (mod (:rows a) sample-every)))
                              (update :sampled conj [lid rec kb vb]))))
                        acc rows)))))))

;; ============================================================== the writers

(defn- act-offer
  "One act of the facts `fs` into the hand layer, as `:alice` under her root
  permission, in the door's default session."
  [fs]
  (c/build {:who owner :layer layer :class :by-layer :permission [owner layer layer] :facts (vec fs)}))

(defn write-phase!
  "One stretch of writing, up to the next point (6.3): the writers take the
  acts of `pairs` ([value-index fact], in order, `per-act` facts an act)
  from one shared counter, each offering one act at a time through the one
  door, until all are answered; then every writer has finished its act and
  waits. Keeps every `sample-every`-th value for the end checks. {:answers
  {key n} :first-error s :samples [...] :secs s}."
  [^ExecutorService ex st pairs per-act sample-every progress]
  (let [n-acts (quot (count pairs) per-act)
        next-act (AtomicLong.)
        yes (LongAdder.) errs (LongAdder.)
        t0 (System/nanoTime)
        stop (volatile! false)
        prog (doto (Thread. ^Runnable
                            (fn []
                              (loop []
                                (Thread/sleep 5000)
                                (when-not @stop
                                  (progress (Math/round (/ (- (System/nanoTime) t0) 1e9)) (.sum yes) (.sum errs))
                                  (recur))))
                            "phase7-progress")
               (.setDaemon true) (.start))
        work (fn []
               (loop [answers {} samples [] first-error nil]
                 (let [j (.getAndIncrement next-act)]
                   (if (>= j n-acts)
                     {:answers answers :samples samples :first-error first-error}
                     (let [ps (subvec pairs (* j per-act) (* (inc j) per-act))
                           o (act-offer (map second ps))
                           a (try (c/offer! st o) (catch Exception e e))
                           k (nb/outcome-key a)]
                       (case k :yes (.increment yes) :error (.increment errs) nil)
                       (recur (update answers k (fnil inc 0))
                              (into samples (for [[idx [vi f]] (map-indexed vector ps)
                                                  :when (and (= :yes k) (zero? (mod vi sample-every)))]
                                              {:name (:name o) :idx idx :i vi :v (:v f) :stamp (:stamp a)}))
                              (or first-error (when (= :error k) (str a)))))))))
        futs (mapv (fn [_] (.submit ex ^Callable work)) (range writers))
        rs (mapv #(.get ^Future %) futs)]
    (vreset! stop true)
    {:answers (apply merge-with + (map :answers rs))
     :first-error (some :first-error rs)
     :samples (vec (mapcat :samples rs))
     :secs (nb/secs-since t0)}))

;; ============================================================ the disk side

(defn- info-log? [^String n] (or (= "LOG" n) (str/starts-with? n "LOG.old")))

(defn- listing [^File d]
  (into (sorted-map) (for [^File f (.listFiles d) :when (.isFile f)] [(.getName f) (.length f)])))

(defn- wait-stable
  "Wait until a directory's file names and lengths hold still for 300 ms
  (the slice's)."
  [^File d]
  (loop [prev (listing d) tries 0]
    (Thread/sleep 300)
    (let [cur (listing d)]
      (if (or (= prev cur) (> tries 100)) cur (recur cur (inc tries))))))

(defn- remove-tree
  "The harness's own scratch copy, removed after its point (6.4 item 3)."
  [^File f]
  (when (.isDirectory f) (doseq [c (.listFiles f)] (remove-tree c)))
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

(defn- cf-name [^ColumnFamilyHandle h] (String. (.getName h) StandardCharsets/UTF_8))

(defn- copy-dir!
  "Every file of `d` but LOCK and the info LOG, into a fresh `copy`."
  [^File d ^File copy]
  (.mkdirs copy)
  (doseq [^File x (.listFiles d)
          :when (and (.isFile x) (not= "LOCK" (.getName x)) (not (info-log? (.getName x))))]
    (Files/copy (.toPath x) (.toPath (io/file copy (.getName x)))
                ^"[Ljava.nio.file.CopyOption;" (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING]))))

(defn compacted
  "6.4 item 3: RocksDB directory `d` (the home task's `$$layers`) copied to
  scratch once its listing held still, without its LOCK and info LOG;
  opened with the options Rama wrote into it (B12's
  `OptionsUtil/loadLatestOptions`); every column family flushed, then
  compacted with kForce (without it a lone flushed file is moved, not
  rewritten, the slice's finding); its SST bytes and every key and value
  counted, per column family (Rama keeps top-level entries in `default` and
  every subindexed element in `subindexed`). `(f db {cf-name handle} copy)`
  runs while the copy is open; its result is `:inside`. The copy is
  removed after; a failure is retried up to five times, as the slice's."
  [^File d ^File scratch f]
  (loop [attempt 1]
    (wait-stable d)
    (let [copy (io/file scratch (str "copy-" (System/nanoTime)))
          r (try
              (copy-dir! d copy)
              (let [dbo (DBOptions.) cfds (ArrayList.) handles (ArrayList.)]
                (OptionsUtil/loadLatestOptions (ConfigOptions.) (.getPath copy) dbo cfds)
                (.setCreateIfMissing dbo false)
                (let [db (RocksDB/open dbo (.getPath copy) cfds handles)]
                  (try
                    (doseq [^ColumnFamilyHandle h handles]
                      (with-open [fo (doto (FlushOptions.) (.setWaitForFlush true))] (.flush db fo h)))
                    (with-open [cro (doto (CompactRangeOptions.)
                                      (.setBottommostLevelCompaction CompactRangeOptions$BottommostLevelCompaction/kForce))]
                      (doseq [^ColumnFamilyHandle h handles] (.compactRange db h nil nil cro)))
                    (let [by-name (into {} (for [^ColumnFamilyHandle h handles] [(cf-name h) h]))
                          per-cf (into (sorted-map)
                                       (for [[n ^ColumnFamilyHandle h] by-name]
                                         [n (assoc (scan-kv db h)
                                                   :sst (Long/parseLong (.getProperty db h "rocksdb.total-sst-files-size")))]))]
                      (assoc (apply merge-with + (map #(select-keys % [:sst :kv-count :kv-key-bytes :kv-value-bytes])
                                                      (vals per-cf)))
                             :per-cf per-cf
                             :inside (f db by-name copy)))
                    (finally
                      (doseq [^ColumnFamilyHandle h handles] (.close h))
                      (.close db)))))
              (catch Throwable t {:error (str t)})
              (finally (remove-tree copy)))]
      (if (and (:error r) (< attempt 5))
        (do (Thread/sleep 1000) (recur (inc attempt)))
        r))))

;; ======================================================= the pick (6.5)

(defn- classify
  "What a thawed value is, for the pick: `:lock` for a lock record, in a
  hand layer the only value that is a plain map carrying `:scheme` and
  `:required` (6.5; locks.clj `lock-record-schema`); `:ref` for a value
  whose key lookup throws; `:other` for everything else. Total. Rama's
  `thaw` accepts the references to nested subindexed structures (`:log`'s
  vectors, `:leases`' session maps): it returns a
  `key_encoding.ReferenceID`, whose lookup of an undeclared key throws
  `NoSuchFieldError` (the first run's T7, 26 September). F9 expected
  `thaw` to refuse them; they are counted, never picked, never silently
  dropped."
  [x]
  (try
    (if (and (map? x) (not (record? x)) (contains? x :scheme) (contains? x :required)) :lock :other)
    (catch Throwable _ :ref)))

(defn- thawed
  "[:ok value] by Rama's own `thaw` (B12), or [:unthawed nil] (F9)."
  [^bytes v]
  (try [:ok (rser/thaw (ByteBuffer/wrap v))] (catch Throwable _ [:unthawed nil])))

(defn- common-len
  "How many leading bytes `a` (its first `n`) and `b` share."
  ^long [^bytes a ^bytes b ^long n]
  (let [m (min n (alength b))]
    (loop [i 0] (if (and (< i m) (== (aget a i) (aget b i))) (recur (inc i)) i))))

(defn- starts-with? [^bytes k ^bytes p]
  (and (>= (alength k) (alength p))
       (Arrays/equals k 0 (alength p) p 0 (alength p))))

(defn- above-prefix
  "The least byte string above every string that starts with `p`, or nil
  when there is none (p all 0xFF)."
  [^bytes p]
  (loop [i (dec (alength p))]
    (cond
      (neg? i) nil
      (not= (byte -1) (aget p i)) (let [q (Arrays/copyOf p (int (inc i)))]
                                    (aset-byte q i (unchecked-byte (inc (aget p i))))
                                    q)
      :else (recur (dec i)))))

(defn- count-under
  "How many keys of the family start with `p`."
  [^RocksDB db ^ColumnFamilyHandle h ^bytes p]
  (with-open [^RocksIterator it (.newIterator db h)]
    (.seek it p)
    (loop [n 0]
      (if (and (.isValid it) (starts-with? (.key it) p))
        (do (.next it) (recur (inc n)))
        n))))

(defn pick-lock-rows
  "6.5's pick, in a compacted copy's `subindexed` family, the whole family
  scanned at every point: every value that Rama's `thaw` turns into a lock
  record; a value `thaw` does not accept is counted as unthawed, never
  dropped silently (F9). P is the picked keys' longest common prefix (F9);
  the keys under P are counted again, and the pick is one structure when
  every key under P was picked. `:runs` is how many separate stretches of
  key order the picked entries make. The picked [key value] pairs are kept,
  for the re-pack.

  Every point scans the whole family, where the plan seeks the points
  between the first and the last to the first point's P: P can reach into
  the lock ids' own bytes (their lease names are time-ordered uuids), so
  rows written later need not start with the first point's P. The full scan
  costs seconds a point and checks one structure at every point
  (BUILD_NOTES-numbers.md)."
  [^RocksDB db ^ColumnFamilyHandle h]
  (let [kept (ArrayList.)]
    (with-open [^RocksIterator it (.newIterator db h)]
      (.seekToFirst it)
      (loop [n 0 picked 0 kb 0 vb 0 unthawed 0 refs 0 lcp nil lcp-len 0 runs 0 prev? false]
        (if (.isValid it)
          (let [k (.key it) v (.value it)
                [s x] (thawed v)
                cls (if (= :unthawed s) :unthawed (classify x))
                lock? (= :lock cls)]
            (when lock? (.add kept [k v]))
            (.next it)
            (recur (inc n) (if lock? (inc picked) picked) (if lock? (+ kb (alength k)) kb)
                   (if lock? (+ vb (alength v)) vb) (if (= :unthawed cls) (inc unthawed) unthawed)
                   (if (= :ref cls) (inc refs) refs)
                   (if (and lock? (nil? lcp)) k lcp)
                   (cond (not lock?) lcp-len
                         (nil? lcp) (alength ^bytes k)
                         :else (common-len lcp k lcp-len))
                   (if (and lock? (not prev?)) (inc runs) runs)
                   lock?))
          (let [p (when lcp (Arrays/copyOf ^bytes lcp (int lcp-len)))
                under (when p (count-under db h p))]
            {:scanned n :picked picked :key-bytes kb :value-bytes vb :unthawed unthawed :references refs
             :prefix p :prefix-length (when p (alength ^bytes p)) :runs runs :under-p under
             :unpicked-under-p (when under (- under picked))
             :one-structure? (boolean (and p (= under picked)))
             :kept kept}))))))

(defn- approximate-under
  "RocksDB's own approximate size of the key range [P, P+1) (6.5's
  cross-check, printed beside the re-pack), or why it could not say."
  [^RocksDB db ^ColumnFamilyHandle h ^bytes p]
  (try
    (let [q (or (above-prefix p) (byte-array (repeat (inc (alength p)) (byte -1))))
          sizes (.getApproximateSizes db h (java.util.List/of (Range. (Slice. p) (Slice. ^bytes q)))
                                      ^"[Lorg.rocksdb.SizeApproximationFlag;"
                                      (into-array SizeApproximationFlag [SizeApproximationFlag/INCLUDE_FILES]))]
      (aget ^longs sizes 0))
    (catch Throwable t (str t))))

(defn repack
  "6.5's re-pack: a fresh RocksDB in scratch, the DB options and the
  `subindexed` family's options loaded from the copy, the picked entries
  put in key order, flushed and force-compacted; its SST bytes, the lock
  store's bytes on disk. Then scanned back (F8): the count and the key and
  value bytes of what it holds, which must be exactly the picked entries."
  [^File copy entries ^File scratch]
  (let [dir (io/file scratch (str "repack-" (System/nanoTime)))
        dbo (DBOptions.)
        cfds (ArrayList.)]
    (OptionsUtil/loadLatestOptions (ConfigOptions.) (.getPath copy) dbo cfds)
    (.setCreateIfMissing dbo true)
    (.setCreateMissingColumnFamilies dbo true)
    (let [named (fn [nm] (first (filter #(= nm (String. (.getName ^ColumnFamilyDescriptor %) StandardCharsets/UTF_8)) cfds)))
          handles (ArrayList.)]
      (.mkdirs dir)
      (try
        (let [db (RocksDB/open dbo (.getPath dir) (java.util.List/of (named "default") (named "subindexed")) handles)
              ^ColumnFamilyHandle h (.get handles 1)]
          (try
            (doseq [[k v] entries] (.put db h ^bytes k ^bytes v))
            (with-open [fo (doto (FlushOptions.) (.setWaitForFlush true))] (.flush db fo h))
            (with-open [cro (doto (CompactRangeOptions.)
                              (.setBottommostLevelCompaction CompactRangeOptions$BottommostLevelCompaction/kForce))]
              (.compactRange db h nil nil cro))
            {:sst (Long/parseLong (.getProperty db h "rocksdb.total-sst-files-size"))
             :read-back (scan-kv db h)}
            (finally
              (doseq [^ColumnFamilyHandle x handles] (.close x))
              (.close db))))
        (finally (remove-tree dir))))))

(defn lock-store-on-disk
  "6.4 items 3 to 5 for one point: the layer store compacted; the lock rows
  picked out of it; the checks of 6.5 (one structure; the exact count of
  `logical`'s rows; the raw bytes within 4 a row of the logical ones); the
  re-pack when they hold, scanned back (F8), else the named estimate with
  what it assumes and its two bounds (F9); RocksDB's approximate size under
  P beside it."
  [^File d ^File scratch logical]
  (let [c (compacted
           d scratch
           (fn [db by-name copy]
             (let [h (get by-name "subindexed")
                   pk (pick-lock-rows db h)
                   rows (:rows logical)
                   raw-per-row (when (pos? rows) (/ (double (+ (:key-bytes pk) (:value-bytes pk))) rows))
                   logical-per-row (when (pos? rows) (/ (double (+ (:key-bytes logical) (:record-bytes logical))) rows))
                   ;; the count check also says whether a value thaw refused was a lock row (F9)
                   checks {:one-structure? (:one-structure? pk)
                           :count? (= rows (:picked pk))
                           :bytes? (boolean (and raw-per-row (<= (Math/abs (- raw-per-row logical-per-row)) 4.0)))}
                   ok? (every? true? (vals checks))
                   rp (when ok? (repack copy (:kept pk) scratch))]
               {:pick (dissoc pk :kept :prefix)
                :prefix (:prefix pk)
                :checks checks
                :raw-per-row raw-per-row
                :logical-per-row logical-per-row
                :repack rp
                :repack-exact? (when rp (and (= (:picked pk) (get-in rp [:read-back :kv-count]))
                                             (= (:key-bytes pk) (get-in rp [:read-back :kv-key-bytes]))
                                             (= (:value-bytes pk) (get-in rp [:read-back :kv-value-bytes]))))
                :approximate (when (:prefix pk) (approximate-under db h (:prefix pk)))})))]
    (if (:error c)
      {:error (:error c)}
      (let [in (:inside c)
            ok? (every? true? (vals (:checks in)))
            rows (:rows logical)
            raw-lock (+ (get-in in [:pick :key-bytes]) (get-in in [:pick :value-bytes]))
            raw-all (+ (:kv-key-bytes c) (:kv-value-bytes c))
            lock-sst (if ok?
                       (get-in in [:repack :sst])
                       (when (pos? raw-all) (Math/round (* (double (:sst c)) (/ (double raw-lock) raw-all)))))]
        {:compacted (dissoc c :inside)
         :lock-store (merge (dissoc in :prefix)
                            {:method (if ok? :re-pack :estimate)
                             :sst lock-sst}
                            (when-not ok?
                              {:failed-checks (vec (keep (fn [[k v]] (when-not v k)) (:checks in)))
                               :assumes "the lock rows compress as the store's average does; the error has no known sign"
                               :bounds {:at-least (* 60 rows) :at-most-raw-bytes raw-lock}}))
         :rest-sst (when lock-sst (- (:sst c) lock-sst))
         :prefix (:prefix in)}))))

;; ============================================================ the end checks

(defn check-values
  "6.4's end checks on sampled values (100): the stored row's `:sealed` is
  not the plaintext; the value opens through its lock row and the person
  entries read from `$$persons` (`locks/unwrap`, then `locks/open`) to its
  canonical text; it does not open without the owner's entry, nor, in the
  `p` variants, without each named person's; the wrap requires exactly the
  owner and the named persons."
  [st cfg samples]
  (let [named (vec (sort (cons owner (seq (:persons cfg)))))
        persons (into {} (for [p named] [p (c/person st p)]))]
    (reduce (fn [acc {:keys [name idx v]}]
              (let [row (nth (vec (c/raw-rows st layer name)) idx nil)
                    rec (some->> (:lock-id row) (c/lock-row st layer))
                    plain (locks/canonical-bytes v)
                    opens (fn [ps] (when-let [K (locks/unwrap rec ps)] (locks/open K (:sealed row))))]
                (cond-> (update acc :checked inc)
                  (and (bytes? (:sealed row)) (not (Arrays/equals ^bytes plain ^bytes (:sealed row))))
                  (update :sealed-is-not-plaintext inc)
                  (Arrays/equals ^bytes plain ^bytes (opens persons))
                  (update :opened inc)
                  (every? (fn [p] (nil? (opens (dissoc persons p)))) named)
                  (update :closed-without-each-named inc)
                  (= named (:required rec))
                  (update :wrap-as-expected inc))))
            {:checked 0 :sealed-is-not-plaintext 0 :opened 0 :closed-without-each-named 0 :wrap-as-expected 0}
            (take 100 samples))))

(defn breakdown
  "6.6: one stored value's lock row and row, part by part, in Rama's
  serializers: the record and its key; the record's 60-byte sealed lock,
  wrap vectors, `:scheme` and nil `:any-blobs`; outside the lock store, the
  row's `:sealed`, `:lock-id` and `:digest`."
  [st {:keys [name idx]}]
  (let [row (nth (vec (c/raw-rows st layer name)) idx nil)
        rec (some->> (:lock-id row) (c/lock-row st layer))]
    {:lock-row {:key (key-bytes (:lock-id row)) :record (value-bytes rec)
                :parts {:blob (value-bytes (:blob rec)) :blob-length (some-> ^bytes (:blob rec) alength)
                        :required (value-bytes (:required rec)) :any-of (value-bytes (:any-of rec))
                        :scheme (value-bytes (:scheme rec)) :any-blobs (value-bytes (:any-blobs rec))}}
     :row {:whole (value-bytes row) :sealed (value-bytes (:sealed row))
           :sealed-length (some-> ^bytes (:sealed row) alength)
           :lock-id (value-bytes (:lock-id row)) :digest (value-bytes (:digest row))}}))

;; ===================================================================== a run

(defn- scratch-dir ^File []
  (doto (io/file (System/getProperty "java.io.tmpdir") (str "phase7-lock-growth-" (System/currentTimeMillis)))
    (.mkdirs)))

(defn- measure-point
  "6.4 at one point, writers paused: the logical rows, the lease rows
  standing (not part of the lock store), the layer store and the lock store
  on disk, the live directory, the replication log."
  [{:keys [st root scratch session]} n t0]
  (let [ps (:layers st)
        logical (dissoc (lock-rows-logical ps layer 10000) :sampled)
        dirs (lb/rocks-dirs root "$$layers")
        disk (lock-store-on-disk (first dirs) scratch logical)]
    (merge {:values n
            :elapsed-ms (quot (- (System/nanoTime) t0) 1000000)
            :rocks-dirs (count dirs)
            :logical logical
            :lease-rows-standing (or (foreign-select-one [(keypath layer :leases session) (view count)] ps) 0)
            :live (lb/live-bytes dirs)
            :replog (lb/replog-bytes root)}
           (dissoc disk :prefix))))

(defn run
  "One variant of number 2 (6.3 to 6.6), in its own JVM and cluster:
  clojure -M:bench rig.bench.lock-growth run <variant>."
  [vname & _]
  (RocksDB/loadLibrary)
  (let [cfg (smaller (or (get variants vname) (throw (ex-info "no such variant" {:variant vname}))))]
    (nb/with-run
      {:number number :run vname :fn-name "run" :args [vname] :launch nb/bytes-launch}
      (fn [{:keys [ipc] :as ctx}]
        (let [st (c/connect ipc)
              _ (nb/people! st)
              persons (vec (sort (cons owner (seq (:persons cfg)))))
              _ (doseq [p persons] (nb/person! st p))
              _ (nb/yes! "making the hand layer"
                         (c/offer-until-answered! st (c/make-layer-offer layer {:kind :hand :owner owner})))
              _ (nb/yes! "granting the root" (c/offer-until-answered! st (c/grant-offer st [owner layer layer])))
              _ (when (seq (:persons cfg)) (nb/write-grammars! st layer {:mention nb/mention-grammar}))
              _ (when (= :per-act (:grain cfg))
                  (nb/yes! "the grain switch"
                           (c/offer-until-answered!
                            st (c/build {:who :operator :layer layer :class :by-layer
                                         :facts [{:e layer :k :lock-grain :v :per-act}]}))))
              env {:st st :root (nb/ipc-root ipc) :scratch (scratch-dir) :session (c/default-session owner)}
              r (Random. (long seed))
              per-act (or (:per-act cfg) 1)
              n (:values cfg)
              every (:every cfg)
              sample-every (max 1 (quot n 100))
              ex (Executors/newFixedThreadPool (int writers))
              t0 (System/nanoTime)]
          (try
            (loop [i 0 points [] answers {} samples [] first-error nil]
              (if (>= i n)
                (let [last-p (peek points)
                      result {:variant vname :config cfg :values n :every every :layer layer
                              :launch nb/bytes-launch :writers writers :primary-key nb/primary-key
                              :answers answers
                              :errors (get answers :error 0)
                              :refused (reduce + 0 (vals (dissoc answers :yes :error)))
                              :first-error first-error
                              :lock-rows-expected (quot n per-act)
                              :lock-rows-at-end (get-in last-p [:logical :rows])
                              :checks (check-values st cfg samples)
                              :breakdown (when-let [s (first samples)] (breakdown st s))
                              :points points
                              :secs (nb/secs-since t0)}]
                  (nb/emit! "RESULT" (assoc result :number number :run vname :uid (:uid ctx)) number)
                  result)
                (let [end (min n (+ i every))
                      pairs (vec (for [j (range i end)] [j (value-spec r j cfg)]))
                      _ (when (zero? i)
                          (let [bad (remove #(= (:size cfg) (canonical-size (:v (second %)))) (take 100 pairs))]
                            (when (seq bad)
                              (throw (ex-info "a value is not its intended size" {:first (second (first bad))})))))
                      ph (write-phase! ex st pairs per-act sample-every
                                       (fn [secs yes errs]
                                         (nb/progress! {:number number :run vname :variant (keyword vname) :k writers
                                                        :t secs :acts (+ i (* per-act yes))
                                                        :rate (Math/round (/ (* 1.0 per-act yes) (max 1 secs)))
                                                        :errors errs})))
                      p (assoc (measure-point env end t0) :write-secs (:secs ph))]
                  (nb/log! (format "%s %,d values: lock rows %,d, logical %,d B, compacted store %s B, lock store %s B (%s), rest %s B"
                                   vname end (get-in p [:logical :rows])
                                   (+ (get-in p [:logical :key-bytes]) (get-in p [:logical :record-bytes]))
                                   (get-in p [:compacted :sst]) (get-in p [:lock-store :sst])
                                   (some-> (get-in p [:lock-store :method]) name) (:rest-sst p)))
                  (nb/progress! {:number number :run vname :variant (keyword vname) :k writers
                                 :t (Math/round (/ (- (System/nanoTime) t0) 1e9)) :acts end
                                 :rate (Math/round (/ (* 1.0 (- end i)) (max 0.001 (:secs ph))))
                                 :errors (get (:answers ph) :error 0)})
                  (recur end (conj points p) (merge-with + answers (:answers ph)) (into samples (:samples ph))
                         (or first-error (:first-error ph))))))
            (finally
              (.shutdown ex)
              (remove-tree (:scratch env)))))))))
