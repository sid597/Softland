(ns app.client.workspace.trail-face-test
  "View-MVP WP-B2 acceptance gates 1-13 (CONTRACT s11), JVM-run over the
   s8 hand fixtures. Gate 5 asserts the REGENERATED atlas (P4). Every
   gate names its falsifier in the contract; assertions mirror them.
   No wall clock anywhere: 'now' comes from the fixtures."
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [app.client.workspace.rect-tree :as rt]
            [app.server.rama.trail-view :as tv-mod]
            [app.server.rama.trail-view-test :as tvt]
            [app.client.workspace.trail-face.sanitize :as san]
            [app.client.workspace.trail-face.text-face :as tf]
            [app.client.workspace.trail-face.cards :as cards]
            [app.client.workspace.trail-face.lanes :as lanes]
            [app.client.workspace.trail-face.scene :as scene]))

;; --- Fixture + atlas loading -------------------------------------------------

(def bundle (edn/read-string (slurp "test/resources/trail_face/bundle.edn")))
(def feed (edn/read-string (slurp "test/resources/trail_face/feed.edn")))
(def projection (:projection/text
                 (edn/read-string (slurp "test/resources/trail_face/projection.edn"))))
(def now-ms (:fixture/now-ms bundle))

(def atlas (json/read-str (slurp "resources/public/font_atlas.json")))
(def coverage (san/coverage-set atlas))

(def geom {:viewport-w 800 :viewport-h 600 :line-height 18 :font-size 13
           :char-advance 7.84 :card-w 300 :pad 8 :now-ms now-ms})

(def expanded-key ["oc:doc:9fdoc" 1782171000000])

(defn text-scene []
  (scene/build-text-face-scene {:text projection
                                :address (:bundle/address bundle)
                                :coverage coverage
                                :geom geom}))

