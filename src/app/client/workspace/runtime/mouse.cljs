(ns app.client.workspace.runtime.mouse
  "Mouse consumer: the ground owns the WHOLE pointer grammar (first-light
   P2b §9.4 — one ~4 CSS px threshold splits click/caret from pan/drag) and
   NOTHING falls through. Paste routes to the ground's content-act lane. The
   dev workspace's sidebar/flow/editor/chat click handlers died with that
   surface (dead-path census, Sid's ruling 2026-08-15)."
  (:require [missionary.core :as m]
            [app.client.workspace.ground :as ground]))

;; WINDOW listeners survive a runtime remount (hot reload, Electric
;; reconnect) — without removal they STACK, and a paste inserts once per
;; stacked handler (the ×N genus, paste-side). One cell per listener kind:
;; installing swaps the window's handler instead of adding a sibling.
(defonce ^:private !window-handlers (atom {}))
(defonce ^:private !paste-intercept (atom nil))

(defn install-paste-intercept!
  "Install T2's nil-default paste intercept. True means the session handled
   the event and the ground router must remain untouched."
  [intercept]
  (reset! !paste-intercept intercept)
  true)

(defn- swap-window-listener!
  [event-name handler]
  (when-let [old (get @!window-handlers event-name)]
    (.removeEventListener js/window event-name old))
  (swap! !window-handlers assoc event-name handler)
  (.addEventListener js/window event-name handler))

(defn install-paste-handler!
  "Wire system clipboard paste to the ground's content-act lane.
   first-light P2b: paste on the ground births a block at the anchor/pointer
   or edits the focused block through the committed lane."
  [{:keys [!focus]}]
  (let [paste-handler
        (fn [e]
          (when-not (and @!paste-intercept (@!paste-intercept e))
            (let [text (.getData (.-clipboardData e) "text/plain")]
              (when (seq text)
                (.preventDefault e)
                (when (= @!focus :ground-input)
                  (ground/handle-paste! text))))))]
    (swap-window-listener! "paste" paste-handler)))

(defn mouse-consumer
  "Missionary consumer: route mouse events to the ground's pointer machine."
  [atoms _layout _deps _io >mouse-events]
  (->> >mouse-events
       (m/reduce
         (fn [_ [type coords]]
           (when (ground/ground-active?)
             (case type
               :mousedown (ground/pointer-down! (:x coords) (:y coords)
                                                (:shift? coords))
               :mousemove (do (reset! (:!mouse-x atoms) (:x coords))
                              (reset! (:!mouse-y atoms) (:y coords))
                              (ground/pointer-move! (:x coords) (:y coords)))
               :mouseup   (ground/pointer-up! (:x coords) (:y coords))
               nil))
           nil)
         nil)))
