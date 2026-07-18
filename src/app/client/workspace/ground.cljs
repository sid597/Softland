(ns app.client.workspace.ground
  "first-light A · P2 — the bare ground (CONTRACT §3 + T9, Sid's 2026-07-17
   ruling verbatim): a blank screen · text can be added · Ctrl+Enter
   communicates with the AI. Nothing else — no command panel, no /commands,
   no sidebar, no workspace bridge, no hatch. The dev workspace stays a
   separately-launched builder scaffold (?dev=1); it is never linked from
   here.

   The ground = the worn conversation face over the GENESIS EPISODE
   (:episode address, resolved server-side to the deterministic object-key)
   + an utterance input at the tip of the trail (WALKTHROUGH laws 1/3: no
   docked input bar — the input is where the trail grows; at world-zero it
   sits alone at view height, the blank-birth caret).

   Truth discipline (the 07-12 no-optimistic-echo ruling): the typed buffer
   is CLIENT state only until Ctrl+Enter; the send lane mints the utterance
   DURABLY (imp:ep:, asserted-by sid, acked) BEFORE the agent is summoned,
   and the worn face renders it via the epoch re-pull FROM TRUTH — the
   buffer clears on the :episode-durable receipt, never optimistically. The
   agent's open turn streams as an honest live window (Law 8) and is
   REPLACED by durable distilled material on :episode-distilled.

   Client-glue module state (the face_wiring S2 shape): defonce atoms +
   an installed refs map; no server names — the send lane is the ONE
   /api/episode/utterance POST (agent/stream-agent-run!)."
  (:require [clojure.string :as str]
            [missionary.core :as m]
            [app.client.workspace.agent :as agent]
            [app.client.workspace.editor-compute :refer [editor-apply-event]]
            [app.client.workspace.face-wiring :as face-wiring]
            [app.client.workspace.rect-tree :as rt :refer [rt-node]]
            [app.client.workspace.scene-runtime :as scene-rt]
            [app.client.workspace.scene-store :as ss]))

;; ===========================================================================
;; Boot decision
;; ===========================================================================

(defn ground-boot?
  "The PRODUCT boot is the ground (T9). Builders launch the dev workspace
   explicitly with ?dev — never a link, never a keystroke, from the ground."
  []
  (not (str/includes? (str (.-search js/window.location)) "dev")))

;; ===========================================================================
;; Module state (client glue — S2)
;; ===========================================================================

(defonce ^:private !refs (atom nil))

(defonce !ground-input
  (atom {:lines [""] :cursor {:line 0 :col 0}}))

(defonce !ground-run
  ;; :phase :idle | :streaming | :distilling  ·  :activity = Law 8's one
  ;; current-activity line  ·  :stream-text = the open turn's text so far
  (atom {:phase :idle :activity nil :stream-text "" :error nil}))

(defonce ^:private !last-turn-id (atom nil))

(def ^:private ground-vi :ground-input)
(def ^:private input-x 40.0)
(def ^:private tip-gap 28.0)

;; ===========================================================================
;; The input tree (pure)
;; ===========================================================================

(defn- text-op
  ;; :y is the glyph BASELINE (renderer convention) — sit it inside the line
  ;; box [i·lh, (i+1)·lh) so the caret rect (top-anchored) brackets the text.
  [text i line-h fs [r g b a]]
  {:text text :type :text :from 0 :to (count text)
   :x 0 :y (+ (* i line-h) fs) :size fs
   :r r :g g :b b :a a})

