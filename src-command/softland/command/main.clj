(ns softland.command.main
  "The command line: Softland from a terminal. It is signed in as an agent
  of hers, with a who of its own, and what it may do is what it was given:
  at her yes, what she ticked in her own lane, and what was given it since.
  No program is a person: the commands that only she may say, at a browser
  (`invite`, `under`, `end-sign-in`), are in the list and get the runtime's
  refusal, since whether one of them can be lent to a program is open.
  `give` to a person is refused by a gate today: passing to a person needs
  `:give`, what her yes lends holds none, and no page lends it. To an agent
  or a run it lends beneath what this program holds.

  The commands are the entries of `commands`, the one list of them: for
  each, how it is typed, the flags that it takes, what it does, and the
  function that does it. A command that is typed with another number of
  plain arguments, with a flag that it does not take, with a flag whose
  value is left out, or with a flag twice, is not run and nothing is
  connected for it: a slip must not become another command. `write e k`
  with the value left out would be sent as a retract, and `write e k v
  --lane` with the lane left out would be written into her own lane.
  The same holds for a value that begins as data and does not read: it is
  not stored as a text.

  Every command takes `--as file`, the file that holds the sign-in, when
  it is not the default. A command that writes or reads takes `--lane id`,
  the lane, when it is not the person's own; one that writes takes
  `--permission uuid`, the permission that the act names, when it is not
  the one that her yes lent this sign-in
  (`softland.wire.credential/lent-at-yes`).

  Values. `e`, `k`, `v`, a who, a lane, a permission and an id are read as
  EDN; a UUID is typed bare, or as EDN writes one. Plain words stay text:
  a bare word (`oak`), and words that are no EDN (`hello world`), are
  taken as a string. A value that begins as data is held to being data:
  when what is typed begins as a map, a vector, a list, a set or a tagged
  form, a string, a keyword, a character or a number, and does not read as
  one EDN form, the command is not run. `{:a 1` with its brace left open
  was stored as the text `{:a 1`. A text that begins so is typed as EDN
  writes a string, in quotes: `\"3 chairs\"`.

  What it keeps. One file, `sign-in.edn` under `$SOFTLAND_HOME` or
  `~/.config/softland`: the runtime's address, the credential, and the id
  of the session that it is in. Who it is and where the person's own lane
  is come from the runtime's hello each time. For `send`, a journal beside
  the file that is sent, `<file>.sent.edn`: who sent each act, the act, and
  its UUID.

  Two commands at once. Each change of the file is made while the command
  holds the file (`with-file-lock`), and is made to what the file holds
  then, never to what the command read when it started.

  One sitting is one session: the commands of one sign-in share it until
  `new-session`. A run of an agent that wants a session of its own says
  `new-session` first.

  It prints one EDN form, the result. The exit status is the first of
  these that holds. 2: the command could not be read. 1: a \"could not\":
  the result holds `:could-not`, or a result of `send` or the word that a
  `watch` ended with holds it, or the ask of `sign-in` ran out. 3: a
  refusal: a gate refused an act, one of `send` too, or she said no to the
  sign-in. 0: otherwise.

  Three of the words that hold `:could-not` are the command line's own:
  `:read-the-command`, `:run-the-command` and `:find-a-credential`.
  `:read-the-command` says how the command is typed under `:said`, and why
  it was not read under `:why`: `:command-not-known`,
  `:wrong-number-of-arguments`, `:argument-not-known`, `:flag-not-known`,
  `:flag-with-no-value`, `:flag-given-twice` or `:value-does-not-read`,
  with the argument or the flag under `:at`."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [softland.command.client :as client]
            [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.wire.credential :as credential]
            [softland.wire.frame :as frame])
  (:import [java.io File PushbackReader]
           [java.nio.channels FileChannel]
           [java.nio.charset StandardCharsets]
           [java.nio.file CopyOption Files OpenOption StandardCopyOption StandardOpenOption]
           [java.nio.file.attribute FileAttribute PosixFilePermissions]
           [java.util UUID]))

(def default-url
  "http://localhost:8500")

(def ^:private replace-under-ms
  (* 2 24 60 60 1000))

(def ^:private paced-wait-ms
  3000)

;; ------------------------------------------------------------------- the file

