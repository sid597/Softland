(ns softland.runtime.review-faults-proof
  "Checks as sentences for the five runtime and command-line faults named in
  `src/proposal/runtime-2026-11-21/overnight-audit/REVIEW-runtime-spine.md`.
  Each check is written to fail before its fix and pass after."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [softland.auth.client :as auth]
            [softland.command.client :as client]
            [softland.command.main :as cmd-main]
            [softland.runtime.drive :as drive]
            [softland.runtime.google-stand-in :as stand-in]
            [softland.runtime.jetty :as jetty]
            [softland.runtime.line :as line]
            [softland.runtime.main :as main]
            [softland.runtime.permissions :as perm]
            [softland.runtime.seam :as seam]
            [softland.runtime.sign-in :as sign-in]
            [softland.runtime.standing :as standing]
            [softland.wire.credential :as credential]
            [softland.wire.frame :as frame]
            [softland.wire.proof :refer [check finish]])
  (:import [java.io BufferedReader Closeable InputStreamReader OutputStream]
           [java.net InetSocketAddress ServerSocket Socket]
           [java.nio.charset StandardCharsets]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.security SecureRandom]
           [java.util Base64 UUID]))

(defn- free-port []
  (with-open [s (ServerSocket. 0)] (.getLocalPort s)))

(defn- wait-for [f ms]
  (let [ends (+ (System/currentTimeMillis) ms)]
    (loop []
      (or (try (f) (catch Throwable _ nil))
          (when (< (System/currentTimeMillis) ends)
            (Thread/sleep 25)
            (recur))))))

(defn- ws-client-frame
  "Encodes a masked client-to-server WebSocket text frame (RFC 6455)."
  ^bytes [^String text]
  (let [payload (.getBytes text StandardCharsets/UTF_8)
        n (alength payload)
        mask (byte-array [0x11 0x22 0x33 0x44])
        out (java.io.ByteArrayOutputStream.)]
    (.write out 0x81) ;; FIN + text
    (cond
      (< n 126) (.write out (bit-or 0x80 n))
      (< n 65536) (do (.write out (bit-or 0x80 126))
                      (.write out (bit-and (unsigned-bit-shift-right n 8) 0xff))
                      (.write out (bit-and n 0xff)))
      :else (throw (IllegalArgumentException. "frame too large for helper")))
    (.write out mask)
    (dotimes [i n]
      (.write out (bit-xor (aget payload i) (aget mask (bit-and i 3)))))
    (.toByteArray out)))

(defn- open-stalled-ws
  "Opens a raw TCP socket with a tiny receive buffer, upgrades it to a
  WebSocket on `/line`, and stops reading from the socket so the server's
  send buffer fills up."
  ^Socket [port credential]
  (let [sock (Socket.)
        key-bytes (byte-array 16)
        _ (.nextBytes (SecureRandom.) key-bytes)
        ws-key (.encodeToString (Base64/getEncoder) key-bytes)]
    (.setReceiveBufferSize sock 4096)
    (.connect sock (InetSocketAddress. "127.0.0.1" (int port)) 5000)
    (.setSoTimeout sock 5000)
    (let [^OutputStream out (.getOutputStream sock)
          req (str "GET /line HTTP/1.1\r\n"
                   "Host: localhost:" port "\r\n"
                   "Upgrade: websocket\r\n"
                   "Connection: Upgrade\r\n"
                   "Sec-WebSocket-Key: " ws-key "\r\n"
                   "Sec-WebSocket-Version: 13\r\n"
                   (when credential (str "Authorization: Bearer " credential "\r\n"))
                   "\r\n")]
      (.write out (.getBytes req StandardCharsets/US_ASCII))
      (.flush out)
      ;; Read only the HTTP 101 response headers, then stop reading!
      (let [in (.getInputStream sock)
            hdr (StringBuilder.)]
        (loop []
          (let [b (.read in)]
            (when (>= b 0)
              (.append hdr (char b))
              (when-not (str/ends-with? (str hdr) "\r\n\r\n")
                (recur)))))
        (assert (str/starts-with? (str hdr) "HTTP/1.1 101")
                (str "Expected 101, got: " hdr))))
    sock))

