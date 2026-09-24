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
            [rig.store.envelope :as env]
            [rig.store.gate :as gate]
            [rig.store.inject :as inject]))

(def layers-schema
  "Everything keyed by a one-owner layer, on the layer's home task
  (PLAN-stream-store.md, `$$layers`)."
  {clojure.lang.Keyword
   (fixed-keys-schema
    {:settings    (fixed-keys-schema {:kind  clojure.lang.Keyword
                                      :owner clojure.lang.Keyword
                                      :class clojure.lang.Keyword
                                      :grain clojure.lang.Keyword})
     :answers     (map-schema clojure.lang.PersistentVector
                              (fixed-keys-schema
                               {:answer       clojure.lang.Keyword
                                :reason       clojure.lang.Keyword
                                :stamp        Long
                                :digest       String
                                :who          clojure.lang.Keyword
                                :class        clojure.lang.Keyword
                                :permission   clojure.lang.PersistentVector
                                :session      clojure.lang.Keyword
                                :stood-on     (map-schema clojure.lang.PersistentVector Long)
                                :because-of   clojure.lang.PersistentVector
                                :claimed-when Long
                                :subjects     (set-schema clojure.lang.Keyword)})
                              {:subindex-options {:track-size? false}})
     :log         (map-schema clojure.lang.PersistentVector
                              (vector-schema
                               (fixed-keys-schema {:e        clojure.lang.Keyword
                                                   :k        clojure.lang.Keyword
                                                   :v        String
                                                   :replaces clojure.lang.PersistentVector
                                                   :mark     (set-schema clojure.lang.Keyword)}))
                              {:subindex-options {:track-size? false}})
     :heads       (map-schema clojure.lang.PersistentVector Long
                              {:subindex-options {:track-size? false}})
     :permissions (map-schema clojure.lang.PersistentVector
                              (fixed-keys-schema {:granted clojure.lang.PersistentVector
                                                  :revoked clojure.lang.PersistentVector})
                              {:subindex-options {:track-size? false}})})})

(defmodule Store
  [setup topologies]
  (declare-depot setup *offers (hash-by :layer))
  (let [s (stream-topology topologies "gate")]
    (declare-pstate s $$layers layers-schema)
    (declare-pstate s $$clock Long {:initial-value 0})
    (<<sources s
      (source> *offers {:retry-mode :all-after} :> *raw)
      (env/parse *raw :> *parsed)
      (<<if (contains? *parsed :refuse)
        ;; refused on its face: answered through the ack, recorded nowhere (P7)
        (ack-return> (gate/face-ack *parsed *raw))
       (else>)
        (get *parsed :ok :> *offer)
        (get *offer :layer :> *layer)
        (get *offer :name :> *name)
        (env/digest *offer :> *digest)
        (local-select> (keypath *layer :answers *name) $$layers :> *rec)
        (<<if (some? *rec)
          ;; decided before: the recorded answer, or the name is taken; no writes
          (gate/answer-from-record *rec *digest *name :> *d)
          (ack-return> (get *d :ack))
         (else>)
          (local-select> (keypath *layer :settings) $$layers :> *settings)
          (local-select> STAY $$clock :> *clock)
          (gate/wall-now :> *wall)
          (gate/pids-to-read *offer :> *pids)
          (loop<- [*todo *pids *acc {} :> *rows]
            (<<if (empty? *todo)
              (:> *acc)
             (else>)
              (first *todo :> *pid)
              (local-select> (keypath *layer :permissions *pid) $$layers :> *row)
              (continue> (rest *todo) (assoc *acc *pid *row))))
          (gate/heads-to-read *offer :> *hkeys)
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
              (local-transform> [(keypath *layer :log *name) (termval (get *d :log))] $$layers)
              (<<atomic
                (ops/explode (get *d :heads-del) :> *hk)
                (local-transform> [(keypath *layer :heads *hk) NONE>] $$layers))
              (<<atomic
                (ops/explode (get *d :heads-put) :> [*hk *hs])
                (local-transform> [(keypath *layer :heads *hk) (termval *hs)] $$layers))
              (<<atomic
                (ops/explode-map (get *d :settings) :> *sk *sv)
                (local-transform> [(keypath *layer :settings *sk) (termval *sv)] $$layers))
              (<<atomic
                (ops/explode (get *d :grants) :> [*gp *gf])
                (local-transform> [(keypath *layer :permissions *gp :granted) (termval *gf)] $$layers))
              (<<atomic
                (ops/explode (get *d :revokes) :> [*rp *rf])
                (local-transform> [(keypath *layer :permissions *rp :revoked) (termval *rf)] $$layers)))
            (local-transform> [(termval (get *d :stamp))] $$clock)
            (inject/point! :after-writes *name))
          (ack-return> (get *d :ack)))))))
