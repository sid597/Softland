(ns app.server.episode.machine-cut
  "A model-driven annotator for :pairs-with relations.
   Takes: object-container and relation runtimes, an address, block limits, and model output.
   Gives: validated pair plans, relation writes, run results, and replay results.
   Holds: data/machine-cut-log.ednl."
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [app.server.rama.core :as core]
            [app.server.rama.relation-kernel :as rk]
            [app.server.rama.object-container.transcript-identity :as tid]
            [app.server.episode.llm :as llm]
            [app.server.rama.util-fns :as util-fns]))

;; ════════════════════════════════════════════════════════════════════════════
;;  Constants — the annotator's identity is VERSIONED, never run-scoped (MC-T1).
;; ════════════════════════════════════════════════════════════════════════════

(def annotator-version
  "The ANNOTATOR version (prompt text + output schema + validation semantics) —
   NOT the run. relation identity includes the asserter (rk CONTRACT §3), so a
   run-scoped actor would duplicate every edge per re-run (MC-T1). Any change to
   prompt/schema/validation bumps v1→v2 = a NEW asserter (§10.7)."
  "v1")

(def machine-cut-actor-id
  "The v0 machine-cut asserter actor id (CONTRACT §4.3, §4.4). Canonical home for
   the shared constant: the projection (Lane B) filters served edges to THIS
   value (§4.4). INT re-homes it to one shared location; v0 defines it here."
  (str "llm:machine-cut/" annotator-version))

(def asserter-type
  "Silver marks: machine-guessed structure is asserter-type :llm, visibly
   distinct from human assertions (CONTRACT §4.3)."
  :llm)

(def relation-kind
  "The ONE kind this package asserts (CONTRACT §4.1; Sid-authorized 2026-07-12,
   registered in relation_kernel.clj `relation-kinds`). Directed: from = the
   RESPONSE event, to = the user-message/prompt event."
  :pairs-with)

(def annotation-space-id
  "One annotation space, constant (CONTRACT §3). Synthetic, deterministic;
   nothing space-semantic beyond the non-blank check llm.clj:264-274 makes."
  "annotation:machine-cut")

(def default-limit
  "The river-page bound the faces wear (block_distiller.clj:1233
   max-river-page-size = 64). The annotation window is river-page's first page,
   same as the faces (CONTRACT §5.1, §10.5). Mirrored here to keep the driver's
   suite independent of the block-kernel ns (loaded only in production)."
  64)

(def wal-relative-path
  "WAL path literal — the face-arsenal `wear-log-relative-path` /git-spine
   `assert-log-relative-path` precedent (CONTRACT §5.5)."
  "data/machine-cut-log.ednl")

(def ^:private id-part-separator
  ;; Escape sequence in source (NOT a raw control byte — G16 / file(1) text),
  ;; matching relation_kernel.clj:90's id-part-separator convention.
  "\u0000")

(defn default-wal-path
  [{:keys [repo-root]}]
  (str (or repo-root (System/getProperty "user.dir")) "/" wal-relative-path))

;; ════════════════════════════════════════════════════════════════════════════
;;  PURE CORE §5.1 — input build + input-hash (coverage parity)
;;
;;  The annotation input is the SAME data the faces wear: the river-page blocks
;;  the `:conversation` projection reads (block_distiller.clj:1305 river-page →
;;  face_projection.clj:194). NEVER a second read path over raw transcript state
;;  (MC-T4). The driver reads river-page (the shared substrate) and groups by
;;  event-uuid exactly as face_projection/blocks->turns does — but RETAINS
;;  :source-id (evidence, §4.3) which the face's post-shape turn drops.
;; ════════════════════════════════════════════════════════════════════════════

(defn river-blocks->events
  "Group ordered river-page blocks into EVENTS by :event-uuid, preserving river
   order (event order = first-appearance order; block order within an event =
   river-page order). One event per :event-uuid. PURE — `blocks` is river-page's
   output vector (each block: :event-uuid :actor :form :text :unit-id :source-id
   :order ...). Mirrors face_projection/blocks->turns (MC-T4 coverage parity) but
   keeps :source-id + :actor, which the annotator needs and the face strips.

   Each event: {:event-uuid <uuid> :actor <first-block actor> :source-id
   <first-block source-id, §4.3 evidence anchor> :blocks [<block>...]}."
  [blocks]
  (->> blocks
       (reduce
        (fn [{:keys [order->idx events] :as acc} b]
          (let [euid (:event-uuid b)]
            (if-let [idx (get order->idx euid)]
              (update-in acc [:events idx :blocks] conj b)
              (let [idx (count events)]
                (-> acc
                    (assoc-in [:order->idx euid] idx)
                    (update :events conj
                            {:event-uuid euid
                             :actor      (:actor b)
                             :source-id  (:source-id b)
                             :blocks     [b]}))))))
        {:order->idx {} :events []})
       :events))

(defn event-text
  "The concatenated block text for one event (river order within the event)."
  [event]
  (->> (:blocks event) (map (comp str :text)) (str/join "\n")))

(defn input-hash
  "sha-256 over the ordered (event-uuid, unit-id, sha-256(text)) triples across
   ALL river blocks in river order (CONTRACT §5.1). Deterministic identity is
   what makes re-annotation of identical input a total no-op (MC-T9 / §3). One
   changed block text → a different hash → a different run (G1)."
  [blocks]
  (core/sha-256
    (str/join id-part-separator
              (mapcat (fn [b]
                        [(str (:event-uuid b))
                         (str (:unit-id b))
                         (core/sha-256 (str (:text b)))])
                      blocks))))

(defn window-record
  "The §5.1 window record — every coverage claim names the WINDOW, never the
   conversation (coverage-verb law). `read-plan` is river-page's metadata."
  [address limit read-plan blocks]
  {:address            address
   :limit              limit
   :river-events-total (:river-events-total read-plan)
   :blocks-returned    (or (:blocks-returned read-plan) (count blocks))
   :truncated?         (boolean (:truncated? read-plan))})

