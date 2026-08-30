(ns app.client.text.shaped-line
  "The shaping provider's output as columns: one shaped source line as typed
   arrays in visual order, with its runs, faces, and total advance. The flat
   road's vocabulary between the shaper and text layout.
   Takes: nothing itself; providers fill a line through `make-line` and the
   column setters, or coerce a legacy map-shaped result through `from-maps`.
   Gives: the shaped line map (scalars + columns), typed-array accessors, and
   the two oracle converters `->maps` / `from-maps`.
   Holds: nothing.")

;; ---------------------------------------------------------------------------
;; Typed columns, cross-platform. HarfBuzz gives int32 font units; cluster
;; offsets and glyph ids are unsigned 32-bit.

(defn i32-array [n]
  #?(:clj (int-array n) :cljs (js/Int32Array. n)))

(defn u32-array [n]
  #?(:clj (int-array n) :cljs (js/Uint32Array. n)))

(defn u8-array [n]
  #?(:clj (byte-array n) :cljs (js/Uint8Array. n)))

(defn i32-get [a i]
  #?(:clj (long (aget ^ints a i)) :cljs (aget a i)))

(defn u32-get [a i]
  #?(:clj (bit-and 0xffffffff (long (aget ^ints a i))) :cljs (aget a i)))

(defn u8-get [a i]
  #?(:clj (bit-and 0xff (long (aget ^bytes a i))) :cljs (aget a i)))

(defn i32-set! [a i v]
  #?(:clj (aset-int ^ints a i (unchecked-int (long v))) :cljs (aset a i v)))

(defn u32-set! [a i v]
  #?(:clj (aset-int ^ints a i (unchecked-int (long v))) :cljs (aset a i v)))

(defn u8-set! [a i v]
  #?(:clj (aset-byte ^bytes a i (unchecked-byte (long v))) :cljs (aset a i v)))

(def no-ink
  "Sentinel in `:ink-w`: the glyph has no extents (virtual tab, or the font
   reports none)."
  -1)

(def rtl-flag 1)
(def tab-flag 2)

(def glyph-columns
  [:glyph-id :cluster-start :cluster-end :advance-x :advance-y :offset-x
   :offset-y :glyph-x :glyph-y :ink-x :ink-y :ink-w :ink-h :run-index])

(def run-columns
  [:run-source-start :run-source-end :run-flags :run-face])

(defn shaped-line?
  "True for a value in this vocabulary (a provider's flat result)."
  [x]
  (and (map? x) (contains? x :glyph-count) (contains? x :run-index)))

(defn make-line
  "Allocate a shaped line with `glyph-count` glyphs and `run-count` runs over
   `faces` ([{:id :revision :upem}] in provider order). Columns are zeroed;
   `:ink-w` starts at `no-ink`."
  [glyph-count run-count faces]
  (let [g (long glyph-count) r (long run-count)
        ink-w (i32-array g)]
    (dotimes [i g] (i32-set! ink-w i no-ink))
    {:glyph-count g
     :run-count r
     :advance 0
     :base-direction :ltr
     :faces (vec faces)
     :glyph-id (u32-array g)
     :cluster-start (u32-array g)
     :cluster-end (u32-array g)
     :advance-x (i32-array g)
     :advance-y (i32-array g)
     :offset-x (i32-array g)
     :offset-y (i32-array g)
     :glyph-x (i32-array g)
     :glyph-y (i32-array g)
     :ink-x (i32-array g)
     :ink-y (i32-array g)
     :ink-w ink-w
     :ink-h (i32-array g)
     :run-index (u32-array g)
     :run-source-start (u32-array r)
     :run-source-end (u32-array r)
     :run-flags (u32-array r)
     :run-face (u32-array r)}))

(defn empty-line [faces]
  (make-line 0 0 faces))

(defn run-rtl? [line run-index]
  (not (zero? (bit-and rtl-flag (u32-get (:run-flags line) run-index)))))

(defn run-tab? [line run-index]
  (not (zero? (bit-and tab-flag (u32-get (:run-flags line) run-index)))))

(defn run-face [line run-index]
  (nth (:faces line) (u32-get (:run-face line) run-index)))

(defn glyph-tab? [line i]
  (run-tab? line (u32-get (:run-index line) i)))

(defn glyph-rtl? [line i]
  (run-rtl? line (u32-get (:run-index line) i)))

(defn glyph-has-ink? [line i]
  (not= no-ink (i32-get (:ink-w line) i)))

;; ---------------------------------------------------------------------------
;; Oracle converters. `->maps` rebuilds EXACTLY the map shape the pre-flat
;; shaper returned, so the frozen oracle roads consume it unchanged.
;; `from-maps` is the seam coercion for map-shaped providers (the JVM test
;; corpora); the round trip `(->maps (from-maps m)) = m` is a tripwire.

(defn- glyph-map [line i]
  (let [run (u32-get (:run-index line) i)
        tab? (run-tab? line run)
        face (run-face line run)
        direction (if (run-rtl? line run) :rtl :ltr)]
    {:glyph-id (when-not tab? (u32-get (:glyph-id line) i))
     :glyph-id-kind (if tab? :virtual/tab :font-glyph-index)
     :font-id (:id face)
     :font-revision (:revision face)
     :cluster-start (u32-get (:cluster-start line) i)
     :cluster-end (u32-get (:cluster-end line) i)
     :advance [(i32-get (:advance-x line) i) (i32-get (:advance-y line) i)]
     :offset [(i32-get (:offset-x line) i) (i32-get (:offset-y line) i)]
     :ink-bounds (when (glyph-has-ink? line i)
                   {:xBearing (i32-get (:ink-x line) i)
                    :yBearing (i32-get (:ink-y line) i)
                    :width (i32-get (:ink-w line) i)
                    :height (i32-get (:ink-h line) i)})
     :direction direction
     :position [(i32-get (:glyph-x line) i) (i32-get (:glyph-y line) i)]}))

(defn- cluster-records
  "Verbatim from the pre-flat shaper: one record per (font-revision,
   cluster-start, cluster-end, direction), sorted by :left."
  [runs]
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

(defn ->maps
  "The pre-flat provider result: {:runs :glyphs :clusters :advance
   :base-direction} with 11-key + :position glyph maps."
  [line]
  (if (zero? (long (:glyph-count line)))
    {:runs [] :glyphs [] :clusters [] :advance (:advance line)}
    (let [g (long (:glyph-count line))
          glyphs (mapv #(glyph-map line %) (range g))
          r (long (:run-count line))
          runs (mapv (fn [run]
                       (let [face (run-face line run)]
                         {:source-start (u32-get (:run-source-start line) run)
                          :source-end (u32-get (:run-source-end line) run)
                          :direction (if (run-rtl? line run) :rtl :ltr)
                          :font-id (:id face)
                          :font-revision (:revision face)
                          :upem (:upem face)
                          :glyphs (into []
                                        (keep (fn [i]
                                                (when (= run (u32-get (:run-index line) i))
                                                  (nth glyphs i))))
                                        (range g))}))
                     (range r))]
      {:runs runs
       :glyphs glyphs
       :clusters (cluster-records runs)
       :advance (:advance line)
       :base-direction (:base-direction line)})))

(defn- int-at [v i]
  (long (or (nth v i nil) 0)))

(defn from-maps
  "Coerce a map-shaped provider result into a shaped line. Visual order is the
   `:glyphs` vector; a glyph's run is the `:runs` entry whose `:glyphs`
   contains it (by value). A result whose `:clusters` is empty while glyphs
   exist has no cluster ownership and coerces to zero glyphs — the flat
   vocabulary's provider fault (I5)."
  [m]
  (if (shaped-line? m)
    m
    (let [glyphs (vec (:glyphs m))
          runs (vec (:runs m))
          faulted? (and (contains? m :clusters) (empty? (:clusters m))
                        (seq glyphs))
          glyphs (if faulted? [] glyphs)
          runs (if faulted? [] runs)
          faces (vec (distinct (map (fn [run]
                                      {:id (:font-id run)
                                       :revision (:font-revision run)
                                       :upem (:upem run)})
                                    runs)))
          face-index (into {} (map-indexed (fn [i f] [f i]) faces))
          glyph->run (into {}
                           (mapcat (fn [ri run]
                                     (map (fn [glyph] [glyph ri]) (:glyphs run)))
                                   (range) runs))
          line (make-line (count glyphs) (count runs) faces)]
      (doseq [[ri run] (map-indexed vector runs)]
        (u32-set! (:run-source-start line) ri (long (:source-start run)))
        (u32-set! (:run-source-end line) ri (long (:source-end run)))
        (u32-set! (:run-flags line) ri
                  (bit-or (if (= :rtl (:direction run)) rtl-flag 0)
                          (if (some #(= :virtual/tab (:glyph-id-kind %))
                                    (:glyphs run))
                            tab-flag 0)))
        (u32-set! (:run-face line) ri
                  (get face-index {:id (:font-id run)
                                   :revision (:font-revision run)
                                   :upem (:upem run)})))
      (doseq [[i glyph] (map-indexed vector glyphs)]
        (let [ri (long (or (get glyph->run glyph) 0))
              ink (:ink-bounds glyph)]
          (u32-set! (:glyph-id line) i (long (or (:glyph-id glyph) 0)))
          (u32-set! (:cluster-start line) i (long (or (:cluster-start glyph) 0)))
          (u32-set! (:cluster-end line) i (long (or (:cluster-end glyph) 0)))
          (i32-set! (:advance-x line) i (int-at (:advance glyph) 0))
          (i32-set! (:advance-y line) i (int-at (:advance glyph) 1))
          (i32-set! (:offset-x line) i (int-at (:offset glyph) 0))
          (i32-set! (:offset-y line) i (int-at (:offset glyph) 1))
          (i32-set! (:glyph-x line) i (int-at (:position glyph) 0))
          (i32-set! (:glyph-y line) i (int-at (:position glyph) 1))
          (u32-set! (:run-index line) i ri)
          (when ink
            (i32-set! (:ink-x line) i (long (or (:xBearing ink) 0)))
            (i32-set! (:ink-y line) i (long (or (:yBearing ink) 0)))
            (i32-set! (:ink-w line) i (long (or (:width ink) 0)))
            (i32-set! (:ink-h line) i (long (or (:height ink) 0))))))
      (assoc line
             :advance (:advance m)
             :base-direction (or (:base-direction m) :ltr)))))
