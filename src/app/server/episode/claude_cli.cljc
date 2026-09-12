(ns app.server.episode.claude-cli
  "Pure provider argv construction and normalization of complete Claude JSON.
   Accepts provider, prompt, session and option values; returns argv or
   {:session-id :content}. Owns no process or state and performs no execution.
   No caller was found in src/, src-inland/, bin/ or test/. The active episode
   and claimed LLM paths use their own builders in episode.clj and llm.clj."
  (:require [clojure.string :as str]
            [cheshire.core :as json]))

(defn provider-default-argv
  "Build argv for :claude, :codex or :gemini; unknown providers use echo.
   Claude accepts output-format (default json), partials, repeated allowed-tool
   patterns, JSON schema, budget, model and appended system prompt. These become
   flags without validation. Claude/Gemini use --resume for a nonempty session;
   Codex uses only exec and prompt. The caller owns spawning and exit handling."
  [provider prompt session-id & {:keys [output-format include-partials? allowed-tools
                                         json-schema max-budget-usd model append-system-prompt]}]
  (case provider
    :claude (vec (concat ["claude"]
                         (when (seq session-id) ["--resume" session-id])
                         ["-p" (or prompt "") "--output-format" (or output-format "json")]
                         (when include-partials? ["--include-partial-messages"])
                         (mapcat (fn [t] ["--allowedTools" t]) allowed-tools)
                         (when json-schema ["--json-schema" json-schema])
                         (when max-budget-usd ["--max-budget-usd" (str max-budget-usd)])
                         (when model ["--model" model])
                         (when append-system-prompt ["--append-system-prompt" append-system-prompt])))
    :codex (vec ["codex" "exec" (or prompt "")])
    :gemini (vec (concat ["gemini"]
                         (when (seq session-id) ["--resume" session-id])
                         ["-p" (or prompt "") "--output-format" "text"]))
    (vec ["echo" (str "Unknown provider: " provider)])))

(defn parse-claude-json-output
  "Normalize a Claude JSON result object or message array to :session-id and
   :content. Arrays use the first system session and last assistant message
   text parts; objects use :result. Missing content falls back to raw-output.
   Parse/extraction exceptions return {:session-id nil :content raw-output}.
   This consumes a complete JSON value, not a stream of JSON lines."
  [raw-output]
  (try
    (let [parsed (json/parse-string raw-output true)]
      (if (vector? parsed)
        ;; Array format: extract session-id from system init, content from last assistant message
        (let [init-msg (first (filter #(= (:type %) "system") parsed))
              assistant-msgs (filter #(and (= (:type %) "message")
                                           (= (:role %) "assistant"))
                                     parsed)
              last-assistant (last assistant-msgs)
              text-content (->> (:content last-assistant)
                                (filter #(= (:type %) "text"))
                                (map :text)
                                (str/join "\n"))]
          {:session-id (:session_id init-msg)
           :content (if (seq text-content) text-content raw-output)})
        ;; Object format: direct extraction
        {:session-id (:session_id parsed)
         :content (or (:result parsed) raw-output)}))
    (catch Exception _
      {:session-id nil :content raw-output})))
