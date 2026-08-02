(ns app.client.workspace.scene-runtime
  "scene-substrate P3a — the LIVE scene store + container registry, the
   effective-transforms + GPU-op derivations, and the face-path plurality
   wiring. The pure logic lives in scene_store.cljc / containers.cljc (JVM
   tested); THIS ns owns the ONE store atom + ONE registry atom and the
   missionary flows over them.

   Discipline:
   - Store/registry MUTATIONS happen at edges only — user actions (the
     sceneFaces window API) and the render consumer edge (the echo fan-out).
     NEVER inside an m/latest (T4).
   - The GPU upload (write-containers!) and the op merge run at the render
     m/reduce edge (render.cljs), NOT here (T4).
   - <store-frame is ONE m/latest over the store — text/rects/shadows
     co-derived together, no diamond off the store (T3).
   - In-flight container transforms are client-side 60Hz, NO events (T10);
     only a future SETTLE commits an assembly event (out of P3a scope).

   Additive by construction: with NO registered slots the store contributes
   nothing and every legacy path renders byte-identical (CONTRACT §3). This ns
   holds the EXTRA reader-face view-instances; the MAIN worn face keeps riding
   its singleton legacy path untouched, and the store fans the SAME projection
   into every extra instance on each edit echo."
  (:require [missionary.core :as m]
            [app.client.workspace.scene-store :as ss]
            [app.client.workspace.containers :as ctn]
            [app.client.workspace.events :as ev]))

;; ---------------------------------------------------------------------------
;; State — the ONE store + the ONE registry (CONTRACT §4/§5)
;; ---------------------------------------------------------------------------

(defonce !scene-store (atom (ss/empty-store)))
(defonce !containers-registry (atom (ctn/empty-registry)))

;; !last-pick (scene-substrate P4) — the deictic seam's memory: the last face
;; pick's world-point + resolved node, recorded at the click consumer edge
;; (mouse.cljs). The context bundle re-picks from :world-point when a cmd/agent
;; submit is NOT itself a click, so "the thing I just pointed at" reaches the
;; agent turn. Cleared on face-mode exit through close-all-slots! (the P3b
;; close-all seam — ONE lifecycle, not a second).
(defonce !last-pick (atom nil))

;; !action-registry (scene-substrate P4 / G10) — {action-kw → handler}. The
;; descriptor router's live half: handlers are fns and MUST NOT enter a store
;; value (G2), so they live HERE in a runtime atom. ss/dispatch-descriptor does
;; the pure dispatch over this value.
(defonce !action-registry (atom {}))

;; Face containers start just above the P2 probe's range (probe uses cids 1..16)
;; so the two dev tools never fight over cids in the shared containers buffer.
;; write-containers! writes the contiguous [1..max-cid] range, so a low base
;; keeps that upload small: base 32 → ~15 identity fillers before the first face
;; cid, vs the old base-100's ~85 (wave-2 finding #3 — whole-range write).
(def ^:private face-cid-base 32)
(defonce ^:private !next-cid (atom face-cid-base))

;; Gate-review F3 (2026-07-13): cids must RECYCLE. A monotonic counter walks
;; into the renderer's 1024-container ceiling after ~992 spawn/despawn cycles,
;; and the resulting write-containers! throw lands OUTSIDE the draw try/catch —
;; reactor death from a dev affordance. Freed cids return to a pool; the
;; counter only grows when the pool is empty (JS is single-threaded, so the
;; peek/pop pair cannot race).
(defonce ^:private !free-cids (atom []))

(defn- alloc-cid! []
  (if-let [cid (peek @!free-cids)]
    (do (swap! !free-cids pop) cid)
    (swap! !next-cid inc)))

(defn- free-cid! [cid]
  (when cid (swap! !free-cids conj cid)))

;; !vi-faces (P3b Rung 1) — vi → {:face <name> :compiled <compiled-assembly>
;; :src <conversation address> :geom <geom> :container <cid>}. This is the OTHER
;; half of a slot that must NOT be serializable: the compiled assembly holds
;; builder closures (face_assembly). Keeping it HERE, out of the store, is what
;; lets each instance wear a DIFFERENT face (its own compiled) while the store
;; slots stay closure-free (G2). The echo fan-out rebuilds each slot through the
;; compiled recorded here — never through the singleton !face-scene (P3a's mirror
;; path retires for spawned slots).
(defonce !vi-faces (atom {}))

