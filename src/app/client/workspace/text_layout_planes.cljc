(ns app.client.workspace.text-layout-planes
  "Columnar retained storage and derived views for Contract-T layout results.

   Raw arrays are private to this namespace.  Callers receive line/result
   handles and rich values reconstructed at the accessor boundary; no derived
   glyph or cluster map is retained in the layout result."
  )

(def ^:private id-mask 0x3fffffff)
(def ^:private kind-mask 0x40000000)
(def ^:private rtl-mask 0x80000000)
(def ^:private no-ink #?(:clj Float/NaN :cljs js/NaN))

(declare register-live-planes!)

(defn- f32-array [n]
  #?(:clj (float-array n) :cljs (js/Float32Array. n)))

(defn- u32-array [n]
  #?(:clj (int-array n) :cljs (js/Uint32Array. n)))

(defn- f32-set! [a i value]
  #?(:clj (aset-float ^floats a i (float value))
     :cljs (aset a i (double value))))

(defn- u32-set! [a i value]
  #?(:clj (aset-int ^ints a i (unchecked-int (long value)))
     :cljs (aset a i value)))

(defn- f32-get [a i]
  #?(:clj (double (aget ^floats a i))
     :cljs (aget a i)))

(defn- view-number [line value]
  #?(:clj (if (and (not (::shaped? line))
                    (not (Double/isNaN value))
                    (== value (Math/rint value)))
             (long value)
             value)
     :cljs value))

(defn- u32-get [a i]
  #?(:clj (bit-and 0xffffffff (long (aget ^ints a i)))
     :cljs (aget a i)))

(defn- array-byte-length [a bytes-per-entry]
  #?(:clj (* (alength a) bytes-per-entry)
     :cljs (.-byteLength a)))

(defn- nan-value? [value]
  #?(:clj (Double/isNaN value)
     :cljs (js/Number.isNaN value)))

(defn- source-offset [index]
  (cond
    (map? index) (:offset index)
    (and (vector? index) (= :header (first index))) (nth index 2)
    :else index))

(defn- source-bounds [source-range]
  (if (and (vector? source-range) (= :header (first source-range)))
    (nth source-range 2)
    (mapv source-offset source-range)))

(defn- tagged-index [offset]
  {:index-space {:domain :utf-16-code-unit :version 1}
   :offset offset})

(defn- line-index-value [line offset]
  (if (= :header (first (:source-range line)))
    [:header (second (:source-range line)) offset]
    (tagged-index offset)))

(defn- exact-range? [{:keys [glyph-start glyph-end glyph-indexes]}]
  (= (vec (range glyph-start glyph-end)) (vec glyph-indexes)))

(defn- line-cluster-inputs [line]
  (if (seq (:clusters line))
    (:clusters line)
    (mapv (fn [glyph]
            {:source-range (get-in glyph [:cluster :source-range])})
          (:glyphs line))))

(defn- line-span-inputs [line]
  (if (seq (:glyph-span-index line))
    (:glyph-span-index line)
    (mapv (fn [i glyph]
            (let [[start end] (source-bounds
                               (get-in glyph [:cluster :source-range]))]
              {:source-start start :source-end end
               :glyph-start i :glyph-end (inc i) :glyph-indexes [i]}))
          (range) (:glyphs line))))

(defn- non-monotonic-result? [lines]
  (boolean
   (some (fn [line]
           (some (complement exact-range?) (line-span-inputs line)))
         lines)))

