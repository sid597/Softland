(ns app.server.rama.core
  (:use [com.rpl.rama]
       [com.rpl.rama.path]
       [com.rpl.rama.ops]
       [com.rpl.rama.aggs])
  (:require [app.server.env :refer [oai-key roam-api-key roam-graph-name]]
            [app.server.rama.objects :refer [http-post-future query-roam-req task-global-client roam-client
                                             cli-exec-future cli-client]])
  (:import (clojure.lang Keyword)
           [app.server.rama.objects CljHttpTaskGlobal roam-task-global CliProcessTaskGlobal]
           [com.rpl.rama.helpers ModuleUniqueIdPState]))





(defn toggle-set
  "Toggle membership of `item` in set `s`. Rama DSL cannot inline
   let/if, so this must be a plain function called from the topology."
  [s item]
  (let [s (or s #{})]
    (if (contains? s item)
      (disj s item)
      (conj s item))))

(defn now-ms
  "Wrapper for System/currentTimeMillis — Rama's dataflow DSL cannot
   inline Java static method calls, so we wrap it in a plain function."
  ^long []
  (System/currentTimeMillis))

(defmodule node-events-module [setup topologies]
  (declare-depot setup *node-events-depot :random)
  (declare-depot setup *user-registration-depot (hash-by :username))
  (declare-depot setup *user-graph-settings-depot (hash-by :user-id))
  (declare-object setup *http-client (CljHttpTaskGlobal.))
  (declare-object setup *cli-process (CliProcessTaskGlobal.))
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
    (declare-pstate n $$dg-edges-pstate {Keyword (map-schema Keyword (vector-schema Object) #_{:subindex? true})})
    (declare-pstate n $$components-pstate {Keyword (map-schema Keyword Object)})
    (declare-pstate n $$node-ids-pstate {Keyword (vector-schema Keyword)})
    (declare-pstate n $$node-ids-inview-pstate {Keyword (vector-schema Keyword)})
    (declare-pstate n $$event-id-pstate Long {:global? true
                                              :initial-value 0})
    (declare-pstate n $$agent-runs-pstate {String (map-schema Keyword Object)})
    (declare-pstate n $$cli-sessions-pstate
      {String
       {Keyword
        (fixed-keys-schema
          {:session-id String
           :last-active Long})}})

    ;; Sidebar committed truth — project, expanded dirs, selected file
    ;; Global PState: single workspace, keyed by field name
    (declare-pstate n $$sidebar-pstate {Keyword (map-schema Keyword Object)})

    ;; User settings — font-size, line-height, theme-id, etc.
    ;; Keyed by namespace (e.g. :settings), values are setting keyword → value
    (declare-pstate n $$settings-pstate {Keyword (map-schema Keyword Object)})

    ;; Agent trails — completed run trail data, keyed by run-id
    ;; Each run: {:status :trail :provider :prompt :started-at :completed-at}
    (declare-pstate n $$agent-trails-pstate {String (map-schema Keyword Object)})

    ;; Flow session — DG workflow FSM state, keyed by namespace
    ;; Persists :node, :tickets, :batch, :session-id, :history across reload
    (declare-pstate n $$flow-session-pstate {Keyword (map-schema Keyword Object)})

    ;; Editor state — document content keyed by file path
    ;; Phase 4B: measuring direct committed editing through Rama
    (declare-pstate n $$editor-state-pstate {String (map-schema Keyword Object)})
    (declare-pstate n $$user-registration-pstate {String ; username
                                                  (fixed-keys-schema {:user-id Long
                                                                      :uuid String})})
    (declare-pstate n $$user-graph-settings-pstate {Long ;user-id
                                                    (map-schema
                                                      Keyword
                                                       (fixed-keys-schema {:ui-mode Keyword
                                                                           :viewbox (vector-schema Long)}))})
    (.declarePState id-gen n)

    (<<query-topology topologies "get-in-view-nodeids"
      [*cx *cy *ch *cw *graph-name *path :> *in-view-ids]
      ;(println " QUERY topology start")
      (|hash *graph-name)
      (local-select> *path $$nodes-pstate :> *all-nodes)
      ;(println "selected all nodes" *all-nodes)
      (explode-map *all-nodes :> *nuid *ndata)
      ;(println "NODE: " *ndata)
      (local-select> [:x :pos] *ndata :> *nx)
      (local-select> [:y :pos] *ndata :> *ny)
      (identity (and> (>= *nx *cx)
                  (< *nx (+ *cx *ch))
                  (>= *ny *cy)
                  (< *ny (+ *cy *ch))) :> *t?)

      ;(println "local select" *nuid *nx *ny *t? *cx *cy *ch *cw)
      (<<cond
        (case> *t?)
        ;(println "TRUE")
        (identity *nuid :> *x-uid))

      (|origin)
      (+vec-agg *x-uid :> *in-view-ids))




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
        ;; ========llm request========
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


        ;; ========Query roam========
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


        ;; ======== Add roam node ========
        (case> (= :add-dg-page-data *action-type))
        (local-transform>
          [*graph-name
           *uid
           (termval *node-data)]
          $$dg-pages-pstate)


        ;; ======== add dg nodes ========
        (case> (= :add-dg-nodes *action-type))
        (assoc *node-data
          :width 80
          :height 4
          :> *updated-node)
        (identity (keyword *uid) :> *kuid)
        (identity {:id *kuid
                   :x {:pos (+ 0.0009 (rand-int 400))
                       :time 0}
                   :y {:pos (+ 0.00009 (rand-int 400))
                       :time 0}
                   :type-specific-data *updated-node
                   :type "rect"
                   :fill "lightblue"}
          :> *d)
        (println "NODE DATA: " *d #_{:uid *uid})

        (local-transform>
          [*graph-name
           *uid
           (termval *node-data)]
          $$dg-nodes-pstate)
        (local-transform>
          [*graph-name
           (keypath *kuid)
           (termval *d)]
          $$nodes-pstate)
        (local-transform>
          [(keypath *graph-name)
           AFTER-ELEM
           (termval *kuid)]
          $$node-ids-pstate)
        (local-transform>
          [*graph-name
           AFTER-ELEM
           (termval *uid)]
          $$dg-node-ids-pstate)

        ;; ========Add dg edges========
        (case> (= :add-dg-edges *action-type))
        (local-select> [(keypath :edges) FIRST] *node-data :> *edges)
        (local-select> [FIRST  (keypath :uid)] *edges :> *suid)
        (identity (keyword *suid) :> *ksuid)
        (println "Ksuid ::" *ksuid)
        (identity {:to (last *edges)
                   :relation (second *edges)}
          :> *edge-map)
        (println "EDGE::::" *edges)
        (local-transform>
          [*graph-name
           *ksuid
           AFTER-ELEM
           (termval *edge-map)]
          $$dg-edges-pstate)

        #_(local-transform>
            [*graph-name AFTER-ELEM (termval *edges)]
            $$dg-edges-pstate)



        ;; ========Add nodes========
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

        ;; ========Delete nodes========
        #_#_#_(case> (= :delete-node *action-type))
        (local-transform>
          [*graph-name (keypath (first *node-data)) NONE>]
          $$nodes-pstate)
        (local-transform>
          [*graph-name
           [ALL (= % (first *node-data))] NONE>]
          $$node-ids-pstate)

        ;; ========Update nodes========
        (case> (= :update-node *action-type))
        (println "----------------------------------------------------")
        (println "R: UPDATING NODE" *node-data)
        (local-transform>
          [*graph-name (first *node-data) (termval (second *node-data))]
          $$nodes-pstate)
        (println "R: NODE UPDATED")
        ;(clojure.pprint/pprint (local-select> ALL $$nodes-pstate))
        (println "----------------------------------------------------")

        ;; ========update event id========
        (case> (= :update-event-id *action-type))
        (local-select> [] $$event-id-pstate :> *event-id)
        (local-transform> [(termval (inc *event-id))] $$event-id-pstate)

        ;; ========agent run========
        (case> (= :agent-run *action-type))
        (local-select> (keypath :request-data) *event-data :> *request-data)
        (local-select> (keypath :run-id) *event-data :> *run-id)
        (local-select> (keypath :provider) *request-data :> *provider)
        (local-select> (keypath :prompt) *request-data :> *prompt)
        (now-ms :> *start-ms)
        (local-transform> [(keypath *run-id) :status (termval :running)] $$agent-runs-pstate)
        (local-transform> [(keypath *run-id) :provider (termval *provider)] $$agent-runs-pstate)
        (local-transform> [(keypath *run-id) :prompt (termval *prompt)] $$agent-runs-pstate)
        (local-transform> [(keypath *run-id) :request-data (termval *request-data)] $$agent-runs-pstate)
        (local-transform> [(keypath *run-id) :started-at (termval *start-ms)] $$agent-runs-pstate)
        (local-transform> [(keypath *run-id) :updated-at (termval *start-ms)] $$agent-runs-pstate)

        ;; ========update cli session (stores session-id for --resume)========
        (case> (= :update-cli-session *action-type))
        (local-select> (keypath :file-path) *event-data :> *file-path)
        (local-select> (keypath :provider) *event-data :> *provider)
        (local-select> (keypath :session-id) *event-data :> *session-id)
        (now-ms :> *now)
        (local-transform>
          [(keypath *file-path) (keypath *provider)
           (multi-path
             [:session-id (termval *session-id)]
             [:last-active (termval *now)])]
          $$cli-sessions-pstate)

        ;; ========sidebar: toggle dir expand/collapse========
        (case> (= :sidebar/dir-toggle *action-type))
        (local-select> (keypath :path) *node-data :> *dir-path)
        (local-select> [*graph-name (keypath :expanded-dirs)] $$sidebar-pstate :> *dirs)
        (identity (toggle-set *dirs *dir-path) :> *new-dirs)
        (local-transform> [*graph-name (keypath :expanded-dirs) (termval *new-dirs)] $$sidebar-pstate)
        (println "R: SIDEBAR dir-toggle" *dir-path)

        ;; ========sidebar: select file========
        (case> (= :sidebar/file-select *action-type))
        (local-select> (keypath :path) *node-data :> *file-path)
        (local-select> (keypath :name) *node-data :> *file-name)
        (local-transform> [*graph-name (keypath :selected-file)
                           (termval {:path *file-path :name *file-name})]
          $$sidebar-pstate)
        (println "R: SIDEBAR file-select" *file-path)

        ;; ========sidebar: select project (home dir)========
        (case> (= :sidebar/project-select *action-type))
        (local-select> (keypath :name) *node-data :> *proj-name)
        (local-select> (keypath :path) *node-data :> *proj-path)
        (local-transform> [*graph-name (keypath :project)
                           (termval {:name *proj-name :path *proj-path})]
          $$sidebar-pstate)
        (local-transform> [*graph-name (keypath :expanded-dirs) (termval #{})] $$sidebar-pstate)
        (local-transform> [*graph-name (keypath :selected-file) (termval nil)] $$sidebar-pstate)
        (println "R: SIDEBAR project-select" *proj-name)

        ;; ========sidebar: go back to home dirs========
        (case> (= :sidebar/project-back *action-type))
        (local-transform> [*graph-name (keypath :project) (termval nil)] $$sidebar-pstate)
        (local-transform> [*graph-name (keypath :expanded-dirs) (termval #{})] $$sidebar-pstate)
        (local-transform> [*graph-name (keypath :selected-file) (termval nil)] $$sidebar-pstate)
        (println "R: SIDEBAR project-back")

        ;; ========settings: update (merge partial settings map)========
        (case> (= :settings/update *action-type))
        (explode-map *node-data :> *setting-key *setting-val)
        (local-transform> [*graph-name (keypath *setting-key) (termval *setting-val)] $$settings-pstate)
        (println "R: SETTINGS update" *setting-key *setting-val)

        ;; ========editor: save document state========
        (case> (= :editor/save-doc *action-type))
        (local-select> (keypath :file-path) *node-data :> *file-path)
        (local-select> (keypath :doc-state) *node-data :> *doc-state)
        (explode-map *doc-state :> *dkey *dval)
        (local-transform> [(keypath *file-path) (keypath *dkey) (termval *dval)] $$editor-state-pstate)

        ;; ========flow session: save FSM state========
        (case> (= :flow/save-state *action-type))
        (explode-map *node-data :> *fkey *fval)
        (local-transform> [*graph-name (keypath *fkey) (termval *fval)] $$flow-session-pstate)
        (println "R: FLOW save-state")

        ;; ========agent trail: save completed run========
        (case> (= :agent-trail/save-run *action-type))
        (local-select> (keypath :run-id) *node-data :> *trail-run-id)
        (local-select> (keypath :trail-data) *node-data :> *trail-data)
        (explode-map *trail-data :> *tkey *tval)
        (local-transform> [(keypath *trail-run-id) (keypath *tkey) (termval *tval)] $$agent-trails-pstate)
        (println "R: AGENT-TRAIL save-run" *trail-run-id)

        (default>) (println "FALSE" *action-type)))))
