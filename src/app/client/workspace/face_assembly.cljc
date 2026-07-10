(ns app.client.workspace.face-assembly
  "Framework Wave 1, lane A — the two-stage assembly interpreter.

   A *face* is an arrangement of the land's UI vocabulary described as pure EDN
   data (an *assembly*). This namespace is the pure interpreter that turns an
   assembly + a data context into an rt-node tree, through a *registry* of
   primitive builders. Faces are DATA, so what renders them is an INTERPRETER —
   never code generation into the compiled substrate (CONTRACT §3 seam law;
   D-011 middle regime).

   Two-stage law (CONTRACT §5):
     (compile-assembly registry assembly-edn) -> compiled   ; WEAR-TIME, once
     (apply-assembly   compiled data view-ctx) -> rt-tree    ; DATA-CHANGE-TIME
   Frame-time does nothing new: the existing RAF sample + `identical?` skip +
   keyed pool diff consume the produced scene like any pane. Interpretation cost
   lives on the data-change path, never the frame path (trap T3).

   Both functions are TOTAL — they NEVER throw. A malformed assembly, an unknown
   primitive, a corrupted registry, or a primitive that throws all resolve to a
   renderable ERROR-CARD rt-node carrying the assembly name, grammar version,
   address, and the first <=5 validation errors. The render path has no `try`
   upstream (L13: `try` is TODO inside e/defn), so one bad AI-authored assembly
   must land as a visible card beside the chat, never as a black screen (trap
   T4). The error-card constructor is built into THIS namespace and is
   registry-independent — a corrupted registry cannot take the error path down
   with it.

   Grammar v0 — five keys, one guard (CONTRACT §4):
     prim node  {:prim <kw> :props <map>? :children [<node>...]?}
     each node  {:each [<kw>...] :template <node>}   ; iterate seq at path,
                                                       descend context per item
     bind ref   {:bind [<kw>...]}                     ; legal ONLY as a prop
                                                       value (not nested)
   THE GUARD (V4, trap T2): the assembly form contains no list and no symbol
   anywhere — keywords, strings, numbers, booleans, nil, vectors, maps only.
   Arrangement only, no logic ever. Anything that wants to be a program gets to
   be a real one, in the code lane (primitives §6, projections §7).

   `.cljc` so the golden harness runs JVM-side with no browser — the block-kernel
   goldens discipline applied to UI. Depends only on `rect_tree` (also cljc)."
  (:require [clojure.set :as set]
            [app.client.workspace.rect-tree :as rt]))

;; ===========================================================================
;; §4 · Grammar v0 — validation (all at compile time)
;; ===========================================================================

(def ^:private known-envelope-keys
  #{:assembly/name :assembly/grammar :assembly/belief :root})

(defn- guard-reason
  "Name the offending value class for a guard-scan violation (V4)."
  [x]
  (cond
    (symbol? x) "a symbol"
    (seq? x)    "a list"
    (set? x)    "a set"
    (char? x)   "a char"
    ;; INT fix: `class`/.getSimpleName is JVM-only — on cljs it threw inside
    ;; the guard, breaking error-card totality in the browser (trap T4, the
    ;; exact layer that must never throw). Platform-split the type name.
    :else       (str "a disallowed value ("
                     #?(:clj  (some-> x class .getSimpleName)
                        :cljs (some-> x type pr-str))
                     ")")))

(defn- scan-guard
  "V4 THE GUARD, mechanical form (trap T2). Depth-first walk of the assembly
   form; returns {:path <path> :reason <str>} for the FIRST value that is not
   arrangement data (keyword / string / number / boolean / nil / vector / map),
   or nil when the whole form is clean. Lists and symbols are the headline
   rejects — a list is an s-expression waiting to happen, a symbol is a
   reference to code; neither may appear in data-only arrangement."
  [form path]
  (cond
    (or (keyword? form) (string? form) (number? form) (boolean? form) (nil? form))
    nil
    (map? form)
    (some (fn [[k v]]
            (or (scan-guard k (conj path k))
                (scan-guard v (conj path k))))
          form)
    (vector? form)
    (first (keep-indexed (fn [i v] (scan-guard v (conj path i))) form))
    :else
    {:path path :reason (guard-reason form)}))

