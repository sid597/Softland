(ns app.client.substrate.chrome-material-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.substrate.chrome-material :as chrome]))

(def handle
  {:chrome/form :handle
   :chrome/anchor-bounds {:x 10.0 :y 20.0 :w 0.0 :h 0.0}
   :chrome/derived-from {:vi :fixture :address :ink}
   :chrome/selection-rev 3
   :chrome/pick :interior
   :chrome/corner :nw})

(deftest grammar-declarations-and-packing-tripwire
  (is (= handle (chrome/validate-material! handle)))
  (is (= 6 (count (chrome/material-vertices handle))))
  (is (= chrome/vertex-words
         (count (chrome/vertex-values (first (chrome/material-vertices handle)) 4))))
  (doseq [[form declaration] chrome/form-declarations]
    (is (= #{:inside :boundary :outside}
           (get-in declaration [:classify :result])))
    (is (= (if (= :handle form) :interior :none)
           (get-in declaration [:pick :policy]))))
  (testing "grammar is closed and corner identity is load-bearing"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"unknown fields"
                          (chrome/validate-material! (assoc handle :font "no"))))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"legal :chrome/corner"
                          (chrome/validate-material! (dissoc handle :chrome/corner)))))
  (is (chrome/assert-corpus-coverage!
       {:selection-outline [:jvm :gpu]
        :handle [:jvm :gpu :pick]
        :marquee [:jvm :gpu]
        :guide-line [:jvm :gpu]
        :gap-tick [:jvm :gpu]})))

(deftest hybrid-anchor-and-screen-metric-tripwire
  (let [effective {:affine [2.0 0.0 0.0 2.0 30.0 40.0] :flags 0}
        anchor {:x 10.0 :y 20.0 :w 40.0 :h 30.0}
        stations [0.01 0.1 1.0 8.0 100.0 1000.0]
        handle-rects
        (mapv #(chrome/chrome-screen-rect
                {:x 10.0 :y 20.0 :w 0.0 :h 0.0}
                effective {:x 7.0 :y 11.0 :zoom %}
                {:x -5.0 :y -5.0 :w 10.0 :h 10.0})
              stations)
        outlines
        (mapv #(chrome/chrome-screen-rect
                anchor effective {:x 7.0 :y 11.0 :zoom %}
                {:x -0.5 :y -0.5 :w 1.0 :h 1.0})
              stations)]
    (is (every? #(= [10.0 10.0] [(:w %) (:h %)]) handle-rects)
        "metric size is invariant over the legal zoom envelope")
    (is (= [1.8000000000000007 81.0 8001.0 80001.0]
           (mapv :w [(first outlines) (nth outlines 2)
                     (nth outlines 4) (last outlines)]))
        "anchor dimensions track container and camera scale")
    (is (= {:x 2.5 :y 6.800000000000001 :w 10.0 :h 10.0}
           (first handle-rects)))))

(deftest gap-tick-is-a-constant-screen-space-quad
  (let [base {:chrome/form :gap-tick
              :chrome/anchor-bounds {:x 50.0 :y 60.0 :w 0.0 :h 0.0}
              :chrome/derived-from :gesture
              :chrome/selection-rev 0
              :chrome/pick :none
              :chrome/from [50.0 60.0]
              :chrome/to [50.0 60.0]}
        x-offsets (mapv :offset-px
                        (first (chrome/material-quads
                                (assoc base :chrome/alignment {:axis :x}))))
        y-offsets (mapv :offset-px
                        (first (chrome/material-quads
                                (assoc base :chrome/alignment [:y :equal-gap 60.0]))))]
    (is (= [[-0.5 -4.0] [0.5 -4.0] [0.5 4.0] [-0.5 4.0]] x-offsets))
    (is (= [[-4.0 -0.5] [4.0 -0.5] [4.0 0.5] [-4.0 0.5]] y-offsets))))
