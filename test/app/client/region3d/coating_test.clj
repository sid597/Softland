(ns app.client.region3d.coating-test
  (:require [clojure.test :refer [deftest is]]
            [app.client.region3d.records :as records]
            [app.client.region3d.support :as support]
            [app.client.region3d.coating :as coating]
            [app.client.region3d.capabilities :as capabilities]
            [app.client.engine.executor :as executor]
            [app.client.engine.value-bytes :as vb]))

(deftest retained-sequence-and-withheld-merge
  (let [record (records/program-record records/sequence-record)
        run #(executor/run record {:host records/host} capabilities/table {:budget %})
        eager (run 1) held (run 0)]
    (is (= :complete (:status eager)) (pr-str eager))
    (is (= [[0.25 0.0 0.5 0.75] [0.25 0.0 0.5 0.75] [0.25 0.0 0.5 0.75]]
           (mapv #(get-in % [:state-after :last :color]) (:history eager))))
    (is (= [:suspended :pending 2] ((juxt :status :reason :at) held)))
    (is (= 2 (count (get-in held [:continuation :history]))))
    (is (vb/equal? (:history eager) (:history (executor/resume (:continuation held) capabilities/table {:budget 1}))))))

(deftest inverse-demands-use-retained-chains-and-root-restrictions
  (let [p (support/curve-point records/host "kA/arc-AB" 0.41)
        read #(coating/locate % 2 p {:budget 1 :order ["kA" "kB"]})
        r (read records/host)
        cold-host (vb/decode (vb/encode records/host))
        missing (update-in cold-host [:bindings 2 1 :chain] #(vec (remove (fn [x] (= "φ" (:id x))) %)))
        widened (assoc-in cold-host [:B :domain] [-100 -80 200 90])]
    (is (= ["kA"] (:contributors r)) "B's metric disc contains this point, its root does not")
    (is (= [0.5 0.0 0.0 0.5] (:color r)))
    (is (= r (read cold-host)))
    (is (= :pending (:status (read missing))))
    (is (= ["kA" "kB"] (:contributors (read widened))))))

(deftest composition-order-and-work-are-separate-inputs
  (let [p (support/curve-point records/host "kA/arc-AB" 0.65)
        read #(coating/locate records/host 2 p %)
        held (read {:budget 0 :order ["kA" "kB"]})]
    (is (= :pending (:status held)))
    (is (= ["kA"] (:known held)))
    (is (= [0.5 0.0 0.0 0.5] (:partial held)))
    (is (re-find #"B@0 → M@2" (get-in held [:missing 0 :missing])))
    (is (= :needs-policy (:status (read {:budget 1 :order nil}))))
    (is (= [0.5 0.0 0.25 0.75] (:color (read {:budget 1 :order ["kB" "kA"]}))))))

(deftest an-explicit-order-cannot-omit-the-only-visible-mark
  (let [p (support/curve-point records/host "kA/arc-AB" 0.41)
        read #(coating/locate records/host 2 p {:budget 1 :order %})]
    (is (= :needs-policy (:status (read ["kB"]))))
    (is (= :needs-policy (:status (read ["kA" "kA"]))))
    (is (= :needs-policy (:status (read []))))
    (is (= [0.5 0.0 0.0 0.5] (:color (read nil))))
    (is (= (coating/snapshot records/host 2 nil) (:snapshot (read nil))
           (:snapshot (coating/locate records/host 2 p {:budget 0 :order nil}))))))