(defn- contains-bind?
  "True if a bind ref {:bind ..} appears anywhere inside `form` — used to reject
   binds nested in literal collections (V5; only a top-position prop value may be
   a bind ref)."
  [form]
  (cond
    (and (map? form) (contains? form :bind)) true
    (map? form)    (boolean (some (fn [[_ v]] (contains-bind? v)) form))
    (vector? form) (boolean (some contains-bind? form))
    :else          false))

(defn- validate-prop-value
  "V5: a resolved prop value is either an EDN literal or a bind ref
   {:bind [<kw>...]} in TOP position. A malformed bind ref, or a bind ref nested
   inside a literal collection, is an error."
  [v path]
  (cond
    (and (map? v) (contains? v :bind))
    (if (and (= #{:bind} (set (keys v)))
             (vector? (:bind v))
             (seq (:bind v))
             (every? keyword? (:bind v)))
      []
      [{:path path :msg "malformed :bind ref — must be {:bind [<kw> ...]} with a non-empty keyword path"}])
    (contains-bind? v)
    [{:path path :msg "nested :bind not allowed — a bind ref is legal only as a top-level prop value (V5)"}]
    :else []))

(declare validate-node)

(defn- validate-children [children registry path]
  (if (vector? children)
    (into [] (comp (map-indexed vector)
                   (mapcat (fn [[i c]] (validate-node c registry (conj path i)))))
          children)
    [{:path path :msg ":children must be a vector of nodes (V6)"}]))

(defn- validate-props [props registry path]
  (cond
    (nil? props) []
    (not (map? props)) [{:path path :msg ":props must be a map (V5)"}]
    :else
    (let [key-errs (if (every? keyword? (keys props))
                     []
                     [{:path path :msg ":props keys must be keywords (V5)"}])]
      (into key-errs
            (mapcat (fn [[k v]] (validate-prop-value v (conj path k))))
            props))))

(defn- validate-node
  "V2/V3/V5/V6: a node is a prim node XOR an each node. Collects all errors
   (never throws)."
  [node registry path]
  (cond
    (not (map? node))
    [{:path path :msg (str "node must be a map, got " (pr-str node))}]

    :else
    (let [ks       (set (keys node))
          has-prim (contains? node :prim)
          has-each (contains? node :each)]
      (cond
        (and has-prim has-each)
        [{:path path :msg "node has both :prim and :each — exactly one is required (V2)"}]

        has-prim
        (let [extra   (set/difference ks #{:prim :props :children})
              errs    (if (seq extra)
                        [{:path path :msg (str "unknown node keys " extra " — a prim node's closed key set is #{:prim :props :children} (V2)")}]
                        [])
              ;; V3 — :prim must resolve to a FUNCTION in the registry. Checking
              ;; fn? (not just presence) keeps the error path registry-independent:
              ;; a corrupted registry entry error-cards, never throws (trap T4).
              errs    (into errs
                            (cond
                              (not (keyword? (:prim node)))
                              [{:path (conj path :prim) :msg (str ":prim must be a keyword, got " (pr-str (:prim node)))}]
                              (not (fn? (get registry (:prim node))))
                              [{:path (conj path :prim) :msg (str "unknown :prim " (:prim node) " — not a builder in the registry (V3)")}]
                              :else []))
              errs    (into errs (validate-props (:props node) registry (conj path :props)))
              errs    (into errs (if (contains? node :children)
                                   (validate-children (:children node) registry (conj path :children))
                                   []))]
          errs)

        has-each
        (let [extra (set/difference ks #{:each :template})
              errs  (if (seq extra)
                      [{:path path :msg (str "unknown keys " extra " on an :each node — closed key set is #{:each :template} (V2)")}]
                      [])
              errs  (into errs
                          (if (and (vector? (:each node)) (seq (:each node)) (every? keyword? (:each node)))
                            []
                            [{:path (conj path :each) :msg ":each must be a non-empty vector of keywords (V6)"}]))
              errs  (into errs
                          (if (contains? node :template)
                            (validate-node (:template node) registry (conj path :template))
                            [{:path path :msg ":each node is missing its required :template (V6)"}]))]
          errs)

        ;; §4: a bind ref is legal ONLY as a prop value — never at node position.
        (contains? node :bind)
        [{:path path :msg "a bind ref {:bind ..} is legal only as a prop value, never at node position (§4)"}]

        :else
        [{:path path :msg (str "node has neither :prim nor :each (keys: " ks ") (V2)")}]))))

(defn- count-nodes
  "V7: total template-node count in the assembly (prim + each + template)."
  [node]
  (cond
    (not (map? node))       0
    (contains? node :each)  (+ 1 (count-nodes (:template node)))
    (contains? node :prim)  (+ 1 (reduce + 0 (map count-nodes (:children node))))
    :else                   1))

(defn- validate-envelope
  "V1: envelope shape. Unknown NAMESPACED keys are tolerated and preserved (the
   §8 schema adds namespaced fields in Wave 2; old interpreters must not
   error-card new files). Unknown non-namespaced keys reject."
  [assembly]
  (let [errs (cond-> []
               (not (string? (:assembly/name assembly)))
               (conj {:path [:assembly/name] :msg ":assembly/name is required and must be a string (V1)"})
               (not (contains? assembly :assembly/grammar))
               (conj {:path [:assembly/grammar] :msg ":assembly/grammar is required (V1)"})
               (and (contains? assembly :assembly/belief)
                    (not (string? (:assembly/belief assembly))))
               (conj {:path [:assembly/belief] :msg ":assembly/belief must be a string (V1)"})
               (not (contains? assembly :root))
               (conj {:path [] :msg "envelope is missing :root (V1)"}))
        unknown (remove (fn [k]
                          (or (contains? known-envelope-keys k)
                              (and (keyword? k) (namespace k))))
                        (keys assembly))]
    (into errs
          (map (fn [k] {:path [k] :msg (str "unknown envelope key " k " — only namespaced keys are tolerated for forward-compat (V1)")}))
          unknown)))

(defn- validate-assembly
  "Collect ALL validation errors (V1-V7) for an assembly. Empty vector = valid."
  [registry assembly]
  (if-not (map? assembly)
    [{:path [] :msg (str "an assembly must be a map, got " (pr-str assembly))}]
    (let [errs (cond-> []
                 ;; V4 guard first — the most fundamental (no logic in data, T2)
                 :always (into (if-let [g (scan-guard assembly [])]
                                 [{:path (:path g) :msg (str "V4 guard: " (:reason g)
                                                             " is not allowed anywhere in an assembly — arrangement only, no logic (trap T2)")}]
                                 []))
                 :always (into (validate-envelope assembly))
                 ;; grammar version
                 (not= 0 (:assembly/grammar assembly))
                 (conj {:path [:assembly/grammar]
                        :msg (str "unsupported :assembly/grammar " (pr-str (:assembly/grammar assembly))
                                  " — this interpreter speaks grammar 0 (V1)")}))
          errs (into errs (if (contains? assembly :root)
                            (validate-node (:root assembly) registry [:root])
                            []))
          ;; the root must be a BUILDABLE node — an :each cannot stand alone
          ;; at the root (it expands into siblings, and a root has no parent
          ;; to receive them). Rejecting HERE keeps ALL structural rejection
          ;; at wear-time compile (§5 two-stage law; trap T3 — G16
          ;; falsification fix: this previously error-carded only at apply)
          errs (cond-> errs
                 (and (map? (:root assembly)) (contains? (:root assembly) :each))
                 (conj {:path [:root]
                        :msg "the root node must be a :prim node — an :each cannot stand alone at the root (V2)"}))
          errs (let [n (count-nodes (:root assembly))]
                 (cond-> errs
                   (> n 1000) (conj {:path [:root] :msg (str "template-node count " n " exceeds the 1000 cap (V7)")})))]
      errs)))

;; ===========================================================================
;; §4 · Error card — the total-function landing pad (registry-independent)
;; ===========================================================================

(defn error-card-node
  "Build a VALID, renderable error-card rt-node (trap T4). Registry-independent:
   constructed entirely here, so a corrupted registry cannot corrupt the error
   path. Carries the assembly name + grammar + address and the first <=5 errors,
   each with its path into the form. `tree->rects` / `tree->text-ops` both
   succeed on it (G3)."
  [id name grammar address errors geom]
  (let [{:keys [content-w font-size line-height]
         :or   {content-w 600 font-size 14 line-height 20}} (or geom {})
        title (str "⚠ assembly error"
                   (when name    (str " · " name))
                   (when grammar (str " · grammar " grammar))
                   (when address (str " · " (pr-str address))))
        shown (take 5 errors)
        lines (into [title]
                    (map (fn [e] (str "  • " (pr-str (:path e)) " — " (:msg e))))
                    shown)
        pad   10
        n     (count lines)
        h     (+ (* 2 pad) (* n line-height))
        ops   (into []
                    (map-indexed
                     (fn [i ln]
                       {:text ln :type :text
                        :x    pad
                        :y    (+ pad font-size (* i line-height))
                        :size font-size :r 0.95 :g 0.6 :b 0.6 :a 1.0}))
                    lines)]
    (rt/rt-node id :error-card
                {:x 0 :y 0 :w content-w :h h}
                :style {:bg [0.35 0.10 0.10 1.0] :radius 6
                        :border-width 1 :border-color [0.90 0.30 0.30 1.0]}
                :text ops
                :data {:assembly/error true
                       :assembly/errors (vec (take 5 errors))})))

;; ===========================================================================
;; §5 · compile-assembly — wear-time (compile the builder graph ONCE)
;; ===========================================================================

(defn- compile-node
  "Compile a validated node into a PLAN — a data-only intermediate with the
   builder fn already resolved from the registry (the graph is closed over
   once, trap T3). apply-* interprets the plan against a data context."
  [node registry]
  (if (contains? node :each)
    {:kind :each :path (:each node) :template (compile-node (:template node) registry)}
    {:kind     :prim
     :prim     (:prim node)
     :builder  (get registry (:prim node))
     :props    (:props node)
     :children (into [] (map-indexed (fn [i c] (assoc (compile-node c registry) :seg i)))
                     (or (:children node) []))}))

(defn compile-assembly
  "WEAR-TIME. Pure, TOTAL (never throws). Parses, validates (V1-V7), resolves
   prims against the registry, and closes over the builder graph ONCE. On any
   validation failure returns a compiled form whose `apply-assembly` renders the
   §4 error card (trap T4) — a malformed assembly compiles, it does not crash.

   `registry` is a plain map {<prim-kw> -> (fn [ctx props children] -> rt-node)}
   (CONTRACT §6). No global mutable registry; the map is a value."
  [registry assembly]
  (let [errors (validate-assembly registry assembly)]
    (if (seq errors)
      {::status  :error
       ::name    (when (map? assembly) (:assembly/name assembly))
       ::grammar (when (map? assembly) (:assembly/grammar assembly))
       ::errors  (vec errors)}
      {::status :ok
       ::name   (:assembly/name assembly)
       ::plan   (compile-node (:root assembly) registry)})))

;; ===========================================================================
;; §5 · apply-assembly — data-change-time (pure; measure then arrange)
;; ===========================================================================

(def ^:private zero-report {:items-without-id 0 :binds-missing 0})

(defn- merge-reports [a b]
  {:items-without-id (+ (:items-without-id a 0) (:items-without-id b 0))
   :binds-missing    (+ (:binds-missing a 0)    (:binds-missing b 0))})

(defn- resolve-props
  "Substitute {:bind [path]} prop values via get-in against the CURRENT data
   context. Missing paths resolve to nil (honest data absence renders as a gap,
   never a crash) and are COUNTED — the apply-report must not lie about
   degradation (§5). Returns [resolved-props binds-missing-count]."
  [props data]
  (reduce-kv
   (fn [[m cnt] k v]
     (if (and (map? v) (contains? v :bind))
       (let [rv (get-in data (:bind v))]
         [(assoc m k rv) (if (nil? rv) (inc cnt) cnt)])
       [(assoc m k v) cnt]))
   [{} 0]
   (or props {})))

(defn- invoke-builder
  "Call a builder over the §6 interface (fn [ctx props children] -> rt-node).
   Defensive try/catch keeps apply TOTAL even if a builder throws on odd props
   (reinforces T4/L13); V3 already guarantees `builder` is a fn."
  [builder ctx props children]
  (if (fn? builder)
    #?(:clj  (try (builder ctx props children)
                  (catch Throwable e
                    (error-card-node (:id ctx) "primitive" nil (:address ctx)
                                     [{:path [] :msg (str "primitive " (:prim ctx) " threw: " (ex-message e))}]
                                     (:geom ctx))))
       :cljs (try (builder ctx props children)
                  (catch :default e
                    (error-card-node (:id ctx) "primitive" nil (:address ctx)
                                     [{:path [] :msg (str "primitive " (:prim ctx) " threw: " (str e))}]
                                     (:geom ctx)))))
    (error-card-node (:id ctx) "unknown-prim" nil (:address ctx)
                     [{:path [] :msg "registry entry is not a function"}] (:geom ctx))))

