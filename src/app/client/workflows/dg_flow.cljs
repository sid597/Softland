(ns app.client.workflows.dg-flow
  "DG workflow: flow state machine, Screen 1 intake tree, ticket layout."
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
                     ui-scrollbar build-empty-state]]))

;; ============================================================================
;; FLOW STATE MACHINE (V0 — "Prompts as API Calls")
;; ============================================================================
;; Encodes the state graph from commission-consensus.md Section 4.
;; Pure functions — no atoms, no side effects.

(def flow-transitions
  "Directed graph of valid state transitions.
   Keys are from-states, values are sets of reachable to-states.
   NOTE: :idle is a pre-graph state (not in Section 4's locked graph).
   It exists only as the initial state before the first bootstrap.
   Once bootstrapping begins, all transitions follow the locked graph."
  {:idle            #{:bootstrapping}
   :bootstrapping   #{:intake :bootstrapping}         ;; retry on failure
   :intake          #{:arrange :bootstrapping}       ;; re-bootstrap from intake
   :arrange         #{:run}
   :run             #{:review :intake}                 ;; back-edge: scope change
   :review          #{:rework :finalize :arrange}      ;; back-edge: re-arrange
   :rework          #{:review :arrange}                ;; back-edge: split/reorder
   :finalize        #{:intake}})

(defn valid-transition?
  "Check if moving from `from` to `to` is allowed.
   Human override: any state can jump to :intake or :arrange."
  [from to]
  (or (contains? (get flow-transitions from) to)
      (contains? #{:intake :arrange} to)))

(defn initial-flow-state
  "Fresh flow state for a new session.
   Starts in :idle — ticket list only appears after /bootstrap (/dg)."
  []
  {:node :idle
   :tickets []
   :selected []
   :arrangement nil
   :session-id nil
   :history []})

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
  "True when the flow state machine is in a node that shows the master-detail list view
   instead of the code editor. Currently :intake and :arrange."
  [flow-state]
  (contains? #{:intake :arrange} (:node flow-state)))

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

    (str/starts-with? trimmed "/arrange ")
    (let [mode (-> trimmed (subs (count "/arrange ")) str/trim str/lower-case keyword)]
      (if (contains? #{:sequential :parallel} mode)
        {:kind :flow-arrange :mode mode}
        {:kind :error :message "Usage: /arrange sequential|parallel"}))

    (= trimmed "/arrange")
    {:kind :error :message "Usage: /arrange sequential|parallel"}

    (= trimmed "/run-flow")
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
  [parsed {:keys [!flow-state !scroll-y show-flow-info! fire-flow-run!]}]
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
            (show-flow-info! "Fetching tickets from Linear...")
            (-> (js/fetch "/api/linear/issues?team=DIS")
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
            (reset! !scroll-y 0)
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
                (swap! !flow-state assoc :selected (vec indices))
                (show-flow-info!
                 (str "Selected " (count indices) " ticket(s):\n"
                      (str/join "\n" (map (fn [i]
                                             (let [t (nth tickets i)]
                                               (str "  " (inc i) ". " (:id t) " — " (:title t))))
                                           indices))
                      "\n\nUse /arrange sequential|parallel to set execution mode.")))))))
      true)

    :flow-arrange
    (do
      (let [flow @!flow-state
            mode (:mode parsed)]
        (if (empty? (:selected flow))
          (show-flow-info! "No tickets selected. Use /select first.")
          (let [next (transition-flow-state flow :arrange {:arrangement mode})]
            (if next
              (do
                (reset! !flow-state next)
                (show-flow-info!
                 (str "Arrangement set to: " (name mode)
                      "\n" (count (:selected flow)) " ticket(s) ready."
                      "\n\nUse /run-flow to start execution.")))
              (show-flow-info! (str "Cannot arrange from state: " (name (:node flow))))))))
      true)

    :flow-run
    (do
      (let [flow @!flow-state
            next (transition-flow-state flow :run)]
        (if next
          (let [mode (or (:arrangement flow) :sequential)
                action (if (= mode :parallel) :run-parallel :run-sequential)]
            (reset! !flow-state next)
            (fire-flow-run! action
                            :on-done (fn []
                                       (swap! !flow-state assoc :node :review)
                                       (show-flow-info!
                                        (str "Run complete. Now in review state.\n\n"
                                             "Use /rework <comment> to request changes,\n"
                                             "or /finalize to wrap up.")))))
          (show-flow-info!
           (str "Cannot run from state: " (name (:node flow))
                "\nExpected :arrange. Current: " (name (:node flow))))))
      true)

    :flow-review
    (do
      (let [flow @!flow-state]
        (show-flow-info!
         (str "Current state: " (name (:node flow))
              (when (= (:node flow) :review)
                "\n\nOptions:\n  /rework <comment> — request changes\n  /finalize — wrap up batch"))))
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
                                              :arrangement nil)
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
      (let [flow @!flow-state]
        (show-flow-info!
         (str "=== Flow State ===\n"
              "Node: " (name (:node flow)) "\n"
              "Session: " (or (:session-id flow) "none") "\n"
              "Tickets: " (count (:tickets flow)) "\n"
              "Selected: " (if (seq (:selected flow))
                             (str/join ", " (map inc (:selected flow)))
                             "none") "\n"
              "Arrangement: " (or (some-> (:arrangement flow) name) "none") "\n"
              "History: [" (str/join " -> " (map name (:history flow))) "]")))
      true)

    :flow-reset
    (do
      (reset! !flow-state (initial-flow-state))
      (reset! !scroll-y 0)
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
  "Build the right pane content for Screen 1 detail view.
   Returns a vector of rt-node children for the right panel.
   4 states: empty tickets, no selection, single selection, multi selection."
  [tickets selected right-w viewport-h font-size char-advance]
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
            plabel (case tprio 1 "Urgent" 2 "High" 3 "Medium" 4 "Low" "None")
            mline  (str tstatus "  |  " plabel "  |  " tassn)
            tlines (wrap-line ttitle detail-max-chars)
            tlh    20
            hdr-y  52
            meta-y (+ hdr-y (* (count tlines) tlh) 4)
            desc-y (+ meta-y 24)
            dlines (if (seq tdesc) (wrap-line tdesc detail-max-chars) [])
            dlh    18
            hint-y (+ desc-y (max dlh (* (count dlines) dlh)) 16)
            htext  "/arrange sequential|parallel to proceed"
            title-ops (mapv (fn [i l]
                              {:text l :type :text
                               :from 0 :to (count l)
                               :x list-padding-x :y (+ hdr-y (* i tlh))
                               :size (:size typo-subtitle)
                               :r 0.85 :g 0.85 :b 0.88 :a (:a typo-title)})
                            (range) tlines)
            desc-ops  (mapv (fn [i l]
                              {:text l :type :text
                               :from 0 :to (count l)
                               :x list-padding-x :y (+ desc-y (* i dlh))
                               :size (:size typo-body)
                               :r 0.70 :g 0.70 :b 0.73 :a (:a typo-body)})
                            (range) dlines)
            all-ops  (into (into title-ops
                             [{:text mline :type :comment
                               :from 0 :to (count mline)
                               :x list-padding-x :y meta-y
                               :size (:size typo-body)
                               :r 0.55 :g 0.55 :b 0.60 :a (:a typo-caption)}
                              {:text htext :type :comment
                               :from 0 :to (count htext)
                               :x list-padding-x :y hint-y
                               :size (:size typo-caption)
                               :r 0.40 :g 0.40 :b 0.45 :a 0.5}])
                           desc-ops)]
        [(ui-card :detail-card
           {:x (:lg (:spacing dt)) :y 36
            :w (- right-w (* 2 (:lg (:spacing dt)))) :h (+ hint-y 24)}
           :shadow :md
           :children [(ui-divider :detail-sep
                        {:x (:md (:spacing dt)) :y (- meta-y 34)
                         :w (- right-w (* 2 (:lg (:spacing dt))) (* 2 (:md (:spacing dt)))) :h 1})])
         (rt-node :detail-content :text-block
           {:x 0 :y 0 :w right-w :h viewport-h}
           :text all-ops)])

      ;; Multi-selection — batch summary
      :else
      (let [n   (count selected)
            ctxt (str n " tickets selected")
            sops (mapv (fn [i si]
                         (let [tkt (nth tickets si nil)
                               t   (or (:title tkt) "Untitled")
                               tr  (if (> (count t) detail-max-chars)
                                     (str (subs t 0 (- detail-max-chars 2)) "..")
                                     t)]
                           {:text tr :type :text
                            :from 0 :to (count tr)
                            :x list-padding-x :y (+ 88 (* i 20))
                            :size (:size typo-body)
                            :r 0.70 :g 0.75 :b 0.80 :a (:a typo-body)}))
                       (range) selected)
            hy   (+ 88 (* n 20) 16)
            htxt "/arrange sequential|parallel to proceed"]
        [(rt-node :multi-detail :text-block
           {:x 0 :y 0 :w right-w :h viewport-h}
           :text (into [{:text ctxt :type :macro
                         :from 0 :to (count ctxt)
                         :x list-padding-x :y 52
                         :size (:size typo-subtitle)
                         :r 0.75 :g 0.80 :b 0.95 :a (:a typo-title)}
                        {:text htxt :type :comment
                         :from 0 :to (count htxt)
                         :x list-padding-x :y hy
                         :size (:size typo-caption)
                         :r 0.40 :g 0.40 :b 0.45 :a 0.5}]
                       sops))]))))

