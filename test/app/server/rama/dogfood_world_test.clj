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
          (is (contains? (llm/read-pending runtime llm/pending-task-id)
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
              bundle-id (world/read-context-bundle-by-turn runtime "WT-one-bundle")
              run-id (world/read-llm-run-by-turn runtime "WT-one-bundle")
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
