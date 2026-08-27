(ns app.client.substrate.chrome-material
  "Neutral world-anchor/screen-metric quad geometry for the chrome render road.")

(defn finite-number? [x]
  (and (number? x)
       #?(:clj (Double/isFinite (double x))
          :cljs (js/Number.isFinite x))))

(defn- point? [point]
  (and (vector? point) (= 2 (count point)) (every? finite-number? point)))

(defn- vertex [anchor offset color]
  {:anchor anchor :offset-px offset :color color})

(defn- quad [anchors offsets color]
  (mapv #(vertex %1 %2 color) anchors offsets))

(defn- validate-quad! [{:keys [anchors offsets-px color] :as value}]
  (when-not (= #{:anchors :offsets-px :color} (set (keys value)))
    (throw (ex-info "Neutral quad has unknown or missing fields" {:quad value})))
  (when-not (and (vector? anchors) (= 4 (count anchors))
                 (every? point? anchors))
    (throw (ex-info "Neutral quad requires four finite world anchors"
                    {:anchors anchors})))
  (when-not (and (vector? offsets-px) (= 4 (count offsets-px))
                 (every? point? offsets-px))
    (throw (ex-info "Neutral quad requires four finite px offsets"
                    {:offsets-px offsets-px})))
  (when-not (and (vector? color) (= 4 (count color))
                 (every? finite-number? color))
    (throw (ex-info "Neutral quad requires finite RGBA" {:color color})))
  value)

(defn material-vertices
  "Expand a vector of neutral quads to two triangles per quad."
  [quads]
  (when-not (vector? quads)
    (throw (ex-info "Neutral material must be a vector of quads"
                    {:material quads})))
  (into []
        (mapcat (fn [quad-row]
                  (let [{:keys [anchors offsets-px color]}
                        (validate-quad! quad-row)
                        [a b c d] (quad anchors offsets-px color)]
                    [a b c a c d])))
        quads))

(def vertex-words 9)
(def vertex-stride (* vertex-words 4))

(defn vertex-values [{:keys [anchor offset-px color]} container-idx]
  (into (into (vec anchor) offset-px)
        (conj (vec color) (or container-idx 0))))
