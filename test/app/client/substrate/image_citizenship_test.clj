(ns app.client.substrate.image-citizenship-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.substrate.image-material :as image]
            [app.client.substrate.scene-tape :as tape]
            [app.client.workspace.containers :as containers]
            [app.client.workspace.rect-tree :as rt]
            [app.client.workspace.scene-store :as store]))

(def digest (apply str (repeat 64 "c")))

(defn- sha256 [value]
  (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                        (.getBytes (pr-str value) "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

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
   :paint (if (= :render.family/image family-id)
            {:paint/source ::image-system
             :paint/source-type :image-system
             :op-offset 0
             :instance-count 0}
            {:batch id})
   :pick :none
   :visibility {:visible? true}})

(defn image-node
  [id address bounds]
  (rt/rt-node id :image bounds
              :data {:address address
                     :image/digest digest
                     :image/color-tag :srgb
                     :image/alpha-association :straight
                     :image/intrinsic-size [200 100]
                     :image/crop {:x 0 :y 0 :w 200 :h 100}}))

(deftest g1-image-registration-is-total-and-existing-values-stay-pinned
  (is (= :render.family/image (:family/id tape/image-registration)))
  (is (= tape/image-registration
         (get tape/default-family-registry :render.family/image)))
  (is (= 6 (count tape/family-ids)))
  (is (= :none-promised
         (get-in tape/image-registration [:grammar :export-projections])))
  (is (= :none-static
         (get-in tape/image-registration [:render :geometry :time-sample])))
  (is (= [:paint/source :paint/source-type :op-offset :instance-count]
         (get-in tape/image-registration
                 [:grammar :entry-paint-required-keys])))
  (is (= "581daf7fc7980853e5e71df8aa4b4ad689d6d5a6adb93e4b09a994bb863aed0b"
         (sha256 (vec (take 5 tape/family-contracts))))
      "the five Act-0 registration values remain exactly pinned")
  (is (= [[0.01 0.1] [0.1 8.0] [8.0 1000.0]]
         (mapv (juxt #(get-in % [:zoom :min]) #(get-in % [:zoom :max]))
               (get-in tape/image-registration [:render :geometry :regimes]))))
  (is (thrown? clojure.lang.ExceptionInfo
               (tape/compile-tape
                :invalid-image-paint
                [(assoc (tape-entry :image :render.family/image 3)
                        :paint {:batch :image})]))))

(deftest g5-image-kind-layer-order-is-unique-and-shuffle-deterministic
  (let [lane-specs [[:shadow :render.family/shadow 0]
                    [:rect :render.family/rect 1]
                    [:slot-text :render.family/msdf 2]
                    [:image :render.family/image 3]]
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
    ;; IMAGE-ATOM T13: a collision would route cross-lane order through
    ;; stable-tie's spelling fallback.  Rank 3 is a hard contract value.
    (is (= [0 1 2 3] part-ranks))
    (is (= (count part-ranks) (count (set part-ranks))))
    (is (every? #(= [:shadow :rect :slot-text :image] (:ids %)) receipts))
    (is (= 1 (count (set (map :order-hash receipts)))))))

(deftest g8-tree-images-address-refusal-and-clipped-crop
  (testing "an image-bearing node without its own address is refused"
    (is (thrown? clojure.lang.ExceptionInfo
                 (rt/tree->images
                  (rt/rt-node :bad :image {:x 0 :y 0 :w 10 :h 10}
                              :data {:image/digest digest
                                     :image/intrinsic-size [10 10]})))))
  (testing "ancestor clipping carries into both placement and UV crop"
    (let [tree (rt/rt-node :clip :box {:x 0 :y 0 :w 60 :h 50}
                           :clip? true
                           :children [(image-node :image "image/1"
                                                  {:x 20 :y 10 :w 80 :h 50})])
          [op] (rt/tree->images tree)]
      (is (= {:x 20 :y 10 :w 40 :h 40}
             (select-keys op [:x :y :w :h])))
      (is (= {:x 0.0 :y 0.0 :w 100.0 :h 80.0} (:image/crop op)))
      (is (= [0.0 0.0 0.5 0.8] (:image/uv op)))
      (is (= "image/1" (:address op))))))

(deftest g8-pure-store-payload-carries-image-lane-with-stable-identity
  (let [vi-a [:vi :image :a]
        vi-b [:vi :image :b]
        tree-a (image-node :image-a "image/a" {:x 0 :y 0 :w 100 :h 50})
        tree-b (image-node :image-b "image/b" {:x 10 :y 5 :w 100 :h 50})
        store-a (store/upsert-slot (store/empty-store) vi-a
                                   {:tree tree-a :container 1
                                    :container-slot 7})
        images-a (get-in (store/slot store-a vi-a) [:ops :images])
        store-b (store/upsert-slot store-a vi-b
                                   {:tree tree-b :container 2
                                    :container-slot 8})
        payload (store/derive-store-frame store-b)]
    (is (identical? images-a (get-in (store/slot store-b vi-a) [:ops :images])))
    (is (= [7 8] (mapv :container-idx (:images payload))))
    (is (= 1 (get-in payload [:ops-count-by-vi vi-a :images])))
    (is (= 1 (get-in payload [:ops-count-by-vi vi-b :images])))
    (is (= #{vi-a vi-b} (set (keys (:order-by-vi payload)))))
    (is (not (contains? (get-in payload [:order-by-vi vi-a]) :images))
        "order-by-vi stays lane-agnostic")
    (is (empty? (:rects payload)))
    (is (empty? (:shadows payload)))
    (is (= {vi-a [] vi-b []} (:text-by-vi payload)))))

(deftest g8-product-pick-is-image-own-address-and-half-open
  (let [low-vi [:vi :low]
        high-vi [:vi :high]
        low-tree (rt/rt-node :low-root :box {:x 0 :y 0 :w 100 :h 100}
                             :data {:address "ancestor/low"}
                             :children [(image-node :low-image "image/low"
                                                    {:x 0 :y 0 :w 100 :h 100})])
        high-tree (rt/rt-node :high-root :box {:x 0 :y 0 :w 100 :h 100}
                              :data {:address "ancestor/high"}
                              :children [(image-node :high-image "image/high"
                                                     {:x 0 :y 0 :w 100 :h 100})])
        registry (-> (containers/empty-registry)
                     (containers/add-container 1 {:layer 1})
                     (containers/add-container 2 {:layer 2}))
        effective (containers/effective registry)
        scene (-> (store/empty-store)
                  (store/upsert-slot low-vi {:tree low-tree :container 1
                                             :container-slot 1
                                             :stack-path (:stack-path (get effective 1))})
                  (store/upsert-slot high-vi {:tree high-tree :container 2
                                              :container-slot 2
                                              :stack-path (:stack-path (get effective 2))}))]
    (is (= "image/high" (:address (store/pick scene effective [50 50]))))
    (is (nil? (store/pick scene effective [100 50]))
        "max edge is excluded by the product half-open law")))

(deftest g8-rt-node-key-set-pins-the-no-transform-precondition
  (is (= #{:id :type :bounds :style :actions :children :text :clip? :data :layout}
         (set (keys (rt/rt-node :id :image {:x 0 :y 0 :w 1 :h 1})))))
  (is (not (contains? (rt/rt-node :id :image {:x 0 :y 0 :w 1 :h 1})
                      :transform))))
