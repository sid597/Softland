(ns app.server.ingest.ingest-watchers
  "Near-live ingest watchers (view-MVP WP-B2 sec 2.4/6, OP-36..OP-39).

   OUTSIDE every Rama module by construction: a plain namespace with no depots
   and no topologies. It is an OS-side trigger layer that watches configured
   filesystem roots and calls the EXISTING object-container import seam only --
   it introduces NO new ingestor and NO new truth (trap 2). Convergent
   re-import is the safety net: deterministic content-addressed ids +
   idempotency journals (D-008.3) make a byte-identical rewrite replay the
   accepted decision without writing new rows.

   Event-driven, never polled (quirks INV-19): a java.nio.file.WatchService
   (inotify on Linux) blocks on `.take`, so no check-and-sleep loop exists
   anywhere. Per-path debounce (>= 500 ms) coalesces the create/modify burst of
   a single save; imports are serialized on one thread. On the DETERMINISTIC
   decision latch (append + await-decision -- never a filesystem poll) the
   monotonic ingest-epoch counter is bumped by exactly one. Import failures are
   logged and retried on the next change event; the loop never crashes."
  (:require [app.server.rama.object-container.runtime :as ocr]
            [app.server.ingest.markdown-adapter :as markdown-adapter]
            [app.server.ingest.transcript :as transcript]
            [app.server.ingest.git-spine :as git-spine]
            [app.server.rama.util-fns :as util-fns])
  (:import [java.io File]
           [java.nio.file FileSystems Files LinkOption Path Paths
            StandardWatchEventKinds WatchKey WatchService]
           [java.util.concurrent Executors ExecutorService ScheduledExecutorService
            ScheduledFuture TimeUnit]))

(def ^:private default-debounce-ms 500)

;; -- helpers ------------------------------------------------------------------

(defn- ^Path ->path
  [x]
  (cond
    (instance? Path x) x
    (instance? File x) (.toPath ^File x)
    :else (Paths/get (str x) (make-array String 0))))

(defn- directory?
  [^Path p]
  (Files/isDirectory p (make-array LinkOption 0)))

(defn- regular-file?
  [^Path p]
  (Files/isRegularFile p (make-array LinkOption 0)))

(defn- classify
  "Return :md, :jsonl, or nil for a path, dispatching on the file extension.
   The DEFAULT classify-fn (framework W2, CONTRACT §16): watchers constructed
   without :classify-fn behave exactly as before — zero change for existing
   callers, and bare `.edn` is deliberately NOT here (trap T19)."
  [^Path p]
  (let [name (str (.getFileName p))]
    (cond
      (.endsWith name ".md") :md
      (.endsWith name ".jsonl") :jsonl
      :else nil)))

(defn- log
  [level & args]
  (binding [*out* *err*]
    (println (str "[ingest-watchers] " (name level)) (apply pr-str args))))

;; -- the existing import seam (NO new truth; slurp/read is the only new code) -

(defn- import-md!
  "Slurp a settled .md file and drive it through the EXISTING markdown import
   seam: markdown-source-import-request -> append -> await the decision. The
   slurp is the only new reader; the request builder and kernel-write seam are
   verbatim the existing path (trap 2). Returns the decision (a map with
   :status)."
  [runtime ^File file]
  (let [raw-text (slurp file)
        source-ref (.getPath file)
        request (markdown-adapter/markdown-source-import-request raw-text source-ref)]
    (ocr/append-object-container-request! runtime request)
    (ocr/await-object-container-decision runtime request 5000)))

(defn- import-jsonl!
  "Drive one settled .jsonl transcript file through the EXISTING per-file
   transcript import driver (read-jsonl-observations +
   import-observations-into-object-container!). Reads from offset 0 and relies
   on per-line idempotency to converge on re-import. Returns the import result
   (a map with :status)."
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
  "Import one settled file. Bounded by try/catch so the loop NEVER crashes: any
   import failure is logged, the epoch is NOT bumped, and the watcher keeps
   running (the next change event retries). On the accepted decision latch the
   monotonic ingest-epoch counter is bumped by exactly one. `on-import`, when
   supplied, is called with an event map for every attempt (the sanctioned
   no-poll completion signal for tests).

   `classify-fn` is the constructing watcher's own classifier; the 3-arity
   keeps the default `classify` — zero behavior change for existing callers."
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
          (let [epoch (swap! util-fns/!ingest-epoch-atom inc)]
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
  "One-time import of every classified file ALREADY under the roots.
   WatchService fires only on CHANGES, and the in-process runtime is
   non-durable — a fresh boot needs the existing material imported once.
   Serialized in the calling thread; re-runs converge (deterministic ids +
   idempotency journals, D-008.3). Returns {:imported N :attempted N}.
   Optional :classify-fn (framework W2, CONTRACT §16) — default = the
   existing `classify`, zero change for existing callers."
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
  "git-spine WP2 boot hook (CONTRACT §3.C, P1-owned). After the initial sweep, on
   the SAME trail-view runtime, run the git-spine sequence IN ORDER:
   replay-assert-log! (re-append the durable /assert write-ahead log) ->
   spine-sync! (git commit metadata + parent :based-on edges) ->
   extract-session-joins! (transcript -> commit/doc :produced edges). Each stage
   is bounded so a failure NEVER crashes boot; each is idempotent (deterministic
   ids + idempotency journals), so a re-run converges. `cfg` carries :runtime
   :repo-root :transcript-roots :spine-cursor-path :assert-log-path. Runs
   fire-and-forget wrt the relation microbatch (edges materialize async)."
  [cfg]
  (doseq [[label f] [[:replay git-spine/replay-assert-log!]
                     [:spine-sync git-spine/spine-sync!]
                     [:extract git-spine/extract-session-joins!]]]
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
  "Start event-driven ingest watchers.

   config:
     :runtime      the object-container runtime handle (start-object-container-runtime!)
     :roots        seq of directory paths (String/File/Path) to watch (trees)
     :debounce-ms  per-path settle window, >= 500 (default 500)
     :on-import    optional (fn [event-map]) fired on each import attempt --
                   {:file File :kind :md|:jsonl :status :accepted|:error|... }.
                   Tests latch on this callback (no polling).
     :classify-fn  optional per-watcher classifier; default = the existing
                   `classify`, zero change for existing callers.

   Returns a handle map: {:stop! (fn []) :watch-service ws :roots [...]}."
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
