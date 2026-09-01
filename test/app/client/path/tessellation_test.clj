(ns app.client.path.tessellation-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.path.fixtures :as fixtures]
            [app.client.path.material :as material]
            [app.client.path.tessellation :as tessellation])
  (:import [java.security MessageDigest]))

(defn- float-bits [value]
  (Float/floatToIntBits (float value)))

(defn- mesh-bytes [mesh]
  (into []
        (mapcat (fn [[x y]] [(float-bits x) (float-bits y)]))
        (:vertices mesh)))

(defn- mesh-fingerprint [mesh]
  (format "%064x"
          (BigInteger. 1
                       (.digest (MessageDigest/getInstance "SHA-256")
                                (.getBytes (pr-str (mesh-bytes mesh))
                                           "UTF-8")))))

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

(defn- ink-material [revision samples]
  (material/validate-material!
   (-> fixtures/example-ink-material
       (assoc :path/revision revision)
       (assoc-in [:path/geometry :knots]
                 (mapv (fn [index [x y pressure]]
                         {:knot/id [:knot index]
                          :position [x y]
                          :width (* 10.0 pressure)
                          :pressure pressure})
                       (range) samples)))))

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
           (mesh-bytes (tessellation/tessellate left 1.0))))
    (is (pos? (:triangle-count left-mesh)))
    (is (> (:triangle-count left-mesh) 4))
    (is (empty? (:derived-keys replay)))
    (is (= 1 (count (:derived-keys edit-pass))))
    (is (identical? right-mesh (second (:meshes edit-pass))))
    (testing "algorithm version changes identity without changing bytes"
      (let [bumped (tessellation/tessellate left
                                            :path-tessellation-v2 1.0)
            revision-only
            (tessellation/tessellate
             (assoc left :path/revision :left/rev-2) 1.0)]
        (is (not= (:cache-key left-mesh) (:cache-key bumped)))
        (is (= (:cache-key left-mesh) (:cache-key revision-only)))
        (is (= (mesh-bytes left-mesh) (mesh-bytes bumped)))))))

(deftest concave-hole-ear-clip-tripwire
  (let [shape (material/validate-material! fixtures/example-shape-material)
        mesh (tessellation/tessellate shape 10.0)
        replay (tessellation/tessellate shape 10.0)]
    (is (= :legal-max (:regime mesh)))
    (is (= (mesh-bytes mesh) (mesh-bytes replay)))
    (is (point-in-mesh? mesh [2.0 20.0]))
    (is (not (point-in-mesh? mesh [8.0 8.0])))))

(deftest zoom-regime-and-geometry-dedupe-tripwire
  (let [ink (material/validate-material! fixtures/example-ink-material)
        repainted (assoc-in ink [:path/paint :color] [0.9 0.1 0.2 0.8])
        derivation (tessellation/derive-mesh-set {} [ink repainted] 1.0)]
    (is (= [:legal-min :floor-default :floor-default :legal-max :legal-max]
           (mapv (comp :regime/id tessellation/zoom-regime)
                 [0.01 0.1 8.0 10.0 1000.0])))
    (is (= 1 (count (:cache derivation))))
    (is (= 1 (count (:derived-keys derivation))))
    (is (identical? (first (:meshes derivation))
                    (second (:meshes derivation))))))

(deftest width-move-preserves-pinned-mesh-bytes-tripwire
  (let [ink (material/validate-material! fixtures/example-ink-material)
        shape (material/validate-material! fixtures/example-shape-material)]
    (is (= "730fce62a455428e0dc98297c57ec0939969c8740c7cbe3fae52d38ba123340b"
           (mesh-fingerprint (tessellation/tessellate ink 10.0))))
    (is (= "f9f0c49ce9054a46d2099fb01247afc1c8dacd9bc7a94f55ab2433816e6f8270"
           (mesh-fingerprint (tessellation/tessellate shape 10.0))))))
