(ns softland.runtime.attacks.slow-readers
  "Attack 1: carrier-thread pinning under slow WebSocket readers.

  Opens slow WebSocket readers on a copy of the runtime that never read from
  their sockets while large answers and a ping cycle wait on them, then asks
  the same copy on platform threads (with bounded timeouts) to answer a page
  request, a word over the line, and a call to Auth."
  (:require [clojure.string :as str]
            [softland.runtime.attacks.harness :as h]
            [softland.runtime.drive :as drive]
            [softland.runtime.jetty :as jetty]
            [softland.runtime.permissions :as perm]
            [softland.wire.frame :as frame]
            [softland.wire.proof :refer [check finish]])
  (:import [java.io BufferedReader InputStreamReader OutputStream]
           [java.net InetSocketAddress Socket]
           [java.nio.charset StandardCharsets]
           [java.security SecureRandom]
           [java.util Base64 UUID]))

(defn- ws-client-frame
  "Encodes `text` as one masked WebSocket client text frame (RFC 6455)."
  ^bytes [^String text]
  (let [payload (.getBytes text StandardCharsets/UTF_8)
        len (alength payload)
        mask (byte-array 4)
        _ (.nextBytes (SecureRandom.) mask)
        header (cond
                 (< len 126)
                 (byte-array [(unchecked-byte 0x81)
                              (unchecked-byte (bit-or 0x80 len))])
                 (< len 65536)
                 (byte-array [(unchecked-byte 0x81)
                              (unchecked-byte (bit-or 0x80 126))
                              (unchecked-byte (bit-and 0xff (bit-shift-right len 8)))
                              (unchecked-byte (bit-and 0xff len))])
                 :else
                 (throw (IllegalArgumentException. "frame too long for test helper")))
        out (byte-array (+ (alength header) 4 len))]
    (System/arraycopy header 0 out 0 (alength header))
    (System/arraycopy mask 0 out (alength header) 4)
    (dotimes [i len]
      (aset-byte out (+ (alength header) 4 i)
                 (unchecked-byte (bit-xor (aget payload i) (aget mask (bit-and i 3))))))
    out))

(defn- open-slow-ws!
  "Opens a raw TCP socket to `127.0.0.1:port` with a 4 KiB receive buffer and
  completes the `/line` WebSocket upgrade handshake, without reading frames
  afterward."
  ^Socket [port credential]
  (let [sock (Socket.)]
    (.setReceiveBufferSize sock 4096)
    (.connect sock (InetSocketAddress. "127.0.0.1" (int port)) 5000)
    (.setSoTimeout sock 5000)
    (let [key-bytes (byte-array 16)
          _ (.nextBytes (SecureRandom.) key-bytes)
          ws-key (.encodeToString (Base64/getEncoder) key-bytes)
          req (str "GET /line HTTP/1.1\r\n"
                   "Host: localhost:" port "\r\n"
                   "Upgrade: websocket\r\n"
                   "Connection: Upgrade\r\n"
                   "Sec-WebSocket-Version: 13\r\n"
                   "Sec-WebSocket-Key: " ws-key "\r\n"
                   "Authorization: Bearer " credential "\r\n"
                   "\r\n")
          ^OutputStream out (.getOutputStream sock)
          reader (BufferedReader. (InputStreamReader. (.getInputStream sock) StandardCharsets/ISO_8859_1))]
      (.write out (.getBytes req StandardCharsets/ISO_8859_1))
      (.flush out)
      (loop [line (.readLine reader)]
        (when (and line (not (str/blank? line)))
          (recur (.readLine reader))))
      (.setSoTimeout sock 0)
      sock)))

(defn- queue-reads!
  "Queues `reads-count` large-answer `:read` frames on an already-upgraded
  slow WebSocket `sock` without reading any reply."
  [^Socket sock query reads-count]
  (let [^OutputStream out (.getOutputStream sock)]
    (dotimes [i reads-count]
      (.write out (ws-client-frame (frame/write {:id (inc i) :word :read :query query}))))
    (.flush out)))

