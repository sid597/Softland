(ns app.client.workspace.block-edit-wiring
  "Lane B — the thin cljs glue around app.client.workspace.block-edit (pure).

   Owns the session atoms, mints the fresh-per-session edit-client-id (BW-T8),
   and adapts the pure state machine to the runtime: keystrokes in, envelopes to
   the injected `submit!` seam, durable decisions back to `on-decision`.

   BACK-ARROW (settled substrate law): the client streams edit requests INTO
   Rama; it never calls a server fn directly. So the real `submit!` resets an
   OUTBOX atom that Electric watches (the same shape as the face wear outbox,
   face_wiring.cljs:88-90/165-176) and the durable decision returns via a RESULT
   atom whose watch routes the ack back by request-id. Tests inject a synchronous
   stub `submit!` instead — the pure logic is identical either way.

   CARET LAW (BW-T6 / L8): the whole edit UI — :focused-id, :buffer, :refusal —
   lives in ONE atom (!edit-state). The focused block's (text, caret) is derived
   from a SINGLE m/watch over that atom (see <focused-view). Because text and
   caret co-vary inside :buffer (one value), there is no second signal to glitch
   against. Do NOT split caret into its own flow and recombine downstream."
  (:require [missionary.core :as m]
            [app.client.workspace.block-edit :as be]))

;; ---------------------------------------------------------------------------
;; Session state
;; ---------------------------------------------------------------------------

(defn new-session-state
  "Fresh edit-session state with a client-id minted NOW and never persisted
   (BW-T8). Called once per app boot / editing session."
  []
  (be/init-state (str "edit:" (random-uuid))))

(defonce !edit-state (atom (new-session-state)))

;; Pending decision continuations, keyed by request-id (real async path).
(defonce ^:private !continuations (atom {}))

;; ---------------------------------------------------------------------------
;; The submit! seam
;; ---------------------------------------------------------------------------

(defn atom-submit!
  "Build the real `submit!` from an outbox atom + a result atom. Returns a fn
   (env, done) — resets the outbox (Electric appends :object/edit with :ack,
   Lane A / INT wiring) and registers `done` to fire when the matching durable
   decision lands in !edit-result. depth-1 outbox by design: the open-loop
   keystroke grain (BW-T3) means the newest keystroke is the one that matters;
   the RESULT watch routes every decision by request-id so no ack is lost."
  [!edit-outbox !edit-result]
  ;; one watch, installed once, routes durable decisions to continuations.
  ;; INT reconcile (LANE_A.md): submit-block-edit! returns the plain map
  ;; {:accepted? :replay? :reason :errors :status :request-id ...} — adapt it
  ;; to the seam's {:status :accepted|:rejected :reason} here, ONE place.
  (add-watch !edit-result ::edit-ack
             (fn [_ _ _ res]
               (when-let [rid (:request-id res)]
                 (when-let [done (get @!continuations rid)]
                   (swap! !continuations dissoc rid)
                   (done {:status (if (:accepted? res) :accepted :rejected)
                          :reason (or (:reason res)
                                      (some-> (:errors res) first :type))})))))
  (fn submit! [env done]
    ;; FALSIFY F3 bound: continuations for envelopes skipped by Electric
    ;; conflation never fire; keep the newest 64 (request-id = client:seq,
    ;; ordered by the numeric seq tail).
    (swap! !continuations
           (fn [conts]
             (let [conts (if (>= (count conts) 64)
                           (dissoc conts
                                   (apply min-key
                                          #(js/parseInt (subs % (inc (.lastIndexOf % ":"))) 10)
                                          (keys conts)))
                           conts)]
               (assoc conts (:request-id env) done))))
    (reset! !edit-outbox env)))

;; ---------------------------------------------------------------------------
;; Runtime actions
;; ---------------------------------------------------------------------------

(defn focus!
  "Enter edit mode on a face block (its unit-id + current materialized text)."
  [block-id materialized-text]
  (swap! !edit-state be/focus block-id materialized-text))

(defn blur!
  "Leave edit mode."
  []
  (swap! !edit-state be/blur))

(defn handle-keystroke!
  "One keystroke on the focused block. `block` is the focused FACE block map
   (carries :id + :document-container-id); `object-key` is the face :address;
   new-text/new-caret co-vary. Applies the buffer transition and submits the
   minted envelope through `submit!`; the durable decision routes to on-decision.
   `materialized-text-fn` yields truth for a block-id (used on refusal revert)."
  [submit! materialized-text-fn {:keys [block object-key new-text new-caret]}]
  (let [{:keys [state envelope]}
        (be/input @!edit-state {:block block :object-key object-key
                                :new-text new-text :new-caret new-caret})]
    (reset! !edit-state state)
    (when envelope
      (submit! envelope
               (fn [decision]
                 (let [block-id (get-in envelope [:target :target/id])]
                   (swap! !edit-state be/on-decision (:request-id envelope)
                          decision (materialized-text-fn block-id))))))))

