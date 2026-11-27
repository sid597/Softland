(ns softland.runtime.command-proof
  "The proof of the command line as a program: its commands, each run as a
  process of its own with the command line's own classpath, against one
  copy of the runtime on Rama's in-process cluster. One command, from the
  repository root:

    clojure -M:runtime -m softland.runtime.command-proof

  It is with the runtime's checks because it needs a runtime to speak to.
  The command line itself is started as `clojure -M:command -m
  softland.command.main`, so what runs is what a person runs: a JVM with no
  Rama on its path, which keeps its file in a folder that the proof makes
  and names with `SOFTLAND_HOME`.

  What a pass shows. The commands do what the command line's page says,
  for the cases written, between a real program and a real runtime. A
  person's yes is said by the proof's driver, at the page, with her cookie.

  State. A cluster that is thrown away at the end; the folder of the
  command line's file, under the JVM's temporary folder."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [softland.runtime.drive :as drive]
            [softland.runtime.google-stand-in :as stand-in]
            [softland.runtime.jetty :as jetty]
            [softland.runtime.main :as main]
            [softland.runtime.permissions :as perm]
            [softland.runtime.sign-in :as sign-in]
            [softland.wire.frame :as frame]
            [softland.wire.proof :refer [check finish]])
  (:import [java.io Closeable File]
           [java.net ServerSocket]
           [java.nio.file Files]
           [java.nio.file.attribute PosixFilePermissions]
           [java.util.concurrent TimeUnit]))

(defn- command-sdeps
  "When the JVM property `softland.store.path` is set (to isolate from a
  concurrent edit of `src-store/`), gives the `-Sdeps` arguments that swap
  only the store's path in the `:command` alias while preserving its
  `:replace-deps` and `:jvm-opts`; otherwise empty."
  []
  (if-let [store-path (not-empty (System/getProperty "softland.store.path"))]
    ["-Sdeps" (pr-str {:aliases {:command {:replace-paths [store-path "src-wire" "src-command"]
                                           :replace-deps  {'org.clojure/clojure {:mvn/version "1.12.4"}}
                                           :jvm-opts      ["-XX:TieredStopAtLevel=1"]}}})]
    []))

(defn- started
  "Starts the command line as a process. Takes its home folder and its
  arguments. Gives the process."
  ^Process [^File home args]
  (let [builder (ProcessBuilder. ^java.util.List (into (into ["clojure"] (command-sdeps))
                                                       (into ["-M:command" "-m" "softland.command.main"] args)))]
    (.put (.environment builder) "SOFTLAND_HOME" (.getPath home))
    (.start builder)))

(defn- ended
  "Waits for a process of the command line. Gives {:exit its status, :out
  the last EDN form that it printed, :err what it wrote beside it}."
  [^Process process]
  (let [out (future (slurp (.getInputStream process)))
        err (future (slurp (.getErrorStream process)))]
    (.waitFor process 120 TimeUnit/SECONDS)
    (let [lines (remove str/blank? (str/split-lines @out))
          [outcome form] (frame/read-one (or (last lines) ""))]
      {:exit (.exitValue process) :out (when (= :ok outcome) form) :lines lines :err @err})))

(defn- soft
  "Runs one command to its end. Takes the home folder and the arguments."
  [home & args]
  (ended (started home args)))

(defn- wait-for
  "Calls a function of no argument until it gives a thing that is truthy
  or the span passes."
  [f ms]
  (let [ends (+ (System/currentTimeMillis) ms)]
    (loop []
      (or (f) (when (< (System/currentTimeMillis) ends) (Thread/sleep 100) (recur))))))

