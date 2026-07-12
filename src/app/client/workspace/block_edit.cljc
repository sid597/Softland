(ns app.client.workspace.block-edit
  "Lane B — block-write client request outbox + edit-buffer state machine.

   PURE .cljc so the envelope law (CONTRACT §3) and the caret / pending-input
   law (CONTRACT §5, electric-docs L8) are JVM-testable — G6 + the G5 client
   half run over these functions with no cljs runtime. No I/O, no crypto, no
   atoms, no clock: the caller (block_edit_wiring.cljs) owns the atoms, the
   session clock, and the injected `submit!` seam.

   Envelope vocabulary is the EXISTING object-container `:object/edit` request
   (object_container.clj:1650-1720). The client mints identity + payload only;
   the kernel's own `source-hash` stamps `content-hash` server-side (BW-T5 note
   below), so the client carries content-text and NEVER hashes.")

;; ============================================================================
;; Identity — the op-id law (CONTRACT §3, traps BW-T5, BW-T8)
;; ============================================================================

(defn request-id
  "Client-minted request id, DETERMINISTIC from (edit-client-id, edit-seq):
   - unique per keystroke: edit-seq is monotonic per session, so no two
     keystrokes share an id;
   - replay-stable (BW-T5): re-sending the SAME keystroke re-derives the SAME
     id → the SAME idempotency-key → the kernel journals a no-op instead of a
     second write. There is no randomness, hence no cljs crypto.
   One monotonic seq thus drives BOTH ordering (edit-seq) AND identity."
  [edit-client-id edit-seq]
  (str edit-client-id ":" edit-seq))

