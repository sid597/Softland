(ns app.client.substrate.scene-tape
  "W2-B's pure render contract and ordered-scene-tape core.

   Family registration owns citizenship, geometry, and color declarations.
   Scene entries own semantic order.  Paint and pick are projections of the
   same compiled tape: paint walks forward, pick walks exact reverse.  This
   namespace is data-only and has no GPU objects, atoms, renderer imports, or
   family-specific ordering branches."
  (:require [app.client.substrate.chrome-material :as chrome-material]
            [app.client.substrate.connector-material :as connector-material]
            [app.client.substrate.path-material :as path-material]
            [app.client.substrate.region3d-material :as region3d-material]))

(def family-ids
  "The complete render-family set. Adding a road changes this registry; it
   never adds a branch to the frame executor."
  [:render.family/msdf
   :render.family/slug
   :render.family/clip
   :render.family/image
   :render.family/path
   :render.family/connector
   :render.family/chrome
   :render.family/region-3d])

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

(defn text-family-id [backend]
  (case backend
    :msdf :render.family/msdf
    :slug :render.family/slug
    (throw (ex-info "Unregistered text backend" {:backend backend}))))

(defn- regime
  [backend precision normalization extent verdict]
  {:zoom {:min 0.01 :max 1000.0}
   :extent extent
   :normalization normalization
   :coordinate-precision precision
   :coverage-precision precision
   :lifecycle :live
   :backend backend
   :verdict verdict})

(defn- geometry
  [{:keys [kind algorithm local-space fill-rule coverage-operator
           boundary-relation visual-factors pick-policy hit-slop owner
           math-bounds paint-bounds pick-bounds derivations regimes]}]
  {:geometry/version 1
   :authority {:kind kind
               :source-id :scene-entry/material-id
               :source-revision :scene-entry/material-revision
               :algorithm-version algorithm
               :local-space local-space}
   :classify {:result #{:inside :boundary :outside}
              :fill-rule fill-rule
              :boundary-rule :explicit}
   :coverage {:geometry-operator coverage-operator
              :operator-version algorithm
              :boundary-relation boundary-relation
              :reference-isocontour (if (= :isocontour-0.5 boundary-relation)
                                      0.5
                                      :not-applicable)
              :visual-factors visual-factors
              :tie-token :half
              :quantization :declared-per-regime}
   :pick {:policy pick-policy
          :boundary (if (= :none pick-policy) :not-applicable :hit)
          :hit-slop {:metric :screen-px :radius hit-slop}
          :owner owner}
   :bounds {:math math-bounds
            :paint paint-bounds
            :pick pick-bounds}
   :derivations derivations
   :regimes regimes})

(def ^:private msdf-geometry
  (geometry
   {:kind :glyph-outline
    :algorithm :dejavu-msdf-atlas-v1
    :local-space :font-outline-positioned-by-layout-result
    :fill-rule :nonzero
    :coverage-operator :msdf-atlas-sample
    :boundary-relation :isocontour-0.5
    :visual-factors [:atlas-sample :material-alpha :effective-opacity]
    :pick-policy :layout-cluster
    :hit-slop 0.0
    :owner :layout-result/cluster-owner
    :math-bounds :font-outline
    :paint-bounds :atlas-plane-bounds
    :pick-bounds :layout-cluster
    :derivations [{:kind :atlas
                   :source-revision :font-asset/digest
                   :algorithm-version :dejavu-msdf-atlas-v1
                   :tolerance-lod :screen-constant
                   :normalization :atlas-plane-bounds
                   :precision :rgba8unorm
                   :backend :webgpu-filtered-msdf
                   :regime :known-negative-provenance}]
    ;; The declaration is unconditional and honest: the existing road remains
    ;; registered while its permanent 47-mismatch counterexample stays RED.
    :regimes [(regime :webgpu-filtered-msdf :rgba8unorm
                      :atlas-plane-bounds :font-glyph-atlas
                      :falsified-47-isocontour-mismatches-per-regime)]}))

