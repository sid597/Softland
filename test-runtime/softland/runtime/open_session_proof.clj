(ns softland.runtime.open-session-proof
  "Checks as sentences for Step 4: the wire word `{:word :open-session :driven-by D}`,
  `:driven-by` in the runtime, browser faces passing `{:kind :person}`, and the
  command line opening a session with a model's name. Written first to fail before
  implementation and pass after."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [softland.auth.client :as auth]
            [softland.command.client :as client]
            [softland.runtime.drive :as drive]
            [softland.runtime.google-stand-in :as stand-in]
            [softland.runtime.jetty :as jetty]
            [softland.runtime.main :as main]
            [softland.runtime.seam :as seam]
            [softland.runtime.sign-in :as sign-in]
            [softland.wire.credential :as credential]
            [softland.wire.frame :as frame]
            [softland.wire.proof :refer [check finish]]
            [softland.wire.words :as words])
  (:import [java.io Closeable File]
           [java.net ServerSocket URI]
           [java.net.http HttpClient WebSocket WebSocket$Listener]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.time Duration]
           [java.util UUID]
           [java.util.concurrent CompletableFuture TimeUnit]))

(defn- free-port []
  (with-open [s (ServerSocket. 0)] (.getLocalPort s)))

(defn- command-sdeps []
  (if-let [store-path (not-empty (System/getProperty "softland.store.path"))]
    ["-Sdeps" (pr-str {:aliases {:command {:replace-paths [store-path "src-wire" "src-command"]
                                           :replace-deps  {'org.clojure/clojure {:mvn/version "1.12.4"}}
                                           :jvm-opts      ["-XX:TieredStopAtLevel=1"]}}})]
    []))

(defn- run-cli [^File home & args]
  (let [builder (ProcessBuilder. ^java.util.List (into (into ["clojure"] (command-sdeps))
                                                       (into ["-M:command" "-m" "softland.command.main"] args)))
        _ (.put (.environment builder) "SOFTLAND_HOME" (.getPath home))
        proc (.start builder)
        out (future (slurp (.getInputStream proc)))
        err (future (slurp (.getErrorStream proc)))]
    (.waitFor proc 120 TimeUnit/SECONDS)
    (let [lines (remove str/blank? (str/split-lines @out))
          [outcome form] (frame/read-one (or (last lines) ""))]
      {:exit (.exitValue proc) :out (when (= :ok outcome) form) :lines lines :err @err})))

(defn- browser-ws-hello
  "Opens a WebSocket to `/line` as a browser (cookie + Origin, no Authorization)
  and reads the initial `:hello` frame."
  [url cookie]
  (let [ws-url (str (str/replace url #"^http" "ws") "/line")
        hello (promise)
        parts (StringBuilder.)
        listener (reify WebSocket$Listener
                   (onOpen [_ sock] (.request sock 1))
                   (onText [_ sock data last?]
                     (.append parts data)
                     (when last?
                       (let [[outcome form] (frame/read-one (.toString parts))]
                         (.setLength parts 0)
                         (when (and (= :ok outcome) (contains? form :hello))
                           (deliver hello (:hello form)))))
                     (.request sock 1)
                     nil)
                   (onError [_ _ err] (deliver hello {:error (str err)})))
        http-client (-> (HttpClient/newBuilder) (.connectTimeout (Duration/ofSeconds 10)) (.build))
        ^WebSocket sock (.join ^CompletableFuture
                               (-> (.newWebSocketBuilder http-client)
                                   (.header "Cookie" (str credential/cookie-name "=" cookie))
                                   (.header "Origin" url)
                                   (.buildAsync (URI/create ws-url) listener)))]
    (try
      (deref hello 5000 :timeout)
      (finally
        (try (.join (.sendClose sock WebSocket/NORMAL_CLOSURE "done")) (catch Throwable _ nil))))))

(defn- check-wire-word! []
  (let [ok-omitted (words/check {:word :open-session})
        ok-nil (words/check {:word :open-session :driven-by nil})
        ok-person (words/check {:word :open-session :driven-by {:kind :person}})
        ok-model (words/check {:word :open-session :driven-by {:kind :model :model "claude-opus-4-6"}})
        max-model (apply str (repeat words/max-text "m"))
        ok-max (words/check {:word :open-session :driven-by {:kind :model :model max-model}})
        bad-inputs [{:word :open-session :driven-by {:kind :robot}}
                    {:word :open-session :driven-by {:kind :person :extra 1}}
                    {:word :open-session :driven-by {:kind :model}}
                    {:word :open-session :driven-by {:kind :model :model ""}}
                    {:word :open-session :driven-by {:kind :model :model (str max-model "x")}}
                    {:word :open-session :driven-by {:kind :model :model "ok" :extra 1}}
                    {:word :open-session :driven-by "person"}
                    {:word :open-session :driven-by 42}]
        bads (mapv #(select-keys (words/check %) [:refuse :at]) bad-inputs)]
    (check "The wire word :open-session accepts :driven-by as {:kind :person}, {:kind :model :model text} up to 256 characters, or absent/nil, and turns away any other form."
           (and (contains? words/words :open-session)
                (contains? words/reasons :driven-by-has-no-form)
                (= {:ok {:word :open-session :driven-by nil}} ok-omitted)
                (= {:ok {:word :open-session :driven-by nil}} ok-nil)
                (= {:ok {:word :open-session :driven-by {:kind :person}}} ok-person)
                (= {:ok {:word :open-session :driven-by {:kind :model :model "claude-opus-4-6"}}} ok-model)
                (= {:ok {:word :open-session :driven-by {:kind :model :model max-model}}} ok-max)
                (every? #(= {:refuse :driven-by-has-no-form :at :driven-by} %) bads))
           {:ok [ok-omitted ok-nil ok-person ok-model] :bads bads})))

(defn -main [& _]
  (check-wire-word!)
  (let [cluster (main/in-process-cluster)
        stand-in (stand-in/make)
        port-a (free-port)
        port-b (free-port)
        url-a (str "http://localhost:" port-a)
        url-b (str "http://localhost:" port-b)
        copy-a (main/start {:cluster cluster :origin url-a :stand-in stand-in})
        copy-b (main/start {:cluster cluster :origin url-b :stand-in stand-in})
        served-a (main/serve! copy-a "127.0.0.1" port-a)
        served-b (main/serve! copy-b "127.0.0.1" port-b)
        first-person {:sub "os-1" :email "os1@example.com" :name "Session Person"}]
    (try
      (sign-in/setup! (:the-seam copy-a) {:email (:email first-person)})
      ;; Sign in without completing the name box first, so we can inspect GET /sign-in/name
      (let [signed-raw (drive/request :post (str url-a "/sign-in/stand-in?next=%2F")
                                      {:origin url-a
                                       :form {:sub (:sub first-person)
                                              :email (:email first-person)
                                              :name (:name first-person)}})
            cookie (:cookie signed-raw)
            {:keys [root sign-in]} (credential/parts cookie)
            name-box (drive/request :get (str url-a (:location signed-raw)) {:cookie cookie})
            box-session (some-> (second (re-find #"name=\"session\" value=\"([^\"]*)\"" (:body name-box)))
                                UUID/fromString)
            the-auth (seam/auth-of (:the-seam copy-a))
            box-who-is (when box-session
                         (auth/who-is the-auth {:root root :sign-in sign-in :session box-session}))
            ;; Complete name box so normal flows work cleanly
            box-act (second (re-find #"name=\"act\" value=\"([^\"]*)\"" (:body name-box)))
            _ (drive/request :post (str url-a "/sign-in/name?next=%2F")
                             {:cookie cookie :origin url-a
                              :form {:session (str box-session) :act box-act :name "Session Person"}})
            ;; Browser WebSocket face
            ws-hello (browser-ws-hello url-a cookie)
            ws-who-is (when (uuid? (:session ws-hello))
                        (auth/who-is the-auth {:root root :sign-in sign-in :session (:session ws-hello)}))]
        (check "The browser's faces (the name box on GET /sign-in/name and a cookie connection to /line) open sessions with :driven-by {:kind :person}, readable from who-is and reported in hello."
               (and (uuid? box-session)
                    (= {:kind :person} (get-in box-who-is [:session :driven-by]))
                    (uuid? (:session ws-hello))
                    (= {:kind :person} (:driven-by ws-hello))
                    (= {:kind :person} (get-in ws-who-is [:session :driven-by])))
               {:box-who-is box-who-is :ws-hello ws-hello :ws-who-is ws-who-is})

        ;; Visitor refused :open-session
        (let [vis (client/connect {:url url-a})
              _ (client/hello vis)
              vis-res (client/say vis {:word :open-session :driven-by {:kind :person}})]
          (client/close! vis)
          (check "A visitor cannot open a session with :open-session and is refused with :find-her-sign-in."
                 (= :find-her-sign-in (:could-not vis-res))
                 vis-res))

        ;; Signed-in program opens sessions with :open-session (model, person, omitted),
        ;; reads :driven-by from who-is across copies, and verifies immutability.
        (let [prog (drive/program url-a "open-session-prog" (fn [code] (drive/yes url-a cookie code {})))
              conn-a (client/connect {:url url-a :credential (:credential prog)})
              _ (client/hello conn-a)
              d-model {:kind :model :model "gemini-4-pro"}
              d-person {:kind :person}
              res-model (client/say conn-a {:word :open-session :driven-by d-model})
              res-person (client/say conn-a {:word :open-session :driven-by d-person})
              res-none (client/say conn-a {:word :open-session})
              ;; Attempt to overwrite :driven-by via repeat Auth open-session on res-model's session
              _ (when (uuid? (:open res-model))
                  (auth/open-session the-auth {:root (:root prog) :sign-in (:sign-in prog)
                                               :session (:open res-model)
                                               :until (+ (seam/now-ms) 100000)
                                               :at (seam/now-ms)
                                               :driven-by {:kind :person}}))
              conn-b-model (when (uuid? (:open res-model))
                             (client/connect {:url url-b :credential (:credential prog) :session (:open res-model)}))
              hello-b-model (when (and conn-b-model (not (:could-not conn-b-model))) (client/hello conn-b-model))
              who-b-model (when hello-b-model (client/say conn-b-model {:word :who-am-i}))
              conn-b-none (when (uuid? (:open res-none))
                            (client/connect {:url url-b :credential (:credential prog) :session (:open res-none)}))
              hello-b-none (when (and conn-b-none (not (:could-not conn-b-none))) (client/hello conn-b-none))]
          (when (and conn-b-model (not (:could-not conn-b-model))) (client/close! conn-b-model))
          (when (and conn-b-none (not (:could-not conn-b-none))) (client/close! conn-b-none))
          (client/close! conn-a)
          (check "A signed-in caller opens a session with {:word :open-session :driven-by D} (person, model, or omitted), gets {:open session :until time :driven-by D}, connects to it across copies, sees :driven-by in hello and :who-am-i from who-is, and :driven-by never changes after opening."
                 (and (uuid? (:open res-model)) (int? (:until res-model)) (= d-model (:driven-by res-model))
                      (uuid? (:open res-person)) (int? (:until res-person)) (= d-person (:driven-by res-person))
                      (uuid? (:open res-none)) (int? (:until res-none)) (contains? res-none :driven-by) (nil? (:driven-by res-none))
                      (= d-model (:driven-by hello-b-model))
                      (= d-model (:driven-by who-b-model))
                      (contains? hello-b-none :driven-by) (nil? (:driven-by hello-b-none)))
                 {:res-model res-model :res-person res-person :res-none res-none
                  :hello-b-model hello-b-model :who-b-model who-b-model :hello-b-none hello-b-none}))

        ;; Command line: new-session --model <name>
        (let [prog2 (drive/program url-a "cli-model-test" (fn [code] (drive/yes url-a cookie code {})))
              home (.toFile (Files/createTempDirectory "softland-open-sess-cli" (make-array FileAttribute 0)))
              file (io/file home "sign-in.edn")
              _ (spit file (str (frame/write {:url url-a :credential (:credential prog2)}) "\n"))
              initial (run-cli home "who")
              model-sess (run-cli home "new-session" "--model" "claude-opus-4-6")
              after-who (run-cli home "who")
              wrote (run-cli home "write" ":note-1" ":body" "hello")
              plain-sess (run-cli home "new-session")]
          (check "The command line opens a session with a model's name via `new-session --model <name>`, persists it, and reports {:kind :model :model <name>} on subsequent commands without changing it."
                 (and (= 0 (:exit initial))
                      (= 0 (:exit model-sess))
                      (uuid? (get-in model-sess [:out :session]))
                      (not= (get-in initial [:out :session]) (get-in model-sess [:out :session]))
                      (= {:kind :model :model "claude-opus-4-6"} (get-in model-sess [:out :driven-by]))
                      (= 0 (:exit after-who))
                      (= (get-in model-sess [:out :session]) (get-in after-who [:out :session]))
                      (= {:kind :model :model "claude-opus-4-6"} (get-in after-who [:out :driven-by]))
                      (= 0 (:exit wrote))
                      (= 0 (:exit plain-sess))
                      (not= (get-in model-sess [:out :session]) (get-in plain-sess [:out :session]))
                      (nil? (get-in plain-sess [:out :driven-by])))
                 {:initial (:out initial) :model-sess model-sess :after-who (:out after-who)
                  :wrote (:exit wrote) :plain-sess (:out plain-sess)})))
      (catch Throwable t
        (check "The open-session proof ran to its end without a throw." false (str t))
        (.printStackTrace t))
      (finally
        (try (jetty/stop! served-a) (catch Throwable _ nil))
        (try (jetty/stop! served-b) (catch Throwable _ nil))
        (try ((:stop copy-a)) (catch Throwable _ nil))
        (try ((:stop copy-b)) (catch Throwable _ nil))
        (try (.close ^Closeable cluster) (catch Throwable _ nil))))
    (finish)))
