(ns app.client.workspace.runtime.workspace-actions
  "Semantic workspace actions — central dispatch for workspace-level transitions.
   Replaces scattered atom mutations with named entry points.

   SEMANTIC (routed here):
     select-artifact!, clear-artifact!,
     set-active-pane!, toggle-sidebar!, hide-sidebar!,
     enter-workflow!, exit-workflow!
   EPHEMERAL (stays as direct mutation):
     !focus, !caret-visible, !cmd-panel :visible, !settings :visible,
     scrolls, hover, drag, mouse position")

(defn set-active-pane!
  "Set the primary visible pane. This is workspace truth — it determines
   which pane the user is working in. Keyboard focus follows pane for
   :editor and :chat; other panes don't auto-focus."
  [{:keys [!active-pane !focus !caret-visible]} pane]
  (reset! !active-pane pane)
  (case pane
    :editor (reset! !focus :editor)
    :chat   (do (reset! !focus :chat)
                (reset! !caret-visible true))
    nil))

(defn toggle-sidebar!
  "Toggle the sidebar file explorer."
  [{:keys [!sidebar-visible]}]
  (when !sidebar-visible
    (swap! !sidebar-visible not)))

(defn hide-sidebar!
  "Hide the sidebar (e.g. on Escape when nothing else to dismiss)."
  [{:keys [!sidebar-visible]}]
  (when !sidebar-visible
    (reset! !sidebar-visible false)))

;; ── Artifact selection ──────────────────────────────────────────

(defn select-artifact!
  "Select an artifact. This is the single semantic entry point for
   'the user chose something.' Coordinates selection, downstream
   I/O, and sidebar overlay updates.

   artifact-ref: {:kind :file :path '...' :name '...'}
                 {:kind :trail :run-id '...'}
                 {:kind :workflow :flow-id '...'}

   io: map with :fetch-file! and :fetch-dir! closures (from sidebar-io)
   opts: optional {:emit-sidebar? true} to also fire sidebar Rama action"
  [{:keys [!selected-artifact !current-file]} artifact-ref]
  (reset! !selected-artifact artifact-ref)
  ;; Keep !current-file in sync for the 40+ transitional readers.
  ;; Only set for :file artifacts; clear for everything else.
  (if (= :file (:kind artifact-ref))
    (reset! !current-file {:path (:path artifact-ref) :name (:name artifact-ref)})
    (reset! !current-file nil)))

(defn clear-artifact!
  "Clear the artifact selection (back to home / no file open).
   Also resets active pane to :editor — chat and preview are meaningless
   without an open artifact."
  [{:keys [!selected-artifact !current-file !active-pane !focus]}]
  (reset! !selected-artifact nil)
  (reset! !current-file nil)
  (reset! !active-pane :editor)
  (reset! !focus :editor))

;; ── Workflow entry/exit ──────────────────────────────────────────

(defn enter-workflow!
  "Workspace-level consequences of entering a workflow.
   DG command handlers own the FSM transition; this handles the
   substrate side: clear file artifact (workflow takes full screen),
   reset pane to editor, reset scroll."
  [{:keys [!selected-artifact !current-file !active-pane !focus !scroll-y]}]
  (reset! !selected-artifact nil)
  (reset! !current-file nil)
  (reset! !active-pane :editor)
  (reset! !focus :editor)
  (reset! !scroll-y 0))

(defn exit-workflow!
  "Workspace-level consequences of exiting a workflow (back to idle).
   DG command handlers call this after resetting !flow-state."
  [{:keys [!active-pane !focus !scroll-y]}]
  (reset! !active-pane :editor)
  (reset! !focus :editor)
  (reset! !scroll-y 0))

;; ── Editor event classification ──────────────────────────────────
;;
;; Phase 4A: classify editor events for the commitment boundary.
;; COMMITTED events change document content — candidates for Rama persistence.
;; EPHEMERAL events change cursor/selection — local-only, never persisted.
;;
;; Phase 4B will wire committed events through Rama and measure latency.
;; Until that measurement, the classification is the design contract.

(def editor-committed-events
  "Event types that change document content. These are the candidates
   for Rama persistence (Phase 4B will measure whether direct committed
   path is fast enough at keystroke rate)."
  #{:char :backspace :delete :enter :paste :cut :undo :redo})

(def editor-ephemeral-events
  "Event types that change cursor/selection/view only. Never persisted."
  #{:left :right :up :down :home :end :word-left :word-right
    :copy :eval})

(def editor-structural-events
  "Events that change document structure (folding). Committed, but
   persisted via !folded-lines (per-file), not !editor-doc.
   Rama persistence deferred to Phase 7 (workspace-schema widening)
   because folds need the artifact model for per-file scoping."
  #{:fold :unfold})

(defn editor-event-committed?
  "True if this editor event type is committed (content-changing or structural)."
  [event-type]
  (or (contains? editor-committed-events event-type)
      (contains? editor-structural-events event-type)))

;; ── Local world derivation ──────────────────────────────────────

(defn derive-effective-local-world
  "Derive the workspace's semantic root from current atom values.
   Pure function — no side effects, no atom reads.

   :mode is the key layout-branching field:
     :flow-intake   — DG workflow intake screen
     :flow-run      — DG workflow run/review screen
     :file-workspace — 3-pane file layout (editor + chat + preview)
     :editor        — standalone editor (no file open, no workflow)

   All consumers that currently branch on (flow-canvas-active?) and
   (some? current-file) can instead read :mode from this object."
  [{:keys [selected-artifact active-pane sidebar-visible
           flow-state agent-output project trail-face-state]}]
  (let [file-open? (and (some? selected-artifact)
                        (= :file (:kind selected-artifact)))
        flow-active? (and (some? flow-state)
                          (contains? #{:bootstrapping :intake :select :arrange :run :review
                                       :rework :finalize}
                                     (:node flow-state)))
        ;; trail face (view-mvp WP-B2): an explicit full-screen mode set by
        ;; the /trail entry command; wins while set, cleared by /trail off.
        trail-face (:face trail-face-state)
        mode (cond
               (= :text trail-face)     :trail-text
               (= :timeline trail-face) :trail-timeline
               (and flow-active? (contains? #{:bootstrapping :intake} (:node flow-state))) :flow-intake
               flow-active?       :flow-run
               file-open?         :file-workspace
               :else              :editor)
        ;; trail-room R1 guard: the trail faces are the ground + the rim -
        ;; NOTHING else is ambient (no sidebar). The sidebar never auto-
        ;; appears in a trail face; the effective world reports it hidden.
        trail-face?      (contains? #{:trail-text :trail-timeline} mode)]
    {:mode             mode
     :selected-artifact selected-artifact
     :file-open?       file-open?
     :flow-active?     flow-active?
     :project          project
     :active-pane      (or active-pane :editor)
     ;; R1 guard: never report the sidebar visible in a trail face
     :sidebar-visible  (boolean (and sidebar-visible (not trail-face?)))
     :flow-node        (:node flow-state)
     :flow-session-id  (:session-id flow-state)
     :agent-status     (:status agent-output)
     :agent-run-id     (:run-id agent-output)
     ;; Split — preserved co-presence. Describes what's held together,
     ;; which artifact is primary, what's adjacent.
     ;; :direction = :horizontal (panes side-by-side) or :single (one pane fills)
     ;; :primary = :pane/id of the primary artifact pane
     ;; :adjacent = vec of :pane/id that share the split
     :split
     (case mode
       :file-workspace {:direction :horizontal
                        :primary   :main
                        :adjacent  [:right :preview]}
       {:direction :single
        :primary   :main
        :adjacent  []})
     ;; Pane descriptors — semantic fills derived from mode + artifacts.
     ;; Each pane: {:pane/id :role :artifact-ref :content}
     ;; :role = what the pane is for (:primary-artifact, :trail, :preview, :command, :flow-canvas)
     ;; :artifact-ref = which artifact it shows (nil = empty/placeholder)
     ;; :content = keyword for non-artifact content (:cmd-panel, :intake-tree, etc.)
     :panes
     (case mode
       :file-workspace
       [{:pane/id :main    :role :primary-artifact
         :artifact-ref selected-artifact
         :width-pct 0.4}
        {:pane/id :right   :role :trail
         :artifact-ref (when (:run-id agent-output)
                         {:kind :trail :run-id (:run-id agent-output)})
         :width-pct 0.55}
        {:pane/id :preview :role :preview
         :artifact-ref nil
         :width-pct 0.05}]

       :flow-intake
       [{:pane/id :main    :role :flow-canvas
         :content :intake-tree
         :width-pct 1.0}]

       :flow-run
       [{:pane/id :main    :role :flow-canvas
         :content :run-detail
         :width-pct 1.0}]

       :editor
       [{:pane/id :main    :role :primary-artifact
         :artifact-ref nil
         :width-pct 1.0}]

       ;; trail faces: single full-width pane. The :panes case has NO
       ;; default branch, so these entries are load-bearing (a new mode
       ;; without them throws here).
       :trail-text
       [{:pane/id :main    :role :trail-face
         :content :trail-text
         :width-pct 1.0}]

       :trail-timeline
       [{:pane/id :main    :role :trail-face
         :content :trail-timeline
         :width-pct 1.0}])}))

(defn local-world-mode
  "Read the effective mode from a local-world object, defaulting to :editor
   during boot before the first derivation runs."
  [local-world]
  (or (:mode local-world) :editor))

(defn local-world-flow?
  "True when the local world is showing a workflow surface."
  [local-world]
  (contains? #{:flow-intake :flow-run} (local-world-mode local-world)))

(defn local-world-intake?
  "True when the local world is showing the intake workflow surface."
  [local-world]
  (= :flow-intake (local-world-mode local-world)))

(defn local-world-run?
  "True when the local world is showing a run/review workflow surface."
  [local-world]
  (= :flow-run (local-world-mode local-world)))

(defn local-world-trail-text?
  "True when the local world is showing the View-3 trail text face."
  [local-world]
  (= :trail-text (local-world-mode local-world)))

(defn local-world-trail-timeline?
  "True when the local world is showing the trail timeline face."
  [local-world]
  (= :trail-timeline (local-world-mode local-world)))

(defn local-world-trail-face?
  "True when the local world is showing either trail face."
  [local-world]
  (contains? #{:trail-text :trail-timeline} (local-world-mode local-world)))

(defn local-world-file-workspace?
  "True when the local world is showing the file workspace split."
  [local-world]
  (= :file-workspace (local-world-mode local-world)))

(defn pane-descriptor
  "Look up a semantic pane descriptor by stable :pane/id."
  [local-world pane-id]
  (some #(when (= pane-id (:pane/id %)) %) (:panes local-world)))

(defn pane-width-pct
  "Read a pane's semantic width percentage, falling back when the pane is absent."
  [local-world pane-id fallback]
  (or (:width-pct (pane-descriptor local-world pane-id))
      fallback))
