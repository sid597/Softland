(ns rig.probe.micro-decl-test
  "Probe, 26 September 2026, on the in-process cluster (Rama 1.6.0).
  Q1: can a plain defn declare a module's depots, microbatch topology (with
  its PStates and <<sources) and query topology when defmodule's body is one
  call to it. Q2: what (ops/current-microbatch-id) emits, whether dec works
  on it, and whether it holds across a retried attempt. Q3: a vector as a
  PState's top-level key, written after (|hash *k) and read by
  foreign-select-one. Q4: a tick depot as a second source of the same
  microbatch: does the microbatch run with no depot records.
  The atoms below are a side channel the module's code can reach only
  because every task of the in-process cluster runs in this JVM."
  (:require [clojure.test :refer [deftest is]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.test :as rtest]))

(defonce attempts (atom []))  ; one entry per record per attempt that reached it
(defonce fired (atom []))     ; the one-shot throws that fired
(defonce armed (atom nil))    ; the tag whose next attempt throws, once

(defn note-attempt!
  "Records the microbatch id an attempt saw for a record, its class, and dec of it."
  [tag b cls b-1]
  (swap! attempts conj {:tag tag :id b :class (when cls (.getName ^Class cls)) :dec b-1
                        :thread (.getName (Thread/currentThread))})
  nil)

