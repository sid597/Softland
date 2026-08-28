(ns app.server.door.server-jetty
  "HTTP and SSE entry point for the land.
   Takes: Ring requests for ten POST routes, turn streams, assertions, edits, activations, and room actions.
   Gives: Ring responses, SSE events, and durable request results; spawns `claude`.
   Holds: ambient-autotag-runtime and data/relation-assert-log.ednl."
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [clojure.walk :as walk]
    [com.rpl.rama.path :refer [keypath]]
    [app.server.page.block-edit :as block-edit]
    [app.server.episode.cascade :as cascade]
    [app.server.episode.episode :as episode]
    [app.server.rama.envelope :as rama-core]
    [app.server.episode.material-circulation :as circulation]
    [app.server.page.face-projection :as face-projection]
    [app.server.worn.material-truth :as material-truth]
    [app.server.rama.object-container :as oc]
    [app.server.worn.facet-master :as facet-master]
    [app.server.rama.object-container.runtime :as ocr]
    [app.server.episode.llm :as llm]
    [app.server.door.cluster :as cluster]
    [app.server.worn.facet-engine :as facet-engine]
    [app.server.worn.facet-masters :as facet-masters]
    [app.server.worn.invocation-material :as invocation-material]
    [app.server.page.matter-room :as matter-room]
    [app.server.rama.ingest-epoch :as ingest-epoch]
    [app.server.rama.relation-kernel :as rk]
    [app.server.page.reply-to-block :as reply-to-block]
    [app.server.env :as env]
    [clj-http.client :as http]
    [cheshire.core :as json]
    [ring.adapter.jetty :as ring]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.core.protocols :as ring-protocols]
    [ring.util.response :as res])
  (:import
    (java.io BufferedReader InputStreamReader OutputStreamWriter)
    (java.util.concurrent TimeUnit)
    (org.eclipse.jetty.server.handler.gzip GzipHandler)))

(defn not-found-handler [_ring-request]
  (-> (res/not-found "Not found")
    (res/content-type "text/plain")))

(defn json-response [data]
  (-> (res/response (pr-str data))
      (res/content-type "application/edn")))

(defn parse-edn-body
  [ring-req]
  (let [body-str (some-> ring-req :body slurp str/trim)]
    (if (str/blank? body-str)
      {}
      (edn/read-string body-str))))

(defn preview-str
  [x]
  (let [s (str (or x ""))]
    (if (> (count s) 180)
      (str (subs s 0 180) "...<truncated>")
      s)))

(defn destroy-process-tree!
  [proc]
  (try
    (doseq [child-handle (iterator-seq (.iterator (.descendants (.toHandle proc))))]
      (try
        (.destroyForcibly child-handle)
        (catch Exception _ nil)))
    (catch Exception _ nil))
  (try
    (.destroy proc)
    (catch Exception _ nil))
  (when-not (.waitFor proc 2000 TimeUnit/MILLISECONDS)
    (try
      (.destroyForcibly proc)
      (catch Exception _ nil)))
  nil)

;;; ── Streaming agent execution (SSE over POST) ──────────────────────────────

(defn stream-cli-process
  "Spawn a CLI process and read its stdout line-by-line.
   Calls (on-line line-str) for each line, (on-done info-map) when the process
   exits or times out. Runs reader + waiter on daemon threads."
  [argv cwd timeout-ms on-line on-done]
  (let [cmd  (vec (map str argv))
        pb   (ProcessBuilder. (into-array String cmd))]
    (when (seq cwd)
      (.directory pb (io/file cwd)))
    (.redirectErrorStream pb true)
    (let [proc       (.start pb)
          _          (try (.close (.getOutputStream proc)) (catch Exception _ nil))
          start-ms   (System/currentTimeMillis)
          reader     (BufferedReader. (InputStreamReader. (.getInputStream proc)))
          ;; Reader thread: emit lines until EOF
          read-thread
          (doto (Thread.
                  (fn []
                    (try
                      (loop []
                        (when-let [line (.readLine reader)]
                          (try (on-line line) (catch Exception _ nil))
                          (recur)))
                      (catch Exception e
                        (log/debug "[STREAM] reader exception" (.getMessage e)))
                      (finally
                        (try (.close reader) (catch Exception _ nil))))))
            (.setName (str "stream-reader-" (System/currentTimeMillis)))
            (.setDaemon true))
          ;; Waiter thread: wait for exit or timeout, then invoke on-done
          wait-thread
          (doto (Thread.
                  (fn []
                    (let [finished? (.waitFor proc timeout-ms TimeUnit/MILLISECONDS)
                          end-ms    (System/currentTimeMillis)]
                      (if finished?
                        (do (.join read-thread 2000) ;; let reader drain
                            (on-done {:exit-code   (.exitValue proc)
                                      :timed-out?  false
                                      :duration-ms (- end-ms start-ms)}))
                        (do (destroy-process-tree! proc)
                            (try (.close (.getInputStream proc)) (catch Exception _ nil))
                            (.join read-thread 1000)
                            (on-done {:exit-code   124
                                      :timed-out?  true
                                      :duration-ms (- end-ms start-ms)}))))))
            (.setName (str "stream-waiter-" (System/currentTimeMillis)))
            (.setDaemon true))]
      (.start read-thread)
      (.start wait-thread)
      proc)))

