(ns app.client.substrate.frame-inputs
  "Declared semantic inputs and receipts for the retained frame edge.

   This namespace is deliberately pure-loadable: it knows no DOM or GPU.  The
   renderer supplies current values; this namespace owns the declaration
   fence, change ladder, camera quantization doors, and the live ledger."
  (:require [clojure.set :as set]))

(def region-lease-quant 256)
(def region-lease-max 4096)
(def region-encode-step 1.12)

(def quantization-doors
  {:path-zoom-regime
   {:version :frame-input/path-zoom-regime-v1
    :input-key :path-zoom-regime}
   :connector-zoom-regime
   {:version :frame-input/connector-zoom-regime-v1
    :input-key :connector-zoom-regime}
   :region-lease-allocation
   {:version :frame-input/region-lease-allocation-v1
    :input-key :region-lease-size
    :quant region-lease-quant :max region-lease-max}
   :region-interior-encode
   {:version :frame-input/region-interior-encode-v1
    :input-key :region-encode-scale
    :step region-encode-step}})

(def forbidden-declared-inputs
  #{:frame-idx :pan-x :pan-y :zoom :pixel-size})

(def family-input-declarations
  {:render.family/rect
   #{:rects :ordered-vis :ops-count-by-vi :order-by-vi :rect-clips-by-vi
     :editor-pool-info :editor-rect-count :sidebar-pool-info
     :cmd-rect-sys :cmd-rect-sys-token :cmd-panel-visible
     :settings-rect-sys :settings-rect-sys-token :settings-visible
     :agent-visible :chrome-text-sys :chrome-text-sys-token}

   :render.family/shadow
   #{:shadows :ordered-vis :ops-count-by-vi :order-by-vi
     :editor-shadow-pool-info :editor-shadow-count
     :sidebar-shadow-pool-info}

   :render.family/msdf
   #{:text-sys :text-sys-token :extra-text-geos
     :chrome-text-sys :chrome-text-sys-token :chrome-base-line-count
     :settings-line-count :settings-visible :diagnostics-visible
     :diagnostics-line-index :cmd-panel-visible :font-provider-token}

   :render.family/slug
   #{:text-sys :text-sys-token :extra-text-geos
     :chrome-text-sys :chrome-text-sys-token :chrome-base-line-count
     :settings-line-count :settings-visible :diagnostics-visible
     :diagnostics-line-index :cmd-panel-visible :font-provider-token}

   :render.family/clip
   #{:partial? :clear-quad :dirty-rect}

   :render.family/image
   #{:images :ordered-vis :ops-count-by-vi :order-by-vi
     :image-system :image-system-token}

   :render.family/path
   #{:paths :ordered-vis :ops-count-by-vi :order-by-vi
     :path-system :path-system-token :path-zoom-regime}

   :render.family/connector
   #{:connectors :ordered-vis :order-by-vi
     :connector-system :connector-system-token :connector-zoom-regime}

   :render.family/chrome
   #{:chromes :ordered-vis :ops-count-by-vi :order-by-vi
     :chrome-system :chrome-system-token
     :diagnostics-visible :agent-visible}

   :render.family/region-3d
   #{:regions :order-by-vi :region3d-system :region3d-system-token}})

(def family-ids (set (keys family-input-declarations)))

(defn validate-declarations!
  ([] (validate-declarations! family-input-declarations quantization-doors))
  ([declarations doors]
   (let [declared (reduce set/union #{} (vals declarations))
         raw-camera (set/intersection declared forbidden-declared-inputs)
         door-inputs (set (map :input-key (vals doors)))
         camera-door-inputs (set/intersection declared
                                              #{:path-zoom-regime
                                                :connector-zoom-regime
                                                :region-lease-size
                                                :region-encode-scale})
         unregistered (set/difference camera-door-inputs door-inputs)]
     (when (seq raw-camera)
       (throw (ex-info "Raw camera input declared at semantic frame edge"
                       {:inputs raw-camera})))
     (when (seq unregistered)
       (throw (ex-info "Camera-derived input has no registered quantization door"
                       {:inputs unregistered})))
     true)))

(validate-declarations!)

(defn declared-inputs [family-id inputs]
  (let [declaration (get family-input-declarations family-id)]
    (when-not declaration
      (throw (ex-info "Frame family has no input declaration"
                      {:family/id family-id})))
    (select-keys inputs declaration)))

