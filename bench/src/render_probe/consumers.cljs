(ns render-probe.consumers
  "The three scene-store consumers for the NORTH.md §9 probe, all over the
   REAL product buffer pool (app.client.engine.buffer-pool) —
   read-only use, no product code modified.

   C1 :mount     — the as-built dormant gpu-mount bridge driven by
                   hyperfiddle.incseq.mount-impl/mount (the electric-dom
                   consumer contract). NOTE, found by reading both sides:
                   mount's rotation phase passes existing CHILD HANDLES back
                   through insert-before (DOM insertBefore = move), while
                   gpu-mount's insert-before allocates a fresh buffer-index from
                   whatever it receives — so this bridge is only valid for
                   change / append / tail-shrink diffs; any :permutation
                   corrupts the pool. Measured only where valid, plus one
                   deliberate corruption-demo cell.

   C2 :direct    — the north-shaped consumer (NORTH §2.4): address→buffer-index with
                   an order-indirection vector. :grow → allocate-buffer-index!,
                   :permutation → indirection vector only (no GPU writes),
                   :shrink → free-buffer-index! (buffer-index zeroed by the pool),
                   :change → update-buffer-index! (writeBuffer at buffer-index offset).
                   Phase order mirrors incseq's patch-vec exactly.

   C3 :recollect — keyed-diff-update-pool! fed whole collections: the
                   pre-registered FALLBACK transport (windowed rows re-diffed
                   client-side) behind the same store."
  (:require [app.client.engine.buffer-pool :as pool]
            [hyperfiddle.incseq :as i]
            [hyperfiddle.incseq.perm-impl :as perm]
            [hyperfiddle.incseq.mount-impl :as mi]))

;; ---------------------------------------------------------------------------
;; Devices
;; ---------------------------------------------------------------------------

(defn ensure-gpu-usage-shim!
  "buffer-pool reads js/GPUBufferUsage at pool creation; define it when the
   page has no WebGPU so the stub device can run."
  []
  (when-not (exists? js/GPUBufferUsage)
    (set! (.-GPUBufferUsage js/globalThis)
          #js {:VERTEX 32 :COPY_DST 8 :COPY_SRC 4})))

(defn stub-device
  "CPU-backed stand-in exposing the WebGPU surface buffer-pool touches.
   writeBuffer copies into a backing Float32Array — measures the CPU side of
   the apply path only (no driver/queue cost). Runs are tagged :stub."
  []
  (let [queue #js {:writeBuffer
                   (fn [buf offset data]
                     (.set (.-backing buf) data (/ offset 4)))
                   :submit (fn [_])
                   :onSubmittedWorkDone (fn [] (js/Promise.resolve nil))}]
    #js {:queue queue
         :createBuffer
         (fn [desc]
           (let [size (.-size desc)]
             #js {:size size
                  :backing (js/Float32Array. (/ size 4))
                  :destroy (fn [])}))
         :createCommandEncoder
         (fn []
           #js {:copyBufferToBuffer
                (fn [src so dst dofs nbytes]
                  (.set (.-backing dst)
                        (.subarray (.-backing src) (/ so 4) (/ (+ so nbytes) 4))
                        (/ dofs 4)))
                :finish (fn [] #js {})})}))

(defn request-real-device
  "Promise of {:device d :adapter-info m} or nil when WebGPU is unavailable."
  []
  (if-not (and (exists? js/navigator) (.-gpu js/navigator))
    (js/Promise.resolve nil)
    (-> (.requestAdapter (.-gpu js/navigator))
        (.then (fn [adapter]
                 (if (nil? adapter)
                   nil
                   (-> (.requestDevice adapter)
                       (.then (fn [device]
                                {:device device
                                 :adapter-info
                                 (let [info (.-info adapter)]
                                   {:vendor (some-> info .-vendor)
                                    :architecture (some-> info .-architecture)
                                    :description (some-> info .-description)})}))))))
        (.catch (fn [_] nil)))))

;; ---------------------------------------------------------------------------
;; C2 :direct — permutation cycles applied to the order-indirection vector,
;; identical algorithm to stateful-diff-impl/patch-vec's cycles!.
;; ---------------------------------------------------------------------------

(def ^:private cycles!
  (partial perm/decompose
           (fn [v c]
             (let [i (nth c 0)
                   x (nth v i)]
               (loop [v v, i i, k 1]
                 (let [j (nth c k)
                       v (assoc! v i (nth v j))
                       k (inc k)]
                   (if (< k (count c))
                     (recur v j k)
                     (assoc! v j x))))))))

(defn make-direct [pool*]
  (let [!buffer-indexes (atom []) ;; position -> pool buffer-index index (order indirection)
        !rows (atom [])] ;; semantic half of the store, via the real patch-vec
    {:kind :direct
     :apply-diff!
     (fn [{:keys [grow shrink permutation change] :as diff}]
       (let [v (transient @!buffer-indexes)
             v (loop [k 0, v v]
                 (if (< k grow)
                   (recur (inc k) (conj! v (pool/allocate-buffer-index! pool*)))
                   v))
             v (if (pos? (count permutation)) (cycles! v permutation) v)
             v (loop [k 0, v v]
                 (if (< k shrink)
                   (do (pool/free-buffer-index! pool* (nth v (dec (count v))))
                       (recur (inc k) (pop! v)))
                   v))
             v (persistent! v)]
         (reduce-kv (fn [_ idx row]
                      (pool/update-buffer-index! pool* (nth v idx) row)
                      nil)
                    nil change)
         (reset! !buffer-indexes v)
         (swap! !rows i/patch-vec diff)
         nil))
     :verify
     (fn [expected]
       (cond-> {:active-buffer-indexes (count (:active-buffer-indexes @pool*))
                :rows-n (count @!rows)}
         expected (assoc :rows-match? (= @!rows (vec expected))
                         :expected-n (count expected))))}))

;; ---------------------------------------------------------------------------
;; C1 :mount — as-built bridge under the real electric-dom consumer algorithm
;; ---------------------------------------------------------------------------

(defn make-mount [pool*]
  (let [{:keys [append-child replace-child insert-before
                remove-child nth-child]} (pool/gpu-mount pool*)
        mnt (mi/mount append-child replace-child insert-before
                      remove-child nth-child)
        !rows (atom [])]
    {:kind :mount
     :apply-diff!
     (fn [diff]
       (mnt nil diff)
       (swap! !rows i/patch-vec diff)
       nil)
     :verify
     (fn [expected]
       (cond-> {:active-buffer-indexes (count (:active-buffer-indexes @pool*))
                :rows-n (count @!rows)}
         expected (assoc :rows-match? (= @!rows (vec expected))
                         :expected-n (count expected))))}))

;; ---------------------------------------------------------------------------
;; C3 :recollect — fallback transport: whole collections, re-diffed client-side
;; ---------------------------------------------------------------------------

(defn make-recollect [pool*]
  {:kind :recollect
   :apply-coll!
   (fn [coll] (pool/keyed-diff-update-pool! pool* coll))
   :verify
   (fn [expected]
     (cond-> {:active-buffer-indexes (count (:active-buffer-indexes @pool*))}
       expected (assoc :expected-n (count expected))))})

(defn make-consumer [kind pool*]
  (case kind
    :direct (make-direct pool*)
    :mount (make-mount pool*)
    :recollect (make-recollect pool*)))
