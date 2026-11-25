(ns softland.cli.commands
  "The commands of the program `softland`, and the steps that the MCP
  server shares with them. A command takes a context and its arguments,
  read, and gives a promise of an outcome; `run` takes a plan of the
  command line, does it, shows what came back and gives the exit status.

  The context is {:server url :credentials-path path}. The credentials file
  is an EDN map from a server's URL to the credential saved for it,
  {:token :who :own-lane}. A step reads the file each time it needs the
  credential, so that a sign-in done in another process is seen at once.

  An outcome is {:form x :text t :fine? bool}, shown as its text on
  standard output, or {:failure text}, shown as a message on standard
  error (`softland.cli.pure`). The token is never shown.

  Data passes through as text. A word carries each argument as the
  person typed it, and a result is shown as the server wrote it; the
  program reads a text only to check that it is one form, or to take out
  one small thing: the `:lane` of an act, the act-id and the digest of a
  receipt, whether a result holds `:could-not`. So an integer wider than
  2^53, an exact decimal or a ratio is never turned into a JavaScript
  number on its way.

  The steps that more than one command takes:

    say             a word, with the saved credential -> a response
    act-outcome     an act -> asks one act-id of its lane, tells it, sends
                    the act under it, reads its answer with a wait
    write-outcome   a fact -> the act of that fact in her own lane, then
                    as `act-outcome`
    watch-collected a query and a span -> the messages of a watch that came
                    within the span

  State and effects. No state of its own. Its effects are the Node part's:
  the network, the credentials file, standard input, output and error."
  (:require [clojure.string :as str]
            [softland.cli.node :as node]
            [softland.cli.pure :as pure]))

;; ------------------------------------------------------- the credentials

(defn context
  "Takes what `--server` gave, or nil. Gives the context of a run: the
  server's URL and the path of the credentials file, from the option and
  the environment."
  [server-option]
  {:server (pure/server-of server-option (node/env "SOFTLAND_SERVER"))
   :credentials-path (pure/credentials-path (node/env "SOFTLAND_CREDENTIALS") (node/home))})

(defn- credentials
  "Takes a context. Gives the map that the credentials file holds, {} when
  there is no file. Throws an ex-info with a message for the person when
  the file holds no EDN map."
  [{:keys [credentials-path]}]
  (let [text (node/read-text credentials-path)]
    (if (nil? text)
      {}
      (let [[outcome form] (pure/read-edn text)]
        (if (and (= :ok outcome) (map? form))
          form
          (throw (ex-info (str "the credentials file " credentials-path " holds no EDN map") {})))))))

(defn saved
  "Takes a context. Gives the credential saved for its server,
  {:token :who :own-lane}, or nil."
  [ctx]
  (pure/saved-entry (credentials ctx) (:server ctx)))

(defn- save!
  "Takes a context and a credential. Writes it into the credentials file
  under the context's server, keeping the entries of other servers."
  [ctx entry]
  (node/write-private! (:credentials-path ctx)
                       (str (pure/edn-text (assoc (credentials ctx) (:server ctx) entry)) "\n")))

(defn- forget!
  "Takes a context. Takes the entry of its server out of the credentials
  file, when the file holds one."
  [ctx]
  (let [all (credentials ctx)]
    (when (contains? all (:server ctx))
      (node/write-private! (:credentials-path ctx)
                           (str (pure/edn-text (dissoc all (:server ctx))) "\n")))))

(defn no-credential-text
  "Takes a context. Gives the message that tells the person that no
  credential is saved for its server, and what to run."
  [ctx]
  (str "no credential is saved for " (:server ctx) ": run `softland sign-in`"
       (when (not= pure/default-server (:server ctx)) (str " --server " (:server ctx)))))

(defn sign-in-hint
  "Takes a context. Gives the message for the person when the server found
  no session for what the program sent: that no credential is saved, or
  that the saved one stands for nobody now; and what to run."
  [ctx]
  (if (saved ctx)
    (str "the credential saved for " (:server ctx) " stands for no session now: run `softland sign-in`"
         (when (not= pure/default-server (:server ctx)) (str " --server " (:server ctx))))
    (no-credential-text ctx)))

;; ---------------------------------------------------------------- the steps

