(ns app.client.workspace.scene-runtime
  "The LIVE scene store + container registry, effective transforms, GPU-op
   derivations, and Missionary flows over them. Pure logic lives in
   scene_store.cljc / containers.cljc (JVM tested); THIS ns owns the ONE store
   atom + ONE registry atom.

   Discipline:
   - Store/registry MUTATIONS happen at edges only, NEVER inside an m/latest
     (T4).
   - The GPU upload (write-containers!) and the op merge run at the render
     m/reduce edge (render.cljs), NOT here (T4).
   - <store-frame is ONE m/latest over the store — text/rects/shadows
     co-derived together, no diamond off the store (T3).
   - In-flight container transforms are client-side 60Hz, NO events (T10)."
  (:require [missionary.core :as m]
            [app.client.substrate.frame-delta :as frame-delta]
            [app.client.substrate.frame-effects :as frame-effects]
            [app.client.workspace.scene-store :as ss]
            [app.client.workspace.containers :as ctn]))

;; ---------------------------------------------------------------------------
;; State — the ONE store + the ONE registry (CONTRACT §4/§5)
;; ---------------------------------------------------------------------------

(defonce !scene-store (atom (ss/empty-store)))
(defonce !containers-registry (atom (ctn/empty-registry)))
(defonce ^:private !frame-container-delta-journal
  (atom {:high-water 0 :deltas []}))

(defn- mint-container-delta! [delta]
  (when delta
    (swap! !frame-container-delta-journal
           (fn [{:keys [high-water deltas]}]
             (let [sequence (inc high-water)]
               {:high-water sequence
                :deltas (conj deltas (assoc delta :delta/sequence sequence))}))))
  delta)

(defn frame-container-delta-snapshot []
  @!frame-container-delta-journal)

