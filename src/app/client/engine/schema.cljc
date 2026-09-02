(ns app.client.engine.schema
  "A small declared-map schema shared by client material kinds.
   Takes: a schema spec and an EDN form.
   Gives: the unchanged form, or a named rejection with its path and value.
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

(defn- reject! [message error-type path value & [data]]
  (throw (ex-info message
                  (merge {:error-type error-type :path path :value value}
                         data))))

(declare check-form!)

(defn- check-predicate! [predicate value path]
  (try
    (when-not (predicate value)
      (reject! "Schema predicate rejected value"
               :schema/invalid-value path value))
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (if-let [error-type (:error-type (ex-data error))]
        (reject! (ex-message error) error-type path value (ex-data error))
        (throw error)))))

(defn- check-vector! [[_ item-spec opts :as validator] value path]
  (when-not (= 3 (count validator))
    (reject! "Invalid vector schema" :schema/invalid-spec path validator))
  (when-not (vector? value)
    (reject! "Schema requires a vector" :schema/vector-required path value))
  (when-let [minimum (:min opts)]
    (when (< (count value) minimum)
      (reject! "Schema vector is too short" :schema/vector-too-short
               path value {:minimum minimum})))
  (when-let [unique-by (:unique-by opts)]
    (let [identities (mapv unique-by value)]
      (when-not (= (count identities) (count (set identities)))
        (reject! "Schema vector identities must be unique"
                 :schema/not-unique path value {:unique-by unique-by}))))
  (doseq [[index item] (map-indexed vector value)]
    (cond
      (map? item-spec) (check-form! item-spec item (conj path index))
      (ifn? item-spec) (check-predicate! item-spec item (conj path index))
      :else (reject! "Invalid vector item schema"
                     :schema/invalid-spec path item-spec)))
  value)

(defn- check-map-of! [[_ key-pred item-spec :as validator] value path]
  (when-not (= 3 (count validator))
    (reject! "Invalid map-of schema" :schema/invalid-spec path validator))
  (when-not (map? value)
    (reject! "Schema requires a map" :schema/map-required path value))
  (when-not (ifn? key-pred)
    (reject! "Invalid map key schema" :schema/invalid-spec path key-pred))
  (doseq [[key item] (sort-by (comp pr-str first) value)]
    (when-not (key-pred key)
      (reject! "Schema map key was rejected"
               :schema/invalid-key (conj path key) key))
    (cond
      (map? item-spec) (check-form! item-spec item (conj path key))
      (ifn? item-spec) (check-predicate! item-spec item (conj path key))
      :else (reject! "Invalid map value schema"
                     :schema/invalid-spec path item-spec)))
  value)

(defn- check-validator! [validator value path]
  (cond
    (set? validator)
    (when-not (contains? validator value)
      (reject! "Schema membership rejected value"
               :schema/invalid-value path value {:allowed validator}))

    (and (vector? validator) (= :vector-of (first validator)))
    (check-vector! validator value path)

    (and (vector? validator) (= :map-of (first validator)))
    (check-map-of! validator value path)

    (map? validator)
    (check-form! validator value path)

    (ifn? validator)
    (check-predicate! validator value path)

    :else
    (reject! "Invalid schema validator" :schema/invalid-spec path validator))
  value)

(defn- check-form! [spec form path]
  (when-not (map? form)
    (reject! "Schema requires a map" :schema/map-required path form))
  (let [required (:keys spec #{})
        optional (:optional spec #{})
        validators (:validators spec {})
        allowed (into required optional)
        present (set (keys form))]
    (doseq [key (sort-by pr-str (remove present required))]
      (if-let [validator (get validators key)]
        (do (check-validator! validator nil (conj path key))
            (reject! "Schema is missing a required key"
                     :schema/missing-key (conj path key) nil))
        (reject! "Schema is missing a required key"
                 :schema/missing-key (conj path key) nil)))
    (doseq [key (sort-by pr-str (remove allowed present))]
      (reject! "Schema has an unknown key"
               :schema/unknown-key (conj path key) (get form key)))
    (doseq [[key validator] validators
            :when (contains? form key)]
      (check-validator! validator (get form key) (conj path key)))
    (doseq [{:keys [valid? error-type explain]} (:form-validators spec)]
      (when-not (valid? form)
        (reject! "Schema form invariant failed" error-type path form
                 (when explain (explain form)))))
    form))

(defn check [spec form]
  (check-form! spec form []))
