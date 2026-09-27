(ns softland.inland.server
  "Local HTTP/Electric host for the product runtime on the rig's store, with login.
   Takes a running Rama cluster with the rig's module and compiled assets; gives a
   loopback HTTP server, a login page, and native Electric WebSocket sessions, each
   for the person who logged in. Owns Jetty and starts process-scoped store and
   resident resources. Browser-owned computation begins at app/Main.

   Login is the host's (people): a passphrase gives the browser a token in an
   HttpOnly cookie; the page and its WebSocket are refused without one; Electric
   boots Main with the token's person, and every act the page writes is that
   person's, under their own session's permission. Optional test controls
   inspect isolated state; normal serving disables them. The health route reports
   HTTP reachability, not dependency health."
  (:require [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-ring-adapter3 :as electric-ring]
            [ring.adapter.jetty :as jetty]
            [ring.util.codec :as codec]
            [ring.websocket :as ws]
            [ring.util.response :as response]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [softland.inland.app :as app]
            [softland.inland.people :as people]
            [softland.inland.store :as store]
            [softland.inland.resident :as resident]))

(defn configure-websocket!
  "Jetty server → native WebSocket limits of 1 MiB and five-minute idle timeout.
   A measured 69 KB Electric startup frame exceeded the default 64 KB limit."
  [server]
  (org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer/configure
    (.getHandler ^org.eclipse.jetty.server.Server server)
    (reify org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer$Configurator
      (accept [_ _ container]
        ;; A measured 69KB Electric startup frame exceeds Jetty's 64KB default.
        ;; This changes the native transport limit, not the application's reads.
        (.setMaxTextMessageSize container 1048576)
        (.setMaxBinaryMessageSize container 1048576)
        (.setIdleTimeout container (java.time.Duration/ofMinutes 5))))))

(defn json-response
  "JSON-encodable value → HTTP 200 JSON response with caching disabled.
   Serialization errors propagate; this helper does not establish readiness."
  [value]
  {:status 200 :headers {"Content-Type" "application/json" "Cache-Control" "no-store"}
   :body (json/write-str value)})

;; ------------------------------------------------------------ login

(def cookie-name "inland")

(defn token-of
  "Ring request → the login token its cookie carries, or nil."
  [request]
  (some (fn [part]
          (let [[k v] (str/split (str/trim part) #"=" 2)]
            (when (= cookie-name k) v)))
        (str/split (or (get-in request [:headers "cookie"]) "") #";")))

(defn person-of
  "Ring request → the person its token was given to, or nil."
  [request]
  (some-> (token-of request) people/person-of-token))

(defn login-page
  "The login form, with a message when a try failed. Plain HTML: the screen
   itself is Softland-rendered once logged in."
  [message]
  {:status 200 :headers {"Content-Type" "text/html; charset=utf-8" "Cache-Control" "no-store"}
   :body (str "<!doctype html><html><head><meta charset=\"utf-8\"><title>Softland — log in</title>"
              "<style>body{font:16px system-ui;background:#16141f;color:#eeeef6;display:grid;place-items:center;height:100vh;margin:0}"
              "form{display:grid;gap:12px;width:280px}input,button{font:inherit;padding:8px;border-radius:6px;border:1px solid #555;background:#221f2e;color:inherit}"
              "p{color:#f08070;margin:0}</style></head><body><form method=\"post\" action=\"/login\">"
              "<h2 style=\"margin:0\">Softland</h2>"
              (when message (str "<p>" message "</p>"))
              "<input name=\"name\" placeholder=\"name\" autocomplete=\"username\" autofocus>"
              "<input name=\"pass\" type=\"password\" placeholder=\"passphrase\" autocomplete=\"current-password\">"
              "<button>Log in</button></form></body></html>")})

(defn redirect [to & [cookie]]
  (cond-> {:status 303 :headers {"Location" to "Cache-Control" "no-store"} :body ""}
    cookie (assoc-in [:headers "Set-Cookie"] cookie)))

(defn login!
  "POST /login → a token cookie and the page, or the form again."
  [request]
  (let [form (codec/form-decode (slurp (:body request)) "UTF-8")
        form (if (map? form) form {})
        token (people/login! (get form "name") (get form "pass"))]
    (if token
      (redirect "/" (str cookie-name "=" token "; Path=/; HttpOnly; SameSite=Strict"))
      (login-page "That name and passphrase do not match."))))

;; ------------------------------------------------------------ routes

(defn handler
  "Ring request → login, static asset, health response, enabled test control or 404.
   The page itself needs a login; assets do not. Test reads are the store's own
   view and never shown to a person. Electric owns reactive client/server traffic."
  [request]
  (let [test? (= "1" (System/getenv "INLAND_TEST_CONTROLS"))
        uri (:uri request)]
    (cond
      (= "/health" uri) (json-response {:ready true :build "softland-in-softland"})
      (and (= "/login" uri) (= :post (:request-method request))) (login! request)
      (= "/login" uri) (login-page nil)
      (= "/logout" uri) (do (some-> (token-of request) people/logout!)
                            (redirect "/login" (str cookie-name "=; Path=/; Max-Age=0")))
      (and (= "/" uri) (nil? (person-of request))) (redirect "/login")
      (and test? (= "/__test/metrics" uri)) (json-response (store/metrics-snapshot))
      (and test? (= :post (:request-method request)) (= "/__test/read" uri))
      (let [{:keys [kind path]} (json/read-str (slurp (:body request)) :key-fn keyword)
            kind (keyword kind)]
        (if (and (#{:rows :versions :index} kind) (vector? path) (<= 1 (count path) 5))
          (json-response (store/read-one kind path))
          {:status 400 :body "Invalid test read"}))
      (and test? (= :post (:request-method request)) (= "/__test/fault" uri))
      (let [mode (:mode (json/read-str (slurp (:body request)) :key-fn keyword))]
        (reset! resident/fault (case mode "failure" :failure "uncertain" :uncertain nil))
        (json-response {:mode mode}))
      :else
      (or (when-let [file (response/file-response (if (= "/" uri) "index.html" (subs uri 1))
                                                {:root "target/inland/public"})]
            (if (= "/" uri) (response/content-type file "text/html; charset=utf-8") file))
          {:status 404 :body "Not found"}))))

(defn wrap-login-guard
  "A WebSocket upgrade without a login token is refused: Electric boots only
   for a known person."
  [next-handler]
  (fn [request]
    (if (and (ws/upgrade-request? request) (nil? (person-of request)))
      {:ring.websocket/listener (electric-ring/reject-websocket-handler 1008 "Log in first.")}
      (next-handler request))))

(defn -main
  "Launch → connect the store (its first facts when not in), start the resident,
   make the login file, and serve 8127 on loopback. Requires the rig's module
   deployed and browser assets built; joins Jetty until shutdown. Electric boots
   app/Main per connection for the connection's person."
  [& _]
  (store/connect!)
  (people/ensure-passphrases!)
  (resident/start!)
  (let [server (jetty/run-jetty
                 (-> (wrap-content-type handler)
                     (electric-ring/wrap-electric-websocket
                       (fn [ring-request]
                         (let [person (person-of ring-request)]
                           (try
                             (e/boot-server {} app/Main person)
                             (catch Throwable error
                               (.printStackTrace error)
                               (throw error))))))
                     (wrap-login-guard))
                 {:host "127.0.0.1" :port 8127 :join? false :configurator configure-websocket!})]
    (println "Softland in Softland: http://localhost:8127 (log in with a passphrase from .inland-runtime/passphrases.txt)")
    (.join server)))
