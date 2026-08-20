(ns app.space-material-test
  "Space-as-entity P2 executable gates: the camera reservation, space-master
   totality, and RULING R1's compile+wear whole-form seam."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [app.shared.binding-material :as binding-material]
            [app.shared.facet-material :as facet-material]
            [app.shared.facet-masters :as facet-masters]
            [app.shared.space-material :as space]))

(defn- floor-rows
  []
  (binding-material/with-halo-floor-bindings
   (assoc
    (into {}
          (map (fn [spec]
                 [(:facet-master/facet spec)
                  (:facet-master/bindings (facet-material/code-floor spec))]))
          facet-masters/specs)
    binding-material/space-facet binding-material/space-floor-bindings)))

(defn- served-from-form
  [form revision-id]
  (let [compiled (space/compile-form form)]
    {:facet-master/id space/master-id
     :facet-master/facet :space
     :facet-master/grammar (:grammar compiled)
     :facet-master/active-revision-id revision-id
     :facet-master/material (:material compiled)}))

(defn- report-with-space
  [served]
  (let [wear (space/resolved-wear served)]
    (binding-material/drill-report
     {:facet-rows
      {:space (when-not (:facet-master/floor? wear)
                (:facet-master/bindings wear))}
      :floor-rows (floor-rows)
      :instance-rows {}})))

(defn- report-by-label
  [report]
  (into {} (map (juxt :probe/label identity)) report))

(def capture-row
  {:binding/gesture :pointer/press
   :binding/phase :threshold
   :binding/modifiers #{}
   :binding/verb {:verb/name :selection/marquee-begin :verb/version 0}
   :binding/priority 10})

(def r3-instance-tap-row
  {:binding/gesture :pointer/tap
   :binding/phase :complete
   :binding/modifiers #{}
   :binding/verb {:verb/name :selection/marquee-begin :verb/version 0}
   :binding/priority 10})

(deftest r3-g1-g2-space-instance-drill
  (let [served (served-from-form space/default-form "rev:space:active")
        baseline (report-by-label (report-with-space served))
        injected
        (report-by-label
         (binding-material/drill-report
          {:facet-rows {:space (:facet-master/bindings
                                (space/resolved-wear served))}
           :floor-rows (floor-rows)
           :instance-rows {[:space :space/ground] [r3-instance-tap-row]}}))
        foreign-subject
        (report-by-label
         (binding-material/drill-report
          {:facet-rows {:space (:facet-master/bindings
                                (space/resolved-wear served))}
           :floor-rows (floor-rows)
           :instance-rows {["other-space" :space/ground]
                           [r3-instance-tap-row]}}))]
    (testing "R3-G1 — the old twelve probes plus Halo's four floor probes answer"
      (is (= 16 (count baseline)))
      (is (= [:anchor/place :master]
             ((juxt :probe/verb :probe/tier)
              (get baseline "tap empty space → caret anchor"))))
      (is (= :floor
             (:probe/tier
              (get baseline "wheel at a block → zoom through the space rung")))))
    (testing "R3-G2/T-R1/T-R5 — injected keyword-space rows fire at instance"
      (is (= [:selection/marquee-begin :instance]
             ((juxt :probe/verb :probe/tier)
              (get injected "tap empty space → caret anchor")))))
    (testing "T-R1 — another durable space subject cannot hijack THE space"
      (is (= [:anchor/place :master]
             ((juxt :probe/verb :probe/tier)
              (get foreign-subject "tap empty space → caret anchor")))))
    (testing "R3-G2 — the same instance map cannot capture either camera probe"
      (doseq [label ["drag empty space → pan the camera"
                     "wheel → zoom at the pointer"
                     "wheel at a block → zoom through the space rung"]]
        (is (= :floor (:probe/tier (get injected label))) label)))))

(deftest g4-camera-gesture-fence-refuses-material-and-preserves-the-floor
  (let [candidate (assoc space/default-form
                         :facet-master/bindings
                         {:space/ground [capture-row]})
        compiled (space/compile-form candidate)
        synthetic-served
        {:facet-master/id space/master-id
         :facet-master/facet :space
         :facet-master/grammar space/bindings-grammar-version
         :facet-master/active-revision-id "rev:space:capture-attempt"
         :facet-master/material
         (select-keys candidate
                      (get-in space/spec
                              [:facet-master/grammars
                               space/bindings-grammar-version
                               :material-keys]))}
        wear (space/resolved-wear synthetic-served)
        by-label (report-by-label (report-with-space synthetic-served))]
    (testing "the naked threshold gesture is refused independently of its verb"
      (is (false? (:valid? compiled)))
      (is (some #(= :facet-master/bindings-invalid (:type %))
                (:errors compiled))))
    (testing "the hostile served value floors the whole facet"
      (is (true? (:facet-master/floor? wear)))
      (is (= 0.1 (:space/zoom-min wear)))
      (is (= 8.0 (:space/zoom-max wear))))
    (testing "the unhurt floor still owns pan and zoom"
      (is (= [:camera/pan :floor :claimed]
             ((juxt :probe/verb :probe/tier :probe/outcome)
              (get by-label "drag empty space → pan the camera"))))
      (is (= [:camera/zoom-at-pointer :floor :claimed]
             ((juxt :probe/verb :probe/tier :probe/outcome)
              (get by-label "wheel → zoom at the pointer")))))))

