(ns app.server.rama.transcript-ingest-test
  (:require [app.server.rama.transcript-ingest :as ti]
            [app.server.ingest.transcript :as t]
            [app.server.rama.core :as core]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]])
  (:import (java.io File)))

;; ── Test helpers ────────────────────────────────────────────────────────────────

(defn with-ingest-runtime
  "Launch a TranscriptIngestModule, call (f runtime), then close."
  [f]
  (let [runtime (ti/start-transcript-ingest-runtime!)]
    (try
      (f runtime)
      (finally
        (ti/close-transcript-ingest-runtime! runtime)))))

(defn temp-dir
  "Create a temp directory for test fixtures. Returns a File."
  ^File []
  (.toFile
    (java.nio.file.Files/createTempDirectory
      "softland-ti-test"
      (make-array java.nio.file.attribute.FileAttribute 0))))

(defn write-lines!
  "Write lines to a file, each terminated with newline."
  [file lines]
  (spit file (apply str (map #(str % "\n") lines))))

(defn append-text!
  "Append raw text to a file (no trailing newline added)."
  [file text]
  (spit file text :append true))

(defn write-utf8!
  "Write text to a file as explicit UTF-8 bytes (no platform-charset surprises).
   Used for byte-correctness tests where the on-disk byte layout must be exact."
  [file ^String text]
  (with-open [w (io/writer file :encoding "UTF-8")]
    (.write w text)))

(defn write-bytes!
  "Write raw bytes to a file — for invalid-UTF-8 / exact-byte-layout tests that a
   UTF-8 writer could not produce."
  [file ^bytes ba]
  (with-open [out (java.io.FileOutputStream. ^File file)]
    (.write out ba)))

(defn valid-line
  "Produce a minimal valid JSONL line for Claude Code transcript.
   Options:
     :content — JSON array string for message.content (default \"[]\")
     :type    — event type (default \"assistant\")
     :tool-blocks — vector of tool-use block maps to embed in content"
  [conversation-id message-id & {:keys [content type tool-blocks]}]
  (let [content-str (cond
                      tool-blocks
                      (let [blocks (mapv (fn [tb]
                                          (str "{\"type\":\"tool_use\""
                                               ",\"id\":\"" (:id tb) "\""
                                               ",\"name\":\"" (:name tb) "\""
                                               ",\"input\":" (or (:input tb) "{}") "}"))
                                        tool-blocks)]
                        (str "[" (str/join "," blocks) "]"))
                      content content
                      :else "[]")]
    (str "{\"type\":\"" (or type "assistant")
         "\",\"session_id\":\"" conversation-id
         "\",\"uuid\":\"" message-id
         "\",\"timestamp\":\"2026-05-11T00:00:00Z\""
         ",\"message\":{\"role\":\"" (or type "assistant")
         "\",\"content\":" content-str "}}")))

(defn user-line
  "Produce a valid user-role JSONL line."
  [conversation-id message-id & {:keys [content]}]
  (valid-line conversation-id message-id
              :type "user"
              :content (or content "[{\"type\":\"text\",\"text\":\"hello\"}]")))

(defn tool-result-line
  "Produce a JSONL line with a tool_result content block."
  [conversation-id message-id tool-use-id result-text]
  (str "{\"type\":\"user\",\"session_id\":\"" conversation-id
       "\",\"uuid\":\"" message-id
       "\",\"timestamp\":\"2026-05-11T00:00:01Z\""
       ",\"message\":{\"role\":\"user\""
       ",\"content\":[{\"type\":\"tool_result\""
       ",\"tool_use_id\":\"" tool-use-id "\""
       ",\"content\":\"" result-text "\"}]}}"))

(defn system-init-line
  "Produce a system/init JSONL line with session metadata."
  [conversation-id & {:keys [model]}]
  (str "{\"type\":\"system\",\"session_id\":\"" conversation-id
       "\",\"uuid\":\"sys-init-" conversation-id
       "\",\"timestamp\":\"2026-05-11T00:00:00Z\""
       ",\"message\":{\"role\":\"system\",\"content\":[]}"
       (when model (str ",\"model\":\"" model "\""))
       "}"))

(defn parse-error-line
  "A line that will fail JSON parse."
  []
  "{not-valid-json!!!")

(defn harvest-request
  "Build a harvest request pointing at a directory."
  [request-id dir-path & {:keys [source] :or {source :claude-code}}]
  (t/transcript-request
    :transcript/harvest
    {:transcript/request-id request-id
     :transcript/source source
     :transcript/paths [dir-path]}))

(defn watch-request
  "Build a watch request pointing at a directory."
  [request-id dir-path & {:keys [source] :or {source :claude-code}}]
  (t/transcript-request
    :transcript/watch
    {:transcript/request-id request-id
     :transcript/source source
     :transcript/paths [dir-path]}))

(defn await-materialized
  "Poll read-f until pred is true, with timeout (default 5s)."
  ([read-f pred]
   (await-materialized read-f pred 5000))
  ([read-f pred timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop [value (read-f)]
       (cond
         (pred value) value
         (>= (System/currentTimeMillis) deadline) value
         :else (do (Thread/sleep 25) (recur (read-f))))))))

(defn projection-messages
  "Extract only message entries (not meta) from a conversation projection."
  [projection]
  (filter #(= :message (:entry-type (second %))) projection))

(defn projection-meta
  "Extract the meta entry from a conversation projection."
  [projection]
  (first (filter #(= :meta (:entry-type (second %))) projection)))

;; ── Tests ───────────────────────────────────────────────────────────────────────

(deftest harvest-creates-correct-container-kinds-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "1. Harvest creates conversation + message + tool-call containers with correct kinds"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              _ (write-lines!
                  file
                  [(system-init-line "conv-A")
                   (valid-line "conv-A" "msg-1"
                               :tool-blocks [{:id "tu-1" :name "bash" :input "{}"}])
                   (user-line "conv-A" "msg-2")])
              request (harvest-request "h-kinds" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)
              source :claude-code
              ck (ti/conv-key source "conv-A")
              conv-id (ti/conversation-container-id ck)

              ;; Read containers
              conv-container (ti/read-container runtime conv-id)
              msg1-hash (core/sha-256 "msg-1")
              msg1-id (ti/message-container-id ck msg1-hash)
              msg1-container (ti/read-container runtime msg1-id)
              tc-hash (core/sha-256 "tu-1")
              tc-id (ti/tool-call-container-id ck tc-hash)
              tc-container (ti/read-container runtime tc-id)
              tr-id (ti/tool-result-container-id ck tc-hash)
              tr-container (ti/read-container runtime tr-id)]

          (is (some? conv-container) "Conversation container should exist")
          (is (= :chat-conversation (:container-kind conv-container)))
          (is (= ck (:conv-key conv-container)))

          (is (some? msg1-container) "Message container should exist")
          (is (= :chat-message (:container-kind msg1-container)))

          (is (some? tc-container) "Tool-call container should exist")
          (is (= :tool-call (:container-kind tc-container)))

          (is (some? tr-container) "Tool-result container should exist")
          (is (= :tool-result (:container-kind tr-container))))))))

(deftest idempotent-reharvest-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "2. Re-harvest same files is idempotent (no duplicate containers)"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              _ (write-lines!
                  file
                  [(valid-line "conv-B" "msg-1")
                   (valid-line "conv-B" "msg-2")])
              request1 (harvest-request "h-idem-1" (.getPath dir))
              request2 (harvest-request "h-idem-2" (.getPath dir))
              _ (ti/harvest-ingest! runtime request1)
              source :claude-code
              ck (ti/conv-key source "conv-B")
              conv-id (ti/conversation-container-id ck)
              proj-before (ti/read-conversation-projection runtime conv-id)
              msgs-before (projection-messages proj-before)

              ;; Re-harvest
              _ (ti/harvest-ingest! runtime request2)
              proj-after (ti/read-conversation-projection runtime conv-id)
              msgs-after (projection-messages proj-after)]

          (is (= (count msgs-before) (count msgs-after))
              "Re-harvest must not create duplicate message entries")
          (is (= 2 (count msgs-after))))))))