(defn- get-page-with-timeout
  "Fetches `GET /` on a platform socket with a hard `timeout-ms` read bound,
  so a pinned virtual-thread pool inside the same JVM cannot stall the probe
  itself."
  [port timeout-ms]
  (try
    (with-open [sock (Socket.)]
      (.connect sock (InetSocketAddress. "127.0.0.1" (int port)) (int timeout-ms))
      (.setSoTimeout sock (int timeout-ms))
      (let [out (.getOutputStream sock)
            reader (BufferedReader. (InputStreamReader. (.getInputStream sock) StandardCharsets/UTF_8))]
        (.write out (.getBytes (str "GET / HTTP/1.1\r\nHost: localhost:" port "\r\nConnection: close\r\n\r\n")
                               StandardCharsets/ISO_8859_1))
        (.flush out)
        (let [status-line (.readLine reader)]
          {:ok (boolean (and status-line (str/includes? status-line "200")))
           :status-line status-line})))
    (catch Throwable t
      {:ok false :threw (.getName (class t)) :said (.getMessage t)})))

(defn -main [& _]
  (h/with-cluster
    (fn [{:keys [cluster stand-in]}]
      (let [copy (h/start-copy! {:cluster cluster :stand-in stand-in})
            slow-sockets (atom [])]
        (try
          (h/setup-first-person! copy)
          (let [url (:url copy)
                port (:port copy)
                signed (drive/sign-in url (assoc h/first-person :call-her "Sid"))
                prog (drive/program url "slow-reader-probe"
                                    (fn [code] (drive/yes url (:cookie signed) code {})))
                healthy (h/line url (:credential prog) nil)
                hello (:hello healthy)
                who (:who hello)
                lane (:own-lane hello)
                own (perm/first-permission who lane)
                chunk (apply str (repeat 60000 "s"))
                wrote (h/say healthy {:word :send
                                      :act {:version 1 :who who :session (:session hello)
                                            :lane lane :permission (perm/permission-id own)
                                            :facts (mapv (fn [i] {:e :blob :k (keyword (str "p" i)) :v chunk})
                                                         (range 20))}
                                      :act-id (UUID/randomUUID)})
                _ (assert (= :accepted (get-in wrote [:answer :answer]))
                          (str "setup write must be accepted, got: " (pr-str wrote)))
                query {:asks [:entity :blob] :asker who :lanes [lane]}
                cores (.availableProcessors (Runtime/getRuntime))
                ;; Upgrade `cores + 2` connections up front while carriers are
                ;; free, then stall all of them on unread large answers across
                ;; one ping cycle so both `say!` and the ping path are pressed.
                n (+ cores 2)]
            (dotimes [_ n]
              (swap! slow-sockets conj (open-slow-ws! port (:credential prog))))
            (doseq [^Socket s @slow-sockets]
              (queue-reads! s query 24))
            (Thread/sleep (+ (long jetty/ping-ms) 1500))
            (let [page-f (future (get-page-with-timeout port 4000))
                  line-f (future (h/say healthy {:word :who-am-i} 4000))
                  auth-f (future (h/say healthy {:word :under} 4000))
                  page-res @page-f
                  line-res @line-f
                  auth-res @auth-f]
              (check "While several readers are slow, a copy still answers a page request, the line and a call to Auth."
                     (and (:ok page-res)
                          (= who (:who line-res))
                          (vector? (:sign-ins auth-res)))
                     {:cores cores
                      :slow-readers n
                      :page page-res
                      :line line-res
                      :auth auth-res}))
            (h/close-line! healthy))
          (finally
            ;; Close client sockets first so kernel RST/FIN unblocks any pinned
            ;; server writes before Jetty stops.
            (doseq [^Socket s @slow-sockets]
              (try (.setSoLinger s true 0) (catch Throwable _ nil))
              (try (.close s) (catch Throwable _ nil)))
            (Thread/sleep 200)
            (h/stop-copy! copy))))))
  (finish))
