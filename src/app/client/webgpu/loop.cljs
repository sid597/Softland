(ns app.client.webgpu.loop
  "Reactive-first editor loop following Electric/Missionary patterns.

   Architecture:
   - Layer 1: Primary Sources (6 atoms)
   - Layer 2: Event Flows (keyboard, mouse, wheel, resize, blink)
   - Layer 3: Derived Flows (line-lengths, fold-regions, tokenized, render-ops)
   - Layer 4: Focus-based Event Routing
   - Layer 5: Component Update Flows
   - Layer 6: GPU State Derived Flows
   - Layer 7: Terminal Render Consumer"
  (:require [clojure.string :as str]
            [cljs.reader :as reader]
            [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            [missionary.core :as m]
            [contrib.missionary-contrib :as mx]
            [app.client.webgpu.editor :as editor]
            [app.client.webgpu.text-input :as text-input]
            [app.client.webgpu.themes :as themes]))

;; ============================================================================
;; LAYER 1: PRIMARY SOURCES (The Only Atoms)
;; ============================================================================
;; These are created per-instance in start-loop! to support multiple editors

;; ============================================================================
;; LAYER 2: EVENT FLOWS (Produce Values, Don't Store)
;; ============================================================================

(defn >canvas-resize
  "Flow that emits viewport dimensions when the canvas element resizes.
   Uses ResizeObserver to detect size changes from flex layout, sidebar toggle, etc."
  [canvas-node]
  (->> (m/observe
         (fn [!]
           (let [emit! (fn []
                         (! {:width  (.-clientWidth canvas-node)
                             :height (.-clientHeight canvas-node)
                             :dpr    (or js/window.devicePixelRatio 1)}))]
             (if (exists? js/ResizeObserver)
               (let [obs (js/ResizeObserver. (fn [_entries] (emit!)))]
                 (.observe obs canvas-node)
                 (emit!)  ;; Emit initial value
                 #(.disconnect obs))
               ;; Fallback: window resize (won't catch sidebar toggle)
               (do (js/window.addEventListener "resize" emit!)
                   (emit!)
                   #(js/window.removeEventListener "resize" emit!))))))
       (m/relieve (fn [_old new] new))))

(defn >wheel [node]
  "Flow that emits wheel delta values"
  (->> (m/observe
         (fn [!]
           (let [handler (fn [e]
                           (.preventDefault e)
                           (! (.-deltaY e)))]
             (.addEventListener node "wheel" handler #js {:passive false})
             #(.removeEventListener node "wheel" handler))))
       (m/relieve +)))

(defn >mouse [node]
  "Flow that emits mouse events [:mousedown/:mouseup/:mousemove coords]"
  (m/observe
    (fn [!]
      (let [get-coords (fn [e]
                         (let [rect (.getBoundingClientRect node)]
                           {:x (- (.-clientX e) (.-left rect))
                            :y (- (.-clientY e) (.-top rect))}))
            down-h (fn [e] (! [:mousedown (get-coords e)]))
            up-h   (fn [e] (! [:mouseup (get-coords e)]))
            move-h (fn [e] (! [:mousemove (get-coords e)]))]
        (.addEventListener node "mousedown" down-h)
        (.addEventListener js/window "mouseup" up-h)
        (.addEventListener js/window "mousemove" move-h)
        (fn []
          (.removeEventListener node "mousedown" down-h)
          (.removeEventListener js/window "mouseup" up-h)
          (.removeEventListener js/window "mousemove" move-h))))))

(defn snap-to-dpr [v dpr]
  (let [scale (or dpr 1)]
    (/ (Math/round (* v scale)) scale)))

(defn maybe-snap [v dpr snap?]
  (if snap? (snap-to-dpr v dpr) v))

(defn manifest-defaults->settings [manifest-settings]
  (let [get-default (fn [k fallback]
                      (or (get-in manifest-settings [k :default]) fallback))]
    {:font-size (get-default :fontSize 19)
     :line-height (get-default :lineHeight 1.2)
     :px-range (get-default :pxRange 8)
     :sharpness (get-default :sharpness 0.0)
     :snap-to-pixel? (get-default :snapToPixel true)
     :show-diagnostics? (get-default :showDiagnostics false)
     :theme-id (get-default :theme :gruvbox-dark)}))

(defn compact-map [m]
  (into {} (filter (comp some? val) m)))

(defn font-defaults->settings [font]
  (let [defaults (:defaults font)]
    (when defaults
      (compact-map
        {:font-size (or (:fontSize defaults) (:font-size defaults))
         :line-height (or (:lineHeight defaults) (:line-height defaults))
         :px-range (or (:pxRange defaults) (:px-range defaults))
         :sharpness (or (:sharpness defaults) (:sharpness defaults))
         :snap-to-pixel? (or (:snapToPixel defaults) (:snap-to-pixel? defaults))
         :show-diagnostics? (or (:showDiagnostics defaults) (:show-diagnostics? defaults))}))))

(defn slider-specs [settings]
  [{:id :theme-id :label "Theme" :val (themes/theme-index (:theme-id settings)) :min 0 :max (dec (count themes/theme-list)) :discrete true}
   {:id :font-size :label "Font Size" :val (:font-size settings) :min 8 :max 40}
   {:id :line-height :label "Line Height" :val (:line-height settings) :min 1.0 :max 2.0}
   {:id :px-range :label "pxRange" :val (:px-range settings) :min 4 :max 12}
   {:id :sharpness :label "Sharpness" :val (:sharpness settings) :min -0.2 :max 0.2}
   {:id :snap-to-pixel? :label "Snap" :val (if (:snap-to-pixel? settings) 1 0) :min 0 :max 1}
   {:id :show-diagnostics? :label "Diagnostics" :val (if (:show-diagnostics? settings) 1 0) :min 0 :max 1}])

(defn parse-key-event
  "Parse DOM keyboard event into semantic event map"
  [e]
  (let [key (.-key e)
        ctrl? (or (.-ctrlKey e) (.-metaKey e))
        shift? (.-shiftKey e)]
    (cond
      ;; Global shortcuts (not affected by focus)
      (and ctrl? (= key "k")) {:type :toggle-command-panel :global? true}
      (and ctrl? (= key "g")) {:type :toggle-settings-panel :global? true}
      (and ctrl? (= key "b")) {:type :toggle-file-viewer :global? true}
      (and ctrl? (= key "s")) {:type :save :global? true}

      ;; Escape - context dependent but handled globally
      (= key "Escape") {:type :escape :global? true}

      ;; Editor-specific shortcuts
      (and ctrl? (= key "Enter")) {:type :eval}
      (and ctrl? (= key "z") (not shift?)) {:type :undo}
      (and ctrl? (= key "z") shift?) {:type :redo}
      (and ctrl? (= key "y")) {:type :redo}
      (and ctrl? (= key "c")) {:type :copy}
      (and ctrl? (= key "x")) {:type :cut}
      (and ctrl? (= key "v")) {:type :paste}

      ;; Word navigation
      (and ctrl? (= key "ArrowLeft")) {:type :word-left}
      (and ctrl? (= key "ArrowRight")) {:type :word-right}

      ;; Navigation keys
      (= key "ArrowLeft") {:type :left}
      (= key "ArrowRight") {:type :right}
      (= key "ArrowUp") {:type :up}
      (= key "ArrowDown") {:type :down}
      (= key "Home") {:type :home}
      (= key "End") {:type :end}

      ;; Editing keys
      (= key "Backspace") {:type :backspace}
      (= key "Delete") {:type :delete}
      (= key "Enter") {:type :enter}

      ;; Character input
      (and (= 1 (count key)) (not ctrl?) (not (.-altKey e)))
      {:type :char :char key}

      :else nil)))

(defn >keyboard [node]
  "Flow that emits parsed keyboard events"
  (->> (m/observe
         (fn [!]
           (let [handler (fn [e]
                           (when-let [event (parse-key-event e)]
                             (.preventDefault e)
                             (! event)))]
             (.addEventListener node "keydown" handler)
             #(.removeEventListener node "keydown" handler))))
       (m/relieve (fn [_ x] x))))

(defn make-raf-flow
  "Creates a fresh RAF flow - emits timestamps on each animation frame.
   IMPORTANT: Must be called fresh for each subscription, not shared!"
  []
  (m/observe
    (fn [!]
      (let [active? (volatile! true)
            callback (fn loop [t]
                       (when @active?
                         (! t)
                         (js/requestAnimationFrame loop)))]
        (js/requestAnimationFrame callback)
        #(vreset! active? false)))))

(defn make-blink-timer
  "Creates a fresh blink timer flow - emits true/false every 530ms.
   IMPORTANT: Must be called fresh for each subscription, not shared!"
  []
  (m/ap
    (loop []
      (m/amb true
             (do (m/? (m/sleep 530))
                 (m/amb false
                        (do (m/? (m/sleep 530))
                            (recur))))))))


;; ============================================================================
;; LAYER 3: DERIVED FLOWS (Pure Transformations)
;; ============================================================================

;; NOTE: <line-lengths, <fold-regions, <line-mapping were removed (dead code using banned m/ap+m/?< pattern).
;; Their functionality is now consolidated in <fold-state (line 633) which uses m/latest.

;; ============================================================================
;; LAYER 4: FOCUS-BASED EVENT ROUTING
;; ============================================================================
;;
;; IMPORTANT: Do NOT use m/ap with m/?< on m/watch here!
;; These flows filter discrete events - use m/eduction with deref instead.
;; This avoids cancellation when focus changes.

(defn <global-events
  "Flow of global events (not affected by focus)"
  [>keyboard]
  (->> >keyboard
       (m/eduction (filter :global?))))

(defn <editor-keys
  "Flow of keyboard events routed to editor (when focused).
   Uses deref instead of m/watch to avoid cancellation on focus change."
  [>keyboard !focus]
  (->> >keyboard
       (m/eduction (filter (fn [event]
                             (and (= @!focus :editor)
                                  (not (:global? event))))))))

(defn <cmd-panel-keys
  "Flow of keyboard events routed to command panel (when focused).
   Uses deref instead of m/watch to avoid cancellation on focus change."
  [>keyboard !focus]
  (->> >keyboard
       (m/eduction (filter (fn [event]
                             (and (= @!focus :command-panel)
                                  (not (:global? event))))))))

(defn <settings-panel-keys
  "Flow of keyboard events routed to settings panel (when focused).
   Uses deref instead of m/watch to avoid cancellation on focus change."
  [>keyboard !focus]
  (->> >keyboard
       (m/eduction (filter (fn [event]
                             (and (= @!focus :settings-panel)
                                  (not (:global? event))))))))


;; ============================================================================
;; LAYER 5: COMPONENT UPDATE FLOWS
;; ============================================================================

(defn editor-apply-event
  "Pure function: apply event to editor doc, returns new doc"
  [doc event line-lengths clipboard]
  (let [input {:lines (:lines doc)
               :cursor (:cursor doc)
               :selection (:selection doc)
               :desired-col (:desired-col doc)}]
    (case (:type event)
      :char
      (let [new-input (text-input/insert-char input (:char event) true)]
        (merge doc new-input {:selection nil}))

      :backspace
      (let [new-input (text-input/delete-backward input true)]
        (merge doc new-input))

      :delete
      (let [new-input (text-input/delete-forward input true)]
        (merge doc new-input))

      :enter
      (let [new-input (text-input/insert-char input "\n" true)]
        (merge doc new-input {:selection nil}))

      :left
      (let [new-input (text-input/move-cursor input :left true line-lengths)]
        (merge doc new-input))

      :right
      (let [new-input (text-input/move-cursor input :right true line-lengths)]
        (merge doc new-input))

      :up
      (let [new-input (text-input/move-cursor input :up true line-lengths)]
        (merge doc new-input))

      :down
      (let [new-input (text-input/move-cursor input :down true line-lengths)]
        (merge doc new-input))

      :home
      (let [new-input (text-input/move-cursor input :home true line-lengths)]
        (merge doc new-input))

      :end
      (let [new-input (text-input/move-cursor input :end true line-lengths)]
        (merge doc new-input))

      :word-left
      (let [new-input (text-input/move-word input :left true line-lengths)]
        (merge doc new-input))

      :word-right
      (let [new-input (text-input/move-word input :right true line-lengths)]
        (merge doc new-input))

      :paste
      (if clipboard
        (let [new-input (text-input/paste input clipboard true)]
          (merge doc new-input))
        doc)

      ;; Default: no change
      doc)))

(defn cmd-panel-apply-event
  "Pure function: apply event to command panel, returns new panel state"
  [panel event clipboard]
  (let [input {:text (:text panel) :cursor (:cursor panel)}]
    (case (:type event)
      :char
      (let [new-input (text-input/insert-char input (:char event) false)]
        (assoc panel :text (:text new-input) :cursor (:cursor new-input)))

      :backspace
      (let [new-input (text-input/delete-backward input false)]
        (assoc panel :text (:text new-input) :cursor (:cursor new-input)))

      :delete
      (let [new-input (text-input/delete-forward input false)]
        (assoc panel :text (:text new-input) :cursor (:cursor new-input)))

      :enter
      ;; Submit command - will be handled by caller
      panel

      :left
      (let [new-input (text-input/move-cursor input :left false nil)]
        (assoc panel :cursor (:cursor new-input)))

      :right
      (let [new-input (text-input/move-cursor input :right false nil)]
        (assoc panel :cursor (:cursor new-input)))

      :home
      (let [new-input (text-input/move-cursor input :home false nil)]
        (assoc panel :cursor (:cursor new-input)))

      :end
      (let [new-input (text-input/move-cursor input :end false nil)]
        (assoc panel :cursor (:cursor new-input)))

      :word-left
      (let [new-input (text-input/move-word input :left false nil)]
        (assoc panel :cursor (:cursor new-input)))

      :word-right
      (let [new-input (text-input/move-word input :right false nil)]
        (assoc panel :cursor (:cursor new-input)))

      :paste
      (if clipboard
        (let [new-input (text-input/paste input clipboard false)]
          (assoc panel :text (:text new-input) :cursor (:cursor new-input)))
        panel)

      ;; Default: no change
      panel)))

(defn cmd-prompt-text
  "Build the command panel prompt string for a given provider."
  [provider]
  (str "[" (-> (or provider :claude) name str/upper-case) "]> "))

(defn cmd-text-start-x
  "Compute the x-pixel where user-typed text begins, after the prompt.
   Must be used consistently by caret, text-ops, and mouse click handlers."
  [provider font-size char-width dpr snap?]
  (let [prompt (cmd-prompt-text provider)
        char-advance (maybe-snap (* font-size char-width) dpr snap?)
        prompt-x (maybe-snap 24 dpr snap?)]
    (maybe-snap (+ prompt-x (* (count prompt) char-advance)) dpr snap?)))

(defn parse-agent-command
  "Parse command-panel text into an agent action.
   Supported forms:
   - /provider claude|codex|gemini
   - /run <raw argv...>
   - /bootstrap, /select, /arrange, /run-flow, /review
   - /rework <comment>, /finalize, /status, /reset
   - plain text prompt"
  [cmd-text current-provider]
  (let [trimmed (str/trim (or cmd-text ""))]
    (cond
      (str/blank? trimmed)
      {:kind :noop}

      (str/starts-with? trimmed "/provider ")
      (let [arg (-> trimmed
                    (subs (count "/provider "))
                    str/trim
                    str/lower-case
                    keyword)]
        (if (contains? #{:claude :codex :gemini} arg)
          {:kind :set-provider :provider arg}
          {:kind :error :message (str "Unknown provider: " arg)}))

      (str/starts-with? trimmed "/replay")
      {:kind :replay}

      (str/starts-with? trimmed "/run ")
      (let [argv (-> trimmed
                     (subs (count "/run "))
                     str/trim
                     (str/split #"\s+")
                     vec)
            first-bin (some-> (first argv) str/lower-case)
            provider (case first-bin
                       "claude" :claude
                       "codex" :codex
                       "gemini" :gemini
                       current-provider)]
        (if (seq argv)
          {:kind :run :provider provider :argv argv :prompt (str/join " " argv)}
          {:kind :error :message "Missing argv for /run"}))

      ;; --- Flow commands (V0 state machine) ---
      (= trimmed "/bootstrap")
      {:kind :flow-bootstrap}

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
      {:kind :run :provider current-provider :prompt trimmed})))

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
   :intake          #{:arrange}
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
  "Fresh flow state for a new session."
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

(defn wrap-line
  "Wrap a single string into lines of at most max-chars, breaking at word
   boundaries (spaces). Falls back to hard char-split when a single word
   exceeds max-chars."
  [line max-chars]
  (if (or (<= (count line) max-chars) (< max-chars 1))
    [line]
    (let [words (str/split line #" ")]
      (loop [ws words cur "" result []]
        (if (empty? ws)
          (if (seq cur)
            (conj result cur)
            result)
          (let [w (first ws)
                candidate (if (seq cur) (str cur " " w) w)]
            (cond
              ;; Fits on current line
              (<= (count candidate) max-chars)
              (recur (rest ws) candidate result)
              ;; Current line has content — flush it, retry word on new line
              (seq cur)
              (recur ws "" (conj result cur))
              ;; Single word longer than max-chars — hard-split it
              :else
              (let [chunks (loop [rem w acc []]
                             (if (<= (count rem) max-chars)
                               (conj acc rem)
                               (recur (subs rem max-chars)
                                      (conj acc (subs rem 0 max-chars)))))]
                (recur (rest ws)
                       (peek chunks)
                       (into result (pop chunks)))))))))))

;; ============================================================================
;; TICKET CARD LAYOUT (V0 Flow Canvas — visual grid of Linear tickets)
;; ============================================================================

(defn flow-canvas-active?
  "True when the flow state machine is in a node that shows the master-detail list view
   instead of the code editor. Currently :intake and :arrange."
  [flow-state]
  (contains? #{:intake :arrange} (:node flow-state)))

;; ============================================================================
;; SCREEN 1: MASTER-DETAIL INTAKE (List View)
;; ============================================================================

(def list-row-h 24)
(def list-group-header-h 28)
(def list-left-pane-pct 0.40)
(def list-padding-x 12)
(def list-padding-top 36)
(def list-checkbox-size 12)
(def list-divider-w 1)

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
  "Total content height for the grouped ticket list (for scroll clamping)."
  [grouped-tickets collapsed-groups]
  (reduce (fn [h {:keys [status tickets]}]
            (+ h list-group-header-h
               (if (contains? collapsed-groups status) 0 (* (count tickets) list-row-h))))
          list-padding-top
          grouped-tickets))

(defn compute-ticket-list-rects
  "All rects for Screen 1 intake: left pane list + divider + right pane.
   Coordinates: left pane items in content space (camera scrolls them).
   Fixed elements (backgrounds, divider, right pane) use scroll-y offset."
  [flow-state viewport-w viewport-h scroll-y hovered-row-idx collapsed-groups]
  (let [tickets (:tickets flow-state)
        selected-set (set (:selected flow-state))
        grouped (group-tickets-by-status tickets)
        layout (ticket-list-layout grouped collapsed-groups selected-set)
        left-w (int (* viewport-w list-left-pane-pct))
        ;; Fixed backgrounds (compensate camera with scroll-y)
        left-bg {:x 0 :y scroll-y :w left-w :h viewport-h
                 :r 0.10 :g 0.10 :b 0.12 :a 1.0}
        divider {:x left-w :y scroll-y :w list-divider-w :h viewport-h
                 :r 0.25 :g 0.25 :b 0.30 :a 1.0}
        right-bg {:x (+ left-w list-divider-w) :y scroll-y
                  :w (- viewport-w left-w list-divider-w) :h viewport-h
                  :r 0.11 :g 0.11 :b 0.13 :a 1.0}
        ;; Project header (fixed to top of left pane)
        header-bg {:x 0 :y scroll-y :w left-w :h list-padding-top
                   :r 0.12 :g 0.12 :b 0.15 :a 1.0}
        ;; Separator line under header (matching mockup)
        header-sep {:x list-padding-x :y (+ scroll-y list-padding-top -1)
                    :w (- left-w (* 2 list-padding-x)) :h 1
                    :r 0.25 :g 0.25 :b 0.30 :a 0.6}
        ;; Right-pane detail rects (when tickets are selected)
        selected (:selected flow-state)
        right-x (+ left-w list-divider-w)
        right-w (- viewport-w left-w list-divider-w)
        right-detail-rects
        (when (seq selected)
          (let [detail-y (+ scroll-y 44)
                ;; Header bar for ticket ID
                id-bar {:x (+ right-x list-padding-x) :y detail-y
                        :w (- right-w (* 2 list-padding-x)) :h 28
                        :r 0.13 :g 0.14 :b 0.18 :a 1.0}
                ;; Separator under ID bar
                id-sep {:x (+ right-x list-padding-x) :y (+ detail-y 28)
                        :w (- right-w (* 2 list-padding-x)) :h 1
                        :r 0.25 :g 0.25 :b 0.30 :a 0.4}]
            [id-bar id-sep]))
        ;; Visible viewport bounds for clipping (in content space)
        visible-top (+ scroll-y list-padding-top)
        visible-bottom (+ scroll-y viewport-h)
        ;; Layout items (clipped to visible left-pane area)
        item-rects
        (into []
          (comp
            (filter (fn [entry]
                      (let [y (:y entry)
                            h (if (= (:type entry) :group-header) list-group-header-h list-row-h)]
                        (and (< y visible-bottom) (> (+ y h) visible-top)))))
            (mapcat (fn [entry]
                      (case (:type entry)
                        :group-header
                        [{:x 0 :y (:y entry) :w left-w :h list-group-header-h
                          :r 0.13 :g 0.13 :b 0.16 :a 1.0}]
                        :ticket-row
                        (let [y (:y entry)
                              idx (:idx entry)
                              sel? (:selected? entry)
                              hov? (= idx hovered-row-idx)
                              ;; Row bg
                              row-bg {:x 0 :y y :w left-w :h list-row-h
                                      :r (cond sel? 0.15 hov? 0.13 :else 0.10)
                                      :g (cond sel? 0.17 hov? 0.13 :else 0.10)
                                      :b (cond sel? 0.25 hov? 0.16 :else 0.12)
                                      :a 1.0}
                              ;; Checkbox outline
                              cb-x (+ list-padding-x 4)
                              cb-y (+ y (/ (- list-row-h list-checkbox-size) 2))
                              cb-border {:x cb-x :y cb-y :w list-checkbox-size :h list-checkbox-size
                                         :r 0.35 :g 0.40 :b 0.50 :a (if sel? 1.0 0.6)}
                              cb-fill (when sel?
                                        {:x (+ cb-x 2) :y (+ cb-y 2)
                                         :w (- list-checkbox-size 4) :h (- list-checkbox-size 4)
                                         :r 0.35 :g 0.55 :b 0.95 :a 1.0})
                              ;; Priority dot (for P1/P2)
                              prio (or (:priority (:ticket entry)) 0)
                              prio-dot (when (<= 1 prio 2)
                                         (let [pc (get priority-colors prio)]
                                           {:x (- left-w 20) :y (+ y (/ (- list-row-h 6) 2))
                                            :w 6 :h 6
                                            :r (:r pc) :g (:g pc) :b (:b pc) :a (:a pc)}))]
                          (cond-> [row-bg cb-border]
                            cb-fill (conj cb-fill)
                            prio-dot (conj prio-dot)))))))
          layout)]
    (cond-> (into [left-bg right-bg header-bg header-sep divider] item-rects)
      right-detail-rects (into right-detail-rects))))

(defn compute-ticket-list-text-ops
  "All text ops for Screen 1 intake: project header + grouped list + right pane content.
   Left pane items in content space. Right pane and header pinned to viewport via scroll-y."
  [flow-state viewport-w viewport-h font-size char-advance scroll-y
   hovered-row-idx collapsed-groups]
  (let [tickets (:tickets flow-state)
        selected (:selected flow-state)
        selected-set (set selected)
        grouped (group-tickets-by-status tickets)
        layout (ticket-list-layout grouped collapsed-groups selected-set)
        left-w (int (* viewport-w list-left-pane-pct))
        right-x (+ left-w list-divider-w list-padding-x)
        right-w (- viewport-w left-w list-divider-w (* 2 list-padding-x))
        list-font (- font-size 2)
        meta-font (- font-size 3)
        list-advance (if (pos? font-size) (* list-font (/ char-advance font-size)) char-advance)
        ;; Visible viewport bounds (content space)
        visible-top (+ scroll-y list-padding-top)
        visible-bottom (+ scroll-y viewport-h)
        ;; Project header (pinned to viewport top)
        header-text (str "DISCOURSE-GRAPH  " (count tickets) " active")
        header-ops [{:text header-text :type :macro
                     :from 0 :to (count header-text)
                     :x list-padding-x :y (+ scroll-y 22)
                     :size font-size
                     :r 0.75 :g 0.80 :b 0.95 :a 1.0}]
        ;; Layout items (clipped to visible)
        list-ops
        (into []
          (comp
            (filter (fn [entry]
                      (let [y (:y entry)
                            h (if (= (:type entry) :group-header) list-group-header-h list-row-h)]
                        (and (< y visible-bottom) (> (+ y h) visible-top)))))
            (mapcat (fn [entry]
                      (case (:type entry)
                        :group-header
                        (let [y (:y entry)
                              collapse-ind (if (:collapsed? entry) "\u25B8" "\u25BE")
                              label (str collapse-ind " " (:icon entry) "  " (:status entry) "  " (:count entry))
                              ic (get status-icon-colors (:status entry) {:r 0.6 :g 0.6 :b 0.6 :a 0.8})]
                          [[{:text label :type :keyword
                             :from 0 :to (count label)
                             :x list-padding-x :y (+ y 19)
                             :size list-font
                             :r (:r ic) :g (:g ic) :b (:b ic) :a (:a ic)}]])
                        :ticket-row
                        (let [y (:y entry)
                              ticket (:ticket entry)
                              sel? (:selected? entry)
                              title (or (:title ticket) "Untitled")
                              ;; Column X positions
                              cb-x (+ list-padding-x 4)
                              title-x (+ list-padding-x list-checkbox-size 14)
                              max-title-chars (if (pos? list-advance)
                                                (max 8 (int (/ (- left-w title-x 36) list-advance)))
                                                30)
                              trunc-title (if (> (count title) max-title-chars)
                                            (str (subs title 0 (- max-title-chars 1)) "\u2026")
                                            title)
                              prio (or (:priority ticket) 0)
                              prio-text (str "P" prio)
                              prio-x (- left-w 36)
                              text-y (+ y 17)
                              ;; Checkbox character
                              cb-text (if sel? "\u2611" "\u2610")]
                          [[{:text cb-text :type :keyword
                             :from 0 :to (count cb-text)
                             :x cb-x :y text-y
                             :size list-font
                             :r 0.45 :g 0.60 :b 0.85 :a (if sel? 1.0 0.5)}]
                           [{:text trunc-title :type :text
                             :from 0 :to (count trunc-title)
                             :x title-x :y text-y
                             :size list-font
                             :r 0.80 :g 0.80 :b 0.82 :a 1.0}]
                           [{:text prio-text :type :comment
                             :from 0 :to (count prio-text)
                             :x prio-x :y text-y
                             :size meta-font
                             :r (if (<= prio 2) 0.95 0.55)
                             :g (if (<= prio 2) 0.55 0.55)
                             :b (if (<= prio 2) 0.30 0.60)
                             :a 0.8}]])))))
          layout)
        ;; Right pane content (pinned to viewport)
        right-center-y (+ scroll-y (/ viewport-h 2))
        detail-max-chars (if (pos? char-advance)
                           (max 20 (int (/ right-w char-advance)))
                           60)
        right-ops
        (cond
          ;; Empty tickets (bootstrap returned nothing)
          (empty? tickets)
          [[{:text "No tickets found." :type :text
             :from 0 :to 18
             :x right-x :y (- right-center-y 20)
             :size font-size
             :r 0.60 :g 0.60 :b 0.65 :a 0.8}]
           [{:text "Run /bootstrap to fetch Linear tickets." :type :comment
             :from 0 :to 39
             :x right-x :y (+ right-center-y 8)
             :size (- font-size 1)
             :r 0.50 :g 0.50 :b 0.55 :a 0.6}]]
          ;; No selection
          (empty? selected)
          [[{:text "No tickets selected yet." :type :text
             :from 0 :to 24
             :x right-x :y (- right-center-y 20)
             :size font-size
             :r 0.60 :g 0.60 :b 0.65 :a 0.8}]
           [{:text "Select tickets from the list," :type :comment
             :from 0 :to 29
             :x right-x :y (+ right-center-y 8)
             :size (- font-size 1)
             :r 0.50 :g 0.50 :b 0.55 :a 0.6}]
           [{:text "then /arrange sequential|parallel." :type :comment
             :from 0 :to 35
             :x right-x :y (+ right-center-y 28)
             :size (- font-size 1)
             :r 0.50 :g 0.50 :b 0.55 :a 0.6}]]
          ;; Single selection — rich detail with description
          (= 1 (count selected))
          (let [idx (first selected)
                ticket (nth tickets idx nil)
                ttitle (or (:title ticket) "Untitled")
                tstatus (or (:status ticket) "?")
                tprio (or (:priority ticket) 0)
                tassignee (or (:assignee ticket) "unassigned")
                tdesc (or (:description ticket) "")
                prio-label (case tprio 1 "Urgent" 2 "High" 3 "Medium" 4 "Low" "None")
                meta-line (str tstatus "  |  " prio-label "  |  " tassignee)
                ;; Layout Y positions (relative to viewport top)
                header-y (+ scroll-y 52)
                ;; Wrap title as the hero header
                title-lines (wrap-line ttitle detail-max-chars)
                title-line-h 20
                title-ops (mapv (fn [i line]
                                  [{:text line :type :text
                                    :from 0 :to (count line)
                                    :x right-x :y (+ header-y (* i title-line-h))
                                    :size (+ font-size 2)
                                    :r 0.85 :g 0.85 :b 0.88 :a 1.0}])
                                (range) title-lines)
                meta-y (+ header-y (* (count title-lines) title-line-h) 4)
                desc-start-y (+ meta-y 24)
                ;; Wrap description text
                desc-lines (if (seq tdesc)
                             (wrap-line tdesc detail-max-chars)
                             [])
                desc-line-h 18
                desc-ops (mapv (fn [i line]
                                 [{:text line :type :text
                                   :from 0 :to (count line)
                                   :x right-x :y (+ desc-start-y (* i desc-line-h))
                                   :size (- font-size 1)
                                   :r 0.70 :g 0.70 :b 0.73 :a 0.9}])
                               (range) desc-lines)
                ;; Action hint below description
                hint-y (+ desc-start-y (max desc-line-h (* (count desc-lines) desc-line-h)) 16)
                hint-text "/arrange sequential|parallel to proceed"]
            (into
              (into
                (into title-ops
                  [[{:text meta-line :type :comment
                     :from 0 :to (count meta-line)
                     :x right-x :y meta-y
                     :size (- font-size 1)
                     :r 0.55 :g 0.55 :b 0.60 :a 0.8}]
                   [{:text hint-text :type :comment
                     :from 0 :to (count hint-text)
                     :x right-x :y hint-y
                     :size (- font-size 2)
                     :r 0.40 :g 0.40 :b 0.45 :a 0.5}]])
              desc-ops))
          ;; Multi selection — batch summary with ticket list
          :else
          (let [n-selected (count selected)
                count-text (str n-selected " tickets selected")
                ;; List each selected ticket
                sel-ticket-ops
                (into []
                  (map-indexed
                    (fn [i sel-idx]
                      (let [ticket (nth tickets sel-idx nil)
                            ttitle (or (:title ticket) "Untitled")
                            trunc (if (> (count ttitle) detail-max-chars)
                                    (str (subs ttitle 0 (- detail-max-chars 1)) "\u2026")
                                    ttitle)]
                        [{:text trunc :type :text
                          :from 0 :to (count trunc)
                          :x right-x :y (+ scroll-y 88 (* i 20))
                          :size (- font-size 1)
                          :r 0.70 :g 0.75 :b 0.80 :a 0.9}]))
                    selected))
                hint-y (+ scroll-y 88 (* n-selected 20) 16)
                hint-text "/arrange sequential|parallel to proceed"]
            (into
              [[{:text count-text :type :macro
                 :from 0 :to (count count-text)
                 :x right-x :y (+ scroll-y 52)
                 :size (+ font-size 2)
                 :r 0.75 :g 0.80 :b 0.95 :a 1.0}]
               [{:text hint-text :type :comment
                 :from 0 :to (count hint-text)
                 :x right-x :y hint-y
                 :size (- font-size 2)
                 :r 0.40 :g 0.40 :b 0.45 :a 0.5}]]
              sel-ticket-ops))))]
    (into (into [header-ops] list-ops) right-ops)))

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
;; RESPONSE PARSER (Extract structured data from agent output)
;; ============================================================================

(def priority-label->num
  {"urgent" 1 "high" 2 "medium" 3 "low" 4 "none" 0
   "1" 1 "2" 2 "3" 3 "4" 4 "0" 0})

(defn normalize-priority
  "Coerce a priority value (number, string label, or string digit) to an int 0-4."
  [p]
  (cond
    (number? p) (int p)
    (string? p) (or (get priority-label->num (str/lower-case p)) 0)
    :else 0))

(defn normalize-ticket
  "Map a raw issue object (from Linear MCP or Claude JSON) to our ticket shape.
   Handles both Linear native fields and pre-formatted fields."
  [t]
  {:id (or (:identifier t) (:id t) "UNKNOWN")
   :title (or (:title t) "Untitled")
   :status (or (get-in t [:state :name]) (:status t) "unknown")
   :assignee (or (get-in t [:assignee :name]) (:assignee t) "unassigned")
   :priority (normalize-priority (:priority t))
   :description (or (:description t) "")})

(defn parse-tickets-from-trail
  "Extract tickets from trail :tool-result events (raw MCP responses).
   Tries to parse each tool result as JSON containing an array of issues.
   Returns [{:id :title :status ...}] or nil."
  [trail]
  (let [tool-results (->> trail
                          (filter #(= :tool-result (:kind %)))
                          (mapv :content))
        ;; Try each tool result — the Linear MCP response is usually a JSON array
        tickets (some (fn [content]
                        (when (string? content)
                          (try
                            (let [parsed (js/JSON.parse content)
                                  data (js->clj parsed :keywordize-keys true)
                                  ;; Handle both direct array and {:issues [...]} wrapper
                                  arr (cond
                                        (vector? data) data
                                        (vector? (:issues data)) (:issues data)
                                        (vector? (:nodes data)) (:nodes data)
                                        :else nil)]
                              (when (and (seq arr) (or (:title (first arr))
                                                       (:identifier (first arr))))
                                (mapv normalize-ticket arr)))
                            (catch :default _ nil))))
                      tool-results)]
    tickets))

(defn parse-tickets-from-output
  "Fallback: extract a JSON ticket array from agent text output.
   Tries ```json code block first, then bracket-matching.
   Returns [{:id :title :status :assignee :priority}] or nil."
  [output-text]
  (when (and output-text (not (str/blank? output-text)))
    (let [json-block-re #"(?s)```json\s*\n?(.*?)\n?\s*```"
          match1 (re-find json-block-re output-text)
          json-str (if match1
                     (second match1)
                     (let [start (str/index-of output-text "[")]
                       (when start
                         (loop [i start depth 0 max-i (min (count output-text) (+ start 50000))]
                           (if (>= i max-i)
                             nil
                             (let [c (.charAt output-text i)
                                   new-depth (cond (= c \[) (inc depth)
                                                   (= c \]) (dec depth)
                                                   :else depth)]
                               (if (zero? new-depth)
                                 (subs output-text start (inc i))
                                 (recur (inc i) new-depth max-i))))))))]
      (when json-str
        (try
          (let [parsed (js/JSON.parse json-str)
                arr (js->clj parsed :keywordize-keys true)]
            (when (vector? arr)
              (mapv normalize-ticket arr)))
          (catch :default e
            (js/console.warn "[FLOW] Failed to parse tickets JSON:" (.-message e))
            nil))))))

(def ticket-json-schema
  "JSON schema for Claude CLI --json-schema flag. Validates structured ticket output."
  (js/JSON.stringify
    (clj->js {:type "object"
              :properties {:tickets {:type "array"
                                     :items {:type "object"
                                             :properties {:id {:type "string"}
                                                          :title {:type "string"}
                                                          :status {:type "string"}
                                                          :assignee {:type "string"}
                                                          :priority {:type "string"}
                                                          :description {:type "string"}}
                                             :required ["title" "status"]}}}
              :required ["tickets"]})))

(defn parse-structured-result
  "Parse the structured result from Claude CLI --json-schema output.
   Returns [{:id :title :status ...}] or nil."
  [structured-result]
  (when structured-result
    (try
      (let [parsed (if (string? structured-result)
                     (js/JSON.parse structured-result)
                     (clj->js structured-result))
            data (js->clj parsed :keywordize-keys true)
            tickets (:tickets data)]
        (when (seq tickets)
          (mapv normalize-ticket tickets)))
      (catch :default e
        (js/console.warn "[FLOW] Failed to parse structured result:" (.-message e))
        nil))))

(defn stream-agent-run!
  "Streaming fetch: POST to url, read SSE events via ReadableStream.
   Calls (on-event edn-map) for each parsed SSE event.
   Calls (on-error err) on failure. Returns nil."
  [url body on-event on-error]
  (-> (js/fetch url
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/edn"}
                          :body (pr-str body)}))
      (.then
        (fn [resp]
          (if-not (.-ok resp)
            (on-error (js/Error. (str "HTTP " (.-status resp))))
            (let [rdr    (.getReader (.-body resp))
                  !buf   (atom "")]
              (letfn [(pump []
                        (-> (.read rdr)
                            (.then
                              (fn [result]
                                (if (.-done result)
                                  ;; Stream ended — flush any remaining buffer
                                  (let [remaining @!buf]
                                    (when (seq remaining)
                                      (doseq [chunk (str/split remaining #"\n\n")]
                                        (let [trimmed (str/trim chunk)]
                                          (when (str/starts-with? trimmed "data: ")
                                            (try
                                              (on-event (reader/read-string (subs trimmed 6)))
                                              (catch :default _ nil)))))))
                                  ;; Got a chunk — decode + split on SSE boundary
                                  (let [text  (.decode (js/TextDecoder.) (.-value result))
                                        buf   (swap! !buf str text)
                                        parts (str/split buf #"\n\n" -1)]
                                    ;; All parts except the last are complete events
                                    (reset! !buf (peek parts))
                                    (doseq [part (pop parts)]
                                      (let [trimmed (str/trim part)]
                                        (when (str/starts-with? trimmed "data: ")
                                          (try
                                            (on-event (reader/read-string (subs trimmed 6)))
                                            (catch :default _ nil)))))
                                    (pump)))))
                            (.catch (fn [e] (on-error e)))))]
                (pump))))))
      (.catch (fn [e] (on-error e))))
  nil)

;; ============================================================================
;; LAYER 6: GPU STATE DERIVED FLOWS
;; ============================================================================

(defn calculate-logical->visual
  "Create reverse mapping from logical line to visual line"
  [line-mapping]
  (reduce-kv (fn [m visual-idx logical-idx]
               (assoc m logical-idx visual-idx))
             {}
             (vec line-mapping)))

(defn build-line-mapping
  "Build visual->logical mapping based on fold regions."
  [lines regions folded]
  (let [num-lines (count lines)]
    (loop [logical-idx 0 mapping []]
      (if (>= logical-idx num-lines)
        mapping
        (let [visible? (not (some (fn [{:keys [start-line end-line]}]
                                    (and (contains? folded start-line)
                                         (> logical-idx start-line)
                                         (<= logical-idx end-line)))
                                  regions))]
          (if visible?
            (recur (inc logical-idx) (conj mapping logical-idx))
            (recur (inc logical-idx) mapping)))))))

(defn compute-fold-state
  "Compute fold regions + line mapping once per doc/fold change.
   Safe for large files because this only runs on document changes (cached in <fold-state),
   NOT on every blink tick."
  [doc folded detect-folds-fn]
  (let [lines (:lines doc)
        lengths (mapv count lines)
        regions (or (detect-folds-fn lines lengths) [])
        line-mapping (build-line-mapping lines regions folded)
        logical->visual (calculate-logical->visual line-mapping)]
    {:lines lines
     :lengths lengths
     :regions regions
     :folded folded
     :line-mapping line-mapping
     :logical->visual logical->visual}))

(defn <fold-state
  "Derived flow: fold regions + line mapping (cached between blinks)."
  [!editor-doc !folded-lines detect-folds-fn]
  (m/latest
    (fn [doc folded]
      (compute-fold-state doc folded detect-folds-fn))
    (m/watch !editor-doc)
    (m/watch !folded-lines)))

(defn <bracket-match
  "Derived flow: cached bracket matching (recomputes on doc change, NOT on blink).
   Safe for all file sizes because this only runs on document changes."
  [!editor-doc find-bracket-fn]
  (m/latest
    (fn [doc]
      (let [lines (:lines doc)
            cursor (:cursor doc)
            selection (:selection doc)]
        (when (and cursor (not selection))
          (let [lengths (mapv count lines)]
            (find-bracket-fn cursor lines lengths)))))
    (m/watch !editor-doc)))

(defn compute-editor-rects
  "Pure function: compute all editor rectangles from PRE-COMPUTED fold state and bracket match.
   No longer calls detect-folds-fn or find-bracket-fn directly — those are cached in separate flows."
  [doc fold-state bracket-match eval-result caret-visible focus
   layout-x layout-y line-h gutter-w char-advance viewport-w]
  (let [cursor (:cursor doc)
        selection (:selection doc)

        ;; Use pre-computed fold state (cached, only changes on doc/fold change)
        {:keys [lengths regions folded line-mapping logical->visual]} fold-state

        ;; Helper to get visual y for logical line
        logical->visual-y (fn [logical-line]
                            (when-let [visual-idx (get logical->visual logical-line)]
                              (+ layout-y (* visual-idx line-h))))

        ;; Use the passed char advance (reactive based on active font)
        char-w char-advance
        gutter-x (- layout-x gutter-w)

        ;; Fold indicator rects
        fold-rects (keep (fn [{:keys [start-line]}]
                          (when-let [visual-y (logical->visual-y start-line)]
                            (let [is-folded? (contains? folded start-line)
                                  indicator-size 8
                                  x (+ gutter-x 2)
                                  y (+ visual-y (/ (- line-h indicator-size) 2))]
                              {:x x :y y :w indicator-size :h indicator-size
                               :r (if is-folded? 0.3 0.7)
                               :g (if is-folded? 0.5 0.6)
                               :b (if is-folded? 0.9 0.3)
                               :a 0.8})))
                        regions)

        ;; Bracket match rects (pre-computed, cached in <bracket-match flow)
        bracket-rects (when bracket-match
                        (keep (fn [{:keys [line col]}]
                                (when-let [visual-y (logical->visual-y line)]
                                  {:x (+ layout-x (* col char-w))
                                   :y visual-y
                                   :w char-w
                                   :h line-h
                                   :r 0.8 :g 0.6 :b 0.2 :a 0.4}))
                              [(:open bracket-match) (:close bracket-match)]))

        ;; Caret rect (only when editor is focused and no selection)
        caret-rect (when (and cursor caret-visible (= focus :editor) (not selection))
                     (when-let [visual-y (logical->visual-y (:line cursor))]
                       {:x (+ layout-x (* (:col cursor) char-w))
                        :y visual-y
                        :w 2
                        :h line-h
                        :r 0.9 :g 0.9 :b 0.9 :a 1.0}))

        ;; Selection rects
        selection-rects (when selection
                          (let [{:keys [start end]} selection
                                [s e] (if (or (> (:line start) (:line end))
                                              (and (= (:line start) (:line end))
                                                   (> (:col start) (:col end))))
                                        [end start]
                                        [start end])]
                            (keep (fn [logical-line]
                                    (when-let [visual-y (logical->visual-y logical-line)]
                                      (let [line-len (get lengths logical-line 0)
                                            col-start (if (= logical-line (:line s)) (:col s) 0)
                                            col-end (if (= logical-line (:line e)) (:col e) line-len)
                                            width-chars (- col-end col-start)]
                                        (when (> width-chars 0)
                                          {:x (+ layout-x (* col-start char-w))
                                           :y visual-y
                                           :w (* width-chars char-w)
                                           :h line-h
                                           :r 0.2 :g 0.4 :b 0.9 :a 0.5}))))
                                  (range (:line s) (inc (:line e))))))

        ;; Current-line highlight (subtle background on cursor's line)
        current-line-rect (when (and cursor (= focus :editor) (not selection))
                            (when-let [visual-y (logical->visual-y (:line cursor))]
                              {:x 0 :y visual-y :w viewport-w :h line-h
                               :r 1.0 :g 1.0 :b 1.0 :a 0.04}))

        ;; Eval result rect
        eval-rect (when eval-result
                    (let [now (js/Date.now)]
                      (when (< now (:expires-at eval-result))
                        (when-let [visual-y (logical->visual-y (:line eval-result))]
                          (let [line-len (get lengths (:line eval-result) 0)
                                result-x (+ layout-x (* (+ line-len 2) char-w))
                                result-w (* (count (:text eval-result)) char-w)]
                            {:x result-x
                             :y visual-y
                             :w (+ result-w 16)
                             :h line-h
                             :r (if (str/starts-with? (:text eval-result) "=>") 0.1 0.4)
                             :g (if (str/starts-with? (:text eval-result) "=>") 0.3 0.1)
                             :b 0.1
                             :a 0.8})))))]

    (vec (concat (if current-line-rect [current-line-rect] [])
                 fold-rects
                 (or bracket-rects [])
                 (or selection-rects [])
                 (if caret-rect [caret-rect] [])
                 (if eval-rect [eval-rect] [])))))

(defn <editor-rects
  "Derived flow: all editor rectangles (selection, caret, brackets, folds, eval)
   Uses m/latest instead of m/ap to avoid cancellation propagation issues.
   REACTIVE: font-size, line-h, char-advance come from !settings and !active-font.
   OPTIMIZED: fold-state and bracket-match are pre-computed in cached flows
   that only recompute when the document changes — NOT on every blink tick.
   MODE-SWITCH: when flow canvas is active, returns ticket card rects instead."
  [!editor-doc !eval-result !caret-visible !focus !settings !active-font !viewport
   <fold-data <bracket-data
   !flow-state !scroll-y !collapsed-groups !hovered-row-idx
   layout-x layout-y gutter-w]
  (m/latest
    (fn [doc fold-state bracket-match eval-result caret-visible focus settings active-font viewport
         flow-state scroll-y collapsed-groups hovered-row-idx]
      (if (flow-canvas-active? flow-state)
        ;; Flow canvas mode: master-detail list view
        (compute-ticket-list-rects flow-state (:width viewport) (:height viewport)
                                   scroll-y hovered-row-idx collapsed-groups)
        ;; Normal editor mode
        (let [dpr (:dpr viewport)
              snap? (:snap-to-pixel? settings)
              font-size (:font-size settings)
              line-h (maybe-snap (* font-size (:line-height settings)) dpr snap?)
              char-advance (maybe-snap (* font-size (:char-width active-font)) dpr snap?)
              layout-x (maybe-snap layout-x dpr snap?)
              layout-y (maybe-snap layout-y dpr snap?)]
          (compute-editor-rects doc fold-state bracket-match eval-result caret-visible focus
                                layout-x layout-y line-h gutter-w char-advance (:width viewport)))))
    (m/watch !editor-doc)
    <fold-data
    <bracket-data
    (m/watch !eval-result)
    (m/watch !caret-visible)
    (m/watch !focus)
    (m/watch !settings)
    (m/watch !active-font)
    (m/watch !viewport)
    (m/watch !flow-state)
    (m/watch !scroll-y)
    (m/watch !collapsed-groups)
    (m/watch !hovered-row-idx)))

(defn trail-node-color
  "Color for a trail node by kind. Returns {:r :g :b :a}."
  [kind tool-name]
  (case kind
    :reasoning    {:r 0.85 :g 0.85 :b 0.85 :a 1.0}
    :tool-call    (case tool-name
                    ("Read" "read")        {:r 0.4 :g 0.85 :b 0.95 :a 1.0}  ;; cyan
                    ("Edit" "edit")        {:r 0.95 :g 0.85 :b 0.35 :a 1.0}  ;; yellow
                    ("Write" "write")      {:r 0.95 :g 0.85 :b 0.35 :a 1.0}  ;; yellow
                    ("Grep" "grep")        {:r 0.55 :g 0.9 :b 0.55 :a 1.0}   ;; green
                    ("Glob" "glob")        {:r 0.55 :g 0.9 :b 0.55 :a 1.0}   ;; green
                    ("Bash" "bash")        {:r 0.9 :g 0.65 :b 0.4 :a 1.0}    ;; orange
                    ("Task" "task")        {:r 0.75 :g 0.6 :b 0.95 :a 1.0}   ;; purple
                                           {:r 0.7 :g 0.7 :b 0.85 :a 1.0})  ;; default blue-gray
    :tool-call-start {:r 0.6 :g 0.6 :b 0.7 :a 0.7}
    :tool-result  {:r 0.55 :g 0.55 :b 0.6 :a 0.7}  ;; dim
    {:r 0.75 :g 0.75 :b 0.75 :a 1.0}))

(defn trail->display-lines
  "Convert trail nodes to [{:text :color}]. Merges consecutive reasoning nodes."
  [trail]
  (reduce
    (fn [acc node]
      (case (:kind node)
        :reasoning
        (let [last-entry (peek acc)]
          (if (and last-entry (= :reasoning (:kind last-entry)))
            ;; Merge with previous reasoning node
            (conj (pop acc) (update last-entry :text str (:text node)))
            (conj acc {:kind :reasoning :text (:text node)
                       :color (trail-node-color :reasoning nil)})))

        :tool-call
        (let [input-summary (let [inp (:input node)]
                              (cond
                                (and (string? (:file_path inp)) (seq (:file_path inp)))
                                (:file_path inp)
                                (and (string? (:pattern inp)) (seq (:pattern inp)))
                                (str "\"" (:pattern inp) "\"")
                                (and (string? (:command inp)) (seq (:command inp)))
                                (let [cmd (:command inp)]
                                  (if (> (count cmd) 60)
                                    (str (subs cmd 0 57) "...")
                                    cmd))
                                :else ""))]
          (conj acc {:kind :tool-call
                     :text (str ">> " (:tool-name node) " " input-summary)
                     :color (trail-node-color :tool-call (:tool-name node))}))

        :tool-call-start
        (conj acc {:kind :tool-call-start
                   :text (str "> " (:tool-name node) "...")
                   :color (trail-node-color :tool-call-start nil)})

        :tool-result
        (let [raw-content (or (:content node) "")
              content (if (string? raw-content) raw-content (pr-str raw-content))
              short (if (> (count content) 120)
                      (str (subs content 0 117) "...")
                      content)]
          (conj acc {:kind :tool-result
                     :text (str "  <- " short)
                     :color (trail-node-color :tool-result nil)}))

        ;; Unknown kind — render as-is
        (conj acc {:kind (:kind node) :text (pr-str node)
                   :color {:r 0.75 :g 0.75 :b 0.75 :a 1.0}})))
    []
    trail))

(defn agent-wrapped-line-count
  "Count wrapped display lines for agent output. Trail-aware: uses structured trail
   when available, falls back to flat :output text. Includes header line in count."
  [agent-output max-chars]
  (let [status (:status agent-output)
        provider-name (some-> (:provider agent-output) name str/upper-case)
        prompt (:prompt agent-output)
        header-text (when status (str "[" provider-name "] " (name status) ": " prompt))
        trail (:trail agent-output)
        display-entries (when (seq trail) (trail->display-lines trail))
        raw-lines (if display-entries
                    ;; Trail path: header + structured trail lines
                    (cond-> []
                      header-text (conj {:text header-text})
                      (and (= status :running) (empty? display-entries)) (conj {:text "..."})
                      (seq display-entries) (into display-entries))
                    ;; Flat text fallback
                    (let [output-lines (str/split-lines (or (:output agent-output) ""))
                          flat-lines (cond-> []
                                       header-text (conj header-text)
                                       (and (= status :running) (empty? output-lines)) (conj "...")
                                       (seq output-lines) (into output-lines))]
                      (mapv (fn [l] {:text l}) flat-lines)))]
    (count (into [] (mapcat (fn [entry]
                              (let [nl-lines (str/split-lines (or (:text entry) ""))]
                                (mapcat #(wrap-line % max-chars) nl-lines))))
                    raw-lines))))

(defn compute-agent-panel-h
  "Pure: dynamic panel height from agent output content.
   Returns 0 when no agent output, otherwise sizes to content capped at 50% viewport.
   Wraps lines to viewport width for accurate height. Trail-aware."
  [agent-output font-size viewport-height viewport-width char-advance]
  (if-not (some? (:status agent-output))
    0
    (let [agent-x 24
          right-pad 24
          available-w (- viewport-width agent-x right-pad)
          max-chars (if (pos? char-advance) (max 1 (int (/ available-w char-advance))) 80)
          line-count (agent-wrapped-line-count agent-output max-chars)
          line-step (* font-size 1.2)
          content-h (+ 16 (* line-count line-step))
          max-h (* viewport-height 0.5)]
      (min content-h max-h))))

(defn <cmd-panel-rects
  "Derived flow: command panel rectangles — always 4 instances:
     [0] agent-output background  (visible when agent has status)
     [1] command-panel background (visible when panel is open)
     [2] caret                    (visible when panel focused + blink on)
     [3] status-bar background    (always visible)
   Zero-size invisible rects for absent elements keep GPU indices stable."
  [!cmd-panel !focus !caret-visible !scroll-y !viewport !settings !active-font
   !ai-provider !agent-output cmd-panel-h status-bar-h]
  (m/latest
    (fn [panel focus caret-visible scroll-y viewport settings active-font
         agent-output]
      (let [dpr (:dpr viewport)
            snap? (:snap-to-pixel? settings)
            font-size (:font-size settings)
            char-advance (maybe-snap (* font-size (:char-width active-font)) dpr snap?)
            invisible {:x 0 :y 0 :w 0 :h 0 :r 0 :g 0 :b 0 :a 0}

            ;; --- Instance 0: agent output background ---
            agent-panel-h (compute-agent-panel-h agent-output font-size (:height viewport)
                                                (:width viewport) char-advance)
            agent-visible? (some? (:status agent-output))
            agent-bg (if agent-visible?
                       (let [agent-y0 (maybe-snap
                                        (+ scroll-y (- (:height viewport)
                                                       cmd-panel-h status-bar-h agent-panel-h 12))
                                        dpr snap?)]
                         {:x 0 :y agent-y0
                          :w (:width viewport) :h (+ agent-panel-h 12)
                          :r 0.10 :g 0.10 :b 0.13 :a 1.0})
                       invisible)

            ;; --- Instance 1: command panel background ---
            panel-y (maybe-snap (+ scroll-y (- (:height viewport) cmd-panel-h status-bar-h)) dpr snap?)
            cmd-bg (if (:visible panel)
                     {:x 0 :y panel-y :w (:width viewport) :h cmd-panel-h
                      :r 0.15 :g 0.15 :b 0.2 :a 1.0}
                     invisible)

            ;; --- Instance 2: caret ---
            text-x (cmd-text-start-x @!ai-provider font-size (:char-width active-font) dpr snap?)
            caret (if (and (:visible panel) caret-visible (= focus :command-panel))
                    {:x (+ text-x (* (:cursor panel) char-advance))
                     :y (maybe-snap (+ panel-y 8) dpr snap?)
                     :w 2
                     :h (maybe-snap (- cmd-panel-h 16) dpr snap?)
                     :r 0.9 :g 0.9 :b 0.9 :a 1.0}
                    invisible)

            ;; --- Instance 3: status bar background (always visible) ---
            status-y (maybe-snap (+ scroll-y (- (:height viewport) status-bar-h)) dpr snap?)
            status-bg {:x 0 :y status-y
                       :w (:width viewport) :h status-bar-h
                       :r 0.12 :g 0.12 :b 0.16 :a 1.0}]

        [agent-bg cmd-bg caret status-bg]))
    (m/watch !cmd-panel)
    (m/watch !focus)
    (m/watch !caret-visible)
    (m/watch !scroll-y)
    (m/watch !viewport)
    (m/watch !settings)
    (m/watch !active-font)
    (m/watch !agent-output)))

(defn compute-settings-panel-rects
  "Pure function: compute settings panel rectangles (background + font list + sliders)"
  [settings focus viewport scroll-y font-manifest font-size]
  (when (:visible settings)
    (let [;; Panel dimensions - centered modal
          panel-w 600
          panel-h 480
          panel-x (/ (- (:width viewport) panel-w) 2)
          panel-y (+ scroll-y (/ (- (:height viewport) panel-h) 2))

          ;; Colors (Modern Dark Theme)
          bg-color       {:r 0.12 :g 0.12 :b 0.14 :a 0.98} ;; Deep dark grey
          border-color   {:r 0.25 :g 0.25 :b 0.28 :a 1.0}
          separator-color {:r 0.20 :g 0.20 :b 0.23 :a 1.0}
          
          item-hover     {:r 0.18 :g 0.18 :b 0.22 :a 1.0}
          item-selected  {:r 0.22 :g 0.22 :b 0.26 :a 1.0}
          item-active    {:r 0.15 :g 0.25 :b 0.40 :a 0.8}  ;; Blue-ish highlight for active focus

          slider-track   {:r 0.20 :g 0.20 :b 0.24 :a 1.0}
          slider-fill    {:r 0.40 :g 0.60 :b 0.85 :a 1.0}  ;; Accent Blue
          slider-thumb   {:r 0.90 :g 0.90 :b 0.95 :a 1.0}

          ;; State
          current-focus (or (:focus-section settings) :fonts) ;; :fonts or :sliders
          font-idx      (or (:selected-index settings) 0)
          slider-idx    (or (:slider-index settings) 0)

          ;; Layout
          left-w 220
          right-w (- panel-w left-w)
          
          ;; Left Pane (Fonts)
          left-pane-x panel-x
          left-pane-y panel-y
          
          ;; Right Pane (Sliders)
          right-pane-x (+ panel-x left-w)
          right-pane-y panel-y
          
          ;; Header
          header-h 40
          content-y (+ panel-y header-h)

          ;; Font List
          fonts (or (:fonts font-manifest)
                    [{:name "DejaVu Sans Mono" :id "dejavu-sans-mono"}])
          available-fonts (filter #(not (false? (:available %))) fonts)
          font-item-h 32
          
          font-rects (map-indexed
                       (fn [idx font]
                         (let [selected? (= idx font-idx)
                               focused?  (= current-focus :fonts)
                               item-y (+ content-y 10 (* idx font-item-h))
                               
                               bg (cond
                                    (and selected? focused?) item-active
                                    selected?                item-selected
                                    :else                    nil)]
                           (when bg
                             {:x (+ left-pane-x 8)
                              :y item-y
                              :w (- left-w 16)
                              :h (- font-item-h 4)
                              :r (:r bg) :g (:g bg) :b (:b bg) :a (:a bg)})))
                       available-fonts)

          ;; Slider List
          sliders (slider-specs settings)
          
          slider-item-h 70
          slider-rects (map-indexed
                         (fn [idx slider]
                           (let [selected? (= idx slider-idx)
                                 focused?  (= current-focus :sliders)
                                 base-y (+ content-y 10 (* idx slider-item-h))
                                 
                                 ;; Calculate ratio
                                 range (- (:max slider) (:min slider))
                                 ratio (/ (- (:val slider) (:min slider)) range)
                                 
                                 ;; Background highlight
                                 bg (cond
                                      (and selected? focused?) item-active
                                      selected?                item-selected
                                      :else                    nil)
                                      
                                 ;; Track geometry
                                 track-x (+ right-pane-x 20)
                                 track-y (+ base-y 40)
                                 track-w (- right-w 40)
                                 track-h 4
                                 fill-w (* track-w ratio)]
                             
                             (concat
                               ;; Item Background
                               (when bg
                                 [{:x (+ right-pane-x 8) :y base-y :w (- right-w 16) :h (- slider-item-h 8)
                                   :r (:r bg) :g (:g bg) :b (:b bg) :a (:a bg)}])
                               
                               ;; Track Background
                               [{:x track-x :y track-y :w track-w :h track-h
                                 :r (:r slider-track) :g (:g slider-track) :b (:b slider-track) :a (:a slider-track)}]
                               
                               ;; Filled Track
                               [{:x track-x :y track-y :w fill-w :h track-h
                                 :r (:r slider-fill) :g (:g slider-fill) :b (:b slider-fill) :a (:a slider-fill)}]
                               
                               ;; Thumb/Knob
                               [{:x (+ track-x fill-w -3) :y (- track-y 5) :w 6 :h 14
                                 :r (:r slider-thumb) :g (:g slider-thumb) :b (:b slider-thumb) :a (:a slider-thumb)}])))
                         sliders)]

      (vec
        (concat
          ;; Main Background
          [{:x panel-x :y panel-y :w panel-w :h panel-h
            :r (:r bg-color) :g (:g bg-color) :b (:b bg-color) :a (:a bg-color)}]
            
          ;; Header Separator
          [{:x panel-x :y (+ panel-y header-h) :w panel-w :h 1
            :r (:r separator-color) :g (:g separator-color) :b (:b separator-color) :a (:a separator-color)}]
            
          ;; Vertical Separator
          [{:x (+ panel-x left-w) :y (+ panel-y header-h) :w 1 :h (- panel-h header-h)
            :r (:r separator-color) :g (:g separator-color) :b (:b separator-color) :a (:a separator-color)}]

          ;; Content
          (filter some? font-rects)
          (mapcat identity slider-rects))))))

(defn <settings-panel-rects
  "Derived flow: settings panel rectangles.
   REACTIVE: font-size comes from !settings."
  [!settings !focus !viewport !scroll-y !font-manifest]
  (m/latest
    (fn [settings focus viewport scroll-y font-manifest]
      (let [font-size (:font-size settings)]
        (compute-settings-panel-rects settings focus viewport scroll-y font-manifest font-size)))
    (m/watch !settings)
    (m/watch !focus)
    (m/watch !viewport)
    (m/watch !scroll-y)
    (m/watch !font-manifest)))

(defn compute-settings-panel-text
  "Pure function: compute settings panel text elements"
  [settings viewport scroll-y font-manifest font-size]
  (when (:visible settings)
    (let [panel-w 600
          panel-h 480
          panel-x (/ (- (:width viewport) panel-w) 2)
          panel-y (+ scroll-y (/ (- (:height viewport) panel-h) 2))

          ;; State
          current-focus (or (:focus-section settings) :fonts)
          font-idx      (or (:selected-index settings) 0)
          slider-idx    (or (:slider-index settings) 0)

          ;; Layout
          left-w 220
          right-w (- panel-w left-w)
          right-pane-x (+ panel-x left-w)
          
          header-h 40
          content-y (+ panel-y header-h)

          ;; Font List
          fonts (or (:fonts font-manifest)
                    [{:name "DejaVu Sans Mono" :id "dejavu-sans-mono"}])
          available-fonts (filter #(not (false? (:available %))) fonts)
          font-item-h 32
          
          font-texts (map-indexed
                       (fn [idx font]
                         (let [selected? (= idx font-idx)
                               focused?  (= current-focus :fonts)
                               item-y (+ content-y 10 (* idx font-item-h))
                               text-y (+ item-y (/ font-item-h 2) (/ font-size 3)) ;; Approx center
                               
                               color (cond
                                       (and selected? focused?) {:r 1.0 :g 1.0 :b 1.0 :a 1.0}
                                       selected?                {:r 0.9 :g 0.9 :b 0.9 :a 1.0}
                                       :else                    {:r 0.6 :g 0.6 :b 0.6 :a 1.0})]
                           {:text (:name font)
                            :type :text
                            :from 0 :to (count (:name font))
                            :x (+ panel-x 20)
                            :y text-y
                            :size font-size
                            :r (:r color) :g (:g color) :b (:b color) :a (:a color)}))
                       available-fonts)

          ;; Slider Labels
          sliders (slider-specs settings)
          slider-item-h 70
          
          slider-texts (mapcat
                         (fn [[idx slider]]
                           (let [selected? (= idx slider-idx)
                                 focused?  (= current-focus :sliders)
                                 base-y (+ content-y 10 (* idx slider-item-h))
                                 
                                 label-color (if (and selected? focused?)
                                               {:r 1.0 :g 1.0 :b 1.0 :a 1.0}
                                               {:r 0.8 :g 0.8 :b 0.8 :a 1.0})
                                 val-color   (if (and selected? focused?)
                                               {:r 0.4 :g 0.7 :b 1.0 :a 1.0}
                                               {:r 0.5 :g 0.5 :b 0.5 :a 1.0})
                                 
                                 val-str (case (:id slider)
                                           :theme-id (let [tid (or (:theme-id settings) :gruvbox-dark)
                                                           theme (themes/get-theme tid)]
                                                       (or (:name theme) (name tid)))
                                           :line-height (.toFixed (:val slider) 1)
                                           :sharpness (.toFixed (:val slider) 2)
                                           :snap-to-pixel? (if (pos? (:val slider)) "On" "Off")
                                           :show-diagnostics? (if (pos? (:val slider)) "On" "Off")
                                           (str (:val slider)))]
                             
                             [{:text (:label slider)
                               :type :text
                               :from 0 :to (count (:label slider))
                               :x (+ right-pane-x 20)
                               :y (+ base-y 25)
                               :size font-size
                               :r (:r label-color) :g (:g label-color) :b (:b label-color) :a (:a label-color)}
                              
                              {:text val-str
                               :type :number
                               :from 0 :to (count val-str)
                               :x (+ right-pane-x right-w -20 -10) ;; Right align approx
                               :y (+ base-y 25)
                               :size font-size
                               :r (:r val-color) :g (:g val-color) :b (:b val-color) :a (:a val-color)}]))
                         (map-indexed vector sliders))
          
          ;; Headers
          title-text {:text "Settings"
                      :type :macro
                      :from 0 :to 8
                      :x (+ panel-x 20)
                      :y (+ panel-y 26)
                      :size (+ font-size 2)
                      :r 0.9 :g 0.9 :b 0.9 :a 1.0}
                      
          hint-text {:text "Tab: Switch Pane   Arrows: Navigate/Adjust"
                     :type :comment
                     :from 0 :to 38
                     :x (+ panel-x 20)
                     :y (+ panel-y panel-h -12)
                     :size (- font-size 2)
                     :r 0.5 :g 0.5 :b 0.5 :a 0.8}]

      (vec
        (concat
          [title-text]
          font-texts
          slider-texts
          [hint-text])))))

(defn <settings-panel-text
  "Derived flow: settings panel text elements.
   REACTIVE: font-size comes from !settings."
  [!settings !viewport !scroll-y !font-manifest]
  (m/latest
    (fn [settings viewport scroll-y font-manifest]
      (let [font-size (:font-size settings)]
        (compute-settings-panel-text settings viewport scroll-y font-manifest font-size)))
    (m/watch !settings)
    (m/watch !viewport)
    (m/watch !scroll-y)
    (m/watch !font-manifest)))

(defn <combined-text-ops
  "Derived flow: combined text render ops (editor + command panel + status bar)
   Uses m/latest instead of m/ap to avoid cancellation propagation.
   REACTIVE: font-size comes from !settings, updates live.
   MODE-SWITCH: when flow canvas is active, returns ticket card text ops instead."
  [!editor-doc !cmd-panel !ai-provider !agent-output !agent-scroll-y !scroll-y !viewport !settings !active-font
   !current-file
   tokenize-fn layout-fn
   <fold-data
   !flow-state !collapsed-groups !hovered-row-idx
   layout-x layout-y cmd-panel-h status-bar-h]
  (m/latest
    (fn [doc panel provider agent-output agent-scroll-y scroll-y viewport fold-state settings active-font
         current-file flow-state collapsed-groups hovered-row-idx]
      (let [dpr (:dpr viewport)
              snap? (:snap-to-pixel? settings)
              ;; Reactive font settings
              font-size (:font-size settings)
              char-width (:char-width active-font)
              char-advance (maybe-snap (* font-size char-width) dpr snap?)
              line-h (maybe-snap (* font-size (:line-height settings)) dpr snap?)
              layout-x (maybe-snap layout-x dpr snap?)
              layout-y (maybe-snap layout-y dpr snap?)
              ;; Theme
              theme-id (or (:theme-id settings) :gruvbox-dark)

              ;; MODE-SWITCH: flow canvas replaces editor ops with ticket card text
              [editor-ops final-line-mapping line-num-ops]
              (if (flow-canvas-active? flow-state)
                ;; Flow canvas mode: master-detail list view text
                [(compute-ticket-list-text-ops flow-state (:width viewport) (:height viewport)
                                               font-size char-advance scroll-y
                                               hovered-row-idx collapsed-groups)
                 (vec (range (count (:lines doc))))
                 []]

                ;; Normal editor mode — original logic below
                (let [;; Pre-computed fold state from <fold-data
                      folded (or (:folded fold-state) #{})
                      regions (or (:regions fold-state) [])
                      lines (:lines doc)
                      total-line-count (count lines)
                      large-file? (and (> total-line-count 500) (empty? folded))
                      visible-start (max 0 (- (int (/ scroll-y line-h)) 5))
                      visible-end (min total-line-count (+ (int (/ (+ scroll-y (:height viewport)) line-h)) 5))
                      visible-lines (subvec lines visible-start visible-end)
                      tokenized-visible (mapv tokenize-fn visible-lines)
                      cursor-line (:line (:cursor doc))]
                  (if large-file?
                    ;; FAST PATH: skip fold detection entirely
                    (let [adjusted-y (+ layout-y (* visible-start line-h))
                          result (layout-fn tokenized-visible layout-x adjusted-y font-size
                                            [] #{} char-advance line-h theme-id)
                          full-mapping (vec (range total-line-count))
                          nums (mapv (fn [i]
                                       (let [logical (+ visible-start i)
                                             num-str (str (inc logical))
                                             num-w (* (count num-str) char-advance)
                                             x (maybe-snap (- layout-x 8 num-w) dpr snap?)
                                             y (+ adjusted-y font-size (* i line-h))
                                             current? (= logical cursor-line)]
                                         [{:text num-str :type :line-number
                                           :from 0 :to (count num-str)
                                           :x x :y y :size font-size
                                           :r (if current? 0.85 0.45)
                                           :g (if current? 0.85 0.45)
                                           :b (if current? 0.85 0.45)
                                           :a (if current? 0.9 0.4)}]))
                                     (range (count visible-lines)))]
                      [(:render-ops result) full-mapping nums])

                    ;; NORMAL PATH (<500 lines or folds active): full fold support
                    (do (when (seq folded)
                          (js/console.log "[TEXT-OPS] folded:" (clj->js folded)
                                          "total-lines:" total-line-count
                                          "regions:" (count regions)))
                    (let [tokenized-all (into []
                                          (map-indexed
                                            (fn [idx _]
                                              (if (and (>= idx visible-start) (< idx visible-end))
                                                (nth tokenized-visible (- idx visible-start))
                                                [])))
                                          lines)
                          result (layout-fn tokenized-all layout-x layout-y font-size
                                            regions folded char-advance line-h theme-id)
                          _ (when (seq folded)
                              (js/console.log "[TEXT-OPS] render-ops:" (count (:render-ops result))
                                              "mapping:" (count (:line-mapping result))
                                              "regions:" (count regions)))
                          mapping (:line-mapping result)
                          nums (mapv (fn [visual-idx]
                                       (let [logical (get mapping visual-idx visual-idx)
                                             num-str (str (inc logical))
                                             num-w (* (count num-str) char-advance)
                                             x (maybe-snap (- layout-x 8 num-w) dpr snap?)
                                             y (+ layout-y font-size (* visual-idx line-h))
                                             current? (= logical cursor-line)]
                                         [{:text num-str :type :line-number
                                           :from 0 :to (count num-str)
                                           :x x :y y :size font-size
                                           :r (if current? 0.85 0.45)
                                           :g (if current? 0.85 0.45)
                                           :b (if current? 0.85 0.45)
                                           :a (if current? 0.9 0.4)}]))
                                     (range (count mapping)))]
                      [(filterv seq (:render-ops result)) mapping nums])))))

              ;; Command panel ops (if visible)
              cmd-ops (when (:visible panel)
                        (let [cmd-panel-y (maybe-snap (+ scroll-y (- (:height viewport) cmd-panel-h status-bar-h)) dpr snap?)
                              cmd-text-y (maybe-snap (+ cmd-panel-y 12 font-size) dpr snap?)
                              prompt-text (cmd-prompt-text provider)
                              prompt-x (maybe-snap 24 dpr snap?)
                              text-x (cmd-text-start-x provider font-size (:char-width active-font) dpr snap?)]
                          [(when (seq (:text panel))
                             [{:text (:text panel)
                               :type :text
                               :from 0 :to (count (:text panel))
                               :x text-x :y cmd-text-y
                               :size font-size
                               :r 0.9 :g 0.9 :b 0.9 :a 1.0}])
                           [{:text prompt-text
                             :type :macro
                             :from 0 :to (count prompt-text)
                             :x prompt-x :y cmd-text-y
                             :size font-size
                             :r 0.3 :g 0.6 :b 1.0 :a 1.0}]
                           (when (empty? (:text panel))
                             [{:text "Ask AI..."
                               :type :comment
                               :from 0 :to 10
                               :x text-x :y cmd-text-y
                               :size font-size
                               :r 0.5 :g 0.5 :b 0.5 :a 0.7}])]))
              cmd-lines (if (:visible panel) (vec (filter some? cmd-ops)) [])]

          (let [status (:status agent-output)
                provider-name (some-> (:provider agent-output) name str/upper-case)
                prompt (:prompt agent-output)
                result-output (or (:output agent-output) "")
                status-color (case status
                               :complete {:r 0.55 :g 0.9 :b 0.55 :a 1.0}
                               :failed {:r 0.95 :g 0.45 :b 0.45 :a 1.0}
                               :timeout {:r 0.95 :g 0.75 :b 0.35 :a 1.0}
                               :running {:r 0.6 :g 0.8 :b 1.0 :a 1.0}
                               :submitting {:r 0.6 :g 0.8 :b 1.0 :a 1.0}
                               {:r 0.75 :g 0.75 :b 0.75 :a 1.0})
                header-text (cond
                              (nil? status) nil
                              (= status :running) (str "[" provider-name "] running: " prompt)
                              (= status :submitting) (str "[" provider-name "] submitting: " prompt)
                              (= status :failed) (str "[" provider-name "] failed: " prompt)
                              (= status :timeout) (str "[" provider-name "] timeout: " prompt)
                              (= status :complete) (str "[" provider-name "] complete: " prompt)
                              :else (str "[" provider-name "] " (name status) ": " prompt))
                output-lines (str/split-lines result-output)
                agent-x-px 24
                right-pad 24
                available-w (- (:width viewport) agent-x-px right-pad)
                max-chars (if (pos? char-advance) (max 1 (int (/ available-w char-advance))) 80)

                ;; Trail-aware line generation: use trail if available, fall back to flat text
                trail (:trail agent-output)
                display-entries (if (seq trail)
                                  (trail->display-lines trail)
                                  nil)
                raw-lines (if display-entries
                            ;; Trail path: header + trail display lines (each carries its own color)
                            (cond-> []
                              header-text (conj {:text header-text :color status-color})
                              (and (= status :running) (empty? display-entries)) (conj {:text "..." :color status-color})
                              (seq display-entries) (into display-entries))
                            ;; Flat text fallback (backward compat)
                            (let [flat-lines (cond-> []
                                              header-text (conj header-text)
                                              (and (= status :running) (empty? output-lines)) (conj "...")
                                              (seq output-lines) (into output-lines))]
                              (mapv (fn [l] {:text l :color status-color}) flat-lines)))

                ;; Wrap all lines (both trail and flat share this path)
                ;; Split by newlines FIRST, then wrap — prevents \n inside text ops
                ;; which causes shape-text to bump Y and overlap with the next text op
                all-lines (into []
                            (mapcat (fn [entry]
                              (let [nl-lines (str/split-lines (or (:text entry) ""))
                                    wrapped (mapcat #(wrap-line % max-chars) nl-lines)]
                                (mapv (fn [wl] {:text wl :color (:color entry)}) wrapped))))
                            raw-lines)

                agent-panel-h (compute-agent-panel-h agent-output font-size (:height viewport)
                                                     (:width viewport) char-advance)
                agent-x (maybe-snap 24 dpr snap?)
                agent-y0 (maybe-snap (+ scroll-y (- (:height viewport) cmd-panel-h status-bar-h agent-panel-h 12)) dpr snap?)
                line-step (maybe-snap (* font-size 1.2) dpr snap?)
                panel-top agent-y0
                panel-bottom (+ agent-y0 agent-panel-h)
                agent-lines (into []
                              (comp
                                (map (fn [idx]
                                       (let [entry (nth all-lines idx)
                                             y (+ agent-y0 8 font-size
                                                  (* idx line-step)
                                                  (- agent-scroll-y))
                                             c (:color entry)]
                                         (when (and (>= y (+ panel-top 8))
                                                    (< y panel-bottom))
                                           [{:text (:text entry)
                                             :type :comment
                                             :from 0 :to (count (:text entry))
                                             :x agent-x
                                             :y y
                                             :size font-size
                                             :r (:r c)
                                             :g (:g c)
                                             :b (:b c)
                                             :a (:a c)}]))))
                                (filter some?))
                              (range (count all-lines)))

                ;; Status bar text (always visible, pinned to bottom)
                status-y (maybe-snap (+ scroll-y (- (:height viewport) status-bar-h) 4 font-size) dpr snap?)
                ;; Flow canvas mode: show flow info instead of cursor position
                status-left-text (if (flow-canvas-active? flow-state)
                                   (let [node-name (some-> (:node flow-state) name str/upper-case)
                                         n-tickets (count (:tickets flow-state))
                                         n-selected (count (:selected flow-state))]
                                     (str node-name " | " n-tickets " tickets | " n-selected " selected"))
                                   (let [sb-cursor (:cursor doc)]
                                     (str "Ln " (inc (:line sb-cursor)) ", Col " (inc (:col sb-cursor)))))
                file-name (or (:name current-file) "untitled")
                provider-upper (some-> provider name str/upper-case)
                status-right-text (if provider-upper
                                    (str file-name "  |  " provider-upper)
                                    file-name)
                status-right-w (* (count status-right-text) char-advance)
                status-right-x (maybe-snap (- (:width viewport) status-right-w 16) dpr snap?)
                status-lines [;; Left: cursor position
                              [{:text status-left-text
                                :type :comment
                                :from 0 :to (count status-left-text)
                                :x (maybe-snap 16 dpr snap?) :y status-y
                                :size font-size
                                :r 0.65 :g 0.65 :b 0.65 :a 0.9}]
                              ;; Right: filename | PROVIDER
                              [{:text status-right-text
                                :type :comment
                                :from 0 :to (count status-right-text)
                                :x status-right-x :y status-y
                                :size font-size
                                :r 0.65 :g 0.65 :b 0.65 :a 0.9}]]]

            {:render-ops (vec (concat line-num-ops editor-ops cmd-lines agent-lines status-lines))
             :line-mapping final-line-mapping
             :editor-line-count (+ (count line-num-ops) (count editor-ops))
             :cmd-line-count (+ (count cmd-lines) (count status-lines))})))
    (m/watch !editor-doc)
    (m/watch !cmd-panel)
    (m/watch !ai-provider)
    (m/watch !agent-output)
    (m/watch !agent-scroll-y)
    (m/watch !scroll-y)
    (m/watch !viewport)
    <fold-data  ;; pre-computed fold state (regions + folded set), replaces (m/watch !folded-lines)
    (m/watch !settings)
    (m/watch !active-font)
    (m/watch !current-file)
    (m/watch !flow-state)
    (m/watch !collapsed-groups)
    (m/watch !hovered-row-idx)))

;; ============================================================================
;; LAYER 7: TERMINAL RENDER CONSUMER
;; ============================================================================

(defn task-from-promise
  "Convert a JavaScript Promise to a Missionary task.
   Missionary tasks are functions that take success/failure callbacks."
  [p]
  (fn [success failure]
    (-> p
        (.then success)
        (.catch failure))
    ;; Return cancellation function (no-op for promises - they can't be cancelled)
    #()))

(defn load-font-assets [font-config]
  (let [base-path "/fonts/"
        atlas-url (str base-path (:atlas font-config))
        metrics-url (str base-path (:metrics font-config))]
    (-> (js/Promise.all
          #js [(-> (js/fetch atlas-url) (.then #(.blob %)) (.then #(js/createImageBitmap %)))
               (-> (js/fetch metrics-url) (.then #(.json %)) (.then #(js->clj % :keywordize-keys true)))])
        (.then (fn [assets]
                 {:bitmap (aget assets 0)
                  :atlas (aget assets 1)
                  :id (:id font-config)})))))

(defn start-loop!
  "Start the reactive editor loop.

   This is the main entry point. It creates the reactive flow graph and
   returns a Missionary task that runs the render loop."
  [node device ctx geometry initial-line-lengths initial-lines
   tokenize-fn layout-fn find-bracket-fn detect-folds-fn
   find-form-fn eval-form-fn atlas & {:keys [font-manifest !sidebar-visible !file-load-request initial-file]}]

  (let [;; Layout Configuration
        font-size 19
        gutter-w  40
        layout-x  (+ 50 gutter-w)
        layout-y  100
        line-h    (* font-size 1.2)
        cmd-panel-h 40
        status-bar-h 24

        ;; =====================================================================
        ;; LAYER 1: PRIMARY SOURCE ATOMS
        ;; =====================================================================

        !editor-doc (atom {:lines initial-lines
                          :cursor {:line 0 :col 0}
                          :selection nil
                          :desired-col 0})

        !cmd-panel (atom {:text "" :cursor 0 :visible true})

        !focus (atom :command-panel)

        !scroll-y (atom 0)

        !viewport (atom {:width  (.-clientWidth node)
                         :height (.-clientHeight node)
                         :dpr    (or (.-devicePixelRatio js/window) 1)})

        !folded-lines (atom #{})

        ;; Additional state atoms (not primary sources, but needed for features)
        !caret-visible (atom true)
        !clipboard (atom nil)
        !undo-stack (atom [])
        !redo-stack (atom [])
        !eval-result (atom nil)
        !dragging? (atom false)

        ;; Font configuration - use passed manifest or default
        default-manifest {:fonts [{:name "DejaVu Sans Mono"
                                   :id "dejavu-sans-mono"
                                   :charWidth 0.60
                                   :default true
                                   :defaults {:fontSize 19
                                              :lineHeight 1.2
                                              :pxRange 8
                                              :sharpness 0.0
                                              :snapToPixel true
                                              :showDiagnostics false}}]
                          :settings {:fontSize {:default 19}
                                     :lineHeight {:default 1.2}
                                     :pxRange {:default 8}
                                     :sharpness {:default 0.0}
                                     :snapToPixel {:default true}
                                     :showDiagnostics {:default false}}}
        manifest (or font-manifest default-manifest)
        fonts (or (:fonts manifest) [])
        available-fonts (filterv #(not (false? (:available %))) fonts)
        default-font (or (first (filter :default available-fonts))
                         (first available-fonts)
                         {:id "dejavu-sans-mono" :name "DejaVu Sans Mono" :charWidth 0.60})
        default-font-idx (or (first (keep-indexed (fn [idx font]
                                                    (when (= (:id font) (:id default-font)) idx))
                                                  available-fonts))
                             0)
        manifest-settings (manifest-defaults->settings (:settings manifest))
        base-settings {:visible false
                       :font-id (:id default-font)
                       :selected-index default-font-idx
                       :slider-index 0
                       :focus-section :fonts}
        initial-settings (merge base-settings
                                manifest-settings
                                (font-defaults->settings default-font))

        ;; Settings panel state
        !settings (atom initial-settings) ;; :fonts or :sliders

        !font-manifest (atom manifest)
        !active-font (atom {:id (:id default-font)
                            :char-width (or (:charWidth default-font) 0.56)
                            :name (:name default-font)})

        ;; Font assets atom - stores loaded atlas/bitmap for current font
        ;; Initial value uses the atlas passed to start-loop!
        !font-assets (atom {:atlas atlas :bitmap nil :id "dejavu-sans-mono"})

        ;; GPU state atoms (terminals update these)
        !text-geo (atom (:text geometry))
        !editor-rect-sys (atom (:rect geometry))
        !cmd-rect-sys (atom (let [capacity 16
                                  instance-buffer (.createBuffer device
                                                    (clj->js {:size (* capacity 32)
                                                              :usage (bit-or js/GPUBufferUsage.VERTEX
                                                                             js/GPUBufferUsage.COPY_DST)}))]
                              {:pipeline (:pipeline (:rect geometry))
                               :bind-group (:bind-group (:rect geometry))
                               :instance-buffer instance-buffer
                               :num-instances 0}))
        !settings-rect-sys (atom (let [capacity 32
                                        instance-buffer (.createBuffer device
                                                          (clj->js {:size (* capacity 32)
                                                                    :usage (bit-or js/GPUBufferUsage.VERTEX
                                                                                   js/GPUBufferUsage.COPY_DST)}))]
                                    {:pipeline (:pipeline (:rect geometry))
                                     :bind-group (:bind-group (:rect geometry))
                                     :instance-buffer instance-buffer
                                     :num-instances 0}))

        ;; Helper functions
        save-undo! (fn [lines cursor]
                     (swap! !undo-stack conj {:lines lines :cursor cursor})
                     (when (> (count @!undo-stack) 100)
                       (swap! !undo-stack #(vec (drop 1 %))))
                     (reset! !redo-stack []))

        apply-font-defaults! (fn [font]
                               (swap! !settings assoc :font-id (:id font))
                               (when-let [defaults (font-defaults->settings font)]
                                 (swap! !settings merge defaults)))

        ;; Watch for font changes - triggers async loading
        ;; Uses atom + watch pattern instead of m/ap to avoid cancellation issues
        _ (add-watch !active-font :font-loader
            (fn [_ _ old-val new-val]
              (when (not= (:id old-val) (:id new-val))
                (let [manifest @!font-manifest
                      font-config (first (filter #(= (:id %) (:id new-val)) (:fonts manifest)))]
                  (js/console.log "[FONT] Loading font:" (:id new-val) font-config)
                  (when font-config
                    (apply-font-defaults! font-config))
                  (when (and font-config (:atlas font-config))
                    ;; Async load - updates !font-assets when complete
                    (-> (load-font-assets font-config)
                        (.then (fn [assets]
                                 (js/console.log "[FONT] Loaded assets for:" (:id new-val))
                                 (reset! !font-assets assets)))
                        (.catch (fn [err]
                                  (js/console.error "[FONT] Failed to load:" err)))))))))

        ;; =====================================================================
        ;; SIDEBAR STATE & DOM RENDERING (imperative, avoids Electric DAG)
        ;; =====================================================================
        !selected-project (atom nil)    ;; {:name :path} or nil
        !expanded-dirs (atom #{})       ;; set of expanded dir paths
        !dir-cache (atom {})            ;; {path -> [entries]}
        !home-dirs (atom nil)           ;; cached home dirs list
        ;; V0: default to discourse-graph entry point
        !current-file (atom {:path "/home/sid/projects/discourse-graph/apps/roam/src/index.ts"
                             :name "index.ts"})
        !sidebar-mode (atom :files)     ;; :files | :review-packs
        !review-pack-list (atom nil)    ;; nil = not loaded yet
        !review-pack-loading? (atom false)
        !review-pack-selected-id (atom nil)
        !review-pack-selected-node-id (atom nil)
        !review-pack-summary-cache (atom {})
        !review-pack-load-error (atom nil)
        !ai-provider (atom :claude)     ;; :claude | :codex | :gemini
        !agent-output (atom nil)        ;; {:status :provider :prompt :output :run-id :trail :tool-buf}
        !agent-scroll-y (atom 0)        ;; scroll offset within agent output panel
        !mouse-y (atom 0)               ;; last known mouse Y (viewport-relative)
        !flow-state (atom (initial-flow-state))  ;; V0 flow state machine
        !collapsed-groups (atom #{})           ;; set of collapsed group status strings
        !hovered-row-idx (atom nil)            ;; 0-based flat ticket index under mouse cursor

        sidebar-el (js/document.getElementById "file-sidebar")

        ;; --- Fetch helpers (call server HTTP API, parse EDN response) ---

        fetch-edn!
        (fn
          ([url callback]
           (-> (js/fetch url)
               (.then (fn [resp] (.text resp)))
               (.then (fn [text] (callback (reader/read-string text))))
               (.catch (fn [err] (js/console.error "[SIDEBAR] Fetch error:" err)))))
          ([url callback err-callback]
           (-> (js/fetch url)
               (.then (fn [resp] (.text resp)))
               (.then (fn [text] (callback (reader/read-string text))))
               (.catch (fn [err] (err-callback err))))))

        post-edn!
        (fn [url body callback]
          (-> (js/fetch url
                        (clj->js {:method "POST"
                                  :headers {"Content-Type" "application/edn"}
                                  :body (pr-str body)}))
              (.then (fn [resp] (.text resp)))
              (.then (fn [text] (callback (reader/read-string text))))
              (.catch (fn [err]
                        (js/console.error "[AGENT][HTTP][POST-ERROR]"
                                          (clj->js {:url url
                                                    :message (.-message err)})
                                          err))))

          nil)

        fetch-home-dirs!
        (fn [render-fn]
          (if @!home-dirs
            (render-fn)
            (fetch-edn! "/api/home-dirs"
                        (fn [dirs]
                          (reset! !home-dirs dirs)
                          (render-fn)))))

        fetch-dir!
        (fn [path render-fn]
          (if (contains? @!dir-cache path)
            (render-fn)
            (fetch-edn! (str "/api/list-dir?path=" (js/encodeURIComponent path))
                        (fn [entries]
                          (swap! !dir-cache assoc path entries)
                          (render-fn)))))

        fetch-file!
        (fn [path root-path]
          (fetch-edn! (str "/api/read-file?path=" (js/encodeURIComponent path)
                           "&root=" (js/encodeURIComponent root-path))
                      (fn [result]
                        (if (:error result)
                          (js/console.error "[SIDEBAR] File read error:" (:error result))
                          (let [lines (str/split-lines (:content result))]
                            (reset! !current-file {:path path :name (last (str/split path #"/"))})
                            (reset! !file-load-request {:lines lines}))))))

        compact-text
        (fn [s max-len]
          (let [txt (or (some-> s str str/trim) "")]
            (if (> (count txt) max-len)
              (str (subs txt 0 max-len) "...")
              txt)))

        build-review-pack-canvas-model
        (fn [pack]
          (let [claims (vec (or (:claims pack) []))
                evidence (vec (get-in pack [:sections :evidence]))
                evidence-by-claim (group-by :claim-id evidence)
                decisions (vec (get-in pack [:sections :decisions]))
                risks (vec (get-in pack [:sections :risks-unknowns]))
                nodes (atom [{:id "root"
                              :kind :question
                              :x 24 :y 18 :w 210 :h 72
                              :title (or (:issue-ref pack) (:pack-id pack) "Thread")
                              :subtitle (compact-text (get-in pack [:sections :intent]) 78)
                              :payload {:kind :question :pack pack}}])
                edges (atom [])
                !cursor-y (atom 118)]
            (doseq [[i claim] (map-indexed vector claims)]
              (let [claim-id (str "claim:" (or (:id claim) i))
                    y @!cursor-y
                    claim-evidence (vec (get evidence-by-claim (:id claim)))
                    claim-h 66]
                (swap! nodes conj
                       {:id claim-id
                        :kind :claim
                        :x 42 :y y :w 232 :h claim-h
                        :title (str "Claim " (inc i))
                        :subtitle (compact-text (:text claim) 88)
                        :payload {:kind :claim :claim claim}})
                (swap! edges conj {:from "root" :to claim-id})
                (if (seq claim-evidence)
                  (do
                    (doseq [[j anchor] (map-indexed vector claim-evidence)]
                      (let [anchor-id (str "evidence:" (:id anchor))
                            ey (+ y (* j 72))
                            anchor-title (or (some-> (:kind anchor) name str/upper-case) "EVIDENCE")
                            anchor-subtitle (compact-text
                                              (or (:file-path anchor) (:snippet anchor) "anchor")
                                              70)]
                        (swap! nodes conj
                               {:id anchor-id
                                :kind :evidence
                                :x 304 :y ey :w 208 :h 62
                                :title anchor-title
                                :subtitle anchor-subtitle
                                :payload {:kind :evidence :anchor anchor}})
                        (swap! edges conj {:from claim-id :to anchor-id})))
                    (reset! !cursor-y (+ y (max 108 (* (count claim-evidence) 72)))))
                  (reset! !cursor-y (+ y 98)))))

            (when (seq decisions)
              (let [decisions-id "decisions"
                    y @!cursor-y]
                (swap! nodes conj
                       {:id decisions-id
                        :kind :decision
                        :x 42 :y y :w 232 :h 62
                        :title "Decisions"
                        :subtitle (str (count decisions) " recorded")
                        :payload {:kind :decision-group :decisions decisions}})
                (swap! edges conj {:from "root" :to decisions-id})
                (doseq [[i d] (map-indexed vector decisions)]
                  (let [did (str "decision:" (or (:id d) i))
                        dy (+ y (* i 66))
                        status (some-> (:status d) name str/upper-case)]
                    (swap! nodes conj
                           {:id did
                            :kind :decision
                            :x 304 :y dy :w 208 :h 58
                            :title (or status "DECISION")
                            :subtitle (compact-text (or (:summary d) (:rationale d) "") 70)
                            :payload {:kind :decision :decision d}})
                    (swap! edges conj {:from decisions-id :to did})))
                (reset! !cursor-y (+ y (max 96 (* (count decisions) 66))))))

            (when (seq risks)
              (let [risks-id "risks"
                    y @!cursor-y]
                (swap! nodes conj
                       {:id risks-id
                        :kind :risk
                        :x 42 :y y :w 232 :h 62
                        :title "Risks & Unknowns"
                        :subtitle (str (count risks) " open items")
                        :payload {:kind :risk-group :risks risks}})
                (swap! edges conj {:from "root" :to risks-id})
                (doseq [[i r] (map-indexed vector risks)]
                  (let [rid (str "risk:" (or (:id r) i))
                        ry (+ y (* i 66))
                        rk (some-> (:kind r) name str/upper-case)
                        sev (some-> (:severity r) name str/upper-case)]
                    (swap! nodes conj
                           {:id rid
                            :kind :risk
                            :x 304 :y ry :w 208 :h 58
                            :title (str (or rk "RISK")
                                        (when sev (str " • " sev)))
                            :subtitle (compact-text (:text r) 70)
                            :payload {:kind :risk :risk r}})
                    (swap! edges conj {:from risks-id :to rid})))
                (reset! !cursor-y (+ y (max 96 (* (count risks) 66))))))

            {:nodes @nodes
             :edges @edges
             :height (+ @!cursor-y 120)}))

        node-by-id
        (fn [nodes node-id]
          (first (filter #(= (:id %) node-id) nodes)))

        fetch-review-packs!
        (fn [render-fn]
          (when-not @!review-pack-loading?
            (reset! !review-pack-loading? true)
            (fetch-edn!
              "/api/review-pack/list"
              (fn [result]
                (reset! !review-pack-loading? false)
                (if (:ok result)
                  (let [packs (vec (or (:review-packs result) []))
                        selected-id @!review-pack-selected-id
                        selected-exists? (some #(= (:pack-id %) selected-id) packs)]
                    (reset! !review-pack-load-error nil)
                    (reset! !review-pack-list packs)
                    (cond
                      (and (seq packs) (nil? selected-id))
                      (do (reset! !review-pack-selected-id (:pack-id (first packs)))
                          (reset! !review-pack-selected-node-id nil))

                      (and (seq packs) (not selected-exists?))
                      (do (reset! !review-pack-selected-id (:pack-id (first packs)))
                          (reset! !review-pack-selected-node-id nil))

                      (empty? packs)
                      (do (reset! !review-pack-selected-id nil)
                          (reset! !review-pack-selected-node-id nil)))
                    (render-fn))
                  (do
                    (reset! !review-pack-load-error (or (:message result) "Failed to load review packs"))
                    (reset! !review-pack-list [])
                    (reset! !review-pack-selected-id nil)
                    (reset! !review-pack-selected-node-id nil)
                    (render-fn))))
              (fn [err]
                (reset! !review-pack-loading? false)
                (reset! !review-pack-load-error (str "Fetch failed: " (.-message err)))
                (reset! !review-pack-list [])
                (reset! !review-pack-selected-id nil)
                (reset! !review-pack-selected-node-id nil)
                (render-fn)))))

        fetch-review-pack-summary!
        (fn [pack-id render-fn]
          (when (and pack-id
                     (not (contains? @!review-pack-summary-cache pack-id)))
            (fetch-edn!
              (str "/api/review-pack/" (js/encodeURIComponent pack-id) "/summary")
              (fn [result]
                (if (:ok result)
                  (swap! !review-pack-summary-cache assoc pack-id result)
                  (swap! !review-pack-summary-cache assoc pack-id
                         {:ok false
                          :message (or (:message result) "Failed to load review pack summary")}))
                (render-fn))
              (fn [err]
                (swap! !review-pack-summary-cache assoc pack-id
                       {:ok false
                        :message (str "Fetch failed: " (.-message err))})
                (render-fn)))))

        trigger-dev-replay!
        (fn []
          (js/console.log "[DEV] Triggering replay fixture...")
          (reset! !agent-scroll-y 0)
          (reset! !agent-output {:status :running
                                 :provider :claude
                                 :prompt "Replay Fixture"
                                 :output ""
                                 :run-id "replay-dev"
                                 :trail []
                                 :tool-buf {}})
          (-> (js/fetch "/api/dev/replay-fixture")
              (.then (fn [resp] (.json resp)))
              (.then (fn [json]
                       (let [data (js->clj json :keywordize-keys true)]
                         (if (:ok data)
                           (let [events (:events data)]
                             (doseq [[i evt] (map-indexed vector events)]
                               (js/setTimeout
                                 (fn []
                                   (let [kind (:event evt)]
                                     (case kind
                                       :text-delta
                                       (do
                                         (swap! !agent-output
                                           (fn [ao]
                                             (-> ao
                                               (update :output str (:text evt))
                                               (update :trail conj {:kind :reasoning :text (:text evt)}))))
                                         (let [ao @!agent-output
                                               viewport @!viewport
                                               settings @!settings
                                               font-size (:font-size settings)
                                               char-advance (* font-size (:char-width @!active-font))
                                               agent-h (compute-agent-panel-h ao font-size (:height viewport) (:width viewport) char-advance)
                                               line-step (* font-size 1.2)
                                               max-chars (if (pos? char-advance)
                                                           (max 1 (int (/ (- (:width viewport) 48) char-advance)))
                                                           80)
                                               line-count (agent-wrapped-line-count ao max-chars)
                                               total-h (* line-count line-step)
                                               max-scroll (max 0 (- total-h (- agent-h 16)))]
                                           (reset! !agent-scroll-y max-scroll)))

                                       :tool-use-start
                                       (swap! !agent-output
                                         (fn [ao]
                                           (-> ao
                                             (assoc-in [:tool-buf (:tool-id evt)]
                                               {:tool-name (:tool-name evt) :json "" :block-idx (:block-idx evt)})
                                             (update :trail conj {:kind :tool-call-start
                                                                  :tool-name (:tool-name evt)
                                                                  :tool-id (:tool-id evt)
                                                                  :block-idx (:block-idx evt)}))))

                                       :tool-input-delta
                                       (if-let [tid (:tool-id evt)]
                                         (swap! !agent-output
                                           (fn [ao]
                                             (update-in ao [:tool-buf tid :json] str (:json-chunk evt))))
                                         (js/console.warn "[DEV][REPLAY] :tool-input-delta missing :tool-id" (clj->js evt)))

                                       :tool-result
                                       (swap! !agent-output
                                         (fn [ao]
                                           (update ao :trail conj {:kind :tool-result
                                                                   :tool-id (:tool-id evt)
                                                                   :content (:content evt)})))

                                       :block-stop
                                       (let [ao @!agent-output
                                             matching-tool (some (fn [[tid buf]]
                                                                   (when (= (:block-idx buf) (:block-idx evt))
                                                                     [tid buf]))
                                                                 (:tool-buf ao))]
                                         (when matching-tool
                                           (let [[tid buf] matching-tool
                                                 parsed-input (try (js/JSON.parse (:json buf))
                                                                   (catch :default _ nil))]
                                             (swap! !agent-output
                                               (fn [ao]
                                                 (-> ao
                                                   (update :trail conj {:kind :tool-call
                                                                        :tool-name (:tool-name buf)
                                                                        :tool-id tid
                                                                        :input (js->clj parsed-input :keywordize-keys true)
                                                                        :block-idx (:block-idx evt)})
                                                   (update :tool-buf dissoc tid)))))))

                                       (:result :run-done)
                                       (swap! !agent-output assoc :status :complete)

                                       :run-error
                                       (swap! !agent-output assoc :status :failed)

                                       ;; Unknown — surface
                                       (js/console.warn "[DEV][UNKNOWN-EVENT]" (clj->js evt)))))
                                 (* i 50))))
                           (js/console.error "[DEV] Replay failed:" (:error data))))))
              (.catch (fn [err] (js/console.error "[DEV] Replay fetch error:" err)))))

        ;; =====================================================================
        ;; EXTRACTED EVENT HANDLER (DRY — used by replay, submit, and flow runs)
        ;; =====================================================================

        auto-scroll-agent!
        (fn []
          (let [ao @!agent-output
                viewport @!viewport
                settings @!settings
                font-size (:font-size settings)
                char-advance (* font-size (:char-width @!active-font))
                agent-h (compute-agent-panel-h ao font-size (:height viewport)
                                               (:width viewport) char-advance)
                line-step (* font-size 1.2)
                max-chars (if (pos? char-advance)
                            (max 1 (int (/ (- (:width viewport) 48) char-advance)))
                            80)
                line-count (agent-wrapped-line-count ao max-chars)
                total-h (* line-count line-step)
                max-scroll (max 0 (- total-h (- agent-h 16)))]
            (reset! !agent-scroll-y max-scroll)))

        make-event-handler
        (fn [run-id on-done-fn]
          (fn [evt]
            (let [kind (:event evt)]
              (case kind
                :text-delta
                (do (swap! !agent-output
                      (fn [ao]
                        (-> ao
                          (update :output str (:text evt))
                          (update :trail conj {:kind :reasoning :text (:text evt)}))))
                    (auto-scroll-agent!))

                :tool-use-start
                (swap! !agent-output
                  (fn [ao]
                    (-> ao
                      (assoc-in [:tool-buf (:tool-id evt)]
                        {:tool-name (:tool-name evt) :json "" :block-idx (:block-idx evt)})
                      (update :trail conj {:kind :tool-call-start
                                           :tool-name (:tool-name evt)
                                           :tool-id (:tool-id evt)
                                           :block-idx (:block-idx evt)}))))

                :tool-input-delta
                (if-let [tid (:tool-id evt)]
                  (swap! !agent-output
                    (fn [ao]
                      (update-in ao [:tool-buf tid :json] str (:json-chunk evt))))
                  (js/console.warn "[AGENT] :tool-input-delta missing :tool-id" (clj->js evt)))

                :tool-result
                (swap! !agent-output
                  (fn [ao]
                    (update ao :trail conj {:kind :tool-result
                                            :tool-id (:tool-id evt)
                                            :content (:content evt)})))

                :block-stop
                (let [ao @!agent-output
                      matching-tool (some (fn [[tid buf]]
                                            (when (= (:block-idx buf) (:block-idx evt))
                                              [tid buf]))
                                          (:tool-buf ao))]
                  (when matching-tool
                    (let [[tid buf] matching-tool
                          parsed-input (try (js/JSON.parse (:json buf))
                                            (catch :default _ nil))]
                      (swap! !agent-output
                        (fn [ao]
                          (-> ao
                            (update :trail conj {:kind :tool-call
                                                 :tool-name (:tool-name buf)
                                                 :tool-id tid
                                                 :input (js->clj parsed-input :keywordize-keys true)
                                                 :block-idx (:block-idx evt)})
                            (update :tool-buf dissoc tid)))))))

                (:done :run-done)
                (do (swap! !agent-output (fn [ao]
                      (cond-> (assoc ao :status (or (:status evt) :complete))
                        (:result evt) (assoc :structured-result (:result evt)))))
                    (js/console.log "[AGENT][DONE]" (clj->js {:run-id run-id
                                                               :status (:status evt)
                                                               :has-result (some? (:result evt))}))
                    (when on-done-fn (on-done-fn)))

                (:start :run-start)
                (do (js/console.log "[AGENT][STREAM-START]" (clj->js evt))
                    ;; Capture session-id into flow state for --resume continuity
                    (when-let [sid (:session-id evt)]
                      (swap! !flow-state assoc :session-id sid)))

                :init
                (do (js/console.log "[AGENT][INIT]" (clj->js evt))
                    (when-let [sid (:session-id evt)]
                      (swap! !flow-state assoc :session-id sid)))

                :result
                (do (js/console.log "[AGENT][RESULT] session-id:" (:session-id evt))
                    (when-let [sid (:session-id evt)]
                      (swap! !flow-state assoc :session-id sid)))

                :run-error
                (do (swap! !agent-output assoc :status :failed)
                    (js/console.error "[AGENT][RUN-ERROR]" (clj->js evt))
                    (when on-done-fn (on-done-fn)))

                ;; Unknown — surface, don't silently drop
                (js/console.warn "[AGENT][UNKNOWN-EVENT]" (clj->js evt))))))

        ;; =====================================================================
        ;; FLOW RUN HELPER (compose prompt → stream)
        ;; =====================================================================

        ;; V0: hardcoded to discourse-graph (per commission-consensus.md scope)
        flow-cwd "/home/sid/projects/discourse-graph"
        ;; Read-only Linear MCP tools pre-approved for non-interactive (-p) mode
        flow-allowed-tools ["mcp__linear-server__list_issues"
                            "mcp__linear-server__get_issue"
                            "mcp__linear-server__search_issues"
                            "mcp__linear-server__list_projects"
                            "mcp__linear-server__get_project"
                            "mcp__linear-server__list_teams"]

        fire-flow-run!
        (fn [prompt-action & {:keys [on-done json-schema max-budget-usd model append-system-prompt]}]
          (let [flow @!flow-state
                {:keys [prompt]} (flow-prompt prompt-action flow)
                provider @!ai-provider
                cwd flow-cwd
                run-id (str (random-uuid))
                session-id (:session-id flow)
                request-body (cond-> {:run-id run-id
                                      :provider provider
                                      :prompt prompt
                                      :cwd cwd
                                      :allowed-tools flow-allowed-tools
                                      :context {:timestamp (js/Date.now)}}
                               session-id          (assoc :session-id session-id)
                               json-schema         (assoc :json-schema json-schema)
                               max-budget-usd      (assoc :max-budget-usd max-budget-usd)
                               model               (assoc :model model)
                               append-system-prompt (assoc :append-system-prompt append-system-prompt))]
            (js/console.log "[FLOW][FIRE]" (clj->js {:action prompt-action
                                                      :node (:node flow)
                                                      :run-id run-id
                                                      :session-id session-id}))
            (reset! !agent-scroll-y 0)
            (reset! !agent-output {:status :running
                                   :provider provider
                                   :prompt (str "[" (name prompt-action) "]")
                                   :output ""
                                   :run-id run-id
                                   :trail []
                                   :tool-buf {}})
            (stream-agent-run!
              "/api/agent/stream"
              request-body
              (make-event-handler run-id on-done)
              (fn [err]
                (js/console.error "[FLOW][STREAM-ERROR]" err)
                (reset! !agent-output {:status :failed
                                       :provider provider
                                       :prompt (str "[" (name prompt-action) "]")
                                       :output (str "Stream error: " (.-message err))
                                       :run-id run-id
                                       :trail []
                                       :tool-buf {}})
                (when on-done (on-done))))))

        show-flow-info!
        (fn [msg]
          (reset! !agent-scroll-y 0)
          (reset! !agent-output {:status :complete
                                 :provider @!ai-provider
                                 :prompt "flow"
                                 :output msg
                                 :run-id nil}))

        submit-agent-run!
        (fn [cmd-text]
          (let [doc @!editor-doc
                file-path (:path @!current-file)
                scroll-y @!scroll-y
                viewport @!viewport
                parsed (parse-agent-command cmd-text @!ai-provider)
                cwd (or (some-> file-path (str/split #"/") butlast seq (str/join "/"))
                        ".")
                context {:cursor (:cursor doc)
                         :selection (:selection doc)
                         :visible-range [scroll-y (+ scroll-y (:height viewport))]
                         :file-path file-path
                         :timestamp (js/Date.now)}]
            (case (:kind parsed)
              :noop
              nil

              :replay
              (trigger-dev-replay!)

              :set-provider
              (do
                (reset! !ai-provider (:provider parsed))
                (reset! !agent-scroll-y 0)
                (reset! !agent-output {:status :complete
                                       :provider (:provider parsed)
                                       :prompt "provider"
                                       :output (str "Provider set to " (-> (:provider parsed) name str/upper-case))
                                       :run-id nil}))

              :error
              (do (reset! !agent-scroll-y 0)
                  (reset! !agent-output {:status :failed
                                          :provider @!ai-provider
                                          :prompt cmd-text
                                          :output (:message parsed)
                                          :run-id nil}))

              ;; === Regular prompt run (refactored to use make-event-handler) ===
              :run
              (let [provider (:provider parsed)
                    prompt (:prompt parsed)
                    argv (:argv parsed)
                    run-id (str (random-uuid))
                    request-body (cond-> {:run-id run-id
                                          :provider provider
                                          :prompt prompt
                                          :cwd cwd
                                          :file file-path
                                          :context context}
                                   (seq argv) (assoc :argv argv)
                                   (:session-id @!flow-state) (assoc :session-id (:session-id @!flow-state)))]
                (js/console.log "[AGENT][CLIENT][SUBMIT]"
                                (clj->js {:run-id run-id
                                          :provider provider
                                          :prompt prompt}))
                (reset! !agent-scroll-y 0)
                (reset! !agent-output {:status :running
                                       :provider provider
                                       :prompt prompt
                                       :output ""
                                       :run-id run-id
                                       :trail []
                                       :tool-buf {}})
                (stream-agent-run!
                  "/api/agent/stream"
                  request-body
                  (make-event-handler run-id nil)
                  (fn [err]
                    (js/console.error "[AGENT][CLIENT][STREAM-ERROR]" err)
                    (reset! !agent-output {:status :failed
                                           :provider provider
                                           :prompt prompt
                                           :output (str "Stream error: " (.-message err))
                                           :run-id run-id
                                           :trail []
                                           :tool-buf {}}))))

              ;; === Flow commands (V0 state machine) ===

              :flow-bootstrap
              (let [flow @!flow-state
                    next (or (transition-flow-state flow :bootstrapping)
                             ;; Allow re-bootstrap from :intake too
                             (when (= (:node flow) :intake)
                               (transition-flow-state flow :bootstrapping)))]
                (if next
                  (do (reset! !flow-state next)
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
                                       (do (swap! !flow-state assoc
                                                  :node :intake
                                                  :tickets tickets)
                                           (reset! !scroll-y 0)
                                           (show-flow-info!
                                             (str "Bootstrap complete. " (count tickets) " tickets loaded.")))
                                       ;; Failed — stay in bootstrapping for retry
                                       (do (swap! !flow-state assoc :node :bootstrapping)
                                           (show-flow-info!
                                             (str "Bootstrap failed: " (or (:error data) "no tickets found") "\nUse /bootstrap to retry.")))))))
                          (.catch (fn [err]
                                    (swap! !flow-state assoc :node :bootstrapping)
                                    (show-flow-info!
                                      (str "Bootstrap fetch error: " (.-message err) "\nUse /bootstrap to retry."))))))
                  (show-flow-info! (str "Cannot bootstrap from state: " (name (:node flow)) "\nUse /reset to return to idle."))))


              :flow-select
              (let [flow @!flow-state
                    indices (:indices parsed)
                    tickets (:tickets flow)]
                (if (not= (:node flow) :intake)
                  (show-flow-info! (str "Cannot select tickets in state: " (name (:node flow))
                                        "\nMust be in :intake state."))
                  (let [invalid (filter #(or (neg? %) (>= % (count tickets))) indices)]
                    (if (seq invalid)
                      (show-flow-info! (str "Invalid ticket numbers: "
                                            (str/join ", " (map inc invalid))
                                            "\nValid range: 1-" (count tickets)))
                      (do (swap! !flow-state assoc :selected (vec indices))
                          (show-flow-info!
                            (str "Selected " (count indices) " ticket(s):\n"
                                 (str/join "\n" (map (fn [i]
                                                       (let [t (nth tickets i)]
                                                         (str "  " (inc i) ". " (:id t) " — " (:title t))))
                                                     indices))
                                 "\n\nUse /arrange sequential|parallel to set execution mode.")))))))

              :flow-arrange
              (let [flow @!flow-state
                    mode (:mode parsed)]
                (if (empty? (:selected flow))
                  (show-flow-info! "No tickets selected. Use /select first.")
                  (let [next (transition-flow-state flow :arrange {:arrangement mode})]
                    (if next
                      (do (reset! !flow-state next)
                          (show-flow-info!
                            (str "Arrangement set to: " (name mode)
                                 "\n" (count (:selected flow)) " ticket(s) ready."
                                 "\n\nUse /run-flow to start execution.")))
                      (show-flow-info! (str "Cannot arrange from state: " (name (:node flow))))))))

              :flow-run
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
                  (show-flow-info! (str "Cannot run from state: " (name (:node flow))
                                        "\nExpected :arrange. Current: " (name (:node flow))))))

              :flow-review
              (show-flow-info!
                (let [flow @!flow-state]
                  (str "Current state: " (name (:node flow))
                       (when (= (:node flow) :review)
                         "\n\nOptions:\n  /rework <comment> — request changes\n  /finalize — wrap up batch"))))

              :flow-rework
              (let [flow @!flow-state
                    next (transition-flow-state flow :rework {:rework-comment (:comment parsed)})]
                (if next
                  (do (reset! !flow-state next)
                      (fire-flow-run! :rework
                        :on-done (fn []
                                   (swap! !flow-state assoc :node :review)
                                   (show-flow-info!
                                     (str "Rework complete. Back in review.\n\n"
                                          "Use /rework <comment> for more changes,\n"
                                          "or /finalize to wrap up.")))))
                  (show-flow-info! (str "Cannot rework from state: " (name (:node flow))
                                        "\nExpected :review. Current: " (name (:node flow))))))

              :flow-finalize
              (let [flow @!flow-state
                    next (transition-flow-state flow :finalize)]
                (if (or next (= (:node flow) :review))
                  (do (reset! !flow-state (or next (assoc flow :node :finalize
                                                          :history (conj (:history flow) (:node flow)))))
                      (fire-flow-run! :finalize
                        :on-done (fn []
                                   (swap! !flow-state assoc
                                          :node :intake
                                          :selected []
                                          :arrangement nil)
                                   (reset! !scroll-y 0) ;; reset scroll on mode switch
                                   (show-flow-info!
                                     (str "Batch finalized. Returned to intake.\n\n"
                                          "Tickets still loaded. Use /select to start a new batch,\n"
                                          "or /bootstrap to refresh tickets.")))))
                  (show-flow-info! (str "Cannot finalize from state: " (name (:node flow))
                                        "\nExpected :review. Current: " (name (:node flow))))))

              :flow-status
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

              :flow-reset
              (do (reset! !flow-state (initial-flow-state))
                  (reset! !scroll-y 0) ;; reset scroll on mode switch
                  (show-flow-info! "Flow state reset to idle.\nUse /bootstrap to start fresh.")))))

        render-sidebar!
        (fn render-sidebar! []
          (when sidebar-el
            (let [visible? (and !sidebar-visible @!sidebar-visible)
                  mode @!sidebar-mode
                  project @!selected-project
                  expanded @!expanded-dirs
                  cache @!dir-cache
                  review-packs @!review-pack-list
                  review-pack-selected-id @!review-pack-selected-id
                  review-pack-summary-cache @!review-pack-summary-cache
                  review-pack-load-error @!review-pack-load-error]
              ;; Toggle visibility
              (set! (.. sidebar-el -style -display) (if visible? "block" "none"))
              (let [target-w (if (= mode :review-packs) 760 250)]
                (set! (.. sidebar-el -style -width) (str target-w "px"))
                (set! (.. sidebar-el -style -minWidth) (str target-w "px")))
              (when visible?
                ;; Clear content
                (set! (.-innerHTML sidebar-el) "")
                ;; Mode selector
                (let [mode-row (js/document.createElement "div")]
                  (set! (.-cssText (.-style mode-row))
                        "display:flex;gap:6px;padding:8px 10px;border-bottom:1px solid #2a2a4a;")
                  (doseq [[mode-k mode-label] [[:files "Files"] [:review-packs "Review Packs"]]]
                    (let [btn (js/document.createElement "button")
                          active? (= mode mode-k)]
                      (set! (.-textContent btn) mode-label)
                      (set! (.-cssText (.-style btn))
                            (str "flex:1;border:1px solid #3a3a5a;border-radius:6px;padding:4px 6px;cursor:pointer;font-size:11px;"
                                 (if active?
                                   "background:#334066;color:#e4e8ff;"
                                   "background:#202038;color:#9a9abf;")))
                      (set! (.-onclick btn)
                            (fn [_]
                              (when (not= @!sidebar-mode mode-k)
                                (reset! !sidebar-mode mode-k)
                                (when (= mode-k :review-packs)
                                  (when (nil? @!review-pack-list)
                                    (fetch-review-packs! render-sidebar!)))
                                (render-sidebar!))))
                      (.appendChild mode-row btn)))
                  (.appendChild sidebar-el mode-row))

                (if (= mode :review-packs)
                  ;; REVIEW PACKS MODE
                  (do
                    (let [header-row (js/document.createElement "div")
                          title (js/document.createElement "div")
                          refresh-btn (js/document.createElement "div")]
                      (set! (.-cssText (.-style header-row))
                            "display:flex;align-items:center;justify-content:space-between;padding:8px 12px;border-bottom:1px solid #2a2a4a;")
                      (set! (.-textContent title) "REVIEW PACKS")
                      (set! (.-cssText (.-style title))
                            "font-size:11px;text-transform:uppercase;letter-spacing:0.05em;color:#7878a0;")
                      (set! (.-textContent refresh-btn) "refresh")
                      (set! (.-cssText (.-style refresh-btn))
                            "font-size:11px;cursor:pointer;color:#8ea0ff;")
                      (set! (.-onclick refresh-btn)
                            (fn [_]
                              (reset! !review-pack-list nil)
                              (reset! !review-pack-load-error nil)
                              (fetch-review-packs! render-sidebar!)))
                      (.appendChild header-row title)
                      (.appendChild header-row refresh-btn)
                      (.appendChild sidebar-el header-row))

                    (cond
                      review-pack-load-error
                      (let [msg (js/document.createElement "div")]
                        (set! (.-textContent msg) review-pack-load-error)
                        (set! (.-cssText (.-style msg))
                              "padding:10px 12px;color:#ff8c8c;font-size:12px;")
                        (.appendChild sidebar-el msg))

                      (nil? review-packs)
                      (let [loading (js/document.createElement "div")]
                        (set! (.-textContent loading) "Loading review packs...")
                        (set! (.-cssText (.-style loading))
                              "padding:10px 12px;color:#7878a0;font-style:italic;font-size:12px;")
                        (.appendChild sidebar-el loading)
                        (fetch-review-packs! render-sidebar!))

                      (empty? review-packs)
                      (let [empty-el (js/document.createElement "div")]
                        (set! (.-textContent empty-el) "No review packs yet.")
                        (set! (.-cssText (.-style empty-el))
                              "padding:10px 12px;color:#7878a0;font-size:12px;")
                        (.appendChild sidebar-el empty-el))

                      :else
                      (let [selected-pack (some #(when (= (:pack-id %) review-pack-selected-id) %) review-packs)
                            summary-result (get review-pack-summary-cache review-pack-selected-id)
                            summary (:summary summary-result)
                            model (when selected-pack (build-review-pack-canvas-model selected-pack))
                            nodes (vec (:nodes model))
                            edges (vec (:edges model))
                            node-index (reduce (fn [m n] (assoc m (:id n) n)) {} nodes)
                            selected-node-id (or @!review-pack-selected-node-id "root")
                            selected-node (or (node-by-id nodes selected-node-id) (first nodes))
                            workspace (js/document.createElement "div")
                            list-col (js/document.createElement "div")
                            right-col (js/document.createElement "div")]
                        (when (and selected-pack (nil? summary-result))
                          (fetch-review-pack-summary! review-pack-selected-id render-sidebar!))
                        (when (and selected-pack
                                   (or (nil? @!review-pack-selected-node-id)
                                       (nil? (node-by-id nodes @!review-pack-selected-node-id))))
                          (reset! !review-pack-selected-node-id "root"))

                        (set! (.-cssText (.-style workspace))
                              "display:grid;grid-template-columns:240px 1fr;gap:10px;padding:10px;min-height:600px;")

                        ;; Left list column
                        (set! (.-cssText (.-style list-col))
                              "border:1px solid #2b2f49;border-radius:8px;background:#14182a;overflow-y:auto;max-height:650px;")
                        (doseq [pack review-packs]
                          (let [pack-id (:pack-id pack)
                                selected? (= pack-id review-pack-selected-id)
                                row (js/document.createElement "div")
                                status (some-> (:status pack) name str/upper-case)
                                issue (or (:issue-ref pack) "NO-ISSUE")
                                line (str issue " [" (or status "DRAFT") "]")]
                            (set! (.-textContent row) line)
                            (set! (.-cssText (.-style row))
                                  (str "padding:8px 10px;cursor:pointer;font-size:12px;line-height:1.35;border-bottom:1px solid #222640;"
                                       (if selected?
                                         "background:#2a3355;color:#ecf0ff;border-left:3px solid #66a2ff;"
                                         "background:transparent;color:#c7cbe3;border-left:3px solid transparent;")))
                            (set! (.-onclick row)
                                  (fn [_]
                                    (reset! !review-pack-selected-id pack-id)
                                    (reset! !review-pack-selected-node-id "root")
                                    (fetch-review-pack-summary! pack-id render-sidebar!)))
                            (.appendChild list-col row)))

                        ;; Right canvas + inspector column
                        (set! (.-cssText (.-style right-col))
                              "display:grid;grid-template-rows:auto 1fr;gap:8px;")
                        (if selected-pack
                          (let [toolbar (js/document.createElement "div")
                                body (js/document.createElement "div")
                                canvas-pane (js/document.createElement "div")
                                inspector-pane (js/document.createElement "div")
                                canvas-surface (js/document.createElement "div")]
                            (set! (.-textContent toolbar)
                                  (str "THREAD CANVAS  •  "
                                       (or (:issue-ref selected-pack) (:pack-id selected-pack))
                                       "  •  "
                                       (count nodes) " nodes"))
                            (set! (.-cssText (.-style toolbar))
                                  "padding:8px 10px;border:1px solid #2b2f49;border-radius:8px;background:#151b30;color:#a8b0d8;font-size:12px;")
                            (.appendChild right-col toolbar)

                            (set! (.-cssText (.-style body))
                                  "display:grid;grid-template-columns:1fr 300px;gap:8px;min-height:540px;")
                            (set! (.-cssText (.-style canvas-pane))
                                  "border:1px solid #2b2f49;border-radius:8px;background:#0f1322;overflow:auto;position:relative;")
                            (set! (.-cssText (.-style canvas-surface))
                                  (str "position:relative;width:560px;height:"
                                       (max 560 (:height model 560))
                                       "px;"))

                            ;; Draw edges first.
                            (doseq [edge edges]
                              (let [from (get node-index (:from edge))
                                    to (get node-index (:to edge))]
                                (when (and from to)
                                  (let [x1 (+ (:x from) (:w from))
                                        y1 (+ (:y from) (int (/ (:h from) 2)))
                                        x2 (:x to)
                                        y2 (+ (:y to) (int (/ (:h to) 2)))
                                        mid-x (+ x1 (max 18 (int (/ (- x2 x1) 2))))
                                        seg1 (js/document.createElement "div")
                                        seg2 (js/document.createElement "div")
                                        seg3 (js/document.createElement "div")
                                        min-y (min y1 y2)
                                        v-h (max 1 (js/Math.abs (- y2 y1)))]
                                    (set! (.-cssText (.-style seg1))
                                          (str "position:absolute;left:" x1 "px;top:" y1 "px;width:" (max 1 (- mid-x x1)) "px;height:1px;background:#355087;"))
                                    (set! (.-cssText (.-style seg2))
                                          (str "position:absolute;left:" mid-x "px;top:" min-y "px;width:1px;height:" v-h "px;background:#355087;"))
                                    (set! (.-cssText (.-style seg3))
                                          (str "position:absolute;left:" mid-x "px;top:" y2 "px;width:" (max 1 (- x2 mid-x)) "px;height:1px;background:#355087;"))
                                    (.appendChild canvas-surface seg1)
                                    (.appendChild canvas-surface seg2)
                                    (.appendChild canvas-surface seg3)))))

                            ;; Draw nodes.
                            (doseq [node nodes]
                              (let [node-el (js/document.createElement "div")
                                    subtitle-el (js/document.createElement "div")
                                    selected? (= (:id node) (:id selected-node))
                                    accent (case (:kind node)
                                             :question "#6ea8ff"
                                             :claim "#69d8a6"
                                             :evidence "#f2ca74"
                                             :decision "#ff9d70"
                                             :risk "#ff7885"
                                             "#8fa1d8")]
                                (set! (.-textContent node-el) (:title node))
                                (set! (.-textContent subtitle-el) (:subtitle node))
                                (set! (.-cssText (.-style node-el))
                                      (str "position:absolute;left:" (:x node) "px;top:" (:y node) "px;width:" (:w node) "px;height:" (:h node) "px;"
                                           "padding:7px 8px;border-radius:8px;border:1px solid #2f3658;border-left:4px solid " accent ";"
                                           "font-size:11px;line-height:1.25;cursor:pointer;overflow:hidden;"
                                           (if selected?
                                             "background:#243055;color:#f0f3ff;box-shadow:0 0 0 1px #6aa5ff inset;"
                                             "background:#171d33;color:#d6dcfb;")))
                                (set! (.-cssText (.-style subtitle-el))
                                      "margin-top:4px;font-size:10px;color:#9ca7d0;line-height:1.25;")
                                (set! (.-onclick node-el) (fn [_] (reset! !review-pack-selected-node-id (:id node))))
                                (.appendChild node-el subtitle-el)
                                (.appendChild canvas-surface node-el)))

                            (.appendChild canvas-pane canvas-surface)
                            (.appendChild body canvas-pane)

                            ;; Inspector pane
                            (set! (.-cssText (.-style inspector-pane))
                                  "border:1px solid #2b2f49;border-radius:8px;background:#141a2f;padding:10px;overflow:auto;")
                            (let [k (get-in selected-node [:payload :kind])
                                  heading (js/document.createElement "div")
                                  info (js/document.createElement "div")
                                  body-text (js/document.createElement "div")]
                              (set! (.-textContent heading)
                                    (str "NODE • " (some-> k name str/upper-case)))
                              (set! (.-cssText (.-style heading))
                                    "font-size:11px;color:#9aa5d3;letter-spacing:0.04em;text-transform:uppercase;margin-bottom:6px;")
                              (.appendChild inspector-pane heading)

                              (set! (.-textContent info)
                                    (str "ID: " (:id selected-node)))
                              (set! (.-cssText (.-style info))
                                    "font-size:11px;color:#c8cff0;margin-bottom:6px;word-break:break-all;")
                              (.appendChild inspector-pane info)

                              (set! (.-textContent body-text) (or (:subtitle selected-node) ""))
                              (set! (.-cssText (.-style body-text))
                                    "font-size:12px;color:#d9def9;line-height:1.4;margin-bottom:8px;")
                              (.appendChild inspector-pane body-text)

                              (when (= k :evidence)
                                (let [anchor (get-in selected-node [:payload :anchor])
                                      meta (js/document.createElement "div")
                                      snippet (js/document.createElement "pre")
                                      open-btn (js/document.createElement "button")
                                      project-root (:path project)]
                                  (set! (.-textContent meta)
                                        (str "File: " (or (:file-path anchor) "n/a")
                                             "\nCommit: " (or (:commit anchor) "n/a")
                                             "\nSpan: L" (get-in anchor [:span :line-start] 1)
                                             "-L" (get-in anchor [:span :line-end] 1)))
                                  (set! (.-cssText (.-style meta))
                                        "font-size:11px;color:#9ea8d7;white-space:pre-wrap;line-height:1.35;margin-bottom:8px;")
                                  (.appendChild inspector-pane meta)

                                  (set! (.-textContent snippet) (or (:snippet anchor) ""))
                                  (set! (.-cssText (.-style snippet))
                                        "margin:0 0 8px 0;padding:6px;background:#0c1020;border:1px solid #303859;border-radius:4px;color:#cad2fa;font-size:10px;line-height:1.35;white-space:pre-wrap;word-break:break-word;")
                                  (.appendChild inspector-pane snippet)

                                  (set! (.-textContent open-btn) "Open Anchor File")
                                  (set! (.-cssText (.-style open-btn))
                                        "border:1px solid #3d4f7d;border-radius:6px;padding:6px 8px;background:#1f2a4a;color:#dde5ff;cursor:pointer;font-size:11px;")
                                  (set! (.-onclick open-btn)
                                        (fn [_]
                                          (let [fp (:file-path anchor)
                                                abs-path (cond
                                                           (nil? fp) nil
                                                           (str/starts-with? fp "/") fp
                                                           (seq project-root) (str project-root "/" fp)
                                                           :else nil)]
                                            (if (and abs-path project-root)
                                              (fetch-file! abs-path project-root)
                                              (js/console.warn "[REVIEW-PACK] Missing project root to open anchor" fp)))))
                                  (.appendChild inspector-pane open-btn)))

                              (when (:ok summary-result)
                                (let [stats (js/document.createElement "div")]
                                  (set! (.-textContent stats)
                                        (str "Changed files: " (:changed-file-count summary)
                                             " | Evidence: " (:evidence-anchor-count summary)
                                             " | Core claims: " (:core-claim-count summary)))
                                  (set! (.-cssText (.-style stats))
                                        "margin-top:10px;padding-top:8px;border-top:1px solid #2a3150;font-size:11px;color:#b8c0e4;line-height:1.35;")
                                  (.appendChild inspector-pane stats))))

                            (.appendChild body inspector-pane)
                            (.appendChild right-col body))
                          (let [empty-right (js/document.createElement "div")]
                            (set! (.-textContent empty-right) "Select a review pack to open the canvas.")
                            (set! (.-cssText (.-style empty-right))
                                  "padding:12px;color:#8f99c8;font-size:12px;border:1px solid #2b2f49;border-radius:8px;background:#14182a;")
                            (.appendChild right-col empty-right)))

                        (.appendChild workspace list-col)
                        (.appendChild workspace right-col)
                        (.appendChild sidebar-el workspace))))

                  ;; FILES MODE
                  (if (nil? project)
                    ;; PROJECT PICKER — fetch home dirs then render
                    (do
                      ;; Header
                      (let [header (js/document.createElement "div")]
                        (set! (.-textContent header) "EXPLORER")
                        (set! (.-cssText (.-style header))
                              "padding:10px 12px;font-size:11px;text-transform:uppercase;letter-spacing:0.05em;color:#7878a0;border-bottom:1px solid #2a2a4a;")
                        (.appendChild sidebar-el header))
                      ;; Render dirs (or loading)
                      (if-let [dirs @!home-dirs]
                        (doseq [d dirs]
                          (let [el (js/document.createElement "div")]
                            (set! (.-textContent el) (str "📁 " (:name d)))
                            (set! (.-cssText (.-style el))
                                  "padding:6px 12px;cursor:pointer;font-size:13px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;")
                            (set! (.-onmouseenter el) #(set! (.. el -style -background) "#252547"))
                            (set! (.-onmouseleave el) #(set! (.. el -style -background) "transparent"))
                            (set! (.-onclick el)
                                  (fn [_]
                                    (reset! !selected-project {:name (:name d) :path (:path d)})
                                    (reset! !expanded-dirs #{})
                                    (reset! !dir-cache {})
                                    ;; Fetch root dir contents, then re-render
                                    (fetch-dir! (:path d) render-sidebar!)))
                            (.appendChild sidebar-el el)))
                        ;; Show loading while fetching
                        (let [loading (js/document.createElement "div")]
                          (set! (.-textContent loading) "Loading...")
                          (set! (.-cssText (.-style loading))
                                "padding:10px 12px;color:#7878a0;font-style:italic;font-size:12px;")
                          (.appendChild sidebar-el loading)
                          ;; Trigger fetch
                          (fetch-home-dirs! render-sidebar!))))

                    ;; FILE TREE VIEW
                    (let [;; Back button
                          back-btn (js/document.createElement "div")
                          _ (do (set! (.-textContent back-btn) (str "← " (:name project)))
                                (set! (.-cssText (.-style back-btn))
                                      "padding:8px 12px;cursor:pointer;font-size:12px;color:#7878a0;border-bottom:1px solid #2a2a4a;")
                                (set! (.-onmouseenter back-btn) #(set! (.. back-btn -style -background) "#252547"))
                                (set! (.-onmouseleave back-btn) #(set! (.. back-btn -style -background) "transparent"))
                                (set! (.-onclick back-btn)
                                      (fn [_]
                                        (reset! !selected-project nil)
                                        (reset! !expanded-dirs #{})
                                        (reset! !dir-cache {})
                                        (reset! !current-file nil)
                                        (render-sidebar!)))
                                (.appendChild sidebar-el back-btn))
                          ;; Render tree entries recursively
                          current-file @!current-file
                          ;; Open file breadcrumb
                          _ (when current-file
                              (let [breadcrumb (js/document.createElement "div")]
                                (set! (.-textContent breadcrumb) (:name current-file))
                                (set! (.-cssText (.-style breadcrumb))
                                      "padding:4px 12px 4px 14px;font-size:11px;color:#9898b8;border-bottom:1px solid #2a2a4a;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;")
                                (.appendChild sidebar-el breadcrumb)))
                          render-entries
                          (fn render-entries [entries depth]
                            (doseq [entry entries]
                              (let [el (js/document.createElement "div")
                                    is-dir? (= (:type entry) :dir)
                                    is-exp? (contains? expanded (:path entry))
                                    is-active? (and (not is-dir?) current-file
                                                    (= (:path entry) (:path current-file)))
                                    pad-left (+ 12 (* depth 16))]
                                (set! (.-textContent el)
                                      (if is-dir?
                                        (str (if is-exp? "▾ " "▸ ") (:name entry) "/")
                                        (str "  " (:name entry))))
                                (set! (.-cssText (.-style el))
                                      (str "padding:4px 12px;padding-left:" pad-left "px;cursor:pointer;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-size:13px;"
                                           (if is-active?
                                             "background:#37375a;border-left:3px solid #5588ff;color:#e0e0ff;"
                                             "border-left:3px solid transparent;")))
                                (set! (.-onmouseenter el) #(set! (.. el -style -background) "#252547"))
                                (set! (.-onmouseleave el) #(set! (.. el -style -background)
                                                                 (if is-active? "#37375a" "transparent")))
                                (set! (.-onclick el)
                                      (fn [_]
                                        (if is-dir?
                                          (do (swap! !expanded-dirs
                                                     (fn [dirs]
                                                       (if (contains? dirs (:path entry))
                                                         (disj dirs (:path entry))
                                                         (conj dirs (:path entry)))))
                                              ;; Fetch children if not cached, then re-render
                                              (fetch-dir! (:path entry) render-sidebar!))
                                          ;; File click — fetch content from server
                                          (fetch-file! (:path entry) (:path project)))))
                                (.appendChild sidebar-el el)
                                ;; Render children if expanded and cached
                                (when (and is-dir? is-exp?)
                                  (if-let [children (get cache (:path entry))]
                                    (render-entries children (inc depth))
                                    ;; Not cached yet — show loading placeholder
                                    (let [loading (js/document.createElement "div")]
                                      (set! (.-textContent loading) "  loading...")
                                      (set! (.-cssText (.-style loading))
                                            (str "padding:4px 12px;padding-left:" (+ pad-left 16) "px;color:#7878a0;font-size:12px;font-style:italic;"))
                                      (.appendChild sidebar-el loading)))))))
                          root-entries (get cache (:path project) [])]
                      (render-entries root-entries 0))))))))

        ;; Watch sidebar-related atoms to re-render
        _ (when !sidebar-visible
            (add-watch !sidebar-visible :sidebar-render
                       (fn [_ _ old-vis new-vis]
                         (render-sidebar!)
                         ;; When becoming visible, load active mode data if needed
                         (when (and new-vis (not old-vis))
                           (if (= @!sidebar-mode :review-packs)
                             (when (nil? @!review-pack-list)
                               (fetch-review-packs! render-sidebar!))
                             (when (nil? @!selected-project)
                               (fetch-home-dirs! render-sidebar!)))))))
        _ (add-watch !sidebar-mode :sidebar-render
                     (fn [_ _ _ mode]
                       (render-sidebar!)
                       (when (= mode :review-packs)
                         (when (nil? @!review-pack-list)
                           (fetch-review-packs! render-sidebar!)))))
        _ (add-watch !selected-project :sidebar-render (fn [_ _ _ _] (render-sidebar!)))
        _ (add-watch !expanded-dirs :sidebar-render (fn [_ _ _ _] (render-sidebar!)))
        _ (add-watch !dir-cache :sidebar-render (fn [_ _ _ _] (render-sidebar!)))
        _ (add-watch !current-file :sidebar-render (fn [_ _ _ _] (render-sidebar!)))
        _ (add-watch !review-pack-list :sidebar-render (fn [_ _ _ _] (render-sidebar!)))
        _ (add-watch !review-pack-selected-id :sidebar-render (fn [_ _ _ _] (render-sidebar!)))
        _ (add-watch !review-pack-selected-node-id :sidebar-render (fn [_ _ _ _] (render-sidebar!)))
        _ (add-watch !review-pack-summary-cache :sidebar-render (fn [_ _ _ _] (render-sidebar!)))
        _ (add-watch !review-pack-load-error :sidebar-render (fn [_ _ _ _] (render-sidebar!)))

        ;; Initial sidebar render (watches only fire on change, not initial state)
        _ (when (and !sidebar-visible @!sidebar-visible)
            (if (= @!sidebar-mode :review-packs)
              (when (nil? @!review-pack-list)
                (fetch-review-packs! render-sidebar!))
              (fetch-home-dirs! render-sidebar!)))

        ;; Seed sidebar with initial file if provided
        _ (when initial-file
            (let [file-path (:path initial-file)
                  file-name (last (str/split file-path #"/"))
                  project-path (:project initial-file)]
              (reset! !current-file {:path file-path :name file-name})
              (when project-path
                ;; Expand dirs along the file path so the file is visible in the tree
                (let [rel (subs file-path (count project-path))
                      rel (if (str/starts-with? rel "/") (subs rel 1) rel)
                      parts (str/split rel #"/")
                      dir-parts (butlast parts)
                      dir-paths (loop [acc [] prefix project-path dirs dir-parts]
                                  (if (empty? dirs)
                                    acc
                                    (let [next-path (str prefix "/" (first dirs))]
                                      (recur (conj acc next-path) next-path (rest dirs)))))]
                  (reset! !selected-project {:name (last (str/split project-path #"/"))
                                             :path project-path})
                  (reset! !expanded-dirs (set dir-paths))
                  ;; Fetch root dir + expanded dirs so tree renders with content
                  (fetch-dir! project-path
                              (fn []
                                (doseq [dp dir-paths]
                                  (fetch-dir! dp render-sidebar!))))))))

        ;; =====================================================================
        ;; LAYER 2: EVENT FLOWS
        ;; =====================================================================
        ;; IMPORTANT: These must be fresh flows, not shared top-level defs!

        >raf (make-raf-flow)              ;; Fresh RAF flow for this instance
        >blink-timer (make-blink-timer)   ;; Fresh blink timer for this instance
        >resize (>canvas-resize node)
        >wheel-events (>wheel node)
        >mouse-events (->> (>mouse node) (m/relieve (fn [_ x] x)))
        >keyboard-events (>keyboard js/window)

        ;; =====================================================================
        ;; LAYER 4: FOCUS-BASED EVENT ROUTING
        ;; =====================================================================

        <global-keys (<global-events >keyboard-events)
        <editor-keyboard (<editor-keys >keyboard-events !focus)
        <cmd-keyboard (<cmd-panel-keys >keyboard-events !focus)
        <settings-keyboard (<settings-panel-keys >keyboard-events !focus)]

    ;; =====================================================================
    ;; AUTO-BOOTSTRAP (V0 — fire once on load if idle)
    ;; =====================================================================
    ;; Semantics (per Codex review):
    ;;   Fresh load:                auto-bootstrap
    ;;   Resume with cached state:  skip bootstrap (atom persists on hot-reload)
    ;;   Resume without cached:     bootstrap (atom reset to idle)
    ;; Guard: only fires if flow state is :idle AND no session-id cached.
    ;; Auto-bootstrap disabled — user triggers /bootstrap manually from cmd panel
    ;; (was: fire once on fresh load if idle + no session-id)

    (m/join vector

      ;; =====================================================================
      ;; BLINK TIMER CONSUMER
      ;; =====================================================================
      (->> >blink-timer
           (m/reduce (fn [_ v] (reset! !caret-visible v) nil) nil))

      ;; =====================================================================
      ;; VIEWPORT RESIZE CONSUMER
      ;; =====================================================================
      (->> >resize
           (m/reduce
             (fn [_ {:keys [width height dpr]}]
               (reset! !viewport {:width width :height height :dpr dpr})
               (set! (.-width node) (Math/floor (* width dpr)))
               (set! (.-height node) (Math/floor (* height dpr)))
               nil)
             nil))

      ;; =====================================================================
      ;; SCROLL CONSUMER
      ;; =====================================================================
      (->> >wheel-events
           (m/reduce (fn [_ delta]
                       (let [viewport @!viewport
                             settings @!settings
                             dpr (:dpr viewport)
                             snap? (:snap-to-pixel? settings)
                             font-size (:font-size settings)
                             char-advance (* font-size (:char-width @!active-font))
                             agent-output @!agent-output
                             agent-h (compute-agent-panel-h agent-output font-size
                                                            (:height viewport) (:width viewport)
                                                            char-advance)
                             ;; Agent panel Y bounds (viewport-relative, no scroll offset)
                             agent-y0 (- (:height viewport) cmd-panel-h status-bar-h agent-h 12)
                             agent-y1 (- (:height viewport) cmd-panel-h status-bar-h)
                             mouse-y @!mouse-y
                             in-agent? (and (pos? agent-h)
                                            (>= mouse-y agent-y0)
                                            (< mouse-y agent-y1))]
                         (if in-agent?
                           ;; Scroll agent panel (clamp to content bounds)
                           (let [line-step (* font-size 1.2)
                                 max-chars (if (pos? char-advance)
                                             (max 1 (int (/ (- (:width viewport) 48) char-advance)))
                                             80)
                                 line-count (agent-wrapped-line-count agent-output max-chars)
                                 total-h (* line-count line-step)
                                 max-scroll (max 0 (- total-h (- agent-h 16)))]
                             (swap! !agent-scroll-y
                                    #(-> (+ % delta) (max 0) (min max-scroll))))
                           ;; Scroll editor / flow canvas
                           (if (flow-canvas-active? @!flow-state)
                             ;; List view: clamp scroll to grouped list content height
                             (let [flow @!flow-state
                                   grouped (group-tickets-by-status (:tickets flow))
                                   content-h (list-content-height grouped @!collapsed-groups)
                                   visible-h (- (:height viewport) cmd-panel-h status-bar-h agent-h 12)
                                   max-scroll (max 0 (- content-h visible-h))]
                               (swap! !scroll-y #(-> (+ % delta) (max 0) (min max-scroll))))
                             ;; Normal editor scroll (unclamped — code can be long)
                             (swap! !scroll-y #(maybe-snap (+ % delta) dpr snap?)))))
                       nil) nil))

      ;; =====================================================================
      ;; MOUSE EVENTS CONSUMER
      ;; =====================================================================
      (->> >mouse-events
           (m/reduce
             (fn [_ [type coords]]
               #_(js/console.log "Mouse:" type coords)
               (case type
                 :mousedown
                 (let [{:keys [x y]} coords
                       viewport @!viewport
                       scroll-y @!scroll-y
                       settings @!settings
                       settings-visible? (:visible settings)

                       ;; Settings panel geometry (must match compute-settings-panel-rects)
                       panel-w 600
                       panel-h 480
                       panel-x (/ (- (:width viewport) panel-w) 2)
                       panel-y (+ scroll-y (/ (- (:height viewport) panel-h) 2))

                       ;; Check if click is inside settings panel
                       clicked-in-settings? (and settings-visible?
                                                  (>= x panel-x) (< x (+ panel-x panel-w))
                                                  (>= y panel-y) (< y (+ panel-y panel-h)))]

                   (if clicked-in-settings?
                     ;; Click in settings panel
                     (let [left-w 220
                           header-h 40
                           content-y (+ panel-y header-h)
                           
                           ;; Font List
                           font-item-h 32
                           
                           ;; Sliders
                           slider-item-h 70
                           slider-count (count (slider-specs settings))
                           
                           rel-x (- x panel-x)
                           rel-y (- y content-y)]

                       (cond
                         ;; Click in Header (ignore)
                         (< rel-y 0) nil
                         
                         ;; Click in Left Pane (Fonts)
                         (< rel-x left-w)
                         (let [font-idx (int (/ rel-y font-item-h))
                               fonts (or (:fonts @!font-manifest)
                                         [{:name "DejaVu Sans Mono" :id "dejavu-sans-mono"}])
                               available-fonts (filterv #(not (false? (:available %))) fonts)]
                           (when (< font-idx (count available-fonts))
                             (swap! !settings assoc 
                                    :selected-index font-idx
                                    :focus-section :fonts)
                             ;; Immediately update active font
                             (let [selected-font (nth available-fonts font-idx)]
                               (reset! !active-font {:id (:id selected-font)
                                                     :char-width (or (:charWidth selected-font) 0.56)
                                                     :name (:name selected-font)}))
                             (js/console.log "[SETTINGS] Clicked font:" font-idx)))

                         ;; Click in Right Pane (Sliders)
                         :else
                         (let [slider-idx (int (/ rel-y slider-item-h))]
                           (when (< slider-idx slider-count)
                             (swap! !settings assoc 
                                    :slider-index slider-idx
                                    :focus-section :sliders)
                            (js/console.log "[SETTINGS] Clicked slider:" slider-idx)))))

                     ;; Not in settings - check status bar, command panel, or editor
                     (let [status-bar-top (- (:height viewport) status-bar-h)
                           clicked-in-status? (>= y status-bar-top)]
                       (if clicked-in-status?
                         nil ;; Clicks in status bar are no-ops
                     (let [cmd-panel @!cmd-panel
                           cmd-visible? (:visible cmd-panel)
                           cmd-panel-top (if cmd-visible?
                                           (- (:height viewport) cmd-panel-h status-bar-h)
                                           (:height viewport))
                           clicked-in-cmd? (and cmd-visible? (>= y cmd-panel-top) (< y status-bar-top))]

                       (if clicked-in-cmd?
                         ;; Click in command panel - use reactive font values
                         (let [font-size (:font-size @!settings)
                              dpr (:dpr @!viewport)
                              snap? (:snap-to-pixel? @!settings)
                              char-width (:char-width @!active-font)
                              char-w (maybe-snap (* font-size char-width) dpr snap?)
                              text-x (cmd-text-start-x @!ai-provider font-size char-width dpr snap?)
                              text (:text cmd-panel)
                              col (-> (/ (- x text-x) char-w)
                                       (Math/round)
                                       (max 0)
                                       (min (count text)))]
                           (reset! !focus :command-panel)
                           (swap! !cmd-panel assoc :cursor col)
                           (reset! !caret-visible true))
                        ;; Click in editor area — flow canvas or code editor
                        (do
                        (when (:visible @!cmd-panel)
                          (swap! !cmd-panel assoc :visible false))
                        (if (flow-canvas-active? @!flow-state)
                          ;; Flow canvas mode: hit-test list rows and group headers
                          (let [adj-y (+ y scroll-y)
                                flow @!flow-state
                                left-w (int (* (:width viewport) list-left-pane-pct))
                                in-left-pane? (< x left-w)]
                            (when in-left-pane?
                              (let [grouped (group-tickets-by-status (:tickets flow))
                                    layout (ticket-list-layout grouped @!collapsed-groups (set (:selected flow)))
                                    hit (first (filter (fn [entry]
                                                         (let [ey (:y entry)
                                                               eh (if (= (:type entry) :group-header)
                                                                    list-group-header-h list-row-h)]
                                                           (and (>= adj-y ey) (< adj-y (+ ey eh)))))
                                                       layout))]
                                (when hit
                                  (case (:type hit)
                                    :group-header
                                    (swap! !collapsed-groups
                                           (fn [cg] (if (contains? cg (:status hit))
                                                      (disj cg (:status hit))
                                                      (conj cg (:status hit)))))
                                    :ticket-row
                                    (let [idx (:idx hit)
                                          selected (:selected flow)
                                          already? (some #{idx} selected)
                                          new-selected (if already?
                                                         (vec (remove #{idx} selected))
                                                         (conj (vec selected) idx))]
                                      (swap! !flow-state assoc :selected new-selected)))))))
                          ;; Normal editor mode: cursor placement / fold toggle
                          (let [adj-y (+ y scroll-y)
                                dpr (:dpr @!viewport)
                                snap? (:snap-to-pixel? @!settings)
                                font-size (:font-size @!settings)
                                char-width (:char-width @!active-font)
                                line-h (maybe-snap (* font-size (:line-height @!settings)) dpr snap?)
                                char-w (maybe-snap (* font-size char-width) dpr snap?)
                                layout-x (maybe-snap layout-x dpr snap?)
                                layout-y (maybe-snap layout-y dpr snap?)
                                gutter-x (- layout-x gutter-w)
                                gutter-right (+ gutter-x gutter-w)]
                            (if (and (>= x gutter-x) (< x gutter-right))
                              ;; Gutter click - toggle fold
                              (let [text-result @!text-geo
                                    line-mapping (or (:line-mapping text-result) [])
                                    visual-line (max 0 (Math/floor (/ (- adj-y layout-y) line-h)))
                                    logical-line (get line-mapping visual-line visual-line)
                                    regions (detect-folds-fn (:lines @!editor-doc)
                                                             (mapv count (:lines @!editor-doc)))
                                    fold-region (first (filter #(= (:start-line %) logical-line)
                                                               (or regions [])))]
                                (js/console.log "[FOLD] visual:" visual-line "logical:" logical-line
                                                "region:" (clj->js fold-region)
                                                "folded-before:" (clj->js @!folded-lines))
                                (when fold-region
                                  (swap! !folded-lines
                                         (fn [folded]
                                           (if (contains? folded logical-line)
                                             (disj folded logical-line)
                                             (conj folded logical-line))))
                                  (js/console.log "[FOLD] folded-after:" (clj->js @!folded-lines)))
                                (reset! !focus :editor))
                              ;; Normal click - place cursor
                              (let [text-result @!text-geo
                                    line-mapping (or (:line-mapping text-result) [])
                                    lengths (mapv count (:lines @!editor-doc))
                                    visual-line (max 0 (Math/floor (/ (- adj-y layout-y) line-h)))
                                    logical-line (get line-mapping visual-line
                                                      (min visual-line (dec (count lengths))))
                                    line-len (get lengths logical-line 0)
                                    col (-> (/ (- x layout-x) char-w)
                                            (Math/round)
                                            (max 0)
                                            (min line-len))
                                    pos {:line logical-line :col col}]
                                (reset! !dragging? true)
                                (swap! !editor-doc assoc
                                       :cursor pos
                                       :selection nil
                                       :desired-col col)
                                (reset! !caret-visible true)
                                (reset! !focus :editor))))))))))))

                 :mousemove
                 (do (reset! !mouse-y (:y coords))
                 ;; Hover tracking for flow canvas list view
                 (when (flow-canvas-active? @!flow-state)
                   (let [mx (:x coords)
                         my (:y coords)
                         scroll-y @!scroll-y
                         left-w (int (* (:width @!viewport) list-left-pane-pct))
                         adj-y (+ my scroll-y)]
                     (if (< mx left-w)
                       ;; In left pane: find which ticket row we're over
                       (let [flow @!flow-state
                             grouped (group-tickets-by-status (:tickets flow))
                             layout (ticket-list-layout grouped @!collapsed-groups (set (:selected flow)))
                             hit (first (filter (fn [entry]
                                                  (and (= (:type entry) :ticket-row)
                                                       (let [ey (:y entry)]
                                                         (and (>= adj-y ey) (< adj-y (+ ey list-row-h))))))
                                                layout))]
                         (reset! !hovered-row-idx (when hit (:idx hit))))
                       ;; Outside left pane
                       (reset! !hovered-row-idx nil))))
                 (when @!dragging?
                   ;; Use reactive font values for mouse drag selection
                   (let [{:keys [x y]} coords
                         scroll-y @!scroll-y
                         dpr (:dpr @!viewport)
                         snap? (:snap-to-pixel? @!settings)
                         font-size (:font-size @!settings)
                         char-width (:char-width @!active-font)
                         line-h (maybe-snap (* font-size (:line-height @!settings)) dpr snap?)
                         adj-y (+ y scroll-y)
                         layout-x (maybe-snap layout-x dpr snap?)
                         layout-y (maybe-snap layout-y dpr snap?)
                         text-result @!text-geo
                         line-mapping (or (:line-mapping text-result) [])
                         lengths (mapv count (:lines @!editor-doc))
                         visual-line (max 0 (Math/floor (/ (- adj-y layout-y) line-h)))
                         logical-line (get line-mapping visual-line
                                          (min visual-line (dec (count lengths))))
                         line-len (get lengths logical-line 0)
                         char-w (maybe-snap (* font-size char-width) dpr snap?)
                         col (-> (/ (- x layout-x) char-w)
                                 (Math/round)
                                 (max 0)
                                 (min line-len))
                         pos {:line logical-line :col col}
                         doc @!editor-doc
                         start-pos (:cursor doc)]
                     (when (not= pos start-pos)
                       (swap! !editor-doc assoc
                              :selection {:start start-pos :end pos})))))

                 :mouseup
                 (reset! !dragging? false))
               nil)
             nil))

      ;; =====================================================================
      ;; GLOBAL KEYBOARD EVENTS CONSUMER
      ;; =====================================================================
      (->> <global-keys
           (m/reduce
             (fn [_ event]
               (when event
                 (case (:type event)
                   :toggle-command-panel
                   (let [visible? (:visible @!cmd-panel)]
                     (if visible?
                       (do (swap! !cmd-panel assoc :visible false)
                           (reset! !focus :editor))
                       (do (swap! !cmd-panel assoc :visible true)
                           (swap! !settings assoc :visible false)  ;; Close settings if open
                           (reset! !focus :command-panel)
                           (reset! !caret-visible true))))

                   :toggle-settings-panel
                   (let [visible? (:visible @!settings)]
                     (if visible?
                       (do (swap! !settings assoc :visible false)
                           (reset! !focus :editor))
                       (do (swap! !settings assoc :visible true)
                           (swap! !cmd-panel assoc :visible false)
                           (reset! !focus :settings-panel))))

                   :escape
                   (cond
                     (:visible @!settings)
                     (do (swap! !settings assoc :visible false)
                         (reset! !focus :editor))

                     (or (:visible @!cmd-panel) (some? (:status @!agent-output)))
                     (do (swap! !cmd-panel assoc :visible false)
                         (reset! !agent-output nil)
                         (reset! !focus :editor))

                     (and !sidebar-visible @!sidebar-visible)
                     (reset! !sidebar-visible false)

                     :else
                     (swap! !editor-doc assoc :selection nil))

                   :toggle-file-viewer
                   (when !sidebar-visible
                     (swap! !sidebar-visible not))

                   :save
                   (let [content (str/join "\n" (:lines @!editor-doc))
                         blob (js/Blob. #js [content] #js {:type "text/plain"})
                         url (js/URL.createObjectURL blob)
                         a (js/document.createElement "a")]
                     (set! (.-href a) url)
                     (set! (.-download a) "code.clj")
                     (.click a)
                     (js/URL.revokeObjectURL url)
                     (js/console.log "Saved file: code.clj" (count content) "bytes"))

                   nil))
               nil)
             nil))

      ;; =====================================================================
      ;; FILE LOAD CONSUMER
      ;; =====================================================================
      ;; Watches !file-load-request atom. When set to a map with :lines,
      ;; resets the editor state to show the new file content.
      (if !file-load-request
        (->> (m/watch !file-load-request)
             (m/eduction (filter some?))
             (m/reduce
               (fn [_ request]
                 (let [{:keys [lines]} request]
                   (when (seq lines)
                     (js/console.log "[FILE-LOAD] Loading file with" (count lines) "lines")
                     (reset! !editor-doc {:lines (vec lines)
                                          :cursor {:line 0 :col 0}
                                          :selection nil
                                          :desired-col 0})
                     (reset! !scroll-y 0)
                     (reset! !undo-stack [])
                     (reset! !redo-stack [])
                     (reset! !folded-lines #{})
                     (reset! !caret-visible true)
                     ;; Only steal focus if command panel is not open
                     (when-not (:visible @!cmd-panel)
                       (reset! !focus :editor))
                     ;; Clear the request so same file can be re-opened
                     (reset! !file-load-request nil)))
                 nil)
               nil))
        ;; No-op task when !file-load-request not provided
        (m/reduce (fn [_ _] nil) nil (m/seed [nil])))

      ;; =====================================================================
      ;; EDITOR KEYBOARD EVENTS CONSUMER
      ;; =====================================================================
      (->> <editor-keyboard
           (m/reduce
             (fn [_ event]
               (when event
                 (let [doc @!editor-doc
                       lengths (mapv count (:lines doc))]
                   (case (:type event)
                     ;; Edit operations (save undo first)
                     (:char :backspace :delete :enter :paste)
                     (let [_ (save-undo! (:lines doc) (:cursor doc))
                           new-doc (editor-apply-event doc event lengths @!clipboard)]
                       (reset! !editor-doc new-doc)
                       (reset! !caret-visible true))

                     ;; Navigation (no undo needed)
                     (:left :right :up :down :home :end :word-left :word-right)
                     (let [new-doc (editor-apply-event doc event lengths nil)
                           ;; Auto-scroll to keep caret visible (use reactive font values)
                           font-size (:font-size @!settings)
                           dpr (:dpr @!viewport)
                           snap? (:snap-to-pixel? @!settings)
                           line-h (maybe-snap (* font-size (:line-height @!settings)) dpr snap?)
                           layout-y (maybe-snap layout-y dpr snap?)
                           caret-y (+ layout-y (* (:line (:cursor new-doc)) line-h))
                           scroll-y @!scroll-y
                           viewport @!viewport
                           viewport-bottom (+ scroll-y (:height viewport))
                           padding line-h
                           new-scroll (cond
                                        (< caret-y (+ scroll-y padding))
                                        (max 0 (- caret-y padding))

                                        (> (+ caret-y line-h) (- viewport-bottom padding))
                                        (+ (- caret-y (:height viewport)) line-h padding)

                                        :else scroll-y)
                           new-scroll (maybe-snap new-scroll dpr snap?)]
                       (reset! !editor-doc new-doc)
                       (reset! !scroll-y new-scroll)
                       (reset! !caret-visible true))

                     :copy
                     (let [input {:lines (:lines doc)
                                  :cursor (:cursor doc)
                                  :selection (:selection doc)}
                           text (text-input/copy input true)]
                       (when text
                         (reset! !clipboard text)
                         (js/console.log "Copied:" text)))

                     :cut
                     (let [input {:lines (:lines doc)
                                  :cursor (:cursor doc)
                                  :selection (:selection doc)}
                           result (text-input/cut input true)]
                       (when (:text result)
                         (save-undo! (:lines doc) (:cursor doc))
                         (reset! !clipboard (:text result))
                         (reset! !editor-doc (merge doc (:state result)))
                         (js/console.log "Cut:" (:text result))))

                     :undo
                     (when-let [prev (peek @!undo-stack)]
                       (swap! !redo-stack conj {:lines (:lines doc) :cursor (:cursor doc)})
                       (swap! !undo-stack pop)
                       (reset! !editor-doc (merge doc {:lines (:lines prev)
                                                       :cursor (:cursor prev)
                                                       :selection nil
                                                       :desired-col (:col (:cursor prev))}))
                       (reset! !caret-visible true))

                     :redo
                     (when-let [next-state (peek @!redo-stack)]
                       (swap! !undo-stack conj {:lines (:lines doc) :cursor (:cursor doc)})
                       (swap! !redo-stack pop)
                       (reset! !editor-doc (merge doc {:lines (:lines next-state)
                                                       :cursor (:cursor next-state)
                                                       :selection nil
                                                       :desired-col (:col (:cursor next-state))}))
                       (reset! !caret-visible true))

                     :eval
                     (when-let [pos (:cursor doc)]
                       (if-let [form-info (find-form-fn pos (:lines doc) lengths)]
                         (let [result-text (eval-form-fn (:form-str form-info))]
                           (js/console.log "SCI Eval:" (:form-str form-info) "=>" result-text)
                           (reset! !eval-result {:text result-text
                                                 :line (:end-line form-info)
                                                 :expires-at (+ (js/Date.now) 5000)}))
                         (do (js/console.log "SCI: No form at cursor")
                             (reset! !eval-result {:text "No form at cursor"
                                                   :line (:line pos)
                                                   :expires-at (+ (js/Date.now) 2000)}))))

                     nil)))
               nil)
             nil))

      ;; =====================================================================
      ;; COMMAND PANEL KEYBOARD EVENTS CONSUMER
      ;; =====================================================================
      (->> <cmd-keyboard
           (m/reduce
             (fn [_ event]
               (when event
                 (case (:type event)
                   :enter
                   (let [cmd-text (:text @!cmd-panel)]
                     (if (seq cmd-text)
                       ;; Submit command, clear text, keep panel open for follow-up
                       (do (submit-agent-run! cmd-text)
                           (swap! !cmd-panel assoc :text "" :cursor 0))
                       ;; Empty Enter = close panel (like Escape)
                       (do (swap! !cmd-panel assoc :text "" :cursor 0 :visible false)
                           (reset! !focus :editor))))

                   ;; Edit/navigation operations
                   (let [panel @!cmd-panel
                         new-panel (cmd-panel-apply-event panel event @!clipboard)]
                     (reset! !cmd-panel new-panel)
                     (reset! !caret-visible true))))
               nil)
             nil))

      ;; =====================================================================
      ;; SETTINGS PANEL KEYBOARD EVENTS CONSUMER
      ;; =====================================================================
      (->> <settings-keyboard
           (m/reduce
             (fn [_ event]
               (when event
                 (js/console.log "[SETTINGS KEY]" (:type event) "focus=" @!focus)
                 (let [settings @!settings
                       fonts (or (:fonts @!font-manifest)
                                 [{:name "DejaVu Sans Mono" :id "dejavu-sans-mono" :charWidth 0.60}])
                       available-fonts (filterv #(not (false? (:available %))) fonts)
                       font-count (count available-fonts)
                       
                       ;; Current state
                       focus-section (:focus-section settings)
                       slider-index (:slider-index settings)
                       sliders (slider-specs settings)
                       slider-count (count sliders)]

                   (case (:type event)
                     ;; Tab: Switch Pane
                     :char
                     (if (= (:char event) "Tab")
                       (swap! !settings update :focus-section
                              (fn [s] (if (= s :fonts) :sliders :fonts)))
                       nil)

                     ;; Navigation
                     :up
                     (if (= focus-section :fonts)
                       ;; Fonts: Change selection
                       (let [new-idx (max 0 (dec (:selected-index settings)))
                             selected-font (nth available-fonts new-idx nil)]
                         (swap! !settings assoc :selected-index new-idx)
                         (when selected-font
                           (reset! !active-font {:id (:id selected-font)
                                                 :char-width (or (:charWidth selected-font) 0.56)
                                                 :name (:name selected-font)})))
                       ;; Sliders: Change selection
                       (swap! !settings update :slider-index #(max 0 (dec %))))

                     :down
                     (if (= focus-section :fonts)
                       ;; Fonts: Change selection
                       (let [new-idx (min (dec font-count) (inc (:selected-index settings)))
                             selected-font (nth available-fonts new-idx nil)]
                         (swap! !settings assoc :selected-index new-idx)
                         (when selected-font
                           (reset! !active-font {:id (:id selected-font)
                                                 :char-width (or (:charWidth selected-font) 0.56)
                                                 :name (:name selected-font)})))
                       ;; Sliders: Change selection
                       (swap! !settings update :slider-index #(min (dec slider-count) (inc %))))

                     ;; Value Adjustment (Sliders only)
                     :left
                     (when (= focus-section :sliders)
                       (let [slider (nth sliders slider-index)
                             slider-id (:id slider)]
                         (case slider-id
                           :theme-id    (let [cur-idx (themes/theme-index (:theme-id settings))
                                              new-idx (mod (dec cur-idx) (count themes/theme-list))]
                                          (swap! !settings assoc :theme-id (nth themes/theme-list new-idx)))
                           :font-size   (swap! !settings update :font-size #(max 8 (dec %)))
                           :line-height (swap! !settings update :line-height #(max 1.0 (- % 0.1)))
                           :px-range    (swap! !settings update :px-range #(max 4 (dec %)))
                           :sharpness   (swap! !settings update :sharpness #(max -0.2 (- % 0.02)))
                           :snap-to-pixel? (swap! !settings assoc :snap-to-pixel? false)
                           :show-diagnostics? (swap! !settings assoc :show-diagnostics? false))))

                     :right
                     (when (= focus-section :sliders)
                       (let [slider (nth sliders slider-index)
                             slider-id (:id slider)]
                         (case slider-id
                           :theme-id    (let [cur-idx (themes/theme-index (:theme-id settings))
                                              new-idx (mod (inc cur-idx) (count themes/theme-list))]
                                          (swap! !settings assoc :theme-id (nth themes/theme-list new-idx)))
                           :font-size   (swap! !settings update :font-size #(min 40 (inc %)))
                           :line-height (swap! !settings update :line-height #(min 2.0 (+ % 0.1)))
                           :px-range    (swap! !settings update :px-range #(min 12 (inc %)))
                           :sharpness   (swap! !settings update :sharpness #(min 0.2 (+ % 0.02)))
                           :snap-to-pixel? (swap! !settings assoc :snap-to-pixel? true)
                           :show-diagnostics? (swap! !settings assoc :show-diagnostics? true))))

                     ;; Close
                     :enter
                     (do
                       (swap! !settings assoc :visible false)
                       (reset! !focus :editor)
                       (js/console.log "[SETTINGS] Closed panel"))
                       
                     :escape
                     (do
                       (swap! !settings assoc :visible false)
                       (reset! !focus :editor))

                     nil)))
               nil)
             nil))

      ;; =====================================================================
      ;; RENDER LOOP (SINGLE TERMINAL - "PULL" MODEL)
      ;; =====================================================================
      ;;
      ;; This is the ONLY place GPU operations happen. We "pull" the latest
      ;; computed state from all derived flows when RAF fires, then upload
      ;; and draw in a single atomic operation per frame.
      ;;
      ;; This ensures:
      ;; 1. Consistent snapshot - all data is from the same logical moment
      ;; 2. No wasted GPU uploads - only upload when we're about to draw
      ;; 3. Frame-synchronized updates - GPU state changes aligned with vsync
      ;;
      (let [;; Derived flows (pure computation, no GPU side effects)
            ;; CACHED: fold state only recomputes when doc/folds change (not on blink)
            <fold-data (<fold-state !editor-doc !folded-lines detect-folds-fn)
            ;; CACHED: bracket match only recomputes when doc changes (not on blink)
            <bracket-data (<bracket-match !editor-doc find-bracket-fn)

            <text-data (<combined-text-ops !editor-doc !cmd-panel !ai-provider !agent-output !agent-scroll-y !scroll-y !viewport !settings !active-font
                                           !current-file
                                           tokenize-fn layout-fn
                                           <fold-data
                                           !flow-state !collapsed-groups !hovered-row-idx
                                           layout-x layout-y cmd-panel-h status-bar-h)
            <editor-rect-data (<editor-rects !editor-doc !eval-result !caret-visible !focus
                                             !settings !active-font !viewport
                                             <fold-data <bracket-data
                                             !flow-state !scroll-y !collapsed-groups !hovered-row-idx
                                             layout-x layout-y gutter-w)
            <cmd-rect-data (<cmd-panel-rects !cmd-panel !focus !caret-visible !scroll-y !viewport
                                             !settings !active-font
                                             !ai-provider !agent-output cmd-panel-h status-bar-h)
            ;; Settings panel flows (reactive: derive font-size from !settings internally)
            <settings-rect-data (<settings-panel-rects !settings !focus !viewport !scroll-y !font-manifest)
            <settings-text-data (<settings-panel-text !settings !viewport !scroll-y !font-manifest)

            ;; Combined world state - sampled on RAF
            ;; m/latest combines flows, m/sample synchronizes with frame clock
            ;; All derived flows now use m/latest internally, so they're continuous
            <world-snapshot (m/latest
                              (fn [text-data editor-rects cmd-rects settings-rects settings-text
                                   viewport scroll-y cmd-panel settings active-font agent-output]
                                {:text-data text-data
                                 :editor-rects editor-rects
                                 :cmd-rects cmd-rects
                                 :settings-rects settings-rects
                                 :settings-text settings-text
                                 :viewport viewport
                                 :scroll-y scroll-y
                                 :cmd-visible (:visible cmd-panel)
                                 :agent-visible (some? (:status agent-output))
                                 :settings-visible (:visible settings)
                                 ;; Include font settings for reactive text rendering
                                 :font-size (:font-size settings)
                                 :px-range (:px-range settings)
                                 :line-height (:line-height settings)
                                 :sharpness (:sharpness settings)
                                 :snap-to-pixel? (:snap-to-pixel? settings)
                                 :show-diagnostics? (:show-diagnostics? settings)
                                 :char-width (:char-width active-font)})
                              <text-data
                              <editor-rect-data
                              <cmd-rect-data
                              <settings-rect-data
                              <settings-text-data
                              (m/watch !viewport)
                              (m/watch !scroll-y)
                              (m/watch !cmd-panel)
                              (m/watch !settings)
                              (m/watch !active-font)
                              (m/watch !agent-output))]

        ;; The render pulse: sample world state on each animation frame
        ;; OPTIMIZATION: Use identical? on flow objects (cheap pointer compare)
        ;; instead of = on reconstructed data (expensive deep structural compare).
        ;; Skip draw-frame! entirely when nothing changed.
        (m/reduce
          (fn [prev-state [world _frame-time]]
              ;; --- Fast dirty check using identical? on flow objects ---
              ;; m/latest caches its result, so when no input changed,
              ;; m/sample returns the exact same object. Quick pointer compare:
              (if (identical? world (:prev-world prev-state))
                ;; FAST PATH: nothing changed, skip everything (no draw-frame!)
                prev-state

                ;; SLOW PATH: something changed, figure out what
                (let [{:keys [text-data editor-rects cmd-rects settings-rects settings-text
                              viewport scroll-y cmd-visible agent-visible settings-visible
                              font-size px-range line-height sharpness char-width
                              snap-to-pixel? show-diagnostics?]} world

                      dpr (:dpr viewport)
                      snap? (not (false? snap-to-pixel?))
                      line-h (maybe-snap (* font-size line-height) dpr snap?)
                      snap-step (when snap? (/ 1 (or dpr 1)))

                      ;; Get current font assets from atom (updated by watch)
                      font-assets @!font-assets

                      ;; Check if font changed
                      prev-font-id (:prev-font-id prev-state)
                      font-changed? (not= (:id font-assets) prev-font-id)

                      ;; Current renderer state
                      current-text-geo (:text-geo prev-state)

                      ;; Update font texture if needed (when we have a new bitmap)
                      updated-text-geo (if (and font-changed? (:bitmap font-assets))
                                         (do
                                           (js/console.log "[RENDER] Updating font texture for:" (:id font-assets))
                                           (editor/update-font-texture device current-text-geo (:bitmap font-assets)))
                                         current-text-geo)

                      ;; Determine atlas to use for shaping
                      active-atlas (or (:atlas font-assets) atlas)

                      ;; Upload text geometry (only if changed)
                      ;; Use identical? on the flow object (cheap) instead of = on vec (expensive)
                      settings-lines (when settings-visible (when settings-text [settings-text]))
                      diagnostics-line (when show-diagnostics?
                                         (let [diag-x (maybe-snap 16 dpr snap?)
                                               diag-y (maybe-snap (+ scroll-y 20) dpr snap?)
                                               diag-size (max 10 (- font-size 2))
                                               atlas-size (get-in active-atlas [:atlas :size])
                                               font-name (:name @!active-font)
                                               diag-text (str "font: " (or font-name (:id font-assets)) "\n"
                                                              "dpr: " dpr "  snap: " (if snap? "on" "off") "\n"
                                                              "pxRange: " px-range "  sharp: " sharpness "\n"
                                                              "atlas: " atlas-size "  charW: " char-width)]
                                           [{:text diag-text
                                             :type :comment
                                             :from 0 :to (count diag-text)
                                             :x diag-x
                                             :y diag-y
                                             :size diag-size
                                             :r 0.7 :g 0.7 :b 0.7 :a 1.0}]))

                      ;; Check text inputs by identity (flow objects are cached by m/latest)
                      text-same? (and (identical? text-data (:prev-text-data prev-state))
                                      (identical? settings-text (:prev-settings-text prev-state))
                                      (= show-diagnostics? (:prev-show-diagnostics prev-state))
                                      (= scroll-y (:prev-scroll-y prev-state))  ;; diagnostics HUD uses scroll-y
                                      (= font-size (:prev-font-size prev-state))
                                      (= px-range (:prev-px-range prev-state))
                                      (= line-h (:prev-line-height prev-state))
                                      (= sharpness (:prev-sharpness prev-state))
                                      (= char-width (:prev-char-width prev-state))
                                      (= snap-step (:prev-snap-step prev-state))
                                      (not font-changed?))

                      all-text-ops (if text-same?
                                     (:prev-text-ops prev-state)
                                     (vec (concat (:render-ops text-data)
                                                  settings-lines
                                                  diagnostics-line)))
                      editor-line-count (:editor-line-count text-data)
                      cmd-line-count (:cmd-line-count text-data)
                      settings-line-count (count (or settings-lines []))
                      diagnostics-line-index (when diagnostics-line
                                               (+ editor-line-count cmd-line-count settings-line-count))

                      base-text-geo (if (not text-same?)
                                      (editor/update-text-data device updated-text-geo
                                                               all-text-ops active-atlas font-size
                                                               :px-range px-range
                                                               :line-height line-h
                                                               :char-width char-width
                                                               :snap-step snap-step
                                                               :sharpness sharpness)
                                      updated-text-geo)
                      new-text-geo (assoc base-text-geo
                                          :line-mapping (:line-mapping text-data)
                                          :editor-line-count editor-line-count
                                          :cmd-line-count cmd-line-count
                                          :diagnostics-line-index diagnostics-line-index)

                      ;; Upload editor rects (only if changed — use identical? for flow objects)
                      new-editor-sys (if (not (identical? editor-rects (:prev-editor-rects prev-state)))
                                       (editor/update-rects device
                                                            (or (:editor-rect-sys prev-state) (:rect geometry))
                                                            editor-rects)
                                       (:editor-rect-sys prev-state))

                      ;; Upload cmd panel rects (only if changed)
                      new-cmd-sys (if (not (identical? cmd-rects (:prev-cmd-rects prev-state)))
                                    (editor/update-rects device
                                                         (or (:cmd-rect-sys prev-state) @!cmd-rect-sys)
                                                         (or cmd-rects []))
                                    (:cmd-rect-sys prev-state))

                      ;; Upload settings panel rects (only if changed)
                      new-settings-sys (if (not (identical? settings-rects (:prev-settings-rects prev-state)))
                                         (editor/update-rects device
                                                              (or (:settings-rect-sys prev-state) @!settings-rect-sys)
                                                              (or settings-rects []))
                                         (:settings-rect-sys prev-state))]

                  ;; Store line-mapping for mouse hit testing
                  (reset! !text-geo new-text-geo)

                  ;; Draw the frame
                  (editor/draw-frame! device ctx
                                      new-text-geo
                                      new-editor-sys
                                      new-cmd-sys
                                      (:camera-floats (:pipelines geometry))
                                      (:pass-descriptor (:pipelines geometry))
                                      0 (- scroll-y)
                                      (:width viewport) (:height viewport)
                                      :cmd-panel-visible cmd-visible
                                      :cmd-panel-h cmd-panel-h
                                      :editor-line-count (:editor-line-count text-data)
                                      :settings-visible settings-visible
                                      :settings-rect-sys new-settings-sys
                                      :diagnostics-visible show-diagnostics?
                                      :diagnostics-line-index diagnostics-line-index
                                      :agent-visible agent-visible)

                  ;; Return state for next frame comparison
                  {:text-geo new-text-geo
                       :editor-rect-sys new-editor-sys
                       :cmd-rect-sys new-cmd-sys
                       :settings-rect-sys new-settings-sys
                       :prev-world world
                       :prev-text-data text-data
                       :prev-settings-text settings-text
                       :prev-show-diagnostics show-diagnostics?
                       :prev-scroll-y scroll-y
                       :prev-text-ops all-text-ops
                       :prev-editor-rects editor-rects
                       :prev-cmd-rects cmd-rects
                       :prev-settings-rects settings-rects
                       :prev-font-size font-size
                       :prev-px-range px-range
                       :prev-line-height line-h
                       :prev-sharpness sharpness
                       :prev-char-width char-width
                       :prev-snap-step snap-step
                       :prev-font-id (:id font-assets)})))

          ;; Initial state
          {:text-geo (:text geometry)
           :editor-rect-sys (:rect geometry)
           :cmd-rect-sys @!cmd-rect-sys
           :settings-rect-sys @!settings-rect-sys
           :prev-world nil
           :prev-text-data nil
           :prev-settings-text nil
           :prev-show-diagnostics nil
           :prev-scroll-y nil
           :prev-text-ops nil
           :prev-editor-rects nil
           :prev-cmd-rects nil
           :prev-settings-rects nil
           :prev-font-size nil
           :prev-px-range nil
           :prev-line-height nil
           :prev-sharpness nil
           :prev-char-width nil
           :prev-snap-step nil
           :prev-font-id "dejavu-sans-mono"}

          ;; Sample world state on each RAF tick
          (m/sample vector <world-snapshot >raf))))))