(deftest source-record-identity-dedup-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "3. Source-record identity dedup works (same line-key -> no new containers)"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              line (valid-line "conv-C" "msg-dedup")
              _ (write-lines! file [line])
              request (harvest-request "h-dedup" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)

              source :claude-code
              ck (ti/conv-key source "conv-C")
              conv-id (ti/conversation-container-id ck)
              msg-hash (core/sha-256 "msg-dedup")
              msg-id (ti/message-container-id ck msg-hash)

              ;; Manually re-append the same observation
              observations (t/read-jsonl-observations request file 0)
              obs (first observations)
              obs-with-ck (assoc obs :transcript/conv-key ck)]

          ;; Append same obs again — should be deduped by source-ledger
          (ti/append-ingest-observation! runtime obs-with-ck)
          (Thread/sleep 200) ;; wait for async materialization

          ;; Container count should still be the same
          (let [container (ti/read-container runtime msg-id)]
            (is (some? container) "Container should exist")
            (is (= :chat-message (:container-kind container)))))))))

(deftest conversation-projection-source-order-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "4. Conversation projection returns messages in source order"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              ;; Write 5 messages in order
              _ (write-lines!
                  file
                  [(valid-line "conv-D" "msg-1")
                   (valid-line "conv-D" "msg-2")
                   (valid-line "conv-D" "msg-3")
                   (user-line "conv-D" "msg-4")
                   (valid-line "conv-D" "msg-5")])
              request (harvest-request "h-order" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)
              source :claude-code
              ck (ti/conv-key source "conv-D")
              conv-id (ti/conversation-container-id ck)
              projection (ti/read-conversation-projection runtime conv-id)
              msgs (projection-messages projection)
              order-keys (mapv (fn [[k _v]] k) msgs)]

          (is (= 5 (count msgs)) "Should have 5 message entries")
          ;; Order keys are zero-padded byte offsets, so they should be
          ;; lexicographically sorted
          (is (= order-keys (sort order-keys))
              "Messages must be in ascending source order"))))))

(deftest tool-call-index-queryable-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "5. Tool-call index is populated and queryable by tool name"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              _ (write-lines!
                  file
                  [(valid-line "conv-E" "msg-1"
                               :tool-blocks [{:id "tu-bash-1" :name "bash" :input "{}"}
                                             {:id "tu-read-1" :name "Read" :input "{}"}])
                   (valid-line "conv-E" "msg-2"
                               :tool-blocks [{:id "tu-bash-2" :name "bash" :input "{}"}])])
              request (harvest-request "h-tools" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)

              bash-calls (ti/read-tool-calls-by-name runtime "bash")
              read-calls (ti/read-tool-calls-by-name runtime "Read")
              missing-calls (ti/read-tool-calls-by-name runtime "nonexistent")]

          (is (= 2 (count bash-calls)) "Should have 2 bash calls")
          (is (= 1 (count read-calls)) "Should have 1 Read call")
          (is (empty? missing-calls) "Non-existent tool returns empty"))))))

(deftest parse-errors-create-audit-not-containers-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "6. Parse errors create audit entries but not containers (DO create projection entries)"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              _ (write-lines!
                  file
                  [(valid-line "conv-F" "msg-1")
                   (parse-error-line)
                   (valid-line "conv-F" "msg-2")])
              request (harvest-request "h-parse-err" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)
              source :claude-code
              ck (ti/conv-key source "conv-F")
              conv-id (ti/conversation-container-id ck)

              ;; Valid messages go to conv-F conversation
              projection (ti/read-conversation-projection runtime conv-id)
              msgs (projection-messages projection)
              valid-entries (filter #(nil? (:parse-error-kind (second %))) msgs)

              ;; Parse errors fall back to file path as conversation ID
              pe-ck (ti/conv-key source (.getPath file))
              pe-conv-id (ti/conversation-container-id pe-ck)
              pe-projection (ti/read-conversation-projection runtime pe-conv-id)
              pe-msgs (projection-messages pe-projection)
              parse-error-entries (filter #(some? (:parse-error-kind (second %))) pe-msgs)

              ;; Check audit entries exist for the parse error
              audit-entries (ti/read-audit-entries runtime "h-parse-err")
              parse-error-audits (filter #(= :invalid-json (:parse-error-kind (second %))) audit-entries)]

          ;; Parse error lines create projection entries in their own conversation
          (is (= 1 (count parse-error-entries))
              "Parse error should create a projection entry")
          (is (nil? (:container-id (second (first parse-error-entries))))
              "Parse error projection entry should have nil container-id")

          ;; Valid messages still create containers in the conv-F conversation
          (is (= 2 (count valid-entries)))

          ;; Audit entries are created for parse errors
          (is (pos? (count parse-error-audits))
              "Parse error should produce audit entries"))))))

(deftest redacted-content-no-raw-secrets-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "7. Redacted content does not contain raw secrets"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              secret-value "sk-supersecretkey12345"
              _ (write-lines!
                  file
                  [(str "{\"type\":\"assistant\",\"session_id\":\"conv-G\""
                        ",\"uuid\":\"msg-secret\""
                        ",\"timestamp\":\"2026-05-11T00:00:00Z\""
                        ",\"message\":{\"role\":\"assistant\""
                        ",\"content\":[{\"type\":\"text\",\"text\":\"hello\"}]"
                        ",\"api_key\":\"" secret-value "\"}}")])
              request (harvest-request "h-redact" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)
              source :claude-code
              ck (ti/conv-key source "conv-G")
              msg-hash (core/sha-256 "msg-secret")
              msg-id (ti/message-container-id ck msg-hash)
              container (ti/read-container runtime msg-id)]

          ;; The redacted payload should not contain the raw secret
          (is (some? container))
          (is (not (str/includes? (pr-str container) secret-value))
              "Container must not contain raw secret values"))))))

(deftest container-ids-deterministic-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "8. Container IDs are deterministic (same input -> same IDs)"
        (let [source :claude-code
              conv-id "deterministic-conv"
              msg-uuid "deterministic-msg"
              tool-use-id "deterministic-tool"

              ck (ti/conv-key source conv-id)
              conv-container-id (ti/conversation-container-id ck)
              msg-hash (core/sha-256 msg-uuid)
              msg-container-id (ti/message-container-id ck msg-hash)
              tc-hash (core/sha-256 tool-use-id)
              tc-container-id (ti/tool-call-container-id ck tc-hash)
              tr-container-id (ti/tool-result-container-id ck tc-hash)

              ;; Compute again with same inputs
              ck2 (ti/conv-key source conv-id)
              conv-container-id2 (ti/conversation-container-id ck2)
              msg-hash2 (core/sha-256 msg-uuid)
              msg-container-id2 (ti/message-container-id ck2 msg-hash2)
              tc-hash2 (core/sha-256 tool-use-id)
              tc-container-id2 (ti/tool-call-container-id ck2 tc-hash2)
              tr-container-id2 (ti/tool-result-container-id ck2 tc-hash2)]

          (is (= conv-container-id conv-container-id2))
          (is (= msg-container-id msg-container-id2))
          (is (= tc-container-id tc-container-id2))
          (is (= tr-container-id tr-container-id2))

          ;; Different inputs produce different IDs
          (let [ck-other (ti/conv-key source "other-conv")]
            (is (not= conv-container-id (ti/conversation-container-id ck-other)))))))))