(def ^:private slug-geometry
  (geometry
   {:kind :glyph-outline
    :algorithm :slug-bands-v1
    :local-space :font-outline-positioned-by-layout-result
    :fill-rule :nonzero
    :coverage-operator :analytic-band-coverage
    :boundary-relation :isocontour-0.5
    :visual-factors [:analytic-coverage :material-alpha :effective-opacity]
    :pick-policy :layout-cluster
    :hit-slop 0.0
    :owner :layout-result/cluster-owner
    :math-bounds :font-outline
    :paint-bounds :slug-sample-bounds
    :pick-bounds :layout-cluster
    :derivations [{:kind :bands
                   :source-revision :font-asset/digest
                   :algorithm-version :slug-bands-v1
                   :tolerance-lod :font-asset-version
                   :normalization :glyph-bbox-centered
                   :precision :rgba16float
                   :backend :webgpu-unfilterable-float
                   :regime :measured-font-corpus}]
    :regimes [(regime :webgpu-unfilterable-float :rgba16float
                      :glyph-bbox-centered :font-glyph-bounds
                      :measured-corpus-only-q2-boundary-retained)]}))

(def ^:private clip-geometry
  (geometry
   {:kind :quad
    :algorithm :clip-intersection-v1
    :local-space :declared-screen-or-container-space
    :fill-rule :not-applicable
    :coverage-operator :scissor-intersection
    :boundary-relation :not-applicable
    :visual-factors [:shared-visibility :load-op :clear-value]
    :pick-policy :none
    :hit-slop 0.0
    :owner :none
    :math-bounds :clip-quad
    :paint-bounds :clip-intersection
    :pick-bounds :none
    :derivations [{:kind :clip-intersection
                   :source-revision :scene-entry/material-revision
                   :algorithm-version :clip-intersection-v1
                   :tolerance-lod :exact-axis-aligned
                   :normalization :target-pixels
                   :precision :u32-scissor
                   :backend :webgpu-render-pass
                   :regime :legal-zoom}]
    :regimes [(regime :webgpu-render-pass :u32-scissor :target-pixels
                      :render-target :measured-production)]}))

(def ^:private image-regimes
  ;; IMAGE-ATOM T8: the image family consumes the existing f32 affine transport;
  ;; these rows describe that road rather than minting a second transform path.
  [{:zoom {:min 0.01 :max 0.1}
    :extent [:standard-fixture :world-256]
    :normalization :screen-constant
    :coordinate-precision :f32
    :coverage-precision :rgba8unorm
    :lifecycle :settle-to-scene
    :backend :webgpu-sampled-image
    :verdict :asserted-image-parity}
   {:zoom {:min 0.1 :max 8.0}
    :extent [:standard-fixture :world-256]
    :normalization :screen-constant
    :coordinate-precision :f32
    :coverage-precision :rgba8unorm
    :lifecycle :settle-to-scene
    :backend :webgpu-sampled-image
    :verdict :asserted-image-parity}
   {:zoom {:min 8.0 :max 1000.0}
    :extent [:standard-fixture :world-256]
    :normalization :screen-constant
    :coordinate-precision :f32
    :coverage-precision :rgba8unorm
    :lifecycle :settle-to-scene
    :backend :webgpu-sampled-image
    :verdict :asserted-image-parity}])

