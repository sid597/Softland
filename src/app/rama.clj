(ns app.rama
  (:use [com.rpl.rama]
       [com.rpl.rama.path])
  (:require [com.rpl.rama :as r :refer [<<sources
                                        <<cond
                                        NONE>
                                        case>
                                        close!
                                        defmodule
                                        declare-depot
                                        foreign-append!
                                        foreign-depot
                                        foreign-pstate
                                        foreign-proxy
                                        fixed-keys-schema
                                        foreign-select-one
                                        get-module-name
                                        hash-by
                                        local-select>
                                        local-transform>
                                        <<query-topology
                                        source>
                                        stream-topology]]
            [com.rpl.specter :as s]
            [com.rpl.rama.test :as rtest :refer [create-ipc launch-module!]]
            [com.rpl.rama.aggs :as aggs :refer [+merge +map-agg]]
            [com.rpl.rama.test :as rtest]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.path :as path :refer [termval
                                                ALL
                                                MAP-KEYS
                                                MAP-VALS
                                                multi-path
                                                keypath]]
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
                                                            :x                  Long
                                                            :y                  Long
                                                            :type-specific-data (map-schema Keyword Object)
                                                            :type               String
                                                            :fill               String}))}
      #_{:global? true})

    (<<sources n
      (source> *node-events-depot :> {:keys [*action-type *node-data *event-data]})
      (local-select> (keypath :graph-name) *event-data :> *graph-name)
      (println "graph name" *graph-name "action type" *action-type "node data" *node-data "event data" (local-select> (keypath :graph-name) *event-data))


      (<<cond
        ;; Add nodes
        (case> (= :new-node *action-type))
        (local-transform>
          [*graph-name
           (keypath (ffirst *node-data))
           (termval (val (first *node-data)))]
          $$nodes-pstate)

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
        ;;
        (default>) (println "FALSE" *action-type)))))

;; ====== RAMA ======


(defonce !rama-ipc (atom nil))
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


  (foreign-append! event-depot (->node-events
                                 :new-node
                                 {:rect {:id :rect
                                         :x 500
                                         :y 600
                                         :type-specific-data {:text "GM Hello"
                                                              :width 400
                                                              :height 800}
                                         :type "rect"
                                         :fill  "lightblue"}}
                                 {:graph-name :main})
    :append-ack)

  (foreign-select [ALL FIRST] nodes-pstate {:pkey :rect})
  (foreign-select [:main :react] nodes-pstate {:pkey :rect})


  (do
    (foreign-append! event-depot (->node-events
                                   :new-node
                                   {:rect3 {:id :rect3
                                            :x 500
                                            :y 600
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
                                            :x 500
                                            :y 600
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
                                            :x 500
                                            :y 600
                                            :type-specific-data {:text "GM Hello"
                                                                 :width 400
                                                                 :height 800}
                                            :type "rect"
                                            :fill  "lightblue"}}
                                   {:graph-name :level-1})
      :append-ack))
  (foreign-select (keypath ["main" ALL]) nodes-pstate {:pkey :rect})


  (foreign-select ALL nodes-pstate {:pkey :rect})


  (defn qry-res [] (foreign-select ALL nodes-pstate {:pkey :rect}))

  (qry-res))
#_(def ipc
    (let [c (create-ipc)]
      (println "--R--: Start ipc, launch module")
      (reset! !rama-ipc c)
      (launch-module! c node-events-module {:tasks 4 :threads 2})))

;; Define clj defs

#_(def event-depot (r/foreign-depot @!rama-ipc (r/get-module-name node-events-module) "*node-events-depot"))
#_(def nodes-pstate (r/foreign-pstate @!rama-ipc (r/get-module-name node-events-module) "$$nodes-pstate"))


#_(defn proxy-callback [f]
    (fn [new-val diff old-val]
      (println "R: nodes-pstate callback" new-val diff old-val)
      (f new-val)))


#_(defn subscribe []
    (println "---R---: SUBSCRIBE")
    (->> (m/observe
           (fn [!]
             (println "SUBSCRIBE")
             ;; check https://clojurians.slack.com/archives/CL85MBPEF/p1698064128506939?thread_ts=1698062851.851949&cid=CL85MBPEF
             (! (Failure. (Pending.)))
             ;; using subselect because foreign-procxy takes exactly one path
             (let [proxy (r/foreign-proxy-async (subselect ALL) nodes-pstate
                           {:callback-fn (proxy-callback !)
                            :pkey :rect})]
               #(.close @proxy))))
      ; discard stale values, DOM doesn't support backpressure
      (m/relieve {})))


