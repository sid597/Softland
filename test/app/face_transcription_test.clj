(ns app.face-transcription-test
  "Framework Wave 2 — lane W2-E gates (CONTRACT §19 G22-G23) for the two
   design-round transcriptions: 1e Boxes + 1f Minimap+Reader (Sid's picks,
   2026-07-11), authored as assemblies over the REAL vocabulary
   (`resources/public/faces/{boxes-face,minimap-reader-face}.edn`).

   Run:
     clj -M:test -e \"(require 'app.face-transcription-test)
                      (clojure.test/run-tests 'app.face-transcription-test)\"

   G22 two faces compile + golden — each face compiles CLEAN against the real
      registry (V1-V7); golden rt-tree over the committed projection-shaped
      fixture (regen = explicit diff-reviewed act via `regen-goldens!`);
      apply-report honest and ZERO; positive content-h; every NEW primitive
      (:box :header-band :text-clip :sliver) carries a measured golden (G8
      discipline: wraps once where prose, :text-layout NOWHERE — covered at
      source grain by face_primitives_test/data-atoms over the whole file —
      resolve-layout -> tree->rects non-zero non-overlapping bounds; the ONE
      fallback-char-width constant). Includes the regression for the ONE named
      interpreter extension taken this wave (child content-w narrowing,
      W1-INT Lack 2 / CONTRACT §16): :child-w / :child-inset.
   G23 fidelity-to-design — per face, a NAMED anatomy checklist extracted from
      BlockExplorer.dc.html (each item cites the design source line) asserted
      against the golden tree (presence + relative geometry). Every design
      element the vocabulary/data CANNOT express is LOGGED as a named lack in
      build/framework/W2-E-plurality.md (D-005 ordering), never improvised."
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [app.client.workspace.rect-tree :as rt]
            [app.client.workspace.face-assembly :as fa]
            [app.client.workspace.face-primitives :as fp]))

;; ===========================================================================
;; Fixtures + view-ctx (the lane-A golden discipline)
;; ===========================================================================

(defn- read-fixture [rel]
  (edn/read-string (slurp (or (io/resource (str "app/fixtures/faces/" rel))
                              (str "test/app/fixtures/faces/" rel)))))

(defn- read-face [rel]
  (edn/read-string (slurp (or (io/resource (str "public/faces/" rel))
                              (str "resources/public/faces/" rel)))))

(def boxes-assembly   (delay (read-face "boxes-face.edn")))
(def minimap-assembly (delay (read-face "minimap-reader-face.edn")))
(def conversation     (delay (read-fixture "plurality-conversation.edn")))

;; char-advance = font-size * 0.56 (Ubuntu Sans Mono advance; the ONE
;; fallback-char-width law). content-w = pane width. now-ms = honest stamp.
(def geom
  {:viewport-w 800 :viewport-h 600
   :content-w 760 :font-size 14 :line-height 20
   :char-advance 7.84 :now-ms 0})

(def boxes-ctx   {:view-instance :boxes-pane   :address "chat:10c22f9b" :geom geom})
(def minimap-ctx {:view-instance :minimap-pane :address "chat:10c22f9b" :geom geom})

(defn build-boxes []
  (fa/apply-assembly (fa/compile-assembly fp/registry @boxes-assembly)
                     @conversation boxes-ctx))

(defn build-minimap []
  (fa/apply-assembly (fa/compile-assembly fp/registry @minimap-assembly)
                     @conversation minimap-ctx))

(defn regen-goldens!
  "Explicit, diff-reviewed regen act (CONTRACT G22 — the block-kernel goldens
   discipline). Never called by the suite."
  []
  (doseq [[f tree] [["boxes-face.golden.edn" (build-boxes)]
                    ["minimap-reader-face.golden.edn" (build-minimap)]]]
    (spit (str "test/app/fixtures/faces/" f)
          (str ";; GOLDEN — " f " rt-tree snapshot (CONTRACT §19 G22). Regen is an\n"
               ";; explicit diff-reviewed act: (app.face-transcription-test/regen-goldens!).\n"
               ";; DO NOT hand-edit.\n"
               (with-out-str (pprint/pprint tree))))))

;; ===========================================================================
;; Tree helpers
;; ===========================================================================

