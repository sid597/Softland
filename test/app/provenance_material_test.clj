(ns app.provenance-material-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [app.server.rama.face-projection :as face-projection]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.facet-master :as adapter]
            [app.server.rama.object-container.runtime :as ocr]
            [app.shared.attention-material :as attention]
            [app.shared.facet-material :as facet-material]
            [app.shared.facet-masters :as facet-masters]
            [app.shared.foldable-material :as foldable]
            [app.shared.positioned-material :as positioned]
            [app.shared.provenance-material :as provenance]
            [app.shared.text-body-material :as text-body]
            [app.shared.threaded-material :as threaded]))

(deftest facet-grammars-are-total-and-current-values-are-exact
  (testing "durable provenance v0 keeps its original bytes and meaning"
    (is (= provenance/default-source (pr-str provenance/default-form)))
    (is (= {:provenance/tint [0.62 0.66 0.76 0.6]}
           (:material
            (provenance/compile-source provenance/default-source))))
    (is (= 0
           (:grammar
            (provenance/compile-source provenance/default-source)))))
  (testing "the explicitly versioned provenance composition form is exact"
    (is (= {:facet-master/merge :append
            :facet-master/priority 10
            :provenance/tint [0.62 0.66 0.76 0.6]}
           (:material
            (provenance/compile-source provenance/composition-source))))
    (is (= 1
           (:grammar
            (provenance/compile-source provenance/composition-source)))))
  (testing "attention owns exactly the worn box policy"
    (is (= {:facet-master/merge :append
            :facet-master/priority 20
            :attention/hit-padding 8.0
            :attention/border-width 1.0
            :attention/border-color [0.45 0.52 0.66 0.55]
            :attention/background [0.0 0.0 0.0 0.0]}
           (:material
            (attention/compile-source attention/default-source))))
    (is (= (:material
            (attention/compile-source attention/default-source))
           (dissoc attention/code-floor
                   :facet-master/id
                   :facet-master/facet
                   :facet-master/grammar
                   :facet-master/revision-id
                   :facet-master/floor?))))
  (testing "foldable owns the exact collapsed defaults and visible copy"
    (is (= {:foldable/defaults {:noise? false :prose? false}
            :foldable/header-copy
            {:noise-label "thinking+tools"
             :prose-label "reply"
             :collapsed-marker "▸ "
             :expanded-marker "▾ "
             :show-suffix " — click to show"
             :hide-suffix " — click to hide"
             :line-count-prefix " (+"
             :line-count-suffix " lines)"}}
           (:material
            (foldable/compile-source foldable/default-source))))
    (is (= (:material
            (foldable/compile-source foldable/default-source))
           (dissoc foldable/code-floor
                   :facet-master/id
                   :facet-master/facet
                   :facet-master/grammar
                   :facet-master/revision-id
                   :facet-master/floor?))))
  (testing "positioned owns only shared pre-cell defaults and birth policy"
    (is (= {:positioned/reply-gap 34.0
            :positioned/fallback-position {:x 60.0 :y 60.0}
            :positioned/anchor-order
            {:machine [:same-source-tail :source :previous]
             :ordinary [:previous]}
            :positioned/persist-derived-reply-birth? true}
           (:material
            (positioned/compile-source positioned/default-source))))
    (is (= (:material
            (positioned/compile-source positioned/default-source))
           (dissoc positioned/code-floor
                   :facet-master/id
                   :facet-master/facet
                   :facet-master/grammar
                   :facet-master/revision-id
                   :facet-master/floor?))))
  (testing "threaded owns only the column-adoption reach"
    (is (= {:threaded/column-adoption-reach-lines 3.0}
           (:material
            (threaded/compile-source threaded/default-source))))
    (is (= (:material
            (threaded/compile-source threaded/default-source))
           (dissoc threaded/code-floor
                   :facet-master/id
                   :facet-master/facet
                   :facet-master/grammar
                   :facet-master/revision-id
                   :facet-master/floor?))))
  (testing "text-body owns only the wrap floor and no-source fallback"
    (is (= {:text-body/wrap-floor-columns 32
            :text-body/wrap-fallback-columns 80}
           (:material
            (text-body/compile-source text-body/default-source))))
    (is (= (:material
            (text-body/compile-source text-body/default-source))
           (dissoc text-body/code-floor
                   :facet-master/id
                   :facet-master/facet
                   :facet-master/grammar
                   :facet-master/revision-id
                   :facet-master/floor?))))
  (testing "facet compilers close malformed, expanded, and invalid policy"
    (is (false? (:valid? (attention/compile-source "{:broken"))))
    (is (false?
         (:valid?
          (attention/compile-form
           (assoc attention/default-form :material/recipe :forbidden)))))
    (is (false?
         (:valid?
          (attention/compile-form
           (assoc attention/default-form
                  :attention/border-color [1.1 0.2 0.3 0.4])))))
    (is (false?
         (:valid?
          (provenance/compile-form
           (assoc provenance/default-form
                  :facet-master/merge :append)))))
    (is (false?
         (:valid?
          (foldable/compile-form
           (assoc foldable/default-form
                  :foldable/defaults {:noise? :sometimes
                                      :prose? false})))))
    (is (false?
         (:valid?
          (positioned/compile-form
           (assoc positioned/default-form
                  :positioned/anchor-order
                  {:machine [:source :source]
                   :ordinary [:previous]})))))
    (is (false?
         (:valid?
          (threaded/compile-form
           (assoc threaded/default-form
                  :threaded/column-adoption-reach-lines -1.0)))))
    (is (false?
         (:valid?
          (text-body/compile-form
           (assoc text-body/default-form
                  :text-body/wrap-floor-columns 0)))))))

