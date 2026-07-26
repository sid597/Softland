(ns app.space-as-entity-receipt
  "Live, server-read receipts for space-as-entity.

   This is intentionally not a *_test namespace: the ordinary suite remains
   hermetic, while the gate command attaches to the real durable cluster."
  (:require [app.server.rama.cluster :as cluster]
            [app.server.rama.face-projection :as face-projection]
            [app.server.rama.object-container.facet-master :as facet-master]
            [app.server.rama.object-container.runtime :as ocr]
            [app.shared.facet-material :as facet-material]
            [app.shared.facet-masters :as facet-masters]))

(def ^:private block-sites
  #{:block/user-hit-area
    :block/machine-hit-area
    :block/fold-header})

(def ^:private history-limit
  10000)

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

(defn -main
  [& [gate]]
  (try
    (case gate
      "g2"
      (let [receipt (g2-live-receipt)]
        (println (pr-str receipt))
        (System/exit (if (= :green (:status receipt)) 0 2)))

      (do
        (println "usage: clj -M:test -m app.space-as-entity-receipt g2")
        (System/exit 64)))
    (catch Throwable t
      (binding [*out* *err*]
        (println (pr-str {:gate :G2
                          :status :error
                          :message (.getMessage t)})))
      (System/exit 1))))
