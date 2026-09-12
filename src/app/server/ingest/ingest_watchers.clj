(ns app.server.ingest.ingest-watchers
  "Filesystem-triggered imports over a caller-owned ObjectContainer runtime.
   initial-sweep! imports existing classified files; start-ingest-watchers!
   registers create/modify events, debounces each path and serializes imports.
   Markdown delegates to markdown-adapter; JSONL delegates to transcript's
   common import driver. Accepted results increment the process-local ingest
   epoch, which is an invalidation signal rather than durable material.

   Each watcher owns its WatchService, directory-key/pending-timer maps,
   scheduler, importer executor, daemon watch thread and running flag. The
   returned :stop! releases those resources; it does not close the borrowed
   runtime. door/cluster exposes explicit sweep/watch entrypoints. The retained
   run-git-spine-boot! name denotes a three-stage helper, not automatic startup."
  (:require [app.server.rama.object-container.runtime :as ocr]
            [app.server.ingest.markdown-adapter :as markdown-adapter]
            [app.server.ingest.transcript :as transcript]
            [app.server.ingest.git-import :as git-import]
            [app.server.rama.ingest-epoch :as ingest-epoch])
  (:import [java.io File]
           [java.nio.file FileSystems Files LinkOption Path Paths
            StandardWatchEventKinds WatchKey WatchService]
           [java.util.concurrent Executors ExecutorService ScheduledExecutorService
            ScheduledFuture TimeUnit]))

(def ^:private default-debounce-ms 500)

;; -- helpers ------------------------------------------------------------------

(defn- ^Path ->path
  "Coerce a Path, File or string-like value to java.nio.file.Path."
  [x]
  (cond
    (instance? Path x) x
    (instance? File x) (.toPath ^File x)
    :else (Paths/get (str x) (make-array String 0))))

(defn- directory?
  "Test whether the path resolves to a directory using default link options."
  [^Path p]
  (Files/isDirectory p (make-array LinkOption 0)))

(defn- regular-file?
  "Test whether the path resolves to a regular file using default link options."
  [^Path p]
  (Files/isRegularFile p (make-array LinkOption 0)))

(defn- classify
  "Return :md or :jsonl for matching filename suffixes, otherwise nil. A custom classifier changes selection, not the importer dispatch table."
  [^Path p]
  (let [name (str (.getFileName p))]
    (cond
      (.endsWith name ".md") :md
      (.endsWith name ".jsonl") :jsonl
      :else nil)))

(defn- log
  "Write one ingest-watcher log line to stderr."
  [level & args]
  (binding [*out* *err*]
    (println (str "[ingest-watchers] " (name level)) (apply pr-str args))))

;; -- the existing import seam (NO new truth; slurp/read is the only new code) -

(defn- import-md!
  "Read a Markdown file, append its common material-import request and await its decision for 5 seconds; return the decision or nil on timeout."
  [runtime ^File file]
  (let [raw-text (slurp file)
        source-ref (.getPath file)
        request (markdown-adapter/markdown-source-import-request raw-text source-ref)]
    (ocr/append-object-container-request! runtime request)
    (ocr/await-object-container-decision runtime request 5000)))

(defn- import-jsonl!
  "Read a JSONL file from offset zero as Claude Code observations and invoke
   the common OC import driver. Returns its result without updating file-state
   cursors. Re-import may fail if stored predecessor context changes a previously
   seen line's fingerprint; this is not the incremental episode path."
  [runtime ^File file]
  (let [request (transcript/transcript-request
                 :transcript/watch
                 {:transcript/paths [(.getPath file)]
                  :transcript/source :claude-code})
        observations (vec (transcript/read-jsonl-observations request file 0))]
    (transcript/import-observations-into-object-container! runtime request observations {})))

(defn- import-succeeded?
  "The deterministic decision latch: a returned decision/result whose :status is
   :accepted. Never an fs poll."
  [result]
  (= :accepted (:status result)))

