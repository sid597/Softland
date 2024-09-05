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

     (defn -main [& args]
       (log/info "Starting Electric compiler and server...")

       (shadow-server/start!)
       (shadow/watch :dev)
       (comment (shadow-server/stop!))

       (def server (jetty/start-server!
                     (fn [ring-request]
                       (e/boot-server {} app.electric-flow/main ring-request))
                     config))

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