(declare expand-slot)

(defn- build-node
  "Build ONE rt-node from a :prim plan at exactly `id`. Children are built
   post-order (already-built child rt-nodes are passed to the builder, §6), each
   occupying a slot whose base id extends this node's id by the child's
   structural segment. Returns {:node <rt-node> :report <report>}.

   A non-:prim plan reaching here (e.g. an :each as the root) cannot stand alone —
   it renders an error card rather than crashing (trap T4)."
  [plan data ctx-base id]
  (if (not= :prim (:kind plan))
    {:node (error-card-node id "each-standalone" nil (:address ctx-base)
                            [{:path [] :msg "an :each node cannot stand alone here — wrap it in a :prim container"}]
                            (:geom ctx-base))
     :report zero-report}
    (let [[props binds-missing] (resolve-props (:props plan) data)
          ;; §5: a bound :props {:id ..} OVERRIDES the derived id segment.
          id* (if (contains? props :id)
                (conj (if (seq id) (pop id) id) (:id props))
                id)
          {:keys [nodes report]}
          (reduce
           (fn [acc cplan]
             (let [child-base (conj id* (:seg cplan))
                   r          (expand-slot cplan data ctx-base child-base)]
               {:nodes  (into (:nodes acc) (:nodes r))
                :report (merge-reports (:report acc) (:report r))}))
           {:nodes [] :report zero-report}
           (:children plan))
          ctx   {:id id* :prim (:prim plan)
                 :view-instance (:view-instance ctx-base)
                 :address (:address ctx-base) :geom (:geom ctx-base)}
          built (invoke-builder (:builder plan) ctx props nodes)]
      {:node   built
       :report (merge-reports report {:binds-missing binds-missing})})))

