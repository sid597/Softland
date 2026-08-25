(ns app.client.substrate.frame-effects
  "Pure W4 container-effect grammar and span derivation.

   Effects are session/arrangement facts. They never enter durable
   derivation. Keyed inputs are the container registry's effect declarations
   and the ordered arrangement's semantic order tokens; mutation enters through
   set-effects!; this namespace owns validation, effect-chain projection, and
   the independent CPU color oracle. The maintained door re-derives only
   changed containers while derive-effect-spans is its full-recompute oracle."
  (:require [clojure.set :as set]
            [app.client.workspace.containers :as containers]))

(def effect-grammar-version 1)
(def blur-algorithm-version :gaussian-9tap-downsample-v1)
(def default-max-blur-px 64.0)
(def legal-effect-keys
  #{:effects/version :opacity :mask :layer-blur :backdrop-blur :isolate?})

(def seam-declarations
  {:keyed-inputs [:container-ids+nested-paths :effect-values :arrangement-order]
   :doors [:set-effects! :arrangement-change]
   :ownership :frame-plan/session-effects
   :projections [:effect-spans :entry-effect-chains :derived-effect-contracts]
   :oracle :derive-effect-spans/full-recompute})

(defn- finite-number? [x]
  (and (number? x)
       #?(:clj (Double/isFinite (double x))
          :cljs (js/Number.isFinite x))))

(defn- validate-blur! [kind blur]
  (when-not (map? blur)
    (throw (ex-info "Blur declaration must be a map"
                    {:effect kind :value blur})))
  (let [unknown (seq (remove #{:radius-world :max-px :algorithm-version}
                             (keys blur)))
        radius (:radius-world blur)
        max-px (get blur :max-px default-max-blur-px)
        algorithm (:algorithm-version blur)]
    (when unknown
      (throw (ex-info "Blur declaration has unknown fields"
                      {:effect kind :unknown (vec unknown)})))
    (when-not (and (finite-number? radius) (not (neg? radius)))
      (throw (ex-info "Blur radius must be a finite non-negative world value"
                      {:effect kind :radius-world radius})))
    (when-not (and (finite-number? max-px) (pos? max-px))
      (throw (ex-info "Blur max-px must be finite and positive"
                      {:effect kind :max-px max-px})))
    (when-not (= blur-algorithm-version algorithm)
      (throw (ex-info "Blur algorithm-version is absent or unsupported"
                      {:effect kind :algorithm-version algorithm
                       :required blur-algorithm-version})))
    (assoc blur :max-px (double max-px)
                :radius-world (double radius))))

(defn validate-effects!
  "Validate and normalize one container's v1 effect declaration. Empty/nil is
   legal and means the container contributes no effect pass. Unknown fields
   fail closed so effect ordering cannot grow accidentally."
  [effects]
  (let [effects (or effects {})
        unknown (seq (remove legal-effect-keys (keys effects)))]
    (when-not (map? effects)
      (throw (ex-info "Container effects must be a map" {:effects effects})))
    (when unknown
      (throw (ex-info "Container effects contain unknown fields"
                      {:unknown (vec unknown)})))
    (when (contains? effects :opacity)
      (let [opacity (:opacity effects)]
        (when-not (and (finite-number? opacity) (<= 0.0 opacity 1.0))
          (throw (ex-info "Group opacity must be in [0,1]"
                          {:opacity opacity})))))
    (when (and (contains? effects :mask) (nil? (:mask effects)))
      (throw (ex-info "Mask designator cannot be nil" {:effects effects})))
    (when (and (contains? effects :isolate?)
               (not (or (true? (:isolate? effects))
                        (false? (:isolate? effects)))))
      (throw (ex-info "isolate? must be Boolean"
                      {:isolate? (:isolate? effects)})))
    (cond-> {:effects/version effect-grammar-version
             :opacity (double (get effects :opacity 1.0))
             :isolate? (true? (:isolate? effects))}
      (contains? effects :mask) (assoc :mask (:mask effects))
      (contains? effects :layer-blur)
      (assoc :layer-blur (validate-blur! :layer-blur (:layer-blur effects)))
      (contains? effects :backdrop-blur)
      (assoc :backdrop-blur
             (validate-blur! :backdrop-blur (:backdrop-blur effects))))))

