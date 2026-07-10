(ns app.face-assembly-test
  "Framework Wave 1, lane A — the golden harness + acceptance gates G1-G6 for the
   two-stage assembly interpreter (CONTRACT §11). JVM-side, no browser (the
   block-kernel goldens discipline applied to UI). Run:

     clj -M:test -e \"(require 'app.face-assembly-test)
                      (clojure.test/run-tests 'app.face-assembly-test)\"

   The interpreter is tested against 2-3 LOCALLY-DEFINED stub primitives
   implementing the fixed §6 builder-fn interface (fn [ctx props children] ->
   rt-node). The interface is contract-fixed, so goldens written against stubs
   stay valid when lane B's real vocabulary arrives (§2 fence: lane A does NOT
   wait on lane B)."
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [app.client.workspace.rect-tree :as rt]
            [app.client.workspace.face-assembly :as fa]))

;; ===========================================================================
;; Stub primitives — the §6 builder-fn interface (fn [ctx props children])
;; ===========================================================================
;; ctx      {:id <node-id> :view-instance <kw> :address <addr> :geom <geom> :prim <kw>}
;; props    resolved props (binds already substituted; may contain nils)
;; children vector of already-built child rt-nodes (post-order walk)
;;
;; Each stub follows the §6 MEASURE RULE: it computes its own :w/:h BEFORE
;; returning, so the parent can stack it and resolve-layout can arrange it
;; (measure in the primitive, arrange in the engine — trap T7). Width flows down
;; via (:content-w geom); nested indentation is realized as x-offset from the
;; parent's :layout :padding (NOT by shrinking child width — that is lane B's
;; :indent-rail concern). Text-runs wrap ONCE and emit their own positioned ops;
;; NO stub touches :text-layout (the dead hook, §6/PROBE Finding 1, trap T7).

(defn- child-h [c] (get-in c [:bounds :h] 0))

(defn stack-stub
  "A bare layout container. :dir :v (column, default) or :h (row). Measures its
   own height from its children (column: sum + gaps; row: max)."
  [ctx props children]
  (let [geom (:geom ctx)
        w    (or (:w props) (:content-w geom 760))
        dir  (case (:dir props) (:h :row) :row :column)
        gap  (or (:gap props) 0)
        n    (count children)
        h    (if (= dir :column)
               (+ (reduce + 0 (map child-h children)) (* gap (max 0 (dec n))))
               (reduce max 0 (map child-h children)))]
    (rt/rt-node (:id ctx) :stack
                {:x 0 :y 0 :w w :h h}
                :layout {:direction dir :gap gap}
                :children (vec children))))

(defn card-stub
  "A bordered container with an optional header label line (as a prepended child
   node) + its children stacked in a column with padding. :variant :turn/:block
   only tints the background. Measures its own height from header + children."
  [ctx props children]
  (let [geom  (:geom ctx)
        id    (vec (:id ctx))
        w     (or (:w props) (:content-w geom 760))
        lh    (:line-height geom 20)
        fs    (:font-size geom 14)
        pad   (or (:pad props) 10)
        gap   (or (:gap props) 6)
        label (:label props)
        header (when (some? label)
                 (rt/rt-node (conj id :hdr) :card-header
                             {:x 0 :y 0 :w w :h (+ lh 4)}
                             :text [{:text (str label) :type :keyword
                                     :x 0 :y (- lh 2) :size fs
                                     :r 0.55 :g 0.75 :b 1.0 :a 1.0}]))
        kids  (if header (into [header] children) (vec children))
        h     (+ (* 2 pad)
                 (reduce + 0 (map child-h kids))
                 (* gap (max 0 (dec (count kids)))))
        bg    (case (:variant props)
                :turn  [0.12 0.13 0.16 1.0]
                :block [0.10 0.11 0.13 1.0]
                [0.10 0.11 0.13 1.0])]
    (rt/rt-node id :card
                {:x 0 :y 0 :w w :h h}
                :style {:bg bg :radius 6 :border-width 1 :border-color [0.25 0.27 0.32 1.0]}
                :layout {:direction :column :gap gap :padding pad}
                :children kids)))

