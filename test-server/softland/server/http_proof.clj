(ns softland.server.http-proof
  "The proof of the face over HTTP: the words, the stream, and the pages of
  sign-in, through real requests to a Jetty that runs the handler, on
  Rama's in-process cluster. One command, from the repository root:

    clojure -M:server -m softland.server.http-proof

  Google is stood in for: a key pair of the proof's own signs ID tokens
  (`softland.server.google-stand-in`). One handler is opened as it is for
  Google, with the stand-in's client id, issuer and public key in Google's
  place, so `/sign-in/google` and its check run as they do for Google. A
  second handler is opened with the stand-in route, as a run on one machine
  has it. Both stand on one seam.

  State. One in-process cluster, two Jetty servers on two free ports of the
  loopback address, both closed at the end. The proof makes people, layers
  and acts in the store that it started."
  (:require [clojure.string :as str]
            [com.rpl.rama :as rama]
            [com.rpl.rama.test :as rtest]
            [missionary.core :as m]
            [ring.util.codec :as codec]
            [softland.server.frame :as frame]
            [softland.server.google-stand-in :as stand-in]
            [softland.server.http :as http]
            [softland.server.main :as main]
            [softland.server.module :as module]
            [softland.server.proof :refer [check finish]]
            [softland.server.seam :as seam]
            [softland.server.session.credential :as credential]
            [softland.server.sessions :as sessions]
            [softland.server.sign-in :as sign-in]
            [softland.server.words :as words]
            [softland.store.door :as door]
            [softland.store.module :as store]
            [softland.store.read-path :as read-path])
  (:import [java.io BufferedReader InputStream InputStreamReader]
           [java.net ServerSocket URI]
           [java.net.http HttpClient HttpClient$Redirect HttpClient$Version HttpRequest
            HttpRequest$BodyPublishers HttpResponse HttpResponse$BodyHandlers]
           [java.nio.charset StandardCharsets]
           [java.time Duration]
           [org.eclipse.jetty.server Server]))

(def ^:private ^HttpClient client
  (-> (HttpClient/newBuilder)
      (.version HttpClient$Version/HTTP_1_1)
      (.followRedirects HttpClient$Redirect/NEVER)
      (.build)))

(defn- free-port
  "Gives a port of the loopback address that nothing listens on now."
  []
  (with-open [socket (ServerSocket. 0)] (.getLocalPort socket)))

(defn- request
  "Makes one request. Takes the method, the URL, a map of headers and a
  body's text or nil. Gives {:status :headers :body}, where a header's name
  is in lower case."
  [method url headers body]
  (let [builder (-> (HttpRequest/newBuilder (URI/create url)) (.timeout (Duration/ofSeconds 30)))
        builder (reduce (fn [b [k v]] (.header b k v)) builder headers)
        builder (if (= :post method)
                  (.POST builder (HttpRequest$BodyPublishers/ofString (or body "")))
                  (.GET builder))
        ^HttpResponse response (.send client (.build builder) (HttpResponse$BodyHandlers/ofString))]
    {:status (.statusCode response)
     :headers (into {} (map (fn [[k v]] [(str/lower-case k) (first v)])) (.map (.headers response)))
     :body (.body response)}))

(defn- say
  "Says one word to a server. Takes the server's origin, a token or nil,
  and the word. Gives [the status, the result read as EDN]."
  [origin token word]
  (let [{:keys [status body]} (request :post (str origin "/word")
                                       (cond-> {"Content-Type" "application/edn"}
                                         token (assoc "Authorization" (str "Bearer " token)))
                                       (if (string? word) word (frame/write word)))]
    [status (second (frame/read-one body))]))

(defn- form
  "Takes a map of fields. Gives the body of a form's post."
  [fields]
  (codec/form-encode fields))

