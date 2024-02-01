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
           [com.rpl.rama.helpers ModuleUniqueIdPState]))


(defrecord node-events [action-type node-data event-data])

(defmodule node-events-module [setup topologies]
  (declare-depot setup *node-events-depot :random)
  (let [n (stream-topology topologies "events-topology")]
    (declare-pstate n $$nodes-pstate { Keyword ;; node-id
                                      (fixed-keys-schema
                                        {:id                 Keyword
                                         :x                  Long
                                         :y                  Long
                                         :type-specific-data (map-schema Keyword Object)
                                         :type               String
                                         :fill               String})}
      #_{:global? true})

    (<<sources n
      (source> *node-events-depot :> {:keys [*action-type *node-data *event-data]})
      (println "action type" *action-type "node data" *node-data "event data" *event-data)
      (println "==========" (local-select> (first *node-data) $$nodes-pstate))

      (<<cond
        ;; Add nodes
        (case> (= :new-node *action-type))
        (local-transform>
          [(keypath (ffirst *node-data)) (termval (val (first *node-data)))]
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

(def ipc
  (let [c (create-ipc)]
    (println "--R--: Start ipc, launch module")
    (reset! !rama-ipc c)
    (launch-module! c node-events-module {:tasks 4 :threads 2})))

;; Define clj defs

(def event-depot (r/foreign-depot @!rama-ipc (r/get-module-name node-events-module) "*node-events-depot"))
(def nodes-pstate (r/foreign-pstate @!rama-ipc (r/get-module-name node-events-module) "$$nodes-pstate"))


(defn subscribe []
  (->> (m/observe
         (fn [!]
           (let [proxy (r/foreign-proxy ALL nodes-pstate
                         {:callback (fn [new-val diff old-val]
                                      (println "R: nodes-pstate callback" new-val diff old-val)
                                      (! new-val))
                          :pkey :rect})]
             #(.close @proxy))))
    ; discard stale values, DOM doesn't support backpressure
    (m/relieve {})))

(comment
  (r/close! @!rama-ipc)
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
                                 {})
    :append-ack))
