(ns app.server.rama.object-container-test
  (:require [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.markdown-adapter :as markdown-adapter]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.object-container.transcript-adapter :as transcript-adapter]
            [app.server.rama.object-container.transcript-identity :as transcript-identity]
            [app.server.rama.dogfood.transcript :as transcript]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]))

(defn test-runtime
  [ipc]
  (let [module-name (get-module-name oc/object-container-module)]
    (rtest/launch-module! ipc oc/object-container-module {:tasks 4 :threads 2})
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
	     :revisions-by-id (foreign-pstate ipc module-name "$$revisions-by-id")
	     :revision-history-by-container
	     (foreign-pstate ipc module-name "$$revision-history-by-container")
     :derived-units-by-id (foreign-pstate ipc module-name "$$derived-units-by-id")
     :unit-graduations-by-id (foreign-pstate ipc module-name "$$unit-graduations-by-id")
     :source-anchors-by-target (foreign-pstate ipc module-name "$$source-anchors-by-target")
     :composition-children-by-parent
     (foreign-pstate ipc module-name "$$composition-children-by-parent")
	     :composition-parent-by-child (foreign-pstate ipc module-name "$$composition-parent-by-child")
	     :outline-by-document (foreign-pstate ipc module-name "$$outline-by-document")
	     :transcript-conversation-projection
	     (foreign-pstate ipc module-name "$$transcript-conversation-projection")
	     :transcript-tool-calls-by-name
	     (foreign-pstate ipc module-name "$$transcript-tool-calls-by-name")
	     :transcript-audit-by-request
	     (foreign-pstate ipc module-name "$$transcript-audit-by-request")
	     :transcript-last-message-by-conversation
	     (foreign-pstate ipc module-name "$$transcript-last-message-by-conversation")
	     :transcript-source-lines-by-file
	     (foreign-pstate ipc module-name "$$transcript-source-lines-by-file")
	     :edit-order-by-target (foreign-pstate ipc module-name "$$edit-order-by-target")
	     :read-latest-source-by-ref-query
	     (foreign-query ipc module-name "read-latest-source-by-ref")
	     :read-source-by-ref-version-query
	     (foreign-query ipc module-name "read-source-by-ref-version")
	     :read-unit-query (foreign-query ipc module-name "read-unit")
	     :read-current-revision-query (foreign-query ipc module-name "read-current-revision")
	     :read-common-material-for-source-query
	     (foreign-query ipc module-name "read-common-material-for-source")}))

(defn append-and-await!
  [runtime request]
  (ocr/append-object-container-request! runtime request)
  (ocr/await-object-container-decision runtime
                                      (oc/request-partition-key request)
                                      (oc/request-id request)
                                      5000))

(defn append-only!
  [runtime request]
  (ocr/append-object-container-request! runtime request)
  request)

(defn source-id
  [request]
  (or (get-in request [:payload :source-artifacts 0 :source-id])
      (get-in request [:target :target/id])))

(defn document-id
  [request]
  (oc/document-id-for-object-key (:object/key request)))

(defn derived-unit-id-for-outline-node
  [request outline-node]
  (markdown-adapter/derived-unit-id (:object/key request) (:block-path outline-node)))

(defn outline-node-at
  [runtime document-id block-path]
  (some #(when (= block-path (:block-path %)) %)
        (ocr/read-outline runtime document-id)))

(defn read-source-anchor
  [runtime target-id]
  (first (ocr/read-source-anchors runtime target-id)))

(defn read-parent-edge
  [runtime child-slot-id]
  (first (ocr/read-composition-parents runtime child-slot-id)))

(defn child-edges
  [runtime parent-slot-id]
  (foreign-select [(keypath parent-slot-id) MAP-VALS]
                  (:composition-children-by-parent runtime)))

(defn ingest!
  [runtime raw-text source-ref opts]
  (let [request (markdown-adapter/source-ingest-request raw-text source-ref opts)
        decision (append-and-await! runtime request)]
    {:request request
     :decision decision
     :source-id (source-id request)
     :document-id (document-id request)
     :object-key (:object/key request)
     :source-hash (get-in request [:payload :source-hash])}))

(defn edit!
  [runtime target-kind target-id content opts]
  (let [request (oc/object-edit-request target-kind target-id content opts)
        decision (append-and-await! runtime request)]
    {:request request
     :decision decision}))

(defn transcript-observation
  [conversation-id line-idx message-uuid role content]
  (let [offset (* 100 line-idx)
        line-hash (oc/source-hash (pr-str content))]
    {:transcript/ingest-request-id "transcript-common-run"
     :transcript/source :claude-code
     :transcript/conversation-id conversation-id
     :transcript/message-uuid message-uuid
     :transcript/redacted-payload {:type "message"
                                   :message {:role role
                                             :content content}}
     :transcript/redacted-preview (pr-str content)
     :source/file-key (str "file:" conversation-id)
     :source/file-id (str "fid:" conversation-id)
     :source/file-path (str "/tmp/" conversation-id ".jsonl")
     :source/file-generation-key "generation-1"
     :source/byte-offset offset
     :source/byte-length 80
     :source/line-hash line-hash}))

