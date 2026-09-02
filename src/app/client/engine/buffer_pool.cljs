(ns app.client.engine.buffer-pool
  "A buffer-index pool for GPU instance buffers that writes only the items that changed
   since last time.
   Takes: a device, a capacity, the pipeline and bind group the items draw
   with, an item width and a pack function; then a new item list each frame.
   Gives: the number of GPU writes made; draw parameters for the pool.
   Holds: the pool atom (items, capacity, shape revision)."
  (:require [clojure.set]))

(defn- make-buffer [^js device capacity bytes-per-item]
  (.createBuffer device
    (clj->js {:size (* capacity bytes-per-item)
              :usage (bit-or js/GPUBufferUsage.VERTEX
                             js/GPUBufferUsage.COPY_DST
                             js/GPUBufferUsage.COPY_SRC)})))

(defn create-pool
  "Create a buffer-index-based GPU buffer pool. Returns an atom.
   pipeline/bind-group are shared with the rendering system (same shader, same camera).
   :floats-per-item and :pack-fn are required so the engine carries no implicit
  product geometry."
  [device initial-capacity pipeline bind-group
   & {:keys [floats-per-item pack-fn] :as options}]
  (when-not (and (pos-int? floats-per-item) (fn? pack-fn))
    (throw (ex-info "Buffer pool requires :floats-per-item and :pack-fn"
                    {:options (keys options)})))
  (let [bytes-per-item (* floats-per-item 4)]
    (atom (let [buffer (make-buffer device initial-capacity bytes-per-item)]
            {:device device
           :frame-input/identity (js-obj)
           :!shape-rev (atom 0)
           :buffer buffer
           :capacity initial-capacity
           :free-buffer-indexes ()
           :active-buffer-indexes #{}
           :high-water-mark 0
           :pipeline pipeline
           :bind-group bind-group
           :floats-per-item floats-per-item
           :bytes-per-item bytes-per-item
           :pack-fn pack-fn
           :generations (vec (repeat initial-capacity 0))
           :prev-items nil}))))

(defn- bump-shape-on-count-boundary! [pool prior-count next-count]
  (when (not= (pos? prior-count) (pos? next-count))
    (swap! (:!shape-rev @pool) inc)))

