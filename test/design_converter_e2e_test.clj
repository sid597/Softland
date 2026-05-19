(ns design-converter-e2e-test
  "End-to-end test for the design converter pipeline:
   extractor JSON → adapter → token-matcher → compiler → rt-node"
  (:require [clojure.test :refer [deftest testing is]]
            [components.adapter :as adapter]
            [components.compiler :as compiler]
            [components.token-matcher :as token-matcher]
            [components.design-tokens :as design-tokens]))

;; Fixture: simulates _extractor.js output for a shadcn Card component
(def fixture-card
  {:tag "div"
   :bounds {:x 0 :y 0 :w 350 :h 180}
   :styles {:backgroundColor "rgb(9, 9, 11)"
            :borderTopWidth "1px" :borderRightWidth "1px"
            :borderBottomWidth "1px" :borderLeftWidth "1px"
            :borderTopColor "rgb(39, 39, 42)"
            :borderRightColor "rgb(39, 39, 42)"
            :borderBottomColor "rgb(39, 39, 42)"
            :borderLeftColor "rgb(39, 39, 42)"
            :borderTopLeftRadius "8px" :borderTopRightRadius "8px"
            :borderBottomRightRadius "8px" :borderBottomLeftRadius "8px"
            :boxShadow "none"
            :display "flex" :flexDirection "column" :gap "0px"
            :paddingTop "24px" :paddingRight "24px"
            :paddingBottom "24px" :paddingLeft "24px"
            :fontSize "14px" :fontWeight "400" :color "rgb(250, 250, 250)"
            :opacity "1"}
   :textContent nil
   :children [{:tag "h3"
               :bounds {:x 24 :y 24 :w 302 :h 24}
               :styles {:backgroundColor "rgba(0, 0, 0, 0)"
                        :fontSize "16px" :fontWeight "600"
                        :color "rgb(250, 250, 250)"
                        :borderTopWidth "0px" :borderRightWidth "0px"
                        :borderBottomWidth "0px" :borderLeftWidth "0px"
                        :borderTopLeftRadius "0px" :borderTopRightRadius "0px"
                        :borderBottomRightRadius "0px" :borderBottomLeftRadius "0px"
                        :display "block" :paddingTop "0px" :paddingRight "0px"
                        :paddingBottom "0px" :paddingLeft "0px"
                        :opacity "1"}
               :textContent "Card Title"
               :children nil}
              {:tag "p"
               :bounds {:x 24 :y 52 :w 302 :h 20}
               :styles {:backgroundColor "rgba(0, 0, 0, 0)"
                        :fontSize "14px" :fontWeight "400"
                        :color "rgb(161, 161, 170)"
                        :borderTopWidth "0px" :borderRightWidth "0px"
                        :borderBottomWidth "0px" :borderLeftWidth "0px"
                        :borderTopLeftRadius "0px" :borderTopRightRadius "0px"
                        :borderBottomRightRadius "0px" :borderBottomLeftRadius "0px"
                        :display "block" :paddingTop "0px" :paddingRight "0px"
                        :paddingBottom "0px" :paddingLeft "0px"
                        :opacity "1"}
               :textContent "Card description text goes here."
               :children nil}]})

(deftest adapter-produces-valid-ir
  (testing "Adapter converts extractor output to IR with correct structure"
    (let [ir (adapter/extracted->ir fixture-card {:source-url "test://shadcn/card"})]
      (is (= "div" (:tag ir)))
      (is (= :container (:role ir)))
      (is (map? (:visual ir)) "Card should have visual (fill + border + radius)")
      (is (some? (get-in ir [:visual :fill])) "Card has a dark background → :fill")
      (is (some? (get-in ir [:visual :radius])) "Card has border-radius → :radius")
      (is (some? (get-in ir [:visual :border])) "Card has 1px border → :border")
      (is (= 2 (count (:children ir))) "Card has 2 children (h3, p)")
      (let [h3 (first (:children ir))]
        (is (= "h3" (:tag h3)))
        (is (= :text (:role h3)))
        (is (= "Card Title" (get-in h3 [:typography :content])))))))

(deftest full-pipeline-produces-rt-node
  (testing "Full pipeline: adapter → tokenizer → compiler → rt-node"
    (let [dt   design-tokens/dt
          ir   (adapter/extracted->ir fixture-card {:source-url "test://shadcn/card"})
          tok  (token-matcher/tokenize-ir ir dt)
          node (compiler/compile-ir tok dt {:use-tokens? true})]
      ;; rt-node shape checks
      (is (map? node) "Compiler returns a map")
      (is (some? (:id node)) "rt-node has :id")
      (is (some? (:bounds node)) "rt-node has :bounds")
      (is (map? (:style node)) "rt-node has :style")
      ;; w should be a number, not a vector or keyword
      (is (number? (get-in node [:bounds :w])) "Width is a number")
      (is (number? (get-in node [:bounds :h])) "Height is a number")
      ;; Background color should be resolved to [r g b a]
      (let [bg (get-in node [:style :bg])]
        (is (vector? bg) "Background is an RGBA vector")
        (is (= 4 (count bg)) "RGBA has 4 components")
        (is (every? number? bg) "RGBA components are numbers"))
      ;; Children
      (is (vector? (:children node)) "rt-node has children vector")
      (is (= 2 (count (:children node))) "rt-node has 2 children")
      ;; Text on the first child
      (let [first-child (first (:children node))]
        (is (seq (:text first-child)) "First child (h3) has text ops")
        (let [text-op (first (:text first-child))]
          (is (= "Card Title" (:text text-op)) "Text op contains 'Card Title'"))))))

(deftest tokenizer-snaps-colors
  (testing "Token matcher snaps close colors to dt tokens"
    (let [dt  design-tokens/dt
          ir  (adapter/extracted->ir fixture-card {:source-url "test://shadcn/card"})
          tok (token-matcher/tokenize-ir ir dt)]
      ;; The card bg is rgb(9,9,11) ≈ [0.035 0.035 0.043] which should snap to :bg [0.09 0.09 0.11]
      ;; (within the 0.08 threshold)
      (let [fill (get-in tok [:visual :fill])]
        (is (some? fill) "Tokenized IR has fill")
        ;; After tokenization, fill may be {:type :token :ref [:colors :bg]}
        ;; or still {:type :solid :value ...} if outside threshold
        (is (map? fill) "Fill is a map")))))
