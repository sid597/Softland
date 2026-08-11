(ns app.client.workspace.scene-store
  "The one client scene store (scene-substrate P1): view-instances → resolved
   rect-trees, keyed for address fan-out, picked through per-container transforms.

   PURE .cljc: pure functions over an EDN store value, JVM- and CLJS-runnable.
   No atoms, no interop, no renderer imports. The one runtime atom that holds the
   live store, and the GPU upload, live at the reduce/consumer edge elsewhere
   (CONTRACT §5, traps T3/T4) — NOT here.

   Store value: {:slots {vi → slot} :index {address → #{vi}}
                 :ordered {[order-token entry-id] → entry}}. A slot is one
   view-instance's resolved tree + flattened ops + its address→paths subtree
   index (CONTRACT §5):
     {:vi <edn> :container <int> :tree <resolved rt-tree, container-LOCAL>
      :ops {:text [...] :rects [...] :shadows [...] :images [...] :paths [...]
            :connectors [...] :regions [...]}
      :addresses {address → #{index-path}} :meta {...} :stratum :world|:overlay}
   The :index fan-out is maintained incrementally by upsert/remove, NEVER
   recomputed by scanning slots at read time. Resolution route:
   address → :index → vis → slot :addresses → index-paths → nodes."
  (:require [app.client.substrate.region3d-placement :as region3d-placement]
            [app.client.substrate.scene-tape :as scene-tape]
            [app.client.workspace.rect-tree :as rt]
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
   :shadows (rt/tree->shadows tree)
   :images  (rt/tree->images tree)
   :regions (rt/tree->regions tree)
   :paths   (rt/tree->paths tree)
   :connectors (rt/tree->connectors tree)
   :chromes (rt/tree->chromes tree)})

(defn- stamp-ops-container
  "Bake the slot's compact transform-table index onto every flattened op so the GPU places each
   glyph/rect/shadow through its container transform (P2 plumbing: pack-rect,
   pack-shadow, shape-* all read :container-idx). Done HERE at upsert (P3b Rung 2),
   not per-frame, so a slot's ops stay identity-stable across a SIBLING slot's
   change — the per-slot text geo's identical?-skip (G8) depends on it (trap T5).
   :container-slot is stable for a slot's whole life (move/affine changes the
   registry transform, not the slot). Text ops are nested [[op..]..] (lines);
   rects, shadows, and images are flat."
  [ops container-slot container vi]
  (let [slot (or container-slot 0)]
    {:text    (mapv (fn [line] (mapv #(assoc % :container-idx slot
                                              :container container) line)) (:text ops))
     :rects   (mapv #(assoc % :container-idx slot :container container) (:rects ops))
     :shadows (mapv #(assoc % :container-idx slot) (:shadows ops))
     ;; IMAGE-ATOM T8/T14: stamping reuses W2-A's compact slot; image ops
     ;; acquire no per-node transform representation.
     :images  (mapv #(assoc % :container-idx slot) (:images ops))
     :regions (mapv #(assoc % :container-idx slot
                            :container container
                            :owner-vi vi)
                    (:regions ops))
     :paths   (mapv #(assoc % :container-idx slot) (:paths ops))
     ;; Connector anchor space is always the SLOT's own container. Both the
     ;; semantic cid (CPU route resolution) and compact transport slot (GPU)
     ;; are stamped here; no per-edge foreign transform road exists.
     :connectors (mapv #(assoc % :container-idx slot
                               :container container
                               :owner-vi vi)
                       (:connectors ops))
     :chromes (mapv #(assoc % :container-idx slot
                            :container container
                            :owner-vi vi)
                    (:chromes ops))}))

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
  [vi {:keys [tree container container-slot stack-path meta stratum pre-resolved?]}]
  (let [resolved (if pre-resolved? tree (rt/resolve-layout tree))]
    {:vi        vi
     :container container
     :container-slot (or container-slot container 0)
     ;; W2-B order projection of W2-A's canonical nested path.  It is stamped
     ;; beside the compact transport slot, never encoded into that transport.
     :stack-path (or stack-path [])
     :tree      resolved
     :ops       (stamp-ops-container (flatten-ops resolved)
                                     (or container-slot container 0)
                                     (or container 0)
                                     vi)
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

(declare ^:private slot-entry)

(defn empty-store
  "The empty store value."
  []
  {:slots {}
   :index {}
   :ordered (sorted-map-by scene-tape/entry-key-compare)})

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
        ;; Existing callers that rebuild only tree content must not lose the
        ;; compact W2-A slot or the W2-B order path.  Both are stable for the
        ;; live container lifetime and are inherited when omitted.
        opts  (cond-> opts
                (and old (not (contains? opts :container-slot)))
                (assoc :container-slot (:container-slot old))

                (and old (not (contains? opts :stack-path)))
                (assoc :stack-path (:stack-path old)))
        slot' (build-slot vi opts)
        index (-> (:index store)
                  (cond-> old (index-disj vi (:addresses old)))
                  (index-conj vi (:addresses slot')))
        ;; SEAM-STEP1 T2/T10: order truth is the stamped slot entry, and the
        ;; entry retains the exact slot (including identical ops arrays).
        ordered0 (cond-> (:ordered store)
                   old (scene-tape/ordered-remove (slot-entry old {})))
        ordered (scene-tape/ordered-insert
                 scene-tape/default-family-registry
                 ordered0
                 (slot-entry slot' {}))]
    (-> store
        (assoc-in [:slots vi] slot')
        (assoc :index index :ordered ordered))))

(defn remove-slot
  "Drop vi's slot and prune it from the fan-out :index (emptied address entries
   disappear). No-op when vi is absent."
  [store vi]
  (if-let [old (get-in store [:slots vi])]
    (-> store
        (update :slots dissoc vi)
        (update :index index-disj vi (:addresses old))
        (update :ordered scene-tape/ordered-remove (slot-entry old {})))
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
;; Ordered scene tape + pick — Contract O / address-context seam
;; ============================================================================

(defn- deepest-addressed
  "Given a hit-test result (nodes root→leaf), return the deepest node carrying
   [:data :address], or nil. Deepest-first so an addressed ancestor is returned
   when the hit leaf itself has no address."
  [path]
  (some (fn [n] (when (get-in n [:data :address]) n)) (rseq path)))

(defn- slot-entry [slot effective-transforms]
  (let [vi (:vi slot)
        cid (:container slot)
        effective (get effective-transforms cid)
        stack-path (or (:stack-path effective)
                       (:stack-path slot)
                       ;; Hand-built legacy fixtures without a registry retain
                       ;; deterministic root placement; runtime registrations
                       ;; always carry W2-A's full path.
                       [[cid 0 0]])]
    {:entry/id [:scene-slot vi]
     :material/id (or (get-in slot [:meta :material/id]) vi)
     :material/revision (or (get-in slot [:meta :material/revision]) 0)
     :instance/id vi
     ;; Current product picking is rect-tree bounds (the W0-A divergence
     ;; sentinel says so explicitly); text roads remain paint derivations.
     :family/id :render.family/rect
     :order {:stratum (or (:stratum slot) :world)
             :pass-class :direct
             :stack-path stack-path
             :part-rank 0
             :stable-tie vi}
     :paint {:batch/id [:scene-slot vi]}
     :pick {:geometry :rect-tree-bounds :owner vi}
     :visibility {:visible? true :clip :shared-tree-clip}
     :runtime/slot slot}))

(defn scene-tape
  "Compile the store's ONE immutable ordered tape.  The one-argument form is
   the paint projection over paths stamped at registration.  The two-argument
   form refreshes those same paths from the current W2-A effective map for
   explicit batch-oracle callers; live pick walks the stamped maintained view.
   Neither form sorts by family, map iteration, or registration."
  ([store]
   (scene-tape store {}))
  ([store effective-transforms]
   (let [entries (mapv #(slot-entry % effective-transforms)
                       (vals (:slots store)))
         revision (mapv (fn [entry]
                          [(:entry/id entry)
                           (:material/revision entry)
                           (get-in entry [:order :stack-path])])
                        entries)]
     (scene-tape/compile-tape [:scene-store revision] entries))))

(defn rebuild-ordered
  "Rebuild the maintained sorted view from slots after rehydration. This is the
   one sanctioned reconstruction door for representations (such as EDN) that
   preserve map values but not a sorted map's comparator."
  [store]
  ;; SEAM-STEP1 T3: never trust an EDN-round-tripped :ordered map's iteration
  ;; order; restore the comparator through this explicit door.
  (assoc store :ordered
         (reduce (partial scene-tape/ordered-insert
                          scene-tape/default-family-registry)
                 (sorted-map-by scene-tape/entry-key-compare)
                 (map #(slot-entry % {}) (vals (:slots store))))))

(defn maintained-entries
  "Walk the write-maintained store order without invoking the batch compiler."
  [store]
  (into [] (map val) (:ordered store)))

(declare targets-by-address)

(defn derive-store-frame
  "Pure GPU payload projection from the write-maintained scene order.

   `scene_runtime.cljs` owns only the Missionary wrapper around this function,
   so the payload/count/order leg is executable on the JVM.  `:order-by-vi`
   remains lane-agnostic; only `:ops-count-by-vi` gains the image lane."
  [store]
  (let [entries (maintained-entries store)
        ordered (mapv :runtime/slot entries)
        resolved-regions (region3d-placement/resolve-placed-refs ordered)]
    {:rects (into [] (mapcat (comp :rects :ops)) ordered)
     :shadows (into [] (mapcat (comp :shadows :ops)) ordered)
     :images (into [] (mapcat (comp :images :ops)) ordered)
     :regions resolved-regions
     :paths (into [] (mapcat (comp :paths :ops)) ordered)
     :connectors (into [] (mapcat (comp :connectors :ops)) ordered)
     :chromes (into [] (mapcat (comp :chromes :ops)) ordered)
     :targets-by-address (targets-by-address ordered)
     :text-by-vi (reduce (fn [result slot]
                           (assoc result (:vi slot) (get-in slot [:ops :text])))
                         {}
                         ordered)
     :rect-clips-by-vi
     (into {}
           (map (fn [slot]
                  [(:vi slot)
                   (mapv (fn [op] {:clip (:gpu/clip op)
                                   :container (:container op)})
                         (get-in slot [:ops :rects]))]))
           ordered)
     :text-clips-by-vi
     (into {}
           (map (fn [slot]
                  [(:vi slot)
                   (mapv (fn [line]
                           (mapv (fn [op] {:clip (:gpu/clip op)
                                          :container (:container op)}) line))
                         (get-in slot [:ops :text]))]))
           ordered)
     :ordered-vis (mapv :vi ordered)
     :ops-count-by-vi
     (into {}
           (map (fn [slot]
                  [(:vi slot)
                   {:rects (count (get-in slot [:ops :rects]))
                    :shadows (count (get-in slot [:ops :shadows]))
                    :text-lines (count (get-in slot [:ops :text]))
                    :images (count (get-in slot [:ops :images]))
                    :regions (count (get-in slot [:ops :regions]))
                    :paths (count (get-in slot [:ops :paths]))
                    :connectors (count (get-in slot [:ops :connectors]))
                    :chromes (count (get-in slot [:ops :chromes]))}]))
           ordered)
     :order-by-vi
     (into {}
           (map (fn [entry]
                  [(get-in entry [:runtime/slot :vi]) (:order entry)]))
           entries)}))

(defn- normalize-pick-points [point]
  ;; SEAM-STEP1 T9: the bare-vector form is the backward-compatible contract;
  ;; the map form makes world and screen coordinates explicit for cursor paths.
  (if (map? point)
    {:world (:world point) :screen (:screen point)}
    {:world point :screen point}))

(defn ordered-slots
  "The paint projection's slots in exact forward tape order."
  ([store]
   (ordered-slots store {}))
  ([store effective-transforms]
   (mapv :runtime/slot
         (:entries (scene-tape store effective-transforms)))))

(defn- node-local-point
  "Convert a slot-local point to the deepest node's local coordinates."
  [path [slot-x slot-y]]
  (let [[node-x node-y]
        (reduce (fn [[x y] node]
                  [(+ x (get-in node [:bounds :x] 0))
                   (+ y (get-in node [:bounds :y] 0))])
                [0.0 0.0]
                path)]
    [(- slot-x node-x) (- slot-y node-y)]))

(defn pick
  "Resolve a world/screen point through the exact reverse of the paint tape.
   Every entry consumes W2-A's effective affine and nested stack path.  The
   point is inverse-transformed into container-local space before rect-tree
   classification; there is no layer/family/map-iteration re-sort."
  [store effective-transforms point]
  (let [{:keys [world screen]} (normalize-pick-points point)]
    (some->
     (scene-tape/pick-reverse
      {:entries (maintained-entries store)}
      (fn [entry]
        (let [s (:runtime/slot entry)
              eff (get effective-transforms (:container s))
              point (if (= 1 (:flags eff)) screen world)
              [lx ly] (containers/inverse-point eff point)]
          (when-let [path (rt/hit-test (:tree s) lx ly)]
            (when-let [node (deepest-addressed path)]
              (let [base {:vi          (:vi s)
                          :path        path
                          :address     (get-in node [:data :address])
                          :src-path    (get-in node [:data :assembly/src-path])
                          :actions     (get-in node [:data :actions])
                          :point-local [lx ly]}]
                (if-let [region (get-in node [:data :region3d/scene])]
                  (assoc base
                         :route :region3d
                         :region-id (get-in node [:data :region3d/id])
                         :css-point screen
                         :region-local (node-local-point path [lx ly])
                         :region-size [(get-in node [:bounds :w])
                                       (get-in node [:bounds :h])]
                         :region-scale (Math/sqrt
                                        (Math/abs
                                         (containers/determinant (:affine eff))))
                         :region-material region
                         :region-revision (:region3d/version region))
                  base)))))))
     :hit)))

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

(defn- node-family [node]
  (or (get-in node [:data :render/family])
      (when (get-in node [:data :region3d/scene])
        :render.family/region-3d)
      (when (get-in node [:data :connector/material])
        :render.family/connector)
      (when (get-in node [:data :path/material]) :render.family/path)
      (when (get-in node [:data :image/digest]) :render.family/image)
      :render.family/rect))

(defn addressed-rects
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
                        (conj acc {:address addr :x ax :y ay :w w :h h
                                   :family (node-family node)})
                        acc)]
              (reduce (fn [a c] (walk c ax ay a)) acc (:children node))))]
    (walk tree 0 0 [])))

(defn targets-by-address
  "Batch oracle for connector attachment: address -> every occurrence
   with its slot identity, absolute container-local bounds, semantic container,
   and compact GPU slot. The future incremental sibling must fence against this
   exact walk."
  [ordered-slots]
  (reduce
   (fn [index slot]
     (let [largest-by-address
           (reduce
            (fn [by-address {:keys [address w h] :as rect}]
              (let [prior (get by-address address)]
                (if (or (nil? prior)
                        (> (* w h) (* (:w prior) (:h prior))))
                  (assoc by-address address rect)
                  by-address)))
            {}
            (addressed-rects (:tree slot)))]
       (reduce-kv
        (fn [index address {:keys [x y w h family]}]
          (update index address (fnil conj [])
                  {:vi (:vi slot)
                   :bounds {:x x :y y :w w :h h}
                   :family family
                   :container (:container slot)
                   :container-idx (:container-slot slot)}))
        index
        largest-by-address)))
   {}
   ordered-slots))

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
