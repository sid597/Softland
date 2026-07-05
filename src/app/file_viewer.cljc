(ns app.file-viewer
  "File explorer sidebar — server-side file I/O and Electric bridge functions.

   Server: list-home-dirs, list-directory, read-file-content
   Electric: HomeDirs, DirContents, FileContent (e/defn wrappers)"
  (:require [hyperfiddle.electric3 :as e]
            #?(:clj [clojure.java.io :as io])
            #?(:clj [clojure.string :as str])
            #?(:clj [app.server.rama.util-fns :as util-fns])
            #?(:clj [app.server.rama.trail-view :as trail-view])
            #?(:clj [app.server.ingest-watchers :as ingest-watchers])))

;; ============================================================================
;; SERVER SIDE — File I/O (JVM only)
;; ============================================================================

#?(:clj
   (do
     (def hidden-dir-prefixes
       "Directories to filter out from home listing"
       #{"." "snap" "lost+found"})

     (def ignored-names
       "Names to filter from directory listings"
       #{"node_modules" "target" ".git" ".cpcache" ".shadow-cljs"
         "__pycache__" ".clj-kondo" ".lsp" ".nrepl-port"})

     (defn list-home-dirs []
       "List top-level directories in user's home folder.
        Returns [{:name \"projects\" :path \"/home/sid/projects\"} ...]"
       (let [home (io/file (System/getProperty "user.home"))
             dirs (->> (.listFiles home)
                       (filter #(.isDirectory %))
                       (remove #(some (fn [prefix] (.startsWith (.getName %) prefix))
                                      hidden-dir-prefixes))
                       (sort-by #(.toLowerCase (.getName %))))]
         (mapv (fn [f] {:name (.getName f)
                        :path (.getAbsolutePath f)})
               dirs)))

     (defn list-directory [path]
       "List one level of a directory. Dirs first, then files, both alphabetical.
        Returns [{:name :type :path} ...]"
       (let [dir (io/file path)]
         (if (and (.exists dir) (.isDirectory dir))
           (let [children (->> (.listFiles dir)
                               (remove #(or (.startsWith (.getName %) ".")
                                            (contains? ignored-names (.getName %))))
                               (sort-by #(.toLowerCase (.getName %))))
                 dirs  (filter #(.isDirectory %) children)
                 files (remove #(.isDirectory %) children)]
             (vec (concat
                    (mapv (fn [f] {:name (.getName f) :type :dir  :path (.getAbsolutePath f)}) dirs)
                    (mapv (fn [f] {:name (.getName f) :type :file :path (.getAbsolutePath f)}) files))))
           [])))

     (defn path-under-root? [path root-path]
       "Security check: ensure path is under root-path (no directory traversal)"
       (let [canonical-path (.getCanonicalPath (io/file path))
             canonical-root (.getCanonicalPath (io/file root-path))]
         (.startsWith canonical-path canonical-root)))

     (defn read-file-content [path root-path]
       "Read file content safely. Validates path is under root-path.
        Returns {:content \"...\" :path path :name \"file.clj\"} or error map."
       (let [f (io/file path)]
         (cond
           (not (path-under-root? path root-path))
           {:error "Access denied: path outside project root"}

           (not (.exists f))
           {:error (str "File not found: " path)}

           (.isDirectory f)
           {:error "Cannot read directory as file"}

           (> (.length f) (* 1024 1024)) ;; 1MB limit
           {:error (str "File too large: " (quot (.length f) 1024) "KB")}

           :else
           (try
             {:content (slurp f)
              :path    path
              :name    (.getName f)}
             (catch Exception e
               {:error (str "Read error: " (.getMessage e))})))))))

#?(:cljs
   (do
     (defn list-home-dirs [] [])
     (defn list-directory [_] [])
     (defn read-file-content [_ _] {:error "Server only"})))

;; ============================================================================
;; ELECTRIC BRIDGE FUNCTIONS
;; ============================================================================

(e/defn HomeDirs []
  (e/server (list-home-dirs)))

(e/defn DirContents [path]
  (e/server (list-directory path)))

(e/defn FileContent [path root]
  (e/server (read-file-content path root)))

(e/defn WatchSidebarTruth
  "Reactive bridge: Rama sidebar truth → Electric client.
   Watches the server-side mirror atom (updated by emit-sidebar-event!
   after each Rama write). foreign-proxy-async is broken in Rama 1.6.0
   test IPC, so this uses e/watch on the atom instead."
  []
  (e/server (e/watch util-fns/!sidebar-truth-atom)))

(e/defn WatchUserSettings
  "Reactive bridge: Rama user settings → Electric client.
   Same pattern as WatchSidebarTruth — server atom mirror + e/watch."
  []
  (e/server (e/watch util-fns/!settings-truth-atom)))

(e/defn WatchAgentTrail
  "Reactive bridge: latest completed agent trail → Electric client.
   Returns {:run-id \"...\" :trail-data {...}} or nil."
  []
  (e/server (e/watch util-fns/!agent-trail-atom)))

(e/defn WatchFlowSession
  "Reactive bridge: DG workflow FSM state → Electric client."
  []
  (e/server (e/watch util-fns/!flow-session-atom)))

(e/defn WatchWorkspaceTruth
  "Reactive bridge: workspace truth (selected artifact, active pane, sidebar) → Electric client."
  []
  (e/server (e/watch util-fns/!workspace-truth-atom)))

;; ============================================================================
;; Trail face bridge (view-mvp WP-B2, CONTRACT §2.3)
;; Trail* names are THIS package's e/defns; each calls ONLY the named WP1 §7
;; wrapper inside e/server (S1). WatchIngestEpoch reads the ingest-epoch
;; mirror atom — the single sanctioned out-of-§7 read (S1a: a counter, not
;; truth).
;; ============================================================================

#?(:clj
   (defonce trail-view-runtime
     ;; OI-1: no production OC runtime exists yet. First light runs on the
     ;; same in-process IPC boot the test suites use, lazily (the util-fns
     ;; text-kernel delay precedent). The FIRST /trail pull pays the cluster
     ;; boot (seconds); ingest then streams ASYNC — initial sweep of the
     ;; md corpus + live watchers share this handle, and every accepted
     ;; import bumps the epoch, so the face fills in near-live as material
     ;; lands. First-light scope: docs/current-mental-model + vision only
     ;; (the 1.2GB transcript dir waits for offset-incremental re-read —
     ;; named follow-up in the watcher design notes).
     (delay
       (let [rt (trail-view/start-trail-view-runtime!)
             dir (System/getProperty "user.dir")
             roots (into []
                         (filter #(.exists (io/file %)))
                         [(str dir "/docs/current-mental-model")
                          (str dir "/vision")])
             cfg {:runtime rt :roots roots
                  ;; git-spine WP2 cfg (CONTRACT §3.C plumbing): repo-root drives
                  ;; the commit reader + the repo-root-relative durable paths; the
                  ;; transcript corpus is read raw (never OC-swept) for the join
                  ;; pass; the cursor + assert-log live under untracked data/.
                  :repo-root dir
                  :transcript-roots (into []
                                          (filter #(.exists (io/file %)))
                                          [(str (System/getProperty "user.home") "/.claude/projects")])
                  :spine-cursor-path (str dir "/data/git-spine-cursor.edn")
                  ;; minted HERE, in the same delay body that creates the
                  ;; cluster, so cursor validity is bound to THIS cluster
                  ;; instance's lifetime: a fresh JVM = fresh empty cluster =
                  ;; fresh run-id, and the durable cursor from the previous
                  ;; boot is ignored (full reprocess — the edges must be
                  ;; re-asserted into the empty cluster, never skipped)
                  :spine-run-id (str (java.util.UUID/randomUUID))
                  :assert-log-path (str dir "/data/relation-assert-log.ednl")}]
         (future
           (try
             (let [{:keys [imported attempted]} (ingest-watchers/initial-sweep! cfg)]
               (println "[TRAIL] initial sweep done:" imported "of" attempted "imported"))
             (ingest-watchers/start-ingest-watchers! cfg)
             (println "[TRAIL] live watchers running on" (pr-str roots))
             ;; git-spine WP2 boot hook (CONTRACT §3.C): replay -> spine-sync -> extract
             (ingest-watchers/run-git-spine-boot! cfg)
             (println "[TRAIL] git-spine boot done")
             (catch Throwable t
               (println "[TRAIL] ingest boot failed:" (.getMessage t)))))
         rt))))

#?(:clj (defn trail-rt [] @trail-view-runtime))

(e/defn TrailBundle [targets opts]
  (e/server (trail-view/read-context-bundle (trail-rt) targets opts)))

(e/defn TrailFeed [window opts]
  (e/server (trail-view/read-recent-activity (trail-rt) window opts)))

(e/defn TrailConversation [conversation cursor limit]
  (e/server (trail-view/read-conversation-trail (trail-rt) conversation cursor limit)))

(e/defn TrailText [targets opts]
  (e/server (trail-view/render-bundle-text
             (trail-view/read-context-bundle (trail-rt) (or targets []) (or opts {})))))

(e/defn WatchIngestEpoch []
  (e/server (e/watch util-fns/!ingest-epoch-atom)))
