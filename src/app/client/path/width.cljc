(ns app.client.path.width
  "Compile a width declaration in the shared recipe expression language.
   Takes numeric parameters and a width expression. Gives a pure width
   function and its declared names. Holds no state. Source and geometry
   use this same function; source_test and executor_test exercise it."
  (:require [app.client.engine.executor :as executor]
            [app.client.engine.schema :as schema]))

(def default-rule
  [:* [:get :size] [:- 1.0 [:* [:get :thinning] [:- 1.0 [:get :p]]]]])

(defn width-function
  "Parameters → {:width-fn (pressure, progress, speed → width) :names}.
   Defaults are size 8, thinning .5. Invalid expressions are explicit
   errors. Negative finite widths clamp to zero, as in the bench."
  [parameters]
  (let [rule (or (:width parameters) default-rule)
        _ (when-not (or (number? rule) (vector? rule))
            (throw (ex-info "Width is an EDN expression" {:error-type :path/width-expression :value rule})))
        compiled (executor/compile-expression rule)
        names (set (map first (executor/references rule)))
        base (merge {:size 8.0 :thinning 0.5} (into {} (filter (comp number? val)) parameters))]
    {:names names
     :width-fn (fn [p s speed]
                 (let [w (compiled (assoc base :p p :s s :v speed))]
                   (when-not (schema/finite-number? w)
                     (throw (ex-info "Width must be a finite number" {:error-type :path/width :value w})))
                   (max 0.0 w)))}))
