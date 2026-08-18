(ns app.client.workspace.block-edit-wiring
  "The ground editor's thin transport seam.

   The client streams edit envelopes into the Electric-owned outbox and routes
   durable decisions back by request-id. Accepted decisions arm one narrow
   truth pull; that truth overlays the whole-face mirror until the next full
   pull reconciles it. Cursor, selection, keymap, and rendered edit state do not
   live here.")

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

;; §5 narrowing echo: unit-id → newest MATERIALIZED text, set from the
;; :block-truth single-unit pull on an accepted decision. Truth only (never
;; buffer text — BW-T4 keeps pending-input out of every truth channel);
;; pruned when the full face pull catches up.
(defonce !truth-overlay (atom {}))
(defonce ^:private !submit (atom nil))

(defn install-block-edit-wiring!
  "Wire the edit seam into the runtime (called once from runtime.cljs):
   - submit! over Lane A's !block-edit-outbox/!block-edit-result (back-arrow);
   - §5 narrowing: an ACCEPTED decision arms ONE :block-truth pull for the
     edited unit (same FacePull transport; the INV-19 1s debounce guards
     FULL-face pulls — the edit echo reads one unit, no debounce);
   - truth overlay merge + prune (prune when the full pull catches up);
   - publish the submit seam consumed by the ground editor."
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
    ;; narrowed truth arrives → overlay (truth only; the ground's truth lookup
    ;; and overlay watch rebuild the addressed block)
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
    (reset! !submit submit!)))

(defn edit-submit!
  "Raw access to the installed submit! seam (first-light P2b: the ground's
   intent queue routes decisions through its OWN pure machine — ground_edit
   — the continuation registry + result watch are shared, keyed by request-id).
   nil until install."
  []
  @!submit)
