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
            [rig.store.inject :as inject]
            [rig.store.locks :as locks]
            [rig.store.micro :as micro]
            [rig.store.reads :as reads]))

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
  stage 5a: its four index fields, whose entries carry the row's fields)."
  {clojure.lang.Keyword (fixed-keys-schema (merge layer-fields
                                                  (locks/layer-fields)
                                                  (reads/layer-fields row-fields)))})

;; The one event, on the layer's home task, with no partitioner, so every
;; read sees this task's state and every write commits in one group (RQ 1):
;;   intake (parse, digest, read keys; total) -> a face refusal is answered
;;   and nothing else happens (P7) -> the name's record (F4: first) -> a
;;   record answers or refuses as taken, nothing written -> else settings,
;;   clock, wall, permission rows and heads rows (loop<-, never explode: an
;;   act with no replaces must still reach the decision, F7) -> decide
;;   (total) -> the writes it precomputed, every one a set keyed by name,
;;   fact id, pid or layer (I-G2) -> the answer through the ack.
;; Stage 2's steps are calls into rig.store.locks inside this one event
;; (PLAN-locks-and-forgetting.md, gate event steps 3 to 11): the record
;; path's value check and consumption; the delivery, whose missing lock is a
;; face refusal; the decision reads and fresh draw; the lock writes, in the
;; decision's group; and the person fan-out, the one partitioner, after the
;; answer is set. Stage 5a's index writes are the decision's too, in the
;; same group.
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
        (local-select> (keypath *layer :answers *name) $$layers :> *rec)
        (<<if (some? *rec)
          ;; decided before: the recorded answer, or the name is taken; the one
          ;; write is the consumption of the cited lease rows (stage 2, V-F1)
          (gate/answer-from-record *rec *digest *name :> *d0)
          (locks/record-path> *layer *offer *rec *d0 :> *d)
          (inject/point! :recorded *name)
          (locks/consume-locks> *layer (get *offer :session) (get *d :consume))
          (ack-return> (get *d :ack))
          ;; a recorded person act fans out again (L9); a recorded person forget's
          ;; values are purged again on every task, idempotently
          (locks/fan-out> *name (get *d :fan-out) :> *rlayer *rerased *rdate)
          (reads/purge> *rlayer *rerased *rdate)
         (else>)
          (local-select> (keypath *layer :settings) $$layers :> *settings)
          ;; stage 2: the delivery; a missing lock is refused on its face
          (locks/deliver-all> *layer *offer *settings :> *lk)
          (<<if (get *lk :missing?)
            (ack-return> (gate/no-such-lock-ack *name))
           (else>)
            (local-select> STAY $$clock :> *clock)
            (gate/wall-now :> *wall)
            (get *in :pids :> *pids)
            (loop<- [*todo *pids *acc {} :> *rows]
              (<<if (empty? *todo)
                (:> *acc)
               (else>)
                (first *todo :> *pid)
                (local-select> (keypath *layer :permissions *pid) $$layers :> *row)
                (continue> (rest *todo) (assoc *acc *pid *row))))
            (get *in :heads :> *hkeys)
            (loop<- [*todo *hkeys *acc {} :> *heads]
              (<<if (empty? *todo)
                (:> *acc)
               (else>)
                (first *todo :> *hk)
                (local-select> (keypath *layer :heads *hk) $$layers :> *hstamp)
                (continue> (rest *todo) (assoc *acc *hk *hstamp))))
            (locks/decision-reads> *layer *offer *settings *lk :> *lx)
            (gate/decide *offer *settings *rows *heads *clock *wall *digest *lx :> *d)
            (<<if (= :decide (get *d :kind))
              (inject/point! :before-writes *name)
              ;; every write is a set of a value computed before any write, so a
              ;; replay that reaches here writes the same rows (I-G2)
              (get *d :record :> *record)
              (local-transform> [(keypath *layer :answers *name) (termval *record)] $$layers)
              (<<if (= :yes (get *record :answer))
                ;; the act's rows, one whole-vector write into the subindexed vector (F1, F14)
                (local-transform> [(keypath *layer :log *name) (termval (get *d :log))] $$layers)
                ;; what the act stood on, one set per carried entry (F2)
                (<<atomic
                  (ops/explode-map (get *d :stood-on) :> *sf *ss)
                  (local-transform> [(keypath *layer :stood-on *name *sf) (termval *ss)] $$layers))
                (<<atomic
                  (ops/explode (get *d :heads-del) :> *hk)
                  (local-transform> [(keypath *layer :heads *hk) NONE>] $$layers))
                (<<atomic
                  (ops/explode (get *d :heads-put) :> [*hk *hs])
                  (local-transform> [(keypath *layer :heads *hk) (termval *hs)] $$layers))
                ;; stage 5a: the act's index entries, computed in decide (reads/index-writes);
                ;; the same three blocks apply a purge's and a rebuild's lists
                (<<atomic
                  (ops/explode (get *d :index-put) :> [*ix *ia *ie])
                  (local-transform> [(keypath *layer *ix *ia) (termval *ie)] $$layers))
                (<<atomic
                  (ops/explode (get *d :index-of) :> [*ofid *ias])
                  (local-transform> [(keypath *layer :ix-of *ofid) (termval *ias)] $$layers))
                (<<atomic
                  (ops/explode (get *d :index-del) :> [*dx *da])
                  (local-transform> [(keypath *layer *dx *da) NONE>] $$layers))
                ;; settings and permission rows as they stand after the act; both were
                ;; read in this event, so each is one write with no read
                (get *d :settings :> *new-settings)
                (<<if (some? *new-settings)
                  (local-transform> [(keypath *layer :settings) (termval *new-settings)] $$layers))
                (<<atomic
                  (ops/explode (get *d :permissions) :> [*pp *prow])
                  (local-transform> [(keypath *layer :permissions *pp) (termval *prow)] $$layers)))
              ;; stage 2's lock writes, yes or no, in the same group
              (locks/write-decision> *layer *offer *d)
              ;; a value forget purges the read exit's indexes of every value its lock
              ;; erased, in the same group, dated by its stamp (the ledger's date)
              (get (get *d :locks) :purge :> *purge)
              (<<if (seq *purge)
                (reads/purge> *layer *purge (get *d :stamp)))
              (local-transform> [(termval (get *d :stamp))] $$clock)
              (inject/point! :after-writes *name))
            (ack-return> (get *d :ack))
            ;; the person fan-out, after the answer is set: the ack returns once
            ;; every task holds the entry (stream.md, the event tree); for a person
            ;; forget each task purges the read exit's indexes of every value that
            ;; died there with the person, dated by its wrap's close
            (locks/fan-out> *name (get (get *d :locks) :fan-out) :> *dlayer *derased *ddate)
            (reads/purge> *dlayer *derased *ddate)))))
    ;; stage 5a: the *index-ops source (rebuild pages, test-only ops) on this topology
    (reads/declare-index-ops-source! s))
  ;; stage 2's query topologies: lease-locks and read-as-of
  (locks/declare-queries! topologies)
  ;; stage 5a: the read queries read-point and read-pattern
  (reads/declare-queries! topologies)
  ;; the micro store (stage 3): its depot, gate and queries, from its own namespace (M1)
  (micro/declare! setup topologies))
