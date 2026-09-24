(ns rig.test-runner
  "Runs clojure.test over the namespaces named on the command line and exits
  non-zero on any failure or error."
  (:require [clojure.test :as t]))

(defn -main [& nss]
  (let [syms (map symbol nss)]
    (doseq [s syms] (require s))
    (let [{:keys [fail error] :as r} (apply t/run-tests syms)]
      (println (select-keys r [:test :pass :fail :error]))
      (shutdown-agents)
      (System/exit (if (zero? (+ fail error)) 0 1)))))
