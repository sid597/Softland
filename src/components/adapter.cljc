(ns components.adapter
  "Bridge between _extractor.js JSON output and Design IR format.

   Extractor output (JS-style camelCase):
     {:tag \"div\" :bounds {:x :y :w :h}
      :styles {:backgroundColor \"rgb(...)\" :borderTopWidth \"1px\" ...}
      :textContent \"Hello\" :children [...]}

   Design IR (what the compiler expects):
     {:tag \"div\" :role :container :bounds {:w :h}
      :visual {:fill {:type :solid :value \"rgb(...)\"} :radius {:uniform 8} ...}
      :typography {:content \"Hello\" :size 14 :color \"rgb(...)\" ...}
      :layout {:display :flex :direction :row :gap 8 :padding [8 8 8 8]}
      :children [...]}"
  (:require [clojure.string :as str]
            [components.css-parsers :as css]))

;; ---------------------------------------------------------------------------
;; Role inference
;; ---------------------------------------------------------------------------

(defn- infer-role
  "Infer IR :role from tag name and context."
  [tag has-text? has-children?]
  (let [tag (when tag (str/lower-case tag))]
    (cond
      (#{"button" "a" "input" "select" "textarea"} tag) :interactive
      (#{"img" "svg" "hr" "canvas"} tag)                :decorative
      has-text?                                          :text
      :else                                              :container)))

;; ---------------------------------------------------------------------------
;; Style helpers
;; ---------------------------------------------------------------------------

(defn- transparent? [color-str]
  (or (nil? color-str)
      (= color-str "rgba(0, 0, 0, 0)")
      (= color-str "transparent")
      (str/blank? color-str)))

(defn- zero-px? [s]
  (or (nil? s) (= s "0px") (= s "0") (str/blank? s)))

(defn- all-zero? [& strs]
  (every? zero-px? strs))

;; ---------------------------------------------------------------------------
;; Core adapter
;; ---------------------------------------------------------------------------

(defn extracted->ir
  "Convert a single extracted node (from _extractor.js JSON) to Design IR.
   Recursively converts children.

   opts: {:source-url string  ;; for :source-meta
          :library string}    ;; e.g. \"shadcn\""
  ([node] (extracted->ir node {}))
  ([node opts]
   (when node
     (let [styles   (or (:styles node) {})
           tag      (:tag node)
           text     (:textContent node)
           children (:children node)
           bounds   (:bounds node)

           ;; --- Visual ---
           bg       (:backgroundColor styles)
           fill     (when-not (transparent? bg)
                      {:type :solid :value bg})

           ;; Border radius
           br-tl (css/parse-px (:borderTopLeftRadius styles))
           br-tr (css/parse-px (:borderTopRightRadius styles))
           br-br (css/parse-px (:borderBottomRightRadius styles))
           br-bl (css/parse-px (:borderBottomLeftRadius styles))
           radius (when (and br-tl br-tr br-br br-bl
                             (not (every? zero? [br-tl br-tr br-br br-bl])))
                    (if (= br-tl br-tr br-br br-bl)
                      {:uniform br-tl}
                      {:corners [br-tl br-tr br-br br-bl]}))

           ;; Border
           bw-t (css/parse-px (:borderTopWidth styles))
           bw-r (css/parse-px (:borderRightWidth styles))
           bw-b (css/parse-px (:borderBottomWidth styles))
           bw-l (css/parse-px (:borderLeftWidth styles))
           has-border? (and bw-t bw-r bw-b bw-l
                            (not (every? zero? [bw-t bw-r bw-b bw-l])))
           border-color (when has-border?
                          (css/parse-border-color
                            (:borderTopColor styles)
                            (:borderRightColor styles)
                            (:borderBottomColor styles)
                            (:borderLeftColor styles)))
           border (when has-border?
                    (let [widths [bw-t bw-r bw-b bw-l]]
                      (cond-> (if (apply = widths)
                                {:width (first widths)}
                                {:widths widths})
                        border-color (assoc :color border-color))))

           ;; Box shadow
           shadow-raw (css/parse-box-shadow (:boxShadow styles))
           shadow (when shadow-raw (first shadow-raw)) ;; take first shadow for IR (single)

           ;; Background gradient
           gradient (css/parse-gradient (:backgroundImage styles))

           ;; Opacity
           opacity-str (:opacity styles)
           opacity (when (and opacity-str (string? opacity-str))
                     (let [o (css/parse-px opacity-str)] ;; parse-px handles bare numbers
                       (when (and o (< o 1.0)) o)))

           visual (let [v (cond-> {}
                           fill     (assoc :fill fill)
                           radius   (assoc :radius radius)
                           border   (assoc :border border)
                           shadow   (assoc :shadow shadow)
                           gradient (assoc :gradient gradient)
                           opacity  (assoc :opacity opacity))]
                    (when (seq v) v))

           ;; --- Typography ---
           font-size   (css/parse-px (:fontSize styles))
           font-weight (css/parse-font-weight (:fontWeight styles))
           text-color  (:color styles)
           font-family (:fontFamily styles)

           typography (when text
                        (cond-> {:content text}
                          font-size   (assoc :size font-size)
                          font-weight (assoc :weight font-weight)
                          text-color  (assoc :color text-color)
                          font-family (assoc :family font-family)))

           ;; --- Layout ---
           display   (css/parse-display (:display styles))
           direction (css/parse-direction (:flexDirection styles))
           align     (css/parse-align (:alignItems styles))
           gap       (css/parse-px (:gap styles))
           pad-t     (css/parse-px (:paddingTop styles))
           pad-r     (css/parse-px (:paddingRight styles))
           pad-b     (css/parse-px (:paddingBottom styles))
           pad-l     (css/parse-px (:paddingLeft styles))
           has-padding? (and pad-t pad-r pad-b pad-l
                             (not (every? zero? [pad-t pad-r pad-b pad-l])))
           padding (when has-padding? [pad-t pad-r pad-b pad-l])

           layout (when (or (= display :flex) (= display :grid))
                    (cond-> {:display :flex}
                      direction (assoc :direction direction)
                      align     (assoc :align-items align)
                      gap       (assoc :gap gap)
                      padding   (assoc :padding padding)))

           ;; If not flex but has padding, still capture padding in layout
           layout (if (and (nil? layout) padding)
                    {:display :block :padding padding}
                    layout)

           ;; --- Children ---
           ir-children (when (seq children)
                         (mapv #(extracted->ir % opts) children))

           ;; --- Role ---
           role (infer-role tag (some? text) (seq children))

           ;; --- Assemble ---
           ir (cond-> {:tag    (or tag "div")
                       :role   role
                       :bounds {:w (or (:w bounds) 0)
                                :h (or (:h bounds) 0)}}
                visual       (assoc :visual visual)
                typography   (assoc :typography typography)
                layout       (assoc :layout layout)
                ir-children  (assoc :children (vec (remove nil? ir-children)))
                (:source-url opts) (assoc :source-meta
                                          (cond-> {:url (:source-url opts)}
                                            (:library opts) (assoc :library (:library opts))
                                            tag             (assoc :component tag))))]
       ir))))

;; ---------------------------------------------------------------------------
;; Convenience: full pipeline in one call
;; ---------------------------------------------------------------------------

(defn extract->rt-node
  "Full pipeline: extractor JSON → Design IR → rt-node tree.
   Requires compiler namespace and dt (design tokens).

   extracted-json: the parsed JSON from _extractor.js (:tree key)
   compile-fn:    components.compiler/compile-ir
   dt:            design tokens map from loop.cljs
   opts:          {:source-url :library :use-tokens?}"
  [extracted-json compile-fn dt opts]
  (let [ir (extracted->ir extracted-json (select-keys opts [:source-url :library]))]
    (compile-fn ir dt (select-keys opts [:use-tokens? :id-prefix]))))
