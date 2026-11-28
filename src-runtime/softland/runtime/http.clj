(ns softland.runtime.http
  "The face over plain HTTP: the pages by which a person signs in, confirms
  her name and answers a program; the two requests by which a program asks
  to be signed in and waits; and who a connection to the line is
  (`accept`). The words are not here: everything that speaks them speaks
  them over the line (`softland.runtime.line`).

  Two carriers of a credential, kept apart. A browser's sign-in is taken
  from the cookie and never from a header; a program's from the
  `Authorization` header and never from a cookie; and Auth holds which of
  the two a sign-in is. Each post that rests on the cookie, and each
  connection to the line that does, must carry an `Origin` that is this
  runtime's own. So no page of another site can make a signed-in browser
  say a word, a yes or a sign-out; and a program that holds its own secret
  cannot say her yes.

  A program's sign-in. The program makes two secrets and keeps both: one
  that becomes its credential, and one only for asking. It posts the hash
  of each with what it says of itself. It gets a code, and shows the code
  and a link. She opens the link where she is signed in, sees the code
  again, and says yes, as a new agent with a name or as an agent of hers
  signed in again, or no. A yes never names her: only she, at a browser,
  is she. She ticks what the agent is lent in her own lane, to see and to
  write: both for a new agent, neither for one of hers, which keeps what it
  holds. After a yes the page says what the agent is lent by it, read as
  it stands. The
  program asks for the ask's state with its code and its secret for asking
  until she has answered, and puts its credential together from what it is
  told and the other secret, which it has sent nowhere until then. An ask
  that is answered after its end is refused.

  A first sign-in. An account that proves no invitation is turned away. A
  person who has confirmed no name is sent to the box: her provider's name
  for her, which she confirms or changes.

  The pace. Each post to `/sign-in/*` is paced by the address that it comes
  from. An IPv6 address counts by its first 64 bits, the part that one
  network is given, so that one network cannot take a new address for each
  post. A poll that proves a program's own ask is given back to the
  address, so that programs that wait behind one address do not use it up
  for a person's sign-in; the polls of one ask are paced on their own.

  Not here: the browser's client, which is another build (the runtime's
  page, piece 7), and MCP (piece 6)."
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
  "The referrer policy of every page. A post that rests on the cookie must
  carry this runtime's `Origin`, and under `no-referrer` a browser writes
  `Origin: null` on a form's post to its own site (seen with Chrome on 20
  November 2026). And the address of a page must not reach another site:
  under this policy a request to another site carries the origin alone."
  "strict-origin-when-cross-origin")

(def ^:private max-form-bytes
  "The most bytes of a page's form, or of the EDN of a program's request:
  an ID token and a few short fields."
  65536)

;; ----------------------------------------------------------- reading a request

(defn- body-text
  "The bound is tested before the body is read and again while it is read."
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
  "Nil for a body that is too large."
  [request]
  (let [[outcome text] (body-text request max-form-bytes)]
    (when (= :ok outcome)
      (let [form (codec/form-decode text "UTF-8")]
        (if (map? form) form {})))))

(defn- query-of
  [request]
  (let [form (some-> (:query-string request) (codec/form-decode "UTF-8"))]
    (if (map? form) form {})))

