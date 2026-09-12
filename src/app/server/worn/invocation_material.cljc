(ns app.server.worn.invocation-material
  "Pure specification for block invocation model, effort and precontext.
   The v0 default/floor selects model, effort and a thread+N context setting.
   Compilation through this namespace adds field paths/legal-value hints to
   validation errors; callers of facet-engine directly receive generic errors.
   Served active maps resolve through facet-engine and precontext-depth decodes N.

   Owns immutable specification and vocabulary values only. No provider is
   called and no installation is checked: model validation accepts any nonempty
   string, including whitespace. Effort is a local allowlist, not a capability
   query. Context selection and invocation execution belong to callers."
  (:require #?(:clj [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [app.server.worn.facet-engine :as facet-engine]))

(def master-id "fm:invocation")
(def grammar-version 0)
(def code-floor-revision-id "code-floor:fm:invocation:v0")

(def effort-vocabulary
  "The effort strings accepted by grammar v0; runtime provider support is separate."
  #{"low" "medium" "high" "xhigh" "max"})

(def precontext-vocabulary
  "A deliberately small, teaching vocabulary. `thread+N` admits the addressed
   block plus at most N immediately preceding wearer rows into the portal open."
  (into #{} (map #(str "thread+" %)) (range 0 9)))

(def default-form
  {:facet-master/id master-id
   :facet-master/grammar grammar-version
   :facet-master/facet :invocation
   :invocation/model "sonnet"
   :invocation/effort "low"
   :invocation/precontext "thread+2"})

(def default-source (pr-str default-form))

(defn- non-blank-string?
  "Accept a nonempty string; despite the name, whitespace-only values also pass."
  [x]
  (and (string? x) (not-empty x)))

(defn- legal-effort?
  "Check membership in this spec's local effort vocabulary; no provider query."
  [x]
  (contains? effort-vocabulary x))

(defn- legal-precontext?
  "Check membership in the declared thread+0 through thread+8 vocabulary."
  [x]
  (contains? precontext-vocabulary x))

(def spec
  {:facet-master/id master-id
   :facet-master/facet :invocation
   :facet-master/source-ref "softland://facet-master/invocation"
   :facet-master/default-form default-form
   :facet-master/floor-form default-form
   :facet-master/code-floor-revision-id code-floor-revision-id
   :facet-master/grammars
   {grammar-version
    {:material-keys
     #{:invocation/model
       :invocation/effort
       :invocation/precontext}
     :validators
     {:invocation/model
      {:valid? non-blank-string?
       :error-type :invocation/model-invalid
       :legal "a nonblank installed Claude model alias or full model id"}
      :invocation/effort
      {:valid? legal-effort?
       :error-type :invocation/effort-invalid
       :legal effort-vocabulary}
      :invocation/precontext
      {:valid? legal-precontext?
       :error-type :invocation/precontext-invalid
       :legal precontext-vocabulary}}}}})

(defn- teach
  "The generic facet compiler intentionally emits compact errors. Invocation is
   agent-edited, so attach the offending key and legal vocabulary at this
   boundary without changing the shared compiler's historical error shape."
  [form error]
  (let [k (case (:type error)
            :invocation/model-invalid :invocation/model
            :invocation/effort-invalid :invocation/effort
            :invocation/precontext-invalid :invocation/precontext
            nil)
        declaration (get-in spec [:facet-master/grammars grammar-version
                                  :validators k])]
    (cond-> error
      k (assoc :path [k]
               :actual (get form k)
               :legal (:legal declaration)))))

(defn compile-form
  "Validate an invocation form and add field paths/legal-value hints to errors."
  [form]
  (let [compiled (facet-engine/compile-form spec form)]
    (if (:valid? compiled)
      compiled
      (update compiled :errors
              #(mapv (partial teach form) %)))))

(defn compile-source
  "Read invocation EDN; return taught validation errors or a parse-error result."
  [source]
  (try
    (compile-form (edn/read-string (str source)))
    (catch #?(:clj Throwable :cljs :default) t
      {:valid? false
       :errors [{:type :facet-master/parse-error
                 :actual (str source)
                 :legal "EDN invocation form"
                 :message #?(:clj (.getMessage t)
                             :cljs (.-message t))}]
       :grammar nil
       :material nil})))

(def code-floor
  (facet-engine/code-floor spec))

(defn resolved-wear
  "Resolve a complete served active map, falling back to this spec's code floor."
  [served]
  (facet-engine/resolved-wear spec served))

(defn precontext-depth
  "Decode one already-validated `thread+N` setting. Invalid values narrow to
   zero; callers never gain context from malformed material."
  [value]
  (if (contains? precontext-vocabulary value)
    #?(:clj (Long/parseLong (subs value (count "thread+")))
       :cljs (js/parseInt (subs value (count "thread+")) 10))
    0))

(defn contribution-stamp
  "Return subject/facet/revision and site/role/slot provenance for supplied wear."
  [wear subject site role slot]
  (facet-engine/contribution-stamp wear subject site role slot))
