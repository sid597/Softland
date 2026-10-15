(ns softland.runtime.http-proof
  "The proof of the runtime's way in over HTTP: what `softland.runtime.main`
  starts, driven as a browser, an agent and Sid's command line drive it. It
  needs no network. One command, from the repository root, under the lock
  that the store's cluster proofs share:

    flock src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock \\
      clojure -M:runtime-module -m softland.runtime.http-proof

  It prints each check's sentence after PASSED or FAILED, prints what it
  saw on lines that start with SHOWN, prints those lines again together at
  the end, and exits with a status that is not zero when a check fails.

  What it starts. `softland.runtime.main/start!`, as `-main` does, with
  three things of its own: a free port (port 0), an admin token's file in
  a temporary folder, and a stand-in for Google. The stand-in is a key pair
  of the proof's, made with Nimbus's `RSAKeyGenerator` with a `kid`, whose
  public half is the key set handed to the service in place of Google's;
  the proof signs ID tokens with it as Google does (RS256, `iss`
  `https://accounts.google.com`, `aud` the client id, `sub`, `exp`).

  What it checks, in order, each over HTTP with `java.net.http.HttpClient`:
  an admin route without the admin token, and with a wrong one, refused; an
  invitation made by Sid's command line through the admin route, and a
  limit set by it; a sign-in
  POST whose `g_csrf_token` cookie and body differ refused, one with no
  cookie refused, one from a foreign Origin refused, none of them setting a
  cookie; a sign-in POST with a matching `g_csrf_token` and the invitation
  answered with an HttpOnly, SameSite=Lax cookie; the page and `/me` with
  that cookie, each answer with the headers that forbid a frame; a request
  to 127.0.0.1 sent on to localhost by a 308; a bad `%` escape refused; the
  sitting's cookie sent twice refused; a POST by that cookie with no Origin
  refused; an agent made,
  a right given to it in her own layer, its token made; a run started by
  the agent with its token; an act with the token accepted and read back
  as the agent's, in its run, beneath her right; the token ended and the
  agent's next act refused; a token asked for by the agent with no
  sign-in, approved by her, and collected once; and a read with no cookie
  and no token that sees the base and not her own layer, which is a
  members layer.

  What it does not reach: Google itself, its keys and its button in a
  browser (Part 3); the JavaScript of the page, which no browser runs here;
  a Host header that is not localhost, which Java's client does not let a
  caller set.

  State. One in-process cluster, with the store's module, which the proof
  closes at the end; one server on a free port of 127.0.0.1; one temporary
  folder with the admin token's file, removed at the end. The proof owns
  one atom of the lines it showed. It reads the clock for the times in the
  ID tokens it signs."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]
            [softland.runtime.command :as command]
            [softland.runtime.main :as main]
            [softland.store.act :as act])
  (:import [com.nimbusds.jose JOSEObjectType JWSAlgorithm JWSHeader$Builder]
           [com.nimbusds.jose.crypto RSASSASigner]
           [com.nimbusds.jose.jwk JWKSet KeyUse RSAKey]
           [com.nimbusds.jose.jwk.gen RSAKeyGenerator]
           [com.nimbusds.jwt JWTClaimsSet$Builder SignedJWT]
           [java.io BufferedReader File InputStreamReader]
           [java.net Socket URI URLDecoder URLEncoder]
           [java.net.http HttpClient HttpClient$Redirect HttpRequest HttpRequest$BodyPublishers
            HttpResponse HttpResponse$BodyHandlers]
           [java.nio.charset StandardCharsets]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.util Date]))

;; ------------------------------------------------------------------ printing

(defonce ^{:private true :doc "The lines that the proof printed with SHOWN, for the recap."}
  shown-lines (atom []))

(defn- shown
  "Prints one line of what the proof saw, and keeps it for the recap."
  [& xs]
  (let [line (apply print-str "SHOWN" xs)]
    (swap! shown-lines conj line)
    (println line)
    (flush)))

(defn- holds
  "Checks one thing: the value must be true. Prints the sentence after
  PASSED or FAILED."
  [sentence ok?]
  (println (if (is (true? (boolean ok?)) sentence) "PASSED" "FAILED") sentence)
  (flush))

;; ---------------------------------------------------------- Google's stand-in

(def ^:private client-id "proof-client.apps.googleusercontent.com")

