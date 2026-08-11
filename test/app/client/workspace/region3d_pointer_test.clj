(ns app.client.workspace.region3d-pointer-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.substrate.region3d-material :as material]
            [app.client.substrate.region3d-scene :as scene]
            [app.client.substrate.region3d-material-test :as fixture]
            [app.client.workspace.region3d-pointer :as pointer]))

(def base-view
  {:pivot [0.0 0.0 0.0]
   :distance 8.0
   :yaw 0.0
   :pitch 0.0
   :lens material/default-perspective-lens})

(defn- near?
  ([expected actual] (near? expected actual 1.0e-6))
  ([expected actual epsilon]
   (<= (Math/abs (- (double expected) (double actual))) epsilon)))

(defn- vec-near? [expected actual epsilon]
  (every? true? (map #(near? %1 %2 epsilon) expected actual)))

(defn- point-on-segment [[ax ay] [bx by] t]
  [(+ ax (* (- bx ax) t))
   (+ ay (* (- by ay) t))])

(defn- perpendicular-offset [[ax ay] [bx by] amount]
  (let [dx (- bx ax)
        dy (- by ay)
        length (Math/sqrt (+ (* dx dx) (* dy dy)))]
    [(* (/ (- dy) length) amount)
     (* (/ dx length) amount)]))

(defn- add2 [[ax ay] [bx by]] [(+ ax bx) (+ ay by)])

(deftest s1-typed-packet-and-anchored-dolly-preserve-the-original-anchor
  (doseq [scale [1.0 2.0 3.5]]
    (let [local-viewport [720.0 480.0]
          packet (pointer/pointer-packet
                  {:css-point [165.6 340.8]
                   :region-local [165.6 340.8]
                   :viewport-local local-viewport
                   :scale scale})
          cursor (:region-device packet)
          camera (scene/camera-matrices base-view (:viewport-device packet))
          ray (scene/ray-from-region-point camera cursor)
          normal (scene/normalize
                  (scene/v- (:eye camera) (:pivot base-view)))
          anchor (pointer/ray-plane-point ray (:pivot base-view) normal)]
      (is (= {:css-point [165.6 340.8]
              :region-local [165.6 340.8]
              :region-device (mapv #(* scale %) [165.6 340.8])
              :viewport-local local-viewport
              :viewport-device (mapv #(* scale %) local-viewport)
              :scale scale}
             packet))
      (loop [view base-view
             deltas [70.0 -32.0 115.0]]
        (when-let [delta (first deltas)]
          (let [next-view (pointer/anchored-dolly view anchor delta)
                projected (:screen
                           (scene/project-point
                            (scene/camera-matrices next-view
                                                   (:viewport-device packet))
                            anchor))]
            (is (vec-near? cursor projected 0.5)
                (str "scale " scale " preserves the original anchor"))
            (recur next-view (next deltas))))))))

(deftest s2-glued-pan-uses-the-start-camera-for-every-move
  (doseq [scale [1.0 2.75]]
    (let [viewport (mapv #(* scale %) [720.0 480.0])
          start-camera (scene/camera-matrices base-view viewport)
          g0 (mapv #(* scale %) [260.0 190.0])
          start-ray (scene/ray-from-region-point start-camera g0)
          normal (scene/normalize
                  (scene/v- (:eye start-camera) (:pivot base-view)))
          p0 (pointer/ray-plane-point start-ray (:pivot base-view) normal)
          pan (pointer/start-glued-pan base-view start-ray p0 normal)]
      (doseq [fraction [0.25 0.5 0.75 1.0]]
        (let [cursor (scene/v+ g0 [(* fraction 180.0)
                                   (* fraction -120.0)])
              current-ray (scene/ray-from-region-point start-camera cursor)
              moved-view (pointer/glued-pan pan current-ray)
              projected (:screen
                         (scene/project-point
                          (scene/camera-matrices moved-view viewport) p0))]
          (is (vec-near? cursor projected 0.5)
              (str "scale " scale " glues move " fraction)))))))

(deftest s3-device-paired-rays-deltas-and-projection-have-one-custody
  (let [packet (pointer/pointer-packet
                {:css-point [360.0 240.0]
                 :region-local [360.0 240.0]
                 :viewport-local [720.0 480.0]
                 :scale 2.0})
        camera (scene/camera-matrices base-view (:viewport-device packet))
        start-ray (scene/ray-from-region-point camera (:region-device packet))
        current-ray (scene/ray-from-region-point camera (:region-device packet))
        moved-device (update (:region-device packet) 0 + 120.0)
        moved-ray (scene/ray-from-region-point camera moved-device)
        delta (scene/translate-delta start-ray moved-ray
                                     (:pivot base-view) [0.0 0.0 1.0]
                                     [1.0 0.0 0.0])
        fov-radians (* (get-in base-view [:lens :fov-y-deg])
                       (/ Math/PI 180.0))
        expected-x (* 2.0 (:distance base-view)
                      (Math/tan (/ fov-radians 2.0))
                      (/ 120.0 (second (:viewport-device packet))))]
    (is (vec-near? (:origin start-ray) (:origin current-ray) 1.0e-6))
    (is (vec-near? (:direction start-ray) (:direction current-ray) 1.0e-6))
    (is (near? expected-x (first delta) 1.0e-3))
    (is (vec-near? [0.0 0.0] (subvec delta 1) 1.0e-3)))
  (doseq [scale [2.0 3.5]]
    (let [packet (pointer/pointer-packet
                  {:css-point [0.0 0.0]
                   :region-local [0.0 0.0]
                   :viewport-local [720.0 480.0]
                   :scale scale})
          [width height] (:viewport-device packet)
          camera (scene/camera-matrices base-view (:viewport-device packet))
          fov-radians (* (get-in base-view [:lens :fov-y-deg])
                         (/ Math/PI 180.0))
          expected [(+ (/ width 2.0)
                       (/ height
                          (* 2.0 (Math/tan (/ fov-radians 2.0))
                             (:distance base-view))))
                    (/ height 2.0)]
          projected (:screen (scene/project-point camera [1.0 0.0 0.0]))]
      (is (vec-near? expected projected 0.5)
          (str "desired-size projection owns scale " scale)))))

(deftest s4-painted-span-metric-boundaries-hover-and-glass-slop
  (let [region (fixture/region {:box (fixture/mesh-object :box nil
                                                           [0.0 0.0 0.0])})
        maintained (scene/derive-scene region)
        camera (scene/camera-matrices base-view [1440.0 960.0])
        handles (scene/gizmo-handles (:effective-transforms maintained)
                                     camera :box :translate 2.0)
        x-handle (first (filter #(= [:translate :x] (:handle/id %)) handles))
        [a b] (mapv (comp :screen #(scene/project-point camera %))
                    (:segment x-handle))
        old-dead-point (point-on-segment a b 0.15)
        old-sample-midpoint (point-on-segment a b 0.35)
        inside (add2 (point-on-segment a b 0.7)
                     (perpendicular-offset a b 19.6))
        outside (add2 (point-on-segment a b 0.7)
                      (perpendicular-offset a b 20.4))
        pick (fn [point]
               (scene/pick-region {:maintained maintained :camera camera
                                   :region-point point
                                   :gizmo-handles [x-handle]
                                   :dpr 2.0}))]
    (is (< 130.0 (scene/length (scene/v- b a)) 170.0))
    (is (= [:translate :x] (:handle-id (pick old-dead-point))))
    (is (= [:translate :x] (:handle-id (pick old-sample-midpoint))))
    (is (= [:translate :x] (:handle-id (pick inside))))
    (is (not= :gizmo (:route (pick outside))))
    (is (= 20.0 (:screen-radius-px x-handle)))
    (is (= [:translate :x]
           (pointer/gizmo-hover-id {:route :gizmo
                                    :handle-id [:translate :x]})))
    (is (nil? (pointer/gizmo-hover-id {:route :region-background}))))
  (testing "polyline distance covers the spaces between ring samples"
    (is (near? 4.0
               (pointer/point-polyline-distance2
                [5.0 2.0] [[0.0 0.0] [10.0 0.0] [10.0 10.0]])))))

(deftest s5-one-wheel-court-and-standing-dual-road-counter
  (is (= :region3d (pointer/wheel-route true true)))
  (is (= :ground (pointer/wheel-route false true)))
  (is (= :scroll (pointer/wheel-route false false)))
  (let [receipt (-> (pointer/empty-wheel-receipt)
                    (pointer/record-wheel-roads [:region3d])
                    (pointer/record-wheel-roads [:ground]))]
    (is (= {:samples 2 :camera-roads 2 :last-camera-roads 1
            :dual-dispatches 0}
           receipt)))
  (is (= 1 (:dual-dispatches
            (pointer/record-wheel-roads (pointer/empty-wheel-receipt)
                                        [:region3d :ground])))))
