(ns app.server.worn.material-truth-test
  "editable-material P6 — the truth loop whole. Gates G1–G14.

   Every gate is numbered against CONTRACT_P6.md §Acceptance gates and cites
   the trap it exists to keep shut. Gates marked LIVE in the contract (G6 echo
   bar, G12's live drill re-run, G3's camera-pinned byte proof) are proven on
   the cluster and recorded in the gate report; what is executable in the JVM
   is executed here."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [app.server.episode.episode :as episode]
            [app.server.page.face-projection :as face-projection]
            [app.server.worn.material-truth :as material-truth]
            [app.server.rama.object-container :as oc]
            [app.server.worn.facet-master :as adapter]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.ingest-epoch :as ingest-epoch]
            [app.server.worn.activation-event :as activation-event]
            [app.server.worn.attention-material :as attention]
            [app.server.worn.binding-material :as binding-material]
            [app.server.worn.facet-material :as facet-material]
            [app.server.worn.facet-masters :as facet-masters]
            [app.server.worn.foldable-material :as foldable]
            [app.server.worn.positioned-material :as positioned]
            [app.server.worn.space-material :as space]
            [app.server.page.verb-registry :as verb-registry]))

(def sid {:actor/id "sid" :actor/type :human})
(def alpha "du:block:alpha")
(def beta "du:block:beta")
(def gamma "du:block:gamma")
(def delta "du:block:delta")
(def wearers [alpha beta gamma delta])

(defn- now [] (System/currentTimeMillis))

(defn- boot-attention!
  "Bootstrap the shared attention master and activate its v2 bindings, the way
   the deploy-time ingest does. Returns the active revision id."
  [rt]
  (adapter/ensure-master! rt attention/spec)
  (get-in (adapter/ensure-active-source!
           rt attention/spec attention/strict-bindings-source
           {:request-id "gate-att-v2"
            :activation-request-id "gate-att-activate-v2"
            :time-ms (now)})
          [:state :active-revision :revision-id]))

(defn- worn
  "One subject's attention wear, resolved through the SERVED tiers exactly as
   the client resolves it — the serve is the only input, so a gate cannot pass
   by reading something the land cannot see."
  [rt subject]
  (let [served (face-projection/facet-materials-projection
                {:oc-rt rt} {:params {:subjects wearers}})
        shared (get-in served [:facet-materials/by-id attention/master-id])
        inst (get-in served [:facet-materials/instances :attention subject])]
    (facet-material/wear-for-subject attention/spec shared inst)))

;; ===========================================================================
;; G1 — instance deviation end to end
;; ===========================================================================