(defn- sign-in!
  "Runs `sign-in` in a home folder and answers its ask at the page with a
  cookie. Takes the home, the runtime's address, her cookie, and the name
  of an agent or nil. Gives what the command ended with."
  [home url cookie agent]
  (let [process (started home (cond-> ["sign-in" "--url" url] agent (into ["--agent" agent])))
        err (.getErrorStream process)
        shown (StringBuilder.)
        code (wait-for (fn []
                         (while (pos? (.available err)) (.append shown (char (.read err))))
                         (second (re-find #"code: ([A-Z0-9]{4}-[A-Z0-9]{4})" (str shown))))
                       60000)]
    (when code (drive/yes url cookie code {:agent agent}))
    (assoc (ended process) :code code :shown (str shown))))

(defn -main
  "Runs the proof, prints the counts, and ends the process with a status."
  [& _]
  (let [cluster (main/in-process-cluster)
        port (with-open [socket (ServerSocket. 0)] (.getLocalPort socket))
        url (str "http://localhost:" port)
        copy (main/start {:cluster cluster :origin url :stand-in (stand-in/make)})
        served (main/serve! copy "127.0.0.1" port)
        first-person {:sub "1000001" :email "first@example.com" :name "First Person"}
        home (.toFile (Files/createTempDirectory "softland-command-proof" (make-array java.nio.file.attribute.FileAttribute 0)))
        agent-home (io/file home "agent")]
    (try
      (sign-in/setup! (:the-seam copy) {:email (:email first-person)})
      (let [cookie (:cookie (drive/sign-in url first-person))
            signed (sign-in! home url cookie nil)
            who (:who (:out signed))
            lane (:own-lane (:out signed))
            own (perm/first-permission who lane)
            file (io/file home "sign-in.edn")]
        (check "The command line's own classpath holds no Rama: the program that a person runs can open no cluster."
               (let [cmd (into (into ["clojure"] (command-sdeps)) ["-Spath" "-A:command"])
                     path (:lines (ended (.start (ProcessBuilder. ^java.util.List cmd))))]
                 (and (seq path) (not (str/includes? (str/join path) "rama")))))
        (check "`sign-in` shows a link and a code and waits; after her yes at the page it prints who it is, with its session and her own lane, and ends with 0."
               (and (:code signed) (str/includes? (:shown signed) (str url "/yes?code="))
                    (= 0 (:exit signed)) (uuid? who) (uuid? lane) (uuid? (:session (:out signed))))
               (dissoc signed :shown))
        (check "It keeps one file, which its owner alone can read, and the file holds the runtime's address, the credential and the session, and nothing of who she is."
               (and (= "rw-------" (PosixFilePermissions/toString (Files/getPosixFilePermissions (.toPath file) (make-array java.nio.file.LinkOption 0))))
                    (= #{:url :credential :session} (set (keys (second (frame/read-one (slurp file)))))))
               (keys (second (frame/read-one (slurp file)))))
        (let [wrote (soft home "write" ":chair-7" ":colour" ":green")
              read (soft home "read" ":chair-7" ":colour")
              big "123456789012345678901234567890"
              wrote-big (soft home "write" ":ledger" ":total" big)
              read-big (soft home "read" ":ledger" ":total")
              word (soft home "write" ":chair-7" ":label" "oak")]
          (check "`write` sends one fact into her own lane and ends with 0 when a gate accepted it; `read` gives it back."
                 (and (= 0 (:exit wrote)) (= :accepted (get-in wrote [:out :answer :answer]))
                      (= :green (get-in read [:out :place :value])))
                 [wrote read])
          (check "An integer of thirty digits is written and read back digit for digit: the command line holds a number as itself."
                 (and (= 0 (:exit wrote-big)) (= (bigint big) (get-in read-big [:out :place :value])))
                 [(:out wrote-big) (:out read-big)])
          (check "A bare word on the command line is a text."
                 (= "oak" (get-in (soft home "read" ":chair-7" ":label") [:out :place :value])) word))
        (let [refused (soft home "write" ":chair-7" ":colour" ":red" "--permission" (pr-str (perm/permission-id (perm/given-permission who lane own :see))))]
          (check "An act that a gate refuses ends the command with 3, and the answer says refused: a script can tell it from a command that could not be done."
                 (and (= 3 (:exit refused)) (= :refused (get-in refused [:out :answer :answer])))
                 refused))
        (let [one (:out (soft home "who"))
              two (:out (soft home "who"))
              fresh (:out (soft home "new-session"))]
          (check "The commands of one sign-in share one session, and `new-session` leaves it and starts another."
                 (and (= (:session (:out signed)) (:session one) (:session two))
                      (uuid? (:session fresh)) (not= (:session one) (:session fresh)))
                 [one two fresh]))
        (let [watching (started home ["watch" ":lamp-2" ":lit"])
              out (.getInputStream watching)
              seen (StringBuilder.)
              has (fn [text] (wait-for (fn []
                                         (while (pos? (.available out)) (.append seen (char (.read out))))
                                         (str/includes? (str seen) text))
                                       30000))]
          (check "`watch` prints the whole answer and stays; when another run of the command line writes the place, it prints the change."
                 (and (has ":anew")
                      (do (soft home "write" ":lamp-2" ":lit" "true") true)
                      (has ":changed") (has "true"))
                 (str seen))
          (.destroyForcibly watching))
        (let [made (soft home "agent" "new" "importer" "--out" (.getPath (io/file agent-home "sign-in.edn")))
              agent (:made (:out made))
              as-agent (:out (soft agent-home "who"))
              before (soft agent-home "write" ":crate-1" ":packed" "true" "--lane" (str lane)
                           "--permission" (pr-str (perm/permission-id (perm/first-permission (perm/agent agent) lane))))
              given (pr-str (perm/permission-id (perm/given-permission (perm/agent agent) lane own :see-and-act)))
              gave (soft home "give" (str agent) (str lane) "act")
              after (soft agent-home "write" ":crate-1" ":packed" "true" "--lane" (str lane)
                          "--permission" given)]
          (check "`agent new` makes an agent with a who of its own and writes the file that it signs in with; with that file the command line is the agent, beneath her."
                 (and (= 0 (:exit made)) (uuid? agent) (not= who agent)
                      (= agent (:who as-agent)) (= who (:root as-agent)))
                 [(:out made) as-agent])
          (check "An agent's write is refused by a gate before she gives it a permission, and accepted after `give`."
                 (and (= 3 (:exit before)) (= 0 (:exit gave)) (= 0 (:exit after))
                      (= :accepted (get-in after [:out :answer :answer])))
                 [(:out before) (:out gave) (:out after)])
          (check "`under` lists the agent with its name, and `agent end` ends it: its file then signs in as nobody, and the command ends with 1."
                 (and (some (fn [actor] (= "importer" (:label actor))) (:actors (:out (soft home "under"))))
                      (= 0 (:exit (soft home "agent" "end" (str agent))))
                      (= 1 (:exit (soft agent-home "who"))))))
        (let [acts (io/file home "acts.edn")
              _ (spit acts (str/join "\n" (for [n (range 3)]
                                            (pr-str {:version 1 :lane lane :permission (perm/permission-id own)
                                                     :facts [{:e :shelf :k (keyword (str "slot-" n)) :v n}]}))))
              first-run (soft home "send" (.getPath acts))
              second-run (soft home "send" (.getPath acts))]
          (check "`send` sends the acts of a file in order, and a second run of it sends the same acts under the same UUIDs: the answers of the two runs are the same, so each act was taken once."
                 (and (= 0 (:exit first-run)) (= 3 (count (:results (:out first-run))))
                      (every? (fn [r] (= :accepted (get-in r [:answer :answer]))) (:results (:out first-run)))
                      (= (map :answer (:results (:out first-run))) (map :answer (:results (:out second-run)))))
                 [(:out first-run) (:out second-run)]))
        (let [invited (soft home "invite")
              code (second (re-find #"invited=([A-Za-z0-9_-]+)" (str (:link (:out invited)))))
              guest (drive/sign-in url {:sub "3000001" :email "guest@example.com" :name "A Guest" :invited code})]
          (check "`invite` prints the id of a person who waits and a link; the account that opens the link becomes that person."
                 (and (= 0 (:exit invited)) code (= (:invited (:out invited)) (:person guest)))
                 [(:out invited) (dissoc guest :cookie)]))
        (check "A command that it does not know ends with 2, and `sign-out` ends the sign-in: the next command ends with 1 and says to sign in."
               (and (= 2 (:exit (soft home "fly")))
                    (= 0 (:exit (soft home "sign-out")))
                    (let [after (soft home "who")]
                      (and (= 1 (:exit after)) (str/includes? (str (:said (:out after))) "sign-in"))))))
      (check "The proof ran to its end without a throw." true)
      (catch Throwable t
        (check "The proof ran to its end without a throw." false (str t))
        (.printStackTrace t))
      (finally
        (try (jetty/stop! served) (catch Throwable _ nil))
        (try ((:stop copy)) (catch Throwable _ nil))
        (try (.close ^Closeable cluster) (catch Throwable _ nil))))
    (finish)))
