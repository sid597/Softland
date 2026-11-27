(ns softland.command.client
  "The library by which a Clojure program speaks to the runtime: it asks to
  be signed in, opens the line, says words and holds watches. The JVM reads
  EDN's numbers as they are, so a value of any width goes through this
  library as itself.

  A word of this library's own is a map that holds `:could-not`:
  `:reach-the-runtime` when a request or the connection failed, with what
  failed; `:hear-the-runtime` when a wait for a result passed. A word of
  the runtime's comes back as it is: a result, the word with which it
  refused an opening, and the word of its `:bye`, which everything that
  waits on the connection and each later call on it is handed. One does
  not: a word that names no number, for a frame that the runtime could not
  read, is kept and handed to nobody, and the call that sent the frame ends
  as `:hear-the-runtime`. No call throws for what the runtime did.

  One connection is one session. A program that was cut off connects again
  under the same session and sends the act again under the same UUID: the
  runtime and the store then take it once.

  Threads. A result is delivered, and a watch's function is called, on the
  thread of the JDK's client that received the frame, one frame at a time:
  a watch's function that stays holds back every result of the connection."
  (:require [softland.wire.credential :as credential]
            [softland.wire.frame :as frame])
  (:import [java.net URI]
           [java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers
            HttpResponse$BodyHandlers WebSocket WebSocket$Listener WebSocketHandshakeException]
           [java.time Duration]
           [java.util.concurrent CompletableFuture]))

(def ^:private connect-ms
  15000)

(defn- refusal
  "The runtime answers an opening that it refuses over HTTP, with its own
  word as the body. Nil when the body is no such word."
  [^WebSocketHandshakeException refused]
  (let [response (.getResponse refused)
        [outcome form] (frame/read-one (.body response))]
    (when (and (= :ok outcome) (map? form) (contains? form :could-not))
      (assoc form :status (.statusCode response)))))

(defn- what-threw
  [^Throwable t]
  (let [cause (or (.getCause t) t)
        refused? (instance? WebSocketHandshakeException cause)]
    (or (when refused? (refusal cause))
        (cond-> {:could-not :reach-the-runtime :threw (.getName (class cause)) :said (.getMessage cause)}
          refused? (assoc :status (.statusCode (.getResponse ^WebSocketHandshakeException cause)))))))

(def ^:private shared-http-client
  "Shared, so that polling `waited` and opening connections do not leak a
  new SelectorManager thread on every call."
  (delay (-> (HttpClient/newBuilder) (.connectTimeout (Duration/ofMillis connect-ms)) (.build))))

(defn- http-client
  ^HttpClient []
  @shared-http-client)

(defn- post
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
  "The secret's hash is posted, and it is all that the runtime keeps of the
  secret. The runtime gives {:code :shown :link :ends}: the code under
  which the ask is found, the code as a person reads it, the link for her
  to open, and when the ask is over."
  [url asks]
  (let [secret (credential/make-secret)
        got (post url "/sign-in/program" {:hash (credential/hash-of secret) :asks asks})]
    (if (:could-not got) got (assoc got :secret secret))))

(defn waited
  "The secret is posted with the code: that is how the runtime knows that
  the program that waits is the one that asked. So the secret is on the
  wire before her yes, and is as safe as what carries it: over plain http,
  whoever can read the wire has it.

  The runtime says {:state :waiting}, {:state :yes :root :actor :sign-in},
  {:state :no} or {:state :over}."
  [url code secret]
  (let [got (post url "/sign-in/program/poll" {:code code :secret secret})]
    (cond-> got
      (= :yes (:state got)) (assoc :credential (credential/text (:root got) (:sign-in got) secret)))))

;; ------------------------------------------------------------------- the line

(defrecord Connection [^WebSocket socket state])

(defn- line-url
  [url]
  (str (if (.startsWith ^String url "https") "wss" "ws")
       (subs url (.indexOf ^String url "://"))
       "/line"))