(defn- cookie-of
  "Takes a response. Gives the `softland=...` part of its Set-Cookie
  header, to send back as a Cookie header, or nil."
  [response]
  (some-> (get (:headers response) "set-cookie") (str/split #";") first))

(defn- stream-lines
  "Opens the stream of a `:watch`. Takes the server's origin, a token and
  the query. Gives an atom of the lines that have come, as EDN forms, with
  `::closed` at the end when the server closed the stream."
  [origin token query]
  (let [got (atom [])
        builder (-> (HttpRequest/newBuilder (URI/create (str origin "/word")))
                    (.header "Content-Type" "application/edn")
                    (.header "Authorization" (str "Bearer " token))
                    (.POST (HttpRequest$BodyPublishers/ofString (frame/write {:word :watch :query query}))))]
    (future
      (try
        (let [^HttpResponse response (.send client (.build builder) (HttpResponse$BodyHandlers/ofInputStream))]
          (with-open [reader (BufferedReader. (InputStreamReader. ^InputStream (.body response) StandardCharsets/UTF_8))]
            (loop []
              (when-let [line (.readLine reader)]
                (when (seq line)
                  (swap! got conj (second (frame/read-one line))))
                (recur)))))
        (catch Throwable _ nil))
      (swap! got conj ::closed))
    got))

(defn- until
  "Waits until a function of no argument gives a true thing, for ten
  seconds at most. Gives what it gave last."
  [f]
  (loop [n 0]
    (let [got (f)]
      (if (or got (<= 200 n))
        got
        (do (Thread/sleep 50) (recur (inc n)))))))

(defn- run-checks [the-seam the-sessions google origin dev-origin]
  (let [id-token (fn [claims] (stand-in/token google claims (System/currentTimeMillis)))
        from-google (fn [token cookie-csrf form-csrf]
                      (request :post (str origin "/sign-in/google")
                               (cond-> {"Content-Type" "application/x-www-form-urlencoded"
                                        "Origin" "https://accounts.google.com"}
                                 cookie-csrf (assoc "Cookie" (str "g_csrf_token=" cookie-csrf)))
                               (form (cond-> {"credential" token} form-csrf (assoc "g_csrf_token" form-csrf)))))]

    ;; ------------------------------------------------------- the frame and the word
    (check "The process answers that it is there."
           (= 200 (:status (request :get (str origin "/health") {} nil))))
    (check "A word with no credential is a visitor's."
           (= [200 {:visitor true}] (say origin nil {:word :who-am-i})))
    (check "A body that is no one EDN form is turned away with 400 and the server's word, and nothing is done."
           (= [[400 {:could-not :read-the-frame :why :not-edn}]
               [400 {:could-not :read-the-frame :why :more-than-one-form}]
               [400 {:could-not :read-the-frame :why :empty}]]
              [(say origin nil "{:word") (say origin nil "{:word :who-am-i} {:word :who-am-i}") (say origin nil "  ")]))
    (check "A frame over a visitor's bound is turned away with 413 before it is read as EDN, with the bound in the word."
           (= [413 {:could-not :read-the-frame :why :too-large :most words/max-visitor-frame-bytes}]
              (say origin nil (str "{:word :who-am-i :pad \"" (apply str (repeat (quot words/max-visitor-frame-bytes 8) "xxxxxxx ")) "\"}"))))
    (check "A frame whose one token is a number of a million digits is turned away with 400, in far less time than reading the number takes."
           (let [began (System/nanoTime)
                 got (say origin nil (apply str (repeat 60000 "7")))
                 got-long (with-redefs [words/max-visitor-frame-bytes 2000000]
                            (say origin nil (apply str (repeat 1000000 "7"))))
                 ms (quot (- (System/nanoTime) began) 1000000)]
             (and (= 400 (first got))
                  (= [400 {:could-not :read-the-frame :why :a-token-too-long}] got-long)
                  (< ms 3000)))
           :slow-or-read)
    (check "A thing that is no word of the form is turned away with 400, the reason and the part."
           (= [[400 {:could-not :read-the-word :why :word-not-known :at :word}]
               [400 {:could-not :read-the-word :why :holds-a-part-that-the-server-writes :at :who}]]
              [(say origin nil {:word :dance})
               (say origin nil {:word :send :act {:who :system :version 1}})]))
    (check "A credential that is no token, and a token that stands for nobody, get 401 and are not made a visitor."
           (= [[401 {:could-not :find-her-session :why :not-a-token}]
               [401 {:could-not :find-her-session :why :not-known}]]
              [(say origin "nonsense" {:word :who-am-i}) (say origin (credential/make) {:word :who-am-i})]))
    (check "A path that is no route gives 404, and the stand-in's route is not served where the server stands before Google."
           (= [404 404] [(:status (request :get (str origin "/nowhere") {} nil))
                         (:status (request :post (str origin "/sign-in/stand-in") {"Origin" origin} (form {"sub" "1"})))]))

    ;; ------------------------------------------------------------- sign-in
    (check "Google's post is turned away when its two texts against forgery are not both there and the same."
           (= [400 400 400]
              (mapv :status [(from-google (id-token {"sub" "5000001"}) nil "x")
                             (from-google (id-token {"sub" "5000001"}) "x" nil)
                             (from-google (id-token {"sub" "5000001"}) "x" "y")])))
    (check "A token that is not the issuer's for this server gives no cookie: changed after signing, for another audience, or past its time."
           (every? (fn [response] (and (= 401 (:status response)) (nil? (cookie-of response))))
                   [(from-google (str (id-token {"sub" "5000001"}) "x") "t" "t")
                    (from-google (id-token {"sub" "5000001" "aud" "another"}) "t" "t")
                    (from-google (id-token {"sub" "5000001" "exp" 1}) "t" "t")]))
    (let [signed-in (from-google (id-token {"sub" "5000001" "email" "asha@example.com"}) "t" "t")
          cookie (cookie-of signed-in)
          set-cookie (get (:headers signed-in) "set-cookie")]
      (check "A token that passes the check signs her in: the browser is sent on with a cookie that a page's code cannot read, marked SameSite=Lax."
             (and (= 303 (:status signed-in)) (= "/" (get (:headers signed-in) "location"))
                  (credential/token? (second (str/split (str cookie) #"=" 2)))
                  (str/includes? set-cookie "HttpOnly") (str/includes? set-cookie "SameSite=Lax"))
             [(:status signed-in) set-cookie])
      (check "The page says that she is signed in when the cookie comes back, and asks to sign in when none does."
             (and (str/includes? (:body (request :get (str origin "/") {"Cookie" cookie} nil)) "You are signed in")
                  (str/includes? (:body (request :get (str origin "/") {} nil)) "accounts.google.com/gsi/client")))
      (check "Where the browser goes on to after a sign-in is a path that the server built, never the text that came: a tab after the first slash, two slashes, another site, and a yes page with no hash all lead to the first page; a yes page with a hash is written anew."
             (let [on (fn [next] (get (:headers (request :get (str origin "/?next=" (codec/url-encode next)) {"Cookie" cookie} nil)) "location"))
                   hash (apply str (repeat 64 "c"))]
               (= [nil nil nil nil nil (str "/sign-in/yes?hash=" hash "&what=a%20b")]
                  [(on "/\t/evil.example/phish") (on "//evil.example") (on "https://evil.example/") (on "/\\evil.example")
                   (on "/sign-in/yes?hash=zz&what=x") (on (str "/sign-in/yes?what=a%20b&hash=" hash "&more=1"))]))
             (let [on (fn [next] (get (:headers (request :get (str origin "/?next=" (codec/url-encode next)) {"Cookie" cookie} nil)) "location"))]
               [(on "/\t/evil.example/phish") (on "//evil.example") (on (str "/sign-in/yes?what=a%20b&hash=" (apply str (repeat 64 "c")) "&more=1"))]))
      (check "A signed-in person who follows a link with such a text stays on this server: the answer to the tab is the first page itself."
             (= 200 (:status (request :get (str origin "/?next=" (codec/url-encode "/\t/evil.example")) {"Cookie" cookie} nil))))
      (check "The pages may not be framed by another site."
             (= "DENY" (get (:headers (request :get (str origin "/") {} nil)) "x-frame-options")))

      ;; ---------------------------------------------------- a program's sign-in
      (let [token (credential/make)
            hash (credential/hash-of token)
            waiting (future (say origin token {:word :who-am-i :wait-ms 20000}))
            yes (fn [headers fields] (request :post (str origin "/sign-in/yes")
                                              (merge {"Content-Type" "application/x-www-form-urlencoded"} headers)
                                              (form fields)))
            page (request :get (str origin "/sign-in/yes?hash=" hash "&what=" (codec/url-encode "a proof <b>")) {"Cookie" cookie} nil)]
        (check "Somebody who is not signed in is sent from the yes page to sign in, and comes back to it."
               (let [response (request :get (str origin "/sign-in/yes?hash=" hash "&what=x") {} nil)]
                 (and (= 303 (:status response))
                      (str/starts-with? (get (:headers response) "location") "/?next=%2Fsign-in%2Fyes"))))
        (check "The yes page shows the short code of the program's credential and what asks, with its markup made harmless."
               (and (= 200 (:status page)) (str/includes? (:body page) (credential/code-of hash))
                    (str/includes? (:body page) "a proof &lt;b&gt;") (not (str/includes? (:body page) "<b>")))
               (:status page))
        (Thread/sleep 300)
        (check "Her yes is taken only from this server's own page, with her cookie: not with no origin, not from another site, not without the cookie."
               (= [400 400 400]
                  (mapv :status [(yes {"Cookie" cookie} {"hash" hash "say" "yes"})
                                 (yes {"Cookie" cookie "Origin" "http://evil.example"} {"hash" hash "say" "yes"})
                                 (yes {"Origin" origin} {"hash" hash "say" "yes"})])))
        (check "The program that waits has not been answered: none of those was a yes."
               (not (realized? waiting)))
        (check "Her yes is taken."
               (= 200 (:status (yes {"Cookie" cookie "Origin" origin} {"hash" hash "say" "yes"}))))
        (let [[status me] (deref waiting 10000 [:late nil])]
          (check "The program that waited is answered at her yes: who she is, its session, and her own layer."
                 (and (= 200 status) (uuid? (:who me)) (uuid? (:session me)) (uuid? (:own-layer me)))
                 [status me])
          (check "The same link a second time is refused: a hash is bound once."
                 (= 409 (:status (yes {"Cookie" cookie "Origin" origin} {"hash" hash "say" "yes"}))))

          ;; --------------------------------------------------------- the words
          (let [layer (:own-layer me)
                who (:who me)
                [_ [act-id]] (say origin token {:word :act-ids :layer layer})
                act {:version 1 :layer layer :permission [who layer layer] :facts [{:e :chair-7 :k :colour :v :green}]}
                watched (stream-lines origin token {:asks [:entity :chair-7] :from-layer layer})
                _ (until #(seq @watched))
                [send-status receipt] (say origin token {:word :send :act act :act-id act-id})
                [_ answer] (say origin token {:word :answer :act-id act-id :digest (:digest receipt) :wait-ms 2000})]
            (check "A program asks an act-id, sends an act under it and reads its answer: the receipt holds the act-id and the digest, and the answer says accepted."
                   (and (= 200 send-status) (= act-id (:act-id receipt)) (= :accepted (:answer answer)))
                   [receipt answer])
            (check "A read gives the fact back, and a visitor who names the layer is told that she may not see it."
                   (and (= :green (:value (:place (second (say origin token {:word :read :query {:asks [:place :chair-7 :colour] :from-layer layer}})))))
                        (= [{:layer layer :why :may-not-see}]
                           (:left-out (second (say origin nil {:word :read :query {:asks [:place :chair-7 :colour] :layers [layer]}}))))))
            (check "A visitor who sends an act gets 401 and the server's word."
                   (= [401 {:could-not :find-her-session :why :no-credential}] (say origin nil {:word :send :act act})))
            (check "A gate's refusal is no fault of the request: the act is sent, the status is 200, and the answer says refused."
                   (let [[status wrong] (say origin token {:word :send :act (assoc act :version 9)})]
                     (and (= 200 status)
                          (= :refused (:answer (second (say origin token {:word :answer :act-id (:act-id wrong) :digest (:digest wrong) :wait-ms 2000})))))))
            (check "The stream of a watch carried the whole answer and then the place that the act changed, one form on a line."
                   (let [lines (until #(when (<= 2 (count @watched)) @watched))]
                     (and (contains? (first lines) :anew)
                          (= :green (:value (get (:changed (second lines)) [:chair-7 :colour])))))
                   @watched)
            (check "The program ends its own session with a word."
                   (= [200 [:ok]] (say origin token {:word :sign-out})))
            (check "The stream is told the end, with the word that her session ended, and the server closes it."
                   (let [lines (until #(when (= ::closed (last @watched)) @watched))]
                     (= {:ended {:could-not :find-her-session :why :ended}} (last (butlast lines))))
                   @watched)
            (check "After that the token stands for nobody."
                   (= [401 {:could-not :find-her-session :why :ended}] (say origin token {:word :who-am-i}))))))

      ;; ------------------------------------------------------------- her no
      (let [token (credential/make)
            hash (credential/hash-of token)]
        (check "Her no refuses a program for good: the program is told that the credential ended."
               (and (= 200 (:status (request :post (str origin "/sign-in/yes")
                                             {"Content-Type" "application/x-www-form-urlencoded" "Cookie" cookie "Origin" origin}
                                             (form {"hash" hash "say" "no"}))))
                    (= [401 {:could-not :find-her-session :why :ended}] (say origin token {:word :who-am-i :wait-ms 2000})))))

      ;; ----------------------------------------------------------- sign-out
      (check "A sign-out from another site's page is not taken."
             (= 400 (:status (request :post (str origin "/sign-out") {"Cookie" cookie "Origin" "http://evil.example"} ""))))
      (let [made-up (credential/make)
            out (request :post (str origin "/sign-out") {"Cookie" (str "softland=" made-up) "Origin" origin} "")]
        (check "A sign-out with a cookie that only has the form of a token takes the cookie away and writes nothing: no row is made for its hash."
               (and (= 303 (:status out))
                    (= [:ok nil] (sessions/row-of the-sessions (credential/hash-of made-up))))
               [(:status out) (sessions/row-of the-sessions (credential/hash-of made-up))]))
      (let [out (request :post (str origin "/sign-out") {"Cookie" cookie "Origin" origin} "")]
        (check "A sign-out ends the cookie's session and takes the cookie away: the page asks to sign in again when the old cookie comes back."
               (and (= 303 (:status out)) (str/includes? (get (:headers out) "set-cookie") "Max-Age=0")
                    (not (str/includes? (:body (request :get (str origin "/") {"Cookie" cookie} nil)) "You are signed in"))))))

    ;; ------------------------------------------------------- the stand-in's route
    (check "With a stand-in, the page says plainly that anybody can sign in as any account."
           (str/includes? (:body (request :get (str dev-origin "/") {} nil)) "stand-in for Google"))
    (check "The stand-in's sign-in is taken from this server's own page only, and then signs in through the same check."
           (let [post (fn [headers] (request :post (str dev-origin "/sign-in/stand-in")
                                             (merge {"Content-Type" "application/x-www-form-urlencoded"} headers)
                                             (form {"sub" "5000002" "email" "ben@example.com"})))
                 taken (post {"Origin" dev-origin})]
             (and (= 400 (:status (post {})))
                  (= 303 (:status taken))
                  (str/includes? (:body (request :get (str dev-origin "/") {"Cookie" (cookie-of taken)} nil)) "You are signed in"))))))

(defn -main
  "Starts the cluster and the two servers, runs every check, closes
  everything, prints the counts, and ends the process with status 0 when
  none failed."
  [& _]
  (with-open [cluster (rtest/create-ipc)]
    (rtest/launch-module! cluster store/Store {:tasks 4 :threads 2})
    (rtest/launch-module! cluster module/Sessions {:tasks 4 :threads 2})
    (let [the-sessions (sessions/open cluster (rama/get-module-name module/Sessions))
          the-seam (seam/open {:door (door/open cluster (rama/get-module-name store/Store))
                               :read-path (read-path/open cluster (rama/get-module-name store/Store))
                               :sessions the-sessions})
          google (stand-in/make)
          port (free-port)
          dev-port (free-port)
          origin (str "http://localhost:" port)
          dev-origin (str "http://localhost:" dev-port)
          ^Server server (main/serve (http/handler {:the-seam the-seam
                                                    :origin origin
                                                    :google {:client-id (:client-id google)
                                                             :issuers #{(:issuer google)}
                                                             :keys-fn (fn [] [:ok (:keys google)])}})
                                     port)
          ^Server dev-server (main/serve (http/handler {:the-seam the-seam :origin dev-origin :stand-in (stand-in/make)})
                                         dev-port)]
      (try
        (check "The setup of sign-in ends ready." (= :ready (m/? (sign-in/setup the-seam))))
        (run-checks the-seam the-sessions google origin dev-origin)
        (catch Throwable t
          (check "The proof ran to its end without a throw." false (str t))
          (.printStackTrace t))
        (finally
          (.stop server)
          (.stop dev-server)
          (seam/close! the-seam)
          (sessions/close! the-sessions)))))
  (finish))
