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
            [app.server.rama.material-circulation :as circulation]
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
   (authorized-request? checks capabilities for non-:system actors).

   matter-room P2 (CONTRACT §11, scoped parameterization): the 1-arity threads
   a CALLER-SUPPLIED actor through the SAME lane, so machine material (the
   room's residents) is born by the ONE existing import path instead of a
   second import family (T1/G5). `nil` — and every existing 0-arity call —
   yields the identical sid actor map, byte for byte."
  ([] (utterance-actor nil))
  ([actor]
   (or actor
       {:actor/id utterance-actor-id
        :actor/type :human
        :actor/capabilities #{:object-container/import-material}})))

;; ===========================================================================
;; §B · The utterance import (pure rows → one action-request)
;; ===========================================================================

(defn utterance-rows
  "Surface + units + anchors for ONE utterance, cut at the SAME grain as
   distilled human turns (bd/free-cut-part :human-message — whole-message
   block + proper structural silver subs), so the unit grammar never forks
   by lane (T1). created-by = sid (G3's provenance read lands here);
   :production-event is the delegation home (the F2-option-A extra key).

   matter-room P2 (CONTRACT §11, scoped): `:actor-id`, `:actor-role` and
   `:part-type` are OPTIONAL parameters whose defaults reproduce Sid's lane
   byte for byte. A room resident passes the machine actor and part-type
   `:material` — `free-cut-part`'s WHOLE-BLOCK cut (`:material-part`), never
   `:text`, which would fan one resident into N markdown blocks and break the
   one-unit edit lane. Known consequence, INTENDED: `:material-part` is in
   `face-projection/seed-noise-kinds`, so residents are elided from episode
   seeds — the resident's context rides the portal briefing (L4), never the
   conversational seed."
  [{:keys [object-key turn-id text time-ms prev-turn-id receipt
           actor-id actor-role part-type]}]
  (let [actor-id   (or actor-id utterance-actor-id)
        actor-role (or actor-role "user")
        part-type  (or part-type :human-message)
        source-id  (utterance-source-id object-key turn-id)
        source-ref (str "ep-utterance:" turn-id)
        text       (str text)
        text-hash  (oc/source-hash text)
        event-id   (str "evt:" object-key ":" (core/sha-256 (str "episode " turn-id)))
        doc-id     (tid/chat-message-id object-key (core/sha-256 (str turn-id)))
        blocks     (vec (map-indexed vector (bd/free-cut-part {:part-type part-type :text text})))
        production {:production/class         :river
                    :production/classifier-id episode-distiller-id
                    :production/actor         actor-id
                    :production/actor-role    actor-role
                    :context/parents          (when prev-turn-id [prev-turn-id])
                    :context/degenerate?      true
                    :production/time-ms       (long time-ms)}
        surface    (assoc (oc/->SourceArtifactRow
                           source-id source-ref text-hash :episode-utterance text
                           doc-id
                           (long (count (.getBytes text "UTF-8")))
                           (long time-ms) actor-id event-id)
                          :production-event production
                          :receipt receipt)
        units      (mapv (fn [[i b]]
                           (let [unit-id (utterance-unit-id object-key turn-id i)
                                 btext   (str (:text b))]
                             (assoc
                              (oc/->DerivedUnitRow
                               unit-id doc-id source-id (:unit-kind b)
                               (str "ep:" (subs (core/sha-256 (str turn-id)) 0 8) ":"
                                    (format "%06d" (long i)))
                               nil
                               (oc/source-anchor-id unit-id)
                               btext (oc/source-hash btext)
                               episode-distiller-id episode-distiller-version event-id)
                              :receipt receipt)))
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
   transport role; documented divergence from the :message rows).

   matter-room P2: the `role` slot is the ONE field the render's machine
   classification actually reads (`ground.cljs` `machine? (not= speaker
   \"sid\")` ← `face_projection.clj` turn `:speaker` ← `block_distiller.clj`
   `:actor (:role row)` ← here). Parameterizing the actor WITHOUT this slot
   would land a machine resident that renders as Sid — R2 finding 2. Default
   (`nil`) is `utterance-actor-id`, byte-identical to the pre-P2 row."
  [{:keys [object-key turn-id time-ms text receipt origin-unit-ids actor-id]}
   imp-key request-id source-id event-id]
  (assoc
   (oc/->TranscriptConversationProjectionRow
    :transcript-conversation-projection
    (tid/chat-conversation-id object-key)
    (utterance-order-key time-ms turn-id)
    :episode-utterance
    nil nil nil source-id nil nil
    event-id request-id imp-key
    (str turn-id) (or actor-id utterance-actor-id)
    (subs (str text) 0 (min 120 (count (str text))))
    nil)
   :receipt receipt
   :origin-unit-ids origin-unit-ids))

;; ===========================================================================
;; §B2 · Geometry cells + turn records (P2b — settled truth as projection
;; cells; receipt: P2B.md §receipt-a)
;;
;; A settled cell = ONE :transcript-conversation-projection hint at a
;; DETERMINISTIC order-key — the import topology's hint write is an upsert
;; keyed (conversation-container-id, order-key), so each settle overwrites
;; the cell in place; last acknowledged settle wins. Rides hint-only imports
;; (the F3 ruling) under the EXISTING imp:ep: shape (routing is shape-based —
;; zero kernel edits). geo:/ep-turn: order-keys are namespace-disjoint from
;; every co-tenant and invisible to river-page/read-utterance-rows (positive
;; entry-kind filters). The cell value rides an extra assoc'd :geometry /
;; :turn key (round-trip proven live) with :content-preview carrying
;; (pr-str value) as the fingerprinted belt.
;;
;; Placement identity is world-scoped (§9.3): for A the genesis world IS the
;; episode container, so world-id = the episode object-key; B/worlds mint
;; real world-ids. A unit never means one position in all Softlands.
;; ===========================================================================

(defn world-id
  "The world a geometry cell belongs to. A-scoped: the episode object-key."
  [object-key]
  object-key)

(defn geometry-order-key
  "geo:unit:<sha8(unit-id)> — ONE settled cell per unit (upsert-in-place)."
  [unit-id]
  (str "geo:unit:" (subs (core/sha-256 (str unit-id)) 0 8)))

(def camera-order-key "geo:camera")

(defn geometry-cell-hint
  "ONE settled-cell hint row. `value` is the whole world-scoped cell value
   ({:world-id :unit-id :x :y} or camera {:world-id :x :y :zoom})."
  [object-key order-key entry-kind value settle-id imp-key request-id]
  (let [event-id (str "evt:" object-key ":"
                      (core/sha-256 (str "geo " settle-id " " order-key)))]
    (assoc (oc/->TranscriptConversationProjectionRow
            :transcript-conversation-projection
            (tid/chat-conversation-id object-key)
            order-key entry-kind
            nil nil nil nil nil nil
            event-id request-id imp-key
            nil nil
            (pr-str value)
            nil)
           :geometry value)))

(defn geometry-settle-request
  "ONE hint-only import carrying a settle write: per-unit position cells +
   (optionally) the camera cell, all in one acked barrier. settle-id is
   CLIENT-minted once per gesture-end (the turn-id precedent): a retry
   re-derives identical identity and journals a no-op; a REUSED settle-id
   with different geometry fingerprint-conflicts into a durable rejection
   (the G4b forced-stale drill's mechanism)."
  [{:keys [object-key cells camera settle-id time-ms]}]
  (let [imp-key    (str "imp:ep:" object-key ":"
                        (core/sha-256 (str "geometry-settle " settle-id)))
        request-id (str "req:episode-geo:" object-key ":"
                        (core/sha-256 (str settle-id)))
        wid        (world-id object-key)
        cell-hints (mapv (fn [{:keys [unit-id x y deleted?]}]
                           (geometry-cell-hint
                            object-key (geometry-order-key unit-id)
                            :episode-geometry
                            (cond-> {:world-id wid :unit-id unit-id
                                     :x (double x) :y (double y)}
                              ;; Task 18: a tombstone cell — the serve drops
                              ;; the unit from the page (upsert-in-place: the
                              ;; LAST settle wins, so un-delete = re-settle)
                              deleted? (assoc :deleted? true))
                            settle-id imp-key request-id))
                         cells)
        cam-hint   (when camera
                     (geometry-cell-hint
                      object-key camera-order-key :episode-camera
                      {:world-id wid
                       :x (double (:x camera)) :y (double (:y camera))
                       :zoom (double (or (:zoom camera) 1.0))}
                      settle-id imp-key request-id))
        hints      (cond-> cell-hints cam-hint (conj cam-hint))
        payload    {:object-key           object-key
                    :source-artifacts     []
                    :object-containers    []
                    :revisions            []
                    :derived-units        []
                    :source-anchors       []
                    :composition-edges    []
                    :source-versions      []
                    :projection-hints     hints
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
                            :action/params {:source/format :episode-geometry}}
             :routing/key  [:object-container/import object-key]
             :payload      payload
             :provenance   {:source/type :episode}})
           :partition/key      object-key
           :object/key         object-key
           :import/key         imp-key
           :idempotency/key    imp-key
           :material/fingerprint fingerprint)))

(defn settle-geometry!
  "Land ONE settle write (append+await — the ack IS the acknowledged settle,
   the safety mechanism; the exit flush is only a belt)."
  [oc-rt {:keys [conversation-id] :as args}]
  (let [object-key (episode-object-key (or conversation-id genesis-conversation-id))
        req (geometry-settle-request (assoc args :object-key object-key))]
    (ocr/append-object-container-request! oc-rt req)
    (let [decision (ocr/await-object-container-decision oc-rt req 20000)]
      {:status (if (= :accepted (:status decision)) :accepted :rejected)
       :address object-key
       :decision decision})))

(defn read-geometry-cells
  "{unit-id → cell-value} + :camera — the boot-restore read (also served
   through the face pull; this is the receipt/drill form)."
  [oc-rt object-key]
  (let [rows (->> (ocr/read-transcript-conversation-projection
                   oc-rt (tid/chat-conversation-id object-key) "" 100000)
                  (filter #(contains? #{:episode-geometry :episode-camera}
                                      (:entry-kind %))))]
    {:cells  (into {} (keep (fn [r]
                              (when (= :episode-geometry (:entry-kind r))
                                (when-let [g (:geometry r)]
                                  [(:unit-id g) g]))))
                   rows)
     :camera (some #(when (= :episode-camera (:entry-kind %)) (:geometry %)) rows)}))

;; ===========================================================================
;; §B3 · The instance-master REGISTRY INDEX (editable-material P6 · R2/T7)
;;
;; An index, never a truth owner. The TRUTH that a subject deviates is the
;; existence of its instance-master container; this row exists only so the
;; serve and the blast-radius projection never have to enumerate blind. T7 is
;; the trap it is written against: `a missed upsert silently unhosts a
;; deviation`. It cannot, because nothing resolves wear through this row —
;; drop every row and the deviations still wear correctly, they are merely
;; slower to find. G13 drops them and rebuilds byte-equal.
;;
;; Rides the SAME hint-only import lane as the geometry cells: one upsert at a
;; deterministic order-key in a namespace (`fmi:`) disjoint from `geo:` and
;; `ep-turn:`, under the existing `imp:ep:` shape. Zero kernel edits.
;; ===========================================================================

(def instance-registry-entry-kind :facet-instance-registry)

(defn instance-registry-order-key
  "fmi:<facet>:<sha8(subject-uid)> — ONE cell per (facet, subject), upserted in
   place, so re-registering the same deviation is a no-op rather than a
   duplicate."
  [facet subject-uid]
  (str "fmi:" (name facet) ":"
       (subs (core/sha-256 (str subject-uid)) 0 8)))

(defn instance-registry-request
  "ONE hint-only import registering (or re-registering) instance masters.
   `entries` are {:facet :subject :instance-master-id :parent-id}."
  [{:keys [object-key entries time-ms]}]
  (let [entries (vec entries)
        digest (core/sha-256 (pr-str (mapv (juxt :facet :subject
                                                 :instance-master-id)
                                           entries)))
        imp-key (str "imp:ep:" object-key ":"
                     (core/sha-256 (str "fm-instance-registry " digest)))
        request-id (str "req:episode-fmi:" object-key ":" (subs digest 0 32))
        wid (world-id object-key)
        hints (mapv
               (fn [{:keys [facet subject instance-master-id parent-id]}]
                 (geometry-cell-hint
                  object-key
                  (instance-registry-order-key facet subject)
                  instance-registry-entry-kind
                  {:world-id wid
                   :facet facet
                   :subject subject
                   :instance-master-id instance-master-id
                   :parent-id parent-id}
                  digest imp-key request-id))
               entries)
        payload {:object-key object-key
                 :source-artifacts []
                 :object-containers []
                 :revisions []
                 :derived-units []
                 :source-anchors []
                 :composition-edges []
                 :source-versions []
                 :projection-hints hints
                 :source-line-statuses []}
        fingerprint (oc/import-material-fingerprint object-key imp-key payload)]
    (assoc (core/action-request
            {:request-id request-id
             :request-type :object-container/import-material
             :time-ms (long (or time-ms (core/now-ms)))
             :actor (utterance-actor)
             :target {:target/kind :object-container-import
                      :target/id imp-key
                      :target/address {:object/key object-key}}
             :action {:action/type :object-container/import-material
                      :action/capability :object-container/import-material
                      :action/params {:source/format :facet-instance-registry}}
             :routing/key [:object-container/import object-key]
             :payload payload
             :provenance {:source/type :episode}})
           :partition/key object-key
           :object/key object-key
           :import/key imp-key
           :idempotency/key imp-key
           :material/fingerprint fingerprint)))

(defn register-instance-masters!
  "Upsert index rows. Best-effort by design: this is an index, and a failure
   here must never fail the deviation that has already landed."
  [oc-rt {:keys [conversation-id entries time-ms]}]
  (if (empty? entries)
    {:status :noop :entries 0}
    (let [object-key (episode-object-key
                      (or conversation-id genesis-conversation-id))
          req (instance-registry-request
               {:object-key object-key :entries entries :time-ms time-ms})]
      (ocr/append-object-container-request! oc-rt req)
      (let [decision (ocr/await-object-container-decision oc-rt req 20000)]
        {:status (if (= :accepted (:status decision)) :accepted :rejected)
         :address object-key
         :entries (count entries)
         :decision decision}))))

(defn read-instance-registry
  "[{:facet :subject :instance-master-id :parent-id} …], deterministically
   ordered. An INDEX read — callers that need certainty read container
   existence (that is the truth), and G13 proves the two agree."
  [oc-rt object-key]
  (->> (ocr/read-transcript-conversation-projection
        oc-rt (tid/chat-conversation-id object-key) "" 100000)
       (filter #(= instance-registry-entry-kind (:entry-kind %)))
       (keep :geometry)
       (sort-by (juxt (comp str :facet) (comp str :subject)))
       vec))

(defn turn-order-key
  "ep-turn:<%020d time>:<sha8(turn-id)> — ONE cell per turn; status updates
   overwrite it (open → complete/failed). Disjoint from every co-tenant."
  [time-ms turn-id]
  (str "ep-turn:" (format "%020d" (long time-ms)) ":"
       (subs (core/sha-256 (str turn-id)) 0 8)))

(defn turn-record-request
  "The revision-pinned turn record (P2b addressing): source-block-id + the
   pinned content (text + kernel source-hash) + send-time position + status.
   Durable BEFORE the agent is spawned; later edits/moves never rewrite what
   the resident answered — the pin lives in this cell. Each STATUS mints its
   own import-key (same-status retries converge; different statuses overwrite
   the one cell). :thread-id names the conversation lane the turn ran on
   (one canvas, many conversations): the per-thread CLI session uuid, nil =
   the genesis thread. The cell is the canvas's thread registry — the serve
   merge discovers thread containers from these values. :episode-id names the
   CLI session the turn actually rode (D-core): nil/= lane-id for the lane's
   first episode; a successor uuid after a boundary — the cells are the
   durable episode chain the serve weaves successor containers from."
  [{:keys [object-key turn-id source-unit-id content-text position
           time-ms prev-turn-id status thread-id episode-id scene-context]}]
  (let [imp-key    (str "imp:ep:" object-key ":"
                        (core/sha-256 (str "turn-record " turn-id " " (name status))))
        request-id (str "req:episode-turn:" object-key ":"
                        (core/sha-256 (str turn-id " " (name status))))
        receipt    (circulation/receipt-from-context
                    {:created-during
                     {:conversation/address object-key
                      :episode/id (some-> episode-id str)
                      :turn/id (str turn-id)}
                     :captured-at-ms
                     (or (:receipt/captured-at-ms scene-context) time-ms)
                     :position position
                     :scene-context scene-context})
        value      {:world-id       (world-id object-key)
                    :turn-id        (str turn-id)
                    :source-unit-id source-unit-id
                    :content-text   (str content-text)
                    :content-hash   (oc/source-hash (str content-text))
                    :position       position
                    :status         status
                    :time-ms        (long time-ms)
                    :prev-turn-id   prev-turn-id
                    :thread-id      (some-> thread-id str)
                    :episode-id     (some-> episode-id str)
                    :receipt        receipt}
        event-id   (str "evt:" object-key ":"
                        (core/sha-256 (str "turn " turn-id " " (name status))))
        hint       (assoc (oc/->TranscriptConversationProjectionRow
                           :transcript-conversation-projection
                           (tid/chat-conversation-id object-key)
                           (turn-order-key time-ms turn-id)
                           :episode-turn
                           nil nil nil nil nil nil
                           event-id request-id imp-key
                           (str turn-id) utterance-actor-id
                           ;; fingerprinted pin: hash + status + source + pos
                           ;; + thread (full text rides :turn; preview stays
                           ;; bounded — and the belt must not lose the lane)
                           (pr-str (select-keys value
                                                [:turn-id :source-unit-id
                                                 :content-hash :position
                                                 :status :prev-turn-id
                                                 :thread-id :episode-id
                                                 :receipt]))
                           nil)
                          :turn value)
        payload    {:object-key           object-key
                    :source-artifacts     []
                    :object-containers    []
                    :revisions            []
                    :derived-units        []
                    :source-anchors       []
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
                            :action/params {:source/format :episode-turn}}
             :routing/key  [:object-container/import object-key]
             :payload      payload
             :provenance   {:source/type :episode}})
           :partition/key      object-key
           :object/key         object-key
           :import/key         imp-key
           :idempotency/key    imp-key
           :material/fingerprint fingerprint)))

(defn record-turn!
  "Land ONE turn-record status write (append+await). :open lands BEFORE the
   agent spawns (durable-BEFORE-agent, the existing lane's law); :complete/
   :failed/:timeout overwrite the same cell at turn end. An abrupt JVM death
   between the two leaves :open — the honest open fact G4b demands."
  [oc-rt {:keys [conversation-id] :as args}]
  (let [object-key (episode-object-key (or conversation-id genesis-conversation-id))
        req (turn-record-request (assoc args :object-key object-key))]
    (ocr/append-object-container-request! oc-rt req)
    (let [decision (ocr/await-object-container-decision oc-rt req 20000)]
      {:status (if (= :accepted (:status decision)) :accepted :rejected)
       :address object-key
       :import-key (:import/key req)
       :receipt (get-in req [:payload :projection-hints 0 :turn :receipt])
       :decision decision})))

(defn read-turn-records
  "Turn cells in time order (the drill/receipt read; the serve path threads
   the same rows through the face pull)."
  [oc-rt object-key]
  (->> (ocr/read-transcript-conversation-projection
        oc-rt (tid/chat-conversation-id object-key) "" 100000)
       (filter #(= :episode-turn (:entry-kind %)))
       (sort-by :order-key)
       (keep :turn)
       vec))

(defn read-receipt-records
  "All durable receipt carriers for one conversation, normalized to
   {:origin-unit-id :receipt :receipt/act}. Birth and utterance-time records
   remain distinct facts; neither is interpreted as aboutness."
  [oc-rt object-key]
  (let [rows
        (ocr/read-transcript-conversation-projection
         oc-rt (tid/chat-conversation-id object-key) "" 100000)
        births
        (for [row rows
              :when (= :episode-utterance (:entry-kind row))
              origin (:origin-unit-ids row)
              :when (:receipt row)]
          {:origin-unit-id origin
           :receipt/act :birth
           :receipt (:receipt row)})
        turns
        (for [row rows
              :when (= :episode-turn (:entry-kind row))
              :let [turn (:turn row)]
              :when (and (:source-unit-id turn) (:receipt turn))]
          {:origin-unit-id (:source-unit-id turn)
           :receipt/act :utterance
           :receipt (:receipt turn)})]
    (vec (concat births turns))))

(defn read-birth-receipt
  "The durable birth receipt for one unit, or nil for pre-P4 material."
  [oc-rt object-key unit-id]
  (some (fn [record]
          (when (and (= unit-id (:origin-unit-id record))
                     (= :birth (:receipt/act record)))
            (:receipt record)))
        (read-receipt-records oc-rt object-key)))

(defn utterance-import-request
  "ONE :object-container/import-material action-request for ONE utterance.
   Deterministic request-id/import-key/fingerprint on (turn-id, text,
   time-ms) — the client mints turn-id + time-ms ONCE per Ctrl+Enter, so
   retries converge (T8's class). Mirrors bd/import-request.
   P2b: `turn-id` is the BIRTH id — the client-minted block id whose first
   content act mints this unit; `:position` (optional) adds the block's
   birth-position geometry cell to the SAME payload, so birth + placement
   land in one acked import (birth-position at mint, §9.3).

   matter-room P2: `:actor` (a full actor map), `:actor-id`, `:actor-role`
   and `:part-type` are OPTIONAL and thread through `utterance-rows` +
   `utterance-projection-hint` unchanged. Omitting them reproduces Sid's
   request byte for byte — this is the ONE import path, parameterized, never
   a second import family (G5)."
  [{:keys [object-key turn-id time-ms position scene-context actor] :as args}]
  (let [receipt      (circulation/receipt-from-context
                      {:created-during
                       {:conversation/address object-key
                        :birth/id (str turn-id)}
                       :captured-at-ms
                       (or (:receipt/captured-at-ms scene-context) time-ms)
                       :position position
                       :scene-context scene-context})
        args         (assoc args :receipt receipt)
        {:keys [surface units anchors event-id source-id]} (utterance-rows args)
        imp-key     (utterance-import-key object-key turn-id)
        request-id  (utterance-request-id object-key turn-id)
        hint        (utterance-projection-hint
                     (assoc args :origin-unit-ids (mapv :unit-id units))
                     imp-key request-id source-id event-id)
        geo-hints   (when position
                      (mapv (fn [u]
                              (geometry-cell-hint
                               object-key (geometry-order-key (:unit-id u))
                               :episode-geometry
                               {:world-id (world-id object-key)
                                :unit-id (:unit-id u)
                                :x (double (:x position))
                                :y (double (:y position))}
                               (str "birth " turn-id) imp-key request-id))
                            units))
        payload     {:object-key           object-key
                     :source-artifacts     [surface]
                     :object-containers    []
                     :revisions            []
                     :derived-units        units
                     :source-anchors       anchors
                     :composition-edges    []
                     :source-versions      []
                     :projection-hints     (into [hint] geo-hints)
                     :source-line-statuses []}
        fingerprint (oc/import-material-fingerprint object-key imp-key payload)]
    (assoc (core/action-request
            {:request-id   request-id
             :request-type :object-container/import-material
             :time-ms      (long time-ms)
             :actor        (utterance-actor actor)
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
  [oc-rt {:keys [text turn-id time-ms prev-turn-id conversation-id position
                 scene-context]}]
  (let [object-key (episode-object-key (or conversation-id genesis-conversation-id))
        args {:object-key object-key
              :turn-id (str turn-id)
              :text (str text)
              :time-ms (long time-ms)
              :prev-turn-id prev-turn-id
              :position position
              :scene-context scene-context}
        req  (utterance-import-request args)]
    (ocr/append-object-container-request! oc-rt req)
    (let [decision (ocr/await-object-container-decision oc-rt req 20000)
          accepted? (= :accepted (:status decision))]
      {:status (if accepted? :accepted :rejected)
       :address object-key
       :import-key (:import/key req)
       :unit-ids (mapv :unit-id (get-in req [:payload :derived-units]))
       ;; P2b: the birth ack hands the client the envelope identity it needs
       ;; for the FIRST replayed edits (BW-T7 — the client computes nothing;
       ;; the served pull is not back yet at replay time)
       :document-container-id (:document-container-id
                               (first (get-in req [:payload :derived-units])))
       :receipt (get-in req [:payload :projection-hints 0 :receipt])
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
;; §C2 · The episode chain (D-core, Sid 2026-07-22) — bounded CLI sessions
;;
;; A LANE (the genesis column, or one thread's column) is permanent; the CLI
;; session serving it is not. Turns ride the lane's CURRENT episode while the
;; lane stays warm; silence past the boundary closes that episode FOREVER —
;; no cross-boundary --resume exists anywhere in the system (the re-harvest
;; duplication class dies structurally). The next turn opens a FRESH CLI
;; session seeded with the lane's prose thread (identity B: containers stay
;; keyed by the CLI sessionId — the P2 adjudication untouched; the chain is
;; woven from turn cells' :episode-id by the serve merge).
;; ===========================================================================

(declare episode-jsonl-file)

(def episode-idle-ms
  "The boundary (Sid: default): a lane silent this long closes its episode.
   Aligned with the 1h prompt-cache TTL — past it the old session's context
   re-reads cold anyway, so the cut is the cheap place."
  (* 60 60 1000))

(defonce ^:private !episode-chains
  ;; runtime currency ONLY — {lane-id {:episode-id str :last-turn-ms long}}.
  ;; The durable chain is the turn cells' :episode-id; a JVM restart re-adopts
  ;; from those (current-episode!'s fallback read).
  (atom {}))

(defn decide-episode
  "PURE boundary rule. `entry` is the runtime cell {:episode-id :last-turn-ms};
   `last-turn` the lane's newest durable turn cell (restart adoption); `mint-id`
   a thunk minting a fresh session uuid. Returns {:episode-id :fresh? :seed?}:
   fresh? = spawn with --session-id (else --resume); seed? = a predecessor
   exists, inherit its prose."
  [{:keys [lane-id entry last-turn now-ms idle-ms mint-id]}]
  (let [idle (long (or idle-ms episode-idle-ms))
        cur  (or entry
                 (when last-turn
                   {:episode-id   (or (:episode-id last-turn) lane-id)
                    :last-turn-ms (long (or (:time-ms last-turn) 0))}))]
    (cond
      (nil? cur)
      {:episode-id lane-id :fresh? true :seed? false}

      (< (- (long now-ms) (long (:last-turn-ms cur))) idle)
      {:episode-id (:episode-id cur) :fresh? false :seed? false}

      :else
      {:episode-id (mint-id) :fresh? true :seed? true})))

(defn current-episode!
  "The lane's episode for a turn arriving now (stateful shell over
   decide-episode). Fallback order: runtime cell → the lane's durable turn
   cells (JVM restart adopts a still-warm episode) → file-existence belt (a
   pre-chain lane whose cells never carried an episode resumes its file
   rather than minting over it). Total: any read failure degrades to the
   virgin-lane decision."
  [oc-rt {:keys [conversation-id thread-id now-ms cwd]}]
  (let [conv-id (or conversation-id genesis-conversation-id)
        lane-id (or (some-> thread-id str not-empty) conv-id)
        entry   (get @!episode-chains lane-id)
        last-turn
        (when (nil? entry)
          (try
            (->> (read-turn-records oc-rt (episode-object-key conv-id))
                 (filter #(= (some-> thread-id str not-empty) (:thread-id %)))
                 (sort-by #(:time-ms % 0))
                 last)
            (catch Exception _ nil)))
        d (decide-episode {:lane-id lane-id :entry entry :last-turn last-turn
                           :now-ms now-ms
                           :mint-id #(str (java.util.UUID/randomUUID))})]
    (if (and (:fresh? d) (= (:episode-id d) lane-id)
             (.exists ^java.io.File (episode-jsonl-file cwd lane-id)))
      {:episode-id lane-id :fresh? false :seed? false}
      d)))

(defn note-episode-turn!
  "Stamp the lane's runtime cell at spawn time — the warmth the NEXT turn's
   decide-episode reads."
  [thread-id conversation-id episode-id now-ms]
  (let [lane-id (or (some-> thread-id str not-empty)
                    (or conversation-id genesis-conversation-id))]
    (swap! !episode-chains assoc lane-id
           {:episode-id (str episode-id) :last-turn-ms (long now-ms)})))

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
  "The resident agent's argv. The episode CHAIN decides the session (D-core):
   a fresh episode opens AS its minted uuid (--session-id, seeded prompt);
   a warm episode resumes WITHIN its boundary (--resume appends to the SAME
   jsonl — docs-verified — so the offset-cursor harvest stays sound; no
   cross-boundary resume exists). Subscription CLI, zero keys (hard rule)."
  [{:keys [prompt session-id fresh?]}]
  (vec (concat ["claude"]
               (if fresh?
                 ["--session-id" (str session-id)]
                 ["--resume" (str session-id)])
               ["-p" (str prompt)
                "--output-format" "stream-json"
                "--include-partial-messages"])))

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
