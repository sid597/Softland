(ns app.client.workspace.containers
  "Container registry + transform-tree composition (scene-substrate P1).

   PURE .cljc: pure functions over an EDN registry value, JVM- and
   CLJS-runnable. No atoms, no interop, no renderer imports. The one runtime
   atom that holds a live registry, and the GPU buffer writer, live where
   consumed (runtime/ + renderer) — NOT here (CONTRACT §4).

   A container is a node in a transform tree: an offset + scale relative to its
   parent, plus a camera choice (:world pans/zooms with the scene, :screen is
   pinned). `effective` composes the tree into absolute transforms — nested
   containers are groups for free. cid 0 is reserved: identity, :world, layer 0;
   every producer that has not opted into a container rides it (trap T6 — the
   safe stride-migration default). Forward transform (in-shader):
   world = eff-offset + local * eff-scale; `inverse-point` is its inverse.")

;; ============================================================================
;; Registry construction
;; ============================================================================

(def ^:private root-container
  "cid 0: the reserved identity/world container (trap T6)."
  {:parent nil :x 0.0 :y 0.0 :scale 1.0 :camera :world :layer 0})

(defn empty-registry
  "A registry holding only the reserved cid 0 (identity, :world, layer 0)."
  []
  {:containers {0 root-container}})

(defn add-container
  "Register a NEW container. Defaults: parent nil (= root/world), x 0.0, y 0.0,
   scale 1.0, camera nil (inherit), layer 0. Throws when cid is 0 (reserved),
   cid already exists, or :parent names a container that does not exist."
  [reg cid {:keys [parent x y scale camera layer]
            :or   {parent nil x 0.0 y 0.0 scale 1.0 camera nil layer 0}}]
  (when (= cid 0)
    (throw (ex-info "cid 0 is reserved (identity/world) and cannot be added"
                    {:cid cid})))
  (when (contains? (:containers reg) cid)
    (throw (ex-info "container already exists" {:cid cid})))
  (when (and (some? parent) (not (contains? (:containers reg) parent)))
    (throw (ex-info "parent container does not exist" {:cid cid :parent parent})))
  (assoc-in reg [:containers cid]
            {:parent parent :x x :y y :scale scale :camera camera :layer layer}))

(defn set-transform
  "Partial in-place update of a container's :x/:y/:scale (only the supplied keys
   change). Throws on cid 0 (reserved) or an unknown cid. Does NOT touch :parent
   — there is no re-parenting API, so cycles cannot form through the public
   surface (effective still guards, for hand-built registries)."
  [reg cid t]
  (when (= cid 0)
    (throw (ex-info "cid 0 is reserved and cannot be mutated" {:cid cid})))
  (when-not (contains? (:containers reg) cid)
    (throw (ex-info "unknown container" {:cid cid})))
  (update-in reg [:containers cid] merge (select-keys t [:x :y :scale])))

(defn remove-container
  "Drop a container from the registry (P3b Rung 1 despawn — the lifecycle half
   P3a lacked). Throws on cid 0 (reserved) or an unknown cid; refuses to remove a
   container that any other container still names as :parent (that would orphan
   the child and make `effective` throw). Face containers are flat (no parent, no
   children), so a despawn is always a clean single dissoc."
  [reg cid]
  (when (= cid 0)
    (throw (ex-info "cid 0 is reserved and cannot be removed" {:cid cid})))
  (when-not (contains? (:containers reg) cid)
    (throw (ex-info "unknown container" {:cid cid})))
  (when-let [child (some (fn [[c m]] (when (= cid (:parent m)) c))
                         (:containers reg))]
    (throw (ex-info "cannot remove a container with children"
                    {:cid cid :child child})))
  (update reg :containers dissoc cid))

;; ============================================================================
;; Transform-tree composition → effective (absolute) transforms
;; ============================================================================

(def ^:private world-base
  "The virtual transform above a root container (:parent nil): identity, world."
  {:x 0.0 :y 0.0 :scale 1.0 :camera :world})

(defn- compose-one
  "Return [cache eff] for cid — eff = {:x :y :scale :camera :layer}, composed
   along the parent chain and memoized in `cache` (each chain computed once,
   CONTRACT §5). `seen` is the set of cids on the current chain; revisiting one
   is a parent cycle."
  [containers cache seen cid]
  (cond
    (contains? cache cid) [cache (get cache cid)]
    (contains? seen cid)  (throw (ex-info "container parent cycle" {:cid cid}))
    :else
    (let [c         (get containers cid)
          parent    (:parent c)
          [cache p] (if (nil? parent)
                      [cache world-base]
                      (compose-one containers cache (conj seen cid) parent))
          ps        (:scale p)
          ;; eff-scale  = parent-eff-scale  * own-scale
          ;; eff-offset = parent-eff-offset + own-offset * parent-eff-scale
          eff       {:x      (+ (:x p) (* (:x c) ps))
                     :y      (+ (:y p) (* (:y c) ps))
                     :scale  (* ps (:scale c))
                     ;; nearest explicitly-set ancestor camera; own wins,
                     ;; default :world (carried by world-base)
                     :camera (or (:camera c) (:camera p))
                     :layer  (:layer c)}]
      [(assoc cache cid eff) eff])))

(defn effective
  "Compose the whole registry into absolute transforms:
   {cid → {:x ex :y ey :scale es :flags 0|1 :layer l}}. flags bit0 = 1 when the
   resolved camera is :screen (pan/zoom go identity in-shader). A parent cycle
   throws."
  [reg]
  (let [containers (:containers reg)
        cache      (reduce (fn [cache cid]
                             (first (compose-one containers cache #{} cid)))
                           {}
                           (keys containers))]
    (reduce-kv (fn [m cid eff]
                 (assoc m cid {:x     (:x eff)
                               :y     (:y eff)
                               :scale (:scale eff)
                               :flags (if (= :screen (:camera eff)) 1 0)
                               :layer (:layer eff)}))
               {}
               cache)))

(defn inverse-point
  "Map a world/screen point into a container's LOCAL space, given that
   container's effective entry: local = (point - eff-offset) / eff-scale. The
   inverse of the in-shader forward transform world = offset + local*scale.
   Pick calls this BEFORE hit-test (trap T8)."
  [{:keys [x y scale]} [px py]]
  [(/ (- px x) scale) (/ (- py y) scale)])