(defn input-tree
  "The tip's rt-tree (container-LOCAL, root at 0,0): the typed buffer's lines
   + a caret rect at the cursor, and — during an open turn — the honest
   activity line + streamed text (Law 8), dimmed: a live window, never truth.
   No wrap: long lines overflow visibly (the ~70ch measure is named
   wish-fodder, WALKTHROUGH law 14). Monospace metrics only."
  [{:keys [lines cursor]} {:keys [phase activity stream-text error]}
   {:keys [font-size char-advance line-h content-w]}]
  (let [fg      [0.92 0.92 0.94 1.0]
        dim     [0.55 0.58 0.62 1.0]
        err     [0.95 0.45 0.40 1.0]
        buf-ops (vec (map-indexed
                      (fn [i line] (text-op line i line-h font-size fg))
                      lines))
        buf-h   (* (count lines) line-h)
        caret   (rt-node :ground-caret :rect
                         {:x (* (:col cursor) char-advance)
                          :y (* (:line cursor) line-h)
                          :w 2 :h line-h}
                         :style {:bg [0.95 0.95 0.95 1.0]})
        stream-lines (when (seq (str stream-text))
                       (str/split-lines (str stream-text)))
        open-turn?  (contains? #{:streaming :distilling} phase)
        below   (cond-> []
                  open-turn?
                  (conj (rt-node :ground-activity :text-run
                                 {:x 0 :y 0 :w content-w
                                  :h line-h}
                                 :text [(text-op (str "· " (or activity "the agent is working")) 0 line-h font-size dim)]))
                  (seq stream-lines)
                  (conj (rt-node :ground-stream :text-run
                                 {:x 0 :y 0 :w content-w
                                  :h (* (count stream-lines) line-h)}
                                 :text (vec (map-indexed
                                             (fn [i l] (text-op l i line-h font-size dim))
                                             stream-lines))))
                  (some? error)
                  (conj (rt-node :ground-error :text-run
                                 {:x 0 :y 0 :w content-w :h line-h}
                                 :text [(text-op (str "⟂ " error) 0 line-h font-size err)])))
        buffer  (rt-node :ground-buffer :text-run
                         {:x 0 :y 0 :w content-w :h (max line-h buf-h)}
                         :text buf-ops
                         :children (when-not open-turn? [caret]))]
    (rt-node :ground-input :stack
             {:x 0 :y 0 :w content-w :h 0}
             :layout {:direction :column :gap 10 :auto-height? true}
             :children (into [buffer] below))))

;; ===========================================================================
;; Slot lifecycle — content upserts on change; position rides the container
;; transform (set-transform!, no rebuild)
;; ===========================================================================

(defn- metrics []
  (let [{:keys [!settings !active-font !viewport]} (:atoms @!refs)
        fs (:font-size @!settings 19)
        cw (:char-width @!active-font 0.56)
        vp @!viewport]
    {:font-size fs
     :char-advance (* fs cw)
     :line-h (js/Math.round (* fs 1.4))
     :content-w (max 200 (- (:width vp 1200) input-x 32))
     :viewport-h (:height vp 800)}))

(defn- face-bottom
  "The trail tip's y: below the worn face's content when it has any, else the
   blank-birth resting height (view center-ish — WALKTHROUGH moment 0)."
  [viewport-h]
  (let [root-h (get-in (ss/slot (scene-rt/store-snapshot) :face-main)
                       [:tree :bounds :h])]
    (if (and root-h (pos? root-h))
      (+ root-h tip-gap)
      (* 0.4 viewport-h))))

(defn refresh!
  "Rebuild the tip slot from the current input + run state (edge-only — called
   from the keyboard consumer and the SSE event callbacks, never from RAF)."
  []
  (when @!refs
    (let [m    (metrics)
          tree (rt/resolve-layout
                (input-tree @!ground-input @!ground-run m))]
      (if-let [slot (ss/slot (scene-rt/store-snapshot) ground-vi)]
        (swap! scene-rt/!scene-store ss/upsert-slot ground-vi
               {:tree tree :container (:container slot)
                :meta (:meta slot) :stratum (:stratum slot)
                :pre-resolved? true})
        (scene-rt/register-face-instance! ground-vi tree
                                          {:x input-x
                                           :y (face-bottom (:viewport-h m))
                                           :scale 1.0 :layer 5
                                           :meta {:ground-input? true}
                                           :pre-resolved? true}))
      nil)))

(defn reposition!
  "Keep the tip below the growing trail — container transform only (cheap).
   Called from the render consumer edge right after build-main-face! lands a
   new face slot (the P1 edge — already ordered after the face upsert)."
  []
  (when @!refs
    (when-let [slot (ss/slot (scene-rt/store-snapshot) ground-vi)]
      (let [m (metrics)]
        (scene-rt/set-transform! (:container slot)
                                 {:x input-x
                                  :y (face-bottom (:viewport-h m))
                                  :scale 1.0})))))

;; ===========================================================================
;; The send lane — Ctrl+Enter → /api/episode/utterance (SSE)
;; ===========================================================================

