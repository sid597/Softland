(ns app.client.path.fixtures)

(def example-ink-material
  {:path/material-id :path/ink-fixture
   :path/revision :ink/rev-1
   :path/kind :ink
   :path/geometry
   {:knots [{:knot/id [:knot 0] :position [0.0 0.0]
             :width 2.0 :pressure 0.2}
            {:knot/id [:knot 1] :position [20.0 0.0]
             :width 6.0 :pressure 0.6}
            {:knot/id [:knot 2] :position [30.0 10.0]
             :width 10.0 :pressure 1.0}]
    :cap :round
    :join :round}
   :path/paint {:color [0.2 0.6 0.9 0.8]
                :opacity 0.75
                :color-space :srgb
                :alpha-association :straight}})

(def example-shape-material
  {:path/material-id :path/holed-concave
   :path/revision :shape/rev-1
   :path/kind :shape
   :path/geometry
   {:contours
    [{:contour/id :outer :role :outer
      :points [[0.0 0.0] [40.0 0.0] [40.0 40.0]
               [24.0 40.0] [24.0 16.0] [16.0 16.0]
               [16.0 40.0] [0.0 40.0]]}
     {:contour/id :hole :role :hole
      :points [[4.0 4.0] [13.0 4.0] [13.0 13.0] [4.0 13.0]]}]}
   :path/paint {:color [0.9 0.3 0.2 1.0]
                :opacity 1.0
                :color-space :srgb
                :alpha-association :straight}})

(def tapered-segment
  {:path/material-id :path/tapered-segment
   :path/revision :tapered/rev-1
   :path/kind :ink
   :path/geometry
   {:knots [{:knot/id [:tapered 0] :position [0.0 0.0] :width 4.0}
            {:knot/id [:tapered 1] :position [10.0 0.0] :width 12.0}]
    :cap :round
    :join :round}
   :path/paint {:color [0.25 0.5 0.75 1.0]
                :opacity 1.0
                :color-space :srgb
                :alpha-association :straight}})
