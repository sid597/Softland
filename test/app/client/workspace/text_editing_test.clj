(ns app.client.workspace.text-editing-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.workspace.text-editing :as editing]
            [app.client.workspace.text-layout :as tl]))

(defn- mock-clusters [text]
  (loop [offset 0 result []]
    (if (>= offset (tl/code-unit-count text))
      result
      (let [tail (subs text offset)
            [cluster-text width]
            (cond
              (.startsWith tail "fi") ["fi" 1000]
              (.startsWith tail "e\u0301") ["e\u0301" 620]
              :else (let [unit (subs text offset (inc offset))]
                      [unit (case unit "W" 900 "i" 300 " " 300 500)]))
            end (+ offset (tl/code-unit-count cluster-text))]
        (recur end (conj result {:text cluster-text :start offset :end end
                                 :width width}))))))

(def shaped-provider
  {:face-id :t2/mock-proportional
   :face-revision "t2-v1"
   :shaper-id :t2/mock-shaper
   :shaper-version 1
   :upem 1000
   :features ["liga"]
   :variations {}
   :axes {}
   :fallback-chain []
   :metrics {:ascender 800 :descender -200 :lineGap 0}
   :shape-line
   (fn [text _]
     (let [[glyphs clusters advance]
           (reduce
            (fn [[glyphs clusters x] {:keys [text start end width]}]
              (let [rtl? (boolean (re-find #"[\u0600-\u06ff]" text))
                    direction (if rtl? :rtl :ltr)
                    glyph {:glyph-id (int (.charAt ^String text 0))
                           :glyph-id-kind :font-glyph-index
                           :font-id :t2/mock-proportional
                           :font-revision "t2-v1"
                           :cluster-start start :cluster-end end
                           :advance [width 0] :offset [0 0]
                           :position [x 0]
                           :ink-bounds {:xBearing 0 :yBearing 700
                                        :width width :height 800}
                           :direction direction}
                    cluster {:source-start start :source-end end
                             :direction direction :font-revision "t2-v1"
                             :left x :right (+ x width)}]
                [(conj glyphs glyph) (conj clusters cluster) (+ x width)]))
            [[] [] 0]
            (mock-clusters text))
           runs (mapv (fn [glyph]
                        {:source-start (:cluster-start glyph)
                         :source-end (:cluster-end glyph)
                         :direction (:direction glyph)
                         :font-id (:font-id glyph)
                         :font-revision (:font-revision glyph)
                         :upem 1000 :glyphs [glyph]})
                      glyphs)]
       {:runs runs :glyphs glyphs :clusters clusters :advance advance
        :base-direction :ltr}))})

(defn- layout
  ([text] (layout text {}))
  ([text more]
   (tl/layout (merge {:text text :provider shaped-provider
                      :font-size 10 :line-height 12 :baseline-offset 9
                      :origin [0 0] :source-id :t2-test :source-revision 1}
                     more))))

(defn- all-state-indices-safe? [state]
  (let [text (get-in state [:document :text])]
    (and (editing/safe-offset? text (get-in state [:caret :index]))
         (or (nil? (:anchor state))
             (editing/safe-offset? text (:anchor state))))))

(deftest t2-unicode-motion-deletion-and-surrogate-invariant
  (testing "combining and ZWJ sequences are one injected grapheme unit"
    (doseq [text ["e\u0301" "👨‍👩‍👧‍👦"]]
      (let [length (tl/code-unit-count text)
            boundaries {:grapheme [0 length] :word [0 length]
                        :word-spans [[0 length]]}
            at-end (editing/initial-state
                    {:text text :caret (editing/tagged-offset length)})
            moved (:state (editing/move-horizontal at-end :left :grapheme
                                                   boundaries false))
            deleted (editing/delete-direction at-end :left boundaries)]
        (is (= 0 (get-in moved [:caret :index :offset])))
        (is (= "" (get-in deleted [:state :document :text])))
        (is (= :delete-range (get-in deleted [:ops 0 :op]))))))
  (testing "a ligature remains two editing units"
    (let [boundaries {:grapheme [0 1 2] :word [0 2] :word-spans [[0 2]]}
          state (editing/initial-state
                 {:text "fi" :caret (editing/tagged-offset 2)})
          moved (:state (editing/move-horizontal state :left :grapheme
                                                 boundaries false))
          deleted (editing/delete-direction state :left boundaries)]
      (is (= 1 (get-in moved [:caret :index :offset])))
      (is (= "f" (get-in deleted [:state :document :text])))
      (is (= [(editing/tagged-offset 1) (editing/tagged-offset 2)]
             (get-in deleted [:ops 0 :range])))))
  (testing "word motion lands only on injected word boundaries"
    (let [boundaries {:grapheme (range 12)
                      :word [0 5 6 11]
                      :word-spans [[0 5] [6 11]]}
          state (editing/initial-state {:text "hello world"})
          first-hop (:state (editing/move-horizontal state :right :word
                                                     boundaries false))
          second-hop (:state (editing/move-horizontal first-hop :right :word
                                                      boundaries false))]
      (is (= 5 (get-in first-hop [:caret :index :offset])))
      (is (= 6 (get-in second-hop [:caret :index :offset])))))
  (testing "no deterministic edit/motion sequence can create a surrogate interior"
    (let [emoji "😀"
          length (tl/code-unit-count emoji)
          boundaries {:grapheme [0 length] :word [0 length]
                      :word-spans [[0 length]]}
          start (editing/initial-state {:text emoji})
          states (take 80
                       (iterate
                        (fn [state]
                          (let [at-end (assoc state :caret
                                              {:index (editing/tagged-offset
                                                       (tl/code-unit-count
                                                        (get-in state [:document :text])))
                                               :affinity :downstream})]
                            (:state
                             (if (empty? (get-in state [:document :text]))
                               (editing/replace-input state emoji)
                               (editing/delete-direction at-end :left boundaries)))))
                        start))]
      (is (every? all-state-indices-safe? states))
      (is (editing/surrogate-interior? emoji 1))
      (is (thrown? clojure.lang.ExceptionInfo
                   (editing/initial-state
                    {:text emoji :caret (editing/tagged-offset 1)}))))))

(deftest t2-live-readers-enter-ligature-interiors
  (let [layout-result (layout "fi")
        options {:grapheme-boundaries [0 1 2]}
        caret (tl/caret-result layout-result 0 1 options)
        start-x (get-in (tl/caret-result layout-result 0 0 options) [:position 0])
        end-x (get-in (tl/caret-result layout-result 0 2 options) [:position 0])
        hit (tl/hit-test-result layout-result (:position caret) options)]
    (is (< start-x (first (:position caret)) end-x))
    (is (= (editing/tagged-offset 1) (:index caret)))
    (is (= (editing/tagged-offset 1) (:index hit)))
    (is (= (:layout/id layout-result) (:layout/id caret) (:layout/id hit))))
  (testing "bidi and wrapped-boundary affinities survive through state"
    (let [bidi-layout (layout "aب")
          boundaries {:grapheme [0 1 2] :word [0 2]
                      :word-spans [[0 2]]}
          upstream-state (editing/initial-state
                          {:text "aب" :caret (editing/tagged-offset 1)
                           :affinity :upstream})
          downstream-state (editing/initial-state
                            {:text "aب" :caret (editing/tagged-offset 1)
                             :affinity :downstream})
          upstream (editing/caret-geometry bidi-layout
                                            (:caret upstream-state) boundaries)
          downstream (editing/caret-geometry bidi-layout
                                              (:caret downstream-state) boundaries)
          upstream-hit (tl/hit-test-result bidi-layout (:position upstream))
          downstream-hit (tl/hit-test-result bidi-layout (:position downstream))
          wrapped-layout (layout "Wi Wi Wi"
                                 {:wrap-policy :block-greedy :wrap-col 4})
          wrap-upstream (editing/caret-geometry
                         wrapped-layout
                         {:index (editing/tagged-offset 3)
                          :affinity :upstream}
                         {:grapheme (range 9)})
          wrap-downstream (editing/caret-geometry
                           wrapped-layout
                           {:index (editing/tagged-offset 3)
                            :affinity :downstream}
                           {:grapheme (range 9)})]
      (is (not= (:position upstream) (:position downstream)))
      (is (= [(editing/tagged-offset 1) :upstream]
             [(:index upstream-hit) (:affinity upstream-hit)]))
      (is (= [(editing/tagged-offset 1) :downstream]
             [(:index downstream-hit) (:affinity downstream-hit)]))
      (is (= [0 :upstream]
             [(:line wrap-upstream) (:affinity wrap-upstream)]))
      (is (= [1 :downstream]
             [(:line wrap-downstream) (:affinity wrap-downstream)])))))

(deftest t2-consumed-range-and-visual-line-motion
  (let [layout-result (layout "abc   def"
                              {:wrap-policy :block-greedy :wrap-col 5})
        line0 (first (:lines layout-result))
        consumed (:consumed-range line0)
        caret4 (tl/caret-result layout-result 0 4)
        caret3 (tl/caret-result layout-result 0 3)
        selection (tl/selection-result layout-result 0 3 6)
        copied (tl/copy-result layout-result
                               [(editing/tagged-offset 0)
                                (editing/tagged-offset 6)])]
    (is (= [3 6] (mapv tl/source-index-offset consumed)))
    (is (= (:position caret3) (:position caret4)))
    (is (every? zero? (map :w (:rects selection))))
    (is (= "abc   " (:text copied))))
  (let [layout-result (layout "Wi Wi Wi"
                              {:wrap-policy :block-greedy :wrap-col 4})
        boundaries {:grapheme (range (inc (tl/code-unit-count "Wi Wi Wi")))
                    :word [0 2 3 5 6 8]
                    :word-spans [[0 2] [3 5] [6 8]]}
        state (editing/initial-state
               {:text "Wi Wi Wi" :caret (editing/tagged-offset 1)})
        down (editing/move-vertical state :down layout-result boundaries false)
        up (editing/move-vertical (:state down) :up layout-result boundaries false)
        ended (editing/move-line-edge state :end layout-result boundaries false)
        homed (editing/move-line-edge (:state ended) :home layout-result
                                      boundaries false)]
    (is (number? (get-in down [:state :desired-x])))
    (is (= (get-in state [:caret :index]) (get-in up [:state :caret :index])))
    (is (= :upstream (get-in ended [:state :caret :affinity])))
    (is (= :downstream (get-in homed [:state :caret :affinity])))))

(deftest t2-ime-is-one-commit-or-zero
  (let [initial (editing/initial-state
                 {:text "ab" :caret (editing/tagged-offset 1)})
        started (:state (editing/composition-start initial))
        updated (:state (editing/composition-update started "漢字" 1))
        view (editing/composition-view updated)
        committed (editing/composition-end updated "漢字")
        cancelled (editing/composition-end updated "")]
    (is (= "a漢字b" (:text view)))
    (is (= "a漢字b" (get-in committed [:state :document :text])))
    (is (= 1 (count (:ops committed))))
    (is (= :insert (get-in committed [:ops 0 :op])))
    (is (= "ab" (get-in cancelled [:state :document :text])))
    (is (empty? (:ops cancelled)))
    (is (= :dropped-composing-key
           (:status (editing/handle-key updated
                                        {:key "x" :is-composing? true}
                                        {:boundaries {:grapheme [0 1 2]
                                                      :word [0 2]}
                                         :layout-result (layout "ab")}))))))

(deftest t2-paste-dispatch-selection-and-tagged-boundaries
  (let [state (editing/initial-state
               {:text "abcd" :caret (editing/tagged-offset 3)
                :anchor (editing/tagged-offset 1)})
        pasted (editing/paste state "x\r\ny\r\u0001\tz")
        refused (editing/paste state "12345" 4)
        copy-result (editing/handle-key state {:key "c" :ctrl? true} {})
        cut-result (editing/handle-key state {:key "x" :ctrl? true} {})]
    (is (= "ax\ny\n\tzd" (get-in pasted [:state :document :text])))
    (is (= "x\ny\n\tz" (:normalized-text pasted)))
    (is (= :replace-range (get-in pasted [:ops 0 :op])))
    (is (= :paste/over-budget (:reason refused)))
    (is (identical? state (:state refused)))
    (is (= "bc" (:clipboard copy-result)))
    (is (= "bc" (:clipboard cut-result)))
    (is (= "ad" (get-in cut-result [:state :document :text])))
    (is (= :delete-range (get-in cut-result [:ops 0 :op]))))
  (is (= :insert (:action (editing/dispatch-key {:key "😀"}))))
  (is (= :browser-zoom
         (:reason (editing/dispatch-key {:key "+" :ctrl? true}))))
  (is (= :function-key
         (:reason (editing/dispatch-key {:key "F8"}))))
  (is (= :os-shortcut
         (:reason (editing/dispatch-key {:key "a" :meta? true}))))
  (is (thrown? clojure.lang.ExceptionInfo
               (editing/initial-state {:text "x" :caret 0}))))
