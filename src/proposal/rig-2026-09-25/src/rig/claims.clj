(ns rig.claims
  "Rig phase 0. Throwaway modules that check, on an in-process cluster, the
  two Rama claims the formal model assumes, and whether a microbatch topology
  can read what another module wrote.

  Failure injection uses global atoms. The in-process cluster runs every task
  in this JVM, so module code sees them. A real module must never do this."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.aggs :as aggs]))

;; ------------------------------------------------------------ injection

(defonce fail-once (atom #{}))  ; tags whose next attempt throws
(defonce executed (atom {}))    ; tag -> [[what key] ...] in the order the code ran
(defonce gates (atom {}))       ; tag -> promise a held branch waits on

(defn note!
  "Record that a step ran. The record lives outside Rama, so it keeps the
  steps of attempts whose writes Rama discarded."
  [tag what k]
  (when tag (swap! executed update tag (fnil conj []) [what k]))
  nil)

(defn maybe-fail!
  "Throw once for a tag armed in `fail-once`."
  [tag]
  (when (and tag (contains? @fail-once tag))
    (swap! fail-once disj tag)
    (throw (ex-info "injected failure" {:tag tag})))
  nil)

(defn hold!
  "Block this task thread until the tag's gate is delivered, at most 20 s."
  [tag]
  (when-let [p (get @gates tag)]
    (note! tag :held nil)
    (deref p 20000 :timeout))
  nil)

(defn await-note!
  "Wait, at most 5 s, until a step has run for a tag. Used to order a
  failure or a hold after a write on another task."
  [tag what k]
  (when tag
    (let [deadline (+ (System/currentTimeMillis) 5000)]
      (loop []
        (when (and (not (some #{[what k]} (get @executed tag)))
                   (< (System/currentTimeMillis) deadline))
          (Thread/sleep 5)
          (recur)))))
  nil)

;; ---------------------------------------------------------------- stream

;; Claim 1: a stream event is atomic on one partition, and a record is
;; processed at least once when its processing fails partway.
(defmodule StreamClaims
  [setup topologies]
  (declare-depot setup *s-depot :random)
  (let [s (stream-topology topologies "s")]
    (declare-pstate s $$left {String Long})
    (declare-pstate s $$right {String Long})
    (<<sources s
      (source> *s-depot :> {:keys [*ka *kb *tag *mode]})
      (|hash *ka)
      (local-transform> [(keypath *ka) (nil->val 0) (term inc)] $$left)
      (note! *tag :left *ka)
      (<<if (= *mode :one-task)
        ;; a failure between two writes on one task, in one event
        (maybe-fail! *tag)
        (local-transform> [(keypath *ka) (nil->val 0) (term inc)] $$right)
        (note! *tag :right *ka)
       (else>)
        ;; the partitioner commits the first task's writes; the failure or
        ;; the hold happens on the second task
        (|hash *kb)
        (hold! *tag)
        (maybe-fail! *tag)
        (local-transform> [(keypath *kb) (nil->val 0) (term inc)] $$right)
        (note! *tag :right *kb)))))

;; ------------------------------------------------------------ microbatch

;; Claim 2: a microbatch is decided before it is visible, made visible at
;; commit, and atomic across partitions.
(defmodule MicroClaims
  [setup topologies]
  (declare-depot setup *m-depot :random)
  (let [mb (microbatch-topology topologies "mb")]
    (declare-pstate mb $$mcount {String Long})
    (declare-pstate mb $$mlast {String Long})
    ;; the highest record sequence number applied on each key: after a batch
    ;; commits on every task, every key holds the same value
    (declare-pstate mb $$mseq {String Long})
    (<<sources mb
      (source> *m-depot :> %mb)
      (%mb :> {:keys [*keys *tag *fail-key *hold-key *after-key *seq]})
      (ops/current-microbatch-id :> *mbid)
      (ops/explode *keys :> *k)
      (|hash *k)
      (local-transform> [(keypath *k) (nil->val 0) (term inc)] $$mcount)
      (local-transform> [(keypath *k) (termval *mbid)] $$mlast)
      (<<if (some? *seq)
        (+compound $$mseq {*k (aggs/+max *seq)}))
      (note! *tag :wrote *k)
      (<<if (= *k *fail-key)
        (await-note! *tag :wrote *after-key)
        (maybe-fail! *tag))
      (<<if (= *k *hold-key)
        (await-note! *tag :wrote *after-key)
        (hold! *tag)))))

;; ---------------------------------------------------- cross-module reads

;; Stands in for the stream store: it writes stamps.
(defmodule StampSource
  [setup topologies]
  (declare-depot setup *stamp-depot (hash-by :k))
  (let [s (stream-topology topologies "s")]
    (declare-pstate s $$stamps {String Long})
    (<<sources s
      (source> *stamp-depot :> {:keys [*k *stamp]})
      (local-transform> [(keypath *k) (termval *stamp)] $$stamps))))

;; Stands in for the micro store: its microbatch topology reads a stamp
;; written by another module, through a mirror, and one written by a stream
;; topology of its own module.
(defmodule StampReader
  [setup topologies]
  (mirror-pstate setup $$other-stamps "rig.claims/StampSource" "$$stamps")
  (declare-depot setup *own-stamp-depot (hash-by :k))
  (declare-depot setup *read-depot (hash-by :k))
  (let [s (stream-topology topologies "own")]
    (declare-pstate s $$own-stamps {String Long})
    (<<sources s
      (source> *own-stamp-depot :> {:keys [*k *stamp]})
      (local-transform> [(keypath *k) (termval *stamp)] $$own-stamps)))
  (let [mb (microbatch-topology topologies "mb")]
    (declare-pstate mb $$seen {String Object})
    (<<sources mb
      (source> *read-depot :> %mb)
      (%mb :> {:keys [*k]})
      (select> (keypath *k) $$other-stamps :> *other)
      (|hash *k)
      (local-select> (keypath *k) $$own-stamps :> *own)
      (local-transform> [(keypath *k) (termval [*other *own])] $$seen))))
