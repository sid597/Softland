(ns app.server.rama.object-container-test
  (:require [app.server.rama.object-container :as oc]
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

(defn append-and-await!
  [runtime request]
  (oc/append-object-container-request! runtime request)
  (oc/await-object-container-decision runtime
                                      (oc/request-partition-key request)
                                      (oc/request-id request)
                                      5000))

(defn append-only!
  [runtime request]
  (oc/append-object-container-request! runtime request)
  request)

(defn source-id
  [request]
  (get-in request [:target :target/id]))

(defn document-id
  [request]
  (oc/document-id-for-object-key (:object/key request)))

(defn derived-unit-id-for-outline-node
  [request outline-node]
  (oc/derived-unit-id (:object/key request) (:block-path outline-node)))

(defn outline-node-at
  [runtime document-id block-path]
  (some #(when (= block-path (:block-path %)) %)
        (oc/read-outline runtime document-id)))

(defn read-source-anchor
  [runtime target-id]
  (foreign-select-one [(keypath target-id)] (:source-anchors-by-target runtime)))

(defn read-parent-edge
  [runtime child-slot-id]
  (foreign-select-one [(keypath child-slot-id)] (:composition-parent-by-child runtime)))

(defn child-edges
  [runtime parent-slot-id]
  (foreign-select [(keypath parent-slot-id) MAP-VALS]
                  (:composition-children-by-parent runtime)))

(defn ingest!
  [runtime raw-text source-ref opts]
  (let [request (oc/source-ingest-request raw-text source-ref opts)
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
              source-row (oc/read-source runtime source-id)
              source-row-by-ref (oc/read-source runtime
                                                "phase5/main.md"
                                                (get-in request [:payload :source-hash]))
              latest-source (oc/read-latest-source-by-ref runtime "phase5/main.md")
              document-container (oc/read-container runtime document-id)
              outline (oc/read-outline runtime document-id)
              outline-texts (mapv :content-text outline)
              unit-results (mapv #(oc/read-unit runtime (:target-id %)) outline)
              units (mapv :unit unit-results)
              outline-kinds (mapv :unit-kind units)
              anchors (mapv #(read-source-anchor runtime (:target-id %)) outline)
              root-unit-id (:target-id (first outline))
              paragraph-node (second outline)
              paragraph-unit-id (:target-id paragraph-node)
              paragraph-unit (oc/read-unit runtime paragraph-unit-id)
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
          (is (every? #(= oc/markdown-distiller-id (:distiller-id %)) units))
          (is (every? #(= oc/markdown-distiller-version (:distiller-version %)) units))
          (is (every? :source-anchor-id units))
          (is (= (mapv :source-anchor-id units) (mapv :source-anchor-id anchors)))
          (is (= (mapv :unit-id units) (mapv :target-id anchors)))
          (is (= (vec (repeat (count anchors) source-id)) (mapv :source-id anchors)))
          (is (= (mapv :block-path outline) (mapv :block-path anchors)))
          (is (= [paragraph-unit-id (:target-id (nth outline 2)) (:target-id (nth outline 3))
                  (:target-id (nth outline 4))]
                 (mapv :child-slot-id root-children)))))

      (testing "same ref/hash re-ingest is idempotent and does not duplicate the outline"
        (let [duplicate (oc/source-ingest-request "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                                                  "phase5/main.md"
                                                  {:request/id "ingest-main-duplicate"
                                                   :idempotency/key "different-ingest-idem"
                                                   :time-ms 1100})
              decision (append-and-await! runtime duplicate)
              outline (oc/read-outline runtime (document-id duplicate))]
          (is (= :accepted (:status decision)))
          (is (= "ingest-main-duplicate" (:request-id decision)))
          (is (= 6 (count outline)))
          (is (= ["Root" "A\nB" "one" "two" "Next" "C"]
                 (mapv :content-text outline)))))

      (testing "back-to-back same ref/hash ingests dedupe before either decision is awaited"
        (let [raw "# Race\nSame source"
              request-a (oc/source-ingest-request raw
                                                  "phase5/race-ingest.md"
                                                  {:request/id "ingest-race-a"
                                                   :idempotency/key "ingest-race-a"
                                                   :time-ms 1200})
              request-b (oc/source-ingest-request raw
                                                  "phase5/race-ingest.md"
                                                  {:request/id "ingest-race-b"
                                                   :idempotency/key "ingest-race-b"
                                                   :time-ms 1201})
              _ (append-only! runtime request-a)
              _ (append-only! runtime request-b)
              decision-a (oc/await-object-container-decision runtime request-a 5000)
              decision-b (oc/await-object-container-decision runtime request-b 5000)
              outline (oc/read-outline runtime (document-id request-a))
              source-row (oc/read-source runtime (source-id request-a))
              latest-source (oc/read-latest-source-by-ref runtime "phase5/race-ingest.md")
              source-versions (foreign-select [(keypath (:partition/key request-a)) MAP-VALS]
                                              (:source-versions-by-ref runtime))
              source-completions (foreign-select [(keypath (:partition/key request-a)) MAP-VALS]
                                                 (:source-ingest-completions-by-ref runtime))]
          (is (= :accepted (:status decision-a)))
          (is (= :accepted (:status decision-b)))
          (is (= source-row latest-source))
          (is (= 1 (count source-versions)))
          (is (= 1 (count source-completions)))
          (is (= 2 (count outline)))
          (is (= ["Race" "Same source"] (mapv :content-text outline)))))

      (testing "await-object-container-decision supports request-map and partition-key arities"
        (let [request (oc/source-ingest-request "Await me"
                                                "phase5/await-arity.md"
                                                {:request/id "ingest-await-arity"
                                                 :time-ms 1300})
              _ (append-only! runtime request)
              by-request (oc/await-object-container-decision runtime request 5000)
              by-key (oc/await-object-container-decision runtime
                                                         (:partition/key request)
                                                         (:request/id request)
                                                         5000)]
          (is (= :accepted (:status by-request)))
          (is (= by-request by-key))))

      (testing "editing a derived unit graduates once, preserves provenance, and updates outline content"
        (let [main-request (oc/source-ingest-request "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                                                     "phase5/main.md"
                                                     {:request/id "ingest-main-readonly"})
              doc-id (document-id main-request)
              source-id (source-id main-request)
              outline-before (oc/read-outline runtime doc-id)
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
              container (oc/read-container runtime container-id)
              history (oc/read-revision-history runtime container-id)
              first-revision (first history)
              first-history-page (oc/read-revision-history runtime
                                                           container-id
                                                           (:order-key first-revision)
                                                           1)
              unit-after (oc/read-unit runtime paragraph-unit-id)
              outline-after (outline-node-at runtime doc-id (:block-path paragraph-node))
              parent-edge-after (read-parent-edge runtime paragraph-unit-id)
              anchor-after (read-source-anchor runtime container-id)
              source-after (oc/read-source runtime source-id)]
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
        (let [doc-id (document-id (oc/source-ingest-request "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                                                           "phase5/main.md"
                                                           {}))
              request (oc/source-ingest-request "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                                                "phase5/main.md"
                                                {})
              paragraph-node (second (oc/read-outline runtime doc-id))
              paragraph-unit-id (derived-unit-id-for-outline-node request paragraph-node)
              existing-container-id (:target-id (oc/read-unit runtime paragraph-unit-id))
              {:keys [decision]} (edit! runtime
                                        :derived-unit
                                        paragraph-unit-id
                                        "Edited through graduated unit"
                                        {:request/id "edit-graduated-unit"
                                         :edit/client-id "client-main"
                                         :edit/seq 2
                                         :time-ms 3000})
              container-id (get-in decision [:event-row :target-id])
              unit-after (oc/read-unit runtime paragraph-unit-id)
              container (oc/read-container runtime container-id)
              history (oc/read-revision-history runtime container-id)
              first-revision (first history)
              second-revision (second history)
              second-history-page (oc/read-revision-history runtime
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
              unit-id (:target-id (second (oc/read-outline runtime (:document-id source))))
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
              decision-a (oc/await-object-container-decision runtime edit-a 5000)
              decision-b (oc/await-object-container-decision runtime edit-b 5000)
              container-id-a (get-in decision-a [:event-row :target-id])
              container-id-b (get-in decision-b [:event-row :target-id])
              container (oc/read-container runtime container-id-a)
              history (oc/read-revision-history runtime container-id-a)]
          (is (= :accepted (:status decision-a)))
          (is (= :accepted (:status decision-b)))
          (is (= :object/graduated (get-in decision-a [:event-row :event-type])))
          (is (= :object/revised (get-in decision-b [:event-row :event-type])))
          (is (= container-id-a container-id-b))
          (is (= "Race edit B" (:current-content-text container)))
          (is (= 2 (count history)))
          (is (= (:revision-id (first history)) (:parent-revision-id (second history))))))

      (testing "re-ingest cannot overwrite graduated authored content, even when the source ref changes hash"
        (let [old-request (oc/source-ingest-request "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                                                    "phase5/main.md"
                                                    {})
              old-doc-id (document-id old-request)
              old-paragraph-node (second (oc/read-outline runtime old-doc-id))
              changed (ingest! runtime
                               "# Root\nChanged source paragraph"
                               "phase5/main.md"
                               {:request/id "ingest-main-new-hash"
                                :time-ms 4000})
              changed-outline (oc/read-outline runtime (:document-id changed))
              latest-source (oc/read-latest-source-by-ref runtime "phase5/main.md")]
          (is (= "Edited through graduated unit"
                 (:content-text (outline-node-at runtime old-doc-id (:block-path old-paragraph-node)))))
          (is (= (:source-id changed) (:source-id latest-source)))
          (is (= ["Root" "Changed source paragraph"] (mapv :content-text changed-outline)))))

      (testing "duplicate idempotency key and duplicate request id create no second revision"
        (let [doc-id (document-id (oc/source-ingest-request "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                                                           "phase5/main.md"
                                                           {}))
              request (oc/source-ingest-request "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                                                "phase5/main.md"
                                                {})
              paragraph-node (second (oc/read-outline runtime doc-id))
              paragraph-unit-id (derived-unit-id-for-outline-node request paragraph-node)
              container-id (:target-id (oc/read-unit runtime paragraph-unit-id))
              history-before (count (oc/read-revision-history runtime container-id))
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
              history-after-idem (oc/read-revision-history runtime container-id)
              container-after-idem (oc/read-container runtime container-id)
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
              history-after-request-id (oc/read-revision-history runtime container-id)
              request-row (oc/read-request runtime (:request first-request-id))]
          (is (= :accepted (get-in first-idem [:decision :status])))
          (is (= :accepted (get-in second-idem [:decision :status])))
          (is (= (+ history-before 1) (count history-after-idem)))
          (is (= "Idempotent first" (:current-content-text container-after-idem)))
          (is (= (:event-id (:decision first-idem))
                 (:event-id (:decision second-idem))))
          (is (= :accepted (get-in first-request-id [:decision :status])))
          (is (= :accepted (get-in second-request-id [:decision :status])))
          (is (= (+ history-before 2) (count history-after-request-id)))
          (is (= "Request id first" (:current-content-text (oc/read-container runtime container-id))))
          (is (= "Request id first" (get-in request-row [:payload :content-text])))))

      (testing "stale same-client edit seq is rejected durably"
        (let [doc-id (document-id (oc/source-ingest-request "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                                                           "phase5/main.md"
                                                           {}))
              request (oc/source-ingest-request "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                                                "phase5/main.md"
                                                {})
              paragraph-node (second (oc/read-outline runtime doc-id))
              paragraph-unit-id (derived-unit-id-for-outline-node request paragraph-node)
              container-id (:target-id (oc/read-unit runtime paragraph-unit-id))
              stale-request (oc/object-edit-request :object-container
                                                    container-id
                                                    "Stale content"
                                                    {:request/id "edit-stale"
                                                     :edit/client-id "client-main"
                                                     :edit/seq 19
                                                     :time-ms 6000})
              decision (append-and-await! runtime stale-request)
              stored-request (oc/read-request runtime stale-request)
              stored-decision (oc/read-decision runtime
                                                (:partition/key stale-request)
                                                (:request/id stale-request))]
          (is (= :rejected (:status decision)))
          (is (= :edit/stale (:reason decision)))
          (is (= stale-request (:raw-request stored-request)))
          (is (= decision stored-decision))
          (is (= "Request id first" (:current-content-text (oc/read-container runtime container-id))))))

      (testing "rejected requests are durable decisions and do not write target rows"
        (let [object-key (:object/key (oc/source-ingest-request "# Root\nA\nB\n\n- one\n- two\n\n## Next\nC"
                                                                "phase5/main.md"
                                                                {}))
              missing-unit-id (oc/derived-unit-id object-key "999999")
              missing-request (oc/object-edit-request :derived-unit
                                                      missing-unit-id
                                                      "Missing target"
                                                      {:request/id "edit-missing-target"
                                                       :edit/client-id "client-missing"
                                                       :edit/seq 1
                                                       :time-ms 7000})
              decision (append-and-await! runtime missing-request)
              stored-request (oc/read-request runtime missing-request)
              stored-decision (oc/read-decision runtime missing-request)]
          (is (= :rejected (:status decision)))
          (is (= :target/not-found (:reason decision)))
          (is (= missing-request (:raw-request stored-request)))
          (is (= decision stored-decision))
          (is (nil? (oc/read-unit runtime missing-unit-id)))))

      (testing "rejected source ingests are durable decisions and do not write source rows"
        (let [request (-> (oc/source-ingest-request "Bad hash"
                                                    "phase5/bad-source.md"
                                                    {:request/id "ingest-bad-hash"
                                                     :time-ms 7500})
                          (assoc-in [:payload :source-hash] "not-the-real-hash"))
              decision (append-and-await! runtime request)
              stored-request (oc/read-request runtime request)
              stored-decision (oc/read-decision runtime request)]
          (is (= :rejected (:status decision)))
          (is (= :request-invalid (:reason decision)))
          (is (some #(= :source/hash-mismatch (:type %)) (:errors decision)))
          (is (= request (:raw-request stored-request)))
          (is (= decision stored-decision))
          (is (nil? (oc/read-source runtime (source-id request))))))

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
              unit-id (:target-id (first (oc/read-outline runtime (:document-id one-line))))
              empty-edit (edit! runtime
                                :derived-unit
                                unit-id
                                ""
                                {:request/id "edit-empty-content"
                                 :edit/client-id "client-empty"
                                 :edit/seq 1
                                 :time-ms 8300})
              container-id (get-in empty-edit [:decision :event-row :target-id])
              container (oc/read-container runtime container-id)]
          (is (= :accepted (get-in empty-source [:decision :status])))
          (is (= "" (:source-raw-text (oc/read-source runtime (:source-id empty-source)))))
          (is (empty? (oc/read-outline runtime (:document-id empty-source))))
          (is (= "   \n\t" (:source-raw-text (oc/read-source runtime (:source-id whitespace-source)))))
          (is (empty? (oc/read-outline runtime (:document-id whitespace-source))))
          (is (= :accepted (get-in empty-edit [:decision :status])))
          (is (= "" (:current-content-text container)))
          (is (= 1 (count (oc/read-revision-history runtime container-id))))))

      (testing "outline range reads preserve source order for many derived blocks"
        (let [raw (->> (range 64)
                       (map #(str "- item-" %))
                       (str/join "\n"))
              large (ingest! runtime
                             raw
                             "phase5/large.md"
                             {:request/id "ingest-large"
                              :time-ms 9000})
              outline (oc/read-outline runtime (:document-id large))
              page (oc/read-outline runtime (:document-id large) "000050" 5)]
          (is (= 64 (count outline)))
          (is (= (mapv #(str "item-" %) (range 64)) (mapv :content-text outline)))
          (is (= ["item-50" "item-51" "item-52" "item-53" "item-54"]
                 (mapv :content-text page))))))))
