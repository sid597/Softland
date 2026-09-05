(ns app.client.engine.color
  "Declare the two scene color modes.

   Input: channel values or a mode boolean. Output: transfer-function
   results or a mode configuration map. It holds constants, a tagged
   straight-sRGB schema, and two immutable mode descriptions. The legacy
   direct mode is selected for nil/false; linear premultiplied mode is
   explicitly selected with a truthy argument. Declaring both modes here is
   different from activating either in a renderer.

   Folder map: README.md."
  (:require [app.client.engine.schema :as schema]))

(defn- pow
  "Base and exponent → power.

   CLJ/CLJS adapter."
  [base exponent]
  #?(:clj (Math/pow (double base) (double exponent))
     :cljs (js/Math.pow base exponent)))

(def srgb-encoded-cutoff 0.04045)
(def srgb-linear-cutoff 0.0031308)
(def srgb-linear-scale 12.92)
(def srgb-transfer-scale 1.055)
(def srgb-transfer-offset 0.055)
(def srgb-transfer-exponent 2.4)

(defn srgb-channel->linear
  "Encoded channel → linear channel.

   Piecewise transfer function using shared constants. Assumes an
   appropriate input domain rather than clamping."
  [value]
  (let [v (double value)]
    (if (<= v srgb-encoded-cutoff)
      (/ v srgb-linear-scale)
      (pow (/ (+ v srgb-transfer-offset) srgb-transfer-scale)
           srgb-transfer-exponent))))

(defn linear->srgb-channel
  "Linear channel → encoded channel.

   Inverse piecewise transfer. Preserves values outside [0,1] rather than
   imposing output policy."
  [value]
  (let [v (double value)]
    (if (<= v srgb-linear-cutoff)
      (* v srgb-linear-scale)
      (- (* srgb-transfer-scale
            (pow v (/ 1.0 srgb-transfer-exponent)))
         srgb-transfer-offset))))

(def tagged
  {:keys #{:rgba :color-space :alpha-association}
   :validators {:rgba schema/valid-rgba?
                :color-space #{:srgb}
                :alpha-association #{:straight}}})

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

(def scene-color-boundary
  "The candidate scene-color resource is code-real but deliberately default-off.
   Direct presentation remains the byte-identical legacy route until a later
   activation evidence explicitly selects the linear-premultiplied candidate."
  {:scene-color-boundary/version 1
   :default (:scene-color/id legacy-direct-color)
   :candidate (:scene-color/id linear-premultiplied-color)
   :default-off? true})

(defn scene-color
  "Mode flag → one of the two configuration maps.

   Simple explicit selection. The default-off behavior is visible in
   executable code."
  [linear-premultiplied?]
  (if linear-premultiplied?
    linear-premultiplied-color
    legacy-direct-color))
