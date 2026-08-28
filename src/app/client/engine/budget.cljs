(ns app.client.engine.budget
  "A byte ledger for GPU buffers and textures, by label and kind, reported to
   the console.
   Takes: adapter limits to create a tracker; a GPU object with its label and
   reserved or active bytes; an object to destroy.
   Gives: a snapshot of bytes per label and kind; a summary line; a startup
   report.
   Holds: the tracker's state atom and a WeakMap from GPU object to id."
  (:require [clojure.string :as str]))

(def ^:private max-events 200)
(def ^:private warn-threshold 0.8)

(defn- now-ms []
  (.now js/Date))

(defn- clamp-bytes [n]
  (max 0 (long (or n 0))))

(defn format-bytes [n]
  (let [n (double (clamp-bytes n))
        kb 1024.0
        mb (* kb 1024.0)
        gb (* mb 1024.0)]
    (cond
      (>= n gb) (str (.toFixed (/ n gb) 2) " GB")
      (>= n mb) (str (.toFixed (/ n mb) 2) " MB")
      (>= n kb) (str (.toFixed (/ n kb) 2) " KB")
      :else (str (long n) " B"))))

(defn- pct-str [ratio]
  (when (number? ratio)
    (str (.toFixed (* 100.0 ratio) 1) "%")))

(defn snapshot-adapter-limits [^js adapter]
  (when adapter
    (let [limits (.-limits adapter)]
      {:max-buffer-size (some-> limits .-maxBufferSize)
       :max-storage-buffer-binding-size (some-> limits .-maxStorageBufferBindingSize)
       :max-texture-dimension-2d (some-> limits .-maxTextureDimension2D)
       :max-texture-array-layers (some-> limits .-maxTextureArrayLayers)})))

(defn create-tracker [adapter-limits]
  {:ids (js/WeakMap.)
   :state (atom {:limits (or adapter-limits {})
                 :resources {}
                 :events []
                 :next-id 1
                 :startup-logged? false})})

(defn- next-id! [tracker]
  (let [!id (volatile! nil)]
    (swap! (:state tracker)
           (fn [state]
             (vreset! !id (:next-id state))
             (update state :next-id inc)))
    @!id))

(defn- lookup-id [tracker obj]
  (when (and tracker obj)
    (.get ^js (:ids tracker) obj)))

(defn- lookup-resource [tracker obj]
  (when-let [id (lookup-id tracker obj)]
    (get-in @(:state tracker) [:resources id])))

(defn- push-event! [tracker event]
  (when tracker
    (swap! (:state tracker)
           (fn [state]
             (let [events (conj (:events state) (assoc event :ts (now-ms)))
                   trimmed (if (> (count events) max-events)
                             (subvec (vec events) (- (count events) max-events))
                             (vec events))]
               (assoc state :events trimmed))))))

(defn- buffer-limit-ratio [limits reserved-bytes]
  (when-let [max-buffer-size (:max-buffer-size limits)]
    (when (pos? max-buffer-size)
      (/ reserved-bytes max-buffer-size))))

(defn- texture-limit-ratio [limits {:keys [width height depth-or-array-layers]}]
  (let [max-dim (:max-texture-dimension-2d limits)
        max-layers (:max-texture-array-layers limits)
        ratios (cond-> []
                 (and max-dim (pos? max-dim))
                 (into [(/ (or width 0) max-dim)
                        (/ (or height 0) max-dim)])

                 (and max-layers (pos? max-layers) depth-or-array-layers)
                 (conj (/ depth-or-array-layers max-layers)))]
    (when (seq ratios)
      (apply max ratios))))

(defn- limit-note [limits {:keys [kind reserved-bytes details]}]
  (case kind
    :buffer
    (some-> (buffer-limit-ratio limits reserved-bytes) pct-str (str "maxBuffer " ))

    :texture
    (some-> (texture-limit-ratio limits details) pct-str (str "maxTextureDim " ))

    nil))

(defn- maybe-warn! [tracker resource]
  (let [limits (:limits @(:state tracker))
        ratio (case (:kind resource)
                :buffer (buffer-limit-ratio limits (:reserved-bytes resource))
                :texture (texture-limit-ratio limits (:details resource))
                nil)]
    (when (and ratio (>= ratio warn-threshold))
      (js/console.warn
        (str "[GPU-BUDGET] " (:label resource) " is at "
             (pct-str ratio) " of the relevant WebGPU limit.")))))

(defn- log-replacement! [tracker old-resource new-resource reason]
  (let [limits (:limits @(:state tracker))
        note (limit-note limits new-resource)
        parts (cond-> [(str "[GPU-BUDGET] " (:label new-resource) " "
                            (name (:kind new-resource)) " "
                            (or (some-> reason name) "replace")
                            ": " (format-bytes (:reserved-bytes old-resource))
                            " -> " (format-bytes (:reserved-bytes new-resource)))]
                (number? (:active-bytes new-resource))
                (conj (str "active " (format-bytes (:active-bytes new-resource))))
                note
                (conj note))]
    (js/console.log (str/join " | " parts))
    (maybe-warn! tracker new-resource)))