(defn timeline-scene
  ([] (timeline-scene {:expanded #{expanded-key} :order :arrival}))
  ([view-state]
   (scene/build-timeline-scene {:feed feed
                                :bundles {"oc:doc:9fdoc" bundle}
                                :view-state view-state
                                :coverage coverage
                                :geom geom})))

;; --- Tree helpers --------------------------------------------------------------

(defn all-nodes [node]
  (cons node (mapcat all-nodes (:children node))))

(defn find-node [tree pred]
  (first (filter pred (all-nodes tree))))

(defn all-text-ops
  "Every text op in the tree (raw, node-local coords)."
  [tree]
  (mapcat :text (all-nodes tree)))

(defn address-ops [tree]
  (filter :trail-face/address? (all-text-ops tree)))

(defn node-by-id [tree id]
  (find-node tree #(= id (:id %))))

;; --- Gate 1: address law ---------------------------------------------------------

(deftest address-law-test
  (testing "View-3 scene: exactly one marked header address line, pure-EDN round-trip"
    (let [s (text-scene)
          header (node-by-id s :trail-face/address-header)
          ops (address-ops s)]
      (is (some? header))
      (is (= 1 (count ops)) "exactly one marked address op in the text face")
      (is (= (:bundle/address bundle) (edn/read-string (:text (first ops))))
          "rendered address round-trips shape-equal (A1: pure EDN)")))
  (testing "timeline scene: one header address + one per expanded card"
    (let [s (timeline-scene)
          header (node-by-id s :trail-face/address-header)
          header-ops (address-ops header)
          exp (find-node s #(= :expansion (:type %)))
          exp-ops (address-ops exp)]
      (is (some? header))
      (is (= 1 (count header-ops)))
      (is (= (:feed/address feed) (edn/read-string (:text (first header-ops))))
          "feed address round-trips (order param matches fixture's :arrival)")
      (is (some? exp) "expanded card present")
      (is (= 1 (count exp-ops)) "expansion renders the expanded target's address")
      (is (= (:bundle/address bundle) (edn/read-string (:text (first exp-ops))))))))

;; --- Gate 2: View-3 verbatim ------------------------------------------------------

(deftest view3-verbatim-test
  (let [lines (tf/split-projection-lines projection)
        s (text-scene)
        body (node-by-id s :trail-face/text-body)
        ops (vec (:text body))]
    (is (= (count lines) (count ops)) "line count == projection line count")
    (doseq [[line op] (map vector lines ops)]
      (is (= (san/sanitize-text coverage san/fallback-cp line) (:text op))
          "per-line op text == projection line after sanitize (no drop/reorder/re-wrap)")
      (is (= (san/codepoint-count line) (san/op-advance-count op))
          "fallback substitutions counted, positions preserved"))
    (testing "one op per line at its own y - no re-wrap"
      (is (= (count ops) (count (distinct (map :y ops))))))))

;; --- Gate 3: marker totality -------------------------------------------------------

(deftest marker-totality-test
  (testing "every line maps to a style in the table"
    (let [{:keys [ok? ops]} (tf/projection->line-ops projection)]
      (is ok?)
      (is (every? #(contains? tf/marker->style (:style %)) ops))
      (is (= :meta (:style (nth ops 0))))
      (is (= :address (:style (nth ops 1))))
      (is (= :heading (:style (nth ops 2))))
      (is (= :stamps (:style (nth ops 3))))
      (is (= :section (:style (nth ops 4))))
      (is (= :relation-out (:style (nth ops 6))))
      (is (= :relation-in (:style (nth ops 7))))
      (is (= :verdict (:style (nth ops 9))))))
  (testing "unknown marker line -> :normal, never dropped"
    (let [mutated (str projection "\n@@ weird unknown marker")
          {:keys [ops]} (tf/projection->line-ops mutated)]
      (is (= (inc (count (tf/split-projection-lines projection))) (count ops)))
      (is (= :normal (:style (peek ops))))))
  (testing "version-header mismatch -> whole projection plain + exactly ONE notice"
    (let [mutated (str/replace-first projection "trail-text v0" "trail-text v9")
          {:keys [ok? ops]} (tf/projection->line-ops mutated)
          notices (filter #(= :notice (:style %)) ops)
          rest-ops (remove #(= :notice (:style %)) ops)]
      (is (not ok?))
      (is (= 1 (count notices)) "exactly one notice line")
      (is (every? #(= :normal (:style %)) rest-ops) "everything else plain")
      (is (= (count (tf/split-projection-lines mutated)) (count rest-ops))
          "no projection line dropped")))
  (testing "no header at all"
    (let [{:keys [ok? ops]} (tf/projection->line-ops "just some text\nno header")]
      (is (not ok?))
      (is (= 1 (count (filter #(= :notice (:style %)) ops)))))))

;; --- Gate 4: sanitize correctness (V3-5) --------------------------------------------

(deftest sanitize-correctness-test
  (testing "class (i): audit top-missing, covered after regen -> verbatim"
    (let [salt "\u2500\u2192\u2014"]
      (is (contains? coverage 0x2500) "regen landed: U+2500 covered")
      (is (= salt (san/sanitize-text coverage san/fallback-cp salt))
          "covered codepoints pass through verbatim")
      (is (= 3 (san/codepoint-count salt)) "advance count == codepoint count")))
  (testing "class (ii): permanently-uncovered astral -> fallback, count preserved"
    (let [red "\uD83D\uDD34"
          s (str "a" red "b")
          out (san/sanitize-text coverage san/fallback-cp s)]
      (is (not (contains? coverage 0x1F534)) "emoji never added to the atlas")
      (is (= 3 (san/codepoint-count s)) "surrogate pair = ONE codepoint")
      (is (= "a\uFFFDb" out) "uncovered -> U+FFFD, position preserved")
      (is (= 3 (san/codepoint-count out)) "codepoint count preserved")
      (is (= 3 (san/op-advance-count {:text out})) "advance == codepoint count")))
  (testing "surrogate-pair advance on ops"
    (let [op {:text "\uD83D\uDD34\uD83D\uDFE2" :to 4}
          out (san/sanitize-op coverage san/fallback-cp op)]
      (is (= 2 (san/op-advance-count op)) "two astral cps = two advances, not four")
      (is (= "\uFFFD\uFFFD" (:text out)))
      (is (= 2 (:to out)) ":to tracks the shrunk UTF-16 length")))
  (testing "E-2: a literal U+FFFD in the source passes through as itself"
    (is (contains? coverage 0xFFFD) "fallback glyph itself is in the atlas")
    (is (= "\uFFFD" (san/sanitize-text coverage san/fallback-cp "\uFFFD"))))
  (testing "F-L1 (first light): control whitespace passes through, never tofu"
    (is (= "a\nb\tc\r" (san/sanitize-text coverage san/fallback-cp "a\nb\tc\r"))
        "newlines/tabs are the renderer's, not the sanitizer's")))

;; --- Gate 5: atlas coverage regression (P4: runs against the REGENERATED atlas) -----

(deftest atlas-coverage-regression-test
  (let [must-have (concat [0x2500 0x2502 0x2550 0x2551]       ; box drawing
                          [0x2190 0x2192 0x2194 0x21D2]       ; arrows
                          [0x2018 0x2019 0x201C 0x201D]       ; curly quotes
                          [0x2013 0x2014]                     ; dashes
                          [0x2713 0x2605 0x26A0]              ; check/star/warn
                          [0xFFFD])                           ; the fallback ITSELF
        ascii (range 32 127)]
    (doseq [cp must-have]
      (is (contains? coverage cp)
          (str "must-have codepoint U+" (Integer/toHexString cp) " present")))
    (testing "regen did not silently narrow: printable ASCII intact"
      (doseq [cp ascii]
        (is (contains? coverage cp))))))

;; --- Gate 6: clip containment (T-4) ---------------------------------------------------

(defn assert-rects-within [rects {:keys [x y w h]}]
  (doseq [r rects]
    (is (>= (:x r) x) (str "rect left >= clip left: " r))
    (is (>= (:y r) y) (str "rect top >= clip top: " r))
    (is (<= (+ (:x r) (:w r)) (+ x w)) (str "rect right <= clip right: " r))
    (is (<= (+ (:y r) (:h r)) (+ y h)) (str "rect bottom <= clip bottom: " r))))

(deftest clip-containment-test
  (testing "cards straddling a :clip? boundary emit clamped bg rects"
    (let [tree (rt/rt-node :clip-parent :box {:x 0 :y 0 :w 200 :h 100}
                           :clip? true
                           :style {:bg [0 0 0 1]}
                           :children
                           [(rt/rt-node :straddle-right :box {:x 150 :y 10 :w 100 :h 30}
                                        :style {:bg [1 0 0 1]})
                            (rt/rt-node :straddle-bottom :box {:x 10 :y 80 :w 50 :h 60}
                                        :style {:bg [0 1 0 1]})
                            (rt/rt-node :straddle-topleft :box {:x -20 :y -10 :w 60 :h 40}
                                        :style {:bg [0 0 1 1]})])
          rects (rt/tree->rects tree)
          child-rects (remove #(= :clip-parent (:id %)) rects)]
      (is (= 3 (count child-rects)) "all three straddlers still emit (visible)")
      (assert-rects-within child-rects {:x 0 :y 0 :w 200 :h 100})))
  (testing "timeline face bg rects within viewport clip at 3 scroll offsets"
    (let [s (timeline-scene)]
      (doseq [offset [0 120 400]]
        (let [clip {:x 0 :y 0 :w 800 :h 600}
              rects (rt/tree->rects s 0 (- offset) clip)]
          (is (seq rects) (str "something visible at offset " offset))
          (assert-rects-within rects clip))))))

;; --- Gate 7: lane/connector geometry ---------------------------------------------------

(deftest lane-connector-geometry-test
  (testing "lane assignment is deterministic"
    (let [entries (:feed/entries feed)]
      (is (= (lanes/assign-lanes entries) (lanes/assign-lanes entries)))
      (is (= (timeline-scene) (timeline-scene)) "same fixture -> identical scene twice"))
    (testing "family grouping: doc and its du: block share a lane"
      (let [entries (:feed/entries feed)
            l (lanes/assign-lanes entries)
            doc-lane (get l ["oc:doc:9fdoc" 1782171000000])
            blk-lane (get l ["du:9fdoc:000003" 1782166000000])]
        (is (some? doc-lane))
        (is (= doc-lane blk-lane))))
    (testing "F-L2 (first light): lane wrap is deterministic and bounded"
      (let [entries (:feed/entries feed)
            w (lanes/assign-lanes entries 2)]
        (is (= w (lanes/assign-lanes entries 2)))
        (is (every? #(< % 2) (vals w)) "wrapped lanes stay under max-lanes"))))
  (testing "connector endpoints touch their cards' bounds"
    (let [from-b {:x 10 :y 10 :w 100 :h 40}
          to-b {:x 200 :y 100 :w 100 :h 40}
          [spine elbow junction] (lanes/connector-rects
                                  {:kind :based-on :status :asserted} from-b to-b)
          from-cx (+ 10 50)
          to-cy (+ 100 20)]
      (is (= (float (:y spine)) (float 50.0)) "spine starts at from-card bottom edge")
      (is (<= (:x from-b) (:x spine) (+ (:x from-b) (:w from-b)))
          "spine x within from-card")
      (is (= (float (+ (:x elbow) (:w elbow))) (float 200.0))
          "elbow reaches the to-card left edge (endpoint touches)")
      (is (<= (:y to-b) (:y elbow) (+ (:y to-b) (:h to-b))) "elbow y within to-card")
      (is (some? junction))))
  (testing "retracted renders struck/dim, still visible"
    (let [rects (lanes/connector-rects {:kind :new-direction :status :retracted}
                                       {:x 0 :y 0 :w 10 :h 10}
                                       {:x 50 :y 50 :w 10 :h 10})]
      (is (= 3 (count rects)) "retracted still emits geometry")))
  (testing "dead-end lanes emit NO forward segment past the terminal card"
    (let [cards [{:entry-key :a :bounds {:x 0 :y 0 :w 100 :h 40} :dead-end? true}
                 {:entry-key :b :bounds {:x 0 :y 100 :w 100 :h 40} :dead-end? false}]
          lanes-map {:a 0 :b 0}]
      (is (empty? (lanes/lane-spines cards lanes-map))
          "spine stops at the dead-end card")
      (is (= 1 (count (lanes/lane-spines
                       (mapv #(assoc % :dead-end? false) cards) lanes-map)))
          "without the dead-end mark the spine connects them"))))

;; --- Gate 8: two clocks -------------------------------------------------------------------

(deftest two-clocks-test
  (let [back-dated (nth (:feed/entries feed) 1)]
    (testing "back-dated entry IS in the arrival window (selection is arrival-only)"
      (let [s (timeline-scene {:expanded #{} :order :arrival})
            card (find-node s #(= ["oc:doc:earlier" 1782172000000]
                                  (get-in % [:data :trail-face/entry-key])))]
        (is (some? card) "claimed-April entry renders in today's arrival window")))
    (testing "both stamps render when the clocks diverge"
      (let [stamp (cards/two-clock-stamp back-dated)]
        (is (str/includes? stamp "claimed 2026-04-24"))
        (is (str/includes? stamp "arrived 2026-06-22"))))
    (testing "nil claimed is honest"
      (let [stamp (cards/two-clock-stamp (nth (:feed/entries feed) 2))]
        (is (str/includes? stamp "claimed unknown"))))
    (testing ":order re-sorts WITHIN the window (never re-selects a claimed window)"
      (let [pos (fn [s k] (get-in (find-node s #(= k (get-in % [:data :trail-face/entry-key])))
                                  [:bounds :y]))
            sa (timeline-scene {:expanded #{} :order :arrival})
            sc (timeline-scene {:expanded #{} :order :claimed})
            bk ["oc:doc:earlier" 1782172000000]
            keys-of (fn [s] (into #{} (keep #(get-in % [:data :trail-face/entry-key]))
                                  (all-nodes s)))]
        (is (< (pos sa bk) (pos sa ["oc:doc:9fdoc" 1782171000000]))
            "arrival order: newest arrival first")
        (is (> (pos sc bk) (pos sc ["oc:doc:9fdoc" 1782171000000]))
            "claimed order: April claim sorts away from the top")
        (is (= (keys-of sa) (keys-of sc))
            "SAME entry set under both orders - ordering only, no claimed window")))
    (testing "the order param changes the rendered address line"
      (let [addr (fn [s] (edn/read-string (:text (first (address-ops (node-by-id s :trail-face/address-header))))))
            sa (timeline-scene {:expanded #{} :order :arrival})
            sc (timeline-scene {:expanded #{} :order :claimed})]
        (is (= :arrival (:order (second (addr sa)))))
        (is (= :claimed (:order (second (addr sc)))))
        (is (not= (addr sa) (addr sc)))))))

;; --- Gate 9: staleness triad -----------------------------------------------------------------

(deftest staleness-triad-test
  (let [recent (cards/staleness-treatment
                (get-in bundle [:bundle/targets "oc:doc:9fdoc" :times]) now-ms)
        stale (cards/staleness-treatment
               (get-in bundle [:bundle/targets "oc:doc:stale-doc" :times]) now-ms)
        never (cards/staleness-treatment
               (get-in bundle [:bundle/targets "oc:doc:never-doc" :times]) now-ms)]
    (is (= :attested-recent (:state recent)))
    (is (= :attested-stale (:state stale)))
    (is (= :never-attested (:state never)))
    (testing "three pairwise-distinct visual treatments"
      (is (distinct? (:rgba recent) (:rgba stale) (:rgba never)))
      (is (distinct? (:marker recent) (:marker stale) (:marker never))))
    (testing "nil must NOT read as fresh"
      (is (not= (:rgba never) (:rgba recent)))
      (is (str/includes? (:label never) "walked unknown")))))

;; --- Gate 10: badges ---------------------------------------------------------------------------

(deftest badges-test
  (testing "asserted-by always; written-by iff differs"
    (let [differs (cards/provenance-badges {:asserted-by "sid"
                                            :written-by "agent:claude-code/session-x"})
          same (cards/provenance-badges {:asserted-by "sid" :written-by "sid"})
          none (cards/provenance-badges {:asserted-by "sid" :written-by nil})]
      (is (= [:asserted-by :written-by] (mapv :badge differs)))
      (is (= [:asserted-by] (mapv :badge same)) "written-by hidden when equal")
      (is (= [:asserted-by] (mapv :badge none)))))
  (testing "disagreeing asserters BOTH render, badged, never merged"
    (let [vlines (cards/verdict-lines
                  (get-in bundle [:bundle/targets "oc:doc:9fdoc" :verdicts :current]))]
      (is (= 2 (count vlines)) "two current verdicts -> two lines, never merged")
      (is (every? :disagrees? vlines) "kinds differ on the same object -> both disagree")
      (is (every? #(str/includes? (:text %) "disagrees") vlines))
      (is (= #{"sid" "llm:opus-4-8/x"}
             (into #{} (map #(get-in % [:badges 0 :text])) vlines)))))
  (testing "edge row with divergent custody carries the written-by badge"
    (let [edge (first (get-in bundle [:bundle/targets "oc:doc:9fdoc"
                                      :relations :this :based-on]))
          badges (cards/provenance-badges edge)]
      (is (= 2 (count badges)))
      (is (str/includes? (:text (second badges)) "agent:claude-code/session-x")))))

;; --- Gate 11: omissions are pixels --------------------------------------------------------------

(deftest omissions-are-pixels-test
  (testing "every omission renders with its count"
    (let [om (get-in bundle [:bundle/targets "oc:doc:9fdoc" :omissions])
          lines (mapv cards/omission-line om)]
      (is (str/includes? (:text (first lines)) "3"))
      (is (str/includes? (:text (first lines)) "cap 10"))
      (is (str/includes? (:text (first lines)) "more") "resumable cursor affordance (E-15)")
      (is (str/includes? (:text (second lines)) "7"))))
  (testing "forced cap changes render - never silently narrow"
    (let [om (first (get-in bundle [:bundle/targets "oc:doc:9fdoc" :omissions]))]
      (is (not= (:text (cards/omission-line om))
                (:text (cards/omission-line (assoc om :dropped 99)))))))
  (testing "feed :feed/uncovered standing declarations render"
    (let [s (timeline-scene {:expanded #{} :order :arrival})
          om-node (find-node s #(= :omissions (:type %)))]
      (is (some? om-node))
      (is (= 2 (get-in om-node [:data :trail-face/omission-count])))
      (is (every? #(str/includes? (:text %) "uncovered") (:text om-node)))))
  (testing "bundle-level unrecognized-target omission renders in the expansion"
    (let [s (timeline-scene)
          exp (find-node s #(= :expansion (:type %)))
          om-node (find-node exp #(= :omissions (:type %)))]
      (is (some? om-node))
      (is (some #(str/includes? (:text %) "target/unrecognized") (:text om-node))
          "E-14: unknown ids render as omission chips, never crash"))))

;; --- Gate 12: hole endpoint -----------------------------------------------------------------------

(deftest hole-endpoint-test
  (testing "hole detection is total"
    (is (cards/hole-endpoint? {:kind :hole :id nil}))
    (is (cards/hole-endpoint? {:kind :doc :id nil}))
    (is (cards/hole-endpoint? nil))
    (is (not (cards/hole-endpoint? {:kind :doc :id "oc:doc:x"}))))
  (testing "the hole-shaped fixture row renders with the explicit hole style"
    (let [hole-edge (first (get-in bundle [:bundle/targets "oc:doc:9fdoc"
                                           :relations :this :references]))
          card (cards/hole-endpoint-card hole-edge geom)]
      (is (cards/hole-endpoint? (:to hole-edge)) "fixture row IS hole-shaped")
      (is (get-in card [:data :trail-face/hole?]))
      (is (str/includes? (:text (first (:text card))) "hole"))))
  (testing "the timeline expansion renders the hole row - no exception, no skip"
    (let [s (timeline-scene)
          holes (find-node s #(= :holes (:type %)))]
      (is (some? holes))
      (is (= 1 (count (:children holes))))))
  (testing "an empty/garbage row never crashes"
    (is (some? (cards/hole-endpoint-card {} geom)))))

;; --- Gate 13: cached scene identity -----------------------------------------------------------------

(deftest cached-scene-identity-test
  (let [s (timeline-scene)]
    (testing "two flattens from ONE scene value are stable"
      (is (= (rt/tree->rects s) (rt/tree->rects s)))
      (is (= (rt/tree->text-ops s) (rt/tree->text-ops s))))
    (testing "hit-test resolves against the SAME tree object the flatten used"
      (let [card (find-node s #(= :feed-card (:type %)))
            cx (+ (get-in card [:bounds :x]) 5)
            cy (+ (get-in card [:bounds :y]) 5)
            path (rt/hit-test s cx cy)]
        (is (some? path) "click lands")
        (is (identical? s (first path))
            "path root IS the cached scene object - no rebuild")
        (is (some #(get-in % [:data :trail-face/entry-key]) path)
            "the card node is on the hit path")))
    (testing "collapse/interactive ids are :trail-face/* namespaced (S5)"
      (doseq [n (all-nodes s)
              :when (get-in n [:data :trail-face/click])]
        (is (= "trail-face" (namespace (get-in n [:data :trail-face/click :action]))))))))

;; --- Gate 16: fixture fidelity (hand fixtures vs LIVE WP1 wrapper shapes) ----

(defn- shape-keys= [hand live label]
  (is (= (set (keys hand)) (set (keys live)))
      (str label " key sets match (hand vs live)")))

(deftest fixture-fidelity-test
  (let [rt-live (tv-mod/start-trail-view-runtime! {:tasks 2 :threads 2})]
    (try
      (let [fx (tvt/build-fixture! rt-live)
            {:keys [doc1-id conv*]} fx
            ;; two unknown classes (live-verified): a well-formed-but-absent
            ;; oc: id resolves as a :kind :unresolved target-bundle (dangling
            ;; is a view state); an UNPARSEABLE id lands in :bundle/omissions
            ;; as :target/unrecognized
            live-bundle (tv-mod/read-context-bundle
                         rt-live [doc1-id conv* "oc:doc:no-such-target"
                                  "garbage-unrecognizable-id"] {})
            live-tb (get-in live-bundle [:bundle/targets doc1-id])
            live-edge (first (mapcat identity
                                     (vals (get-in live-tb [:relations :this]))))
            live-verdict (first (first (vals (get-in live-tb [:verdicts :current]))))
            live-feed (tv-mod/read-recent-activity
                       rt-live {:from-ms 0 :to-ms 2000000000000} {})
            live-entry (first (:feed/entries live-feed))
            hand-tb (get-in bundle [:bundle/targets "oc:doc:9fdoc"])
            hand-edge (first (get-in hand-tb [:relations :this :based-on]))
            hand-verdict (first (get-in hand-tb [:verdicts :current "sid"]))
            hand-entry (first (:feed/entries feed))]
        (shape-keys= (dissoc bundle :fixture/now-ms) live-bundle "bundle top-level")
        (shape-keys= hand-tb live-tb "target-bundle (six layers)")
        (shape-keys= (:identity hand-tb) (:identity live-tb) "L0 identity")
        (shape-keys= (:material hand-tb) (:material live-tb) "L1 material")
        (shape-keys= (get-in hand-tb [:material :raw])
                     (get-in live-tb [:material :raw]) "L1 raw")
        (shape-keys= (first (get-in hand-tb [:material :anchors]))
                     (first (get-in live-tb [:material :anchors])) "L1 anchor")
        (shape-keys= (:structure hand-tb) (:structure live-tb) "L2 structure")
        (shape-keys= (first (get-in hand-tb [:structure :children]))
                     (first (get-in live-tb [:structure :children])) "L2 child")
        (shape-keys= (:times hand-tb) (:times live-tb) "L5 times")
        (is (some? live-edge) "live fixture produced a relation edge")
        (shape-keys= hand-edge live-edge "relation edge row")
        (is (some? live-verdict) "live fixture produced a current verdict")
        (shape-keys= hand-verdict live-verdict "verdict entry (stance edge + :current)")
        (shape-keys= (dissoc feed :fixture/now-ms) live-feed "feed top-level")
        (is (some? live-entry) "live feed non-empty")
        (shape-keys= hand-entry live-entry "feed entry")
        (shape-keys= (:entry/target hand-entry) (:entry/target live-entry) "entry target")
        (shape-keys= (:entry/actor hand-entry) (:entry/actor live-entry) "entry actor")
        (testing "unknown target -> :bundle/omissions entry, never silently dropped"
          (is (some #(= :target/unrecognized (:reason %))
                    (:bundle/omissions live-bundle)))
          (is (= :unresolved
                 (get-in live-bundle
                         [:bundle/targets "oc:doc:no-such-target" :kind]))
              "well-formed absent id renders as an :unresolved target - a view state"))
        (testing "standing feed uncovered declarations match live shape"
          (shape-keys= (first (:feed/omissions feed))
                       (first (:feed/omissions live-feed))
                       "feed uncovered omission")))
      (finally (tv-mod/close-trail-view-runtime! rt-live)))))