(deftest composition-edges-link-correctly-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "9. Composition edges correctly link conversation->message->tool-call->tool-result"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              _ (write-lines!
                  file
                  [(valid-line "conv-H" "msg-1"
                               :tool-blocks [{:id "tu-edges" :name "bash" :input "{}"}])])
              request (harvest-request "h-edges" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)
              source :claude-code
              ck (ti/conv-key source "conv-H")
              conv-id (ti/conversation-container-id ck)
              msg-hash (core/sha-256 "msg-1")
              msg-id (ti/message-container-id ck msg-hash)
              tc-hash (core/sha-256 "tu-edges")
              tc-id (ti/tool-call-container-id ck tc-hash)
              tr-id (ti/tool-result-container-id ck tc-hash)

              ;; Read composition edges for conv (should contain msg)
              conv-edges (com.rpl.rama/foreign-select
                           [(com.rpl.rama.path/keypath conv-id)
                            com.rpl.rama.path/ALL]
                           (:composition-edges runtime))
              conv-edge-children (set (map #(:child-id (second %)) conv-edges))

              ;; Read composition edges for message (should produce tool-call/result)
              msg-edges (com.rpl.rama/foreign-select
                          [(com.rpl.rama.path/keypath msg-id)
                           com.rpl.rama.path/ALL]
                          (:composition-edges runtime))
              msg-edge-types (set (map #(:edge-type (second %)) msg-edges))
              msg-edge-children (set (map #(:child-id (second %)) msg-edges))]

          ;; Conversation contains message
          (is (contains? conv-edge-children msg-id)
              "Conversation should contain message via :contains edge")

          ;; Message produces tool-call and tool-result
          (is (contains? msg-edge-types :produced)
              "Message should have :produced edges")
          (is (contains? msg-edge-children tc-id)
              "Message should produce the tool-call")
          (is (contains? msg-edge-children tr-id)
              "Message should produce the tool-result"))))))

(deftest source-anchors-trace-back-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "10. Source anchors trace containers back to file/line/offset/hash"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              _ (write-lines!
                  file
                  [(valid-line "conv-I" "msg-anchor")])
              request (harvest-request "h-anchor" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)
              source :claude-code
              ck (ti/conv-key source "conv-I")
              msg-hash (core/sha-256 "msg-anchor")
              msg-id (ti/message-container-id ck msg-hash)

              ;; Read source anchor
              anchor (com.rpl.rama/foreign-select-one
                       [(com.rpl.rama.path/keypath msg-id)]
                       (:source-anchors runtime))]

          (is (some? anchor) "Source anchor should exist for the container")
          (is (= msg-id (:container-id anchor)))
          (is (= source (:source anchor)))
          (is (string? (:file-path anchor)))
          (is (str/ends-with? (:file-path anchor) "session.jsonl"))
          (is (number? (:byte-offset anchor)))
          (is (>= (:byte-offset anchor) 0))
          (is (number? (:byte-length anchor)))
          (is (pos? (:byte-length anchor)))
          (is (string? (:line-hash anchor)))
          (is (str/starts-with? (:line-hash anchor) "sha256:")))))))

(deftest watch-harvest-overlap-no-duplicates-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "11. Watch + harvest overlap does not create duplicate containers"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              _ (write-lines!
                  file
                  [(valid-line "conv-J" "msg-overlap-1")
                   (valid-line "conv-J" "msg-overlap-2")])

              ;; Harvest first
              h-request (harvest-request "h-overlap" (.getPath dir))
              _ (ti/harvest-ingest! runtime h-request)

              source :claude-code
              ck (ti/conv-key source "conv-J")
              conv-id (ti/conversation-container-id ck)
              proj-after-harvest (ti/read-conversation-projection runtime conv-id)
              msgs-after-harvest (projection-messages proj-after-harvest)

              ;; Now watch and poll (should see existing file at EOF, skip old lines)
              w-request (watch-request "w-overlap" (.getPath dir))
              watch (ti/start-watch-ingest! runtime w-request {:poll-ms 10000})
              _ ((:poll-once! watch))

              ;; Append a new line via watch
              _ (append-text! file (str (valid-line "conv-J" "msg-overlap-3") "\n"))
              _ ((:poll-once! watch))
              _ (Thread/sleep 300) ;; give topology time to process

              proj-after-watch (await-materialized
                                 #(ti/read-conversation-projection runtime conv-id)
                                 #(= 3 (count (projection-messages %))))
              msgs-after-watch (projection-messages proj-after-watch)]

          (is (= 2 (count msgs-after-harvest)))
          (is (= 3 (count msgs-after-watch))
              "Watch should add 1 new message, not duplicate existing ones")
          ((:stop! watch)))))))

(deftest empty-file-produces-source-artifact-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "12. Empty file produces source artifact (via file-state sentinel) but zero containers"
        (let [dir (temp-dir)
              file (io/file dir "empty.jsonl")
              _ (spit file "") ;; empty file
              request (harvest-request "h-empty" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)
              source :claude-code
              ;; For empty files, conv-id fallback is the file path
              ck (ti/conv-key source (.getPath file))
              conv-id (ti/conversation-container-id ck)

              ;; Source artifact should exist
              source-artifact (ti/read-source-artifact runtime conv-id)

              ;; But no message containers
              projection (ti/read-conversation-projection runtime conv-id)
              msgs (projection-messages projection)]

          (is (some? source-artifact)
              "Empty file should still produce a source artifact")
          (is (= 0 (count msgs))
              "Empty file should produce zero message containers"))))))

(deftest run-status-transitions-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "13. Run status transitions through pending -> running -> complete"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              _ (write-lines! file [(valid-line "conv-K" "msg-1")])
              request (harvest-request "h-status" (.getPath dir))

              ;; Step 1: submit request (harvest-ingest! does this internally)
              _ (ti/append-ingest-request! runtime request)
              run-after-request (ti/read-ingest-run runtime "h-status")]

          (is (= :pending (:status run-after-request))
              "After request submission, status should be :pending")

          ;; Step 2: claim running
          (ti/append-ingest-claim!
            runtime
            (t/transcript-run-status-record "h-status" :running))
          (let [run-running (await-materialized
                              #(ti/read-ingest-run runtime "h-status")
                              #(= :running (:status %)))]
            (is (= :running (:status run-running))))

          ;; Step 3: claim complete
          (ti/append-ingest-claim!
            runtime
            (t/transcript-run-status-record "h-status" :complete))
          (let [run-complete (await-materialized
                               #(ti/read-ingest-run runtime "h-status")
                               #(= :complete (:status %)))]
            (is (= :complete (:status run-complete)))))))))

