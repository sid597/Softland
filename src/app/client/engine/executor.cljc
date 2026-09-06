(ns app.client.engine.executor
  "One expression language for recipe arguments and width rules.

   Takes EDN expressions, explicit bindings and a capability table. Gives
   values; execute also returns final bindings. Holds no state or clock.
   Capabilities own their effects. No read report is a dependency key.
   Evidence: test/app/client/engine/executor_test.clj."
  (:require [app.client.engine.schema :as schema]))

(def operations
  "Operator → [minimum arity, maximum arity or nil, implementation]."
  {:+ [0 nil +] :- [1 nil -] :* [0 nil *] :/ [1 nil (fn [& xs] (apply / (map double xs)))]
   :min [1 nil min] :max [1 nil max] :abs [1 1 #(Math/abs (double %))]
   :sqrt [1 1 #(Math/sqrt (double %))] :pow [2 2 #(Math/pow (double %1) (double %2))]
   :sin [1 1 #(Math/sin (double %))] :cos [1 1 #(Math/cos (double %))]
   :exp [1 1 #(Math/exp (double %))] :floor [1 1 #(Math/floor (double %))]
   :clamp [3 3 (fn [x lo hi] (max lo (min hi x)))]
   :step [2 2 (fn [edge x] (if (< x edge) 0.0 1.0))]
   :smoothstep [3 3 (fn [lo hi x] (let [t (max 0.0 (min 1.0 (/ (- x lo) (- hi lo))))] (* t t (- 3.0 (* 2.0 t)))))]
   :mix [3 3 (fn [a b t] (+ a (* (- b a) t)))]
   :< [2 2 <] :<= [2 2 <=] := [2 2 =] :not [1 1 not]})

(defn- arity! [op args lo hi]
  (when-not (and (<= lo (count args)) (or (nil? hi) (<= (count args) hi)))
    (throw (ex-info "Wrong expression arity" {:error-type :executor/arity :operator op :arguments args}))))

(defn compile-expression
  "Expression → a pure function of bindings. :get is required, :literal
   quotes data, :if is lazy; maps and ordinary vectors resolve children.
   Compilation happens once for a width rule, before sampling its points."
  [expression]
  (cond
    (map? expression)
    (let [entries (mapv (fn [[k v]] [k (compile-expression v)]) expression)]
      (fn [bindings] (into {} (map (fn [[k f]] [k (f bindings)])) entries)))

    (vector? expression)
    (let [[op & args] expression]
      (case op
        :literal (do (arity! op args 1 1) (constantly (first args)))
        :get (do (arity! op args 1 nil)
                 (fn [bindings]
                   (let [missing #?(:clj (Object.) :cljs (js-obj))
                         value (get-in bindings args missing)]
                     (when (identical? missing value)
                       (throw (ex-info "Missing construction binding"
                                       {:error-type :executor/missing-binding :path (vec args)})))
                     value)))
        :if (do (arity! op args 3 3)
                (let [[pred yes no] (mapv compile-expression args)]
                  (fn [bindings] ((if (pred bindings) yes no) bindings))))
        (if-let [[lo hi f] (get operations op)]
          (do (arity! op args lo hi)
              (let [fs (mapv compile-expression args)]
                (fn [bindings]
                  (let [result (apply f (map #(% bindings) fs))]
                    (when (and (number? result) (not (schema/finite-number? result)))
                      (throw (ex-info "Expression produced a non-finite number"
                                      {:error-type :executor/non-finite :operator op})))
                    result))))
          (if (keyword? op)
            (throw (ex-info "Unknown expression operator; quote literal data"
                            {:error-type :executor/operator :operator op}))
            (let [fs (mapv compile-expression expression)]
              (fn [bindings] (mapv #(% bindings) fs)))))))
    :else (constantly expression)))

(defn evaluate
  "Expression and bindings → value, using the same compiler as width rules."
  [expression bindings]
  ((compile-expression expression) bindings))

(defn references
  "Expression → all declared :get paths, including both :if branches.
   Diagnostic syntax information only; never observations or cache keys."
  [expression]
  (cond
    (map? expression) (into #{} (mapcat references) (vals expression))
    (vector? expression) (case (first expression)
                          :literal #{}
                          :get #{(vec (rest expression))}
                          (into #{} (mapcat references) expression))
    :else #{}))

(declare run-steps)

(defn- run-step [capabilities bindings {:keys [bind call args value each item steps] :as step}]
  (cond
    (contains? step :each)
    (do
      (when-not item
        (throw (ex-info "Each requires an explicit item binding" {:error-type :executor/missing-item})))
      (let [elements (evaluate each bindings)]
        (when-not (sequential? elements)
          (throw (ex-info "Each requires a finite sequence value" {:error-type :executor/each-value})))
        (reduce (fn [scope element]
                  (let [result (run-steps capabilities (assoc scope item element) steps)]
                    (if (contains? scope item) (assoc result item (get scope item)) (dissoc result item))))
                bindings elements)))
    call
    (let [capability (get capabilities call)]
      (when-not capability
        (throw (ex-info "Unavailable construction capability"
                        {:error-type :executor/unavailable-capability :capability call})))
      (let [result (apply capability (map #(evaluate % bindings) args))]
        (if bind (assoc bindings bind result) bindings)))
    bind (assoc bindings bind (evaluate value bindings))
    :else (throw (ex-info "Malformed construction step" {:error-type :executor/invalid-step :step step}))))

(defn run-steps
  "Capabilities, bindings and ordered steps → final bindings."
  [capabilities bindings steps]
  (reduce (partial run-step capabilities) bindings steps))

(defn execute
  "Program {:bindings :steps :return}, inputs and capabilities →
   {:value :bindings}. Missing inputs/operations throw before later steps.
   Program and the complete supplied inputs define the computation."
  [{:keys [bindings steps return]} inputs capabilities]
  (let [scope (reduce (fn [scope [name expression]] (assoc scope name (evaluate expression scope))) inputs bindings)
        result (run-steps capabilities scope steps)]
    {:value (evaluate return result) :bindings result}))