(defn build-input
  "Assemble the pure annotation input from river-page output (CONTRACT §5.1).
   PURE. `read-plan` is river-page's `:river-page/read-plan` metadata (or a
   synthetic equivalent in gates)."
  [{:keys [address blocks read-plan limit]}]
  (let [events (river-blocks->events blocks)]
    {:address     address
     :limit       (or limit default-limit)
     :blocks      (vec blocks)
     :events      events
     :event-uuids (mapv :event-uuid events)          ; the SHOWN set, river order
     :window      (window-record address (or limit default-limit) read-plan blocks)
     :input-hash  (input-hash blocks)}))

;; ════════════════════════════════════════════════════════════════════════════
;;  PURE CORE §3 — run-hash + synthetic deterministic ids
;;
;;  Run ids are single-use, first-request-wins (llm.clj:1497-1500); the default
;;  idempotency key "space-event:<turn-id>:<bundle-id>" (llm.clj:195-197) is
;;  therefore deterministic — re-submitting identical annotation intent is a
;;  total no-op. The input-hash rides the run-hash (MC-T9): content changes ⇒
;;  the run does not dedup as already-done.
;; ════════════════════════════════════════════════════════════════════════════

(defn run-hash
  "sha-256(conversation object-key, input-hash, annotator-version, salt)
   (CONTRACT §3). A deliberate re-guess over identical input takes an explicit
   non-empty `salt`; default salt \"\" makes re-submission a no-op."
  [address input-hash* salt]
  (core/sha-256 (str/join id-part-separator
                          [(str address) (str input-hash*)
                           annotator-version (str (or salt ""))])))

(defn run-ids
  "The synthetic, deterministic llm-module ids for one annotation run (§3).
   Bundle id encodes the run-hash so the executor's rebuild-verify (§3, MC-T9)
   can compare against a freshly recomputed hash."
  [address input-hash* salt]
  (let [rh (run-hash address input-hash* salt)]
    {:run-hash          rh
     :llm-thread-id     (str "mc-thread:" address)      ; one conversation → one thread
     :turn-id           (str "mc-turn:" rh)
     :llm-turn-run-id   (str "llm-run-mc:" rh)
     :context-bundle-id (str "mc-bundle:" rh)}))

(defn bundle-id->run-hash
  "Recover the run-hash embedded in a context-bundle id (§3 rebuild-verify)."
  [bundle-id]
  (let [s (str bundle-id) p "mc-bundle:"]
    (when (str/starts-with? s p) (subs s (count p)))))

(defn verify-bundle-hash
  "MC-T9 rebuild guard (PURE): the run's bundle id embeds the run-hash of the
   input it was minted from; a fresh rebuild of the input must reproduce it. A
   mismatch means the underlying material changed under the run — the run is
   failed-stale, honestly. Returns {:ok? bool :expected .. :actual ..}."
  [bundle-id address rebuilt-input-hash salt]
  (let [expected (bundle-id->run-hash bundle-id)
        actual   (run-hash address rebuilt-input-hash salt)]
    {:ok?      (= expected actual)
     :expected expected
     :actual   actual}))

;; ════════════════════════════════════════════════════════════════════════════
;;  PURE CORE §5.2 — prompt render + strict, closed-world, TOTAL output contract
;; ════════════════════════════════════════════════════════════════════════════

(def output-schema-doc
  "The strict JSON output shape (CONTRACT §5.2). Prompt wording is lane freedom
   within the contract; this is the closed-world schema the driver validates."
  (str "{\"pairs\": [{\"prompt\": \"<event-uuid>\", "
       "\"responses\": [\"<event-uuid>\", ...]}, ...], "
       "\"unpaired\": [\"<event-uuid>\", ...]}"))

(defn render-event-line
  [event]
  (format "[%s] %s:\n%s"
          (:event-uuid event)
          (str (:actor event))
          (event-text event)))

(defn render-prompt
  "Render the annotation prompt (CONTRACT §5.2). Instructs strict JSON covering
   EVERY shown event exactly once; ambiguity policy fixed — WHEN UNSURE, UNPAIRED
   (silver marks under-claim, never over-claim: the map must not lie, applied to
   guesses)."
  [input]
  (str
    "You are a conversation-structure annotator. Below are the events of one\n"
    "conversation window, each tagged with a stable [event-uuid] and its actor.\n"
    "Group them into TURNS: each turn is one user/human PROMPT event followed by\n"
    "the RESPONSE event(s) that answer it. A response \"pairs-with\" its prompt.\n\n"
    "Rules:\n"
    "- Only a human/user event may be a prompt.\n"
    "- Every shown event-uuid must appear EXACTLY ONCE, as a prompt, a response,\n"
    "  or in \"unpaired\".\n"
    "- When you are unsure where an event belongs, put it in \"unpaired\". Never\n"
    "  guess a pairing you are not confident about.\n"
    "- Use ONLY the event-uuids shown below; never invent ids.\n\n"
    "Respond with STRICT JSON, no prose, matching exactly:\n"
    output-schema-doc "\n\n"
    "Events (window " (get-in input [:window :blocks-returned])
    " blocks of " (get-in input [:window :river-events-total]) " river events"
    (if (get-in input [:window :truncated?]) ", truncated" "") "):\n\n"
    (str/join "\n\n" (map render-event-line (:events input)))))

(defn parse-output
  "Parse the LLM's raw result text as the strict JSON output (CONTRACT §5.2).
   Returns {:ok? true :parsed {...}} or {:ok? false :error {...}} — malformed
   JSON never throws into the driver (§5.3 malformed → recorded failed, zero
   writes)."
  [result-text]
  (try
    (let [parsed (json/read-str (str result-text) :key-fn keyword)]
      (if (map? parsed)
        {:ok? true :parsed parsed}
        {:ok? false :error {:reason :not-a-map :value parsed}}))
    (catch Throwable t
      {:ok? false :error {:reason :invalid-json :message (.getMessage t)}})))

;; ════════════════════════════════════════════════════════════════════════════
;;  PURE CORE §5.3 — validation (driver-side, BEFORE any write). TOTAL, keyed on
;;  the THING (the /atomize A5 law). Closed-world (MC-T3): hallucinated ids must
;;  not mint dangling-legal edges.
;; ════════════════════════════════════════════════════════════════════════════

