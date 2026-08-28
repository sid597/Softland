(ns app.server.ingest.dogfood-transcript-probe-test
  "Depot-adversary + contract probes for the transcript kernel (fix session 5).

   One IPC launch, one deftest, sequential testing blocks on disjoint keys.
   Each block asserts the SEMANTIC payload the implicit spec promises (redacted
   previews, byte offsets, per-run counters, sticky terminals, claim ownership),
   not just row existence.

   Failing baseline (pre-fix, recorded 2026-06-12):
     parse-error-redaction   → raw secret persisted in ledger preview, redactions []
     duplicate-submit        → :complete run reset to :pending, counters zeroed
     duplicate-submit-diff   → same reset, no conflict recorded
     harvest-partial-tail    → fragment ingested as parse error, cursor mid-line
     multi-tool-use          → second tool_use block unaddressable, inputs absent
     late-claim-post-terminal→ :complete flipped back to :running
     per-run-counters        → re-harvest run reports 0 observed lines
     claim-arbitration       → no ownership; both executors 'win'
     failed-then-cancel      → terminal :failed overwritten by :cancelled"
  (:require [app.server.ingest.transcript :as transcript]
            [app.server.rama.probe-harness :as probe]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(defn temp-dir
  []
  (.toFile (java.nio.file.Files/createTempDirectory
             "softland-transcript-probe"
             (make-array java.nio.file.attribute.FileAttribute 0))))

(defn write-text!
  [file text]
  (spit file text))

(defn valid-line
  [conversation-id message-id & [content]]
  (str "{\"type\":\"assistant\",\"session_id\":\"" conversation-id
       "\",\"uuid\":\"" message-id
       "\",\"timestamp\":\"2026-05-11T00:00:00Z\",\"message\":{\"content\":"
       (or content "[]")
       "}}"))

(defn harvest-request
  [request-id dir]
  (transcript/transcript-request
    :transcript/harvest
    {:transcript/request-id request-id
     :transcript/source :claude-code
     :transcript/paths [(.getPath ^java.io.File dir)]}))

(defn run-semantics
  "Committed-truth snapshot of a run row: the fields a regression would corrupt."
  [runtime request-id]
  (select-keys (transcript/read-run runtime request-id)
               [:status :observed-line-count :parse-error-count :claim/owner]))

(defn built-observations
  "Pure-build the file's observations (incl. a trailing partial, per the June
   reader contract) so probes can compute deterministic line keys."
  [request file]
  (vec (transcript/read-jsonl-observations request file 0)))

(defn status-claim
  [request-id status executor-id & [opts]]
  (assoc (transcript/transcript-run-status-record request-id status opts)
         :executor/id executor-id))

(deftest transcript-adversary-probe-matrix-test
  (let [runtime (transcript/start-transcript-runtime!)]
    (try
      (testing "utf8 byte identity: offsets, lengths, payload text survive multi-byte content"
        (let [dir (temp-dir)
              file (io/file dir "utf8.jsonl")
              text-content "héllo 世界 → ok"
              line1 (valid-line "utf8-conv" "msg-é-1"
                                (str "[{\"type\":\"text\",\"text\":\"" text-content "\"}]"))
              line2 (valid-line "utf8-conv" "msg-é-2")
              line1-bytes (alength (.getBytes ^String line1 "UTF-8"))
              _ (write-text! file (str line1 "\n" line2 "\n"))
              request (harvest-request "utf8-harvest" dir)
              obs (built-observations request file)]
          (is (= 2 (count obs)))
          (is (= 0 (:source/byte-offset (first obs))))
          (is (= (inc line1-bytes) (:source/byte-length (first obs)))
              "byte-length counts real UTF-8 bytes + newline, not chars")
          (is (= (inc line1-bytes) (:source/byte-offset (second obs)))
              "second line starts at the first line's true byte end")
          (transcript/harvest-transcripts! runtime request)
          (transcript/await-materialized
            #(transcript/read-run runtime "utf8-harvest")
            #(= :complete (:status %)))
          (let [ledger-row (transcript/read-ledger-line
                             runtime (transcript/source-line-key (first obs)))
                stored-text (get-in ledger-row
                                    [:transcript/redacted-payload :message :content 0 :text])]
            (is (= text-content stored-text)
                "persisted payload text is byte-identical to the source")
            (is (= (:source/line-hash (first obs)) (:source/line-hash ledger-row))
                "re-derived hash re-matches the persisted identity"))
          (let [file-state (transcript/read-source-file-state
                             runtime :claude-code (:source/file-id (first obs)))]
            (is (= (.length ^java.io.File file)
                   (:source/last-byte-offset file-state))
                "cursor lands exactly on the file's true byte length"))))

      (testing "parse-error redaction: malformed line with a secret persists redacted, with metadata"
        (let [dir (temp-dir)
              file (io/file dir "secrets.jsonl")
              ;; Writer killed mid-line: unterminated JSON carrying a secret, then
              ;; completed by a newline so it IS harvested as a parse-error row.
              malformed "{\"api_key\":\"never-store-me-probe\", \"x\": tru"
              _ (write-text! file (str (valid-line "secret-conv" "msg-1") "\n"
                                       malformed "\n"))
              request (harvest-request "secret-harvest" dir)
              obs (built-observations request file)
              parse-error-obs (first (filter :transcript/parse-error-kind obs))]
          (is (some? parse-error-obs))
          (transcript/harvest-transcripts! runtime request)
          (transcript/await-materialized
            #(transcript/read-run runtime "secret-harvest")
            #(= :complete (:status %)))
          (let [row (transcript/read-ledger-line
                      runtime (transcript/source-line-key parse-error-obs))]
            (is (some? row) "parse-error line has a ledger row")
            (is (not (str/includes? (pr-str row) "never-store-me-probe"))
                "no readable projection of the row carries the raw secret")
            (is (seq (:transcript/redactions row))
                "redaction metadata answers 'what was masked?'"))))

      (testing "duplicate submit after :complete: same payload replays as a total no-op"
        (let [dir (temp-dir)
              file (io/file dir "dup.jsonl")
              _ (write-text! file (str (valid-line "dup-conv" "msg-1") "\n"
                                       (valid-line "dup-conv" "msg-2") "\n"))
              request (harvest-request "dup-harvest" dir)]
          (transcript/harvest-transcripts! runtime request)
          (transcript/await-materialized
            #(transcript/read-run runtime "dup-harvest")
            #(and (= :complete (:status %)) (= 2 (:observed-line-count %))))
          (let [result (probe/probe-duplicate-id-same-payload!
                         {:read-state #(run-semantics runtime "dup-harvest")
                          :append! #(transcript/append-transcript-request! runtime request)
                          :settle-ms 600})]
            (is (:pass? result) (pr-str result))
            (is (= :complete (:status (:after result)))
                "terminal status survives a duplicate submit")
            (is (= 2 (:observed-line-count (:after result)))
                "counters survive a duplicate submit"))))

      (testing "duplicate submit, different payload: conflict recorded, truth untouched"
        (let [dir (temp-dir)
              other-dir (temp-dir)
              file (io/file dir "dupdiff.jsonl")
              _ (write-text! file (str (valid-line "dupdiff-conv" "msg-1") "\n"))
              request (harvest-request "dupdiff-harvest" dir)
              impostor (harvest-request "dupdiff-harvest" other-dir)]
          (transcript/harvest-transcripts! runtime request)
          (transcript/await-materialized
            #(transcript/read-run runtime "dupdiff-harvest")
            #(= :complete (:status %)))
          (let [result (probe/probe-duplicate-id-different-payload!
                         {:read-state #(run-semantics runtime "dupdiff-harvest")
                          :append! #(transcript/append-transcript-request! runtime impostor)
                          :read-signal #(:request-conflicts
                                          (transcript/read-run runtime "dupdiff-harvest"))
                          :settle-ms 600})]
            (is (:pass? result) (pr-str result))
            (is (seq (:signal-after result))
                "the conflicting reuse is recorded, not silently swallowed"))))

      (testing "harvest partial trailing line: not ingested, cursor gated at last complete line"
        (let [dir (temp-dir)
              file (io/file dir "partial.jsonl")
              complete-1 (valid-line "partial-conv" "msg-1")
              complete-2 (valid-line "partial-conv" "msg-2")
              fragment "{\"type\":\"assistant\",\"session_id\":\"partial-conv\",\"uuid\":\"msg-3"
              _ (write-text! file (str complete-1 "\n" complete-2 "\n" fragment))
              request (harvest-request "partial-harvest" dir)
              obs (built-observations request file)
              fragment-obs (last obs)
              complete-end (+ (long (:source/byte-offset (second obs)))
                              (long (:source/byte-length (second obs))))]
          (is (= 3 (count obs)) "the raw builder does surface the fragment")
          (transcript/harvest-transcripts! runtime request)
          (transcript/await-materialized
            #(transcript/read-run runtime "partial-harvest")
            #(= :complete (:status %)))
          (let [run (transcript/read-run runtime "partial-harvest")]
            (is (= 2 (:observed-line-count run))
                "only newline-terminated lines are observed")
            (is (= 0 (:parse-error-count run))
                "the fragment is not a parse error"))
          (is (nil? (transcript/read-ledger-line
                      runtime (transcript/source-line-key fragment-obs)))
              "no ledger row exists for the fragment")
          (is (= 2 (count (transcript/read-conversation runtime "partial-conv"))))
          (let [file-state (transcript/read-source-file-state
                             runtime :claude-code (:source/file-id fragment-obs))]
            (is (= complete-end (:source/last-byte-offset file-state))
                "cursor stops at the last complete line, never mid-line"))))

      (testing "multi tool_use line: every block indexed, with redacted inputs"
        (let [dir (temp-dir)
              file (io/file dir "tools.jsonl")
              content (str "[{\"type\":\"tool_use\",\"id\":\"probe-tool-a\",\"name\":\"bash\","
                           "\"input\":{\"api_key\":\"never-store-me-tool\"}},"
                           "{\"type\":\"tool_use\",\"id\":\"probe-tool-b\",\"name\":\"read\","
                           "\"input\":{\"path\":\"/tmp/x\"}}]")
              _ (write-text! file (str (valid-line "tools-conv" "msg-1" content) "\n"))
              request (harvest-request "tools-harvest" dir)]
          (transcript/harvest-transcripts! runtime request)
          (transcript/await-materialized
            #(transcript/read-run runtime "tools-harvest")
            #(= :complete (:status %)))
          (let [tool-a (transcript/await-materialized
                         #(transcript/read-tool-call runtime "probe-tool-a") some?)
                tool-b (transcript/await-materialized
                         #(transcript/read-tool-call runtime "probe-tool-b") some?)]
            (is (= "bash" (:tool-call/name tool-a)))
            (is (= "read" (:tool-call/name tool-b))
                "the SECOND tool_use block is addressable too")
            (is (= {:path "/tmp/x"} (:tool-call/input tool-b))
                "redacted inputs are stored on the index row")
            (is (some? (:tool-call/input tool-a)))
            (is (not (str/includes? (pr-str [tool-a tool-b]) "never-store-me-tool"))
                "stored inputs are post-redaction"))))

      (testing "late claim after terminal: :complete never regresses, rejection auditable"
        (let [dir (temp-dir)
              file (io/file dir "late.jsonl")
              _ (write-text! file (str (valid-line "late-conv" "msg-1") "\n"))
              request (harvest-request "late-harvest" dir)]
          (transcript/harvest-transcripts! runtime request)
          (transcript/await-materialized
            #(transcript/read-run runtime "late-harvest")
            #(= :complete (:status %)))
          (let [late-claim (status-claim "late-harvest" :running "late-executor")
                result (probe/probe-post-terminal-write!
                         {:read-state #(run-semantics runtime "late-harvest")
                          :append-late! #(transcript/append-transcript-status!
                                           runtime late-claim)
                          :settle-ms 600})]
            (is (:pass? result) (pr-str result))
            (is (= :complete (:status (:after result)))
                "a stale :running heartbeat cannot reopen a terminal run"))))

      (testing "duplicate observation append: counters and views absorb the replay"
        (let [dir (temp-dir)
              file (io/file dir "obsdup.jsonl")
              _ (write-text! file (str (valid-line "obsdup-conv" "msg-1") "\n"))
              request (harvest-request "obsdup-harvest" dir)
              obs (built-observations request file)]
          (transcript/harvest-transcripts! runtime request)
          (transcript/await-materialized
            #(transcript/read-run runtime "obsdup-harvest")
            #(and (= :complete (:status %)) (= 1 (:observed-line-count %))))
          (let [result (probe/probe-duplicate-id-same-payload!
                         {:read-state #(merge (run-semantics runtime "obsdup-harvest")
                                              {:conversation-count
                                               (count (transcript/read-conversation
                                                        runtime "obsdup-conv"))})
                          :append! #(transcript/append-transcript-observation!
                                      runtime (first obs))
                          :settle-ms 600})]
            (is (:pass? result) (pr-str result)))))

      (testing "orphan observation: no phantom run row for an unsubmitted request-id"
        (let [dir (temp-dir)
              file (io/file dir "orphan.jsonl")
              _ (write-text! file (str (valid-line "orphan-conv" "msg-1") "\n"))
              request (harvest-request "orphan-request" dir)
              obs (built-observations request file)
              result (probe/probe-append-before-request!
                       {:read-state #(transcript/read-run runtime "orphan-request")
                        :append-obs! #(transcript/append-transcript-observation!
                                        runtime (first obs))
                        :settle-ms 600})]
          (is (:pass? result) (pr-str result))))

      (testing "per-run counters: a re-harvest run reports ITS OWN processed lines"
        (let [dir (temp-dir)
              file (io/file dir "rerun.jsonl")
              _ (write-text! file (str (valid-line "rerun-conv" "msg-1") "\n"
                                       (valid-line "rerun-conv" "msg-2") "\n"))
              request-1 (harvest-request "rerun-1" dir)
              request-2 (harvest-request "rerun-2" dir)]
          (transcript/harvest-transcripts! runtime request-1)
          (transcript/await-materialized
            #(transcript/read-run runtime "rerun-1")
            #(= :complete (:status %)))
          (transcript/harvest-transcripts! runtime request-2)
          (let [run-2 (transcript/await-materialized
                        #(transcript/read-run runtime "rerun-2")
                        #(= :complete (:status %)))]
            (is (= 2 (:observed-line-count run-2))
                "second run processed 2 lines even though both deduped in views")
            (is (= 2 (count (transcript/read-conversation runtime "rerun-conv")))
                "views still hold exactly one record per unique line"))))

      (testing "claim arbitration: one executor wins, the loser is rejected"
        (let [dir (temp-dir)
              request (transcript/transcript-request
                        :transcript/harvest
                        {:transcript/request-id "race-harvest"
                         :transcript/source :claude-code
                         :transcript/paths [(.getPath ^java.io.File dir)]})]
          (transcript/append-transcript-request! runtime request)
          (transcript/await-materialized
            #(transcript/read-run runtime "race-harvest")
            #(= :pending (:status %)))
          (transcript/append-transcript-status!
            runtime (status-claim "race-harvest" :running "executor-A"))
          (transcript/await-materialized
            #(transcript/read-run runtime "race-harvest")
            #(= :running (:status %)))
          (is (= "executor-A" (:claim/owner (transcript/read-run runtime "race-harvest")))
              "first claim is granted ownership")
          (transcript/append-transcript-status!
            runtime (status-claim "race-harvest" :running "executor-B"))
          (Thread/sleep 600)
          (let [run (transcript/read-run runtime "race-harvest")]
            (is (= "executor-A" (:claim/owner run))
                "the losing executor cannot steal the claim")
            (is (some #(= :token-mismatch (:dead-letter/reason %)) (:claim-errors run))
                "the losing claim is rejected with an auditable reason"))))

      (testing "terminal :failed survives a later :cancelled (watch error then stop!)"
        (let [dir (temp-dir)
              request (transcript/transcript-request
                        :transcript/watch
                        {:transcript/request-id "failcancel-watch"
                         :transcript/source :claude-code
                         :transcript/paths [(.getPath ^java.io.File dir)]})]
          (transcript/append-transcript-request! runtime request)
          (transcript/await-materialized
            #(transcript/read-run runtime "failcancel-watch")
            #(= :pending (:status %)))
          (transcript/append-transcript-status!
            runtime (status-claim "failcancel-watch" :running "watch-exec"))
          (transcript/await-materialized
            #(transcript/read-run runtime "failcancel-watch")
            #(= :running (:status %)))
          (transcript/append-transcript-status!
            runtime (status-claim "failcancel-watch" :failed "watch-exec"
                                  {:error {:message "boom"}}))
          (transcript/await-materialized
            #(transcript/read-run runtime "failcancel-watch")
            #(= :failed (:status %)))
          (transcript/append-transcript-status!
            runtime (status-claim "failcancel-watch" :cancelled "watch-exec"))
          (Thread/sleep 600)
          (is (= :failed (:status (transcript/read-run runtime "failcancel-watch")))
              "the first terminal status is final; no terminal flip-flop")))

      (finally
        (transcript/close-transcript-runtime! runtime)))))
