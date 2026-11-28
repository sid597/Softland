(ns softland.runtime.attacks.sign-in-pace
  "Attack 6: sign-in is paced by remote address.

  Sends a flood of posts to `/sign-in/*` from `127.0.0.1` — including forged
  `RS256` tokens to `POST /sign-in/google` with unknown `kid`s (which would
  otherwise trigger a JWKS key fetch per request) as well as posts to
  `POST /sign-in/program` and `POST /sign-in/program/poll` — and verifies
  that:
  1. The runtime's default numbers define `:sign-in-window-ms` and
     `:max-sign-in-posts-per-address`.
  2. Past `:max-sign-in-posts-per-address` in `:sign-in-window-ms`, posts
     from `127.0.0.1` reach neither Google (`google/fetch-keys`) nor Auth.
  3. Another remote address (`127.0.0.2`) is not blocked by `127.0.0.1`'s
     flood."
  (:require [clojure.string :as str]
            [softland.auth.client :as auth]
            [softland.runtime.attacks.harness :as h]
            [softland.runtime.attacks.numbers :as numbers]
            [softland.runtime.google :as google]
            [softland.runtime.google-stand-in :as stand-in]
            [softland.runtime.seam :as seam]
            [softland.wire.credential :as credential]
            [softland.wire.frame :as frame]
            [softland.wire.proof :refer [check finish]])
  (:import [java.io BufferedReader InputStreamReader]
           [java.net InetAddress InetSocketAddress Socket]
           [java.nio.charset StandardCharsets]
           [java.util Base64]))

(defn- b64url [^String s]
  (.encodeToString (.withoutPadding (Base64/getUrlEncoder))
                   (.getBytes s StandardCharsets/UTF_8)))

(defn- forged-rs256-token [kid]
  (str (b64url (str "{\"alg\":\"RS256\",\"kid\":\"" kid "\"}"))
       "."
       (b64url "{\"sub\":\"999\",\"iss\":\"https://accounts.google.com\",\"aud\":\"test-google-client\",\"exp\":99999999999}")
       "."
       (b64url "not-a-valid-rsa-signature")))

