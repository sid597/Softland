(ns app.client.engine.schema
  "Reject malformed declared maps at a common boundary.

   Input: schema description and EDN value. Output: the original value, or
   an exception carrying :error-type, :path, and :value. It recursively
   dispatches among predicates, sets, nested maps, vectors, and maps of
   entries; then checks whole-form invariants. There is no retained state.
   Family schemas supply vocabulary; this file supplies validation
   mechanics.

   Folder map: README.md."
  (:refer-clojure :exclude [check]))

(defn finite-number?
  "Value → boolean: numeric and finite.

   Platform-specific finite check. Rejects NaN/infinity explicitly."
  [value]
  (and (number? value)
       #?(:clj (Double/isFinite (double value))
          :cljs (js/Number.isFinite value))))

(defn non-negative-number?
  "Value → finite number ≥ 0?

   Builds on the common predicate."
  [value]
  (and (finite-number? value) (not (neg? value))))

(defn positive-number?
  "Value → finite number > 0?

   Builds on the common predicate."
  [value]
  (and (finite-number? value) (pos? value)))

(defn valid-rgba?
  "Value → four finite channels in [0,1]?

   Checks vector shape and each channel. Intended for the declared bounded
   RGBA contract."
  [value]
  (and (vector? value)
       (= 4 (count value))
       (every? #(and (finite-number? %) (<= 0.0 % 1.0)) value)))

(defn point?
  "Value → two finite coordinates?

   Fixed-width vector validation."
  [value]
  (and (vector? value)
       (= 2 (count value))
       (every? finite-number? value)))

(defn- reject!
  "Message, error type, path, value, optional data → throws.

   Central exception construction. Merged extra data can override common
   fields."
  [message error-type path value & [data]]
  (throw (ex-info message
                  (merge {:error-type error-type :path path :value value}
                         data))))

(declare check-form!)

(defn- check-predicate!
  "Predicate, value, path → nil on acceptance; throws otherwise.

   Preserves named predicate failures. Predicate-specific exception data can
   override the supplied path."
  [predicate value path]
  (try
    (when-not (predicate value)
      (reject! "Schema predicate rejected value"
               :schema/invalid-value path value))
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (if-let [error-type (:error-type (ex-data error))]
        (reject! (ex-message error) error-type path value (ex-data error))
        (throw error)))))

(defn- check-vector!
  "Vector schema, value, path → original vector or throws.

   Checks schema shape, minimum length, uniqueness, then indexed elements.
   Uniqueness adds a temporary identity vector/set."
  [[_ item-spec opts :as validator] value path]
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

(defn- check-map-of!
  "Map-of schema, value, path → original map or throws.

   Sorts entries for deterministic traversal, validates keys and values.
   Sorting is a deliberate cost for diagnostic stability."
  [[_ key-pred item-spec :as validator] value path]
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

(defn- check-validator!
  "Validator, value, path → original value or throws.

   Explicit dispatch before generic callable handling. Sets and vectors
   cannot accidentally act as predicates."
  [validator value path]
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

(defn- check-form!
  "Map spec, form, path → original form or throws.

   Enforces required/allowed keys, field validators, form invariants. A
   missing required key can first fail its nil predicate, so the reported
   error need not be missing-key."
  [spec form path]
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

(defn check
  "Spec and form → validated unchanged form.

   Supplies root path []. A narrow public entry point."
  [spec form]
  (check-form! spec form []))
