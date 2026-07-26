(ns app.server.rama.material-circulation
  "Editable-material P4 circulation, as a plain foreign-client driver over the
   EXISTING object-container, relation-kernel, and llm-module APIs.

   This namespace declares no module, depot, topology, or PState. Durable truth
   remains split at the already-ratified owners:

   - mechanical receipts and calibrated machine-run records are ordinary OC
     projection hints;
   - semantic associations are relation-kernel edges;
   - LLM intent/claim/observation lifecycle stays in the existing llm-module.

   The epistemic ladder is deliberately orthogonal to relation meaning:
   receipts are co-presence only, :felt-at/:instance-of proposals are silver,
   and a Sid-authored :references assertion is gold. In particular, this file
   does not mint a Wish kind (binding FLAG-A ruling, 2026-07-24)."
  (:require [app.shared.activation-event :as activation-event]
            [app.server.rama.core :as core]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.facet-master :as facet-master]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.relation-kernel :as rk]
            [app.server.rama.dogfood.llm :as llm]
            [app.server.rama.util-fns :as util-fns]
            [app.shared.facet-masters :as facet-masters]
            [clojure.data.json :as json]
            [clojure.string :as str]))

;; ===========================================================================
;; Vocabulary and custody
;; ===========================================================================

(def gold-edge-kind
  "The binding FLAG-A ruling: wish-ness remains in the material/projection;
   :references is only the directed wish-unit → target coupling."
  :references)

(def about-edge-kind :felt-at)
(def kind-edge-kind :instance-of)

(def gold-actor-id "sid")
(def autotag-version "v1")
(def autotag-actor-id (str "llm:material-autotag/" autotag-version))
(def starter-culture-actor-id "llm:starter-culture/v1")

(def task-kind-id "kind/task")
(def feedback-kind-id "kind/feedback")

(def ^:private material-reference-keys
  "The generic P3 contribution-stamp identity retained by P4 receipts.
   Older receipt rows with only master/revision/site remain readable."
  [:material/subject
   :material/attachment
   :material/master
   :material/revision
   :material/site
   :material/role
   :material/slot])

(def default-material-policy-paths
  "The current code floor whose changes require a material activation event.

   P6 grows the set, as P4 said it would. By P3 the worn five had joined
   provenance on the generic facet-master layer, and by P5 three of them
   carried the interaction rows too — so each of these files now holds policy
   that is SUPPOSED to move by activation. A commit that changes one without a
   following activation event is the escape this detector exists to catch:
   policy that changed in code and never entered the record.

   Not included, deliberately: `binding_material.cljc` and `verb_registry.cljc`.
   Those are the KERNEL — the dispatch law and the closed verb vocabulary — and
   the code floor is where they are supposed to live. Adding them would report
   every kernel change as an escape and teach the reader to ignore the alarm."
  #{"src/app/shared/provenance_material.cljc"
    "src/app/shared/attention_material.cljc"
    "src/app/shared/foldable_material.cljc"
    "src/app/shared/positioned_material.cljc"
    "src/app/shared/space_material.cljc"
    "src/app/shared/threaded_material.cljc"
    "src/app/shared/text_body_material.cljc"})