(defn- resource-map [kind label reserved-bytes active-bytes details replacement-of]
  (let [reserved-bytes (clamp-bytes reserved-bytes)
        active-bytes (when (some? active-bytes) (clamp-bytes active-bytes))]
    {:kind kind
     :label label
     :reserved-bytes reserved-bytes
     :active-bytes active-bytes
     :details details
     :replacement-of replacement-of
     :created-at (now-ms)
     :updated-at (now-ms)}))

(defn- install-resource! [tracker obj resource]
  (let [id (next-id! tracker)]
    (.set ^js (:ids tracker) obj id)
    (swap! (:state tracker) assoc-in [:resources id] (assoc resource :id id))
    (assoc resource :id id)))

(defn register-buffer!
  [tracker obj label reserved-bytes & {:keys [active-bytes details]}]
  (when (and tracker obj)
    (let [resource (resource-map :buffer label reserved-bytes active-bytes details nil)]
      (push-event! tracker {:type :create :kind :buffer :label label :bytes (clamp-bytes reserved-bytes)})
      (install-resource! tracker obj resource))))

(defn texture-reserved-bytes
  "Price every allocated mip level and MSAA sample, not only level zero."
  ([width height depth-or-array-layers bytes-per-pixel mip-level-count]
   (texture-reserved-bytes width height depth-or-array-layers bytes-per-pixel
                           mip-level-count 1))
  ([width height depth-or-array-layers bytes-per-pixel mip-level-count
    sample-count]
   (let [depth (max 1 (or depth-or-array-layers 1))
         levels (max 1 (or mip-level-count 1))
         samples (max 1 (or sample-count 1))]
     (reduce + 0
             (map (fn [level]
                    (* (max 1 (quot (max 1 (or width 1))
                                    (bit-shift-left 1 level)))
                       (max 1 (quot (max 1 (or height 1))
                                    (bit-shift-left 1 level)))
                       depth bytes-per-pixel samples))
                  (range levels))))))

(defn register-texture!
  [tracker obj label & {:keys [width height depth-or-array-layers format
                               mip-level-count active-bytes details]}]
  (when (and tracker obj)
    (let [details (merge {:width width
                          :height height
                          :depth-or-array-layers (or depth-or-array-layers 1)
                          :format format
                          :mip-level-count (or mip-level-count 1)}
                         details)
          bytes-per-pixel (case format
                            ("rgba8unorm" "bgra8unorm" "rgba8unorm-srgb" "bgra8unorm-srgb") 4
                            "rgba16float" 8
                            "rg16uint" 4
                            4)
          reserved-bytes (texture-reserved-bytes
                          width height depth-or-array-layers bytes-per-pixel
                          mip-level-count (or (:sample-count details)
                                              (:sampleCount details) 1))
          resource (resource-map :texture label reserved-bytes
                                 (or active-bytes reserved-bytes) details nil)]
      (push-event! tracker {:type :create :kind :texture :label label :bytes reserved-bytes})
      (install-resource! tracker obj resource))))

(defn replace-buffer!
  [tracker old-obj new-obj label reserved-bytes & {:keys [active-bytes details reason]}]
  (when (and tracker new-obj)
    (let [old-resource (lookup-resource tracker old-obj)
          new-resource (resource-map :buffer label reserved-bytes active-bytes details (:id old-resource))]
      (when old-obj
        (.delete ^js (:ids tracker) old-obj))
      (when old-resource
        (swap! (:state tracker) update :resources dissoc (:id old-resource)))
      (let [installed (install-resource! tracker new-obj new-resource)]
        (push-event! tracker {:type :replace
                              :kind :buffer
                              :label label
                              :old-bytes (:reserved-bytes old-resource)
                              :new-bytes (:reserved-bytes installed)
                              :reason reason})
        (when old-resource
          (log-replacement! tracker old-resource installed reason))
        installed))))

(defn replace-texture!
  [tracker old-obj new-obj label
   & {:keys [width height depth-or-array-layers format mip-level-count
             active-bytes details reason]}]
  (when (and tracker new-obj)
    (let [old-resource (lookup-resource tracker old-obj)
          details (merge {:width width
                          :height height
                          :depth-or-array-layers (or depth-or-array-layers 1)
                          :format format
                          :mip-level-count (or mip-level-count 1)}
                         details)
          bytes-per-pixel (case format
                            ("rgba8unorm" "bgra8unorm" "rgba8unorm-srgb" "bgra8unorm-srgb") 4
                            "rgba16float" 8
                            "rg16uint" 4
                            4)
          reserved-bytes (texture-reserved-bytes
                          width height depth-or-array-layers bytes-per-pixel
                          mip-level-count (or (:sample-count details)
                                              (:sampleCount details) 1))
          new-resource (resource-map :texture label reserved-bytes
                                     (or active-bytes reserved-bytes)
                                     details
                                     (:id old-resource))]
      (when old-obj
        (.delete ^js (:ids tracker) old-obj))
      (when old-resource
        (swap! (:state tracker) update :resources dissoc (:id old-resource)))
      (let [installed (install-resource! tracker new-obj new-resource)]
        (push-event! tracker {:type :replace
                              :kind :texture
                              :label label
                              :old-bytes (:reserved-bytes old-resource)
                              :new-bytes (:reserved-bytes installed)
                              :reason reason})
        (when old-resource
          (log-replacement! tracker old-resource installed reason))
        installed))))

