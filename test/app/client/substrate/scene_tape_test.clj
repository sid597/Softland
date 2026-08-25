(ns app.client.substrate.scene-tape-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.substrate.scene-tape :as tape]))

(defn- entry
  ([entry-id family-id layer]
   (entry entry-id family-id layer {:geometry :test :owner entry-id}))
  ([entry-id family-id layer pick]
   {:entry/id entry-id
    :material/id [:material entry-id]
    :material/revision 1
    :instance/id [:instance entry-id]
    :family/id family-id
    :order {:stratum :world
            :pass-class :direct
            :stack-path [[:root layer layer]]
            :part-rank 0
            :stable-tie entry-id}
    :paint {:batch/id entry-id}
    :pick pick
    :visibility {:visible? true}}))

(deftest w2b-family-admission-is-complete-and-fail-closed
  (testing "the executor registry admits exactly the declared families"
    (is (= (set tape/family-ids)
           (set (keys tape/default-family-registry))))
    (is (= tape/default-family-registry
           (tape/register-families (reverse tape/family-contracts)))
        "registration order is not scene order"))
  (testing "every family unconditionally declares geometry, Q2 regimes, and color"
    (doseq [[family-id family] tape/default-family-registry]
      (let [geometry (get-in family [:render :geometry])
            regimes (:regimes geometry)]
        (is (= family-id (:family/id family)))
        (is (map? (:authority geometry)))
        (is (map? (:classify geometry)))
        (is (map? (:coverage geometry)))
        (is (map? (:pick geometry)))
        (is (map? (:bounds geometry)))
        (is (seq regimes))
        (is (= 0.01 (get-in (first regimes) [:zoom :min])))
        (is (= 1000.0 (get-in (last regimes) [:zoom :max])))
        (is (= :scene-color/linear-premultiplied-srgb
               (get-in family [:render :color-alpha :scene]))))))
  (testing "missing geometry and undeclared family ids fail before a frame exists"
    (is (thrown? clojure.lang.ExceptionInfo
                 (tape/register-family
                  {}
                  (update (first tape/family-contracts) :render dissoc :geometry))))
    (is (thrown? clojure.lang.ExceptionInfo
                 (tape/compile-tape
                  :bad-family
                  [(entry :image :render.family/image 0)])))))

(deftest w2b-color-seam-is-tagged-linear-premultiplied-and-default-off
  (is (false? (:enabled? (tape/scene-color false))))
  (is (= :scene-color/legacy-direct
         (:scene-color/id (tape/scene-color nil))))
  (is (= {:color [:src-alpha :one-minus-src-alpha]
          :alpha [:src-alpha :one-minus-src-alpha]}
         (:blend (tape/scene-color false))))
  (is (= :linear-srgb (:working-space (tape/scene-color true))))
  (is (= :premultiplied (:alpha-association (tape/scene-color true))))
  (is (= {:color [:one :one-minus-src-alpha]
          :alpha [:one :one-minus-src-alpha]}
         (:blend (tape/scene-color true))))
  (is (true? (:default-off? tape/scene-color-seam))))

(deftest w2b-one-tape-paints-forward-and-picks-exact-reverse
  (let [entries [(entry :msdf :render.family/msdf 10)
                 (entry :slug :render.family/slug 20)
                 (entry :clip :render.family/clip 0 :none)]
        compiled (tape/compile-tape :world-r1 (reverse entries))
        compiled-again (tape/compile-tape :world-r1 entries)
        !painted (atom [])
        !picked (atom [])
        _ (tape/paint-forward
           compiled
           (fn [scene-entry]
             (swap! !painted conj (:entry/id scene-entry))))
        resolved (tape/pick-reverse
                  compiled
                  (fn [scene-entry]
                    (swap! !picked conj (:entry/id scene-entry))
                    (when (= :msdf (:entry/id scene-entry))
                      {:owner :msdf})))]
    (is (= [:clip :msdf :slug] @!painted))
    (is (= [:slug :msdf] @!picked)
        "non-pickable clip entries are filtered, never independently sorted")
    (is (= :msdf (get-in resolved [:entry :entry/id])))
    (is (= {:owner :msdf} (:hit resolved)))
    (is (= (:order-hash compiled) (:order-hash compiled-again)))
    (is (= (mapv :entry/id (:entries compiled))
           (mapv :entry/id (:entries compiled-again))))))

(deftest w2b-msdf-counterexample-remains-declared-not-blessed
  (is (= :falsified-47-isocontour-mismatches-per-regime
         (get-in tape/default-family-registry
                 [:render.family/msdf :render :geometry :regimes 0 :verdict])))
  (is (= [:w0-a/msdf-47-mismatch-counterexample]
         (get-in tape/default-family-registry
                 [:render.family/msdf :receipts]))))
