(ns app.client.workspace.runtime.scroll
  "Scroll consumer: wheel routing across sidebar, agent, chat, editor, flow canvas."
  (:require [missionary.core :as m]
            [app.client.workspace.events :refer [maybe-snap]]
            [app.client.workspace.ground :as ground]
            [app.client.workspace.runtime.workspace-actions :as ws]
            [app.client.workspace.sidebar :refer [sidebar-w sidebar-tab-h cmd-panel-h status-bar-h compute-sidebar-content-height derive-effective-sidebar]]
            [app.client.workspace.trail :refer [compute-agent-panel-h agent-wrapped-line-count]]
            [app.client.workspace.ui-primitives :refer [list-left-pane-pct list-divider-w]]
            [app.client.workspace.trail-face.scene :as trail-scene]
            [app.client.workflows.dg-flow :refer [group-tickets-by-status list-content-height]]))

(defn scroll-consumer
  "Missionary consumer: route wheel events to the appropriate scroll target."
  [{:keys [!scroll-y !scroll-x !viewport !settings !active-font !sidebar-visible
           !mouse-x !mouse-y !sidebar-truth !sidebar-overlay !sidebar-ui !effective-local-world !agent-output
           !agent-scroll-y !chat-scroll-y !detail-scroll-y !flow-state !collapsed-groups !editor-doc
           !trail-face-scene !face-scene !face-list]}
   >wheel-events]
  (->> >wheel-events
       (m/reduce
         (fn [_ wheel-evt]
           (if (ground/ground-active?)
             ;; first-light P2b: on the open ground the wheel ZOOMS at the
             ;; pointer (§9.4 — the world point under the pointer stays under
             ;; it); there is no scroll rail to ride (Law 2: no top-left).
             (ground/handle-wheel! wheel-evt)
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
                 local-world @!effective-local-world
                 file-workspace? (ws/local-world-file-workspace? local-world)
                 flow-active? (ws/local-world-flow? local-world)
                 trail-face-active? (ws/local-world-trail-face? local-world)
                 face-assembly-active? (ws/local-world-face-assembly? local-world)
                 ;; R-1 debt 1, the mouse.cljs rule applied to the wheel:
                 ;; wheel space must match render space — the full-screen
                 ;; faces force-hide the sidebar, so the wheel must not
                 ;; route to it (framework W1-INT).
                 in-sidebar? (and sb-vis? (< mouse-x sidebar-w)
                                  (not trail-face-active?)
                                  (not face-assembly-active?))
                 agent-output @!agent-output
                 ;; leftover agent output must not steal the wheel under the
                 ;; assembly-hosted face (G16 falsification fix)
                 agent-h (if (or file-workspace? face-assembly-active?)
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
                                (< mouse-y agent-y1))]
             (cond
               ;; Sidebar file tree
               in-sidebar?
               ;; W2: the clamp must see the FACES section too (its height is
               ;; part of what build-sidebar-tree renders)
               (let [ss (derive-effective-sidebar @!sidebar-truth @!sidebar-overlay @!sidebar-ui
                                                  (some-> !face-list deref))
                     content-h (compute-sidebar-content-height ss)
                     ;; G26 fix: build-sidebar-tree renders NO tab bar ("Content
                     ;; fills full height"); subtracting sidebar-tab-h clamped
                     ;; the bottom ~36px out of reach
                     visible-h (:height viewport)
                     max-scroll (max 0 (- content-h visible-h))]
                 (swap! !sidebar-ui update :scroll-y
                        #(-> (+ (or % 0) delta) (max 0) (min max-scroll))))

               ;; Agent panel
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

               ;; Flow canvas (intake / run) — must check BEFORE chat to avoid false match
               flow-active?
               (let [flow @!flow-state
                     sb-off (if sb-vis? sidebar-w 0)
                     content-w (- (:width viewport) sb-off)
                     left-w (int (* content-w list-left-pane-pct))
                     right-x0 (+ sb-off left-w list-divider-w)]
                 (if (>= mouse-x right-x0)
                   ;; Right detail pane
                   (swap! !detail-scroll-y #(max 0 (+ % delta)))
                   ;; Left ticket list
                   (let [grouped (group-tickets-by-status (:tickets flow))
                         content-h (list-content-height grouped @!collapsed-groups)
                         visible-h (- (:height viewport) cmd-panel-h status-bar-h agent-h 12)
                         max-scroll (max 0 (- content-h visible-h))]
                     (swap! !scroll-y #(-> (+ % delta) (max 0) (min max-scroll))))))

               ;; Trail face (view-mvp WP-B2) — full-screen mode; MUST sit
               ;; before the chat/editor clauses: the editor :else fires on
               ;; (not file-workspace?), which is true in a trail mode and
               ;; would silently steal this wheel (trap 9). Reuses !scroll-y
               ;; (camera pan-y = -scroll-y); clamp on THIS wheel event
               ;; against the cached scene's content height (RETRO s3.8).
               trail-face-active?
               (let [scene @!trail-face-scene
                     content-h (if scene (trail-scene/content-height scene) 0)
                     visible-h (- (:height viewport) cmd-panel-h status-bar-h)]
                 (swap! !scroll-y
                        #(trail-scene/clamp-scroll (+ % delta) content-h visible-h)))

               ;; Assembly-hosted face (framework W1-INT) — the trail-face
               ;; shape: full-screen, reuses !scroll-y (camera pan-y =
               ;; -scroll-y); clamp on THIS wheel event against the content
               ;; height the interpreter DECLARED in the scene root :data
               ;; (:assembly/content-h, §10 SLOT-C — the scene stays
               ;; scroll-independent; scroll-y is never baked into the tree).
               face-assembly-active?
               (let [scene @!face-scene
                     content-h (get-in scene [:data :assembly/content-h] 0)
                     visible-h (- (:height viewport) cmd-panel-h status-bar-h)]
                 (swap! !scroll-y
                        #(trail-scene/clamp-scroll (+ % delta) content-h visible-h)))

               ;; Chat pane (3-pane mode)
               (let [sb-off (if sb-vis? sidebar-w 0)
                     cw (- (:width viewport) sb-off)
                     code-w (int (* cw (ws/pane-width-pct local-world :main 0.4)))
                     chat-w (int (* cw (ws/pane-width-pct local-world :right 0.55)))
                     rel-mx (- mouse-x sb-off)
                     in-chat? (and file-workspace? (>= rel-mx code-w) (< rel-mx (+ code-w chat-w)))]
                 (and file-workspace? in-chat?))
               (swap! !chat-scroll-y #(max 0 (+ % delta)))

               ;; Editor (no file open, or mouse in code pane)
               :else
               (when (or (not file-workspace?)
                         (< (- mouse-x (if sb-vis? sidebar-w 0))
                            (int (* (- (:width viewport) (if sb-vis? sidebar-w 0))
                                    (ws/pane-width-pct local-world :main 0.4)))))
                 (let [doc @!editor-doc
                       line-h (* font-size (:line-height settings))
                       total-lines (count (:lines doc))
                       chrome-h (+ cmd-panel-h status-bar-h)
                       overscroll (* 10 line-h)
                       visible-h (- (:height viewport) chrome-h)
                       max-scroll (max 0 (- (+ (* total-lines line-h) overscroll) visible-h))]
                   (swap! !scroll-y #(-> (+ % delta) (max 0) (min max-scroll) (maybe-snap dpr snap?))))
                 (let [h-delta (if shift? delta dx)
                       sb-off (if sb-vis? sidebar-w 0)
                       cw (- (:width viewport) sb-off)
                       editor-right (if file-workspace?
                                      (+ sb-off (int (* cw (ws/pane-width-pct local-world :main 0.4))))
                                      (+ sb-off cw))]
                   (when (and (not (zero? h-delta)) (< mouse-x editor-right))
                     (swap! !scroll-x #(max 0 (+ (or % 0) h-delta)))))))))
           nil)
         )))
