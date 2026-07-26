(ns app.cascade-table-test
  "Multi-cascade R1 executable gates. The production table has one live row;
   fixture rows exercise sibling isolation and the dark interval."
  (:require [app.server.cascade :as cascade]
            [app.server-jetty :as server]
            [app.server.rama.dogfood.llm :as llm]
            [app.server.rama.material-circulation :as circulation]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.relation-kernel :as rk]
            [app.shared.verb-registry :as verb-registry]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [clojure.tools.logging :as logging]
            [clojure.tools.logging.impl :as logging-impl])
  (:import (java.io PushbackReader StringReader)
           (java.nio.charset StandardCharsets)
           (java.util Arrays)
           (java.util.concurrent LinkedBlockingQueue TimeUnit)))

(defn throwing-fixture-handler!
  [ctx _payload]
  (deliver (:throwing-started ctx) true)
  (throw (ex-info "fixture row failed" {:fixture :throwing})))

(defn recording-fixture-handler!
  [ctx _payload]
  (deliver (:recording-started ctx) true)
  @(:release-recording ctx)
  (deliver (:recording-finished ctx) true))

(defn dark-fixture-handler!
  [ctx _payload]
  (deliver (:dark-called ctx) true))

(defn- canned-lines
  [output]
  ["{\"type\":\"system\",\"session_id\":\"cascade-table\"}"
   (json/write-str
    {:type "result"
     :is_error false
     :session_id "cascade-table"
     :total_cost_usd 0.0
     :result (json/write-str output)})])

(defn- start-runtime-set
  []
  {:llm-rt (llm/start-llm-runtime!)
   :oc-rt (ocr/start-object-container-runtime!)
   :rk-rt (rk/start-relation-runtime! {:tasks 4 :threads 2})})

(defn- close-runtime-set!
  [{:keys [llm-rt oc-rt rk-rt]}]
  (rk/close-relation-runtime! rk-rt)
  (ocr/close-object-container-runtime! oc-rt)
  (llm/close-llm-runtime! llm-rt))

(defn- take-result!
  [^LinkedBlockingQueue results]
  (.poll results 120 TimeUnit/SECONDS))

(defn- utf8-bytes=
  [a b]
  (Arrays/equals (.getBytes (str a) StandardCharsets/UTF_8)
                 (.getBytes (str b) StandardCharsets/UTF_8)))

