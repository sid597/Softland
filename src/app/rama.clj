(ns app.rama
  (:use [com.rpl.rama]
       [com.rpl.rama.path])
  (:require [com.rpl.specter :as s]
            [app.file :refer [save-event load-events]]
            [com.rpl.rama.test :as rtest :refer [create-ipc launch-module! gen-hashing-index-keys]]
            [com.rpl.rama.aggs :as aggs :refer [+merge +map-agg]]
            [com.rpl.rama.ops :as ops]
            [missionary.core :as m])
  (:import (clojure.lang Keyword)
           [hyperfiddle.electric Failure Pending]
           [com.rpl.rama.helpers ModuleUniqueIdPState]))

;; Each node-pstate is a map of graph-id and its nodes


(defrecord node-events [action-type node-data event-data])

(defmodule node-events-module [setup topologies]
  (declare-depot setup *node-events-depot :random)
  (let [n (stream-topology topologies "events-topology")]
    (declare-pstate n $$nodes-pstate {Keyword (map-schema
                                                Keyword (fixed-keys-schema
                                                           {:id                 Keyword
                                                            :x                  Double
                                                            :y                  Double
                                                            :type-specific-data (map-schema Keyword Object)
                                                            :type               String
                                                            :fill               String}))}
      #_{:global? true})
    (declare-pstate n $$event-id-pstate Long {:global? true
                                              :initial-value 0})

    (<<sources n
      (source> *node-events-depot :> {:keys [*action-type *node-data *event-data]})
      (local-select> (keypath :graph-name) *event-data :> *graph-name)
      (|hash *graph-name)
      (println "R: PROCESSING EVENT" (= :new-node *action-type))


      (<<cond
        ;; Add nodes
        (case> (= :new-node *action-type))
        (println "----------------------------------------------------")
        (println "R: ADDING NODE" *node-data)
        (local-transform>
          [*graph-name
           (keypath (ffirst *node-data))
           (termval (val (first *node-data)))]
          $$nodes-pstate)
        (println "R: NODE ADDED?" (local-select> ALL $$nodes-pstate))
        (println "----------------------------------------------------")

        ;; Delete nodes
        (case> (= :delete-node *action-type))
        (local-transform>
          [(keypath (first *node-data)) NONE>]
          $$nodes-pstate)

        ;; Update nodes
        (case> (= :update-node *action-type))
        (local-transform>
          [(first *node-data) (termval (second *node-data))]
          $$nodes-pstate)

        ;; update event id
        (case> (= :update-event-id *action-type))
        (println "R: UPDATING EVENT ID")
        (local-select> [] $$event-id-pstate :> *event-id)
        (println "R: EVENT ID" *event-id (inc *event-id))
        (local-transform> [(termval (inc *event-id))] $$event-id-pstate)


        (default>) (println "FALSE" *action-type)))))

;; ====== RAMA ======


(defonce !rama-ipc (atom nil))


(def ipc
  (let [c (create-ipc)]
    (println "--R--: Start ipc, launch module")
    (reset! !rama-ipc c)
    (launch-module! c node-events-module {:tasks 4 :threads 2})))

;; Define clj defs

(def event-depot (foreign-depot @!rama-ipc (get-module-name node-events-module) "*node-events-depot"))
(def nodes-pstate (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$nodes-pstate"))
(def event-id-pstate (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$event-id-pstate"))

(defn update-event-id []
  (foreign-append! event-depot (->node-events
                                 :update-event-id
                                 {}
                                 {})
    :append-ack))

(defn get-event-id []
  (first (foreign-select [] event-id-pstate)))


(defn proxy-callback [f]
  (fn [new-val diff old-val]
    (println "R: nodes-pstate callback" new-val diff old-val)
    (f new-val)))


(defn !subscribe [path pstate]
  (println "---R---: SUBSCRIBE")
  (->> (m/observe
         (fn [!]
           (println "SUBSCRIBE")
           ;; check https://clojurians.slack.com/archives/CL85MBPEF/p1698064128506939?thread_ts=1698062851.851949&cid=CL85MBPEF
           (! (Failure. (Pending.)))
           ;; using subselect because foreign-procxy takes exactly one path
           (let [proxy (foreign-proxy-async path pstate
                         {:callback-fn (proxy-callback !)})]
             #(.close @proxy))))
    ; discard stale values, DOM doesn't support backpressure
    (m/relieve {})))




(defn add-new-node
  ([node-map event-data]
   (add-new-node node-map event-data false false))
  ([node-map event-data save?]
   (add-new-node node-map event-data save? false))
  ([node-map event-data save? update?]
   (do
     (foreign-append! event-depot (->node-events
                                         :new-node
                                         node-map
                                         event-data)
            :append-ack)
    (when save?
      (save-event "add-new-node" [node-map event-data]))
    (when (or update?
            (some? (:event-id event-data)))
      (update-event-id)))))


(defn test []
  (println "test"))

(defn get-path-data [path pstate]
  (foreign-select path pstate {:pkey :rect}))

(load-events) ;; THIS IS A HACK: Will not work when we move away from ipc.


(comment
  (do
    (def ipc
      (let [c (create-ipc)]
        (println "--R--: Start ipc, launch module")
        (reset! !rama-ipc c)
        (launch-module! c node-events-module {:tasks 4 :threads 2})))

    ;; Define clj defs

    (def event-depot (r/foreign-depot @!rama-ipc (r/get-module-name node-events-module) "*node-events-depot"))
    (def nodes-pstate (r/foreign-pstate @!rama-ipc (r/get-module-name node-events-module) "$$nodes-pstate")))

  (r/close! @!rama-ipc)

  (foreign-select [] event-id-pstate)

  (foreign-append! event-depot (->node-events
                                 :update-event-id
                                 {}
                                 {}))


  (foreign-append! event-depot (->node-events
                                 :new-node
                                 {:rect11 {:id :rect11
                                           :x 50.99
                                           :y 60.0
                                           :type-specific-data {:text "GM Hello"
                                                                :width 400
                                                                :height 800}
                                           :type "rect"
                                           :fill  "lightblue"}}
                                 {:graph-name :main})
    :append-ack)

  (foreign-select [(keypath :main) ALL FIRST ] nodes-pstate)
  (foreign-select [(keypath :main) ] nodes-pstate)
  (foreign-select-one [(keypath :main) ALL FIRST ] nodes-pstate)

  (foreign-select  ALL  nodes-pstate {:pkey 4})
  (foreign-select  [(keypath :main) ALL]  nodes-pstate {:pkey :rect})
  (foreign-select [:main :6f818232-b41d-405f-9e2f-df2c66c9181f :x] nodes-pstate {:pkey :rect})
  (foreign-select-one [:main ] nodes-pstate {:pkey :rect})
  (foreign-select-one [:main FIRST ] nodes-pstate {:pkey :rect})
  ;(foreign-select-one [:main :cc61a1a9-663e-4d70-9347-bb9571588eb6 FIRST ] nodes-pstate {:pkey :rect})
  (foreign-select-one [:main ALL] nodes-pstate {:pkey :rect})


  (do
    (foreign-append! event-depot (->node-events
                                   :new-node
                                   {:rect3 {:id :rect3
                                            :x 500.032452345
                                            :y 600.989070
                                            :type-specific-data {:text "GM Hello"
                                                                 :width 400
                                                                 :height 800}
                                            :type "rect"
                                            :fill  "lightblue"}}
                                   {:graph-name :main})
      :append-ack)
    (foreign-append! event-depot (->node-events
                                   :new-node
                                   {:rect4 {:id :rect4
                                            :x 500.2
                                            :y 600.3
                                            :type-specific-data {:text "GM Hello"
                                                                 :width 400
                                                                 :height 800}
                                            :type "rect"
                                            :fill  "lightblue"}}
                                   {:graph-name :level-1})
      :append-ack)
    (foreign-append! event-depot (->node-events
                                   :new-node
                                   {:rect5 {:id :rect5
                                            :x 500.44
                                            :y 600.223
                                            :type-specific-data {:text "GM Hello"
                                                                 :width 400
                                                                 :height 800}
                                            :type "rect"
                                            :fill  "lightblue"}}
                                   {:graph-name :level-1})
      :append-ack))
  (foreign-select (keypath ["main" ALL]) nodes-pstate {:pkey :rect})


  (foreign-select ALL nodes-pstate {:pkey :rect}))