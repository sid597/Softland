(ns app.client.text.layout-oracle
  "The frozen map route of shaped text layout: the pre-flat `shaped-layout`
   and its helpers, verbatim, kept as the consistency-check oracle for the
   flat route (decisions.md \"The render seam\": a batch stage is demoted to its
   sibling's oracle, never deleted). Never improved; only read by tests and
   the verifier.
   Takes: the same layout input as `app.client.text.layout/layout`, with a
   flat or map-shaped provider.
   Gives: a Contract-T result built through per-glyph maps and
   `planes/compact-result`.
   Holds nothing."
  (:require [clojure.string :as str]
            [app.client.text.layout :as layout
             :refer [code-unit-count tagged-index header-index
                     break-whitespace-at? layout-version legacy-index-space]]
            [app.client.text.layout-planes :as planes]
            [app.client.text.shaped-line :as sl]))

;; --- verbatim from app.client.text.layout at 9f839a5 ------------------------

(defn- shaped-provider? [provider]
  (and provider (fn? (:shape-line provider))))

(defn- provider-identity [provider]
  (select-keys provider [:face-id :face-revision :shaper-id :shaper-version
                         :features :variations :axes :fallback-chain :upem
                         :metrics]))

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

(defn- work+! [!work key n]
  (vswap! !work update key (fnil + 0) n))

(defn- cluster-width [cluster]
  (Math/abs (- (double (or (:right cluster) 0))
               (double (or (:left cluster) 0)))))

(defn- cluster-break-whitespace?
  [text {:keys [source-start source-end]}]
  (and (< source-start source-end)
       (every? #(break-whitespace-at? text %)
               (range source-start source-end))))


(defn- scan-wrap-cut
  "Choose one segment-relative cut from an already-shaped source line.
   A cluster is revisited at most once after the chosen whitespace boundary,
   keeping the complete cut walk linear with a <=2C visit bound."
  [line-text clusters start-index segment-start max-units !work]
  (let [cluster-count (count clusters)
        source-length (code-unit-count line-text)]
    (loop [i start-index
           advance 0.0
           last-fitting nil
           last-candidate nil]
      (if (>= i cluster-count)
        {:kind :final :paint-end source-length :owned-end source-length
         :next-index cluster-count}
        (let [cluster (nth clusters i)
              _ (work+! !work :wrap-candidate-visits 1)
              whitespace? (cluster-break-whitespace? line-text cluster)]
          (if whitespace?
            (let [run (loop [j i width 0.0 fitting-end last-fitting
                             fitting-next-index i]
                        (if (and (< j cluster-count)
                                 (cluster-break-whitespace? line-text
                                                            (nth clusters j)))
                          (let [width' (+ width (cluster-width (nth clusters j)))
                                fits? (<= (+ advance width') max-units)]
                            (when (> j i)
                              (work+! !work :wrap-candidate-visits 1))
                            (recur (inc j) width'
                                   (if fits? (:source-end (nth clusters j))
                                       fitting-end)
                                   (if fits? (inc j) fitting-next-index)))
                          {:next-index j
                           :run-end (if (> j i)
                                      (:source-end (nth clusters (dec j)))
                                      (:source-end cluster))
                           :width width
                           :fitting-end fitting-end
                           :fitting-next-index fitting-next-index}))
                  run-start (:source-start cluster)
                  candidate? (and (> run-start segment-start)
                                  (< (:run-end run) source-length)
                                  (<= advance max-units))
                  candidate (if candidate?
                              {:kind :break
                               :paint-end run-start
                               :owned-end (:run-end run)
                               :consumed-start run-start
                               :consumed-end (:run-end run)
                               :next-index (:next-index run)}
                              last-candidate)
                  next-advance (+ advance (:width run))]
              (if (> next-advance max-units)
                (or candidate
                    (when (:fitting-end run)
                      {:kind :hard :paint-end (:fitting-end run)
                       :owned-end (:fitting-end run)
                       :next-index (:fitting-next-index run)})
                    {:kind :hard
                     :paint-end (:source-end cluster)
                     :owned-end (:source-end cluster)
                     :next-index (inc i)})
                (recur (:next-index run) next-advance (:run-end run) candidate)))
            (let [next-advance (+ advance (cluster-width cluster))
                  fitting? (<= next-advance max-units)
                  last-fitting (if fitting? (:source-end cluster) last-fitting)]
              (if (and (not fitting?) (> next-advance max-units))
                (or last-candidate
                    (when last-fitting
                      {:kind :hard :paint-end last-fitting
                       :owned-end last-fitting :next-index i})
                    {:kind :hard
                     :paint-end (:source-end cluster)
                     :owned-end (:source-end cluster)
                     :next-index (inc i)})
                (recur (inc i) next-advance last-fitting last-candidate)))))))))

(defn- shaped-segments
  "Choose all cuts from one shaped pass, then let `shaped-layout` reshape only
   the final segments. The returned ranges are relative to the source line."
  [line-text shaped inline-size !work]
  (let [source-length (code-unit-count line-text)
        clusters (vec (sort-by (juxt :source-start :source-end)
                               (:clusters shaped)))]
    (cond
      (or (not (number? inline-size)) (not (pos? inline-size)))
      [{:text line-text :relative-start 0 :relative-end source-length
        :owned-end source-length :shaped shaped}]

      (and (pos? source-length) (empty? clusters))
      [{:text line-text :relative-start 0 :relative-end source-length
        :owned-end source-length :shaped shaped :provider-fault? true}]

      (or (zero? source-length) (<= (double (or (:advance shaped) 0))
                                    inline-size))
      [{:text line-text :relative-start 0 :relative-end source-length
        :owned-end source-length :shaped shaped}]

      :else
      (loop [segment-start 0 start-index 0 result []]
        (if (>= segment-start source-length)
          result
          (let [{:keys [paint-end owned-end consumed-start consumed-end
                        next-index] :as cut}
                (scan-wrap-cut line-text clusters start-index segment-start
                               inline-size !work)
                paint-end (max segment-start (min source-length paint-end))
                owned-end (max paint-end (min source-length owned-end))
                ;; Provider cluster boundaries are the only progress unit.
                owned-end (if (= owned-end segment-start)
                            (min source-length
                                 (:source-end (nth clusters start-index)))
                            owned-end)]
            (recur owned-end
                   (max (or next-index (inc start-index))
                        (loop [i start-index]
                          (if (and (< i (count clusters))
                                   (< (:source-start (nth clusters i)) owned-end))
                            (recur (inc i))
                            i)))
                   (conj result
                         (cond-> {:text (subs line-text segment-start paint-end)
                                  :relative-start segment-start
                                  :relative-end paint-end
                                  :owned-end owned-end}
                           consumed-start
                           (assoc :consumed-relative [consumed-start consumed-end])
                           (:provider-fault? cut)
                           (assoc :provider-fault? true))))))))))

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

(defn- monotonic-glyph-order? [glyphs]
  (every? (fn [[a b]]
            (<= (long (or (:cluster-start a) 0))
                (long (or (:cluster-start b) 0))))
          (partition 2 1 glyphs)))

(defn- glyph-span-index
  "T1/T2: build source-cluster -> glyph spans once without changing the
   provider's visual glyph order. `source-offset` translates the provider's
   line-local cluster coordinates into the retained line's source domain;
   headers pass zero because each header already owns its own local domain.
   Non-monotonic sources retain exact index vectors, avoiding both a repeated
   scan and a paint-order mutation."
  [glyphs source-offset !work]
  (let [glyphs (vec glyphs)
        monotonic? (monotonic-glyph-order? glyphs)
        g (count glyphs)
        _ (work+! !work :glyph-visits g)
        grouped (reduce-kv
                 (fn [m i glyph]
                   (update m [(+ source-offset (:cluster-start glyph))
                              (+ source-offset (:cluster-end glyph))]
                           (fnil conj []) i))
                 {} glyphs)
        spans (mapv (fn [[[source-start source-end] indexes]]
                      {:source-start source-start :source-end source-end
                       :glyph-start (reduce min indexes)
                       :glyph-end (inc (reduce max indexes))
                       :glyph-indexes (vec indexes)})
                    (sort-by first grouped))]
    (work+! !work :cluster-index-writes (count spans))
    {:glyphs glyphs
     :spans spans
     :by-start (into {} (map (juxt :source-start identity)) spans)
     :monotonic? monotonic?}))

(defn- first-owned-span-index
  "Binary-search the first retained span whose source start is at or after
   `start`. A cluster crossing a style boundary is owned by the range containing
   its first source unit, exactly once."
  [spans start]
  (loop [low 0 high (count spans)]
    (if (< low high)
      (let [mid (quot (+ low high) 2)]
        (if (< (:source-start (nth spans mid)) start)
          (recur (inc mid) high)
          (recur low mid)))
      low)))

(defn- spans-for-source-range [spans start end]
  (let [spans (vec spans)
        first-index (first-owned-span-index spans start)]
    (loop [i first-index selected []]
      (if (and (< i (count spans))
               (< (:source-start (nth spans i)) end))
        (recur (inc i) (conj selected (nth spans i)))
        selected))))

(defn- glyph-indexes-for-source-range [spans start end]
  (->> (spans-for-source-range spans start end)
       (mapcat :glyph-indexes)
       distinct
       sort
       vec))


(defn- shaped-layout
  [{:keys [text source-lines provider font-size line-height origin
           baseline-offset inline-size wrap-policy wrap-col headers clip
           line-map source-id source-revision features variations language
           direction tab-stops zoom]
    :or {text "" font-size 14 line-height 14 origin [0 0]
         baseline-offset 0 wrap-policy :none zoom 1}}]
  (let [text (str (or text ""))
        headers (mapv str (or headers []))
        [ox oy] origin
        shape-opts {:features (or features (:features provider))
                    :variations (or variations (:variations provider))
                    :language (or language "und")
                    :direction (or direction :bidi)
                    :tab-columns (or (:columns tab-stops) 4)}
        !work (volatile! {:glyph-visits 0
                          :cluster-index-writes 0
                          :cluster-index-reads 0
                          :run-index-writes 0
                          :run-index-reads 0
                          :wrap-candidate-visits 0
                          :shape-calls 0
                          :reference-shapes 0
                          :provider-fault 0})
        shape! (fn [s]
                 (work+! !work :shape-calls 1)
                 ((:shape-line provider) s shape-opts))
        upem (double (or (:upem provider) 1000))
        scale (/ font-size upem)
        positive-wrap-col? (and (= :block-greedy wrap-policy)
                                (number? wrap-col) (pos? wrap-col))
        reference-shaped
        (when positive-wrap-col?
          (work+! !work :reference-shapes 1)
          ((:shape-line provider) " " shape-opts))
        reference-units
        (when reference-shaped
          (let [advance (:advance reference-shaped)]
            (when (and (seq (:clusters reference-shaped))
                       (number? advance) (pos? advance))
              (double advance))))
        _ (when (and positive-wrap-col? (nil? reference-units))
            (work+! !work :provider-fault 1))
        reference-advance (some-> reference-units (* scale))
        effective-inline-size
        (cond
          positive-wrap-col? (when reference-units (* wrap-col reference-units))
          (= :word wrap-policy) (when (and (number? inline-size)
                                           (pos? inline-size))
                                  (/ inline-size scale))
          :else nil)
        source-records (source-line-records text source-lines)
        header-records
        (mapv (fn [h header-text]
                (let [shaped (shape! header-text)]
                  (when (and (pos? (code-unit-count header-text))
                             (empty? (:clusters shaped)))
                    (work+! !work :provider-fault 1))
                  {:kind :header :header-index h :text header-text
                   :relative-start 0 :relative-end (code-unit-count header-text)
                   :owned-end (code-unit-count header-text) :shaped shaped}))
              (range) headers)
        visual-records
        (vec
         (concat
          header-records
          (mapcat
           (fn [{line-text :text source-start :source-start :as source-line}]
             (let [wrapped? (and (#{:word :block-greedy} wrap-policy)
                                 (number? effective-inline-size)
                                 (pos? effective-inline-size))
                   initial (when wrapped? (shape! line-text))
                   fault? (and initial (pos? (code-unit-count line-text))
                               (empty? (:clusters initial)))
                   _ (when fault? (work+! !work :provider-fault 1))
                   segments (if wrapped?
                              (shaped-segments line-text initial
                                               effective-inline-size !work)
                              [{:text line-text :relative-start 0
                                :relative-end (code-unit-count line-text)
                                :owned-end (code-unit-count line-text)}])]
               (map (fn [segment]
                      (let [relative-start (:relative-start segment)
                            relative-end (:relative-end segment)
                            owned-end (:owned-end segment)
                            consumed (:consumed-relative segment)]
                        (cond-> (merge source-line segment
                                       {:kind :body
                                        :source-start (+ source-start relative-start)
                                        :paint-end (+ source-start relative-end)
                                        :source-end (+ source-start owned-end)})
                          consumed
                          (assoc :consumed-absolute
                                 (mapv #(+ source-start %) consumed)))))
                    segments)))
           source-records)))
        semantic-input {:text text :source-lines source-lines
                        :headers headers
                        :source-id source-id :source-revision source-revision
                        :index-space legacy-index-space
                        :font (provider-identity provider)
                        :font-size font-size :line-height line-height
                        :origin origin :baseline-offset baseline-offset
                        :inline-size inline-size :wrap-policy wrap-policy
                        :wrap-col wrap-col
                        :features (:features shape-opts)
                        :variations (:variations shape-opts)
                        :language (:language shape-opts)
                        :direction (:direction shape-opts)
                        :tab-stops tab-stops :clip clip :line-map line-map
                        :zoom zoom}
        id (str "t1/" (hash semantic-input))
        line-data
        (mapv
          (fn [visual-index {:keys [text source-start source-end paint-end
                                    logical-line kind shaped
                                    consumed-absolute provider-fault?]
                             :as visual}]
            (let [top-y (+ oy (* visual-index line-height))
                  baseline-y (+ top-y baseline-offset)
                  header-ordinal (:header-index visual)
                  shaped (or shaped (shape! text))
                  line-fault? (and (pos? (code-unit-count text))
                                   (empty? (:clusters shaped)))
                  _ (when (and line-fault? (not provider-fault?))
                      (work+! !work :provider-fault 1))
                  span-source-offset (if (= kind :header) 0 source-start)
                  indexed (glyph-span-index (:glyphs shaped) span-source-offset !work)
                  glyphs
                  (mapv
                    (fn [glyph]
                      (let [[gx gy] (:position glyph)
                            [ax ay] (:advance glyph)
                            [off-x off-y] (:offset glyph)
                            local-start (:cluster-start glyph)
                            local-end (:cluster-end glyph)
                            start (if (= kind :header)
                                    (header-index header-ordinal local-start)
                                    (tagged-index (+ source-start local-start)))
                            end (if (= kind :header)
                                  (header-index header-ordinal local-end)
                                  (tagged-index (+ source-start local-end)))
                            positioned (assoc glyph
                                              :character (subs text
                                                               local-start local-end)
                                              :cluster {:source-range [start end]}
                                              :position [(+ ox (* gx scale))
                                                         (- baseline-y (* gy scale))]
                                              :advance [(* ax scale) (* ay scale)]
                                              :offset [(* off-x scale) (* off-y scale)])]
                        (assoc positioned :ink-bounds
                               (material-ink-bounds glyph scale ox baseline-y))))
                    (:glyphs indexed))
                  clusters
                  (mapv
                    (fn [{:keys [source-start source-end direction left right]
                          :as cluster}]
                      (let [absolute-start (if (= kind :header)
                                             (header-index header-ordinal source-start)
                                             (tagged-index (+ (:source-start visual)
                                                              source-start)))
                            absolute-end (if (= kind :header)
                                           (header-index header-ordinal source-end)
                                           (tagged-index (+ (:source-start visual)
                                                            source-end)))
                            left (+ ox (* left scale))
                            right (+ ox (* right scale))
                            span (get-in indexed
                                         [:by-start (+ span-source-offset source-start)])
                            _ (work+! !work :cluster-index-reads 1)]
                        ;; Caret stops and cluster ink bounds are NOT retained:
                        ;; both are pure functions of the retained fields
                        ;; (logical-bounds + direction + source-range, glyphs)
                        ;; and are derived at read time (cluster-caret-stops).
                        ;; Measured 2026-08-08: retaining them cost ~150MB on a
                        ;; 228k-glyph boot with zero readers outside this file.
                        (assoc cluster
                               :source-range [absolute-start absolute-end]
                               :glyph-span (when span
                                             [(:glyph-start span) (:glyph-end span)])
                               :logical-bounds {:x (min left right) :y top-y
                                                :w (Math/abs (- right left))
                                                :h line-height})))
                    (sort-by (juxt :source-start :source-end) (:clusters shaped)))
                  consumed-cluster
                  (when consumed-absolute
                    (let [[consumed-start consumed-end] consumed-absolute
                          x (+ ox (* (double (or (:advance shaped) 0)) scale))]
                      {:source-range [(tagged-index consumed-start)
                                      (tagged-index consumed-end)]
                       :consumed? true
                       :caret-stops
                       (mapv (fn [offset]
                               {:index (tagged-index offset)
                                :position [x top-y]
                                :affinity (if (= offset consumed-end)
                                            :upstream
                                            :downstream)})
                             (range consumed-start (inc consumed-end)))
                       :logical-bounds {:x x :y top-y :w 0 :h line-height}
                       :ink-bounds nil}))
                  clusters (cond-> clusters consumed-cluster
                             (conj consumed-cluster))
                  runs
                  (mapv
                    (fn [run]
                      (let [local-start (:source-start run)
                            local-end (:source-end run)
                            indexes (glyph-indexes-for-source-range
                                     (:spans indexed)
                                     (+ span-source-offset local-start)
                                     (+ span-source-offset local-end))
                            span (when (seq indexes)
                                   [(first indexes) (inc (last indexes))])
                            _ (do (work+! !work :run-index-writes 1)
                                  (work+! !work :run-index-reads 1))
                            run-start (if (= kind :header)
                                        (header-index header-ordinal local-start)
                                        (tagged-index (+ source-start local-start)))
                            run-end (if (= kind :header)
                                      (header-index header-ordinal local-end)
                                      (tagged-index (+ source-start local-end)))]
                        {:source-range [run-start run-end]
                         :direction (:direction run)
                         :font-revision (:font-revision run)
                         :glyph-span span
                         :glyphs (mapv #(nth glyphs %) indexes)}))
                    (:runs shaped))
                  advance (* (:advance shaped 0) scale)
                  logical-bounds {:x ox :y top-y :w advance :h line-height}
                  line-source-range
                  (if (= kind :header)
                    [:header header-ordinal [0 (code-unit-count text)]]
                    [(tagged-index source-start) (tagged-index source-end)])
                  paint-source-range
                  (if (= kind :header)
                    line-source-range
                    [(tagged-index source-start)
                     (tagged-index (or paint-end source-end))])]
              {:line/id [id visual-index]
               :line/index visual-index
               :logical-line logical-line
               :text text
               :source-range line-source-range
               :paint-source-range paint-source-range
               :consumed-range (when consumed-absolute
                                 (mapv tagged-index consumed-absolute))
               :baseline [ox baseline-y]
               :advance advance
               :logical-bounds logical-bounds
               :ink-bounds (union-bounds (keep :ink-bounds glyphs))
               :run-range [0 (count runs)]
               :glyphs glyphs
               :glyph-span-index (:spans indexed)
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
        zoom-legal? (layout/legal-zoom? zoom)
        work @!work]
    (when-not zoom-legal?
      (throw (ex-info "Text zoom is outside Contract-T's legal material range."
                      {:zoom zoom :legal-range [0.01 1000]})))
    (planes/compact-result
     {:text-layout/version layout-version
     :layout/id id
     :source {:id source-id :revision source-revision :text text
              :headers headers
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
              :paint-route :consumer-selected}
     :constraints {:inline-size (or inline-size :unbounded)
                   :wrap wrap-policy :line-height line-height
                   :alignment :start :tab-stops (or tab-stops {:columns 4})
                   :clip clip :line-map line-map}
     :metrics {:advance [logical-w logical-h]
               :stack-advance (* (count line-data) line-height)
               :ink-bounds (union-bounds (keep :ink-bounds line-data))
               :logical-bounds {:x ox :y oy :w logical-w :h logical-h}
               :ascent ascent :descent descent :leading leading}
     :lines line-data
     :line-index (into {} (map (juxt :line/id identity)) line-data)
     :runs runs :clusters clusters
     :reference-advance reference-advance
     :inline-size (when effective-inline-size (* effective-inline-size scale))
     :clip-plan {:visible-lines (mapv :line/id line-data)
                 :visible-glyph-ranges (mapv :source-range line-data)
                 :clip-geometry clip}
     :stats {:output-hash (str id "/" (hash [layout-version
                                                (mapv :glyph-id
                                                      (mapcat :glyphs line-data))
                                                logical-w logical-h]))
                :source-lines (vec (or source-lines (mapv :text source-records)))
                :font-shaper-environment (provider-identity provider)
                :proportionality (dissoc work :provider-fault :reference-shapes)
                :reference-shapes (:reference-shapes work)
                :provider-fault (:provider-fault work)}})))

;; --- the entry point ---------------------------------------------------------

(defn layout
  "Run the frozen map route on `input`. The provider's result is coerced to a
   shaped line and back to the pre-flat map shape, so both routes read the
   same provider output."
  [{:keys [provider] :as input}]
  (let [shape-line (:shape-line provider)]
    (shaped-layout
     (assoc input :provider
            (assoc provider :shape-line
                   (fn [text opts]
                     (sl/->maps (sl/from-maps (shape-line text opts)))))))))
