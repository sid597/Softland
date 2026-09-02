(ns app.client.region3d.oracle
  "Test/verifier oracles for retained Region3D production derivations."
  (:require [app.client.engine.color :as color]
            [app.client.region3d.scene :as scene]))

(defn- bvh-triangle-receipt [bvh]
  (letfn [(walk [node]
            (case (:kind node)
              :leaf (:triangles node)
              :branch (concat (walk (:left node)) (walk (:right node)))
              []))]
    (->> (walk bvh)
         (sort-by (juxt (comp pr-str :object-id) :triangle-index))
         vec)))

(defn scene-equivalent? [maintained]
  (let [oracle (scene/derive-scene (:region maintained))]
    (and (= (select-keys maintained
                         [:effective-transforms :instances
                          :triangles-by-object])
            (select-keys oracle
                         [:effective-transforms :instances
                          :triangles-by-object]))
         (= (get-in maintained [:bvh :bounds])
            (get-in oracle [:bvh :bounds]))
         (= (bvh-triangle-receipt (:bvh maintained))
            (bvh-triangle-receipt (:bvh oracle))))))

(defn- rgba-linear [tagged]
  (let [[r g b a] (:rgba tagged)]
    [(color/srgb-channel->linear r)
     (color/srgb-channel->linear g)
     (color/srgb-channel->linear b)
     a]))

(defn- fresnel-schlick [f0 view-dot-half]
  (mapv (fn [base]
          (+ base (* (- 1.0 base)
                     (Math/pow (- 1.0 view-dot-half) 5.0))))
        f0))

(defn- pbr-brdf
  [{:keys [base-color metallic roughness normal view light radiance]}]
  (let [[r g b _] (rgba-linear base-color)
        base [r g b]
        n (scene/normalize normal)
        v (scene/normalize view)
        l (scene/normalize light)
        h (scene/normalize (scene/v+ v l))
        ndotl (max 0.0 (scene/dot n l))
        ndotv (max scene/ray-epsilon (scene/dot n v))
        ndoth (max 0.0 (scene/dot n h))
        vdoth (max 0.0 (scene/dot v h))
        alpha (max 0.0025 (* roughness roughness))
        alpha2 (* alpha alpha)
        denominator (+ (* ndoth ndoth (- alpha2 1.0)) 1.0)
        distribution (/ alpha2 (* Math/PI denominator denominator))
        visibility-denominator
        (+ (* ndotl (Math/sqrt (+ (* ndotv ndotv (- 1.0 alpha2)) alpha2)))
           (* ndotv (Math/sqrt (+ (* ndotl ndotl (- 1.0 alpha2)) alpha2))))
        visibility (if (pos? visibility-denominator)
                     (/ 0.5 visibility-denominator) 0.0)
        f0 (mapv #(scene/mix 0.04 % metallic) base)
        fresnel (fresnel-schlick f0 vdoth)
        specular (mapv #(* distribution visibility %) fresnel)
        diffuse (mapv (fn [channel f]
                        (* (/ channel Math/PI) (- 1.0 metallic) (- 1.0 f)))
                      base fresnel)]
    (mapv (fn [d s incoming]
            (* (+ d s) incoming ndotl))
          diffuse specular radiance)))

(defn- punctual-radiance
  [{:keys [kind intensity range cone color]} light-position point light-dir]
  (let [[r g b _] (rgba-linear color)
        distance (scene/length (scene/v- light-position point))
        attenuation
        (case kind
          :directional 1.0
          (let [inverse-square (/ 1.0 (max scene/ray-epsilon
                                           (* distance distance)))
                cutoff (Math/pow
                        (scene/clamp 0.0
                                     (- 1.0 (Math/pow (/ distance range) 4.0))
                                     1.0)
                        2.0)]
            (* inverse-square cutoff)))
        spot
        (if (= :spot kind)
          (let [cos-angle (scene/dot
                           (scene/normalize (scene/v- point light-position))
                           (scene/normalize light-dir))
                inner (Math/cos (* (:inner-deg cone) (/ Math/PI 180.0)))
                outer (Math/cos (* (:outer-deg cone) (/ Math/PI 180.0)))]
            (scene/clamp 0.0 (/ (- cos-angle outer)
                                (max scene/ray-epsilon (- inner outer))) 1.0))
          1.0)]
    (mapv #(* % intensity attenuation spot) [r g b])))

(defn- khronos-neutral-tone-map [color]
  (let [start-compression 0.76
        desaturation 0.15
        color (mapv (fn [channel]
                      (let [x (min channel 0.08)
                            offset (- x (* 6.25 x x))]
                        (- channel offset)))
                    color)
        peak (apply max color)]
    (if (< peak start-compression)
      color
      (let [distance (- 1.0 start-compression)
            new-peak (- 1.0
                        (/ (* distance distance)
                           (+ peak distance (- start-compression))))
            color (mapv #(* % (/ new-peak peak)) color)
            amount (- 1.0
                      (/ 1.0 (+ (* desaturation (- peak new-peak)) 1.0)))]
        (mapv #(scene/mix % new-peak amount) color)))))

(defn- shadowed?
  [bvh point normal direction max-distance source-object-id]
  (let [origin (scene/v+ point (scene/v* normal 1.0e-4))
        hit (scene/query-bvh
             bvh {:origin origin :direction (scene/normalize direction)})]
    (boolean (and hit
                  (not= source-object-id (:object-id hit))
                  (< (:t hit) (or max-distance ##Inf))))))

(defn shade-reference
  "CPU shading oracle for the production Region3D GPU verifier."
  [{:keys [material normal point eye lights ambient bvh object-id]
    :or {lights []}}]
  (let [[br bg bb alpha] (rgba-linear (:base-color material))
        base [br bg bb]
        view (scene/v- eye point)
        direct
        (reduce
         (fn [sum {:keys [light position direction]}]
           (let [to-light (if (= :directional (:kind light))
                            (scene/v* (scene/normalize direction) -1.0)
                            (scene/v- position point))
                 distance (when-not (= :directional (:kind light))
                            (scene/length to-light))
                 shadow? (and (:cast-shadow light)
                              (shadowed? bvh point normal to-light distance
                                         object-id))
                 radiance (if shadow?
                            [0.0 0.0 0.0]
                            (punctual-radiance light position point direction))]
             (scene/v+ sum
                       (pbr-brdf {:base-color (:base-color material)
                                  :metallic (:metallic material)
                                  :roughness (:roughness material)
                                  :normal normal :view view :light to-light
                                  :radiance radiance}))))
         [0.0 0.0 0.0] lights)
        [ar ag ab _] (rgba-linear (:color ambient))
        ambient-term (scene/hadamard base
                                     (scene/v* [ar ag ab] (:intensity ambient)))
        [er eg eb _] (rgba-linear (:emissive material))
        linear (khronos-neutral-tone-map
                (scene/v+ direct ambient-term [er eg eb]))]
    (conj (mapv #(* % alpha) linear) alpha)))