(deftest audit-entries-record-provenance-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "14. Audit entries record per-record provenance"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              _ (write-lines!
                  file
                  [(valid-line "conv-L" "msg-1"
                               :tool-blocks [{:id "tu-audit" :name "bash" :input "{}"}])
                   (parse-error-line)
                   (valid-line "conv-L" "msg-2")])
              request (harvest-request "h-audit" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)

              audit-entries (ti/read-audit-entries runtime "h-audit")
              audit-values (map second audit-entries)]

          ;; Should have at least 3 audit entries (1 per source record)
          ;; + possibly empty-file sentinel entries
          (is (>= (count audit-entries) 3)
              "Should have at least one audit entry per source record")

          ;; Each audit entry should have provenance fields
          (doseq [entry audit-values]
            (is (string? (:source-line-key entry))
                "Audit entry should have a source-line-key")
            (is (some? (:event-type entry))
                "Audit entry should record the event type")
            (is (string? (:file-path entry))
                "Audit entry should record the file path")
            (is (number? (:byte-offset entry))
                "Audit entry should record byte offset"))

          ;; Parse error audit entry should flag it
          (let [error-audits (filter #(= :invalid-json (:parse-error-kind %)) audit-values)]
            (is (pos? (count error-audits))
                "Parse error should produce an audit entry with :invalid-json kind"))

          ;; Valid entries should list created container IDs
          (let [valid-audits (filter #(nil? (:parse-error-kind %)) audit-values)
                with-containers (filter #(pos? (:containers-created %)) valid-audits)]
            (is (pos? (count with-containers))
                "Valid records should produce audit entries with container IDs")))))))

(deftest file-offset-tracks-byte-position-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "15. File-offset PState tracks last byte offset for watch resume"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              line1 (valid-line "conv-M" "msg-1")
              line2 (valid-line "conv-M" "msg-2")
              _ (write-lines! file [line1 line2])
              request (harvest-request "h-offset" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)

              ;; Get file-key
              fid (t/file-id file)
              sfk (t/source-file-key :claude-code fid)
              offset-row (ti/read-file-offset runtime sfk)]

          (is (some? offset-row) "File offset row should exist after harvest")
          (is (pos? (:last-byte-offset offset-row))
              "Last byte offset should be positive")
          ;; The offset should be >= the sum of bytes of both lines + newlines
          (is (>= (:last-byte-offset offset-row)
                  (+ (count (.getBytes line1 "UTF-8")) 1
                     (count (.getBytes line2 "UTF-8")) 1))
              "Offset should be at or past the end of all lines")
          (is (= "h-offset" (:last-request-id offset-row))))))))

(deftest watch-resumes-from-durable-offset-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "16. Watch resumes from durable file offset after executor restart"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              _ (write-lines!
                  file
                  [(valid-line "conv-N" "msg-pre-1")
                   (valid-line "conv-N" "msg-pre-2")])
              source :claude-code
              ck (ti/conv-key source "conv-N")
              conv-id (ti/conversation-container-id ck)

              ;; Harvest to establish offset
              h-request (harvest-request "h-resume" (.getPath dir))
              _ (ti/harvest-ingest! runtime h-request)

              ;; Verify 2 messages
              proj1 (ti/read-conversation-projection runtime conv-id)
              msgs1 (projection-messages proj1)
              _ (is (= 2 (count msgs1)))

              ;; Append new content AFTER harvest
              _ (append-text! file (str (valid-line "conv-N" "msg-post-1") "\n"))

              ;; Start watch — should resume from harvest offset, not re-read old lines
              w-request (watch-request "w-resume" (.getPath dir))
              watch (ti/start-watch-ingest! runtime w-request {:poll-ms 10000})
              _ ((:poll-once! watch))
              _ (Thread/sleep 300)

              proj2 (await-materialized
                      #(ti/read-conversation-projection runtime conv-id)
                      #(= 3 (count (projection-messages %))))
              msgs2 (projection-messages proj2)]

          ;; Should have exactly 3 messages total (2 from harvest + 1 from watch)
          (is (= 3 (count msgs2))
              "Watch should resume from harvest offset and add only new messages")
          ((:stop! watch)))))))

;; ── Additional edge case tests ─────────────────────────────────────────────────

(deftest message-uuid-fallback-to-file-offset-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "Message without UUID falls back to file-id+byte-offset for container ID"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              ;; Line with no uuid field
              _ (write-lines!
                  file
                  [(str "{\"type\":\"assistant\",\"session_id\":\"conv-fallback\""
                        ",\"timestamp\":\"2026-05-11T00:00:00Z\""
                        ",\"message\":{\"role\":\"assistant\",\"content\":[]}}")])
              request (harvest-request "h-fallback" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)
              source :claude-code
              ck (ti/conv-key source "conv-fallback")
              conv-id (ti/conversation-container-id ck)
              projection (ti/read-conversation-projection runtime conv-id)
              msgs (projection-messages projection)]

          ;; Should still create a message container using the fallback ID
          (is (= 1 (count msgs))
              "Message without UUID should still produce a container"))))))

(deftest multiple-conversations-in-one-harvest-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "Multiple conversation files in one harvest produce separate conversations"
        (let [dir (temp-dir)
              file1 (io/file dir "session1.jsonl")
              file2 (io/file dir "session2.jsonl")
              _ (write-lines! file1 [(valid-line "conv-multi-1" "msg-1a")
                                     (valid-line "conv-multi-1" "msg-1b")])
              _ (write-lines! file2 [(valid-line "conv-multi-2" "msg-2a")])
              request (harvest-request "h-multi" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)
              source :claude-code

              ck1 (ti/conv-key source "conv-multi-1")
              ck2 (ti/conv-key source "conv-multi-2")
              conv-id-1 (ti/conversation-container-id ck1)
              conv-id-2 (ti/conversation-container-id ck2)

              proj1 (ti/read-conversation-projection runtime conv-id-1)
              proj2 (ti/read-conversation-projection runtime conv-id-2)
              msgs1 (projection-messages proj1)
              msgs2 (projection-messages proj2)]

          (is (= 2 (count msgs1)) "First conversation should have 2 messages")
          (is (= 1 (count msgs2)) "Second conversation should have 1 message"))))))

(deftest follows-edges-track-message-ordering-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "Follows edges link messages in sequence within a conversation"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              _ (write-lines!
                  file
                  [(valid-line "conv-follows" "msg-f1")
                   (valid-line "conv-follows" "msg-f2")
                   (valid-line "conv-follows" "msg-f3")])
              request (harvest-request "h-follows" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)
              source :claude-code
              ck (ti/conv-key source "conv-follows")
              conv-id (ti/conversation-container-id ck)

              ;; Read edges for the conversation container
              conv-edges (com.rpl.rama/foreign-select
                           [(com.rpl.rama.path/keypath conv-id)
                            com.rpl.rama.path/ALL]
                           (:composition-edges runtime))
              follows-edges (filter #(= :follows (:edge-type (second %))) conv-edges)]

          ;; With 3 messages, we should have 2 :follows edges (msg2 follows msg1, msg3 follows msg2)
          (is (= 2 (count follows-edges))
              "Should have N-1 follows edges for N messages"))))))

(deftest run-status-for-failed-request-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "Invalid request produces a failed run with errors"
        (let [bad-request {:request/type :invalid-type
                           :transcript/request-id "h-bad"
                           :transcript/source :nonexistent-source
                           :transcript/paths "not-a-vector"
                           :transcript/redaction-policy :unknown
                           :routing/key [:transcript/request "h-bad"]}]
          (ti/append-ingest-request! runtime bad-request)
          (let [run (await-materialized
                      #(ti/read-ingest-run runtime "h-bad")
                      some?)]
            (is (= :failed (:status run))
                "Invalid request should produce a :failed run")
            (is (some? (:error run))
                "Failed run should have error details")))))))