(defn- home
  ^File []
  (io/file (or (System/getenv "SOFTLAND_HOME")
               (io/file (System/getProperty "user.home") ".config" "softland"))))

(defn- file-of
  ^File [{:keys [as]}]
  (if as (io/file as) (io/file (home) "sign-in.edn")))

(defn- kept
  [^File file]
  (when (.isFile file)
    (let [[outcome form] (frame/read-one (slurp file))]
      (when (and (= :ok outcome) (map? form)) form))))

(defn- owner-only
  "None where the file system has no such attributes."
  []
  (try (into-array FileAttribute [(PosixFilePermissions/asFileAttribute
                                   (PosixFilePermissions/fromString "rw-------"))])
       (catch Throwable _ (make-array FileAttribute 0))))

(defn- keep!
  "The file is `rw-------` from birth: there is no window in which another
  user reads it. It takes no lock: the caller holds the file
  (`with-file-lock`), or goes through `update-kept!`."
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
  "One object for each sign-in file, by its absolute path. A lock of a
  file's channel is held by a whole process, and a second lock of it from
  the same process throws, so threads of one process take the file one
  after the other on this object first."
  (atom {}))

(defn- with-file-lock
  "The lock is of the file `<file>.lock` beside it, which holds nothing. The
  system holds it against every other process, and lets go of it when the
  process ends."
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
  "What was typed after the command, as {:flags {flag value} :rest [plain
  arguments]}, or {:fault why :at flag} at the first flag that cannot be
  read: one with no value after it, where the next thing typed is another
  flag or nothing, and one that was given before. Which flags a command
  takes is `read-command`'s."
  [args]
  (loop [args args flags {} others []]
    (if-let [[arg & more] (seq args)]
      (if (str/starts-with? arg "--")
        (let [flag (keyword (subs arg 2))
              value (first more)]
          (cond
            (or (nil? value) (str/starts-with? value "--")) {:fault :flag-with-no-value :at arg}
            (contains? flags flag) {:fault :flag-given-twice :at arg}
            :else (recur (rest more) (assoc flags flag value) others)))
        (recur more flags (conj others arg)))
      {:flags flags :rest others})))

