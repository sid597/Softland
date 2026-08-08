(ns app.client.substrate.connector-route-test
  (:require [app.client.substrate.connector-material-test :as fixture]
            [app.client.substrate.connector-route :as route]
            [app.client.workspace.rect-tree :as rt]
            [app.client.workspace.scene-store :as scene-store]
            [clojure.test :refer [deftest is testing]]))

(def identity-effective
  {:affine [1.0 0.0 0.0 1.0 0.0 0.0]
   :flags 0 :transport-slot 0})

(defn effective
  ([tx ty slot] (effective tx ty slot 0))
  ([tx ty slot flags]
   {:affine [1.0 0.0 0.0 1.0 tx ty]
    :flags flags :transport-slot slot}))

(defn target [vi container container-idx bounds]
  {:vi vi :container container :container-idx container-idx :bounds bounds})

(defn targets
  ([a b]
   {:a [(target :vi/a 1 1 a)]
    :b [(target :vi/b 2 2 b)]})
  ([a-occurrences b-occurrences extra]
   (merge {:a a-occurrences :b b-occurrences} extra)))

(defn edge
  ([id] (edge id (fixture/connector id)))
  ([id material]
   {:id id :address id :container 0 :container-idx 0
    :connector/material material}))

(def base-effective
  {0 identity-effective 1 identity-effective 2 identity-effective})

(defn one-edge [material targets-map effective-map]
  (let [expanded (route/expand-edge-instances [(edge :edge material)] targets-map)]
    (route/resolve-route-geometry (first expanded) effective-map)))

(defn distance [[ax ay] [bx by]]
  (Math/sqrt (+ (Math/pow (- bx ax) 2.0)
                (Math/pow (- by ay) 2.0))))

(deftest occurrence-expansion-is-visible-pair-identity
  (let [a1 (target :a/one 1 1 {:x 0 :y 0 :w 10 :h 10})
        a2 (target :a/two 3 3 {:x 20 :y 0 :w 10 :h 10})
        b1 (target :b/one 2 2 {:x 40 :y 0 :w 10 :h 10})
        expanded (route/expand-edge-instances
                  [(edge :relation/multi)]
                  (targets [a1 a2] [b1] {}))]
    (is (= 2 (count expanded)))
    (is (= #{[:relation/multi :a/one :b/one]
             [:relation/multi :a/two :b/one]}
           (set (map :connector/edge-instance-id expanded))))))

(deftest target-index-collapses-address-stamped-descendants-per-instance
  (let [tree-a
        (rt/rt-node
         :a/root :group {:x 0 :y 0 :w 80 :h 60}
         :data {:address :a}
         :children [(rt/rt-node
                     :a/child :rect {:x 4 :y 4 :w 12 :h 10}
                     :data {:address :a})])
        tree-b (rt/rt-node :b/root :rect {:x 0 :y 0 :w 30 :h 20}
                           :data {:address :a})
        index (scene-store/targets-by-address
               [{:vi :vi/a :container 1 :container-slot 1 :tree tree-a}
                {:vi :vi/b :container 2 :container-slot 2 :tree tree-b}])]
    (is (= 2 (count (:a index)))
        "one stamped block occurrence survives per view-instance")
    (is (= {:x 0 :y 0 :w 80 :h 60}
           (:bounds (first (:a index)))))))

(deftest straight-boundary-clipping-covers-four-quadrants
  (doseq [[label to-bounds expected-from expected-to]
          [[:south-east {:x 20 :y 20 :w 10 :h 10} [10.0 10.0] [20.0 20.0]]
           [:north-east {:x 20 :y -20 :w 10 :h 10} [10.0 0.0] [20.0 -10.0]]
           [:south-west {:x -20 :y 20 :w 10 :h 10} [0.0 10.0] [-10.0 20.0]]
           [:north-west {:x -20 :y -20 :w 10 :h 10} [0.0 0.0] [-10.0 -10.0]]]]
    (testing label
      (let [resolved (one-edge (fixture/connector)
                               (targets {:x 0 :y 0 :w 10 :h 10} to-bounds)
                               base-effective)]
        (is (= :resolved (:status resolved)))
        (is (= expected-from (first (:anchor-points resolved))))
        (is (= expected-to (peek (:anchor-points resolved))))
        (is (= expected-to (first (:to-head resolved)))
            "head tip is the unclipped semantic anchor")
        (let [stroke-width (:stroke-width resolved)
              cap-radius (/ stroke-width 2.0)
              head-length (* stroke-width
                             (get-in resolved
                                     [:material :connector/heads :size-k]))]
          (is (< (Math/abs (- cap-radius
                              (distance expected-from
                                        (first (:stroke-points resolved)))))
                 1.0e-9)
              "the unheaded round cap meets, but does not bleed into, the node")
          (is (< (Math/abs (- (+ head-length cap-radius)
                              (distance expected-to
                                        (peek (:stroke-points resolved)))))
                 1.0e-9)
              "the headed round cap meets, but does not bleed into, the head"))))))

