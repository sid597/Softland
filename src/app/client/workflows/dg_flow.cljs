(ns app.client.workflows.dg-flow
  "DG workflow: flow state machine, intake tree, run/review trees, ticket layout."
  (:require [clojure.string :as str]
            [cljs.reader :as reader]
            [app.client.workspace.rect-tree :refer [rt-node wrap-line resolve-layout tree->rects tree->shadows tree->text-ops]]
            [app.client.workspace.ui-primitives :as ui
             :refer [dt typo-title typo-subtitle typo-body typo-caption
                     list-row-h list-group-header-h list-left-pane-pct
                     list-padding-x list-item-inset list-padding-top
                     list-checkbox-size list-divider-w list-group-gap list-footer-h
                     ui-card ui-badge ui-button ui-divider ui-panel
                     ui-panel-header ui-panel-content ui-panel-footer
                     ui-panel-group ui-list-item ui-checkbox ui-priority-dot
                     ui-scrollbar build-empty-state]]
            [app.client.workspace.trail :refer [trail->chat-nodes]]))

;; ============================================================================
;; FLOW STATE MACHINE (V0 — "Prompts as API Calls")
;; ============================================================================
;; Encodes the state graph from commission-consensus.md Section 4.
;; Pure functions — no atoms, no side effects.

