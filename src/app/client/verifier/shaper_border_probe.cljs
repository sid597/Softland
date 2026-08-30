(ns app.client.verifier.shaper-border-probe
  "The shaper WASM-border receipt probe: at a served corpus, how many ms of
   shaping go to the JS/CLJS side of the HarfBuzz border versus HarfBuzz's own
   shape() call. Verifier-only; never a product path.
   Takes: /corpus.json (blocks: uid · text) and the live font manifest, both
   served by test/render_engine/profile_shaper_border.mjs.
   Gives: window.__shaperBorderResult — the serialized adapter attestation
   FIRST, then an ablation ladder: one performance.now() bracket around one
   whole-corpus pass per rung (Chrome clamps the clock to 100µs, so per-call
   timers cannot see a 1µs WASM crossing; a bracket over ~15k lines can).
   Rungs L0–L4c replicate shaper.cljs's border code verbatim on a private
   font; L5 is the real provider's shape-line; L6 the real layout per block.
   Holds: nothing durable."
  (:require [clojure.string :as str]
            [app.client.text.fonts :as fonts]
            [app.client.text.layout :as tl]
            [app.client.text.shaper :as text-shaper]
            ["bidi-js" :as bidi-module]))

(defn- module-default [module]
  (or (.-default module) module))

(def ^:private passes 3)
(def ^:private font-size 19)
(def ^:private line-height 22.8)

(defn- now [] (js/performance.now))

(defn- adapter-attestation [^js adapter]
  (let [info (.-info adapter)
        architecture (some-> info .-architecture)
        native-fallback (some-> info .-isFallbackAdapter)]
    {:is-fallback-adapter (if (some? native-fallback)
                            (boolean native-fallback)
                            (= "swiftshader" architecture))
     :fallback-attestation-source (if (some? native-fallback)
                                    "GPUAdapterInfo.isFallbackAdapter"
                                    "GPUAdapterInfo.architecture=swiftshader")
     :vendor (some-> info .-vendor)
     :architecture architecture
     :device (some-> info .-device)
     :description (some-> info .-description)
     :feature-count (.-size (.-features adapter))}))

(defn- clock-resolution-ms []
  (loop [i 0 last (now) best js/Infinity]
    (if (>= i 50000)
      best
      (let [t (now) d (- t last)]
        (recur (inc i) t (if (pos? d) (min best d) best))))))

(defn- environment []
  {:user-agent js/navigator.userAgent
   :hardware-concurrency js/navigator.hardwareConcurrency
   :device-pixel-ratio js/window.devicePixelRatio
   :viewport [js/window.innerWidth js/window.innerHeight]
   :cross-origin-isolated (boolean js/window.crossOriginIsolated)
   :clock-resolution-ms (clock-resolution-ms)
   :gc-exposed (exists? js/window.gc)
   :memory-api (exists? js/performance.memory)})

(defn- heap-bytes []
  (when (exists? js/performance.memory)
    (.-usedJSHeapSize js/performance.memory)))

(defn- gc! [] (when (exists? js/window.gc) (js/window.gc)))

;; --- the ladder -----------------------------------------------------------

(defn- shape-buffer!
  "Lines 169–177 of shaper.cljs, verbatim: one buffer, shaped."
  [hb font line features]
  (let [buffer (.createBuffer hb)]
    (.addText buffer line)
    (.setClusterLevel buffer 1)
    (.setDirection buffer "ltr")
    (.setLanguage buffer "und")
    (.guessSegmentProperties buffer)
    (.shape hb font buffer features)
    buffer))

(defn- cluster-end-map
  "shaper.cljs :160–166 verbatim."
  [glyphs run-end]
  (let [starts (->> glyphs (map :cluster) distinct sort vec)]
    (into {}
          (map-indexed
            (fn [idx start]
              [start (get starts (inc idx) run-end)]))
          starts)))