(defn- run-import!
  "Dispatch a classified file to the Markdown or JSONL importer. An :accepted
   result increments the local ingest epoch and invokes on-import with a receipt;
   rejected/missing results do not bump it. Exceptions inside the try are logged
   and reported as :error. The classifier runs before the try, and a throwing
   error callback can escape; callbacks/classifiers should not throw. Retry is
   triggered by a later change event, not by a dedicated retry queue."
  ([runtime ^File file on-import]
   (run-import! runtime file on-import classify))
  ([runtime ^File file on-import classify-fn]
   (let [kind (classify-fn (->path file))]
    (try
      (let [result (case kind
                     :md (import-md! runtime file)
                     :jsonl (import-jsonl! runtime file)
                     nil)]
        (if (import-succeeded? result)
          (let [epoch (swap! ingest-epoch/!ingest-epoch-atom inc)]
            (when on-import
              (on-import {:file file :kind kind :status :accepted
                          :decision result :epoch epoch}))
            {:status :accepted :epoch epoch})
          (do
            (log :warn "import not accepted" {:file (.getPath file) :kind kind
                                              :status (:status result)})
            (when on-import
              (on-import {:file file :kind kind :status (or (:status result) :skipped)
                          :decision result}))
            {:status (or (:status result) :skipped)})))
      (catch Throwable t
        (log :error "import threw; loop survives" {:file (.getPath file) :kind kind
                                                   :ex (.getName (class t))
                                                   :error (.getMessage t)})
        (when on-import
          (on-import {:file file :kind kind :status :error :error t}))
        {:status :error :error t})))))

;; -- recursive directory registration -----------------------------------------

(defn- register-dir!
  "Register create/modify events for one directory and retain WatchKey-to-directory routing in the supplied atom."
  [^WatchService ws ^Path dir key->dir]
  (let [key (.register dir ws (into-array [StandardWatchEventKinds/ENTRY_CREATE
                                           StandardWatchEventKinds/ENTRY_MODIFY]))]
    (swap! key->dir assoc key dir)
    key))

(defn- register-tree!
  "Register `root` and every existing subdirectory. WatchService is non-recursive
   by design, so a tree (docs/, vision/) needs each directory registered."
  [^WatchService ws ^Path root key->dir]
  (when (directory? root)
    (register-dir! ws root key->dir)
    (doseq [^Path child (vec (.toArray (Files/list root)))]
      (when (directory? child)
        (register-tree! ws child key->dir)))))

;; -- debounce + serialized import ---------------------------------------------

(defn- schedule-import!
  "Debounce per path: cancel any pending timer for this path and (re)arm one
   `debounce-ms` out. When it fires it submits the import to the single-thread
   importer executor (serialized imports). A ScheduledExecutorService timer is
   event-driven, not a poll loop."
  [^ScheduledExecutorService scheduler ^ExecutorService importer pending
   debounce-ms ^File file run-fn]
  (let [key (.getPath file)]
    (swap! pending
           (fn [m]
             (when-let [^ScheduledFuture prev (get m key)]
               (.cancel prev false))
             (let [task (fn []
                          (swap! pending dissoc key)
                          (.submit importer ^Runnable (fn [] (run-fn file))))
                   fut (.schedule scheduler ^Runnable task
                                  (long debounce-ms) TimeUnit/MILLISECONDS)]
               (assoc m key fut))))))

;; -- public API ---------------------------------------------------------------

