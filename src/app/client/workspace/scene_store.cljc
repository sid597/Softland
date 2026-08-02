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
            [app.client.workspace.containers :as containers]
            [app.client.workspace.face-assembly :as fa]
            [app.shared.material-inspector :as material-inspector]))

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

(defn- stamp-ops-container
  "Bake the slot's compact transform-table index onto every flattened op so the GPU places each
   glyph/rect/shadow through its container transform (P2 plumbing: pack-rect,
   pack-shadow, shape-* all read :container-idx). Done HERE at upsert (P3b Rung 2),
   not per-frame, so a slot's ops stay identity-stable across a SIBLING slot's
   change — the per-slot text geo's identical?-skip (G8) depends on it (trap T5).
   :container-slot is stable for a slot's whole life (move/affine changes the
   registry transform, not the slot). Text ops are nested [[op..]..] (lines);
   rects and shadows are flat."
  [ops container-slot]
  (let [slot (or container-slot 0)]
    {:text    (mapv (fn [line] (mapv #(assoc % :container-idx slot) line)) (:text ops))
     :rects   (mapv #(assoc % :container-idx slot) (:rects ops))
     :shadows (mapv #(assoc % :container-idx slot) (:shadows ops))}))

(defn- build-slot
  "Resolve → flatten → stamp container-idx → index a tree into a slot value. The
   ONE slot-building path, shared by upsert-slot and update-nodes-by-address so
   ops are computed on a single code path (trap T5).

   `:pre-resolved? true` (first-light P1) skips the resolve pass for trees that
   ALREADY went through resolve-layout (build-face-tree output — apply-assembly
   resolves internally). resolve-layout is idempotent on a resolved tree, so
   the skip changes nothing semantically; it removes a full-tree pass from the
   per-keystroke main-face rebuild. Opt-in only — mutated trees
   (update-nodes-by-address) still resolve."
  [vi {:keys [tree container container-slot meta stratum pre-resolved?]}]
  (let [resolved (if pre-resolved? tree (rt/resolve-layout tree))]
    {:vi        vi
     :container container
     :container-slot (or container-slot container 0)
     :tree      resolved
     :ops       (stamp-ops-container (flatten-ops resolved)
                                     (or container-slot container 0))
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
  "Insert or replace vi's slot from opts
   {:tree :container :container-slot :meta :stratum},
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
                           :container-slot (:container-slot s)
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
            (let [id    (:id node)
                  addr  (when (and (vector? id) (seq unit-ids))
                          (some unit-ids id))
                  cs    (:children node)
                  ;; structure-sharing (first-light P1): return the SAME child
                  ;; vector — and the SAME node — when nothing below changed,
                  ;; so the per-keystroke stamp pass allocates only along
                  ;; stamped paths instead of rebuilding the whole tree.
                  cs'   (when (seq cs)
                          (let [mapped (mapv walk cs)]
                            (if (some identity (map #(when-not (identical? %1 %2) true)
                                                    mapped cs))
                              mapped
                              cs)))
                  node' (cond-> node
                          addr (assoc-in [:data :address] addr)
                          (and cs' (not (identical? cs' cs))) (assoc :children cs'))]
              node'))]
    (walk tree)))

(defn block-unit-ids
  "The set of block unit-ids in a served face data-context (turns → blocks →
   :id). PURE (first-light P1: moved here from scene-runtime so the store path
   and every pick consumer resolve identical blocks from one derivation)."
  [ctx]
  (into #{} (comp (mapcat :blocks) (keep :id)) (:turns ctx)))

;; ============================================================================
;; Per-view-instance face build (P3b Rung 1) — one compiled face, one projection
;; ============================================================================

(defn build-face-tree
  "PURE per-view-instance face build (P3b Rung 1, JVM-testable). Run ONE compiled
   assembly over ONE data projection into a RESOLVED, address-stamped,
   container-LOCAL rt-tree — the tree a store slot holds. This is the general
   form P3a lacked: P3a reused the ALREADY-built singleton !face-scene for every
   spawned instance (so all instances wore the MAIN face); this builds each
   instance through ITS OWN compiled assembly, so N different arsenal faces can
   render one live conversation at once (Sid's MINDBLOW).

   `compiled` is a face_assembly/compile-assembly result (holds builder closures —
   it lives in the runtime's !vi-faces registry, NEVER in a store slot; slots stay
   serializable, G2). `projection` is the §7 data-context. `view-ctx` is the
   apply-assembly view context {:view-instance :address :geom}. `unit-ids` are the
   block unit-ids in the projection — stamp-block-addresses lifts them to
   [:data :address] so the store's fan-out index (CONTRACT §5) keys blocks.

   apply-assembly is pure + TOTAL (never throws — a bad assembly renders an error
   card); so is this. Same (compiled, projection, view-ctx) → an EQUAL tree, so
   an unchanged echo leaves a slot's :ops identical? after upsert (trap T5)."
  [compiled projection view-ctx unit-ids]
  (-> (fa/apply-assembly compiled projection view-ctx)
      (stamp-block-addresses unit-ids)))

;; ============================================================================
;; Context bundle — point-and-say's data half (scene-substrate P4, CONTRACT §5)
;; ============================================================================
;; The deictic seam: pointing at the scene produces a plain-EDN bundle an agent
;; reads instead of hunter-gathering context. PURE over the store value + the
;; effective transforms (JVM-tested). Zero fn values (store-fns-free? applies).

(def visible-cap
  "Cap on :visible addresses in a bundle — the top-N by on-screen area. The
   TOTAL distinct visible count rides alongside (:visible-count) so a truncated
   set is never silent (CONTRACT §5)."
  32)

(def ^:private identity-camera {:x 0.0 :y 0.0 :zoom 1.0})

(defn- addressed-rects
  "Walk a RESOLVED tree; for every node carrying [:data :address], return
   {:address a :x ax :y ay :w w :h h} in ABSOLUTE container-LOCAL coords (bounds
   offset down the parent chain — the same accumulation tree->rects/hit-test do).
   A block's addressed descendants each appear; the caller dedupes per address by
   MAX area (the shallowest node is the largest, so the block's own rect wins)."
  [tree]
  (letfn [(walk [node px py acc]
            (let [b   (:bounds node)
                  ax  (+ px (:x b 0))
                  ay  (+ py (:y b 0))
                  w   (:w b 0)
                  h   (:h b 0)
                  addr (get-in node [:data :address])
                  acc (if (some? addr)
                        (conj acc {:address addr :x ax :y ay :w w :h h})
                        acc)]
              (reduce (fn [a c] (walk c ax ay a)) acc (:children node))))]
    (walk tree 0 0 [])))

(defn- rect-screen-area
  "Clipped on-screen area of a container-LOCAL rect. local →(container eff)→
   world →(camera)→ screen, then clip to the viewport [0,0,vw,vh]. Camera maps
   exactly like the shader: screen = world·zoom + pan. A fully off-screen rect → 0.0.
   Uniform camera zoom is a global factor, so ranking by this area matches
   ranking by any consistent zoom (CONTRACT §5)."
  [bounds eff camera vw vh]
  (let [{sx :x sy :y sw :w sh :h}
        (containers/screen-bounds eff bounds camera)
        ;; clip to viewport
        x0 (max 0.0 sx)
        y0 (max 0.0 sy)
        x1 (min (double vw) (+ sx sw))
        y1 (min (double vh) (+ sy sh))
        cw (- x1 x0)
        ch (- y1 y0)]
    (if (and (> cw 0.0) (> ch 0.0)) (* cw ch) 0.0)))

(defn visible-addresses
  "Addresses whose nodes are on screen NOW, selected top-`cap` by descending
   clipped screen area, plus the total distinct visible count (CONTRACT §5 — the
   cap carries its count so truncation is never silent). PURE. Every returned
   address is drawn by a live slot, so it resolves through the fan-out :index.
   Screen-camera containers (effective :flags bit0) ignore the world camera (they
   are pinned). Returns {:visible #{addr…} :ranked [addr…] :count n} — :ranked is
   the full descending order (the cap slices its head); :visible is the pinned
   §5 set of the head. Ties break by pr-str address for determinism."
  [store effective-transforms camera vw vh cap]
  (let [areas (reduce
               (fn [m slot]
                 (let [eff (get effective-transforms (:container slot))]
                   (if (nil? eff)
                     m
                     (let [cam (if (= 1 (:flags eff)) identity-camera camera)]
                       (reduce
                        (fn [m r]
                          (let [a (rect-screen-area r eff cam vw vh)]
                            (if (pos? a)
                              (update m (:address r) (fnil max 0.0) a)
                              m)))
                        m
                        (addressed-rects (:tree slot)))))))
               {}
               (vals (:slots store)))
        ranked (->> areas
                    (sort-by (fn [[addr a]] [(- a) (pr-str addr)]))
                    (mapv key))]
    {:visible (into #{} (take cap ranked))
     :ranked  ranked
     :count   (count ranked)}))

(defn context-bundle
  "Point-and-say's data half (scene-substrate P4, CONTRACT §5). PURE, plain EDN,
   fn-free — round-trips pr-str/read-string. Pointing at `world-point` picks the
   deepest addressed node (nil world-point / a miss → :address nil, but :visible
   is STILL populated: pointing at empty canvas still describes the scene). The
   camera describes how the scene maps to the screen: {:world <world camera>
   :container <the picked container's effective transform, or nil on a miss>}.
   `viewport` = {:width :height :camera {:x :y :scale}} (camera defaults to
   identity). Every :visible address AND the picked :address resolves back through
   the store's fan-out :index (G9)."
  [store effective-transforms world-point viewport]
  (let [camera (or (:camera viewport) identity-camera)
        vw     (:width viewport 0)
        vh     (:height viewport 0)
        picked (when world-point (pick store effective-transforms world-point))
        pcid   (when picked (:container (slot store (:vi picked))))
        peff   (when pcid (get effective-transforms pcid))
        {:keys [visible count]} (visible-addresses store effective-transforms
                                                    camera vw vh visible-cap)]
    {:vi            (:vi picked)
     :address       (:address picked)
     :src-path      (:src-path picked)
     :camera        {:world camera :container peff}
     :visible       visible
     :visible-count count
     ;; editable-material P4: the receipt half of the deictic seam. These are
     ;; mechanical co-presence facts only; no relation/aboutness is inferred.
     ;; Worn material is referenced by master+revision ids found in the actual
     ;; picked render path — never copied as material bytes.
     :receipt/picked-at
     {:address (:address picked)
      :src-path (:src-path picked)
      :view-instance (:vi picked)
      :point-world world-point
      :point-local (:point-local picked)}
     :receipt/placement
     {:point-world world-point
      :point-local (:point-local picked)
      :camera {:world camera :container peff}}
     :receipt/worn-materials
     (material-inspector/contribution-stamps (:path picked))}))

;; ============================================================================
;; Actions router (scene-substrate P4, CONTRACT §5, trap T1) — dispatch on data
;; ============================================================================
;; Descriptors are DATA carried in node :data ({:action <kw> …}); a registry
;; {action-kw → handler} dispatches them. The registry (fns) lives in a RUNTIME
;; atom, NEVER in a store value — so scenes stay serializable (G2) while the ONE
;; legitimate fn-over-descriptor invocation happens here. These pure fns take the
;; registry as a value so they are JVM-testable (the live atom is in the runtime).

(defn dispatch-descriptor
  "Invoke `registry`'s handler for descriptor {:action <kw> …} with `ctx`.
   Returns the handler's result, or ::unregistered when no handler is bound.
   Pure w.r.t. the registry value (handlers may effect at the consumer edge)."
  [registry descriptor ctx]
  (if-let [h (get registry (:action descriptor))]
    (h descriptor ctx)
    ::unregistered))

(defn replay-descriptors
  "Replay `descriptors` in order through `registry` with `ctx` (G10 helper —
   captures identical call order + args when the handler records). Returns the
   vector of per-descriptor results."
  [registry descriptors ctx]
  (mapv #(dispatch-descriptor registry % ctx) descriptors))

(defn tree-descriptors
  "Depth-first pre-order (render/replay order) collection of the descriptor at
   [:data k] from every node that carries one. PURE; used to lift a scene tree's
   action descriptors for the router (G10)."
  [tree k]
  (letfn [(walk [node acc]
            (let [d   (get-in node [:data k])
                  acc (if (some? d) (conj acc d) acc)]
              (reduce (fn [a c] (walk c a)) acc (:children node))))]
    (walk tree [])))

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
