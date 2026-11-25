(ns softland.cli.mcp
  "The MCP server of the program, `softland mcp`: the words of the server
  as tools for an agent's harness, over stdio. JSON-RPC 2.0, one JSON
  object on each line of standard input, one answer on each line of
  standard output, and nothing else on standard output; every message for
  the person goes to standard error.

  Methods. `initialize` answers with the protocol version that the client
  sent, the capability of tools and the server's name; `ping` answers {};
  `tools/list` gives `tools`; `tools/call` does one tool. Another method
  with an id answers the error -32601; a notification, a message with no
  id, gets no answer, whatever it is.

  A tool takes each argument that is data as a string of EDN text and
  gives {content [{type text, text}], isError}: the text is EDN, one form
  on each line, as the server wrote it, or a sentence when the server could not be reached or no
  credential is saved. `isError` is true when the server's status was not
  200 or the result holds `:could-not`. The tools are the steps of
  `softland.cli.commands`, with the credential that is saved for the
  server; each call reads the credentials file again.

  State. The context, fixed at the start: the server's URL and the path of
  the credentials file. Calls run at the same time and answer in the order
  in which they end; each answer carries its request's id."
  (:require [clojure.string :as str]
            [softland.cli.commands :as commands]
            [softland.cli.node :as node]
            [softland.cli.pure :as pure]))

;; ---------------------------------------------------------------- the tools

(def ^:private forms-text
  "What every tool's description says of the EDN forms."
  (str "Each argument that is data is a string of EDN text. Ids (an entity, a lane) are keywords "
       "such as :chair-7 or UUIDs written #uuid \"...\"; keys are keywords; a value is any EDN value "
       "(nil, true, a string, a keyword, a UUID, a number, a map, a vector, a set). "))

