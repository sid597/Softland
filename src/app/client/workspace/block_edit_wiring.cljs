(ns app.client.workspace.block-edit-wiring
  "The ground editor's thin transport seam.

   The client streams edit envelopes into the Electric-owned outbox and routes
   durable decisions back by request-id. Accepted decisions arm one narrow
   truth pull; that truth overlays the whole-face mirror until the next full
   pull reconciles it. Cursor, selection, keymap, and rendered edit state do not
   live here.")

;; Pending decision continuations, keyed by request-id (real async path).
(defonce ^:private !continuations (atom {}))

(defn cancel-continuation!
  "Dispose one caller-owned callback without cancelling the durable write.
   A later accepted result still arms keyed truth through this wiring owner."
  [request-id]
  (swap! !continuations dissoc request-id)
  true)

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
  ([!edit-outbox !edit-result]
   (atom-submit! !edit-outbox !edit-result nil))
  ([!edit-outbox !edit-result arm-truth!]
   ;; One result watch performs the causal sequence: allocate the keyed nonce,
   ;; deliver it with the decision so the caller installs its wait barrier,
   ;; then publish the read. A cancelled callback still publishes the refresh.
   (add-watch !edit-result ::edit-ack
              (fn [_ _ _ res]
                (let [{:keys [request-nonce publish!]}
                      (when arm-truth! (arm-truth! res))
                      rid (:request-id res)
                      done (get @!continuations rid)]
                  (when done (swap! !continuations dissoc rid))
                  (try
                    (when done
                      (done {:status (if (:accepted? res) :accepted :rejected)
                             :reason (or (:reason res)
                                         (some-> (:errors res) first :type))
                             :request-id rid
                             :target-id (:target-id res)
                             :object-key (:object-key res)
                             :replay? (:replay? res)
                             :request-nonce request-nonce}))
                    (finally
                      (when publish! (publish!)))))))
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
     (reset! !edit-outbox env))))

;; §5 narrowing echo: unit-id → newest MATERIALIZED text, set from the
;; :block-truth single-unit pull on an accepted decision. Truth only (never
;; buffer text — BW-T4 keeps pending-input out of every truth channel);
;; pruned when the full face pull catches up.
(defonce !truth-overlay (atom {}))
(defonce ^:private !submit (atom nil))
(defonce ^:private !truth-nonce (atom 0))

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
        ;; §5 narrowing trigger prepares, but does not publish, the read. The
        ;; single result watch delivers its nonce to the continuation first;
        ;; the returned publish! then resets the request atom. Replays and
        ;; rejections changed no truth, so return nil and perform no pull.
        arm-truth!
        (when !block-truth-request
          (fn [res]
            (when (and (:accepted? res) (not (:replay? res)))
              (let [nonce (swap! !truth-nonce inc)]
                {:request-nonce nonce
                 :publish!
                 (fn []
                   ;; FALSIFY F1: union-map conflation is lossless across
                   ;; units. Keep newest eight; full pull is overflow recovery.
                   (swap! !block-truth-request
                          (fn [req]
                            (let [units (-> (get-in req [:params :units] {})
                                            (assoc (:target-id res) nonce))
                                  units (if (> (count units) 8)
                                          (dissoc units
                                                  (key (apply min-key val units)))
                                          units)]
                              {:face :block-truth
                               :address (:object-key res)
                               :params {:units units}
                               :epoch nonce}))))}))))
        submit! (when (and !block-edit-outbox !block-edit-result)
                  (atom-submit! !block-edit-outbox !block-edit-result arm-truth!))]
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
