(ns app.server.episode
  "first-light A · P2 — the episode seam (CONTRACT §2: the ONE sanctioned new
   server ns). The genesis conversation is a NATIVE source whose material
   accretes turn by turn (CONTRACT §3): Sid's typed utterance lands in
   object-container as addressable material `asserted-by: sid` BEFORE any
   agent sees it, riding the EXISTING import family under the new `imp:ep:`
   prefix (§5.1 — the extract-object-key branch is the package's one
   sanctioned kernel edit); the resident agent is the existing CLI lane
   (subscription auth, NO API keys — hard rule); its responses become durable
   via post-turn harvest+distill of the episode's own jsonl (the proven
   transcript organ, T8-idempotent).

   NOT a Rama module: pure row builders + a foreign-client driver over the
   deployed object-container module — the block_distiller adapter shape.

   Identity design (the P2 adjudication of P0 flags C/D — receipts in the
   phase record):
   - conversation-id = `genesis-conversation-id`, a fixed UUID: passed to the
     CLI as --session-id on turn 1 and --resume after (no --fork-session, so
     the id and the jsonl file stay stable), and harvest derives conv-id from
     each jsonl line's OWN sessionId — so native utterances and distilled
     agent material share ONE object-key, one conversation container, one
     face address. Boot COMPUTES the address (the durable-ground
     default-address pattern); birth-blankness = the container being empty,
     never a state flag. A-scoped: first-light A's episode is a singleton by
     design (world-state-zero happens once); B/worlds mint real ids.
   - the utterance's projection rows use entry-kind :episode-utterance with
     `ep:`-namespaced order-keys — NEVER :message (river-page indexes the
     sb: class ledger into the ordered :message rows POSITIONALLY; a foreign
     :message row would shift every ledger order) and NEVER the distiller's
     sb: namespace. Surface ids reuse the routed `src:tr:` prefix with a
     disjoint hash input (the distiller's own N1b precedent) so no new
     source-id routing branch is needed.
   - post-turn distill SKIPS user-role plain-text river events (flag D): at
     genesis the utterance lane is the ONLY input surface (T9 bare ground),
     so every such event is already durable natively; each skipped event
     still gets its durable class-ledger row (entry-kind :native) through
     the distiller's class-hint path — deterministic keys, re-runs converge
     (T8/G4). Tool-result-bearing user events distill normally (they are the
     agent's turn material, T1 at event grain)."
  (:require [app.server.rama.core :as core]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.block-distiller :as bd]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.object-container.transcript-identity :as tid]
            [app.server.rama.dogfood.transcript :as transcript]
            [app.server.rama.util-fns :as util-fns]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; ===========================================================================
;; §A · Identity (pure)
;; ===========================================================================

(def genesis-conversation-id
  "The genesis episode's conversation-id AND the resident agent's CLI session
   uuid (minted at birth, 2026-07-17 — the mint is the constant; see ns doc)."
  "d7bed351-cae6-4ee0-869f-96b61924969e")

(def episode-source :claude-code)

(def episode-distiller-id
  "The native-utterance stratum. Id-disjoint from sense-block-v0 by the
   distiller segment in unit-ids (the block layer's G5/R3 rule)."
  "episode-native-v0")

(def episode-distiller-version 1)

(def utterance-actor-id "sid")

(defn episode-object-key
  "chat:<sha> for ANY episode conversation-id — deterministic; the ONE
   address native + distilled episode material shares."
  [conversation-id]
  (tid/transcript-object-key episode-source conversation-id))

(defn genesis-object-key
  "The genesis episode's address. Boot computes it and reads (never
   re-harvests). The genesis FIRST utterance is Sid's irreplaceable act
   (CONTRACT §11) — machinery drills (G3/G4) run against a DRILL episode
   via the :conversation-id override below, never against this key."
  []
  (episode-object-key genesis-conversation-id))

(defn utterance-import-key
  "imp:ep:<object-key>:<sha(turn-id)> — routed by the §5.1 extract-object-key
   branch to partition(object-key), where the import topology writes the
   completion (|hash *object-key). Deterministic on turn-id: an HTTP retry of
   the same turn re-mints NOTHING (journaled no-op)."
  [object-key turn-id]
  (str "imp:ep:" object-key ":" (core/sha-256 (str turn-id))))

(defn utterance-source-id
  "src:tr:<object-key>:<sha(\"episode-utterance \" turn-id)> — REUSES the
   routed src:tr: prefix (the distiller's N1b precedent: routing is what the
   prefix buys; the namespaced hash input keeps it collision-disjoint from
   line surfaces and per-part surfaces)."
  [object-key turn-id]
  (str "src:tr:" object-key ":" (core/sha-256 (str "episode-utterance " turn-id))))

(defn utterance-unit-id
  "du:<object-key>:episode-native-v0:ep:<sha8(turn-id)>:<block-index> — the
   ep: path segment keeps paths disjoint from sense-block-v0's numeric paths;
   the %06d tail preserves cut order within the utterance's one surface."
  [object-key turn-id block-index]
  (str "du:" object-key ":" episode-distiller-id ":ep:"
       (subs (core/sha-256 (str turn-id)) 0 8) ":"
       (format "%06d" (long block-index))))

(defn utterance-order-key
  "ep:<%020d time-ms>:<sha8(turn-id)> — sorts native rows by utterance time
   within the projection's ep: namespace; the sha tail disambiguates equal
   clocks. Namespace-disjoint from `%020d:` (:message) and `sb:%020d`
   (class ledger) co-tenants."
  [time-ms turn-id]
  (str "ep:" (format "%020d" (long time-ms)) ":"
       (subs (core/sha-256 (str turn-id)) 0 8)))

(defn utterance-request-id
  [object-key turn-id]
  (str "req:episode:" object-key ":" (core/sha-256 (str turn-id))))

(defn utterance-actor
  "asserted-by: sid — an honest :human actor CARRYING the import capability
   (authorized-request? checks capabilities for non-:system actors)."
  []
  {:actor/id utterance-actor-id
   :actor/type :human
   :actor/capabilities #{:object-container/import-material}})

;; ===========================================================================
;; §B · The utterance import (pure rows → one action-request)
;; ===========================================================================

(defn utterance-rows
  "Surface + units + anchors for ONE utterance, cut at the SAME grain as
   distilled human turns (bd/free-cut-part :human-message — whole-message
   block + proper structural silver subs), so the unit grammar never forks
   by lane (T1). created-by = sid (G3's provenance read lands here);
   :production-event is the delegation home (the F2-option-A extra key)."
  [{:keys [object-key turn-id text time-ms prev-turn-id]}]
  (let [source-id  (utterance-source-id object-key turn-id)
        source-ref (str "ep-utterance:" turn-id)
        text       (str text)
        text-hash  (oc/source-hash text)
        event-id   (str "evt:" object-key ":" (core/sha-256 (str "episode " turn-id)))
        doc-id     (tid/chat-message-id object-key (core/sha-256 (str turn-id)))
        blocks     (vec (map-indexed vector (bd/free-cut-part {:part-type :human-message :text text})))
        production {:production/class         :river
                    :production/classifier-id episode-distiller-id
                    :production/actor         utterance-actor-id
                    :production/actor-role    "user"
                    :context/parents          (when prev-turn-id [prev-turn-id])
                    :context/degenerate?      true
                    :production/time-ms       (long time-ms)}
        surface    (assoc (oc/->SourceArtifactRow
                           source-id source-ref text-hash :episode-utterance text
                           doc-id
                           (long (count (.getBytes text "UTF-8")))
                           (long time-ms) utterance-actor-id event-id)
                          :production-event production)
        units      (mapv (fn [[i b]]
                           (let [unit-id (utterance-unit-id object-key turn-id i)
                                 btext   (str (:text b))]
                             (oc/->DerivedUnitRow
                              unit-id doc-id source-id (:unit-kind b)
                              (str "ep:" (subs (core/sha-256 (str turn-id)) 0 8) ":"
                                   (format "%06d" (long i)))
                              nil
                              (oc/source-anchor-id unit-id)
                              btext (oc/source-hash btext)
                              episode-distiller-id episode-distiller-version event-id)))
                         blocks)
        anchors    (mapv (fn [[i b]]
                           (let [unit-id (utterance-unit-id object-key turn-id i)]
                             (oc/->SourceAnchorRow
                              (oc/source-anchor-id unit-id) :derived-unit unit-id
                              source-id source-ref text-hash
                              (long (:start-offset b)) (long (:end-offset b))
                              (str "ep:" (subs (core/sha-256 (str turn-id)) 0 8) ":"
                                   (format "%06d" (long i)))
                              event-id)))
                         blocks)]
    {:surface surface :units units :anchors anchors
     :event-id event-id :source-id source-id}))

(defn utterance-projection-hint
  "The native row in the conversation projection: entry-kind
   :episode-utterance, ep:-namespaced order-key, source-id set (the merge
   read walks source→units exactly like river-page's message rows), role =
   the ACTOR id (sid — this lane's rows carry actor identity, not a
   transport role; documented divergence from the :message rows)."
  [{:keys [object-key turn-id time-ms text]} imp-key request-id source-id event-id]
  (oc/->TranscriptConversationProjectionRow
   :transcript-conversation-projection
   (tid/chat-conversation-id object-key)
   (utterance-order-key time-ms turn-id)
   :episode-utterance
   nil nil nil source-id nil nil
   event-id request-id imp-key
   (str turn-id) utterance-actor-id
   (subs (str text) 0 (min 120 (count (str text))))
   nil))

(defn utterance-import-request
  "ONE :object-container/import-material action-request for ONE utterance.
   Deterministic request-id/import-key/fingerprint on (turn-id, text,
   time-ms) — the client mints turn-id + time-ms ONCE per Ctrl+Enter, so
   retries converge (T8's class). Mirrors bd/import-request."
  [{:keys [object-key turn-id text time-ms] :as args}]
  (let [{:keys [surface units anchors event-id source-id]} (utterance-rows args)
        imp-key     (utterance-import-key object-key turn-id)
        request-id  (utterance-request-id object-key turn-id)
        hint        (utterance-projection-hint args imp-key request-id source-id event-id)
        payload     {:object-key           object-key
                     :source-artifacts     [surface]
                     :object-containers    []
                     :revisions            []
                     :derived-units        units
                     :source-anchors       anchors
                     :composition-edges    []
                     :source-versions      []
                     :projection-hints     [hint]
                     :source-line-statuses []}
        fingerprint (oc/import-material-fingerprint object-key imp-key payload)]
    (assoc (core/action-request
            {:request-id   request-id
             :request-type :object-container/import-material
             :time-ms      (long time-ms)
             :actor        (utterance-actor)
             :target       {:target/kind :object-container-import
                            :target/id imp-key
                            :target/address {:object/key object-key}}
             :action       {:action/type :object-container/import-material
                            :action/capability :object-container/import-material
                            :action/params {:source/format :episode-utterance}}
             :routing/key  [:object-container/import object-key]
             :payload      payload
             :provenance   {:source/type :episode}})
           :partition/key      object-key
           :object/key         object-key
           :import/key         imp-key
           :idempotency/key    imp-key
           :material/fingerprint fingerprint)))

;; ===========================================================================
;; §C · Driver — utterance durability (foreign client; G3's first half)
;; ===========================================================================

(defn append-utterance!
  "Land ONE utterance durably: append + await the per-request decision (the
   distiller driver's barrier). Returns {:status :accepted/:rejected
   :address :unit-ids :import-key :decision}. The caller MUST see :accepted
   before the agent is summoned — the utterance is durable BEFORE any agent
   reads it (CONTRACT §3, G3)."
  [oc-rt {:keys [text turn-id time-ms prev-turn-id conversation-id]}]
  (let [object-key (episode-object-key (or conversation-id genesis-conversation-id))
        args {:object-key object-key
              :turn-id (str turn-id)
              :text (str text)
              :time-ms (long time-ms)
              :prev-turn-id prev-turn-id}
        req  (utterance-import-request args)]
    (ocr/append-object-container-request! oc-rt req)
    (let [decision (ocr/await-object-container-decision oc-rt req 20000)
          accepted? (= :accepted (:status decision))]
      {:status (if accepted? :accepted :rejected)
       :address object-key
       :import-key (:import/key req)
       :unit-ids (mapv :unit-id (get-in req [:payload :derived-units]))
       :decision decision})))

(defn read-utterance-rows
  "The episode's native rows (entry-kind :episode-utterance), ordered by
   ep: order-key — the merge lane's enumeration AND the G3 receipt read."
  [oc-rt object-key]
  (->> (ocr/read-transcript-conversation-projection
        oc-rt (tid/chat-conversation-id object-key) "" 100000)
       (filter #(= :episode-utterance (:entry-kind %)))
       (sort-by :order-key)
       vec))

;; ===========================================================================
;; §D · The resident agent (the existing CLI lane — argv, no keys)
;; ===========================================================================

(defn episode-jsonl-file
  "An episode session's transcript file for a given cwd (the CLI writes
   ~/.claude/projects/<cwd-slug>/<session-uuid>.jsonl)."
  ([cwd] (episode-jsonl-file cwd genesis-conversation-id))
  ([cwd conversation-id]
   (let [slug (str/replace (str cwd) #"[/.]" "-")]
     (io/file (str (System/getProperty "user.home")
                   "/.claude/projects/" slug "/" conversation-id ".jsonl")))))

(defn summon-argv
  "The resident agent's argv. Turn 1 opens the session AS the episode uuid
   (--session-id); later turns resume it (--resume, NO --fork-session — the
   id and jsonl stay stable, so harvest keys every line to the ONE episode
   conversation). Subscription CLI, zero keys (hard rule)."
  [{:keys [cwd prompt conversation-id]}]
  (let [conv-id (or conversation-id genesis-conversation-id)
        existing? (.exists ^java.io.File (episode-jsonl-file cwd conv-id))]
    (vec (concat ["claude"]
                 (if existing?
                   ["--resume" conv-id]
                   ["--session-id" conv-id])
                 ["-p" (str prompt)
                  "--output-format" "stream-json"
                  "--include-partial-messages"]))))

;; ===========================================================================
;; §E · Post-turn harvest + distill (flag F: the sanctioned turn-end trigger
;; — ONE file, ONE conversation, never boot, never a sweep)
;; ===========================================================================

(defn native-turn-event?
  "The flag-D skip predicate: a user-role event whose parts carry NO
   tool_use/tool_result — at genesis such an event is the CLI's echo of an
   utterance that is ALREADY durable under imp:ep: (the utterance lane is
   the only input surface, T9). Tool-result-bearing user events return
   false and distill normally."
  [parsed distilled]
  (and (= "user" (bd/message-role parsed))
       (not-any? #(contains? #{:tool-use :tool-result} (:part-type %))
                 (:parts distilled))))

(defn harvest-episode-increment!
  "One INCREMENTAL harvest pass over the episode's jsonl: stored file offset →
   read-complete-appended-lines → import → advance file-state. This is the
   watcher loop's poll body run ONCE — NEVER the migration-day offset-0
   harvest: a grown conversation's already-imported lines re-fingerprint
   DIFFERENTLY (their previous-message chain now resolves against the stored
   last message), so an offset-0 re-harvest fails honestly on line 0 with
   :source-line-completion-missing (found live at the G3/G4 drill, turn 2).
   The offset cursor is exactly what makes turn-end harvest converge."
  [oc-rt file]
  (let [req (transcript/transcript-request
             :transcript/harvest
             {:transcript/request-id (str "episode-turn:" (System/currentTimeMillis))
              :transcript/source episode-source
              :transcript/paths [(.getPath ^java.io.File file)]
              :time-ms 0})
        file-key (transcript/source-file-key episode-source (transcript/file-id file))
        stored   (ocr/read-transcript-file-offset oc-rt file-key)
        start    (long (or (:last-byte-offset stored) 0))
        {:keys [observations next-offset]}
        (transcript/read-complete-appended-lines req file start)
        observations (vec observations)]
    (if (empty? observations)
      {:status :complete :new-lines 0 :from-offset start}
      (let [import-result (transcript/import-observations-into-object-container!
                           oc-rt req observations {})]
        (if-not (= :accepted (:status import-result))
          {:status :failed :from-offset start :import import-result}
          (let [fs (transcript/append-and-await-object-container-file-state!
                    oc-rt req file (:source-lines import-result) next-offset)]
            {:status (if (= :accepted (:status fs)) :complete :failed)
             :new-lines (count observations)
             :from-offset start
             :next-offset next-offset
             :file-state (:status fs)}))))))

(defn post-turn-distill!
  "Turn end: incrementally harvest the episode's OWN jsonl (offset cursor —
   see harvest-episode-increment!), then distill the conversation with the
   flag-D skip (user text events → class-only :native rows). Bumps the
   ingest epoch so the worn face re-pulls durable truth (INV-19). Returns
   the distill summary merged with the harvest receipt."
  [oc-rt {:keys [cwd conversation-id]}]
  (let [conv-id (or conversation-id genesis-conversation-id)
        file (episode-jsonl-file cwd conv-id)]
    (if-not (.exists ^java.io.File file)
      {:status :no-transcript :file (.getPath ^java.io.File file)}
      (let [harvest (harvest-episode-increment! oc-rt file)]
        (if-not (= :complete (:status harvest))
          {:status :harvest-failed :harvest harvest}
          (let [summary (bd/distill-conversation!
                         {:oc-rt oc-rt
                          :source episode-source
                          :conversation-id conv-id
                          :skip-event? native-turn-event?})]
            (swap! util-fns/!ingest-epoch-atom inc)
            (assoc summary
                   :status :distilled
                   :harvest-new-lines (:new-lines harvest))))))))
