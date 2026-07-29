(ns app.material-circulation-test
  "Editable-material P4 Gate-2 executable checks. No live model is called: the
  llm-module runs its established canned stream-json adapter seam."
  (:require [app.server.rama.material-circulation :as circulation]
            [app.server.rama.relation-kernel :as rk]
            [app.server.rama.object-container.facet-master :as facet-master]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.dogfood.llm :as llm]
            [app.shared.attention-material :as attention]
            [app.shared.provenance-material :as material]
            [clojure.data.json :as json]
            [clojure.test :refer [deftest is testing]]))

(deftest p4-reviewed-kind-growth-and-binding-wish-ruling
  (testing "the two P4 semantic predicates are registered"
    (is (contains? rk/relation-kinds :instance-of))
    (is (contains? rk/relation-kinds :felt-at)))
  (testing "wish coupling remains the pre-existing :references edge"
    (is (= :references circulation/gold-edge-kind))
    (is (not-any? #(= "wish" (name %)) rk/relation-kinds))))

(deftest space-material-joins-the-escape-detector
  (is (contains? circulation/default-material-policy-paths
                 "src/app/shared/space_material.cljc")
      "G8 stops at membership; the existing detector needs no parallel harness"))

(deftest receipt-is-mechanical-co-presence-only
  (let [receipt
        (circulation/receipt-from-context
         {:created-during {:conversation/address "chat:receipt"
                           :birth/id "b1"}
          :captured-at-ms 42
          :position {:x 10 :y 20}
          :scene-context
          {:receipt/picked-at
           {:address "du:chat:receipt:target"
            :src-path [:turns 1 :blocks 0]
            :view-instance [:vi :ground "target"]}
           :receipt/placement {:point-world [10 20]}
           :receipt/worn-materials
           [{:material/subject "du:chat:receipt:target"
             :material/attachment
             [:derived :provenance "du:chat:receipt:target"]
             :material/master "fm:provenance"
             :material/revision "rev:fm:provenance:r1"
             :material/site :machine-rail
             :material/role :edge-tint
             :material/slot :block/decorations}]
           :visible-count 7}})]
    (is (= "du:chat:receipt:target"
           (circulation/receipt-target receipt)))
    (is (= [:turns 1 :blocks 0]
           (get-in receipt [:receipt/picked-at :src-path])))
    (is (= {:x 10.0 :y 20.0}
           (select-keys (:receipt/placement receipt) [:x :y])))
    (is (= 1 (count (:receipt/worn-materials receipt))))
    (is (= [:derived :provenance "du:chat:receipt:target"]
           (get-in receipt
                   [:receipt/worn-materials 0 :material/attachment])))
    (is (= :edge-tint
           (get-in receipt [:receipt/worn-materials 0 :material/role])))
    (is (= :block/decorations
           (get-in receipt [:receipt/worn-materials 0 :material/slot])))
    (is (not (contains? receipt :relation-kind)))
    (is (not (contains? receipt :about)))))

(deftest gold-bank-and-starter-culture-materialize-and-dedup
  (let [rt (rk/start-relation-runtime! {:tasks 4 :threads 2})]
    (try
      (let [receipt
            (circulation/receipt-from-context
             {:created-during {:conversation/address "chat:gate"}
              :captured-at-ms 100
              :position {:x 1 :y 2}
              :scene-context
              {:receipt/picked-at
               {:address "du:chat:gate:target"
                :src-path [:turns 0 :blocks 0]}}})
            gold-args {:source-unit-id "du:chat:gate:wish"
                       :target-unit-id "du:chat:gate:target"
                       :receipt receipt
                       :asserted-at-ms 100}
            g1 (circulation/bank-gold! rt gold-args)
            g2 (circulation/bank-gold! rt gold-args)
            rows (get (rk/read-relations-for-targets
                       rt ["du:chat:gate:target"] [:references] false)
                      "du:chat:gate:target")]
        (is (= :materialized (:status g1)))
        (is (= (:relation-id g1) (:relation-id g2)))
        (is (= 1 (count rows)) "retry converges, never duplicate gold")
        (is (circulation/gold-edge? (first rows)))
        (is (= :references (:relation-kind (first rows)))))
      (let [blocks [{:unit-id "du:chat:gate:old-task"
                     :text "#TASK preserve the old record"
                     :source-id "src:tr:chat:gate:task"}
                    {:unit-id "du:chat:gate:old-feedback"
                     :text "A note with #Feedback inside"
                     :source-id "src:tr:chat:gate:feedback"}
                    {:unit-id "du:chat:gate:plain"
                     :text "ordinary untyped material"}]
            s1 (circulation/seed-starter-culture! rt blocks 200)
            s2 (circulation/seed-starter-culture! rt blocks 200)
            task-rows
            (get (rk/read-relations-for-targets
                  rt ["du:chat:gate:old-task"] [:instance-of] false)
                 "du:chat:gate:old-task")]
        (is (= :completed (:status s1)))
        (is (= 2 (:materialized s1)))
        (is (= 2 (:materialized s2)))
        (is (= 1 (count task-rows)))
        (is (circulation/silver-edge? (first task-rows)))
        (is (= "kind/task" (get-in (first task-rows) [:to :target-id])))
        (is (every? #(not (contains? % :receipt)) (:writes s1))
            "retroactive association never invents a historical receipt"))
      (finally
        (rk/close-relation-runtime! rt)))))

(deftest halo-reference-bank-is-idempotent-and-custody-honest
  (let [rt (rk/start-relation-runtime! {:tasks 4 :threads 2})
        base {:source-unit-id "du:halo:say"
              :target-unit-id "du:halo:target"
              :actor {:actor/id "sid" :actor/type :human}
              :asserted-at-ms 301
              :say-id "say-301"
              :master-id "fm:attention"}]
    (try
      (let [a (circulation/bank-reference! rt base)
            replay (circulation/bank-reference! rt base)
            row (first
                 (get
                  (rk/read-relations-for-targets
                   rt [(:target-unit-id base)] [:references] false)
                  (:target-unit-id base)))]
        (is (= :materialized (:status a)))
        (is (= (:relation-id a) (:relation-id replay)))
        (is (= 1
               (count
                (get
                 (rk/read-relations-for-targets
                  rt [(:target-unit-id base)] [:references] false)
                 (:target-unit-id base)))))
        (is (circulation/gold-edge? row))
        (is (= {:mark/type :halo/say
                :mark/say-id "say-301"
                :mark/master-id "fm:attention"}
               (read-string (:note row)))))
      (let [machine
            (circulation/bank-reference!
             rt
             (assoc base
                    :source-unit-id "du:halo:machine-say"
                    :actor {:actor/id "assistant:1" :actor/type :agent}
                    :say-id "say-machine"))
            row (get-in
                 (rk/read-relation-detail rt (:relation-id machine))
                 [:row])]
        (is (= :materialized (:status machine)))
        (is (not (circulation/gold-edge? row))
            "nonhuman custody remains asserted but never painted gold"))
      (finally
        (rk/close-relation-runtime! rt)))))

(deftest halo-say-gold-mark-keeps-generic-source-and-never-wish-labels
  (let [target "du:halo:target"
        source "du:halo:say"
        row {:relation-id "rel-halo"
             :relation-kind :references
             :from (rk/->target-ref :derived-unit source)
             :to (rk/->target-ref :derived-unit target)
             :relation-status :asserted
             :asserter-actor-id "sid"
             :asserter-type :human
             :first-asserted-at-ms 9
             :note (pr-str {:mark/type :halo/say
                            :mark/say-id "say-9"
                            :mark/master-id "fm:attention"})}
        mark (get-in
              (circulation/compose-experience
               [target] {target [row]} [])
              [:experience/gold-marks-by-target target 0])]
    (is (= source (:source-unit-id mark)))
    (is (= :halo/say (:mark/type mark)))
    (is (not (contains? mark :wish-unit-id)))))

(deftest standing-query-exposes-receipt-silver-gold-composition
  (let [target "du:chat:q:target"
        wish "du:chat:q:wish"
        silver "du:chat:q:legacy"
        mk-row
        (fn [rid kind from to actor-id actor-type]
          {:relation-id rid
           :relation-kind kind
           :from (rk/->target-ref :derived-unit from)
           :to (rk/->target-ref :derived-unit to)
           :relation-status :asserted
           :asserter-actor-id actor-id
           :asserter-type actor-type})
        gold (mk-row "rel-gold" :references wish target "sid" :human)
        felt (mk-row "rel-silver" :felt-at silver target
                     circulation/autotag-actor-id :llm)
        relation-map {target [gold felt]}
        receipt {:receipt/version 1
                 :receipt/picked-at {:address target}}
        result (circulation/compose-experience
                [target] relation-map
                [{:origin-unit-id wish :receipt receipt}])]
    (is (= {:receipt 1 :silver 1 :gold 1}
           (select-keys (:experience/composition result)
                        [:receipt :silver :gold])))
    (is (= wish
           (get-in result
                   [:experience/gold-marks-by-target target 0 :wish-unit-id])))
    (is (= #{:receipt :gold}
           (:experience/strata
            (some #(when (= wish (:experience/origin-unit-id %)) %)
                  (:experience/items result)))))
    (is (some #{target}
              (:experience/reverse-links
               (some #(when (= wish (:experience/origin-unit-id %)) %)
                     (:experience/items result)))))))

(deftest receipt-worn-context-resolves-from-activation-history-as-of
  (let [oc-rt (ocr/start-object-container-runtime!)]
    (try
      (let [_ (facet-master/ensure-master! oc-rt material/spec)
            p3-migration
            (facet-master/ensure-active-source!
             oc-rt
             material/spec
             material/composition-source
             {:request-id "facet-master-provenance-v1"
              :activation-request-id
              "facet-master-provenance-activate-v1"
              :time-ms 1})
            original-id
            (get-in p3-migration [:state :active-revision :revision-id])
            attention-boot
            (facet-master/ensure-master! oc-rt attention/spec)
            attention-id
            (get-in attention-boot [:state :active-revision :revision-id])
            candidate
            (facet-master/import-candidate!
             oc-rt
             material/spec
             (pr-str (assoc material/default-form
                            :provenance/tint [0.11 0.22 0.33 0.44]))
             {:request/id "p4-as-of-candidate" :time-ms 10})
            candidate-id (:revision-id candidate)
            activation
            (facet-master/activate!
             oc-rt material/spec candidate-id
             {:request/id "p4-as-of-activation" :time-ms 20})
            receipt-at
            (fn [time-ms revision-id]
              {:receipt/version 1
               :receipt/captured-at-ms time-ms
               :receipt/worn-materials
               [{:material/subject "du:as-of"
                 :material/attachment
                 [:derived :provenance "du:as-of"]
                 :material/master material/master-id
                 :material/revision revision-id
                 :material/site :machine-rail
                 :material/role :edge-tint
                 :material/slot :block/decorations}
                {:material/subject "du:as-of"
                 :material/attachment
                 [:derived :attention "du:as-of"]
                 :material/master attention/master-id
                 :material/revision attention-id
                 :material/site :attention-box
                 :material/role :attention-border
                 :material/slot :block/decorations}]})
            resolved
            (circulation/resolve-records-as-of
             oc-rt
             [{:origin-unit-id "du:before"
               :receipt (receipt-at 15 original-id)}
              {:origin-unit-id "du:after"
               :receipt (receipt-at 25 candidate-id)}])
            before (get-in resolved [0 :receipt :receipt/worn-as-of 0])
            before-attention
            (get-in resolved [0 :receipt :receipt/worn-as-of 1])
            after (get-in resolved [1 :receipt :receipt/worn-as-of 0])]
        (is (:accepted? candidate))
        (is (:accepted? activation))
        (is (= original-id (:material/active-revision-as-of before)))
        (is (:material/as-of-matches-capture? before))
        (is (= attention-id
               (:material/active-revision-as-of before-attention)))
        (is (:material/as-of-matches-capture? before-attention))
        (is (= [:derived :attention "du:as-of"]
               (:material/attachment before-attention)))
        (is (= candidate-id (:material/active-revision-as-of after)))
        (is (:material/as-of-matches-capture? after))
        (is (not= (:material/active-revision-as-of before)
                  (:material/active-revision-as-of after)))

        (let [regressed-candidate
              (facet-master/import-candidate!
               oc-rt
               material/spec
               (pr-str (assoc material/default-form
                              :provenance/tint [0.66 0.55 0.44 0.33]))
               {:request/id "p4-as-of-regressed-candidate" :time-ms 30})
              regressed-id (:revision-id regressed-candidate)
              regressed-activation
              (facet-master/activate!
               oc-rt material/spec regressed-id
               {:request/id "p4-as-of-regressed-activation" :time-ms 1})
              present
              (get-in
               (circulation/resolve-records-as-of
                oc-rt
                [{:origin-unit-id "du:present"
                  :receipt (receipt-at 40 regressed-id)}])
               [0 :receipt :receipt/worn-as-of 0])]
          (is (:accepted? regressed-activation))
          (is (= regressed-id (:material/active-revision-as-of present))
              "causal parent order wins over a regressed producer clock")
          (is (:material/as-of-matches-capture? present))
          (is (seq (:material/activation-clock-regressions present)))))
      (finally
        (ocr/close-object-container-runtime! oc-rt)))))

(deftest terminal-escape-detector-counts-policy-commits-after-activation
  (let [commits [{:sha "old" :committed-at-ms 100
                  :subject "old policy"
                  :files ["src/app/shared/provenance_material.cljc"]}
                 {:sha "other" :committed-at-ms 300
                  :subject "unrelated"
                  :files ["src/app/client/workspace/ground.cljs"]}
                 {:sha "escape" :committed-at-ms 400
                  :subject "changed material policy without activation"
                  :files ["src/app/shared/provenance_material.cljc"]}]
        activations [{:created-at-ms 200
                      :content-text "rev:fm:provenance:r1"}]
        report (circulation/terminal-escape-report commits activations)]
    (is (= 1 (:terminal-escape/count report)))
    (is (= ["escape"]
           (mapv :sha (:terminal-escape/escapes report))))
    (is (= 2 (:terminal-escape/policy-commits-scanned report)))
    (is (= 3 (:terminal-escape/commits-scanned report)))
    (is (= 1 (:terminal-escape/activation-events-scanned report)))))

(defn- canned-lines
  [output]
  ["{\"type\":\"system\",\"session_id\":\"p4-autotag\"}"
   (json/write-str
    {:type "result"
     :is_error false
     :session_id "p4-autotag"
     :total_cost_usd 0.0
     :result (if (string? output) output (json/write-str output))})])

(deftest ambient-autotag-keeps-stages-separate-and-condenses-failure
  (let [llm-rt (llm/start-llm-runtime!)
        oc-rt (ocr/start-object-container-runtime!)
        rk-rt (rk/start-relation-runtime! {:tasks 4 :threads 2})
        ctx {:llm-rt llm-rt :oc-rt oc-rt :rk-rt rk-rt}
        ok-args {:record-unit-id "du:chat:autotag:record"
                 :record-text "the spacing here makes the thread hard to read"
                 :candidates
                 [{:id "du:chat:autotag:target"
                   :text "thread placement policy"}]
                 :lines (canned-lines
                         {:target "du:chat:autotag:target"
                          :interpretation "concerns thread placement"
                          :confidence 0.91})}
        bad-args {:record-unit-id "du:chat:autotag:bad"
                  :record-text "ambiguous pressure"
                  :candidates
                  [{:id "du:chat:autotag:target"
                    :text "thread placement policy"}]
                  :lines (canned-lines "not json")}]
    (try
      (let [first-run
            (circulation/autotag-material! ctx "chat:autotag" ok-args)
            retry
            (circulation/autotag-material! ctx "chat:autotag" ok-args)
            edge (:edge first-run)]
        (is (= :completed (:status first-run)))
        (is (= :observation
               (get-in first-run [:record :observation :stage])))
        (is (= :interpretation
               (get-in first-run [:record :interpretation :stage])))
        (is (= :proposal
               (get-in first-run [:record :proposal :stage])))
        (is (= :materialized (:status edge)))
        (is (= :felt-at
               (:relation-kind
                (:row (rk/read-relation-detail rk-rt (:relation-id edge))))))
        (is (= :already-recorded (:status retry)))
        (is (false? (:adapter-called? retry))))
      (let [failed
            (circulation/autotag-material! ctx "chat:autotag" bad-args)
            retry
            (circulation/autotag-material! ctx "chat:autotag" bad-args)
            records
            (filter #(= (:run-id failed) (:run/id %))
                    (circulation/read-circulation-records oc-rt "chat:autotag"))]
        (is (= :failed (:status failed)))
        (is (= :condensed-failure (:status retry)))
        (is (false? (:adapter-called? retry)))
        (is (= 1 (count records))
            "identical machine failure is one durable record"))
      (finally
        (rk/close-relation-runtime! rk-rt)
        (ocr/close-object-container-runtime! oc-rt)
        (llm/close-llm-runtime! llm-rt)))))
