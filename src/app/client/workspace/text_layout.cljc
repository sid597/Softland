(ns app.client.workspace.text-layout
  "Contract-T's single text-layout seam.

   T0 deliberately keeps the shipped monospace behavior. The value returned by
   `layout` is nevertheless the one immutable, versioned owner of wrap,
   measurement, positioned glyphs, caret stops, selection geometry, clipping,
   and hit testing. T1 replaces this provider with shaping; readers do not
   acquire another metric route."
  (:require [clojure.string :as str]))

(def layout-version 1)

(def legacy-index-space
  "T0's explicit adapter boundary. Offsets inside a layout result are UTF-16
   code-unit offsets; T2 can replace this source map without changing readers."
  {:domain :utf-16-code-unit :version 1})

(def legacy-provider
  {:face-id :legacy/monospace
   :face-revision "t0-v1"
   :shaper-id :legacy/code-unit-grid
   :shaper-version 1})

(defn code-unit-count [s]
  #?(:clj  (.length ^String (str (or s "")))
     :cljs (.-length (str (or s "")))))

(defn- code-unit-at [s i]
  #?(:clj  (int (.charAt ^String s i))
     :cljs (.charCodeAt s i)))

(defn- tagged-index [offset]
  {:index-space legacy-index-space :offset offset})

(defn legacy-char-advance
  "The sole T0 owner of the shipped `font-size * char-width`, including the
   existing DPR snap. Callers supply font/style inputs, never redo the metric."
  ([font-size char-width]
   (* font-size char-width))
  ([font-size char-width dpr snap?]
   (let [v (* font-size char-width)]
     (if snap?
       (/ (Math/round (double (* v (or dpr 1)))) (or dpr 1))
       v))))

(defn legacy-char-advance-step
  "Renderer adapter for the existing arbitrary snap-step projection."
  [font-size char-width snap-step]
  (let [v (legacy-char-advance font-size char-width)]
    (if (and snap-step (pos? snap-step))
      (* (Math/round (double (/ v snap-step))) snap-step)
      v)))

