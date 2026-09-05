(ns app.client.text.layout-planes
  "Own retained layout columns and reconstruct views.

   Input: a rich layout result to compact, or glyph/span counts and
   primitive row writes to build directly; subsequently a line and query.
   Output: plane-backed results, rich views, selected glyph indexes, packing
   callbacks and comparisons. Arrays are reachable from the returned result
   and its lines; this namespace exposes access operations rather than
   requiring renderer code to know column addresses.

   Folder map: README.md."
  )

;; Positioned geometry uses float32 columns. Glyph IDs occupy 30 bits with
;; tab/direction flags in the remaining bits. Source spans address
;; result-wide glyphs; compatibility views derive line-local offsets.
(def ^:private id-mask 0x3fffffff)
(def ^:private kind-mask 0x40000000)
(def ^:private rtl-mask 0x80000000)
(def ^:private no-ink #?(:clj Float/NaN :cljs js/NaN))

(defn- f32-array
  "Length → zeroed float32 column.

   Uses JVM primitive arrays or native JS typed arrays."
  [n]
  #?(:clj (float-array n) :cljs (js/Float32Array. n)))

(defn- u32-array
  "Length → zeroed unsigned 32-bit integer column.

   Uses JVM primitive arrays or native JS typed arrays."
  [n]
  #?(:clj (int-array n) :cljs (js/Uint32Array. n)))

(defn- f32-set!
  "Column, index and value → assigned value; mutates float32 storage.

   Numeric narrowing and precision loss follow the column format."
  [a i value]
  #?(:clj (aset-float ^floats a i (float value))
     :cljs (aset a i (double value))))

(defn- u32-set!
  "Column, index and value → assigned value; mutates unsigned 32-bit integer
   storage.

   Numeric narrowing and precision loss follow the column format."
  [a i value]
  #?(:clj (aset-int ^ints a i (unchecked-int (long value)))
     :cljs (aset a i value)))

(defn- f32-get
  "Column and index → decoded float32 value.

   Handles platform storage interpretation."
  [a i]
  #?(:clj (double (aget ^floats a i))
     :cljs (aget a i)))

(defn- view-number
  "Line and number → legacy integral JVM number when applicable, otherwise
   unchanged.

   Compatibility normalization. Output numeric type depends on shaped/legacy
   line marker."
  [line value]
  #?(:clj (if (and (not (::shaped? line))
                    (not (Double/isNaN value))
                    (== value (Math/rint value)))
             (long value)
             value)
     :cljs value))

(defn- u32-get
  "Column and index → decoded unsigned 32-bit integer value.

   Handles platform storage interpretation."
  [a i]
  #?(:clj (bit-and 0xffffffff (long (aget ^ints a i)))
     :cljs (aget a i)))

(defn- array-byte-length
  "Array and bytes per element → storage byte count.

   JVM length×width or JS byteLength. Excludes containing maps/metadata."
  [a bytes-per-entry]
  #?(:clj (* (alength a) bytes-per-entry)
     :cljs (.-byteLength a)))

(defn- nan-value?
  "Number → NaN?

   Platform adapter. Intended for sentinel handling."
  [value]
  #?(:clj (Double/isNaN value)
     :cljs (js/Number.isNaN value)))

(defn- source-offset
  "Tagged body/header index or scalar → numeric offset.

   Shape dispatch. Accepts a broader scalar form than public layout
   accessor."
  [index]
  (cond
    (map? index) (:offset index)
    (and (vector? index) (= :header (first index))) (nth index 2)
    :else index))

(defn- source-bounds
  "Header/body range → numeric pair.

   Decodes index representation."
  [source-range]
  (if (and (vector? source-range) (= :header (first source-range)))
    (nth source-range 2)
    (mapv source-offset source-range)))

(defn- tagged-index
  "Offset → UTF-16 tagged body index.

   Small constructor. Does not use layout's intern cache."
  [offset]
  {:index-space {:domain :utf-16-code-unit :version 1}
   :offset offset})

(defn- line-index-value
  "Line and offset → header index or tagged body index.

   Preserves source domain."
  [line offset]
  (if (= :header (first (:source-range line)))
    [:header (second (:source-range line)) offset]
    (tagged-index offset)))

(defn- exact-range?
  "Span with endpoints/indexes → indexes equal full contiguous range?

   Materialized vector comparison. Intended for compatibility compaction;
   allocates range vector."
  [{:keys [glyph-start glyph-end glyph-indexes]}]
  (= (vec (range glyph-start glyph-end)) (vec glyph-indexes)))

