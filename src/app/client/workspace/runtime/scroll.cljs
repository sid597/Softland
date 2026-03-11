(ns app.client.workspace.runtime.scroll
  "Scroll consumer: wheel routing across sidebar, agent, chat, editor, flow canvas."
  (:require [missionary.core :as m]
            [app.client.workspace.events :refer [maybe-snap]]
            [app.client.workspace.sidebar :refer [sidebar-w sidebar-tab-h cmd-panel-h status-bar-h compute-sidebar-content-height]]
            [app.client.workspace.trail :refer [compute-agent-panel-h agent-wrapped-line-count]]
            [app.client.workspace.ui-primitives :refer [list-left-pane-pct list-divider-w]]
            [app.client.workflows.dg-flow :refer [flow-canvas-active? group-tickets-by-status list-content-height]]))

(defn scroll-consumer
  "Missionary consumer: route wheel events to the appropriate scroll target."
  [{:keys [!scroll-y !scroll-x !viewport !settings !active-font !sidebar-visible
           !mouse-x !mouse-y !sidebar-state !current-file !agent-output
           !agent-scroll-y !chat-scroll-y !detail-scroll-y !flow-state !collapsed-groups]}
   >wheel-events]
  (->> >wheel-events
       (m/reduce
         (fn [_ wheel-evt]
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
                           0
                           (compute-agent-panel-h agent-output font-size
                                                  (:height viewport) (:width viewport)
                                                  char-advance))
                 agent-y0 (- (:height viewport) cmd-panel-h status-bar-h agent-h 12)
                 agent-y1 (- (:height viewport) cmd-panel-h status-bar-h)
                 mouse-y @!mouse-y
                 in-agent? (and (not in-sidebar?)
                                (pos? agent-h)
                                (>= mouse-y agent-y0)
                                (< mouse-y agent-y1))
                 flow-active? (flow-canvas-active? @!flow-state)]
             (js/console.log "[SCROLL][WHEEL] delta:" delta "mx:" mouse-x "my:" mouse-y
                             "sb?" sb-vis? "sidebar?" in-sidebar? "agent?" in-agent?
                             "flow?" flow-active?
                             "file?" (some? (:path @!current-file)))
             (cond
               ;; Sidebar file tree
               in-sidebar?
               (do (js/console.log "[SCROLL] -> sidebar")
                   (let [ss @!sidebar-state
                         content-h (compute-sidebar-content-height ss @!current-file)
                         visible-h (- (:height viewport) sidebar-tab-h)
                         max-scroll (max 0 (- content-h visible-h))]
                     (swap! !sidebar-state update :scroll-y
                            #(-> (+ (or % 0) delta) (max 0) (min max-scroll)))))

               ;; Agent panel
               in-agent?
               (do (js/console.log "[SCROLL] -> agent")
                   (let [line-step (* font-size 1.2)
                         max-chars (if (pos? char-advance)
                                     (max 1 (int (/ (- (:width viewport) 48) char-advance)))
                                     80)
                         line-count (agent-wrapped-line-count agent-output max-chars)
                         total-h (* line-count line-step)
                         max-scroll (max 0 (- total-h (- agent-h 16)))]
                     (swap! !agent-scroll-y
                            #(-> (+ % delta) (max 0) (min max-scroll)))))

               ;; Flow canvas (intake / run) — must check BEFORE chat to avoid false match
               flow-active?
               (let [flow @!flow-state
                     sb-off (if sb-vis? sidebar-w 0)
                     content-w (- (:width viewport) sb-off)
                     left-w (int (* content-w list-left-pane-pct))
                     right-x0 (+ sb-off left-w list-divider-w)]
                 (js/console.log "[SCROLL][FLOW] sb-off:" sb-off "content-w:" content-w
                                 "left-w:" left-w "right-x0:" right-x0 "mx:" mouse-x
                                 "in-right?:" (>= mouse-x right-x0))
                 (if (>= mouse-x right-x0)
                   ;; Right detail pane
                   (let [old-val @!detail-scroll-y]
                     (swap! !detail-scroll-y #(max 0 (+ % delta)))
                     (js/console.log "[SCROLL] -> detail-right"
                                     "old:" old-val "new:" @!detail-scroll-y))
                   ;; Left ticket list
                   (let [grouped (group-tickets-by-status (:tickets flow))
                         content-h (list-content-height grouped @!collapsed-groups)
                         visible-h (- (:height viewport) cmd-panel-h status-bar-h agent-h 12)
                         max-scroll (max 0 (- content-h visible-h))]
                     (js/console.log "[SCROLL] -> flow-left  content-h:" content-h
                                     "visible-h:" visible-h "max:" max-scroll)
                     (swap! !scroll-y #(-> (+ % delta) (max 0) (min max-scroll))))))

               ;; Chat pane (3-pane mode)
               (let [file-open? (some? (:path @!current-file))
                     sb-off (if sb-vis? sidebar-w 0)
                     cw (- (:width viewport) sb-off)
                     code-w (int (* cw 0.4))
                     chat-w (int (* cw 0.55))
                     rel-mx (- mouse-x sb-off)
                     in-chat? (and file-open? (>= rel-mx code-w) (< rel-mx (+ code-w chat-w)))]
                 (and file-open? in-chat?))
               (do (js/console.log "[SCROLL] -> chat")
                   (swap! !chat-scroll-y #(max 0 (+ % delta))))

               ;; Editor (no file open, or mouse in code pane)
               :else
               (do (js/console.log "[SCROLL] -> editor")
                   (when (or (not (some? (:path @!current-file)))
                             (< (- mouse-x (if sb-vis? sidebar-w 0))
                                (int (* (- (:width viewport) (if sb-vis? sidebar-w 0)) 0.4))))
                     (do (swap! !scroll-y #(maybe-snap (+ % delta) dpr snap?))
                         (let [h-delta (if shift? delta dx)
                               sb-off (if sb-vis? sidebar-w 0)
                               cw (- (:width viewport) sb-off)
                               editor-right (if (some? @!current-file)
                                              (+ sb-off (int (* cw 0.4)))
                                              (+ sb-off cw))]
                           (when (and (not (zero? h-delta)) (< mouse-x editor-right))
                             (swap! !scroll-x #(max 0 (+ (or % 0) h-delta)))))))))))
           nil)
         ))
