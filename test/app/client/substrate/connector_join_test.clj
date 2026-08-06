(ns app.client.substrate.connector-join-test
  (:require [app.client.substrate.connector-material :as material]
            [app.client.substrate.connector-route :as route]
            [app.client.workspace.live-edges :as live-edges]
            [app.server.rama.relation-kernel :as rk]
            [com.rpl.rama.test :as rtest]
            [clojure.test :refer [deftest is]]))

(def topology-name "relation-kernel-topology")

(defn effective [tx]
  {:affine [1.0 0.0 0.0 1.0 tx 0.0]
   :flags 0 :transport-slot (long tx)})

(deftest durable-r1-row-projects-and-routes-through-public-truth
  (let [runtime (rk/start-relation-runtime! {:tasks 2 :threads 2})]
    (try
      (let [from-address "connector-fixture/from"
            to-address "connector-fixture/to"
            from (rk/->target-ref :derived-unit from-address)
            to (rk/->target-ref :derived-unit to-address)
            request (rk/assert-request
                     {:kind :references :from from :to to
                      :asserter-actor-id "sid" :asserter-type :human
                      :asserted-at-ms 1000 :sent-at-ms 1000
                      :request-id "connector-join-request"
                      :idempotency-key "connector-join-idempotency"})]
        (rk/append-relation-request! runtime request)
        (rtest/wait-for-microbatch-processed-count
         (:ipc runtime) (:module-name runtime) topology-name 1 30000)
        (let [response (live-edges/read-live-edges
                        runtime [from-address to-address])
              rows (:rows response)
              projected (route/project-edge-row (first rows))
              targets {from-address [{:vi :from/vi :container 1
                                      :container-idx 1
                                      :bounds {:x 0 :y 0 :w 10 :h 10}}]
                       to-address [{:vi :to/vi :container 2
                                    :container-idx 2
                                    :bounds {:x 40 :y 0 :w 10 :h 10}}]}
              op {:container 0 :container-idx 0
                  :connector/material projected}
              instance (first (route/expand-edge-instances [op] targets))
              resolved (route/resolve-route-geometry
                        instance {0 (effective 0) 1 (effective 0) 2 (effective 0)})]
          (is (= 1 (count rows)))
          (is (= :relation-kernel/r1 (:source response)))
          (is (true? (:zero-writes response)))
          (is (= :asserted (:connector/status projected)))
          (is (= {:actor-id "sid" :asserter-type :human}
                 (:connector/provenance projected)))
          (is (= [(:event-id (first rows)) 0]
                 (material/composite-revision projected)))
          (is (= [(:relation-id (first rows)) :from/vi :to/vi]
                 (:connector/edge-instance-id instance)))
          (is (= :resolved (:status resolved)))
          (is (material/hit? resolved [25.0 5.0]))))
      (finally
        (rk/close-relation-runtime! runtime)))))