(defn maybe-throw!
  "Throws once, on the first attempt that reaches a record of the armed tag."
  [tag b]
  (let [[old _] (swap-vals! armed #(if (= % tag) nil %))]
    (when (= old tag)
      (swap! fired conj {:tag tag :id b})
      (throw (ex-info "probe: one-shot throw on the first attempt" {:tag tag :id b}))))
  nil)

(defn declare-micro!
  "Declares every depot, PState and topology of the probe's module on the
  setup and topologies that defmodule hands it."
  [setup topologies]
  (declare-depot setup *events (hash-by :tag))
  (declare-tick-depot setup *tick 250)
  (let [mb (microbatch-topology topologies "micro")]
    (declare-pstate mb $$by-tag {clojure.lang.Keyword Long})
    (declare-pstate mb $$by-vec {clojure.lang.PersistentVector (fixed-keys-schema {:x Long})})
    (declare-pstate mb $$runs Long {:global? true :initial-value 0})   ; once per microbatch
    (declare-pstate mb $$ticks Long {:global? true :initial-value 0})  ; once per tick emitted
    (<<sources mb
      (source> *tick :> %tick)
      (%tick)
      (|global)
      (local-transform> (term inc) $$ticks)

      (source> *events :> %mb)
      ;; runs every microbatch, whether or not *events has records in it
      (<<batch
        (|global)
        (local-transform> (term inc) $$runs))
      (<<batch
        (%mb :> {:keys [*tag *k *x]})
        (ops/current-microbatch-id :> *b)
        (class *b :> *cls)
        (<<if (number? *b)
          (dec *b :> *b-1)
         (else>)
          (identity nil :> *b-1))
        (note-attempt! *tag *b *cls *b-1)
        (maybe-throw! *tag *b)
        (|hash *tag)
        (local-transform> [(keypath *tag) (termval *x)] $$by-tag)
        (filter> (some? *k))
        (|hash *k)
        (local-transform> [(keypath *k :x) (termval *x)] $$by-vec))))
  (<<query-topology topologies "x-of" [*tag :> *x]
    (|hash *tag)
    (local-select> (keypath *tag) $$by-tag :> *x)
    (|origin)))

(defmodule MicroDecl [setup topologies]
  (declare-micro! setup topologies))

(defonce poll-errors (atom []))  ; reads that threw while polling (a worker restarting)

(defn- poll
  "Calls f every 50 ms until it returns non-nil or timeout-ms passes. A read
  that throws (the worker is restarting after the one-shot throw) counts as
  not yet and is recorded."
  [f timeout-ms]
  (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
    (loop []
      (let [v (try (f)
                   (catch Exception e
                     (swap! poll-errors conj (str (.getSimpleName (class e)) " "
                                                  (re-find #":reason :[a-z-]+" (str (.getMessage e)))))
                     nil))]
        (if (or (some? v) (> (System/currentTimeMillis) deadline))
          v
          (do (Thread/sleep 50) (recur)))))))

(defn- p [& xs] (apply println "PROBE" xs) (flush))

(deftest micro-decl
  (reset! attempts []) (reset! fired []) (reset! armed nil) (reset! poll-errors [])
  (with-open [ipc (rtest/create-ipc)]
    (let [t0 (System/currentTimeMillis)
          _ (rtest/launch-module! ipc MicroDecl {:tasks 4 :threads 2 :workers 1})
          launched (System/currentTimeMillis)
          mn (get-module-name MicroDecl)
          events (foreign-depot ipc mn "*events")
          by-tag (foreign-pstate ipc mn "$$by-tag")
          by-vec (foreign-pstate ipc mn "$$by-vec")
          runs (foreign-pstate ipc mn "$$runs")
          ticks (foreign-pstate ipc mn "$$ticks")
          x-of (foreign-query ipc mn "x-of")]
      (p "Q1 launched" mn "in" (- launched t0) "ms")

      ;; Q1: a microbatch declared from the defn processes an append; the query answers
      (foreign-append! events {:tag :q1 :x 7})
      (let [v (poll #(foreign-select-one (keypath :q1) by-tag) 60000)
            q (foreign-invoke-query x-of :q1)]
        (p "Q1 $$by-tag :q1 =" v "; (foreign-invoke-query (foreign-query ipc mn \"x-of\") :q1) =" q)
        (is (= 7 v))
        (is (= 7 q)))

      ;; Q2: the first attempt of the batch holding :q2 throws once; the retry must land it
      (reset! armed :q2)
      (let [a0 (System/currentTimeMillis)]
        (foreign-append! events {:tag :q2 :x 11})
        (let [v (poll #(foreign-select-one (keypath :q2) by-tag) 120000)
              q2 (filterv #(= :q2 (:tag %)) @attempts)]
          (p "Q2 $$by-tag :q2 =" v "after" (- (System/currentTimeMillis) a0) "ms; armed now" (pr-str @armed))
          (p "Q2 fired" (pr-str @fired) "; reads that threw while polling:" (count @poll-errors) (pr-str (frequencies @poll-errors)))
          (p "Q2 attempts for :q2" (pr-str q2))
          (p "Q2 all attempts" (pr-str @attempts))
          (is (= 11 v))
          (is (= 1 (count @fired)))
          (is (= 2 (count q2)))
          (is (apply = (map :id q2)))))

      ;; Q4: no *events appends for ~2 s; do the counters move?
      (let [r0 (poll #(foreign-select-one STAY runs) 30000)
            k0 (poll #(foreign-select-one STAY ticks) 30000)
            w0 (System/currentTimeMillis)]
        (Thread/sleep 2000)
        (let [r1 (poll #(foreign-select-one STAY runs) 30000)
              k1 (poll #(foreign-select-one STAY ticks) 30000)
              w1 (System/currentTimeMillis)]
          (p "Q4 window" (- w1 w0) "ms with no *events appends: $$runs" r0 "->" r1
             "; $$ticks" k0 "->" k1 "; ms since launch at end" (- w1 launched))
          (is (> r1 r0))))

      ;; Q3: a vector as the top-level key, written after (|hash *k), read by the client
      (let [k [:group :by-entity :offer "abc"]]
        (foreign-append! events {:tag :q3 :k k :x 3})
        (let [t (poll #(foreign-select-one (keypath :q3) by-tag) 60000)
              v (poll #(foreign-select-one (keypath k) by-vec) 10000)]
          (p "Q3 key" (pr-str k) "class" (.getName (class k))
             "; $$by-tag :q3 =" t "; (foreign-select-one (keypath k) $$by-vec) =" (pr-str v))
          (is (= {:x 3} v))))

      (p "END attempts" (pr-str @attempts)))))
