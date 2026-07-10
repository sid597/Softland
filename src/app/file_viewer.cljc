(ns app.file-viewer
  "File explorer sidebar — server-side file I/O and Electric bridge functions.

   Server: list-home-dirs, list-directory, read-file-content
   Electric: HomeDirs, DirContents, FileContent (e/defn wrappers)"
  (:require [hyperfiddle.electric3 :as e]
            #?(:clj [clojure.java.io :as io])
            #?(:clj [clojure.string :as str])
            #?(:clj [app.server.rama.util-fns :as util-fns])
            #?(:clj [app.server.rama.trail-view :as trail-view])
            #?(:clj [app.server.ingest-watchers :as ingest-watchers])
            ;; Faces-as-assemblies · the ONE generic face artery (CONTRACT §7).
            #?(:clj [app.server.rama.face-projection :as face-projection])
            #?(:clj [app.server.rama.object-container.block-distiller :as block-distiller])
            #?(:clj [app.server.rama.dogfood.transcript :as transcript])))

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

;; ============================================================================
;; Faces-as-assemblies · the ONE generic face artery (framework CONTRACT §7).
;; FacePull is the single, face-agnostic pull (trap T8: NO face-keyword dispatch
;; here — dispatch lives server-side in face-projection/serve, the projection
;; registry). It hands the whole request to `serve` and returns ONE data-context;
;; the request-watch loop in electric_flow resets ONE !face-data atom with it.
;;
;; First-light runtime (OI-1, the trail-view-runtime precedent): no production
;; block-distiller runtime exists yet, so the first face pull boots an in-process
;; OC runtime and asynchronously harvests + distills the default conversation
;; (7c80ce2a) so the Outline face has real durable state to wear. The heavy
;; exercise of this path is W1-INT/G15 (the wearing); the projection GATES
;; (G10-G13) drive face-projection/serve directly over a test-stood-up runtime.
;; ============================================================================

#?(:clj
   (defn- find-default-transcript []
     (let [dir (io/file (str (System/getProperty "user.home")
                             "/.claude/projects/-mnt-data-projects-Softland"))]
       (when (.isDirectory dir)
         (first (filter #(re-find #"^7c80ce2a-.*\.jsonl$" (.getName ^java.io.File %))
                        (.listFiles dir)))))))

#?(:clj
   (defonce face-projection-runtime
     ;; Boots the OC runtime synchronously (seconds); harvests + distills the
     ;; default conversation in a future (minutes on the 6.9MB corpus), publishing
     ;; the resulting object-key into !default-address when ready. FacePull serves
     ;; whatever is ready — empty until the distill lands, then the epoch re-pull
     ;; (INV-19) fills it in. READ-ONLY afterwards (G12): serve never writes.
     (delay
       (let [rt (block-distiller/start-distiller-runtime!)
             !default-address (atom nil)
             ;; G16 falsification fix: pending / failed / genuinely-blank are
             ;; THREE realities — the map must not lie. This flag + the
             ;; resolve-request hint let the projection name each distinctly.
             !first-light-failed (atom false)]
         (if-let [file (find-default-transcript)]
           (future
             (try
               (let [req (transcript/transcript-request
                          :transcript/harvest
                          {:transcript/request-id "face-projection-default"
                           :transcript/source :claude-code
                           :transcript/paths [(.getPath ^java.io.File file)]
                           :time-ms 0})
                     obs (vec (transcript/read-jsonl-observations req file 0))
                     conv-id (some #(when (str/starts-with? (str %) "7c80ce2a") %)
                                   (distinct (map :transcript/conversation-id obs)))
                     harvest (transcript/harvest-transcripts-into-object-container!
                              (:oc-rt rt) req)]
                 (when (= :complete (:status harvest))
                   (let [summary (block-distiller/distill-conversation!
                                  {:oc-rt (:oc-rt rt)
                                   :source :claude-code
                                   :conversation-id conv-id})]
                     (reset! !default-address (:object-key summary))
                     ;; W1-INT wearing fix (G15): the distill IS an ingest —
                     ;; bump the ingest epoch like every watcher import does
                     ;; (ingest_watchers.clj:113), so the INV-19 epoch push →
                     ;; client debounce → re-stamped request → re-pull fills
                     ;; the face in. Without this the request value never
                     ;; changes and the face stays stuck empty (the
                     ;; "state stuck masking future truth" lifecycle failure).
                     (swap! util-fns/!ingest-epoch-atom inc)
                     (println "[FACE] default conversation distilled:"
                              (:object-key summary) "river=" (:river summary)))))
               (catch Throwable t
                 (println "[FACE] default distill failed:" (.getMessage t))
                 ;; failure is pushed, not hidden: mark failed AND bump the
                 ;; epoch so the client re-pulls and renders the honest
                 ;; :first-light-failed error instead of hanging on pending
                 ;; forever (G16 falsification fix)
                 (reset! !first-light-failed true)
                 (swap! util-fns/!ingest-epoch-atom inc))))
           ;; off-box (no transcript file): first-light can never fill — failed,
           ;; not eternally pending (G16 falsification fix)
           (reset! !first-light-failed true))
         {:oc-rt (:oc-rt rt)
          :!default-address !default-address
          :!first-light-failed !first-light-failed}))))

#?(:clj
   (defn face-ctx
     "The server ctx `serve` reads: {:oc-rt <runtime>}. Plain map, no face names."
     []
     (select-keys @face-projection-runtime [:oc-rt])))

#?(:clj
   (defn resolve-request
     "Substitute the first-light default address when a request leaves :address blank
      or names :default (the bottom-bar command may pick the conversation before the
      hash is known). Address defaulting only — NOT face dispatch (trap T8).
      While the default is not (yet) available, stamp the honest first-light
      status so the projection's error names the ACTUAL reality — distilling vs
      failed vs a genuinely blank request (G16 falsification fix)."
     [request]
     (let [addr (:address request)]
       (if (or (nil? addr) (= :default addr))
         (let [{:keys [!default-address !first-light-failed]} @face-projection-runtime]
           (if-let [default @!default-address]
             (assoc request :address default)
             (assoc request :address nil
                    :face/first-light (if @!first-light-failed
                                        :first-light-failed
                                        :first-light-pending))))
         request))))

(e/defn FacePull [request]
  ;; ONE generic pull. Server-side `serve` routes the request through the projection
  ;; registry and returns the whole §7 data-context. No per-face branch, ever.
  (e/server (face-projection/serve (face-ctx) (resolve-request request))))
