(ns softland.runtime.drive
  "What the runtime's proofs share to drive a runtime as a browser and as a
  program would, over real requests. It requires the JDK's HTTP client,
  the wire's forms and the command line's client. It is no check: it holds
  none.

  A browser here is a cookie that the proof carries, and each post names
  the `Origin` that it is told to: what a real browser writes is not held
  by these proofs.

  Inputs and results, by call:

    request    a method, an address, and what goes with it -> {:status
               :location :cookie :body}
    sign-in    a runtime's address and an account -> {:cookie :person
               :status}: a sign-in through the stand-in's form, and the
               name box when the runtime sends her there
    yes        the address, a cookie, a code, and her answer -> the page's
               status
    program    the address, what the program says, and a function that
               answers the ask -> the credential's text, or what went wrong

    browser-line  the address and her cookie -> {:line :hello}: the line
               opened as her browser opens it, for what only she may say
               there; the command line's client speaks on it

  State. None."
  (:require [clojure.string :as str]
            [softland.command.client :as client]
            [softland.wire.credential :as credential]
            [softland.wire.frame :as frame])
  (:import [java.net URI URLEncoder]
           [java.net.http HttpClient HttpClient$Redirect HttpRequest HttpRequest$BodyPublishers
            HttpResponse HttpResponse$BodyHandlers]
           [java.time Duration]
           [java.util.concurrent CompletableFuture]))

(defn- encoded
  "Takes a map of fields. Gives the body of a form's post."
  [fields]
  (str/join "&" (for [[k v] fields]
                  (str (URLEncoder/encode (name k) "UTF-8") "=" (URLEncoder/encode (str v) "UTF-8")))))

