(ns app.server.rama.dogfood-transcript-test
  (:require [app.server.rama.dogfood.transcript :as transcript]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]))

(defn with-transcript-runtime
  [f]
  (let [runtime (transcript/start-transcript-runtime!)]
    (try
      (f runtime)
      (finally
        (transcript/close-transcript-runtime! runtime)))))

(defn temp-dir
  []
  (doto (java.nio.file.Files/createTempDirectory "softland-transcript-test"
                                                 (make-array java.nio.file.attribute.FileAttribute 0))
    (.toFile)))

(defn write-lines!
  [file lines]
  (spit file (apply str (map #(str % "\n") lines))))

(defn append-text!
  [file text]
  (spit file text :append true))

(defn valid-line
  [conversation-id message-id & [content]]
  (str "{\"type\":\"assistant\",\"session_id\":\"" conversation-id
       "\",\"uuid\":\"" message-id
       "\",\"timestamp\":\"2026-05-11T00:00:00Z\",\"message\":{\"content\":"
       (or content "[]")
       "}}"))

(deftest transcript-harvest-contract-test
  (with-transcript-runtime
    (fn [runtime]
      (testing "harvest records source-general redacted transcript observations idempotently"
        (let [dir (.toFile (temp-dir))
              file (io/file dir "claude.jsonl")
              tool-content "[{\"type\":\"tool_use\",\"id\":\"tool-1\",\"name\":\"bash\",\"input\":{\"api_key\":\"never-store-me\"}}]"
              _ (write-lines!
                  file
                  [(valid-line "conv-1" "msg-1" tool-content)
                   "{not-json"
                   (valid-line "conv-1" "msg-2")])
              request (transcript/transcript-request
                        :transcript/harvest
                        {:transcript/request-id "harvest-1"
                         :transcript/source :claude-code
                         :transcript/paths [(.getPath dir)]})
              result (transcript/harvest-transcripts! runtime request)
              run (transcript/read-run runtime "harvest-1")
              conversation (transcript/read-conversation runtime "conv-1")
              tool-call (transcript/read-tool-call runtime "tool-1")]
          (is (= :complete (:status run)))
          (is (= 3 (:observations-appended result)))
          (is (= 3 (:observed-line-count run)))
          (is (= 1 (:parse-error-count run)))
          (is (= 2 (count conversation)))
          (is (= "bash" (:tool-call/name tool-call)))
          (is (not (clojure.string/includes? (pr-str conversation) "never-store-me")))
          (is (not (clojure.string/includes? (pr-str tool-call) "never-store-me")))

          (let [request-2 (assoc request :transcript/request-id "harvest-2"
                                         :routing/key (transcript/transcript-routing-key "harvest-2"))]
            (transcript/harvest-transcripts! runtime request-2)
            (is (= 2 (count (transcript/read-conversation runtime "conv-1"))))))))))

(deftest transcript-builder-boundary-test
  (with-transcript-runtime
    (fn [runtime]
      (testing "transcript builders parse files but only depot appends materialize PStates"
        (let [dir (.toFile (temp-dir))
              file (io/file dir "boundary.jsonl")
              _ (write-lines! file [(valid-line "boundary-conv" "msg-1")])
              request (transcript/transcript-request
                        :transcript/harvest
                        {:transcript/request-id "boundary-harvest"
                         :transcript/source :claude-code
                         :transcript/paths [(.getPath dir)]})
              observations (transcript/read-jsonl-observations request file 0)]
          (is (nil? (transcript/read-run runtime "boundary-harvest")))
          (is (empty? (transcript/read-conversation runtime "boundary-conv")))
          (is (= 1 (count observations)))
          (transcript/append-transcript-request! runtime request)
          (transcript/await-materialized
            #(transcript/read-run runtime "boundary-harvest")
            #(= :pending (:status %)))
          (is (= :pending (:status (transcript/read-run runtime "boundary-harvest"))))
          (transcript/append-transcript-observation! runtime (first observations))
          (transcript/await-materialized
            #(transcript/read-conversation runtime "boundary-conv")
            #(= 1 (count %)))
          (is (= 1 (count (transcript/read-conversation runtime "boundary-conv")))))))))

(deftest transcript-watch-offset-contract-test
  (with-transcript-runtime
    (fn [runtime]
      (testing "watch resumes known files from ledger offsets and waits for complete lines"
        (let [dir (.toFile (temp-dir))
              file (io/file dir "watch.jsonl")
              _ (write-lines! file [(valid-line "watch-conv" "msg-1")])
              harvest-request (transcript/transcript-request
                                :transcript/harvest
                                {:transcript/request-id "watch-harvest"
                                 :transcript/source :claude-code
                                 :transcript/paths [(.getPath dir)]})
              _ (transcript/harvest-transcripts! runtime harvest-request)
              watch-request (transcript/transcript-request
                              :transcript/watch
                              {:transcript/request-id "watch-1"
                               :transcript/source :claude-code
                               :transcript/paths [(.getPath dir)]})
              watch (transcript/start-transcript-watch! runtime watch-request {:poll-ms 10000})]
          ((:poll-once! watch))
          (is (= 1 (count (transcript/read-conversation runtime "watch-conv"))))
          (append-text! file (subs (valid-line "watch-conv" "msg-2") 0 20))
          ((:poll-once! watch))
          (is (= 1 (count (transcript/read-conversation runtime "watch-conv"))))
          (append-text! file (str (subs (valid-line "watch-conv" "msg-2") 20) "\n"))
          ((:poll-once! watch))
          (transcript/await-materialized
            #(transcript/read-conversation runtime "watch-conv")
            #(= 2 (count %)))
          (is (= 2 (count (transcript/read-conversation runtime "watch-conv"))))
          ((:stop! watch))
          (is (= :cancelled (:status (transcript/await-materialized
                                       #(transcript/read-run runtime "watch-1")
                                       #(= :cancelled (:status %))))))))))

  (with-transcript-runtime
    (fn [runtime]
      (testing "watch starts existing unknown files at EOF and new files at byte zero"
        (let [dir (.toFile (temp-dir))
              existing (io/file dir "existing.jsonl")
              _ (write-lines! existing [(valid-line "existing-conv" "old")])
              watch-request (transcript/transcript-request
                              :transcript/watch
                              {:transcript/request-id "watch-unknown"
                               :transcript/source :claude-code
                               :transcript/paths [(.getPath dir)]})
              watch (transcript/start-transcript-watch! runtime watch-request {:poll-ms 10000})]
          ((:poll-once! watch))
          (is (empty? (transcript/read-conversation runtime "existing-conv")))
          (append-text! existing (valid-line "existing-conv" "new"))
          (append-text! existing "\n")
          ((:poll-once! watch))
          (transcript/await-materialized
            #(transcript/read-conversation runtime "existing-conv")
            #(= 1 (count %)))
          (let [new-file (io/file dir "new.jsonl")]
            (write-lines! new-file [(valid-line "new-conv" "msg-1")])
            ((:poll-once! watch))
            (transcript/await-materialized
              #(transcript/read-conversation runtime "new-conv")
              #(= 1 (count %)))
            (is (= 1 (count (transcript/read-conversation runtime "new-conv")))))
          ((:stop! watch)))))))
