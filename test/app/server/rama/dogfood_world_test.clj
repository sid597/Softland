(ns app.server.rama.dogfood-world-test
  (:require [app.server.rama.dogfood.llm :as llm]
            [app.server.rama.dogfood.world :as world]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(defn with-world-runtime
  [f]
  (let [runtime (world/start-world-runtime!)]
    (try
      (f runtime)
      (finally
        (world/close-world-runtime! runtime)))))

(defn append-and-await-decision!
  [runtime request]
  (world/append-world-action! runtime request)
  (world/await-decision runtime (:request/id request)))

(defn append-send-and-await-run!
  [runtime {:keys [world-thread-id world-turn-id bundle-id run-id thread-id request-id]}]
  (let [request (world/compose-and-send-request
                  world-thread-id
                  (str "Prompt for " run-id)
                  {:request-id request-id
                   :time-ms 100
                   :payload {:world-turn/id world-turn-id
                             :context-bundle/id bundle-id
                             :llm-turn-run/id run-id
                             :llm-thread/id thread-id
                             :executor/task-id llm/pending-task-id}})]
    (append-and-await-decision! runtime request)
    (llm/await-run runtime run-id #(= :pending (:status %)))
    request))

(defn append-approval-observation!
  [runtime {:keys [run-id thread-id approval-id native-id]}]
  (llm/append-observation!
    runtime
    (llm/observation
      run-id
      thread-id
      :codex/approval-request
      0
      {:observation-id (str approval-id "/obs")
       :approval/id approval-id
       :approval/type :exec
       :native/json-rpc-request-id native-id
       :codex/event-method "item/cmdExec/requestApproval"
       :codex/event-params {:cmd "echo approval"}}))
  (llm/await-materialized
    #(llm/read-pending-approval runtime approval-id)
    some?))

(deftest world-thread-create-test
  (with-world-runtime
    (fn [runtime]
      (testing "world-thread/create materializes a user-facing WorldThread"
        (let [request (world/world-thread-create-request
                        "chat-A"
                        {:request-id "req-create-chat-A"
                         :time-ms 1
                         :title "Chat A"
                         :actor {:actor/id "sid"
                                 :actor/type :human}})
              decision (append-and-await-decision! runtime request)
              thread (world/await-thread runtime "chat-A" some?)]
          (is (= :accepted (:decision/status decision)))
          (is (= [:world-thread "chat-A"] (:routing/key decision)))
          (is (= ["req-create-chat-A/event/world-thread"] (:event/ids decision)))
          (is (= "Chat A" (:title thread)))
          (is (= :active (:status thread)))
          (is (= 0 (:turn-count thread)))
          (is (= [] (world/read-turns-by-thread runtime "chat-A")))
          (is (= :world-thread/created
                 (:event/type (world/read-event runtime (:event/id decision))))))))))

(deftest compose-and-send-freezes-context-bundle-test
  (with-world-runtime
    (fn [runtime]
      (testing "compose-and-send creates thread, turn, order row, and immutable bundle"
        (let [request (world/compose-and-send-request
                        "chat-B"
                        "Explain the contract."
                        {:request-id "req-send-chat-B"
                         :time-ms 10
                         :title "Contract chat"
                         :payload {:world-turn/id "WT-1"
                                   :context-bundle/id "B-1"
                                   :refs [{:object/id "note-17"}
                                          {:slice/id "slice-3"}]
                                   :llm/options {:agent/kind :codex
                                                 :model "gpt-5.2-codex"
                                                 :approval-policy :on-request
                                                 :sandbox :workspace-write
                                                 :cwd "/mnt/data/projects/Softland"
                                                 :executor/pool :not-bundle-owned}}})
              decision (append-and-await-decision! runtime request)
              thread (world/await-thread runtime "chat-B" #(= 1 (:turn-count %)))
              turn (world/await-turn runtime "WT-1" some?)
              bundle (world/await-materialized
                       #(world/read-context-bundle runtime "B-1")
                       some?)]
          (is (= :accepted (:decision/status decision)))
          (is (= ["req-send-chat-B/event/world-thread"
                  "req-send-chat-B/event/world-turn"
                  "req-send-chat-B/event/context-bundle"
                  "req-send-chat-B/event/llm-turn-run"]
                 (:event/ids decision)))
          (is (= "Contract chat" (:title thread)))
          (is (= ["WT-1"] (world/read-turns-by-thread runtime "chat-B")))
          (is (= :compose-and-send (:world-turn/kind turn)))
          (is (= "B-1" (:context-bundle/id turn)))
          (is (= "B-1" (world/read-context-bundle-by-turn runtime "WT-1")))
          (is (= {:agent/kind :codex
                  :model "gpt-5.2-codex"
                  :approval-policy :on-request
                  :sandbox :workspace-write
                  :cwd "/mnt/data/projects/Softland"}
                 (:execution/options bundle)))
          (is (not (contains? (:execution/options bundle) :executor/pool)))
          (is (str/starts-with? (:context-bundle/hash bundle) "sha256:"))
          (is (str/includes? (:rendered/model-input bundle)
                             "Explain the contract."))
          (is (str/includes? (:rendered/model-input bundle) "object:note-17"))
          (is (str/includes? (:rendered/model-input bundle) "slice:slice-3")))))))

(deftest world-catalog-materializes-eager-objects-test
  (with-world-runtime
    (fn [runtime]
      (testing "accepted world facts become catalog objects and relation edges"
        (let [request (world/compose-and-send-request
                        "chat-catalog"
                        "Catalog this send."
                        {:request-id "req-catalog"
                         :time-ms 15
                         :title "Catalog chat"
                         :payload {:world-turn/id "WT-catalog"
                                   :context-bundle/id "B-catalog"
                                   :llm-turn-run/id "run-catalog"
                                   :llm-thread/id "llm-thread-catalog"}})
              _decision (append-and-await-decision! runtime request)
              thread-object-id (world/catalog-object-id :world-thread "chat-catalog")
              turn-object-id (world/catalog-object-id :world-turn "WT-catalog")
              bundle-object-id (world/catalog-object-id :context-bundle "B-catalog")
              run-object-id (world/catalog-object-id :llm-turn-run "run-catalog")
              thread-object (world/await-materialized
                              #(world/read-object runtime thread-object-id)
                              some?)
              turn-object (world/await-materialized
                            #(world/read-object runtime turn-object-id)
                            some?)
              bundle-object (world/await-materialized
                              #(world/read-object runtime bundle-object-id)
                              some?)
              run-object (world/await-materialized
                           #(world/read-object runtime run-object-id)
                           some?)
              thread-out (world/await-materialized
                           #(world/read-artifact-graph runtime thread-object-id)
                           #(= #{turn-object-id}
                               (set (map :to/object-id (vals %)))))
              turn-out (world/await-materialized
                         #(world/read-artifact-graph runtime turn-object-id)
                         #(= #{bundle-object-id run-object-id}
                             (set (map :to/object-id (vals %)))))
              bundle-out (world/await-materialized
                           #(world/read-artifact-graph runtime bundle-object-id)
                           #(= #{run-object-id}
                               (set (map :to/object-id (vals %)))))
              run-in (world/await-materialized
                       #(world/read-artifact-graph-in runtime run-object-id)
                       #(= #{turn-object-id bundle-object-id}
                           (set (map :from/object-id (vals %)))))]
          (is (= :world-thread (:object/type thread-object)))
          (is (= :world-turn (:object/type turn-object)))
          (is (= :context-bundle (:object/type bundle-object)))
          (is (= :llm-turn-run (:object/type run-object)))
          (is (= "Catalog chat" (:title thread-object)))
          (is (= "B-catalog" (:context-bundle/id turn-object)))
          (is (= "WT-catalog" (:world-turn/id bundle-object)))
          (is (= "llm-thread-catalog" (:llm-thread/id run-object)))
          (is (= #{turn-object-id}
                 (set (map :to/object-id (vals thread-out)))))
          (is (= #{bundle-object-id run-object-id}
                 (set (map :to/object-id (vals turn-out)))))
          (is (= #{run-object-id}
                 (set (map :to/object-id (vals bundle-out)))))
          (is (= #{turn-object-id bundle-object-id}
                 (set (map :from/object-id (vals run-in))))))))))

