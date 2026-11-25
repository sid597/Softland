(ns softland.cli.pure
  "The part of the program `softland` that touches nothing: the command line
  read into a plan, the words built from it, the token's text, the short
  code and the link of a sign-in, EDN read and printed, and what a result
  of the server means for the exit status. Every function here takes plain
  data and gives plain data; none reads a clock, a file, the network or a
  random number.

  Inputs and results, by call:

    plan-of          the arguments of the command line -> a plan, or
                     {:wrong text}
    read-edn         a text -> [:ok form] or [:no text]
    edn-text         a form -> its text on one line
    server-of        an option and an environment value -> the server's URL
    credentials-path an environment value and a home folder -> a path
    token-of         base64url text of 32 bytes -> a token
    code-of          a hash -> the short code
    sign-in-link     the server, a hash and what asks -> the link
    sign-in-step     a response to the waiting `:who-am-i` -> what it means
    write-act        a saved credential and a fact -> an act, or nil
    outcome-of       a response -> {:form :fine?} or {:failure}
    exit-status      an outcome -> 0 or 1

  A response, everywhere in the program, is what the Node part gives for a
  word that was posted: {:status n :body form :text t} when the server answered
  with one EDN form, or {:failure text} when it could not be reached or
  its body was no EDN. An outcome is what a command gives to be shown:
  {:form x :text t :fine? bool}, shown as its text, or {:failure text}.

  State and effects. None."
  (:require [cljs.reader :as reader]
            [cljs.tools.reader.reader-types :as reader-types]
            [clojure.string :as str]))

;; -------------------------------------------------------------- the numbers

(def default-server
  "The server that the program speaks to when no option and no environment
  names one."
  "http://localhost:8400")

(def sign-in-wait-ms
  "How long one waiting `:who-am-i` of a sign-in asks the server to hold
  the request open, in milliseconds. The server cuts a wait at 60000."
  30000)

(def sign-in-total-ms
  "How long a sign-in waits for her yes in all, in milliseconds, before the
  program gives up."
  (* 10 60 1000))

(def act-answer-wait-ms
  "How long `act` and `write` ask the server to wait for an act's answer,
  in milliseconds."
  5000)

(def watch-default-ms
  "How long the MCP tool `watch` collects messages when the call gives no
  wait, in milliseconds."
  2000)

(def watch-longest-ms
  "The longest that the MCP tool `watch` collects messages, in
  milliseconds; a longer wait is cut to it. It stays under a minute so that
  a harness that bounds a tool call by a minute gets its answer."
  55000)

;; ------------------------------------------------------------------ EDN

(defn- reject-tag
  "A reader of a tag that the wire does not carry. Takes the tagged form
  and throws, so that `read-edn` turns the text away. The server reads EDN
  with `clojure.edn` and knows `#inst` and `#uuid` alone."
  [_]
  (throw (js/Error. "a tag that the server does not read (only #inst and #uuid)")))

(defn read-edn
  "Takes a text. Gives [:ok form] when it holds exactly one EDN form, or
  [:no why], with `why` a sentence for the person: the text is empty, is no
  EDN, or holds something after its first form. The program reads a text
  to check that it is one form, or to take one small thing out of it; what
  it prints and what it sends is the text (`verbatim`). It reads with
  `cljs.reader`, which knows `#uuid` and `#inst`; `#js` and `#queue` are
  turned away. Limits: an integer wider than JavaScript's 2^53 loses its
  low digits, as every number does in JavaScript."
  [text]
  (if-not (string? text)
    [:no "no text was given"]
    (try
      (let [in (reader-types/string-push-back-reader text)
            end (js-obj)
            opts {:eof end :readers {'js reject-tag 'queue reject-tag}}
            form (reader/read opts in)]
        (cond
          (identical? end form) [:no "the text is empty"]
          (identical? end (reader/read opts in)) [:ok form]
          :else [:no "the text holds more than one form"]))
      (catch :default e
        [:no (str "the text is no EDN: " (.-message e))]))))

(deftype Verbatim [text]
  IPrintWithWriter
  (-pr-writer [_ writer _] (-write writer text)))