(defn effectful? [effects]
  (let [{:keys [opacity mask layer-blur backdrop-blur isolate?]}
        (if (= effect-grammar-version (:effects/version effects))
          effects
          (validate-effects! effects))]
    (or (< opacity 1.0) (some? mask) (some? layer-blur)
        (some? backdrop-blur) isolate?)))

(defn derived-effect-declarations [effects]
  (let [{:keys [mask layer-blur backdrop-blur] :as normalized}
        (if (= effect-grammar-version (:effects/version effects))
          effects
          (validate-effects! effects))]
    (cond-> []
      mask
      (conj {:kind :alpha-mask
             :coverage {:boundary-relation :derived-effect
                        :geometry-operator :coverage-alpha-multiply
                        :operator-version effect-grammar-version
                        :support :designated-mask-subtree}
             :pick :none})
      layer-blur
      (conj {:kind :layer-blur
             :coverage {:boundary-relation :derived-effect
                        :geometry-operator :separable-gaussian
                        :operator-version (:algorithm-version layer-blur)
                        :support {:radius-world (:radius-world layer-blur)
                                  :max-px (:max-px layer-blur)}}
             :pick :none})
      backdrop-blur
      (conj {:kind :backdrop-blur
             :coverage {:boundary-relation :derived-effect
                        :geometry-operator :scene-snapshot-separable-gaussian
                        :operator-version (:algorithm-version backdrop-blur)
                        :support {:radius-world (:radius-world backdrop-blur)
                                  :max-px (:max-px backdrop-blur)}}
             :pick :none})
      (effectful? normalized)
      (conj {:kind :group-composite
             :coverage {:boundary-relation :derived-effect
                        :geometry-operator :premultiplied-group-composite
                        :operator-version effect-grammar-version
                        :support :group-span}
             :pick :none}))))

(defn projected-blur
  "Project a world-space blur declaration to pixels and name its clamp regime."
  [blur effective-scale camera-zoom]
  (let [{:keys [radius-world max-px algorithm-version]}
        (validate-blur! :blur blur)
        raw (* radius-world (double effective-scale) (double camera-zoom))
        px (min max-px raw)]
    {:radius-px px
     :raw-radius-px raw
     :clamped? (> raw max-px)
     :regime (if (> raw max-px) :max-px-clamped :projected-world-radius)
     :algorithm-version algorithm-version
     :downsample-levels
     (loop [radius px levels 0]
       (if (and (> radius 8.0) (< levels 3))
         (recur (/ radius 2.0) (inc levels))
         levels))}))

(defn- prefix-at? [path prefix offset]
  (and (<= (+ offset (count prefix)) (count path))
       (= prefix (subvec (vec path) offset (+ offset (count prefix))))))

(defn stack-prefix?
  "True when semantic container-path is present as a prefix after an optional
   executor-owned root token. This admits source/store orders and frame orders
   without confusing a later matching descendant for the root chain."
  [container-path entry-path]
  (let [container-path (vec container-path)
        entry-path (vec entry-path)]
    (or (prefix-at? entry-path container-path 0)
        (and (seq entry-path)
             (= :frame/root (ffirst entry-path))
             (prefix-at? entry-path container-path 1)))))

(defn contiguous-ranges [indices]
  (when-let [indices (seq (sort indices))]
    (loop [remaining (next indices)
           start (first indices)
           prior (first indices)
           result []]
      (if-let [index (first remaining)]
        (if (= index (inc prior))
          (recur (next remaining) start index result)
          (recur (next remaining) index index
                 (conj result [start (inc prior)])))
        (conj result [start (inc prior)])))))

(defn- effect-container-rows [registry]
  (let [effective (containers/effective registry)]
    (into []
          (keep (fn [[cid row]]
                  (let [effects (validate-effects! (:effects row))]
                    (when (effectful? effects)
                      {:container/id cid
                       :stack-path (:stack-path (get effective cid))
                       :depth (count (:stack-path (get effective cid)))
                       :effects effects
                       :derived-effects (derived-effect-declarations effects)}))))
          (:containers registry))))

(defn- parent-container-id [rows row]
  (let [path (:stack-path row)]
    (->> rows
         (filter #(and (< (:depth %) (:depth row))
                       (stack-prefix? (:stack-path %) path)))
         (sort-by :depth >)
         first
         :container/id)))