(deftest registered-master-families-route-to-their-own-two-segment-key
  (doseq [[spec source]
          [[provenance/spec provenance/default-source]
           [attention/spec attention/default-source]
           [foldable/spec foldable/default-source]
           [positioned/spec positioned/default-source]
           [threaded/spec threaded/default-source]
           [text-body/spec text-body/default-source]]]
    (let [master-id (:facet-master/id spec)
          m (adapter/materialization
             spec source
             {:request/id (str "routing-" master-id)
              :time-ms 1
              :include-active-pointer? true})]
      (doseq [key [master-id
                   (:source-id m)
                   (adapter/document-id spec)
                   (adapter/active-pointer-container-id spec)
                   (:revision-id m)
                   (:import-key m)]]
        (is (= master-id (oc/extract-object-key key))
            (str "routing key " key))))))

(deftest second-wearer-rides-the-p1-lifecycle-and-single-batch
  (let [runtime (ocr/start-object-container-runtime!)]
    (try
      (let [provenance-v0
            (adapter/ensure-master! runtime provenance/spec)
            provenance-v0-id
            (get-in provenance-v0
                    [:state :active-revision :revision-id])
            provenance-v1
            (adapter/ensure-active-source!
             runtime
             provenance/spec
             provenance/composition-source
             {:request-id "test-provenance-v1"
              :activation-request-id "test-provenance-activate-v1"
              :time-ms 1})
            provenance-v1-id
            (get-in provenance-v1
                    [:state :active-revision :revision-id])
            attention-boot
            (adapter/ensure-master! runtime attention/spec)
            foldable-boot
            (adapter/ensure-master! runtime foldable/spec)
            positioned-boot
            (adapter/ensure-master! runtime positioned/spec)
            threaded-boot
            (adapter/ensure-master! runtime threaded/spec)
            text-body-boot
            (adapter/ensure-master! runtime text-body/spec)
            original-state (:state attention-boot)
            original-id
            (some-> original-state :active-revision :revision-id)
            original-pointer-id
            (some-> original-state :active-pointer :revision-id)
            initial-batch
            (face-projection/facet-materials-projection
             {:oc-rt runtime} {:params {}})
            initial-attention
            (get-in initial-batch
                    [:facet-materials/by-id attention/master-id])]
        (testing "provenance v0 remains rewearable while v1 is active"
          (is (string? provenance-v0-id))
          (is (not= provenance-v0-id provenance-v1-id))
          (is (= provenance/default-source
                 (:content-text
                  (ocr/read-revision runtime provenance-v0-id))))
          (is (= provenance/composition-source
                 (get-in provenance-v1
                         [:state :active-revision :content-text]))))

        (testing "the batch serves registered masters with independent identities"
          (is (= (set facet-masters/master-ids)
                 (set
                  (keys (:facet-materials/by-id initial-batch)))))
          (is (= original-id
                 (:facet-master/active-revision-id initial-attention)))
          (is (= attention/default-source
                 (some-> original-state :active-revision :content-text)))
          (is (some?
               (ocr/read-import-completion
                runtime
                (get-in attention-boot
                        [:import :request :import/key]))))
          (is (some?
               (ocr/read-container
                runtime (adapter/document-id attention/spec))))
          (is (= foldable/default-source
                 (get-in foldable-boot
                         [:state :active-revision :content-text])))
          (is (= {:noise? false :prose? false}
                 (:foldable/defaults
                  (foldable/resolved-wear
                   (get-in initial-batch
                           [:facet-materials/by-id
                            foldable/master-id])))))
          (is (= positioned/default-source
                 (get-in positioned-boot
                         [:state :active-revision :content-text])))
          (is (= {:x 60.0 :y 60.0}
                 (:positioned/fallback-position
                  (positioned/resolved-wear
                   (get-in initial-batch
                           [:facet-materials/by-id
                            positioned/master-id])))))
          (is (= threaded/default-source
                 (get-in threaded-boot
                         [:state :active-revision :content-text])))
          (is (= 3.0
                 (:threaded/column-adoption-reach-lines
                  (threaded/resolved-wear
                   (get-in initial-batch
                           [:facet-materials/by-id
                            threaded/master-id])))))
          (is (= text-body/default-source
                 (get-in text-body-boot
                         [:state :active-revision :content-text])))
          (is (= {:text-body/wrap-floor-columns 32
                  :text-body/wrap-fallback-columns 80}
                 (select-keys
                  (text-body/resolved-wear
                   (get-in initial-batch
                           [:facet-materials/by-id
                            text-body/master-id]))
                  [:text-body/wrap-floor-columns
                   :text-body/wrap-fallback-columns])))

        (let [next-color [0.12 0.34 0.56 0.78]
              candidate-source
              (pr-str
               (assoc attention/default-form
                      :attention/border-color next-color))
              imported
              (adapter/import-candidate!
               runtime attention/spec candidate-source
               {:request/id "attention-valid-candidate"
                :time-ms 10})
              candidate-id (:revision-id imported)
              before-activation
              (get-in
               (face-projection/facet-materials-projection
                {:oc-rt runtime} {:params {}})
               [:facet-materials/by-id attention/master-id])]
          (testing "candidate save advances latest, never active"
            (is (:accepted? imported))
            (is (= candidate-id
                   (some->
                    (adapter/read-master runtime attention/spec)
                    :latest-revision
                    :revision-id)))
            (is (= original-id
                   (:facet-master/active-revision-id before-activation)))
            (is (= [0.45 0.52 0.66 0.55]
                   (get-in before-activation
                           [:facet-master/material
                            :attention/border-color]))))

          (let [activation
                (adapter/activate!
                 runtime attention/spec candidate-id
                 {:request/id "attention-activate-candidate"
                  :time-ms 11})
                active (adapter/read-master runtime attention/spec)
                served
                (get-in
                 (face-projection/facet-materials-projection
                  {:oc-rt runtime} {:params {}})
                 [:facet-materials/by-id attention/master-id])
                wear (attention/resolved-wear served)
                attention-stamp
                (attention/contribution-stamp
                 wear "du:test" :attention-box
                 :attention-border :block/decorations)
                provenance-wear
                (provenance/resolved-wear
                 (get-in
                  initial-batch
                  [:facet-materials/by-id provenance/master-id]))
                provenance-stamp
                (provenance/contribution-stamp
                 provenance-wear "du:test" :machine-rail
                 :provenance-marker :block/decorations)
                composition
                (facet-material/compose
                 [{:wear wear
                   :stamp attention-stamp
                   :value :attention}
                  {:wear provenance-wear
                   :stamp provenance-stamp
                   :value :provenance}])]
            (testing "confirmed activation moves attention and stamps causality"
              (is (:accepted? activation))
              (is (= candidate-id
                     (some-> active :active-revision :revision-id)))
              (is (= candidate-id
                     (some-> active :active-pointer :content-text)))
              (is (not= original-pointer-id
                        (some-> active :active-pointer :revision-id)))
              (is (= next-color (:attention/border-color wear)))
              (is (= {:material/subject "du:test"
                      :material/attachment
                      [:derived :attention "du:test"]
                      :material/master attention/master-id
                      :material/revision candidate-id
                      :material/site :attention-box
                      :material/role :attention-border
                      :material/slot :block/decorations}
                     attention-stamp)))

            (testing "the first collision declares append order and no conflict"
              (is (= [:provenance :attention]
                     (mapv :value (:contributions composition))))
              (is (empty? (:conflicts composition)))
              (is (= :material-composition/priority-tie
                     (-> (facet-material/compose
                          [{:wear
                            (assoc wear :facet-master/priority 10)
                            :stamp attention-stamp
                            :value :attention}
                           {:wear provenance-wear
                            :stamp provenance-stamp
                            :value :provenance}])
                         :conflicts first :type))))

            (let [rollback
                  (adapter/activate!
                   runtime attention/spec original-id
                   {:request/id "attention-rollback" :time-ms 12})
                  rolled-back
                  (adapter/read-master runtime attention/spec)
                  pointer-before-drill
                  (some-> rolled-back :active-pointer :revision-id)
                  drill
                  (adapter/malformed-drill!
                   runtime attention/spec "attention-malformed")
                  after-drill
                  (adapter/read-master runtime attention/spec)
                  drill-batch
                  (face-projection/facet-materials-projection
                   {:oc-rt runtime} {:params {:drill? true}})
                  drill-served
                  (get-in drill-batch
                          [:facet-materials/by-id attention/master-id])
                  ordinary-served
                  (get-in
                   (face-projection/facet-materials-projection
                    {:oc-rt runtime} {:params {}})
                   [:facet-materials/by-id attention/master-id])]
              (testing "rollback is only a pointer repoint"
                (is (:accepted? rollback))
                (is (= original-id
                       (some-> rolled-back
                               :active-revision
                               :revision-id)))
                (is (= candidate-id
                       (some-> rolled-back
                               :latest-revision
                               :revision-id))))
              (testing "malformed candidate is retained; active wear is total"
                (is (seq (:activation-errors drill)))
                (is (= pointer-before-drill
                       (some-> after-drill
                               :active-pointer
                               :revision-id)))
                (is (= original-id
                       (some-> after-drill
                               :active-revision
                               :revision-id)))
                (is (= (:candidate-revision-id drill)
                       (some-> after-drill
                               :latest-revision
                               :revision-id)))
                (is (= original-id
                       (:facet-master/active-revision-id drill-served)))
                (is (seq
                     (:facet-master/candidate-errors drill-served)))
                (is (not
                     (contains?
                      ordinary-served
                      :facet-master/candidate-errors)))))))

        (testing "unavailable serve remains a total registered-master floor"
          (let [batch
                (face-projection/facet-materials-projection
                 {:oc-rt nil} {:params {}})]
            (is (= (set facet-masters/master-ids)
                   (set (keys (:facet-materials/by-id batch)))))
            (is (every?
                 :facet-master/floor?
                 (vals (:facet-materials/by-id batch))))))))
      (finally
        (ocr/close-object-container-runtime! runtime)))))

