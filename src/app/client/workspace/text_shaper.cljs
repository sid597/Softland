(ns app.client.workspace.text-shaper
  "HarfBuzz + Unicode-bidi provider for Contract T.

   This namespace owns font-program interpretation only. It returns glyph IDs,
   clusters, advances, offsets, extents, directions, and font provenance in
   font units. `text-layout` is the sole owner of material-space placement."
  (:require [clojure.string :as str]
            ["harfbuzzjs/hb.js" :as hb-module]
            ["harfbuzzjs/hbjs.js" :as hbjs-module]
            ["bidi-js" :as bidi-module]))

(def wasm-path "/fonts/harfbuzz-0.10.3.wasm")
(def shaper-id :harfbuzz/wasm)

(defonce ^:private !harfbuzz (atom nil))
(defonce ^:private !harfbuzz-promise (atom nil))

(defn- module-default [module]
  (or (.-default module) module))

(defn- fetch-bytes*
  "One fetch attempt for url → ArrayBuffer."
  [url]
  (-> (js/fetch url)
      (.then (fn [response]
               (when-not (.-ok response)
                 (throw (js/Error. (str "Font resource fetch failed: " url
                                        " (" (.-status response) ")"))))
               (.arrayBuffer response)))))

(defn- fetch-bytes
  "fetch-bytes* with up to 4 attempts and linear backoff — mobile networks
   drop parallel asset fetches wholesale."
  ([url] (fetch-bytes url 1))
  ([url attempt]
   (-> (fetch-bytes* url)
       (.catch (fn [e]
                 (if (< attempt 4)
                   (let [delay-ms (* 600 attempt)]
                     (js/console.warn "[SHAPER/RETRY]" url "attempt" attempt
                                      "failed:" (str e) "— retry in" delay-ms "ms")
                     (js/Promise.
                       (fn [resolve _]
                         (js/setTimeout #(resolve (fetch-bytes url (inc attempt)))
                                        delay-ms))))
                   (throw e)))))))

