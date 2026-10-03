(ns rig.bench-runner
  "Runs one measurement: clojure -M:bench <namespace> <function> [args ...].
  The function takes the remaining arguments as strings and prints its own
  results.")

(defn -main [ns-name fn-name & args]
  (let [ns-sym (symbol ns-name)]
    (require ns-sym)
    (apply (ns-resolve ns-sym (symbol fn-name)) args)
    (shutdown-agents)
    (System/exit 0)))
