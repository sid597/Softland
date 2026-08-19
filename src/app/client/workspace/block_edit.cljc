(ns app.client.workspace.block-edit
  "The ground editor's shared edit-request vocabulary and key transition.

   PURE .cljc: no I/O, atoms, clock, or randomness. The ground owns edit state;
   this namespace owns deterministic request identity, the durable envelope
   shape, and the small semantic text/caret transition it currently reuses.

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
  "PURE: addressed block + projected text + session identity → the edit-request
   envelope handed to `submit!`. Fields:
   - target = {:target/kind :derived-unit :target/id <unit-id>} — the unit-id
     FOREVER, even after graduation; the kernel resolves unit→container through
     the graduation join, the client tracks NO container id (BW-T7).
   - payload document-container-id is copied VERBATIM off the focused block
     (PHASE_0 rule 3 / BW-T7): the client computes nothing.
   - payload object-key = the served block's exact page provenance (the
     partition key); passed explicitly because this pure builder does not
     infer identity from a container id or a whole face address.
   - payload carries content-text ONLY. `content-hash` is NOT the client's:
     the kernel stamps it with its own `source-hash` server-side (no cljs
     crypto). Lane A's server entry point adds it before append (BW-T5).
   Precondition: (:id block) is the addressed unit."
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
;; Keydown → semantic transition
;; ============================================================================

(defn apply-keydown
  "PURE: semantic keydown event (events.cljs/parse-key-event shape) + the
   current buffer {:text :caret} → the transition to apply, or nil when the
   key is a no-op here (unhandled types, backspace at 0, delete at end).
   :op :edit  → content changed: new-text + new-caret co-supplied (BW-T6);
               the caller mints ONE envelope for it (keystroke grain, BW-T3).
   :op :caret → caret-only move: no envelope; the caller stores the caret."
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
