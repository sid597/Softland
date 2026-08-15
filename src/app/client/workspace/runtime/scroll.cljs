(ns app.client.workspace.runtime.scroll
  "Wheel consumer: the wheel court. Region3D claims custody before the
   ground (innermost-first — the wheel court's standing receipt counter is
   the dispatch record's one durable court receipt). The dev workspace's
   sidebar/agent/flow/chat/editor wheel rails died with that surface
   (dead-path census, Sid's ruling 2026-08-15)."
  (:require [missionary.core :as m]
            [app.client.workspace.ground :as ground]
            [app.client.workspace.region3d-pointer :as region3d-pointer]
            [app.client.workspace.region3d-runtime :as region3d]))

(defonce ^:private !wheel-court-receipt
  (atom (region3d-pointer/empty-wheel-receipt)))

(defn wheel-court-receipt [] @!wheel-court-receipt)

(defn- record-wheel-road! [route]
  (let [roads (if (contains? #{:region3d :ground} route) [route] [])
        receipt (swap! !wheel-court-receipt
                       region3d-pointer/record-wheel-roads roads)]
    (set! (.-__softlandRegion3dWheelCourt js/window) (clj->js receipt))))

(defn- route-camera-wheel! [wheel-evt]
  (let [region-consumed? (boolean (region3d/wheel! wheel-evt))
        route (region3d-pointer/wheel-route
               region-consumed?
               (and (not region-consumed?) (ground/ground-active?)))]
    (when (= :ground route) (ground/handle-wheel! wheel-evt))
    (record-wheel-road! route)
    (contains? #{:region3d :ground} route)))

(defn scroll-consumer
  "Missionary consumer: every wheel sample goes through the court."
  [_atoms >wheel-events]
  (->> >wheel-events
       (m/reduce
         (fn [_ wheel-evt]
           (route-camera-wheel! wheel-evt)
           nil)
         nil)))
