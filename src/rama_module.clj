(ns rama-module
  (:use [com.rpl.rama]
       [com.rpl.rama.path])
  (:require [com.rpl.rama :as r :refer [<<sources
                                        close!
                                        defmodule
                                        declare-depot
                                        foreign-append!
                                        foreign-depot
                                        foreign-pstate
                                        fixed-keys-schema
                                        foreign-select-one
                                        get-module-name
                                        hash-by
                                        local-select>
                                        local-transform>
                                        source>
                                        stream-topology]]
            [com.rpl.specter :as s]
            [com.rpl.rama.aggs :as aggs]
            [com.rpl.rama.test :as rtest]
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


(defmodule rfModule
  [setup topologies]
  ;; Declare a depot so we can attach data to this depot
  ;; *my-depot is used to make a depot name.
  ;; This depot takes in app-state objects. The second argument is a "depot partitioner" that controls
  ;; how appended data is partitioned across the depot, affecting on which task each piece of data begins
  ;; processing in ETLs.
  (declare-depot setup *app-state-depot :random #_(hash-by :uuid))
  ;; Stream topologies process appended data within a few milliseconds and guarantee all data will be fully processed.
  ;; Their low latency makes them appropriate for a use case like this.
  (let [s      (stream-topology topologies "app-state-topology") ;; ---Extract--
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


    ;; This is just a map saying the key has type Long and the value is a fixed key schema for viewbox and username
    ;; we could also have gone with a simple map-schema where we just mention the type of the key and value.
    ;; Example f the map-schema is
    ;; The following schema means there is a map where each long has a map which is long  key and string value type
    ;; (declare-pstate s $$app-state-pstate {Long (map-schema Long String})
    ;; ==>
    ;; {1 {2 "hello"}
    ;; 3 {4 "world"}}

    (declare-pstate s $$app-state-pstate {Long ; uuid
                                          (fixed-keys-schema {:viewbox  (vector-schema Long)
                                                              :username String})})

    ;;   This PState is used to assign a userId to every registered username. It also prevents race conditions in the case
    ;; of multiple concurrent registrations of the same username. Every registration contains a UUID that uniquely identifies
    ;; the registration request. The first registration records its UUID along with the generated 64-bit userId in this PState.
    ;; A registration request is known to be successful if the UUID used for registration is recorded in this PState.
    ;; Further details are described below with the ETL definition.
    #_(declare-pstate s $$username->registration {String ; Username
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

      (source> *app-state-depot :> {:keys [*viewbox *username *uuid]})

      ;; We can just print the value to see what they are.
      (println "username" *username "viewbox" *viewbox "-->" *uuid)
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

      ;; The basic operation for reading from a PState is local-select>. This reads from the PState partition colocated
      ;; on the executing event’s current task.
      #_(local-select> (keypath *username) $$username->registration :> {*curr-uuid :uuid :as *curr-info})

      ;; If *curr-info is null it means, what? nothing I think in my case because I can initialise default values of these
      ;; username and viewbox, right??
      ;; Anyway I think for the sake of following the example I can. null means, data is not there and I have to seed the data.
      ;; if its there then  I bind it to something? or something lets see.
      ;; On further thought we can just update the viewbox value there are no conditionals right?


      ;; Apparently this is how we update the values I don't get it as of now what is going on.
      ;; The basic operation for writing to a PState is local-transform>. Transform paths for this operation are like
      ;; transform paths for multi-transform in open-source Specter. Terminal navigators in the path must be either term,
      ;; termval, or NONE>.)

      ;; How to do this in one line???
      (local-transform>
        [(keypath *uuid) :username (termval *username)]
        $$app-state-pstate)
      (local-transform>
        [(keypath *uuid) :viewbox (termval *viewbox)]
        $$app-state-pstate)


      ;; Stream topologies can return information back to depot append clients with "ack returns". The client
      ;; receives the resulting "ack return" for each subscribed colocated stream topology in a map from
      ;; topology name to value. Here, the ack return is used to let the client know the user ID for their
      ;; newly registered username. If the ack return is nil, then the client knows the username registration
      ;; failed.
      #_(r/ack-return> *user-id))))



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



;; Now coming to how to query the Pstates


(comment
 ;; launch the cluster and test ??
 (do
  (def ipc (rtest/create-ipc))
  (rtest/launch-module! ipc rfModule {:tasks 4 :threads 2})

 ;; foreign-depot: Retrieve a client for a depot
 ;; The term `foreign` refers to Rama objects that live outside of Modules

  (def app-state-depot (foreign-depot ipc (get-module-name rfModule) "*app-state-depot"))
  (def app-state-pstate (foreign-pstate ipc (get-module-name rfModule) "$$app-state-pstate"))

 ;; Append data to depot

  (foreign-append! app-state-depot (->app-state [0 0 3000 3000] "sid" 1))
  (foreign-append! app-state-depot (->app-state [0 0 2030 3300] "sid2" 2)))

 ;; foreign-select-one queries Pstate with path. Path must navigate to exactly one value

 (foreign-append! app-state-depot (->app-state [0 0 2030 3300] "sid2" 2))

 (foreign-select (keypath 1) app-state-pstate)
 (foreign-select (keypath 2) app-state-pstate)


 (close! ipc))

(defn -main [& args]
  (println "Hello world" args))


;; so the steps to doing this are

;; Decide what is the schema of the pstate
;; - Then based on the schema we declare the depot inside which we mention how we
;;   hash the event by or we can just make it :random.
;; - After declaring the depot we declare a few stream topologies
;; - Then we go on to describe the pstate along with its schema
;; - After the pstate we can go into streaming the events from the depot.
;; - For each of the even we can then get the data out of the stream using (local-select>)...
;; - Then we can use the data from the event to transform it, do some calculation on it
;;   and then store the results in another variable
;; - After the transformation we can store the results in the pstate according to its
;;   schema, NOTE: currently I don't know how to update multiple paths in the schema.
;; - Then comes the outside part, where we initialise the ipc, module and get the
;;   pointers to the pstate and depots,
;; - Then we append data to the pstate which is basically a simulation of what we excpect
;;   the client to append.   `foreign-append!`
;; - Once data is appended we can query the pstate for data that is present on it. `foreign-select`

;; What is the minimum data required for an event?
;; What has to be done?
;; New data related to this action?
;; Who is doing it?
;; Path to where this action will take place?
;; I Think this can be handled using specter?


;; If I mention path then I am making the client and server
;; dependent on the ineer workings, is it a good or a bad thing?
;; what are and might be the consequences?
;; maybe not the question to ask right now

;; I think the best way to make the events would be just like rama
;; have correspoding data for ETL i.e
;;
;; extract-path
;; data to be transformed
;; transform logic
;; load path
;; generally extract and load path will be the same??
;;

;; More close to how "setval" is defined
;; "setval": (setval path value data-to-transform)
;; e.g (setval [MAP-KEYS NAMESPACE] (str *ns*) any-map)

(defrecord events [event-data extract-path transform-data transform-action load-path])
;(defrecord edges [extract-path edge-id edge-data action])


(defmodule nodes [setup topologies]
  (declare-depot setup *events-depot :random)
  (let [n (stream-topology topologies "events-topology")]
    (declare-pstate n $$nodes-pstate {clojure.lang.Keyword ;; node-id
                                      (fixed-keys-schema
                                        {:id                 clojure.lang.Keyword
                                         :x                  Long
                                         :y                  Long
                                         :type-specific-data (map-schema clojure.lang.Keyword Object)
                                         :type               String
                                         :color              String})})
    (declare-pstate n $$edges-pstate {clojure.lang.Keyword ;; node-id
                                      (fixed-keys-schema
                                        {:id                 clojure.lang.Keyword
                                         :type-specific-data (map-schema clojure.lang.Keyword Object)
                                         :type               String
                                         :color              String})})

    (<<sources n
      ;; So how does this work?
      ;; what happens from the client side?
      ;; I modify a node's parameter say text wor a editor in rect node.
      ;; What expect to happen is to update the text aprameter on the server when that happens
      ;; I want to listen to he chaneges in the ui related to the text itself.
      ;; So what event fwill be sent to the server?
      ;; I think it would be node-id, and the pair of new values that have to be updated ??

      (source> *events-depot :> {:keys [*event-data
                                        *extract-path
                                        *transform-data
                                        *transform-action
                                        *load-path]})
      (println "event-data" *event-data "extract-path" *extract-path "transform-action" *transform-action "load-path" *load-path))))

;; how much to do in 1 event? how big should 1 event be?

(comment
  (do
    (def ipc (rtest/create-ipc))
    (rtest/launch-module! ipc rfModule {:tasks 4 :threads 2})

    ;; foreign-depot: Retrieve a client for a depot
    ;; The term `foreign` refers to Rama objects that live outside of Modules

    (def events-depot (foreign-depot ipc (get-module-name rfModule) "*events-depot"))
    (def nodes-pstate (foreign-pstate ipc (get-module-name rfModule) "$$nodes-pstate"))
    (def edges-pstate (foreign-pstate ipc (get-module-name rfModule) "$$edges-pstate"))

    ;; Append data to depot

    ;; different type of actions that we can do on the nodes:
    ;; - add new nodes
    ;; - delete a node
    ;; - update a node
    ;; -- multi-update
    ;; -- single-update

    (foreign-append! events-depot (->events
                                    {:event-id 1
                                     :username "sid"}
                                    [:node-id]
                                    [[:x]
                                     [:y]]
                                    [:multi-update]))))

(def data
  {"sid" {:nodes {:sv-circle {:id :sv-circle
                              :x 700
                              :y 100
                              :type-specific-data {:r 100
                                                   :dragging? false}
                              :type "circle"
                              :color "red"}
                  :rect       {:id :rect
                               :x 500
                               :y 600
                               :type-specific-data {:text "GM Hello"
                                                    :width 400
                                                    :height 800}
                               :type "rect"
                               :fill  "lightblue"}}}})



(s/select-one (s/keypath "sid" :nodes :rect) data)
(s/select-one ["sid" :nodes :rect] data)
(s/select (s/multi-path
            ["sid" :nodes :rect :y]
            ["sid" :nodes :rect :x]) data)
(s/select (s/multi-path
            (s/keypath "sid" :nodes :rect :y)
            ["sid" :nodes :rect :x]) data)
(s/setval
  (s/multi-path
    (s/keypath "sid" :nodes :rect :y)
    ["sid" :nodes :rect :x])
  10
  data)
(s/setval
  (s/multi-path
    ["sid" :nodes :rect :y]
    ["sid" :nodes :rect :x])
  [10 100]
  data)

;; This is how we update multiple values
(s/multi-transform
    ["sid" :nodes :rect (s/multi-path
                          [:x (s/terminal-val 1)]
                          [:y (s/terminal-val 3)]
                          [:type-specific-data :text (s/terminal-val "NGMI")])]
  data)