(def tools
  "The tools, as MCP lists them: name, description and the JSON Schema of
  the arguments."
  [{:name "who_am_i"
    :description (str "Who the saved credential stands for. Gives {:who #uuid \"..\" :session #uuid \"..\" "
                      ":until ms :own-lane #uuid \"..\"}. Call it first: :who and :own-lane are what an act "
                      "in her own lane needs. Takes no argument.")
    :inputSchema {:type "object" :properties {}}}
   {:name "write"
    :description (str "Writes one fact into her own lane and gives {:receipt .. :answer ..}. The simplest "
                      "way to write. It builds the act {:version 1 :lane own :permission [who own own] "
                      ":facts [{:e entity :k key :v value}]}, asks an act-id, sends, and waits up to 5 s for "
                      "the answer: {:answer :accepted :stamp n :time ms} or {:answer :refused :reason word ..}; "
                      "{:answer :nothing-yet} means ask again with the tool answer. " forms-text
                      "Example: entity \":chair-7\", key \":colour\", value \":green\".")
    :inputSchema {:type "object"
                  :properties {:entity {:type "string" :description "EDN text of the entity id, e.g. :chair-7"}
                               :key {:type "string" :description "EDN text of the key, a keyword, e.g. :colour"}
                               :value {:type "string" :description "EDN text of the value, e.g. :green or \"a text\""}}
                  :required ["entity" "key" "value"]}}
   {:name "read"
    :description (str "A read, once. The query is a map WITHOUT :asker (the server adds it): "
                      "{:asks Q :lanes [lane ...]} or {:asks Q :from-lane lane}, optionally with "
                      ":with #{:writer :permission :time}. Q is one of [:place entity key], [:entity entity], "
                      "[:run first-end last-end], [:points-at key thing]. Gives {:lanes [..] :left-out [..] "
                      ":place {..}} or {.. :places {[e k] {..}}}. " forms-text
                      "Example: {:asks [:place :chair-7 :colour] :from-lane #uuid \"<own-lane>\"}.")
    :inputSchema {:type "object"
                  :properties {:query {:type "string" :description "EDN text of the query"}}
                  :required ["query"]}}
   {:name "watch"
    :description (str "A read kept live, for a while. Opens the stream of the query, collects what arrives "
                      "within wait_ms (default 2000, at most 55000), closes it, and gives the messages, one "
                      "EDN form per line: first {:anew answer}, then {:changed {[entity key] place-answer}} "
                      "or {:anew answer} as things change, and {:ended word} if the server ended it. A last "
                      "line {:still-open true} means the stream had not ended. The query is as for read "
                      "(no :asker). Example: query {:asks [:entity :chair-7] :from-lane #uuid \"<own-lane>\"}, "
                      "wait_ms 3000.")
    :inputSchema {:type "object"
                  :properties {:query {:type "string" :description "EDN text of the query"}
                               :wait_ms {:type "integer" :description "how long to collect, in ms (default 2000, at most 55000)"}}
                  :required ["query"]}}
   {:name "act"
    :description (str "Sends any act and gives {:receipt .. :answer ..}: asks one act-id of the act's :lane, "
                      "sends the act under it, and waits up to 5 s for the answer. An act is a map WITHOUT "
                      ":who and :session (the server adds them; an act holding either is refused): "
                      "{:version 1 :lane L :permission [who L L] :facts [{:e entity :k key :v value} ...]}. "
                      "In her own lane the permission is [who own-lane own-lane], from who_am_i. " forms-text
                      "Example: {:version 1 :lane #uuid \"<own>\" :permission [#uuid \"<who>\" #uuid \"<own>\" "
                      "#uuid \"<own>\"] :facts [{:e :chair-7 :k :colour :v :green}]}.")
    :inputSchema {:type "object"
                  :properties {:act {:type "string" :description "EDN text of the act"}}
                  :required ["act"]}}
   {:name "act_ids"
    :description (str "Asks the server for act-ids of a lane, ahead of sending. Gives [[lane depot #uuid \"..\"] ...]. "
                      "Use it with send when you want to hold the act-id before sending. " forms-text
                      "Example: lane \"#uuid \\\"<own-lane>\\\"\", count 2.")
    :inputSchema {:type "object"
                  :properties {:lane {:type "string" :description "EDN text of the lane id"}
                               :count {:type "integer" :description "how many act-ids (default 1)"}}
                  :required ["lane"]}}
   {:name "send"
    :description (str "Sends one act and gives the receipt {:act-id [..] :digest \"64 hex\" :ack :gate-ran | :depot-holds}. "
                      "The act is as for the tool act (no :who, no :session). act_id, optional, is an act-id "
                      "from act_ids, as EDN text; sending again under the same act-id is safe. Read the "
                      "answer with the tool answer. Example: act \"{:version 1 :lane ... :facts [...]}\".")
    :inputSchema {:type "object"
                  :properties {:act {:type "string" :description "EDN text of the act"}
                               :act_id {:type "string" :description "EDN text of an act-id, [lane depot #uuid \"..\"]"}}
                  :required ["act"]}}
   {:name "answer"
    :description (str "The answer of a sent act, by its act-id and the digest of its receipt. Gives "
                      "{:answer :accepted :stamp n :time ms}, {:answer :refused :reason word ..}, "
                      "{:answer :nothing-yet} (ask again, with a wait) or {:answer :not-mine}. wait_ms, "
                      "optional, holds the request until the answer comes (at most 60000). Example: act_id "
                      "\"[#uuid \\\"..\\\" :streaming #uuid \\\"..\\\"]\", digest \"3fa0...\", wait_ms 5000.")
    :inputSchema {:type "object"
                  :properties {:act_id {:type "string" :description "EDN text of the act-id from the receipt"}
                               :digest {:type "string" :description "the receipt's digest, 64 hex characters"}
                               :wait_ms {:type "integer" :description "how long to wait for the answer, in ms"}}
                  :required ["act_id" "digest"]}}])

;; ------------------------------------------------------------ the results

(defn- result
  "Takes a text and whether it is an error. Gives the result of a tool
  call, as JSON-RPC carries it."
  [text error?]
  {:content [{:type "text" :text text}] :isError (boolean error?)})

