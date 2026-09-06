(ns app.client.engine.coverage
  "Fill regions per pixel from curves and bands: the filler, shared by text
   and paths.

   Input: a device, packs (quadratics in local units with sorted bands, from
   path/pack) and instance descriptions. Output: WGSL for the per-pixel
   coverage program, a dynamic atlas of curve and band textures that the
   program reads, and the packed instance rows a region draw consumes.
   The atlas owns its two textures and their CPU mirrors; slots are values
   its owner (a renderer system) keys by pack.

   The program is the Slug algorithm as text has always run it: signed root
   crossings of the quadratics in the pixel's row band and column band, a
   weight per axis from the nearest crossing, one coverage. Text keeps its
   offline-built font atlas; paths pack into this atlas at edit time. Both
   address bands the same way: a header per band at the region's base, then
   lists of the curves' texel coordinates, wrapping at a power-of-two width
   the caller names. Even-odd is the parity of the same crossings.

   Folder map: README.md.")

;; ---- the program ----

;; Bindings 0 and 1 are the curve and band textures; every lane that
;; composes this text binds them there. calc_band_loc wraps a linear offset
;; from a base texel at the texture's power-of-two width. region_coverage
;; is the one coverage: p and ppu are in the region's own units (local units
;; for paths, em units for glyphs), band_base and band_max locate the
;; region's bands, band_xf maps p to a band index, rule 0 is nonzero and 1
;; even-odd.
(def coverage-wgsl "
  const kMinDerivative: f32 = 1.0 / 65536.0;

  @group(0) @binding(0) var curveTexture: texture_2d<f32>;
  @group(0) @binding(1) var bandTexture: texture_2d<u32>;

  fn saturate(x: f32) -> f32 {
    return clamp(x, 0.0, 1.0);
  }

  fn calc_root_code(y1: f32, y2: f32, y3: f32) -> u32 {
    let i1 = bitcast<u32>(y1) >> 31u;
    let i2 = bitcast<u32>(y2) >> 30u;
    let i3 = bitcast<u32>(y3) >> 29u;
    var shift = (i2 & 2u) | (i1 & ~2u);
    shift = (i3 & 4u) | (shift & ~4u);
    return (0x2E74u >> shift) & 0x0101u;
  }

  fn solve_horiz_poly(p12: vec4<f32>, p3: vec2<f32>) -> vec2<f32> {
    let a = p12.xy - p12.zw * 2.0 + p3;
    let b = p12.xy - p12.zw;
    let ra = 1.0 / a.y;
    let rb = 0.5 / b.y;
    let d = sqrt(max(b.y * b.y - a.y * p12.y, 0.0));
    var t1 = (b.y - d) * ra;
    var t2 = (b.y + d) * ra;
    if (abs(a.y) < kMinDerivative) {
      t1 = p12.y * rb;
      t2 = t1;
    }
    return vec2<f32>((a.x * t1 - b.x * 2.0) * t1 + p12.x,
                     (a.x * t2 - b.x * 2.0) * t2 + p12.x);
  }

  fn solve_vert_poly(p12: vec4<f32>, p3: vec2<f32>) -> vec2<f32> {
    let a = p12.xy - p12.zw * 2.0 + p3;
    let b = p12.xy - p12.zw;
    let ra = 1.0 / a.x;
    let rb = 0.5 / b.x;
    let d = sqrt(max(b.x * b.x - a.x * p12.x, 0.0));
    var t1 = (b.x - d) * ra;
    var t2 = (b.x + d) * ra;
    if (abs(a.x) < kMinDerivative) {
      t1 = p12.x * rb;
      t2 = t1;
    }
    return vec2<f32>((a.y * t1 - b.y * 2.0) * t1 + p12.y,
                     (a.y * t2 - b.y * 2.0) * t2 + p12.y);
  }

  fn calc_band_loc(base: vec2<i32>, offset: u32, log2_width: u32) -> vec2<i32> {
    let width = 1i << log2_width;
    var x = base.x + i32(offset);
    var y = base.y + (x >> log2_width);
    x = x & (width - 1);
    return vec2<i32>(x, y);
  }

  fn calc_coverage(xcov: f32, ycov: f32, xwgt: f32, ywgt: f32) -> f32 {
    let weighted = abs(xcov * xwgt + ycov * ywgt) / max(xwgt + ywgt, kMinDerivative);
    let coverage = max(weighted, min(abs(xcov), abs(ycov)));
    return saturate(coverage);
  }

  fn region_coverage(p: vec2<f32>, ppu: vec2<f32>, band_base: vec2<i32>, band_max: vec2<i32>,
                     band_xf: vec4<f32>, rule: u32, log2_width: u32) -> f32 {
    let band_index = clamp(vec2<i32>(floor(p * band_xf.xy + band_xf.zw)),
                           vec2<i32>(0, 0),
                           band_max);

    var xcov = 0.0;
    var xwgt = 0.0;
    var xabs = 0.0;
    let hband_data = textureLoad(bandTexture, calc_band_loc(band_base, u32(band_index.y), log2_width), 0).xy;
    let hband_loc = calc_band_loc(band_base, hband_data.y, log2_width);
    for (var curve_index = 0u; curve_index < hband_data.x; curve_index = curve_index + 1u) {
      let curve_loc_data = textureLoad(bandTexture, calc_band_loc(hband_loc, curve_index, log2_width), 0).xy;
      let curve_loc = vec2<i32>(i32(curve_loc_data.x), i32(curve_loc_data.y));
      let p12 = textureLoad(curveTexture, curve_loc, 0) - vec4<f32>(p, p);
      let p3 = textureLoad(curveTexture, vec2<i32>(curve_loc.x + 1, curve_loc.y), 0).xy - p;
      if (max(max(p12.x, p12.z), p3.x) * ppu.x < -0.5) {
        break;
      }
      let code = calc_root_code(p12.y, p12.w, p3.y);
      if (code != 0u) {
        let roots = solve_horiz_poly(p12, p3) * ppu.x;
        if ((code & 1u) != 0u) {
          let c = saturate(roots.x + 0.5);
          xcov = xcov + c;
          xabs = xabs + c;
          xwgt = max(xwgt, saturate(1.0 - abs(roots.x) * 2.0));
        }
        if (code > 1u) {
          let c = saturate(roots.y + 0.5);
          xcov = xcov - c;
          xabs = xabs + c;
          xwgt = max(xwgt, saturate(1.0 - abs(roots.y) * 2.0));
        }
      }
    }

    var ycov = 0.0;
    var ywgt = 0.0;
    var yabs = 0.0;
    let vband_data = textureLoad(bandTexture, calc_band_loc(band_base, u32(band_max.y + 1 + band_index.x), log2_width), 0).xy;
    let vband_loc = calc_band_loc(band_base, vband_data.y, log2_width);
    for (var curve_index = 0u; curve_index < vband_data.x; curve_index = curve_index + 1u) {
      let curve_loc_data = textureLoad(bandTexture, calc_band_loc(vband_loc, curve_index, log2_width), 0).xy;
      let curve_loc = vec2<i32>(i32(curve_loc_data.x), i32(curve_loc_data.y));
      let p12 = textureLoad(curveTexture, curve_loc, 0) - vec4<f32>(p, p);
      let p3 = textureLoad(curveTexture, vec2<i32>(curve_loc.x + 1, curve_loc.y), 0).xy - p;
      if (max(max(p12.y, p12.w), p3.y) * ppu.y < -0.5) {
        break;
      }
      let code = calc_root_code(p12.x, p12.z, p3.x);
      if (code != 0u) {
        let roots = solve_vert_poly(p12, p3) * ppu.y;
        if ((code & 1u) != 0u) {
          let c = saturate(roots.x + 0.5);
          ycov = ycov - c;
          yabs = yabs + c;
          ywgt = max(ywgt, saturate(1.0 - abs(roots.x) * 2.0));
        }
        if (code > 1u) {
          let c = saturate(roots.y + 0.5);
          ycov = ycov + c;
          yabs = yabs + c;
          ywgt = max(ywgt, saturate(1.0 - abs(roots.y) * 2.0));
        }
      }
    }

    if (rule == 0u) {
      return calc_coverage(xcov, ycov, xwgt, ywgt);
    }
    let ex = 1.0 - abs((xabs % 2.0) - 1.0);
    let ey = 1.0 - abs((yabs % 2.0) - 1.0);
    return calc_coverage(ex, ey, xwgt, ywgt);
  }

  // A region instance's alpha: its own coverage under its rule, times the
  // clip's coverage when flag bit 1 is set. flags bit 0: even-odd.
  fn region_alpha(p: vec2<f32>, ppu: vec2<f32>, band: vec4<u32>, band_xf: vec4<f32>, flags: u32,
                  clip_band: vec4<u32>, clip_xf: vec4<f32>, log2_width: u32) -> f32 {
    var alpha = region_coverage(p, ppu, vec2<i32>(i32(band.x), i32(band.y)),
                                vec2<i32>(i32(band.z), i32(band.w)), band_xf, flags & 1u, log2_width);
    if ((flags & 2u) != 0u) {
      alpha = alpha * region_coverage(p, ppu, vec2<i32>(i32(clip_band.x), i32(clip_band.y)),
                                      vec2<i32>(i32(clip_band.z), i32(clip_band.w)), clip_xf,
                                      (flags >> 2u) & 1u, log2_width);
    }
    return alpha;
  }
")

;; ---- the region instance row ----

(def instance-words 28)
(def instance-stride (* 4 instance-words))

;; One row per painted cover rectangle. Words: 0-3 rect x y w h · 4-7 band
;; base.xy, max.xy (u32) · 8-11 band transform · 12-15 straight RGBA ·
;; 16 flags (bit 0 even-odd, bit 1 clip on, bit 2 clip even-odd) · 17 group
;; or placement index · 18-19 unused · 20-23 clip band base.xy, max.xy ·
;; 24-27 clip band transform.
(def instance-attributes
  [{:shaderLocation 0 :offset 0 :format "float32x4"}
   {:shaderLocation 1 :offset 16 :format "uint32x4"}
   {:shaderLocation 2 :offset 32 :format "float32x4"}
   {:shaderLocation 3 :offset 48 :format "float32x4"}
   {:shaderLocation 4 :offset 64 :format "uint32x4"}
   {:shaderLocation 5 :offset 80 :format "uint32x4"}
   {:shaderLocation 6 :offset 96 :format "float32x4"}])

(def instance-input-wgsl "
  struct RegionInstance {
    @location(0) rect: vec4<f32>,
    @location(1) band: vec4<u32>,
    @location(2) band_xf: vec4<f32>,
    @location(3) color: vec4<f32>,
    @location(4) tags: vec4<u32>,
    @location(5) clip_band: vec4<u32>,
    @location(6) clip_xf: vec4<f32>,
  };
")

(defn pack-instance
  "Instance description → its 112 bytes.

   {:rect [x y w h] :slot atlas-slot :color [r g b a] :rule :nonzero|:even-odd
    :index group-or-placement-index :clip {:slot :rule} or nil}"
  [{:keys [rect slot color rule index clip]}]
  (let [buffer (js/ArrayBuffer. instance-stride)
        floats (js/Float32Array. buffer)
        uints (js/Uint32Array. buffer)
        [x y w h] rect
        [r g b a] color
        [bx by] (:band-base slot)
        [bmx bmy] (:band-max slot)
        [sx sy ox oy] (:band-xf slot)
        flags (bit-or (if (= :even-odd rule) 1 0)
                      (if clip 2 0)
                      (if (= :even-odd (:rule clip)) 4 0))]
    (aset floats 0 x) (aset floats 1 y) (aset floats 2 w) (aset floats 3 h)
    (aset uints 4 bx) (aset uints 5 by) (aset uints 6 bmx) (aset uints 7 bmy)
    (aset floats 8 sx) (aset floats 9 sy) (aset floats 10 ox) (aset floats 11 oy)
    (aset floats 12 r) (aset floats 13 g) (aset floats 14 b) (aset floats 15 a)
    (aset uints 16 flags) (aset uints 17 index)
    (when-let [cs (:slot clip)]
      (let [[cx cy] (:band-base cs) [cmx cmy] (:band-max cs) [csx csy cox coy] (:band-xf cs)]
        (aset uints 20 cx) (aset uints 21 cy) (aset uints 22 cmx) (aset uints 23 cmy)
        (aset floats 24 csx) (aset floats 25 csy) (aset floats 26 cox) (aset floats 27 coy)))
    (js/Uint8Array. buffer)))

;; ---- the dynamic atlas ----

(def atlas-width 1024)
(def log2-atlas-width 10)

(defn- create-texture
  [^js device format width rows label]
  (.createTexture device (clj->js {:label label
                                   :size {:width width :height rows :depthOrArrayLayers 1}
                                   :format format
                                   :usage (bit-or js/GPUTextureUsage.TEXTURE_BINDING
                                                  js/GPUTextureUsage.COPY_DST)})))

(defn- fresh-plane
  "Device, format, rows, words per texel, label → a plane: texture, view,
   mirror, fill, dirty range, rows."
  [device format rows words label mirror-ctor]
  (let [texture (create-texture device format atlas-width rows label)]
    {:format format :rows rows :words words :label label
     :texture texture :view (.createView texture)
     :mirror (mirror-ctor (* rows atlas-width words))
     :fill 0 :dirty nil}))

(defn create-atlas
  "Device and options → an atlas: a curve plane (rgba32float, two texels per
   quadratic) and a band plane (rg32uint), each 1024 texels wide, with
   CPU mirrors, plus the slots keyed by their owner's pack key."
  [device & {:keys [curve-rows band-rows label] :or {curve-rows 16 band-rows 16 label "coverage"}}]
  {:device device
   :label label
   :log2-width log2-atlas-width
   :!state (atom {:curves (fresh-plane device "rgba32float" curve-rows 4 (str label "/curves") #(js/Float32Array. %))
                  :bands (fresh-plane device "rg32uint" band-rows 2 (str label "/bands") #(js/Uint32Array. %))
                  :slots {}
                  :packs {}
                  :garbage 0})})

(defn- mark-dirty
  [plane from-texel to-texel]
  (let [r0 (quot from-texel atlas-width)
        r1 (quot (max from-texel (dec to-texel)) atlas-width)
        [d0 d1] (:dirty plane)]
    (assoc plane :dirty [(if d0 (min d0 r0) r0) (if d1 (max d1 r1) r1)])))

(defn- grow-plane
  "Plane and required texels → the plane with at least that many, regrown
   by doubling when needed; the old texture is destroyed and the whole
   mirror marked dirty."
  [device plane required]
  (let [capacity (* (:rows plane) atlas-width)]
    (if (<= required capacity)
      plane
      (let [rows (loop [rows (:rows plane)] (if (>= (* rows atlas-width) required) rows (recur (* 2 rows))))
            texture (create-texture device (:format plane) atlas-width rows (:label plane))
            mirror (if (= "rgba32float" (:format plane))
                     (js/Float32Array. (* rows atlas-width (:words plane)))
                     (js/Uint32Array. (* rows atlas-width (:words plane))))]
        (.set mirror (:mirror plane))
        (.destroy ^js (:texture plane))
        (-> plane
            (assoc :rows rows :texture texture :view (.createView texture) :mirror mirror)
            (mark-dirty 0 (* rows atlas-width))
            (assoc :regrown? true))))))

(defn- write-pack
  "State, key, pack → state with the pack written into both mirrors at the
   fill points and a slot recorded."
  [device state key pack]
  (let [n (:count pack)
        curve-texels (* 2 n)
        band-texels (:band-texels pack)
        curves (grow-plane device (:curves state) (+ (:fill (:curves state)) curve-texels))
        bands (grow-plane device (:bands state) (+ (:fill (:bands state)) band-texels))
        curve-base (:fill curves)
        band-base (:fill bands)
        ^js cm (:mirror curves)
        ^js bm (:mirror bands)
        hb (:h-bands pack) vb (:v-bands pack)]
    (doseq [[i [x1 y1 cx cy x3 y3]] (map-indexed vector (:quads pack))]
      (let [t (* 4 (+ curve-base (* 2 i)))]
        (aset cm t x1) (aset cm (+ t 1) y1) (aset cm (+ t 2) cx) (aset cm (+ t 3) cy)
        (aset cm (+ t 4) x3) (aset cm (+ t 5) y3) (aset cm (+ t 6) 0.0) (aset cm (+ t 7) 0.0)))
    (let [curve-loc (fn [i] (let [t (+ curve-base (* 2 i))] [(mod t atlas-width) (quot t atlas-width)]))]
      (loop [lists (concat (:h-lists pack) (:v-lists pack))
             header 0
             offset (+ hb vb)]
        (when-let [l (first lists)]
          (let [h (* 2 (+ band-base header))]
            (aset bm h (count l))
            (aset bm (+ h 1) offset)
            (doseq [[j i] (map-indexed vector l)]
              (let [[cx cy] (curve-loc i)
                    e (* 2 (+ band-base offset j))]
                (aset bm e cx)
                (aset bm (+ e 1) cy)))
            (recur (next lists) (inc header) (+ offset (count l)))))))
    (-> state
        (assoc :curves (-> curves (assoc :fill (+ curve-base curve-texels)) (mark-dirty curve-base (+ curve-base curve-texels))))
        (assoc :bands (-> bands (assoc :fill (+ band-base band-texels)) (mark-dirty band-base (+ band-base band-texels))))
        (assoc-in [:slots key] {:curve-base curve-base :curve-texels curve-texels
                                :band-base [(mod band-base atlas-width) (quot band-base atlas-width)]
                                :band-texels band-texels
                                :band-max [(dec vb) (dec hb)]
                                :band-xf (:band-xf pack)})
        (assoc-in [:packs key] pack))))

(defn insert!
  "Atlas, key, pack → the slot for that key, writing the pack when new."
  [atlas key pack]
  (or (get-in @(:!state atlas) [:slots key])
      (do (swap! (:!state atlas) (fn [state] (write-pack (:device atlas) state key pack)))
          (get-in @(:!state atlas) [:slots key]))))

(defn slot
  "Atlas and key → its slot or nil."
  [atlas key]
  (get-in @(:!state atlas) [:slots key]))

(defn- rebuild
  "State and the keys to keep → the state with only those packs, written
   afresh from the start of both planes."
  [device state keys]
  (let [empty-state (-> state
                        (assoc-in [:curves :fill] 0)
                        (assoc-in [:bands :fill] 0)
                        (assoc :slots {} :garbage 0)
                        (update :curves mark-dirty 0 (* (:rows (:curves state)) atlas-width))
                        (update :bands mark-dirty 0 (* (:rows (:bands state)) atlas-width)))]
    (reduce (fn [s key] (write-pack device s key (get-in state [:packs key])))
            (assoc empty-state :packs (select-keys (:packs state) keys))
            keys)))

(defn retain!
  "Atlas and the set of keys still in use → the count of slots dropped.
   Dropped slots become garbage; when garbage passes half of what is
   written, the live packs are rewritten compactly."
  [atlas keys]
  (let [state @(:!state atlas)
        dropped (remove keys (clojure.core/keys (:slots state)))
        dropped-texels (reduce + (map (fn [k] (get-in state [:slots k :curve-texels])) dropped))]
    (when (seq dropped)
      (swap! (:!state atlas)
             (fn [state]
               (let [state (-> state
                               (update :slots #(apply dissoc % dropped))
                               (update :packs #(apply dissoc % dropped))
                               (update :garbage + dropped-texels))]
                 (if (> (:garbage state) (/ (:fill (:curves state)) 2))
                   (rebuild (:device atlas) state (vec (clojure.core/keys (:slots state))))
                   state)))))
    (count dropped)))

(defn- flush-plane!
  [^js device plane]
  (if-let [[r0 r1] (:dirty plane)]
    (let [rows (inc (- r1 r0))
          words (:words plane)
          bytes-per-row (* 4 words atlas-width)
          ^js mirror (:mirror plane)
          view (js/Uint8Array. (.-buffer mirror) (* r0 bytes-per-row) (* rows bytes-per-row))]
      (.writeTexture (.-queue device)
                     (clj->js {:texture (:texture plane) :origin {:x 0 :y r0 :z 0}})
                     view
                     (clj->js {:bytesPerRow bytes-per-row :rowsPerImage rows})
                     (clj->js {:width atlas-width :height rows :depthOrArrayLayers 1}))
      [(dissoc plane :dirty :regrown?) rows (boolean (:regrown? plane))])
    [plane 0 false]))

(defn flush!
  "Atlas → {:curve-rows :band-rows :regrown?}: uploads the dirty rows of
   each plane, once per plane."
  [atlas]
  (let [device (:device atlas)
        state @(:!state atlas)
        [curves c-rows c-regrown] (flush-plane! device (:curves state))
        [bands b-rows b-regrown] (flush-plane! device (:bands state))]
    (swap! (:!state atlas) assoc :curves curves :bands bands)
    {:curve-rows c-rows :band-rows b-rows :regrown? (or c-regrown b-regrown)}))

(defn views
  "Atlas → its current curve and band texture views; a regrow replaces
   them, so bind groups are rebuilt after a flush that regrew."
  [atlas]
  (let [state @(:!state atlas)]
    {:curve-view (:view (:curves state)) :band-view (:view (:bands state))}))

(defn stats
  [atlas]
  (let [state @(:!state atlas)]
    {:slots (count (:slots state))
     :curve-texels (:fill (:curves state)) :curve-rows (:rows (:curves state))
     :band-texels (:fill (:bands state)) :band-rows (:rows (:bands state))
     :garbage (:garbage state)}))

(defn destroy-atlas!
  [atlas]
  (let [state @(:!state atlas)]
    (.destroy ^js (:texture (:curves state)))
    (.destroy ^js (:texture (:bands state)))
    (reset! (:!state atlas) nil)
    nil))

(defn bind-group-layout-entries
  "Visibility flag → the two texture entries every coverage pipeline binds
   at 0 and 1."
  [visibility]
  [{:binding 0 :visibility visibility :texture {:sampleType "unfilterable-float"}}
   {:binding 1 :visibility visibility :texture {:sampleType "uint"}}])
