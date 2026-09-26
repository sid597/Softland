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
    (let [out (sh-out "df" "-T" "--output=source,fstype" (str dir))
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
