(ns app.client.workspace.agent
  "Agent streaming: ticket parsing and SSE stream management."
  (:require [clojure.string :as str]
            [cljs.reader :as reader]))

(def priority-label->num
  {"urgent" 1 "high" 2 "medium" 3 "low" 4 "none" 0
   "1" 1 "2" 2 "3" 3 "4" 4 "0" 0})

(defn normalize-priority
  "Coerce a priority value (number, string label, or string digit) to an int 0-4."
  [p]
  (cond
    (number? p) (int p)
    (string? p) (or (get priority-label->num (str/lower-case p)) 0)
    :else 0))

(defn normalize-ticket
  "Map a raw issue object (from Linear MCP or Claude JSON) to our ticket shape.
   Handles both Linear native fields and pre-formatted fields."
  [t]
  {:id (or (:identifier t) (:id t) "UNKNOWN")
   :title (or (:title t) "Untitled")
   :status (or (get-in t [:state :name]) (:status t) "unknown")
   :assignee (or (get-in t [:assignee :name]) (:assignee t) "unassigned")
   :priority (normalize-priority (:priority t))
   :description (or (:description t) "")})

(defn parse-tickets-from-trail
  "Extract tickets from trail :tool-result events (raw MCP responses).
   Tries to parse each tool result as JSON containing an array of issues.
   Returns [{:id :title :status ...}] or nil."
  [trail]
  (let [tool-results (->> trail
                          (filter #(= :tool-result (:kind %)))
                          (mapv :content))
        ;; Try each tool result — the Linear MCP response is usually a JSON array
        tickets (some (fn [content]
                        (when (string? content)
                          (try
                            (let [parsed (js/JSON.parse content)
                                  data (js->clj parsed :keywordize-keys true)
                                  ;; Handle both direct array and {:issues [...]} wrapper
                                  arr (cond
                                        (vector? data) data
                                        (vector? (:issues data)) (:issues data)
                                        (vector? (:nodes data)) (:nodes data)
                                        :else nil)]
                              (when (and (seq arr) (or (:title (first arr))
                                                       (:identifier (first arr))))
                                (mapv normalize-ticket arr)))
                            (catch :default _ nil))))
                      tool-results)]
    tickets))

(defn parse-tickets-from-output
  "Fallback: extract a JSON ticket array from agent text output.
   Tries ```json code block first, then bracket-matching.
   Returns [{:id :title :status :assignee :priority}] or nil."
  [output-text]
  (when (and output-text (not (str/blank? output-text)))
    (let [json-block-re #"(?s)```json\s*\n?(.*?)\n?\s*```"
          match1 (re-find json-block-re output-text)
          json-str (if match1
                     (second match1)
                     (let [start (str/index-of output-text "[")]
                       (when start
                         (loop [i start depth 0 max-i (min (count output-text) (+ start 50000))]
                           (if (>= i max-i)
                             nil
                             (let [c (.charAt output-text i)
                                   new-depth (cond (= c \[) (inc depth)
                                                   (= c \]) (dec depth)
                                                   :else depth)]
                               (if (zero? new-depth)
                                 (subs output-text start (inc i))
                                 (recur (inc i) new-depth max-i))))))))]
      (when json-str
        (try
          (let [parsed (js/JSON.parse json-str)
                arr (js->clj parsed :keywordize-keys true)]
            (when (vector? arr)
              (mapv normalize-ticket arr)))
          (catch :default e
            (js/console.warn "[FLOW] Failed to parse tickets JSON:" (.-message e))
            nil))))))

(def ticket-json-schema
  "JSON schema for Claude CLI --json-schema flag. Validates structured ticket output."
  (js/JSON.stringify
    (clj->js {:type "object"
              :properties {:tickets {:type "array"
                                     :items {:type "object"
                                             :properties {:id {:type "string"}
                                                          :title {:type "string"}
                                                          :status {:type "string"}
                                                          :assignee {:type "string"}
                                                          :priority {:type "string"}
                                                          :description {:type "string"}}
                                             :required ["title" "status"]}}}
              :required ["tickets"]})))

(defn parse-structured-result
  "Parse the structured result from Claude CLI --json-schema output.
   Returns [{:id :title :status ...}] or nil."
  [structured-result]
  (when structured-result
    (try
      (let [parsed (if (string? structured-result)
                     (js/JSON.parse structured-result)
                     (clj->js structured-result))
            data (js->clj parsed :keywordize-keys true)
            tickets (:tickets data)]
        (when (seq tickets)
          (mapv normalize-ticket tickets)))
      (catch :default e
        (js/console.warn "[FLOW] Failed to parse structured result:" (.-message e))
        nil))))

(defn stream-agent-run!
  "Streaming fetch: POST to url, read SSE events via ReadableStream.
   Calls (on-event edn-map) for each parsed SSE event.
   Calls (on-error err) on failure. Returns nil."
  [url body on-event on-error]
  (-> (js/fetch url
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/edn"}
                          :body (pr-str body)}))
      (.then
        (fn [resp]
          (if-not (.-ok resp)
            (on-error (js/Error. (str "HTTP " (.-status resp))))
            (let [rdr    (.getReader (.-body resp))
                  !buf   (atom "")]
              (letfn [(pump []
                        (-> (.read rdr)
                            (.then
                              (fn [result]
                                (if (.-done result)
                                  ;; Stream ended — flush any remaining buffer
                                  (let [remaining @!buf]
                                    (when (seq remaining)
                                      (doseq [chunk (str/split remaining #"\n\n")]
                                        (let [trimmed (str/trim chunk)]
                                          (when (str/starts-with? trimmed "data: ")
                                            (try
                                              (on-event (reader/read-string (subs trimmed 6)))
                                              (catch :default _ nil)))))))
                                  ;; Got a chunk — decode + split on SSE boundary
                                  (let [text  (.decode (js/TextDecoder.) (.-value result))
                                        buf   (swap! !buf str text)
                                        parts (str/split buf #"\n\n" -1)]
                                    ;; All parts except the last are complete events
                                    (reset! !buf (peek parts))
                                    (doseq [part (pop parts)]
                                      (let [trimmed (str/trim part)]
                                        (when (str/starts-with? trimmed "data: ")
                                          (try
                                            (on-event (reader/read-string (subs trimmed 6)))
                                            (catch :default _ nil)))))
                                    (pump)))))
                            (.catch (fn [e] (on-error e)))))]
                (pump))))))
      (.catch (fn [e] (on-error e))))
  nil)


