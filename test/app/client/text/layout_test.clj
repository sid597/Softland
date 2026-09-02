(ns app.client.text.layout-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.text.layout :as tl]))

(defn- mock-clusters [text]
  (loop [offset 0 clusters []]
    (if (>= offset (.length ^String text))
      clusters
      (let [remaining (subs text offset)
            [cluster-text width glyph-id]
            (cond
              (.startsWith remaining "ffi") ["ffi" 1100 900]
              (.startsWith remaining "e\u0301") ["e\u0301" 620 901]
              (.startsWith remaining "AV") ["A" 560 65]
              (= \tab (.charAt ^String text offset)) ["\t" 1600 nil]
              :else (let [s (subs text offset (inc offset))]
                      [s (case s
                           "i" 280 "l" 300 "W" 940 " " 360
                           "漢" 1000 "字" 1000
                           620)
                       (int (.charAt ^String text offset))]))
            end (+ offset (.length ^String cluster-text))]
        (recur end (conj clusters {:text cluster-text :start offset :end end
                                   :width width :glyph-id glyph-id}))))))

(def shaped-provider
  {:face-id :mock/proportional
   :face-revision "mock-variable-v1"
   :shaper-id :mock/harfbuzz
   :shaper-version "8.3"
   :upem 1000
   :features ["kern" "liga" "clig"]
   :variations {:wght 425 :wdth 92}
   :axes {:wght {:min 100 :default 400 :max 800}
          :wdth {:min 75 :default 100 :max 125}}
   :fallback-chain [{:id :mock/cjk :revision "cjk-v1"}]
   :metrics {:ascender 800 :descender -200 :lineGap 100}
   :shape-line
   (fn [text _opts]
     (let [logical (mock-clusters text)
           rtl? (boolean (re-find #"[\u0600-\u06ff]" text))
           visual (if rtl? (reverse logical) logical)
           [glyphs clusters advance]
           (reduce
             (fn [[glyphs clusters x] {:keys [text start end width glyph-id]}]
               (let [direction (if rtl? :rtl :ltr)
                     fallback? (boolean (re-find #"[漢字]" text))
                     font-id (if fallback? :mock/cjk :mock/proportional)
                     font-revision (if fallback? "cjk-v1" "mock-variable-v1")
                     glyph {:glyph-id glyph-id
                            :glyph-id-kind (if glyph-id :font-glyph-index :virtual/tab)
                            :font-id font-id :font-revision font-revision
                            :cluster-start start :cluster-end end
                            :advance [width 0] :offset [0 0]
                            :position [x 0]
                            :ink-bounds {:xBearing 10 :yBearing 700
                                         :width (max 0 (- width 20)) :height 800}
                            :direction direction}
                     cluster {:source-start start :source-end end
                              :direction direction :font-revision font-revision
                              :left x :right (+ x width)}]
                 [(conj glyphs glyph) (conj clusters cluster) (+ x width)]))
             [[] [] 0]
             visual)
           runs (mapv (fn [glyph]
                        {:source-start (:cluster-start glyph)
                         :source-end (:cluster-end glyph)
                         :direction (:direction glyph)
                         :font-id (:font-id glyph)
                         :font-revision (:font-revision glyph)
                         :upem 1000
                         :glyphs [glyph]})
                      glyphs)]
       {:runs runs :glyphs glyphs :clusters clusters :advance advance
        :base-direction (if rtl? :rtl :ltr)}))})

(defn- shaped-corpus-layout []
  (tl/layout {:text "AV office e\u0301\tسلام\n漢字"
              :provider shaped-provider
              :font-size 10 :line-height 14
              :origin [20 30] :baseline-offset 10
              :clip {:left 24 :right 90 :top 30 :bottom 60}
              :source-id :t1-corpus :source-revision 1
              :zoom 1}))

(deftest one-identity-for-seven-readers
  (let [layout-result (shaped-corpus-layout)
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
      (is (= tl/index-space (get-in layout-result [:source :index-space])))
      (is (every? #(= tl/index-space (:index-space %))
                  (mapcat :source-range (:lines layout-result)))))))

(deftest layout-requires-shaped-provider
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"requires a shaped provider"
                        (tl/layout {:text "no grid fallback"}))))

(deftest t1-shaped-corpus-moves-all-seven-readers-together
  (let [layout-result (shaped-corpus-layout)
        readers [(tl/measure-result layout-result)
                 (tl/wrap-result layout-result)
                 (tl/paint-result layout-result)
                 (tl/caret-result layout-result 0 4)
                 (tl/selection-result layout-result 0 3 9)
                 (tl/clip-result layout-result
                                 {:text "AV office e\u0301\tسلام"
                                  :from 0 :to 19 :x 20 :y 40 :size 10})
                 (tl/hit-test-result layout-result [42 34])]]
    (testing "all readers retain one shaped layout identity"
      (is (every? #{(:layout/id layout-result)} (map :layout/id readers))))
    (testing "ligatures and combining marks remain declared clusters"
      (let [clusters (mapcat tl/line-clusters (:lines layout-result))]
        (is (some #(= 3 (- (get-in % [:source-range 1 :offset])
                            (get-in % [:source-range 0 :offset])))
                  clusters))
        (is (some #(= 2 (- (get-in % [:source-range 1 :offset])
                            (get-in % [:source-range 0 :offset])))
                  clusters))))
    (testing "bidi, fallback, tabs/newlines, and variable axes survive the result"
      (is (some #(= :rtl (:direction %)) (tl/result-runs layout-result)))
      (is (some #(= "cjk-v1" (:font-revision %))
                (tl/result-runs layout-result)))
      (is (some #(= :virtual/tab (:glyph-id-kind %))
                (:glyphs (tl/paint-result layout-result))))
      (is (= 2 (count (:lines layout-result))))
      (is (= {:wght 425 :wdth 92} (get-in layout-result [:font :variations]))))
    (testing "proportional advances and cluster-derived carets are observable"
      (let [advances (mapv #(get-in % [:advance 0])
                           (:glyphs (tl/paint-result layout-result)))]
        (is (> (count (distinct advances)) 1)))
      (is (not= 40 (get-in (tl/caret-result layout-result 0 4) [:rect :x]))))
    (testing "ink bounds remain in the same nonzero-origin material space"
      (is (< (Math/abs
              (- 20.1
                 (get-in (first (tl/line-glyphs
                                 (first (:lines layout-result))))
                         [:ink-bounds :x])))
             1.0e-5)))))

(deftest t1-wrap-and-legal-zoom-are-result-owned
  (let [wrapped (tl/layout {:text "WW ii WW"
                            :provider shaped-provider
                            :font-size 10 :line-height 12
                            :inline-size 30 :wrap-policy :word
                            :zoom 0.01})]
    (is (> (count (:lines wrapped)) 1))
    (is (= [0.01 1000] (get-in wrapped [:lod :legal-zoom])))
    (is (= 0.01 (get-in wrapped [:lod :zoom]))))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"legal component range"
                        (tl/layout {:text "zoom"
                                    :provider shaped-provider
                                    :font-size 10 :line-height 12
                                    :zoom 1000.01}))))
