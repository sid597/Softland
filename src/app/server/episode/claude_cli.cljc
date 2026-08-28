(ns app.server.episode.claude-cli
  "Claude command construction and response parsing.
   Takes: prompts, session ids, model settings, and Claude JSON output.
   Gives: command argument vectors and normalized response maps.
   Holds nothing."
  (:require [clojure.string :as str]
            [cheshire.core :as json]))

(defn provider-default-argv
  "Build argv when client does not send raw argv.
   Session support is best-effort per provider.
   Optional :output-format overrides Claude's default (\"json\").
   When streaming, pass :output-format \"stream-json\" :include-partials? true.
   Optional :allowed-tools is a seq of tool patterns (e.g. [\"mcp__linear-server__*\"]).
   Optional :json-schema is a JSON string for --json-schema (structured output).
   Optional :max-budget-usd caps API spend (only works with -p/--print).
   Optional :model overrides the default model.
   Optional :append-system-prompt appends to the system prompt."
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
  "Parse Claude's JSON output to extract session-id and text content.
   Handles two formats:
   1. Object: {\"type\":\"result\", \"session_id\":\"...\", \"result\":\"...\"}
   2. Array:  [{\"type\":\"system\",\"session_id\":\"...\",...}, ..., {\"type\":\"message\",\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"...\"}]}]"
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
