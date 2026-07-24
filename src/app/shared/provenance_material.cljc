(ns app.shared.provenance-material
  (:require #?(:clj [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])))

(def master-id "fm:provenance")
(def grammar-version 0)
(def code-floor-revision-id "code-floor:fm:provenance:v0")

(def default-form
  {:facet-master/id master-id
   :facet-master/grammar grammar-version
   :facet-master/facet :provenance
   :provenance/tint [0.62 0.66 0.76 0.6]})

(def default-source (pr-str default-form))

(def ^:private allowed-keys
  #{:facet-master/id
    :facet-master/grammar
    :facet-master/facet
    :provenance/tint})

(defn- finite-number?
  [x]
  (and (number? x)
       #?(:clj (Double/isFinite (double x))
          :cljs (js/Number.isFinite x))))

(defn valid-tint?
  [x]
  (and (vector? x)
       (= 4 (count x))
       (every? #(and (finite-number? %) (<= 0 % 1)) x)))

(defn compile-form
  "Total compiler for the one P1 facet. It accepts data only and returns a
   render-ready value or closed, machine-readable errors."
  [form]
  (let [unknown (when (map? form)
                  (seq (remove allowed-keys (keys form))))
        errors (cond-> []
                 (not (map? form))
                 (conj {:type :facet-master/not-a-map})

                 (and (map? form)
                      (not= master-id (:facet-master/id form)))
                 (conj {:type :facet-master/id-invalid
                        :expected master-id
                        :actual (:facet-master/id form)})

                 (and (map? form)
                      (not= grammar-version (:facet-master/grammar form)))
                 (conj {:type :facet-master/grammar-invalid
                        :expected grammar-version
                        :actual (:facet-master/grammar form)})

                 (and (map? form)
                      (not= :provenance (:facet-master/facet form)))
                 (conj {:type :facet-master/facet-invalid
                        :expected :provenance
                        :actual (:facet-master/facet form)})

                 (and (map? form)
                      (not (valid-tint? (:provenance/tint form))))
                 (conj {:type :provenance/tint-invalid
                        :actual (:provenance/tint form)})

                 unknown
                 (conj {:type :facet-master/unknown-keys
                        :keys (vec unknown)}))]
    (if (seq errors)
      {:valid? false :errors errors :material nil}
      {:valid? true
       :errors []
       :material {:provenance/tint (:provenance/tint form)}})))

(defn compile-source
  "Parse and compile material source without throwing."
  [source]
  (try
    (compile-form (edn/read-string (str source)))
    (catch #?(:clj Throwable :cljs :default) t
      {:valid? false
       :errors [{:type :facet-master/parse-error
                 :message #?(:clj (.getMessage t)
                             :cljs (.-message t))}]
       :material nil})))

(def code-floor
  (assoc (:material (compile-form default-form))
         :facet-master/id master-id
         :facet-master/revision-id code-floor-revision-id
         :facet-master/floor? true))

(defn resolved-wear
  "Return the served active material when complete, otherwise the shared code
   floor. The floor is also the exact form imported at bootstrap, so P1 has
   one default policy value rather than a client-side tint copy."
  [served]
  (let [tint (get-in served [:facet-master/material :provenance/tint])
        revision-id (:facet-master/active-revision-id served)]
    (if (and (= master-id (:facet-master/id served))
             (string? revision-id)
             (valid-tint? tint))
      {:facet-master/id master-id
       :facet-master/revision-id revision-id
       :facet-master/floor? false
       :provenance/tint tint}
      code-floor)))

(defn contribution-stamp
  [wear site]
  {:material/master (:facet-master/id wear)
   :material/revision (:facet-master/revision-id wear)
   :material/site site})
