(ns softland.runtime.drive
  "What the runtime's proofs share to drive a runtime as a browser and as a
  program would, over real requests. It requires the JDK's HTTP client and
  the wire's forms. It is no check: it holds none.

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

  State. None."
  (:require [clojure.string :as str]
            [softland.wire.credential :as credential]
            [softland.wire.frame :as frame])
  (:import [java.net URI URLEncoder]
           [java.net.http HttpClient HttpClient$Redirect HttpRequest HttpRequest$BodyPublishers
            HttpResponse HttpResponse$BodyHandlers]
           [java.time Duration]))

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
  (let [signed (request :post (str url "/sign-in/stand-in?next=%2F" (when invited (str "&invited=" invited)))
                        {:origin url :form (cond-> {:sub sub}
                                             email (assoc :email email)
                                             name (assoc :name name))})
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

(defn yes
  "Answers a program's ask as a signed-in person. Takes the runtime's
  address; her cookie; the code; and a map: `:say`, \"yes\" or \"no\";
  `:agent`, the name of a new agent, or nil to sign the program in as
  herself; `:origin`, the `Origin` to write, the runtime's own when left
  out. Gives the page's status."
  [url cookie code {:keys [say agent origin]}]
  (:status (request :post (str url "/yes")
                    {:cookie cookie :origin (or origin url)
                     :form (cond-> {:code code :say (or say "yes") :as (if agent "agent" "me")}
                             agent (assoc :label agent))})))

(defn program
  "Signs a program in. Takes the runtime's address, what the program says
  of itself, and a function of the ask's code that makes somebody answer
  it. Gives {:credential text :root :actor :sign-in}, or what the runtime
  said when the answer was no yes."
  [url asks answer!]
  (let [secret (credential/make-secret)
        asked (:body (request :post (str url "/sign-in/program")
                              {:edn {:hash (credential/hash-of secret) :asks asks}}))
        [_ asked] (frame/read-one asked)]
    (if-not (:code asked)
      asked
      (do (answer! (:code asked))
          (let [[_ got] (frame/read-one (:body (request :post (str url "/sign-in/program/poll")
                                                        {:edn {:code (:code asked) :secret secret}})))]
            (if (= :yes (:state got))
              (assoc got :credential (credential/text (:root got) (:sign-in got) secret))
              got))))))