(def ^:private kid "proof-key-1")

(defonce ^{:private true :doc "The proof's key pair, in place of Google's."}
  google-key
  (delay (-> (RSAKeyGenerator. (int 2048)) (.keyID kid) (.keyUse KeyUse/SIGNATURE)
             (.algorithm JWSAlgorithm/RS256) (.generate))))

(defn- google-keys
  "The key set handed to the service in place of Google's: the public half
  of the proof's key pair."
  []
  (JWKSet. (.toPublicJWK ^RSAKey @google-key)))

(defn- id-token
  "Takes an account's number. Gives an ID token signed as Google signs one:
  RS256 under the proof's kid, from `https://accounts.google.com`, for the
  proof's client id, issued now and good for an hour."
  [sub]
  (let [now (System/currentTimeMillis)
        claims (-> (JWTClaimsSet$Builder.)
                   (.issuer "https://accounts.google.com")
                   (.audience ^String client-id)
                   (.subject sub)
                   (.issueTime (Date. now))
                   (.expirationTime (Date. (+ now 3600000)))
                   (.claim "email" (str sub "@example.com"))
                   (.claim "email_verified" true)
                   (.claim "name" "Proof Person")
                   (.build))
        header (-> (JWSHeader$Builder. JWSAlgorithm/RS256) (.keyID kid) (.type JOSEObjectType/JWT) (.build))
        jwt (SignedJWT. header claims)]
    (.sign jwt (RSASSASigner. ^RSAKey @google-key))
    (.serialize jwt)))

;; ----------------------------------------------------------------- HTTP

(def ^:private ^:dynamic *at*
  "The runtime's address, `http://localhost:<port>`, bound by `-main`."
  nil)

(def ^:private client
  (delay (-> (HttpClient/newBuilder) (.followRedirects HttpClient$Redirect/NEVER) (.build))))

(defn- form-body [m]
  (str/join "&" (for [[k v] m]
                  (str (URLEncoder/encode (name k) StandardCharsets/UTF_8) "="
                       (URLEncoder/encode (str v) StandardCharsets/UTF_8)))))

(defn- call
  "Takes a method, a path, and {:headers {name value} :edn value :form
  map :raw-form text :at address}: an EDN body, a form encoded from a map,
  or a form's text sent as it is; `:at` in place of the runtime's address.
  Sends the request to the runtime and gives {:status :headers :body
  :edn}: the headers as a map from a lower-case name to its values, and the
  body read as EDN when it is EDN, else nil."
  [method path {:keys [headers edn form raw-form at]}]
  (let [b (-> (HttpRequest/newBuilder (URI/create (str (or at *at*) path))))
        _ (doseq [[k v] headers :when (some? v)] (.header b k v))
        body (cond edn (do (.header b "Content-Type" "application/edn") (pr-str edn))
                   form (do (.header b "Content-Type" "application/x-www-form-urlencoded") (form-body form))
                   raw-form (do (.header b "Content-Type" "application/x-www-form-urlencoded") raw-form)
                   :else nil)
        _ (if (= method :get)
            (.GET b)
            (.POST b (if body (HttpRequest$BodyPublishers/ofString body StandardCharsets/UTF_8)
                         (HttpRequest$BodyPublishers/noBody))))
        ^HttpResponse r (.send ^HttpClient @client (.build b) (HttpResponse$BodyHandlers/ofString StandardCharsets/UTF_8))
        hs (into {} (map (fn [[k v]] [(str/lower-case k) (vec v)])) (.map (.headers r)))
        text (.body r)]
    {:status (.statusCode r)
     :headers hs
     :body text
     :edn (when (some #(str/starts-with? % "application/edn") (get hs "content-type"))
            (try (edn/read-string text) (catch Exception _ nil)))}))

