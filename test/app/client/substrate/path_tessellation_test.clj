(ns app.client.substrate.path-tessellation-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.substrate.path-material-test
             :refer [holed-shape ink-material]]
            [app.client.substrate.path-material :as path-material]
            [app.client.substrate.path-tessellation :as tessellation]))

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
        edited-left (path-material/move-knot
                     left [:knot 1] [21.0 1.0] :left/rev-2)
        edit-pass (tessellation/derive-mesh-set (:cache replay)
                                                [edited-left right] 1.0)
        left-mesh (first (:meshes first-pass))
        right-mesh (second (:meshes first-pass))]
    (is (= (tessellation/mesh-bytes left-mesh)
           (tessellation/mesh-bytes
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
                                            :path-tessellation-v2 1.0)]
        (is (not= (:cache-key left-mesh) (:cache-key bumped)))
        (is (= (tessellation/mesh-bytes left-mesh)
               (tessellation/mesh-bytes bumped)))))))

(deftest concave-hole-ear-clip-and-open-line-tripwire
  (let [material (holed-shape)
        mesh (tessellation/tessellate material 10.0)
        replay (tessellation/tessellate material 10.0)
        quantization (tessellation/quantization-receipt mesh 10.0)]
    (is (= :legal-max (:regime mesh)))
    (is (= (tessellation/mesh-bytes mesh)
           (tessellation/mesh-bytes replay)))
    (is (tessellation/point-in-mesh? mesh [2.0 20.0])
        "concave outer interior is covered")
    (is (not (tessellation/point-in-mesh? mesh [8.0 8.0]))
        "bridged explicit hole remains uncovered")
    (is (tessellation/point-in-mesh? mesh [31.0 25.0])
        "the shape's open polyline uses the same direct stroke road")
    (is (= :f32 (:coordinate-precision quantization)))
    (is (<= 0.0 (:max-screen-px-error quantization)))))