(deftest world-only-turn-no-context-bundle-test
  (with-world-runtime
    (fn [runtime]
      (testing "world-only turns are ordered material but do not freeze a model input"
        (let [create-request (world/world-thread-create-request
                               "chat-C"
                               {:request-id "req-create-chat-C"
                                :time-ms 20})
              comment-request (world/world-only-turn-request
                                :world-turn/comment-create
                                "chat-C"
                                {:request-id "req-comment-chat-C"
                                 :time-ms 21
                                 :payload {:world-turn/id "WT-comment"
                                           :prompt/text "Actually, pin this nuance."
                                           :refs [{:world-turn/id "WT-older"}]}})]
          (append-and-await-decision! runtime create-request)
          (let [decision (append-and-await-decision! runtime comment-request)
                turn (world/await-turn runtime "WT-comment" some?)
                thread (world/await-thread runtime "chat-C" #(= 1 (:turn-count %)))]
            (is (= :accepted (:decision/status decision)))
            (is (= ["req-comment-chat-C/event/world-turn"] (:event/ids decision)))
            (is (= :world-turn/comment-create (:world-turn/kind turn)))
            (is (= "Actually, pin this nuance." (:prompt/text turn)))
            (is (= ["WT-comment"] (world/read-turns-by-thread runtime "chat-C")))
            (is (= 1 (:turn-count thread)))
            (is (nil? (world/read-context-bundle-by-turn runtime "WT-comment")))))))))

(deftest world-only-turn-requires-thread-test
  (with-world-runtime
    (fn [runtime]
      (testing "world-only turns cannot invent their containing WorldThread"
        (let [request (world/world-only-turn-request
                        :world-turn/comment-create
                        "missing-chat"
                        {:request-id "req-comment-missing"
                         :time-ms 30
                         :payload {:world-turn/id "WT-missing"
                                   :prompt/text "No container."}})
              decision (append-and-await-decision! runtime request)]
          (is (= :rejected (:decision/status decision)))
          (is (= :world-thread/not-found (:decision/reason decision)))
          (is (nil? (world/read-turn runtime "WT-missing"))))))))

(deftest world-first-send-test
  (with-world-runtime
    (fn [runtime]
      (testing "the user send enters World first and World derives the LLM run request"
        (let [request (world/compose-and-send-request
                        "chat-world-first"
                        "Start from the world."
                        {:request-id "req-world-first"
                         :time-ms 40
                         :idempotency-key "idem-world-first"
                         :payload {:world-turn/id "WT-world-first"
                                   :context-bundle/id "B-world-first"
                                   :llm-turn-run/id "run-world-first"
                                   :llm-thread/id "llm-thread-world-first"
                                   :executor/task-id llm/pending-task-id}})
              decision (append-and-await-decision! runtime request)
              llm-request (world/await-materialized
                            #(world/read-llm-run-request runtime "run-world-first")
                            some?)
              llm-decision (llm/await-decision runtime "run-world-first")
              llm-run (llm/await-run runtime "run-world-first"
                                     #(= :pending (:status %)))]
          (is (= :accepted (:decision/status decision)))
          (is (= "WT-world-first" (:world-turn/id decision)))
          (is (= "B-world-first" (:context-bundle/id decision)))
          (is (= "run-world-first" (:llm-turn-run/id decision)))
          (is (= :llm/turn-run-request (:request/type llm-request)))
          (is (= "WT-world-first" (:world-turn/id llm-request)))
          (is (= "B-world-first" (:context-bundle/id llm-request)))
          (is (= "req-world-first/llm-request" (:request/id llm-request)))
          (is (= :accepted (:decision/status llm-decision)))
          (is (= "B-world-first" (:context-bundle/id llm-run)))
          (is (contains?
                (llm/await-materialized
                  #(llm/read-pending runtime llm/pending-task-id)
                  #(contains? % "run-world-first"))
                "run-world-first")))))))

(deftest context-bundle-before-run-test
  (with-world-runtime
    (fn [runtime]
      (testing "World materializes the bundle that the bridged LLM run points at"
        (let [request (world/compose-and-send-request
                        "chat-bundle-before-run"
                        "Freeze this before execution."
                        {:request-id "req-bundle-before-run"
                         :time-ms 50
                         :payload {:world-turn/id "WT-bundle-before-run"
                                   :context-bundle/id "B-bundle-before-run"
                                   :llm-turn-run/id "run-bundle-before-run"}})
              decision (append-and-await-decision! runtime request)
              bundle (world/await-materialized
                       #(world/read-context-bundle runtime "B-bundle-before-run")
                       some?)
              llm-run (llm/await-run runtime "run-bundle-before-run"
                                     #(= :pending (:status %)))]
          (is (= "B-bundle-before-run" (:context-bundle/id bundle)))
          (is (= (:context-bundle/hash bundle) (:context-bundle/hash decision)))
          (is (= (:context-bundle/id bundle) (:context-bundle/id llm-run)))
          (is (= "run-bundle-before-run"
                 (world/read-llm-run-by-turn runtime "WT-bundle-before-run"))))))))

(deftest one-bundle-per-run-test
  (with-world-runtime
    (fn [runtime]
      (testing "one compose-and-send creates exactly one bundle and one run"
        (let [request (world/compose-and-send-request
                        "chat-one-bundle"
                        "One run, one bundle."
                        {:request-id "req-one-bundle"
                         :time-ms 60
                         :payload {:world-turn/id "WT-one-bundle"
                                   :context-bundle/id "B-one-bundle"
                                   :llm-turn-run/id "run-one-bundle"}})
              decision (append-and-await-decision! runtime request)
              bundle-id (world/await-materialized
                          #(world/read-context-bundle-by-turn runtime "WT-one-bundle")
                          some?)
              run-id (world/await-materialized
                       #(world/read-llm-run-by-turn runtime "WT-one-bundle")
                       some?)
              llm-run (llm/await-run runtime "run-one-bundle"
                                     #(= :pending (:status %)))]
          (is (= "B-one-bundle" bundle-id))
          (is (= "run-one-bundle" run-id))
          (is (= "B-one-bundle" (:context-bundle/id llm-run)))
          (is (= "run-one-bundle" (:llm-turn-run/id decision)))
          (is (= ["WT-one-bundle"] (world/read-turns-by-thread runtime "chat-one-bundle"))))))))

(deftest accepted-decision-fields-test
  (with-world-runtime
    (fn [runtime]
      (testing "accepted compose decisions expose the sibling facts needed by projections"
        (let [request (world/compose-and-send-request
                        "chat-decision-fields"
                        "Expose the accepted fields."
                        {:request-id "req-decision-fields"
                         :time-ms 70
                         :payload {:world-turn/id "WT-decision-fields"
                                   :context-bundle/id "B-decision-fields"
                                   :llm-turn-run/id "run-decision-fields"
                                   :llm-thread/id "llm-thread-decision-fields"}})
              decision (append-and-await-decision! runtime request)]
          (is (= :accepted (:decision/status decision)))
          (is (= "req-decision-fields/event/world-turn" (:event/id decision)))
          (is (= ["req-decision-fields/event/world-thread"
                  "req-decision-fields/event/world-turn"
                  "req-decision-fields/event/context-bundle"
                  "req-decision-fields/event/llm-turn-run"]
                 (:event/ids decision)))
          (is (= "chat-decision-fields" (:world-thread/id decision)))
          (is (= "WT-decision-fields" (:world-turn/id decision)))
          (is (= "B-decision-fields" (:context-bundle/id decision)))
          (is (str/starts-with? (:context-bundle/hash decision) "sha256:"))
          (is (= "llm-thread-decision-fields" (:llm-thread/id decision)))
          (is (= "run-decision-fields" (:llm-turn-run/id decision)))
          (is (= "req-decision-fields/llm-request" (:llm/request-id decision))))))))

(deftest idempotency-test
  (with-world-runtime
    (fn [runtime]
      (testing "replaying the same send key does not mint duplicate turns, bundles, or runs"
        (let [request-a (world/compose-and-send-request
                          "chat-idempotent"
                          "Send once."
                          {:request-id "req-idem-A"
                           :time-ms 80
                           :idempotency-key "idem-same-send"
                           :payload {:world-turn/id "WT-idem-A"
                                     :context-bundle/id "B-idem-A"
                                     :llm-turn-run/id "run-idem-A"
                                     :llm-thread/id "llm-thread-idem"}})
              request-b (world/compose-and-send-request
                          "chat-idempotent"
                          "Send once, replayed."
                          {:request-id "req-idem-B"
                           :time-ms 81
                           :idempotency-key "idem-same-send"
                           :payload {:world-turn/id "WT-idem-B"
                                     :context-bundle/id "B-idem-B"
                                     :llm-turn-run/id "run-idem-B"
                                     :llm-thread/id "llm-thread-idem"}})
              decision-a (append-and-await-decision! runtime request-a)
              _ (llm/await-run runtime "run-idem-A" #(= :pending (:status %)))
              decision-b (append-and-await-decision! runtime request-b)]
          (is (= :accepted (:decision/status decision-a)))
          (is (= :accepted (:decision/status decision-b)))
          (is (:idempotency/replayed? decision-b))
          (is (= (:event/ids decision-a) (:event/ids decision-b)))
          (is (= "WT-idem-A" (:world-turn/id decision-b)))
          (is (= "B-idem-A" (:context-bundle/id decision-b)))
          (is (= "run-idem-A" (:llm-turn-run/id decision-b)))
          (is (= ["WT-idem-A"] (world/read-turns-by-thread runtime "chat-idempotent")))
          (is (= "B-idem-A" (world/read-context-bundle-by-turn runtime "WT-idem-A")))
          (is (nil? (world/read-context-bundle-by-turn runtime "WT-idem-B")))
          (is (some? (world/read-context-bundle runtime "B-idem-A")))
          (is (nil? (world/read-context-bundle runtime "B-idem-B")))
          (is (some? (world/read-llm-run-request runtime "run-idem-A")))
          (is (nil? (world/read-llm-run-request runtime "run-idem-B")))
          (is (some? (llm/read-run runtime "run-idem-A")))
          (is (nil? (llm/read-run runtime "run-idem-B")))
          (is (= "run-idem-A"
                 (:llm-turn-run/id
                  (world/read-send-by-idempotency runtime "idem-same-send")))))))))

(deftest approval-world-first-test
  (with-world-runtime
    (fn [runtime]
      (testing "approval resolution enters World and derives an LLM control record"
        (let [ids {:world-thread-id "chat-approval"
                   :world-turn-id "WT-approval-send"
                   :bundle-id "B-approval"
                   :run-id "run-approval-world-first"
                   :thread-id "llm-thread-approval"
                   :request-id "req-approval-send"}
              approval-id "approval-world-first"
              native-id 44
              _ (append-send-and-await-run! runtime ids)
              _ (append-approval-observation!
                  runtime
                  {:run-id (:run-id ids)
                   :thread-id (:thread-id ids)
                   :approval-id approval-id
                   :native-id native-id})
              control-request (world/world-only-turn-request
                                :world-turn/tool-approval-resolve
                                (:world-thread-id ids)
                                {:request-id "req-approval-resolve"
                                 :time-ms 110
                                 :payload {:world-turn/id "WT-approval-resolve"
                                           :llm-turn-run/id (:run-id ids)
                                           :approval/id approval-id
                                           :native/json-rpc-request-id native-id
                                           :decision :approved}})
              decision (append-and-await-decision! runtime control-request)
              run (llm/await-run runtime (:run-id ids) #(= :running (:status %)))
              approval (get (llm/read-approvals-by-run runtime (:run-id ids)) approval-id)
              control (llm/await-materialized
                        #(llm/read-control runtime "req-approval-resolve/llm-control")
                        some?)]
          (is (= :accepted (:decision/status decision)))
          (is (= :approval/resolve (:llm-control/type decision)))
          (is (= "WT-approval-resolve" (:world-turn/id decision)))
          (is (= "req-approval-resolve/llm-control"
                 (world/read-llm-control-by-turn runtime "WT-approval-resolve")))
          (is (= :approval/resolve (:control/type control)))
          (is (= native-id (:native/json-rpc-request-id control)))
          (is (= :running (:status run)))
          (is (= :approved (:status approval)))
          (is (nil? (llm/await-materialized
                      #(llm/read-pending-approval runtime approval-id)
                      nil?))))))))

(deftest approval-is-not-patch-acceptance-test
  (with-world-runtime
    (fn [runtime]
      (testing "tool approval resolution is separate from patch acceptance"
        (let [ids {:world-thread-id "chat-approval-not-patch"
                   :world-turn-id "WT-approval-not-patch-send"
                   :bundle-id "B-approval-not-patch"
                   :run-id "run-approval-not-patch"
                   :thread-id "llm-thread-approval-not-patch"
                   :request-id "req-approval-not-patch-send"}
              approval-id "approval-not-patch"
              _ (append-send-and-await-run! runtime ids)
              _ (append-approval-observation!
                  runtime
                  {:run-id (:run-id ids)
                   :thread-id (:thread-id ids)
                   :approval-id approval-id
                   :native-id 55})
              approval-request (world/world-only-turn-request
                                 :world-turn/tool-approval-resolve
                                 (:world-thread-id ids)
                                 {:request-id "req-approval-not-patch"
                                  :time-ms 120
                                  :payload {:world-turn/id "WT-approval-not-patch"
                                            :llm-turn-run/id (:run-id ids)
                                            :approval/id approval-id
                                            :native/json-rpc-request-id 55
                                            :decision :approved}})
              patch-request (world/world-only-turn-request
                              :world-turn/patch-accept
                              (:world-thread-id ids)
                              {:request-id "req-patch-accept-not-approval"
                               :time-ms 121
                               :payload {:world-turn/id "WT-patch-accept"
                                         :prompt/text "accept patch"}})
              approval-decision (append-and-await-decision! runtime approval-request)
              patch-decision (append-and-await-decision! runtime patch-request)]
          (is (= :approval/resolve (:llm-control/type approval-decision)))
          (is (= :world-turn/tool-approval-resolve
                 (:world-turn/kind (world/read-turn runtime "WT-approval-not-patch"))))
          (is (= :accepted (:decision/status patch-decision)))
          (is (= :world-turn/patch-accept
                 (:world-turn/kind (world/read-turn runtime "WT-patch-accept"))))
          (is (nil? (:llm-control/type patch-decision)))
          (is (nil? (world/read-llm-control-by-turn runtime "WT-patch-accept"))))))))

(deftest cancel-world-first-test
  (with-world-runtime
    (fn [runtime]
      (testing "cancel enters World first and derives an LLM cancel control"
        (let [ids {:world-thread-id "chat-cancel"
                   :world-turn-id "WT-cancel-send"
                   :bundle-id "B-cancel"
                   :run-id "run-cancel-world-first"
                   :thread-id "llm-thread-cancel"
                   :request-id "req-cancel-send"}
              _ (append-send-and-await-run! runtime ids)
              request (world/world-only-turn-request
                        :world-turn/cancel
                        (:world-thread-id ids)
                        {:request-id "req-cancel"
                         :time-ms 130
                         :payload {:world-turn/id "WT-cancel"
                                   :llm-turn-run/id (:run-id ids)
                                   :reason :user-request}})
              decision (append-and-await-decision! runtime request)
              run (llm/await-run runtime (:run-id ids) #(= :cancelled (:status %)))
              control (llm/await-materialized
                        #(llm/read-control runtime "req-cancel/llm-control")
                        some?)]
          (is (= :accepted (:decision/status decision)))
          (is (= :turn/cancel (:llm-control/type decision)))
          (is (= :turn/cancel (:control/type control)))
          (is (= :cancelled (:status run)))
          (is (nil? (llm/await-materialized
                      #(get (llm/read-pending runtime llm/pending-task-id)
                            (:run-id ids))
                      nil?))))))))

(deftest compaction-world-first-test
  (with-world-runtime
    (fn [runtime]
      (testing "compaction request enters World first and records an LLM control"
        (let [ids {:world-thread-id "chat-compact"
                   :world-turn-id "WT-compact-send"
                   :bundle-id "B-compact"
                   :run-id "run-compact-world-first"
                   :thread-id "llm-thread-compact"
                   :request-id "req-compact-send"}
              _ (append-send-and-await-run! runtime ids)
              request (world/world-only-turn-request
                        :world-turn/compact-request
                        (:world-thread-id ids)
                        {:request-id "req-compact"
                         :time-ms 140
                         :payload {:world-turn/id "WT-compact"
                                   :llm-turn-run/id (:run-id ids)
                                   :strategy :summarize-prefix}})
              decision (append-and-await-decision! runtime request)
              control (llm/await-materialized
                        #(llm/read-control runtime "req-compact/llm-control")
                        some?)
              run (llm/read-run runtime (:run-id ids))]
          (is (= :accepted (:decision/status decision)))
          (is (= :compact/request (:llm-control/type decision)))
          (is (= :compact/request (:control/type control)))
          (is (= :pending (:status run)))
          (is (= [{:control/id "req-compact/llm-control"
                   :time-ms 140
                   :actor {:actor/id "system" :actor/type :system}
                   :payload {:world-thread/id "chat-compact"
                             :world-turn/id "WT-compact"
                             :llm-turn-run/id "run-compact-world-first"
                             :strategy :summarize-prefix}}]
                 (:compactions run))))))))

(deftest approval-timeout-test
  (with-world-runtime
    (fn [runtime]
      (testing "approval timeout is durably recorded as an expired approval"
        (let [ids {:world-thread-id "chat-timeout"
                   :world-turn-id "WT-timeout-send"
                   :bundle-id "B-timeout"
                   :run-id "run-approval-timeout"
                   :thread-id "llm-thread-timeout"
                   :request-id "req-timeout-send"}
              approval-id "approval-timeout"
              _ (append-send-and-await-run! runtime ids)
              _ (append-approval-observation!
                  runtime
                  {:run-id (:run-id ids)
                   :thread-id (:thread-id ids)
                   :approval-id approval-id
                   :native-id 66})
              request (world/world-only-turn-request
                        :world-turn/tool-approval-resolve
                        (:world-thread-id ids)
                        {:request-id "req-approval-timeout"
                         :time-ms 150
                         :payload {:world-turn/id "WT-approval-timeout"
                                   :llm-turn-run/id (:run-id ids)
                                   :approval/id approval-id
                                   :native/json-rpc-request-id 66
                                   :decision :expired
                                   :reason :timeout}})
              decision (append-and-await-decision! runtime request)
              run (llm/await-run runtime (:run-id ids) #(= :failed (:status %)))
              approval (get (llm/read-approvals-by-run runtime (:run-id ids)) approval-id)]
          (is (= :accepted (:decision/status decision)))
          (is (= :approval/resolve (:llm-control/type decision)))
          (is (= :expired (:status approval)))
          (is (= :expired (:decision approval)))
          (is (nil? (llm/await-materialized
                      #(llm/read-pending-approval runtime approval-id)
                      nil?)))
          (is (= :approval/declined (get-in run [:error :reason])))
          (is (= :expired (get-in run [:error :decision]))))))))