(defn parse-stream-json-line
  "Parse a single NDJSON line from `claude --output-format stream-json --include-partial-messages`.
   Event types:
     system       → {:session_id ...}
     stream_event → wraps Anthropic API events (content_block_delta, etc.)
     assistant    → full message (ignored — we already got deltas)
     result       → {:session_id ... :total_cost_usd ...}"
  [line]
  (let [mk-event (fn [kind payload]
                   (assoc payload
                          :kind kind
                          :event kind
                          :ts (System/currentTimeMillis)))]
    (try
      (let [obj (json/parse-string line true)]
        (case (:type obj)
          ;; system line carries session-id; model as :run-start envelope.
          "system"
          (mk-event :run-start {:session-id (:session_id obj)})

          "stream_event"
          (let [inner (:event obj)]
            (case (:type inner)
              "content_block_delta"
              (let [delta (:delta inner)
                    idx   (:index inner)]
                (cond
                  (= (:type delta) "text_delta")
                  (mk-event :text-delta {:text (:text delta)})

                  (= (:type delta) "input_json_delta")
                  (mk-event :tool-input-delta
                            {:block-idx idx
                             :json-chunk (:partial_json delta)})

                  (= (:type delta) "thinking_delta")
                  (mk-event :thinking-delta {:text (:thinking delta)})

                  (= (:type delta) "signature_delta")
                  nil  ;; verification signature — not needed for display

                  :else nil))

              "content_block_start"
              (let [block (:content_block inner)
                    idx   (:index inner)]
                (cond
                  (= (:type block) "tool_use")
                  (mk-event :tool-use-start
                            {:block-idx idx
                             :tool-id (:id block)
                             :tool-name (:name block)})

                  (= (:type block) "tool_result")
                  (mk-event :tool-result
                            {:block-idx idx
                             :tool-id (:tool_use_id block)
                             :content (:content block)})

                  (= (:type block) "thinking")
                  (mk-event :thinking-start {:block-idx idx})

                  :else nil))

              "content_block_stop"
              (mk-event :block-stop {:block-idx (:index inner)})

              ;; message_start, message_delta, message_stop — skip
              nil))

          ;; assistant — full message; fallback if --include-partial-messages wasn't passed
          "assistant"
          (let [text (->> (get-in obj [:message :content])
                          (filter #(= (:type %) "text"))
                          (map :text)
                          (str/join "\n"))]
            (when (seq text)
              (mk-event :text-delta {:text text})))

          "result"
          (mk-event :run-done
                    {:status :complete
                     :session-id (:session_id obj)
                     :cost-usd (:total_cost_usd obj)
                     :result (:result obj)})

          ;; Unknown types — skip
          nil))
      (catch Exception e
        (mk-event :run-error
                  {:error :invalid-json-line
                   :message (.getMessage e)
                   :raw (preview-str line)})))))

(defn initial-stream-state []
  {:tool-ids #{}
   :tool-by-block {}
   :terminal-kind nil
   :saw-run-start? false})

(defn apply-stream-invariants
  "Validate and enrich one parsed stream event.
   Returns [next-state maybe-emit-event]."
  [state evt]
  (let [mk-error (fn [payload]
                   [(assoc state :terminal-kind :run-error)
                    (assoc payload
                           :kind :run-error
                           :event :run-error
                           :ts (System/currentTimeMillis))])]
    (cond
      (:terminal-kind state)
      [state nil]

      (or (nil? (:kind evt)) (nil? (:ts evt)))
      (mk-error {:error :invalid-envelope
                 :details (dissoc evt :ts)})

      (= :run-start (:kind evt))
      [(assoc state :saw-run-start? true) evt]

      (= :tool-use-start (:kind evt))
      (let [tool-id (:tool-id evt)
            block-idx (:block-idx evt)]
        (if (or (nil? tool-id) (str/blank? (str tool-id)))
          (mk-error {:error :tool-use-missing-id
                     :block-idx block-idx})
          [(-> state
               (update :tool-ids conj tool-id)
               (assoc-in [:tool-by-block block-idx] tool-id))
           evt]))

      (= :tool-input-delta (:kind evt))
      (let [tool-id (or (:tool-id evt)
                        (get-in state [:tool-by-block (:block-idx evt)]))]
        (if (contains? (:tool-ids state) tool-id)
          [state (assoc evt :tool-id tool-id)]
          (mk-error {:error :unknown-tool-reference
                     :source-kind :tool-input-delta
                     :tool-id tool-id
                     :block-idx (:block-idx evt)})))

      (= :tool-result (:kind evt))
      (let [tool-id (:tool-id evt)]
        (if (contains? (:tool-ids state) tool-id)
          [state evt]
          (mk-error {:error :unknown-tool-reference
                     :source-kind :tool-result
                     :tool-id tool-id
                     :block-idx (:block-idx evt)})))

      (= :run-done (:kind evt))
      [(assoc state :terminal-kind :run-done) evt]

      (= :run-error (:kind evt))
      [(assoc state :terminal-kind :run-error) evt]

      :else
      [state evt])))

(defn parse-stream-json-lines
  "Parse + validate a sequence of raw NDJSON lines, returning normalized events."
  [lines]
  (let [{:keys [events state]}
        (reduce (fn [{:keys [events state]} line]
                  (if-let [evt (parse-stream-json-line line)]
                    (let [[state* emit] (apply-stream-invariants state evt)]
                      {:events (cond-> events emit (conj emit))
                       :state state*})
                    {:events events :state state}))
                {:events [] :state (initial-stream-state)}
                lines)]
    (if (:terminal-kind state)
      events
      (conj events
            {:kind :run-error
             :event :run-error
             :ts (System/currentTimeMillis)
             :error :missing-terminal-event}))))

(defn write-event!
  "Write one SSE event as `data: {edn}\\n\\n` and flush."
  [^java.io.Writer writer evt]
  (.write writer (str "data: " (pr-str evt) "\n\n"))
  (.flush writer))


;;; ── first-light A P2b · the episode turn (SSE over POST) ───────────────────
;;
;; The ground's send lane (CONTRACT §7 P2b addressing): Ctrl+Enter fires from
;; the FOCUSED block, whose content is ALREADY durable (birthed + committed-
;; echo edits — the P2 client-buffer mint is dead). What becomes durable HERE,
;; BEFORE the agent spawns, is the revision-pinned TURN RECORD: source-block-id
;; + the pinned content (text + kernel hash) + send-time position — later
;; edits or moves never rewrite what the resident answered. Event order:
;;   :episode-durable   — the turn record is acked (append+await), BEFORE the
;;                        agent is spawned (the durable-BEFORE-agent law,
;;                        unchanged)
;;   <claude stream events> — the resident agent's live turn (the existing CLI
;;                        lane; subscription auth, zero keys)
;;   :run-done/:run-error   — the turn closes honestly (timeout/failed named)
;;   :episode-distilled — post-turn harvest+distill receipt (T8; G4); the
;;                        ingest epoch bump makes the worn face re-pull TRUTH
;; The turn cell's status is overwritten :open → :complete/:failed/:timeout at
;; turn end; an abrupt JVM death leaves :open — the honest open fact (G4b).
;; A failed turn-record mint emits :run-error and never spawns the agent.

(defonce ^:private ambient-autotag-runtime
  ;; The llm-module remains an intent/observation lifecycle organ only; durable
  ;; record/proposal truth lands in the cluster-backed OC + relation runtimes.
  ;; Delay it so gold-pointed work pays zero annotation boot cost.
  (delay (llm/start-llm-runtime!)))

