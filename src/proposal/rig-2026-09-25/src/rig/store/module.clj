;; IMPORTANT: Before modifying this file, re-read PLAN-stream-store.md (and
;; the later stages' plans) and adhere to their decisions.
(ns rig.store.module
  "The rig's store: one module, two fact stores (P1). Stage 1 builds the
  stream store: the `*offers` depot placed by layer and the `gate` stream
  topology, which decides each offer on its layer's home task in one event
  and answers through the ack."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.ops :as ops]
            [rig.store.gate :as gate]
            [rig.store.inject :as inject]))

(def layers-schema
  "Everything keyed by a one-owner layer, on the layer's home task
  (PLAN-stream-store.md, `$$layers`). The answer record keeps the bounded
  parts of an act (subjects capped at 256 by the parser, F3); the act's rows
  are a subindexed vector (F1) and what it stood on its own subindexed map
  (F2), because neither has an enforced bound. Every stamp in it (the
  record's, what an act stood on, a head's) is a hybrid stamp, one long in
  rig.store.clock's encoding."
  {clojure.lang.Keyword
   (fixed-keys-schema
    {:settings    (fixed-keys-schema {:kind  clojure.lang.Keyword    ; :personal :hand :agent
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
                               (fixed-keys-schema {:e        clojure.lang.Keyword
                                                   :k        clojure.lang.Keyword
                                                   :v        String              ; canonical EDN; nil for a retract
                                                   :replaces clojure.lang.PersistentVector
                                                   :mark     (set-schema clojure.lang.Keyword)})
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
                              {:subindex-options {:track-size? false}})})})

;; The one event, on the layer's home task, with no partitioner, so every
;; read sees this task's state and every write commits in one group (RQ 1):
;;   intake (parse, digest, read keys; total) -> a face refusal is answered
;;   and nothing else happens (P7) -> the name's record (F4: first) -> a
;;   record answers or refuses as taken, nothing written -> else settings,
;;   clock, wall, permission rows and heads rows (loop<-, never explode: an
;;   act with no replaces must still reach the decision, F7) -> decide
;;   (total) -> the writes it precomputed, every one a set keyed by name,
;;   fact id, pid or layer (I-G2) -> the answer through the ack.
(defmodule Store
  [setup topologies]
  (declare-depot setup *offers (hash-by :layer))
  (let [s (stream-topology topologies "gate")]
    (declare-pstate s $$layers layers-schema)
    ;; the task's last stamp, a hybrid stamp (rig.store.clock); 0 before the first
    (declare-pstate s $$clock Long {:initial-value 0})
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
          ;; decided before: the recorded answer, or the name is taken; no writes
          (gate/answer-from-record *rec *digest *name :> *d)
          (inject/point! :recorded *name)
          (ack-return> (get *d :ack))
         (else>)
          (local-select> (keypath *layer :settings) $$layers :> *settings)
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
          (gate/decide *offer *settings *rows *heads *clock *wall *digest :> *d)
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
              ;; settings and permission rows as they stand after the act; both were
              ;; read in this event, so each is one write with no read
              (get *d :settings :> *new-settings)
              (<<if (some? *new-settings)
                (local-transform> [(keypath *layer :settings) (termval *new-settings)] $$layers))
              (<<atomic
                (ops/explode (get *d :permissions) :> [*pp *prow])
                (local-transform> [(keypath *layer :permissions *pp) (termval *prow)] $$layers)))
            (local-transform> [(termval (get *d :stamp))] $$clock)
            (inject/point! :after-writes *name))
          (ack-return> (get *d :ack)))))))
