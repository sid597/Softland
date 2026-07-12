(ns app.client.workspace.scene-store-test
  "P1 gates for scene-substrate (CONTRACT §8) + containers coverage. JVM-run over
   the pure .cljc libs — no cljs runtime. Covers:
   - G1: one address in two view-instances with independent geometry; an
     address-level write patches both via the index; pick on each returns the
     same address, different appearance (Δ1 falsifier);
   - G2: EDN round-trip of a descriptor store + store-fns-free? (trap T1);
   - G3: identity preservation — untouched slots' :ops stay identical? (trap T5);
   - containers: nested composition, inverse-point round-trip, :screen camera
     inheritance, cid-0 reserved, parent-cycle guard;
   - pick: layer ordering, miss → nil, deepest-addressed ancestor fallback."
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.edn :as edn]
            [clojure.set]
            [app.client.workspace.scene-store :as ss]
            [app.client.workspace.containers :as ctn]
            [app.client.workspace.face-assembly :as fa]
            [app.client.workspace.rect-tree :as rt]))

;; ---------------------------------------------------------------------------
;; G1 — one address, two view-instances (the Δ1 falsifier, CONTRACT §8)
;; ---------------------------------------------------------------------------

(deftest g1-one-address-two-view-instances
  (let [address "addr/block-7"
        vi-a    [:vi :reader-face 1]
        vi-b    [:vi :reader-face 2]
        ;; independent LOCAL geometry: A's addressed leaf at (10,10), B's at (30,5)
        tree-a  (rt/rt-node :root :box {:x 0 :y 0 :w 200 :h 100}
                            :children [(rt/rt-node :leaf :box {:x 10 :y 10 :w 50 :h 20}
                                                   :data {:address address})])
        tree-b  (rt/rt-node :root :box {:x 0 :y 0 :w 200 :h 100}
                            :children [(rt/rt-node :leaf :box {:x 30 :y 5 :w 50 :h 20}
                                                   :data {:address address})])
        ;; independent containers: cid 1 offset (1000,0) s1 layer1; cid 2 (0,500) s2 layer2
        reg     (-> (ctn/empty-registry)
                    (ctn/add-container 1 {:x 1000 :y 0 :scale 1 :layer 1})
                    (ctn/add-container 2 {:x 0 :y 500 :scale 2 :layer 2}))
        effs    (ctn/effective reg)
        store   (-> (ss/empty-store)
                    (ss/upsert-slot vi-a {:tree tree-a :container 1})
                    (ss/upsert-slot vi-b {:tree tree-b :container 2}))]
    (testing "the index fans ONE address out to BOTH view-instances"
      (is (= #{vi-a vi-b} (ss/slots-for-address store address))))
    (testing "an address-level write patches BOTH slots' trees via the index"
      (let [before-a (:tree (ss/slot store vi-a))
            before-b (:tree (ss/slot store vi-b))
            store'   (ss/update-nodes-by-address
                      store address (fn [n] (assoc-in n [:data :marked?] true)))
            after-a  (:tree (ss/slot store' vi-a))
            after-b  (:tree (ss/slot store' vi-b))]
        (is (not= before-a after-a) "slot A's tree changed")
        (is (not= before-b after-b) "slot B's tree changed")
        (is (true? (get-in after-a [:children 0 :data :marked?])) "A's leaf patched")
        (is (true? (get-in after-b [:children 0 :data :marked?])) "B's leaf patched")))
    (testing "pick on each copy returns the SAME address with DIFFERENT vi"
      ;; A's leaf world (container 1): x[1010,1060] y[10,30]; probe (1020,15)
      ;; B's leaf world (container 2): x[60,160] y[510,550]; probe (100,520)
      (let [pa (ss/pick store effs [1020 15])
            pb (ss/pick store effs [100 520])]
        (is (= address (:address pa)))
        (is (= address (:address pb)))
        (is (= vi-a (:vi pa)))
        (is (= vi-b (:vi pb)))
        (is (not= (:vi pa) (:vi pb)) "same address, different appearance")))))

;; ---------------------------------------------------------------------------
;; G2 — EDN round-trip + serializability guard (CONTRACT §8, trap T1)
;; ---------------------------------------------------------------------------

(deftest g2-edn-roundtrip-and-no-fns
  (let [address "addr/focus-1"
        vi      [:vi :reader-face 1]
        tree    (rt/rt-node :root :box {:x 0 :y 0 :w 120 :h 40}
                            :children [(rt/rt-node :leaf :box {:x 0 :y 0 :w 120 :h 40}
                                                   :data {:address address
                                                          :actions [{:action :block/focus
                                                                     :target address}]})])
        store   (-> (ss/empty-store)
                    (ss/upsert-slot vi {:tree tree :container 1
                                        :meta {:face "assembly/x" :src "data/y"}}))]
    (testing "the store round-trips pr-str → edn/read-string to an = value"
      (is (= store (edn/read-string (pr-str store)))))
    (testing "store-fns-free? is true for a descriptor-only store"
      (is (true? (ss/store-fns-free? store))))
    (testing "action descriptors survive the round-trip as data"
      (let [store' (edn/read-string (pr-str store))
            node   (get-in (ss/slot store' vi) [:tree :children 0])]
        (is (= [{:action :block/focus :target address}]
               (get-in node [:data :actions])))))
    (testing "NEGATIVE: a closure in slot :data trips the guard"
      (let [dirty (assoc-in store [:slots vi :tree :children 0 :data :handler]
                            (fn [_] :boom))]
        (is (false? (ss/store-fns-free? dirty)))))))

;; ---------------------------------------------------------------------------
;; G3 — identity preservation (CONTRACT §8, trap T5)
;; ---------------------------------------------------------------------------

(deftest g3-identity-preservation
  (let [tree-a (rt/rt-node :a :box {:x 0 :y 0 :w 100 :h 40}
                           :children [(rt/rt-node :la :box {:x 0 :y 0 :w 100 :h 40}
                                                  :data {:address "addr/a"})])
        tree-b (rt/rt-node :b :box {:x 0 :y 0 :w 100 :h 40}
                           :children [(rt/rt-node :lb :box {:x 0 :y 0 :w 100 :h 40}
                                                  :data {:address "addr/b"})])
        vi-a   [:vi :face :a]
        vi-b   [:vi :face :b]
        store  (-> (ss/empty-store)
                   (ss/upsert-slot vi-a {:tree tree-a :container 1}))
        ops-a  (:ops (ss/slot store vi-a))]
    (testing "upserting slot B leaves slot A's :ops identical?"
      (let [store' (ss/upsert-slot store vi-b {:tree tree-b :container 1})]
        (is (identical? ops-a (:ops (ss/slot store' vi-a))))))
    (testing "an address-level write on B's address leaves A's :ops identical?, rebuilds B's"
      (let [store2 (ss/upsert-slot store vi-b {:tree tree-b :container 1})
            ops-b0 (:ops (ss/slot store2 vi-b))
            store3 (ss/update-nodes-by-address
                    store2 "addr/b" (fn [n] (assoc-in n [:style :bg] [1 0 0 1])))]
        (is (identical? ops-a (:ops (ss/slot store3 vi-a)))
            "A untouched — ops identical")
        (is (not (identical? ops-b0 (:ops (ss/slot store3 vi-b))))
            "B rebuilt — ops reallocated")))))

;; ---------------------------------------------------------------------------
;; Store — index lifecycle (remove + re-upsert remap)
;; ---------------------------------------------------------------------------

(deftest store-remove-slot-prunes-index
  (let [vi    [:vi :face 1]
        tree  (rt/rt-node :root :box {:x 0 :y 0 :w 10 :h 10} :data {:address "addr/z"})
        store (-> (ss/empty-store) (ss/upsert-slot vi {:tree tree :container 1}))]
    (is (= #{vi} (ss/slots-for-address store "addr/z")))
    (let [store' (ss/remove-slot store vi)]
      (is (nil? (ss/slot store' vi)) "slot dropped")
      (is (= #{} (ss/slots-for-address store' "addr/z")) "index pruned to empty set")
      (is (not (contains? (:index store') "addr/z")) "emptied address entry removed"))))

(deftest store-reupsert-remaps-index
  (let [vi     [:vi :face 1]
        tree-x (rt/rt-node :r :box {:x 0 :y 0 :w 10 :h 10} :data {:address "addr/x"})
        tree-y (rt/rt-node :r :box {:x 0 :y 0 :w 10 :h 10} :data {:address "addr/y"})
        store  (-> (ss/empty-store) (ss/upsert-slot vi {:tree tree-x :container 1}))
        store' (ss/upsert-slot store vi {:tree tree-y :container 1})]
    (testing "re-upsert drops the vi's OLD address and adds the NEW one (no scan)"
      (is (= #{} (ss/slots-for-address store' "addr/x")) "old address released")
      (is (= #{vi} (ss/slots-for-address store' "addr/y")) "new address indexed")
      (is (not (contains? (:index store') "addr/x")) "emptied old entry pruned"))))

(deftest store-update-root-addressed-node
  ;; the [] root index-path: update-in with [] is unsafe, so the root is
  ;; special-cased in update-node-at — exercise that branch directly.
  (let [vi     [:vi :face 1]
        tree   (rt/rt-node :root :box {:x 0 :y 0 :w 10 :h 10} :data {:address "addr/root"})
        store  (-> (ss/empty-store) (ss/upsert-slot vi {:tree tree :container 1}))
        store' (ss/update-nodes-by-address store "addr/root"
                                           (fn [n] (assoc-in n [:data :marked?] true)))]
    (is (= [] (first (get-in (ss/slot store vi) [:addresses "addr/root"])))
        "the root node carries the empty index-path")
    (is (true? (get-in (ss/slot store' vi) [:tree :data :marked?]))
        "the root node itself was patched via the [] path")))

;; ---------------------------------------------------------------------------
;; Containers — composition, inverse, camera inheritance, guards
;; ---------------------------------------------------------------------------

(deftest containers-nested-composition
  ;; grandparent(1) → parent(2) → child(3); assert EXACT composed transforms.
  (let [reg (-> (ctn/empty-registry)
                (ctn/add-container 1 {:x 100 :y 0 :scale 2})
                (ctn/add-container 2 {:parent 1 :x 10 :y 5 :scale 3})
                (ctn/add-container 3 {:parent 2 :x 1 :y 1 :scale 4}))
        eff (ctn/effective reg)]
    (testing "eff-scale multiplies down the chain; eff-offset composes with parent scale"
      (is (= {:x 100.0 :y 0.0  :scale 2.0  :flags 0 :layer 0} (get eff 1)))
      (is (= {:x 120.0 :y 10.0 :scale 6.0  :flags 0 :layer 0} (get eff 2)))
      (is (= {:x 126.0 :y 16.0 :scale 24.0 :flags 0 :layer 0} (get eff 3))))
    (testing "cid 0 stays identity/world"
      (is (= {:x 0.0 :y 0.0 :scale 1.0 :flags 0 :layer 0} (get eff 0))))))

(deftest containers-inverse-point-roundtrip
  (let [reg   (-> (ctn/empty-registry)
                  (ctn/add-container 1 {:x 100 :y 0 :scale 2})
                  (ctn/add-container 2 {:parent 1 :x 10 :y 5 :scale 3})
                  (ctn/add-container 3 {:parent 2 :x 1 :y 1 :scale 4}))
        e3    (get (ctn/effective reg) 3)          ; {x126 y16 s24}
        local [2 3]
        ;; forward: world = eff-offset + local * eff-scale
        world [(+ (:x e3) (* (first local)  (:scale e3)))
               (+ (:y e3) (* (second local) (:scale e3)))]]
    (is (= [174.0 88.0] world) "forward transform")
    (is (= [2.0 3.0] (ctn/inverse-point e3 world)) "inverse recovers local")))

(deftest containers-screen-camera-inheritance
  (let [reg (-> (ctn/empty-registry)
                (ctn/add-container 1 {:camera :screen})   ; explicit screen
                (ctn/add-container 2 {:parent 1})          ; inherit → screen
                (ctn/add-container 3 {}))                  ; default → world
        eff (ctn/effective reg)]
    (is (= 1 (:flags (get eff 1))) "explicit :screen → flag set")
    (is (= 1 (:flags (get eff 2))) "child inherits :screen through the chain")
    (is (= 0 (:flags (get eff 3))) "default resolves to :world")
    (is (= 0 (:flags (get eff 0))) "cid 0 is world")))

(deftest containers-cid0-reserved
  (let [reg (ctn/empty-registry)]
    (testing "adding cid 0 throws"
      (is (thrown? clojure.lang.ExceptionInfo (ctn/add-container reg 0 {:x 5}))))
    (testing "mutating cid 0 throws"
      (is (thrown? clojure.lang.ExceptionInfo (ctn/set-transform reg 0 {:x 5}))))))

(deftest containers-add-guards-and-partial-set
  (let [reg (-> (ctn/empty-registry) (ctn/add-container 1 {:x 5}))]
    (testing "adding an existing cid throws"
      (is (thrown? clojure.lang.ExceptionInfo (ctn/add-container reg 1 {:x 9}))))
    (testing "a non-existent parent throws"
      (is (thrown? clojure.lang.ExceptionInfo (ctn/add-container reg 2 {:parent 99}))))
    (testing "set-transform does PARTIAL updates; effective reflects them"
      (let [reg' (ctn/set-transform reg 1 {:x 50})
            e1   (get (ctn/effective reg') 1)]
        (is (= 50.0 (:x e1)) "x updated")
        (is (= 0.0  (:y e1)) "y untouched (default)")
        (is (= 1.0  (:scale e1)) "scale untouched (default)")))))

(deftest containers-parent-cycle-throws
  ;; Cycles cannot form through add-container/set-transform (no re-parenting),
  ;; so hand-build a cyclic registry to exercise effective's guard.
  (let [cyclic {:containers {0 {:parent nil :x 0.0 :y 0.0 :scale 1.0 :camera :world :layer 0}
                             1 {:parent 2 :x 0.0 :y 0.0 :scale 1.0 :camera nil :layer 0}
                             2 {:parent 1 :x 0.0 :y 0.0 :scale 1.0 :camera nil :layer 0}}}]
    (is (thrown? clojure.lang.ExceptionInfo (ctn/effective cyclic)))))

;; ---------------------------------------------------------------------------
;; Pick — layer ordering, miss, deepest-addressed, context extraction
;; ---------------------------------------------------------------------------

(deftest pick-layer-and-miss
  (let [reg     (-> (ctn/empty-registry)
                    (ctn/add-container 10 {:x 0 :y 0 :scale 1 :layer 1})
                    (ctn/add-container 20 {:x 0 :y 0 :scale 1 :layer 5}))
        effs    (ctn/effective reg)
        vi-lo   [:vi :lo 1]
        vi-hi   [:vi :hi 1]
        tree-lo (rt/rt-node :rlo :box {:x 0 :y 0 :w 100 :h 100} :data {:address "addr/lo"})
        tree-hi (rt/rt-node :rhi :box {:x 0 :y 0 :w 100 :h 100} :data {:address "addr/hi"})
        store   (-> (ss/empty-store)
                    (ss/upsert-slot vi-lo {:tree tree-lo :container 10})
                    (ss/upsert-slot vi-hi {:tree tree-hi :container 20}))]
    (testing "overlapping containers: the higher :layer wins"
      (let [p (ss/pick store effs [50 50])]
        (is (= "addr/hi" (:address p)))
        (is (= vi-hi (:vi p)))))
    (testing "a point outside every tree → nil"
      (is (nil? (ss/pick store effs [9999 9999]))))))

(deftest pick-deepest-addressed-ancestor-fallback
  ;; hit leaf carries NO address; an ancestor does → the ancestor's address wins.
  (let [reg   (-> (ctn/empty-registry)
                  (ctn/add-container 1 {:x 0 :y 0 :scale 1 :layer 1}))
        effs  (ctn/effective reg)
        vi    [:vi :nested 1]
        tree  (rt/rt-node :outer :box {:x 0 :y 0 :w 100 :h 100}
                          :data {:address "addr/outer"}
                          :children [(rt/rt-node :inner :box {:x 10 :y 10 :w 50 :h 50}
                                                 :data {:role :decoration})]) ; NO :address
        store (-> (ss/empty-store) (ss/upsert-slot vi {:tree tree :container 1}))
        p     (ss/pick store effs [20 20])]
    (testing "the inner leaf is the hit, but the addressed ancestor is returned"
      (is (= "addr/outer" (:address p)))
      (is (= vi (:vi p)))
      (is (= :inner (:id (last (:path p)))) "the hit path's leaf is the non-addressed inner node"))))

(deftest pick-carries-actions-and-src-path
  (let [reg   (-> (ctn/empty-registry) (ctn/add-container 1 {:layer 1}))
        effs  (ctn/effective reg)
        vi    [:vi :face 1]
        tree  (rt/rt-node :root :box {:x 0 :y 0 :w 100 :h 100}
                          :children [(rt/rt-node :leaf :box {:x 0 :y 0 :w 100 :h 100}
                                                 :data {:address "addr/x"
                                                        :actions [{:action :block/focus
                                                                   :target "addr/x"}]
                                                        :assembly/src-path [:turns 0 :blocks 2]})])
        store (-> (ss/empty-store) (ss/upsert-slot vi {:tree tree :container 1}))
        p     (ss/pick store effs [10 10])]
    (is (= "addr/x" (:address p)))
    (is (= [{:action :block/focus :target "addr/x"}] (:actions p)) "actions from [:data :actions]")
    (is (= [:turns 0 :blocks 2] (:src-path p)) "src-path from [:data :assembly/src-path]")
    (is (= [10.0 10.0] (:point-local p)) "point-local is the container-local point")))

;; ============================================================================
;; Falsification-pass regressions (wave finder 2026-07-12, findings #1 and #4)
;; ============================================================================

(deftest nested-same-address-write-applies-deepest-first
  ;; Finding #1: one address on a node AND its ancestor. A shallow-first fold
  ;; could detach the descendant and hand f a nil node. Deepest-first must
  ;; leave the tree well-formed even when f rewrites :children.
  (let [vi    [:vi :nested-addr 1]
        tree  (rt/rt-node :root :box {:x 0 :y 0 :w 100 :h 100}
                          :data {:address "addr/both"}
                          :children [(rt/rt-node :child :box {:x 0 :y 0 :w 50 :h 50}
                                                 :data {:address "addr/both"})])
        store (-> (ss/empty-store) (ss/upsert-slot vi {:tree tree :container 0}))
        ;; f drops all children — applied to the root FIRST this orphans the
        ;; child path and injects a garbage node.
        st'   (ss/update-nodes-by-address store "addr/both"
                                          (fn [n] (assoc n :children [])))
        tree' (:tree (ss/slot st' vi))]
    (is (= [] (:children tree')) "root's children dropped by f")
    (is (every? some? (tree-seq map? :children tree'))
        "no nil/garbage nodes anywhere in the rebuilt tree")
    (is (= #{[]} (get-in (ss/slot st' vi) [:addresses "addr/both"]))
        "index re-collected from the rebuilt tree: only the root remains")))

;; ============================================================================
;; P3a — face-path producer helper + plurality (CONTRACT §8 G7, JVM half)
;; ============================================================================

(deftest g7-stamp-block-addresses
  ;; The assembly interpreter threads a block's unit-id into node :ids
  ;; (face_assembly trap T6), never into :data. stamp-block-addresses lifts it
  ;; to [:data :address] — which the store indexes (CONTRACT §5) — while :id
  ;; stays intact for the legacy hit-test path (CONTRACT §3).
  (let [unit-ids #{"blk-1" "blk-2"}
        tree (rt/rt-node [:vi "face"] :box {:x 0 :y 0 :w 300 :h 100}
                         :children
                         [(rt/rt-node [:vi "face" "blk-1"] :box {:x 0 :y 0 :w 100 :h 40}
                                      :children [(rt/rt-node [:vi "face" "blk-1" :hdr]
                                                             :box {:x 0 :y 0 :w 100 :h 20})])
                          (rt/rt-node [:vi "face" "blk-2"] :box {:x 100 :y 0 :w 100 :h 40})
                          (rt/rt-node [:vi "face" :chrome] :box {:x 0 :y 60 :w 300 :h 20})])
        stamped (ss/stamp-block-addresses tree unit-ids)]
    (is (= "blk-1" (get-in stamped [:children 0 :data :address])) "block node addressed")
    (is (= "blk-1" (get-in stamped [:children 0 :children 0 :data :address]))
        "block descendant inherits the block's address (id prefix shared)")
    (is (= "blk-2" (get-in stamped [:children 1 :data :address])))
    (is (nil? (get-in stamped [:data :address])) "root (no unit-id in :id) untouched")
    (is (nil? (get-in stamped [:children 2 :data :address])) "chrome node untouched")
    (is (= [:vi "face" "blk-1"] (get-in stamped [:children 0 :id])) ":id left intact")))

(deftest g7-plurality-two-instances-one-tree-family
  ;; TWO reader-face view-instances of ONE conversation, independent containers;
  ;; an address-level write patches BOTH; pick through DIFFERENT container
  ;; transforms returns the same address, different vi.
  (let [unit-ids #{"blk-1" "blk-2"}
        tree (ss/stamp-block-addresses
              (rt/rt-node [:vi "face"] :box {:x 0 :y 0 :w 300 :h 100}
                          :children
                          [(rt/rt-node [:vi "face" "blk-1"] :box {:x 0 :y 0 :w 100 :h 40})
                           (rt/rt-node [:vi "face" "blk-2"] :box {:x 100 :y 0 :w 100 :h 40})])
              unit-ids)
        vi-a  [:vi :reader-face 1]
        vi-b  [:vi :reader-face 2]
        ;; container cids in the P3 face range; B offset 700px right, layer above
        reg   (-> (ctn/empty-registry)
                  (ctn/add-container 101 {:x 0   :y 0 :scale 1 :layer 1 :camera :world})
                  (ctn/add-container 102 {:x 700 :y 0 :scale 1 :layer 2 :camera :world}))
        effs  (ctn/effective reg)
        store (-> (ss/empty-store)
                  (ss/upsert-slot vi-a {:tree tree :container 101})
                  (ss/upsert-slot vi-b {:tree tree :container 102}))]
    (testing "ONE block address fans out to BOTH instances via the index"
      (is (= #{vi-a vi-b} (ss/slots-for-address store "blk-1")))
      (is (= #{vi-a vi-b} (ss/slots-for-address store "blk-2"))))
    (testing "an address-level write (edit echo) patches BOTH slots' trees"
      (let [st' (ss/update-nodes-by-address
                 store "blk-2" (fn [n] (assoc-in n [:data :edited?] true)))]
        (is (true? (get-in (ss/slot st' vi-a) [:tree :children 1 :data :edited?])) "A patched")
        (is (true? (get-in (ss/slot st' vi-b) [:tree :children 1 :data :edited?])) "B patched")))
    (testing "pick through DIFFERENT container transforms → same address, different vi"
      ;; blk-1 local x[0,100] y[0,40]; A container (0,0) → world same; B (700,0)
      ;; → world x[700,800]. Probe each instance's blk-1.
      (let [pa (ss/pick store effs [50 20])
            pb (ss/pick store effs [750 20])]
        (is (= "blk-1" (:address pa)))
        (is (= "blk-1" (:address pb)))
        (is (= vi-a (:vi pa)))
        (is (= vi-b (:vi pb)))
        (is (not= (:vi pa) (:vi pb)) "same address, different appearance")
        (is (= [50.0 20.0] (:point-local pa)) "A container-local point")
        (is (= [50.0 20.0] (:point-local pb)) "B container-local point (inverse of the 700 offset)")))))

;; ============================================================================
;; P3b Rung 1 — per-vi face build fn + despawn/index cleanup (CONTRACT §8 G7)
;; ============================================================================

(def ^:private p3b-registry
  "A trivial builder registry (the §6 interface: fn [ctx props children] → rt-node)
   — enough to exercise build-face-tree JVM-side without the cljs face-primitives."
  {:box   (fn [ctx _props children]
            (rt/rt-node (:id ctx) :box {:x 0 :y 0 :w 300 :h 40}
                        :children (vec children)))
   :block (fn [ctx _props children]
            (rt/rt-node (:id ctx) :box {:x 0 :y 0 :w 300 :h 20}
                        :children (vec children)))})

(def ^:private p3b-assembly
  "root box → each turn → each block. Block item :id drives the built node id
   (face_assembly expand-slot seg), which stamp-block-addresses lifts to
   [:data :address]."
  {:assembly/name    "p3b-face"
   :assembly/grammar 0
   :root {:prim :box
          :children [{:each [:turns]
                      :template {:prim :box
                                 :children [{:each [:blocks]
                                             :template {:prim :block}}]}}]}})

(def ^:private p3b-projection
  {:turns [{:blocks [{:id "blk-1"} {:id "blk-2"}]}]
   :conversation/address "conv/1"})

(defn- tree-addresses [tree]
  (into #{} (keep #(get-in % [:data :address])) (tree-seq map? :children tree)))

(deftest build-face-tree-stamps-addresses-and-resolves
  (let [compiled (fa/compile-assembly p3b-registry p3b-assembly)
        _        (is (not (fa/error? compiled)) "the test assembly compiles clean")
        uids     (into #{} (comp (mapcat :blocks) (keep :id)) (:turns p3b-projection))
        vi       [:vi :reader-face 1]
        view-ctx {:view-instance vi :address "conv/1"
                  :geom {:content-w 300 :font-size 14 :line-height 20 :char-advance 8}}
        tree     (ss/build-face-tree compiled p3b-projection view-ctx uids)]
    (testing "block unit-ids are lifted to [:data :address] for the store index"
      ;; the root also carries the conversation address (apply-assembly stamp-root)
      (is (clojure.set/subset? #{"blk-1" "blk-2"} (tree-addresses tree)))
      (is (= "conv/1" (get-in tree [:data :address])) "root addressed by the conversation"))
    (testing "the tree is a RESOLVED rt-tree (arrange pass ran — bounds present)"
      (is (map? (:bounds tree)))
      (is (number? (get-in tree [:bounds :h]))))
    (testing "same (compiled, projection, view-ctx, uids) → an EQUAL tree (pure)"
      (is (= tree (ss/build-face-tree compiled p3b-projection view-ctx uids))))))

(deftest build-face-tree-different-faces-one-projection
  ;; The MINDBLOW in the pure layer: TWO different compiled faces over the SAME
  ;; projection produce two DIFFERENT trees that nonetheless carry the SAME block
  ;; addresses — so an address-level echo fans into both, each keeping its shape.
  (let [face-a   (fa/compile-assembly p3b-registry p3b-assembly)
        ;; face B: same data, a DIFFERENT arrangement (blocks wrapped one level
        ;; deeper) → a structurally different tree, identical addresses.
        asm-b    (assoc p3b-assembly :assembly/name "p3b-face-b"
                        :root {:prim :box
                               :children [{:prim :box
                                           :children [{:each [:turns]
                                                       :template {:prim :box
                                                                  :children [{:each [:blocks]
                                                                              :template {:prim :block}}]}}]}]})
        face-b   (fa/compile-assembly p3b-registry asm-b)
        uids     (into #{} (comp (mapcat :blocks) (keep :id)) (:turns p3b-projection))
        geom     {:content-w 300 :font-size 14 :line-height 20 :char-advance 8}
        tree-a   (ss/build-face-tree face-a p3b-projection
                                     {:view-instance [:vi :reader-face 1] :address "conv/1" :geom geom} uids)
        tree-b   (ss/build-face-tree face-b p3b-projection
                                     {:view-instance [:vi :reader-face 2] :address "conv/1" :geom geom} uids)]
    (is (not= tree-a tree-b) "two faces render the same conversation differently")
    (is (= (tree-addresses tree-a) (tree-addresses tree-b))
        "both carry the SAME address set — one edit echoes into both")
    (is (clojure.set/subset? #{"blk-1" "blk-2"} (tree-addresses tree-a))
        "both carry the block addresses")
    (testing "each lands as a store slot; the index fans one address to both vis"
      (let [store (-> (ss/empty-store)
                      (ss/upsert-slot [:vi :reader-face 1] {:tree tree-a :container 33})
                      (ss/upsert-slot [:vi :reader-face 2] {:tree tree-b :container 34}))]
        (is (= #{[:vi :reader-face 1] [:vi :reader-face 2]}
               (ss/slots-for-address store "blk-1")))))))

(deftest g8-per-slot-text-identity-survives-sibling-edit
  ;; The store-side guarantee the P3b Rung-2 per-slot text geo skip depends on:
  ;; container-idx is baked at upsert, so a slot's :ops :text is a stable vector
  ;; that stays identical? when a SIBLING slot changes. render's reconcile then
  ;; skips that slot's geo (zero GPU writes) — G8 isolation.
  (let [tree-a (rt/rt-node :a :box {:x 0 :y 0 :w 100 :h 40}
                           :children [(rt/rt-node :la :text {:x 0 :y 0 :w 100 :h 40}
                                                  :text [{:text "hello" :type :text :x 0 :y 0 :size 14}]
                                                  :data {:address "addr/a"})])
        tree-b (rt/rt-node :b :box {:x 0 :y 0 :w 100 :h 40}
                           :data {:address "addr/b"})
        vi-a   [:vi :reader-face 1]
        vi-b   [:vi :reader-face 2]
        store  (-> (ss/empty-store) (ss/upsert-slot vi-a {:tree tree-a :container 33}))
        text-a (get-in (ss/slot store vi-a) [:ops :text])]
    (testing "container-idx is baked into the slot's ops at upsert"
      (is (= 33 (get-in (first (first text-a)) [:container-idx]))
          "each text op carries its slot's container index"))
    (testing "upserting a SIBLING leaves slot A's :ops :text identical? (geo skip)"
      (let [store' (ss/upsert-slot store vi-b {:tree tree-b :container 34})]
        (is (identical? text-a (get-in (ss/slot store' vi-a) [:ops :text]))
            "A's text vector is the SAME object — its per-slot geo is skipped")))))

(deftest remove-container-drops-and-guards
  (let [reg (-> (ctn/empty-registry)
                (ctn/add-container 33 {:x 100 :y 0 :scale 1 :layer 1})
                (ctn/add-container 34 {:x 700 :y 0 :scale 1 :layer 2}))]
    (testing "removing a leaf container drops it from :containers and :effective"
      (let [reg' (ctn/remove-container reg 33)]
        (is (not (contains? (:containers reg') 33)) "container 33 gone")
        (is (contains? (:containers reg') 34) "sibling untouched")
        (is (not (contains? (ctn/effective reg') 33)) "no effective entry for a removed cid")))
    (testing "cid 0 is reserved, unknown cids and parents-of-children are refused"
      (is (thrown? clojure.lang.ExceptionInfo (ctn/remove-container reg 0)))
      (is (thrown? clojure.lang.ExceptionInfo (ctn/remove-container reg 999)))
      (let [nested (ctn/add-container reg 35 {:parent 34})]
        (is (thrown? clojure.lang.ExceptionInfo (ctn/remove-container nested 34))
            "removing a container with a child throws (would orphan)")))))

(deftest despawn-removes-slot-and-container-together
  ;; close-instance! (cljs) composes ss/remove-slot + ctn/remove-container +
  ;; !vi-faces dissoc; the two PURE halves are exercised here as one lifecycle.
  (let [vi    [:vi :reader-face 1]
        tree  (rt/rt-node :root :box {:x 0 :y 0 :w 10 :h 10} :data {:address "blk-1"})
        store (-> (ss/empty-store) (ss/upsert-slot vi {:tree tree :container 33}))
        reg   (-> (ctn/empty-registry) (ctn/add-container 33 {:x 0 :y 0 :scale 1 :layer 1}))
        store' (ss/remove-slot store vi)
        reg'   (ctn/remove-container reg 33)]
    (is (nil? (ss/slot store' vi)) "slot dropped")
    (is (= #{} (ss/slots-for-address store' "blk-1")) "index pruned")
    (is (not (contains? (:index store') "blk-1")) "emptied address entry removed")
    (is (not (contains? (:containers reg') 33)) "its container removed — no orphan")))

(deftest store-fns-free?-sees-metadata-closures
  ;; Finding #4: a closure smuggled in metadata survives BOTH pr-str
  ;; round-trip (printing drops meta) and a keys/vals-only walk.
  (let [vi    [:vi :meta 1]
        tree  (rt/rt-node :root :box {:x 0 :y 0 :w 10 :h 10}
                          :data {:address "addr/m"})
        store (-> (ss/empty-store) (ss/upsert-slot vi {:tree tree :container 0}))
        dirty (update-in store [:slots vi :meta] assoc
                         :note (with-meta {:k 1} {:handler (fn [] :boom)}))]
    (is (ss/store-fns-free? store) "clean store passes")
    (is (not (ss/store-fns-free? dirty)) "metadata closure is caught")))