(def ^:private image-geometry
  {:geometry/version 1
   :authority {:kind :image-quad
               :source-id :scene-entry/material-id
               :source-revision :scene-entry/material-revision
               :algorithm-version :image-quad-crop-v1
               :local-space :container-local-f32}
   :classify {:result #{:inside :boundary :outside}
              :fill-rule :not-applicable
              :boundary-rule :explicit}
   :coverage {:geometry-operator :aa-filter
              :operator-version :image-quad-ramped-edge-v1
              :boundary-relation :isocontour-0.5
              :reference-isocontour 0.5
              :visual-factors [:texture-alpha :effective-opacity]
              :tie-token :half
              :quantization :rgba8unorm}
   :time-sample :none-static
   ;; IMAGE-ATOM T6/T14: image picking uses the half-open quad interior with
   ;; zero slop; per-image transforms would invalidate this row.
   :pick {:policy :interior
          :boundary :half-open-interior
          :hit-slop {:metric :screen-px :radius 0.0}
          :owner :image-node/address}
   :bounds {:math :crop-quad
            :paint :half-pixel-conservative-support
            :pick :crop-quad-half-open}
   ;; IMAGE-ATOM T4/T5: mips are linear-filtered derivations and atlas identity
   ;; includes the source revision, algorithm, and declared regime.
   :derivations [{:kind :mips
                  :source-revision :image-source/digest
                  :algorithm-version :linear-box-mips-v1
                  :tolerance-lod :declared-per-placement-tier
                  :normalization :image-local-pixels
                  :precision :rgba8unorm
                  :backend :webgpu-render-pass
                  :regime :image-regime}
                 {:kind :atlas
                  :source-revision :image-source/digest
                  :algorithm-version :shelf-atlas-v1
                  :tolerance-lod :two-pixel-gutter-lod1
                  :normalization :atlas-uv-inset
                  :precision :f32
                  :backend :webgpu-sampled-image
                  :regime :image-regime}]
   :regimes image-regimes})

(defn- registration
  ([family-id geometry receipts]
   ;; Keep the historical three-argument result byte-for-value identical for
   ;; the five admitted W2-B families (IMAGE-ATOM G1/Q5).
   (registration family-id geometry receipts {}))
  ([family-id geometry receipts
    {:keys [material-fields instance-fields validation defaults
            edit-operations serialization export-projections resources]
     :or {material-fields :legacy-family-adapter
          instance-fields [:instance/id :container-slot :paint-reference]
          validation :scene-tape/fail-closed-v1
          defaults :versioned-explicit
          edit-operations :existing-semantic-operations
          serialization :edn-v1
          export-projections :declared-by-future-exporter
          resources :registered-executor-owned}}]
   {:family/id family-id
   :family/version 1
   :grammar {:schema/version 1
             :material-fields material-fields
             :instance-fields instance-fields
             :validation validation
             :defaults defaults
             :edit-operations edit-operations
             :serialization serialization
             :export-projections export-projections}
   :pick {:geometry :render/geometry
          :scene-order :scene-order/v1
          :visibility :scene-entry/visibility
          :modalities [:pointer :pen :touch :accessibility]
          :result :stable-semantic-identity}
   :provenance {:material-id :scene-entry/material-id
                :revision :scene-entry/material-revision
                :parents :scene-entry/provenance-parents
                :author/actor :scene-entry/provenance-actor
                :act :scene-entry/provenance-act
                :source-assets :scene-entry/source-assets
                :derivations :render/geometry
                :draft-settle :scene-entry/lifecycle}
   :versioning {:schema-version 1
                :algorithm-versions :render/geometry
                :migration :explicit-family-migration-chain
                :unknown-field-policy :reject
                :cache-invalidation :source+algorithm+regime
                :compatibility {:reader-min 1 :reader-max 1}}
   :render {:order :scene-order/v1
            :geometry geometry
            :color-alpha {:material {:color-space :srgb
                                     :alpha-association :straight}
                          :scene (:scene-color/id linear-premultiplied-color)
                          :seam scene-color-seam}
            :resources resources
            :regimes (:regimes geometry)}
   :receipts receipts}))

(def image-registration
  (assoc-in
   (registration
    :render.family/image image-geometry
    [:image-atom/admission :image-atom/geometry :image-atom/color
     :image-atom/resources :image-atom/store-lane]
    {:material-fields [:image/material-id :image/revision :image/source-digest
                       :image/color-tag :image/intrinsic-size :image/provenance]
     :instance-fields [:instance/id :container-slot :image/crop :opacity
                       :paint-reference]
     :validation :image-material/fail-closed-v1
     :defaults :image-material/explicit-v1
     :edit-operations [:image/crop :image/replace-source]
     :serialization :canonical-edn-v1
     :export-projections :none-promised
     :resources :image-system/digest-keyed-lifecycle-v1})
   [:grammar :entry-paint-required-keys]
   [:paint/source :paint/source-type :op-offset :instance-count]))