(defn ack-frame-container-deltas! [high-water]
  (swap! !frame-container-delta-journal
         update :deltas
         (fn [deltas]
           (into [] (remove #(<= (:delta/sequence %) high-water)) deltas)))
  nil)

(defn- mutate-container-registry! [cid mutation]
  (let [minted (volatile! nil)]
    (swap! !containers-registry
           (fn [before]
             (let [after (mutation before)]
               (vreset! minted
                        (frame-delta/container-delta
                         cid
                         (frame-delta/container-declaration before cid)
                         (frame-delta/container-declaration after cid)))
               after)))
    (mint-container-delta! @minted)
    @!containers-registry))

;; !last-pick (scene-substrate P4) — the deictic seam's memory: the last scene
;; pick's world-point + resolved node, recorded at the click consumer edge
;; (the future host edge). The context bundle re-picks from :world-point when a
;; command is NOT itself a click, so "the thing I just pointed at" remains
;; available to the caller.
(defonce !last-pick (atom nil))

;; !action-registry (scene-substrate P4 / G10) — {action-kw → handler}. The
;; descriptor router's live half: handlers are fns and MUST NOT enter a store
;; value (G2), so they live HERE in a runtime atom. ss/dispatch-descriptor does
;; the pure dispatch over this value.
(defonce !action-registry (atom {}))

;; Region3D completes the existing pick road only after scene-store has
;; returned a session-free region route. The installed resolver owns the
;; session camera; nil preserves the pre-Region3D result exactly.
(defonce ^:private !region-pick-resolver (atom nil))

(defn install-region-pick-resolver! [resolver]
  (reset! !region-pick-resolver resolver)
  resolver)

(defn set-transform!
  "In-flight container transform (T10: client-side 60Hz, NO events). Mutates
   the registry only; the render consumer edge re-uploads the changed effective
   transforms (write-containers!) and redraws — instance buffers untouched, so
   drag/zoom is smooth (transform, not re-shape)."
  [cid t]
  (swap! !containers-registry ctn/set-transform cid t))

(defn set-effects!
  "Replace one container's flag-lane session effects. Validation happens before
   the registry mutation; scene-store derivation remains untouched."
  [cid effects]
  (let [normalized (when (seq effects)
                     (frame-effects/validate-effects! effects))]
    (mutate-container-registry! cid #(ctn/set-effects % cid normalized))
    normalized))

;; ---------------------------------------------------------------------------
;; Reads / pick
;; ---------------------------------------------------------------------------

(defn store-snapshot [] @!scene-store)

(defn any-slots? [] (boolean (seq (:slots @!scene-store))))

(defn effective-transforms
  "Client-side composed transforms {cid → eff} — used BOTH for pick and for the
   GPU upload. Pure over the registry atom (CONTRACT §4/§5)."
  []
  (ctn/effective @!containers-registry))

(defn container-registry-snapshot [] @!containers-registry)

(defn pick-world
  "Pick through the store from either a backward-compatible bare world point or
   {:world [wx wy] :screen [sx sy]}. Pick inverse-transforms the coordinate
   selected by each container's effective camera flag and returns
   {:vi :address …} for the deepest addressed node, or nil on a miss."
  [point]
  (let [hit (ss/pick @!scene-store (effective-transforms) point)]
    (if (and (= :region3d (:route hit)) @!region-pick-resolver)
      (@!region-pick-resolver hit)
      hit)))

;; ---------------------------------------------------------------------------
;; Deictic seam — last pick + context bundle (scene-substrate P4, CONTRACT §5)
;; ---------------------------------------------------------------------------

(defn record-pick!
  "Consumer-edge record of a scene pick (mouse.cljs): the WORLD point pointed at
   + the resolved pick (or nil on a store miss). The context bundle re-picks from
   :world-point at submit time, so an agent turn fired AFTER a click still carries
   what was pointed at. Edge-only mutation (no m/latest, T4)."
  [world-point hit]
  (reset! !last-pick (assoc hit :world-point world-point)))

(defn last-pick [] @!last-pick)

(defn bundle-for-viewport
  "Assemble the deictic context bundle for the CURRENT scene at the last pick
   (scene-substrate P4). `viewport` = the runtime viewport; `scroll-y` is the
   current world-camera pan (screen = world − [0 scroll-y], zoom 1.0 — the world
   camera is not live yet, CONTRACT §5). Pure delegate to ss/context-bundle over
   the live store snapshot + effective transforms."
  [viewport scroll-y]
  (let [vp {:width  (:width viewport)
            :height (:height viewport)
            :camera {:x 0.0 :y (double (or scroll-y 0)) :scale 1.0}}]
    (ss/context-bundle @!scene-store (effective-transforms)
                       (:world-point @!last-pick) vp)))

(defn bundle-at-world-point
  "Build a receipt-capable context bundle at an explicit WORLD point. The open
   ground uses this at birth and Ctrl+Enter so the pointer may indicate a target
   while keyboard focus remains on the utterance block — point→say with no
   selection ritual. `camera` is the live world camera {:x :y :zoom|:scale}."
  [world-point viewport camera]
  (let [scale (double (or (:scale camera) (:zoom camera) 1.0))
        vp {:width (:width viewport)
            :height (:height viewport)
            :camera {:x (double (or (:x camera) 0))
                     :y (double (or (:y camera) 0))
                     :scale scale}}]
    (ss/context-bundle @!scene-store (effective-transforms) world-point vp)))

;; ---------------------------------------------------------------------------
;; Actions router (scene-substrate P4 / G10) — descriptor dispatch
;; ---------------------------------------------------------------------------

(defn register-action!
  "Bind a handler (fn [descriptor ctx] → result) to an action keyword in the live
   registry. Idempotent (assoc); safe to re-run on hot reload."
  [kw handler]
  (swap! !action-registry assoc kw handler))

(defn dispatch-action
  "Dispatch a descriptor {:action <kw> …} through the live registry with `ctx`.
   Pure dispatch lives in ss/dispatch-descriptor; this wraps the runtime atom."
  [descriptor ctx]
  (ss/dispatch-descriptor @!action-registry descriptor ctx))

;; ---------------------------------------------------------------------------
;; GPU-frame derivations (missionary flows; merged at the render consumer edge)
;; ---------------------------------------------------------------------------

(defn <store-frame
  "ONE m/latest over !scene-store → the maintained Contract-O view and its GPU
   contribution.  Rects, shadows, and per-slot text all project from the same
   forward slot order; pick walks those same entries in exact reverse.
   W2-A paths were stamped when the container registered, so an affine-only
   gesture still changes only the transport upload—not these instance arrays."
  []
  (m/latest
    ;; IMAGE-ATOM G8: the pure payload derivation is JVM-owned by scene-store;
    ;; this is only the single reactive sharing point.
    ss/derive-store-frame
    (m/watch !scene-store)))

(defn <effective
  "ONE m/latest over !containers-registry → containers/effective {cid → eff}.
   Feeds the world snapshot; the consumer edge uploads it via write-containers!
   only when it changes (identical? skip)."
  []
  (m/latest ctn/effective (m/watch !containers-registry)))

(defn <frame-registry
  "Plan-layer view of container topology/effects. It is deliberately separate
   from derive-store-frame so session effects never become store derivation."
  []
  (m/latest identity (m/watch !containers-registry)))

(defn install-context-window-api!
  "window.sceneContext — inspect the deictic bundle for the current scene at the
   last pick (scene-substrate P4 dev affordance, UNCOMMITTED). bundle() → the
   EDN as a JS object; edn() → the exact
   pr-str EDN string (round-trips read-string). The SAME bundle is attached to a
   cmd/agent turn at submit time (agent_flow.cljs) and logged [SCENE-CTX]."
  [{:keys [!viewport !scroll-y]}]
  (set! (.-sceneContext js/window)
        #js {:bundle (fn [] (clj->js (bundle-for-viewport @!viewport @!scroll-y)))
             :edn    (fn [] (pr-str (bundle-for-viewport @!viewport @!scroll-y)))})
  (js/console.log "[SCENE-CTX] window.sceneContext installed — sceneContext.bundle() / sceneContext.edn()"))