(defn one-child-edge
  [runtime parent-slot-id pred]
  (some #(when (pred %) %) (ocr/read-composition-children runtime parent-slot-id)))

(deftest object-container-slice-contract-test
  (with-open [ipc (rtest/create-ipc)]
    (let [runtime (test-runtime ipc)]
      (testing "source/ingest preserves raw source and derives markdown blocks in order"
        (let [raw "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
              {:keys [request decision source-id document-id]} (ingest! runtime
                                                                        raw
                                                                        "phase5/main.md"
                                                                        {:request/id "ingest-main"
                                                                         :time-ms 1000})
              source-row (ocr/read-source runtime source-id)
              source-row-by-ref (ocr/read-source runtime
                                                "phase5/main.md"
                                                (get-in request [:payload :source-hash]))
              latest-source (ocr/read-latest-source-by-ref runtime "phase5/main.md")
              document-container (ocr/read-container runtime document-id)
              outline (ocr/read-outline runtime document-id)
              outline-texts (mapv :content-text outline)
              unit-results (mapv #(ocr/read-unit runtime (:target-id %)) outline)
              units (mapv :unit unit-results)
              outline-kinds (mapv :unit-kind units)
              anchors (mapv #(read-source-anchor runtime (:target-id %)) outline)
              root-unit-id (:target-id (first outline))
              paragraph-node (second outline)
              paragraph-unit-id (:target-id paragraph-node)
              paragraph-unit (ocr/read-unit runtime paragraph-unit-id)
              root-children (child-edges runtime root-unit-id)]
          (is (= :accepted (:status decision)))
          (is (= raw (:source-raw-text source-row)))
          (is (= source-row source-row-by-ref))
          (is (= source-row latest-source))
          (is (= :document (:container-kind document-container)))
          (is (= [":markdown/heading" ":markdown/paragraph" ":markdown/list-item"
                  ":markdown/list-item" ":markdown/heading" ":markdown/paragraph"]
                 (mapv str outline-kinds)))
          (is (= ["Root" "A\nB" "one" "two" "Next" "C"] outline-texts))
          (is (= root-unit-id (:parent-slot-id paragraph-node)))
          (is (= :derived-unit (:target-kind paragraph-unit)))
          (is (= "A\nB" (:content-text paragraph-unit)))
          (is (every? #(= markdown-adapter/markdown-distiller-id (:distiller-id %)) units))
          (is (every? #(= markdown-adapter/markdown-distiller-version
                          (:distiller-version %))
                      units))
          (is (every? :source-anchor-id units))
          (is (= (mapv :source-anchor-id units) (mapv :source-anchor-id anchors)))
          (is (= (mapv :unit-id units) (mapv :target-id anchors)))
          (is (= (vec (repeat (count anchors) source-id)) (mapv :source-id anchors)))
          (is (= (mapv :block-path outline) (mapv :block-path anchors)))
          (is (= [paragraph-unit-id (:target-id (nth outline 2)) (:target-id (nth outline 3))
                  (:target-id (nth outline 4))]
                 (mapv :child-slot-id root-children)))))

      (testing "same ref/hash re-ingest is idempotent and does not duplicate the outline"
        (let [duplicate (markdown-adapter/source-ingest-request
                         "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                         "phase5/main.md"
                         {:request/id "ingest-main-duplicate"
                          :idempotency/key "different-ingest-idem"
                          :time-ms 1100})
              decision (append-and-await! runtime duplicate)
              outline (ocr/read-outline runtime (document-id duplicate))]
          (is (= :accepted (:status decision)))
          (is (= "ingest-main-duplicate" (:request-id decision)))
          (is (= 6 (count outline)))
          (is (= ["Root" "A\nB" "one" "two" "Next" "C"]
                 (mapv :content-text outline)))))

      (testing "back-to-back same ref/hash ingests dedupe before either decision is awaited"
        (let [raw "# Race\nSame source"
              request-a (markdown-adapter/source-ingest-request
                         raw
                         "phase5/race-ingest.md"
                         {:request/id "ingest-race-a"
                          :idempotency/key "ingest-race-a"
                          :time-ms 1200})
              request-b (markdown-adapter/source-ingest-request
                         raw
                         "phase5/race-ingest.md"
                         {:request/id "ingest-race-b"
                          :idempotency/key "ingest-race-b"
                          :time-ms 1201})
              _ (append-only! runtime request-a)
              _ (append-only! runtime request-b)
              decision-a (ocr/await-object-container-decision runtime request-a 5000)
              decision-b (ocr/await-object-container-decision runtime request-b 5000)
              outline (ocr/read-outline runtime (document-id request-a))
              source-row (ocr/read-source runtime (source-id request-a))
              latest-source (ocr/read-latest-source-by-ref runtime "phase5/race-ingest.md")
	              source-ref-key (oc/source-ref-key "phase5/race-ingest.md")
	              source-versions (foreign-select [(keypath source-ref-key) MAP-VALS]
	                                              (:source-versions-by-ref runtime))
	              source-completions (foreign-select [(keypath source-ref-key) MAP-VALS]
	                                                 (:source-ingest-completions-by-ref runtime))]
          (is (= :accepted (:status decision-a)))
          (is (= :accepted (:status decision-b)))
          (is (= source-row latest-source))
          (is (= 1 (count source-versions)))
          (is (= 1 (count source-completions)))
          (is (= 2 (count outline)))
          (is (= ["Race" "Same source"] (mapv :content-text outline)))))

      (testing "await-object-container-decision supports request-map and partition-key arities"
        (let [request (markdown-adapter/source-ingest-request
                       "Await me"
                       "phase5/await-arity.md"
                       {:request/id "ingest-await-arity"
                        :time-ms 1300})
              _ (append-only! runtime request)
              by-request (ocr/await-object-container-decision runtime request 5000)
              by-key (ocr/await-object-container-decision runtime
                                                         (:partition/key request)
                                                         (:request/id request)
                                                         5000)]
          (is (= :accepted (:status by-request)))
          (is (= by-request by-key))))

      (testing "editing a derived unit graduates once, preserves provenance, and updates outline content"
        (let [main-request (markdown-adapter/source-ingest-request
                            "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                            "phase5/main.md"
                            {:request/id "ingest-main-readonly"})
              doc-id (document-id main-request)
              source-id (source-id main-request)
              outline-before (ocr/read-outline runtime doc-id)
              paragraph-node (second outline-before)
              paragraph-unit-id (:target-id paragraph-node)
              parent-edge-before (read-parent-edge runtime paragraph-unit-id)
              anchor-before (read-source-anchor runtime paragraph-unit-id)
              {:keys [decision]} (edit! runtime
                                        :derived-unit
                                        paragraph-unit-id
                                        "Edited paragraph"
                                        {:request/id "edit-graduate"
                                         :edit/client-id "client-main"
                                         :edit/seq 1
                                         :time-ms 2000})
              container-id (get-in decision [:event-row :target-id])
              container (ocr/read-container runtime container-id)
              history (ocr/read-revision-history runtime container-id)
              first-revision (first history)
              first-history-page (ocr/read-revision-history runtime
                                                           container-id
                                                           (:order-key first-revision)
                                                           1)
              unit-after (ocr/read-unit runtime paragraph-unit-id)
              outline-after (outline-node-at runtime doc-id (:block-path paragraph-node))
              parent-edge-after (read-parent-edge runtime paragraph-unit-id)
              anchor-after (read-source-anchor runtime container-id)
              source-after (ocr/read-source runtime source-id)]
          (is (= :accepted (:status decision)))
          (is (= :object/graduated (get-in decision [:event-row :event-type])))
          (is (= :text-block (:container-kind container)))
          (is (= "Edited paragraph" (:current-content-text container)))
          (is (= 1 (count history)))
          (is (= nil (:parent-revision-id first-revision)))
          (is (= "Edited paragraph" (:content-text first-revision)))
          (is (= [first-revision] first-history-page))
          (is (= container-id (:target-id unit-after)))
          (is (= :object-container (:target-kind unit-after)))
          (is (= "Edited paragraph" (:content-text unit-after)))
          (is (= :object-container (:target-kind outline-after)))
          (is (= container-id (:target-id outline-after)))
          (is (= "Edited paragraph" (:content-text outline-after)))
          (is (= "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC" (:source-raw-text source-after)))
          (is (= (:parent-slot-id parent-edge-before) (:parent-slot-id parent-edge-after)))
          (is (= (:child-order-key parent-edge-before) (:child-order-key parent-edge-after)))
          (is (= container-id (:child-target-id parent-edge-after)))
          (is (= :object-container (:child-target-kind parent-edge-after)))
          (is (= (:source-id anchor-before) (:source-id anchor-after)))
          (is (= (:start-offset anchor-before) (:start-offset anchor-after)))
          (is (= (:end-offset anchor-before) (:end-offset anchor-after)))
          (is (= :object-container (:target-kind anchor-after)))))

      (testing "editing an already-graduated unit revises the same container"
        (let [doc-id (document-id
                      (markdown-adapter/source-ingest-request
                       "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                       "phase5/main.md"
                       {}))
              request (markdown-adapter/source-ingest-request
                       "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                       "phase5/main.md"
                       {})
              paragraph-node (second (ocr/read-outline runtime doc-id))
              paragraph-unit-id (derived-unit-id-for-outline-node request paragraph-node)
              existing-container-id (:target-id (ocr/read-unit runtime paragraph-unit-id))
              {:keys [decision]} (edit! runtime
                                        :derived-unit
                                        paragraph-unit-id
                                        "Edited through graduated unit"
                                        {:request/id "edit-graduated-unit"
                                         :edit/client-id "client-main"
                                         :edit/seq 2
                                         :time-ms 3000})
              container-id (get-in decision [:event-row :target-id])
              unit-after (ocr/read-unit runtime paragraph-unit-id)
              container (ocr/read-container runtime container-id)
              history (ocr/read-revision-history runtime container-id)
              first-revision (first history)
              second-revision (second history)
              second-history-page (ocr/read-revision-history runtime
                                                            container-id
                                                            (:order-key second-revision)
                                                            1)]
          (is (= existing-container-id container-id))
          (is (= :object/revised (get-in decision [:event-row :event-type])))
          (is (= "Edited through graduated unit" (:content-text unit-after)))
          (is (= "Edited through graduated unit" (:current-content-text container)))
          (is (= 2 (count history)))
          (is (= (:revision-id first-revision) (:parent-revision-id second-revision)))
          (is (= "Edited paragraph" (:content-text first-revision)))
          (is (= "Edited through graduated unit" (:content-text second-revision)))
          (is (= [second-revision] second-history-page))))

      (testing "back-to-back first-touch edits create one graduation then one revision"
        (let [source (ingest! runtime
                              "# Race edit\nOriginal block"
                              "phase5/race-edit.md"
                              {:request/id "ingest-race-edit"
                               :time-ms 3500})
              unit-id (:target-id (second (ocr/read-outline runtime (:document-id source))))
              edit-a (oc/object-edit-request :derived-unit
                                             unit-id
                                             "Race edit A"
                                             {:request/id "edit-race-a"
                                              :edit/client-id "client-race"
                                              :edit/seq 1
                                              :time-ms 3600})
              edit-b (oc/object-edit-request :derived-unit
                                             unit-id
                                             "Race edit B"
                                             {:request/id "edit-race-b"
                                              :edit/client-id "client-race"
                                              :edit/seq 2
                                              :time-ms 3601})
              _ (append-only! runtime edit-a)
              _ (append-only! runtime edit-b)
              decision-a (ocr/await-object-container-decision runtime edit-a 5000)
              decision-b (ocr/await-object-container-decision runtime edit-b 5000)
              container-id-a (get-in decision-a [:event-row :target-id])
              container-id-b (get-in decision-b [:event-row :target-id])
              container (ocr/read-container runtime container-id-a)
              history (ocr/read-revision-history runtime container-id-a)]
          (is (= :accepted (:status decision-a)))
          (is (= :accepted (:status decision-b)))
          (is (= :object/graduated (get-in decision-a [:event-row :event-type])))
          (is (= :object/revised (get-in decision-b [:event-row :event-type])))
          (is (= container-id-a container-id-b))
          (is (= "Race edit B" (:current-content-text container)))
          (is (= 2 (count history)))
          (is (= (:revision-id (first history)) (:parent-revision-id (second history))))))

      (testing "re-ingest cannot overwrite graduated authored content, even when the source ref changes hash"
        (let [old-request (markdown-adapter/source-ingest-request
                           "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                           "phase5/main.md"
                           {})
              old-doc-id (document-id old-request)
              old-paragraph-node (second (ocr/read-outline runtime old-doc-id))
              changed (ingest! runtime
                               "# Root\nChanged source paragraph"
                               "phase5/main.md"
                               {:request/id "ingest-main-new-hash"
                                :time-ms 4000})
              changed-outline (ocr/read-outline runtime (:document-id changed))
              latest-source (ocr/read-latest-source-by-ref runtime "phase5/main.md")]
          (is (= "Edited through graduated unit"
                 (:content-text (outline-node-at runtime old-doc-id (:block-path old-paragraph-node)))))
          (is (= (:source-id changed) (:source-id latest-source)))
          (is (= ["Root" "Changed source paragraph"] (mapv :content-text changed-outline)))))

	      (testing "conflicting idempotency keys reject and duplicate request ids create no second revision"
        (let [doc-id (document-id
                      (markdown-adapter/source-ingest-request
                       "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                       "phase5/main.md"
                       {}))
              request (markdown-adapter/source-ingest-request
                       "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                       "phase5/main.md"
                       {})
              paragraph-node (second (ocr/read-outline runtime doc-id))
              paragraph-unit-id (derived-unit-id-for-outline-node request paragraph-node)
              container-id (:target-id (ocr/read-unit runtime paragraph-unit-id))
              history-before (count (ocr/read-revision-history runtime container-id))
              first-idem (edit! runtime
                                :object-container
                                container-id
                                "Idempotent first"
                                {:request/id "edit-idem-a"
                                 :idempotency/key "same-idem-key"
                                 :edit/client-id "client-main"
                                 :edit/seq 10
                                 :time-ms 5000})
              second-idem (edit! runtime
                                 :object-container
                                 container-id
                                 "Idempotent second"
                                 {:request/id "edit-idem-b"
                                  :idempotency/key "same-idem-key"
                                  :edit/client-id "client-main"
                                  :edit/seq 11
                                  :time-ms 5100})
              history-after-idem (ocr/read-revision-history runtime container-id)
              container-after-idem (ocr/read-container runtime container-id)
              first-request-id (edit! runtime
                                      :object-container
                                      container-id
                                      "Request id first"
                                      {:request/id "edit-duplicate-request-id"
                                       :idempotency/key "request-id-idem-a"
                                       :edit/client-id "client-main"
                                       :edit/seq 20
                                       :time-ms 5200})
              second-request-id (edit! runtime
                                       :object-container
                                       container-id
                                       "Request id second"
                                       {:request/id "edit-duplicate-request-id"
                                        :idempotency/key "request-id-idem-b"
                                        :edit/client-id "client-main"
                                        :edit/seq 21
                                        :time-ms 5300})
              history-after-request-id (ocr/read-revision-history runtime container-id)
              request-row (ocr/read-request runtime (:request first-request-id))]
	          (is (= :accepted (get-in first-idem [:decision :status])))
	          (is (= :rejected (get-in second-idem [:decision :status])))
	          (is (= :idempotency/material-fingerprint-conflict
	                 (get-in second-idem [:decision :reason])))
	          (is (= (+ history-before 1) (count history-after-idem)))
	          (is (= "Idempotent first" (:current-content-text container-after-idem)))
	          (is (= (:decision-id (:decision first-idem))
	                 (:conflict-with-decision-id (:decision second-idem))))
          (is (= :accepted (get-in first-request-id [:decision :status])))
          (is (= :accepted (get-in second-request-id [:decision :status])))
          (is (= (+ history-before 2) (count history-after-request-id)))
          (is (= "Request id first" (:current-content-text (ocr/read-container runtime container-id))))
          (is (= "Request id first" (get-in request-row [:payload :content-text])))))

      (testing "stale same-client edit seq is rejected durably"
        (let [doc-id (document-id
                      (markdown-adapter/source-ingest-request
                       "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                       "phase5/main.md"
                       {}))
              request (markdown-adapter/source-ingest-request
                       "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                       "phase5/main.md"
                       {})
              paragraph-node (second (ocr/read-outline runtime doc-id))
              paragraph-unit-id (derived-unit-id-for-outline-node request paragraph-node)
              container-id (:target-id (ocr/read-unit runtime paragraph-unit-id))
              stale-request (oc/object-edit-request :object-container
                                                    container-id
                                                    "Stale content"
                                                    {:request/id "edit-stale"
                                                     :edit/client-id "client-main"
                                                     :edit/seq 19
                                                     :time-ms 6000})
              decision (append-and-await! runtime stale-request)
              stored-request (ocr/read-request runtime stale-request)
              stored-decision (ocr/read-decision runtime
                                                (:partition/key stale-request)
                                                (:request/id stale-request))]
          (is (= :rejected (:status decision)))
          (is (= :edit/stale (:reason decision)))
          (is (= stale-request (:raw-request stored-request)))
          (is (= decision stored-decision))
          (is (= "Request id first" (:current-content-text (ocr/read-container runtime container-id))))))

      (testing "rejected requests are durable decisions and do not write target rows"
        (let [object-key (:object/key (markdown-adapter/source-ingest-request
                                       "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                                       "phase5/main.md"
                                       {}))
              missing-unit-id (markdown-adapter/derived-unit-id object-key "999999")
              missing-request (oc/object-edit-request :derived-unit
                                                      missing-unit-id
                                                      "Missing target"
                                                      {:request/id "edit-missing-target"
                                                       :edit/client-id "client-missing"
                                                       :edit/seq 1
                                                       :time-ms 7000})
              decision (append-and-await! runtime missing-request)
              stored-request (ocr/read-request runtime missing-request)
              stored-decision (ocr/read-decision runtime missing-request)]
          (is (= :rejected (:status decision)))
          (is (= :target/not-found (:reason decision)))
          (is (= missing-request (:raw-request stored-request)))
          (is (= decision stored-decision))
          (is (nil? (ocr/read-unit runtime missing-unit-id)))))

      (testing "rejected source ingests are durable decisions and do not write source rows"
        (let [request (-> (markdown-adapter/source-ingest-request
                           "Bad hash"
                           "phase5/bad-source.md"
                           {:request/id "ingest-bad-hash"
                            :time-ms 7500})
                          (assoc-in [:payload :source-hash] "not-the-real-hash"))
              decision (append-and-await! runtime request)
              stored-request (ocr/read-request runtime request)
              stored-decision (ocr/read-decision runtime request)]
          (is (= :rejected (:status decision)))
          (is (= :request-invalid (:reason decision)))
          (is (some #(= :source/hash-mismatch (:type %)) (:errors decision)))
          (is (= request (:raw-request stored-request)))
          (is (= decision stored-decision))
          (is (nil? (ocr/read-source runtime (source-id request))))))

      (testing "empty file and empty edit are valid edge cases"
        (let [empty-source (ingest! runtime
                                    ""
                                    "phase5/empty.md"
                                    {:request/id "ingest-empty"
                                     :time-ms 8000})
              whitespace-source (ingest! runtime
                                         "   \n\t"
                                         "phase5/whitespace.md"
                                         {:request/id "ingest-whitespace"
                                          :time-ms 8100})
              one-line (ingest! runtime
                                "Plain"
                                "phase5/empty-edit.md"
                                {:request/id "ingest-empty-edit"
                                 :time-ms 8200})
              unit-id (:target-id (first (ocr/read-outline runtime (:document-id one-line))))
              empty-edit (edit! runtime
                                :derived-unit
                                unit-id
                                ""
                                {:request/id "edit-empty-content"
                                 :edit/client-id "client-empty"
                                 :edit/seq 1
                                 :time-ms 8300})
              container-id (get-in empty-edit [:decision :event-row :target-id])
              container (ocr/read-container runtime container-id)]
          (is (= :accepted (get-in empty-source [:decision :status])))
          (is (= "" (:source-raw-text (ocr/read-source runtime (:source-id empty-source)))))
          (is (empty? (ocr/read-outline runtime (:document-id empty-source))))
          (is (= "   \n\t" (:source-raw-text (ocr/read-source runtime (:source-id whitespace-source)))))
          (is (empty? (ocr/read-outline runtime (:document-id whitespace-source))))
          (is (= :accepted (get-in empty-edit [:decision :status])))
          (is (= "" (:current-content-text container)))
          (is (= 1 (count (ocr/read-revision-history runtime container-id))))))

	      (testing "outline range reads preserve source order for many derived blocks"
	        (let [raw (->> (range 64)
	                       (map #(str "- item-" %))
	                       (str/join "\n"))
              large (ingest! runtime
                             raw
                             "phase5/large.md"
                             {:request/id "ingest-large"
                              :time-ms 9000})
              outline (ocr/read-outline runtime (:document-id large))
              page (ocr/read-outline runtime (:document-id large) "000050" 5)]
	          (is (= 64 (count outline)))
	          (is (= (mapv #(str "item-" %) (range 64)) (mapv :content-text outline)))
	          (is (= ["item-50" "item-51" "item-52" "item-53" "item-54"]
	                 (mapv :content-text page))))))))

(deftest transcript-common-import-contract-test
  (with-open [ipc (rtest/create-ipc)]
    (let [runtime (test-runtime ipc)
          conversation-id "phase5-transcript-common"
          obs1 (transcript-observation
                conversation-id
                1
                "msg-1"
                "assistant"
                [{:type "text" :text "I will inspect it."}
                 {:type "tool_use"
                  :id "toolu-1"
                  :name "Read"
                  :input {:file "src/app/server/rama/object_container.clj"}}])
          request1 (transcript-adapter/transcript-observation-import-request
                    obs1
                    {:request/id "transcript-common-1"
                     :time-ms 10000})
          message1 (some #(when (= :chat-message (:container-kind %)) %)
                         (get-in request1 [:payload :object-containers]))
          tool-call (some #(when (= :tool-call (:container-kind %)) %)
                          (get-in request1 [:payload :object-containers]))
          obs2 (assoc (transcript-observation
                       conversation-id
                       2
                       "msg-2"
                       "user"
                       [{:type "tool_result"
                         :tool_use_id "toolu-1"
                         :content "The file has the expected namespace."}])
                      :transcript/previous-message-container-id
                      (:container-id message1))
          request2 (transcript-adapter/transcript-observation-import-request
                    obs2
                    {:request/id "transcript-common-2"
                     :time-ms 10100})
          message2 (some #(when (= :chat-message (:container-kind %)) %)
                         (get-in request2 [:payload :object-containers]))
          tool-result (some #(when (= :tool-result (:container-kind %)) %)
                            (get-in request2 [:payload :object-containers]))
          conversation (some #(when (= :chat-conversation (:container-kind %)) %)
                             (get-in request1 [:payload :object-containers]))
          decision1 (append-and-await! runtime request1)
          decision2 (append-and-await! runtime request2)
          conversation-id* (:container-id conversation)
          conversation-projection (ocr/read-transcript-conversation-projection
                                   runtime
                                   conversation-id*)
          completed-lines (ocr/read-transcript-source-lines
                           runtime
                           (str "file:" conversation-id))
          contains-message1 (one-child-edge
                             runtime
                             conversation-id*
                             #(= (:container-id message1) (:child-target-id %)))
          contains-message2 (one-child-edge
                             runtime
                             conversation-id*
                             #(= (:container-id message2) (:child-target-id %)))
          follows-edge (one-child-edge
                        runtime
                        (:container-id message1)
                        #(= (:container-id message2) (:child-target-id %)))
          tool-call-edge (one-child-edge
                          runtime
                          (:container-id message1)
                          #(= (:container-id tool-call) (:child-target-id %)))
          tool-result-edge (one-child-edge
                            runtime
                            (:container-id tool-call)
                            #(= (:container-id tool-result) (:child-target-id %)))
          common-material (ocr/read-common-material-for-source
                           runtime
                           (get-in request1 [:payload :source-artifacts 0 :source-id]))]
      (is (= :accepted (:status decision1)))
      (is (= :accepted (:status decision2)))
      (is (= :chat-conversation
             (:container-kind (ocr/read-container runtime conversation-id*))))
      (is (= :chat-message
             (:container-kind (ocr/read-container runtime (:container-id message1)))))
      (is (= :tool-call
             (:container-kind (ocr/read-container runtime (:container-id tool-call)))))
      (is (= :tool-result
             (:container-kind (ocr/read-container runtime (:container-id tool-result)))))
      (is (= #{:message :tool-call :tool-result}
             (set (map :entry-kind conversation-projection))))
      (is (= #{:import-complete}
             (set (map :status completed-lines))))
      (is (= 2 (count completed-lines)))
      (is (= :object-container (:parent-target-kind contains-message1)))
      (is (= :object-container (:child-target-kind contains-message2)))
      (is (= (:container-id message1) (:parent-target-id follows-edge)))
      (is (= (:container-id message2) (:child-target-id follows-edge)))
      (is (= (:container-id message1) (:parent-target-id tool-call-edge)))
      (is (= (:container-id tool-call) (:child-target-id tool-call-edge)))
      (is (= (:container-id tool-call) (:parent-target-id tool-result-edge)))
      (is (= (:container-id tool-result) (:child-target-id tool-result-edge)))
      (is (= 3 (count (:containers common-material))))
      (is (seq (:anchors common-material)))
      (is (seq (:edges common-material))))))

(deftest transcript-harvest-uses-common-object-container-path-test
  (let [runtime (ocr/start-object-container-runtime!)
        file (doto (java.io.File/createTempFile "softland-transcript-common" ".jsonl")
               (.deleteOnExit))]
    (try
      (spit file
            (str "{\"session_id\":\"harvest-conv\",\"uuid\":\"harvest-msg-1\","
                 "\"message\":{\"role\":\"assistant\",\"content\":["
                 "{\"type\":\"tool_use\",\"id\":\"toolu-harvest-1\","
                 "\"name\":\"Read\",\"input\":{\"file\":\"x.clj\"}}]}}\n"
                 "{\"session_id\":\"harvest-conv\",\"uuid\":\"harvest-msg-2\","
                 "\"message\":{\"role\":\"user\",\"content\":["
                 "{\"type\":\"tool_result\",\"tool_use_id\":\"toolu-harvest-1\","
                 "\"content\":\"ok\"}]}}\n"))
	      (let [request (transcript/transcript-request
	                     :transcript/harvest
	                     {:transcript/request-id "harvest-common-run"
	                      :transcript/source :claude-code
                      :transcript/paths [(.getPath file)]
                      :time-ms 12000})
	            result (transcript/harvest-transcripts! runtime request)
	            object-key (transcript-identity/transcript-object-key :claude-code "harvest-conv")
	            conversation-id (transcript-identity/chat-conversation-id object-key)
	            file-key (transcript/source-file-key :claude-code (transcript/file-id file))
	            projection (ocr/read-transcript-conversation-projection runtime conversation-id)
	            file-offset (ocr/read-transcript-file-offset runtime file-key)
	            messages (filter #(= :message (:entry-kind %)) projection)
            tool-call (some #(when (= :tool-call (:entry-kind %)) %) projection)
            tool-result (some #(when (= :tool-result (:entry-kind %)) %) projection)
            message1-id (:container-id (first messages))
            message2-id (:container-id (second messages))
            follows-edge (one-child-edge runtime message1-id #(= message2-id (:child-target-id %)))
            tool-result-edge (one-child-edge
                              runtime
                              (:container-id tool-call)
                              #(= (:container-id tool-result) (:child-target-id %)))]
	        (is (= 2 (:observations-appended result)))
	        (is (= :complete (:status (ocr/read-transcript-run runtime "harvest-common-run"))))
	        (is (= (.length file) (:last-byte-offset file-offset)))
	        (is (= [:message :tool-call :message :tool-result]
	               (mapv :entry-kind projection)))
        (is (= message1-id (:parent-target-id follows-edge)))
        (is (= message2-id (:child-target-id follows-edge)))
        (is (= (:container-id tool-call) (:parent-target-id tool-result-edge)))
        (is (= (:container-id tool-result) (:child-target-id tool-result-edge))))
	      (finally
	        (io/delete-file file true)
	        (ocr/close-object-container-runtime! runtime)))))

(deftest transcript-harvest-fails-when-common-import-rejects-test
  (let [runtime (ocr/start-object-container-runtime!)
        file (doto (java.io.File/createTempFile "softland-transcript-common-fail" ".jsonl")
               (.deleteOnExit))]
    (try
      (spit file
            (str "{\"session_id\":\"harvest-conflict\",\"uuid\":\"same-message\","
                 "\"message\":{\"role\":\"user\",\"content\":\"first\"}}\n"
                 "{\"session_id\":\"harvest-conflict\",\"uuid\":\"same-message\","
                 "\"message\":{\"role\":\"user\",\"content\":\"different\"}}\n"))
      (let [request (transcript/transcript-request
                     :transcript/harvest
                     {:transcript/request-id "harvest-common-fail-run"
                      :transcript/source :claude-code
                      :transcript/paths [(.getPath file)]
                      :time-ms 12100})
            result (transcript/harvest-transcripts! runtime request)
            run-row (ocr/read-transcript-run runtime "harvest-common-fail-run")]
        (is (= :failed (:status result)))
        (is (= :failed (:status run-row)))
        (is (= :object-container/import-failed (get-in run-row [:error :type])))
        (is (= :native-identity/conflict (get-in run-row [:error :reason]))))
      (finally
        (io/delete-file file true)
        (ocr/close-object-container-runtime! runtime)))))

(deftest transcript-watch-uses-common-object-container-path-test
  (let [runtime (ocr/start-object-container-runtime!)
        file (doto (java.io.File/createTempFile "softland-transcript-common-watch" ".jsonl")
               (.deleteOnExit))
        handle (atom nil)]
    (try
      (let [request (transcript/transcript-request
                     :transcript/watch
                     {:transcript/request-id "watch-common-run"
                      :transcript/source :claude-code
                      :transcript/paths [(.getPath file)]
                      :time-ms 12200})]
        (reset! handle (transcript/start-transcript-watch!
                        runtime
                        request
                        {:poll-ms 10000}))
        (spit file
              (str "{\"session_id\":\"watch-conv\",\"uuid\":\"watch-msg-1\","
                   "\"message\":{\"role\":\"assistant\",\"content\":\"watch me\"}}\n"))
        ((:poll-once! @handle))
        (let [object-key (transcript-identity/transcript-object-key :claude-code "watch-conv")
              conversation-id (transcript-identity/chat-conversation-id object-key)
              file-key (transcript/source-file-key :claude-code (transcript/file-id file))
              projection (ocr/read-transcript-conversation-projection runtime conversation-id)
              file-offset (ocr/read-transcript-file-offset runtime file-key)]
          (is (= [:message] (mapv :entry-kind projection)))
          (is (= (.length file) (:last-byte-offset file-offset)))))
      (finally
        (when-let [h @handle]
          ((:stop! h)))
        (io/delete-file file true)
        (ocr/close-object-container-runtime! runtime)))))

(deftest transcript-run-terminal-status-monotonic-test
  (let [runtime (ocr/start-object-container-runtime!)]
    (try
      (let [request {:request/type :transcript/harvest
                     :transcript/request-id "terminal-run"
                     :transcript/source :claude-code
                     :transcript/redaction-policy :standard
                     :transcript/paths []
                     :request/time-ms 11000}
            complete {:request/type :transcript/run-status
                      :transcript/request-id "terminal-run"
                      :status :complete
                      :time-ms 11100}
            late-running {:request/type :transcript/run-status
                          :transcript/request-id "terminal-run"
                          :status :running
                          :time-ms 11200}]
        (ocr/append-transcript-control! runtime request)
        (is (= :accepted-running
               (:status (ocr/read-transcript-run runtime "terminal-run"))))
        (ocr/append-transcript-control! runtime complete)
        (is (= :complete
               (:status (ocr/read-transcript-run runtime "terminal-run"))))
        (ocr/append-transcript-control! runtime late-running)
        (is (= :complete
               (:status (ocr/read-transcript-run runtime "terminal-run")))))
      (finally
        (ocr/close-object-container-runtime! runtime)))))