(defn- vi-of [n] [:vi :reader-face n])
(defn- vi-index [vi] (nth vi 2))

(defn- conv-address
  "The conversation address a projection is OF (the §7 data-context field the
   worn face uses as its Δ1 identity). Drives echo staleness: a projection whose
   address differs from an instance's stored :src is a DIFFERENT conversation, so
   that instance is closed rather than refreshed."
  [projection]
  (:conversation/address projection))

(defn- face-geom
  "Reproduce editor_compute's <face-assembly geom from the runtime atoms so a
   spawned instance builds through the SAME layout the worn face uses (font size,
   char advance, content wrap width, honest server now-ms)."
  [viewport settings active-font projection]
  (let [dpr       (:dpr viewport)
        snap?     (:snap-to-pixel? settings)
        font-size (:font-size settings)]
    {:viewport-w   (:width viewport)
     :viewport-h   (:height viewport)
     :content-w    (- (:width viewport) 32)
     :line-height  (js/Math.round (* font-size 1.4))
     :font-size    font-size
     :char-advance (ev/maybe-snap (* font-size (:char-width active-font)) dpr snap?)
     :now-ms       (or (:face/rendered-at-ms projection) 0)}))

;; ---------------------------------------------------------------------------
;; Pure-ish helpers
;; ---------------------------------------------------------------------------

(defn block-unit-ids
  "The set of block unit-ids in a served face data-context. Pure derivation
   lives in scene-store (first-light P1); this alias keeps runtime callers."
  [ctx]
  (ss/block-unit-ids ctx))

(defn- assert-container-registered!
  "FALSIFY finding #5 (carried to P3): a slot whose :container cid is NOT in
   the registry is both invisible (GPU reads a default/stale container) and
   unpickable (effective has no entry). Assert it at every upsert edge."
  [cid]
  (assert (contains? (:containers @!containers-registry) cid)
          (str "scene-runtime: slot :container " cid
               " is not registered before upsert (FALSIFY finding #5 — "
               "unregistered container = invisible + unpickable)")))

;; ---------------------------------------------------------------------------
;; Store mutations (edges only)
;; ---------------------------------------------------------------------------

(defn register-face-instance!
  "Register a face view-instance as a store slot in its OWN container. Adds the
   container FIRST (finding #5: the cid must exist before the slot names it),
   asserts it registered, THEN upserts the slot. `tree` is a RESOLVED,
   address-stamped, container-LOCAL rt-tree (root at 0,0 — the container
   transform places it; :world camera so it pans/zooms with the scene). Returns
   {:vi :container}."
  [vi tree {:keys [x y scale layer meta pre-resolved?]
            :or   {x 0.0 y 0.0 scale 1.0 layer 1}}]
  (let [cid (alloc-cid!)]
    (swap! !containers-registry ctn/add-container cid
           {:x x :y y :scale scale :camera :world :layer layer})
    (assert-container-registered! cid)
    (let [container-slot (ctn/transport-slot @!containers-registry cid)]
      (swap! !scene-store ss/upsert-slot vi
             {:tree tree :container cid :container-slot container-slot
              :meta (or meta {})
              :pre-resolved? pre-resolved?})
      {:vi vi :container cid :container-slot container-slot})))

(defn close-instance!
  "Despawn (P3b Rung 1 — the lifecycle half P3a lacked): drop the slot, its
   container, and its !vi-faces entry. No-op when the vi is absent, so it is safe
   to call before a re-spawn or over a projection change."
  [vi]
  (when-let [slot (ss/slot @!scene-store vi)]
    (swap! !scene-store ss/remove-slot vi)
    (swap! !containers-registry ctn/remove-container (:container slot))
    (free-cid! (:container slot)))
  (swap! !vi-faces dissoc vi)
  nil)

(defn close-all-slots!
  "Clear EVERY registered instance (face mode exited, or a hard reset). Called at
   the consumer edge when !face-scene goes nil (the worn face turned off) — the
   spawned copies of that conversation have no live projection to echo, so they
   go with it."
  []
  (doseq [vi (keys (:slots @!scene-store))] (close-instance! vi))
  (doseq [vi (keys @!vi-faces)] (close-instance! vi))
  ;; scene-substrate P4 — the deictic memory dies with the scene it pointed at
  ;; (reuse THIS close-all seam; no second lifecycle).
  (reset! !last-pick nil))

(defn- backdrop-wrapped
  "Wrap a built face tree in an opaque backdrop card (G11 round-2 finding:
   overlapping copies interleave text without one). Plain rt-node wrapper —
   no :layout (children keep their own bounds), no :address (a backdrop can
   never hijack a pick — deepest ADDRESSED node wins). Used by BOTH spawn
   and refresh so the card survives echo rebuilds."
  [tree]
  (let [w (or (get-in tree [:bounds :w]) 600.0)
        h (or (get-in tree [:bounds :h]) 400.0)]
    {:bounds {:x 0.0 :y 0.0 :w w :h h}
     :children [{:bounds {:x -12.0 :y -12.0 :w (+ w 24.0) :h (+ h 24.0)}
                 :style {:bg [0.07 0.09 0.13 1.0]
                         :radius 10.0
                         :border-width 1.5
                         :border-color [0.45 0.58 0.85 0.9]}}
                tree]}))

