(ns softland.runtime.http
  "The runtime's way in over HTTP, on localhost only: the routes that a
  browser, an agent, a program and Sid's command line call. Not pure: it
  opens a port, and each request calls the runtime's service, which calls
  the cluster.

  The server is the JDK's own `com.sun.net.httpserver.HttpServer`, bound to
  127.0.0.1 and nothing else (BUILD.md, default 19: a store that is thrown
  away, on localhost only). It answers only a request whose `Host` is
  `localhost:<port>`, so that a page of another site whose name was pointed
  at 127.0.0.1 is not answered (a session's choice, builder F). A request
  to `127.0.0.1:<port>` is sent on with a `308` to the same path at
  `localhost:<port>`: the page's origin is localhost's alone, since
  Google's client names `http://localhost:8400` and a browser's POST must
  carry that origin. Every answer carries `Referrer-Policy:
  no-referrer-when-downgrade`, which Google asks of a page served over http
  on localhost (BUILD.md, \"For the builder\"); `Cache-Control: no-store`,
  since an answer can hold a token; and `X-Frame-Options: DENY` with
  `Content-Security-Policy: frame-ancestors 'none'`, so that no page frames
  this one and takes a click on its buttons (\"Approve\" among them). A
  page on another port of localhost is the same site, so the Lax cookie
  would go into its frame.

  Who is asking. A person's sitting is an HttpOnly cookie,
  `softland-sitting`, with `SameSite=Lax` and `Path=/` and no `Secure` flag,
  since the page is http on localhost; its value is the sitting's token
  (`softland.runtime.token`). That a browser holds an HttpOnly cookie is a
  finding of Inland's (BUILD.md, \"Read first\"); the cookie's name and
  flags are a session's (builder F). Agents and programs send
  `Authorization: Bearer <token>`. A request with a bearer header is that
  token's, and its cookie is not read. An agent that acts in a run names
  the run in the header `Softland-Run: <run id>`, a session's word: the
  binding refuses an agent's act without a run
  (`softland.runtime.binding`, `:agent-act-without-a-run`), and the run is
  part of the actor, not of a route's body. The runtime's service says
  which actor a token is, with its run (`softland.runtime.service/actor-of`),
  and its refusal is answered as it gives it (`:token-not-valid`,
  `:session-ended`, `:run-not-the-sessions`); a token says who, and carries
  no right (BUILD.md, default 5).

  A limit of the cookie: a browser keeps cookies by host, not by port. Any
  other server on localhost, on any port, is sent this cookie and can set
  one of the same name. Its own cookie under a narrower path would be sent
  beside the sitting's, first, for the paths under it; so a request that
  carries `softland-sitting` more than once is refused
  `:sitting-cookie-twice`. What another server on this machine does with
  the cookie it is sent is not in reach of this namespace; the server is
  bound to 127.0.0.1, so only programs on this machine reach it.

  Browser posts. Every POST that carries an `Origin` header must carry
  exactly the runtime's own origin, `http://localhost:<port>`; any other is
  refused `:foreign-origin`. A POST whose sitting comes from the cookie
  must carry it: one with no `Origin` is refused `:no-origin`, since a
  browser always sends it on a POST and a program sends a bearer token.
  `/sign-in` and `/link` must carry it too, and they check Google's
  double-submit token: the `g_csrf_token` cookie and the `g_csrf_token` of
  the body must both be there and be equal, else `:csrf-token-missing` or
  `:csrf-token-differs` (BUILD.md, \"For the builder\").

  Bodies and answers. A request body is EDN (`application/edn`), read with
  `clojure.edn` and never evaluated, at most `max-body` bytes; or a form
  (`application/x-www-form-urlencoded`), which only the page sends. A bad
  `%` escape in a form or in the query is refused `:form-field-not-read`. An
  answer to EDN or to a GET but `/` is EDN, `{:ok …}` or `{:refuse word …}`, the
  service's own answer or one of `words`. An answer to a form is the page
  again (`softland.runtime.page`), with what the form did shown on top; a
  sign-in, a link and a sign-out by form answer `303 See Other` to `/`, so
  the browser shows the page with its new cookie. A token made by the page
  is shown on that answer once.

  The routes (`routes`), each with who may call it:

    GET  /                  the page; signed in or not
    POST /sign-in           a form with credential, g_csrf_token, invitation
    POST /link              a second Google account, while signed in
    POST /sign-out          ends the sitting or the token that calls it
    GET  /me                who is asking: the actor, and her identity's
                            record (name, own layer, record layer)
    POST /act               an act, bound to who is asking
    POST /read              a query, asked as who is asking, or not signed in
    POST /name              {:name}, and :identity for an agent she made
    POST /agent             {:kind :name :description}: an agent or program
    POST /agent/copy        {:agent :name}: a copy of an agent
    POST /agent/token       {:agent}: its token, shown once
    POST /agent/token/end   {:session}, and :identity when the session is
                            an agent's or a program's that she made
    POST /token/ask         {:agent}: an agent asks for a token; no sign-in
    GET  /token/pending     the requests that wait for the one asking
    POST /token/approve     {:request}
    POST /token/collect     {:request}: the agent takes its token, once
    POST /run               {:engine :context :human-in-loop :cause}: an
                            agent starts a run in its session
    POST /right/give        {:layer :to :allows}
    POST /right/end         {:layer :of}
    POST /admin/invite      {:gives}: an invitation's code and link
    POST /admin/link        {:account :person}: Sid links an account
    POST /admin/block       {:sender :block}
    POST /admin/unblock     {:sender}
    POST /admin/limit       {:entity :limits}: a kind of actor's or one
                            identity's {:acts-per-minute :bytes-per-minute}

  The admin routes take only `Authorization: Bearer <admin token>`, compared
  in constant time with the token handed in at `serve!`
  (`softland.runtime.admin`); no cookie and no actor's token opens them.

  What each route hands the service, and the shape of its answer, are the
  service's (`softland.runtime.service`); this namespace reads the request,
  finds who is asking, and writes the answer. It keeps no state of its own
  between requests: a sitting is the cookie the browser holds, checked by
  the service at each request.

  `/me`, and the page's forms that need her own layer, read the actor's own
  identity record with the service's `own-identity`: the identities layer
  is the system's, members only, and the system reads her own record for
  her and no other.

  Inputs and results, by call:

    serve!  a service and options -> {:server :port :origin :stop}
    words   the words this namespace refuses with, in the order of its checks

  State and effects. `serve!` opens a server socket on 127.0.0.1 and a pool
  of threads; `:stop` closes both. Each request calls the service. Nothing
  here prints a token, a code or the admin token. Faults that the service
  or this code throw are answered `{:refuse :runtime-fault}` with status
  500, and their class and message are printed to stderr.

  Callers. `softland.runtime.main`, and the HTTP proof
  (`test-runtime/softland/runtime/http_proof.clj`)."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [softland.runtime.admin :as admin]
            [softland.runtime.page :as page]
            [softland.runtime.service :as service]
            [softland.store.act :as act])
  (:import [com.sun.net.httpserver HttpExchange HttpHandler HttpServer]
           [java.io ByteArrayOutputStream InputStream]
           [java.net InetAddress InetSocketAddress URLDecoder]
           [java.nio.charset StandardCharsets]
           [java.security MessageDigest]
           [java.util.concurrent ExecutorService Executors ThreadFactory]))

;; ------------------------------------------------------------------ words

(def words
  "The words this namespace refuses with, in the order of its checks. The
  service's own words come back as it gives them: a token that it does not
  take is refused with its word (`softland.runtime.service/actor-of`)."
  [:not-this-host        ; Host is not localhost:<port> or 127.0.0.1:<port>
   :form-field-not-read  ; a bad % escape in the query or a form; or a form
                         ; field that is not an id, a name or keys
   :sitting-cookie-twice ; the request carries `softland-sitting` more than once
   :no-such-route
   :method-not-allowed
   :foreign-origin       ; a POST with an Origin that is not the runtime's
   :body-too-large
   :body-not-edn
   :body-not-a-map
   :not-admin            ; an admin route without the admin token
   :run-not-read         ; a Softland-Run header that is no UUID
   :no-origin            ; a POST by cookie, or a sign-in, with no Origin
   :csrf-token-missing
   :csrf-token-differs
   :not-signed-in        ; a route that needs someone, asked by no one
   :runtime-fault])

(def max-body
  "The most bytes a request body may hold: 1 MiB. A session's number."
  (* 1024 1024))

(def ^:private cookie-name "The sitting's cookie." "softland-sitting")

;; ------------------------------------------------------------- the request

(defn- read-body
  "Takes an exchange. Gives {:ok bytes} with at most `max-body` bytes read,
  or {:refuse :body-too-large} when the body holds more."
  [^HttpExchange ex]
  (let [^InputStream in (.getRequestBody ex)
        out (ByteArrayOutputStream.)
        buf (byte-array 8192)]
    (loop [total 0]
      (let [n (.read in buf)]
        (cond
          (neg? n) {:ok (.toByteArray out)}
          (> (+ total n) max-body) {:refuse :body-too-large}
          :else (do (.write out buf 0 n) (recur (+ total n))))))))

(defn- header
  "Takes an exchange and a header's name. Gives its first value, or nil."
  [^HttpExchange ex nm]
  (.getFirst (.getRequestHeaders ex) nm))

(defn- cookies
  "Takes an exchange. Gives a map from each cookie's name to the vector of
  its values, in the order the request gives them, from every `Cookie`
  header. A browser sends one name more than once when cookies of that name
  were set with different paths or by different servers of the host."
  [^HttpExchange ex]
  (let [hs (.get (.getRequestHeaders ex) "Cookie")]
    (reduce (fn [m part]
              (let [[k v] (str/split (str/trim part) #"=" 2)]
                (if (str/blank? k) m (update m k (fnil conj []) (or v "")))))
            {}
            (mapcat #(str/split % #";") (or hs [])))))

(defn- cookie
  "Takes the request and a cookie's name. Gives the first value the request
  carries under that name, or nil."
  [req nm]
  (first (get (:cookies req) nm)))

(defn- decode
  "Takes a text of a form or a query. Gives it decoded (`+` a space, `%xx`
  a byte of UTF-8), or nil when an escape is bad."
  [s]
  (try (URLDecoder/decode (str s) StandardCharsets/UTF_8)
       (catch IllegalArgumentException _ nil)))

(defn- form-fields
  "Takes the text of a form body or a query string. Gives {:ok map}, from
  each field's name to its value, both decoded, the first value of a name
  winning; or {:refuse :form-field-not-read :at text} for the first part
  with a bad `%` escape."
  [text]
  (reduce (fn [acc pair]
            (let [[k v] (str/split pair #"=" 2)
                  dk (decode k)
                  dv (decode (or v ""))]
              (cond
                (or (nil? dk) (nil? dv)) (reduced {:refuse :form-field-not-read :at pair})
                (or (str/blank? dk) (contains? (:ok acc) dk)) acc
                :else (assoc-in acc [:ok dk] dv))))
          {:ok {}}
          (remove str/blank? (str/split (str text) #"&"))))

(defn- read-edn
  "Takes the text of an EDN body. Gives {:ok value}, or {:refuse
  :body-not-edn} when it is not one EDN value. `clojure.edn` evaluates
  nothing; it knows the tags #uuid and #inst and no other. A body nested so
  deep that the reader runs out of stack is refused the same way."
  [text]
  (try
    (let [v (edn/read-string {:eof ::nothing} text)]
      (if (= ::nothing v) {:refuse :body-not-edn} {:ok v}))
    (catch Throwable _ {:refuse :body-not-edn})))

(defn- form-type?
  "True when a Content-Type names a form, `application/x-www-form-urlencoded`."
  [content-type]
  (str/starts-with? (str/lower-case (str content-type)) "application/x-www-form-urlencoded"))

(defn- form? [req] (form-type? (:content-type req)))

(defn- bearer
  "Takes an exchange. Gives the text after `Bearer ` in its Authorization
  header, trimmed, or nil."
  [ex]
  (let [h (header ex "Authorization")]
    (when (and h (str/starts-with? h "Bearer ")) (str/trim (subs h 7)))))

(defn- request-of
  "Takes an exchange. Gives the request as a value: :method, :path,
  :raw-path and :raw-query (as the request wrote them), :query (the query
  string's fields, {:ok map} or a refusal), :host, :origin, :content-type,
  :bearer, :run (the Softland-Run header), :cookies (each name to its
  values), :body {:ok bytes} or {:refuse word}, and :fields, for a form
  whose body was read: its fields, {:ok map} or a refusal."
  [^HttpExchange ex]
  (let [uri (.getRequestURI ex)
        content-type (header ex "Content-Type")
        body (read-body ex)]
    {:method (str/upper-case (.getRequestMethod ex))
     :path (.getPath uri)
     :raw-path (.getRawPath uri)
     :raw-query (.getRawQuery uri)
     :query (form-fields (.getRawQuery uri))
     :host (header ex "Host")
     :origin (header ex "Origin")
     :content-type content-type
     :bearer (bearer ex)
     :run (header ex "Softland-Run")
     :cookies (cookies ex)
     :body body
     :fields (when (and (form-type? content-type) (:ok body))
               (form-fields (String. ^bytes (:ok body) StandardCharsets/UTF_8)))}))

;; ----------------------------------------------------------- the answer

(defn- edn-text [v]
  (binding [*print-length* nil *print-level* nil *print-meta* false *print-namespace-maps* false]
    (pr-str v)))

(defn- answer
  "Takes an exchange, a status, the headers to add (a seq of name and
  value) and a body text. Sends them, with the content's type and the
  headers of every answer: `Referrer-Policy: no-referrer-when-downgrade`,
  `Cache-Control: no-store`, `X-Frame-Options: DENY` and
  `Content-Security-Policy: frame-ancestors 'none'`; and closes the
  exchange."
  [^HttpExchange ex status headers content-type ^String text]
  (let [bs (.getBytes (str text) StandardCharsets/UTF_8)
        hs (.getResponseHeaders ex)]
    (.set hs "Content-Type" content-type)
    (.set hs "Referrer-Policy" "no-referrer-when-downgrade")
    (.set hs "Cache-Control" "no-store")
    (.set hs "X-Frame-Options" "DENY")
    (.set hs "Content-Security-Policy" "frame-ancestors 'none'")
    (doseq [[k v] headers] (.add hs k v))
    (.sendResponseHeaders ex status (if (zero? (alength bs)) -1 (alength bs)))
    (when (pos? (alength bs))
      (with-open [out (.getResponseBody ex)] (.write out bs)))
    (.close ex)))

(def ^:private status-of-word
  "The status of each word of this namespace, and of the service's words
  that say a token is not taken. Every other refusal is 422: the request
  was read, and the runtime said no."
  {:not-this-host 421 :no-such-route 404 :method-not-allowed 405
   :foreign-origin 403 :no-origin 403 :csrf-token-missing 403 :csrf-token-differs 403
   :not-admin 403 :body-too-large 413 :body-not-edn 400 :body-not-a-map 400
   :form-field-not-read 400 :sitting-cookie-twice 400 :run-not-read 400
   :not-signed-in 401 :runtime-fault 500
   :token-not-valid 401 :session-ended 401 :run-not-the-sessions 403})

(defn- status-of [result]
  (if (contains? result :ok) 200 (get status-of-word (:refuse result) 422)))

(defn- sitting-cookie
  "Takes a sitting's token. Gives the Set-Cookie value that the browser keeps
  it under: HttpOnly, SameSite=Lax, Path=/, and no Secure flag (http on
  localhost). It has no Max-Age, so it ends with the browser's session."
  [token]
  (str cookie-name "=" token "; HttpOnly; SameSite=Lax; Path=/"))

(def ^:private cleared-cookie
  (str cookie-name "=; HttpOnly; SameSite=Lax; Path=/; Max-Age=0"))

;; ------------------------------------------------------ form fields to values

(defn- field-uuid
  "Takes a form's text. Gives its UUID, or nil when it is not one."
  [s]
  (try (parse-uuid (str/trim (str s))) (catch Exception _ nil)))

(defn- field-allows
  "Takes the text of a form's keys field. Gives `::default` for a blank
  field, `:see` for `see`, or the set of key patterns it names, each an EDN
  keyword, split at spaces and commas; nil when a part is no keyword. The
  page gives rights to agents and programs, so it offers no `see-and-act`:
  a blank field leaves `:allows` out, and the service then gives `#{:*}`,
  every plain key and no store key (`softland.runtime.service/give-right!`).
  Whether a pattern is allowed is the store's to say
  (`softland.store.act`)."
  [s]
  (let [s (str/trim (str s))]
    (cond
      (str/blank? s) ::default
      (= s "see") :see
      :else (let [parts (remove str/blank? (str/split s #"[\s,]+"))
                  ks (map #(try (edn/read-string %) (catch Exception _ nil)) parts)]
              (when (and (seq ks) (every? keyword? ks)) (set ks))))))

(defn- need
  "Takes values of a form, each a [field-name value]. Gives nil when every
  value is there, or {:refuse :form-field-not-read :at name} for the first
  that is not."
  [& pairs]
  (some (fn [[nm v]] (when (nil? v) {:refuse :form-field-not-read :at nm})) pairs))

;; ------------------------------------------------------------- the routes

(defn- me
  "Takes the service and an actor. Gives what `/me` answers: {:ok} the
  actor, {:id :kind :session} and its `:run`, with what her identity's
  record holds: `:name`, `:own-layer`, `:record-layer`, and `:made-by`,
  `:copied-from` and `:invited-by` when they are there. The record is the
  service's `own-identity`: an identity reads its own record and no other.
  A refusal of it is given as it came."
  [svc actor]
  (let [a (service/own-identity svc actor)]
    (if (:refuse a)
      a
      (let [r (:ok a)]
        {:ok (merge actor
                    (into {} (keep (fn [[k to]] (when (some? (get r k)) [to (get r k)])))
                          {:identity/name :name :identity/own-layer :own-layer
                           :identity/record-layer :record-layer :identity/made-by :made-by
                           :identity/copied-from :copied-from :identity/invited-by :invited-by}))}))))

(defn- own-layer
  "Takes the service and an actor. Gives the actor's own layer, or nil."
  [svc actor]
  (get-in (me svc actor) [:ok :own-layer]))

(defn- note-act
  "Takes a person's own layer, her id and a note's text. Gives the act the
  page writes for a note: one fact `:note/text` on a new entity, under her
  first permission in her own layer, `[person layer layer]`
  (`softland.runtime.steps/making-act`). The binding sets `:who` and
  `:session`. The key `:note/text` is a session's word (builder F)."
  [layer person text]
  {:version act/version
   :layer layer
   :permission [person layer layer]
   :facts [{:e (random-uuid) :k :note/text :v text}]})

(def routes
  "Each route: [method path] -> {:who :csrf :call :form :page}.

    :who    :anyone (no one needed; who is asking is passed when known),
            :actor (someone signed in, by cookie or bearer), :admin
    :csrf   true for the routes that Google's credential comes to
    :call   (fn [svc actor input]) -> the service's answer; input is the
            request's EDN map, or what :form gave
    :form   (fn [svc actor fields]) -> {:ok input} or a refusal: the input
            that a form of the page gives; nil when the page sends none
    :page   what the page says the form did, or :to-page when a form's
            answer is a 303 to `/`

  What each call takes and gives is in its docstring in
  `softland.runtime.service`."
  {[:get "/me"]
   {:who :actor :call (fn [svc actor _] (me svc actor))}

   [:post "/sign-in"]
   {:who :anyone :csrf true :page :to-page
    :call (fn [svc _ in] (service/sign-in! svc (:credential in) (not-empty (:invitation in))))
    :form (fn [_ _ f] {:ok {:credential (get f "credential") :invitation (get f "invitation")}})}

   [:post "/link"]
   {:who :actor :csrf true :page :to-page
    :call (fn [svc actor in] (service/link-account! svc actor (:credential in)))
    :form (fn [_ _ f] {:ok {:credential (get f "credential")}})}

   [:post "/sign-out"]
   {:who :actor :page :to-page
    :call (fn [svc actor _] (service/sign-out! svc actor))
    :form (fn [_ _ _] {:ok {}})}

   [:post "/act"]
   {:who :actor :page "Your note, written"
    :call (fn [svc actor in] (service/act! svc actor in))
    :form (fn [svc actor f]
            (let [layer (own-layer svc actor) text (get f "note")]
              (or (need ["own-layer" layer] ["note" (not-empty text)])
                  {:ok (note-act layer (:id actor) text)})))}

   [:post "/read"]
   {:who :anyone :page "Your layer, read back"
    :call (fn [svc actor in] (service/read! svc actor in))
    :form (fn [svc actor _]
            (let [layer (when actor (own-layer svc actor))]
              (or (need ["own-layer" layer])
                  {:ok {:asks [:run :first :last] :layers [layer] :with #{:writer :permission :time}}})))}

   [:post "/name"]
   {:who :actor :page "Your name"
    :call (fn [svc actor in] (service/rename! svc actor (:name in) (:identity in)))
    :form (fn [_ _ f] {:ok {:name (str/trim (str (get f "name")))}})}

   [:post "/agent"]
   {:who :actor :page "Made"
    :call (fn [svc actor in] (service/make-agent! svc actor in))
    :form (fn [_ _ f]
            (let [kind (get {"agent" :agent "program" :program} (get f "kind"))
                  instructions (str/trim (str (get f "description")))]
              (or (need ["kind" kind])
                  {:ok (cond-> {:kind kind :name (str/trim (str (get f "name")))}
                         (seq instructions) (assoc :description {:instructions instructions}))})))}

   [:post "/agent/copy"]
   {:who :actor
    :call (fn [svc actor in] (service/copy-agent! svc actor (:agent in) (select-keys in [:name])))}

   [:post "/agent/token"]
   {:who :actor :page "Its token"
    :call (fn [svc actor in] (service/make-token! svc actor (:agent in)))
    :form (fn [_ _ f]
            (let [a (field-uuid (get f "agent"))]
              (or (need ["agent" a]) {:ok {:agent a}})))}

   [:post "/agent/token/end"]
   {:who :actor :page "The token, ended"
    :call (fn [svc actor in] (service/end-token! svc actor (:session in) (:identity in)))
    :form (fn [_ _ f]
            (let [s (field-uuid (get f "session"))
                  i (field-uuid (get f "identity"))]
              (or (need ["session" s] ["identity" i]) {:ok {:session s :identity i}})))}

   [:post "/token/ask"]
   {:who :anyone :call (fn [svc _ in] (service/ask-token! svc (:agent in)))}

   [:get "/token/pending"]
   {:who :actor :call (fn [svc actor _] (service/pending-requests svc actor))}

   [:post "/token/approve"]
   {:who :actor :page "Approved; the agent collects its token"
    :call (fn [svc actor in] (service/approve-token! svc actor (:request in)))
    :form (fn [_ _ f]
            (let [r (field-uuid (get f "request"))]
              (or (need ["request" r]) {:ok {:request r}})))}

   [:post "/token/collect"]
   {:who :anyone :call (fn [svc _ in] (service/collect-token! svc (:request in)))}

   [:post "/run"]
   {:who :actor :call (fn [svc actor in] (service/start-run! svc actor in))}

   [:post "/right/give"]
   {:who :actor :page "The right, given"
    :call (fn [svc actor in] (service/give-right! svc actor in))
    :form (fn [svc actor f]
            (let [layer (own-layer svc actor)
                  to (field-uuid (get f "to"))
                  allows (field-allows (get f "allows"))]
              (or (need ["own-layer" layer] ["to" to] ["allows" allows])
                  {:ok (cond-> {:layer layer :to to}
                         (not= ::default allows) (assoc :allows allows))})))}

   [:post "/right/end"]
   {:who :actor :page "The right, ended"
    :call (fn [svc actor in] (service/end-right! svc actor in))
    :form (fn [svc actor f]
            (let [layer (own-layer svc actor)
                  of (field-uuid (get f "of"))]
              (or (need ["own-layer" layer] ["of" of])
                  {:ok {:layer layer :of of}})))}

   [:post "/admin/invite"]
   {:who :admin :call (fn [svc _ in] (service/invite! svc (select-keys in [:gives])))}

   [:post "/admin/link"]
   {:who :admin :call (fn [svc _ in] (service/link-by-admin! svc (:account in) (:person in)))}

   [:post "/admin/block"]
   {:who :admin :call (fn [svc _ in] (service/block! svc (:sender in) (or (:block in) {})))}

   [:post "/admin/unblock"]
   {:who :admin :call (fn [svc _ in] (service/unblock! svc (:sender in)))}

   [:post "/admin/limit"]
   {:who :admin :call (fn [svc _ in] (service/set-limits! svc (:entity in) (or (:limits in) {})))}})

(def ^:private paths (set (map second (keys routes))))

;; --------------------------------------------------------------- handling

(defn- host-ok?
  "True when the request's Host is `localhost:<port>` or `127.0.0.1:<port>`."
  [req port]
  (contains? #{(str "localhost:" port) (str "127.0.0.1:" port)} (:host req)))

(defn- localhost-of
  "Takes the request and the port. Gives the same path and query at
  `http://localhost:<port>`, for a request whose Host is
  `127.0.0.1:<port>`; nil for any other Host. The page's origin is
  localhost's alone: Google's client names it, and a browser post must
  carry it."
  [req port]
  (when (= (:host req) (str "127.0.0.1:" port))
    (str "http://localhost:" port (:raw-path req)
         (when (:raw-query req) (str "?" (:raw-query req))))))

(defn- same-text?
  "True when two texts are there and equal, compared in constant time."
  [a b]
  (and (string? a) (string? b) (not (str/blank? a))
       (MessageDigest/isEqual (.getBytes ^String a StandardCharsets/UTF_8)
                              (.getBytes ^String b StandardCharsets/UTF_8))))

(defn- input-of
  "Takes the request, the route, the service and the actor. Gives {:ok
  input}: the form's input for a form, the EDN map for EDN, {} for a GET or
  an empty body; or a refusal."
  [req route svc actor]
  (let [body (:body req)]
    (cond
      (:refuse body) body
      (= "GET" (:method req)) {:ok {}}
      (form? req)
      (cond (:refuse (:fields req)) (:fields req)
            (:form route) ((:form route) svc actor (:ok (:fields req)))
            :else {:refuse :body-not-edn})
      (zero? (alength ^bytes (:ok body))) {:ok {}}
      :else
      (let [r (read-edn (String. ^bytes (:ok body) StandardCharsets/UTF_8))]
        (cond (:refuse r) r
              (map? (:ok r)) r
              :else {:refuse :body-not-a-map})))))

(defn- by-what
  "Takes the request. Gives how it names who is asking, from its headers
  alone: `:bearer`, `:cookie` (the sitting's cookie, and no bearer), or nil."
  [req]
  (cond (:bearer req) :bearer
        (not (str/blank? (cookie req cookie-name))) :cookie))

(defn- who-asks
  "Takes the service and the request. Gives {:actor a}, the actor that the
  service's `actor-of` gave for the bearer token or the sitting's cookie,
  with the run of the `Softland-Run` header when there is one; {:actor nil}
  when the request names no one; or the refusal: the service's word
  (`:token-not-valid`, `:session-ended`, `:run-not-the-sessions`, …), or
  `:run-not-read` for a header that is no UUID."
  [svc req]
  (let [token (case (by-what req)
                :bearer (:bearer req)
                :cookie (cookie req cookie-name)
                nil)
        run-text (:run req)
        run (some-> run-text str/trim parse-uuid)]
    (cond
      (nil? token) {:actor nil}
      (and (some? run-text) (nil? run)) {:refuse :run-not-read}
      :else (let [r (service/actor-of svc token run)]
              (if (contains? r :ok) {:actor (:ok r)} r)))))

(defn- check-post
  "Takes the request, the route, how the actor was found, and the origin.
  Gives nil when the POST may go on, or a refusal: the checks of Origin
  and of Google's double-submit token (the namespace docstring)."
  [req route by origin]
  (let [o (:origin req)]
    (cond
      (and o (not= o origin)) {:refuse :foreign-origin}
      (and (nil? o) (or (= by :cookie) (:csrf route))) {:refuse :no-origin}
      (:csrf route)
      (let [in-cookie (cookie req "g_csrf_token")
            in-body (when (form? req) (get-in req [:fields :ok "g_csrf_token"]))]
        (cond (or (str/blank? in-cookie) (str/blank? in-body)) {:refuse :csrf-token-missing}
              (not (same-text? in-cookie in-body)) {:refuse :csrf-token-differs})))))

(defn- page-data
  "Takes the service and an actor or nil. Gives what the page shows of her:
  :me and :pending, each the service's `:ok`, or nil."
  [svc actor]
  (when actor
    {:me (:ok (me svc actor))
     :pending (let [p (:ok (service/pending-requests svc actor))] (if (sequential? p) p (some-> p seq vec)))}))

(defn- shown-once
  "Takes a route's path and its result. Gives the text the page shows once,
  or nil: a token that `/agent/token` made."
  [path result]
  (when (= path "/agent/token")
    (when-let [t (get-in result [:ok :token])]
      {:what "The token, for the agent to send as Authorization: Bearer <token>." :text t})))

(defn- answer-page
  "Answers with the page: for the actor (or no one), with what a form did."
  [ex status headers svc actor client-id said shown]
  (answer ex status headers "text/html; charset=utf-8"
          (page/render (merge {:client-id client-id :said said :shown shown}
                              (page-data svc actor)))))

(defn- handle
  "Takes the server's settings and an exchange. Reads the request; answers
  a Host that is not this runtime's `:not-this-host`, and one of
  `127.0.0.1:<port>` with a 308 to the same path at `localhost:<port>`;
  refuses a bad `%` escape in the query or a form `:form-field-not-read`,
  and the sitting's cookie sent twice `:sitting-cookie-twice`; then finds
  the route and who is asking, checks what a POST must carry, calls the
  route, and answers. Never throws: a fault is answered `:runtime-fault`."
  [{:keys [svc port origin admin-token client-id]} ^HttpExchange ex]
  (try
    (let [req (request-of ex)
          method (keyword (str/lower-case (:method req)))
          route (get routes [method (:path req)])
          edn-answer (fn [result & headers]
                       (answer ex (status-of result) headers "application/edn; charset=utf-8" (edn-text result)))]
      (cond
        (not (host-ok? req port)) (edn-answer {:refuse :not-this-host})

        (localhost-of req port)
        (answer ex 308 [["Location" (localhost-of req port)]] "text/plain; charset=utf-8" "")

        (:refuse (:query req)) (edn-answer (:query req))
        (:refuse (:fields req)) (edn-answer (:fields req))

        ;; Cookies are kept by host, not by port: another server on
        ;; localhost can set a cookie of this name with a narrower path,
        ;; which the browser then sends beside the sitting's.
        (< 1 (count (get (:cookies req) cookie-name))) (edn-answer {:refuse :sitting-cookie-twice})

        (and (= :get method) (= "/" (:path req)))
        ;; The page for whoever the cookie names. A cookie whose token the
        ;; service no longer takes is cleared; on any other refusal (a read
        ;; that failed) the page shows signed out and the cookie is kept.
        (let [w (who-asks svc req)]
          (cond
            (contains? #{:token-not-valid :session-ended} (:refuse w))
            (answer-page ex 200 [["Set-Cookie" cleared-cookie]] svc nil client-id nil nil)
            (:refuse w)
            (answer-page ex 200 [] svc nil client-id {:what "Who you are could not be read" :answer w} nil)
            :else (answer-page ex 200 [] svc (:actor w) client-id nil nil)))

        (nil? route) (edn-answer {:refuse (if (contains? paths (:path req)) :method-not-allowed :no-such-route)})

        (= :admin (:who route))
        (cond
          (and (:origin req) (not= (:origin req) origin)) (edn-answer {:refuse :foreign-origin})
          (not (admin/matches? (:bearer req) admin-token)) (edn-answer {:refuse :not-admin})
          :else (let [in (input-of req route svc nil)]
                  (edn-answer (if (:refuse in) in ((:call route) svc nil (:ok in))))))

        :else
        (let [by (by-what req)
              page? (form? req)
              post-fault (when (= :post method) (check-post req route by origin))
              ;; A sign-in with a cookie that the service no longer takes
              ;; goes on as no one; every other route answers the refusal.
              w (cond post-fault {:actor nil}
                      (= (:path req) "/sign-in") {:actor nil}
                      :else (who-asks svc req))
              fail (fn [result]
                     (if page?
                       (answer-page ex (status-of result) [] svc (:actor w) client-id
                                    {:what "Refused" :answer result} nil)
                       (edn-answer result)))]
          (cond
            post-fault (fail post-fault)
            (:refuse w) (fail w)
            (and (= :actor (:who route)) (nil? (:actor w))) (fail {:refuse :not-signed-in})
            :else
            (let [actor (:actor w)
                  in (input-of req route svc actor)]
              (if (:refuse in)
                (fail in)
                (let [result ((:call route) svc actor (:ok in))]
                  (cond
                    (not page?)
                    (apply edn-answer result
                           (concat
                            (when (and (= (:path req) "/sign-in") (get-in result [:ok :token]))
                              [["Set-Cookie" (sitting-cookie (get-in result [:ok :token]))]])
                            (when (and (= (:path req) "/sign-out") (:ok result) (= by :cookie))
                              [["Set-Cookie" cleared-cookie]])))

                    (:refuse result) (fail result)

                    (= :to-page (:page route))
                    (answer ex 303
                            (concat [["Location" "/"]]
                                    (when-let [t (and (= (:path req) "/sign-in") (get-in result [:ok :token]))]
                                      [["Set-Cookie" (sitting-cookie t)]])
                                    (when (= (:path req) "/sign-out") [["Set-Cookie" cleared-cookie]]))
                            "text/plain; charset=utf-8" "")

                    :else
                    (let [shown (shown-once (:path req) result)
                          said-result (if shown (update result :ok #(if (map? %) (assoc % :token "<shown above>") %)) result)]
                      (answer-page ex 200 [] svc actor client-id
                                   {:what (:page route) :answer said-result} shown))))))))))
    (catch Throwable t
      (binding [*out* *err*]
        (println "runtime http: fault" (.getName (class t)) (.getMessage t)))
      (try
        (answer ex 500 [] "application/edn; charset=utf-8" (edn-text {:refuse :runtime-fault}))
        (catch Throwable _ (.close ex))))))

;; ------------------------------------------------------------------ serve

(defn serve!
  "Takes the runtime's service and options:

    :port         the port, 8400 when left out; 0 takes a free one
    :client-id    the Google client id, for the page
    :admin-token  the admin token's text (`softland.runtime.admin`)
    :threads      the threads that answer requests, 16 when left out

  Binds 127.0.0.1 only, and starts answering. Gives {:server :port :origin
  :stop}: the port actually bound, the origin that browser posts must carry
  (`http://localhost:<port>`), and a function of no argument that stops the
  server and its threads. Throws when the port cannot be bound."
  [svc {:keys [port client-id admin-token threads] :or {port 8400 threads 16}}]
  (let [server (HttpServer/create (InetSocketAddress. (InetAddress/getByName "127.0.0.1") (int port)) 64)
        bound (.getPort (.getAddress server))
        settings {:svc svc :port bound :origin (str "http://localhost:" bound)
                  :admin-token admin-token :client-id client-id}
        n (atom 0)
        pool (Executors/newFixedThreadPool
              (int threads)
              (reify ThreadFactory
                (newThread [_ r]
                  (doto (Thread. ^Runnable r (str "runtime-http-" (swap! n inc)))
                    (.setDaemon true)))))]
    (.createContext server "/" (reify HttpHandler (handle [_ ex] (handle settings ex))))
    (.setExecutor server pool)
    (.start server)
    {:server server :port bound :origin (:origin settings)
     :stop (fn [] (.stop server 0) (.shutdownNow ^ExecutorService pool))}))
