(ns build.slug-font
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io])
  (:import [java.awt Font RenderingHints]
           [java.awt.font FontRenderContext]
           [java.awt.geom PathIterator]
           [java.nio ByteBuffer ByteOrder]))

(def ^:const curve-texture-width 4096)
(def ^:const band-texture-width 4096)
(def ^:const band-overlap (/ 1.0 1024.0))
(def ^:const max-band-candidates 32)
(def ^:const epsilon 1.0e-6)

(def ^:private outline-frc
  (delay
    (FontRenderContext.
      nil
      RenderingHints/VALUE_TEXT_ANTIALIAS_OFF
      RenderingHints/VALUE_FRACTIONALMETRICS_OFF)))

(def ^:private metrics-frc
  (delay
    (FontRenderContext.
      nil
      RenderingHints/VALUE_TEXT_ANTIALIAS_OFF
      RenderingHints/VALUE_FRACTIONALMETRICS_ON)))

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

(defn- glyph-vector [^Font font ^FontRenderContext context unicode]
  (.createGlyphVector font context (Character/toChars (int unicode))))

(defn- glyph-outline [^Font font unicode]
  (let [glyph-vector (glyph-vector font @outline-frc unicode)]
    (.getGlyphOutline glyph-vector 0)))

(defn- glyph-advance [^Font font unicode]
  (-> (glyph-vector font @metrics-frc unicode)
      (.getGlyphMetrics 0)
      .getAdvanceX
      double))

(defn- outline-plane-bounds [outline]
  (let [bounds (.getBounds2D outline)
        width (.getWidth bounds)
        height (.getHeight bounds)]
    (when (and (pos? width) (pos? height))
      {:left (.getMinX bounds)
       :bottom (- (.getMaxY bounds))
       :right (.getMaxX bounds)
       :top (- (.getMinY bounds))})))

(defn- font-line-metrics [^Font font]
  (let [line-metrics (.getLineMetrics font "Ag" ^FontRenderContext @metrics-frc)
        underline-thickness (double (.getUnderlineThickness line-metrics))
        underline-offset (double (.getUnderlineOffset line-metrics))]
    (array-map
      :emSize 1.0
      :lineHeight (double (.getHeight line-metrics))
      :ascender (double (.getAscent line-metrics))
      :descender (- (double (.getDescent line-metrics)))
      :underlineY (- (+ underline-offset (/ underline-thickness 2.0)))
      :underlineThickness underline-thickness)))

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

