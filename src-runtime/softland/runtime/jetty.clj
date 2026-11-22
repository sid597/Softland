(ns softland.runtime.jetty
  "What carries the runtime's faces: one Jetty server, which serves a Ring
  handler for the pages and upgrades one path to a WebSocket for the line.
  It requires Jetty 11, Ring's servlet helpers and the line.

  Why it is written here and not taken from Ring's adapter. The adapter
  upgrades a request to a WebSocket and hands a listener a socket that can
  send and close, and nothing more. The line needs two things of the
  connection itself: a bound on the bytes of one frame that depends on who
  connected, set before a frame is taken into memory; and the request, to
  make the caller before the upgrade. So this namespace speaks to Jetty's
  own API for that one path, and uses Ring's helpers for everything else.

  The calls:

    serve   a map -> Jetty's server, started and not joined
    stop!   the server -> nil

  How a connection to the line opens. The request is read as a Ring
  request map and handed to the opener's `:accept`, which gives one of:

    {:caller caller :max-frame-bytes n}   upgrade, as this caller
    a Ring response                       no upgrade: the response is sent

  So a credential that stands for nobody gets an answer over HTTP, with a
  status, and no socket. After the upgrade each text frame goes to
  `softland.runtime.line/text!`, which returns at once; a binary frame
  ends the connection.

  A quiet connection. The server sends a ping on each open connection
  every `ping-ms`, so that a thing in front does not close a quiet line,
  and a system that went away is noticed: a connection that is silent for
  `idle-ms`, pongs included, is closed by Jetty.

  Threads. Each request of a page runs on a virtual thread. A callback of a
  WebSocket runs on a thread of Jetty's and does nothing but hand on.

  State. The server, and the set of open connections for the ping, in the
  memory of the process."
  (:require [ring.util.jakarta.servlet :as servlet]
            [softland.runtime.line :as line])
  (:import [jakarta.servlet.http HttpServletRequest HttpServletResponse]
           [java.nio ByteBuffer]
           [java.time Duration]
           [java.util.concurrent ConcurrentHashMap Executors ScheduledExecutorService
            ThreadFactory TimeUnit]
           [org.eclipse.jetty.server HttpConfiguration HttpConnectionFactory Request Server
            ServerConnector]
           [org.eclipse.jetty.servlet ServletContextHandler ServletHandler]
           [org.eclipse.jetty.util.thread QueuedThreadPool]
           [org.eclipse.jetty.websocket.api Session WebSocketListener WriteCallback]
           [org.eclipse.jetty.websocket.server JettyWebSocketCreator JettyWebSocketServerContainer]
           [org.eclipse.jetty.websocket.server.config JettyWebSocketServletContainerInitializer]))

(def ^:private noop-write-callback
  "Non-blocking callback for Jetty WebSocket pings."
  (reify WriteCallback
    (writeFailed [_ _] nil)
    (writeSuccess [_] nil)))

(def ping-ms
  "How often the server pings each open connection of the line. A number of
  the build, kept and not decided."
  20000)

(def idle-ms
  "After how long with nothing received, a pong included, a connection of
  the line is closed. A number of the build, kept and not decided."
  60000)

(defn- listener
  "Makes Jetty's listener for one connection of the line. Takes the seam,
  the caller, the bound on one frame's bytes, and the map of open
  sessions, from a session to its connection. At the connect it sets the
  bounds on the session, opens the line's connection with the session's
  send and close, and keeps both for the ping."
  [the-seam caller max-frame-bytes ^ConcurrentHashMap sessions]
  (let [connection (volatile! nil)
        session-of (volatile! nil)]
    (reify WebSocketListener
      (onWebSocketConnect [_ session]
        (let [^Session session session]
          (.setMaxTextMessageSize session (long max-frame-bytes))
          (.setMaxBinaryMessageSize session 1)
          (.setIdleTimeout session (Duration/ofMillis idle-ms))
          (vreset! session-of session)
          (let [conn (line/open the-seam caller
                                {:send! (fn [^String text] (.sendString (.getRemote session) text))
                                 :close! (fn [status reason]
                                           (.remove sessions session)
                                           (.close session (int status) (str reason)))})]
            (vreset! connection conn)
            (.put sessions session conn)
            (when-not (line/open? conn)
              (.remove sessions session)))))
      (onWebSocketText [_ text]
        (when-let [connection @connection]
          (line/text! connection text)))
      (onWebSocketBinary [_ _payload _offset _length]
        (when-let [^Session session @session-of]
          (.close session 1003 "the line carries text")))
      (onWebSocketClose [_ _status _reason]
        (when-let [session @session-of] (.remove sessions session))
        (when-let [connection @connection]
          (line/closed! connection)))
      (onWebSocketError [_ _throwable]
        (when-let [session @session-of] (.remove sessions session))
        (when-let [connection @connection]
          (line/closed! connection))))))

