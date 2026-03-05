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
            [app.client.webgpu.themes :as themes]
            [components.design-tokens :as design-tokens]))

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
  "Flow that emits wheel delta values as {:dy N :dx N}"
  (->> (m/observe
         (fn [!]
           (let [handler (fn [e]
                           (.preventDefault e)
                           (! {:dy (.-deltaY e)
                               :dx (.-deltaX e)
                               :shift? (.-shiftKey e)}))]
             (.addEventListener node "wheel" handler #js {:passive false})
             #(.removeEventListener node "wheel" handler))))
       (m/relieve (fn [a b] {:dy (+ (:dy a) (:dy b))
                              :dx (+ (:dx a) (:dx b))
                              :shift? (:shift? b)}))))

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
      ;; Pane focus shortcuts
      (and ctrl? (= key "1")) {:type :focus-pane :pane :editor :global? true}
      (and ctrl? (= key "2")) {:type :focus-pane :pane :chat :global? true}
      (and ctrl? (= key "3")) {:type :focus-pane :pane :preview :global? true}

      ;; Escape - context dependent but handled globally
      (= key "Escape") {:type :escape :global? true}

      ;; Editor-specific shortcuts
      (and ctrl? (= key "Enter")) {:type :eval}
      (and ctrl? (= key "z") (not shift?)) {:type :undo}
      (and ctrl? (= key "z") shift?) {:type :redo}
      (and ctrl? (= key "y")) {:type :redo}
      (and ctrl? (= key "c")) {:type :copy}
      (and ctrl? (= key "x")) {:type :cut}
      ;; Ctrl+V: return nil so .preventDefault is NOT called — lets browser fire native paste event

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

(defn <chat-input-keys
  "Flow of keyboard events routed to chat input (when focused).
   Uses deref instead of m/watch to avoid cancellation on focus change."
  [>keyboard !focus]
  (->> >keyboard
       (m/eduction (filter (fn [event]
                             (and (= @!focus :chat)
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

      (str/starts-with? trimmed "/extract ")
      (let [args (-> trimmed (subs (count "/extract ")) str/trim)]
        (if (str/blank? args)
          {:kind :error :message "Usage: /extract [url] [selector]\n  /extract https://site.com .card\n  /extract .card\n  /extract clear"}
          (if (= args "clear")
            {:kind :extract-component :clear? true}
            (let [parts (str/split args #"\s+" 2)
                  first-part (first parts)]
              (if (str/starts-with? first-part "http")
                {:kind :extract-component :url first-part :selector (second parts)}
                {:kind :extract-component :selector args})))))

      (= trimmed "/extract")
      {:kind :extract-component}

      (str/starts-with? trimmed "/hardcode ")
      {:kind :hardcode :name (-> trimmed (subs (count "/hardcode ")) str/trim)}

      (= trimmed "/hardcode")
      {:kind :hardcode :name "button"}

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
       (let [;; Clip bounds: right + vertical (top/bottom) for text op filtering
             clip-right (when clip-bounds (+ (:x clip-bounds) (:w clip-bounds)))
             clip-top (when clip-bounds (:y clip-bounds))
             clip-bottom (when clip-bounds (+ (:y clip-bounds) (:h clip-bounds)))
             truncate-op (fn [op]
                           (if (and clip-right (:text op))
                             (let [ox (:x op 0)
                                   fs (:size op 14)
                                   cw (* fs 0.56)
                                   avail (- clip-right ox)
                                   max-chars (if (pos? cw) (max 0 (int (/ avail cw))) 1000)
                                   txt (:text op)]
                               (if (> (count txt) max-chars)
                                 (assoc op :text (subs txt 0 max-chars) :to max-chars)
                                 op))
                             op))
             in-clip? (fn [shifted]
                        (and (or (nil? clip-right) (< (:x shifted) clip-right))
                             (or (nil? clip-top) (>= (:y shifted) clip-top))
                             (or (nil? clip-bottom) (< (:y shifted) clip-bottom))))
             ;; Offset this node's text ops to absolute space + clip truncation
             own-ops (when (seq text)
                       (mapv (fn [op]
                               (if (vector? op)
                                 ;; op is already a vec of text-op maps (nested format)
                                 (into [] (keep (fn [sub]
                                                  (let [shifted (-> sub
                                                                    (update :x + abs-x)
                                                                    (update :y + abs-y))]
                                                    (when (in-clip? shifted)
                                                      (truncate-op shifted)))))
                                       op)
                                 ;; Single text-op map
                                 (let [shifted (-> op
                                                   (update :x + abs-x)
                                                   (update :y + abs-y))]
                                   (when (in-clip? shifted)
                                     [(truncate-op shifted)]))))
                             text))
             child-clip (if clip?
                          {:x abs-x :y abs-y :w w :h h}
                          clip-bounds)
             child-ops (into [] (mapcat #(tree->text-ops % abs-x abs-y child-clip)) children)]
         (into (vec (filterv some? (or own-ops []))) child-ops))))))

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
(def sidebar-item-inset 8)
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
  (let [{:keys [project expanded-dirs dir-cache home-dirs]} sidebar-state]
    (if (nil? project)
      ;; Home dirs list
      (* (count (or home-dirs [])) sidebar-row-h)
      ;; File tree
      (let [root-entries (get dir-cache (:path project) [])
            flat (flatten-file-tree root-entries expanded-dirs current-file dir-cache 0)]
        (* (count flat) sidebar-row-h)))))

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
  "Design tokens — imported from shared components.design-tokens."
  design-tokens/dt)

;; --- Typography hierarchy ----------------------------------------------------
;; Consistent type scale: reference these instead of ad-hoc font sizes/alphas.

(def typo-title    {:size (:xl (:font-sizes dt)) :a 1.0})
(def typo-subtitle {:size (:lg (:font-sizes dt)) :a 0.9})
(def typo-body     {:size (:md (:font-sizes dt)) :a 0.85})
(def typo-caption  {:size (:sm (:font-sizes dt)) :a 0.6})

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

(defn build-empty-state
  "Centered empty state with icon, headline, description.
   Returns an rt-node positioned at center of w x h bounds.
   icon: 2-char text symbol (e.g. \"--\", \"[]\", \"<>\")
   headline: main message text
   description: secondary guidance text"
  [id w h {:keys [icon headline description]}]
  (let [surfaces (:surfaces dt)
        colors (:colors dt)
        text-sec (or (:text-secondary surfaces) (:fg-muted colors))
        text-mut (or (:text-muted surfaces) (:fg-subtle colors))
        cx (/ w 2)
        ;; Center vertically, offset upward slightly for visual balance
        cy (- (/ h 2) 40)
        icon-size (:size typo-title)
        head-size (:size typo-subtitle)
        desc-size (:size typo-body)
        icon-w (* (count (or icon "")) icon-size 0.56)
        head-w (* (count (or headline "")) head-size 0.56)
        desc-lines (when description
                     (let [max-chars (max 20 (int (/ (* w 0.6) (* desc-size 0.56))))]
                       (wrap-line description max-chars)))]
    (rt-node id :empty-state
      {:x 0 :y 0 :w w :h h}
      :text
      (cond-> []
        ;; Icon
        icon
        (conj {:text icon :type :comment
               :from 0 :to (count icon)
               :x (- cx (/ icon-w 2)) :y cy
               :size icon-size
               :r (nth text-mut 0) :g (nth text-mut 1)
               :b (nth text-mut 2) :a (nth text-mut 3 0.5)})
        ;; Headline
        headline
        (conj {:text headline :type :text
               :from 0 :to (count headline)
               :x (- cx (/ head-w 2)) :y (+ cy 32)
               :size head-size
               :r (nth text-sec 0) :g (nth text-sec 1)
               :b (nth text-sec 2) :a (nth text-sec 3 0.7)})
        ;; Description lines (centered)
        desc-lines
        (into (map-indexed
                (fn [i line]
                  (let [line-w (* (count line) desc-size 0.56)]
                    {:text line :type :comment
                     :from 0 :to (count line)
                     :x (- cx (/ line-w 2)) :y (+ cy 56 (* i 22))
                     :size desc-size
                     :r (nth text-mut 0) :g (nth text-mut 1)
                     :b (nth text-mut 2) :a 0.7}))
                desc-lines))))))

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
    (let [{:keys [project expanded-dirs dir-cache home-dirs
                  hovered-id]} sidebar-state
          sidebar-scroll-y (or (:scroll-y sidebar-state) 0)
          sb-w sidebar-w
          sb-font font-size
          sb-char-advance (* sb-font 0.56)
          max-chars (max 8 (int (/ (- sb-w (* 2 sidebar-padding-x)) sb-char-advance)))
          colors (:colors dt)

          ;; Right border line (1px separator between sidebar and content)
          border-node (rt-node :sidebar-border :chrome
                        {:x (dec sb-w) :y 0 :w 1 :h viewport-h}
                        :style {:bg (:border colors)})

          ;; Content fills full height (no tab bar)
          content-top 0
          content-h viewport-h

          content-children
          (cond
            ;; No project selected — show home dirs
            (nil? project)
            (let [;; Header
                  explorer-label "EXPLORER"
                  ;; Overline typography for category labels
                  text-muted-c (or (:text-muted (:surfaces dt)) [0.36 0.42 0.50 1.0])
                  header-node (rt-node :explorer-hdr :header
                                {:x 0 :y 0 :w sb-w :h sidebar-back-h}
                                :style {:bg (:bg-elevated colors)
                                        :border-widths [0 0 1 0]
                                        :border-color (:border-subtle colors)}
                                :text [{:text explorer-label :type :comment
                                        :from 0 :to (count explorer-label)
                                        :x sidebar-padding-x :y 28
                                        :size 11
                                        :r (nth text-muted-c 0) :g (nth text-muted-c 1)
                                        :b (nth text-muted-c 2) :a (nth text-muted-c 3)}])
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
                                      :radius (:sm (:radii dt))})
                            :children
                            (if hovered?
                              [(rt-node (keyword (str "home-" i "-hl")) :highlight
                                 {:x sidebar-item-inset :y 2
                                  :w (- sb-w (* 2 sidebar-item-inset)) :h (- sidebar-row-h 4)}
                                 :style {:bg (:bg-hover colors)
                                         :radius (:sm (:radii dt))})]
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
                                      :size (:size typo-subtitle)
                                      :r 0.90 :g 0.90 :b 0.92 :a (:a typo-subtitle)}
                                     {:text subtitle :type :comment
                                      :from 0 :to (count subtitle)
                                      :x (+ sidebar-padding-x 16) :y 40
                                      :size (:size typo-body)
                                      :r 0.55 :g 0.55 :b 0.60 :a 0.7}])
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
                                :size (:size typo-body)
                                :r 0.55 :g 0.55 :b 0.60 :a 0.8}])))
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
                                                  :radius (:sm (:radii dt))}))
                            ;; Active accent bar (2px, inner-left edge)
                            accent-bar (when active?
                                         (rt-node (keyword (str "entry-" i "-acc")) :accent-bar
                                           {:x (+ sidebar-item-inset 1) :y 6
                                            :w 2 :h (- sidebar-row-h 12)}
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
        :style {:bg (or (:base (:surfaces dt)) (:bg colors))}
        :children [border-node content-node]))))

;; ============================================================================
;; FILE 3-PANE LAYOUT (Editor | Chat | Preview)
;; ============================================================================

(declare trail->display-lines)
(declare trail->chat-nodes)

