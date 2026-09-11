(ns softland.inland.server
  "Local HTTP/Electric host for the isolated product runtime.
   Takes a running Rama cluster and compiled assets; gives a loopback HTTP server
   and native Electric WebSocket sessions. Owns Jetty and starts process-scoped store
   and resident resources. Browser-owned computation begins at app/Main. Optional
   test controls inspect isolated state and inject faults; normal serving disables
   them. The health route reports HTTP reachability, not dependency health."
  (:require [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-ring-adapter3 :as electric-ring]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as response]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [softland.inland.app :as app]
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

(defn handler
  "Ring request → static asset, health response, enabled test control or 404.
   Test reads are restricted to named PStates and short paths. This handler serves
   no application-state transport; Electric owns reactive client/server traffic."
  [request]
  (let [test? (= "1" (System/getenv "INLAND_TEST_CONTROLS"))]
    (cond
      (= "/health" (:uri request)) (json-response {:ready true :build "softland-in-softland"})
      (and test? (= "/__test/metrics" (:uri request))) (json-response (store/metrics-snapshot))
      (and test? (= :post (:request-method request)) (= "/__test/read" (:uri request)))
      (let [{:keys [kind path]} (json/read-str (slurp (:body request)) :key-fn keyword)
            kind (keyword kind)]
        (if (and (#{:rows :versions :index :decisions} kind) (vector? path) (<= 1 (count path) 5))
          (json-response (store/read-one kind path))
          {:status 400 :body "Invalid test read"}))
      (and test? (= :post (:request-method request)) (= "/__test/fault" (:uri request)))
      (let [mode (:mode (json/read-str (slurp (:body request)) :key-fn keyword))]
        (reset! resident/fault (case mode "failure" :failure "uncertain" :uncertain nil))
        (json-response {:mode mode}))
      (and test? (= :post (:request-method request)) (= "/__test/hold" (:uri request)))
      (do (when-not @store/test-hold (reset! store/test-hold (promise)))
          (json-response {:held true}))
      (and test? (= :post (:request-method request)) (= "/__test/release" (:uri request)))
      (do (when-let [hold @store/test-hold] (reset! store/test-hold nil) (deliver hold true))
          (json-response {:held false}))
      :else
      (or (when-let [file (response/file-response (if (= "/" (:uri request)) "index.html" (subs (:uri request) 1))
                                                {:root "target/inland/public"})]
            (if (= "/" (:uri request)) (response/content-type file "text/html; charset=utf-8") file))
          {:status 404 :body "Not found"}))))

(defn -main
  "Launch → connect store, seed default workspace, start resident and serve 8127.
   Requires deployed isolated Rama and browser assets; joins Jetty until shutdown.
   Electric boots app/Main per connection, with test controls governed by environment."
  [& _]
  (store/connect!)
  (store/ensure-workspace! "workbench")
  (resident/start!)
  (let [server (jetty/run-jetty
                 (electric-ring/wrap-electric-websocket
                   (wrap-content-type handler)
                   (fn [_]
                     (try
                       (e/boot-server {} app/Main)
                       (catch Throwable error
                         (.printStackTrace error)
                         (throw error)))))
                 {:host "127.0.0.1" :port 8127 :join? false :configurator configure-websocket!})]
    (println "Softland in Softland: http://localhost:8127")
    (.join server)))