;; ---------------------------------------------------------------------------
;; Caret-law render flow (single signal — BW-T6 / L8)
;; ---------------------------------------------------------------------------

(defn <focused-view
  "Single-signal derivation of the FOCUSED block's render model. ONE m/watch over
   !edit-state → {:block-id :text :caret :pending? :refusal}. text and caret both
   come from :buffer inside that one value: no diamond, no tear (BW-T6/L8). Nil
   when nothing is focused."
  []
  (m/latest (fn [st]
              (when-let [fid (:focused-id st)]
                (be/block-view st fid nil)))
            (m/watch !edit-state)))

(defn <block-view
  "Render model for an arbitrary block. The focused/unfocused BRANCH lives inside
   be/block-view, keyed off :focused-id in the SAME value as :buffer — so even
   though truth (materialized-flow) is a second flow, the co-varying pair
   (text,caret) for the focused block is sourced from !edit-state alone. Unfocused
   blocks take text from truth, caret nil (nothing co-varies). See the ns caret-law
   note; this is the sanctioned single-source-per-covarying-pair shape."
  [block-id materialized-flow]
  (m/latest (fn [st materialized-text]
              (be/block-view st block-id materialized-text))
            (m/watch !edit-state)
            materialized-flow))

;; ---------------------------------------------------------------------------
;; INT wiring (orchestrating session) — face runtime integration
;; ---------------------------------------------------------------------------

;; §5 narrowing echo: unit-id → newest MATERIALIZED text, set from the
;; :block-truth single-unit pull on an accepted decision. Truth only (never
;; buffer text — BW-T4 keeps pending-input out of every truth channel);
;; pruned when the full face pull catches up.
(defonce !truth-overlay (atom {}))

;; Runtime refs published by install-block-edit-wiring! (the face_wiring
;; !electric-refs precedent): mouse/keyboard consumers reach the seam without
;; threading new args through their signatures. nil until installed.
(defonce ^:private !api (atom nil))

(defn- context-block
  "Find a face block by unit-id in a served data-context (searches :turns;
   blocks carry :id + :document-container-id after Lane A's projection add)."
  [ctx unit-id]
  (when (and ctx unit-id)
    (some (fn [turn] (some (fn [b] (when (= unit-id (:id b)) b)) (:blocks turn)))
          (:turns ctx))))

(defn- truth-text*
  "Materialized truth for a block: the single-unit overlay when newer, else the
   served face-context text. This is the refusal-revert + focus-seed source —
   NEVER the buffer (BW-T4)."
  [!face-context block-id]
  (or (get @!truth-overlay block-id)
      (:text (context-block @!face-context block-id))))

