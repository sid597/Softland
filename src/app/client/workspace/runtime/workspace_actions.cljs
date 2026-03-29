(ns app.client.workspace.runtime.workspace-actions
  "Semantic workspace actions — central dispatch for workspace-level transitions.
   Replaces scattered atom mutations with named entry points.

   SEMANTIC (routed here):
     select-artifact!, clear-artifact!,
     set-active-pane!, toggle-sidebar!, hide-sidebar!
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
           flow-state agent-output project]}]
  (let [file-open? (and (some? selected-artifact)
                        (= :file (:kind selected-artifact)))
        flow-active? (and (some? flow-state)
                          (contains? #{:intake :select :arrange :run :review
                                       :rework :finalize}
                                     (:node flow-state)))
        mode (cond
               (and flow-active? (= :intake (:node flow-state))) :flow-intake
               flow-active?       :flow-run
               file-open?         :file-workspace
               :else              :editor)]
    {:mode             mode
     :selected-artifact selected-artifact
     :file-open?       file-open?
     :flow-active?     flow-active?
     :project          project
     :active-pane      (or active-pane :editor)
     :sidebar-visible  (boolean sidebar-visible)
     :flow-node        (:node flow-state)
     :flow-session-id  (:session-id flow-state)
     :agent-status     (:status agent-output)
     :agent-run-id     (:run-id agent-output)
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
         :width-pct 1.0}])}))