(def path-registration
  (assoc-in
   (registration
    :render.family/path path-material/geometry-declaration
    [:path-atom/admission :path-atom/geometry :path-atom/color
     :path-atom/resources :path-atom/store-lane :path-atom/product-pick]
    {:material-fields [:path/material-id :path/revision :path/kind
                       :path/geometry :path/paint :path/provenance]
     :instance-fields [:instance/id :container-slot :path/material
                       :path/local-origin]
     :validation :path-material/fail-closed-v1
     :defaults :path-material/explicit-v1
     :edit-operations [:path/move-knot :path/set-knot-pressure
                       :path/move-contour-point :path/set-paint
                       :path/replace-contours]
     :serialization :canonical-edn-v1
     :export-projections :none-promised
     :resources :path-system/content-keyed-mesh-lifecycle-v1})
   [:grammar :entry-paint-required-keys]
   [:vertex-count]))

(def connector-registration
  (assoc-in
   (registration
    :render.family/connector connector-material/geometry-declaration
    [:connector-atom/admission :connector-atom/geometry
     :connector-atom/color :connector-atom/resources
     :connector-atom/store-lane :connector-atom/product-pick
     :connector-atom/durable-r1-read]
    {:material-fields [:connector/relation-id :connector/row-stamp
                       :connector/dress-revision :connector/kind
                       :connector/from :connector/to :connector/route
                       :connector/heads :connector/label :connector/paint
                       :connector/status :connector/provenance]
     :instance-fields [:instance/id :container-slot
                       :connector/edge-instance-id]
     :validation :connector-material/fail-closed-v1
     :defaults :connector-material/relation-row-plus-session-dress-v1
     :edit-operations [:connector/set-binding :connector/set-route
                       :connector/set-waypoints :connector/set-heads
                       :connector/set-label :connector/set-paint]
     :serialization :canonical-edn-v1
     :export-projections :none-promised
     :resources :connector-system/bounded-current-route-lifecycle-v1})
   [:grammar :entry-paint-required-keys]
   [:vertex-count]))

(def chrome-registration
  (->
   (registration
    :render.family/chrome chrome-material/geometry-declaration
    [:chrome-atom/admission :chrome-atom/geometry :chrome-atom/color
     :chrome-atom/resources :chrome-atom/store-lane
     :chrome-atom/session-only-absence]
    {:material-fields [:chrome/form :chrome/anchor-bounds
                       :chrome/derived-from :chrome/selection-rev
                       :chrome/pick :chrome/corner :chrome/gesture-id]
     :instance-fields [:instance/id :container-slot :stratum
                       :chrome/form-identity]
     :validation :chrome-material/fail-closed-v1
     :defaults :chrome-material/explicit-v1
     :edit-operations [:selection/toggle :selection/marquee-commit
                       :selection/clear :arrangement/translate
                       :arrangement/uniform-scale]
     :serialization :none-session-truth
     :export-projections :none-by-design
     :resources :chrome-system/value-keyed-buffer-lifecycle-v1})
   (assoc :provenance {:kind :derived-session-chrome
                       :source :target-identity+bounds+selection-revision
                       :durable-rows :none-by-design}
          :versioning {:schema-version 1
                       :algorithm-versions chrome-material/algorithm-version
                       :migration :none-ephemeral
                       :unknown-field-policy :reject
                       :cache-invalidation :source+algorithm+selection-revision
                       :compatibility {:reader-min 1 :reader-max 1}})
   (assoc-in [:grammar :entry-paint-required-keys] [:vertex-count])))