(deftest resumed-facets-activate-and-rollback-on-the-shared-lifecycle
  (let [runtime (ocr/start-object-container-runtime!)
        cases
        [[foldable/spec
          (pr-str
           (-> foldable/default-form
               (assoc :foldable/defaults
                      {:noise? false :prose? true})
               (assoc-in [:foldable/header-copy :collapsed-marker] "x ")))
          "foldable"]
         [positioned/spec
          (pr-str
           (assoc positioned/default-form
                  :positioned/reply-gap 134.0))
          "positioned"]
         [threaded/spec
          (pr-str
           (assoc threaded/default-form
                  :threaded/column-adoption-reach-lines 0.0))
          "threaded"]
         [text-body/spec
          (pr-str
           (assoc text-body/default-form
                  :text-body/wrap-floor-columns 100
                  :text-body/wrap-fallback-columns 120))
          "text-body"]]]
    (try
      (doseq [[i [spec candidate-source slug]]
              (map-indexed vector cases)]
        (let [master-id (:facet-master/id spec)
              boot (adapter/ensure-master! runtime spec)
              original-id
              (get-in boot [:state :active-revision :revision-id])
              imported
              (adapter/import-candidate!
               runtime spec candidate-source
               {:request/id (str "p3-" slug "-candidate")
                :time-ms (+ 100 (* i 3))})
              candidate-id (:revision-id imported)
              before-activation (adapter/read-master runtime spec)
              activation
              (adapter/activate!
               runtime spec candidate-id
               {:request/id (str "p3-" slug "-activate")
                :time-ms (+ 101 (* i 3))})
              active-served
              (get-in
               (face-projection/facet-materials-projection
                {:oc-rt runtime} {:params {}})
               [:facet-materials/by-id master-id])
              rollback
              (adapter/activate!
               runtime spec original-id
               {:request/id (str "p3-" slug "-rollback")
                :time-ms (+ 102 (* i 3))})
              rolled-back (adapter/read-master runtime spec)]
          (testing (str slug " uses candidate, activation, and pointer rollback")
            (is (:accepted? imported))
            (is (= original-id
                   (get-in before-activation
                           [:active-revision :revision-id])))
            (is (= candidate-id
                   (get-in before-activation
                           [:latest-revision :revision-id])))
            (is (:accepted? activation))
            (is (= candidate-id
                   (:facet-master/active-revision-id active-served)))
            (is (= (:material
                    (facet-material/compile-source spec candidate-source))
                   (:facet-master/material active-served)))
            (is (:accepted? rollback))
            (is (= original-id
                   (get-in rolled-back
                           [:active-revision :revision-id])))
            (is (= candidate-id
                   (get-in rolled-back
                           [:latest-revision :revision-id]))))))
      (finally
        (ocr/close-object-container-runtime! runtime)))))

