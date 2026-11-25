(ns softland.runtime.http
  "The face over plain HTTP: the few pages by which a person signs in,
  confirms her name and answers a program; the two requests by which a
  program asks to be signed in and waits; and who a connection to the line
  is. It is a Ring handler and one function for the line's upgrade. It
  requires Ring's small parts, Missionary, the seam and sign-in's steps.

  The routes:

    GET  /                        the page: sign in, or who is signed in
    POST /sign-in/google          Google's post of an ID token -> a cookie
    POST /sign-in/stand-in        the same, with a token that a stand-in for
                                  Google signs here; only when the opener
                                  gave one
    GET  /sign-in/name            the box with her provider's name for her
    POST /sign-in/name            the name that she confirms
    GET  /yes                     the page on which she answers a program
    POST /yes                     her yes, or her no
    POST /sign-out                ends the cookie's sign-in
    POST /sign-in/program         a program asks -> a code
    POST /sign-in/program/poll    a program waits -> the ask's state
    GET  /health                  that the process answers

  The words are not here. Everything that speaks the words speaks them over
  the line (`softland.runtime.line`), and `accept` is where a request to
  the line becomes a caller.

  Two carriers of a credential, kept apart. A browser's sign-in is taken
  from the cookie and never from a header; a program's from the
  `Authorization` header and never from a cookie; and Auth holds which of
  the two a sign-in is, so that a secret of one kind in the carrier of the
  other stands for nobody (`softland.runtime.seam/caller`). Each post that
  rests on the cookie, and each connection to the line that does, must
  carry an `Origin` that is this runtime's own. So no page of another site
  can make a signed-in browser say a word, a yes or a sign-out; and a
  program that holds its own secret cannot say her yes.

  A program's sign-in. The program makes a secret, keeps it, and posts its
  hash with what it says of itself. The runtime keeps the ask in Auth, with
  an end, and gives a code. The program shows the code and a link. She
  opens the link where she is signed in, sees the code again, and says yes,
  as herself or as a new agent with a name, or no. The program asks for the
  ask's state with its code and its secret until she has answered, and then
  puts its credential together from what it is told. An ask grows old: one
  that is answered after its end is refused.

  A first sign-in. Google's post proves an account. `softland.runtime.
  sign-in/enter!` finds its person, or takes the person of the invitation
  that the account proves, and makes her own lane; an account that proves
  no invitation is turned away. A person who has confirmed no name is sent
  to the box: her provider's name for her, which she confirms or changes
  (Sid, 21 November 2026: \"we can make C default\").

  Where a request comes from. The address by which posts are paced is the
  request's own; or, when the request comes from a proxy that the opener
  names as trusted, the last address in its `X-Forwarded-For` that is not
  one of those proxies. An IPv6 address counts by its first 64 bits, the
  part that one network is given, so that one network cannot take a new
  address for each post (`client-address`).

  The pace. Each post to `/sign-in/*` is paced by that address
  (`softland.runtime.seam/ration-sign-in!`). A poll that proves a
  program's own ask, its code with its secret, is given back to the
  address, so that programs that wait behind one address do not use it up
  for a person's sign-in; the polls of one ask are paced on their own.

  State. None of its own. `handler` and `accept` close over the seam and
  the options.

  Not here. The browser's client, which is another build (the runtime's
  page, piece 7). MCP (piece 6). Counts by person in Auth, and blocks:
  `softland.runtime.seam/ration`, not built."
  (:require [clojure.string :as str]
            [missionary.core :as m]
            [ring.util.codec :as codec]
            [softland.runtime.google :as google]
            [softland.runtime.google-stand-in :as stand-in]
            [softland.runtime.seam :as seam]
            [softland.runtime.sign-in :as sign-in]
            [softland.wire.credential :as credential]
            [softland.wire.frame :as frame]
            [softland.wire.words :as words])
  (:import [java.io ByteArrayOutputStream InputStream]
           [java.net Inet4Address Inet6Address InetAddress]
           [java.nio.charset StandardCharsets]
           [java.util UUID]))

(def referrer-policy
  "The referrer policy of every page that this runtime gives. A page's own
  post must carry this runtime's `Origin`, which each post that rests on
  the cookie is held to: under `no-referrer` a browser writes `Origin:
  null` on a form's post to its own site (seen with Chrome on 20 November
  2026). And the address of a page must not reach another site. Under this
  policy a request to the same site carries the origin, and a request to
  another site carries the origin alone and no address."
  "strict-origin-when-cross-origin")

(def ^:private max-form-bytes
  "How many bytes the form of a page's post, or the EDN of a program's
  request, can be: an ID token and a few short fields."
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

(defn- carried
  "Takes a text and the most characters that it may have. Gives it cut to
  at most that many, never between the two halves of a surrogate pair, and
  with each surrogate that has no other half written as U+FFFD, as a UTF-8
  reader writes what it cannot read: Auth does not take a text that UTF-8
  cannot carry, and a provider's claim, read from JSON, can hold one."
  [text most]
  (let [text (str text)
        n (count text)
        end (let [end (min n (long most))]
              (if (and (< 0 end n) (Character/isHighSurrogate (.charAt text (dec end)))
                       (Character/isLowSurrogate (.charAt text end)))
                (dec end)
                end))
        out (StringBuilder.)]
    (loop [i 0]
      (when (< i end)
        (let [c (.charAt text i)]
          (cond
            (and (Character/isHighSurrogate c) (< (inc i) end) (Character/isLowSurrogate (.charAt text (inc i))))
            (do (.append out c) (.append out (.charAt text (inc i))) (recur (+ i 2)))

            (Character/isSurrogate c)
            (do (.append out \uFFFD) (recur (inc i)))

            :else
            (do (.append out c) (recur (inc i)))))))
    (str out)))

(defn- uuid-of
  "Takes a text. Gives the UUID that it is, or nil."
  [text]
  (when (and (string? text) (re-matches #"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" text))
    (UUID/fromString text)))

(defn- address-key
  "Takes an address as a text, an IPv4 or IPv6 literal. Gives the key by
  which its posts are paced: an IPv4 address as it is, an IPv4 address
  mapped into IPv6 as the IPv4 one, and an IPv6 address by its first 64
  bits, written `<hex>/64`. A text that is no literal address is given as
  it is. It looks up no name: a text without `:` is never parsed."
  [text]
  (let [text (str text)
        bare (-> text (str/replace #"^\[|\]$" "") (str/replace #"%.*$" ""))]
    (if-not (str/includes? bare ":")
      text
      (try
        (let [address (InetAddress/getByName bare)]
          (cond
            (instance? Inet4Address address) (.getHostAddress address)
            (instance? Inet6Address address)
            (let [bytes (.getAddress address)]
              (str (apply str (map (fn [b] (format "%02x" (bit-and 0xff (long b)))) (take 8 bytes))) "/64"))
            :else text))
        (catch Throwable _ text)))))

(defn- client-address
  "Takes the options and a request. Gives the key of the address that the
  request comes from (`address-key`): the request's own; or, when that is
  one of the opener's `:trusted-proxies`, the last address of its
  `X-Forwarded-For` that is not a trusted proxy, and the request's own when
  there is none."
  [{:keys [trusted-proxies]} request]
  (let [own (str (:remote-addr request))]
    (address-key
     (if-not (contains? trusted-proxies own)
       own
       (let [chain (->> (str/split (str (get-in request [:headers "x-forwarded-for"])) #",")
                        (map str/trim)
                        (remove str/blank?))]
         (or (some (fn [address] (when-not (contains? trusted-proxies address) address))
                   (reverse chain))
             own))))))

(defn- from
  "Takes the options, a face's name and a request. Gives what the face says
  of the request, for the seam's `ration`."
  [options face request]
  {:face face :address (client-address options request)})

;; ------------------------------------------------------------ the answers

(defn- edn
  "Takes a status and a thing. Gives the response that carries the thing as
  one EDN form and a line end."
  [status thing]
  {:status status
   :headers {"Content-Type" "application/edn; charset=utf-8" "Cache-Control" "no-store"}
   :body (str (frame/write thing) "\n")})

(defn- answered
  "Takes a result. Gives its response: the status that
  `softland.wire.words/could-nots` gives one of the runtime's own words,
  and 200 for everything else."
  [result]
  (edn (or (when (map? result) (get-in words/could-nots [(:could-not result) :status])) 200)
       result))

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
              ".warn{color:#f0b070}form{margin:.6rem 0}a{color:inherit}</style></head><body>"
              "<h2>Softland</h2>" body "</body></html>")})

(defn- redirect
  "Takes a local path, and when there is one, the text of a Set-Cookie
  header. Gives the response that sends the browser there."
  [to & [cookie]]
  (cond-> {:status 303 :headers {"Location" to "Cache-Control" "no-store"} :body ""}
    cookie (assoc-in [:headers "Set-Cookie"] cookie)))

(defn- way-on
  "Takes what a request gave as the place to go on to after a sign-in.
  Gives a path of this runtime, and never the text that was given: the yes
  page, written anew from the code that the text names, or `/`. A text
  that is followed as it came can send her browser to another site by a
  character that a browser drops while it reads an address (the review of
  20 November 2026 ran it)."
  [next]
  (let [next (str next)]
    (if (str/starts-with? next "/yes?")
      (let [fields (codec/form-decode (subs next (count "/yes?")) "UTF-8")
            code (credential/of-shown (when (map? fields) (get fields "code")))]
        (if code (str "/yes?code=" code) "/"))
      "/")))

(defn- cookie-text
  "Takes the options of the handler, a credential's text and for how many
  seconds it is kept. Gives the text of the Set-Cookie header. The cookie
  is not read by a page's code (`HttpOnly`), and is sent over TLS alone
  when this runtime's origin is `https`. `SameSite=Lax`: a browser leaves
  it off a post, a fetch and a socket that another site's page makes, and
  sends it when a link on another site is followed to a page here. A
  browser also sends it to every server on the same host, whatever its
  port: on `localhost`, to each program that listens there."
  [{:keys [origin]} credential seconds]
  (str credential/cookie-name "=" credential "; Path=/; HttpOnly; SameSite=Lax; Max-Age=" seconds
       (when (str/starts-with? (str origin) "https://") "; Secure")))

(defn- own-origin?
  "Takes the options and a request. Gives true when the request's `Origin`
  is one of this runtime's own. A post that rests on the cookie must pass
  it."
  [{:keys [origins]} request]
  (contains? origins (get-in request [:headers "origin"])))

(defn- of-cookie
  "Takes the options, a request, and when a page will send an act, the UUID
  of that one act. Gives the caller that the request's cookie stands for
  now, or nil. With the act's UUID, the caller is in the session that the
  seam derives from it (`softland.runtime.seam/caller`, `:session-for`),
  opened when it is not open yet, with `:driven-by {:kind :person}`: a
  page opens a session only when it sends, and a form that is posted twice
  is in one session."
  ([options request] (of-cookie options request nil))
  ([{:keys [the-seam] :as options} request act]
   (when-let [text (credential/of-cookie (get-in request [:headers "cookie"]))]
     (let [caller (m/? (seam/caller the-seam (cond-> {:credential text
                                                      :carrier :cookie
                                                      :from (from options :pages request)}
                                               (uuid? act) (assoc :session-for act
                                                                  :driven-by {:kind :person}))))]
       (when (seam/caller? caller) caller)))))

;; -------------------------------------------------------------------- the pages

(defn- home
  "GET /. The page on which she signs in, or sees that she is signed in. An
  invitation's code that came with the address is carried into the
  sign-in."
  [{:keys [google stand-in origin] :as options} request]
  (let [fields (query-of request)
        next (way-on (get fields "next"))
        invited (get fields "invited")
        carried (str "next=" (codec/url-encode next)
                     (when (credential/secret? invited) (str "&invited=" invited)))]
    (if-let [caller (of-cookie options request)]
      (if (not= "/" next)
        (redirect next)
        (page 200 (str "<p>You are signed in. Your id is <code>" (escaped (seam/who-of caller)) "</code>.</p>"
                       "<form method=\"post\" action=\"/sign-out\"><button>Sign out</button></form>")))
      (page 200
            (str (when (and google (not stand-in))
                   (str "<script src=\"https://accounts.google.com/gsi/client\" async></script>"
                        "<div id=\"g_id_onload\" data-client_id=\"" (escaped (:client-id google)) "\""
                        " data-login_uri=\"" (escaped (str origin "/sign-in/google?" carried)) "\""
                        " data-ux_mode=\"redirect\" data-auto_prompt=\"false\"></div>"
                        "<div class=\"g_id_signin\" data-type=\"standard\"></div>"))
                 (when stand-in
                   (str "<p class=\"warn\">This runtime runs with a stand-in for Google. Anybody who reaches it can "
                        "sign in as any account. It is for a store that is thrown away.</p>"
                        "<form method=\"post\" action=\"/sign-in/stand-in?" (escaped carried) "\">"
                        "<input name=\"sub\" placeholder=\"account number\" value=\"1000001\"> "
                        "<input name=\"email\" placeholder=\"email\" value=\"first@example.com\"> "
                        "<input name=\"name\" placeholder=\"name\" value=\"First Person\"> "
                        "<button>Sign in</button></form>"))
                 (when-not (or google stand-in)
                   "<p class=\"warn\">No way to sign in is set: the runtime was started without a Google client id.</p>"))))))

(defn- signed-in!
  "The end of a sign-in at a browser. Takes the options, the request and
  the text of an ID token. It checks the token against the issuer's keys;
  finds her, or takes the person of the invitation that her account
  proves, and makes her own lane; begins a browser's sign-in; and sends
  the browser on with the credential in a cookie: to the box for her name
  when she has confirmed none, else to where she was going. A token that
  fails the check, an account that proves no invitation, and a step that
  could not be done give a page that says so and no cookie."
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
                         "<p><a href=\"/\">Back</a></p>"))
          (let [fields (query-of request)
                says (into {}
                           (comp (keep (fn [[k v]]
                                         (when (and (string? k) (or (string? v) (number? v) (boolean? v)))
                                           [(carried k words/max-text) (carried v 2048)])))
                                 (take 32))
                           claims)
                person (sign-in/enter! the-seam
                                       {:account [:google (get claims "sub")]
                                        :says says
                                        :email (when (true? (get claims "email_verified"))
                                                 (let [email (get claims "email")]
                                                   (when (words/text? email) email)))
                                        :code-hash (credential/hash-of (get fields "invited"))})]
            (cond
              (= :not-invited (:why person))
              (page 403 (str "<p>This account has no invitation. Softland takes a new person by invitation: "
                             "open the link that you were sent, or ask whoever invited you for one.</p>"
                             "<p><a href=\"/\">Back</a></p>"))

              (:could-not person)
              (page 503 "<p>The sign-in could not be written. Try again.</p>")

              :else
              (let [begun (sign-in/begin-browser! the-seam (:person person))]
                (if (:could-not begun)
                  (page 503 "<p>The sign-in could not be begun. Try again.</p>")
                  (let [caller (m/? (seam/caller the-seam {:credential (:credential begun) :carrier :cookie
                                                           :from (from options :pages request)}))
                        named? (and (seam/caller? caller) (some? (sign-in/name-of the-seam caller)))
                        next (way-on (get fields "next"))]
                    (redirect (if named?
                                next
                                (str "/sign-in/name?next=" (codec/url-encode next)
                                     (when-let [offer (get says "name")]
                                       (str "&offer=" (codec/url-encode offer)))))
                              (cookie-text options (:credential begun)
                                           (quot (- (:until begun) (System/currentTimeMillis)) 1000)))))))))))))

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
                                    (seq (get form "email")) (assoc "email" (get form "email") "email_verified" true)
                                    (seq (get form "name")) (assoc "name" (get form "name")))
                                  (System/currentTimeMillis)))
      (page 400 "<p>The sign-in was not taken.</p>"))))

(defn- name-page
  "GET /sign-in/name. The box: her provider's name for her, which she
  confirms or changes. The name comes in the address, from the sign-in
  that sent her here: it is her own, in her own browser, and Auth is not
  read for it. The page opens no session: it carries the UUID of the one
  act that the box sends, and the post opens the session that the seam
  derives from it, so that a form that is posted twice is one act in one
  session."
  [{:keys [the-seam] :as options} request]
  (if-let [caller (of-cookie options request)]
    (let [fields (query-of request)
          next (way-on (get fields "next"))
          offer (str (get fields "offer"))
          offered (or (sign-in/name-of the-seam caller)
                      (carried offer words/max-text))]
      (page 200 (str "<p>What should you be called here? You can change it.</p>"
                     "<form method=\"post\" action=\"/sign-in/name?next=" (escaped (codec/url-encode next)) "\">"
                     "<input type=\"hidden\" name=\"act\" value=\"" (UUID/randomUUID) "\">"
                     "<input name=\"name\" maxlength=\"" words/max-text "\" value=\"" (escaped offered) "\" autofocus> "
                     "<button>This is my name</button></form>")))
    (redirect "/")))

(defn- name!
  "POST /sign-in/name. Writes the name that she confirms, as her own act in
  her own lane, in the session derived from the act's UUID, closes that
  session, and sends her on. The post must come from this runtime's own
  page and rest on a cookie that stands for her."
  [{:keys [the-seam] :as options} request]
  (let [form (form-of request)
        act (uuid-of (get form "act"))
        name (str/trim (str (get form "name")))
        next (way-on (get (query-of request) "next"))
        ;; The session is opened only for a post that will send its act.
        caller (when (and form act (own-origin? options request) (words/text? name))
                 (of-cookie options request act))]
    (cond
      (not (and form act (own-origin? options request)))
      (page 400 "<p>That was not taken.</p>")

      (not (words/text? name))
      (redirect (str "/sign-in/name?next=" (codec/url-encode next)))

      (not (and caller (seam/person? caller)))
      (page 400 "<p>That was not taken.</p>")

      :else
      (let [sent (sign-in/name! the-seam caller name act)]
        (if (= :accepted (get-in sent [:answer :answer]))
          (do (m/? (seam/call the-seam caller {:word :close-session}))
              (redirect next))
          (page 503 "<p>The name could not be written. Try again.</p>"))))))

(defn- yes-page
  "GET /yes. The page on which she answers a program: the code, which the
  program shows too; what the program says of itself; and the choice, as a
  new agent with a name, which is chosen when the page opens, or as
  herself. Somebody who is not signed in is sent to sign in first and
  comes back."
  [{:keys [the-seam] :as options} request]
  (let [code (credential/of-shown (get (query-of request) "code"))
        shown (when code (sign-in/ask-shown the-seam code))]
    (cond
      (nil? shown)
      (page 404 "<p>This link names no program that waits.</p>")

      (nil? (of-cookie options request))
      (redirect (str "/?next=" (codec/url-encode (str "/yes?code=" code))))

      (or (not= :waiting (:state shown))
          (<= (long (:ends shown)) (seam/now-ms)))
      (page 409 "<p>This program was answered before or waited too long. Start its sign-in again if you want another answer.</p>")

      :else
      (page 200
            (str "<p>A program asks to be signed in.</p>"
                 "<p class=\"warn\">Say yes only if you started that program yourself, just now, and it shows the code "
                 "<code>" (credential/shown code) "</code>. If a link brought you here and you started no program, "
                 "say no: whoever made the link would act through you.</p>"
                 "<p>What the program says of itself, which anybody could have written: <code>"
                 (escaped (:asks shown)) "</code></p>"
                 "<form method=\"post\" action=\"/yes\">"
                 "<input type=\"hidden\" name=\"code\" value=\"" code "\">"
                 ;; An agent of its own is chosen at the start: each program has a
                 ;; who of its own by default (the runtime's page, entry 8). The
                 ;; build's default, for Sid to turn.
                 "<p><label><input type=\"radio\" name=\"as\" value=\"agent\" checked> It is an agent of its own, called "
                 "<input name=\"label\" maxlength=\"" words/max-text "\" placeholder=\"a name\"></label></p>"
                 "<p><label><input type=\"radio\" name=\"as\" value=\"me\"> It acts as me</label></p>"
                 "<button name=\"say\" value=\"yes\">Yes</button> "
                 "<button name=\"say\" value=\"no\">No</button></form>")))))

(defn- yes!
  "POST /yes. Her answer to a program's ask. The post must come from this
  runtime's own page and rest on a cookie that stands for a person: a
  browser's sign-in, which no program holds."
  [{:keys [the-seam] :as options} request]
  (let [form (form-of request)
        code (credential/of-shown (get form "code"))
        caller (when (and form (own-origin? options request)) (of-cookie options request))
        label (str/trim (str (get form "label")))
        agent? (= "agent" (get form "as"))]
    (cond
      (not (and caller code (seam/person? caller)))
      (page 400 "<p>That was not taken.</p>")

      (and (= "yes" (get form "say")) agent? (not (words/text? label)))
      (page 400 "<p>An agent needs a name. Go back and give it one.</p>")

      :else
      (let [yes? (= "yes" (get form "say"))
            got (sign-in/answer! the-seam caller code {:yes? yes? :label (when (and yes? agent?) label)})
            standing (:yes got)
            mine? (and standing (= (seam/root-of caller) (:root standing)))
            as-me? (and mine? (= (seam/root-of caller) (:actor standing)))
            actor-matches? (and mine? yes?
                                (if agent?
                                  (and (not as-me?)
                                       (let [listed (m/? (seam/call the-seam caller {:word :under}))
                                             actors (:actors listed)]
                                         (if (sequential? actors)
                                           (= label (some (fn [a] (when (= (:actor standing) (:actor a)) (:label a)))
                                                          actors))
                                           true)))
                                  as-me?))]
        (cond
          actor-matches?
          (page 200 "<p>Done. The program is signed in; go back to it.</p>")

          mine?
          (page 409
                (if as-me?
                  (str "<p>This program was already answered before: the program is signed in as you (sign-in <code>"
                       (escaped (:sign-in standing)) "</code>). End that sign-in if you did not mean to allow it.</p>")
                  (str "<p>This program was already answered before: the program is signed in as an agent of yours (sign-in <code>"
                       (escaped (:sign-in standing)) "</code>). End that sign-in if you did not mean to allow it.</p>")))

          standing
          (page 409 "<p>This program was already answered before.</p>")

          (and (:no got) (not yes?))
          (page 200 "<p>Refused. The program is not signed in.</p>")

          (:no got)
          (page 409 "<p>This program was already refused before.</p>")

          (= :over (:why got)) (page 409 "<p>This program waited too long. Start its sign-in again.</p>")
          (= :take-that (:could-not got)) (page 409 "<p>That was not taken. Start the program's sign-in again.</p>")
          :else (page 503 "<p>That could not be written. Try again.</p>"))))))

(defn- sign-out!
  "POST /sign-out. Ends the cookie's sign-in, for every face and every
  copy of the runtime, and takes the cookie away. Only a cookie that
  stands for somebody now ends anything."
  [{:keys [the-seam] :as options} request]
  (if-not (own-origin? options request)
    (page 400 "<p>That was not taken.</p>")
    (do (when-let [caller (of-cookie options request)]
          (m/? (seam/call the-seam caller {:word :sign-out})))
        (redirect "/" (cookie-text options "" 0)))))

;; ------------------------------------------------------ a program's sign-in

(defn- edn-of
  "Reads the body of a program's request as one EDN form. Gives [:ok form],
  or [:no the response that says why]."
  [request]
  (let [[outcome text] (body-text request max-form-bytes)]
    (if (= :no outcome)
      [:no (assoc (answered (words/could-not :read-the-frame {:why :too-large :most max-form-bytes})) :status 413)]
      (let [[outcome form] (frame/read-one text)]
        (if (= :no outcome)
          [:no (answered (words/could-not :read-the-frame {:why form}))]
          [:ok form])))))

(defn- program-asks!
  "POST /sign-in/program. A program posts {:hash :asks}: the hash of the
  secret that it made, and what it says of itself. It gets {:code :shown
  :link :ends}."
  [{:keys [the-seam origin]} request]
  (let [[outcome form] (edn-of request)]
    (cond
      (= :no outcome) form

      (not (and (map? form) (= #{:hash :asks} (set (keys form)))
                (credential/hash? (:hash form)) (words/text? (:asks form))))
      (answered (words/could-not :read-the-word {:why :part-has-no-form :at nil}))

      :else
      (let [got (sign-in/ask! the-seam form)]
        (answered (if (:could-not got)
                    got
                    {:code (:code got)
                     :shown (credential/shown (:code got))
                     :link (str origin "/yes?code=" (:code got))
                     :ends (:ends got)}))))))

(defn- program-waits!
  "POST /sign-in/program/poll. A program posts {:code :secret} and is told
  the state of its ask (`softland.runtime.sign-in/asked`). The polls of one
  code are paced before Auth is read (`softland.runtime.seam/ration-poll!`);
  a poll that proves its ask is given back to the address that it came
  from (`softland.runtime.seam/give-back-sign-in!`): a program's waiting
  does not use up the posts of the address."
  [{:keys [the-seam] :as options} request]
  (let [[outcome form] (edn-of request)]
    (cond
      (= :no outcome) form

      (not (and (map? form) (= #{:code :secret} (set (keys form)))
                (credential/code? (:code form)) (credential/secret? (:secret form))))
      (answered (words/could-not :read-the-word {:why :part-has-no-form :at nil}))

      :else
      (or (some-> (seam/ration-poll! the-seam (:code form)) answered)
          (let [got (sign-in/asked the-seam (:code form) (:secret form))]
            (when (:state got)
              (seam/give-back-sign-in! the-seam (client-address options request)))
            (answered got))))))

;; -------------------------------------------------------------------- the line

(defn accept
  "Who a connection to the line is. Takes the options of `handler` and the
  Ring request map of the upgrade. Gives {:caller :max-frame-bytes}, or a
  Ring response when the connection is not taken.

    an `Authorization` header   a program's credential: her caller, or the
                                response of the seam's word
    else the cookie             a browser's credential, and the request's
                                `Origin` must be this runtime's own: 403
                                otherwise
    neither                     the visitor

  The session. A system that was in a session names it, in the header
  `softland-session` or the query field `session`, and stays in it; one
  that names none is given a new one. A visitor is in none. The bound on a
  frame's bytes is the small one for a visitor."
  [{:keys [the-seam] :as options} request]
  (let [header (get-in request [:headers "authorization"])
        cookie-header (get-in request [:headers "cookie"])
        has-sign-in-cookie? (some? (cookie-of request credential/cookie-name))
        cookie (credential/of-cookie cookie-header)
        named (or (get-in request [:headers "softland-session"]) (get (query-of request) "session"))
        session (uuid-of named)
        about (from options :line request)
        taken (fn [credential carrier]
                (let [caller (m/? (seam/caller the-seam
                                               (cond-> {:credential credential :carrier carrier
                                                        :session session :new-session? (nil? session)
                                                        :from about}
                                                 (and (= :cookie carrier) (nil? session))
                                                 (assoc :driven-by {:kind :person}))))]
                  (if (seam/caller? caller)
                    {:caller caller :max-frame-bytes words/max-frame-bytes}
                    (answered caller))))]
    (cond
      (and named (nil? session))
      (answered (words/could-not :find-her-session {:why :not-known}))

      header
      (if-let [credential (credential/of-header header)]
        (taken credential :header)
        (answered (words/could-not :find-her-sign-in {:why :not-a-credential})))

      has-sign-in-cookie?
      (if-not cookie
        (answered (words/could-not :find-her-sign-in {:why :not-a-credential}))
        (if (own-origin? options request)
          (taken cookie :cookie)
          {:status 403 :headers {"Content-Type" "text/plain; charset=utf-8"} :body "Not from this origin\n"}))

      :else {:caller (seam/visitor about) :max-frame-bytes words/max-visitor-frame-bytes})))

;; --------------------------------------------------------------- the handler

(defn options-of
  "Makes the options that `handler` and `accept` share. Takes a map:

    :the-seam  an opened seam (`softland.runtime.seam/open`)
    :origin    this runtime's own origin as a browser names it, such as
               `http://localhost:8500`
    :origins   the origins whose posts are taken; left out, the one origin
    :google    {:client-id :issuers :keys-fn}, where `:keys-fn` is a
               function of no argument that gives [:ok {kid public-key}]
               or [:threw info]; nil when there is no way to sign in
    :stand-in  a stand-in for Google
               (`softland.runtime.google-stand-in/make`), or nil. With one,
               `:google` is made from it and anybody can sign in as any
               account that is invited
    :trusted-proxies
               the addresses of the proxies in front of this runtime whose
               `X-Forwarded-For` is believed (`client-address`); left out,
               none"
  [{:keys [origin origins stand-in google trusted-proxies] :as options}]
  (assoc options
         :trusted-proxies (set trusted-proxies)
         :origins (or origins #{origin})
         :google (if stand-in
                   {:client-id (:client-id stand-in)
                    :issuers #{(:issuer stand-in)}
                    :keys-fn (fn [] [:ok (:keys stand-in)])}
                   google)))

(defn handler
  "Makes the Ring handler of the pages and of a program's sign-in. Takes
  the options (`options-of`). Gives a function from a request to a
  response. A path that is none of the routes gives 404. The handler throws
  for nothing that a request holds."
  [{:keys [stand-in] :as options}]
  (fn [request]
    (let [method (:request-method request)
          uri (:uri request)]
      (try
        (cond
          (and (= :get method) (= "/" uri)) (home options request)
          (and (= :get method) (= "/health" uri)) (edn 200 {:ready true})
          (and (= :post method) (str/starts-with? uri "/sign-in/"))
          (if-let [paced (seam/ration-sign-in! (:the-seam options) (client-address options request))]
            (if (or (= "/sign-in/program" uri) (= "/sign-in/program/poll" uri))
              (answered paced)
              (page 429 "<p>Too many sign-in requests from this address. Wait a moment and try again.</p>"))
            (cond
              (and (= "/sign-in/google" uri) (:google options) (not stand-in)) (from-google! options request)
              (and (= "/sign-in/stand-in" uri) stand-in) (from-stand-in! options request)
              (= "/sign-in/name" uri) (name! options request)
              (= "/sign-in/program" uri) (program-asks! options request)
              (= "/sign-in/program/poll" uri) (program-waits! options request)
              :else {:status 404 :headers {"Content-Type" "text/plain; charset=utf-8"} :body "Not found\n"}))
          (and (= :get method) (= "/sign-in/name" uri)) (name-page options request)
          (and (= :get method) (= "/yes" uri)) (yes-page options request)
          (and (= :post method) (= "/yes" uri)) (yes! options request)
          (and (= :post method) (= "/sign-out" uri)) (sign-out! options request)
          :else {:status 404 :headers {"Content-Type" "text/plain; charset=utf-8"} :body "Not found\n"})
        (catch Throwable t
          {:status 500
           :headers {"Content-Type" "text/plain; charset=utf-8"}
           :body (str "The runtime failed: " (.getName (class t)) "\n")})))))