(defn human-actor?
  "Prompt-side actor law (CONTRACT §5.3): a pair's prompt event's actor must be
   `human:*` (block_distiller.clj:187-199 resolve-actor — tool results are
   `tool`, harness noise is `harness`; T1 role≠actor is already the kernel's
   law). Tool/harness events cannot open a pair."
  [actor]
  (str/starts-with? (str actor) "human:"))

(defn validate-output
  "TOTAL classification of a parsed output against the shown event set
   (CONTRACT §5.3). PURE. Deterministic policy (documented, within the contract):

   1. Closed world (MC-T3): a pair whose prompt or ANY response uuid is NOT in
      the shown set is REJECTED whole; each unknown uuid is counted (:unknown-ids).
   2. Prompt-side actor law: a pair whose prompt actor is not `human:*` is
      REJECTED whole and counted (:non-human-prompts).
   3. At-most-one + first-in-river-order wins: surviving pairs are processed in
      PROMPT river order; the first pair to claim an event (as prompt or response)
      owns it; a later claim on an already-owned event is counted (:duplicates)
      and dropped from the later pair. A pair whose prompt is already owned is
      dropped whole (a pair without its own prompt is meaningless), its prompt
      counted as a duplicate.
   4. Totality by construction: unpaired = shown-set MINUS every owned event, in
      river order. :unclassified counts shown events entirely ABSENT from the
      LLM output (neither paired nor listed in its \"unpaired\").

   Returns {:pairs [{:prompt <uuid> :responses [<uuid>...]}...] (accepted, prompt
   river-order) :unpaired [<uuid>...] :counts {...}}."
  [parsed input]
  (let [shown        (:event-uuids input)
        shown-set    (set shown)
        order-of     (into {} (map-indexed (fn [i u] [u i]) shown))
        actor-of     (into {} (map (juxt :event-uuid :actor) (:events input)))
        raw-pairs    (vec (:pairs parsed))
        raw-unpaired (vec (:unpaired parsed))
        ;; ── step 1+2: shape-reject pairs; count unknown ids + non-human prompts.
        {:keys [survivors unknown-ids non-human malformed empty-pairs]}
        (reduce
          (fn [acc pr]
            (let [rs-raw (:responses pr)
                  ;; F11 fix (2026-07-12 gate): a non-sequential :responses
                  ;; (e.g. a bare string) would seq into characters and count
                  ;; as N unknown-ids — shape-reject it whole instead.
                  rs     (when (sequential? rs-raw) (vec rs-raw))
                  p      (:prompt pr)
                  all    (cons p (or rs []))
                  unk    (remove shown-set all)]
              (cond
                (not (map? pr))
                (update acc :malformed inc)
                (nil? rs)
                (update acc :malformed inc)
                ;; F5 fix (2026-07-12 gate): an empty-responses pair produced
                ;; ZERO edges yet counted as a pair and pulled its prompt out
                ;; of :unpaired — WAL/receipt counts disagreed with what the
                ;; projection can serve. Dropped; the prompt falls to
                ;; :unpaired by totality; counted honestly.
                (empty? rs)
                (update acc :empty-pairs inc)
                (seq unk)
                (-> acc (update :unknown-ids + (count unk)))
                (not (human-actor? (actor-of p)))
                (-> acc (update :non-human inc))
                :else
                (update acc :survivors conj {:prompt p :responses rs}))))
          {:survivors [] :unknown-ids 0 :non-human 0 :malformed 0 :empty-pairs 0}
          raw-pairs)
        ;; ── step 3: deterministic dedup in PROMPT river order.
        ordered (sort-by (fn [pr] (order-of (:prompt pr))) survivors)
        {:keys [accepted owned duplicates]}
        (reduce
          (fn [{:keys [owned] :as acc} pr]
            (let [p (:prompt pr)]
              (if (contains? owned p)
                (update acc :duplicates inc)                 ; prompt already owned → drop pair
                (let [{:keys [rs owned* dups]}
                      (reduce (fn [a r]
                                (if (contains? (:owned* a) r)
                                  (update a :dups inc)
                                  (-> a (update :rs conj r)
                                        (update :owned* conj r))))
                              {:rs [] :owned* (conj owned p) :dups 0}
                              (:responses pr))]
                  (-> acc
                      (update :accepted conj {:prompt p :responses rs})
                      (assoc :owned owned*)
                      (update :duplicates + dups))))))
          {:accepted [] :owned #{} :duplicates 0}
          ordered)
        ;; ── step 4: totality. unpaired = shown - owned, river order.
        unpaired      (vec (remove owned shown))
        ;; :unclassified = shown events the LLM never mentioned at all.
        mentioned     (into #{} (concat (mapcat (fn [pr] (cons (:prompt pr) (:responses pr)))
                                                raw-pairs)
                                        raw-unpaired))
        unclassified  (count (remove mentioned shown))]
    {:pairs    (vec accepted)
     :unpaired unpaired
     :counts   {:pairs            (count accepted)
                :unpaired         (count unpaired)
                :unknown-ids      unknown-ids
                :non-human-prompts non-human
                :duplicates       duplicates
                :malformed-pairs  malformed
                :empty-pairs      empty-pairs
                :unclassified     unclassified}}))

;; ════════════════════════════════════════════════════════════════════════════
;;  PURE CORE §4 — pair → edge specs. Endpoint addressing is EXISTING forms only
;;  (§4.2 — no new target-kind, no new id fn, no kernel routing edit).
;; ════════════════════════════════════════════════════════════════════════════

(defn edge-note
  "note grammar v1 (CONTRACT §4.3, git-spine precedent):
   \"machine-cut v1 run=<run-id> model=<observed-model> window=<n>/<total>\"."
  [run-id observed-model window]
  (format "machine-cut %s run=%s model=%s window=%s/%s"
          annotator-version
          (str run-id)
          (str (or observed-model "unknown"))
          (:blocks-returned window)
          (:river-events-total window)))

