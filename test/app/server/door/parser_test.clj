(ns app.server.door.parser-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.server.door.server-jetty :as sut]
            [clojure.java.io :as io]))

(deftest parse-stream-json-line-test
  (let [lines (line-seq (io/reader "test/fixtures/claude-stream-sample.jsonl"))
        events (sut/parse-stream-json-lines lines)
        kinds  (map :kind events)
        terminals (filter #{:run-done :run-error} kinds)]

    (testing "Parses canonical event kinds from fixture"
      (is (some #{:run-start} kinds))
      (is (some #{:text-delta} kinds))
      (is (some #{:tool-use-start} kinds))
      (is (some #{:tool-input-delta} kinds))
      (is (some #{:block-stop} kinds))
      (is (some #{:tool-result} kinds))
      (is (some #{:run-done} kinds)))

    (testing "Envelope invariants: each emitted event has :kind, :event, :ts"
      (is (every? :kind events))
      (is (every? :event events))
      (is (every? :ts events))
      (is (every? (fn [e] (= (:kind e) (:event e))) events)))

    (testing "tool-input-delta and tool-result reference existing tool-id"
      (let [tool-ids (set (map :tool-id (filter #(= :tool-use-start (:kind %)) events)))]
        (is (every? (fn [e]
                      (contains? tool-ids (:tool-id e)))
                    (filter #(= :tool-input-delta (:kind %)) events)))
        (is (every? (fn [e]
                      (contains? tool-ids (:tool-id e)))
                    (filter #(= :tool-result (:kind %)) events)))))

    (testing "Exactly one terminal event exists"
      (is (= 1 (count terminals))))))

(deftest parse-stream-json-line-malformed-line-test
  (let [events (sut/parse-stream-json-lines ["{not-json"])]
    (is (= :run-error (:kind (last events))))
    (is (= :invalid-json-line (:error (last events))))))

(deftest parse-stream-json-line-unknown-tool-reference-test
  (let [lines ["{\"type\":\"stream_event\",\"event\":{\"type\":\"content_block_delta\",\"index\":1,\"delta\":{\"type\":\"input_json_delta\",\"partial_json\":\"{}\"}}}"
               "{\"type\":\"result\",\"session_id\":\"sesh_123\",\"total_cost_usd\":0.0001}"]
        events (sut/parse-stream-json-lines lines)
        terminal (last events)]
    (is (= :run-error (:kind terminal)))
    (is (= :unknown-tool-reference (:error terminal)))))