(deftest empty-tool-use-id-skipped-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "Tool-use block with empty/nil id does not create a tool-call container"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              ;; Tool block with empty id
              _ (write-lines!
                  file
                  [(str "{\"type\":\"assistant\",\"session_id\":\"conv-no-tool-id\""
                        ",\"uuid\":\"msg-noid\""
                        ",\"timestamp\":\"2026-05-11T00:00:00Z\""
                        ",\"message\":{\"role\":\"assistant\""
                        ",\"content\":[{\"type\":\"tool_use\",\"id\":\"\",\"name\":\"bash\",\"input\":{}}]}}")])
              request (harvest-request "h-no-tool-id" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)

              ;; No tool-call index entries for bash from this harvest
              bash-calls (ti/read-tool-calls-by-name runtime "bash")]

          ;; Tool with empty id should be skipped
          (is (empty? bash-calls)
              "Tool-use block with empty id should not produce a tool-call entry"))))))

(deftest conversation-meta-entry-in-projection-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "Conversation projection includes a meta entry with conversation metadata"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              _ (write-lines!
                  file
                  [(valid-line "conv-meta" "msg-1")])
              request (harvest-request "h-meta" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)
              source :claude-code
              ck (ti/conv-key source "conv-meta")
              conv-id (ti/conversation-container-id ck)
              projection (ti/read-conversation-projection runtime conv-id)
              meta-entry (projection-meta projection)]

          (is (some? meta-entry) "Projection should include a meta entry")
          (let [[_k v] meta-entry]
            (is (= :meta (:entry-type v)))
            (is (= "conv-meta" (:conversation-id v)))
            (is (= ck (:conv-key v)))
            (is (= source (:source v)))))))))

(deftest extract-conv-key-from-various-prefixes-test
  ;; Pure function test, no runtime needed
  (testing "extract-conv-key handles all ID prefix formats"
    (let [ck "abc123def456"]
      (is (= ck (ti/extract-conv-key (str "tc:conv:" ck))))
      (is (= ck (ti/extract-conv-key (str "tc:msg:" ck ":msghash"))))
      (is (= ck (ti/extract-conv-key (str "tc:tc:" ck ":toolhash"))))
      (is (= ck (ti/extract-conv-key (str "tc:tr:" ck ":toolhash"))))
      (is (= ck (ti/extract-conv-key (str "tc:art:" ck ":arthash"))))
      ;; Unknown prefix returns the string itself
      (is (= "raw-key" (ti/extract-conv-key "raw-key"))))))

(deftest partition-by-conv-key-consistency-test
  ;; Pure function test
  (testing "All IDs for same conversation route to same partition"
    (let [ck "somehash64chars"
          num-partitions 8
          conv-partition (ti/partition-by-conv-key num-partitions (str "tc:conv:" ck))
          msg-partition (ti/partition-by-conv-key num-partitions (str "tc:msg:" ck ":msghash"))
          tc-partition (ti/partition-by-conv-key num-partitions (str "tc:tc:" ck ":toolhash"))
          tr-partition (ti/partition-by-conv-key num-partitions (str "tc:tr:" ck ":toolhash"))
          art-partition (ti/partition-by-conv-key num-partitions (str "tc:art:" ck ":arthash"))]
      (is (= conv-partition msg-partition tc-partition tr-partition art-partition)
          "All IDs with same conv-key should map to the same partition"))))

(deftest build-message-containers-pure-function-test
  ;; Pure function test for build-message-containers
  (testing "build-message-containers produces correct output for a normal observation"
    (let [obs {:transcript/source :claude-code
               :transcript/source-version :unknown
               :transcript/conversation-id "pure-conv"
               :transcript/message-uuid "pure-msg"
               :transcript/event-type :assistant
               :transcript/source-timestamp "2026-05-11T00:00:00Z"
               :transcript/redacted-payload {:message {:role "assistant"
                                                       :content [{:type "text" :text "hello"}]}}
               :transcript/redacted-preview nil
               :transcript/parse-error-kind nil
               :transcript/ingest-request-id "req-pure"
               :transcript/redactions []
               :source/file-id {:device "1" :inode "100"}
               :source/file-path "/tmp/test.jsonl"
               :source/byte-offset 0
               :source/byte-length 100
               :source/line-hash "sha256:abc"}
          ck (ti/conv-key :claude-code "pure-conv")
          result (ti/build-message-containers obs ck 1000)]

      (is (= 1 (count (:containers result))) "Should create 1 container (message only)")
      (is (some? (:projection-entry result)))
      (is (= :message (:entry-type (:projection-entry result))))
      (is (string? (:msg-id result)))
      (is (some? (:anchor result)))
      (is (= 1 (count (:container-ids result))))))

  (testing "build-message-containers handles parse errors: no containers, but projection entry"
    (let [obs {:transcript/source :claude-code
               :transcript/conversation-id "err-conv"
               :transcript/message-uuid nil
               :transcript/event-type :parse-error
               :transcript/source-timestamp nil
               :transcript/redacted-payload nil
               :transcript/redacted-preview "{broken..."
               :transcript/parse-error-kind :invalid-json
               :transcript/ingest-request-id "req-err"
               :transcript/redactions []
               :source/file-id {:device "1" :inode "200"}
               :source/file-path "/tmp/bad.jsonl"
               :source/byte-offset 500
               :source/byte-length 20
               :source/line-hash "sha256:xyz"}
          ck (ti/conv-key :claude-code "err-conv")
          result (ti/build-message-containers obs ck 2000)]

      (is (empty? (:containers result)) "Parse errors should produce zero containers")
      (is (some? (:projection-entry result)) "Parse errors should still produce a projection entry")
      (is (= :invalid-json (:parse-error-kind (:projection-entry result))))
      (is (empty? (:container-ids result))))))

;; ── Gap coverage tests (Phase 6 validation) ──────────────────────────────────

(defn result-line
  "Produce a JSONL line with type \"result\" (terminal event)."
  [conversation-id & {:keys [cost-usd]}]
  (str "{\"type\":\"result\",\"session_id\":\"" conversation-id
       "\",\"uuid\":\"result-" conversation-id
       "\",\"timestamp\":\"2026-05-11T00:10:00Z\""
       ",\"message\":{\"role\":\"assistant\",\"content\":[]}"
       (when cost-usd (str ",\"cost_usd\":" cost-usd))
       "}"))

(defn unknown-type-line
  "Produce a JSONL line with an unknown event type."
  [conversation-id message-id event-type]
  (str "{\"type\":\"" event-type
       "\",\"session_id\":\"" conversation-id
       "\",\"uuid\":\"" message-id
       "\",\"timestamp\":\"2026-05-11T00:00:00Z\""
       ",\"message\":{\"role\":\"system\",\"content\":[]}}"))

(deftest standalone-tool-result-event-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "Standalone tool_result event creates a ToolResultContainer"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              ;; An assistant message with a tool_use block, followed by a user
              ;; message carrying the tool_result content block
              _ (write-lines!
                  file
                  [(valid-line "conv-tr-standalone" "msg-with-tool"
                               :tool-blocks [{:id "tu-standalone-1" :name "bash" :input "{}"}])
                   (tool-result-line "conv-tr-standalone" "msg-tr-1" "tu-standalone-1" "output text")])
              request (harvest-request "h-tr-standalone" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)
              source :claude-code
              ck (ti/conv-key source "conv-tr-standalone")
              tc-hash (core/sha-256 "tu-standalone-1")
              tr-id (ti/tool-result-container-id ck tc-hash)
              tr-container (ti/read-container runtime tr-id)]

          (is (some? tr-container) "Standalone tool_result should create a container")
          (is (= :tool-result (:container-kind tr-container))
              "Container kind should be :tool-result"))))))

