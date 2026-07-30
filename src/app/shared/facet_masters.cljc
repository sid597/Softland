(ns app.shared.facet-masters
  "The worn facet-master registry. This is a compiler/serve registry, not a
   recipe: it names independently revisioned masters and nothing about which
   entities are legal or which combination constitutes a type."
  (:require [app.shared.facet-material :as facet-material]
            [app.shared.anatomy-material :as anatomy]
            [app.shared.attention-material :as attention]
            [app.shared.foldable-material :as foldable]
            [app.shared.positioned-material :as positioned]
            [app.shared.provenance-material :as provenance]
            [app.shared.space-material :as space]
            [app.shared.text-body-material :as text-body]
            [app.shared.threaded-material :as threaded]))

(def specs
  [provenance/spec
   attention/spec
   foldable/spec
   positioned/spec
   space/spec
   threaded/spec
   text-body/spec
   ;; Order-bearing registry: append the Workshop master so the historical
   ;; drill fallback remains provenance (T11 / CONTRACT §0).
   anatomy/spec])

(def by-id
  (into {} (map (juxt :facet-master/id identity)) specs))

(def master-ids
  (mapv :facet-master/id specs))

(def by-facet
  (into {} (map (juxt :facet-master/facet identity)) specs))

(defn spec-for-facet
  "P6 · R2 — the instance tier is keyed by FACET (a subject deviates from a
   facet, not from a master-id it has never seen), so resolution needs this
   direction of the registry too."
  [facet]
  (get by-facet facet))

(defn spec
  [master-id]
  (get by-id master-id))

(def floor-master-id-by-facet
  "facet → the ONE id a FLOOR row names as its deciding master.

   G14 (P5 gate finding 3): the served projection emitted
   `code-floor:fm:attention:v1` while the client tiers emitted
   `code-floor:attention` and a nil revision — same row, two labels, so a
   reader comparing client and server had to know which side to believe.
   Cosmetic, but the whole point of the served table is that it answers
   `who decided that?`. Both sides now read the label from here."
  (into {}
        (map (juxt :facet-master/facet facet-material/floor-master-id))
        specs))

(defn floor-master-id
  [facet]
  (get floor-master-id-by-facet facet))
