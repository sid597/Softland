(ns build.slug-font
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io])
  (:import [java.awt Font RenderingHints]
           [java.awt.font FontRenderContext]
           [java.awt.geom PathIterator]
           [java.io File]
           [java.nio ByteBuffer ByteOrder]))

(def ^:const curve-texture-width 4096)
(def ^:const band-texture-width 4096)
(def ^:const band-overlap (/ 1.0 1024.0))
(def ^:const max-band-candidates 32)
(def ^:const epsilon 1.0e-6)

(def ^:private frc
  (delay
    (FontRenderContext.
      nil
      RenderingHints/VALUE_TEXT_ANTIALIAS_OFF
      RenderingHints/VALUE_FRACTIONALMETRICS_OFF)))

(defn- read-json [path]
  (json/read-str (slurp path) :key-fn keyword))

(defn- write-json! [path data]
  (spit path (json/write-str data)))

(defn- ensure-parent-dir! [path]
  (some-> path io/file .getParentFile .mkdirs)
  path)

(defn- approx= [a b]
  (< (Math/abs (- (double a) (double b))) epsilon))

(defn- midpoint [[x1 y1] [x2 y2]]
  [(/ (+ x1 x2) 2.0) (/ (+ y1 y2) 2.0)])

(defn- line->quad [p1 p3]
  {:p1 p1 :p2 (midpoint p1 p3) :p3 p3})

(defn- closed? [p1 p2]
  (and (approx= (first p1) (first p2))
       (approx= (second p1) (second p2))))

(defn- close-contour [segments current start]
  (cond-> segments
    (and current start (not (closed? current start)))
    (conj (line->quad current start))))

(defn- point-from [coords idx]
  [(aget coords idx) (- (aget coords (inc idx)))])

(defn- finalize-contour [acc contour current start]
  (cond-> acc
    (seq contour)
    (conj (close-contour contour current start))))

(defn- outline->contours [shape]
  (let [pi (.getPathIterator shape nil)
        coords (double-array 6)]
    (loop [acc []
           contour []
           current nil
           start nil]
      (if (.isDone pi)
        (vec (remove empty? (finalize-contour acc contour current start)))
        (let [seg (.currentSegment pi coords)]
          (.next pi)
          (cond
            (= seg PathIterator/SEG_MOVETO)
            (let [p (point-from coords 0)]
              (recur (finalize-contour acc contour current start) [] p p))

            (= seg PathIterator/SEG_LINETO)
            (let [p (point-from coords 0)]
              (recur acc (conj contour (line->quad current p)) p start))

            (= seg PathIterator/SEG_QUADTO)
            (let [ctrl (point-from coords 0)
                  p (point-from coords 2)]
              (recur acc (conj contour {:p1 current :p2 ctrl :p3 p}) p start))

            (= seg PathIterator/SEG_CLOSE)
            (recur (finalize-contour acc contour current start) [] nil nil)

            (= seg PathIterator/SEG_CUBICTO)
            (throw (ex-info "Cubic outlines are not supported by the Slug asset generator."
                            {:segment seg}))

            :else
            (throw (ex-info "Unexpected path segment while generating Slug assets."
                            {:segment seg}))))))))

(defn- load-font [path]
  (.deriveFont
    (Font/createFont Font/TRUETYPE_FONT (io/file path))
    (float 1.0)))

(defn- glyph-outline [^Font font {:keys [index unicode]}]
  (let [glyph-vector (if (some? index)
                       (.createGlyphVector font ^FontRenderContext @frc
                                           (int-array [(int index)]))
                       (.createGlyphVector font ^FontRenderContext @frc
                                           (Character/toChars (int unicode))))]
    (.getGlyphOutline glyph-vector 0)))

(defn- flatten-curves [contours]
  (vec (mapcat identity contours)))

(defn- curve-bounds [{:keys [p1 p2 p3]}]
  (let [xs [(first p1) (first p2) (first p3)]
        ys [(second p1) (second p2) (second p3)]]
    {:min-x (apply min xs)
     :max-x (apply max xs)
     :min-y (apply min ys)
     :max-y (apply max ys)}))

(defn- merge-bounds [bounds]
  (if (seq bounds)
    {:min-x (apply min (map :min-x bounds))
     :max-x (apply max (map :max-x bounds))
     :min-y (apply min (map :min-y bounds))
     :max-y (apply max (map :max-y bounds))}
    {:min-x 0.0 :max-x 0.0 :min-y 0.0 :max-y 0.0}))

