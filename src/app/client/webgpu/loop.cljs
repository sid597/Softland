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
   Starts in :intake so the sidebar list is visible by default."
  []
  {:node :intake
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
;; RECT TREE — Scene graph for nested UI (Step 1: data structure + tree walk)
;; ============================================================================
;;
;; Everything is a rect. The tree replaces scattered compute-*-rects fns with
;; one generic walk that produces flat GPU-compatible vectors.
;;
;; Node: {:id :type :bounds {:x :y :w :h}  ;; parent-relative
;;         :style {:bg [r g b a]}           ;; optional fill
;;         :actions {:click fn :scroll {:axis :y :atom !a}}
;;         :children [...]                  ;; back-to-front order
;;         :text [{text-op} ...]            ;; leaf text (absolute offsets applied by walk)
;;         :clip? bool}                     ;; if true, children clipped to this bounds

(defn rt-node
  "Create a rect tree node.  Bounds are in parent-relative coordinates.
   Children are rendered back-to-front (painter's order).
   Optional :layout {:direction :column/:row :gap N :padding N :align :start/:center/:end :auto-height? bool}
   enables automatic child positioning via resolve-layout."
  [id type bounds & {:keys [style actions children text clip? data layout]
                     :or {clip? false}}]
  {:id       id
   :type     type
   :bounds   bounds
   :style    (or style {})
   :actions  (or actions {})
   :children (vec (or children []))
   :text     (or text [])
   :clip?    clip?
   :data     data
   :layout   layout})

;; --- Layout engine ----------------------------------------------------------
;; Pure pre-pass: walks tree depth-first, computes child :x/:y from :layout
;; directives. Nodes without :layout pass through unchanged.

(defn normalize-padding
  "CSS-style padding shorthand:
   number        → [n n n n]       (uniform)
   [vert horiz]  → [v h v h]       (vertical, horizontal)
   [t r b l]     → [t r b l]       (clockwise from top)"
  [p]
  (cond
    (number? p)               [p p p p]
    (nil? p)                  [0 0 0 0]
    (and (vector? p) (= 2 (count p))) [(nth p 0) (nth p 1) (nth p 0) (nth p 1)]
    (and (vector? p) (= 4 (count p))) p
    :else                     [0 0 0 0]))

(defn layout-children
  "Position children inside a parent node according to its :layout directive.
   Returns the node with children's :bounds :x/:y updated.
   Children with (:data child :layout-skip?) pass through unchanged.

   Layout keys:
     :direction   :column (default) or :row
     :gap         px between children (default 0)
     :padding     number, [v h], or [t r b l] (default 0)
     :align       :start (default), :center, or :end — cross-axis alignment
     :auto-height? if true, parent :h = content height + padding"
  [node]
  (let [layout   (:layout node)
        bounds   (:bounds node)
        parent-w (:w bounds 0)
        parent-h (:h bounds 0)]
    (if-not layout
      node ;; no layout directive → pass through
      (let [{:keys [direction gap padding align auto-height?]
             :or   {direction :column gap 0 align :start}} layout
            [pt pr pb pl] (normalize-padding padding)
            children (:children node)]
        (if (empty? children)
          node
          (let [;; Separate layout-managed children from skip children
                positioned
                (loop [cs       children
                       cursor   (if (= direction :column) pt pl) ;; start after top/left padding
                       result   []]
                  (if (empty? cs)
                    result
                    (let [child (first cs)]
                      (if (get-in child [:data :layout-skip?])
                        ;; Skip — preserve as-is
                        (recur (rest cs) cursor (conj result child))
                        ;; Position this child
                        (let [cb    (:bounds child)
                              cw    (:w cb 0)
                              ch    (:h cb 0)
                              ;; Cross-axis position
                              cross (case direction
                                      :column
                                      (case align
                                        :center (+ pl (/ (- parent-w pl pr cw) 2))
                                        :end    (- parent-w pr cw)
                                        ;; :start
                                        pl)
                                      :row
                                      (case align
                                        :center (+ pt (/ (- parent-h pt pb ch) 2))
                                        :end    (- parent-h pb ch)
                                        ;; :start
                                        pt))
                              ;; Set x/y based on direction
                              new-bounds (if (= direction :column)
                                           (assoc cb :x cross :y cursor)
                                           (assoc cb :y cross :x cursor))
                              new-child  (assoc child :bounds new-bounds)
                              ;; Advance cursor along main axis
                              advance    (if (= direction :column) ch cw)
                              next-cursor (+ cursor advance gap)]
                          (recur (rest cs) next-cursor (conj result new-child)))))))
                ;; Auto-height: shrink-wrap parent to content
                total-main (if auto-height?
                             (let [managed (filterv #(not (get-in % [:data :layout-skip?])) positioned)
                                   last-child (peek managed)]
                               (when last-child
                                 (let [lb (:bounds last-child)]
                                   (+ (if (= direction :column)
                                        (+ (:y lb 0) (:h lb 0) pb)
                                        (+ (:x lb 0) (:w lb 0) pr))))))
                             nil)
                new-bounds (if total-main
                             (if (= direction :column)
                               (assoc bounds :h total-main)
                               (assoc bounds :w total-main))
                             bounds)]
            (assoc node :children positioned :bounds new-bounds)))))))

(defn resolve-text-layout
  "Auto-position text ops on a node that has :text-layout.
   Text-layout map: {:line-height N :max-chars N :padding [t r b l] or N}
   Text ops provide :text, :size, :r/:g/:b/:a, :type — but NOT :x/:y.
   This fn computes :x/:y by wrapping text and stacking lines vertically.
   Returns the node with :text updated (local coords)."
  [node]
  (let [tl (:text-layout node)]
    (if-not tl
      node
      (let [{:keys [line-height max-chars padding]} tl
            [pt _pr _pb pl] (normalize-padding padding)
            text-specs (:text node)]
        (if (empty? text-specs)
          node
          (let [ops (loop [specs text-specs
                           y     pt
                           acc   []]
                     (if (empty? specs)
                       acc
                       (let [spec  (first specs)
                             txt   (:text spec "")
                             size  (:size spec 14)
                             ;; Split by newlines first, then wrap each line
                             raw-lines  (str/split-lines txt)
                             lines      (if max-chars
                                          (vec (mapcat #(wrap-line % max-chars) raw-lines))
                                          raw-lines)
                             line-ops   (mapv (fn [i line-text]
                                               (assoc spec
                                                      :text line-text
                                                      :from 0
                                                      :to   (count line-text)
                                                      :x    pl
                                                      :y    (+ y (* i (or line-height size)))))
                                             (range) lines)
                             next-y     (+ y (* (count lines) (or line-height size)))]
                         (recur (rest specs) next-y (into acc line-ops)))))]
            (assoc node :text ops)))))))

(defn resolve-layout
  "Recursive depth-first pre-pass: apply layout-children at each level,
   resolve text-layout, then recurse into children. Returns a fully-positioned
   tree ready for tree->rects / tree->text-ops / tree->shadows."
  [node]
  (let [laid-out  (-> node layout-children resolve-text-layout)
        children  (:children laid-out)]
    (if (empty? children)
      laid-out
      (assoc laid-out :children (mapv resolve-layout children)))))

;; --- Tree walk: rects -------------------------------------------------------

(defn tree->rects
  "Walk rect tree depth-first, emit flat vector of GPU rect maps.
   Parent-relative coords are converted to absolute via parent-x/parent-y.
   Clip-bounds is {:x :y :w :h} in absolute space (nil = no clipping).
   Style keys: :bg, :radius, :corner-radii, :border-width, :border-widths,
               :border-color, :gradient, :gradient-color2"
  ([node] (tree->rects node 0 0 nil))
  ([node parent-x parent-y clip-bounds]
   (let [{:keys [bounds style children clip?]} node
         abs-x (+ parent-x (:x bounds 0))
         abs-y (+ parent-y (:y bounds 0))
         w     (:w bounds 0)
         h     (:h bounds 0)
         ;; If parent clips, check visibility
         visible? (if clip-bounds
                    (let [cx (:x clip-bounds) cy (:y clip-bounds)
                          cw (:w clip-bounds) ch (:h clip-bounds)]
                      (and (< abs-x (+ cx cw))
                           (< abs-y (+ cy ch))
                           (> (+ abs-x w) cx)
                           (> (+ abs-y h) cy)))
                    true)]
     (when visible?
       (let [;; Background rect from style — now includes SDF properties
             bg  (when-let [c (:bg style)]
                   (cond-> {:x abs-x :y abs-y :w w :h h
                            :r (nth c 0) :g (nth c 1) :b (nth c 2) :a (nth c 3)}
                     (:radius style)         (assoc :radius (:radius style))
                     (:corner-radii style)   (assoc :corner-radii (:corner-radii style))
                     (:border-width style)   (assoc :border-width (:border-width style))
                     (:border-widths style)  (assoc :border-widths (:border-widths style))
                     (:border-color style)   (assoc :border-color (:border-color style))
                     (:gradient style)       (assoc :gradient (:gradient style))
                     (:gradient-color2 style)(assoc :gradient-color2 (:gradient-color2 style))))
             ;; This node's clip bounds for children (if clip? is set)
             child-clip (if clip?
                          {:x abs-x :y abs-y :w w :h h}
                          clip-bounds)
             ;; Recurse children (depth-first, painter's order)
             child-rects (into [] (mapcat #(tree->rects % abs-x abs-y child-clip)) children)]
         (cond-> []
           bg   (conj bg)
           true (into child-rects)))))))

;; --- Tree walk: text ops ----------------------------------------------------

(defn tree->text-ops
  "Walk rect tree depth-first, emit nested vector of text-op vectors.
   Text ops on each node have :x/:y in node-local space; the walk
   offsets them to absolute coordinates.  Returns [[{op}] ...]."
  ([node] (tree->text-ops node 0 0 nil))
  ([node parent-x parent-y clip-bounds]
   (let [{:keys [bounds style children text clip?]} node
         abs-x (+ parent-x (:x bounds 0))
         abs-y (+ parent-y (:y bounds 0))
         w     (:w bounds 0)
         h     (:h bounds 0)
         visible? (if clip-bounds
                    (let [cx (:x clip-bounds) cy (:y clip-bounds)
                          cw (:w clip-bounds) ch (:h clip-bounds)]
                      (and (< abs-x (+ cx cw))
                           (< abs-y (+ cy ch))
                           (> (+ abs-x w) cx)
                           (> (+ abs-y h) cy)))
                    true)]
     (when visible?
       (let [;; Offset this node's text ops to absolute space
             own-ops (when (seq text)
                       (mapv (fn [op]
                               (if (vector? op)
                                 ;; op is already a vec of text-op maps (nested format)
                                 (mapv #(-> %
                                            (update :x + abs-x)
                                            (update :y + abs-y)) op)
                                 ;; Single text-op map
                                 [(-> op
                                      (update :x + abs-x)
                                      (update :y + abs-y))]))
                             text))
             child-clip (if clip?
                          {:x abs-x :y abs-y :w w :h h}
                          clip-bounds)
             child-ops (into [] (mapcat #(tree->text-ops % abs-x abs-y child-clip)) children)]
         (into (vec (or own-ops [])) child-ops))))))

;; --- Tree walk: shadows -----------------------------------------------------

(defn tree->shadows
  "Walk rect tree depth-first, emit flat vector of shadow maps.
   Only nodes with :shadow in style produce shadows.
   Shadow map keys: :x :y :w :h :blur :offset-x :offset-y :spread :color :radius :corner-radii"
  ([node] (tree->shadows node 0 0))
  ([node parent-x parent-y]
   (let [{:keys [bounds style children]} node
         abs-x (+ parent-x (:x bounds 0))
         abs-y (+ parent-y (:y bounds 0))
         w     (:w bounds 0)
         h     (:h bounds 0)
         shadow-spec (:shadow style)
         own-shadow (when shadow-spec
                      (let [s shadow-spec]
                        {:x abs-x :y abs-y :w w :h h
                         :blur     (or (:blur s) 8.0)
                         :offset-x (or (:offset-x s) 0.0)
                         :offset-y (or (:offset-y s) 0.0)
                         :spread   (or (:spread s) 0.0)
                         :color    (or (:color s) [0 0 0 0.25])
                         :radius   (:radius style)
                         :corner-radii (:corner-radii style)}))
         child-shadows (into [] (mapcat #(tree->shadows % abs-x abs-y)) children)]
     (cond-> []
       own-shadow (conj own-shadow)
       true       (into child-shadows)))))

;; --- Hit testing ------------------------------------------------------------

(defn hit-test
  "Find the deepest node containing point (px, py).
   Returns a vector of nodes from root to deepest hit [root ... leaf],
   or nil if the point misses the tree entirely.
   The LAST element is the deepest (innermost) hit — the event target.
   Earlier elements are ancestors — used for bubbling."
  ([node px py] (hit-test node px py 0 0))
  ([node px py parent-x parent-y]
   (let [{:keys [bounds children]} node
         abs-x (+ parent-x (:x bounds 0))
         abs-y (+ parent-y (:y bounds 0))
         w     (:w bounds 0)
         h     (:h bounds 0)]
     (when (and (>= px abs-x) (< px (+ abs-x w))
                (>= py abs-y) (< py (+ abs-y h)))
       ;; Point is inside this node — check children (reverse order = front-to-back)
       (let [child-hit (some (fn [child]
                               (hit-test child px py abs-x abs-y))
                             (rseq children))]
         (if child-hit
           (into [node] child-hit)
           [node]))))))

;; --- Event dispatch with bubbling -------------------------------------------

(defn dispatch-event
  "Dispatch an event to the hit-test path (innermost → outermost).
   event-type is a keyword (:click, :scroll, etc.).
   event is the event data map.
   path is the hit-test result [root ... target].
   Walks from target to root (bubbling).  First handler that returns
   a non-nil value stops propagation.  Returns {:handled? bool :result any}."
  [path event-type event]
  (when (seq path)
    (loop [nodes (rseq path)]  ;; target first, root last
      (if-let [node (first nodes)]
        (let [handler (get-in node [:actions event-type])]
          (if (and handler (fn? handler))
            (let [result (handler node event)]
              (if (some? result)
                {:handled? true :result result :node node}
                (recur (rest nodes))))  ;; nil = let it bubble
            (recur (rest nodes))))
        {:handled? false}))))

;; ============================================================================
;; SCREEN 1: MASTER-DETAIL INTAKE (List View)
;; ============================================================================

(def list-row-h 36)
(def list-group-header-h 28)
(def list-left-pane-pct 0.40)
(def list-padding-x 16)
(def list-item-inset 6)       ;; horizontal inset for rounded hover bg
(def list-padding-top 44)
(def list-checkbox-size 16)
(def list-divider-w 1)
(def list-group-gap 12)       ;; vertical space between groups
(def list-footer-h 40)        ;; footer height

;; ============================================================================
;; SIDEBAR CONSTANTS (WebGPU-native sidebar)
;; ============================================================================

(def sidebar-w 256)
(def sidebar-tab-h 36)
(def sidebar-row-h 32)
(def sidebar-back-h 48)
(def sidebar-breadcrumb-h 24)
(def sidebar-indent-px 14)
(def sidebar-padding-x 16)
(def sidebar-item-inset 6)
(def sidebar-font-size 13)

(defn split-filename
  "Split a filename into [stem extension] at the last dot.
   Handles dotfiles (.gitignore → ['.gitignore' nil]), no-ext (Makefile → ['Makefile' nil]),
   and truncated names ending in '..' (returned as-is, no split)."
  [name]
  (if (str/ends-with? name "..")
    [name nil]  ;; Truncated — don't split the '..' marker
    (let [dot-idx (str/last-index-of name ".")]
      (if (and dot-idx (pos? dot-idx))
        [(subs name 0 dot-idx) (subs name dot-idx)]
        [name nil]))))

(defn flatten-file-tree
  "Recursively walk dir-cache tree and return a flat vector of row descriptors.
   Each row: {:entry {:name :path :type} :depth N :expanded? bool :active? bool}
   Dirs listed before files at each level, both sorted alphabetically."
  [entries expanded-dirs current-file cache depth]
  (let [sorted (sort-by (fn [e] [(if (= (:type e) :dir) 0 1)
                                  (str/lower-case (or (:name e) ""))])
                         entries)]
    (into []
      (mapcat
        (fn [entry]
          (let [is-dir? (= (:type entry) :dir)
                is-exp? (and is-dir? (contains? expanded-dirs (:path entry)))
                is-active? (and (not is-dir?) current-file
                                (= (:path entry) (:path current-file)))
                row {:entry entry :depth depth :expanded? is-exp? :active? is-active?}
                children (when (and is-dir? is-exp?)
                           (when-let [child-entries (get cache (:path entry))]
                             (flatten-file-tree child-entries expanded-dirs current-file
                                                cache (inc depth))))]
            (if children
              (into [row] children)
              [row]))))
      sorted)))

(defn compute-sidebar-content-height
  "Total content height in px for sidebar scroll clamping."
  [sidebar-state current-file]
  (let [{:keys [mode project expanded-dirs dir-cache home-dirs]} sidebar-state]
    (if (= mode :review-packs)
      100  ;; placeholder height
      (if (nil? project)
        ;; Home dirs list
        (* (count (or home-dirs [])) sidebar-row-h)
        ;; File tree
        (let [root-entries (get dir-cache (:path project) [])
              flat (flatten-file-tree root-entries expanded-dirs current-file dir-cache 0)]
          (* (count flat) sidebar-row-h))))))

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
;; DESIGN TOKENS (Linear/shadcn-inspired dark theme)
;; ============================================================================

(def dt
  "Design tokens — single source of truth for colors, spacing, radii, shadows."
  {:colors {:bg           [0.09 0.09 0.11 1.0]
            :bg-subtle    [0.11 0.11 0.13 1.0]
            :bg-muted     [0.14 0.14 0.17 1.0]
            :bg-elevated  [0.13 0.13 0.16 1.0]
            :bg-hover     [0.16 0.16 0.19 1.0]   ;; rounded hover highlight (solid)
            :bg-selected  [0.20 0.24 0.36 0.9]   ;; selected item bg (prominent)
            :bg-active    [0.15 0.15 0.18 1.0]   ;; pressed/active state
            :border       [0.22 0.22 0.28 1.0]
            :border-subtle [0.18 0.18 0.22 0.6]
            :fg           [0.90 0.90 0.92 1.0]
            :fg-muted     [0.55 0.55 0.60 1.0]
            :fg-subtle    [0.40 0.40 0.45 0.8]
            :fg-section   [0.42 0.42 0.48 1.0]   ;; section label text (solid)
            :accent       [0.35 0.55 0.95 1.0]
            :accent-muted [0.25 0.38 0.65 0.3]
            :destructive  [0.90 0.30 0.30 1.0]
            :success      [0.30 0.80 0.50 1.0]
            :warning      [0.95 0.75 0.25 1.0]}
   :spacing {:xs 4 :sm 8 :md 12 :lg 16 :xl 24 :xxl 32}
   :radii   {:sm 4 :md 6 :lg 8 :xl 12 :full 9999}
   :shadows {:sm  {:blur 4  :offset-y 1 :color [0 0 0 0.15]}
             :md  {:blur 8  :offset-y 2 :color [0 0 0 0.25]}
             :lg  {:blur 16 :offset-y 4 :color [0 0 0 0.35]}}
   :font-sizes {:xs 10 :sm 12 :md 14 :lg 16 :xl 20}})

;; ============================================================================
;; COMPONENT LIBRARY (pure fns → rt-node trees)
;; ============================================================================

(defn ui-card
  "Card component: rounded rect with border, optional shadow.
   Returns an rt-node with :shadow and :radius in style."
  [id bounds & {:keys [children text shadow variant]
                :or {shadow :md variant :default}}]
  (let [bg     (case variant
                 :elevated (:bg-elevated (:colors dt))
                 :muted    (:bg-muted (:colors dt))
                 (:bg-subtle (:colors dt)))
        border (:border (:colors dt))
        radius (:lg (:radii dt))
        shadow-spec (get (:shadows dt) shadow)]
    (rt-node id :card bounds
             :style (cond-> {:bg bg :radius radius
                             :border-width 1 :border-color border}
                      shadow-spec (assoc :shadow shadow-spec))
             :children (vec (or children []))
             :text (or text []))))

(defn ui-badge
  "Small pill badge with tinted background. Returns an rt-node."
  [id bounds label & {:keys [color text-color font-size]
                      :or {color (:accent-muted (:colors dt))
                           text-color (:accent (:colors dt))
                           font-size (:xs (:font-sizes dt))}}]
  (rt-node id :badge bounds
           :style {:bg color :radius (:full (:radii dt))}
           :text [{:text label :type :keyword
                   :from 0 :to (count label)
                   :x 6 :y (- (:h bounds) 4)
                   :size font-size
                   :r (nth text-color 0) :g (nth text-color 1)
                   :b (nth text-color 2) :a (nth text-color 3)}]))

(defn ui-button
  "Button component: solid/outline/ghost variants. Returns an rt-node."
  [id bounds label & {:keys [variant on-click font-size]
                      :or {variant :solid font-size (:sm (:font-sizes dt))}}]
  (let [accent (:accent (:colors dt))
        styles (case variant
                 :solid   {:bg accent :radius (:md (:radii dt))}
                 :outline {:bg [0 0 0 0] :radius (:md (:radii dt))
                           :border-width 1 :border-color accent}
                 :ghost   {:bg [0 0 0 0] :radius (:md (:radii dt))}
                 {:bg accent :radius (:md (:radii dt))})
        text-c (case variant
                 :solid [1 1 1 1]
                 :outline accent
                 :ghost (:fg-muted (:colors dt))
                 [1 1 1 1])]
    (rt-node id :button bounds
             :style styles
             :actions (if on-click {:click on-click} {})
             :text [{:text label :type :text
                     :from 0 :to (count label)
                     :x (:sm (:spacing dt)) :y (- (:h bounds) 5)
                     :size font-size
                     :r (nth text-c 0) :g (nth text-c 1)
                     :b (nth text-c 2) :a (nth text-c 3)}])))

(defn ui-divider
  "Thin horizontal separator line. Returns an rt-node."
  [id bounds & {:keys [color] :or {color (:border-subtle (:colors dt))}}]
  (rt-node id :divider bounds :style {:bg color}))

(defn ui-progress
  "Progress bar (track + fill). Returns an rt-node with child fill rect."
  [id bounds progress & {:keys [color track-color]
                         :or {color (:accent (:colors dt))
                              track-color (:bg-muted (:colors dt))}}]
  (let [fill-w (* (:w bounds) (min 1.0 (max 0.0 progress)))]
    (rt-node id :progress bounds
             :style {:bg track-color :radius (:sm (:radii dt))}
             :children [(rt-node (keyword (str (name id) "-fill")) :progress-fill
                          {:x 0 :y 0 :w fill-w :h (:h bounds)}
                          :style {:bg color :radius (:sm (:radii dt))})])))

(defn ui-scrollbar
  "Vertical scrollbar (track + thumb). Returns an rt-node."
  [id bounds thumb-pct thumb-offset & {:keys [track-color thumb-color]
                                       :or {track-color [0 0 0 0]
                                            thumb-color [0.35 0.35 0.40 0.5]}}]
  (let [track-h (:h bounds)
        thumb-h (max 20 (* track-h thumb-pct))
        thumb-y (* (- track-h thumb-h) (min 1.0 (max 0.0 thumb-offset)))]
    (rt-node id :scrollbar bounds
             :style {:bg track-color}
             :children [(rt-node (keyword (str (name id) "-thumb")) :scrollbar-thumb
                          {:x 1 :y thumb-y :w (- (:w bounds) 2) :h thumb-h}
                          :style {:bg thumb-color :radius (:full (:radii dt))})])))

(defn ui-tabs
  "Tab bar with active indicator. Returns an rt-node.
   tabs: [{:id :label}], active-id: keyword."
  [id bounds tabs active-id & {:keys [font-size padding]
                                :or {font-size (:sm (:font-sizes dt)) padding 16}}]
  (let [char-adv (* font-size 0.56)
        {tab-nodes :nodes}
        (reduce
          (fn [{:keys [nodes tx]} tab]
            (let [active? (= (:id tab) active-id)
                  label-str (:label tab)
                  text-w (* (count label-str) char-adv)
                  tab-w (+ text-w (* 2 padding))]
              {:nodes
               (conj nodes
                 (rt-node (:id tab) :tab
                   {:x tx :y 0 :w tab-w :h (:h bounds)}
                   :data {:tab-id (:id tab)}
                   :children (when active?
                               [(rt-node (keyword (str (name (:id tab)) "-ind")) :tab-indicator
                                  {:x padding :y (- (:h bounds) 2) :w text-w :h 2}
                                  :style {:bg [0.9 0.9 0.92 1.0]})])
                   :text [{:text label-str :type (if active? :keyword :text)
                           :from 0 :to (count label-str)
                           :x padding :y (- (:h bounds) 14)
                           :size font-size
                           :r (if active? 0.9 0.55)
                           :g (if active? 0.9 0.55)
                           :b (if active? 0.92 0.60)
                           :a 1.0}]))
               :tx (+ tx tab-w)}))
          {:nodes [] :tx 0}
          tabs)]
    (rt-node id :tab-bar bounds
             :style {:bg (:bg-elevated (:colors dt))
                     :border-widths [0 0 1 0]
                     :border-color (:border (:colors dt))}
             :children tab-nodes)))

(defn ui-tooltip
  "Small floating card with text, positioned at anchor. Returns an rt-node."
  [id bounds label & {:keys [font-size] :or {font-size (:xs (:font-sizes dt))}}]
  (let [fg (:fg (:colors dt))]
    (ui-card id bounds
             :shadow :sm
             :variant :elevated
             :text [{:text label :type :text
                     :from 0 :to (count label)
                     :x (:sm (:spacing dt)) :y (- (:h bounds) 5)
                     :size font-size
                     :r (nth fg 0) :g (nth fg 1) :b (nth fg 2) :a (nth fg 3)}])))

;; ============================================================================
;; COMPOSABLE PANEL COMPONENTS (shadcn Sidebar pattern for WebGPU)
;; ============================================================================
;; Pure fns returning rt-nodes with :layout directives.
;; Composition: ui-panel > ui-panel-header > ui-panel-content > ui-panel-group > ui-list-item
;; The layout engine (resolve-layout) handles all child positioning.

(defn ui-panel
  "Container panel — the outermost sidebar/panel frame.
   Vertical column layout: stacks header/content/footer automatically.
   variant: :default (bg), :elevated (bg-elevated), :subtle (bg-subtle)"
  [id bounds & {:keys [children variant style]
                :or {variant :default}}]
  (let [bg (case variant
             :elevated (:bg-elevated (:colors dt))
             :subtle   (:bg-subtle (:colors dt))
             (:bg (:colors dt)))]
    (rt-node id :panel bounds
             :style (merge {:bg bg} style)
             :layout {:direction :column}
             :children (vec (or children [])))))

(defn ui-panel-header
  "Fixed-height header slot with integrated bottom border.
   text: vector of text-op maps (with :x/:y in local coords)."
  [id w h & {:keys [children text style]}]
  (rt-node id :panel-header
    {:x 0 :y 0 :w w :h h}
    :style (merge {:bg (:bg-elevated (:colors dt))
                   :border-widths [0 0 1 0]
                   :border-color (:border-subtle (:colors dt))}
                  style)
    :children (vec (or children []))
    :text (or text [])))

(defn ui-panel-content
  "Scrollable content area — fills remaining height, clips children.
   layout-opts override defaults: {:direction :column :gap list-group-gap}."
  [id w h & {:keys [children layout-opts]}]
  (rt-node id :panel-content
    {:x 0 :y 0 :w w :h h}
    :clip? true
    :layout (merge {:direction :column :gap list-group-gap :padding [4 0 0 0]} layout-opts)
    :children (vec (or children []))))

(defn ui-panel-footer
  "Fixed-height footer slot with top border separator."
  [id w h & {:keys [children text style]}]
  (rt-node id :panel-footer
    {:x 0 :y 0 :w w :h h}
    :style (merge {:bg (:bg-elevated (:colors dt))
                   :border-widths [1 0 0 0]
                   :border-color (:border-subtle (:colors dt))} style)
    :children (vec (or children []))
    :text (or text [])))

(defn ui-panel-group
  "Collapsible labeled section — shadcn-style uppercase muted label + items.
   items: vector of rt-nodes (typically ui-list-item results).
   status: raw status string (for hit-test/collapse). label: display string.
   Height is auto-computed: header-h + (collapsed? 0 : items)."
  [id w & {:keys [label status icon icon-color collapsed? items font-size first-group?]
           :or {font-size (:xs (:font-sizes dt))
                collapsed? false
                first-group? false}}]
  (let [header-h list-group-header-h
        sc (:fg-section (:colors dt))
        ind (if collapsed? ">" "v")
        upper-label (str/upper-case (or label ""))
        label-str (str ind " " upper-label)
        ;; Subtle top separator between groups (skip first)
        sep-node (when-not first-group?
                   (rt-node (keyword (str (name id) "-sep")) :divider
                     {:x list-padding-x :y 0 :w (- w (* 2 list-padding-x)) :h 1}
                     :style {:bg (:border-subtle (:colors dt))}))
        group-header (rt-node (keyword (str (name id) "-hdr")) :group-header
                       {:x 0 :y 0 :w w :h header-h}
                       :data {:status (or status label)}
                       :text [{:text label-str :type :comment
                               :from 0 :to (count label-str)
                               :x list-padding-x :y 19
                               :size font-size
                               :r (nth sc 0) :g (nth sc 1) :b (nth sc 2) :a (nth sc 3)}])
        visible-items (if collapsed? [] (vec items))
        all-children (cond-> []
                       sep-node  (conj sep-node)
                       true      (conj group-header)
                       true      (into visible-items))
        sep-h (if sep-node 1 0)
        total-h (+ sep-h header-h (if collapsed? 0 (* (count (or items [])) list-row-h)))]
    (rt-node id :panel-group
      {:x 0 :y 0 :w w :h total-h}
      :layout {:direction :column}
      :children all-children)))

(defn ui-list-item
  "Row with leading/title/trailing slots — shadcn SidebarMenuItem style.
   Rounded inset hover/selected background with left accent bar on selected.
   leading/trailing are child rt-nodes.
   title: string — rendered as text between leading and trailing."
  [id w & {:keys [title leading trailing selected? hovered? ghost? data
                  font-size title-x-offset]
           :or {font-size (:sm (:font-sizes dt))
                title-x-offset (+ list-padding-x list-checkbox-size 16)}}]
  (let [;; Outer row is transparent — inner child gets the rounded bg
        ga (if ghost? 0.3 1.0)
        ;; Inner rounded highlight rect (inset from edges)
        inner-bg (cond
                   (and selected? ghost?) (assoc (:bg-selected (:colors dt)) 3 0.15)
                   selected?              (:bg-selected (:colors dt))
                   hovered?               (:bg-hover (:colors dt))
                   :else                  nil)
        inset list-item-inset
        inner-h (- list-row-h 4)  ;; 2px top + 2px bottom breathing room
        inner-w (- w (* 2 inset))
        highlight-node (when inner-bg
                         (rt-node (keyword (str (name id) "-hl")) :highlight
                           {:x inset :y 2 :w inner-w :h inner-h}
                           :style {:bg inner-bg
                                   :radius (:lg (:radii dt))}))
        ;; Left accent bar on selected items (shadcn active indicator)
        accent-bar (when (and selected? (not ghost?))
                     (rt-node (keyword (str (name id) "-acc")) :accent-bar
                       {:x (+ inset 1) :y 6 :w 3 :h (- list-row-h 12)}
                       :style {:bg (:accent (:colors dt))
                               :radius (:sm (:radii dt))}))
        ;; Text truncation
        trunc-max (if (pos? font-size)
                    (max 8 (int (/ (- w title-x-offset 36)
                                   (* font-size 0.56))))
                    30)
        trunc (when title
                (if (> (count title) trunc-max)
                  (str (subs title 0 (- trunc-max 2)) "..")
                  title))
        ;; Text vertically centered in row
        text-y (+ (/ list-row-h 2) (/ font-size 2.5))
        children (cond-> []
                   highlight-node (conj highlight-node)
                   accent-bar     (conj accent-bar)
                   (and leading (not ghost?)) (conj leading)
                   (and trailing (not ghost?)) (conj trailing))
        ;; Selected text gets slightly brighter
        fg (if selected?
             [0.95 0.95 0.98 1.0]
             (:fg (:colors dt)))
        text-ops (cond-> []
                   trunc
                   (conj {:text trunc :type :text
                          :from 0 :to (count trunc)
                          :x title-x-offset :y text-y
                          :size font-size
                          :r (nth fg 0) :g (nth fg 1) :b (nth fg 2)
                          :a (* (nth fg 3) ga)}))]
    (rt-node id :ticket-row
      {:x 0 :y 0 :w w :h list-row-h}
      :data (merge {:selected? selected? :hovered? hovered? :ghost? ghost?}
                   data)
      :children children
      :text text-ops)))

(defn ui-checkbox
  "Simplified checkbox — single rect, no inner fill child.
   Checked: accent bg + accent border. Unchecked: transparent + subtle border."
  [id & {:keys [checked? size]
         :or {size list-checkbox-size}}]
  (let [cb-x (+ list-padding-x 4)
        cb-y (/ (- list-row-h size) 2)]
    (rt-node id :checkbox
      {:x cb-x :y cb-y :w size :h size}
      :style {:bg (if checked?
                    (:accent (:colors dt))
                    [0.15 0.15 0.18 0.6])
              :radius (:sm (:radii dt))
              :border-width 1.5
              :border-color (if checked?
                              (:accent (:colors dt))
                              [0.30 0.30 0.36 0.5])})))

(defn ui-priority-dot
  "Circular color dot indicating priority level (1-4).
   Uses priority-colors lookup. 8px dot, vertically centered."
  [id priority & {:keys [parent-w] :or {parent-w 0}}]
  (let [prio (or priority 0)
        dot-size 8
        pc (get priority-colors prio {:r 0.55 :g 0.55 :b 0.60 :a 0.8})]
    (rt-node id :priority-dot
      {:x (if (pos? parent-w) (- parent-w 24) 0)
       :y (/ (- list-row-h dot-size) 2)
       :w dot-size :h dot-size}
      :style {:bg [(:r pc) (:g pc) (:b pc) (:a pc)]
              :radius (:full (:radii dt))})))

;; ============================================================================
;; SIDEBAR TREE BUILDER (rect tree for file sidebar)
;; ============================================================================

(defn build-sidebar-tree
  "Build the file sidebar scene graph. Pure function, same pattern as build-intake-tree.
   Returns a single rt-node tree. Walk with tree->rects for GPU rects, tree->text-ops for text.
   The sidebar root is pinned to the viewport via scroll-y offset."
  [sidebar-state current-file sidebar-visible?
   viewport-h scroll-y font-size char-advance]
  (when sidebar-visible?
    (let [{:keys [mode project expanded-dirs dir-cache home-dirs
                  hovered-id review-pack-list review-pack-load-error]} sidebar-state
          sidebar-scroll-y (or (:scroll-y sidebar-state) 0)
          sb-w sidebar-w
          sb-font font-size
          sb-char-advance (* sb-font 0.56)
          max-chars (max 8 (int (/ (- sb-w (* 2 sidebar-padding-x)) sb-char-advance)))
          colors (:colors dt)

          ;; Tab bar at top
          tabs-node (ui-tabs :sidebar-tabs
                     {:x 0 :y 0 :w sb-w :h sidebar-tab-h}
                     [{:id :files :label "Files"} {:id :review-packs :label "Review"}]
                     mode
                     :font-size (:md (:font-sizes dt))
                     :padding 16)

          ;; Right border line (1px separator between sidebar and content)
          border-node (rt-node :sidebar-border :chrome
                        {:x (dec sb-w) :y 0 :w 1 :h viewport-h}
                        :style {:bg (:border colors)})

          ;; Content below tabs
          content-top sidebar-tab-h
          content-h (- viewport-h content-top)

          content-children
          (cond
            ;; Review packs mode — placeholder
            (= mode :review-packs)
            (let [msg (cond
                        review-pack-load-error (str "Error: " review-pack-load-error)
                        (nil? review-pack-list) "Loading review packs..."
                        (empty? review-pack-list) "No review packs yet."
                        :else "Review packs (coming soon)")]
              [(rt-node :review-placeholder :text-block
                 {:x 0 :y 0 :w sb-w :h 40}
                 :text [{:text msg :type :comment
                         :from 0 :to (count msg)
                         :x sidebar-padding-x :y 24
                         :size sb-font
                         :r 0.40 :g 0.40 :b 0.45 :a 0.8}])])

            ;; Files mode, no project selected — show home dirs
            (nil? project)
            (let [;; Header
                  explorer-label "EXPLORER"
                  header-node (rt-node :explorer-hdr :header
                                {:x 0 :y 0 :w sb-w :h sidebar-back-h}
                                :style {:bg (:bg-elevated colors)
                                        :border-widths [0 0 1 0]
                                        :border-color (:border-subtle colors)}
                                :text [{:text explorer-label :type :comment
                                        :from 0 :to (count explorer-label)
                                        :x sidebar-padding-x :y 28
                                        :size (:xs (:font-sizes dt))
                                        :r 0.40 :g 0.40 :b 0.48 :a 1.0}])
                  ;; Dir list items
                  dir-items
                  (if (nil? home-dirs)
                    ;; Loading state
                    [(rt-node :home-loading :text-block
                       {:x 0 :y 0 :w sb-w :h sidebar-row-h}
                       :text [{:text "Loading..." :type :comment
                               :from 0 :to 10
                               :x sidebar-padding-x :y 20
                               :size sb-font
                               :r 0.40 :g 0.40 :b 0.45 :a 0.6}])]
                    ;; Render each home dir
                    (mapv
                      (fn [i d]
                        (let [id-kw (keyword (str "home-" i))
                              name-str (str "▸ " (:name d))
                              hovered? (= id-kw hovered-id)
                              text-y (+ (/ sidebar-row-h 2) (/ sb-font 2.5))]
                          (rt-node id-kw :sidebar-entry
                            {:x 0 :y 0 :w sb-w :h sidebar-row-h}
                            :data {:entry-type :home-dir :entry d :idx i}
                            :style (when hovered?
                                     {:bg (:bg-hover colors)
                                      :radius (:md (:radii dt))})
                            :children
                            (if hovered?
                              [(rt-node (keyword (str "home-" i "-hl")) :highlight
                                 {:x sidebar-item-inset :y 2
                                  :w (- sb-w (* 2 sidebar-item-inset)) :h (- sidebar-row-h 4)}
                                 :style {:bg (:bg-hover colors)
                                         :radius (:md (:radii dt))})]
                              [])
                            :text [{:text name-str :type :text
                                    :from 0 :to (count name-str)
                                    :x sidebar-padding-x :y text-y
                                    :size sb-font
                                    :r 0.65 :g 0.65 :b 0.70 :a 1.0}])))
                      (range) home-dirs))]
              (into [header-node] dir-items))

            ;; Files mode, project selected — file tree
            :else
            (let [;; Back button
                  back-label (str "← " (:name project))
                  subtitle "Project Workspace"
                  back-node (rt-node :back-btn :sidebar-entry
                              {:x 0 :y 0 :w sb-w :h sidebar-back-h}
                              :data {:entry-type :back-btn}
                              :style {:bg (:bg-elevated colors)
                                      :border-widths [0 0 1 0]
                                      :border-color (:border-subtle colors)}
                              :text [{:text back-label :type :text
                                      :from 0 :to (count back-label)
                                      :x sidebar-padding-x :y 26
                                      :size (+ sb-font 1)
                                      :r 0.90 :g 0.90 :b 0.92 :a 1.0}
                                     {:text subtitle :type :comment
                                      :from 0 :to (count subtitle)
                                      :x (+ sidebar-padding-x 16) :y 40
                                      :size (:xs (:font-sizes dt))
                                      :r 0.55 :g 0.55 :b 0.60 :a 1.0}])
                  ;; Breadcrumb (when file is open)
                  breadcrumb-node
                  (when current-file
                    (let [fname (:name current-file)]
                      (rt-node :breadcrumb :text-block
                        {:x 0 :y 0 :w sb-w :h sidebar-breadcrumb-h}
                        :style {:bg [0.05 0.05 0.06 1.0]
                                :border-widths [0 0 1 0]
                                :border-color (:border-subtle colors)}
                        :text [{:text fname :type :comment
                                :from 0 :to (count fname)
                                :x sidebar-padding-x :y 17
                                :size (:xs (:font-sizes dt))
                                :r 0.45 :g 0.45 :b 0.50 :a 1.0}])))
                  ;; Flatten the file tree
                  root-entries (get dir-cache (:path project) [])
                  flat-rows (flatten-file-tree root-entries expanded-dirs current-file dir-cache 0)
                  ;; Build file entry nodes
                  file-nodes
                  (mapv
                    (fn [i {:keys [entry depth expanded? active?]}]
                      (let [is-dir? (= (:type entry) :dir)
                            id-kw (keyword (str "entry-" i))
                            hovered? (= id-kw hovered-id)
                            indent (* depth sidebar-indent-px)
                            chevron (cond
                                      (not is-dir?) "  "
                                      expanded?     "▾ "
                                      :else         "▸ ")
                            label (str chevron (:name entry))
                            avail-chars (max 5 (- max-chars (int (/ indent sb-char-advance))))
                            trunc (if (> (count label) avail-chars)
                                    (str (subs label 0 (- avail-chars 2)) "..")
                                    label)
                            fg-color (cond
                                       active?  [0.95 0.95 0.98 1.0]
                                       :else    (:fg colors))
                            ;; Highlight background
                            inner-bg (cond
                                       active?  (:bg-selected colors)
                                       hovered? (:bg-hover colors)
                                       :else    nil)
                            highlight (when inner-bg
                                        (rt-node (keyword (str "entry-" i "-hl")) :highlight
                                          {:x sidebar-item-inset :y 2
                                           :w (- sb-w (* 2 sidebar-item-inset)) :h (- sidebar-row-h 4)}
                                          :style {:bg inner-bg
                                                  :radius (:md (:radii dt))}))
                            ;; Active accent bar
                            accent-bar (when active?
                                         (rt-node (keyword (str "entry-" i "-acc")) :accent-bar
                                           {:x (+ sidebar-item-inset 1) :y 6
                                            :w 3 :h (- sidebar-row-h 12)}
                                           :style {:bg (:accent colors)
                                                   :radius (:sm (:radii dt))}))
                            ;; Indent guide lines (1px vertical per depth level)
                            guide-color (:border colors)
                            indent-guides
                            (when (pos? depth)
                              (mapv (fn [d]
                                      (let [gx (+ sidebar-padding-x (* d sidebar-indent-px) (/ sidebar-indent-px 2))]
                                        (rt-node (keyword (str "entry-" i "-g" d)) :indent-guide
                                          {:x gx :y 0 :w 1 :h sidebar-row-h}
                                          :style {:bg [(nth guide-color 0) (nth guide-color 1)
                                                       (nth guide-color 2) 0.10]})))
                                    (range depth)))
                            text-y (+ (/ sidebar-row-h 2) (/ sb-font 2.5))]
                        (rt-node id-kw :sidebar-entry
                          {:x 0 :y 0 :w sb-w :h sidebar-row-h}
                          :data {:entry-type (if is-dir? :dir :file) :entry entry :idx i
                                 :expanded? expanded? :active? active? :depth depth}
                          :children (cond-> []
                                      highlight     (conj highlight)
                                      accent-bar    (conj accent-bar)
                                      indent-guides (into indent-guides))
                          :text [{:text trunc :type :text
                                  :from 0 :to (count trunc)
                                  :x (+ sidebar-padding-x indent) :y text-y
                                  :size sb-font
                                  :r (nth fg-color 0) :g (nth fg-color 1)
                                  :b (nth fg-color 2) :a (nth fg-color 3)}])))
                    (range) flat-rows)]
              (cond-> [back-node]
                breadcrumb-node (conj breadcrumb-node)
                true (into file-nodes))))

          ;; Scrollable content area (clips children)
          ;; Inner scroll container: offset by -scroll-y, layout positions children,
          ;; outer clip-node hides overflow
          scroll-inner (rt-node :sidebar-scroll-inner :container
                         {:x 0 :y (- sidebar-scroll-y) :w sb-w :h 99999}
                         :layout {:direction :column}
                         :children (vec content-children))
          content-node (rt-node :sidebar-content :panel-content
                         {:x 0 :y content-top :w sb-w :h content-h}
                         :clip? true
                         :children [scroll-inner])]

      ;; Root node: pinned to viewport via scroll-y
      (rt-node :sidebar-root :panel
        {:x 0 :y scroll-y :w sb-w :h viewport-h}
        :style {:bg (:bg colors)}
        :children [border-node tabs-node content-node]))))

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
      ;; No tickets loaded — centered card prompt
      (empty? tickets)
      (let [card-w (- right-w (* 2 (:xl (:spacing dt))))
            card-h 80
            card-x (:xl (:spacing dt))
            card-y (- cy (/ card-h 2))]
        [(ui-card :empty-card
           {:x card-x :y card-y :w card-w :h card-h}
           :shadow :md
           :text [{:text "No tickets found." :type :text
                   :from 0 :to 18
                   :x (:lg (:spacing dt)) :y 28
                   :size font-size
                   :r 0.60 :g 0.60 :b 0.65 :a 0.8}
                  {:text "Run /bootstrap to fetch Linear tickets." :type :comment
                   :from 0 :to 39
                   :x (:lg (:spacing dt)) :y 52
                   :size (- font-size 1)
                   :r 0.50 :g 0.50 :b 0.55 :a 0.6}])])

      ;; Nothing selected — centered card prompt
      (empty? selected)
      (let [card-w (- right-w (* 2 (:xl (:spacing dt))))
            card-h 100
            card-x (:xl (:spacing dt))
            card-y (- cy (/ card-h 2))]
        [(ui-card :no-sel-card
           {:x card-x :y card-y :w card-w :h card-h}
           :shadow :md
           :text [{:text "No tickets selected yet." :type :text
                   :from 0 :to 24
                   :x (:lg (:spacing dt)) :y 28
                   :size font-size
                   :r 0.60 :g 0.60 :b 0.65 :a 0.8}
                  {:text "Select tickets from the list," :type :comment
                   :from 0 :to 29
                   :x (:lg (:spacing dt)) :y 52
                   :size (- font-size 1)
                   :r 0.50 :g 0.50 :b 0.55 :a 0.6}
                  {:text "then /arrange sequential|parallel." :type :comment
                   :from 0 :to 35
                   :x (:lg (:spacing dt)) :y 72
                   :size (- font-size 1)
                   :r 0.50 :g 0.50 :b 0.55 :a 0.6}])])

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
                               :size (+ font-size 2)
                               :r 0.85 :g 0.85 :b 0.88 :a 1.0})
                            (range) tlines)
            desc-ops  (mapv (fn [i l]
                              {:text l :type :text
                               :from 0 :to (count l)
                               :x list-padding-x :y (+ desc-y (* i dlh))
                               :size (- font-size 1)
                               :r 0.70 :g 0.70 :b 0.73 :a 0.9})
                            (range) dlines)
            all-ops  (into (into title-ops
                             [{:text mline :type :comment
                               :from 0 :to (count mline)
                               :x list-padding-x :y meta-y
                               :size (- font-size 1)
                               :r 0.55 :g 0.55 :b 0.60 :a 0.8}
                              {:text htext :type :comment
                               :from 0 :to (count htext)
                               :x list-padding-x :y hint-y
                               :size (- font-size 2)
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
                            :size (- font-size 1)
                            :r 0.70 :g 0.75 :b 0.80 :a 0.9}))
                       (range) selected)
            hy   (+ 88 (* n 20) 16)
            htxt "/arrange sequential|parallel to proceed"]
        [(rt-node :multi-detail :text-block
           {:x 0 :y 0 :w right-w :h viewport-h}
           :text (into [{:text ctxt :type :macro
                         :from 0 :to (count ctxt)
                         :x list-padding-x :y 52
                         :size (+ font-size 2)
                         :r 0.75 :g 0.80 :b 0.95 :a 1.0}
                        {:text htxt :type :comment
                         :from 0 :to (count htxt)
                         :x list-padding-x :y hy
                         :size (- font-size 2)
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
                     :y (+ (/ list-padding-top 2) (/ font-size 2.5))
                     :size font-size
                     :r 0.75 :g 0.80 :b 0.95 :a 1.0}])
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
   MODE-SWITCH: when flow canvas is active, returns ticket card rects instead.
   SIDEBAR: when sidebar visible, sidebar rects prepended, content offset right."
  [!editor-doc !eval-result !caret-visible !focus !settings !active-font !viewport
   <fold-data <bracket-data
   !flow-state !scroll-y !collapsed-groups !hovered-row-idx !drag-state
   !sidebar-state !sidebar-visible !current-file
   layout-x layout-y gutter-w]
  (m/latest
    (fn [doc fold-state bracket-match eval-result caret-visible focus settings active-font viewport
         flow-state scroll-y collapsed-groups hovered-row-idx drag-state
         sidebar-state sidebar-visible? current-file]
      (let [sb-vis? (boolean sidebar-visible?)
            sb-w (if sb-vis? sidebar-w 0)
            dpr (:dpr viewport)
            snap? (:snap-to-pixel? settings)
            font-size (:font-size settings)
            char-advance (maybe-snap (* font-size (:char-width active-font)) dpr snap?)
            ;; Build sidebar rects when visible
            sidebar-result
            (when sb-vis?
              (let [tree (resolve-layout
                           (build-sidebar-tree sidebar-state current-file true
                                               (:height viewport) scroll-y font-size char-advance))]
                (when tree
                  {:rects (tree->rects tree)
                   :shadows (tree->shadows tree)})))
            ;; Build content rects (offset by sb-w)
            content-result
            (if (flow-canvas-active? flow-state)
              (compute-ticket-list-rects flow-state (- (:width viewport) sb-w) (:height viewport)
                                         scroll-y hovered-row-idx collapsed-groups drag-state)
              (let [line-h (maybe-snap (* font-size (:line-height settings)) dpr snap?)
                    lx (maybe-snap layout-x dpr snap?)
                    ly (maybe-snap layout-y dpr snap?)]
                {:rects (compute-editor-rects doc fold-state bracket-match eval-result caret-visible focus
                                              lx ly line-h gutter-w char-advance (- (:width viewport) sb-w))
                 :shadows []}))]
        {:rects (into (or (:rects sidebar-result) [])
                      (offset-rects (:rects content-result) sb-w))
         :shadows (into (or (:shadows sidebar-result) [])
                        (offset-shadows (:shadows content-result) sb-w))}))
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
    (m/watch !hovered-row-idx)
    (m/watch !drag-state)
    (m/watch !sidebar-state)
    (m/watch !sidebar-visible)
    (m/watch !current-file)))

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
   Zero-size invisible rects for absent elements keep GPU indices stable.
   SIDEBAR: backgrounds span full viewport, caret offset by sb-w."
  [!cmd-panel !focus !caret-visible !scroll-y !viewport !settings !active-font
   !ai-provider !agent-output !sidebar-visible cmd-panel-h status-bar-h]
  (m/latest
    (fn [panel focus caret-visible scroll-y viewport settings active-font
         agent-output sidebar-visible?]
      (let [sb-w (if (boolean sidebar-visible?) sidebar-w 0)
            dpr (:dpr viewport)
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

            ;; --- Instance 2: caret (offset by sidebar width) ---
            text-x (+ (cmd-text-start-x @!ai-provider font-size (:char-width active-font) dpr snap?) sb-w)
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
    (m/watch !agent-output)
    (m/watch !sidebar-visible)))

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
   MODE-SWITCH: when flow canvas is active, returns ticket card text ops instead.
   SIDEBAR: when sidebar visible, sidebar text ops prepended, content offset right."
  [!editor-doc !cmd-panel !ai-provider !agent-output !agent-scroll-y !scroll-y !viewport !settings !active-font
   !current-file
   tokenize-fn layout-fn
   <fold-data
   !flow-state !collapsed-groups !hovered-row-idx !drag-state
   !sidebar-state !sidebar-visible
   layout-x layout-y cmd-panel-h status-bar-h]
  (m/latest
    (fn [doc panel provider agent-output agent-scroll-y scroll-y viewport fold-state settings active-font
         current-file flow-state collapsed-groups hovered-row-idx drag-state
         sidebar-state sidebar-visible?]
      (let [sb-vis? (boolean sidebar-visible?)
            sb-w (if sb-vis? sidebar-w 0)
            dpr (:dpr viewport)
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
              ;; Content viewport width (minus sidebar)
              content-vw (- (:width viewport) sb-w)

              ;; Sidebar text ops
              sidebar-text-ops
              (when sb-vis?
                (let [tree (resolve-layout
                             (build-sidebar-tree sidebar-state current-file true
                                                 (:height viewport) scroll-y font-size char-advance))]
                  (when tree (tree->text-ops tree))))

              ;; MODE-SWITCH: flow canvas replaces editor ops with ticket card text
              [editor-ops final-line-mapping line-num-ops]
              (if (flow-canvas-active? flow-state)
                ;; Flow canvas mode: master-detail list view text
                [(compute-ticket-list-text-ops flow-state content-vw (:height viewport)
                                               font-size char-advance scroll-y
                                               hovered-row-idx collapsed-groups drag-state)
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

              ;; Offset editor/flow text ops by sidebar width
              offset-editor-ops (offset-text-ops editor-ops sb-w)
              offset-line-num-ops (offset-text-ops line-num-ops sb-w)

              ;; Command panel ops (if visible) — offset by sb-w
              cmd-ops (when (:visible panel)
                        (let [cmd-panel-y (maybe-snap (+ scroll-y (- (:height viewport) cmd-panel-h status-bar-h)) dpr snap?)
                              cmd-text-y (maybe-snap (+ cmd-panel-y 12 font-size) dpr snap?)
                              prompt-text (cmd-prompt-text provider)
                              prompt-x (maybe-snap (+ 24 sb-w) dpr snap?)
                              text-x (+ (cmd-text-start-x provider font-size (:char-width active-font) dpr snap?) sb-w)]
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
                agent-x-px (+ 24 sb-w)
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
                agent-x (maybe-snap (+ 24 sb-w) dpr snap?)
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

                ;; Status bar text (always visible, pinned to bottom) — offset by sb-w
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
                                :x (maybe-snap (+ 16 sb-w) dpr snap?) :y status-y
                                :size font-size
                                :r 0.65 :g 0.65 :b 0.65 :a 0.9}]
                              ;; Right: filename | PROVIDER
                              [{:text status-right-text
                                :type :comment
                                :from 0 :to (count status-right-text)
                                :x status-right-x :y status-y
                                :size font-size
                                :r 0.65 :g 0.65 :b 0.65 :a 0.9}]]]

            {:render-ops (vec (concat (or sidebar-text-ops [])
                                      offset-line-num-ops offset-editor-ops
                                      cmd-lines agent-lines status-lines))
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
    (m/watch !hovered-row-idx)
    (m/watch !drag-state)
    (m/watch !sidebar-state)
    (m/watch !sidebar-visible)))

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
                                                    (clj->js {:size (* capacity editor/rect-stride)
                                                              :usage (bit-or js/GPUBufferUsage.VERTEX
                                                                             js/GPUBufferUsage.COPY_DST)}))]
                              {:pipeline (:pipeline (:rect geometry))
                               :bind-group (:bind-group (:rect geometry))
                               :instance-buffer instance-buffer
                               :num-instances 0}))
        !settings-rect-sys (atom (let [capacity 32
                                        instance-buffer (.createBuffer device
                                                          (clj->js {:size (* capacity editor/rect-stride)
                                                                    :usage (bit-or js/GPUBufferUsage.VERTEX
                                                                                   js/GPUBufferUsage.COPY_DST)}))]
                                    {:pipeline (:pipeline (:rect geometry))
                                     :bind-group (:bind-group (:rect geometry))
                                     :instance-buffer instance-buffer
                                     :num-instances 0}))
        !shadow-sys (atom (:shadow geometry))

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
        ;; V0: default to discourse-graph entry point
        !current-file (atom {:path "/home/sid/projects/discourse-graph/apps/roam/src/index.ts"
                             :name "index.ts"})
        ;; Consolidated sidebar state (replaces 9 individual atoms)
        !sidebar-state (atom {:mode :files           ;; :files | :review-packs
                              :project nil           ;; {:name :path} or nil
                              :expanded-dirs #{}
                              :dir-cache {}
                              :home-dirs nil
                              :scroll-y 0
                              :hovered-id nil
                              :loading? false
                              ;; Review pack sub-state
                              :review-pack-list nil
                              :review-pack-loading? false
                              :review-pack-selected-id nil
                              :review-pack-selected-node-id nil
                              :review-pack-summary-cache {}
                              :review-pack-load-error nil})
        !ai-provider (atom :claude)     ;; :claude | :codex | :gemini
        !agent-output (atom nil)        ;; {:status :provider :prompt :output :run-id :trail :tool-buf}
        !agent-scroll-y (atom 0)        ;; scroll offset within agent output panel
        !mouse-x (atom 0)               ;; last known mouse X (viewport-relative)
        !mouse-y (atom 0)               ;; last known mouse Y (viewport-relative)
        !flow-state (atom (initial-flow-state))  ;; V0 flow state machine
        !collapsed-groups (atom #{})           ;; set of collapsed group status strings
        !hovered-row-idx (atom nil)            ;; 0-based flat ticket index under mouse cursor
        !drag-state (atom {:phase :idle})      ;; drag state machine: :idle/:pending/:dragging

        ;; sidebar-el removed — sidebar now rendered via WebGPU rect tree

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
        (fn []
          (when (nil? (:home-dirs @!sidebar-state))
            (fetch-edn! "/api/home-dirs"
                        (fn [dirs]
                          (swap! !sidebar-state assoc :home-dirs dirs)))))

        fetch-dir!
        (fn [path]
          (when-not (contains? (:dir-cache @!sidebar-state) path)
            (fetch-edn! (str "/api/list-dir?path=" (js/encodeURIComponent path))
                        (fn [entries]
                          (swap! !sidebar-state assoc-in [:dir-cache path] entries)))))

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
        (fn []
          (when-not (:review-pack-loading? @!sidebar-state)
            (swap! !sidebar-state assoc :review-pack-loading? true)
            (fetch-edn!
              "/api/review-pack/list"
              (fn [result]
                (if (:ok result)
                  (let [packs (vec (or (:review-packs result) []))
                        selected-id (:review-pack-selected-id @!sidebar-state)
                        selected-exists? (some #(= (:pack-id %) selected-id) packs)]
                    (swap! !sidebar-state assoc
                           :review-pack-loading? false
                           :review-pack-load-error nil
                           :review-pack-list packs)
                    (cond
                      (and (seq packs) (nil? selected-id))
                      (swap! !sidebar-state assoc
                             :review-pack-selected-id (:pack-id (first packs))
                             :review-pack-selected-node-id nil)

                      (and (seq packs) (not selected-exists?))
                      (swap! !sidebar-state assoc
                             :review-pack-selected-id (:pack-id (first packs))
                             :review-pack-selected-node-id nil)

                      (empty? packs)
                      (swap! !sidebar-state assoc
                             :review-pack-selected-id nil
                             :review-pack-selected-node-id nil)))
                  (swap! !sidebar-state assoc
                         :review-pack-loading? false
                         :review-pack-load-error (or (:message result) "Failed to load review packs")
                         :review-pack-list []
                         :review-pack-selected-id nil
                         :review-pack-selected-node-id nil)))
              (fn [err]
                (swap! !sidebar-state assoc
                       :review-pack-loading? false
                       :review-pack-load-error (str "Fetch failed: " (.-message err))
                       :review-pack-list []
                       :review-pack-selected-id nil
                       :review-pack-selected-node-id nil)))))

        fetch-review-pack-summary!
        (fn [pack-id]
          (when (and pack-id
                     (not (contains? (:review-pack-summary-cache @!sidebar-state) pack-id)))
            (fetch-edn!
              (str "/api/review-pack/" (js/encodeURIComponent pack-id) "/summary")
              (fn [result]
                (swap! !sidebar-state assoc-in [:review-pack-summary-cache pack-id]
                       (if (:ok result)
                         result
                         {:ok false :message (or (:message result) "Failed to load review pack summary")})))
              (fn [err]
                (swap! !sidebar-state assoc-in [:review-pack-summary-cache pack-id]
                       {:ok false :message (str "Fetch failed: " (.-message err))})))))

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

              :flow-mock-bootstrap
              (let [flow @!flow-state
                    mock-tickets [{:id "DIS-101" :title "Fix SSO auth flow for enterprise users"
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
                                   :description "Tag and release the v0.2.0 milestone including streaming agent output, rect tree UI, and the intake list view."}]
                    can-transition (or (= (:node flow) :idle)
                                      (= (:node flow) :intake))]
                (if can-transition
                  (do (reset! !flow-state (assoc (initial-flow-state)
                                                 :node :intake
                                                 :tickets mock-tickets))
                      (reset! !scroll-y 0)
                      (show-flow-info!
                        (str "Mock bootstrap complete. " (count mock-tickets) " tickets loaded.")))
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



        ;; Initial sidebar data fetch (reactive — m/watch triggers re-render)
        _ (when (and !sidebar-visible @!sidebar-visible)
            (if (= (:mode @!sidebar-state) :review-packs)
              (when (nil? (:review-pack-list @!sidebar-state))
                (fetch-review-packs!))
              (fetch-home-dirs!)))
        ;; Fetch data when sidebar becomes visible
        _ (when !sidebar-visible
            (add-watch !sidebar-visible :sidebar-fetch
                       (fn [_ _ old-vis new-vis]
                         (when (and new-vis (not old-vis))
                           (if (= (:mode @!sidebar-state) :review-packs)
                             (when (nil? (:review-pack-list @!sidebar-state))
                               (fetch-review-packs!))
                             (fetch-home-dirs!))))))

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
                  (swap! !sidebar-state assoc
                         :project {:name (last (str/split project-path #"/"))
                                   :path project-path}
                         :expanded-dirs (set dir-paths))
                  ;; Fetch root dir + expanded dirs so tree renders with content
                  (fetch-dir! project-path)
                  (doseq [dp dir-paths]
                    (fetch-dir! dp))))))

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
                             sb-vis? (and !sidebar-visible @!sidebar-visible)
                             mouse-x @!mouse-x
                             in-sidebar? (and sb-vis? (< mouse-x sidebar-w))
                             agent-output @!agent-output
                             agent-h (compute-agent-panel-h agent-output font-size
                                                            (:height viewport) (:width viewport)
                                                            char-advance)
                             ;; Agent panel Y bounds (viewport-relative, no scroll offset)
                             agent-y0 (- (:height viewport) cmd-panel-h status-bar-h agent-h 12)
                             agent-y1 (- (:height viewport) cmd-panel-h status-bar-h)
                             mouse-y @!mouse-y
                             in-agent? (and (not in-sidebar?)
                                            (pos? agent-h)
                                            (>= mouse-y agent-y0)
                                            (< mouse-y agent-y1))]
                         (cond
                           ;; Scroll sidebar file tree
                           in-sidebar?
                           (let [ss @!sidebar-state
                                 content-h (compute-sidebar-content-height ss @!current-file)
                                 visible-h (- (:height viewport) sidebar-tab-h)
                                 max-scroll (max 0 (- content-h visible-h))]
                             (swap! !sidebar-state update :scroll-y
                                    #(-> (+ (or % 0) delta) (max 0) (min max-scroll))))
                           ;; Scroll agent panel
                           in-agent?
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
                           :else
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

                     ;; Not in settings - check sidebar, status bar, command panel, or editor
                     (let [sb-vis? (and !sidebar-visible @!sidebar-visible)
                           clicked-in-sidebar? (and sb-vis? (< x sidebar-w))]
                       (if clicked-in-sidebar?
                         ;; Click in sidebar — hit-test the sidebar tree
                         (let [ss @!sidebar-state
                               font-size (:font-size @!settings)
                               char-advance (* font-size (:char-width @!active-font))
                               tree (resolve-layout
                                      (build-sidebar-tree ss @!current-file true
                                                          (:height viewport) scroll-y font-size char-advance))
                               path (when tree (hit-test tree x (+ y scroll-y)))]
                           (when path
                             (some (fn [node]
                                     (case (:type node)
                                       ;; Tab click — switch mode
                                       :tab
                                       (let [tab-id (:tab-id (:data node))]
                                         (when tab-id
                                           (swap! !sidebar-state assoc :mode tab-id)
                                           ;; Fetch review packs if switching to that mode
                                           (when (and (= tab-id :review-packs)
                                                      (nil? (:review-pack-list @!sidebar-state)))
                                             (fetch-review-packs!)))
                                         true)
                                       ;; File/dir entry click
                                       :sidebar-entry
                                       (let [d (:data node)
                                             et (:entry-type d)]
                                         (case et
                                           :back-btn
                                           (do (swap! !sidebar-state assoc
                                                      :project nil
                                                      :expanded-dirs #{}
                                                      :dir-cache {}
                                                      :scroll-y 0)
                                               (reset! !current-file nil)
                                               true)
                                           :home-dir
                                           (let [entry (:entry d)]
                                             (swap! !sidebar-state assoc
                                                    :project {:name (:name entry) :path (:path entry)}
                                                    :expanded-dirs #{}
                                                    :dir-cache {}
                                                    :scroll-y 0)
                                             (fetch-dir! (:path entry))
                                             true)
                                           :dir
                                           (let [entry (:entry d)
                                                 path (:path entry)]
                                             (swap! !sidebar-state update :expanded-dirs
                                                    (fn [dirs]
                                                      (if (contains? dirs path)
                                                        (disj dirs path)
                                                        (conj dirs path))))
                                             (fetch-dir! path)
                                             true)
                                           :file
                                           (let [entry (:entry d)
                                                 project (:project @!sidebar-state)]
                                             (fetch-file! (:path entry) (:path project))
                                             true)
                                           ;; Unknown entry type
                                           nil))
                                       ;; Other node types — skip
                                       nil))
                                   (rseq path))))
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
                         ;; Click in command panel - use reactive font values (offset by sidebar)
                         (let [font-size (:font-size @!settings)
                              dpr (:dpr @!viewport)
                              snap? (:snap-to-pixel? @!settings)
                              char-width (:char-width @!active-font)
                              char-w (maybe-snap (* font-size char-width) dpr snap?)
                              sb-w (if sb-vis? sidebar-w 0)
                              text-x (+ (cmd-text-start-x @!ai-provider font-size char-width dpr snap?) sb-w)
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
                          ;; Flow canvas mode: rect tree hit-test
                          (let [flow @!flow-state
                                tree (resolve-layout
                                       (build-intake-tree flow (:width viewport) (:height viewport)
                                                          scroll-y nil @!collapsed-groups 0 0 nil))
                                path (hit-test tree x (+ y scroll-y))]
                            ;; Walk path innermost→outermost, handle first recognized type
                            (when path
                              (some (fn [node]
                                      (case (:type node)
                                        :group-header
                                        ;; Immediate — not draggable
                                        (let [status (:status (:data node))]
                                          (swap! !collapsed-groups
                                                 (fn [cg] (if (contains? cg status)
                                                            (disj cg status)
                                                            (conj cg status))))
                                          true)
                                        :ticket-row
                                        ;; Enter PENDING — defer click vs drag to mouseup/mousemove
                                        (do (reset! !drag-state
                                                    {:phase :pending
                                                     :origin {:x x :y y}
                                                     :node node})
                                            true)
                                        ;; Other node types — skip, let it bubble
                                        nil))
                                    (rseq path))))
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
                                (reset! !focus :editor))))))))))))))

                 :mousemove
                 (do (reset! !mouse-x (:x coords))
                     (reset! !mouse-y (:y coords))
                 ;; --- Sidebar hover tracking ---
                 (let [sb-vis? (and !sidebar-visible @!sidebar-visible)
                       in-sidebar? (and sb-vis? (< (:x coords) sidebar-w))]
                   (if in-sidebar?
                     ;; Hit-test sidebar for hover
                     (let [ss @!sidebar-state
                           font-size (:font-size @!settings)
                           char-advance (* font-size (:char-width @!active-font))
                           tree (resolve-layout
                                  (build-sidebar-tree ss @!current-file true
                                                      (:height @!viewport) @!scroll-y font-size char-advance))
                           path (when tree (hit-test tree (:x coords) (+ (:y coords) @!scroll-y)))
                           target (peek path)
                           new-id (when (and target (= (:type target) :sidebar-entry))
                                    (:id target))]
                       (when (not= new-id (:hovered-id @!sidebar-state))
                         (swap! !sidebar-state assoc :hovered-id new-id)))
                     ;; Clear sidebar hover when outside
                     (when (:hovered-id @!sidebar-state)
                       (swap! !sidebar-state assoc :hovered-id nil))))
                 ;; --- Drag state machine transitions ---
                 (let [ds @!drag-state
                       mx (:x coords) my (:y coords)]
                   (case (:phase ds)
                     :pending
                     (when (> (drag-distance ds mx my) drag-threshold-px)
                       (reset! !drag-state
                               {:phase :dragging
                                :origin (:origin ds)
                                :node (:node ds)
                                :current {:x mx :y my}}))
                     :dragging
                     (swap! !drag-state assoc :current {:x mx :y my})
                     nil))
                 ;; Hover tracking — suppress during drag
                 (when (and (flow-canvas-active? @!flow-state)
                            (= :idle (:phase @!drag-state)))
                   (let [flow @!flow-state
                         tree (resolve-layout
                                (build-intake-tree flow (:width @!viewport) (:height @!viewport)
                                                   @!scroll-y nil @!collapsed-groups 0 0 nil))
                         path (hit-test tree (:x coords) (+ (:y coords) @!scroll-y))
                         target (peek path)]
                     (reset! !hovered-row-idx
                             (when (and target (= (:type target) :ticket-row))
                               (:idx (:data target))))))
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
                 (let [ds @!drag-state]
                   (case (:phase ds)
                     :pending
                     ;; Under threshold — treat as click (toggle selection)
                     (do (let [node (:node ds)]
                           (when (= :ticket-row (:type node))
                             (let [idx (:idx (:data node))
                                   flow @!flow-state
                                   selected (:selected flow)
                                   already? (some #{idx} selected)
                                   new-sel (if already?
                                             (vec (remove #{idx} selected))
                                             (conj (vec selected) idx))]
                               (swap! !flow-state assoc :selected new-sel))))
                         (reset! !drag-state {:phase :idle}))
                     :dragging
                     ;; Past threshold — check drop zone
                     (let [node (:node ds)
                           cur (:current ds)
                           left-w (int (* (:width @!viewport) list-left-pane-pct))]
                       (when (and node cur (= :ticket-row (:type node)))
                         (if (>= (:x cur) left-w)
                           ;; Dropped on right pane → select ticket (cross-panel DnD)
                           (let [idx (:idx (:data node))
                                 flow @!flow-state
                                 selected (:selected flow)
                                 already? (some #{idx} selected)]
                             (when-not already?
                               (swap! !flow-state assoc :selected
                                      (conj (vec selected) idx))))
                           ;; Dropped on left pane → reorder (future)
                           nil))
                       (reset! !drag-state {:phase :idle}))
                     ;; :idle — normal mouseup (editor text selection)
                     (reset! !dragging? false))))
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
                                           !flow-state !collapsed-groups !hovered-row-idx !drag-state
                                           !sidebar-state !sidebar-visible
                                           layout-x layout-y cmd-panel-h status-bar-h)
            <editor-rect-data (<editor-rects !editor-doc !eval-result !caret-visible !focus
                                             !settings !active-font !viewport
                                             <fold-data <bracket-data
                                             !flow-state !scroll-y !collapsed-groups !hovered-row-idx !drag-state
                                             !sidebar-state !sidebar-visible !current-file
                                             layout-x layout-y gutter-w)
            <cmd-rect-data (<cmd-panel-rects !cmd-panel !focus !caret-visible !scroll-y !viewport
                                             !settings !active-font
                                             !ai-provider !agent-output !sidebar-visible
                                             cmd-panel-h status-bar-h)
            ;; Settings panel flows (reactive: derive font-size from !settings internally)
            <settings-rect-data (<settings-panel-rects !settings !focus !viewport !scroll-y !font-manifest)
            <settings-text-data (<settings-panel-text !settings !viewport !scroll-y !font-manifest)

            ;; Combined world state - sampled on RAF
            ;; m/latest combines flows, m/sample synchronizes with frame clock
            ;; All derived flows now use m/latest internally, so they're continuous
            <world-snapshot (m/latest
                              (fn [text-data editor-rect-data cmd-rects settings-rects settings-text
                                   viewport scroll-y cmd-panel settings active-font agent-output]
                                {:text-data text-data
                                 :editor-rect-data editor-rect-data
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
                (let [{:keys [text-data editor-rect-data cmd-rects settings-rects settings-text
                              viewport scroll-y cmd-visible agent-visible settings-visible
                              font-size px-range line-height sharpness char-width
                              snap-to-pixel? show-diagnostics?]} world
                      editor-rects   (:rects editor-rect-data)
                      editor-shadows (:shadows editor-rect-data)

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

                      ;; Upload shadows (only if changed)
                      new-shadow-sys (if (not (identical? editor-shadows (:prev-editor-shadows prev-state)))
                                       (editor/update-shadows device
                                                              (or (:shadow-sys prev-state) @!shadow-sys)
                                                              (or editor-shadows []))
                                       (:shadow-sys prev-state))

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
                                      :agent-visible agent-visible
                                      :shadow-sys new-shadow-sys)

                  ;; Return state for next frame comparison
                  {:text-geo new-text-geo
                       :editor-rect-sys new-editor-sys
                       :cmd-rect-sys new-cmd-sys
                       :settings-rect-sys new-settings-sys
                       :shadow-sys new-shadow-sys
                       :prev-world world
                       :prev-text-data text-data
                       :prev-settings-text settings-text
                       :prev-show-diagnostics show-diagnostics?
                       :prev-scroll-y scroll-y
                       :prev-text-ops all-text-ops
                       :prev-editor-rects editor-rects
                       :prev-editor-shadows editor-shadows
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
           :shadow-sys @!shadow-sys
           :prev-world nil
           :prev-text-data nil
           :prev-settings-text nil
           :prev-show-diagnostics nil
           :prev-scroll-y nil
           :prev-text-ops nil
           :prev-editor-rects nil
           :prev-editor-shadows nil
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
