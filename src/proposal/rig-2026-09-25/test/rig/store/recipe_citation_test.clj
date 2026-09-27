(ns rig.store.recipe-citation-test
  "The vocabulary the citation needed (PLAN-citation.md section 3), pure: the
  four tools of tools/citation.edn parse and pass the loop check; `[:vec]`,
  `[:in :replaces]` and `[:in :run]`; `:read` through a read function, a
  nil pattern reading nothing, a refused or partial read failing the run;
  `:emit-all` checked against the signature; `:stand-on`."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [clojure.walk :as walk]
            [rig.store.gate :as gate]
            [rig.store.recipe :as recipe]))

(def tools
  (walk/postwalk #(if (= :the-layer %) :l %) (edn/read-string (slurp "tools/citation.edn"))))

(def nm [:l :by-layer :offer #uuid "01920000-0000-7000-8000-00000000000a"])

(deftest the-four-tools-parse
  (let [parsed (mapv (fn [t] [(:e t) (recipe/parse-tool (:v t) gate/store-key?)]) tools)]
    (doseq [[id p] parsed] (is (contains? p :ok) (str id " " (pr-str p))))
    (is (= [:material-reader :cite-finder :cite-binder :cite-marker] (mapv first parsed)))
    (let [{:keys [run refused]} (recipe/loop-free (vec (map-indexed (fn [i [_ p]] [[nm i] (:ok p)]) parsed))
                                                  gate/store-key?)]
      (is (= 4 (count run)) "no cycle: every tool runs")
      (is (empty? refused)))))

(defn- tool [recipe out] {:matches [:k :x] :out out :rows? false :permission [:t :l :l] :recipe recipe})

(def row {:e :e1 :k :x :value {:a 1} :fid [nm 0] :stamp 7 :replaces [nm 3] :run nm})

(deftest formulas
  (is (= {:facts [{:e :e1 :k :y :v [:latest :e1 :material/file]}] :stood-on {}}
         (recipe/run (tool [{:name :o :do :emit :e [:in :e] :k :y :v [:vec :latest [:in :e] :material/file]}] #{:y}) row)))
  (is (= [nm 3] (get-in (recipe/run (tool [{:name :o :do :emit :e [:in :e] :k :y :v [:in :replaces]}] #{:y}) row)
                        [:facts 0 :v])))
  (is (= nm (get-in (recipe/run (tool [{:name :o :do :emit :e [:in :e] :k :y :v [:in :run]}] #{:y}) row)
                    [:facts 0 :v]))))

(deftest reading-through-the-exit
  (let [seen (atom [])
        read (fn [p] (swap! seen conj p) {:rows [{:fid [nm 1] :stamp 5 :value "v"}] :mark :complete :entry nm})
        t (tool [{:name :r :do :read :pattern [:vec :latest [:in :e] :material/file]}
                 {:name :o :do :emit :e [:in :e] :k :y :v [:got :r :rows 0 :value]}]
                #{:y})]
    (is (= "v" (get-in (recipe/run t row {:read read}) [:facts 0 :v])))
    (is (= [[:latest :e1 :material/file]] @seen) "the pattern the step built")
    (testing "a pattern holding nil reads nothing"
      (reset! seen [])
      (let [t (tool [{:name :r :do :read :pattern [:vec :dependents [:in :nothing]]}
                     {:name :o :do :emit :e [:in :e] :k :y :v [:count [:got :r :rows]]}]
                    #{:y})]
        (is (= 0 (get-in (recipe/run t row {:read read}) [:facts 0 :v])))
        (is (empty? @seen))))
    (testing "a refused read, a partial one, no read function: the run fails and offers nothing"
      (is (= :read-refused (get-in (recipe/run t row {:read (constantly {:refused :not-visible})}) [:error :error])))
      (is (= :partial-read (get-in (recipe/run t row {:read (constantly {:rows [] :mark :partial})}) [:error :error])))
      (is (= :no-read (get-in (recipe/run t row {}) [:error :error]))))))

(deftest emitting-and-standing
  (let [t (fn [facts] (tool [{:name :a :do :emit-all :facts [:lit facts]}] #{:y :z}))]
    (is (= {:facts [{:e :e1 :k :y :v 1} {:e :e2 :k :z :v 2 :replaces [nm 0]}] :stood-on {}}
           (recipe/run (t [{:e :e1 :k :y :v 1} {:e :e2 :k :z :v 2 :replaces [nm 0]}]) row)))
    (is (= :key-not-in-signature (:error (recipe/run (t [{:e :e1 :k :w :v 1}]) row))))
    (is (= :no-value (:error (recipe/run (t [{:e :e1 :k :y :v nil}]) row))))
    (is (= :bad-entity (:error (recipe/run (t [{:e "e1" :k :y :v 1}]) row))))
    (is (= :bad-replaces (:error (recipe/run (t [{:e :e1 :k :y :v 1 :replaces [:nope 0]}]) row))))
    (is (= :unknown-part (:error (recipe/run (t [{:e :e1 :k :y :v 1 :layer :other}]) row))))
    (is (= :not-a-list (:error (recipe/run (t {:e :e1}) row)))))
  (let [t (fn [pairs] (tool [{:name :s :do :stand-on :pairs [:lit pairs]}
                             {:name :o :do :emit :e [:in :e] :k :y :v 1}]
                            #{:y}))]
    (is (= {[nm 1] 5 [nm 2] 6} (:stood-on (recipe/run (t [[[nm 1] 5] [[nm 2] 6]]) row))))
    (is (= :bad-pair (:error (recipe/run (t [[[nm 1] "5"]]) row))))))