(defn destroy-resource!
  [tracker obj & {:keys [reason]}]
  (when-let [id (lookup-id tracker obj)]
    (when-let [resource (get-in @(:state tracker) [:resources id])]
      (.delete ^js (:ids tracker) obj)
      (swap! (:state tracker) update :resources dissoc id)
      (push-event! tracker {:type :destroy
                            :kind (:kind resource)
                            :label (:label resource)
                            :bytes (:reserved-bytes resource)
                            :reason reason})
      resource)))

(defn set-active-bytes!
  [tracker obj active-bytes]
  (when-let [id (lookup-id tracker obj)]
    (swap! (:state tracker)
           (fn [state]
             (if-let [resource (get-in state [:resources id])]
               (assoc-in state [:resources id]
                         (assoc resource
                                :active-bytes (clamp-bytes active-bytes)
                                :updated-at (now-ms)))
               state)))))

(defn snapshot [tracker]
  (let [{:keys [limits resources events]} (or (some-> tracker :state deref) {})
        entries (vals (or resources {}))
        by-label (->> entries
                      (reduce (fn [acc {:keys [label kind reserved-bytes active-bytes]}]
                                (update acc label
                                        (fn [entry]
                                          (let [entry (or entry {:label label
                                                                 :resource-count 0
                                                                 :reserved-bytes 0
                                                                 :active-bytes 0
                                                                 :kinds #{}})]
                                            (-> entry
                                                (update :resource-count inc)
                                                (update :reserved-bytes + reserved-bytes)
                                                (update :active-bytes + (or active-bytes 0))
                                                (update :kinds conj kind))))))
                              {})
                      vals
                      (sort-by :reserved-bytes >)
                      vec)
        total-reserved-bytes (reduce + 0 (map :reserved-bytes entries))
        total-active-bytes (reduce + 0 (map #(or (:active-bytes %) 0) entries))
        buffer-reserved-bytes (reduce + 0 (map :reserved-bytes (filter #(= :buffer (:kind %)) entries)))
        texture-reserved-bytes (reduce + 0 (map :reserved-bytes (filter #(= :texture (:kind %)) entries)))
        largest-buffer (first (sort-by :reserved-bytes > (filter #(= :buffer (:kind %)) entries)))
        largest-texture (first (sort-by :reserved-bytes > (filter #(= :texture (:kind %)) entries)))]
    {:limits (or limits {})
     :resources (vec entries)
     :events (or events [])
     :by-label by-label
     :total-reserved-bytes total-reserved-bytes
     :total-active-bytes total-active-bytes
     :buffer-reserved-bytes buffer-reserved-bytes
     :texture-reserved-bytes texture-reserved-bytes
     :largest-buffer largest-buffer
     :largest-texture largest-texture}))

(defn summary-line [tracker]
  (when tracker
    (let [{:keys [limits total-reserved-bytes buffer-reserved-bytes
                  texture-reserved-bytes largest-buffer]} (snapshot tracker)
          max-buffer-note (when largest-buffer
                            (some-> (buffer-limit-ratio limits (:reserved-bytes largest-buffer))
                                    pct-str
                                    (str "largest " (:label largest-buffer) " ")))
          parts (cond-> [(str "gpu: " (format-bytes total-reserved-bytes))
                         (str "buf " (format-bytes buffer-reserved-bytes))
                         (str "tex " (format-bytes texture-reserved-bytes))]
                  max-buffer-note
                  (conj max-buffer-note))]
      (str/join " | " parts))))

(defn log-startup-report! [tracker]
  (when tracker
    (let [!should-log? (volatile! false)]
      (swap! (:state tracker)
             (fn [state]
               (if (:startup-logged? state)
                 state
                 (do (vreset! !should-log? true)
                     (assoc state :startup-logged? true)))))
      (when @!should-log?
        (let [{:keys [limits by-label total-reserved-bytes total-active-bytes]} (snapshot tracker)
              rows (mapv (fn [{:keys [label resource-count reserved-bytes active-bytes kinds]}]
                           {:subsystem label
                            :resources resource-count
                            :kinds (->> kinds (map name) sort (str/join ", "))
                            :reserved (format-bytes reserved-bytes)
                            :active (format-bytes active-bytes)})
                         by-label)]
          (js/console.groupCollapsed
            (str "[GPU-BUDGET] Startup report | reserved "
                 (format-bytes total-reserved-bytes)
                 " | active " (format-bytes total-active-bytes)))
          (js/console.log "[GPU-BUDGET] Adapter limits:" (clj->js limits))
          (when (seq rows)
            (js/console.table (clj->js rows)))
          (js/console.groupEnd))))))