(defn pairs->edge-specs
  "Turn accepted pairs into edge specs (CONTRACT §4). One edge per (response,
   prompt): `from` = the RESPONSE event, `to` = the user-message/prompt event
   (directed, §4.1 — \"this response pairs-with that prompt\"). evidence-source-id
   = the RESPONSE event's first river block's :source-id (§4.3; evidence-anchor
   nil in v0). PURE.

   `address` = the conversation object-key. target-id = chat-message-id
   (transcript_identity.clj:18-20); both endpoints route to the CONVERSATION
   object-key (object_container.clj:329-330), so every pair edge of one
   conversation COLOCATES under one target-key (§4.2 — the read plan depends on
   this; MC-T14 dedup applies)."
  [{:keys [address pairs events run-id observed-model window asserted-at-ms]}]
  (let [source-of (into {} (map (juxt :event-uuid :source-id) events))
        note      (edge-note run-id observed-model window)]
    (vec
      (for [pr pairs
            r  (:responses pr)]
        (let [from-ref (rk/->target-ref :container (tid/chat-message-id address r))
              to-ref   (rk/->target-ref :container (tid/chat-message-id address (:prompt pr)))
              rel-id   (rk/relation-id-for relation-kind from-ref to-ref machine-cut-actor-id)]
          {:relation-id        rel-id
           ;; idempotency-key = request-id = "mc:" + relation-id — STABLE across
           ;; runs (MC-T2: never embed run-id or any per-run volatile; the W2-F3
           ;; fire minted a fresh key per run and defeated the relation journal).
           :request-id         (str "mc:" rel-id)
           :kind               relation-kind
           :from-ref           from-ref
           :to-ref             to-ref
           :from-id            (:target-id from-ref)         ; chat-message-id (WAL-plain)
           :to-id              (:target-id to-ref)
           :response-uuid      r
           :prompt-uuid        (:prompt pr)
           :evidence-source-id (source-of r)
           :note               note
           :asserted-at-ms     asserted-at-ms})))))

;; ════════════════════════════════════════════════════════════════════════════
;;  PURE CORE §5.4 — pair-plan diff (durable-read reconcile). The existing set is
;;  QUERYABLE (colocated edges, §4.2) — no basis atom, nothing to lose across
;;  boots (contrast code_atoms' in-memory basis).
;; ════════════════════════════════════════════════════════════════════════════

(defn machine-cut-edge?
  "True for a RelationEdgeRow authored by THIS annotator actor over :pairs-with."
  [row]
  (and (= relation-kind (:relation-kind row))
       (= machine-cut-actor-id (:asserter-actor-id row))))

(defn in-window-scope?
  "An existing edge is in reconcile scope iff BOTH endpoints' event-uuids are in
   the annotated window (CONTRACT §5.4 — re-annotating one window must not retract
   edges outside it). `scope-ids` = the window's chat-message-ids."
  [scope-ids row]
  (and (contains? scope-ids (:target-id (:from row)))
       (contains? scope-ids (:target-id (:to row)))))

(defn transition-request-id
  "Idempotency/request id for ONE reconcile transition (CONTRACT §4.3 as
   AMENDED 2026-07-12 at gate — FALSIFY_A F2/F3): the relation journal is
   relation-scoped and REPLAYS a duplicate key's prior decision
   (relation_kernel CONTRACT §5 step 2), so a key stable per-edge-FOREVER
   swallows every transition after the first — assert→retract→re-assert left
   the edge :retracted and the map lying. The key is stable per logical
   TRANSITION instead: the FIRST assert of a never-seen relation keeps the
   plain key; any transition on an INCUMBENT row keys on the incumbent's
   status-changed-at-ms — deterministic from READ state, so a client retry
   of the same transition re-derives the same key (MC-T2's ban on per-run
   volatiles still holds: no run-id, no wall clock)."
  [relation-id desired-status incumbent-row]
  (if (nil? incumbent-row)
    (str "mc:" relation-id)
    (str "mc:" relation-id ":" (name desired-status) ":"
         (long (or (:status-changed-at-ms incumbent-row) 0)))))