(defn- horizontal-line? [{:keys [p1 p2 p3]}]
  (let [ys [(second p1) (second p2) (second p3)]]
    (every? #(approx= (first ys) %) (rest ys))))

(defn- vertical-line? [{:keys [p1 p2 p3]}]
  (let [xs [(first p1) (first p2) (first p3)]]
    (every? #(approx= (first xs) %) (rest xs))))

(defn- band-intervals [min-val max-val band-count]
  (let [span (max epsilon (- max-val min-val))
        thickness (/ span band-count)]
    (mapv (fn [idx]
            (let [start (+ min-val (* idx thickness))
                  end (if (= idx (dec band-count))
                        max-val
                        (+ start thickness))]
              [start end]))
          (range band-count))))

(defn- curve-band-overlaps?
  [{:keys [min-val max-val]} [band-start band-end]]
  (and (>= max-val (- band-start band-overlap))
       (<= min-val (+ band-end band-overlap))))

(defn- curve-max-x [curve]
  (:max-x (curve-bounds curve)))

(defn- curve-max-y [curve]
  (:max-y (curve-bounds curve)))

(defn- choose-band-layout [curves axis]
  (if (empty? curves)
    {:count 1
     :min 0.0
     :max 0.0
     :scale 0.0
     :offset 0.0
     :lists [[]]}
    (let [curve-stats (mapv (fn [curve]
                              (let [bounds (curve-bounds curve)]
                                (case axis
                                  :horizontal {:min-val (:min-y bounds)
                                               :max-val (:max-y bounds)
                                               :sort-max (curve-max-x curve)
                                               :skip? (horizontal-line? curve)}
                                  :vertical {:min-val (:min-x bounds)
                                             :max-val (:max-x bounds)
                                             :sort-max (curve-max-y curve)
                                             :skip? (vertical-line? curve)})))
                            curves)
          candidates (for [band-count (range 1 (inc (min max-band-candidates (max 1 (count curves)))))]
                       (let [min-val (apply min (map :min-val curve-stats))
                             max-val (apply max (map :max-val curve-stats))
                             intervals (band-intervals min-val max-val band-count)
                             lists (mapv (fn [interval]
                                           (->> curve-stats
                                                (keep-indexed
                                                  (fn [idx {:keys [skip?] :as stat}]
                                                    (when (and (not skip?)
                                                               (curve-band-overlaps? stat interval))
                                                      idx)))
                                                (sort-by (fn [idx] (get-in curve-stats [idx :sort-max])) >)
                                                vec))
                                         intervals)
                             max-populated (reduce max 0 (map count lists))
                             total-refs (reduce + 0 (map count lists))
                             span (max epsilon (- max-val min-val))
                             scale (/ band-count span)
                             offset (- (* min-val scale))]
                         {:count band-count
                          :min min-val
                          :max max-val
                          :scale scale
                          :offset offset
                          :lists lists
                          :score [max-populated total-refs band-count]}))
          best (first (sort-by :score candidates))]
      (dissoc best :score))))

(defn- curve-texels [curves]
  (vec
    (mapcat
      (fn [{:keys [p1 p2 p3]}]
        [[(double (first p1))
          (double (second p1))
          (double (first p2))
          (double (second p2))]
         [(double (first p3))
          (double (second p3))
          0.0
          0.0]])
      curves)))

(defn- pack-row-segments [segments width]
  (loop [remaining segments
         x 0
         y 0
         placements []]
    (if (empty? remaining)
      {:height (max 1 (if (zero? x) y (inc y)))
       :placements placements}
      (let [{:keys [id entries]} (first remaining)
            seg-len (count entries)]
        (when (> seg-len width)
          (throw (ex-info "Segment is wider than the fixed Slug texture width."
                          {:id id :width width :segment-length seg-len})))
        (if (> (+ x seg-len) width)
          (recur remaining 0 (inc y) placements)
          (recur (rest remaining)
                 (+ x seg-len)
                 y
                 (conj placements {:id id :x x :y y :entries entries})))))))

(defn- build-band-entries [curve-positions horizontal-layout vertical-layout]
  (let [bands (vec (concat (:lists horizontal-layout) (:lists vertical-layout)))
        header-count (count bands)]
    (loop [entries (vec (repeat header-count nil))
           bands bands
           idx 0
           offsets {}]
      (if (empty? bands)
        entries
        (let [curve-idxs (first bands)]
          (if (empty? curve-idxs)
            (recur (assoc entries idx [0 0]) (rest bands) (inc idx) offsets)
            (if-let [offset (get offsets curve-idxs)]
              (recur (assoc entries idx [(count curve-idxs) offset]) (rest bands) (inc idx) offsets)
              (let [offset (count entries)
                    curve-locs (mapv curve-positions curve-idxs)
                    new-entries (into entries curve-locs)]
                (recur (assoc new-entries idx [(count curve-idxs) offset])
                       (rest bands)
                       (inc idx)
                       (assoc offsets curve-idxs offset))))))))))

(defn- glyph-sample-bounds [curve-bounds plane-bounds]
  (if (and curve-bounds (not (and (approx= (:min-x curve-bounds) (:max-x curve-bounds))
                                  (approx= (:min-y curve-bounds) (:max-y curve-bounds)))))
    {:left (:min-x curve-bounds)
     :right (:max-x curve-bounds)
     :top (:max-y curve-bounds)
     :bottom (:min-y curve-bounds)}
    plane-bounds))

(defn- compile-glyph [^Font font font-id glyph]
  (let [outline (glyph-outline font glyph)
        contours (outline->contours outline)
        curves (flatten-curves contours)
        bounds (when (seq curves)
                 (merge-bounds (map curve-bounds curves)))
        sample-bounds (glyph-sample-bounds bounds (:planeBounds glyph))
        horizontal-layout (choose-band-layout curves :horizontal)
        vertical-layout (choose-band-layout curves :vertical)]
    {:id (if font-id
           [font-id (or (:index glyph) (:unicode glyph))]
           (or (:index glyph) (:unicode glyph)))
     :fontId font-id
     :index (:index glyph)
     :unicode (:unicode glyph)
     :advance (:advance glyph)
     :planeBounds (:planeBounds glyph)
     :sampleBounds sample-bounds
     :curves curves
     :curve-texels (curve-texels curves)
     :horizontal-layout horizontal-layout
     :vertical-layout vertical-layout}))

(defn- pack-glyphs [glyphs]
  (let [curve-segments (mapv (fn [{:keys [id curve-texels]}]
                               {:id id :entries curve-texels})
                             glyphs)
        curve-pack (pack-row-segments curve-segments curve-texture-width)
        curve-placement-by-id (into {}
                                    (map (juxt :id identity))
                                    (:placements curve-pack))
        glyphs-with-band-data
        (mapv
          (fn [{:keys [id curves horizontal-layout vertical-layout] :as glyph}]
            (let [{:keys [x y]} (get curve-placement-by-id id)
                  curve-positions (into {}
                                        (map-indexed
                                          (fn [idx _]
                                            [idx [(+ x (* idx 2)) y]]))
                                        curves)
                  band-entries (build-band-entries curve-positions horizontal-layout vertical-layout)]
              (assoc glyph
                     :curve-position {:x x :y y}
                     :band-entries band-entries)))
          glyphs)
        band-segments (mapv (fn [{:keys [id band-entries]}]
                              {:id id :entries band-entries})
                            glyphs-with-band-data)
        band-pack (pack-row-segments band-segments band-texture-width)
        band-placement-by-id (into {}
                                   (map (juxt :id identity))
                                   (:placements band-pack))]
    {:glyphs
     (mapv
       (fn [{:keys [id fontId index unicode advance planeBounds sampleBounds horizontal-layout vertical-layout band-entries] :as glyph}]
         (let [{curve-x :x curve-y :y} (:curve-position glyph)
               {band-x :x band-y :y} (get band-placement-by-id id)
               packed-band-max-y (bit-or (bit-and (dec (:count horizontal-layout)) 0xFFFF) 0)]
           (cond-> (array-map)
             (some? fontId) (assoc :fontId fontId)
             (some? index) (assoc :index index)
             (some? unicode) (assoc :unicode unicode)
             true (assoc :advance advance
                         :planeBounds planeBounds
                         :sampleBounds sampleBounds
                         :slug {:glyphLoc {:x band-x :y band-y}
                                :curveLoc {:x curve-x :y curve-y}
                                :banding {:scaleX (:scale vertical-layout)
                                          :scaleY (:scale horizontal-layout)
                                          :offsetX (:offset vertical-layout)
                                          :offsetY (:offset horizontal-layout)}
                                :bandMax {:x (dec (:count vertical-layout))
                                          :y (dec (:count horizontal-layout))}
                                :packedBandMeta packed-band-max-y}))))
       glyphs-with-band-data)
     :curve-pack curve-pack
     :band-pack band-pack}))

(defn- float->half-bits [value]
  (let [fbits (Float/floatToIntBits (float value))
        sign (bit-and (unsigned-bit-shift-right fbits 16) 0x8000)
        val (bit-and fbits 0x7fffffff)]
    (cond
      (>= val 0x7f800000)
      (bit-or sign (if (zero? (bit-and fbits 0x007fffff)) 0x7c00 0x7e00))

      (> val 0x477fefff)
      (bit-or sign 0x7c00)

      (< val 0x33000001)
      sign

      (< val 0x38800000)
      (let [mantissa (bit-or (bit-and val 0x7fffff) 0x800000)
            exp (- 113 (unsigned-bit-shift-right val 23))
            rounded (+ mantissa (bit-shift-left 1 (max 0 (dec exp))))
            half (unsigned-bit-shift-right rounded exp)]
        (bit-or sign (unsigned-bit-shift-right half 13)))

      :else
      (let [rounded (+ val 0x00001000)
            exp (unsigned-bit-shift-right rounded 23)
            mantissa (bit-and rounded 0x7fffff)]
        (bit-or sign
                (bit-shift-left (- exp 112) 10)
                (unsigned-bit-shift-right mantissa 13))))))

(defn- write-curve-texture! [path width height placements]
  (let [buffer (doto (ByteBuffer/allocate (* width height 8))
                 (.order ByteOrder/LITTLE_ENDIAN))]
    (doseq [{:keys [x y entries]} placements]
      (doseq [[offset [a b c d]] (map-indexed vector entries)]
        (let [pixel-index (+ x offset (* y width))
              byte-index (* pixel-index 8)]
          (.putShort buffer byte-index (unchecked-short (int (float->half-bits a))))
          (.putShort buffer (+ byte-index 2) (unchecked-short (int (float->half-bits b))))
          (.putShort buffer (+ byte-index 4) (unchecked-short (int (float->half-bits c))))
          (.putShort buffer (+ byte-index 6) (unchecked-short (int (float->half-bits d)))))))
    (ensure-parent-dir! path)
    (with-open [out (io/output-stream path)]
      (.write out (.array buffer)))))

(defn- write-band-texture! [path width height placements]
  (let [buffer (doto (ByteBuffer/allocate (* width height 4))
                 (.order ByteOrder/LITTLE_ENDIAN))]
    (doseq [{:keys [x y entries]} placements]
      (doseq [[offset [a b]] (map-indexed vector entries)]
        (let [pixel-index (+ x offset (* y width))
              byte-index (* pixel-index 4)]
          (.putShort buffer byte-index (unchecked-short (int a)))
          (.putShort buffer (+ byte-index 2) (unchecked-short (int b))))))
    (ensure-parent-dir! path)
    (with-open [out (io/output-stream path)]
      (.write out (.array buffer)))))

(defn default-config []
  {:font-path "resources/public/fonts/dejavu_sans_mono.ttf"
   :metrics-path "resources/public/fonts/dejavu_sans_mono_atlas.json"
   :meta-out "resources/public/fonts/dejavu_sans_mono_slug_meta.json"
   :curve-out "resources/public/fonts/dejavu_sans_mono_slug_curve.bin"
   :band-out "resources/public/fonts/dejavu_sans_mono_slug_band.bin"})

(defn write-font-assets!
  [{:keys [font-path font-paths font-ids metrics-path meta-out curve-out band-out]
    :or {font-path (:font-path (default-config))
         metrics-path (:metrics-path (default-config))
         meta-out (:meta-out (default-config))
         curve-out (:curve-out (default-config))
         band-out (:band-out (default-config))}}]
  (let [metrics-json (read-json metrics-path)
        variants (:variants metrics-json)
        glyphs (if (seq variants)
                 (let [font-paths (vec font-paths)
                       font-ids (vec font-ids)]
                   (when-not (= (count variants) (count font-paths) (count font-ids))
                     (throw (ex-info "Slug variant inputs must match atlas variants."
                                     {:variants (count variants)
                                      :font-paths (count font-paths)
                                      :font-ids (count font-ids)})))
                   (vec
                     (mapcat (fn [variant path font-id]
                               (let [font (load-font path)]
                                 (mapv (partial compile-glyph font font-id)
                                       (:glyphs variant))))
                             variants font-paths font-ids)))
                 (let [font (load-font font-path)]
                   (mapv (partial compile-glyph font nil) (:glyphs metrics-json))))
        {:keys [glyphs curve-pack band-pack]} (pack-glyphs glyphs)
        meta {:version 1
              :metrics (or (:metrics metrics-json)
                           (get-in metrics-json [:variants 0 :metrics]))
              :curveTexture {:width curve-texture-width
                             :height (:height curve-pack)
                             :format "rgba16float"}
              :bandTexture {:width band-texture-width
                            :height (:height band-pack)
                            :format "rg16uint"}
              :glyphs glyphs}]
    (write-json! meta-out meta)
    (write-curve-texture! curve-out curve-texture-width (:height curve-pack) (:placements curve-pack))
    (write-band-texture! band-out band-texture-width (:height band-pack) (:placements band-pack))
    {:meta-out meta-out
     :curve-out curve-out
     :band-out band-out
     :glyph-count (count glyphs)
     :curve-height (:height curve-pack)
     :band-height (:height band-pack)}))

(defn -main [& _args]
  (println (pr-str (write-font-assets! (default-config)))))
