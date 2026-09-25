;; Phase 8 plan probe (no cluster): the model's own account of each fixed
;; history in scenarios.clj under `baseline`, as a replay would compare it.
;; Run from src/proposal/formal-model-2026-09-24:
;;   clojure -M -i ../rig-2026-09-25/runs/phase8-model-traces.clj
;; For each case: play's verdict and what it saw; every answer recorded
;; under every name any offer used (client, operator, store), in the order
;; first sent; each read's facts (open with token, or erased) and
;; promotions; then the model's trace.
(require '[formal.model :as m] '[formal.scenarios :as sc] '[clojure.string :as str])

(defn answers [st]
  (for [nm (distinct (concat (keys (:sent st))))
        :let [as (m/answers-for st nm)]
        :when (not (str/starts-with? nm "grant:"))]
    [nm (vec (for [a as] [(:where a) (:answer a) (:reason a)]))]))

(defn read-line* [r]
  (str "as of " (:as-of r) ": "
       (str/join ", " (for [f (:facts r)]
                        (str (m/fid-str (:id f)) " " (name (:layer f)) " "
                             (if (:erased-at f) (str "erased@" (:erased-at f))
                                 (or (get-in f [:value :token]) (pr-str (:value f)))))))
       (when (seq (:promotions r))
         (str " | promotions: " (str/join ", " (for [p (:promotions r)] (str (:request p) " " (name (:status p)))))))))

(doseq [[label cases] [["A" sc/a-cases] ["B" sc/b-cases] ["D" sc/d-cases]]
        [i [nm history expect :as c]] (map-indexed vector cases)]
  (let [[ok seen _] (sc/play m/baseline c [])
        st (m/run m/baseline history)]
    (println "==" (str label (inc i)) (if ok "as said" "DIFFERS") "-" nm)
    (println "   history:" (pr-str history))
    (println "   seen:" (pr-str seen))
    (println "   client-sent:" (pr-str (:client-sent st)))
    (doseq [[n as] (answers st)] (println "   answer" n (pr-str as)))
    (doseq [r (:reads st)] (println "   read" (read-line* r)))
    (println "   persons:" (pr-str (:persons st)))
    (doseq [t (:trace st)] (println "   |" t))))
