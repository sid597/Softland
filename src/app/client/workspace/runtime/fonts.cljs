(ns app.client.workspace.runtime.fonts
  "Font manifest helpers and runtime font asset loading."
  (:require [app.client.workspace.settings-view :refer [font-defaults->settings]]))

(def ^:private base-path "/fonts/")
(declare resolve-default-font-config)

(defn load-font-manifest-async []
  "Load the font manifest from the fonts directory."
  (-> (js/fetch (str base-path "manifest.json"))
      (.then #(.json %))
      (.then #(js->clj % :keywordize-keys true))
      (.then (fn [manifest]
               (js/console.log "[FONT] Manifest loaded"
                               {:font-count (count (:fonts manifest))
                                :default-font (:id (resolve-default-font-config manifest))
                                :settings-keys (keys (:settings manifest))})
               manifest))
      (.catch
        (fn [e]
          (js/console.error "[FONT] Manifest load failed, using fallback manifest" e)
          {:fonts [{:name "Ubuntu Sans Mono"
                    :id "ubuntu-sans-mono"
                    :atlas "ubuntu_sans_mono_atlas.png"
                    :metrics "ubuntu_sans_mono_atlas.json"
                    :charWidth 0.56
                    :default true
                    :defaults {:fontSize 19
                               :lineHeight 1.2
                               :pxRange 8
                               :sharpness 0.0
                               :snapToPixel true
                               :showDiagnostics false}}]
           :settings {:fontSize {:default 19}
                      :lineHeight {:default 1.2}
                      :pxRange {:default 8}
                      :sharpness {:default 0.0}
                      :snapToPixel {:default true}
                      :showDiagnostics {:default false}}}))))

(defn available-fonts [manifest]
  (filterv #(not (false? (:available %))) (:fonts manifest)))

(defn resolve-default-font-config [manifest]
  (let [fonts (available-fonts manifest)]
    (or (first (filter :default fonts))
        (first fonts)
        {:name "DejaVu Sans Mono"
         :id "dejavu-sans-mono"
         :charWidth 0.56})))

(defn- fetch-json [url]
  (-> (js/fetch url)
      (.then #(.json %))
      (.then #(js->clj % :keywordize-keys true))))

(defn- fetch-bitmap [url]
  (-> (js/fetch url)
      (.then #(.blob %))
      (.then #(js/createImageBitmap %))))

(defn- fetch-bytes [url]
  (-> (js/fetch url)
      (.then #(.arrayBuffer %))))

(defn load-font-assets
  "Load the runtime assets for a font config. Slug-enabled fonts still load
   their MSDF bundle so the old path remains available as a fallback."
  [font-config]
  (let [msdf-promises (cond-> []
                        (:atlas font-config)
                        (conj (fetch-bitmap (str base-path (:atlas font-config))))

                        (:metrics font-config)
                        (conj (fetch-json (str base-path (:metrics font-config)))))
        slug-config (:slug font-config)
        slug-promises (cond-> []
                        (:meta slug-config)
                        (conj (fetch-json (str base-path (:meta slug-config))))

                        (:curve slug-config)
                        (conj (fetch-bytes (str base-path (:curve slug-config))))

                        (:band slug-config)
                        (conj (fetch-bytes (str base-path (:band slug-config)))))]
    (-> (js/Promise.all (clj->js (concat msdf-promises slug-promises)))
        (.then
          (fn [assets]
            (let [msdf-asset-count (count msdf-promises)
                  bitmap (when (:atlas font-config) (aget assets 0))
                  atlas (when (:metrics font-config)
                          (aget assets (if (:atlas font-config) 1 0)))
                  slug-start msdf-asset-count
                  slug-meta (when (:meta slug-config) (aget assets slug-start))
                  slug-curve (when (:curve slug-config) (aget assets (+ slug-start (if (:meta slug-config) 1 0))))
                  slug-band (when (:band slug-config)
                              (aget assets (+ slug-start
                                              (count (filter some? [(:meta slug-config) (:curve slug-config)]))))
                              )
                  slug-ready? (and slug-meta slug-curve slug-band)
                  preferred-backend (keyword (or (:preferredBackend font-config) "msdf"))
                  active-backend (if (and (= preferred-backend :slug) slug-ready?)
                                   :slug
                                   :msdf)]
              (js/console.log "[FONT] Asset resolution"
                              {:id (:id font-config)
                               :preferred-backend preferred-backend
                               :active-backend active-backend
                               :has-msdf? (boolean (and bitmap atlas))
                               :has-slug-config? (boolean slug-config)
                               :slug-ready? (boolean slug-ready?)})
              {:id (:id font-config)
               :name (:name font-config)
               :backend active-backend
               :bitmap bitmap
               :atlas atlas
               :msdf (when (and bitmap atlas)
                       {:bitmap bitmap :atlas atlas})
               :slug (when slug-ready?
                       {:meta slug-meta
                        :curve-bytes slug-curve
                        :band-bytes slug-band})}))))))

(defn load-default-font-data-async []
  (-> (load-font-manifest-async)
      (.then
        (fn [manifest]
          (let [font-config (resolve-default-font-config manifest)]
            (js/console.log "[FONT] Loading default font"
                            {:id (:id font-config)
                             :name (:name font-config)
                             :preferred-backend (:preferredBackend font-config)})
            (-> (load-font-assets font-config)
                (.then
                  (fn [font-assets]
                    (js/console.log "[FONT] Default font ready"
                                    {:id (:id font-config)
                                     :backend (:backend font-assets)})
                    {:font-manifest manifest
                     :font-config font-config
                     :font-assets font-assets}))))))))

(defn install-font-watch!
  "Watch !active-font for id changes; apply defaults and async-load new font assets."
  [{:keys [!active-font !font-manifest !font-assets !settings]}]
  (add-watch !active-font :font-loader
    (fn [_ _ old-val new-val]
      (when (not= (:id old-val) (:id new-val))
        (let [manifest @!font-manifest
              font-config (first (filter #(= (:id %) (:id new-val)) (:fonts manifest)))]
          (js/console.log "[FONT] Loading font:" (:id new-val) font-config)
          (when font-config
            (swap! !settings assoc :font-id (:id font-config))
            (when-let [defaults (font-defaults->settings font-config)]
              (swap! !settings merge defaults))
            (-> (load-font-assets font-config)
                (.then
                  (fn [assets]
                    (js/console.log "[FONT] Loaded assets for:" (:id new-val) "backend=" (name (:backend assets)))
                    (reset! !font-assets assets)))
                (.catch
                  (fn [err]
                    (js/console.error "[FONT] Failed to load:" err))))))))))
