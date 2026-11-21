(ns auth-blind.run
  "Entry point for running the blind Auth property suite against any 18-function
  Auth map, plus verifying that every single-rule broken variant is caught."
  (:require [auth-blind.model :as model]
            [auth-blind.mutants :as mutants]
            [auth-blind.properties :as props]
            [clojure.test.check :as tc]))

(defn check-auth-shape [auth]
  (let [missing (remove #(ifn? (get auth %)) props/required-calls)]
    (when (seq missing)
      {:missing-functions (vec missing)})))

(defn run-suite
  "Runs all spec properties against `auth` (a map of the 18 contract functions).
  Options:
    `:new-world!`  0-arg fn -> `{:cluster c :module-name m :cleanup! (fn [])}`
                   (defaults to `auth-blind.model/make-world` for in-memory runs)
    `:seed`        long seed for deterministic reproduction
    `:scale`       multiplier on per-property trial counts (default 1.0)
    `:quiet?`      suppress per-property lines

  Returns `{:pass? bool :results [...] :shape-error map-or-nil}`."
  ([auth] (run-suite auth {}))
  ([auth {:keys [new-world! seed scale quiet?]
          :or {new-world! #(let [w (model/make-world "auth")]
                             {:cluster w :module-name "auth" :cleanup! nil})
               seed 20261121
               scale 1.0
               quiet? false}}]
   (if-let [err (check-auth-shape auth)]
     {:pass? false :shape-error err :results []}
     (let [results
           (mapv (fn [{:keys [id sentence trials make-prop]}]
                   (let [n (max 1 (long (Math/round (* (double scale) (double trials)))))
                         res (try
                               (tc/quick-check n (make-prop auth new-world!) :seed seed :max-size 45)
                               (catch Throwable t
                                 {:pass? false :threw (.getName (class t)) :message (.getMessage t)}))]
                     (when-not quiet?
                       (println (format "  [%s] (%d trials) %s"
                                        (if (:pass? res) "PASS" "FAIL")
                                        n
                                        sentence))
                       (when-not (:pass? res)
                         (println "         failure detail:"
                                  (select-keys res [:result :failed-after-ms :num-tests :seed :shrunk :threw :message]))))
                     (assoc res :id id :sentence sentence)))
                 props/properties)]
       {:pass? (every? :pass? results)
        :results results}))))

(defn run-broken-variants
  "Runs the property suite against each broken variant in
  `auth-blind.mutants/broken-variants`, stopping each variant at the first
  property that catches it."
  [{:keys [seed quiet?] :or {seed 20261121 quiet? false}}]
  (let [default-world #(let [w (model/make-world "auth")]
                         {:cluster w :module-name "auth" :cleanup! nil})
        outcomes
        (mapv (fn [{:keys [id breaks auth]}]
                (let [caught-by
                      (some (fn [{:keys [sentence trials make-prop] prop-id :id}]
                              (let [res (try
                                          (tc/quick-check trials (make-prop auth default-world) :seed seed :max-size 45)
                                          (catch Throwable t
                                            {:pass? false :threw (.getName (class t))}))]
                                (when-not (:pass? res)
                                  {:property-id prop-id :sentence sentence})))
                            props/properties)]
                  (when-not quiet?
                    (println (format "  [%s] %-36s (%s)%s"
                                     (if caught-by "CAUGHT" "MISSED")
                                     (name id)
                                     breaks
                                     (if caught-by (str " -> by " (name (:property-id caught-by))) ""))))
                  {:id id :breaks breaks :caught? (some? caught-by) :caught-by caught-by}))
              mutants/broken-variants)]
    {:all-caught? (every? :caught? outcomes)
     :outcomes outcomes}))

(defn -main [& args]
  (let [seed (if (seq args) (parse-long (first args)) 20261121)]
    (println "=== 1. Blind Auth Contract Properties against Spec Model (seed" seed ") ===")
    (let [suite-res (run-suite (model/make-auth) {:seed seed})]
      (println (format "Suite result: %d/%d properties passed."
                       (count (filter :pass? (:results suite-res)))
                       (count (:results suite-res))))
      (println)
      (println "=== 2. Broken Variants (one rule broken at a time) ===")
      (let [mut-res (run-broken-variants {:seed seed})]
        (println (format "Mutants result: %d/%d broken variants caught."
                         (count (filter :caught? (:outcomes mut-res)))
                         (count (:outcomes mut-res))))
        (System/exit (if (and (:pass? suite-res) (:all-caught? mut-res)) 0 1))))))
