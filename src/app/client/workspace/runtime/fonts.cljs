(ns app.client.workspace.runtime.fonts
  "Font manifest helpers and runtime font asset loading."
  (:require [app.client.workspace.text-shaper :as text-shaper]))

(def ^:private base-path "/fonts/")
(declare resolve-default-font-config)

(defn- compact-map [m]
  (into {} (remove (fn [[_ v]] (nil? v)) m)))

(defn manifest-defaults->settings
  "Settings defaults from the font manifest's :settings block."
  [manifest-settings]
  (let [get-default (fn [k fallback]
                      (or (get-in manifest-settings [k :default]) fallback))]
    {:font-size (get-default :fontSize 19)
     :line-height (get-default :lineHeight 1.2)
     :px-range (get-default :pxRange 8)
     :sharpness (get-default :sharpness 0.0)
     :snap-to-pixel? (get-default :snapToPixel true)
     :show-diagnostics? (get-default :showDiagnostics false)
     :theme-id (get-default :theme :gruvbox-dark)}))

(defn font-defaults->settings
  "Settings overrides carried by one font config's :defaults block."
  [font]
  (let [defaults (:defaults font)]
    (when defaults
      (compact-map
        {:font-size (or (:fontSize defaults) (:font-size defaults))
         :line-height (or (:lineHeight defaults) (:line-height defaults))
         :px-range (or (:pxRange defaults) (:px-range defaults))
         :sharpness (or (:sharpness defaults) (:sharpness defaults))
         :snap-to-pixel? (or (:snapToPixel defaults) (:snap-to-pixel? defaults))
         :show-diagnostics? (or (:showDiagnostics defaults) (:show-diagnostics? defaults))}))))

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

;; TEMP mobile-boot probe: per-asset timing + failure logs. Remove with the
;; ?mdbg=1 overlay in resources/public/index.html once the phone boots.
(defn- probe [label p]
  (let [t0 (js/performance.now)]
    (-> p
        (.then (fn [v]
                 (js/console.log "[FONT/PROBE]" label "ok"
                                 (js/Math.round (- (js/performance.now) t0)) "ms")
                 v))
        (.catch (fn [e]
                  (js/console.error "[FONT/PROBE]" label "FAILED" e)
                  (throw e))))))

(defn- with-retry
  "Run thunk (→ promise) with up to 4 attempts and linear backoff. Mobile
   networks drop parallel asset fetches wholesale; one failure must not
   kill the boot."
  ([label thunk] (with-retry label thunk 1))
  ([label thunk attempt]
   (-> (thunk)
       (.catch (fn [e]
                 (if (< attempt 4)
                   (let [delay-ms (* 600 attempt)]
                     (js/console.warn "[FONT/RETRY]" label "attempt" attempt
                                      "failed:" (str e) "— retry in" delay-ms "ms")
                     (js/Promise.
                       (fn [resolve _]
                         (js/setTimeout
                           #(resolve (with-retry label thunk (inc attempt)))
                           delay-ms))))
                   (throw e)))))))

(defn- fetch-ok [url]
  (-> (js/fetch url)
      (.then (fn [response]
               (when-not (.-ok response)
                 (throw (js/Error. (str "HTTP " (.-status response) " " url))))
               response))))

(defn- fetch-json [url]
  (probe (str "json " url)
         (with-retry url
           #(-> (fetch-ok url)
                (.then (fn [r] (.json r)))
                (.then (fn [j] (js->clj j :keywordize-keys true)))))))

(defn- fetch-bitmap [url]
  (-> (probe (str "blob " url)
             (with-retry url
               #(-> (fetch-ok url) (.then (fn [r] (.blob r))))))
      (.then (fn [blob]
               (probe (str "decode " url) (js/createImageBitmap blob))))))

(defn- fetch-bytes [url]
  (probe (str "bytes " url)
         (with-retry url
           #(-> (fetch-ok url) (.then (fn [r] (.arrayBuffer r)))))))

(defn- shaper-source [font-config]
  (when-let [font-file (:font font-config)]
    {:id (:id font-config)
     :revision (or (:faceRevision font-config) font-file)
     :url (str base-path font-file)
     :variations (or (:variations font-config) {})}))

(defn- shaper-sources [font-config]
  (into (cond-> []
          (shaper-source font-config) (conj (shaper-source font-config)))
        (map (fn [fallback]
               {:id (:id fallback)
                :revision (or (:faceRevision fallback) (:font fallback))
                :url (str base-path (:font fallback))
                :variations (or (:variations fallback) {})}))
        (:fallbacks font-config)))

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
                        (conj (fetch-bytes (str base-path (:band slug-config)))))
        sources (shaper-sources font-config)
        shaper-promise (if (seq sources)
                         (probe "shaper load-provider!"
                                (text-shaper/load-provider!
                                  sources
                                  {:features (or (:features font-config)
                                                 ["kern" "liga" "clig" "calt"])
                                   :language (or (:language font-config) "und")
                                   :tab-columns (or (:tabColumns font-config) 4)}))
                         (js/Promise.resolve nil))
        asset-promises (vec (concat msdf-promises slug-promises
                                    [shaper-promise]))]
    (-> (js/Promise.all (clj->js asset-promises))
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
                  layout-provider (aget assets (dec (count asset-promises)))
                  preferred-backend (keyword (or (:preferredBackend font-config) "msdf"))
                  active-backend (if (and (= preferred-backend :slug) slug-ready?)
                                   :slug
                                   :msdf)]
              (js/console.log "[FONT] Asset resolution"
                              {:id (:id font-config)
                               :preferred-backend preferred-backend
                               :active-backend active-backend
                               :shaper (some-> layout-provider :shaper-id)
                               :has-msdf? (boolean (and bitmap atlas))
                               :has-slug-config? (boolean slug-config)
                               :slug-ready? (boolean slug-ready?)})
              {:id (:id font-config)
               :name (:name font-config)
               :backend active-backend
               :layout-provider layout-provider
               :atlas-faces (:atlasFaces font-config)
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
                     :font-assets font-assets}))))))
      (.catch
        (fn [e]
          (js/console.error "[FONT] Default font load FAILED — boot cannot continue" e)
          (throw e)))))

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
                    (reset! !font-assets assets)
                    ;; Same-id enrichment does not trigger another async load;
                    ;; it makes the one provider visible to layout/caret/hit.
                    (swap! !active-font assoc
                           :layout-provider (:layout-provider assets))))
                (.catch
                  (fn [err]
                    (js/console.error "[FONT] Failed to load:" err))))))))))