(defn- outcome-result
  "Takes a context and an outcome. Gives the result of a tool call: the
  form's EDN text, or the failure's sentence; with a line that tells the
  person to sign in when the server found no session."
  [ctx outcome]
  (let [text (if (:failure outcome)
               (:failure outcome)
               (pure/text-of outcome))]
    (result (cond-> text
              (pure/needs-sign-in? outcome) (str "\n;; " (commands/sign-in-hint ctx)))
            (not (:fine? outcome)))))

(defn- arguments-read
  "Takes the arguments of a call, as a Clojure map with keyword keys, and
  the names of the EDN arguments, each with whether it is needed. Gives
  [:ok {name verbatim} {name form}]: each argument as its text, which is
  what a word carries (`softland.cli.pure/verbatim`), and as the form read,
  used only to take one small thing out; or [:no text]: an argument that
  is needed and absent, that is no string, or that is no EDN."
  [args specs]
  (reduce (fn [[_ read forms] [k needed?]]
            (let [v (get args k)]
              (cond
                (and (nil? v) (not needed?)) [:ok read forms]
                (nil? v) (reduced [:no (str "the argument " (name k) " is needed")])
                (not (string? v)) (reduced [:no (str "the argument " (name k) " must be a string of EDN text")])
                :else (let [[outcome form] (pure/read-edn v)]
                        (if (= :ok outcome)
                          [:ok (assoc read k (pure/verbatim (str/trim v))) (assoc forms k form)]
                          (reduced [:no (str "the argument " (name k) ": " form)]))))))
          [:ok {} {}]
          specs))

