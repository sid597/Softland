(ns app.client.path.records
  "The definer's records: the constructions the implementation is measured
   by, as data.

   Input: none. Output: authored records for path/construction, the
   same ones the bench ran (docs/below-the-waist/path-kind/bench-9/, with
   the measured numbers in its HANDOVER.md). The JVM tests and the browser
   harness read them from here so a record means one thing in both.

   Folder map: README.md.")

(def z-samples
  "The harness's translucent self-crossing Z: four samples with pressure."
  [[26.0 28.0 0.65 0.0] [102.0 100.0 0.9 90.0] [28.0 100.0 1.0 180.0] [102.0 28.0 0.7 270.0]])

(def harness-z
  "Width 16 × pressure, polyline, one union region painted once at 62 %."
  {:path/material-id :fixture/z
   :path/revision 1
   :path/tool {:name "harness ink" :size 16 :fit :polyline :streamline 0}
   :path/source {:kind :pen :samples z-samples}
   :path/paint {:stroke {:overlap :union :tip :nib :width [:* [:get :size] [:get :p]] :unit :local
                         :cap :round :join :round :color [0.84 0.36 0.94 0.62]}}})

(def z-as-dabs
  "The definer's attack 1: the same Z as 24 round dabs at spacing 12, radius
   8 × pressure; two dabs cover the crossing: 0.62 + 0.38 × 0.62 = 0.8556."
  (assoc-in harness-z [:path/paint :stroke :overlap] :accumulate))

(def z-nonlinear
  "The definer's nonlinear response: width 16 p², evaluated on the
   interpolated pressure, never interpolated between evaluated widths."
  (assoc-in harness-z [:path/paint :stroke :width] [:* [:get :size] [:pow [:get :p] 2.0]]))

(def pressure-ink
  "The harness's pressure-ink golden: four samples, pressure 0.2 to 1.0."
  {:path/material-id :fixture/pressure
   :path/revision 1
   :path/tool {:size 16 :fit :polyline :streamline 0}
   :path/source {:kind :pen :samples [[26.0 72.0 0.2] [48.0 36.0 0.45] [78.0 84.0 0.72] [102.0 42.0 1.0]]}
   :path/paint {:stroke {:width [:* [:get :size] [:get :p]] :color [0.16 0.68 0.96 0.94]}}})

(defn- ring
  [points]
  {:closed? true :anchors (mapv (fn [[x y]] {:p [x y]}) points)})

(def holed-concave
  "The harness's holed concave shape as anchors, both rings wound the same
   way, filled even-odd: the hole is a hole by parity, not by a role."
  {:path/material-id :fixture/holed
   :path/revision 1
   :path/source {:kind :anchors
                 :contours [(ring [[24.0 24.0] [104.0 24.0] [104.0 104.0] [72.0 104.0]
                                   [72.0 64.0] [56.0 64.0] [56.0 104.0] [24.0 104.0]])
                            (ring [[34.0 34.0] [50.0 34.0] [50.0 50.0] [34.0 50.0]])]}
   :path/paint {:fill {:rule :even-odd :color [0.94 0.32 0.18 0.96]}}})

(def border
  "A UI border: rect 24, 36, 80 × 52, r 6; fill; stroke one device pixel,
   miter, inside; snapped to the device grid."
  {:path/material-id :fixture/border
   :path/revision 1
   :path/source {:kind :rect :x 24.0 :y 36.0 :w 80.0 :h 52.0 :r 6.0}
   :path/snap? true
   :path/paint {:fill {:rule :nonzero :color [0.96 0.96 0.98 1.0]}
                :stroke {:width 1.0 :unit :device :join :miter :cap :butt :align :inside
                         :color [0.2 0.2 0.25 1.0]}}})

(def draw-tool
  "tldraw's draw tool: 70 samples along a limaçon with synthetic pressure and
   times; size 10, thinning 0.6, streamline 0.35, fit 0.9, taperEnd 22."
  {:path/material-id :fixture/draw
   :path/revision 1
   :path/tool {:name "draw" :size 10 :thinning 0.6 :streamline 0.35 :fit 0.9 :taper-end 22
               :width [:* [:get :size] [:- 1.0 [:* [:get :thinning] [:- 1.0 [:get :p]]]]]}
   :path/source {:kind :pen
                 :samples (vec (for [i (range 70)]
                                 (let [t (* i 0.09)
                                       r (* 40.0 (+ 1.0 (* 0.5 (Math/cos t))))]
                                   [(+ 64.0 (* r (Math/cos t))) (+ 64.0 (* r (Math/sin t)))
                                    (+ 0.4 (* 0.5 (Math/abs (Math/sin (* 2.0 t))))) (* i 16.0)])))}
   :path/paint {:stroke {:color [0.1 0.1 0.12 0.85]}}})

(def pen-tool
  "A designer's closed outline with handles, filled nonzero and stroked
   miter with butt caps, alignable inside or outside."
  {:path/material-id :fixture/pen
   :path/revision 1
   :path/source {:kind :anchors
                 :contours [{:closed? true
                             :anchors [{:id "a" :p [40.0 30.0] :out [70.0 20.0]}
                                       {:id "b" :p [100.0 50.0] :in [110.0 30.0] :out [90.0 70.0]}
                                       {:id "c" :p [70.0 100.0] :in [90.0 100.0]}
                                       {:id "d" :p [30.0 80.0] :out [20.0 60.0]}]}]}
   :path/paint {:fill {:rule :nonzero :color [0.25 0.55 0.9 0.5]}
                :stroke {:width 3.0 :join :miter :cap :butt :color [0.05 0.1 0.3 1.0]}}})