(defn refresh-all-slots!
  "Echo fan-out (G7 / deliverable #6), Rung-1 form: the projection changed (a
   block edit echoed) — rebuild EVERY registered instance through ITS OWN
   compiled face (recorded in !vi-faces) over the SHARED projection. One edit
   patches all appearances, each still wearing its own face — the P3a singleton
   !face-scene mirror is gone. An instance whose stored :src conversation no
   longer matches the projection is STALE (a different conversation is now worn)
   and is closed, not rebuilt (the conversation-change clear half). Survivors
   rebuild in ONE atomic swap!; stale instances are closed first (multi-atom)."
  [projection]
  (let [uids  (block-unit-ids projection)
        src'  (conv-address projection)
        vfs   @!vi-faces
        stale (into #{} (keep (fn [[vi vf]] (when (not= src' (:src vf)) vi))) vfs)]
    (doseq [vi stale] (close-instance! vi))
    (let [survivors (apply dissoc vfs stale)]
      (when (seq survivors)
        (swap! !scene-store
               (fn [store]
                 (reduce
                  (fn [st [vi vf]]
                    (if-let [slot (get-in st [:slots vi])]
                      (let [view-ctx {:view-instance vi :address src' :geom (:geom vf)}
                            tree     (backdrop-wrapped
                                      (ss/build-face-tree (:compiled vf) projection view-ctx uids))]
                        (ss/upsert-slot st vi {:tree      tree
                                               :container (:container slot)
                                               :container-slot (:container-slot slot)
                                               :meta      (:meta slot)
                                               :stratum   (:stratum slot)}))
                      st))
                  store
                  survivors)))))))

;; ---------------------------------------------------------------------------
;; Main-face slot (first-light P1 — the P3c minimum flip)
;; ---------------------------------------------------------------------------
;; The worn face's singleton legacy render path retires: the MAIN face becomes
;; a store slot like any other view-instance, in its OWN container at identity
;; transform (x 0, y 0, scale 1, :world camera) so it renders pixel-identically
;; to the legacy path — the world camera pan (0,−scroll-y) applies in-shader
;; exactly as before. Layer 1: below spawned copies (layer n ≥ 2), above root.
;; No backdrop wrap — the main face IS the ground, not a floating card.
;; Mutations here are called ONLY from the dedicated consumer edge in
;; render.cljs (T4: edges only, and NOT the RAF edge — a same-wave upsert keeps
;; the keystroke→paint path at one frame, no +1-frame store lag).

(def main-face-vi
  "The worn conversation face's view-instance key (the legacy singleton's
   :face-main identity, carried into the store)."
  :face-main)

(defn upsert-main-face!
  "Insert or refresh the main face's slot from a RESOLVED, address-stamped
   tree (ss/build-face-tree output — root Δ1 stamp + per-block [:data :address],
   trap T7). First call registers the container; later calls re-use it."
  [tree]
  (if-let [slot (ss/slot @!scene-store main-face-vi)]
    (swap! !scene-store ss/upsert-slot main-face-vi
           {:tree          tree
            :container     (:container slot)
            :container-slot (:container-slot slot)
            :meta          (:meta slot)
            :stratum       (:stratum slot)
            ;; build-face-tree output is already resolved (apply-assembly
            ;; resolves internally) — skip the second resolve pass (P1 perf)
            :pre-resolved? true})
    (register-face-instance! main-face-vi tree
                             {:x 0.0 :y 0.0 :scale 1.0 :layer 1
                              :meta {:main? true}
                              :pre-resolved? true}))
  nil)

(defn close-main-face!
  "Drop the main face's slot + container. No-op when absent (close-all-slots!
   may have already taken it on a mode exit — both paths stay idempotent)."
  []
  (close-instance! main-face-vi))

