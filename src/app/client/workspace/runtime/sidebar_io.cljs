(ns app.client.workspace.runtime.sidebar-io
  "Sidebar I/O: HTTP fetch helpers, directory/file loading, sidebar visibility watch."
  (:require [clojure.string :as str]
            [cljs.reader :as reader]))

(defn save-editor-doc!
  "Persist editor document state to Rama. Logs full round-trip latency
   for Phase 4B measurement. Fire-and-forget — the client does NOT wait
   for the response to update the editor."
  [file-path doc-state]
  (when (and file-path doc-state)
    (let [t0 (js/performance.now)]
      (-> (js/fetch "/api/editor/save-doc"
            (clj->js {:method "POST"
                      :headers {"Content-Type" "application/edn"}
                      :body (pr-str {:file-path file-path :doc-state doc-state})}))
          (.then (fn [resp] (.text resp)))
          (.then (fn [text]
                   (let [t1 (js/performance.now)
                         result (reader/read-string text)
                         server-ms (:latency-ms result)
                         total-ms (- t1 t0)]
                     (js/console.log "[EDITOR-RAMA] round-trip:" (.toFixed total-ms 1) "ms"
                                     "| server:" server-ms "ms"
                                     "| network:" (.toFixed (- total-ms (or server-ms 0)) 1) "ms"))))
          (.catch (fn [err] (js/console.error "[EDITOR-RAMA] Save failed:" err)))))))

(defn save-flow-state!
  "Persist flow session FSM state to Rama via HTTP. Fire-and-forget."
  [flow-data]
  (when (seq flow-data)
    (-> (js/fetch "/api/flow/save-state"
          (clj->js {:method "POST"
                    :headers {"Content-Type" "application/edn"}
                    :body (pr-str {:data flow-data})}))
        (.then (fn [resp] (.text resp)))
        (.then (fn [_] (js/console.log "[FLOW-HTTP] save-state")))
        (.catch (fn [err] (js/console.error "[FLOW] Save failed:" err))))))

(defn save-agent-trail!
  "Persist a completed agent trail to Rama via HTTP. Fire-and-forget."
  [run-id trail-data]
  (when (and run-id trail-data)
    (let [t0 (js/performance.now)]
      (-> (js/fetch "/api/agent/trail/save"
            (clj->js {:method "POST"
                      :headers {"Content-Type" "application/edn"}
                      :body (pr-str {:run-id run-id :trail-data trail-data})}))
          (.then (fn [resp] (.text resp)))
          (.then (fn [_text]
                   (js/console.log "[TRAIL-HTTP] save round-trip:"
                                   (.toFixed (- (js/performance.now) t0) 1) "ms")))
          (.catch (fn [err] (js/console.error "[TRAIL] Save failed:" err)))))))

(defn emit-settings-update!
  "Submit a settings update to Rama via HTTP. Fire-and-forget — truth
   flows back via Electric subscription, not via HTTP response."
  [settings-data]
  (let [t0 (js/performance.now)]
    (-> (js/fetch "/api/settings/update"
          (clj->js {:method "POST"
                    :headers {"Content-Type" "application/edn"}
                    :body (pr-str {:data settings-data})}))
        (.then (fn [resp] (.text resp)))
        (.then (fn [_text]
                 (js/console.log "[SETTINGS-HTTP] update round-trip:"
                                 (.toFixed (- (js/performance.now) t0) 1) "ms")))
        (.catch (fn [err] (js/console.error "[SETTINGS] Update failed:" err))))))

(defn emit-sidebar-action!
  "Submit a sidebar action to Rama via HTTP. Calls callback with the new committed state."
  [action-type data callback]
  (let [t0 (js/performance.now)]
    (-> (js/fetch "/api/sidebar/action"
          (clj->js {:method "POST"
                    :headers {"Content-Type" "application/edn"}
                    :body (pr-str {:action-type action-type :data data})}))
        (.then (fn [resp] (.text resp)))
        (.then (fn [text]
                 (js/console.log "[SIDEBAR-HTTP]" (str action-type) "round-trip:" (.toFixed (- (js/performance.now) t0) 1) "ms")
                 (let [state (reader/read-string text)]
                   (when callback (callback state)))))
        (.catch (fn [err] (js/console.error "[SIDEBAR] Rama action failed:" err))))))

(defn make-sidebar-io
  "Create sidebar I/O closures. Returns {:fetch-edn! :post-edn! :fetch-home-dirs! :fetch-dir! :fetch-file!}."
  [{:keys [!sidebar-ui !current-file !scroll-x !file-load-request]}]
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
          (when (nil? (:home-dirs @!sidebar-ui))
            (fetch-edn! "/api/home-dirs"
                        (fn [dirs]
                          (swap! !sidebar-ui assoc :home-dirs dirs)))))

        fetch-dir!
        (fn [path]
          (let [ui @!sidebar-ui]
            (when-not (or (contains? (:dir-cache ui) path)
                          (contains? (:in-flight-dirs ui) path))
              (swap! !sidebar-ui update :in-flight-dirs conj path)
              (let [t0 (js/performance.now)]
                (fetch-edn! (str "/api/list-dir?path=" (js/encodeURIComponent path))
                            (fn [entries]
                              (js/console.log "[SIDEBAR-FETCH-DIR]" path ":" (.toFixed (- (js/performance.now) t0) 1) "ms |" (count entries) "entries")
                              (swap! !sidebar-ui (fn [s]
                                                   (-> s
                                                       (update :in-flight-dirs disj path)
                                                       (assoc-in [:dir-cache path] entries)))))
                            (fn [err]
                              (js/console.error "[SIDEBAR] Dir fetch error:" err)
                              (swap! !sidebar-ui update :in-flight-dirs disj path)))))))

        !latest-file-req (atom nil)
        fetch-file!
        (fn [path root-path & {:keys [target-line]}]
          (reset! !latest-file-req path)
          (let [ui @!sidebar-ui]
            (when-not (contains? (:in-flight-files ui) path)
              (swap! !sidebar-ui update :in-flight-files conj path)
              (fetch-edn! (str "/api/read-file?path=" (js/encodeURIComponent path)
                               "&root=" (js/encodeURIComponent root-path))
                          (fn [result]
                            (swap! !sidebar-ui update :in-flight-files disj path)
                            (if (:error result)
                              (js/console.error "[SIDEBAR] File read error:" (:error result))
                              (when (= @!latest-file-req path)
                                (let [lines (str/split-lines (:content result))]
                                  (reset! !current-file {:path path :name (last (str/split path #"/"))})
                                  (reset! !scroll-x 0)
                                  (reset! !file-load-request
                                          (cond-> {:lines lines}
                                            target-line (assoc :target-line target-line)))))))
                          (fn [err]
                            (js/console.error "[SIDEBAR] File read error:" err)
                            (swap! !sidebar-ui update :in-flight-files disj path))))))]
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
  "If initial-file provided, set selected-artifact + current-file and expand sidebar dirs."
  [{:keys [!selected-artifact !current-file !sidebar-truth]} {:keys [fetch-dir!]} initial-file]
  (when initial-file
    (let [file-path (:path initial-file)
          file-name (last (str/split file-path #"/"))
          project-path (:project initial-file)]
      (reset! !selected-artifact {:kind :file :path file-path :name file-name})
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
          (swap! !sidebar-truth assoc
                 :project {:name (last (str/split project-path #"/"))
                           :path project-path}
                 :expanded-dirs (set dir-paths))
          (fetch-dir! project-path)
          (doseq [dp dir-paths]
            (fetch-dir! dp)))))))