(defn request
  "Makes one request and follows no redirect. Takes the method, `:get` or
  `:post`; the whole address; and a map: `:cookie`, a credential's text to
  send as the runtime's cookie; `:origin`, the text of the `Origin`
  header; `:form`, a map of fields for a form's post; `:edn`, a thing to
  post as one EDN form; `:headers`, more headers. Gives {:status :location
  :cookie :body}: `:cookie` is the credential that the answer set, the
  empty text when it took the cookie away, or nil."
  [method address {:keys [cookie origin form edn headers]}]
  (let [builder (-> (HttpRequest/newBuilder (URI/create address))
                    (.timeout (Duration/ofSeconds 60)))
        builder (reduce (fn [^java.net.http.HttpRequest$Builder b [k v]] (.header b k v))
                        builder
                        (cond-> (or headers {})
                          cookie (assoc "Cookie" (str credential/cookie-name "=" cookie))
                          origin (assoc "Origin" origin)
                          form (assoc "Content-Type" "application/x-www-form-urlencoded")
                          edn (assoc "Content-Type" "application/edn")))
        builder (case method
                  :get (.GET builder)
                  :post (.POST builder (HttpRequest$BodyPublishers/ofString
                                        (cond form (encoded form) edn (frame/write edn) :else ""))))
        client (-> (HttpClient/newBuilder) (.followRedirects HttpClient$Redirect/NEVER) (.build))
        ^HttpResponse response (.send client (.build builder) (HttpResponse$BodyHandlers/ofString))
        set-cookie (some (fn [text] (when (str/starts-with? text (str credential/cookie-name "=")) text))
                         (.allValues (.headers response) "set-cookie"))]
    {:status (.statusCode response)
     :location (.orElse (.firstValue (.headers response) "location") nil)
     :cookie (when set-cookie
               (subs (first (str/split set-cookie #";")) (inc (count credential/cookie-name))))
     :body (.body response)}))

(defn- paced
  "Makes a post to `/sign-in/*` again while the runtime paces this address:
  a page of 429, or a word `:take-more`. A proof posts many sign-ins from
  one address within the runtime's window, as a person behind one network
  could; the runtime says to wait, and the driver waits, as the command
  line does, for at most a minute. Gives the last answer."
  [post! paced?]
  (let [ends (+ (System/currentTimeMillis) 60000)]
    (loop []
      (let [got (post!)]
        (if (and (paced? got) (< (System/currentTimeMillis) ends))
          (do (Thread/sleep 1000) (recur))
          got)))))

(defn- take-more?
  [response]
  (= :take-more (:could-not (second (frame/read-one (:body response))))))

(defn- hidden
  "Takes a page's HTML and a field's name. Gives the value of the hidden
  field of that name, or nil."
  [html field]
  (second (re-find (re-pattern (str "name=\"" field "\" value=\"([^\"]*)\"")) html)))

(defn sign-in
  "Signs an account in at a runtime that runs with the stand-in for Google,
  as a browser would. Takes the runtime's address and a map: `:sub`, the
  account's number; `:email`; `:name`, what the provider calls her;
  `:invited`, an invitation's code, or nil; `:call-her`, the name that she
  confirms in the box when the runtime sends her there, or nil to confirm
  what the box offers. Gives {:status the status of the sign-in's answer,
  :cookie the credential, :person her id, :named? whether she went through
  the box, :offered what the box offered}; with no `:cookie` when the
  sign-in was not taken."
  [url {:keys [sub email name invited call-her]}]
  (let [signed (paced #(request :post (str url "/sign-in/stand-in?next=%2F" (when invited (str "&invited=" invited)))
                                 {:origin url :form (cond-> {:sub sub}
                                                      email (assoc :email email)
                                                      name (assoc :name name))})
                       (fn [got] (= 429 (:status got))))
        cookie (:cookie signed)]
    (if-not (seq cookie)
      {:status (:status signed) :body (:body signed)}
      (let [person (:root (credential/parts cookie))
            to-name? (boolean (some-> (:location signed) (str/starts-with? "/sign-in/name")))]
        (if-not to-name?
          {:status (:status signed) :cookie cookie :person person :named? false}
          (let [box (request :get (str url (:location signed)) {:cookie cookie})
                offered (second (re-find #"name=\"name\"[^>]*value=\"([^\"]*)\"" (:body box)))
                posted (request :post (str url "/sign-in/name?next=%2F")
                                {:cookie cookie :origin url
                                 :form {:act (hidden (:body box) "act")
                                        :name (or call-her offered)}})]
            {:status (:status signed) :cookie cookie :person person :named? true
             :offered offered :name-status (:status posted)}))))))

(def default-agent
  "The name of the new agent that a yes makes when the proof names none."
  "a program")

(defn yes-form
  "The fields of her post on the yes page. Takes the code and a map:
  `:say`, \"yes\" or \"no\"; `:agent`, the name of a new agent; `:mine`,
  the id of an agent of hers to sign in again; `:as-herself`, true to post
  a yes as herself, which the page no longer offers and refuses; with none
  of the three, a new agent called `default-agent`, as the page offers
  first. `:see` and `:write` say whether each box is ticked: both for a new
  agent and neither for one of hers when left out, as the page shows them."
  [code {:keys [say agent mine as-herself see write]}]
  (let [ticked? (fn [given by-default] (if (some? given) given by-default))
        agent (when-not (or mine as-herself) (or agent default-agent))]
    (cond-> {:code code :say (or say "yes") :as (cond agent "new-agent" mine "my-agent" :else "me")}
      agent (assoc :label agent)
      mine (assoc :agent (str mine))
      (ticked? see (some? agent)) (assoc :see "yes")
      (ticked? write (some? agent)) (assoc :write "yes"))))

(defn yes
  "Answers a program's ask as a signed-in person. Takes the runtime's
  address; her cookie; the code; and a map, as `yes-form` takes it, with
  `:origin`, the `Origin` to write, the runtime's own when left out. Gives
  the page's status."
  [url cookie code {:keys [origin] :as answer}]
  (:status (request :post (str url "/yes")
                    {:cookie cookie :origin (or origin url) :form (yes-form code answer)})))

(defn program
  "Signs a program in, by the two requests themselves and not through the
  command line's library. Takes the runtime's address, what the program
  says of itself, and a function of the ask's code that makes somebody
  answer it. Gives {:credential text :root :actor :sign-in}, or what the
  runtime said when the answer was no yes."
  [url asks answer!]
  (let [secret (credential/make-secret)
        asking-secret (credential/make-secret)
        asked (:body (paced #(request :post (str url "/sign-in/program")
                                      {:edn {:hash (credential/hash-of secret)
                                             :asking-hash (credential/hash-of asking-secret) :asks asks}})
                            take-more?))
        [_ asked] (frame/read-one asked)]
    (if-not (:code asked)
      asked
      (do (answer! (:code asked))
          (let [[_ got] (frame/read-one (:body (paced #(request :post (str url "/sign-in/program/poll")
                                                                {:edn {:code (:code asked) :asking-secret asking-secret}})
                                                      take-more?)))]
            (if (= :yes (:state got))
              (assoc got :credential (credential/text (:root got) (:sign-in got) secret))
              got))))))

(defn browser-line
  "Opens the line at an address as a browser does, with the cookie and the
  runtime's own `Origin`: past the library's `connect`, which carries a
  credential in a header alone, and onto the library's own listener, so
  that the library's `say` speaks on it. Gives {:line the connection
  :hello what the runtime said first}."
  [url cookie]
  (let [state (atom {:hello (promise) :next 0 :waits {} :watches {}})
        socket (.join ^CompletableFuture
                      (-> (.newWebSocketBuilder (HttpClient/newHttpClient))
                          (.header "Cookie" (str credential/cookie-name "=" cookie))
                          (.header "Origin" url)
                          (.buildAsync (URI/create (str (str/replace url #"^http" "ws") "/line"))
                                       (#'client/listener state))))
        connection (client/->Connection socket state)]
    {:line connection :hello (client/hello connection)}))
