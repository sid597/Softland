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
    write e k v         one fact, written into a lane
    read e k            one place, read once
    watch e k           one place, kept live until the program is stopped
    send file           the acts of a file, sent in order, each once
    give who lane see|act
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
    new-session [--model name]
                        leaves the session and starts another, optionally
                        naming the model that drives it
    sign-out            ends this program's sign-in

  Common arguments: `--as file`, the file that holds the sign-in, when it
  is not the default; `--lane id`, the lane, when it is not the person's
  own; `--permission edn`, the permission that an act names, when it is
  not the first permission of the person's own lane.

  Values. `e`, `k`, `v`, a lane and a permission are read as EDN, so a
  keyword, a set and a number of any width are typed as they are; a UUID
  is typed bare, or as EDN writes one; a text that is no EDN, or a bare
  word, is taken as a string.

  What it keeps. One file, `sign-in.edn` under `$SOFTLAND_HOME` or
  `~/.config/softland`, readable by its owner alone: the runtime's address,
  the credential, and the id of the session that it is in. It keeps
  nothing else between two runs: who it is and where the person's own
  lane is come from the runtime's hello each time. `send` keeps the UUIDs
  of a file's acts beside the file, so that a run that was cut off sends
  each act again under its own UUID and the store takes it once.

  Two commands at once. Each change of the file is made while the command
  holds the file (`with-file-lock`: a lock of the file `sign-in.edn.lock`
  beside it, which the system holds against every other process), and is
  made to what the file holds then, never to what the command read when it
  started. A replacement of the secret holds the file from its decision to
  its last write, so a second command that waited finds the replacement
  done and replaces nothing.

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
           [java.nio.channels FileChannel]
           [java.nio.charset StandardCharsets]
           [java.nio.file CopyOption Files OpenOption StandardCopyOption StandardOpenOption]
           [java.nio.file.attribute FileAttribute PosixFilePermissions]
           [java.util UUID]))

(def default-url
  "The runtime's address when none was given."
  "http://localhost:8500")

(def ^:private replace-under-ms
  "With less than this left of a secret's time, a command replaces the
  secret before it does its work: two days. A number of the build."
  (* 2 24 60 60 1000))

(def ^:private paced-wait-ms
  "How long `sign-in` waits before it asks again about its ask when the
  runtime said `:take-more`: three seconds. A number of the build."
  3000)

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

(defn- owner-only
  "Gives the attribute of a file that its owner alone reads and writes,
  where the file system has such attributes, and none otherwise."
  []
  (try (into-array FileAttribute [(PosixFilePermissions/asFileAttribute
                                   (PosixFilePermissions/fromString "rw-------"))])
       (catch Throwable _ (make-array FileAttribute 0))))

