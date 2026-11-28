(ns softland.runtime.pure-proof
  "The proof of the runtime's pure part, and of what the source of the
  runtime, of the wire and of the command line may hold. It needs no
  cluster. One command, from the repository root:

    clojure -M:runtime -m softland.runtime.pure-proof

  It checks the check of an ID token (`softland.runtime.google`) against
  tokens that a stand-in signs: the checks of the server's pure proof of 20
  November 2026, carried as they were.

  Then it reads the source and holds what the trees' pages say of their
  boundaries: which files may reach the store's door, its read path and
  Auth's client; that one place calls the door's `send-act`; that the
  system's acts are sent from one file; that the wire and the command line
  can open no cluster; and that nothing requires Electric, the rig, the
  server of 20 November, the old server or Inland.

  Then it holds who may say each word: what the seam answers a visitor, an
  agent and a person, in a session and in none, for each word of the wire's
  table, against a list that is written out here a second time. Then the
  hash by which Auth is handed an email, and that no address is handed it
  under `:email`. And who
  may end an agent, against a list of actors in which whose ending ends an
  agent is not who made it.

  State. It changes nothing outside its own process."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [softland.runtime.google :as google]
            [softland.runtime.google-stand-in :as stand-in]
            [softland.runtime.seam :as seam]
            [softland.wire.proof :refer [check finish]]
            [softland.wire.words :as words])
  (:import [java.util UUID]))

;; -------------------------------------------------------- the check of an ID token

