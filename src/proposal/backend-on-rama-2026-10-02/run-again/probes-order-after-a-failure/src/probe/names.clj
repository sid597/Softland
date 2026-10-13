(ns probe.names
  "Reads the loaded Rama 1.6.0 jar, with no cluster: which public names of
  `com.rpl.rama`, `com.rpl.rama.ops` and `com.rpl.rama.test` could give a
  stream event the offset of its source record, or fail or queue a record
  again without a throw. It prints the names that match a few words, and the
  docstring of each. A probe, not a design."
  (:require [clojure.string :as str]
            [com.rpl.rama]
            [com.rpl.rama.ops]
            [com.rpl.rama.path]
            [com.rpl.rama.test]))

(def words
  "The words looked for in a public name."
  ["offset" "current" "fail" "retry" "source" "ack" "pause" "resume" "option" "yield" "partition-info" "depot"])

(defn -main [& _]
  (doseq [n '[com.rpl.rama com.rpl.rama.ops com.rpl.rama.test]]
    (let [vars (sort-by key (ns-publics n))]
      (println "PROBE" n "has" (count vars) "public names")
      (doseq [[sym v] vars
              :when (some #(str/includes? (name sym) %) words)]
        (let [doc (str/replace (or (:doc (meta v)) "") #"\s+" " ")]
          (println "PROBE  " (str n "/" sym)
                   (pr-str (:arglists (meta v)))
                   "·" (subs doc 0 (min 150 (count doc))))))))
  (shutdown-agents)
  (System/exit 0))