(defn run-ambient-autotag!
  "Best-effort ambient P4 silver lane. It runs off the response thread and
   cannot delay or prevent the already-durable resident turn. Identical input
   converges through material-circulation's OC record before any proposal edge."
  [{:keys [oc-rt rk-rt]}
   {:keys [object-key source-unit-id text receipt gold-receipt lines]}]
  ;; CONTRACT T6: the emission is unconditional. This handler alone owns the
  ;; old call-site guard, so sibling rows on the trigger cannot be suppressed.
  (if-not (and (nil? gold-receipt) rk-rt)
    {:status :skipped}
    (let [candidate-ids (->> (:receipt/visible-addresses receipt)
                             (remove #{source-unit-id})
                             distinct
                             vec)
          candidates
          (->> candidate-ids
               (keep
                (fn [unit-id]
                  (when-let [result (ocr/read-unit oc-rt unit-id)]
                    {:id unit-id :text (:content-text result)})))
               vec)]
      (when (seq candidates)
        (let [source-read (ocr/read-unit oc-rt source-unit-id)
              ;; CONTRACT T1: preserve the exact material-circulation argument
              ;; shapes; its derived durable identity bytes remain untouched.
              result
              (circulation/autotag-material!
               {:llm-rt @ambient-autotag-runtime
                :oc-rt oc-rt
                :rk-rt rk-rt}
               object-key
               (cond-> {:record-unit-id source-unit-id
                        :record-text text
                        :candidates candidates
                        :evidence-source-id (get-in source-read
                                                   [:unit :source-id])
                        :timeout-ms 120000}
                 lines (assoc :lines lines)))]
          (log/info "[CIRCULATION][AUTOTAG]"
                    {:source-unit-id source-unit-id
                     :candidates (count candidates)
                     :status (:status result)
                     :run-id (:run-id result)
                     :relation-id (get-in result [:edge :relation-id])})
          result)))))

(defn invocation-wear-for-source
  "Read the source block's actual invocation wear at the send seam.

   Shared active and source instance state retain their existing owners; this
   function only composes those reads through the one `wear-for-subject` law.
   Any read failure fails closed to the invocation code floor."
  [oc-rt source-unit-id]
  (try
    (let [compiled (facet-master/compiled-active
                    oc-rt invocation-material/spec)
          shared {:facet-master/id invocation-material/master-id
                  :facet-master/facet :invocation
                  :facet-master/grammar (:grammar compiled)
                  :facet-master/material (:material compiled)
                  :facet-master/active-revision-id (:revision-id compiled)}
          instance (material-truth/served-instance
                    oc-rt invocation-material/spec source-unit-id)]
      (facet-engine/wear-for-subject
       invocation-material/spec shared instance))
    (catch Exception e
      (log/warn e "[INVOCATION] source wear read failed; using code floor"
                {:source-unit-id source-unit-id})
      invocation-material/code-floor)))

(defn resident-portal-open
  "The actual Ctrl+Enter briefing authority.

   A registered matter-room conversation always wins over client-supplied
   portal coordinates and narrows to exactly its server-derived master. Every
   other conversation keeps P8's addressed-block narrowing unchanged."
  ([request-data source-unit-id conversation-id]
   (resident-portal-open
    request-data source-unit-id conversation-id invocation-material/code-floor))
  ([request-data source-unit-id conversation-id invocation-wear]
   (or
    (matter-room/narrowed-portal-open
     {:conversation-id conversation-id})
    (reply-to-block/narrowed-portal-open
     (assoc (or (:portal-open request-data) {})
            :entity-id source-unit-id
            :precontext (:invocation/precontext invocation-wear)
            :conversation-id
            (or conversation-id episode/genesis-conversation-id))))))

(defn run-episode-turn
  "POST /api/episode/utterance {:content-text :turn-id :time-ms, optional
   :source-unit-id/:position/:prev-turn-id} → SSE. turn-id + time-ms are
   CLIENT-minted once per Ctrl+Enter (the wear-id precedent) so an HTTP retry
   re-derives identical import identity and the journal no-ops — never a
   double turn. A source unit narrows source-specific wear/receipt work; it is
   not required for a top-level durable turn."
  [request-data]
  (let [text           (str (:content-text request-data))
        source-unit-id (:source-unit-id request-data)
        position       (:position request-data)
        turn-id        (str (:turn-id request-data))
        time-ms        (long (or (:time-ms request-data) (System/currentTimeMillis)))
        prev-turn-id   (:prev-turn-id request-data)
        cwd            (str (or (:cwd request-data) (System/getProperty "user.dir")))
        timeout-ms     (long (or (:timeout-ms request-data) 600000))
        scene-context  (:scene-context request-data)
        ;; drill seam (G3/G4/G4b): a machinery drill names its OWN episode so
        ;; the GENESIS first utterance stays Sid's act (§11). The ground client
        ;; never sends this; nil = the genesis episode.
        conv-id        (:conversation-id request-data)
        ;; one canvas, many conversations: :thread-id scopes the LANE while
        ;; conv-id keeps naming the canvas container where turn records land.
        ;; Absent = the genesis lane. The CLI SESSION serving the lane is the
        ;; lane's CURRENT EPISODE (D-core) — decided inside the body where the
        ;; OC runtime binds (current-episode!), never (or thread-id conv-id)
        ;; directly: lanes are permanent, sessions are bounded.
        thread-id      (some-> (:thread-id request-data) str not-empty)]
    {:status  200
     :headers {"Content-Type"      "text/event-stream"
               "Cache-Control"     "no-cache"
               "X-Accel-Buffering" "no"
               "Connection"        "keep-alive"}
     :body
     (reify ring-protocols/StreamableResponseBody
       (write-body-to-stream [_ _response output-stream]
         (let [writer (OutputStreamWriter. output-stream "UTF-8")]
           (try
             (let [{:keys [oc-rt rk-rt] :as face-ctx} (select-keys (cluster/face-projection-runtime) [:oc-rt :arsenal-rt :rk-rt])]
               (if (or (str/blank? text) (str/blank? turn-id) (nil? oc-rt))
                 (write-event! writer {:kind :run-error :event :run-error
                                       :ts (System/currentTimeMillis)
                                       :error (cond (nil? oc-rt) :land-unavailable
                                                    (str/blank? text) :empty-utterance
                                                    :else :missing-turn-id)})
                 (let [;; D-core: the lane's CURRENT episode decides the CLI
                       ;; session BEFORE anything durable lands — the turn
                       ;; cell carries :episode-id, the durable chain link.
                       ;; Total: a failed decision degrades to the lane's
                       ;; pre-chain behavior (its own file, resume-if-exists).
                       episode (try
                                 (episode/current-episode!
                                  oc-rt {:conversation-id conv-id
                                         :thread-id thread-id
                                         :now-ms time-ms :cwd cwd})
                                 (catch Exception _
                                   (let [lid (or thread-id conv-id
                                                 episode/genesis-conversation-id)]
                                     {:episode-id lid
                                      :fresh? (not (.exists ^java.io.File
                                                            (episode/episode-jsonl-file cwd lid)))
                                      :seed? false})))
                       episode-id (:episode-id episode)
                       durable (try
                                 (episode/record-turn!
                                  oc-rt {:turn-id turn-id
                                         :source-unit-id source-unit-id
                                         :content-text text
                                         :position position
                                         :status :open
                                         :time-ms time-ms :prev-turn-id prev-turn-id
                                         :conversation-id conv-id
                                         :thread-id thread-id
                                         :episode-id episode-id
                                         :scene-context scene-context})
                                 (catch Exception e
                                   {:status :error :error (.getMessage e)}))]
                   (if-not (= :accepted (:status durable))
                     (write-event! writer {:kind :run-error :event :run-error
                                           :ts (System/currentTimeMillis)
                                           :error :turn-not-durable
                                           :detail (dissoc durable :decision)})
                     (let [addressed? (not (str/blank? (str source-unit-id)))
                           birth-receipt
                           (when addressed?
                             (try
                               (episode/read-birth-receipt
                                oc-rt (:address durable) source-unit-id)
                               (catch Exception _ nil)))
                           gold-receipt
                           (when addressed?
                             (circulation/select-gold-receipt
                              source-unit-id (:receipt durable) birth-receipt))
                           gold
                           (when gold-receipt
                             (try
                               (circulation/bank-gold!
                                rk-rt
                                {:source-unit-id source-unit-id
                                 :target-unit-id
                                 (circulation/receipt-target gold-receipt)
                                 :receipt gold-receipt
                                 :asserted-at-ms time-ms})
                               (catch Exception e
                                 {:status :error :error (.getMessage e)})))
                           ;; P8 + matter-room P3: the server re-derives the
                           ;; narrowed open from durable/address authority.
                           ;; A room id names exactly one master; otherwise the
                           ;; durable target names exactly one addressed block.
                           invocation-wear
                           (if addressed?
                             (invocation-wear-for-source oc-rt source-unit-id)
                             invocation-material/code-floor)
                           portal-open
                           (resident-portal-open request-data source-unit-id
                                                 conv-id invocation-wear)
                           portal-briefing
                           (face-projection/portal-briefing
                            face-ctx portal-open)]
                       (if (and gold-receipt
                                (not= :materialized (:status gold)))
                         (write-event!
                          writer
                          {:kind :run-error :event :run-error
                           :ts (System/currentTimeMillis)
                           :error :wish-not-durable
                           :turn-id turn-id
                           :source-unit-id source-unit-id
                           :target-unit-id
                           (circulation/receipt-target gold-receipt)
                           :detail gold})
                         (do
                       (write-event! writer {:kind :episode-durable :event :episode-durable
                                             :ts (System/currentTimeMillis)
                                             :turn-id turn-id
                                             :source-unit-id source-unit-id
                                             :address (:address durable)
                                             :import-key (:import-key durable)
                                             :wish-relation-id (:relation-id gold)
                                             :wish-target-id (:target-unit-id gold)
                                             :portal-briefing-bytes
                                             (count (.getBytes
                                                     (or portal-briefing "")
                                                     "UTF-8"))
                                             :portal-master-ids
                                             (:master-ids portal-open)})
                       ;; Emit unconditionally after the durable turn. Autotag
                       ;; owns its gold/runtime decline; future rows on this
                       ;; trigger must still fire. Dispatch remains asynchronous,
                       ;; so the user's resident turn starts now.
                       (cascade/react!
                        {:oc-rt oc-rt :rk-rt rk-rt}
                        :episode/turn-durable
                        {:object-key (:address durable)
                         :source-unit-id source-unit-id
                         :text text
                         :receipt (:receipt durable)
                         :gold-receipt gold-receipt})
                       (let [!stream-state (atom (initial-stream-state))
                             done-promise  (promise)
                             ;; D-core seed: a successor episode's first prompt
                             ;; inherits the lane's prose thread (durable truth,
                             ;; never a jsonl replay). The seed rides ONLY the
                             ;; CLI prompt — the turn record above carries the
                             ;; RAW text, and flag-D skips the whole user event
                             ;; at distill, so seed material never re-enters
                             ;; the container as new blocks.
                             seed (when (:seed? episode)
                                    (face-projection/episode-seed
                                     (select-keys (cluster/face-projection-runtime) [:oc-rt :arsenal-rt :rk-rt])
                                     (episode/episode-object-key
                                      (or conv-id episode/genesis-conversation-id))
                                     thread-id))
                             argv (episode/summon-argv {:prompt
                                                        (reply-to-block/compose-resident-prompt
                                                         seed portal-briefing text
                                                         (:invocation/precontext
                                                          invocation-wear))
                                                        :session-id episode-id
                                                        :fresh? (:fresh? episode)
                                                        :model
                                                        (:invocation/model
                                                         invocation-wear)
                                                        :effort
                                                        (:invocation/effort
                                                         invocation-wear)})]
                         (episode/note-episode-turn! thread-id conv-id
                                                     episode-id time-ms)
                         ;; The dev classpath's SLF4J/logback versions can bind
                         ;; tools.logging to a silent provider.  G7 requires the
                         ;; argv receipt from the actual spawn seam, so keep the
                         ;; receipt on the process's observable stdout.
                         (println
                          "[EPISODE][TURN-START]"
                          (pr-str
                           {:turn-id turn-id :cwd cwd
                            :thread-id thread-id
                            :episode-id episode-id
                            :fresh? (:fresh? episode)
                            :invocation/revision-id
                            (:facet-master/revision-id invocation-wear)
                            :spawn/argv-options
                            ["--model"
                             (:invocation/model invocation-wear)
                             "--effort"
                             (:invocation/effort invocation-wear)]
                            :invocation/precontext
                            (:invocation/precontext invocation-wear)
                            :seed-chars (count (or seed ""))
                            :portal-briefing-bytes
                            (count (.getBytes
                                    (or portal-briefing "")
                                    "UTF-8"))
                            :portal-master-ids
                            (:master-ids portal-open)}))
                         (stream-cli-process
                          argv cwd timeout-ms
                          (fn [line]
                            (when-let [evt0 (parse-stream-json-line line)]
                              (let [[state* evt] (apply-stream-invariants @!stream-state evt0)]
                                (reset! !stream-state state*)
                                (when evt (write-event! writer evt)))))
                          (fn [{:keys [exit-code timed-out? duration-ms]}]
                            (let [status (cond timed-out? :timeout
                                               (zero? exit-code) :complete
                                               :else :failed)]
                              (when-not (:terminal-kind @!stream-state)
                                (let [terminal-evt (if (= status :complete)
                                                     {:kind :run-done :event :run-done
                                                      :ts (System/currentTimeMillis)
                                                      :status status :exit-code exit-code
                                                      :duration-ms duration-ms}
                                                     {:kind :run-error :event :run-error
                                                      :ts (System/currentTimeMillis)
                                                      :status status :exit-code exit-code
                                                      :duration-ms duration-ms
                                                      :error (if timed-out? :timeout :process-failed)})
                                      [state* emit] (apply-stream-invariants @!stream-state terminal-evt)]
                                  (reset! !stream-state state*)
                                  (when emit (write-event! writer emit))))
                              ;; the turn cell's status overwrite (:open →
                              ;; :complete/:failed/:timeout) — best-effort; a
                              ;; failure here leaves the honest :open fact
                              (try
                                (episode/record-turn!
                                 oc-rt {:turn-id turn-id
                                        :source-unit-id source-unit-id
                                        :content-text text
                                        :position position
                                        :status status
                                        :time-ms time-ms :prev-turn-id prev-turn-id
                                        :conversation-id conv-id
                                        :thread-id thread-id
                                        :episode-id episode-id
                                        :scene-context scene-context})
                                (catch Exception e
                                  (log/warn "[EPISODE][TURN-STATUS-FAILED]"
                                            {:turn-id turn-id :error (.getMessage e)})))
                              ;; post-turn distill runs on the waiter thread —
                              ;; the stream stays open until the receipt lands.
                              ;; It harvests the EPISODE's jsonl into the
                              ;; episode's own container (identity follows the
                              ;; line's sessionId by design) — the serve merge
                              ;; weaves it back into the lane.
                              (let [distill (try
                                              (episode/post-turn-distill!
                                               oc-rt {:cwd cwd :conversation-id episode-id})
                                              (catch Exception e
                                                {:status :distill-failed :error (.getMessage e)}))]
                                (log/info "[EPISODE][TURN-DISTILLED]"
                                          {:turn-id turn-id
                                           :status (:status distill)
                                           :river (:river distill)
                                           :native (:native distill)})
                                (write-event! writer
                                              {:kind :episode-distilled :event :episode-distilled
                                               :ts (System/currentTimeMillis)
                                               :status (:status distill)
                                               :river (:river distill)
                                               :native (:native distill)
                                               :debris (:debris distill)
                                               :error (:error distill)}))
                              (deliver done-promise true))))
                         @done-promise))))))))
             (catch Exception e
               (println "[EPISODE][STREAM-ERROR]" (.getMessage e)))
             (finally
               (try (.flush writer) (catch Exception _ nil))
               (try (.close writer) (catch Exception _ nil)))))))}))

