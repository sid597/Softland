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

(deftest a-complete-run-keeps-its-continuation-and-an-appended-item-resumes-alone
  (let [calls (atom 0)
        t (assoc-in table [:identity :run] (fn [args _] (swap! calls inc) (get args :value (:alternative args))))
        done (executor/run sum-record {} t)
        _ (is (= 3 @calls))
        more (executor/resume (:continuation done) t {:record (update-in sum-record [:roots :numbers] conj {:n 7})})]
    (is (= :complete (:status done)))
    (is (= 3 (get-in done [:continuation :at])))
    (is (= 26 (get-in more [:results :sum])))
    (is (= 4 @calls) "only the appended item ran")
    (is (= 4 (count (:history more))))
    (is (= :complete (:status (executor/resume (:continuation done) t {}))))
    (is (= 4 @calls) "nothing appended runs nothing")
    (is (= :recipe-differs (:reason (executor/resume (:continuation done) t {:scope {:factor 3}}))))
    (is (= (:results done) (:results (executor/resume (executor/decode (executor/encode (:continuation done)) t) t {}))))))

(deftest inputs-chain-through-tools-and-stale-scope-or-items-refuse
  (let [retained (fn [r out] (assoc (get-in r [:results out]) :subject (get-in r [:subjects out])))
        a {:roots {:n 2} :program {:steps [{:out :v :op :identity :args {:value {:n [:get :n]}}}] :return {:out [:get :v]}}}
        b {:roots {:inputs {:x {:from {:record "a" :output :out}}}}
           :program {:steps [{:out :v :op :identity :args {:value {:doubled [:* 2 [:get :inputs :x :n]] :zoom [:get :zoom]}}}]
                     :return {:out [:get :v]}}}
        c {:roots {:inputs {:y {:from {:record "b" :output :out}}}}
           :program {:steps [{:out :v :op :identity :args {:value {:seen [:get :inputs :y :doubled]}}}] :return {:out [:get :v]}}}
        ra (executor/run a {} table)
        rb (executor/run b {:zoom 1 :inputs {:x (retained ra :out)}} table {:records {"a" a}})
        run-c (fn [scope records] (executor/run c scope table {:records records}))]
    (is (= :complete (:status rb)))
    (is (= {:seen 4} (get-in (run-c {:zoom 1 :inputs {:y (retained rb :out)}} {"a" a "b" b}) [:results :out]))
        "two levels deep; the middle record read its inputs outside a loop")
    (is (= :subject (:reason (run-c {:zoom 2 :inputs {:y (retained rb :out)}} {"a" a "b" b}))) "a scope root the consumer names differently")
    (is (= :subject (:reason (run-c {:zoom 1 :inputs {:y (retained rb :out)}} {"b" b}))) "the grand producer is unknown")
    (is (= :subject (:reason (run-c {:zoom 1 :inputs {:y (retained rb :out)}}
                                    {"a" (assoc-in a [:program :steps 0 :args :value :n] 5) "b" b})))
        "the resolved input came from another program"))
  (let [retained (fn [r out] (assoc (get-in r [:results out]) :subject (get-in r [:subjects out])))
        summing {:roots {:events [{:x 1} {:x 2}]}
                 :program {:each {:items [:get :events] :item :e :fields [:x] :state {:sum 0} :steps []
                                  :next {:sum [:+ [:get :state :sum] [:get :e :x]]}}
                           :return {:sum {:total [:get :state :sum]}}}}
        r (executor/run summing {} table)
        consumer {:roots {:inputs {:s {:from {:record "l" :output :sum}}}}
                  :program {:steps [{:out :v :op :identity :args {:value [:get :inputs :s :total]}}] :return {:v [:get :v]}}}]
    (is (= {:events [{:x 1} {:x 2}]} (get-in r [:subjects :sum :item-roots])) "the items' roots ride beside the recipe")
    (is (= #{} (set (keys (get-in r [:subjects :sum :recipe :roots])))) "and stay out of it")
    (is (= 3 (get-in (executor/run consumer {:inputs {:s (retained r :sum)}} table {:records {"l" summing}}) [:results :v])))
    (is (= :subject (:reason (executor/run consumer {:inputs {:s (retained r :sum)}} table
                                           {:records {"l" (update-in summing [:roots :events] conj {:x 3})}})))
        "a value summed over other items is not this record's subject")))

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
