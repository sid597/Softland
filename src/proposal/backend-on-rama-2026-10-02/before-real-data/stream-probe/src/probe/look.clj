(ns probe.look
  "What a run uses to look at the probe's module from outside: the clients,
  one read of a row, and two threads that watch, one a row and one a
  partition of the second depot.

  Every read here is a client's read. In Rama a client's read of a PState
  and a client's read of a depot are both served by the thread of the task
  that holds the partition. So while an event holds that thread, a watcher
  is given nothing, and it sees the state only between two events."
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest]
            [probe.gate :as g]
            [probe.util :as u])
  (:import [java.util.concurrent.locks LockSupport]))

(defn open
  "Takes an in-process cluster on which `GateProbe` runs with four tasks.
  Gives the clients of its two depots and of its PState, and one key for
  each task, in the order of the tasks."
  [ipc]
  (let [m (get-module-name g/GateProbe)]
    {:ipc ipc
     :m m
     :things (foreign-depot ipc m "*things")
     :log (foreign-depot ipc m "*log")
     :rows (foreign-pstate ipc m "$$rows")
     :ks (vec (rtest/gen-hashing-index-keys "key-" 4))}))

(defn row
  "Takes the clients, a task's key and an id. Gives the row that a client is given under the id, or nil."
  [ctx k id]
  (foreign-select-one (keypath id) (:rows ctx) {:pkey k}))

(defn fmt
  "Takes a number. Gives it with one decimal, or a dash for nil."
  [x]
  (if x (format "%.1f" (double x)) "—"))

(defn watch-row
  "Starts a thread that reads the row of an id again and again and says
  each change, a first nil left unsaid. Takes the clients, the task's key,
  the id, an atom that holds true when the thread must end, and an atom of
  a vector to which each sighting is added as {:t :kind :row :id :attempt}.
  A read that throws is counted as no change."
  [ctx k id stop seen]
  (doto (Thread.
         (fn []
           (loop [last-value nil]
             (when-not @stop
               (let [v (try (row ctx k id) (catch Throwable _ last-value))]
                 (when (not= v last-value)
                   (swap! seen conj {:t (u/ms) :kind :row :id id :attempt (:attempt v)})
                   (u/say "SELECT " id "is now given as" (pr-str v)))
                 (LockSupport/parkNanos 100000)
                 (recur v))))))
    (.setDaemon true)
    (.setName (str "watch row " id))
    (.start)))

(defn watch-log
  "Starts a thread that reads the end offset of one partition of the second
  depot again and again and, when it grew, reads the new records and says
  each. Takes the clients, the partition, the offset to start from, an atom
  that holds true when the thread must end, and an atom of a vector to
  which each sighting is added as {:t :kind :record :offset :id :attempt}.
  A read that throws is counted as no change."
  [ctx p from stop seen]
  (let [log (:log ctx)]
    (doto (Thread.
           (fn []
             (loop [at from]
               (when-not @stop
                 (let [end (try (g/log-end log p) (catch Throwable _ at))
                       recs (when (> end at) (try (foreign-depot-read log p at end) (catch Throwable _ nil)))]
                   (if recs
                     (do (doseq [[i r] (map-indexed vector recs)]
                           (swap! seen conj {:t (u/ms) :kind :record :offset (+ at i) :id (:id r) :attempt (:attempt r)})
                           (u/say "DEPOT   offset" (+ at i) "of partition" p "is now readable:" (pr-str r)))
                         (LockSupport/parkNanos 100000)
                         (recur (long end)))
                     (do (LockSupport/parkNanos 100000)
                         (recur (long at)))))))))
      (.setDaemon true)
      (.setName (str "watch log " p))
      (.start))))

(defn began-since
  "Takes the map of attempts as it was before. Gives, for each id whose
  event began since then, how many times it began since then, as text."
  [before]
  (let [now @g/attempts
        diff (for [[id n] now :let [d (- n (get before id 0))] :when (pos? d)] [id d])]
    (pr-str (into (sorted-map) diff))))
