(ns app.server.rama.dogfood.llm
  (:use [com.rpl.rama]
        [com.rpl.rama.path]
        [com.rpl.rama.ops])
  (:require [app.server.rama.core :as kernel]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [com.rpl.rama.test :refer [create-ipc launch-module!]])
  (:import (java.io BufferedReader File InputStreamReader OutputStreamWriter)
           (java.util UUID)
           (java.util.concurrent TimeUnit)))

(def schema-version 1)
(def pending-task-id "local")
(def default-error-limit 50)
(def default-claude-timeout-ms 120000)

(def llm-backends
  #{:codex :claude})

(def claude-auth-modes
  #{:subscription :api-key})

(def observation-types
  #{:codex/item-completed
    :codex/approval-request
    :codex/token-usage
    :codex/tool-call
    :codex/patch-proposal
    :codex/run-finished
    :codex/run-failed
    :claude/system-init
    :claude/stream-delta
    :claude/message
    :claude/user-message
    :claude/tool-call
    :claude/tool-result
    :claude/hook-event
    :claude/control-response
    :claude/rate-limit
    :claude/auth
    :claude/api-retry
    :claude/compact-boundary
    :claude/source-boundary
    :claude/tombstone
    :claude/result
    :claude/run-failed})

(def terminal-statuses
  #{:succeeded :failed :cancelled})

(def control-types
  #{:approval/resolve
    :turn/cancel
    :compact/request
    :turn/steer})

(def terminal-approval-decisions
  #{:denied :declined :rejected :expired :timeout})

(def forbidden-payload-execution-option-keys
  #{:model :approval-policy :sandbox :cwd :execution/options})

(def sensitive-env-keys
  #{"CLAUDE_CODE_OAUTH_TOKEN"
    "ANTHROPIC_AUTH_TOKEN"
    "ANTHROPIC_API_KEY"
    "OPENAI_API_KEY"
    "GEMINI_API_KEY"
    "GOOGLE_API_KEY"
    "GOOGLE_GENERATIVE_AI_API_KEY"
    "AZURE_OPENAI_API_KEY"
    "AWS_ACCESS_KEY_ID"
    "AWS_SECRET_ACCESS_KEY"
    "AWS_SESSION_TOKEN"})

(def sensitive-key-names
  #{"api_key" "apikey" "api-key" "token" "auth_token" "auth-token"
    "oauth_token" "oauth-token" "password" "secret" "authorization"})

(defn now-ms [] (kernel/now-ms))
(defn random-id [prefix] (kernel/random-id prefix))

(defn llm-routing-key
  [run-id]
  [:llm-run run-id])

(defn normalize-task-id
  [task-id]
  (when (some? task-id)
    (str task-id)))

(defn blank-string?
  [x]
  (or (not (string? x)) (str/blank? x)))

(defn opts-executor-task-id
  [opts]
  (normalize-task-id
    (or (:executor-task-id opts)
        (:executor/task-id opts))))

(defn opts-backend
  [opts]
  (or (:llm/backend opts)
      (:backend opts)
      (:agent-kind opts)
      (get-in opts [:executor :agent/kind])
      :codex))

(defn normalize-backend
  [backend]
  (or backend :codex))

(defn normalize-auth-mode
  [backend auth-mode]
  (cond
    (some? auth-mode) auth-mode
    (= :claude backend) :subscription
    :else nil))

(defn request-executor-task-id
  [request]
  (normalize-task-id
    (or (get-in request [:payload :executor/task-id])
        (get-in request [:executor :executor/task-id]))))

(defn request-backend
  [request]
  (normalize-backend
    (or (:llm/backend request)
        (get-in request [:executor :agent/kind]))))

(defn request-auth-mode
  [request]
  (normalize-auth-mode (request-backend request) (:llm/auth-mode request)))

(defn normalize-turn-run-request
  [request]
  (if (map? request)
    (let [backend (request-backend request)
          auth-mode (request-auth-mode request)]
      (cond-> (assoc request
                     :llm/backend backend
                     :executor (assoc (:executor request)
                                      :agent/kind backend))
        auth-mode
        (assoc :llm/auth-mode auth-mode)))
    request))

(defn turn-run-request
  "Build the LLMTopology input record. In the full system WorldTopology is the
   only writer of this value to *llm-depot after it has accepted a WorldTurn and
   frozen the ContextBundle."
  [world-thread-id world-turn-id context-bundle-id & [opts]]
  (let [run-id (or (:llm-turn-run-id opts) (:llm-turn-run/id opts) (random-id "llm-run"))
        llm-thread-id (or (:llm-thread-id opts) (:llm-thread/id opts) (random-id "llm-thread"))
        request-id (or (:request-id opts) (:request/id opts) (random-id "llm-req"))
        time-ms (or (:time-ms opts) (now-ms))
        backend (normalize-backend (opts-backend opts))
        auth-mode (normalize-auth-mode backend (or (:llm/auth-mode opts)
                                                   (:auth-mode opts)))
        default-executor-pool (case backend
                                :claude :local-claude
                                :local-codex)
        executor-task-id (opts-executor-task-id opts)]
    (cond-> {:request/id request-id
             :request/type :llm/turn-run-request
             :request/schema-version schema-version
             :request/time-ms time-ms
             :idempotency/key (or (:idempotency-key opts)
                                  (:idempotency/key opts)
                                  (str "world-event:" world-turn-id ":" context-bundle-id))
             :routing/key (llm-routing-key run-id)
             :world-thread/id world-thread-id
             :world-turn/id world-turn-id
             :context-bundle/id context-bundle-id
             :llm-thread/id llm-thread-id
             :llm-turn-run/id run-id
             :llm/backend backend
             :executor {:agent/kind backend
                        :native/thread-id (:native/thread-id opts)
                        :fork/from-native-thread-id (:fork/from-native-thread-id opts)}
             :payload (cond-> {:context-bundle/id context-bundle-id
                               :executor/pool (or (:executor-pool opts)
                                                  default-executor-pool)
                               :executor/hints (or (:executor-hints opts) {:interactive? true})
                               :run/restart-policy (or (:run-restart-policy opts)
                                                       (:run/restart-policy opts)
                                                       :fail-on-stale-approval)}
                        executor-task-id
                        (assoc :executor/task-id executor-task-id))}
      auth-mode
      (assoc :llm/auth-mode auth-mode))))

(def required-request-keys
  [:request/id :request/type :request/schema-version :request/time-ms
   :idempotency/key :routing/key :world-thread/id :world-turn/id
   :context-bundle/id :llm-thread/id :llm-turn-run/id :executor :payload])

(defn request-validation-errors
  [request]
  (let [request (normalize-turn-run-request request)
        run-id (:llm-turn-run/id request)
        context-bundle-id (:context-bundle/id request)
        payload-context-bundle-id (get-in request [:payload :context-bundle/id])
        backend (:llm/backend request)
        auth-mode (:llm/auth-mode request)
        forbidden-options (vec (filter #(contains? (:payload request) %)
                                       forbidden-payload-execution-option-keys))]
    (cond-> []
      (not (map? request))
      (conj {:type :request/not-map})

      (and (map? request) (not-every? #(contains? request %) required-request-keys))
      (conj {:type :request/missing-envelope-key
             :missing (vec (remove #(contains? request %) required-request-keys))})

      (and (map? request) (not= :llm/turn-run-request (:request/type request)))
      (conj {:type :request/type-invalid
             :value (:request/type request)})

      (and (map? request) (blank-string? (:request/id request)))
      (conj {:type :request/id-invalid
             :value (:request/id request)})

      (and (map? request) (blank-string? (:idempotency/key request)))
      (conj {:type :idempotency/key-invalid
             :value (:idempotency/key request)})

      (and (map? request) (blank-string? run-id))
      (conj {:type :llm-turn-run/id-invalid
             :value run-id})

      (and (map? request) (not= (llm-routing-key run-id) (:routing/key request)))
      (conj {:type :routing/key-invalid
             :value (:routing/key request)
             :expected (llm-routing-key run-id)})

      (and (map? request) (blank-string? (:world-thread/id request)))
      (conj {:type :world-thread/id-invalid
             :value (:world-thread/id request)})

      (and (map? request) (blank-string? (:world-turn/id request)))
      (conj {:type :world-turn/id-invalid
             :value (:world-turn/id request)})

      (and (map? request) (blank-string? context-bundle-id))
      (conj {:type :context-bundle/id-invalid
             :value context-bundle-id})

      (and (map? request) (not= context-bundle-id payload-context-bundle-id))
      (conj {:type :context-bundle/payload-drift
             :context-bundle/id context-bundle-id
             :payload/context-bundle-id payload-context-bundle-id})

      (and (map? request) (blank-string? (:llm-thread/id request)))
      (conj {:type :llm-thread/id-invalid
             :value (:llm-thread/id request)})

      (and (map? request) (not (contains? llm-backends backend)))
      (conj {:type :llm/backend-invalid
             :value backend
             :allowed llm-backends})

      (and (map? request) (not= backend (get-in request [:executor :agent/kind])))
      (conj {:type :executor/agent-kind-backend-drift
             :llm/backend backend
             :executor/agent-kind (get-in request [:executor :agent/kind])})

      (and (map? request) (not (keyword? (get-in request [:executor :agent/kind]))))
      (conj {:type :executor/agent-kind-invalid
             :value (get-in request [:executor :agent/kind])})

      (and (map? request) (= :passive-observe auth-mode))
      (conj {:type :llm/auth-mode-passive-observe-not-active
             :value auth-mode})

      (and (map? request) (= :claude backend) (not (contains? claude-auth-modes auth-mode)))
      (conj {:type :llm/auth-mode-invalid
             :llm/backend backend
             :value auth-mode
             :allowed claude-auth-modes})

      (and (map? request) (seq forbidden-options))
      (conj {:type :payload/execution-options-not-bundle-owned
             :keys forbidden-options}))))

(defn request-errors? [errors] (boolean (seq errors)))
(defn request-run-id [request] (:llm-turn-run/id request))
(defn decision-run-id [decision] (:llm-turn-run/id decision))
(defn decision-accepted? [decision] (= :accepted (:decision/status decision)))

(defn decision-id-for-run-id
  [run-id]
  (str run-id "/decision"))

(defn decision-time-ms
  [request]
  (:request/time-ms request))

(defn run-event
  [request]
  (let [request (normalize-turn-run-request request)]
    {:event/id (str (:request/id request) "/event")
   :event/type :llm-turn-run/requested
   :event/time-ms (:request/time-ms request)
   :event/schema-version schema-version
   :request/id (:request/id request)
   :routing/key (:routing/key request)
   :world-thread/id (:world-thread/id request)
   :world-turn/id (:world-turn/id request)
   :context-bundle/id (:context-bundle/id request)
   :llm-thread/id (:llm-thread/id request)
   :llm-turn-run/id (:llm-turn-run/id request)
   :llm/backend (:llm/backend request)
   :llm/auth-mode (:llm/auth-mode request)
   :executor (:executor request)
   :payload (:payload request)}))

(defn accepted-decision
  [request event]
  {:decision/id (decision-id-for-run-id (:llm-turn-run/id request))
   :decision/status :accepted
   :llm-turn-run/id (:llm-turn-run/id request)
   :request/id (:request/id request)
   :request/type (:request/type request)
   :routing/key (:routing/key request)
   :event/id (:event/id event)
   :event/ids [(:event/id event)]
   :event event
   :decided-at (decision-time-ms request)})

(defn rejected-decision
  [request reason & [errors]]
  {:decision/id (decision-id-for-run-id (:llm-turn-run/id request))
   :decision/status :rejected
   :llm-turn-run/id (:llm-turn-run/id request)
   :request/id (:request/id request)
   :request/type (:request/type request)
   :routing/key (:routing/key request)
   :event/id nil
   :event/ids []
   :decision/reason reason
   :errors (vec errors)
   :decided-at (decision-time-ms request)})

(defn interpret-turn-run-request
  [request]
  (let [request (normalize-turn-run-request request)
        errors (request-validation-errors request)]
    (if (seq errors)
      (rejected-decision request :request-invalid errors)
      (accepted-decision request (run-event request)))))

(defn conj-distinct
  [xs x]
  (let [v (vec xs)]
    (if (some #{x} v)
      v
      (conj v x))))

(defn initial-turn-run-row
  [decision]
  (let [event (:event decision)
        payload (:payload event)
        time-ms (:event/time-ms event)
        backend (or (:llm/backend event)
                    (get-in event [:executor :agent/kind])
                    :codex)
        native-thread-id (get-in event [:executor :native/thread-id])]
    {:llm-turn-run/id (:llm-turn-run/id decision)
     :llm-thread/id (:llm-thread/id event)
     :world-thread/id (:world-thread/id event)
     :world-turn/id (:world-turn/id event)
     :context-bundle/id (:context-bundle/id event)
     :request/id (:request/id decision)
     :decision/id (:decision/id decision)
     :status :pending
     :llm/backend backend
     :llm/auth-mode (:llm/auth-mode event)
     :agent/kind (get-in event [:executor :agent/kind])
     :native/codex-thread-id (when (= :codex backend) native-thread-id)
     :native/claude-session-id (when (= :claude backend) native-thread-id)
     :fork/from-native-thread-id (get-in event [:executor :fork/from-native-thread-id])
     :executor/pool (:executor/pool payload)
     :executor/hints (:executor/hints payload)
     :executor/task-id (request-executor-task-id event)
     :run/restart-policy (or (:run/restart-policy payload)
                             :fail-on-stale-approval)
     :created-at time-ms
     :updated-at time-ms
     :claimed-by nil
     :claim/token nil
     :claimed-at nil
     :started-at nil
     :finished-at nil
     :last-seq -1
     :obs-buffer {}
     :items-by-id {}
     :item-order []
     :raw-response-items {}
     :tool-calls-by-id {}
     :patch-proposals-by-id {}
     :patch-proposal-order []
     :approvals-pending {}
     :approvals-by-id {}
     :controls-by-id {}
     :control-order []
     :compactions []
     :steers []
     :token-usage {}
     :observation-errors []}))

(defn assign-executor-task
  [run-row current-task-id]
  (let [executor-task-id (or (:executor/task-id run-row)
                             (normalize-task-id current-task-id)
                             pending-task-id)]
    (assoc run-row :executor/task-id executor-task-id)))

(defn run-executor-task-id [run-row] (:executor/task-id run-row))
(defn run-thread-id [run-row] (:llm-thread/id run-row))
(defn run-world-thread-id [run-row] (:world-thread/id run-row))
(defn run-world-turn-id [run-row] (:world-turn/id run-row))
(defn known-run-row? [run-row] (some? run-row))
(defn terminal-run-row? [run-row] (contains? terminal-statuses (:status run-row)))
(defn fork-binding-required? [run-row]
  (and (:fork/from-native-thread-id run-row)
       (case (:llm/backend run-row)
         :claude (nil? (:native/claude-session-id run-row))
         (nil? (:native/codex-thread-id run-row)))))

(defn turn-run-summary
  [run-row]
  (select-keys run-row
               [:llm-turn-run/id :request/id :status :world-turn/id
                :context-bundle/id :created-at :updated-at
                :llm/backend :native/codex-thread-id
                :native/claude-session-id]))

(defn pending-entry
  [run-row]
  {:llm-turn-run/id (:llm-turn-run/id run-row)
   :llm-thread/id (:llm-thread/id run-row)
   :request/id (:request/id run-row)
   :context-bundle/id (:context-bundle/id run-row)
   :executor/task-id (:executor/task-id run-row)
   :created-at (:created-at run-row)
   :status (:status run-row)})

(defn upsert-thread-row
  [existing run-row]
  (let [time-ms (:updated-at run-row)
        base (or existing
                 {:llm-thread/id (:llm-thread/id run-row)
                  :world-thread/id (:world-thread/id run-row)
                  :agent/kind (:agent/kind run-row)
                  :llm/backend (:llm/backend run-row)
                  :native/codex-thread-id (:native/codex-thread-id run-row)
                  :native/claude-session-id (:native/claude-session-id run-row)
                  :created-at (:created-at run-row)
                  :turn-run/ids []})]
    (-> base
        (assoc :updated-at time-ms
               :world-thread/id (:world-thread/id run-row)
               :agent/kind (:agent/kind run-row)
               :llm/backend (:llm/backend run-row)
               :native/codex-thread-id (or (:native/codex-thread-id run-row)
                                           (:native/codex-thread-id base))
               :native/claude-session-id (or (:native/claude-session-id run-row)
                                             (:native/claude-session-id base)))
        (update :turn-run/ids conj-distinct (:llm-turn-run/id run-row)))))

(defn bind-run-to-existing-thread
  [run-row existing-thread-row]
  (cond-> run-row
    (and (nil? (:native/codex-thread-id run-row))
         (some? (:native/codex-thread-id existing-thread-row)))
    (assoc :native/codex-thread-id (:native/codex-thread-id existing-thread-row))

    (and (nil? (:native/claude-session-id run-row))
         (some? (:native/claude-session-id existing-thread-row)))
    (assoc :native/claude-session-id (:native/claude-session-id existing-thread-row))))

(defn run-items-vector
  [run-row]
  (vec (keep #(get-in run-row [:items-by-id %]) (:item-order run-row))))

(defn run-view
  [run-row]
  (assoc (select-keys run-row
                      [:llm-turn-run/id :llm-thread/id :world-thread/id
                       :world-turn/id :context-bundle/id :request/id :status
                       :llm/backend :llm/auth-mode :agent/kind
                       :native/codex-thread-id :native/claude-session-id
                       :executor/task-id
                       :claimed-by :started-at :finished-at :created-at
                       :updated-at :last-seq :token-usage :observation-errors])
         :items (run-items-vector run-row)
         :approvals-pending (vec (vals (:approvals-pending run-row)))))

(defn run-detail-projection
  [run-row]
  (assoc (run-view run-row)
         :projection/type :llm-run-detail
         :projection/source :llm-turn-runs
         :controls (vec (keep #(get-in run-row [:controls-by-id %])
                              (:control-order run-row)))
         :patch-proposals (vec (keep #(get-in run-row [:patch-proposals-by-id %])
                                     (:patch-proposal-order run-row)))))

(def cost-rollup-token-keys
  [:tokens/input-total :tokens/cached-input :tokens/output
   :tokens/reasoning-output])

(defn numeric-token-value
  [usage k]
  (let [v (get usage k)]
    (if (number? v) v 0)))

;; Keep the hot observation topology incremental. A previous version recomputed
;; totals from every entry in :runs on each token observation, which was more
;; self-repairing if the rollup PState was manually corrupted but made repeated
;; embedded Rama deploys fragile. This delta form still replays deterministically
;; from depot history and avoids double-counting updated usage for the same run;
;; full repair should rebuild $$llm-cost-by-thread from canonical run token usage.
(defn apply-token-usage-delta
  [totals previous-usage usage]
  (reduce (fn [acc k]
            (assoc acc k (+ (numeric-token-value acc k)
                            (- (numeric-token-value usage k)
                               (numeric-token-value previous-usage k)))))
          (or totals {})
          cost-rollup-token-keys))

(defn cost-rollup-run-entry
  [run-row]
  {:llm-turn-run/id (:llm-turn-run/id run-row)
   :world-turn/id (:world-turn/id run-row)
   :context-bundle/id (:context-bundle/id run-row)
   :status (:status run-row)
   :token-usage (:token-usage run-row)
   :updated-at (:updated-at run-row)})

(defn cost-rollup-for-thread
  [existing run-row]
  (let [run-id (:llm-turn-run/id run-row)
        usage (:token-usage run-row)
        existing-runs (or (:runs existing) {})
        previous-usage (get-in existing-runs [run-id :token-usage])
        runs (cond-> existing-runs
               (seq usage) (assoc run-id (cost-rollup-run-entry run-row)))
        totals (cond-> (:tokens existing)
                 (seq usage) (apply-token-usage-delta previous-usage usage))]
    {:projection/type :llm-thread-cost-rollup
     :projection/source :llm-token-usage-by-run-id
     :llm-thread/id (:llm-thread/id run-row)
     :world-thread/id (:world-thread/id run-row)
     :run-count (count runs)
     :runs runs
     :tokens totals
     :updated-at (:updated-at run-row)}))

(defn claim-record
  [run-id llm-thread-id executor-id & [opts]]
  (let [task-id (or (opts-executor-task-id opts) pending-task-id)]
    {:claim/id (or (:claim-id opts) (random-id "claim"))
     :llm-turn-run/id run-id
     :llm-thread/id llm-thread-id
     :executor/id executor-id
     :executor/task-id task-id
     :claim/token (or (:claim-token opts) (str (UUID/randomUUID)))
     :claimed-at-ms (or (:claimed-at-ms opts) (:claimed-at opts) (now-ms))
     :routing/key (llm-routing-key run-id)}))

(defn claim-run-id [claim] (:llm-turn-run/id claim))
(defn observation-run-id [obs] (:llm-turn-run/id obs))

(defn control-record
  [run-id control-type & [opts]]
  {:control/id (or (:control-id opts) (:control/id opts) (random-id "llm-control"))
   :control/type control-type
   :control/schema-version schema-version
   :routing/key (llm-routing-key run-id)
   :llm-turn-run/id run-id
   :llm-thread/id (:llm-thread/id opts)
   :world-thread/id (:world-thread/id opts)
   :world-turn/id (:world-turn/id opts)
   :approval/id (:approval/id opts)
   :native/json-rpc-request-id (:native/json-rpc-request-id opts)
   :decision (:decision opts)
   :actor (or (:actor opts) {:actor/id "system" :actor/type :system})
   :time-ms (or (:time-ms opts) (now-ms))
   :reason (:reason opts)
   :payload (or (:payload opts) {})})

(defn control-run-id [control] (:llm-turn-run/id control))
(defn control-id [control] (:control/id control))

(defn valid-control?
  [control]
  (and (map? control)
       (contains? control-types (:control/type control))
       (not (blank-string? (:control/id control)))
       (not (blank-string? (:llm-turn-run/id control)))
       (= (llm-routing-key (:llm-turn-run/id control)) (:routing/key control))))

(defn valid-claim?
  [claim]
  (and (map? claim)
       (not (blank-string? (:llm-turn-run/id claim)))
       (not (blank-string? (:llm-thread/id claim)))
       (not (blank-string? (:executor/id claim)))
       (not (blank-string? (:executor/task-id claim)))
       (not (blank-string? (:claim/token claim)))
       (= (llm-routing-key (:llm-turn-run/id claim)) (:routing/key claim))))

(defn grantable-claim?
  [run-row claim]
  (and (valid-claim? claim)
       (= :pending (:status run-row))
       (= (:llm-turn-run/id run-row) (:llm-turn-run/id claim))
       (= (:llm-thread/id run-row) (:llm-thread/id claim))
       (= (:executor/task-id run-row) (:executor/task-id claim))))

(defn grant-claim
  [run-row claim]
  (let [t (or (:claimed-at-ms claim) (now-ms))]
    (assoc run-row
           :status :claimed
           :claimed-by (:executor/id claim)
           :claim/token (:claim/token claim)
           :claimed-at t
           :updated-at t)))

(defn claim-state
  [run-row claim]
  (cond
    (nil? run-row) :not-yet-processed
    (and (= :claimed (:status run-row))
         (= (:executor/id claim) (:claimed-by run-row))
         (= (:claim/token claim) (:claim/token run-row))) :granted-to-us
    (= :pending (:status run-row)) :not-yet-processed
    :else :conflict-or-past))

(defn append-bounded
  [xs x limit]
  (let [v (conj (vec xs) x)
        c (count v)]
    (if (> c limit)
      (subvec v (- c limit))
      v)))

(defn observation-error
  [reason run-row obs]
  {:reason reason
   :llm-turn-run/id (:llm-turn-run/id run-row)
   :observation/type (:observation/type obs)
   :sequence (:sequence obs)
   :received-at-ms (or (:received-at-ms obs) (now-ms))})

(defn add-observation-error
  [run-row reason obs]
  (let [t (or (:received-at-ms obs) (now-ms))]
    (-> run-row
        (update :observation-errors
                append-bounded
                (observation-error reason run-row obs)
                default-error-limit)
        (assoc :updated-at t))))

(defn valid-observation-sequence?
  [obs]
  (let [seq-id (:sequence obs)]
    (and (integer? seq-id) (not (neg? seq-id)))))

(defn valid-observation-routing?
  [obs]
  (= (llm-routing-key (:llm-turn-run/id obs)) (:routing/key obs)))

(defn observation
  [run-id llm-thread-id observation-type sequence & [opts]]
  (let [obs-id (or (:observation-id opts) (:observation/id opts) (random-id "obs"))
        received-at (or (:received-at-ms opts) (:observed-at opts) (now-ms))]
    (merge
      {:observation/id obs-id
       :observation/type observation-type
       :observation/schema-version schema-version
       :routing/key (llm-routing-key run-id)
       :llm-turn-run/id run-id
       :llm-thread/id llm-thread-id
       :llm/backend (or (:llm/backend opts)
                        (:backend opts)
                        (when (namespace observation-type)
                          (keyword (namespace observation-type))))
       :native/codex-thread-id (:native/codex-thread-id opts)
       :native/codex-turn-id (:native/codex-turn-id opts)
       :native/claude-session-id (:native/claude-session-id opts)
       :sequence sequence
       :received-at-ms received-at
       :codex/event-method (:codex/event-method opts)
       :codex/event-params (:codex/event-params opts)
       :claude/event-type (:claude/event-type opts)
       :claude/event (:claude/event opts)
       :raw/json (or (:raw/json opts) (:raw-json opts) {})}
      (select-keys opts
                   [:world-thread/id :world-turn/id
                    :llm-item/id :native/item-id :item/type :content/text
                    :content/hash :approval/id :approval/type
                    :native/json-rpc-request-id :tokens/input-total
                    :tokens/cached-input :tokens/output
                    :tokens/reasoning-output :model/context-window
                    :subscription/messages-used :billing/mode
                    :tool-call/id :tool-call/type :tool-call/name
                    :tool-call/status :patch-proposal/id :turn-diff/id
                    :patch/files :summary/text :error
                    :result/status :result/cost-usd :result/text
                    :provider/native-redacted]))))

(defn sensitive-key?
  [k]
  (let [s (-> k name str/lower-case)]
    (or (contains? sensitive-key-names s)
        (str/includes? s "token")
        (str/includes? s "secret")
        (str/includes? s "password")
        (str/includes? s "authorization")
        (str/includes? s "api-key")
        (str/includes? s "api_key"))))

(defn redact-provider-payload
  [x]
  (cond
    (map? x)
    (into (empty x)
          (map (fn [[k v]]
                 [k (if (sensitive-key? k)
                      "[REDACTED]"
                      (redact-provider-payload v))]))
          x)

    (vector? x)
    (mapv redact-provider-payload x)

    (sequential? x)
    (mapv redact-provider-payload x)

    :else x))

(defn observation-source
  [obs]
  (or (:llm/backend obs)
      (when (namespace (:observation/type obs))
        (keyword (namespace (:observation/type obs))))
      :codex))

(defn claude-observation-type?
  [obs]
  (= :claude (observation-source obs)))

(defn item-source
  [obs]
  (observation-source obs))

(defn observation->item-row
  [obs]
  (let [text (or (:content/text obs)
                 (get-in obs [:codex/event-params :content])
                 (get-in obs [:claude/event :content])
                 "")
        item-id (or (:llm-item/id obs)
                    (:native/item-id obs)
                    (:observation/id obs))]
    {:llm-item/id item-id
     :llm-turn-run/id (:llm-turn-run/id obs)
     :llm-thread/id (:llm-thread/id obs)
     :native/item-id (:native/item-id obs)
     :item/type (or (:item/type obs) :assistant-message)
     :item/order (:sequence obs)
     :content/text text
     :content/hash (or (:content/hash obs) (str "sha256:" (kernel/sha-256 text)))
     :source (item-source obs)
     :created-at-ms (:received-at-ms obs)}))

(defn add-item
  [run-row item]
  (if (get-in run-row [:items-by-id (:llm-item/id item)])
    run-row
    (-> run-row
        (assoc-in [:items-by-id (:llm-item/id item)] item)
        (update :item-order conj-distinct (:llm-item/id item)))))

(defn observation->approval-row
  [obs]
  {:approval/id (or (:approval/id obs) (:observation/id obs))
   :approval/type (or (:approval/type obs) :exec)
   :llm-turn-run/id (:llm-turn-run/id obs)
   :llm-thread/id (:llm-thread/id obs)
   :native/json-rpc-request-id (:native/json-rpc-request-id obs)
   :codex/event-method (:codex/event-method obs)
   :codex/event-params (:codex/event-params obs)
   :sequence (:sequence obs)
   :status :pending
   :received-at-ms (:received-at-ms obs)})

(defn add-approval
  [run-row approval]
  (-> run-row
      (assoc :status :blocked-awaiting-approval)
      (assoc-in [:approvals-pending (:approval/id approval)] approval)
      (assoc-in [:approvals-by-id (:approval/id approval)] approval)))

(def token-usage-keys
  [:tokens/input-total :tokens/cached-input :tokens/output
   :tokens/reasoning-output :model/context-window
   :subscription/messages-used :billing/mode])

(defn observation->token-usage
  [obs]
  (select-keys obs token-usage-keys))

(defn observation->tool-call-row
  [obs]
  {:tool-call/id (or (:tool-call/id obs) (:observation/id obs))
   :tool-call/type (:tool-call/type obs)
   :tool-call/name (:tool-call/name obs)
   :tool-call/status (:tool-call/status obs)
   :llm-turn-run/id (:llm-turn-run/id obs)
   :llm-thread/id (:llm-thread/id obs)
   :sequence (:sequence obs)
   :received-at-ms (:received-at-ms obs)
   :source (observation-source obs)
   :raw/json (:raw/json obs)})

(defn observation->patch-proposal-row
  [obs]
  (let [proposal-id (or (:patch-proposal/id obs)
                        (:turn-diff/id obs)
                        (:observation/id obs))]
    {:patch-proposal/id proposal-id
     :turn-diff/id (or (:turn-diff/id obs) proposal-id)
     :llm-turn-run/id (:llm-turn-run/id obs)
     :llm-thread/id (:llm-thread/id obs)
     :sequence (:sequence obs)
     :status :pending
     :patch/files (:patch/files obs)
     :summary/text (:summary/text obs)
     :received-at-ms (:received-at-ms obs)
     :raw/json (:raw/json obs)}))

(defn add-patch-proposal
  [run-row proposal]
  (-> run-row
      (assoc-in [:patch-proposals-by-id (:patch-proposal/id proposal)] proposal)
      (update :patch-proposal-order conj-distinct (:patch-proposal/id proposal))))

(defn add-raw-response-item
  [run-row obs]
  (assoc-in run-row [:raw-response-items (:observation/id obs)] (:raw/json obs)))

(defn observation-native-thread-id
  [obs]
  (or (:native/codex-thread-id obs)
      (get-in obs [:codex/event-params :thread/id])
      (get-in obs [:codex/event-params :thread-id])
      (get-in obs [:codex/event-params :thread_id])
      (get-in obs [:raw/json :thread/id])
      (get-in obs [:raw/json :thread-id])
      (get-in obs [:raw/json :thread_id])))

(defn observation-native-claude-session-id
  [obs]
  (or (:native/claude-session-id obs)
      (get-in obs [:claude/event :session_id])
      (get-in obs [:claude/event :session-id])
      (get-in obs [:raw/json :session_id])
      (get-in obs [:raw/json :session-id])))

(defn bind-run-to-observation-thread
  [run-row obs]
  (cond-> run-row
    (observation-native-thread-id obs)
    (assoc :native/codex-thread-id
           (or (:native/codex-thread-id run-row)
               (observation-native-thread-id obs)))

    (observation-native-claude-session-id obs)
    (assoc :native/claude-session-id
           (or (:native/claude-session-id run-row)
               (observation-native-claude-session-id obs)))))

(defn apply-observation-effect
  [run-row obs]
  (let [t (or (:received-at-ms obs) (now-ms))
        run-row (-> run-row
                    (add-raw-response-item obs)
                    (bind-run-to-observation-thread obs))]
    (case (:observation/type obs)
      :codex/item-completed
      (-> run-row
          (add-item (observation->item-row obs))
          (assoc :status (if (contains? #{:pending :claimed} (:status run-row))
                           :running
                           (:status run-row))
                 :started-at (or (:started-at run-row) t)
                 :updated-at t))

      :codex/approval-request
      (-> run-row
          (add-approval (observation->approval-row obs))
          (assoc :updated-at t))

      :codex/token-usage
      (-> run-row
          (update :token-usage merge (observation->token-usage obs))
          (assoc :updated-at t))

      :codex/tool-call
      (-> run-row
          (assoc-in [:tool-calls-by-id (or (:tool-call/id obs) (:observation/id obs))]
                    (observation->tool-call-row obs))
          (assoc :updated-at t))

      :codex/patch-proposal
      (-> run-row
          (add-patch-proposal (observation->patch-proposal-row obs))
          (assoc :updated-at t))

      :codex/run-finished
      (assoc run-row
             :status :succeeded
             :finished-at t
             :updated-at t)

      :codex/run-failed
      (assoc run-row
             :status :failed
             :error (:error obs)
             :finished-at t
             :updated-at t)

      :claude/system-init
      (assoc run-row
             :status (if (contains? #{:pending :claimed} (:status run-row))
                       :running
                       (:status run-row))
             :started-at (or (:started-at run-row) t)
             :updated-at t)

      :claude/stream-delta
      (-> run-row
          (add-item (observation->item-row obs))
          (assoc :status (if (contains? #{:pending :claimed} (:status run-row))
                           :running
                           (:status run-row))
                 :started-at (or (:started-at run-row) t)
                 :updated-at t))

      :claude/message
      (cond-> (assoc run-row
                     :status (if (contains? #{:pending :claimed} (:status run-row))
                               :running
                               (:status run-row))
                     :started-at (or (:started-at run-row) t)
                     :updated-at t)
        (seq (:content/text obs))
        (add-item (observation->item-row obs))
        (seq (observation->token-usage obs))
        (update :token-usage merge (observation->token-usage obs)))

      :claude/user-message
      (-> run-row
          (add-item (observation->item-row obs))
          (assoc :updated-at t))

      :claude/tool-call
      (-> run-row
          (assoc-in [:tool-calls-by-id (or (:tool-call/id obs) (:observation/id obs))]
                    (observation->tool-call-row obs))
          (assoc :status (if (contains? #{:pending :claimed} (:status run-row))
                           :running
                           (:status run-row))
                 :started-at (or (:started-at run-row) t)
                 :updated-at t))

      :claude/tool-result
      (-> run-row
          (add-item (observation->item-row obs))
          (assoc-in [:tool-calls-by-id (or (:tool-call/id obs) (:observation/id obs))]
                    (observation->tool-call-row
                      (assoc obs :tool-call/status (or (:tool-call/status obs) :completed))))
          (assoc :updated-at t))

      :claude/hook-event
      (cond-> (assoc run-row :updated-at t)
        (:approval/id obs)
        (add-approval (observation->approval-row obs)))

      :claude/control-response
      (assoc run-row :updated-at t)

      :claude/rate-limit
      (assoc run-row
             :status :failed
             :error (or (:error obs) {:reason :claude/rate-limit})
             :finished-at t
             :updated-at t)

      :claude/auth
      (assoc run-row
             :status :failed
             :error (or (:error obs) {:reason :claude/auth})
             :finished-at t
             :updated-at t)

      :claude/api-retry
      (assoc run-row :updated-at t)

      :claude/compact-boundary
      (-> run-row
          (update :compactions conj {:observation/id (:observation/id obs)
                                     :sequence (:sequence obs)
                                     :received-at-ms t
                                     :payload (:provider/native-redacted obs)})
          (assoc :updated-at t))

      :claude/source-boundary
      (assoc run-row :updated-at t)

      :claude/tombstone
      (assoc run-row :updated-at t)

      :claude/result
      (assoc run-row
             :status (if (contains? #{:failed :error} (:result/status obs))
                       :failed
                       :succeeded)
             :error (:error obs)
             :finished-at t
             :updated-at t)

      :claude/run-failed
      (assoc run-row
             :status :failed
             :error (:error obs)
             :finished-at t
             :updated-at t)

      (add-observation-error run-row :observation/type-invalid obs))))

(declare drain-observation-buffer)

(defn apply-observation-in-order
  [run-row obs]
  (-> run-row
      (apply-observation-effect obs)
      (assoc :last-seq (:sequence obs))))

(defn drain-observation-buffer
  [run-row]
  (loop [row run-row]
    (let [next-seq (inc (long (:last-seq row)))
          obs (get-in row [:obs-buffer next-seq])]
      (if obs
        (recur (-> row
                   (update :obs-buffer dissoc next-seq)
                   (apply-observation-in-order obs)))
        row))))

(defn fold-observation
  [run-row obs]
  (let [seq-id (:sequence obs)
        expected (inc (long (:last-seq run-row)))]
    (cond
      (not= (:llm-turn-run/id run-row) (:llm-turn-run/id obs))
      (add-observation-error run-row :observation/run-mismatch obs)

      (not= (:llm-thread/id run-row) (:llm-thread/id obs))
      (add-observation-error run-row :observation/thread-mismatch obs)

      (not (valid-observation-routing? obs))
      (add-observation-error run-row :observation/routing-key-invalid obs)

      (not (contains? observation-types (:observation/type obs)))
      (add-observation-error run-row :observation/type-invalid obs)

      (not (valid-observation-sequence? obs))
      (add-observation-error run-row :observation/sequence-invalid obs)

      (< seq-id expected)
      run-row

      (= seq-id expected)
      (drain-observation-buffer (apply-observation-in-order run-row obs))

      :else
      (assoc-in run-row [:obs-buffer seq-id] obs))))

(defn observation-approval-materialized?
  [run-row obs]
  (and (contains? #{:codex/approval-request :claude/hook-event}
                  (:observation/type obs))
       (:approval/id obs)
       (<= (long (:sequence obs)) (long (:last-seq run-row)))))

(defn indexable-item-observation?
  [run-row obs]
  (and (contains? #{:codex/item-completed
                   :claude/stream-delta
                   :claude/message
                   :claude/user-message
                   :claude/tool-result}
                 (:observation/type obs))
       (seq (:content/text obs))
       (= (:llm-turn-run/id run-row) (:llm-turn-run/id obs))
       (= (:llm-thread/id run-row) (:llm-thread/id obs))
       (valid-observation-routing? obs)
       (valid-observation-sequence? obs)))

(defn item-row-id
  [item-row]
  (:llm-item/id item-row))

(defn keep-existing-item-row
  [existing item-row]
  (or existing item-row))

(defn run-items-by-id [run-row] (:items-by-id run-row))
(defn run-raw-response-items [run-row] (:raw-response-items run-row))
(defn run-tool-calls-by-id [run-row] (:tool-calls-by-id run-row))
(defn run-approvals-by-id [run-row] (:approvals-by-id run-row))
(defn run-token-usage [run-row] (:token-usage run-row))
(defn run-controls-by-id [run-row] (:controls-by-id run-row))
(defn approval-id [approval] (:approval/id approval))
(defn control-approval-id [control] (:approval/id control))
(defn control-has-approval? [control] (not (blank-string? (:approval/id control))))

(defn record-control
  [run-row control]
  (-> run-row
      (assoc-in [:controls-by-id (:control/id control)] control)
      (update :control-order conj-distinct (:control/id control))))

(defn approval-resolution-status
  [decision]
  (case decision
    :approved :approved
    :allow :approved
    :denied :denied
    :declined :declined
    :rejected :rejected
    :expired :expired
    :timeout :expired
    decision))

(defn approval-terminal-decision?
  [decision]
  (contains? terminal-approval-decisions decision))

(defn resolve-approval
  [run-row control]
  (let [approval-id (:approval/id control)
        decision (or (:decision control) :approved)
        status (approval-resolution-status decision)
        t (:time-ms control)
        existing (or (get-in run-row [:approvals-by-id approval-id])
                     {:approval/id approval-id
                      :llm-turn-run/id (:llm-turn-run/id control)
                      :llm-thread/id (:llm-thread/id control)})
        approval (assoc existing
                        :status status
                        :decision decision
                        :resolved-at-ms t
                        :resolved-by (:actor control)
                        :control/id (:control/id control)
                        :native/json-rpc-request-id
                        (or (:native/json-rpc-request-id control)
                            (:native/json-rpc-request-id existing)))
        pending-after (dissoc (:approvals-pending run-row) approval-id)
        failed? (approval-terminal-decision? decision)]
    (cond-> (-> run-row
                (assoc-in [:approvals-by-id approval-id] approval)
                (assoc :approvals-pending pending-after
                       :updated-at t))
      (and (not failed?)
           (= :blocked-awaiting-approval (:status run-row))
           (empty? pending-after))
      (assoc :status :running)

      failed?
      (assoc :status :failed
             :finished-at t
             :error {:reason :approval/declined
                     :decision decision
                     :approval/id approval-id
                     :control/id (:control/id control)}))))

(defn cancel-run
  [run-row control]
  (let [t (:time-ms control)]
    (assoc run-row
           :status :cancelled
           :finished-at t
           :updated-at t
           :cancelled-by (:actor control)
           :cancel/reason (:reason control))))

(defn add-compaction
  [run-row control]
  (-> run-row
      (update :compactions conj {:control/id (:control/id control)
                                 :time-ms (:time-ms control)
                                 :actor (:actor control)
                                 :payload (:payload control)})
      (assoc :updated-at (:time-ms control))))

(defn add-steer
  [run-row control]
  (-> run-row
      (update :steers conj {:control/id (:control/id control)
                            :time-ms (:time-ms control)
                            :actor (:actor control)
                            :payload (:payload control)})
      (assoc :updated-at (:time-ms control))))

(defn fold-control
  [run-row control]
  (let [run-row (record-control run-row control)]
    (cond
      (not (valid-control? control))
      (add-observation-error run-row :control/invalid control)

      (not= (:llm-turn-run/id run-row) (:llm-turn-run/id control))
      (add-observation-error run-row :control/run-mismatch control)

      (= :approval/resolve (:control/type control))
      (resolve-approval run-row control)

      (= :turn/cancel (:control/type control))
      (cancel-run run-row control)

      (= :compact/request (:control/type control))
      (add-compaction run-row control)

      (= :turn/steer (:control/type control))
      (add-steer run-row control)

      :else
      (add-observation-error run-row :control/type-invalid control))))

(defmodule llm-module [setup topologies]
  (declare-depot setup *llm-depot (hash-by :llm-turn-run/id))
  (declare-depot setup *llm-claim-depot (hash-by :llm-turn-run/id))
  (declare-depot setup *llm-obs-depot (hash-by :llm-turn-run/id))
  (declare-depot setup *llm-control-depot (hash-by :llm-turn-run/id))
  (let [n (stream-topology topologies "llm-track-topology")]
    (declare-pstate n $$llm-threads {String Object})
    (declare-pstate n $$llm-thread-by-world-thread {String String})
    (declare-pstate n $$llm-thread-graph {String Object})
    (declare-pstate n $$llm-turn-runs {String Object})
    (declare-pstate n $$llm-turn-runs-by-thread {String Object})
    (declare-pstate n $$llm-turn-run-by-world-turn {String String})
    (declare-pstate n $$llm-decisions-by-run-id {String Object})
    (declare-pstate n $$llm-pending-by-task {String Object})
    (declare-pstate n $$llm-items-by-turn-run {String Object})
    (declare-pstate n $$llm-items-by-thread {String Object})
    (declare-pstate n $$llm-item-by-id {String Object})
    (declare-pstate n $$llm-raw-response-items {String Object})
    (declare-pstate n $$llm-tool-calls-by-run-id {String Object})
    (declare-pstate n $$llm-approvals-pending {String Object})
    (declare-pstate n $$llm-approvals-by-run-id {String Object})
    (declare-pstate n $$llm-token-usage-by-run-id {String Object})
    (declare-pstate n $$llm-cost-by-thread {String Object})
    (declare-pstate n $$llm-controls-by-run-id {String Object})
    (declare-pstate n $$llm-control-by-id {String Object})
    (declare-pstate n $$llm-views {String Object})
    (declare-pstate n $$projection-run-detail {String Object})

    (<<sources n
      (source> *llm-depot :> *request)
      (interpret-turn-run-request *request :> *decision)
      (decision-run-id *decision :> *run-id)
      (|hash *run-id)
      (local-transform> [(keypath *run-id) (termval *decision)] $$llm-decisions-by-run-id)
      (<<if (decision-accepted? *decision)
        (initial-turn-run-row *decision :> *run-row)
        (current-task-id :> *current-task-id)
        (assign-executor-task *run-row *current-task-id :> *assigned-run-row)
        (run-thread-id *assigned-run-row :> *thread-id)
        (|hash *thread-id)
        (local-select> [(keypath *thread-id)] $$llm-threads :> *existing-thread-row)
        (bind-run-to-existing-thread *assigned-run-row *existing-thread-row :> *bound-run-row)
        (run-executor-task-id *bound-run-row :> *executor-task-id)
        (run-view *bound-run-row :> *view)
        (run-detail-projection *bound-run-row :> *run-detail)
        (pending-entry *bound-run-row :> *pending-entry)
        (turn-run-summary *bound-run-row :> *run-summary)
        (run-world-thread-id *assigned-run-row :> *world-thread-id)
        (run-world-turn-id *assigned-run-row :> *world-turn-id)
        (upsert-thread-row *existing-thread-row *bound-run-row :> *thread-row)
        (local-transform> [(keypath *thread-id) (termval *thread-row)] $$llm-threads)
        (local-transform> [(keypath *thread-id) (keypath *run-id) (termval *run-summary)] $$llm-turn-runs-by-thread)
        (|hash *run-id)
        (local-transform> [(keypath *run-id) (termval *bound-run-row)] $$llm-turn-runs)
        (local-transform> [(keypath *run-id) (termval *view)] $$llm-views)
        (local-transform> [(keypath *run-id) (termval *run-detail)] $$projection-run-detail)
        (|hash *world-turn-id)
        (local-transform> [(keypath *world-turn-id) (termval *run-id)] $$llm-turn-run-by-world-turn)
        (|hash *executor-task-id)
        (local-transform> [(keypath *executor-task-id) (keypath *run-id) (termval *pending-entry)] $$llm-pending-by-task)
        (|hash *world-thread-id)
        (local-transform> [(keypath *world-thread-id) (termval *thread-id)] $$llm-thread-by-world-thread))

      (source> *llm-claim-depot :> *claim)
      (claim-run-id *claim :> *run-id)
      (|hash *run-id)
      (local-select> [(keypath *run-id)] $$llm-turn-runs :> *run-row)
      (<<if (grantable-claim? *run-row *claim)
        (grant-claim *run-row *claim :> *claimed-run-row)
        (run-executor-task-id *claimed-run-row :> *executor-task-id)
        (run-view *claimed-run-row :> *view)
        (run-detail-projection *claimed-run-row :> *run-detail)
        (local-transform> [(keypath *run-id) (termval *claimed-run-row)] $$llm-turn-runs)
        (local-transform> [(keypath *run-id) (termval *view)] $$llm-views)
        (local-transform> [(keypath *run-id) (termval *run-detail)] $$projection-run-detail)
        (|hash *executor-task-id)
        (local-transform> [(keypath *executor-task-id) (keypath *run-id) NONE>] $$llm-pending-by-task))

      (source> *llm-obs-depot {:retry-mode :all-after} :> *obs)
      (observation-run-id *obs :> *run-id)
      (|hash *run-id)
      (local-select> [(keypath *run-id)] $$llm-turn-runs :> *run-row)
      (<<if (known-run-row? *run-row)
        (fold-observation *run-row *obs :> *updated-run-row)
        (run-view *updated-run-row :> *view)
        (run-detail-projection *updated-run-row :> *run-detail)
        (run-items-by-id *updated-run-row :> *items-by-id)
        (run-raw-response-items *updated-run-row :> *raw-response-items)
        (run-tool-calls-by-id *updated-run-row :> *tool-calls-by-id)
        (run-approvals-by-id *updated-run-row :> *approvals-by-id)
        (run-token-usage *updated-run-row :> *token-usage)
        (run-thread-id *updated-run-row :> *thread-id)
        (turn-run-summary *updated-run-row :> *run-summary)
        (local-transform> [(keypath *run-id) (termval *updated-run-row)] $$llm-turn-runs)
        (local-transform> [(keypath *run-id) (termval *view)] $$llm-views)
        (local-transform> [(keypath *run-id) (termval *run-detail)] $$projection-run-detail)
        (local-transform> [(keypath *run-id) (termval *items-by-id)] $$llm-items-by-turn-run)
        (local-transform> [(keypath *run-id) (termval *raw-response-items)] $$llm-raw-response-items)
        (local-transform> [(keypath *run-id) (termval *tool-calls-by-id)] $$llm-tool-calls-by-run-id)
        (local-transform> [(keypath *run-id) (termval *approvals-by-id)] $$llm-approvals-by-run-id)
        (local-transform> [(keypath *run-id) (termval *token-usage)] $$llm-token-usage-by-run-id)
        (|hash *thread-id)
        (local-select> [(keypath *thread-id)] $$llm-threads :> *existing-thread-row)
        (local-select> [(keypath *thread-id)] $$llm-cost-by-thread :> *existing-cost-rollup)
        (upsert-thread-row *existing-thread-row *updated-run-row :> *thread-row)
        (cost-rollup-for-thread *existing-cost-rollup *updated-run-row :> *cost-rollup)
        (local-transform> [(keypath *thread-id) (termval *thread-row)] $$llm-threads)
        (local-transform> [(keypath *thread-id) (keypath *run-id) (termval *run-summary)] $$llm-turn-runs-by-thread)
        (local-transform> [(keypath *thread-id) (keypath *run-id) (termval *items-by-id)] $$llm-items-by-thread)
        (local-transform> [(keypath *thread-id) (termval *cost-rollup)] $$llm-cost-by-thread)
        (<<if (observation-approval-materialized? *updated-run-row *obs)
          (observation->approval-row *obs :> *approval)
          (approval-id *approval :> *approval-id)
          (|hash *approval-id)
          (local-transform> [(keypath *approval-id) (termval *approval)] $$llm-approvals-pending))
        (<<if (indexable-item-observation? *run-row *obs)
          (observation->item-row *obs :> *item-row)
          (item-row-id *item-row :> *item-id)
          (|hash *item-id)
          (local-select> [(keypath *item-id)] $$llm-item-by-id :> *existing-item-row)
          (keep-existing-item-row *existing-item-row *item-row :> *indexed-item-row)
          (local-transform> [(keypath *item-id) (termval *indexed-item-row)] $$llm-item-by-id)))

      (source> *llm-control-depot :> *control)
      (control-run-id *control :> *run-id)
      (|hash *run-id)
      (local-select> [(keypath *run-id)] $$llm-turn-runs :> *run-row)
      (<<if (known-run-row? *run-row)
        (fold-control *run-row *control :> *updated-run-row)
        (run-view *updated-run-row :> *view)
        (run-detail-projection *updated-run-row :> *run-detail)
        (run-approvals-by-id *updated-run-row :> *approvals-by-id)
        (run-controls-by-id *updated-run-row :> *controls-by-id)
        (run-executor-task-id *updated-run-row :> *executor-task-id)
        (control-id *control :> *control-id)
        (local-transform> [(keypath *run-id) (termval *updated-run-row)] $$llm-turn-runs)
        (local-transform> [(keypath *run-id) (termval *view)] $$llm-views)
        (local-transform> [(keypath *run-id) (termval *run-detail)] $$projection-run-detail)
        (local-transform> [(keypath *run-id) (termval *approvals-by-id)] $$llm-approvals-by-run-id)
        (local-transform> [(keypath *run-id) (termval *controls-by-id)] $$llm-controls-by-run-id)
        (|hash *control-id)
        (local-transform> [(keypath *control-id) (termval *control)] $$llm-control-by-id)
        (<<if (control-has-approval? *control)
          (control-approval-id *control :> *approval-id)
          (|hash *approval-id)
          (local-transform> [(keypath *approval-id) NONE>] $$llm-approvals-pending))
        (<<if (terminal-run-row? *updated-run-row)
          (|hash *executor-task-id)
          (local-transform> [(keypath *executor-task-id) (keypath *run-id) NONE>] $$llm-pending-by-task))))))

(defn start-llm-runtime!
  []
  (let [ipc (create-ipc)
        module-name (get-module-name llm-module)
        launch-opts {:tasks 4 :threads 2}]
    (launch-module! ipc llm-module launch-opts)
    {:ipc ipc
     :module-name module-name
     :llm-depot (foreign-depot ipc module-name "*llm-depot")
     :llm-claim-depot (foreign-depot ipc module-name "*llm-claim-depot")
     :llm-obs-depot (foreign-depot ipc module-name "*llm-obs-depot")
     :llm-control-depot (foreign-depot ipc module-name "*llm-control-depot")
     :llm-threads (foreign-pstate ipc module-name "$$llm-threads")
     :llm-thread-by-world-thread (foreign-pstate ipc module-name "$$llm-thread-by-world-thread")
     :llm-turn-runs (foreign-pstate ipc module-name "$$llm-turn-runs")
     :llm-turn-runs-by-thread (foreign-pstate ipc module-name "$$llm-turn-runs-by-thread")
     :llm-turn-run-by-world-turn (foreign-pstate ipc module-name "$$llm-turn-run-by-world-turn")
     :llm-decisions-by-run-id (foreign-pstate ipc module-name "$$llm-decisions-by-run-id")
     :llm-pending-by-task (foreign-pstate ipc module-name "$$llm-pending-by-task")
     :llm-items-by-turn-run (foreign-pstate ipc module-name "$$llm-items-by-turn-run")
     :llm-items-by-thread (foreign-pstate ipc module-name "$$llm-items-by-thread")
     :llm-item-by-id (foreign-pstate ipc module-name "$$llm-item-by-id")
     :llm-raw-response-items (foreign-pstate ipc module-name "$$llm-raw-response-items")
     :llm-tool-calls-by-run-id (foreign-pstate ipc module-name "$$llm-tool-calls-by-run-id")
     :llm-approvals-pending (foreign-pstate ipc module-name "$$llm-approvals-pending")
     :llm-approvals-by-run-id (foreign-pstate ipc module-name "$$llm-approvals-by-run-id")
     :llm-token-usage-by-run-id (foreign-pstate ipc module-name "$$llm-token-usage-by-run-id")
     :llm-cost-by-thread (foreign-pstate ipc module-name "$$llm-cost-by-thread")
     :llm-controls-by-run-id (foreign-pstate ipc module-name "$$llm-controls-by-run-id")
     :llm-control-by-id (foreign-pstate ipc module-name "$$llm-control-by-id")
     :llm-views (foreign-pstate ipc module-name "$$llm-views")
     :projection-run-detail (foreign-pstate ipc module-name "$$projection-run-detail")}))

(defn close-llm-runtime!
  [runtime]
  (when-let [ipc (:ipc runtime)]
    (try
      (.close ipc)
      (catch Exception _ nil))))

(defn append-turn-run-request!
  ([runtime request]
   (append-turn-run-request! runtime request :append-ack))
  ([runtime request ack-level]
   (foreign-append! (:llm-depot runtime) request ack-level)
   request))

(defn append-claim!
  ([runtime claim]
   (append-claim! runtime claim :append-ack))
  ([runtime claim ack-level]
   (foreign-append! (:llm-claim-depot runtime) claim ack-level)
   claim))

(defn append-observation!
  ([runtime obs]
   (append-observation! runtime obs :append-ack))
  ([runtime obs ack-level]
   (foreign-append! (:llm-obs-depot runtime) obs ack-level)
   obs))

(defn append-control!
  ([runtime control]
   (append-control! runtime control :append-ack))
  ([runtime control ack-level]
   (foreign-append! (:llm-control-depot runtime) control ack-level)
   control))

(declare read-pending read-run await-materialized await-run)

(defn first-pending-entry
  ([runtime]
   (first-pending-entry runtime pending-task-id))
  ([runtime task-id]
   (some->> (read-pending runtime task-id)
            (sort-by key)
            first
            val)))

(defn await-claim-resolution
  ([runtime claim]
   (await-claim-resolution runtime claim 2000))
  ([runtime claim timeout-ms]
   (await-materialized
     (fn []
       (let [row (read-run runtime (:llm-turn-run/id claim))]
         {:claim claim
          :run row
          :claim-state (claim-state row claim)}))
     #(not= :not-yet-processed (:claim-state %))
     timeout-ms)))

(defn claim-run!
  ([runtime run-id executor-id]
   (claim-run! runtime run-id executor-id {}))
  ([runtime run-id executor-id opts]
   (let [run-row (read-run runtime run-id)
         opts (cond-> opts
                (and run-row (not (opts-executor-task-id opts)))
                (assoc :executor-task-id (:executor/task-id run-row)))
         claim (claim-record run-id
                             (or (:llm-thread/id opts) (:llm-thread/id run-row))
                             executor-id
                             opts)]
     (append-claim! runtime claim)
     (await-claim-resolution runtime claim (or (:timeout-ms opts) 2000)))))

(defn claude-stream-argv
  [run-row & [opts]]
  (let [auth-mode (or (:llm/auth-mode run-row) (:llm/auth-mode opts) :subscription)
        session-id (or (:native/claude-session-id run-row)
                       (:native/thread-id opts))]
    (vec (concat ["claude" "-p"]
                 (when (= :api-key auth-mode) ["--bare"])
                 (when (seq session-id) ["--resume" session-id])
                 ["--input-format" "stream-json"
                  "--output-format" "stream-json"
                  "--include-partial-messages"
                  "--include-hook-events"
                  "--replay-user-messages"]))))

(defn resolve-secret-value
  [secret-handle opts]
  (when secret-handle
    (if-let [resolver (:secret-resolver opts)]
      (resolver secret-handle)
      (:anthropic-api-key opts))))

(defn claude-child-env
  [auth-mode opts]
  (let [base-env (or (:env opts) (System/getenv))
        stripped (apply dissoc (into {} base-env) sensitive-env-keys)]
    (case auth-mode
      :subscription stripped
      :api-key (let [api-key (resolve-secret-value (:anthropic-api-key-secret opts) opts)]
                 (if (str/blank? (str api-key))
                   (throw (ex-info "Claude API-key mode requires a secret handle"
                                   {:llm/backend :claude
                                    :llm/auth-mode :api-key}))
                   (assoc stripped "ANTHROPIC_API_KEY" api-key)))
      (throw (ex-info "Unsupported Claude auth mode"
                      {:llm/backend :claude
                       :llm/auth-mode auth-mode})))))

(defn redacted-env-preview
  [env]
  (into {}
        (map (fn [[k v]]
               [k (if (contains? sensitive-env-keys k)
                    "[REDACTED]"
                    v)]))
        env))

(defn claude-process-spec
  [run-row context-bundle opts]
  (let [auth-mode (or (:llm/auth-mode run-row) (:llm/auth-mode opts) :subscription)
        argv (claude-stream-argv run-row (assoc opts :llm/auth-mode auth-mode))
        child-env (claude-child-env auth-mode opts)]
    {:argv argv
     :cwd (or (:cwd opts) (System/getProperty "user.dir"))
     :env child-env
     :redacted-env (redacted-env-preview child-env)
     :llm/backend :claude
     :llm/auth-mode auth-mode
     :context-bundle/id (:context-bundle/id context-bundle)}))

(defn claude-user-envelope
  [context-bundle]
  {:type "user"
   :message {:role "user"
             :content [{:type "text"
                        :text (str (or (:rendered/model-input context-bundle)
                                       (:prompt/text context-bundle)
                                       ""))}]}})

(defn claude-json-read
  [line]
  (json/read-str line :key-fn keyword))

(defn safe-claude-json-read
  [line]
  (try
    {:ok true :value (claude-json-read line)}
    (catch Throwable t
      {:ok false
       :error {:reason :invalid-json
               :message (.getMessage t)
               :raw-preview (subs (str line) 0 (min 200 (count (str line))))}})))

(defn claude-usage->token-usage
  [usage]
  (cond-> {}
    (number? (:input_tokens usage))
    (assoc :tokens/input-total (:input_tokens usage))
    (number? (:cache_read_input_tokens usage))
    (assoc :tokens/cached-input (:cache_read_input_tokens usage))
    (number? (:output_tokens usage))
    (assoc :tokens/output (:output_tokens usage))))

(defn claude-stream-line->adapter-events
  [state line]
  (let [{:keys [ok value error]} (safe-claude-json-read line)
        sequence-base (:line-index state)
        next-state (update state :line-index (fnil inc 0))
        redacted (when ok (redact-provider-payload value))
        event-id (fn [suffix] (str "claude-line-" sequence-base "-" suffix))
        base (fn [observation-type suffix payload]
               (merge {:observation/type observation-type
                       :observation-id (event-id suffix)
                       :llm/backend :claude
                       :claude/event-type (or (:type value)
                                              (get-in value [:event :type]))
                       :claude/event redacted
                       :provider/native-redacted redacted
                       :raw/json redacted}
                      payload))]
    (if-not ok
      [next-state [(base :claude/run-failed
                         "invalid-json"
                         {:result/status :failed
                          :error error})]]
      (case (:type value)
        "system"
        [(assoc next-state :session-id (:session_id value))
         [(base :claude/system-init
                "system"
                {:native/claude-session-id (:session_id value)})]]

        "assistant"
        (let [text (->> (get-in value [:message :content])
                        (filter #(= "text" (:type %)))
                        (map :text)
                        (str/join "\n"))]
          [next-state
           (cond-> []
             (seq text)
             (conj (base :claude/message
                         "assistant"
                         {:llm-item/id (or (get-in value [:message :id])
                                           (event-id "assistant-item"))
                          :native/item-id (get-in value [:message :id])
                          :item/type :assistant-message
                          :content/text text})))])

        "stream_event"
        (let [inner (:event value)
              inner-type (:type inner)]
          (case inner-type
            "message_start"
            [next-state
             [(base :claude/message
                    "message-start"
                    (merge {:native/item-id (get-in inner [:message :id])
                            :llm-item/id (get-in inner [:message :id])}
                           (claude-usage->token-usage (get-in inner [:message :usage]))))]]

            "message_delta"
            [next-state
             [(base :claude/message
                    "message-delta"
                    (claude-usage->token-usage (:usage inner)))]]

            "content_block_start"
            (let [idx (:index inner)
                  block (:content_block inner)]
              (case (:type block)
                "tool_use"
                [(assoc-in next-state [:tool-id-by-block idx] (:id block))
                 [(base :claude/tool-call
                        (str "tool-call-" idx)
                        {:tool-call/id (:id block)
                         :tool-call/type :claude-tool-use
                         :tool-call/name (:name block)
                         :tool-call/status :requested})]]

                "tool_result"
                [next-state
                 [(base :claude/tool-result
                        (str "tool-result-" idx)
                        {:llm-item/id (or (:tool_use_id block)
                                          (event-id (str "tool-result-item-" idx)))
                         :native/item-id (:tool_use_id block)
                         :item/type :tool-result
                         :tool-call/id (:tool_use_id block)
                         :tool-call/type :claude-tool-use
                         :tool-call/status :completed
                         :content/text (str (:content block))})]]

                [next-state
                 [(base :claude/stream-delta
                        (str "content-block-start-" idx)
                        {:item/type :assistant-event
                         :content/text ""})]]))

            "content_block_delta"
            (let [idx (:index inner)
                  delta (:delta inner)]
              (case (:type delta)
                "text_delta"
                [next-state
                 [(base :claude/stream-delta
                        (str "text-delta-" idx)
                        {:llm-item/id (event-id (str "text-" idx))
                         :item/type :assistant-message
                         :content/text (:text delta)})]]

                "input_json_delta"
                [next-state
                 [(base :claude/tool-call
                        (str "tool-input-" idx)
                        {:tool-call/id (get-in state [:tool-id-by-block idx])
                         :tool-call/type :claude-tool-use
                         :tool-call/status :input-delta
                         :content/text (:partial_json delta)})]]

                [next-state
                 [(base :claude/stream-delta
                        (str "delta-" idx)
                        {:item/type :assistant-event
                         :content/text (str delta)})]]))

            "content_block_stop"
            [next-state
             [(base :claude/stream-delta
                    (str "block-stop-" (:index inner))
                    {:item/type :assistant-event
                     :content/text ""})]]

            [next-state
             [(base :claude/message
                    (str "stream-" inner-type)
                    {})]]))

        "hook_event"
        [next-state
         [(base :claude/hook-event
                "hook"
                {:approval/id (:approval_id value)
                 :approval/type (keyword (or (:permission_type value) "tool"))
                 :content/text (str (:message value))})]]

        "control_response"
        [next-state [(base :claude/control-response "control" {})]]

        "rate_limit"
        [next-state
         [(base :claude/rate-limit
                "rate-limit"
                {:result/status :failed
                 :error {:reason :claude/rate-limit
                         :message (:message value)}})]]

        "auth"
        [next-state
         [(base :claude/auth
                "auth"
                {:result/status :failed
                 :error {:reason :claude/auth
                         :message (:message value)}})]]

        "api_retry"
        [next-state [(base :claude/api-retry "api-retry" {})]]

        "compact"
        [next-state [(base :claude/compact-boundary "compact" {})]]

        "compact_boundary"
        [next-state [(base :claude/compact-boundary "compact-boundary" {})]]

        "source_boundary"
        [next-state [(base :claude/source-boundary "source-boundary" {})]]

        "tombstone"
        [next-state [(base :claude/tombstone "tombstone" {})]]

        "result"
        [next-state
         [(base :claude/result
                "result"
                {:native/claude-session-id (:session_id value)
                 :result/status (if (:is_error value) :failed :succeeded)
                 :result/cost-usd (:total_cost_usd value)
                 :result/text (:result value)
                 :content/text (or (:result value) "")})]]

        [next-state
         [(base :claude/message
                (str "unknown-" (:type value))
                {})]]))))

(defn claude-stream-json-lines->events
  [lines]
  (:events
   (reduce (fn [{:keys [state events]} line]
             (let [[state* emitted] (claude-stream-line->adapter-events state line)]
               {:state state*
                :events (into events emitted)}))
           {:state {:line-index 0
                    :tool-id-by-block {}}
            :events []}
           lines)))

(defn stream-lines
  [stream]
  (with-open [reader (BufferedReader. (InputStreamReader. stream))]
    (doall (line-seq reader))))

(defn write-claude-input!
  [process context-bundle]
  (with-open [writer (OutputStreamWriter. (.getOutputStream process) "UTF-8")]
    (.write writer (json/write-str (claude-user-envelope context-bundle)))
    (.write writer "\n")
    (.flush writer)))

(defn run-claude-process->events
  [run-row context-bundle opts]
  (let [{:keys [argv cwd env]} (claude-process-spec run-row context-bundle opts)
        pb (ProcessBuilder. ^java.util.List argv)
        timeout-ms (long (or (:timeout-ms opts) default-claude-timeout-ms))]
    (when-not (str/blank? cwd)
      (.directory pb (File. cwd)))
    (let [process-env (.environment pb)]
      (.clear process-env)
      (doseq [[k v] env]
        (.put process-env k v)))
    (try
      (let [process (.start pb)
            _ (write-claude-input! process context-bundle)
            stdout-lines (future (stream-lines (.getInputStream process)))
            stderr-lines (future (stream-lines (.getErrorStream process)))
            exited? (.waitFor process timeout-ms TimeUnit/MILLISECONDS)
            exit-code (if exited?
                        (.exitValue process)
                        (do
                          (.destroyForcibly process)
                          124))
            events (claude-stream-json-lines->events @stdout-lines)]
        (cond-> events
          (not (zero? exit-code))
          (conj {:observation/type :claude/run-failed
                 :observation-id (str (:llm-turn-run/id run-row) "/claude-exit")
                 :llm/backend :claude
                 :result/status :failed
                 :error {:reason :claude/exit-nonzero
                         :exit-code exit-code
                         :stderr-tail (take-last 20 @stderr-lines)}
                 :raw/json {}})))
      (catch Throwable t
        [{:observation/type :claude/run-failed
          :observation-id (str (:llm-turn-run/id run-row) "/claude-spawn-error")
          :llm/backend :claude
          :result/status :failed
          :error {:reason :claude/spawn-error
                  :message (.getMessage t)}
          :raw/json {}}]))))

(defn claude-stream-json-adapter
  [& [opts]]
  {:run-turn (fn [{:keys [run context-bundle]}]
               (if-let [lines (:lines opts)]
                 (claude-stream-json-lines->events lines)
                 (run-claude-process->events run context-bundle opts)))})

(defn fake-codex-adapter
  [events]
  {:run-turn (fn [_ctx] events)})

(defn run-adapter-turn
  [adapter ctx]
  (cond
    (fn? adapter) (adapter ctx)
    (and (map? adapter) (fn? (:run-turn adapter))) ((:run-turn adapter) ctx)
    :else (throw (ex-info "Invalid Codex adapter" {:adapter adapter}))))

(defn adapter-event->observation
  [run-row sequence event]
  (let [event (assoc event
                     :observation-id (or (:observation-id event)
                                         (:observation/id event)
                                         (str (:llm-turn-run/id run-row) "/adapter-obs-" sequence)))]
    (observation
      (:llm-turn-run/id run-row)
      (:llm-thread/id run-row)
      (:observation/type event)
      sequence
      event)))

(defn run-one-pending-with-adapter!
  [runtime {:keys [task-id executor-id adapter load-context-bundle timeout-ms]
            :or {task-id pending-task-id
                 executor-id "llm-executor-local"
                 timeout-ms 2000}}]
  (when-let [pending-entry (first-pending-entry runtime task-id)]
    (let [run-id (:llm-turn-run/id pending-entry)
          pending-run-row (read-run runtime run-id)
          claim-result (when-not (fork-binding-required? pending-run-row)
                         (claim-run! runtime run-id executor-id {:timeout-ms timeout-ms
                                                                 :executor-task-id task-id}))
          granted? (= :granted-to-us (:claim-state claim-result))
          run-row (:run claim-result)
          bundle (when (and granted? load-context-bundle)
                   (load-context-bundle (:context-bundle/id run-row)))]
      (cond
        (fork-binding-required? pending-run-row)
        {:run pending-run-row
         :pending pending-entry
         :spawned? false
         :observations-appended 0
         :reason :fork-binding-not-durable}

        (not granted?)
        (assoc claim-result :spawned? false :observations-appended 0)

        :else
        (let [ctx {:run run-row
                   :pending pending-entry
                   :claim (:claim claim-result)
                   :context-bundle bundle}
              events (vec (run-adapter-turn adapter ctx))
              observations (map-indexed #(adapter-event->observation run-row %1 %2)
                                        events)]
          (doseq [obs observations]
            (append-observation! runtime obs))
          {:claim (:claim claim-result)
           :claim-state (:claim-state claim-result)
           :run run-row
           :context-bundle bundle
           :spawned? true
           :spawned-after-grant? true
           :observations-appended (count observations)
           :observations (vec observations)})))))

(defn run-one-pending-with-claude!
  [runtime opts]
  (run-one-pending-with-adapter!
    runtime
    (merge {:task-id pending-task-id
            :executor-id "claude-executor-local"
            :adapter (claude-stream-json-adapter opts)}
           opts)))

(defn stale-approval-control
  [run-row approval opts]
  (control-record
    (:llm-turn-run/id run-row)
    :approval/resolve
    {:control-id (or (:control-id opts)
                     (str (:llm-turn-run/id run-row)
                          "/stale-approval/"
                          (:approval/id approval)))
     :llm-thread/id (:llm-thread/id run-row)
     :world-thread/id (:world-thread/id run-row)
     :world-turn/id (:world-turn/id run-row)
     :approval/id (:approval/id approval)
     :native/json-rpc-request-id (:native/json-rpc-request-id approval)
     :decision :expired
     :actor (or (:actor opts) {:actor/id "llm-executor" :actor/type :system})
     :time-ms (or (:time-ms opts) (now-ms))
     :reason (or (:reason opts) :executor-stale)
     :payload {:executor/id (:executor-id opts)
               :run/restart-policy (:run/restart-policy run-row)}}))

(defn mark-stale-approvals!
  ([runtime run-id]
   (mark-stale-approvals! runtime run-id {}))
  ([runtime run-id opts]
   (let [run-row (read-run runtime run-id)
         approvals (vec (vals (:approvals-pending run-row)))]
     (doseq [approval approvals]
       (append-control! runtime (stale-approval-control run-row approval opts)))
     (when (seq approvals)
       (await-run runtime run-id #(contains? terminal-statuses (:status %))))
     {:llm-turn-run/id run-id
      :run/restart-policy (:run/restart-policy run-row)
      :stale-approval-ids (mapv :approval/id approvals)
      :action (if (seq approvals) :failed :none)})))

(defn select-pstate-one
  [pstate path]
  (first (foreign-select path pstate)))

(defn read-thread
  [runtime thread-id]
  (select-pstate-one (:llm-threads runtime) [(keypath thread-id)]))

(defn read-thread-binding
  [runtime world-thread-id]
  (select-pstate-one (:llm-thread-by-world-thread runtime) [(keypath world-thread-id)]))

(defn read-run
  [runtime run-id]
  (select-pstate-one (:llm-turn-runs runtime) [(keypath run-id)]))

(defn read-run-for-world-turn
  [runtime world-turn-id]
  (select-pstate-one (:llm-turn-run-by-world-turn runtime) [(keypath world-turn-id)]))

(defn read-decision
  [runtime run-id]
  (select-pstate-one (:llm-decisions-by-run-id runtime) [(keypath run-id)]))

(defn read-view
  [runtime run-id]
  (select-pstate-one (:llm-views runtime) [(keypath run-id)]))

(defn read-run-detail-projection
  [runtime run-id]
  (select-pstate-one (:projection-run-detail runtime) [(keypath run-id)]))

(defn read-pending
  ([runtime]
   (read-pending runtime pending-task-id))
  ([runtime task-id]
   (or (select-pstate-one (:llm-pending-by-task runtime) [(keypath task-id)])
       {})))

(defn read-items-by-run
  [runtime run-id]
  (or (select-pstate-one (:llm-items-by-turn-run runtime) [(keypath run-id)])
      {}))

(defn read-items-by-thread
  [runtime thread-id]
  (or (select-pstate-one (:llm-items-by-thread runtime) [(keypath thread-id)])
      {}))

(defn read-item-by-id
  [runtime item-id]
  (select-pstate-one (:llm-item-by-id runtime) [(keypath item-id)]))

(defn read-runs-by-thread
  [runtime thread-id]
  (or (select-pstate-one (:llm-turn-runs-by-thread runtime) [(keypath thread-id)])
      {}))

(defn read-raw-response-items
  [runtime run-id]
  (or (select-pstate-one (:llm-raw-response-items runtime) [(keypath run-id)])
      {}))

(defn read-tool-calls-by-run
  [runtime run-id]
  (or (select-pstate-one (:llm-tool-calls-by-run-id runtime) [(keypath run-id)])
      {}))

(defn read-approvals-by-run
  [runtime run-id]
  (or (select-pstate-one (:llm-approvals-by-run-id runtime) [(keypath run-id)])
      {}))

(defn read-controls-by-run
  [runtime run-id]
  (or (select-pstate-one (:llm-controls-by-run-id runtime) [(keypath run-id)])
      {}))

(defn read-control
  [runtime control-id]
  (select-pstate-one (:llm-control-by-id runtime) [(keypath control-id)]))

(defn read-token-usage
  [runtime run-id]
  (or (select-pstate-one (:llm-token-usage-by-run-id runtime) [(keypath run-id)])
      {}))

(defn read-cost-by-thread
  [runtime thread-id]
  (or (select-pstate-one (:llm-cost-by-thread runtime) [(keypath thread-id)])
      {}))

(defn read-pending-approval
  [runtime approval-id]
  (select-pstate-one (:llm-approvals-pending runtime) [(keypath approval-id)]))

(defn await-materialized
  ([read-f pred]
   (await-materialized read-f pred 2000))
  ([read-f pred timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop [value (read-f)]
       (cond
         (pred value) value
         (>= (System/currentTimeMillis) deadline) value
         :else (do
                 (Thread/sleep 25)
                 (recur (read-f))))))))

(defn await-decision
  [runtime run-id]
  (await-materialized #(read-decision runtime run-id) some?))

(defn await-run
  ([runtime run-id]
   (await-run runtime run-id some?))
  ([runtime run-id pred]
   (await-materialized #(read-run runtime run-id) pred)))

(defn await-view
  ([runtime run-id pred]
   (await-view runtime run-id pred 2000))
  ([runtime run-id pred timeout-ms]
   (await-materialized #(read-view runtime run-id) pred timeout-ms)))
