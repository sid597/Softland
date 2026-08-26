(ns app.client.substrate.scene-tape
  "Pure render-family admission and ordered-scene-tape core.

   Family registration carries only the paint shape consumed by entry
   validation. Scene entries own semantic order. Paint and pick are projections
   of the same compiled tape: paint walks forward, pick walks exact reverse.")

(def family-ids
  "The complete render-family set. Adding a road changes this registry; it
   never adds a branch to the frame executor."
  [:render.family/msdf
   :render.family/slug
   :render.family/clip
   :render.family/image
   :render.family/path
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

(defn- registration [family-id required-paint-keys]
  {:family/id family-id
   :entry-paint-required-keys required-paint-keys})

(def family-contracts
  [(registration :render.family/msdf [])
   (registration :render.family/slug [])
   (registration :render.family/clip [])
   (registration :render.family/image
                 [:paint/source :paint/source-type :op-offset :instance-count])
   (registration :render.family/path [:vertex-count])
   (registration :render.family/chrome [:vertex-count])
   (registration :render.family/region-3d [:region-id :resolve-view])])

(def ^:private required-family-keys
  [:family/id :entry-paint-required-keys])

(defn- require-keys! [label m ks]
  (let [missing (filterv #(not (contains? m %)) ks)]
    (when (seq missing)
      (throw (ex-info (str label " is incomplete")
                      {:label label :missing missing :value m}))))
  m)

(defn validate-family!
  "Fail closed unless a registration contains exactly the fields read at run
   time: its admitted family id and the required entry-paint keys."
  [family]
  (require-keys! "family registration" family required-family-keys)
  (let [family-id (:family/id family)
        unknown (seq (remove (set required-family-keys) (keys family)))]
    (when-not (some #{family-id} family-ids)
      (throw (ex-info "Family is outside the W2-B admission set"
                      {:family/id family-id :admitted family-ids})))
    (when unknown
      (throw (ex-info "Family registration contains unread fields"
                      {:family/id family-id :unknown (vec unknown)})))
    (when-not (and (vector? (:entry-paint-required-keys family))
                   (every? keyword? (:entry-paint-required-keys family)))
      (throw (ex-info "Family paint requirements must be a keyword vector"
                      {:family/id family-id
                       :entry-paint-required-keys
                       (:entry-paint-required-keys family)}))))
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
        required-paint-keys (:entry-paint-required-keys family)
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
