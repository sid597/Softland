(ns app.client.engine.grammar
  "A small declared-map grammar shared by client material kinds.
   Takes: a grammar spec and an EDN form.
   Gives: the unchanged form, or a named refusal with its path and value.
   Holds nothing."
  (:refer-clojure :exclude [check]))

(defn finite-number? [value]
  (and (number? value)
       #?(:clj (Double/isFinite (double value))
          :cljs (js/Number.isFinite value))))

(defn non-negative-number? [value]
  (and (finite-number? value) (not (neg? value))))

(defn positive-number? [value]
  (and (finite-number? value) (pos? value)))

(defn valid-rgba? [value]
  (and (vector? value)
       (= 4 (count value))
       (every? #(and (finite-number? %) (<= 0.0 % 1.0)) value)))

(defn point? [value]
  (and (vector? value)
       (= 2 (count value))
       (every? finite-number? value)))

(defn- refuse! [message error-type path value & [data]]
  (throw (ex-info message
                  (merge {:error-type error-type :path path :value value}
                         data))))

(declare check-form!)

(defn- check-predicate! [predicate value path]
  (try
    (when-not (predicate value)
      (refuse! "Grammar predicate refused value"
               :grammar/invalid-value path value))
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (if-let [error-type (:error-type (ex-data error))]
        (refuse! (ex-message error) error-type path value (ex-data error))
        (throw error)))))

(defn- check-vector! [[_ item-spec opts :as validator] value path]
  (when-not (= 3 (count validator))
    (refuse! "Invalid vector grammar" :grammar/invalid-spec path validator))
  (when-not (vector? value)
    (refuse! "Grammar requires a vector" :grammar/vector-required path value))
  (when-let [minimum (:min opts)]
    (when (< (count value) minimum)
      (refuse! "Grammar vector is too short" :grammar/vector-too-short
               path value {:minimum minimum})))
  (when-let [unique-by (:unique-by opts)]
    (let [identities (mapv unique-by value)]
      (when-not (= (count identities) (count (set identities)))
        (refuse! "Grammar vector identities must be unique"
                 :grammar/not-unique path value {:unique-by unique-by}))))
  (doseq [[index item] (map-indexed vector value)]
    (cond
      (map? item-spec) (check-form! item-spec item (conj path index))
      (ifn? item-spec) (check-predicate! item-spec item (conj path index))
      :else (refuse! "Invalid vector item grammar"
                     :grammar/invalid-spec path item-spec)))
  value)

(defn- check-map-of! [[_ key-pred item-spec :as validator] value path]
  (when-not (= 3 (count validator))
    (refuse! "Invalid map-of grammar" :grammar/invalid-spec path validator))
  (when-not (map? value)
    (refuse! "Grammar requires a map" :grammar/map-required path value))
  (when-not (ifn? key-pred)
    (refuse! "Invalid map key grammar" :grammar/invalid-spec path key-pred))
  (doseq [[key item] (sort-by (comp pr-str first) value)]
    (when-not (key-pred key)
      (refuse! "Grammar map key was refused"
               :grammar/invalid-key (conj path key) key))
    (cond
      (map? item-spec) (check-form! item-spec item (conj path key))
      (ifn? item-spec) (check-predicate! item-spec item (conj path key))
      :else (refuse! "Invalid map value grammar"
                     :grammar/invalid-spec path item-spec)))
  value)

(defn- check-validator! [validator value path]
  (cond
    (set? validator)
    (when-not (contains? validator value)
      (refuse! "Grammar membership refused value"
               :grammar/invalid-value path value {:allowed validator}))

    (and (vector? validator) (= :vector-of (first validator)))
    (check-vector! validator value path)

    (and (vector? validator) (= :map-of (first validator)))
    (check-map-of! validator value path)

    (map? validator)
    (check-form! validator value path)

    (ifn? validator)
    (check-predicate! validator value path)

    :else
    (refuse! "Invalid grammar validator" :grammar/invalid-spec path validator))
  value)

(defn- check-form! [spec form path]
  (when-not (map? form)
    (refuse! "Grammar requires a map" :grammar/map-required path form))
  (let [required (:keys spec #{})
        optional (:optional spec #{})
        validators (:validators spec {})
        allowed (into required optional)
        present (set (keys form))]
    (doseq [key (sort-by pr-str (remove present required))]
      (if-let [validator (get validators key)]
        (do (check-validator! validator nil (conj path key))
            (refuse! "Grammar is missing a required key"
                     :grammar/missing-key (conj path key) nil))
        (refuse! "Grammar is missing a required key"
                 :grammar/missing-key (conj path key) nil)))
    (doseq [key (sort-by pr-str (remove allowed present))]
      (refuse! "Grammar has an unknown key"
               :grammar/unknown-key (conj path key) (get form key)))
    (doseq [[key validator] validators
            :when (contains? form key)]
      (check-validator! validator (get form key) (conj path key)))
    (doseq [{:keys [valid? error-type explain]} (:form-validators spec)]
      (when-not (valid? form)
        (refuse! "Grammar form invariant failed" error-type path form
                 (when explain (explain form)))))
    form))

(defn check [spec form]
  (check-form! spec form []))
