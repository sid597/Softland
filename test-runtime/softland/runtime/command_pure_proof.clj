(ns softland.runtime.command-pure-proof
  "The proof of the command line's parts that need no runtime: how many
  plain arguments a command takes. One command, from the repository root:

    clojure -M:runtime -m softland.runtime.command-pure-proof

  It calls private functions of `softland.command.main` through their vars,
  with values that the proof makes. What the command line does with them as
  a program, against a runtime, is `command_review_proof.clj`.

  State. None."
  (:require [clojure.string :as str]
            [softland.command.main]
            [softland.wire.proof :refer [check finish]]))

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

(defn -main
  "Runs every check, prints the counts, and ends the process with a status."
  [& _]
  (argument-checks)
  (finish))
