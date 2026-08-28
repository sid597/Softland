(ns dev
  (:require [app.server.door.server-jetty :as jetty]
            [clojure.tools.logging :as log]))

(comment (-main))

(def config
  {:host "0.0.0.0"
   :port 8080})

(defn -main [& args]
  (log/info "[DEV] Starting server-only application"
            {:args (vec args) :config config})
  (def server (jetty/start-server! config))
  (log/info "[DEV] Jetty server started"
            {:host (:host config) :port (:port config)})
  (comment (.stop server)))
