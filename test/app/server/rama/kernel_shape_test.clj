(ns app.server.rama.kernel-shape-test
  "Loadability + shape test for the shared kernel contract artifact.
   KERNEL-SHAPE's whole value is being inspectable Clojure data; that is a
   property only proven by requiring the namespace and reading the data."
  (:require [app.server.rama.kernel :as kernel]
            [clojure.test :refer [deftest is testing]]))

(def five-kernels
  #{:text-kernel :space-kernel :compute-kernel :transcript-kernel :llm-kernel})

(deftest kernel-shape-loads-and-is-inspectable-test
  (testing "the defkernel form produced inspectable data"
    (is (= 'KERNEL-SHAPE (:kernel/name kernel/KERNEL-SHAPE)))
    (is (map? (:kernel/shape kernel/KERNEL-SHAPE))))
  (let [shape (:kernel/shape kernel/KERNEL-SHAPE)]
    (testing "the documented fields are present"
      (is (every? (set (keys shape))
                  [:ingress-partitioner :intent-depot :claim-depot
                   :observation-depot :control-depot :task-global-executor
                   :cross-module-wires :interpret-fn :materialize-fns
                   :pstate-spec :projections])))
    (testing "every always-present field carries examples from all five committed instances"
      (doseq [field [:ingress-partitioner :intent-depot :interpret-fn :materialize-fns]]
        (is (= five-kernels (set (keys (get-in shape [field :examples]))))
            (str field " :examples must cover the five kernels"))))
    (testing "pstate counts cover the five kernels"
      (is (= five-kernels (set (keys (get-in shape [:pstate-spec :counts]))))))
    (testing "optional depots declare where they are present"
      (is (= #{:compute-kernel :transcript-kernel :llm-kernel}
             (set (get-in shape [:claim-depot :present-in]))
             (set (get-in shape [:observation-depot :present-in]))))
      (is (= [:llm-kernel] (get-in shape [:control-depot :present-in]))))))
