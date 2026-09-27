(ns softland.inland.boot
  "Browser entry for the inland Shadow module.
   Takes page/module startup; gives an Electric client rooted at app/Main. Holds one
   cancellation function in the page so a repeated start replaces the previous client.
   It borrows Electric transport and reports termination/errors to the console."
  (:require [hyperfiddle.electric3 :as e]
            [softland.inland.app :as app]))

(defonce cancel nil)
(defn ^:export start!
  "No arguments → cancel any prior client, then boot and retain a new one.
   Exported for the compiled browser entry; only page-local lifetime is changed."
  []
  (when cancel (cancel))
  ;; both peers boot the program in app/electric-boot (its person is the server's)
  (set! cancel ((app/electric-boot nil)
               #(js/console.info "Electric stopped")
               #(js/console.error "Electric failed" %))))