(defn- check-pings! [cluster first-person]
  (let [port (free-port)
        url (str "http://localhost:" port)
        copy (main/start {:cluster cluster :origin url :stand-in (stand-in/make)})
        served (with-redefs [jetty/ping-ms 20]
                 (main/serve! copy "127.0.0.1" port))
        cookie (:cookie (drive/sign-in url first-person))
        prog (drive/program url "slow-reader-test" (fn [code] (drive/yes url cookie code {})))
        me (client/connect {:url url :credential (:credential prog)})
        hello (client/hello me)
        lane (:own-lane hello)
        who (:who hello)
        own (perm/first-permission who lane)
        big-val (apply str (repeat 60000 "x"))
        _ (client/say me {:word :send
                          :act {:version 1 :who who :session (:session hello) :lane lane
                                :permission (perm/permission-id own)
                                :facts [{:e :blob :k :data :v big-val}]}
                          :act-id (UUID/randomUUID)})
        _ (client/close! me)
        socks (atom [])]
    (try
      ;; Open 4 stalled raw WebSockets and flood each with 200 large read
      ;; requests without reading any responses, filling TCP buffers so
      ;; Jetty's sendString blocks inside line/say!.
      (dotimes [_ 4]
        (let [s (open-stalled-ws port (:credential prog))
              out (.getOutputStream s)]
          (swap! socks conj s)
          (dotimes [i 120]
            (let [frame (ws-client-frame
                         (frame/write {:id i :word :read
                                       :query {:asks [:place :blob :data] :asker who :lanes [lane]}}))]
              (.write out frame)))
          (.flush out)))
      ;; Wait long enough for >40 ping ticks (40 * 4 = 160 pings > 24 carriers on old code).
      (Thread/sleep 1500)
      (let [vthread-ran (promise)
            _ (Thread/startVirtualThread (fn [] (deliver vthread-ran :ok)))
            vt-res (deref vthread-ran 2000 :hung)
            health-fut (future
                         (try
                           (:status (drive/request :get (str url "/health") {}))
                           (catch Throwable t (str t))))
            health-res (deref health-fut 2000 :hung)]
        (check "Pinging open connections uses Jetty's non-blocking sendPing without holding the connection's monitor or spawning a virtual thread per ping, so slow readers never stall pings or exhaust virtual-thread carriers."
               (and (= :ok vt-res) (= 200 health-res))
               {:vthread vt-res :health health-res}))
      (finally
        (doseq [^Socket s @socks]
          (try (.close s) (catch Throwable _ nil)))
        (try (jetty/stop! served) (catch Throwable _ nil))
        (try ((:stop copy)) (catch Throwable _ nil))))))

(defn- check-yes-page! [url cookie-a cookie-b]
  (let [post-yes (fn [cookie code form]
                   (drive/request :post (str url "/yes")
                                  {:cookie cookie :origin url :form (assoc form :code code)}))
        ask-code (fn [label]
                   (let [[_ asked] (frame/read-one
                                    (:body (drive/request :post (str url "/sign-in/program")
                                                          {:edn {:hash (credential/hash-of (credential/make-secret))
                                                                 :asking-hash (credential/hash-of (credential/make-secret))
                                                                 :asks label}})))]
                     (:code asked)))
        ;; Case 1: yes as me, then yes as agent
        c1 (ask-code "yes-me-then-agent")
        r1a (post-yes cookie-a c1 {:say "yes" :as "me"})
        r1b (post-yes cookie-a c1 {:say "yes" :as "new-agent" :label "helper"})
        ;; Case 2: yes as agent, then yes as me
        c2 (ask-code "yes-agent-then-me")
        r2a (post-yes cookie-a c2 {:say "yes" :as "new-agent" :label "helper"})
        r2b (post-yes cookie-a c2 {:say "yes" :as "me"})
        ;; Case 3: yes as me, then no by same person
        c3 (ask-code "yes-then-no-same")
        r3a (post-yes cookie-a c3 {:say "yes" :as "me"})
        r3b (post-yes cookie-a c3 {:say "no" :as "me"})
        ;; Case 4: yes as me by A, then no by B
        c4 (ask-code "yes-a-then-no-b")
        r4a (post-yes cookie-a c4 {:say "yes" :as "me"})
        r4b (post-yes cookie-b c4 {:say "no" :as "me"})]
    (check "A second yes on the yes page that names another actor than the one that stands is refused with 409 and does not say Done."
           (and (= 200 (:status r1a)) (str/includes? (:body r1a) "Done")
                (= 409 (:status r1b)) (not (str/includes? (:body r1b) "Done"))
                (= 200 (:status r2a)) (str/includes? (:body r2a) "Done")
                (= 409 (:status r2b)) (not (str/includes? (:body r2b) "Done")))
           {:me-then-agent [(:status r1b) (:body r1b)]
            :agent-then-me [(:status r2b) (:body r2b)]})
    (check "A person who says no on the yes page after her own yes is told with 409 that the program is signed in as her, while another person is not told whose sign-in stands."
           (and (= 200 (:status r3a))
                (= 409 (:status r3b))
                (str/includes? (:body r3b) "signed in")
                (str/includes? (:body r3b) "as you")
                (= 200 (:status r4a))
                (= 409 (:status r4b))
                (not (str/includes? (:body r4b) "as you")))
           {:no-after-own-yes [(:status r3b) (:body r3b)]
            :no-by-other [(:status r4b) (:body r4b)]})))