(defn build-intake-tree
  "Build the Screen 1 intake scene graph.  Returns an rt-node tree.
   Walk with tree->rects for GPU rects, tree->text-ops for text.
   All fixed elements (backgrounds, header, right pane) compensate for
   scroll-y so the global camera can scroll content items.
   drag-state: {:phase :idle|:pending|:dragging, :node ..., :current {:x :y}}"
  [flow-state viewport-w viewport-h scroll-y hovered-row-idx collapsed-groups
   font-size char-advance drag-state]
  (let [tickets       (:tickets flow-state)
        selected      (:selected flow-state)
        selected-set  (set selected)
        grouped       (group-tickets-by-status tickets)
        left-w        (int (* viewport-w list-left-pane-pct))
        right-x0      (+ left-w list-divider-w)
        right-w       (- viewport-w left-w list-divider-w)
        sy            scroll-y
        list-font     (- font-size 2)
        meta-font     (- font-size 3)

        ;; === DRAG STATE ===
        dragging?     (drag-active? (or drag-state {:phase :idle}))
        dragged-idx   (when dragging? (:idx (:data (:node drag-state))))
        drag-cur      (when dragging? (:current drag-state))
        ;; Is cursor over right pane? (drop zone highlight)
        drag-over-right? (and dragging? drag-cur (>= (:x drag-cur) left-w))

        ;; === FIXED CHROME (scroll-compensated, using design tokens) ===
        left-bg    (rt-node :left-bg :bg
                     {:x 0 :y sy :w left-w :h viewport-h}
                     :style {:bg (:bg (:colors dt))})
        right-bg   (rt-node :right-bg :bg
                     {:x right-x0 :y sy :w right-w :h viewport-h}
                     :style {:bg (if drag-over-right?
                                   (:accent-muted (:colors dt))
                                   (:bg-subtle (:colors dt)))})
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
                      "/arrange to proceed")
        footer-fg  (:fg-muted (:colors dt))
        footer-acc (:fg-subtle (:colors dt))
        content-h  (- viewport-h list-padding-top list-footer-h)

        left-panel
        (ui-panel :left-panel {:x 0 :y sy :w left-w :h viewport-h}
          :children
          [(ui-panel-header :header left-w list-padding-top
             :text [{:text header-str :type :macro
                     :from 0 :to (count header-str)
                     :x list-padding-x
                     :y (+ (/ list-padding-top 2) (/ (:size typo-subtitle) 2.5))
                     :size (:size typo-subtitle)
                     :r 0.75 :g 0.80 :b 0.95 :a (:a typo-subtitle)}])
           (ui-panel-content :linear-panel left-w content-h
             :children group-nodes)
           (ui-panel-footer :footer left-w list-footer-h
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
        right-children (build-right-detail tickets selected right-w viewport-h
                                           font-size char-advance)
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
  [flow-state viewport-w viewport-h scroll-y hovered-row-idx collapsed-groups drag-state]
  (let [tree (resolve-layout
               (build-intake-tree flow-state viewport-w viewport-h scroll-y
                                  hovered-row-idx collapsed-groups 0 0 drag-state))]
    {:rects   (tree->rects tree)
     :shadows (tree->shadows tree)}))

(defn compute-ticket-list-text-ops
  "Text ops for Screen 1 intake (delegates to rect tree)."
  [flow-state viewport-w viewport-h font-size char-advance scroll-y
   hovered-row-idx collapsed-groups drag-state]
  (tree->text-ops
    (resolve-layout
      (build-intake-tree flow-state viewport-w viewport-h scroll-y
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