(defn install-block-edit-wiring!
  "Wire the edit seam into the runtime (called once from runtime.cljs):
   - submit! over Lane A's !block-edit-outbox/!block-edit-result (back-arrow);
   - §5 narrowing: an ACCEPTED decision arms ONE :block-truth pull for the
     edited unit (same FacePull transport; the INV-19 1s debounce guards
     FULL-face pulls — the edit echo reads one unit, no debounce);
   - truth overlay merge + prune (prune when the full pull catches up).
   Publishes the api refs for the mouse/keyboard consumers."
  [atoms {:keys [!block-edit-outbox !block-edit-result
                 !block-truth-request !block-truth-data]}]
  (let [!face-context (:!face-context atoms)
        submit! (when (and !block-edit-outbox !block-edit-result)
                  (atom-submit! !block-edit-outbox !block-edit-result))
        !nonce (atom 0)]
    ;; §5 narrowing trigger — fires in the decision continuation path only,
    ;; never from render (BW-T9). Replays/rejections changed no truth → no
    ;; pull. FALSIFY F1 fix: the request carries a UNION map {unit → nonce},
    ;; not a single unit — Electric conflates the request atom to its latest
    ;; VALUE, and with a union map the latest value contains every armed unit,
    ;; so a cross-unit burst can never lose a unit's final pull. Capped at 8
    ;; entries (drop the lowest-nonce = oldest arm; its pull already executed
    ;; and the clear-all prune below reconciles at the next full pull).
    (when (and !block-edit-result !block-truth-request)
      (add-watch !block-edit-result ::block-truth-pull
                 (fn [_ _ _ res]
                   (when (and (:accepted? res) (not (:replay? res)))
                     (swap! !block-truth-request
                            (fn [req]
                              (let [units (-> (get-in req [:params :units] {})
                                              (assoc (:target-id res) (swap! !nonce inc)))
                                    units (if (> (count units) 8)
                                            (dissoc units (key (apply min-key val units)))
                                            units)]
                                {:face    :block-truth
                                 :address (:object-key res)
                                 :params  {:units units}
                                 :epoch   @!nonce})))))))
    ;; narrowed truth arrives → overlay (truth only; render reads it in the
    ;; SAME m/latest as !edit-state — editor_compute <face-assembly)
    (when !block-truth-data
      (add-watch !block-truth-data ::block-truth-merge
                 (fn [_ _ _ data]
                   (doseq [[uid {:keys [found? text]}] (:block-truth/units data)]
                     (when found?
                       (swap! !truth-overlay assoc uid text))))))
    ;; full face pull arrived → the overlay is superseded WHOLESALE (FALSIFY
    ;; F1/F5 fix): the arriving context was read after every earlier overlay
    ;; merge, and read-unit reads at execution time, so any still-in-flight
    ;; narrow result re-adds CURRENT truth after this clear. Equality-based
    ;; pruning could keep a stale entry forever (F1) and never dropped
    ;; paged-out units (F5); clear-all does both correctly.
    (when !face-context
      (add-watch !face-context ::truth-overlay-prune
                 (fn [_ _ _ _ctx]
                   (reset! !truth-overlay {}))))
    (reset! !api {:submit!       submit!
                  :!face-context !face-context
                  :!focus        (:!focus atoms)})))

(defn edit-submit!
  "Raw access to the installed submit! seam (first-light P2b: the ground's
   intent queue routes decisions through its OWN pure machine — ground_edit
   — not be/on-decision; the continuation registry + result watch are
   shared, keyed by request-id). nil until install."
  []
  (:submit! @!api))

(defn edit-focused?
  "True when a face block is in edit mode (drives keyboard routing + escape)."
  []
  (some? (:focused-id @!edit-state)))

(defn face-blur!
  "Leave edit mode + hand keyboard focus back to the editor."
  []
  (blur!)
  (when-let [!focus (:!focus @!api)]
    (reset! !focus :editor)))

(defn face-click!
  "Assembly-face click → edit focus (INT wiring, mouse.cljs face branch).
   `unit-id-at` is resolved by the caller from the cached scene hit path;
   a click on a block focuses it (seeded from materialized truth, BW-T4);
   a click elsewhere blurs."
  [unit-id]
  (let [{:keys [!face-context !focus]} @!api]
    (if unit-id
      (do (focus! unit-id (truth-text* !face-context unit-id))
          (when !focus (reset! !focus :face-edit)))
      (when (edit-focused?) (face-blur!)))))

(defn submit-envelope!
  "Raw seam access (the G8 forced-refusal drill + probes): push ONE envelope
   through the installed submit! seam and route its durable decision through
   on-decision exactly like a keystroke — so a rejection visibly reverts the
   focused block and raises the notice (G5). No buffer transition here: the
   envelope is the caller's."
  [env]
  (when-let [{:keys [submit! !face-context]} @!api]
    (when submit!
      (submit! env
               (fn [decision]
                 (swap! !edit-state be/on-decision (:request-id env) decision
                        (truth-text* !face-context
                                     (get-in env [:target :target/id]))))))))

(defn face-edit-keys-consumer
  "Missionary consumer for keystrokes while a face block is focused (routed by
   !focus = :face-edit, events.cljs <face-edit-keys). Content keys mint ONE
   envelope each through the submit! seam (BW-T3 open-loop grain); caret keys
   move the caret inside the one buffer value (BW-T6); everything else no-ops."
  [<face-keys]
  (m/reduce
   (fn [_ ev]
     (let [{:keys [submit! !face-context]} @!api
           st  @!edit-state
           fid (:focused-id st)
           buf (:buffer st)]
       (when (and submit! fid buf)
         (when-let [r (be/apply-keydown buf ev)]
           (case (:op r)
             :caret (swap! !edit-state be/move-caret (:new-caret r))
             :edit  (when-let [block (context-block @!face-context fid)]
                      (handle-keystroke!
                       submit!
                       (fn [block-id] (truth-text* !face-context block-id))
                       {:block      block
                        :object-key (:conversation/address @!face-context)
                        :new-text   (:new-text r)
                        :new-caret  (:new-caret r)}))))))
     nil)
   nil <face-keys))