(defn- google-checks []
  (let [google (stand-in/make)
        other (stand-in/make)
        now 1795000000000
        options {:client-id (:client-id google) :issuers #{(:issuer google)} :keys (:keys google) :now-ms now}
        good (stand-in/token google {"sub" "1082" "email" "sid@example.com"} now)
        refuse (fn [token] (:refuse (google/check token options)))
        b64 (fn [^String text] (.encodeToString (.withoutPadding (java.util.Base64/getUrlEncoder)) (.getBytes text "UTF-8")))
        [header payload signature] (str/split good #"\.")]
    (check "A token that the issuer signed, for this server, in its time, with a subject, passes, and its claims come back under the issuer's own names."
           (= ["1082" "sid@example.com"] (mapv #(get (:ok (google/check good options)) %) ["sub" "email"])))
    (check "A token that names no algorithm, or a shared secret's, is turned away before any key is looked at."
           (= [:wrong-algorithm :wrong-algorithm]
              [(refuse (str (b64 "{\"alg\":\"none\",\"kid\":\"softland-stand-in-1\"}") "." payload "."))
               (refuse (str (b64 "{\"alg\":\"HS256\",\"kid\":\"softland-stand-in-1\"}") "." payload "." signature))]))
    (check "A token whose claims were changed after it was signed is turned away."
           (= :signature-wrong
              (refuse (str header "." (b64 "{\"sub\":\"9999\",\"iss\":\"https://google-stand-in.softland.invalid\",\"aud\":\"softland-stand-in\",\"exp\":99999999999}") "." signature))))
    (check "A token that another key pair signed under the same key name is turned away."
           (= :signature-wrong (refuse (stand-in/token other {"sub" "1082"} now))))
    (check "A token whose key name the issuer does not publish is turned away."
           (= :key-not-known (:refuse (google/check good (assoc options :keys {"another" (val (first (:keys google)))})))))
    (check "A token of another issuer, for another audience, past its time, or with no subject is turned away, each with its word."
           (= [:wrong-issuer :wrong-audience :wrong-audience :expired :expired :no-subject :no-subject]
              [(refuse (stand-in/token google {"sub" "1" "iss" "https://accounts.example"} now))
               (refuse (stand-in/token google {"sub" "1" "aud" "another-client"} now))
               (refuse (stand-in/token google {"sub" "1" "aud" [(:client-id google) "another-client"]} now))
               (refuse (stand-in/token google {"sub" "1" "exp" (quot now 1000)} now))
               (refuse (stand-in/token google {"sub" "1" "exp" "tomorrow"} now))
               (refuse (stand-in/token google {} now))
               (refuse (stand-in/token google {"sub" ""} now))]))
    (check "A text that is no token is turned away and does not throw."
           (every? #(= :not-a-token %) (map refuse [nil "" "a.b" "a.b.c" "a.b.c.d" 7 (str header "." payload)])))
    (check "The name of the key that a token asks for is given only for a text of three parts whose header names RS256 and a key: a face fetches the issuer's keys for no other text."
           (= ["softland-stand-in-1" nil nil nil nil]
              [(google/key-name good)
               (google/key-name (str (b64 "{\"alg\":\"none\",\"kid\":\"softland-stand-in-1\"}") "." payload "."))
               (google/key-name (str (b64 "{\"alg\":\"RS256\"}") "." payload "." signature))
               (google/key-name "x")
               (google/key-name nil)]))
    (check "The keys of a JWKS document are read by key name, and a text that is none gives no key."
           (and (= {} (google/keys-of "not json")) (= {} (google/keys-of "{\"keys\":[{\"kty\":\"EC\",\"kid\":\"x\"}]}"))))))


;; --------------------------------------------------------- what the source holds

(defn- source-files
  "Takes a folder. Gives each Clojure file under it, as [its path, its
  text]."
  [folder]
  (for [^java.io.File file (file-seq (io/file folder))
        :when (and (.isFile file) (re-find #"\.clj[cs]?$" (.getName file)))]
    [(.getPath file) (slurp file)]))

(defn- code-of
  "Takes the text of a Clojure file. Gives it without its docstrings and
  comments, roughly: each string and each comment is taken out, so that a
  name which a docstring only speaks of is not counted as a call."
  [text]
  (-> text
      (str/replace #"(?s)\"(?:\\.|[^\"\\])*\"" "\"\"")
      (str/replace #";[^\n]*" "")))

(defn- source-checks []
  (let [runtime (source-files "src-runtime")
        wire (source-files "src-wire")
        command (source-files "src-command")
        holding (fn [files pattern] (sort (for [[path text] files :when (re-find pattern (code-of text))] path)))
        in (fn [& names] (mapv (fn [n] (str "src-runtime/softland/runtime/" n ".clj")) names))
        seam-code (code-of (slurp "src-runtime/softland/runtime/seam.clj"))
        main-code (code-of (slurp "src-runtime/softland/runtime/main.clj"))]
    (check "The source is read from src-runtime, src-wire and src-command, so this proof ran at the repository root."
           (and (< 5 (count runtime)) (< 3 (count wire)) (< 1 (count command)))
           (mapv count [runtime wire command]))
    (check "Two files of the runtime require the store's door and its read path: the seam, and the program that opens them. No other file can call either, under any name."
           (= [(in "main" "seam") (in "main" "seam")]
              [(holding runtime #"softland\.store\.door(?![.\w-])") (holding runtime #"softland\.store\.read-path(?![.\w-])")])
           [(holding runtime #"softland\.store\.door(?![.\w-])") (holding runtime #"softland\.store\.read-path(?![.\w-])")])
    (check "The program that opens the door and the read path calls nothing of them but their opening."
           (= #{"door/open" "read-path/open"}
              (set (re-seq #"(?<![\w.-])(?:door|read-path)/[\w!?*-]+" main-code)))
           (set (re-seq #"(?<![\w.-])(?:door|read-path)/[\w!?*-]+" main-code)))
    (check "The seam calls the door's send-act in one place: no act reaches a depot but through the one function behind which each sender's act has been held against its sender."
           (= 1 (count (re-seq #"door/send-act" seam-code)))
           (count (re-seq #"door/send-act" seam-code)))
    (check "Four files of the runtime require Auth's client: the program that opens it, the seam, the standing, and the file that writes what sign-in writes. No face requires it."
           (= (in "main" "seam" "sign_in" "standing") (holding runtime #"softland\.auth\.client"))
           (holding runtime #"softland\.auth\.client"))
    (check "The runtime opens no table and no depot of Rama's itself: what it reads of the store, it reads through the read path, and what it keeps, it keeps through Auth's client."
           (empty? (holding runtime #"foreign-pstate|foreign-select|foreign-proxy|foreign-depot|foreign-append|foreign-query|foreign-invoke"))
           (holding runtime #"foreign-pstate|foreign-select|foreign-proxy|foreign-depot|foreign-append|foreign-query|foreign-invoke"))
    (check "The acts and the reads of the system go out from one file beside the seam, the one that writes what sign-in writes: no face can send as the system. A face reaches the system's steps only by calling that file's functions."
           (= (in "sign_in") (remove (set (in "seam")) (holding runtime #"send-as-system|read-as-system")))
           (holding runtime #"send-as-system|read-as-system"))
    (check "The seam writes no who and no session into an act, and no asker into a query: it holds no call that puts one of those parts into a map."
           (empty? (re-seq #"assoc\s+\S+\s+:(?:who|session)\b|assoc\s+(?:act|query)\s+:asker" seam-code))
           (re-seq #"assoc\s+\S+\s+:(?:who|session)\b|assoc\s+(?:act|query)\s+:asker" seam-code))
    (check "The wire and the command line require no Rama, nothing of the runtime and nothing of Auth: neither can open a cluster, and a program that holds them speaks to the runtime alone."
           (empty? (holding (concat wire command) #"com\.rpl|softland\.runtime|softland\.auth"))
           (holding (concat wire command) #"com\.rpl|softland\.runtime|softland\.auth"))
    (check "The wire requires nothing of the command line."
           (empty? (holding wire #"softland\.command")) (holding wire #"softland\.command"))
    (check "No file of the three trees requires Electric: the seam and the words are called by every face."
           (empty? (holding (concat runtime wire command) #"hyperfiddle"))
           (holding (concat runtime wire command) #"hyperfiddle"))
    (check "No file of the three trees requires the rig, the server of 20 November 2026, the old server or Inland."
           (empty? (holding (concat runtime wire command) #"\[rig\.|\[app\.|softland\.inland|softland\.server"))
           (holding (concat runtime wire command) #"\[rig\.|\[app\.|softland\.inland|softland\.server"))
    (check "No file of the three trees reads an environment file."
           (empty? (holding (concat runtime wire command) #"env\.clj|\.env\b"))
           (holding (concat runtime wire command) #"env\.clj|\.env\b"))))

;; ------------------------------------------------------ who may say each word

(def ^:private who-says
  "For each word of the wire, what the seam answers before it does anything
  of the word: nil when the caller may say it, or the runtime's word. In
  order: a visitor; an agent in a session; a person at a browser in no
  session; a person at a browser in a session. Written out here as literals, a second time beside the
  wire's table, so that a change of who may say a word is made in two
  places on purpose."
  {:who-am-i       [nil               nil                  nil               nil]
   :send           [:find-her-sign-in nil                  :find-her-session nil]
   :send-all       [:find-her-sign-in nil                  :find-her-session nil]
   :answer         [:find-her-sign-in nil                  nil               nil]
   :read           [nil               nil                  nil               nil]
   :watch          [nil               nil                  nil               nil]
   :leave          [nil               nil                  nil               nil]
   :replace-secret [:find-her-sign-in nil                  nil               nil]
   :sign-out       [:find-her-sign-in nil                  nil               nil]
   :open-session   [:find-her-sign-in nil                  nil               nil]
   :close-session  [:find-her-sign-in nil                  :find-her-session nil]
   :invite         [:find-her-sign-in :do-that-as-an-agent nil               nil]
   :take-back-invite [:find-her-sign-in :do-that-as-an-agent :find-her-session nil]
   :accounts       [:find-her-sign-in :do-that-as-an-agent :find-her-session nil]
   :unlink-account [:find-her-sign-in :do-that-as-an-agent :find-her-session nil]
   :make-agent     [:find-her-sign-in nil                  nil               nil]
   :end-agent      [:find-her-sign-in nil                  nil               nil]
   :under          [:find-her-sign-in :do-that-as-an-agent nil               nil]
   :end-sign-in    [:find-her-sign-in :do-that-as-an-agent nil               nil]})

(defn- who-says-checks []
  (let [root (UUID/randomUUID)
        agent (UUID/randomUUID)
        made (fn [actor session kind]
               (#'seam/->Caller (keyword (str (if (= actor root) "person." "agent.") actor))
                                actor root (UUID/randomUUID) session kind (atom "") 0 root nil nil))
        callers [(seam/visitor) (made agent (UUID/randomUUID) :program) (made root nil :browser) (made root (UUID/randomUUID) :browser)]
        ;; Auth begins no sign-in of kind `:program` for her; the seam holds it too.
        her-root-as-a-program (made root (UUID/randomUUID) :program)
        answered (into {}
                       (map (fn [word] [word (mapv (fn [caller] (:could-not (seam/word-fault caller word))) callers)]))
                       (keys words/words))
        seam-code (code-of (slurp "src-runtime/softland/runtime/seam.clj"))
        named (fn [f] (count (re-seq (re-pattern (str "(?<![\\w-])" f "(?![\\w-])")) seam-code)))]
    (check "For each word of the wire's table, the seam answers a visitor, an agent in a session, a person in no session and a person in a session as the list of this proof says: `:find-her-sign-in` for a visitor at each word but `:who-am-i`, `:read`, `:watch` and `:leave`; `:do-that-as-an-agent` for an agent at `:invite`, `:take-back-invite`, `:accounts`, `:unlink-account`, `:under` and `:end-sign-in`; `:find-her-session` for a caller in no session at `:send`, `:send-all`, `:close-session`, `:take-back-invite`, `:accounts` and `:unlink-account`."
           (= who-says answered)
           (into {} (remove (fn [[word got]] (= got (get who-says word)))) answered))
    (check "No program is a person: a caller in a session whose actor is her root, on a sign-in of a program's, is answered at each word of the table as an agent in a session is, so `:do-that-as-an-agent` at each word that the table gives to a person alone; `person?` says no of it, and yes of her at a browser."
           (let [as-it (into {} (map (fn [word] [word (:could-not (seam/word-fault her-root-as-a-program word))])) (keys words/words))]
             (and (= (update-vals who-says second) as-it)
                  (not (seam/person? her-root-as-a-program))
                  (seam/person? (nth callers 3))))
           (into {} (map (fn [word] [word (:could-not (seam/word-fault her-root-as-a-program word))])) (keys words/words)))
    (check "A word that the table does not hold is held back by nothing here: the seam's `call` then says that it does not know the word."
           (nil? (seam/word-fault (seam/visitor) :no-such-word)))
    (check "The seam holds a word against its caller in one place: each of its three tests of a caller is named twice in its code, where it is defined and in the one function that reads the table, and no word of `call` names one."
           (= [2 2 2] (mapv named ["needs-a-who" "needs-a-session" "needs-the-person"]))
           (mapv named ["needs-a-who" "needs-a-session" "needs-the-person"]))))

(defn- email-checks []
  (check "Auth is handed an email as its hash, made in one place: SHA-256, in 64 lower-case hex characters, of `softland/email/` and the address lower-cased in the root locale, so one address in other letters gives one hash. The value here was made outside the JVM, with `sha256sum`."
         (and (= "3ffd44dc74bd01c0d90f733d07c6727eba012689ce82ff1c6ca8eb7a8ae98cb9"
                 (seam/email-hash "asha@example.com") (seam/email-hash "Asha@EXAMPLE.com"))
              (not= (seam/email-hash "asha@example.com") (seam/email-hash "asha@example.org")))
         (seam/email-hash "Asha@EXAMPLE.com"))
  (let [keyed (fn [file] (re-seq #"(?<!:by |\(= ):email(?![\w-])" (code-of (slurp file))))]
    (check "No address reaches Auth: in the two files that call Auth's invite, take-back and enter, `:email` is only ever the kind of a claim, never a key that carries an address."
           (= [nil nil] (mapv keyed ["src-runtime/softland/runtime/sign_in.clj" "src-runtime/softland/runtime/seam.clj"]))
           (mapv keyed ["src-runtime/softland/runtime/sign_in.clj" "src-runtime/softland/runtime/seam.clj"]))))

(defn- who-ends-an-agent-checks []
  (let [[person a1 a2 a3] (repeatedly 4 #(UUID/randomUUID))
        ;; No write of Auth points an agent at another actor than its maker,
        ;; so the list in which the two differ is made by hand, in the form
        ;; that `under` gives.
        actors [{:actor a1 :made-by person :ends-with person}
                {:actor a2 :made-by a1 :ends-with person}
                {:actor a3 :made-by person :ends-with a1}]
        beneath? #'seam/beneath?]
    (check "Who may end an agent is read from whose ending ends it, at any depth, and not from who made it: an agent that agent 1 made and that ends with the person is not agent 1's to end, an agent that the person made and that ends with agent 1 is, and both are the person's."
           (= [false true true true]
              [(beneath? actors a2 a1) (beneath? actors a3 a1) (beneath? actors a2 person) (beneath? actors a3 person)]))))

(defn -main
  "Runs every check, prints the counts, and ends the process with a status."
  [& _]
  (google-checks)
  (source-checks)
  (who-says-checks)
  (email-checks)
  (who-ends-an-agent-checks)
  (finish))
