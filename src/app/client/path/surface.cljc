(ns app.client.path.surface
  "Path coverage bound into the shared CPU compositor.
   Takes path regions, plane surfaces and explicit paint; gives a new surface.
   Holds nothing. Coverage is lowered in texel coordinates, so quarter-texel
   tolerance also works for rotated/sheared affine domains. Evidence:
   pickup_test.clj and surface_test.clj."
  (:require [app.client.engine.surface :as surface]
            [app.client.path.pack :as pack]
            [app.client.path.value :as value]))

(defn- coverage [surface region]
  (let [path (or (:path region) region)
        rule (or (:rule region) :nonzero)
        packed (:pack (pack/pack-region (value/map-points path #(surface/to-texel surface %)) 0.25 {}))]
    {:bounds (when packed (mapv + (:bbox packed) [-1 -1 1 1]))
     :coverage (if packed (fn [x y] (pack/coverage-at packed (+ x 0.5) (+ y 0.5) 1.0 1.0 rule))
                   (fn [_ _] 0.0))}))

(defn paint
  "Named paint arguments and executor position → CPU surface value. The
   wrapper supplies path coverage and the step-derived key to the engine."
  [{:keys [surface region rgba opacity blend clips]} {:keys [at step]}]
  (let [opts (coverage surface region)]
    (surface/paint surface region {:kind :color :rgba rgba :opacity opacity :blend blend}
                   (assoc opts :key (str (:surface/id surface) ":" at "/" (name step))
                          :clips (mapv #(:coverage (coverage surface %)) clips)))))
