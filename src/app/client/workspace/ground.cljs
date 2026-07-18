(ns app.client.workspace.ground
  "first-light A · P2b — the OPEN ground (CONTRACT §3.1 + §7 P2b, WALKTHROUGH
   moments 0–6). Arrival renders NOTHING: zero content pixels, zero caret, no
   hidden focused input — the substrate never chooses the first position; the
   symmetry is broken by the inhabitant, literally, with a click (Law 1).

   The P2 tip pattern (pre-placed blank-birth caret + the client-only typed
   buffer) DIED in this diff — deleted, never gated (the swap rule).

   What lives here (client glue, S2 shape — the pure machine is
   ground_edit.cljc):
   - the world camera (pan/zoom; shader terms: screen = world·zoom + pan) —
     wheel zooms at the pointer, drag on empty ground pans (Laws 2/3/9);
   - per-block scene slots in their OWN containers (the container-transform
     substrate): click = caret anchor on ground / edit caret on a block;
     press-drag past ~4 CSS px moves a block or pans (§9.4 grammar);
   - birth at the FIRST content act (POST /api/episode/block-birth — unit +
     birth-position in ONE acked import; Escape before content leaves
     NOTHING);
   - committed-echo typing: every keystroke rides the block-write
     :object/edit artery; glyphs + caret render from the ONE confirmed value
     (ground_edit's invisible intent queue — receipt P2B.md §b);
   - Ctrl+Enter = revision-pinned send from the FOCUSED block on the
     existing durable-BEFORE-agent lane; busy resident → visible transient
     refusal AT the block, no queue; the open turn renders an explicitly
     PROVISIONAL projection (process-state, never truth — Law 8) REPLACED at
     distill by the durable provenance-marked reply (Law 6);
   - camera + positions persist as SETTLE-STATE truth only: gesture-end ARMS
     the settle write, ~400ms debounce coalesces bursts, exit flush is a
     best-effort belt — SAFETY IS THE ACKNOWLEDGED SETTLE WRITE
     (/api/episode/geometry; settled cells, P2B.md receipt a). Return
     restores camera + positions, never focus/caret/hover/selection."
  (:require [clojure.string :as str]
            [cljs.reader :as reader]
            [missionary.core :as m]
            [app.client.workspace.agent :as agent]
            [app.client.workspace.block-edit-wiring :as bew]
            [app.client.workspace.face-wiring :as face-wiring]
            [app.client.workspace.ground-edit :as ge]
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

(defn- drill-conversation-id
  "The G4b drill seam's client half (§11: machinery drills run on their OWN
   episode; the genesis stays virgin). ?drill=<conv-id> threads through the
   face pull + all three write lanes; absent = the genesis episode."
  []
  (second (re-find #"[?&]drill=([^&]+)" (str (.-search js/window.location)))))

;; ===========================================================================
;; Module state
;; ===========================================================================

(defonce ^:private !refs (atom nil))

(defonce !camera
  ;; shader terms (renderer.cljs vertex): screen = world·zoom + pan.
  (atom {:x 0.0 :y 0.0 :zoom 1.0}))

(defonce !ground-edit (atom nil))   ; ge/init at install (fresh client-id)

(defonce ^:private !world
  ;; :blocks {unit-id {:vi :cid :x :y :w :h :machine? :local? :settled {:x :y}}}
  ;; :context — last served face-context · :camera-restored? — boot-once flag
  (atom {:blocks {} :context nil :camera-restored? false}))

(defonce !ground-run
  ;; Law 8: one current-activity line + the arriving stream — explicitly
  ;; PROVISIONAL (process-state, never truth). :source-unit-id anchors the
  ;; projection beneath the spoken block.
  (atom {:phase :idle :activity nil :stream-text "" :error nil
         :source-unit-id nil :turn-id nil :await-reply nil}))

(defonce ^:private !last-turn-id (atom nil))

(defonce ^:private !pointer (atom {:phase :idle}))
(defonce ^:private !hover (atom nil))

(defonce ^:private !settle
  ;; :dirty {unit-id {:x :y}} :camera? bool :timer id
  ;; :acked {unit-id {:x :y}} — the revert base for a refused write (G4b)
  (atom {:dirty {} :camera? false :timer nil :acked {}}))

(defonce ^:private !notice (atom nil))  ; {:unit-id :text} transient (busy etc.)

(def ^:private drag-threshold-px 4.0)
(def ^:private block-pad 8.0)
(def ^:private reply-gap 34.0)
(def ^:private settle-debounce-ms 400)

(declare rebuild-block! reconcile! refresh-provisional!)

;; ===========================================================================
;; Camera math (screen = world·zoom + pan)
;; ===========================================================================

(defn- screen->world [sx sy]
  (let [{:keys [x y zoom]} @!camera]
    [(/ (- sx x) zoom) (/ (- sy y) zoom)]))

;; ===========================================================================
;; Metrics + block trees
;; ===========================================================================

(defn- metrics []
  (let [{:keys [!settings !active-font !viewport]} (:atoms @!refs)
        fs (:font-size @!settings 19)
        cw (:char-width @!active-font 0.56)]
    {:font-size fs
     :char-advance (* fs cw)
     :line-h (js/Math.round (* fs 1.4))
     :viewport @!viewport}))

(defn- text-op
  ;; :y is the glyph BASELINE (renderer convention).
  [text i line-h fs [r g b a] x]
  {:text text :type :text :from 0 :to (count text)
   :x x :y (+ (* i line-h) fs) :size fs
   :r r :g g :b b :a a})

(def ^:private fg [0.92 0.92 0.94 1.0])
(def ^:private dim [0.55 0.58 0.62 1.0])
(def ^:private err-col [0.95 0.45 0.40 1.0])
(def ^:private amber [0.92 0.75 0.35 1.0])
(def ^:private machine-tint [0.62 0.66 0.76 0.6])
(def ^:private attention-border [0.45 0.52 0.66 0.55])

(defn- block-tree
  "One block's container-LOCAL resolved tree (root at 0,0; the container
   transform places it in the world). No wrap, no clip — width grows with
   the longest line (Law/WALKTHROUGH 14). Root carries [:data :address] so
   picks resolve to the unit (T7). At rest the land reads as material: the
   interaction box shows only on attention (Law 10); machine provenance is a
   quiet persistent edge tint (Law 6) — two separate primitives."
  [unit-id {:keys [text caret focused? refusal]} machine? hover? notice
   {:keys [font-size char-advance line-h]}]
  (let [lines   (str/split (or text "") #"\n" -1)
        n       (count lines)
        max-len (reduce max 1 (map count lines))
        w       (+ (* max-len char-advance) (* 2 block-pad))
        h       (+ (* n line-h) (* 2 block-pad))
        ops     (vec (map-indexed
                      (fn [i l] (text-op l i line-h font-size
                                         (if machine? dim fg) 0))
                      lines))
        caret-lc (when (and focused? caret)
                   (ge/caret->line-col text caret))
        extra   (cond-> []
                  ;; provenance mark (Law 6): quiet persistent edge tint,
                  ;; machine stratum only — never an attention effect
                  machine?
                  (conj (rt-node :ground-mark :rect
                                 {:x (- block-pad) :y (- block-pad) :w 2.5 :h h}
                                 :style {:bg machine-tint}))
                  ;; interaction box (Law 10): attention only
                  (or focused? hover?)
                  (conj (rt-node :ground-box :rect
                                 {:x (- block-pad) :y (- block-pad) :w w :h h}
                                 :style {:border-width 1.0
                                         :border-color attention-border
                                         :bg [0.0 0.0 0.0 0.0]}))
                  (and focused? caret-lc)
                  (conj (rt-node :ground-caret :rect
                                 {:x (* (:col caret-lc) char-advance)
                                  :y (* (:line caret-lc) line-h)
                                  :w 2 :h line-h}
                                 :style {:bg [0.95 0.95 0.95 1.0]}))
                  (some? refusal)
                  (conj (rt-node :ground-refusal :text-run
                                 {:x 0 :y (* n line-h) :w w :h line-h}
                                 :text [(text-op (str "⟂ edit refused: "
                                                      (if (keyword? refusal)
                                                        (name refusal) (str refusal)))
                                                 0 line-h font-size err-col 0)]))
                  (some? notice)
                  (conj (rt-node :ground-notice :text-run
                                 {:x 0 :y (* (+ n (if refusal 1 0)) line-h)
                                  :w w :h line-h}
                                 :text [(text-op (str notice) 0 line-h font-size
                                                 amber 0)])))]
    (rt/resolve-layout
     (rt-node :ground-block :text-run
              {:x 0 :y 0 :w (max w char-advance) :h (max h line-h)}
              :text ops
              :data {:address unit-id}
              :children extra))))

;; ===========================================================================
;; Slot lifecycle
;; ===========================================================================

(defn- block-vi [unit-id] [:vi :ground-block unit-id])

(defn- upsert-block-slot! [unit-id tree x y]
  (if-let [slot (ss/slot (scene-rt/store-snapshot) (block-vi unit-id))]
    (swap! scene-rt/!scene-store ss/upsert-slot (block-vi unit-id)
           {:tree tree :container (:container slot)
            :meta (:meta slot) :stratum (:stratum slot)
            :pre-resolved? true})
    (let [{:keys [container]}
          (scene-rt/register-face-instance!
           (block-vi unit-id) tree
           {:x x :y y :scale 1.0 :layer 2
            :meta {:ground-block unit-id}
            :pre-resolved? true})]
      (swap! !world assoc-in [:blocks unit-id :cid] container)))
  nil)

(defn- truth-text
  "Materialized truth for a block: the narrowing overlay when present, else
   the served context text. NEVER a queue value."
  [unit-id]
  (or (get @bew/!truth-overlay unit-id)
      (some (fn [t] (some #(when (= unit-id (:id %)) (:text %)) (:blocks t)))
            (:turns (:context @!world)))))

(defn rebuild-block!
  "Rebuild ONE block slot from the current edit state + truth (the
   keystroke-echo hot path — one small tree, same-frame paint)."
  [unit-id]
  (when-let [b (get-in @!world [:blocks unit-id])]
    (let [st   @!ground-edit
          view (ge/block-view st unit-id (truth-text unit-id))
          n    @!notice
          tree (block-tree unit-id view (:machine? b)
                           (= unit-id @!hover)
                           (when (= unit-id (:unit-id n)) (:text n))
                           (metrics))]
      (upsert-block-slot! unit-id tree (:x b) (:y b))
      (swap! !world update-in [:blocks unit-id]
             assoc :w (get-in tree [:bounds :w]) :h (get-in tree [:bounds :h])))))

;; ===========================================================================
;; Anchor + provisional slots
;; ===========================================================================

(defn- refresh-anchor! []
  (let [st @!ground-edit
        {:keys [line-h]} (metrics)]
    (if-let [a (:anchor st)]
      (let [tree (rt/resolve-layout
                  (rt-node :ground-anchor :rect {:x 0 :y 0 :w 2 :h line-h}
                           :style {:bg [0.95 0.95 0.95 0.9]}))]
        (if-let [slot (ss/slot (scene-rt/store-snapshot) :ground-anchor)]
          (do (swap! scene-rt/!scene-store ss/upsert-slot :ground-anchor
                     {:tree tree :container (:container slot)
                      :meta (:meta slot) :stratum (:stratum slot)
                      :pre-resolved? true})
              (scene-rt/set-transform! (:container slot) {:x (:x a) :y (:y a)}))
          (scene-rt/register-face-instance! :ground-anchor tree
                                            {:x (:x a) :y (:y a) :scale 1.0
                                             :layer 4 :meta {:ground-anchor? true}
                                             :pre-resolved? true})))
      (scene-rt/close-instance! :ground-anchor))))

(defn- refresh-provisional!
  "The open turn's PROVISIONAL projection (Law 8): one current-activity line
   + the arriving stream, dimmed — process-state, never truth, never camera
   motion. Placed beneath the source block; REPLACED at distill by the
   durable provenance-marked reply."
  []
  (let [{:keys [phase activity stream-text error source-unit-id]} @!ground-run
        {:keys [font-size line-h char-advance]} (metrics)
        open? (contains? #{:streaming :distilling} phase)]
    (if (or open? (some? error))
      (let [src    (get-in @!world [:blocks source-unit-id])
            sx     (if src (:x src) 60.0)
            sy     (if src (+ (:y src) (or (:h src) line-h) reply-gap) 60.0)
            slines (when (seq (str stream-text))
                     (str/split-lines (str stream-text)))
            kids   (cond-> []
                     open?
                     (conj (rt-node :ground-activity :text-run
                                    {:x 0 :y 0 :w 600 :h line-h}
                                    :text [(text-op (str "· " (or activity "the resident is working"))
                                                    0 line-h font-size dim 0)]))
                     (seq slines)
                     (conj (rt-node :ground-stream :text-run
                                    {:x 0 :y line-h
                                     :w (+ (* (reduce max 1 (map count slines))
                                              char-advance) 16)
                                     :h (* (count slines) line-h)}
                                    :text (vec (map-indexed
                                                (fn [i l] (text-op l i line-h font-size dim 0))
                                                slines))))
                     (some? error)
                     (conj (rt-node :ground-turn-error :text-run
                                    {:x 0 :y 0 :w 600 :h line-h}
                                    :text [(text-op (str "⟂ " error " — retry is a new turn")
                                                    0 line-h font-size err-col 0)])))
            tree (rt/resolve-layout
                  (rt-node :ground-provisional :stack
                           {:x 0 :y 0 :w 600 :h 0}
                           :layout {:direction :column :gap 6 :auto-height? true}
                           :children kids))]
        (if-let [slot (ss/slot (scene-rt/store-snapshot) :ground-provisional)]
          (do (swap! scene-rt/!scene-store ss/upsert-slot :ground-provisional
                     {:tree tree :container (:container slot)
                      :meta (:meta slot) :stratum (:stratum slot)
                      :pre-resolved? true})
              (scene-rt/set-transform! (:container slot) {:x sx :y sy}))
          (scene-rt/register-face-instance! :ground-provisional tree
                                            {:x sx :y sy :scale 1.0 :layer 6
                                             :meta {:ground-provisional? true}
                                             :pre-resolved? true})))
      (scene-rt/close-instance! :ground-provisional))))

;; ===========================================================================
;; The settle driver — camera + positions as SETTLE-STATE truth
;; ===========================================================================

(defn- fire-settle! [& {:keys [keepalive?]}]
  (let [{:keys [dirty camera?]} @!settle]
    (when (or (seq dirty) camera?)
      (let [cells  (mapv (fn [[uid p]] {:unit-id uid :x (:x p) :y (:y p)}) dirty)
            cam    @!camera
            body   (pr-str (cond-> {:cells cells
                                    :camera (when camera? cam)
                                    :settle-id (str (random-uuid))
                                    :time-ms (js/Date.now)}
                             (drill-conversation-id)
                             (assoc :conversation-id (drill-conversation-id))))]
        (swap! !settle assoc :dirty {} :camera? false)
        (-> (js/fetch "/api/episode/geometry"
                      (clj->js (cond-> {:method "POST"
                                        :headers {"Content-Type" "application/edn"}
                                        :body body}
                                 keepalive? (assoc :keepalive true))))
            (.then (fn [resp]
                     (if (.-ok resp)
                       (swap! !settle update :acked
                              (fn [a] (reduce (fn [m c] (assoc m (:unit-id c)
                                                               {:x (:x c) :y (:y c)}))
                                              a cells)))
                       ;; refused settle → VISIBLE revert to the last
                       ;; acknowledged positions (G4b forced-stale drill)
                       (do (doseq [c cells]
                             (when-let [acked (get-in @!settle [:acked (:unit-id c)])]
                               (swap! !world update-in [:blocks (:unit-id c)]
                                      assoc :x (:x acked) :y (:y acked))
                               (when-let [cid (get-in @!world [:blocks (:unit-id c) :cid])]
                                 (scene-rt/set-transform! cid acked)))
                             (reset! !notice {:unit-id (:unit-id c)
                                              :text "position write refused — reverted"})
                             (rebuild-block! (:unit-id c)))
                           (js/setTimeout (fn [] (reset! !notice nil)
                                            (doseq [c cells] (rebuild-block! (:unit-id c))))
                                          2500)))))
            (.catch (fn [_e]
                      ;; transport failure: keep dirty for the next arm — the
                      ;; acked state is the safety floor, never the exit
                      (swap! !settle (fn [s] (-> s
                                                 (update :dirty #(merge (into {} (map (fn [c] [(:unit-id c) {:x (:x c) :y (:y c)}]) cells)) %))
                                                 (assoc :camera? (boolean (or (:camera? s) camera?)))))))))))))

(defn- arm-settle!
  "Gesture-end ARMS the settle write; the debounce coalesces continuous
   bursts into ONE acked write (never per-event)."
  [kind & [unit-id pos]]
  (swap! !settle (fn [s]
                   (cond-> s
                     (= kind :cell)   (assoc-in [:dirty unit-id] pos)
                     (= kind :camera) (assoc :camera? true))))
  (when-let [t (:timer @!settle)] (js/clearTimeout t))
  (swap! !settle assoc :timer
         (js/setTimeout (fn [] (swap! !settle assoc :timer nil) (fire-settle!))
                        settle-debounce-ms)))

;; ===========================================================================
;; Reconcile — served truth → per-block slots (context/echo edge)
;; ===========================================================================

(defn- context-blocks
  "Reading-order blocks with :speaker attached (the served time merge)."
  [ctx]
  (vec (mapcat (fn [t] (map #(assoc % :speaker (:speaker t)) (:blocks t)))
               (:turns ctx))))

(defn- default-position
  "Derived default for a block with no settled cell and no live position:
   a machine block lands beneath the source block of its turn (§9.4 reply
   placement, left-aligned — parent-relative at birth, independent after);
   successive reply blocks of one turn stack beneath EACH OTHER (all-
   anchored-on-source would pile them on one point); anything else falls
   beneath the previous block in reading order. Ephemeral until a real
   gesture settles it — a derived default is not a settle-write."
  [blocks-acc prev-uid turn-recs block machine? {:keys [line-h]}]
  (let [src-uid (when machine?
                  (:source-unit-id
                   (last (filter #(<= (:time-ms % 0) (:time-ms block 0)) turn-recs))))
        prev    (get blocks-acc prev-uid)
        anchor  (or (when (and machine? (:machine? prev)) prev)
                    (get blocks-acc src-uid)
                    prev)]
    (if anchor
      {:x (:x anchor)
       :y (+ (:y anchor) (or (:h anchor) line-h) reply-gap)}
      {:x 60.0 :y 60.0})))

(defn reconcile!
  "Full reconcile of block slots against a served face-context (the context/
   echo edge — never the RAF edge). Live positions win over cells; cells win
   over derived defaults; return restores NO attention state (moment 6)."
  [ctx]
  (when (and @!refs ctx)
    ;; the context lands FIRST: rebuild-block! reads truth THROUGH it during
    ;; the placement loop (found live: end-of-fn assoc rendered every block
    ;; from the PREVIOUS pull — empty boxes on the first pull after boot)
    (swap! !world assoc :context ctx)
    (let [m        (metrics)
          geometry (:conversation/geometry ctx)
          turn-recs (sort-by :time-ms (:conversation/turn-records ctx []))
          blocks   (context-blocks ctx)
          seen     (set (map :id blocks))
          await    (:await-reply @!ground-run)]
      ;; camera restore — ONCE, at boot (later logins resume the scene; the
      ;; live camera is the inhabitant's after that — Law 3)
      (when (and (not (:camera-restored? @!world)))
        (swap! !world assoc :camera-restored? true)
        (when-let [cam (:conversation/camera ctx)]
          (reset! !camera {:x (double (:x cam)) :y (double (:y cam))
                           :zoom (double (or (:zoom cam) 1.0))})))
      ;; place + upsert every served block
      (loop [bs blocks, placed {}, prev nil, new-replies []]
        (if-let [b (first bs)]
          (let [uid      (:id b)
                machine? (not= (str (:speaker b)) "sid")
                live     (get-in @!world [:blocks uid])
                cell     (get geometry uid)
                pos      (cond
                           (and live (:x live)) {:x (:x live) :y (:y live)}
                           cell {:x (double (:x cell)) :y (double (:y cell))}
                           :else (default-position placed prev turn-recs b machine? m))
                derived? (and (nil? (and live (:x live))) (nil? cell))]
            (swap! !world update-in [:blocks uid]
                   (fn [e] (merge e {:machine? machine? :local? false} pos)))
            (when-let [cid (get-in @!world [:blocks uid :cid])]
              (scene-rt/set-transform! cid pos))
            (rebuild-block! uid)
            (when cell
              (swap! !settle assoc-in [:acked uid] {:x (:x pos) :y (:y pos)}))
            (recur (rest bs)
                   (assoc placed uid (merge pos {:h (get-in @!world [:blocks uid :h])
                                                 :machine? machine?}))
                   uid
                   (cond-> new-replies
                     ;; a reply born by THIS session's distill: its computed
                     ;; birth position becomes durable (independent once
                     ;; born — moving the source later never moves it)
                     (and machine? derived? await
                          (>= (:time-ms b 0) (:time-ms await 0)))
                     (conj [uid pos]))))
          (do
            ;; blocks gone from truth: close their slots (locally-birthed
            ;; blocks awaiting their first re-pull stay)
            (doseq [[uid e] (:blocks @!world)]
              (when (and (not (contains? seen uid)) (not (:local? e)))
                (scene-rt/close-instance! (block-vi uid))
                (swap! !world update :blocks dissoc uid)))
            (when (seq new-replies)
              (doseq [[uid pos] new-replies]
                (arm-settle! :cell uid pos))
              (swap! !ground-run assoc :await-reply nil)))))
      (refresh-provisional!)
      nil)))

(defn on-face-bundle!
  "The <face-main consumer edge in ground mode (replaces build-main-face! —
   the outline-face tree never renders here; the ground IS per-block slots)."
  [{:keys [face-context]}]
  (when face-context
    (when-not (identical? face-context (:context @!world))
      (reconcile! face-context))
    ;; adopt narrowed truth into a resting confirmed value (cross-check)
    (when-let [fid (:focus @!ground-edit)]
      (swap! !ground-edit ge/adopt-truth fid (truth-text fid))
      (rebuild-block! fid))))

;; ===========================================================================
;; Typing — envelopes through the block-write artery (committed echo)
;; ===========================================================================

(defn- block-info
  "The focused block's envelope identity {:id :document-container-id} from
   served truth (BW-T7: the client computes nothing)."
  [unit-id]
  (some (fn [t] (some #(when (= unit-id (:id %))
                         {:id (:id %)
                          :document-container-id (:document-container-id %)})
                      (:blocks t)))
        (:turns (:context @!world))))

;; narrow-echo samples (the G1 bar's measurement seam — envelope submit →
;; decision → confirmed render, ms). Read via window.__softland_atoms-style
;; console access; never rendered.
(defonce !echo-samples (atom []))

(defn- submit-envelope! [env]
  (when-let [submit! (bew/edit-submit!)]
    (let [t0 (js/performance.now)]
      (submit! env
               (fn [decision]
                 (swap! !ground-edit ge/on-decision (:request-id env) decision)
                 (when-let [fid (or (:focus @!ground-edit)
                                    (get-in env [:target :target/id]))]
                   (rebuild-block! fid))
                 (swap! !echo-samples
                        (fn [xs] (let [xs (if (>= (count xs) 512) (subvec xs 1) xs)]
                                   (conj xs (- (js/performance.now) t0))))))))))

(defn- object-key* []
  (:conversation/address (:context @!world)))

;; ===========================================================================
;; Birth — the first content act mints the durable block (§5.1 lane)
;; ===========================================================================

(defn- post-birth! [{:keys [block-id text pos]}]
  (-> (js/fetch "/api/episode/block-birth"
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/edn"}
                          :body (pr-str (cond-> {:block-id block-id
                                                 :text text
                                                 :time-ms (js/Date.now)
                                                 :position {:x (:x pos) :y (:y pos)}}
                                          (drill-conversation-id)
                                          (assoc :conversation-id (drill-conversation-id))))}))
      (.then (fn [resp] (.then (.text resp)
                               (fn [t] {:ok? (.-ok resp)
                                        :data (try (reader/read-string t)
                                                   (catch :default _ nil))}))))
      (.then
       (fn [{:keys [ok? data]}]
         (if-not (and ok? (= :accepted (:status data)))
           (do (swap! !ground-edit ge/birth-failed
                      (or (:error data) :birth-failed))
               (refresh-anchor!))
           (let [unit-id (first (:unit-ids data))
                 pos     {:x (double (:x pos)) :y (double (:y pos))}]
             ;; the block exists NOW: slot from the acked birth text
             (swap! !world assoc-in [:blocks unit-id]
                    (merge {:machine? false :local? true} pos))
             (swap! !settle assoc-in [:acked unit-id] pos)
             (let [{:keys [state envelopes]}
                   (ge/birth-acked @!ground-edit unit-id
                                   {:id unit-id
                                    ;; envelope identity from the birth ack
                                    ;; (BW-T7: the client computes nothing)
                                    :document-container-id
                                    (:document-container-id data)}
                                   (or (object-key*) (:address data)))]
               (reset! !ground-edit state)
               (refresh-anchor!)
               (rebuild-block! unit-id)
               (doseq [env envelopes] (submit-envelope! env)))))))
      (.catch (fn [_e]
                (swap! !ground-edit ge/birth-failed :birth-unreachable)
                (refresh-anchor!)))))

;; ===========================================================================
;; The send lane — Ctrl+Enter, revision-pinned, durable-BEFORE-agent
;; ===========================================================================

(defn- run-event! [turn-id evt]
  (case (:kind evt)
    :episode-durable
    (do (reset! !last-turn-id turn-id)
        (swap! !ground-run assoc :phase :streaming
               :activity "the resident is reading" :error nil))

    :text-delta
    (swap! !ground-run
           (fn [r] (-> r (assoc :activity nil)
                       (update :stream-text str (:text evt)))))

    :thinking-delta
    (swap! !ground-run assoc :activity "thinking")

    :tool-use-start
    (swap! !ground-run assoc :activity (str "using " (:tool-name evt)))

    :run-done
    (swap! !ground-run assoc :phase :distilling :activity "landing the turn")

    :run-error
    ;; the turn closes VISIBLY at its source; retry is a new turn; the
    ;; durable failure fact is the turn cell's status (server-side)
    (swap! !ground-run assoc :phase :idle :activity nil
           :error (str (or (:error evt) :run-error)
                       (when (:detail evt) (str " " (pr-str (:detail evt))))))

    :episode-distilled
    ;; provisional text never survives as truth: the projection drops WHOLE;
    ;; the epoch re-pull renders the durable provenance-marked reply
    (swap! !ground-run
           (fn [r] {:phase :idle :activity nil :stream-text "" :error nil
                    :source-unit-id (:source-unit-id r) :turn-id nil
                    :await-reply {:source-unit-id (:source-unit-id r)
                                  :time-ms (js/Date.now)}}))

    nil)
  (refresh-provisional!))

(defn- transient-notice! [unit-id text]
  (reset! !notice {:unit-id unit-id :text text})
  (rebuild-block! unit-id)
  (js/setTimeout (fn [] (when (= text (:text @!notice))
                          (reset! !notice nil)
                          (rebuild-block! unit-id)))
                 2200))

(defn submit-turn!
  "Ctrl+Enter from the FOCUSED block (target never ambiguous). The pinned
   revision = the block's confirmed text at send time — the turn record
   makes the pin durable BEFORE the agent spawns; mid-stream edits never
   rewrite what the resident answered. A busy resident refuses VISIBLY at
   the block — no queue at genesis; other blocks stay writable/draggable."
  []
  (let [st  @!ground-edit
        fid (:focus st)]
    (cond
      (nil? fid) nil

      (not= :idle (:phase @!ground-run))
      (transient-notice! fid "the resident is mid-turn — this block was not sent")

      :else
      (let [text (get-in st [:queue :confirmed :text])
            b    (get-in @!world [:blocks fid])]
        (when-not (str/blank? (or text ""))
          (let [turn-id (str (random-uuid))]
            (swap! !ground-run assoc :phase :streaming
                   :activity "reaching the land" :stream-text ""
                   :error nil :source-unit-id fid :turn-id turn-id)
            (refresh-provisional!)
            (agent/stream-agent-run!
             "/api/episode/utterance"
             (cond-> {:source-unit-id fid
                      :content-text text
                      :position {:x (:x b) :y (:y b)}
                      :turn-id turn-id
                      :time-ms (js/Date.now)
                      :prev-turn-id @!last-turn-id}
               (drill-conversation-id)
               (assoc :conversation-id (drill-conversation-id)))
             (partial run-event! turn-id)
             (fn [err]
               (swap! !ground-run assoc :phase :idle :activity nil
                      :error (str "send failed: " (.-message err)))
               (refresh-provisional!)))))))))

;; ===========================================================================
;; Keys — the :ground-input focus lane (ALL ground keys route here)
;; ===========================================================================

(defn- handle-content-key!
  "Route one content/caret key by the edit machine's mode."
  [event]
  (let [st @!ground-edit]
    (case (:mode st)
      :editing
      (let [fid (:focus st)
            bi  (or (block-info fid) {:id fid})
            {:keys [state envelope]} (ge/input st event bi (object-key*))]
        (reset! !ground-edit state)
        (when envelope (submit-envelope! envelope))
        (rebuild-block! fid))

      :anchor
      (when-let [{:keys [state post]} (ge/begin-birth st event (str (random-uuid)))]
        (reset! !ground-edit state)
        (refresh-anchor!)
        (post-birth! post))

      :birthing
      (reset! !ground-edit (ge/birth-key st event))

      :rest
      ;; type-without-click: the words land under the pointer (Law 1 — the
      ;; land never swallows speech waiting for a ceremony)
      (when (contains? #{:char :enter :paste} (:type event))
        (let [{:keys [!mouse-x !mouse-y]} (:atoms @!refs)
              [wx wy] (screen->world (or @!mouse-x 200) (or @!mouse-y 200))]
          (swap! !ground-edit ge/set-anchor {:x wx :y wy})
          (refresh-anchor!)
          (handle-content-key! event)))

      nil)))

(defn ground-keys-consumer
  [_atoms <ground-keyboard]
  (->> <ground-keyboard
       (m/reduce
        (fn [_ event]
          (when event
            (case (:type event)
              :eval (submit-turn!)
              (:char :backspace :delete :enter :paste
               :left :right :up :down :home :end :word-left :word-right)
              (handle-content-key! event)
              nil))
          nil)
        nil)))

(defn handle-paste! [text]
  (when (seq text)
    (handle-content-key! {:type :paste :text text})))

(defn escape!
  "Escape: blur the focused block / discard the anchor (an abandoned anchor
   leaves NOTHING). Attention state only — nothing durable moves."
  []
  (swap! !ground-edit (fn [st]
                        (let [fid (:focus st)
                              st' (ge/escape st)]
                          (when fid (js/setTimeout #(rebuild-block! fid) 0))
                          st')))
  (refresh-anchor!))

;; ===========================================================================
;; Pointer grammar (§9.4) — one ~4 CSS px threshold splits click from
;; pan/drag; wheel zooms at the pointer
;; ===========================================================================

(defn- pick-at [sx sy]
  (let [[wx wy] (screen->world sx sy)]
    (when (scene-rt/any-slots?) (scene-rt/pick-world [wx wy]))))

(defn pointer-down! [sx sy]
  (let [hit  (pick-at sx sy)
        ;; deictic seam (scene-substrate P4): pointing is a click act, never
        ;; a hover side effect
        _    (scene-rt/record-pick! (vec (screen->world sx sy)) hit)
        uid  (:address hit)
        b    (when uid (get-in @!world [:blocks uid]))
        [wx wy] (screen->world sx sy)]
    (reset! !pointer
            (if b
              {:phase :pending :screen [sx sy] :world [wx wy]
               :target uid :grab [(- (:x b) wx) (- (:y b) wy)]}
              {:phase :pending :screen [sx sy] :world [wx wy]
               :target :ground :cam-start @!camera}))))

(defn pointer-move! [sx sy]
  ;; hover = attention (Law 10) — ephemeral, never restored
  (let [p @!pointer]
    (when (= :idle (:phase p))
      (let [uid (:address (pick-at sx sy))
            uid (when (get-in @!world [:blocks uid]) uid)]
        (when (not= uid @!hover)
          (let [old @!hover]
            (reset! !hover uid)
            (when old (rebuild-block! old))
            (when uid (rebuild-block! uid))))))
    (case (:phase p)
      :pending
      (when (ge/drag? (:screen p) [sx sy] drag-threshold-px)
        (swap! !pointer assoc :phase
               (if (= :ground (:target p)) :panning :dragging)))
      :panning
      (let [[sx0 sy0] (:screen p)
            cam0 (:cam-start p)]
        (reset! !camera (assoc cam0
                               :x (+ (:x cam0) (- sx sx0))
                               :y (+ (:y cam0) (- sy sy0)))))
      :dragging
      (let [[wx wy] (screen->world sx sy)
            [gx gy] (:grab p)
            uid (:target p)
            pos {:x (+ wx gx) :y (+ wy gy)}]
        (swap! !world update-in [:blocks uid] merge pos)
        (when-let [cid (get-in @!world [:blocks uid :cid])]
          (scene-rt/set-transform! cid pos)))
      nil)))

(defn pointer-up! [sx sy]
  (let [p @!pointer]
    (reset! !pointer {:phase :idle})
    (case (:phase p)
      :pending
      (if (= :ground (:target p))
        ;; click on empty ground: caret anchor at the chosen point (Law 1);
        ;; click-elsewhere leaves a focused block first
        (let [[wx wy] (:world p)]
          (swap! !ground-edit ge/set-anchor {:x wx :y wy})
          (refresh-anchor!)
          (when-let [old (:focus @!ground-edit)] (rebuild-block! old)))
        ;; clean click on a block: edit caret at the clicked position
        (let [uid   (:target p)
              b     (get-in @!world [:blocks uid])
              old   (:focus @!ground-edit)
              m     (metrics)
              [wx wy] (:world p)
              truth (or (truth-text uid) "")
              line  (js/Math.floor (/ (- wy (:y b)) (:line-h m)))
              col   (js/Math.round (/ (- wx (:x b)) (:char-advance m)))
              caret (ge/line-col->caret truth (max 0 line) (max 0 col))]
          (swap! !ground-edit ge/focus-block uid truth caret)
          (refresh-anchor!)
          (when (and old (not= old uid)) (rebuild-block! old))
          (rebuild-block! uid)))
      :dragging
      (let [uid (:target p)
            b   (get-in @!world [:blocks uid])]
        ;; gesture end ARMS the settle (positions settle as truth at
        ;; release — the settled state of a burst, never per-event)
        (arm-settle! :cell uid {:x (:x b) :y (:y b)}))
      :panning
      (arm-settle! :camera)
      nil)))

(defn handle-wheel!
  "Wheel zooms at the pointer — the world point under the pointer stays
   under it (§9.4). Camera settles at burst end (debounce)."
  [{:keys [dy x y]}]
  (let [{:keys [zoom] :as cam} @!camera
        factor (js/Math.pow 1.0015 (- dy))
        zoom'  (-> (* zoom factor) (max 0.1) (min 8.0))
        [wx wy] (screen->world x y)]
    (reset! !camera {:x (- x (* wx zoom'))
                     :y (- y (* wy zoom'))
                     :zoom zoom'})
    (arm-settle! :camera)))

;; ===========================================================================
;; Install (the boot seam)
;; ===========================================================================

(defn install-ground!
  "Boot the open ground: wear the conversation projection over the genesis
   episode (the pull artery — its TREE never renders; per-block slots do),
   route keys to :ground-input, and render NOTHING until the inhabitant
   points (T9: zero content pixels, zero caret, no hidden input)."
  [atoms]
  (reset! !refs {:atoms atoms})
  (reset! !ground-edit (ge/init (str "ground:" (random-uuid))))
  ;; T9 belt: the command panel boots :visible in the dev workspace — the
  ;; ground closes it before the first frame.
  (when-let [!p (:!cmd-panel atoms)] (swap! !p assoc :visible false))
  (reset! (:!face-state atoms)
          {:face :outline-face :address :episode
           :params (cond-> {:limit 64}
                     (drill-conversation-id)
                     (assoc :drill-conversation-id (drill-conversation-id)))})
  (face-wiring/wear-face! atoms :outline-face :episode)
  (reset! (:!focus atoms) :ground-input)
  ;; narrowed truth (the §5 single-unit echo) re-renders its block — the
  ;; committed-echo cross-check channel for unfocused blocks
  (add-watch bew/!truth-overlay ::ground-truth
             (fn [_ _ old new]
               (doseq [uid (keys new)]
                 (when (and (not= (get old uid) (get new uid))
                            (get-in @!world [:blocks uid]))
                   (rebuild-block! uid)))))
  ;; dev observability (the __softland_atoms precedent): read-only state +
  ;; the narrow-echo samples — drives G4b console receipts, renders nothing
  (set! (.-__ground js/window)
        #js {:echo      (fn [] (clj->js @!echo-samples))
             :camera    (fn [] (clj->js @!camera))
             :blocks    (fn [] (clj->js (into {}
                                              (map (fn [[k v]]
                                                     [k (select-keys v [:x :y :machine? :local?])]))
                                              (:blocks @!world))))
             :mode      (fn [] (name (:mode @!ground-edit)))
             :focus     (fn [] (str (:focus @!ground-edit)))
             :confirmed (fn [] (clj->js (get-in @!ground-edit [:queue :confirmed])))
             :run       (fn [] (name (:phase @!ground-run)))})
  ;; exit flush — best-effort BELT; safety is the acknowledged settle write
  (js/window.addEventListener "beforeunload"
                              (fn [_] (fire-settle! :keepalive? true)))
  (js/document.addEventListener "visibilitychange"
                                (fn [_] (when (= "hidden" (.-visibilityState js/document))
                                          (fire-settle! :keepalive? true))))
  nil)

(defn ground-active? [] (some? @!refs))

(defn camera-snapshot [] @!camera)