(defn- raw-post
  "Sends an HTTP POST from `local-ip` to `127.0.0.1:port` so Jetty sees
  `:remote-addr` equal to `local-ip`."
  [local-ip port path content-type extra-headers ^String body]
  (with-open [sock (Socket.)]
    (.bind sock (InetSocketAddress. (InetAddress/getByName local-ip) 0))
    (.connect sock (InetSocketAddress. "127.0.0.1" (int port)) 5000)
    (.setSoTimeout sock 5000)
    (let [body-bytes (.getBytes body StandardCharsets/UTF_8)
          req (str "POST " path " HTTP/1.1\r\n"
                   "Host: localhost:" port "\r\n"
                   "Content-Type: " content-type "\r\n"
                   "Content-Length: " (alength body-bytes) "\r\n"
                   "Connection: close\r\n"
                   (apply str (for [[k v] extra-headers] (str k ": " v "\r\n")))
                   "\r\n")
          out (.getOutputStream sock)
          reader (BufferedReader. (InputStreamReader. (.getInputStream sock) StandardCharsets/UTF_8))]
      (.write out (.getBytes req StandardCharsets/ISO_8859_1))
      (.write out body-bytes)
      (.flush out)
      (let [status-line (.readLine reader)
            status (some-> status-line (str/split #"\s+") second parse-long)
            _ (loop [hdr (.readLine reader)]
                (when (and hdr (not (str/blank? hdr)))
                  (recur (.readLine reader))))
            resp-body (slurp reader)]
        {:status status :body resp-body}))))

(defn -main [& _]
  (h/with-cluster
    (fn [{:keys [cluster]}]
      (let [fake-idp (stand-in/make)
            default-copy (h/start-copy! {:cluster cluster :stand-in fake-idp})
            pace-limit 6
            paced-copy (h/start-copy! {:cluster cluster
                                       :google {:client-id "test-google-client"}
                                       :numbers {:sign-in-window-ms 10000
                                                 :max-sign-in-posts-per-address pace-limit}})]
        (try
          (h/setup-first-person! default-copy)
          (let [defaults (seam/numbers-of (:the-seam default-copy))
                has-keys? (and (contains? defaults :sign-in-window-ms)
                               (contains? defaults :max-sign-in-posts-per-address))
                google-calls (atom 0)
                auth-calls (atom 0)
                orig-enter auth/enter
                orig-begin auth/begin-sign-in
                orig-ask auth/ask
                orig-ask-of auth/ask-of
                orig-who-is auth/who-is]
            (with-redefs [google/fetch-keys
                          (fn [_url]
                            (swap! google-calls inc)
                            [:ok (:keys fake-idp)])
                          auth/enter
                          (fn [c m] (swap! auth-calls inc) (orig-enter c m))
                          auth/begin-sign-in
                          (fn [c m] (swap! auth-calls inc) (orig-begin c m))
                          auth/ask
                          (fn [c m] (swap! auth-calls inc) (orig-ask c m))
                          auth/ask-of
                          (fn [c code] (swap! auth-calls inc) (orig-ask-of c code))
                          auth/who-is
                          (fn [c m] (swap! auth-calls inc) (orig-who-is c m))]
              (let [port (:port paced-copy)
                    ;; 30 posts from 127.0.0.1: 15 forged `/sign-in/google`,
                    ;; 10 `/sign-in/program`, 5 `/sign-in/program/poll`.
                    forged-resps
                    (mapv (fn [i]
                            (raw-post "127.0.0.1" port "/sign-in/google"
                                      "application/x-www-form-urlencoded"
                                      [["Cookie" "g_csrf_token=csrf1"]
                                       ["Origin" (:url paced-copy)]]
                                      (str "g_csrf_token=csrf1&credential=" (forged-rs256-token (str "forged-" i)))))
                          (range 15))
                    prog-resps
                    (mapv (fn [i]
                            (raw-post "127.0.0.1" port "/sign-in/program"
                                      "application/edn"
                                      []
                                      (frame/write {:hash (credential/hash-of (credential/make-secret))
                                                    :asking-hash (credential/hash-of (credential/make-secret))
                                                    :asks (str "flood-" i)})))
                          (range 10))
                    poll-resps
                    (mapv (fn [_]
                            (raw-post "127.0.0.1" port "/sign-in/program/poll"
                                      "application/edn"
                                      []
                                      (frame/write {:code "ABCD2345"
                                                    :asking-secret (credential/make-secret)})))
                          (range 5))
                    google-from-flood @google-calls
                    auth-from-flood @auth-calls
                    ;; Another address (127.0.0.2) must still get through.
                    other-addr-resp
                    (raw-post "127.0.0.2" port "/sign-in/program"
                              "application/edn"
                              []
                              (frame/write {:hash (credential/hash-of (credential/make-secret))
                                            :asking-hash (credential/hash-of (credential/make-secret))
                                            :asks "other-address"}))
                    [_ other-form] (frame/read-one (:body other-addr-resp))
                    all-flood (concat forged-resps prog-resps poll-resps)
                    turned-away (count (filter #(and (:status %) (<= 429 (:status %) 429)) all-flood))
                    rejected (count (filter #(and (:status %) (<= 400 (:status %))) all-flood))]
                (check "Sign-in is paced by address: a flood of posts to `/sign-in/*` from one address, with forged tokens too, reaches neither Google nor Auth past the pace."
                       (and has-keys?
                            (<= (+ google-from-flood auth-from-flood) pace-limit)
                            (<= google-from-flood pace-limit)
                            (<= auth-from-flood pace-limit)
                            (<= (- 30 pace-limit) rejected)
                            (= 200 (:status other-addr-resp))
                            (string? (:code other-form)))
                       {:default-number-keys (sort (keys defaults))
                        :pace-limit pace-limit
                        :flood-sent 30
                        :google-calls-during-flood google-from-flood
                        :auth-calls-during-flood auth-from-flood
                        :flood-429-count turned-away
                        :other-address {:status (:status other-addr-resp) :form other-form}}))))
          (finally
            (h/stop-copy! paced-copy)
            (h/stop-copy! default-copy))))))
  (finish))
