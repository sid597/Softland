(ns app.server.worn.facet-masters
  "Static, ordered registry of shared facet-master specifications.
   Supplies master-id and facet lookups plus the common code-floor label for
   projections and wear resolution. The order is deliberate: provenance is the
   first entry. Instance specs are synthesized separately by facet-engine.

   Owns immutable registry values only. Requiring this namespace neither imports
   source nor activates a master; bootstrap/write callers must use facet-master.
   A registry entry establishes availability, not a running presentation path."
  (:require [app.server.worn.facet-engine :as facet-engine]
            [app.server.worn.attention-material :as attention]
            [app.server.worn.foldable-material :as foldable]
            [app.server.worn.invocation-material :as invocation]
            [app.server.worn.positioned-material :as positioned]
            [app.server.worn.provenance-material :as provenance]
            [app.server.worn.space-material :as space]
            [app.server.worn.text-body-material :as text-body]
            [app.server.worn.threaded-material :as threaded]))

(def specs
  [provenance/spec
   attention/spec
   foldable/spec
   positioned/spec
   space/spec
   threaded/spec
   text-body/spec
   ;; Order-bearing registry: append Workshop masters so the historical
   ;; drill fallback remains provenance (T11 / CONTRACT §0).
   invocation/spec])

(def by-id
  (into {} (map (juxt :facet-master/id identity)) specs))

(def master-ids
  (mapv :facet-master/id specs))

(def by-facet
  (into {} (map (juxt :facet-master/facet identity)) specs))

(defn spec-for-facet
  "Look up a shared spec by facet keyword; return nil for an unregistered facet."
  [facet]
  (get by-facet facet))

(defn spec
  "Look up a shared spec by master id; synthesized instance ids are not registered."
  [master-id]
  (get by-id master-id))

(def floor-master-id-by-facet
  "Facet -> the common code-floor revision label used for deciding-master fields.
   Deriving this from specs keeps served tables and pure resolution consistent."
  (into {}
        (map (juxt :facet-master/facet facet-engine/floor-master-id))
        specs))

(defn floor-master-id
  "Return the registered facet's common fallback label, or nil if unknown."
  [facet]
  (get floor-master-id-by-facet facet))
