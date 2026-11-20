(ns softland.browser.host
  "The host of the browser's program: a Ring handler in front of the face
  over HTTP (`softland.server.http`), and the setting of Jetty's
  WebSocket. It requires Electric's Ring adapter, the browser's program,
  the server's credential, and the face over HTTP for the one referrer
  policy of the server's pages.

  What `wrap` adds in front of the handler that it is given:

    a WebSocket upgrade  refused unless its `Origin` is one of the
                         server's own; otherwise Electric's server program
                         is booted for it, with the seam and the token of
                         the request's cookie
    GET /app             the page that loads the browser's program
    GET /js/...          the files of the built program, from `js-root`
    anything else        the handler given

  Why the upgrade checks `Origin`. A browser adds its cookie to a socket
  that any page opens, of any site, and Electric's adapter checks no
  origin. Without the check, a page of another site could open a socket
  that stands for her and drive the program. This is the third rule of
  the browser's program (`softland.browser.app`), and it is kept here.

  State. None. `wrap` closes over the seam and the origins; each socket's
  program owns its own state, and Electric ends it when the socket closes."
  (:require [clojure.string :as str]
            [hyperfiddle.electric-ring-adapter3 :as electric-ring]
            [ring.websocket :as ws]
            [softland.browser.app :as app]
            [softland.server.http :as http]
            [softland.server.session.credential :as credential])
  (:import [java.io File]
           [java.nio.file Path]
           [java.time Duration]
           [org.eclipse.jetty.server Server]
           [org.eclipse.jetty.websocket.server.config JettyWebSocketServletContainerInitializer
            JettyWebSocketServletContainerInitializer$Configurator]))

(def js-root
  "The folder, from the repository root, that the build of the browser's
  program writes (`shadow-cljs.edn`, the build `:softland-browser`). The
  files under `/js/` are served from it and from nowhere else."
  "target/softland-browser/public/js")

(def ^:private page-html
  "The page that loads the browser's program: one element into which the
  program draws, and the script."
  (str "<!doctype html><html><head><meta charset=\"utf-8\">"
       "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
       "<title>Softland</title></head><body>"
       "<div id=\"softland\"></div>"
       "<script src=\"/js/main.js\"></script>"
       "</body></html>"))

(defn- app-page
  "GET /app. Gives the page that loads the program. It is never framed by
  another site and never kept by a browser."
  []
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"
             "Cache-Control" "no-store"
             "X-Frame-Options" "DENY"
             "Content-Security-Policy" "frame-ancestors 'none'"
             "Referrer-Policy" http/referrer-policy}
   :body page-html})

(defn- content-type-of
  "Takes a file's name. Gives the content type to serve it with."
  [^String file-name]
  (cond
    (str/ends-with? file-name ".js") "text/javascript; charset=utf-8"
    (str/ends-with? file-name ".map") "application/json; charset=utf-8"
    (str/ends-with? file-name ".json") "application/json; charset=utf-8"
    :else "application/octet-stream"))

(defn- js-file
  "GET /js/... Takes the request's path. Gives the response of the file at
  that path under `js-root`, or nil when there is no such file, or when the
  path would leave the folder: the path is resolved and normalized, and
  must still begin with the folder."
  [^String uri]
  (let [root (.normalize (.toAbsolutePath (.toPath (File. ^String js-root))))
        ^Path wanted (.normalize (.resolve root (subs uri (count "/js/"))))
        file (.toFile wanted)]
    (when (and (.startsWith wanted root)
               (not= wanted root)
               (.isFile file))
      {:status 200
       :headers {"Content-Type" (content-type-of (.getName file))
                 "Cache-Control" "no-store"}
       :body file})))

(defn- refused-socket
  "Gives the response that accepts an upgrade and closes the socket at
  once, with the code 1008 and a reason: Electric's way to refuse a socket
  cleanly."
  []
  {::ws/listener (electric-ring/reject-websocket-handler 1008 "This origin is not the server's.")})

(defn wrap
  "Puts the browser's program in front of a Ring handler. Takes the handler
  and a map: `:the-seam`, an opened seam, and `:origins`, the set of this
  server's own origins as a browser names them. Gives a Ring handler, as
  the namespace docstring says.

  The upgrade is tested first, and its `Origin` before anything else. An
  accepted upgrade boots the program with the seam and the token of the
  cookie `softland`, or nil when the request has none; a token that stands
  for nobody is the program's to show. The handler given sees no upgrade."
  [handler {:keys [the-seam origins]}]
  (let [electric (electric-ring/wrap-electric-websocket
                  handler
                  (fn [request]
                    (app/electric-boot the-seam (credential/of-cookie (get-in request [:headers "cookie"])))))]
    (fn [request]
      (let [method (:request-method request)
            ^String uri (:uri request)]
        (cond
          (ws/upgrade-request? request)
          (if (contains? origins (get-in request [:headers "origin"]))
            (electric request)
            (refused-socket))

          (and (= :get method) (= "/app" uri))
          (app-page)

          (and (= :get method) (str/starts-with? uri "/js/"))
          (or (js-file uri)
              {:status 404 :headers {"Content-Type" "text/plain; charset=utf-8"} :body "Not found\n"})

          :else
          (handler request))))))

(defn configure-websocket!
  "The configurator of Jetty for the browser's program: it is given the
  server before it starts, and raises the WebSocket's text and binary
  message limits to 1 MiB and its idle timeout to five minutes. Electric's
  first frame of a program can pass Jetty's default of 64 KB; a measured
  one was 69 KB. It changes the transport's limits, not what the program
  reads. Gives nil."
  [^Server server]
  (JettyWebSocketServletContainerInitializer/configure
   (.getHandler server)
   (reify JettyWebSocketServletContainerInitializer$Configurator
     (accept [_ _ container]
       (.setMaxTextMessageSize container 1048576)
       (.setMaxBinaryMessageSize container 1048576)
       (.setIdleTimeout container (Duration/ofMinutes 5)))))
  nil)
