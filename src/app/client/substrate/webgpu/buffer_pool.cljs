(ns app.client.substrate.webgpu.buffer-pool
  "Slot-based GPU buffer pool for differential rect rendering.
   Each slot holds one rect (28 floats = 112 bytes).
   Supports per-slot updates via writeBuffer for O(1) partial writes,
   and batch-update with diff for O(changed) bulk sync.")

(def floats-per-rect 28)
(def bytes-per-rect 112) ;; 28 × 4

(defn- make-buffer [^js device capacity]
  (.createBuffer device
    (clj->js {:size (* capacity bytes-per-rect)
              :usage (bit-or js/GPUBufferUsage.VERTEX
                             js/GPUBufferUsage.COPY_DST
                             js/GPUBufferUsage.COPY_SRC)})))

(defn create-pool
  "Create a slot-based GPU buffer pool. Returns an atom.
   pipeline/bind-group are shared with the rect system (same shader, same camera)."
  [device initial-capacity pipeline bind-group]
  (atom {:device device
         :buffer (make-buffer device initial-capacity)
         :capacity initial-capacity
         :free-list ()
         :active-slots #{}
         :high-water-mark 0
         :pipeline pipeline
         :bind-group bind-group
         :prev-rects nil}))

(defn- grow-pool!
  "Double the pool's buffer capacity, copying existing data via command encoder."
  [pool]
  (let [{:keys [^js device ^js buffer capacity high-water-mark]} @pool
        new-capacity (* capacity 2)
        new-buffer (make-buffer device new-capacity)
        copy-bytes (* high-water-mark bytes-per-rect)]
    (when (pos? copy-bytes)
      (let [encoder (.createCommandEncoder device)]
        (.copyBufferToBuffer ^js encoder buffer 0 new-buffer 0 copy-bytes)
        (.submit (.-queue device) #js [(.finish ^js encoder)])))
    (.destroy buffer)
    (swap! pool assoc :buffer new-buffer :capacity new-capacity)))

(defn- ensure-capacity! [pool needed]
  (while (> needed (:capacity @pool))
    (grow-pool! pool)))

(defn- pack-rect
  "Pack a rect map into a Float32Array(28). Same layout as renderer/update-rects."
  [rect-map]
  (let [data (js/Float32Array. floats-per-rect)
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
    data))

;; ============================================================================
;; PER-SLOT API (for future Electric e/for-by differential rendering)
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
          (swap! pool #(-> % (update :high-water-mark inc) (update :active-slots conj slot)))
          slot)))))

(defn free-slot!
  "Free a slot, zeroing its GPU data to prevent ghost rendering."
  [pool slot-index]
  (let [{:keys [^js device ^js buffer]} @pool
        zeros (js/Float32Array. floats-per-rect)]
    (.writeBuffer (.-queue device) buffer (* slot-index bytes-per-rect) zeros))
  (swap! pool #(-> % (update :free-list conj slot-index)
                     (update :active-slots disj slot-index)))
  nil)

(defn update-slot!
  "Write a single rect to a specific slot. O(1) GPU write — 112 bytes."
  [pool slot-index rect-map]
  (let [{:keys [^js device ^js buffer]} @pool
        data (pack-rect rect-map)]
    (.writeBuffer (.-queue device) buffer (* slot-index bytes-per-rect) data))
  nil)

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
        {:keys [^js device prev-rects]} @pool
        prev-n (count (or prev-rects []))
        writes (volatile! 0)]
    ;; Grow buffer if needed
    (ensure-capacity! pool new-n)
    ;; Re-read buffer after potential grow (grow replaces :buffer)
    (let [^js buf (:buffer @pool)]
      ;; Write changed rects
      (dotimes [i new-n]
        (let [rect (nth new-rects i)
              prev-rect (when (< i prev-n) (nth prev-rects i))]
          (when-not (= rect prev-rect)
            (let [data (pack-rect rect)]
              (.writeBuffer (.-queue device) buf (* i bytes-per-rect) data)
              (vswap! writes inc)))))
      ;; Zero freed slots (list shrank)
      (when (> prev-n new-n)
        (let [zero-count (- prev-n new-n)
              zeros (js/Float32Array. (* zero-count floats-per-rect))]
          (.writeBuffer (.-queue device) buf (* new-n bytes-per-rect) zeros))))
    ;; Update pool state
    (swap! pool assoc
           :prev-rects new-rects
           :high-water-mark new-n)
    @writes))

(defn pool-draw-info
  "Get draw parameters for the render pass.
   Returns {:buffer :draw-count :pipeline :bind-group}."
  [pool]
  (let [{:keys [buffer high-water-mark pipeline bind-group]} @pool]
    {:buffer buffer
     :draw-count high-water-mark
     :pipeline pipeline
     :bind-group bind-group}))
