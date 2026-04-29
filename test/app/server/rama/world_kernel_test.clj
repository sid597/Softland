(ns app.server.rama.world-kernel-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.server.rama.core :as kernel]))

(deftest kernel-event-envelope-test
  (testing "The action request enters Rama before a KernelEvent exists"
    (let [request (kernel/ingest-text-request
                    "alpha\nbeta"
                    {:request-id "req_test"
                     :event-id "evt_test"
                     :artifact-id "art_test"
                     :revision-id "rev_test"
                     :time-ms 1})]
      (is (= :artifact/ingest (:request/type request)))
      (is (= "req_test" (:request/id request)))
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
                                :event-id "evt_loop_ingest"
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
                               :event-id "evt_loop_reject"
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
          (is (= :accepted (:decision/status ingest-decision)))
          (is (= "evt_loop_ingest" (:event/id (:event ingest-decision))))
          (is (= :unit/status-set (:request/type status-request)))
          (is (= :accepted (:decision/status status-decision)))
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

(deftest rejected-action-request-is-durable-test
  (testing "A rejected request still enters Rama and materializes an ActionDecision"
    (let [runtime (kernel/start-kernel-runtime!)]
      (try
        (let [request (kernel/unit-status-request
                        "missing_art/line/9"
                        :rejected
                        {:request-id "req_missing_unit"
                         :event-id "evt_should_not_exist"
                         :artifact-id "missing_art"})
              _ (kernel/append-action-request! runtime request)
              decision (kernel/await-decision runtime "req_missing_unit")]
          (is (= request (kernel/read-request runtime "req_missing_unit")))
          (is (= :rejected (:decision/status decision)))
          (is (= :target-unit-not-found (:reason decision)))
          (is (nil? (kernel/read-event runtime "evt_should_not_exist"))))
        (finally
          (kernel/close-kernel-runtime! runtime))))))