(deftest elbow-v1-and-waypoint-routing-are-pinned
  (let [elbow-material (assoc-in (fixture/connector)
                                 [:connector/route :policy] :elbow/v1)
        elbow (one-edge elbow-material
                        (targets {:x 0 :y 0 :w 10 :h 10}
                                 {:x 30 :y 20 :w 10 :h 10})
                        base-effective)
        waypoint-material (-> (fixture/connector)
                              (assoc-in [:connector/route :policy] :elbow/v1)
                              (assoc-in [:connector/route :waypoints]
                                        [[14.0 5.0] [14.0 5.0] [22.0 17.0]]))
        waypoint (one-edge waypoint-material
                           (targets {:x 0 :y 0 :w 10 :h 10}
                                    {:x 30 :y 20 :w 10 :h 10})
                           base-effective)]
    (is (= [[10.0 5.0] [20.0 5.0] [20.0 25.0] [30.0 25.0]]
           (:anchor-points elbow)))
    (is (= [[14.0 5.0] [22.0 17.0]]
           (subvec (:anchor-points waypoint) 1 3))
        "explicit waypoints are verbatim and consecutive duplicates dedupe")
    (is (= 30.0 (first (peek (:anchor-points waypoint))))
        "terminal segment clips the destination boundary along its own angle")
    (is (= [[5.0 5.0] [35.0 5.0]]
           (route/elbow-points [5.0 5.0] [35.0 5.0]))
        "zero non-dominant delta degrades to straight")))

(deftest cross-container-and-degenerate-states-are-declared
  (let [target-map (targets {:x 0 :y 0 :w 10 :h 10}
                            {:x 0 :y 0 :w 10 :h 10})
        moved {0 identity-effective
               1 (effective 100.0 0.0 1)
               2 (effective 200.0 30.0 2)}
        crossing (one-edge (fixture/connector) target-map moved)
        missing (one-edge (fixture/connector) {:a (get target-map :a)} moved)
        mixed (one-edge (fixture/connector) target-map
                        (assoc-in moved [2 :flags] 1))
        coincident (one-edge (assoc (fixture/connector)
                                    :connector/heads
                                    {:from :none :to :none :size-k 4.0})
                             (targets {:x 0 :y 0 :w 10 :h 10}
                                      {:x 0 :y 0 :w 10 :h 10})
                             base-effective)]
    (is (= :resolved (:status crossing)))
    (is (= [110.0 6.5] (first (:anchor-points crossing))))
    (is (= :unresolved (:status missing)))
    (is (= :mixed-camera (:status mixed)))
    (is (= :degenerate (:status coincident)))))