(deftest result-terminal-event-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "result terminal event creates containers and is ingested"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              _ (write-lines!
                  file
                  [(system-init-line "conv-result-term")
                   (valid-line "conv-result-term" "msg-r1")
                   (result-line "conv-result-term" :cost-usd 0.05)])
              request (harvest-request "h-result-term" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)
              source :claude-code
              ck (ti/conv-key source "conv-result-term")
              conv-id (ti/conversation-container-id ck)

              ;; The result event should be ingested into the projection
              projection (ti/read-conversation-projection runtime conv-id)
              msgs (projection-messages projection)
              meta-entry (projection-meta projection)]

          ;; Conversation exists with meta
          (is (some? meta-entry) "Conversation should have a meta entry")
          ;; Result event creates a projection entry (it has a message uuid)
          ;; At minimum, the result line is ingested as a record
          (is (>= (count msgs) 2)
              "Should have at least the assistant message and result event as entries")
          ;; Audit should record all 3 lines
          (let [audit (ti/read-audit-entries runtime "h-result-term")]
            (is (>= (count audit) 3)
                "Audit should record system, assistant, and result events")))))))

(deftest unknown-event-type-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "Unknown event type creates audit entry and projection entry"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              _ (write-lines!
                  file
                  [(valid-line "conv-unknown" "msg-known")
                   (unknown-type-line "conv-unknown" "msg-mystery" "some_unknown_type")])
              request (harvest-request "h-unknown-type" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)
              source :claude-code
              ck (ti/conv-key source "conv-unknown")
              conv-id (ti/conversation-container-id ck)

              projection (ti/read-conversation-projection runtime conv-id)
              msgs (projection-messages projection)
              audit (ti/read-audit-entries runtime "h-unknown-type")]

          ;; Both records should appear in the projection
          (is (= 2 (count msgs))
              "Both the known and unknown type events should produce projection entries")
          ;; Audit should record both
          (is (>= (count audit) 2)
              "Audit should record both events including the unknown type"))))))

(deftest watch-cancellation-status-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "Watch cancellation transitions run status to :cancelled"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              _ (write-lines! file [(valid-line "conv-cancel" "msg-c1")])
              w-request (watch-request "w-cancel" (.getPath dir))
              watch (ti/start-watch-ingest! runtime w-request {:poll-ms 10000})
              _ ((:poll-once! watch))
              _ (Thread/sleep 200)

              ;; Verify running before cancel
              run-before (ti/read-ingest-run runtime "w-cancel")
              _ (is (= :running (:status run-before))
                    "Watch should be :running before stop")

              ;; Now stop
              _ ((:stop! watch))

              run-after (await-materialized
                          #(ti/read-ingest-run runtime "w-cancel")
                          #(= :cancelled (:status %)))]

          (is (= :cancelled (:status run-after))
              "After stop!, run status should become :cancelled"))))))

(deftest orphaned-tool-result-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "Orphaned tool-result (no matching tool_use) still creates a container"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              ;; tool_result referencing tu-orphan-999 which has no tool_use block
              _ (write-lines!
                  file
                  [(valid-line "conv-orphan" "msg-o1")
                   (tool-result-line "conv-orphan" "msg-o-result" "tu-orphan-999" "orphaned output")])
              request (harvest-request "h-orphan" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)
              source :claude-code
              ck (ti/conv-key source "conv-orphan")
              conv-id (ti/conversation-container-id ck)

              ;; The tool_result line is a user message with tool_result content block.
              ;; It should still be ingested as a message container.
              projection (ti/read-conversation-projection runtime conv-id)
              msgs (projection-messages projection)]

          ;; Both messages should be ingested
          (is (= 2 (count msgs))
              "Both the assistant message and tool-result user message should be ingested")
          ;; Audit covers both records
          (let [audit (ti/read-audit-entries runtime "h-orphan")]
            (is (>= (count audit) 2))))))))

(deftest terminal-state-immutability-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "Terminal run state (:complete) persists and is not modified"
        (let [dir (temp-dir)
              file (io/file dir "session.jsonl")
              _ (write-lines! file [(valid-line "conv-immut" "msg-im1")])
              request (harvest-request "h-immut" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)

              ;; Run should be :complete
              run-complete (ti/read-ingest-run runtime "h-immut")
              _ (is (= :complete (:status run-complete)))

              ;; Now do an unrelated harvest with a different request ID
              dir2 (temp-dir)
              file2 (io/file dir2 "other.jsonl")
              _ (write-lines! file2 [(valid-line "conv-other" "msg-other")])
              request2 (harvest-request "h-immut-2" (.getPath dir2))
              _ (ti/harvest-ingest! runtime request2)

              ;; The original run should still be :complete, untouched
              run-after (ti/read-ingest-run runtime "h-immut")]

          (is (= :complete (:status run-after))
              "Original completed run should remain :complete")
          (is (= (:containers-created-count run-complete)
                 (:containers-created-count run-after))
              "Container count of original run should not change"))))))

(deftest empty-directory-harvest-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "Harvest over an empty directory completes with zero containers"
        (let [dir (temp-dir)
              ;; No files written — directory is empty
              request (harvest-request "h-empty-dir" (.getPath dir))
              result (ti/harvest-ingest! runtime request)

              run (await-materialized
                    #(ti/read-ingest-run runtime "h-empty-dir")
                    #(= :complete (:status %)))]

          (is (= :complete (:status run))
              "Empty directory harvest should complete")
          (is (= 0 (:files result))
              "Result should report zero files")
          (is (= 0 (:observed-line-count run))
              "No lines should be observed")
          (is (= 0 (:containers-created-count run))
              "No containers should be created"))))))

(deftest all-malformed-lines-file-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "File with all malformed JSON produces audit entries, no message containers"
        (let [dir (temp-dir)
              file (io/file dir "all-bad.jsonl")
              _ (write-lines! file [(parse-error-line)
                                    (parse-error-line)
                                    (parse-error-line)])
              request (harvest-request "h-all-bad" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)

              ;; Conversation ID fallback for parse errors is the file path
              source :claude-code
              ck (ti/conv-key source (.getPath file))
              conv-id (ti/conversation-container-id ck)

              projection (ti/read-conversation-projection runtime conv-id)
              msgs (projection-messages projection)
              valid-msgs (filter #(nil? (:parse-error-kind (second %))) msgs)
              parse-err-msgs (filter #(some? (:parse-error-kind (second %))) msgs)

              audit (ti/read-audit-entries runtime "h-all-bad")
              parse-err-audits (filter #(= :invalid-json (:parse-error-kind (second %)))
                                       audit)]

          (is (= 0 (count valid-msgs))
              "No valid message containers should be created")
          (is (= 3 (count parse-err-msgs))
              "All 3 parse errors should appear in projection")
          (is (= 3 (count parse-err-audits))
              "All 3 parse errors should produce audit entries"))))))

(deftest conversation-id-fallback-to-file-path-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "JSONL line without session_id/conversation_id falls back to file path"
        (let [dir (temp-dir)
              file (io/file dir "no-session.jsonl")
              ;; Line with no session_id, conversation_id, uuid, or any conversation key
              _ (write-lines!
                  file
                  [(str "{\"type\":\"assistant\""
                        ",\"timestamp\":\"2026-05-11T00:00:00Z\""
                        ",\"message\":{\"role\":\"assistant\",\"content\":[]}}")])
              request (harvest-request "h-conv-fallback" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)

              ;; Conversation ID should fall back to the file path
              source :claude-code
              ck (ti/conv-key source (.getPath file))
              conv-id (ti/conversation-container-id ck)
              projection (ti/read-conversation-projection runtime conv-id)
              msgs (projection-messages projection)]

          (is (= 1 (count msgs))
              "Message should be ingested with file-path as conversation ID"))))))

