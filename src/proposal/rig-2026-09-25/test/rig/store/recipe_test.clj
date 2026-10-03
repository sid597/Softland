(ns rig.store.recipe-test
  "The recipe executor (PLAN-tools-and-grammars.md 5, 6.3, 6.4, 7.1): what
  `parse-tool` refuses, a run over a matched row, a failed step as data,
  the loop check in stamp order, the run's name, and the revision reader
  as a capability behind the operator's repository ids (V-F13). No
  cluster; the revision steps read this repository through git."
  (:require [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [rig.store.envelope :as env]
            [rig.store.gate :as gate]
            [rig.store.recipe :as recipe]))

(def test-tool
  "The proof's test tool (5.1): counts the people a mention names, as a note."
  {:matches [:k :mention]
   :signature {:in :match :out #{:note} :rows? false}
   :permission [:mention-count :alice :alice]
   :recipe [{:name :count-note :do :emit :e [:in :e] :k :note
             :v [:map {:token [:str [:count [:in :v :persons]] " named"]}]}]})

(defn- parse [v] (recipe/parse-tool v gate/store-key?))
(defn- why [v] (:why (parse v)))
(def ^:private n1 [:alice :by-layer :offer #uuid "00000000-0000-7000-8000-000000000001"])
(def ^:private row {:fid [n1 0] :stamp 7 :e :e1 :k :mention :value {:persons #{:bob}}})

(deftest parse-tool
  (is (= {:ok {:matches [:k :mention] :out #{:note} :rows? false :permission [:mention-count :alice :alice]
               :recipe (:recipe test-tool)}}
         (parse test-tool)))
  (let [step (first (:recipe test-tool))
        with-step (fn [s] (assoc test-tool :recipe [s]))]
    (is (= :unknown-step (why (with-step (assoc step :do :shell)))))
    (is (= :bad-arguments (why (with-step (assoc step :v [:eval "(rm -rf)"])))) "an unknown formula")
    (is (= :bad-arguments (why (with-step (assoc step :k :mention)))) "an :emit of a key outside :out")
    (is (= :bad-arguments (why (with-step (assoc step :v [:got :later])))) "a [:got] of no earlier step")
    (is (= :bad-arguments (why (assoc test-tool :recipe [(assoc step :v [:got :count-note])])))
        "a step's [:got] of itself: only earlier steps")
    (is (nil? (why (assoc test-tool :recipe [step (assoc step :name :again :v [:got :count-note :v])])))
        "a [:got] of an earlier step")
    (is (= :bad-recipe (why (assoc test-tool :recipe (vec (for [i (range 17)] (assoc step :name (keyword (str "s" i)))))))) "17 steps")
    (is (nil? (why (assoc test-tool :recipe (vec (for [i (range 16)] (assoc step :name (keyword (str "s" i)))))))) "16 steps")
    (let [deep (fn [n] (reduce (fn [f _] [:count f]) [:lit 1] (range (dec n))))]
      (is (nil? (why (with-step (assoc step :v (deep 8))))) "a formula 8 deep")
      (is (= :bad-arguments (why (with-step (assoc step :v (deep 9))))) "a formula 9 deep"))
    (is (= :step-name-twice (why (assoc test-tool :recipe [step step]))))
    (doseq [k [:grammar :read/pattern :tool :permission :lease]]
      (is (= :writes-a-store-key (why (assoc-in test-tool [:signature :out] #{k}))) (str "an :out holding " k " (V-F7)")))
    (is (= :bad-signature (why (assoc-in test-tool [:signature :in] :all))))
    (is (= :bad-signature (why (update test-tool :signature dissoc :rows?))))
    (is (= :bad-pattern (why (assoc test-tool :matches [:k]))))
    (is (= :bad-permission (why (assoc test-tool :permission :alice))))
    (is (= :not-a-tool (why (assoc test-tool :run "code"))))
    (is (= :not-a-tool (why "a tool")))
    (testing "V-F13: a repository is a literal id, never a formula"
      (is (nil? (why (with-step {:name :u :do :revision/read-units :repo :softland :rev [:lit "HEAD"] :path [:lit "README.md"]}))))
      (is (= :bad-arguments (why (with-step {:name :u :do :revision/read-units :repo [:lit "/"] :rev [:lit "HEAD"] :path [:lit "x"]}))))
      (is (= :bad-arguments (why (with-step {:name :u :do :revision/read-units :repo :softland :rev [:lit "HEAD"] :path [:lit "x"] :cut :lines})))))))

(deftest run
  (let [tool (:ok (parse test-tool))]
    (is (= {:facts [{:e :e1 :k :note :v {:token "1 named"}}]} (recipe/run tool row)))
    (is (= {:facts [{:e :e2 :k :note :v {:token "2 named"}}]}
           (recipe/run tool (assoc row :e :e2 :value {:persons #{:alice :bob}}))))
    (testing "the formulas, total: a path into a missing place is nil"
      (is (= {:facts [{:e :e1 :k :note :v {:token " named"}}]} (recipe/run tool (assoc row :value "not a map")))))
    (testing "a failed step stops the run as data, and nothing is offered"
      (let [bad (:ok (parse (assoc-in test-tool [:recipe 0 :e] [:lit "not a keyword"])))]
        (is (= {:refused :step-failed :step 0 :error :bad-entity} (recipe/run bad row))))
      (let [nil-v (:ok (parse (assoc-in test-tool [:recipe 0 :v] [:in :v :nothing])))]
        (is (= {:refused :step-failed :step 0 :error :no-value} (recipe/run nil-v row)) "a nil value would be a retract")))
    (testing "every part of the match a formula may name"
      (let [t (:ok (parse (assoc-in test-tool [:recipe 0 :v] [:map {:fid [:in :fid] :stamp [:in :stamp] :k [:in :k]}])))]
        (is (= {:fid [n1 0] :stamp 7 :k :mention} (:v (first (:facts (recipe/run t row))))))))))

(deftest the-loop-check
  (let [tool (fn [m out] {:matches m :out out})
        run-ids (fn [r] (mapv first (:run r)))]
    (is (= {:run [] :refused {:t1 :tool-loop}} (recipe/loop-free [[:t1 (tool [:k :note] #{:note})]] gate/store-key?))
        "a tool matching its own output")
    (is (= [:ta] (run-ids (recipe/loop-free [[:ta (tool [:k :mention] #{:note})] [:tb (tool [:k :note] #{:mention})]] gate/store-key?)))
        "A before B: A runs, B would close the cycle")
    (is (= {:tb :tool-loop} (:refused (recipe/loop-free [[:ta (tool [:k :mention] #{:note})] [:tb (tool [:k :note] #{:mention})]] gate/store-key?))))
    (is (= [:tb] (run-ids (recipe/loop-free [[:tb (tool [:k :note] #{:mention})] [:ta (tool [:k :mention] #{:note})]] gate/store-key?)))
        "in the other stamp order the other one runs")
    (doseq [m [[:all] [:e :e1]]]
      (is (= {:x :tool-loop} (:refused (recipe/loop-free [[:x (tool m #{:note})]] gate/store-key?))) (str (pr-str m) " matches every key")))
    (doseq [k [:read/pattern :lease :grammar]]
      (is (= {:x :tool-loop} (:refused (recipe/loop-free [[:x (tool [:k k] #{:note})]] gate/store-key?)))
          (str "a pattern on the store key " k ": the runner writes those on every pass (V-F7)")))
    (is (= [:a :b :c] (run-ids (recipe/loop-free [[:a (tool [:k :x] #{:y})] [:b (tool [:k :y] #{:z})] [:c (tool [:kv :x 1] #{:w})]]
                                                 gate/store-key?)))
        "a chain with no cycle runs whole")
    (is (= [:a :b] (run-ids (recipe/loop-free [[:a (tool [:k :x] #{:y})] [:b (tool [:k :y] #{:z})] [:c (tool [:ek :e :z] #{:x})]]
                                              gate/store-key?)))
        "a longer cycle is closed by its last tool")))

(deftest run-name
  (let [tf [n1 0] mf [n1 1]
        a (recipe/run-name :alice tf mf)]
    (is (= a (recipe/run-name :alice tf mf)) "the same triple, the same name")
    (is (not= a (recipe/run-name :alice-hand tf mf)))
    (is (not= a (recipe/run-name :alice mf tf)))
    (is (not= a (recipe/run-name :alice tf [n1 2])))
    (is (env/valid-name? a))
    (is (= [:alice :by-layer :offer] (subvec a 0 3)))
    (is (= [8 2] ((juxt #(.version ^java.util.UUID %) #(.variant ^java.util.UUID %)) (nth a 3))) "a version 8 UUID")))

(def ^:private repo (str/trim (:out (sh/sh "git" "rev-parse" "--show-toplevel"))))
(def ^:private rig-456 "45627e45d412b2c4c1b1224594823adc47e59d99")
(def ^:private gate-path "src/proposal/rig-2026-09-25/src/rig/store/gate.clj")

(deftest the-revision-reader-as-a-capability
  (let [t (fn [steps] (:ok (parse (assoc test-tool :recipe steps))))
        units {:name :u :do :revision/read-units :repo :softland :rev [:lit rig-456] :path [:lit gate-path] :cut :forms}
        emit {:name :n :do :emit :e [:lit :forms] :k :note :v [:count [:got :u :units]]}
        config {:repos {:softland repo}}]
    (testing "a step binds the reader's units to its name, and a later step reads them"
      (let [r (recipe/run (t [units emit]) row config)]
        (is (= 1 (count (:facts r))) (pr-str r))
        (is (pos? (:v (first (:facts r)))) "the file's top-level forms, counted")))
    (testing "the span step"
      (let [r (recipe/run (t [{:name :s :do :revision/read-span :repo :softland :rev [:lit rig-456] :path [:lit gate-path]
                               :first [:lit 1] :last [:lit 2]}
                              {:name :n :do :emit :e [:lit :span] :k :note :v [:got :s :unit :position :lines]}])
                          row config)]
        (is (= [1 2] (:v (first (:facts r)))) (pr-str r))))
    (testing "an error from the reader comes back as data and stops the run"
      (let [r (recipe/run (t [(assoc units :rev [:lit "no-such-revision-6b"]) emit]) row config)]
        (is (= [:step-failed 0 :unknown-revision] [(:refused r) (:step r) (get-in r [:error :error])]) (pr-str r))))
    (testing "V-F13: a repository id the runner was not given reads nothing"
      (is (= {:refused :step-failed :step 0 :error {:error :unknown-repo :repo :softland}}
             (recipe/run (t [units emit]) row {})))
      (is (= {:refused :step-failed :step 0 :error {:error :unknown-repo :repo :softland}}
             (recipe/run (t [units emit]) row))
          "the default configuration names no repository"))))