(defn build-file-layout
  "Build the 3-pane layout for any open file.
   ┌──────────────┬──────────────┬──────────────┐
   │  CODE FILE   │ CHAT SESSION │   PREVIEW    │
   │  (live file) │ (Claude CLI) │ (placeholder)│
   └──────────────┴──────────────┴──────────────┘
   Returns a single rt-node tree spanning the full content area.
   shimmer-alpha: 0.0-1.0 pulse for pending tool cards.
   collapsed: #{keyword} set of collapsed block ids."
  [w h current-file agent-output font-size shimmer-alpha collapsed
   & {:keys [active-pane char-advance chat-scroll-y chat-input focus]
      :or {active-pane :editor char-advance nil chat-scroll-y 0
           chat-input {:text "" :cursor 0} focus :editor}}]
  (let [colors (:colors dt)
        surfaces (:surfaces dt)
        fg (:fg colors)
        fg-dim (:fg-muted colors)
        border (:border colors)
        ;; 3-column widths: 40% / 55% / 5%
        code-w (int (* w 0.4))
        chat-w (int (* w 0.55))
        render-w (- w code-w chat-w)
        ;; Header height — 36px (4px grid rhythm)
        header-h 36
        ;; Text helpers — use typography hierarchy
        fs (max font-size (:md (:font-sizes dt)))
        fs-hdr (:size typo-subtitle)
        hdr-alpha (:a typo-subtitle)
        line-h (+ fs 4)
        text-y (+ fs 6)
        pad (:lg (:spacing dt))
        char-advance (or char-advance (* fs 0.56))
        ;; Surface colors for depth hierarchy — focused pane gets elevated, others sunken
        elevated-bg (or (:elevated surfaces) (:bg-elevated colors))
        sunken-bg (or (:sunken surfaces) (:bg colors))
        hdr-bg elevated-bg
        ;; Chat pane uses a warm dark bg (matching terminal #090200 feel)
        chat-warm-bg [0.06 0.04 0.03 1.0]
        ;; Per-pane background — focus tracked but same bg (no highlight shift)
        code-bg (if (= active-pane :editor) sunken-bg sunken-bg)
        chat-bg (if (= active-pane :chat) chat-warm-bg chat-warm-bg)
        preview-bg (if (= active-pane :preview) sunken-bg sunken-bg)
        ;; Header text — brighter for focused pane
        text-primary (or (:text-primary surfaces) fg)
        text-secondary (or (:text-secondary surfaces) fg-dim)

        ;; === CODE PANE (left) — real editor renders beneath, we just add header + border ===
        code-header-label (or (:name current-file) "No file open")

        code-focused? (= active-pane :editor)
        code-hdr-fg (if code-focused? text-primary text-secondary)
        code-pane
        (rt-node :file-code-pane :panel
          {:x 0 :y 0 :w code-w :h h}
          :style {:bg code-bg}
          :children
          (cond-> [(rt-node :file-code-hdr :header
                     {:x 0 :y 0 :w code-w :h header-h}
                     :style {:bg hdr-bg
                             :border-widths [0 0 1 0]
                             :border-color (:border-subtle colors)}
                     :text [{:text code-header-label :type :keyword
                              :from 0 :to (count code-header-label)
                              :x pad :y 24 :size fs-hdr
                              :r (nth code-hdr-fg 0) :g (nth code-hdr-fg 1)
                              :b (nth code-hdr-fg 2) :a hdr-alpha}])]
            ;; Focus indicator: 2px bottom accent underline on focused pane header
            code-focused?
            (conj (rt-node :file-code-focus :accent-bar
                    {:x pad :y (- header-h 2) :w 40 :h 2}
                    :style {:bg (or (:accent surfaces) (:accent colors))
                            :radius 1}))))

        ;; === CHAT PANE (center) — typed block rendering ===
        a-status (:status agent-output)
        failed? (= :failed a-status)
        complete? (= :complete a-status)
        running? (or (= :running a-status) (= :submitting a-status))
        chat-status (cond
                      running? "Streaming..."
                      failed? "Failed"
                      complete? "Complete"
                      :else "Idle")
        chat-header-str (str "Chat - " chat-status)
        chat-input-h 36   ;; height of the chat input bar
        chat-body-h (- h header-h chat-input-h)
        trail (:trail agent-output)
        ;; Build chat body children: either typed blocks from trail, or placeholder
        chat-children
        (if (seq trail)
          ;; Status header + typed block nodes
          (let [provider-name (some-> (:provider agent-output) name str/upper-case)
                prompt-text (:prompt agent-output)
                status-label (str "[" (or provider-name "AI") "] " (when a-status (name a-status)) ": " prompt-text)
                status-c (case a-status
                           :complete {:r 0.55 :g 0.9 :b 0.55 :a 1.0}
                           :failed {:r 0.95 :g 0.45 :b 0.45 :a 1.0}
                           :running {:r 0.6 :g 0.8 :b 1.0 :a 1.0}
                           :submitting {:r 0.6 :g 0.8 :b 1.0 :a 1.0}
                           {:r 0.75 :g 0.75 :b 0.75 :a 1.0})
                status-node (rt-node :chat-status-hdr :reasoning-block
                              {:x 0 :y 0 :w chat-w :h (+ line-h 4)}
                              :text [{:text status-label :type :keyword
                                      :from 0 :to (count status-label)
                                      :x pad :y (+ fs 2) :size fs
                                      :r (:r status-c) :g (:g status-c)
                                      :b (:b status-c) :a (:a status-c)}])
                block-nodes (trail->chat-nodes trail chat-w fs char-advance
                                               (or shimmer-alpha 0.4) (or collapsed #{}))]
            (into [status-node] block-nodes))
          ;; No trail — centered empty state
          (if (seq (or (:output agent-output) ""))
            ;; Has flat output text — render it
            (let [result-text (:output agent-output)
                  c {:r 0.55 :g 0.55 :b 0.60 :a 0.7}
                  chat-max-chars (max 20 (int (/ (- chat-w (* 2 pad)) char-advance)))
                  all-lines (vec (mapcat #(wrap-line % chat-max-chars) (str/split-lines result-text)))
                  text-ops (vec (map-indexed
                                  (fn [i line]
                                    {:text line :type :comment
                                     :from 0 :to (count line)
                                     :x pad :y (+ fs (* i line-h))
                                     :size fs :r (:r c) :g (:g c) :b (:b c) :a (:a c)})
                                  all-lines))]
              [(rt-node :chat-placeholder :text-block
                 {:x 0 :y 0 :w chat-w :h (+ fs (* (count all-lines) line-h))}
                 :text text-ops)])
            ;; Empty — centered icon + headline + description
            [(build-empty-state :chat-empty chat-w chat-body-h
               {:icon "--"
                :headline "No session"
                :description "Type a prompt below to start a conversation."})]))

        ;; Chat scroll: use interactive scroll-y, clamped to content bounds
        chat-content-h (reduce + 0 (map #(get-in % [:bounds :h] 0) chat-children))
        max-chat-scroll (max 0 (- chat-content-h chat-body-h))
        chat-scroll-offset (min chat-scroll-y max-chat-scroll)

        ;; Chat header status color
        chat-hdr-accent (cond
                          running?  (:accent colors)
                          complete? (:success colors)
                          failed?   (:destructive colors)
                          :else     nil)

        chat-focused? (= active-pane :chat)
        chat-hdr-fg (if chat-focused? text-primary text-secondary)

        chat-pane
        (rt-node :file-chat-pane :panel
          {:x code-w :y 0 :w chat-w :h h}
          :style {:bg chat-bg}
          :children
          (cond-> [(rt-node :file-chat-hdr :header
                     {:x 0 :y 0 :w chat-w :h header-h}
                     :style {:bg hdr-bg
                             :border-widths [0 0 1 0]
                             :border-color (:border-subtle colors)}
                     :text [{:text chat-header-str :type :keyword
                              :from 0 :to (count chat-header-str)
                              :x pad :y 24 :size fs-hdr
                              :r (nth chat-hdr-fg 0) :g (nth chat-hdr-fg 1)
                              :b (nth chat-hdr-fg 2) :a hdr-alpha}])
                   (rt-node :file-chat-body :panel-content
                     {:x 0 :y header-h :w chat-w :h chat-body-h}
                     :clip? true
                     :children
                     [(rt-node :file-chat-scroll :scroll-container
                        {:x 0 :y (- 4 chat-scroll-offset) :w chat-w :h (+ chat-content-h 8)}
                        :layout {:direction :column :gap 4 :padding [0 0 0 0]}
                        :children chat-children)])
                   ;; === CHAT INPUT BAR (bottom of chat pane) ===
                   (let [input-y (- h chat-input-h)
                         ci-text (:text chat-input)
                         ci-cursor (:cursor chat-input)
                         chat-focused? (= focus :chat)
                         prompt-str "> "
                         prompt-len (count prompt-str)
                         display-text (str prompt-str ci-text)
                         placeholder? (and (empty? ci-text) (not chat-focused?))
                         input-fg (if chat-focused?
                                    {:r 0.85 :g 0.84 :b 0.83 :a 1.0}
                                    {:r 0.50 :g 0.49 :b 0.48 :a 0.7})
                         prompt-fg {:r 0.45 :g 0.70 :b 0.45 :a 0.9}
                         placeholder-fg {:r 0.45 :g 0.43 :b 0.41 :a 0.5}
                         ;; Input text ops
                         input-text-ops
                         (if placeholder?
                           [{:text "Type a message..." :type :comment
                             :from 0 :to 18
                             :x (+ pad (* prompt-len char-advance)) :y (+ fs 10) :size fs
                             :r (:r placeholder-fg) :g (:g placeholder-fg)
                             :b (:b placeholder-fg) :a (:a placeholder-fg)}
                            {:text prompt-str :type :keyword
                             :from 0 :to prompt-len
                             :x pad :y (+ fs 10) :size fs
                             :r (:r prompt-fg) :g (:g prompt-fg)
                             :b (:b prompt-fg) :a (:a prompt-fg)}]
                           [{:text prompt-str :type :keyword
                             :from 0 :to prompt-len
                             :x pad :y (+ fs 10) :size fs
                             :r (:r prompt-fg) :g (:g prompt-fg)
                             :b (:b prompt-fg) :a (:a prompt-fg)}
                            {:text ci-text :type :keyword
                             :from 0 :to (count ci-text)
                             :x (+ pad (* prompt-len char-advance)) :y (+ fs 10) :size fs
                             :r (:r input-fg) :g (:g input-fg)
                             :b (:b input-fg) :a (:a input-fg)}])
                         ;; Caret rect (only when focused)
                         caret-x (+ pad (* (+ prompt-len ci-cursor) char-advance))
                         input-children
                         (if chat-focused?
                           [(rt-node :chat-input-caret :rect
                              {:x caret-x :y 8 :w 2 :h (+ fs 4)}
                              :style {:bg [0.85 0.84 0.83 1.0]})]
                           [])]
                     (rt-node :file-chat-input :panel
                       {:x 0 :y input-y :w chat-w :h chat-input-h}
                       :style {:bg [0.08 0.06 0.05 1.0]
                               :border-widths [1 0 0 0]
                               :border-color (:border-subtle colors)}
                       :text input-text-ops
                       :children input-children))]
            ;; Status accent: bottom underline for focus, left bar for streaming status
            chat-hdr-accent
            (conj (rt-node :file-chat-status :accent-bar
                    {:x 0 :y 0 :w 2 :h header-h}
                    :style {:bg chat-hdr-accent}))
            chat-focused?
            (conj (rt-node :file-chat-focus :accent-bar
                    {:x pad :y (- header-h 2) :w 40 :h 2}
                    :style {:bg (or (:accent surfaces) (:accent colors))
                            :radius 1}))))

        ;; === PREVIEW PANE (right) ===
        render-label "Preview"
        render-msg "No preview"

        preview-focused? (= active-pane :preview)
        preview-hdr-fg (if preview-focused? text-primary text-secondary)

        render-pane
        (rt-node :file-render-pane :panel
          {:x (+ code-w chat-w) :y 0 :w render-w :h h}
          :style {:bg preview-bg}
          :children
          (cond-> [(rt-node :file-render-hdr :header
                     {:x 0 :y 0 :w render-w :h header-h}
                     :style {:bg hdr-bg
                             :border-widths [0 0 1 0]
                             :border-color (:border-subtle colors)}
                     :text [{:text render-label :type :keyword
                              :from 0 :to (count render-label)
                              :x pad :y 24 :size fs-hdr
                              :r (nth preview-hdr-fg 0) :g (nth preview-hdr-fg 1)
                              :b (nth preview-hdr-fg 2) :a hdr-alpha}])
                   (rt-node :file-render-body :panel-content
                     {:x 0 :y header-h :w render-w :h (- h header-h)}
                     :children [(build-empty-state :preview-empty render-w (- h header-h)
                                  {:icon "[]"
                                   :headline "No preview"
                                   :description "Preview will appear when a component is compiled."})])]
            preview-focused?
            (conj (rt-node :file-render-focus :accent-bar
                    {:x pad :y (- header-h 2) :w 40 :h 2}
                    :style {:bg (or (:accent surfaces) (:accent colors))
                            :radius 1}))))]

    ;; Root: spans full content area — transparent so child pane bgs define depth
    (rt-node :file-layout-root :panel
      {:x 0 :y 0 :w w :h h}
      :children [code-pane chat-pane render-pane])))

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
   layout-x layout-y line-h gutter-w char-advance viewport-w
   & {:keys [gutter-lx]}]
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
        ;; Gutter uses unscrolled x so fold indicators stay fixed
        gutter-x (- (or gutter-lx layout-x) gutter-w)

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
   !sidebar-state !sidebar-visible !current-file !extract-preview !agent-output
   !shimmer-phase !trail-collapsed !active-pane !scroll-x !chat-scroll-y !chat-input
   layout-x layout-y gutter-w]
  (m/latest
    (fn [doc fold-state bracket-match eval-result caret-visible focus settings active-font viewport
         flow-state scroll-y collapsed-groups hovered-row-idx drag-state
         sidebar-state sidebar-visible? current-file extract-preview agent-output
         shimmer-phase trail-collapsed active-pane scroll-x chat-scroll-y chat-input]
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
            content-w (- (:width viewport) sb-w)
            file-open? (some? current-file)
            content-result
            (if (:rt-node extract-preview)
              ;; Extract preview on right half
              (let [half-w (/ (- (:width viewport) sb-w) 2)
                    preview-tree (when-let [rt (:rt-node extract-preview)]
                                   (resolve-layout
                                     (rt-node :extract-preview-root :rect
                                              {:x half-w :y 0 :w half-w :h (:height viewport)}
                                              :style {:bg (:bg (:colors dt))}
                                              :layout {:direction :column :padding [16 16 16 16] :gap 8}
                                              :children [(rt-node :extract-label :text
                                                                  {:x 0 :y 0 :w (- half-w 32) :h 24}
                                                                  :text [{:text "Compiled Preview" :type :keyword
                                                                          :from 0 :to 16 :x 0 :y 16
                                                                          :size 14 :r 0.55 :g 0.55 :b 0.60 :a 1.0}])
                                                         (assoc-in rt [:bounds :w] (- half-w 32))])))]
                {:rects (if preview-tree (tree->rects preview-tree) [])
                 :shadows (if preview-tree (tree->shadows preview-tree) [])})
              (if (flow-canvas-active? flow-state)
                (compute-ticket-list-rects flow-state content-w (:height viewport)
                                           scroll-y hovered-row-idx collapsed-groups drag-state)
                ;; File open -> 3-pane layout; no file -> plain editor
                (if file-open?
                  (let [code-w (int (* content-w 0.4))
                        content-h (:height viewport)
                        line-h (maybe-snap (* font-size (:line-height settings)) dpr snap?)
                        lx (maybe-snap (- layout-x (or scroll-x 0)) dpr snap?)
                        ly (maybe-snap layout-y dpr snap?)
                        ulx (maybe-snap layout-x dpr snap?) ;; unscrolled for gutter
                        editor-rects (compute-editor-rects doc fold-state bracket-match eval-result
                                                           caret-visible focus lx ly line-h gutter-w
                                                           char-advance code-w
                                                           :gutter-lx ulx)
                        shimmer-alpha (if shimmer-phase 0.9 0.4)
                        right-tree (resolve-layout
                                     (build-file-layout content-w content-h current-file agent-output font-size
                                                        shimmer-alpha trail-collapsed
                                                        :active-pane active-pane :char-advance char-advance
                                                        :chat-scroll-y (or chat-scroll-y 0)
                                                        :chat-input chat-input :focus focus))
                        right-rects (mapv #(update % :y + scroll-y) (tree->rects right-tree))
                        right-shadows (mapv #(update % :y + scroll-y) (tree->shadows right-tree))]
                    {:rects (into (vec right-rects) editor-rects)
                     :shadows (vec right-shadows)})
                  (let [line-h (maybe-snap (* font-size (:line-height settings)) dpr snap?)
                        lx (maybe-snap (- layout-x (or scroll-x 0)) dpr snap?)
                        ulx (maybe-snap layout-x dpr snap?)
                        ly (maybe-snap layout-y dpr snap?)]
                    {:rects (compute-editor-rects doc fold-state bracket-match eval-result caret-visible focus
                                                  lx ly line-h gutter-w char-advance content-w
                                                  :gutter-lx ulx)
                     :shadows []}))))]
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
    (m/watch !current-file)
    (m/watch !extract-preview)
    (m/watch !agent-output)
    (m/watch !shimmer-phase)
    (m/watch !trail-collapsed)
    (m/watch !active-pane)
    (m/watch !scroll-x)
    (m/watch !chat-scroll-y)
    (m/watch !chat-input)))

;; --- Markdown rendering helpers for chat pane trail --------------------------

(def md-style-colors
  "Colors for inline markdown styles in reasoning blocks.
   Tuned for warm terminal-like feel on dark bg."
  {:normal {:r 0.72 :g 0.71 :b 0.71 :a 1.0}    ;; warm neutral, softer than terminal #a4a1a1
   :bold   {:r 0.88 :g 0.87 :b 0.87 :a 1.0}    ;; brighter for emphasis but not harsh white
   :code   {:r 0.00 :g 0.63 :b 0.89 :a 1.0}    ;; terminal blue (color4 #00a0e4)
   :link   {:r 0.00 :g 0.63 :b 0.89 :a 0.85}}) ;; same blue, slightly dimmer

(defn parse-md-inline-spans
  "Parse inline markdown: **bold**, *emphasis*, `code`, [link](url).
   Returns [{:text str :style :normal/:bold/:code/:link} ...]"
  [line]
  (let [len (count line)]
    (loop [i 0 spans [] cur ""]
      (if (>= i len)
        (let [final (if (seq cur) (conj spans {:text cur :style :normal}) spans)]
          (if (empty? final) [{:text "" :style :normal}] final))
        (let [ch (.charAt line i)]
          (cond
            ;; **bold**
            (and (= ch \*) (< (inc i) len) (= (.charAt line (inc i)) \*))
            (let [end (str/index-of line "**" (+ i 2))]
              (if (and end (> end (+ i 2)))
                (recur (+ end 2)
                       (-> (if (seq cur) (conj spans {:text cur :style :normal}) spans)
                           (conj {:text (subs line (+ i 2) end) :style :bold}))
                       "")
                (recur (+ i 2) spans (str cur "**"))))
            ;; *emphasis* (single asterisk, not followed by another *)
            (and (= ch \*)
                 (or (>= (inc i) len) (not= (.charAt line (inc i)) \*)))
            (let [end (str/index-of line "*" (inc i))]
              (if (and end (> end (inc i))
                       ;; Ensure closing * is not part of **
                       (or (>= (inc end) len) (not= (.charAt line (inc end)) \*)))
                (recur (inc end)
                       (-> (if (seq cur) (conj spans {:text cur :style :normal}) spans)
                           (conj {:text (subs line (inc i) end) :style :bold}))
                       "")
                (recur (inc i) spans (str cur "*"))))
            ;; `code`
            (= ch \`)
            (let [end (str/index-of line "`" (inc i))]
              (if (and end (> end (inc i)))
                (recur (inc end)
                       (-> (if (seq cur) (conj spans {:text cur :style :normal}) spans)
                           (conj {:text (subs line (inc i) end) :style :code}))
                       "")
                (recur (inc i) spans (str cur "`"))))
            ;; [link](url)
            (= ch \[)
            (let [close-bracket (str/index-of line "](" i)]
              (if close-bracket
                (let [close-paren (str/index-of line ")" (+ close-bracket 2))]
                  (if close-paren
                    (recur (inc close-paren)
                           (-> (if (seq cur) (conj spans {:text cur :style :normal}) spans)
                               (conj {:text (subs line (inc i) close-bracket) :style :link}))
                           "")
                    (recur (inc i) spans (str cur "["))))
                (recur (inc i) spans (str cur "["))))
            ;; Normal character
            :else
            (recur (inc i) spans (str cur ch))))))))

(defn wrap-md-spans
  "Word-wrap styled spans to fit max-chars per line.
   Returns [[{:text str :style kw} ...] ...] — one vector of spans per visual line."
  [spans max-chars]
  (let [total-len (reduce + 0 (map (comp count :text) spans))]
    (if (<= total-len max-chars)
      [spans]
      ;; Build flat [char style] vector, then greedy-wrap
      (let [flat (vec (mapcat (fn [{:keys [text style]}]
                                (map #(vector % style) text))
                              spans))
            n (count flat)
            reconstitute (fn [chars]
                           (if (empty? chars)
                             [{:text "" :style :normal}]
                             (->> chars
                                  (partition-by second)
                                  (mapv (fn [g] {:text (apply str (map first g))
                                                :style (second (first g))})))))]
        (loop [pos 0 lines []]
          (if (>= pos n)
            lines
            (let [remaining (- n pos)
                  line-end (+ pos (min remaining max-chars))]
              (if (<= remaining max-chars)
                ;; Last line
                (conj lines (reconstitute (subvec flat pos n)))
                ;; Find last space in [pos, line-end) to break at word boundary
                (let [break-at (loop [j (dec line-end)]
                                 (cond
                                   (<= j pos) -1
                                   (= (first (nth flat j)) \space) j
                                   :else (recur (dec j))))]
                  (if (>= break-at 0)
                    (recur (inc break-at)
                           (conj lines (reconstitute (subvec flat pos break-at))))
                    ;; No space found — hard break at max-chars
                    (recur line-end
                           (conj lines (reconstitute (subvec flat pos line-end))))))))))))))

(defn spans->text-ops
  "Convert a single visual line of styled spans into positioned text-ops.
   style-colors maps :normal/:bold/:code/:link to {:r :g :b :a}."
  [spans x y font-size char-advance style-colors]
  (loop [ss spans cx x ops []]
    (if (empty? ss)
      ops
      (let [{:keys [text style]} (first ss)
            c (get style-colors style (get style-colors :normal))
            op {:text text :type :comment
                :from 0 :to (count text)
                :x cx :y y
                :size font-size
                :r (:r c) :g (:g c) :b (:b c) :a (:a c)}]
        (recur (rest ss) (+ cx (* (count text) char-advance)) (conj ops op))))))

(defn- decorative-line?
  "True when line is a backtick-wrapped decorative border (contains ─ or ★)."
  [trimmed]
  (and (str/starts-with? trimmed "`")
       (str/ends-with? trimmed "`")
       (> (count trimmed) 2)
       (re-find #"[\u2500\u2605]" trimmed)))

(defn- decorative-inner
  "Extract meaningful ASCII text from a decorative border line."
  [trimmed]
  (-> (subs trimmed 1 (dec (count trimmed)))
      (str/replace #"[\u2500\u2605\u2014\u2022]" "")
      str/trim))

(defn parse-md-blocks
  "Parse markdown text into block-level elements.
   States: :normal, :in-code, :in-callout.
   Returns [{:type :header/:paragraph/:code-block/:list/:callout ...}]"
  [text]
  (let [src-lines (str/split-lines text)]
    (loop [ls src-lines state :normal blocks [] cur-para [] code-lang nil callout-label nil]
      (if (empty? ls)
        ;; Flush remaining
        (cond
          (= state :in-code)
          (conj blocks {:type :code-block :lang code-lang :lines cur-para})
          (= state :in-callout)
          (let [body-text (str/join "\n" cur-para)
                body-blocks (when (seq body-text) (parse-md-blocks body-text))]
            (conj blocks {:type :callout :label callout-label :body (or body-blocks [])}))
          (seq cur-para)
          (conj blocks {:type :paragraph :content (str/join " " cur-para)})
          :else blocks)
        (let [line (first ls)
              trimmed (str/trim line)]
          (case state
            :in-code
            (if (str/starts-with? trimmed "```")
              (recur (rest ls) :normal
                     (conj blocks {:type :code-block :lang code-lang :lines cur-para})
                     [] nil nil)
              (recur (rest ls) :in-code blocks (conj cur-para line) code-lang nil))

            :in-callout
            (if (and (decorative-line? trimmed) (empty? (decorative-inner trimmed)))
              ;; Closing border — emit callout block with recursively-parsed body
              (let [body-text (str/join "\n" cur-para)
                    body-blocks (when (seq body-text) (parse-md-blocks body-text))]
                (recur (rest ls) :normal
                       (conj blocks {:type :callout :label callout-label :body (or body-blocks [])})
                       [] nil nil))
              ;; Content inside callout — collect lines
              (recur (rest ls) :in-callout blocks (conj cur-para line) nil callout-label))

            ;; :normal state
            (cond
              ;; Code fence opening
              (str/starts-with? trimmed "```")
              (let [blocks (if (seq cur-para)
                             (conj blocks {:type :paragraph :content (str/join " " cur-para)})
                             blocks)
                    lang (let [r (str/trim (subs trimmed 3))] (when (seq r) r))]
                (recur (rest ls) :in-code blocks [] lang nil))
              ;; Decorative border line: backtick-wrapped ★/─ chars (Insight blocks)
              (decorative-line? trimmed)
              (let [blocks (if (seq cur-para)
                             (conj blocks {:type :paragraph :content (str/join " " cur-para)})
                             blocks)
                    inner (decorative-inner trimmed)]
                (if (seq inner)
                  ;; Has meaningful text (e.g., "Insight") — enter callout mode
                  (recur (rest ls) :in-callout blocks [] nil inner)
                  ;; Just decorative — horizontal rule
                  (recur (rest ls) :normal (conj blocks {:type :hr}) [] nil nil)))
              ;; Table lines (pipe-delimited)
              (str/starts-with? trimmed "|")
              (let [blocks (if (seq cur-para)
                             (conj blocks {:type :paragraph :content (str/join " " cur-para)})
                             blocks)
                    [remaining table-lines]
                    (loop [rem ls tl []]
                      (let [l (first rem)
                            t (when l (str/trim l))]
                        (if (and t (str/starts-with? t "|"))
                          (recur (rest rem) (conj tl t))
                          [rem tl])))]
                (recur remaining :normal
                       (conj blocks {:type :table :lines table-lines}) [] nil nil))
              ;; Header
              (re-find #"^#{1,6}\s+" trimmed)
              (let [blocks (if (seq cur-para)
                             (conj blocks {:type :paragraph :content (str/join " " cur-para)})
                             blocks)
                    level (count (re-find #"^#+" trimmed))
                    content (str/trim (subs trimmed (inc level)))]
                (recur (rest ls) :normal
                       (conj blocks {:type :header :level level :content content})
                       [] nil nil))
              ;; Horizontal rule: --- or *** or ___ or repeated ─
              (or (re-find #"^[-*_]{3,}\s*$" trimmed)
                  (re-find #"^\u2500{3,}" trimmed))
              (let [blocks (if (seq cur-para)
                             (conj blocks {:type :paragraph :content (str/join " " cur-para)})
                             blocks)]
                (recur (rest ls) :normal (conj blocks {:type :hr}) [] nil nil))
              ;; Bullet list item
              (re-find #"^[-*]\s+" trimmed)
              (let [blocks (if (seq cur-para)
                             (conj blocks {:type :paragraph :content (str/join " " cur-para)})
                             blocks)
                    [remaining items]
                    (loop [rem ls items []]
                      (let [l (first rem)
                            t (when l (str/trim l))]
                        (if (and t (re-find #"^[-*]\s+" t))
                          (recur (rest rem) (conj items {:content (str/trim (subs t 2))}))
                          [rem items])))]
                (recur remaining :normal (conj blocks {:type :list :items items}) [] nil nil))
              ;; Numbered list item (1. 2. 3. etc.)
              (re-find #"^\d+\.\s+" trimmed)
              (let [blocks (if (seq cur-para)
                             (conj blocks {:type :paragraph :content (str/join " " cur-para)})
                             blocks)
                    [remaining items]
                    (loop [rem ls items []]
                      (let [l (first rem)
                            t (when l (str/trim l))]
                        (if (and t (re-find #"^\d+\.\s+" t))
                          (let [after-num (str/replace-first t #"^\d+\.\s+" "")]
                            (recur (rest rem) (conj items {:content after-num})))
                          [rem items])))]
                (recur remaining :normal (conj blocks {:type :numbered-list :items items}) [] nil nil))
              ;; Blank line — paragraph break
              (empty? trimmed)
              (let [blocks (if (seq cur-para)
                             (conj blocks {:type :paragraph :content (str/join " " cur-para)})
                             blocks)]
                (recur (rest ls) :normal blocks [] nil nil))
              ;; Regular text — accumulate into paragraph
              :else
              (recur (rest ls) :normal blocks (conj cur-para trimmed) nil nil))))))))

(defn trail-node-color
  "Color for a trail node by kind. Returns {:r :g :b :a}."
  [kind tool-name]
  (case kind
    :reasoning    {:r 0.72 :g 0.71 :b 0.71 :a 1.0}
    :thinking     {:r 0.65 :g 0.65 :b 0.75 :a 0.6}  ;; dimmed — internal reasoning
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

(defn- tool-input-summary
  "One-line summary of tool input for card header."
  [tool-name input]
  (cond
    (and (string? (:file_path input)) (seq (:file_path input)))
    (:file_path input)
    (and (string? (:pattern input)) (seq (:pattern input)))
    (str "\"" (:pattern input) "\"")
    (and (string? (:command input)) (seq (:command input)))
    (let [cmd (:command input)]
      (if (> (count cmd) 60) (str (subs cmd 0 57) "...") cmd))
    :else ""))

(defn- file-op-tool?
  "True for tools that are read-only file operations (groupable)."
  [tool-name]
  (contains? #{"Read" "read" "Grep" "grep" "Glob" "glob"} tool-name))

(defn- group-trail-blocks
  "Group consecutive trail nodes into logical blocks.
   Merges consecutive :reasoning and :thinking nodes.
   Groups :tool-call-start + :tool-call + :tool-result by tool-id.
   Returns [{:block-type :nodes [...]}]."
  [trail]
  (reduce
    (fn [acc node]
      (let [kind (:kind node)
            last-block (peek acc)]
        (case kind
          :reasoning
          (if (and last-block (= :reasoning (:block-type last-block)))
            (conj (pop acc) (update last-block :nodes conj node))
            (conj acc {:block-type :reasoning :nodes [node]}))

          :thinking
          (if (and last-block (= :thinking (:block-type last-block)))
            (conj (pop acc) (update last-block :nodes conj node))
            (conj acc {:block-type :thinking :nodes [node]}))

          :tool-call-start
          (conj acc {:block-type :tool-card
                     :tool-id (:tool-id node)
                     :tool-name (:tool-name node)
                     :status :pending
                     :nodes [node]})

          :tool-call
          ;; Find matching tool-card block by tool-id, update it
          (let [tid (:tool-id node)
                idx (some (fn [i]
                            (when (and (= :tool-card (:block-type (nth acc i)))
                                       (= tid (:tool-id (nth acc i))))
                              i))
                          (range (dec (count acc)) -1 -1))]
            (if idx
              (update (vec acc) idx
                      (fn [b] (-> b (assoc :status :complete :input (:input node))
                                    (update :nodes conj node))))
              ;; Orphan tool-call — make standalone card
              (conj acc {:block-type :tool-card
                         :tool-id tid
                         :tool-name (:tool-name node)
                         :status :complete
                         :input (:input node)
                         :nodes [node]})))

          :tool-result
          (let [tid (:tool-id node)
                idx (some (fn [i]
                            (when (and (= :tool-card (:block-type (nth acc i)))
                                       (= tid (:tool-id (nth acc i))))
                              i))
                          (range (dec (count acc)) -1 -1))]
            (if idx
              (update (vec acc) idx
                      (fn [b] (-> b (assoc :result-content (:content node))
                                    (update :nodes conj node))))
              ;; Orphan result
              (conj acc {:block-type :tool-card
                         :tool-id tid
                         :tool-name "?"
                         :status :complete
                         :result-content (:content node)
                         :nodes [node]})))

          ;; Unknown kind — treat as reasoning
          (conj acc {:block-type :reasoning :nodes [node]}))))
    []
    trail))

(defn- group-consecutive-file-ops
  "Collapse runs of 3+ consecutive file-op tool cards into group cards."
  [blocks]
  (loop [bs blocks result []]
    (if (empty? bs)
      result
      (let [b (first bs)]
        (if (and (= :tool-card (:block-type b))
                 (file-op-tool? (:tool-name b)))
          ;; Count consecutive file-op cards
          (let [run (take-while #(and (= :tool-card (:block-type %))
                                      (file-op-tool? (:tool-name %)))
                                bs)
                n (count run)]
            (if (>= n 3)
              (recur (drop n bs)
                     (conj result {:block-type :tool-group
                                   :cards (vec run)
                                   :count n}))
              (recur (rest bs) (conj result b))))
          (recur (rest bs) (conj result b)))))))

(defn trail->chat-nodes
  "Convert trail to rt-node children for the chat pane.
   Each logical block becomes an rt-node with typed visual treatment.
   Returns [rt-node ...] — children for the chat body container."
  [trail pane-w font-size char-advance shimmer-alpha collapsed]
  (let [colors (:colors dt)
        fg (:fg colors)
        fg-dim (:fg-muted colors)
        border (:border colors)
        pad 12
        max-chars (max 20 (int (/ (- pane-w (* 2 pad)) char-advance)))
        line-h (+ font-size 4)
        card-h-header 28
        blocks (-> trail group-trail-blocks group-consecutive-file-ops)]
    (vec
      (map-indexed
        (fn [bi block]
          (case (:block-type block)
            ;; --- Reasoning: markdown-formatted text ---
            :reasoning
            (let [merged-text (apply str (map :text (:nodes block)))
                  md-blocks (parse-md-blocks merged-text)
                  inner-w (- pane-w (* 2 pad))
                  inner-max-chars (max 20 (int (/ inner-w char-advance)))
                  code-bg (get-in dt [:colors :bg-muted])
                  code-pad 8
                  code-max-chars (max 20 (int (/ (- inner-w (* 2 code-pad)) char-advance)))
                  block-gap 8
                  ;; Build child nodes for each markdown block
                  children
                  (vec
                    (map-indexed
                      (fn [mi mb]
                        (case (:type mb)
                          :header
                          (let [level (or (:level mb) 2)
                                hdr-size (if (<= level 2) (:size typo-title) (:size typo-subtitle))
                                hdr-color {:r 0.90 :g 0.89 :b 0.89 :a 1.0}
                                hdr-max (max 20 (int (/ inner-w (* hdr-size 0.56))))
                                text (:content mb)
                                wrapped (wrap-line text hdr-max)
                                hdr-line-h (+ hdr-size 5)
                                text-h (* (count wrapped) hdr-line-h)
                                ;; Top margin + text + bottom accent + gap
                                top-margin (if (<= level 2) 10 6)
                                bottom-pad 6
                                h (+ top-margin text-h bottom-pad)
                                text-ops (vec (map-indexed
                                               (fn [i ln]
                                                 {:text ln :type :keyword
                                                  :from 0 :to (count ln)
                                                  :x pad :y (+ top-margin hdr-size (* i hdr-line-h))
                                                  :size hdr-size
                                                  :r (:r hdr-color) :g (:g hdr-color)
                                                  :b (:b hdr-color) :a (:a hdr-color)})
                                               wrapped))
                                ;; Subtle bottom border for h1/h2
                                accent-line (when (<= level 2)
                                              (rt-node (keyword (str "md-hdr-line-" bi "-" mi)) :hdr-accent
                                                {:x pad :y (- h 2) :w (min (* (count (first wrapped)) (* hdr-size 0.56)) inner-w) :h 1}
                                                :style {:bg (:border-subtle colors)}))]
                            (rt-node (keyword (str "md-hdr-" bi "-" mi)) :md-header
                              {:x 0 :y 0 :w pane-w :h h}
                              :text text-ops
                              :children (if accent-line [accent-line] [])))

                          :paragraph
                          (let [spans (parse-md-inline-spans (:content mb))
                                wrapped-lines (wrap-md-spans spans inner-max-chars)
                                all-ops (vec (apply concat
                                              (map-indexed
                                                (fn [li line-spans]
                                                  (spans->text-ops line-spans pad
                                                                   (+ font-size (* li line-h))
                                                                   font-size char-advance md-style-colors))
                                                wrapped-lines)))
                                h (+ 4 (* (count wrapped-lines) line-h))]
                            (rt-node (keyword (str "md-para-" bi "-" mi)) :md-paragraph
                              {:x 0 :y 0 :w pane-w :h h}
                              :text all-ops))

                          :code-block
                          (let [code-color {:r 0.00 :g 0.63 :b 0.32 :a 1.0}
                                code-lines (:lines mb)
                                wrapped-lines (vec (mapcat #(wrap-line % code-max-chars) code-lines))
                                text-ops (vec (map-indexed
                                               (fn [i ln]
                                                 {:text ln :type :comment
                                                  :from 0 :to (count ln)
                                                  :x (+ pad code-pad) :y (+ code-pad font-size (* i line-h))
                                                  :size font-size
                                                  :r (:r code-color) :g (:g code-color)
                                                  :b (:b code-color) :a (:a code-color)})
                                               wrapped-lines))
                                body-h (+ (* 2 code-pad) (* (count wrapped-lines) line-h))
                                total-h (+ body-h 4)]
                            (rt-node (keyword (str "md-code-" bi "-" mi)) :md-code-block
                              {:x 0 :y 0 :w pane-w :h total-h}
                              :children
                              [(rt-node (keyword (str "md-code-bg-" bi "-" mi)) :code-bg
                                 {:x pad :y 0 :w inner-w :h body-h}
                                 :style {:bg code-bg :radius 4})
                               (rt-node (keyword (str "md-code-text-" bi "-" mi)) :code-text
                                 {:x 0 :y 0 :w pane-w :h body-h}
                                 :text text-ops)]))

                          :list
                          (let [items (:items mb)
                                bullet-indent 2
                                item-max-chars (max 10 (- inner-max-chars bullet-indent))
                                item-data (mapv (fn [item]
                                                  (let [spans (parse-md-inline-spans (:content item))
                                                        wrapped (wrap-md-spans spans item-max-chars)]
                                                    {:wrapped wrapped}))
                                                items)
                                all-ops (loop [items-rem item-data li 0 ops []]
                                          (if (empty? items-rem)
                                            ops
                                            (let [{:keys [wrapped]} (first items-rem)
                                                  item-ops
                                                  (vec (apply concat
                                                    (map-indexed
                                                      (fn [wi line-spans]
                                                        (let [bullet-ops (when (= wi 0)
                                                                          [{:text "- " :type :comment
                                                                            :from 0 :to 2
                                                                            :x pad :y (+ font-size (* (+ li wi) line-h))
                                                                            :size font-size
                                                                            :r 0.55 :g 0.55 :b 0.6 :a 0.8}])
                                                              span-ops (spans->text-ops
                                                                         line-spans
                                                                         (+ pad (* bullet-indent char-advance))
                                                                         (+ font-size (* (+ li wi) line-h))
                                                                         font-size char-advance md-style-colors)]
                                                          (into (vec (or bullet-ops [])) span-ops)))
                                                      wrapped)))]
                                              (recur (rest items-rem) (+ li (count wrapped)) (into ops item-ops)))))
                                total-lines (reduce + 0 (map (comp count :wrapped) item-data))
                                h (+ 4 (* total-lines line-h))]
                            (rt-node (keyword (str "md-list-" bi "-" mi)) :md-list
                              {:x 0 :y 0 :w pane-w :h h}
                              :text all-ops))

                          :hr
                          (let [rule-h 8
                                border-c (:border-subtle colors)]
                            (rt-node (keyword (str "md-hr-" bi "-" mi)) :md-hr
                              {:x 0 :y 0 :w pane-w :h rule-h}
                              :children
                              [(rt-node (keyword (str "md-hr-line-" bi "-" mi)) :hr-line
                                 {:x pad :y 3 :w inner-w :h 1}
                                 :style {:bg border-c})]))

                          :numbered-list
                          (let [items (:items mb)
                                item-data (mapv (fn [idx item]
                                                  (let [prefix (str (inc idx) ". ")
                                                        prefix-w (count prefix)
                                                        item-max (max 10 (- inner-max-chars prefix-w))
                                                        spans (parse-md-inline-spans (:content item))
                                                        wrapped (wrap-md-spans spans item-max)]
                                                    {:wrapped wrapped :prefix prefix :prefix-w prefix-w}))
                                                (range) items)
                                all-ops (loop [items-rem item-data li 0 ops []]
                                          (if (empty? items-rem)
                                            ops
                                            (let [{:keys [wrapped prefix prefix-w]} (first items-rem)
                                                  item-ops
                                                  (vec (apply concat
                                                    (map-indexed
                                                      (fn [wi line-spans]
                                                        (let [num-ops (when (= wi 0)
                                                                        [{:text prefix :type :comment
                                                                          :from 0 :to (count prefix)
                                                                          :x pad :y (+ font-size (* (+ li wi) line-h))
                                                                          :size font-size
                                                                          :r 0.55 :g 0.55 :b 0.6 :a 0.8}])
                                                              span-ops (spans->text-ops
                                                                         line-spans
                                                                         (+ pad (* prefix-w char-advance))
                                                                         (+ font-size (* (+ li wi) line-h))
                                                                         font-size char-advance md-style-colors)]
                                                          (into (vec (or num-ops [])) span-ops)))
                                                      wrapped)))]
                                              (recur (rest items-rem) (+ li (count wrapped)) (into ops item-ops)))))
                                total-lines (reduce + 0 (map (comp count :wrapped) item-data))
                                h (+ 4 (* total-lines line-h))]
                            (rt-node (keyword (str "md-nlist-" bi "-" mi)) :md-numbered-list
                              {:x 0 :y 0 :w pane-w :h h}
                              :text all-ops))

                          :callout
                          (let [label (:label mb)
                                body-blocks (:body mb)
                                accent-c (:accent colors)
                                label-c {:r 0.70 :g 0.80 :b 1.0 :a 1.0}
                                callout-pad (+ pad 10)
                                callout-w (- pane-w callout-pad pad)
                                callout-max (max 20 (int (/ callout-w char-advance)))
                                ;; Header node
                                hdr-h (+ font-size 6)
                                hdr-node (rt-node (keyword (str "md-co-hdr-" bi "-" mi)) :callout-hdr
                                           {:x 0 :y 0 :w pane-w :h hdr-h}
                                           :text [{:text label :type :keyword
                                                   :from 0 :to (count label)
                                                   :x callout-pad :y (+ font-size 2)
                                                   :size font-size
                                                   :r (:r label-c) :g (:g label-c)
                                                   :b (:b label-c) :a (:a label-c)}])
                                ;; Body content nodes — reuse the same rendering logic
                                body-children
                                (vec (map-indexed
                                  (fn [ci cb]
                                    (case (:type cb)
                                      :paragraph
                                      (let [spans (parse-md-inline-spans (:content cb))
                                            wrapped-lines (wrap-md-spans spans callout-max)
                                            ops (vec (apply concat
                                                      (map-indexed
                                                        (fn [li ls]
                                                          (spans->text-ops ls callout-pad
                                                                           (+ font-size (* li line-h))
                                                                           font-size char-advance md-style-colors))
                                                        wrapped-lines)))
                                            h (+ 4 (* (count wrapped-lines) line-h))]
                                        (rt-node (keyword (str "md-co-p-" bi "-" mi "-" ci)) :callout-para
                                          {:x 0 :y 0 :w pane-w :h h}
                                          :text ops))
                                      :list
                                      (let [items (:items cb)
                                            bullet-indent 2
                                            item-mc (max 10 (- callout-max bullet-indent))
                                            item-d (mapv (fn [item]
                                                           {:wrapped (wrap-md-spans (parse-md-inline-spans (:content item)) item-mc)})
                                                         items)
                                            ops (loop [ir item-d li 0 o []]
                                                  (if (empty? ir) o
                                                    (let [{:keys [wrapped]} (first ir)
                                                          io (vec (apply concat
                                                               (map-indexed
                                                                 (fn [wi ls]
                                                                   (let [bp (when (= wi 0)
                                                                              [{:text "- " :type :comment :from 0 :to 2
                                                                                :x callout-pad :y (+ font-size (* (+ li wi) line-h))
                                                                                :size font-size :r 0.55 :g 0.55 :b 0.6 :a 0.8}])
                                                                         sp (spans->text-ops ls (+ callout-pad (* bullet-indent char-advance))
                                                                              (+ font-size (* (+ li wi) line-h))
                                                                              font-size char-advance md-style-colors)]
                                                                     (into (vec (or bp [])) sp)))
                                                                 wrapped)))]
                                                      (recur (rest ir) (+ li (count wrapped)) (into o io)))))
                                            tl (reduce + 0 (map (comp count :wrapped) item-d))
                                            h (+ 4 (* tl line-h))]
                                        (rt-node (keyword (str "md-co-l-" bi "-" mi "-" ci)) :callout-list
                                          {:x 0 :y 0 :w pane-w :h h}
                                          :text ops))
                                      :numbered-list
                                      (let [items (:items cb)
                                            item-d (mapv (fn [idx item]
                                                           (let [pfx (str (inc idx) ". ")
                                                                 pw (count pfx)]
                                                             {:wrapped (wrap-md-spans (parse-md-inline-spans (:content item))
                                                                         (max 10 (- callout-max pw)))
                                                              :prefix pfx :prefix-w pw}))
                                                         (range) items)
                                            ops (loop [ir item-d li 0 o []]
                                                  (if (empty? ir) o
                                                    (let [{:keys [wrapped prefix prefix-w]} (first ir)
                                                          io (vec (apply concat
                                                               (map-indexed
                                                                 (fn [wi ls]
                                                                   (let [np (when (= wi 0)
                                                                              [{:text prefix :type :comment :from 0 :to (count prefix)
                                                                                :x callout-pad :y (+ font-size (* (+ li wi) line-h))
                                                                                :size font-size :r 0.55 :g 0.55 :b 0.6 :a 0.8}])
                                                                         sp (spans->text-ops ls (+ callout-pad (* prefix-w char-advance))
                                                                              (+ font-size (* (+ li wi) line-h))
                                                                              font-size char-advance md-style-colors)]
                                                                     (into (vec (or np [])) sp)))
                                                                 wrapped)))]
                                                      (recur (rest ir) (+ li (count wrapped)) (into o io)))))
                                            tl (reduce + 0 (map (comp count :wrapped) item-d))
                                            h (+ 4 (* tl line-h))]
                                        (rt-node (keyword (str "md-co-n-" bi "-" mi "-" ci)) :callout-nlist
                                          {:x 0 :y 0 :w pane-w :h h}
                                          :text ops))
                                      ;; Other block types inside callout — render as paragraph
                                      (let [content (or (:content cb) "")
                                            spans (parse-md-inline-spans content)
                                            wrapped-lines (wrap-md-spans spans callout-max)
                                            ops (vec (apply concat
                                                      (map-indexed
                                                        (fn [li ls]
                                                          (spans->text-ops ls callout-pad
                                                                           (+ font-size (* li line-h))
                                                                           font-size char-advance md-style-colors))
                                                        wrapped-lines)))
                                            h (+ 4 (* (count wrapped-lines) line-h))]
                                        (rt-node (keyword (str "md-co-x-" bi "-" mi "-" ci)) :callout-misc
                                          {:x 0 :y 0 :w pane-w :h h}
                                          :text ops))))
                                  body-blocks))
                                all-children (into [hdr-node] body-children)
                                body-gap 4
                                content-h (+ (reduce + 0 (map #(get-in % [:bounds :h] 0) all-children))
                                             (* body-gap (max 0 (dec (count all-children)))))
                                total-h (+ content-h 4)]
                            (rt-node (keyword (str "md-callout-" bi "-" mi)) :md-callout
                              {:x 0 :y 0 :w pane-w :h total-h}
                              :layout {:direction :column :gap body-gap :padding [0 0 0 0]}
                              :children
                              (into [(rt-node (keyword (str "md-co-bar-" bi "-" mi)) :accent-bar
                                       {:x pad :y 2 :w 3 :h (- total-h 4)}
                                       :data {:layout-skip? true}
                                       :style {:bg accent-c :radius 2})]
                                    all-children)))

                          :table
                          (let [table-lines (:lines mb)
                                ;; Filter separator rows (|---|---|)
                                is-separator? #(boolean (re-find #"^\|[\s\-:|\+]+\|$" %))
                                content-rows (filterv (complement is-separator?) table-lines)
                                ;; Parse each row: split by |, trim cells
                                parse-row (fn [row-str]
                                            (->> (str/split row-str #"\|")
                                                 (map str/trim)
                                                 (filterv #(seq %))))
                                rows (mapv parse-row content-rows)
                                header-row (first rows)
                                data-rows (rest rows)
                                ;; Render each data row as "Name — value — value" with inline md
                                table-pad (+ pad code-pad)
                                table-max (max 20 (int (/ (- inner-w (* 2 code-pad)) char-advance)))
                                ;; Build text-ops: header row bold, data rows with inline parsing
                                all-ops
                                (loop [rs (cons {:cells header-row :is-header true}
                                                (map #(hash-map :cells % :is-header false) data-rows))
                                       li 0 ops []]
                                  (if (empty? rs)
                                    ops
                                    (let [{:keys [cells is-header]} (first rs)
                                          row-text (str/join "  |  " cells)
                                          spans (if is-header
                                                  [{:text row-text :style :bold}]
                                                  (parse-md-inline-spans row-text))
                                          wrapped (wrap-md-spans spans table-max)
                                          row-ops (vec (apply concat
                                                    (map-indexed
                                                      (fn [wi line-spans]
                                                        (spans->text-ops line-spans table-pad
                                                                         (+ font-size (* (+ li wi) line-h))
                                                                         font-size char-advance md-style-colors))
                                                      wrapped)))]
                                      (recur (rest rs) (+ li (count wrapped)) (into ops row-ops)))))
                                total-lines (+ (if header-row
                                                 (count (wrap-md-spans [{:text (str/join "  |  " header-row) :style :bold}] table-max))
                                                 0)
                                               (reduce + 0
                                                 (map (fn [cells]
                                                        (count (wrap-md-spans
                                                                 (parse-md-inline-spans (str/join "  |  " cells))
                                                                 table-max)))
                                                      data-rows)))
                                body-h (+ (* 2 code-pad) (* total-lines line-h))
                                total-h (+ body-h 4)]
                            (rt-node (keyword (str "md-table-" bi "-" mi)) :md-table
                              {:x 0 :y 0 :w pane-w :h total-h}
                              :children
                              [(rt-node (keyword (str "md-table-bg-" bi "-" mi)) :table-bg
                                 {:x pad :y 0 :w inner-w :h body-h}
                                 :style {:bg (get-in dt [:colors :bg-subtle]) :radius 4})
                               (rt-node (keyword (str "md-table-text-" bi "-" mi)) :table-text
                                 {:x 0 :y 0 :w pane-w :h body-h}
                                 :text all-ops)]))

                          ;; Fallback for unknown block types
                          (rt-node (keyword (str "md-unk-" bi "-" mi)) :md-unknown
                            {:x 0 :y 0 :w pane-w :h 0})))
                      md-blocks))
                  ;; Compute total height including gaps between blocks
                  total-h (+ (reduce + 0 (map #(get-in % [:bounds :h] 0) children))
                             (* block-gap (max 0 (dec (count children)))))]
              (rt-node (keyword (str "reasoning-" bi)) :reasoning-block
                {:x 0 :y 0 :w pane-w :h total-h}
                :layout {:direction :column :gap block-gap}
                :children children))

            ;; --- Thinking: dimmed block with left accent bar ---
            :thinking
            (let [merged-text (apply str (map :text (:nodes block)))
                  tid (str "thinking-" bi)
                  collapsed? (contains? collapsed (keyword tid))
                  header-text "Thinking..."
                  c (trail-node-color :thinking nil)
                  accent-color [0.45 0.55 0.75 0.5]
                  header-op {:text header-text :type :comment
                             :from 0 :to (count header-text)
                             :x (+ pad 8) :y (+ font-size 0)
                             :size font-size
                             :r (:r c) :g (:g c) :b (:b c) :a (:a c)}
                  arrow-text (if collapsed? ">" "v")
                  arrow-op {:text arrow-text :type :comment
                            :from 0 :to 1
                            :x (- pane-w pad 10) :y (+ font-size 0)
                            :size font-size
                            :r (:r c) :g (:g c) :b (:b c) :a 0.5}]
              (if collapsed?
                ;; Collapsed thinking — just header
                (rt-node (keyword tid) :thinking-block
                  {:x 0 :y 0 :w pane-w :h (+ card-h-header 4)}
                  :data {:collapse-id (keyword tid)}
                  :children
                  [(rt-node (keyword (str tid "-accent")) :accent-bar
                     {:x 2 :y 2 :w 3 :h (- card-h-header 0)}
                     :style {:bg accent-color :radius 2})
                   (rt-node (keyword (str tid "-hdr")) :tool-header
                     {:x 0 :y 0 :w pane-w :h card-h-header}
                     :data {:collapse-id (keyword tid)}
                     :text [header-op arrow-op])])
                ;; Expanded thinking — header + body
                (let [lines (mapcat #(wrap-line % (- max-chars 2))
                                    (str/split-lines merged-text))
                      body-ops (vec (map-indexed
                                      (fn [i line]
                                        {:text line :type :comment
                                         :from 0 :to (count line)
                                         :x (+ pad 8) :y (+ font-size (* i line-h))
                                         :size font-size
                                         :r (:r c) :g (:g c) :b (:b c) :a (* (:a c) 0.8)})
                                      lines))
                      body-h (+ 4 (* (count lines) line-h))
                      total-h (+ card-h-header body-h 4)]
                  (rt-node (keyword tid) :thinking-block
                    {:x 0 :y 0 :w pane-w :h total-h}
                    :data {:collapse-id (keyword tid)}
                    :children
                    [(rt-node (keyword (str tid "-accent")) :accent-bar
                       {:x 2 :y 2 :w 3 :h (- total-h 4)}
                       :style {:bg accent-color :radius 2})
                     (rt-node (keyword (str tid "-hdr")) :tool-header
                       {:x 0 :y 0 :w pane-w :h card-h-header}
                       :data {:collapse-id (keyword tid)}
                       :text [header-op arrow-op])
                     (rt-node (keyword (str tid "-body")) :thinking-body
                       {:x 0 :y card-h-header :w pane-w :h body-h}
                       :text body-ops)]))))

            ;; --- Tool card: status dot + header + collapsible result ---
            :tool-card
            (let [tool-name (:tool-name block)
                  tid (or (:tool-id block) (str "tool-" bi))
                  status (:status block)
                  pending? (= :pending status)
                  input (:input block)
                  ;; Navigation target: extract file path + line from tool input
                  nav-target (when input
                               (let [fp (or (:file_path input) (:path input))]
                                 (when (and (string? fp) (seq fp))
                                   {:file-path fp
                                    :line (or (:offset input) (:line input) 0)})))
                  summary (if input (tool-input-summary tool-name input)
                                    (some-> (:nodes block) first :tool-name (str "...")))
                  header-label (str tool-name (when (seq summary) (str "  " summary)))
                  header-label (if (> (count header-label) (- max-chars 6))
                                 (str (subs header-label 0 (- max-chars 9)) "...")
                                 header-label)
                  collapsed? (contains? collapsed (keyword tid))
                  c (trail-node-color :tool-call tool-name)
                  ;; Status dot: yellow = pending, green = complete
                  dot-color (if pending?
                              [0.95 0.85 0.25 (if pending? shimmer-alpha 1.0)]
                              [0.4 0.85 0.45 1.0])
                  ;; Header text alpha pulses with shimmer when pending
                  text-alpha (if pending? shimmer-alpha (:a c))
                  header-text-op {:text header-label :type :keyword
                                  :from 0 :to (count header-label)
                                  :x (+ pad 14) :y (+ font-size 2)
                                  :size font-size
                                  :r (:r c) :g (:g c) :b (:b c) :a text-alpha}
                  arrow-text (if collapsed? ">" "v")
                  arrow-op {:text arrow-text :type :comment
                            :from 0 :to 1
                            :x (- pane-w pad 10) :y (+ font-size 2)
                            :size font-size
                            :r (nth fg-dim 0) :g (nth fg-dim 1) :b (nth fg-dim 2) :a 0.5}
                  ;; Result body
                  result-content (:result-content block)
                  has-body? (and result-content (not collapsed?))
                  body-lines (when has-body?
                               (let [raw (if (string? result-content) result-content (pr-str result-content))
                                     all-lines (mapcat #(wrap-line % (- max-chars 2))
                                                       (str/split-lines raw))
                                     ;; Truncate to 2 lines max
                                     limited (take 2 all-lines)]
                                 (vec limited)))
                  body-h (if has-body? (+ 4 (* (count body-lines) line-h)) 0)
                  total-h (+ card-h-header body-h 6)
                  card-bg (:bg-subtle colors)]
              (rt-node (keyword (str "tc-" bi)) :tool-card-node
                {:x 0 :y 0 :w pane-w :h total-h}
                :style {:bg card-bg :radius 4
                        :border-width 1
                        :border-color (if nav-target (:border colors) (:border-subtle colors))}
                :data (when nav-target {:nav nav-target})
                :children
                (cond-> [(rt-node (keyword (str "dot-" bi)) :status-dot
                           {:x (+ pad 2) :y 10 :w 6 :h 6}
                           :style {:bg dot-color :radius 3})
                         (rt-node (keyword (str "tch-" bi)) :tool-header
                           {:x 0 :y 0 :w pane-w :h card-h-header}
                           :data {:collapse-id (keyword tid)}
                           :text [header-text-op arrow-op])]
                  has-body?
                  (conj (rt-node (keyword (str "tcb-" bi)) :tool-body
                          {:x 0 :y card-h-header :w pane-w :h body-h}
                          :text (vec (map-indexed
                                       (fn [i line]
                                         (let [rc (trail-node-color :tool-result nil)]
                                           {:text (str "  " line) :type :comment
                                            :from 0 :to (+ 2 (count line))
                                            :x pad :y (+ font-size (* i line-h))
                                            :size font-size
                                            :r (:r rc) :g (:g rc) :b (:b rc) :a (:a rc)}))
                                       body-lines)))))))

            ;; --- Tool group: collapsed run of 3+ file ops ---
            :tool-group
            (let [n (:count block)
                  cards (:cards block)
                  group-id (str "tg-" bi)
                  collapsed? (contains? collapsed (keyword group-id))
                  header-label (str n " file operations")
                  c {:r 0.55 :g 0.75 :b 0.65 :a 1.0}
                  arrow-text (if collapsed? ">" "v")
                  header-text-op {:text header-label :type :keyword
                                  :from 0 :to (count header-label)
                                  :x (+ pad 4) :y (+ font-size 2)
                                  :size font-size
                                  :r (:r c) :g (:g c) :b (:b c) :a (:a c)}
                  arrow-op {:text arrow-text :type :comment
                            :from 0 :to 1
                            :x (- pane-w pad 10) :y (+ font-size 2)
                            :size font-size
                            :r (:r c) :g (:g c) :b (:b c) :a 0.5}]
              (if collapsed?
                (rt-node (keyword group-id) :tool-group-node
                  {:x 0 :y 0 :w pane-w :h (+ card-h-header 6)}
                  :style {:bg (:bg-subtle colors) :radius 4
                          :border-width 1 :border-color (:border-subtle colors)}
                  :children
                  [(rt-node (keyword (str group-id "-hdr")) :tool-header
                     {:x 0 :y 0 :w pane-w :h card-h-header}
                     :data {:collapse-id (keyword group-id)}
                     :text [header-text-op arrow-op])])
                ;; Expanded: show each card as a summary line
                (let [item-lines
                      (vec (map-indexed
                             (fn [i card]
                               (let [tn (:tool-name card)
                                     inp (:input card)
                                     summary (if inp (tool-input-summary tn inp) "")
                                     label (str ">> " tn " " summary)
                                     label (if (> (count label) max-chars)
                                             (str (subs label 0 (- max-chars 3)) "...")
                                             label)
                                     tc (trail-node-color :tool-call tn)]
                                 {:text label :type :keyword
                                  :from 0 :to (count label)
                                  :x (+ pad 4) :y (+ font-size (* i line-h))
                                  :size font-size
                                  :r (:r tc) :g (:g tc) :b (:b tc) :a (:a tc)}))
                             cards))
                      body-h (+ 4 (* n line-h))
                      total-h (+ card-h-header body-h 6)]
                  (rt-node (keyword group-id) :tool-group-node
                    {:x 0 :y 0 :w pane-w :h total-h}
                    :style {:bg (:bg-subtle colors) :radius 4
                            :border-width 1 :border-color (:border-subtle colors)}
                    :children
                    [(rt-node (keyword (str group-id "-hdr")) :tool-header
                       {:x 0 :y 0 :w pane-w :h card-h-header}
                       :data {:collapse-id (keyword group-id)}
                       :text [header-text-op arrow-op])
                     (rt-node (keyword (str group-id "-body")) :tool-group-body
                       {:x 0 :y card-h-header :w pane-w :h body-h}
                       :text item-lines)]))))

            ;; Fallback — render as reasoning
            (let [text (pr-str block)
                  lines (wrap-line text max-chars)
                  c {:r 0.75 :g 0.75 :b 0.75 :a 1.0}
                  text-ops (vec (map-indexed
                                  (fn [i line]
                                    {:text line :type :comment
                                     :from 0 :to (count line)
                                     :x pad :y (+ font-size (* i line-h))
                                     :size font-size
                                     :r (:r c) :g (:g c) :b (:b c) :a (:a c)})
                                  lines))
                  h (+ 4 (* (count lines) line-h))]
              (rt-node (keyword (str "unknown-" bi)) :reasoning-block
                {:x 0 :y 0 :w pane-w :h h}
                :text text-ops))))
        blocks))))

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

        :thinking
        (let [last-entry (peek acc)]
          (if (and last-entry (= :thinking (:kind last-entry)))
            (conj (pop acc) (update last-entry :text str (:text node)))
            (conj acc {:kind :thinking :text (:text node)
                       :color (trail-node-color :thinking nil)})))

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
   !ai-provider !agent-output !sidebar-visible !sidebar-state !current-file cmd-panel-h status-bar-h]
  (m/latest
    (fn [panel focus caret-visible scroll-y viewport settings active-font
         agent-output sidebar-visible? sidebar-state current-file]
      (let [sb-w (if (boolean sidebar-visible?) sidebar-w 0)
            dpr (:dpr viewport)
            snap? (:snap-to-pixel? settings)
            font-size (:font-size settings)
            char-advance (maybe-snap (* font-size (:char-width active-font)) dpr snap?)
            invisible {:x 0 :y 0 :w 0 :h 0 :r 0 :g 0 :b 0 :a 0}

            ;; --- Instance 0: agent output background ---
            agent-panel-h (compute-agent-panel-h agent-output font-size (:height viewport)
                                                 (:width viewport) char-advance)
            agent-visible? (and (some? (:status agent-output)) (not (some? current-file)))
            agent-bg (if agent-visible?
                       (let [agent-y0 (maybe-snap
                                        (+ scroll-y (- (:height viewport)
                                                       cmd-panel-h status-bar-h agent-panel-h 12))
                                        dpr snap?)]
                         {:x 0 :y agent-y0
                          :w (:width viewport) :h (+ agent-panel-h 12)
                          :r 0.10 :g 0.10 :b 0.13 :a 1.0})
                       invisible)

            ;; --- Instance 1: command panel background (elevated + top border) ---
            ;; Always visible when file is open (persistent chat input)
            panel-visible? (or (:visible panel) (some? current-file))
            panel-y (maybe-snap (+ scroll-y (- (:height viewport) cmd-panel-h status-bar-h)) dpr snap?)
            elevated-surface (or (:elevated (:surfaces dt)) [0.10 0.13 0.19 1.0])
            cmd-bg (if panel-visible?
                     {:x 0 :y panel-y :w (:width viewport) :h cmd-panel-h
                      :r (nth elevated-surface 0) :g (nth elevated-surface 1)
                      :b (nth elevated-surface 2) :a (nth elevated-surface 3)}
                     invisible)

            ;; --- Instance 2: caret (offset by sidebar width) ---
            text-x (+ (cmd-text-start-x @!ai-provider font-size (:char-width active-font) dpr snap?) sb-w)
            caret (if (and panel-visible? caret-visible (= focus :command-panel))
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
    (m/watch !sidebar-visible)
    (m/watch !sidebar-state)
    (m/watch !current-file)))

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
   !sidebar-state !sidebar-visible !extract-preview
   !shimmer-phase !trail-collapsed !active-pane !scroll-x !chat-scroll-y !chat-input !focus
   layout-x layout-y cmd-panel-h status-bar-h]
  (m/latest
    (fn [doc panel provider agent-output agent-scroll-y scroll-y viewport fold-state settings active-font
         current-file flow-state collapsed-groups hovered-row-idx drag-state
         sidebar-state sidebar-visible? extract-preview
         shimmer-phase trail-collapsed active-pane scroll-x chat-scroll-y chat-input focus]
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
              ;; Scrolled x for editor text only — gutter/line-nums stay fixed
              editor-lx (maybe-snap (- layout-x (or scroll-x 0)) dpr snap?)
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

              ;; MODE-SWITCH: file open (3-pane), flow canvas, or plain editor
              file-open? (some? current-file)
              [editor-ops final-line-mapping line-num-ops]
              (if (:rt-node extract-preview)
                ;; Extract preview on right half
                (let [half-w (/ content-vw 2)
                      preview-tree (when-let [rt (:rt-node extract-preview)]
                                     (resolve-layout
                                       (rt-node :extract-preview-root :rect
                                                {:x half-w :y 0 :w half-w :h (:height viewport)}
                                                :layout {:direction :column :padding [16 16 16 16] :gap 8}
                                                :children [(rt-node :extract-label :text
                                                                    {:x 0 :y 0 :w (- half-w 32) :h 24}
                                                                    :text [{:text "Compiled Preview" :type :keyword
                                                                            :from 0 :to 16 :x 0 :y 16
                                                                            :size 14 :r 0.55 :g 0.55 :b 0.60 :a 1.0}])
                                                           (assoc-in rt [:bounds :w] (- half-w 32))])))]
                  [(if preview-tree (tree->text-ops preview-tree) [])
                   (vec (range (count (:lines doc))))
                   []])
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
                          result (layout-fn tokenized-visible editor-lx adjusted-y font-size
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
                          result (layout-fn tokenized-all editor-lx layout-y font-size
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
                      [(filterv seq (:render-ops result)) mapping nums]))))))

              ;; When file is open, clip editor text to left 40% and add right-tree text ops
              [editor-ops final-line-mapping line-num-ops]
              (if (and file-open? (not (flow-canvas-active? flow-state)) (not (:rt-node extract-preview)))
                (let [code-w (int (* content-vw 0.4))
                      ;; Clip text ops to editor pane [layout-x, code-w] — left (gutter edge) AND right
                      font-cw (:char-width active-font 0.56)
                      clip-left layout-x ;; left boundary = gutter right edge (text start)
                      header-h 36
                      clip-top (+ scroll-y header-h) ;; viewport-pinned top edge below header
                      clip-sub (fn [sub]
                                 (let [x (or (:x sub) 0)
                                       y (or (:y sub) 0)
                                       fs (or (:size sub) font-size)
                                       cw (* fs font-cw)
                                       txt (or (:text sub) "")
                                       text-end (+ x (* (count txt) cw))]
                                   ;; Drop if outside horizontal [clip-left, code-w] or above header
                                   (when (and (< x code-w) (> text-end clip-left) (>= y clip-top))
                                     ;; Left-trim chars before gutter edge
                                     (let [skip (if (< x clip-left) (min (count txt) (int (Math/ceil (/ (- clip-left x) cw)))) 0)
                                           adj-x (+ x (* skip cw))
                                           adj-txt (if (pos? skip) (subs txt skip) txt)
                                           ;; Right-truncate at code-w
                                           max-chars (if (pos? cw)
                                                       (max 0 (int (/ (- code-w adj-x) cw)))
                                                       1000)
                                           final-txt (if (> (count adj-txt) max-chars)
                                                       (subs adj-txt 0 max-chars)
                                                       adj-txt)]
                                       (when (seq final-txt)
                                         (assoc sub :text final-txt :x adj-x
                                                :from skip :to (+ skip (count final-txt))))))))
                      clip-op (fn [op]
                                (if (vector? op)
                                  (let [clipped (into [] (keep clip-sub) op)]
                                    (when (seq clipped) clipped))
                                  (clip-sub op)))
                      clipped (into [] (keep clip-op) editor-ops)
                      shimmer-alpha (if shimmer-phase 0.9 0.4)
                      right-tree (resolve-layout
                                   (build-file-layout content-vw (:height viewport)
                                                      current-file agent-output font-size
                                                      shimmer-alpha trail-collapsed
                                                      :active-pane active-pane :char-advance char-advance
                                                      :chat-scroll-y (or chat-scroll-y 0)
                                                      :chat-input chat-input :focus focus))
                      right-text-ops (tree->text-ops right-tree)
                      ;; Pin to viewport: offset by scroll-y so camera pan doesn't move it
                      pinned-ops (mapv (fn [op]
                                         (if (vector? op)
                                           (mapv #(update % :y + scroll-y) op)
                                           (update op :y + scroll-y)))
                                       right-text-ops)]
                  (let [clip-ln (fn [op]
                                  (if (vector? op)
                                    (let [f (filterv #(>= (or (:y %) 0) clip-top) op)]
                                      (when (seq f) f))
                                    (when (>= (or (:y op) 0) clip-top) op)))
                        clipped-ln (into [] (keep clip-ln) line-num-ops)]
                    [(into (vec clipped) pinned-ops) final-line-mapping clipped-ln]))
                [editor-ops final-line-mapping line-num-ops])

              ;; Bottom clip: filter out text ops that would render inside cmd panel / status bar
              bottom-clip-y (+ scroll-y (- (:height viewport) cmd-panel-h status-bar-h))
              clip-bottom (fn [ops]
                            (into []
                              (keep (fn [op]
                                      (if (vector? op)
                                        (let [clipped (filterv #(< (or (:y %) 0) bottom-clip-y) op)]
                                          (when (seq clipped) clipped))
                                        (when (< (or (:y op) 0) bottom-clip-y) op))))
                              ops))

              ;; Offset editor/flow text ops by sidebar width
              offset-editor-ops (offset-text-ops (clip-bottom editor-ops) sb-w)
              offset-line-num-ops (offset-text-ops (clip-bottom line-num-ops) sb-w)

              ;; Command panel ops (if visible or file open) — offset by sb-w
              panel-visible? (or (:visible panel) file-open?)
              cmd-ops (when panel-visible?
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
              cmd-lines (if panel-visible? (vec (filter some? cmd-ops)) [])]

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

                agent-panel-h (if file-open?
                                0 ;; Chat pane shows trail; suppress bottom panel
                                (compute-agent-panel-h agent-output font-size (:height viewport)
                                                       (:width viewport) char-advance))
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
                                      cmd-lines
                                      ;; Suppress bottom agent output when 3-pane chat shows trail
                                      (when-not file-open? agent-lines)
                                      status-lines))
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
    (m/watch !sidebar-visible)
    (m/watch !extract-preview)
    (m/watch !shimmer-phase)
    (m/watch !trail-collapsed)
    (m/watch !active-pane)
    (m/watch !scroll-x)
    (m/watch !chat-scroll-y)
    (m/watch !chat-input)
    (m/watch !focus)))

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
   find-form-fn eval-form-fn atlas & {:keys [font-manifest !sidebar-visible !file-load-request !preview-el initial-file]}]

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
        !scroll-x (atom 0)

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

        ;; Focus hierarchy — which pane is active
        !active-pane (atom :editor)

        ;; Font configuration - use passed manifest or default
        default-manifest {:fonts [{:name "DejaVu Sans Mono"
                                   :id "dejavu-sans-mono"
                                   :charWidth 0.56
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
                         {:id "dejavu-sans-mono" :name "DejaVu Sans Mono" :charWidth 0.56})
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
        !sidebar-state (atom {:project nil           ;; {:name :path} or nil
                              :expanded-dirs #{}
                              :dir-cache {}
                              :home-dirs nil
                              :scroll-y 0
                              :hovered-id nil
                              :loading? false})
        !ai-provider (atom :claude)     ;; :claude | :codex | :gemini
        !agent-output (atom nil)        ;; {:status :provider :prompt :output :run-id :trail :tool-buf}
        !agent-scroll-y (atom 0)        ;; scroll offset within agent output panel
        !chat-scroll-y (atom 0)         ;; scroll offset within chat pane (3-pane mode)
        !chat-input (atom {:text "" :cursor 0})  ;; dedicated text buffer for chat pane input
        !mouse-x (atom 0)               ;; last known mouse X (viewport-relative)
        !mouse-y (atom 0)               ;; last known mouse Y (viewport-relative)
        !flow-state (atom (initial-flow-state))  ;; V0 flow state machine
        !collapsed-groups (atom #{})           ;; set of collapsed group status strings
        !hovered-row-idx (atom nil)            ;; 0-based flat ticket index under mouse cursor
        !drag-state (atom {:phase :idle})      ;; drag state machine: :idle/:pending/:dragging
        !extract-preview (atom nil)           ;; {:rt-node :ir :source-url :selector :html-styles}
        !trail-collapsed (atom #{})            ;; set of tool-ids whose card body is collapsed
        !shimmer-phase (atom false)            ;; toggled by blink timer — maps to shimmer-alpha

        ;; Extract preview overlay watcher — show/hide DOM overlay + populate HTML
        _ (add-watch !extract-preview :overlay
            (fn [_ _ _ new-val]
              (when-let [el (and !preview-el @!preview-el)]
                (if new-val
                  (do (set! (.-display (.-style el)) "block")
                      ;; Populate with reconstructed HTML if available
                      (if-let [html (:html new-val)]
                        (set! (.-innerHTML el)
                              (str "<div style='color:#888;font-size:12px;margin-bottom:8px;'>Source: "
                                   (or (:source-url new-val) "extracted") "</div>"
                                   "<div style='padding:16px;border:1px solid #333;border-radius:8px;background:#0d0d0f;'>"
                                   html "</div>"))
                        (set! (.-innerHTML el)
                              "<div style='color:#666;padding:40px;text-align:center;'>No HTML preview available.<br>Run extractor to capture source HTML.</div>")))
                  (do (set! (.-display (.-style el)) "none")
                      (set! (.-innerHTML el) ""))))))

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
        (fn [path root-path & {:keys [target-line]}]
          (fetch-edn! (str "/api/read-file?path=" (js/encodeURIComponent path)
                           "&root=" (js/encodeURIComponent root-path))
                      (fn [result]
                        (if (:error result)
                          (js/console.error "[SIDEBAR] File read error:" (:error result))
                          (let [lines (str/split-lines (:content result))]
                            (reset! !current-file {:path path :name (last (str/split path #"/"))})
                            (reset! !scroll-x 0)
                            (reset! !file-load-request
                                    (cond-> {:lines lines}
                                      target-line (assoc :target-line target-line))))))))

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

                :thinking-delta
                (do (swap! !agent-output
                      (fn [ao]
                        (-> ao
                          (update :output str (:text evt))
                          (update :trail conj {:kind :thinking :text (:text evt)}))))
                    (auto-scroll-agent!))

                :thinking-start
                nil  ;; marker only, content comes via :thinking-delta

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

        ;; Register global injection fns at startup (always available for Claude-in-Chrome)
        _ (set! (.-__softland_inject_preview js/window)
                (fn [data-json]
                  (let [data (js->clj (.parse js/JSON data-json) :keywordize-keys true)]
                    (-> (js/fetch "/api/extract/compile"
                          (clj->js {:method "POST"
                                    :headers {"Content-Type" "application/edn"}
                                    :body (pr-str {:tree data :source-url (.-href js/location)})}))
                        (.then #(.text %))
                        (.then (fn [text]
                                 (let [result (reader/read-string text)]
                                   (if (:ok result)
                                     (do (reset! !extract-preview {:rt-node (:rt-node result)
                                                                    :ir (:ir result)
                                                                    :source-url (:source-url result)})
                                         (show-flow-info! (str "Component compiled.\nSource: " (:source-url result))))
                                     (show-flow-info! (str "Compile failed: " (:error result)))))))
                        (.catch (fn [err]
                                  (show-flow-info! (str "Compile error: " (.-message err)))))))))
        _ (set! (.-__softland_inject_rt_node js/window)
                (fn [rt-node-json]
                  (let [rt-node (js->clj (.parse js/JSON rt-node-json) :keywordize-keys true)]
                    (reset! !extract-preview {:rt-node rt-node})
                    (show-flow-info! "rt-node injected directly."))))

        submit-agent-run!
        (fn [cmd-text]
          (let [doc @!editor-doc
                file-path (:path @!current-file)
                scroll-y @!scroll-y
                viewport @!viewport
                parsed (parse-agent-command cmd-text @!ai-provider)
                cwd (or (:path (:project @!sidebar-state))
                        (some-> file-path (str/split #"/") butlast seq (str/join "/"))
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


              :extract-component
              (if (:clear? parsed)
                (do (reset! !extract-preview nil)
                    (show-flow-info! "Extract preview cleared."))
                (let [url (:url parsed)
                      selector (:selector parsed)
                      msg (cond
                            (and url selector) (str "Extract mode active.\nURL: " url "\nSelector: " selector
                                                    "\n\nWaiting for extracted data...\nUse /extract clear to exit.")
                            url                (str "Extract mode active.\nURL: " url "\nSelector: auto-detect"
                                                    "\n\nWaiting for extracted data...\nUse /extract clear to exit.")
                            selector           (str "Extract mode active.\nSelector: " selector
                                                    "\n\nWaiting for extracted data...\nUse /extract clear to exit.")
                            :else              (str "Extract mode active.\nSelector: auto-detect"
                                                    "\n\nWaiting for extracted data...\nUse /extract clear to exit."))]
                  (reset! !extract-preview {:pending? true :url url :selector selector})
                  (show-flow-info! msg)))

              :hardcode
              (let [name (:name parsed)
                    ;; --- shadcn v4 button — extracted 2026-03-01 from live page ---
                    ;; Colors (RGBA 0-1, converted from lab/oklab via canvas)
                    ;; Token map:   shadcn var           → RGBA
                    ;;   --color-primary       lab(90.95%) → [0.898 0.898 0.898 1.0]
                    ;;   --color-primary-fg    lab(7.78%)  → [0.09  0.09  0.09  1.0]
                    ;;   --color-secondary     lab(15.20%) → [0.149 0.149 0.149 1.0]
                    ;;   --color-secondary-fg  lab(98.26%) → [0.98  0.98  0.98  1.0]
                    ;;   --color-destructive   lab(63.7%)  → [1.0   0.392 0.404 1.0]
                    ;;   --color-foreground    lab(98.26%) → [0.98  0.98  0.98  1.0]
                    ;;   --color-muted         lab(15.20%) → [0.149 0.149 0.149 1.0]
                    ;;   --color-border        lab(100%/0.1) → [1.0 1.0 1.0 0.1]
                    ;;   --color-background    lab(2.75%)  → [0.039 0.039 0.039 1.0]
                    ;;   --radius: 0.625rem = 10px
                    ;; Shared: radius 10, font 14/500, padding [0 10 0 10], h 32
                    ;; Hover rules (from Tailwind classes):
                    ;;   default:     hover:bg-primary/80      → alpha 1.0 → 0.8
                    ;;   outline:     hover:bg-muted            → swap bg to muted
                    ;;   secondary:   hover:bg-secondary/80    → alpha 1.0 → 0.8
                    ;;   ghost:       hover:bg-muted            → swap bg to muted
                    ;;   destructive: hover:bg-destructive/30  → alpha 0.2 → 0.3
                    ;;   link:        hover:underline           → text decoration
                    sc-primary     [0.898 0.898 0.898 1.0]
                    sc-primary-fg  [0.09  0.09  0.09  1.0]
                    sc-secondary   [0.149 0.149 0.149 1.0]
                    sc-secondary-fg [0.98  0.98  0.98  1.0]
                    sc-destructive [1.0   0.392 0.404 1.0]
                    sc-foreground  [0.98  0.98  0.98  1.0]
                    sc-fg-muted    [0.831 0.831 0.831 1.0]
                    sc-muted       [0.149 0.149 0.149 1.0]
                    sc-border      [1.0   1.0   1.0   0.15]
                    sc-bg          [0.039 0.039 0.039 1.0]
                    sc-radius      10
                    sc-h           32
                    sc-font        14
                    ;; Helper: make a button rt-node from extracted values
                    make-btn (fn [id label bg-color fg-color & {:keys [border-w border-c]}]
                               (let [char-w (* sc-font 0.56)
                                     text-w (* (count label) char-w)
                                     btn-w  (+ text-w 20)]  ;; 10px padding each side
                                 (rt-node id :button
                                          {:x 0 :y 0 :w btn-w :h sc-h}
                                          :style (cond-> {:bg bg-color
                                                          :radius sc-radius}
                                                   border-w (assoc :border-width border-w)
                                                   border-c (assoc :border-color border-c))
                                          :text [{:text label :type :text
                                                  :from 0 :to (count label)
                                                  :x 10 :y (+ sc-font 5)
                                                  :size sc-font
                                                  :r (nth fg-color 0) :g (nth fg-color 1)
                                                  :b (nth fg-color 2) :a (nth fg-color 3)}])))
                    section-label (fn [id text]
                                   (rt-node id :text
                                            {:x 0 :y 0 :w 200 :h 18}
                                            :text [{:text text :type :keyword
                                                    :from 0 :to (count text) :x 0 :y 13
                                                    :size 12 :r 0.55 :g 0.55 :b 0.60 :a 1.0}]))
                    node (case name
                           "button"
                           (rt-node :hc-button-demo :rect
                                    {:x 0 :y 0 :w 520 :h 500}
                                    :style {:bg sc-bg}
                                    :layout {:direction :column :padding [24 24 24 24] :gap 12}
                                    :children
                                    [;; Title
                                     (rt-node :hc-title :text
                                              {:x 0 :y 0 :w 472 :h 24}
                                              :text [{:text "shadcn/ui Button (v4) -- extracted from live page"
                                                      :type :keyword :from 0 :to 49 :x 0 :y 17
                                                      :size 15 :r 0.98 :g 0.98 :b 0.98 :a 1.0}])
                                     ;; Variants section
                                     (section-label :hc-s1 "Variants")
                                     ;; Row of all 6 variants
                                     (rt-node :hc-variant-row :rect
                                              {:x 0 :y 0 :w 472 :h (+ sc-h 0)}
                                              :style {:bg [0 0 0 0]}
                                              :layout {:direction :row :gap 10}
                                              :children
                                              [(make-btn :hc-default "Default" sc-primary sc-primary-fg)
                                               (make-btn :hc-secondary "Secondary" sc-secondary sc-secondary-fg)
                                               (make-btn :hc-destructive "Destructive"
                                                         (assoc sc-destructive 3 0.2) sc-destructive)
                                               (make-btn :hc-outline "Outline"
                                                         [1.0 1.0 1.0 0.04] sc-fg-muted
                                                         :border-w 1 :border-c sc-border)
                                               (make-btn :hc-ghost "Ghost" [0 0 0 0] sc-fg-muted)
                                               (make-btn :hc-link "Link" [0 0 0 0] [0.35 0.55 0.95 1.0])])
                                     ;; Hover states section
                                     (section-label :hc-s2 "Hover states (simulated)")
                                     (rt-node :hc-hover-row :rect
                                              {:x 0 :y 0 :w 472 :h sc-h}
                                              :style {:bg [0 0 0 0]}
                                              :layout {:direction :row :gap 10}
                                              :children
                                              [(make-btn :hc-hov-default "Default"
                                                         (assoc sc-primary 3 0.8) sc-primary-fg)
                                               (make-btn :hc-hov-secondary "Secondary"
                                                         (assoc sc-secondary 3 0.8) sc-secondary-fg)
                                               (make-btn :hc-hov-destructive "Destructive"
                                                         (assoc sc-destructive 3 0.3) sc-destructive)
                                               (make-btn :hc-hov-outline "Outline"
                                                         sc-muted sc-foreground
                                                         :border-w 1 :border-c sc-border)
                                               (make-btn :hc-hov-ghost "Ghost"
                                                         sc-muted sc-foreground)
                                               (make-btn :hc-hov-link "Link" [0 0 0 0] [0.35 0.55 0.95 1.0])])
                                     ;; Sizes section
                                     (section-label :hc-s3 "Sizes")
                                     (rt-node :hc-size-row :rect
                                              {:x 0 :y 0 :w 472 :h 48}
                                              :style {:bg [0 0 0 0]}
                                              :layout {:direction :row :gap 10 :align :end}
                                              :children
                                              [(let [f 12 ch (* f 0.56) l "Extra Small" w (+ (* (count l) ch) 16)]
                                                 (rt-node :hc-sz-xs :button
                                                          {:x 0 :y 0 :w w :h 24}
                                                          :style {:bg sc-primary :radius 8}
                                                          :text [{:text l :type :text :from 0 :to (count l)
                                                                  :x 8 :y 17 :size f
                                                                  :r 0.09 :g 0.09 :b 0.09 :a 1.0}]))
                                               (let [f 12 ch (* f 0.56) l "Small" w (+ (* (count l) ch) 20)]
                                                 (rt-node :hc-sz-sm :button
                                                          {:x 0 :y 0 :w w :h 28}
                                                          :style {:bg sc-primary :radius 8}
                                                          :text [{:text l :type :text :from 0 :to (count l)
                                                                  :x 10 :y 19 :size f
                                                                  :r 0.09 :g 0.09 :b 0.09 :a 1.0}]))
                                               (make-btn :hc-sz-md "Default" sc-primary sc-primary-fg)
                                               (let [f 14 ch (* f 0.56) l "Large" w (+ (* (count l) ch) 28)]
                                                 (rt-node :hc-sz-lg :button
                                                          {:x 0 :y 0 :w w :h 40}
                                                          :style {:bg sc-primary :radius 10}
                                                          :text [{:text l :type :text :from 0 :to (count l)
                                                                  :x 14 :y 26 :size f
                                                                  :r 0.09 :g 0.09 :b 0.09 :a 1.0}]))])
                                     ;; Token mapping section
                                     (section-label :hc-s4 "Token mapping: shadcn v4 -> Softland dt")
                                     (rt-node :hc-token-info :text
                                              {:x 0 :y 0 :w 472 :h 100}
                                              :text [{:text "primary [229,229,229] -> dt :fg" :type :text
                                                      :from 0 :to 30 :x 0 :y 14
                                                      :size 11 :r 0.55 :g 0.55 :b 0.60 :a 1.0}
                                                     {:text "secondary [38,38,38] -> dt :bg-muted" :type :text
                                                      :from 0 :to 35 :x 0 :y 28
                                                      :size 11 :r 0.55 :g 0.55 :b 0.60 :a 1.0}
                                                     {:text "destructive [255,100,103] -> dt :destructive" :type :text
                                                      :from 0 :to 44 :x 0 :y 42
                                                      :size 11 :r 0.55 :g 0.55 :b 0.60 :a 1.0}
                                                     {:text "background [10,10,10] -> dt :bg" :type :text
                                                      :from 0 :to 30 :x 0 :y 56
                                                      :size 11 :r 0.55 :g 0.55 :b 0.60 :a 1.0}
                                                     {:text "foreground [250,250,250] -> dt :fg" :type :text
                                                      :from 0 :to 33 :x 0 :y 70
                                                      :size 11 :r 0.55 :g 0.55 :b 0.60 :a 1.0}
                                                     {:text "radius: 10px | border: white@15% | font: 14/500" :type :text
                                                      :from 0 :to 48 :x 0 :y 84
                                                      :size 11 :r 0.55 :g 0.55 :b 0.60 :a 1.0}])])

                           ;; Default: unknown component
                           (rt-node :hc-unknown :rect
                                    {:x 0 :y 0 :w 300 :h 60}
                                    :style {:bg (:bg-muted (:colors dt))}
                                    :text [{:text (str "Unknown: " name) :type :text
                                            :from 0 :to (+ 9 (count name)) :x 16 :y 36
                                            :size 14 :r 0.90 :g 0.30 :b 0.30 :a 1.0}]))]
                (reset! !extract-preview {:rt-node node})
                (show-flow-info! (str "Hardcoded: " name)))

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



        ;; Initial sidebar data fetch
        _ (when (and !sidebar-visible @!sidebar-visible)
            (fetch-home-dirs!))
        ;; Fetch data when sidebar becomes visible
        _ (when !sidebar-visible
            (add-watch !sidebar-visible :sidebar-fetch
                       (fn [_ _ old-vis new-vis]
                         (when (and new-vis (not old-vis))
                           (fetch-home-dirs!)))))

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
        >shimmer-timer (make-blink-timer) ;; Separate blink for tool-card shimmer
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
        <chat-keyboard (<chat-input-keys >keyboard-events !focus)
        <settings-keyboard (<settings-panel-keys >keyboard-events !focus)

        ;; System clipboard paste: listen for native paste event (fired by Ctrl+V)
        _ (let [paste-handler
                (fn [e]
                  (let [text (.getData (.-clipboardData e) "text/plain")]
                    (when (seq text)
                      (.preventDefault e)
                      (reset! !clipboard text)
                      (case @!focus
                        :editor
                        (let [doc @!editor-doc
                              lengths (mapv count (:lines doc))]
                          (save-undo! (:lines doc) (:cursor doc))
                          (let [new-doc (editor-apply-event doc {:type :paste} lengths text)]
                            (reset! !editor-doc new-doc)
                            (reset! !caret-visible true)))
                        :command-panel
                        (let [panel @!cmd-panel
                              new-panel (cmd-panel-apply-event panel {:type :paste} text)]
                          (reset! !cmd-panel new-panel)
                          (reset! !caret-visible true))
                        :chat
                        (let [ci @!chat-input
                              ci-text (:text ci)
                              ci-cursor (:cursor ci)
                              new-text (str (subs ci-text 0 ci-cursor) text (subs ci-text ci-cursor))]
                          (reset! !chat-input {:text new-text :cursor (+ ci-cursor (count text))})
                          (reset! !caret-visible true))
                        nil))))]
            (.addEventListener js/window "paste" paste-handler))]

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
      ;; SHIMMER TIMER CONSUMER (pulses pending tool cards)
      ;; =====================================================================
      (->> >shimmer-timer
           (m/reduce (fn [_ v] (reset! !shimmer-phase v) nil) nil))

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
           (m/reduce (fn [_ wheel-evt]
                       (let [delta (:dy wheel-evt 0)
                             dx (:dx wheel-evt 0)
                             shift? (:shift? wheel-evt)
                             viewport @!viewport
                             settings @!settings
                             dpr (:dpr viewport)
                             snap? (:snap-to-pixel? settings)
                             font-size (:font-size settings)
                             char-advance (* font-size (:char-width @!active-font))
                             sb-vis? (and !sidebar-visible @!sidebar-visible)
                             mouse-x @!mouse-x
                             in-sidebar? (and sb-vis? (< mouse-x sidebar-w))
                             agent-output @!agent-output
                             agent-h (if (some? (:path @!current-file))
                                       0 ;; Suppress agent panel when 3-pane layout active
                                       (compute-agent-panel-h agent-output font-size
                                                              (:height viewport) (:width viewport)
                                                              char-advance))
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
                           ;; Scroll chat pane when mouse is over it (3-pane mode)
                           (let [file-open? (some? (:path @!current-file))
                                 sb-off (if sb-vis? sidebar-w 0)
                                 cw (- (:width viewport) sb-off)
                                 code-w (int (* cw 0.4))
                                 chat-w (int (* cw 0.3))
                                 rel-mx (- mouse-x sb-off)
                                 in-chat? (and file-open? (>= rel-mx code-w) (< rel-mx (+ code-w chat-w)))]
                             (and file-open? in-chat?))
                           (swap! !chat-scroll-y #(max 0 (+ % delta)))

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
                             ;; Normal editor scroll: vertical + horizontal (skip if mouse over preview pane)
                             (when (or (not (some? (:path @!current-file)))
                                       (< (- mouse-x (if sb-vis? sidebar-w 0))
                                          (int (* (- (:width viewport) (if sb-vis? sidebar-w 0)) 0.4))))
                             (do (swap! !scroll-y #(maybe-snap (+ % delta) dpr snap?))
                                 ;; Horizontal scroll: only within editor pane
                                 (let [h-delta (if shift? delta dx)
                                       sb-off (if sb-vis? sidebar-w 0)
                                       cw (- (:width viewport) sb-off)
                                       editor-right (if (some? @!current-file)
                                                      (+ sb-off (int (* cw 0.4)))
                                                      (+ sb-off cw))]
                                   (when (and (not (zero? h-delta)) (< mouse-x editor-right))
                                     (swap! !scroll-x #(max 0 (+ (or % 0) h-delta))))))))))
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
                           cmd-visible? (or (:visible cmd-panel) (some? @!current-file))
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
                        ;; Click in editor area — check flow canvas, then editor
                        (do
                        (when (:visible @!cmd-panel)
                          (swap! !cmd-panel assoc :visible false))
                        (cond
                          ;; Flow canvas mode: rect tree hit-test
                          (flow-canvas-active? @!flow-state)
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

                          ;; File layout mode: set active pane + click in chat → toggle tool card collapse
                          (some? @!current-file)
                          (let [content-w (- (:width viewport) (if sb-vis? sidebar-w 0))
                                code-w (int (* content-w 0.4))
                                chat-w (int (* content-w 0.55))
                                ;; Adjust x relative to content area (subtract sidebar)
                                rel-x (- x (if sb-vis? sidebar-w 0))
                                in-chat? (and (>= rel-x code-w) (< rel-x (+ code-w chat-w)))
                                in-editor? (< rel-x code-w)
                                ;; Focus hierarchy: set active pane based on click position
                                clicked-pane (cond
                                               in-editor? :editor
                                               in-chat? :chat
                                               :else :preview)]
                            (reset! !active-pane clicked-pane)
                            ;; Editor clicks: place cursor + set focus (same as normal mode)
                            (when in-editor?
                              (let [adj-y (+ y scroll-y)
                                    dpr (:dpr @!viewport)
                                    snap? (:snap-to-pixel? @!settings)
                                    font-size (:font-size @!settings)
                                    char-width (:char-width @!active-font)
                                    line-h (maybe-snap (* font-size (:line-height @!settings)) dpr snap?)
                                    char-w (maybe-snap (* font-size char-width) dpr snap?)
                                    elx (maybe-snap (- layout-x (or @!scroll-x 0)) dpr snap?)
                                    ely (maybe-snap layout-y dpr snap?)
                                    gutter-x (- elx gutter-w)
                                    gutter-right (+ gutter-x gutter-w)]
                                (if (and (>= x gutter-x) (< x gutter-right))
                                  ;; Gutter click - toggle fold
                                  (let [text-result @!text-geo
                                        line-mapping (or (:line-mapping text-result) [])
                                        visual-line (max 0 (Math/floor (/ (- adj-y ely) line-h)))
                                        logical-line (get line-mapping visual-line visual-line)
                                        regions (detect-folds-fn (:lines @!editor-doc)
                                                                 (mapv count (:lines @!editor-doc)))
                                        fold-region (first (filter #(= (:start-line %) logical-line)
                                                                   (or regions [])))]
                                    (when fold-region
                                      (swap! !folded-lines
                                             (fn [folded]
                                               (if (contains? folded logical-line)
                                                 (disj folded logical-line)
                                                 (conj folded logical-line))))))
                                  ;; Normal click - place cursor
                                  (let [text-result @!text-geo
                                        line-mapping (or (:line-mapping text-result) [])
                                        lengths (mapv count (:lines @!editor-doc))
                                        visual-line (max 0 (Math/floor (/ (- adj-y ely) line-h)))
                                        logical-line (get line-mapping visual-line
                                                          (min visual-line (dec (count lengths))))
                                        line-len (get lengths logical-line 0)
                                        col (-> (/ (- x elx) char-w)
                                                (Math/round)
                                                (max 0)
                                                (min line-len))
                                        pos {:line logical-line :col col}]
                                    (reset! !dragging? true)
                                    (swap! !editor-doc assoc
                                           :cursor pos
                                           :selection nil
                                           :desired-col col)))
                                (reset! !caret-visible true)
                                (reset! !focus :editor)))
                            (when in-chat?
                              ;; Click in bottom 36px of viewport → focus chat input
                              (if (>= y (- (:height viewport) 36))
                                (do (reset! !focus :chat)
                                    (reset! !caret-visible true))
                                ;; Hit-test the file layout tree for tool-header + nav clicks
                                (let [font-size (:font-size @!settings)
                                    char-advance (* font-size (:char-width @!active-font))
                                    shimmer-alpha (if @!shimmer-phase 0.9 0.4)
                                    tree (resolve-layout
                                           (build-file-layout content-w (:height viewport)
                                                              @!current-file @!agent-output font-size
                                                              shimmer-alpha @!trail-collapsed
                                                              :active-pane @!active-pane :char-advance char-advance
                                                              :chat-scroll-y (or @!chat-scroll-y 0)
                                                              :chat-input @!chat-input :focus @!focus))
                                    path (hit-test tree rel-x y)]
                                (when path
                                  ;; Priority 1: navigate (tool cards with :nav file path)
                                  (let [handled-nav?
                                        (some (fn [node]
                                                (when-let [nav (:nav (:data node))]
                                                  (let [file-path (:file-path nav)
                                                        target-line (or (:line nav) 0)
                                                        current-path (:path @!current-file)
                                                        project (or (:path (:project @!sidebar-state)) flow-cwd)
                                                        same-file? (or (= file-path current-path)
                                                                       (and current-path
                                                                            (str/ends-with? current-path file-path)))]
                                                    (if same-file?
                                                      ;; Same file: reposition cursor + scroll
                                                      (let [safe-line (min target-line
                                                                          (max 0 (dec (count (:lines @!editor-doc)))))]
                                                        (swap! !editor-doc assoc
                                                               :cursor {:line safe-line :col 0}
                                                               :selection nil :desired-col 0)
                                                        (let [line-h (* font-size (:line-height @!settings))]
                                                          (reset! !scroll-y (max 0 (- (* safe-line line-h) 100)))))
                                                      ;; Different file: load with target-line
                                                      (fetch-file! file-path project :target-line target-line))
                                                    (reset! !active-pane :editor)
                                                    true)))
                                              (rseq path))]
                                    ;; Priority 2: collapse toggle (thinking/grep cards without nav)
                                    (when-not handled-nav?
                                      (some (fn [node]
                                              (when (= :tool-header (:type node))
                                                (when-let [cid (:collapse-id (:data node))]
                                                  (swap! !trail-collapsed
                                                         (fn [s] (if (contains? s cid)
                                                                    (disj s cid)
                                                                    (conj s cid))))
                                                  true)))
                                            (rseq path))))))))

                          ;; Normal editor mode: cursor placement / fold toggle
                          :else
                          (let [adj-y (+ y scroll-y)
                                dpr (:dpr @!viewport)
                                snap? (:snap-to-pixel? @!settings)
                                font-size (:font-size @!settings)
                                char-width (:char-width @!active-font)
                                line-h (maybe-snap (* font-size (:line-height @!settings)) dpr snap?)
                                char-w (maybe-snap (* font-size char-width) dpr snap?)
                                layout-x (maybe-snap (- layout-x (or @!scroll-x 0)) dpr snap?)
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
                           new-id (when (and target (#{:sidebar-entry :ticket-row} (:type target)))
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
                         layout-x (maybe-snap (- layout-x (or @!scroll-x 0)) dpr snap?)
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
             nil)))

      ;; =====================================================================
      ;; GLOBAL KEYBOARD EVENTS CONSUMER
      ;; =====================================================================
      (->> <global-keys
           (m/reduce
             (fn [_ event]
               (when event
                 (case (:type event)
                   :toggle-command-panel
                   (let [visible? (:visible @!cmd-panel)
                         file-open? (some? (:path @!current-file))]
                     (if (and visible? (not file-open?))
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

                     (= @!focus :chat)
                     (do (reset! !chat-input {:text "" :cursor 0})
                         (reset! !focus :editor))

                     (or (:visible @!cmd-panel) (some? (:status @!agent-output)))
                     (if (some? (:path @!current-file))
                       ;; File open: just unfocus (panel stays visible)
                       (reset! !focus :editor)
                       ;; No file: hide panel + clear agent output
                       (do (swap! !cmd-panel assoc :visible false)
                           (reset! !agent-output nil)
                           (reset! !focus :editor)))

                     (and !sidebar-visible @!sidebar-visible)
                     (reset! !sidebar-visible false)

                     :else
                     (swap! !editor-doc assoc :selection nil))

                   :toggle-file-viewer
                   (when !sidebar-visible
                     (swap! !sidebar-visible not))

                   :focus-pane
                   (when (some? @!current-file)
                     (let [pane (:pane event)]
                       (reset! !active-pane pane)
                       (when (= pane :chat)
                         (reset! !focus :chat)
                         (reset! !caret-visible true))
                       (when (= pane :editor)
                         (reset! !focus :editor))))

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
      (cond
        !file-load-request
        (->> (m/watch !file-load-request)
             (m/eduction (filter some?))
             (m/reduce
               (fn [_ request]
                 (let [{:keys [lines target-line]} request
                       target-line (or target-line 0)]
                   (when (seq lines)
                     (let [safe-line (min target-line (max 0 (dec (count lines))))]
                       (js/console.log "[FILE-LOAD] Loading file with" (count lines) "lines, target:" safe-line)
                       (reset! !editor-doc {:lines (vec lines)
                                            :cursor {:line safe-line :col 0}
                                            :selection nil
                                            :desired-col 0})
                       ;; Scroll to target line with ~100px top margin
                       (let [font-size (:font-size @!settings)
                             line-h (* font-size (:line-height @!settings))
                             target-y (* safe-line line-h)]
                         (reset! !scroll-y (max 0 (- target-y 100))))
                       (reset! !scroll-x 0)
                       (reset! !undo-stack [])
                       (reset! !redo-stack [])
                       (reset! !folded-lines #{})
                       (reset! !caret-visible true)
                       ;; Only steal focus if command panel is not open
                       (when-not (:visible @!cmd-panel)
                         (reset! !focus :editor))
                       ;; Clear the request so same file can be re-opened
                       (reset! !file-load-request nil))))
                 nil)
               nil))
        ;; No-op task when !file-load-request not provided
        :else
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
      (m/reduce
        (fn [_ event]
          (when event
            (let [tickets (:tickets @!flow-state)
                  has-tickets? (seq tickets)
                  ;; Visual order: ticket indices in grouped display order
                  visual-order (when has-tickets?
                                 (vec (mapcat (fn [g] (mapv :idx (:tickets g)))
                                              (group-tickets-by-status tickets))))
                  n-visual (count visual-order)]
              (case (:type event)
                :enter
                (let [cmd-text (:text @!cmd-panel)]
                  (if (seq cmd-text)
                    ;; Submit command, clear text, keep panel open for follow-up
                    (do (submit-agent-run! cmd-text)
                        (swap! !cmd-panel assoc :text "" :cursor 0))
                    ;; Empty Enter with hovered ticket = toggle selection
                    (if (and has-tickets? @!hovered-row-idx)
                      (let [idx @!hovered-row-idx]
                        (swap! !flow-state update :selected
                               (fn [sel]
                                 (if (some #{idx} sel)
                                   (vec (remove #{idx} sel))
                                   (conj (vec sel) idx)))))
                      ;; No tickets or no hover = unfocus (close if no file open)
                      (do (when-not (some? (:path @!current-file))
                            (swap! !cmd-panel assoc :text "" :cursor 0 :visible false))
                          (reset! !focus :editor)))))

                ;; Up/Down: navigate ticket list in visual (grouped) order
                :up
                (when has-tickets?
                  (let [cur-idx @!hovered-row-idx
                        vis-pos (when cur-idx
                                  (some (fn [[i v]] (when (= v cur-idx) i))
                                        (map-indexed vector visual-order)))
                        new-pos (if (nil? vis-pos)
                                  (dec n-visual)
                                  (mod (dec vis-pos) n-visual))]
                    (reset! !hovered-row-idx (nth visual-order new-pos))))

                :down
                (when has-tickets?
                  (let [cur-idx @!hovered-row-idx
                        vis-pos (when cur-idx
                                  (some (fn [[i v]] (when (= v cur-idx) i))
                                        (map-indexed vector visual-order)))
                        new-pos (if (nil? vis-pos)
                                  0
                                  (mod (inc vis-pos) n-visual))]
                    (reset! !hovered-row-idx (nth visual-order new-pos))))

                ;; Edit/navigation operations (char, backspace, left, right, etc.)
                (let [panel @!cmd-panel
                      new-panel (cmd-panel-apply-event panel event @!clipboard)]
                  (reset! !cmd-panel new-panel)
                  (reset! !caret-visible true)))))
          nil)
        nil
        <cmd-keyboard)

      ;; =====================================================================
      ;; CHAT INPUT KEYBOARD EVENTS CONSUMER
      ;; =====================================================================
      (m/reduce
        (fn [_ event]
          (when event
            (case (:type event)
              :enter
              (let [cmd-text (:text @!chat-input)]
                (when (seq cmd-text)
                  (submit-agent-run! cmd-text)
                  (reset! !chat-input {:text "" :cursor 0})))

              :escape
              (do (reset! !chat-input {:text "" :cursor 0})
                  (reset! !focus :editor)
                  (reset! !active-pane :editor))

              (:up :down)
              nil ;; no-op for now (could scroll chat later)

              ;; Edit/navigation: reuse cmd-panel-apply-event pure fn
              (let [panel @!chat-input
                    new-panel (cmd-panel-apply-event panel event @!clipboard)]
                (reset! !chat-input new-panel)
                (reset! !caret-visible true))))
          nil)
        nil
        <chat-keyboard)

      ;; =====================================================================
      ;; SETTINGS PANEL KEYBOARD EVENTS CONSUMER
      ;; =====================================================================
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
        nil
        <settings-keyboard)

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
                                           !sidebar-state !sidebar-visible !extract-preview
                                           !shimmer-phase !trail-collapsed !active-pane !scroll-x !chat-scroll-y !chat-input !focus
                                           layout-x layout-y cmd-panel-h status-bar-h)
            <editor-rect-data (<editor-rects !editor-doc !eval-result !caret-visible !focus
                                             !settings !active-font !viewport
                                             <fold-data <bracket-data
                                             !flow-state !scroll-y !collapsed-groups !hovered-row-idx !drag-state
                                             !sidebar-state !sidebar-visible !current-file !extract-preview !agent-output
                                             !shimmer-phase !trail-collapsed !active-pane !scroll-x !chat-scroll-y !chat-input
                                             layout-x layout-y gutter-w)
            <cmd-rect-data (<cmd-panel-rects !cmd-panel !focus !caret-visible !scroll-y !viewport
                                             !settings !active-font
                                             !ai-provider !agent-output !sidebar-visible !sidebar-state
                                             !current-file cmd-panel-h status-bar-h)
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