;; =====================================================================
;; matter-room P2 — the room: birth once, refresh through the edit lane.
;;
;; A master's room is a REAL conversation container (CONTRACT L3): its machine
;; residents are ordinary durable blocks, born through the EXISTING episode
;; import path with the machine actor (G5 — no new import family, no adapter
;; namespace, no bespoke import composer) and refreshed through the EXISTING
;; `:object/edit` lane (`block-edit/submit-block-edit!`, the ONE server edit
;; entry point — a second write path here would be the T1 second-wearer tell).
;;
;; Birth is NOT part of the portal open: opening a master must stay a read.
;; This driver is the room ENTRY act, and it is idempotent by construction —
;; N opens converge on one row set (T2, G3).
;; =====================================================================

(defn matter-room-next-edit-seq
  "The next STRICTLY MONOTONE edit seq for one resident, from a DURABLE read
   of that unit's own edit-order row (never a content hash: `stale-edit?`
   rejects `(<= seq last-seq)` inside `edit-effects`, OUTSIDE
   `edit-request-validation-errors`, so a hash-derived seq would silently drop
   about half of all refreshes). Lineage key = the unit-id, constant per
   resident; client id = the room's own, so the only seq this competes with is
   the room's. Fallback 0 → first refresh 1."
  [oc-rt unit-id]
  (let [row (ocr/foreign-one (:edit-order-by-target oc-rt)
                             [(keypath unit-id)
                              (keypath matter-room/resident-edit-client-id)])]
    (inc (long (or (:edit-seq row) 0)))))

(defn matter-room-birth!
  "Land ONE resident through the parameterized episode import path: unit +
   birth-position in one acked import, machine actor, machine role."
  [oc-rt object-key resident]
  (let [request (episode/utterance-import-request
                 {:object-key object-key
                  :turn-id (:resident/turn-id resident)
                  :text (:resident/text resident)
                  :time-ms (long (:resident/time-ms resident))
                  :position (:resident/position resident)
                  :actor matter-room/resident-actor
                  :actor-id matter-room/resident-actor-id
                  :actor-role matter-room/resident-actor-role
                  :part-type matter-room/resident-part-type})]
    (ocr/append-object-container-request! oc-rt request)
    (let [decision (ocr/await-object-container-decision oc-rt request 20000)]
      {:act :birth
       :status (if (= :accepted (:status decision)) :accepted :rejected)
       :import-key (:import/key request)
       :reason (:reason decision)})))

(defn matter-room-refresh!
  "Land ONE resident's changed content as an EDIT on the same unit — the
   append-only trail residents never take this path (`:resident/refreshable?`
   false), because a new revision births a NEW resident instead."
  [oc-rt object-key unit-id unit resident]
  (let [seq* (matter-room-next-edit-seq oc-rt unit-id)
        request-id (str "req:matter-room:edit:" unit-id ":" seq*)
        result (block-edit/submit-block-edit!
                oc-rt
                {:request-id request-id
                 :edit-client-id matter-room/resident-edit-client-id
                 :edit-seq seq*
                 :actor matter-room/resident-actor
                 :time-ms (System/currentTimeMillis)
                 :target {:target/kind :derived-unit :target/id unit-id}
                 :payload {:document-container-id
                           (get-in unit [:unit :document-container-id])
                           :object-key object-key
                           :content-text (:resident/text resident)}})]
    {:act :refresh
     :status (if (:accepted? result) :accepted :rejected)
     :edit-seq seq*
     :replay? (:replay? result)
     :reason (:reason result)}))

