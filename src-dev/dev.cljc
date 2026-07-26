(ns dev
  (:require
    app.electric-flow
    [hyperfiddle.electric3 :as e]
    #?(:cljs hyperfiddle.electric-client3)
    #?(:clj [app.server-jetty :as jetty])
    #?(:clj [shadow.cljs.devtools.api :as shadow])
    #?(:clj [shadow.cljs.devtools.server :as shadow-server])
    #?(:clj [clojure.tools.logging :as log])))

(comment (-main)) ; repl entrypoint

#?(:clj ;; Server Entrypoint
   (do
     (def config
       {:host "0.0.0.0"
        :port 8080
        :resources-path "public/"
        :manifest-path ; contains Electric compiled program's version so client and server stays in sync
        "public/js/manifest.edn"})

     (defn- wrap-request-logging [handler]
       (fn [ring-request]
         (let [started-ns (System/nanoTime)
               request-summary {:method (some-> (:request-method ring-request) name)
                                :uri (:uri ring-request)
                                :query (:query-string ring-request)
                                :websocket? (boolean (:websocket? ring-request))}]
           (log/info "[DEV/REQ]" request-summary)
           (try
             (let [response (handler ring-request)
                   elapsed-ms (/ (double (- (System/nanoTime) started-ns)) 1000000.0)]
               (log/info "[DEV/RESP]"
                         (assoc request-summary
                                :status (:status response)
                                :elapsed-ms (format "%.2f" elapsed-ms)))
               response)
             (catch Throwable t
               (log/error t "[DEV/ERR]" request-summary)
               (throw t))))))

     (defn -main [& args]
       (log/info "[DEV] Starting Electric compiler and server..."
                 {:args (vec args)
                  :config config})

       (if (= "1" (System/getenv "LAND_PINNED"))
         ;; Pinned wear: serve the client assets already compiled at this HEAD
         ;; (clj -M:dev -m shadow.cljs.devtools.cli compile dev) — no watch, so
         ;; concurrent sessions editing the tree never hot-swap this session.
         (log/info "[DEV] LAND_PINNED=1 — no shadow watch; serving compiled client as-is")
         (do
           (shadow-server/start!)
           (log/info "[DEV] shadow-cljs server started")
           (shadow/watch :dev)
           (log/info "[DEV] shadow-cljs watch started for build :dev")))
       (comment (shadow-server/stop!))

       (def server (jetty/start-server!
                     (wrap-request-logging
                       (fn [ring-request]
                         (e/boot-server {} app.electric-flow/main ring-request)))
                     config))
       (log/info "[DEV] Jetty server started" {:host (:host config)
                                               :port (:port config)
                                               :resources-path (:resources-path config)
                                               :manifest-path (:manifest-path config)})

       (comment (.stop server)))))


#?(:cljs ;; Client Entrypoint
   (do
     (defonce reactor nil)

     (defn ^:dev/after-load ^:export start! []
       (set! reactor ((e/boot-client {} app.electric-flow/main nil)
                      #(js/console.log "Reactor success:" %)
                      #(js/console.error "Reactor failure:" %))))

     (defn ^:dev/before-load stop! []
       (when reactor (reactor)) ; stop the reactor
       (set! reactor nil))))