(defn reconcile-plan
  "Diff desired vs existing (CONTRACT §5.4). PURE.
     desired-edges : edge specs (each with :relation-id) — the validated set.
     existing-rows : RelationEdgeRow maps ALREADY filtered to the machine-cut
                     actor + window scope + deduped by relation-id (MC-T14);
                     MUST include retracted rows (incumbents key transitions).
   Returns {:asserts [<spec>...] :retracts [<row>...] :unchanged [<spec>...]}:
     asserts   = desired not currently ASSERTED (new OR re-assert-after-retract),
                 each carrying its transition :request-id (F2 fix);
     retracts  = currently-asserted machine-cut edges (in scope) not desired,
                 each carrying its transition :mc/request-id (F2 fix);
     unchanged = desired already asserted (no-op; nothing appended)."
  [desired-edges existing-rows]
  (let [desired-by-rid (into {} (map (juxt :relation-id identity)) desired-edges)
        desired-rids   (set (keys desired-by-rid))
        row-by-rid     (into {} (map (juxt :relation-id identity)) existing-rows)
        asserted-rows  (filter #(= :asserted (:relation-status %)) existing-rows)
        asserted-rids  (set (map :relation-id asserted-rows))]
    {:asserts   (->> desired-edges
                     (remove #(contains? asserted-rids (:relation-id %)))
                     (mapv (fn [e]
                             (let [rid (:relation-id e)
                                   k   (transition-request-id
                                         rid :asserted (get row-by-rid rid))]
                               (assoc e :request-id k)))))
     :retracts  (->> asserted-rows
                     (remove #(contains? desired-rids (:relation-id %)))
                     (mapv (fn [row]
                             (assoc row :mc/request-id
                                    (transition-request-id
                                      (:relation-id row) :retracted row)))))
     :unchanged (vec (filter #(contains? asserted-rids (:relation-id %)) desired-edges))}))

;; ════════════════════════════════════════════════════════════════════════════
;;  IPC SHELL — the annotation ride (CONTRACT §3), reconcile append (§5.4),
;;  WAL-first (§5.5), epoch bump (§5.6). llm-module rows are intent/lifecycle;
;;  relation writes are foreign appends AFTER validation (MC-T5).
;; ════════════════════════════════════════════════════════════════════════════

(defn- default-load-river-blocks
  "Production river-page loader — lazily resolved so the driver's own ns/suite
   never loads the block-kernel runtime (the driver depends only on
   relation_kernel + llm; block material is injected). Returns
   {:blocks [...] :read-plan {...}} shaped like river-page + its metadata."
  [oc-rt address limit]
  (let [river-page (requiring-resolve 'app.server.ingest.block-distiller/river-page)
        page       (river-page {:oc-rt oc-rt :object-key address} limit)]
    {:blocks (vec page)
     :read-plan (:river-page/read-plan (meta page))}))

(defn- append-wal-line!
  "One pure-edn line, UTF-8 (the face-arsenal append-wear-log-line! / git-spine
   route-writer discipline, CONTRACT §5.5). Plain maps only — safe under
   clojure.edn at replay."
  [path event]
  (locking append-wal-line!
    (let [f (io/file path)]
      (io/make-parents f)
      (with-open [w (io/writer f :append true :encoding "UTF-8")]
        (.write w (pr-str event))
        (.write w "\n")))))

;; The final JSON output rides the `:claude/result` observation's :result/text
;; (the result fold closes the run terminal but does NOT store the text; the
;; returned observations carry it — llm.clj:2105, observation constructor
;; select-keys :result/text). Terminal STATUS is read from the MATERIALIZED run
;; row (barrier), never inferred from the adapter output.
(defn- finished-at-of [run-row] (:finished-at run-row))

(declare annotate-run! apply-transitions! reconcile-from-wal-line!
         read-wal-lines wal-line-for-run read-machine-cut-edges)

(defn annotate-conversation!
  "The v0 trigger surface (CONTRACT §5.7): annotate one conversation window's
   pair structure and reconcile the machine-cut edges. D-008 A2's writer is
   exactly this agent-on-Sid's-instruction path — no workspace command, no
   Electric write surface (§10.4).

   `ctx`  : {:llm-rt <llm runtime> :rk-rt <relation runtime>
             :oc-rt <oc runtime, optional — only for the default river loader>
             :load-river-blocks (fn [address limit] -> {:blocks :read-plan})
                — INJECTED; defaults to river-page over :oc-rt (MC-T4 shared read)
             :wal-path <override, optional>}
   `address` : the conversation object-key (conversation-projection's :address).
   `opts`  : {:limit <default 64> :salt <default \"\"> :executor-id
             :lines <canned stream-json lines → fake adapter; ALL suite gates use
                     this, no live LLM (§3 test seam llm.clj:2179-2184)>
             :timeout-ms}

   Flow: build input (§5.1) → run-hash + synthetic ids (§3) → submit turn-run
   intent + await pending → claim & run the executor (spawn is HERE, MC-T5) with
   a rebuild-verify bundle (MC-T9) → await terminal → parse + validate (§5.2/3) →
   WAL line FIRST (§5.5) → reconcile append (§5.4) → epoch bump (§5.6, only after
   edge acks on a non-zero-write run). Returns a summary map (never throws on a
   failed/malformed run — records it honestly, zero edge writes)."
  [ctx address opts]
  (let [{:keys [llm-rt rk-rt oc-rt load-river-blocks wal-path]} ctx
        {:keys [limit salt executor-id lines timeout-ms]
         :or   {salt "" executor-id "machine-cut-executor" timeout-ms 5000}} opts
        limit     (or limit default-limit)
        loader    (or load-river-blocks (fn [a l] (default-load-river-blocks oc-rt a l)))
        wal-path  (or wal-path (:wal-path ctx) (default-wal-path {}))
        ;; ── §5.1 input (the projection's own serve; window + hash recorded).
        {:keys [blocks read-plan]} (loader address limit)
        input     (build-input {:address address :blocks blocks
                                :read-plan read-plan :limit limit})
        ih        (:input-hash input)
        window    (:window input)
        ;; ── §3 synthetic ids.
        ids       (run-ids address ih salt)
        run-id    (:llm-turn-run-id ids)
        ;; ── Idempotent guard (§3 first-request-wins; G6): the run-id is single-
        ;;    use. If this exact run (same input+version+salt) already reached a
        ;;    terminal state, re-annotation is a TOTAL no-op — ZERO adapter calls,
        ;;    zero writes (the edges are already asserted; desired == existing).
        ;;    A deliberate re-guess takes an explicit :salt → a fresh run-id.
        existing-run (llm/read-run llm-rt run-id)]
    (cond
      ;; F1 fix (2026-07-12 gate): a prior :succeeded run is NOT taken on
      ;; faith — run-state and edge-state are written non-atomically, so the
      ;; noop path VERIFIES-AND-CONVERGES from the run's own durable WAL line
      ;; (zero adapter calls; a crash between WAL write and edge appends
      ;; converges here instead of freezing wrong forever). A succeeded run
      ;; with NO WAL line (crash between terminal and WAL write) cannot
      ;; rebuild its desired set without the LLM output → honest stale, salt
      ;; to re-guess.
      (and existing-run (= :succeeded (:status existing-run)))
      (if-let [line (wal-line-for-run wal-path run-id)]
        (let [{:keys [asserted retracted unmaterialized]}
              (reconcile-from-wal-line! rk-rt line)
              wrote? (pos? (+ (long asserted) (long retracted)))
              epoch  (when wrote? (swap! util-fns/!ingest-epoch-atom inc))]
          {:status :noop-complete :run-id run-id :address address :window window
           :input-hash ih :terminal-status :succeeded
           :edges-asserted asserted :edges-retracted retracted
           :edges-unmaterialized unmaterialized
           :epoch-bumped? (boolean wrote?) :epoch epoch})
        {:status :stale-no-wal :run-id run-id :address address :window window
         :input-hash ih :terminal-status :succeeded :retry-with-salt true
         :edges-asserted 0 :edges-retracted 0 :epoch-bumped? false})

      ;; F8 (accepted v0 limitation, falsifier named in the gate artifact): a
      ;; transiently-failed run permanently noops THIS identity; the honest
      ;; escape is an explicit :salt (a deliberate new paid run), hinted here.
      (and existing-run (contains? llm/terminal-statuses (:status existing-run)))
      {:status :failed :run-id run-id :address address :window window
       :input-hash ih :terminal-status (:status existing-run)
       :reason :prior-run-failed :retry-with-salt true
       :edges-asserted 0 :edges-retracted 0 :epoch-bumped? false}

      :else
      (annotate-run! ctx address input ids
                     {:salt salt :limit limit :loader loader :wal-path wal-path
                      :executor-id executor-id :lines lines :timeout-ms timeout-ms})))
  )

(defn- annotate-run!
  "The RUN path of annotate-conversation! (extracted for readability): submit the
   llm-module intent, run the executor (spawn HERE, MC-T5), validate, WAL-first,
   reconcile, append, epoch bump. Only reached when this run-id has no terminal
   record (a fresh or crashed-pending run)."
  [ctx address input ids
   {:keys [salt limit loader wal-path executor-id lines timeout-ms]}]
  (let [{:keys [llm-rt rk-rt]} ctx
        ih        (:input-hash input)
        window    (:window input)
        run-id    (:llm-turn-run-id ids)
        prompt    (render-prompt input)
        ;; The executor's rebuild-verify (MC-T9): recompute the input hash from a
        ;; fresh load and compare against the run-hash embedded in the bundle id.
        load-bundle (fn [bundle-id]
                      (let [{:keys [blocks]} (loader address limit)
                            rih   (input-hash blocks)
                            check (verify-bundle-hash bundle-id address rih salt)]
                        (when-not (:ok? check)
                          (throw (ex-info "machine-cut: bundle hash mismatch (failed-stale, MC-T9)"
                                          {:bundle-id bundle-id :check check})))
                        {:context-bundle/id  bundle-id
                         :rendered/model-input prompt
                         :input-hash rih}))
        ;; ── §3 submit intent (llm-module turn-run-request; synthetic ids).
        request   (llm/turn-run-request
                    annotation-space-id (:turn-id ids) (:context-bundle-id ids)
                    {:llm-turn-run-id   run-id
                     :llm-thread-id     (:llm-thread-id ids)
                     :request-id        (str "mc-req:" (:run-hash ids))
                     :time-ms           (core/now-ms)
                     :llm/backend       :claude
                     :llm/auth-mode     :subscription
                     :executor-task-id  llm/pending-task-id})
        _         (llm/append-turn-run-request! llm-rt request)
        _         (llm/await-decision llm-rt run-id)
        _         (llm/await-run llm-rt run-id #(= :pending (:status %)))
        ;; ── executor: TARGETED claim of OUR run-id (F7 fix, 2026-07-12 gate:
        ;;    run-one-pending-with-* executes the lowest-sorted pending entry,
        ;;    which under a stale/concurrent pending is a DIFFERENT run — and
        ;;    its bundle-hash mismatch then threw out of this fn). Spawn stays
        ;;    in the DRIVER (MC-T5). Fake adapter via :lines for every suite
        ;;    gate. A bundle-hash mismatch is caught → honest :failed, never a
        ;;    throw.
        claim-result (llm/claim-run! llm-rt run-id executor-id
                                     {:timeout-ms 5000})
        granted?     (= :granted-to-us (:claim-state claim-result))
        claimed-row  (:run claim-result)
        bundle       (when granted?
                       (try {:ok (load-bundle (:context-bundle-id ids))}
                            (catch Throwable t
                              {:err (.getMessage t)})))
        adapter      (llm/claude-stream-json-adapter
                       (cond-> {:timeout-ms timeout-ms}
                         lines (assoc :lines lines)))
        obss         (when (:ok bundle)
                       (let [events (vec (llm/run-adapter-turn
                                           adapter
                                           {:run claimed-row
                                            :claim (:claim claim-result)
                                            :context-bundle (:ok bundle)}))
                             observations
                             (map-indexed
                               #(llm/adapter-event->observation
                                  claimed-row (:claim claim-result) %1 %2)
                               events)]
                         (doseq [obs observations]
                           (llm/append-observation! llm-rt obs))
                         (vec observations)))
        ;; ── barrier: microbatch append-ack does NOT imply the fold happened;
        ;;    wait on the MATERIALIZED terminal run row, bounded by the RUN's
        ;;    timeout (F9 fix: the 2s await default under a slow fold read a
        ;;    non-terminal row and recorded an honest run as :failed). A
        ;;    claim/bundle failure short-circuits — no observations were
        ;;    appended, so no terminal fold is coming; don't wait for it.
        run-row   (if (and granted? (:ok bundle))
                    (llm/await-materialized
                      #(llm/read-run llm-rt run-id)
                      #(contains? llm/terminal-statuses (:status %))
                      (max 10000 (long timeout-ms)))
                    (or claimed-row (llm/read-run llm-rt run-id)))
        result-obs (last (filter #(= :claude/result (:observation/type %)) obss))
        terminal-status (:status run-row)
        succeeded? (= :succeeded terminal-status)
        result-text (:result/text result-obs)
        ;; INT-3 plumb (2026-07-12): the model rides the :claude/system-init
        ;; observation's :raw/json (llm.clj:1929-1949 — the redacted system
        ;; line; a real `claude -p` run's first line carries :model). The
        ;; token-usage read stays as fallback; both absent → "unknown" in the
        ;; note (honest recording, not unplumbed control — CONTRACT §3/§10.8).
        observed-model (or (some #(when (= :claude/system-init (:observation/type %))
                                    (get-in % [:raw/json :model]))
                                 obss)
                           (get-in run-row [:token-usage :model]))
        asserted-at-ms (or (finished-at-of run-row) (core/now-ms))
        ;; ── §5.2/§5.3 parse + validate.
        parse     (when (and succeeded? (some? result-text)) (parse-output result-text))
        valid     (when (:ok? parse) (validate-output (:parsed parse) input))]
    (if-not (and succeeded? (:ok? parse) valid)
      ;; ── FAILED / MALFORMED run: WAL a failed line, ZERO edge writes, no
      ;;    epoch bump (§5.3, MC-T6 negative side). Honest counts (§5.3).
      (let [wal {:mc/version annotator-version
                 :mc/status  :failed
                 :mc/address address
                 :mc/window  window
                 :mc/input-hash ih
                 :mc/run     {:run-id run-id :terminal-status terminal-status
                              :observed-model observed-model :cost-usd (:result/cost-usd result-obs)
                              :asserted-at-ms asserted-at-ms}
                 :mc/reason  (cond (not granted?)     :claim-not-granted
                                   (:err bundle)      :bundle-hash-mismatch
                                   (not succeeded?)   :run-not-succeeded
                                   (not (:ok? parse)) :malformed-json
                                   :else              :validation-empty)
                 :mc/parse-error (or (:error parse) (:err bundle))}]
        (append-wal-line! wal-path wal)
        {:status :failed :run-id run-id :address address :window window
         :input-hash ih :terminal-status terminal-status
         :reason (:mc/reason wal) :edges-asserted 0 :edges-retracted 0
         :epoch-bumped? false})
      ;; ── SUCCEEDED: build edges, WAL-first, reconcile, append, epoch bump.
      (let [edges (pairs->edge-specs {:address address :pairs (:pairs valid)
                                      :events (:events input) :run-id run-id
                                      :observed-model observed-model :window window
                                      :asserted-at-ms asserted-at-ms})
            wal   {:mc/version annotator-version
                   :mc/status  :completed
                   :mc/address address
                   :mc/window  window
                   :mc/input-hash ih
                   :mc/run     {:run-id run-id :terminal-status terminal-status
                                :observed-model observed-model :cost-usd (:result/cost-usd result-obs)
                                :asserted-at-ms asserted-at-ms}
                   :mc/counts  (:counts valid)
                   :mc/pairs   (:pairs valid)
                   :mc/unpaired (:unpaired valid)
                   ;; WAL-plain edge recipe (no defrecords): replay rebuilds the
                   ;; assert request deterministically from these fields (§5.5).
                   :mc/edges   (mapv (fn [e] {:kind (:kind e) :from-id (:from-id e)
                                              :to-id (:to-id e)
                                              :evidence-source-id (:evidence-source-id e)
                                              :note (:note e)
                                              :asserted-at-ms (:asserted-at-ms e)})
                                     edges)}
            ;; §5.5: WAL line BEFORE the relation appends — an append failure
            ;; after the WAL line converges at boot replay (MC-T7).
            _     (append-wal-line! wal-path wal)
            ;; §5.4 existing set: colocated read under the conversation key
            ;; (§4.4), retracted included (transition-key incumbents, F2),
            ;; deduped by relation-id (MC-T14 — both endpoint copies land under
            ;; ONE key), filtered to the machine-cut actor + window scope.
            scope-ids (into #{} (map #(tid/chat-message-id address %)) (:event-uuids input))
            existing  (->> (read-machine-cut-edges rk-rt address)
                           (filter #(in-window-scope? scope-ids %))
                           vec)
            plan      (reconcile-plan edges existing)
            ;; §5.4 append + barrier-CHECK (F4 fix): counts and the epoch
            ;; decision come from MATERIALIZED transitions, never the plan; a
            ;; timed-out barrier surfaces as :incomplete-writes, not success.
            {a-ok :asserted r-ok :retracted unmat :unmaterialized}
            (apply-transitions! rk-rt plan asserted-at-ms)
            wrote? (pos? (+ (long a-ok) (long r-ok)))
            ;; §5.6 completion side-effect: bump the ingest epoch AFTER
            ;; materialization so INV-19 re-pulls fire and worn faces re-render
            ;; (MC-T6). ONLY when at least one transition materialized (G9).
            epoch (when wrote? (swap! util-fns/!ingest-epoch-atom inc))]
        {:status (if (zero? (long unmat)) :completed :incomplete-writes)
         :run-id run-id :address address :window window
         :input-hash ih :terminal-status terminal-status
         :counts (:counts valid)
         :edges-desired (count edges)
         :edges-asserted a-ok
         :edges-retracted r-ok
         :edges-unmaterialized unmat
         :edges-unchanged (count (:unchanged plan))
         :epoch-bumped? (boolean wrote?)
         :epoch epoch
         :observed-model observed-model}))))

;; ════════════════════════════════════════════════════════════════════════════
;;  §5.5 — WAL boot replay. Re-derive edge requests from each completed line
;;  deterministically and re-append; ZERO adapter calls (G11). Torn trailing
;;  line dropped honestly (face-arsenal replay-wear-log! precedent). Journal +
;;  deterministic ids converge; later lines re-apply reconcile in file order.
;; ════════════════════════════════════════════════════════════════════════════

(defn wal-line->edge-specs
  "Deterministically rebuild the §4 edge specs from ONE completed WAL line
   (§5.5). PURE (no IPC): ids reconstruct from kind/from-id/to-id via
   ->target-ref + relation-id-for, so replay converges on the same relation-ids."
  [{:keys [:mc/edges]}]
  (mapv (fn [e]
          (let [from-ref (rk/->target-ref :container (:from-id e))
                to-ref   (rk/->target-ref :container (:to-id e))
                rel-id   (rk/relation-id-for (:kind e) from-ref to-ref machine-cut-actor-id)]
            {:relation-id rel-id :request-id (str "mc:" rel-id)
             :kind (:kind e) :from-ref from-ref :to-ref to-ref
             :from-id (:from-id e) :to-id (:to-id e)
             :evidence-source-id (:evidence-source-id e) :note (:note e)
             :asserted-at-ms (:asserted-at-ms e)}))
        edges))

(defn- read-machine-cut-edges
  "Every machine-cut :pairs-with edge currently under a conversation key
   (colocated, §4.2), deduped by relation-id (MC-T14), retracted included."
  [rk-rt address]
  (->> (get (rk/read-relations-for-targets rk-rt [address] [relation-kind] true) address)
       (map (juxt :relation-id identity)) (into {}) vals
       (filter machine-cut-edge?) vec))

(defn- materialized-to?
  "Barrier + CHECK (FALSIFY_A F4 fix, 2026-07-12 gate): await-relation returns
   the LAST value without throwing on deadline, so the awaited outcome must be
   verified — counts and the epoch decision use MATERIALIZED transitions only,
   never the plan."
  [rk-rt relation-id status]
  (= status
     (:relation-status
      (:row (rk/await-relation
              #(rk/read-relation-detail rk-rt relation-id)
              #(= status (:relation-status (:row %))))))))

(defn- apply-transitions!
  "Append the reconcile plan (asserts + retracts, transition-keyed — F2 fix)
   for ONE annotation/line and barrier-CHECK every targeted relation. Returns
   {:asserted <n materialized> :retracted <n materialized> :unmaterialized <n>}."
  [rk-rt plan asserted-at-ms]
  (doseq [e (:asserts plan)]
    (rk/append-relation-request!
      rk-rt (rk/assert-request {:kind (:kind e) :from (:from-ref e) :to (:to-ref e)
                                :asserter-actor-id machine-cut-actor-id
                                :asserter-type asserter-type
                                :asserted-at-ms (:asserted-at-ms e) :sent-at-ms (:asserted-at-ms e)
                                :request-id (:request-id e) :idempotency-key (:request-id e)
                                :evidence-source-id (:evidence-source-id e) :note (:note e)})))
  (doseq [row (:retracts plan)]
    (rk/append-relation-request!
      rk-rt (rk/retract-request {:kind (:relation-kind row) :from (:from row) :to (:to row)
                                 :asserter-actor-id machine-cut-actor-id :asserter-type asserter-type
                                 :actor {:actor/id machine-cut-actor-id :actor/type asserter-type}
                                 :asserted-at-ms asserted-at-ms :sent-at-ms asserted-at-ms
                                 :request-id (:mc/request-id row)
                                 :idempotency-key (:mc/request-id row)})))
  (let [a-ok (count (filter #(materialized-to? rk-rt (:relation-id %) :asserted)
                            (:asserts plan)))
        r-ok (count (filter #(materialized-to? rk-rt (:relation-id %) :retracted)
                            (:retracts plan)))
        unmat (- (+ (count (:asserts plan)) (count (:retracts plan)))
                 (+ a-ok r-ok))]
    {:asserted a-ok :retracted r-ok :unmaterialized unmat}))

(defn- reconcile-from-wal-line!
  "Window-scoped reconcile of ONE completed WAL line against current rk state
   (F1/F6 fix, 2026-07-12 gate): scope = the LINE's OWN annotated window (its
   pairs + unpaired event-uuids) — a line's replay must never retract edges
   outside the window it annotated (§5.4 applied to replay). Existing read
   includes retracted rows (transition-key incumbents). Zero adapter calls."
  [rk-rt line]
  (let [address   (:mc/address line)
        desired   (wal-line->edge-specs line)
        scope-ids (into #{} (map #(tid/chat-message-id address %))
                        (concat (mapcat (fn [p] (cons (:prompt p) (:responses p)))
                                        (:mc/pairs line))
                                (:mc/unpaired line)))
        existing  (->> (read-machine-cut-edges rk-rt address)
                       (filter #(in-window-scope? scope-ids %))
                       vec)
        plan      (reconcile-plan desired existing)
        ts        (or (get-in line [:mc/run :asserted-at-ms]) (core/now-ms))]
    (apply-transitions! rk-rt plan ts)))

(defn replay-wal!
  "Re-apply every completed WAL line's reconcile to the relation kernel, in FILE
   ORDER (CONTRACT §5.5, §9 G11). ZERO adapter calls — no LLM (structurally: this
   fn takes no adapter). Deterministic ids converge; the relation journal makes a
   double replay within one cluster a no-op; a fresh cluster reconstructs the
   edges exactly. Each completed line reconciles its edges against the current
   fresh-cluster state (so a later re-annotation line's move retracts the stale
   edge, reproducing the live state). Malformed/torn lines are counted + SKIPPED,
   never aborting the reduce (face-arsenal replay-wear-log! precedent; torn
   trailing line dropped, the rest replayed). Missing file → no-op. Returns
   {:asserted <n> :retracted <n> :lines <completed> :failed <torn>}."
  [{:keys [rk-rt wal-path] :as ctx}]
  (let [path (or wal-path (:wal-path ctx) (default-wal-path {}))]
    (reduce
      (fn [stats [line-idx line]]
        (if (str/blank? line)
          stats
          (try
            (let [entry (edn/read-string line)]
              (if (= :completed (:mc/status entry))
                (let [{:keys [asserted retracted unmaterialized]}
                      (reconcile-from-wal-line! rk-rt entry)]
                  (-> stats
                      (update :asserted + asserted)
                      (update :retracted + retracted)
                      (update :unmaterialized + unmaterialized)
                      (update :lines inc)))
                stats))                                    ; failed lines assert nothing
            (catch Exception e
              (binding [*out* *err*]
                (println "[MACHINE-CUT] replay: line" line-idx "failed, skipping:"
                         (.getMessage e)))
              (update stats :failed inc)))))
      {:asserted 0 :retracted 0 :unmaterialized 0 :lines 0 :failed 0}
      (map-indexed vector (read-wal-lines path)))))

(defn- read-wal-lines
  "Raw WAL lines (strings), [] when the file is absent."
  [path]
  (let [f (io/file path)]
    (if-not (.exists f)
      []
      (with-open [rdr (io/reader f :encoding "UTF-8")]
        (vec (line-seq rdr))))))

(defn- wal-line-for-run
  "The last COMPLETED WAL line whose run-id matches (F1 fix: the noop path
   verifies-and-converges from the run's own durable line). nil when absent."
  [path run-id]
  (->> (read-wal-lines path)
       (keep (fn [line]
               (when-not (str/blank? line)
                 (try (edn/read-string line) (catch Exception _ nil)))))
       (filter #(and (= :completed (:mc/status %))
                     (= run-id (get-in % [:mc/run :run-id]))))
       last))