(defn open-matter-room!
  "Open (and materialize) one master's room. Returns a plain map:
   {:status :ok/:error :master-id :room-id :object-key :entry :residents [...]}.

   Every resident is composed PURELY from the master-anchored portal
   projection, then reconciled against durable truth:
     absent            → birth once (episode import path)
     present, same     → nothing (no write, no revision noise)
     present, changed  → edit lane (refreshable residents only)
   A changed resident is NEVER re-imported: the kernel's import fingerprint is
   strict, so a re-import of changed content under the same import key is a
   durable conflict (PLAN §P2 F5)."
  [{:keys [oc-rt] :as face-ctx} {:keys [master-id]}]
  (if (or (nil? oc-rt) (not (string? master-id)) (str/blank? master-id))
    {:status :error
     :error (if (nil? oc-rt) :land-unavailable :bad-request)
     :master-id master-id}
    (let [room-id (matter-room/room-id master-id)
          object-key (episode/episode-object-key room-id)
          portal (face-projection/serve
                  face-ctx
                  {:face :material-portal
                   :params {:master-id master-id}})
          result (:portal/result portal)
          residents (matter-room/residents result)
          acts (mapv
                (fn [resident]
                  (let [turn-id (:resident/turn-id resident)
                        unit-id (episode/utterance-unit-id object-key turn-id 0)
                        unit (ocr/read-unit oc-rt unit-id)
                        base {:section (:resident/section resident)
                              :turn-id turn-id
                              :unit-id unit-id}]
                    (merge
                     base
                     (cond
                       (nil? unit)
                       (matter-room-birth! oc-rt object-key resident)

                       (= (str (:content-text unit)) (:resident/text resident))
                       {:act :unchanged :status :accepted}

                       (true? (:resident/refreshable? resident))
                       (matter-room-refresh! oc-rt object-key unit-id unit
                                             resident)

                       :else
                       {:act :append-only :status :accepted}))))
                residents)]
      {:status :ok
       :master-id master-id
       :room-id room-id
       :object-key object-key
       :entry (str "?drill=" room-id)
       ;; both honesty channels: the portal's per-section errors AND the
       ;; whole-open failure shape (`material-portal-projection`'s outer
       ;; catch swaps :portal/errors for :portal/error, and an empty room
       ;; must never look like a healthy one)
       :portal-errors (vec (:portal/errors result))
       :portal-error (:portal/error result)
       :residents acts})))

;; =====================================================================
;; matter-room P3 — the named ACT lane over existing P6 machinery.
;;
;; This namespace composes invocation only. The four durable functions below
;; call the existing owners verbatim; they build no ActionRequest and append no
;; depot directly. Preview intentionally has NO function or route here — it
;; remains `ground/preview-candidate!` on the client (L5/G7).
;; =====================================================================

(defn- prepare-matter-act
  [request]
  (let [request (or request {})]
    (assoc request
           :request-id (or (:request-id request)
                           (:request/id request)
                           (str (java.util.UUID/randomUUID)))
           :time-ms (or (:time-ms request) (System/currentTimeMillis))
           :actor (or (:actor request) matter-room/matter-actor))))

(defn- matter-error-card
  [verb error errors]
  {:card/kind :matter-act
   :card/status :error
   :card/verb verb
   :card/error error
   :card/errors (vec errors)})

(defn- invalid-matter-act
  ([verb error] (invalid-matter-act verb error []))
  ([verb error errors]
   {:status :error
    :verb verb
    :accepted? false
    :error error
    :errors (vec errors)
    :card (matter-error-card verb error errors)}))

(defn- completed-matter-act
  [verb branch result]
  (let [accepted? (true? (:accepted? result))
        errors (vec (:errors result))
        error (or (:reason result)
                  (when (seq errors) :matter/act-rejected))]
    (cond-> {:status (if accepted? :accepted :rejected)
             :verb verb
             :branch branch
             :accepted? accepted?
             :replay? (true? (:replay? result))
             :revision-id (:revision-id result)
             :decision-status (get-in result [:decision :status])
             :event (:event result)
             :errors errors}
      (not accepted?)
      (assoc :error error
             :card (matter-error-card verb error errors)))))

(defn matter-room-deviate!
  "Invoke `:matter/deviate` through one of its two existing P6 owners:
   instance deviation (`material-truth/deviate!`) or master candidate import
   (`facet-master/import-candidate!`)."
  [{:keys [oc-rt]} request]
  (let [act (matter-room/deviate-request (prepare-matter-act request))
        verb :matter/deviate
        spec (facet-masters/spec (:act/master-id act))]
    (cond
      (nil? oc-rt)
      (invalid-matter-act verb :land-unavailable)

      (not (:act/valid? act))
      (invalid-matter-act verb (:act/error act) (:act/errors act))

      (nil? spec)
      (invalid-matter-act verb :facet-master/unknown-master)

      (= :instance (:act/branch act))
      (completed-matter-act
       verb :instance
       (material-truth/deviate!
        oc-rt spec (:act/subject-uid act) (:act/overrides act)
        (:act/options act)))

      :else
      (completed-matter-act
       verb :master-candidate
       (facet-master/import-candidate!
        oc-rt spec (:act/source act) (:act/options act))))))

(defn matter-room-activate!
  "Activate one retained candidate through the existing P6 pointer act.
   `matter-room/activation-request` validates the closed event form before the
   owner receives it, so malformed act metadata cannot touch the pointer."
  [{:keys [oc-rt]} request]
  (let [act (matter-room/activation-request
             :activate (prepare-matter-act request))
        verb :matter/activate
        spec (facet-masters/spec (:act/master-id act))]
    (cond
      (nil? oc-rt)
      (invalid-matter-act verb :land-unavailable)

      (not (:act/valid? act))
      (invalid-matter-act verb (:act/error act) (:act/errors act))

      (nil? spec)
      (invalid-matter-act verb :facet-master/unknown-master)

      :else
      (completed-matter-act
       verb :master
       (facet-master/activate!
        oc-rt spec (:act/revision-id act) (:act/options act))))))

(defn matter-room-rollback!
  "Re-wear exactly one revision currently offered by the room's served
   `:portal/recovery` section. A stale/forged target fails before the existing
   P6 activation owner is invoked."
  [{:keys [oc-rt] :as face-ctx} request]
  (let [act (matter-room/activation-request
             :rollback (prepare-matter-act request))
        verb :matter/rollback
        spec (facet-masters/spec (:act/master-id act))]
    (cond
      (nil? oc-rt)
      (invalid-matter-act verb :land-unavailable)

      (not (:act/valid? act))
      (invalid-matter-act verb (:act/error act) (:act/errors act))

      (nil? spec)
      (invalid-matter-act verb :facet-master/unknown-master)

      :else
      (let [portal (:portal/result
                    (face-projection/serve
                     face-ctx
                     {:face :material-portal
                      :params {:master-id (:act/master-id act)}}))
            offer (matter-room/recovery-offer
                   portal (:act/master-id act) (:act/revision-id act))]
        (if-not offer
          (invalid-matter-act verb :matter/recovery-offer-not-found)
          (assoc
           (completed-matter-act
            verb :recovery-offer
            (facet-master/activate!
             oc-rt spec (:act/revision-id act) (:act/options act)))
           :recovery-offer offer))))))