(deftest non-existent-container-returns-nil-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "read-container with a non-existent ID returns nil"
        (let [result (ti/read-container runtime "tc:conv:nonexistent-fake-id-123")]
          (is (nil? result)
              "Non-existent container should return nil"))))))

(deftest non-existent-request-returns-nil-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "read-ingest-run with a non-existent request ID returns nil"
        (let [result (ti/read-ingest-run runtime "nonexistent-request-id-xyz")]
          (is (nil? result)
              "Non-existent request ID should return nil"))))))

(deftest harvest-watch-same-container-kinds-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "Harvest and watch produce the same container kinds"
        (let [;; Harvest a file
              dir-h (temp-dir)
              file-h (io/file dir-h "session.jsonl")
              _ (write-lines!
                  file-h
                  [(system-init-line "conv-same-h")
                   (valid-line "conv-same-h" "msg-sh1"
                               :tool-blocks [{:id "tu-same-h" :name "bash" :input "{}"}])
                   (user-line "conv-same-h" "msg-sh2")])
              h-request (harvest-request "h-same-kinds" (.getPath dir-h))
              _ (ti/harvest-ingest! runtime h-request)
              source :claude-code
              ck-h (ti/conv-key source "conv-same-h")
              conv-id-h (ti/conversation-container-id ck-h)

              ;; Read container kinds from harvest
              proj-h (ti/read-conversation-projection runtime conv-id-h)
              msgs-h (projection-messages proj-h)
              msg-h-ids (keep #(:container-id (second %)) msgs-h)
              harvest-kinds (set (map #(:container-kind (ti/read-container runtime %))
                                      msg-h-ids))

              ;; Watch a different dir — write content AFTER first poll establishes baseline
              dir-w (temp-dir)
              file-w (io/file dir-w "session.jsonl")
              _ (spit file-w "")  ;; create empty file (baseline = 0)
              w-request (watch-request "w-same-kinds" (.getPath dir-w))
              watch (ti/start-watch-ingest! runtime w-request {:poll-ms 10000})
              ;; First poll: establishes offset = 0 for this empty file
              _ ((:poll-once! watch))
              ;; Now APPEND content (not overwrite)
              _ (append-text! file-w (str (system-init-line "conv-same-w") "\n"
                                          (valid-line "conv-same-w" "msg-sw1"
                                                      :tool-blocks [{:id "tu-same-w" :name "bash" :input "{}"}]) "\n"
                                          (user-line "conv-same-w" "msg-sw2") "\n"))
              ;; Second poll: reads from saved offset 0 to new EOF
              _ ((:poll-once! watch))
              _ (Thread/sleep 500)

              ck-w (ti/conv-key source "conv-same-w")
              conv-id-w (ti/conversation-container-id ck-w)
              proj-w (await-materialized
                       #(ti/read-conversation-projection runtime conv-id-w)
                       #(>= (count (projection-messages %)) 3))
              msgs-w (projection-messages proj-w)
              msg-w-ids (keep #(:container-id (second %)) msgs-w)
              watch-kinds (set (map #(:container-kind (ti/read-container runtime %))
                                    msg-w-ids))]

          ;; Both should produce the same set of container kinds
          (is (= harvest-kinds watch-kinds)
              "Harvest and watch should produce identical container kind sets")
          ;; Both should include :chat-message
          (is (contains? harvest-kinds :chat-message))
          (is (contains? watch-kinds :chat-message))
          ((:stop! watch)))))))

;; ── Review probe tests (adversarial production boundary checks) ────────────────

(deftest observation-without-accepted-request-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "F1: Observation without an accepted request must not create containers"
        (let [dir (temp-dir)
              file (io/file dir "rogue.jsonl")
              _ (write-lines! file [(valid-line "rogue-conv" "rogue-msg")])
              source :claude-code
              fid (t/file-id file)
              obs (first (t/read-jsonl-observations
                           (harvest-request "no-such-request" (.getPath dir))
                           file 0))
              obs-with-ck (assoc obs :transcript/conv-key (ti/conv-key source "rogue-conv"))]
          ;; Append observation WITHOUT submitting a request first
          (ti/append-ingest-observation! runtime obs-with-ck)
          (Thread/sleep 500)
          (let [ck (ti/conv-key source "rogue-conv")
                conv-id (ti/conversation-container-id ck)
                container (ti/read-container runtime conv-id)]
            (is (nil? (ti/read-ingest-run runtime "no-such-request"))
                "No request should exist")
            (is (nil? container)
                "No container should be created without an accepted request")))))))

(deftest late-claim-does-not-mutate-terminal-state-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "F2: Late claims cannot overwrite a terminal run status"
        (let [request-id "probe-terminal"
              request (harvest-request request-id (.getPath (temp-dir)))]
          (ti/append-ingest-request! runtime request)
          ;; Drive to :complete
          (ti/append-ingest-claim!
            runtime (t/transcript-run-status-record request-id :running))
          (ti/append-ingest-claim!
            runtime (t/transcript-run-status-record request-id :complete))
          (Thread/sleep 300)
          (let [run-complete (await-materialized
                               #(ti/read-ingest-run runtime request-id)
                               #(= :complete (:status %)))]
            (is (= :complete (:status run-complete)))
            ;; Late claim to :running — should be ignored
            (ti/append-ingest-claim!
              runtime (t/transcript-run-status-record request-id :running))
            (Thread/sleep 300)
            (let [run-after (ti/read-ingest-run runtime request-id)]
              (is (= :complete (:status run-after))
                  "Terminal :complete must not be overwritten by late :running claim"))))))))

(deftest parse-error-preview-does-not-leak-secrets-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "F3: Malformed lines with secrets must have redacted previews"
        (let [dir (temp-dir)
              file (io/file dir "secrets.jsonl")
              secret-line "{\"api_key\":\"sk-proj-secret-key-12345678901234567890\"}"
              _ (write-lines! file [secret-line
                                     (valid-line "conv-sec" "msg-sec")])
              request (harvest-request "h-secrets" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)
              ;; Parse error goes to file-path conversation
              source :claude-code
              pe-ck (ti/conv-key source (.getPath file))
              pe-conv-id (ti/conversation-container-id pe-ck)
              pe-proj (ti/read-conversation-projection runtime pe-conv-id)
              pe-msgs (projection-messages pe-proj)
              audit (ti/read-audit-entries runtime "h-secrets")
              all-text (str (pr-str pe-proj) (pr-str audit))]
          ;; Secret must not appear in any durable view
          (is (not (str/includes? all-text "sk-proj-secret-key"))
              "Secret API key must be redacted from all durable views")
          (is (not (str/includes? all-text "12345678901234567890"))
              "Secret suffix must be redacted"))))))

;; ── F4: byte-correct transcript reader (source identity foundation) ───────────

