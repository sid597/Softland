(ns softland.cli.node
  "The program's touch of Node: the word posted over HTTP, the stream of
  `:watch` read line by line, the credentials file, standard input and
  output, random bytes, the hash, the host's name and the environment. It
  is thin on purpose: each function does one thing that Node does, and
  hands plain data back. What a result means is `softland.cli.pure`'s.

  Inputs and results, by call:

    post-word        the server, a token or nil, a word -> a promise of a
                     response
    open-watch       the server, a token or nil, a word, two callbacks ->
                     a function that closes the stream
    read-stdin       nothing -> a promise of standard input's whole text
    stdin-lines      two callbacks -> standard input, one line at a time
    random-base64url a count -> that many random bytes as base64url
    sha256-hex       a text -> the SHA-256 of its UTF-8 bytes as hex
    read-text        a path -> the file's text, or nil
    write-private!   a path and a text -> the file written, mode 0600, its
                     folder made with mode 0700
    out!, err!       a line -> standard output, standard error

  A response is {:status n :body form :text t} when the server answered with one
  EDN form, or {:failure text} when it could not be reached or the body was
  no EDN. Every word goes as one EDN form with `Content-Type:
  application/edn` and, when there is a token, `Authorization: Bearer`.

  State and effects. No state of its own. Every function but the two hash
  and text helpers has an effect on the process, the network or the file
  system. It needs Node 18 or later, for `fetch` and web streams."
  (:require ["crypto" :as crypto]
            ["fs" :as fs]
            ["os" :as os]
            ["path" :as path]
            ["readline" :as readline]
            [clojure.string :as str]
            [softland.cli.pure :as pure]))

;; ------------------------------------------------------- the process

(defn out!
  "Takes a text. Writes it and a line end to standard output."
  [text]
  (.write (.-stdout js/process) (str text "\n")))

(defn err!
  "Takes a text. Writes it and a line end to standard error, where every
  message for the person goes."
  [text]
  (.write (.-stderr js/process) (str text "\n")))

(defn env
  "Takes the name of an environment variable. Gives its value, or nil."
  [the-name]
  (aget (.-env js/process) the-name))

(defn home
  "Gives the home folder of the user who runs the program."
  []
  (.homedir os))

(defn host-name
  "Gives the name of this machine."
  []
  (.hostname os))

(defn now-ms
  "Gives the time now, in milliseconds since 1970."
  []
  (js/Date.now))

(defn pause
  "Takes a span in milliseconds. Gives a promise that resolves to nil after
  it."
  [ms]
  (js/Promise. (fn [resolve] (js/setTimeout resolve ms))))

;; -------------------------------------------------------- bytes and hashes

(defn random-base64url
  "Takes a count. Gives that many bytes from Node's secure random numbers,
  as base64url without padding."
  [n]
  (.toString (.randomBytes crypto n) "base64url"))

(defn sha256-hex
  "Takes a text. Gives the SHA-256 of its UTF-8 bytes as 64 lower-case hex
  characters."
  [text]
  (-> (.createHash crypto "sha256") (.update text "utf8") (.digest "hex")))

;; ------------------------------------------------------------------ files

(defn read-text
  "Takes a path. Gives the file's text, or nil when there is no file there.
  Another fault, such as a file that cannot be read, throws."
  [the-path]
  (try (.readFileSync fs the-path "utf8")
       (catch :default e
         (if (= "ENOENT" (.-code e)) nil (throw e)))))

