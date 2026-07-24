(ns app.material-inspector-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.server.rama.face-projection :as face-projection]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.markdown-adapter :as markdown]
            [app.server.rama.object-container.provenance-material :as adapter]
            [app.server.rama.object-container.runtime :as ocr]
            [app.shared.material-inspector :as inspector]
            [app.shared.provenance-material :as material]))

(deftest projected-wearers-and-canonical-bytes
  (let [rev "rev:active"
        stamp #(material/contribution-stamp
                {:facet-master/id material/master-id
                 :facet-master/revision-id rev}
                %)
        store
        {:slots
         {:one {:vi [:vi :ground-block "du:b"]
                :meta {:ground-block "du:b"}
                :tree {:data (stamp :machine-rail)
                       :text [[(stamp :fold-header)]]}}
          :two {:vi [:vi :ground-block "du:a"]
                :meta {:ground-block "du:a"}
                :tree {:children [{:data (stamp :episode-boundary)}]}}
          :duplicate {:vi [:vi :ground-block-copy "du:b"]
                      :meta {:ground-block "du:b"}
                      :tree {:children [{:data (stamp :episode-boundary)}]}}
          :plain {:vi [:vi :ground-block "du:plain"]
                  :meta {:ground-block "du:plain"}
                  :tree {:data {:address "du:plain"}}}}}
        wearers (inspector/wearers-from-scene-store store)]
    (testing "wearers come only from stamped ground-block appearances"
      (is (= [{:wearer/entity-id "du:a"
               :wearer/revision-ids [rev]
               :wearer/contribution-sites [:episode-boundary]
               :wearer/view-instances [[:vi :ground-block "du:a"]]}
              {:wearer/entity-id "du:b"
               :wearer/revision-ids [rev]
               :wearer/contribution-sites
               [:fold-header :machine-rail :episode-boundary]
               :wearer/view-instances
               [[:vi :ground-block "du:b"]
                [:vi :ground-block-copy "du:b"]]}]
             wearers)))
    (testing "hash/set insertion order cannot change inspector bytes"
      (is (= (inspector/canonical-edn
              {:z #{:b :a} :a {:y 2 :x 1}})
             (inspector/canonical-edn
              (array-map :a (array-map :x 1 :y 2)
                         :z #{:a :b})))))))

(deftest material-inspector-product-path
  (let [runtime (ocr/start-object-container-runtime!)]
    (try
      (let [block-request
            (markdown/source-ingest-request
             "Inspectable block"
             "material-inspector/block.md"
             {:request/id "p2-block"
              :time-ms 1})
            _ (ocr/append-object-container-request!
               runtime block-request :ack)
            block-id (some-> (ocr/read-outline
                              runtime
                              (oc/document-id-for-object-key
                               (:object/key block-request)))
                             first
                             :target-id)
            boot (adapter/ensure-master! runtime)
            original-id (get-in boot [:state :active-revision :revision-id])
            candidate
            (adapter/import-candidate!
             runtime
             (pr-str (assoc material/default-form
                            :provenance/tint [0.1 0.2 0.3 0.9]))
             {:request/id "p2-candidate" :time-ms 10})
            candidate-id (:revision-id candidate)
            activation
            (adapter/activate!
             runtime candidate-id
             {:request/id "p2-activate" :time-ms 20})
            malformed
            (adapter/import-candidate!
             runtime
             "{:facet-master/id"
             {:request/id "p2-malformed" :time-ms 30})
            malformed-id (:revision-id malformed)
            rejected (adapter/activate!
                      runtime malformed-id
                      {:request/id "p2-reject" :time-ms 31})
            rollback
            (adapter/activate!
             runtime original-id
             {:request/id "p2-rollback" :time-ms 40})
            wearers
            [{:wearer/entity-id "du:z"
              :wearer/revision-ids [original-id]
              :wearer/contribution-sites [:machine-rail]
              :wearer/view-instances [[:vi :ground-block "du:z"]]}
             {:wearer/entity-id block-id
              :wearer/revision-ids [original-id]
              :wearer/contribution-sites
              [:episode-boundary :fold-header :machine-rail]
              :wearer/view-instances [[:vi :ground-block block-id]]}]
            request
            {:face :material-inspector
             :params {:entity-id block-id
                      :wearers wearers}}
            state-before (adapter/read-master runtime)
            candidate-history-before
            (ocr/read-revision-history runtime adapter/document-id)
            pointer-history-before
            (ocr/read-revision-history
             runtime adapter/active-pointer-container-id)
            response-a
            (face-projection/serve
             {:oc-rt runtime}
             (assoc-in request [:params :request-token] "first"))
            response-b
            (face-projection/serve
             {:oc-rt runtime}
             (assoc-in request [:params :request-token] "second"))
            result (:material-inspector/result response-a)
            entity (:material-inspector/entity result)
            attachment
            (:material-inspector/provenance-attachment result)
            master (:material-inspector/facet-master result)
            trail (:material-inspector/revision-trail result)
            candidates (filter #(= :candidate (:trail/kind %)) trail)
            activations (filter #(= :activation (:trail/kind %)) trail)
            rollbacks (filter #(= :rollback (:trail/kind %)) trail)]
        (testing "fixture establishes active, latest, and rejected-candidate truth"
          (is (string? block-id))
          (is (:accepted? candidate))
          (is (:accepted? activation))
          (is (:accepted? malformed))
          (is (false? (:accepted? rejected)))
          (is (:accepted? rollback)))

        (testing "pick resolves to the durable block entity"
          (is (= block-id (:entity/id entity)))
          (is (true? (:entity/found? entity)))
          (is (= :markdown/paragraph (:entity/kind entity)))
          (is (= :derived-unit (:entity/target-kind entity)))
          (is (= block-id (:entity/target-id entity))))

        (testing "the attachment is honestly derived from current stamps"
          (is (= {:attachment/authority :derived
                  :attachment/basis :rendered-contribution-stamps
                  :attachment/contribution-sites
                  [:fold-header :machine-rail :episode-boundary]
                  :attachment/facet :provenance
                  :attachment/master-id material/master-id
                  :attachment/present? true
                  :attachment/wears-active? true}
                 attachment)))

        (testing "master reports active and latest independently"
          (is (= original-id (:facet-master/active-revision-id master)))
          (is (= malformed-id (:facet-master/latest-revision-id master)))
          (is (true? (:facet-master/active-latest-distinct? master)))
          (is (= material/master-id (:facet-master/id master)))
          (is (string? (:facet-master/pointer-revision-id master))))

        (testing "all three sites and every current wearer are one answer"
          (is (= inspector/contribution-sites
                 (:material-inspector/contribution-sites result)))
          (is (= (vec (sort [block-id "du:z"]))
                 (mapv :wearer/entity-id
                       (:material-inspector/current-wearers result)))))

        (testing "the complete trail distinguishes candidates, activations,
                  rollback, and their durable request times"
          (is (true?
               (:material-inspector/revision-trail-complete? result)))
          (is (= 3 (count candidates)))
          (is (= 2 (count activations)))
          (is (= 1 (count rollbacks)))
          (is (= #{0 10 30} (set (map :trail/time-ms candidates))))
          (is (= #{0 20} (set (map :trail/time-ms activations))))
          (is (= [40] (mapv :trail/time-ms rollbacks)))
          (is (= original-id (:trail/revision-id (first rollbacks))))
          (is (false?
               (:trail/valid?
                (some #(when (= malformed-id (:trail/revision-id %)) %)
                      candidates)))))

        (testing "same input and world returns byte-equal answer"
          (is (= (:material-inspector/result response-a)
                 (:material-inspector/result response-b)))
          (is (= (:material-inspector/edn response-a)
                 (:material-inspector/edn response-b)))
          (is (= (:material-inspector/edn response-a)
                 (inspector/canonical-edn result))))

        (testing "the inspector is read-only"
          (is (= state-before (adapter/read-master runtime)))
          (is (= candidate-history-before
                 (ocr/read-revision-history runtime adapter/document-id)))
          (is (= pointer-history-before
                 (ocr/read-revision-history
                  runtime adapter/active-pointer-container-id)))))
      (finally
        (ocr/close-object-container-runtime! runtime)))))