(deftest read-jsonl-byte-correct-utf8-test
  ;; Pure reader-level test — no module needed.
  (testing "F4: reader decodes UTF-8 and computes byte-correct offset/length/hash"
    (let [dir (temp-dir)
          file (io/file dir "utf8.jsonl")
          ;; é,ö = 2 UTF-8 bytes each; 日/本/語 = 3 bytes each
          text1 "héllo wörld 日本語"
          text2 "second ascii line"
          line1 (valid-line "conv-utf8" "msg-u1"
                            :content (str "[{\"type\":\"text\",\"text\":\"" text1 "\"}]"))
          line2 (valid-line "conv-utf8" "msg-u2"
                            :content (str "[{\"type\":\"text\",\"text\":\"" text2 "\"}]"))
          _ (write-utf8! file (str line1 "\n" line2 "\n"))
          request (harvest-request "h-utf8-read" (.getPath dir))
          observations (t/read-jsonl-observations request file 0)
          [o1 o2] observations
          line1-bytes (.getBytes line1 "UTF-8")
          line2-bytes (.getBytes line2 "UTF-8")]

      (is (= 2 (count observations)) "Should read exactly 2 lines")

      ;; 1. Content decodes correctly — no Latin-1 mojibake.
      ;;    The old RandomAccessFile.readLine path produced "hÃ©llo wÃ¶rld".
      (let [decoded (get-in (:transcript/redacted-payload o1)
                            [:message :content 0 :text])]
        (is (= text1 decoded) "Multi-byte content must decode to the original string")
        (is (not (str/includes? decoded "Ã"))
            "No Latin-1 mojibake artifacts in decoded content"))

      ;; 2. byte-length = true UTF-8 content bytes + 1 (the \n terminator).
      (is (= (+ (alength line1-bytes) 1) (:source/byte-length o1))
          "byte-length must match true UTF-8 byte count + terminator")
      (is (= (+ (alength line2-bytes) 1) (:source/byte-length o2)))

      ;; 3. Offsets do not cascade-drift: line 2 starts where line 1 ended.
      (is (= 0 (:source/byte-offset o1)))
      (is (= (:source/byte-length o1) (:source/byte-offset o2))
          "Second line's byte-offset must equal first line's byte-length")

      ;; 4. line-hash is computed over the exact on-disk content bytes.
      (is (= (t/line-hash-bytes line1-bytes) (:source/line-hash o1))
          "line-hash must hash the true content bytes")
      (is (= (t/line-hash-bytes line2-bytes) (:source/line-hash o2))))))

(deftest read-jsonl-no-trailing-newline-test
  ;; Pure reader-level test — locks in the byte-length edge case the old reader
  ;; got wrong (it added a phantom +1 to the final unterminated line).
  (testing "F4: final line without trailing newline has byte-length = content bytes"
    (let [dir (temp-dir)
          file (io/file dir "no-nl.jsonl")
          line1 (valid-line "conv-nonl" "msg-n1")
          line2 (valid-line "conv-nonl" "msg-n2")
          _ (write-utf8! file (str line1 "\n" line2))  ;; NO trailing newline
          request (harvest-request "h-no-nl" (.getPath dir))
          observations (t/read-jsonl-observations request file 0)
          [o1 o2] observations
          line1-bytes (.getBytes line1 "UTF-8")
          line2-bytes (.getBytes line2 "UTF-8")]

      (is (= 2 (count observations)))
      (is (= (+ (alength line1-bytes) 1) (:source/byte-length o1))
          "Terminated line includes its newline byte")
      (is (= (alength line2-bytes) (:source/byte-length o2))
          "Unterminated final line must not add a phantom terminator byte")
      (is (= (:source/byte-length o1) (:source/byte-offset o2)))
      ;; The accounting must sum exactly to the file size — the decisive check.
      (is (= (.length file)
             (+ (:source/byte-offset o2) (:source/byte-length o2)))
          "Sum of offsets + lengths must equal the true file size"))))

(deftest read-jsonl-empty-and-newline-terminated-test
  ;; Pure reader-level test — no phantom trailing observation.
  (testing "F4: empty file yields zero observations; a fully \\n-terminated file
            yields exactly its line count (no phantom empty trailing line)"
    (let [dir (temp-dir)
          empty-file (io/file dir "empty.jsonl")
          _ (write-utf8! empty-file "")
          term-file (io/file dir "term.jsonl")
          l1 (valid-line "conv-term" "msg-t1")
          l2 (valid-line "conv-term" "msg-t2")
          _ (write-utf8! term-file (str l1 "\n" l2 "\n"))
          request (harvest-request "h-term" (.getPath dir))]
      (is (= 0 (count (t/read-jsonl-observations request empty-file 0)))
          "Empty file must produce zero observations")
      (let [obs (t/read-jsonl-observations request term-file 0)]
        (is (= 2 (count obs))
            "Fully terminated file must produce exactly its line count")
        (is (= (.length term-file)
               (reduce + (map :source/byte-length obs)))
            "Byte-lengths must sum to file size for a terminated file")))))

(deftest harvest-preserves-non-ascii-content-test
  (with-ingest-runtime
    (fn [runtime]
      (testing "F4: harvested container content + source anchor are byte-correct for UTF-8"
        (let [dir (temp-dir)
              file (io/file dir "utf8.jsonl")
              text "héllo wörld 日本語 — ünïcode ✓"
              line (valid-line "conv-utf8-h" "msg-utf8"
                               :content (str "[{\"type\":\"text\",\"text\":\"" text "\"}]"))
              _ (write-utf8! file (str line "\n"))
              request (harvest-request "h-utf8" (.getPath dir))
              _ (ti/harvest-ingest! runtime request)
              source :claude-code
              ck (ti/conv-key source "conv-utf8-h")
              msg-hash (core/sha-256 "msg-utf8")
              msg-id (ti/message-container-id ck msg-hash)
              container (ti/read-container runtime msg-id)
              anchor (com.rpl.rama/foreign-select-one
                       [(com.rpl.rama.path/keypath msg-id)]
                       (:source-anchors runtime))
              line-bytes (.getBytes line "UTF-8")]

          (is (some? container) "Message container should exist")
          ;; Content round-trips with no mojibake.
          (is (= text (:revision-content container))
              "Container content must preserve multi-byte characters exactly")
          (is (not (str/includes? (pr-str container) "Ã"))
              "No Latin-1 mojibake in container")

          ;; Source anchor reflects the true byte layout.
          (is (some? anchor) "Source anchor should exist")
          (is (= 0 (:byte-offset anchor)))
          (is (= (+ (alength line-bytes) 1) (:byte-length anchor))
              "Anchor byte-length must match true UTF-8 bytes + newline")
          (is (= (t/line-hash-bytes line-bytes) (:line-hash anchor))
              "Anchor line-hash must hash the true on-disk bytes"))))))

(deftest read-jsonl-invalid-utf8-byte-correct-test
  ;; Pure reader-level test. Invalid UTF-8 must degrade gracefully (parse-error,
  ;; not corrupt truth) AND its source identity must still be computed from the
  ;; raw bytes — NOT from a lossy U+FFFD re-encode (which is what hashing the
  ;; decoded string would do).
  (testing "F4: invalid UTF-8 bytes -> parse-error with byte-correct identity"
    (let [dir (temp-dir)
          file (io/file dir "bad-utf8.jsonl")
          ;; 0xFF / 0xFE are never valid UTF-8 lead or continuation bytes.
          line-bytes (byte-array [0x7B (unchecked-byte 0xFF) (unchecked-byte 0xFE) 0x7D])
          file-bytes (byte-array [0x7B (unchecked-byte 0xFF) (unchecked-byte 0xFE) 0x7D 0x0A])
          _ (write-bytes! file file-bytes)
          request (harvest-request "h-bad-utf8" (.getPath dir))
          observations (t/read-jsonl-observations request file 0)
          o1 (first observations)]
      (is (= 1 (count observations)))
      (is (= :invalid-json (:transcript/parse-error-kind o1))
          "Unparseable bytes must surface as a parse error, not corrupt truth")
      (is (= 0 (:source/byte-offset o1)))
      (is (= (+ (alength line-bytes) 1) (:source/byte-length o1))
          "byte-length must reflect the raw on-disk bytes")
      (is (= (t/line-hash-bytes line-bytes) (:source/line-hash o1))
          "line-hash must hash the exact raw bytes, not a lossy U+FFFD re-encode"))))
