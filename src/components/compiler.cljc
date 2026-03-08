(ns components.compiler
  "Compile Design IR nodes → rt-node trees compatible with loop.cljs.

   The rt-node shape (loop.cljs:594-610):
     {:id :type :bounds {:x :y :w :h} :style {} :actions {} :children [] :text [] :clip? :data :layout}

   Design IR shape (components._design_ir):
     {:tag :role :bounds :visual :typography :layout :children :slots :states :source-meta}

   This compiler maps between the two, resolving token refs and CSS color strings
   via the dt (design tokens) map."
  (:require [clojure.string :as str]
            [components.css-parsers :as css]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- deep-merge
  "Recursively merge b into a. b values win for non-map keys."
  [a b]
  (if (and (map? a) (map? b))
    (merge-with deep-merge a b)
    b))

(defn- resolve-color
  "Resolve a color value from IR to [r g b a].
   Handles:
   - {:type :token :ref [:colors :bg]} → lookup in dt
   - {:type :solid :value [r g b a]}   → extract value
   - {:type :solid :value \"#fff\"}     → parse CSS string
   - [r g b a] vector                  → pass through
   - CSS string                        → parse"
  [color-val dt use-tokens?]
  (cond
    (nil? color-val)
    nil

    ;; Direct RGBA vector
    (vector? color-val)
    color-val

    ;; Token reference
    (and (map? color-val) (= :token (:type color-val)))
    (if use-tokens?
      (get-in dt (:ref color-val))
      ;; Fallback: resolve token to literal
      (or (get-in dt (:ref color-val))
          (:value color-val)))

    ;; Solid literal
    (and (map? color-val) (= :solid (:type color-val)))
    (let [v (:value color-val)]
      (cond
        (vector? v) v
        (string? v) (css/parse-color v)
        :else nil))

    ;; Plain CSS string
    (string? color-val)
    (css/parse-color color-val)

    :else nil))

;; ---------------------------------------------------------------------------
;; Style compiler (IR :visual → rt-node :style)
;; ---------------------------------------------------------------------------

(defn- compile-style
  "Map IR :visual to rt-node :style map."
  [visual dt use-tokens?]
  (if (nil? visual)
    {}
    (let [;; Background fill
          bg (resolve-color (:fill visual) dt use-tokens?)

          ;; Radius
          radius-val (:radius visual)
          radius (when radius-val
                   (cond
                     (:uniform radius-val)  {:radius (:uniform radius-val)}
                     (:corners radius-val)  {:corner-radii (:corners radius-val)}
                     (number? radius-val)   {:radius radius-val}
                     :else nil))

          ;; Border
          border (:border visual)
          border-style (when border
                         (cond-> {}
                           (:width border)
                           (assoc :border-width (:width border))

                           (:widths border)
                           (assoc :border-widths (:widths border))

                           (:color border)
                           (assoc :border-color
                                  (resolve-color (:color border) dt use-tokens?))))

          ;; Shadow: IR uses {:offset [x y]}, rt-node uses {:offset-x N :offset-y N}
          shadow (:shadow visual)
          shadow-style (when shadow
                         (let [color (resolve-color (:color shadow) dt use-tokens?)
                               offset (:offset shadow)
                               ox (if offset (nth offset 0) (or (:offset-x shadow) 0))
                               oy (if offset (nth offset 1) (or (:offset-y shadow) 0))]
                           (cond-> {:offset-x ox :offset-y oy}
                             (:blur shadow)   (assoc :blur (:blur shadow))
                             (:spread shadow) (assoc :spread (:spread shadow))
                             color            (assoc :color color))))

          ;; Gradient: extract angle + first/last stop colors for rt-node format
          gradient (:gradient visual)
          gradient-style (when (and gradient (= :linear (:type gradient)) (seq (:stops gradient)))
                           (let [stops (:stops gradient)
                                 first-color (resolve-color (:value (first stops)) dt use-tokens?)
                                 last-color  (resolve-color (:value (last stops)) dt use-tokens?)
                                 angle (or (:angle gradient) Math/PI)
                                 ;; rt-node gradient format: [angle t_stop 0 0]
                                 t-stop (or (:at (first (rest stops))) 0.5)]
                             (cond-> {}
                               true        (assoc :gradient [angle t-stop 0 0])
                               first-color (assoc :bg first-color)
                               last-color  (assoc :gradient-color2 last-color))))

          ;; Opacity: multiply into bg alpha
          opacity (:opacity visual)]

      (cond-> {}
        bg           (assoc :bg (if (and opacity bg)
                                  (assoc bg 3 (* (nth bg 3 1.0) opacity))
                                  bg))
        radius       (merge radius)
        border-style (merge border-style)
        shadow-style (assoc :shadow shadow-style)
        gradient-style (merge gradient-style)))))

;; ---------------------------------------------------------------------------
;; Layout compiler (IR :layout → rt-node :layout)
;; ---------------------------------------------------------------------------

(defn- compile-layout
  "Map IR :layout to rt-node :layout map. Only for flex/grid layouts."
  [ir-layout]
  (when (and ir-layout
             (contains? #{:flex :grid} (:display ir-layout)))
    (let [direction (or (:direction ir-layout) :column)
          gap (or (:gap ir-layout) 0)
          padding (:padding ir-layout)
          align (or (:align ir-layout)
                    (case (or (:align-items ir-layout) :start)
                      :flex-start :start
                      :start      :start
                      :center     :center
                      :flex-end   :end
                      :end        :end
                      :start))
          auto-height? (:auto-height? ir-layout)]
      (cond-> {:direction direction
               :gap       gap
               :align     align}
        padding      (assoc :padding padding)
        auto-height? (assoc :auto-height? true)))))

;; ---------------------------------------------------------------------------
;; Text compiler (IR :typography → rt-node :text ops)
;; ---------------------------------------------------------------------------

(defn- compile-text
  "Map IR :typography to rt-node :text vector.
   Returns a vector of text-op maps."
  [typography dt use-tokens? bounds]
  (when (and typography (:content typography))
    (let [content (:content typography)
          size    (or (:size typography) 14)
          color   (resolve-color (:color typography) dt use-tokens?)
          [r g b a] (or color [0.9 0.9 0.92 1.0])
          ;; Position text relative to node bounds
          ;; x starts at 0 (layout engine handles absolute positioning)
          ;; y baseline at size (single-line default)
          line-h  (or (:line-height typography) (* size 1.4))]
      [{:text content
        :type :text
        :from 0
        :to   (count content)
        :x    0
        :y    line-h
        :size size
        :r    r :g g :b b :a a}])))

;; ---------------------------------------------------------------------------
;; Type mapping (IR :tag + :role → rt-node :type)
;; ---------------------------------------------------------------------------

(defn- compile-type
  "Map IR :tag + :role to rt-node :type keyword."
  [tag role]
  (let [tag (when tag (str/lower-case tag))]
    (cond
      (= tag "button")  :button
      (= tag "input")   :input
      (= tag "a")       :link
      (= tag "img")     :image
      (= tag "svg")     :icon
      (= tag "span")    :text
      (= tag "p")       :text
      (= tag "h1")      :text
      (= tag "h2")      :text
      (= tag "h3")      :text
      (= tag "label")   :text
      (= role :text)    :text
      (= role :interactive) :interactive
      (= role :decorative) :decorative
      :else             :rect)))

;; ---------------------------------------------------------------------------
;; Main compiler
;; ---------------------------------------------------------------------------

(defn- is-rt-node?
  "Detect if input is already in rt-node format (not Design IR)."
  [node]
  (and (map? node)
       (or (contains? node :style)
           (contains? node :type))
       (not (contains? node :visual))
       (not (contains? node :tag))))

(defn compile-ir
  "Compile a Design IR node → rt-node map.

   dt: design tokens map from loop.cljs
   opts: {:use-tokens? bool     ;; resolve token refs (default true)
          :id-prefix string}    ;; prefix for generated IDs

   Also handles direct rt-node input (from blueprints) — passes through unchanged."
  ([node dt] (compile-ir node dt {}))
  ([node dt opts]
   (if (nil? node)
     nil
     ;; If it's already an rt-node, pass through
     (if (is-rt-node? node)
       (cond-> node
         (:children node)
         (update :children (fn [cs] (mapv #(compile-ir % dt opts) cs))))

       ;; Compile from Design IR
       (let [{:keys [use-tokens? id-prefix]
              :or   {use-tokens? true id-prefix ""}} opts

             ;; ID
             ir-id (or (:id node) (str id-prefix (gensym "ir-")))
             id    (if (keyword? ir-id) ir-id (keyword (str id-prefix ir-id)))

             ;; Type
             node-type (compile-type (:tag node) (:role node))

             ;; Bounds — always start at 0,0; layout engine positions
             ir-bounds (:bounds node)
             bounds {:x (or (:x ir-bounds) 0)
                     :y (or (:y ir-bounds) 0)
                     :w (or (:w ir-bounds) 0)
                     :h (or (:h ir-bounds) 0)}

             ;; Style from :visual
             style (compile-style (:visual node) dt use-tokens?)

             ;; Layout from :layout
             layout (compile-layout (:layout node))

             ;; Text from :typography
             text-ops (compile-text (:typography node) dt use-tokens? bounds)

             ;; Children — recursive compile
             children (when (:children node)
                        (mapv #(compile-ir % dt opts) (:children node)))

             ;; Slots → placeholder children
             slot-children (when (:slots node)
                             (mapv (fn [[slot-name slot-def]]
                                     {:id       (keyword (str (name id) "-slot-" (name slot-name)))
                                      :type     :slot
                                      :bounds   (or (:bounds slot-def) {:x 0 :y 0 :w 0 :h 0})
                                      :style    {}
                                      :actions  {}
                                      :children []
                                      :text     []
                                      :clip?    false
                                      :data     {:slot-name slot-name
                                                 :slot-role (:role slot-def)
                                                 :slot-description (:description slot-def)}
                                      :layout   nil})
                                   (:slots node)))

             all-children (vec (concat (or children []) (or slot-children [])))]

         ;; Assemble rt-node
         {:id       id
          :type     node-type
          :bounds   bounds
          :style    (or style {})
          :actions  {}
          :children all-children
          :text     (or text-ops [])
          :clip?    false
          :data     (cond-> {}
                      (:source-meta node) (assoc :source-meta (:source-meta node))
                      (:tag node)         (assoc :ir-tag (:tag node))
                      (:role node)        (assoc :ir-role (:role node)))
          :layout   layout})))))

;; ---------------------------------------------------------------------------
;; Component compiler (blueprint with states)
;; ---------------------------------------------------------------------------

(defn compile-component
  "Compile a blueprint (with :states) → map of state-name → rt-node.

   Handles two blueprint formats:
   1. Codex format: {:states [{:name :default :tree {rt-node-like-map}}]}
      Each state has a complete :tree — compile each independently
   2. Design IR format: {:states {:hover {:visual ...}}}
      States are override maps — deep-merge with :default, then compile

   Returns: {:default rt-node, :hover rt-node, ...}"
  ([blueprint dt] (compile-component blueprint dt {}))
  ([blueprint dt opts]
   (cond
     ;; Codex blueprint format: :states is a vector of {:name :tree}
     (and (vector? (:states blueprint))
          (every? #(and (:name %) (:tree %)) (:states blueprint)))
     (reduce (fn [acc state]
               (assoc acc (:name state)
                      (compile-ir (:tree state) dt opts)))
             {}
             (:states blueprint))

     ;; Design IR format: :states is a map of overrides
     (map? (:states blueprint))
     (let [base-node (dissoc blueprint :states)
           default-rt (compile-ir base-node dt opts)]
       (reduce-kv
         (fn [acc state-name overrides]
           (let [merged (deep-merge base-node overrides)
                 state-rt (compile-ir merged dt opts)]
             (assoc acc state-name state-rt)))
         {:default default-rt}
         (:states blueprint)))

     ;; No states — single default
     :else
     {:default (compile-ir blueprint dt opts)})))