(defn- whole
  "Takes an argument that should be a whole number: a number or a text of
  digits. Gives the number, or nil."
  [v]
  (cond
    (and (number? v) (js/Number.isInteger v) (<= 0 v)) v
    (and (string? v) (re-matches #"[0-9]+" v)) (js/parseInt v 10)
    :else nil))

(def ^:private edn-arguments
  "The EDN arguments of each tool, each with whether it is needed."
  {"who_am_i" {}
   "act_ids" {:lane true}
   "send" {:act true :act_id false}
   "answer" {:act_id true}
   "act" {:act true}
   "write" {:entity true :key true :value true}
   "read" {:query true}
   "watch" {:query true}})

(defn- call-tool
  "Takes the context, a tool's name and its arguments, a Clojure map with
  keyword keys. Does the tool with the saved credential. Gives a promise of
  the call's result; a result with `isError` when no credential is saved,
  when an argument is wrong, or when the server could not do it."
  [ctx tool-name args]
  (let [[outcome read forms] (arguments-read args (get edn-arguments tool-name))
        act-id (atom nil)
        on-act-id (fn [id]
                    (reset! act-id id)
                    (node/err! (str "softland mcp: act-id " (pure/edn-text id))))]
    (cond
      (= :no outcome) (js/Promise.resolve (result read true))
      (nil? (commands/saved ctx)) (js/Promise.resolve (result (commands/no-credential-text ctx) true))
      :else
      (case tool-name
        "who_am_i" (.then (commands/say ctx {:word :who-am-i}) #(outcome-result ctx (pure/outcome-of %)))
        "act_ids" (.then (commands/say ctx {:word :act-ids :lane (:lane read) :count (or (whole (:count args)) 1)})
                         #(outcome-result ctx (pure/outcome-of %)))
        "send" (.then (commands/say ctx (cond-> {:word :send :act (:act read)}
                                          (contains? read :act_id) (assoc :act-id (:act_id read))))
                      #(outcome-result ctx (pure/outcome-of %)))
        "answer" (if-not (string? (:digest args))
                   (js/Promise.resolve (result "the argument digest is needed, as a string" true))
                   (.then (commands/say ctx (cond-> {:word :answer :act-id (:act_id read) :digest (:digest args)}
                                              (whole (:wait_ms args)) (assoc :wait-ms (whole (:wait_ms args)))))
                          #(outcome-result ctx (pure/outcome-of %))))
        ("act" "write")
        (-> (if (= "act" tool-name)
              (commands/act-outcome ctx (:act read) (when (map? (:act forms)) (:lane (:act forms))) on-act-id)
              (commands/write-outcome ctx (:entity read) (:key read) (:value read) on-act-id))
            (.then (fn [outcome]
                     (let [r (outcome-result ctx outcome)]
                       (if (and @act-id (not (:fine? outcome)))
                         (update-in r [:content 0 :text] str "\n" (pure/edn-text {:act-id @act-id}))
                         r)))))
        "read" (.then (commands/say ctx {:word :read :query (:query read)}) #(outcome-result ctx (pure/outcome-of %)))
        "watch" (let [span (min pure/watch-longest-ms (or (whole (:wait_ms args)) pure/watch-default-ms))]
                  (.then (commands/watch-collected ctx (:query read) span)
                         (fn [{:keys [failure lines still-open fine?]}]
                           (if failure
                             (result failure true)
                             (result (->> (cond-> lines
                                            still-open (conj (pure/edn-text {:still-open true})))
                                          (interpose "\n")
                                          (apply str))
                                     (not fine?))))))))))

;; ---------------------------------------------------------------- JSON-RPC

(defn- answer!
  "Takes an id and either a result or an error, a Clojure map. Writes the
  JSON-RPC answer on one line of standard output."
  [id {:keys [result error]}]
  (node/out! (js/JSON.stringify
              (clj->js (cond-> {:jsonrpc "2.0" :id id}
                         error (assoc :error error)
                         (not error) (assoc :result result))))))

(defn- handle
  "Takes the context and one request, read from JSON into a Clojure map
  with keyword keys. Gives a promise of {:result ..} or {:error ..}."
  [ctx {:keys [method params]}]
  (let [params (if (map? params) params {})]
    (case method
      "initialize" (js/Promise.resolve
                    {:result {:protocolVersion (or (:protocolVersion params) "2025-06-18")
                              :capabilities {:tools {}}
                              :serverInfo {:name "softland" :version "0.1.0"}}})
      "ping" (js/Promise.resolve {:result {}})
      "tools/list" (js/Promise.resolve {:result {:tools tools}})
      "tools/call" (let [tool-name (:name params)
                         args (:arguments params)]
                     (if-not (contains? edn-arguments tool-name)
                       (js/Promise.resolve {:error {:code -32602 :message (str "no tool is called " tool-name)}})
                       (-> (call-tool ctx tool-name (if (map? args) args {}))
                           (.then (fn [r] {:result r})))))
      (js/Promise.resolve {:error {:code -32601 :message (str "no method " method)}}))))

(defn- on-line
  "Takes the context and one line of standard input. Reads it as JSON and
  answers it when it is a request, a message with an id; a notification
  gets no answer. A line that is no JSON answers the error -32700, and a
  message that is no object -32600."
  [ctx line]
  (when-not (re-matches #"\s*" line)
    (let [parsed (try (js/JSON.parse line) (catch :default _ ::no-json))
          message (when (and (object? parsed) (not (array? parsed)))
                    (js->clj parsed :keywordize-keys true))]
      (cond
        (= ::no-json parsed)
        (answer! nil {:error {:code -32700 :message "the line is no JSON"}})

        (nil? message)
        (answer! nil {:error {:code -32600 :message "the message is no JSON-RPC object"}})

        (not (contains? message :id))
        nil

        :else
        (let [id (:id message)]
          (-> (handle ctx message)
              (.then (fn [r] (answer! id r)))
              (.catch (fn [e]
                        (node/err! (str "softland mcp: " (or (ex-message e) e)))
                        (answer! id {:error {:code -32603 :message (str (or (ex-message e) e))}})))))))))

(defn serve!
  "Takes what `--server` gave, or nil. Runs the MCP server on stdio until
  standard input ends; the process then ends when the calls under way
  have answered."
  [server-option]
  (let [ctx (commands/context server-option)]
    (node/err! (str "softland mcp: speaking to " (:server ctx)))
    (node/stdin-lines #(on-line ctx %) (fn [] nil))))
