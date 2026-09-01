(ns app.client.path.tessellation-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.path.material-test
             :refer [holed-shape ink-material]]
            [app.client.path.material :as path-material]
            [app.client.path.tessellation :as tessellation]))

(defn- float-bits [value]
  (Float/floatToIntBits (float value)))

(defn- mesh-bytes [mesh]
  (into []
        (mapcat (fn [[x y]] [(float-bits x) (float-bits y)]))
        (:vertices mesh)))

(defn- orientation [[ax ay] [bx by] [px py]]
  (- (* (- bx ax) (- py ay))
     (* (- by ay) (- px ax))))

(defn- point-in-triangle? [point a b c]
  (let [epsilon 1.0e-10]
    (every? #(>= % (- epsilon))
            [(orientation a b point)
             (orientation b c point)
             (orientation c a point)])))

(defn- point-in-mesh? [mesh point]
  (boolean
   (some (fn [[a b c]] (point-in-triangle? point a b c))
         (partition 3 (:vertices mesh)))))

(deftest deterministic-direct-stroke-and-invalidation-tripwire
  (let [left (ink-material :left/rev-1
                           [[0.0 0.0 0.2] [20.0 0.0 0.6]
                            [30.0 10.0 1.0]])
        right (assoc (ink-material :right/rev-1
                                   [[50.0 0.0 1.0] [70.0 0.0 1.0]])
                     :path/material-id :path/right)
        first-pass (tessellation/derive-mesh-set {} [left right] 1.0)
        replay (tessellation/derive-mesh-set (:cache first-pass)
                                             [left right] 1.0)
        edited-left (-> left
                        (assoc :path/revision :left/rev-2)
                        (assoc-in [:path/geometry :knots 1 :position]
                                  [21.0 1.0]))
        edit-pass (tessellation/derive-mesh-set (:cache replay)
                                                [edited-left right] 1.0)
        left-mesh (first (:meshes first-pass))
        right-mesh (second (:meshes first-pass))]
    (is (= (mesh-bytes left-mesh)
           (mesh-bytes
            (tessellation/tessellate left 1.0)))
        "identical material/version/regime emits byte-identical f32 mesh")
    (is (pos? (:triangle-count left-mesh)))
    (is (> (:triangle-count left-mesh) 4)
        "segment quads are supplemented by round cap/join fans")
    (is (empty? (:derived-keys replay)))
    (is (= 1 (count (:derived-keys edit-pass))))
    (is (identical? right-mesh (second (:meshes edit-pass)))
        "a point edit leaves its sibling mesh byte object untouched")
    (testing "version bump changes identity without silently changing bytes"
      (let [bumped (tessellation/tessellate left
                                            :path-tessellation-v2 1.0)
            revision-only (tessellation/tessellate
                           (assoc left :path/revision :left/rev-2) 1.0)]
        (is (not= (:cache-key left-mesh) (:cache-key bumped)))
        (is (= (:cache-key left-mesh) (:cache-key revision-only)))
        (is (= (mesh-bytes left-mesh)
               (mesh-bytes bumped)))))))

(deftest concave-hole-ear-clip-tripwire
  (let [material (holed-shape)
        mesh (tessellation/tessellate material 10.0)
        replay (tessellation/tessellate material 10.0)
        quantization (tessellation/quantization-receipt mesh 10.0)]
    (is (= :legal-max (:regime mesh)))
    (is (= (mesh-bytes mesh) (mesh-bytes replay)))
    (is (point-in-mesh? mesh [2.0 20.0])
        "concave outer interior is covered")
    (is (not (point-in-mesh? mesh [8.0 8.0]))
        "bridged explicit hole remains uncovered")
    (is (= :f32 (:coordinate-precision quantization)))
    (is (<= 0.0 (:max-screen-px-error quantization)))))

(deftest zoom-regime-and-geometry-dedupe-tripwire
  (let [material (ink-material)
        repainted (assoc-in material [:path/paint :color]
                            [0.9 0.1 0.2 0.8])
        derivation (tessellation/derive-mesh-set
                    {} [material repainted] 1.0)]
    (is (= [:legal-min :floor-default :floor-default :legal-max :legal-max]
           (mapv (comp :regime/id tessellation/zoom-regime)
                 [0.01 0.1 8.0 10.0 1000.0])))
    (is (= 1 (count (:cache derivation))))
    (is (= 1 (count (:derived-keys derivation))))
    (is (identical? (first (:meshes derivation))
                    (second (:meshes derivation)))
        "paint is not a tessellation input")))
