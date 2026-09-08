(ns app.client.text.truetype
  "TrueType outlines and metrics as path values.

   Takes a font file's bytes. Gives the font's metrics and character map,
   and per glyph the advance, the bounding box and the outline as a path
   value in font units with y up (quadratic segments, closed subpaths, no
   knots). Holds nothing: every read is over the bytes given, and a glyph
   is read when asked for. Composite glyphs resolve their components'
   offsets and scales; point-matched components, hinting, kerning and
   variations are not read. Evidence: truetype_test.clj.

   Folder map: README.md."
  (:refer-clojure :exclude [bytes]))

;; ---------------------------------------------------------------- big-endian reads
(defn- u8 [b i] #?(:clj (bit-and 255 (aget ^bytes b (int i))) :cljs (aget b i)))
(defn- u16 [b i] (+ (* 256 (u8 b i)) (u8 b (inc i))))
(defn- i16 [b i] (let [v (u16 b i)] (if (>= v 32768) (- v 65536) v)))
(defn- i8 [b i] (let [v (u8 b i)] (if (>= v 128) (- v 256) v)))
(defn- u32 [b i] (+ (* 65536 (u16 b i)) (u16 b (+ i 2))))
(defn- f2dot14 [b i] (/ (i16 b i) 16384.0))
(defn- tag [b i] (apply str (map #(char (u8 b (+ i %))) (range 4))))

(defn digest
  "Bytes → the FNV-1a 32-bit digest as hex: an identity for the file the
   font record can carry."
  [b]
  (let [n #?(:clj (alength ^bytes b) :cljs (.-length b))]
    (loop [i 0 h 2166136261]
      (if (= i n)
        #?(:clj (Long/toHexString h) :cljs (.toString (unsigned-bit-shift-right h 0) 16))
        (recur (inc i)
               #?(:clj (bit-and 0xFFFFFFFF (* (bit-xor h (u8 b i)) 16777619))
                  :cljs (unsigned-bit-shift-right (js/Math.imul (bit-xor h (u8 b i)) 16777619) 0)))))))

;; ---------------------------------------------------------------- the table directory and the character map
(defn- directory [b]
  (let [n (u16 b 4)]
    (into {} (for [k (range n) :let [o (+ 12 (* 16 k))]]
               [(tag b o) {:offset (u32 b (+ o 8)) :length (u32 b (+ o 12))}]))))

(defn- format-4
  "A format 4 subtable at `at` → {codepoint glyph-index} for every mapped
   character; unmapped characters are absent."
  [b at]
  (let [seg-count (quot (u16 b (+ at 6)) 2)
        ends (+ at 14)
        starts (+ ends (* 2 seg-count) 2)
        deltas (+ starts (* 2 seg-count))
        ranges (+ deltas (* 2 seg-count))]
    (into {}
          (for [s (range seg-count)
                :let [end (u16 b (+ ends (* 2 s))) start (u16 b (+ starts (* 2 s)))
                      delta (u16 b (+ deltas (* 2 s))) range-offset (u16 b (+ ranges (* 2 s)))]
                :when (and (<= start end) (< start 0xFFFF))
                c (range start (inc (min end 0xFFFE)))
                :let [gid (if (zero? range-offset)
                            (mod (+ c delta) 65536)
                            (let [g (u16 b (+ ranges (* 2 s) range-offset (* 2 (- c start))))]
                              (if (zero? g) 0 (mod (+ g delta) 65536))))]
                :when (pos? gid)]
            [c gid]))))

(defn- character-map [b {:keys [offset]}]
  (let [n (u16 b (+ offset 2))
        subtables (for [k (range n) :let [o (+ offset 4 (* 8 k))]]
                    {:platform (u16 b o) :encoding (u16 b (+ o 2)) :at (+ offset (u32 b (+ o 4)))})
        f4 (filter #(= 4 (u16 b (:at %))) subtables)
        pick (or (first (filter #(and (= 3 (:platform %)) (= 1 (:encoding %))) f4))
                 (first (filter #(= 0 (:platform %)) f4))
                 (first f4))]
    (if pick (format-4 b (:at pick)) {})))

;; ---------------------------------------------------------------- the font
(defn parse
  "A TrueType file's bytes → the font: metrics in font units, the character
   map, and the table offsets later reads use. Refuses a file without glyf
   outlines with :truetype/tables."
  [b]
  (let [dir (directory b)
        at (fn [t] (get-in dir [t :offset]))
        head (at "head") hhea (at "hhea") maxp (at "maxp")]
    (when-not (and head hhea maxp (at "loca") (at "glyf") (at "hmtx") (at "cmap"))
      (throw (ex-info "Not a TrueType font with glyf outlines" {:error-type :truetype/tables :tables (sort (keys dir))})))
    {:bytes b
     :upem (u16 b (+ head 18))
     :loca-format (i16 b (+ head 50))
     :ascender (i16 b (+ hhea 4))
     :descender (i16 b (+ hhea 6))
     :line-gap (i16 b (+ hhea 8))
     :hmetrics (u16 b (+ hhea 34))
     :glyph-count (u16 b (+ maxp 4))
     :loca (at "loca") :glyf (at "glyf") :hmtx (at "hmtx")
     :cmap (character-map b (get dir "cmap"))}))

(defn metrics
  "Font → the values a font record carries: units per em, ascender,
   descender, line gap, glyph count."
  [font]
  (select-keys font [:upem :ascender :descender :line-gap :glyph-count]))

(defn advance
  "Font and glyph index → the advance width in font units. Past the last
   horizontal metric every glyph shares the last advance."
  [{:keys [bytes hmtx hmetrics]} gid]
  (u16 bytes (+ hmtx (* 4 (min gid (dec hmetrics))))))

(defn- glyph-span [{:keys [bytes loca loca-format]} gid]
  (if (zero? loca-format)
    [(* 2 (u16 bytes (+ loca (* 2 gid)))) (* 2 (u16 bytes (+ loca (* 2 (inc gid)))))]
    [(u32 bytes (+ loca (* 4 gid))) (u32 bytes (+ loca (* 4 (inc gid))))]))

(defn- simple-points
  "Bytes, a simple glyph's start and its contour count → the end index of
   each contour and every point {:x :y :on}."
  [b g n-contours]
  (let [ends (mapv #(u16 b (+ g 10 (* 2 %))) (range n-contours))
        n-points (if (pos? n-contours) (inc (peek ends)) 0)
        instructions (u16 b (+ g 10 (* 2 n-contours)))
        flags-at (+ g 12 (* 2 n-contours) instructions)
        [flags after-flags] (loop [i flags-at acc []]
                              (if (>= (count acc) n-points)
                                [(vec (take n-points acc)) i]
                                (let [f (u8 b i)]
                                  (if (pos? (bit-and f 8))
                                    (recur (+ i 2) (into acc (repeat (inc (u8 b (inc i))) f)))
                                    (recur (inc i) (conj acc f))))))
        coords (fn [from short-bit same-bit]
                 (loop [i from k 0 v 0 acc []]
                   (if (= k n-points)
                     [acc i]
                     (let [f (nth flags k)]
                       (cond
                         (pos? (bit-and f short-bit))
                         (let [d (u8 b i) v (if (pos? (bit-and f same-bit)) (+ v d) (- v d))]
                           (recur (inc i) (inc k) v (conj acc v)))
                         (pos? (bit-and f same-bit)) (recur i (inc k) v (conj acc v))
                         :else (let [v (+ v (i16 b i))] (recur (+ i 2) (inc k) v (conj acc v))))))))
        [xs after-x] (coords after-flags 2 16)
        [ys _] (coords after-x 4 32)]
    {:ends ends
     :points (mapv (fn [k] {:x (nth xs k) :y (nth ys k) :on (pos? (bit-and (nth flags k) 1))}) (range n-points))}))

(defn- contour->subpath
  "One contour's points, cyclic → a closed subpath of lines and quadratics.
   Two off-curve points in a row imply the on-curve point between them."
  [pts]
  (let [n (count pts)
        first-on (some (fn [k] (when (:on (nth pts k)) k)) (range n))
        start (if first-on
                (let [p (nth pts first-on)] [(:x p) (:y p)])
                (let [a (nth pts 0) c (nth pts (mod 1 n))] [(/ (+ (:x a) (:x c)) 2.0) (/ (+ (:y a) (:y c)) 2.0)]))
        walk (if first-on
               (map #(nth pts (mod (+ first-on 1 %) n)) (range n))
               (map #(nth pts (mod (inc %) n)) (range n)))
        [segments ctrl] (reduce (fn [[segs ctrl] p]
                                  (let [pt [(:x p) (:y p)]]
                                    (cond
                                      (:on p) [(conj segs (if ctrl {:kind :quad :c ctrl :p pt} {:kind :line :p pt})) nil]
                                      ctrl [(conj segs {:kind :quad :c ctrl :p [(/ (+ (ctrl 0) (pt 0)) 2.0) (/ (+ (ctrl 1) (pt 1)) 2.0)]}) pt]
                                      :else [segs pt])))
                                [[] nil] walk)]
    {:closed? true :start start
     :segments (cond-> segments ctrl (conj {:kind :quad :c ctrl :p start}))}))

(defn- map-subpath [sp f]
  (-> sp
      (update :start f)
      (update :segments (fn [segs] (mapv (fn [s] (cond-> (update s :p f)
                                                   (:c s) (update :c f)
                                                   (:c1 s) (update :c1 f)
                                                   (:c2 s) (update :c2 f))) segs)))))

(declare glyph*)

(defn- composite-subpaths
  "Bytes, a composite glyph's start and the recursion depth → the components'
   subpaths, each offset and scaled as declared."
  [font b g depth]
  (loop [i (+ g 10) acc []]
    (let [flags (u16 b i) gid (u16 b (+ i 2))
          words? (pos? (bit-and flags 1)) xy? (pos? (bit-and flags 2))
          [dx dy i] (if words? [(i16 b (+ i 4)) (i16 b (+ i 6)) (+ i 8)] [(i8 b (+ i 4)) (i8 b (+ i 5)) (+ i 6)])
          [dx dy] (if xy? [dx dy] [0 0])
          [a b2 c d i] (cond (pos? (bit-and flags 8)) (let [s (f2dot14 b i)] [s 0 0 s (+ i 2)])
                             (pos? (bit-and flags 64)) [(f2dot14 b i) 0 0 (f2dot14 b (+ i 2)) (+ i 4)]
                             (pos? (bit-and flags 128)) [(f2dot14 b i) (f2dot14 b (+ i 2)) (f2dot14 b (+ i 4)) (f2dot14 b (+ i 6)) (+ i 8)]
                             :else [1 0 0 1 i])
          place (fn [[x y]] [(+ dx (* a x) (* c y)) (+ dy (* b2 x) (* d y))])
          acc (into acc (map #(map-subpath % place)) (get-in (glyph* font gid (inc depth)) [:outline :subpaths]))]
      (if (and (pos? (bit-and flags 32)) (< depth 8)) (recur i acc) acc))))

(defn- glyph* [font gid depth]
  (let [{:keys [bytes glyf]} font
        [from to] (glyph-span font gid)
        adv (advance font gid)]
    (if (or (<= (- to from) 0) (>= gid (:glyph-count font)))
      {:advance adv :bbox [0 0 0 0] :outline {:subpaths []} :contours 0}
      (let [g (+ glyf from)
            n-contours (i16 bytes g)
            bbox [(i16 bytes (+ g 2)) (i16 bytes (+ g 4)) (i16 bytes (+ g 6)) (i16 bytes (+ g 8))]
            subpaths (if (neg? n-contours)
                       (if (< depth 8) (composite-subpaths font bytes g depth) [])
                       (let [{:keys [ends points]} (simple-points bytes g n-contours)]
                         (vec (for [[k end] (map-indexed vector ends)
                                    :let [begin (if (zero? k) 0 (inc (nth ends (dec k))))
                                          pts (subvec points begin (inc end))]
                                    :when (seq pts)]
                                (contour->subpath pts)))))]
        {:advance adv :bbox bbox :outline {:subpaths subpaths} :contours (count subpaths)}))))

(defn glyph
  "Font and glyph index → {:advance :bbox [xmin ymin xmax ymax] :outline
   :contours}, the outline a path value in font units, y up."
  [font gid]
  (glyph* font gid 0))

(defn glyph-index
  "Font and a one-character string → its glyph index, 0 when unmapped."
  [font ch]
  (get (:cmap font) (.codePointAt ^String ch 0) 0))

(defn glyph-for-char
  "Font and a one-character string → that character's glyph, the missing
   glyph when unmapped."
  [font ch]
  (glyph font (glyph-index font ch)))
