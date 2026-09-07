(ns app.client.engine.executor-test
  (:require [app.client.engine.executor :as executor]
            [app.client.engine.value-bytes :as vb]
            [app.client.engine.surface :as surface]
            [clojure.test :refer [deftest is testing]]))

(def table {:vocabulary "executor test 1"
            :identity {:args [:value :alternative] :needs [[:value :alternative]]
                       :run (fn [args _] (get args :value (:alternative args)))}})

(def sum-record
  {:roots {:numbers [{:n 1 :unused 7} {:n 2} {:n 3}] :factor 2}
   :program {:each {:items [:get :numbers] :item :number :fields [:n]
                    :state {:sum 0 :dropped true}
                    :steps [{:out :term :op :identity :args {:value [:* [:get :factor] [:get :number :n]]}}]
                    :next {:sum [:+ [:get :state :sum] [:get :term]]}}
             :return {:sum [:get :state :sum]}}})

(deftest one-language-and-explicit-state
  (let [r (executor/run sum-record {} table)]
    (is (= :complete (:status r)) (pr-str r))
    (is (= 12 (get-in r [:results :sum])))
    (is (= {:sum 12} (:state r)) "next replaces, never merges")
    (is (= 3 (count (:history r))))
    (is (= r (executor/run sum-record {} table)) "no clock")
    (is (= 2 (get (:reads r) [:factor])))
    (is (= #{:factor} (set (keys (:roots (executor/recipe sum-record {}))))))
    (is (= #{[:size] [:p]} (executor/references [:* [:get :size] [:get :p]])))))

(deftest admission-and-errors-stop-all-later-work
  (let [calls (atom 0) t (assoc-in table [:identity :run] (fn [args _] (swap! calls inc) (:value args)))
        record {:roots {:root 1}
                :program {:steps [{:out :ok :op :identity :args {:value 1}}] :return {:v [:get :ok]}}}]
    (doseq [[edit reason] [[#(assoc-in % [:program :steps 0 :op] :absent) :missing-capability]
                           [#(assoc-in % [:program :steps 0 :args :typo] 1) :unconsumed-argument]
                           [#(assoc-in % [:program :steps 0 :args] {}) :missing-argument]
                           [#(assoc-in % [:program :steps 0 :out] :root) :shadows-root]
                           [#(assoc-in % [:program :steps 0 :each] []) :one-loop]]]
      (is (= reason (:reason (executor/run (edit record) {} t)))))
    (is (zero? @calls))
    (is (= :error (:status (executor/run (assoc-in record [:program :steps 0 :args :value] [:get :absent]) {} t))))
    (is (= :ok (:step (executor/run (assoc-in record [:program :steps 0 :args :value] [:get :absent]) {} t))))
    (is (zero? @calls))
    (is (= :complete (:status (executor/run (assoc-in record [:program :steps 0 :args] {:alternative 3}) {} t)))))
  (is (= :unknown-field (:reason (executor/run (assoc-in sum-record [:program :each :fields] []) {} table))))
  (is (= :executor/missing-field (get-in (executor/run (assoc-in sum-record [:roots :numbers 1] {}) {} table) [:data :error-type])))
  (doseq [expression [[:get :missing] [:pow 2] [:unknown 1] [:sqrt -1] [:/ 1 0]]]
    (is (thrown? Exception (executor/evaluate expression {})))))

(deftest checkpoint-is-independent-of-unconsumed-fields-and-future-items
  (let [c (:continuation (executor/run sum-record {} table {:until 1}))
        loaded (executor/decode (executor/encode c) table)
        r (executor/resume loaded table {})]
    (is (vb/equal? c loaded))
    (is (= (:results (executor/run sum-record {} table)) (:results r)))
    (is (= (:history (executor/run sum-record {} table)) (:history r)))
    (is (= :complete (:status (executor/resume c table {:record (assoc-in sum-record [:roots :numbers 0 :unused] 99)}))))
    (is (= 26 (get-in (executor/resume c table {:record (update-in sum-record [:roots :numbers] conj {:n 7})}) [:results :sum])))
    (is (= :consumed-items-differ (:reason (executor/resume c table {:record (assoc-in sum-record [:roots :numbers 0 :n] 7)}))))
    (is (= :recipe-differs (:reason (executor/resume c table {:scope {:factor 3}}))))
    (is (= :load (:reason (executor/decode (executor/encode c) (assoc table :vocabulary "changed")))))
    (is (= :load (:reason (executor/decode (vb/encode (assoc c :schema "changed")) table))))))

(deftest subjects-start-at-return-and-reach-pre-loop-producers
  (let [record {:roots {:tool {:radius 150} :unused 8}
                :program {:steps [{:out :region :op :identity :args {:value [:get :tool :radius]}}]
                          :return {:region [:get :region]}}}
        a (executor/run record {} table)
        b (executor/run (assoc-in record [:roots :tool :radius] 140) {} table)]
    (is (not= (:subjects a) (:subjects b)))
    (is (= {:tool {:radius 150}} (get-in a [:subjects :region :recipe :roots])))
    (is (= [] (get-in a [:subjects :region :consumed])))
    (is (= [:unused] (:unread a))))
  (let [read {:status :resolved :color [0.5 0 0 0.5] :snapshot :ignored}
        record {:roots {:read read} :program {:steps [{:out :sample :op :identity :args {:value [:get :read]}}
                                                      {:out :color :op :identity :args {:value [:get :sample :color]}}]
                                            :return {:color [:get :color]}}}
        r (executor/run record {} table)]
    (is (= [(dissoc read :snapshot)] (get-in r [:subjects :color :reads])))))

(deftest expression-vocabulary-and-lazy-branches
  (doseq [[expression expected] [[[:+ 2 [:* 3 4]] 14]
                                [[:pow 2 [:pow 3 2]] 512.0]
                                [[:/ 10 4] 2.5]
                                [[:max 1 3] 3]
                                [[:clamp 2 0 0.5] 0.5]
                                [[:step 0.5 0.7] 1.0]
                                [[:smoothstep 0 1 0.5] 0.5]
                                [[:mix 1 2 0.5] 1.5]
                                [[:floor 1.9] 1.0]
                                [[:exp 0] 1.0]
                                [[:if true 7 [:get :missing]] 7]
                                [[:literal [:unknown :data]] [:unknown :data]]]]
    (is (= expected (executor/evaluate expression {}))))
  (is (= #{[:yes] [:no]} (executor/references [:if true [:get :yes] [:get :no]])))
  (is (= {:path [1 2] :label "source.x"}
         (executor/evaluate {:path [1 2] :label "source.x"} {}))))
