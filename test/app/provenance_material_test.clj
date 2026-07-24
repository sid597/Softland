(ns app.provenance-material-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.server.rama.face-projection :as face-projection]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.provenance-material :as adapter]
            [app.server.rama.object-container.runtime :as ocr]
            [app.shared.provenance-material :as material]))

(deftest provenance-grammar-is-total-and-byte-identical
  (testing "the code floor and imported v0 are one shared value"
    (is (= material/default-source (pr-str material/default-form)))
    (is (= [0.62 0.66 0.76 0.6]
           (get-in (material/compile-source material/default-source)
                   [:material :provenance/tint])))
    (is (= [0.62 0.66 0.76 0.6]
           (:provenance/tint material/code-floor))))
  (testing "the one-facet compiler closes malformed and expanded forms"
    (is (false? (:valid? (material/compile-source "{:broken"))))
    (is (false? (:valid?
                 (material/compile-form
                  (assoc material/default-form :material/recipe :forbidden)))))
    (is (false? (:valid?
                 (material/compile-form
                  (assoc material/default-form
                         :provenance/tint [1.1 0.2 0.3 0.4])))))))

(deftest facet-master-routing-prefixes
  (testing "every P1 foreign key routes to the same two-segment master"
    (let [m (adapter/materialization material/default-source
                                     {:request/id "routing"
                                      :time-ms 1
                                      :include-active-pointer? true})]
      (doseq [key [material/master-id
                   (:source-id m)
                   adapter/document-id
                   adapter/active-pointer-container-id
                   (:revision-id m)
                   (:import-key m)]]
        (is (= material/master-id (oc/extract-object-key key))
            (str "routing key " key))))))

(deftest revisioned-provenance-master-probe
  (let [runtime (ocr/start-object-container-runtime!)]
    (try
      (let [boot (adapter/ensure-master! runtime)
            initial (:state boot)
            original-id (some-> initial :active-revision :revision-id)
            original-pointer-id (some-> initial :active-pointer :revision-id)
            imported-request (some-> boot :import :request)
            default-projection
            (face-projection/provenance-material-projection
             {:oc-rt runtime} {:params {}})]
        (testing "bootstrap is revisioned OC material with an explicit pointer"
          (is (string? original-id))
          (is (= original-id
                 (some-> initial :latest-revision :revision-id)))
          (is (= original-id
                 (some-> initial :active-pointer :content-text)))
          (is (= material/default-source
                 (some-> initial :active-revision :content-text)))
          (is (= [0.62 0.66 0.76 0.6]
                 (get-in default-projection
                         [:facet-master/material :provenance/tint])))
          ;; The IPC runtime launches four tasks. These point reads cross the
          ;; partition boundary and fail if fm:/imp:fm: routing is incomplete.
          (is (some? (ocr/read-import-completion
                      runtime (:import/key imported-request))))
          (is (some? (ocr/read-source runtime
                                      (oc/source-id-for-object-key
                                       material/master-id))))
          (is (some? (ocr/read-container runtime adapter/document-id)))
          (is (some? (ocr/read-revision runtime original-id))))

        (let [next-tint [0.12 0.34 0.56 0.78]
              candidate-source
              (pr-str (assoc material/default-form
                             :provenance/tint next-tint))
              imported
              (adapter/import-candidate!
               runtime candidate-source
               {:request/id "p1-valid-candidate" :time-ms 10})
              candidate-id (:revision-id imported)
              saved (adapter/read-master runtime)
              before-activation
              (face-projection/provenance-material-projection
               {:oc-rt runtime} {:params {}})]
          (testing "candidate save advances latest but never active"
            (is (:accepted? imported))
            (is (= candidate-id
                   (some-> saved :latest-revision :revision-id)))
            (is (= original-id
                   (some-> saved :active-revision :revision-id)))
            (is (not= candidate-id original-id))
            (is (= [0.62 0.66 0.76 0.6]
                   (get-in before-activation
                           [:facet-master/material :provenance/tint]))))

          (let [activation
                (adapter/activate!
                 runtime candidate-id
                 {:request/id "p1-activate-candidate" :time-ms 11})
                active (adapter/read-master runtime)
                projection
                (face-projection/provenance-material-projection
                 {:oc-rt runtime} {:params {}})
                wear (material/resolved-wear projection)]
            (testing "one confirmed activation moves the served revision"
              (is (:accepted? activation))
              (is (= candidate-id
                     (some-> active :active-revision :revision-id)))
              (is (= candidate-id
                     (some-> active :active-pointer :content-text)))
              (is (not= original-pointer-id
                        (some-> active :active-pointer :revision-id)))
              (is (= next-tint (:provenance/tint wear)))
              (is (= candidate-id (:facet-master/revision-id wear)))
              (is (= #{{:material/master material/master-id
                        :material/revision candidate-id
                        :material/site :fold-header}
                       {:material/master material/master-id
                        :material/revision candidate-id
                        :material/site :machine-rail}
                       {:material/master material/master-id
                        :material/revision candidate-id
                        :material/site :episode-boundary}}
                     (set (map #(material/contribution-stamp wear %)
                               [:fold-header
                                :machine-rail
                                :episode-boundary])))))

            (let [rollback
                  (adapter/activate!
                   runtime original-id
                   {:request/id "p1-rollback" :time-ms 12})
                  rolled-back (adapter/read-master runtime)
                  pointer-before-drill
                  (some-> rolled-back :active-pointer :revision-id)
                  drill (adapter/malformed-drill! runtime "p1-malformed")
                  after-drill (adapter/read-master runtime)
                  drill-projection
                  (face-projection/provenance-material-projection
                   {:oc-rt runtime} {:params {:drill? true}})
                  ordinary-projection
                  (face-projection/provenance-material-projection
                   {:oc-rt runtime} {:params {}})]
              (testing "rollback is a repoint and active remains independent"
                (is (:accepted? rollback))
                (is (= original-id
                       (some-> rolled-back :active-revision :revision-id)))
                (is (= candidate-id
                       (some-> rolled-back :latest-revision :revision-id)))
                (is (not= (some-> rolled-back
                                  :active-revision :revision-id)
                          (some-> rolled-back
                                  :latest-revision :revision-id))))
              (testing "malformed candidate is retained without harming wear"
                (is (seq (:activation-errors drill)))
                (is (= pointer-before-drill
                       (some-> after-drill :active-pointer :revision-id)))
                (is (= original-id
                       (some-> after-drill :active-revision :revision-id)))
                (is (= (:candidate-revision-id drill)
                       (some-> after-drill :latest-revision :revision-id)))
                (is (= original-id
                       (:facet-master/active-revision-id drill-projection)))
                (is (= [0.62 0.66 0.76 0.6]
                       (get-in drill-projection
                               [:facet-master/material :provenance/tint])))
                (is (seq (:facet-master/candidate-errors drill-projection)))
                (is (= (:candidate-revision-id drill)
                       (:facet-master/candidate-revision-id
                        drill-projection)))
                (is (not (contains? ordinary-projection
                                    :facet-master/candidate-errors))))))))
      (finally
        (ocr/close-object-container-runtime! runtime)))))
