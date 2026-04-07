(ns components._design_ir
  "Design IR schema and deterministic validator.

   The validator is intentionally strict:
   - rejects malformed node shapes
   - rejects unknown keys
   - validates recursive children/state nodes"
  (:require [clojure.set :as set]
            [clojure.string :as str]))

(def schema-version 1)

(def allowed-node-roles
  #{:container :interactive :decorative :text})

(def allowed-node-keys
  #{:id :tag :role :bounds :visual :typography :layout :states :slots :children :source-meta})

(def allowed-bounds-keys
  #{:x :y :w :h})

(def allowed-visual-keys
  #{:fill :gradient :radius :border :shadow :opacity})

(def allowed-fill-keys
  #{:type :value :ref :token-ref :token-distance})

(def allowed-fill-types
  #{:solid :token})

(def allowed-gradient-keys
  #{:type :angle :stops})

(def allowed-gradient-stop-keys
  #{:at :value})

(def allowed-radius-keys
  #{:uniform :corners})

(def allowed-border-keys
  #{:width :widths :color})

(def allowed-shadow-keys
  #{:offset :blur :spread :color})

(def allowed-typography-keys
  #{:content :size :weight :color :family :line-height :letter-spacing :align})

(def allowed-layout-keys
  #{:display :direction :gap :padding :align-items :justify :justify-content :align :auto-height?})

(def allowed-state-keys
  #{:visual :typography :layout :slots :children})

(def allowed-slot-keys
  #{:role :description :bounds})

(def allowed-source-meta-keys
  #{:library :component :variant :url :extractor :source-path})

(def allowed-layout-directions
  #{:row :column})

(def allowed-layout-displays
  #{:flex :block :inline :none})

(defn finite-number?
  [x]
  (and (number? x)
       #?(:clj
          (let [d (double x)]
            (and (not (Double/isNaN d))
                 (not (Double/isInfinite d))))
          :cljs
          (js/isFinite x))))

(defn non-negative-number?
  [x]
  (and (finite-number? x) (<= 0 x)))

(defn positive-number?
  [x]
  (and (finite-number? x) (< 0 x)))

(defn non-empty-string?
  [s]
  (and (string? s) (not (str/blank? s))))

(defn token-ref?
  [x]
  (and (sequential? x)
       (seq x)
       (every? keyword? x)))

(defn rgba-vector?
  [x]
  (and (vector? x)
       (= 4 (count x))
       (every? finite-number? x)))

(defn color-value?
  [x]
  (or (string? x) (rgba-vector? x)))

(defn padding-value?
  [x]
  (or (non-negative-number? x)
      (and (vector? x)
           (contains? #{2 4} (count x))
           (every? non-negative-number? x))))

(defn indexed-mapcat
  [f coll]
  (mapcat identity (map-indexed f coll)))

(defn validation-error
  ([path code message]
   {:path path :code code :message message})
  ([path code message details]
   {:path path :code code :message message :details details}))

(defn unknown-key-errors
  [m allowed-keys path]
  (let [unknown (sort (set/difference (set (keys m)) allowed-keys))]
    (mapv (fn [k]
            (validation-error (conj path k)
                              :unknown-key
                              "Unknown key in Design IR map."
                              {:allowed (sort allowed-keys)}))
          unknown)))

(declare validate-ir-node)

(defn validate-bounds
  [bounds path]
  (cond
    (nil? bounds)
    [(validation-error path :missing-bounds "Missing required :bounds map.")]

    (not (map? bounds))
    [(validation-error path :invalid-bounds "Expected :bounds to be a map.")]

    :else
    (vec
      (concat
        (unknown-key-errors bounds allowed-bounds-keys path)
        (when-not (contains? bounds :w)
          [(validation-error (conj path :w) :missing-width "Missing required bounds width :w.")])
        (when-not (contains? bounds :h)
          [(validation-error (conj path :h) :missing-height "Missing required bounds height :h.")])
        (when (contains? bounds :w)
          (when-not (positive-number? (:w bounds))
            [(validation-error (conj path :w) :invalid-width "Bounds :w must be a positive number.")]))
        (when (contains? bounds :h)
          (when-not (positive-number? (:h bounds))
            [(validation-error (conj path :h) :invalid-height "Bounds :h must be a positive number.")]))
        (for [k [:x :y]
              :when (contains? bounds k)
              :when (not (finite-number? (get bounds k)))]
          (validation-error (conj path k) :invalid-coordinate "Bounds coordinate must be a finite number."))))))

(defn validate-fill
  [fill path]
  (cond
    (not (map? fill))
    [(validation-error path :invalid-fill "Expected :fill to be a map.")]

    :else
    (let [fill-type (:type fill)
          ref (or (:ref fill) (:token-ref fill))]
      (vec
        (concat
          (unknown-key-errors fill allowed-fill-keys path)
          (when-not (contains? fill :type)
            [(validation-error (conj path :type) :missing-fill-type "Missing required fill :type.")])
          (when (contains? fill :type)
            (when-not (contains? allowed-fill-types fill-type)
              [(validation-error (conj path :type)
                                 :invalid-fill-type
                                 "Fill :type must be :solid or :token.")]))
          (when (= fill-type :solid)
            (when-not (color-value? (:value fill))
              [(validation-error (conj path :value)
                                 :invalid-fill-value
                                 "Solid fill requires :value as color string or [r g b a].")]))
          (when (= fill-type :token)
            (when-not (token-ref? ref)
              [(validation-error path
                                 :invalid-token-ref
                                 "Token fill requires :ref or :token-ref as vector of keywords.")]))
          (when (contains? fill :value)
            (when-not (color-value? (:value fill))
              [(validation-error (conj path :value)
                                 :invalid-fill-value
                                 "Fill :value must be color string or [r g b a].")]))
          (when (contains? fill :token-distance)
            (when-not (non-negative-number? (:token-distance fill))
              [(validation-error (conj path :token-distance)
                                 :invalid-token-distance
                                 "Fill :token-distance must be a non-negative number.")])))))))

(defn validate-gradient-stop
  [stop path]
  (cond
    (not (map? stop))
    [(validation-error path :invalid-gradient-stop "Gradient stop must be a map.")]

    :else
    (vec
      (concat
        (unknown-key-errors stop allowed-gradient-stop-keys path)
        (when-not (contains? stop :at)
          [(validation-error (conj path :at) :missing-stop-at "Gradient stop missing :at.")])
        (when-not (contains? stop :value)
          [(validation-error (conj path :value) :missing-stop-value "Gradient stop missing :value.")])
        (when (contains? stop :at)
          (let [at (:at stop)]
            (when-not (and (finite-number? at) (<= 0 at 1))
              [(validation-error (conj path :at)
                                 :invalid-stop-at
                                 "Gradient stop :at must be a number in [0,1].")])))
        (when (contains? stop :value)
          (when-not (color-value? (:value stop))
            [(validation-error (conj path :value)
                               :invalid-stop-value
                               "Gradient stop :value must be a color string or [r g b a].")]))))))

(defn validate-gradient
  [gradient path]
  (cond
    (not (map? gradient))
    [(validation-error path :invalid-gradient "Expected :gradient to be a map.")]

    :else
    (vec
      (concat
        (unknown-key-errors gradient allowed-gradient-keys path)
        (when (contains? gradient :type)
          (when-not (= :linear (:type gradient))
            [(validation-error (conj path :type)
                               :invalid-gradient-type
                               "Only :linear gradients are currently supported.")]))
        (when (contains? gradient :angle)
          (when-not (finite-number? (:angle gradient))
            [(validation-error (conj path :angle)
                               :invalid-gradient-angle
                               "Gradient :angle must be a finite number.")]))
        (let [stops (:stops gradient)]
          (when (contains? gradient :stops)
            (cond
              (not (vector? stops))
              [(validation-error (conj path :stops)
                                 :invalid-gradient-stops
                                 "Gradient :stops must be a vector.")]

              (empty? stops)
              [(validation-error (conj path :stops)
                                 :empty-gradient-stops
                                 "Gradient :stops must not be empty.")]

              :else
              (indexed-mapcat
                (fn [idx stop]
                  (validate-gradient-stop stop (conj path :stops idx)))
                stops))))))))

(defn validate-radius
  [radius path]
  (cond
    (not (map? radius))
    [(validation-error path :invalid-radius "Expected :radius to be a map.")]

    :else
    (let [uniform (:uniform radius)
          corners (:corners radius)]
      (vec
        (concat
          (unknown-key-errors radius allowed-radius-keys path)
          (when (and (nil? uniform) (nil? corners))
            [(validation-error path
                               :missing-radius-shape
                               "Radius requires :uniform or :corners.")])
          (when (and (some? uniform) (some? corners))
            [(validation-error path
                               :ambiguous-radius-shape
                               "Radius cannot define both :uniform and :corners.")])
          (when (some? uniform)
            (when-not (non-negative-number? uniform)
              [(validation-error (conj path :uniform)
                                 :invalid-uniform-radius
                                 "Radius :uniform must be a non-negative number.")]))
          (when (some? corners)
            (cond
              (not (vector? corners))
              [(validation-error (conj path :corners)
                                 :invalid-corner-radii
                                 "Radius :corners must be a vector.")]

              (not= 4 (count corners))
              [(validation-error (conj path :corners)
                                 :invalid-corner-radii-count
                                 "Radius :corners must contain exactly 4 values.")]

              (not-every? non-negative-number? corners)
              [(validation-error (conj path :corners)
                                 :invalid-corner-radii
                                 "All :corners values must be non-negative numbers.")])))))))

(defn validate-border
  [border path]
  (cond
    (not (map? border))
    [(validation-error path :invalid-border "Expected :border to be a map.")]

    :else
    (let [w (:width border)
          ws (:widths border)]
      (vec
        (concat
          (unknown-key-errors border allowed-border-keys path)
          (when (and (nil? w) (nil? ws) (nil? (:color border)))
            [(validation-error path
                               :empty-border
                               "Border map is empty. Provide :width/:widths and/or :color.")])
          (when (and (some? w) (some? ws))
            [(validation-error path
                               :ambiguous-border-width
                               "Border cannot define both :width and :widths.")])
          (when (some? w)
            (when-not (non-negative-number? w)
              [(validation-error (conj path :width)
                                 :invalid-border-width
                                 "Border :width must be a non-negative number.")]))
          (when (some? ws)
            (cond
              (not (vector? ws))
              [(validation-error (conj path :widths)
                                 :invalid-border-widths
                                 "Border :widths must be a vector.")]

              (not= 4 (count ws))
              [(validation-error (conj path :widths)
                                 :invalid-border-widths-count
                                 "Border :widths must contain exactly 4 values.")]

              (not-every? non-negative-number? ws)
              [(validation-error (conj path :widths)
                                 :invalid-border-widths
                                 "All border widths must be non-negative numbers.")]))
          (when (contains? border :color)
            (when-not (color-value? (:color border))
              [(validation-error (conj path :color)
                                 :invalid-border-color
                                 "Border :color must be a color string or [r g b a].")])))))))

(defn validate-shadow
  [shadow path]
  (cond
    (not (map? shadow))
    [(validation-error path :invalid-shadow "Expected :shadow to be a map.")]

    :else
    (vec
      (concat
        (unknown-key-errors shadow allowed-shadow-keys path)
        (when (contains? shadow :offset)
          (let [offset (:offset shadow)]
            (cond
              (not (vector? offset))
              [(validation-error (conj path :offset)
                                 :invalid-shadow-offset
                                 "Shadow :offset must be [x y].")]

              (not= 2 (count offset))
              [(validation-error (conj path :offset)
                                 :invalid-shadow-offset-count
                                 "Shadow :offset must contain exactly 2 values.")]

              (not-every? finite-number? offset)
              [(validation-error (conj path :offset)
                                 :invalid-shadow-offset
                                 "Shadow :offset values must be finite numbers.")])))
        (for [k [:blur :spread]
              :when (contains? shadow k)
              :when (not (non-negative-number? (get shadow k)))]
          (validation-error (conj path k)
                            :invalid-shadow-size
                            "Shadow blur/spread must be non-negative numbers."))
        (when (contains? shadow :color)
          (when-not (color-value? (:color shadow))
            [(validation-error (conj path :color)
                               :invalid-shadow-color
                               "Shadow :color must be a color string or [r g b a].")]))))))

(defn validate-visual
  [visual path]
  (cond
    (not (map? visual))
    [(validation-error path :invalid-visual "Expected :visual to be a map.")]

    :else
    (vec
      (concat
        (unknown-key-errors visual allowed-visual-keys path)
        (when-let [fill (:fill visual)]
          (validate-fill fill (conj path :fill)))
        (when-let [gradient (:gradient visual)]
          (validate-gradient gradient (conj path :gradient)))
        (when-let [radius (:radius visual)]
          (validate-radius radius (conj path :radius)))
        (when-let [border (:border visual)]
          (validate-border border (conj path :border)))
        (when-let [shadow (:shadow visual)]
          (validate-shadow shadow (conj path :shadow)))
        (when (contains? visual :opacity)
          (let [opacity (:opacity visual)]
            (when-not (and (finite-number? opacity) (<= 0 opacity 1))
              [(validation-error (conj path :opacity)
                                 :invalid-opacity
                                 "Visual :opacity must be in [0,1].")])))))))

(defn validate-typography
  [typography path {:keys [partial?] :or {partial? false}}]
  (cond
    (not (map? typography))
    [(validation-error path :invalid-typography "Expected :typography to be a map.")]

    :else
    (vec
      (concat
        (unknown-key-errors typography allowed-typography-keys path)
        (when (and (not partial?) (not (contains? typography :content)))
          [(validation-error (conj path :content)
                             :missing-typography-content
                             "Typography requires :content for full node specs.")])
        (when (contains? typography :content)
          (when-not (string? (:content typography))
            [(validation-error (conj path :content)
                               :invalid-typography-content
                               "Typography :content must be a string.")]))
        (for [k [:size :weight :line-height]
              :when (contains? typography k)
              :when (not (positive-number? (get typography k)))]
          (validation-error (conj path k)
                            :invalid-typography-number
                            "Typography numeric value must be a positive number."))
        (when (contains? typography :letter-spacing)
          (when-not (finite-number? (:letter-spacing typography))
            [(validation-error (conj path :letter-spacing)
                               :invalid-letter-spacing
                               "Typography :letter-spacing must be finite.")]))
        (when (contains? typography :family)
          (when-not (non-empty-string? (:family typography))
            [(validation-error (conj path :family)
                               :invalid-family
                               "Typography :family must be a non-empty string.")]))
        (when (contains? typography :color)
          (when-not (color-value? (:color typography))
            [(validation-error (conj path :color)
                               :invalid-typography-color
                               "Typography :color must be a color string or [r g b a].")]))
        (when (contains? typography :align)
          (when-not (keyword? (:align typography))
            [(validation-error (conj path :align)
                               :invalid-typography-align
                               "Typography :align must be a keyword.")]))))))

(defn validate-layout
  [layout path]
  (cond
    (not (map? layout))
    [(validation-error path :invalid-layout "Expected :layout to be a map.")]

    :else
    (vec
      (concat
        (unknown-key-errors layout allowed-layout-keys path)
        (when (contains? layout :display)
          (let [display (:display layout)]
            (when-not (contains? allowed-layout-displays display)
              [(validation-error (conj path :display)
                                 :invalid-layout-display
                                 "Layout :display must be one of #{:flex :block :inline :none}.")])))
        (when (contains? layout :direction)
          (let [direction (:direction layout)]
            (when-not (contains? allowed-layout-directions direction)
              [(validation-error (conj path :direction)
                                 :invalid-layout-direction
                                 "Layout :direction must be :row or :column.")])))
        (when (contains? layout :gap)
          (when-not (non-negative-number? (:gap layout))
            [(validation-error (conj path :gap)
                               :invalid-layout-gap
                               "Layout :gap must be a non-negative number.")]))
        (when (contains? layout :padding)
          (when-not (padding-value? (:padding layout))
            [(validation-error (conj path :padding)
                               :invalid-layout-padding
                               "Layout :padding must be number or 2/4-value vector.")]))
        (when (contains? layout :auto-height?)
          (when-not (boolean? (:auto-height? layout))
            [(validation-error (conj path :auto-height?)
                               :invalid-auto-height
                               "Layout :auto-height? must be boolean.")]))
        (for [k [:align-items :justify :justify-content :align]
              :when (contains? layout k)
              :when (not (keyword? (get layout k)))]
          (validation-error (conj path k)
                            :invalid-layout-alignment
                            "Layout alignment values must be keywords."))))))

(defn validate-slot
  [slot path]
  (cond
    (not (map? slot))
    [(validation-error path :invalid-slot "Slot definition must be a map.")]

    :else
    (vec
      (concat
        (unknown-key-errors slot allowed-slot-keys path)
        (when-not (contains? slot :role)
          [(validation-error (conj path :role)
                             :missing-slot-role
                             "Slot definition requires :role.")])
        (when (contains? slot :role)
          (let [role (:role slot)]
            (when-not (contains? allowed-node-roles role)
              [(validation-error (conj path :role)
                                 :invalid-slot-role
                                 "Slot :role must be one of allowed node roles.")]))
          nil)
        (when (contains? slot :description)
          (when-not (string? (:description slot))
            [(validation-error (conj path :description)
                               :invalid-slot-description
                               "Slot :description must be a string.")]))
        (when (contains? slot :bounds)
          (validate-bounds (:bounds slot) (conj path :bounds)))))))

(defn validate-slots
  [slots path]
  (cond
    (not (map? slots))
    [(validation-error path :invalid-slots "Expected :slots to be a map.")]

    :else
    (indexed-mapcat
      (fn [_ [slot-id slot]]
        (let [slot-path (conj path slot-id)
              slot-id-errors (when-not (keyword? slot-id)
                               [(validation-error slot-path
                                                  :invalid-slot-id
                                                  "Slot id must be a keyword.")])]
          (concat slot-id-errors
                  (validate-slot slot slot-path))))
      (vec slots))))

(defn validate-source-meta
  [source-meta path]
  (cond
    (not (map? source-meta))
    [(validation-error path :invalid-source-meta "Expected :source-meta to be a map.")]

    :else
    (vec
      (concat
        (unknown-key-errors source-meta allowed-source-meta-keys path)
        (for [k [:library :component :variant :url :source-path]
              :when (contains? source-meta k)
              :when (not (non-empty-string? (get source-meta k)))]
          (validation-error (conj path k)
                            :invalid-source-meta-value
                            "Source metadata string must be non-empty."))
        (when (contains? source-meta :extractor)
          (let [extractor (:extractor source-meta)]
            (when-not (keyword? extractor)
              [(validation-error (conj path :extractor)
                                 :invalid-extractor
                                 "Source metadata :extractor must be a keyword.")])))))))

(defn validate-state-overrides
  [states path]
  (cond
    (not (map? states))
    [(validation-error path :invalid-states "Expected :states to be a map of state keyword to overrides.")]

    :else
    (indexed-mapcat
      (fn [_ [state-id override]]
        (let [state-path (conj path state-id)]
          (concat
            (when-not (keyword? state-id)
              [(validation-error state-path :invalid-state-id "State id must be a keyword.")])
            (cond
              (not (map? override))
              [(validation-error state-path
                                 :invalid-state-override
                                 "State override must be a map.")]

              :else
              (concat
                (unknown-key-errors override allowed-state-keys state-path)
                (when-let [visual (:visual override)]
                  (validate-visual visual (conj state-path :visual)))
                (when-let [typography (:typography override)]
                  (validate-typography typography (conj state-path :typography) {:partial? true}))
                (when-let [layout (:layout override)]
                  (validate-layout layout (conj state-path :layout)))
                (when-let [slots (:slots override)]
                  (validate-slots slots (conj state-path :slots)))
                (when-let [children (:children override)]
                  (if-not (vector? children)
                    [(validation-error (conj state-path :children)
                                       :invalid-state-children
                                       "State override :children must be a vector.")]
                    (indexed-mapcat
                      (fn [idx child]
                        (validate-ir-node child (conj state-path :children idx)))
                      children))))))))
      (vec states))))

(defn validate-ir-node
  ([node] (validate-ir-node node [:design-ir]))
  ([node path]
   (cond
     (not (map? node))
     [(validation-error path :invalid-node "Design IR node must be a map.")]

     :else
     (vec
       (concat
         (unknown-key-errors node allowed-node-keys path)
         (when-not (contains? node :tag)
           [(validation-error (conj path :tag) :missing-tag "Design IR node requires :tag.")])
         (when (contains? node :tag)
           (when-not (non-empty-string? (:tag node))
             [(validation-error (conj path :tag)
                                :invalid-tag
                                "Design IR node :tag must be a non-empty string.")]))
         (when-not (contains? node :role)
           [(validation-error (conj path :role) :missing-role "Design IR node requires :role.")])
         (when (contains? node :role)
           (let [role (:role node)]
             (when-not (contains? allowed-node-roles role)
               [(validation-error (conj path :role)
                                  :invalid-role
                                  "Design IR node :role must be a supported keyword.")])))
         (when (contains? node :id)
           (let [id (:id node)]
             (when-not (or (keyword? id) (string? id))
               [(validation-error (conj path :id)
                                  :invalid-id
                                  "Design IR node :id must be keyword or string.")])))
         (validate-bounds (:bounds node) (conj path :bounds))
         (when-let [visual (:visual node)]
           (validate-visual visual (conj path :visual)))
         (when-let [typography (:typography node)]
           (validate-typography typography (conj path :typography) {:partial? false}))
         (when-let [layout (:layout node)]
           (validate-layout layout (conj path :layout)))
         (when-let [slots (:slots node)]
           (validate-slots slots (conj path :slots)))
         (when-let [source-meta (:source-meta node)]
           (validate-source-meta source-meta (conj path :source-meta)))
         (when-let [states (:states node)]
           (validate-state-overrides states (conj path :states)))
         (let [children (:children node)]
           (cond
             (nil? children)
             []

             (not (vector? children))
             [(validation-error (conj path :children)
                                :invalid-children
                                "Design IR :children must be a vector.")]

             :else
             (indexed-mapcat
               (fn [idx child]
                 (validate-ir-node child (conj path :children idx)))
               children))))))))

(defn validate-design-ir
  "Validate a design IR document.

   Accepts either:
   - a raw Design IR root node map
   - a wrapper map containing :design-ir root node

   Returns:
   {:schema-version 1
    :valid? boolean
    :errors [{:path .. :code .. :message ..}]}"
  [doc]
  (let [root (if (and (map? doc) (contains? doc :design-ir))
               (:design-ir doc)
               doc)
        errors (validate-ir-node root [:design-ir])]
    {:schema-version schema-version
     :valid? (empty? errors)
     :errors (vec errors)}))

(defn valid-design-ir?
  [doc]
  (:valid? (validate-design-ir doc)))

(defn assert-valid-design-ir!
  "Throws ex-info if the Design IR document is invalid."
  [doc]
  (let [{:keys [valid? errors]} (validate-design-ir doc)]
    (when-not valid?
      (throw (ex-info "Invalid Design IR document."
                      {:type ::invalid-design-ir
                       :errors errors})))
    doc))
