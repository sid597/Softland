(ns softland.server.http
  "The face over HTTP: the words for a system that we do not compile, and
  the few pages by which a person signs in and says yes to a program. It is
  a Ring handler. It requires Ring's small parts, Missionary and the seam.

  One word is one request, or one stream. A system posts a word
  (`softland.server.words`) as one EDN form to `/word`, with its credential
  in the `Authorization` header, and gets one EDN form back; for `:watch`
  the answer stays open and carries one form on each line until the system
  closes it. That is derived from the systems that connect and not taken
  from habit: a command and a tool call are a request and an answer; a
  stream's end is the watch's end, and HTTP gives the leaving and the
  holding back; with the credential on each request any copy of the server
  can answer; and what ends TLS in front already speaks it.

  The routes:

    POST /word              one word -> its result, or a stream
    GET  /                  the page: sign in, or who is signed in
    POST /sign-in/google    Google's post of an ID token -> a cookie
    POST /sign-in/stand-in  the same, with a token that a stand-in for
                            Google signs here; only when the opener gave one
    GET  /sign-in/yes       the page on which she says yes to a program
    POST /sign-in/yes       her yes, or her no
    POST /sign-out          ends the cookie's session
    GET  /health            that the process answers

  Two carriers of a credential, kept apart. A word takes its token from the
  `Authorization` header and never from a cookie: a browser adds a cookie
  to a request that another site made, and adds no header. The pages take
  the token from the cookie, and each post that rests on the cookie must
  carry an `Origin` that is this server's own. So no page of another site
  can make a signed-in browser say a word, a yes or a sign-out.

  A program's sign-in. A program has no browser, and the proof that it is
  her is a sign-in in one. So the program makes a token, keeps it, and
  shows her a link to `/sign-in/yes` that carries the token's hash, with a
  short code. She opens the link where she is signed in, sees the code
  again and what asks, and says yes: the hash then stands for her. The
  program waits with `{:word :who-am-i :wait-ms n}`. Nothing is written
  for a sign-in that is under way: it lives in the program that holds the
  token, and her yes is the first event.

  What that sign-in does not hold, said plainly. Whoever makes a link and
  gets a signed-in person to say yes on it acts as her until the session
  ends: the page warns her, and nothing else stops it. Whoever sees a
  program's link before its person says yes can say yes first, and the
  program then acts as that other person and is not told. The server
  cannot make a link grow old, since it keeps nothing of one. And she has
  no page that lists the programs that act as her; she ends one with its
  own link. Each is due before a stranger can reach the server
  (src/proposal/server-2026-11-20/BUILD.md).

  What a result's status is. 200 when the server did the word, whatever the
  store said of it: a receipt, an answer, a gate's refusal read back, a word
  of the door or of the read path. The server's own words have the
  statuses that `softland.server.words/could-nots` gives. The body is EDN
  either way, and a system reads the body.

  The stream. `application/edn`, one form and a line end for each message
  of `softland.server.seam/watch`; an empty line when nothing changed for
  `beat-ms`, so that a thing in front does not close a quiet stream and a
  system that went away is noticed at the next write. The system leaves by
  closing the connection; the server then cancels the flow, which leaves
  the live read. The request's thread writes the stream and stays for its
  life, so the program that runs this handler gives each request a thread
  that is cheap to hold (`softland.server.main`).

  State. None of its own. `handler` closes over the seam and its options.

  Not here. The browser's Electric program: it is another tree, and it adds
  its socket in front of this handler. A bound on how many requests one
  system makes: `softland.server.seam/ration`, not built."
  (:require [clojure.string :as str]
            [missionary.core :as m]
            [ring.core.protocols :as protocols]
            [ring.util.codec :as codec]
            [softland.server.frame :as frame]
            [softland.server.google :as google]
            [softland.server.google-stand-in :as stand-in]
            [softland.server.seam :as seam]
            [softland.server.session.credential :as credential]
            [softland.server.session.record :as record]
            [softland.server.sign-in :as sign-in]
            [softland.server.words :as words])
  (:import [java.io ByteArrayOutputStream InputStream OutputStream]
           [java.nio.charset StandardCharsets]
           [java.util.concurrent Semaphore TimeUnit]))

