(ns app.client.path.pack-test
  (:require [app.client.path.component :as component]
            [app.client.path.fixtures :as fixtures]
            [app.client.path.pack :as pack]
            [app.client.path.source :as source]
            [app.client.path.value :as v]
            [clojure.test :refer [deftest is testing]]))

(defn- region [record kind]
  (first (filter #(= kind (:kind %)) (:regions (component/run record {})))))

(deftest tolerance-buckets-are-powers-of-two
  (is (= 0 (pack/scale-bucket 1.0)))
  (is (= 0 (pack/scale-bucket 1.9)))
  (is (= 1 (pack/scale-bucket 2.0)))
  (is (= 3 (pack/scale-bucket 10.0)))
  (is (= -4 (pack/scale-bucket 0.1)))
  (is (= (/ 0.25 16.0) (pack/bucket-tolerance 3)) "a quarter device pixel at the bucket's top")
  (is (= 0.125 (pack/bucket-margin 3)) "one device pixel at the bucket's coarsest scale"))

(deftest lowering-respects-the-tolerance
  (let [a [0.0 0.0] c1 [30.0 100.0] c2 [70.0 -100.0] b [100.0 0.0]
        error (fn [tol]
                (let [quads (pack/cubic->quads a c1 c2 b tol)]
                  (reduce max 0.0
                          (for [[i [x1 y1 cx cy x3 y3]] (map-indexed vector quads)
                                j (range 1 10)
                                :let [t (/ j 10.0)
                                      on-quad (v/quad-at [x1 y1] [cx cy] [x3 y3] t)
                                      tt (/ (+ i t) (count quads))
                                      on-cubic (v/cubic-at a c1 c2 b tt)]]
                            (v/dist on-quad on-cubic)))))]
    (is (> (count (pack/cubic->quads a c1 c2 b 0.01)) (count (pack/cubic->quads a c1 c2 b 1.0))))
    (is (< (error 0.05) (error 5.0)))
    (is (< (error 0.05) 0.5)))
  (let [{:keys [quads cubics from-cubics]} (pack/lower (:path (source/build (:path/source fixtures/border) nil)) 0.1)]
    (is (= 4 cubics))
    (is (= (+ 4 from-cubics) (count quads)) "four lines become one quad each")))

(deftest bands-are-sorted-for-the-early-exit
  (let [p (:pack (pack/pack-region (:path (region fixtures/harness-z :stroke)) 0.025 {}))]
    (is (= 26 (:count p)) "HANDOVER.md: 26 curves")
    (is (= 4 (:h-bands p)))
    (is (= 4 (:v-bands p)))
    (doseq [l (:h-lists p)]
      (is (= l (vec (sort-by (fn [i] (- (nth (nth (:boxes p) i) 2))) l))) "row lists by max x descending"))
    (doseq [l (:v-lists p)]
      (is (= l (vec (sort-by (fn [i] (- (nth (nth (:boxes p) i) 3))) l))) "column lists by max y descending"))
    (is (= (:band-texels p) (+ 8 (reduce + (map count (:h-lists p))) (reduce + (map count (:v-lists p))))))))

(deftest the-cpu-twin-answers-membership-and-coverage
  (let [p (:pack (pack/pack-region (:path (region fixtures/harness-z :stroke)) 0.025 {}))]
    (testing "inside the crossing the skin overlaps itself: winding 2, still one region"
      (is (= 2 (pack/winding-at p 65.0 64.0)))
      (is (pack/inside? p :nonzero 65.0 64.0))
      (is (not (pack/inside? p :even-odd 65.0 64.0)) "even-odd would punch the fold out")
      (is (= 1.0 (pack/coverage-at p 65.0 64.0 10.0 10.0 :nonzero))))
    (testing "outside is zero"
      (is (= 0 (pack/winding-at p 5.0 5.0)))
      (is (= 0.0 (pack/coverage-at p 5.0 5.0 10.0 10.0 :nonzero))))
    (testing "on an edge, coverage is about a half"
      (let [d (pack/outline-distance p [64.0 40.0])
            ;; walk down from a point above the stroke until we sit on its edge
            y (loop [y 30.0] (if (< (pack/outline-distance p [64.0 y]) 0.005) y (recur (+ y 0.001))))
            c (pack/coverage-at p 64.0 y 10.0 10.0 :nonzero)]
        (is (pos? d))
        (is (< 0.4 c 0.6))))))

(deftest even-odd-and-nonzero-on-the-holed-shape
  (let [f (region fixtures/holed-concave :fill)
        p (:pack (pack/pack-region (:path f) 0.01 {}))]
    (is (= :even-odd (:rule f)))
    (testing "both rings wound the same way: the hole is a hole only by parity"
      (is (= 2 (Math/abs (pack/winding-at p 42.0 42.0))))
      (is (not (pack/inside? p :even-odd 42.0 42.0)))
      (is (pack/inside? p :nonzero 42.0 42.0))
      (is (pack/inside? p :even-odd 90.0 90.0))
      (is (not (pack/inside? p :even-odd 64.0 90.0)) "the notch")
      (is (= 0.0 (pack/coverage-at p 42.0 42.0 1.0 1.0 :even-odd)))
      (is (= 1.0 (pack/coverage-at p 42.0 42.0 1.0 1.0 :nonzero)))
      (is (= 1.0 (pack/coverage-at p 90.0 90.0 1.0 1.0 :even-odd))))))

(deftest cover-cells-drop-the-empty-middle-of-a-thin-diagonal
  (let [line {:subpaths [{:closed? false :start [0.0 0.0] :segments [(v/line [200.0 200.0])]
                          :knots [{:id 0 :width 2.0} {:id 1 :width 2.0}]}]}
        skin (first ((:path/envelope component/capabilities) {:path line :stroke {} :tool {}}))
        p (:pack (pack/pack-region (:path skin) 0.1 {}))
        box (pack/cover p {:mode :box :margin 1.0})
        cells (pack/cover p {:mode :cells :margin 1.0 :cell 25.0 :rule :nonzero})]
    (is (= 1 (count (:rects box))))
    (is (> (count (:rects cells)) 1))
    (is (< (:area cells) (* 0.5 (:area box))))
    (testing "an interior cell no curve touches is kept by winding"
      (let [fill (first ((:path/fill-region component/capabilities)
                         {:path {:subpaths [{:closed? true :start [0.0 0.0]
                                             :segments [(v/line [100.0 0.0]) (v/line [100.0 100.0]) (v/line [0.0 100.0])]}]}
                          :rule :nonzero}))
            fp (:pack (pack/pack-region (:path fill) 0.1 {}))
            c (pack/cover fp {:mode :cells :margin 0.0 :cell 20.0 :rule :nonzero})]
        (is (= 25 (count (:rects c))) "every cell of a filled square, including the nine no edge touches")))))

(deftest snapping-moves-knots-to-the-device-grid-and-carries-handles
  (let [path (:path (source/build (:path/source fixtures/border) nil))
        scale 3.0 pan [0.5 0.25]
        snapped (pack/snap-path path
                                (fn [[x y]] [(+ (* x scale) (pan 0)) (+ (* y scale) (pan 1))])
                                (fn [[dx dy]] [(/ (- dx (pan 0)) scale) (/ (- dy (pan 1)) scale)]))
        sp (first (:subpaths snapped))]
    (doseq [p (cons (:start sp) (map :p (:segments sp)))]
      (let [[dx dy] [(+ (* (p 0) scale) (pan 0)) (+ (* (p 1) scale) (pan 1))]]
        (is (< (Math/abs (- dx (Math/round dx))) 1e-9))
        (is (< (Math/abs (- dy (Math/round dy))) 1e-9))))
    (testing "a cubic's handles move with their ends"
      (let [before (second (:segments (first (:subpaths path))))
            after (second (:segments sp))
            d-end (v/sub (:p after) (:p before))]
        (is (= (v/add (:c2 before) d-end) (:c2 after)))))))