(defn set-transform!
  "In-flight container transform (T10: client-side 60Hz, NO events). Mutates
   the registry only; the render consumer edge re-uploads the changed effective
   transforms (write-containers!) and redraws — instance buffers untouched, so
   drag/zoom is smooth (transform, not re-shape)."
  [cid t]
  (swap! !containers-registry ctn/set-transform cid t))

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

(defn pick-world
  "Pick through the store at a WORLD point. The caller converts client→world
   FIRST: world = client − camera-pan; in face mode pan = (0,−scroll-y),
   zoom 1.0, sb-w 0, so world = [x (y + scroll-y)] — exactly the (x, y-scene)
   the click handler already holds. Pick inverse-transforms per container into
   container-local BEFORE hit-test (trap T8) and returns {:vi :address …} for
   the deepest addressed node, or nil on a miss."
  [world-point]
  (ss/pick @!scene-store (effective-transforms) world-point))

;; ---------------------------------------------------------------------------
;; Deictic seam — last pick + context bundle (scene-substrate P4, CONTRACT §5)
;; ---------------------------------------------------------------------------

(defn record-pick!
  "Consumer-edge record of a face pick (mouse.cljs): the WORLD point pointed at
   + the resolved pick (or nil on a store miss). The context bundle re-picks from
   :world-point at submit time, so an agent turn fired AFTER a click still carries
   what was pointed at. Edge-only mutation (no m/latest, T4)."
  [world-point hit]
  (reset! !last-pick (assoc hit :world-point world-point)))

(defn last-pick [] @!last-pick)

