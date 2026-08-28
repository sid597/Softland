(ns prod
  (:require [app.server.door.server-jetty :as jetty]
            [clojure.tools.logging :as log]))

(def config
  {:host "0.0.0.0"
   :port 8080})

(defn -main [& {:strs [] :as args}]
  (log/info "[PROD] Starting server-only application"
            {:args args :config config})
  (jetty/start-server! config))
