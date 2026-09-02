(ns app.client.text.shaper
  "Text shaping with HarfBuzz: turns a string plus font programs into glyph
   ids, clusters, advances, offsets, and extents in font units, with bidi
   direction — as columns. The flat route: HarfBuzz's glyph structs are read
   straight out of the WASM heap into one shaped line (`shaped-line`), one
   crossing in (`addText`) and one crossing out (the two heap views) per run;
   no object per glyph.
   Takes: ordered font sources (primary and fallbacks, with variations) and
   shaping options.
   Gives: a promise of a provider, the handle text layout calls to shape runs.
   Holds: the loaded HarfBuzz wasm module, its hbjs wrapper, and the in-flight
   load promise."
  (:require [clojure.string :as str]
            ["harfbuzzjs/hb.js" :as hb-module]
            ["harfbuzzjs/hbjs.js" :as hbjs-module]
            ["bidi-js" :as bidi-module]
            [app.client.text.shaped-line :as sl]))

(def wasm-path "/fonts/harfbuzz-0.10.3.wasm")
(def shaper-id :harfbuzz/wasm)

(defonce ^:private !harfbuzz (atom nil))
(defonce ^:private !harfbuzz-module (atom nil))
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
  "Instantiate the pinned HarfBuzz WASM once; resolves to the hbjs wrapper.
   The explicit wasmBinary keeps asset resolution independent of the
   bundle/script URL. A failed attempt clears the promise cache so a later
   call can retry instead of reusing the rejection forever. The raw
   Emscripten module (heap views + exports) is kept beside the wrapper for
   the flat route."
  []
  (or @!harfbuzz-promise
      (let [create-hb (module-default hb-module)
            wrap-hb (module-default hbjs-module)
            p (-> (fetch-bytes wasm-path)
                  (.then (fn [wasm]
                           (create-hb #js {:wasmBinary wasm})))
                  (.then (fn [module]
                           (let [hb (wrap-hb module)]
                             (reset! !harfbuzz-module module)
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
        extents (js->clj (.hExtents font) :keywordize-keys true)
        unicodes (.collectUnicodes face)]
    {:id id
     :revision revision
     :blob blob
     :face face
     :font font
     :upem upem
     :axes axes
     :variations variations
     :unicodes (typed-set unicodes)
     ;; the flat route's membership test — one JS Set, no CLJS hashing per char
     :unicode-set (js/Set. unicodes)
     :metrics extents}))

;; ---------------------------------------------------------------------------
;; The flat route: run split over arrays, shaping into columns.

(defn- face-index-at
  "One byte per UTF-16 code unit: the index of the first face covering that
   code point (the primary when none does), exactly `faces-by-offset`."
  [faces text n]
  (let [out (js/Uint8Array. n)
        face-count (count faces)
        sets (to-array (map :unicode-set faces))]
    (loop [offset 0]
      (when (< offset n)
        (let [codepoint (.codePointAt text offset)
              width (if (> codepoint 0xFFFF) 2 1)
              face (loop [f 0]
                     (cond (>= f face-count) 0
                           (.has (aget sets f) codepoint) f
                           :else (recur (inc f))))]
          (aset out offset face)
          (when (= width 2) (aset out (inc offset) face))
          (recur (+ offset width)))))
    out))

(defn- logical-runs
  "Maximal runs of one bidi level, one face, and one tab-ness, in logical
   order — small maps, a handful per line."
  [text levels face-at n]
  (loop [start 0 result (transient [])]
    (if (>= start n)
      (persistent! result)
      (let [level (aget levels start)
            face (aget face-at start)
            tab? (= 9 (.charCodeAt text start))
            end (loop [i (inc start)]
                  (if (and (< i n)
                           (= tab? (= 9 (.charCodeAt text i)))
                           (= level (aget levels i))
                           (= face (aget face-at i)))
                    (recur (inc i))
                    i))]
        (recur end (conj! result {:start start :end end :level level
                                  :face face :tab? tab?}))))))

(defn- visually-order-runs [bidi text embedding runs n]
  (if (empty? runs)
    []
    (let [indices (.getReorderedIndices bidi text embedding)
          rank (js/Int32Array. n)]
      (dotimes [r (.-length indices)]
        (aset rank (aget indices r) r))
      (let [ranked (mapv (fn [{:keys [start end] :as run}]
                           (assoc run :rank
                                  (loop [i start best js/Infinity]
                                    (if (>= i end)
                                      best
                                      (recur (inc i) (min best (aget rank i)))))))
                         runs)]
        (vec (sort-by :rank ranked))))))

(defn- apply-variations! [faces variations]
  (doseq [face faces]
    (let [supported (set (keys (:axes face)))
          requested (merge (:variations face) variations)
          active (into {} (filter (fn [[axis _]] (contains? supported axis))) requested)]
      (.setVariations (:font face) (clj->js active)))))

(defn- next-cluster-end
  "The next distinct cluster start after `cluster` in the run's sorted,
   deduplicated starts, else the run's end — `cluster-end-map`'s rule."
  [starts start-count cluster run-end]
  (loop [lo 0 hi start-count]
    (if (< lo hi)
      (let [mid (bit-shift-right (+ lo hi) 1)]
        (if (<= (aget starts mid) cluster)
          (recur (inc mid) hi)
          (recur lo mid)))
      (if (< lo start-count) (aget starts lo) run-end))))

(defn- shape-line-flat
  [hb ^js module bidi faces face-meta scratch text
   {:keys [features language tab-columns variations]}]
  (apply-variations! faces variations)
  (let [n (.-length text)]
    (if (zero? n)
      (sl/empty-line face-meta)
      (let [embedding (.getEmbeddingLevels bidi text nil)
            levels (.-levels embedding)
            face-at (face-index-at faces text n)
            runs (visually-order-runs bidi text embedding
                                      (logical-runs text levels face-at n) n)
            run-count (count runs)
            ^js exports (.-wasmExports module)
            features-str (str/join "," features)
            language (or language "und")
            ;; crossing in: one buffer per non-tab run, shaped; lengths read
            buffers (js/Array. run-count)
            lengths (js/Int32Array. run-count)
            total (loop [r 0 total 0]
                    (if (>= r run-count)
                      total
                      (let [{:keys [start end level tab? face]} (nth runs r)]
                        (if tab?
                          (do (aset lengths r 1)
                              (recur (inc r) (inc total)))
                          (let [buffer (.createBuffer hb)
                                font (:font (nth faces face))]
                            (.addText buffer (.slice text start end))
                            (.setClusterLevel buffer 1)
                            (.setDirection buffer (if (odd? level) "rtl" "ltr"))
                            (.setLanguage buffer language)
                            (.guessSegmentProperties buffer)
                            (.shape hb font buffer features-str)
                            (let [len (.hb_buffer_get_length exports (.-ptr ^js buffer))]
                              (aset buffers r buffer)
                              (aset lengths r len)
                              (recur (inc r) (+ total len))))))))
            line (sl/make-line total run-count face-meta)
            gid-col (:glyph-id line)
            cs-col (:cluster-start line)
            ce-col (:cluster-end line)
            ax-col (:advance-x line)
            ay-col (:advance-y line)
            ox-col (:offset-x line)
            oy-col (:offset-y line)
            gx-col (:glyph-x line)
            gy-col (:glyph-y line)
            ix-col (:ink-x line)
            iy-col (:ink-y line)
            iw-col (:ink-w line)
            ih-col (:ink-h line)
            run-col (:run-index line)
            primary-upem (or (:upem (first face-meta)) 1000)
            tab-width (* (max 1 (or tab-columns 4)) primary-upem 0.5)
            scratch-index (/ scratch 4)]
        (loop [r 0 base 0 pen-x 0]
          (if (>= r run-count)
            (assoc line
                   :advance pen-x
                   :base-direction (if (and (pos? (.-length levels))
                                            (odd? (aget levels 0)))
                                     :rtl :ltr))
            (let [{:keys [start end level tab? face]} (nth runs r)
                  len (aget lengths r)
                  rtl? (odd? level)]
              (aset (:run-source-start line) r start)
              (aset (:run-source-end line) r end)
              (aset (:run-flags line) r (bit-or (if rtl? sl/rtl-flag 0)
                                                (if tab? sl/tab-flag 0)))
              (aset (:run-face line) r face)
              (if tab?
                (let [advance (- (* (inc (long (/ pen-x tab-width))) tab-width)
                                 pen-x)]
                  (aset cs-col base start)
                  (aset ce-col base end)
                  (aset ax-col base advance)
                  (aset gx-col base pen-x)
                  (aset run-col base r)
                  (recur (inc r) (inc base) (+ pen-x advance)))
                (let [^js buffer (aget buffers r)
                      bptr (.-ptr buffer)
                      ^js font (:font (nth faces face))
                      fptr (.-ptr font)
                      ;; crossing out: the two heap views, read before any
                      ;; further WASM call (memory growth replaces them)
                      ip (/ (.hb_buffer_get_glyph_infos exports bptr 0) 4)
                      pp (/ (.hb_buffer_get_glyph_positions exports bptr 0) 4)
                      ^js heapu32 (.-HEAPU32 module)
                      ^js heap32 (.-HEAP32 module)
                      starts (js/Int32Array. len)]
                  (dotimes [i len]
                    (let [k (+ base i)
                          s5 (* 5 i)
                          cluster (+ start (aget heapu32 (+ ip s5 2)))]
                      (aset gid-col k (aget heapu32 (+ ip s5)))
                      (aset cs-col k cluster)
                      (aset starts i cluster)
                      (aset ax-col k (aget heap32 (+ pp s5)))
                      (aset ay-col k (aget heap32 (+ pp s5 1)))
                      (aset ox-col k (aget heap32 (+ pp s5 2)))
                      (aset oy-col k (aget heap32 (+ pp s5 3)))
                      (aset run-col k r)))
                  (.destroy buffer)
                  ;; cluster ends: next distinct start in the run, else run end
                  (.sort starts)
                  (let [start-count (loop [i 1 w (min 1 len)]
                                      (if (>= i len)
                                        w
                                        (if (= (aget starts i) (aget starts (dec i)))
                                          (recur (inc i) w)
                                          (do (aset starts w (aget starts i))
                                              (recur (inc i) (inc w))))))]
                    (dotimes [i len]
                      (let [k (+ base i)]
                        (aset ce-col k (next-cluster-end starts start-count
                                                         (aget cs-col k) end)))))
                  ;; extents: N calls into one provider-lifetime scratch, zero
                  ;; objects; the heap view is re-read after every call
                  (dotimes [i len]
                    (let [k (+ base i)]
                      (when (not= 0 (.hb_font_get_glyph_extents
                                     exports fptr (aget gid-col k) scratch))
                        (let [^js heap (.-HEAP32 module)]
                          (aset ix-col k (aget heap scratch-index))
                          (aset iy-col k (aget heap (+ scratch-index 1)))
                          (aset iw-col k (aget heap (+ scratch-index 2)))
                          (aset ih-col k (aget heap (+ scratch-index 3)))))))
                  ;; the pen walk, exactly `position-runs`
                  (let [pen (loop [i 0 pen pen-x]
                              (if (>= i len)
                                pen
                                (let [k (+ base i)]
                                  (aset gx-col k (+ pen (aget ox-col k)))
                                  (aset gy-col k (aget oy-col k))
                                  (recur (inc i) (+ pen (aget ax-col k))))))]
                    (recur (inc r) (+ base len) pen)))))))))))

(defn create-provider
  "Create a synchronous Contract-T provider after HarfBuzz and font bytes are
   loaded. `font-sources` is an ordered primary+fallback vector."
  [hb font-sources {:keys [features language tab-columns]
                    :or {features ["kern" "liga" "clig" "calt"]
                         language "und" tab-columns 4}}]
  (let [^js module @!harfbuzz-module
        bidi ((module-default bidi-module))
        raw-faces (mapv #(create-face-state hb %) font-sources)
        primary-upem (:upem (first raw-faces))
        faces (mapv (fn [face]
                      ;; A fallback run shares the primary font-unit space;
                      ;; layout never reconciles private per-face scales.
                      (.setScale (:font face) primary-upem primary-upem)
                      (assoc face :upem primary-upem))
                    raw-faces)
        face-meta (mapv #(select-keys % [:id :revision :upem]) faces)
        primary (first faces)
        version (js-invoke hb "version_string")
        ;; the extents scratch: 16 bytes for the provider's lifetime
        scratch (.malloc ^js (.-wasmExports module) 16)
        defaults {:features features
                  :language language
                  :tab-columns tab-columns
                  :variations (:variations primary)}]
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
                   (shape-line-flat hb module bidi faces face-meta scratch
                                    (str (or text "")) (merge defaults opts)))}))

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
