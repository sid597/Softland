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
            [app.client.workspace.containers :as ctn]))

;; ---------------------------------------------------------------------------
;; State — the ONE store + the ONE registry (CONTRACT §4/§5)
;; ---------------------------------------------------------------------------

(defonce !scene-store (atom (ss/empty-store)))
(defonce !containers-registry (atom (ctn/empty-registry)))

;; Face containers start well above the P2 probe's range (1..~64) so the two
;; dev tools never fight over cids in the shared containers buffer. write-
;; containers! writes the contiguous [1..max-cid] range, so keeping the base
;; modest keeps that upload small (CONTRACT G4 finding #3 — whole-range write).
(def ^:private face-cid-base 100)
(defonce ^:private !next-cid (atom face-cid-base))

(defn- vi-of [n] [:vi :reader-face n])

;; ---------------------------------------------------------------------------
;; Pure-ish helpers
;; ---------------------------------------------------------------------------

(defn block-unit-ids
  "The set of block unit-ids in a served face data-context (turns → blocks →
   :id) — the SAME derivation the legacy handle-face-assembly-click! uses, so
   the store path and the legacy path resolve identical blocks."
  [ctx]
  (into #{} (comp (mapcat :blocks) (keep :id)) (:turns ctx)))

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
  [vi tree {:keys [x y scale layer meta]
            :or   {x 0.0 y 0.0 scale 1.0 layer 1}}]
  (let [cid (swap! !next-cid inc)]
    (swap! !containers-registry ctn/add-container cid
           {:x x :y y :scale scale :camera :world :layer layer})
    (assert-container-registered! cid)
    (swap! !scene-store ss/upsert-slot vi
           {:tree tree :container cid :meta (or meta {})})
    {:vi vi :container cid}))

(defn refresh-all-slots!
  "Echo fan-out (G7 / deliverable #6): rebuild EVERY registered view-instance's
   slot from ONE freshly-built face scene. Called at the render consumer edge
   when the projection updates (a block edit echo rebuilds the singleton face
   scene). `stamped-tree` is the SAME conversation for every instance, so one
   edit patches all appearances; each slot keeps its own :container/:meta/
   :stratum. One swap! — the whole fan-out is atomic."
  [stamped-tree]
  (let [reg-cids (set (keys (:containers @!containers-registry)))]
    (swap! !scene-store
           (fn [store]
             (reduce (fn [st [vi slot]]
                       (assert (contains? reg-cids (:container slot))
                               (str "finding #5: slot " vi " container "
                                    (:container slot) " unregistered on refresh"))
                       (ss/upsert-slot st vi {:tree      stamped-tree
                                              :container (:container slot)
                                              :meta      (:meta slot)
                                              :stratum   (:stratum slot)}))
                     store
                     (:slots store))))))

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
;; GPU-frame derivations (missionary flows; merged at the render consumer edge)
;; ---------------------------------------------------------------------------

(defn- stamp-container-idx
  "Stamp a slot's :container onto each flattened op so the GPU places every
   glyph/rect/shadow through its container transform (P2 plumbing: pack-rect,
   pack-shadow and shape-* all read :container-idx). Text ops are nested
   [[op..]..] — the tree->text-ops shape update-text-data consumes as lines;
   rects and shadows are flat."
  [{:keys [ops container]}]
  {:text    (mapv (fn [line] (mapv #(assoc % :container-idx container) line))
                  (:text ops))
   :rects   (mapv #(assoc % :container-idx container) (:rects ops))
   :shadows (mapv #(assoc % :container-idx container) (:shadows ops))})

(defn <store-frame
  "ONE m/latest over !scene-store → the store's whole GPU contribution
   {:text-ops <nested> :rects <flat> :shadows <flat>}, each op stamped with its
   slot's :container-idx. ONE flow co-deriving all three op kinds together —
   no diamond off the store (T3). Recomputes ONLY when the store changes, so
   unrelated frames keep the SAME value (identical? — no re-upload on scroll/
   cmd/etc.)."
  []
  (m/latest
    (fn [store]
      (let [parts (mapv stamp-container-idx (vals (:slots store)))]
        {:text-ops (into [] (mapcat :text) parts)
         :rects    (into [] (mapcat :rects) parts)
         :shadows  (into [] (mapcat :shadows) parts)}))
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
  [!face-scene !face-context n]
  (if-let [scene @!face-scene]
    (let [uids    (block-unit-ids @!face-context)
          stamped (ss/stamp-block-addresses scene uids)
          w       (or (get-in scene [:bounds :w]) 600)
          vi      (vi-of n)
          x-off   (* (dec n) (+ w 40.0))]  ; the nth instance steps to the right of the main face
      (register-face-instance! vi stamped
                               {:x x-off :y 0.0 :scale 1.0 :layer n
                                :meta {:face :reader-face :n n
                                       :src (get-in scene [:data :address])}})
      (js/console.log "[SCENE-FACES] spawned" (pr-str vi) "at x" x-off
                      "blocks" (count uids))
      (pr-str vi))
    (js/console.warn "[SCENE-FACES] no face worn — /face … first, then sceneFaces.spawn(n)")))

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
        reg   @!containers-registry]
    (clj->js
      (mapv (fn [[vi slot]]
              {:vi        (pr-str vi)
               :container (:container slot)
               :transform (get-in reg [:containers (:container slot)])
               :addresses (count (:addresses slot))})
            (:slots store)))))

(defn install-window-api!
  "window.sceneFaces — spawn/move/scale/list extra reader-face instances of the
   CURRENT worn conversation (scene-substrate P3 dev affordance, UNCOMMITTED;
   mirrors the ct-probe install pattern). spawn(n) registers the nth instance
   in a container offset to the right of the main face; move/scale drive its
   transform (T10, no events); list() dumps the store."
  [{:keys [!face-scene !face-context]}]
  (set! (.-sceneFaces js/window)
        #js {:spawn (fn [n] (spawn-instance! !face-scene !face-context (or n 2)))
             :move  (fn [vi-n x y] (move-instance! (or vi-n 2) x y))
             :scale (fn [vi-n s] (scale-instance! (or vi-n 2) s))
             :list  (fn [] (list-instances))})
  (js/console.log "[SCENE-FACES] window.sceneFaces installed — sceneFaces.spawn(2)"))