(defn matter-room-say!
  "Halo P1 · H5 — import one immutable whole message, then assert its picked
   subject reference.

   Both existing runtimes are preflighted before the first append. The OC
   import is append+await+query first; only then may the relation wrapper append
   and await. Thus a lost response replays to one unit and one edge, an import
   fingerprint conflict cannot leak a relation, and an import-only partial
   attempt repairs on exact replay."
  [{:keys [oc-rt rk-rt]} request]
  (let [act (matter-room/say-request (or request {}))
        verb :matter/say]
    (cond
      (or (nil? oc-rt) (nil? rk-rt))
      (assoc (invalid-matter-act verb :land-unavailable)
             :runtime {:object-container (boolean oc-rt)
                       :relation-kernel (boolean rk-rt)})

      (not (:act/valid? act))
      (invalid-matter-act verb (:act/error act) (:act/errors act))

      :else
      (let [actor (:act/actor act)
            envelope-actor
            (update actor :actor/capabilities
                    (fn [capabilities]
                      (conj (set capabilities)
                            :object-container/import-material)))
            object-key (episode/episode-object-key (:act/room-id act))
            episode-request
            (episode/utterance-import-request
             {:object-key object-key
              :turn-id (:act/say-id act)
              :text (:act/text act)
              :time-ms (:act/time-ms act)
              :actor envelope-actor
              :actor-id (:actor/id actor)
              :actor-role (if (= :human (:actor/type actor))
                            "user"
                            "assistant")
              :part-type :human-message
              :scene-context
              {:receipt/picked-at
               {:address (:act/subject-uid act)}}})
            ;; The shared OC import fingerprint deliberately excludes
            ;; projection hints, while Halo's picked target lives in the
            ;; existing receipt hint. Strengthen only this composed request's
            ;; fingerprint with the immutable act body so target divergence is
            ;; an import conflict without changing episode.clj or creating a
            ;; second request builder.
            import-request
            (assoc
             episode-request
             :material/fingerprint
             (rama-core/sha-256
              (pr-str
               {:halo/say-version 0
                :episode/fingerprint
                (:material/fingerprint episode-request)
                :master-id (:act/master-id act)
                :subject-uid (:act/subject-uid act)
                :text (:act/text act)
                :say-id (:act/say-id act)
                :time-ms (:act/time-ms act)
                :actor actor})))
            ;; T5 falsifier: a depot-level duplicate request may hand back the
            ;; prior decision before the topology evaluates the incoming
            ;; fingerprint. Read that durable decision first and apply the
            ;; owner's existing conflict predicate; divergent reuse must not
            ;; reach either append.
            prior-decision (ocr/read-decision oc-rt import-request)]
        (if (oc/material-fingerprint-conflict?
             import-request prior-decision)
          {:status :rejected
           :verb verb
           :accepted? false
           :error :idempotency/material-fingerprint-conflict
           :errors [(oc/material-fingerprint-conflict-error
                     import-request prior-decision)]
           :decision prior-decision
           :relation-appended? false
           :card
           (matter-error-card
            verb :idempotency/material-fingerprint-conflict
            [(oc/material-fingerprint-conflict-error
              import-request prior-decision)])}
          (do
            (ocr/append-object-container-request! oc-rt import-request)
            (let [decision
                  (ocr/await-object-container-decision
                   oc-rt import-request 20000)]
              (if-not (= :accepted (:status decision))
            {:status :rejected
             :verb verb
             :accepted? false
             :error (or (:reason decision) :matter/say-import-rejected)
             :decision decision
             :relation-appended? false
             :card
             (matter-error-card
              verb
              (or (:reason decision) :matter/say-import-rejected)
              [])}
            (let [root
                  (first (get-in import-request
                                 [:payload :derived-units]))
                  root-id (:unit-id root)
                  durable-root (ocr/read-unit oc-rt root-id)]
              (if-not durable-root
                {:status :incomplete
                 :verb verb
                 :accepted? false
                 :error :matter/say-unit-not-queryable
                 :decision decision
                 :unit-id root-id
                 :relation-appended? false
                 :card
                 (matter-error-card
                  verb :matter/say-unit-not-queryable [])}
                (let [reference
                      (circulation/bank-reference!
                       rk-rt
                       {:source-unit-id root-id
                        :target-unit-id (:act/subject-uid act)
                        :actor actor
                        :asserted-at-ms (:act/time-ms act)
                        :evidence-source-id
                        (or (:source-id root)
                            (:source-artifact-id root)
                            root-id)
                        :evidence-anchor-id (:act/subject-uid act)
                        :say-id (:act/say-id act)
                        :master-id (:act/master-id act)})
                      completed? (= :materialized (:status reference))]
                  (cond-> {:status (if completed?
                                    :accepted
                                    :incomplete)
                           :verb verb
                           :accepted? completed?
                           :replay? (true? (:replay? decision))
                           :object-key object-key
                           :room-id (:act/room-id act)
                           :unit-id root-id
                           :import-key (:import/key import-request)
                           :decision decision
                           :relation reference
                           :relation-appended? true}
                    (not completed?)
                    (assoc
                     :error :matter/say-reference-not-queryable
                     :card
                     (matter-error-card
                      verb :matter/say-reference-not-queryable
                      []))))))))))))))

(defn- matter-act-http-status
  [result]
  (case (:error result)
    :land-unavailable 503
    nil (if (= :rejected (:status result)) 422 200)
    400))

(defn- record-free-edn
  "Make a composed result readable by the browser's data-only EDN reader.

   Rama decisions may contain internal records such as ActorRow. Those records
   are useful on the JVM but their tagged print form is not part of the HTTP
   protocol. Preserve every field while erasing only the record constructor."
  [value]
  (walk/postwalk
   (fn [x]
     (if (record? x) (into {} x) x))
   value))

;; =====================================================================
;; Relation /assert write shim — git-spine WP2 component W (CONTRACT §3.E).
;;
;; POST /api/relation/assert is the land's first write affordance (D-008): a CLI
;; agent asserts a RelationEdge via `curl`. The route validates route-side, does
;; WRITE-AHEAD durability (one plain-map line, flushed) BEFORE appending the
;; envelope to the running trail runtime's relation depot, and returns
;; {relation-id request-id}. Boot replay of that log is P1's job (git_spine.clj);
;; the two components meet ONLY at the file format + path, so this file NEVER
;; requires git_spine (SF-R2.1) — it derives the shared path from a local
;; constant, identically to P1.
;; =====================================================================

(defn edn-response
  "EDN response map with an explicit HTTP status (mirrors json-response's
   application/edn content-type). Real 4xx/5xx status codes, unlike the legacy
   always-200 json-response helper."
  ([data] (edn-response 200 data))
  ([status data]
   {:status  status
    :headers {"Content-Type" "application/edn"}
    :body    (pr-str data)}))

(def assert-log-relative-path
  "Repo-root-relative path of the shared relation-assert write-ahead log
   (CONTRACT §3.C/§3.E, pinned by SF-R2.1). This route (PW) and the boot
   replayer (P1) each derive the identical absolute path from this constant with
   ZERO shared code and no dependency on the git_spine namespace."
  "data/relation-assert-log.ednl")

(defn default-assert-log-path
  "Absolute assert-log path anchored at the repo root (user.dir), matching
   file_viewer's boot-cfg root idiom. Computed at request time only; the parent
   dir is created at runtime by the writer, never pre-created in the tree."
  []
  (str (System/getProperty "user.dir") "/" assert-log-relative-path))

(def relation-target-kind-allowlist
  "Route-side target-kind allowlist (CONTRACT §3.E / validator A4): the kernel
   exports no target-kind validator and ->target-ref never throws, so the route
   guards the joinable kinds explicitly. :git-commit is deliberately ABSENT
   (gate review 2026-07-05): its target-key derives as the BARE sha
   (relation_kernel ->target-ref), which can never join the RENDERED commit
   object (keyed on its oc:doc container id) — the exact non-join CONTRACT
   v1.1 removed from the import path (§2.3). Assert about a commit as
   :container + the commit's oc:doc:<key> id, which View-3 prints."
  #{:container :source :doc-file :conversation :derived-unit :kind})

(defn validate-assert-params
  "nil when `params` is a valid assert; else {:reason <string>} naming the first
   failure. Guards (CONTRACT §3.E): kind ∈ rk/relation-kinds; from/to kinds ∈ the
   route-side target allowlist; from/to ids are non-blank strings; asserter-id a
   non-blank string and asserter-type a keyword (gate review 2026-07-05: the
   depot rejects a nil :actor/id AFTER the route would have 200'd — the route
   must never 200 a request it can know the depot will reject, nor write-ahead
   a poison line that re-fails on every boot replay)."
  [{:keys [kind from-kind from-id to-kind to-id asserter-id asserter-type]}]
  (cond
    (not (contains? rk/relation-kinds kind))
    {:reason (str "unknown relation kind " (pr-str kind)
                  "; expected one of " (pr-str (vec (sort rk/relation-kinds))))}

    (not (contains? relation-target-kind-allowlist from-kind))
    {:reason (str "from-kind " (pr-str from-kind) " not in allowlist "
                  (pr-str (vec (sort relation-target-kind-allowlist))))}

    (not (contains? relation-target-kind-allowlist to-kind))
    {:reason (str "to-kind " (pr-str to-kind) " not in allowlist "
                  (pr-str (vec (sort relation-target-kind-allowlist))))}

    (not (rk/present-string? from-id))
    {:reason "from-id must be a non-blank string"}

    (not (rk/present-string? to-id))
    {:reason "to-id must be a non-blank string"}

    (not (rk/present-string? asserter-id))
    {:reason "asserter-id must be a non-blank string (custody: who asserts this?)"}

    (not (keyword? asserter-type))
    {:reason "asserter-type must be a keyword, e.g. :human :agent :import"}

    :else nil))

(defn envelope->plain-map
  "Convert an assert envelope to a pure-EDN plain map for the write-ahead log
   (CONTRACT §3.C): the :payload record and its nested :from/:to target-ref
   records become plain maps (`into {}`, recursive). Every other envelope value is
   already EDN, so the line pr-str's tag-free and round-trips through
   clojure.edn/read-string."
  [envelope]
  (let [ref->map (fn [ref] (when ref (into {} ref)))]
    (update envelope :payload
            (fn [payload]
              (-> (into {} payload)
                  (update :from ref->map)
                  (update :to ref->map))))))

(def ^:private assert-log-lock
  "Serializes assert-log appends so concurrent /assert POSTs (multiple curl
   agents, D-008) cannot interleave bytes and corrupt a line — every line must
   round-trip through clojure.edn/read-string on replay. In-process monitor; the
   endpoint is low-frequency so contention is negligible."
  (Object.))

