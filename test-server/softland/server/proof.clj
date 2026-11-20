(ns softland.server.proof
  "What the server's proofs share: one way to say what a check holds, to
  print whether it held, and to end the process with a status. It requires
  nothing outside Clojure.

  A check is a sentence and a value. The sentence says what must hold, in
  words that stay true when the code changes. A run prints `PASSED` or
  `FAILED` in front of the sentence, and for a failure what was seen. So a
  check is explained once, in its test file, and not a second time on a
  page (src-store/CLAUDE.md, \"The checks explain themselves\").

  State. One atom of the counts of the run, in the memory of the process.")

(defonce ^{:doc "The counts of this run: how many checks passed and failed."}
  counts (atom {:passed 0 :failed 0}))

(defn check
  "Takes a sentence that says what must hold, whether it held, and when the
  caller wants, what was seen. Prints one line, counts it, and gives
  whether it held."
  ([sentence held?] (check sentence held? nil))
  ([sentence held? seen]
   (if held?
     (do (swap! counts update :passed inc)
         (println "PASSED" sentence))
     (do (swap! counts update :failed inc)
         (println "FAILED" sentence)
         (when (some? seen) (println "       seen:" (pr-str seen)))))
   (boolean held?)))

(defn finish
  "Prints the counts of the run and ends the process: status 0 when no
  check failed, 1 otherwise."
  []
  (let [{:keys [passed failed]} @counts]
    (println)
    (println (str passed " passed, " failed " failed."))
    (flush)
    (shutdown-agents)
    (System/exit (if (zero? failed) 0 1))))
