(ns app.client.path.construction
  "Execute a tool record at its caller's edit boundary.

   Takes an authored record and optional pure capability extensions. Gives
   a validated path-value component for rendering. Holds no state or cache.
   The caller keeps the authored record beside that value and calls this
   function on a source/recipe edit; paint edits can update the component
   directly. Cameras and frame clocks are not recipe inputs.
   Evidence: construction_test.clj; the browser production checks exercise
   a recipe edit in a retained renderer."
  (:require [app.client.engine.executor :as executor]
            [app.client.path.component :as component]
            [app.client.path.source :as source]
            [app.client.path.stroke :as stroke]))

(def capabilities
  "Pure path capabilities available to authored recipes. Geometry operations
   take explicit options; returned paths may be reused as another input."
  {:path/source source/build
   :path/envelope stroke/envelope
   :path/dabs stroke/dabs
   :path/regions component/regions})

(defn default-construction
  "Authored record → recipe returning a path and overlap declaration.
   Overlap is captured as data, preserving the original union-to-dabs
   recipe replacement counterexample; other paint fields stay caller inputs."
  [record]
  {:steps [{:bind :built :call :path/source :args [[:get :source] [:get :tool]]}]
   :return (cond-> {:path [:get :built :path]}
             (get-in record [:path/paint :stroke])
             (assoc :paint {:stroke {:overlap (get-in record [:path/paint :stroke :overlap] :union)}}))})

(defn construct
  "Record and capability extensions → validated component. A recipe returns
   a path value directly, or {:path path :paint optional-overrides}. Errors
   propagate with the missing input/capability, never as an empty drawing."
  ([record] (construct record {}))
  ([record extensions]
   (let [result (:value (executor/execute (or (:path/construction record) (default-construction record))
                                          {:source (:path/source record) :tool (dissoc (:path/tool record) :name)
                                           :paint (:path/paint record)
                                           :identity {:id (:path/material-id record) :revision (:path/revision record)}}
                                          (merge capabilities extensions)))
         result (if (:subpaths result) {:path result} result)
         paint (merge-with merge (:path/paint record) (:paint result))
         width-names (set (map first (executor/references (get-in paint [:stroke :width]))))
         parameters (select-keys (into {} (filter (comp number? val)) (:path/tool record)) width-names)]
     (component/validate-component!
      (cond-> {:path/material-id (:path/material-id record) :path/revision (:path/revision record)
               :path/value (:path result) :path/paint paint :path/parameters parameters}
        (:path/snap? record) (assoc :path/snap? true))))))
