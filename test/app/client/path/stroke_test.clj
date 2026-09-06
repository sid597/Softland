(ns app.client.path.stroke-test
  (:require [app.client.path.component :as component]
            [app.client.path.records :as fixtures]
            [app.client.path.source :as source]
            [app.client.path.stroke :as stroke]
            [app.client.path.value :as v]
            [clojure.test :refer [deftest is testing]]))

(defn- path-of [record]
  (:path (source/build (:path/source record) (:path/tool record))))

(defn- options [record scale]
  (component/stroke-options (get-in record [:path/paint :stroke]) (:path/tool record) scale))

(def z-crossing
  "Where the Z's first and third lines cross, and the parameter on each."
  (let [[a b _ c d] (map (fn [[x y]] [x y]) (concat fixtures/z-samples [nil]))
        a [26.0 28.0] b [102.0 100.0] c [28.0 100.0] d [102.0 28.0]
        r (v/sub b a) s (v/sub d c)
        den (v/cross r s)
        t (/ (v/cross (v/sub c a) s) den)
        u (/ (v/cross (v/sub c a) r) den)]
    {:point (v/add a (v/scale r t)) :t t :u u}))

(deftest the-z-skin-matches-the-bench
  (let [path (path-of fixtures/harness-z)
        result (stroke/envelope path (component/stroke-defaults (get-in fixtures/harness-z [:path/paint :stroke]))
                                (options fixtures/harness-z 1.0))
        st (v/stats (:path result))]
    (testing "3 pieces → 10 lines + 16 arc quads, one closed outline (HANDOVER.md, harness Z)"
      (is (= 3 (:pieces result)))
      (is (= 1 (:open result)))
      (is (= 16 (:arcs result)))
      (is (= {:subpaths 1 :lines 10 :quads 16} (select-keys st [:subpaths :lines :quads])))
      (is (every? :closed? (:subpaths (:path result)))))))

(deftest the-nib-leans-with-the-taper-and-the-ribbon-does-not
  (let [path {:subpaths [{:closed? false :start [0.0 0.0] :segments [(v/line [10.0 0.0])]
                          :knots [{:id 0 :width 2.0} {:id 1 :width 8.0}]}]}
        skin (fn [tip] (:path (stroke/envelope path (component/stroke-defaults {:cap :butt}) {:tip tip :tolerance 0.1})))
        first-side-point (fn [p] (:p (first (:segments (first (:subpaths p))))))]
    (is (not= (skin :nib) (skin :ribbon)) "the two tips differ in one line of the tracer")
    (testing "the ribbon's side point sits perpendicular to the centerline at the knot's radius"
      (is (= [10.0 4.0] (first-side-point (skin :ribbon)))))
    (testing "the nib's side point is the external tangent of the two end discs"
      (let [[x y] (first-side-point (skin :nib))
            c (/ (- 1.0 4.0) 10.0) sn (Math/sqrt (- 1.0 (* c c)))]
        (is (< (Math/abs (- x (+ 10.0 (* 4.0 c)))) 1e-9))
        (is (< (Math/abs (- y (* 4.0 sn))) 1e-9))))))

(deftest joins-and-caps-by-declaration
  (let [bend {:subpaths [{:closed? false :start [0.0 0.0] :segments [(v/line [20.0 0.0]) (v/line [20.0 20.0])]
                          :knots [{:id 0 :width 4.0} {:id 1 :width 4.0} {:id 2 :width 4.0}]}]}
        run (fn [stroke] (stroke/envelope bend (component/stroke-defaults stroke) {:tip :nib :tolerance 0.1 :knot-scale 1.0}))]
    (is (< (:arcs (run {:join :miter :cap :butt})) (:arcs (run {:join :round :cap :round}))))
    (is (zero? (:arcs (run {:join :bevel :cap :square}))))
    (testing "a miter past its limit becomes a bevel"
      (let [sharp {:subpaths [{:closed? false :start [0.0 0.0] :segments [(v/line [20.0 0.0]) (v/line [0.0 1.0])]
                               :knots [{:id 0 :width 4.0} {:id 1 :width 4.0} {:id 2 :width 4.0}]}]}
            count-lines (fn [limit] (:lines (v/stats (:path (stroke/envelope sharp (component/stroke-defaults {:join :miter :cap :butt :miter-limit limit}) {:tip :nib :tolerance 0.1})))))]
        (is (< (count-lines 1.0) (count-lines 100.0)))))))

