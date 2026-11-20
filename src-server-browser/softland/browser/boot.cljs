(ns softland.browser.boot
  "The browser's entry of the program: `start!` boots the client's half of
  `softland.browser.app` into the page that `/app` serves. It makes the
  page's state once, so that the acts under way outlast a socket that
  closes and opens again, and it holds the function that stops the client,
  so that a second start replaces the first."
  (:require [softland.browser.app :as app]))

(defonce ^{:doc "The page's state (`softland.browser.app/page-state`), made once for
  each load of the page."}
  page
  (app/page-state))

(defonce ^{:doc "The function that stops the running client, or nil before the first
  start."}
  stop
  nil)

(defn ^:export start!
  "Takes nothing. Stops a client that runs, then boots a new one with the
  page's state, and keeps its stop. A client that ends or fails says so on
  the console. Gives nil."
  []
  (when stop (stop))
  (set! stop ((app/electric-boot page)
              (fn [_] (js/console.info "The program stopped."))
              (fn [error] (js/console.error "The program failed." error))))
  nil)
