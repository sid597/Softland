(ns app.server.rama.core
  (:use [com.rpl.rama]
       [com.rpl.rama.path])
  (:require [app.server.env :refer [oai-key roam-api-key roam-graph-name]]
            [app.server.rama.objects :refer [http-post-future query-roam-req task-global-client roam-client]])
  (:import (clojure.lang Keyword)
           [app.server.rama.objects CljHttpTaskGlobal roam-task-global]
           [com.rpl.rama.helpers ModuleUniqueIdPState]))





(defmodule node-events-module [setup topologies]
  (declare-depot setup *node-events-depot :random)
  (declare-depot setup *user-registration-depot (hash-by :username))
  (declare-depot setup *user-graph-settings-depot (hash-by :user-id))
  (declare-object setup *http-client (CljHttpTaskGlobal.))
  (declare-object setup *roam-client (roam-task-global.
                                       roam-api-key
                                       roam-graph-name))
  (let [n      (stream-topology topologies "events-topology")
        id-gen (ModuleUniqueIdPState. "$$id")]
    (declare-pstate n $$nodes-pstate {Keyword (map-schema
                                                Keyword (fixed-keys-schema
                                                           {:id                 Keyword
                                                            :x                  (fixed-keys-schema
                                                                                   {:pos Double
                                                                                    :time Long})
                                                            :y                  (fixed-keys-schema
                                                                                   {:pos Double
                                                                                    :time Long})
                                                            :type-specific-data (map-schema Keyword Object)
                                                            :type               String
                                                            :fill               String}))}
      #_{:global? true})
    (declare-pstate n $$dg-node-ids-pstate {Keyword (vector-schema String)})
    (declare-pstate n $$dg-pages-pstate {Keyword (map-schema String Object {:subindex? true})})
    (declare-pstate n $$dg-nodes-pstate {Keyword (map-schema String Object {:subindex? true})})
    (declare-pstate n $$dg-edges-pstate {Keyword (vector-schema Object {:subindex? true})})
    (declare-pstate n $$components-pstate {Keyword (map-schema Keyword Object)})
    (declare-pstate n $$node-ids-pstate {Keyword (vector-schema Keyword)})
    (declare-pstate n $$event-id-pstate Long {:global? true
                                              :initial-value 0})
    (declare-pstate n $$user-registration-pstate {String ; username
                                                  (fixed-keys-schema {:user-id Long
                                                                      :uuid String})})
    (declare-pstate n $$user-graph-settings-pstate {Long ;user-id
                                                    (map-schema
                                                      Keyword
                                                       (fixed-keys-schema {:ui-mode Keyword
                                                                           :viewbox (vector-schema Long)}))})
    (.declarePState id-gen n)

    (<<sources n
      ;; Source from user-graph-settings-depot
      (source> *user-graph-settings-depot :> {:keys [*user-id *graph-name *settings-data *event-data]})
      (|hash *user-id)
      (local-transform> [(keypath *user-id)
                         *graph-name
                         (first *settings-data) (termval (second *settings-data))]
        $$user-graph-settings-pstate)


      ;; Source from user-registration-depot
      (source> *user-registration-depot :> {:keys [*username *uuid]})
      (local-select> (keypath *username) $$user-registration-pstate :> {*curr-uuid :uuid :as *curr-info})
      (<<if (or> (nil? *curr-info)
              (= *curr-uuid *uuid))
        (java-macro! (.genId id-gen "*user-id"))
        (println "R: -- user id--" *user-id)
        (local-transform> [(keypath *username)
                           (multi-path [:user-id (termval *user-id)]
                             [:uuid (termval *uuid)])]
          $$user-registration-pstate)
        (|hash *user-id)
        ;; by default new user gets access to :main graph
        (local-transform> [(keypath *user-id)
                           :main
                           (multi-path [:ui-mode (termval :dark)]
                                       [:viewbox (termval [0 0 2000 2000])])]
          $$user-graph-settings-pstate))


      ;; Source from node-events-depot
      (source> *node-events-depot :> {:keys [*action-type *node-data *event-data]})
      (local-select> (keypath :graph-name) *event-data :> *graph-name)
      (local-select> (keypath :uid) *node-data :> *uid)
      (|hash *graph-name)
      (println "R: PROCESSING EVENT" *action-type)

      (<<cond
        ;; llm request
        (case> (= :llm-request *action-type))
        ;; request data attached to event data
        (local-select> (keypath :request-data) *event-data :> *request-data)
        (println "R: GOT LLM REQUEST: " *request-data)
        ;; Send the data to post function
        ;; which takes the data and posts it open ai
        ;; givens response all at once
        ;; have to figure out how to do streaming
        (completable-future>
          (http-post-future (task-global-client *http-client) (first *node-data) *event-data)
          :> *response-body)
        ;; find whats the current value at the given data path
        #_(println "R: RESPONSE-->" *response-body)
        #_(first *node-data :> *data-path)
        #_(local-select> [*graph-name *data-path] $$nodes-pstate :> *cur-val)
        #_(println "R: CURRENT VALUE LLM WILL REPLACE:  " *cur-val)
        ;; Update the response at the given path
        #_(local-transform>
            [*graph-name
             *data-path (termval *response-body)]
            $$nodes-pstate)
        #_(println "R: UPDATED VALUE" (local-select> *data-path $$nodes-pstate))


        ;; Query roam
        (case> (= :roam-query *action-type))
        (completable-future>
          (query-roam-req
            (roam-client *roam-client)
            "[:find (pull ?e [*])
              :in $ ?uid
              :where [?e :node/title ?uid]]"
            "Testing")
          :> *query-result)
        (println "R: ** QUERY RESULT **" *query-result)


        ;; Add roam node
        (case> (= :add-dg-page-data *action-type))
        (local-transform>
          [*graph-name
           *uid
           (termval *node-data)]
          $$dg-pages-pstate)

        (case> (= :add-dg-nodes *action-type))
        (println "NODE DATA: " *uid)
        (local-transform>
          [*graph-name
           *uid
           (termval *node-data)]
          $$dg-nodes-pstate)

        (local-transform>
          [*graph-name
           AFTER-ELEM
           (termval *uid)]
          $$dg-node-ids-pstate)

        (case> (= :add-dg-edges *action-type))
        (local-transform>
          [*graph-name AFTER-ELEM (termval *node-data)]
          $$dg-edges-pstate)



        ;; Add nodes
        (case> (= :new-node *action-type))
        (println "R: ADDING NODE" *node-data)
        (local-transform>
          [*graph-name
           (keypath (ffirst *node-data))
           (termval (val (first *node-data)))]
          $$nodes-pstate)
        (local-transform>
          [(keypath *graph-name)
           AFTER-ELEM
           (termval (ffirst *node-data))]
          $$node-ids-pstate)

        ;; Delete nodes
        #_#_#_(case> (= :delete-node *action-type))
        (local-transform>
          [*graph-name (keypath (first *node-data)) NONE>]
          $$nodes-pstate)
        (local-transform>
          [*graph-name
           [ALL (= % (first *node-data))] NONE>]
          $$node-ids-pstate)

        ;; Update nodes
        (case> (= :update-node *action-type))
        (println "----------------------------------------------------")
        (println "R: UPDATING NODE" *node-data)
        (local-transform>
          [*graph-name (first *node-data) (termval (second *node-data))]
          $$nodes-pstate)
        (println "R: NODE UPDATED")
        ;(clojure.pprint/pprint (local-select> ALL $$nodes-pstate))
        (println "----------------------------------------------------")

        ;; update event id
        (case> (= :update-event-id *action-type))
        (local-select> [] $$event-id-pstate :> *event-id)
        (local-transform> [(termval (inc *event-id))] $$event-id-pstate)


        (default>) (println "FALSE" *action-type)))))

