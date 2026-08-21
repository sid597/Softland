;; R5 — restart readback receipt.  READ-ONLY: only read-* fns, no depot append,
;; no ingest, no migration.  Proves every written kind survives a cluster bounce.
;;
;;   bin/land up                    # then wait for RUNNING x5
;;   clj -M:dev -e '(load-file "bin/r5_readback.clj")'
;;
;; Overrides (all optional):
;;   R5_ADDRESS  R5_TURN  R5_FACE  R5_RELATION_TARGET  R5_FILE_KEY

(require '[app.server.rama.cluster :as cluster]
         '[app.server.rama.object-container.runtime :as ocr]
         '[app.server.rama.relation-kernel :as rk]
         '[app.server.rama.face-arsenal :as fa]
         '[clojure.string :as str])

(def ADDRESS (or (System/getenv "R5_ADDRESS")
                 "chat:77088a4f028100d3e93a99d291e2a184c7ed6ca66b3a998a74f9fbddfe62b34b"))
(def TURN-ID (or (System/getenv "R5_TURN") "waist-close-turn"))
(def FACE    (or (System/getenv "R5_FACE") "outline-face"))
(def REL-TARGET (System/getenv "R5_RELATION_TARGET"))
(def FILE-KEY   (System/getenv "R5_FILE_KEY"))

(def !rows (atom []))

(defn- preview [x]
  (let [s (pr-str x)]
    (if (> (count s) 220) (str (subs s 0 220) " …") s)))

(defn probe
  "expect: :present | :absent | :info .  Returns the read value (nil on throw)."
  [label expect f]
  (let [r (try {:got (f)} (catch Throwable t {:err (str (.getSimpleName (class t))
                                                        ": " (.getMessage t))}))
        got (:got r)
        verdict (cond (:err r)            :ERROR
                      (= expect :info)    :INFO
                      (= expect :absent)  (if (nil? got) :PASS :FAIL)
                      :else               (if (some? got) :PASS :FAIL))]
    (swap! !rows conj (assoc r :label label :expect expect :verdict verdict))
    (println (format "  %-6s %-42s %s" (name verdict) label
                     (if (:err r) (:err r) (preview got))))
    got))

(println "\nR5 — restart readback   READ-ONLY")
(println "address:" ADDRESS)
(println (apply str (repeat 78 \-)))

(if-not (some? (cluster/manager))
  (do (println "\nCLUSTER DOWN — no manager. Run `bin/land up`, wait for RUNNING x5,")
      (println "then re-run. (`cd rama-release && ./rama moduleStatus <module-var>`)")
      (System/exit 2))
  (let [ctx        (cluster/face-projection-runtime)
        oc-rt      (:oc-rt ctx)
        arsenal-rt (:arsenal-rt ctx)
        ;; NOTE the naming: :rk-rt IS cluster/trail-runtime, whose own docstring
        ;; says ":module-name = the RELATION kernel name". Trail bundle, relation reads.
        rk-rt      (:rk-rt ctx)]

    (println "\n[1] TEXT — container + current revision (object-container)")
    (probe "read-container" :present #(ocr/read-container oc-rt ADDRESS))
    (probe "read-current-revision" :present #(ocr/read-current-revision oc-rt ADDRESS))

    (println "\n[2] TURNS / PLACEMENT / CAMERA — conversation projection")
    (let [cells (probe "conversation-projection (all cells)" :present
                       #(vec (ocr/read-transcript-conversation-projection oc-rt ADDRESS "" 500)))
          ks    (mapv #(or (:cell/key %) (:cell/id %) (first (keys %))) (or cells []))]
      (println (format "  INFO   %-42s %s cells" "cell count" (count (or cells []))))
      (probe (str "cell matching turn " TURN-ID) :present
             #(first (filter (fn [c] (str/includes? (pr-str c) TURN-ID)) cells)))
      (probe "cell(s) matching geo/camera" :present
             #(seq (filter (fn [c] (re-find #"geo[:\-]|camera" (pr-str c))) cells)))
      (println "\n[3] UNITS — derived unit + geometry (ids discovered from the projection)")
      (let [unit-ids (->> (re-seq #"du:[A-Za-z0-9:_\-\.]+" (pr-str cells)) distinct (take 3) vec)]
        (println (format "  INFO   %-42s %s" "discovered unit ids" (preview unit-ids)))
        (doseq [uid unit-ids]
          (probe (str "read-unit " (subs uid 0 (min 34 (count uid))) "…") :present
                 #(ocr/read-unit oc-rt uid)))
        (when (empty? unit-ids)
          (probe "read-unit (no id discovered)" :info (constantly ::none)))))

    (println "\n[4] WEAR — face-arsenal (pre-restart nil MUST still be nil)")
    (probe (str "read-wear-count " FACE) :absent #(fa/read-wear-count arsenal-rt FACE))
    (probe "list-faces (roster)" :info #(mapv (fn [f] (or (:face/name f) f)) (fa/list-faces arsenal-rt)))

    (println "\n[5] RELATIONS — relation kernel (via the 'trail' bundle)")
    (if REL-TARGET
      (probe (str "read-relations-for-targets " REL-TARGET) :present
             #(rk/read-relations-for-targets rk-rt [REL-TARGET]))
      (probe "read-relations-for-targets ADDRESS" :info
             #(rk/read-relations-for-targets rk-rt [ADDRESS])))

    (println "\n[6] TRANSCRIPT OFFSETS — transcript-ops module")
    (if FILE-KEY
      (probe (str "read-transcript-file-offset " FILE-KEY) :present
             #(ocr/read-transcript-file-offset oc-rt FILE-KEY))
      (probe "read-transcript-file-offset (default conv file)" :info
             #(when-some [f (cluster/default-transcript-file)]
                (ocr/read-transcript-file-offset oc-rt (.getName ^java.io.File f)))))

    (println (apply str "\n" (repeat 78 \-)))
    (let [rows @!rows
          by   (frequencies (map :verdict rows))
          bad  (filter #(#{:FAIL :ERROR} (:verdict %)) rows)]
      (println (format "R5: %d PASS · %d FAIL · %d ERROR · %d INFO"
                       (get by :PASS 0) (get by :FAIL 0)
                       (get by :ERROR 0) (get by :INFO 0)))
      (when (seq bad)
        (println "\nnot-green:")
        (doseq [b bad] (println "  -" (name (:verdict b)) (:label b)
                                (or (:err b) (str "expected " (name (:expect b)))))))
      (shutdown-agents)
      (System/exit (if (seq bad) 1 0)))))
