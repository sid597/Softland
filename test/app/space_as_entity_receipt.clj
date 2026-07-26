(ns app.space-as-entity-receipt
  "Live, server-read receipts for space-as-entity.

   This is intentionally not a *_test namespace: the ordinary suite remains
   hermetic, while the gate command attaches to the real durable cluster."
  (:require [app.server.rama.cluster :as cluster]
            [app.server.rama.face-projection :as face-projection]
            [app.server.rama.object-container.facet-master :as facet-master]
            [app.server.rama.object-container.runtime :as ocr]
            [app.shared.binding-material :as binding-material]
            [app.shared.facet-material :as facet-material]
            [app.shared.facet-masters :as facet-masters]
            [app.shared.space-material :as space]))

(def ^:private block-sites
  #{:block/user-hit-area
    :block/machine-hit-area
    :block/fold-header})

(def ^:private history-limit
  10000)

(def ^:private g6-base-source
  ;; First contact must mint the active-pointer container in the SAME accepted
  ;; import. The newline gives this bootstrap payload its own immutable revision
  ;; after the pre-pointer diagnostic imports retained above.
  (str space/default-source "\n"))

(def ^:private g6-clamped-form
  (assoc space/default-form :space/zoom-max 2.0))

(def ^:private g6-malformed-form
  (assoc space/default-form
         :space/zoom-min 5.0
         :space/zoom-max 4.0))

(defn- wheel-at-block?
  [row]
  (and (= :wheel (:table/gesture row))
       (contains? block-sites (:table/site row))))

(defn- revision-binding-rows
  [spec active-id revision]
  (let [compiled (facet-material/compile-source spec (:content-text revision))
        bindings (get-in compiled [:material :facet-master/bindings])]
    (for [[site rows] bindings
          row rows]
      {:table/gesture (:binding/gesture row)
       :table/phase (:binding/phase row)
       :table/modifiers (:binding/modifiers row)
       :table/site site
       :table/master-id (:facet-master/id spec)
       :table/revision-id (:revision-id revision)
       :table/active? (= active-id (:revision-id revision))
       :table/verb (get-in row [:binding/verb :verb/name])})))

