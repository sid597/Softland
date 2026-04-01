(ns app.server.rama.core
  (:use [com.rpl.rama]
       [com.rpl.rama.path]
       [com.rpl.rama.ops])
  (:import (clojure.lang Keyword)
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
  (let [n      (stream-topology topologies "events-topology")
        id-gen (ModuleUniqueIdPState. "$$id")]
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
    (declare-pstate n $$settings-pstate {Keyword (map-schema Keyword Object)})

    ;; Agent trails — completed run trail data, keyed by run-id
    (declare-pstate n $$agent-trails-pstate {String (map-schema Keyword Object)})

    ;; Flow session — DG workflow FSM state, keyed by namespace
    (declare-pstate n $$flow-session-pstate {Keyword (map-schema Keyword Object)})

    ;; Editor state — document content keyed by file path
    (declare-pstate n $$editor-state-pstate {String (map-schema Keyword Object)})

    ;; Workspace truth — selected artifact, active pane, sidebar visible
    (declare-pstate n $$workspace-truth-pstate {Keyword (map-schema Keyword Object)})

    (declare-pstate n $$user-registration-pstate {String
                                                  (fixed-keys-schema {:user-id Long
                                                                      :uuid String})})
    (declare-pstate n $$user-graph-settings-pstate {Long
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
      (|hash *graph-name)
      (println "R: PROCESSING EVENT" *action-type)

      (<<cond
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

        ;; ========workspace: save truth========
        (case> (= :workspace/save-truth *action-type))
        (explode-map *node-data :> *wkey *wval)
        (local-transform> [*graph-name (keypath *wkey) (termval *wval)] $$workspace-truth-pstate)
        (println "R: WORKSPACE save-truth")

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