(defn say
  "Takes a context and a word. Posts the word with the saved credential,
  or with none when nothing is saved. Gives a promise of the response."
  [ctx word]
  (node/post-word (:server ctx) (:token (saved ctx)) word))

(defn- say-outcome
  "Takes a context and a word. Gives a promise of the outcome of saying it."
  [ctx word]
  (.then (say ctx word) pure/outcome-of))

(defn act-outcome
  "Takes a context, an act, the act's lane and a function of one act-id.
  The act is anything that prints as the act's EDN: the person's own text
  (`softland.cli.pure/verbatim`), or a map that holds such texts. Asks the
  server for one act-id of the lane, hands it to the function before
  anything is sent, so that the person can send again under it when the
  program dies, sends the act under it, and reads its answer with a wait of
  `softland.cli.pure/act-answer-wait-ms`. Gives a promise of the outcome,
  whose text is `{:receipt r :answer a}` with each result's text as the
  server wrote it, fine when each step was; or, when a step failed, what
  that step gave: the result of `:act-ids`, or `{:receipt r}`."
  [ctx act lane on-act-id]
  (-> (say ctx {:word :act-ids :lane lane :count 1})
      (.then
       (fn [response]
         (let [ids (pure/outcome-of response)
               act-id (when (:fine? ids) (pure/first-act-id (:form ids)))]
           (cond
             (not (:fine? ids)) ids
             (nil? act-id) (assoc ids :fine? false)
             :else
             (do (on-act-id act-id)
                 (-> (say ctx {:word :send :act act :act-id act-id})
                     (.then
                      (fn [response]
                        (let [receipt (pure/outcome-of response)]
                          (cond
                            (:failure receipt) receipt
                            (not (:fine? receipt)) {:form {:receipt (:form receipt)}
                                                    :text (str "{:receipt " (pure/text-of receipt) "}")
                                                    :fine? false}
                            :else
                            (-> (say ctx {:word :answer
                                          :act-id (:act-id (:form receipt) act-id)
                                          :digest (:digest (:form receipt))
                                          :wait-ms pure/act-answer-wait-ms})
                                (.then
                                 (fn [response]
                                   (let [answer (pure/outcome-of response)]
                                     (if (:failure answer)
                                       {:failure (str (:failure answer) "; the receipt was "
                                                      (pure/text-of receipt))}
                                       {:form {:receipt (:form receipt) :answer (:form answer)}
                                        :text (str "{:receipt " (pure/text-of receipt)
                                                   ", :answer " (pure/text-of answer) "}")
                                        :fine? (:fine? answer)})))))))))))))))))

(defn write-outcome
  "Takes a context, an entity, a key, a value and a function of one act-id;
  the three are the person's texts (`softland.cli.pure/verbatim`), which
  go into the act as they are. Builds the act of that one fact in her own
  lane under her first permission, from the saved credential, and does
  what `act-outcome` does. Gives a promise of the outcome; a failure, with
  nothing sent, when no credential is saved or it holds no own lane."
  [ctx entity k v on-act-id]
  (let [entry (saved ctx)
        act (when entry (pure/write-act entry entity k v))]
    (cond
      (nil? entry) (js/Promise.resolve {:failure (no-credential-text ctx)})
      (nil? act) (js/Promise.resolve {:failure (str "the credential saved for " (:server ctx)
                                                    " names no own lane: run `softland sign-in` again")})
      :else (act-outcome ctx act (:lane act) on-act-id))))

(defn watch-collected
  "Takes a context, a query (the person's text, or a form) and a span in
  milliseconds. Opens a watch of the query, collects the messages that
  arrive within the span, and closes it. Gives a promise of {:lines [text
  ...] :still-open bool :fine? bool}, each line a message's text as the
  server wrote it; or {:failure text}. It is fine when the server answered
  200 and no message holds `:could-not`; the message `{:ended word}` holds
  the word."
  [ctx query span-ms]
  (js/Promise.
   (fn [resolve]
     (let [lines (atom [])
           fine? (atom true)
           timer (atom nil)
           close (node/open-watch
                  (:server ctx) (:token (saved ctx)) {:word :watch :query query}
                  (fn [message line]
                    (when (or (pure/could-not? message) (pure/could-not? (:ended message)))
                      (reset! fine? false))
                    (swap! lines conj line))
                  (fn [end]
                    (js/clearTimeout @timer)
                    (cond
                      (:failure end) (resolve end)
                      (:status end) (resolve {:lines [(:text end)] :still-open false :fine? false})
                      :else (resolve {:lines @lines
                                      :still-open (boolean (:closed end))
                                      :fine? @fine?}))))]
       (reset! timer (js/setTimeout close span-ms))))))