(defn- compile-glyph [^Font font unicode]
  (let [outline (glyph-outline font unicode)
        contours (outline->contours outline)
        curves (flatten-curves contours)
        bounds (when (seq curves)
                 (merge-bounds (map curve-bounds curves)))
        plane-bounds (outline-plane-bounds outline)
        sample-bounds (glyph-sample-bounds bounds plane-bounds)
        horizontal-layout (choose-band-layout curves :horizontal)
        vertical-layout (choose-band-layout curves :vertical)]
    {:id unicode
     :unicode unicode
     :advance (glyph-advance font unicode)
     :planeBounds plane-bounds
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
   :codepoints
   [32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54
    55 56 57 58 59 60 61 62 63 64 65 66 67 68 69 70 71 72 73 74 75 76 77
    78 79 80 81 82 83 84 85 86 87 88 89 90 91 92 93 94 95 96 97 98 99 100
    101 102 103 104 105 106 107 108 109 110 111 112 113 114 115 116 117 118
    119 120 121 122 123 124 125 126 160 161 162 163 164 165 166 167 168 169
    170 171 172 173 174 175 176 177 178 179 180 181 182 183 184 185 186 187
    188 189 190 191 192 193 194 195 196 197 198 199 200 201 202 203 204 205
    206 207 208 209 210 211 212 213 214 215 216 217 218 219 220 221 222 223
    224 225 226 227 228 229 230 231 232 233 234 235 236 237 238 239 240 241
    242 243 244 245 246 247 248 249 250 251 252 253 254 255 8208 8209 8210
    8211 8212 8213 8214 8215 8216 8217 8218 8219 8220 8221 8222 8223 8224
    8225 8226 8227 8230 8592 8593 8594 8595 8596 8597 8598 8599 8600 8601
    8602 8603 8604 8605 8606 8607 8608 8609 8610 8611 8612 8613 8614 8615
    8616 8617 8618 8619 8620 8621 8622 8623 8624 8625 8626 8627 8628 8629
    8630 8631 8632 8633 8634 8635 8636 8637 8638 8639 8640 8641 8642 8643
    8644 8645 8646 8647 8648 8649 8650 8651 8652 8653 8654 8655 8656 8657
    8658 8659 8660 8661 8662 8663 8664 8665 8666 8667 8668 8669 8670 8671
    8672 8673 8674 8675 8676 8677 8678 8679 8680 8681 8682 8683 8684 8685
    8686 8687 8688 8689 8690 8691 8692 8693 8694 8695 8696 8697 8698 8699
    8700 8701 8702 8703 8866 9472 9473 9474 9475 9476 9477 9478 9479 9480
    9481 9482 9483 9484 9485 9486 9487 9488 9489 9490 9491 9492 9493 9494
    9495 9496 9497 9498 9499 9500 9501 9502 9503 9504 9505 9506 9507 9508
    9509 9510 9511 9512 9513 9514 9515 9516 9517 9518 9519 9520 9521 9522
    9523 9524 9525 9526 9527 9528 9529 9530 9531 9532 9533 9534 9535 9536
    9537 9538 9539 9540 9541 9542 9543 9544 9545 9546 9547 9548 9549 9550
    9551 9552 9553 9554 9555 9556 9557 9558 9559 9560 9561 9562 9563 9564
    9565 9566 9567 9568 9569 9570 9571 9572 9573 9574 9575 9576 9577 9578
    9579 9580 9581 9582 9583 9584 9585 9586 9587 9588 9589 9590 9591 9592
    9593 9594 9595 9596 9597 9598 9599 9600 9601 9602 9603 9604 9605 9606
    9607 9608 9609 9610 9611 9612 9613 9614 9615 9616 9617 9618 9619 9620
    9621 9622 9623 9624 9625 9626 9627 9628 9629 9630 9631 9632 9633 9634
    9635 9636 9637 9638 9639 9640 9641 9642 9643 9644 9645 9646 9647 9648
    9649 9650 9651 9652 9653 9654 9655 9656 9657 9658 9659 9660 9661 9662
    9663 9664 9665 9666 9667 9668 9669 9670 9671 9672 9673 9674 9675 9676
    9677 9678 9679 9680 9681 9682 9683 9684 9685 9686 9687 9688 9689 9690
    9691 9692 9693 9694 9695 9696 9697 9698 9699 9700 9701 9702 9703 9704
    9705 9706 9707 9708 9709 9710 9711 9712 9713 9714 9715 9716 9717 9718
    9719 9720 9721 9722 9723 9724 9725 9726 9727 9733 9734 9872 9873 9888
    9998 10003 10004 10007 65533]
   :meta-out "resources/public/fonts/dejavu_sans_mono_slug_meta.json"
   :curve-out "resources/public/fonts/dejavu_sans_mono_slug_curve.bin"
   :band-out "resources/public/fonts/dejavu_sans_mono_slug_band.bin"})

(defn write-font-assets!
  [{:keys [font-path codepoints meta-out curve-out band-out]
    :or {font-path (:font-path (default-config))
         codepoints (:codepoints (default-config))
         meta-out (:meta-out (default-config))
         curve-out (:curve-out (default-config))
         band-out (:band-out (default-config))}}]
  (let [font (load-font font-path)
        glyphs (mapv (partial compile-glyph font) codepoints)
        {:keys [glyphs curve-pack band-pack]} (pack-glyphs glyphs)
        meta {:version 1
              :metrics (font-line-metrics font)
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