(defn- grow-pool!
  "Double the pool's buffer capacity, copying existing data via command encoder."
  [pool]
  (let [{:keys [^js device ^js buffer capacity high-water-mark bytes-per-item]} @pool
        new-capacity (* capacity 2)
        new-buffer (make-buffer device new-capacity bytes-per-item)
        copy-bytes (* high-water-mark bytes-per-item)]
    (when (pos? copy-bytes)
      (let [encoder (.createCommandEncoder device)]
        (.copyBufferToBuffer ^js encoder buffer 0 new-buffer 0 copy-bytes)
        (.submit (.-queue device) #js [(.finish ^js encoder)])))
    (.destroy buffer)
    (swap! pool #(-> %
                     (assoc :buffer new-buffer :capacity new-capacity)
                     (update :generations into (repeat capacity 0))))))

(defn- ensure-capacity! [pool needed]
  (while (> needed (:capacity @pool))
    (grow-pool! pool)))

;; ============================================================================
;; RAW PER-BUFFER-INDEX API (internal — used by diff engines. External callers use handles.)
;; ============================================================================

(defn allocate-buffer-index!
  "Allocate a buffer-index from the pool. Returns buffer-index index.
   Takes from free-buffer-indexes, or advances high-water-mark (growing buffer if needed)."
  [pool]
  (let [{:keys [free-buffer-indexes high-water-mark]} @pool]
    (if (seq free-buffer-indexes)
      (let [buffer-index (first free-buffer-indexes)]
        (swap! pool #(-> % (update :free-buffer-indexes rest) (update :active-buffer-indexes conj buffer-index)))
        buffer-index)
      (do
        (ensure-capacity! pool (inc high-water-mark))
        (let [buffer-index high-water-mark]
          (bump-shape-on-count-boundary! pool high-water-mark
                                         (inc high-water-mark))
          (swap! pool #(-> % (update :high-water-mark inc) (update :active-buffer-indexes conj buffer-index)))
          buffer-index)))))

(defn free-buffer-index!
  "Free a buffer-index, zeroing its GPU data to prevent ghost rendering."
  [pool buffer-index]
  (let [{:keys [^js device ^js buffer floats-per-item bytes-per-item]} @pool
        zeros (js/Float32Array. floats-per-item)]
    (.writeBuffer (.-queue device) buffer (* buffer-index bytes-per-item) zeros))
  (swap! pool #(-> % (update :free-buffer-indexes conj buffer-index)
                     (update :active-buffer-indexes disj buffer-index)
                     (update-in [:generations buffer-index] inc)))
  nil)

(defn update-buffer-index!
  "Write a single item to a specific buffer-index. O(1) GPU write."
  [pool buffer-index item-map]
  (let [{:keys [^js device ^js buffer bytes-per-item pack-fn]} @pool
        data (pack-fn item-map)]
    (.writeBuffer (.-queue device) buffer (* buffer-index bytes-per-item) data))
  nil)

;; ============================================================================
;; KEYED DIFF API (Phase 5: differential rendering by identity)
;; ============================================================================

(defn keyed-diff-update-pool!
  "Sync pool contents with a keyed item list. Each item must have an :id field.
   Allocates new buffer indexes for new IDs, updates changed items, frees removed IDs.
   Returns {:added N :updated N :freed N :total-writes N} for diagnostics.

   This is the Missionary-side equivalent of what e/for-by would do:
   - new ID → allocate-buffer-index! + update-buffer-index!
   - same ID, changed item → update-buffer-index!
   - removed ID → free-buffer-index!"
  [pool new-items]
  (let [new-items (or new-items [])
        {:keys [id->buffer-index prev-keyed-items]} @pool
        id->buffer-index (or id->buffer-index {})
        prev-keyed (or prev-keyed-items {})
        new-keyed (into {} (map (fn [r] [(:id r) r])) new-items)
        new-ids (set (keys new-keyed))
        old-ids (set (keys prev-keyed))
        added-ids (clojure.set/difference new-ids old-ids)
        removed-ids (clojure.set/difference old-ids new-ids)
        kept-ids (clojure.set/intersection new-ids old-ids)
        added (volatile! 0)
        updated (volatile! 0)
        freed (volatile! 0)
        ;; Free removed buffer indexes
        new-id->buffer-index (reduce (fn [m id]
                               (when-let [buffer-index (get m id)]
                                 (free-buffer-index! pool buffer-index))
                               (vswap! freed inc)
                               (dissoc m id))
                             id->buffer-index removed-ids)
        ;; Allocate + write new buffer indexes
        new-id->buffer-index (reduce (fn [m id]
                               (let [buffer-index (allocate-buffer-index! pool)
                                     item (get new-keyed id)]
                                 (update-buffer-index! pool buffer-index item)
                                 (vswap! added inc)
                                 (assoc m id buffer-index)))
                             new-id->buffer-index added-ids)
        ;; Update changed kept buffer indexes
        new-id->buffer-index (reduce (fn [m id]
                               (let [old-item (get prev-keyed id)
                                     new-item (get new-keyed id)]
                                 (when-not (= old-item new-item)
                                   (when-let [buffer-index (get m id)]
                                     (update-buffer-index! pool buffer-index new-item))
                                   (vswap! updated inc))
                                 m))
                             new-id->buffer-index kept-ids)]
    (swap! pool assoc
           :id->buffer-index new-id->buffer-index
           :prev-keyed-items new-keyed
           :high-water-mark (max (:high-water-mark @pool)
                                 (count (:active-buffer-indexes @pool))))
    {:added @added :updated @updated :freed @freed
     :total-writes (+ @added @updated @freed)}))

;; ============================================================================
;; ORDERED KEYED API (Phase 6A: identity-based diff with z-order preservation)
;; ============================================================================

(defn ordered-diff-update-pool!
  "Sync pool with an item list, maintaining input order in the buffer.
   Buffer index i always holds the i-th input item, preserving draw order (z-correctness).
   Identity tracking via :id skips unchanged items at unchanged positions.
   Returns {:added :updated :freed :total-writes}."
  [pool new-items]
  (let [new-items (or new-items [])
        new-n (count new-items)
        {:keys [^js device ordered-ids prev-keyed-items bytes-per-item pack-fn floats-per-item]} @pool
        prev-ids (or ordered-ids [])
        prev-n (count prev-ids)
        prev-keyed (or prev-keyed-items {})
        new-keyed (into {} (map (fn [r] [(:id r) r])) new-items)
        new-ids (mapv :id new-items)
        added (volatile! 0)
        updated (volatile! 0)
        freed (volatile! 0)]
    (ensure-capacity! pool new-n)
    (let [^js buf (:buffer @pool)]
      ;; Write items where identity shifted or content changed
      (dotimes [i new-n]
        (let [item (nth new-items i)
              id (:id item)
              prev-id-at-pos (when (< i prev-n) (nth prev-ids i))
              prev-item (get prev-keyed id)]
          (when (or (nil? prev-item)              ;; new ID
                    (not= id prev-id-at-pos)      ;; different ID at this buffer-index
                    (not= item prev-item))         ;; same ID, content changed
            (let [data (pack-fn item)]
              (.writeBuffer (.-queue device) buf (* i bytes-per-item) data)
              (if (nil? prev-item)
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
           :prev-keyed-items new-keyed
           :high-water-mark new-n)
    {:added @added :updated @updated :freed @freed
     :total-writes (+ @added @updated @freed)}))

;; ============================================================================
;; BATCH API (for Missionary-based diff: compare new vs previous item list)
;; ============================================================================

(defn batch-update-pool!
  "Sync pool contents with a new item list. Compares each item against the
   previous list and only issues writeBuffer for changed items.
   Returns the number of GPU writes performed (for diagnostics)."
  [pool new-items]
  (let [new-items (or new-items [])
        new-n (count new-items)
        {:keys [^js device prev-items bytes-per-item pack-fn floats-per-item]} @pool
        prev-n (count (or prev-items []))
        writes (volatile! 0)]
    ;; Grow buffer if needed
    (ensure-capacity! pool new-n)
    ;; Re-read buffer after potential grow (grow replaces :buffer)
    (let [^js buf (:buffer @pool)]
      ;; Write changed items
      (dotimes [i new-n]
        (let [item (nth new-items i)
              prev-item (when (< i prev-n) (nth prev-items i))]
          (when-not (= item prev-item)
            (let [data (pack-fn item)]
              (.writeBuffer (.-queue device) buf (* i bytes-per-item) data)
              (vswap! writes inc)))))
      ;; Zero freed buffer indexes (list shrank)
      (when (> prev-n new-n)
        (let [zero-count (- prev-n new-n)
              zeros (js/Float32Array. (* zero-count floats-per-item))]
          (.writeBuffer (.-queue device) buf (* new-n bytes-per-item) zeros)
          (vswap! writes + zero-count))))
    ;; Update pool state
    (bump-shape-on-count-boundary! pool prev-n new-n)
    (swap! pool assoc
           :prev-items new-items
           :high-water-mark new-n)
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
;; HANDLE-CHECKED API (Phase 6D: safe per-buffer-index access for external callers)
;; Raw buffer-index indices stay internal. External code holds handles {:buffer-index :gen}.
;; ============================================================================

(defn allocate-handle!
  "Allocate a buffer-index and return a handle {:buffer-index idx :gen g}.
   If item-map is provided, writes it to the buffer-index immediately."
  ([pool] (allocate-handle! pool nil))
  ([pool item-map]
   (let [buffer-index (allocate-buffer-index! pool)
         gen (nth (:generations @pool) buffer-index)]
     (when item-map
       (update-buffer-index! pool buffer-index item-map))
     {:buffer-index buffer-index :gen gen})))

(defn- validate-handle
  "Check handle against pool state. Returns buffer-index index if valid, nil if stale.
   Degrades to warning on malformed input — never throws."
  [pool handle]
  (if-not (and (map? handle) (integer? (:buffer-index handle)) (integer? (:gen handle)))
    (do (js/console.warn "[POOL] Malformed handle:" (pr-str handle)) nil)
    (let [{:keys [buffer-index gen]} handle
          {:keys [generations active-buffer-indexes]} @pool]
      (cond
        (or (neg? buffer-index) (>= buffer-index (count generations)))
        (do (js/console.warn "[POOL] Handle out of range: buffer-index" buffer-index "capacity" (count generations))
            nil)

        (not= gen (nth generations buffer-index))
        (do (js/console.warn "[POOL] Stale handle: buffer-index" buffer-index "expected gen" (nth generations buffer-index) "got" gen)
            nil)

        (not (contains? active-buffer-indexes buffer-index))
        (do (js/console.warn "[POOL] Handle references inactive buffer-index:" buffer-index)
            nil)

        :else buffer-index))))

(defn update-handle!
  "Write item data to a handle's buffer-index. Returns handle if valid, nil if stale."
  [pool handle item-map]
  (when-let [buffer-index (validate-handle pool handle)]
    (update-buffer-index! pool buffer-index item-map)
    handle))

(defn free-handle!
  "Free a handle's buffer-index. Returns true if successful, nil if stale."
  [pool handle]
  (when-let [buffer-index (validate-handle pool handle)]
    (free-buffer-index! pool buffer-index)
    true))
