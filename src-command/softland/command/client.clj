(ns softland.command.client
  "The library by which a Clojure program speaks to the runtime: it asks to
  be signed in, opens the line, says words and holds watches. It requires
  the JDK's own HTTP client and the wire's forms, and nothing else: no
  Rama, and no server.

  It shares its forms with the runtime. A frame is read and written by
  `softland.wire.frame`, a credential is put together by
  `softland.wire.credential`, and the words are `softland.wire.words`'s.
  The JVM reads EDN's numbers as they are, so a value of any width goes
  through this library as itself.

  The calls:

    ask       a runtime's address and what the program says of itself ->
              {:secret :code :shown :link :ends}, or a word
    waited    the address, a code and a secret -> the ask's state, once
    connect   {:url :credential :session} -> a connection, or a word
    hello     a connection -> what the runtime said first: who it is
    say       a connection, a word, and a wait -> the word's result
    watch     a connection, a query and a function -> the watch's number
    leave     a connection and a watch's number -> the result of `:leave`
    bye       a connection -> the word with which the runtime ended it, or
              nil
    close!    a connection -> nil

  A word of this library's own is a map that holds `:could-not`:
  `:reach-the-runtime` when a request or the connection failed, with what
  failed; `:hear-the-runtime` when a wait for a result passed. A word of
  the runtime's comes back as it is. No call throws for what the runtime
  did.

  One connection is one session. `connect` names the session that the
  program was in, when it holds one, and the runtime keeps it in that
  session; with none it is given a new one, and `hello` says which. A
  program that was cut off connects again under the same session and
  sends the act again under the same UUID: the runtime and the store then
  take it once.

  Threads. A result is delivered, and a watch's function is called, on the
  thread of the JDK's client that received the frame, one frame at a time:
  a watch's function that stays holds back every result of the connection.

  State. A connection holds, in the memory of the process: the socket; the
  number of the next word; the results that are waited for; the functions
  of its watches; and what the runtime said first and last."
  (:require [softland.wire.credential :as credential]
            [softland.wire.frame :as frame])
  (:import [java.net URI]
           [java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers
            HttpResponse$BodyHandlers WebSocket WebSocket$Listener WebSocketHandshakeException]
           [java.time Duration]
           [java.util.concurrent CompletableFuture]))

(def ^:private connect-ms
  "How long a request and the opening of the line may take."
  15000)

(defn- what-threw
  "Takes a thing that a call threw. Gives the word `:reach-the-runtime`
  with the name of its class and what it said; and, for a refused opening
  of the line, the status of the runtime's answer."
  [^Throwable t]
  (let [cause (or (.getCause t) t)]
    (cond-> {:could-not :reach-the-runtime :threw (.getName (class cause)) :said (.getMessage cause)}
      (instance? WebSocketHandshakeException cause)
      (assoc :status (.statusCode (.getResponse ^WebSocketHandshakeException cause))))))

(def ^:private shared-http-client
  "Shared JDK HttpClient so polling `waited` and opening connections do not
  leak a new SelectorManager thread on every call."
  (delay (-> (HttpClient/newBuilder) (.connectTimeout (Duration/ofMillis connect-ms)) (.build))))

(defn- http-client
  "Gives the shared JDK HTTP client."
  ^HttpClient []
  @shared-http-client)

(defn- post
  "Posts one EDN form to a path of the runtime and reads the one form that
  comes back. Takes the runtime's address, the path and the thing. Gives
  the form, or the word `:reach-the-runtime`."
  [url path thing]
  (try
    (let [request (-> (HttpRequest/newBuilder (URI/create (str url path)))
                      (.timeout (Duration/ofMillis connect-ms))
                      (.header "Content-Type" "application/edn")
                      (.POST (HttpRequest$BodyPublishers/ofString (frame/write thing)))
                      (.build))
          response (.send (http-client) request (HttpResponse$BodyHandlers/ofString))
          [outcome form] (frame/read-one (.body response))]
      (if (= :ok outcome)
        form
        {:could-not :reach-the-runtime :status (.statusCode response) :said (str "no EDN came back: " form)}))
    (catch Throwable t (what-threw t))))

;; ------------------------------------------------------ a program's sign-in