(deftest second-wearer-left-no-parallel-artery-or-old-policy-literals
  (let [ground (slurp "src/app/client/workspace/ground.cljs")
        wiring
        (str (slurp "src/app/client/workspace/face_wiring.cljs")
             (slurp "src/app/electric_flow.cljc")
             (slurp "src/app/client/workspace/runtime.cljs"))
        projection
        (slurp "src/app/server/rama/face_projection.clj")]
    (is (not (str/includes? ground "(def ^:private block-pad")))
    (is (not (str/includes? ground "(def ^:private attention-border")))
    (is (not (str/includes? ground "(def ^:private fold-default")))
    (is (not (str/includes? ground "(def ^:private reply-gap")))
    (is (not (str/includes? ground "34.0")))
    (is (not (str/includes? ground "{:x 60.0 :y 60.0}")))
    (is (not (str/includes? ground "reach (* 3")))
    (is (not (str/includes? ground "(max 32")))
    (is (not (str/includes? ground "fallback 80")))
    (is (str/includes? ground
                       "(not (:placement-derived? live))"))
    (is (str/includes? ground
                       ":wrap-source-columns (reply-source-columns text)"))
    (is (str/includes? ground "(reconcile! ctx)"))
    (is (not (str/includes? ground "[0.45 0.52 0.66 0.55]")))
    (is (not (str/includes? ground "\"thinking+tools\"")))
    (is (not (str/includes? ground "\" — click to show\"")))
    (is (not (str/includes? ground "\" — click to hide\"")))
    (is (not (str/includes? wiring "!attention-material")))
    (is (not (str/includes? wiring ":attention-material")))
    (is (not (str/includes? wiring "!provenance-material")))
    (is (not (str/includes? projection ":provenance-material")))
    (is (not
         (.exists
          (io/file
           "src/app/server/rama/object_container/provenance_material.clj"))))))
