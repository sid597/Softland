;; IMPORTANT: Before modifying this file, re-read PLAN-stream-store.md (and
;; the later stages' plans) and adhere to their decisions.
(ns rig.store.module
  "The rig's store: one module, two fact stores (P1). Stage 1 builds the
  stream store: the `*offers` depot placed by layer and the `gate` stream
  topology, which decides each offer on its layer's home task in one event
  and answers through the ack. Stage 2 (PLAN-locks-and-forgetting.md) adds
  its fields to `$$layers` and its row, `$$persons`, the two query
  topologies, and its call sites inside the one event (rig.store.locks).
  Stage 5a (PLAN-read-exit.md) adds its four index fields, written in the
  same event, the `*index-ops` source and the read queries (rig.store.reads).
  Stage 3 (PLAN-micro-store.md) declares the micro store after them
  (rig.store.micro)."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.ops :as ops]
            [rig.store.gate :as gate]
            [rig.store.gate-event :as gate-event]
            [rig.store.grammar :as grammar]
            [rig.store.dependents :as dependents]
            [rig.store.dependents-query :as dependents-query]
            [rig.store.inject :as inject]
            [rig.store.locks :as locks]
            [rig.store.micro :as micro]
            [rig.store.promote :as promote]
            [rig.store.promote-flow :as promote-flow]
            [rig.store.reads :as reads]
            [rig.store.shared-reads :as shared-reads]))

(def row-fields
  "A log row's fields, named once: stage 1's positional core (`:v` the
  canonical EDN of a control value, nil for a value fact and a retract),
  then each later stage's, one merge argument per stage. The log's rows
  carry them, and so does every index entry of stage 5a (PLAN-read-exit.md
  F6), so a field a later stage adds to a row rides in every entry."
  (merge {:e        clojure.lang.Keyword
          :k        clojure.lang.Keyword
          :v        String
          :replaces clojure.lang.PersistentVector
          :mark     (set-schema clojure.lang.Keyword)}
         (locks/row-fields)))

(def layer-fields
  "Stage 1's fields of a one-owner layer's value (PLAN-stream-store.md,
  `$$layers`). The answer record keeps the bounded parts of an act
  (subjects capped at 256, F3; stage 2: L13); the act's rows are a
  subindexed vector (F1) and what it stood on its own subindexed map (F2),
  because neither has an enforced bound. Every stamp in it (the record's,
  what an act stood on, a head's) is a hybrid stamp, one long in
  rig.store.clock's encoding."
  {:settings    (fixed-keys-schema {:kind  clojure.lang.Keyword    ; :personal :hand :agent :base :store
                                    :owner clojure.lang.Keyword
                                    :class clojure.lang.Keyword    ; :by-layer :by-entity
                                    :grain clojure.lang.Keyword})  ; :per-value :per-act
   :answers     (map-schema clojure.lang.PersistentVector          ; name
                            (fixed-keys-schema
                             {:answer       clojure.lang.Keyword
                              :reason       clojure.lang.Keyword
                              :stamp        Long
                              :digest       String
                              :who          clojure.lang.Keyword
                              :class        clojure.lang.Keyword
                              :permission   clojure.lang.PersistentVector
                              :session      clojure.lang.Keyword
                              :because-of   clojure.lang.PersistentVector
                              :claimed-when Long
                              :subjects     (set-schema clojure.lang.Keyword)})
                            {:subindex-options {:track-size? false}})
   :log         (map-schema clojure.lang.PersistentVector          ; name -> the act's rows
                            (vector-schema
                             (fixed-keys-schema row-fields)
                             ;; [F1] subindexed; Rama 1.6.0's vector-schema takes only
                             ;; :subindex? (no :subindex-options), so size tracking stays on
                             {:subindex? true})
                            {:subindex-options {:track-size? false}})
   :stood-on    (map-schema clojure.lang.PersistentVector          ; name -> {fid stamp} as carried [F2]
                            (map-schema clojure.lang.PersistentVector Long
                                        {:subindex-options {:track-size? false}})
                            {:subindex-options {:track-size? false}})
   :heads       (map-schema clojure.lang.PersistentVector Long     ; [e k fid] -> stamp, while unreplaced
                            {:subindex-options {:track-size? false}})
   :permissions (map-schema clojure.lang.PersistentVector          ; pid [who layer in]
                            (fixed-keys-schema {:granted clojure.lang.PersistentVector
                                                :revoked clojure.lang.PersistentVector})
                            {:subindex-options {:track-size? false}})})

