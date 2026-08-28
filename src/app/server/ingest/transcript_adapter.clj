(ns app.server.ingest.transcript-adapter
  "One redacted transcript line converted into container rows.
   Takes: a parsed JSONL event, source identity, actor data, and claimed time.
   Gives: container, projection, edge, anchor, artifact, tool-call, and audit rows.
   Holds nothing."
  (:require [app.server.rama.envelope :as envelope]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.transcript-identity :as transcript-identity]
            [clojure.string :as str]))

(defn transcript-content-blocks
  [payload]
  (let [content (or (get-in payload [:message :content])
                    (:content payload)
                    [])]
    (if (sequential? content) content [])))

(defn transcript-tool-result-blocks
  [payload]
  (filter #(and (map? %) (#{"tool_result" "tool-result"} (:type %)))
          (transcript-content-blocks payload)))

(defn transcript-tool-use-blocks
  [payload]
  (filter #(and (map? %) (#{"tool_use" "tool-use"} (:type %)))
          (transcript-content-blocks payload)))

(defn transcript-text-content
  [payload]
  (let [content (or (get-in payload [:message :content])
                    (:content payload))]
    (cond
      (string? content) content
      (sequential? content)
      (str/join "\n"
                (keep (fn [block]
                        (when (and (map? block)
                                   (#{"text" "tool_result" "tool-result"} (:type block)))
                          (str (or (:text block) (:content block) ""))))
                      content))
      :else "")))

(defn transcript-role
  [payload]
  (or (get-in payload [:message :role])
      (:role payload)))

(defn transcript-content-preview
  [content]
  (let [s (str (or content ""))]
    (subs s 0 (min 240 (count s)))))

(defn transcript-conversation-projection-row
  [conversation-container-id order-key entry-kind container-id revision-id source-anchor-id
   source-id source-ref source-line-key event-id request-id import-key message-uuid role
   content-preview parse-error-kind]
  (oc/->TranscriptConversationProjectionRow :transcript-conversation-projection
                                            conversation-container-id
                                            order-key
                                            entry-kind
                                            container-id
                                            revision-id
                                            source-anchor-id
                                            source-id
                                            source-ref
                                            source-line-key
                                            event-id
                                            request-id
                                            import-key
                                            message-uuid
                                            role
                                            content-preview
                                            parse-error-kind))

(defn transcript-tool-call-index-row
  [tool-name order-key tool-call-container-id conversation-container-id message-container-id
   source-id source-ref source-line-key event-id request-id import-key]
  (oc/->TranscriptToolCallIndexRow :transcript-tool-call-index
                                   tool-name
                                   order-key
                                   tool-call-container-id
                                   conversation-container-id
                                   message-container-id
                                   source-id
                                   source-ref
                                   source-line-key
                                   event-id
                                   request-id
                                   import-key))

(defn transcript-audit-entry-row
  [request-id order-key entry-kind conversation-container-id container-id source-id
   source-ref source-line-key parse-error-kind event-id import-key message]
  (oc/->TranscriptAuditEntryRow :transcript-audit-entry
                                request-id
                                order-key
                                entry-kind
                                conversation-container-id
                                container-id
                                source-id
                                source-ref
                                source-line-key
                                parse-error-kind
                                event-id
                                import-key
                                message))

(defn transcript-last-message-row
  [conversation-container-id message-container-id source-line-key order-key event-id
   request-id import-key updated-at-ms]
  (oc/->TranscriptLastMessageRow :transcript-last-message
                                 conversation-container-id
                                 message-container-id
                                 source-line-key
                                 order-key
                                 event-id
                                 request-id
                                 import-key
                                 updated-at-ms))

(defn transcript-source-line-status-row
  [obs import-key material-fingerprint source-id source-ref source-line-key order-key
   request-id now]
  (when-let [file-key (transcript-identity/transcript-source-file-key obs)]
    (oc/->TranscriptSourceLineStatusRow file-key
                                        order-key
                                        :observed
                                        import-key
                                        nil
                                        material-fingerprint
                                        (transcript-identity/transcript-file-generation-key obs)
                                        source-line-key
                                        source-id
                                        source-ref
                                        (long (or (:source/byte-offset obs) 0))
                                        (long (or (:source/byte-length obs) 0))
                                        (:source/line-hash obs)
                                        request-id
                                        (:transcript/parse-error-kind obs)
                                        now
                                        nil
                                        nil)))

(defn transcript-anchor-row
  [object-key source-id source-ref source-hash target-kind target-id obs event-id]
  (let [offset (long (or (:source/byte-offset obs) 0))
        byte-length (long (or (:source/byte-length obs) 0))
        anchor-hash (envelope/sha-256 (str source-id ":" target-id ":" (:source/line-hash obs)))]
    (oc/->SourceAnchorRow (str "sa:" object-key ":" (envelope/sha-256 target-id) ":" anchor-hash)
                          target-kind
                          target-id
                          source-id
                          source-ref
                          source-hash
                          offset
                          (+ offset byte-length)
                          (:source/line-hash obs)
                          event-id)))

(defn transcript-container-row
  [container-id kind object-key source-id anchor-id revision-id content-text content-hash
   now actor-id event-id]
  (oc/->ObjectContainerRow container-id
                           kind
                           object-key
                           :private
                           source-id
                           anchor-id
                           nil
                           container-id
                           revision-id
                           content-text
                           content-hash
                           now
                           actor-id
                           event-id))

(defn transcript-revision-row
  [revision-id container-id content-text content-hash order-key now actor-id event-id]
  (oc/->RevisionRow revision-id
                    container-id
                    nil
                    content-text
                    content-hash
                    order-key
                    now
                    actor-id
                    event-id))

(defn transcript-composition-edge
  [object-key edge-kind parent-kind parent-id child-kind child-id order-key source-id
   anchor-id event-id]
  (oc/->CompositionEdgeRow (str "ce:" object-key ":" (name edge-kind) ":"
                                (envelope/sha-256 parent-id) ":" (envelope/sha-256 child-id))
                           object-key
                           parent-id
                           parent-id
                           child-id
                           order-key
                           parent-kind
                           parent-id
                           child-kind
                           child-id
                           source-id
                           anchor-id
                           event-id))

(defn transcript-previous-message-container-id
  [object-key obs opts]
  (or (:transcript/previous-message-container-id obs)
      (:transcript/previous-message-container-id opts)
      (some-> (or (:transcript/previous-message-id obs)
                  (:transcript/previous-message-id opts))
              str)
      (when-let [previous-uuid (or (:transcript/previous-message-uuid obs)
                                   (:transcript/previous-message-uuid opts))]
        (transcript-identity/chat-message-id object-key (envelope/sha-256 (str previous-uuid))))
      (when-let [previous-line-key (or (:transcript/previous-source-line-key obs)
                                       (:transcript/previous-source-line-key opts)
                                       (:source/previous-line-key obs)
                                       (:source/previous-line-key opts))]
        (transcript-identity/chat-message-id object-key
                                             (envelope/sha-256 (str previous-line-key))))))

(defn transcript-observation-import-request
  ([obs]
   (transcript-observation-import-request obs {}))
  ([obs opts]
   (let [source (or (:transcript/source obs) :transcript)
         conversation-id (:transcript/conversation-id obs)
         object-key (transcript-identity/transcript-object-key source conversation-id)
         source-line-key (transcript-identity/transcript-source-line-key obs)
         source-line-key-hash (envelope/sha-256 source-line-key)
         source-id (transcript-identity/transcript-source-id object-key source-line-key)
         source-ref (str (:source/file-path obs) "#" (:source/byte-offset obs))
         source-hash-value (or (:source/line-hash obs)
                               (envelope/sha-256 (pr-str (:transcript/redacted-payload obs))))
         request-id (or (:request/id opts)
                        (:request-id opts)
                        (str "import-tr:" source-line-key-hash))
         import-key (str "imp:tr:" object-key ":" source-line-key-hash)
         idempotency-key (or (:idempotency/key opts)
                             (:idempotency-key opts)
                             import-key)
         now (long (or (:time-ms opts) (envelope/now-ms)))
         actor (or (:actor opts)
                   {:actor/id "system"
                    :actor/type :system
                    :actor/capabilities #{:object-container/import-material
                                          :source/ingest
                                          :object/edit}})
         actor-id (:actor/id actor)
         event-id (str "evt:" object-key ":" request-id)
         order-key (format "%020d:%s" (long (or (:source/byte-offset obs) 0)) source-line-key-hash)
         source-text (or (:transcript/redacted-preview obs)
                         (pr-str (:transcript/redacted-payload obs))
                         "")
         conversation-container-id (transcript-identity/chat-conversation-id object-key)
         source-row (oc/->SourceArtifactRow source-id
                                            source-ref
                                            source-hash-value
                                            :transcript
                                            source-text
                                            conversation-container-id
                                            (long (count (.getBytes (str source-text) "UTF-8")))
                                            now
                                            actor-id
                                            event-id)
         source-version-row (oc/->SourceVersionRow (oc/source-ref-key source-ref)
                                                   source-ref
                                                   source-hash-value
                                                   source-id
                                                   conversation-container-id
                                                   object-key
                                                   order-key
                                                   now
                                                   event-id)
         parse-error? (some? (:transcript/parse-error-kind obs))
         conv-id conversation-container-id
         conv-content (str "Conversation " conversation-id)
         conv-hash (oc/source-hash conv-content)
         conv-rev-id (oc/import-revision-id object-key conv-id (str object-key ":conversation"))
         conv-anchor (transcript-anchor-row object-key source-id source-ref source-hash-value
                                           :object-container conv-id obs event-id)
         conv-row (transcript-container-row conv-id :chat-conversation object-key source-id
                                            (:source-anchor-id conv-anchor) conv-rev-id
                                            conv-content conv-hash now actor-id event-id)
         conv-rev (transcript-revision-row conv-rev-id conv-id conv-content conv-hash
                                           order-key now actor-id event-id)
         message-key (envelope/sha-256 (str (or (:transcript/message-uuid obs) source-line-key)))
         message-id (transcript-identity/chat-message-id object-key message-key)
         previous-message-id (transcript-previous-message-container-id object-key obs opts)
         message-content (transcript-text-content (:transcript/redacted-payload obs))
         message-hash (oc/source-hash message-content)
         message-rev-id (oc/import-revision-id object-key message-id import-key)
         message-anchor (transcript-anchor-row object-key source-id source-ref source-hash-value
                                              :object-container message-id obs event-id)
         message-row (transcript-container-row message-id :chat-message object-key source-id
                                               (:source-anchor-id message-anchor) message-rev-id
                                               message-content message-hash now actor-id event-id)
         message-rev (transcript-revision-row message-rev-id message-id message-content
                                              message-hash order-key now actor-id event-id)
         tool-use-rows
         (vec
          (for [block (transcript-tool-use-blocks (:transcript/redacted-payload obs))
                :let [tool-use-id (or (:id block) (:tool_use_id block) (:tool-use-id block))
                      tool-key (envelope/sha-256 (str tool-use-id))
                      container-id (transcript-identity/tool-call-id object-key tool-key)
                      content-text (pr-str (select-keys block [:name :input]))
                      content-hash (oc/source-hash content-text)
                      revision-id (oc/import-revision-id object-key container-id import-key)
                      anchor (transcript-anchor-row object-key source-id source-ref source-hash-value
                                                    :object-container container-id obs event-id)]
                :when (oc/string-present? (str tool-use-id))]
            {:container (transcript-container-row container-id :tool-call object-key source-id
                                                 (:source-anchor-id anchor) revision-id
                                                 content-text content-hash now actor-id event-id)
             :revision (transcript-revision-row revision-id container-id content-text content-hash
                                                order-key now actor-id event-id)
             :anchor anchor
             :tool-use-id (str tool-use-id)
             :tool-name (:name block)
             :edge (transcript-composition-edge object-key :produced
                                                :object-container message-id
                                                :object-container container-id
                                                order-key source-id (:source-anchor-id anchor) event-id)}))
         tool-use-container-id-by-source-id
         (into {}
               (map (fn [{:keys [container tool-use-id]}]
                      [tool-use-id (:container-id container)]))
               tool-use-rows)
         tool-result-rows
         (vec
          (for [block (transcript-tool-result-blocks (:transcript/redacted-payload obs))
                :let [tool-use-id (or (:tool_use_id block) (:tool-use-id block) (:id block))
                      tool-use-id-str (str tool-use-id)
                      tool-parent-id (when (oc/string-present? tool-use-id-str)
                                       (or (get tool-use-container-id-by-source-id tool-use-id-str)
                                           (transcript-identity/tool-call-id
                                            object-key
                                            (envelope/sha-256 tool-use-id-str))))
                      result-key (envelope/sha-256 (str tool-use-id ":" source-line-key))
                      container-id (transcript-identity/tool-result-id object-key result-key)
                      content-text (str (or (:content block) (:text block) ""))
                      content-hash (oc/source-hash content-text)
                      revision-id (oc/import-revision-id object-key container-id import-key)
                      anchor (transcript-anchor-row object-key source-id source-ref source-hash-value
                                                    :object-container container-id obs event-id)]
                :when (or (oc/string-present? (str tool-use-id))
                          (oc/string-present? content-text))]
            {:container (transcript-container-row container-id :tool-result object-key source-id
                                                 (:source-anchor-id anchor) revision-id
                                                 content-text content-hash now actor-id event-id)
             :revision (transcript-revision-row revision-id container-id content-text content-hash
                                                order-key now actor-id event-id)
             :anchor anchor
             :tool-use-id tool-use-id-str
             :edge (transcript-composition-edge object-key :produced
                                                :object-container (or tool-parent-id message-id)
                                                :object-container container-id
                                                order-key source-id (:source-anchor-id anchor) event-id)}))
         containers (if parse-error?
                      []
                      (into [conv-row message-row] (map :container) (concat tool-use-rows tool-result-rows)))
         revisions (if parse-error?
                     []
                     (into [conv-rev message-rev] (map :revision) (concat tool-use-rows tool-result-rows)))
         anchors (if parse-error?
                   []
                   (into [conv-anchor message-anchor] (map :anchor) (concat tool-use-rows tool-result-rows)))
         contains-edge (when-not parse-error?
                         (transcript-composition-edge object-key :contains
                                                      :object-container conv-id
                                                      :object-container message-id
                                                      order-key source-id
                                                      (:source-anchor-id message-anchor)
                                                      event-id))
         follows-edge (when (and (not parse-error?) (oc/string-present? previous-message-id))
                        (transcript-composition-edge object-key :follows
                                                     :object-container previous-message-id
                                                     :object-container message-id
                                                     order-key source-id
                                                     (:source-anchor-id message-anchor)
                                                     event-id))
         edges (if parse-error?
                 []
                 (into (cond-> [contains-edge]
                         follows-edge (conj follows-edge))
                       (map :edge)
                       (concat tool-use-rows tool-result-rows)))
         message-role (transcript-role (:transcript/redacted-payload obs))
         message-preview (transcript-content-preview message-content)
         parse-error-kind (:transcript/parse-error-kind obs)
         conversation-projection-hints
         (if parse-error?
           [(transcript-conversation-projection-row
             conv-id
             order-key
             :parse-error
             nil
             nil
             nil
             source-id
             source-ref
             source-line-key
             event-id
             request-id
             import-key
             (:transcript/message-uuid obs)
             message-role
             (transcript-content-preview source-text)
             parse-error-kind)]
           (vec
            (concat
             [(transcript-conversation-projection-row
               conv-id
               order-key
               :message
               message-id
               message-rev-id
               (:source-anchor-id message-anchor)
               source-id
               source-ref
               source-line-key
               event-id
               request-id
               import-key
               (:transcript/message-uuid obs)
               message-role
               message-preview
               nil)]
             (mapv (fn [{:keys [container revision anchor tool-name]}]
                     (transcript-conversation-projection-row
                      conv-id
                      (str order-key ":tool-call:" (:container-id container))
                      :tool-call
                      (:container-id container)
                      (:revision-id revision)
                      (:source-anchor-id anchor)
                      source-id
                      source-ref
                      source-line-key
                      event-id
                      request-id
                      import-key
                      (:transcript/message-uuid obs)
                      message-role
                      (transcript-content-preview tool-name)
                      nil))
                   tool-use-rows)
             (mapv (fn [{:keys [container revision anchor]}]
                     (transcript-conversation-projection-row
                      conv-id
                      (str order-key ":tool-result:" (:container-id container))
                      :tool-result
                      (:container-id container)
                      (:revision-id revision)
                      (:source-anchor-id anchor)
                      source-id
                      source-ref
                      source-line-key
                      event-id
                      request-id
                      import-key
                      (:transcript/message-uuid obs)
                      message-role
                      (transcript-content-preview (:current-content-text container))
                      nil))
                   tool-result-rows))))
         tool-call-index-hints
         (mapv (fn [{:keys [container tool-use-id tool-name]}]
                 (transcript-tool-call-index-row
                  (str tool-name)
                  (str source-line-key ":" tool-use-id)
                  (:container-id container)
                  conv-id
                  message-id
                  source-id
                  source-ref
                  source-line-key
                  event-id
                  request-id
                  import-key))
               tool-use-rows)
         audit-hint (transcript-audit-entry-row
                     request-id
                     order-key
                     (if parse-error? :parse-error :imported)
                     conv-id
                     (when-not parse-error? message-id)
                     source-id
                     source-ref
                     source-line-key
                     parse-error-kind
                     event-id
                     import-key
                     (if parse-error?
                       (str "Transcript parse error: " (name parse-error-kind))
                       "Transcript source record imported"))
         last-message-hint (when-not parse-error?
                             (transcript-last-message-row conv-id
                                                          message-id
                                                          source-line-key
                                                          order-key
                                                          event-id
                                                          request-id
                                                          import-key
                                                          now))
         projection-hints (cond-> (vec conversation-projection-hints)
                            (seq tool-call-index-hints) (into tool-call-index-hints)
                            true (conj audit-hint)
                            last-message-hint (conj last-message-hint))
         source-line-status-hints (if-let [line-status-row
                                           (transcript-source-line-status-row
                                            obs
                                            import-key
                                            nil
                                            source-id
                                            source-ref
                                            source-line-key
                                            order-key
                                            request-id
                                            now)]
                                    [line-status-row]
                                    [])
         payload {:object-key object-key
                  :source-artifacts [source-row]
                  :object-containers containers
                  :revisions revisions
                  :derived-units []
                  :source-anchors anchors
                  :composition-edges edges
                  :source-versions [source-version-row]
                  :projection-hints projection-hints
                  :source-line-statuses source-line-status-hints}
         fingerprint (envelope/sha-256
                      (pr-str
                       {:object-key object-key
                        :import-key import-key
                        :source-line-key source-line-key
                        :source-ref source-ref
                        :source-hash source-hash-value
                        :source-artifacts
                        (mapv #(select-keys %
                                            [:source-id
                                             :source-ref
                                             :source-hash
                                             :source-format
                                             :source-raw-text
                                             :document-container-id
                                             :content-byte-count])
                              [source-row])
                        :object-containers
                        (mapv #(select-keys %
                                            [:container-id
                                             :container-kind
                                             :object-key
                                             :visibility
                                             :source-id
                                             :source-anchor-id
                                             :source-unit-id
                                             :document-container-id
                                             :current-revision-id
                                             :current-content-hash])
                              containers)
                        :revisions
                        (mapv #(select-keys %
                                            [:revision-id
                                             :container-id
                                             :parent-revision-id
                                             :content-hash
                                             :order-key])
                              revisions)
                        :source-anchors
                        (mapv #(select-keys %
                                            [:source-anchor-id
                                             :target-kind
                                             :target-id
                                             :source-id
                                             :source-ref
                                             :source-hash
                                             :start-offset
                                             :end-offset
                                             :block-path])
                              anchors)
                        :composition-edges
                        (mapv #(select-keys %
                                            [:edge-id
                                             :object-key
                                             :document-container-id
                                             :parent-slot-id
                                             :child-slot-id
                                             :child-order-key
                                             :parent-target-kind
                                             :parent-target-id
                                             :child-target-kind
                                             :child-target-id
                                             :source-id
                                             :source-anchor-id])
                              edges)
                        :projection-hints
                        (mapv oc/projection-hint-fingerprint projection-hints)
                        :source-line-statuses
                        (mapv oc/source-line-status-fingerprint
                              source-line-status-hints)}))]
     (assoc (envelope/action-request
             {:request-id request-id
              :request-type :object-container/import-material
              :time-ms now
              :actor actor
              :target {:target/kind :object-container-import
                       :target/id import-key
                       :target/address {:source/ref source-ref
                                        :source/hash source-hash-value
                                        :object/key object-key}}
              :action {:action/type :object-container/import-material
                       :action/capability :object-container/import-material
                       :action/params {:source/format :transcript}}
              :routing/key [:object-container/import object-key]
              :payload payload
              :provenance {:source/type :transcript
                           :source/ref source-ref}})
            :partition/key object-key
            :object/key object-key
            :import/key import-key
            :idempotency/key idempotency-key
            :material/fingerprint fingerprint))))
