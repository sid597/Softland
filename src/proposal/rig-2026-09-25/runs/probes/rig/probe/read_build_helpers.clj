(ns rig.probe.read-build-helpers
  "Install functions for the read exit build's probe (read_build_probe.clj):
  a depot, a second <<sources call on the module's stream topology, and
  query topologies, each declared from a function in another namespace than
  the module, as rig.store.reads will."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.ops :as ops]))

(defn describe [sub clock]
  {:class (str (class sub)) :keys (vec (keys sub)) :clock clock})

(defn page-keys [sub] (vec (keys sub)))

(defn next-from [k] (str k "\u0000"))

(defn boom! [x]
  (if (= x :boom) (throw (ex-info "probe: a query that throws" {:x x})) x))

(defn declare-ops-depot! [setup]
  (declare-depot setup *ops (hash-by :layer)))

(defn add-ops-source! [s]
  (<<sources s
    (source> *ops {:retry-mode :all-after} :> *raw)
    (get *raw :layer :> *layer)
    (get *raw :field :> *field)
    (get *raw :n :> *n)
    (local-select> [(keypath *layer *field) (sorted-map-range-from-start *n)] $$layers :> *sub)
    (page-keys *sub :> *ks)
    (<<atomic
      (ops/explode *ks :> *k)
      (local-transform> [(keypath *layer *field *k) NONE>] $$layers))
    (count *ks :> *c)
    (ack-return> {:deleted *c})))

(defn declare-queries! [topologies]
  (<<query-topology topologies "page" [*layer *field *from *n :> *out]
    (|hash *layer)
    (local-select> [(keypath *layer *field) (sorted-map-range-from *from *n)] $$layers {:allow-yield? true} :> *sub)
    (local-select> STAY $$clock :> *clock)
    (describe *sub *clock :> *out)
    (|origin))
  (<<query-topology topologies "tail" [*layer *field *end :> *out]
    (|hash *layer)
    (local-select> [(keypath *layer *field) (sorted-map-range-to *end {:max-amt 1})] $$layers :> *sub)
    (describe *sub 0 :> *out)
    (|origin))
  (<<query-topology topologies "row" [*layer *nm *idx :> *out]
    (|hash *layer)
    (local-select> [(keypath *layer :log *nm *idx)] $$layers :> *out)
    (|origin))
  (<<query-topology topologies "walk" [*layer *field *page :> *out]
    (|hash *layer)
    (loop<- [*from "" *acc [] :> *all]
      (yield-if-overtime)
      (local-select> [(keypath *layer *field) (sorted-map-range-from *from *page)] $$layers {:allow-yield? true} :> *sub)
      (page-keys *sub :> *ks)
      (<<if (< (count *ks) *page)
        (:> (into *acc *ks))
       (else>)
        (next-from (last *ks) :> *nf)
        (continue> *nf (into *acc *ks))))
    (identity *all :> *out)
    (|origin))
  (<<query-topology topologies "boom" [*x :> *out]
    (|hash *x)
    (boom! *x :> *out)
    (|origin)))
