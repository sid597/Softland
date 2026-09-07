(ns app.client.engine.buffer-pool
  "Synchronize an ordered instance vector to a GPU buffer.

   Input: device, initial capacity, row width/packer, then successive item
   vectors. Output: an atom-owned pool and a reported update count. State
   retains the GPU buffer, capacity, preceding items and active length. Vector callers compare rows by index; direct-range callers name changed
   rows and the active count, with no population comparison. Evidence:
   harness/path_push.cljs counts actual queue writes and prior-row comparisons.

   Folder map: README.md.")

(defn- make-buffer
  "Device, row capacity, bytes per row → vertex/copy buffer.

   Direct allocation. Intended for the fixed-width row contract."
  [^js device capacity bytes-per-item]
  (.createBuffer device
    (clj->js {:size (* capacity bytes-per-item)
              :usage (bit-or js/GPUBufferUsage.VERTEX
                             js/GPUBufferUsage.COPY_DST
                             js/GPUBufferUsage.COPY_SRC)})))

(defn create-pool
  "Device, capacity, keyword row width/packer → pool atom; throws for
   invalid width/packer.

   Stores packing policy with allocation. Current limitation: initial
   capacity is not required to be positive."
  [device initial-capacity & {:keys [floats-per-item pack-fn] :as options}]
  (when-not (and (pos-int? floats-per-item) (fn? pack-fn))
    (throw (ex-info "Buffer pool requires :floats-per-item and :pack-fn"
                    {:options (keys options)})))
  (let [bytes-per-item (* floats-per-item 4)]
    (atom {:device device
           :buffer (make-buffer device initial-capacity bytes-per-item)
           :capacity initial-capacity
           :high-water-mark 0
           :floats-per-item floats-per-item
           :bytes-per-item bytes-per-item
           :pack-fn pack-fn
           :prev-items nil})))

(defn- grow-pool!
  "Pool atom → updated state; allocates doubled buffer, copies active
   prefix, submits, destroys old buffer.

   Geometric growth. Intended for positive capacity; zero remains zero."
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
    (swap! pool assoc :buffer new-buffer :capacity new-capacity)))

(defn- ensure-capacity!
  "Pool and needed rows → nil after any growth.

   Doubles until enough room. Current limitation: positive need with zero
   starting capacity does not terminate by this logic."
  [pool needed]
  (while (> needed (:capacity @pool))
    (grow-pool! pool)))

(defn batch-update-pool!
  "Pool and new items (nil treated empty) → reported write count; updates
   rows and previous items.

   Equality skips unchanged rows; one zero-filled tail upload clears removed
   rows. Current limitation: the return counts removed rows, although the
   tail uses one API write, so “GPU write count” is not literally queue-call
   count on shrink."
  [pool new-items]
  (let [new-items (or new-items [])
        new-count (count new-items)
        {:keys [^js device prev-items bytes-per-item pack-fn floats-per-item]} @pool
        previous-count (count (or prev-items []))
        writes (volatile! 0)]
    (ensure-capacity! pool new-count)
    (let [^js buffer (:buffer @pool)]
      (dotimes [index new-count]
        (let [item (nth new-items index)
              previous (when (< index previous-count)
                         (nth prev-items index))]
          (when-not (= item previous)
            (.writeBuffer (.-queue device) buffer (* index bytes-per-item)
                          (pack-fn item))
            (vswap! writes inc))))
      (when (> previous-count new-count)
        (let [zero-count (- previous-count new-count)
              zeros (js/Float32Array. (* zero-count floats-per-item))]
          (.writeBuffer (.-queue device) buffer (* new-count bytes-per-item) zeros)
          (vswap! writes + zero-count))))
    (swap! pool assoc :prev-items new-items :high-water-mark new-count)
    @writes))

(defn write-range!
  "Pool, first row, changed rows → one queue write, without comparing any
   previous row. Grows while preserving the active prefix. The caller owns
   row/range validity; use set-count! after all range writes. Evidence:
   harness/path_push.cljs instruments the actual queue and pack calls."
  [pool first-row rows]
  (let [n (count rows)]
    (when (pos? n)
      (ensure-capacity! pool (+ first-row n))
      (let [{:keys [^js device buffer bytes-per-item pack-fn]} @pool
            bytes (js/Uint8Array. (* n bytes-per-item))]
        (doseq [[i row] (map-indexed vector rows)]
          (let [^js packed (pack-fn row)
                raw (js/Uint8Array. (.-buffer packed) (.-byteOffset packed) (.-byteLength packed))]
            (.set bytes raw (* i bytes-per-item))))
        (.writeBuffer (.-queue device) buffer (* first-row bytes-per-item) bytes)
        (swap! pool update :high-water-mark max (+ first-row n))))
    {:ranges (if (pos? n) 1 0) :rows n :comparisons 0}))

(defn set-count!
  "Pool and active row count → nil. Draws stop at this count; an unused
   tail needs no clearing upload. Direct-range callers own their row values."
  [pool n]
  (swap! pool assoc :high-water-mark n :prev-items nil)
  nil)
