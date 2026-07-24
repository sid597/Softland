(ns app.shared.facet-material
  "Shared, total conventions for one served facet-master. A facet namespace
   supplies its identities, grammar declarations, and default/floor forms;
   this namespace supplies the reusable compile, resolve, stamp, and minimal
   collision rules proven by the second wearer."
  (:require #?(:clj [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])))

(def common-form-keys
  #{:facet-master/id
    :facet-master/grammar
    :facet-master/facet})

(defn finite-number?
  [x]
  (and (number? x)
       #?(:clj (Double/isFinite (double x))
          :cljs (js/Number.isFinite x))))

(defn valid-rgba?
  [x]
  (and (vector? x)
       (= 4 (count x))
       (every? #(and (finite-number? %) (<= 0 % 1)) x)))

(defn non-negative-number?
  [x]
  (and (finite-number? x) (<= 0 x)))

(defn integer-number?
  [x]
  (integer? x))

(defn- grammar-declaration
  [spec form]
  (get-in spec [:facet-master/grammars (:facet-master/grammar form)]))

(defn compile-form
  "Compile one facet form against its declared grammar without throwing.

   `:facet-master/grammars` maps a version to:
   - `:material-keys` — the exact keys returned to the renderer;
   - `:validators` — key -> {:valid? fn :error-type keyword}.

   Old grammar declarations remain explicit entries. Adding a grammar never
   reinterprets an already-durable form."
  [spec form]
  (let [master-id (:facet-master/id spec)
        facet (:facet-master/facet spec)
        declaration (when (map? form) (grammar-declaration spec form))
        material-keys (set (:material-keys declaration))
        allowed-keys (into common-form-keys material-keys)
        unknown (when (and (map? form) declaration)
                  (seq
                   (sort-by pr-str
                            (remove allowed-keys (keys form)))))
        validation-errors
        (when declaration
          (->> (:validators declaration)
               (sort-by (comp pr-str key))
               (keep (fn [[k {:keys [valid? error-type]}]]
                       (when-not (valid? (get form k))
                         {:type error-type
                          :actual (get form k)})))))
        errors (cond-> []
                 (not (map? form))
                 (conj {:type :facet-master/not-a-map})

                 (and (map? form)
                      (not= master-id (:facet-master/id form)))
                 (conj {:type :facet-master/id-invalid
                        :expected master-id
                        :actual (:facet-master/id form)})

                 (and (map? form)
                      (not (contains? (:facet-master/grammars spec)
                                      (:facet-master/grammar form))))
                 (conj {:type :facet-master/grammar-invalid
                        :expected
                        (vec (sort (keys (:facet-master/grammars spec))))
                        :actual (:facet-master/grammar form)})

                 (and (map? form)
                      (not= facet (:facet-master/facet form)))
                 (conj {:type :facet-master/facet-invalid
                        :expected facet
                        :actual (:facet-master/facet form)})

                 (seq validation-errors)
                 (into validation-errors)

                 unknown
                 (conj {:type :facet-master/unknown-keys
                        :keys (vec unknown)}))]
    (if (seq errors)
      {:valid? false :errors errors :grammar nil :material nil}
      {:valid? true
       :errors []
       :grammar (:facet-master/grammar form)
       :material (select-keys form material-keys)})))

(defn compile-source
  [spec source]
  (try
    (compile-form spec (edn/read-string (str source)))
    (catch #?(:clj Throwable :cljs :default) t
      {:valid? false
       :errors [{:type :facet-master/parse-error
                 :message #?(:clj (.getMessage t)
                             :cljs (.-message t))}]
       :grammar nil
       :material nil})))

(defn source-for
  [form]
  (pr-str form))

(defn code-floor
  [spec]
  (let [compiled (compile-form spec (:facet-master/floor-form spec))]
    (merge (:material compiled)
           {:facet-master/id (:facet-master/id spec)
            :facet-master/facet (:facet-master/facet spec)
            :facet-master/grammar (:grammar compiled)
            :facet-master/revision-id
            (:facet-master/code-floor-revision-id spec)
            :facet-master/floor? true})))

(defn valid-material?
  [spec grammar material]
  (when-let [declaration
             (get-in spec [:facet-master/grammars grammar])]
    (let [material-keys (set (:material-keys declaration))]
      (and (map? material)
           (= material-keys (set (keys material)))
           (every?
            (fn [[k {:keys [valid?]}]]
              (valid? (get material k)))
            (:validators declaration))))))

(defn resolved-wear
  "Resolve only complete served active material. Anything absent, malformed,
   or from the wrong master returns that facet's total code floor."
  [spec served]
  (let [master-id (:facet-master/id spec)
        revision-id (:facet-master/active-revision-id served)
        grammar (:facet-master/grammar served)
        material (:facet-master/material served)]
    (if (and (= master-id (:facet-master/id served))
             (string? revision-id)
             (valid-material? spec grammar material))
      (merge material
             {:facet-master/id master-id
              :facet-master/facet (:facet-master/facet spec)
              :facet-master/grammar grammar
              :facet-master/revision-id revision-id
              :facet-master/floor? false})
      (code-floor spec))))

(defn contribution-stamp
  "Causal render stamp at the grain currently known. The attachment is
   explicitly derived from the subject+facet until a durable attachment owner
   exists; view-instance never enters the identity."
  [wear subject site role slot]
  {:material/subject subject
   :material/attachment
   [:derived (:facet-master/facet wear) subject]
   :material/master (:facet-master/id wear)
   :material/revision (:facet-master/revision-id wear)
   :material/site site
   :material/role role
   :material/slot slot})

(defn compose
  "Minimum composition vocabulary demanded by the first collision.

   Descriptors carry `:wear`, `:stamp`, and `:value`. Same-slot contributions
   compose only when every active master declares `:append` and priorities are
   distinct. The deterministic order is still returned on conflict so the
   caller can keep rendering, but `:conflicts` must be rendered as lint."
  [descriptors]
  (let [descriptors (vec (remove nil? descriptors))
        groups (vals (group-by #(get-in % [:stamp :material/slot])
                               descriptors))
        conflicts
        (->> groups
             (keep
              (fn [group]
                (when (> (count group) 1)
                  (let [group
                        (sort-by
                         #(get-in % [:stamp :material/master])
                         group)
                        merges (mapv #(get-in % [:wear :facet-master/merge])
                                      group)
                        priorities
                        (mapv #(get-in % [:wear :facet-master/priority])
                              group)
                        reason
                        (cond
                          (not-every? #{:append} merges)
                          :material-composition/merge-missing-or-incompatible

                          (not-every? integer? priorities)
                          :material-composition/priority-missing

                          (not= (count priorities)
                                (count (set priorities)))
                          :material-composition/priority-tie)]
                    (when reason
                      {:type reason
                       :material/slot
                       (get-in (first group) [:stamp :material/slot])
                       :material/masters
                       (->> group
                            (map #(get-in % [:stamp :material/master]))
                            sort
                            vec)
                       :material/priorities priorities})))))
             (sort-by (juxt (comp pr-str :material/slot)
                            (comp pr-str :type)))
             vec)
        ordered
        (->> descriptors
             (sort-by
              (fn [{:keys [wear stamp]}]
                [(or (:facet-master/priority wear)
                     #?(:clj Long/MAX_VALUE :cljs js/Number.MAX_SAFE_INTEGER))
                 (str (:material/master stamp))
                 (pr-str (:material/site stamp))]))
             vec)]
    {:contributions ordered
     :conflicts conflicts}))