(defn append-assert-log-line!
  "Write-ahead durability (CONTRACT §3.E): append ONE pr-str'd plain-map line to
   the assert-log, creating the parent dir at runtime (never pre-created in the
   tree) and flushing per append. `request` is a relation envelope.

   *print-namespace-maps* is bound false so the :actor submap prints as explicit
   {:actor/id .. :actor/type ..} rather than the #:actor{..} namespace-map
   shorthand — the line then contains NO '#' at all (no ambiguity vs reader
   tags), and clojure.edn/read-string reads both forms identically, so P1's
   replayer is unaffected."
  [log-path request]
  (let [f    (io/file log-path)
        ;; pure work off the lock: build the whole line first
        line (binding [*print-namespace-maps* false]
               (pr-str (envelope->plain-map request)))]
    (io/make-parents f)
    (locking assert-log-lock
      ;; UTF-8 pinned on BOTH sides of the seam (replay reader pins it too):
      ;; the JVM default charset flipped at Java 18 (JEP 400), and a non-ASCII
      ;; :note must replay identically on a host with a different file.encoding.
      (with-open [w (io/writer f :append true :encoding "UTF-8")]
        (.write w line)
        (.write w "\n")
        (.flush w)))))

(defn assert-relation-handler
  "git-spine WP2 component W core. Validates route-side, then on success does
   WRITE-AHEAD (one durable plain-map line) BEFORE appending the envelope to the
   trail runtime's relation depot; returns a ring response. Decoupled from HTTP
   routing + runtime resolution so tests (and the final-phase G8 pair test) drive
   it directly with a test runtime + tmp log path.

   opts   {:runtime <relation runtime handle> :log-path <assert-log path string>}
   params {:kind :from-kind :from-id :to-kind :to-id :note :asserter-id
           :asserter-type + optional :idempotency-key}

   Idempotency (gate review 2026-07-05, trap-4 parity with the import path):
   the default request-id/idempotency-key is the DETERMINISTIC
   \"assert:<relation-id>\" — a curl retry after a timeout re-sends the same
   key, the journal drops the replay, and NO duplicate decision/event/activity
   rows accrue. An optional :idempotency-key param overrides it for the
   deliberate re-assert case (e.g. after a retract, or to update :note), where
   the journal must NOT drop the request.

   Valid   → write log line, append to depot, 200 {:ok true :relation-id :request-id}.
   Invalid → 400 {:ok false :reason ...}; NO file write, NO depot append."
  [{:keys [runtime log-path]} params]
  (if-let [{:keys [reason]} (validate-assert-params params)]
    (edn-response 400 {:ok false :error :invalid-request :reason reason})
    (let [{:keys [kind from-kind from-id to-kind to-id note asserter-id asserter-type]} params
          from-ref    (rk/->target-ref from-kind from-id)
          to-ref      (rk/->target-ref to-kind to-id)
          relation-id (rk/relation-id-for kind from-ref to-ref asserter-id)
          ;; blank/non-string overrides fall through to the default — a blank
          ;; key would be depot-rejected AFTER the 200 (the poison-line class)
          request-id  (if (rk/present-string? (:idempotency-key params))
                        (:idempotency-key params)
                        (str "assert:" relation-id))
          request     (rk/assert-request
                       {:kind kind :from from-ref :to to-ref
                        :asserter-actor-id asserter-id
                        :asserter-type asserter-type
                        :asserted-at-ms (System/currentTimeMillis)
                        :request-id request-id
                        :idempotency-key request-id
                        :note note})]
      ;; write-ahead FIRST: if the log write throws, the depot is never touched.
      ;; durable-ground P4: a nil log-path means the WAL is OFF (cluster mode —
      ;; the depot append below IS the durable log); the depot ack replaces the
      ;; write-ahead line, not the other way around.
      (when log-path
        (append-assert-log-line! log-path request))
      (rk/append-relation-request! runtime request)
      (edn-response 200 {:ok true
                         :relation-id (rk/relreq-routing-key request)
                         :request-id request-id}))))

(defn resolve-trail-runtime-or-503
  "Return an availability tuple for the external-cluster TrailView runtime."
  [runtime]
  (if (some? runtime)
    [:ok runtime]
    [:unavailable "trail runtime not booted"]))

(defn handle-assert-route
  "Route composition for POST /api/relation/assert over a runtime-or-nil."
  [ring-req runtime-or-nil log-path]
  (let [[status runtime] (resolve-trail-runtime-or-503 runtime-or-nil)]
    (if (= status :ok)
      (assert-relation-handler {:runtime runtime :log-path log-path}
                               (parse-edn-body ring-req))
      (edn-response 503 {:ok false :error :runtime-unavailable
                         :message "trail runtime not booted"}))))