(defn- run-event!
  "Fold ONE SSE event into the run state. The buffer clears on the DURABLE
   receipt (never optimistically); truth replaces the stream window when the
   distill receipt closes the turn (the epoch re-pull renders it)."
  [turn-id evt]
  (case (:kind evt)
    :episode-durable
    (do (reset! !ground-input {:lines [""] :cursor {:line 0 :col 0}})
        (reset! !last-turn-id turn-id)
        (swap! !ground-run assoc :phase :streaming :activity "the agent is reading" :error nil))

    :text-delta
    (swap! !ground-run
           (fn [r] (-> r
                       (assoc :activity nil)
                       (update :stream-text str (:text evt)))))

    :thinking-delta
    (swap! !ground-run assoc :activity "thinking")

    :tool-use-start
    (swap! !ground-run assoc :activity (str "using " (:tool-name evt)))

    :run-done
    (swap! !ground-run assoc :phase :distilling :activity "landing the turn")

    :run-error
    (swap! !ground-run assoc
           :phase :idle
           :activity nil
           :error (str (or (:error evt) :run-error)
                       (when (:detail evt) (str " " (pr-str (:detail evt))))))

    :episode-distilled
    ;; the turn is durable truth now; the epoch re-pull renders it — drop the
    ;; live window whole (nothing erased: the material is in the land)
    (reset! !ground-run {:phase :idle :activity nil :stream-text "" :error nil})

    nil)
  (refresh!))

(defn submit!
  "Ctrl+Enter. Mints turn-id + time-ms ONCE (client-side — the wear-id
   precedent) so a retry of this turn converges server-side (imp:ep:
   idempotence). One turn in flight at a time — the land has one resident."
  []
  (let [text (str/join "\n" (:lines @!ground-input))]
    (when (and (not (str/blank? text))
               (= :idle (:phase @!ground-run)))
      (let [turn-id (str (random-uuid))
            body {:text text
                  :turn-id turn-id
                  :time-ms (js/Date.now)
                  :prev-turn-id @!last-turn-id}]
        (swap! !ground-run assoc :phase :streaming
               :activity "reaching the land" :stream-text "" :error nil)
        (refresh!)
        (agent/stream-agent-run!
         "/api/episode/utterance" body
         (partial run-event! turn-id)
         (fn [err]
           (swap! !ground-run assoc :phase :idle :activity nil
                  :error (str "send failed: " (.-message err)))
           (refresh!)))))))

;; ===========================================================================
;; Keys — the :ground-input focus lane
;; ===========================================================================

(defn ground-keys-consumer
  "The ground's key consumer (the editor-keys-consumer shape): buffer edits
   ride editor-apply-event verbatim (multi-line — Enter is a NEWLINE; only
   Ctrl+Enter speaks to the AI, Sid's ruling); :eval sends."
  [atoms <ground-keyboard]
  (->> <ground-keyboard
       (m/reduce
        (fn [_ event]
          (when event
            (let [doc @!ground-input
                  lengths (mapv count (:lines doc))]
              (case (:type event)
                (:char :backspace :delete :enter :paste
                 :left :right :up :down :home :end :word-left :word-right)
                (do (reset! !ground-input
                            (select-keys (editor-apply-event doc event lengths
                                                             @(:!clipboard atoms))
                                         [:lines :cursor]))
                    (refresh!))

                :eval (submit!)
                nil)))
          nil)
        nil)))

;; ===========================================================================
;; Install (the boot seam)
;; ===========================================================================

(defn install-ground!
  "Wear the conversation face over the genesis episode (the /face path driven
   directly — the block_edit_probe precedent) and mount the tip. Focus lands
   on the ground input; the face-assembly mode's R1 guard suppresses the
   sidebar by construction."
  [atoms]
  (reset! !refs {:atoms atoms})
  ;; T9 belt: the command panel boots :visible in the dev workspace — the
  ;; ground closes it before the first frame (the chrome flows also gate on
  ;; ground-active?, so this is belt over suspenders).
  (when-let [!p (:!cmd-panel atoms)] (swap! !p assoc :visible false))
  (reset! (:!face-state atoms)
          {:face :outline-face :address :episode :params {:limit 64}})
  (face-wiring/wear-face! atoms :outline-face :episode)
  (reset! (:!focus atoms) :ground-input)
  (refresh!)
  nil)

(defn ground-active?
  "True when this session booted as the ground (refs installed)."
  []
  (some? @!refs))

(defn focus-ground!
  "Return the caret to the tip (Escape / background click in ground mode)."
  []
  (when-let [atoms (:atoms @!refs)]
    (reset! (:!focus atoms) :ground-input)
    (refresh!)))
