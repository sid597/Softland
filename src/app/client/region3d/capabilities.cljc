(ns app.client.region3d.capabilities
  "The 3D vocabulary bound to the shared executor and CPU compositor.
   Takes named arguments, including the whole host as data wherever used.
   Gives regions, reads and paintings; owns no host, store, device or grant.
   Evidence: brush_test.clj. The only support implementation is the sphere."
  (:require [app.client.region3d.support :as support]
            [app.client.region3d.coating :as coating]
            [app.client.region3d.surface-region :as region]
            [app.client.engine.surface :as surface]))

(def vocabulary "3D production 1: explicit sphere host · intrinsic caps · retained affine binding chains and root restrictions · nearest chart samples · source-over RGBA32F · phase-addressed reads")

(defn- read-context [{:keys [host support revision layers order] :as args} ctx]
  (support/validate! host)
  (when-not (= (:id host) (:id support)) (support/refuse! :support support))
  (let [revision (or revision (:revision support) 0)
        _ (support/revision-chart host revision)]
    {:ctx (assoc ctx :domains (support/domains host))
     :stack (mapv (fn [layer]
                    (cond
                      (= "coating" layer)
                      {:layer/kind :coating
                       :snapshot (fn [_ _ _] (coating/snapshot host revision order))
                       :sample (fn [point _ context]
                                 (coating/locate host revision point {:order order :budget (:budget context)}))}
                      (and (map? layer) (:surface/id layer)) layer
                      :else (support/refuse! :layer layer))) layers)}))

(defn read-snapshot [args ctx]
  (let [{:keys [stack ctx]} (read-context args ctx)]
    (assoc (surface/snapshot stack (:point args) (get args :filter :nearest) ctx) :host (:host args))))

(defn read-surface [args ctx]
  (let [{:keys [stack ctx]} (read-context args ctx)]
    (update (surface/sample stack (:point args) (get args :filter :nearest) ctx) :snapshot assoc :host (:host args))))

(defn paint
  "Paint a cap through optional cap clips on a declared chart patch.
   Coverage evaluates membership once per texel centre, never per chart copy."
  [{:keys [host surface region rgba opacity blend clips]} {:keys [at step]}]
  (support/validate! host)
  (let [ctx {:domains (support/domains host)}
        coverage (fn [cap] (fn [x y] (if (region/member cap (surface/to-point surface x y ctx)) 1.0 0.0)))]
    (surface/paint surface region {:kind :color :rgba rgba :opacity (or opacity 1.0) :blend (or blend :source-over)}
                   {:coverage (coverage region) :clips (mapv coverage clips)
                    :key (str (:surface/id surface) ":" at "/" (name step))})))

(def table
  {:vocabulary vocabulary
   :curve-point {:args [:host :curve :t] :needs [[:host] [:curve] [:t]]
                 :run (fn [{:keys [host curve t]} _] (support/curve-point host curve t))}
   :surface-region {:args [:host :support :seed :point :radius :distance] :needs [[:host] [:support] [:seed :point] [:radius]]
                    :run (fn [{:keys [host support] :as args} _] (region/construct host support args))}
   :member {:args [:region :point] :needs [[:region] [:point]]
            :run (fn [{:keys [region point]} _]
                   {:status :resolved :inside (region/member region point)
                    :distance (region/distance region point) :radius (:radius region)})}
   :read-surface {:args [:host :support :revision :point :layers :order :filter]
                  :needs [[:host] [:support] [:point] [:layers] [:order]]
                  :snapshot read-snapshot :run read-surface}
   :surface/new {:args [:declaration] :needs [[:declaration]] :run (fn [args _] (surface/new (:declaration args)))}
   :mix {:args [:a :b :amount] :needs [[:a] [:b] [:amount]] :run (fn [{:keys [a b amount]} _] (surface/mix a b amount))}
   :paint {:args [:host :surface :region :rgba :opacity :blend :clips] :needs [[:host] [:surface] [:region] [:rgba]] :run paint}
   :sample {:args [:host :surface :point :filter] :needs [[:host] [:surface] [:point] [:filter]]
            :snapshot (fn [{:keys [host surface point filter]} _]
                        ;; Chart meaning is part of a read even if its surface bytes stay equal.
                        {:host host :surface surface :point point :filter filter})
            :run (fn [{:keys [host surface point filter]} _]
                   (surface/sample [surface] point filter {:domains (support/domains host)}))}})
