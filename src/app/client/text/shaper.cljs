(ns app.client.text.shaper
  "Produce visual-order glyph columns from font programs.

   Input: ordered font sources and shaping options; then text passed to the
   provider's closure. Output: a provider whose :shape-line returns typed
   glyph/run columns. The namespace caches the HarfBuzz wrapper, raw module
   and in-flight promise. Each provider closes over font/blob/face objects,
   bidi engine, metadata and a 16-byte WASM extents scratch allocation.

   Splits by bidi level, face and tab status, shapes runs, reads WASM glyph
   columns and writes the provider result. Per-glyph extents queries still
   cross into WASM.

   Folder map: README.md."
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

(defn- module-default
  "Imported module → default export or module itself.

   Interop normalization."
  [module]
  (or (.-default module) module))

(defn- fetch-bytes*
  "URL → ArrayBuffer promise; bad status throws.

   One checked fetch."
  [url]
  (-> (js/fetch url)
      (.then (fn [response]
               (when-not (.-ok response)
                 (throw (js/Error. (str "Font resource fetch failed: " url
                                        " (" (.-status response) ")"))))
               (.arrayBuffer response)))))

(defn- fetch-bytes
  "URL/optional attempt → retried byte promise.

   Four attempts with linear backoff. Retry policy is duplicated with font
   asset loading."
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
  "No caller input; pinned WASM URL → shared wrapper promise.

   Caches in-flight/successful load; clears failed promise. Concurrent
   callers share initialization and later calls can retry failure. Explicit
   wasmBinary makes asset resolution independent of the bundle URL. The raw
   Emscripten module is retained beside the wrapper for heap access."
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

(defn- typed-set
  "Typed array → ClojureScript set.

   Materializes membership values. Face state also retains a JS Set of the
   same unicodes."
  [typed-array]
  (into #{} (array-seq typed-array)))

(defn- js-object->map
  "JS object → keyword-keyed map.

   Enumerates own keys. Intended for axis metadata."
  [object]
  (into {}
        (map (fn [key]
               [(keyword key) (aget object key)]))
        (js/Object.keys object)))

(defn- create-face-state
  "HarfBuzz wrapper and bytes/identity/variations → face/font/blob state,
   coverage sets and metrics.

   Creates and configures native handles owned by the returned provider.
   Its :dispose! releases these handles after the last use."
  [hb {:keys [id revision bytes variations]}]
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
  "Faces, text, UTF-16 length → byte array selecting face per code unit.

   First face covering each codepoint, primary fallback when none; surrogate
   pair shares face. Byte indexes allow only 256 distinct face indexes;
   fallback is per codepoint, not whole grapheme/script shaping context."
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
  "Text, bidi levels, face indexes, length → maximal
   same-level/face/tab-status runs.

   Linear scan. Intended for this segmentation; adjacent tabs become one tab
   run."
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

(defn- visually-order-runs
  "Bidi engine/text/embedding/runs/length → runs sorted by visual rank.

   Builds code-unit rank array then sorts runs. Retains small per-run maps."
  [bidi text embedding runs n]
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

(defn- apply-variations!
  "Faces and requested variations → nil; mutates font variation settings.

   Merges defaults and filters unsupported axes. Intended for synchronous
   provider use; provider is stateful."
  [faces variations]
  (doseq [face faces]
    (let [supported (set (keys (:axes face)))
          requested (merge (:variations face) variations)
          active (into {} (filter (fn [[axis _]] (contains? supported axis))) requested)]
      (.setVariations (:font face) (clj->js active)))))

(defn- next-cluster-end
  "Sorted distinct starts/count/current start/run end → next larger start or
   run end.

   Binary search."
  [starts start-count cluster run-end]
  (loop [lo 0 hi start-count]
    (if (< lo hi)
      (let [mid (bit-shift-right (+ lo hi) 1)]
        (if (<= (aget starts mid) cluster)
          (recur (inc mid) hi)
          (recur lo mid)))
      (if (< lo start-count) (aget starts lo) run-end))))

(defn- shape-line-flat
  "Loaded engines/font state/scratch/text/options → shaped-line columns.

   Shape runs, bulk-read infos/positions, query extents, derive clusters and
   pen positions. Direction option is not destructured/applied; bidi chooses
   direction. One virtual glyph/advance is emitted per tab run, including a
   run of consecutive tabs. Temporary buffers are destroyed on normal path,
   without finally for intermediate exceptions."
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
  "Loaded wrapper, font bytes, defaults → provider with :shape-line(text,
   opts) → columns.

   Normalizes fallback font scales to primary UPEM, allocates scratch,
   closes over face state. Intended for synchronous shaping.
   Provider owners call :dispose! after the final layout. Disposal is
   idempotent; shaping afterward throws. Empty source vectors are invalid."
  [hb font-sources {:keys [features language tab-columns]
                    :or {features ["kern" "liga" "clig" "calt"]
                         language "und" tab-columns 4}}]
  (assert (seq font-sources) "A shaping provider requires at least one font")
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
        !disposed (atom false)
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
     :dispose! (fn []
                 (when (compare-and-set! !disposed false true)
                   (doseq [{:keys [font face blob]} faces]
                     (.destroy font) (.destroy face) (.destroy blob))
                   (.free ^js (.-wasmExports module) scratch)))
     :shape-line (fn [text opts]
                   (when @!disposed
                     (throw (ex-info "Shaping provider has been disposed" {:error-type :text/provider-disposed})))
                   (shape-line-flat hb module bidi faces face-meta scratch
                                    (str (or text "")) (merge defaults opts)))}))

(defn load-provider!
  "URL source vector and options → provider promise.

   Loads WASM and fonts in parallel then constructs provider. Owns loading,
   not resource disposal. Font source records carry :id, :revision, :url and
   :variations, ordered primary first and then fallbacks."
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
