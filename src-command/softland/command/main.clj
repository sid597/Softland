(ns softland.command.main
  "The command line: Softland from a terminal, for a person and for an
  agent. It is Clojure on the JVM, and it requires the client library
  (`softland.command.client`) and the wire's forms, which rest on the
  store's form of an act. A person and an agent use the same commands: an agent is signed in
  with a who of its own, and what it may do is what it was given.

  The commands:

    sign-in [--url address] [--agent name]
                        asks the runtime, shows a link and a code, waits for
                        her yes, and keeps the credential. With `--agent`
                        the yes page offers to make this program an agent
                        of that name
    who                 who this program is signed in as
    write e k v         one fact, written into a layer
    read e k            one place, read once
    watch e k           one place, kept live until the program is stopped
    send file           the acts of a file, sent in order, each once
    give who layer see|act
                        gives a permission beneath the one that this
                        program acts under
    invite [--email a]  makes a person who waits for an account; prints the
                        link for her, or names the email
    agent new name [--out file]
                        makes an agent beneath this program's who, and
                        writes the file that the agent signs in with
    agent end uuid      ends an agent
    under               the actors and the sign-ins of this person
    end-sign-in uuid    ends one sign-in of this person
    replace-secret      a new secret for this sign-in
    new-session         leaves the session and starts another
    sign-out            ends this program's sign-in

  Common arguments: `--as file`, the file that holds the sign-in, when it
  is not the default; `--layer id`, the layer, when it is not the person's
  own; `--permission edn`, the permission that an act names, when it is
  not the first permission of the person's own layer.

  Values. `e`, `k`, `v`, a layer and a permission are read as EDN, so a
  keyword, a set and a number of any width are typed as they are; a UUID
  is typed bare, or as EDN writes one; a text that is no EDN, or a bare
  word, is taken as a string.

  What it keeps. One file, `sign-in.edn` under `$SOFTLAND_HOME` or
  `~/.config/softland`, readable by its owner alone: the runtime's address,
  the credential, and the id of the session that it is in. It keeps
  nothing else between two runs: who it is and where the person's own
  layer is come from the runtime's hello each time. `send` keeps the UUIDs
  of a file's acts beside the file, so that a run that was cut off sends
  each act again under its own UUID and the store takes it once.

  The session. One sitting is one session: the commands of one sign-in
  share it until `new-session`. A run of an agent that wants a session of
  its own says `new-session` first.

  What it prints, and how it ends. One EDN form on standard output: the
  result. The exit status is 0 when the thing was done; 1 when the runtime
  or the library gave a word that holds `:could-not`; 3 when a gate
  refused an act; 2 for a command that it does not know.

  State. The file, and for `send` the file of UUIDs."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [softland.command.client :as client]
            [softland.wire.credential :as credential]
            [softland.wire.frame :as frame])
  (:import [java.io File PushbackReader]
           [java.nio.charset StandardCharsets]
           [java.nio.file CopyOption Files StandardCopyOption]
           [java.nio.file.attribute FileAttribute PosixFilePermissions]
           [java.util UUID]))

(def default-url
  "The runtime's address when none was given."
  "http://localhost:8500")

(def ^:private replace-under-ms
  "With less than this left of a secret's time, a command replaces the
  secret before it does its work: two days. A number of the build."
  (* 2 24 60 60 1000))

;; ------------------------------------------------------------------- the file

(defn- home
  "Gives the folder that holds the command line's file."
  ^File []
  (io/file (or (System/getenv "SOFTLAND_HOME")
               (io/file (System/getProperty "user.home") ".config" "softland"))))

(defn- file-of
  "Takes the arguments. Gives the file that holds the sign-in: `--as`, or
  the default."
  ^File [{:keys [as]}]
  (if as (io/file as) (io/file (home) "sign-in.edn")))

(defn- kept
  "Takes a file. Gives the map that it holds, or nil."
  [^File file]
  (when (.isFile file)
    (let [[outcome form] (frame/read-one (slurp file))]
      (when (and (= :ok outcome) (map? form)) form))))

