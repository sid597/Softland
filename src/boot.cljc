(ns boot
  (:require
    app.electric-flow
    [hyperfiddle.electric :as e]))

#?(:clj (defn with-ring-request [ring-request] (e/boot-server {} app.electric-flow/main ring-request)))
#?(:cljs (def client (e/boot-client {} app.electric-flow/main nil)))