(deftest g6-space-master-meaning-changes-tier-but-camera-never-does
  (let [report (report-by-label
                (report-with-space
                 (served-from-form space/default-form "rev:space:active")))]
    (is (= [:anchor/place :master]
           ((juxt :probe/verb :probe/tier)
            (get report "tap empty space → caret anchor"))))
    (is (= [:selection/marquee-begin :master]
           ((juxt :probe/verb :probe/tier)
            (get report "shift-drag empty space → marquee"))))
    (is (= [:camera/pan :floor]
           ((juxt :probe/verb :probe/tier)
            (get report "drag empty space → pan the camera"))))
    (is (= [:camera/zoom-at-pointer :floor]
           ((juxt :probe/verb :probe/tier)
            (get report "wheel → zoom at the pointer"))))))

(deftest g9-space-floor-label-is-spec-derived-and-coherent
  (is (= space/code-floor-revision-id
         binding-material/space-floor-master-id
         (facet-material/floor-master-id space/spec)
         (facet-masters/floor-master-id :space)))
  (is (not (str/includes?
            (slurp "test/app/binding_dispatch_test.clj")
            "\"code-floor:space\""))))

(deftest g11-arbitrary-garbage-floors-the-whole-new-facet
  (let [compiled (space/compile-form {:drill/garbage true})
        served {:facet-master/id space/master-id
                :facet-master/facet :space
                :facet-master/grammar space/bindings-grammar-version
                :facet-master/active-revision-id "rev:space:garbage"
                :facet-master/material {:drill/garbage true}}
        wear (space/resolved-wear served)
        report (report-with-space served)]
    (is (false? (:valid? compiled)))
    (is (seq (:errors compiled)) "candidate supplies the error card")
    (is (true? (:facet-master/floor? wear)))
    (is (= #{:space/zoom-min :space/zoom-max}
           (set (keys (select-keys wear
                                  [:space/zoom-min :space/zoom-max])))))
    (is (= 16 (count report)))
    (is (every? #(= :claimed (:probe/outcome %)) report))
    (is (every? #(= :floor (:probe/tier %)) report))))

(deftest g12-form-validator-seam-is-a-no-op-and-guards-both-sites
  (testing "existing declarations have no entry; the old output shape is exact"
    (let [existing-specs
          (remove #(= space/master-id (:facet-master/id %))
                  facet-masters/specs)]
      (is (= 8 (count existing-specs))
          "smalltalk-ui-vm adds anatomy and invocation without changing the form-validator seam")
      (doseq [spec existing-specs
              [_ declaration] (:facet-master/grammars spec)]
        (is (not (contains? declaration :form-validators))
            (:facet-master/id spec))))
    (let [probe-spec
          {:facet-master/id "fm:probe"
           :facet-master/facet :probe
           :facet-master/default-form
           {:facet-master/id "fm:probe" :facet-master/grammar 0
            :facet-master/facet :probe :probe/value 7}
           :facet-master/floor-form
           {:facet-master/id "fm:probe" :facet-master/grammar 0
            :facet-master/facet :probe :probe/value 7}
           :facet-master/code-floor-revision-id "code-floor:fm:probe:v0"
           :facet-master/grammars
           {0 {:material-keys #{:probe/value}
               :validators
               {:probe/value {:valid? integer?
                              :error-type :probe/value-invalid}}}}}
          form (:facet-master/default-form probe-spec)]
      (is (= {:valid? true :errors [] :grammar 0
              :material {:probe/value 7}}
             (facet-material/compile-form probe-spec form)))
      (is (true? (facet-material/valid-material?
                  probe-spec 0 {:probe/value 7})))))

  (testing "candidate min>=max refuses with the declared error"
    (let [compiled (space/compile-form
                    (assoc space/default-form
                           :space/zoom-min 5.0
                           :space/zoom-max 4.0))]
      (is (false? (:valid? compiled)))
      (is (= [:space/zoom-clamp-invalid]
             (mapv :type (:errors compiled))))))

  (testing "the same malformed material is refused at wear time"
    (let [valid (:material (space/compile-form space/default-form))
          served {:facet-master/id space/master-id
                  :facet-master/facet :space
                  :facet-master/grammar space/bindings-grammar-version
                  :facet-master/active-revision-id "rev:space:bad-clamp"
                  :facet-master/material
                  (assoc valid :space/zoom-min 5.0 :space/zoom-max 4.0)}
          wear (space/resolved-wear served)]
      (is (true? (:facet-master/floor? wear)))
      (is (= [0.1 8.0]
             ((juxt :space/zoom-min :space/zoom-max) wear)))))

  (testing "instance projections carry extra keys without breaking the predicate"
    (let [subject "du:space-form-validator"
          iid (facet-material/instance-master-id
               space/master-id "spaceform")
          ispec (facet-material/instance-spec space/spec iid subject)
          shared (:material (space/compile-form space/default-form))
          form (facet-material/instance-form
                space/spec iid subject
                {:grammar space/bindings-grammar-version
                 :material shared
                 :overrides {}
                 :deviates? true
                 :pin nil})
          compiled (facet-material/compile-form ispec form)
          malformed
          (facet-material/compile-form
           ispec (assoc form :space/zoom-min 5.0 :space/zoom-max 4.0))]
      (is (true? (:valid? compiled)))
      (is (false? (:valid? malformed)))
      (is (= [:space/zoom-clamp-invalid]
             (mapv :type (:errors malformed)))))))