(defn- keep!
  "Writes a map atomically into a file created with `rw-------` permissions
  from birth (no world-readable window)."
  [^File file thing]
  (let [parent (or (.getParentFile (.getAbsoluteFile file)) (io/file "."))
        _ (.mkdirs parent)
        target (.toPath (.getAbsoluteFile file))
        bytes (.getBytes (str (frame/write thing) "\n") StandardCharsets/UTF_8)
        attrs (try (into-array FileAttribute [(PosixFilePermissions/asFileAttribute
                                               (PosixFilePermissions/fromString "rw-------"))])
                   (catch Throwable _ (make-array FileAttribute 0)))
        tmp (Files/createTempFile (.toPath parent) ".softland-" ".tmp" attrs)]
    (try
      (Files/write tmp bytes (make-array java.nio.file.OpenOption 0))
      (try (Files/move tmp target
                       (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING
                                               StandardCopyOption/ATOMIC_MOVE]))
           (catch Throwable _
             (Files/move tmp target
                         (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING]))))
      (catch Throwable t
        (try (Files/deleteIfExists tmp) (catch Throwable _ nil))
        (throw t))))
  nil)

;; ------------------------------------------------------------- the arguments

(defn- parsed
  "Takes the arguments after the command. Gives {:flags {...} :rest [...]}:
  each `--name value` under `:name`, and the others in order."
  [args]
  (loop [args args flags {} others []]
    (if-let [[arg & more] (seq args)]
      (if (str/starts-with? arg "--")
        (recur (rest more) (assoc flags (keyword (subs arg 2)) (first more)) others)
        (recur more flags (conj others arg)))
      {:flags flags :rest others})))