(def beat-ms
  "How long a stream stays quiet before the server writes an empty line. A
  number of the build, kept and not decided."
  15000)

(def referrer-policy
  "The referrer policy of every page that this server gives. Two things
  hang on it. A page's own post must carry this server's `Origin`, which
  each post that rests on the cookie is held to: under `no-referrer` a
  browser writes `Origin: null` on a form's post to its own site, and the
  server then refused its own forms (seen with Chrome on 20 November 2026,
  by the check that drives a browser). And the address of a page must not
  reach another site: the page on which she says yes carries the hash of a
  program's credential in its address. Under this policy a request to the
  same site carries the origin, and a request to another site, Google's
  among them, carries the origin alone and no address."
  "strict-origin-when-cross-origin")

(def ^:private max-form-bytes
  "How many bytes the form of a page's post can be: an ID token and a few
  short fields."
  65536)

;; ----------------------------------------------------------- reading a request

(defn- body-text
  "Reads the body of a request as UTF-8 text, with a bound that is tested
  before the body is read and again while it is read. Takes the request and
  the most bytes. Gives [:ok text], or [:no :too-large]."
  [request most]
  (let [declared (some-> (get-in request [:headers "content-length"]) parse-long)
        ^InputStream in (:body request)]
    (cond
      (and declared (< most declared)) [:no :too-large]
      (nil? in) [:ok ""]
      :else
      (let [buffer (byte-array 8192)
            out (ByteArrayOutputStream.)]
        (loop [total 0]
          (let [n (.read in buffer)]
            (cond
              (neg? n) [:ok (String. (.toByteArray out) StandardCharsets/UTF_8)]
              (< most (+ total n)) [:no :too-large]
              :else (do (.write out buffer 0 n) (recur (+ total n))))))))))

(defn- form-of
  "Reads the form of a page's post. Gives a map from a field's name to its
  text, or nil for a body that is too large."
  [request]
  (let [[outcome text] (body-text request max-form-bytes)]
    (when (= :ok outcome)
      (let [form (codec/form-decode text "UTF-8")]
        (if (map? form) form {})))))

(defn- query-of
  "Gives the fields of a request's query string, as a map of texts."
  [request]
  (let [form (some-> (:query-string request) (codec/form-decode "UTF-8"))]
    (if (map? form) form {})))