(defn write-private!
  "Takes a path and a text. Writes the text to the file so that only the
  user who runs the program can read it: the folder is made with mode 0700
  when it is not there, the text goes to a file beside it with mode 0600,
  which is then renamed over the path, so a reader never sees half a file.
  A folder that is already there keeps its mode. Throws when it cannot."
  [the-path text]
  (let [folder (.dirname path the-path)
        temporary (str the-path ".writing-" (.-pid js/process))]
    (.mkdirSync fs folder #js {:recursive true :mode 0700})
    (.writeFileSync fs temporary text #js {:mode 0600})
    (.chmodSync fs temporary 0600)
    (.renameSync fs temporary the-path)))

;; ---------------------------------------------------------- standard input

(defn read-stdin
  "Gives a promise of the whole text of standard input, read as UTF-8."
  []
  (js/Promise.
   (fn [resolve reject]
     (let [chunks #js []
           stdin (.-stdin js/process)]
       (.setEncoding stdin "utf8")
       (.on stdin "data" (fn [chunk] (.push chunks chunk)))
       (.on stdin "end" (fn [] (resolve (.join chunks ""))))
       (.on stdin "error" reject)))))

(defn stdin-lines
  "Takes a function of one line and a function of nothing. Calls the first
  with each line of standard input, without its line end, and the second
  once when standard input has ended."
  [on-line on-end]
  (let [lines (.createInterface readline #js {:input (.-stdin js/process) :terminal false})]
    (.on lines "line" on-line)
    (.on lines "close" on-end)))

;; -------------------------------------------------------------------- HTTP

(defn- request-init
  "Takes a token or nil, a word, and an AbortSignal or nil. Gives the
  options of `fetch` for the post of the word."
  [token word signal]
  (let [headers (cond-> {"Content-Type" "application/edn" "Accept" "application/edn"}
                  token (assoc "Authorization" (str "Bearer " token)))]
    (cond-> #js {:method "POST" :headers (clj->js headers) :body (pure/edn-text word)}
      signal (doto (aset "signal" signal)))))

(defn- reach-failure
  "Takes the server's URL and what `fetch` threw. Gives the response that
  says the server could not be reached, with Node's reason when it gave
  one."
  [server e]
  (let [cause (some-> e .-cause .-message)]
    {:failure (str "the server at " server " did not answer: "
                   (.-message e) (when cause (str " (" cause ")")))}))

(defn- response-of-text
  "Takes the status of an HTTP response and its body's text. Gives the
  response: the one EDN form that the body holds and the body's text, or a failure that quotes
  the start of a body that is no EDN."
  [status text]
  (let [[outcome form] (pure/read-edn text)]
    (if (= :ok outcome)
      {:status status :body form :text (str/trim text)}
      {:failure (str "the server answered " status " with a body that is no EDN ("
                     form "): " (subs text 0 (min 200 (count text))))})))

(defn post-word
  "Takes the server's URL, a token or nil, and a word. Posts the word to
  `/word` and gives a promise of the response. It never rejects: a fault of
  the network is a response with `:failure`."
  [server token word]
  (-> (js/fetch (str server "/word") (request-init token word nil))
      (.then (fn [response]
               (-> (.text response)
                   (.then (fn [text] (response-of-text (.-status response) text))))))
      (.catch (fn [e] (reach-failure server e)))))

(defn open-watch
  "Takes the server's URL, a token or nil, a word (`:watch`), a function of
  one message and a function of one end. Posts the word and reads the
  answer as a stream: each line that holds a form is read as EDN and handed
  to `on-message` with the line's text, which is what a caller shows, and an empty line, the server's heartbeat, is skipped.
  `on-end` is called once, with one of:

    {:ended true}            the server closed the stream
    {:closed true}           the program closed it
    {:status n :body form :text t}  the server answered with a status that is not
                             200, and so with one form and no stream
    {:failure text}          the server could not be reached, a line was no
                             EDN, or the stream broke

  Gives a function of nothing that closes the stream; calling it after the
  end does nothing."
  [server token word on-message on-end]
  (let [controller (js/AbortController.)
        closed? (atom false)
        ended? (atom false)
        end! (fn [what] (when-not @ended? (reset! ended? true) (on-end what)))
        decoder (js/TextDecoder. "utf-8")
        held (atom "")
        line! (fn [line]
                (when-not (str/blank? line)
                  (let [[outcome form] (pure/read-edn line)]
                    (if (= :ok outcome)
                      (on-message form line)
                      (throw (ex-info (str "a line of the stream is no EDN: " form) {:no-edn true}))))))
        pump (fn pump [reader]
               (-> (.read reader)
                   (.then (fn [chunk]
                            (if (.-done chunk)
                              (do (let [rest-text (str @held (.decode decoder))]
                                    (reset! held "")
                                    (line! rest-text))
                                  (end! {:ended true}))
                              (let [text (str @held (.decode decoder (.-value chunk) #js {:stream true}))
                                    parts (str/split text #"\n" -1)]
                                (reset! held (last parts))
                                (doseq [line (butlast parts)] (line! line))
                                (pump reader)))))))]
    (-> (js/fetch (str server "/word") (request-init token word (.-signal controller)))
        (.then (fn [response]
                 (if (= 200 (.-status response))
                   (pump (.getReader (.-body response)))
                   (-> (.text response)
                       (.then (fn [text] (end! (response-of-text (.-status response) text))))))))
        (.catch (fn [e]
                  (cond
                    @closed? (end! {:closed true})
                    (:no-edn (ex-data e)) (do (.abort controller) (end! {:failure (ex-message e)}))
                    :else (end! (reach-failure server e))))))
    (fn []
      (when-not (or @closed? @ended?)
        (reset! closed? true)
        (.abort controller)))))
