(ns app.client.text.glyph-pack
  "Read positioned columns directly into GPU words.

   Input: positioned draw items selecting layout glyph indexes, world
   transforms and Slug glyph metadata. Output: instance counts and writes
   into caller-owned float/uint views. A WeakMap holds one derived lookup
   table per glyph-list object. It does not shape text or create GPU
   buffers.

   Folder map: README.md."
  (:require [app.client.engine.transform :as transform]
            [app.client.text.layout :as tl]))

(def instance-words 25)

(defonce ^:private slug-table-cache (js/WeakMap.))

(defn- map-of!
  "JS outer map/key → existing or newly inserted inner map.

   Lazy nested-map construction."
  [^js outer key]
  (let [m (.get outer key)]
    (if (undefined? m)
      (let [m (js/Map.)]
        (.set outer key m)
        m)
      m)))

(defn slug-table
  "Glyph metadata list → cached typed lookup table.

   Packs metadata and builds last-wins lookup maps. Intended for immutable
   list identity; mutating the list in place would leave a stale cache. Each
   metadata row retains eight float64 values and four uint32 values, plus
   font-specific/global resolution maps."
  [glyphs]
  (or (.get slug-table-cache glyphs)
      (let [n (count glyphs)
            floats (js/Float64Array. (* 8 n))
            uints (js/Uint32Array. (* 4 n))
            by-font-index (js/Map.)
            by-index (js/Map.)
            by-font-unicode (js/Map.)
            by-unicode (js/Map.)]
        (doseq [[row g] (map-indexed vector glyphs)]
          (let [sb (or (:sampleBounds g) (:planeBounds g))
                slug (:slug g)
                f8 (* 8 row)
                u4 (* 4 row)
                unicode (:unicode g)
                index (:index g)
                font-id (:fontId g)]
            (aset floats f8 (or (:left sb) 0.0))
            (aset floats (+ f8 1) (or (:top sb) 0.0))
            (aset floats (+ f8 2) (or (:right sb) 0.0))
            (aset floats (+ f8 3) (or (:bottom sb) 0.0))
            (aset floats (+ f8 4) (or (get-in slug [:banding :scaleX]) 0.0))
            (aset floats (+ f8 5) (or (get-in slug [:banding :scaleY]) 0.0))
            (aset floats (+ f8 6) (or (get-in slug [:banding :offsetX]) 0.0))
            (aset floats (+ f8 7) (or (get-in slug [:banding :offsetY]) 0.0))
            (aset uints u4 (or (get-in slug [:glyphLoc :x]) 0))
            (aset uints (+ u4 1) (or (get-in slug [:glyphLoc :y]) 0))
            (aset uints (+ u4 2) (or (get-in slug [:bandMax :x]) 0))
            (aset uints (+ u4 3) (or (:packedBandMeta slug) 0))
            (when (some? unicode)
              (.set by-unicode unicode row)
              (when font-id (.set (map-of! by-font-unicode font-id) unicode row)))
            (when (some? index)
              (.set by-index index row)
              (when font-id (.set (map-of! by-font-index font-id) index row)))))
        (let [table {:floats floats :uints uints
                     :by-font-index by-font-index :by-index by-index
                     :by-font-unicode by-font-unicode :by-unicode by-unicode}]
          (when glyphs (.set slug-table-cache glyphs table))
          table))))

(defn- lookup
  "JS map/key → row index or -1.

   Distinguishes missing from row zero."
  [^js m k]
  (let [v (.get m k)]
    (if (undefined? v) -1 v)))

(defn- font-lookup
  "Nested maps/font/key → row or -1.

   Guarded font-specific lookup."
  [^js outer font-id k]
  (if (nil? font-id)
    -1
    (let [m (.get outer font-id)]
      (if (undefined? m) -1 (lookup m k)))))

(defn- row-for
  "Table/font/kind/glyph ID → selected metadata row or -1.

   Six-step direct/replacement/missing-glyph fallback. Global glyph-index
   fallback assumes the combined metadata mapping is meaningful across
   fonts."
  [{:keys [by-font-index by-index by-font-unicode by-unicode]} font-id kind glyph-id]
  (let [font-kind (if (= kind :index) by-font-index by-font-unicode)
        kind-map (if (= kind :index) by-index by-unicode)
        r (font-lookup font-kind font-id glyph-id)]
    (if (>= r 0)
      r
      (let [r (lookup kind-map glyph-id)]
        (if (>= r 0)
          r
          (let [r (font-lookup by-font-unicode font-id 0xFFFD)]
            (if (>= r 0)
              r
              (let [r (lookup by-unicode 0xFFFD)]
                (if (>= r 0)
                  r
                  (let [r (font-lookup by-font-index font-id 0)]
                    (if (>= r 0)
                      r
                      (lookup by-index 0))))))))))))

