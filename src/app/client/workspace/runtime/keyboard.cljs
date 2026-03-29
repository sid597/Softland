(ns app.client.workspace.runtime.keyboard
  "Keyboard consumers: global, editor, command panel, chat, settings, file-load."
  (:require [clojure.string :as str]
            [missionary.core :as m]
            [app.client.workspace.events :refer [maybe-snap]]
            [app.client.workspace.sidebar :refer [cmd-panel-h status-bar-h]]
            [app.client.workspace.text-input :as text-input]
            [app.client.workspace.themes :as themes]
            [app.client.workspace.cmd-panel :refer [cmd-panel-apply-event]]
            [app.client.workspace.editor-compute :refer [editor-apply-event]]
            [app.client.workspace.settings-view :refer [slider-specs font-defaults->settings]]
            [app.client.workspace.runtime.state :refer [save-undo!]]
            [app.client.workspace.runtime.workspace-actions :as ws]
            [app.client.workflows.dg-flow :refer [flow-canvas-active? group-tickets-by-status
                                                   set-selection]]))

;; ─────────────────────────────────────────────────
;; GLOBAL KEYS
;; ─────────────────────────────────────────────────

(defn global-keys-consumer
  [{:keys [!cmd-panel !settings !focus !caret-visible !editor-doc !sidebar-visible
           !current-file !agent-output !active-pane !chat-input]
    :as atoms}
   <global-keys]
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
                       (swap! !settings assoc :visible false)
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
                 (do (swap! !settings assoc :visible false) (reset! !focus :editor))

                 (= @!focus :chat)
                 (do (reset! !chat-input {:text "" :cursor 0}) (reset! !focus :editor))

                 (or (:visible @!cmd-panel) (some? (:status @!agent-output)))
                 (if (some? (:path @!current-file))
                   (reset! !focus :editor)
                   (do (swap! !cmd-panel assoc :visible false)
                       (reset! !agent-output nil)
                       (reset! !focus :editor)))

                 (and !sidebar-visible @!sidebar-visible)
                 (ws/hide-sidebar! atoms)

                 :else
                 (swap! !editor-doc assoc :selection nil))

               :toggle-file-viewer
               (ws/toggle-sidebar! atoms)

               :focus-pane
               (when (some? @!current-file)
                 (ws/set-active-pane! atoms (:pane event)))

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
         nil)))

;; ─────────────────────────────────────────────────
;; FILE LOAD
;; ─────────────────────────────────────────────────

(defn file-load-consumer
  [{:keys [!file-load-request !editor-doc !scroll-y !scroll-x !undo-stack !redo-stack
           !folded-lines !caret-visible !focus !cmd-panel !settings]}]
  (if !file-load-request
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
                   (let [font-size (:font-size @!settings)
                         line-h (* font-size (:line-height @!settings))
                         target-y (* safe-line line-h)]
                     (reset! !scroll-y (max 0 (- target-y 100))))
                   (reset! !scroll-x 0)
                   (reset! !undo-stack [])
                   (reset! !redo-stack [])
                   (reset! !folded-lines #{})
                   (reset! !caret-visible true)
                   (when-not (:visible @!cmd-panel) (reset! !focus :editor))
                   (reset! !file-load-request nil))))
             nil)
           nil))
    (m/reduce (fn [_ _] nil) nil (m/seed [nil]))))

;; ─────────────────────────────────────────────────
;; EDITOR KEYS
;; ─────────────────────────────────────────────────

