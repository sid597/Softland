(ns softland.runtime.jetty
  "What carries the runtime's faces: one Jetty server, which serves a Ring
  handler for the pages and upgrades one path to a WebSocket for the line.
  Jetty 11.

  Why it is written here and not taken from Ring's adapter. The adapter
  upgrades a request to a WebSocket and hands a listener a socket that can
  send and close, and nothing more. The line needs two things of the
  connection itself: a bound on the bytes of one frame that depends on who
  connected, set before a frame is taken into memory; and the request, to
  make the caller before the upgrade. So this namespace speaks to Jetty's
  own API for that one path, and uses Ring's helpers for everything else.

  How a connection to the line opens. The request is read as a Ring request
  map and handed to the opener's `:accept`, which gives
  {:caller caller :max-frame-bytes n}, to upgrade as this caller, or a Ring
  response, which is sent with no upgrade. So a credential that stands for
  nobody gets an answer over HTTP, with a status, and no socket. A binary
  frame ends the connection.

  A quiet connection. The server sends a ping on each open connection every
  `ping-ms`, so that a thing in front does not close a quiet line. A
  connection from which nothing was heard for `idle-ms`, a frame or a pong,
  is closed. This namespace keeps when it last heard from each connection
  and closes it: Jetty's own idle time counts what the server writes, its
  pings too, so it would keep a system open that never answers.

  A system that does not read. One write of a frame waits at most
  `send-ms`; past that the write is a failure, and the line ends the
  connection, which is then dropped without a closing handshake: a close
  frame could not reach a system that does not read either.

  Threads. Each request of a page runs on a virtual thread. A callback of a
  WebSocket runs on a thread of Jetty's and does nothing but hand on."
  (:require [ring.util.jakarta.servlet :as servlet]
            [softland.runtime.line :as line])
  (:import [jakarta.servlet.http HttpServletRequest HttpServletResponse]
           [java.nio ByteBuffer]
           [java.time Duration]
           [java.util.concurrent CompletableFuture ConcurrentHashMap Executors ScheduledExecutorService
            ThreadFactory TimeUnit TimeoutException]
           [java.util.concurrent.atomic AtomicLong]
           [org.eclipse.jetty.server HttpConfiguration HttpConnectionFactory Request Server
            ServerConnector]
           [org.eclipse.jetty.servlet ServletContextHandler ServletHandler]
           [org.eclipse.jetty.util.thread QueuedThreadPool]
           [org.eclipse.jetty.websocket.api Session WebSocketListener WebSocketPingPongListener
            WriteCallback]
           [org.eclipse.jetty.websocket.server JettyWebSocketCreator JettyWebSocketServerContainer]
           [org.eclipse.jetty.websocket.server.config JettyWebSocketServletContainerInitializer]))

(def ^:private noop-write-callback
  "Non-blocking callback for Jetty WebSocket pings."
  (reify WriteCallback
    (writeFailed [_ _] nil)
    (writeSuccess [_] nil)))

(def ping-ms
  "How often the server pings each open connection of the line."
  20000)

(def idle-ms
  "After how long with nothing received, a pong included, a connection of
  the line is closed."
  60000)

(def send-ms
  "How long one write of a frame to a connection of the line may wait for
  the system to read it before the write is a failure."
  30000)

(defn- sent!
  "Throws when the write failed or waited longer than `send-ms`. A thread
  that waits is parked, never pinned."
  [^Session session ^String text]
  (let [done (CompletableFuture.)]
    (.sendString (.getRemote session) text
                 (reify WriteCallback
                   (writeFailed [_ x] (.completeExceptionally done x))
                   (writeSuccess [_] (.complete done nil))))
    (try (.get done send-ms TimeUnit/MILLISECONDS)
         (catch TimeoutException _
           (throw (ex-info "The system does not read: a write waited too long." {:send-ms send-ms}))))
    nil))

(defn- closed-by!
  "1006, which no close frame may carry, says that the system is not to be
  waited for: the session is dropped without a closing handshake."
  [^Session session status reason]
  (if (= 1006 (int status))
    (.disconnect session)
    (.close session (int status) (str reason))))

