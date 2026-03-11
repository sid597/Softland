(ns app.client.workspace.runtime.sidebar-io
  "Sidebar I/O: HTTP fetch helpers, directory/file loading, sidebar visibility watch."
  (:require [clojure.string :as str]
            [cljs.reader :as reader]))

(defn make-sidebar-io
  "Create sidebar I/O closures. Returns {:fetch-edn! :post-edn! :fetch-home-dirs! :fetch-dir! :fetch-file!}."
  [{:keys [!sidebar-state !current-file !scroll-x !file-load-request]}]
  (let [fetch-edn!
        (fn
          ([url callback]
           (-> (js/fetch url)
               (.then (fn [resp] (.text resp)))
               (.then (fn [text] (callback (reader/read-string text))))
               (.catch (fn [err] (js/console.error "[SIDEBAR] Fetch error:" err)))))
          ([url callback err-callback]
           (-> (js/fetch url)
               (.then (fn [resp] (.text resp)))
               (.then (fn [text] (callback (reader/read-string text))))
               (.catch (fn [err] (err-callback err))))))

        post-edn!
        (fn [url body callback]
          (-> (js/fetch url
                        (clj->js {:method "POST"
                                  :headers {"Content-Type" "application/edn"}
                                  :body (pr-str body)}))
              (.then (fn [resp] (.text resp)))
              (.then (fn [text] (callback (reader/read-string text))))
              (.catch (fn [err]
                        (js/console.error "[AGENT][HTTP][POST-ERROR]"
                                          (clj->js {:url url
                                                    :message (.-message err)})
                                          err))))
          nil)

        fetch-home-dirs!
        (fn []
          (when (nil? (:home-dirs @!sidebar-state))
            (fetch-edn! "/api/home-dirs"
                        (fn [dirs]
                          (swap! !sidebar-state assoc :home-dirs dirs)))))

        fetch-dir!
        (fn [path]
          (when-not (contains? (:dir-cache @!sidebar-state) path)
            (fetch-edn! (str "/api/list-dir?path=" (js/encodeURIComponent path))
                        (fn [entries]
                          (swap! !sidebar-state assoc-in [:dir-cache path] entries)))))

        fetch-file!
        (fn [path root-path & {:keys [target-line]}]
          (fetch-edn! (str "/api/read-file?path=" (js/encodeURIComponent path)
                           "&root=" (js/encodeURIComponent root-path))
                      (fn [result]
                        (if (:error result)
                          (js/console.error "[SIDEBAR] File read error:" (:error result))
                          (let [lines (str/split-lines (:content result))]
                            (reset! !current-file {:path path :name (last (str/split path #"/"))})
                            (reset! !scroll-x 0)
                            (reset! !file-load-request
                                    (cond-> {:lines lines}
                                      target-line (assoc :target-line target-line))))))))]
    {:fetch-edn! fetch-edn!
     :post-edn! post-edn!
     :fetch-home-dirs! fetch-home-dirs!
     :fetch-dir! fetch-dir!
     :fetch-file! fetch-file!}))

(defn install-sidebar-watch!
  "Watch !sidebar-visible; fetch home dirs when sidebar becomes visible."
  [{:keys [!sidebar-visible]} fetch-home-dirs!]
  (when !sidebar-visible
    (when @!sidebar-visible
      (fetch-home-dirs!))
    (add-watch !sidebar-visible :sidebar-fetch
               (fn [_ _ old-vis new-vis]
                 (when (and new-vis (not old-vis))
                   (fetch-home-dirs!))))))

(defn seed-initial-file!
  "If initial-file provided, set current-file and expand sidebar dirs."
  [{:keys [!current-file !sidebar-state]} {:keys [fetch-dir!]} initial-file]
  (when initial-file
    (let [file-path (:path initial-file)
          file-name (last (str/split file-path #"/"))
          project-path (:project initial-file)]
      (reset! !current-file {:path file-path :name file-name})
      (when project-path
        (let [rel (subs file-path (count project-path))
              rel (if (str/starts-with? rel "/") (subs rel 1) rel)
              parts (str/split rel #"/")
              dir-parts (butlast parts)
              dir-paths (loop [acc [] prefix project-path dirs dir-parts]
                          (if (empty? dirs)
                            acc
                            (let [next-path (str prefix "/" (first dirs))]
                              (recur (conj acc next-path) next-path (rest dirs)))))]
          (swap! !sidebar-state assoc
                 :project {:name (last (str/split project-path #"/"))
                           :path project-path}
                 :expanded-dirs (set dir-paths))
          (fetch-dir! project-path)
          (doseq [dp dir-paths]
            (fetch-dir! dp)))))))
