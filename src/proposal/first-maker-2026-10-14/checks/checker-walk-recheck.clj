;; The checker's own run, 15 October 2026. Reads runs/walk-act-maps.edn (does
;; not write it), gives each act again to softland.store.act/parse, and each
;; act that carries a door word again to softland.store.door.declaration/check,
;; arriving as act-id [:base :microbatch a0000000-0000-4000-8000-<n>] (the walk
;; script's own act-uuid form, with arrive-as = n), depot :microbatch, the
;; walk's fixed time 1760500000000. Then the four queries again through
;; softland.store.read-path.query/check. Prints only; writes nothing.
;; Run from the repository root:
;;   clojure -M:store src/proposal/first-maker-2026-10-14/checks/checker-walk-recheck.clj
(require (quote [clojure.java.shell]))
(require '[softland.store.act :as act]
         '[softland.store.door.declaration :as declaration]
         '[softland.store.read-path.query :as query]
         '[clojure.edn :as edn])

(def acts (edn/read-string (slurp "src/proposal/first-maker-2026-10-14/runs/walk-act-maps.edn")))
(defn act-uuid [n] (java.util.UUID/fromString (format "a0000000-0000-4000-8000-%012d" n)))
(defn word [r] (if (:ok r) :ok (:refuse r)))

(doseq [{:keys [n act parse-word door-check-word]} acts]
  (let [p (word (act/parse act))
        d (when door-check-word
            (word (declaration/check (assoc act :act-id [:base :microbatch (act-uuid n)]
                                            :depot :microbatch
                                            declaration/time-part 1760500000000))))]
    (println (format "act %2d parse recorded %-12s again %-12s %s | door recorded %-20s again %-20s %s"
                     n parse-word p (if (= p parse-word) "SAME" "DIFFERS")
                     (pr-str door-check-word) (pr-str d) (if (= d door-check-word) "SAME" "DIFFERS")))))

(def key-as-uuid #uuid "6b65792d-0000-4000-8000-00000000d0c0")
(doseq [[n q] [[1 {:asks [:place :softland.store.gate.stamp/next-stamp :code/doc] :asker :system :layers [:base]}]
               [2 {:asks [:entity :softland.store.gate.stamp/next-stamp] :asker :system :layers [:base] :with #{:writer :time}}]
               [3 {:asks [:place :softland.store.gate.stamp/next-stamp key-as-uuid] :asker :system :layers [:base]}]]]
  (let [r (query/check q)]
    (println "query" n (if (:ok r) :ok [(:refuse r) (:at r)]))))
(println "max-facts" declaration/max-facts "max-value-bytes" (try (deref (resolve 'softland.store.door.declaration/max-value-bytes)) (catch Exception e :not-found)))

;; Act 12 is elided in the EDN (its note says so). Rebuild it with the whole
;; src-store/README.md at HEAD c90c7bcc, read from git, and check it again.
(let [readme (:out (clojure.java.shell/sh "git" "show" "c90c7bcc:src-store/README.md"))
      a (nth acts 12)
      real (update-in (:act a) [:facts] (fn [fs] (mapv #(assoc % :v readme) fs)))]
  (println "act 12 with the real README:" (count (.getBytes ^String readme "UTF-8")) "bytes;"
           "parse" (word (act/parse real))
           "door" (word (declaration/check (assoc real :act-id [:base :microbatch (act-uuid 12)]
                                                  :depot :microbatch
                                                  declaration/time-part 1760500000000)))))
