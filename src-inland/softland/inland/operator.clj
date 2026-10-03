(ns softland.inland.operator
  "The operator's command line (`bin/inland forget|seed ...`): acts no screen
   gesture makes. Takes a running cluster with the rig's module; gives the
   store's answers, printed. Connects as the host does (store/connect!, which
   makes the first facts when they are not in) and exits."
  (:require [softland.inland.seed :as seed]
            [softland.inland.store :as store]))

(defn -main
  "`forget <person>`: the operator forgets a person; their lock is destroyed on
   every task before the answer. `seed`: genesis records put again where they
   changed. Exits 0 when every act was admitted."
  [& [command arg]]
  (store/connect!)
  (let [answers (case command
                  "forget" [(store/forget-person! (keyword arg))]
                  "seed" (seed/put-genesis! (:store @store/connection))
                  (do (println "bin/inland forget <person> | seed") []))]
    (doseq [a answers] (println (select-keys a [:answer :reason :stamp :name])))
    (System/exit (if (every? #(= :yes (:answer %)) answers) 0 1))))