(defn ask
  "Asks the runtime to sign this program in. Takes the runtime's address,
  such as `http://localhost:8500`, and what the program says of itself, a
  text. It makes a secret, which never leaves the program before a person
  has said yes, and posts its hash. Gives {:secret :code :shown :link
  :ends}: the secret to keep, the code under which the ask is found, the
  code as a person reads it, the link for her to open, and when the ask is
  over; or a word."
  [url asks]
  (let [secret (credential/make-secret)
        got (post url "/sign-in/program" {:hash (credential/hash-of secret) :asks asks})]
    (if (:could-not got) got (assoc got :secret secret))))

(defn waited
  "Asks once what became of an ask. Takes the runtime's address, the code
  and the secret. Gives what the runtime says: {:state :waiting}, {:state
  :yes :root :actor :sign-in}, {:state :no}, {:state :over}; or a word.
  With a yes, `:credential` holds the text that the program signs in with
  from now on."
  [url code secret]
  (let [got (post url "/sign-in/program/poll" {:code code :secret secret})]
    (cond-> got
      (= :yes (:state got)) (assoc :credential (credential/text (:root got) (:sign-in got) secret)))))

;; ------------------------------------------------------------------- the line

(defrecord Connection [^WebSocket socket state])

(defn- line-url
  "Takes the runtime's address. Gives the address of its line: `ws` for
  `http`, and `wss` for `https`."
  [url]
  (str (if (.startsWith ^String url "https") "wss" "ws")
       (subs url (.indexOf ^String url "://"))
       "/line"))

(defn- heard!
  "Takes a connection's state and one form that the runtime sent. A hello
  and a bye are kept; a result is delivered to whoever waits for its
  number; a watch's message is handed to the watch's function, and an end
  takes the watch away."
  [state form]
  (cond
    (contains? form :hello) (deliver (:hello @state) (:hello form))
    (contains? form :bye) (swap! state assoc :bye (:bye form))
    (contains? form :gives) (when-let [waits (get-in @state [:waits (:id form)])]
                              (swap! state update :waits dissoc (:id form))
                              (deliver waits (:gives form)))
    (contains? form :id) (when-let [f (get-in @state [:watches (:id form)])]
                           (when (contains? form :ended)
                             (swap! state update :watches dissoc (:id form)))
                           (try (f (dissoc form :id)) (catch Throwable _ nil)))
    ;; One of the runtime's words that names no number: a frame of ours
    ;; that it could not read.
    :else (swap! state assoc :said form)))

(defn- gone!
  "Takes a connection's state and why the line is gone. Everything that
  waits is told so, and each watch is told an end."
  [state why]
  (let [[before _] (swap-vals! state assoc :gone why :waits {} :watches {})]
    (deliver (:hello before) (assoc why :could-not :reach-the-runtime))
    (doseq [waits (vals (:waits before))]
      (deliver waits (assoc why :could-not :reach-the-runtime)))
    (doseq [f (vals (:watches before))]
      (try (f {:ended (assoc why :could-not :reach-the-runtime)}) (catch Throwable _ nil)))))

(defn- listener
  "Makes the JDK's listener of one line. A text can arrive in parts; the
  parts are put together, and each whole text is read as one frame."
  ^WebSocket$Listener [state]
  (let [parts (StringBuilder.)]
    (reify WebSocket$Listener
      (onOpen [_ socket] (.request socket 1))
      (onText [_ socket data last?]
        (.append parts data)
        (when last?
          (let [text (.toString parts)
                [outcome form] (frame/read-one text)]
            (.setLength parts 0)
            (when (and (= :ok outcome) (map? form))
              (heard! state form))))
        (.request socket 1)
        nil)
      (onClose [_ _socket status reason]
        (gone! state {:closed status :said reason})
        nil)
      (onError [_ _socket error]
        (gone! state {:threw (.getName (class error)) :said (.getMessage ^Throwable error)})))))