(defn- check-moved-on! [cluster url cookie]
  (let [prog (drive/program url "moved-on-test" (fn [code] (drive/yes url cookie code {})))
        copy (main/start {:cluster cluster :origin "http://localhost:0"
                          :numbers {:secret-ms 60000 :previous-ms 1000 :session-ms 12000}})
        the-seam (:the-seam copy)
        the-auth (seam/auth-of the-seam)
        session (UUID/randomUUID)
        now (seam/now-ms)]
    (try
      ;; Open a session with 4000ms left (< half of 12000ms), so the first
      ;; standing check triggers moved-on! immediately.
      (auth/open-session the-auth {:root (:root prog) :sign-in (:sign-in prog)
                                   :session session :until (+ now 4000) :at now})
      (let [real-open-session auth/open-session
            closed-once (atom false)
            ;; Build caller while session is still open (avoid triggering moved-on! here
            ;; by reading directly after setting up wrapper).
            caller (with-redefs [auth/open-session (fn [_ _] {:open session})]
                     (missionary.core/? (seam/caller the-seam {:credential (:credential prog)
                                                               :carrier :header
                                                               :session session
                                                               :new-session? false
                                                               :from {:face :test}})))
            ended (promise)]
        ;; Suppress watch pushes AND close the session right before moved-on!'s
        ;; open-session executes, so Auth returns {:open S} on a closed session.
        (with-redefs [standing/listen! (fn [_ _ _] (fn [] nil))
                      auth/open-session (fn [c m]
                                          (when (compare-and-set! closed-once false true)
                                            (auth/close-session c {:root (:root m) :session (:session m) :at (:at m)}))
                                          (real-open-session c m))]
          (let [cancel ((missionary.core/reduce (fn [_ w] (deliver ended w) nil)
                                                nil
                                                (seam/standing the-seam caller))
                        (fn [_] nil) (fn [_] nil))
                got (deref ended 2500 :still-alive)]
            (cancel)
            (check "A closed session whose close push never reached the copy is ended by the seam's timer on its next read rather than extended by a repeat open-session's {:open S}."
                   (and (map? got) (= :find-her-session (:could-not got)))
                   got))))
      (finally
        ((:stop copy))))))

