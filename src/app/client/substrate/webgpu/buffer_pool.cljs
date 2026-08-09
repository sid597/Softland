(ns app.client.substrate.webgpu.buffer-pool
  "Slot-based GPU buffer pool for differential rendering.
   Default: 29 words (116 bytes) per rect — 28 floats + container u32
   (scene-substrate P2). Configurable for other item types (e.g. shadows:
   21 words, 84 bytes) via :floats-per-item and :pack-fn.
   Supports per-slot updates via writeBuffer for O(1) partial writes,
   and batch-update with diff for O(changed) bulk sync."
  (:require [clojure.set]
            [app.client.substrate.webgpu.gpu-budget :as gpu-budget]))

(def floats-per-rect 29)
(def bytes-per-rect 116) ;; 29 × 4 (28 floats + container u32)

(defn- make-buffer [^js device capacity bytes-per-item]
  (.createBuffer device
    (clj->js {:size (* capacity bytes-per-item)
              :usage (bit-or js/GPUBufferUsage.VERTEX
                             js/GPUBufferUsage.COPY_DST
                             js/GPUBufferUsage.COPY_SRC)})))

(defn- pack-rect
  "Pack a rect map into a Float32Array(29). Same layout as renderer/update-rects."
  [rect-map]
  (let [data (js/Float32Array. floats-per-rect)
        u32-view (js/Uint32Array. (.-buffer data))
        {:keys [x y w h r g b a]} rect-map
        cr (:corner-radii rect-map)
        uniform-r (or (:radius rect-map) 0.0)
        bw (:border-widths rect-map)
        uniform-bw (or (:border-width rect-map) 0.0)
        bc (or (:border-color rect-map) [0 0 0 0])
        gr (or (:gradient rect-map) [0 0 0 0])
        gc2 (or (:gradient-color2 rect-map) [0 0 0 0])]
    ;; Slot 0: rect_geometry [x y w h]
    (aset data 0 (or x 0)) (aset data 1 (or y 0))
    (aset data 2 (or w 0)) (aset data 3 (or h 0))
    ;; Slot 1: color [r g b a]
    (aset data 4 (or r 0)) (aset data 5 (or g 0))
    (aset data 6 (or b 0)) (aset data 7 (or a 0))
    ;; Slot 2: corner_radii [tl tr br bl]
    (if cr
      (do (aset data 8  (nth cr 0)) (aset data 9  (nth cr 1))
          (aset data 10 (nth cr 2)) (aset data 11 (nth cr 3)))
      (do (aset data 8  uniform-r) (aset data 9  uniform-r)
          (aset data 10 uniform-r) (aset data 11 uniform-r)))
    ;; Slot 3: border_widths [top right bottom left]
    (if bw
      (do (aset data 12 (nth bw 0)) (aset data 13 (nth bw 1))
          (aset data 14 (nth bw 2)) (aset data 15 (nth bw 3)))
      (do (aset data 12 uniform-bw) (aset data 13 uniform-bw)
          (aset data 14 uniform-bw) (aset data 15 uniform-bw)))
    ;; Slot 4: border_color [r g b a]
    (aset data 16 (nth bc 0)) (aset data 17 (nth bc 1))
    (aset data 18 (nth bc 2)) (aset data 19 (nth bc 3))
    ;; Slot 5: gradient [angle t_stop 0 0]
    (aset data 20 (nth gr 0)) (aset data 21 (nth gr 1))
    (aset data 22 (nth gr 2)) (aset data 23 (nth gr 3))
    ;; Slot 6: gradient_color2 [r g b a]
    (aset data 24 (nth gc2 0)) (aset data 25 (nth gc2 1))
    (aset data 26 (nth gc2 2)) (aset data 27 (nth gc2 3))
    ;; Slot 7: container_idx (u32 view; trap T6 default 0 = identity world)
    (aset u32-view 28 (or (:container-idx rect-map) 0))
    data))

