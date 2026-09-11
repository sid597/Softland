(ns softland.inland.geometry
  "Pure instrument geometry over Softland path construction.
   Takes primitive coordinates and paint; gives constructed path materials for the
   existing path renderer. Owns no runtime state. Colors here are presentation
   constants; coverage, tessellation and GPU retention belong to the shared client.
   Geometry construction may reject invalid input; paint translates those errors."
  (:require [app.client.path.construction :as construction]))

(def ink [0.13 0.12 0.19 1])
(def silver [0.94 0.94 0.98 1])
(def muted [0.65 0.63 0.73 1])
(def blue [0.57 0.61 1.0 1])
(def coral [1.0 0.62 0.52 1])
(def line-color [0.7 0.67 0.82 0.2])

(defn material
  "Identity, source, fill, stroke and width → constructed revision-zero path.
   Optional paints omit their entries; strokes use local units and round caps/joins."
  [id source fill stroke width]
  (construction/construct
    {:path/material-id id :path/revision 0 :path/source source
     :path/paint (cond-> {}
                   fill (assoc :fill {:rule :nonzero :color fill})
                   stroke (assoc :stroke {:width (or width 1) :unit :local
                                          :color stroke :cap :round :join :round}))}))

(defn rect
  "Identity, [x y width height], radius and paints → rounded rectangle material.
   Uses the shared constructor and a one-unit stroke when supplied."
  [id [x y w h] radius fill stroke]
  (material id {:kind :rect :x x :y y :w w :h h :r radius} fill stroke 1))

(defn line
  "Identity, point sequence, color and width → open stroked anchor contour."
  [id points color width]
  (material id {:kind :anchors :contours [{:closed? false :anchors (mapv #(hash-map :p %) points)}]}
            nil color width))

(defn ellipse
  "Identity, [center-x center-y radius-x radius-y], radians and paints → path.
   Four cubic Bezier segments approximate the ellipse; control points receive the
   same scale/rotation/translation as endpoints. This is not an exact conic."
  [id [cx cy rx ry] angle fill stroke width]
  (let [k 0.5522847498 c #?(:clj (Math/cos angle) :cljs (js/Math.cos angle))
        s #?(:clj (Math/sin angle) :cljs (js/Math.sin angle))
        point (fn [[x y]] [(+ cx (- (* rx x c) (* ry y s))) (+ cy (* rx x s) (* ry y c))])]
    (material id
      {:kind :anchors
       :contours [{:closed? true
                   :anchors (mapv (fn [[p i o]] {:p (point p) :in (point i) :out (point o)})
                              [[[1 0] [1 (- k)] [1 k]] [[0 1] [k 1] [(- k) 1]]
                               [[-1 0] [-1 k] [-1 (- k)]] [[0 -1] [(- k) -1] [k -1]]])}]}
      fill stroke width)))

