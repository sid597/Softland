(ns app.client.text.layout-planes-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.text.layout :as tl]))

(defn- monotonic-provider []
  {:face-id :planes/mock
   :face-revision "planes-v1"
   :shaper-id :planes/mock
   :shaper-version 1
   :upem 1000
   :metrics {:ascender 800 :descender -200 :lineGap 0}
   :shape-line
   (fn [text _]
     (let [n (.length ^String text)
           glyphs (mapv (fn [i]
                          {:glyph-id (int (.charAt ^String text i))
                           :glyph-id-kind :font-glyph-index
                           :font-id :planes/mock
                           :font-revision "planes-v1"
                           :cluster-start i :cluster-end (inc i)
                           :advance [500 0] :offset [0 0]
                           :position [(* i 500) 0]
                           :ink-bounds nil :direction :ltr})
                        (range n))
           clusters (mapv (fn [i]
                            {:source-start i :source-end (inc i)
                             :direction :ltr :font-revision "planes-v1"
                             :left (* i 500) :right (* (inc i) 500)})
                          (range n))]
       {:runs (cond-> []
                (pos? n)
                (conj {:source-start 0 :source-end n :direction :ltr
                       :font-id :planes/mock :font-revision "planes-v1"
                       :glyphs glyphs}))
        :glyphs glyphs :clusters clusters :advance (* n 500)}))})

(defn- bytes-per-code-unit [layout]
  (let [units (tl/code-unit-count (get-in layout [:source :text]))]
    (/ (:plane-bytes (tl/plane-coverage-check layout)) (max 1 units))))

(deftest retained-layout-is-planes-not-rich-maps
  (let [layout (tl/layout {:text "shaped plane"
                           :provider (monotonic-provider)
                           :font-size 12 :line-height 16})]
    (testing "the retained result owns only columnar populous data"
      (is (= 2 (:text-layout/version layout)))
      (is (map? (:layout/planes layout)))
      (is (nil? (:runs layout)))
      (is (nil? (:clusters layout)))
      (is (false? (tl/retained-rich-map? layout)))
      (is (every? #(not-any? (set (keys %))
                             [:glyphs :clusters :glyph-span-index])
                  (:lines layout))))
    (testing "the derived Contract-T view preserves characters and identity"
      (is (= (get-in layout [:source :text])
             (apply str (map :character
                             (:glyphs (tl/paint-result layout))))))
      (is (every? #{(:layout/id layout)}
                  (map :layout/id [(tl/paint-result layout)
                                   (tl/caret-result layout 0 1)
                                   (tl/selection-result layout 0 0 1)]))))))

(deftest plane-coverage-check-pins-the-contract-budget
  (let [text (apply str (repeat 10000 "x"))
        layout (tl/layout {:text text :provider (monotonic-provider)
                           :font-size 12 :line-height 16})
        {:keys [glyph-count span-count glyph-bytes-per-glyph
                span-bytes-per-entry]} (tl/plane-coverage-check layout)]
    (is (= 10000 glyph-count))
    (is (= 10000 span-count))
    (is (<= glyph-bytes-per-glyph 48))
    (is (<= span-bytes-per-entry 16))
    (is (<= (bytes-per-code-unit layout) 64))))

(deftest indexed-view-is-derived-from-the-span-plane
  (let [layout (tl/layout {:text "first\nlater"
                           :provider (monotonic-provider)
                           :font-size 12 :line-height 16})
        [first-line later-line] (:lines layout)]
    (is (= [0 1]
           ((juxt :source-start :source-end)
            (first (tl/glyph-span-index-view first-line)))))
    (is (= [6 7]
           ((juxt :source-start :source-end)
            (first (tl/glyph-span-index-view later-line)))))
    (is (= "later"
           (apply str
                  (map :character
                       (:glyphs
                        (tl/glyphs-in-source-range
                         later-line (:paint-source-range later-line)))))))))
