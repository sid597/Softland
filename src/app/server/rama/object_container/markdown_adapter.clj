(ns app.server.rama.object-container.markdown-adapter
  (:require [app.server.rama.core :as core]
            [app.server.rama.object-container :as oc]
            [clojure.string :as str]))

(def markdown-distiller-id "markdown-block-v0")
(def markdown-distiller-version 1)

(def heading-pattern #"^\s{0,3}(#{1,6})(?:\s+(.*?)\s*|\s*)$")
(def unordered-list-pattern #"^(\s*)[-*+]\s+(.*?)\s*$")
(def ordered-list-pattern #"^(\s*)\d+[\.)]\s+(.*?)\s*$")

(defn derived-unit-id
  [object-key block-path]
  (str "du:" object-key ":" markdown-distiller-id ":" block-path))

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
  (let [payload (oc/request-payload request)
        source-ref (oc/payload-source-ref payload)
        source-hash-value (oc/payload-source-hash payload)
        raw-text (str (oc/payload-source-raw-text payload))
        source-ref-key (oc/source-ref-key source-ref)
        object-key (oc/object-key-for source-ref source-hash-value)
        source-id (oc/source-id-for-object-key object-key)
        document-id (oc/document-id-for-object-key object-key)
        event-id (oc/event-id-for-request object-key request)
        created-at (:request/time-ms request)
        created-by (get-in request [:actor :actor/id])
        blocks (markdown-block-v0 raw-text)
        source-event-payload (oc/->SourceIngestedPayload source-id
                                                         source-ref
                                                         source-hash-value
                                                         document-id
                                                         markdown-distiller-id
                                                         markdown-distiller-version
                                                         (count blocks)
                                                         (count blocks))
        event (oc/event-row event-id
                            :source/ingested
                            object-key
                            :source-artifact
                            source-id
                            (:actor request)
                            source-event-payload
                            created-at
                            request)
        source-row (oc/->SourceArtifactRow source-id
                                           source-ref
                                           source-hash-value
                                           (oc/payload-source-format payload)
                                           raw-text
                                           document-id
                                           (long (count (.getBytes raw-text "UTF-8")))
                                           created-at
                                           created-by
                                           event-id)
        version-row (oc/->SourceVersionRow source-ref-key
                                           source-ref
                                           source-hash-value
                                           source-id
                                           document-id
                                           object-key
                                           (oc/fixed-width-order-key created-at (oc/request-id request))
                                           created-at
                                           event-id)
        completion-row (oc/->SourceIngestCompletionRow source-id
                                                       source-ref-key
                                                       source-ref
                                                       source-hash-value
                                                       document-id
                                                       (count blocks)
                                                       (count blocks)
                                                       created-at
                                                       (oc/request-id request)
                                                       event-id)
        document-anchor-id (oc/source-anchor-id document-id)
        document-revision-id (oc/import-revision-id object-key
                                                    document-id
                                                    source-hash-value)
        document-revision-order-key (oc/fixed-width-order-key created-at
                                                              (oc/request-id request))
        document-revision (oc/->RevisionRow document-revision-id
                                            document-id
                                            nil
                                            raw-text
                                            source-hash-value
                                            document-revision-order-key
                                            created-at
                                            created-by
                                            event-id)
        document-row (oc/->ObjectContainerRow document-id
                                              :document
                                              object-key
                                              :private
                                              source-id
                                              document-anchor-id
                                              nil
                                              document-id
                                              document-revision-id
                                              raw-text
                                              source-hash-value
                                              created-at
                                              created-by
                                              event-id)
        document-anchor (oc/->SourceAnchorRow document-anchor-id
                                              :object-container
                                              document-id
                                              source-id
                                              source-ref
                                              source-hash-value
                                              0
                                              (count raw-text)
                                              nil
                                              event-id)
        unit-rows (mapv (fn [{:keys [block-path parent-block-path unit-kind
                                      text start-offset end-offset]}]
                          (let [unit-id (derived-unit-id object-key block-path)
                                parent-slot-id (parent-block-slot-id object-key parent-block-path)
                                anchor-id (oc/source-anchor-id unit-id)]
                            (oc/->DerivedUnitRow unit-id
                                                 document-id
                                                 source-id
                                                 unit-kind
                                                 block-path
                                                 parent-slot-id
                                                 anchor-id
                                                 text
                                                 (oc/source-hash text)
                                                 markdown-distiller-id
                                                 markdown-distiller-version
                                                 event-id)))
                        blocks)
        unit-anchor-rows (mapv (fn [{:keys [block-path text start-offset end-offset]}]
                                 (let [unit-id (derived-unit-id object-key block-path)]
                                   (oc/->SourceAnchorRow (oc/source-anchor-id unit-id)
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
                                   anchor-id (oc/source-anchor-id unit-id)]
                               (oc/->OutlineNodeRow document-id
                                                    unit-id
                                                    block-path
                                                    parent-slot-id
                                                    :derived-unit
                                                    unit-id
                                                    anchor-id
                                                    text
                                                    (oc/source-hash text)
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
                                edge-id (oc/composition-edge-id object-key parent-id block-path)]
                            (oc/->CompositionEdgeRow edge-id
                                                     object-key
                                                     document-id
                                                     parent-id
                                                     unit-id
                                                     block-path
                                                     parent-target-kind
                                                     parent-id
                                                     :derived-unit
                                                     unit-id
                                                     source-id
                                                     nil
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
     :document-revision-row document-revision
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
(defn materialization-document-revision-row [m] (:document-revision-row m))
(defn materialization-document-id [m] (:document-id m))
(defn materialization-document-anchor-row [m] (:document-anchor-row m))
(defn materialization-event [m] (:event m))
(defn materialization-unit-rows [m] (:unit-rows m))
(defn materialization-unit-anchor-rows [m] (:unit-anchor-rows m))
(defn materialization-outline-rows [m] (:outline-rows m))
(defn materialization-edge-rows [m] (:edge-rows m))

(defn- source-ingest-kernel-request
  ([raw-text source-ref]
   (source-ingest-kernel-request raw-text source-ref {}))
  ([raw-text source-ref opts]
   (let [raw-text (str raw-text)
         source-hash (or (:source/hash opts)
                         (:source-hash opts)
                         (oc/source-hash raw-text))
         source-ref-key (oc/source-ref-key source-ref)
         object-key (oc/object-key-for source-ref source-hash)
         source-id (oc/source-id-for-object-key object-key)
         request-id (or (:request/id opts)
                        (:request-id opts)
                        (core/random-id "req"))
         idempotency-key (or (:idempotency/key opts)
                             (:idempotency-key opts)
                             (str "source/ingest:" source-ref-key ":" source-hash))
         payload (oc/->SourceIngestPayload source-ref
                                           source-hash
                                           raw-text
                                           :markdown
                                           markdown-distiller-id
                                           markdown-distiller-version)
         material-fingerprint (core/sha-256
                               (pr-str {:request/type :source/ingest
                                        :partition/key source-ref-key
                                        :object/key object-key
                                        :source/ref source-ref
                                        :source/hash source-hash
                                        :source/format :markdown
                                        :distiller/id markdown-distiller-id
                                        :distiller/version markdown-distiller-version}))]
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
            :idempotency/key idempotency-key
            :object/key object-key
            :material/fingerprint material-fingerprint))))

(defn markdown-import-key
  [object-key source-ref-key source-hash]
  (str "imp:md:" object-key ":" (core/sha-256 (str source-ref-key ":" source-hash))))

(defn markdown-import-payload
  [materialization]
  {:object-key (:object-key materialization)
   :source-ref (:source-ref (:source-row materialization))
   :source-hash (:source-hash materialization)
   :source-raw-text (:source-raw-text (:source-row materialization))
   :source-format (:source-format (:source-row materialization))
   :source-artifacts [(:source-row materialization)]
   :object-containers [(:document-row materialization)]
   :revisions [(:document-revision-row materialization)]
   :derived-units (:unit-rows materialization)
   :source-anchors (into [(:document-anchor-row materialization)]
                         (:unit-anchor-rows materialization))
   :composition-edges (:edge-rows materialization)
   :source-versions [(:version-row materialization)]
   :projection-hints (mapv #(assoc % :projection-kind :markdown-outline)
                           (:outline-rows materialization))})

(defn markdown-source-import-request
  ([raw-text source-ref]
   (markdown-source-import-request raw-text source-ref {}))
  ([raw-text source-ref opts]
   (let [legacy-request (source-ingest-kernel-request raw-text source-ref opts)
         materialization (source-materialization legacy-request)
         object-key (:object-key materialization)
         source-ref-key (:source-ref-key materialization)
         source-hash-value (:source-hash materialization)
         source-row (:source-row materialization)
         import-key (or (:import/key opts)
                        (:import-key opts)
                        (markdown-import-key object-key source-ref-key source-hash-value))
         idempotency-key (or (:idempotency/key opts)
                             (:idempotency-key opts)
                             import-key)
         payload (markdown-import-payload materialization)
         material-fingerprint (oc/import-material-fingerprint object-key import-key payload)
         actor (or (:actor opts)
                   {:actor/id "system"
                    :actor/type :system
                    :actor/capabilities #{:object-container/import-material
                                          :source/ingest
                                          :object/edit}})]
     (assoc (core/action-request
             {:request-id (oc/request-id legacy-request)
              :request-type :object-container/import-material
              :time-ms (:request/time-ms legacy-request)
              :actor actor
              :branch (:branch legacy-request)
              :context (:context legacy-request)
              :target {:target/kind :object-container-import
                       :target/id import-key
                       :target/address {:source/ref source-ref
                                        :source/hash source-hash-value
                                        :object/key object-key}}
              :action {:action/type :object-container/import-material
                       :action/capability :object-container/import-material
                       :action/params {:source/family :markdown
                                       :source/format :markdown}}
              :routing/key [:object-container/import object-key]
              :payload payload
              :causal (:causal legacy-request)
              :provenance (or (:provenance opts)
                              {:source/type :markdown
                               :source/ref source-ref})})
            :partition/key object-key
            :object/key object-key
            :import/key import-key
            :source/family :markdown
            :source/format (:source-format source-row)
            :idempotency/key idempotency-key
            :material/fingerprint material-fingerprint))))

(defn source-ingest-request
  ([raw-text source-ref]
   (source-ingest-request raw-text source-ref {}))
  ([raw-text source-ref opts]
   (markdown-source-import-request raw-text source-ref opts)))
