(ns app.client.engine.buffer-pool
  "A GPU instance buffer that writes only changed items.
   Takes: a device, an initial capacity, an item width and a pack function;
   then a new item list each frame.
   Gives: the number of GPU writes made.
   Holds: the buffer, capacity, previous items, and live item count.")

(defn- make-buffer [^js device capacity bytes-per-item]
  (.createBuffer device
    (clj->js {:size (* capacity bytes-per-item)
              :usage (bit-or js/GPUBufferUsage.VERTEX
                             js/GPUBufferUsage.COPY_DST
                             js/GPUBufferUsage.COPY_SRC)})))

(defn create-pool
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

(defn- ensure-capacity! [pool needed]
  (while (> needed (:capacity @pool))
    (grow-pool! pool)))

(defn batch-update-pool!
  "Sync the buffer with a new item list and return the GPU write count."
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