(defn verbatim
  "Takes the text of one EDN form, as the person gave it. Gives a thing
  that prints as that text, unchanged, inside any form that `edn-text`
  prints: so a word carries an argument exactly as it was typed. A number
  that JavaScript cannot hold, an integer wider than 2^53, an exact
  decimal such as 1.10M or a ratio, so reaches the server whole. The text
  must be one form (`read-edn`); it is not read again here."
  [text]
  (Verbatim. text))

(defn edn-text
  "Takes a form. Gives its EDN text on one line, as `pr-str` prints it:
  UUIDs as `#uuid \"...\"`, keywords as keywords, maps whole with no shared
  namespace in front, and no length or depth cut."
  [form]
  (binding [*print-namespace-maps* false
            *print-length* nil
            *print-level* nil
            *print-meta* false]
    (pr-str form)))

;; ---------------------------------------------------------- the command line

(def commands
  "Each command of the program: the names of its positional arguments, the
  names of those that may be left out at the end, the options it takes
  beside `--server`, and one line that says what it does. `:edn` names the
  arguments that are EDN text; `:int` those that are a whole number."
  {:sign-in  {:args [] :options #{:as}
              :says "sign in: shows a link and a code, and waits for her yes"}
   :sign-out {:args [] :options #{}
              :says "ends the session of the saved credential and forgets it"}
   :who      {:args [] :options #{}
              :says "who the saved credential stands for"}
   :act-ids  {:args [:lane :count] :optional #{:count} :options #{}
              :edn #{:lane} :int #{:count}
              :says "<lane> [<count>]: act-ids of a lane"}
   :send     {:args [:act] :options #{:act-id} :edn #{:act :act-id}
              :says "<act> [--act-id <act-id>]: sends one act; prints the receipt"}
   :answer   {:args [:act-id :digest] :options #{:wait} :edn #{:act-id} :int #{:wait}
              :says "<act-id> <digest> [--wait <ms>]: the answer of an act"}
   :act      {:args [:act] :options #{} :edn #{:act}
              :says "<act>: asks an act-id, sends, and reads the answer"}
   :write    {:args [:entity :key :value] :options #{} :edn #{:entity :key :value}
              :says "<entity> <key> <value>: one fact into her own lane, as `act` does"}
   :read     {:args [:query] :options #{} :edn #{:query}
              :says "<query>: a read, once"}
   :watch    {:args [:query] :options #{} :edn #{:query}
              :says "<query>: a read kept live; a line for each message, until it ends or ^C"}
   :say      {:args [:word] :options #{} :edn #{:word}
              :says "<word>: any word, as given"}
   :mcp      {:args [] :options #{}
              :says "the MCP server on stdio, for an agent's harness"}
   :help     {:args [] :options #{}
              :says "this text"}})

(def ^:private command-order
  "The order in which `usage` lists the commands."
  [:sign-in :sign-out :who :act-ids :send :answer :act :write :read :watch :say :mcp :help])

(def usage
  "The text that `help` prints, and that a wrong command line is shown
  with."
  (str "softland <command> [arguments] [--server <url>]\n\n"
       (str/join "\n" (for [c command-order]
                        (str "  " (name c) (apply str (repeat (- 10 (count (name c))) " "))
                             (:says (get commands c)))))
       "\n\nEvery argument that is data is EDN text; `-` reads it from standard input.\n"
       "The server: --server <url>, else $SOFTLAND_SERVER, else " default-server ".\n"
       "The credentials: $SOFTLAND_CREDENTIALS, else ~/.config/softland/credentials.edn.\n"))