(defn- cookie-of
  [request cookie]
  (some (fn [part]
          (let [[k v] (str/split (str/trim part) #"=" 2)]
            (when (= cookie k) v)))
        (str/split (or (get-in request [:headers "cookie"]) "") #";")))

(defn- carried
  "Cuts a text to at most `most` characters, never between the two halves
  of a surrogate pair, and writes a surrogate that has no other half as
  U+FFFD: Auth does not take a text that UTF-8 cannot carry, and a
  provider's claim, read from JSON, can hold one."
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
  [text]
  (when (and (string? text) (re-matches #"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" text))
    (UUID/fromString text)))

(defn- address-key
  "The key by which an address's posts are paced: an IPv4 address mapped
  into IPv6 as the IPv4 one, and an IPv6 address by its first 64 bits,
  written `<hex>/64`. It looks up no name: a text without `:` is never
  parsed."
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
  "The request's own address; or, when that is one of the opener's
  `:trusted-proxies`, the last address of its `X-Forwarded-For` that is not
  a trusted proxy."
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
  [options face request]
  {:face face :address (client-address options request)})

;; ------------------------------------------------------------ the answers

(defn- edn
  [status thing]
  {:status status
   :headers {"Content-Type" "application/edn; charset=utf-8" "Cache-Control" "no-store"}
   :body (str (frame/write thing) "\n")})

(defn- answered
  [result]
  (edn (or (when (map? result) (get-in words/could-nots [(:could-not result) :status])) 200)
       result))

(defn- escaped
  [text]
  (-> (str text)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")
      (str/replace "'" "&#39;")))

(defn- page
  "A page is never framed by another site and never kept by a browser."
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
  [to & [cookie]]
  (cond-> {:status 303 :headers {"Location" to "Cache-Control" "no-store"} :body ""}
    cookie (assoc-in [:headers "Set-Cookie"] cookie)))

(defn- way-on
  "Gives a path of this runtime, and never the text that was given: the yes
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
  "`seconds` is for how long the cookie is kept. `SameSite=Lax`: a browser
  leaves the cookie off a post, a fetch and a socket that another site's
  page makes, and sends it when a link on another site is followed to a
  page here. A browser also sends it to every server on the same host,
  whatever its port: on `localhost`, to each program that listens there."
  [{:keys [origin]} credential seconds]
  (str credential/cookie-name "=" credential "; Path=/; HttpOnly; SameSite=Lax; Max-Age=" seconds
       (when (str/starts-with? (str origin) "https://") "; Secure")))

(defn- own-origin?
  "A post that rests on the cookie must pass it."
  [{:keys [origins]} request]
  (contains? origins (get-in request [:headers "origin"])))

(defn- cookie-caller
  "As `of-cookie`, with the runtime's word in place of nil when the cookie
  stands for nobody or the session could not be opened; nil when the
  request carries no cookie."
  [{:keys [the-seam] :as options} request act]
  (when-let [text (credential/of-cookie (get-in request [:headers "cookie"]))]
    (m/? (seam/caller the-seam (cond-> {:credential text
                                        :carrier :cookie
                                        :from (from options :pages request)}
                                 (uuid? act) (assoc :session-for act
                                                    :driven-by {:kind :person}))))))

(defn- of-cookie
  "The caller that the request's cookie stands for, or nil. `act` is the
  UUID of the one act that a page will send: the caller is then in the
  session that the seam derives from it, opened when it is not open yet. A
  page opens a session only when it sends, and a form that is posted twice
  is in one session."
  ([options request] (of-cookie options request nil))
  ([options request act]
   (let [caller (cookie-caller options request act)]
     (when (seam/caller? caller) caller))))

;; -------------------------------------------------------------------- the pages

(defn- home
  "GET /. An invitation's code that came with the address is carried into
  the sign-in."
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
  "The end of a sign-in at a browser. A token that fails the check, an
  account that proves no invitation, and a step that could not be done give
  a page that says so and no cookie."
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
              (page 403 (str "<p>No invitation lets this account in: it has none, or its invitation is over or was "
                             "taken back. Softland takes a new person by invitation: open the link that you were "
                             "sent, or ask whoever invited you for a new one.</p>"
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
  "POST /sign-in/stand-in. Goes on as Google's post does, through the same
  check."
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
  "GET /sign-in/name. The offered name comes in the address, from the
  sign-in that sent her here: it is her own, in her own browser, and Auth
  is not read for it. The page opens no session: it carries the UUID of the
  one act that the box sends, and the post opens the session that the seam
  derives from it."
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

(defn- live-agents
  "Her agents that have not ended, as `:under` gives them, in the order in
  which they were made; nil when the read failed."
  [the-seam caller]
  (let [actors (:actors (m/? (seam/call the-seam caller {:word :under})))]
    (when (sequential? actors)
      (filterv (fn [a] (not (:ended? a))) actors))))

(defn- lend-boxes
  "The two boxes of what a program is lent in her own lane, ticked or not."
  [ticked?]
  (let [checked (if ticked? " checked" "")]
    (str "<label><input type=\"checkbox\" name=\"see\" value=\"yes\"" checked "> to see</label> "
         "<label><input type=\"checkbox\" name=\"write\" value=\"yes\"" checked "> to write</label>")))

(defn- yes-page
  [{:keys [the-seam] :as options} request]
  (let [code (credential/of-shown (get (query-of request) "code"))
        shown (when code (sign-in/ask-shown the-seam code))
        caller (when shown (of-cookie options request))]
    (cond
      (nil? shown)
      (page 404 "<p>This link names no program that waits.</p>")

      (nil? caller)
      (redirect (str "/?next=" (codec/url-encode (str "/yes?code=" code))))

      (or (not= :waiting (:state shown))
          (<= (long (:ends shown)) (seam/now-ms)))
      (page 409 "<p>This program was answered before or waited too long. Start its sign-in again if you want another answer.</p>")

      :else
      (let [form-start (str "<form method=\"post\" action=\"/yes\">"
                            "<input type=\"hidden\" name=\"code\" value=\"" code "\">")
            agents (live-agents the-seam caller)]
        (page 200
              (str "<p>A program asks to be signed in.</p>"
                   "<p class=\"warn\">Say yes only if you started that program yourself, just now, and it shows the code "
                   "<code>" (credential/shown code) "</code>. If a link brought you here and you started no program, "
                   "say no: whoever made the link would act through you.</p>"
                   "<p>What the program says of itself, which anybody could have written: <code>"
                   (escaped (:asks shown)) "</code></p>"
                   ;; A new agent comes first: each program has a who of its own by
                   ;; default (the runtime's page, entry 8).
                   form-start
                   "<input type=\"hidden\" name=\"as\" value=\"new-agent\">"
                   "<p>As a new agent, called "
                   "<input name=\"label\" maxlength=\"" words/max-text "\" placeholder=\"a name\"></p>"
                   "<p>It is lent, in your own lane: " (lend-boxes true) "</p>"
                   "<button name=\"say\" value=\"yes\">Yes, as a new agent</button></form>"
                   (if (seq agents)
                     (str form-start
                          "<input type=\"hidden\" name=\"as\" value=\"my-agent\">"
                          "<p>As this agent of yours, signed in again:</p>"
                          (apply str (map (fn [a]
                                            (str "<p><label><input type=\"radio\" name=\"agent\" value=\"" (:actor a) "\"> "
                                                 (escaped (:label a)) " <code>" (subs (str (:actor a)) 0 8) "</code></label></p>"))
                                          agents))
                          "<p>It keeps what it holds. It is lent besides, in your own lane: " (lend-boxes false) "</p>"
                          "<button name=\"say\" value=\"yes\">Yes, as this agent</button></form>")
                     "<p>You have no agent yet that a program could sign in as again.</p>")
                   form-start
                   "<button name=\"say\" value=\"no\">No</button></form>"))))))

(defn- lent
  "What the two boxes of a post ask to lend: an `:allows` of the store's, or
  nil when neither is ticked."
  [form]
  (let [see? (= "yes" (get form "see"))
        write? (= "yes" (get form "write"))]
    (when (or see? write?)
      (cond-> {}
        see? (assoc :see true)
        write? (assoc :write :all)))))

(defn- lent-words
  [allows]
  (case [(boolean (:see allows)) (boolean (:write allows))]
    [true true] "to see and to write"
    [true false] "to see"
    [false true] "to write"))

(defn- yes!
  "POST /yes. The post must come from this runtime's own page and rest on a
  cookie that stands for a person: a browser's sign-in, which no program
  holds.

  Auth first, then the lend. A yes is written in Auth; when the program is
  signed in as the agent that she chose and she ticked a box, her act that
  lends it what she ticked is sent after. Then the page reads the lend as
  it stands and says what the agent is lent by it, whatever the send
  answered: 200 when that is what she ticked; else 503 when sending again
  may lend it, and 409 when it cannot, since the same post again sends the
  same act."
  [{:keys [the-seam] :as options} request]
  (let [form (form-of request)
        code (credential/of-shown (get form "code"))
        caller (when (and form (own-origin? options request)) (of-cookie options request))
        as (get form "as")
        yes? (= "yes" (get form "say"))
        label (str/trim (str (get form "label")))
        agent (uuid-of (get form "agent"))
        allows (lent form)]
    (cond
      (not (and caller code (seam/person? caller)))
      (page 400 "<p>That was not taken.</p>")

      ;; No yes signs a program in as herself.
      (and yes? (not (contains? #{"new-agent" "my-agent"} as)))
      (page 400 "<p>That was not taken: a program is signed in as an agent of yours, new or one that you have.</p>")

      (and yes? (= "new-agent" as) (not (words/text? label)))
      (page 400 "<p>An agent needs a name. Go back and give it one.</p>")

      (and yes? (= "my-agent" as) (nil? agent))
      (page 400 "<p>Choose one of your agents. Go back and choose one.</p>")

      :else
      (let [new? (and yes? (= "new-agent" as))
            mine-agent? (and yes? (= "my-agent" as))
            got (sign-in/answer! the-seam caller code {:yes? yes?
                                                       :label (when new? label)
                                                       :agent (when mine-agent? agent)})
            standing (:yes got)
            mine? (and standing (= (seam/root-of caller) (:root standing)))
            listed (when mine? (live-agents the-seam caller))
            named (or (some (fn [a] (when (= (:actor standing) (:actor a)) (:label a))) listed)
                      (when new? label))
            actor-matches? (and mine? yes?
                                (if new?
                                  (or (nil? listed) (= label named))
                                  (= agent (:actor standing))))]
        (cond
          (and actor-matches? allows)
          (let [ids (credential/lent-at-yes (:root standing) (:sign-in standing))
                ;; The session is left open: the same post again must find it,
                ;; to send the same act. It ends when its time passes.
                in-session (cookie-caller options request (:act ids))
                sent (if (seam/caller? in-session)
                       (sign-in/lend! the-seam in-session standing allows)
                       in-session)
                ;; What stands, whatever the send answered: a post again of
                ;; a page that was lent before is answered from the first
                ;; act, and one from her other browser is in a session that
                ;; is not its own.
                stands (sign-in/lent-now the-seam caller standing)
                holds (cond (:could-not stands) "what it is lent could not be read"
                            (:allows stands) (str "it is lent " (lent-words (:allows stands)) " in your own lane")
                            :else "this page has lent it nothing")
                ;; A send that was answered cannot lend it when it is sent
                ;; again: the same act is answered as it was first, after an
                ;; end too, another content under its UUID is not this act,
                ;; and a session that is not this sign-in's, or is closed,
                ;; stays so.
                for-good? (or (contains? #{:accepted :refused :not-mine} (get-in sent [:answer :answer]))
                              (= :find-her-session (:could-not sent)))]
            (cond
              (= allows (:allows stands))
              (page 200 (str "<p>Done. The program is signed in as " (escaped named) ", and it is lent "
                             (lent-words allows) " in your own lane; go back to it.</p>"))

              for-good?
              (page 409 (str "<p>The program is signed in as " (escaped named) ", but what you ticked was not lent: "
                             holds ". To change it, start the program's sign-in again as this agent.</p>"))

              :else
              (page 503 (str "<p>The program is signed in as " (escaped named) ", but what you ticked could not be lent: "
                             holds ". Send this page again; if it is still not lent, start the program's sign-in "
                             "again as this agent and tick again.</p>"))))

          actor-matches?
          (page 200 (if new?
                      (str "<p>Done. The program is signed in as " (escaped named)
                           ", and it holds nothing; go back to it.</p>")
                      (str "<p>Done. The program is signed in as " (escaped named)
                           ", which keeps what it holds; go back to it.</p>")))

          mine?
          (page 409
                (str "<p>This program was already answered before: the program is signed in as an agent of yours, "
                     (escaped named) " (sign-in <code>" (escaped (:sign-in standing))
                     "</code>). End that sign-in if you did not mean to allow it.</p>"))

          standing
          (page 409 "<p>This program was already answered before.</p>")

          (and (:no got) (not yes?))
          (page 200 "<p>Refused. The program is not signed in.</p>")

          (:no got)
          (page 409 "<p>This program was already refused before.</p>")

          (= :over (:why got)) (page 409 "<p>This program waited too long. Start its sign-in again.</p>")
          (= :is-a-person (:why got))
          (page 409 "<p>That was not taken: a program is never signed in as you. Choose an agent of yours, new or one that you have.</p>")
          (= :take-that (:could-not got)) (page 409 "<p>That was not taken. Start the program's sign-in again.</p>")
          :else (page 503 "<p>That could not be written. Try again.</p>"))))))

(defn- sign-out!
  "POST /sign-out. Ends the cookie's sign-in for every face and every copy
  of the runtime."
  [{:keys [the-seam] :as options} request]
  (if-not (own-origin? options request)
    (page 400 "<p>That was not taken.</p>")
    (do (when-let [caller (of-cookie options request)]
          (m/? (seam/call the-seam caller {:word :sign-out})))
        (redirect "/" (cookie-text options "" 0)))))

;; ------------------------------------------------------ a program's sign-in

(defn- edn-of
  [request]
  (let [[outcome text] (body-text request max-form-bytes)]
    (if (= :no outcome)
      [:no (assoc (answered (words/could-not :read-the-frame {:why :too-large :most max-form-bytes})) :status 413)]
      (let [[outcome form] (frame/read-one text)]
        (if (= :no outcome)
          [:no (answered (words/could-not :read-the-frame {:why form}))]
          [:ok form])))))

(defn- program-asks!
  [{:keys [the-seam origin]} request]
  (let [[outcome form] (edn-of request)]
    (cond
      (= :no outcome) form

      ;; Two hashes that are one would make the credential's secret the
      ;; secret for asking: a poll would then carry it before her yes.
      (not (and (map? form) (= #{:hash :asking-hash :asks} (set (keys form)))
                (credential/hash? (:hash form)) (credential/hash? (:asking-hash form))
                (not= (:hash form) (:asking-hash form))
                (words/text? (:asks form))))
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
  "POST /sign-in/program/poll. The polls of one code are paced before Auth
  is read. A poll that proves its ask is given back to the address that it
  came from."
  [{:keys [the-seam] :as options} request]
  (let [[outcome form] (edn-of request)]
    (cond
      (= :no outcome) form

      (not (and (map? form) (= #{:code :asking-secret} (set (keys form)))
                (credential/code? (:code form)) (credential/secret? (:asking-secret form))))
      (answered (words/could-not :read-the-word {:why :part-has-no-form :at nil}))

      :else
      (or (some-> (seam/ration-poll! the-seam (:code form)) answered)
          (let [got (sign-in/asked the-seam (:code form) (:asking-secret form))]
            (when (:state got)
              (seam/give-back-sign-in! the-seam (client-address options request)))
            (answered got))))))

;; -------------------------------------------------------------------- the line

(defn accept
  "Who a connection to the line is. `request` is the Ring request map of the
  upgrade. Gives {:caller :max-frame-bytes}, or a Ring response when the
  connection is not taken. A connection that rests on the cookie must carry
  this runtime's own `Origin`.

  A system that was in a session names it, in the header `softland-session`
  or the query field `session`, and stays in it; one that names none is
  given a new one. A visitor is in none."
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
  "The options that `handler` and `accept` share:

    :the-seam  an opened seam
    :origin    this runtime's own origin as a browser names it, such as
               `http://localhost:8500`
    :origins   the origins whose posts are taken; left out, the one origin
    :google    {:client-id :issuers :keys-fn}, where `:keys-fn` takes no
               argument and gives [:ok {kid public-key}] or [:threw info];
               nil when there is no way to sign in
    :stand-in  a stand-in for Google, or nil. With one, `:google` is made
               from it and anybody can sign in as any account that is
               invited
    :trusted-proxies
               the addresses of the proxies in front of this runtime whose
               `X-Forwarded-For` is believed; left out, none"
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
  "The handler throws for nothing that a request holds."
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