(defn- keep!
  "Writes a map atomically into a file created with `rw-------` permissions
  from birth (no world-readable window). It takes no lock: a change of the
  sign-in file goes through `update-kept!`, or is made while the command
  holds the file (`with-file-lock`)."
  [^File file thing]
  (let [parent (or (.getParentFile (.getAbsoluteFile file)) (io/file "."))
        _ (.mkdirs parent)
        target (.toPath (.getAbsoluteFile file))
        bytes (.getBytes (str (frame/write thing) "\n") StandardCharsets/UTF_8)
        tmp (Files/createTempFile (.toPath parent) ".softland-" ".tmp" (owner-only))]
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

(def ^:private held-in-this-process
  "One object for each sign-in file that a thread of this process holds or
  waits for, by the file's absolute path. A lock of a file's channel is held
  by a whole process, and a second lock of it from the same process throws,
  so threads of one process take the file one after the other on this
  object first."
  (atom {}))

(defn- with-file-lock
  "Runs `f`, a function of no argument, while this command holds the file:
  the object of this process for the file (`held-in-this-process`) and an
  exclusive lock of the file `<file>.lock` beside it, which the system holds
  against every other process and lets go of when the process ends. Gives
  what `f` gives. The lock file holds nothing and is made, when it is not
  there, readable by its owner alone."
  [^File file f]
  (let [target (.getAbsoluteFile file)
        path (.getPath target)
        lock-file (io/file (str path ".lock"))
        monitor (get (swap! held-in-this-process (fn [m] (if (contains? m path) m (assoc m path (Object.)))))
                     path)]
    (.mkdirs (.getParentFile lock-file))
    (locking monitor
      (with-open [channel (FileChannel/open (.toPath lock-file)
                                            #{StandardOpenOption/CREATE StandardOpenOption/WRITE}
                                            (owner-only))]
        (let [lock (.lock channel)]
          (try (f) (finally (.release lock))))))))

(defn- update-kept!
  "Changes the sign-in file while this command holds it: reads what it
  holds now, calls `f` with that map (or nil), and writes what `f` gives
  when it is a map other than the one read. Gives what the file holds
  after."
  [^File file f]
  (with-file-lock file
    (fn []
      (let [now (kept file)
            changed (f now)]
        (if (and (map? changed) (not= changed now))
          (do (keep! file changed) changed)
          now)))))

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

(defn- same-sign-in?
  "True when `cred` parses as a credential for `root` and `sign-in`."
  [cred root sign-in]
  (when-let [parts (credential/parts cred)]
    (and (= root (:root parts)) (= sign-in (:sign-in parts)))))

(defn- replace-on-line!
  "Replaces the sign-in's secret on an open connection. Takes the file, the
  credential that the connection came in with, the connection and the
  hello. Gives the runtime's answer to `:replace-secret`, or
  `{:replaced sign-in :by :another-command}` when another command replaced
  the secret first.

  It holds the file from the decision to the last write
  (`with-file-lock`), and decides on what the file holds then: when its
  credential is no longer the one that this connection came in with, and
  no replacement waits, another command has replaced the secret and kept
  the new credential, and nothing is sent. Otherwise the new credential is
  kept on disk as `:next-credential` before `:replace-secret` is sent, so a
  lost answer never locks the command line out; a `:next-credential` of
  this sign-in that a cut-off run left is sent again rather than a new one.
  It becomes `:credential` on `:replaced`; it is dropped on a definitive
  refusal of Auth (`:take-that`); and it is kept on an uncertain failure,
  so the next run recovers it."
  [file connected-with connection hello]
  (with-file-lock file
    (fn []
      (let [{:keys [root sign-in]} hello
            held (kept file)
            waiting (:next-credential held)
            reuse? (same-sign-in? waiting root sign-in)]
        (if (and (not reuse?) (not= connected-with (:credential held)))
          {:replaced sign-in :by :another-command}
          (let [secret (if reuse?
                         (:secret (credential/parts waiting))
                         (credential/make-secret))
                next-cred (if reuse?
                            waiting
                            (credential/text root sign-in secret))
                pending (assoc held :next-credential next-cred)]
            (keep! file pending)
            (let [got (client/say connection {:word :replace-secret :hash (credential/hash-of secret)})]
              (cond
                (:replaced got)
                (keep! file (-> pending (assoc :credential next-cred) (dissoc :next-credential)))

                (= :take-that (:could-not got))
                (keep! file (dissoc pending :next-credential)))
              got)))))))

(defn- connect-held
  "Opens the line for a kept sign-in. When a prior replacement left
  `:next-credential` on disk, tries it first so a replacement whose answer
  was lost is recovered even after the old secret expires; falls back to
  `:credential` on 401."
  [{:keys [url credential next-credential session]}]
  (if next-credential
    (let [trying (client/connect {:url url :credential next-credential :session session})]
      (cond
        (not (:could-not trying))
        {:connection trying :credential next-credential :promoted? true}

        (and (= 401 (:status trying)) credential)
        (let [fallback (client/connect {:url url :credential credential :session session})]
          (if (:could-not fallback)
            {:error fallback}
            {:connection fallback :credential credential :promoted? false}))

        :else {:error trying}))
    (let [conn (client/connect {:url url :credential credential :session session})]
      (if (:could-not conn)
        {:error conn}
        {:connection conn :credential credential :promoted? false}))))

(defn- with-line*
  "Like `with-line`, passing `[connection hello promoted? connected-with]`
  to `f`, where `promoted?` is true when a pending `:next-credential`
  connected and was promoted on this run, and `connected-with` is the
  credential that the connection came in with."
  [flags f auto-replace?]
  (let [file (file-of flags)
        {:keys [credential session] :as held} (kept file)]
    (if-not (or credential (:next-credential held))
      {:could-not :find-the-sign-in :said "This program is not signed in. Run: sign-in"}
      (let [{:keys [connection error promoted?] active-cred :credential} (connect-held held)]
        (if error
          (cond-> error
            (= 401 (:status error)) (assoc :said "The sign-in stands for nobody now, or its session is closed. Run: sign-in"))
          (let [hello (client/hello connection)]
            (try
              (if (:could-not hello)
                hello
                (do
                  ;; Made to what the file holds now: a credential that
                  ;; connected is kept when it still waits there, and a new
                  ;; session when the file is still in the one read at the
                  ;; start, so another command's change is never written over.
                  (when (or promoted? (not= session (:session hello)))
                    (update-kept! file
                                  (fn [now]
                                    (when now
                                      (cond-> now
                                        (and promoted? (= active-cred (:next-credential now)))
                                        (-> (assoc :credential active-cred) (dissoc :next-credential))

                                        (and (not= session (:session hello)) (= session (:session now)))
                                        (assoc :session (:session hello)))))))
                  (when (and auto-replace?
                             (:until hello)
                             (< (- (:until hello) (System/currentTimeMillis)) replace-under-ms))
                    (replace-on-line! file active-cred connection hello))
                  (f connection hello promoted? active-cred)))
              (finally (client/close! connection)))))))))

(defn- with-line
  "Opens the line as the kept sign-in, calls `f` with the connection and
  the runtime's hello, closes the line, and gives what `f` gave. A sign-in
  that the runtime gave a new session keeps it. A secret that has little
  time left is replaced first (unless `auto-replace?` is false), keeping the
  new credential on disk before sending so a lost answer never locks the
  command line out. Gives a word when there is no sign-in or the line could
  not be opened."
  ([flags f] (with-line flags f true))
  ([flags f auto-replace?]
   (with-line* flags (fn [connection hello _ _] (f connection hello)) auto-replace?)))

(defn- act-of
  "Builds one act of this program. Takes the hello, the flags, and the
  facts. Gives the act, whole: the who and the session are the hello's;
  the lane is `--lane` or the person's own; the permission is
  `--permission`, or the first permission of that lane for its person."
  [hello {:keys [lane permission]} facts]
  (let [lane (if lane (value-of lane) (:own-lane hello))]
    {:version 1
     :who (:who hello)
     :session (:session hello)
     :lane lane
     :permission (if permission (value-of permission) [(:who hello) lane lane])
     :facts facts}))

(defn- query-of
  "Builds the query of one place. Takes the hello, the flags, an entity
  and a key."
  [hello {:keys [lane]} e k]
  {:asks [:place e k]
   :asker (:who hello)
   :lanes [(if lane (value-of lane) (:own-lane hello))]})

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
            (cond
              (= :waiting (:state got))
              (do (Thread/sleep 1000) (recur))

              ;; A runtime that paces sign-ins says so with `:take-more`; the
              ;; ask still waits for her, so the program waits and asks again
              ;; until the ask's end.
              (and (= :take-more (:could-not got))
                   (number? (:ends asked))
                   (< (System/currentTimeMillis) (long (:ends asked))))
              (do (Thread/sleep paced-wait-ms) (recur))

              (= :yes (:state got))
              (let [file (file-of flags)]
                (with-file-lock file (fn [] (keep! file {:url url :credential (:credential got)})))
                (with-line flags (fn [_ hello] hello)))

              :else got)))))))

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
                lane (value-of b)
                flags (assoc flags :lane b)
                act (act-of hello flags [])
                allows (case c "see" :see "act" :see-and-act nil)]
            (if-not allows
              {:could-not :read-the-command :said "give who lane see|act"}
              (client/say connection
                          {:word :send
                           :act (assoc act
                                       :subjects #{who}
                                       :facts [{:e who :k :softland.store/permission
                                                :v [who lane lane (:permission act) allows]}])
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
      (with-line* flags
        (fn [connection hello promoted? connected-with]
          (if promoted?
            {:replaced (:sign-in hello) :until (:until hello)}
            (replace-on-line! (file-of flags) connected-with connection hello)))
        false)

      "new-session"
      ;; The file is moved off the session before that session is closed:
      ;; a run that is cut off between the two leaves the file in the new
      ;; session, or in none, and never in one that is closed.
      (let [file (file-of flags)
            held (kept file)
            drop-session! (fn [] (update-kept! file (fn [now] (when now (dissoc now :session)))))]
        (if-not (or (:credential held) (:next-credential held))
          {:could-not :find-the-sign-in :said "This program is not signed in. Run: sign-in"}
          (if (contains? flags :model)
            (let [open-word {:word :open-session :driven-by {:kind :model :model (or (:model flags) "")}}
                  open-then-close
                  (fn []
                    (with-line flags
                      (fn [connection hello]
                        (let [opened (client/say connection open-word)]
                          (if (:could-not opened)
                            opened
                            (do (update-kept! file (fn [now]
                                                     (when (and now (= (:session hello) (:session now)))
                                                       (assoc now :session (:open opened)))))
                                (client/say connection {:word :close-session})
                                opened))))))
                  opened (let [first-try (open-then-close)]
                           (if (and (:could-not first-try) (= 401 (:status first-try)))
                             (do (drop-session!)
                                 (open-then-close))
                             first-try))]
              (if (:could-not opened)
                opened
                (with-line flags (fn [_ hello] hello))))
            (let [closed (with-line flags
                           (fn [connection hello]
                             (update-kept! file (fn [now]
                                                  (when (and now (= (:session hello) (:session now)))
                                                    (dissoc now :session))))
                             (client/say connection {:word :close-session})))]
              (if (and (:could-not closed) (not= 401 (:status closed)))
                closed
                (do (when (= 401 (:status closed)) (drop-session!))
                    (with-line flags (fn [_ hello] hello))))))))

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
