(ns app.server.worn.invocation-material
  "The block invocation facet specification.
   Takes: served model, effort, and precontext values.
   Gives: compiled invocation settings and contribution rows.
   Holds: spec."
  (:require #?(:clj [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [app.server.worn.facet-material :as facet-material]))

(def master-id "fm:invocation")
(def grammar-version 0)
(def code-floor-revision-id "code-floor:fm:invocation:v0")

(def effort-vocabulary
  "The exact subset re-verified against Claude Code 2.1.220 at P2 wiring time."
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
  [x]
  (and (string? x) (not-empty x)))

(defn- legal-effort?
  [x]
  (contains? effort-vocabulary x))

(defn- legal-precontext?
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
  [form]
  (let [compiled (facet-material/compile-form spec form)]
    (if (:valid? compiled)
      compiled
      (update compiled :errors
              #(mapv (partial teach form) %)))))

(defn compile-source
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
  (facet-material/code-floor spec))

(defn resolved-wear
  [served]
  (facet-material/resolved-wear spec served))

(defn precontext-depth
  "Decode one already-validated `thread+N` setting. Invalid values narrow to
   zero; callers never gain context from malformed material."
  [value]
  (if (contains? precontext-vocabulary value)
    #?(:clj (Long/parseLong (subs value (count "thread+")))
       :cljs (js/parseInt (subs value (count "thread+")) 10))
    0))

(defn contribution-stamp
  [wear subject site role slot]
  (facet-material/contribution-stamp wear subject site role slot))
