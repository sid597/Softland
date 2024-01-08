(ns app.rama-module
  (:use [com.rpl.rama]
       [com.rpl.rama.path])
  (:require [com.rpl.rama :as r :refer [<<sources
                                        ack-return>
                                        defmodule
                                        declare-depot
                                        defpath
                                        fixed-keys-schema
                                        hash-by
                                        local-select>
                                        local-transform>
                                        source>
                                        stream-topology]]

            [com.rpl.rama.aggs :as aggs]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.path :as path :refer [termval
                                                multi-path
                                                keypath]])
  (:import [com.rpl.rama.helpers ModuleUniqueIdPState]))


;; In Clojure, defrecord is a macro used to define a new record type that implements
;; a set of interfaces and can have fields. Records in Clojure are a way to create
;; data structures that are more efficient and structured than general maps, while
;; still behaving like maps.

;; uuid is added to provide a unique identifier to each request.
(defrecord app-state [viewbox username uuid])

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
  (let [s      (stream-topology topologies "state") ;; ---Extract--
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
                                   (map-keys {:username String
                                              :viewbox  (vector-schema Long)})})

    ;;   This PState is used to assign a userId to every registered username. It also prevents race conditions in the case
    ;; of multiple concurrent registrations of the same username. Every registration contains a UUID that uniquely identifies
    ;; the registration request. The first registration records its UUID along with the generated 64-bit userId in this PState.
    ;; A registration request is known to be successful if the UUID used for registration is recorded in this PState.
    ;; Further details are described below with the ETL definition.
    (declare-pstate s $$username->registration {String ; Username
                                                {fixed-keys-schema {:user-id Long
                                                                    :uuid String}}})

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

      ;; ---TRANSFORM---
      ;; Now the step here is to see if the user already exists, if it does we fetch the information and bind it to
      ;; some variable.
      ;; A critical property of Rama is that only one event can run on a task at time. So while an ETL event is running,
      ;; no other ETL events, PState queries, or other events can run on the task. In this case, we know that any other
      ;; registration requests for the same username are queued behind this event, and there are no race conditions with
      ;; concurrent registrations because they are run serially on this task for this username.

      ;; Get the values, think of this as `let` statement.
      ;; NOTE:`{*curr-uuid :uuid :as *curr-info}` this basically means bind :uuid to *curr-uuid and bind the whole incoming
      ;; value to *curr-info. Neat this is just clojure nothing else.
      (local-select> (keypath *username) $$username->registration :> {*curr-uuid :uuid :as *curr-info})

      ;; If *curr-info is null it means, what? nothing I think in my case because I can initialise default values of these
      ;; username and viewbox, right??
      ;; Anyway I think for the sake of following the example I can. null means, data is not there and I have to seed the data.
      ;; if its there then  I bind it to something? or something lets see.
      ;; On further thought we can just update the viewbox value there are no conditionals right?


      ;; Apparently this is how we update the values I don't get it as of now what is going on.

      (local-transform>
        [(keypath *username)
         (multi-path [:username (termval *username)
                      :viewbox (termval *viewbox)])]
        $$app-state)


      ;; Stream topologies can return information back to depot append clients with "ack returns". The client
      ;; receives the resulting "ack return" for each subscribed colocated stream topology in a map from
      ;; topology name to value. Here, the ack return is used to let the client know the user ID for their
      ;; newly registered username. If the ack return is nil, then the client knows the username registration
      ;; failed.
      (ack-return> *user-id))))



;; Ok I think I get it more now, lets talk about the ETL flow.
;; We create a stream from one of the depots.
;; In the stream we make a few  source>? statements which are basically let statements
;; using these statements we can extract the values of varionus fields of events present in the depot/
;; Inside the stream we can also query the various Pstates we have and get values from any of them for
;; computation. For e.g check if the user already exists in the User's Pstate then get the values and
;; destruct or map it to some vars represent using `*` in front of them.
;; now we can use conditionals loops etc. for these vars etc, map the restult to some other vars
;; Then use the new vars to update the Pstate with new value.
;; Finally return information back to depot append cleints with "ack returns"


