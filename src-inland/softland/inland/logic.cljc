(ns softland.inland.logic
  "Pure finite relational closure over explicit facts and rules.
   Takes identified tuples, range-restricted positive rules, lower-stratum negative
   conditions and a budget; gives conclusions with minimal support sets or a tagged
   failure/exhaustion. Holds temporary maps and a work counter only during a call.
   The caller owns source completeness and reruns this solver when its inputs change.
   This is a bounded full closure calculation, not an incremental truth-maintenance
   store. Budget units count join candidates and rule emissions, not elapsed time."
  (:refer-clojure :exclude [derive])
  (:require [clojure.set :as set]))

(defn variable?
  "Value → true when it is a symbol whose spelling begins with question mark."
  [x] (and (symbol? x) (.startsWith (str x) "?")))
(defn variables
  "Tuple → set of top-level variable symbols; nested terms are not traversed."
  [tuple] (set (filter variable? tuple)))
(defn unify
  "Pattern tuple, concrete tuple and bindings → extended bindings or nil.
   Requires equal tuple length; constants and repeated variables must match exactly.
   Unification is flat and adds no values beyond those in the concrete tuple."
  [pattern tuple bindings]
  (when (= (count pattern) (count tuple))
    (reduce (fn [b [p v]]
              (when b
                (if (variable? p)
                  (if (contains? b p) (when (= (get b p) v) b) (assoc b p v))
                  (when (= p v) b)))) bindings (map vector pattern tuple))))

(defn rule-error
  "Rules → first range-restriction or stratum error, otherwise nil.
   Head/negative variables must be positively bound; negative dependencies must be
   strictly lower and positive dependencies no higher than the rule's stratum."
  [rules]
  (let [levels (reduce (fn [m r] (update m (first (:head r)) (fnil max -1) (:stratum r 0))) {} rules)]
    (some (fn [r]
            (let [bound (apply set/union #{} (map variables (:body r)))]
              (cond
                (not (set/subset? (variables (:head r)) bound)) "A recursive head cannot manufacture values."
                (some #(not (set/subset? (variables %) bound)) (:not r)) "Negation uses positively bound variables."
                (some #(>= (get levels (first %) -1) (:stratum r 0)) (:not r)) "Negation must refer to a completed lower stratum."
                (some #(> (get levels (first %) -1) (:stratum r 0)) (:body r)) "A positive dependency cannot read a later stratum."))) rules)))

(defn minimal-supports
  "Set of support-id sets → remove strict supersets of another support.
   Independent minimal proofs remain available; redundant larger proofs are omitted."
  [supports]
  (set (remove (fn [s] (some #(and (not= s %) (set/subset? % s)) supports)) supports)))

(defn derive*
  "Validated fact/rule vectors and budget → completed closure or tagged outcome.
   Iterates strata in order and each positive stratum to a fixed point; negative
   lookups use completed lower strata. Caps work at 10000 and conclusions at 512.
   Its catch maps calculation exceptions to exhaustion; derive handles input shape.
   Work accounting does not meter every negative lookup or support-set operation."
  [{:keys [facts rules budget] :or {budget 4096}}]
  (if-let [error (rule-error rules)]
    {:runtime/status :failed :reason error}
    (let [work (atom (min 10000 budget))
          spend! #(when (neg? (swap! work dec)) (throw (ex-info "Relational work budget exhausted." {})))
          join (fn [known patterns]
                 (reduce (fn [bindings pattern]
                           (vec (for [[b support] bindings [tuple supports] known s supports
                                      :let [_ (spend!) next (unify pattern tuple b)] :when next]
                                  [next (set/union support s)]))) [[{} #{}]] patterns))]
      (try
        (let [initial (reduce (fn [m {:keys [id tuple]}] (update m tuple (fnil conj #{}) #{id})) {} facts)
              result
              (reduce
                (fn [lower level]
                  (loop [known lower]
                    (let [next
                          (reduce
                            (fn [out rule]
                              (reduce
                                (fn [out [bindings support]]
                                  (spend!)
                                  (if (some (fn [negative] (some #(unify negative % bindings) (keys lower))) (:not rule)) out
                                    (let [tuple (mapv #(get bindings % %) (:head rule))
                                          support (conj support (:id rule))]
                                      (update out tuple #(minimal-supports (conj (or % #{}) support))))))
                                out (join known (:body rule)))) known (filter #(= level (:stratum % 0)) rules))]
                      (when (> (count next) 512) (throw (ex-info "Finite conclusion budget exceeded." {})))
                      (if (= known next) next (recur next))))) initial (sort (set (map #(:stratum % 0) rules))))]
          {:conclusions result :work (- (min 10000 budget) @work) :complete? true})
        (catch #?(:clj Throwable :cljs :default) _
          {:runtime/status :exhausted :reason "The finite derivation exhausted its work budget; absence is not established."})))))

(defn derive
  "Authored derive arguments → bounded closure or explicit failure.
   Requires fact/rule vectors of at most 512/64 entries and a positive integer budget
   (default 4096). Failure or exhaustion never establishes negative absence."
  [args]
  (try
    (if (and (vector? (:facts args)) (vector? (:rules args))
             (<= (count (:facts args)) 512) (<= (count (:rules args)) 64)
             (integer? (:budget args 4096)) (pos? (:budget args 4096)))
      (derive* args)
      {:runtime/status :failed :reason "A finite derivation needs bounded fact and rule vectors and a positive budget."})
    (catch #?(:clj Throwable :cljs :default) _
      {:runtime/status :failed :reason "The relational recipe cannot consume these inputs; absence is not established."})))
