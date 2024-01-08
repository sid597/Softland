(ns app.rama-module
  (:use [com.rpl.rama]
       [com.rpl.rama.path])
  (:require [com.rpl.rama :as r :refer [<<sources
                                        defmodule
                                        declare-depot
                                        defpath
                                        fixed-keys-schema
                                        hash-by
                                        stream-topology]]
            [com.rpl.rama.aggs :as aggs]
            [com.rpl.rama.ops :as ops])
  (:import [com.rpl.rama.helpers ModuleUniqueIdPState]))


;; In Clojure, defrecord is a macro used to define a new record type that implements
;; a set of interfaces and can have fields. Records in Clojure are a way to create
;; data structures that are more efficient and structured than general maps, while
;; still behaving like maps.
(defrecord app-state [viewbox username])

;; Module
;; This defines the module, whose body is a regular Clojure function implementation. All depots, ETLs,
;; PStates, and query topologies are defined via this entry point.
;; Any write you could do to a database you can easily model in Rama as a depot append being consumed by
;; an ETL topology to update a PState.

;; If you want to change how an ETL topology updates PStates or add new functionality, you simply
;; (update the module) [https://redplanetlabs.com/docs/~/operating-rama.html#_updating_modules].

;; Additionally, the integration of PStates, depots, and topologies onto the same set of processes/threads
;; means there’s very little cost to the additional step of new data going to a depot first.

;; Finally, another major difference between PStates and databases is how reactivity is provided.
;; Reactivity refers to being pushed notifications immediately when state you care about within an index changes.
;; Databases provide, at best, "coarse-grained reactivity". These are typically called "triggers" and only tell you
;; that a whole value has changed. They don’t tell you how that value has changed in any more detail.
;; In Postgres or MySQL, for example, they only tell you that a particular row/column changed.


(defmodule MyModule
  [setup topologies]
  ;; Declare a depot so we can attach data to this depot
  ;; *my-depot is used to make a depot name.
  ;; This depot takes in app-state objects. The second argument is a "depot partitioner" that controls
  ;; how appended data is partitioned across the depot, affecting on which task each piece of data begins
  ;; processing in ETLs.
  (declare-depot setup *app-state (hash-by :username))
  ;; Stream topologies process appended data within a few milliseconds and guarantee all data will be fully processed.
  ;; Their low latency makes them appropriate for a use case like this.
  (let [s      (stream-topology topologies "state")
        ;; ModuleUniqueIdPState is a small utility from rama-helpers that abstracts away the pattern of generating
        ;; unique 64-bit IDs. 64-bit IDs are preferable to UUIDs because they take half the space, but since they're
        ;; smaller generating them randomly has too high a chance of not being globally unique. ModuleUniqueIdPState
        ;; uses a PState to track a task-specific counter, and it combines that counter with the task ID to generate IDs
        ;; that are globally unique.
        id-gen (ModuleUniqueIdPState. "$$id")]

    ;; Declare the pstate, the schema here defines the type of the schema, it can either be map-schema (which I think can be edited later)
    ;; or a fixed-keys-schema (which I suspect can't be modified once created). Either way each of the values per key then further be
    ;; brokend down into say another map, or just a string, vector, set etc. each of these indexed maps then can be "subindexed" which
    ;; means the retrieval becomes much efficient if it is "subindexed" if the length is small no. say less than 50 then it does not matter if its
    ;; subindexed or not but if its in millions it matters very much there is massive performance difference.

    ;; PStates are durable and replicated datastores and are represented as an arbitrary combination of data structures.
    ;; Reads and writes to PStates go to disk and are not purely in-memory operations.
    (declare-pstate s $$app-state {Long ; user ID
                                   (fixed-keys-schema {:username String
                                                       :viewbox  (vector-schema Long)})})

    ;; ETL
    ;; <<sources defines the ETL logic as Rama dataflow code. Rama's dataflow API works differently than Clojure, but it has
    ;; the same expressiveness as any general purpose language while also being able to seamlessly distribute computation.
    (<<sources s
      ;; This describes the ETL to *app-state depot. The :> meyword seperates the inputs and outputs of the form. The output
      ;; here is destructured to capture the fielts "viewbox", "username" to Rama variables of the same name.
      ;; Because the depot partitioner on the *app-state, computation starts on the same task where app-state info is stored
      ;; for that username in the $$app-state Pstate.
      (source> *app-state :> {:keys [*viewbox *username]})
      ;; This is where we write the server logic code, for e.g is the user already registered? if so get the data for this user
      ;; and then update its state?


      ())))


