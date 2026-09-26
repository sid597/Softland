(ns rig.store.shape-test
  "The shape language (PLAN-tools-and-grammars.md 4.2, 7.1): every form
  accepts and refuses what the table says; the bounds at admission; the
  visit budget; and totality over the envelope's values. No cluster."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [rig.store.grammar :as grammar]
            [rig.store.shape :as shape]))

(defn- ok? [s v] (nil? (shape/check s v)))

(deftest every-form
  (testing "scalars"
    (is (every? #(ok? [:any] %) [nil 1 "x" :k {:a [1]} #{2}]))
    (is (ok? [:nil] nil))
    (is (not (ok? [:nil] false)))
    (is (and (ok? [:boolean] true) (ok? [:boolean] false) (not (ok? [:boolean] nil))))
    (is (and (ok? [:int] 7) (not (ok? [:int] 7.0)) (not (ok? [:int] "7"))))
    (is (and (ok? [:uuid] #uuid "00000000-0000-4000-8000-000000000000") (not (ok? [:uuid] "u"))))
    (is (and (ok? [:keyword] :a/b) (not (ok? [:keyword] (keyword "a b"))) (not (ok? [:keyword] "a")))
        "a readable keyword only")
    (is (and (ok? [:string] "") (ok? [:string 3] "abc") (not (ok? [:string 3] "abcd")) (not (ok? [:string] :s)))))
  (testing "enum, vector-of, set-of"
    (is (and (ok? [:enum :a 1 "x" nil] 1) (ok? [:enum :a 1 "x" nil] nil) (not (ok? [:enum :a 1] 1.0))))
    (is (ok? [:vector-of [:int] 0 2] []))
    (is (= [1] (shape/check [:vector-of [:int] 0 2] [1 "two"])) "the path to the first mismatch")
    (is (= [] (shape/check [:vector-of [:int] 0 2] [1 2 3])) "too many elements: the value itself")
    (is (= [] (shape/check [:vector-of [:int] 0 2] #{1})) "a set is not a vector")
    (is (ok? [:set-of [:keyword] 1 2] #{:bob}))
    (is (= [] (shape/check [:set-of [:keyword] 1 2] #{})) "fewer than min")
    (is (= [] (shape/check [:set-of [:keyword] 1 2] #{:a :b :c})))
    (is (= ["bob"] (shape/check [:set-of [:keyword] 1 2] #{"bob"})) "a set's element is its own path step"))
  (testing "map: required, optional, closed, open"
    (let [s [:map {:persons [:set-of [:keyword] 1 2] :token [:string]} {:optional #{:token}}]]
      (is (ok? s {:persons #{:bob}}))
      (is (ok? s {:persons #{:bob} :token "t"}))
      (is (= [:persons] (shape/check s {:token "t"})) "a required entry missing")
      (is (= [:other] (shape/check s {:persons #{:bob} :other 1})) "a closed map admits no other entry")
      (is (= [:persons :bob-str] (shape/check [:map {:persons [:map {:bob-str [:int]}]}] {:persons {:bob-str "x"}})))
      (is (ok? [:map {:persons [:set-of [:keyword] 1 2]} {:open? true}] {:persons #{:bob} :token "t"}))
      (is (= [] (shape/check s "not a map")))
      (is (ok? [:map {}] {})) (is (not (ok? [:map {}] {:a 1})))))
  (testing "or: the first branch that matches"
    (is (and (ok? [:or [:int] [:string]] "x") (ok? [:or [:int] [:string]] 1)))
    (is (= [] (shape/check [:or [:int] [:string]] :k))))
  (testing "a shape outside the language is a mismatch, never a throw"
    (is (= [] (shape/check [:no-such-form] 1)))
    (is (= [] (shape/check nil 1)))))

(deftest admission-bounds
  (let [nest (fn [n] (reduce (fn [s _] [:vector-of s 0 1]) [:int] (range (dec n))))]
    (is (nil? (shape/refusal (nest 8))) "8 deep is admitted")
    (is (= :too-deep (shape/refusal (nest 9))) "9 deep is refused")
    (is (nil? (shape/refusal (into [:or] (repeat 8 [:int])))))
    (is (= :not-a-shape (shape/refusal (into [:or] (repeat 9 [:int])))) "9 [:or] branches")
    (is (nil? (shape/refusal (into [:enum] (range 64)))))
    (is (= :too-many-values (shape/refusal (into [:enum] (range 65)))) "65 enum values")
    (is (= :too-many-values (shape/refusal [:map {:a (into [:enum] (range 65))}])) "an enum inside a map")
    (is (nil? (shape/refusal [:map (into {} (for [i (range 255)] [(keyword (str "k" i)) [:int]]))])) "256 nodes")
    (is (= :too-many-nodes (shape/refusal [:map (into {} (for [i (range 256)] [(keyword (str "k" i)) [:int]]))])) "257 nodes")
    (doseq [bad [nil [] [:int 1] [:string -1] [:string 1048577] [:vector-of [:int] 2 1] [:set-of [:int] 0 65537]
                 [:map {"a" [:int]}] [:map {:a [:int]} {:optional #{:b}}] [:map {:a [:int]} {:open? 1}]
                 [:enum] [:enum 1/3] [:or] ["any"] '(:any)]]
      (is (= :not-a-shape (shape/refusal bad)) (pr-str bad)))))

(deftest the-budget
  (testing "a nested [:or] over a wide value stops at 65,536 visits and refuses as a mismatch"
    (let [s [:vector-of [:or [:string] [:string] [:string] [:string] [:string] [:string] [:string] [:int]] 0 65536]]
      (is (nil? (shape/refusal s)))
      (is (= :budget (shape/check s (vec (repeat 10000 7))))
          "10,000 elements x 9 visits each passes 65,536")
      (is (nil? (shape/check s (vec (repeat 7000 7)))) "7,000 x 9 = 63,001 visits fit"))))

(def ^:private gen-leaf
  (gen/one-of [(gen/return nil) gen/boolean gen/small-integer gen/large-integer gen/string-ascii
               gen/keyword gen/keyword-ns gen/uuid (gen/double* {:infinite? false :NaN? false})]))

(def ^:private gen-value
  (gen/recursive-gen
   (fn [inner] (gen/one-of [(gen/vector inner 0 4) (gen/set inner {:max-elements 4})
                            (gen/map inner inner {:max-elements 4})]))
   gen-leaf))

(deftest total-over-values
  (let [r (tc/quick-check 1500
                          (prop/for-all [s gen-value v gen-value]
                            (and (let [x (shape/refusal s)] (or (nil? x) (keyword? x)))
                                 (let [x (shape/check s v)] (or (nil? x) (vector? x) (= :budget x)))
                                 (let [x (grammar/refusal {:shape s :subjects-at v :opaque v :index v})]
                                   (or (nil? x) (keyword? x)))
                                 (let [x (grammar/refusal v)] (or (nil? x) (keyword? x)))))
                          :seed 26092026)]
    (is (:pass? r) (pr-str (dissoc r :result-data)))))
