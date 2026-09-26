(ns rig.bench.numbers
  "Phase 7's shared harness (PLAN-numbers.md, 9.2): the three numbers on the
  finished store, each through the store's own road (the door, leases,
  sealing, the gate's one event, the read exit), never a slice module.

  This namespace holds what the three harnesses share: the machine record
  (`machine`, 3.1), one cluster per run (`run!`, `launch!`, 8.4's busy
  port), setup through the store's own acts (`people!`, `person!`,
  `layer!`, `layers-on-task!`), the per-act write lists as data (`writes`,
  2.3) and the store's own growth to hold them to (`field-counts`), the
  windows (`closed-window`, `open-window`, `idle-window`), the counts the
  numbers are made of (leases, placement, CPU, GC, the clock's lead), the
  sample read-back (`check-sample`), the overlap monitor (8.4), the output
  (`emit!`, `progress!`), the verdict rules (7.2, 7.3) and the report (8.5).

  Harness state lives in this JVM only; a run's result is its RESULT lines,
  written as it goes to `runs/phase7-final-<number>.edn` (PHASE7_OUT
  overrides the prefix, for small runs). PHASE7_SMOKE=1 shortens every
  window, for the one small run each harness gets before the numbers run.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [rig.store.client :as c]
            [rig.store.clock :as hlc]
            [rig.store.envelope :as env]
            [rig.store.locks :as locks]
            [rig.store.micro-client :as mc]
            [rig.store.module :as m]
            [rig.store.reads :as reads])
  (:import [java.io File]
           [java.lang ProcessHandle ProcessHandle$Info]
           [java.lang.management GarbageCollectorMXBean ManagementFactory ThreadMXBean]
           [java.time Instant LocalTime ZoneId]
           [java.time.temporal ChronoUnit]
           [java.util ArrayList Arrays HashMap]
           [java.util.concurrent Callable CountDownLatch ExecutorService Executors Future]
           [java.util.concurrent.atomic AtomicLong LongAdder]
           [java.util.concurrent.locks LockSupport]))

;; ================================================================= settings

(def smoke?
  "PHASE7_SMOKE=1: every window short and the levels few, for the one small
  run each harness gets before the numbers (never a measurement)."
  (= "1" (System/getenv "PHASE7_SMOKE")))

(def out-prefix
  "Where the `.edn` results go: `<prefix>-<number>.edn`. PHASE7_OUT overrides
  it (the small runs write `runs/phase7-smoke-*`)."
  (or (System/getenv "PHASE7_OUT") "runs/phase7-final"))

(defn out-file [number] (str out-prefix "-" (name number) ".edn"))

(def timing-launch
  "Numbers 1 and 3 (3.2): four tasks, a thread each, one worker, as the
  slices ran; the layer measured lives on one task."
  {:tasks 4 :threads 4 :workers 1})

(def bytes-launch
  "Number 2 (3.2): one task, so the hand layer's whole store is one RocksDB
  directory; bytes do not depend on the task count."
  {:tasks 1 :threads 1 :workers 1})

(def caveat
  "3.3, verbatim, in every result file."
  (str "In-process cluster: every task, the RocksDB stores and the clients run "
       "in one JVM on one machine, with replication factor 1 and this machine's "
       "disk. The timings mean orders of magnitude only (the rama skill's "
       "testing.md, axiom 1; SPEC.md, \"Tests and evidence\"): a result ten times "
       "or more from its threshold decides the question; one within ten times "
       "needs a real cluster. Logical bytes are exact for Rama 1.6.0's "
       "serializers; bytes on disk depend on the RocksDB options Rama sets here."))

(def thresholds-line "thresholds assumed (RIG.md default 7), not Sid's")

(def thresholds
  "RIG.md default 7 (7.1), assumed and not Sid's."
  {:agent-acts-per-s 1000
   :person-acts-per-s 100
   :person-p99-ms 20
   :lock-fine 2
   :lock-change 4})

(def primary-key
  "The primary act's key (F1): `:note`, because the landed stream gate takes
  index hints from the layer's key rows only (gate.clj `decide*`,
  `reads/hints-of`), so a `:note` with no grammar in the layer gets no value
  index. BUILD_NOTES-numbers.md, 'Binding check', B9."
  :note)

;; =================================================================== output

(def ^:private out-lock (Object.))

(defn edn-str
  "x as one line of EDN, nothing elided."
  [x]
  (binding [*print-length* nil *print-level* nil *print-namespace-maps* false]
    (pr-str x)))

(defn clock-now
  "The wall clock as HH:mm:ss, local time (IST on this machine)."
  []
  (str (.truncatedTo (LocalTime/now) ChronoUnit/SECONDS)))

(defn emit!
  "`TAG <edn>` on stdout; META, RESULT and RUN-END maps are also appended to
  `number`'s `.edn`, one map a line, tagged, as they are made (8.5)."
  ([tag m] (emit! tag m nil))
  ([tag m number]
   (locking out-lock
     (println tag (edn-str m))
     (flush)
     (when number
       (let [f (io/file (out-file number))]
         (when-let [d (.getParentFile f)] (.mkdirs d))
         (spit f (str (edn-str (assoc m :tag tag)) "\n") :append true))))))

(defn log!
  "A plain line on stdout with the time, for the step's `.log`."
  [& xs]
  (locking out-lock
    (println (str (clock-now) " " (str/join " " xs)))
    (flush)))

