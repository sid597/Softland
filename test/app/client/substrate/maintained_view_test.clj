(ns app.client.substrate.maintained-view-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [app.client.substrate.scene-tape :as tape]
            [app.client.workspace.containers :as ctn]
            [app.client.workspace.rect-tree :as rt]
            [app.client.workspace.scene-store :as ss]))

(defn- box [id address]
  (rt/rt-node id :box {:x 0 :y 0 :w 100 :h 100}
              :data {:address address}))

(defn- oracle-pick [store effective point]
  (some->
   (tape/pick-reverse
    (ss/scene-tape store)
    (fn [entry]
      (let [slot (:runtime/slot entry)
            eff (get effective (:container slot))
            [lx ly] (ctn/inverse-point eff point)]
        (when (rt/hit-test (:tree slot) lx ly)
          {:vi (:vi slot)}))))
   :hit))

(defn- assert-store-equivalent! [store effective probes]
  (is (= (ss/maintained-entries store)
         (:entries (ss/scene-tape store)))
      "maintained walk equals the independent one-arg batch oracle")
  (doseq [point probes]
    (is (= (some-> (ss/pick store effective point) (select-keys [:vi]))
           (oracle-pick store effective point))
        (str "maintained and oracle pick agree at " point))))

(deftest g1-maintained-store-equivalence-across-public-writes
  (let [registry0 (-> (ctn/empty-registry)
                      (ctn/add-container 10 {:x 0 :y 0 :layer 1})
                      (ctn/add-container 20 {:x 200 :y 0 :layer 5}))
        effective0 (ctn/effective registry0)
        vi-a [:vi :a]
        vi-b [:vi :b]
        store0 (ss/empty-store)
        store1 (ss/upsert-slot store0 vi-a
                               {:tree (box :a "a") :container 10
                                :stack-path (:stack-path (get effective0 10))})
        store2 (ss/upsert-slot store1 vi-b
                               {:tree (box :b "b") :container 20
                                :stack-path (:stack-path (get effective0 20))})
        store3 (ss/update-nodes-by-address
                store2 "a" #(assoc-in % [:style :bg] [1 0 0 1]))
        registry1 (ctn/set-transform registry0 10 {:x 25})
        effective1 (ctn/effective registry1)
        store4 (ss/remove-slot store3 vi-b)]
    (testing "every public write surface keeps the maintained view fenced"
      (assert-store-equivalent! store0 effective0 [[0 0]])
      (assert-store-equivalent! store1 effective0 [[50 50] [250 50]])
      (assert-store-equivalent! store2 effective0 [[50 50] [250 50]])
      (assert-store-equivalent! store3 effective0 [[50 50] [250 50]])
      (assert-store-equivalent! store3 effective1 [[75 50] [250 50]])
      (assert-store-equivalent! store4 effective1 [[75 50] [250 50]]))
    (testing "an untouched slot keeps the same ops arrays through the view"
      (is (identical? (:ops (ss/slot store2 vi-b))
                      (:ops (:runtime/slot
                             (first (filter #(= vi-b (:instance/id %))
                                            (ss/maintained-entries store3))))))))
    (testing "insert-edge validation is independent and fail-closed"
      (is (thrown? clojure.lang.ExceptionInfo
                   (tape/ordered-insert
                    tape/default-family-registry
                    (sorted-map-by tape/entry-key-compare)
                    {:entry/id [:malformed]}))))
    (testing "EDN rehydration uses the one explicit comparator rebuild door"
      (let [round-tripped (edn/read-string (pr-str store3))
            rebuilt (ss/rebuild-ordered round-tripped)]
        (is (not (sorted? (:ordered round-tripped))))
        (is (sorted? (:ordered rebuilt)))
        (is (= (ss/maintained-entries rebuilt)
               (:entries (ss/scene-tape rebuilt))))
        (is (ss/store-fns-free? rebuilt))))))

(deftest g1-stamped-and-stampless-ruling-probes
  (let [registry (-> (ctn/empty-registry)
                     (ctn/add-container 10 {:layer 1})
                     (ctn/add-container 20 {:layer 5}))
        effective (ctn/effective registry)
        stamped (-> (ss/empty-store)
                    (ss/upsert-slot :z-low
                                    {:tree (box :low "low") :container 10
                                     :stack-path (:stack-path (get effective 10))})
                    (ss/upsert-slot :a-high
                                    {:tree (box :high "high") :container 20
                                     :stack-path (:stack-path (get effective 20))}))
        stampless (-> (ss/empty-store)
                      (ss/upsert-slot :z-low {:tree (box :low "low") :container 10})
                      (ss/upsert-slot :a-high {:tree (box :high "high") :container 20}))]
    (testing "production-stamped order agrees in stamped, refreshed, and maintained forms"
      (is (= (ss/maintained-entries stamped)
             (:entries (ss/scene-tape stamped))
             (:entries (ss/scene-tape stamped effective)))))
    (testing "R1: stampless maintained order follows paint, not refreshed pick order"
      (is (= (ss/maintained-entries stampless)
             (:entries (ss/scene-tape stampless))))
      (is (not= (mapv :entry/id (ss/maintained-entries stampless))
                (mapv :entry/id (:entries (ss/scene-tape stampless effective))))))))

(defn- frame-entry [entry-id rank payload visible?]
  {:entry/id entry-id
   :material/id [:material entry-id]
   :material/revision payload
   :instance/id [:instance entry-id]
   :family/id :render.family/rect
   :order {:stratum :world :pass-class :direct
           :stack-path [[:frame/root rank rank]]
           :part-rank 0 :stable-tie entry-id}
   :paint {:payload payload}
   :pick :none
   :visibility {:visible? visible?}})

(defn- update-synthetic-arrangement [registry arrangement entries]
  (let [wanted (into #{} (map tape/entry-key) entries)
        arrangement (reduce (fn [ordered entry]
                              (if (contains? wanted (tape/entry-key entry))
                                ordered
                                (tape/ordered-remove ordered entry)))
                            arrangement
                            (vals arrangement))]
    (reduce (fn [ordered entry]
              (let [key (tape/entry-key entry)]
                (if (contains? ordered key)
                  (assoc ordered key entry)
                  (tape/ordered-insert registry ordered entry))))
            arrangement
            entries)))

(deftest g2-frame-arrangement-ops-equal-batch-oracle
  (let [registry (select-keys tape/default-family-registry
                              [:render.family/rect])
        a0 (frame-entry :a 10 0 true)
        a1 (frame-entry :a 10 1 true)
        a2 (frame-entry :a 10 1 false)
        b0 (frame-entry :b 20 0 true)
        b1 (frame-entry :b 5 1 true)
        frames [[a0]                    ; enter
                [a1]                    ; payload-only rebind
                [a1 b0]                 ; enter another entry
                [a1 b1]                 ; order-token change
                [a2 b1]                 ; visibility payload toggle
                [b1]                    ; exit
                []]]
    (loop [arrangement (sorted-map-by tape/entry-key-compare)
           frame-number 0
           frames frames]
      (when-let [entries (first frames)]
        (let [next-arrangement
              (update-synthetic-arrangement registry arrangement entries)
              oracle (tape/compile-tape registry
                                        [:synthetic-frame frame-number]
                                        entries)]
          (is (= (vec (vals next-arrangement)) (:entries oracle))
              (str "frame arrangement equals batch oracle at step " frame-number))
          (when (= frame-number 1)
            (is (= 1 (get-in (first (vals next-arrangement))
                             [:paint :payload]))
                "same order key cheaply rebinds the current payload"))
          (recur next-arrangement (inc frame-number) (next frames)))))))

(deftest g7-screen-space-pick-and-world-compatibility
  (let [registry (-> (ctn/empty-registry)
                     (ctn/add-container :world
                                        {:x 0 :y 0 :layer 1 :camera :world})
                     (ctn/add-container :screen
                                        {:x 100 :y 50 :layer 5 :camera :screen}))
        effective (ctn/effective registry)
        screen-store
        (-> (ss/empty-store)
            (ss/upsert-slot :screen-slot
                            {:tree (box :screen "screen")
                             :container :screen
                             :stack-path (:stack-path (get effective :screen))}))
        world-store
        (-> (ss/empty-store)
            (ss/upsert-slot :world-slot
                            {:tree (box :world "world")
                             :container :world
                             :stack-path (:stack-path (get effective :world))}))
        ;; Non-identity camera: screen = world*2 + [100 50].
        point-map {:world [10 10] :screen [120 70]}]
    (testing "screen-camera containers consume the cursor's screen coordinate"
      (is (= "screen" (:address (ss/pick screen-store effective point-map))))
      (is (nil? (ss/pick screen-store effective (:world point-map)))
          "legacy all-world conversion would miss this screen-pinned container"))
    (testing "world-camera containers keep exact bare-vector semantics"
      (is (= (ss/pick world-store effective [10 10])
             (ss/pick world-store effective
                      {:world [10 10] :screen [10 10]}))
          "identity-camera map and bare forms agree")
      (is (= (ss/pick world-store effective [10 10])
             (ss/pick world-store effective point-map))
          "non-identity pan/zoom changes only screen, never world picking"))))
