;; IMPORTANT: Before modifying this file, re-read
;; docs/current-mental-model/build/object-container/PLAN.md and check pending todos.
;; Adhere to all previously decided design decisions.

(ns app.server.rama.object-container
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [app.server.rama.core :as core]
            [clojure.string :as str]
            [com.rpl.rama.test :refer [create-ipc launch-module!]]))

;; Object Container kernel, Slice 1.
;;
;; This module is intentionally a new storage shape rather than a copy of the
;; old text kernel. Immutable source artifacts, derived units, authored object
;; containers, revisions, anchors, composition edges, and the outline projection
;; are separate rows with deterministic ids.

(def markdown-distiller-id "markdown-block-v0")
(def markdown-distiller-version 1)
(def object-key-separator (str (char 0)))
(def default-outline-page-size 1000)

(defprotocol IObjectContainerRequestPayload)
(defprotocol IObjectContainerEventPayload)

(defrecord ActorRow [actor-id actor-type actor-capabilities])
(defrecord TargetRefRow [target-kind target-id target-address])

(defrecord SourceIngestPayload
  [source-ref source-hash source-raw-text source-format distiller-id distiller-version]
  IObjectContainerRequestPayload)

(defrecord ObjectEditPayload
  [document-container-id object-key content-text content-hash edit-client-id edit-seq
   edit-lineage-key revision-id]
  IObjectContainerRequestPayload)

(defrecord SourceIngestedPayload
  [source-id source-ref source-hash document-container-id distiller-id distiller-version
   derived-unit-count composition-edge-count]
  IObjectContainerEventPayload)

(defrecord ObjectGraduatedPayload
  [unit-id container-id document-container-id revision-id content-hash edit-client-id
   edit-seq]
  IObjectContainerEventPayload)

(defrecord ObjectRevisedPayload
  [container-id source-unit-id document-container-id parent-revision-id revision-id
   content-hash edit-client-id edit-seq]
  IObjectContainerEventPayload)

(defrecord UnitReadResult [unit graduation target-kind target-id content-text content-hash])

(defrecord ObjectContainerRequestRow
  [audit-id partition-key request-id request-type routing-key idempotency-key actor target
   payload requested-at-ms raw-request])

(defrecord ObjectContainerDecisionRow
  [decision-id audit-id partition-key request-id request-type idempotency-key status reason
   errors event-id event-row decided-at-ms replayed-from-decision-id])

(defrecord ObjectContainerEventRow
  [event-id event-type object-key target-kind target-id actor payload event-time-ms
   request-id audit-id])

(defrecord SourceArtifactRow
  [source-id source-ref source-hash source-format source-raw-text document-container-id
   content-byte-count created-at-ms created-by event-id])

(defrecord SourceVersionRow
  [source-ref-key source-ref source-hash source-id document-container-id object-key
   order-key created-at-ms event-id])

(defrecord SourceIngestCompletionRow
  [source-id source-ref-key source-ref source-hash document-container-id derived-unit-count
   composition-edge-count completed-at-ms completed-by-request-id event-id])

(defrecord ObjectContainerRow
  [container-id container-kind object-key visibility source-id source-anchor-id
   source-unit-id document-container-id current-revision-id current-content-text
   current-content-hash created-at-ms created-by event-id])

(defrecord RevisionRow
  [revision-id container-id parent-revision-id content-text content-hash order-key
   created-at-ms created-by event-id])

(defrecord DerivedUnitRow
  [unit-id document-container-id source-id unit-kind block-path parent-slot-id
   source-anchor-id derived-content-text derived-content-hash distiller-id
   distiller-version event-id])

(defrecord UnitGraduationRow
  [unit-id container-id current-revision-id current-content-text current-content-hash
   graduated-at-ms last-revised-at-ms event-id])

(defrecord SourceAnchorRow
  [source-anchor-id target-kind target-id source-id source-ref source-hash start-offset
   end-offset block-path event-id])

(defrecord CompositionEdgeRow
  [edge-id object-key document-container-id parent-slot-id child-slot-id child-order-key
   parent-target-kind parent-target-id child-target-kind child-target-id event-id])

(defrecord OutlineNodeRow
  [document-container-id node-slot-id block-path parent-slot-id target-kind target-id
   source-anchor-id content-text content-hash graduated container-id event-id])

(defrecord EditOrderRow
  [lineage-key edit-client-id edit-seq idempotency-key request-id revision-id event-id
   accepted-at-ms])

(defn string-present?
  [x]
  (and (string? x) (not (str/blank? x))))

(defn actor-row
  [actor]
  (->ActorRow (get actor :actor/id)
              (get actor :actor/type)
              (set (get actor :actor/capabilities))))

(defn target-row
  [target]
  (->TargetRefRow (get target :target/kind)
                  (get target :target/id)
                  (get target :target/address)))

(defn source-ref-key
  [source-ref]
  (core/sha-256 source-ref))

(defn object-key-for
  [source-ref source-hash]
  (core/sha-256 (str source-ref object-key-separator source-hash)))

(defn source-id-for-object-key
  [object-key]
  (str "src:" object-key))

(defn document-id-for-object-key
  [object-key]
  (str "oc:doc:" object-key))

(defn derived-unit-id
  [object-key block-path]
  (str "du:" object-key ":" markdown-distiller-id ":" block-path))

(defn source-anchor-id
  [target-id]
  (str "sa:" target-id))

(defn block-container-id
  [object-key unit-local-id]
  (str "oc:block:" object-key ":" unit-local-id))

(defn composition-edge-id
  [object-key parent-slot-id child-order-key]
  (str "ce:" object-key ":" parent-slot-id ":" child-order-key))

(defn revision-id-for-request
  [object-key request]
  (or (get-in request [:payload :revision-id])
      (str "rev:" object-key ":" (:request/id request))))

(defn event-id-for-request
  [object-key request]
  (str "evt:" object-key ":" (:request/id request)))

(defn fixed-width-order-key
  [time-ms request-id]
  (format "%020d:%s" (long (or time-ms 0)) request-id))

(defn audit-id
  [partition-key request-id]
  (str partition-key "/request/" request-id))

(defn decision-id-for-audit-id
  [audit-id]
  (str audit-id "/decision"))

(defn source-id-for
  [source-ref source-hash]
  (source-id-for-object-key (object-key-for source-ref source-hash)))

(defn document-id-for
  [source-ref source-hash]
  (document-id-for-object-key (object-key-for source-ref source-hash)))

(defn source-hash
  [raw-text]
  (core/sha-256 (str raw-text)))

(defn positive-partition
  [num-partitions k]
  (if (pos? num-partitions)
    (mod (hash k) num-partitions)
    0))

(defn extract-object-key
  [id-or-key]
  (let [s (str id-or-key)]
    (cond
      (str/starts-with? s "src:")
      (subs s 4)

      (str/starts-with? s "oc:doc:")
      (subs s 7)

      (str/starts-with? s "du:")
      (let [rest (subs s 3)
            idx (str/index-of rest ":")]
        (if idx (subs rest 0 idx) rest))

      (str/starts-with? s "sa:")
      (extract-object-key (subs s 3))

      (str/starts-with? s "ce:")
      (let [rest (subs s 3)
            idx (str/index-of rest ":")]
        (if idx (subs rest 0 idx) rest))

      (str/starts-with? s "oc:block:")
      (let [rest (subs s 9)
            idx (str/index-of rest ":")]
        (if idx (subs rest 0 idx) rest))

      (str/starts-with? s "rev:")
      (let [rest (subs s 4)
            idx (str/index-of rest ":")]
        (if idx (subs rest 0 idx) rest))

      (str/starts-with? s "evt:")
      (let [rest (subs s 4)
            idx (str/index-of rest ":")]
        (if idx (subs rest 0 idx) rest))

      :else s)))