(defn- glyph-extents
  "shaper.cljs :156–158 verbatim."
  [font glyph-id]
  (when-let [extents (.glyphExtents font glyph-id)]
    (js->clj extents :keywordize-keys true)))

(defn- glyph-maps
  "shaper.cljs :180–194 verbatim (start = 0, direction :ltr, face fields
   constant)."
  [font raw end]
  (let [raw (mapv #(update % :cluster + 0) raw)
        ends (cluster-end-map raw end)]
    (mapv (fn [{:keys [codepoint cluster x_advance y_advance x_offset y_offset]}]
            {:glyph-id codepoint
             :glyph-id-kind :font-glyph-index
             :font-id "ubuntu-sans-variable"
             :font-revision "probe"
             :cluster-start cluster
             :cluster-end (get ends cluster end)
             :advance [(or x_advance 0) (or y_advance 0)]
             :offset [(or x_offset 0) (or y_offset 0)]
             :ink-bounds (glyph-extents font codepoint)
             :direction :ltr})
          raw)))

;; --- shaper.cljs :102–154 and :221–260, replicated verbatim so the ladder can
;; name the cost of the run split (bidi · codepoint-spans · faces-by-offset ·
;; logical-runs · visually-order-runs) and of positioning (position-runs ·
;; cluster-records) separately from the border sites. Single face, no tabs.

