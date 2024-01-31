(ns prod
  (:gen-class)
  (:require
            clojure.string
            app.electric-server-java8-jetty9
            boot))

(def electric-server-config
  {:host "0.0.0.0", :port 8080, :resources-path "public"})

(defn -main [& args] ; run with `clj -M -m prod`
  (when (clojure.string/blank? (System/getProperty "ELECTRIC_USER_VERSION"))
     (throw (ex-info "ELECTRIC_USER_VERSION jvm property must be set in prod" {}))
   (app.electric-server-java8-jetty9/start-server! boot/with-ring-request electric-server-config)))

; On CLJS side we reuse src/user.cljs for prod entrypoint