(defn wrap-line
  "The shipped word-boundary wrapper, now owned by the T0 provider."
  [line max-chars]
  (if (or (<= (code-unit-count line) max-chars) (< max-chars 1))
    [line]
    (let [words (str/split line #" ")]
      (loop [ws words cur "" result []]
        (if (empty? ws)
          (if (seq cur) (conj result cur) result)
          (let [w (first ws)
                candidate (if (seq cur) (str cur " " w) w)]
            (cond
              (<= (code-unit-count candidate) max-chars)
              (recur (rest ws) candidate result)

              (seq cur)
              (recur ws "" (conj result cur))

              :else
              (let [chunks (loop [remaining w acc []]
                             (if (<= (code-unit-count remaining) max-chars)
                               (conj acc remaining)
                               (recur (subs remaining max-chars)
                                      (conj acc (subs remaining 0 max-chars)))))]
                (recur (rest ws) (peek chunks) (into result (pop chunks)))))))))))

(defn block-wrap-lines
  "The shipped ground-block greedy column wrapper, owned by the same provider.
   Its behavior intentionally differs from `wrap-line`."
  [lines col]
  (vec
   (mapcat
    (fn [line]
      (if (<= (code-unit-count line) col)
        [line]
        (loop [remaining line out []]
          (if (<= (code-unit-count remaining) col)
            (conj out remaining)
            (let [head (subs remaining 0 (inc col))
                  i (str/last-index-of head " ")
                  cut (if (and i (pos? i)) i col)]
              (recur (str/triml (subs remaining cut))
                     (conj out (subs remaining 0 cut))))))))
    lines)))

(defn- visual-lines
  [{:keys [text source-lines wrap-policy max-chars headers]}]
  (let [body (or source-lines
                 (case wrap-policy
                   :word (str/split-lines (or text ""))
                   :block-greedy (str/split (or text "") #"\n" -1)
                   ;; Painting and hit adapters preserve explicit empty lines.
                   (str/split (or text "") #"\n" -1)))
        body (case wrap-policy
               :word (if max-chars
                       (vec (mapcat #(wrap-line % max-chars) body))
                       (vec body))
               :block-greedy (if max-chars
                               (block-wrap-lines body max-chars)
                               (vec body))
               (vec body))]
    (into (vec (or headers [])) body)))

(defn- input-id [semantic-input]
  ;; The semantic input itself remains in :receipts; this stable process-local
  ;; hash is identity, never a source offset or shaping substitute.
  (str "t0/" (hash semantic-input)))

(defn- legacy-layout
  "Produce the immutable Contract-T result using the behavior-identical T0
   monospace provider.

   Required semantic inputs are source text (or exact `source-lines`) and the
   current advance/line height. `origin` is material-local. `baseline-offset`
   defaults to zero because existing text-op call sites already carry baseline
   y; ground blocks pass font-size to preserve their existing baseline."
  [{:keys [text source-lines font-size char-advance line-height origin
           baseline-offset inline-size max-chars wrap-policy headers clip
           line-map source-id source-revision]
    :or {text "" font-size 14 char-advance 7.84 line-height 14
         origin [0 0] baseline-offset 0 wrap-policy :none}}]
  (let [[ox oy] origin
        text (str (or text ""))
        max-chars (or max-chars
                      (when (and (= wrap-policy :word)
                                 (number? inline-size)
                                 (pos? char-advance))
                        (max 1 (long (/ inline-size char-advance)))))
        semantic-input
        {:text text
         :source-lines source-lines
         :source-id source-id
         :source-revision source-revision
         :index-space legacy-index-space
         :font legacy-provider
         :font-size font-size
         :char-advance char-advance
         :line-height line-height
         :origin origin
         :baseline-offset baseline-offset
         :inline-size inline-size
         :max-chars max-chars
         :wrap-policy wrap-policy
         :headers headers
         :clip clip
         :line-map line-map}
        id (input-id semantic-input)
        texts (visual-lines semantic-input)
        ;; Visual offsets are an explicit T0 adapter map. They are not silently
        ;; claimed to be shaped-cluster boundaries.
        line-data
        (loop [i 0 visual-offset 0 texts texts acc []]
          (if (empty? texts)
            acc
            (let [line-text (first texts)
                  n (code-unit-count line-text)
                  top-y (+ oy (* i line-height))
                  baseline-y (+ top-y baseline-offset)
                  glyphs
                  (mapv (fn [col]
                          (let [start (+ visual-offset col)]
                            {:glyph-id (code-unit-at line-text col)
                             :character (subs line-text col (inc col))
                             :cluster {:source-range [(tagged-index start)
                                                      (tagged-index (inc start))]}
                             :position [(+ ox (* col char-advance)) baseline-y]
                             :advance [char-advance 0]
                             :offset [0 0]
                             :ink-bounds nil}))
                        (range n))
                  source-range [(tagged-index visual-offset)
                                (tagged-index (+ visual-offset n))]
                  line {:line/id [id i]
                        :line/index i
                        :text line-text
                        :source-range source-range
                        :baseline [ox baseline-y]
                        :advance (* n char-advance)
                        :logical-bounds {:x ox :y top-y
                                         :w (* n char-advance) :h line-height}
                        :ink-bounds nil
                        :run-range [i (inc i)]
                        :glyphs glyphs}]
              (recur (inc i) (+ visual-offset n 1) (rest texts) (conj acc line)))))
        runs (mapv (fn [line]
                     {:source-range (:source-range line)
                      :direction :ltr
                      :font-revision (:face-revision legacy-provider)
                      :glyphs (:glyphs line)})
                   line-data)
        clusters (mapv (fn [glyph]
                         (let [[start end] (get-in glyph [:cluster :source-range])
                               [x y] (:position glyph)
                               [advance _] (:advance glyph)]
                           {:source-range [start end]
                            :caret-stops [{:index start :position [x y]
                                           :affinity :downstream}
                                          {:index end :position [(+ x advance) y]
                                           :affinity :upstream}]
                            :logical-bounds {:x x :y (- y baseline-offset)
                                             :w advance :h line-height}
                            :ink-bounds nil}))
                       (mapcat :glyphs line-data))
        logical-w (reduce max 0 (map :advance line-data))
        logical-h (* (max 1 (count line-data)) line-height)]
    {:text-layout/version layout-version
     :layout/id id
     :source {:id source-id
              :revision source-revision
              :text text
              :index-space legacy-index-space
              :source-map {:kind :legacy-visual-lines
                           :visual-text (str/join "\n" texts)}}
     :font (merge legacy-provider {:size font-size :variations {}
                                   :features [] :fallback-chain []})
     :shaping {:shaper-id (:shaper-id legacy-provider)
               :version (:shaper-version legacy-provider)
               :language :und :script :legacy :direction :ltr}
     :space {:coordinates :material-local}
     :constraints {:inline-size (or inline-size :unbounded)
                   :wrap wrap-policy
                   :line-height line-height
                   :alignment :start
                   :tab-stops :legacy
                   :clip clip
                   :line-map line-map}
     :metrics {:advance [logical-w logical-h]
               :stack-advance (* (count line-data) line-height)
               :ink-bounds nil
               :logical-bounds {:x ox :y oy :w logical-w :h logical-h}
               :ascent baseline-offset
               :descent (- line-height baseline-offset)
               :leading 0}
     :lines line-data
     :runs runs
     :clusters clusters
     :clip-plan {:visible-lines (mapv :line/id line-data)
                 :visible-glyph-ranges (mapv :source-range line-data)
                 :clip-geometry clip}
     :receipts {:input-hash id
                :output-hash (str id "/" (hash [texts logical-w logical-h]))
                :source-lines (vec (or source-lines texts))
                :font-shaper-environment legacy-provider}}))

(defn- shaped-provider? [provider]
  (and provider (fn? (:shape-line provider))))

(defn- provider-identity [provider]
  (select-keys provider [:face-id :face-revision :shaper-id :shaper-version
                         :features :variations :axes :fallback-chain :upem]))

(defn- source-line-records [text source-lines]
  (let [lines (vec (or source-lines (str/split (str (or text "")) #"\n" -1)))]
    (loop [remaining lines line-index 0 source-start 0 records []]
      (if (empty? remaining)
        records
        (let [line-text (first remaining)
              n (code-unit-count line-text)]
          (recur (rest remaining) (inc line-index) (+ source-start n 1)
                 (conj records {:text line-text
                                :logical-line line-index
                                :source-start source-start
                                :source-end (+ source-start n)})))))))

(defn- whitespace-at? [text offset]
  (when (pos? offset)
    (let [ch (subs text (dec offset) offset)]
      (boolean (re-find #"\s" ch)))))

(defn- shaped-segments
  "Greedy wrap over shaped cluster geometry. Candidate cuts are declared
   cluster ends; word wrapping prefers a whitespace boundary and reshapes each
   final segment so bidi and contextual shaping are line-correct."
  [provider line-text inline-size font-size shape-opts]
  (if (or (not (number? inline-size)) (not (pos? inline-size)))
    [{:text line-text :relative-start 0}]
    (let [scale (/ font-size (double (or (:upem provider) 1000)))
          max-units (/ inline-size scale)]
      (loop [remaining line-text relative-start 0 result []]
        (let [shaped ((:shape-line provider) remaining shape-opts)]
          (if (or (empty? remaining) (<= (:advance shaped 0) max-units))
            (conj result {:text remaining :relative-start relative-start})
            (let [clusters (sort-by :source-end (:clusters shaped))
                  fitting (filter #(<= (:right %) max-units) clusters)
                  word-cuts (filter #(whitespace-at? remaining (:source-end %)) fitting)
                  cut (or (:source-end (last word-cuts))
                          (:source-end (last fitting))
                          (:source-end (first clusters))
                          1)
                  cut (max 1 (min (code-unit-count remaining) cut))]
              (recur (subs remaining cut)
                     (+ relative-start cut)
                     (conj result {:text (subs remaining 0 cut)
                                   :relative-start relative-start})))))))))

(defn- material-ink-bounds
  [glyph scale origin-x baseline-y]
  (when-let [{:keys [xBearing yBearing width height]} (:ink-bounds glyph)]
    (let [[gx gy] (:position glyph)
          x1 (+ origin-x (* gx scale) (* xBearing scale))
          x2 (+ x1 (* width scale))
          y1 (- baseline-y (* (+ gy yBearing) scale))
          y2 (- y1 (* height scale))]
      {:x (min x1 x2) :y (min y1 y2)
       :w (Math/abs (- x2 x1)) :h (Math/abs (- y2 y1))})))

(defn- union-bounds [bounds]
  (when (seq bounds)
    (let [x1 (reduce min (map :x bounds))
          y1 (reduce min (map :y bounds))
          x2 (reduce max (map #(+ (:x %) (:w %)) bounds))
          y2 (reduce max (map #(+ (:y %) (:h %)) bounds))]
      {:x x1 :y y1 :w (- x2 x1) :h (- y2 y1)})))

(defn- shaped-layout
  [{:keys [text source-lines provider font-size line-height origin
           baseline-offset inline-size wrap-policy clip line-map source-id
           source-revision features variations language tab-stops zoom]
    :or {text "" font-size 14 line-height 14 origin [0 0]
         baseline-offset 0 wrap-policy :none zoom 1}}]
  (let [text (str (or text ""))
        [ox oy] origin
        shape-opts {:features (or features (:features provider))
                    :variations (or variations (:variations provider))
                    :language (or language "und")
                    :tab-columns (or (:columns tab-stops) 4)}
        source-records (source-line-records text source-lines)
        visual-records
        (vec
          (mapcat
            (fn [{:keys [text source-start] :as source-line}]
              (map #(merge source-line %
                           {:source-start (+ source-start (:relative-start %))
                            :source-end (+ source-start (:relative-start %)
                                           (code-unit-count (:text %)))})
                   (if (= wrap-policy :word)
                     (shaped-segments provider text inline-size font-size shape-opts)
                     [{:text text :relative-start 0}])))
            source-records))
        semantic-input {:text text :source-lines source-lines
                        :source-id source-id :source-revision source-revision
                        :index-space legacy-index-space
                        :font (provider-identity provider)
                        :font-size font-size :line-height line-height
                        :origin origin :baseline-offset baseline-offset
                        :inline-size inline-size :wrap-policy wrap-policy
                        :features (:features shape-opts)
                        :variations (:variations shape-opts)
                        :language (:language shape-opts)
                        :tab-stops tab-stops :clip clip :line-map line-map
                        :zoom zoom}
        id (str "t1/" (hash semantic-input))
        upem (double (or (:upem provider) 1000))
        scale (/ font-size upem)
        line-data
        (mapv
          (fn [visual-index {:keys [text source-start source-end logical-line] :as visual}]
            (let [top-y (+ oy (* visual-index line-height))
                  baseline-y (+ top-y baseline-offset)
                  shaped ((:shape-line provider) text shape-opts)
                  glyphs
                  (mapv
                    (fn [glyph]
                      (let [[gx gy] (:position glyph)
                            [ax ay] (:advance glyph)
                            [off-x off-y] (:offset glyph)
                            start (+ source-start (:cluster-start glyph))
                            end (+ source-start (:cluster-end glyph))
                            positioned (assoc glyph
                                              :character (subs text
                                                               (:cluster-start glyph)
                                                               (:cluster-end glyph))
                                              :cluster {:source-range [(tagged-index start)
                                                                       (tagged-index end)]}
                                              :position [(+ ox (* gx scale))
                                                         (- baseline-y (* gy scale))]
                                              :advance [(* ax scale) (* ay scale)]
                                              :offset [(* off-x scale) (* off-y scale)])]
                        (assoc positioned :ink-bounds
                               (material-ink-bounds glyph scale ox baseline-y))))
                    (:glyphs shaped))
                  clusters
                  (mapv
                    (fn [{:keys [source-start source-end direction left right]
                          :as cluster}]
                      (let [absolute-start (+ (:source-start visual) source-start)
                            absolute-end (+ (:source-start visual) source-end)
                            left (+ ox (* left scale))
                            right (+ ox (* right scale))
                            start-x (if (= direction :rtl) right left)
                            end-x (if (= direction :rtl) left right)]
                        (assoc cluster
                               :source-range [(tagged-index absolute-start)
                                              (tagged-index absolute-end)]
                               :caret-stops [{:index (tagged-index absolute-start)
                                              :position [start-x top-y]
                                              :affinity :downstream}
                                             {:index (tagged-index absolute-end)
                                              :position [end-x top-y]
                                              :affinity :upstream}]
                               :logical-bounds {:x (min left right) :y top-y
                                                :w (Math/abs (- right left))
                                                :h line-height}
                               :ink-bounds (union-bounds
                                             (keep :ink-bounds
                                                   (filter
                                                     (fn [glyph]
                                                       (= [absolute-start absolute-end]
                                                          (mapv :offset
                                                                (get-in glyph [:cluster :source-range]))))
                                                     glyphs))))))
                    (:clusters shaped))
                  runs
                  (mapv
                    (fn [run]
                      (let [run-start (+ source-start (:source-start run))
                            run-end (+ source-start (:source-end run))]
                        {:source-range [(tagged-index run-start) (tagged-index run-end)]
                         :direction (:direction run)
                         :font-revision (:font-revision run)
                         :glyphs (filterv
                                   (fn [glyph]
                                     (let [gstart (get-in glyph [:cluster :source-range 0 :offset])]
                                       (<= run-start gstart (dec (max (inc run-start) run-end)))))
                                   glyphs)}))
                    (:runs shaped))
                  advance (* (:advance shaped 0) scale)
                  logical-bounds {:x ox :y top-y :w advance :h line-height}]
              {:line/id [id visual-index]
               :line/index visual-index
               :logical-line logical-line
               :text text
               :source-range [(tagged-index source-start) (tagged-index source-end)]
               :baseline [ox baseline-y]
               :advance advance
               :logical-bounds logical-bounds
               :ink-bounds (union-bounds (keep :ink-bounds glyphs))
               :run-range [0 (count runs)]
               :glyphs glyphs
               :runs runs
               :clusters clusters}))
          (range) visual-records)
        runs (vec (mapcat :runs line-data))
        clusters (vec (mapcat :clusters line-data))
        logical-w (reduce max 0 (map :advance line-data))
        logical-h (* (max 1 (count line-data)) line-height)
        metrics (:metrics provider)
        ascent (* (or (:ascender metrics) 0) scale)
        descent (* (- (or (:descender metrics) 0)) scale)
        leading (* (or (:lineGap metrics) 0) scale)
        legal-zoom? (<= 0.01 zoom 1000)]
    (when-not legal-zoom?
      (throw (ex-info "Text zoom is outside Contract-T's legal material range."
                      {:zoom zoom :legal-range [0.01 1000]})))
    {:text-layout/version layout-version
     :layout/id id
     :source {:id source-id :revision source-revision :text text
              :index-space legacy-index-space
              :source-map {:kind :shaped-visual-lines
                           :visual-lines (mapv #(select-keys % [:text :source-start
                                                               :source-end :logical-line])
                                               visual-records)}}
     :font (merge (provider-identity provider)
                  {:size font-size :variations (:variations shape-opts)
                   :features (:features shape-opts)})
     :shaping {:shaper-id (:shaper-id provider)
               :version (:shaper-version provider)
               :language (:language shape-opts) :script :auto
               :direction :bidi}
     :space {:coordinates :material-local}
     :regime {:legal-zoom [0.01 1000] :zoom zoom
              :precision :material-f64
              :paint-road :consumer-selected}
     :constraints {:inline-size (or inline-size :unbounded)
                   :wrap wrap-policy :line-height line-height
                   :alignment :start :tab-stops (or tab-stops {:columns 4})
                   :clip clip :line-map line-map}
     :metrics {:advance [logical-w logical-h]
               :stack-advance (* (count line-data) line-height)
               :ink-bounds (union-bounds (keep :ink-bounds line-data))
               :logical-bounds {:x ox :y oy :w logical-w :h logical-h}
               :ascent ascent :descent descent :leading leading}
     :lines line-data :runs runs :clusters clusters
     :clip-plan {:visible-lines (mapv :line/id line-data)
                 :visible-glyph-ranges (mapv :source-range line-data)
                 :clip-geometry clip}
     :receipts {:input-hash id
                :output-hash (str id "/" (hash [(mapv :glyph-id (mapcat :glyphs line-data))
                                                logical-w logical-h]))
                :source-lines (vec (or source-lines (mapv :text source-records)))
                :font-shaper-environment (provider-identity provider)}}))

(defn layout
  "Produce the one immutable Contract-T result. A real provider selects T1;
   absence of a provider preserves the exact T0 compatibility road."
  [{:keys [provider] :as input}]
  (if (shaped-provider? provider)
    (shaped-layout input)
    (legacy-layout input)))

(defn measure-result [layout-result]
  {:layout/id (:layout/id layout-result)
   :metrics (:metrics layout-result)})

(defn wrap-result [layout-result]
  {:layout/id (:layout/id layout-result)
   :lines (mapv :text (:lines layout-result))})

(defn paint-result [layout-result]
  {:layout/id (:layout/id layout-result)
   :glyphs (vec (mapcat :glyphs (:lines layout-result)))
   :lines (:lines layout-result)})

(defn line-paint-ops
  "Adapt layout lines back to the existing text-op maps without changing the
   renderer-facing schema. Style/range keys come from `template`; positions and
   line text come only from the layout result."
  [layout-result template]
  (mapv (fn [{:keys [line/id text baseline source-range]}]
          (cond-> (assoc template
                         :text text
                         :from 0
                         :to (code-unit-count text)
                         :x (first baseline)
                         :y (second baseline))
            (not= :legacy/code-unit-grid (get-in layout-result [:shaping :shaper-id]))
            (assoc :layout-result layout-result
                   :layout-line-id id
                   :layout-anchor [(first baseline) (second baseline)]
                   :paint-source-range source-range)))
        (:lines layout-result)))

(defn- shaped-result? [layout-result]
  (not= :legacy/code-unit-grid (get-in layout-result [:shaping :shaper-id])))

(defn- line-caret-stops [line-data]
  (vec (mapcat :caret-stops (:clusters line-data))))

(defn- nearest-by [value value-fn xs]
  (when (seq xs)
    (reduce (fn [best candidate]
              (if (< (Math/abs (- (double (value-fn candidate)) value))
                     (Math/abs (- (double (value-fn best)) value)))
                candidate
                best))
            (first xs) (rest xs))))

(defn- shaped-caret-result [layout-result line col]
  (let [lines (:lines layout-result)
        line (max 0 (min (long (or line 0)) (dec (max 1 (count lines)))))
        line-data (nth lines line {:text "" :logical-bounds {:x 0 :y 0 :h 0}
                                   :source-range [(tagged-index 0) (tagged-index 0)]})
        line-start (get-in line-data [:source-range 0 :offset] 0)
        line-end (get-in line-data [:source-range 1 :offset] line-start)
        requested (+ line-start (max 0 (min (long (or col 0))
                                               (- line-end line-start))))
        stops (line-caret-stops line-data)
        exact (filter #(= requested (get-in % [:index :offset])) stops)
        stop (or (first (filter #(= :downstream (:affinity %)) exact))
                 (first exact)
                 (nearest-by requested #(get-in % [:index :offset]) stops)
                 {:index (tagged-index line-start)
                  :position [(:x (:logical-bounds line-data))
                             (:y (:logical-bounds line-data))]
                  :affinity :downstream})
        [x _] (:position stop)
        bounds (:logical-bounds line-data)
        actual-col (- (get-in stop [:index :offset] line-start) line-start)]
    {:layout/id (:layout/id layout-result)
     :index (:index stop)
     :line line
     :col actual-col
     :position [x (:y bounds)]
     :rect {:x x :y (:y bounds) :w 2 :h (:h bounds)}
     :affinity (:affinity stop)}))

(defn caret-result [layout-result line col]
  (if (shaped-result? layout-result)
    (shaped-caret-result layout-result line col)
    (let [lines (:lines layout-result)
        line (max 0 (min (long (or line 0)) (dec (max 1 (count lines)))))
        line-data (nth lines line {:text "" :logical-bounds {:x 0 :y 0 :h 0}
                                   :baseline [0 0]})
        col (max 0 (min (long (or col 0)) (code-unit-count (:text line-data))))
        advance (get-in layout-result [:font :size])
        char-advance (or (get-in (first (:glyphs line-data)) [:advance 0])
                         (let [w (get-in line-data [:logical-bounds :w] 0)
                               n (code-unit-count (:text line-data))]
                           (if (pos? n) (/ w n) 0)))
        bounds (:logical-bounds line-data)]
    {:layout/id (:layout/id layout-result)
     :index (tagged-index (+ (get-in line-data [:source-range 0 :offset] 0) col))
     :line line
     :col col
     :position [(+ (:x bounds) (* col char-advance)) (:y bounds)]
     :rect {:x (+ (:x bounds) (* col char-advance))
            :y (:y bounds) :w 2 :h (:h bounds)}
     :affinity :downstream
     :legacy/font-size advance})))

(defn- shaped-selection-result [layout-result line col-start col-end min-width]
  (let [a (shaped-caret-result layout-result line col-start)
        b (shaped-caret-result layout-result line col-end)
        [start end] (sort [(:offset (:index a)) (:offset (:index b))])
        line-data (get (:lines layout-result) (:line a))
        selected (filter
                   (fn [cluster]
                     (let [[cs ce] (mapv :offset (:source-range cluster))]
                       (and (< cs end) (> ce start))))
                   (:clusters line-data))
        rects (mapv :logical-bounds selected)
        rects (if (seq rects)
                rects
                [{:x (first (:position a))
                  :y (get-in line-data [:logical-bounds :y] 0)
                  :w min-width
                  :h (get-in line-data [:logical-bounds :h] 0)}])
        bounding (or (union-bounds rects)
                     {:x (first (:position a)) :y 0 :w min-width :h 0})]
    {:layout/id (:layout/id layout-result)
     :source-range [(:index a) (:index b)]
     :rects rects
     :rect (update bounding :w max min-width)}))

(defn selection-result
  [layout-result line col-start col-end & {:keys [min-width] :or {min-width 0}}]
  (if (shaped-result? layout-result)
    (shaped-selection-result layout-result line col-start col-end min-width)
    (let [a (caret-result layout-result line col-start)
        b (caret-result layout-result line col-end)
        x1 (first (:position a))
        x2 (first (:position b))
        line-bounds (get-in layout-result [:lines (:line a) :logical-bounds]
                            {:y 0 :h 0})]
    {:layout/id (:layout/id layout-result)
     :source-range [(:index a) (:index b)]
     :rect {:x (min x1 x2) :y (:y line-bounds)
            :w (max min-width (Math/abs (- x2 x1)))
            :h (:h line-bounds)}})))

(defn- ceil-long [x]
  (long (Math/ceil (double x))))

(defn- floor-long [x]
  (long (Math/floor (double x))))

(defn clip-result
  "Clip one existing text op through the layout's declared clip geometry.
   `range-mode` preserves the two legacy adapters exactly: `:left-right`
   rewrites :from/:to after substring clipping; `:right-only` preserves :from
   and only updates :to, matching rect-tree's old path."
  [layout-result op & {:keys [range-mode] :or {range-mode :left-right}}]
  (if (shaped-result? layout-result)
    (let [{:keys [left right top bottom]} (get-in layout-result [:constraints :clip])
          requested-line-id (:layout-line-id op)
          line (or (first (filter #(= requested-line-id (:line/id %))
                                  (:lines layout-result)))
                   (nearest-by (:y op 0) #(second (:baseline %)) (:lines layout-result))
                   (first (:lines layout-result)))
          [line-start line-end] (mapv :offset (:source-range line))
          op-start (+ line-start (long (or (:from op) 0)))
          op-end (+ line-start (long (or (:to op)
                                         (code-unit-count (:text op "")))))
          vertical? (and (or (nil? top) (>= (second (:baseline line)) top))
                         (or (nil? bottom) (< (second (:baseline line)) bottom)))
          visible (filter
                    (fn [cluster]
                      (let [{:keys [x w]} (:logical-bounds cluster)
                            [start end] (mapv :offset (:source-range cluster))]
                        (and (< start op-end) (> end op-start)
                             (or (nil? left) (> (+ x w) left))
                             (or (nil? right) (< x right)))))
                    (:clusters line))
          visible-start (if (seq visible)
                          (reduce min (map #(get-in % [:source-range 0 :offset]) visible))
                          op-start)
          visible-end (if (seq visible)
                        (reduce max (map #(get-in % [:source-range 1 :offset]) visible))
                        op-start)
          relative-start (- visible-start line-start)
          relative-end (- visible-end line-start)
          visible-range [(tagged-index visible-start) (tagged-index visible-end)]]
      {:layout/id (:layout/id layout-result)
       :op (when (and vertical? (or (and (nil? left) (nil? right)) (seq visible)))
             (cond-> (assoc op
                            :paint-source-range visible-range)
               (= range-mode :left-right)
               (assoc :from relative-start :to relative-end)

               (= range-mode :right-only)
               (assoc :to relative-end)))
       :visible-range visible-range})
    (let [{:keys [left right top bottom]} (get-in layout-result [:constraints :clip])
        line (first (:lines layout-result))
        txt (:text line "")
        n (code-unit-count txt)
        [x y] (:baseline line [(:x op 0) (:y op 0)])
        cw (or (get-in (first (:glyphs line)) [:advance 0])
               (let [w (get-in line [:logical-bounds :w] 0)]
                 (if (pos? n) (/ w n) 0)))
        text-end (+ x (* n cw))
        vertical? (and (or (nil? top) (>= y top))
                       (or (nil? bottom) (< y bottom)))
        horizontal? (and (or (nil? right) (< x right))
                         (or (nil? left) (> text-end left)))
        skip (if (and left (< x left) (pos? cw))
               (min n (ceil-long (/ (- left x) cw)))
               0)
        adj-x (+ x (* skip cw))
        take-n (if (and right (pos? cw))
                 (max 0 (floor-long (/ (- right adj-x) cw)))
                 (- n skip))
        end (min n (+ skip take-n))]
    {:layout/id (:layout/id layout-result)
     :op (when (and vertical?
                    (if (or left right) (and horizontal? (< skip end)) true))
           (if-not (or left right)
             op
             (cond-> (assoc op :text (subs txt skip end) :x adj-x)
               (= range-mode :left-right)
               (assoc :from skip :to end)

               (= range-mode :right-only)
               (assoc :to end))))
     :visible-range [(tagged-index skip) (tagged-index end)]})))

(defn- shaped-hit-test-result [layout-result [x y]]
  (let [lines (:lines layout-result)
        line-data (or (first (filter (fn [line]
                                      (let [{ly :y h :h} (:logical-bounds line)]
                                        (<= ly y (+ ly h))))
                                    lines))
                      (nearest-by y #(get-in % [:logical-bounds :y]) lines)
                      (first lines))
        stops (line-caret-stops line-data)
        stop (or (nearest-by x #(first (:position %)) stops)
                 {:index (first (:source-range line-data))})
        visual-line (:line/index line-data 0)
        logical-line (or (get-in layout-result [:constraints :line-map visual-line])
                         (:logical-line line-data)
                         visual-line)
        source-line-start (get-in line-data [:source-range 0 :offset] 0)
        col (max 0 (- (get-in stop [:index :offset] source-line-start)
                      source-line-start))]
    {:layout/id (:layout/id layout-result)
     :visual-line visual-line
     :line logical-line
     :col col
     :index (:index stop)
     :affinity (:affinity stop)
     :index-space legacy-index-space}))

(defn hit-test-result
  "Point -> visual line -> optional logical line map -> legacy caret stop."
  [layout-result [x y]]
  (if (shaped-result? layout-result)
    (shaped-hit-test-result layout-result [x y])
    (let [{:keys [line-map]} (:constraints layout-result)
        source-lines (or (get-in layout-result [:receipts :source-lines])
                         (mapv :text (:lines layout-result)))
        line-count (max 1 (if (seq line-map) (count line-map) (count source-lines)))
        bounds (get-in layout-result [:metrics :logical-bounds])
        line-h (get-in layout-result [:constraints :line-height])
        visual-line (-> (/ (- y (:y bounds)) line-h)
                        (Math/floor) long (max 0) (min (dec line-count)))
        logical-line (if (seq line-map)
                       (get line-map visual-line visual-line)
                       visual-line)
        line-text (get source-lines logical-line "")
        line-len (code-unit-count line-text)
        first-line (first (:lines layout-result))
        cw (or (get-in (first (:glyphs first-line)) [:advance 0])
               (let [w (get-in first-line [:logical-bounds :w] 0)
                     n (code-unit-count (:text first-line ""))]
                 (if (pos? n) (/ w n) 0)))
        col (if (pos? cw)
              (-> (/ (- x (:x bounds)) cw)
                  double Math/round long (max 0) (min line-len))
              0)]
    {:layout/id (:layout/id layout-result)
     :visual-line visual-line
     :line logical-line
     :col col
     :index-space legacy-index-space})))
