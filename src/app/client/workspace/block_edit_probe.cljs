(ns app.client.workspace.block-edit-probe
  "block-write G7 probe (UNCOMMITTED, islands-probe convention — evidence, not
   product). Drives the REAL edit path end-to-end in a headed browser:
   real window KeyboardEvents → events.cljs >keyboard → <face-edit-keys →
   handle-keystroke! → outbox → Electric → submit-block-edit! (:ack) →
   decision → :block-truth single-unit pull → !truth-overlay → <face-assembly
   scene rebuild → RAF paint.

   Echo (CONTRACT §3/§8 G7) = keydown → painted truth: per keystroke k with
   expected buffer text T_k, t-echo(k) = (first RAF after the truth signal for
   T_k reaches the client render model) − t-keydown(k). Measured on BOTH truth
   channels simultaneously:
     :narrow — !truth-overlay (the §5 single-unit pull)
     :full   — !face-context (the INV-19 debounced full face pull)
   Resolution note: paint timestamps ride requestAnimationFrame → ±1 frame
   (~16.7ms) quantization; the criterion (50ms) spans ~3 frames.

   Usage (console or driver), face already worn (/face minimap-reader-face):
     window.__blockwrite.setup()        — focus the first reader block
     window.__blockwrite.start(60, 12)  — 60s at 12 keys/s
     window.__blockwrite.report()       — stats JSON string"
  (:require [app.client.workspace.block-edit :as be]
            [app.client.workspace.block-edit-wiring :as bew]
            [app.client.workspace.face-wiring :as face-wiring]))

(defonce !run (atom nil))

(defn- now [] (js/performance.now))

(defn- pct [sorted p]
  (when (pos? (count sorted))
    (nth sorted (min (dec (count sorted))
                     (long (js/Math.floor (* p (count sorted))))))))