(defn idempotency-key
  "Deterministic, object-key-scoped op-id. Reuses the object-container edit
   builder's OWN default shape VERBATIM (object_container.clj:1680-1682:
   (str \"object/edit:\" object-key \":\" target-id \":\" request-id)) so a
   client-minted key is byte-identical to the server default — ONE op-id law,
   no second derivation, no drift (BW-T5). Because request-id already carries
   (client-id, seq), this is a pure function of request-id (+ the scope keys)."
  [object-key target-id req-id]
  (str "object/edit:" object-key ":" target-id ":" req-id))

;; The local human actor for Consumer 1. actor/type :human (NOT :system) so the
;; kernel's capability check is genuinely exercised (core/authorized-request?,
;; core.clj:343-350): capabilities MUST include :object/edit or the write is a
;; durable :actor-not-authorized rejection. "human:local" matches the id the
;; face wear outbox already uses (face_wiring.cljs:88).
(def sid-actor
  {:actor/id "human:local"
   :actor/type :human
   :actor/capabilities #{:object/edit}})

;; ============================================================================
;; Envelope minting (CONTRACT §3)
;; ============================================================================

(defn mint-envelope
  "PURE: focused face block + buffer text + session identity → the edit-request
   envelope handed to `submit!`. Fields:
   - target = {:target/kind :derived-unit :target/id <unit-id>} — the unit-id
     FOREVER, even after graduation; the kernel resolves unit→container through
     the graduation join, the client tracks NO container id (BW-T7).
   - payload document-container-id is copied VERBATIM off the focused block
     (PHASE_0 rule 3 / BW-T7): the client computes nothing.
   - payload object-key = the FACE :address (the conversation object-key = the
     partition key, CONTRACT §4); passed in, not read off the block.
   - payload carries content-text ONLY. `content-hash` is NOT the client's:
     the kernel stamps it with its own `source-hash` server-side (no cljs
     crypto). Lane A's server entry point adds it before append (BW-T5).
   Precondition: (:id block) is the currently-focused unit."
  [{:keys [block object-key content-text edit-client-id edit-seq actor]}]
  (let [target-id (:id block)                 ; unit-id (face_projection.clj:82)
        req-id    (request-id edit-client-id edit-seq)]
    {:request-id      req-id
     :idempotency-key (idempotency-key object-key target-id req-id)
     :edit-client-id  edit-client-id
     :edit-seq        edit-seq
     :actor           (or actor sid-actor)
     :target          {:target/kind :derived-unit
                        :target/id   target-id}
     :payload         {:document-container-id (:document-container-id block)
                       :object-key            object-key
                       :content-text          content-text}}))

;; ============================================================================
;; Edit-buffer state machine (CONTRACT §5, traps BW-T4, BW-T6, BW-T8)
;; ============================================================================
;;
;; The buffer is pending-INPUT, not an echo channel (BW-T4). It renders ONLY
;; inside the focused block; a lost echo leaves the block on last-materialized
;; truth (never phantom text); a refusal reverts it. Echoes do NOT drive the
;; buffer — echoes update materialized truth (the pull), which the block shows
;; once focus leaves it. This is the BW-T4 separation made concrete.
;;
;; CARET LAW (BW-T6 / L8): `:buffer` co-carries `:text` AND `:caret` in ONE
;; value. Any single-source derivation over it therefore yields a consistent
;; (text, caret) pair — there is no second signal to glitch against. `block-view`
;; reads the pair from that one value; the wiring watches ONE atom (see the ns
;; comment in block_edit_wiring.cljs).

(defn init-state
  "Fresh edit session. `edit-client-id` is minted FRESH per session by the
   caller (random-uuid at boot) and NEVER persisted/resumed — a resumed old
   client-id with a reset seq is the :edit/stale storm BW-T8 exists to prevent;
   a fresh id per session makes the reset harmless (the kernel keys staleness by
   (client-id, seq), object_container.clj:1374-1379). `next-seq` starts at 0 and
   only ever increases within the session."
  [edit-client-id]
  {:edit-client-id edit-client-id
   :next-seq       0
   :focused-id     nil    ; unit-id of the block in edit mode (BW-T4 scope)
   :buffer         nil    ; {:block-id :text :caret} — text+caret CO-VARY (BW-T6)
   :refusal        nil    ; {:block-id :reason} — the G5 refusal notice
   :pending        {}})   ; request-id -> {:block-id block-id :seq n} in-flight

(defn focus
  "Enter edit mode on a block: seed the buffer from materialized truth, caret at
   end. Clears any stale refusal on that block."
  [state block-id materialized-text]
  (let [txt (or materialized-text "")]
    (-> state
        (assoc :focused-id block-id
               :buffer {:block-id block-id :text txt :caret (count txt)})
        (update :refusal (fn [r] (when (and r (not= (:block-id r) block-id)) r))))))

(defn blur
  "Leave edit mode: drop the buffer AND the refusal notice. The block falls
   back to last-materialized truth on the next render — pending-input never
   outlives focus (BW-T4), and a refusal notice never outlives the edit
   session that provoked it (FALSIFY F6: it used to persist indefinitely)."
  [state]
  (assoc state :focused-id nil :buffer nil :refusal nil))

(defn input
  "One keystroke on the focused block. new-text + new-caret are supplied TOGETHER
   (they co-vary — BW-T6) and stored as one buffer value. Mints the envelope with
   the next seq, advances the seq, and records the request as in-flight. Returns
   {:state s' :envelope env}. No-op (nil :envelope) if `block` is not the focused
   block — a keystroke can only edit the focused unit (BW-T4)."
  [state {:keys [block object-key new-text new-caret]}]
  (if (not= (:id block) (:focused-id state))
    {:state state :envelope nil}
    (let [seq' (:next-seq state)
          env  (mint-envelope {:block          block
                               :object-key     object-key
                               :content-text   new-text
                               :edit-client-id (:edit-client-id state)
                               :edit-seq       seq'
                               :actor          sid-actor})
          ;; FALSIFY F3 bound: envelopes skipped by Electric conflation never
          ;; get a decision, so their :pending entries would grow forever.
          ;; Keep the newest 64 by seq (a trimmed entry's late decision no-ops
          ;; harmlessly in on-decision).
          pending (cond-> (:pending state)
                    (>= (count (:pending state)) 64)
                    (as-> p (dissoc p (key (apply min-key (comp :seq val) p)))))]
      {:state (-> state
                  (assoc :buffer {:block-id (:id block)
                                  :text     new-text
                                  :caret    new-caret})
                  (update :next-seq inc)
                  ;; resuming typing dismisses a standing refusal notice
                  ;; (the documented dismiss path — FALSIFY F6)
                  (assoc :refusal nil)
                  (assoc :pending
                         (assoc pending (:request-id env)
                                {:block-id (:id block) :seq seq'})))
       :envelope env})))

(defn on-decision
  "Consume the durable decision for an in-flight request (CONTRACT §5, G5).
   `decision` = {:status :accepted|:rejected :reason <kw>?}. On accept, retire
   the pending entry (the buffer stays as pending-input until focus leaves and
   the pull shows truth). On reject, REVERT the buffer to materialized truth and
   raise a refusal notice with the reason — no silent drop (G5), and pending-input
   never survives a refusal (BW-T4). `materialized-text` is truth for that block."
  [state req-id decision materialized-text]
  (let [{:keys [block-id]} (get-in state [:pending req-id])
        state (update state :pending dissoc req-id)]
    (case (:status decision)
      :accepted state
      ;; nil block-id = a trimmed/unknown pending entry (F3 bound) — a
      ;; refusal with no addressable block would render nowhere; no-op.
      :rejected (if (nil? block-id)
                  state
                  (cond-> (assoc state :refusal {:block-id block-id
                                                 :reason   (:reason decision)})
                    (= block-id (:focused-id state))
                    (assoc :buffer {:block-id block-id
                                    :text     (or materialized-text "")
                                    :caret    (count (or materialized-text ""))})))
      ;; unknown status: leave state untouched but drop the pending entry
      state)))

(defn dismiss-refusal
  "Clear the refusal notice (e.g. the user resumes typing)."
  [state]
  (assoc state :refusal nil))

(defn move-caret
  "Caret-only transition: update the caret INSIDE the one buffer value (BW-T6 —
   text+caret stay co-carried). Mints NO envelope: the keystroke grain is
   CONTENT events (BW-T3); a same-content request would only mint revision
   noise. Clamped to the buffer text."
  [state new-caret]
  (if-let [buf (:buffer state)]
    (assoc state :buffer
           (assoc buf :caret (max 0 (min (long new-caret) (count (:text buf))))))
    state))

;; ============================================================================
;; Keydown → buffer transition (INT wiring, orchestrating session)
;; ============================================================================

(defn apply-keydown
  "PURE: semantic keydown event (events.cljs/parse-key-event shape) + the
   current buffer {:text :caret} → the transition to apply, or nil when the
   key is a no-op here (unhandled types, backspace at 0, delete at end).
   :op :edit  → content changed: new-text + new-caret co-supplied (BW-T6);
               the caller mints ONE envelope for it (keystroke grain, BW-T3).
   :op :caret → caret-only move: no envelope (see move-caret)."
  [{:keys [text caret]} {:keys [type char]}]
  (let [text  (or text "")
        caret (max 0 (min (long (or caret 0)) (count text)))]
    (case type
      :char      (when (and (string? char) (= 1 (count char)))
                   {:op :edit
                    :new-text  (str (subs text 0 caret) char (subs text caret))
                    :new-caret (inc caret)})
      :enter     {:op :edit
                  :new-text  (str (subs text 0 caret) "\n" (subs text caret))
                  :new-caret (inc caret)}
      :backspace (when (pos? caret)
                   {:op :edit
                    :new-text  (str (subs text 0 (dec caret)) (subs text caret))
                    :new-caret (dec caret)})
      :delete    (when (< caret (count text))
                   {:op :edit
                    :new-text  (str (subs text 0 caret) (subs text (inc caret)))
                    :new-caret caret})
      :left      {:op :caret :new-caret (dec caret)}
      :right     {:op :caret :new-caret (inc caret)}
      :home      {:op :caret :new-caret 0}
      :end       {:op :caret :new-caret (count text)}
      nil)))

;; ============================================================================
;; Face-context projection (INT wiring) — the edit UI painted onto served data
;; ============================================================================

(declare block-view)

(def caret-glyph
  "Thin caret bar rendered INSIDE the focused block's pending-input text.
   A presentation projection of the ONE buffer value (text+caret) — never a
   second signal (BW-T6/L8)."
  "▏")

(defn display-text
  "Pending-input presentation for the FOCUSED block only (BW-T4 scope):
   buffer text with the caret glyph at the caret index. Both inputs come from
   the one buffer value."
  [text caret]
  (let [text  (or text "")
        caret (max 0 (min (long (or caret 0)) (count text)))]
    (str (subs text 0 caret) caret-glyph (subs text caret))))

(defn refusal-notice
  "The visible refusal line (CONTRACT §5 / G5): a rejected decision must
   SURFACE with its reason — silent drops are a gate failure."
  [reason]
  (str "⟂ edit refused: " (if (keyword? reason) (name reason) (str reason))))

(defn overlay-face-context
  "PURE: project the edit UI onto a served face data-context (CONTRACT §5).
   Per block, block-view (the ONE-signal render model, G6) decides what shows:
   - focused block → buffer as pending-input + caret glyph (BW-T4: never
     outside the focused block), refusal notice appended when refused;
   - every other block → truth: the single-unit truth overlay when present
     (the §5 narrowing echo, newest by construction), else the served text.
   Fast path: no focus, no refusal, empty overlay → ctx unchanged (identical),
   so the scene cache's value compare still short-circuits."
  [ctx st truth-overlay]
  (if (or (nil? ctx)
          (and (nil? (:focused-id st)) (nil? (:refusal st)) (empty? truth-overlay)))
    ctx
    (let [xblock (fn [b]
                   (let [id    (:id b)
                         truth (get truth-overlay id (:text b))
                         v     (block-view st id truth)
                         txt   (if (:pending? v)
                                 (display-text (:text v) (:caret v))
                                 (:text v))
                         txt   (if-let [r (:refusal v)]
                                 (str txt "\n" (refusal-notice r))
                                 txt)]
                     (assoc b :text txt)))
          xturn (fn [t] (update t :blocks #(mapv xblock %)))
          turns (mapv xturn (:turns ctx))
          reader (:reader-turn ctx)
          reader' (when reader
                    (or (first (filter #(= (:id reader) (:id %)) turns))
                        (xturn reader)))]
      (assoc ctx :turns turns :reader-turn reader'))))

;; ============================================================================
;; Caret law — the ONE-signal render derivation (CONTRACT §5, BW-T6 / L8, G6)
;; ============================================================================

(defn block-view
  "PURE render model for ONE block. Given the edit-ui `state` (which co-carries
   :focused-id + :buffer + :refusal in a single value) and that block's
   materialized truth, return {:block-id :text :caret :pending? :refusal}.

   The co-varying pair (:text, :caret) is sourced from a SINGLE value:
   - focused block  → both from `:buffer` (one map) → never tear (BW-T6/L8);
   - other blocks   → :text from materialized truth, :caret nil (no caret to
     co-vary), :pending? false — pending-input NEVER renders outside the focused
     block (BW-T4).
   The focused/unfocused choice is a BRANCH on :focused-id (carried in the same
   value as :buffer), never a downstream COMBINE of two signals — so no diamond."
  [state block-id materialized-text]
  (let [focused? (and (= block-id (:focused-id state))
                      (some? (:buffer state))
                      (= block-id (:block-id (:buffer state))))
        buf      (:buffer state)]
    (cond-> (if focused?
              {:block-id block-id
               :text     (:text buf)      ; both from buf — one source (BW-T6)
               :caret    (:caret buf)
               :pending? true}
              {:block-id block-id
               :text     materialized-text
               :caret    nil
               :pending? false})
      (= block-id (:block-id (:refusal state)))
      (assoc :refusal (:reason (:refusal state))))))