(defn wrap-file-api
  "Handle /api/* routes for file explorer sidebar.
   Returns EDN responses consumable by ClojureScript client."
  [next-handler]
  (fn [{:keys [uri query-params request-method] :as ring-req}]
    (cond
      ;; ===== Relation assert (/assert write shim — git-spine WP2 component W) =====
      (= uri "/api/relation/assert")
      (if (= request-method :post)
        (try
          (handle-assert-route ring-req (cluster/trail-runtime)
                               ;; durable-ground P4: cluster mode writes no
                               ;; assert WAL — the relation depot is the log.
                               (when-not (cluster/cluster-boot?)
                                 (default-assert-log-path)))
          (catch Exception e
            (log/error e "[RELATION][ASSERT][ERROR]" {:uri uri})
            (edn-response 500 {:ok false :error :server-error
                               :message (str "assert failed: " (.getMessage e))})))
        (edn-response 405 {:ok false :error :method-not-allowed
                           :message "Method not allowed. Use POST."}))

      ;; editable-material — one deterministic malformed-candidate drill for
      ;; any registered master. Validation closes before active-pointer edit;
      ;; rendering still waits for the one batched FacePull.
      ;; ===== matter-room P2 · room entry (open = materialize + address) =====
      ;; The DELIBERATE MVP (PLAN §P2 F15): entry is this act followed by a URL
      ;; change to the returned `?drill=<room-uuid>` lane — the existing UUID
      ;; conversation lane, not a new one, and not yet an in-land gesture.
      (= uri "/api/matter-room/open")
      (if (= request-method :post)
        (try
          (let [{:keys [master-id]} (parse-edn-body ring-req)
                result (open-matter-room! (select-keys (cluster/face-projection-runtime) [:oc-rt :arsenal-rt :rk-rt]) {:master-id master-id})]
            (edn-response (case (:error result)
                            nil 200
                            :land-unavailable 503
                            400)
                          result))
          (catch Exception e
            (log/error e "[MATTER-ROOM][OPEN-ERROR]" {:uri uri})
            (edn-response 500 {:status :error :error (.getMessage e)})))
        (edn-response 405 {:status :error :error :method-not-allowed}))

      ;; ===== matter-room P3 · named durable ACT endpoints (L5/G7) =====
      ;; These are the complete server invocation surface. Preview is absent
      ;; by law: window.__portal.preview delegates to the existing client
      ;; membrane and never reaches Jetty.
      (= uri "/api/matter-room/deviate")
      (if (= request-method :post)
        (try
          (let [result (matter-room-deviate!
                        (select-keys (cluster/face-projection-runtime) [:oc-rt :arsenal-rt :rk-rt]) (parse-edn-body ring-req))]
            (edn-response (matter-act-http-status result) result))
          (catch Exception e
            (log/error e "[MATTER-ROOM][DEVIATE-ERROR]" {:uri uri})
            (edn-response
             500 (invalid-matter-act :matter/deviate :matter/server-error
                                     [{:message (.getMessage e)}]))))
        (edn-response 405 {:status :error :error :method-not-allowed}))

      (= uri "/api/matter-room/activate")
      (if (= request-method :post)
        (try
          (let [result (matter-room-activate!
                        (select-keys (cluster/face-projection-runtime) [:oc-rt :arsenal-rt :rk-rt]) (parse-edn-body ring-req))]
            (edn-response (matter-act-http-status result) result))
          (catch Exception e
            (log/error e "[MATTER-ROOM][ACTIVATE-ERROR]" {:uri uri})
            (edn-response
             500 (invalid-matter-act :matter/activate :matter/server-error
                                     [{:message (.getMessage e)}]))))
        (edn-response 405 {:status :error :error :method-not-allowed}))

      (= uri "/api/matter-room/rollback")
      (if (= request-method :post)
        (try
          (let [result (matter-room-rollback!
                        (select-keys (cluster/face-projection-runtime) [:oc-rt :arsenal-rt :rk-rt]) (parse-edn-body ring-req))]
            (edn-response (matter-act-http-status result) result))
          (catch Exception e
            (log/error e "[MATTER-ROOM][ROLLBACK-ERROR]" {:uri uri})
            (edn-response
             500 (invalid-matter-act :matter/rollback :matter/server-error
                                     [{:message (.getMessage e)}]))))
        (edn-response 405 {:status :error :error :method-not-allowed}))

      (= uri "/api/matter-room/say")
      (if (= request-method :post)
        (try
          (let [result (matter-room-say!
                        (select-keys (cluster/face-projection-runtime) [:oc-rt :arsenal-rt :rk-rt]) (parse-edn-body ring-req))]
            (edn-response (matter-act-http-status result)
                          (record-free-edn result)))
          (catch Exception e
            (log/error e "[MATTER-ROOM][SAY-ERROR]" {:uri uri})
            (edn-response
             500 (invalid-matter-act :matter/say :matter/server-error
                                     [{:message (.getMessage e)}]))))
        (edn-response 405 {:status :error :error :method-not-allowed}))

      (= uri "/api/material/facet-master/drill")
      (if (= request-method :post)
        (try
          (let [oc-rt (:oc-rt (select-keys (cluster/face-projection-runtime) [:oc-rt :arsenal-rt :rk-rt]))
                {:keys [drill-id master-id]} (parse-edn-body ring-req)
                spec (facet-masters/spec master-id)]
            (cond
              (nil? oc-rt)
              (edn-response 503 {:status :error :error :land-unavailable})

              (or (str/blank? (str drill-id))
                  (nil? spec))
              (edn-response 400 {:status :error :error :bad-request})

              :else
              (let [_ (facet-master/ensure-master! oc-rt spec)
                    result
                    (facet-master/malformed-drill!
                     oc-rt spec drill-id)]
                (edn-response
                 200
                 {:status :rejected
                  :master-id master-id
                  :candidate-revision-id (:candidate-revision-id result)
                  :activation-errors (:activation-errors result)
                  :active-revision-id
                  (some-> result :state :active-revision :revision-id)
                  :trace :candidate-import-retained}))))
          (catch Exception e
            (log/error e "[MATERIAL][DRILL-ERROR]" {:uri uri})
            (edn-response 500 {:status :error :error (.getMessage e)})))
        (edn-response 405 {:error "Method not allowed. Use POST."}))

      ;; first-light A P2 — the ground's one lane (T9: Ctrl+Enter lands here)
      (= uri "/api/episode/utterance")
      (if (= request-method :post)
        (try
          (let [request-data (parse-edn-body ring-req)]
            (log/info "[EPISODE][HTTP-IN]"
                      {:remote-addr (:remote-addr ring-req)
                       :turn-id (:turn-id request-data)
                       :chars (count (str (:text request-data)))})
            (run-episode-turn request-data))
          (catch Exception e
            (log/error e "[EPISODE][HTTP-ERROR]"
                       {:remote-addr (:remote-addr ring-req) :uri uri})
            (json-response {:error (str "Episode turn failed: " (.getMessage e))})))
        (json-response {:error "Method not allowed. Use POST."}))

      ;; first-light A P2b — block birth: the FIRST content act mints the
      ;; durable block at the chosen point (unit + birth-position geometry
      ;; cell in ONE acked import — §5.1 lane; block-id + time-ms are
      ;; CLIENT-minted once, so retries converge). Escape before content
      ;; never reaches here — no unit is ever minted for an abandoned anchor.
      (= uri "/api/episode/block-birth")
      (if (= request-method :post)
        (try
          (let [{:keys [block-id text time-ms position conversation-id
                        scene-context]}
                (parse-edn-body ring-req)
                oc-rt (:oc-rt (select-keys (cluster/face-projection-runtime) [:oc-rt :arsenal-rt :rk-rt]))]
            (if (or (nil? oc-rt) (str/blank? (str block-id)) (nil? text))
              (edn-response 400 {:status :rejected
                                 :error (if (nil? oc-rt) :land-unavailable :bad-request)})
              (let [r (episode/append-utterance!
                       oc-rt {:text (str text) :turn-id (str block-id)
                              :time-ms (long (or time-ms (System/currentTimeMillis)))
                              :position position
                              :scene-context scene-context
                              :conversation-id conversation-id})]
                (when (= :accepted (:status r))
                  ;; the mint IS an ingest (INV-19) — the face re-pull is the
                  ;; committed-echo cross-check channel
                  (swap! ingest-epoch/!ingest-epoch-atom inc))
                (edn-response (if (= :accepted (:status r)) 200 409)
                              (dissoc r :decision)))))
          (catch Exception e
            (log/error e "[EPISODE][BIRTH-ERROR]" {:uri uri})
            (edn-response 500 {:status :error :error (.getMessage e)})))
        (edn-response 405 {:error "Method not allowed. Use POST."}))

      ;; first-light A P2b — the geometry settle write: camera + block
      ;; positions as SETTLE-STATE truth (one acked write per gesture burst,
      ;; never per-event; the ack IS the safety mechanism). Hint-only import,
      ;; settled cells (P2B.md receipt a). No epoch bump for position/camera
      ;; cells (they change no served block material) — but a TOMBSTONE cell
      ;; (Task 18 delete) does, so those settles bump the epoch below.
      (= uri "/api/episode/geometry")
      (if (= request-method :post)
        (try
          (let [{:keys [cells camera settle-id time-ms conversation-id]}
                (parse-edn-body ring-req)
                oc-rt (:oc-rt (select-keys (cluster/face-projection-runtime) [:oc-rt :arsenal-rt :rk-rt]))]
            (if (or (nil? oc-rt) (str/blank? (str settle-id))
                    (and (empty? cells) (nil? camera)))
              (edn-response 400 {:status :rejected
                                 :error (if (nil? oc-rt) :land-unavailable :bad-request)})
              (let [r (episode/settle-geometry!
                       oc-rt {:cells (vec cells) :camera camera
                              :settle-id (str settle-id)
                              :time-ms (long (or time-ms (System/currentTimeMillis)))
                              :conversation-id conversation-id})]
                ;; Task 18: a tombstone settle changes served block material —
                ;; bump the epoch so faces re-pull without waiting for the
                ;; next content act
                (when (and (= :accepted (:status r)) (some :deleted? cells))
                  (swap! ingest-epoch/!ingest-epoch-atom inc))
                (edn-response (if (= :accepted (:status r)) 200 409)
                              (dissoc r :decision)))))
          (catch Exception e
            (log/error e "[EPISODE][GEOMETRY-ERROR]" {:uri uri})
            (edn-response 500 {:status :error :error (.getMessage e)})))
        (edn-response 405 {:error "Method not allowed. Use POST."}))

      :else
      ;; Not an API route — pass through
      (next-handler ring-req))))

(defn http-middleware []
  ;; these compose as functions, so are applied bottom up
  (-> not-found-handler
    (wrap-content-type)
    (wrap-params)
    ;; Product EDN routes own their raw request bodies. Keep them outside
    ;; wrap-params: curl -d defaults to form encoding, whose parser otherwise
    ;; drains the stream before parse-edn-body can read it.
    (wrap-file-api)))

(defn- add-gzip-handler!
  "Makes Jetty server compress responses. Optional but recommended."
  [server]
  (.setHandler server
    (doto (GzipHandler.)
      #_(.setIncludedMimeTypes (into-array ["text/css" "text/plain" "text/javascript" "application/javascript" "application/json" "image/svg+xml"])) ; only compress these
      (.setMinGzipSize 1024)
      (.setHandler (.getHandler server)))))

(defn start-server! [{:keys [port host]
                      :or   {port 8080, host "0.0.0.0"}
                      :as   config}]
  (let [server     (ring/run-jetty (http-middleware)
                     (merge {:port         port
                             :join?        false
                             :configurator (fn [server]
                                             (add-gzip-handler! server))}
                       config))]
    (log/info "👉" (str "http://" host ":" (-> server (.getConnectors) first (.getPort))))
    server))