(defn text-run-stub
  "Prose leaf. Wraps :value ONCE via rt/wrap-line, emits its own positioned text
   ops, and sets :h = wrapped-line-count * line-height + padding (the §6 measure
   rule; NO :text-layout)."
  [ctx props children]
  (let [geom  (:geom ctx)
        w     (or (:w props) (:content-w geom 760))
        fs    (:font-size geom 14)
        lh    (:line-height geom 20)
        ca    (:char-advance geom 7.84)
        pad   4
        value (str (:value props ""))
        mc    (max 1 (int (/ (- w (* 2 pad)) ca)))
        raw   (str/split-lines value)
        lines (into [] (mapcat #(rt/wrap-line % mc)) raw)
        n     (max 1 (count lines))
        color (or (:color props) [0.85 0.87 0.90 1.0])
        h     (+ (* 2 pad) (* n lh))
        ops   (into []
                    (map-indexed
                     (fn [i ln]
                       {:text ln :type :text
                        :x pad :y (+ pad fs (* i lh)) :size fs
                        :r (nth color 0) :g (nth color 1) :b (nth color 2) :a (nth color 3)}))
                    lines)]
    (rt/rt-node (:id ctx) :text-run
                {:x 0 :y 0 :w w :h h}
                :text ops)))

(def stub-registry
  {:stack    stack-stub
   :card     card-stub
   :text-run text-run-stub})

;; ===========================================================================
;; Fixtures + view-ctx
;; ===========================================================================

(defn- read-fixture [rel]
  (edn/read-string (slurp (or (io/resource (str "app/fixtures/faces/" rel))
                              (str "test/app/fixtures/faces/" rel)))))

(def outline-assembly (delay (read-fixture "outline.edn")))
(def outline-conversation (delay (read-fixture "outline-conversation.edn")))

;; char-advance = font-size * 0.56 (Ubuntu Sans Mono glyph advance, pixels-per-
;; char; editor_compute.cljs:290). content-w = pane width. now-ms = honest stamp.
(def geom
  {:viewport-w 800 :viewport-h 600
   :content-w 760 :font-size 14 :line-height 20
   :char-advance 7.84 :now-ms 0})

(def view-ctx
  {:view-instance :outline-pane :address "conv:7c80ce2a" :geom geom})

(defn build-outline
  "Compile the Outline assembly once, apply it to the committed conversation —
   the exact tree the golden captures. Also the golden regen entry point:
     (spit \"test/app/fixtures/faces/outline.golden.edn\"
           (with-out-str (clojure.pprint/pprint (build-outline))))"
  []
  (fa/apply-assembly (fa/compile-assembly stub-registry @outline-assembly)
                     @outline-conversation view-ctx))

;; ===========================================================================
;; Tree helpers
;; ===========================================================================

(defn walk-nodes
  "Lazy seq of every rt-node in a tree (self + descendants, pre-order)."
  [node]
  (when (map? node)
    (cons node (mapcat walk-nodes (:children node)))))

(defn find-node [tree pred]
  (first (filter pred (walk-nodes tree))))

(defn all-heights-positive? [node]
  (and (pos? (get-in node [:bounds :h] 0))
       (every? all-heights-positive? (:children node))))

;; ===========================================================================
;; G1 — golden walk
;; ===========================================================================

(deftest g1-golden-walk
  (testing "Outline fixture + committed data -> compile -> apply == committed golden"
    (let [tree   (build-outline)
          golden (read-fixture "outline.golden.edn")]
      (is (= golden tree)
          "apply-assembly output must equal the committed golden EDN (regen is an explicit diff-reviewed act)")
      ;; the golden is a real, renderable scene, not a curiosity
      (is (= :stack (:type tree)) "root is the :stack primitive")
      (is (= 3 (count (:children tree))) ":each [:turns] expanded to 3 turn cards")
      (is (all-heights-positive? tree) "every node measured a positive :h (measure rule, T7)")
      (is (seq (rt/tree->rects tree)) "the golden flattens to GPU rects")
      (is (seq (rt/tree->text-ops tree)) "the golden flattens to text ops"))))

;; ===========================================================================
;; G2 — the guard bites (five rejection classes)
;; ===========================================================================

(defn- compile-bad [assembly]
  (fa/compile-assembly stub-registry assembly))

(defn- error-msgs [compiled]
  (map :msg (fa/compile-errors compiled)))

(deftest g2-guard-bites
  (testing "(a) a list anywhere rejects at compile into an error-card builder"
    (let [c (compile-bad {:assembly/name "x" :assembly/grammar 0
                          :root {:prim :stack :props {:bad (list 1 2 3)} :children []}})]
      (is (fa/error? c))
      (is (some #(and (str/includes? % "guard") (str/includes? % "list")) (error-msgs c)))
      (is (= :error-card (:type (fa/apply-assembly c {} view-ctx))))))
  (testing "(b) a symbol anywhere rejects"
    (let [c (compile-bad {:assembly/name "x" :assembly/grammar 0
                          :root {:prim :stack :props {:bad 'a-symbol} :children []}})]
      (is (fa/error? c))
      (is (some #(and (str/includes? % "guard") (str/includes? % "symbol")) (error-msgs c)))
      (is (= :error-card (:type (fa/apply-assembly c {} view-ctx))))))
  (testing "(c) an unknown node key rejects"
    (let [c (compile-bad {:assembly/name "x" :assembly/grammar 0
                          :root {:prim :stack :bogus 1 :children []}})]
      (is (fa/error? c))
      (is (some #(str/includes? % "unknown node keys") (error-msgs c)))
      (is (= :error-card (:type (fa/apply-assembly c {} view-ctx))))))
  (testing "(d) both :prim and :each on one node rejects"
    (let [c (compile-bad {:assembly/name "x" :assembly/grammar 0
                          :root {:prim :stack :each [:xs] :template {:prim :text-run}}})]
      (is (fa/error? c))
      (is (some #(str/includes? % "both :prim and :each") (error-msgs c)))
      (is (= :error-card (:type (fa/apply-assembly c {} view-ctx))))))
  (testing "(e) a nested bind rejects"
    (let [c (compile-bad {:assembly/name "x" :assembly/grammar 0
                          :root {:prim :text-run :props {:value [{:bind [:x]}]}}})]
      (is (fa/error? c))
      (is (some #(str/includes? % "nested :bind") (error-msgs c)))
      (is (= :error-card (:type (fa/apply-assembly c {} view-ctx)))))))

;; ===========================================================================
;; G3 — error-card totality (incl. the corrupted-registry case)
;; ===========================================================================

(defn- valid-error-card? [tree name-substr]
  (and (= :error-card (:type tree))
       ;; renderable: both walks succeed and produce output
       (seq (rt/tree->rects tree))
       ;; carries name + address + <=5 errors
       (let [txt (str/join " " (map :text (flatten (rt/tree->text-ops tree))))]
         (and (str/includes? txt name-substr)
              (str/includes? txt (pr-str (:address view-ctx)))))
       (<= (count (:assembly/errors (:data tree))) 5)))

(deftest g3-error-card-totality
  (testing "a malformed assembly applies to a VALID renderable error card; nothing throws"
    (let [c    (fa/compile-assembly stub-registry {:root 42})   ; :root is not a node, no name
          tree (fa/apply-assembly c {} view-ctx)]
      (is (= :error-card (:type tree)))
      (is (seq (rt/tree->rects tree)))
      (is (>= 5 (count (:assembly/errors (:data tree)))))))
  (testing "an unknown-prim assembly applies to a valid error card naming the prim"
    (let [c    (fa/compile-assembly stub-registry
                                    {:assembly/name "unknown-face" :assembly/grammar 0
                                     :root {:prim :does-not-exist :props {} :children []}})
          tree (fa/apply-assembly c {} view-ctx)]
      (is (valid-error-card? tree "unknown-face"))
      (is (some #(str/includes? % "unknown :prim") (error-msgs c)))))
  (testing "the error-card path is REGISTRY-INDEPENDENT — a corrupted registry still error-cards, never throws"
    (let [corrupted {:stack "not-a-fn" :card 42 :text-run nil}
          c    (fa/compile-assembly corrupted
                                    {:assembly/name "corrupt-face" :assembly/grammar 0
                                     :root {:prim :stack :props {} :children []}})
          tree (fa/apply-assembly c {} view-ctx)]
      ;; :stack IS present in the corrupted registry but is not a fn -> V3 rejects
      (is (fa/error? c))
      (is (valid-error-card? tree "corrupt-face"))
      ;; and a well-formed-but-unknown prim over the corrupted registry, too
      (is (= :error-card (:type (fa/apply-assembly
                                 (fa/compile-assembly corrupted
                                                      {:assembly/name "y" :assembly/grammar 0
                                                       :root {:prim :nope :children []}})
                                 {} view-ctx)))))))

;; ===========================================================================
;; G4 — two-stage purity + Δ1 carry
;; ===========================================================================

(deftest g4-two-stage-and-delta1
  (let [compiled (fa/compile-assembly stub-registry @outline-assembly)]
    (testing "apply is pure — same (compiled, data, ctx) yields EQUAL trees"
      (is (= (fa/apply-assembly compiled @outline-conversation view-ctx)
             (fa/apply-assembly compiled @outline-conversation view-ctx))))
    (testing "ONE compiled builder over TWO data contexts -> two correct differing trees, no recompile"
      (let [data-a {:turns [{:id "a" :speaker "sid" :blocks [{:id "x" :kind "text" :text "one"}]}]}
            data-b {:turns [{:id "a" :speaker "sid" :blocks [{:id "x" :kind "text" :text "one"}]}
                            {:id "b" :speaker "claude" :blocks [{:id "y" :kind "text" :text "two"}]}]}
            ta (fa/apply-assembly compiled data-a view-ctx)
            tb (fa/apply-assembly compiled data-b view-ctx)]
        (is (= 1 (count (:children ta))))
        (is (= 2 (count (:children tb))))
        (is (not= ta tb))))
    (testing "Δ1 carry (trap T10): root :data carries (view-instance, address) + apply-report + content-h"
      (let [tree (fa/apply-assembly compiled @outline-conversation view-ctx)
            d    (:data tree)]
        (is (= :outline-pane (:view-instance d)))
        (is (= "conv:7c80ce2a" (:address d)))
        (is (map? (:assembly/apply-report d)))
        (is (= (get-in tree [:bounds :h]) (:assembly/content-h d))
            "content-h is the measured bottom-up root height (pane scroll contract, §5)")))))

;; ===========================================================================
;; G5 — :each id discipline
;; ===========================================================================

(deftest g5-each-discipline
  (let [compiled (fa/compile-assembly stub-registry @outline-assembly)]
    (testing "nested :each (turns -> blocks) descends the data context"
      (let [tree (fa/apply-assembly compiled @outline-conversation view-ctx)]
        ;; the second turn's speaker binds against the turn context; its block's
        ;; heading prose binds against the descended block context
        (is (some? (find-node tree #(and (= :card-header (:type %))
                                         (= "claude" (:text (first (:text %)))))))
            ":bind [:speaker] resolved against the turn context")
        (is (some? (find-node tree #(and (= :text-run (:type %))
                                         (= "The seam law" (:text (first (:text %)))))))
            ":bind [:text] resolved against the descended block context")))
    (testing "item :id drives node ids — ids TRAVEL with items across a reorder (trap T6)"
      (let [conv     {:turns [{:id "t-sid-1"    :speaker "sid"    :blocks [{:id "b1" :kind "text" :text "first"}]}
                              {:id "t-claude-1" :speaker "claude" :blocks [{:id "b2" :kind "text" :text "second"}]}]}
            reordered {:turns (vec (reverse (:turns conv)))}
            claude?  (fn [n] (and (= :card (:type n))
                                  (= "t-claude-1" (last (:id n)))))
            id-1     (:id (find-node (fa/apply-assembly compiled conv view-ctx) claude?))
            id-2     (:id (find-node (fa/apply-assembly compiled reordered view-ctx) claude?))]
        (is (= [:outline-pane "outline-face" 0 "t-claude-1"] id-1))
        (is (= id-1 id-2) "the claude turn's node id is identical before and after reorder")))
    (testing "missing item :id -> index fallback AND counted in the apply-report"
      (let [conv {:turns [{:speaker "sid"    :blocks [{:kind "text" :text "a"}]}
                          {:speaker "claude" :blocks [{:kind "text" :text "b"}]}]}
            tree (fa/apply-assembly compiled conv view-ctx)]
        ;; 2 turns + 2 blocks = 4 id-less items
        (is (= 4 (get-in tree [:data :assembly/apply-report :items-without-id])))
        ;; index fallback ids are legal and present
        (is (some? (find-node tree #(= [:outline-pane "outline-face" 0 0] (:id %)))))))))

;; ===========================================================================
;; G6 — :bind discipline
;; ===========================================================================

(deftest g6-bind-discipline
  (testing "bind paths resolve against the CURRENT (descended) context"
    (let [asm  {:assembly/name "b" :assembly/grammar 0
                :root {:prim :stack :props {}
                       :children [{:each [:items]
                                   :template {:prim :text-run :props {:value {:bind [:label]}}}}]}}
          tree (fa/apply-assembly (fa/compile-assembly stub-registry asm)
                                  {:items [{:id "1" :label "alpha"} {:id "2" :label "beta"}]} view-ctx)
          texts (map #(:text (first (:text %)))
                     (filter #(= :text-run (:type %)) (walk-nodes tree)))]
      (is (= ["alpha" "beta"] texts) ":bind [:label] resolved per descended item")))
  (testing "a missing bind path -> nil prop + COUNTED (binds-missing), never a crash"
    (let [asm  {:assembly/name "b" :assembly/grammar 0
                :root {:prim :text-run :props {:value {:bind [:nope :missing]}}}}
          tree (fa/apply-assembly (fa/compile-assembly stub-registry asm) {:present 1} view-ctx)]
      (is (= :text-run (:type tree)))
      (is (= 1 (get-in tree [:data :assembly/apply-report :binds-missing])))))
  (testing "a bind ref at NODE position rejects (legal only as a prop value)"
    (let [c (fa/compile-assembly stub-registry
                                 {:assembly/name "b" :assembly/grammar 0
                                  :root {:prim :stack :props {} :children [{:bind [:x]}]}})]
      (is (fa/error? c))
      (is (some #(str/includes? % "bind ref") (error-msgs c)))
      (is (= :error-card (:type (fa/apply-assembly c {} view-ctx)))))))
