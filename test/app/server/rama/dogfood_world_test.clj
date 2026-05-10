(ns app.server.rama.dogfood-world-test
  (:require [app.server.rama.dogfood.world :as world]
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
                  "req-send-chat-B/event/context-bundle"]
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
