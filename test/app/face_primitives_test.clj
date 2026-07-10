(ns app.face-primitives-test
  "Framework Wave 1 — lane W1-B gates (CONTRACT §11 G7-G9) for the primitive
   vocabulary `app.client.workspace.face-primitives`.

   Run:
     clj -M:test -e \"(require 'app.face-primitives-test)
                      (clojure.test/run-tests 'app.face-primitives-test)\"

   G7 mechanical builder fidelity — a SOURCE-FORM DIFF (not call-and-compare;
      the originals are cljs, this suite is JVM). Reads ui_primitives.cljs,
      trail_face/cards.cljc and face_primitives.cljc as DATA and asserts every
      copied builder's defn form is byte-for-form identical to its origin. The
      allowlist of reviewed divergences starts EMPTY; ns forms are exempt
      (carve-out, §11-G7).
   G8 the two new primitives measure — :text-run wraps ONCE via wrap-line, emits
      its own positioned text ops, and its :h = wrapped-line-count * line-height
      (trap T7). Carve-out asserted: :text-layout / resolve-text-layout appear
      NOWHERE in the vocabulary (the dead hook, §6 / PROBE Finding 1). :indent-rail
      golden. Both include a resolve-layout -> tree->rects pass proving non-zero,
      non-overlapping stacked bounds.
   G9 JVM purity — the whole vocabulary + goldens run JVM-side with no browser;
      no `js/` outside reader conditionals. The suite existing and green IS the
      gate."
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [app.client.workspace.rect-tree :as rt]
            [app.client.workspace.face-primitives :as fp]))

;; ===========================================================================
;; G7 — mechanical builder fidelity (source-form diff)
;; ===========================================================================

(defn read-all-forms
  "Read every top-level form of a Clojure/ClojureScript source file as DATA.
   No eval (`*read-eval*` false); reader conditionals tolerated. Classpath
   resource first (src is a classpath root), then a relative-path fallback."
  [resource-path relative-path]
  (let [src (or (io/resource resource-path)
                (let [f (io/file relative-path)] (when (.exists f) f)))]
    (assert src (str "cannot locate source for form-diff: " resource-path))
    (with-open [r (java.io.PushbackReader. (io/reader src))]
      (binding [*read-eval* false]
        (loop [forms []]
          (let [f (read {:read-cond :allow :eof ::eof} r)]
            (if (= f ::eof) forms (recur (conj forms f)))))))))

(defn def-forms-by-sym
  "Index the def/defn/defn- top-level forms of a file by their defined symbol."
  [forms]
  (into {}
        (comp (filter #(and (seq? %)
                            (contains? '#{def defn defn-} (first %))
                            (symbol? (second %))))
              (map (fn [f] [(second f) f])))
        forms))

(def face-forms
  (def-forms-by-sym
    (read-all-forms "app/client/workspace/face_primitives.cljc"
                    "src/app/client/workspace/face_primitives.cljc")))

(def ui-forms
  (def-forms-by-sym
    (read-all-forms "app/client/workspace/ui_primitives.cljs"
                    "src/app/client/workspace/ui_primitives.cljs")))

(def cards-forms
  (def-forms-by-sym
    (read-all-forms "app/client/workspace/trail_face/cards.cljc"
                    "src/app/client/workspace/trail_face/cards.cljc")))

;; ui takes precedence on the (empty) name intersection with cards.
(def origin-forms (merge cards-forms ui-forms))

(def named-ui-builders
  "The 11 ui_primitives.cljs builders §6 names for verbatim extraction."
  '#{ui-panel ui-panel-header ui-panel-content ui-panel-footer ui-panel-group
     ui-list-item ui-card ui-badge ui-divider build-empty-state ui-scrollbar})