(deftest cache-is-bounded-and-proportional-through-a-drag
  (let [material-a (fixture/connector :relation/a)
        material-b (-> (fixture/connector :relation/b)
                       (assoc :connector/from
                              {:bind :node :target :c :anchor :boundary})
                       (assoc :connector/to
                              {:bind :node :target :d :anchor :boundary}))
        target-map {:a [(target :a 1 1 {:x 0 :y 0 :w 10 :h 10})]
                    :b [(target :b 2 2 {:x 60 :y 0 :w 10 :h 10})]
                    :c [(target :c 3 3 {:x 0 :y 40 :w 10 :h 10})]
                    :d [(target :d 4 4 {:x 60 :y 40 :w 10 :h 10})]}
        ops [(edge :a material-a) (edge :b material-b)]
        initial-effective {0 identity-effective
                           1 (effective 0 0 1) 2 (effective 0 0 2)
                           3 (effective 0 0 3) 4 (effective 0 0 4)}
        initial (route/derive-route-set nil ops target-map initial-effective
                                        1.0 {})
        static (route/derive-route-set (:state initial) ops target-map
                                       initial-effective 1.0 {})
        bounds-moved-targets
        (assoc-in target-map [:a 0 :bounds] {:x 12 :y 0 :w 10 :h 10})
        bounds-moved
        (route/derive-route-set (:state static) ops bounds-moved-targets
                                initial-effective 1.0 {})
        translated-together
        (into {}
              (map (fn [[container eff]]
                     [container (assoc eff
                                       :affine [1.0 0.0 0.0 1.0
                                                10.0 6.0])]))
              initial-effective)
        same-local-geometry
        (route/derive-route-set (:state static) ops target-map
                                translated-together 1.0 {})
        dragged-effective (assoc initial-effective 1 (effective 12 0 1))
        dragged (route/derive-route-set (:state static) ops target-map
                                        dragged-effective 1.0 {})
        a-id [:relation/a :a :b]
        b-id [:relation/b :c :d]]
    (is (= 2 (get-in initial [:frame-receipt :cache-size])))
    (is (= 0 (get-in static [:frame-receipt :route-resolutions])))
    (is (= #{a-id} (get-in bounds-moved
                            [:frame-receipt :affected-edges]))
        "a target bounds edit rekeys its resolved anchor without a transform edit")
    (is (= 1 (get-in bounds-moved
                      [:frame-receipt :route-resolutions])))
    (is (= 2 (get-in same-local-geometry
                      [:frame-receipt :route-resolutions])))
    (is (zero? (get-in same-local-geometry
                        [:frame-receipt :mesh-derivations]))
        "co-moving anchor and endpoints resolve but reuse byte-identical meshes")
    (is (identical? (get-in static [:state :entries a-id :route])
                    (get-in same-local-geometry
                            [:state :entries a-id :route])))
    (is (= #{a-id} (get-in dragged [:frame-receipt :affected-edges])))
    (is (= 1 (get-in dragged [:frame-receipt :route-resolutions])))
    (is (= 1 (get-in dragged [:frame-receipt :mesh-derivations])))
    (is (= 2 (get-in dragged [:frame-receipt :cache-size])))
    (is (= 2 (get-in dragged [:state :per-edge a-id :route-resolutions])))
    (is (= 1 (get-in dragged [:state :per-edge b-id :route-resolutions])))
    (is (identical? (get-in static [:state :entries b-id])
                    (get-in dragged [:state :entries b-id]))
        "unaffected sibling cache entry survives by identity")))

(deftest s3-region-object-resolver-and-value-door-are-bounded-to-one-region
  (let [material (-> (fixture/connector :relation/region)
                     (assoc :connector/to
                            {:bind :region-object :region :region/address
                             :object :grey-box :local [0.4 0.25 0.3]}))
        ops [(edge :region-edge material)]
        target-map {:a [(target :vi/a 1 1 {:x 0 :y 0 :w 10 :h 10})]}
        effective-map {0 identity-effective 1 identity-effective}
        resolver (fn [_binding _anchor _effective]
                   {:status :resolved :center [70.0 15.0] :camera 0
                    :clip :none :anchor-clamped true})
        door-a {:region/address [:view/a nil {} identity-effective]}
        initial (route/derive-route-set
                 nil ops target-map effective-map 1.0 {}
                 {:region-anchor-resolver resolver :region-doors door-a})
        static (route/derive-route-set
                (:state initial) ops target-map effective-map 1.0 {}
                {:region-anchor-resolver resolver :region-doors door-a})
        moved (route/derive-route-set
               (:state static) ops target-map effective-map 1.0 {}
               {:region-anchor-resolver resolver
                :region-doors
                {:region/address [:view/b nil {} identity-effective]}})
        absent (route/derive-route-set
                nil ops target-map effective-map 1.0 {}
                {:region-anchor-resolver (fn [& _] nil)
                 :region-doors door-a})
        edge-id [:relation/region :vi/a
                 [:region-object :region/address :grey-box]]]
    (is (= :resolved (get-in initial [:routes 0 :status])))
    (is (true? (get-in initial [:routes 0 :anchor-clamped])))
    (is (= 1 (get-in initial [:census :anchor-clamped])))
    (is (= 1 (get-in initial [:frame-receipt :anchor-projections])))
    (is (zero? (get-in static [:frame-receipt :route-resolutions])))
    (is (= #{edge-id} (get-in moved [:frame-receipt :affected-edges])))
    (is (= 1 (get-in moved [:frame-receipt :anchor-projections])))
    (is (= :region-anchor-absent (get-in absent [:routes 0 :status])))
    (is (= 1 (get-in absent [:census :region-anchor-absent])))))

(deftest live-pick-re-resolves-after-transform-without-tree-write
  (let [target-map (targets {:x 0 :y 0 :w 10 :h 10}
                            {:x 60 :y 0 :w 10 :h 10})
        !effective (atom {0 identity-effective
                          1 (effective 0 0 1)
                          2 (effective 0 0 2)})
        result (route/derive-route-set nil [(edge :pick)] target-map
                                       @!effective 1.0 {})
        edge-id [:pick :vi/a :vi/b]
        old-mid [35.0 5.0]]
    (route/set-live-effective-provider! #(deref !effective))
    (is (route/live-hit? edge-id old-mid))
    (swap! !effective assoc 1 (effective 0 30 1))
    (is (not (route/live-hit? edge-id old-mid)))
    (is (route/live-hit? edge-id [35.0 20.0]))
    (route/set-live-effective-provider! nil)))

(deftest live-pick-refreshes-each-edge-bound-to-one-moved-container
  (let [material-a (fixture/connector :pick/a)
        material-b (-> (fixture/connector :pick/b)
                       (assoc :connector/to
                              {:bind :node :target :c :anchor :boundary}))
        target-map
        {:a [(target :vi/a 1 1 {:x 0 :y 0 :w 10 :h 10})]
         :b [(target :vi/b 2 2 {:x 60 :y 0 :w 10 :h 10})]
         :c [(target :vi/c 3 3 {:x 60 :y 40 :w 10 :h 10})]}
        !effective (atom {0 identity-effective
                          1 (effective 0 0 1)
                          2 (effective 0 0 2)
                          3 (effective 0 0 3)})
        ops [(edge :pick/a material-a) (edge :pick/b material-b)]
        _ (route/derive-route-set nil ops target-map @!effective 1.0 {})
        a-id [:pick/a :vi/a :vi/b]
        b-id [:pick/b :vi/a :vi/c]]
    (route/set-live-effective-provider! #(deref !effective))
    (swap! !effective assoc 1 (effective 0 20 1))
    (is (route/live-hit? a-id [35.0 15.0]))
    (is (route/live-hit? b-id [35.0 35.0])
        "refreshing one edge cannot mark a sibling's cached anchors current")
    (is (not (route/live-hit? b-id [35.0 25.0])))
    (route/set-live-effective-provider! nil)))

(deftest rect-tree-registered-predicate-uses-live-route-not-spanning-bounds
  (let [material (fixture/connector :pick/registered)
        target-map (targets {:x 0 :y 0 :w 10 :h 10}
                            {:x 60 :y 0 :w 10 :h 10})
        !effective (atom {0 identity-effective
                          1 (effective 0 0 1)
                          2 (effective 0 0 2)})
        _ (route/derive-route-set nil [(edge :pick/registered material)]
                                  target-map @!effective 1.0 {})
        edge-id [:pick/registered :vi/a :vi/b]
        tree (rt/rt-node
              :connector :connector {:x 0 :y 0 :w 100 :h 100}
              :data {:address edge-id
                     :connector/edge-instance-id edge-id
                     :connector/from-vi :vi/a
                     :connector/to-vi :vi/b
                     :connector/material material})]
    (route/set-live-effective-provider! #(deref !effective))
    (try
      (is (nil? (rt/hit-test tree 35.0 20.0))
          "broad-phase inclusion cannot turn an off-stroke point into a hit")
      (is (= :connector (:id (peek (rt/hit-test tree 35.0 5.0)))))
      (swap! !effective assoc 1 (effective 0 30 1))
      (is (nil? (rt/hit-test tree 35.0 5.0)))
      (is (= :connector (:id (peek (rt/hit-test tree 35.0 20.0))))
          "the unchanged tree picks the lazily refreshed route")
      (finally
        (route/set-live-effective-provider! nil)))))

(deftest deterministic-mesh-and-key-cover-version-provider-and-regime
  (let [labeled (assoc (fixture/connector)
                       :connector/label {:text "reference" :at 0.5
                                         :offset [0.0 -8.0]})
        target-map (targets {:x 0 :y 0 :w 10 :h 10}
                            {:x 60 :y 0 :w 10 :h 10})
        edge (first (route/expand-edge-instances [(edge :det labeled)] target-map))
        geometry (route/resolve-route-geometry edge base-effective)
        mesh-a (route/derive-mesh geometry 1.0)
        mesh-b (route/derive-mesh geometry 1.0)
        key-a (app.client.substrate.connector-material/derivation-key
               labeled (:resolved-anchor-tuple geometry) {:face-revision :a} 1.0)
        key-b (app.client.substrate.connector-material/derivation-key
               labeled (:resolved-anchor-tuple geometry) {:face-revision :b} 1.0)]
    (is (= (:mesh-bytes mesh-a) (:mesh-bytes mesh-b)))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo #"requires the live text provider"
         (route/layout-label mesh-a {})))
    (is (not= key-a key-b))
    (is (not= key-a
              (app.client.substrate.connector-material/derivation-key
               (assoc labeled :connector/dress-revision 2)
               (:resolved-anchor-tuple geometry) {:face-revision :a} 1.0)))
    (is (= :floor-default (:regime mesh-a)))
    (is (= :legal-max (:regime (route/derive-mesh geometry 100.0))))))