(defn- raw-status-line
  "Takes a request's target, such as `/me?x=%zz`, and headers. Sends a GET
  of it over a plain socket, since Java's client refuses a URI with a bad
  escape, and gives the answer's status line."
  [target headers]
  (let [port (Integer/parseInt (re-find #"\d+$" *at*))]
    (with-open [sock (Socket. "127.0.0.1" port)]
      (let [out (.getOutputStream sock)]
        (.write out (.getBytes (str "GET " target " HTTP/1.1\r\nHost: localhost:" port "\r\n"
                                    (apply str (for [[k v] headers] (str k ": " v "\r\n")))
                                    "Connection: close\r\n\r\n")
                               StandardCharsets/UTF_8))
        (.flush out)
        (first (line-seq (BufferedReader. (InputStreamReader. (.getInputStream sock) StandardCharsets/UTF_8))))))))

(defn- set-cookie
  "Takes an answer. Gives its Set-Cookie value for the sitting's cookie, or nil."
  [answer]
  (some #(when (str/starts-with? % "softland-sitting=") %) (get-in answer [:headers "set-cookie"])))

(defn- origin [] {"Origin" *at*})

(defn- said-of
  "Takes an answer that is the page. Gives the text of what the page says
  the form did (its first box's `<pre>`), entities read back, or nil."
  [answer]
  (some-> (re-find #"(?s)<div class=\"said\"><strong>[^<]*</strong><pre>(.*?)</pre>" (str (:body answer)))
          second
          (str/replace "&quot;" "\"") (str/replace "&#39;" "'") (str/replace "&lt;" "<")
          (str/replace "&gt;" ">") (str/replace "&amp;" "&")))

;; ------------------------------------------------- readers of the service's answers

(defn- id-in
  "Takes the `:ok` of an answer and the keys to try. Gives the first value
  under one of them, or the value itself when it is no map."
  [ok & ks]
  (if (map? ok) (some #(get ok %) ks) ok))

;; ------------------------------------------------------------------ the checks

(def ^:private state (atom {}))

(deftest the-admin-routes-need-the-admin-token
  (let [none (call :post "/admin/invite" {:edn {:gives []}})
        wrong (call :post "/admin/invite" {:edn {:gives []}
                                           :headers {"Authorization" (str "Bearer sla." (apply str (repeat 43 "A")))}})]
    (shown "admin route, no token:" (:status none) (pr-str (:edn none)) "| a wrong token:" (:status wrong) (pr-str (:edn wrong)))
    (holds "an admin route without the admin token is refused :not-admin, and with a wrong one too"
           (and (= 403 (:status none) (:status wrong))
                (= {:refuse :not-admin} (:edn none) (:edn wrong))))))

(deftest an-invitation-by-the-command-line
  (let [r (command/run ["invite" "--base" "see-and-act"] {:at *at* :admin-token-file (:admin-token-file @state)})
        link (:ok r)
        code (some->> link (re-find #"#invitation=(.+)$") second (#(URLDecoder/decode ^String % StandardCharsets/UTF_8)))]
    (shown "invitation link:" (some-> link (str/replace #"invitation=.+$" "invitation=<code>")) "| code starts" (some-> code (subs 0 4)))
    (holds "Sid's command line makes an invitation through the admin route and prints http://localhost:<port>/#invitation=<code>, the code in the fragment"
           (and (string? link) (str/starts-with? link (str *at* "/#invitation=")) (str/starts-with? (str code) "sli.")))
    (swap! state assoc :code code)))

(deftest a-limit-by-the-command-line
  (let [settings {:at *at* :admin-token-file (:admin-token-file @state)}
        set-it (command/run ["limit" "program" "{:acts-per-minute 120000}"] settings)
        unread (command/run ["limit" "nobody" "{:acts-per-minute 1}"] settings)]
    (shown "limit of programs:" (pr-str (dissoc set-it :answer)) "| of nobody:" (pr-str (dissoc unread :answer)))
    (holds "Sid's command line sets the limit of a kind of actor through the admin route; a kind or id it cannot read is refused :entity-not-read"
           (and (= {:entity :program} (get-in set-it [:answer :ok]))
                (= :entity-not-read (:refuse unread))))))

(defn- sign-in-post
  "Posts a sign-in form as the page does: the credential, the body's
  g_csrf_token, the invitation; with the given cookie text and Origin."
  [{:keys [token csrf-body csrf-cookie origin-header code]}]
  (call :post "/sign-in"
        {:headers (cond-> {}
                    origin-header (assoc "Origin" origin-header)
                    csrf-cookie (assoc "Cookie" (str "g_csrf_token=" csrf-cookie)))
         :form (cond-> {:credential token :invitation (or code "")}
                 csrf-body (assoc :g_csrf_token csrf-body))}))

(deftest sign-in-refusals
  (let [t (id-token "proof-sub-1")
        code (:code @state)
        differs (sign-in-post {:token t :csrf-body "aaaa" :csrf-cookie "bbbb" :origin-header *at* :code code})
        missing (sign-in-post {:token t :csrf-body "aaaa" :origin-header *at* :code code})
        foreign (sign-in-post {:token t :csrf-body "aaaa" :csrf-cookie "aaaa" :origin-header "http://evil.example" :code code})
        no-origin (sign-in-post {:token t :csrf-body "aaaa" :csrf-cookie "aaaa" :code code})]
    (shown "sign-in refusals, status:" (mapv :status [differs missing foreign no-origin])
           "| said:" (mapv said-of [differs missing foreign no-origin])
           "| set-cookie:" (mapv set-cookie [differs missing foreign no-origin]))
    (holds "a sign-in POST whose g_csrf_token cookie and body differ is refused :csrf-token-differs, and sets no cookie"
           (and (= 403 (:status differs)) (str/includes? (:body differs) ":csrf-token-differs") (nil? (set-cookie differs))))
    (holds "a sign-in POST with no g_csrf_token cookie is refused :csrf-token-missing, and sets no cookie"
           (and (= 403 (:status missing)) (str/includes? (:body missing) ":csrf-token-missing") (nil? (set-cookie missing))))
    (holds "a sign-in POST from a foreign Origin is refused :foreign-origin, and one with no Origin :no-origin"
           (and (= 403 (:status foreign) (:status no-origin))
                (str/includes? (:body foreign) ":foreign-origin") (str/includes? (:body no-origin) ":no-origin")
                (nil? (set-cookie foreign)) (nil? (set-cookie no-origin))))))

(deftest a-sign-in
  (let [r (sign-in-post {:token (id-token "proof-sub-1") :csrf-body "c5rf" :csrf-cookie "c5rf"
                         :origin-header *at* :code (:code @state)})
        sc (set-cookie r)
        cookie (some-> sc (str/split #";") first)]
    (shown "sign-in:" (:status r) "Location" (get-in r [:headers "location"]) "| said:" (said-of r)
           "| Set-Cookie:" (some-> sc (str/replace #"=sl1\.[^;]+" "=sl1.<token>")))
    (holds "a sign-in POST with a matching g_csrf_token and the invitation answers 303 to / and sets the sitting's cookie: HttpOnly, SameSite=Lax, Path=/, and no Secure flag"
           (and (= 303 (:status r)) (= ["/"] (get-in r [:headers "location"]))
                (str/starts-with? (str sc) "softland-sitting=sl1.")
                (str/includes? sc "HttpOnly") (str/includes? sc "SameSite=Lax") (str/includes? sc "Path=/")
                (not (str/includes? sc "Secure"))))
    (swap! state assoc :cookie cookie)))

(deftest who-she-is
  (let [c {"Cookie" (:cookie @state)}
        me (call :get "/me" {:headers c})
        page (call :get "/" {:headers c})
        anon (call :get "/" {})
        ok (:ok (:edn me))]
    (shown "/me:" (:status me) (pr-str (:edn me)))
    (shown "page signed in:" (:status page) (get-in page [:headers "referrer-policy"]) "| bytes" (count (:body page)))
    (shown "frame headers: page" (get-in page [:headers "x-frame-options"]) (get-in page [:headers "content-security-policy"])
           "| /me" (get-in me [:headers "x-frame-options"]) (get-in me [:headers "content-security-policy"]))
    (holds "GET /me with the cookie names her: a person, with her id, her name and her own layer"
           (and (= 200 (:status me)) (= :person (:kind ok)) (uuid? (:id ok)) (string? (:name ok)) (some? (:own-layer ok))))
    (holds "the page with the cookie shows who she is and her forms; every answer of the page carries Referrer-Policy: no-referrer-when-downgrade; without the cookie it shows Google's button"
           (and (= 200 (:status page) (:status anon))
                (= ["no-referrer-when-downgrade"] (get-in page [:headers "referrer-policy"]) (get-in anon [:headers "referrer-policy"]))
                (str/includes? (:body page) "Who you are") (str/includes? (:body page) (str (:id ok)))
                (str/includes? (:body anon) "https://accounts.google.com/gsi/client")
                (str/includes? (:body anon) (str "data-client-id=\"" client-id "\""))
                (str/includes? (:body anon) "google.accounts.id.initialize")
                (str/includes? (:body anon) "location.hash")))
    (holds "the page and an EDN answer each carry X-Frame-Options: DENY and Content-Security-Policy: frame-ancestors 'none'"
           (every? (fn [a] (and (= ["DENY"] (get-in a [:headers "x-frame-options"]))
                                (= ["frame-ancestors 'none'"] (get-in a [:headers "content-security-policy"]))))
                   [page anon me]))
    (swap! state assoc :me ok)))

(deftest what-a-request-must-be
  (let [port (re-find #"\d+$" *at*)
        redirected (call :get "/me?x=1" {:at (str "http://127.0.0.1:" port) :headers {"Cookie" (:cookie @state)}})
        bad-query (raw-status-line "/me?x=%zz" {"Cookie" (:cookie @state)})
        bad-form (call :post "/name" {:headers (merge {"Cookie" (:cookie @state)} (origin)) :raw-form "name=%zz"})
        twice (call :get "/me" {:headers {"Cookie" (str "softland-sitting=sl1.from-another-server; " (:cookie @state))}})]
    (shown "Host 127.0.0.1:" (:status redirected) "Location" (get-in redirected [:headers "location"]))
    (shown "bad escape in the query:" bad-query
           "| in a form:" (:status bad-form) (pr-str (:edn bad-form)))
    (shown "the sitting's cookie twice:" (:status twice) (pr-str (:edn twice)))
    (holds "a request to 127.0.0.1:<port> is answered 308 to the same path and query at localhost:<port>"
           (and (= 308 (:status redirected))
                (= [(str *at* "/me?x=1")] (get-in redirected [:headers "location"]))))
    (holds "a bad % escape in a form's body is refused :form-field-not-read, status 400; one in the query is answered 400 by the JDK's server before any route"
           (and (= 400 (:status bad-form)) (= :form-field-not-read (:refuse (:edn bad-form)))
                (str/starts-with? (str bad-query) "HTTP/1.1 400")))
    (holds "a request that carries the sitting's cookie twice is refused :sitting-cookie-twice, status 400"
           (and (= 400 (:status twice)) (= {:refuse :sitting-cookie-twice} (:edn twice))))))

(deftest a-cookie-post-needs-its-origin
  (let [r (call :post "/name" {:headers {"Cookie" (:cookie @state)} :edn {:name "proof-person"}})]
    (shown "POST /name by cookie, no Origin:" (:status r) (pr-str (:edn r)))
    (holds "a POST by the sitting's cookie with no Origin is refused :no-origin"
           (and (= 403 (:status r)) (= {:refuse :no-origin} (:edn r))))))

(defn- as-her
  "Posts EDN to a route by her cookie and the runtime's origin."
  [path body]
  (call :post path {:headers (merge {"Cookie" (:cookie @state)} (origin)) :edn body}))

(defn- as-agent
  "Posts EDN to a route with the agent's bearer token, and its run in the
  Softland-Run header once it has one."
  [path body]
  (call :post path {:headers (cond-> {"Authorization" (str "Bearer " (:token @state))}
                               (:run @state) (assoc "Softland-Run" (str (:run @state))))
                    :edn body}))

(deftest an-agent-acts-with-its-token
  (let [{:keys [id own-layer]} (:me @state)
        made (as-her "/agent" {:kind :agent :name "proof-scribe" :description {:instructions "writes notes"}})
        agent (id-in (:ok (:edn made)) :agent :id)
        given (as-her "/right/give" {:layer own-layer :to agent :allows #{:note/*}})
        permission (id-in (:ok (:edn given)) :permission)
        tok (as-her "/agent/token" {:agent agent})
        token (id-in (:ok (:edn tok)) :token)
        _ (swap! state assoc :token token :agent agent :permission permission
                 :agent-session (:session (:ok (:edn tok))))
        run (as-agent "/run" {:engine {:provider "proof" :model "none" :effort :low}
                              :context {:given "a note to write"} :human-in-loop true
                              :cause "the HTTP proof"})
        run-id (id-in (:ok (:edn run)) :run :id)
        _ (swap! state assoc :run run-id)
        note (random-uuid)
        the-act {:version act/version :layer own-layer :permission permission
                 :facts [{:e note :k :note/text :v "a note by the agent"}]}
        acted (as-agent "/act" the-act)
        back (as-her "/read" {:asks [:place note :note/text] :layers [own-layer] :with #{:writer :permission}})
        place (:place (:ok (:edn back)))]
    (shown "agent made:" (:status made) (pr-str (:edn made)))
    (shown "right given:" (:status given) (pr-str (:edn given)))
    (shown "token made:" (:status tok) (pr-str (update (:edn tok) :ok #(if (map? %) (dissoc % :token) %))))
    (shown "run:" (:status run) (pr-str (:edn run)))
    (shown "act with the bearer token:" (:status acted) (pr-str (:edn acted)))
    (shown "read back by her:" (:status back) (pr-str (:edn back)))
    (holds "an agent is made, given a right in her own layer, and its token is made and shown once"
           (and (= 200 (:status made) (:status given) (:status tok)) (some? agent) (vector? permission)
                (str/starts-with? (str token) "sl1.")))
    (holds "the agent starts a run with its token"
           (and (= 200 (:status run)) (some? run-id)))
    (holds "an act with the agent's bearer token and its run is accepted, names the run as its session, and reads back as written by the agent, beneath her first permission in her own layer"
           (and (= 200 (:status acted))
                (= :accepted (get-in (:edn acted) [:ok :answer :answer]))
                (= run-id (get-in (:edn acted) [:ok :session]))
                (= "a note by the agent" (:value place))
                (= agent (id-in (:writer place) :who :id))
                (= [id own-layer own-layer] (nth (:permission place) 3 nil))))
    (swap! state assoc :note note :act the-act)))

(deftest the-token-ended
  (let [{:keys [agent-session act agent]} @state
        ended (as-her "/agent/token/end" {:session agent-session :identity agent})
        after (as-agent "/act" (assoc act :facts [{:e (random-uuid) :k :note/text :v "after the end"}]))]
    (shown "token ended:" (:status ended) (pr-str (:edn ended)) "| the next act:" (:status after) (pr-str (:edn after)))
    (holds "her end of the agent's token is taken, and the agent's next act is refused :session-ended, status 401"
           (and (= 200 (:status ended)) (= 401 (:status after)) (= :session-ended (:refuse (:edn after)))))))

(deftest a-token-asked-and-approved
  (let [agent (:agent @state)
        asked (call :post "/token/ask" {:edn {:agent agent}})
        request (get-in asked [:edn :ok :request])
        early (call :post "/token/collect" {:edn {:request request}})
        pending (call :get "/token/pending" {:headers {"Cookie" (:cookie @state)}})
        approved (as-her "/token/approve" {:request request})
        collected (call :post "/token/collect" {:edn {:request request}})
        token (get-in collected [:edn :ok :token])
        me (call :get "/me" {:headers {"Authorization" (str "Bearer " token)}})
        again (call :post "/token/collect" {:edn {:request request}})]
    (shown "asked:" (:status asked) (pr-str (:edn asked)) "| collected early:" (:status early) (pr-str (:edn early)))
    (shown "pending for her:" (:status pending) (pr-str (:edn pending)) "| approved:" (:status approved) (pr-str (:edn approved)))
    (shown "collected:" (:status collected) (pr-str (update (:edn collected) :ok #(if (map? %) (dissoc % :token) %)))
           "| /me with it:" (:status me) (pr-str (:edn me)) "| collected again:" (:status again) (pr-str (:edn again)))
    (holds "an agent asks for a token with no sign-in, and its request waits for her with its code; collected before she approves, it is refused :not-approved-yet"
           (and (= 200 (:status asked)) (uuid? request)
                (= :not-approved-yet (:refuse (:edn early)))
                (some #(and (= request (:request %)) (= (get-in asked [:edn :ok :code]) (:code %))) (:ok (:edn pending)))))
    (holds "she approves it in her sitting, the agent collects its token once, and /me with that token names the agent"
           (and (= 200 (:status approved) (:status collected) (:status me))
                (str/starts-with? (str token) "sl1.")
                (= agent (get-in me [:edn :ok :id])) (= :agent (get-in me [:edn :ok :kind]))
                (= :request-not-known (:refuse (:edn again)))))))

(deftest a-read-by-no-one
  (let [{:keys [id own-layer]} (:me @state)
        e (random-uuid)
        base-act {:version act/version :layer :base :permission [id :base :base [:system :base :base] :see-and-act]
                  :facts [{:e e :k :note/text :v "a note in the base"}]}
        mine-act {:version act/version :layer own-layer :permission [id own-layer own-layer]
                  :facts [{:e e :k :note/text :v "a note in her layer"}]}
        in-base (as-her "/act" base-act)
        in-mine (as-her "/act" mine-act)
        ;; The base is a microbatch layer: an accepted act is in a read once
        ;; the task that serves the read holds its rows. Read again, a
        ;; quarter second apart, for at most twenty seconds.
        anon (loop [n 0]
               (let [r (call :post "/read" {:edn {:asks [:place e :note/text] :layers [own-layer :base]}})]
                 (if (or (= "a note in the base" (get-in r [:edn :ok :place :value])) (>= n 80))
                   (assoc r :tries (inc n))
                   (do (Thread/sleep 250) (recur (inc n))))))
        her (as-her "/read" {:asks [:place e :note/text] :layers [own-layer :base]})
        a (:ok (:edn anon))]
    (shown "her acts in the base and her layer:" (:status in-base) (pr-str (:edn in-base)) "|" (:status in-mine) (pr-str (:edn in-mine)))
    (shown "read by no one, after" (:tries anon) "reads:" (:status anon) (pr-str (:edn anon)))
    (shown "read by her:" (:status her) (pr-str (:edn her)))
    (holds "her acts in the base, by her invitation's right, and in her own layer are accepted"
           (and (= :accepted (get-in (:edn in-base) [:ok :answer :answer]))
                (= :accepted (get-in (:edn in-mine) [:ok :answer :answer]))))
    (holds "a read with no cookie and no token sees the base's note, and leaves out her own layer as :may-not-see"
           (and (= 200 (:status anon))
                (= "a note in the base" (get-in a [:place :value])) (= :base (get-in a [:place :layer]))
                (some #(= {:layer own-layer :why :may-not-see} %) (:left-out a))))
    (holds "the same read by her sees her own layer's note, nearer than the base's"
           (= "a note in her layer" (get-in (:edn her) [:ok :place :value])))))

(defn test-ns-hook
  "Runs the checks in their order: each later one builds on what the
  earlier ones made (the invitation, her cookie, the agent's token)."
  []
  (the-admin-routes-need-the-admin-token)
  (an-invitation-by-the-command-line)
  (a-limit-by-the-command-line)
  (sign-in-refusals)
  (a-sign-in)
  (who-she-is)
  (what-a-request-must-be)
  (a-cookie-post-needs-its-origin)
  (an-agent-acts-with-its-token)
  (the-token-ended)
  (a-token-asked-and-approved)
  (a-read-by-no-one))

;; ------------------------------------------------------------------ main

(defn -main
  "Starts what `softland.runtime.main` starts, on a free port, with an
  admin token's file in a temporary folder and the proof's stand-in for
  Google; runs the checks; stops it all, removes the folder and exits:
  status 0 when each check holds."
  [& _]
  (let [dir (Files/createTempDirectory "softland-http-proof" (make-array FileAttribute 0))
        token-file (str (.resolve dir "runtime-admin-token"))
        started (main/start! {:client-id client-id :port 0 :admin-token-file token-file
                              :google-keys google-keys})]
    (if-not (:ok started)
      (do (println "FAILED the runtime started:" (pr-str started))
          (shutdown-agents)
          (System/exit 1))
      (let [{:keys [origin stop]} (:ok started)
            result (try
                     (binding [*at* origin]
                       (reset! state {:admin-token-file token-file})
                       (shown "runtime at" origin "| admin token file mode"
                              (str (Files/getPosixFilePermissions (.resolve dir "runtime-admin-token") (make-array java.nio.file.LinkOption 0))))
                       (run-tests 'softland.runtime.http-proof))
                     (finally
                       (stop)
                       (doseq [^File f (reverse (file-seq (.toFile dir)))] (.delete f))))
            {:keys [fail error]} result]
        (println)
        (println "THE PROOF, IN ONE PLACE")
        (run! println @shown-lines)
        (println (select-keys result [:test :pass :fail :error]))
        (shutdown-agents)
        (System/exit (if (zero? (+ fail error)) 0 1))))))