(def named-trail-builders
  "The trail_face/cards.cljc proto-primitives harvested (+ omission-line dep)."
  '#{omission-line omissions-block hole-endpoint-card})

(def allowlist
  "Named, reviewed per-builder divergences. STARTS EMPTY (§11-G7)."
  #{})

(deftest g7-named-builders-present
  (testing "every §6-named copied builder is present in face_primitives.cljc"
    (doseq [b named-ui-builders]
      (is (contains? face-forms b)
          (str "missing verbatim ui_primitives copy: " b)))
    (doseq [b named-trail-builders]
      (is (contains? face-forms b)
          (str "missing verbatim trail_face copy: " b)))))

(deftest g7-source-form-fidelity
  (testing "every copied def/defn form is byte-for-form identical to its origin"
    ;; Any symbol defined in BOTH face_primitives and an origin file is a copy
    ;; and must not have drifted. New symbols (wrappers, stack/text-run/
    ;; indent-rail, registry) are unique to face_primitives -> not compared.
    (let [pinned (->> (keys face-forms)
                      (filter #(contains? origin-forms %))
                      (remove allowlist)
                      sort)]
      (is (seq pinned) "the diff pins at least the named builders")
      ;; the pinned set must cover every named builder (nothing silently dropped)
      (doseq [b (concat named-ui-builders named-trail-builders)]
        (is (some #{b} pinned)
            (str b " must be pinned by the source-form diff")))
      (doseq [sym pinned]
        (is (= (get face-forms sym) (get origin-forms sym))
            (str "SOURCE-FORM DRIFT in copied builder " sym
                 " — face_primitives.cljc diverged from its origin"))))))

;; ===========================================================================
;; G8 — the two new primitives measure
;; ===========================================================================

(def geom-760
  "A §5 view-ctx geom: width 760, font 14, advance 14*0.56, line-height 20."
  {:viewport-w 800 :viewport-h 600 :font-size 14 :char-advance (* 14 0.56)
   :line-height 20 :content-w 760 :now-ms 0})

(defn ctx-for [id] {:id id :view-instance :test :address [:test] :geom geom-760})

(def long-prose
  "Prose guaranteed to wrap to many lines at max-chars ~= 760/(14*0.56) ~= 96."
  (str "The map must not lie: known knowns and known unknowns must be visibly "
       "distinct, and the frontier is where trails end and evidence thins. "
       "A face is an assembly — pure EDN describing arrangement, walked by one "
       "interpreter over a registry of primitive builders, so the AI writes "
       "arrangement data through the lawful path and never code into Rama. "
       "Measure in the primitive, arrange in the engine; history is terrain and "
       "the log is primary, every view a projection of it."))

(deftest g8-text-run-wraps-once-and-measures
  (testing ":text-run wraps once, emits positioned ops, sets :h = lines*line-height"
    (let [ca        (:char-advance geom-760)
          max-chars (max 1 (int (/ 760 ca)))
          expected-lines (count (rt/wrap-line long-prose max-chars))
          lh        (:line-height geom-760)
          node      (fp/text-run-prim (ctx-for [:root :tr]) {:value long-prose} [])]
      (is (= :text-run (:type node)))
      (is (> expected-lines 1) "fixture prose actually wraps to >1 line")
      ;; MEASURE RULE: :h = wrapped-line-count * line-height (no padding here)
      (is (= (* expected-lines lh) (get-in node [:bounds :h]))
          ":h equals wrapped-line-count * line-height (trap T7)")
      ;; ONE op per wrapped line, each carrying its OWN :x/:y (positioned)
      (is (= expected-lines (count (:text node)))
          "one positioned text op per wrapped line (emitted, not :text-layout)")
      (is (every? (fn [op] (and (contains? op :x) (contains? op :y))) (:text node))
          "every op is pre-positioned by the primitive")
      ;; ops stack downward by line-height
      (let [ys (map :y (:text node))]
        (is (= ys (sort ys)) "ops stack in increasing y")
        (is (= (* (dec expected-lines) lh) (- (last ys) (first ys)))
            "consecutive ops are line-height apart"))
      ;; the wrapped text reconstructs the source words (wrapped ONCE, no loss)
      (is (= (str/replace long-prose #"\s+" " ")
             (str/replace (str/join " " (map :text (:text node))) #"\s+" " "))
          "one-wrap preserves the prose"))))

(defn data-atoms
  "Every leaf datum of a read form (descends lists/vectors/maps/sets). String
   literals (docstrings) stay whole atoms — so a keyword/symbol NAMED inside a
   docstring is never mistaken for one USED in code."
  [form]
  (tree-seq coll?
            (fn [x] (if (map? x) (mapcat identity x) (seq x)))
            form))

(deftest g8-text-run-does-not-ride-text-layout
  (testing "the dead :text-layout hook appears NOWHERE in CODE (§6 / PROBE Finding 1)"
    (let [node (fp/text-run-prim (ctx-for [:root :tr]) {:value long-prose} [])]
      (is (nil? (:text-layout node))
          ":text-run must not attach :text-layout (dead hook, doubles wrap cost)"))
    ;; carve-out at source grain, on PARSED forms (docstrings are string atoms,
    ;; not code): the :text-layout KEYWORD and any resolve-text-layout SYMBOL
    ;; are used nowhere in the vocabulary's code.
    (let [atoms (mapcat data-atoms (vals face-forms))]
      (is (not (some #{:text-layout} (filter keyword? atoms)))
          ":text-layout keyword must not be USED anywhere in the vocabulary")
      (is (not (some #(str/includes? (name %) "resolve-text-layout")
                     (filter symbol? atoms)))
          "resolve-text-layout must not be CALLED anywhere in the vocabulary"))))

(deftest g8-text-run-renders-through-real-primitives
  (testing ":text-run flattens through the real render fns (non-zero bounds)"
    (let [node   (fp/text-run-prim (ctx-for [:root :tr]) {:value long-prose} [])
          laid   (rt/resolve-layout node)
          rects  (rt/tree->rects laid)
          ops    (rt/tree->text-ops laid)]
      ;; no bg style -> no rect, but the walk must not throw and text survives
      (is (pos? (get-in laid [:bounds :h])) "non-zero height after resolve-layout")
      (is (seq ops) "text ops flatten through tree->text-ops")
      (is (vector? rects) "tree->rects returns a vector (render-compatible)"))))

(deftest g8-indent-rail-golden
  (testing ":indent-rail stacks children indented, paints a gutter rail, measures h"
    (let [child (fn [seg h]
                  (rt/rt-node [:root :ir seg] :text-run
                              {:x 0 :y 0 :w 700 :h h} :text []))
          kids  [(child :a 40) (child :b 60) (child :c 20)]
          node  (fp/indent-rail-prim (ctx-for [:root :ir])
                                     {:indent 16 :gap 8} kids)]
      (is (= :indent-rail (:type node)))
      ;; MEASURE RULE: h = sum children heights + inter-child gaps
      (is (= (+ 40 60 20 (* 8 2)) (get-in node [:bounds :h]))
          ":indent-rail :h = sum child heights + gaps (measure rule)")
      ;; the rail is child 0, layout-skipped, spanning full content height
      (let [rail (first (:children node))]
        (is (= :indent-rail-guide (:type rail)))
        (is (true? (get-in rail [:data :layout-skip?])) "rail is layout-skipped")
        (is (= (get-in node [:bounds :h]) (get-in rail [:bounds :h]))
            "rail spans the full content height"))
      ;; resolve-layout -> tree->rects: non-zero, non-overlapping stacked bounds
      (let [laid  (rt/resolve-layout node)
            real  (rest (:children laid))          ;; drop the rail
            ys    (map #(get-in % [:bounds :y]) real)
            bots  (map #(+ (get-in % [:bounds :y]) (get-in % [:bounds :h])) real)]
        (is (every? #(pos? (get-in % [:bounds :h])) real)
            "every stacked child has non-zero height")
        (is (= ys (sort ys)) "children stack in increasing y")
        ;; non-overlap: each child starts at or after the previous child's bottom
        (is (every? true? (map (fn [y prev-bot] (>= y prev-bot))
                               (rest ys) (butlast bots)))
            "stacked children do not overlap")
        ;; the real children are indented right of the gutter rail
        (is (every? #(= 16 (get-in % [:bounds :x])) real)
            "children indented by :indent, rail stays in the gutter")
        (let [rail (first (:children laid))]
          (is (< (get-in rail [:bounds :x]) 16)
              "gutter rail sits left of the indented children"))))))

;; ===========================================================================
;; G9 — JVM purity + registry is a plain value
;; ===========================================================================

(deftest g9-jvm-purity-and-registry
  (testing "the whole vocabulary loaded + ran JVM-side; registry is a plain map"
    (is (map? fp/registry) "registry is a plain map value (§6)")
    (is (every? keyword? (keys fp/registry)) "keys are primitive keywords")
    (is (every? fn? (vals fp/registry)) "values are builder fns")
    ;; §6 vocabulary coverage: the 11 ui copies + 2 new + :stack + 2 trail
    (is (every? (set (keys fp/registry))
                [:panel :panel-header :panel-content :panel-footer :panel-group
                 :list-item :card :badge :divider :empty-state :scrollbar
                 :stack :text-run :indent-rail :omissions :hole-card])
        "every §6-named primitive is registered")
    ;; every builder returns a valid rt-node (has :type/:bounds) under a call
    (let [ctx (ctx-for [:root :x])]
      (doseq [[k builder] fp/registry]
        (let [node (builder ctx
                            (case k
                              :text-run    {:value "hello world"}
                              :badge       {:label "tag"}
                              :panel-group {:label "grp"}
                              :list-item   {:title "row"}
                              :omissions   {:omissions [{:layer :bundle :dropped 3}]}
                              :hole-card   {:row {:kind :hole}}
                              {})
                            [])]
          (is (map? node) (str k " returns a map rt-node"))
          (is (keyword? (:type node)) (str k " rt-node carries a :type"))
          (is (map? (:bounds node)) (str k " rt-node carries :bounds")))))
    ;; no js/ interop in CODE (parsed forms; a "js/" mention in a docstring is a
    ;; string atom, never a js-namespaced symbol). No reader conditionals either.
    (let [atoms (mapcat data-atoms (vals face-forms))]
      (is (not (some #(= "js" (namespace %)) (filter symbol? atoms)))
          "no js/-namespaced symbol (interop) anywhere in the vocabulary"))))
