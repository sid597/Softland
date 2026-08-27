(ns app.client.substrate.image-citizenship-test
  (:require [clojure.test :refer [deftest is]]
            [app.client.substrate.image-material :as image]
            [app.client.substrate.scene-tape :as tape]))

(defn- shuffled [seed values]
  (let [items (java.util.ArrayList. values)]
    (java.util.Collections/shuffle items (java.util.Random. seed))
    (vec items)))

(defn- tape-entry [id family-id part-rank]
  {:entry/id id
   :material/id id
   :material/revision 0
   :instance/id id
   :family/id family-id
   :order {:stratum :world :pass-class :direct
           :stack-path [[:root 0 0]]
           :part-rank part-rank :stable-tie id}
   :paint (case family-id
            :render.family/image
            {:paint/source ::image-system
             :paint/source-type :image-system
             :op-offset 0
             :instance-count 0}

            :render.family/path
            {:vertex-count 3}

            {:batch id})
   :visibility {:visible? true}})

(deftest g1-image-admission-and-paint-shape-stay-fail-closed
  (let [registration (get tape/default-family-registry :render.family/image)]
    (is (= :render.family/image (:family/id registration)))
    (is (= (set tape/family-ids) (set (keys tape/default-family-registry))))
    (is (= #{:family/id :entry-paint-required-keys}
           (set (keys registration))))
    (is (= [:paint/source :paint/source-type :op-offset :instance-count]
           (:entry-paint-required-keys registration))))
  (is (thrown? clojure.lang.ExceptionInfo
               (tape/compile-tape
                :invalid-image-paint
                [(assoc (tape-entry :image :render.family/image 3)
                        :paint {:batch :image})]))))

(deftest image-layer-order-is-unique-and-shuffle-deterministic
  (let [lane-specs [[:clip :render.family/clip 0]
                    [:slot-text :render.family/slug 1]
                    [:image :render.family/image 2]
                    [:path :render.family/path 3]]
        part-ranks (mapv #(nth % 2) lane-specs)
        entries (mapv #(apply tape-entry %) lane-specs)
        receipts
        (mapv (fn [seed]
                (let [registry (tape/register-families
                                (shuffled seed tape/family-contracts))
                      compiled (tape/compile-tape
                                registry [:g5 seed]
                                (shuffled (+ seed 1000) entries))]
                  {:ids (mapv :entry/id (:entries compiled))
                   :order-hash (:order-hash compiled)}))
              (range 40))]
    (is (= [0 1 2 3] part-ranks))
    (is (= (count part-ranks) (count (set part-ranks))))
    (is (every? #(= [:clip :slot-text :image :path] (:ids %)) receipts))
    (is (= 1 (count (set (map :order-hash receipts)))))))
