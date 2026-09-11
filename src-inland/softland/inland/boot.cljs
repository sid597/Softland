(ns softland.inland.boot
  "Browser boot. Takes page lifetime. Gives the Electric client. Holds cancel."
  (:require [hyperfiddle.electric3 :as e]
            [softland.inland.app :as app]))

(defonce cancel nil)
(defn ^:export start! []
  (when cancel (cancel))
  (set! cancel ((e/boot-client {} app/Main)
               #(js/console.info "Electric stopped")
               #(js/console.error "Electric failed" %))))
