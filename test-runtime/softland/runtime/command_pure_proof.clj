(ns softland.runtime.command-pure-proof
  "The proof of the command line's parts that need no runtime: how each
  command is typed, which plain arguments and flags it takes, under which
  UUID an act of a file is sent, and the exit status of a result. One
  command, from the repository root:

    clojure -M:runtime -m softland.runtime.command-pure-proof

  It calls private functions of `softland.command.main` through their vars,
  with values that the proof makes. What the command line does with them as
  a program, against a runtime, is `command_review_proof.clj`.

  State. None."
  (:require [clojure.string :as str]
            [softland.command.main]
            [softland.wire.proof :refer [check finish]])
  (:import [java.util UUID]))

(defn- command
  "Calls a private function of the command line by its name. A name that
  the command line does not hold gives `:no-such-function`, so that the
  sentence fails and the run goes on."
  [name & args]
  (if-let [f (ns-resolve 'softland.command.main name)]
    (apply f args)
    :no-such-function))

;; ------------------------------------------------- how a command is typed

(def ^:private as-typed
  "Each command with as many plain arguments as it takes."
  [["sign-in"] ["who"] ["write" ":chair-7" ":colour" ":green"] ["read" ":chair-7" ":colour"]
   ["watch" ":chair-7" ":colour"] ["send" "acts.edn"] ["give" "a-who" "a-lane" "see"] ["invite"]
   ["agent" "new" "importer"] ["agent" "end" "a-uuid"] ["under"] ["end-sign-in" "a-uuid"]
   ["replace-secret"] ["new-session"] ["sign-out"]])

(def ^:private how-each-is-typed
  "How each command is typed, written out here a second time as literals,
  beside the command line's one list of its commands: a change of a
  command's arguments or flags is made in two places on purpose."
  ["sign-in [--url address] [--agent name]"
   "who"
   "write e k v [--lane id] [--permission uuid]; a retract is typed with the value nil"
   "read e k [--lane id]"
   "watch e k [--lane id]"
   "send file"
   "give who lane see|act [--permission uuid]"
   "invite [--email address]"
   "agent new name [--out file]"
   "agent end uuid"
   "under"
   "end-sign-in uuid"
   "replace-secret"
   "new-session [--model name]"
   "sign-out"])

(defn- argument-checks []
  (let [read (fn [[name & args]] (command 'read-command name args))
        left-to-run? (fn [typed] (let [got (read typed)] (and (map? got) (fn? (:does got)) (not (contains? got :could-not)))))
        not-read (fn [typed] (let [got (read typed)] (when (and (map? got) (= :read-the-command (:could-not got))) got)))
        not-read? (fn [[name :as typed]] (str/starts-with? (str (:said (not-read typed))) name))
        why (fn [typed] (select-keys (not-read typed) [:why :at]))
        commands @(ns-resolve 'softland.command.main 'commands)]
    (check "The command line's one list of its commands gives, for each, how it is typed, and each line is the one that this proof holds: the plain arguments by name, then the flags that the command takes beside `--as`, each with the name of its value."
           (= how-each-is-typed (mapv (fn [[named]] (command 'typed named)) commands))
           (mapv (fn [[named]] (command 'typed named)) commands))
    (check "Each entry of that list holds a function that does the command and says what the command does."
           (every? (fn [[_ entry]] (and (fn? (:does entry)) (string? (:says entry)) (vector? (:args entry)))) commands))
    (check "Each command that is typed with its own number of plain arguments is left to run: sign-in 0, who 0, write 3, read 2, watch 2, send 1, give 3, invite 0, agent new 1, agent end 1, under 0, end-sign-in 1, replace-secret 0, new-session 0, sign-out 0."
           (every? left-to-run? as-typed)
           (remove left-to-run? as-typed))
    (check "Each of them typed with one plain argument more, or with one fewer where it takes any, is not run: the result says that the command could not be read, and how the command is typed."
           (every? not-read? (concat (map (fn [typed] (conj typed "one-more")) as-typed)
                                     (map pop (filter next as-typed))))
           (remove not-read? (concat (map (fn [typed] (conj typed "one-more")) as-typed)
                                     (map pop (filter next as-typed)))))
    (check "A `--flag value` pair is no plain argument: `write e k v --lane l --permission p` is left to run, and `write e k --lane l` is not."
           (and (left-to-run? ["write" ":e" ":k" ":v" "--lane" "l" "--permission" "p"])
                (not-read? ["write" ":e" ":k" "--lane" "l"]))
           [(read ["write" ":e" ":k" ":v" "--lane" "l" "--permission" "p"]) (read ["write" ":e" ":k" "--lane" "l"])])
    (check "`write e k` with the value left out is not run, and what it says names how a retract is typed: the value nil."
           (and (not-read? ["write" ":e" ":k"]) (str/includes? (:said (read ["write" ":e" ":k"])) "nil"))
           (read ["write" ":e" ":k"]))
    (check "The value that is typed `nil` is read as nil, so `write e k nil` is the retract."
           (and (nil? (command 'value-of "nil")) (left-to-run? ["write" ":e" ":k" "nil"]))
           [(command 'value-of "nil") (read ["write" ":e" ":k" "nil"])])
    (check "A flag whose value is left out is not run, and the result names the flag: `write e k v --lane`, which would be written into her own lane; `new-session --model`; and `write e k v --lane --as file`, where the next thing typed is another flag."
           (= [{:why :flag-with-no-value :at "--lane"} {:why :flag-with-no-value :at "--model"} {:why :flag-with-no-value :at "--lane"}]
              (mapv why [["write" ":e" ":k" ":v" "--lane"] ["new-session" "--model"] ["write" ":e" ":k" ":v" "--lane" "--as" "file"]]))
           (mapv why [["write" ":e" ":k" ":v" "--lane"] ["new-session" "--model"] ["write" ":e" ":k" ":v" "--lane" "--as" "file"]]))
    (check "A flag that the command does not take is not run, and the result names the flag: one that no command takes, `write e k v --lain l`; one that another command takes, `who --lane l` and `read e k --permission p`; and one that the other command of two words takes, `agent end uuid --out file`."
           (= [{:why :flag-not-known :at "--lain"} {:why :flag-not-known :at "--lane"} {:why :flag-not-known :at "--permission"}
               {:why :flag-not-known :at "--out"}]
              (mapv why [["write" ":e" ":k" ":v" "--lain" "l"] ["who" "--lane" "l"] ["read" ":e" ":k" "--permission" "p"]
                         ["agent" "end" "a-uuid" "--out" "file"]]))
           (mapv why [["write" ":e" ":k" ":v" "--lain" "l"] ["who" "--lane" "l"] ["read" ":e" ":k" "--permission" "p"]
                      ["agent" "end" "a-uuid" "--out" "file"]]))
    (check "A flag that is given twice is not run: of `write e k v --lane a --lane b` the command line cannot say which lane is meant."
           (= {:why :flag-given-twice :at "--lane"} (why ["write" ":e" ":k" ":v" "--lane" "a" "--lane" "b"]))
           (why ["write" ":e" ":k" ":v" "--lane" "a" "--lane" "b"]))
    (check "Every command takes `--as file`, before or after its plain arguments."
           (every? left-to-run? (concat (map (fn [typed] (into typed ["--as" "a-file"])) as-typed)
                                        (map (fn [[name & args]] (into [name "--as" "a-file"] args)) as-typed)))
           (remove left-to-run? (concat (map (fn [typed] (into typed ["--as" "a-file"])) as-typed)
                                        (map (fn [[name & args]] (into [name "--as" "a-file"] args)) as-typed))))
    (check "A command that the command line does not know, and no command at all, could not be read, and the result names the commands, each once; `agent` with a word that is neither `new` nor `end`, or with none, says how the two commands of `agent` are typed."
           (let [names "sign-in, who, write, read, watch, send, give, invite, agent, under, end-sign-in, replace-secret, new-session, sign-out"
                 agents "agent new name [--out file] | agent end uuid"]
             (= [{:could-not :read-the-command :why :command-not-known :said names}
                 {:could-not :read-the-command :why :command-not-known :said names}
                 {:could-not :read-the-command :why :command-not-known :said agents}
                 {:could-not :read-the-command :why :command-not-known :said agents}]
                [(read ["fly" "away"]) (read [nil]) (read ["agent" "rename" "importer"]) (read ["agent"])]))
           [(read ["fly" "away"]) (read [nil]) (read ["agent" "rename" "importer"]) (read ["agent"])])
    (check "A command that could not be read is not run: `run` gives the same result as the reading, and calls no command's function."
           (= (read ["write" ":e" ":k" ":v" "--lane"]) (command 'run "write" [":e" ":k" ":v" "--lane"]))
           (command 'run "write" [":e" ":k" ":v" "--lane"]))))

;; --------------------------------------------------------------- the journal

(defn- journal-checks []
  (let [her (UUID/randomUUID)
        agent (UUID/randomUUID)
        journal (fn [who entries acts] (command 'journal-of who entries acts (fn [] (UUID/randomUUID))))
        act (fn [n] {:version 1 :facts [{:e :shelf :k (keyword (str "slot-" n)) :v n}]})
        acts [(act 0) (act 1) (act 2)]
        first-run (journal her nil acts)
        uuids (:uuids first-run)
        whole? (fn [got n] (and (= n (count (:uuids got))) (every? uuid? (:uuids got))))]
    (check "A file that is sent for the first time gets a UUID for each act, each its own, and the journal keeps for each act who sent it, the act as the file holds it and its UUID, in the file's order."
           (and (whole? first-run 3) (= 3 (count (set uuids)))
                (= (:sent first-run) (mapv (fn [a u] {:who her :act a :uuid u}) acts uuids)))
           first-run)
    (let [again (journal her (:sent first-run) acts)]
      (check "The same file sent again by the same who gives the same UUIDs, and the journal to keep is the one that was kept."
             (and (whole? again 3) (= uuids (:uuids again)) (= (:sent first-run) (:sent again)))
             again))
    (let [edited (journal her (:sent first-run) [(act 0) (act 7) (act 2)])]
      (check "When one act of a file is edited and the count stays, the edited act gets a new UUID, the other acts keep theirs, and the entry of the act that the file holds no more is dropped."
             (and (whole? edited 3)
                  (= [(uuids 0) (uuids 2)] [((:uuids edited) 0) ((:uuids edited) 2)])
                  (not (contains? (set uuids) ((:uuids edited) 1)))
                  (= [(act 0) (act 7) (act 2)] (mapv :act (:sent edited))))
             edited))
    (let [turned (journal her (:sent first-run) [(act 2) (act 0) (act 1)])]
      (check "Acts in another order keep their UUIDs: a UUID belongs to its act and not to the act's place in the file."
             (and (whole? turned 3) (= [(uuids 2) (uuids 0) (uuids 1)] (:uuids turned)))
             turned))
    (let [twice (journal her nil [(act 0) (act 0)])
          twice-again (journal her (:sent twice) [(act 0) (act 0)])
          thrice (journal her (:sent twice) [(act 0) (act 0) (act 0)])]
      (check "Two equal acts in one file are two acts and take two UUIDs; sent again they keep the two, and a third equal act gets a new one."
             (and (whole? twice 2) (= 2 (count (set (:uuids twice))))
                  (= (:uuids twice) (:uuids twice-again))
                  (whole? thrice 3) (= (:uuids twice) (subvec (:uuids thrice) 0 2))
                  (not (contains? (set (:uuids twice)) ((:uuids thrice) 2))))
             [twice twice-again thrice]))
    (let [as-agent (journal agent (:sent first-run) acts)]
      (check "The same acts sent by another who get new UUIDs, and the journal then keeps them under that who."
             (and (whole? as-agent 3) (empty? (filter (set uuids) (:uuids as-agent)))
                  (= #{agent} (set (map :who (:sent as-agent)))))
             as-agent))
    (let [earlier {:uuids uuids :session (UUID/randomUUID)}
          got (journal her (:sent earlier) acts)]
      (check "A journal in the earlier form, `{:uuids [...] :session s}`, holds no entry under `:sent`, so nothing of it is matched: each act gets a new UUID."
             (and (whole? got 3) (empty? (filter (set uuids) (:uuids got))))
             got))))

;; ------------------------------------------------------------ the exit status

(defn- status-checks []
  (let [status (fn [result] (command 'status-of result))
        could-not {:could-not :reach-the-runtime :said "the connection was closed"}
        accepted {:answer {:answer :accepted}}
        refused {:answer {:answer :refused}}]
    (check "A command that could not be read gives 2."
           (= 2 (status {:could-not :read-the-command :said "write e k v"}))
           (status {:could-not :read-the-command :said "write e k v"}))
    (check "A result that holds `:could-not` gives 1."
           (= 1 (status could-not)) (status could-not))
    (check "A `send` one of whose results holds `:could-not` gives 1."
           (= 1 (status {:results [accepted could-not]})) (status {:results [accepted could-not]}))
    (check "A `send` that holds both a refusal and a \"could not\" gives 1: the run did not finish."
           (= 1 (status {:results [refused could-not]})) (status {:results [refused could-not]}))
    (check "A `watch` that ended with a word that holds `:could-not` gives 1."
           (= 1 (status {:ended could-not})) (status {:ended could-not}))
    (check "A `sign-in` whose ask ran out, `{:state :over}`, gives 1."
           (= 1 (status {:state :over})) (status {:state :over}))
    (check "An act that a gate refused gives 3, as the result and as a result of `send`."
           (= [3 3] [(status refused) (status {:results [accepted refused]})])
           [(status refused) (status {:results [accepted refused]})])
    (check "Her no to the sign-in, `{:state :no}`, gives 3."
           (= 3 (status {:state :no})) (status {:state :no}))
    (check "Everything else gives 0: an act that was accepted, a `send` whose every act was accepted, a hello, a place that was read; and an answer of `:nothing-yet` or `:not-mine`, which is neither a \"could not\" nor a refusal."
           (every? (fn [result] (= 0 (status result)))
                   [accepted {:results [accepted accepted]} {:who (UUID/randomUUID) :session (UUID/randomUUID)}
                    {:place {:value :green}} {:answer {:answer :nothing-yet}} {:answer {:answer :not-mine}}])
           (mapv status [accepted {:results [accepted accepted]} {:who (UUID/randomUUID)}
                         {:place {:value :green}} {:answer {:answer :nothing-yet}} {:answer {:answer :not-mine}}]))))

(defn -main
  "Runs every check, prints the counts, and ends the process with a status."
  [& _]
  (argument-checks)
  (journal-checks)
  (status-checks)
  (finish))