(defn silver-edge?
  "Machine/import custody makes a semantic edge silver; relation kind does not."
  [row]
  (contains? #{:llm :agent :import :machine} (:asserter-type row)))

(defn gold-edge?
  "Gold is narrow human custody over the binding wish edge. A human-authored
   :felt-at edge would still not become a wish."
  [row]
  (and (= gold-edge-kind (:relation-kind row))
       (= :human (:asserter-type row))))

;; ===========================================================================
;; Mechanical receipts — co-presence, never aboutness
;; ===========================================================================

(defn receipt-from-context
  "Build one durable receipt from a client scene context. The context carries
   only identities, transforms, placement, and material revision references;
   no rendered material bytes are copied.

   `created-during` is supplied server-side (conversation/episode/turn or
   conversation/birth identity). `captured-at-ms` is the act's own client stamp.
   A nil/missed pick is still an honest receipt with placement + visible-count."
  [{:keys [created-during captured-at-ms position scene-context]}]
  (let [picked-at (or (:receipt/picked-at scene-context)
                      (when (or (:address scene-context)
                                (:src-path scene-context)
                                (:vi scene-context))
                        {:address (:address scene-context)
                         :src-path (:src-path scene-context)
                         :view-instance (:vi scene-context)}))
        placement (merge
                   {:world-id (:conversation/address created-during)}
                   (when (map? position)
                     {:x (double (:x position))
                      :y (double (:y position))})
                   (:receipt/placement scene-context))
        worn (->> (or (:receipt/worn-materials scene-context) [])
                  (keep (fn [m]
                          (when (and (string? (:material/master m))
                                     (string? (:material/revision m)))
                            (select-keys m material-reference-keys))))
                  distinct
                  (sort-by (juxt :material/master :material/revision
                                 (comp pr-str :material/site)
                                 (comp pr-str :material/role)
                                 (comp pr-str :material/slot)
                                 (comp pr-str :material/attachment)
                                 :material/subject))
                  vec)]
    {:receipt/version 1
     :receipt/captured-at-ms (long (or captured-at-ms 0))
     :receipt/created-during created-during
     :receipt/picked-at
     (merge {:address nil :src-path nil :view-instance nil}
            picked-at)
     :receipt/placement placement
     :receipt/worn-materials worn
     :receipt/visible-addresses
     (->> (:visible scene-context)
          (filter rk/present-string?)
          sort
          vec)
     :receipt/visible-count (long (or (:visible-count scene-context) 0))}))

(defn receipt-target
  "The precise address mechanically picked for this receipt, if any."
  [receipt]
  (get-in receipt [:receipt/picked-at :address]))

(defn select-gold-receipt
  "Prefer the utterance-time point when it names a material other than the wish
   unit; otherwise use the birth point. This makes the ordinary no-ritual path
   work: point with the mouse, keep the wish block focused, Ctrl+Enter."
  [source-unit-id utterance-receipt birth-receipt]
  (some (fn [receipt]
          (let [target (receipt-target receipt)]
            (when (and (rk/present-string? target)
                       (not= source-unit-id target))
              receipt)))
        [utterance-receipt birth-receipt]))

;; ===========================================================================
;; Relation writes — deterministic identity + checked materialization
;; ===========================================================================

(defn- materialized-to?
  [rk-rt relation-id status]
  (= status
     (get-in
      (rk/await-relation
       #(rk/read-relation-detail rk-rt relation-id)
       #(= status (get-in % [:row :relation-status]))
       5000)
      [:row :relation-status])))

(defn- append-asserted-edge!
  [rk-rt {:keys [kind from to actor-id actor-type asserted-at-ms
                 evidence-source-id evidence-anchor-id note request-prefix]}]
  (let [relation-id (rk/relation-id-for kind from to actor-id)
        request-id (str request-prefix relation-id)
        request (rk/assert-request
                 {:kind kind
                  :from from
                  :to to
                  :asserter-actor-id actor-id
                  :asserter-type actor-type
                  :actor {:actor/id actor-id :actor/type actor-type}
                  :asserted-at-ms (long asserted-at-ms)
                  :sent-at-ms (long asserted-at-ms)
                  :request-id request-id
                  :idempotency-key request-id
                  :evidence-source-id evidence-source-id
                  :evidence-anchor-id evidence-anchor-id
                  :note note})]
    (rk/append-relation-request! rk-rt request)
    {:status (if (materialized-to? rk-rt relation-id :asserted)
               :materialized
               :unmaterialized)
     :relation-id relation-id
     :request-id request-id}))

(defn bank-gold!
  "Bank one human point→say wish coupling. Returns :no-target for a miss/self
   pick; otherwise it does not report success until the :references row is
   queryable. The receipt remains mechanical evidence and is not converted into
   a semantic edge kind."
  [rk-rt {:keys [source-unit-id target-unit-id receipt asserted-at-ms
                 evidence-source-id]}]
  (if (nil? rk-rt)
    {:status :runtime-unavailable
     :source-unit-id source-unit-id
     :target-unit-id target-unit-id}
    (if (or (not (rk/present-string? source-unit-id))
          (not (rk/present-string? target-unit-id))
          (= source-unit-id target-unit-id))
      {:status :no-target :source-unit-id source-unit-id
       :target-unit-id target-unit-id}
      (merge
       {:stratum :gold
        :source-unit-id source-unit-id
        :target-unit-id target-unit-id}
       (append-asserted-edge!
        rk-rt
        {:kind gold-edge-kind
         :from (rk/->target-ref :derived-unit source-unit-id)
         :to (rk/->target-ref :derived-unit target-unit-id)
         :actor-id gold-actor-id
         :actor-type :human
         :asserted-at-ms asserted-at-ms
         :evidence-source-id evidence-source-id
         :note (pr-str
                {:mark/type :wish
                 :mark/receipt-version (:receipt/version receipt)
                 :mark/src-path (get-in receipt [:receipt/picked-at :src-path])})
         :request-prefix "circulation:gold:"})))))

;; ===========================================================================
;; OC records for the calibrated machine mouth
;; ===========================================================================

(defn circulation-order-key
  "One stable projection cell per calibrated machine run/association."
  [record-id]
  (str "circulation:" (core/sha-256 (str record-id))))

(defn circulation-record-request
  "Hint-only OC import for a machine observation/interpretation/proposal record.
   The three stages are separate values. Retrying one identical run reuses the
   same import-key and projection cell, so an identical failure condenses to one
   durable fact instead of inflaming the record."
  [{:keys [object-key record-id time-ms record]}]
  (let [imp-key (str "imp:ep:" object-key ":"
                     (core/sha-256 (str "circulation " record-id)))
        request-id (str "req:circulation:" object-key ":"
                        (core/sha-256 (str record-id)))
        event-id (str "evt:" object-key ":"
                      (core/sha-256 (str "circulation " record-id)))
        ;; OC's custody vocabulary names the process as :agent; the record and
        ;; proposed RelationEdge retain :llm as the epistemic asserter type.
        actor {:actor/id autotag-actor-id
               :actor/type :agent
               :actor/capabilities #{:object-container/import-material}}
        hint (assoc
              (oc/->TranscriptConversationProjectionRow
               :transcript-conversation-projection
               (str "oc:chat-conversation:" object-key)
               (circulation-order-key record-id)
               :material-circulation
               nil nil nil nil nil nil
               event-id request-id imp-key
               record-id autotag-actor-id
               (pr-str
                (select-keys record
                             [:circulation/status :circulation/input-hash
                              :circulation/failure-signature]))
               nil)
              :circulation record)
        payload {:object-key object-key
                 :source-artifacts []
                 :object-containers []
                 :revisions []
                 :derived-units []
                 :source-anchors []
                 :composition-edges []
                 :source-versions []
                 :projection-hints [hint]
                 :source-line-statuses []}
        fingerprint (oc/import-material-fingerprint object-key imp-key payload)]
    (assoc
     (core/action-request
      {:request-id request-id
       :request-type :object-container/import-material
       :time-ms (long time-ms)
       :actor actor
       :target {:target/kind :object-container-import
                :target/id imp-key
                :target/address {:object/key object-key}}
       :action {:action/type :object-container/import-material
                :action/capability :object-container/import-material
                :action/params {:source/format :material-circulation}}
       :routing/key [:object-container/import object-key]
       :payload payload
       :provenance {:source/type :material-circulation}})
     :partition/key object-key
     :object/key object-key
     :import/key imp-key
     :idempotency/key imp-key
     :material/fingerprint fingerprint)))

(defn append-circulation-record!
  [oc-rt args]
  (let [request (circulation-record-request args)]
    (ocr/append-object-container-request! oc-rt request)
    (let [decision (ocr/await-object-container-decision oc-rt request 20000)]
      {:status (if (= :accepted (:status decision)) :accepted :rejected)
       :request request
       :decision decision})))

(defn read-circulation-records
  [oc-rt object-key]
  (->> (ocr/read-transcript-conversation-projection
        oc-rt (str "oc:chat-conversation:" object-key) "" 100000)
       (filter #(= :material-circulation (:entry-kind %)))
       (sort-by :order-key)
       (keep :circulation)
       vec))

;; ===========================================================================
;; Starter culture — retroactive predicates, never synthetic receipts
;; ===========================================================================

(def starter-predicates
  [{:kind-id task-kind-id
    :pattern #"(?i)(?:^|\s)#TASK\b"
    :observation "#TASK"}
   {:kind-id feedback-kind-id
    :pattern #"(?i)(?:^|\s)#Feedback\b"
    :observation "#Feedback"}])

(defn starter-culture-plan
  "Pure predicate pass over existing served blocks. It emits silver
   :instance-of proposals only; it never manufactures a historical receipt."
  [blocks asserted-at-ms]
  (vec
   (for [block blocks
         {:keys [kind-id pattern observation]} starter-predicates
         :let [unit-id (or (:unit-id block) (:id block))
               text (str (:text block))]
         :when (and (rk/present-string? unit-id)
                    (re-find pattern text))]
     {:unit-id unit-id
      :kind-id kind-id
      :asserted-at-ms (long (or (:time-ms block) asserted-at-ms 0))
      :evidence-source-id (:source-id block)
      :observation {:stage :observation
                    :predicate :literal-hashtag
                    :matched observation}
      :interpretation {:stage :interpretation
                       :kind-id kind-id
                       :basis :starter-culture}
      :proposal {:stage :proposal
                 :relation-kind kind-edge-kind
                 :from unit-id
                 :to kind-id}})))

(defn seed-starter-culture!
  "Assert the starter plan through the existing relation kernel. Repeated
   scans converge on the same asserter-scoped relation ids."
  [rk-rt blocks asserted-at-ms]
  (let [plan (starter-culture-plan blocks asserted-at-ms)
        writes
        (mapv
         (fn [{:keys [unit-id kind-id evidence-source-id] :as item}]
           (merge
            item
            (append-asserted-edge!
             rk-rt
             {:kind kind-edge-kind
              :from (rk/->target-ref :derived-unit unit-id)
              :to (rk/->target-ref :kind kind-id)
              :actor-id starter-culture-actor-id
              :actor-type :llm
              :asserted-at-ms (:asserted-at-ms item)
              :evidence-source-id evidence-source-id
              :note (pr-str
                     (select-keys item
                                  [:observation :interpretation :proposal]))
              :request-prefix "circulation:starter:"})))
         plan)]
    {:status (if (every? #(= :materialized (:status %)) writes)
               :completed
               :incomplete-writes)
     :matched (count plan)
     :materialized (count (filter #(= :materialized (:status %)) writes))
     :writes writes}))

;; ===========================================================================
;; Terminal-escape detector — log-derived, no heuristic prose
;; ===========================================================================

(declare ^:private analyze-activation-history)

(defn- commit-touches-policy?
  [policy-paths commit]
  (boolean
   (some
    (fn [file-entry]
      (some
       (fn [path]
         (or (= path file-entry)
             (str/ends-with? file-entry (str " -> " path))))
       policy-paths))
    (:files commit))))

(defn terminal-escape-report
  "A policy-changing code commit after the latest activation event is one
   terminal escape. Inputs are already-log-derived commit rows and active-pointer
   revision rows; no filesystem mtime or current-value inference participates."
  ([commits activation-events]
   (terminal-escape-report commits activation-events
                           default-material-policy-paths))
  ([commits activation-events policy-paths]
   (let [analysis (analyze-activation-history activation-events)
         activation-line (:activation-history/rows analysis)
         latest-activation (last activation-line)
         latest-activation-ms (long (:created-at-ms latest-activation 0))
         ambiguous?
         (or (not (:activation-history/linear? analysis))
             (seq (:activation-history/clock-regressions analysis)))
         policy-commits
         (->> commits
              (filter #(commit-touches-policy? policy-paths %))
              (sort-by (juxt :committed-at-ms :sha))
              vec)
         escape-candidates
         (->> policy-commits
              (filter #(> (long (:committed-at-ms % 0))
                          latest-activation-ms))
              (mapv #(select-keys % [:sha :committed-at-ms :subject :files])))]
     {:terminal-escape/status
      (if ambiguous? :ambiguous-activation-history :measured)
      :terminal-escape/count
      (when-not ambiguous? (count escape-candidates))
      :terminal-escape/escapes
      (when-not ambiguous? escape-candidates)
      :terminal-escape/candidates escape-candidates
      :terminal-escape/ambiguous? (boolean ambiguous?)
      :terminal-escape/policy-paths (vec (sort policy-paths))
      :terminal-escape/policy-commits-scanned (count policy-commits)
      :terminal-escape/commits-scanned (count commits)
      :terminal-escape/activation-events-scanned (count activation-events)
      :terminal-escape/latest-activation-ms latest-activation-ms
      :terminal-escape/latest-activation-revision-id
      (:revision-id latest-activation)
      :terminal-escape/activation-history-linear?
      (:activation-history/linear? analysis)
      :terminal-escape/activation-clock-regressions
      (:activation-history/clock-regressions analysis)})))

;; ===========================================================================
;; Ambient silver autotag — llm-module lifecycle, strict closed-world output
;; ===========================================================================

(defn autotag-input
  [record-unit-id record-text candidates]
  (let [candidates (->> candidates
                        (keep (fn [c]
                                (when (rk/present-string? (:id c))
                                  {:id (:id c) :text (str (:text c))})))
                        (sort-by :id)
                        distinct
                        vec)
        input {:record-unit-id record-unit-id
               :record-text (str record-text)
               :candidates candidates}]
    (assoc input :input-hash (core/sha-256 (pr-str input)))))

(defn autotag-run-id
  [object-key input-hash salt]
  (str "llm-run-material-autotag:"
       (core/sha-256
        (str/join "\u0000" [object-key input-hash autotag-version
                              (str (or salt ""))]))))

(defn render-autotag-prompt
  [{:keys [record-unit-id record-text candidates]}]
  (str
   "You are Softland's calibrated material-autotag resident. A record was\n"
   "uttered during ordinary work. Decide whether it concerns exactly one of\n"
   "the candidate material units below. This is a SILVER proposal, never a\n"
   "claim about what Sid explicitly pointed at. When uncertain, choose null.\n\n"
   "Return STRICT JSON only:\n"
   "{\"target\": <candidate id or null>, \"interpretation\": <short string or null>, "
   "\"confidence\": <number 0..1>}\n\n"
   "Record [" record-unit-id "]:\n" record-text "\n\nCandidates:\n"
   (str/join "\n" (map (fn [{:keys [id text]}]
                         (str "[" id "] " text))
                       candidates))))

(defn parse-autotag-output
  [s]
  (try
    (let [v (json/read-str (str s) :key-fn keyword)]
      (if (map? v)
        {:ok? true :parsed v}
        {:ok? false :error {:reason :not-a-map}}))
    (catch Throwable t
      {:ok? false
       :error {:reason :invalid-json :message (.getMessage t)}})))

(defn validate-autotag-output
  "Closed-world and total. Observation, interpretation, and proposal remain
   separate even in the successful return."
  [input parsed]
  (let [target (:target parsed)
        confidence (:confidence parsed)
        interpretation (:interpretation parsed)
        candidate-ids (set (map :id (:candidates input)))
        error
        (cond
          (and (some? target) (not (string? target))) :target-not-string
          (and (some? target) (not (contains? candidate-ids target))) :unknown-target
          (not (number? confidence)) :confidence-not-number
          (not (<= 0.0 (double confidence) 1.0)) :confidence-out-of-range
          (and (some? target) (str/blank? (str interpretation))) :missing-interpretation
          :else nil)]
    (if error
      {:ok? false :error {:reason error}}
      {:ok? true
       :observation {:stage :observation
                     :record-unit-id (:record-unit-id input)
                     :input-hash (:input-hash input)
                     :candidate-ids (mapv :id (:candidates input))}
       :interpretation {:stage :interpretation
                        :text (when (some? target) (str interpretation))
                        :confidence (double confidence)}
       :proposal (when (some? target)
                   {:stage :proposal
                    :relation-kind about-edge-kind
                    :from (:record-unit-id input)
                    :to target})})))

(defn- autotag-record-for-run
  [oc-rt object-key run-id]
  (some #(when (= run-id (:run/id %)) %)
        (read-circulation-records oc-rt object-key)))

(defn- assert-autotag-proposal!
  [rk-rt record]
  (when-let [{:keys [from to]} (:proposal record)]
    (merge
     {:stratum :silver}
     (append-asserted-edge!
      rk-rt
      {:kind about-edge-kind
       :from (rk/->target-ref :derived-unit from)
       :to (rk/->target-ref :derived-unit to)
       :actor-id autotag-actor-id
       :actor-type :llm
       :asserted-at-ms (:circulation/recorded-at-ms record)
       :evidence-source-id (:circulation/evidence-source-id record)
       :note (pr-str
              {:run/id (:run/id record)
               :interpretation (:interpretation record)
               :proposal (:proposal record)})
       :request-prefix "circulation:autotag:"}))))

(defn autotag-material!
  "Run the ambient silver lane through the EXISTING llm-module. `:lines` is the
   established canned-stream test seam; absent lines use the subscription CLI.

   Identical input derives one run id. Once its calibrated OC record exists,
   retries never invoke the model again: failed records return :condensed and
   successful records re-check/re-converge the relation edge."
  [{:keys [llm-rt oc-rt rk-rt]} object-key
   {:keys [record-unit-id record-text candidates evidence-source-id
           salt lines timeout-ms executor-id]
    :or {salt "" timeout-ms 5000 executor-id "material-autotag-executor"}}]
  (let [input (autotag-input record-unit-id record-text candidates)
        run-id (autotag-run-id object-key (:input-hash input) salt)
        record-id (str "autotag:" run-id)
        existing-record (autotag-record-for-run oc-rt object-key run-id)
        existing-run (llm/read-run llm-rt run-id)]
    (cond
      existing-record
      (let [edge (when (= :completed (:circulation/status existing-record))
                   (assert-autotag-proposal! rk-rt existing-record))]
        {:status (if (= :failed (:circulation/status existing-record))
                   :condensed-failure
                   :already-recorded)
         :run-id run-id
         :record existing-record
         :edge edge
         :adapter-called? false})

      (and existing-run
           (contains? llm/terminal-statuses (:status existing-run)))
      {:status :terminal-without-record
       :run-id run-id
       :terminal-status (:status existing-run)
       :retry-with-salt true
       :adapter-called? false}

      :else
      (let [bundle-id (str "material-autotag:" (:input-hash input))
            turn-id (str "material-autotag-turn:" (:input-hash input))
            request
            (llm/turn-run-request
             "annotation:material-autotag" turn-id bundle-id
             {:llm-turn-run-id run-id
              :llm-thread-id (str "material-autotag-thread:" object-key)
              :request-id (str "material-autotag-request:" run-id)
              :time-ms (core/now-ms)
              :llm/backend :claude
              :llm/auth-mode :subscription
              :executor-task-id llm/pending-task-id})
            _ (llm/append-turn-run-request! llm-rt request)
            _ (llm/await-decision llm-rt run-id)
            _ (llm/await-run llm-rt run-id #(= :pending (:status %)))
            claim (llm/claim-run! llm-rt run-id executor-id
                                  {:timeout-ms 5000})
            granted? (= :granted-to-us (:claim-state claim))
            claimed-row (:run claim)
            adapter (llm/claude-stream-json-adapter
                     (cond-> {:timeout-ms timeout-ms}
                       lines (assoc :lines lines)))
            observations
            (when granted?
              (let [events
                    (vec
                     (llm/run-adapter-turn
                      adapter
                      {:run claimed-row
                       :claim (:claim claim)
                       :context-bundle
                       {:context-bundle/id bundle-id
                        :rendered/model-input (render-autotag-prompt input)
                        :input-hash (:input-hash input)}}))
                    obs (map-indexed
                         #(llm/adapter-event->observation
                           claimed-row (:claim claim) %1 %2)
                         events)]
                (doseq [o obs] (llm/append-observation! llm-rt o))
                (vec obs)))
            run-row
            (if granted?
              (llm/await-materialized
               #(llm/read-run llm-rt run-id)
               #(contains? llm/terminal-statuses (:status %))
               (max 10000 (long timeout-ms)))
              (or claimed-row (llm/read-run llm-rt run-id)))
            result-obs
            (last (filter #(= :claude/result (:observation/type %))
                          observations))
            result-text (:result/text result-obs)
            parsed (when (= :succeeded (:status run-row))
                     (parse-autotag-output result-text))
            valid (when (:ok? parsed)
                    (validate-autotag-output input (:parsed parsed)))
            recorded-at-ms (long (or (:finished-at run-row) (core/now-ms)))
            success? (and granted?
                          (= :succeeded (:status run-row))
                          (:ok? parsed)
                          (:ok? valid))
            failure-signature
            (when-not success?
              (core/sha-256
               (pr-str
                {:claim-state (:claim-state claim)
                 :terminal-status (:status run-row)
                 :parse-error (:error parsed)
                 :validation-error (:error valid)})))
            record
            (merge
             {:circulation/version 1
              :circulation/kind :ambient-autotag
              :circulation/status (if success? :completed :failed)
              :circulation/input-hash (:input-hash input)
              :circulation/recorded-at-ms recorded-at-ms
              :circulation/evidence-source-id evidence-source-id
              :run/id run-id
              :run/terminal-status (:status run-row)
              :observation
              (or (:observation valid)
                  {:stage :observation
                   :record-unit-id record-unit-id
                   :input-hash (:input-hash input)
                   :candidate-ids (mapv :id (:candidates input))})
              :interpretation
              (or (:interpretation valid)
                  {:stage :interpretation
                   :status :failed
                   :parse-error (:error parsed)
                   :validation-error (:error valid)})
              :proposal (when success? (:proposal valid))}
             (when failure-signature
               {:circulation/failure-signature failure-signature}))
            durable
            (append-circulation-record!
             oc-rt {:object-key object-key
                    :record-id record-id
                    :time-ms recorded-at-ms
                    :record record})
            edge
            (when (and (= :accepted (:status durable)) success?)
              (assert-autotag-proposal! rk-rt record))
            epoch
            (when (= :materialized (:status edge))
              (swap! util-fns/!ingest-epoch-atom inc))]
        {:status (cond
                   (not= :accepted (:status durable)) :record-rejected
                   (not success?) :failed
                   (and edge (not= :materialized (:status edge)))
                   :incomplete-writes
                   :else :completed)
         :run-id run-id
         :record record
         :edge edge
         :epoch epoch
         :adapter-called? granted?}))))

;; ===========================================================================
;; Standing query — one batched edge read, receipt/silver/gold composition
;; ===========================================================================

(defn- activation-history
  [oc-rt spec]
  (when (and oc-rt spec)
    (vec (ocr/read-revision-history
          oc-rt (facet-master/active-pointer-container-id spec) "" 100000))))

(defn- analyze-activation-history
  "Reconstruct the pointer's causal activation line from parent revision ids.
   Producer clocks are metadata, not ordering authority: P3 legitimately
   appended a later activation with time-ms 1 after older browser drills. A
   wall-clock sort therefore lies about the worn tip.

   A non-linear/incomplete history stays explicit and yields no invented
   ordering. Callers mark such an as-of ambiguous."
  [history]
  (let [rows (->> history
                  (map (juxt :revision-id identity))
                  (into {})
                  vals
                  vec)
        by-id (into {} (map (juxt :revision-id identity)) rows)
        parent-ids (into #{} (keep :parent-revision-id) rows)
        tips (->> rows
                  (remove #(contains? parent-ids (:revision-id %)))
                  (sort-by :revision-id)
                  vec)
        chain
        (when (= 1 (count tips))
          (loop [row (first tips), acc (), seen #{}]
            (cond
              (nil? row) (vec acc)
              (contains? seen (:revision-id row)) nil
              :else
              (let [parent-id (:parent-revision-id row)]
                (if (and parent-id (nil? (get by-id parent-id)))
                  nil
                  (recur (when parent-id (get by-id parent-id))
                         (conj acc row)
                         (conj seen (:revision-id row))))))))
        linear? (and chain (= (count chain) (count rows)))
        ordered (if linear? chain [])
        regressions
        (->> (partition 2 1 ordered)
             (keep
              (fn [[parent child]]
                (when (< (long (:created-at-ms child 0))
                         (long (:created-at-ms parent 0)))
                  {:parent-revision-id (:revision-id parent)
                   :parent-time-ms (:created-at-ms parent)
                   :child-revision-id (:revision-id child)
                   :child-time-ms (:created-at-ms child)})))
             vec)]
    {:activation-history/rows ordered
     :activation-history/linear? (boolean linear?)
     :activation-history/tip-count (count tips)
     :activation-history/clock-regressions regressions}))

(defn- activation-as-of
  [analysis time-ms captured-revision]
  (let [rows (:activation-history/rows analysis)
        selected (last (filter #(<= (long (:created-at-ms % 0))
                                    (long time-ms))
                               rows))
        ;; P6 · R4: the pointer's source is an activation EVENT now, not a
        ;; bare revision-id. `worn-revision-id` reads both generations through
        ;; one door, so a v0 row minted by P1 and a P6 event resolve to the
        ;; same worn revision. Reading `:content-text` raw here would have made
        ;; every post-P6 as-of answer the whole event map.
        active (activation-event/worn-revision-id (:content-text selected))
        clock-regressions
        (:activation-history/clock-regressions analysis)
        ambiguous?
        (or (not (:activation-history/linear? analysis))
            ;; A captured tip resolves the ordinary present-tense receipt even
            ;; when an older producer clock regressed. Any disagreement under
            ;; such a clock is not safely adjudicable from time alone.
            (and (seq clock-regressions)
                 (not= captured-revision active)))]
    {:material/active-revision-as-of active
     :material/activation-revision-id (:revision-id selected)
     :material/as-of-source :activation-history
     :material/as-of-ambiguous? (boolean ambiguous?)
     :material/activation-history-linear?
     (:activation-history/linear? analysis)
     :material/activation-clock-regressions clock-regressions}))

(defn active-revision-as-of
  "Resolve one registered facet's active revision at `time-ms` from its active
   pointer's CAUSAL revision history. Parent links order activations; timestamps
   only select the eligible causal prefix. No current-state snapshot is read."
  [oc-rt master-id time-ms]
  (when-let [spec (and oc-rt (facet-masters/spec master-id))]
    (:material/active-revision-as-of
     (activation-as-of
      (analyze-activation-history (activation-history oc-rt spec))
      time-ms nil))))

(defn- analyses-for-references
  [oc-rt references]
  (into
   {}
   (keep
    (fn [master-id]
      (when-let [spec (facet-masters/spec master-id)]
        [master-id
         (analyze-activation-history (activation-history oc-rt spec))])))
   (distinct (keep :material/master references))))

(defn- resolve-reference-as-of
  [analyses time-ms ref]
  (let [resolved
        (when-let [analysis (get analyses (:material/master ref))]
          (activation-as-of analysis time-ms (:material/revision ref)))]
    (merge
     ref
     {:material/captured-revision (:material/revision ref)
      :material/as-of-matches-capture?
      (and (not (:material/as-of-ambiguous? resolved))
           (= (:material/active-revision-as-of resolved)
              (:material/revision ref)))}
     resolved)))

(defn resolve-receipt-as-of
  [oc-rt receipt]
  (let [references (:receipt/worn-materials receipt)
        analyses (analyses-for-references oc-rt references)]
    (assoc
     receipt
     :receipt/worn-as-of
     (mapv #(resolve-reference-as-of
             analyses (:receipt/captured-at-ms receipt) %)
           references))))

(defn resolve-records-as-of
  "Resolve a batch of receipt records with one causal activation-history read
   per referenced registered master. Unknown masters stay explicit with a nil
   as-of revision; no current-state snapshot is substituted."
  [oc-rt records]
  (let [references
        (mapcat #(get-in % [:receipt :receipt/worn-materials]) records)
        analyses (analyses-for-references oc-rt references)]
    (mapv
     (fn [record]
       (update
        record :receipt
        (fn [receipt]
          (let [t (:receipt/captured-at-ms receipt)]
            (assoc
             receipt
             :receipt/worn-as-of
             (mapv #(resolve-reference-as-of analyses t %)
                   (:receipt/worn-materials receipt)))))))
     records)))

(defn compose-experience
  "Pure composition over a batch relation read and receipt-bearing records.
   Edges are visible in both directions because the relation query indexes both
   endpoints. Items retain exact origin reverse-links."
  [material-ids relation-map records]
  (let [ids (set material-ids)
        records-by-origin
        (group-by :origin-unit-id records)
        rows (->> material-ids
                  (mapcat #(get relation-map % []))
                  (map (juxt :relation-id identity))
                  (into {})
                  vals)
        incident
        (filter
         (fn [row]
           (or (contains? ids (get-in row [:from :target-id]))
               (contains? ids (get-in row [:to :target-id]))))
         rows)
        edge-origins
        (into #{}
              (mapcat (fn [row]
                        [(get-in row [:from :target-id])
                         (get-in row [:to :target-id])]))
              incident)
        record-origins
        (into #{}
              (keep (fn [{:keys [origin-unit-id receipt circulation]}]
                      (when (or (contains? ids origin-unit-id)
                                (and receipt
                                     (contains? ids (receipt-target receipt)))
                                (and circulation
                                     (contains? ids
                                                (get-in circulation
                                                        [:proposal :to]))))
                        origin-unit-id)))
              records)
        origins (sort (into edge-origins record-origins))
        edges-for
        (fn [origin]
          (filterv
           #(or (= origin (get-in % [:from :target-id]))
                (= origin (get-in % [:to :target-id])))
           incident))
        items
        (mapv
         (fn [origin]
           (let [origin-records (get records-by-origin origin [])
                 receipts (filterv :receipt origin-records)
                 machine-records (filterv :circulation origin-records)
                 edges (edges-for origin)
                 gold (filterv gold-edge? edges)
                 silver (filterv silver-edge? edges)
                 strata (cond-> #{}
                          (seq receipts) (conj :receipt)
                          (or (seq silver) (seq machine-records)) (conj :silver)
                          (seq gold) (conj :gold))]
             {:experience/origin-unit-id origin
              :experience/reverse-links
              (vec
               (sort
                (keep
                 (fn [row]
                   (let [from (get-in row [:from :target-id])
                         to (get-in row [:to :target-id])]
                     (cond
                       (= origin from) to
                       (= origin to) from)))
                 edges)))
              :experience/strata strata
              :experience/receipts (mapv :receipt receipts)
              :experience/machine-records
              (mapv :circulation machine-records)
              :experience/silver silver
              :experience/gold gold}))
         origins)
        gold-marks-by-target
        (->> incident
             (filter gold-edge?)
             (group-by #(get-in % [:to :target-id]))
             (reduce-kv
              (fn [m target rows]
                (assoc m target
                       (mapv (fn [row]
                               {:wish-unit-id (get-in row [:from :target-id])
                                :relation-id (:relation-id row)
                                :asserted-at-ms (:first-asserted-at-ms row)
                                :asserter-actor-id (:asserter-actor-id row)})
                             (sort-by :relation-id rows))))
              {}))
        silver-marks-by-target
        (->> incident
             (filter #(and (= about-edge-kind (:relation-kind %))
                           (silver-edge? %)))
             (group-by #(get-in % [:to :target-id]))
             (reduce-kv
              (fn [m target rows]
                (assoc m target
                       (mapv (fn [row]
                               {:record-unit-id (get-in row [:from :target-id])
                                :relation-id (:relation-id row)
                                :asserter-actor-id (:asserter-actor-id row)
                                :asserter-type (:asserter-type row)})
                             (sort-by :relation-id rows))))
              {}))]
    {:experience/items items
     :experience/gold-marks-by-target gold-marks-by-target
     :experience/silver-marks-by-target silver-marks-by-target
     :experience/composition
     {:receipt (count (filter #(contains? (:experience/strata %) :receipt) items))
      ;; `items` intentionally expose an edge from BOTH endpoints, but the
      ;; gauge counts relation identity once.
      :silver (count (filter silver-edge? incident))
      :gold (count (filter gold-edge? incident))
      :machine-records
      (count (filter :circulation records))
      :machine-failures
      (count
       (filter #(= :failed (get-in % [:circulation :circulation/status]))
               records))}}))

(defn experience-around-many
  "Serve 'everything experienced around this material' for many material ids.
   Relation state is fetched with ONE batched public query. Callers supply
   receipt records already obtained from their one conversation projection read
   so this helper never opens a source-specific second truth artery."
  [rk-rt material-ids records]
  (let [ids (vec (distinct (filter rk/present-string? material-ids)))
        relation-map
        (if (and rk-rt (seq ids))
          (rk/read-relations-for-targets
           rk-rt ids [gold-edge-kind about-edge-kind kind-edge-kind] false)
          {})]
    (merge
     {:experience/material-ids ids
      :experience/query-plan
      {:relation-roundtrips (if (and rk-rt (seq ids)) 1 0)
       :targets-requested (count ids)
       :batched? true
       :relation-runtime-available? (boolean rk-rt)}}
     (compose-experience ids relation-map records))))
