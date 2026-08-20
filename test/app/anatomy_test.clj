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
            [app.shared.anatomy-material :as anatomy]
            [app.shared.invocation-material :as invocation]
            [app.shared.material-portal :as portal]))

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

(deftest p2-invocation-master-and-agent-candidate-teach
  (testing "the installed CLI subset is a closed, visible material vocabulary"
    (is (:valid? (invocation/compile-form invocation/default-form)))
    (doseq [[k bad legal]
            [[:invocation/effort "ultra" invocation/effort-vocabulary]
             [:invocation/precontext "whole-world"
              invocation/precontext-vocabulary]]]
      (let [result (invocation/compile-form
                    (assoc invocation/default-form k bad))
            error (first (:errors result))]
        (is (false? (:valid? result)))
        (is (= [k] (:path error)))
        (is (= bad (:actual error)))
        (is (= legal (:legal error))))))
  (testing "visible defaults are a candidate revision, never a seed mutation"
    (let [candidate
          (anatomy/invocation-candidate-form anatomy/default-form)
          compiled (anatomy/compile-form candidate)
          seed-ids (set (map :part/id anatomy/seed-parts))
          candidate-ids (set (map :part/id (:anatomy/parts candidate)))]
      (is (:valid? compiled))
      (is (empty?
           (set/intersection
            seed-ids
            (set (map :part/id anatomy/invocation-visible-parts)))))
      (is (set/subset?
           (set (map :part/id anatomy/invocation-visible-parts))
           candidate-ids))
      (doseq [k [:invocation/model
                 :invocation/effort
                 :invocation/precontext]]
        (is (some
             (fn [part]
               (= [:wear :invocation k]
                  (get-in part [:part/props :notice])))
             anatomy/invocation-visible-parts))))))

(deftest p2-edit-to-candidate-is-one-teaching-boundary
  (let [valid
        (anatomy/edit-candidate
         anatomy/default-form
         {:edit/op :retune-props
          :part/id :root
          :part/props {:wrap-col [:view :wrap-col]}})
        bad-op
        (anatomy/edit-candidate
         anatomy/default-form
         {:edit/op :execute-code :part/id :root})
        bad-primitive
        (anatomy/edit-candidate
         anatomy/default-form
         {:edit/op :add
          :part {:part/id :bad
                 :part/prim :call-arbitrary-function
                 :part/when :always
                 :part/order 130
                 :part/props {}}})]
    (is (= :candidate (:status valid)))
    (is (string? (:source valid)))
    (is (:valid? (anatomy/compile-source (:source valid))))
    (is (= :anatomy/edit-op-invalid
           (get-in bad-op [:errors 0 :type])))
    (is (= anatomy/edit-ops
           (get-in bad-op [:errors 0 :legal])))
    (is (= :anatomy/part-prim-invalid
           (get-in bad-primitive [:errors 0 :type])))
    (is (= :call-arbitrary-function
           (get-in bad-primitive [:errors 0 :actual])))
    (is (= anatomy/part-prim-vocabulary
           (get-in bad-primitive [:errors 0 :legal])))))

(deftest p2-composition-section-carries-all-three-strata-and-inverse-paths
  (let [candidate-form
        (anatomy/invocation-candidate-form anatomy/default-form)
        compiled (anatomy/compile-form candidate-form)
        wear (merge (:material compiled)
                    {:facet-master/id anatomy/master-id
                     :facet-master/facet :anatomy
                     :facet-master/grammar (:grammar compiled)
                     :facet-master/revision-id "rev:anatomy"
                     :facet-master/floor? false})
        wears {:anatomy wear
               :invocation invocation/code-floor}
        section (portal/composition-section
                 {:anatomy-wear wear :wears wears})]
    (is (= (count (:anatomy/parts candidate-form))
           (:composition/row-count section)))
    (is (= anatomy/master-id (:composition/master-id section)))
    (is (:valid?
         (anatomy/compile-source
          (:composition/invocation-candidate-source section))))
    (doseq [[i row] (map-indexed vector (:composition/rows section))]
      (is (= [:material :softland-code :floor]
             (mapv :stratum/id (:part/strata row))))
      (is (= [:green :amber :red]
             (mapv :stratum/color (:part/strata row))))
      (is (= (if (zero? i)
               [:root]
               [:root :children (dec i) :template])
             (:part/source-path row)))
      (is (= portal/primitive-source-path
             (get-in row [:part/strata 1 :code/src-path]))))
    (let [model-row
          (first
           (filter #(= :invocation-model (:part/id %))
                   (:composition/rows section)))]
      (is (= "sonnet"
             (get-in model-row
                     [:part/strata 0 :material/worn-values
                      :notice :wear/value])))
      (is (= [:wear :invocation :invocation/model]
             (get-in model-row
                     [:part/strata 0 :material/worn-values
                      :notice :wear/ref]))))))

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
