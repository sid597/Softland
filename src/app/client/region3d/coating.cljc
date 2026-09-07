(ns app.client.region3d.coating
  "Retained marks read through revisioned chains and root restrictions.
   Takes a sphere value, revision, point, order and per-read work grant.
   Gives a compositor read with known partial color or missing dependencies.
   Holds nothing; no binding work is cached. Evidence: coating_test.clj."
  (:require [app.client.region3d.support :as support]
            [app.client.engine.surface :as surface]))

(defn snapshot [host revision order]
  {:support host :revision revision :order order})

(defn- invert-chain [binding chart uv]
  (loop [relations (reverse (:chain binding)) here (:id chart) uv uv]
    (if-let [relation (first relations)]
      (when (= here (:to relation))
        (recur (next relations) (:from relation) (support/inverse (:map relation) uv)))
      (when (= here (:root binding)) uv))))

(defn- inside-domain? [[u0 v0 u1 v1] [u v]] (and (<= u0 u u1) (<= v0 v v1)))

(defn- mark-distance [host record point]
  (case (:kind record)
    :arc (support/arc-distance host record point)
    :tap {:distance (support/distance host point (support/curve-point host (:on-curve record) (:at record)))}
    (support/refuse! :mark-kind (:kind record))))

(defn locate
  "Read at one support revision. Invert every declared periodic branch,
   restrict in the retained root, then classify the mark intrinsically.
   A missing chain or withheld work remains pending even at an unseen point."
  [host revision point {:keys [budget order]}]
  (support/validate! host)
  (support/point! point)
  (let [chart (support/revision-chart host revision)
        [a b] (support/point->chart host (:id chart) point)
        bindings (or (get (:bindings host) revision) (support/refuse! :bindings revision))
        rows
        (mapv (fn [binding]
                (let [record (get host (:record binding))
                      work (reduce + (map :work (:chain binding)))
                      missing (fn [why] {:status :pending :binding (:id binding) :missing why :work work})]
                  (cond
                    (and budget (> work budget))
                    (missing (str (:id (last (:chain binding))) ": " (:id record) " → M@" revision " at the demanded chart point"))
                    (nil? record) (missing "retained record missing")
                    :else
                    (let [preimages (mapv (fn [k] {:k k :root (invert-chain binding chart [(+ a (* k (:period chart))) b])}) [-1 0 1])]
                      (if (some #(nil? (:root %)) preimages)
                        (missing "retained dependency removed without a composed replacement")
                        (let [hits (filter #(inside-domain? (:domain record) (:root %)) preimages)
                              hit (first hits)]
                          (if-not hit
                            {:status :outside :binding (:id binding) :record (:id record)}
                            (let [d (mark-distance host record point)]
                              (merge {:status :resolved :binding (:id binding) :record (:id record)
                                      :mark (:mark record) :rgba (:rgba record) :inside (<= (:distance d) (:radius record))
                                      :chain (:chain binding) :preimages (vec hits)} d))))))))) bindings)
        known (filter #(and (= :resolved (:status %)) (:inside %)) rows)
        names (mapv :mark known)
        missing (vec (filter #(= :pending (:status %)) rows))
        composition-order (if (and (nil? order) (<= (count names) 1)) names order)
        valid-order? (and (sequential? composition-order)
                          (= (count composition-order) (count (distinct composition-order)))
                          (every? (set composition-order) names))
        policy? (not valid-order?)
        rgba (reduce (fn [acc name]
                       (if-let [r (some #(when (= name (:mark %)) %) known)] (surface/over (:rgba r) acc) acc))
                     [0.0 0.0 0.0 0.0] composition-order)
        result (cond
                 policy? {:status :needs-policy :candidates names}
                 (seq missing) {:status :pending :missing missing :known names :partial rgba}
                 :else {:status :resolved :color rgba :contributors names :covered? (boolean (seq names))})]
    (assoc result :snapshot (snapshot host revision order) :bindings rows)))