(defn partition-by-object-key
  [num-partitions id-or-key]
  (positive-partition num-partitions (extract-object-key id-or-key)))

(defn audit-partition-key
  [audit-id]
  (let [s (str audit-id)
        marker "/request/"
        idx (str/index-of s marker)]
    (if idx (subs s 0 idx) s)))

(defn partition-by-audit-id
  [num-partitions audit-id]
  (positive-partition num-partitions (audit-partition-key audit-id)))

(defn payload-source-ref [payload] (:source-ref payload))
(defn payload-source-hash [payload] (:source-hash payload))
(defn payload-source-raw-text [payload] (:source-raw-text payload))
(defn payload-source-format [payload] (:source-format payload))
(defn payload-distiller-id [payload] (:distiller-id payload))
(defn payload-distiller-version [payload] (:distiller-version payload))
(defn payload-document-container-id [payload] (:document-container-id payload))
(defn payload-object-key [payload] (:object-key payload))
(defn payload-content-text [payload] (:content-text payload))
(defn payload-content-hash [payload] (:content-hash payload))
(defn payload-edit-client-id [payload] (:edit-client-id payload))
(defn payload-edit-seq [payload] (:edit-seq payload))
(defn payload-edit-lineage-key [payload] (:edit-lineage-key payload))

(defn request-partition-key [request] (:partition/key request))
(defn request-id [request] (:request/id request))
(defn request-type [request] (:request/type request))
(defn request-payload [request] (:payload request))
(defn request-idempotency-key [request] (:idempotency/key request))
(defn request-audit-id [request] (audit-id (request-partition-key request) (request-id request)))
(defn request-object-key [request] (payload-object-key (request-payload request)))
(defn request-source-ref-key [request] (request-partition-key request))

(defn request-row
  [request]
  (let [audit (request-audit-id request)]
    (->ObjectContainerRequestRow audit
                                 (request-partition-key request)
                                 (request-id request)
                                 (request-type request)
                                 (:routing/key request)
                                 (request-idempotency-key request)
                                 (actor-row (:actor request))
                                 (target-row (:target request))
                                 (request-payload request)
                                 (:request/time-ms request)
                                 request)))

(defn event-row
  [event-id event-type object-key target-kind target-id actor payload time-ms request]
  (->ObjectContainerEventRow event-id
                             event-type
                             object-key
                             target-kind
                             target-id
                             (actor-row actor)
                             payload
                             time-ms
                             (request-id request)
                             (request-audit-id request)))

(defn accepted-decision-row
  [request event]
  (let [audit (request-audit-id request)]
    (->ObjectContainerDecisionRow (decision-id-for-audit-id audit)
                                  audit
                                  (request-partition-key request)
                                  (request-id request)
                                  (request-type request)
                                  (request-idempotency-key request)
                                  :accepted
                                  nil
                                  []
                                  (:event-id event)
                                  event
                                  (core/now-ms)
                                  nil)))

(defn rejected-decision-row
  [request reason errors]
  (let [audit (request-audit-id request)]
    (->ObjectContainerDecisionRow (decision-id-for-audit-id audit)
                                  audit
                                  (request-partition-key request)
                                  (request-id request)
                                  (request-type request)
                                  (request-idempotency-key request)
                                  :rejected
                                  reason
                                  (vec errors)
                                  nil
                                  nil
                                  (core/now-ms)
                                  nil)))

(defn replay-decision-row
  [request prior-decision]
  (let [audit (request-audit-id request)]
    (assoc prior-decision
           :decision-id (decision-id-for-audit-id audit)
           :audit-id audit
           :partition-key (request-partition-key request)
           :request-id (request-id request)
           :request-type (request-type request)
           :idempotency-key (request-idempotency-key request)
           :decided-at-ms (core/now-ms)
           :replayed-from-decision-id (:decision-id prior-decision))))

(defn completed-source-decision-row
  [request completion]
  (let [payload (->SourceIngestedPayload (:source-id completion)
                                         (:source-ref completion)
                                         (:source-hash completion)
                                         (:document-container-id completion)
                                         markdown-distiller-id
                                         markdown-distiller-version
                                         (:derived-unit-count completion)
                                         (:composition-edge-count completion))
        event (event-row (:event-id completion)
                         :source/ingested
                         (extract-object-key (:source-id completion))
                         :source-artifact
                         (:source-id completion)
                         (:actor request)
                         payload
                         (:request/time-ms request)
                         request)]
    (accepted-decision-row request event)))

(defn decision-accepted?
  [decision]
  (= :accepted (:status decision)))

(defn decision-event-id
  [decision]
  (:event-id decision))

(defn source-request-validation-errors
  [request]
  (let [payload (request-payload request)
        raw-text (str (payload-source-raw-text payload))
        expected-hash (source-hash raw-text)]
    (cond-> (vec (core/request-validation-errors request))
      (not= :source/ingest (request-type request))
      (conj {:type :request/type-invalid :value (request-type request)})

      (not (string-present? (request-partition-key request)))
      (conj {:type :partition/key-invalid :value (request-partition-key request)})

      (not (string-present? (request-idempotency-key request)))
      (conj {:type :idempotency/key-invalid :value (request-idempotency-key request)})

      (not (string-present? (payload-source-ref payload)))
      (conj {:type :source/ref-invalid :value (payload-source-ref payload)})

      (not (string-present? (payload-source-hash payload)))
      (conj {:type :source/hash-invalid :value (payload-source-hash payload)})

      (not= expected-hash (payload-source-hash payload))
      (conj {:type :source/hash-mismatch
             :expected expected-hash
             :actual (payload-source-hash payload)})

      (not= :markdown (payload-source-format payload))
      (conj {:type :source/format-invalid :value (payload-source-format payload)})

      (not= markdown-distiller-id (payload-distiller-id payload))
      (conj {:type :distiller/unsupported :value (payload-distiller-id payload)})

      (not= markdown-distiller-version (long (or (payload-distiller-version payload) 0)))
      (conj {:type :distiller/version-unsupported
             :value (payload-distiller-version payload)})

      (not= (source-ref-key (payload-source-ref payload)) (request-partition-key request))
      (conj {:type :partition/source-ref-key-mismatch})

      (not (core/authorized-request? request))
      (conj {:type :actor-not-authorized}))))

(defn source-request-valid?
  [errors]
  (empty? errors))

