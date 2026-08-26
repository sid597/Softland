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
  (testing "registrations carry exactly the two fields consumed at runtime"
    (doseq [[family-id family] tape/default-family-registry]
      (is (= family-id (:family/id family)))
      (is (= #{:family/id :entry-paint-required-keys} (set (keys family))))
      (is (vector? (:entry-paint-required-keys family)))))
  (testing "unread registration fields and undeclared family ids fail closed"
    (is (thrown? clojure.lang.ExceptionInfo
                 (tape/register-family
                  {}
                  (assoc (first tape/family-contracts) :receipts [:paper]))))
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