(deftest a-closed-centerline-is-a-ring-and-alignment-picks-its-loops
  (let [path (path-of fixtures/border)
        run (fn [align] (stroke/envelope path (component/stroke-defaults {:join :miter :cap :butt :align align})
                                         {:tip :nib :tolerance 0.1 :width-local 2.0}))]
    (is (= 1 (:closed (run :center))))
    (is (= 2 (count (:subpaths (:path (run :center))))) "two offset loops")
    (testing "inside keeps the exact path as one loop"
      (let [inside (:path (run :inside))]
        (is (= 2 (count (:subpaths inside))))
        (is (some #(= [30.0 36.0] (:start %)) (:subpaths inside)) "the path itself, with its cubics")
        (is (pos? (:cubics (v/stats inside))))))
    (testing "outside reverses the exact path"
      (let [outside (:path (run :outside))]
        (is (= 2 (count (:subpaths outside))))
        (is (pos? (:cubics (v/stats outside))))))))

(deftest dashes-walk-by-arc-length
  (let [line {:subpaths [{:closed? false :start [0.0 0.0] :segments [(v/line [100.0 0.0])]
                          :knots [{:id 0 :width 2.0} {:id 1 :width 2.0}]}]}
        result (stroke/envelope line (component/stroke-defaults {:cap :butt}) {:tip :nib :tolerance 0.1 :dash [10.0 10.0]})]
    (is (= 5 (:open result)) "on 10, off 10 over 100 gives five dashes")
    (is (= 5 (count (:subpaths (:path result)))))))

(deftest the-crossing-as-dabs
  (let [path (path-of fixtures/z-as-dabs)
        result (stroke/dabs path (options fixtures/z-as-dabs 1.0))
        dabs (:dabs result)]
    (testing "24 dabs at spacing 12 along a centerline of length 281.94 (HANDOVER.md)"
      (is (= 24 (count dabs)))
      (is (< (Math/abs (- 281.94 (:length result))) 0.01))
      (is (= (mapv #(* 12.0 %) (range 24)) (mapv :at dabs))))
    (testing "each dab is a disc of radius 8 × its own pressure with source correspondence"
      (doseq [d dabs]
        (is (< (Math/abs (- (:r d) (* 8.0 (:p d)))) 1e-9))
        (is (<= 0 (:seg d) 2))
        (is (= 8 (count (:segments (first (:subpaths (:path d)))))) "HANDOVER.md: dabs of 8 arc quads")))
    (testing "two dabs cover the crossing"
      (let [{:keys [point]} z-crossing
            covering (filter (fn [d] (<= (v/dist [(:x d) (:y d)] point) (:r d))) dabs)]
        (is (= 2 (count covering)))))))

(deftest a-nonlinear-response-is-not-the-interpolation-of-widths
  (let [path (path-of fixtures/z-nonlinear)
        result (stroke/dabs path (options fixtures/z-nonlinear 1.0))
        dab (nth (:dabs result) 3)
        p (:p dab)
        interpolated-width (let [w0 (* 16.0 0.65 0.65) w1 (* 16.0 0.9 0.9)
                                 t (/ (- p 0.65) (- 0.9 0.65))]
                             (+ w0 (* (- w1 w0) t)))]
    (is (< (Math/abs (- (* 2.0 (:r dab)) (* 16.0 p p))) 1e-9) "the dab's width is 16 p² at its own pressure")
    (is (> (Math/abs (- (* 2.0 (:r dab)) interpolated-width)) 0.1)
        "interpolating the knot widths would give a different footprint")))
