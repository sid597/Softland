(ns app.anatomy-test
  "smalltalk-ui-vm P1 permanent post-invariant: the material interpreter must
   reproduce the browser-generated legacy trees after only the contract-named
   interpreter provenance is normalized."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [clojure.walk :as walk]
            [app.client.workspace.face-assembly :as face-assembly]
            [app.client.workspace.face-primitives :as face-primitives]
            [app.shared.anatomy-material :as anatomy]))

(def ^:private fixture-dir
  (io/file "test/app/fixtures/anatomy"))

(def ^:private falsifier-fixture
  (io/file fixture-dir "counterexamples/machine-hover-normal-priority.edn"))

(defn- fixtures
  []
  (->> (.listFiles fixture-dir)
       (filter #(str/ends-with? (.getName %) ".edn"))
       (sort-by #(.getName %))
       (mapv #(edn/read-string (slurp %)))))

(defn- all-numbers-double
  [form]
  (walk/postwalk #(if (number? %) (double %) %) form))

(defn- normalize-interpreted-tree
  ([tree] (normalize-interpreted-tree tree true))
  ([tree root?]
   (let [data
         (cond-> (dissoc (:data tree) :assembly/src-path)
           root?
           (dissoc :view-instance
                   :assembly/content-h
                   :assembly/apply-report))
         data (when (seq data) data)]
     (-> tree
         (assoc :data data)
         (update :children
                 (fn [children]
                   (mapv #(normalize-interpreted-tree % false) children)))
         all-numbers-double))))

(defn- interpreter-provenance?
  [tree]
  (let [nodes (tree-seq #(seq (:children %)) :children tree)]
    (and
     (every? #(some? (get-in % [:data :assembly/src-path])) nodes)
     (every? #(contains? (:data tree) %)
             [:view-instance
              :address
              :assembly/content-h
              :assembly/apply-report]))))

(defn- compile-wear
  [anatomy-wear]
  (face-assembly/compile-assembly
   face-primitives/registry
   (anatomy/assembly-for anatomy-wear)))

(defn- render-inputs
  [{:keys [unit-id metrics wears anatomy-view]} anatomy-wear]
  (let [parts (anatomy/expand-parts
               (:anatomy/parts anatomy-wear)
               (:anatomy/defs anatomy-wear))
        viewport (:viewport metrics)
        data (anatomy/apply-data parts wears anatomy-view unit-id)]
    (face-assembly/apply-assembly
     (compile-wear anatomy-wear)
     data
     {:view-instance [:vi :ground-block unit-id]
      :address unit-id
      :geom
      {:viewport-w (:w viewport)
       :viewport-h (:h viewport)
       :content-w (:block-w anatomy-view)
       :font-size (:font-size metrics)
       :char-advance (:char-advance metrics)
       :line-height (:line-h metrics)}})))

(deftest generated-goldens-are-the-post-invariant
  (let [cases (fixtures)
        expected-names
        #{:plain-user
          :focused-caret
          :selection
          :machine-wrap-headers-folds
          :machine-selection
          :hover-box
          :group-selection
          :refusal
          :notice
          :boundary
          :gold-and-silver
          :composition-conflict
          :empty-text
          :hard-break-and-multiline
          :placement-derived}]
    (is (= expected-names (set (map :name cases)))
        "the browser-generated census covers every contract-named edge")
    (doseq [{:keys [name inputs tree]} cases]
      (testing (str "browser-generated legacy tree: " name)
        (let [anatomy-wear (:anatomy (:wears inputs))
              interpreted (render-inputs inputs anatomy-wear)]
          (is (not (face-assembly/error? (compile-wear anatomy-wear))))
          (is (interpreter-provenance? interpreted)
              "the exact comparison may remove provenance only after proving it")
          (is (= (all-numbers-double tree)
                 (normalize-interpreted-tree interpreted))))))))

(deftest falsifier-machine-hover-preserves-live-composition-order
  (let [{:keys [base input-overrides expected]}
        (edn/read-string (slurp falsifier-fixture))
        {:keys [inputs]}
        (first (filter #(= base (:name %)) (fixtures)))
        inputs
        (-> inputs
            (merge (dissoc input-overrides :anatomy-view))
            (update :anatomy-view merge (:anatomy-view input-overrides)))
        anatomy-wear (:anatomy (:wears inputs))
        interpreted (render-inputs inputs anatomy-wear)
        decoration-ids
        (->> (:children interpreted)
             (filter #(= :block/decorations
                         (get-in % [:data :material/slot])))
             (mapv :id))
        priorities
        [(get-in inputs [:wears :provenance :facet-master/priority])
         (get-in inputs [:wears :attention :facet-master/priority])]]
    (is (= (:priorities expected) priorities))
    (is (= (:decoration-child-ids expected) decoration-ids)
        "the normal priority order is rail then box")
    (is (not-any? #(= :error-card (:type %))
                  (:children interpreted))
        "ordinary distinct priorities do not invent conflict lint")))

(deftest grammar-is-closed-and-refusals-teach
  (testing "the material vocabulary is a subset of the code registry"
    (is (empty?
         (set/difference anatomy/part-prim-vocabulary
                         (set (keys face-primitives/registry))))))
  (testing "unknown primitive and presence values name the offender and law"
    (let [bad-prim (assoc-in anatomy/default-form
                             [:anatomy/parts 1 :part/prim]
                             :arbitrary-code-address)
          bad-when (assoc-in anatomy/default-form
                             [:anatomy/parts 1 :part/when]
                             :whenever-the-agent-wants)
          prim-error (first (:errors (anatomy/compile-form bad-prim)))
          when-error (first (:errors (anatomy/compile-form bad-when)))]
      (is (= :anatomy/part-prim-invalid (:type prim-error)))
      (is (= :arbitrary-code-address (:actual prim-error)))
      (is (= anatomy/part-prim-vocabulary (:legal prim-error)))
      (is (= :anatomy/part-presence-invalid (:type when-error)))
      (is (= :whenever-the-agent-wants (:actual when-error)))
      (is (= anatomy/presence-vocabulary (:legal when-error)))))
  (testing "a missing sub-anatomy definition refuses at the material boundary"
    (let [form (update anatomy/default-form :anatomy/parts conj
                       {:part/id :nested
                        :part/prim :sub-anatomy
                        :part/when :always
                        :part/order 130
                        :part/props {}
                        :part/def :missing})
          error (some #(when (= :anatomy/sub-anatomy-missing-def (:type %)) %)
                      (:errors (anatomy/compile-form form)))]
      (is (= [:missing] (:actual error)))
      (is (= #{} (:legal error))))))

(deftest one-level-sub-anatomy-expands-through-the-same-interpreter
  (let [child {:part/id :nested-notice
               :part/prim :block-notice
               :part/when :always
               :part/order 1
               :part/props
               {:notice "nested"
                :refusal nil
                :line-count 1
                :w 120
                :line-h 27
                :font-size 19}}
        form (-> anatomy/default-form
                 (update :anatomy/parts conj
                         {:part/id :nested
                          :part/prim :sub-anatomy
                          :part/when :always
                          :part/order 130
                          :part/props {}
                          :part/def :notice})
                 (assoc :anatomy/defs {:notice [child]}))
        result (anatomy/compile-form form)
        wear (assoc (:material result)
                    :facet-master/revision-id "test:sub-anatomy")
        parts (anatomy/expand-parts (:anatomy/parts wear)
                                   (:anatomy/defs wear))]
    (is (:valid? result))
    (is (some #(= :nested/nested-notice (:part/id %)) parts))
    (is (not (face-assembly/error? (compile-wear wear))))))

(deftest interpreter-is-total-and-anatomy-identity-is-slow
  (testing "malformed arrangement reaches a visible error card, never a throw"
    (let [compiled
          (face-assembly/compile-assembly
           face-primitives/registry
           {:assembly/name "bad-anatomy"
            :assembly/grammar 0
            :root {:prim :not-registered}})
          tree
          (face-assembly/apply-assembly
           compiled {} {:view-instance :bad
                        :address "bad"
                        :geom {:content-w 300}})]
      (is (face-assembly/error? compiled))
      (is (= :error-card (:type tree)))
      (is (true? (get-in tree [:data :assembly/error])))))
  (testing "the cache key contains only master + revision identity"
    (let [a (assoc anatomy/code-floor :facet-master/revision-id "rev:a")
          b (assoc anatomy/code-floor :facet-master/revision-id "rev:b")]
      (is (= [anatomy/master-id "rev:a"] (anatomy/compile-cache-key a)))
      (is (not= (anatomy/compile-cache-key a)
                (anatomy/compile-cache-key b)))))
  (testing "revision flips preserve caret, focus, selection, and structural ids"
    (let [{:keys [inputs]} (first (filter #(= :selection (:name %))
                                          (fixtures)))
          a (assoc (:anatomy (:wears inputs))
                   :facet-master/revision-id "rev:a")
          b (assoc a :facet-master/revision-id "rev:b")
          tree-a (normalize-interpreted-tree (render-inputs inputs a))
          tree-b (normalize-interpreted-tree (render-inputs inputs b))]
      (is (= (:view inputs) {:text "alpha\nbeta"
                             :caret nil
                             :focused? true
                             :refusal nil
                             :selection [1 8]}))
      (is (= tree-a tree-b)))))

(deftest w8-defers-material-rebuild-during-a-pointer-gesture
  (let [source (slurp "src/app/client/workspace/ground.cljs")]
    (is (true? (str/includes? source "!pending-material-rederive?")))
    (is (true? (str/includes? source "(= :idle (:phase @!pointer))")))
    (is (true? (str/includes? source
                              "preview ended: served base changed")))
    (is (true? (str/includes? source
                              "(reset! !pending-material-rederive? false)")))))