(defn- servlet-handler
  "Makes the one handler of the server. Takes the seam, the Ring handler of
  the pages, the path of the line, the line's `:accept`, and the map of
  open sessions. A request to the line's path that asks for an upgrade is
  accepted or answered as the namespace docstring says; every other
  request goes to the Ring handler."
  ^ServletHandler [the-seam handler line-path accept sessions]
  (proxy [ServletHandler] []
    (doHandle [_ ^Request base-request ^HttpServletRequest request ^HttpServletResponse response]
      (try
        (let [request-map (servlet/build-request-map request)]
          (if (and (= line-path (:uri request-map))
                   (= "websocket" (some-> (get-in request-map [:headers "upgrade"]) .toLowerCase)))
            (let [accepted (accept request-map)]
              (if-let [caller (:caller accepted)]
                (let [container (JettyWebSocketServerContainer/getContainer (.getServletContext request))
                      creator (reify JettyWebSocketCreator
                                (createWebSocket [_ _req _resp]
                                  (listener the-seam caller (:max-frame-bytes accepted) sessions)))]
                  (.upgrade container creator request response))
                (servlet/update-servlet-response response accepted)))
            (servlet/update-servlet-response response (handler request-map))))
        (finally
          (.setHandled base-request true))))))

(defn serve
  "Starts the server. Takes a map:

    :the-seam   an opened seam
    :handler    the Ring handler of the pages (`softland.runtime.http`)
    :line-path  the path of the line, such as \"/line\"
    :accept     a function from a Ring request map to
                {:caller :max-frame-bytes} or a Ring response
    :host       the address to listen on; \"127.0.0.1\" when left out
    :port       the port; 0 for one that the system picks

  Gives a map: `:server`, Jetty's server, started and not joined; `:port`,
  the port that it listens on; `:pinger`, the timer of the ping."
  [{:keys [the-seam handler line-path accept host port]}]
  (let [sessions (ConcurrentHashMap.)
        pool (doto (QueuedThreadPool.)
               (.setVirtualThreadsExecutor (Executors/newVirtualThreadPerTaskExecutor)))
        server (Server. pool)
        connector (doto (ServerConnector. server
                                          (into-array org.eclipse.jetty.server.ConnectionFactory
                                                      [(HttpConnectionFactory.
                                                        (doto (HttpConfiguration.)
                                                          (.setSendServerVersion false)))]))
                    (.setHost (or host "127.0.0.1"))
                    (.setPort (int (or port 0)))
                    (.setIdleTimeout 200000))
        context (doto (ServletContextHandler.)
                  (.setServletHandler (servlet-handler the-seam handler line-path accept sessions))
                  (.setAllowNullPathInfo true)
                  (JettyWebSocketServletContainerInitializer/configure nil))
        pinger (Executors/newSingleThreadScheduledExecutor
                (reify ThreadFactory
                  (newThread [_ runnable]
                    (doto (Thread. runnable "softland-line-ping") (.setDaemon true)))))]
    (.addConnector server connector)
    (.setHandler server context)
    (.start server)
    (.scheduleAtFixedRate ^ScheduledExecutorService pinger
                          (fn []
                            (doseq [^java.util.Map$Entry entry (.entrySet sessions)]
                              (let [^Session session (.getKey entry)
                                    conn (.getValue entry)]
                                ;; Send non-blocking pings directly on the one ping thread
                                ;; without taking the connection's send lock, so a stalled
                                ;; reader never blocks pings or pins virtual-thread carriers.
                                (when (and (line/open? conn) (.isOpen session))
                                  (try (.sendPing (.getRemote session) (ByteBuffer/allocate 0) noop-write-callback)
                                       (catch Throwable _ nil))))))
                          ping-ms ping-ms TimeUnit/MILLISECONDS)
    {:server server :port (.getLocalPort connector) :pinger pinger}))

(defn stop!
  "Stops what `serve` gave: the ping's timer and the server."
  [{:keys [^Server server ^ScheduledExecutorService pinger]}]
  (.shutdownNow pinger)
  (.stop server)
  nil)