(defn derive-effect-spans
  "Full recompute oracle. A container may lawfully bind multiple disjoint
   ranges; pass-class precedes stack-path in scene ordering, so contiguity is an
   optimization fact, never a compiler assumption."
  [registry arrangement]
  (let [entries (vec arrangement)
        rows (effect-container-rows registry)]
    (->> rows
         (map (fn [row]
                (let [indices (keep-indexed
                               (fn [index entry]
                                 (when (stack-prefix?
                                        (:stack-path row)
                                        (get-in entry [:order :stack-path]))
                                   index))
                               entries)]
                  (assoc row
                         :parent/container-id (parent-container-id rows row)
                         :entry-ranges (vec (or (contiguous-ranges indices) []))
                         :entry-count (count indices)))))
         (filter #(pos? (:entry-count %)))
         (sort-by (juxt (comp - :depth) (comp pr-str :container/id)))
         vec)))

(defn entry-effect-chain [spans entry-index]
  (->> spans
       (filter (fn [{:keys [entry-ranges]}]
                 (some (fn [[start end]] (<= start entry-index (dec end)))
                       entry-ranges)))
       (sort-by :depth)
       (mapv #(select-keys % [:container/id :effects :depth]))))

(defn empty-maintained-state []
  {:declarations {} :order-token nil :spans-by-container {}
   :last-derivation {:containers 0 :siblings 0 :full? true}
   :total-derivations 0})

(defn- declarations [registry]
  (into {}
        (map (fn [{:keys [container/id stack-path effects depth]}]
               [id {:stack-path stack-path :effects effects :depth depth}]))
        (effect-container-rows registry)))

(defn- arrangement-token [arrangement]
  (mapv (fn [entry] [(:entry/id entry) (:order entry)]) arrangement))

(defn maintain-effect-spans
  "Incremental maintained door. When order is unchanged, only containers whose
   normalized declaration changed are recomputed; untouched sibling spans keep
   value identity. The full oracle remains derive-effect-spans."
  [state registry arrangement]
  (let [state (or state (empty-maintained-state))
        current-declarations (declarations registry)
        order-token (arrangement-token arrangement)
        full? (not= order-token (:order-token state))
        changed (if full?
                  (set/union (set (keys (:declarations state)))
                             (set (keys current-declarations)))
                  (set (for [cid (set/union (set (keys (:declarations state)))
                                           (set (keys current-declarations)))
                             :when (not= (get (:declarations state) cid)
                                         (get current-declarations cid))]
                         cid)))
        full-spans (when (or full? (seq changed))
                     (derive-effect-spans registry arrangement))
        fresh-by-id (into {} (map (juxt :container/id identity)) full-spans)
        next-by-id (if full?
                     fresh-by-id
                     (reduce (fn [rows cid]
                               (if-let [span (get fresh-by-id cid)]
                                 (assoc rows cid span)
                                 (dissoc rows cid)))
                             (:spans-by-container state)
                             changed))
        ordered (->> next-by-id vals
                     (sort-by (juxt (comp - :depth)
                                    (comp pr-str :container/id))) vec)]
    {:declarations current-declarations
     :order-token order-token
     :spans-by-container next-by-id
     :spans ordered
     :last-derivation {:containers (count changed)
                       :siblings (if full? (max 0 (- (count current-declarations)
                                                     (count changed))) 0)
                       :full? full?}
     :total-derivations (+ (:total-derivations state 0) (count changed))}))

(defn source-over
  "CPU linear-premultiplied source-over oracle."
  [[sr sg sb sa] [dr dg db da]]
  (let [remain (- 1.0 sa)]
    [(+ sr (* dr remain))
     (+ sg (* dg remain))
     (+ sb (* db remain))
     (+ sa (* da remain))]))

(defn apply-group-opacity [[r g b a] opacity]
  (let [opacity (double opacity)]
    [(* r opacity) (* g opacity) (* b opacity) (* a opacity)]))

(defn apply-alpha-mask [[r g b a] mask-alpha]
  (let [mask-alpha (max 0.0 (min 1.0 (double mask-alpha)))]
    [(* r mask-alpha) (* g mask-alpha)
     (* b mask-alpha) (* a mask-alpha)]))

(defn composite-group
  "Composite already-premultiplied children in forward order, then apply mask
   and group opacity exactly once."
  [children {:keys [opacity mask-alpha]
             :or {opacity 1.0 mask-alpha 1.0}}]
  (-> (reduce (fn [dst src] (source-over src dst))
              [0.0 0.0 0.0 0.0] children)
      (apply-alpha-mask mask-alpha)
      (apply-group-opacity opacity)))
