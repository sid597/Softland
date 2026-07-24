(ns app.shared.facet-masters
  "The worn facet-master registry. This is a compiler/serve registry, not a
   recipe: it names independently revisioned masters and nothing about which
   entities are legal or which combination constitutes a type."
  (:require [app.shared.attention-material :as attention]
            [app.shared.foldable-material :as foldable]
            [app.shared.positioned-material :as positioned]
            [app.shared.provenance-material :as provenance]
            [app.shared.text-body-material :as text-body]
            [app.shared.threaded-material :as threaded]))

(def specs
  [provenance/spec
   attention/spec
   foldable/spec
   positioned/spec
   threaded/spec
   text-body/spec])

(def by-id
  (into {} (map (juxt :facet-master/id identity)) specs))

(def master-ids
  (mapv :facet-master/id specs))

(defn spec
  [master-id]
  (get by-id master-id))