(defn- cookie-of
  "Takes a request and a cookie's name. Gives the cookie's text, or nil."
  [request cookie]
  (some (fn [part]
          (let [[k v] (str/split (str/trim part) #"=" 2)]
            (when (= cookie k) v)))
        (str/split (or (get-in request [:headers "cookie"]) "") #";")))

;; ------------------------------------------------------------ the answers

(defn- edn
  "Takes a status and a thing. Gives the response that carries the thing as
  one EDN form and a line end."
  [status thing]
  {:status status
   :headers {"Content-Type" "application/edn; charset=utf-8" "Cache-Control" "no-store"}
   :body (str (frame/write thing) "\n")})

(defn- status-of
  "Takes a result of a word. Gives its status: the one that
  `softland.server.words/could-nots` gives a word of the server's own, and
  200 for everything else."
  [result]
  (or (when (map? result) (get-in words/could-nots [(:could-not result) :status]))
      200))

(defn- answered
  "Takes a result of a word. Gives its response."
  [result]
  (edn (status-of result) result))

;; ------------------------------------------------------------------ the words

(defn- token-of-header
  "Takes a request. Gives [:ok the token or nil], or [:no] when the request
  has an Authorization header that holds no token of the form: a system
  that sent a credential is told that it did not count."
  [request]
  (let [header (get-in request [:headers "authorization"])]
    (cond
      (nil? header) [:ok nil]
      :else (if-let [token (credential/of-header header)] [:ok token] [:no]))))

(defn- standing-within
  "Waits for a credential to stand for somebody. Takes the seam, a token and
  a wait in milliseconds. Gives the first standing that is not
  `:not-known`, or nil when the wait passed."
  [the-seam token wait-ms]
  (m/? (m/timeout
        (m/reduce (fn [_ standing] (when-not (= :not-known (:state standing)) (reduced standing)))
                  nil
                  (seam/standing the-seam token))
        wait-ms
        nil)))

(defn- with-own-layer
  "Takes the seam and what a signed-in credential stands for, {:who
  :session :until}. Gives it with `:own-layer`, the id of her own layer,
  when her entity holds one (`softland.server.sign-in/own-layer-of`)."
  [the-seam about]
  (let [layer (m/? (sign-in/own-layer-of the-seam (:who about)))]
    (cond-> about layer (assoc :own-layer layer))))

(defn- who-am-i
  "The word `:who-am-i`. Takes the seam, the token or nil, the word, and
  what `softland.server.seam/actor` gave. Gives the result: {:visitor
  true}, or {:who :session :until :own-layer}; or the seam's word. With a
  wait, for a token that stands for nobody yet, it waits for her yes."
  [the-seam token word actor]
  (cond
    (seam/actor? actor)
    (let [about (seam/about actor)]
      (if (:who about) (with-own-layer the-seam about) about))

    (and (= :not-known (:why actor)) (pos? (:wait-ms word)))
    (let [standing (standing-within the-seam token (:wait-ms word))]
      (case (:state standing)
        :live (with-own-layer the-seam (select-keys standing [:who :session :until]))
        :not-reached (words/could-not :reach-the-sessions nil)
        nil actor
        (words/could-not :find-her-session {:why (:state standing)})))

    :else actor))

(defn- one-result
  "Does a word whose result is one thing. Takes the seam, the actor and the
  word in its one form. Gives what the seam's task gave."
  [the-seam actor word]
  (case (:word word)
    :act-ids  (m/? (seam/act-ids the-seam actor (:layer word) (:count word)))
    :send     (m/? (seam/send the-seam actor (:act word) (:act-id word)))
    :send-all (m/? (seam/send-all the-seam actor (:acts word)))
    :answer   (m/? (seam/answer the-seam actor (:act-id word) (:digest word) (:wait-ms word)))
    :read     (m/? (seam/read the-seam actor (:query word)))
    :sign-out (m/? (seam/sign-out the-seam actor))))

(defn- write-stream
  "Writes a flow of messages to an output stream, one EDN form on each
  line, until the flow ends or the stream cannot be written. Takes the flow
  and the stream. It runs on the caller's thread and holds it for the
  stream's life.

  It drives the flow by hand: a semaphore is released when a message is
  ready and when the flow has ended, and the thread waits on it for
  `beat-ms` at most. When the wait passes it writes an empty line. A write
  that throws means that the system went away: the flow is cancelled, and
  the thread stays until the flow says that it has ended, so that the live
  read is left before the request is over."
  [flow ^OutputStream out]
  (let [ready (Semaphore. 0)
        ended (atom false)
        iterator (flow (fn [] (.release ready))
                       (fn [] (reset! ended true) (.release ready)))
        write! (fn [^String line]
                 (try (.write out (.getBytes line StandardCharsets/UTF_8))
                      (.flush out)
                      true
                      (catch Throwable _ false)))]
    (loop [open? true]
      (if (.tryAcquire ready beat-ms TimeUnit/MILLISECONDS)
        (when-not @ended
          (let [message (try @iterator (catch Throwable _ ::none))]
            (if (and open? (not= ::none message))
              (if (write! (str (frame/write message) "\n"))
                (recur true)
                (do (iterator) (recur false)))
              (recur open?))))
        (if (and open? (write! "\n"))
          (recur true)
          (do (when open? (iterator)) (recur false)))))
    (try (.close out) (catch Throwable _ nil))))

(defn- stream-of
  "Takes a flow of messages. Gives the response whose body writes them as a
  stream. `X-Accel-Buffering: no` asks a proxy in front not to hold the
  lines back."
  [flow]
  {:status 200
   :headers {"Content-Type" "application/edn; charset=utf-8"
             "Cache-Control" "no-store"
             "X-Accel-Buffering" "no"}
   :body (reify protocols/StreamableResponseBody
           (write-body-to-stream [_ _ out]
             (write-stream flow out)))})

(defn- word!
  "POST /word. Takes the seam and the request. The steps, in order: the
  credential, from the header; the bound on the frame, which is small for
  a request whose credential stands for nobody, and is tested before the
  frame is read; the one EDN form, whose longest token is bounded too; the
  check of the word; the word.

  The credential comes first so that somebody with no account cannot make
  the server read a large frame: reading costs more than the frame's bytes
  say (`softland.server.frame`)."
  [the-seam request]
  (let [[outcome token] (token-of-header request)
        actor (if (= :no outcome)
                (words/could-not :find-her-session {:why :not-a-token})
                (m/? (seam/actor the-seam token)))
        most (if (and (seam/actor? actor) (seam/who-of actor))
               words/max-frame-bytes
               words/max-visitor-frame-bytes)
        [outcome text] (body-text request most)]
    (if (= :no outcome)
      (assoc (answered (words/could-not :read-the-frame {:why :too-large :most most}))
             :status 413)
      (let [[outcome form] (frame/read-one text)]
        (if (= :no outcome)
          (answered (words/could-not :read-the-frame {:why form}))
          (let [{word :ok why :refuse at :at} (words/check form)]
            (cond
              why
              (answered (words/could-not :read-the-word {:why why :at at}))

              (= :who-am-i (:word word))
              (answered (who-am-i the-seam token word actor))

              (not (seam/actor? actor))
              (answered actor)

              (= :watch (:word word))
              (stream-of (seam/watch the-seam actor (:query word)))

              :else
              (answered (one-result the-seam actor word)))))))))

;; ------------------------------------------------------------------ the pages

(defn- escaped
  "Takes a text. Gives it with the five characters of HTML escaped."
  [text]
  (-> (str text)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")
      (str/replace "'" "&#39;")))

(defn- page
  "Takes a status and the HTML of a page's body. Gives the response. A page
  is never framed by another site and never kept by a browser."
  [status body]
  {:status status
   :headers {"Content-Type" "text/html; charset=utf-8"
             "Cache-Control" "no-store"
             "X-Frame-Options" "DENY"
             "Content-Security-Policy" "frame-ancestors 'none'"
             "Referrer-Policy" referrer-policy}
   :body (str "<!doctype html><html><head><meta charset=\"utf-8\">"
              "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
              "<title>Softland</title>"
              "<style>body{font:16px system-ui;background:#16141f;color:#eeeef6;max-width:34rem;"
              "margin:12vh auto;padding:0 1rem}input,button{font:inherit;padding:.45rem .7rem;"
              "border-radius:6px;border:1px solid #555;background:#221f2e;color:inherit}"
              "code{background:#221f2e;padding:.1rem .35rem;border-radius:4px}"
              ".warn{color:#f0b070}form{margin:.6rem 0}</style></head><body>"
              "<h2>Softland</h2>" body "</body></html>")})

(defn- redirect
  "Takes a local path, and when there is one, the text of a Set-Cookie
  header. Gives the response that sends the browser there."
  [to & [cookie]]
  (cond-> {:status 303 :headers {"Location" to "Cache-Control" "no-store"} :body ""}
    cookie (assoc-in [:headers "Set-Cookie"] cookie)))

(defn- way-on
  "Takes the options of the handler and what a request gave as the place to
  go on to after a sign-in. Gives a path of this server, and never the text
  that was given: one of the three places that a sign-in leads to, built
  here.

    the yes page   when the text names it, with a hash that is a hash's
                   text: the path is written anew from the hash and from
                   what asks, both read out of the text
    the program    when the text is the path of the browser's program
    `/`            for anything else

  A text that is followed as it came can send her browser to another site
  by a character that a browser drops while it reads an address: a tab
  after the first slash did so (the review of 20 November 2026 ran it)."
  [{:keys [app-path]} next]
  (let [next (str next)]
    (cond
      (and app-path (= app-path next)) app-path

      (str/starts-with? next "/sign-in/yes?")
      (let [fields (codec/form-decode (subs next (count "/sign-in/yes?")) "UTF-8")
            fields (if (map? fields) fields {})
            hash (get fields "hash")
            what (get fields "what")]
        (if (record/hash-text? hash)
          (str "/sign-in/yes?hash=" hash
               (when (string? what) (str "&what=" (codec/url-encode what))))
          "/"))

      :else "/")))

(defn- cookie-text
  "Takes the options of the handler, a token and for how many seconds it is
  kept. Gives the text of the Set-Cookie header. The cookie is not read by
  a page's code (`HttpOnly`), and is sent over TLS alone when this server's
  origin is `https`. `SameSite=Lax`: a browser leaves it off a post, a
  fetch and a socket that another site's page makes, and sends it when a
  link on another site is followed to a page here. So a page that is
  opened from elsewhere, the yes page among them, sees her as signed in;
  what such a page then does rests on her own click and on the `Origin` of
  its post. A browser also sends it to every server on the same host,
  whatever its port: on `localhost`, to each program that listens there."
  [{:keys [origin]} token seconds]
  (str credential/cookie-name "=" token "; Path=/; HttpOnly; SameSite=Lax; Max-Age=" seconds
       (when (str/starts-with? (str origin) "https://") "; Secure")))

(defn- actor-of-cookie
  "Takes the seam and a request. Gives [token actor] for a cookie whose
  credential stands for somebody now, or nil."
  [the-seam request]
  (when-let [token (credential/of-cookie (get-in request [:headers "cookie"]))]
    (let [actor (m/? (seam/actor the-seam token))]
      (when (and (seam/actor? actor) (seam/who-of actor))
        [token actor]))))

(defn- own-origin?
  "Takes the options and a request. Gives true when the request's `Origin`
  is one of this server's own. A post that rests on the cookie must pass
  it."
  [{:keys [origins]} request]
  (contains? origins (get-in request [:headers "origin"])))

(defn- home
  "GET /. The page on which she signs in, or sees that she is signed in."
  [{:keys [the-seam google stand-in origin app-path] :as options} request]
  (let [next (way-on options (get (query-of request) "next"))]
    (if-let [[_ actor] (actor-of-cookie the-seam request)]
      (if (not= "/" next)
        (redirect next)
        (page 200 (str "<p>You are signed in. Your id is <code>" (escaped (seam/who-of actor)) "</code>.</p>"
                       (when app-path (str "<p><a href=\"" (escaped app-path) "\" style=\"color:inherit\">Open Softland</a></p>"))
                       "<form method=\"post\" action=\"/sign-out\"><button>Sign out</button></form>")))
      (page 200
            (str (when (and google (not stand-in))
                   (str "<script src=\"https://accounts.google.com/gsi/client\" async></script>"
                        "<div id=\"g_id_onload\" data-client_id=\"" (escaped (:client-id google)) "\""
                        " data-login_uri=\"" (escaped (str origin "/sign-in/google?next=" (codec/url-encode next))) "\""
                        " data-ux_mode=\"redirect\" data-auto_prompt=\"false\"></div>"
                        "<div class=\"g_id_signin\" data-type=\"standard\"></div>"))
                 (when stand-in
                   (str "<p class=\"warn\">This server runs with a stand-in for Google. Anybody who reaches it can "
                        "sign in as any account. It is for one machine, on a store that is thrown away.</p>"
                        "<form method=\"post\" action=\"/sign-in/stand-in?next=" (escaped (codec/url-encode next)) "\">"
                        "<input name=\"sub\" placeholder=\"account number\" value=\"1000001\"> "
                        "<input name=\"email\" placeholder=\"email\" value=\"sid@example.com\"> "
                        "<button>Sign in</button></form>"))
                 (when-not (or google stand-in)
                   "<p class=\"warn\">No way to sign in is set: the server was started without a Google client id.</p>"))))))

(defn- signed-in!
  "The end of a sign-in at a browser. Takes the options, the request and
  the text of an ID token. It checks the token against the issuer's keys,
  finds or registers her, begins a session under a new token, and sends
  the browser on with the token in a cookie. A token that fails the check,
  and a step that could not be done, give a page that says so and no
  cookie."
  [{:keys [the-seam google] :as options} request id-token]
  (let [[outcome fetched] (if (google/key-name id-token)
                            ((:keys-fn google))
                            ;; A text that the check turns away at its first
                            ;; two tests costs no request to the issuer.
                            [:ok {}])]
    (if (not= :ok outcome)
      (page 503 "<p>The keys of the sign-in service could not be fetched. Try again.</p>")
      (let [{claims :ok why :refuse} (google/check id-token {:client-id (:client-id google)
                                                             :issuers (:issuers google)
                                                             :keys fetched
                                                             :now-ms (System/currentTimeMillis)})]
        (if why
          (page 401 (str "<p>The sign-in was not taken: <code>" (escaped (name why)) "</code>.</p>"
                         "<p><a href=\"/\" style=\"color:inherit\">Back</a></p>"))
          (let [person (m/? (sign-in/person-for the-seam claims))]
            (if (:could-not person)
              (page 503 "<p>The sign-in could not be written. Try again.</p>")
              (let [token (credential/make)
                    begun (m/? (seam/begin the-seam (:who person) (credential/hash-of token)))]
                (if (:could-not begun)
                  (page 503 "<p>The session could not be begun. Try again.</p>")
                  (redirect (way-on options (get (query-of request) "next"))
                            (cookie-text options token
                                         (quot (- (:until begun) (System/currentTimeMillis)) 1000))))))))))))

(defn- from-google!
  "POST /sign-in/google. Google's button posts the ID token as `credential`
  and a text `g_csrf_token`, and sets a cookie of the same name: the two
  must be the same text, which a post from another site cannot make them."
  [options request]
  (let [form (form-of request)
        in-form (get form "g_csrf_token")
        in-cookie (cookie-of request "g_csrf_token")]
    (if (and form (seq in-form) (= in-form in-cookie))
      (signed-in! options request (get form "credential"))
      (page 400 "<p>The sign-in was not taken: the request did not come from Google's button.</p>"))))

(defn- from-stand-in!
  "POST /sign-in/stand-in, only when the opener gave a stand-in. It signs
  an ID token for the account that the form names and goes on as Google's
  post does, through the same check."
  [{:keys [stand-in] :as options} request]
  (let [form (form-of request)
        sub (str/trim (str (get form "sub")))]
    (if (and form (own-origin? options request) (seq sub))
      (signed-in! options request
                  (stand-in/token stand-in
                                  (cond-> {"sub" sub}
                                    (seq (get form "email")) (assoc "email" (get form "email") "email_verified" true))
                                  (System/currentTimeMillis)))
      (page 400 "<p>The sign-in was not taken.</p>"))))

(defn- yes-page
  "GET /sign-in/yes. The page on which she says yes to a program: what
  asks, in the program's own words, and the short code of its credential's
  hash, which the program shows too. Somebody who is not signed in is sent
  to sign in first and comes back."
  [{:keys [the-seam]} request]
  (let [{hash "hash" what "what"} (query-of request)]
    (cond
      (not (record/hash-text? hash))
      (page 400 "<p>This link names no program.</p>")

      (nil? (actor-of-cookie the-seam request))
      (redirect (str "/?next=" (codec/url-encode (str "/sign-in/yes?" (:query-string request)))))

      :else
      (page 200
            (str "<p>A program asks to act as you, with everything that you may do, for 30 days.</p>"
                 "<p class=\"warn\">Say yes only if you started that program yourself, just now, and it shows the code "
                 "<code>" (credential/code-of hash) "</code>. If a link brought you here and you started no program, "
                 "say no: whoever made the link would act as you.</p>"
                 "<p>What the program says of itself, which anybody could have written: <code>"
                 (escaped (subs (str what) 0 (min 80 (count (str what))))) "</code></p>"
                 "<form method=\"post\" action=\"/sign-in/yes\">"
                 "<input type=\"hidden\" name=\"hash\" value=\"" hash "\">"
                 "<button name=\"say\" value=\"yes\">Yes</button> "
                 "<button name=\"say\" value=\"no\">No</button></form>")))))

(defn- yes!
  "POST /sign-in/yes. Her yes makes the program's hash stand for her; her
  no refuses the hash for good. The post must come from this server's own
  page and rest on a cookie that stands for her."
  [{:keys [the-seam] :as options} request]
  (let [form (form-of request)
        hash (get form "hash")
        signed-in (actor-of-cookie the-seam request)]
    (cond
      (not (and form (own-origin? options request) signed-in (record/hash-text? hash)))
      (page 400 "<p>That was not taken.</p>")

      (= "yes" (get form "say"))
      (let [begun (m/? (seam/begin the-seam (seam/who-of (second signed-in)) hash))]
        (cond
          (not (:could-not begun)) (page 200 "<p>Done. The program is signed in as you; go back to it.</p>")
          (= :credential-in-use (:why begun)) (page 409 "<p>This link was used before, or refused. Start the program's sign-in again.</p>")
          :else (page 503 "<p>That could not be written. Try again.</p>")))

      :else
      (if (:could-not (m/? (seam/refuse the-seam (second signed-in) hash)))
        (page 409 "<p>That was not taken: the link stands for somebody else.</p>")
        (page 200 "<p>Refused. The program is not signed in.</p>")))))

(defn- sign-out!
  "POST /sign-out. Ends the session of the cookie's credential, for every
  face and every copy of the server, and takes the cookie away. Only a
  cookie that stands for somebody now ends anything: a text that has the
  form of a token and stands for nobody is taken away and nothing is
  written for it, so a stranger cannot fill the server's log with ends."
  [{:keys [the-seam] :as options} request]
  (if-not (own-origin? options request)
    (page 400 "<p>That was not taken.</p>")
    (do (when-let [[_ actor] (actor-of-cookie the-seam request)]
          (m/? (seam/sign-out the-seam actor)))
        (redirect "/" (cookie-text options "" 0)))))

;; --------------------------------------------------------------- the handler

(defn handler
  "Makes the Ring handler of the face over HTTP. Takes a map:

    :the-seam  an opened seam (`softland.server.seam/open`)
    :origin    this server's own origin as a browser names it, such as
               `http://localhost:8400`
    :origins   the origins whose posts are taken; left out, the one origin
    :google    {:client-id :issuers :keys-fn}, where `:keys-fn` is a
               function of no argument that gives [:ok {kid public-key}]
               or [:threw info]; nil when there is no way to sign in
    :stand-in  a stand-in for Google
               (`softland.server.google-stand-in/make`), or nil. With one,
               `:google` is made from it and anybody can sign in as anybody
    :app-path  the path of the browser's program, when one is served in
               front of this handler; the page then links to it

  Gives a function from a request to a response. A path that is none of
  the routes gives 404. The handler throws for nothing that a request
  holds; a throw of the store's client is the seam's to turn into a word."
  [{:keys [origin origins stand-in google] :as options}]
  (let [options (assoc options
                       :origins (or origins #{origin})
                       :google (if stand-in
                                 {:client-id (:client-id stand-in)
                                  :issuers #{(:issuer stand-in)}
                                  :keys-fn (fn [] [:ok (:keys stand-in)])}
                                 google))
        the-seam (:the-seam options)]
    (fn [request]
      (let [method (:request-method request)
            uri (:uri request)]
        (try
          (cond
            (and (= :post method) (= "/word" uri)) (word! the-seam request)
            (and (= :get method) (= "/" uri)) (home options request)
            (and (= :get method) (= "/health" uri)) (edn 200 {:ready true})
            (and (= :post method) (= "/sign-in/google" uri) (:google options) (not stand-in)) (from-google! options request)
            (and (= :post method) (= "/sign-in/stand-in" uri) stand-in) (from-stand-in! options request)
            (and (= :get method) (= "/sign-in/yes" uri)) (yes-page options request)
            (and (= :post method) (= "/sign-in/yes" uri)) (yes! options request)
            (and (= :post method) (= "/sign-out" uri)) (sign-out! options request)
            :else {:status 404 :headers {"Content-Type" "text/plain; charset=utf-8"} :body "Not found\n"})
          (catch Throwable t
            {:status 500
             :headers {"Content-Type" "text/plain; charset=utf-8"}
             :body (str "The server failed: " (.getName (class t)) "\n")}))))))