(def edit-target-kinds
  #{:derived-unit :object-container})

(defn edit-request-validation-errors
  [request]
  (let [payload (request-payload request)
        content (str (payload-content-text payload))
        expected-hash (source-hash content)
        target-kind (get-in request [:target :target/kind])
        target-id (get-in request [:target :target/id])]
    (cond-> (vec (core/request-validation-errors request))
      (not= :object/edit (request-type request))
      (conj {:type :request/type-invalid :value (request-type request)})

      (not (contains? edit-target-kinds target-kind))
      (conj {:type :target/kind-invalid :value target-kind})

      (not (string-present? target-id))
      (conj {:type :target/id-invalid :value target-id})

      (not (string-present? (request-partition-key request)))
      (conj {:type :partition/key-invalid :value (request-partition-key request)})

      (not (string-present? (request-idempotency-key request)))
      (conj {:type :idempotency/key-invalid :value (request-idempotency-key request)})

      (not (string-present? (payload-document-container-id payload)))
      (conj {:type :document/container-id-invalid
             :value (payload-document-container-id payload)})

      (not (string-present? (payload-object-key payload)))
      (conj {:type :object/key-invalid :value (payload-object-key payload)})

      (not= (payload-object-key payload) (request-partition-key request))
      (conj {:type :partition/object-key-mismatch})

      (not (string-present? (payload-content-hash payload)))
      (conj {:type :content/hash-invalid :value (payload-content-hash payload)})

      (not= expected-hash (payload-content-hash payload))
      (conj {:type :content/hash-mismatch
             :expected expected-hash
             :actual (payload-content-hash payload)})

      (not (string-present? (payload-edit-client-id payload)))
      (conj {:type :edit/client-id-invalid :value (payload-edit-client-id payload)})

      (not (integer? (payload-edit-seq payload)))
      (conj {:type :edit/seq-invalid :value (payload-edit-seq payload)})

      (not (core/authorized-request? request))
      (conj {:type :actor-not-authorized}))))

(def heading-pattern #"^\s{0,3}(#{1,6})(?:\s+(.*?)\s*|\s*)$")
(def unordered-list-pattern #"^(\s*)[-*+]\s+(.*?)\s*$")
(def ordered-list-pattern #"^(\s*)\d+[\.)]\s+(.*?)\s*$")

(defn strip-trailing-cr
  [s]
  (if (str/ends-with? s "\r")
    (subs s 0 (dec (count s)))
    s))

(defn markdown-source-lines
  [raw-text]
  (let [lines (str/split (str raw-text) #"\n" -1)]
    (loop [line-idx 0
           offset 0
           remaining lines
           result []]
      (if-let [line (first remaining)]
        (let [text (strip-trailing-cr line)
              raw-end (+ offset (count line))
              text-end (+ offset (count text))
              next-offset (inc raw-end)]
          (recur (inc line-idx)
                 next-offset
                 (next remaining)
                 (conj result {:line-idx line-idx
                               :text text
                               :start-offset offset
                               :end-offset text-end})))
        result))))

(defn current-heading-parent
  [heading-stack]
  (some->> heading-stack
           (sort-by key >)
           first
           val))

(defn heading-parent
  [heading-stack level]
  (some->> heading-stack
           (filter (fn [[heading-level _]] (< heading-level level)))
           (sort-by key >)
           first
           val))

(defn prune-heading-stack
  [heading-stack level]
  (into {} (filter (fn [[heading-level _]] (< heading-level level)) heading-stack)))

(defn list-parent
  [heading-stack list-stack indent]
  (or (some->> list-stack
               (filter (fn [[parent-indent _]] (< parent-indent indent)))
               (sort-by key >)
               first
               val)
      (current-heading-parent heading-stack)))

(defn update-list-stack
  [list-stack indent block-path]
  (assoc (into {} (filter (fn [[parent-indent _]] (< parent-indent indent)) list-stack))
         indent
         block-path))

(defn markdown-block-v0
  [raw-text]
  (letfn [(block-path [idx]
            (format "%06d" idx))
          (emit-block [state unit-kind parent-block-path text start-offset end-offset]
            (let [path (block-path (:next-idx state))]
              (-> state
                  (update :blocks conj {:block-path path
                                        :unit-local-id path
                                        :parent-block-path parent-block-path
                                        :unit-kind unit-kind
                                        :text text
                                        :start-offset start-offset
                                        :end-offset end-offset})
                  (update :next-idx inc)
                  (assoc :last-block-path path))))
          (flush-paragraph [state]
            (if-let [paragraph (:paragraph state)]
              (-> state
                  (emit-block :markdown/paragraph
                              (:parent-block-path paragraph)
                              (str/join "\n" (:lines paragraph))
                              (:start-offset paragraph)
                              (:end-offset paragraph))
                  (assoc :paragraph nil))
              state))
          (start-or-extend-paragraph [state {:keys [text start-offset end-offset]}]
            (if (:paragraph state)
              (-> state
                  (update-in [:paragraph :lines] conj text)
                  (assoc-in [:paragraph :end-offset] end-offset))
              (assoc state
                     :paragraph {:lines [text]
                                 :parent-block-path (current-heading-parent (:heading-stack state))
                                 :start-offset start-offset
                                 :end-offset end-offset})))]
    (:blocks
     (flush-paragraph
      (reduce (fn [state {:keys [text start-offset end-offset] :as line}]
                (cond
                  (str/blank? text)
                  (-> state flush-paragraph (assoc :list-stack {}))

                  :else
                  (let [heading-match (re-matches heading-pattern text)
                        unordered-match (re-matches unordered-list-pattern text)
                        ordered-match (re-matches ordered-list-pattern text)
                        list-match (or unordered-match ordered-match)]
                    (cond
                      heading-match
                      (let [[_ markers heading-text] heading-match
                            level (count markers)
                            state' (-> state flush-paragraph (assoc :list-stack {}))
                            parent-path (heading-parent (:heading-stack state') level)
                            emitted (emit-block state'
                                                :markdown/heading
                                                parent-path
                                                (or heading-text "")
                                                start-offset
                                                end-offset)]
                        (assoc emitted
                               :heading-stack (assoc (prune-heading-stack (:heading-stack state') level)
                                                     level
                                                     (:last-block-path emitted))))

                      list-match
                      (let [[_ indent item-text] list-match
                            indent-width (count indent)
                            state' (flush-paragraph state)
                            parent-path (list-parent (:heading-stack state')
                                                     (:list-stack state')
                                                     indent-width)
                            emitted (emit-block state'
                                                :markdown/list-item
                                                parent-path
                                                (or item-text "")
                                                start-offset
                                                end-offset)]
                        (assoc emitted
                               :list-stack (update-list-stack (:list-stack emitted)
                                                              indent-width
                                                              (:last-block-path emitted))))

                      :else
                      (-> state
                          (assoc :list-stack {})
                          (start-or-extend-paragraph line))))))
              {:blocks []
               :next-idx 0
               :heading-stack {}
               :list-stack {}
               :paragraph nil}
              (markdown-source-lines raw-text))))))

(defn parent-block-slot-id
  [object-key parent-block-path]
  (when parent-block-path
    (derived-unit-id object-key parent-block-path)))

(defn source-materialization
  [request]
  (let [payload (request-payload request)
        source-ref (payload-source-ref payload)
        source-hash-value (payload-source-hash payload)
        raw-text (str (payload-source-raw-text payload))
        source-ref-key (source-ref-key source-ref)
        object-key (object-key-for source-ref source-hash-value)
        source-id (source-id-for-object-key object-key)
        document-id (document-id-for-object-key object-key)
        event-id (event-id-for-request object-key request)
        created-at (:request/time-ms request)
        created-by (get-in request [:actor :actor/id])
        blocks (markdown-block-v0 raw-text)
        source-event-payload (->SourceIngestedPayload source-id
                                                       source-ref
                                                       source-hash-value
                                                       document-id
                                                       markdown-distiller-id
                                                       markdown-distiller-version
                                                       (count blocks)
                                                       (count blocks))
        event (event-row event-id
                         :source/ingested
                         object-key
                         :source-artifact
                         source-id
                         (:actor request)
                         source-event-payload
                         created-at
                         request)
        source-row (->SourceArtifactRow source-id
                                        source-ref
                                        source-hash-value
                                        (payload-source-format payload)
                                        raw-text
                                        document-id
                                        (long (count (.getBytes raw-text "UTF-8")))
                                        created-at
                                        created-by
                                        event-id)
        version-row (->SourceVersionRow source-ref-key
                                        source-ref
                                        source-hash-value
                                        source-id
                                        document-id
                                        object-key
                                        (fixed-width-order-key created-at (request-id request))
                                        created-at
                                        event-id)
        completion-row (->SourceIngestCompletionRow source-id
                                                    source-ref-key
                                                    source-ref
                                                    source-hash-value
                                                    document-id
                                                    (count blocks)
                                                    (count blocks)
                                                    created-at
                                                    (request-id request)
                                                    event-id)
        document-anchor-id (source-anchor-id document-id)
        document-row (->ObjectContainerRow document-id
                                           :document
                                           object-key
                                           :private
                                           source-id
                                           document-anchor-id
                                           nil
                                           document-id
                                           nil
                                           nil
                                           nil
                                           created-at
                                           created-by
                                           event-id)
        document-anchor (->SourceAnchorRow document-anchor-id
                                           :object-container
                                           document-id
                                           source-id
                                           source-ref
                                           source-hash-value
                                           0
                                           (count raw-text)
                                           nil
                                           event-id)
        unit-rows (mapv (fn [{:keys [block-path unit-local-id parent-block-path unit-kind
                                      text start-offset end-offset]}]
                          (let [unit-id (derived-unit-id object-key block-path)
                                parent-slot-id (parent-block-slot-id object-key parent-block-path)
                                anchor-id (source-anchor-id unit-id)]
                            (->DerivedUnitRow unit-id
                                              document-id
                                              source-id
                                              unit-kind
                                              block-path
                                              parent-slot-id
                                              anchor-id
                                              text
                                              (source-hash text)
                                              markdown-distiller-id
                                              markdown-distiller-version
                                              event-id)))
                        blocks)
        unit-anchor-rows (mapv (fn [{:keys [block-path text start-offset end-offset]}]
                                 (let [unit-id (derived-unit-id object-key block-path)]
                                   (->SourceAnchorRow (source-anchor-id unit-id)
                                                      :derived-unit
                                                      unit-id
                                                      source-id
                                                      source-ref
                                                      source-hash-value
                                                      start-offset
                                                      end-offset
                                                      block-path
                                                      event-id)))
                               blocks)
        outline-rows (mapv (fn [{:keys [block-path parent-block-path text]}]
                             (let [unit-id (derived-unit-id object-key block-path)
                                   parent-slot-id (parent-block-slot-id object-key parent-block-path)
                                   anchor-id (source-anchor-id unit-id)]
                               (->OutlineNodeRow document-id
                                                 unit-id
                                                 block-path
                                                 parent-slot-id
                                                 :derived-unit
                                                 unit-id
                                                 anchor-id
                                                 text
                                                 (source-hash text)
                                                 false
                                                 nil
                                                 event-id)))
                           blocks)
        edge-rows (mapv (fn [{:keys [block-path parent-block-path]}]
                          (let [unit-id (derived-unit-id object-key block-path)
                                parent-slot-id (parent-block-slot-id object-key parent-block-path)
                                parent-id (or parent-slot-id document-id)
                                parent-target-kind (if parent-slot-id
                                                     :derived-unit
                                                     :object-container)
                                edge-id (composition-edge-id object-key parent-id block-path)]
                            (->CompositionEdgeRow edge-id
                                                  object-key
                                                  document-id
                                                  parent-id
                                                  unit-id
                                                  block-path
                                                  parent-target-kind
                                                  parent-id
                                                  :derived-unit
                                                  unit-id
                                                  event-id)))
                        blocks)]
    {:object-key object-key
     :source-ref-key source-ref-key
     :source-hash source-hash-value
     :source-id source-id
     :document-id document-id
     :event event
     :source-row source-row
     :version-row version-row
     :completion-row completion-row
     :document-row document-row
     :document-anchor-row document-anchor
     :unit-rows unit-rows
     :unit-anchor-rows unit-anchor-rows
     :outline-rows outline-rows
     :edge-rows edge-rows}))

(defn materialization-object-key [m] (:object-key m))
(defn materialization-source-ref-key [m] (:source-ref-key m))
(defn materialization-source-hash [m] (:source-hash m))
(defn materialization-source-row [m] (:source-row m))
(defn materialization-source-id [m] (:source-id m))
(defn materialization-version-row [m] (:version-row m))
(defn materialization-completion-row [m] (:completion-row m))
(defn materialization-document-row [m] (:document-row m))
(defn materialization-document-id [m] (:document-id m))
(defn materialization-document-anchor-row [m] (:document-anchor-row m))
(defn materialization-event [m] (:event m))
(defn materialization-unit-rows [m] (:unit-rows m))
(defn materialization-unit-anchor-rows [m] (:unit-anchor-rows m))
(defn materialization-outline-rows [m] (:outline-rows m))
(defn materialization-edge-rows [m] (:edge-rows m))

(defn row-source-id [row] (:source-id row))
(defn row-source-ref-key [row] (:source-ref-key row))
(defn row-source-hash [row] (:source-hash row))
(defn row-document-container-id [row] (:document-container-id row))
(defn row-container-id [row] (:container-id row))
(defn row-current-revision-id [row] (:current-revision-id row))
(defn row-current-content-text [row] (:current-content-text row))
(defn row-current-content-hash [row] (:current-content-hash row))
(defn row-source-unit-id [row] (:source-unit-id row))
(defn row-revision-id [row] (:revision-id row))
(defn row-order-key [row] (:order-key row))
(defn row-unit-id [row] (:unit-id row))
(defn row-source-anchor-id [row] (:source-anchor-id row))
(defn row-block-path [row] (:block-path row))
(defn row-parent-slot-id [row] (:parent-slot-id row))
(defn row-target-id [row] (:target-id row))
(defn row-edge-id [row] (:edge-id row))
(defn row-child-slot-id [row] (:child-slot-id row))
(defn row-child-order-key [row] (:child-order-key row))
(defn row-lineage-key [row] (:lineage-key row))
(defn row-edit-client-id [row] (:edit-client-id row))
(defn row-edit-seq [row] (:edit-seq row))
(defn row-event-id [row] (:event-id row))
(defn edge-parent-slot-id [row] (:parent-slot-id row))
(defn edge-child-order-key [row] (:child-order-key row))

(defn event-id-from-row [row] (:event-id row))
(defn decision-row-id [row] (:decision-id row))

(defn latest-source-id
  [version-row]
  (:source-id version-row))

(defn latest-source-present?
  [version-row]
  (some? version-row))

(defn unit-read-result
  [unit graduation]
  (if unit
    (if graduation
      (->UnitReadResult unit
                        graduation
                        :object-container
                        (:container-id graduation)
                        (:current-content-text graduation)
                        (:current-content-hash graduation))
      (->UnitReadResult unit
                        nil
                        :derived-unit
                        (:unit-id unit)
                        (:derived-content-text unit)
                        (:derived-content-hash unit)))
    nil))

(defn edit-lineage-key
  [request derived-unit graduation container]
  (or (payload-edit-lineage-key (request-payload request))
      (some-> derived-unit :unit-id)
      (some-> container :source-unit-id)
      (some-> graduation :unit-id)
      (get-in request [:target :target/id])))

(defn stale-edit?
  [request last-edit-order]
  (let [seq (payload-edit-seq (request-payload request))
        last-seq (:edit-seq last-edit-order)]
    (and (some? last-edit-order)
         (not= (request-idempotency-key request) (:idempotency-key last-edit-order))
         (<= (long seq) (long last-seq)))))

(defn target-not-found?
  [request derived-unit container]
  (let [target-kind (get-in request [:target :target/kind])]
    (case target-kind
      :derived-unit (nil? derived-unit)
      :object-container (nil? container)
      true)))

(defn edit-effects
  [request derived-unit graduation container source-anchor outline-node child-edge last-edit-order]
  (let [validation-errors (edit-request-validation-errors request)
        target-kind (get-in request [:target :target/kind])
        payload (request-payload request)
        object-key (payload-object-key payload)
        target-id (get-in request [:target :target/id])
        now (:request/time-ms request)
        created-by (get-in request [:actor :actor/id])
        lineage-key (edit-lineage-key request derived-unit graduation container)
        stale? (stale-edit? request last-edit-order)
        not-found? (target-not-found? request derived-unit container)]
    (cond
      (seq validation-errors)
      {:decision (rejected-decision-row request :request-invalid validation-errors)}

      not-found?
      {:decision (rejected-decision-row request :target/not-found
                                        [{:type :target/not-found
                                          :target-kind target-kind
                                          :target-id target-id}])}

      stale?
      {:decision (rejected-decision-row request :edit/stale
                                        [{:type :edit/stale
                                          :edit-client-id (payload-edit-client-id payload)
                                          :edit-seq (payload-edit-seq payload)
                                          :last-seq (:edit-seq last-edit-order)}])}

      :else
      (let [graduating? (and (= :derived-unit target-kind) (nil? graduation))
            existing-container (if (= :object-container target-kind)
                                 container
                                 (when graduation container))
            source-unit-id (or (:unit-id derived-unit)
                               (:source-unit-id existing-container)
                               (:unit-id graduation))
            document-id (payload-document-container-id payload)
            unit-local-id (or (some-> source-unit-id (str/split #":") last)
                              (str "standalone-" target-id))
            container-id (if graduating?
                           (block-container-id object-key unit-local-id)
                           (:container-id existing-container))
            revision-id (revision-id-for-request object-key request)
            parent-revision-id (:current-revision-id existing-container)
            event-id (event-id-for-request object-key request)
            content-text (str (payload-content-text payload))
            content-hash (payload-content-hash payload)
            container-anchor-id (source-anchor-id container-id)
            event-type (if graduating? :object/graduated :object/revised)
            event-payload (if graduating?
                            (->ObjectGraduatedPayload source-unit-id
                                                      container-id
                                                      document-id
                                                      revision-id
                                                      content-hash
                                                      (payload-edit-client-id payload)
                                                      (payload-edit-seq payload))
                            (->ObjectRevisedPayload container-id
                                                    source-unit-id
                                                    document-id
                                                    parent-revision-id
                                                    revision-id
                                                    content-hash
                                                    (payload-edit-client-id payload)
                                                    (payload-edit-seq payload)))
            event (event-row event-id
                             event-type
                             object-key
                             :object-container
                             container-id
                             (:actor request)
                             event-payload
                             now
                             request)
            container-row (->ObjectContainerRow container-id
                                                (if (= document-id container-id)
                                                  :document
                                                  :text-block)
                                                object-key
                                                :private
                                                (or (:source-id existing-container)
                                                    (:source-id derived-unit))
                                                container-anchor-id
                                                source-unit-id
                                                document-id
                                                revision-id
                                                content-text
                                                content-hash
                                                (or (:created-at-ms existing-container) now)
                                                (or (:created-by existing-container) created-by)
                                                event-id)
            revision-row (->RevisionRow revision-id
                                        container-id
                                        parent-revision-id
                                        content-text
                                        content-hash
                                        (fixed-width-order-key now (request-id request))
                                        now
                                        created-by
                                        event-id)
            graduation-row (when source-unit-id
                             (->UnitGraduationRow source-unit-id
                                                  container-id
                                                  revision-id
                                                  content-text
                                                  content-hash
                                                  (or (:graduated-at-ms graduation) now)
                                                  now
                                                  event-id))
            copied-anchor-row (when source-anchor
                                (assoc source-anchor
                                       :source-anchor-id container-anchor-id
                                       :target-kind :object-container
                                       :target-id container-id
                                       :event-id event-id))
            outline-row (when outline-node
                          (assoc outline-node
                                 :target-kind :object-container
                                 :target-id container-id
                                 :content-text content-text
                                 :content-hash content-hash
                                 :graduated true
                                 :container-id container-id
                                 :event-id event-id))
            edge-row (when child-edge
                       (assoc child-edge
                              :child-target-kind :object-container
                              :child-target-id container-id
                              :event-id event-id))
            edit-order-row (->EditOrderRow lineage-key
                                           (payload-edit-client-id payload)
                                           (payload-edit-seq payload)
                                           (request-idempotency-key request)
                                           (request-id request)
                                           revision-id
                                           event-id
                                           now)
            decision (accepted-decision-row request event)]
        {:decision decision
         :event event
         :container-row container-row
         :revision-row revision-row
         :graduation-row graduation-row
         :copied-anchor-row copied-anchor-row
         :outline-row outline-row
         :edge-row edge-row
         :edit-order-row edit-order-row}))))

(defn effect-decision [effects] (:decision effects))
(defn effect-event [effects] (:event effects))
(defn effect-container-row [effects] (:container-row effects))
(defn effect-revision-row [effects] (:revision-row effects))
(defn effect-graduation-row [effects] (:graduation-row effects))
(defn effect-copied-anchor-row [effects] (:copied-anchor-row effects))
(defn effect-outline-row [effects] (:outline-row effects))
(defn effect-edge-row [effects] (:edge-row effects))
(defn effect-edit-order-row [effects] (:edit-order-row effects))
(defn effect-has-event? [effects] (some? (:event effects)))
(defn effect-has-graduation? [effects] (some? (:graduation-row effects)))
(defn effect-has-anchor? [effects] (some? (:copied-anchor-row effects)))
(defn effect-has-outline? [effects] (some? (:outline-row effects)))
(defn effect-has-edge? [effects] (some? (:edge-row effects)))

(defn source-ingest-request
  ([raw-text source-ref]
   (source-ingest-request raw-text source-ref {}))
  ([raw-text source-ref opts]
   (let [raw-text (str raw-text)
         source-hash (or (:source/hash opts)
                         (:source-hash opts)
                         (source-hash raw-text))
         source-ref-key (source-ref-key source-ref)
         object-key (object-key-for source-ref source-hash)
         source-id (source-id-for-object-key object-key)
         request-id (or (:request/id opts)
                        (:request-id opts)
                        (core/random-id "req"))
         payload (->SourceIngestPayload source-ref
                                        source-hash
                                        raw-text
                                        :markdown
                                        markdown-distiller-id
                                        markdown-distiller-version)]
     (assoc (core/action-request
              {:request-id request-id
               :request-type :source/ingest
               :time-ms (:time-ms opts)
               :actor (or (:actor opts)
                          {:actor/id "system"
                           :actor/type :system
                           :actor/capabilities #{:source/ingest :object/edit}})
               :branch (:branch opts)
               :context (:context opts)
               :target {:target/kind :source-artifact
                        :target/id source-id
                        :target/address {:source/ref source-ref
                                         :source/hash source-hash}}
               :action {:action/type :source/ingest
                        :action/capability :source/ingest
                        :action/params {:source/format :markdown}}
               :routing/key [:source-ref source-ref-key]
               :payload payload
               :causal (:causal opts)
               :provenance (or (:provenance opts)
                               {:source/type :manual
                                :source/ref source-ref})})
            :partition/key source-ref-key
            :idempotency/key (or (:idempotency/key opts)
                                 (:idempotency-key opts)
                                 (str "source/ingest:" source-ref-key ":" source-hash))
            :object/key object-key))))

(defn object-edit-request
  ([target-kind target-id content-text]
   (object-edit-request target-kind target-id content-text {}))
  ([target-kind target-id content-text opts]
   (let [content-text (str content-text)
         object-key (or (:object/key opts)
                        (:object-key opts)
                        (extract-object-key target-id))
         document-id (or (:document/container-id opts)
                         (:document-container-id opts)
                         (document-id-for-object-key object-key))
         request-id (or (:request/id opts)
                        (:request-id opts)
                        (core/random-id "req"))
         payload (->ObjectEditPayload document-id
                                      object-key
                                      content-text
                                      (or (:content/hash opts)
                                          (:content-hash opts)
                                          (source-hash content-text))
                                      (or (:edit/client-id opts)
                                          (:edit-client-id opts)
                                          "default")
                                      (long (or (:edit/seq opts)
                                                (:edit-seq opts)
                                                0))
                                      (or (:edit/lineage-key opts)
                                          (:edit-lineage-key opts))
                                      (or (:revision/id opts)
                                          (:revision-id opts)))]
     (assoc (core/action-request
              {:request-id request-id
               :request-type :object/edit
               :time-ms (:time-ms opts)
               :actor (or (:actor opts)
                          {:actor/id "system"
                           :actor/type :system
                           :actor/capabilities #{:source/ingest :object/edit}})
               :branch (:branch opts)
               :context (:context opts)
               :target {:target/kind target-kind
                        :target/id target-id
                        :target/address {:object/key object-key
                                         :document/container-id document-id}}
               :action {:action/type :object/edit
                        :action/capability :object/edit
                        :action/params {:edit/client-id (:edit-client-id payload)
                                        :edit/seq (:edit-seq payload)}}
               :routing/key [:object-container object-key]
               :payload payload
               :causal (:causal opts)
               :provenance (or (:provenance opts)
                               {:source/type :manual
                                :source/ref nil})})
            :partition/key object-key
            :idempotency/key (or (:idempotency/key opts)
                                 (:idempotency-key opts)
                                 (str "object/edit:" object-key ":" target-id ":" request-id))))))

(defmodule object-container-module [setup topologies]
  (declare-depot setup *object-container-requests-depot (hash-by :partition/key))
  (let [s (stream-topology topologies "object-container-topology")]
    (declare-pstate s $$requests-by-audit-id {String ObjectContainerRequestRow}
                    {:key-partitioner partition-by-audit-id})
    (declare-pstate s $$decisions-by-audit-id {String ObjectContainerDecisionRow}
                    {:key-partitioner partition-by-audit-id})
    (declare-pstate s $$decisions-by-idempotency
                    {String (map-schema String ObjectContainerDecisionRow {:subindex? true})})
    (declare-pstate s $$events-by-id {String ObjectContainerEventRow}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$source-artifacts-by-id {String SourceArtifactRow}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$source-versions-by-ref
                    {String (map-schema String SourceVersionRow {:subindex? true})})
    (declare-pstate s $$source-latest-by-ref {String SourceVersionRow})
    (declare-pstate s $$source-ingest-completions-by-ref
                    {String (map-schema String SourceIngestCompletionRow {:subindex? true})})
    (declare-pstate s $$containers-by-id {String ObjectContainerRow}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$revision-history-by-container
                    {String (map-schema String RevisionRow {:subindex? true})}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$derived-units-by-id {String DerivedUnitRow}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$unit-graduations-by-id {String UnitGraduationRow}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$source-anchors-by-target {String SourceAnchorRow}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$composition-children-by-parent
                    {String (map-schema String CompositionEdgeRow {:subindex? true})}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$composition-parent-by-child {String CompositionEdgeRow}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$outline-by-document
                    {String (map-schema String OutlineNodeRow {:subindex? true})}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$edit-order-by-target
                    {String (map-schema String EditOrderRow {:subindex? true})}
                    {:key-partitioner partition-by-object-key})

    (<<sources s
      (source> *object-container-requests-depot {:retry-mode :all-after} :> *request)
      (request-id *request :> *request-id)
      (request-type *request :> *request-type)
      (request-partition-key *request :> *partition-key)
      (request-idempotency-key *request :> *idempotency-key)
      (request-audit-id *request :> *audit-id)
      (decision-id-for-audit-id *audit-id :> *audit-decision-id)
      (local-select> [(keypath *audit-decision-id)] $$decisions-by-audit-id
                     :> *prior-audit-decision)

      (<<if (some? *prior-audit-decision)
        (ack-return> *prior-audit-decision)
        (else>)
        (request-row *request :> *request-row)
        (local-transform> [(keypath *audit-id) (termval *request-row)] $$requests-by-audit-id)
        (local-select> [(keypath *partition-key *idempotency-key)]
                       $$decisions-by-idempotency :> *prior-decision)

        (<<if (some? *prior-decision)
          (replay-decision-row *request *prior-decision :> *replay-decision)
          (decision-row-id *replay-decision :> *replay-decision-id)
          (local-transform> [(keypath *replay-decision-id) (termval *replay-decision)]
                            $$decisions-by-audit-id)
          (ack-return> *replay-decision)
          (else>)
          (<<cond
            (case> (= :source/ingest *request-type))
            (source-request-validation-errors *request :> *source-errors)
            (<<if (source-request-valid? *source-errors)
              (request-payload *request :> *source-payload)
              (payload-source-hash *source-payload :> *source-hash)
              (local-select> [(keypath *partition-key *source-hash)]
                             $$source-ingest-completions-by-ref :> *completion)
            (<<if (some? *completion)
              (completed-source-decision-row *request *completion :> *completed-decision)
              (decision-row-id *completed-decision :> *completed-decision-id)
              (local-transform> [(keypath *completed-decision-id) (termval *completed-decision)]
                                $$decisions-by-audit-id)
              (local-transform> [(keypath *partition-key *idempotency-key)
                                  (termval *completed-decision)]
                                $$decisions-by-idempotency)
              (ack-return> *completed-decision)
              (else>)
              (source-materialization *request :> *materialization)
              (materialization-object-key *materialization :> *object-key)
              (materialization-source-row *materialization :> *source-row)
              (materialization-source-id *materialization :> *source-id)
              (materialization-document-row *materialization :> *document-row)
              (materialization-document-id *materialization :> *document-id)
              (materialization-document-anchor-row *materialization :> *document-anchor-row)
              (materialization-event *materialization :> *event-row)
              (event-id-from-row *event-row :> *event-id)
              (|hash *object-key)
              (local-transform> [(keypath *source-id) (termval *source-row)]
                                $$source-artifacts-by-id)
              (local-transform> [(keypath *document-id) (termval *document-row)]
                                $$containers-by-id)
              (local-transform> [(keypath *document-id) (termval *document-anchor-row)]
                                $$source-anchors-by-target)
              (local-transform> [(keypath *event-id) (termval *event-row)]
                                $$events-by-id)
              (materialization-unit-rows *materialization :> *unit-rows)
              (loop<- [*remaining-unit-rows *unit-rows :> *unit-write-done]
                (<<if (empty? *remaining-unit-rows)
                  (:> true)
                  (else>)
                  (first *remaining-unit-rows :> *unit-row)
                  (row-unit-id *unit-row :> *unit-id)
                  (local-transform> [(keypath *unit-id) (termval *unit-row)]
                                    $$derived-units-by-id)
                  (continue> (rest *remaining-unit-rows))))
              (materialization-unit-anchor-rows *materialization :> *unit-anchor-rows)
              (loop<- [*remaining-unit-anchor-rows *unit-anchor-rows :> *unit-anchor-write-done]
                (<<if (empty? *remaining-unit-anchor-rows)
                  (:> true)
                  (else>)
                  (first *remaining-unit-anchor-rows :> *unit-anchor-row)
                  (row-target-id *unit-anchor-row :> *anchor-target-id)
                  (local-transform> [(keypath *anchor-target-id) (termval *unit-anchor-row)]
                                    $$source-anchors-by-target)
                  (continue> (rest *remaining-unit-anchor-rows))))
              (materialization-outline-rows *materialization :> *outline-rows)
              (loop<- [*remaining-outline-rows *outline-rows :> *outline-write-done]
                (<<if (empty? *remaining-outline-rows)
                  (:> true)
                  (else>)
                  (first *remaining-outline-rows :> *outline-row)
                  (row-block-path *outline-row :> *outline-key)
                  (local-transform> [(keypath *document-id *outline-key)
                                      (termval *outline-row)]
                                    $$outline-by-document)
                  (continue> (rest *remaining-outline-rows))))
              (materialization-edge-rows *materialization :> *edge-rows)
              (loop<- [*remaining-edge-rows *edge-rows :> *edge-write-done]
                (<<if (empty? *remaining-edge-rows)
                  (:> true)
                  (else>)
                  (first *remaining-edge-rows :> *edge-row)
                  (row-edge-id *edge-row :> *edge-id)
                  (edge-parent-slot-id *edge-row :> *edge-parent-slot-id)
                  (edge-child-order-key *edge-row :> *edge-child-order-key)
                  (row-child-slot-id *edge-row :> *edge-child-slot-id)
                  (local-transform> [(keypath *edge-parent-slot-id *edge-child-order-key)
                                      (termval *edge-row)]
                                    $$composition-children-by-parent)
                  (local-transform> [(keypath *edge-child-slot-id) (termval *edge-row)]
                                    $$composition-parent-by-child)
                  (continue> (rest *remaining-edge-rows))))
              (materialization-version-row *materialization :> *version-row)
              (materialization-completion-row *materialization :> *completion-row)
              (materialization-source-ref-key *materialization :> *source-ref-key)
              (materialization-source-hash *materialization :> *materialization-source-hash)
              (accepted-decision-row *request *event-row :> *decision)
              (decision-row-id *decision :> *decision-id)
              (|hash *source-ref-key)
              (local-transform> [(keypath *source-ref-key *materialization-source-hash)
                                  (termval *version-row)]
                                $$source-versions-by-ref)
              (local-transform> [(keypath *source-ref-key) (termval *version-row)]
                                $$source-latest-by-ref)
              (local-transform> [(keypath *source-ref-key *materialization-source-hash)
                                  (termval *completion-row)]
                                $$source-ingest-completions-by-ref)
              (local-transform> [(keypath *decision-id) (termval *decision)]
                                $$decisions-by-audit-id)
              (local-transform> [(keypath *source-ref-key *idempotency-key)
                                  (termval *decision)]
                                $$decisions-by-idempotency)
              (ack-return> *decision))
            (else>)
            (rejected-decision-row *request :request-invalid *source-errors :> *decision)
            (decision-row-id *decision :> *decision-id)
            (local-transform> [(keypath *decision-id) (termval *decision)]
                              $$decisions-by-audit-id)
            (local-transform> [(keypath *partition-key *idempotency-key)
                                (termval *decision)]
                              $$decisions-by-idempotency)
            (ack-return> *decision))

          (case> (= :object/edit *request-type))
          (request-payload *request :> *edit-payload)
          (payload-object-key *edit-payload :> *object-key)
          (get-in *request [:target :target/kind] :> *target-kind)
          (get-in *request [:target :target/id] :> *target-id)
          (|hash *object-key)
          (<<cond
            (case> (= :derived-unit *target-kind))
            (local-select> [(keypath *target-id)] $$derived-units-by-id :> *derived-unit)
            (local-select> [(keypath *target-id)] $$unit-graduations-by-id :> *graduation)
            (<<if (some? *graduation)
              (row-container-id *graduation :> *container-id)
              (local-select> [(keypath *container-id)] $$containers-by-id :> *container)
              (else>)
              (identity nil :> *container))
            (local-select> [(keypath *target-id)] $$source-anchors-by-target :> *source-anchor)
            (row-block-path *derived-unit :> *block-path)
            (payload-document-container-id *edit-payload :> *document-id)
            (local-select> [(keypath *document-id *block-path)]
                           $$outline-by-document :> *outline-node)
            (local-select> [(keypath *target-id)] $$composition-parent-by-child :> *child-edge)
            (edit-lineage-key *request *derived-unit *graduation *container :> *lineage-key)

            (case> (= :object-container *target-kind))
            (identity nil :> *derived-unit)
            (identity nil :> *graduation)
            (local-select> [(keypath *target-id)] $$containers-by-id :> *container)
            (row-source-unit-id *container :> *source-unit-id)
            (local-select> [(keypath *target-id)] $$source-anchors-by-target :> *source-anchor)
            (payload-document-container-id *edit-payload :> *document-id)
            (<<if (some? *source-unit-id)
              (local-select> [(keypath *source-unit-id)] $$derived-units-by-id :> *source-unit)
              (row-block-path *source-unit :> *block-path)
              (local-select> [(keypath *document-id *block-path)]
                             $$outline-by-document :> *outline-node)
              (local-select> [(keypath *source-unit-id)] $$composition-parent-by-child :> *child-edge)
              (else>)
              (identity nil :> *outline-node)
              (identity nil :> *child-edge))
            (edit-lineage-key *request *derived-unit *graduation *container :> *lineage-key)

            (default>)
            (identity nil :> *derived-unit)
            (identity nil :> *graduation)
            (identity nil :> *container)
            (identity nil :> *source-anchor)
            (identity nil :> *outline-node)
            (identity nil :> *child-edge)
            (identity *target-id :> *lineage-key))
          (local-select> [(keypath *lineage-key (payload-edit-client-id *edit-payload))]
                         $$edit-order-by-target :> *last-edit-order)
          (edit-effects *request
                        *derived-unit
                        *graduation
                        *container
                        *source-anchor
                        *outline-node
                        *child-edge
                        *last-edit-order
                        :> *effects)
          (effect-decision *effects :> *decision)
          (decision-row-id *decision :> *decision-id)
          (local-transform> [(keypath *decision-id) (termval *decision)]
                            $$decisions-by-audit-id)
          (local-transform> [(keypath *object-key *idempotency-key)
                              (termval *decision)]
                            $$decisions-by-idempotency)
          (<<if (effect-has-event? *effects)
            (effect-event *effects :> *event)
            (event-id-from-row *event :> *event-id)
            (effect-container-row *effects :> *container-row)
            (row-container-id *container-row :> *container-row-id)
            (effect-revision-row *effects :> *revision-row)
            (row-order-key *revision-row :> *revision-order-key)
            (row-revision-id *revision-row :> *revision-id)
            (effect-edit-order-row *effects :> *edit-order-row)
            (row-lineage-key *edit-order-row :> *edit-lineage-key)
            (row-edit-client-id *edit-order-row :> *edit-client-id)
            (local-transform> [(keypath *event-id) (termval *event)] $$events-by-id)
            (local-transform> [(keypath *container-row-id) (termval *container-row)]
                              $$containers-by-id)
            (local-transform> [(keypath *container-row-id *revision-order-key)
                                (termval *revision-row)]
                              $$revision-history-by-container)
            (local-transform> [(keypath *edit-lineage-key *edit-client-id)
                                (termval *edit-order-row)]
                              $$edit-order-by-target)
            (<<if (effect-has-graduation? *effects)
              (effect-graduation-row *effects :> *graduation-row)
              (row-unit-id *graduation-row :> *graduated-unit-id)
              (local-transform> [(keypath *graduated-unit-id) (termval *graduation-row)]
                                $$unit-graduations-by-id))
            (<<if (effect-has-anchor? *effects)
              (effect-copied-anchor-row *effects :> *copied-anchor-row)
              (row-target-id *copied-anchor-row :> *copied-anchor-target-id)
              (local-transform> [(keypath *copied-anchor-target-id)
                                  (termval *copied-anchor-row)]
                                $$source-anchors-by-target))
            (<<if (effect-has-outline? *effects)
              (effect-outline-row *effects :> *outline-row)
              (row-document-container-id *outline-row :> *outline-document-id)
              (row-block-path *outline-row :> *outline-block-path)
              (local-transform> [(keypath *outline-document-id *outline-block-path)
                                  (termval *outline-row)]
                                $$outline-by-document))
            (<<if (effect-has-edge? *effects)
              (effect-edge-row *effects :> *edge-row)
              (edge-parent-slot-id *edge-row :> *edge-parent-slot-id)
              (edge-child-order-key *edge-row :> *edge-child-order-key)
              (row-child-slot-id *edge-row :> *edge-child-slot-id)
              (local-transform> [(keypath *edge-parent-slot-id *edge-child-order-key)
                                  (termval *edge-row)]
                                $$composition-children-by-parent)
              (local-transform> [(keypath *edge-child-slot-id) (termval *edge-row)]
                                $$composition-parent-by-child)))
          (ack-return> *decision)

          (default>)
          (rejected-decision-row *request :unknown-action-type
                                 [{:type :unknown-action-type :value *request-type}]
                                 :> *decision)
          (decision-row-id *decision :> *decision-id)
          (local-transform> [(keypath *decision-id) (termval *decision)]
                            $$decisions-by-audit-id)
          (local-transform> [(keypath *partition-key *idempotency-key)
                              (termval *decision)]
                            $$decisions-by-idempotency)
          (ack-return> *decision))))))

  (<<query-topology topologies "read-latest-source-by-ref" [*source-ref :> *source-row]
    (source-ref-key *source-ref :> *source-ref-key)
    (|hash *source-ref-key)
    (local-select> [(keypath *source-ref-key)] $$source-latest-by-ref :> *latest)
    (<<if (latest-source-present? *latest)
      (latest-source-id *latest :> *source-id)
      (extract-object-key *source-id :> *object-key)
      (|hash *object-key)
      (local-select> [(keypath *source-id)] $$source-artifacts-by-id :> *source-row)
      (|origin)
      (else>)
      (identity nil :> *source-row)
      (|origin)))

  (<<query-topology topologies "read-unit" [*unit-id :> *result]
    (extract-object-key *unit-id :> *object-key)
    (|hash *object-key)
    (local-select> [(keypath *unit-id)] $$derived-units-by-id :> *unit)
    (<<if (some? *unit)
      (local-select> [(keypath *unit-id)] $$unit-graduations-by-id :> *graduation)
      (unit-read-result *unit *graduation :> *result)
      (|origin)
      (else>)
      (identity nil :> *result)
      (|origin))))

(defn start-object-container-runtime!
  []
  (let [ipc (create-ipc)
        module-name (get-module-name object-container-module)
        launch-opts {:tasks 4 :threads 2}]
    (launch-module! ipc object-container-module launch-opts)
    {:ipc ipc
     :module-name module-name
     :object-container-requests-depot
     (foreign-depot ipc module-name "*object-container-requests-depot")
     :requests-by-audit-id (foreign-pstate ipc module-name "$$requests-by-audit-id")
     :decisions-by-audit-id (foreign-pstate ipc module-name "$$decisions-by-audit-id")
     :decisions-by-idempotency (foreign-pstate ipc module-name "$$decisions-by-idempotency")
     :events-by-id (foreign-pstate ipc module-name "$$events-by-id")
     :source-artifacts-by-id (foreign-pstate ipc module-name "$$source-artifacts-by-id")
     :source-versions-by-ref (foreign-pstate ipc module-name "$$source-versions-by-ref")
     :source-latest-by-ref (foreign-pstate ipc module-name "$$source-latest-by-ref")
     :source-ingest-completions-by-ref
     (foreign-pstate ipc module-name "$$source-ingest-completions-by-ref")
     :containers-by-id (foreign-pstate ipc module-name "$$containers-by-id")
     :revision-history-by-container
     (foreign-pstate ipc module-name "$$revision-history-by-container")
     :derived-units-by-id (foreign-pstate ipc module-name "$$derived-units-by-id")
     :unit-graduations-by-id (foreign-pstate ipc module-name "$$unit-graduations-by-id")
     :source-anchors-by-target (foreign-pstate ipc module-name "$$source-anchors-by-target")
     :composition-children-by-parent
     (foreign-pstate ipc module-name "$$composition-children-by-parent")
     :composition-parent-by-child (foreign-pstate ipc module-name "$$composition-parent-by-child")
     :outline-by-document (foreign-pstate ipc module-name "$$outline-by-document")
     :edit-order-by-target (foreign-pstate ipc module-name "$$edit-order-by-target")
     :read-latest-source-by-ref-query
     (foreign-query ipc module-name "read-latest-source-by-ref")
     :read-unit-query (foreign-query ipc module-name "read-unit")}))

(defn close-object-container-runtime!
  [runtime]
  (when-let [ipc (:ipc runtime)]
    (try
      (.close ipc)
      (catch Exception _ nil))))

(defn append-object-container-request!
  ([runtime request]
   (append-object-container-request! runtime request :append-ack))
  ([runtime request ack-level]
   (foreign-append! (:object-container-requests-depot runtime) request ack-level)))

(defn foreign-one
  [pstate path]
  (foreign-select-one path pstate))

(defn read-request
  ([runtime request]
   (read-request runtime (request-partition-key request) (request-id request)))
  ([runtime partition-key request-id]
   (foreign-one (:requests-by-audit-id runtime)
                [(keypath (audit-id partition-key request-id))])))

(defn read-audit-request
  ([runtime request]
   (read-request runtime request))
  ([runtime partition-key request-id]
   (read-request runtime partition-key request-id)))

(defn read-decision
  ([runtime request]
   (read-decision runtime (request-partition-key request) (request-id request)))
  ([runtime partition-key request-id]
   (foreign-one (:decisions-by-audit-id runtime)
                [(keypath (decision-id-for-audit-id (audit-id partition-key request-id)))])))

(defn read-source
  ([runtime source-id]
   (foreign-one (:source-artifacts-by-id runtime) [(keypath source-id)]))
  ([runtime source-ref source-hash]
   (read-source runtime (source-id-for source-ref source-hash))))

(defn read-latest-source-by-ref
  [runtime source-ref]
  (foreign-invoke-query (:read-latest-source-by-ref-query runtime) source-ref))

(defn read-outline
  ([runtime document-id]
   (read-outline runtime document-id "" default-outline-page-size))
  ([runtime document-id cursor limit]
   (foreign-select [(keypath document-id)
                    (sorted-map-range-from (or cursor "") (or limit default-outline-page-size))
                    MAP-VALS]
                   (:outline-by-document runtime))))

(defn read-container
  [runtime container-id]
  (foreign-one (:containers-by-id runtime) [(keypath container-id)]))

(defn read-revision-history
  ([runtime container-id]
   (read-revision-history runtime container-id "" default-outline-page-size))
  ([runtime container-id cursor limit]
   (foreign-select [(keypath container-id)
                    (sorted-map-range-from (or cursor "") (or limit default-outline-page-size))
                    MAP-VALS]
                   (:revision-history-by-container runtime))))

(defn read-unit
  [runtime unit-id]
  (foreign-invoke-query (:read-unit-query runtime) unit-id))

(defn await-object-container-decision
  ([runtime request]
   (await-object-container-decision runtime request 2000))
  ([runtime request timeout-ms]
   (let [partition-key (request-partition-key request)
         request-id (request-id request)]
     (await-object-container-decision runtime partition-key request-id timeout-ms)))
  ([runtime partition-key request-id timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop [decision (read-decision runtime partition-key request-id)]
       (cond
         (some? decision) decision
         (>= (System/currentTimeMillis) deadline) decision
         :else (do
                 (Thread/sleep 25)
                 (recur (read-decision runtime partition-key request-id))))))))