(defn bundle-for-viewport
  "Assemble the deictic context bundle for the CURRENT scene at the last pick
   (scene-substrate P4). `viewport` = the runtime viewport; `scroll-y` is the
   face-mode world-camera pan (screen = world − [0 scroll-y], zoom 1.0 — the world
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

;; Registered actions (G10) — the trail-face click case migrated to descriptors.
;; Behavior identical to the pre-P4 case in mouse.cljs/handle-trail-face-click!;
;; the atom to mutate arrives per-call in ctx so the handler stays atom-agnostic.
(register-action! :trail-face/toggle-expand
  (fn [{:keys [id]} {:keys [!trail-face-state]}]
    (swap! !trail-face-state update :expanded
           (fnil (fn [s] (if (contains? s id) (disj s id) (conj s id))) #{}))
    true))

;; ---------------------------------------------------------------------------
;; GPU-frame derivations (missionary flows; merged at the render consumer edge)
;; ---------------------------------------------------------------------------

(defn <store-frame
  "ONE m/latest over !scene-store → the store's GPU contribution. Rects/shadows
   MERGE into flat seqs (they ride the shared editor pools; the :container-idx
   baked at upsert places each op through its container transform). Text is
   exposed PER-SLOT under :text-by-vi (P3b Rung 2 / G8 isolation) so each instance
   drives its OWN text geo — an unchanged slot's text vector stays identical?
   across a sibling's edit (ops stamped+stable at upsert, trap T5), so its geo is
   skipped. ONE flow co-deriving all three op kinds — no diamond off the store
   (T3). Recomputes ONLY when the store changes (identical? on unrelated frames)."
  []
  (m/latest
    (fn [store]
      (let [slots (:slots store)
            ;; first-light P1: deterministic paint order — the main face draws
            ;; FIRST (under every spawned copy), copies then by vi. Pre-flip the
            ;; legacy path appended store rects AFTER the singleton's, so copies
            ;; always painted over the main face; hash order would break that.
            ordered (sort-by (fn [s] [(if (= main-face-vi (:vi s)) 0 1)
                                      (pr-str (:vi s))])
                             (vals slots))]
        {:rects      (into [] (mapcat (comp :rects :ops)) ordered)
         :shadows    (into [] (mapcat (comp :shadows :ops)) ordered)
         :text-by-vi (reduce-kv (fn [m vi slot] (assoc m vi (get-in slot [:ops :text])))
                                {} slots)}))
    (m/watch !scene-store)))

(defn <effective
  "ONE m/latest over !containers-registry → containers/effective {cid → eff}.
   Feeds the world snapshot; the consumer edge uploads it via write-containers!
   only when it changes (identical? skip)."
  []
  (m/latest ctn/effective (m/watch !containers-registry)))

;; ---------------------------------------------------------------------------
;; Dev affordance — window.sceneFaces (UNCOMMITTED, delete-to-remove)
;; ---------------------------------------------------------------------------

(defn- spawn-instance!
  "Spawn the nth reader-face instance of the CURRENTLY worn conversation, built
   through the CURRENTLY worn compiled face (P3b Rung 1). The face used is the
   worn one captured at spawn time — so the MINDBLOW (one conversation through N
   different faces) is: wear face A → spawn → wear face B → spawn → two instances
   of the same conversation, each wearing its own face, echoing together.

   STOP-CLAUSE (per contract §9 half): `face-name`, when supplied and different
   from the worn face, cannot be honored — compiling an ARBITRARY named face
   client-side needs its assembly source, and the ONLY client path to a compiled
   assembly is the single !assembly-request/!assembly-data artery, guarded to the
   currently-worn face (face_wiring/compile-served-source!). So a mismatched
   face-name warns and spawns through the WORN face instead of lying. `spawn(n)`
   (worn face) is fully live."
  [{:keys [!face-compiled !face-context !face-state !viewport !settings !active-font]} n face-name]
  (let [compiled   @!face-compiled
        projection @!face-context]
    (cond
      (nil? compiled)
      (js/console.warn "[SCENE-FACES] no face worn — /face … first, then sceneFaces.spawn(n)")

      (nil? projection)
      (js/console.warn "[SCENE-FACES] no conversation projection yet — wait for the /face pull, then spawn")

      :else
      (let [worn      (some-> @!face-state :face)
            _         (when (and face-name (not= (name face-name) (some-> worn name)))
                        (js/console.warn
                          "[SCENE-FACES] STOP-CLAUSE: compiling an arbitrary named face"
                          (str face-name)
                          "client-side needs a server assembly-source pull (single guarded"
                          "!assembly-request artery). Spawning through the WORN face"
                          (str (some-> worn name))
                          "instead — to wear a DIFFERENT face on this conversation, /face it, THEN spawn."))
            vi        (vi-of n)
            geom      (face-geom @!viewport @!settings @!active-font projection)
            uids      (block-unit-ids projection)
            src       (conv-address projection)
            view-ctx  {:view-instance vi :address src :geom geom}
            tree      (ss/build-face-tree compiled projection view-ctx uids)
            w         (or (get-in tree [:bounds :w]) (:content-w geom) 600)
            ;; G11 wearing finding (07-13): real faces are wider than the
            ;; viewport (w ≈ 3.6k px), so x-off = n·(w+40) landed every copy
            ;; off-screen ("spawned at x 7352 — nothing happens"). Place
            ;; copies IN FRAME: scaled to ~45% of viewport width, right
            ;; half, cascading 60px per n; move/scale adjust from there.
            vw        (or (:width @!viewport) 1830.0)
            vh        (or (:height @!viewport) 1020.0)
            idx       (max 0 (- n 2))
            ;; scale clamped ≥0.25 so very wide faces stay readable (they may
            ;; overflow right — move/scale adjust); copies cascade a third of
            ;; a viewport down per n so they don't stack (G11 round-2 finding).
            scale     (-> (/ (* 0.45 vw) (max w 1.0)) (min 1.0) (max 0.25))
            x-off     (* vw (+ 0.5 (* 0.04 idx)))
            y-off     (* vh 0.36 idx)]
        ;; clean re-spawn: drop any prior instance at this n (orphan container)
        (close-instance! vi)
        (let [{:keys [container]} (register-face-instance!
                                    vi (backdrop-wrapped tree)
                                    {:x x-off :y y-off :scale scale :layer n
                                     :meta {:face (some-> worn name) :n n :src src}})]
          (swap! !vi-faces assoc vi
                 {:face (some-> worn name) :compiled compiled
                  :src src :geom geom :container container}))
        (js/console.log "[SCENE-FACES] spawned" (pr-str vi)
                        "face" (some-> worn name) "at x" x-off "y" y-off
                        "scale" scale "blocks" (count uids))
        (pr-str vi)))))

(defn- move-instance! [n x y]
  (if-let [slot (ss/slot @!scene-store (vi-of n))]
    (do (set-transform! (:container slot) {:x x :y y})
        (js/console.log "[SCENE-FACES] move vi" n "→" x y))
    (js/console.warn "[SCENE-FACES] no instance" n)))

(defn- scale-instance! [n s]
  (if-let [slot (ss/slot @!scene-store (vi-of n))]
    (do (set-transform! (:container slot) {:scale s})
        (js/console.log "[SCENE-FACES] scale vi" n "→" s))
    (js/console.warn "[SCENE-FACES] no instance" n)))

(defn- list-instances []
  (let [store @!scene-store
        reg   @!containers-registry
        vfs   @!vi-faces]
    (clj->js
      (mapv (fn [[vi slot]]
              {:vi        (pr-str vi)
               :face      (some-> (get-in vfs [vi :face]) name)
               :container (:container slot)
               :transform (get-in reg [:containers (:container slot)])
               :addresses (count (:addresses slot))})
            (:slots store)))))

(defn install-window-api!
  "window.sceneFaces — spawn/close/move/scale/list reader-face instances of the
   CURRENTLY worn conversation (scene-substrate P3b dev affordance, UNCOMMITTED;
   mirrors the ct-probe install pattern).
     spawn(n, faceName?) — register the nth instance through the WORN face (see
       the spawn-instance! STOP-CLAUSE for faceName); container offset to the
       right of the main face.
     close(n)            — despawn the nth instance (slot + container + vi-face).
     move/scale          — drive its transform (T10, no events).
     list()              — dump the store (vi · face · container · transform)."
  [{:keys [!face-scene !face-context !face-compiled !face-state
           !viewport !settings !active-font] :as atoms}]
  (set! (.-sceneFaces js/window)
        #js {:spawn (fn [n face] (spawn-instance! atoms (or n 2) (when face (keyword face))))
             :close (fn [n] (close-instance! (vi-of (or n 2))))
             :move  (fn [vi-n x y] (move-instance! (or vi-n 2) x y))
             :scale (fn [vi-n s] (scale-instance! (or vi-n 2) s))
             :list  (fn [] (list-instances))})
  (js/console.log "[SCENE-FACES] window.sceneFaces installed — sceneFaces.spawn(2); wear A, spawn, wear B, spawn(3) for two faces"))

(defn install-context-window-api!
  "window.sceneContext — inspect the deictic bundle for the current scene at the
   last pick (scene-substrate P4 dev affordance, UNCOMMITTED; mirrors the
   sceneFaces install). bundle() → the EDN as a JS object; edn() → the exact
   pr-str EDN string (round-trips read-string). The SAME bundle is attached to a
   cmd/agent turn at submit time (agent_flow.cljs) and logged [SCENE-CTX]."
  [{:keys [!viewport !scroll-y]}]
  (set! (.-sceneContext js/window)
        #js {:bundle (fn [] (clj->js (bundle-for-viewport @!viewport @!scroll-y)))
             :edn    (fn [] (pr-str (bundle-for-viewport @!viewport @!scroll-y)))})
  (js/console.log "[SCENE-CTX] window.sceneContext installed — sceneContext.bundle() / sceneContext.edn()"))