(defn- stats [xs]
  (let [s (vec (sort xs))]
    {:n (count s)
     :p50 (pct s 0.50) :p95 (pct s 0.95) :p99 (pct s 0.99)
     :max (last s)
     :stalls>100ms (count (filter #(> % 100) s))}))

;; window.__softland_atoms is the cljs runtime atoms MAP (keyword keys) —
;; keyword access, never aget.
(defn- atoms* [] (.-__softland_atoms js/window))

(defn wear!
  "Wear a face + set its address (the /face command path, driven directly:
   !face-state reset + face-wiring/wear-face!). nil address → :default (the
   first-light conversation)."
  [face-name address]
  (let [atoms (atoms*)
        kw    (keyword face-name)
        addr  (or address :default)]
    (reset! (:!face-state atoms) {:face kw :address addr :params {:limit 64}})
    (face-wiring/wear-face! atoms kw addr)
    (str kw " @ " (pr-str addr))))

(defn setup!
  "Focus the nth block (reader-turn blocks first, then the river order;
   default 0). Returns unit-id or nil."
  ([] (setup! 0))
  ([n]
   (let [!fc (:!face-context (atoms*))
         ctx (when !fc @!fc)
         blocks (concat (get-in ctx [:reader-turn :blocks])
                        (mapcat :blocks (:turns ctx)))
         b   (nth (vec blocks) (or n 0) nil)]
     (when-let [uid (:id b)]
       (bew/face-click! uid)
       uid))))

(defn force-stale!
  "G8 forced-refusal drill: mint an envelope with the CURRENT session's
   client-id, an ALREADY-USED edit-seq, and a DIFFERENT request-id → the
   kernel's stale-edit? rejects it as a durable :edit/stale (BW-T8 semantics);
   the focused block must visibly revert + show the notice (G5). Requires at
   least one prior keystroke on the focused block."
  []
  (let [st  @bew/!edit-state
        uid (:focused-id st)
        !fc (:!face-context (atoms*))
        ctx (when !fc @!fc)
        b   (some (fn [t] (some #(when (= uid (:id %)) %) (:blocks t))) (:turns ctx))
        okey (:conversation/address ctx)
        seq* (dec (:next-seq st))]
    (if-not (and b (>= seq* 0))
      (js/console.error "[G8] focus a block and type at least one key first")
      (let [env (be/mint-envelope {:block b :object-key okey
                                   :content-text "stale drill"
                                   :edit-client-id (:edit-client-id st)
                                   :edit-seq seq*})
            rid (str (:request-id env) "-stale")
            env (assoc env
                       :request-id rid
                       :idempotency-key (be/idempotency-key okey (:id b) rid))]
        (bew/submit-envelope! env)
        rid))))

(defn- dispatch-key! [ch]
  (.dispatchEvent js/window
                  (js/KeyboardEvent. "keydown"
                                     #js {:key ch :bubbles true :cancelable true})))

(defn- mature!
  "Truth text T arrived on `channel`: every un-claimed pending keystroke whose
   expected text is a prefix of (or equals) T is echoed by T (append-only run ⇒
   prefix order = keystroke order). Claim synchronously (one JS thread — no
   double count), paint-stamp the claimed batch on the next RAF."
  [run channel truth-text]
  (when (string? truth-text)
    (let [r    @run
          ripe (into []
                     (filter (fn [[_ e]]
                               (and (not (contains? (:claimed e #{}) channel))
                                    (let [x (:expected e)]
                                      (and (<= (count x) (count truth-text))
                                           (= x (subs truth-text 0 (count x))))))))
                     (:pending r))]
      (when (seq ripe)
        (swap! run
               (fn [r]
                 (reduce (fn [r [k e]]
                           (let [cl (conj (:claimed e #{}) channel)]
                             (if (= cl #{:narrow :full})
                               (update r :pending dissoc k)
                               (assoc-in r [:pending k :claimed] cl))))
                         r ripe)))
        (js/requestAnimationFrame
         (fn [_]
           (let [tp (now)]
             (swap! run
                    (fn [r]
                      (reduce (fn [r [k e]]
                                (let [echo (- tp (:t0 e))
                                      r (update-in r [:echoes channel] conj echo)]
                                  ;; stall forensics: keystroke index + offset
                                  ;; into the run (clustering separates GC-ish
                                  ;; pauses from load-proportional cost)
                                  (if (> echo 100)
                                    (update-in r [:stall-detail channel] (fnil conj [])
                                               {:k k
                                                :at-s (/ (- (:t0 e) (:t-start r)) 1000.0)
                                                :echo (js/Math.round echo)})
                                    r)))
                              r ripe))))))))))

(defn start!
  "Run the G7 load: `secs` seconds at `rate` keys/s of REAL keydown events on
   the focused block (open-loop — emit regardless of echo, BW-T3 grain)."
  [secs rate]
  (let [uid (or (:focused-id @bew/!edit-state) (setup!))]
    (if-not uid
      (js/console.error "[G7] no face block to focus — wear the face first")
      (let [run (atom {:uid uid :pending {} :echoes {:narrow [] :full []}
                       :emitted 0 :t-start (now)})
            !fc (:!face-context (atoms*))]
        (add-watch bew/!truth-overlay ::g7-narrow
                   (fn [_ _ _ ov] (mature! run :narrow (get ov uid))))
        (add-watch !fc ::g7-full
                   (fn [_ _ _ ctx]
                     (let [b (some (fn [t] (some #(when (= uid (:id %)) %) (:blocks t)))
                                   (:turns ctx))]
                       (mature! run :full (:text b)))))
        (reset! !run run)
        (js/console.log "[G7] start" #js {:secs secs :rate rate :uid uid})
        (let [n-total (long (* secs rate))
              step-ms (/ 1000.0 rate)
              letters "abcdefghijklmnopqrstuvwxyz "]
          ((fn tick [k]
             (if (>= k n-total)
               (js/console.log "[G7] emit done — let it drain, then report()")
               (do
                 (let [ch  (str (nth letters (mod k (count letters))))
                       cur (or (get-in @bew/!edit-state [:buffer :text]) "")]
                   (swap! run (fn [r]
                                (-> r
                                    (assoc-in [:pending (:emitted r)]
                                              {:t0 (now) :expected (str cur ch) :claimed #{}})
                                    (update :emitted inc))))
                   (dispatch-key! ch))
                 (js/setTimeout #(tick (inc k)) step-ms))))
           0))))))

(defn report []
  (if-let [run @!run]
    (let [{:keys [echoes emitted stall-detail]} @run
          out {:emitted emitted
               :narrow (assoc (stats (:narrow echoes))
                              :unechoed (- emitted (count (:narrow echoes))))
               :full   (assoc (stats (:full echoes))
                              :unechoed (- emitted (count (:full echoes))))
               :stall-detail (:narrow stall-detail)}]
      (js/console.log "[G7] report" (clj->js out))
      (js/JSON.stringify (clj->js out)))
    "{}"))

(defonce _mount
  (do (set! (.-__blockwrite js/window)
            #js {:wear       (fn [face addr] (wear! face addr))
                 :setup      (fn [& [n]] (setup! (or n 0)))
                 :start      (fn [secs rate] (start! secs rate))
                 :forceStale (fn [] (force-stale!))
                 :report     (fn [] (report))})
      true))
