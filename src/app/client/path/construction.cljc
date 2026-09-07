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
            [app.client.path.stroke :as stroke]
            [app.client.path.surface :as path-surface]
            [app.client.engine.surface :as surface]))

(def capabilities
  "Named pure operations for construction and painting programs. :needs
   declares required/alternative argument groups; the executor checks it."
  {:vocabulary "path kind, production 2: sample nearest · mix linear · paint = coverage × opacity·alpha, source-over, linear premultiplied RGBA32F · dabs by arc length · capsule nib"
   :path/source {:args [:source :tool] :needs [[:source] [:tool]]
                 :run (fn [{:keys [source tool]} _] (source/build source tool))}
   :path/envelope {:args [:path :stroke :options] :needs [[:path] [:stroke] [:options]]
                   :run (fn [{:keys [path stroke options]} _] (stroke/envelope path stroke options))}
   :path/dabs {:args [:path :tool :stroke] :needs [[:path] [:tool] [:stroke]]
               :run (fn [{:keys [path tool stroke]} _]
                      (:dabs (stroke/dabs path (component/stroke-options stroke tool 1.0))))}
   :path/regions {:args [:component :view] :needs [[:component] [:view]]
                  :run (fn [{:keys [component view]} _] (component/regions component view))}
   :surface/new {:args [:declaration] :needs [[:declaration]] :run (fn [{:keys [declaration]} _] (surface/new declaration))}
   :sample {:args [:surface :point :filter] :needs [[:surface] [:point] [:filter]]
            :run (fn [{:keys [surface point filter]} ctx] (surface/sample [surface] point filter ctx))}
   :mix {:args [:a :b :amount] :needs [[:a] [:b] [:amount]]
         :run (fn [{:keys [a b amount]} _] (surface/mix a b amount))}
   :paint {:args [:surface :region :rgba :opacity :blend :clips]
           :needs [[:surface] [:region] [:rgba] [:opacity] [:blend]] :run path-surface/paint}})

(defn program-record
  "Authored path record → executor record. Identity and authored source are
   caller concerns; program roots use unqualified field names."
  [record]
  {:program (:path/program record)
   :roots (into {} (for [[k v] record :when (and (= "path" (namespace k))
                                                 (not (#{:path/material-id :path/revision :path/program :path/construction} k)))]
                     [(keyword (name k)) (if (= k :path/tool) (dissoc v :name) v)]))})

(defn default-construction
  "Authored record → recipe returning a path and overlap declaration.
   Overlap is captured as data, preserving the original union-to-dabs
   recipe replacement counterexample; other paint fields stay caller inputs."
  [record]
  {:steps [{:out :built :op :path/source :args {:source [:get :source] :tool [:get :tool]}}]
   :return (cond-> {:path [:get :built :path]}
             (get-in record [:path/paint :stroke])
             (assoc :paint {:stroke {:overlap (get-in record [:path/paint :stroke :overlap] :union)}}))})

(defn construct
  "Record and capability extensions → validated component. A recipe returns
   a path value directly, or {:path path :paint optional-overrides}. Errors
   propagate with the missing input/capability, never as an empty drawing."
  ([record] (construct record {}))
  ([record extensions]
   (let [run (executor/run {:program (or (:path/construction record) (default-construction record))
                            :roots {:source (:path/source record) :tool (dissoc (:path/tool record) :name)
                                    :paint (:path/paint record)}} {} (merge capabilities extensions))
         _ (when-not (= :complete (:status run))
             (throw (ex-info "Construction did not complete" run)))
         result (:results run)
         result (if (:subpaths result) {:path result} result)
         paint (merge-with merge (:path/paint record) (:paint result))
         width-names (set (map first (executor/references (get-in paint [:stroke :width]))))
         parameters (select-keys (into {} (filter (comp number? val)) (:path/tool record)) width-names)]
     (component/validate-component!
      (cond-> {:path/material-id (:path/material-id record) :path/revision (:path/revision record)
               :path/value (:path result) :path/paint paint :path/parameters parameters}
        (:path/snap? record) (assoc :path/snap? true))))))
