(ns softland.inland.logic
  "Finite positive relational closure with stratified absence. Takes explicit
   facts/rules and a work budget. Gives conclusions with independent supports.
   Holds no sources or retained result; callers own the input dependency scope."
  (:refer-clojure :exclude [derive])
  (:require [clojure.set :as set]))

(defn variable? [x] (and (symbol? x) (.startsWith (str x) "?")))
(defn variables [tuple] (set (filter variable? tuple)))
(defn unify [pattern tuple bindings]
  (when (= (count pattern) (count tuple))
    (reduce (fn [b [p v]]
              (when b
                (if (variable? p)
                  (if (contains? b p) (when (= (get b p) v) b) (assoc b p v))
                  (when (= p v) b)))) bindings (map vector pattern tuple))))

(defn rule-error [rules]
  (let [levels (reduce (fn [m r] (update m (first (:head r)) (fnil max -1) (:stratum r 0))) {} rules)]
    (some (fn [r]
            (let [bound (apply set/union #{} (map variables (:body r)))]
              (cond
                (not (set/subset? (variables (:head r)) bound)) "A recursive head cannot manufacture values."
                (some #(not (set/subset? (variables %) bound)) (:not r)) "Negation uses positively bound variables."
                (some #(>= (get levels (first %) -1) (:stratum r 0)) (:not r)) "Negation must refer to a completed lower stratum."
                (some #(> (get levels (first %) -1) (:stratum r 0)) (:body r)) "A positive dependency cannot read a later stratum."))) rules)))

(defn minimal-supports [supports]
  (set (remove (fn [s] (some #(and (not= s %) (set/subset? % s)) supports)) supports)))

(defn derive*
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

(defn derive [args]
  (try
    (if (and (vector? (:facts args)) (vector? (:rules args))
             (<= (count (:facts args)) 512) (<= (count (:rules args)) 64)
             (integer? (:budget args 4096)) (pos? (:budget args 4096)))
      (derive* args)
      {:runtime/status :failed :reason "A finite derivation needs bounded fact and rule vectors and a positive budget."})
    (catch #?(:clj Throwable :cljs :default) _
      {:runtime/status :failed :reason "The relational recipe cannot consume these inputs; absence is not established."})))