(defn walk-nodes [node]
  (when (map? node)
    (cons node (mapcat walk-nodes (:children node)))))

(defn find-nodes [tree pred] (filter pred (walk-nodes tree)))
(defn find-node  [tree pred] (first (find-nodes tree pred)))

(defn abs-nodes
  "Seq of {:node n :x abs-x :y abs-y} for every node of a RESOLVED tree."
  ([tree] (abs-nodes tree 0 0))
  ([node px py]
   (let [ax (+ px (get-in node [:bounds :x] 0))
         ay (+ py (get-in node [:bounds :y] 0))]
     (cons {:node node :x ax :y ay}
           (mapcat #(abs-nodes % ax ay) (:children node))))))

(defn- inside?
  "Child abs rect fully inside parent abs rect (closed comparison)."
  [{cx :x cy :y {{cw :w ch :h} :bounds} :node}
   {px :x py :y {{pw :w ph :h} :bounds} :node}]
  (and (>= cx px) (>= cy py)
       (<= (+ cx cw) (+ px pw))
       (<= (+ cy ch) (+ py ph))))

(defn- id-has? [node seg] (and (vector? (:id node)) (some #{seg} (:id node))))

(defn- flat-ops [tree] (mapcat identity (rt/tree->text-ops tree)))

(defn- approx= [a b] (< (Math/abs (double (- a b))) 0.01))

(def b3-text (-> @conversation :turns second :blocks second :text))

(defn- words-of [s] (count (re-seq #"\S+" s)))

(defn- sliver-h [words]
  (max 2 (min 26 (int (+ 0.5 (* 0.75 (Math/sqrt words)))))))

;; ===========================================================================
;; G22 — the two faces compile CLEAN + golden walk + honest report
;; ===========================================================================

(deftest g22-boxes-compiles-clean-and-golden
  (testing "Boxes: compile CLEAN against the REAL registry, golden-equal, honest report"
    (let [compiled (fa/compile-assembly fp/registry @boxes-assembly)
          tree     (build-boxes)
          golden   (read-fixture "boxes-face.golden.edn")]
      (is (not (fa/error? compiled)) "boxes-face.edn compiles clean (V1-V7)")
      (is (= golden tree)
          "apply output equals the committed golden (regen is an explicit diff-reviewed act)")
      (is (= {:items-without-id 0 :binds-missing 0}
             (get-in tree [:data :assembly/apply-report]))
          "apply-report has nothing to confess: every item carries :id, every bind resolves")
      (is (pos? (get-in tree [:data :assembly/content-h])) "positive content-h")
      (is (= (get-in tree [:bounds :h]) (get-in tree [:data :assembly/content-h]))
          "content-h = measured root height (§5 pane scroll contract)")
      (is (seq (rt/tree->rects tree)) "flattens to GPU rects")
      (is (seq (flat-ops tree)) "flattens to text ops"))))

(deftest g22-minimap-compiles-clean-and-golden
  (testing "Minimap+Reader: compile CLEAN, golden-equal, honest report"
    (let [compiled (fa/compile-assembly fp/registry @minimap-assembly)
          tree     (build-minimap)
          golden   (read-fixture "minimap-reader-face.golden.edn")]
      (is (not (fa/error? compiled)) "minimap-reader-face.edn compiles clean (V1-V7)")
      (is (= golden tree) "golden-equal")
      (is (= {:items-without-id 0 :binds-missing 0}
             (get-in tree [:data :assembly/apply-report]))
          "zero missing ids/binds over the fixture (fixture models the :reader-turn shape)")
      (is (pos? (get-in tree [:data :assembly/content-h])))
      (is (= (get-in tree [:bounds :h]) (get-in tree [:data :assembly/content-h])))
      (is (seq (rt/tree->rects tree)))
      (is (seq (flat-ops tree))))))

;; ===========================================================================
;; G22 — measured goldens for the four NEW primitives (G8 discipline)
;; ===========================================================================

(defn- ctx-for [id] {:id id :view-instance :test :address [:test] :geom geom})

(deftest g22-box-measures
  (testing ":box measures h = padding + children + gaps; stacks non-overlapping (measure rule, T7)"
    (let [child (fn [seg h] (rt/rt-node [:root :bx seg] :text-run
                                        {:x 0 :y 0 :w 700 :h h} :text []))
          kids  [(child :a 40) (child :b 60)]
          node  (fp/box-prim (ctx-for [:root :bx])
                             {:padding [11 12 12 12] :gap 8 :radius 6
                              :border-width 1 :bg [0.1 0.1 0.1 1.0]}
                             kids)]
      (is (= :box (:type node)))
      (is (= (+ 11 12 40 60 8) (get-in node [:bounds :h]))
          ":h = pt + pb + sum children + gap")
      (is (= 760 (get-in node [:bounds :w])) "width flows down from geom content-w")
      (is (= 6 (get-in node [:style :radius])))
      (is (= 1 (get-in node [:style :border-width])))
      (let [laid (rt/resolve-layout node)
            [a b] (:children laid)]
        (is (= [12 11] [(get-in a [:bounds :x]) (get-in a [:bounds :y])])
            "first child lands inside the padding")
        (is (= (+ 11 40 8) (get-in b [:bounds :y])) "children stack with the gap")
        (is (every? #(pos? (get-in % [:bounds :h])) (:children laid)))
        (is (seq (rt/tree->rects laid)) "renders (non-zero bounds through tree->rects)")))))

(deftest g22-header-band-measures
  (testing ":header-band emits its own ops; meta RIGHT-ALIGNED (the design flex-spacer, BlockExplorer:281)"
    (let [node (fp/header-band-prim (ctx-for [:root :hb])
                                    {:h 26 :label-prefix "turn " :label 1
                                     :sub "sid"
                                     :count-of [{:id 1} {:id 2} {:id 3}]
                                     :count-suffix " blocks"}
                                    [])
          [lbl sub meta*] (:text node)
          fs   10                                  ;; (:xs font-sizes) default
          ca   (* fs (/ 7.84 14))]                 ;; geom advance ratio, F10 law
      (is (= :header-band (:type node)))
      (is (= 26 (get-in node [:bounds :h])) "fixed band height (measure rule)")
      (is (= "turn 1" (:text lbl)) ":label-prefix + :label")
      (is (= "sid" (:text sub)))
      (is (= "3 blocks" (:text meta*)) ":count-of computed INSIDE the primitive")
      (is (approx= (+ (:x meta*) (* (count "3 blocks") ca)) (- 760 10))
          "meta right-aligned at w - 10 (the flex:1 spacer made flesh)")
      (is (empty? (:children node)) "no dot without :dot-color/:kind"))
    (testing ":words-of computes word counts inside the primitive (INTENT.md rule)"
      (let [node (fp/header-band-prim (ctx-for [:root :hb2])
                                      {:label "k" :words-of "one two three four"} [])]
        (is (= "4w" (:text (last (:text node)))))))
    (testing ":kind derives the dot + label color from the palette (design b.color, :321-322)"
      (let [node (fp/header-band-prim (ctx-for [:root :hb3])
                                      {:label "code" :kind "code"} [])
            dot  (first (:children node))]
        (is (= :header-dot (:type dot)) "leading role dot present (:287)")
        (is (= (assoc (get fp/sliver-palette "code") 3 1.0)
               (get-in dot [:style :bg])) "dot colored by kind")
        (is (true? (get-in dot [:data :layout-skip?])))
        (let [lbl (first (:text node))]
          (is (approx= (nth (get fp/sliver-palette "code") 0) (:r lbl))
              "label rides the kind color"))))))

(deftest g22-text-clip-measures
  (testing ":text-clip wraps ONCE, clips at :max-lines, shows an honest stub (intent 'clip at ~6 lines'; :333-338/:488)"
    (let [w         696
          pad       [6 9 8 9]
          avail     (- w 18)
          max-chars (int (/ avail 7.84))
          all-lines (vec (mapcat #(rt/wrap-line % max-chars) (str/split-lines b3-text)))
          node      (fp/text-clip-prim (ctx-for [:root :tc])
                                       {:value b3-text :max-lines 6 :padding pad :w w}
                                       [])]
      (is (> (count all-lines) 6) "fixture prose actually exceeds the clip")
      (is (true? (get-in node [:data :text/clipped?])))
      (is (= (count all-lines) (get-in node [:data :text/lines-total]))
          "the data does not lie about the total")
      (is (= 6 (get-in node [:data :text/lines-shown])))
      (is (= 7 (count (:text node))) "6 shown lines + 1 stub op")
      (is (= (mapv :text (take 6 (:text node))) (vec (take 6 all-lines)))
          "shown lines are the one-wrap prefix (wrapped ONCE via wrap-line)")
      (is (str/starts-with? (:text (last (:text node))) "▸ ")
          "the visible remainder stub (the design's ▸ measured stub, :488)")
      (is (str/includes? (:text (last (:text node)))
                         (str (- (count all-lines) 6) " more lines"))
          "stub names the honest remainder")
      (is (= (+ 6 8 (* 7 20)) (get-in node [:bounds :h]))
          ":h = pt + pb + (shown+stub) x line-height (measure rule)")
      (is (nil? (:text-layout node)) "never rides the dead :text-layout hook")
      ;; ops stack, positive, renderable (no bg -> no rects, but the walks
      ;; must succeed and the ops must flatten — the G8 text-run precedent)
      (let [ys   (map :y (:text node))
            laid (rt/resolve-layout node)]
        (is (= ys (sort ys)))
        (is (vector? (rt/tree->rects laid)) "tree->rects walk succeeds (render-compatible)")
        (is (= 7 (count (mapcat identity (rt/tree->text-ops laid))))
            "all 7 ops flatten through tree->text-ops"))
      (testing "short text does not clip and carries honest data"
        (let [short (fp/text-clip-prim (ctx-for [:root :tc2])
                                       {:value "one line" :max-lines 6} [])]
          (is (false? (get-in short [:data :text/clipped?])))
          (is (= 1 (count (:text short)))
              "no stub op when nothing is clipped"))))))

(deftest g22-sliver-measures
  (testing ":sliver h = clamp(round(sqrt(words)*0.75), 2, 26) — the design formula verbatim (:795)"
    (let [mk (fn [text kind] (fp/sliver-prim (ctx-for [:root :sv])
                                             {:text text :kind kind} []))]
      (doseq [[text expected-words]
              [["" 0]
               ["three little words" 3]
               [(str/join " " (repeat 100 "w")) 100]
               [(str/join " " (repeat 2000 "w")) 2000]]]
        (let [node (mk text "text")
              w*   (get-in node [:data :sliver/words])]
          (is (= expected-words w*) "words computed inside the primitive")
          (is (= (sliver-h w*) (get-in node [:bounds :h]))
              "height follows the design formula")))
      (is (= 2 (get-in (mk "" "text") [:bounds :h])) "lower clamp 2px")
      (is (= 26 (get-in (mk (str/join " " (repeat 2000 "w")) "text") [:bounds :h]))
          "upper clamp 26px")
      (testing "kind-coded colors (design :814); unknown kind takes the neutral default"
        (let [bg (fn [k] (get-in (mk "some words here" k) [:style :bg]))]
          (is (not= (bg "code") (bg "text")))
          (is (not= (bg "heading") (bg "human-message")))
          (is (= [0.42 0.51 0.60 0.6] (bg "no-such-kind")))
          (is (= (bg "code") (bg :code)) "keyword and string kinds normalize")))
      (testing "renders: non-zero bounds through tree->rects"
        (let [node (mk "a few words" "text")]
          (is (pos? (get-in node [:bounds :h])))
          (is (seq (rt/tree->rects node))))))))

;; ===========================================================================
;; G22 — regression for the ONE named interpreter extension taken this wave:
;; child content-w narrowing (:child-w / :child-inset), W1-INT Lack 2, §16.
;; ===========================================================================

(def ^:private narrow-prose
  (str "The strip narrows and the reader narrows differently, so prose must "
       "wrap at the width of the pane that actually holds it — never at the "
       "full content width of the whole face, which was the W1 worn-face "
       "symptom this named extension exists to close."))

(deftest g22-child-content-w-narrowing
  (let [asm  (fn [props]
               {:assembly/name "narrow" :assembly/grammar 0
                :root {:prim :stack :props props
                       :children [{:prim :text-run :props {:value narrow-prose}}]}})
        run  (fn [props]
               (fa/apply-assembly (fa/compile-assembly fp/registry (asm props))
                                  {} boxes-ctx))
        lines (fn [tree] (count (:text (first (:children tree)))))]
    (testing "no narrowing props -> W1 behavior unchanged (child wraps at content-w)"
      (let [tree (run {})]
        (is (= 760 (get-in tree [:children 0 :bounds :w])))
        (is (= (count (rt/wrap-line narrow-prose (int (/ 760 7.84))))
               (lines tree)))))
    (testing ":child-inset narrows the SUBTREE's content-w relatively"
      (let [tree (run {:child-inset 160})]
        (is (= 760 (get-in tree [:bounds :w]))
            "the node ITSELF builds at the parent's width")
        (is (= 600 (get-in tree [:children 0 :bounds :w]))
            "children build at content-w - inset")
        (is (= (count (rt/wrap-line narrow-prose (int (/ 600 7.84))))
               (lines tree))
            "prose wraps at the narrowed width")
        (is (> (lines tree) (lines (run {}))) "narrower pane -> more wrapped lines")))
    (testing ":child-w sets the subtree's content-w absolutely"
      (let [tree (run {:child-w 134})]
        (is (= 134 (get-in tree [:children 0 :bounds :w])))
        (is (= (count (rt/wrap-line narrow-prose (int (/ 134 7.84))))
               (lines tree)))))
    (testing "narrowing propagates through :each (the minimap strip shape)"
      (let [asm2 {:assembly/name "narrow-each" :assembly/grammar 0
                  :root {:prim :stack :props {:child-w 124}
                         :children [{:each [:items]
                                     :template {:prim :sliver
                                                :props {:text {:bind [:text]}}}}]}}
            tree (fa/apply-assembly (fa/compile-assembly fp/registry asm2)
                                    {:items [{:id "a" :text "x y z"}]} boxes-ctx)]
        (is (= 124 (get-in tree [:children 0 :bounds :w]))
            "an :each item's node builds at the ancestor's narrowed width")))))

;; ===========================================================================
;; G23 — Boxes: the NAMED anatomy checklist (each item cites the design source)
;; ===========================================================================

(defn- turn-frame? [n] (and (= :box (:type n))
                            (= 10 (get-in n [:style :radius]))
                            (some? (get-in n [:style :bg]))))  ;; the page-end box is unfilled
(defn- block-card? [n] (and (= :box (:type n)) (= 6 (get-in n [:style :radius]))))

(deftest g23-boxes-anatomy
  (let [tree  (build-boxes)
        turns (:turns @conversation)
        abs   (abs-nodes tree)
        frames (filter (comp turn-frame? :node) abs)
        cards  (filter (comp block-card? :node) abs)]
    (testing "B1 · one russian-doll turn frame per turn — bordered, rounded 10 (BlockExplorer:277)"
      (is (= (count turns) (count frames)))
      (doseq [t turns]
        (is (some #(id-has? (:node %) (:id t)) frames)
            (str "a turn frame carries turn id " (:id t)))))
    (testing "B2 · each frame OPENS with a header band: bg tint + bottom hairline + speaker + 'N blocks' meta (:278-283)"
      (doseq [f frames]
        (let [hdr (first (:children (:node f)))]
          (is (= :header-band (:type hdr)) "first child is the header band")
          (is (some? (get-in hdr [:style :bg])) "header band bg tint (:278)")
          (is (= [0 0 1 0] (get-in hdr [:style :border-widths])) "bottom hairline (:278)")))
      (let [hdr-texts (map (fn [f] (mapv :text (:text (first (:children (:node f)))))) frames)]
        (is (= [["turn 1" "sid" "1 blocks"]
                ["turn 2" "claude" "3 blocks"]
                ["turn 3" "sid" "1 blocks"]]
               hdr-texts)
            "label 'turn N' + speaker sub + right meta 'N blocks' (:279-282)")))
    (testing "B3 · containment made literal: every block card nests INSIDE its turn frame (intent line; :285-343)"
      (is (= 5 (count cards)) "one card per fixture block")
      (doseq [c cards]
        (is (some #(inside? c %) frames)
            (str "block card " (:id (:node c)) " sits inside a turn frame"))))
    (testing "B4 · block-card header: kind-colored dot + kind label + words meta (:320-329, :486)"
      (doseq [c cards]
        (let [hdr (first (:children (:node c)))]
          (is (= :header-band (:type hdr)))
          (is (= :header-dot (:type (first (:children hdr)))) "role dot (:321)")))
      (let [b3-card (find-node tree #(and (block-card? %) (id-has? % "b3")))
            hdr     (first (:children b3-card))
            [lbl _meta] (:text hdr)]
        (is (= "text" (:text lbl)) "label = block :kind (:322)")
        (is (approx= (nth (get fp/sliver-palette "text") 0) (:r lbl))
            "label colored by kind (b.color, :322)")
        (is (= (str (words-of b3-text) "w") (:text (last (:text hdr))))
            "meta chip = word count, computed in the primitive (:486)")))
    (testing "B5 · long blocks clip at ~6 lines with a visible measured stub; short blocks don't (intent; :333-338, :488)"
      (let [clip (fn [seg] (find-node tree #(and (= :text-clip (:type %)) (id-has? % seg))))
            long-clip  (clip "b3")
            code-clip  (clip "b4")
            short-clip (clip "b1")]
        (is (true? (get-in long-clip [:data :text/clipped?])) "the monster prose block clips")
        (is (= 6 (get-in long-clip [:data :text/lines-shown])))
        (is (str/starts-with? (:text (last (:text long-clip))) "▸ ") "visible ▸ stub")
        (is (true? (get-in code-clip [:data :text/clipped?])) "the 10-line code block clips")
        (is (false? (get-in short-clip [:data :text/clipped?])) "one-liners stay whole")))
    (testing "B6 · vertical rhythm: frames stack non-overlapping with the 16px gap (:277 margin-bottom:16px)"
      (let [kids (:children tree)
            tops (map #(get-in % [:bounds :y]) kids)]
        (is (= tops (sort tops)))
        (doseq [[a b] (partition 2 1 kids)]
          (is (= (+ (get-in a [:bounds :y]) (get-in a [:bounds :h]) 16)
                 (get-in b [:bounds :y]))
              "next frame starts exactly one gap below"))))
    (testing "B7 · page-end honesty: the view ENDS with the explicit paging answer (frame page verbatim; :347)"
      (let [tail (last (:children tree))]
        (is (= :box (:type tail)))
        (is (some #(str/includes? (str (:text %)) "river-page-has-no-cursor")
                  (mapcat :text (walk-nodes tail)))
            "the honest :conversation/paging-lack renders, never a silent cut")))
    (testing "B8 · geometry honesty: everything visible stays inside the pane width"
      (doseq [{:keys [node x]} (filter #(get-in % [:node :style :bg]) abs)]
        (is (<= (+ x (get-in node [:bounds :w])) 760.01)
            (str (:id node) " stays inside content-w"))))))

;; ===========================================================================
;; G23 — Minimap+Reader: the NAMED anatomy checklist (design-source cites)
;; ===========================================================================

(deftest g23-minimap-anatomy
  (let [tree    (build-minimap)
        abs     (abs-nodes tree)
        strip   (find-node tree #(and (= :box (:type %)) (= 152 (get-in % [:bounds :w]))))
        reader  (second (:children tree))
        slivers (find-nodes tree #(= :sliver (:type %)))
        rblocks (:blocks (:reader-turn @conversation))]
    (testing "M1 · two panes: fixed 152px strip left, reader right, no overlap, sum = pane (:353-354)"
      (is (some? strip) "the strip is a 152px-wide box (:354 width:152px;flex:none)")
      (is (= 0 (get-in strip [:bounds :x])) "strip at the left edge")
      (is (= 160 (get-in reader [:bounds :x])) "reader starts after strip + 8px gap")
      (is (= 600 (get-in reader [:bounds :w])) "reader takes the rest: 760 - 152 - 8")
      (is (= [0 1 0 0] (get-in strip [:style :border-widths])) "strip right hairline (:354)"))
    (testing "M2 · the strip header names the density law: 'map · h ∝ √words' (:355)"
      (let [hdr (first (:children strip))]
        (is (= :header-band (:type hdr)))
        (is (= "map · h ∝ √words" (:text (first (:text hdr)))))))
    (testing "M3 · one group card per turn: T-label + words meta + sliver column (:357-368)"
      (let [groups (filter #(and (= :box (:type %)) (= 4 (get-in % [:style :radius]))
                                 ;; the page-2 box is also radius 4 but holds no slivers
                                 (some (fn [n] (= :sliver (:type n))) (walk-nodes %)))
                           (:children strip))]
        (is (= 3 (count groups)) "one group per fixture turn")
        (doseq [[g t] (map vector groups (:turns @conversation))]
          (let [hdr (first (:children g))]
            (is (= (str "T" (:order t)) (:text (first (:text hdr)))) "T{n} label (:360)")
            (is (= (str (reduce + (map (comp words-of :text) (:blocks t))) "w")
                   (:text (last (:text hdr))))
                "per-turn words meta, computed in the primitive (:361)")
            (is (= (count (:blocks t))
                   (count (filter #(= :sliver (:type %)) (walk-nodes g))))
                "one sliver per block (:364)")))))
    (testing "M4 · sliver heights follow h ∝ √words — the design formula (:795) — so density IS navigable"
      (is (= 5 (count slivers)) "one sliver per fixture block, all in the strip — the reader has none")
      (doseq [s slivers]
        (is (= (sliver-h (get-in s [:data :sliver/words])) (get-in s [:bounds :h]))
            "every sliver height = clamp(round(sqrt(words)*0.75),2,26)"))
      (let [h-of (fn [seg] (get-in (find-node tree #(and (= :sliver (:type %)) (id-has? % seg)))
                                   [:bounds :h]))]
        (is (> (h-of "b3") (h-of "b2"))
            "the monster block reads taller than the heading — density is visible as shape")))
    (testing "M5 · slivers are kind-coded (:814)"
      (let [bgs (set (map #(get-in % [:style :bg]) slivers))]
        (is (>= (count bgs) 3) "at least kind-count distinct colors across the fixture")))
    (testing "M6 · strip ends with the page-2 honesty box (:370)"
      (let [tail (last (:children strip))]
        (is (= :box (:type tail)))
        ;; the 134px strip hard-wraps the keyword across ops — join before match
        (is (str/includes? (apply str (map :text (mapcat :text (walk-nodes tail))))
                           "river-page-has-no-cursor"))))
    (testing "M7 · reader top bar: 34px band naming the focused turn + speaker + words (:374-378)"
      (let [bar (first (:children reader))]
        (is (= :header-band (:type bar)))
        (is (= 34 (get-in bar [:bounds :h])))
        (is (= ["turn 2" "claude"] (mapv :text (take 2 (:text bar))))
            "the focused turn named in the bar")))
    (testing "M8 · the reader shows exactly ONE turn (intent: 'one turn fully expanded on the right')"
      (let [reader-cards (filter #(and (= :box (:type %)) (= 6 (get-in % [:style :radius])))
                                 (walk-nodes reader))]
        (is (= (count rblocks) (count reader-cards)) "one card per focused-turn block, no more")
        (let [reader-ops (map :text (mapcat :text (walk-nodes reader)))]
          (is (not-any? #(str/includes? (str %) "Evidence before architecture") reader-ops)
              "other turns' prose does NOT appear in the reader"))))
    (testing "M9 · full fidelity: the monster block renders EVERY wrapped line in the reader (:422-424; judge line)"
      (let [tr (find-node reader #(and (= :text-run (:type %)) (id-has? % "b3")))
            max-chars (int (/ (- 576 20) 7.84))
            expected  (count (mapcat #(rt/wrap-line % max-chars) (str/split-lines b3-text)))]
        (is (some? tr) "the long block is a FULL :text-run in the reader, never :text-clip")
        (is (= expected (count (:text tr))) "all wrapped lines present — nothing clipped")
        (is (> expected 6) "and it is genuinely longer than the Boxes clip")))
    (testing "M10 · wrap-width honesty (the narrowing extension at face grain): strip content stays in the strip, reader text stays in the pane"
      (doseq [{:keys [node x]} (filter #(= :sliver (:type (:node %))) abs)]
        (is (<= (+ x (get-in node [:bounds :w])) 152.01) "slivers never leak out of the strip"))
      (doseq [op (flat-ops tree)]
        (is (<= (+ (:x op) (* (count (:text op)) (:size op) 0.56)) 760.5)
            "every text op ends inside the pane"))
      (doseq [{:keys [node x]} (filter #(get-in % [:node :style :bg]) abs)]
        (is (<= (+ x (get-in node [:bounds :w])) 760.01))))))
