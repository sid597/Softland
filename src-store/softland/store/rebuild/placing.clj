(ns softland.store.rebuild.placing
  "What gives the task of a route: a module of one query, `Placing`, and the
  client's call of it. It requires Rama.

  Why it is here. A row of a batch is on the task that Rama's hash
  partitioner gives its route, and no log holds that
  (`softland.store.rebuild`, `:task-of-route`). The store's own road from a
  routing id to a task is a call with `{:pkey routing-id}`, in which Rama's
  client finds the task and tells no number; and from a task to a routing
  id, `com.rpl.rama.Helpers/genHashingIndexKeys`, which gives a key for a
  task and not the task of a key. Rama's client has no call that gives the
  task of a value. So Rama is asked where it does the placing itself, in a
  topology: the query partitions by the route with the hash partitioner,
  reads the number of the task that it is then on, and gives it back. The
  rebuild has no hash of its own to keep equal to Rama's.

  What it takes and gives. `tasks-of-routes` takes a cluster handle of
  Rama's and routes, and gives a map from each route to its task.

  What it rests on. Rama's hash partitioner gives one value one task for
  one number of tasks, in whatever module. `Placing` is a module of its
  own, so it must be launched with the number of tasks of the module whose
  rows are placed; a rebuild at another number of tasks launches it with
  that number. Rama's own keys for a task rest on the same thing: they are
  made for a number of tasks and for no module. The rebuild's proof holds
  this query against those keys, and holds every row that it places against
  the task where the store's gate wrote it. Neither is a word of Rama's
  team.

  Not decided. That the query is in a module beside the store and not one
  query more in the store's own module is this build's choice, made because
  this build adds no topology to the store; it is Sid's to change.

  State and effects. The module declares no depot and no PState and writes
  nothing. A call reads nothing of the store.

  Limits. `tasks-of-routes` makes one call of the query for each route, and
  each is one trip to the cluster. A rebuild asks the task of the uuid of
  every act that a batch decided, of every entity of a fact whose row is on
  one task, and of every value digest under a marked key, so the calls grow
  with the store. A query that takes many routes in one call is not built.

  Callers. The rebuild's proof."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.ops :as ops])
  (:import [java.util.concurrent CompletableFuture TimeUnit]))

(defmodule Placing [setup topologies]
  ;; task-of-route [route] -> the number of the task that the hash
  ;; partitioner gives the route, at this module's number of tasks.
  (<<query-topology topologies "task-of-route" [*route :> *task]
    (|hash *route)
    (ops/current-task-id :> *task)
    (|origin)))

(def ^:private asked-at-once
  "How many routes are asked for before the first answer is waited for. A
  bound on the calls that are open at once; each is one hop to a task and
  back."
  64)

(def ^:private wait-ms
  "How long one answer is waited for, in milliseconds, before the call
  throws."
  30000)

(defn tasks-of-routes
  "Takes a cluster handle of Rama's, in which `Placing` is launched, and a
  collection of routes: ids and texts, as
  `softland.store.rebuild/routes-asked` gives them. Gives a map from each
  route to the number of its task. One call of the query for each route,
  `asked-at-once` of them open at a time. It throws when a call throws or
  gives no answer within `wait-ms`. What must hold: `Placing` has the number
  of tasks of the module whose rows are placed."
  [cluster routes]
  (let [query (foreign-query cluster (get-module-name Placing) "task-of-route")]
    (into {}
          (mapcat (fn [some-routes]
                    (let [asked (mapv (fn [route] [route (foreign-invoke-query-async query route)])
                                      some-routes)]
                      (mapv (fn [[route ^CompletableFuture answer]]
                              [route (long (.get answer (long wait-ms) TimeUnit/MILLISECONDS))])
                            asked))))
          (partition-all asked-at-once routes))))