(defn- run-records [line glyph-base]
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

(defn- run-source-index [runs]
  (->> runs
       (map-indexed
        (fn [i run]
          (let [[start end] (source-bounds (:source-range run))]
            {:source-start start :source-end end :run-index i})))
       (sort-by (juxt :source-start :source-end))
       vec))

(defn- build-shape [result]
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

(defn- pack-id [glyph]
  (let [glyph-id (long (or (:glyph-id glyph) 0))]
    (bit-or (bit-and glyph-id id-mask)
            (if (= :virtual/tab (:glyph-id-kind glyph)) kind-mask 0)
            (if (= :rtl (:direction glyph)) rtl-mask 0))))

(defn- allocate-planes [{:keys [glyphs spans glyph-order non-monotonic?]} source]
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
  "Replace retained glyph/cluster maps with typed planes and rebuild every
   line-bearing index so no old rich line remains reachable."
  [result]
  (let [{:keys [line-specs] :as shape} (build-shape result)
        planes (allocate-planes shape (:source result))
        lines (mapv #(assoc % ::planes planes) line-specs)]
    (register-live-planes! planes)
    (-> result
        (assoc :text-layout/version 2
               :layout/planes planes
               :lines lines
               :line-index (into {} (map (juxt :line/id identity)) lines))
        (dissoc :runs :clusters))))

(defn- plane-ref [line]
  (or (::planes line)
      (throw (ex-info "Layout line has no plane owner" {:line/id (:line/id line)}))))

(defn- raw-glyph-indexes [planes span-index]
  (let [start (u32-get (:span-glyph-start planes) span-index)
        end (u32-get (:span-glyph-end planes) span-index)]
    (if-let [order (:glyph-order planes)]
      (mapv #(u32-get order %) (range start end))
      (vec (range start end)))))

(defn- run-for-source [line source-start source-end]
  (let [runs (:runs line)
        indexed (::run-source-index line)]
    (when-let [{:keys [run-index]}
               (first (filter (fn [{run-start :source-start run-end :source-end}]
                                (and (<= run-start source-start)
                                     (<= source-end run-end)))
                              indexed))]
      (get runs run-index))))

(defn- glyph-view* [line glyph-index dx dy]
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
  ([line] (line-glyphs line 0 0))
  ([line dx dy]
   (mapv #(glyph-view* line % dx dy)
         (range (:glyph-start line) (:glyph-end line)))))

(defn first-glyph-advance-x [line]
  (when (< (:glyph-start line 0) (:glyph-end line 0))
    (view-number line
                 (f32-get (:advance-x (plane-ref line))
                          (:glyph-start line)))))

(defn- span-selected? [planes span-index start end]
  (let [source-start (u32-get (:span-source-start planes) span-index)]
    (and (<= start source-start) (< source-start end))))

(defn glyphs-in-source-range
  ([line source-range] (glyphs-in-source-range line source-range 0 0))
  ([line source-range dx dy]
   (let [planes (plane-ref line)
         [start end] (source-bounds source-range)
         span-indexes (filterv #(span-selected? planes % start end)
                               (range (:cluster-start line) (:cluster-end line)))
         indexes (->> span-indexes
                      (mapcat #(raw-glyph-indexes planes %))
                      distinct sort vec)
         local-indexes (mapv #(- % (:glyph-start line)) indexes)]
     {:glyphs (mapv #(glyph-view* line % dx dy) indexes)
      :glyph-span (when (seq local-indexes)
                    [(reduce min local-indexes)
                     (inc (reduce max local-indexes))])
      :visited-glyphs (count indexes)})))

(defn glyph-span-index-view [line]
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

(defn- union-bounds [bounds]
  (when (seq bounds)
    (let [x1 (reduce min (map :x bounds))
          y1 (reduce min (map :y bounds))
          x2 (reduce max (map #(+ (:x %) (:w %)) bounds))
          y2 (reduce max (map #(+ (:y %) (:h %)) bounds))]
      {:x x1 :y y1 :w (- x2 x1) :h (- y2 y1)})))

(defn- cluster-view [line span-index]
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

(defn line-clusters [line]
  (mapv #(cluster-view line %)
        (range (:cluster-start line) (:cluster-end line))))

(defn result-runs [result]
  (vec (mapcat :runs (:lines result))))

(defn rich-lines [result]
  (mapv (fn [line]
          (-> line
              (dissoc ::planes ::run-source-index ::shaped?)
              (assoc :glyphs (line-glyphs line)
                     :glyph-span-index (glyph-span-index-view line)
                     :clusters (line-clusters line))))
        (:lines result)))

(defn plane-census [result]
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

(defn retained-rich-map? [result]
  (boolean
   (some (fn [line]
           (or (contains? line :glyphs)
               (contains? line :clusters)
               (contains? line :glyph-span-index)
               (some #(contains? % :glyphs) (:runs line))))
         (:lines result))))

(defonce ^:private !live-plane-refs
  #?(:clj nil :cljs (atom [])))

(defn register-live-planes! [planes]
  #?(:clj nil
     :cljs
     (when (exists? js/WeakRef)
       (swap! !live-plane-refs
              (fn [refs]
                (let [refs (conj refs (js/WeakRef. planes))]
                  ;; The receipt instrument must not become its own unbounded
                  ;; metadata leak during long editing sessions.
                  (if (> (count refs) 4096)
                    (into [] (filter #(.deref %)) refs)
                    refs))))))
  planes)

(defn live-plane-census
  "Browser receipt surface: census only plane owners still live after GC.
   Weak references make the instrument incapable of extending retention."
  []
  #?(:clj {:layout-count 0 :plane-bytes 0 :glyph-count 0 :span-count 0}
     :cljs
     (let [live (into [] (keep (fn [ref] (.deref ref))) @!live-plane-refs)
           _ (reset! !live-plane-refs (mapv #(js/WeakRef. %) live))
           rows (map #(plane-census {:layout/planes %}) live)]
       {:layout-count (count live)
        :plane-bytes (reduce + 0 (map :plane-bytes rows))
        :glyph-bytes (reduce + 0 (map :glyph-bytes rows))
        :span-bytes (reduce + 0 (map :span-bytes rows))
        :glyph-order-bytes (reduce + 0 (map :glyph-order-bytes rows))
        :glyph-count (reduce + 0 (map :glyph-count rows))
        :span-count (reduce + 0 (map :span-count rows))})))

#?(:cljs
   (let [api (or (aget js/globalThis "__softlandLayoutRetention") (js-obj))]
     (aset api "planeCensus" (fn [] (clj->js (live-plane-census))))
     (aset js/globalThis "__softlandLayoutRetention" api)))
