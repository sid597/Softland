(ns app.client.workspace.live-edges
  "Dark-by-default, read-only R1 join from durable reference rows to connector
   rt-nodes. Namespace load is pure; the client issues a query and mounts a
   join slot only when `?live-atoms=1` is present."
  (:require [app.client.substrate.connector-route :as connector-route]
            #?(:clj [app.server.rama.relation-kernel :as relation-kernel])
            #?(:cljs [app.client.workspace.rect-tree :as rt])
            #?(:cljs [app.client.workspace.scene-runtime :as scene-runtime])
            #?(:cljs [app.client.workspace.scene-store :as scene-store])))

(def day-one-kinds [:references])
(def join-vi :live-edges/durable-join)
(def join-span {:x 0.0 :y 0.0 :w 1000000.0 :h 1000000.0})

#?(:clj
   (defn- wire-row [row]
     (-> (select-keys row
                      [:relation-id :relation-kind :asserter-actor-id
                       :asserter-type :relation-status :first-asserted-at-ms
                       :status-changed-at-ms :event-id :request-id])
         (assoc :from (into {} (:from row))
                :to (into {} (:to row))))))

#?(:clj
   (defn read-live-edges
     "One batch product read over R1. Keeps only asserted day-one relations
      whose endpoints are both in the supplied on-canvas address set, and
      returns plain wire maps deduped by durable relation identity."
     [rk-rt addresses]
     (let [addresses (vec (distinct (remove nil? addresses)))
           address-set (set addresses)
           rows-by-target
           (if (and rk-rt (seq addresses))
             (relation-kernel/read-relations-for-targets
              rk-rt addresses day-one-kinds false)
             {})
           rows (->> (vals rows-by-target)
                     (apply concat)
                     (filter #(and (contains? address-set
                                              (get-in % [:from :target-id]))
                                   (contains? address-set
                                              (get-in % [:to :target-id]))))
                     (reduce (fn [by-id row]
                               (assoc by-id (:relation-id row) row))
                             {})
                     vals
                     (sort-by :relation-id)
                     (mapv wire-row))]
       {:rows rows
        :edges-read (count rows)
        :source (if rk-rt :relation-kernel/r1 :relation-kernel/unavailable)
        :zero-writes true})))

#?(:cljs
   (do
     (defonce ^:private !boot-addresses (atom nil))
     (defonce ^:private !mounted-token (atom ::never))

     (defn live-edges-enabled? []
       (= "1" (.get (js/URLSearchParams. (or (.-search js/location) ""))
                     "live-atoms")))

     (defn- product-slot? [slot]
       (let [meta (:meta slot)]
         (and (not (:live-atoms? meta))
              (not (:live-edges? meta)))))

     (defn on-canvas-addresses
       "Addresses from real product slots only. Fixture and prior join slots
        never become R1 target keys."
       [store]
       (->> (:slots store)
            vals
            (filter product-slot?)
            (mapcat (comp keys :addresses))
            distinct
            (sort-by pr-str)
            vec))

     (defn capture-boot-addresses!
       "Freeze the first non-empty real on-canvas address set for the day-one
        boot-static durable edge read."
       [store]
       (when (and (live-edges-enabled?) (nil? @!boot-addresses))
         (let [addresses (on-canvas-addresses store)]
           (when (seq addresses)
             (reset! !boot-addresses addresses))))
       @!boot-addresses)

     (defn- connector-node [instance]
       (let [edge-instance-id (:connector/edge-instance-id instance)]
         (rt/rt-node
          [:live-edges/connector edge-instance-id] :connector join-span
          :data {:address edge-instance-id
                 :connector/edge-instance-id edge-instance-id
                 :connector/from-vi (:connector/from-vi instance)
                 :connector/to-vi (:connector/to-vi instance)
                 :connector/material (:connector/material instance)})))

     (defn mount-live-edges!
       "Project wire rows, expand visible occurrences, and register one
        identity-transform join slot. Idempotent for the boot-static edge set."
       [{:keys [rows edges-read source zero-writes] :as response}]
       (when (and (live-edges-enabled?) response)
         (let [token (mapv (juxt :relation-id :event-id) rows)]
           (when (not= token @!mounted-token)
             (let [store-frame
                   (scene-store/derive-store-frame
                    (scene-runtime/store-snapshot))
                   materials (mapv connector-route/project-edge-row rows)
                   seed-ops (mapv (fn [material]
                                    {:container :live-edges/pending
                                     :container-idx 0
                                     :owner-vi join-vi
                                     :connector/material material})
                                  materials)
                   instances
                   (connector-route/expand-edge-instances
                    seed-ops (:targets-by-address store-frame))
                   rendered (count (filter #(and (:connector/from-target %)
                                                 (:connector/to-target %))
                                           instances))
                   unresolved (- (count instances) rendered)
                   tree (rt/rt-node
                         :live-edges/root :group join-span
                         :children (mapv connector-node instances))]
               (scene-runtime/close-instance! join-vi)
               (when (seq instances)
                 (scene-runtime/register-face-instance!
                  join-vi tree
                  {:x 0.0 :y 0.0 :scale 1.0 :layer 10 :sibling-rank 10
                   :meta {:live-edges? true
                          :material/id :live-edges/durable-r1
                          :material/revision token}}))
               (reset! !mounted-token token)
               (aset js/globalThis "__softlandLiveEdgesReceipt"
                     (clj->js {:edges-read (or edges-read (count rows))
                               :rendered rendered
                               :unresolved unresolved
                               :source source
                               :zero-writes (true? zero-writes)}))))))
       nil)))
