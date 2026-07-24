(ns app.material-inspector-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.server.rama.face-projection :as face-projection]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.facet-master :as adapter]
            [app.server.rama.object-container.markdown-adapter :as markdown]
            [app.server.rama.object-container.runtime :as ocr]
            [app.shared.attention-material :as attention]
            [app.shared.facet-material :as facet-material]
            [app.shared.material-inspector :as inspector]
            [app.shared.provenance-material :as provenance]))

(defn- stamp
  [master-id facet revision subject site role slot]
  (facet-material/contribution-stamp
   {:facet-master/id master-id
    :facet-master/facet facet
    :facet-master/revision-id revision}
   subject site role slot))

(deftest projected-wearers-and-canonical-bytes
  (let [provenance-rev "rev:provenance"
        attention-rev "rev:attention"
        p #(stamp provenance/master-id :provenance provenance-rev
                  %1 %2 %3 %4)
        a #(stamp attention/master-id :attention attention-rev
                  %1 %2 %3 %4)
        store
        {:slots
         {:one
          {:vi [:vi :ground-block "du:b"]
           :meta {:ground-block "du:b"}
           :tree
           {:data
            (a "du:b" :hit-box :hit-target :block/hit-area)
            :children
            [{:data
              (p "du:b" :machine-rail
                 :provenance-marker :block/decorations)}
             {:data
              (a "du:b" :attention-box
                 :attention-border :block/decorations)}]}}
          :two
          {:vi [:vi :ground-block "du:a"]
           :meta {:ground-block "du:a"}
           :tree
           {:data
            (a "du:a" :hit-box :hit-target :block/hit-area)}}
          :duplicate
          {:vi [:vi :ground-block-copy "du:b"]
           :meta {:ground-block "du:b"}
           :tree
           {:children
            [{:data
              (p "du:b" :episode-boundary
                 :boundary-label :block/prelude)}]}}
          :plain
          {:vi [:vi :ground-block "du:plain"]
           :meta {:ground-block "du:plain"}
           :tree {:data {:address "du:plain"}}}}}
        wearers (inspector/wearers-from-scene-store store)
        by-id (into {} (map (juxt :wearer/entity-id identity)) wearers)]
    (testing "wearers come only from causal stamps and group by master"
      (is (= ["du:a" "du:b"] (mapv :wearer/entity-id wearers)))
      (is (= [attention/master-id]
             (mapv :wearer/master-id
                   (:wearer/facets (get by-id "du:a")))))
      (is (= [attention/master-id provenance/master-id]
             (mapv :wearer/master-id
                   (:wearer/facets (get by-id "du:b")))))
      (is (= [:attention-box :hit-box]
             (->> (get by-id "du:b")
                  :wearer/facets
                  (some
                   #(when
                      (= attention/master-id
                         (:wearer/master-id %))
                      (:wearer/contribution-sites %))))))
      (is (= [[:derived :attention "du:b"]]
             (->> (get by-id "du:b")
                  :wearer/facets
                  (some
                   #(when
                      (= attention/master-id
                         (:wearer/master-id %))
                      (:wearer/attachments %))))))
      (is (= [[:vi :ground-block "du:b"]
              [:vi :ground-block-copy "du:b"]]
             (:wearer/view-instances (get by-id "du:b")))))
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
            block-id
            (some->
             (ocr/read-outline
              runtime
              (oc/document-id-for-object-key
               (:object/key block-request)))
             first
             :target-id)
            provenance-boot
            (adapter/ensure-master! runtime provenance/spec)
            provenance-original-id
            (get-in provenance-boot
                    [:state :active-revision :revision-id])
            attention-boot
            (adapter/ensure-master! runtime attention/spec)
            attention-id
            (get-in attention-boot
                    [:state :active-revision :revision-id])
            candidate
            (adapter/import-candidate!
             runtime
             provenance/spec
             (pr-str
              (assoc provenance/default-form
                     :provenance/tint [0.1 0.2 0.3 0.9]))
             {:request/id "p2-candidate" :time-ms 10})
            candidate-id (:revision-id candidate)
            activation
            (adapter/activate!
             runtime provenance/spec candidate-id
             {:request/id "p2-activate" :time-ms 20})
            malformed
            (adapter/import-candidate!
             runtime provenance/spec
             "{:facet-master/id"
             {:request/id "p2-malformed" :time-ms 30})
            malformed-id (:revision-id malformed)
            rejected
            (adapter/activate!
             runtime provenance/spec malformed-id
             {:request/id "p2-reject" :time-ms 31})
            rollback
            (adapter/activate!
             runtime provenance/spec provenance-original-id
             {:request/id "p2-rollback" :time-ms 40})
            wearers
            [{:wearer/entity-id "du:z"
              :wearer/facets
              [{:wearer/master-id attention/master-id
                :wearer/revision-id attention-id
                :wearer/attachments
                [[:derived :attention "du:z"]]
                :wearer/subjects ["du:z"]
                :wearer/contribution-sites [:hit-box]
                :wearer/contribution-roles [:hit-target]
                :wearer/contribution-slots [:block/hit-area]}]
              :wearer/view-instances
              [[:vi :ground-block "du:z"]]}
             {:wearer/entity-id block-id
              :wearer/facets
              [{:wearer/master-id provenance/master-id
                :wearer/revision-id provenance-original-id
                :wearer/attachments
                [[:derived :provenance block-id]]
                :wearer/subjects [block-id]
                :wearer/contribution-sites
                [:episode-boundary :machine-rail]
                :wearer/contribution-roles
                [:boundary-label :provenance-marker]
                :wearer/contribution-slots
                [:block/prelude :block/decorations]}
               {:wearer/master-id attention/master-id
                :wearer/revision-id attention-id
                :wearer/attachments
                [[:derived :attention block-id]]
                :wearer/subjects [block-id]
                :wearer/contribution-sites
                [:attention-box :hit-box]
                :wearer/contribution-roles
                [:attention-border :hit-target]
                :wearer/contribution-slots
                [:block/decorations :block/hit-area]}]
              :wearer/view-instances
              [[:vi :ground-block block-id]]}]
            request
            {:face :material-inspector
             :params {:entity-id block-id
                      :wearers wearers}}
            states-before
            {provenance/master-id
             (adapter/read-master runtime provenance/spec)
             attention/master-id
             (adapter/read-master runtime attention/spec)}
            histories-before
            (into
             {}
             (mapcat
              (fn [spec]
                [[[(adapter/document-id spec) :candidate]
                  (ocr/read-revision-history
                   runtime (adapter/document-id spec))]
                 [[(adapter/active-pointer-container-id spec) :pointer]
                  (ocr/read-revision-history
                   runtime
                   (adapter/active-pointer-container-id spec))]])
              [provenance/spec attention/spec]))
            response-a
            (face-projection/serve
             {:oc-rt runtime}
             (assoc-in request
                       [:params :request-token] "first"))
            response-b
            (face-projection/serve
             {:oc-rt runtime}
             (assoc-in request
                       [:params :request-token] "second"))
            result (:material-inspector/result response-a)
            entity (:material-inspector/entity result)
            facets
            (into
             {}
             (map
              (fn [facet]
                [(get-in
                  facet
                  [:material-inspector/facet-master
                   :facet-master/id])
                 facet]))
             (:material-inspector/facets result))
            provenance-result (get facets provenance/master-id)
            attention-result (get facets attention/master-id)
            provenance-attachment
            (:material-inspector/attachment provenance-result)
            provenance-master
            (:material-inspector/facet-master provenance-result)
            provenance-trail
            (:material-inspector/revision-trail provenance-result)
            candidates
            (filter #(= :candidate (:trail/kind %))
                    provenance-trail)
            activations
            (filter #(= :activation (:trail/kind %))
                    provenance-trail)
            rollbacks
            (filter #(= :rollback (:trail/kind %))
                    provenance-trail)]
        (testing "fixture establishes active, latest, and rejected truth"
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

        (testing "one answer explains both stamped masters"
          (is (= 1 (:material-inspector/version result)))
          (is (= #{provenance/master-id attention/master-id}
                 (set (keys facets))))
          (is (= [:episode-boundary :machine-rail]
                 (:attachment/contribution-sites
                  provenance-attachment)))
          (is (= [[:derived :provenance block-id]]
                 (:attachment/identities
                  provenance-attachment)))
          (is (true?
               (:attachment/present?
                provenance-attachment)))
          (is (true?
               (:attachment/wears-active?
                provenance-attachment)))
          (is (= attention-id
                 (get-in
                  attention-result
                  [:material-inspector/facet-master
                   :facet-master/active-revision-id]))))

        (testing "provenance reports active and latest independently"
          (is (= provenance-original-id
                 (:facet-master/active-revision-id
                  provenance-master)))
          (is (= malformed-id
                 (:facet-master/latest-revision-id
                  provenance-master)))
          (is (true?
               (:facet-master/active-latest-distinct?
                provenance-master)))
          (is (string?
               (:facet-master/pointer-revision-id
                provenance-master))))

        (testing "the complete trail preserves candidate and activation kinds"
          (is (true?
               (:material-inspector/revision-trail-complete?
                provenance-result)))
          (is (= 3 (count candidates)))
          (is (= 2 (count activations)))
          (is (= 1 (count rollbacks)))
          (is (= #{0 10 30}
                 (set (map :trail/time-ms candidates))))
          (is (= #{0 20}
                 (set (map :trail/time-ms activations))))
          (is (= [40] (mapv :trail/time-ms rollbacks)))
          (is (false?
               (:trail/valid?
                (some
                 #(when
                    (= malformed-id (:trail/revision-id %))
                    %)
                 candidates)))))

        (testing "same input and world returns byte-equal answer"
          (is (= (:material-inspector/result response-a)
                 (:material-inspector/result response-b)))
          (is (= (:material-inspector/edn response-a)
                 (:material-inspector/edn response-b)))
          (is (= (:material-inspector/edn response-a)
                 (inspector/canonical-edn result))))

        (testing "other wearers add no durable reads or master output"
          (is (= (vec (sort ["du:z" block-id]))
                 (mapv
                  :wearer/entity-id
                  (:material-inspector/current-wearers result)))))

        (testing "the batched inspector is read-only for both masters"
          (is (= states-before
                 {provenance/master-id
                  (adapter/read-master runtime provenance/spec)
                  attention/master-id
                  (adapter/read-master runtime attention/spec)}))
          (is (every?
               (fn [[[container-id _] before]]
                 (= before
                    (ocr/read-revision-history
                     runtime container-id)))
               histories-before))))
      (finally
        (ocr/close-object-container-runtime! runtime)))))