(deftest g1-instance-deviation-end-to-end
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [shared-rev (boot-attention! rt)
            before-shared (adapter/read-master rt attention/spec)
            d (material-truth/deviate!
               rt attention/spec alpha {:attention/hit-padding 24.0}
               {:actor sid
                :grounds [(activation-event/ground
                           :grounded-in :experience "exp:reply-width")]
                :time-ms (now)})
            after-shared (adapter/read-master rt attention/spec)]

        (testing "the deviation lands as a revision of an instance master"
          (is (:accepted? d))
          (is (true? (:bootstrap? d)) "the first deviation mints the master")
          (is (= (adapter/instance-master-id attention/spec alpha)
                 (:instance-master-id d))))

        (testing "the wearer resolves the INSTANCE tier"
          (let [w (worn rt alpha)]
            (is (= :instance (:facet-master/tier w)))
            (is (= 24.0 (:attention/hit-padding w)))
            (is (= (:revision-id d) (:facet-master/revision-id w)))))

        (testing "other subjects are untouched"
          (doseq [s [beta gamma delta]]
            (let [w (worn rt s)]
              (is (= :shared (:facet-master/tier w)) (str s))
              (is (= 8.0 (:attention/hit-padding w)) (str s)))))

        (testing "G1 negative reader — NO shared-master row was rewritten"
          ;; the whole point of T1: an instance activation moves an instance
          ;; pointer and nothing else. Read the shared master's rows directly
          ;; rather than asking a product surface that could mask the write.
          (is (= shared-rev
                 (get-in after-shared [:active-revision :revision-id])))
          (is (= (get-in before-shared [:active-pointer :revision-id])
                 (get-in after-shared [:active-pointer :revision-id])))
          (is (= (get-in before-shared [:active-revision :content-text])
                 (get-in after-shared [:active-revision :content-text])))
          (is (= (get-in before-shared [:latest-revision :revision-id])
                 (get-in after-shared [:latest-revision :revision-id]))))

        (testing "the diff vs inherited is served, and is a projection"
          (let [served (face-projection/facet-materials-projection
                        {:oc-rt rt} {:params {:subjects wearers}})
                diff (facet-material/deviation-diff
                      attention/spec
                      (get-in served [:facet-materials/by-id
                                      attention/master-id])
                      (get-in served [:facet-materials/instances
                                      :attention alpha]))]
            (is (= :holds/deviation (:diff/holds diff)))
            (is (= shared-rev (:diff/inherited-revision-id diff)))
            (is (= {:attention/hit-padding {:from 8.0 :to 24.0}}
                   (:diff/changed diff)))))

        (testing "T11 — removing the deviation is a ROLLBACK to the inherited
                  state, never a tombstone and never a deletion"
          (let [r (material-truth/release-deviation!
                   rt attention/spec alpha {:actor sid :time-ms (now)})
                w (worn rt alpha)]
            (is (:accepted? r))
            (is (= :rollback (:kind r)))
            (is (= :shared (:facet-master/tier w)))
            (is (= 8.0 (:attention/hit-padding w)))
            ;; the instance master still EXISTS — its history is intact and
            ;; the subject can deviate again without re-minting anything
            (is (true? (:exists? (adapter/instance-state
                                  rt attention/spec alpha))))))

        (testing "the whole trail reads as events"
          (let [t (adapter/activation-trail
                   rt (adapter/instance-spec attention/spec alpha))
                kinds (mapv #(get-in % [:event :activation/kind]) (:chain t))]
            (is (= [:rollback :activate] kinds))
            (is (every? #(= [:scope/subject alpha]
                            (get-in % [:event :activation/scope]))
                        (:chain t)))
            (is (= :grounds/declared
                   (activation-event/grounds-label
                    (:event (last (:chain t)))))))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G2 — pins
;; ===========================================================================

(deftest g2-pin-survives-a-shared-activation
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [rev1 (boot-attention! rt)
            _ (material-truth/pin! rt attention/spec gamma rev1
                                   {:actor sid :time-ms (now)})
            _ (adapter/ensure-active-source!
               rt attention/spec
               (pr-str (assoc attention/strict-bindings-form
                              :attention/hit-padding 12.0))
               {:request-id "gate-att-v3"
                :activation-request-id "gate-att-activate-v3"
                :time-ms (now)})]

        (testing "the pinned subject does not move; unpinned subjects do"
          (let [p (worn rt gamma)
                u (worn rt beta)]
            (is (= 8.0 (:attention/hit-padding p)))
            (is (true? (:facet-master/pinned? p)))
            (is (= rev1 (:facet-master/revision-id p)))
            (is (= 12.0 (:attention/hit-padding u)))
            (is (false? (:facet-master/pinned? u)))))

        (testing "unpin re-joins the shared master"
          (material-truth/unpin! rt attention/spec gamma
                                 {:actor sid :time-ms (now)})
          (let [w (worn rt gamma)]
            (is (= :shared (:facet-master/tier w)))
            (is (= 12.0 (:attention/hit-padding w)))
            (is (false? (:facet-master/pinned? w)))))

        (testing "T4 — both are recorded events, not a serve-time special case"
          (let [t (adapter/activation-trail
                   rt (adapter/instance-spec attention/spec gamma))]
            (is (= [:unpin :pin]
                   (mapv #(get-in % [:event :activation/kind]) (:chain t))))
            (is (every? #(= "sid" (get-in % [:event :activation/actor
                                             :actor/id]))
                        (:chain t)))))

        (testing "a malformed pinned revision floors — totality, not a crash"
          (let [served (face-projection/facet-materials-projection
                        {:oc-rt rt} {:params {:subjects wearers}})
                shared (get-in served [:facet-materials/by-id
                                       attention/master-id])
                ispec (adapter/instance-spec attention/spec delta)
                iid (adapter/master-id ispec)
                form (facet-material/instance-form
                      attention/spec iid delta
                      {:grammar 2 :material attention/strict-bindings-form
                       :pin {:pinned-revision-id "rev:does-not-exist"}})
                compiled (facet-material/compile-form ispec form)
                broken {:facet-master/id iid
                        :facet-master/subject delta
                        :facet-master/grammar 2
                        :facet-master/active-revision-id "rev:inst:broken"
                        :facet-master/material (:material compiled)
                        :facet-master/pinned
                        {:valid? false :revision-id "rev:does-not-exist"}}
                w (facet-material/wear-for-subject
                   attention/spec shared broken)]
            (is (true? (:valid? compiled))
                "the instance form itself is well-formed; only its TARGET is not")
            (is (= :floor (:facet-master/tier w)))
            (is (true? (:facet-master/floor? w)))
            (is (true? (:facet-master/pin-unresolved? w))))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G4 — scoped activation + blast radius · G5 announcements · G7 epoch
;; ===========================================================================

(deftest g4-g5-g7-blast-radius-announcements-and-epoch
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [rev1 (boot-attention! rt)]
        (material-truth/deviate! rt attention/spec alpha
                                 {:attention/hit-padding 24.0}
                                 {:actor sid :time-ms (now)})
        (material-truth/pin! rt attention/spec gamma rev1
                             {:actor sid :time-ms (now)})

        (testing "G4 — blast radius is the exact wearer set, BEFORE the flip"
          (let [b (material-truth/blast-radius
                   rt attention/spec
                   {:scope (activation-event/all-unpinned-scope)
                    :candidate-wearers wearers})]
            (is (= [beta delta] (:blast/will-move b)))
            (is (= [gamma] (:blast/pinned b)))
            (is (= [alpha] (:blast/deviating b)))
            (is (true? (:blast/includes-future-wearers? b)))))

        (testing "G4 — a subject BORN AFTER the activation wears the new
                  revision without anything being copied to it"
          (let [epoch-before @ingest-epoch/!ingest-epoch-atom
                _ (adapter/ensure-active-source!
                   rt attention/spec
                   (pr-str (assoc attention/strict-bindings-form
                                  :attention/hit-padding 12.0))
                   {:request-id "gate-att-v3b"
                    :activation-request-id "gate-att-activate-v3b"
                    :time-ms (now)})
                epoch-after @ingest-epoch/!ingest-epoch-atom
                newborn "du:block:born-after"]
            (is (= 12.0 (:attention/hit-padding (worn rt newborn)))
                "a subject that did not exist at activation time")
            (is (= :shared (:facet-master/tier (worn rt newborn))))

            (testing "G7/T12 — the activation moved the client-visible epoch"
              (is (> epoch-after epoch-before)))))

        (testing "G5 — one activation announces at three scales, and preview /
                  activation / rollback stay distinguishable"
          (let [a (material-truth/master-announcements
                   rt attention/spec {:affected wearers})
                head (first (:announcements a))]
            (is (contains? material-truth/change-kinds
                           (:announce/change-kind head)))
            (is (= :change/canonical-activation (:announce/change-kind head)))
            (is (seq (get-in head [:announce/breath :breath/subjects])))
            (is (= [:scope/all-unpinned]
                   (get-in head [:announce/trace :trace/scope])))
            (is (some? (get-in head [:announce/trace :trace/reversal
                                     :reversal/to-revision-id]))
                "the trace always names a reversal path")
            (is (some? (get-in head [:announce/weather :weather/change-kind])))
            ;; the vocabulary can SAY preview, and the server never mints one
            (is (contains? material-truth/change-kinds :change/preview))
            (is (not-any? #(= :change/preview (:announce/change-kind %))
                          (:announcements a))))

          (testing "an instance activation announces as SCOPED, not canonical"
            (let [a (material-truth/master-announcements
                     rt (adapter/instance-spec attention/spec gamma)
                     {:affected [gamma]})]
              (is (= #{:change/pin}
                     (set (map :announce/change-kind
                               (:announcements a))))))))

        (testing "a re-wear of a previously worn revision is a RECOVERY"
          (adapter/activate! rt attention/spec rev1
                             {:request/id "gate-recover"
                              :time-ms (now)
                              :actor sid})
          (let [a (material-truth/master-announcements
                   rt attention/spec {:affected wearers})]
            (is (= :change/recovery
                   (:announce/change-kind (first (:announcements a))))))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G8 — the trail reads DECLARED kinds; v0 history still renders
;; ===========================================================================

(deftest g8-event-truth-trail
  (testing "a v0 bare-string pointer source parses as an honest unknown"
    (let [e (activation-event/parse "rev:fm:attention:abc:def")]
      (is (= "rev:fm:attention:abc:def" (:activation/revision-id e)))
      (is (= :activate (:activation/kind e)))
      (is (true? (:activation/v0? e)))
      (is (= :grounds/unknown (activation-event/grounds-label e)))
      (is (nil? (:activation/time-ms e))
          "a v0 row never learns a time it did not record")))

  (testing "unknown grounds and NO grounds are different facts"
    (let [declared (activation-event/parse
                    (activation-event/source-for
                     (activation-event/event
                      {:revision-id "r" :kind :activate :time-ms 5
                       :grounds [(activation-event/ground
                                  :responds-to :conflict "c:1")]})))
          ungrounded (activation-event/parse
                      (activation-event/source-for
                       (activation-event/event
                        {:revision-id "r" :kind :activate :time-ms 5})))]
      (is (= :grounds/declared (activation-event/grounds-label declared)))
      (is (= :grounds/ungrounded (activation-event/grounds-label ungrounded)))
      (is (= :grounds/unknown
             (activation-event/grounds-label
              (activation-event/parse "rev:bare"))))))

  (testing "T8 — the event form is CLOSED; a hostile form cannot pass"
    (is (false? (activation-event/valid-event? {})))
    (is (false? (activation-event/valid-event?
                 {:activation/revision-id "r" :activation/kind :bogus
                  :activation/scope [:scope/all-unpinned]
                  :activation/actor {:actor/id "a" :actor/type :human}
                  :activation/time-ms 1 :activation/grounds []})))
    (is (false? (activation-event/valid-event?
                 (assoc (activation-event/event
                         {:revision-id "r" :kind :activate :time-ms 1})
                        :activation/grounds [{:ground/relation :invented
                                              :ground/kind :experience
                                              :ground/id "x"}]))))
    (is (false? (activation-event/valid-event?
                 (assoc (activation-event/event
                         {:revision-id "r" :kind :activate :time-ms 1})
                        :activation/extra :smuggled)))
        "an extra key is refused — closed means closed")
    (testing "totality: a corrupt source degrades, it never throws"
      (is (= "rev:fallback"
             (:activation/revision-id
              (activation-event/parse "{:activation/kind" "rev:fallback"))))))

  (testing "T2 — durable bytes are stable regardless of the printer's flags"
    (let [e (activation-event/event {:revision-id "r" :kind :activate
                                     :time-ms 7})]
      (is (= (activation-event/source-for e)
             (binding [*print-namespace-maps* true]
               (activation-event/source-for e)))
          "import identity is content-hash keyed; REPL and server must agree")))

  (testing "clock regressions are EXPOSED, never sorted away"
    (is (= 1 (count (activation-event/trail-regressions
                     [{:activation/revision-id "a"
                       :activation/time-ms 1753400000002}
                      {:activation/revision-id "b" :activation/time-ms 1}]))))))

(deftest g8-declared-kinds-over-REAL-durable-pointer-rows
  "G8's classification, proven over the actual rows a cluster holds — both
   generations at once. The inspector's INTEGRATION of this is proven in
   `app.server.page.material-inspector-test`, which drives a real durable entity; what is
   proven here is that the classification is right about real bytes."
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [_ (boot-attention! rt)
            _ (adapter/activate!
               rt attention/spec
               (get-in (adapter/read-master rt attention/spec)
                       [:latest-revision :revision-id])
               {:request/id "g8-rollback" :time-ms (now) :actor sid
                :activation/kind :rollback
                :activation/grounds [(activation-event/ground
                                      :responds-to :conflict "c:g8")]})
            rows (ocr/read-revision-history
                  rt (adapter/active-pointer-container-id attention/spec)
                  "" 1000)
            parsed (mapv #(activation-event/parse (:content-text %)) rows)
            v0 (filterv :activation/v0? parsed)
            p6 (filterv (complement :activation/v0?) parsed)]

        (testing "both generations of pointer source live in one container"
          (is (seq v0) "P1's bootstrap pointer is a bare revision-id string")
          (is (seq p6) "P6's activations are declared event forms"))

        (testing "v0 rows render, labeled — never reinterpreted"
          (is (every? #(= :activate (:activation/kind %)) v0))
          (is (every? #(= :grounds/unknown
                          (activation-event/grounds-label %)) v0))
          (is (every? #(nil? (:activation/time-ms %)) v0)))

        (testing "P6 rows carry DECLARED kinds, not pointer-shape guesses"
          (is (contains? (set (map :activation/kind p6)) :rollback))
          (is (every? #(contains? activation-event/kinds
                                  (:activation/kind %)) p6))
          (is (every? #(integer? (:activation/time-ms %)) p6))
          (is (some #(= :grounds/declared (activation-event/grounds-label %))
                    p6)))

        (testing "every parsed row still names the revision that is worn"
          (is (every? #(string? (:activation/revision-id %)) parsed))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G9 — T10 required args
;; ===========================================================================

(deftest g9-required-args-refuses-an-arg-starved-rebind
  (let [probe {:binding/gesture :pointer/tap
               :binding/phase :complete
               :binding/modifiers :any
               :binding/verb {:verb/name :fold/toggle-section
                              :verb/version 0}
               :binding/priority 10}]
    (testing "the P5 gate's exact probe row is the regression fixture"
      (is (true? (binding-material/valid-row? probe true))
          "shape alone is legal — that is why it shipped")
      (is (false? (binding-material/valid-row?
                   probe true :block/user-hit-area))
          "the site cannot feed :fold-key, so the row is refused")
      (is (true? (binding-material/valid-row?
                  probe true :block/fold-header))
          "the header's claim carries the section"))

    (testing "the strict (v2) grammar refuses it; v1 is NOT reinterpreted"
      (is (false? (binding-material/valid-bindings-strict?
                   {:block/user-hit-area [probe]})))
      (is (true? (binding-material/valid-bindings?
                  {:block/user-hit-area [probe]}))
          "a durable v1 revision keeps its original meaning forever"))

    (testing "every shipped row still validates under v2"
      (doseq [[label form] [["attention" attention/strict-bindings-form]
                            ["foldable" foldable/strict-bindings-form]
                            ["positioned" positioned/strict-bindings-form]]]
        (is (true? (binding-material/valid-bindings-strict?
                    (:facet-master/bindings form)))
            label))
      (is (true? (binding-material/valid-bindings-strict?
                  binding-material/space-floor-bindings false))))

    (testing "the registry declares required args on EVERY verb"
      (is (true? (verb-registry/well-formed-registry?)))
      (is (= #{:fold-key} (verb-registry/required-args :fold/toggle-section)))
      (is (every? #(set? (verb-registry/required-args %))
                  verb-registry/names)))

    (testing "the twenty shipped table rows are unchanged in count"
      (let [rows (binding-material/table-rows
                  (concat
                   (for [f [attention/strict-bindings-form
                            foldable/strict-bindings-form
                            positioned/strict-bindings-form]]
                     {:tier :master :facet (:facet-master/facet f)
                      :master-id (:facet-master/id f) :revision-id "r"
                      :floor? false :bindings (:facet-master/bindings f)})
                   (for [spec facet-masters/specs
                         :let [b (:facet-master/bindings
                                  (facet-material/code-floor spec))]
                         :when (seq b)]
                     {:tier :floor :facet (:facet-master/facet spec)
                      :master-id (facet-material/floor-master-id spec)
                      :revision-id (facet-material/floor-master-id spec)
                      :floor? true :bindings b})
                   [{:tier :floor :facet binding-material/space-facet
                     :master-id binding-material/space-floor-master-id
                     :revision-id binding-material/space-floor-master-id
                     :floor? true
                     :bindings binding-material/space-floor-bindings}]))]
        (is (= 21 (count rows))
            "P8 adds exactly one eval → reply row to the twenty shipped rows")
        (is (empty? (binding-material/table-conflicts rows)))))))

;; ===========================================================================
;; G10 / rung 3 — the space alone gains an instance tier
;; ===========================================================================

(deftest g10-r3-owner-aware-instance-sites
  ;; T-R6 — the old `space has no instance tier` pin moves with the lift.
  (testing "one arity retains the old block-only fail-closed meaning"
    (is (false? (binding-material/instance-site-legal? :space/ground)))
    (is (every? binding-material/instance-site-legal?
                [:block/user-hit-area :block/machine-hit-area
                 :block/fold-header])))
  (testing "two arities open ground only for the space owner (T-R3)"
    (is (true? (binding-material/instance-site-legal?
                :space/ground :space)))
    (is (false? (binding-material/instance-site-legal?
                 :space/ground :attention)))
    (is (false? (binding-material/instance-site-legal?
                 :space/ground "space")))
    (is (every? #(binding-material/instance-site-legal?
                  % :attention)
                [:block/user-hit-area :block/machine-hit-area
                 :block/fold-header])))
  (testing "the refusal-card enumeration remains the block-only set"
    (is (= #{:space/ground}
           (clojure.set/difference binding-material/sites
                                   binding-material/instance-legal-sites))))
  (testing "the camera stays floor-reserved on top of the refusal"
    (is (false? (verb-registry/bindable? :camera/pan 0)))
    (is (false? (verb-registry/bindable? :camera/zoom-at-pointer 0)))))

(def r3-camera-row
  {:binding/gesture :wheel
   :binding/phase :complete
   :binding/modifiers #{}
   :binding/verb {:verb/name :camera/zoom-at-pointer :verb/version 0}
   :binding/priority 10})

(def r3-space-site-row
  {:binding/gesture :pointer/tap
   :binding/phase :complete
   :binding/modifiers #{}
   :binding/verb {:verb/name :anchor/place :verb/version 0}
   :binding/priority 10})

(defn- instance-history
  [rt parent-spec subject]
  (let [ispec (adapter/instance-spec parent-spec subject)]
    {:revisions
     (ocr/read-revision-history rt (adapter/document-id ispec) "" 100)
     :pointers
     (ocr/read-revision-history
      rt (adapter/active-pointer-container-id ispec) "" 100)}))

(deftest r3-g5-write-lane-refusals-append-nothing
  (let [rt (ocr/start-object-container-runtime!)
        camera-subject "space-g5-camera"
        foreign-subject "attention-g5-space-site"
        clamp-subject "space-g5-clamp"]
    (try
      ;; G5a's candidate must inherit grammar v1, whose material key set carries
      ;; bindings. The v0 code floor legitimately drops a binding override before
      ;; validation because bindings did not exist in that frozen grammar.
      (adapter/ensure-master! rt space/spec)
      (let [camera
            (material-truth/deviate!
             rt space/spec camera-subject
             {:facet-master/bindings {:space/ground [r3-camera-row]}}
             {:actor sid :time-ms (now)
              :request-id (str "r3-g5a-" (random-uuid))})
            foreign
            (material-truth/deviate!
             rt attention/spec foreign-subject
             {:facet-master/bindings {:space/ground [r3-space-site-row]}}
             {:actor sid :time-ms (now)
              :request-id (str "r3-g5b-" (random-uuid))})
            clamp
            (material-truth/deviate!
             rt space/spec clamp-subject
             {:space/zoom-min 5.0 :space/zoom-max 4.0}
             {:actor sid :time-ms (now)
              :request-id (str "r3-g5c-" (random-uuid))})]
        (testing "R3-G5a/T-R4 — the inherited fm:space grammar owns the fence"
          (is (false? (:accepted? camera)))
          (is (= :facet-master/instance-form-invalid (:reason camera)))
          (is (= [:facet-master/bindings-invalid]
                 (mapv :type (:errors camera)))))
        (testing "R3-G5b/T-R3 — a non-space parent cannot mint dead space rows"
          (is (false? (:accepted? foreign)))
          (is (= :facet-master/instance-site-refused (:reason foreign)))
          (is (= [:space/ground] (:sites foreign))))
        (testing "R3-G5c — instance grammars inherit the whole-form seam"
          (is (false? (:accepted? clamp)))
          (is (= :facet-master/instance-form-invalid (:reason clamp)))
          (is (= [:space/zoom-clamp-invalid]
                 (mapv :type (:errors clamp)))))
        (testing "physical OC histories prove every refusal preceded all appends"
          (doseq [[spec subject]
                  [[space/spec camera-subject]
                   [attention/spec foreign-subject]
                   [space/spec clamp-subject]]]
            (is (= {:revisions [] :pointers []}
                   (instance-history rt spec subject))
                subject))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G11 — honest times
;; ===========================================================================

(def deterministic-time-baseline
  "The GRANDFATHERED deterministic `:time-ms` literals in src, by file.

   These are durable history: P1 minted v0 at 0, P3 minted provenance v1 at 1,
   P5 minted the bindings v1 migration at 2. Rewriting them would be a data
   migration (a stop clause), and they are exactly what makes the activation
   history non-monotone in clock terms — which is why history is CAUSAL.

   R3/G11 forbids a FOURTH. This baseline is a fail-closed fence: a new
   deterministic constant anywhere in a production path fails this gate and has
   to be argued for explicitly."
  {"src/app/server/episode/episode.clj" 1
   "src/app/server/door/cluster.clj" 3
   "src/app/server/worn/facet_master.clj" 2})

(deftest g11-no-new-deterministic-time-in-a-production-write-path
  (let [hits (->> (file-seq (io/file "src"))
                  (filter #(.isFile ^java.io.File %))
                  (filter #(re-find #"\.clj[cs]?$" (.getName ^java.io.File %)))
                  (remove #(str/ends-with? (str %) "env.clj"))
                  (mapcat
                   (fn [f]
                     (keep (fn [line]
                             (when (re-find #":time-ms\s+\d" line)
                               (str/replace (str f) "\\" "/")))
                           (str/split-lines (slurp f)))))
                  frequencies)]
    (testing "no NEW deterministic activation time joined the grandfathered set"
      (is (= deterministic-time-baseline hits)
          (str "deterministic :time-ms literals moved. baseline="
               (pr-str deterministic-time-baseline)
               " actual=" (pr-str hits))))
    (testing "P6's own write paths take an honest clock"
      ;; every P6 entry point defaults `time-ms` to `core/now-ms`; passing one
      ;; explicitly is what tests do, and that is the only sanctioned route
      (is (nil? (get hits "src/app/server/worn/material_truth.clj"))))))

(deftest g11-causal-as-of-is-unchanged-by-honest-times
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [rev1 (boot-attention! rt)
            _ (adapter/ensure-active-source!
               rt attention/spec
               (pr-str (assoc attention/strict-bindings-form
                              :attention/hit-padding 12.0))
               {:request-id "gate-att-asof"
                :activation-request-id "gate-att-activate-asof"
                :time-ms (now)})
            t (adapter/activation-trail rt attention/spec)
            root (last (:chain t))
            at (adapter/worn-at rt attention/spec
                                (:pointer-revision-id root))]
        (testing "standable history stands at a CAUSAL cut, not a timestamp"
          (is (true? (:found? at)))
          (is (= 2 (count (:since at))))
          (is (true? (get-in at [:event :activation/v0?]))))
        (testing "the trail's own order is the parent chain"
          (is (= (mapv :pointer-revision-id (:chain t))
                 (mapv :pointer-revision-id (:chain t))))
          (is (every? some? (map :pointer-revision-id (:chain t)))))
        (testing "the world at a cut is projectable for every master"
          (let [w (material-truth/world-at
                   rt {attention/master-id (:pointer-revision-id root)})]
            (is (= (count facet-masters/specs)
                   (count (:history/masters w))))
            (is (= (:worn-revision-id at)
                   (get-in w [:history/masters attention/master-id
                              :worn-revision-id])))))
        (testing "the current cut names the revision actually worn"
          (let [w (material-truth/world-at rt {})]
            (is (not= rev1
                      (get-in w [:history/masters attention/master-id
                                 :worn-revision-id]))))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G12 — immunity: the malformed drill re-runs because the kernel changed
;; ===========================================================================

(deftest g12-malformed-drill-still-holds-after-the-kernel-change
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [_ (boot-attention! rt)
            before (adapter/read-master rt attention/spec)
            drill (adapter/malformed-drill! rt attention/spec "p6-immunity")
            after (adapter/read-master rt attention/spec)]
        (testing "the rejected candidate is RETAINED as latest"
          (is (some? (:candidate-revision-id drill)))
          (is (= (:candidate-revision-id drill)
                 (get-in after [:latest-revision :revision-id])))
          (is (seq (:activation-errors drill))))
        (testing "the active revision is byte-untouched — latest ≠ active"
          (is (= (get-in before [:active-revision :revision-id])
                 (get-in after [:active-revision :revision-id])))
          (is (= (get-in before [:active-revision :content-text])
                 (get-in after [:active-revision :content-text])))
          (is (= (get-in before [:active-pointer :revision-id])
                 (get-in after [:active-pointer :revision-id]))))
        (testing "a previous revision can be re-worn from inside the land"
          (let [original (get-in before [:active-revision :revision-id])
                back (adapter/activate!
                      rt attention/spec original
                      {:request/id "p6-immune-rewear"
                       :time-ms (now)
                       :actor sid
                       :activation/kind :rollback
                       :activation/grounds
                       [(activation-event/ground
                         :responds-to :conflict "drill:p6-immunity")]})]
            (is (:accepted? back))
            (is (= original
                   (get-in (adapter/read-master rt attention/spec)
                           [:active-revision :revision-id])))
            (is (= :rollback (get-in back [:event :activation/kind])))))
        (testing "the wound is standable in history"
          (let [c (material-truth/case-report rt attention/spec {})]
            (is (seq (:case/claims c)))
            (is (= "drill:p6-immunity"
                   (-> c :case/claims first :claim/grounds first :ground/id))))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G13 — the registry index is rebuildable (T7)
;; ===========================================================================

(deftest g13-registry-rebuilds-from-container-existence
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [rev1 (boot-attention! rt)
            object-key (episode/episode-object-key
                        episode/genesis-conversation-id)]
        (material-truth/deviate! rt attention/spec alpha
                                 {:attention/hit-padding 24.0}
                                 {:actor sid :time-ms (now)})
        (material-truth/pin! rt attention/spec gamma rev1
                             {:actor sid :time-ms (now)})
        (let [indexed (episode/read-instance-registry rt object-key)
              rebuilt (material-truth/rebuild-instance-registry!
                       rt {:subjects wearers :time-ms (now)})
              after (episode/read-instance-registry rt object-key)]
          (testing "the index lists exactly the deviating subjects"
            (is (= #{alpha gamma} (set (map :subject indexed)))))
          (testing "rebuilding from container existence is byte-equal"
            (is (= indexed after))
            (is (= (set (map :instance-master-id indexed))
                   (set (map :instance-master-id (:rebuilt rebuilt))))))
          (testing "T7 — the index is NOT the truth: wear resolves without it"
            ;; the serve is handed the subjects directly, as if the index had
            ;; never been written; container existence still answers
            (let [served (face-projection/facet-materials-projection
                          {:oc-rt rt} {:params {:subjects wearers}})]
              (is (some? (get-in served [:facet-materials/instances
                                         :attention alpha])))
              (is (= 24.0 (:attention/hit-padding (worn rt alpha))))))))
      (finally (ocr/close-object-container-runtime! rt)))))

;; ===========================================================================
;; G14 — floor-label parity, client tiers vs served projection
;; ===========================================================================

(deftest g14-floor-label-parity
  (testing "one map answers for both sides"
    (doseq [spec facet-masters/specs]
      (is (= (facet-material/floor-master-id spec)
             (facet-masters/floor-master-id (:facet-master/facet spec)))
          (:facet-master/id spec))))
  (testing "the space's floor label is named once and read from there"
    (is (= binding-material/space-floor-master-id
           (facet-masters/floor-master-id binding-material/space-facet)))
    (is (= space/code-floor-revision-id
           (facet-masters/floor-master-id :space))))
  (testing "every floor row's master-id and revision-id agree"
    (doseq [spec facet-masters/specs
            :let [label (facet-material/floor-master-id spec)]]
      (is (string? label))
      (is (str/starts-with? label "code-floor:"))))
  (testing "P8 moves attention alone to v3; the other v2 floors stand"
    (is (= "code-floor:fm:attention:v3"
           (facet-masters/floor-master-id :attention)))
    (is (= "code-floor:fm:foldable:v3"
           (facet-masters/floor-master-id :foldable)))
    (is (= "code-floor:fm:positioned:v2"
           (facet-masters/floor-master-id :positioned)))
    (is (= "code-floor:fm:space:v0"
           (facet-masters/floor-master-id :space)))))

;; ===========================================================================
;; Routing — the id-shape finding that made R1 hold (Phase 0)
;; ===========================================================================

(deftest instance-master-ids-route-to-ONE-partition
  (testing "every derived id of an instance master hashes to one object key"
    (doseq [spec facet-masters/specs
            subject [alpha "du:x" "chat:deadbeef:9"]]
      (let [ispec (adapter/instance-spec spec subject)
            iid (adapter/master-id ispec)
            keys* [iid
                   (adapter/document-id ispec)
                   (adapter/active-pointer-container-id ispec)
                   (oc/source-id-for-object-key iid)
                   (adapter/candidate-revision-id ispec "{}")
                   (adapter/import-key ispec (oc/source-hash "{}"))]]
        (is (= 1 (count (set (map oc/extract-object-key keys*))))
            (str "instance id " iid " must not straddle two Rama partitions"))
        (is (= 2 (count (str/split iid #":")))
            (str iid " must stay TWO colon segments — `oc:doc:` returns the "
                 "whole remainder while every sibling branch collapses an "
                 "`fm:` key to its first two segments")))))

  (testing "a three-segment id WOULD straddle — the finding, pinned"
    (let [bad "fm:attention:i:abc12345"]
      (is (not= (oc/extract-object-key (oc/document-id-for-object-key bad))
                (oc/extract-object-key
                 (oc/block-container-id bad "active-pointer")))
          "if this ever becomes equal the routing kernel changed; revisit")))

  (testing "an instance master never enters the served spec registry"
    (doseq [spec facet-masters/specs]
      (is (nil? (facet-masters/spec
                 (adapter/instance-master-id spec alpha)))))))

;; ===========================================================================
;; Gate P6 findings F1 + F2, pinned as regressions
;; ===========================================================================

(deftest f1-f2-transition-identity-and-durable-site-refusal
  (let [rt (ocr/start-object-container-runtime!)]
    (try
      (let [rev1 (boot-attention! rt)]
        (testing "F1 — the SECOND byte-identical transition still moves the
                  pointer: pin → unpin → re-pin → unpin2. A content-keyed
                  activation request-id replays the FIRST unpin's decision
                  forever (the machine-cut stable-per-transition class); the
                  id must be keyed to the transition — content plus the
                  pointer revision it moves from."
          (material-truth/pin! rt attention/spec gamma rev1
                               {:actor sid :time-ms (now)})
          (material-truth/unpin! rt attention/spec gamma
                                 {:actor sid :time-ms (now)})
          (material-truth/pin! rt attention/spec gamma rev1
                               {:actor sid :time-ms (now)})
          (is (true? (:facet-master/pinned? (worn rt gamma))))
          (let [u2 (material-truth/unpin! rt attention/spec gamma
                                          {:actor sid :time-ms (now)})]
            (is (true? (:accepted? u2)))
            (is (false? (:facet-master/pinned? (worn rt gamma)))
                "the second unpin must not be an idempotency replay")))

        (testing "F1 — same class on the deviation axis:
                  deviate → release → re-deviate → release2"
          (material-truth/deviate! rt attention/spec alpha
                                   {:attention/hit-padding 24.0}
                                   {:actor sid :time-ms (now)})
          (material-truth/release-deviation! rt attention/spec alpha
                                             {:actor sid :time-ms (now)})
          (material-truth/deviate! rt attention/spec alpha
                                   {:attention/hit-padding 24.0}
                                   {:actor sid :time-ms (now)})
          (is (= :instance (:facet-master/tier (worn rt alpha))))
          (let [r2 (material-truth/release-deviation!
                    rt attention/spec alpha {:actor sid :time-ms (now)})]
            (is (true? (:accepted? r2)))
            (is (= :shared (:facet-master/tier (worn rt alpha)))
                "the second release must not be an idempotency replay")))

        (testing "F2 — the DURABLE instance lane refuses :space/ground at
                  WRITE time (G10), not merely at client consumption"
          (let [row (-> attention/strict-bindings-form
                        (get-in [:facet-master/bindings :block/user-hit-area])
                        first)
                d (material-truth/deviate!
                   rt attention/spec beta
                   {:facet-master/bindings {:space/ground [row]}}
                   {:actor sid :time-ms (now)})]
            (is (false? (:accepted? d)))
            (is (= :facet-master/instance-site-refused (:reason d)))
            (is (= [:space/ground] (:sites d)))
            (is (nil? (get-in (face-projection/facet-materials-projection
                               {:oc-rt rt} {:params {:subjects wearers}})
                              [:facet-materials/instances :attention beta]))
                "nothing durable landed")
            (is (= :shared (:facet-master/tier (worn rt beta)))))))
      (finally (ocr/close-object-container-runtime! rt)))))