(def flow-transitions
  "Directed graph of valid state transitions.
   Keys are from-states, values are sets of reachable to-states.
   NOTE: :idle is a pre-graph state (not in Section 4's locked graph).
   Arrangement is an intake subphase (no :arrange node).
   Run is triggered directly from :intake when batch has lanes."
  {:idle            #{:bootstrapping}
   :bootstrapping   #{:intake :bootstrapping}         ;; retry on failure
   :intake          #{:run :bootstrapping}             ;; run directly from intake
   :run             #{:review :intake}                 ;; back-edge: scope change
   :review          #{:rework :finalize :intake}       ;; back-edge: re-scope
   :rework          #{:review :intake}                 ;; back-edge: re-scope
   :finalize        #{:intake}})

(defn valid-transition?
  "Check if moving from `from` to `to` is allowed.
   Human override: any state can jump to :intake."
  [from to]
  (or (contains? (get flow-transitions from) to)
      (= to :intake)))

(defn initial-flow-state
  "Fresh flow state for a new session.
   Starts in :idle — ticket list only appears after /bootstrap (/dg).
   Object model: batch > lanes > runs > artifacts > decisions."
  []
  {:node :idle
   :tickets []         ;; all fetched tickets
   :batch {:lanes []   ;; ordered vec of ticket indices (execution stack)
           :id nil}    ;; batch identifier
   :active-lane-idx 0  ;; focused lane in the stack
   :runs {}            ;; {lane-idx -> [{:id :status :trail :artifacts}]}
   :decisions {}       ;; {lane-idx -> :approve/:rework/:defer/:finalize}
   :session-id nil
   :history []
   ;; Legacy compat — kept during transition, will be removed
   :selected []
   :arrangement nil})

(defn set-selection
  "Update flow-state with new selection, syncing batch lanes and clamping active-lane-idx."
  [flow-state new-sel]
  (let [n (count new-sel)
        ai (or (:active-lane-idx flow-state) 0)
        clamped-ai (if (pos? n) (min ai (dec n)) 0)]
    (-> flow-state
        (assoc :selected new-sel)
        (assoc-in [:batch :lanes] new-sel)
        (assoc :active-lane-idx clamped-ai))))

(defn transition-flow-state
  "Attempt to transition flow state. Returns updated state or nil if invalid.
   Appends previous node to :history for auditability."
  [flow-state to-node & [extra-merge]]
  (let [from (:node flow-state)]
    (when (valid-transition? from to-node)
      (cond-> (assoc flow-state
                :node to-node
                :history (conj (:history flow-state) from))
        extra-merge (merge extra-merge)))))

(defn flow-canvas-active?
  "True when the flow state machine is in a node that shows the master-detail
   layout instead of the code editor. All active flow states use this view —
   left pane is always the map, right pane is always the current artifact."
  [flow-state]
  (contains? #{:intake :run :review :rework :finalize} (:node flow-state)))

(defn parse-dg-command
  "Parse DG workflow commands. Returns nil when the command does not belong to
   the DG workflow."
  [trimmed]
  (cond
    (= trimmed "/bootstrap")
    {:kind :flow-bootstrap}

    (= trimmed "/mock")
    {:kind :flow-mock-bootstrap}

    (str/starts-with? trimmed "/select ")
    (let [args (-> trimmed (subs (count "/select ")) str/trim (str/split #"\s+"))
          indices (try (mapv #(dec (js/parseInt % 10)) args)
                       (catch :default _ nil))]
      (if (and (seq indices) (every? #(and (number? %) (not (js/isNaN %))) indices))
        {:kind :flow-select :indices indices}
        {:kind :error :message "Usage: /select 1 2 3 (1-based ticket numbers)"}))

    (= trimmed "/run")
    {:kind :flow-run}

    (= trimmed "/review")
    {:kind :flow-review}

    (str/starts-with? trimmed "/rework ")
    {:kind :flow-rework :comment (-> trimmed (subs (count "/rework ")) str/trim)}

    (= trimmed "/rework")
    {:kind :error :message "Usage: /rework <feedback comment>"}

    (= trimmed "/finalize")
    {:kind :flow-finalize}

    (= trimmed "/status")
    {:kind :flow-status}

    (= trimmed "/reset")
    {:kind :flow-reset}

    :else
    nil))

(def mock-tickets
  [{:id "DIS-101" :title "Fix SSO auth flow for enterprise users"
    :status "In Progress" :assignee "Siddharth" :priority 1
    :description "Enterprise SSO login fails when SAML assertion contains multiple group claims. Need to handle array-valued attributes in the assertion parser."}
   {:id "DIS-102" :title "Add batch export for reasoning trails"
    :status "Todo" :assignee "Siddharth" :priority 2
    :description "Users want to export multiple reasoning trails as a single PDF or markdown bundle for offline review and sharing with stakeholders."}
   {:id "DIS-103" :title "WebGPU text rendering perf regression"
    :status "In Progress" :assignee "Siddharth" :priority 1
    :description "After adding MSDF font atlas, frame times spiked from 2ms to 8ms on large files. Suspect redundant texture uploads per frame."}
   {:id "DIS-104" :title "Design review screen diff viewer"
    :status "Backlog" :assignee "unassigned" :priority 3
    :description "Screen 4 needs a side-by-side diff viewer for code changes produced by agent runs. Should support syntax highlighting and inline comments."}
   {:id "DIS-105" :title "Implement parallel lane arrangement"
    :status "Todo" :assignee "unassigned" :priority 2
    :description "The arrange step currently only supports sequential chains. Add parallel lane layout so independent tickets can run concurrently."}
   {:id "DIS-106" :title "Add keyboard navigation to ticket list"
    :status "Backlog" :assignee "unassigned" :priority 4
    :description "j/k to move selection, space to toggle, enter to view detail. Vim-style navigation for the intake screen ticket list."}
   {:id "DIS-107" :title "Streaming token counter in agent panel"
    :status "Done" :assignee "Siddharth" :priority 3
    :description "Show a live token count in the agent output panel header during streaming. Helps users gauge cost and progress of long-running agent sessions."}
   {:id "DIS-108" :title "Fix scroll clamping on window resize"
    :status "In Progress" :assignee "Siddharth" :priority 2
    :description "When the browser window is resized smaller, scroll position can exceed content bounds. Need to re-clamp scroll-y in the resize handler."}
   {:id "DIS-109" :title "Rama PState schema migration for trails"
    :status "Todo" :assignee "unassigned" :priority 2
    :description "The reasoning trail PState needs a schema evolution to support the new structured tool-use events. Plan the migration path."}
   {:id "DIS-110" :title "Release v0.2.0 milestone"
    :status "Released" :assignee "Siddharth" :priority 1
    :description "Tag and release the v0.2.0 milestone including streaming agent output, rect tree UI, and the intake list view."}])

(defn handle-dg-command!
  "Apply DG workflow side effects through the provided runtime env.
   Returns true when the command was handled."
  [parsed {:keys [!flow-state !scroll-y !agent-output show-flow-info! fire-flow-run!
                  enter-workflow! exit-workflow!]}]
  (case (:kind parsed)
    :flow-bootstrap
    (do
      (let [flow @!flow-state
            next (or (transition-flow-state flow :bootstrapping)
                     (when (= (:node flow) :intake)
                       (transition-flow-state flow :bootstrapping)))]
        (if next
          (do
            (reset! !flow-state next)
            (when enter-workflow! (enter-workflow!))
            (show-flow-info! "Fetching tickets from Linear...")
            (-> (js/fetch "/api/linear/issues?team=Engineering")
                (.then (fn [resp] (.text resp)))
                (.then (fn [text]
                         (let [data (reader/read-string text)
                               tickets (:tickets data)]
                           (js/console.log "[FLOW][BOOTSTRAP]"
                                           (clj->js {:ok (:ok data)
                                                     :ticket-count (count tickets)}))
                           (if (and (:ok data) (seq tickets))
                             (do
                               (swap! !flow-state assoc :node :intake :tickets tickets)
                               (reset! !scroll-y 0)
                               (show-flow-info!
                                (str "Bootstrap complete. " (count tickets) " tickets loaded.")))
                             (do
                               (swap! !flow-state assoc :node :bootstrapping)
                               (show-flow-info!
                                (str "Bootstrap failed: " (or (:error data) "no tickets found")
                                     "\nUse /bootstrap to retry.")))))))
                (.catch (fn [err]
                          (swap! !flow-state assoc :node :bootstrapping)
                          (show-flow-info!
                           (str "Bootstrap fetch error: " (.-message err)
                                "\nUse /bootstrap to retry."))))))
          (show-flow-info!
           (str "Cannot bootstrap from state: " (name (:node flow))
                "\nUse /reset to return to idle."))))
      true)

    :flow-mock-bootstrap
    (do
      (let [flow @!flow-state
            can-transition (or (= (:node flow) :idle)
                               (= (:node flow) :intake))]
        (if can-transition
          (do
            (reset! !flow-state (assoc (initial-flow-state)
                                       :node :intake
                                       :tickets mock-tickets))
            (when enter-workflow! (enter-workflow!))
            (show-flow-info!
             (str "Mock bootstrap complete. " (count mock-tickets) " tickets loaded.")))
          (show-flow-info!
           (str "Cannot bootstrap from state: " (name (:node flow))
                "\nUse /reset to return to idle."))))
      true)

    :flow-select
    (do
      (let [flow @!flow-state
            indices (:indices parsed)
            tickets (:tickets flow)]
        (if (not= (:node flow) :intake)
          (show-flow-info!
           (str "Cannot select tickets in state: " (name (:node flow))
                "\nMust be in :intake state."))
          (let [invalid (filter #(or (neg? %) (>= % (count tickets))) indices)]
            (if (seq invalid)
              (show-flow-info!
               (str "Invalid ticket numbers: "
                    (str/join ", " (map inc invalid))
                    "\nValid range: 1-" (count tickets)))
              (do
                (swap! !flow-state set-selection (vec indices))
                (show-flow-info!
                 (str "Selected " (count indices) " ticket(s):\n"
                      (str/join "\n" (map (fn [i]
                                             (let [t (nth tickets i)]
                                               (str "  " (inc i) ". " (:id t) " — " (:title t))))
                                           indices))
                      "\n\nReorder in the stack, then /run to start.")))))))
      true)

    :flow-run
    (do
      (let [flow @!flow-state
            lanes (get-in flow [:batch :lanes])
            selected (:selected flow)]
        (cond
          ;; No lanes or selected tickets
          (and (empty? lanes) (empty? selected))
          (show-flow-info! "No tickets selected. Select tickets first, then /run.")

          :else
          (let [;; Use batch lanes if set, fall back to selected for compat
                effective-lanes (if (seq lanes) lanes selected)
                next (transition-flow-state flow :run)]
            (if next
              (do
                (reset! !flow-state (assoc next
                                           :batch (assoc (:batch next) :lanes effective-lanes)))
                (reset! !scroll-y 0)
                (fire-flow-run! :run-sequential
                                :on-done (fn []
                                           (swap! !flow-state assoc :node :review)
                                           (show-flow-info!
                                            (str "Run complete. Now in review state.\n\n"
                                                 "Use /rework <comment> to request changes,\n"
                                                 "or /finalize to wrap up.")))))
              (show-flow-info!
               (str "Cannot run from state: " (name (:node flow))
                    "\nMust be in :intake state."))))))
      true)

    :flow-review
    (do
      (let [flow @!flow-state
            msg (str "Current state: " (name (:node flow))
                     (when (= (:node flow) :review)
                       "\n\nOptions:\n  /rework <comment> — request changes\n  /finalize — wrap up batch"))
            has-trail? (seq (:trail @!agent-output))]
        (if has-trail?
          (js/console.log "[FLOW][REVIEW]" msg)
          (show-flow-info! msg)))
      true)

    :flow-rework
    (do
      (let [flow @!flow-state
            next (transition-flow-state flow :rework {:rework-comment (:comment parsed)})]
        (if next
          (do
            (reset! !flow-state next)
            (fire-flow-run! :rework
                            :on-done (fn []
                                       (swap! !flow-state assoc :node :review)
                                       (show-flow-info!
                                        (str "Rework complete. Back in review.\n\n"
                                             "Use /rework <comment> for more changes,\n"
                                             "or /finalize to wrap up.")))))
          (show-flow-info!
           (str "Cannot rework from state: " (name (:node flow))
                "\nExpected :review. Current: " (name (:node flow))))))
      true)

    :flow-finalize
    (do
      (let [flow @!flow-state
            next (transition-flow-state flow :finalize)]
        (if (or next (= (:node flow) :review))
          (do
            (reset! !flow-state (or next
                                    (assoc flow :node :finalize
                                           :history (conj (:history flow) (:node flow)))))
            (fire-flow-run! :finalize
                            :on-done (fn []
                                       (swap! !flow-state assoc
                                              :node :intake
                                              :selected []
                                              :arrangement nil
                                              :batch {:lanes [] :id nil}
                                              :active-lane-idx 0
                                              :runs {}
                                              :decisions {})
                                       (reset! !scroll-y 0)
                                       (show-flow-info!
                                        (str "Batch finalized. Returned to intake.\n\n"
                                             "Tickets still loaded. Use /select to start a new batch,\n"
                                             "or /bootstrap to refresh tickets.")))))
          (show-flow-info!
           (str "Cannot finalize from state: " (name (:node flow))
                "\nExpected :review. Current: " (name (:node flow))))))
      true)

    :flow-status
    (do
      (let [flow @!flow-state
            msg (str "=== Flow State ===\n"
                     "Node: " (name (:node flow)) "\n"
                     "Session: " (or (:session-id flow) "none") "\n"
                     "Tickets: " (count (:tickets flow)) "\n"
                     "Selected: " (if (seq (:selected flow))
                                    (str/join ", " (map inc (:selected flow)))
                                    "none") "\n"
                     "Arrangement: " (or (some-> (:arrangement flow) name) "none") "\n"
                     "History: [" (str/join " -> " (map name (:history flow))) "]")
            has-trail? (seq (:trail @!agent-output))]
        (if has-trail?
          (js/console.log "[FLOW][STATUS]" msg)
          (show-flow-info! msg)))
      true)

    :flow-reset
    (do
      (reset! !flow-state (initial-flow-state))
      (when exit-workflow! (exit-workflow!))
      (show-flow-info! "Flow state reset to idle.\nUse /bootstrap to start fresh.")
      true)

    false))

(def priority-colors
  "Priority level \u2192 RGBA color for the priority dot."
  {1 {:r 0.95 :g 0.30 :b 0.30 :a 1.0}   ;; urgent - red
   2 {:r 0.95 :g 0.60 :b 0.25 :a 1.0}   ;; high - orange
   3 {:r 0.90 :g 0.80 :b 0.30 :a 1.0}   ;; medium - yellow
   4 {:r 0.45 :g 0.85 :b 0.45 :a 1.0}   ;; low - green
   0 {:r 0.55 :g 0.55 :b 0.60 :a 0.8}}) ;; none - gray

(def status-group-order
  ["Ready to Merge" "Ready for Review" "In Progress" "Todo" "Backlog" "Done" "Released"])

(def status-icons
  {"Ready to Merge"    "\u25CF"   ;; \u25CF
   "Ready for Review"  "\u25CB"   ;; \u25CB
   "In Progress"       "\u25D0"   ;; \u25D0
   "Todo"              "\u25CB"   ;; \u25CB
   "Backlog"           "\u25C7"   ;; \u25C7
   "Done"              "\u2713"   ;; \u2713
   "Released"          "\u2713"}) ;; \u2713

(def status-icon-colors
  {"Ready to Merge"    {:r 0.45 :g 0.85 :b 0.45 :a 1.0}  ;; green
   "Ready for Review"  {:r 0.55 :g 0.75 :b 0.95 :a 1.0}  ;; blue
   "In Progress"       {:r 0.95 :g 0.75 :b 0.30 :a 1.0}  ;; amber
   "Todo"              {:r 0.65 :g 0.65 :b 0.70 :a 0.8}  ;; gray
   "Backlog"           {:r 0.55 :g 0.55 :b 0.60 :a 0.6}  ;; dim gray
   "Done"              {:r 0.45 :g 0.85 :b 0.45 :a 0.7}  ;; green dim
   "Released"          {:r 0.45 :g 0.85 :b 0.45 :a 0.5}}) ;; green dimmer

(defn group-tickets-by-status
  "Group tickets into ordered sections by status.
   Returns [{:status \"Ready to Merge\" :icon \"\u25CF\" :count N
             :tickets [{:idx 0 :ticket {...}} ...]} ...]
   :idx is the 0-based index into the original flat tickets vector."
  [tickets]
  (let [indexed (mapv (fn [i t] {:idx i :ticket t}) (range) tickets)
        known-set (set status-group-order)
        known-groups (->> status-group-order
                          (mapv (fn [status]
                                  (let [group-tix (filterv #(= (:status (:ticket %)) status) indexed)]
                                    (when (seq group-tix)
                                      {:status status
                                       :icon (get status-icons status "?")
                                       :count (count group-tix)
                                       :tickets group-tix}))))
                          (filterv some?))
        unknown-tix (filterv #(not (known-set (:status (:ticket %)))) indexed)
        unknown-groups (when (seq unknown-tix)
                         [{:status "Other" :icon "?" :count (count unknown-tix) :tickets unknown-tix}])]
    (into known-groups unknown-groups)))

(defn ticket-list-layout
  "Compute layout entries for the grouped ticket list in CONTENT SPACE (no scroll).
   Returns flat vec of {:type :group-header|:ticket-row ...} with :y positions.
   collapsed-groups is a set of status strings."
  [grouped-tickets collapsed-groups selected-set]
  (loop [groups (seq grouped-tickets)
         y list-padding-top
         result []]
    (if-not groups
      result
      (let [{:keys [status icon tickets] grp-count :count} (first groups)
            collapsed? (contains? collapsed-groups status)
            header {:type :group-header
                    :status status :icon icon :count grp-count
                    :y y :collapsed? collapsed?}
            next-y (+ y list-group-header-h)
            rows (if collapsed?
                   []
                   (mapv (fn [i {:keys [idx ticket]}]
                           {:type :ticket-row
                            :idx idx :ticket ticket
                            :y (+ next-y (* i list-row-h))
                            :selected? (contains? selected-set idx)})
                         (range) tickets))
            total-rows-h (if collapsed? 0 (* (count tickets) list-row-h))]
        (recur (next groups)
               (+ next-y total-rows-h)
               (into (conj result header) rows))))))

(defn list-content-height
  "Total content height for the grouped ticket list (for scroll clamping).
   Accounts for group gaps and padding."
  [grouped-tickets collapsed-groups]
  (let [n-groups (count grouped-tickets)
        gaps (* (max 0 (dec n-groups)) list-group-gap)]
    (+ list-padding-top gaps 4  ;; 4 = panel-content top padding
       (reduce (fn [h {:keys [status tickets]}]
                 (+ h list-group-header-h
                    (if (contains? collapsed-groups status) 0 (* (count tickets) list-row-h))))
               0
               grouped-tickets))))

;; --- Drag state machine (IDLE → PENDING → DRAGGING → DROP/CLICK) -----------

(def drag-threshold-px 5)

(defn drag-pending?
  "True when drag state is :pending (mousedown happened, waiting for threshold)."
  [drag-state] (= :pending (:phase drag-state)))

(defn drag-active?
  "True when drag state is :dragging (past threshold, item follows cursor)."
  [drag-state] (= :dragging (:phase drag-state)))

(defn drag-distance
  "Euclidean distance from drag origin to point (px, py)."
  [{:keys [origin]} px py]
  (let [dx (- px (:x origin))
        dy (- py (:y origin))]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))

;; ============================================================================
;; INTAKE TREE BUILDER (rect tree for Screen 1)
;; ============================================================================

(defn build-right-detail
  "Build the right pane content for the intake right panel.
   Returns a vector of rt-node children.
   4 states: empty tickets, no selection, single selection, execution stack."
  [tickets selected right-w viewport-h font-size char-advance active-lane-idx detail-scroll-y]
  (let [right-inner-w (- right-w (* 2 list-padding-x))
        detail-max-chars (if (pos? char-advance)
                           (max 20 (int (/ right-inner-w char-advance)))
                           60)
        cy (/ viewport-h 2)]
    (cond
      ;; No tickets loaded — centered empty state
      (empty? tickets)
      [(build-empty-state :empty-tickets right-w viewport-h
         {:icon "{}"
          :headline "No tickets"
          :description "Run /bootstrap to fetch tickets from Linear."})]

      ;; Nothing selected — centered empty state
      (empty? selected)
      [(build-empty-state :no-selection right-w viewport-h
         {:icon "<>"
          :headline "Select a ticket"
          :description "Click a ticket from the list to view its details."})]

      ;; Single ticket selected — rich detail view
      (= 1 (count selected))
      (let [idx    (first selected)
            tkt    (nth tickets idx nil)
            ttitle (or (:title tkt) "Untitled")
            tstatus (or (:status tkt) "?")
            tprio  (or (:priority tkt) 0)
            tassn  (or (:assignee tkt) "unassigned")
            tdesc  (or (:description tkt) "")
            card-x (:lg (:spacing dt))
            card-y 36
            card-w (- right-w (* 2 (:lg (:spacing dt))))
            card-h (max 180 (- viewport-h 72))
            content-w (- card-w (* 2 list-padding-x))
            title-size (max (+ font-size 6) (:size typo-title))
            meta-size  (max font-size (:size typo-body))
            body-size  (max (+ font-size 4) (:size typo-subtitle))
            hint-size  (max (- body-size 4) (:size typo-caption))
            font-scale (if (pos? font-size) (/ char-advance font-size) 0.56)
            body-char-advance (* body-size font-scale)
            plabel (case tprio 1 "Urgent" 2 "High" 3 "Medium" 4 "Low" "None")
            mline  (str tstatus "  |  " plabel "  |  " tassn)
            title-max-chars (if (pos? body-char-advance)
                              (max 20 (int (/ content-w body-char-advance)))
                              detail-max-chars)
            tlines (vec (mapcat #(wrap-line % title-max-chars)
                                (str/split-lines (or ttitle ""))))
            tlh    (max 20 (+ title-size 6))
            hdr-y  28
            meta-y (+ hdr-y (* (count tlines) tlh) 4)
            divider-y (+ meta-y 18)
            section-y (+ divider-y 22)
            footer-y (- card-h 18)
            desc-y (+ section-y 18)
            desc-max-h (max 84 (- footer-y desc-y 18))
            desc-nodes (if (seq tdesc)
                         (trail->chat-nodes [{:kind :reasoning :text tdesc}]
                                            content-w body-size body-char-advance 0.0 #{})
                         [(build-empty-state :ticket-md-empty content-w desc-max-h
                            {:icon "md"
                             :headline "No description"
                             :description "This issue does not include a Linear description yet."})])
            desc-content-h (reduce + 0 (map #(get-in % [:bounds :h] 0) desc-nodes))
            desc-gap 8
            desc-stack-h (+ desc-content-h (* desc-gap (max 0 (dec (count desc-nodes)))))
            overflow? (> desc-stack-h desc-max-h)
            max-scroll (max 0 (- desc-stack-h desc-max-h))
            clamped-scroll (min (max detail-scroll-y 0) max-scroll)
            htext  (if overflow?
                     "Wheel to scroll description  |  Select more or Enter to run"
                     "Select more or Enter to run")
            title-ops (mapv (fn [i l]
                              {:text l :type :text
                               :from 0 :to (count l)
                               :x list-padding-x :y (+ hdr-y (* i tlh))
                               :size title-size
                               :r 0.85 :g 0.85 :b 0.88 :a (:a typo-title)})
                            (range) tlines)
            static-ops [{:text mline :type :comment
                         :from 0 :to (count mline)
                         :x list-padding-x :y meta-y
                         :size meta-size
                         :r 0.55 :g 0.55 :b 0.60 :a (:a typo-caption)}
                        {:text "Description" :type :keyword
                         :from 0 :to 11
                         :x list-padding-x :y section-y
                         :size hint-size
                         :r 0.58 :g 0.68 :b 0.90 :a 0.95}
                        {:text htext :type :comment
                         :from 0 :to (count htext)
                         :x list-padding-x :y footer-y
                         :size hint-size
                         :r 0.40 :g 0.40 :b 0.45 :a 0.5}]
            all-ops  (into title-ops static-ops)]
        [(ui-card :detail-card
           {:x card-x :y card-y :w card-w :h card-h}
           :shadow :md
           :children [(ui-divider :detail-sep
                        {:x (:md (:spacing dt)) :y divider-y
                         :w (- card-w (* 2 (:md (:spacing dt)))) :h 1})
                      (rt-node :detail-md-clip :detail-md-clip
                        {:x list-padding-x :y desc-y :w content-w :h desc-max-h}
                        :clip? true
                        :children [(rt-node :detail-md-stack :detail-md-stack
                                     {:x 0 :y (- clamped-scroll) :w content-w :h (max desc-max-h desc-stack-h)}
                                     :layout {:direction :column :gap desc-gap}
                                     :children desc-nodes)])])
         (rt-node :detail-content :text-block
           {:x card-x :y card-y :w card-w :h card-h}
           :text all-ops)])

      ;; Multi-selection — execution stack (ordered batch view)
      :else
      (let [n     (count selected)
            hdr   "EXECUTION ORDER"
            badge (str "[" n "]")
            row-h 36
            stack-top 52
            hint-y (+ stack-top (* n row-h) 20)
            htxt  "Enter: run | Shift+\u2191\u2193: reorder | Del: remove"
            ;; Stack item text ops — numbered list with focus highlight
            active-idx (or active-lane-idx 0)
            stack-ops
            (into
              ;; Header
              [{:text hdr :type :macro
                :from 0 :to (count hdr)
                :x list-padding-x :y 28
                :size (:size typo-caption)
                :r 0.55 :g 0.60 :b 0.70 :a 1.0}
               {:text badge :type :comment
                :from 0 :to (count badge)
                :x (+ list-padding-x (* (count hdr) char-advance) 8) :y 28
                :size (:size typo-caption)
                :r 0.40 :g 0.55 :b 0.80 :a 1.0}
               ;; Hint footer
               {:text htxt :type :comment
                :from 0 :to (count htxt)
                :x list-padding-x :y hint-y
                :size (:size typo-caption)
                :r 0.40 :g 0.40 :b 0.45 :a 0.5}]
              ;; Stack items
              (mapcat
                (fn [i si]
                  (let [tkt   (nth tickets si nil)
                        t     (or (:title tkt) "Untitled")
                        tid   (or (:id tkt) "?")
                        label (str (inc i) ". " tid " — " t)
                        tr    (if (> (count label) detail-max-chars)
                                (str (subs label 0 (- detail-max-chars 2)) "..")
                                label)
                        y     (+ stack-top (* i row-h))
                        prio  (or (:priority tkt) 0)
                        pc    (get priority-colors prio {:r 0.55 :g 0.55 :b 0.60 :a 0.8})]
                    [{:text tr :type :text
                      :from 0 :to (count tr)
                      :x (+ list-padding-x 12) :y (+ y 14)
                      :size (:size typo-body)
                      :r 0.80 :g 0.82 :b 0.88 :a (:a typo-body)}]))
                (range) selected))
            ;; Stack item background rects with focus highlight on active lane
            stack-rects
            (mapv (fn [i si]
                    (let [y (+ stack-top (* i row-h))
                          focused? (= i active-idx)
                          prio (or (:priority (nth tickets si nil)) 0)
                          pc   (get priority-colors prio {:r 0.55 :g 0.55 :b 0.60 :a 0.8})
                          bg   (if focused?
                                 {:r 0.15 :g 0.25 :b 0.40 :a 0.8}
                                 {:r 0.16 :g 0.18 :b 0.22 :a 0.6})]
                      (rt-node (keyword (str "stack-item-" i)) :stack-item
                        {:x 8 :y y :w (- right-w 16) :h (- row-h 4)}
                        :style bg
                        :data {:lane-idx i :ticket-idx si}
                        :children [(rt-node (keyword (str "prio-dot-" i)) :decoration
                                    {:x 4 :y 10 :w 6 :h 6}
                                    :style {:r (:r pc) :g (:g pc) :b (:b pc) :a (:a pc)})])))
                  (range) selected)]
        (into stack-rects
              [(rt-node :stack-text :text-block
                 {:x 0 :y 0 :w right-w :h viewport-h}
                 :text stack-ops)])))))

(defn build-intake-tree
  "Build the Screen 1 intake scene graph.  Returns an rt-node tree.
   Walk with tree->rects for GPU rects, tree->text-ops for text.
   All fixed elements (backgrounds, header, right pane) compensate for
   scroll-y so the global camera can scroll content items.
   drag-state: {:phase :idle|:pending|:dragging, :node ..., :current {:x :y}}"
  [flow-state viewport-w viewport-h scroll-y detail-scroll-y hovered-row-idx collapsed-groups
   font-size char-advance drag-state]
  (let [tickets       (:tickets flow-state)
        selected      (:selected flow-state)
        selected-set  (set selected)
        grouped       (group-tickets-by-status tickets)
        left-w        (int (* viewport-w list-left-pane-pct))
        right-x0      (+ left-w list-divider-w)
        right-w       (- viewport-w left-w list-divider-w)
        sy            scroll-y
        list-font     (max font-size (:sm (:font-sizes dt)))
        group-font    (max (- font-size 1) (:sm (:font-sizes dt)))
        meta-font     (max (- font-size 2) (:xs (:font-sizes dt)))

        ;; === DRAG STATE ===
        dragging?     (drag-active? (or drag-state {:phase :idle}))
        dragged-idx   (when dragging? (:idx (:data (:node drag-state))))
        drag-cur      (when dragging? (:current drag-state))
        ;; Is cursor over right pane? (drop zone highlight)
        drag-over-right? (and dragging? drag-cur (>= (:x drag-cur) left-w))

        ;; === FIXED CHROME (scroll-compensated, using design tokens) ===
        left-bg    (rt-node :left-bg :bg
                     {:x 0 :y sy :w left-w :h viewport-h}
                     :style {:bg [0.0 0.0 0.0 1.0]})
        right-bg   (rt-node :right-bg :bg
                     {:x right-x0 :y sy :w right-w :h viewport-h}
                     :style {:bg (if drag-over-right?
                                   (:accent-muted (:colors dt))
                                   [0.0 0.0 0.0 1.0])})
        header-str (str "DISCOURSE-GRAPH  " (count tickets) " active")
        divider    (rt-node :divider :chrome
                     {:x left-w :y sy :w list-divider-w :h viewport-h}
                     :style {:bg (:border (:colors dt))})

        ;; === LEFT PANEL (composable components + layout engine) ===
        group-nodes
        (vec (map-indexed
               (fn [gi {:keys [status icon tickets] grp-count :count}]
                 (let [collapsed? (contains? collapsed-groups status)
                       ic (get status-icon-colors status {:r 0.6 :g 0.6 :b 0.6 :a 0.8})
                       item-nodes
                       (mapv (fn [{:keys [idx ticket]}]
                               (let [sel?   (contains? selected-set idx)
                                     hov?   (= idx hovered-row-idx)
                                     ghost? (= idx dragged-idx)
                                     prio   (or (:priority ticket) 0)
                                     leading (ui-checkbox (keyword (str "cb-" idx))
                                               :checked? sel?)
                                     trailing (when (<= 1 prio 2)
                                                (ui-priority-dot (keyword (str "pd-" idx))
                                                  prio :parent-w left-w))]
                                 (ui-list-item (keyword (str "t-" idx)) left-w
                                   :title (or (:title ticket) "Untitled")
                                   :leading leading
                                   :trailing trailing
                                   :selected? sel?
                                   :hovered? hov?
                                   :ghost? ghost?
                                   :data {:idx idx}
                                   :font-size list-font)))
                             tickets)]
                   (ui-panel-group (keyword (str "grp-" status)) left-w
                     :label (str status "  " grp-count)
                     :status status
                     :icon icon
                     :icon-color ic
                     :font-size group-font
                     :collapsed? collapsed?
                     :first-group? (zero? gi)
                     :items item-nodes)))
               grouped))

        ;; Footer text
        sel-count  (count selected)
        footer-txt (cond
                     (zero? (count tickets)) "/bootstrap to load"
                     (pos? sel-count)         (str sel-count " selected")
                     :else                    "Click to select")
        footer-hint (when (pos? sel-count)
                      "Enter to run | \u2191\u2193 reorder")
        footer-fg  (:fg-muted (:colors dt))
        footer-acc (:fg-subtle (:colors dt))
        content-h  (- viewport-h list-padding-top list-footer-h)

        left-panel
        (ui-panel :left-panel {:x 0 :y sy :w left-w :h viewport-h}
          :style {:bg [0.0 0.0 0.0 1.0]}
          :children
          [(ui-panel-header :header left-w list-padding-top
             :style {:bg [0.0 0.0 0.0 1.0]}
             :text [{:text header-str :type :macro
                     :from 0 :to (count header-str)
                     :x list-padding-x
                     :y (+ (/ list-padding-top 2) (/ (:size typo-subtitle) 2.5))
                     :size (:size typo-subtitle)
                     :r 0.75 :g 0.80 :b 0.95 :a (:a typo-subtitle)}])
           (ui-panel-content :linear-panel left-w content-h
             :children group-nodes)
           (ui-panel-footer :footer left-w list-footer-h
             :style {:bg [0.0 0.0 0.0 1.0]}
             :text (cond-> [{:text footer-txt :type :comment
                             :from 0 :to (count footer-txt)
                             :x list-padding-x :y 24
                             :size (:xs (:font-sizes dt))
                             :r (nth footer-fg 0) :g (nth footer-fg 1)
                             :b (nth footer-fg 2) :a (nth footer-fg 3)}]
                     footer-hint
                     (conj {:text footer-hint :type :comment
                            :from 0 :to (count footer-hint)
                            :x (+ list-padding-x
                                   (* (count footer-txt) (* (:xs (:font-sizes dt)) 0.56))
                                   12)
                            :y 24
                            :size (:xs (:font-sizes dt))
                            :r (nth footer-acc 0) :g (nth footer-acc 1)
                            :b (nth footer-acc 2) :a (nth footer-acc 3)})))])

        ;; === RIGHT DETAIL PANE (delegated to build-right-detail) ===
        active-lane-idx (or (:active-lane-idx flow-state) 0)
        right-children (build-right-detail tickets selected right-w viewport-h
                                           font-size char-advance active-lane-idx detail-scroll-y)
        right-detail (rt-node :right-detail :panel
                       {:x right-x0 :y sy :w right-w :h viewport-h}
                       :children (vec right-children))

        ;; === DROP ZONE BORDER (visible when dragging over right pane) ===
        drop-accent (let [a (:accent (:colors dt))] (assoc a 3 0.7))
        drop-border (when drag-over-right?
                      [(rt-node :drop-top :chrome
                         {:x right-x0 :y sy :w right-w :h 2}
                         :style {:bg drop-accent})
                       (rt-node :drop-bottom :chrome
                         {:x right-x0 :y (+ sy viewport-h -2) :w right-w :h 2}
                         :style {:bg drop-accent})
                       (rt-node :drop-left :chrome
                         {:x right-x0 :y sy :w 2 :h viewport-h}
                         :style {:bg drop-accent})
                       (rt-node :drop-right :chrome
                         {:x (+ right-x0 right-w -2) :y sy :w 2 :h viewport-h}
                         :style {:bg drop-accent})])

        ;; === FLOATING TICKET (follows cursor during drag, renders on top) ===
        float-node
        (when (and dragging? dragged-idx drag-cur)
          (let [tkt (nth tickets dragged-idx nil)]
            (when tkt
              (let [ftitle (or (:title tkt) "Untitled")
                    ftrunc (if (> (count ftitle) 40)
                             (str (subs ftitle 0 38) "..")
                             ftitle)
                    fprio (or (:priority tkt) 0)
                    ;; Position in world space: cursor screen + scroll offset
                    fx (- (:x drag-cur) 20)
                    fy (+ (- (:y drag-cur) 12) sy)
                    fw (min 300 left-w)]
                (rt-node :float-ticket :ticket-row
                  {:x fx :y fy :w fw :h list-row-h}
                  :style {:bg [0.18 0.22 0.35 0.95]
                          :radius (:md (:radii dt))
                          :shadow {:blur 12 :offset-y 4 :color [0 0 0 0.4]}}
                  :text [{:text ftrunc :type :text
                          :from 0 :to (count ftrunc)
                          :x 10 :y 17 :size list-font
                          :r 0.90 :g 0.90 :b 0.95 :a 1.0}
                         {:text (str "P" fprio) :type :comment
                          :from 0 :to (+ 1 (count (str fprio)))
                          :x (- fw 36) :y 17 :size meta-font
                          :r (if (<= fprio 2) 0.95 0.55)
                          :g (if (<= fprio 2) 0.55 0.55)
                          :b (if (<= fprio 2) 0.30 0.60)
                          :a 0.9}])))))

        base-children [left-bg right-bg left-panel divider right-detail]]

    ;; === ROOT ===
    (rt-node :intake-root :root
      {:x 0 :y 0 :w viewport-w :h 100000}
      :children (cond-> base-children
                  drop-border (into drop-border)
                  float-node  (conj float-node)))))

;; === Thin wrappers — drop-in replacements for the old compute fns ===

(defn offset-rects
  "Shift all rect :x by dx. Used to push content right when sidebar visible."
  [rects dx]
  (if (zero? dx)
    rects
    (mapv #(update % :x + dx) rects)))

(defn offset-shadows
  "Shift all shadow :x by dx."
  [shadows dx]
  (if (zero? dx)
    shadows
    (mapv #(update % :x + dx) shadows)))

(defn offset-text-ops
  "Shift text ops :x by dx. Handles both nested [[op]] and flat [op] formats."
  [ops dx]
  (if (zero? dx)
    ops
    (mapv (fn [op]
            (if (vector? op)
              (mapv #(update % :x + dx) op)
              (update op :x + dx)))
          ops)))

(defn compute-ticket-list-rects
  "GPU rects + shadows for Screen 1 intake (delegates to rect tree).
   Returns {:rects [...] :shadows [...]} for flow-canvas mode."
  [flow-state viewport-w viewport-h scroll-y detail-scroll-y hovered-row-idx collapsed-groups drag-state
   font-size char-advance]
  (let [tree (resolve-layout
               (build-intake-tree flow-state viewport-w viewport-h scroll-y detail-scroll-y
                                  hovered-row-idx collapsed-groups font-size char-advance drag-state))]
    {:rects   (tree->rects tree)
     :shadows (tree->shadows tree)}))

(defn compute-ticket-list-text-ops
  "Text ops for Screen 1 intake (delegates to rect tree)."
  [flow-state viewport-w viewport-h font-size char-advance scroll-y detail-scroll-y
   hovered-row-idx collapsed-groups drag-state]
  (tree->text-ops
    (resolve-layout
      (build-intake-tree flow-state viewport-w viewport-h scroll-y detail-scroll-y
                         hovered-row-idx collapsed-groups font-size char-advance drag-state))))

;; ============================================================================
;; PROMPT TEMPLATES (V0 Flow Actions)
;; ============================================================================

(defn flow-prompt
  "Compose a prompt + optional system instruction for a flow action.
   Returns {:prompt \"...\" :system-instruction nil}."
  [action flow-state]
  (case action
    :bootstrap
    {:prompt (str "List all active Linear tickets for the discourse-graph project. "
                  "Output ONLY a JSON array, no other text. Each object needs: "
                  "id, title, status, assignee, priority, description. "
                  "IMPORTANT: Copy the full description text verbatim from Linear — do NOT summarize or truncate it.")
     :system-instruction nil}

    :run-sequential
    (let [tickets (mapv #(nth (:tickets flow-state) %) (:selected flow-state))
          ticket-list (str/join "\n" (map-indexed
                                       (fn [i t]
                                         (str (inc i) ". " (:id t) " — " (:title t)
                                              " [" (:status t) ", P" (:priority t) "]"))
                                       tickets))]
      {:prompt (str "Execute the following tickets SEQUENTIALLY. "
                    "Each ticket's output should feed into the next ticket's context.\n\n"
                    ticket-list "\n\n"
                    "For each ticket: create a worktree, implement the changes, "
                    "run tests, and report the result before moving to the next.")
       :system-instruction nil})

    :run-parallel
    (let [tickets (mapv #(nth (:tickets flow-state) %) (:selected flow-state))
          ticket-list (str/join "\n" (map-indexed
                                       (fn [i t]
                                         (str (inc i) ". " (:id t) " — " (:title t)
                                              " [" (:status t) ", P" (:priority t) "]"))
                                       tickets))]
      {:prompt (str "Execute the following tickets IN PARALLEL (independent worktrees). "
                    "Each ticket is independent — do not chain context between them.\n\n"
                    ticket-list "\n\n"
                    "For each ticket: create a separate worktree from main, "
                    "implement changes, run tests, and report results.")
       :system-instruction nil})

    :rework
    {:prompt (str "Rework the previous implementation based on this feedback:\n\n"
                  (or (:rework-comment flow-state) "Please fix the issues found in review.") "\n\n"
                  "Apply fixes in the existing worktree(s) and re-run tests.")
     :system-instruction nil}

    :finalize
    {:prompt (str "Finalize the current batch of tickets. For each completed ticket:\n"
                  "1. Generate a PR description summarizing the changes\n"
                  "2. List any remaining TODOs or known issues\n"
                  "3. Confirm test status\n\n"
                  "Return a summary of all finalized work.")
     :system-instruction nil}

    ;; Fallback — shouldn't happen if callers validate
    {:prompt (str "Unknown flow action: " action)
     :system-instruction nil}))

;; ============================================================================
;; RUN / REVIEW TREE BUILDER
;; ============================================================================

(def lane-status-colors
  {:queued  {:r 0.50 :g 0.50 :b 0.55 :a 0.8}
   :running {:r 0.40 :g 0.70 :b 1.00 :a 1.0}
   :done    {:r 0.40 :g 0.85 :b 0.45 :a 1.0}
   :failed  {:r 0.95 :g 0.40 :b 0.40 :a 1.0}})

(def lane-status-labels
  {:queued "QUEUED" :running "RUNNING" :done "DONE" :failed "FAILED"})

(defn build-run-tree
  "Build the run/review scene graph. Same master-detail layout as intake.
   Left pane: batch map with lane statuses. Right pane: trail stream.
   mode: :run (live streaming) or :review (static, completed).
   agent-output: the current agent output map with :trail, :status, etc.
   shimmer-alpha: pulse for pending trail cards.
   collapsed: set of collapsed trail block ids.
   trail-scroll-y: scroll offset for the right pane trail content."
  [flow-state viewport-w viewport-h scroll-y agent-output
   font-size char-advance shimmer-alpha collapsed trail-scroll-y]
  (let [tickets   (:tickets flow-state)
        selected  (:selected flow-state)
        mode      (:node flow-state)
        sy        scroll-y ;; scroll compensation for fixed chrome
        left-w    (int (* viewport-w list-left-pane-pct))
        right-x0  (+ left-w list-divider-w)
        right-w   (- viewport-w left-w list-divider-w)
        colors    (:colors dt)
        pad       (:lg (:spacing dt))
        line-h    (+ font-size 4)
        row-h     36
        header-h  40

        ;; Determine per-lane status (V0: all lanes share one run status)
        a-status  (:status agent-output)
        global-lane-status (cond
                             (= a-status :running)  :running
                             (= a-status :complete)  :done
                             (= a-status :failed)    :failed
                             :else                    :queued)
        banner    (case mode
                    :run    "RUNNING"
                    :review "REVIEW"
                    :rework "REWORKING"
                    "BATCH")
        banner-c  (case mode
                    :run     {:r 0.40 :g 0.70 :b 1.00 :a 1.0}
                    :review  {:r 0.40 :g 0.85 :b 0.45 :a 1.0}
                    :rework  {:r 0.95 :g 0.70 :b 0.25 :a 1.0}
                    {:r 0.70 :g 0.70 :b 0.75 :a 1.0})

        ;; === LEFT PANE: batch map with lane statuses ===
        lane-nodes
        (mapv (fn [i si]
                (let [tkt (nth tickets si nil)
                      t   (or (:title tkt) "Untitled")
                      tid (or (:id tkt) "?")
                      sc  (get lane-status-colors global-lane-status)
                      sl  (get lane-status-labels global-lane-status "?")]
                  (rt-node (keyword (str "lane-" i)) :lane-item
                    {:x 8 :y (+ header-h 8 (* i row-h))
                     :w (- left-w 16) :h (- row-h 4)}
                    :style {:r 0.14 :g 0.15 :b 0.18 :a 0.8}
                    :text [{:text (str (inc i) ". " tid) :type :keyword
                            :from 0 :to (+ 3 (count tid))
                            :x 8 :y 22 :size (:size typo-body)
                            :r 0.75 :g 0.78 :b 0.85 :a 1.0}
                           {:text sl :type :comment
                            :from 0 :to (count sl)
                            :x (- left-w 80) :y 22 :size (:size typo-caption)
                            :r (:r sc) :g (:g sc) :b (:b sc) :a (:a sc)}])))
              (range) selected)

        footer-txt (case mode
                     :review "/rework <feedback> | /finalize"
                     :rework "Reworking..."
                     "")

        left-panel
        (rt-node :run-left :panel
          {:x 0 :y sy :w left-w :h viewport-h}
          :style {:bg [0.0 0.0 0.0 1.0]}
          :text [{:text banner :type :macro
                  :from 0 :to (count banner)
                  :x list-padding-x :y 28
                  :size (:size typo-subtitle)
                  :r (:r banner-c) :g (:g banner-c) :b (:b banner-c) :a (:a banner-c)}
                 {:text (str (count selected) " tickets")
                  :type :comment
                  :from 0 :to (+ (count (str (count selected))) 8)
                  :x (+ list-padding-x (* (count banner) char-advance) 12) :y 28
                  :size (:size typo-caption)
                  :r 0.50 :g 0.50 :b 0.55 :a 0.8}
                 {:text footer-txt :type :comment
                  :from 0 :to (count footer-txt)
                  :x list-padding-x :y (- viewport-h 16)
                  :size (:size typo-caption)
                  :r 0.45 :g 0.45 :b 0.50 :a 0.6}]
          :children lane-nodes)

        ;; === RIGHT PANE: trail stream ===
        trail (:trail agent-output)
        chat-w right-w

        trail-children
        (if (seq trail)
          (let [block-nodes (trail->chat-nodes trail chat-w font-size char-advance
                                               (or shimmer-alpha 0.4) (or collapsed #{}))]
            block-nodes)
          [(build-empty-state :run-empty right-w (- viewport-h header-h)
             {:icon (if (= mode :review) "OK" "...")
              :headline (if (= mode :review) "Run complete" "Waiting for output")
              :description (if (= mode :review)
                             "Review the trail below. /rework or /finalize."
                             "Agent is processing your batch...")})])

        trail-content-h (reduce + 0 (map #(get-in % [:bounds :h] 0) trail-children))
        trail-sy (or trail-scroll-y 0)
        max-trail-scroll (max 0 (- trail-content-h viewport-h))
        trail-scroll-offset (min trail-sy max-trail-scroll)

        right-panel
        (rt-node :run-right :panel
          {:x right-x0 :y sy :w right-w :h viewport-h}
          :style {:bg [0.0 0.0 0.0 1.0]}
          :children
          [(rt-node :run-right-body :panel-content
             {:x 0 :y 0 :w right-w :h viewport-h}
             :clip? true
             :children
             [(rt-node :run-trail-scroll :scroll-container
                {:x 0 :y (- 4 trail-scroll-offset) :w right-w :h (+ trail-content-h 8)}
                :layout {:direction :column :gap 4 :padding [0 0 0 0]}
                :children trail-children)])])

        divider (rt-node :run-divider :chrome
                  {:x left-w :y sy :w list-divider-w :h viewport-h}
                  :style {:bg (:border colors)})]

    (rt-node :run-root :scene
      {:x 0 :y 0 :w viewport-w :h viewport-h}
      :children [left-panel divider right-panel])))

(defn compute-run-rects
  "GPU rects + shadows for run/review mode."
  [flow-state viewport-w viewport-h scroll-y agent-output
   font-size char-advance shimmer-alpha collapsed trail-scroll-y]
  (let [tree (resolve-layout
               (build-run-tree flow-state viewport-w viewport-h scroll-y
                               agent-output font-size char-advance
                               shimmer-alpha collapsed trail-scroll-y))]
    {:rects   (tree->rects tree)
     :shadows (tree->shadows tree)}))

(defn compute-run-text-ops
  "Text ops for run/review mode."
  [flow-state viewport-w viewport-h scroll-y agent-output
   font-size char-advance shimmer-alpha collapsed trail-scroll-y]
  (tree->text-ops
    (resolve-layout
      (build-run-tree flow-state viewport-w viewport-h scroll-y
                      agent-output font-size char-advance
                      shimmer-alpha collapsed trail-scroll-y))))