(defn- line-cluster-inputs
  "Rich line → existing clusters or one cluster projection per glyph.

   Compatibility fallback."
  [line]
  (if (seq (:clusters line))
    (:clusters line)
    (mapv (fn [glyph]
            {:source-range (get-in glyph [:cluster :source-range])})
          (:glyphs line))))

(defn- line-span-inputs
  "Rich line → existing span index or one-glyph spans.

   Derives absent legacy indexing. Semantics rely on input rich glyph
   ownership."
  [line]
  (if (seq (:glyph-span-index line))
    (:glyph-span-index line)
    (mapv (fn [i glyph]
            (let [[start end] (source-bounds
                               (get-in glyph [:cluster :source-range]))]
              {:source-start start :source-end end
               :glyph-start i :glyph-end (inc i) :glyph-indexes [i]}))
          (range) (:glyphs line))))

(defn- non-monotonic-result?
  "Rich lines → any noncontiguous span?

   Examines span inputs. Determines optional order storage."
  [lines]
  (boolean
   (some (fn [line]
           (some (complement exact-range?) (line-span-inputs line)))
         lines)))

(defn- run-records
  "Rich line/global glyph base → compact run records with global spans and
   optional Y-advance flag.

   Removes glyph vectors, infers font metadata and Y-advance presence."
  [line glyph-base]
  (mapv
   (fn [run]
     (let [[start end] (or (:glyph-span run) [0 0])
           first-glyph (get (:glyphs line) start)]
       (cond-> (-> run
                   (dissoc :glyphs)
                   (assoc :glyph-span [(+ glyph-base start)
                                       (+ glyph-base end)]
                          :font-id (or (:font-id run)
                                       (:font-id first-glyph))
                          :font-revision (or (:font-revision run)
                                             (:font-revision first-glyph))))
         (some (fn [glyph]
                 (not (zero? (double (or (second (:advance glyph)) 0)))))
               (subvec (vec (:glyphs line))
                       (min start (count (:glyphs line)))
                       (min end (count (:glyphs line)))))
         (assoc :advance-y-column? true))))
   (:runs line)))

(defn- run-source-index
  "Runs → source-sorted run containment records.

   Indexed projection/sort."
  [runs]
  (->> runs
       (map-indexed
        (fn [i run]
          (let [[start end] (source-bounds (:source-range run))]
            {:source-start start :source-end end :run-index i})))
       (sort-by (juxt :source-start :source-end))
       vec))

