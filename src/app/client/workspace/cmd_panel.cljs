(ns app.client.workspace.cmd-panel
  "Command panel: event handling, text parsing, and rect computation."
  (:require [clojure.string :as str]
            [missionary.core :as m]
            [app.client.workspace.events :refer [maybe-snap]]
            [app.client.workspace.rect-tree :refer [rt-node]]
            [app.client.workspace.text-input :as text-input]
            [app.client.workspace.ui-primitives :refer [dt]]
            [app.client.workspace.sidebar :refer [sidebar-w]]
            [app.client.workspace.trail :refer [compute-agent-panel-h]]))

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
  "Parse command-panel text into a generic agent/runtime action.
   Workflow-specific command vocabularies are delegated by runtime."
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

      (str/starts-with? trimmed "/")
      {:kind :workflow-command :command trimmed}

      :else
      {:kind :run :provider current-provider :prompt trimmed})))

(defn <cmd-panel-rects
  "Derived flow: command panel rectangles — always 4 instances:
     [0] agent-output background  (visible when agent has status)
     [1] command-panel background (visible when panel is open)
     [2] caret                    (visible when panel focused + blink on)
     [3] status-bar background    (always visible)
   Zero-size invisible rects for absent elements keep GPU indices stable.
   SIDEBAR: backgrounds span full viewport, caret offset by sb-w."
  [!cmd-panel !focus !caret-visible !scroll-y !viewport !settings !active-font
   !ai-provider !agent-output !sidebar-visible !current-file !flow-state
   flow-canvas-active?* cmd-panel-h status-bar-h]
  (m/latest
    (fn [panel focus caret-visible scroll-y viewport settings active-font
         agent-output sidebar-visible? current-file flow-state]
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
            ;; Persistent in file-open mode and DG flow mode.
            panel-visible? (or (:visible panel)
                               (some? current-file)
                               (flow-canvas-active?* flow-state))
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
    (m/watch !current-file)
    (m/watch !flow-state)))
