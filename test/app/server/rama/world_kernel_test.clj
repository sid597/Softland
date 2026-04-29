(ns app.server.rama.world-kernel-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.server.rama.core :as kernel]))

(deftest kernel-event-envelope-test
  (testing "The action request enters Rama before a KernelEvent exists"
    (let [request (kernel/ingest-text-request
                    "alpha\nbeta"
                    {:request-id "req_test"
                     :proposed-event-id "evt_test"
                     :artifact-id "art_test"
                     :revision-id "rev_test"
                     :time-ms 1})]
      (is (= :artifact/ingest (:request/type request)))
      (is (= (:request/type request) (get-in request [:action :action/type])))
      (is (= "req_test" (:request/id request)))
      (is (= "evt_test" (:proposed/event-id request)))
      (is (not (contains? request :event/id)))
      (is (= [:artifact "art_test"] (:routing/key request)))
      (is (nil? (get-in request [:payload :event/id])))
      (is (kernel/valid-request? request))
      (is (not (kernel/valid-event? request)))
      (is (= :artifact/create (get-in request [:action :action/capability])))
      (is (= :text (get-in request [:payload :artifact/type])))))

  (testing "The accepted KernelEvent envelope is carrier-independent"
    (let [event (kernel/text-artifact-event
                  "alpha\nbeta"
                  {:artifact-id "art_test"
                   :revision-id "rev_test"
                   :event-id "evt_test"
                   :time-ms 1})]
      (is (kernel/valid-event? event))
      (is (= :artifact/ingested (:event/type event)))
      (is (= {:target/kind :artifact
              :target/id "art_test"
              :target/address nil}
             (:target event)))
      (is (= [:artifact "art_test"] (get-in event [:ordering :key])))
      (is (= :text (get-in event [:payload :artifact/type]))))))