(defn- listener
  "`sessions` is the map of open sessions, from a session to
  `[connection heard]`, where `heard` holds when the connection was last
  heard from (`System/nanoTime`). Every frame that comes, a ping or a pong
  too, counts as heard."
  [the-seam caller max-frame-bytes ^ConcurrentHashMap sessions]
  (let [connection (volatile! nil)
        session-of (volatile! nil)
        heard (AtomicLong. (System/nanoTime))
        heard! (fn [] (.set heard (System/nanoTime)))]
    (reify
      WebSocketListener
      (onWebSocketConnect [_ session]
        (let [^Session session session]
          (.setMaxTextMessageSize session (long max-frame-bytes))
          (.setMaxBinaryMessageSize session 1)
          (.setIdleTimeout session (Duration/ofMillis idle-ms))
          (vreset! session-of session)
          (heard!)
          (let [conn (line/open the-seam caller
                                {:send! (fn [^String text] (sent! session text))
                                 :close! (fn [status reason]
                                           (.remove sessions session)
                                           (closed-by! session status reason))})]
            (vreset! connection conn)
            (.put sessions session [conn heard])
            (when-not (line/open? conn)
              (.remove sessions session)))))
      (onWebSocketText [_ text]
        (heard!)
        (when-let [connection @connection]
          (line/text! connection text)))
      (onWebSocketBinary [_ _payload _offset _length]
        (when-let [^Session session @session-of]
          (.close session 1003 "the line carries text")))
      WebSocketPingPongListener
      (onWebSocketPing [_ payload]
        ;; A listener that hears pings answers them itself.
        (heard!)
        (when-let [^Session session @session-of]
          (try (.sendPong (.getRemote session) ^ByteBuffer payload noop-write-callback)
               (catch Throwable _ nil))))
      (onWebSocketPong [_ _payload]
        (heard!))
      (onWebSocketClose [_ _status _reason]
        (when-let [session @session-of] (.remove sessions session))
        (when-let [connection @connection]
          (line/closed! connection)))
      (onWebSocketError [_ _throwable]
        (when-let [session @session-of] (.remove sessions session))
        (when-let [connection @connection]
          (line/closed! connection))))))

(defn- servlet-handler
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
  "Takes a map:

    :the-seam   an opened seam
    :handler    the Ring handler of the pages (`softland.runtime.http`)
    :line-path  the path of the line, such as \"/line\"
    :accept     a function from a Ring request map to
                {:caller :max-frame-bytes} or a Ring response
    :host       the address to listen on; \"127.0.0.1\" when left out
    :port       the port; 0 for one that the system picks

  The server is started and not joined."
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
                            (let [now (System/nanoTime)
                                  idle-ns (* 1000000 (long idle-ms))]
                              (doseq [^java.util.Map$Entry entry (.entrySet sessions)]
                                (let [^Session session (.getKey entry)
                                      [conn ^AtomicLong heard] (.getValue entry)]
                                  (cond
                                    ;; Nothing heard for `idle-ms`, a pong included:
                                    ;; the system went away or does not answer.
                                    (< idle-ns (unchecked-subtract now (.get heard)))
                                    (do (.remove sessions session)
                                        (try (.disconnect session) (catch Throwable _ nil))
                                        (try (line/closed! conn) (catch Throwable _ nil)))

                                    ;; Non-blocking pings on the one ping thread, without the
                                    ;; connection's send lock: a stalled reader never blocks
                                    ;; pings or pins virtual-thread carriers.
                                    (and (line/open? conn) (.isOpen session))
                                    (try (.sendPing (.getRemote session) (ByteBuffer/allocate 0) noop-write-callback)
                                         (catch Throwable _ nil)))))))
                          ping-ms ping-ms TimeUnit/MILLISECONDS)
    {:server server :port (.getLocalPort connector) :pinger pinger}))

(defn stop!
  [{:keys [^Server server ^ScheduledExecutorService pinger]}]
  (.shutdownNow pinger)
  (.stop server)
  nil)