;; ---------------------------------------------------------------- sign-in

(defn- wait-for-yes
  "Takes a context, a token and the time at which to give up, in
  milliseconds since 1970. Says `:who-am-i` with a wait, again and again,
  until she says yes or no, the time passes, or the server gives something
  else. A server that could not be reached is tried again after a pause.
  Gives a promise of the last step (`softland.cli.pure/sign-in-step`), or
  [:too-late]."
  [ctx token give-up-at]
  (if (< give-up-at (node/now-ms))
    (js/Promise.resolve [:too-late])
    (-> (node/post-word (:server ctx) token {:word :who-am-i :wait-ms pure/sign-in-wait-ms})
        (.then (fn [response]
                 (let [[step :as it] (pure/sign-in-step response)]
                   (case step
                     :not-yet (wait-for-yes ctx token give-up-at)
                     :try-again (do (node/err! (str "  (" (second it) "; trying again)"))
                                    (.then (node/pause 2000) #(wait-for-yes ctx token give-up-at)))
                     it)))))))

(defn sign-in
  "Takes a context and the text that says what asks, or nil. Makes a token,
  shows on standard error the link to the page of her yes and the short
  code, waits for her yes, and saves {:token :who :own-lane} for the
  server. Gives a promise of the outcome: {:who :own-lane} when she said
  yes, a failure otherwise. A credential that was saved before is replaced
  only when she says yes."
  [ctx what]
  (let [token (pure/token-of (node/random-base64url 32))
        hash (node/sha256-hex token)
        what (or what (str "softland CLI on " (node/host-name)))
        before (saved ctx)]
    (when before
      (node/err! (str "A credential is saved for " (:server ctx) " already; her yes replaces it.")))
    (node/err! (str "Open this link where you are signed in to Softland:\n\n  "
                    (pure/sign-in-link (:server ctx) hash what)
                    "\n\nSay yes only if the page shows the code  " (pure/code-of hash)
                    "\n\nWaiting for your yes..."))
    (-> (wait-for-yes ctx token (+ (node/now-ms) pure/sign-in-total-ms))
        (.then (fn [[step about]]
                 (case step
                   :yes (let [entry (cond-> {:token token :who (:who about)}
                                      (:own-lane about) (assoc :own-lane (:own-lane about)))]
                          (save! ctx entry)
                          (node/err! (str "Signed in. The credential is saved in " (:credentials-path ctx)))
                          {:form (select-keys entry [:who :own-lane]) :fine? true})
                   :no {:failure "she said no: the program is not signed in"}
                   :too-late {:failure "no yes came within ten minutes: the program is not signed in"}
                   :try-again {:failure about}
                   {:form about :fine? false}))))))

(defn sign-out
  "Takes a context. Says `:sign-out` with the saved credential and forgets
  it when the server ended the session or knows it as no session (a 401:
  the credential is worth nothing either way). A credential is kept when
  the server could not be reached or did not say. Gives a promise of the
  outcome."
  [ctx]
  (-> (say ctx {:word :sign-out})
      (.then (fn [response]
               (let [outcome (pure/outcome-of response)]
                 (when (and (saved ctx) (#{200 401} (:status response)))
                   (forget! ctx)
                   (node/err! (str "The credential for " (:server ctx) " is forgotten.")))
                 outcome)))))

;; ---------------------------------------------------------------- the run

(defn- show!
  "Takes a context and an outcome. Prints its form on standard output, or
  its failure on standard error; and when the server found no session,
  tells the person to sign in. Gives the exit status."
  [ctx outcome]
  (if (:failure outcome)
    (node/err! (str "softland: " (:failure outcome)))
    (node/out! (pure/text-of outcome)))
  (when (pure/needs-sign-in? outcome)
    (node/err! (str "softland: " (sign-in-hint ctx))))
  (pure/exit-status outcome))

(defn- read-texts
  "Takes the plan's EDN texts by name. Reads standard input for the one
  that is `-`, and reads each as EDN, to check that it is one form. Gives
  a promise of [:ok {name verbatim} {name form}], each argument as the
  person's text (`softland.cli.pure/verbatim`), which is what a word
  carries, and as the form read, which is used only to take one small
  thing out; or [:no text], with the name of the argument that is no EDN."
  [texts]
  (let [from-stdin (some (fn [[k v]] (when (= "-" v) k)) texts)]
    (-> (if from-stdin (node/read-stdin) (js/Promise.resolve nil))
        (.then (fn [stdin]
                 (reduce (fn [[_ texts forms] [k v]]
                           (let [text (if (= "-" v) stdin v)
                                 [outcome form] (pure/read-edn text)]
                             (if (= :ok outcome)
                               [:ok (assoc texts k (pure/verbatim (str/trim text))) (assoc forms k form)]
                               (reduced [:no (str "<" (name k) "> " form)]))))
                         [:ok {} {}]
                         texts))))))

(defn- watch-printing
  "Takes a context and a query. Opens a watch and prints each message on a
  line of standard output as it comes, until the stream ends or the person
  presses ^C, which closes it. Gives a promise of the exit status: 0 when
  the stream ended with no word of failure or was closed by the person, 1
  otherwise."
  [ctx query]
  (js/Promise.
   (fn [resolve]
     (let [last-message (atom nil)
           close (node/open-watch
                  (:server ctx) (:token (saved ctx)) {:word :watch :query query}
                  (fn [message line]
                    (reset! last-message message)
                    (node/out! line))
                  (fn [end]
                    (cond
                      (:failure end) (resolve (show! ctx end))
                      (:status end) (resolve (show! ctx (pure/outcome-of end)))
                      (:closed end) (resolve 0)
                      :else (resolve (if (pure/could-not? (:ended @last-message)) 1 0)))))]
       (.on js/process "SIGINT" close)))))

(defn run
  "Takes a plan of the command line (`softland.cli.pure/plan-of`) that is
  no `mcp` and not wrong. Does its command, shows what came back, and gives
  a promise of the exit status. A fault of the credentials file is shown
  as a message, with status 1."
  [{:keys [command options numbers] :as plan}]
  (let [ctx (context (:server plan))
        on-act-id (fn [act-id]
                    (node/err! (str "act-id " (pure/edn-text act-id)
                                    " (to send again under it: softland send <act> --act-id '"
                                    (pure/edn-text act-id) "')")))]
    (-> (read-texts (:texts plan))
        (.then
         (fn [[outcome read forms]]
           (if (= :no outcome)
             (do (node/err! (str "softland: " read)) 2)
             (case command
               :help (do (node/out! pure/usage) 0)
               :watch (watch-printing ctx (:query read))
               (-> (case command
                     :sign-in (sign-in ctx (:as options))
                     :sign-out (sign-out ctx)
                     :who (say-outcome ctx {:word :who-am-i})
                     :act-ids (say-outcome ctx {:word :act-ids :lane (:lane read) :count (:count numbers 1)})
                     :send (say-outcome ctx (cond-> {:word :send :act (:act read)}
                                              (contains? read :act-id) (assoc :act-id (:act-id read))))
                     :answer (say-outcome ctx (cond-> {:word :answer :act-id (:act-id read)
                                                       :digest (let [d (:digest (:plain plan))
                                                                     [o s] (pure/read-edn d)]
                                                                 (if (and (= :ok o) (string? s)) s d))}
                                                (:wait options) (assoc :wait-ms (:wait options))))
                     :act (act-outcome ctx (:act read) (when (map? (:act forms)) (:lane (:act forms))) on-act-id)
                     :write (write-outcome ctx (:entity read) (:key read) (:value read) on-act-id)
                     :read (say-outcome ctx {:word :read :query (:query read)})
                     :say (say-outcome ctx (:word read)))
                   (.then #(show! ctx %)))))))
        (.catch (fn [e]
                  (node/err! (str "softland: " (or (ex-message e) e)))
                  1)))))