(defn- typed-set [typed-array]
  (into #{} (array-seq typed-array)))

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

(defn- split-runs
  "shape-line* :274–277: embedding levels → logical runs → visual order."
  [bidi faces line]
  (let [embedding (.getEmbeddingLevels bidi line nil)
        levels (.-levels embedding)
        runs (logical-runs line levels faces)]
    (visually-order-runs bidi line embedding runs)))

(defn- position-runs [runs tab-columns]
  (let [primary-upem (or (:upem (first runs)) 1000)
        tab-width (* (max 1 (or tab-columns 4)) primary-upem 0.5)]
    (loop [remaining runs x 0 positioned []]
      (if (empty? remaining)
        {:runs positioned :advance x}
        (let [run (first remaining)
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

(defn- rungs [hb font bidi faces features provider lines blocks]
  (let [features-str (str/join "," features)
        shape-opts {:features features
                    :variations (:variations provider)
                    :language "und"
                    :direction :bidi
                    :tab-columns 4}]
    [{:id "L0-hb-shape"
      :what "createBuffer·addText·setClusterLevel·setDirection·setLanguage·guessSegmentProperties·hb.shape·getLength·destroy"
      :unit :line :items lines
      :f (fn [line]
           (let [b (shape-buffer! hb font line features-str)
                 n (.getLength b)]
             (.destroy b) n))}
     {:id "L1-+getGlyphInfosAndPositions"
      :what "L0 + buffer.getGlyphInfosAndPositions() (raw JS objects; hbjs.js:1186)"
      :unit :line :items lines
      :f (fn [line]
           (let [b (shape-buffer! hb font line features-str)
                 raw (.getGlyphInfosAndPositions b)]
             (.destroy b) (.-length raw)))}
     {:id "L2-+js->clj"
      :what "L1 + (js->clj raw :keywordize-keys true) (shaper.cljs:178)"
      :unit :line :items lines
      :f (fn [line]
           (let [b (shape-buffer! hb font line features-str)
                 raw (js->clj (.getGlyphInfosAndPositions b) :keywordize-keys true)]
             (.destroy b) (count raw)))}
     {:id "L3-+glyphExtents-raw"
      :what "L2 + font.glyphExtents(gid) per glyph, raw JS object kept (hbjs.js:594)"
      :unit :line :items lines
      :f (fn [line]
           (let [b (shape-buffer! hb font line features-str)
                 raw (js->clj (.getGlyphInfosAndPositions b) :keywordize-keys true)
                 ext (mapv #(.glyphExtents font (:codepoint %)) raw)]
             (.destroy b) (count ext)))}
     {:id "L4-+glyphExtents-js->clj"
      :what "L3 + (js->clj extents :keywordize-keys true) per glyph (shaper.cljs:156–158)"
      :unit :line :items lines
      :f (fn [line]
           (let [b (shape-buffer! hb font line features-str)
                 raw (js->clj (.getGlyphInfosAndPositions b) :keywordize-keys true)
                 ext (mapv #(glyph-extents font (:codepoint %)) raw)]
             (.destroy b) (count ext)))}
     {:id "L4b-+glyph-maps"
      :what "L2 + shaper.cljs:180–194 verbatim: update :cluster · cluster-end-map · the 11-key glyph map per glyph (extents inside)"
      :unit :line :items lines
      :f (fn [line]
           (let [b (shape-buffer! hb font line features-str)
                 raw (js->clj (.getGlyphInfosAndPositions b) :keywordize-keys true)
                 glyphs (glyph-maps font raw (.-length line))]
             (.destroy b) (count glyphs)))}
     {:id "L4c-+setVariations-per-line"
      :what "L4b + font.setVariations({wght 400, wdth 100}) before each line (apply-variations!, shaper.cljs:262–267 — bumps HarfBuzz's coord serial per line)"
      :unit :line :items lines
      :f (fn [line]
           (.setVariations font #js {:wght 400 :wdth 100})
           (let [b (shape-buffer! hb font line features-str)
                 raw (js->clj (.getGlyphInfosAndPositions b) :keywordize-keys true)
                 glyphs (glyph-maps font raw (.-length line))]
             (.destroy b) (count glyphs)))}
     {:id "L4d-+bidi+run-split"
      :what "L4c + per line: bidi getEmbeddingLevels · codepoint-spans · faces-by-offset · logical-runs · getReorderedIndices/visually-order-runs (shaper.cljs:102–154, :274–277), then shape as one run"
      :unit :line :items lines
      :f (fn [line]
           (.setVariations font #js {:wght 400 :wdth 100})
           (let [runs (split-runs bidi faces line)
                 b (shape-buffer! hb font line features-str)
                 raw (js->clj (.getGlyphInfosAndPositions b) :keywordize-keys true)
                 glyphs (glyph-maps font raw (.-length line))]
             (.destroy b) (+ (count runs) (count glyphs))))}
     {:id "L4e-+position-runs+cluster-records"
      :what "L4d + position-runs (assoc :position per glyph) · cluster-records (group-by juxt per glyph, sort-by) · (vec (mapcat :glyphs runs)) (shaper.cljs:221–260, :283–286)"
      :unit :line :items lines
      :f (fn [line]
           (.setVariations font #js {:wght 400 :wdth 100})
           (let [split (split-runs bidi faces line)
                 b (shape-buffer! hb font line features-str)
                 raw (js->clj (.getGlyphInfosAndPositions b) :keywordize-keys true)
                 glyphs (glyph-maps font raw (.-length line))
                 _ (.destroy b)
                 shaped [{:source-start 0 :source-end (.-length line)
                          :direction :ltr :font-id "ubuntu-sans-variable"
                          :font-revision "probe" :upem 1000 :glyphs glyphs}]
                 {:keys [runs]} (position-runs shaped 4)
                 all (vec (mapcat :glyphs runs))
                 clusters (cluster-records runs)]
             (+ (count split) (count all) (count clusters))))}
     {:id "L5-shape-line"
      :what "the REAL provider: ((:shape-line provider) line opts) — bidi · runs · shape-run · position-runs · cluster-records (shaper.cljs:269–290)"
      :unit :line :items lines
      :f (fn [line]
           (count (:glyphs ((:shape-line provider) line shape-opts))))}
     {:id "L6-layout-per-block"
      :what "the REAL tl/layout per block, wrap-policy :none (layout.cljc shaped-layout)"
      :unit :block :items blocks
      :f (fn [{:keys [uid text]}]
           (let [result (tl/layout {:text text :provider provider
                                    :font-size font-size
                                    :line-height line-height
                                    :origin [0 0] :baseline-offset font-size
                                    :wrap-policy :none
                                    :source-id uid :source-revision 1
                                    :zoom 1})]
             ;; The result is plane-keyed (layout_planes.cljc): a line's glyphs
             ;; are the [glyph-start, glyph-end) span into the shared plane.
             (reduce (fn [n line] (+ n (- (:glyph-end line) (:glyph-start line))))
                     0 (:lines result))))}]))

(defn- run-pass! [{:keys [f items unit]}]
  (gc!)
  (let [per-item? (= unit :block)
        !per-item (volatile! (transient []))
        heap0 (heap-bytes)
        t0 (now)
        n (reduce (fn [acc item]
                    (if per-item?
                      (let [i0 (now) k (f item) i1 (now)]
                        (vswap! !per-item conj! {:uid (:uid item)
                                                 :chars (.-length (:text item))
                                                 :ms (- i1 i0) :count k})
                        (+ acc k))
                      (+ acc (f item))))
                  0 items)
        t1 (now)
        heap1 (heap-bytes)]
    {:ms (- t1 t0) :count n
     :heap-delta-bytes (when (and heap0 heap1) (- heap1 heap0))
     :per-item (when per-item? (persistent! @!per-item))}))

(defn- median [xs]
  (let [s (vec (sort xs)) n (count s)]
    (if (odd? n) (nth s (quot n 2))
        (/ (+ (nth s (dec (quot n 2))) (nth s (quot n 2))) 2))))

(defn- run-rung! [{:keys [id what unit items] :as rung}]
  (js/console.log "[BORDER] rung" id)
  (let [warm (run-pass! rung)
        timed (vec (repeatedly passes #(run-pass! rung)))
        ms (mapv :ms timed)]
    {:id id :what what :unit (name unit) :items (count items)
     :warmup-ms (:ms warm)
     :pass-ms ms
     :median-ms (median ms)
     :min-ms (reduce min ms)
     :count (:count (first timed))
     :heap-delta-bytes (mapv :heap-delta-bytes timed)
     :per-item (:per-item (last timed))}))

(defn- yield-then [f]
  (js/Promise. (fn [resolve] (js/setTimeout #(resolve (f)) 0))))

(defn- run-ladder! [rungs]
  (reduce (fn [p rung]
            (.then p (fn [acc] (yield-then #(conj acc (run-rung! rung))))))
          (js/Promise.resolve [])
          rungs))

;; --- boot -----------------------------------------------------------------

(defn- fetch-json [url]
  (-> (js/fetch url)
      (.then (fn [r] (when-not (.-ok r) (throw (js/Error. (str "fetch " url " " (.-status r))))) (.json r)))))

(defn- fetch-bytes [url]
  (-> (js/fetch url)
      (.then (fn [r] (when-not (.-ok r) (throw (js/Error. (str "fetch " url " " (.-status r))))) (.arrayBuffer r)))))

(defn- probe-font
  "A private face/font mirroring create-face-state (shaper.cljs:80–100) so
   L0–L4c never touch the provider's state."
  [hb bytes variations]
  (let [blob (.createBlob hb bytes)
        face (.createFace hb blob 0)
        font (.createFont hb face)
        upem (.-upem face)]
    (.setScale font upem upem)
    (.setVariations font (clj->js variations))
    {:font font :face face :upem upem
     :unicodes (typed-set (.collectUnicodes face))}))

(defn- corpus-stats [blocks lines]
  {:blocks (count blocks)
   :lines (count lines)
   :chars-utf16 (reduce + (map #(.-length (:text %)) blocks))
   :max-block-chars (reduce max 0 (map #(.-length (:text %)) blocks))
   :machine-blocks (count (filter :machine blocks))})

(defn ^:export run-probe! []
  (when-not (and (.-isSecureContext js/window) (exists? js/navigator.gpu))
    (throw (js/Error. "probe requires a secure origin with WebGPU")))
  (-> (.requestAdapter js/navigator.gpu)
      (.then
        (fn [^js adapter]
          (when-not adapter (throw (js/Error. "no WebGPU adapter")))
          (let [attestation (adapter-attestation adapter)]
            (js/console.log "[BORDER] adapter" (pr-str attestation))
            (-> (js/Promise.all
                  #js [(fonts/load-font-manifest-async)
                       (fetch-json "/corpus.json")
                       (text-shaper/load-harfbuzz!)])
                (.then
                  (fn [values]
                    (let [manifest (aget values 0)
                          corpus (js->clj (aget values 1) :keywordize-keys true)
                          hb (aget values 2)
                          font-config (first (filter #(= "ubuntu-sans-variable" (:id %))
                                                     (:fonts manifest)))
                          _ (when-not font-config (throw (js/Error. "ubuntu-sans-variable absent")))
                          source (fn [config]
                                   {:id (:id config)
                                    :revision (or (:faceRevision config) (:font config))
                                    :url (str "/fonts/" (:font config))
                                    :variations (or (:variations config) {})})
                          sources (into [(source font-config)] (map source) (:fallbacks font-config))]
                      (-> (js/Promise.all
                            #js [(text-shaper/load-provider!
                                   sources
                                   {:features (:features font-config)
                                    :language "und"
                                    :tab-columns (:tabColumns font-config)})
                                 (fetch-bytes (:url (first sources)))])
                          (.then
                            (fn [loaded]
                              (let [provider (aget loaded 0)
                                    {:keys [font] :as pf} (probe-font hb (aget loaded 1)
                                                                      (:variations font-config))
                                    bidi ((module-default bidi-module))
                                    faces [{:id "ubuntu-sans-variable" :revision "probe"
                                            :font font :upem (:upem pf)
                                            :unicodes (:unicodes pf)}]
                                    blocks (vec corpus)
                                    lines (vec (mapcat #(str/split (:text %) #"\n" -1) blocks))
                                    stats (corpus-stats blocks lines)
                                    env (environment)]
                                (js/console.log "[BORDER] corpus" (pr-str stats))
                                (-> (run-ladder! (rungs hb font bidi faces
                                                        (:features font-config)
                                                        provider lines blocks))
                                    (.then
                                      (fn [ladder]
                                        {:adapter attestation
                                         :environment env
                                         :provider {:shaper-id (str (:shaper-id provider))
                                                    :shaper-version (:shaper-version provider)
                                                    :face-id (:face-id provider)
                                                    :face-revision (:face-revision provider)
                                                    :fallback-chain (:fallback-chain provider)
                                                    :features (:features provider)
                                                    :variations (:variations provider)
                                                    :upem (:upem provider)}
                                         :corpus stats
                                         :passes passes
                                         :layout {:font-size font-size :line-height line-height
                                                  :wrap-policy "none"}
                                         :ladder ladder}))))))))))))))))

(defn ^:export start! []
  (js/console.log "[BORDER] start")
  (set! (.-__shaperBorderDone js/window) false)
  (js/setTimeout
    (fn []
      (try
        (-> (run-probe!)
            (.then (fn [result]
                     (set! (.-__shaperBorderResult js/window) (clj->js result))
                     (set! (.-__shaperBorderDone js/window) true)))
            (.catch (fn [error]
                      (set! (.-__shaperBorderResult js/window)
                            #js {:fatal (str error) :stack (.-stack error)})
                      (set! (.-__shaperBorderDone js/window) true))))
        (catch :default error
          (set! (.-__shaperBorderResult js/window)
                #js {:fatal (str error) :stack (.-stack error)})
          (set! (.-__shaperBorderDone js/window) true))))
    0))