(defn pack-shadow
  "Pack a shadow map into a Float32Array(21). Same GPU layout as renderer/update-shadows.
   21 words = 84 bytes: expanded_rect, shadow_color, corner_radii, blur_params,
   inner_rect, container u32 (scene-substrate P2)"
  [shadow-map]
  (let [data (js/Float32Array. 21)
        u32-view (js/Uint32Array. (.-buffer data))
        {:keys [x y w h blur offset-x offset-y spread color radius corner-radii]} shadow-map
        blur   (or blur 8.0)
        ox     (or offset-x 0.0)
        oy     (or offset-y 0.0)
        spread (or spread 0.0)
        sc     (or color [0 0 0 0.25])
        expand (* 3.0 blur)
        ;; Expanded quad (captures Gaussian tail)
        ex     (- x expand (max ox 0))
        ey     (- y expand (max oy 0))
        ew     (+ w (* 2 expand) (Math/abs ox))
        eh     (+ h (* 2 expand) (Math/abs oy))
        ;; Inner rect relative to expanded quad origin
        ix     (- x ex)
        iy     (- y ey)
        cr     corner-radii
        ur     (or radius 0.0)]
    ;; Slot 0: expanded_rect
    (aset data 0 ex) (aset data 1 ey)
    (aset data 2 ew) (aset data 3 eh)
    ;; Slot 1: shadow_color
    (aset data 4 (nth sc 0)) (aset data 5 (nth sc 1))
    (aset data 6 (nth sc 2)) (aset data 7 (nth sc 3))
    ;; Slot 2: corner_radii
    (if cr
      (do (aset data 8  (nth cr 0)) (aset data 9  (nth cr 1))
          (aset data 10 (nth cr 2)) (aset data 11 (nth cr 3)))
      (do (aset data 8  ur) (aset data 9  ur)
          (aset data 10 ur) (aset data 11 ur)))
    ;; Slot 3: blur_params [blur, offset_x, offset_y, spread]
    (aset data 12 blur) (aset data 13 ox)
    (aset data 14 oy) (aset data 15 spread)
    ;; Slot 4: inner_rect (relative to expanded quad)
    (aset data 16 ix) (aset data 17 iy)
    (aset data 18 w) (aset data 19 h)
    ;; Slot 5: container_idx (u32 view; trap T6 default 0 = identity world)
    (aset u32-view 20 (or (:container-idx shadow-map) 0))
    data))

(defn create-pool
  "Create a slot-based GPU buffer pool. Returns an atom.
   pipeline/bind-group are shared with the rendering system (same shader, same camera).
   Optional :floats-per-item (default 28) and :pack-fn (default pack-rect)
   allow the pool to manage different item types (rects, shadows, etc.)."
  [device initial-capacity pipeline bind-group
   & {:keys [floats-per-item pack-fn tracker label]
      :or {floats-per-item floats-per-rect pack-fn pack-rect}}]
  (let [bytes-per-item (* floats-per-item 4)]
    (atom (let [buffer (make-buffer device initial-capacity bytes-per-item)]
            (gpu-budget/register-buffer! tracker buffer (or label "pool/unnamed")
                                         (* initial-capacity bytes-per-item)
                                         :active-bytes 0)
            {:device device
           :frame-input/identity (js-obj)
           :!shape-rev (atom 0)
           :buffer buffer
           :capacity initial-capacity
           :free-list ()
           :active-slots #{}
           :high-water-mark 0
           :pipeline pipeline
           :bind-group bind-group
           :floats-per-item floats-per-item
           :bytes-per-item bytes-per-item
           :pack-fn pack-fn
            :gpu-tracker tracker
            :gpu-label (or label "pool/unnamed")
           :generations (vec (repeat initial-capacity 0))
           :prev-rects nil}))))

(defn- bump-shape-on-count-boundary! [pool prior-count next-count]
  (when (not= (pos? prior-count) (pos? next-count))
    (swap! (:!shape-rev @pool) inc)))

