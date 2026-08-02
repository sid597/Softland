(ns app.client.workspace.text-layout-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.workspace.text-layout :as tl]))

(defn- corpus-layout []
  (tl/layout {:text "alpha beta gamma"
              :font-size 10
              :char-advance 5
              :line-height 12
              :origin [10 20]
              :baseline-offset 8
              :inline-size 50
              :wrap-policy :word
              :clip {:left 14 :right 42 :top 20 :bottom 44}
              :source-id :t0-corpus
              :source-revision 7}))

(deftest t0-one-identity-for-seven-readers
  (let [layout-result (corpus-layout)
        readers [(tl/measure-result layout-result)
                 (tl/wrap-result layout-result)
                 (tl/paint-result layout-result)
                 (tl/caret-result layout-result 0 3)
                 (tl/selection-result layout-result 0 1 4)
                 (tl/clip-result layout-result
                                 {:text "alpha beta" :x 10 :y 28
                                  :from 0 :to 10 :size 10})
                 (tl/hit-test-result layout-result [27 23])]
        ids (mapv :layout/id readers)]
    (testing "measure, wrap, paint, caret, selection, clip, and hit share identity"
      (is (= 7 (count ids)))
      (is (every? #{(:layout/id layout-result)} ids)))
    (testing "the adapter declares its index domain instead of leaking integers"
      (is (= tl/legacy-index-space (get-in layout-result [:source :index-space])))
      (is (every? #(= tl/legacy-index-space (:index-space %))
                  (mapcat :source-range (:lines layout-result)))))))

(deftest t0-legacy-provider-zero-diff-corpus
  (testing "word wrap and measurement reproduce the current monospace road"
    (let [layout-result (corpus-layout)]
      (is (= ["alpha beta" "gamma"] (:lines (tl/wrap-result layout-result))))
      (is (= [50 24] (get-in (tl/measure-result layout-result)
                              [:metrics :advance])))
      (is (= [[10 28] [15 28] [20 28]]
             (mapv :position (take 3 (:glyphs (tl/paint-result layout-result))))))))
  (testing "caret and selection keep the shipped arithmetic geometry"
    (let [layout-result (corpus-layout)]
      (is (= {:x 25 :y 20 :w 2 :h 12}
             (:rect (tl/caret-result layout-result 0 3))))
      (is (= {:x 15 :y 20 :w 15 :h 12}
             (:rect (tl/selection-result layout-result 0 1 4))))))
  (testing "clip uses ceil-left/floor-right and hit uses rounded columns"
    (let [clip-layout (tl/layout {:text "abcdef"
                                  :source-lines ["abcdef"]
                                  :font-size 10 :char-advance 8 :line-height 12
                                  :origin [0 10]
                                  :clip {:left 9 :right 35 :top 10}})
          clipped (:op (tl/clip-result clip-layout
                                       {:text "abcdef" :x 0 :y 10
                                        :from 0 :to 6 :size 10}))
          hit-layout (tl/layout {:text "abcdef"
                                 :source-lines ["abcdef"]
                                 :font-size 10 :char-advance 8 :line-height 12
                                 :origin [0 0]})]
      (is (= {:text "cd" :x 16 :y 10 :from 2 :to 4 :size 10} clipped))
      (is (= {:line 0 :col 3}
             (select-keys (tl/hit-test-result hit-layout [20 2]) [:line :col])))))
  (testing "T0 stays the declared legacy provider; it does not smuggle in T1"
    (is (= :legacy/code-unit-grid
           (get-in (corpus-layout) [:shaping :shaper-id])))))

(deftest t0-ground-wrapper-is-behavior-identical
  (is (= ["one two" "three" "1234567" "8"]
         (tl/block-wrap-lines ["one two three" "12345678"] 7)))
  (is (= ["a long" "word" "abcdefgh" "ij"]
         (vec (mapcat #(tl/wrap-line % 8)
                      ["a long word" "abcdefghij"])))))
