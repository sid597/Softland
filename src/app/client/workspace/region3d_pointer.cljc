(ns app.client.workspace.region3d-pointer
  "Pure coordinate, navigation, handle-metric, and wheel-court math for
   Region3D pointer interaction. Every screen computation consumes a point and
   viewport from one named representation; DOM/session ownership stays at the
   workspace edge."
  (:require [app.client.substrate.region3d-material :as material]))

(def ^:private epsilon 1.0e-7)

(defn- v+
  ([left right] (mapv + left right))
  ([left middle right] (mapv + left middle right)))

(defn- v- [left right] (mapv - left right))
(defn- v* [vector scalar] (mapv #(* % scalar) vector))
(defn- dot [left right] (reduce + (map * left right)))

(defn pointer-packet
  "Build the one typed packet from canvas CSS, region-local, and scale facts.
   The device point and viewport are derived together and therefore cannot be
   cross-paired by a caller."
  [{:keys [css-point region-local viewport-local scale]}]
  (let [scale (double scale)]
    {:css-point (mapv double css-point)
     :region-local (mapv double region-local)
     :region-device (mapv #(* scale (double %)) region-local)
     :viewport-local (mapv double viewport-local)
     :viewport-device (mapv #(* scale (double %)) viewport-local)
     :scale scale}))

(defn ray-plane-point
  "Intersect a positive ray with a plane. Parallel and behind-camera rays
   fail closed."
  [{:keys [origin direction]} plane-point plane-normal]
  (let [denominator (dot direction plane-normal)]
    (when (> (Math/abs (double denominator)) epsilon)
      (let [distance (/ (dot (v- plane-point origin) plane-normal)
                        denominator)]
        (when (pos? distance)
          (v+ origin (v* direction distance)))))))

(defn anchored-dolly
  "Similarity-scale a camera rig about one already-resolved world anchor.
   A nil anchor preserves the prior center-dolly behavior."
  [view anchor wheel-delta]
  (let [view (material/canonical-view view)
        scale (Math/exp (* (double wheel-delta) 0.001))]
    (cond-> (update view :distance * scale)
      anchor (assoc :pivot (v+ anchor
                               (v* (v- (:pivot view) anchor) scale))))))

(defn start-glued-pan
  "Capture the fixed plane and start view for a glued pan. `fallback-point`
   is a surface hit when available; otherwise the start ray meets the supplied
   pivot plane."
  [view start-ray fallback-point plane-normal]
  (let [view (material/canonical-view view)
        anchor (or fallback-point
                   (ray-plane-point start-ray (:pivot view) plane-normal))]
    (when anchor
      {:view view :anchor anchor :plane-normal plane-normal})))

(defn glued-pan
  "Translate the start rig so the captured world point projects at the current
   cursor ray. Every move is evaluated against the start camera/plane."
  [{:keys [view anchor plane-normal]} current-ray]
  (when-let [current (ray-plane-point current-ray anchor plane-normal)]
    (update view :pivot v+ (v- anchor current))))

(defn point-segment-distance2
  "Squared 2D distance from a point to the complete closed segment."
  [point start end]
  (let [span (v- end start)
        length2 (dot span span)
        amount (if (< length2 epsilon)
                 0.0
                 (-> (/ (dot (v- point start) span) length2)
                     (max 0.0)
                     (min 1.0)))
        nearest (v+ start (v* span amount))
        delta (v- point nearest)]
    (dot delta delta)))

(defn point-polyline-distance2
  "Squared 2D distance to every consecutive segment of a polyline."
  [point points]
  (if-let [segments (seq (partition 2 1 points))]
    (reduce min (map (fn [[start end]]
                       (point-segment-distance2 point start end))
                     segments))
    ##Inf))

(defn slop-device
  "Convert one glass-declared CSS radius to device pixels exactly once."
  [slop-css dpr]
  (* (double slop-css) (double dpr)))

(defn gizmo-hover-id [hit]
  (when (= :gizmo (:route hit))
    (:handle-id hit)))

(defn wheel-route
  "The camera court's pure decision. Region custody is evaluated first; only
   an unconsumed sample may reach the ambient ground camera."
  [region-consumed? ground-active?]
  (cond region-consumed? :region3d
        ground-active? :ground
        :else :scroll))

(defn empty-wheel-receipt []
  {:samples 0 :camera-roads 0 :last-camera-roads 0 :dual-dispatches 0})

(defn record-wheel-roads
  "Standing dev receipt: record all camera roads mutated by one input sample."
  [receipt roads]
  (let [camera-roads (count (filter #{:region3d :ground} roads))]
    (-> receipt
        (update :samples inc)
        (update :camera-roads + camera-roads)
        (assoc :last-camera-roads camera-roads)
        (update :dual-dispatches + (if (> camera-roads 1) 1 0)))))
