(ns app.client.text.flat-route-test
  "The flat route's JVM regression tests (docs/shaping-correction/SHAPER-BORDER.md §8):
   F2 — the flat `shaped-layout` and the frozen map route agree plane for plane
   on every consistency-check corpus; the shaped-line converters round-trip; the I1/I2
   entry points keep working on flat results; layout never memoizes."
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.text.layout :as tl]
            [app.client.text.layout-oracle :as oracle]
            [app.client.text.layout-planes :as planes]
            [app.client.text.shaped-line :as sl]
            [app.client.text.layout-test :as layout-test]
            [app.client.text.shaping-correction-test :as sct]))

(def ^:private corpus-text "AV office é\tسلام ɐ\n漢字")

(defn- corpus-input [overrides]
  (merge {:text corpus-text
          :provider layout-test/shaped-provider
          :font-size 10 :line-height 14
          :origin [20 30] :baseline-offset 10
          :clip {:left 24 :right 90 :top 30 :bottom 60}
          :source-id :flat-route :source-revision 1
          :zoom 1}
         overrides))

(defn- synthetic-input [text overrides]
  ((deref #'sct/layout-input) text overrides))

(def ^:private consistency-check-inputs
  (concat
   [(corpus-input {})
    (corpus-input {:headers ["Heading one" "H2"]})
    (corpus-input {:inline-size 30 :wrap-policy :word :zoom 0.01})
    {:text "WW ii WW" :provider layout-test/shaped-provider
     :font-size 10 :line-height 12 :inline-size 30 :wrap-policy :word}]
   (map #(synthetic-input % {})
        ["abc   def" "  abc def" "abc\tdef" "" "a\n\nb\n" "abc   "
         (str "a" "\u00a0" " b")])
   [(synthetic-input "😀a" {:wrap-col 1})
    (synthetic-input "a      z" {:wrap-col 2})
    (synthetic-input "abcdef" {:wrap-col 3})
    (synthetic-input "abc def" {:headers ["hdr"] :wrap-col 4})
    (synthetic-input "abcdef" {:wrap-col 1
                               :provider ((deref #'sct/synthetic-provider) :empty)})
    (synthetic-input "abcdef" {:wrap-col 1
                               :provider ((deref #'sct/synthetic-provider) :zero)})
    (synthetic-input (deref #'sct/pathological-1x)
                     {:provider ((deref #'sct/pathological-provider)
                                 {:shuffle? true})})
    (synthetic-input (deref #'sct/pathological-1x)
                     {:provider ((deref #'sct/pathological-provider)
                                 {:shuffle? false})})]))

(deftest f2-flat-planes-equal-the-frozen-map-route
  (doseq [input consistency-check-inputs]
    (let [flat (tl/layout input)
          mapped (oracle/layout input)]
      (testing (pr-str (select-keys input [:text :wrap-policy :wrap-col :headers]))
        (is (tl/result= flat mapped))
        (is (= (:layout/id flat) (:layout/id mapped)))
        (is (= (tl/plane-coverage-check flat) (tl/plane-coverage-check mapped)))
        (is (= (:glyphs (tl/paint-result flat))
               (:glyphs (tl/paint-result mapped))))
        (is (= (mapv tl/line-clusters (:lines flat))
               (mapv tl/line-clusters (:lines mapped))))))))

(deftest f2-comparator-is-not-vacuous
  (let [flat (tl/layout (corpus-input {}))
        planes (:layout/planes flat)
        touched (let [copy (java.util.Arrays/copyOf ^floats (:position-x planes)
                                                    (alength ^floats (:position-x planes)))]
                  (aset-float copy 0 (+ 1.0 (aget copy 0)))
                  (assoc flat :layout/planes (assoc planes :position-x copy)))]
    (is (tl/result= flat flat))
    (is (false? (tl/result= flat touched)))
    (is (false? (tl/result= flat (assoc-in flat [:lines 0 :advance] -1))))))

(defn- u32-ids
  "Glyph ids are u32 in the flat vocabulary (HarfBuzz's); the synthetic
   providers hash text into signed ids, so the strict round trip compares
   them masked."
  [m]
  (let [mask (fn [glyph] (update glyph :glyph-id #(some-> % (bit-and 0xffffffff))))]
    (-> m
        (update :glyphs #(mapv mask %))
        (update :runs (fn [runs]
                        (mapv #(update % :glyphs (fn [gs] (mapv mask gs))) runs))))))

(deftest shaped-line-converters-round-trip
  (testing "a real provider line survives from-maps -> ->maps exactly"
    (doseq [line [((:shape-line layout-test/shaped-provider)
                   "AV office é\tسلام ɐ" nil)
                  ((deref #'sct/shaped-line) "ab \tcd ef ")]]
      (is (= (u32-ids line) (sl/->maps (sl/from-maps line))))))
  (testing "the shuffled pathological line keeps every glyph, run, and cluster"
    (let [provider ((deref #'sct/pathological-provider) {:shuffle? true})
          line (u32-ids ((:shape-line provider) "ab \tcd ef ab \tcd ef " nil))
          back (sl/->maps (sl/from-maps line))
          run-sets (fn [m] (mapv #(update % :glyphs set) (:runs m)))]
      (is (= (:glyphs line) (:glyphs back)))
      (is (= (:clusters line) (:clusters back)))
      (is (= (:advance line) (:advance back)))
      (is (= (run-sets line) (run-sets back))))))

(deftest i2-oracle-mode-sees-through-planes
  (let [input (corpus-input {})
        address [:flat-route :a]
        key (tl/layout-key {:address address :body-text corpus-text
                            :provider layout-test/shaped-provider
                            :font-size 10 :line-height 14 :baseline-offset 10
                            :wrap-policy :none})
        miss (tl/layout-cache-acquire (tl/empty-layout-cache) address key
                                      #(tl/layout input))
        hit (tl/layout-cache-acquire (:cache miss) address key
                                     #(tl/layout input) :oracle? true)]
    (is (false? (:hit? miss)))
    (is (:hit? hit))
    (is (true? (:oracle-match? hit)))
    (is (zero? (get-in hit [:cache :oracle-mismatches])))))

(deftest layout-never-memoizes-shaping
  (let [!calls (atom 0)
        provider (update layout-test/shaped-provider :shape-line
                         (fn [f] (fn [text opts] (swap! !calls inc) (f text opts))))
        input (corpus-input {:provider provider})
        first-result (tl/layout input)
        after-first @!calls
        second-result (tl/layout input)]
    (is (= 2 (count (:lines first-result))))
    (is (= 2 after-first))
    (is (= 4 @!calls))
    (is (= 2 (get-in first-result [:stats :proportionality :shape-calls])))
    (is (= 2 (get-in second-result [:stats :proportionality :shape-calls])))
    (is (tl/result= first-result second-result))))

(deftest flat-result-keeps-the-retained-vocabulary
  (let [flat (tl/layout (corpus-input {:headers ["Heading"]}))]
    (is (false? (tl/retained-rich-map? flat)))
    (is (= 2 (:text-layout/version flat)))
    (is (nil? (:runs flat)))
    (is (nil? (:clusters flat)))
    (is (some #(= :rtl (:direction %)) (tl/result-runs flat)))
    (is (some #(= :virtual/tab (:glyph-id-kind %))
              (:glyphs (tl/paint-result flat))))
    (is (= (count (mapcat tl/line-clusters (:lines flat)))
           (count (mapcat tl/line-glyphs (:lines flat)))))))

(defn- zoom-refusal [layout-fn]
  (try
    (layout-fn (corpus-input {:zoom 0.001}))
    nil
    (catch clojure.lang.ExceptionInfo error
      {:message (ex-message error) :data (ex-data error)})))

(deftest t3-group-stats-and-legal-zoom-have-one-route
  (let [input (corpus-input {})
        flat (tl/layout input)
        mapped (oracle/layout input)
        template {:size 10 :r 1 :g 1 :b 1 :a 1 :container 17}
        expected-refusal
        {:message "Text zoom is outside Contract-T's legal material range."
         :data {:zoom 0.001 :legal-range [0.01 1000]}}]
    (testing "the draw-item template's semantic group reaches every line"
      (is (seq (tl/line-paint-draw-items flat template)))
      (is (every? #(= 17 (:container %))
                  (tl/line-paint-draw-items flat template))))
    (testing "unused input hashes are absent while hit-test source lines stay"
      (doseq [result [flat mapped]]
        (is (not (contains? (:stats result) :input-hash)))
        (is (seq (get-in result [:stats :source-lines])))))
    (testing "layout and oracle share the public legal zoom predicate"
      (is (tl/legal-zoom? 0.01))
      (is (tl/legal-zoom? 1000))
      (is (false? (tl/legal-zoom? 0.001)))
      (is (= expected-refusal (zoom-refusal tl/layout)))
      (is (= expected-refusal (zoom-refusal oracle/layout))))))
