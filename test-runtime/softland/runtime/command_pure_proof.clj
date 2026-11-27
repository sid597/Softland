(ns softland.runtime.command-pure-proof
  "The proof of the command line's parts that need no runtime: how many
  plain arguments a command takes, under which UUID an act of a file is
  sent, and the exit status of a result. One command, from the repository
  root:

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

;; ------------------------------------------------------- the plain arguments

(def ^:private as-typed
  "Each command with as many plain arguments as it takes."
  [["sign-in"] ["who"] ["write" ":chair-7" ":colour" ":green"] ["read" ":chair-7" ":colour"]
   ["watch" ":chair-7" ":colour"] ["send" "acts.edn"] ["give" "a-who" "a-lane" "see"] ["invite"]
   ["agent" "new" "importer"] ["agent" "end" "a-uuid"] ["under"] ["end-sign-in" "a-uuid"]
   ["replace-secret"] ["new-session"] ["sign-out"]])

(defn- argument-checks []
  (let [mistyped (fn [[name & args]] (command 'mistyped name args))
        not-read? (fn [[name :as typed]]
                    (let [got (mistyped typed)]
                      (and (map? got)
                           (= :read-the-command (:could-not got))
                           (str/starts-with? (str (:said got)) name))))]
    (check "Each command that is typed with its own number of plain arguments is left to run: sign-in 0, who 0, write 3, read 2, watch 2, send 1, give 3, invite 0, agent new 2, agent end 2, under 0, end-sign-in 1, replace-secret 0, new-session 0, sign-out 0."
           (every? (fn [typed] (nil? (mistyped typed))) as-typed)
           (remove (fn [typed] (nil? (mistyped typed))) as-typed))
    (check "Each of them typed with one plain argument more, or with one fewer where it takes any, is not run: the result says that the command could not be read, and how the command is typed."
           (every? not-read? (concat (map (fn [typed] (conj typed "one-more")) as-typed)
                                     (map pop (filter next as-typed))))
           (remove not-read? (concat (map (fn [typed] (conj typed "one-more")) as-typed)
                                     (map pop (filter next as-typed)))))
    (check "A `--flag value` pair is no plain argument: `write e k v --lane l --permission p` is left to run, and `write e k --lane l` is not."
           (and (nil? (mistyped ["write" ":e" ":k" ":v" "--lane" "l" "--permission" "p"]))
                (not-read? ["write" ":e" ":k" "--lane" "l"]))
           [(mistyped ["write" ":e" ":k" ":v" "--lane" "l" "--permission" "p"]) (mistyped ["write" ":e" ":k" "--lane" "l"])])
    (check "`write e k` with the value left out is not run, and what it says names how a retract is typed: the value nil."
           (and (not-read? ["write" ":e" ":k"]) (str/includes? (:said (mistyped ["write" ":e" ":k"])) "nil"))
           (mistyped ["write" ":e" ":k"]))
    (check "The value that is typed `nil` is read as nil, so `write e k nil` is the retract."
           (and (nil? (command 'value-of "nil")) (nil? (mistyped ["write" ":e" ":k" "nil"])))
           [(command 'value-of "nil") (mistyped ["write" ":e" ":k" "nil"])])
    (check "A command that the command line does not know, and no command at all, are left to run, which names the commands."
           (and (nil? (mistyped ["fly" "away"])) (nil? (mistyped [nil])))
           [(mistyped ["fly" "away"]) (mistyped [nil])])))

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