(defn- grow-pool!
  "Double the pool's buffer capacity, copying existing data via command encoder."
  [pool]
  (let [{:keys [^js device ^js buffer capacity high-water-mark bytes-per-item gpu-tracker gpu-label]} @pool
        new-capacity (* capacity 2)
        new-buffer (make-buffer device new-capacity bytes-per-item)
        copy-bytes (* high-water-mark bytes-per-item)]
    (when (pos? copy-bytes)
      (let [encoder (.createCommandEncoder device)]
        (.copyBufferToBuffer ^js encoder buffer 0 new-buffer 0 copy-bytes)
        (.submit (.-queue device) #js [(.finish ^js encoder)])))
    (gpu-budget/replace-buffer! gpu-tracker buffer new-buffer gpu-label
                                (* new-capacity bytes-per-item)
                                :active-bytes copy-bytes
                                :reason :pool-grow)
    (.destroy buffer)
    (swap! pool #(-> %
                     (assoc :buffer new-buffer :capacity new-capacity)
                     (update :generations into (repeat capacity 0))))))

(defn- ensure-capacity! [pool needed]
  (while (> needed (:capacity @pool))
    (grow-pool! pool)))

;; ============================================================================
;; RAW PER-SLOT API (internal — used by diff engines. External callers use handles.)
;; ============================================================================

(defn allocate-slot!
  "Allocate a slot from the pool. Returns slot index.
   Takes from free-list, or advances high-water-mark (growing buffer if needed)."
  [pool]
  (let [{:keys [free-list high-water-mark]} @pool]
    (if (seq free-list)
      (let [slot (first free-list)]
        (swap! pool #(-> % (update :free-list rest) (update :active-slots conj slot)))
        slot)
      (do
        (ensure-capacity! pool (inc high-water-mark))
        (let [slot high-water-mark]
          (bump-shape-on-count-boundary! pool high-water-mark
                                         (inc high-water-mark))
          (swap! pool #(-> % (update :high-water-mark inc) (update :active-slots conj slot)))
          slot)))))

(defn free-slot!
  "Free a slot, zeroing its GPU data to prevent ghost rendering."
  [pool slot-index]
  (let [{:keys [^js device ^js buffer floats-per-item bytes-per-item]} @pool
        zeros (js/Float32Array. floats-per-item)]
    (.writeBuffer (.-queue device) buffer (* slot-index bytes-per-item) zeros))
  (swap! pool #(-> % (update :free-list conj slot-index)
                     (update :active-slots disj slot-index)
                     (update-in [:generations slot-index] inc)))
  nil)

(defn update-slot!
  "Write a single item to a specific slot. O(1) GPU write."
  [pool slot-index item-map]
  (let [{:keys [^js device ^js buffer bytes-per-item pack-fn]} @pool
        data (pack-fn item-map)]
    (.writeBuffer (.-queue device) buffer (* slot-index bytes-per-item) data))
  nil)

;; ============================================================================
;; KEYED DIFF API (Phase 5: differential rendering by identity)
;; ============================================================================

(defn keyed-diff-update-pool!
  "Sync pool contents with a keyed rect list. Each rect must have an :id field.
   Allocates new slots for new IDs, updates changed rects, frees removed IDs.
   Returns {:added N :updated N :freed N :total-writes N} for diagnostics.

   This is the Missionary-side equivalent of what e/for-by would do:
   - new ID → allocate-slot! + update-slot!
   - same ID, changed rect → update-slot!
   - removed ID → free-slot!"
  [pool new-rects]
  (let [new-rects (or new-rects [])
        {:keys [id->slot prev-keyed-rects]} @pool
        id->slot (or id->slot {})
        prev-keyed (or prev-keyed-rects {})
        new-keyed (into {} (map (fn [r] [(:id r) r])) new-rects)
        new-ids (set (keys new-keyed))
        old-ids (set (keys prev-keyed))
        added-ids (clojure.set/difference new-ids old-ids)
        removed-ids (clojure.set/difference old-ids new-ids)
        kept-ids (clojure.set/intersection new-ids old-ids)
        added (volatile! 0)
        updated (volatile! 0)
        freed (volatile! 0)
        ;; Free removed slots
        new-id->slot (reduce (fn [m id]
                               (when-let [slot (get m id)]
                                 (free-slot! pool slot))
                               (vswap! freed inc)
                               (dissoc m id))
                             id->slot removed-ids)
        ;; Allocate + write new slots
        new-id->slot (reduce (fn [m id]
                               (let [slot (allocate-slot! pool)
                                     rect (get new-keyed id)]
                                 (update-slot! pool slot rect)
                                 (vswap! added inc)
                                 (assoc m id slot)))
                             new-id->slot added-ids)
        ;; Update changed kept slots
        new-id->slot (reduce (fn [m id]
                               (let [old-rect (get prev-keyed id)
                                     new-rect (get new-keyed id)]
                                 (when-not (= old-rect new-rect)
                                   (when-let [slot (get m id)]
                                     (update-slot! pool slot new-rect))
                                   (vswap! updated inc))
                                 m))
                             new-id->slot kept-ids)]
    (swap! pool assoc
           :id->slot new-id->slot
           :prev-keyed-rects new-keyed
           :high-water-mark (max (:high-water-mark @pool)
                                 (count (:active-slots @pool))))
    (gpu-budget/set-active-bytes! (:gpu-tracker @pool) (:buffer @pool) (* (count new-rects) (:bytes-per-item @pool)))
    {:added @added :updated @updated :freed @freed
     :total-writes (+ @added @updated @freed)}))

;; ============================================================================
;; ORDERED KEYED API (Phase 6A: identity-based diff with z-order preservation)
;; ============================================================================

(defn ordered-diff-update-pool!
  "Sync pool with a rect list, maintaining input order in the buffer.
   Slot i always holds the i-th input rect, preserving draw order (z-correctness).
   Identity tracking via :id skips unchanged rects at unchanged positions.
   Returns {:added :updated :freed :total-writes}."
  [pool new-rects]
  (let [new-rects (or new-rects [])
        new-n (count new-rects)
        {:keys [^js device ordered-ids prev-keyed-rects bytes-per-item pack-fn floats-per-item]} @pool
        prev-ids (or ordered-ids [])
        prev-n (count prev-ids)
        prev-keyed (or prev-keyed-rects {})
        new-keyed (into {} (map (fn [r] [(:id r) r])) new-rects)
        new-ids (mapv :id new-rects)
        added (volatile! 0)
        updated (volatile! 0)
        freed (volatile! 0)]
    (ensure-capacity! pool new-n)
    (let [^js buf (:buffer @pool)]
      ;; Write rects where identity shifted or content changed
      (dotimes [i new-n]
        (let [rect (nth new-rects i)
              id (:id rect)
              prev-id-at-pos (when (< i prev-n) (nth prev-ids i))
              prev-rect (get prev-keyed id)]
          (when (or (nil? prev-rect)              ;; new ID
                    (not= id prev-id-at-pos)      ;; different ID at this slot
                    (not= rect prev-rect))         ;; same ID, content changed
            (let [data (pack-fn rect)]
              (.writeBuffer (.-queue device) buf (* i bytes-per-item) data)
              (if (nil? prev-rect)
                (vswap! added inc)
                (vswap! updated inc))))))
      ;; Zero freed tail
      (let [old-hwm (:high-water-mark @pool)]
        (when (> old-hwm new-n)
          (let [zero-count (- old-hwm new-n)
                zeros (js/Float32Array. (* zero-count floats-per-item))]
            (.writeBuffer (.-queue device) buf (* new-n bytes-per-item) zeros)
            (vswap! freed + zero-count)))))
    (bump-shape-on-count-boundary! pool prev-n new-n)
    (swap! pool assoc
           :ordered-ids new-ids
           :prev-keyed-rects new-keyed
           :high-water-mark new-n)
    (gpu-budget/set-active-bytes! (:gpu-tracker @pool) (:buffer @pool) (* new-n bytes-per-item))
    {:added @added :updated @updated :freed @freed
     :total-writes (+ @added @updated @freed)}))

;; ============================================================================
;; BATCH API (for Missionary-based diff: compare new vs previous rect list)
;; ============================================================================

(defn batch-update-pool!
  "Sync pool contents with a new rect list. Compares each rect against the
   previous list and only issues writeBuffer for changed rects.
   Returns the number of GPU writes performed (for diagnostics)."
  [pool new-rects]
  (let [new-rects (or new-rects [])
        new-n (count new-rects)
        {:keys [^js device prev-rects bytes-per-item pack-fn floats-per-item]} @pool
        prev-n (count (or prev-rects []))
        writes (volatile! 0)]
    ;; Grow buffer if needed
    (ensure-capacity! pool new-n)
    ;; Re-read buffer after potential grow (grow replaces :buffer)
    (let [^js buf (:buffer @pool)]
      ;; Write changed items
      (dotimes [i new-n]
        (let [rect (nth new-rects i)
              prev-rect (when (< i prev-n) (nth prev-rects i))]
          (when-not (= rect prev-rect)
            (let [data (pack-fn rect)]
              (.writeBuffer (.-queue device) buf (* i bytes-per-item) data)
              (vswap! writes inc)))))
      ;; Zero freed slots (list shrank)
      (when (> prev-n new-n)
        (let [zero-count (- prev-n new-n)
              zeros (js/Float32Array. (* zero-count floats-per-item))]
          (.writeBuffer (.-queue device) buf (* new-n bytes-per-item) zeros)
          (vswap! writes + zero-count))))
    ;; Update pool state
    (bump-shape-on-count-boundary! pool prev-n new-n)
    (swap! pool assoc
           :prev-rects new-rects
           :high-water-mark new-n)
    (gpu-budget/set-active-bytes! (:gpu-tracker @pool) (:buffer @pool) (* new-n bytes-per-item))
    @writes))

(defn pool-draw-info
  "Get draw parameters for the render pass.
   The stable pool reference lets retained entries resolve a grown buffer at
   encode time; the scalar fields remain for existing immediate callers."
  [pool]
  (let [{:keys [buffer high-water-mark pipeline bind-group
                frame-input/identity !shape-rev]} @pool]
    {:pool pool
     :frame-input/identity identity
     :frame-input/shape-rev @!shape-rev
     :buffer buffer
     :draw-count high-water-mark
     :pipeline pipeline
     :bind-group bind-group}))

;; ============================================================================
;; HANDLE-CHECKED API (Phase 6D: safe per-slot access for external callers)
;; Raw slot indices stay internal. External code holds handles {:slot :gen}.
;; ============================================================================

(defn allocate-handle!
  "Allocate a slot and return a handle {:slot idx :gen g}.
   If item-map is provided, writes it to the slot immediately."
  ([pool] (allocate-handle! pool nil))
  ([pool item-map]
   (let [slot (allocate-slot! pool)
         gen (nth (:generations @pool) slot)]
     (when item-map
       (update-slot! pool slot item-map))
     {:slot slot :gen gen})))

(defn- validate-handle
  "Check handle against pool state. Returns slot index if valid, nil if stale.
   Degrades to warning on malformed input — never throws."
  [pool handle]
  (if-not (and (map? handle) (integer? (:slot handle)) (integer? (:gen handle)))
    (do (js/console.warn "[POOL] Malformed handle:" (pr-str handle)) nil)
    (let [{:keys [slot gen]} handle
          {:keys [generations active-slots]} @pool]
      (cond
        (or (neg? slot) (>= slot (count generations)))
        (do (js/console.warn "[POOL] Handle out of range: slot" slot "capacity" (count generations))
            nil)

        (not= gen (nth generations slot))
        (do (js/console.warn "[POOL] Stale handle: slot" slot "expected gen" (nth generations slot) "got" gen)
            nil)

        (not (contains? active-slots slot))
        (do (js/console.warn "[POOL] Handle references inactive slot:" slot)
            nil)

        :else slot))))

(defn update-handle!
  "Write item data to a handle's slot. Returns handle if valid, nil if stale."
  [pool handle item-map]
  (when-let [slot (validate-handle pool handle)]
    (update-slot! pool slot item-map)
    handle))

(defn free-handle!
  "Free a handle's slot. Returns true if successful, nil if stale."
  [pool handle]
  (when-let [slot (validate-handle pool handle)]
    (free-slot! pool slot)
    true))

;; ============================================================================
;; MOUNT CALLBACKS (Phase 6D: bridge between Electric e/for-by and GPU pool)
;; ============================================================================

(defn- vec-index-of
  "Find index of x in vector v by identity. Returns index or nil.
   Uses identical? — handles must be the same object, not structural copies."
  [v x]
  (first (keep-indexed (fn [i h] (when (identical? h x) i)) v)))

(defn gpu-mount
  "Create mount callbacks shaped for hyperfiddle.incseq.mount-impl/mount
   (a POSITIONAL 5-arg fn; there is NO public incseq/mount var).
   Maintains internal ordering state for position-based child lookup.
   Callbacks handle allocation/deallocation — callers pass item data in,
   get handles out. nth-child returns the handle at position i.

   ⚠️ DO NOT WIRE AS-IS (2026-07-05, VERDICTS.md Claim 15 / PROBE-10K §4):
   the mount contract passes existing CHILD HANDLES back through
   insert-before during :permutation rotations (DOM insertBefore = MOVE);
   this implementation allocates a fresh slot on every insert, so any
   :permutation corrupts the pool (measured: 16,750 active slots for 100
   entities after 91 rotate frames). The scene store must consume the six
   diff ops directly (C2 shape) instead of this DOM-shaped bridge.

   Contract shape, for reference only:
     (let [{:keys [append-child replace-child insert-before
                   remove-child nth-child]} (gpu-mount pool)]
       (hyperfiddle.incseq.mount-impl/mount
        append-child replace-child insert-before remove-child nth-child))"
  [pool]
  (let [!children (atom [])]
    {:append-child
     (fn [_element item]
       (let [handle (allocate-handle! pool item)]
         (swap! !children conj handle)
         handle))

     :replace-child
     (fn [_element new-item old-handle]
       (update-handle! pool old-handle new-item)
       old-handle)

     :insert-before
     (fn [_element item sibling]
       (let [handle (allocate-handle! pool item)]
         (swap! !children
                (fn [v]
                  (let [idx (vec-index-of v sibling)]
                    (if (nil? idx)
                      (conj v handle)
                      (into (conj (subvec v 0 idx) handle) (subvec v idx))))))
         handle))

     :remove-child
     (fn [_element handle]
       (free-handle! pool handle)
       (swap! !children (fn [v] (filterv #(not (identical? % handle)) v))))

     :nth-child
     (fn [_element i]
       (nth @!children i nil))}))
