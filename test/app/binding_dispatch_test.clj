(ns app.binding-dispatch-test
  "editable-material P5 — the one dispatch law, the closed row grammar, and the
   code floor's unbreakability, proven where they live: as pure data.

   These tests are the package's per-family proof. Each family's pre-P5 branch
   is named in the test that replaces it, so a reader can check the migration
   rather than trust it. The floor drill is the fence's receipt: `binding-material
   /drill-report` is the SAME function the live console drill calls, so a green
   test and a green browser drill are the same claim."
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [app.shared.attention-material :as attention]
            [app.shared.binding-material :as bm]
            [app.shared.facet-material :as facet-material]
            [app.client.workspace.rect-tree :as rt]
            [app.shared.facet-masters :as facet-masters]
            [app.shared.foldable-material :as foldable]
            [app.shared.material-inspector :as material-inspector]
            [app.shared.positioned-material :as positioned]
            [app.shared.verb-registry :as verbs]))

;; ===========================================================================
;; Fixtures — the tiers exactly as the kernel assembles them
;; ===========================================================================

(def ^:private floor-rows
  "The kernel's `floor-binding-rows`, rebuilt from the same specs."
  (into {bm/space-facet bm/space-floor-bindings}
        (map (fn [spec]
               [(:facet-master/facet spec)
                (:facet-master/bindings (facet-material/code-floor spec))]))
        facet-masters/specs))

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
  (testing "the two camera verbs are the reserved floor, and only those"
    (is (= [:camera/pan :camera/zoom-at-pointer]
           (->> (verbs/declaration-rows)
                (filter :verb/floor-reserved?)
                (mapv :verb/name)))))
  (testing "a durable-via-request verb is exactly one that arms the settle lane"
    (is (= #{:camera/pan :camera/zoom-at-pointer :placement/drag-group}
           (->> (verbs/declaration-rows)
                (filter #(= :durable-via-request (:verb/effect-class %)))
                (map :verb/name)
                set)))))

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

(deftest family-4-space-and-wheel-live-only-on-the-code-floor
  (testing "the space's gestures resolve at the FLOOR tier, never a master"
    (doseq [[kind phase mods verb]
            [[:pointer/tap :complete #{} :anchor/place]
             [:pointer/press :threshold #{} :camera/pan]
             [:pointer/press :threshold #{:shift} :selection/marquee-begin]
             [:wheel :complete #{} :camera/zoom-at-pointer]]]
      (let [d (resolve* kind phase mods bm/space-claim)]
        (is (= verb (verb-of d)))
        (is (= :floor (:decision/tier d)))
        (is (= bm/space-facet (:decision/facet d))))))

  (testing "there is no served master for the space, so material cannot reach it"
    (is (nil? (get master-rows bm/space-facet)))
    (is (not-any? #(= bm/space-facet (:facet-master/facet %))
                  facet-masters/specs)))

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
  (let [grammar (:facet-master/grammar (facet-material/code-floor spec))
        material-keys (get-in spec [:facet-master/grammars grammar
                                    :material-keys])
        floor-material (select-keys (facet-material/code-floor spec)
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
               (let [wear (facet-material/resolved-wear
                           spec (get by-id (:facet-master/id spec)))]
                 [(:facet-master/facet spec)
                  (when-not (:facet-master/floor? wear)
                    (:facet-master/bindings wear))])))
        facet-masters/specs))

(deftest the-code-floor-survives-any-data-revision
  (let [bindings-specs (filterv
                        #(seq (:facet-master/bindings
                               (facet-material/code-floor %)))
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
      (is (= 11 (count baseline))))

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
                     (facet-material/code-floor spec)))))]
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
                               (facet-material/compile-form
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
  (let [rows (bm/table-rows
              (into [{:tier :floor :facet bm/space-facet
                      :master-id "code-floor:space" :revision-id nil
                      :floor? true :bindings bm/space-floor-bindings}]
                    (map (fn [[facet form]]
                           {:tier :master :facet facet
                            :master-id (:facet-master/id form)
                            :revision-id (str "rev:" (name facet))
                            :floor? false
                            :bindings (:facet-master/bindings form)}))
                    {:attention attention/bindings-form
                     :foldable foldable/bindings-form
                     :positioned positioned/bindings-form}))]
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
              (bm/table-rows
               (into [{:tier :master :facet :positioned
                       :master-id (:facet-master/id positioned/bindings-form)
                       :revision-id "rev:positioned" :floor? false
                       :bindings (:facet-master/bindings
                                  positioned/bindings-form)}]
                     [{:tier :master :facet :foldable
                       :master-id (:facet-master/id foldable/bindings-form)
                       :revision-id "rev:foldable" :floor? false
                       :bindings (:facet-master/bindings
                                  foldable/bindings-form)}
                      {:tier :master :facet :attention
                       :master-id (:facet-master/id attention/bindings-form)
                       :revision-id "rev:attention" :floor? false
                       :bindings (:facet-master/bindings
                                  attention/bindings-form)}
                      {:tier :floor :facet bm/space-facet
                       :master-id "code-floor:space" :revision-id nil
                       :floor? true
                       :bindings bm/space-floor-bindings}]))))))
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
        (let [c (facet-material/compile-form spec v0-form)]
          (is (:valid? c))
          (is (= 0 (:grammar c)))
          (is (not (contains? (:material c) :facet-master/bindings))
              "v0 material is never backfilled with row meaning")))

      (testing (str mid " — v1 adds exactly the bindings key")
        (let [c (facet-material/compile-form spec v1-form)]
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
      (testing (str mid " — the FLOOR is the v2 form, so the floor has rows")
        (is (= v2-form (:facet-master/floor-form spec)))
        (is (seq (:facet-master/bindings (facet-material/code-floor spec))))
        (is (str/ends-with? (:facet-master/code-floor-revision-id spec) ":v2"))
        (is (= (:facet-master/bindings v1-form)
               (:facet-master/bindings v2-form))
            "v2 changes the grammar version and NOTHING about the rows")
        (is (:valid? (facet-material/compile-form spec v1-form))
            "a durable v1 revision stays rewearable under v1 forever"))

      (testing (str mid " — a malformed row set is refused, not accepted")
        (is (not (:valid?
                  (facet-material/compile-form
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
      (is (nil? (:facet-master/bindings (facet-material/code-floor spec)))
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
             [:wheel :complete]}
           bm/legal-gestures)
        "growing this set is a kernel change, a grammar version, and this edit")
    (is (= #{:shift} bm/modifier-keys))
    (is (= [:instance :master :floor] bm/tier-order))
    (is (= #{:block/user-hit-area :block/machine-hit-area
             :block/fold-header :space/ground}
           bm/sites)))

  (testing "the kernel declares its own branch budget, and the declaration is
            what a later session must edit to grow it"
    (let [ground (slurp "src/app/client/workspace/ground.cljs")]
      (is (str/includes? ground ":mechanism-branches {:pointer-down! 0"))
      (is (str/includes? ground ":pointer-move! 3"))
      (is (str/includes? ground ":pointer-up! 2"))
      (is (str/includes? ground ":handle-wheel! 0"))
      (is (str/includes? ground ":meaning-branches 0")))))

(defn- code-only
  "Drop `;`-comment lines. The kernel deliberately KEEPS prose pointers at each
   verb's implementation site (and `verb-registry`'s `:verb/extracted-from`
   records the same provenance as data), so a freeze test that greps raw source
   would be asserting about documentation. The claim under test is that the
   ladders are gone from the CODE."
  [source]
  (->> (str/split-lines source)
       (remove #(str/starts-with? (str/triml %) ";"))
       (str/join "\n")))

(deftest the-migrated-ladders-are-deleted-from-the-kernel
  (let [ground (code-only (slurp "src/app/client/workspace/ground.cljs"))]
    (testing "family 1: the fold-header row arithmetic is gone"
      (is (not (str/includes? ground "(<= row 1)")))
      (is (not (str/includes? ground "(if (zero? row) :noise? :prose?)")))
      (is (not (str/includes? ground "run? (contains? (context-block-entry"))))

    (testing "families 2+3: the press-time meaning flags are gone"
      (is (not (str/includes? ground "text?  (boolean (and shift? b")))
      (is (not (str/includes? ground "mtext? (boolean (and shift? b")))
      (is (not (str/includes? ground "(:text? p)")))
      (is (not (str/includes? ground "(:mtext? p)")))
      (is (not (str/includes? ground ":sel-caret")))
      (is (not (str/includes? ground ":sel-lc"))))

    (testing "the pointer machine's meaning-bearing phases are gone"
      (doseq [phase [":phase :selecting" ":phase :mselecting"
                     ":phase :marquee" ":phase :panning" ":phase :dragging"]]
        (is (not (str/includes? ground phase))
            (str phase " became a verb, not a phase"))))

    (testing "the kernel keeps exactly three pointer phases"
      (is (str/includes? ground ":pointer-phases #{:idle :pending :active}")))

    (testing "all four families reach their verbs through exactly TWO call
              sites of the law — the shared discrete dispatch (press/:begin,
              tap, wheel) and the threshold"
      (is (= 2 (count (re-seq #"\(decide \{" ground))))
      (is (= 1 (count (re-seq #"binding-material/resolve-binding" ground)))
          "one law, one implementation")
      (is (= 1 (count (re-seq #"\(defn- invoke-verb!" ground)))
          "one side-effecting site"))

    (testing "the space's rows are code, not material"
      (is (str/includes? ground "binding-material/space-claim"))
      (is (str/includes? ground "floor-binding-rows")))

    (testing "the verb registry is the only vocabulary the kernel registers"
      (let [registered (set (map second (re-seq #"\(register-verb! (:\S+)"
                                                ground)))]
        (is (= (set (map str verbs/names)) registered)
            "every declared verb has an implementation and vice versa")))

    (testing "the interaction table and the drill are both reachable"
      (is (str/includes? ground "binding-floor-drill"))
      (is (str/includes? ground "__bindings")))

    (testing "the shared law and registry exist as .cljc, JVM-provable"
      (is (.exists (io/file "src/app/shared/binding_material.cljc")))
      (is (.exists (io/file "src/app/shared/verb_registry.cljc"))))))

;; ===========================================================================
;; The containment path got finer at ZERO pixels
;;
;; P5 makes claims resolvable by giving the fold headers their own nodes. The
;; byte-identical-render stop is therefore not a screenshot question but a
;; structural one: a hit-only node must contribute nothing to ANY of the three
;; GPU walks, at every camera, forever. That is what is proven here — over the
;; real `rect-tree` walks, with the real node shapes.
;; ===========================================================================

(def ^:private line-h 27.0)
(def ^:private pad 8.0)

(defn- ground-block-tree
  "The shape `block-tree` builds: text ops on the root, a decoration child
   covering the whole block, and (when `headers?`) one hit-only node per header
   row appended LAST."
  [headers?]
  (let [w 400.0
        h (+ (* 5 line-h) (* 2 pad))]
    (rt/resolve-layout
     (rt/rt-node
      :ground-block :text-run
      {:x 0 :y 0 :w w :h h}
      :text [{:text "▾ thinking+tools — click to hide" :type :text
              :from 0 :to 31 :x 0 :y 19.0 :size 19.0
              :r 0.62 :g 0.66 :b 0.76 :a 0.6}
             {:text "▾ reply — click to hide" :type :text
              :from 0 :to 22 :x 0 :y (+ line-h 19.0) :size 19.0
              :r 0.62 :g 0.66 :b 0.76 :a 0.6}]
      :data {:address subject
             :material/claim {:claim/subject subject
                              :claim/site :block/machine-hit-area
                              :claim/facets bm/block-claim-facets
                              :claim/args {}}}
      :children
      (cond-> [(rt/rt-node :ground-box :rect
                           {:x (- pad) :y (- pad) :w w :h h}
                           :style {:border-width 1.0
                                   :border-color [0.45 0.52 0.66 0.55]
                                   :bg [0.0 0.0 0.0 0.0]})]
        headers?
        (into
         (mapv
          (fn [i {:keys [section fold-key]}]
            (rt/rt-node
             (keyword (str "ground-fold-header-hit-" (name section)))
             :hit-area
             {:x 0 :y (* i line-h) :w w :h line-h}
             :data {:address subject
                    :material/claim
                    {:claim/subject subject
                     :claim/site :block/fold-header
                     :claim/facets [:foldable]
                     :claim/args {:section section :fold-key fold-key}}}))
          (range)
          [{:section :noise :fold-key :noise?}
           {:section :prose :fold-key :prose?}])))))))

(deftest hit-only-claim-nodes-contribute-exactly-zero-pixels
  (let [without (ground-block-tree false)
        with (ground-block-tree true)]
    (testing "the root's bounds are untouched — width and height are the
              block's own text metrics, so no position or wrap can move"
      (is (= (:bounds without) (:bounds with))))

    (testing "every GPU walk yields IDENTICAL output"
      (is (= (rt/tree->rects without) (rt/tree->rects with))
          "no :bg ⇒ no rect")
      (is (= (rt/tree->text-ops without) (rt/tree->text-ops with))
          "no :text ⇒ no glyph")
      (is (= (rt/tree->shadows without) (rt/tree->shadows with))
          "no :shadow ⇒ no shadow"))

    (testing "and identical under a clip, at any offset"
      (doseq [[px py clip]
              [[0 0 nil]
               [137.5 -42.25 nil]
               [0 0 {:x 10 :y 10 :w 100 :h 100}]
               [-500 800 {:x 0 :y 0 :w 50 :h 20}]]]
        (is (= (rt/tree->rects without px py clip)
               (rt/tree->rects with px py clip)))
        (is (= (rt/tree->text-ops without px py clip)
               (rt/tree->text-ops with px py clip)))))

    (testing "the pick, by contrast, DOES see them — appended last, so
              hit-test's reverse child walk reaches a header before the
              attention box that covers the same pixels"
      (let [header-hit (rt/hit-test with 200.0 (* 0.5 line-h))
            prose-hit (rt/hit-test with 200.0 (* 1.5 line-h))
            body-hit (rt/hit-test with 200.0 (* 3.5 line-h))]
        (is (= :ground-fold-header-hit-noise (:id (peek header-hit))))
        (is (= :ground-fold-header-hit-prose (:id (peek prose-hit))))
        (is (= :ground-box (:id (peek body-hit)))
            "a body row still resolves to the block, whose claim is the
             block-grain one")
        (is (= subject (get-in (peek header-hit) [:data :address]))
            "the header stays block-addressed, so the deictic pick and the
             material inspector are unaffected")))

    (testing "without the header nodes the same points resolve to the box —
              i.e. the geometry the deleted `row` arithmetic used to compute
              is exactly what containment now answers"
      (is (= :ground-box
             (:id (peek (rt/hit-test without 200.0 (* 0.5 line-h)))))))

    (testing "a claim map is never mistaken for a contribution stamp"
      (is (empty? (material-inspector/contribution-stamps
                   (get-in with [:data :material/claim])))))))

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
                          :master-id "code-floor:space"
                          :bindings bm/space-floor-bindings}]))))
        (is (not (fns? master-rows))
            "binding rows are DATA — a closure in material would be
             unserializable and agent-illegible (the scene-store G2 rule)")))))