(defn input-value-same?
  "The three-tier ladder's first two tiers: identity, then declared value."
  [prior current]
  (or (identical? prior current)
      ;; Immutable system maps are carriers for the current payload.  Their
      ;; stable identity is semantic here; the separately captured system token
      ;; carries shape-rev.  Pool draw-info snapshots carry the revision inline.
      (and (map? prior) (map? current)
           (:frame-input/identity prior)
           (:frame-input/identity current)
           (identical? (:frame-input/identity prior)
                       (:frame-input/identity current))
           (= (:frame-input/shape-rev prior)
              (:frame-input/shape-rev current)))
      (= prior current)))

(defn assert-twin-equal!
  "Compare-only retained/batch fence, kept pure so the lying-prepare class is
   executable without a GPU.  Returns true or throws; it never repairs live state."
  [maintained batch projected-count]
  (let [entry-equality? (= maintained batch)
        projected-length-equality? (= projected-count (count maintained))]
    (when-not (and entry-equality? projected-length-equality?)
      (throw (ex-info "Maintained frame arrangement diverged from batch oracle"
                      {:entry-equality? entry-equality?
                       :projected-length-equality?
                       projected-length-equality?})))
    true))

(defn inputs-same? [keys prior current]
  (every? (fn [key]
            (input-value-same? (get prior key) (get current key)))
          keys))

(defn changed-families
  "Return exactly the families whose declared inputs changed this frame.
   Device identity is the process-wide lifecycle pin: a replacement changes
   every family even when numeric revisions happen to match."
  [prev-inputs inputs]
  (cond
    (nil? prev-inputs) family-ids
    (not (identical? (:device prev-inputs) (:device inputs))) family-ids
    :else
    (into #{}
          (keep (fn [[family-id declaration]]
                  (when-not (inputs-same? declaration prev-inputs inputs)
                    family-id)))
          family-input-declarations)))

(defn system-identity [system]
  (when system (or (:frame-input/identity system) system)))

(defn shape-rev [system]
  (if-let [revision (:!shape-rev system)] @revision 0))

(defn system-token [system]
  (when system [(system-identity system) (shape-rev system)]))

(defn bump-shape-rev! [system]
  (when-let [revision (:!shape-rev system)] (swap! revision inc)))

(defn- logarithm [value]
  #?(:clj (Math/log value) :cljs (js/Math.log value)))

(defn- power [base exponent]
  #?(:clj (Math/pow base exponent) :cljs (js/Math.pow base exponent)))

(defn region-encode-rung
  "Versioned geometric camera door.  The rung, not raw scale, is semantic."
  [scale]
  (let [scale (max 1.0e-9 (double (or scale 1.0)))]
    (long (#?(:clj Math/floor :cljs js/Math.floor)
           (/ (logarithm scale) (logarithm region-encode-step))))))

(defn quantize-region-encode-scale [scale]
  (power region-encode-step (region-encode-rung scale)))

(defn family-entry-delta
  "Pure family-index delta used by the renderer's sorted-map maintenance.
   Current keys come only from produced families; unchanged families are not
   inspected. Entries bucket by the family that PRODUCED them
   (:frame/producer, falling back to :family/id) — a cross-family entry such
   as a connector-minted text-family label must live and die with its
   producer, never with the family whose pipeline draws it."
  [keys-by-family entries produced-families entry-key]
  (let [entries-by-family (group-by #(or (:frame/producer %) (:family/id %))
                                    entries)]
    (reduce
     (fn [{:keys [remove insert next-keys] :as result} family-id]
       (let [prior (get keys-by-family family-id #{})
             rows (get entries-by-family family-id [])
             current (into #{} (map entry-key) rows)]
         (assoc result
                :remove (into remove (set/difference prior current))
                :insert (into insert rows)
                :next-keys (assoc next-keys family-id current))))
     {:remove #{} :insert [] :next-keys keys-by-family}
     produced-families)))

(def ^:private empty-ledger
  {:changed-families #{}
   :produced 0
   :comparator-calls 0
   :arrangement-identical? true
   :effects-maintained? false
   :plan-maintained? false
   :region-prepared 0
   :region-encoded 0
   :region-held 0})

(defonce ^:private !ledger (atom empty-ledger))

(defn begin-ledger! []
  (reset! !ledger empty-ledger))

(defn ledger-receipt [] @!ledger)

(defn assoc-ledger! [& keyvals]
  (apply swap! !ledger assoc keyvals))

(defn increment-ledger!
  ([key] (increment-ledger! key 1))
  ([key amount] (swap! !ledger update key (fnil + 0) amount)))

(defn publish-ledger! []
  #?(:cljs (aset js/globalThis "__softlandFrameLedger" (clj->js @!ledger)))
  @!ledger)
