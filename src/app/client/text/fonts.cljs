(ns app.client.text.fonts
  "Assemble shaping and outline assets from a font configuration.

   Input: /fonts/manifest.json or a selected config. Output: promises of
   manifest/config/assets, including a synchronous layout provider and
   optional complete Slug metadata/curve/band bundle. No persistent state in
   this file. It delegates provider loading to the shaper and joins
   independent asset requests.

   Folder map: README.md."
  (:require [app.client.text.shaper :as text-shaper]))

(def ^:private base-path "/fonts/")

(defn load-font-manifest-async
  "No caller input; fetches /fonts/manifest.json → promise of keywordized
   manifest data.

   Decodes JSON without the HTTP status check or retry used by the asset
   helpers."
  []
  (-> (js/fetch (str base-path "manifest.json"))
      (.then #(.json %))
      (.then #(js->clj % :keywordize-keys true))))

(defn available-fonts
  "Manifest → font configs not explicitly unavailable.

   Filters only :available false."
  [manifest]
  (filterv #(not (false? (:available %))) (:fonts manifest)))

(defn resolve-default-font-config
  "Manifest → first available default, first available font, or hardcoded
   fallback config.

   Explicit fallback order. Fallback config has no asset filenames, so
   selection alone does not ensure usable assets."
  [manifest]
  (let [fonts (available-fonts manifest)]
    (or (first (filter :default fonts))
        (first fonts)
        {:name "DejaVu Sans Mono"
         :id "dejavu-sans-mono"})))

(defn- with-retry
  "Label, promise thunk, optional attempt → promise.

   Up to four attempts, delays 600/1200/1800 ms, logs failures. Intended for
   asynchronous rejection; synchronous thunk throws are outside .catch."
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

(defn- fetch-ok
  "URL → promise of successful response; non-OK throws.

   Checks status before body reading."
  [url]
  (-> (js/fetch url)
      (.then (fn [response]
               (when-not (.-ok response)
                 (throw (js/Error. (str "HTTP " (.-status response) " " url))))
               response))))

(defn- fetch-json
  "URL → retried promise of keywordized JSON.

   Composes status/body/retry."
  [url]
  (with-retry url
    #(-> (fetch-ok url)
         (.then (fn [r] (.json r)))
         (.then (fn [j] (js->clj j :keywordize-keys true))))))

(defn- fetch-bytes
  "URL → retried ArrayBuffer promise.

   Checks HTTP success and reads the body through with-retry."
  [url]
  (with-retry url
    #(-> (fetch-ok url) (.then (fn [r] (.arrayBuffer r))))))

(defn- shaper-source
  "Font config → primary ID/revision/URL/variations or nil.

   Converts asset metadata to shaper vocabulary."
  [font-config]
  (when-let [font-file (:font font-config)]
    {:id (:id font-config)
     :revision (or (:faceRevision font-config) font-file)
     :url (str base-path font-file)
     :variations (or (:variations font-config) {})}))

(defn- shaper-sources
  "Config → ordered primary/fallback source vector.

   Preserves fallback order. Calls primary conversion twice when present."
  [font-config]
  (into (cond-> []
          (shaper-source font-config) (conj (shaper-source font-config)))
        (map (fn [fallback]
               {:id (:id fallback)
                :revision (or (:faceRevision fallback) (:font fallback))
                :url (str base-path (:font fallback))
                :variations (or (:variations fallback) {})}))
        (:fallbacks font-config)))

(defn load-font-assets
  "Config → promise of Slug bundle and layout provider.

   Parallel fetches plus positional result decoding. Partial Slug
   configuration resolves with :slug nil rather than rejecting incomplete
   rendering assets here."
  [font-config]
  (let [slug-config (:slug font-config)
        slug-promises (cond-> []
                        (:meta slug-config)
                        (conj (fetch-json (str base-path (:meta slug-config))))

                        (:curve slug-config)
                        (conj (fetch-bytes (str base-path (:curve slug-config))))

                        (:band slug-config)
                        (conj (fetch-bytes (str base-path (:band slug-config)))))
        sources (shaper-sources font-config)
        shaper-promise (if (seq sources)
                         (text-shaper/load-provider!
                           sources
                           {:features (or (:features font-config)
                                          ["kern" "liga" "clig" "calt"])
                            :language (or (:language font-config) "und")
                            :tab-columns (or (:tabColumns font-config) 4)})
                         (js/Promise.resolve nil))
        asset-promises (conj (vec slug-promises) shaper-promise)]
    (-> (js/Promise.all (clj->js asset-promises))
        (.then
          (fn [assets]
            (let [slug-meta (when (:meta slug-config) (aget assets 0))
                  slug-curve (when (:curve slug-config)
                               (aget assets (if (:meta slug-config) 1 0)))
                  slug-band (when (:band slug-config)
                              (aget assets
                                    (count (filter some? [(:meta slug-config)
                                                          (:curve slug-config)]))))
                  slug-ready? (and slug-meta slug-curve slug-band)
                  layout-provider (aget assets (dec (count asset-promises)))]
              (js/console.log "[FONT] Asset resolution"
                              {:id (:id font-config)
                               :backend :slug
                               :shaper (some-> layout-provider :shaper-id)
                               :has-slug-config? (boolean slug-config)
                               :slug-ready? (boolean slug-ready?)})
              {:id (:id font-config)
               :name (:name font-config)
               :backend :slug
               :layout-provider layout-provider
               :slug (when slug-ready?
                       {:meta slug-meta
                        :curve-bytes slug-curve
                        :band-bytes slug-band})}))))))

(defn load-default-font-data-async
  "No caller input → promise of manifest/config/assets; failure
   logged/rethrown.

   Selects then loads default. Intended for one boot operation."
  []
  (-> (load-font-manifest-async)
      (.then
        (fn [manifest]
          (let [font-config (resolve-default-font-config manifest)]
            (js/console.log "[FONT] Loading default font"
                            {:id (:id font-config)
                             :name (:name font-config)})
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
