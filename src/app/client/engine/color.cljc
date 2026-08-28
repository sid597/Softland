(ns app.client.engine.color
  "The scene-color resource seam: the byte-identical legacy direct-present
   route and the linear-premultiplied candidate that painter pipelines and
   blend states are configured from.")

(def legacy-direct-color
  {:scene-color/version 1
   :scene-color/id :scene-color/legacy-direct
   :enabled? false
   :resource :direct-present
   :working-space :presentation-encoded
   :alpha-association :straight
   :transfer :legacy-none
   :blend {:color [:src-alpha :one-minus-src-alpha]
           :alpha [:src-alpha :one-minus-src-alpha]}
   :clear [0.0 0.0 0.0 1.0]})

(def linear-premultiplied-color
  {:scene-color/version 1
   :scene-color/id :scene-color/linear-premultiplied-srgb
   :enabled? true
   :resource :direct-or-intermediate
   :working-space :linear-srgb
   :alpha-association :premultiplied
   :ingress-transfer :srgb-to-linear-once
   :presentation-transfer :linear-to-output-once
   :blend {:color [:one :one-minus-src-alpha]
           :alpha [:one :one-minus-src-alpha]}
   :clear [0.0 0.0 0.0 0.0]})

(def scene-color-seam
  "The candidate Contract-C resource is code-real but deliberately default-off.
   Direct presentation remains the byte-identical legacy route until a later
   activation receipt explicitly selects the linear-premultiplied candidate."
  {:scene-color-seam/version 1
   :default (:scene-color/id legacy-direct-color)
   :candidate (:scene-color/id linear-premultiplied-color)
   :default-off? true})

(defn scene-color
  "Resolve the declared scene-color resource.  False/nil is the zero-diff
   default; true selects the tagged linear-premultiplied candidate."
  [linear-premultiplied?]
  (if linear-premultiplied?
    linear-premultiplied-color
    legacy-direct-color))