(defn- check-standing-failure! [cluster url cookie]
  (let [prog (drive/program url "standing-fail-test" (fn [code] (drive/yes url cookie code {})))
        copy (main/start {:cluster cluster :origin "http://localhost:0"})
        the-seam (:the-seam copy)
        caller (missionary.core/? (seam/caller the-seam {:credential (:credential prog)
                                                         :carrier :header
                                                         :new-session? true
                                                         :from {:face :test}}))
        sent (atom [])
        closed (promise)]
    (try
      (let [conn (with-redefs [standing/listen! (fn [_ _ _] (throw (RuntimeException. "watch boom")))]
                   (line/open the-seam caller
                              {:send! (fn [t] (swap! sent conj (second (frame/read-one t))))
                               :close! (fn [s r] (deliver closed [s r]))}))
            close-res (deref closed 2000 :still-open)
            byes (filter :bye @sent)]
        (check "A standing flow that fails ends its connection with :reach-auth instead of leaving the connection open and unwatched."
               (and (not (line/open? conn))
                    (vector? close-res)
                    (some #(= :reach-auth (get-in % [:bye :could-not])) byes))
               {:open? (line/open? conn) :closed close-res :byes byes}))
      (finally
        ((:stop copy))))))

(defn- check-lost-replacement! [cluster first-person]
  (let [port (free-port)
        url (str "http://localhost:" port)
        copy (main/start {:cluster cluster :origin url :stand-in (stand-in/make)
                          :numbers {:secret-ms 60000 :previous-ms 700 :session-ms 60000}})
        served (main/serve! copy "127.0.0.1" port)
        cookie (:cookie (drive/sign-in url first-person))
        prog (drive/program url "lost-replace-test" (fn [code] (drive/yes url cookie code {})))
        home (.toFile (Files/createTempDirectory "softland-lost-replace" (make-array FileAttribute 0)))
        sign-in-file (io/file home "sign-in.edn")
        run-cmd (fn [cmd args]
                  ((resolve 'softland.command.main/run) cmd (into ["--as" (.getPath sign-in-file)] args)))]
    (try
      (spit sign-in-file (str (frame/write {:url url :credential (:credential prog)}) "\n"))
      ;; First establish session normally
      (let [before (run-cmd "who" [])
            real-say client/say
            took-replace (atom false)
            ;; Scenario 1: replacement is taken by Auth, then its answer is lost on the wire.
            lost-after (with-redefs [client/say (fn [conn word & more]
                                                  (let [res (apply real-say conn word more)]
                                                    (if (= :replace-secret (:word word))
                                                      (do (when (:replaced res) (reset! took-replace true))
                                                          {:could-not :hear-the-runtime})
                                                      res)))]
                         (run-cmd "replace-secret" []))
            ;; Wait past previous-ms (700ms) so the old secret is completely dead (401).
            _ (Thread/sleep 1100)
            after-who (run-cmd "who" [])
            ;; Scenario 2: replacement fails BEFORE reaching Auth, then retry succeeds.
            lost-before (with-redefs [client/say (fn [conn word & more]
                                                   (if (= :replace-secret (:word word))
                                                     {:could-not :hear-the-runtime}
                                                     (apply real-say conn word more)))]
                          (run-cmd "replace-secret" []))
            retry-replace (run-cmd "replace-secret" [])
            final-who (run-cmd "who" [])]
        (check "When a secret replacement is taken by Auth and its answer is lost, the command line is not locked out after the previous secret expires and can still run commands and retry replacement."
               (and (= (perm/person (:root before)) (:who before))
                    @took-replace
                    (= :hear-the-runtime (:could-not lost-after))
                    (= (:who before) (:who after-who))
                    (= :hear-the-runtime (:could-not lost-before))
                    (= (:sign-in before) (:replaced retry-replace))
                    (= (:who before) (:who final-who)))
               {:before (:who before)
                :took-replace @took-replace
                :lost-after lost-after
                :after-who after-who
                :lost-before lost-before
                :retry-replace retry-replace
                :final-who final-who}))
      (finally
        (try (jetty/stop! served) (catch Throwable _ nil))
        (try ((:stop copy)) (catch Throwable _ nil))))))

(defn -main [& _]
  (let [cluster (main/in-process-cluster)
        port (free-port)
        url (str "http://localhost:" port)
        copy (main/start {:cluster cluster :origin url :stand-in (stand-in/make)})
        served (main/serve! copy "127.0.0.1" port)
        first-person {:sub "rev-1" :email "rev1@example.com" :name "Rev One"}
        second-person {:sub "rev-2" :email "rev2@example.com" :name "Rev Two"}]
    (try
      (sign-in/setup! (:the-seam copy) {:email (:email first-person)})
      (let [cookie-a (:cookie (drive/sign-in url first-person))
            prog-a (drive/program url "inviter" (fn [code] (drive/yes url cookie-a code {})))
            conn-a (client/connect {:url url :credential (:credential prog-a)})
            _ (client/hello conn-a)
            _ (client/say conn-a {:word :invite :uuid (UUID/randomUUID)
                                  :claim {:by :email :email (:email second-person)}})
            _ (client/close! conn-a)
            cookie-b (:cookie (drive/sign-in url second-person))]
        (check-pings! cluster first-person)
        (check-yes-page! url cookie-a cookie-b)
        (check-moved-on! cluster url cookie-a)
        (check-standing-failure! cluster url cookie-a)
        (check-lost-replacement! cluster first-person))
      (catch Throwable t
        (check "The review-faults proof ran to its end without a throw." false (str t))
        (.printStackTrace t))
      (finally
        (try (jetty/stop! served) (catch Throwable _ nil))
        (try ((:stop copy)) (catch Throwable _ nil))
        (try (.close ^Closeable cluster) (catch Throwable _ nil))))
    (finish)))
