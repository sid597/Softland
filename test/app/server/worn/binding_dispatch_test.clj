(ns app.server.worn.binding-dispatch-test
  "editable-material P5 — the one dispatch law, the closed row grammar, and the
   code floor's unbreakability, proven where they live: as pure data.

   These tests are the package's per-family proof. Each family's pre-P5 branch
   is named in the test that replaces it, so a reader can check the migration
   rather than trust it. The floor drill is the fence's receipt: `binding-material
   /drill-report` is the SAME function the live console drill calls, so a green
   test and a green browser drill are the same claim."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [app.server.worn.attention-material :as attention]
            [app.server.worn.binding-material :as bm]
            [app.server.worn.facet-engine :as facet-engine]
            [app.server.worn.facet-masters :as facet-masters]
            [app.server.worn.foldable-material :as foldable]
            [app.server.worn.positioned-material :as positioned]
            [app.server.page.verb-registry :as verbs]))

;; ===========================================================================
;; Fixtures — the tiers exactly as the kernel assembles them
;; ===========================================================================

(def ^:private floor-rows
  "The kernel's `floor-binding-rows`, rebuilt from the same specs."
  (bm/with-halo-floor-bindings
   (assoc
    (into {}
          (map (fn [spec]
                 [(:facet-master/facet spec)
                  (:facet-master/bindings (facet-engine/code-floor spec))]))
          facet-masters/specs)
    bm/space-facet bm/space-floor-bindings)))

(def ^:private master-rows
  "The MASTER tier when every facet serves its bindings revision."
  (into {}
        (map (fn [[facet form]]
               [facet (:facet-master/bindings form)]))
        {:attention attention/bindings-form
         :foldable foldable/bindings-form
         :positioned positioned/bindings-form}))

(def ^:private subject "du:block-1")

(defn- block-claim
  [site]
  [{:claim/subject subject
    :claim/site site
    :claim/facets bm/block-claim-facets
    :claim/args {}}])

(defn- header-claim
  [fold-key]
  [{:claim/subject subject
    :claim/site :block/fold-header
    :claim/facets [:foldable]
    :claim/args {:section (if (= :noise? fold-key) :noise :prose)
                 :fold-key fold-key}}
   {:claim/subject subject
    :claim/site :block/machine-hit-area
    :claim/facets bm/block-claim-facets
    :claim/args {}}])

(defn- resolve*
  [kind phase modifiers claims & {:keys [facet-rows instance-rows]}]
  (bm/resolve-binding
   {:gesture (bm/normalize-gesture kind phase modifiers)
    :claims claims
    :facet-rows (if (contains? (set (keys (or facet-rows {}))) ::none)
                  {}
                  (or facet-rows master-rows))
    :floor-rows floor-rows
    :instance-rows (or instance-rows {})}))

(defn- verb-of [decision] (get-in decision [:decision/verb :verb/name]))

;; ===========================================================================
;; The closed grammar
;; ===========================================================================

(deftest the-row-grammar-is-closed-and-cannot-name-code
  (let [ok {:binding/gesture :pointer/tap
            :binding/phase :complete
            :binding/modifiers #{}
            :binding/verb {:verb/name :focus/place-caret :verb/version 0}
            :binding/priority 10}]
    (testing "a well-formed row passes"
      (is (bm/valid-row? ok))
      (is (bm/valid-row? (assoc ok :binding/modifiers :any))))

    (testing "material may name ONLY a registered verb at its declared version"
      (is (not (bm/valid-row?
                (assoc ok :binding/verb
                       {:verb/name :rm/-rf :verb/version 0}))))
      (is (not (bm/valid-row?
                (assoc ok :binding/verb
                       {:verb/name :focus/place-caret :verb/version 7}))))
      (is (not (bm/valid-row?
                (assoc ok :binding/verb :focus/place-caret)))
          "a bare keyword is not a versioned verb reference"))

    (testing "floor-reserved verbs are refused to MATERIAL, allowed to the floor"
      (let [pan (assoc ok :binding/verb
                       {:verb/name :camera/pan :verb/version 0}
                       :binding/gesture :pointer/press
                       :binding/phase :threshold)]
        (is (not (bm/valid-row? pan true)) "material cannot bind the camera")
        (is (bm/valid-row? pan false) "the code floor is where it lives")))

    (testing "gesture, phase, modifiers and priority are all enumerated"
      (is (not (bm/valid-row? (assoc ok :binding/gesture :pointer/triple))))
      (is (not (bm/valid-row? (assoc ok :binding/phase :whenever))))
      (is (not (bm/valid-row? (assoc ok :binding/gesture :pointer/tap
                                     :binding/phase :begin)))
          "an unproducible (kind, phase) pair is refused, not silently dead")
      (is (not (bm/valid-row? (assoc ok :binding/modifiers #{:hyper})))
          "a modifier the kernel never normalizes could never fire")
      (is (not (bm/valid-row? (assoc ok :binding/priority "10")))))

    (testing "exactly five keys — no smuggling extra fields"
      (is (not (bm/valid-row? (assoc ok :binding/when-i-feel-like-it true))))
      (is (not (bm/valid-row? (dissoc ok :binding/priority)))))

    (testing "a whole bindings map is total and refuses duplicate rows"
      (is (bm/valid-bindings? {:block/user-hit-area [ok]}))
      (is (not (bm/valid-bindings? {:block/nowhere [ok]})))
      (is (not (bm/valid-bindings? {:block/user-hit-area [ok ok]}))
          "the same gesture twice in one facet at one site is an error, not lint")
      (is (not (bm/valid-bindings? nil)))
      (is (not (bm/valid-bindings? {:block/user-hit-area []})))
      (is (not (bm/valid-bindings? "{:block/user-hit-area []}"))))))

(deftest the-verb-registry-is-well-formed-and-fully-extracted
  (is (verbs/well-formed-registry?))
  (testing "every verb points at the pre-P5 branch it was lifted from"
    (doseq [row (verbs/declaration-rows)]
      (is (seq (:verb/extracted-from row))
          (str (:verb/name row) " must name existing behavior"))))
  (testing "effect classes are declared, and the external class is empty"
    (is (= #{:pure-projection :durable-via-request}
           (set (map :verb/effect-class (verbs/declaration-rows))))
        "P5 extracts no external-via-derived-worker verb; the class exists so
         the grammar can SAY it before a verb needs it"))
  (testing "the two camera verbs plus Halo condense are the reserved floor"
    (is (= [:camera/pan :camera/zoom-at-pointer :halo/condense]
           (->> (verbs/declaration-rows)
                (filter :verb/floor-reserved?)
                (mapv :verb/name)))))
  (testing "a durable-via-request verb arms exactly the settle or named act lane"
    (is (= #{:camera/pan :camera/zoom-at-pointer :placement/drag-group
             :resident/reply-to-block
             :matter/deviate :matter/activate :matter/rollback :matter/say}
           (->> (verbs/declaration-rows)
                (filter #(= :durable-via-request (:verb/effect-class %)))
                (map :verb/name)
                set)))))

(deftest matter-verbs-declare-the-honest-unbindable-surface
  (let [rows (into {} (map (juxt :verb/name identity))
                   (verbs/declaration-rows))
        matter-names #{:matter/deviate :matter/preview :matter/say
                       :matter/activate :matter/rollback}
        row-for (fn [verb]
                  {:binding/gesture :pointer/tap
                   :binding/phase :complete
                   :binding/modifiers :any
                   :binding/verb {:verb/name verb :verb/version 0}
                   :binding/priority 1})]
    (testing "declarations name their effects, required args, and act lanes"
      (is (= matter-names
             (set (filter #(= "matter" (namespace %)) (keys rows)))))
      (is (= #{:master-id :subject-uid}
             (set (:verb/required-args (get rows :matter/deviate)))))
      (is (= #{:master-id :subject-uid}
             (set (:verb/required-args (get rows :matter/say)))))
      (doseq [verb [:matter/preview :matter/activate :matter/rollback]]
        (is (= #{:master-id}
               (set (:verb/required-args (get rows verb))))))
      (is (= :pure-projection
             (:verb/effect-class (get rows :matter/preview))))
      (doseq [verb [:matter/deviate :matter/say
                    :matter/activate :matter/rollback]]
        (is (= :durable-via-request
               (:verb/effect-class (get rows verb)))))
      (is (every? :verb/bindable? (map rows matter-names))
          "bindable? means registered/non-floor; required args make the
           current sites structurally unable to feed these verbs"))

    (testing "the refusal's honest scope: v2+ refuses, frozen v1 accepts"
      (doseq [verb matter-names
              :let [bindings {:block/user-hit-area [(row-for verb)]}]]
        (is (bm/valid-bindings? bindings)
            (str verb " remains readable under frozen grammar v1"))
        (is (not (bm/valid-bindings-strict? bindings))
            (str verb " is refused by strict v2+ because no site supplies"
                 " its matter args"))))))

(deftest halo-meta-is-universally-reserved-and-floored-at-four-sites
  (let [material-row
        {:binding/gesture :pointer/meta
         :binding/phase :complete
         :binding/modifiers :any
         :binding/verb {:verb/name :focus/release :verb/version 0}
         :binding/priority 1}
        meta-floor-rows
        (for [[facet by-site] floor-rows
              [site rows] by-site
              row rows
              :when (= :pointer/meta (:binding/gesture row))]
          {:facet facet :site site :row row})]
    (testing "the one predicate rejects every material site under both frozen
              and strict validators, while code-floor validation stays legal"
      (doseq [site bm/sites]
        (is (bm/meta-gesture-reserved? site material-row))
        (is (not (bm/valid-row? material-row true site)))
        (is (bm/valid-row? material-row false site))
        (is (not (bm/valid-bindings? {site [material-row]})))
        (is (not (bm/valid-bindings-strict? {site [material-row]})))))

    (testing "the shared augmenter owns exactly four rows and is idempotent"
      (is (= 4 (count meta-floor-rows)))
      (is (= #{[:attention :block/user-hit-area]
               [:attention :block/machine-hit-area]
               [:foldable :block/fold-header]
               [:space :space/ground]}
             (set (map (juxt :facet :site) meta-floor-rows))))
      (is (= floor-rows (bm/with-halo-floor-bindings floor-rows)))
      (is (every? #(= :halo/condense
                      (get-in % [:row :binding/verb :verb/name]))
                  meta-floor-rows))
      (is (every? #(= :any (get-in % [:row :binding/modifiers]))
                  meta-floor-rows)))

    (testing "each claim site, including shift-meta, resolves to the one floor verb"
      (doseq [[claims]
              [[(into (block-claim :block/user-hit-area) bm/space-claim)]
               [(into (block-claim :block/machine-hit-area) bm/space-claim)]
               [(into (header-claim :noise?) bm/space-claim)]
               [bm/space-claim]]
              modifiers [#{} #{:shift}]]
        (let [decision (resolve* :pointer/meta :complete modifiers claims)]
          (is (= :halo/condense (verb-of decision)))
          (is (= :floor (:decision/tier decision))))))))

(deftest p8-eval-addresses-one-block-through-one-material-row
  (let [reply-master-rows
        (assoc master-rows
               :attention
               (:facet-master/bindings attention/reply-bindings-form))
        pre-rung-1
        (resolve* :key/eval :complete #{} (block-claim :block/user-hit-area)
                  :facet-rows reply-master-rows)
        d (resolve* :key/eval :complete #{}
                    (into (block-claim :block/user-hit-area) bm/space-claim)
                    :facet-rows reply-master-rows)]
    (is (= pre-rung-1 d)
        "G10g: appending the outer space rung leaves key/eval resolution exact")
    (is (= :claimed (:decision/outcome d)))
    (is (= subject (:decision/subject d))
        "the claim's subject is the addressed block; no second target verb")
    (is (= :resident/reply-to-block (verb-of d)))
    (is (= 1 (get-in d [:decision/verb :verb/version])))
    (is (= :master (:decision/tier d)))
    (is (= :attention (:decision/facet d)))
    (is (= attention/reply-binding-row (:decision/row d)))))

;; ===========================================================================
;; Family 1 — fold-header click
;; ===========================================================================

(deftest family-1-fold-header-tap-resolves-to-foldables-row
  (testing "the tap that used to be `(when (and run? (<= row 1)) …)`"
    (let [d (resolve* :pointer/tap :complete #{} (header-claim :noise?))]
      (is (= :fold/toggle-section (verb-of d)))
      (is (= :block/fold-header (:decision/site d)))
      (is (= :master (:decision/tier d)))
      (is (= :foldable (:decision/facet d)))
      (is (= 0 (:decision/depth d)) "the header is the INNERMOST claim")
      (is (= {:section :noise :fold-key :noise?} (:decision/args d))
          "the section arrives from the claim — the kernel computes no row")))

  (testing "the prose header is the same row, a different claim argument"
    (is (= {:section :prose :fold-key :prose?}
           (:decision/args
            (resolve* :pointer/tap :complete #{} (header-claim :prose?))))))

  (testing "the header claims the TAP but not the drag: pressing a header and
            moving still drags the block, via the outward fallthrough"
    (let [d (resolve* :pointer/press :threshold #{} (header-claim :noise?))]
      (is (= :placement/drag-group (verb-of d)))
      (is (= 1 (:decision/depth d)) "resolved at the BLOCK, one step out")
      (is (= :block/machine-hit-area (:decision/site d)))))

  (testing "shift-dragging a header selects the machine text, as before"
    (let [d (resolve* :pointer/press :threshold #{:shift}
                      (header-claim :noise?))]
      (is (= :selection/machine-begin (verb-of d)))
      (is (= 1 (:decision/depth d)))))

  (testing "shift-PRESSING a machine header stays silent, as it always did"
    (let [d (resolve* :pointer/press :begin #{:shift} (header-claim :noise?))]
      (is (= :unclaimed (:decision/outcome d)))
      (is (nil? (verb-of d))))))

;; ===========================================================================
;; Family 2 — block click/focus
;; ===========================================================================

(deftest family-2-block-tap-and-shift-press-resolve-to-attentions-rows
  (testing "tap a user block → place the caret (pointer-up! non-machine)"
    (let [d (resolve* :pointer/tap :complete #{}
                      (block-claim :block/user-hit-area))]
      (is (= :focus/place-caret (verb-of d)))
      (is (= :attention (:decision/facet d)))
      (is (= subject (:decision/subject d)))))

  (testing "tap a machine block → release focus (pointer-up! machine, no fold)"
    (is (= :focus/release
           (verb-of (resolve* :pointer/tap :complete #{}
                              (block-claim :block/machine-hit-area))))))

  (testing "the tap row accepts ANY modifiers — pre-P5 pointer-up! never
            consulted shift at all"
    (is (= :focus/place-caret
           (verb-of (resolve* :pointer/tap :complete #{:shift}
                              (block-claim :block/user-hit-area))))))

  (testing "shift-press a user block → focus in the same gesture (Task 11)"
    (is (= :focus/enter-block
           (verb-of (resolve* :pointer/press :begin #{:shift}
                              (block-claim :block/user-hit-area))))))

  (testing "a plain press claims nothing until the threshold, as before"
    (is (= :unclaimed
           (:decision/outcome
            (resolve* :pointer/press :begin #{}
                      (block-claim :block/user-hit-area))))))

  (testing "shift-press a MACHINE block does not focus it"
    (is (= :unclaimed
           (:decision/outcome
            (resolve* :pointer/press :begin #{:shift}
                      (block-claim :block/machine-hit-area)))))))

;; ===========================================================================
;; Family 3 — drag/move
;; ===========================================================================

(deftest family-3-threshold-resolves-the-whole-pre-p5-cond-ladder
  (testing "every branch of pointer-move!'s :pending cond, as rows"
    (is (= :placement/drag-group
           (verb-of (resolve* :pointer/press :threshold #{}
                              (block-claim :block/user-hit-area))))
        ":else on a block target")
    (is (= :selection/text-begin
           (verb-of (resolve* :pointer/press :threshold #{:shift}
                              (block-claim :block/user-hit-area))))
        "(:text? p)")
    (is (= :selection/machine-begin
           (verb-of (resolve* :pointer/press :threshold #{:shift}
                              (block-claim :block/machine-hit-area))))
        "(:mtext? p)")
    (is (= :placement/drag-group
           (verb-of (resolve* :pointer/press :threshold #{}
                              (block-claim :block/machine-hit-area))))
        "a machine block drags like any other")
    (is (= :selection/marquee-begin
           (verb-of (resolve* :pointer/press :threshold #{:shift}
                              bm/space-claim)))
        "(and (= :ground target) (:shift? p))")
    (is (= :camera/pan
           (verb-of (resolve* :pointer/press :threshold #{} bm/space-claim)))
        ":else on the ground target"))

  (testing "the continuous verbs are the ones with move/end continuations"
    (doseq [v [:placement/drag-group :selection/text-begin
               :selection/machine-begin :selection/marquee-begin :camera/pan]]
      (is (verbs/continuous? v) (str v " must serve :begin/:move/:end")))
    (doseq [v [:focus/place-caret :focus/enter-block :focus/release
               :fold/toggle-section :anchor/place :camera/zoom-at-pointer]]
      (is (not (verbs/continuous? v)) (str v " is discrete")))))

;; ===========================================================================
;; Family 4 — the space and the wheel, on the code floor
;; ===========================================================================

(deftest family-4-space-floor-survives-the-new-served-master
  (testing "without an active fm:space revision, all five gestures use the floor"
    (doseq [[kind phase mods verb]
            [[:pointer/tap :complete #{} :anchor/place]
             [:pointer/press :threshold #{} :camera/pan]
             [:pointer/press :threshold #{:shift} :selection/marquee-begin]
             [:wheel :complete #{} :camera/zoom-at-pointer]
             [:pointer/meta :complete #{} :halo/condense]]]
      (let [d (resolve* kind phase mods bm/space-claim)]
        (is (= verb (verb-of d)))
        (is (= :floor (:decision/tier d)))
        (is (= bm/space-facet (:decision/facet d))))))

  (testing "fm:space is registered; this fixture deliberately serves no rows"
    (is (nil? (get master-rows bm/space-facet)))
    (is (= "fm:space"
           (:facet-master/id
            (facet-masters/spec-for-facet bm/space-facet)))))

  (testing "even a hostile `fm:space` master could not bind the camera"
    (let [hostile {bm/space-facet
                   {:space/ground
                    [{:binding/gesture :pointer/press
                      :binding/phase :threshold
                      :binding/modifiers #{}
                      :binding/verb {:verb/name :camera/pan :verb/version 0}
                      :binding/priority 0}]}}]
      (is (not (bm/valid-bindings? (get hostile bm/space-facet)))
          "the row would be refused by the grammar before it could serve")))

  (testing "a hostile master CAN redirect the space's tap — and the grammar
            says so out loud, which is the point of a closed vocabulary"
    (let [hostile {bm/space-facet
                   {:space/ground
                    [{:binding/gesture :pointer/tap
                      :binding/phase :complete
                      :binding/modifiers :any
                      :binding/verb {:verb/name :focus/release
                                     :verb/version 0}
                      :binding/priority 0}]}}
          d (resolve* :pointer/tap :complete #{} bm/space-claim
                      :facet-rows (merge master-rows hostile))]
      (is (= :focus/release (verb-of d)))
      (is (= :master (:decision/tier d)))
      (is (= :camera/pan
             (verb-of (resolve* :pointer/press :threshold #{} bm/space-claim
                                :facet-rows (merge master-rows hostile))))
          "the camera is untouched even so"))))

;; ===========================================================================
;; Locality tiers
;; ===========================================================================

(deftest instance-rows-are-legal-and-beat-the-master-at-the-same-depth
  (let [instance-row {:binding/gesture :pointer/tap
                      :binding/phase :complete
                      :binding/modifiers :any
                      :binding/verb {:verb/name :focus/release
                                     :verb/version 0}
                      ;; deliberately WORSE priority than attention's row: the
                      ;; tier decides before priority is ever consulted
                      :binding/priority 9999}
        rows {[subject :block/user-hit-area] [instance-row]}]
    (testing "the instance tier wins on locality, not on priority"
      (let [d (resolve* :pointer/tap :complete #{}
                        (block-claim :block/user-hit-area)
                        :instance-rows rows)]
        (is (= :focus/release (verb-of d)))
        (is (= :instance (:decision/tier d)))
        (is (empty? (:decision/conflicts d))
            "a tier win is not a conflict")))

    (testing "another subject's rows are untouched"
      (is (= :focus/place-caret
             (verb-of (resolve* :pointer/tap :complete #{}
                                [{:claim/subject "du:other"
                                  :claim/site :block/user-hit-area
                                  :claim/facets bm/block-claim-facets
                                  :claim/args {}}]
                                :instance-rows rows)))))

    (testing "an instance row it does not name still falls to the master"
      (is (= :placement/drag-group
             (verb-of (resolve* :pointer/press :threshold #{}
                                (block-claim :block/user-hit-area)
                                :instance-rows rows)))))

    (testing "instance rows obey the SAME closed grammar"
      (is (bm/valid-bindings? {:block/user-hit-area [instance-row]}))
      (is (not (bm/valid-bindings?
                {:block/user-hit-area
                 [(assoc instance-row :binding/verb
                         {:verb/name :camera/pan :verb/version 0})]}))))))

(deftest containment-beats-every-tier
  (let [instance-drag {[subject :block/machine-hit-area]
                       [{:binding/gesture :pointer/tap
                         :binding/phase :complete
                         :binding/modifiers :any
                         :binding/verb {:verb/name :focus/place-caret
                                        :verb/version 0}
                         :binding/priority 0}]}]
    (testing "an INSTANCE row on the block loses to a MASTER row on the header:
              depth is resolved before tier, so the innermost claim wins"
      (let [d (resolve* :pointer/tap :complete #{} (header-claim :noise?)
                        :instance-rows instance-drag)]
        (is (= :fold/toggle-section (verb-of d)))
        (is (= 0 (:decision/depth d)))
        (is (= :master (:decision/tier d)))))))

;; ===========================================================================
;; Conflict lint
;; ===========================================================================

(deftest same-depth-ties-lint-with-a-deterministic-winner
  (let [tie-row {:binding/gesture :pointer/tap
                 :binding/phase :complete
                 :binding/modifiers :any
                 :binding/verb {:verb/name :focus/release :verb/version 0}
                 ;; attention's tap row is priority 10 — an exact tie
                 :binding/priority 10}
        tied (assoc-in master-rows
                       [:positioned :block/user-hit-area]
                       [(get-in positioned/bindings-form
                                [:facet-master/bindings
                                 :block/user-hit-area 0])
                        tie-row])
        d (resolve* :pointer/tap :complete #{}
                    (block-claim :block/user-hit-area)
                    :facet-rows tied)]
    (testing "the gesture still fires — a tie degrades to lint, never to a
              dead interaction"
      (is (= :claimed (:decision/outcome d)))
      (is (some? (verb-of d))))
    (testing "and the tie is REPORTED, never silent"
      (let [c (first (:decision/conflicts d))]
        (is (= 1 (count (:decision/conflicts d))))
        (is (= :material-binding/priority-tie (:type c)))
        (is (= [:attention :positioned] (:binding/facets c)))
        (is (= [:focus/place-caret :focus/release] (:binding/verbs c)))
        (is (= 10 (:binding/priority c)))
        (is (= :block/user-hit-area (:binding/site c)))))
    (testing "the winner is deterministic across map iteration order"
      (is (= (verb-of d)
             (verb-of (resolve* :pointer/tap :complete #{}
                                (block-claim :block/user-hit-area)
                                :facet-rows
                                (update-in tied
                                           [:positioned :block/user-hit-area]
                                           (comp vec reverse))))))
      (is (= :focus/place-caret (verb-of d))
            "lowest facet name at equal priority — attention before positioned")))

  (testing "an exact-modifier row beats an :any row at equal priority WITHOUT
            lint: that is a declared refinement, not an ambiguity"
    (let [refined
          (assoc-in master-rows [:positioned :block/user-hit-area]
                    [{:binding/gesture :pointer/tap
                      :binding/phase :complete
                      :binding/modifiers #{:shift}
                      :binding/verb {:verb/name :focus/release
                                     :verb/version 0}
                      :binding/priority 10}])
          shifted (resolve* :pointer/tap :complete #{:shift}
                            (block-claim :block/user-hit-area)
                            :facet-rows refined)
          plain (resolve* :pointer/tap :complete #{}
                          (block-claim :block/user-hit-area)
                          :facet-rows refined)]
      (is (= :focus/release (verb-of shifted)))
      (is (empty? (:decision/conflicts shifted)))
      (is (= :focus/place-caret (verb-of plain)))))

  (testing "the whole served table is linted, not only what a gesture trips"
    (let [rows (bm/table-rows
                [{:tier :master :facet :attention
                  :master-id "fm:attention" :revision-id "rev:a"
                  :bindings (:facet-master/bindings attention/bindings-form)}
                 {:tier :master :facet :positioned
                  :master-id "fm:positioned" :revision-id "rev:p"
                  :bindings {:block/user-hit-area
                             [{:binding/gesture :pointer/tap
                               :binding/phase :complete
                               :binding/modifiers :any
                               :binding/verb {:verb/name :focus/release
                                              :verb/version 0}
                               :binding/priority 10}]}}])
          conflicts (bm/table-conflicts rows)]
      (is (= 1 (count conflicts)))
      (is (= [:focus/place-caret :focus/release]
             (:binding/verbs (first conflicts)))))))

;; ===========================================================================
;; The floor drill — the fence's receipt
;; ===========================================================================

(defn- served-material
  "One served entry as `facet-master-projection` would hand it over."
  [spec bindings]
  (let [grammar (:facet-master/grammar (facet-engine/code-floor spec))
        material-keys (get-in spec [:facet-master/grammars grammar
                                    :material-keys])
        floor-material (select-keys (facet-engine/code-floor spec)
                                    material-keys)]
    {:facet-master/id (:facet-master/id spec)
     :facet-master/facet (:facet-master/facet spec)
     :facet-master/grammar grammar
     :facet-master/active-revision-id "rev:test"
     :facet-master/material
     (if (= ::floor bindings)
       floor-material
       (assoc floor-material :facet-master/bindings bindings))}))

(defn- rows-under
  "The MASTER tier as the client derives it, through the real `resolved-wear`.
   A FLOORED wear contributes nothing: it already IS the floor tier, and
   double-counting it would report `:master` for a floor decision."
  [by-id]
  (into {}
        (map (fn [spec]
               (let [wear (facet-engine/resolved-wear
                           spec (get by-id (:facet-master/id spec)))]
                 [(:facet-master/facet spec)
                  (when-not (:facet-master/floor? wear)
                    (:facet-master/bindings wear))])))
        facet-masters/specs))

(deftest the-code-floor-survives-any-data-revision
  (let [bindings-specs (filterv
                        #(seq (:facet-master/bindings
                               (facet-engine/code-floor %)))
                        facet-masters/specs)
        baseline (bm/drill-report {:facet-rows (rows-under {})
                                   :floor-rows floor-rows
                                   :instance-rows {}
                                   :subject subject})]
    (testing "three masters carry rows; the rest carry none"
      (is (= ["fm:attention" "fm:foldable" "fm:positioned"]
             (sort (map :facet-master/id bindings-specs)))))

    (testing "every probe resolves to a verb — no dead gesture anywhere"
      (is (every? #(= :claimed (:probe/outcome %)) baseline))
      (is (every? (comp some? :probe/verb) baseline))
      (is (= 16 (count baseline))))

    (testing "R3-G1: the twelve frozen receipts stay byte-identical and Halo's
              four additive receipts all land on the code floor"
      (is (= [[:focus/place-caret :floor :attention :claimed]
              [:focus/enter-block :floor :attention :claimed]
              [:placement/drag-group :floor :positioned :claimed]
              [:selection/text-begin :floor :attention :claimed]
              [:focus/release :floor :attention :claimed]
              [:placement/drag-group :floor :positioned :claimed]
              [:fold/toggle-section :floor :foldable :claimed]
              [:anchor/place :floor :space :claimed]
              [:camera/pan :floor :space :claimed]
              [:selection/marquee-begin :floor :space :claimed]
              [:camera/zoom-at-pointer :floor :space :claimed]
              [:camera/zoom-at-pointer :floor :space :claimed]
              [:halo/condense :floor :attention :claimed]
              [:halo/condense :floor :attention :claimed]
              [:halo/condense :floor :foldable :claimed]
              [:halo/condense :floor :space :claimed]]
             (mapv (juxt :probe/verb :probe/tier
                         :probe/facet :probe/outcome)
                   baseline))))

    (testing "G1 probe 12: wheel at a block falls outward to the space"
      (is (= {:probe/label
              "wheel at a block → zoom through the space rung"
              :probe/site :block/user-hit-area
              :probe/decision-site :space/ground
              :probe/depth 1
              :probe/verb :camera/zoom-at-pointer
              :probe/tier :floor
              :probe/facet :space
              :probe/outcome :claimed}
             (select-keys
              (nth baseline 11)
              [:probe/label :probe/site :probe/decision-site :probe/depth
               :probe/verb :probe/tier :probe/facet :probe/outcome]))))

    (doseq [spec bindings-specs]
      (let [mid (:facet-master/id spec)
            served-ok (into {}
                            (map (fn [s]
                                   [(:facet-master/id s)
                                    (served-material s ::floor)]))
                            facet-masters/specs)
            world (fn [entry]
                    (bm/drill-report
                     {:facet-rows (rows-under
                                   (if entry
                                     (assoc served-ok mid entry)
                                     (dissoc served-ok mid)))
                      :floor-rows floor-rows
                      :instance-rows {}
                      :subject subject}))
            verbs-of #(mapv :probe/verb %)
            live (world (get served-ok mid))
            absent (world nil)
            malformed (world {:facet-master/id mid
                              :facet-master/facet
                              (:facet-master/facet spec)
                              :facet-master/grammar 1
                              :facet-master/active-revision-id "rev:garbage"
                              :facet-master/material {:drill/garbage true}})
            stripped
            (world
             (served-material
              spec
              (into {}
                    (keep (fn [[site rows]]
                            (let [kept (filterv
                                        #(not= :pointer/tap
                                               (:binding/gesture %))
                                        rows)]
                              (when (seq kept) [site kept]))))
                    (:facet-master/bindings
                     (facet-engine/code-floor spec)))))]
        (testing (str mid " — a served revision, absent, malformed, and a
                       VALID revision with every tap row removed all resolve
                       identically")
          (is (= (verbs-of baseline) (verbs-of live)) (str mid " live"))
          (is (= (verbs-of baseline) (verbs-of absent)) (str mid " absent"))
          (is (= (verbs-of baseline) (verbs-of malformed))
              (str mid " malformed"))
          (is (= (verbs-of baseline) (verbs-of stripped))
              (str mid " valid-but-stripped")))

        (testing (str mid " — and the TIER is honest about who answered: the
                       probes THIS facet decides drop to the FLOOR, rather than
                       reporting a master that is silently serving floor bytes.
                       Probes another facet decides are untouched.")
          ;; the reference here is the LIVE world (every facet validly served),
          ;; not `baseline` — `baseline` is the empty-serve world, where every
          ;; probe already rides the floor and there is no master to lose.
          (let [facet (:facet-master/facet spec)
                tiers (fn [report]
                        (into {} (map (juxt :probe/label :probe/tier)) report))
                base (tiers live)
                mine (into #{}
                           (comp (filter #(= facet (:probe/facet %)))
                                 (map :probe/label))
                           live)]
            (is (seq mine) (str mid " decides at least one probe"))
            (doseq [[world-name report] [[:absent absent]
                                         [:malformed malformed]]
                    :let [now (tiers report)]]
              (doseq [label mine]
                (is (= :floor (get now label))
                    (str mid " " (name world-name) " → floor: " label)))
              (doseq [label (remove mine (keys base))]
                (is (= (get base label) (get now label))
                    (str mid " " (name world-name)
                         " leaves another facet's probe alone: "
                         label))))))))

    (testing "the named fence gestures specifically: pan, zoom, click-focus"
      (let [by-label (into {} (map (juxt :probe/label :probe/verb)) baseline)]
        (is (= :camera/pan
               (get by-label "drag empty space → pan the camera")))
        (is (= :camera/zoom-at-pointer
               (get by-label "wheel → zoom at the pointer")))
        (is (= :focus/place-caret
               (get by-label "tap a user block → focus")))))

    (testing "a v0 active revision (a cluster that never ran the P5 ingest)
              still folds, focuses, drags, pans and zooms"
      (let [v0 (into {}
                     (map (fn [spec]
                            [(:facet-master/id spec)
                             {:facet-master/id (:facet-master/id spec)
                              :facet-master/facet
                              (:facet-master/facet spec)
                              :facet-master/grammar 0
                              :facet-master/active-revision-id "rev:v0"
                              :facet-master/material
                              (:material
                               (facet-engine/compile-form
                                spec
                                (:facet-master/default-form spec)))}]))
                     facet-masters/specs)
            report (bm/drill-report {:facet-rows (rows-under v0)
                                     :floor-rows floor-rows
                                     :instance-rows {}
                                     :subject subject})]
        (is (= (mapv :probe/verb baseline) (mapv :probe/verb report)))
        (is (every? #(= :floor (:probe/tier %)) report)
            "with no v1 rows served, every gesture rides the code floor")))))

;; ===========================================================================
;; The served interaction table
;; ===========================================================================

(deftest the-interaction-table-links-every-row-to-its-master
  (let [floor-tables
        (mapv (fn [[facet bindings]]
                {:tier :floor :facet facet
                 :master-id (facet-masters/floor-master-id facet)
                 :revision-id (facet-masters/floor-master-id facet)
                 :floor? true :bindings bindings})
              floor-rows)
        master-tables
        (mapv (fn [[facet form]]
                {:tier :master :facet facet
                 :master-id (:facet-master/id form)
                 :revision-id (str "rev:" (name facet))
                 :floor? false
                 :bindings (:facet-master/bindings form)})
              {:attention attention/reply-bindings-form
               :foldable foldable/bindings-form
               :positioned positioned/bindings-form})
        tables (into floor-tables master-tables)
        rows (bm/table-rows tables)]
    (testing "every row carries its master link and its verb's effect class"
      (is (seq rows))
      (is (every? #(and (some? (:table/master-id %))
                        (some? (:table/verb %))
                        (contains? verbs/effect-classes
                                   (:table/effect-class %)))
                  rows)))
    (testing "the shipped table has no lint"
      (is (empty? (bm/table-conflicts rows))))
    (testing "the table is deterministic — same input, same bytes"
      (is (= (pr-str rows)
             (pr-str
              (bm/table-rows (vec (reverse tables)))))))
    (testing "reading the table answers `what does this gesture do, and who
              decided?` for every gesture the kernel can produce"
      (is (= bm/legal-gestures
             (set (map (juxt :table/gesture :table/phase) rows)))))))

;; ===========================================================================
;; Grammar versioning — durable v0 is never reinterpreted
;; ===========================================================================

(deftest bindings-grammars-are-additive-and-v0-keeps-its-meaning
  (doseq [[spec v0-form v1-form v2-form]
          [[attention/spec attention/default-form attention/bindings-form
            attention/strict-bindings-form]
           [foldable/spec foldable/default-form foldable/bindings-form
            foldable/strict-bindings-form]
           [positioned/spec positioned/default-form
            positioned/bindings-form positioned/strict-bindings-form]]]
    (let [mid (:facet-master/id spec)]
      (testing (str mid " — v0 bytes still compile under their OWN grammar")
        (let [c (facet-engine/compile-form spec v0-form)]
          (is (:valid? c))
          (is (= 0 (:grammar c)))
          (is (not (contains? (:material c) :facet-master/bindings))
              "v0 material is never backfilled with row meaning")))

      (testing (str mid " — v1 adds exactly the bindings key")
        (let [c (facet-engine/compile-form spec v1-form)]
          (is (:valid? c))
          (is (= 1 (:grammar c)))
          (is (bm/valid-bindings? (:facet-master/bindings (:material c))))
          (is (= #{:facet-master/bindings}
                 (set/difference
                  (set (get-in spec [:facet-master/grammars 1
                                     :material-keys]))
                  (set (get-in spec [:facet-master/grammars 0
                                     :material-keys])))))))

      ;; P6 · T10: the floor moved to v2 — v1's rows verbatim under a grammar
      ;; that additionally refuses a row whose SITE cannot feed its verb. v1
      ;; keeps its own declaration and is never re-read through v2's validator.
      ;; P8 moves ONLY attention to v3 with exactly one additional eval row.
      ;; smalltalk-ui-vm P2 moves ONLY foldable to v3 with one paste-policy
      ;; value; its binding rows remain byte-equivalent to v2.
      (testing (str mid " — the FLOOR has the latest additive grammar")
        (let [p8-attention? (= attention/master-id mid)
              p2-foldable? (= foldable/master-id mid)
              expected-floor (cond
                               p8-attention?
                               attention/reply-bindings-form

                               p2-foldable?
                               foldable/paste-clamp-form

                               :else v2-form)
              expected-suffix (if (or p8-attention? p2-foldable?)
                                ":v3"
                                ":v2")]
        (is (= expected-floor (:facet-master/floor-form spec)))
        (is (seq (:facet-master/bindings (facet-engine/code-floor spec))))
        (is (str/ends-with? (:facet-master/code-floor-revision-id spec)
                            expected-suffix))
        (cond
          p8-attention?
          (is (= [attention/reply-binding-row]
                 (vec
                  (remove
                   (set (get-in v2-form
                                [:facet-master/bindings
                                 :block/user-hit-area]))
                   (get-in expected-floor
                           [:facet-master/bindings
                            :block/user-hit-area]))))
              "v3 adds exactly the one P8 row")

          p2-foldable?
          (do
            (is (= (:facet-master/bindings v2-form)
                   (:facet-master/bindings expected-floor))
                "foldable v3 leaves the strict binding rows byte-equivalent")
            (is (= #{:foldable/paste-clamp}
                   (set/difference
                    (set (keys expected-floor))
                    (set (keys v2-form))))
                "foldable v3 adds exactly the material paste policy"))

          :else
          (is (= (:facet-master/bindings v1-form)
                 (:facet-master/bindings v2-form))
              "v2 changes the grammar version and NOTHING about the rows"))
        (is (:valid? (facet-engine/compile-form spec v1-form))
            "a durable v1 revision stays rewearable under v1 forever")))

      (testing (str mid " — a malformed row set is refused, not accepted")
        (is (not (:valid?
                  (facet-engine/compile-form
                   spec
                   (assoc v1-form :facet-master/bindings
                          {:block/user-hit-area
                           [{:binding/gesture :pointer/tap
                             :binding/phase :complete
                             :binding/modifiers #{}
                             :binding/verb {:verb/name :launch/missiles
                                            :verb/version 0}
                             :binding/priority 1}]}))))))))

  (testing "provenance, threaded and text-body carry NO rows — P5 extracted
            only the gestures that already existed"
    (doseq [spec facet-masters/specs
            :when (not (contains? #{"fm:attention" "fm:foldable"
                                    "fm:positioned"}
                                  (:facet-master/id spec)))]
      (is (nil? (:facet-master/bindings (facet-engine/code-floor spec)))
          (str (:facet-master/id spec) " must stay row-free")))))

;; ===========================================================================
;; The freeze — the kernel's dispatch surface is stated, and the old ladders
;; are gone from the source
;; ===========================================================================

(deftest the-dispatch-surface-is-frozen-as-data
  (testing "the gestures and phases the kernel can produce are enumerated"
    (is (= #{[:pointer/press :begin]
             [:pointer/press :threshold]
             [:pointer/tap :complete]
             [:pointer/meta :complete]
             [:wheel :complete]
             [:key/eval :complete]}
           bm/legal-gestures)
        "growing this set is a kernel change, a grammar version, and this edit")
    (is (= #{:shift} bm/modifier-keys))
    (is (= [:instance :master :floor] bm/tier-order))
    (is (= #{:block/user-hit-area :block/machine-hit-area
             :block/fold-header :space/ground}
           bm/sites))))

;; The retired scene renderer's pixel equivalence receipt left with it.

(deftest no-side-effect-can-reach-the-resolution-path
  (testing "the law's whole output is data — nothing in a decision is callable"
    (let [d (resolve* :pointer/tap :complete #{}
                      (block-claim :block/user-hit-area))]
      (letfn [(fns? [x]
                (cond
                  (fn? x) true
                  (map? x) (some fns? (concat (keys x) (vals x)))
                  (coll? x) (some fns? x)
                  :else false))]
        (is (not (fns? d)))
        (is (not (fns? (bm/table-rows
                        [{:tier :floor :facet bm/space-facet
                          :master-id bm/space-floor-master-id
                          :bindings bm/space-floor-bindings}]))))
        (is (not (fns? master-rows))
            "binding rows are DATA — a closure in material would be
             unserializable and agent-illegible (the durable-data G2 rule)")))))