(defn g2-live-receipt
  "Enumerate the active served interaction table and every durable candidate
   revision for registered facet masters directly from the live cluster.

   Active block-site wheel rows fail G2. Non-active rows are returned as
   flagged residue. A history page at the hard limit fails closed because the
   receipt could not prove that it enumerated all revisions."
  []
  (let [runtime (or (cluster/object-container-runtime)
                    (throw (ex-info "live cluster unavailable — run bin/land up"
                                    {})))
        served (face-projection/interaction-table-projection
                {:oc-rt runtime} {})
        active-served-wheel-rows
        (->> (:interaction-table/rows served)
             (filter #(and (= :master (:table/tier %))
                           (wheel-at-block? %)))
             vec)
        histories
        (mapv
         (fn [spec]
           (let [state (facet-master/read-master runtime spec)
                 active-id (get-in state [:active-revision :revision-id])
                 revisions (ocr/read-revision-history
                            runtime (facet-master/document-id spec)
                            "" history-limit)]
             {:master-id (:facet-master/id spec)
              :active-revision-id active-id
              :history-count (count revisions)
              :history-complete? (< (count revisions) history-limit)
              :rows (into []
                          (mapcat #(revision-binding-rows spec active-id %))
                          revisions)}))
         facet-masters/specs)
        history-rows (into [] (mapcat :rows) histories)
        active-history-wheel-rows
        (->> history-rows
             (filter #(and (:table/active? %) (wheel-at-block? %)))
             vec)
        flagged-residue
        (->> history-rows
             (filter #(and (not (:table/active? %)) (wheel-at-block? %)))
             vec)
        history-complete? (every? :history-complete? histories)
        pass? (and history-complete?
                   (empty? active-served-wheel-rows)
                   (empty? active-history-wheel-rows))]
    {:gate :G2
     :status (if pass? :green :red)
     :source :live-cluster-server-read
     :masters-scanned (count histories)
     :revisions-scanned (reduce + (map :history-count histories))
     :binding-rows-scanned (count history-rows)
     :history-complete? history-complete?
     :active-served-wheel-rows active-served-wheel-rows
     :active-history-wheel-rows active-history-wheel-rows
     :flagged-non-active-residue flagged-residue}))

(defn- floor-rows
  []
  (assoc
   (into {}
         (map (fn [spec]
                [(:facet-master/facet spec)
                 (:facet-master/bindings (facet-material/code-floor spec))]))
         facet-masters/specs)
   binding-material/space-facet binding-material/space-floor-bindings))

(defn- projected-world
  [runtime]
  (let [served (face-projection/facet-materials-projection
                {:oc-rt runtime} {:params {:drill? true}})
        by-id (:facet-materials/by-id served)
        wears
        (into {}
              (map (fn [spec]
                     [(:facet-master/facet spec)
                      (facet-material/resolved-wear
                       spec (get by-id (:facet-master/id spec)))]))
              facet-masters/specs)
        master-rows
        (into {}
              (map (fn [[facet wear]]
                     [facet (when-not (:facet-master/floor? wear)
                              (:facet-master/bindings wear))]))
              wears)
        report (binding-material/drill-report
                {:facet-rows master-rows
                 :floor-rows (floor-rows)
                 :instance-rows {}})]
    {:served served
     :space-entry (get by-id space/master-id)
     :space-wear (:space wears)
     :drill report
     :tiers-by-label
     (into {} (map (juxt :probe/label :probe/tier)) report)}))

(defn g6-start-live-receipt
  "Stage G6 on durable truth and leave zoom-max=2.0 active so the browser can
   drive the real wheel before `g6-finish` repoints to the 8.0 revision."
  []
  (let [runtime (or (cluster/object-container-runtime)
                    (throw (ex-info "live cluster unavailable — run bin/land up"
                                    {})))
        before (projected-world runtime)
        base (facet-master/import-candidate!
              runtime space/spec g6-base-source
              {:request/id "space-p2-g6-base-pointer-v2"
               :include-active-pointer? true})
        clamped (facet-master/import-candidate!
                 runtime space/spec (pr-str g6-clamped-form)
                 {:request/id "space-p2-g6-clamped-v1"})
        activation (facet-master/activate!
                    runtime space/spec (:revision-id clamped)
                    {:request/id "space-p2-g6-activate-clamped-v2"
                     :activation/kind :activate
                     :activation/grounds
                     [{:ground/type :gate
                       :ground/ref "space-as-entity/G6-clamp-2.0"}]})
        after (projected-world runtime)
        tiers (:tiers-by-label after)
        checks
        {:base-import-accepted? (:accepted? base)
         :clamped-import-accepted? (:accepted? clamped)
         :activation-accepted? (:accepted? activation)
         :clamped-revision-active?
         (= (:revision-id clamped)
            (get-in after [:space-entry :facet-master/active-revision-id]))
         :zoom-max-2? (= 2.0 (get-in after [:space-wear :space/zoom-max]))
         :tap-master? (= :master
                         (get tiers "tap empty space → caret anchor"))
         :marquee-master?
         (= :master (get tiers "shift-drag empty space → marquee"))
         :pan-floor? (= :floor
                        (get tiers "drag empty space → pan the camera"))
         :wheel-floor? (= :floor
                          (get tiers "wheel → zoom at the pointer"))
         :twelve-probes? (= 12 (count (:drill after)))
         :all-probes-claimed?
         (every? #(= :claimed (:probe/outcome %)) (:drill after))}
        pass? (every? true? (vals checks))]
    {:gate :G6
     :stage :clamp-active-awaiting-browser-drive
     :status (if pass? :green :red)
     :source :live-cluster-server-read
     :before-floor?
     (true? (get-in before [:space-wear :facet-master/floor?]))
     :before-tier (get-in before [:space-wear :facet-master/tier])
     :base-revision-id (:revision-id base)
     :clamped-revision-id (:revision-id clamped)
     :active-revision-id
     (get-in after [:space-entry :facet-master/active-revision-id])
     :zoom-range
     ((juxt :space/zoom-min :space/zoom-max) (:space-wear after))
     :space-probe-tiers
     (select-keys tiers
                  ["tap empty space → caret anchor"
                   "shift-drag empty space → marquee"
                   "drag empty space → pan the camera"
                   "wheel → zoom at the pointer"])
     :checks checks}))

(defn g6-finish-live-receipt
  "Repoint to the durable 8.0 revision, retain a malformed candidate, prove its
   activation refusal, and leave the cluster in the rolled-back state."
  []
  (let [runtime (or (cluster/object-container-runtime)
                    (throw (ex-info "live cluster unavailable — run bin/land up"
                                    {})))
        base (facet-master/import-candidate!
              runtime space/spec g6-base-source
              {:request/id "space-p2-g6-base-pointer-v2"
               :include-active-pointer? true})
        rollback (facet-master/activate!
                  runtime space/spec (:revision-id base)
                  {:request/id "space-p2-g6-rollback-base-v1"
                   :activation/kind :rollback
                   :activation/grounds
                   [{:ground/type :gate
                     :ground/ref "space-as-entity/G6-rollback-8.0"}]})
        malformed (facet-master/import-candidate!
                   runtime space/spec (pr-str g6-malformed-form)
                   {:request/id "space-p2-g6-malformed-v1"})
        refusal (facet-master/activate!
                 runtime space/spec (:revision-id malformed)
                 {:request/id "space-p2-g6-refuse-malformed-v1"})
        after (projected-world runtime)
        entry (:space-entry after)
        tiers (:tiers-by-label after)
        malformed-material
        (select-keys
         g6-malformed-form
         (get-in space/spec
                 [:facet-master/grammars space/bindings-grammar-version
                  :material-keys]))
        malformed-wear
        (space/resolved-wear
         {:facet-master/id space/master-id
          :facet-master/facet :space
          :facet-master/grammar space/bindings-grammar-version
          :facet-master/active-revision-id (:revision-id malformed)
          :facet-master/material malformed-material})
        pass?
        (and (:accepted? rollback)
             (false? (:accepted? refusal))
             (= [:space/zoom-clamp-invalid]
                (mapv :type (:errors refusal)))
             (= (:revision-id base)
                (:facet-master/active-revision-id entry))
             (= (:revision-id malformed)
                (:facet-master/candidate-revision-id entry))
             (= [:space/zoom-clamp-invalid]
                (mapv :type (:facet-master/candidate-errors entry)))
             (= [0.1 8.0]
                ((juxt :space/zoom-min :space/zoom-max)
                 (:space-wear after)))
             (true? (:facet-master/floor? malformed-wear))
             (= [0.1 8.0]
                ((juxt :space/zoom-min :space/zoom-max) malformed-wear))
             (= :master (get tiers "tap empty space → caret anchor"))
             (= :master
                (get tiers "shift-drag empty space → marquee"))
             (= :floor (get tiers "drag empty space → pan the camera"))
             (= :floor (get tiers "wheel → zoom at the pointer")))]
    {:gate :G6
     :stage :rollback-and-malformed-refusal
     :status (if pass? :green :red)
     :source :live-cluster-server-read
     :rollback-revision-id (:revision-id base)
     :active-revision-id (:facet-master/active-revision-id entry)
     :zoom-range
     ((juxt :space/zoom-min :space/zoom-max) (:space-wear after))
     :malformed-revision-id (:revision-id malformed)
     :malformed-activation-accepted? (:accepted? refusal)
     :malformed-errors (:errors refusal)
     :candidate-errors (:facet-master/candidate-errors entry)
     :synthetic-malformed-wear
     (select-keys malformed-wear
                  [:facet-master/floor?
                   :facet-master/revision-id
                   :space/zoom-min :space/zoom-max])
     :space-probe-tiers
     (select-keys tiers
                  ["tap empty space → caret anchor"
                   "shift-drag empty space → marquee"
                   "drag empty space → pan the camera"
                   "wheel → zoom at the pointer"])}))

(defn -main
  [& [gate]]
  (try
    (case gate
      "g2"
      (let [receipt (g2-live-receipt)]
        (println (pr-str receipt))
        (System/exit (if (= :green (:status receipt)) 0 2)))

      "g6-start"
      (let [receipt (g6-start-live-receipt)]
        (println (pr-str receipt))
        (System/exit (if (= :green (:status receipt)) 0 2)))

      "g6-finish"
      (let [receipt (g6-finish-live-receipt)]
        (println (pr-str receipt))
        (System/exit (if (= :green (:status receipt)) 0 2)))

      (do
        (println
         "usage: clj -M:test -m app.space-as-entity-receipt g2|g6-start|g6-finish")
        (System/exit 64)))
    (catch Throwable t
      (binding [*out* *err*]
        (println (pr-str {:gate :G2
                          :status :error
                          :message (.getMessage t)})))
      (System/exit 1))))