(defn- option-pairs
  "Takes the arguments of the command line. Gives [positional options] or
  {:wrong text}: `--name value` and `--name=value` become an entry of
  options under the keyword of the name, `--help` and `-h` the entry
  `:help true`, and everything else is positional. A name given twice is
  wrong."
  [args]
  (loop [args (seq args) positional [] options {}]
    (if-not args
      [positional options]
      (let [a (first args)]
        (cond
          (or (= "--help" a) (= "-h" a))
          (recur (next args) positional (assoc options :help true))

          (str/starts-with? a "--")
          (let [[n inline] (str/split (subs a 2) #"=" 2)
                k (keyword n)]
            (cond
              (str/blank? n) {:wrong "an option with no name"}
              (contains? options k) {:wrong (str "the option --" n " is given twice")}
              (some? inline) (recur (next args) positional (assoc options k inline))
              (nil? (second args)) {:wrong (str "the option --" n " needs a value")}
              :else (recur (nnext args) positional (assoc options k (second args)))))

          :else (recur (next args) (conj positional a) options))))))

(defn- whole-number
  "Takes a text. Gives the whole number that it writes in decimal, or nil."
  [text]
  (when (re-matches #"[0-9]+" text)
    (js/parseInt text 10)))

(defn plan-of
  "Takes the arguments of the command line, as texts. Gives a plan:

    {:command :write
     :server  text or nil          what --server gave
     :options {:as text, :wait n}  the other options, read
     :texts   {:entity text ...}   each EDN argument, by name, as text
     :numbers {:count n ...}       each whole-number argument
     :plain   {:digest text}}      each argument that is plain text

  or {:wrong text} when the line names no command, a command that does not
  exist, too many or too few arguments, an option that the command does not
  take, or a number that is no whole number. An EDN text is not read here:
  one of them may be `-`, which the commands read from standard input
  first. Two `-` are wrong, since standard input is read once."
  [args]
  (let [pairs (option-pairs args)]
    (if (:wrong pairs)
      pairs
      (let [[positional options] pairs
            command (if (or (:help options) (empty? positional)) :help (keyword (first positional)))
            spec (get commands command)
            given (if (:help options) [] (vec (rest positional)))
            options (dissoc options :help)
            server (:server options)
            options (dissoc options :server)
            unknown (remove (:options spec) (keys options))
            least (- (count (:args spec)) (count (:optional spec)))]
        (cond
          (nil? spec) {:wrong (str "no command is called " (first positional))}
          (seq unknown) {:wrong (str (name command) " takes no option --" (name (first unknown)))}
          (< (count (:args spec)) (count given)) {:wrong (str (name command) " takes at most " (count (:args spec)) " arguments")}
          (< (count given) least) {:wrong (str (name command) " needs " (str/join " " (map #(str "<" (name %) ">") (take least (:args spec)))))}
          :else
          (let [named (merge (zipmap (:args spec) given) options)
                ints (:int spec #{})
                edns (:edn spec #{})
                bad-int (some (fn [[k v]] (when (and (ints k) (nil? (whole-number v))) k)) named)
                stdin-count (count (filter (fn [[k v]] (and (edns k) (= "-" v))) named))]
            (cond
              bad-int {:wrong (str "<" (name bad-int) "> is no whole number")}
              (< 1 stdin-count) {:wrong "only one argument can be read from standard input"}
              :else
              {:command command
               :server server
               :options (cond-> {}
                          (contains? named :as) (assoc :as (:as named))
                          (contains? named :wait) (assoc :wait (whole-number (:wait named))))
               :texts (into {} (filter (fn [[k _]] (edns k))) named)
               :numbers (into {} (keep (fn [[k v]] (when (and (ints k) (not= :wait k)) [k (whole-number v)]))) named)
               :plain (into {} (remove (fn [[k _]] (or (edns k) (ints k) (= :as k)))) named)})))))))

;; ------------------------------------------------------- where things are

(defn- without-trailing-slash
  "Takes a URL's text. Gives it without slashes at its end, so that one
  server has one key in the credentials file."
  [url]
  (str/replace url #"/+$" ""))

(defn server-of
  "Takes what `--server` gave and the value of `$SOFTLAND_SERVER`, either
  nil. Gives the URL of the server: the option, else the environment, else
  `default-server`, without a slash at its end."
  [option env-value]
  (without-trailing-slash
   (or (when-not (str/blank? option) option)
       (when-not (str/blank? env-value) env-value)
       default-server)))

(defn credentials-path
  "Takes the value of `$SOFTLAND_CREDENTIALS`, or nil, and the home folder.
  Gives the path of the credentials file."
  [env-value home]
  (if (str/blank? env-value)
    (str home "/.config/softland/credentials.edn")
    env-value))

;; ----------------------------------------------------------------- sign-in

(defn token-of
  "Takes 32 random bytes written as base64url without padding: 43
  characters. Gives the token, `sl1_` and those characters."
  [base64url]
  (str "sl1_" base64url))

(defn code-of
  "Takes a hash, 64 hex characters. Gives the short code that she compares:
  its first eight characters as `xxxx-xxxx`."
  [hash]
  (str (subs hash 0 4) "-" (subs hash 4 8)))

(defn sign-in-link
  "Takes the server's URL, a token's hash and the text that says what asks.
  Gives the link to the page on which she says yes."
  [server hash what]
  (str server "/sign-in/yes?hash=" hash "&what=" (js/encodeURIComponent what)))

(defn sign-in-step
  "Takes the response to a `:who-am-i` with a wait, said with the token of a
  sign-in under way. Gives what it means:

    [:yes about]      she said yes; `about` is {:who :session :until
                      :own-lane}
    [:not-yet]        the wait passed with no yes
    [:no]             she said no, or the credential ended
    [:try-again text] the server could not be reached or could not read
                      its sessions: a later try may work
    [:failed form]    anything else"
  [response]
  (let [{:keys [status body failure]} response]
    (cond
      failure [:try-again failure]
      (and (= 200 status) (map? body) (:who body)) [:yes body]
      (and (= 401 status) (= :not-known (:why body))) [:not-yet]
      (and (= 401 status) (#{:ended :over} (:why body))) [:no]
      (= 503 status) [:try-again (edn-text body)]
      :else [:failed body])))

(defn saved-entry
  "Takes what the credentials file gave, as a map, and the server's URL.
  Gives the entry saved for that server, {:token :who :own-lane}, or nil
  when there is none or it holds no token."
  [credentials server]
  (let [entry (get credentials server)]
    (when (and (map? entry) (string? (:token entry)))
      entry)))

;; -------------------------------------------------------------- the words

(defn write-act
  "Takes a saved credential, {:who :own-lane}, and an entity, a key and a
  value. Gives the act that writes that one fact into her own lane under
  her first permission, `[who own-lane own-lane]`, or nil when the
  credential holds no own lane."
  [{:keys [who own-lane]} entity k v]
  (when (and who own-lane)
    {:version 1
     :lane own-lane
     :permission [who own-lane own-lane]
     :facts [{:e entity :k k :v v}]}))

(defn first-act-id
  "Takes the body of an `:act-ids` result. Gives its first act-id, or nil
  when it holds none."
  [body]
  (when (and (vector? body) (vector? (first body)))
    (first body)))

;; --------------------------------------------------------------- outcomes

(defn could-not?
  "Takes a result. Gives true when it is a map that holds `:could-not`, or
  a vector one of whose items is such a map (the receipts of `:send-all`
  end at the first that the door could not send). It does not look deeper:
  a value that a fact holds is the person's data, not a word of the
  server's."
  [form]
  (boolean
   (or (and (map? form) (contains? form :could-not))
       (and (vector? form) (some #(and (map? %) (contains? % :could-not)) form)))))

(defn outcome-of
  "Takes a response. Gives an outcome: {:form body :text text :fine?
  bool}, fine when the status is 200 and the body holds no `:could-not`;
  or the response's `{:failure text}` as it is. The text is the body as
  the server wrote it: it is what is shown, so that a number that
  JavaScript cannot hold is shown as the server wrote it."
  [{:keys [status body text failure]}]
  (if failure
    {:failure failure}
    {:form body :text text :fine? (and (= 200 status) (not (could-not? body)))}))

(defn text-of
  "Takes an outcome with a form. Gives the text to show: the text of the
  server's body as it came, when the outcome holds one, else the form
  printed."
  [outcome]
  (or (:text outcome) (edn-text (:form outcome))))

(defn exit-status
  "Takes an outcome. Gives the exit status: 0 for a fine one, 1 for
  anything else."
  [outcome]
  (if (:fine? outcome) 0 1))

(defn needs-sign-in?
  "Takes an outcome. Gives true when the server said that it found no
  session for the credential, so that the person is told to sign in."
  [outcome]
  (= :find-her-session (:could-not (:form outcome))))