(defn- heard!
  [state form]
  (cond
    (contains? form :hello) (deliver (:hello @state) (:hello form))
    (contains? form :bye) (swap! state assoc :bye (:bye form))
    ;; Each word and each watch has a number of its own here, so a result
    ;; under a watch's number answers its `:watch`: the line did not open it.
    (contains? form :gives) (if-let [waits (get-in @state [:waits (:id form)])]
                              (do (swap! state update :waits dissoc (:id form))
                                  (deliver waits (:gives form)))
                              (when-let [f (get-in @state [:watches (:id form)])]
                                (swap! state update :watches dissoc (:id form))
                                (try (f {:ended (:gives form)}) (catch Throwable _ nil))))
    (contains? form :id) (when-let [f (get-in @state [:watches (:id form)])]
                           (when (contains? form :ended)
                             (swap! state update :watches dissoc (:id form)))
                           (try (f (dissoc form :id)) (catch Throwable _ nil)))
    ;; One of the runtime's words that names no number: a frame of ours
    ;; that it could not read.
    :else (swap! state assoc :said form)))

(defn- last-word
  "What a connection that has ended answers with: the word of the runtime's
  `:bye` as it is, or else what `gone!` kept."
  [st]
  (or (:bye st) (:gone st)))

(defn- gone!
  [state why]
  (let [[before after] (swap-vals! state (fn [st]
                                           (assoc st
                                                  :gone (assoc why :could-not :reach-the-runtime)
                                                  :waits {} :watches {})))
        word (last-word after)]
    (deliver (:hello before) word)
    (doseq [waits (vals (:waits before))]
      (deliver waits word))
    (doseq [f (vals (:watches before))]
      (try (f {:ended word}) (catch Throwable _ nil)))))

(defn- listener
  "A text can arrive in parts."
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
  "A nil `:credential` is a visitor; a nil `:session` asks for a new one. A
  refused opening gives the runtime's own word, with the `:status` of its
  answer added: `:find-her-sign-in` for a credential that stands for
  nobody, `:find-her-session` for a session that is not open under it. An
  answer that holds no word of the runtime's gives `:reach-the-runtime`
  with the `:status`."
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
  "The runtime says {:who :root :sign-in :session :kind :own-lane :until},
  or {:visitor true}."
  [connection]
  (deref (:hello @(:state connection)) connect-ms {:could-not :hear-the-runtime}))

(defn- send!
  "Locks the connection, so that concurrent calls never collide on the JDK's
  `WebSocket.sendText`."
  [connection form]
  (try (locking connection
         (.join ^CompletableFuture (.sendText ^WebSocket (:socket connection) (frame/write form) true)))
       nil
       (catch Throwable t (what-threw t))))

(defn say
  "A word that did not come back can have been done: for an act, the
  program sends it again under the same UUID."
  ([connection word] (say connection word 90000))
  ([connection word wait-ms]
   (let [state (:state connection)
         waits (promise)
         reg (volatile! nil)]
     (swap! state
            (fn [st]
              (if-let [gone (last-word st)]
                (do (vreset! reg [:gone gone]) st)
                (let [id (:next st)]
                  (vreset! reg [:ok id])
                  (-> st (assoc :next (inc id)) (assoc-in [:waits id] waits))))))
     (let [[status val] @reg]
       (if (= :gone status)
         val
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
  "`f` is called with each message of the watch: {:anew answer}, {:changed
  {place place-answer}}, and, for a watch that is not left, {:ended word}
  as the last: the word with which the runtime ended the watch or did not
  open it, or the one that the connection went with. Gives the watch's
  number, or a word that holds `:could-not` when the watch could not be
  sent; `f` is not called for that."
  [connection query f]
  (let [state (:state connection)
        reg (volatile! nil)]
    (swap! state
           (fn [st]
             (if-let [gone (last-word st)]
               (do (vreset! reg [:gone gone]) st)
               (let [id (:next st)]
                 (vreset! reg [:ok id])
                 (-> st (assoc :next (inc id)) (assoc-in [:watches id] f))))))
    (let [[status val] @reg]
      (if (= :gone status)
        val
        (let [id val]
          (if-let [err (send! connection {:id id :word :watch :query query})]
            (do (swap! state update :watches dissoc id)
                err)
            id))))))

(defn leave
  [connection id]
  (swap! (:state connection) update :watches dissoc id)
  (say connection {:word :leave :watch id}))

(defn bye
  [connection]
  (:bye @(:state connection)))

(defn close!
  "The session stays open in the runtime: the program can connect again
  under it."
  [connection]
  (try (locking connection
         (.join ^CompletableFuture (.sendClose ^WebSocket (:socket connection) WebSocket/NORMAL_CLOSURE "done")))
       (catch Throwable _ nil))
  nil)
