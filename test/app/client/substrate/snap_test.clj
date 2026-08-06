(ns app.client.substrate.snap-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.substrate.snap :as snap]))

(def effective
  {1 {:affine [2.0 0.0 0.0 2.0 100.0 0.0] :flags 0}
   2 {:affine [1.0 0.0 0.0 1.0 0.0 0.0] :flags 0}})

(def targets
  {:drag [{:vi :drag/vi :container 2 :bounds {:x 0.0 :y 0.0 :w 10.0 :h 10.0}
           :family :render.family/rect}]
   :fixture [{:vi :fixture/vi :container 1
              :bounds {:x 10.0 :y 10.0 :w 20.0 :h 20.0}
              :family :render.family/path}]
   :edge [{:vi :edge/vi :container 2
           :bounds {:x 0.0 :y 0.0 :w 1000.0 :h 1000.0}
           :family :render.family/connector}]
   :chrome [{:vi :chrome/vi :container 2
             :bounds {:x 0.0 :y 0.0 :w 1000.0 :h 1000.0}
             :family :render.family/chrome}]})

(deftest candidate-world-composition-and-exclusions-tripwire
  (let [candidates (snap/extract-candidates
                    targets effective {:vi :drag/vi :address :drag})]
    (is (= #{:fixture} (set (map (comp :address :target) candidates))))
    (is (= #{120.0 140.0 160.0}
           (set (map :coordinate (filter #(= :x (:axis %)) candidates))))
        "fixture-local bounds compose through its non-identity affine")))

(deftest absolute-snap-ties-guides-and-static-counter-tripwire
  (let [candidates
        [{:axis :x :kind :edge :coordinate 100.0 :target {:vi :a :address :a}
          :target-bounds {:x 100.0 :y 0.0 :w 20.0 :h 40.0}}
         {:axis :x :kind :center :coordinate 100.0 :target {:vi :b :address :b}
          :target-bounds {:x 90.0 :y 20.0 :w 20.0 :h 20.0}}
         {:axis :y :kind :edge :coordinate 60.0 :target {:vi :c :address :c}
          :target-bounds {:x 20.0 :y 60.0 :w 80.0 :h 10.0}}]
        input {:raw-position [91.0 51.0]
               :raw-bounds {:x 91.0 :y 51.0 :w 10.0 :h 10.0}
               :candidates candidates :zoom 1.0}
        first-step (snap/gesture-step (snap/empty-state) input)
        result (:result first-step)
        static-step (snap/gesture-step (:state first-step) input)]
    (is (= [90.0 50.0] (:position result)))
    (is (= {:x 90.0 :y 50.0 :w 10.0 :h 10.0} (:bounds result)))
    (is (= #{[:x :center 100.0] [:y :edge 60.0]} (:alignments result))
        "equal distance chooses center over edge")
    (doseq [guide (:guides result)]
      (let [axis (:axis guide) coord (:candidate-coordinate guide)
            {:keys [x y w h]} (:bounds result)
            features (if (= :x axis)
                       #{x (+ x (/ w 2.0)) (+ x w)}
                       #{y (+ y (/ h 2.0)) (+ y h)})]
        (is (contains? features coord)
            "every guide names an exact feature of the applied position")))
    (is (= 1 (get-in first-step [:state :snap-resolutions])))
    (is (false? (:derived? static-step)))
    (is (= 1 (get-in static-step [:state :snap-resolutions])))
    (is (= (:position result)
           (mapv + (:raw-position result) (:delta result)))
        "input shifting reproduces the resolved position under a linear move law")))

(deftest threshold-identity-and-equal-gap-tripwire
  (let [neighbors
        [{:axis :x :kind :edge :coordinate 20.0 :target {:vi :l :address :l}
          :target-bounds {:x 0.0 :y 0.0 :w 20.0 :h 20.0}}
         {:axis :x :kind :edge :coordinate 40.0 :target {:vi :l :address :l}
          :target-bounds {:x 0.0 :y 0.0 :w 20.0 :h 20.0}}
         {:axis :x :kind :edge :coordinate 80.0 :target {:vi :r :address :r}
          :target-bounds {:x 80.0 :y 0.0 :w 20.0 :h 20.0}}]
        ticks (snap/equal-gap-ticks {:x 40.0 :y 0.0 :w 20.0 :h 20.0}
                                    neighbors 0.0)
        identity (snap/resolve-snap
                  {:raw-position [200.0 200.0]
                   :raw-bounds {:x 200.0 :y 200.0 :w 10.0 :h 10.0}
                   :candidates neighbors :zoom 8.0})]
    (is (= 2 (count ticks)))
    (is (= [200.0 200.0] (:position identity)))
    (is (= #{} (:alignments identity)))
    (testing "screen threshold divides by zoom"
      (is (= 800.0 (snap/threshold-world 0.01)))
      (is (= 0.008 (snap/threshold-world 1000.0))))))

(deftest zoom-envelope-absolute-candidate-and-smaller-coordinate-tie
  (doseq [zoom [0.01 0.1 1.0 8.0 100.0 1000.0]]
    (let [threshold (snap/threshold-world zoom)
          coordinate (+ 10.0 threshold)
          candidate {:axis :x :kind :edge :coordinate coordinate
                     :target {:vi [:station zoom] :address :candidate}
                     :target-bounds {:x coordinate :y 0.0 :w 10.0 :h 20.0}}
          result (snap/resolve-snap
                  {:raw-position [0.0 0.0]
                   :raw-bounds {:x 0.0 :y 0.0 :w 10.0 :h 10.0}
                   :candidates [candidate] :zoom zoom})]
      (is (< (abs (- threshold (first (:delta result)))) 1.0e-12))
      (is (= coordinate (+ (get-in result [:bounds :x])
                           (get-in result [:bounds :w])))
          (str "snapped edge equals candidate at zoom " zoom))
      (is (= coordinate (get-in result [:guides 0 :candidate-coordinate])))))
  (let [moving {:x 0.0 :y 0.0 :w 10.0 :h 10.0}
        match (snap/resolve-axis
               :x moving
               [{:axis :x :kind :edge :coordinate 9.0
                 :target {:vi :right :address :right}
                 :target-bounds {:x 9.0 :y 0.0 :w 1.0 :h 1.0}}
                {:axis :x :kind :edge :coordinate 1.0
                 :target {:vi :left :address :left}
                 :target-bounds {:x 1.0 :y 0.0 :w 1.0 :h 1.0}}]
               10.0)]
    (is (= 1.0 (:candidate-coordinate match))
        "equal distance and kind choose the smaller candidate coordinate")))