(defn initial-sweep!
  "Synchronously attempt each classified existing file under roots and return
   {:attempted N :imported N}; imported counts :accepted results, including
   accepted replays. Selection defaults to .md/.jsonl. This is an explicit sweep
   over the supplied runtime; it neither creates a cluster nor starts watchers."
  [{:keys [runtime roots on-import classify-fn]}]
  (let [classify-fn (or classify-fn classify)
        files (for [root roots
                    ^File f (file-seq (File. (str root)))
                    :when (and (.isFile f) (classify-fn (.toPath f)))]
                f)
        results (mapv #(run-import! runtime % on-import classify-fn) files)]
    {:attempted (count results)
     :imported (count (filter #(= :accepted (:status %)) results))}))

(defn run-git-spine-boot!
  "Run assertion-log replay, commit spine sync and transcript join extraction
   in order with cfg's borrowed runtime and paths. Each stage logs counts and
   catches failure so later stages still run; no aggregate success is returned.
   Called explicitly by door/cluster ingest!, despite the retained boot name.
   Relation requests use append acknowledgements without awaiting microbatches."
  [cfg]
  (doseq [[label f] [[:replay git-import/replay-assert-log!]
                     [:spine-sync git-import/spine-sync!]
                     [:extract git-import/extract-session-joins!]]]
    (try
      ;; Stats line stays counts-only (t4-spine seam 3): :edge-relation-ids is
      ;; a per-edge id VECTOR (one entry per parent edge — hundreds over the
      ;; real repo) returned for tests/queries; printing it buried the counts.
      ;; The counts themselves are printed verbatim from the stage's return.
      (log :info (str "git-spine " (name label))
           (dissoc (f cfg) :edge-relation-ids))
      (catch Throwable t
        (log :error (str "git-spine " (name label) " threw; boot survives")
             {:ex (.getName (class t)) :error (.getMessage t)})))))

(defn start-ingest-watchers!
  "Watch directory roots for create/modify events; imports begin on events,
   not an initial sweep. Debounce each path for at least 500 ms and serialize
   imports on one executor. :classify-fn defaults to .md/.jsonl selection;
   :on-import receives attempt results and should not throw.

   Returns {:watch-service :roots :stop!}. Stop cancels pending timers, closes
   the WatchService, interrupts its daemon thread and shuts down both executors.
   It does not await termination or close the runtime. Delete events are not
   registered, and overflow events have no reconciliation sweep."
  [{:keys [runtime roots debounce-ms on-import classify-fn]}]
  (let [classify-fn (or classify-fn classify)
        debounce-ms (max default-debounce-ms (or debounce-ms default-debounce-ms))
        ws (.newWatchService (FileSystems/getDefault))
        key->dir (atom {})
        pending (atom {})
        scheduler (Executors/newSingleThreadScheduledExecutor)
        importer (Executors/newSingleThreadExecutor)
        running (atom true)
        run-fn (fn [^File file] (run-import! runtime file on-import classify-fn))]
    (doseq [root roots]
      (try
        (register-tree! ws (->path root) key->dir)
        (catch Throwable t
          (log :warn "could not register root" {:root (str root) :error (.getMessage t)}))))
    (let [watch-thread
          (Thread.
           (fn []
             (try
               (while @running
                 (let [^WatchKey key (.take ws)
                       ^Path dir (get @key->dir key)]
                   (when dir
                     (doseq [event (.pollEvents key)]
                       (try
                         (let [^Path rel (cast Path (.context event))
                               ^Path child (.resolve dir rel)]
                           (cond
                             (directory? child)
                             ;; new subdirectory: start watching it too
                             (register-tree! ws child key->dir)

                             (and (regular-file? child) (classify-fn child))
                             (schedule-import! scheduler importer pending debounce-ms
                                               (.toFile child) run-fn)

                             :else nil))
                         (catch Throwable t
                           (log :error "event handling failed; loop survives"
                                {:error (.getMessage t)})))))
                   (.reset key)))
               (catch java.nio.file.ClosedWatchServiceException _ nil)
               (catch InterruptedException _ nil)
               (catch Throwable t
                 (log :error "watch thread exiting on unexpected error"
                      {:error (.getMessage t)}))))
           "ingest-watchers")]
      (.setDaemon watch-thread true)
      (.start watch-thread)
      {:watch-service ws
       :roots (vec roots)
       :stop!
       (fn []
         (reset! running false)
         (doseq [[_ ^ScheduledFuture fut] @pending]
           (try (.cancel fut false) (catch Throwable _ nil)))
         (reset! pending {})
         (try (.close ws) (catch Throwable _ nil))
         (.interrupt watch-thread)
         (.shutdownNow scheduler)
         (.shutdownNow importer)
         nil)})))