(defn- value-of
  "Takes a text as it was typed. Gives the value that it is: a UUID for a
  text that has the form of one, bare; else the EDN value that the text
  reads as; and for a text that is no EDN, or a bare symbol, the string
  itself."
  [text]
  (if (re-matches #"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" (str text))
    (UUID/fromString text)
    (let [[outcome form] (frame/read-one text)]
      (if (and (= :ok outcome) (not (symbol? form))) form text))))

;; ------------------------------------------------------------------ the result

(defn- status-of
  "Takes a result. Gives the exit status that it means."
  [result]
  (cond
    (and (map? result) (:could-not result)) 1
    (= :refused (get-in result [:answer :answer])) 3
    (and (map? result) (some (fn [r] (or (:could-not r) (= :refused (get-in r [:answer :answer]))))
                             (:results result))) 3
    :else 0))

(defn- out!
  "Prints a result as one EDN form. Gives its exit status."
  [result]
  (println (frame/write result))
  (status-of result))

;; ----------------------------------------------------------------- connecting

(defn- with-line
  "Opens the line as the kept sign-in, calls `f` with the connection and
  the runtime's hello, closes the line, and gives what `f` gave. A sign-in
  that the runtime gave a new session keeps it. A secret that has little
  time left is replaced first (unless `:auto-replace?` is false), and the new
  credential is kept before the old one is let go. Gives a word when there is
  no sign-in or the line could not be opened."
  ([flags f] (with-line flags f true))
  ([flags f auto-replace?]
   (let [file (file-of flags)
         {:keys [url credential session] :as held} (kept file)]
     (if-not credential
       {:could-not :find-the-sign-in :said "This program is not signed in. Run: sign-in"}
       (let [connection (client/connect {:url url :credential credential :session session})]
         (if (:could-not connection)
           (cond-> connection
             (= 401 (:status connection)) (assoc :said "The sign-in stands for nobody now, or its session is closed. Run: sign-in"))
           (let [hello (client/hello connection)]
             (try
               (if (:could-not hello)
                 hello
                 (do
                   (when (not= session (:session hello))
                     (keep! file (assoc held :session (:session hello))))
                   (when (and auto-replace?
                              (:until hello)
                              (< (- (:until hello) (System/currentTimeMillis)) replace-under-ms))
                     (let [{:keys [root sign-in]} hello
                           secret (credential/make-secret)
                           replaced (client/say connection {:word :replace-secret :hash (credential/hash-of secret)})]
                       (when (:replaced replaced)
                         (keep! file (assoc held :session (:session hello)
                                            :credential (credential/text root sign-in secret))))))
                   (f connection hello)))
               (finally (client/close! connection))))))))))

(defn- act-of
  "Builds one act of this program. Takes the hello, the flags, and the
  facts. Gives the act, whole: the who and the session are the hello's;
  the layer is `--layer` or the person's own; the permission is
  `--permission`, or the first permission of that layer for its person."
  [hello {:keys [layer permission]} facts]
  (let [layer (if layer (value-of layer) (:own-layer hello))]
    {:version 1
     :who (:who hello)
     :session (:session hello)
     :layer layer
     :permission (if permission (value-of permission) [(:who hello) layer layer])
     :facts facts}))

(defn- query-of
  "Builds the query of one place. Takes the hello, the flags, an entity
  and a key."
  [hello {:keys [layer]} e k]
  {:asks [:place e k]
   :asker (:who hello)
   :layers [(if layer (value-of layer) (:own-layer hello))]})

;; ---------------------------------------------------------------- the commands

(defn- sign-in!
  "The command `sign-in`."
  [{:keys [url agent] :as flags}]
  (let [url (or url (:url (kept (file-of flags))) default-url)
        asks (str (if agent (str "the agent \"" agent "\"") "the command line")
                  " on " (try (.getHostName (java.net.InetAddress/getLocalHost)) (catch Throwable _ "this machine")))
        asked (client/ask url asks)]
    (if (:could-not asked)
      asked
      (do
        (binding [*out* *err*]
          (println "Open this link where you are signed in, and say yes only if it shows the same code:")
          (println " " (:link asked))
          (println "  code:" (:shown asked))
          (when agent (println (str "  On that page, choose \"an agent of its own\" and call it: " agent))))
        (loop []
          (let [got (client/waited url (:code asked) (:secret asked))]
            (case (:state got)
              :waiting (do (Thread/sleep 1000) (recur))
              :yes (do (keep! (file-of flags) {:url url :credential (:credential got)})
                       (with-line flags (fn [_ hello] hello)))
              got)))))))

(defn- send-file!
  "The command `send`: the acts of a file, in order, each once. The file
  holds EDN maps, one after another; each is an act without `:who` and
  `:session`, which are this program's. A UUID is made for each act and
  kept in `<file>.sent.edn` before anything is sent, so a run that was cut
  off sends the same acts again under the same UUIDs."
  [connection hello path]
  (let [file (io/file path)
        acts (with-open [reader (PushbackReader. (io/reader file))]
               (doall (take-while (fn [form] (not= ::end form))
                                  (repeatedly (fn [] (edn/read {:eof ::end} reader))))))
        journal (io/file (str path ".sent.edn"))
        uuids (let [held (:uuids (kept journal))]
                (if (= (count held) (count acts))
                  held
                  (let [made (vec (repeatedly (count acts) (fn [] (UUID/randomUUID))))]
                    (keep! journal {:uuids made :session (:session hello)})
                    made)))]
    (client/say connection
                {:word :send-all
                 :acts (mapv (fn [act uuid]
                               {:act (assoc act :who (:who hello) :session (:session hello)) :act-id uuid})
                             acts uuids)})))

(defn- run
  "Runs one command. Takes the command and its arguments. Gives the
  result."
  [command args]
  (let [{flags :flags [a b c] :rest} (parsed args)]
    (case command
      "sign-in" (sign-in! flags)

      "who" (with-line flags (fn [_ hello] hello))

      "write"
      (with-line flags
        (fn [connection hello]
          (client/say connection {:word :send
                                  :act (act-of hello flags [{:e (value-of a) :k (value-of b) :v (value-of c)}])
                                  :act-id (UUID/randomUUID)})))

      "read"
      (with-line flags
        (fn [connection hello]
          (client/say connection {:word :read :query (query-of hello flags (value-of a) (value-of b))})))

      "watch"
      (with-line flags
        (fn [connection hello]
          (let [ended (promise)]
            (client/watch connection (query-of hello flags (value-of a) (value-of b))
                          (fn [message]
                            (println (frame/write message))
                            (flush)
                            (when (contains? message :ended) (deliver ended (:ended message)))))
            {:ended @ended})))

      "send" (with-line flags (fn [connection hello] (send-file! connection hello a)))

      "give"
      (with-line flags
        (fn [connection hello]
          (let [who (value-of a)
                layer (value-of b)
                flags (assoc flags :layer b)
                act (act-of hello flags [])
                allows (case c "see" :see "act" :see-and-act nil)]
            (if-not allows
              {:could-not :read-the-command :said "give who layer see|act"}
              (client/say connection
                          {:word :send
                           :act (assoc act
                                       :subjects #{who}
                                       :facts [{:e who :k :softland.store/permission
                                                :v [who layer layer (:permission act) allows]}])
                           :act-id (UUID/randomUUID)})))))

      "invite"
      (with-line flags
        (fn [connection hello]
          (let [code (credential/make-secret)
                claim (if-let [email (:email flags)]
                        {:by :email :email email}
                        {:by :code :hash (credential/hash-of code)})
                got (client/say connection {:word :invite :uuid (UUID/randomUUID) :claim claim})]
            (cond-> got
              (and (:invited got) (= :code (:by claim)))
              (assoc :link (str (:url (kept (file-of flags))) "/?invited=" code))))))

      "agent"
      (case a
        "new" (with-line flags
                (fn [connection _hello]
                  (let [secret (credential/make-secret)
                        got (client/say connection {:word :make-agent :uuid (UUID/randomUUID)
                                                    :label b :hash (credential/hash-of secret)})]
                    (if-not (:made got)
                      got
                      (let [out (io/file (or (:out flags) (str "agent-" b ".edn")))]
                        (keep! out {:url (:url (kept (file-of flags)))
                                    :credential (credential/text (:root got) (:sign-in got) secret)})
                        (assoc got :file (.getPath out)))))))
        "end" (with-line flags
                (fn [connection _hello]
                  (client/say connection {:word :end-agent :agent (value-of b)})))
        {:could-not :read-the-command :said "agent new name | agent end uuid"})

      "under" (with-line flags (fn [connection _] (client/say connection {:word :under})))

      "end-sign-in" (with-line flags (fn [connection _] (client/say connection {:word :end-sign-in :sign-in (value-of a)})))

      "replace-secret"
      (with-line flags
        (fn [connection hello]
          (let [secret (credential/make-secret)
                got (client/say connection {:word :replace-secret :hash (credential/hash-of secret)})]
            (when (:replaced got)
              (let [file (file-of flags)]
                (keep! file (assoc (kept file) :credential (credential/text (:root hello) (:sign-in hello) secret)))))
            got))
        false)

      "new-session"
      (let [file (file-of flags)
            held (kept file)]
        (if-not (:credential held)
          {:could-not :find-the-sign-in :said "This program is not signed in. Run: sign-in"}
          (let [closed (with-line flags (fn [connection _] (client/say connection {:word :close-session})))]
            (if (and (:could-not closed) (not= 401 (:status closed)))
              closed
              (do (keep! file (dissoc (or (kept file) held) :session))
                  (with-line flags (fn [_ hello] hello)))))))

      "sign-out" (with-line flags (fn [connection _] (client/say connection {:word :sign-out})))

      {:could-not :read-the-command
       :said "sign-in, who, write, read, watch, send, give, invite, agent, under, end-sign-in, replace-secret, new-session, sign-out"})))

(defn -main
  "Runs one command, prints its result as one EDN form, and ends the
  process with the status that the namespace docstring gives."
  [& [command & args]]
  (let [result (try (run command args)
                    (catch Throwable t
                      {:could-not :run-the-command :threw (.getName (class t)) :said (.getMessage t)}))
        status (out! result)]
    (shutdown-agents)
    (System/exit (if (= :read-the-command (:could-not result)) 2 status))))