(defn editor-keys-consumer
  [{:keys [!editor-doc !caret-visible !clipboard !undo-stack !redo-stack
           !eval-result !scroll-y !viewport !settings !active-font]}
   {:keys [layout-y]}
   {:keys [find-form-fn eval-form-fn]}
   <editor-keyboard]
  (->> <editor-keyboard
       (m/reduce
         (fn [_ event]
           (when event
             (let [doc @!editor-doc
                   lengths (mapv count (:lines doc))]
               (case (:type event)
                 (:char :backspace :delete :enter :paste)
                 (let [_ (save-undo! {:!undo-stack !undo-stack :!redo-stack !redo-stack}
                                     (:lines doc) (:cursor doc))
                       new-doc (editor-apply-event doc event lengths @!clipboard)]
                   (reset! !editor-doc new-doc)
                   (reset! !caret-visible true))

                 (:left :right :up :down :home :end :word-left :word-right)
                 (let [new-doc (editor-apply-event doc event lengths nil)
                       font-size (:font-size @!settings)
                       dpr (:dpr @!viewport)
                       snap? (:snap-to-pixel? @!settings)
                       line-h (maybe-snap (* font-size (:line-height @!settings)) dpr snap?)
                       ly (maybe-snap layout-y dpr snap?)
                       caret-y (+ ly (* (:line (:cursor new-doc)) line-h))
                       scroll-y @!scroll-y
                       viewport @!viewport
                       chrome-h (+ cmd-panel-h status-bar-h)
                       visible-h (- (:height viewport) chrome-h)
                       viewport-bottom (+ scroll-y visible-h)
                       padding line-h
                       new-scroll (cond
                                    (< caret-y (+ scroll-y padding))
                                    (max 0 (- caret-y padding))
                                    (> (+ caret-y line-h) (- viewport-bottom padding))
                                    (+ (- caret-y visible-h) line-h padding)
                                    :else scroll-y)
                       new-scroll (maybe-snap new-scroll dpr snap?)]
                   (reset! !editor-doc new-doc)
                   (reset! !scroll-y new-scroll)
                   (reset! !caret-visible true))

                 :copy
                 (let [input {:lines (:lines doc) :cursor (:cursor doc) :selection (:selection doc)}
                       text (text-input/copy input true)]
                   (when text
                     (reset! !clipboard text)
                     (js/console.log "Copied:" text)))

                 :cut
                 (let [input {:lines (:lines doc) :cursor (:cursor doc) :selection (:selection doc)}
                       result (text-input/cut input true)]
                   (when (:text result)
                     (save-undo! {:!undo-stack !undo-stack :!redo-stack !redo-stack}
                                 (:lines doc) (:cursor doc))
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
         nil)))

;; ─────────────────────────────────────────────────
;; COMMAND PANEL KEYS
;; ─────────────────────────────────────────────────

(defn command-keys-consumer
  [{:keys [!cmd-panel !flow-state !hovered-row-idx !focus !caret-visible
           !clipboard !current-file]}
   submit-agent-run!
   <cmd-keyboard]
  (m/reduce
    (fn [_ event]
      (when event
        (let [tickets (:tickets @!flow-state)
              has-tickets? (seq tickets)
              visual-order (when has-tickets?
                             (vec (mapcat (fn [g] (mapv :idx (:tickets g)))
                                          (group-tickets-by-status tickets))))
              n-visual (count visual-order)]
          (case (:type event)
            :enter
            (let [cmd-text (:text @!cmd-panel)]
              (if (seq cmd-text)
                (do (submit-agent-run! cmd-text)
                    (swap! !cmd-panel assoc :text "" :cursor 0))
                (if (and has-tickets? @!hovered-row-idx)
                  (let [idx @!hovered-row-idx
                        sel (:selected @!flow-state)
                        new-sel (if (some #{idx} sel)
                                  (vec (remove #{idx} sel))
                                  (conj (vec sel) idx))]
                    (swap! !flow-state set-selection new-sel))
                  (do (when-not (some? (:path @!current-file))
                        (swap! !cmd-panel assoc :text "" :cursor 0 :visible false))
                      (reset! !focus :editor)))))

            :up
            (when has-tickets?
              (let [cur-idx @!hovered-row-idx
                    vis-pos (when cur-idx
                              (some (fn [[i v]] (when (= v cur-idx) i))
                                    (map-indexed vector visual-order)))
                    new-pos (if (nil? vis-pos) (dec n-visual) (mod (dec vis-pos) n-visual))]
                (reset! !hovered-row-idx (nth visual-order new-pos))))

            :down
            (when has-tickets?
              (let [cur-idx @!hovered-row-idx
                    vis-pos (when cur-idx
                              (some (fn [[i v]] (when (= v cur-idx) i))
                                    (map-indexed vector visual-order)))
                    new-pos (if (nil? vis-pos) 0 (mod (inc vis-pos) n-visual))]
                (reset! !hovered-row-idx (nth visual-order new-pos))))

            (let [panel @!cmd-panel
                  new-panel (cmd-panel-apply-event panel event @!clipboard)]
              (reset! !cmd-panel new-panel)
              (reset! !caret-visible true)))))
      nil)
    nil
    <cmd-keyboard))

;; ─────────────────────────────────────────────────
;; CHAT KEYS
;; ─────────────────────────────────────────────────

(defn chat-keys-consumer
  [{:keys [!chat-input !focus !active-pane !caret-visible !clipboard]
    :as atoms}
   submit-agent-run!
   <chat-keyboard]
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
              (ws/set-active-pane! atoms :editor))

          (:up :down) nil

          (let [panel @!chat-input
                new-panel (cmd-panel-apply-event panel event @!clipboard)]
            (reset! !chat-input new-panel)
            (reset! !caret-visible true))))
      nil)
    nil
    <chat-keyboard))

;; ─────────────────────────────────────────────────
;; SETTINGS KEYS
;; ─────────────────────────────────────────────────

(defn settings-keys-consumer
  [{:keys [!settings !focus !font-manifest !active-font]}
   <settings-keyboard]
  (m/reduce
    (fn [_ event]
      (when event
        (let [settings @!settings
              fonts (or (:fonts @!font-manifest)
                        [{:name "DejaVu Sans Mono" :id "dejavu-sans-mono" :charWidth 0.60}])
              available-fonts (filterv #(not (false? (:available %))) fonts)
              font-count (count available-fonts)
              focus-section (:focus-section settings)
              slider-index (:slider-index settings)
              sliders (slider-specs settings)
              slider-count (count sliders)]
          (case (:type event)
            :char
            (if (= (:char event) "Tab")
              (swap! !settings update :focus-section (fn [s] (if (= s :fonts) :sliders :fonts)))
              nil)

            :up
            (if (= focus-section :fonts)
              (let [new-idx (max 0 (dec (:selected-index settings)))
                    selected-font (nth available-fonts new-idx nil)]
                (swap! !settings assoc :selected-index new-idx)
                (when selected-font
                  (reset! !active-font {:id (:id selected-font)
                                        :char-width (or (:charWidth selected-font) 0.56)
                                        :name (:name selected-font)})))
              (swap! !settings update :slider-index #(max 0 (dec %))))

            :down
            (if (= focus-section :fonts)
              (let [new-idx (min (dec font-count) (inc (:selected-index settings)))
                    selected-font (nth available-fonts new-idx nil)]
                (swap! !settings assoc :selected-index new-idx)
                (when selected-font
                  (reset! !active-font {:id (:id selected-font)
                                        :char-width (or (:charWidth selected-font) 0.56)
                                        :name (:name selected-font)})))
              (swap! !settings update :slider-index #(min (dec slider-count) (inc %))))

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
                  :px-range    (swap! !settings update :px-range #(min 32 (inc %)))
                  :sharpness   (swap! !settings update :sharpness #(min 0.2 (+ % 0.02)))
                  :snap-to-pixel? (swap! !settings assoc :snap-to-pixel? true)
                  :show-diagnostics? (swap! !settings assoc :show-diagnostics? true))))

            :enter (do (swap! !settings assoc :visible false) (reset! !focus :editor))
            :escape (do (swap! !settings assoc :visible false) (reset! !focus :editor))
            nil)))
      nil)
    nil
    <settings-keyboard))