(deftest action-request-contract-validation-test
  (testing "ActionRequest keeps routing and proposed event ids explicit"
    (let [request (kernel/ingest-text-request
                    "alpha"
                    {:request-id "req_contract"
                     :proposed-event-id "evt_contract"
                     :artifact-id "art_contract"
                     :revision-id "rev_contract"})
          error-types #(set (map :type (kernel/request-validation-errors %)))]
      (is (:transitional? kernel/routing-key-contract))
      (is (kernel/valid-request? request))
      (is (= [:artifact "art_contract"] (:routing/key request)))
      (is (= "evt_contract" (:proposed/event-id request)))
      (is (contains? (error-types (assoc-in request [:action :action/type] :artifact/delete))
                     :request/action-type-drift))
      (is (contains? (error-types (assoc-in request [:payload :event/id] "evt_hidden"))
                     :request/payload-event-id))
      (is (contains? (error-types (assoc request :routing/key nil))
                     :routing/key-invalid)))))

(deftest line-unitization-preserves-provenance-test
  (testing "Text line units are a carrier instance inside the general kernel"
    (let [artifact-event (kernel/text-artifact-event
                           "keep\nreject\nalso keep"
                           {:artifact-id "art_lines"
                            :revision-id "rev_lines"
                            :event-id "evt_ingest"})
          units (kernel/line-units artifact-event)]
      (is (= 3 (count units)))
      (is (= ["keep" "reject" "also keep"] (mapv :unit/preview units)))
      (is (every? #(= "evt_ingest" (get-in % [:provenance :root-event-id])) units))
      (is (every? #(= :text/range (get-in % [:anchor :anchor/type])) units)))))

(deftest rama-v0-text-loop-test
  (testing "Rama receives action requests, decides them, materializes text state, and reads projections"
    (let [runtime (kernel/start-kernel-runtime!)]
      (try
        (let [artifact-event (kernel/ingest-text!
                               runtime
                               "keep\nreject\nalso keep"
                               {:request-id "req_loop_ingest"
                                :artifact-id "art_loop"
                                :revision-id "rev_loop"
                                :proposed-event-id "evt_loop_ingest"
                                :time-ms 1})
              artifact-id (get-in artifact-event [:payload :artifact/id])
              ingest-request (kernel/read-request runtime "req_loop_ingest")
              ingest-decision (kernel/read-decision runtime "req_loop_ingest")
              units (kernel/unitize-lines! runtime artifact-event)
              rejected-unit-id (:unit/id (second units))
              status-event (kernel/set-unit-status!
                              runtime
                              rejected-unit-id
                              :rejected
                              {:request-id "req_loop_reject"
                               :proposed-event-id "evt_loop_reject"
                               :artifact-id artifact-id
                               :time-ms 2
                               :reason "not canonical"})
              status-request (kernel/read-request runtime "req_loop_reject")
              status-decision (kernel/read-decision runtime "req_loop_reject")
              _ (kernel/await-materialized #(kernel/read-units runtime artifact-id)
                                           #(= 3 (count %)))
              _ (kernel/await-materialized #(kernel/read-unit-statuses runtime kernel/default-branch-id)
                                           #(contains? % rejected-unit-id))
              artifact (kernel/read-artifact runtime artifact-id)
              text-head (kernel/read-text-head runtime artifact-id)
              stored-status (get (kernel/read-unit-statuses runtime kernel/default-branch-id)
                                 rejected-unit-id)
              canonical (kernel/read-canonical-view runtime kernel/default-branch-id artifact-id)
              discarded (kernel/read-discarded-view runtime kernel/default-branch-id artifact-id)]
          (is (= :artifact/ingest (:request/type ingest-request)))
          (is (= [:artifact artifact-id] (:routing/key ingest-request)))
          (is (= "evt_loop_ingest" (:proposed/event-id ingest-request)))
          (is (nil? (get-in ingest-request [:payload :event/id])))
          (is (= :accepted (:decision/status ingest-decision)))
          (is (= (:routing/key ingest-request) (:routing/key ingest-decision)))
          (is (= "evt_loop_ingest" (:event/id (:event ingest-decision))))
          (is (= :unit/status-set (:request/type status-request)))
          (is (= [:artifact artifact-id] (:routing/key status-request)))
          (is (= "evt_loop_reject" (:proposed/event-id status-request)))
          (is (nil? (get-in status-request [:payload :event/id])))
          (is (= :accepted (:decision/status status-decision)))
          (is (= (:routing/key status-request) (:routing/key status-decision)))
          (is (= "evt_loop_ingest" (:root-event-id artifact)))
          (is (= "keep\nreject\nalso keep" (:text/content text-head)))
          (is (= :unit/status-set (:event/type status-event)))
          (is (= :rejected (:status stored-status)))
          (is (= ["keep" "also keep"] (mapv :preview canonical)))
          (is (= ["reject"] (mapv :preview discarded)))
          (is (every? #(= :unit (get-in % [:target :target/kind])) canonical))
          (is (= "evt_loop_ingest" (get-in (first canonical) [:provenance :root-event-id]))))
        (finally
          (kernel/close-kernel-runtime! runtime))))))

(deftest hidden-event-id-request-is-rejected-test
  (testing "A proposed event id must not be smuggled through request payload"
    (let [runtime (kernel/start-kernel-runtime!)]
      (try
        (let [request (-> (kernel/ingest-text-request
                            "alpha"
                            {:request-id "req_hidden_event"
                             :artifact-id "art_hidden_event"
                             :revision-id "rev_hidden_event"})
                          (assoc-in [:payload :event/id] "evt_hidden"))
              _ (kernel/append-action-request! runtime request)
              decision (kernel/await-decision runtime "req_hidden_event")
              error-types (set (map :type (:errors decision)))]
          (is (= request (kernel/read-request runtime "req_hidden_event")))
          (is (= :rejected (:decision/status decision)))
          (is (= (:routing/key request) (:routing/key decision)))
          (is (nil? (:event/id decision)))
          (is (= :request-invalid (:decision/reason decision)))
          (is (contains? error-types :request/payload-event-id))
          (is (nil? (kernel/read-event runtime "evt_hidden"))))
        (finally
          (kernel/close-kernel-runtime! runtime))))))

(deftest malformed-unknown-action-is-validated-before-dispatch-test
  (testing "Malformed unknown actions fail common request validation before unknown-action dispatch"
    (let [runtime (kernel/start-kernel-runtime!)]
      (try
        (let [request (kernel/action-request
                        {:request-id "req_bad_unknown"
                         :request-type :unknown/action
                         :target {:target/kind :artifact
                                  :target/id "art_unknown"
                                  :target/address nil}
                         :action {:action/type :unknown/action
                                  :action/capability :world/append
                                  :action/params {}}
                         :payload {:artifact/id "art_unknown"
                                   :event/id "evt_bad_unknown"}})
              _ (kernel/append-action-request! runtime request)
              decision (kernel/await-decision runtime "req_bad_unknown")
              error-types (set (map :type (:errors decision)))]
          (is (= request (kernel/read-request runtime "req_bad_unknown")))
          (is (= :rejected (:decision/status decision)))
          (is (= (:routing/key request) (:routing/key decision)))
          (is (= :request-invalid (:decision/reason decision)))
          (is (not= :unknown-action-type (:decision/reason decision)))
          (is (contains? error-types :request/payload-event-id))
          (is (nil? (:event/id decision)))
          (is (nil? (kernel/read-event runtime "evt_bad_unknown"))))
        (finally
          (kernel/close-kernel-runtime! runtime))))))

(deftest rejected-action-request-is-durable-test
  (testing "A rejected request still enters Rama and materializes an ActionDecision"
    (let [runtime (kernel/start-kernel-runtime!)]
      (try
        (let [request (kernel/unit-status-request
                        "missing_art/line/9"
                        :rejected
                        {:request-id "req_missing_unit"
                         :proposed-event-id "evt_should_not_exist"
                         :artifact-id "missing_art"})
              _ (kernel/append-action-request! runtime request)
              decision (kernel/await-decision runtime "req_missing_unit")]
          (is (= request (kernel/read-request runtime "req_missing_unit")))
          (is (= :rejected (:decision/status decision)))
          (is (= (:routing/key request) (:routing/key decision)))
          (is (nil? (:event/id decision)))
          (is (= :target-unit-not-found (:decision/reason decision)))
          (is (nil? (kernel/read-event runtime "evt_should_not_exist"))))
        (finally
          (kernel/close-kernel-runtime! runtime))))))
