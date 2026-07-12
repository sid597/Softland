(ns app.client.workspace.scene-store
  "The one client scene store (scene-substrate P1): view-instances → resolved
   rect-trees, keyed for address fan-out, picked through per-container transforms.

   PURE .cljc: pure functions over an EDN store value, JVM- and CLJS-runnable.
   No atoms, no interop, no renderer imports. The one runtime atom that holds the
   live store, and the GPU upload, live at the reduce/consumer edge elsewhere
   (CONTRACT §5, traps T3/T4) — NOT here.

   Store value: {:slots {vi → slot} :index {address → #{vi}}}. A slot is one
   view-instance's resolved tree + flattened ops + its address→paths subtree
   index (CONTRACT §5):
     {:vi <edn> :container <int> :tree <resolved rt-tree, container-LOCAL>
      :ops {:text [...] :rects [...] :shadows [...]}
      :addresses {address → #{index-path}} :meta {...} :stratum :world|:overlay}
   The :index fan-out is maintained incrementally by upsert/remove, NEVER
   recomputed by scanning slots at read time. Resolution route:
   address → :index → vis → slot :addresses → index-paths → nodes."
  (:require [app.client.workspace.rect-tree :as rt]
            [app.client.workspace.containers :as containers]))

;; ============================================================================
;; Slot construction — resolve + flatten + subtree address index
;; ============================================================================

(defn- collect-addresses
  "Walk a RESOLVED tree; return {address → #{index-path}} for every node whose
   [:data :address] is non-nil. An index-path is a get-in/update-in vector:
   [] for the root, [:children i] for a child, [:children i :children j] deeper."
  [tree]
  (letfn [(walk [node path acc]
            (let [addr (get-in node [:data :address])
                  acc  (if (some? addr)
                         (update acc addr (fnil conj #{}) path)
                         acc)]
              (reduce-kv (fn [a i child]
                           (walk child (conj path :children i) a))
                         acc
                         (:children node))))]
    (walk tree [] {})))

(defn- flatten-ops
  "Flatten a resolved tree into GPU-ready op arrays via the rect-tree grammar.
   Computed ONCE at upsert; unchanged slots keep the SAME arrays frame-over-frame
   (trap T5 / G3 identity preservation)."
  [tree]
  {:text    (rt/tree->text-ops tree)
   :rects   (rt/tree->rects tree)
   :shadows (rt/tree->shadows tree)})

(defn- build-slot
  "Resolve → flatten → index a tree into a slot value. The ONE slot-building
   path, shared by upsert-slot and update-nodes-by-address so ops are computed
   on a single code path (trap T5)."
  [vi {:keys [tree container meta stratum]}]
  (let [resolved (rt/resolve-layout tree)]
    {:vi        vi
     :container container
     :tree      resolved
     :ops       (flatten-ops resolved)
     :addresses (collect-addresses resolved)
     :meta      (or meta {})
     :stratum   (or stratum :world)}))

;; ============================================================================
;; Fan-out index maintenance (incremental — never a full scan, CONTRACT §5)
;; ============================================================================

(defn- index-disj
  "Remove vi from every key of `addresses`; drop emptied entries."
  [index vi addresses]
  (reduce (fn [idx addr]
            (let [s (disj (get idx addr #{}) vi)]
              (if (empty? s) (dissoc idx addr) (assoc idx addr s))))
          index
          (keys addresses)))

(defn- index-conj
  "Add vi under every key of `addresses`."
  [index vi addresses]
  (reduce (fn [idx addr] (update idx addr (fnil conj #{}) vi))
          index
          (keys addresses)))

;; ============================================================================
;; Store API
;; ============================================================================

(defn empty-store
  "The empty store value."
  []
  {:slots {} :index {}})

(defn slot
  "The slot for vi, or nil."
  [store vi]
  (get-in store [:slots vi]))

(defn slots-for-address
  "The set of view-instances that render `address` (empty set when none). Read
   straight off the maintained :index — no scan (CONTRACT §5)."
  [store address]
  (get-in store [:index address] #{}))

(defn upsert-slot
  "Insert or replace vi's slot from opts {:tree :container :meta :stratum},
   resolving + flattening + indexing the tree (build-slot), and updating the
   fan-out :index incrementally: drop the OLD slot's addresses, add the new ones
   (CONTRACT §5 — never rebuilt by scan). Untouched slots keep their identical?
   ops (trap T5 / G3)."
  [store vi opts]
  (let [old   (get-in store [:slots vi])
        slot' (build-slot vi opts)
        index (-> (:index store)
                  (cond-> old (index-disj vi (:addresses old)))
                  (index-conj vi (:addresses slot')))]
    (-> store
        (assoc-in [:slots vi] slot')
        (assoc :index index))))

(defn remove-slot
  "Drop vi's slot and prune it from the fan-out :index (emptied address entries
   disappear). No-op when vi is absent."
  [store vi]
  (if-let [old (get-in store [:slots vi])]
    (-> store
        (update :slots dissoc vi)
        (update :index index-disj vi (:addresses old)))
    store))

(defn- update-node-at
  "Apply f to the node at index-path in tree. [] targets the root itself
   (update-in with an empty path is unsafe, so the root is special-cased)."
  [tree path f]
  (if (seq path) (update-in tree path f) (f tree)))

(defn update-nodes-by-address
  "Address-level write: for EVERY view-instance that renders `address`, apply f
   (node → node) to each of that slot's nodes carrying the address, then rebuild
   the slot through the upsert path (re-resolve + re-flatten + re-index). Slots
   that do not render the address are never touched, so their ops stay
   identical? (trap T5). This is the Δ1 fan-out: one address, every appearance."
  [store address f]
  (reduce
   (fn [st vi]
     (let [s     (get-in st [:slots vi])
           ;; Deepest paths FIRST: when one address sits on both a node and
           ;; its ancestor, mutating the ancestor first could detach the
           ;; descendant and hand f a nil node (falsification finding #1).
           paths (sort-by count > (get-in s [:addresses address]))
           tree' (reduce (fn [t path] (update-node-at t path f))
                         (:tree s)
                         paths)]
       (upsert-slot st vi {:tree      tree'
                           :container (:container s)
                           :meta      (:meta s)
                           :stratum   (:stratum s)})))
   store
   (slots-for-address store address)))

;; ============================================================================
;; Pick — the address/context seam (CONTRACT §5, trap T8)
;; ============================================================================

(defn- deepest-addressed
  "Given a hit-test result (nodes root→leaf), return the deepest node carrying
   [:data :address], or nil. Deepest-first so an addressed ancestor is returned
   when the hit leaf itself has no address."
  [path]
  (some (fn [n] (when (get-in n [:data :address]) n)) (rseq path)))

(defn pick
  "Resolve a world/screen point to an address through the container transforms.
   `effective-transforms` = containers/effective output {cid → eff}. Containers
   are tried topmost-first (:layer descending); the point is inverse-transformed
   into each container's LOCAL space BEFORE hit-test (trap T8); slots in that
   container are hit-tested in a deterministic order (vis sorted by pr-str). The
   first slot hit whose path contains an addressed node wins, returning
   {:vi :path :address :src-path :actions :point-local}. Miss everywhere → nil."
  [store effective-transforms point]
  (let [cids (sort-by (juxt #(- (:layer (get effective-transforms %)))
                            #(pr-str %))
                      (keys effective-transforms))]
    (some
     (fn [cid]
       (let [eff       (get effective-transforms cid)
             [lx ly]   (containers/inverse-point eff point)
             cid-slots (->> (vals (:slots store))
                            (filter #(= cid (:container %)))
                            (sort-by (comp pr-str :vi)))]
         (some
          (fn [s]
            (when-let [path (rt/hit-test (:tree s) lx ly)]
              (when-let [node (deepest-addressed path)]
                {:vi          (:vi s)
                 :path        path
                 :address     (get-in node [:data :address])
                 :src-path    (get-in node [:data :assembly/src-path])
                 :actions     (get-in node [:data :actions])
                 :point-local [lx ly]})))
          cid-slots)))
     cids)))

;; ============================================================================
;; Producer helper (P3 face path) — stamp block addresses onto a built tree
;; ============================================================================

(defn stamp-block-addresses
  "Face-path producer helper (pure, JVM-testable). The store indexes ONLY
   [:data :address] (CONTRACT §5), but the assembly interpreter threads a
   block's unit-id into node :ids (face_assembly trap T6), not into :data.
   Walk a RESOLVED tree; for every node whose :id vector contains one of
   `unit-ids`, set [:data :address] to that unit-id — so the store fan-out
   indexes blocks by unit-id while :id stays intact for the legacy cached-scene
   hit-test path (CONTRACT §3). A node's :id carries at most one block unit-id
   (blocks don't nest), so the first match is unambiguous; a block's descendants
   share its unit-id prefix and so inherit its address (a click on any pixel of
   a block resolves to that block). Nodes with no matching id are untouched."
  [tree unit-ids]
  (letfn [(walk [node]
            (let [id   (:id node)
                  addr (when (and (vector? id) (seq unit-ids))
                         (some unit-ids id))
                  node (if addr (assoc-in node [:data :address] addr) node)]
              (if (seq (:children node))
                (update node :children (fn [cs] (mapv walk cs)))
                node)))]
    (walk tree)))

;; ============================================================================
;; Serializability guard (G2 / trap T1) — actions are DATA, never closures
;; ============================================================================

(defn store-fns-free?
  "True iff a full recursive walk of the store (trees, ops, meta, index, keys
   AND vals — everything) finds zero fn values. Actions live as descriptor maps;
   a closure anywhere makes the scene non-serializable and agent-illegible
   (trap T1). G2's assertion helper."
  [store]
  (letfn [(has-fn? [x]
            (cond
              (fn? x)   true
              ;; metadata can smuggle a closure past both this walk and the
              ;; EDN round-trip (printing drops meta) — walk it explicitly
              ;; (falsification finding #4)
              (and (meta x) (has-fn? (meta x))) true
              (map? x)  (boolean (some has-fn? (concat (keys x) (vals x))))
              (coll? x) (boolean (some has-fn? x))
              :else     false))]
    (not (has-fn? store))))