(def layers-schema
  "Everything keyed by a one-owner layer, on the layer's home task: stage
  1's fields, then each later stage's, one merge argument per stage (stage
  2: the lock rows, the lease rows, the erasure ledger, the by-stamp index;
  stage 5a: its four index fields, whose entries carry the row's fields;
  stage 4: the stored forwards; phase 6: a row per key, its grammar and
  whether the layer uses it; the citation: `:ix-dep`, the lookup from a
  fact to what stood on it, rig.store.dependents)."
  {clojure.lang.Keyword (fixed-keys-schema (merge layer-fields
                                                  (locks/layer-fields)
                                                  (reads/layer-fields row-fields)
                                                  ;; stage 4: the stored forwards
                                                  (promote/layer-fields)
                                                  ;; phase 6: the key rows
                                                  (grammar/layer-fields)
                                                  ;; the citation: the lookup from a fact to what
                                                  ;; stood on it (PLAN-dependents.md)
                                                  (dependents/layer-fields)))})

;; The one event, on the layer's home task, with no partitioner, so every
;; read sees this task's state and every write commits in one group (RQ 1):
;;   intake (parse, digest, read keys; total) -> a face refusal is answered
;;   and nothing else happens (P7) -> the record path or the decision and
;;   its writes, in rig.store.gate-event (`record-or-decide>`: the name's
;;   record first, F4; else the settings, clock, wall, permission rows and
;;   heads rows, stage 2's delivery and lock reads, `decide` (total) and the
;;   writes it precomputed, every one a set keyed by name, fact id, pid or
;;   layer, I-G2; stage 5a's index writes in the same group) -> the answer
;;   through the ack -> the person fan-out, the one partitioner, after the
;;   answer is set. The path lives in rig.store.gate-event so that the
;;   store's own steps (stage 4) take the same one; a record of `*offers`
;;   enters it here.
(defmodule Store
  [setup topologies]
  (declare-depot setup *offers (hash-by :layer))
  (reads/declare-depots! setup)
  (let [s (stream-topology topologies "gate")]
    (declare-pstate s $$layers layers-schema)
    ;; the task's last stamp, a hybrid stamp (rig.store.clock); 0 before the first
    (declare-pstate s $$clock Long {:initial-value 0})
    ;; every person's lock, on every task (stage 2, L1)
    (locks/declare-pstates! s)
    (<<sources s
      (source> *offers {:retry-mode :all-after} :> *raw)
      (gate/intake *raw :> *in)
      (<<if (contains? *in :refuse)
        ;; refused on its face: answered through the ack, recorded nowhere (P7)
        (ack-return> (gate/face-ack *in *raw))
       (else>)
        (get *in :offer :> *offer)
        (get *in :digest :> *digest)
        (get *offer :layer :> *layer)
        (get *offer :name :> *name)
        (inject/point! :seen *name)
        ;; the record path or the decision, and its writes (rig.store.gate-event)
        (gate-event/record-or-decide> *layer *offer *name *digest *in :> *ack *fan-out)
        (ack-return> *ack)
        ;; stage 4: a promotion request answered yes goes on to its read-out and
        ;; forward (rig.store.promote-flow); every other act passes through
        (promote-flow/continue> *layer *offer *ack)
        ;; the person fan-out, after the answer is set: the ack returns once
        ;; every task holds the entry (stream.md, the event tree); a recorded
        ;; person act fans out again (L9); for a person forget each task then
        ;; purges the read exit's indexes of every value that died there with
        ;; the person, dated by its wrap's close
        (locks/fan-out> *name *fan-out :> *flayer *ferased *fdate)
        (reads/purge> *flayer *ferased *fdate)))
    ;; stage 5a: the *index-ops source (rebuild pages, test-only ops) on this topology
    (reads/declare-index-ops-source! s))
  ;; stage 2's query topologies: lease-locks and read-as-of
  (locks/declare-queries! topologies)
  ;; stage 5a: the read queries read-point and read-pattern
  (reads/declare-queries! topologies)
  ;; stage 4: promotion-status
  (promote-flow/declare-queries! topologies)
  ;; stage 5b: shared reads, deltas and the maintenance reads (rig.store.shared-reads)
  (shared-reads/declare-queries! topologies)
  ;; the citation: the read of the lookup from a fact to what stood on it
  (dependents-query/declare-queries! topologies)
  ;; the micro store (stage 3): its depot, gate and queries, from its own namespace (M1)
  (micro/declare! setup topologies))