(defn- read-top-level-forms
  [source]
  (with-open [reader (PushbackReader. (StringReader. source))]
    (loop [forms []]
      (let [form (read {:eof ::eof :read-cond :allow :features #{:clj}}
                       reader)]
        (if (= ::eof form)
          forms
          (recur (conj forms form)))))))

(defn- circulation-row-for-run
  [oc-rt object-key run-id]
  (some #(when (= run-id (get-in % [:circulation :run/id])) %)
        (ocr/read-transcript-conversation-projection
         oc-rt (str "oc:chat-conversation:" object-key) "" 100000)))

(defn- fixture-row
  [id trigger handler]
  {:cascade/id id
   :cascade/trigger trigger
   :cascade/handler handler
   :cascade/effect-class :pure-projection
   :cascade/idempotency "test-only: no durable transition"
   :cascade/actor "test"})

(deftest cascade-table-contract-gates
  (testing "G3 — loading cascade is pure and its top level is inert"
    (let [llm-boots (atom 0)]
      (with-redefs [llm/start-llm-runtime!
                    (fn [& _]
                      (swap! llm-boots inc)
                      (throw (ex-info "cascade load booted LLM" {})))]
        (require 'app.server.cascade :reload))
      (is (zero? @llm-boots)
          "fresh namespace evaluation must not start the LLM module"))
    (let [source (slurp (io/file "src/app/server/cascade.clj"))
          forms (read-top-level-forms source)
          heads (mapv first (filter seq? forms))
          declared (var-get (ns-resolve 'app.server.cascade 'declared-rows))]
      (is (not (str/includes? source "(defonce")))
      (is (not (str/includes? source "(delay")))
      (is (every? #{'ns 'def 'defn} heads)
          "cascade.clj has declarations only at top level")
      (is (vector? declared))
      (is (every? map? declared)
          "the sole def initializer is literal declaration data")))

  (testing "G5 — row #1 is complete, unique, printable, and vocabulary-bound"
    (let [rows (cascade/rows)
          row (first rows)]
      (is (= 1 (count rows))
          "the production table ships with exactly row #1")
      (is (= 1 (count (set (map :cascade/id rows))))
          "cascade ids are unique")
      (is (= :cascade/material-autotag (:cascade/id row)))
      (is (= :episode/turn-durable (:cascade/trigger row)))
      (is (= 'app.server-jetty/run-ambient-autotag!
             (:cascade/handler row)))
      (is (= :external-via-derived-worker
             (:cascade/effect-class row)))
      (is (contains? verb-registry/effect-classes
                     (:cascade/effect-class row)))
      (is (= circulation/autotag-actor-id (:cascade/actor row)))
      (is (str/includes? (:cascade/idempotency row)
                         "(object-key, input-hash, autotag-version, salt)"))
      (is (str/includes? (:cascade/idempotency row)
                         "no lifecycle transitions"))
      (is (= rows (read-string (pr-str rows)))
          "enumeration is printable EDN")))

  (testing "G1/G2 — the table preserves autotag identity and replay convergence"
    (let [object-key "chat:cascade-identity"
          source-unit-id "du:chat:cascade:source"
          target-unit-id "du:chat:cascade:target"
          source-id "src:chat:cascade:source"
          text "the spacing here makes the thread hard to read"
          target-text "thread placement policy"
          lines (canned-lines
                 {:target target-unit-id
                  :interpretation "concerns thread placement"
                  :confidence 0.91})
          candidates [{:id target-unit-id :text target-text}]
          direct-args {:record-unit-id source-unit-id
                       :record-text text
                       :candidates candidates
                       :evidence-source-id source-id
                       :timeout-ms 120000
                       :lines lines}
          payload {:object-key object-key
                   :source-unit-id source-unit-id
                   :text text
                   :receipt
                   {:receipt/visible-addresses
                    [source-unit-id target-unit-id target-unit-id]}
                   :gold-receipt nil
                   :lines lines}
          direct-runtime (start-runtime-set)
          direct-observation
          (try
            (let [result (circulation/autotag-material!
                          direct-runtime object-key direct-args)
                  record-row
                  (circulation-row-for-run
                   (:oc-rt direct-runtime) object-key (:run-id result))]
              {:result result
               :record-row record-row
               :record-id (:message-uuid record-row)})
            (finally
              (close-runtime-set! direct-runtime)))
          direct-result (:result direct-observation)
          table-runtime (start-runtime-set)
          results (LinkedBlockingQueue.)
          real-handler server/run-ambient-autotag!
          ambient-runtime-var
          (ns-resolve 'app.server-jetty 'ambient-autotag-runtime)
          fake-read-unit
          (fn [_runtime unit-id]
            (case unit-id
              "du:chat:cascade:source" {:unit {:source-id source-id}}
              "du:chat:cascade:target" {:content-text target-text}
              nil))
          capture-handler
          (fn [ctx event-payload]
            (let [result (real-handler ctx event-payload)]
              (.put results result)
              result))]
      (try
        (with-redefs-fn
          {ambient-runtime-var (delay (:llm-rt table-runtime))
           #'ocr/read-unit fake-read-unit
           #'server/run-ambient-autotag! capture-handler}
          (fn []
            (is (= [{:cascade/id :cascade/material-autotag
                     :dispatched? true}]
                   (cascade/react!
                    (select-keys table-runtime [:oc-rt :rk-rt])
                    :episode/turn-durable
                    payload)))
            (let [table-result (take-result! results)
                  direct-record-id (:record-id direct-observation)
                  table-record-row
                  (circulation-row-for-run
                   (:oc-rt table-runtime) object-key (:run-id table-result))
                  table-record-id
                  (:message-uuid table-record-row)]
              (is (some? table-result)
                  "the through-table handler completed")
              (is (= :completed (:status direct-result)
                     (:status table-result)))
              (is (utf8-bytes= (:run-id direct-result)
                               (:run-id table-result))
                  "run-id bytes are behavior-identical")
              (is (some? (:record-row direct-observation))
                  "the direct call materialized a physical projection row")
              (is (some? table-record-row)
                  "the through-table call materialized a physical projection row")
              (is (string? direct-record-id)
                  "the direct durable projection exposes its record id")
              (is (string? table-record-id)
                  "the through-table durable projection exposes its record id")
              (is (utf8-bytes= direct-record-id table-record-id)
                  "actual durable projection record-id bytes are behavior-identical")
              (is (utf8-bytes= (get-in direct-result [:edge :relation-id])
                               (get-in table-result [:edge :relation-id]))
                  "edge relation-id bytes are behavior-identical"))

            (is (= [{:cascade/id :cascade/material-autotag
                     :dispatched? true}]
                   (cascade/react!
                    (select-keys table-runtime [:oc-rt :rk-rt])
                    :episode/turn-durable
                    payload)))
            (let [replay (take-result! results)]
              (is (= :already-recorded (:status replay)))
              (is (false? (:adapter-called? replay))
                  "the identical second emission never invokes the adapter"))

            (let [adapter-called? (atom false)]
              (with-redefs [circulation/autotag-material!
                            (fn [& _]
                              (reset! adapter-called? true)
                              {:status :unexpected})]
                (is (= [{:cascade/id :cascade/material-autotag
                         :dispatched? true}]
                       (cascade/react!
                        (select-keys table-runtime [:oc-rt :rk-rt])
                        :episode/turn-durable
                        (assoc payload :gold-receipt {:receipt/gold true}))))
                (is (= :skipped (:status (take-result! results))))
                (is (false? @adapter-called?)
                    "gold receipt decline is owned by the handler head")

                (is (= [{:cascade/id :cascade/material-autotag
                         :dispatched? true}]
                       (cascade/react!
                        (select-keys table-runtime [:oc-rt :rk-rt])
                        :episode/turn-durable
                        (assoc payload :gold-receipt false))))
                (is (= :skipped (:status (take-result! results)))
                    "false is present under the old nil-only gold guard")

                (is (= [{:cascade/id :cascade/material-autotag
                         :dispatched? true}]
                       (cascade/react!
                        {:oc-rt (:oc-rt table-runtime) :rk-rt false}
                        :episode/turn-durable
                        payload)))
                (is (= :skipped (:status (take-result! results)))
                    "a false runtime preserves the old guard's truth semantics")))))
        (finally
          (close-runtime-set! table-runtime)))))

  (testing "G4/G8 — siblings isolate, receipts preserve order, dark row stays inert"
    (let [throwing-started (promise)
          recording-started (promise)
          release-recording (promise)
          recording-finished (promise)
          dark-called (promise)
          failure-logged (promise)
          logs (atom [])
          fixture
          [(fixture-row :cascade/test-throw
                        :test/two-row-trigger
                        'app.cascade-table-test/throwing-fixture-handler!)
           (fixture-row :cascade/test-record
                        :test/two-row-trigger
                        'app.cascade-table-test/recording-fixture-handler!)
           (fixture-row :cascade/test-dark
                        :test/never-emitted
                        'app.cascade-table-test/dark-fixture-handler!)]
          ctx {:throwing-started throwing-started
               :recording-started recording-started
               :release-recording release-recording
               :recording-finished recording-finished
               :dark-called dark-called}]
      (with-redefs [cascade/rows (constantly fixture)
                    logging-impl/enabled? (fn [_logger _level] true)
                    logging/log*
                    (fn [_logger level throwable message]
                      (swap! logs conj {:level level
                                        :throwable throwable
                                        :message message})
                      (when (str/includes? message "[CASCADE][FAILED]")
                        (deliver failure-logged message)))]
        (is (= fixture (cascade/rows))
            "the test-only dark row enumerates as declared")
        (let [receipts (cascade/react! ctx :test/two-row-trigger
                                       {:source-unit-id "du:test:cascade"})]
          (is (= [{:cascade/id :cascade/test-throw :dispatched? true}
                  {:cascade/id :cascade/test-record :dispatched? true}]
                 receipts)
              "receipt order is declaration order")
          (is (= true (deref throwing-started 5000 ::timeout)))
          (is (= true (deref recording-started 5000 ::timeout)))
          (is (not (realized? recording-finished))
              "react! returned while the sibling handler remained blocked")
          (deliver release-recording true)
          (is (= true (deref recording-finished 5000 ::timeout)))
          (let [failed-message (deref failure-logged 5000 ::timeout)]
            (is (string? failed-message))
            (is (str/includes? failed-message "[CASCADE][FAILED]"))
            (is (str/includes? failed-message ":cascade/test-throw"))
            (is (str/includes? failed-message ":test/two-row-trigger")))
          (is (= 2
                 (count
                  (filter #(and (= :info (:level %))
                                (str/includes? (:message %) "[CASCADE]"))
                          @logs)))
              "each fired row logs its id and trigger")
          (is (not (realized? dark-called))
              "the row on an un-emitted trigger stayed dark"))))))
