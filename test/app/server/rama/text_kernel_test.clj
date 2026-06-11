(ns app.server.rama.text-kernel-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.server.rama.core :as core]
            [app.server.rama.text-kernel :as text-kernel]))

(deftest kernel-event-envelope-test
  (testing "The action request enters Rama before a KernelEvent exists"
    (let [request (text-kernel/ingest-text-request
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
      (is (core/valid-request? request))
      (is (not (core/valid-event? request)))
      (is (= :artifact/create (get-in request [:action :action/capability])))
      (is (= :text (get-in request [:payload :artifact/type])))))

  (testing "The accepted KernelEvent envelope is carrier-independent"
    (let [event (text-kernel/text-artifact-event
                  "alpha\nbeta"
                  {:artifact-id "art_test"
                   :revision-id "rev_test"
                   :event-id "evt_test"
                   :time-ms 1})]
      (is (core/valid-event? event))
      (is (= :artifact/ingested (:event/type event)))
      (is (= {:target/kind :artifact
              :target/id "art_test"
              :target/address nil}
             (:target event)))
      (is (= [:artifact "art_test"] (get-in event [:ordering :key])))
      (is (= :text (get-in event [:payload :artifact/type]))))))

(deftest action-request-contract-validation-test
  (testing "ActionRequest keeps routing and proposed event ids explicit"
    (let [request (text-kernel/ingest-text-request
                    "alpha"
                    {:request-id "req_contract"
                     :proposed-event-id "evt_contract"
                     :artifact-id "art_contract"
                     :revision-id "rev_contract"})
          error-types #(set (map :type (core/request-validation-errors %)))]
      (is (:transitional? text-kernel/routing-key-contract))
      (is (core/valid-request? request))
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
    (let [artifact-event (text-kernel/text-artifact-event
                           "keep\nreject\nalso keep"
                           {:artifact-id "art_lines"
                            :revision-id "rev_lines"
                            :event-id "evt_ingest"})
          units (text-kernel/line-units artifact-event)]
      (is (= 3 (count units)))
      (is (= ["keep" "reject" "also keep"] (mapv :unit/preview units)))
      (is (every? #(= "evt_ingest" (get-in % [:provenance :root-event-id])) units))
      (is (every? #(= :text/range (get-in % [:anchor :anchor/type])) units)))))

(deftest rama-v0-text-loop-test
  (testing "Rama receives action requests, decides them, materializes text state, and reads projections"
    (let [runtime (text-kernel/start-text-runtime!)]
      (try
        (let [artifact-event (text-kernel/ingest-text!
                               runtime
                               "keep\nreject\nalso keep"
                               {:request-id "req_loop_ingest"
                                :artifact-id "art_loop"
                                :revision-id "rev_loop"
                                :proposed-event-id "evt_loop_ingest"
                                :time-ms 1})
              artifact-id (get-in artifact-event [:payload :artifact/id])
              routing-key [:artifact artifact-id]
              ingest-request (text-kernel/read-request runtime routing-key "req_loop_ingest")
              ingest-decision (text-kernel/read-decision runtime routing-key "req_loop_ingest")
              units (text-kernel/unitize-lines! runtime artifact-event)
              rejected-unit-id (:unit/id (second units))
              status-event (text-kernel/set-unit-status!
                              runtime
                              rejected-unit-id
                              :rejected
                              {:request-id "req_loop_reject"
                               :proposed-event-id "evt_loop_reject"
                               :artifact-id artifact-id
                               :time-ms 2
                               :reason "not canonical"})
              status-request (text-kernel/read-request runtime routing-key "req_loop_reject")
              status-decision (text-kernel/read-decision runtime routing-key "req_loop_reject")
              _ (text-kernel/await-materialized #(text-kernel/read-units runtime artifact-id)
                                           #(= 3 (count %)))
              _ (text-kernel/await-materialized #(text-kernel/read-unit-statuses runtime artifact-id core/default-branch-id)
                                           #(contains? % rejected-unit-id))
              artifact (text-kernel/read-artifact runtime artifact-id)
              text-head (text-kernel/read-text-head runtime artifact-id)
              stored-status (get (text-kernel/read-unit-statuses runtime artifact-id core/default-branch-id)
                                 rejected-unit-id)
              canonical (text-kernel/read-canonical-view runtime core/default-branch-id artifact-id)
              discarded (text-kernel/read-discarded-view runtime core/default-branch-id artifact-id)]
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
          (text-kernel/close-text-runtime! runtime))))))

(deftest hidden-event-id-request-is-rejected-test
  (testing "A proposed event id must not be smuggled through request payload"
    (let [runtime (text-kernel/start-text-runtime!)]
      (try
        (let [request (-> (text-kernel/ingest-text-request
                            "alpha"
                            {:request-id "req_hidden_event"
                             :artifact-id "art_hidden_event"
                             :revision-id "rev_hidden_event"})
                          (assoc-in [:payload :event/id] "evt_hidden"))
              _ (text-kernel/append-action-request! runtime request)
              decision (text-kernel/await-decision runtime (:routing/key request) "req_hidden_event")
              error-types (set (map :type (:errors decision)))]
          (is (= request (text-kernel/read-request runtime (:routing/key request) "req_hidden_event")))
          (is (= :rejected (:decision/status decision)))
          (is (= (:routing/key request) (:routing/key decision)))
          (is (nil? (:event/id decision)))
          (is (= :request-invalid (:decision/reason decision)))
          (is (contains? error-types :request/payload-event-id))
          (is (nil? (text-kernel/read-event runtime (:routing/key request) "evt_hidden"))))
        (finally
          (text-kernel/close-text-runtime! runtime))))))

(deftest malformed-unknown-action-is-validated-before-dispatch-test
  (testing "Malformed unknown actions fail common request validation before unknown-action dispatch"
    (let [runtime (text-kernel/start-text-runtime!)]
      (try
        (let [request (core/action-request
                        {:request-id "req_bad_unknown"
                         :request-type :unknown/action
                         :target {:target/kind :artifact
                                  :target/id "art_unknown"
                                  :target/address nil}
                         :action {:action/type :unknown/action
                                  :action/capability :action/append
                                  :action/params {}}
                         :payload {:artifact/id "art_unknown"
                                   :event/id "evt_bad_unknown"}})
              _ (text-kernel/append-action-request! runtime request)
              decision (text-kernel/await-decision runtime (:routing/key request) "req_bad_unknown")
              error-types (set (map :type (:errors decision)))]
          (is (= request (text-kernel/read-request runtime (:routing/key request) "req_bad_unknown")))
          (is (= :rejected (:decision/status decision)))
          (is (= (:routing/key request) (:routing/key decision)))
          (is (= :request-invalid (:decision/reason decision)))
          (is (not= :unknown-action-type (:decision/reason decision)))
          (is (contains? error-types :request/payload-event-id))
          (is (nil? (:event/id decision)))
          (is (nil? (text-kernel/read-event runtime (:routing/key request) "evt_bad_unknown"))))
        (finally
          (text-kernel/close-text-runtime! runtime))))))

(deftest rejected-action-request-is-durable-test
  (testing "A rejected request still enters Rama and materializes an ActionDecision"
    (let [runtime (text-kernel/start-text-runtime!)]
      (try
        (let [request (text-kernel/unit-status-request
                        "missing_art/line/9"
                        :rejected
                        {:request-id "req_missing_unit"
                         :proposed-event-id "evt_should_not_exist"
                         :artifact-id "missing_art"})
              _ (text-kernel/append-action-request! runtime request)
              decision (text-kernel/await-decision runtime (:routing/key request) "req_missing_unit")]
          (is (= request (text-kernel/read-request runtime (:routing/key request) "req_missing_unit")))
          (is (= :rejected (:decision/status decision)))
          (is (= (:routing/key request) (:routing/key decision)))
          (is (nil? (:event/id decision)))
          (is (= :target-unit-not-found (:decision/reason decision)))
          (is (nil? (text-kernel/read-event runtime (:routing/key request) "evt_should_not_exist"))))
        (finally
          (text-kernel/close-text-runtime! runtime))))))
