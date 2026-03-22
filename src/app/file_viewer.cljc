(ns app.file-viewer
  "File explorer sidebar — server-side file I/O and Electric bridge functions.

   Server: list-home-dirs, list-directory, read-file-content
   Electric: HomeDirs, DirContents, FileContent (e/defn wrappers)"
  (:require [hyperfiddle.electric3 :as e]
            #?(:clj [clojure.java.io :as io])
            #?(:clj [clojure.string :as str])
            #?(:clj [app.server.rama.util-fns :as util-fns])))

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