(defn- expand-slot
  "Produce a FLAT vector of rt-nodes for one child slot with base id `base-id`.
     :prim -> [one node with id = base-id]
     :each -> iterate the seq at path in the current data context; per item the
              template is expanded with the context DESCENDED to the item and an
              id extended by the item's segment. An item's segment is its :id
              value when present, else its index — index fallback is legal but
              COUNTED (trap T6: item :id drives node ids so identity travels with
              items across reorders). Returns {:nodes [..] :report <report>}."
  [plan data ctx-base base-id]
  (case (:kind plan)
    :prim
    (let [{:keys [node report]} (build-node plan data ctx-base base-id)]
      {:nodes [node] :report report})

    :each
    (let [items (get-in data (:path plan))]
      (if (sequential? items)
        (reduce
         (fn [acc [i item]]
           (let [has-id? (and (map? item) (contains? item :id))
                 seg     (if has-id? (:id item) i)
                 r       (expand-slot (:template plan) item ctx-base (conj base-id seg))]
             {:nodes  (into (:nodes acc) (:nodes r))
              :report (merge-reports (:report acc)
                                     (cond-> (:report r)
                                       (not has-id?) (update :items-without-id inc)))}))
         {:nodes [] :report zero-report}
         (map-indexed vector items))
        ;; §5: honest data absence — an :each over a non-sequential value renders
        ;; a visible error card, never a crash (trap T4).
        {:nodes  [(error-card-node (conj base-id 0) "each-not-seq" nil (:address ctx-base)
                                   [{:path (:path plan) :msg (str ":each path resolved to a non-sequence: " (pr-str items))}]
                                   (:geom ctx-base))]
         :report zero-report}))))

