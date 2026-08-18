(ns app.client.workspace.runtime
  "Thin shell: build runtime context, wire modules, join the reactive loop.
   The PRODUCT boot is the bare ground (first-light T9): the worn
   conversation face over the genesis episode + the utterance tip. The dev
   workspace boot (?dev) died with the old workspace surface (dead-path
   census, Sid's ruling 2026-08-15) — the ground installs unconditionally."
  (:require [missionary.core :as m]
            [app.client.workspace.events :as events]
            [app.client.workspace.ground :as ground]
            [app.client.workspace.runtime.state :as state]
            [app.client.workspace.runtime.fonts :as fonts]
            [app.client.workspace.runtime.scroll :as scroll]
            [app.client.workspace.runtime.mouse :as mouse]
            [app.client.workspace.runtime.touch :as touch]
            [app.client.workspace.runtime.keyboard :as kbd]
            [app.client.workspace.trail-face.wiring :as trail-wiring]
            [app.client.workspace.face-wiring :as face-wiring]
            [app.client.workspace.block-edit-wiring :as block-edit-wiring]
            [app.client.workspace.runtime.render :as render]))

(defn start-loop!
  "Start the reactive loop. Builds the rt context, wires modules, returns a
   Missionary task that runs the render loop."
  [node device ctx geometry font-assets
   & {:keys [font-manifest gpu-budget
             !trail-request !trail-data !face-request !face-data !ingest-epoch-remote
             !assembly-request !assembly-data !face-list-request !face-list-data
             !facet-materials-request !facet-materials-data
             !material-inspector-request !material-inspector-data
             !interaction-table-request !interaction-table-data
             !material-portal-request !material-portal-data
             !face-wear-outbox !face-wear-result
             !block-edit-outbox !block-edit-result
             !block-truth-request !block-truth-data]}]

  (let [rt (state/make-runtime-state
             {:node node :device device :ctx ctx :geometry geometry
              :font-assets font-assets :font-manifest font-manifest
              :gpu-budget gpu-budget})

        atoms  (:atoms rt)
        layout (:layout rt)
        gpu    (:gpu rt)

        ;; ── Install watches & side effects ──────────────────────────
        _ (fonts/install-font-watch! atoms)

        ;; ── Trail face (view-mvp WP-B2) ─────────────────────────────
        _ (trail-wiring/load-coverage! (:!trail-coverage atoms))
        _ (trail-wiring/install-trail-face-wiring!
            atoms {:!trail-request !trail-request
                   :!trail-data !trail-data
                   :!ingest-epoch-remote !ingest-epoch-remote})

        ;; ── Faces-as-assemblies (framework CONTRACT §7, W1-INT) ─────
        _ (face-wiring/install-face-wiring!
            atoms {:!face-request !face-request
                   :!face-data !face-data
                   :!assembly-request !assembly-request
                   :!assembly-data !assembly-data
                   :!face-list-request !face-list-request
                   :!face-list-data !face-list-data
                   :!facet-materials-request !facet-materials-request
                   :!facet-materials-data !facet-materials-data
                   :!material-inspector-request !material-inspector-request
                   :!material-inspector-data !material-inspector-data
                   :!interaction-table-request !interaction-table-request
                   :!interaction-table-data !interaction-table-data
                   :!material-portal-request !material-portal-request
                   :!material-portal-data !material-portal-data
                   :!face-wear-outbox !face-wear-outbox
                   :!face-wear-result !face-wear-result
                   :!ingest-epoch-remote !ingest-epoch-remote})

        ;; ── Ground edit transport ───────────────────────────────────
        ;; The ground's outbox/result ride the back-arrow artery; the narrow
        ;; accepted-edit truth pull rides !block-truth-*.
        _ (block-edit-wiring/install-block-edit-wiring!
            atoms {:!block-edit-outbox !block-edit-outbox
                   :!block-edit-result !block-edit-result
                   :!block-truth-request !block-truth-request
                   :!block-truth-data !block-truth-data})

        ;; dev observability: the runtime atoms map on window, read-only use
        ;; (drives live INT checks — G14(b)/G15 console equality + state
        ;; inspection without reaching into compiled closures)
        _ (set! (.-__softland_atoms js/window) atoms)

        ;; first-light A P2 (T9): the PRODUCT boot is the bare ground — no
        ;; initial file, no workspace; the worn conversation face over the
        ;; genesis episode + the utterance tip.
        _ (ground/install-ground! atoms)

        ;; ── Event flows (fresh per instance) ────────────────────────
        >raf            (events/make-raf-flow)
        >resize         (events/>canvas-resize node)
        >wheel-events   (events/>wheel node)
        >mouse-events   (events/>mouse node)
        >meta-events    (events/>contextmenu node ground/ground-active?)
        >keyboard-events (events/>keyboard js/window)

        ;; ── Focus-based routing ─────────────────────────────────────
        <global-keys     (events/<global-events >keyboard-events)
        <ground-keyboard (events/<ground-input-keys >keyboard-events (:!focus atoms))

        ;; ── DOM listeners (raw, not Missionary) ─────────────────────
        _ (mouse/install-paste-handler! atoms)
        _ (touch/install-touch-adapter! node)]

    ;; ═══════════════════════════════════════════════════════════════
    ;; JOIN: all consumers run concurrently
    ;; ═══════════════════════════════════════════════════════════════
    (m/join vector
      ;; Viewport resize — update atom + sync canvas pixel dimensions immediately.
      ;; Setting canvas.width/height clears the swap chain, but unconditional RAF
      ;; redraws within 16ms. Without immediate sync, the browser stretches the
      ;; old pixel buffer to fit the new CSS box, causing visible flickering.
      (->> >resize
           (m/reduce
             (fn [_ {:keys [width height dpr]}]
               (let [safe-dpr (or dpr 1)]
                 (reset! (:!viewport atoms) {:width width :height height :dpr safe-dpr})
                 (set! (.-width node) (Math/floor (* width safe-dpr)))
                 (set! (.-height node) (Math/floor (* height safe-dpr))))
               nil)
             nil))

      ;; Consumers from modules
      (scroll/scroll-consumer atoms >wheel-events)
      (mouse/mouse-consumer atoms layout nil nil >mouse-events)
      (ground/meta-consumer >meta-events)
      (kbd/global-keys-consumer atoms <global-keys)
      (ground/ground-keys-consumer atoms <ground-keyboard)

      ;; Render loop (the terminal consumer)
      (render/render-consumer atoms layout gpu nil >raf))))