(defn progress!
  "8.3's PROGRESS line: `<time> <number> run=<n> <variant> K=<k> t=<s>
  acts=<n> rate=<r>/s errors=<e>`."
  [{:keys [number run variant k t acts rate errors]}]
  (locking out-lock
    (println (format "PROGRESS %s %s run=%s %s K=%s t=%s acts=%s rate=%s/s errors=%s"
                     (clock-now) (name number) run (name variant) k t acts rate errors))
    (flush)))

;; ================================================================== machine

(defn- sh-out
  "A command's trimmed stdout, or nil when it fails."
  [& args]
  (try
    (let [{:keys [exit out]} (apply sh/sh args)]
      (when (zero? exit) (str/trim out)))
    (catch Throwable _ nil)))

(defn- file-field
  "The value after `sep` on the first line of file `path` that starts with
  `prefix`, trimmed, or nil."
  [path prefix sep]
  (try
    (some (fn [line]
            (when (str/starts-with? line prefix)
              (str/trim (subs line (inc (str/index-of line sep))))))
          (str/split-lines (slurp path)))
    (catch Throwable _ nil)))

(defn- lscpu-field [out label]
  (some (fn [line] (when (str/starts-with? line label) (str/trim (subs line (count label)))))
        (str/split-lines (or out ""))))

(defn- disk-of
  "The filesystem type, device and disk model under `dir`."
  [dir]
  (when dir
    (let [out (sh-out "df" "--output=source,fstype" (str dir))
          [src fstype] (some-> out str/split-lines second str/trim (str/split #"\s+"))
          parent (when src (sh-out "lsblk" "-no" "PKNAME" src))
          disk (if (str/blank? parent) src (str "/dev/" (first (str/split-lines parent))))
          model (when disk (sh-out "lsblk" "-dno" "MODEL" disk))]
      {:filesystem fstype :device src :disk disk :disk-model model})))

(defn- classpath-jar
  "The file name of the first classpath entry matching `re`."
  [re]
  (some (fn [p] (let [n (.getName (io/file p))] (when (re-find re n) n)))
        (str/split (System/getProperty "java.class.path") (re-pattern File/pathSeparator))))

(defn machine
  "The META map of 3.1, recorded by the harness at every run: nothing in it
  is typed by hand. `dir` is the in-process cluster's directory; `ran` names
  the namespace, function and arguments."
  [{:keys [dir ran]}]
  (let [lscpu (sh-out "lscpu")
        cores (try (* (Long/parseLong (lscpu-field lscpu "Core(s) per socket:"))
                      (Long/parseLong (lscpu-field lscpu "Socket(s):")))
                   (catch Throwable _ nil))
        rt (ManagementFactory/getRuntimeMXBean)]
    {:cpu (file-field "/proc/cpuinfo" "model name" ":")
     :cores cores
     :hardware-threads (.availableProcessors (Runtime/getRuntime))
     :ram (file-field "/proc/meminfo" "MemTotal" ":")
     :os (some-> (file-field "/etc/os-release" "PRETTY_NAME" "=") (str/replace "\"" ""))
     :kernel (sh-out "uname" "-r")
     :jdk {:vm (System/getProperty "java.vm.name")
           :runtime (System/getProperty "java.runtime.version")
           :vendor (System/getProperty "java.vendor")}
     :jvm-flags (vec (.getInputArguments rt))
     :gc (mapv #(.getName ^GarbageCollectorMXBean %) (ManagementFactory/getGarbageCollectorMXBeans))
     :rama (classpath-jar #"^rama-\d")
     :rama-helpers (classpath-jar #"^rama-helpers-")
     :clojure (clojure-version)
     :cluster-dir (some-> dir str)
     :disk (disk-of dir)
     :load (some-> (sh-out "cat" "/proc/loadavg") (str/split #"\s+") (->> (take 3) vec))
     :git {:head (sh-out "git" "rev-parse" "HEAD")
           :code-clean? (= "" (sh-out "git" "status" "--short" "--" "src" "test" "deps.edn"))
           :status (some-> (sh-out "git" "status" "--short" "--" "src" "test" "deps.edn")
                           str/split-lines (->> (take 10) vec))}
     :ran ran
     :host (sh-out "hostname")
     :at (str (Instant/now))
     :smoke? smoke?}))

;; ============================================================== the cluster

(defn ipc-root
  "The in-process cluster's own directory (its `temp_dir` field; the slice's
  `lock-bench/ipc-root`)."
  ^File [ipc]
  (let [f (.getDeclaredField (class ipc) "temp_dir")]
    (.setAccessible f true)
    (io/file (str (.get f ipc)))))

(defn- port-busy?
  [^Throwable t]
  (boolean (some #(str/includes? (str (.getMessage ^Throwable %)) "Address already in use")
                 (take-while some? (iterate #(.getCause ^Throwable %) t)))))

(defn launch!
  "A fresh in-process cluster with the finished store launched under
  `launch` (`rig.store.module/Store`, B1). A cluster that cannot bind its
  port is closed and retried every 30 s, up to five times, each retry
  logged (8.4); under the cluster lock this should not happen."
  [launch]
  (loop [attempt 1]
    (let [r (try
              (let [ipc (rtest/create-ipc)]
                (try
                  (rtest/launch-module! ipc m/Store launch)
                  ipc
                  (catch Throwable t (.close ^java.io.Closeable ipc) (throw t))))
              (catch Throwable t t))]
      (cond
        (not (instance? Throwable r)) r
        (and (port-busy? r) (<= attempt 5))
        (do (log! "the cluster's port is busy; retry" attempt "of 5 in 30 s")
            (Thread/sleep 30000)
            (recur (inc attempt)))
        :else (throw r)))))

;; ========================================================= overlap monitor

(defn- java-process? [^ProcessHandle ph]
  (let [cmd (.orElse (.command (.info ph)) "")]
    (boolean (re-find #"/java$" cmd))))

(defn- describe-process [^ProcessHandle ph]
  (let [info (.info ph)
        args (seq (.orElse (.arguments info) nil))
        cmd (str/join " " (take-last 5 args))]
    {:pid (.pid ph)
     :cmd (subs cmd 0 (min 160 (count cmd)))
     :cpu-ms (some-> ^java.time.Duration (.orElse (.totalCpuDuration info) nil) .toMillis)}))

(defn start-overlap-monitor!
  "8.4: a daemon thread that lists every other Java process on the machine
  every 2 seconds. {:log atom-of-[ms [process ...]]}."
  []
  (let [self (.pid (ProcessHandle/current))
        lg (atom [])
        t (Thread. ^Runnable
                   (fn []
                     (loop []
                       (let [now (System/currentTimeMillis)
                             others (try
                                      (->> (iterator-seq (.iterator (ProcessHandle/allProcesses)))
                                           (remove #(= self (.pid ^ProcessHandle %)))
                                           (filter java-process?)
                                           (mapv describe-process))
                                      (catch Throwable _ []))]
                         (swap! lg conj [now others]))
                       (Thread/sleep 2000)
                       (recur)))
                   "phase7-overlap-monitor")]
    (.setDaemon t true)
    (.start t)
    {:log lg}))

(defn overlap-during
  "The other JVMs the monitor saw between wall times `t0` and `t1` (ms),
  each with its command's tail, how often it was seen and the CPU it used
  between its first and last sighting; nil when none ran."
  [mon t0 t1]
  (when mon
    (let [seen (for [[t others] @(:log mon) :when (<= t0 t t1) o others] o)]
      (when (seq seen)
        (vec (for [[pid os] (group-by :pid seen)
                   :let [cpus (keep :cpu-ms os)]]
               {:pid pid :cmd (:cmd (first os)) :sightings (count os)
                :cpu-ms (when (seq cpus) (- (apply max cpus) (apply min cpus)))}))))))

;; ============================================================ one run, one JVM

(defn secs-since [t0] (/ (Math/round (/ (- (System/nanoTime) t0) 1e6)) 1e3))

(defn with-run
  "One run of one number in this JVM, one cluster (8.1): launch the store
  under `launch`, emit META (the machine, recorded now, with the cluster's
  directory; the caveat; the thresholds line; the primary key), start the
  overlap monitor, call `(f ctx)`, emit RUN-END, close the cluster. A run
  that throws emits RUN-END with `:ok? false` and throws on, so the JVM
  exits non-zero and the summary leaves the run out (15, a killed run).
  ctx: {:ipc :number :run :uid :monitor :launch}."
  [{:keys [number run fn-name args launch]} f]
  (let [uid (str (Instant/now))
        mon (start-overlap-monitor!)
        t0 (System/nanoTime)]
    (log! "launching the store" (edn-str launch) (if smoke? "(small run)" ""))
    (let [ipc (launch! launch)
          ctx {:ipc ipc :number number :run run :uid uid :monitor mon :launch launch}]
      (try
        (emit! "META" {:number number :run run :uid uid :launch launch
                       :machine (machine {:dir (ipc-root ipc)
                                          :ran {:ns (str "rig.bench." (name number)) :fn fn-name :args (vec args)}})
                       :caveat caveat :thresholds thresholds-line :primary-key primary-key
                       :smoke? smoke?}
               number)
        (let [r (f ctx)]
          (emit! "RUN-END" {:number number :run run :uid uid :fn fn-name :ok? true :secs (secs-since t0)} number)
          r)
        (catch Throwable t
          (emit! "RUN-END" {:number number :run run :uid uid :fn fn-name :ok? false :error (str t)
                            :secs (secs-since t0)}
                 number)
          (throw t))
        (finally
          (.close ^java.io.Closeable ipc))))))

;; ==================================================================== setup

(defn yes!
  "`a` when it is a yes; otherwise the run stops with the answer printed
  (4.2: every setup act must be answered `:yes`)."
  [what a]
  (when-not (= :yes (:answer a))
    (throw (ex-info (str what ": not admitted") {:what what :answer a})))
  a)

(defn partition-ends
  "Each `*offers` partition's end offset: how many records it holds."
  [st tasks]
  (mapv #(:end-offset (foreign-depot-partition-info (:depot st) %)) (range tasks)))

(defn grown
  "The partitions (or tasks) whose count moved between two snapshots."
  [before after]
  (vec (keep-indexed (fn [i d] (when (pos? d) i)) (mapv - after before))))

(defn people!
  "The store layer `:people` (4.2 step 1): persons are made in it."
  [st]
  (yes! "the :people layer" (c/offer-until-answered! st (c/make-layer-offer c/people-layer {:kind :store}))))

(defn person!
  "Person `p`, made by the operator's act: a layer's owner needs a person
  lock before its making act (client.clj `seed!`)."
  [st p]
  (yes! (str "person " p) (c/make-person! st p)))

(defn grammar-fact
  "A grammar as a fact of the layer it governs (B9): `{:e <key> :k :grammar
  :v g}`, `g` with exactly `:shape`, `:subjects-at`, `:opaque`, `:index`."
  [k g]
  {:e k :k :grammar :v g})

(def note-by-value
  "A' 's grammar: any value, indexed by value (tools' toy `:note`,
  test/rig/store/toy_grammars.clj)."
  {:shape [:any] :subjects-at nil :opaque false :index #{:by-value}})

(def mention-grammar
  "Number 2's `:mention` grammar: an open map whose `:persons`, one to eight
  people, are the value's subjects. Tools' toy `:mention` admits one or two;
  `h40-p5` names four (BUILD_NOTES-numbers.md, binding check, change 4)."
  {:shape [:map {:persons [:set-of [:keyword] 1 8]} {:open? true}]
   :subjects-at [:persons]
   :opaque false
   :index #{}})

(defn write-grammars!
  "The operator's one act writing grammar facts into `layer` ({key grammar}),
  before any fact under those keys. Its answer, a yes."
  [st layer gs]
  (yes! (str "grammars in " layer)
        (c/offer-until-answered!
         st (c/build {:who :operator :layer layer :class :by-layer
                      :facts (vec (for [[k g] gs] (grammar-fact k g)))}))))

(defn layer!
  "A one-owner layer made through the store's own acts (B6): the operator's
  making act (`c/make-layer-offer`, per-value grain), the owner's root
  permission `[owner L L]`; then, optionally, grammar facts, and a session
  opened beneath the root (`mc/open-session!` on `mst`, an `mc/connect`
  handle, B7). Every act must be a yes. The layer spec: {:layer :kind
  :owner :who :root-pid :session :pid :home}, `:pid` the permission its acts
  cite (the session's when one was opened), `:session` the one its door
  leases in, `:home` the depot partition that grew when it was made."
  [st mst {:keys [layer kind owner session grammars tasks]}]
  (let [p0 (partition-ends st tasks)
        _ (yes! (str "making " layer) (c/offer-until-answered! st (c/make-layer-offer layer {:kind kind :owner owner})))
        home (first (grown p0 (partition-ends st tasks)))
        root [owner layer layer]]
    (yes! (str "granting " root) (c/offer-until-answered! st (c/grant-offer st root)))
    (when (seq grammars) (write-grammars! st layer grammars))
    (let [spid (when session
                 (yes! (str "opening " session " in " layer)
                       (get (mc/open-session! mst session owner [layer]) layer))
                 [session layer layer root])]
      {:layer layer :kind kind :owner owner :who owner :root-pid root
       :session (or session (c/default-session owner))
       :pid (or spid root)
       :home home})))

(defn layers-on-task!
  "Variant B's candidate search (4.5): agent layers `<prefix><j>` owned by
  `owner`, made one at a time; kept are those whose making grew partition
  `target` (placement is by layer, `hash-by :layer`). A candidate that
  landed elsewhere stays made and idle. The kept layer ids, in order."
  [st {:keys [prefix owner n target tasks max-candidates] :or {max-candidates 20000}}]
  (loop [j 0 kept []]
    (cond
      (= n (count kept)) kept
      (>= j max-candidates) (throw (ex-info "too few candidates landed on the target task" {:kept (count kept) :n n}))
      :else
      (let [L (keyword (str prefix j))
            p0 (partition-ends st tasks)
            _ (yes! (str "making " L) (c/offer-until-answered! st (c/make-layer-offer L {:kind :agent :owner owner})))
            p1 (partition-ends st tasks)]
        (recur (inc j) (if (= [target] (grown p0 p1)) (conj kept L) kept))))))

;; ======================================================= the writes (2.3)

(def write-rows
  "PLAN-numbers.md 2.3 as data: the path writes (a set by `termval`, a delete
  by `NONE>`) of the gate's decision event for an admitted act, one entry
  per path written, with the field, set or delete, whether it is a read
  index, and its site on the landed tree (`rig-2026-09-25` at 42619066,
  wave 2 at 27543fd7). Rows 1 to 13 are the plan's; `:s` (what an act stood
  on, variant C's value act) and `:k` (a key's row at its first use in the
  layer, F2) are the two the plan lists as outside the windows' usual acts."
  [{:row 1 :write "answer record" :path "$$layers [L :answers name]" :field :answers :kind :set
    :site "gate_event.clj write-decided> L49"}
   {:row 2 :write "the act's rows" :path "[L :log name]" :field :log :kind :set
    :site "gate_event.clj L52"}
   {:row 3 :write "the fact's head" :path "[L :heads [e k fid]]" :field :heads :kind :set
    :site "gate_event.clj L62"}
   {:row 4 :write "id index by entity" :path "[L :ix-ek address]" :field :ix-ek :kind :set :read-index? true
    :site "gate_event.clj L67; reads.clj fact-writes L247"}
   {:row 5 :write "id index by key" :path "[L :ix-ke address]" :field :ix-ke :kind :set :read-index? true
    :site "gate_event.clj L67; reads.clj fact-writes L247"}
   {:row 6 :write "stamp index" :path "[L :ix-s address]" :field :ix-s :kind :set :read-index? true
    :site "gate_event.clj L67; reads.clj fact-writes L247 (stage 5b)"}
   {:row 7 :write "value index" :path "[L :ix-kv address]" :field :ix-kv :kind :set :read-index? true
    :site "gate_event.clj L67; reads.clj fact-writes, a key its grammar indexes by value"}
   {:row 8 :write "value id to its value addresses" :path "[L :ix-of fid]" :field :ix-of :kind :set :read-index? true
    :site "gate_event.clj L70"}
   {:row 9 :write "lock row" :path "[L :locks lock-id]" :field :locks :kind :set
    :site "locks.clj write-decision> L1297 (lock-plan's :row?: personal, hand, or :own-row)"}
   {:row 10 :write "the cited lease row, consumed" :path "[L :leases s lock-id]" :field :leases :kind :delete
    :site "locks.clj consume-locks> L1142, from write-decision>"}
   {:row 11 :write "the lease's rows" :path "[L :leases s lock-id]" :field :leases :kind :set
    :site "locks.clj write-decision> L1297, lease-writes"}
   {:row 12 :write "stamp to name" :path "[L :by-stamp stamp]" :field :by-stamp :kind :set
    :site "locks.clj lock-effects L897, written by write-decision>"}
   {:row 13 :write "the task's last stamp" :path "$$clock" :field :clock :kind :set
    :site "gate_event.clj L93"}
   {:row :s :write "what the act stood on" :path "[L :stood-on name fid]" :field :stood-on :kind :set
    :site "gate_event.clj L56"}
   {:row :k :write "the key's row, at its first use in the layer" :path "[L :key-rows k]" :field :key-rows :kind :set
    :site "gate_event.clj L84-85; grammar.clj key-row-writes"}])

(def act-writes
  "Per act kind, how many path writes of each row (2.3's columns). A lease
  act writes `:n` rows of 11, its lock count (64 by the door)."
  {:agent-value          {1 1, 2 1, 3 1, 4 1, 5 1, 6 1, 10 1, 12 1, 13 1}
   :agent-value-by-value {1 1, 2 1, 3 1, 4 1, 5 1, 6 1, 7 1, 8 1, 10 1, 12 1, 13 1}
   :agent-value-stood-on {1 1, 2 1, 3 1, 4 1, 5 1, 6 1, 10 1, 12 1, 13 1, :s 1}
   :owner-value          {1 1, 2 1, 3 1, 4 1, 5 1, 6 1, 9 1, 10 1, 12 1, 13 1}
   :lease                {1 1, 2 1, 3 1, 4 1, 5 1, 6 1, 11 :n, 12 1, 13 1}
   :entry                {1 1, 2 1, 3 1, 4 1, 5 1, 6 1, 9 1, 10 1, 12 1, 13 1}})

(defn writes
  "The write list of an act of `kind`: each write row with its `:count`
  (a lease act's rows: `lease-n`)."
  ([kind] (writes kind c/lease-size))
  ([kind lease-n]
   (vec (for [{:keys [row] :as w} write-rows
              :let [n (get-in act-writes [kind row])]
              :when n]
          (assoc w :count (if (= :n n) lease-n n))))))

(defn per-act-writes
  "Kind to its totals: every path write, the read indexes' (rows 4 to 8)
  and the deletes."
  ([kind] (per-act-writes kind c/lease-size))
  ([kind lease-n]
   (let [ws (writes kind lease-n)]
     {:total (reduce + (map :count ws))
      :read-indexes (reduce + (map :count (filter :read-index? ws)))
      :deletes (reduce + (map :count (filter #(= :delete (:kind %)) ws)))})))

(def first-use
  "F2: the one write a layer's first fact under a key that is not a store key
  makes (a `:key-rows` row), made in setup or the warm-up, never in a window."
  {:row :k :field :key-rows :count 1})

(defn expected-growth
  "What the write list claims each `$$layers` field grows by, for `counts`
  ({act-kind admitted}), lease acts of `lease-n` locks, and `first-uses`
  key rows written for the first time: a set adds an entry (every set these
  acts make is at a fresh name, fact id, stamp or lock id), a delete takes
  one away; `$$clock` is not a layer field."
  [counts {:keys [lease-n first-uses] :or {lease-n c/lease-size first-uses 0}}]
  (let [g (reduce (fn [acc [act-kind n]]
                    (reduce (fn [acc {:keys [field kind count]}]
                              (if (= :clock field)
                                acc
                                (update acc field (fnil + 0) (* n (if (= :delete kind) (- count) count)))))
                            acc (writes act-kind lease-n)))
                  {} counts)]
    (cond-> g (pos? first-uses) (update :key-rows (fnil + 0) first-uses))))

;; ===================================================== the store's own growth

(def ^:private task-keys-by-count (atom {}))

(defn task-keys
  "A key hashing to each task, so each task's `$$clock` can be read."
  [tasks]
  (or (get @task-keys-by-count tasks)
      (let [ks (vec (rtest/gen-hashing-index-keys tasks))]
        (swap! task-keys-by-count assoc tasks ks)
        ks)))

(defn task-clocks
  "Each task's last stamp."
  [st tasks]
  (mapv #(foreign-select-one STAY (:clock st) {:pkey %}) (task-keys tasks)))

(defn store-fields
  "Every field of a one-owner layer's value in `$$layers`, from the store
  itself (B2): the field functions module.clj's `layers-schema` merges
  (stage 1's, locks', reads', and, on the landed tree, promotion's and
  grammar's), resolved from the code that is loaded."
  []
  (let [extra (for [s '[rig.store.promote/layer-fields rig.store.grammar/layer-fields]
                    :let [f (try (requiring-resolve s) (catch Throwable _ nil))]
                    :when f]
                (keys (f)))]
    (into (sorted-set)
          (concat (keys m/layer-fields) (keys (locks/layer-fields)) (keys (reads/layer-fields m/row-fields))
                  (apply concat extra)))))

(def leaf-fields
  "Fields counted at their leaves (9.2): `:leases` by session then lock id,
  `:stood-on` by act then fact id."
  #{:leases :stood-on})

(def value-fields "Fields that are one value: compared, not counted." #{:settings})

(defn field-counts
  "For layer `L`: every field's entry count, the fields enumerated from the
  store (`store-fields`), nested fields at their leaves, `:log` by act with
  its rows beside, `:settings` as its value; the keys the store itself
  reports for the layer's value (`MAP-KEYS`, when the path answers); and
  `$$clock` per task. For the tests (T1 to T4), never inside a window."
  [st L tasks]
  (let [ps (:layers st)
        cnt (fn [path] (or (foreign-select-one path ps) 0))]
    {:fields (into (sorted-map)
                   (for [f (store-fields)]
                     [f (cond
                          (value-fields f) (foreign-select-one [(keypath L f)] ps)
                          (leaf-fields f) (reduce + 0 (foreign-select [(keypath L f) MAP-VALS (view count)] ps))
                          :else (cnt [(keypath L f) (view count)]))]))
     :log-rows (reduce + 0 (foreign-select [(keypath L :log) MAP-VALS (view count)] ps))
     :present (try (into (sorted-set) (foreign-select [(keypath L) MAP-KEYS] ps))
                   (catch Throwable t (str "MAP-KEYS did not answer: " (.getMessage t))))
     :clocks (task-clocks st tasks)}))

(defn growth
  "Two `field-counts` apart: each counted field's growth, a value field's
  :same or :changed, the log's rows, and the tasks whose clock moved."
  [before after]
  {:fields (into (sorted-map)
                 (for [[f a] (:fields after)
                       :let [b (get-in before [:fields f])]]
                   [f (if (and (number? a) (number? b)) (- a b) (if (= a b) :same :changed))]))
   :log-rows (- (:log-rows after) (:log-rows before))
   :clocks-moved (grown (:clocks before) (:clocks after))})

;; ================================================================ counting

(definterface ILongBuf
  (^void add [^long x])
  (^long size [])
  (^longs toArray []))

(deftype LongBuf [^:unsynchronized-mutable ^longs arr ^:unsynchronized-mutable ^long n]
  ILongBuf
  (add [_ x]
    (when (== n (alength arr))
      (set! arr (Arrays/copyOf arr (int (* 2 (alength arr))))))
    (aset arr (int n) x)
    (set! n (unchecked-inc n)))
  (size [_] n)
  (toArray [_] (Arrays/copyOf arr (int n))))

(defn long-buf
  "A growable primitive long array, one writer's own (9.2)."
  ^LongBuf []
  (LongBuf. (long-array 1024) 0))

(defn sorted-ns
  "The latencies held by `bufs`, as one sorted long array."
  ^longs [bufs]
  (let [total (reduce + 0 (map #(.size ^LongBuf %) bufs))
        out (long-array total)]
    (loop [bs (seq bufs) at 0]
      (if bs
        (let [^longs a (.toArray ^LongBuf (first bs))]
          (System/arraycopy a 0 out (int at) (alength a))
          (recur (next bs) (+ at (alength a))))
        (do (Arrays/sort out) out)))))

(defn ms "Nanoseconds as milliseconds, to the microsecond." [ns]
  (when ns (/ (Math/round (/ (double ns) 1e3)) 1e3)))

(defn percentiles
  "Nearest rank over sorted ns latencies: count, p50, p95, p99, max and mean,
  in ms; nil when there are none."
  [^longs sorted]
  (let [n (alength sorted)]
    (when (pos? n)
      (let [at (fn [q] (aget sorted (int (max 0 (dec (long (Math/ceil (* q n))))))))]
        {:n n
         :p50 (ms (at 0.50)) :p95 (ms (at 0.95)) :p99 (ms (at 0.99))
         :max (ms (aget sorted (int (dec n))))
         :mean (ms (/ (areduce sorted i s 0.0 (+ s (aget sorted i))) n))}))))

(defn rate
  "Events a second over `elapsed-ns`, to one decimal."
  [n elapsed-ns]
  (if (pos? elapsed-ns)
    (/ (Math/round (* 10.0 (/ (double n) (/ (double elapsed-ns) 1e9)))) 10.0)
    0.0))

(defn new-stats
  "A writer's own tally, merged only after its window (9.2): per kind its
  latencies (a `LongBuf`) and its outcomes, the first error, and what its
  steps sampled; `shared` holds the window's two progress adders."
  [shared]
  {:lat (HashMap.) :outcomes (HashMap.) :first-error (volatile! nil) :sample (ArrayList.)
   :acts (:acts shared) :errors (:errors shared)})

(defn- buf-of ^LongBuf [stats kind]
  (let [^HashMap m (:lat stats)]
    (or (.get m kind) (let [b (long-buf)] (.put m kind b) b))))

(defn record-lat!
  "A latency of `kind` in ns, with no outcome (a send's lateness, say)."
  [stats kind ns]
  (.add (buf-of stats kind) (long ns)))

(defn outcome-key
  "`:yes`, `:error` for a throwable, else the refusal's reason."
  [outcome]
  (cond
    (instance? Throwable outcome) :error
    (= :yes (:answer outcome)) :yes
    :else (or (:reason outcome) :no)))

(defn record!
  "One outcome of `kind` (`:value`, `:entry`, `:read`, ...) and its latency
  in ns. `outcome` is the gate's answer, a map with `:answer`, or a
  throwable. Returns the outcome's key."
  [stats kind outcome lat-ns]
  (record-lat! stats kind lat-ns)
  (let [^HashMap om (:outcomes stats)
        ^HashMap m (or (.get om kind) (let [m (HashMap.)] (.put om kind m) m))
        k (outcome-key outcome)]
    (.put m k (inc (long (or (.get m k) 0))))
    (case k
      :error (do (.increment ^LongAdder (:errors stats))
                 (when (nil? @(:first-error stats)) (vreset! (:first-error stats) (str outcome))))
      :yes (.increment ^LongAdder (:acts stats))
      nil)
    k))

(defn yes-count
  "How many outcomes of `kind` this writer had admitted so far."
  ^long [stats kind]
  (let [^HashMap m (.get ^HashMap (:outcomes stats) kind)]
    (long (or (some-> m (.get :yes)) 0))))

(defn sample!
  "Keep `x` for the read-back after the window."
  [stats x]
  (.add ^ArrayList (:sample stats) x))

(defn merge-stats
  "The writers' tallies as one: per kind the sorted latencies and the
  outcome counts; the first error; everything sampled."
  [ss]
  (let [kinds (distinct (mapcat #(keys (:lat %)) ss))
        okinds (distinct (mapcat #(keys (:outcomes %)) ss))]
    {:lat (into {} (for [k kinds] [k (sorted-ns (keep #(.get ^HashMap (:lat %) k) ss))]))
     :outcomes (into {} (for [k okinds]
                          [k (apply merge-with + {} (keep #(some->> (.get ^HashMap (:outcomes %) k) (into {})) ss))]))
     :first-error (some #(deref (:first-error %)) ss)
     :sample (vec (mapcat #(vec (:sample %)) ss))}))

;; =============================================================== snapshots

(defn gc-totals
  "[collections milliseconds] over every collector so far."
  []
  (reduce (fn [[n t] ^GarbageCollectorMXBean b]
            [(+ n (max 0 (.getCollectionCount b))) (+ t (max 0 (.getCollectionTime b)))])
          [0 0] (ManagementFactory/getGarbageCollectorMXBeans)))

(defn thread-cpu
  "Thread id -> [name cpu-ns], every live thread."
  []
  (let [^ThreadMXBean tb (ManagementFactory/getThreadMXBean)]
    (into {} (for [id (.getAllThreadIds tb)
                   :let [info (.getThreadInfo tb (long id))
                         cpu (.getThreadCpuTime tb (long id))]
                   :when (and info (pos? cpu))]
               [id [(.getThreadName info) cpu]]))))

(defn busiest
  "The `n` threads that used the most CPU between two `thread-cpu`
  snapshots, as [name percent-of-one-core]."
  [cpu0 cpu1 elapsed-ns n]
  (->> cpu1
       (map (fn [[id [nm c1]]] [nm (- c1 (second (get cpu0 id [nm 0])))]))
       (sort-by second >)
       (take n)
       (mapv (fn [[nm d]] [nm (/ (Math/round (* 1000.0 (/ (double d) elapsed-ns))) 10.0)]))))

(defn store-busiest
  "The busiest thread that is not the harness's own (the harness names its
  threads `phase7-*`): in a window on one layer, the home task's thread (the
  slices found it the busiest in every window)."
  [busy]
  (first (remove #(str/starts-with? (first %) "phase7-") busy)))

(defn known-leases
  "The leases the given doors came to know (B5: each door's `:known` lease
  names for [layer session]), summed; `lease-doors` is [[store layer
  session] ...]."
  [lease-doors]
  (reduce + 0 (for [[st layer session] lease-doors]
                (count (get-in @(:door st) [:known [layer session]])))))

(defn- worker-pool
  "`n` daemon threads named `phase7-<role>-<i>`."
  ^ExecutorService [n role]
  (let [ctr (AtomicLong.)]
    (Executors/newFixedThreadPool
     (int n)
     (reify java.util.concurrent.ThreadFactory
       (newThread [_ r]
         (doto (Thread. ^Runnable r (str "phase7-" role "-" (.getAndIncrement ctr)))
           (.setDaemon true)))))))

(defn- start-progress!
  "A PROGRESS line every 5 s while the writers run (8.3). Returns its stop
  flag."
  [{:keys [ctx variant k]} ^LongAdder acts ^LongAdder errors t0]
  (let [stop (volatile! false)
        t (Thread. ^Runnable
                   (fn []
                     (loop []
                       (Thread/sleep 5000)
                       (when-not @stop
                         (let [el (/ (- (System/nanoTime) t0) 1e9)
                               n (.sum acts)]
                           (progress! {:number (:number ctx) :run (:run ctx) :variant variant :k k
                                       :t (Math/round el) :acts n :rate (Math/round (/ n (max 1e-9 el)))
                                       :errors (.sum errors)}))
                         (recur))))
                   "phase7-progress")]
    (.setDaemon t true)
    (.start t)
    stop))

(defn- edges
  "What a window records at each edge (never between them): partitions,
  task clocks, the doors' leases, GC, every thread's CPU, the wall."
  [{:keys [st tasks lease-doors]}]
  (cond-> {:gc (gc-totals) :cpu (thread-cpu) :wall (System/currentTimeMillis)}
    st (assoc :parts (partition-ends st tasks) :clocks (task-clocks st tasks)
              :leases (known-leases lease-doors))))

(defn- edge-summary
  "Placement (4.8: only the home partition grew, by exactly the value, entry
  and lease offers sent; only the home task's clock moved), the two lease
  counts (2.5), the home clock's lead over the wall, CPU, GC and overlap,
  between two `edges`. `offers` is the value and entry offers sent."
  [{:keys [st home ctx]} e0 e1 elapsed offers]
  (merge
   {:cpu (let [b (busiest (:cpu e0) (:cpu e1) elapsed 6)] {:busiest b :store-busiest (store-busiest b)})
    :gc {:count (- (first (:gc e1)) (first (:gc e0))) :ms (- (second (:gc e1)) (second (:gc e0)))}
    :overlap (overlap-during (:monitor ctx) (:wall e0) (:wall e1))
    :wall-ms [(:wall e0) (:wall e1)]}
   (when st
     (let [pd (mapv - (:parts e1) (:parts e0))
           cd (mapv - (:clocks e1) (:clocks e0))
           door (- (:leases e1) (:leases e0))
           rama (when home (- (get pd home) offers))]
       {:leases {:door door :rama rama :agree? (= door rama)}
        :placement {:partition-deltas pd :clock-deltas cd :home home
                    :one-task? (boolean (and home
                                             (= [home] (grown (:parts e0) (:parts e1)))
                                             (= [home] (grown (:clocks e0) (:clocks e1)))
                                             (= (get pd home) (+ offers door))))}
        :clock-lead-ms (when home (- (hlc/ms-of (get (:clocks e1) home)) (:wall e1)))}))))

(defn- counts-of
  "Per kind: sent (every outcome but an error, which may not have reached the
  depot) and admitted."
  [outcomes kind]
  {:sent (reduce + 0 (vals (dissoc (get outcomes kind) :error)))
   :admitted (get-in outcomes [kind :yes] 0)})

;; ================================================================= windows

(defn closed-window
  "K writers in closed loops with no pause (4.3, 5.4): each a thread calling
  `(step w i stats)` until `secs` pass, or until it has made `n` iterations;
  a step times and records its own outcomes (`record!`, `sample!`). At the
  window's edges only: placement, the doors' leases, CPU, GC, the clock's
  lead, overlap. The window ends at the last writer's last answer; its
  rates are over that time. Latencies here are closed-loop service times
  (F5): reported, never judged. `w`: {:ctx :st :tasks :home :lease-doors
  :variant :k :secs :n :step}."
  [{:keys [ctx variant k secs n step] :as w}]
  (let [ex (worker-pool k "writer")
        shared {:acts (LongAdder.) :errors (LongAdder.)}
        stats (vec (repeatedly k #(new-stats shared)))
        go (CountDownLatch. 1)
        deadline (volatile! Long/MAX_VALUE)
        stop? (if n (fn [i] (>= i n)) (fn [_] (>= (System/nanoTime) @deadline)))
        e0 (edges w)
        futs (mapv (fn [wi]
                     (.submit ex ^Callable
                              (fn []
                                (.await go)
                                (let [s (nth stats wi)]
                                  (loop [i 0]
                                    (if (stop? i) i (do (step wi i s) (recur (inc i)))))))))
                   (range k))
        t0 (System/nanoTime)
        _ (when secs (vreset! deadline (+ t0 (long (* 1e9 secs)))))
        prog (start-progress! w (:acts shared) (:errors shared) t0)
        _ (progress! {:number (:number ctx) :run (:run ctx) :variant variant :k k :t 0 :acts 0 :rate 0 :errors 0})
        _ (.countDown go)
        iterations (mapv #(.get ^Future %) futs)
        t1 (System/nanoTime)
        _ (vreset! prog true)
        e1 (edges w)
        _ (.shutdown ex)
        merged (merge-stats stats)
        elapsed (- t1 t0)
        oc (:outcomes merged)
        v (counts-of oc :value)
        e (counts-of oc :entry)
        s (merge
           {:variant variant :k k :loop :closed
            :secs (/ (Math/round (/ elapsed 1e6)) 1e3)
            :iterations (reduce + iterations)
            :iterations-per-writer [(reduce min iterations) (reduce max iterations)]
            :sent {:value (:sent v) :entry (:sent e)}
            :admitted {:value (:admitted v) :entry (:admitted e)}
            :outcomes oc
            :errors (reduce + 0 (keep :error (vals oc)))
            :first-error (:first-error merged)
            :value-acts-per-s (rate (:admitted v) elapsed)
            :iterations-per-s (rate (reduce + iterations) elapsed)
            :lat-kind :closed-loop-service-time
            :lat (into (sorted-map) (for [[kind a] (:lat merged)] [kind (percentiles a)]))}
           (edge-summary w e0 e1 elapsed (+ (:sent v) (:sent e))))
        s (assoc s :all-acts-per-s (rate (+ (:admitted v) (:admitted e) (or (get-in s [:leases :door]) 0)) elapsed))]
    (progress! {:number (:number ctx) :run (:run ctx) :variant variant :k k :t (Math/round (/ elapsed 1e9))
                :acts (:admitted v) :rate (Math/round (double (:value-acts-per-s s))) :errors (:errors s)})
    (assoc s :sample (:sample merged) :sorted-lat (:lat merged))))

(defn- park-until
  "Wait until `System/nanoTime` reaches `t`."
  [^long t]
  (loop []
    (let [d (- t (System/nanoTime))]
      (when (pos? d)
        (LockSupport/parkNanos d)
        (recur)))))

(defn open-window
  "Open arrival (4.5; 5.4b, F5): each lane is a fixed schedule t(n) = t0 +
  phase + n × period, served by its own sender threads, each taking the next
  slot, so a late answer delays no other slot; a send that falls behind its
  slot goes at once, and its latency runs from the slot, not from when it
  was sent, so a queue shows as latency instead of the writers quietly
  slowing down. The first `warm` seconds are unmeasured; the slots scheduled
  in the `secs` after them are measured. Each lane: {:threads :period-ns
  :phase-ns :send (fn [slot] outcome) :kind}; an admitted outcome carrying
  `:sample-item` is kept for the read-back, one in 50. Placement, leases,
  CPU and GC cover the whole window. `:trace? true` keeps every slot's [lane slot
  scheduled started ended] (ns), for T5."
  [{:keys [ctx variant k lanes warm secs trace?] :as w}]
  (let [threads (vec (for [[li lane] (map-indexed vector lanes) _ (range (:threads lane))] li))
        ex (worker-pool (count threads) "sender")
        shared {:acts (LongAdder.) :errors (LongAdder.)}
        m-stats (vec (repeatedly (count threads) #(new-stats shared)))
        w-stats (vec (repeatedly (count threads) #(new-stats shared)))
        slots (mapv (fn [_] (AtomicLong.)) lanes)
        trace (when trace? (java.util.concurrent.ConcurrentLinkedQueue.))
        go (CountDownLatch. 1)
        t0-box (volatile! 0)
        warm-ns (long (* 1e9 (or warm 0)))
        secs-ns (long (* 1e9 secs))
        e0 (edges w)
        futs (mapv
              (fn [ti]
                (let [li (nth threads ti)
                      {:keys [period-ns phase-ns send kind] :or {phase-ns 0 kind :value}} (nth lanes li)
                      ^AtomicLong next-slot (nth slots li)
                      period (long period-ns)
                      phase (long phase-ns)]
                  (.submit ex ^Callable
                           (fn []
                             (.await go)
                             (let [t0 (long @t0-box)
                                   mstart (+ t0 warm-ns)
                                   end (+ mstart secs-ns)]
                               (loop [sent 0]
                                 (let [slot (.getAndIncrement next-slot)
                                       ts (+ t0 phase (* slot period))]
                                   (if (>= ts end)
                                     sent
                                     (do (park-until ts)
                                         (let [started (System/nanoTime)
                                               o (try (send slot) (catch Exception e e))
                                               ended (System/nanoTime)
                                               s (if (>= ts mstart) (nth m-stats ti) (nth w-stats ti))
                                               ok (record! s kind o (- ended ts))]
                                           (record-lat! s :lateness (- started ts))
                                           ;; a lane's send may attach what the read-back needs
                                           (when (and (= :yes ok) (:sample-item o)
                                                      (zero? (mod (yes-count s kind) 50)))
                                             (sample! s (:sample-item o)))
                                           (when trace (.add trace [li slot ts started ended]))
                                           (recur (inc sent))))))))))))
              (range (count threads)))
        t0 (System/nanoTime)
        _ (vreset! t0-box t0)
        prog (start-progress! w (:acts shared) (:errors shared) t0)
        _ (.countDown go)
        _ (mapv #(.get ^Future %) futs)
        t1 (System/nanoTime)
        _ (vreset! prog true)
        e1 (edges w)
        _ (.shutdown ex)
        mm (merge-stats m-stats)
        wm (merge-stats w-stats)
        moc (:outcomes mm)
        v (counts-of moc :value)
        all-sent (+ (:sent v) (:sent (counts-of (:outcomes wm) :value))
                    (:sent (counts-of moc :entry)) (:sent (counts-of (:outcomes wm) :entry)))
        offered (reduce + 0 (vals (get moc :value)))
        s (merge
           {:variant variant :k k :loop :open
            :schedule {:lanes (count lanes) :threads (count threads)
                       :periods-ns (vec (distinct (map :period-ns lanes)))}
            :warm warm :secs secs
            :offered offered :admitted {:value (:admitted v)}
            :whole-window {:sent all-sent
                           :admitted (+ (:admitted v) (:admitted (counts-of (:outcomes wm) :value)))}
            :offered-per-s (rate offered secs-ns)
            :value-acts-per-s (rate (:admitted v) secs-ns)
            :admitted-share (when (pos? offered) (/ (Math/round (* 1000.0 (/ (:admitted v) offered))) 1000.0))
            :outcomes moc
            :errors (+ (reduce + 0 (keep :error (vals moc))) (reduce + 0 (keep :error (vals (:outcomes wm)))))
            :first-error (or (:first-error mm) (:first-error wm))
            :lat-kind :from-schedule
            :lat (into (sorted-map) (for [[kind a] (:lat mm)] [kind (percentiles a)]))
            :whole-secs (/ (Math/round (/ (- t1 t0) 1e6)) 1e3)}
           (edge-summary w e0 e1 (- t1 t0) all-sent))]
    (progress! {:number (:number ctx) :run (:run ctx) :variant variant :k k :t (Math/round (/ (- t1 t0) 1e9))
                :acts (:admitted v) :rate (Math/round (double (:value-acts-per-s s))) :errors (:errors s)})
    (cond-> (assoc s :sample (into (:sample mm) (:sample wm)) :sorted-lat (:lat mm))
      trace (assoc :trace (vec trace)))))

(defn idle-window
  "3.2: `secs` with no offers, every thread's CPU recorded, so whatever the
  idle micro topology costs the task threads is on record."
  [secs]
  (let [c0 (thread-cpu) g0 (gc-totals) t0 (System/nanoTime)]
    (Thread/sleep (long (* 1000 secs)))
    (let [t1 (System/nanoTime) g1 (gc-totals)]
      {:variant :idle :secs secs
       :busiest (busiest c0 (thread-cpu) (- t1 t0) 12)
       :gc {:count (- (first g1) (first g0)) :ms (- (second g1) (second g0))}})))

;; ============================================================ the read-back

(defn- at-address [ps L field addr] (foreign-select-one [(keypath L field addr)] ps))

(def value-parts "An id-index entry's value fields (reads.clj `value-fields`)." [:v :sealed :lock :digest])

(defn check-act
  "Every write of one sampled admitted act, read back (2.3, 4.3): its answer
  record says yes with its stamp; its log holds one row with the sealed
  bytes and the lock id, and the lock record inside the row in an agent
  layer (none there in a personal or hand layer, or for an entry); its head
  at that stamp; its `:ix-ek`, `:ix-ke` and `:ix-s` entries at their
  addresses (an entry's without value fields, `:no-copy`); for a key its
  grammar indexes by value, the `:ix-kv` entry and the `:ix-of` set; the
  cited lease row gone; the stamp-to-name entry; the lock row, or none; what
  it stood on. `item`: {:layer :session :name :kind :stamp :e :k :v
  :stood-on}, `:e` and `:k` read from the row when absent. Check name ->
  passed?."
  [st {:keys [layer session name kind stamp v stood-on] :as item}]
  (let [ps (:layers st)
        rec (c/record st name)
        rows (vec (c/raw-rows st layer name))
        row (first rows)
        e (or (:e item) (:e row))
        k (or (:k item) (:k row))
        fid [name 0]
        lid (:lock-id row)
        row-lock? (contains? #{:owner-value :entry} kind)
        id-entry (fn [field] (at-address ps layer field (reads/address field {:e e :k k :stamp stamp :fid fid})))
        ids {:ix-ek (id-entry :ix-ek) :ix-ke (id-entry :ix-ke) :ix-s (id-entry :ix-s)}
        lock-row (when lid (c/lock-row st layer lid))]
    (cond->
     {:answer (and (= :yes (:answer rec)) (= stamp (:stamp rec)))
      :rows (and (= 1 (count rows)) (bytes? (:sealed row)) (vector? lid)
                 (or (nil? v) (not (Arrays/equals ^bytes (:sealed row) ^bytes (locks/canonical-bytes v)))))
      :lock-in-record (if (or row-lock? (nil? row)) (nil? (:lock row)) (map? (:lock row)))
      :head (= stamp (c/head st layer e k fid))
      :ix-ek (= fid (:fid (:ix-ek ids)))
      :ix-ke (= fid (:fid (:ix-ke ids)))
      :ix-s (= fid (:fid (:ix-s ids)))
      :lease-consumed (and (some? lid) (nil? (foreign-select-one [(keypath layer :leases session lid)] ps)))
      :by-stamp (= name (foreign-select-one [(keypath layer :by-stamp stamp)] ps))
      :lock-row (if row-lock? (and (map? lock-row) (some? (:scheme lock-row))) (nil? lock-row))}
      (= :entry kind)
      (assoc :no-copy (every? (fn [ie] (and (false? (:copy ie)) (every? #(nil? (get ie %)) value-parts))) (vals ids)))
      (= :agent-value-by-value kind)
      (merge (let [a (reads/address :ix-kv {:k k :vtext (env/encode-value v) :stamp stamp :fid fid})]
               {:ix-kv (= fid (:fid (at-address ps layer :ix-kv a)))
                :ix-of (= #{a} (foreign-select-one [(keypath layer :ix-of fid)] ps))}))
      (some? stood-on)
      (assoc :stood-on (= stood-on (c/stood-on st layer name))))))

(defn check-sample
  "4.3's read-back after a window, never during it: up to 200 of the acts
  the steps sampled (one in 50 as answered), each through `check-act`.
  {:checked :ok :failed {check n} :first-failure {...}}; complete when
  `:ok` = `:checked`."
  [st sample]
  (let [picked (take 200 (shuffle sample))
        results (mapv (fn [item] [item (check-act st item)]) picked)
        bad (filter (fn [[_ r]] (some false? (vals r))) results)]
    {:checked (count results)
     :ok (- (count results) (count bad))
     :failed (frequencies (for [[_ r] bad [ck ok?] r :when (false? ok?)] ck))
     :first-failure (when-let [[item r] (first bad)]
                      {:item (dissoc item :v) :checks r})}))

;; ================================================================ verdicts

(defn verdict
  "7.2's rule for a timing, kept from the slices: `value` against
  `threshold`, `better` `:higher` (at least) or `:lower` (at most). Ten
  times or more on one side decides (far); within ten times the in-process
  cluster cannot decide (near). Each level is judged on its own; the call
  names the side and the factor."
  [value threshold better]
  (when (number? value)
    (let [v (double value)
          r (if (= :higher better) (/ v threshold) (if (zero? v) Double/POSITIVE_INFINITY (/ (double threshold) v)))
          meets? (>= r 1.0)
          factor (if meets? r (if (zero? r) Double/POSITIVE_INFINITY (/ 1.0 r)))
          far? (>= factor 10.0)]
      {:value value :threshold threshold :better better :meets? meets?
       :factor (if (Double/isInfinite factor) :infinite (/ (Math/round (* 100.0 factor)) 100.0))
       :call (cond (and meets? far?) :far-passes
                   meets? :near-passes
                   far? :far-fails
                   :else :near-fails)
       :decides? far?})))

(defn bytes-verdict
  "7.3's rule for bytes, judged directly: at most twice the value's bytes is
  fine; above four times, change the default; between, neither."
  [ratio]
  (when (number? ratio)
    {:ratio (/ (Math/round (* 100.0 ratio)) 100.0)
     :call (cond (<= ratio 2.0) :fine (> ratio 4.0) :over-four-times-change-the-default :else :between-two-and-four)}))

(defn result
  "A window's map as a RESULT line carries it: without what is only for the
  read-back or the tests (the sample, the sorted latencies, a trace)."
  [w]
  (dissoc w :sample :sorted-lat :trace))

;; ================================================================== report

(def slices
  "The slices' numbers of 25 September, beside the final ones (section 10;
  runs/phase7-agent-rate.txt runs 4 to 6, runs/phase7-one-thread.txt runs 4
  to 6, runs/phase7-lock-growth.txt), for the comparison."
  {:agent-rate {1 {:acts-per-s 299 :index-writes-per-s 1196 :p99 4.74 :thread 20}
                4 {:acts-per-s 646 :index-writes-per-s 2584 :p99 9.79 :thread 24}
                16 {:acts-per-s 2249 :index-writes-per-s 8996 :p99 9.94 :thread 37}
                :writes-per-act 4}
   :one-thread {:sequential {:acts-per-s 299 :p50 3.26 :p99 4.72 :max 20.9}
                :concurrent "K = 1 to 128: 300 to 5,192 acts a second, p99 4.71 to 43.94 ms, the thread never above 60%"}
   :lock-growth {:raw-one-person {:logical 169 :compacted 86.9}
                 :base64-two-persons {:logical 233 :compacted 148.0}
                 :extra-person-raw "32 to 35 B, computed"
                 :ratios {40 {:logical 4.23 :compacted 2.17} 200 {:logical 0.85 :compacted 0.43}}}})

(defn- read-lines
  "Every tagged map a number's `.edn` holds, in order."
  [number]
  (let [f (io/file (out-file number))]
    (if (.exists f)
      (->> (str/split-lines (slurp f)) (remove str/blank?) (mapv edn/read-string))
      [])))

(defn complete-runs
  "The runs whose RUN-END says ok (15: a killed run's lines are left out):
  [{:meta :results :end}], in order."
  [lines]
  (let [by-uid (group-by :uid lines)]
    (vec (for [[uid ls] by-uid
               :let [end (first (filter #(= "RUN-END" (:tag %)) ls))]
               :when (:ok? end)]
           {:uid uid
            :meta (first (filter #(= "META" (:tag %)) ls))
            :results (filterv #(= "RESULT" (:tag %)) ls)
            :end end}))))

(defn med-spread
  "Median and spread (minimum to maximum) of the numbers in `xs`."
  [xs]
  (let [s (vec (sort (filter number? xs)))
        n (count s)]
    (when (pos? n)
      {:median (s (quot n 2)) :min (first s) :max (peek s) :runs n})))

(defn- fmt
  "A number for a table: whole above 100, else two decimals; '-' for none."
  [x]
  (cond (nil? x) "-"
        (not (number? x)) (str x)
        (>= (Math/abs (double x)) 100) (format "%,d" (Math/round (double x)))
        :else (format "%.2f" (double x))))

(defn- ms-cell [{:keys [median min max]}]
  (if median (format "%s (%s to %s)" (fmt median) (fmt min) (fmt max)) "-"))

(defn- clean-share
  "8.4: when at least two of three runs are clean, the clean ones; else all,
  marked."
  [rs]
  (let [clean (remove :overlap rs)]
    (if (>= (count clean) (min 2 (count rs))) {:rs (vec clean) :marked? false} {:rs (vec rs) :marked? true})))

(defn- by-level
  "A number's results of `variant` across runs, grouped by K."
  [runs variant]
  (into (sorted-map) (group-by :k (for [r runs x (:results r) :when (= variant (:variant x))] x))))

(defn- machine-lines [runs]
  (let [ms (distinct (map #(dissoc (get-in % [:meta :machine]) :at :load :ran :git :cluster-dir) runs))
        m (get-in (first runs) [:meta :machine])]
    (concat
     [(str "Machine (recorded by the harness at every run; " (count runs) " complete runs"
           (if (= 1 (count ms)) ", every run on the same machine" ", MACHINES DIFFER across runs: see the .edn") "):")
      (str "  " (:cpu m) ", " (:cores m) " cores, " (:hardware-threads m) " hardware threads, " (:ram m) " RAM")
      (str "  " (:os m) ", kernel " (:kernel m) "; " (get-in m [:jdk :vm]) " " (get-in m [:jdk :runtime]) " (" (get-in m [:jdk :vendor]) ")")
      (str "  JVM flags " (str/join " " (:jvm-flags m)) "; GC " (str/join ", " (:gc m)))
      (str "  " (:rama m) ", " (:rama-helpers m) ", Clojure " (:clojure m))
      (str "  the cluster's files on " (get-in m [:disk :filesystem]) " on " (get-in m [:disk :device]) " ("
           (get-in m [:disk :disk-model]) ")")
      (str "  load at each run's start: " (str/join "; " (map #(str/join " " (get-in % [:meta :machine :load])) runs)))
      (str "  code: " (str/join ", " (distinct (map #(str (let [h (str (get-in % [:meta :machine :git :head]))] (subs h 0 (min 8 (count h))))
                                                          (when-not (get-in % [:meta :machine :git :code-clean?]) " (not clean)"))
                                                    runs))))])))

(defn- write-list-lines []
  (concat
   ["The writes of the gate's decision event, per admitted act (2.3), named from the code; the harness's claim, which T1 to T4 hold to the store's own growth:"]
   (for [{:keys [row write path kind site]} write-rows]
     (format "  %-3s %-44s %-26s %-6s %s" row write path (name kind) site))
   [(str "  per act: " (str/join "; " (for [k [:agent-value :agent-value-by-value :owner-value :lease :entry :agent-value-stood-on]]
                                        (let [p (per-act-writes k)] (format "%s %d (read indexes %d, deletes %d)" (name k) (:total p) (:read-indexes p) (:deletes p))))))
    "  and once per layer, the first fact under a key that is not a store key: one :key-rows row (F2), in setup or warm-up, never in a window"]))

(defn- header [title runs extra]
  (concat [title "" (str "Thresholds: " thresholds-line) (str "Caveat: " caveat) ""]
          (machine-lines runs) [""] extra [""] (write-list-lines) [""]))

(defn- verdict-str [v]
  (if v (format "%s, %s times" (name (:call v)) (fmt (:factor v))) "-"))

(defn- checks-str [rs]
  (str/join "; " (for [[label f] [["read-back complete" #(let [c (:check %)] (and c (= (:ok c) (:checked c))))]
                                  ["one task" #(get-in % [:placement :one-task?])]
                                  ["lease counts agree" #(get-in % [:leases :agree?])]
                                  ["no error" #(zero? (or (:errors %) 0))]
                                  ["no other JVM" #(nil? (:overlap %))]]]
                           (format "%s %d/%d" label (count (filter f rs)) (count rs)))))

(defn- agent-report [runs]
  (let [rruns (filter #(= "run" (get-in % [:end :fn])) runs)
        sruns (filter #(= "sessions" (get-in % [:end :fn])) runs)
        cruns (filter #(= "reads" (get-in % [:end :fn])) runs)
        level-lines (fn [variant rs-by-k judge?]
                      (for [[k rs0] rs-by-k
                            :let [{:keys [rs marked?]} (clean-share rs0)
                                  acts (med-spread (map :value-acts-per-s rs))
                                  iw (med-spread (map #(get-in % [:index-writes :per-s :all]) rs))
                                  ri (med-spread (map #(get-in % [:index-writes :per-s :read-indexes]) rs))
                                  p50 (med-spread (map #(get-in % [:lat :value :p50]) rs))
                                  p99 (med-spread (map #(get-in % [:lat :value :p99]) rs))
                                  mx (med-spread (map #(get-in % [:lat :value :max]) rs))
                                  cpu (med-spread (map #(second (get-in % [:cpu :store-busiest])) rs))
                                  lead (med-spread (map :clock-lead-ms rs))
                                  sl (get-in slices [:agent-rate k])]]
                        (str (format "  %s K=%-4s value acts/s %-26s index writes/s %-28s read indexes/s %-10s p50 %-7s p99 %-7s max %-7s home thread %s%%  clock lead %s ms%s"
                                     (name variant) k (ms-cell acts) (ms-cell iw) (fmt (:median ri)) (fmt (:median p50)) (fmt (:median p99))
                                     (fmt (:median mx)) (fmt (:median cpu)) (fmt (:median lead)) (if marked? "  [runs marked :overlap]" ""))
                             (when judge? (str "\n      verdict (at least 1,000 value acts a second): " (verdict-str (verdict (:median acts) (:agent-acts-per-s thresholds) :higher))))
                             (when (and judge? sl) (format "\n      slice: %s acts/s, %s index writes/s at %d an act, p99 %s ms, thread %s%%; final/slice %s"
                                                           (:acts-per-s sl) (:index-writes-per-s sl) (get-in slices [:agent-rate :writes-per-act])
                                                           (:p99 sl) (:thread sl) (fmt (when (:median acts) (/ (:median acts) (:acts-per-s sl))))))
                             (str "\n      checks: " (checks-str rs0)))))]
    (concat
     ["Variant A: one agent session layer, K writers sharing one door, closed loops (latencies are closed-loop service times, reported, not judged; F5)."]
     (level-lines :A (by-level rruns :A) true)
     ["" "Variant A': the same act under a by-value :note grammar (11 writes an act)."]
     (level-lines :A-prime (by-level rruns :A-prime) false)
     ["" "D2: A's act citing the owner's root permission (a chain of 1), beside A at K = 16."]
     (level-lines :D2 (by-level rruns :D2) false)
     ["" "D1: the pool stocked ahead (only when A's rate flattened while the home thread stayed under 60%); valid only if the door leased nothing in the window."]
     (for [r rruns x (:results r) :when (= :D1 (:variant x))]
       (format "  run %s: value acts/s %s, valid %s%s, stocking %s" (:run x) (fmt (:value-acts-per-s x)) (:d1-valid? x)
               (if (:d1-mark x) (str " (" (name (:d1-mark x)) ")") "") (pr-str (:stocking x))))
     ["" "Variant B: S sessions on one task, each at an assumed 100 acts a second, open schedule, latency from the schedule."]
     (for [[k rs] (by-level sruns :B)]
       (format "  S=%-4s offered/s %-8s admitted/s %-26s admitted %s  p50 %s p99 %-26s max %s  lease acts/s %s  index writes/s %s  home thread %s%%  checks: %s"
               k (fmt (:median (med-spread (map :offered-per-s rs)))) (ms-cell (med-spread (map :value-acts-per-s rs)))
               (fmt (:median (med-spread (map :admitted-share rs)))) (fmt (:median (med-spread (map #(get-in % [:lat :value :p50]) rs))))
               (ms-cell (med-spread (map #(get-in % [:lat :value :p99]) rs))) (fmt (:median (med-spread (map #(get-in % [:lat :value :max]) rs))))
               (fmt (:median (med-spread (map :lease-acts-per-s rs)))) (fmt (:median (med-spread (map #(get-in % [:index-writes :per-s :all]) rs))))
               (fmt (:median (med-spread (map #(second (get-in % [:cpu :store-busiest])) rs)))) (checks-str rs)))
     (for [r sruns x (:results r) :when (= :B-summary (:variant x))]
       (format "  run %s: the largest S at 95%% admitted and p99 at most 20 ms: %s sessions, %s acts a second on one task (agent speed assumed); against 1,000: %s"
               (:run x) (:largest-s x) (some-> (:largest-s x) (* 100)) (verdict-str (some-> (:largest-s x) (* 100) (verdict (:agent-acts-per-s thresholds) :higher)))))
     ["" "Variant C: read (through the exit, recorded) then write standing on what was read; closed loops."]
     (for [[k rs] (by-level cruns :C)]
       (format "  K=%-3s iterations/s %-26s value/entry/lease acts/s %s/%s/%s  index writes/s %s  entry lock rows %s  read p50/p99/max %s/%s/%s  write p50/p99/max %s/%s/%s  checks: %s"
               k (ms-cell (med-spread (map :iterations-per-s rs)))
               (fmt (:median (med-spread (map #(get-in % [:acts-per-s :value]) rs)))) (fmt (:median (med-spread (map #(get-in % [:acts-per-s :entry]) rs))))
               (fmt (:median (med-spread (map #(get-in % [:acts-per-s :lease]) rs)))) (fmt (:median (med-spread (map #(get-in % [:index-writes :per-s :all]) rs))))
               (fmt (:median (med-spread (map :entry-lock-rows rs))))
               (fmt (:median (med-spread (map #(get-in % [:lat :read :p50]) rs)))) (fmt (:median (med-spread (map #(get-in % [:lat :read :p99]) rs))))
               (fmt (:median (med-spread (map #(get-in % [:lat :read :max]) rs))))
               (fmt (:median (med-spread (map #(get-in % [:lat :value :p50]) rs)))) (fmt (:median (med-spread (map #(get-in % [:lat :value :p99]) rs))))
               (fmt (:median (med-spread (map #(get-in % [:lat :value :max]) rs)))) (checks-str rs)))
     (for [r cruns x (:results r) :when (= :entry-bytes (:variant x)) e (:entries x)]
       (format "  entry bytes, run %s: %d matched, row %s B (sealed %s B), id-index entries %s B, lock row %s B, total %s B"
               (:run x) (:matched e) (+ (get-in e [:row :key]) (get-in e [:row :value])) (:sealed-bytes e)
               (reduce + (map #(+ (:key %) (:value %)) (vals (:id-index-entries e))))
               (+ (get-in e [:lock-row :key]) (get-in e [:lock-row :value])) (:total e)))
     ["" "Idle windows (10 s, no offers): the busiest threads, % of one core."]
     (for [r runs x (:results r) :when (#{:idle :idle-B :idle-C} (:variant x))]
       (str "  run " (:run x) ": " (str/join ", " (map (fn [[n p]] (str n " " p)) (take 6 (:busiest x)))))))))

(defn- person-report [runs]
  (let [rruns (filter #(= "run" (get-in % [:end :fn])) runs)
        cruns (filter #(= "reads" (get-in % [:end :fn])) runs)
        a (for [r rruns x (:results r) :when (= :a (:variant x))] x)
        lat-cells (fn [rs kind] (str/join " " (for [q [:p50 :p95 :p99 :max :mean]]
                                               (str (name q) " " (fmt (:median (med-spread (map #(get-in % [:lat kind q]) rs))))))))]
    (concat
     ["(a) One writer, one act at a time, 6,400 measured (closed loop: service times)."]
     [(format "  value acts/s %s; verdict (at least 100 a second): %s"
              (ms-cell (med-spread (map :value-acts-per-s a)))
              (verdict-str (verdict (:median (med-spread (map :value-acts-per-s a))) (:person-acts-per-s thresholds) :higher)))
      (str "  all offers: " (lat-cells a :value))
      (str "  leased offers (" (fmt (:median (med-spread (map #(get-in % [:lat :value-leased :n]) a)))) " a run): " (lat-cells a :value-leased))
      (str "  unleased offers: " (lat-cells a :value-unleased))
      (str "  slice: " (pr-str (get-in slices [:one-thread :sequential])))
      (str "  checks: " (checks-str a))]
     ["" "(b) K writers sharing one door (closed loops: service times)."]
     (for [[k rs] (by-level rruns :b)]
       (format "  K=%-4s value acts/s %-26s %s  home thread %s%%  verdict (100 a second) %s  checks: %s"
               k (ms-cell (med-spread (map :value-acts-per-s rs))) (lat-cells rs :value)
               (fmt (:median (med-spread (map #(second (get-in % [:cpu :store-busiest])) rs))))
               (verdict-str (verdict (:median (med-spread (map :value-acts-per-s rs))) (:person-acts-per-s thresholds) :higher))
               (checks-str rs)))
     [(str "  slice: " (get-in slices [:one-thread :concurrent]))]
     ["" "(b') Open arrival: value acts on a fixed schedule, latency from the schedule; the latency threshold (100 a second at p99 of 20 ms or less) is judged at 100 a second."]
     (for [[k rs] (by-level rruns :b-prime)]
       (format "  %s a second: offered/s %s admitted/s %s  from the schedule %s%s  checks: %s"
               k (fmt (:median (med-spread (map :offered-per-s rs)))) (ms-cell (med-spread (map :value-acts-per-s rs)))
               (lat-cells rs :value)
               (if (= 100 k)
                 (str "\n      verdict on p99 (20 ms or less): " (verdict-str (verdict (:median (med-spread (map #(get-in % [:lat :value :p99]) rs))) (:person-p99-ms thresholds) :lower)))
                 (str "\n      at ten times the rate, p99 against 20 ms: " (verdict-str (verdict (:median (med-spread (map #(get-in % [:lat :value :p99]) rs))) (:person-p99-ms thresholds) :lower))))
               (checks-str rs)))
     ["" "(c) The person's own reads through the exit (a query and an acked entry each)."]
     (for [r cruns x (:results r) :when (= :c (:variant x))]
       (str "  run " (:run x) ": reads " (get-in x [:lat :read :n]) ", " (lat-cells [x] :read))))))

(defn- lock-report [runs]
  (let [by-v (into (sorted-map) (for [r runs x (:results r)] [(:variant x) x]))
        per (fn [x n] (when (and x (pos? n)) (/ (double x) n)))
        last-p (fn [x] (peek (:points x)))
        lock-sst (fn [p] (get-in p [:lock-store :sst]))
        logical (fn [p] (+ (get-in p [:logical :key-bytes]) (get-in p [:logical :record-bytes])))
        row-bytes (fn [x] (let [p (last-p x) n (get-in p [:logical :rows])]
                            {:logical (per (logical p) n) :compacted (per (lock-sst p) n)}))]
    (concat
     (for [[v x] by-v]
       (let [n (:values x)
             size (get-in x [:config :size])
             steps (let [xs (cons 0 (map logical (:points x)))] (map - (rest xs) xs))
             mean-step (when (seq steps) (/ (double (reduce + steps)) (count steps)))
             worst (when (and mean-step (pos? mean-step)) (apply max (map #(/ (Math/abs (- % mean-step)) mean-step) steps)))
             p (last-p x)
             lg (per (logical p) n)
             cp (per (lock-sst p) n)]
         (str/join "\n"
                   (concat
                    [(format "%s: %,d values of %d B, %s; persons required %s; errors %d, refused %d; lock rows %s (expected %s); end checks %s"
                             v n size (name (get-in x [:config :grain])) (inc (count (get-in x [:config :persons])))
                             (:errors x) (:refused x) (:lock-rows-at-end x) (:lock-rows-expected x) (pr-str (:checks x)))
                     "  values | logical lock rows (B/v) | lock store compacted (B/v, method) | whole layer store compacted (B/v) | the rest (B/v) | live dir | lease rows standing"]
                    (for [q (:points x) :let [m (:values q)]]
                      (format "  %,7d | %,11d (%6.1f) | %,11d (%6.1f, %s) | %,12d (%6.1f) | %,12d (%6.1f) | %,11d | %d"
                              m (logical q) (per (logical q) m) (or (lock-sst q) 0) (or (per (lock-sst q) m) 0.0)
                              (some-> (get-in q [:lock-store :method]) name)
                              (get-in q [:compacted :sst] 0) (or (per (get-in q [:compacted :sst]) m) 0.0)
                              (or (:rest-sst q) 0) (or (per (:rest-sst q) m) 0.0)
                              (reduce + (vals (dissoc (:live q) :info-log))) (:lease-rows-standing q)))
                    [(format "  bytes per value at the end: logical %s, compacted %s; per plaintext value (%d B): logical %s (%s), compacted %s (%s); lock store over the rest: %s"
                             (fmt lg) (fmt cp) size (fmt (per lg size)) (some-> (bytes-verdict (per lg size)) :call name)
                             (fmt (per cp size)) (some-> (bytes-verdict (per cp size)) :call name)
                             (fmt (per (lock-sst p) (:rest-sst p))))
                     (format "  the curve: steps of %,d values, the largest departure of a step from the mean step %s%%"
                             (:every x) (fmt (some-> worst (* 100))))
                     (str "  the pick at the end: " (pr-str (get-in p [:lock-store :pick])) " checks " (pr-str (get-in p [:lock-store :checks]))
                          (when-let [a (get-in p [:lock-store :approximate])] (str "; RocksDB's approximate size under P " a)))
                     (str "  breakdown: " (pr-str (:breakdown x)))]))))
     [""
      "Each extra subject (bytes per lock row at 1, 2, 3 and 5 required persons):"
      (str "  " (str/join "; " (for [[v k] [["h40" 1] ["h40-p2" 2] ["h40-p3" 3] ["h40-p5" 5]] :when (by-v v)]
                                 (let [{:keys [logical compacted]} (row-bytes (by-v v))] (format "%d: %s logical, %s compacted" k (fmt logical) (fmt compacted))))))
      (let [pts (for [[v k] [["h40" 1] ["h40-p2" 2] ["h40-p3" 3] ["h40-p5" 5]] :when (by-v v)] [k (:logical (row-bytes (by-v v)))])
            n (count pts)]
        (if (< n 2)
          "  slope: fewer than two variants"
          (let [mx (/ (reduce + (map first pts)) n) my (/ (reduce + (map second pts)) n)
                slope (/ (reduce + (map (fn [[x y]] (* (- x mx) (- y my))) pts)) (reduce + (map (fn [[x _]] (* (- x mx) (- x mx))) pts)))]
            (format "  least-squares slope per extra person, logical: %s B (from the code: one 28-byte seal and one keyword in :required)" (fmt slope)))))
      (str "  slice: " (pr-str (:lock-growth slices)))])))

(defn report
  "8.5: the three `.txt` files from the three `.edn` files, complete runs
  only; no cluster and no lock. The session that runs the numbers adds the
  reading in words."
  [& _]
  (doseq [[number title body extra]
          [[:agent-rate "Phase 7, number 1: index writes a second, an agent session layer writing small acts continuously, on the finished store" agent-report
            [(str "Method: PLAN-numbers.md 4, as built (BUILD_NOTES-numbers.md). The act: one fact under " primary-key
                  " (F1: no grammar in the layer, so no value index), a 40-byte value sealed at the door, written by :ada in the session :bench-s1 citing its permission.")
             "Index writes a second: the sum over act kinds of admitted acts times that kind's writes below, over the window's seconds (2.5)."]]
           [:one-thread "Phase 7, number 3: one person's layer on one thread, acts a second and latency, on the finished store" person-report
            ["Method: PLAN-numbers.md 5, as built. (a) and (b) are closed loops: their latencies are service times at a fixed number in flight (coordinated omission, F5). (b') is an open schedule; its latency runs from each slot."]]
           [:lock-growth "Phase 7, number 2: lock store growth under hand layers, bytes per value and the curve over 100,000 values" lock-report
            ["Method: PLAN-numbers.md 6, as built: the store's own lock rows, sized in Rama 1.6.0's serializers (logical) and on disk after a forced compaction, picked out by value and re-packed alone (6.5); every point scans the whole family (BUILD_NOTES-numbers.md)."]]]]
    (let [runs (complete-runs (read-lines number))
          f (io/file (str/replace (out-file number) #"\.edn$" ".txt"))]
      (if (empty? runs)
        (log! "no complete runs for" (name number))
        (do (spit f (str (str/join "\n" (concat (header title runs extra) (body runs))) "\n"))
            (log! "wrote" (str f) "from" (count runs) "complete runs"))))))