(def family-contracts
  [(registration :render.family/msdf msdf-geometry
                 [:w0-a/msdf-47-mismatch-counterexample])
   (registration :render.family/slug slug-geometry
                 [:w0-a/slug :w2-a/q8])
   (registration :render.family/clip clip-geometry
                 [:w2-b/shared-visibility])
   image-registration
   path-registration
   connector-registration
   chrome-registration
   region3d-material/region-family-registration])

(def ^:private required-family-keys
  [:family/id :family/version :grammar :pick :provenance :versioning :render
   :receipts])

(def ^:private required-geometry-keys
  [:geometry/version :authority :classify :coverage :pick :bounds :derivations
   :regimes])

(def ^:private required-regime-keys
  [:zoom :extent :normalization :coordinate-precision :coverage-precision
   :lifecycle :backend :verdict])

(defn- require-keys! [label m ks]
  (let [missing (filterv #(not (contains? m %)) ks)]
    (when (seq missing)
      (throw (ex-info (str label " is incomplete")
                      {:label label :missing missing :value m}))))
  m)

(defn- validate-regimes! [family-id regimes]
  (when-not (seq regimes)
    (throw (ex-info "Geometry must declare at least one legal regime"
                    {:family/id family-id})))
  (doseq [r regimes]
    (require-keys! "geometry regime" r required-regime-keys)
    (let [{:keys [min max]} (:zoom r)]
      (when-not (and (number? min) (number? max) (< min max))
        (throw (ex-info "Geometry regime has an invalid zoom interval"
                        {:family/id family-id :regime r})))))
  (let [ordered (sort-by #(get-in % [:zoom :min]) regimes)
        first-min (get-in (first ordered) [:zoom :min])
        last-max (get-in (last ordered) [:zoom :max])]
    (when-not (= 0.01 first-min)
      (throw (ex-info "Geometry regimes must begin at legal zoom 0.01"
                      {:family/id family-id :first-min first-min})))
    (when-not (= 1000.0 last-max)
      (throw (ex-info "Geometry regimes must end at legal zoom 1000"
                      {:family/id family-id :last-max last-max})))
    (doseq [[left right] (partition 2 1 ordered)]
      (when-not (= (get-in left [:zoom :max])
                   (get-in right [:zoom :min]))
        (throw (ex-info "Geometry regimes leave a gap or overlap"
                        {:family/id family-id :left left :right right})))))
  regimes)

(defn validate-family!
  "Fail closed unless a family unconditionally supplies Contract-M citizenship,
   Contract-G geometry (including Q2 regimes), and Contract-C tags."
  [family]
  (require-keys! "family registration" family required-family-keys)
  (let [family-id (:family/id family)
        geometry (get-in family [:render :geometry])
        color-alpha (get-in family [:render :color-alpha])]
    (when-not (some #{family-id} family-ids)
      (throw (ex-info "Family is outside the W2-B admission set"
                      {:family/id family-id :admitted family-ids})))
    (require-keys! "geometry declaration" geometry required-geometry-keys)
    (require-keys! "geometry authority" (:authority geometry)
                   [:kind :source-id :source-revision :algorithm-version
                    :local-space])
    (require-keys! "geometry coverage" (:coverage geometry)
                   [:geometry-operator :operator-version :boundary-relation
                    :reference-isocontour :visual-factors :tie-token
                    :quantization])
    (require-keys! "geometry pick" (:pick geometry)
                   [:policy :boundary :hit-slop :owner])
    (validate-regimes! family-id (:regimes geometry))
    (when-not (= (:scene-color/id linear-premultiplied-color)
                 (:scene color-alpha))
      (throw (ex-info "Family lacks the tagged linear-premultiplied scene-color seam"
                      {:family/id family-id :color-alpha color-alpha}))))
  family)

(defn register-family
  "Pure, fail-closed family registration.  Registration order is never scene
   order; duplicate family ids are rejected rather than silently replaced."
  [registry family]
  (let [family (validate-family! family)
        family-id (:family/id family)]
    (when (contains? registry family-id)
      (throw (ex-info "Duplicate render family registration"
                      {:family/id family-id})))
    (assoc registry family-id family)))

(defn register-families [families]
  (reduce register-family {} families))

(def default-family-registry
  (register-families family-contracts))

(def ^:private stratum-rank
  {:world 0 :overlay 1 :region-composite 2})

(def ^:private pass-rank
  {:frame-policy 0 :direct 1 :intermediate 2 :region 3 :present 4})

;; W4 additive plan vocabulary. These are data declarations consumed by the
;; frame compiler and verifier; they do not dispatch families or effects.
(def frame-pass-kinds
  #{:render :copy :present :readback :region})

(def frame-resource-kinds
  #{:color :coverage :depth :external-swap :buffer})

(def frame-resource-lifetimes
  #{:frame :held :external :export :readback})

(def frame-clip-modes
  #{:scissor :mask})

(defn compare-scalar [a b]
  (cond
    (= a b) 0
    (and (number? a) (number? b)) (compare a b)
    :else (compare (pr-str a) (pr-str b))))

(defn compare-seq [xs ys item-compare]
  (loop [xs (seq xs) ys (seq ys)]
    (cond
      (and (nil? xs) (nil? ys)) 0
      (nil? xs) -1
      (nil? ys) 1
      :else (let [c (item-compare (first xs) (first ys))]
              (if (zero? c)
                (recur (next xs) (next ys))
                c)))))

(defn- compare-stack-node [left right]
  ;; W2-A supplied [context-id layer], and W2-B accepts the Contract-O
  ;; [context-id layer sibling-rank] extension without inventing another path.
  ;; Context identity is a final deterministic tie only; layer/sibling carry
  ;; semantic order.
  (let [[lc ll lsr] left
        [rc rl rsr] right
        by-layer (compare-scalar (or ll 0) (or rl 0))
        by-sibling (compare-scalar (or lsr 0) (or rsr 0))]
    (cond
      (not (zero? by-layer)) by-layer
      (not (zero? by-sibling)) by-sibling
      :else (compare-scalar lc rc))))

(defn- compare-stack-path [left right]
  (compare-seq left right compare-stack-node))

(defn compare-order [left right]
  (let [lo (:order left)
        ro (:order right)
        comparisons [(compare-scalar (get stratum-rank (:stratum lo))
                                     (get stratum-rank (:stratum ro)))
                     (compare-scalar (get pass-rank (:pass-class lo))
                                     (get pass-rank (:pass-class ro)))
                     (compare-stack-path (:stack-path lo) (:stack-path ro))
                     (compare-scalar (:part-rank lo) (:part-rank ro))
                     (compare-scalar (:stable-tie lo) (:stable-tie ro))]]
    (or (some #(when-not (zero? %) %) comparisons) 0)))

(defn validate-entry! [registry entry]
  (require-keys! "scene entry" entry
                 [:entry/id :material/id :material/revision :instance/id
                  :family/id :order :paint :pick :visibility])
  (let [family-id (:family/id entry)
        family (get registry family-id)
        required-paint-keys (get-in family
                                    [:grammar :entry-paint-required-keys])
        {:keys [stratum pass-class stack-path part-rank stable-tie]}
        (:order entry)]
    (when-not (contains? registry (:family/id entry))
      (throw (ex-info "Scene entry names an unregistered family"
                      {:entry/id (:entry/id entry)
                       :family/id (:family/id entry)})))
    ;; IMAGE-ATOM T1: family-specific entry shape is registration data.  The
    ;; validator applies declared keys generically and gains no image branch.
    (when (seq required-paint-keys)
      (require-keys! "family paint" (:paint entry) required-paint-keys))
    (when-not (contains? stratum-rank stratum)
      (throw (ex-info "Scene entry has an unknown stratum"
                      {:entry/id (:entry/id entry) :stratum stratum})))
    (when-not (contains? pass-rank pass-class)
      (throw (ex-info "Scene entry has an unknown pass class"
                      {:entry/id (:entry/id entry) :pass-class pass-class})))
    (when-not (and (vector? stack-path)
                   (every? #(and (vector? %) (<= 2 (count %) 3)) stack-path))
      (throw (ex-info "Scene entry must consume a canonical nested stack path"
                      {:entry/id (:entry/id entry) :stack-path stack-path})))
    (when-not (integer? part-rank)
      (throw (ex-info "Scene entry :part-rank must be an integer"
                      {:entry/id (:entry/id entry) :part-rank part-rank})))
    (when (nil? stable-tie)
      (throw (ex-info "Scene entry requires a stable tie token"
                      {:entry/id (:entry/id entry)}))))
  entry)

(defn entry-key-compare
  "Comparator for maintained ordered-view keys `[order-token entry-id]`.
   Contract-O tokens use this namespace's semantic comparison; entry identity
   is the final deterministic tie."
  [[left-order left-id] [right-order right-id]]
  (let [order-comparison (compare-order {:order left-order}
                                        {:order right-order})
        ;; SEAM-STEP1 T1: default vector compare is length-first and therefore
        ;; is not Contract-O ordering. Scalar ids are lifted to one-item paths
        ;; so frame keywords and store vector ids share this comparator.
        id-path (fn [entry-id]
                  (if (sequential? entry-id) entry-id [entry-id]))]
    (if (zero? order-comparison)
      (compare-seq (id-path left-id) (id-path right-id) compare-scalar)
      order-comparison)))

(defn entry-key [entry]
  [(:order entry) (:entry/id entry)])

(defn ordered-insert
  "Validate and insert one entry into a persistent maintained ordered view."
  [registry ordered entry]
  ;; SEAM-STEP1 T6: insert-edge validation is additive; compile-tape below
  ;; retains its independent validation and remains the batch oracle.
  (validate-entry! registry entry)
  (assoc ordered (entry-key entry) entry))

(defn ordered-remove
  "Remove one entry's exact order/id key from a maintained ordered view."
  [ordered entry]
  (dissoc ordered (entry-key entry)))

(defn- order-receipt [entries]
  (mapv (fn [entry] [(:entry/id entry) (:family/id entry) (:order entry)]) entries))

(defn compile-tape
  "Validate and compile one immutable ordered tape.  Input order and family
   registration order are irrelevant; only Contract-O order tokens sort it."
  ([world-revision entries]
   (compile-tape default-family-registry world-revision entries))
  ([registry world-revision entries]
   (let [entries (mapv #(validate-entry! registry %) entries)
         duplicate-ids (->> entries
                            (group-by :entry/id)
                            (keep (fn [[entry-id xs]]
                                    (when (< 1 (count xs)) entry-id)))
                            vec)]
     (when (seq duplicate-ids)
       (throw (ex-info "Scene tape entry ids must be unique"
                       {:duplicates duplicate-ids})))
     (let [ordered (vec (sort compare-order entries))
           receipt (order-receipt ordered)]
       {:scene-order/version 1
        :world/revision world-revision
        :entries ordered
        ;; The receipt is the canonical preimage, not a process/runtime hash.
        ;; This stays identical in CLJ and CLJS and is directly inspectable.
        :order-hash (str "scene-order-v1/" (pr-str receipt))}))))

(defn paint-forward
  "Execute the paint projection in exact tape order.  `execute!` is the declared
   family executor dispatch; this function contains no family branch."
  [tape execute!]
  (mapv execute! (:entries tape)))

(defn- effectively-visible? [entry]
  (not (false? (get-in entry [:visibility :visible?] true))))

(defn pick-reverse
  "Filter to visible, pickable entries and traverse the exact reverse tape.
   `hit` returns nil for a miss or any resolved hit value for a hit."
  [tape hit]
  (loop [entries (rseq (:entries tape))]
    (when-let [entry (first entries)]
      (if (and (effectively-visible? entry)
               (not= :none (:pick entry)))
        (if-let [resolved (hit entry)]
          {:entry entry :hit resolved}
          (recur (next entries)))
        (recur (next entries))))))