(defn connect
  "Opens the line. Takes a map: `:url`, the runtime's address;
  `:credential`, the credential's text, or nil for a visitor; `:session`,
  the id of the session that the program was in, or nil for a new one.
  Gives a connection, or a word: `:reach-the-runtime`, with the `:status`
  of the runtime's answer when it refused the opening (401 for a credential
  that stands for nobody now, or a session that is not open under it)."
  [{:keys [url credential session]}]
  (try
    (let [state (atom {:hello (promise) :next 0 :waits {} :watches {}})
          builder (cond-> (-> (http-client) (.newWebSocketBuilder)
                              (.connectTimeout (Duration/ofMillis connect-ms)))
                    credential (.header "Authorization" (str "Bearer " credential))
                    session (.header "softland-session" (str session)))
          socket (.join ^CompletableFuture (.buildAsync builder (URI/create (line-url url)) (listener state)))]
      (->Connection socket state))
    (catch Throwable t (what-threw t))))

(defn hello
  "Takes a connection. Gives what the runtime said first: {:who :root
  :sign-in :session :kind :own-layer :until}, or {:visitor true}; or a word
  when the line went before it said anything."
  [connection]
  (deref (:hello @(:state connection)) connect-ms {:could-not :hear-the-runtime}))

(defn- send!
  "Sends one form on the line, serialized per connection so concurrent calls
  never collide on JDK WebSocket.sendText. Gives nil, or a word when the send
  failed."
  [connection form]
  (try (locking connection
         (.join ^CompletableFuture (.sendText ^WebSocket (:socket connection) (frame/write form) true)))
       nil
       (catch Throwable t (what-threw t))))

(defn say
  "Says one word and waits for its result. Takes a connection; a word of
  the wire, without a number; and, when the caller wants, how long to wait
  for the result in milliseconds, 90 seconds when left out. Gives the
  result; `:hear-the-runtime` when the wait passed; or
  `:reach-the-runtime` when the line went. A word that did not come back
  can have been done: for an act, the program sends it again under the
  same UUID."
  ([connection word] (say connection word 90000))
  ([connection word wait-ms]
   (let [state (:state connection)
         waits (promise)
         reg (volatile! nil)]
     (swap! state
            (fn [st]
              (if-let [gone (:gone st)]
                (do (vreset! reg [:gone gone]) st)
                (let [id (:next st)]
                  (vreset! reg [:ok id])
                  (-> st (assoc :next (inc id)) (assoc-in [:waits id] waits))))))
     (let [[status val] @reg]
       (if (= :gone status)
         (assoc val :could-not :reach-the-runtime)
         (let [id val]
           (if-let [err (send! connection (assoc word :id id))]
             (do (swap! state update :waits dissoc id)
                 err)
             (let [res (deref waits wait-ms ::timeout)]
               (if (identical? ::timeout res)
                 (do (swap! state update :waits dissoc id)
                     {:could-not :hear-the-runtime})
                 res)))))))))

(defn watch
  "Opens a watch. Takes a connection, a query, and a function of one
  argument, which is called with each message of the watch: {:anew
  answer}, {:changed {place place-answer}}, and {:ended word} as the last.
  Gives the watch's number, or a word when the send failed."
  [connection query f]
  (let [state (:state connection)
        reg (volatile! nil)]
    (swap! state
           (fn [st]
             (if-let [gone (:gone st)]
               (do (vreset! reg [:gone gone]) st)
               (let [id (:next st)]
                 (vreset! reg [:ok id])
                 (-> st (assoc :next (inc id)) (assoc-in [:watches id] f))))))
    (let [[status val] @reg]
      (if (= :gone status)
        (assoc val :could-not :reach-the-runtime)
        (let [id val]
          (if-let [err (send! connection {:id id :word :watch :query query})]
            (do (swap! state update :watches dissoc id)
                err)
            id))))))

(defn leave
  "Leaves a watch. Takes a connection and the watch's number. Gives the
  result of the word `:leave`."
  [connection id]
  (swap! (:state connection) update :watches dissoc id)
  (say connection {:word :leave :watch id}))

(defn bye
  "Takes a connection. Gives the word with which the runtime ended it, or
  nil."
  [connection]
  (:bye @(:state connection)))

(defn close!
  "Closes the line. The session stays open in the runtime: the program can
  connect again under it."
  [connection]
  (try (locking connection
         (.join ^CompletableFuture (.sendClose ^WebSocket (:socket connection) WebSocket/NORMAL_CLOSURE "done")))
       (catch Throwable _ nil))
  nil)