(defn load-harfbuzz!
  "Instantiate the pinned HarfBuzz WASM once. The explicit wasmBinary keeps
   asset resolution independent of the bundle/script URL. A failed attempt
   clears the promise cache so a later call can retry instead of reusing
   the rejection forever."
  []
  (or @!harfbuzz-promise
      (let [create-hb (module-default hb-module)
            wrap-hb (module-default hbjs-module)
            p (-> (fetch-bytes wasm-path)
                  (.then (fn [wasm]
                           (create-hb #js {:wasmBinary wasm})))
                  (.then (fn [module]
                           (let [hb (wrap-hb module)]
                             (reset! !harfbuzz hb)
                             hb)))
                  (.catch (fn [e]
                            (reset! !harfbuzz-promise nil)
                            (throw e))))]
        (reset! !harfbuzz-promise p)
        p)))

(defn- typed-set [typed-array]
  (into #{} (array-seq typed-array)))

(defn- js-object->map [object]
  (into {}
        (map (fn [key]
               [(keyword key) (aget object key)]))
        (js/Object.keys object)))

(defn- create-face-state [hb {:keys [id revision bytes variations]}]
  (let [blob (.createBlob hb bytes)
        face (.createFace hb blob 0)
        font (.createFont hb face)
        upem (.-upem face)
        axes (js-object->map (.getAxisInfos face))
        variations (or variations {})
        _scale (.setScale font upem upem)
        _variations (when (seq variations)
                      (.setVariations font (clj->js variations)))
        extents (js->clj (.hExtents font) :keywordize-keys true)]
    {:id id
     :revision revision
     :blob blob
     :face face
     :font font
     :upem upem
     :axes axes
     :variations variations
     :unicodes (typed-set (.collectUnicodes face))
     :metrics extents}))

(defn- codepoint-spans [text]
  (loop [offset 0 result []]
    (if (>= offset (.-length text))
      result
      (let [codepoint (.codePointAt text offset)
            width (if (> codepoint 0xFFFF) 2 1)]
        (recur (+ offset width)
               (conj result {:start offset :end (+ offset width)
                             :codepoint codepoint}))))))

(defn- face-for-codepoint [faces codepoint]
  (or (first (filter #(contains? (:unicodes %) codepoint) faces))
      (first faces)))

(defn- faces-by-offset [faces text]
  (reduce
    (fn [result {:keys [start end codepoint]}]
      (let [face (face-for-codepoint faces codepoint)]
        (reduce #(assoc %1 %2 face) result (range start end))))
    (vec (repeat (.-length text) (first faces)))
    (codepoint-spans text)))

(defn- logical-runs [text levels faces]
  (let [n (.-length text)
        faces-at (faces-by-offset faces text)]
    (loop [start 0 result []]
      (if (>= start n)
        result
        (let [level (aget levels start)
              face (nth faces-at start)
              tab? (= "\t" (.charAt text start))
              end (loop [i (inc start)]
                    (if (and (< i n)
                             (= tab? (= "\t" (.charAt text i)))
                             (= level (aget levels i))
                             (identical? face (nth faces-at i)))
                      (recur (inc i))
                      i))]
          (recur end
                 (conj result {:start start :end end :level level
                               :direction (if (odd? level) :rtl :ltr)
                               :face face :tab? tab?})))))))

(defn- visually-order-runs [bidi text embedding runs]
  (if (empty? runs)
    []
    (let [indices (array-seq (.getReorderedIndices bidi text embedding))
          visual-rank (into {} (map-indexed (fn [rank logical]
                                              [logical rank])
                                            indices))]
      (sort-by (fn [{:keys [start end]}]
                 (reduce min (map visual-rank (range start end))))
               runs))))

(defn- glyph-extents [font glyph-id]
  (when-let [extents (.glyphExtents font glyph-id)]
    (js->clj extents :keywordize-keys true)))

(defn- cluster-end-map [glyphs run-end]
  (let [starts (->> glyphs (map :cluster) distinct sort vec)]
    (into {}
          (map-indexed
            (fn [idx start]
              [start (get starts (inc idx) run-end)]))
          starts)))

(defn- shape-run [hb text {:keys [start end direction face]} features language]
  (let [buffer (.createBuffer hb)
        run-text (.slice text start end)
        font (:font face)]
    (.addText buffer run-text)
    (.setClusterLevel buffer 1)
    (.setDirection buffer (name direction))
    (.setLanguage buffer (or language "und"))
    (.guessSegmentProperties buffer)
    (.shape hb font buffer (str/join "," features))
    (let [raw (js->clj (.getGlyphInfosAndPositions buffer)
                       :keywordize-keys true)
          raw (mapv #(update % :cluster + start) raw)
          ends (cluster-end-map raw end)
          glyphs
          (mapv (fn [{:keys [codepoint cluster x_advance y_advance x_offset y_offset]}]
                  {:glyph-id codepoint
                   :glyph-id-kind :font-glyph-index
                   :font-id (:id face)
                   :font-revision (:revision face)
                   :cluster-start cluster
                   :cluster-end (get ends cluster end)
                   :advance [(or x_advance 0) (or y_advance 0)]
                   :offset [(or x_offset 0) (or y_offset 0)]
                   :ink-bounds (glyph-extents font codepoint)
                   :direction direction})
                raw)]
      (.destroy buffer)
      {:source-start start :source-end end
       :direction direction
       :font-id (:id face)
       :font-revision (:revision face)
       :upem (:upem face)
       :glyphs glyphs})))

(defn- tab-run [run advance]
  {:source-start (:start run)
   :source-end (:end run)
   :direction (:direction run)
   :font-id (get-in run [:face :id])
   :font-revision (get-in run [:face :revision])
   :upem (get-in run [:face :upem])
   :glyphs [{:glyph-id nil
             :glyph-id-kind :virtual/tab
             :font-id (get-in run [:face :id])
             :font-revision (get-in run [:face :revision])
             :cluster-start (:start run)
             :cluster-end (:end run)
             :advance [advance 0]
             :offset [0 0]
             :ink-bounds nil
             :direction (:direction run)}]})

(defn- position-runs [runs tab-columns]
  (let [primary-upem (or (:upem (first runs)) 1000)
        tab-width (* (max 1 (or tab-columns 4)) primary-upem 0.5)]
    (loop [remaining runs x 0 positioned []]
      (if (empty? remaining)
        {:runs positioned :advance x}
        (let [run (first remaining)
              run (if (:tab? run)
                    (tab-run run (- (* (inc (long (/ x tab-width))) tab-width) x))
                    run)
              [glyphs next-x]
              (reduce
                (fn [[out pen-x] glyph]
                  (let [[x-advance y-advance] (:advance glyph)
                        [x-offset y-offset] (:offset glyph)]
                    [(conj out (assoc glyph
                                      :position [(+ pen-x x-offset) y-offset]))
                     (+ pen-x x-advance)]))
                [[] x]
                (:glyphs run))]
          (recur (rest remaining) next-x
                 (conj positioned (assoc run :glyphs glyphs))))))))

(defn- cluster-records [runs]
  (->> runs
       (mapcat :glyphs)
       (group-by (juxt :font-revision :cluster-start :cluster-end :direction))
       (map (fn [[[font-revision start end direction] glyphs]]
              (let [left (reduce min (map #(first (:position %)) glyphs))
                    right (reduce max
                                  (map (fn [glyph]
                                         (+ (first (:position glyph))
                                            (first (:advance glyph))))
                                       glyphs))]
                {:source-start start :source-end end
                 :direction direction
                 :font-revision font-revision
                 :left (min left right) :right (max left right)})))
       (sort-by :left)
       vec))

(defn- apply-variations! [faces variations]
  (doseq [face faces]
    (let [supported (set (keys (:axes face)))
          requested (merge (:variations face) variations)
          active (into {} (filter (fn [[axis _]] (contains? supported axis))) requested)]
      (.setVariations (:font face) (clj->js active)))))

(defn- shape-line*
  [hb bidi faces text {:keys [features language tab-columns variations]}]
  (apply-variations! faces variations)
  (if (empty? text)
    {:runs [] :glyphs [] :clusters [] :advance 0}
    (let [embedding (.getEmbeddingLevels bidi text nil)
          levels (.-levels embedding)
          runs (logical-runs text levels faces)
          runs (visually-order-runs bidi text embedding runs)
          shaped (mapv (fn [run]
                         (if (:tab? run)
                           run
                           (shape-run hb text run features language)))
                       runs)
          {:keys [runs advance]} (position-runs shaped tab-columns)]
      {:runs runs
       :glyphs (vec (mapcat :glyphs runs))
       :clusters (cluster-records runs)
       :advance advance
       :base-direction (if (and (pos? (.-length levels))
                                (odd? (aget levels 0)))
                         :rtl :ltr)})))

(defn create-provider
  "Create a synchronous Contract-T provider after HarfBuzz and font bytes are
   loaded. `font-sources` is an ordered primary+fallback vector."
  [hb font-sources {:keys [features language tab-columns]
                    :or {features ["kern" "liga" "clig" "calt"]
                         language "und" tab-columns 4}}]
  (let [bidi ((module-default bidi-module))
        raw-faces (mapv #(create-face-state hb %) font-sources)
        primary-upem (:upem (first raw-faces))
        faces (mapv (fn [face]
                      ;; A fallback run shares the primary font-unit space;
                      ;; layout never reconciles private per-face scales.
                      (.setScale (:font face) primary-upem primary-upem)
                      (assoc face :upem primary-upem))
                    raw-faces)
        primary (first faces)
        version (js-invoke hb "version_string")]
    {:face-id (:id primary)
     :face-revision (:revision primary)
     :shaper-id shaper-id
     :shaper-version version
     :features features
     :variations (:variations primary)
     :axes (:axes primary)
     :fallback-chain (mapv #(select-keys % [:id :revision]) (rest faces))
     :metrics (:metrics primary)
     :upem (:upem primary)
     :shape-line (fn [text opts]
                   (shape-line* hb bidi faces (str (or text ""))
                                (merge {:features features
                                        :language language
                                        :tab-columns tab-columns
                                        :variations (:variations primary)}
                                       opts)))}))

(defn load-provider!
  "Load primary/fallback TTF bytes and return a Promise of a provider. Font
   source maps use {:id :revision :url :variations}."
  [font-sources opts]
  (-> (js/Promise.all
        (clj->js
          (into [(load-harfbuzz!)]
                (map #(fetch-bytes (:url %)) font-sources))))
      (.then
        (fn [values]
          (let [hb (aget values 0)
                sources (mapv (fn [idx source]
                                (assoc source :bytes (aget values (inc idx))))
                              (range) font-sources)]
            (create-provider hb sources opts))))))