(defn- stamp-root
  "§5 Δ1 carry (D-009, trap T10) + apply-report + pane scroll contract. The
   D-009 H6-keyed scene store does not exist yet; carrying (view-instance,
   address) from birth makes Δ1 compliance a RENAME when the Δ3 store lands, not
   a rework. `:assembly/content-h` is the measured bottom-up height so the host
   wheel handler can clamp (§5 pane scroll contract; scroll rides the camera,
   the tree stays scroll-independent)."
  [tree view-instance address report]
  (update tree :data merge
          {:view-instance          view-instance
           :address                address
           :assembly/content-h     (get-in tree [:bounds :h] 0)
           :assembly/apply-report  {:items-without-id (:items-without-id report 0)
                                    :binds-missing    (:binds-missing report 0)}}))

(defn apply-assembly
  "DATA-CHANGE-TIME. Pure. No side effects, no atoms — the caller owns all
   caching (the scene lands in a scene atom watched by nothing, trap T9). Runs
   the compiled builder graph over `data-context`, then ONE `resolve-layout`
   arrange pass (measure in the primitive, arrange in the engine — trap T7), and
   stamps the root's :data with Δ1 identity + the apply-report + content-h.

   view-ctx = {:view-instance <kw> :address <addr, §7>
               :geom {:viewport-w :viewport-h :font-size :char-advance
                      :line-height :content-w :now-ms}}
   (the trail-face geom precedent; :now-ms is the honest server stamp).

   Same (compiled, data, view-ctx) always yields an EQUAL tree; one compiled
   builder over two data contexts yields two correct trees with no recompile
   (G4)."
  [compiled data-context view-ctx]
  (let [vi       (:view-instance view-ctx)
        addr     (:address view-ctx)
        geom     (:geom view-ctx)
        ctx-base {:view-instance vi :address addr :geom geom}
        root-id  [vi (::name compiled)]]
    (if (= :error (::status compiled))
      (-> (error-card-node root-id (::name compiled) (::grammar compiled) addr (::errors compiled) geom)
          (rt/resolve-layout)
          (stamp-root vi addr zero-report))
      (let [{:keys [node report]} (build-node (::plan compiled) data-context ctx-base root-id)]
        (-> node
            (rt/resolve-layout)
            (stamp-root vi addr report))))))

;; ===========================================================================
;; Introspection helpers (goldens / tests / the W1-INT mount)
;; ===========================================================================

(defn error?
  "True if a compiled assembly is an error form (validation failed)."
  [compiled]
  (= :error (::status compiled)))

(defn compile-errors
  "The validation errors of a compiled error form (nil for a valid one)."
  [compiled]
  (::errors compiled))
