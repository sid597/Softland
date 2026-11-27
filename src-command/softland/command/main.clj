(ns softland.command.main
  "The command line: Softland from a terminal. A person and an agent use the
  same commands: an agent is signed in with a who of its own, and what it
  may do is what it was given.

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

  Values. `e`, `k`, `v`, a lane and a permission are read as EDN; a UUID is
  typed bare, or as EDN writes one; a text that is no EDN, or a bare word,
  is taken as a string.

  What it keeps. One file, `sign-in.edn` under `$SOFTLAND_HOME` or
  `~/.config/softland`: the runtime's address, the credential, and the id
  of the session that it is in. Who it is and where the person's own lane
  is come from the runtime's hello each time.

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
  sign-in. 0: otherwise."
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
  [args]
  (loop [args args flags {} others []]
    (if-let [[arg & more] (seq args)]
      (if (str/starts-with? arg "--")
        (recur (rest more) (assoc flags (keyword (subs arg 2)) (first more)) others)
        (recur more flags (conj others arg)))
      {:flags flags :rest others})))

(defn- value-of
  [text]
  (if (re-matches #"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" (str text))
    (UUID/fromString text)
    (let [[outcome form] (frame/read-one text)]
      (if (and (= :ok outcome) (not (symbol? form))) form text))))

(def ^:private typed
  "How each command is typed, after the number of its plain arguments:
  those that are no `--flag value` pair."
  {"sign-in"        [0 "sign-in [--url address] [--agent name]"]
   "who"            [0 "who"]
   "write"          [3 "write e k v; a retract is typed with the value nil"]
   "read"           [2 "read e k"]
   "watch"          [2 "watch e k"]
   "send"           [1 "send file"]
   "give"           [3 "give who lane see|act"]
   "invite"         [0 "invite [--email a]"]
   "agent"          [2 "agent new name [--out file] | agent end uuid"]
   "under"          [0 "under"]
   "end-sign-in"    [1 "end-sign-in uuid"]
   "replace-secret" [0 "replace-secret"]
   "new-session"    [0 "new-session [--model name]"]
   "sign-out"       [0 "sign-out"]})

(defn- mistyped
  "A command that is typed with another number of plain arguments is not
  run, and nothing is connected for it: it opens no session and replaces no
  secret. `write e k` with the value left out would be sent as a retract.
  Nil for a command that `typed` does not hold: `run` names the commands."
  [command args]
  (when-let [[takes how] (get typed command)]
    (when (not= takes (count (:rest (parsed args))))
      {:could-not :read-the-command :said how})))

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
  was lost is recovered even after the old secret expires."
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
  ([flags f] (with-line flags f true))
  ([flags f auto-replace?]
   (with-line* flags (fn [connection hello _ _] (f connection hello)) auto-replace?)))

(defn- first-permission
  "A lane's first owner permission value for `holder`."
  [holder lane]
  [holder lane lane])

(defn- given-permission
  "A given permission value beneath `parent` for `holder` in `lane`."
  [holder lane parent allows]
  [holder lane lane parent allows])

(defn- permission-id
  "The `:permission` slot of an act for `permission`."
  [permission]
  permission)

(defn- permission-fact
  "The store fact that writes `permission`."
  [permission]
  {:e (nth permission 0) :k :softland.store/permission :v permission})

(defn- act-of
  "With no `--permission`, the act names the first permission of the lane
  for its person."
  [hello {:keys [lane permission]} facts]
  (let [lane (if lane (value-of lane) (:own-lane hello))]
    {:version 1
     :who (:who hello)
     :session (:session hello)
     :lane lane
     :permission (if permission (value-of permission) (permission-id (first-permission (:who hello) lane)))
     :facts facts}))

(defn- query-of
  [hello {:keys [lane]} e k]
  {:asks [:place e k]
   :asker (:who hello)
   :lanes [(if lane (value-of lane) (:own-lane hello))]})

;; ---------------------------------------------------------------- the commands

(defn- sign-in!
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

(defn- run
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
          (let [ended (promise)
                sent (client/watch connection (query-of hello flags (value-of a) (value-of b))
                                   (fn [message]
                                     (println (frame/write message))
                                     (flush)
                                     (when (contains? message :ended) (deliver ended (:ended message)))))]
            (if (:could-not sent) sent {:ended @ended}))))

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
                                       :facts [(permission-fact (given-permission who lane (:permission act) allows))])
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
  [& [command & args]]
  (let [result (try (or (mistyped command args) (run command args))
                    (catch Throwable t
                      {:could-not :run-the-command :threw (.getName (class t)) :said (.getMessage t)}))
        status (out! result)]
    (shutdown-agents)
    (System/exit status)))
