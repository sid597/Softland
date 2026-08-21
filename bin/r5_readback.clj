;; R5 — restart readback receipt.  READ-ONLY: only read-* fns, no depot append,
;; no ingest, no migration.  Proves every written kind survives a cluster bounce.
;;
;;   bin/land up                    # then wait for RUNNING x5
;;   clj -M:dev -e '(load-file "bin/r5_readback.clj")'
;;
;; Run it once, bounce the cluster, run it again: the first run writes a
;; baseline, the second compares against it.  That comparison IS R5.
;;
;; Key derivation is delegated to the app's own accessors (episode/read-*),
;; never re-implemented here.  v1 hand-rolled it and read the projection with
;; the bare address instead of `oc:chat-conversation:<address>`, producing four
;; false FAILs.  Same class of error as the map's own R5 spec, which expected
;; wear `nil` for `:outline-face` — the PState is keyed by STRING, so the
;; keyword read missed and the miss was recorded as the baseline.
;;
;; Overrides: R5_ADDRESS  R5_TURN  R5_FACE  R5_RELATION_TARGET  R5_BASELINE

(require '[app.server.rama.cluster :as cluster]
         '[app.server.rama.object-container.runtime :as ocr]
         '[app.server.rama.relation-kernel :as rk]
         '[app.server.rama.face-arsenal :as fa]
         '[app.server.episode :as episode]
         '[app.server.rama.object-container.transcript-identity :as tid]
         '[app.server.rama.dogfood.transcript :as transcript]
         '[clojure.string :as str]
         '[clojure.java.io :as io]
         '[clojure.edn :as edn])

(def ADDRESS (or (System/getenv "R5_ADDRESS")
                 "chat:77088a4f028100d3e93a99d291e2a184c7ed6ca66b3a998a74f9fbddfe62b34b"))
(def TURN-ID (or (System/getenv "R5_TURN") "waist-close-turn"))
(def FACE    (or (System/getenv "R5_FACE") "outline-face"))
(def REL-TARGET (System/getenv "R5_RELATION_TARGET"))
(def BASELINE (io/file (or (System/getenv "R5_BASELINE") "/mnt/data/rama/r5-baseline.edn")))

(def !rows (atom []))
(def !facts (atom (sorted-map)))

(defn- preview [x]
  (let [s (pr-str x)] (if (> (count s) 200) (str (subs s 0 200) " …") s)))

(defn probe
  "expect :present | :absent | :info . fact-key (optional) records a stable
   scalar into the baseline."
  ([label expect f] (probe label expect f nil nil))
  ([label expect f fact-key fact-fn]
   (let [r (try {:got (f)}
                (catch Throwable t {:err (str (.getSimpleName (class t)) ": " (.getMessage t))}))
         got (:got r)
         empty? (or (nil? got) (and (coll? got) (empty? got)))
         verdict (cond (:err r)           :ERROR
                       (= expect :info)   :INFO
                       (= expect :absent) (if empty? :PASS :FAIL)
                       :else              (if empty? :FAIL :PASS))]
     (swap! !rows conj (assoc r :label label :expect expect :verdict verdict))
     (when (and fact-key (not (:err r)))
       (swap! !facts assoc fact-key (try (fact-fn got) (catch Throwable _ ::unreadable))))
     (println (format "  %-6s %-44s %s" (name verdict) label
                      (if (:err r) (:err r) (preview got))))
     got)))

(println "\nR5 — restart readback   READ-ONLY")
(println "address:      " ADDRESS)
(println "conversation: " (tid/chat-conversation-id ADDRESS))
(println "baseline:     " (str BASELINE) (if (.exists BASELINE) "(exists → COMPARE)" "(absent → WRITE)"))
(println (apply str (repeat 80 \-)))

(if-not (some? (cluster/manager))
  (do (println "\nCLUSTER DOWN — no manager. Run `bin/land up`, wait for RUNNING x5,")
      (println "then re-run.  (cd /mnt/data/rama && ./rama moduleStatus <module-var>)")
      (System/exit 2))
  (let [ctx        (cluster/face-projection-runtime)
        oc-rt      (:oc-rt ctx)
        arsenal-rt (:arsenal-rt ctx)
        ;; :rk-rt IS cluster/trail-runtime, whose docstring says
        ;; ":module-name = the RELATION kernel name". Trail bundle, relation reads.
        rk-rt      (:rk-rt ctx)]

    (println "\n[1] EPISODE — turns · geometry · camera   (keys via episode/read-*)")
    (let [turns (probe "read-turn-records" :present
                       #(episode/read-turn-records oc-rt ADDRESS)
                       :turn-count count)
          geo   (probe "read-geometry-cells" :present
                       #(episode/read-geometry-cells oc-rt ADDRESS)
                       :geometry-cell-count #(count (:cells %)))]
      (probe (str "turn " TURN-ID " present") :present
             #(first (filter (fn [t] (str/includes? (pr-str t) TURN-ID)) turns)))
      (probe "camera cell" :present #(:camera geo) :camera pr-str)

      (println "\n[2] UNITS + TEXT — chained from ids the reads themselves returned")
      (let [uid (ffirst (:cells geo))]
        (println (format "  INFO   %-44s %s" "unit id from geometry" (preview uid)))
        (let [res  (when uid (probe "read-unit" :present #(ocr/read-unit oc-rt uid)
                                    :unit-id (constantly uid)))
              grad (:graduation res)
              cid  (:container-id grad)]
          (probe "unit content-text (the TEXT kind)" :present #(:content-text res)
                 :content-hash (fn [_] (:content-hash res)))
          (println (format "  INFO   %-44s %s" "container id (from :graduation)" (preview cid)))
          (if cid
            (do (probe "read-container" :present #(ocr/read-container oc-rt cid))
                (probe "read-current-revision" :present
                       #(ocr/read-current-revision oc-rt cid)
                       :current-revision-id (constantly (:current-revision-id grad))))
            (probe "read-container (unit not graduated → no container)" :info
                   (constantly ::ungraduated))))))

    (println "\n[3] WEAR — face-arsenal")
    (probe (str "read-wear-count " FACE) :present
           #(fa/read-wear-count arsenal-rt FACE)
           :wear-count #(:wear-count %))
    (probe "list-faces (roster size)" :present
           #(count (fa/list-faces arsenal-rt)) :face-roster-size identity)

    (println "\n[4] RELATIONS — relation kernel (via the 'trail' bundle)")
    (let [uid   (str (:unit-id @!facts))
          cands (if REL-TARGET [REL-TARGET]
                  (->> [ADDRESS (cluster/default-address) uid
                        (second (re-find #"^du:(chat:[0-9a-f]+):" uid))]
                       (filter some?) (remove str/blank?) distinct vec))]
      (println (format "  INFO   %-44s %d" "candidate targets swept" (count cands)))
      (probe "read-relations-for-targets (non-empty only)"
             (if REL-TARGET :present :info)
             #(->> (rk/read-relations-for-targets rk-rt cands)
                   (remove (fn [[_ v]] (empty? v))) (into {}))
             :relation-count #(reduce + 0 (map count (vals %)))))

    (println "\n[5] TRANSCRIPT OFFSETS — transcript-ops module")
    (probe "read-transcript-file-offset" :info
           #(when-some [f (cluster/default-transcript-file)]
              (let [fk (transcript/source-file-key :claude-code (transcript/file-id f))]
                (println (format "  INFO   %-44s %s" "file key" fk))
                (ocr/read-transcript-file-offset oc-rt fk)))
           :transcript-offset #(pr-str %))

    ;; ── baseline: write on first run, compare on every run after ──────────
    (println (apply str "\n" (repeat 80 \-)))
    (let [facts @!facts
          drift (when (.exists BASELINE)
                  (let [old (edn/read-string (slurp BASELINE))]
                    (vec (for [[k v] facts
                               :let [o (get old k ::missing)]
                               :when (not= o v)]
                           [k o v]))))]
      (if (.exists BASELINE)
        (do (println "BASELINE COMPARE — every value must be identical across the bounce")
            (if (empty? drift)
              (println (format "  PASS   no drift across %d recorded facts" (count facts)))
              (doseq [[k o v] drift]
                (println (format "  DRIFT  %-24s was %s   now %s" (name k) (pr-str o) (pr-str v))))))
        (do (io/make-parents BASELINE)
            (spit BASELINE (pr-str facts))
            (println "BASELINE WRITTEN →" (str BASELINE))
            (println "  now: bin/land down && bin/land up && re-run this script.")))
      (println "\nfacts:" (pr-str facts))

      (let [rows @!rows
            by (frequencies (map :verdict rows))
            bad (concat (filter #(#{:FAIL :ERROR} (:verdict %)) rows)
                        (map (fn [[k o v]] {:verdict :DRIFT :label (name k)
                                            :err (str "was " (pr-str o) ", now " (pr-str v))})
                             drift))]
        (println (format "\nR5: %d PASS · %d FAIL · %d ERROR · %d INFO · %d DRIFT"
                         (get by :PASS 0) (get by :FAIL 0) (get by :ERROR 0)
                         (get by :INFO 0) (count drift)))
        (when (seq bad)
          (println "\nnot-green:")
          (doseq [b bad]
            (println "  -" (name (:verdict b)) (:label b)
                     (or (:err b) (str "expected " (name (:expect b)))))))
        (shutdown-agents)
        (System/exit (if (seq bad) 1 0))))))