(defn- build-shape
  "Rich result → flattened glyphs/spans/order and compact line specs.

   Computes global offsets and optional noncontiguous order. Temporary rich
   structures remain during conversion; direct builder avoids this path."
  [result]
  (let [top-runs (vec (:runs result))
        lines (mapv (fn [i line]
                      (if (contains? line :runs)
                        line
                        (assoc line :runs
                               (cond-> []
                                 (get top-runs i) (conj (get top-runs i))))))
                    (range) (:lines result))
        non-monotonic? (non-monotonic-result? lines)]
    (loop [remaining lines
           glyph-base 0
           span-base 0
           run-base 0
           glyphs []
           spans []
           glyph-order []
           line-specs []]
      (if-let [line (first remaining)]
        (let [[line-start _] (source-bounds (:source-range line))
              shaped? (boolean (some :cluster-start (:glyphs line)))
              line-glyphs
              (mapv (fn [glyph]
                      (let [[absolute-start absolute-end]
                            (source-bounds (get-in glyph [:cluster :source-range]))]
                        (assoc glyph
                               ::cluster-start-local
                               (or (:cluster-start glyph)
                                   (- absolute-start line-start))
                               ::cluster-end-local
                               (or (:cluster-end glyph)
                                   (- absolute-end line-start)))))
                    (:glyphs line))
              line-spans (line-span-inputs line)
              span-by-range (into {}
                                  (map (fn [span]
                                         [[(:source-start span)
                                           (:source-end span)] span]))
                                  line-spans)
              clusters (line-cluster-inputs line)
              [span-rows next-order]
              (reduce
               (fn [[rows order] cluster]
                 (let [[source-start source-end]
                       (source-bounds (:source-range cluster))
                       span (get span-by-range [source-start source-end])
                       local-indexes (vec (:glyph-indexes span))
                       global-indexes (mapv #(+ glyph-base %) local-indexes)
                       order-start (if non-monotonic?
                                     (count order)
                                     (if (seq global-indexes)
                                       (reduce min global-indexes)
                                       (+ glyph-base (count line-glyphs))))
                       order-end (if non-monotonic?
                                   (+ order-start (count global-indexes))
                                   (if (seq global-indexes)
                                     (inc (reduce max global-indexes))
                                     order-start))]
                   [(conj rows {:source-start source-start
                                :source-end source-end
                                :glyph-start order-start
                                :glyph-end order-end})
                    (if non-monotonic?
                      (into order global-indexes)
                      order)]))
               [[] glyph-order]
               clusters)
              runs (run-records line glyph-base)
              line-spec (-> line
                            (dissoc :glyphs :glyph-span-index :clusters :runs)
                            (assoc :glyph-start glyph-base
                                   :glyph-end (+ glyph-base (count line-glyphs))
                                   :cluster-start span-base
                                   :cluster-end (+ span-base (count span-rows))
                                   :runs runs
                                   :run-range [run-base (+ run-base (count runs))]
                                   ::shaped? shaped?
                                   ::run-source-index (run-source-index runs)))]
          (recur (next remaining)
                 (+ glyph-base (count line-glyphs))
                 (+ span-base (count span-rows))
                 (+ run-base (count runs))
                 (into glyphs line-glyphs)
                 (into spans span-rows)
                 next-order
                 (conj line-specs line-spec)))
        {:glyphs glyphs :spans spans :glyph-order glyph-order
         :line-specs line-specs :non-monotonic? non-monotonic?}))))

(defn- pack-id
  "Glyph map → ID/tab/direction bitfield.

   Masks to 30-bit ID. Intended for expected glyph IDs; larger values
   truncate without validation."
  [glyph]
  (let [glyph-id (long (or (:glyph-id glyph) 0))]
    (bit-or (bit-and glyph-id id-mask)
            (if (= :virtual/tab (:glyph-id-kind glyph)) kind-mask 0)
            (if (= :rtl (:direction glyph)) rtl-mask 0))))

(defn- allocate-planes
  "Flattened shape and source → populated arrays/counts/source.

   One allocation pass and row writes; NaN marks absent ink. Serves as
   compatibility converter."
  [{:keys [glyphs spans glyph-order non-monotonic?]} source]
  (let [glyph-count (count glyphs)
        span-count (count spans)
        position-x (f32-array glyph-count)
        position-y (f32-array glyph-count)
        advance-x (f32-array glyph-count)
        offset-x (f32-array glyph-count)
        offset-y (f32-array glyph-count)
        ink-x (f32-array glyph-count)
        ink-y (f32-array glyph-count)
        ink-w (f32-array glyph-count)
        ink-h (f32-array glyph-count)
        glyph-id+flags (u32-array glyph-count)
        cluster-start (u32-array glyph-count)
        cluster-end (u32-array glyph-count)
        advance-y? (some (fn [glyph]
                           (not (zero? (double (or (second (:advance glyph)) 0)))))
                         glyphs)
        advance-y (when advance-y? (f32-array glyph-count))
        span-source-start (u32-array span-count)
        span-source-end (u32-array span-count)
        span-glyph-start (u32-array span-count)
        span-glyph-end (u32-array span-count)
        glyph-order-array (when non-monotonic?
                            (u32-array (count glyph-order)))]
    (doseq [[i glyph] (map-indexed vector glyphs)]
      (let [[px py] (:position glyph)
            [ax ay] (:advance glyph)
            [ox oy] (:offset glyph)
            ink (:ink-bounds glyph)]
        (f32-set! position-x i (or px 0))
        (f32-set! position-y i (or py 0))
        (f32-set! advance-x i (or ax 0))
        (when advance-y (f32-set! advance-y i (or ay 0)))
        (f32-set! offset-x i (or ox 0))
        (f32-set! offset-y i (or oy 0))
        (f32-set! ink-x i (if ink (:x ink) no-ink))
        (f32-set! ink-y i (if ink (:y ink) 0))
        (f32-set! ink-w i (if ink (:w ink) 0))
        (f32-set! ink-h i (if ink (:h ink) 0))
        (u32-set! glyph-id+flags i (pack-id glyph))
        (u32-set! cluster-start i (::cluster-start-local glyph 0))
        (u32-set! cluster-end i (::cluster-end-local glyph 0))))
    (doseq [[i span] (map-indexed vector spans)]
      (u32-set! span-source-start i (:source-start span))
      (u32-set! span-source-end i (:source-end span))
      (u32-set! span-glyph-start i (:glyph-start span))
      (u32-set! span-glyph-end i (:glyph-end span)))
    (when glyph-order-array
      (doseq [[i glyph-index] (map-indexed vector glyph-order)]
        (u32-set! glyph-order-array i glyph-index)))
    (cond-> {:position-x position-x :position-y position-y
             :advance-x advance-x :offset-x offset-x :offset-y offset-y
             :ink-x ink-x :ink-y ink-y :ink-w ink-w :ink-h ink-h
             :glyph-id+flags glyph-id+flags
             :cluster-start cluster-start :cluster-end cluster-end
             :span-source-start span-source-start
             :span-source-end span-source-end
             :span-glyph-start span-glyph-start
             :span-glyph-end span-glyph-end
             :source source
             :glyph-count glyph-count :span-count span-count}
      advance-y (assoc :advance-y advance-y)
      glyph-order-array (assoc :glyph-order glyph-order-array))))

(defn compact-result
  "Rich result → version-2 plane-backed result with rebuilt lines/index.

   Replaces retained glyph/cluster maps and removes top-level runs/clusters.
   Old rich lines are removed from these result indexes."
  [result]
  (let [{:keys [line-specs] :as shape} (build-shape result)
        planes (allocate-planes shape (:source result))
        lines (mapv #(assoc % ::planes planes) line-specs)]
    (-> result
        (assoc :text-layout/version 2
               :layout/planes planes
               :lines lines
               :line-index (into {} (map (juxt :line/id identity)) lines))
        (dissoc :runs :clusters))))

(defn- plane-ref
  "Line → plane owner or throws.

   Checked ownership boundary."
  [line]
  (or (::planes line)
      (throw (ex-info "Layout line has no plane owner" {:line/id (:line/id line)}))))

(defn- raw-glyph-indexes
  "Planes/span index → result-wide glyph indexes.

   Contiguous range or order indirection."
  [planes span-index]
  (let [start (u32-get (:span-glyph-start planes) span-index)
        end (u32-get (:span-glyph-end planes) span-index)]
    (if-let [order (:glyph-order planes)]
      (mapv #(u32-get order %) (range start end))
      (vec (range start end)))))

(defn- run-for-source
  "Line and source interval → first containing run or nil.

   Linear search in source order. Reconstructing many glyph maps can repeat
   run scans."
  [line source-start source-end]
  (let [runs (:runs line)
        indexed (::run-source-index line)]
    (when-let [{:keys [run-index]}
               (first (filter (fn [{run-start :source-start run-end :source-end}]
                                (and (<= run-start source-start)
                                     (<= source-end run-end)))
                              indexed))]
      (get runs run-index))))

(defn- glyph-view*
  "Line/global glyph index/dx/dy → rich glyph map.

   Decodes flags, local/absolute source, face metadata and translated
   position. Translation affects position; ink-bounds are read unchanged,
   which callers must account for when consuming translated views."
  [line glyph-index dx dy]
  (let [planes (plane-ref line)
        packed (u32-get (:glyph-id+flags planes) glyph-index)
        virtual? (not (zero? (bit-and packed kind-mask)))
        rtl? (not (zero? (bit-and packed rtl-mask)))
        local-start (u32-get (:cluster-start planes) glyph-index)
        local-end (u32-get (:cluster-end planes) glyph-index)
        [line-start _] (source-bounds (:source-range line))
        absolute-start (+ line-start local-start)
        absolute-end (+ line-start local-end)
        line-text (str (or (:text line) ""))
        character (subs line-text
                        (min (count line-text) local-start)
                        (min (count line-text) local-end))
        run (run-for-source line absolute-start absolute-end)
        ink-x-value (f32-get (:ink-x planes) glyph-index)
        base {:glyph-id (when-not virtual? (bit-and packed id-mask))
              :character character
              :cluster {:source-range [(line-index-value line absolute-start)
                                       (line-index-value line absolute-end)]}
              :position [(+ dx (view-number line
                                            (f32-get (:position-x planes)
                                                     glyph-index)))
                         (+ dy (view-number line
                                            (f32-get (:position-y planes)
                                                     glyph-index)))]
              :advance [(view-number line
                                     (f32-get (:advance-x planes) glyph-index))
                        (if-let [advance-y (:advance-y planes)]
                          (view-number line (f32-get advance-y glyph-index))
                          (if (::shaped? line) 0.0 0))]
              :offset [(view-number line
                                    (f32-get (:offset-x planes) glyph-index))
                       (view-number line
                                    (f32-get (:offset-y planes) glyph-index))]
              :ink-bounds (when-not (nan-value? ink-x-value)
                            {:x (view-number line ink-x-value)
                             :y (view-number line
                                             (f32-get (:ink-y planes)
                                                      glyph-index))
                             :w (view-number line
                                             (f32-get (:ink-w planes)
                                                      glyph-index))
                             :h (view-number line
                                             (f32-get (:ink-h planes)
                                                      glyph-index))})}]
    (cond-> base
      (::shaped? line)
      (assoc :glyph-id-kind (if virtual? :virtual/tab :font-glyph-index)
             :font-id (:font-id run)
             :font-revision (:font-revision run)
             :cluster-start local-start :cluster-end local-end
             :direction (if rtl? :rtl :ltr)))))

(defn line-glyphs
  "Line, optional translation → all rich glyph maps in line.

   Materializes a bounded range. Intended for explicit rich-view request."
  ([line] (line-glyphs line 0 0))
  ([line dx dy]
   (mapv #(glyph-view* line % dx dy)
         (range (:glyph-start line) (:glyph-end line)))))

(defn first-glyph-advance-x
  "Line → first advance or nil if empty.

   Single plane read."
  [line]
  (when (< (:glyph-start line 0) (:glyph-end line 0))
    (view-number line
                 (f32-get (:advance-x (plane-ref line))
                          (:glyph-start line)))))

(defn- span-selected?
  "Planes/span/start/end → span start lies in half-open source interval?

   Source-start ownership rule. Overlapping clusters that start before
   requested range are excluded."
  [planes span-index start end]
  (let [source-start (u32-get (:span-source-start planes) span-index)]
    (and (<= start source-start) (< source-start end))))

(declare glyph-indexes-in-source-range)

(defn glyphs-in-source-range
  "Line/range/optional translation → selected glyph maps, span/count
   statistics.

   Index selection then rich materialization. Selection cost includes
   scanning line spans."
  ([line source-range] (glyphs-in-source-range line source-range 0 0))
  ([line source-range dx dy]
   (let [{:keys [indexes] :as selected}
         (glyph-indexes-in-source-range line source-range)]
     (-> selected
         (dissoc :indexes)
         (assoc :glyphs (mapv #(glyph-view* line % dx dy) indexes))))))

(defn glyph-span-index-view
  "Line → compatibility span records with line-local glyph indexes.

   Converts global spans/order."
  [line]
  (let [planes (plane-ref line)]
    (into []
          (keep
           (fn [span-index]
             (let [indexes (mapv #(- % (:glyph-start line))
                                 (raw-glyph-indexes planes span-index))]
               (when (seq indexes)
                 {:source-start (u32-get (:span-source-start planes) span-index)
                  :source-end (u32-get (:span-source-end planes) span-index)
                  :glyph-start (reduce min indexes)
                  :glyph-end (inc (reduce max indexes))
                  :glyph-indexes indexes}))))
          (range (:cluster-start line) (:cluster-end line)))))

(defn- union-bounds
  "Bounds sequence → union or nil.

   Coordinate reductions."
  [bounds]
  (when (seq bounds)
    (let [x1 (reduce min (map :x bounds))
          y1 (reduce min (map :y bounds))
          x2 (reduce max (map #(+ (:x %) (:w %)) bounds))
          y2 (reduce max (map #(+ (:y %) (:h %)) bounds))]
      {:x x1 :y y1 :w (- x2 x1) :h (- y2 y1)})))

(defn- cluster-view
  "Line/span index → rich cluster bounds/source/direction/span and optional
   consumed caret stops.

   Reconstructs selected glyphs and bounds. Consumed source can generate one
   stop per code unit."
  [line span-index]
  (let [planes (plane-ref line)
        source-start (u32-get (:span-source-start planes) span-index)
        source-end (u32-get (:span-source-end planes) span-index)
        [line-start _] (source-bounds (:source-range line))
        local-source-start (- source-start line-start)
        local-source-end (- source-end line-start)
        indexes (raw-glyph-indexes planes span-index)
        glyphs (mapv #(glyph-view* line % 0 0) indexes)
        logical-line (:logical-bounds line)
        consumed-range (some-> (:consumed-range line) source-bounds)
        consumed? (= [source-start source-end] consumed-range)
        xs (mapcat (fn [glyph]
                     (let [[x _] (:position glyph)
                           [advance _] (:advance glyph)]
                       [x (+ x advance)]))
                   glyphs)
        left (if (seq xs) (reduce min xs)
                 (+ (:x logical-line 0) (:w logical-line 0)))
        right (if (seq xs) (reduce max xs) left)
        direction (or (:direction (first glyphs)) :ltr)
        source-range [(line-index-value line source-start)
                      (line-index-value line source-end)]
        logical-bounds {:x left :y (:y logical-line 0)
                        :w (Math/abs (- right left))
                        :h (:h logical-line 0)}
        ink-bounds (union-bounds (keep :ink-bounds glyphs))]
    (cond-> {:source-start local-source-start
             :source-end local-source-end
             :source-range source-range :direction direction
             :logical-bounds logical-bounds :ink-bounds ink-bounds
             :glyph-span (when (seq indexes)
                           [(- (reduce min indexes) (:glyph-start line))
                            (inc (- (reduce max indexes) (:glyph-start line)))])}
      consumed?
      (assoc :consumed? true
             :caret-stops
             (mapv (fn [offset]
                     {:index (line-index-value line offset)
                      :position [left (:y logical-line 0)]
                      :affinity (if (= offset source-end)
                                  :upstream :downstream)})
                   (range source-start (inc source-end)))))))

(defn line-clusters
  "Line → all rich cluster views.

   Span traversal. Intended for explicit geometry queries; allocates views."
  [line]
  (mapv #(cluster-view line %)
        (range (:cluster-start line) (:cluster-end line))))

(defn result-runs
  "Result → all compact runs in line order.

   Flattening projection."
  [result]
  (vec (mapcat :runs (:lines result))))

(defn rich-lines
  "Result → line views with glyph/cluster/span maps and internal keys
   removed.

   On-demand reconstruction. Intended for oracle/export views; not a
   low-allocation paint route."
  [result]
  (mapv (fn [line]
          (-> line
              (dissoc ::planes ::run-source-index ::shaped?)
              (assoc :glyphs (line-glyphs line)
                     :glyph-span-index (glyph-span-index-view line)
                     :clusters (line-clusters line))))
        (:lines result)))

(defn plane-coverage-check
  "Result → plane byte/count breakdown.

   Sums actual typed storage. Not total retained-memory measurement."
  [result]
  (let [planes (:layout/planes result)
        glyph-arrays (cond-> [[:position-x 4] [:position-y 4] [:advance-x 4]
                              [:offset-x 4] [:offset-y 4]
                              [:ink-x 4] [:ink-y 4] [:ink-w 4] [:ink-h 4]
                              [:glyph-id+flags 4] [:cluster-start 4]
                              [:cluster-end 4]]
                       (:advance-y planes) (conj [:advance-y 4]))
        span-arrays [[:span-source-start 4] [:span-source-end 4]
                     [:span-glyph-start 4] [:span-glyph-end 4]]
        glyph-bytes (reduce + 0 (map (fn [[k width]]
                                       (array-byte-length (get planes k) width))
                                     glyph-arrays))
        span-bytes (reduce + 0 (map (fn [[k width]]
                                      (array-byte-length (get planes k) width))
                                    span-arrays))
        order-bytes (if-let [order (:glyph-order planes)]
                      (array-byte-length order 4) 0)
        total (+ glyph-bytes span-bytes order-bytes)
        glyph-count (:glyph-count planes 0)
        span-count (:span-count planes 0)]
    {:plane-bytes total
     :glyph-bytes glyph-bytes
     :span-bytes span-bytes
     :glyph-order-bytes order-bytes
     :glyph-count glyph-count
     :span-count span-count
     :glyph-bytes-per-glyph (if (pos? glyph-count)
                              (/ glyph-bytes glyph-count) 0)
     :span-bytes-per-entry (if (pos? span-count)
                             (/ span-bytes span-count) 0)}))

(defn retained-rich-map?
  "Result → presence of rich glyph/cluster/span keys under retained
   lines/runs?

   Targeted structural check. Does not recursively search arbitrary
   additional result fields."
  [result]
  (boolean
   (some (fn [line]
           (or (contains? line :glyphs)
               (contains? line :clusters)
               (contains? line :glyph-span-index)
               (some #(contains? % :glyphs) (:runs line))))
         (:lines result))))

;; ---------------------------------------------------------------------------
;; The flat route's entry points. The retained vocabulary above is unchanged; these
;; are the only places a layout builder writes planes without a glyph map, and
;; the only place a renderer reads them without one.

(defn plane-builder
  "Glyph/span counts, Y-advance flag, order count → fresh arrays.

   Exact result-sized allocation. Intended for two-pass layout construction."
  [glyph-count span-count advance-y? order-count]
  (let [glyph-count (long glyph-count) span-count (long span-count)]
    (cond-> {:position-x (f32-array glyph-count)
             :position-y (f32-array glyph-count)
             :advance-x (f32-array glyph-count)
             :offset-x (f32-array glyph-count)
             :offset-y (f32-array glyph-count)
             :ink-x (f32-array glyph-count)
             :ink-y (f32-array glyph-count)
             :ink-w (f32-array glyph-count)
             :ink-h (f32-array glyph-count)
             :glyph-id+flags (u32-array glyph-count)
             :cluster-start (u32-array glyph-count)
             :cluster-end (u32-array glyph-count)
             :span-source-start (u32-array span-count)
             :span-source-end (u32-array span-count)
             :span-glyph-start (u32-array span-count)
             :span-glyph-end (u32-array span-count)
             :glyph-count glyph-count
             :span-count span-count}
      advance-y? (assoc :advance-y (f32-array glyph-count))
      (pos? (long order-count)) (assoc :glyph-order (u32-array order-count)))))

(defn put-glyph!
  "Builder/index and primitive glyph fields → same builder; writes row.

   Packs float fields and flags/sentinels. Many positional arguments are an
   internal ABI that requires coordinated callers."
  [b i px py ax ay ox oy ink? ix iy iw ih glyph-id tab? rtl? cluster-start
   cluster-end]
  (f32-set! (:position-x b) i px)
  (f32-set! (:position-y b) i py)
  (f32-set! (:advance-x b) i ax)
  (when-let [advance-y (:advance-y b)] (f32-set! advance-y i ay))
  (f32-set! (:offset-x b) i ox)
  (f32-set! (:offset-y b) i oy)
  (f32-set! (:ink-x b) i (if ink? ix no-ink))
  (f32-set! (:ink-y b) i (if ink? iy 0))
  (f32-set! (:ink-w b) i (if ink? iw 0))
  (f32-set! (:ink-h b) i (if ink? ih 0))
  (u32-set! (:glyph-id+flags b) i
            (bit-or (bit-and (long glyph-id) id-mask)
                    (if tab? kind-mask 0)
                    (if rtl? rtl-mask 0)))
  (u32-set! (:cluster-start b) i cluster-start)
  (u32-set! (:cluster-end b) i cluster-end)
  b)

(defn put-span!
  "Builder/index/source and glyph endpoints → same builder; writes span.

   Four column assignments."
  [b i source-start source-end glyph-start glyph-end]
  (u32-set! (:span-source-start b) i source-start)
  (u32-set! (:span-source-end b) i source-end)
  (u32-set! (:span-glyph-start b) i glyph-start)
  (u32-set! (:span-glyph-end b) i glyph-end)
  b)

(defn put-order!
  "Builder/index/glyph index → same builder; writes indirection.

   Direct assignment. Requires allocated order column."
  [b i glyph-index]
  (u32-set! (:glyph-order b) i glyph-index)
  b)

(defn finish-planes!
  "Builder/source → map with source attached.

   Logical sealing by convention. Arrays remain mutable; no physical freeze
   occurs."
  [b source]
  (assoc b :source source))

(defn glyph-indexes-in-source-range
  "Line/range → sorted distinct global glyph indexes, local span and visited
   count.

   Scans all line spans, collects selected indexes, deduplicates/sorts.
   Visited-glyphs reports selected glyphs, not all span-search work."
  [line source-range]
  (let [planes (plane-ref line)
        [start end] (source-bounds source-range)
        span-indexes (filterv #(span-selected? planes % start end)
                              (range (:cluster-start line) (:cluster-end line)))
        indexes (->> span-indexes
                     (mapcat #(raw-glyph-indexes planes %))
                     distinct sort vec)
        local-indexes (mapv #(- % (:glyph-start line)) indexes)]
    {:indexes indexes
     :glyph-span (when (seq local-indexes)
                   [(reduce min local-indexes)
                    (inc (reduce max local-indexes))])
     :visited-glyphs (count indexes)}))

(defn glyph-views
  "Line/explicit indexes/translation → rich glyph vector.

   Direct indexed reconstruction."
  [line indexes dx dy]
  (mapv #(glyph-view* line % dx dy) indexes))

(defn pack-glyphs!
  "Line, result-wide glyph indexes, translation and callback → traversal
   result; calls the callback with primitive glyph data.

   Callback: (f index glyph-id shaped? Tab? Font-id x y cluster-start
   cluster-end). Coordinates include dx/dy. Font identity uses the first
   source-ordered run containing the cluster, or nil for legacy lines. No
   rich glyph map is allocated."
  [line indexes dx dy f]
  (let [planes (plane-ref line)
        shaped? (boolean (::shaped? line))
        runs (:runs line)
        sorted (::run-source-index line)
        run-count (count sorted)
        [line-start _] (source-bounds (:source-range line))
        packed-plane (:glyph-id+flags planes)
        cs-plane (:cluster-start planes)
        ce-plane (:cluster-end planes)
        px-plane (:position-x planes)
        py-plane (:position-y planes)
        n (count indexes)]
    (loop [k 0 last-run -1]
      (when (< k n)
        (let [i (nth indexes k)
              packed (u32-get packed-plane i)
              tab? (not (zero? (bit-and packed kind-mask)))
              cs (u32-get cs-plane i)
              ce (u32-get ce-plane i)
              abs-start (+ line-start cs)
              abs-end (+ line-start ce)
              contains? (fn [ri]
                          (let [{run-start :source-start run-end :source-end}
                                (nth sorted ri)]
                            (and (<= run-start abs-start) (<= abs-end run-end))))
              run-pos (cond
                        (not shaped?) -1
                        (and (<= 0 last-run) (contains? last-run)) last-run
                        :else (loop [ri 0]
                                (cond (>= ri run-count) -1
                                      (contains? ri) ri
                                      :else (recur (inc ri)))))
              font-id (when (<= 0 run-pos)
                        (:font-id (get runs (:run-index (nth sorted run-pos)))))
              x (+ dx (view-number line (f32-get px-plane i)))
              y (+ dy (view-number line (f32-get py-plane i)))]
          (f i (bit-and packed id-mask) shaped? tab? font-id x y cs ce)
          (recur (inc k) run-pos))))
    nil))

;; --- equality with typed planes (the oracle consistency check's comparator) ------------

(defn- typed-array?
  "Value → array/view?

   Platform predicate. JVM accepts any array; JS ArrayBuffer views include
   DataView although array helpers expect indexed length. Current plane
   values are typed arrays."
  [v]
  #?(:clj (and (some? v) (.isArray (class v)))
     :cljs (js/ArrayBuffer.isView v)))

(defn- array-length
  "Array → length.

   Reflection/native property. Intended for intended arrays."
  [a]
  #?(:clj (java.lang.reflect.Array/getLength a) :cljs (.-length a)))

(defn- array-ref
  "Array/index → element.

   Generic reflection/native access. Intended for oracle comparisons."
  [a i]
  #?(:clj (java.lang.reflect.Array/get a (int i)) :cljs (aget a i)))

(defn- number=*
  "Two values → numeric equality including NaN=NaN.

   Exact equality plus numeric coercion/sentinel rule. Intended for stored
   plane comparison."
  [a b]
  (or (= a b)
      (and (number? a) (number? b)
           (or (== a b)
               (and (nan-value? (double a)) (nan-value? (double b)))))))

(defn- array=
  "Two arrays → elementwise equality?

   Shape check and early-exit scan. O(elements)."
  [a b]
  (and (typed-array? a) (typed-array? b)
       (= (array-length a) (array-length b))
       (loop [i 0]
         (cond (>= i (array-length a)) true
               (number=* (array-ref a i) (array-ref b i)) (recur (inc i))
               :else false))))

(defn planes=
  "Two plane maps → same keys and equal arrays/scalars?

   Array-aware structural comparison."
  [pa pb]
  (and (map? pa) (map? pb)
       (= (set (keys pa)) (set (keys pb)))
       (every? (fn [k]
                 (let [va (get pa k) vb (get pb k)]
                   (if (typed-array? va) (array= va vb) (= va vb))))
               (keys pa))))

(defn- strip-planes
  "Result → structural comparison view without plane
   references/proportionality counters.

   Removes duplicate array owners from lines/index. Intended for comparator
   scope."
  [result]
  (-> result
      (dissoc :layout/planes)
      (update :lines (fn [lines] (mapv #(dissoc % ::planes) lines)))
      (update :line-index (fn [index]
                            (into {} (map (fn [[k v]] [k (dissoc v ::planes)]))
                                  index)))
      (update :stats dissoc :proportionality)))

(defn result=
  "Two results → array-aware equality, or normal equality without planes.

   Compares planes once and stripped structure. Intentionally excludes work
   counters."
  [a b]
  (if (and (map? a) (map? b) (:layout/planes a) (:layout/planes b))
    (and (planes= (:layout/planes a) (:layout/planes b))
         (= (strip-planes a) (strip-planes b)))
    (= a b)))