(defn- begins-as-data?
  "Whether what is typed begins as EDN writes a thing that is no bare word:
  a map, a vector, a list, a set or a tagged form, a string, a keyword, a
  character, or a number, with a sign or without."
  [^String text]
  (boolean (re-find #"^\s*(?:[\[{(#\":\\0-9]|[+-][0-9])" text)))

(defn- value-of
  "What is typed, read as a value: [:ok value], or [:no why] for a text
  that begins as data and does not read as one EDN form, with the wire's
  reader's word for why. A bare UUID is a UUID. A bare word, and a text
  that neither reads nor begins as data, is that text."
  [text]
  (if (re-matches #"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" (str text))
    [:ok (UUID/fromString text)]
    (let [[outcome form] (frame/read-one text)]
      (cond
        (and (= :ok outcome) (not (symbol? form))) [:ok form]
        (and (= :no outcome) (begins-as-data? text)) [:no form]
        :else [:ok text]))))

;; ------------------------------------------------------------------ the result

(defn- status-of
  "A `send` that holds both a \"could not\" and a refusal gives 1: the run
  did not finish, and the refusal is in what is printed."
  [result]
  (let [each (cons result (:results result))
        refused? (fn [r] (= :refused (get-in r [:answer :answer])))]
    (cond
      (= :read-the-command (:could-not result)) 2
      (or (some :could-not each) (:could-not (:ended result)) (= :over (:state result))) 1
      (or (some refused? each) (= :no (:state result))) 3
      :else 0)))

(defn- out!
  [result]
  (println (frame/write result))
  (status-of result))

;; ----------------------------------------------------------------- connecting

(defn- same-sign-in?
  [cred root sign-in]
  (when-let [parts (credential/parts cred)]
    (and (= root (:root parts)) (= sign-in (:sign-in parts)))))

(defn- replace-on-line!
  "Holds the file from the decision to the last write, and decides on what
  the file holds then: when its credential is not the one that this
  connection came in with, and no replacement waits, another command has
  replaced the secret, and nothing is sent.

  The new credential is kept on disk as `:next-credential` before
  `:replace-secret` is sent, so a lost answer never locks the command line
  out; a `:next-credential` of this sign-in that a cut-off run left is sent
  again rather than a new one. It is dropped on a definitive refusal of
  Auth (`:take-that`), and kept on an uncertain failure, so the next run
  recovers it."
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
  "`:next-credential` is tried first, so that a replacement whose answer
  was lost is recovered even after the old secret expires. The kept
  credential is tried after it only when the runtime says that the waiting
  one stands for nobody: the two are of one sign-in, so a session that is
  not open under one is not open under the other."
  [{:keys [url credential next-credential session]}]
  (if next-credential
    (let [trying (client/connect {:url url :credential next-credential :session session})]
      (cond
        (not (:could-not trying))
        {:connection trying :credential next-credential :promoted? true}

        (and (= :find-her-sign-in (:could-not trying)) credential)
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
  [flags f auto-replace?]
  (let [file (file-of flags)
        {:keys [credential session] :as held} (kept file)]
    (if-not (or credential (:next-credential held))
      {:could-not :find-a-credential :said "This program is not signed in. Run: sign-in"}
      (let [{:keys [connection error promoted?] active-cred :credential} (connect-held held)]
        (if error
          (cond-> error
            (= :find-her-sign-in (:could-not error)) (assoc :said "The sign-in stands for nobody now. Run: sign-in")
            (= :find-her-session (:could-not error)) (assoc :said "The session is not open. Run: new-session"))
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
  ([flags f] (with-line flags f true))
  ([flags f auto-replace?]
   (with-line* flags (fn [connection hello _ _] (f connection hello)) auto-replace?)))

(defn- act-of
  "With no `--permission`, the act names the permission that her yes lent
  this sign-in, which covers her own lane alone. A sign-in that her yes
  lent nothing (an agent signed in again with no box ticked, which holds
  what it held) and one that `agent new` began name one with
  `--permission`."
  [hello flags facts]
  (let [lane (if (contains? flags :lane) (:lane flags) (:own-lane hello))]
    {:version 1
     :who (:who hello)
     :session (:session hello)
     :lane lane
     :permission (if (contains? flags :permission)
                   (:permission flags)
                   (:permission (credential/lent-at-yes (:root hello) (:sign-in hello))))
     :facts facts}))

(defn- query-of
  [hello flags e k]
  {:asks [:place e k]
   :asker (:who hello)
   :lanes [(if (contains? flags :lane) (:lane flags) (:own-lane hello))]})

;; ---------------------------------------------------------------- the commands

(defn- sign-in!
  [{:keys [url agent] :as flags} _]
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
          (when agent (println (str "  On that page, say yes as a new agent and call it: " agent))))
        (loop []
          (let [got (client/waited url asked)]
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

(defn- journal-of
  "A UUID belongs to its act and not to the act's place in the file: an act
  takes the UUID of a kept entry of the same who and the same act that no
  act before it took in this run, so two equal acts of one file are two
  acts. Gives {:uuids, in the file's order, :sent, the entries to keep}.

  The `:session` is no part of what makes an act the same: a run that was
  cut off and is sent again after `new-session` keeps its UUIDs, so the
  store takes no act twice."
  [who entries acts make-uuid]
  (let [sent (first (reduce (fn [[sent free] act]
                              (let [[entry & others] (get free [who act])]
                                [(conj sent (or entry {:who who :act act :uuid (make-uuid)}))
                                 (assoc free [who act] others)]))
                            [[] (group-by (juxt :who :act) entries)]
                            acts))]
    {:uuids (mapv :uuid sent) :sent sent}))

(defn- send-file!
  "The file holds EDN maps, one after another; each is an act without `:who`
  and `:session`, which are this program's. The UUID of each act is kept in
  `<file>.sent.edn`, with the act and who sent it, before anything is sent,
  so a run that was cut off sends the same acts again under the same UUIDs,
  and the store takes each once."
  [connection hello path]
  (let [file (io/file path)
        acts (with-open [reader (PushbackReader. (io/reader file))]
               (doall (take-while (fn [form] (not= ::end form))
                                  (repeatedly (fn [] (edn/read {:eof ::end} reader))))))
        journal (io/file (str path ".sent.edn"))
        held (vec (:sent (kept journal)))
        {:keys [uuids sent]} (journal-of (:who hello) held acts (fn [] (UUID/randomUUID)))]
    (when (not= sent held)
      (keep! journal {:sent sent}))
    (client/say connection
                {:word :send-all
                 :acts (mapv (fn [act uuid]
                               {:act (assoc act :who (:who hello) :session (:session hello)) :act-id uuid})
                             acts uuids)})))

(defn- who!
  [flags _]
  (with-line flags (fn [_ hello] hello)))

(defn- write!
  [flags [e k v]]
  (with-line flags
    (fn [connection hello]
      (client/say connection {:word :send
                              :act (act-of hello flags [{:e e :k k :v v}])
                              :act-id (UUID/randomUUID)}))))

(defn- read!
  [flags [e k]]
  (with-line flags
    (fn [connection hello]
      (client/say connection {:word :read :query (query-of hello flags e k)}))))

(defn- watch!
  [flags [e k]]
  (with-line flags
    (fn [connection hello]
      (let [ended (promise)
            sent (client/watch connection (query-of hello flags e k)
                               (fn [message]
                                 (println (frame/write message))
                                 (flush)
                                 (when (contains? message :ended) (deliver ended (:ended message)))))]
        (if (:could-not sent) sent {:ended @ended})))))

(defn- send!
  [flags [path]]
  (with-line flags (fn [connection hello] (send-file! connection hello path))))

(declare could-not-read)

(defn- give!
  [flags [who lane what]]
  (with-line flags
    (fn [connection hello]
      (let [act (act-of hello (assoc flags :lane lane) [])
            allows (case what
                     "see" {:see true}
                     "act" (if (act/borrower? who)
                             {:see true :write :all}
                             {:see true :write :all :give #{:see :write :give}})
                     nil)]
        (if-not allows
          (could-not-read "give" :argument-not-known what)
          (let [perm-id (UUID/randomUUID)
                lent? (or (act/borrower? (:who hello)) (act/borrower? who))
                fact {:e perm-id
                      :k declaration/permission-key
                      :v (cond-> {:holder who
                                  :allows allows
                                  :covers [:default-group lane]
                                  :given-by (:who hello)
                                  :under (:permission act)}
                           lent? (assoc :lent-from (:permission act)))}
                sent (client/say connection
                                 {:word :send
                                  :act (assoc act :facts [fact])
                                  :act-id (UUID/randomUUID)})]
            (if (:could-not sent)
              sent
              (assoc sent :permission perm-id))))))))

(defn- invite!
  [flags _]
  (with-line flags
    (fn [connection _hello]
      (let [code (credential/make-secret)
            claim (if-let [email (:email flags)]
                    {:by :email :email email}
                    {:by :code :hash (credential/hash-of code)})
            got (client/say connection {:word :invite :uuid (UUID/randomUUID) :claim claim})]
        (cond-> got
          (and (:invited got) (= :code (:by claim)))
          (assoc :link (str (:url (kept (file-of flags))) "/?invited=" code)))))))

(defn- agent-new!
  [flags [label]]
  (with-line flags
    (fn [connection _hello]
      (let [secret (credential/make-secret)
            got (client/say connection {:word :make-agent :uuid (UUID/randomUUID)
                                        :label label :hash (credential/hash-of secret)})]
        (if-not (:made got)
          got
          (let [out (io/file (or (:out flags) (str "agent-" label ".edn")))]
            (keep! out {:url (:url (kept (file-of flags)))
                        :credential (credential/text (:root got) (:sign-in got) secret)})
            (assoc got :file (.getPath out))))))))

(defn- agent-end!
  [flags [uuid]]
  (with-line flags (fn [connection _hello] (client/say connection {:word :end-agent :agent uuid}))))

(defn- under!
  [flags _]
  (with-line flags (fn [connection _] (client/say connection {:word :under}))))

(defn- end-sign-in!
  [flags [uuid]]
  (with-line flags (fn [connection _] (client/say connection {:word :end-sign-in :sign-in uuid}))))

(defn- replace-secret!
  [flags _]
  (with-line* flags
    (fn [connection hello promoted? connected-with]
      (if promoted?
        {:replaced (:sign-in hello) :until (:until hello)}
        (replace-on-line! (file-of flags) connected-with connection hello)))
    false))

(defn- new-session!
  "The file is moved off the session before that session is closed: a run
  that is cut off between the two leaves the file in the new session, or
  in none, and never in one that is closed."
  [flags _]
  (let [file (file-of flags)
        held (kept file)
        drop-session! (fn [] (update-kept! file (fn [now] (when now (dissoc now :session)))))]
    (if-not (or (:credential held) (:next-credential held))
      {:could-not :find-a-credential :said "This program is not signed in. Run: sign-in"}
      (if (contains? flags :model)
        (let [open-word {:word :open-session :driven-by {:kind :model :model (:model flags)}}
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
                       (if (= :find-her-session (:could-not first-try))
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
          (if (and (:could-not closed) (not= :find-her-session (:could-not closed)))
            closed
            (do (when (= :find-her-session (:could-not closed)) (drop-session!))
                (with-line flags (fn [_ hello] hello)))))))))

(defn- sign-out!
  [flags _]
  (with-line flags (fn [connection _] (client/say connection {:word :sign-out}))))

;; ------------------------------------------------------- the one list of commands

(defn- value
  "An argument, or a flag's value, that is read as a value (`value-of`)
  before the command is run; the function is handed what was read."
  [named]
  {:named named :value? true})

(defn- text
  "An argument, or a flag's value, that is handed on as it was typed."
  [named]
  {:named named})

(def ^:private commands
  "Every command, in the order in which they are named to somebody who
  typed none. A command is one word, or two.

    :args   its plain arguments, each a `value` or a `text` with its name:
            a command is typed with exactly that many
    :flags  the flags that it takes beside `--as`, each with a `value` or a
            `text` that names what follows the flag
    :note   what else is to be known of how it is typed
    :says   what it does
    :does   the function that does it, which is handed the flags and the
            plain arguments, each value read

  How a command is typed is made of the first three (`typed`), and nothing
  else says it."
  [["sign-in"        {:args [] :flags {:url (text "address") :agent (text "name")} :does sign-in!
                      :says "asks the runtime, shows a link and a code, waits for her yes, and keeps the credential. She signs it in as a new agent or as one of hers; with `--agent` it asks her to call a new agent by that name"}]
   ["who"            {:args [] :does who!
                      :says "who this program is signed in as"}]
   ["write"          {:args [(value "e") (value "k") (value "v")]
                      :flags {:lane (value "id") :permission (value "uuid")} :does write!
                      :note "a retract is typed with the value nil"
                      :says "one fact, written into a lane"}]
   ["read"           {:args [(value "e") (value "k")] :flags {:lane (value "id")} :does read!
                      :says "one place, read once"}]
   ["watch"          {:args [(value "e") (value "k")] :flags {:lane (value "id")} :does watch!
                      :says "one place, kept live until the program is stopped"}]
   ["send"           {:args [(text "file")] :does send!
                      :says "the acts of a file, sent in order, each once"}]
   ["give"           {:args [(value "who") (value "lane") (text "see|act")] :flags {:permission (value "uuid")} :does give!
                      :says "gives a permission beneath the one that this program acts under; to a person, refused while it holds no :give"}]
   ["invite"         {:args [] :flags {:email (text "address")} :does invite!
                      :says "makes a person who waits for an account; prints the link for her, or names the email. Today only a person at a browser may say it: the runtime refuses it to a program"}]
   ["agent new"      {:args [(text "name")] :flags {:out (text "file")} :does agent-new!
                      :says "makes an agent beneath this program's who, and writes the file that the agent signs in with"}]
   ["agent end"      {:args [(value "uuid")] :does agent-end!
                      :says "ends an agent"}]
   ["under"          {:args [] :does under!
                      :says "the actors and the sign-ins of this person. Today only a person at a browser may say it: the runtime refuses it to a program"}]
   ["end-sign-in"    {:args [(value "uuid")] :does end-sign-in!
                      :says "ends one sign-in of this person. Today only a person at a browser may say it: the runtime refuses it to a program"}]
   ["replace-secret" {:args [] :does replace-secret!
                      :says "a new secret for this sign-in"}]
   ["new-session"    {:args [] :flags {:model (text "name")} :does new-session!
                      :says "leaves the session and starts another, optionally naming the model that drives it"}]
   ["sign-out"       {:args [] :does sign-out!
                      :says "ends this program's sign-in"}]])

(def ^:private command-named
  (into {} commands))

(defn- typed
  "How a command of `commands` is typed, as one line."
  [named]
  (let [{:keys [args flags note]} (get command-named named)]
    (str named
         (apply str (map (fn [arg] (str " " (:named arg))) args))
         (apply str (map (fn [[flag value]] (str " [--" (name flag) " " (:named value) "]")) flags))
         (when note (str "; " note)))))

(defn- how
  "What a result says under `:said` of a command that could not be read:
  how the command is typed; for the first word of commands of two words,
  how each of those is typed; else the names of the commands."
  [command]
  (let [first-word-of (filter (fn [named] (str/starts-with? named (str command " "))) (map first commands))]
    (cond
      (contains? command-named command) (typed command)
      (seq first-word-of) (str/join " | " (map typed first-word-of))
      :else (str/join ", " (distinct (map (fn [[named]] (first (str/split named #" "))) commands))))))

(defn- could-not-read
  [command why at]
  (cond-> {:could-not :read-the-command :why why :said (how command)}
    (some? at) (assoc :at at)
    (= :value-does-not-read why)
    (update :said str ". What is typed at " at " begins as data and does not read as one EDN form;"
            " a text that begins so is typed as EDN writes a string, in quotes")))

(defn- as-its-entry-says
  "The plain arguments and the flags of a command that is typed with the
  arguments and the flags of its entry, each `value` among them read. Gives
  {:flags :rest} with what was read in the place of what was typed, or
  {:not-read name} at the first value that begins as data and does not
  read: a plain argument by its name, a flag as it is typed."
  [entry flags plain]
  (let [read (fn [spec typed] (if (:value? spec) (value-of typed) [:ok typed]))
        args (map (fn [spec typed] [(:named spec) (read spec typed)]) (:args entry) plain)
        given (map (fn [[flag typed]] [flag (read (get (:flags entry) flag) typed)]) flags)
        not-read (or (some (fn [[named [outcome]]] (when (= :no outcome) named)) args)
                     (some (fn [[flag [outcome]]] (when (= :no outcome) (str "--" (name flag)))) given))]
    (if not-read
      {:not-read not-read}
      {:flags (into {} (map (fn [[flag [_ v]]] [flag v])) given)
       :rest (mapv (fn [[_ [_ v]]] v) args)})))

(defn- read-command
  "What was typed, held against `commands`. Gives {:does function :flags
  flags :rest plain-arguments} for a command that is typed as its entry
  says, with each value of it read, or the result that says that it could
  not be read. Nothing is connected here."
  [command args]
  (let [{:keys [fault at flags] plain :rest} (parsed args)
        two-words (when (and (string? command) (seq plain)) (str command " " (first plain)))
        [named plain] (cond
                        (contains? command-named command) [command plain]
                        (contains? command-named two-words) [two-words (vec (rest plain))])
        entry (get command-named named)
        not-taken (first (remove (conj (set (keys (:flags entry))) :as) (keys flags)))]
    (cond
      fault (could-not-read command fault at)
      (nil? entry) (could-not-read command :command-not-known nil)
      not-taken (could-not-read named :flag-not-known (str "--" (name not-taken)))
      (not= (count (:args entry)) (count plain)) (could-not-read named :wrong-number-of-arguments nil)
      :else (let [{:keys [not-read] :as read} (as-its-entry-says entry flags plain)]
              (if not-read
                (could-not-read named :value-does-not-read not-read)
                (assoc read :does (:does entry)))))))

(defn- run
  [command args]
  (let [{:keys [does flags] plain :rest :as read} (read-command command args)]
    (if (:could-not read)
      read
      (does flags plain))))

(defn -main
  [& [command & args]]
  (let [result (try (run command args)
                    (catch Throwable t
                      {:could-not :run-the-command :threw (.getName (class t)) :said (.getMessage t)}))
        status (out! result)]
    (shutdown-agents)
    (System/exit status)))