(defn- single-space?
  "Text/length/cluster bounds → exactly one space?

   Clamped code-unit check without substring. Intended for the exact skip
   rule."
  [text len cs ce]
  (let [a (min len cs) b (min len ce)]
    (and (= 1 (- b a)) (= 32 (.charCodeAt text a)))))

(defn- each-painted!
  "Positioned item/table/callback → callback per drawable glyph; nil-like
   traversal result.

   Reads primitive layout fields, skips tabs/spaces, resolves row. No rich
   glyph maps."
  [{:keys [line indexes dx dy]} table f]
  (let [text (str (or (:text line) ""))
        len (.-length text)]
    (tl/pack-glyphs!
     line indexes dx dy
     (fn [_ glyph-id shaped? tab? font-id x0 baseline-y cs ce]
       (when-not (or tab? (single-space? text len cs ce))
         (let [kind (if (and shaped? (not tab?)) :index :unicode)
               row (row-for table font-id kind glyph-id)]
           (when (>= row 0)
             (f row x0 baseline-y))))))))

(defn count-instances
  "Item/table → drawable glyph count.

   Traverses same resolution path with a counter. Sizing agrees with
   packing, at the cost of an extra pass."
  [draw-item table]
  (let [!n (volatile! 0)]
    (each-painted! draw-item table (fn [_ _ _] (vswap! !n inc)))
    @!n))

(defn pack-draw-item!
  "Float/uint views, starting instance, item/table/transforms → next
   instance index; writes 25 words each.

   Resolves group once, converts glyph bounds/position and writes
   metadata/color/index. Intended for direct packing; expects
   capacity/stride/font size supplied consistently."
  [^js float-view ^js uint-view instance-index draw-item table world-transforms]
  (let [{:keys [style font-size]} draw-item
        {:keys [r g b a]} style
        cr (or r 1.0) cg (or g 1.0) cb (or b 1.0) ca (or a 1.0)
        group (transform/buffer-index world-transforms (:container draw-item))
        fsize font-size
        inv-size (if (pos? fsize) (/ 1.0 fsize) 0.0)
        ^js floats (:floats table)
        ^js uints (:uints table)
        !i (volatile! instance-index)]
    (each-painted!
     draw-item table
     (fn [row x0 baseline-y]
       (let [base (* @!i instance-words)
             f8 (* 8 row)
             u4 (* 4 row)
             left (aget floats f8)
             top (aget floats (+ f8 1))
             right (aget floats (+ f8 2))
             bottom (aget floats (+ f8 3))
             world-left (+ x0 (* fsize left))
             world-right (+ x0 (* fsize right))
             world-top (- baseline-y (* fsize top))
             world-bottom (- baseline-y (* fsize bottom))]
         (aset float-view (+ base 0) world-left)
         (aset float-view (+ base 1) world-top)
         (aset float-view (+ base 2) (- world-right world-left))
         (aset float-view (+ base 3) (- world-bottom world-top))
         (aset float-view (+ base 4) left)
         (aset float-view (+ base 5) top)
         (aset float-view (+ base 6) right)
         (aset float-view (+ base 7) bottom)
         (aset float-view (+ base 8) inv-size)
         (aset float-view (+ base 9) 0.0)
         (aset float-view (+ base 10) 0.0)
         (aset float-view (+ base 11) (- inv-size))
         (aset float-view (+ base 12) (aget floats (+ f8 4)))
         (aset float-view (+ base 13) (aget floats (+ f8 5)))
         (aset float-view (+ base 14) (aget floats (+ f8 6)))
         (aset float-view (+ base 15) (aget floats (+ f8 7)))
         (aset uint-view (+ base 16) (aget uints u4))
         (aset uint-view (+ base 17) (aget uints (+ u4 1)))
         (aset uint-view (+ base 18) (aget uints (+ u4 2)))
         (aset uint-view (+ base 19) (aget uints (+ u4 3)))
         (aset float-view (+ base 20) cr)
         (aset float-view (+ base 21) cg)
         (aset float-view (+ base 22) cb)
         (aset float-view (+ base 23) ca)
         (aset uint-view (+ base 24) group)
         (vswap! !i inc))))
    @!i))

(defn pack-lines!
  "Views, ordered line groups, table/transforms → nil; packs all items in
   order.

   Threads next instance index through groups. Each line group has
   :draw-items and :count."
  [^js float-view ^js uint-view lines table world-transforms]
  (loop [remaining lines i 0]
    (when (seq remaining)
      (let [next-i (reduce (fn [i draw-item]
                             (pack-draw-item! float-view uint-view i draw-item table world-transforms))
                           i (:draw-items (first remaining)))]
        (recur (next remaining) next-i))))
  nil)
