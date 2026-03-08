(ns components._verifier
  "Deterministic Design IR verifier.

   This module is intentionally authoritative:
   - no probabilistic scoring
   - no LLM dependency
   - explicit thresholds and pass/fail output"
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [components._design_ir :as design-ir]))

(def default-thresholds
  {:geometry  {:max-deviation-px 2.0}
   :style     {:max-color-dist 0.02
               :max-scalar-diff 2.0}
   :structure {:similarity-ratio 0.70}
   :states    {:coverage-ratio 0.60
               :max-color-dist 0.02
               :max-scalar-diff 2.0}})

(def verification-report-schema
  {:required-keys [:component :timestamp :verdict :metrics]
   :metric-keys
   {:geometry [:max-deviation-px :mean-deviation-px :threshold :pass?]
    :style [:max-color-dist :max-scalar-diff :threshold :pass?]
    :structure [:similarity-ratio :threshold :pass?]
    :states [:covered :expected :ratio :threshold :pass?]}
   :optional-keys [:notes :source-path :extractor]})

(defn abs*
  [x]
  #?(:clj (Math/abs (double x))
     :cljs (js/Math.abs x)))

(defn sqrt*
  [x]
  #?(:clj (Math/sqrt (double x))
     :cljs (js/Math.sqrt x)))

(defn now-iso
  []
  #?(:clj (.toString (java.time.Instant/now))
     :cljs (.toISOString (js/Date.))))

(defn finite-number?
  [x]
  (and (number? x)
       #?(:clj
          (let [d (double x)]
            (and (not (Double/isNaN d))
                 (not (Double/isInfinite d))))
          :cljs
          (js/isFinite x))))

(defn safe-parse-double
  [s]
  (try
    #?(:clj (Double/parseDouble (str/trim s))
       :cljs (js/parseFloat (str/trim s)))
    (catch #?(:clj Throwable :cljs :default) _
      nil)))

(defn clamp01
  [v]
  (-> v (max 0.0) (min 1.0)))

(defn hex-byte
  [s]
  (safe-parse-double (str "0x" s)))

(defn normalize-rgba-vector
  [v]
  (let [[r g b a] (if (= 3 (count v))
                    [(nth v 0) (nth v 1) (nth v 2) 1.0]
                    [(nth v 0) (nth v 1) (nth v 2) (nth v 3)])
        rgb-scale (if (or (> r 1.0) (> g 1.0) (> b 1.0)) 255.0 1.0)
        alpha-scale (if (> a 1.0) 255.0 1.0)]
    [(clamp01 (/ r rgb-scale))
     (clamp01 (/ g rgb-scale))
     (clamp01 (/ b rgb-scale))
     (clamp01 (/ a alpha-scale))]))

(defn parse-hex-color
  [s]
  (let [m (re-matches #"^#([0-9a-fA-F]+)$" (str/trim s))]
    (when m
      (let [h (second m)]
        (case (count h)
          3 (let [r (hex-byte (str (nth h 0) (nth h 0)))
                  g (hex-byte (str (nth h 1) (nth h 1)))
                  b (hex-byte (str (nth h 2) (nth h 2)))]
              (when (every? finite-number? [r g b])
                (normalize-rgba-vector [r g b 255.0])))
          4 (let [r (hex-byte (str (nth h 0) (nth h 0)))
                  g (hex-byte (str (nth h 1) (nth h 1)))
                  b (hex-byte (str (nth h 2) (nth h 2)))
                  a (hex-byte (str (nth h 3) (nth h 3)))]
              (when (every? finite-number? [r g b a])
                (normalize-rgba-vector [r g b a])))
          6 (let [r (hex-byte (subs h 0 2))
                  g (hex-byte (subs h 2 4))
                  b (hex-byte (subs h 4 6))]
              (when (every? finite-number? [r g b])
                (normalize-rgba-vector [r g b 255.0])))
          8 (let [r (hex-byte (subs h 0 2))
                  g (hex-byte (subs h 2 4))
                  b (hex-byte (subs h 4 6))
                  a (hex-byte (subs h 6 8))]
              (when (every? finite-number? [r g b a])
                (normalize-rgba-vector [r g b a])))
          nil)))))

(defn parse-rgba-component
  [s]
  (let [trimmed (str/trim s)]
    (if (str/ends-with? trimmed "%")
      (let [n (safe-parse-double (subs trimmed 0 (dec (count trimmed))))]
        (when (finite-number? n) (* 255.0 (/ n 100.0))))
      (safe-parse-double trimmed))))

(defn parse-alpha-component
  [s]
  (let [trimmed (str/trim s)]
    (if (str/ends-with? trimmed "%")
      (let [n (safe-parse-double (subs trimmed 0 (dec (count trimmed))))]
        (when (finite-number? n) (/ n 100.0)))
      (safe-parse-double trimmed))))

(defn parse-rgb-function
  [s]
  (when-let [[_ fn-name body] (re-matches #"^(rgb|rgba)\((.+)\)$" (str/lower-case (str/trim s)))]
    (let [parts (mapv str/trim (str/split body #","))]
      (cond
        (and (= fn-name "rgb") (= 3 (count parts)))
        (let [[r g b] (map parse-rgba-component parts)]
          (when (every? finite-number? [r g b])
            (normalize-rgba-vector [r g b 255.0])))

        (and (= fn-name "rgba") (= 4 (count parts)))
        (let [r (parse-rgba-component (nth parts 0))
              g (parse-rgba-component (nth parts 1))
              b (parse-rgba-component (nth parts 2))
              a (parse-alpha-component (nth parts 3))]
          (when (every? finite-number? [r g b a])
            (normalize-rgba-vector [r g b a])))

        :else
        nil))))

(defn color->rgba
  [c]
  (cond
    (nil? c)
    nil

    (vector? c)
    (when (contains? #{3 4} (count c))
      (when (every? finite-number? c)
        (normalize-rgba-vector c)))

    (string? c)
    (or (parse-hex-color c)
        (parse-rgb-function c))

    :else
    nil))

(defn color-distance
  [a b]
  (let [va (color->rgba a)
        vb (color->rgba b)]
    (cond
      (and va vb)
      (sqrt* (reduce + (map (fn [x y]
                              (let [d (- x y)]
                                (* d d)))
                            va vb)))

      (or va vb)
      1.0

      :else
      0.0)))

(defn indexed-mapcat
  [f coll]
  (mapcat identity (map-indexed f coll)))

(defn flatten-ir
  ([node] (flatten-ir node []))
  ([node path]
   (let [children (vec (or (:children node) []))]
     (into [{:path path :node node}]
           (indexed-mapcat
             (fn [idx child]
               (flatten-ir child (conj path idx)))
             children)))))

(defn index-by-path
  [root]
  (reduce (fn [acc {:keys [path node]}]
            (assoc acc path node))
          {}
          (flatten-ir root)))

(defn node-depth
  [root]
  (let [flat (flatten-ir root)]
    (if (empty? flat)
      0
      (inc (apply max (map (comp count :path) flat))))))

(defn canonical-bounds
  [node]
  (let [b (:bounds node)]
    {:x (double (or (:x b) 0.0))
     :y (double (or (:y b) 0.0))
     :w (double (or (:w b) 0.0))
     :h (double (or (:h b) 0.0))}))

(defn geometry-diff
  "Compare geometry by matching nodes on tree path.
   Returns metric payload (without threshold/pass?)."
  [source-ir converted-ir]
  (let [source-map (index-by-path source-ir)
        converted-map (index-by-path converted-ir)
        common-paths (sort (set/intersection (set (keys source-map))
                                             (set (keys converted-map))))
        deviations (vec
                     (mapcat
                       (fn [path]
                         (let [sb (canonical-bounds (get source-map path))
                               cb (canonical-bounds (get converted-map path))]
                           (mapv (fn [k] (abs* (- (get sb k) (get cb k))))
                                 [:x :y :w :h])))
                       common-paths))
        max-dev (if (seq deviations) (apply max deviations) nil)
        mean-dev (if (seq deviations) (/ (reduce + deviations) (count deviations)) nil)]
    {:max-deviation-px max-dev
     :mean-deviation-px mean-dev
     :nodes-compared (count common-paths)
     :source-node-count (count source-map)
     :converted-node-count (count converted-map)}))

(defn node-color-channels
  [node]
  (let [fill (get-in node [:visual :fill])
        fill-val (if (map? fill) (:value fill) fill)]
    (cond-> {}
      fill-val (assoc :fill fill-val)
      (get-in node [:visual :border :color]) (assoc :border (get-in node [:visual :border :color]))
      (get-in node [:visual :shadow :color]) (assoc :shadow (get-in node [:visual :shadow :color]))
      (get-in node [:typography :color]) (assoc :text (get-in node [:typography :color])))))

(defn node-scalar-channels
  [node]
  (let [radius (get-in node [:visual :radius])
        border (get-in node [:visual :border])
        shadow (get-in node [:visual :shadow])
        typography (:typography node)
        corners (:corners radius)
        widths (:widths border)
        offset (:offset shadow)]
    (cond-> {}
      (finite-number? (get-in node [:visual :opacity]))
      (assoc :opacity (double (get-in node [:visual :opacity])))

      (finite-number? (:uniform radius))
      (assoc :radius/uniform (double (:uniform radius)))

      (and (vector? corners) (= 4 (count corners)) (every? finite-number? corners))
      (assoc :radius/tl (double (nth corners 0))
             :radius/tr (double (nth corners 1))
             :radius/br (double (nth corners 2))
             :radius/bl (double (nth corners 3)))

      (finite-number? (:width border))
      (assoc :border/width (double (:width border)))

      (and (vector? widths) (= 4 (count widths)) (every? finite-number? widths))
      (assoc :border/top (double (nth widths 0))
             :border/right (double (nth widths 1))
             :border/bottom (double (nth widths 2))
             :border/left (double (nth widths 3)))

      (and (vector? offset) (= 2 (count offset)) (every? finite-number? offset))
      (assoc :shadow/offset-x (double (nth offset 0))
             :shadow/offset-y (double (nth offset 1)))

      (finite-number? (:blur shadow))
      (assoc :shadow/blur (double (:blur shadow)))

      (finite-number? (:spread shadow))
      (assoc :shadow/spread (double (:spread shadow)))

      (finite-number? (:size typography))
      (assoc :type/size (double (:size typography)))

      (finite-number? (:weight typography))
      (assoc :type/weight (double (:weight typography)))

      (finite-number? (:line-height typography))
      (assoc :type/line-height (double (:line-height typography)))

      (finite-number? (:letter-spacing typography))
      (assoc :type/letter-spacing (double (:letter-spacing typography))))))

(defn style-diff
  "Compare style channels by matching nodes on tree path.
   Returns metric payload (without threshold/pass?)."
  [source-ir converted-ir]
  (let [source-map (index-by-path source-ir)
        converted-map (index-by-path converted-ir)
        common-paths (sort (set/intersection (set (keys source-map))
                                             (set (keys converted-map))))
        per-path
        (mapv
          (fn [path]
            (let [source-node (get source-map path)
                  converted-node (get converted-map path)
                  source-colors (node-color-channels source-node)
                  converted-colors (node-color-channels converted-node)
                  source-scalars (node-scalar-channels source-node)
                  converted-scalars (node-scalar-channels converted-node)
                  color-keys (sort (set/union (set (keys source-colors))
                                              (set (keys converted-colors))))
                  scalar-keys (sort (set/union (set (keys source-scalars))
                                               (set (keys converted-scalars))))
                  color-deltas (mapv (fn [k]
                                       (color-distance (get source-colors k)
                                                       (get converted-colors k)))
                                     color-keys)
                  scalar-deltas (mapv (fn [k]
                                        (let [a (get source-scalars k)
                                              b (get converted-scalars k)]
                                          (cond
                                            (and (finite-number? a) (finite-number? b))
                                            (abs* (- a b))

                                            (or (finite-number? a) (finite-number? b))
                                            1.0

                                            :else
                                            0.0)))
                                      scalar-keys)]
              {:path path
               :max-color (if (seq color-deltas) (apply max color-deltas) 0.0)
               :max-scalar (if (seq scalar-deltas) (apply max scalar-deltas) 0.0)}))
          common-paths)
        max-color (if (seq per-path) (apply max (map :max-color per-path)) nil)
        max-scalar (if (seq per-path) (apply max (map :max-scalar per-path)) nil)]
    {:max-color-dist max-color
     :max-scalar-diff max-scalar
     :nodes-compared (count common-paths)}))

(defn structure-diff
  "Compare structure using path coverage, node budget, and depth budget.
   Returns metric payload (without threshold/pass?)."
  [source-ir converted-ir]
  (let [source-flattened (flatten-ir source-ir)
        converted-flattened (flatten-ir converted-ir)
        source-paths (set (map :path source-flattened))
        converted-paths (set (map :path converted-flattened))
        source-count (count source-paths)
        converted-count (count converted-paths)
        common-count (count (set/intersection source-paths converted-paths))
        source-depth (node-depth source-ir)
        converted-depth (node-depth converted-ir)
        path-coverage (if (zero? source-count)
                        1.0
                        (/ common-count source-count))
        node-budget-ratio (if (zero? converted-count)
                            1.0
                            (if (<= converted-count source-count)
                              1.0
                              (/ source-count converted-count)))
        depth-budget-ratio (if (zero? converted-depth)
                             1.0
                             (if (<= converted-depth source-depth)
                               1.0
                               (/ source-depth converted-depth)))
        similarity-ratio (* path-coverage node-budget-ratio depth-budget-ratio)]
    {:similarity-ratio similarity-ratio
     :path-coverage path-coverage
     :node-budget-ratio node-budget-ratio
     :depth-budget-ratio depth-budget-ratio
     :source-node-count source-count
     :converted-node-count converted-count
     :source-depth source-depth
     :converted-depth converted-depth}))

(defn state-style-diff
  [source-state converted-state]
  (let [source-node {:visual (:visual source-state)
                     :typography (:typography source-state)}
        converted-node {:visual (:visual converted-state)
                        :typography (:typography converted-state)}]
    (style-diff source-node converted-node)))

(defn state-parity-diff
  "Compare state coverage and override-style parity.
   Returns metric payload (without threshold/pass?)."
  [source-ir converted-ir]
  (let [source-states (or (:states source-ir) {})
        converted-states (or (:states converted-ir) {})
        expected-keys (set (keys source-states))
        converted-keys (set (keys converted-states))
        covered-keys (sort (set/intersection expected-keys converted-keys))
        expected (count expected-keys)
        covered (count covered-keys)
        ratio (if (zero? expected) 1.0 (/ covered expected))
        style-metrics (mapv (fn [state-id]
                              (state-style-diff (get source-states state-id)
                                                (get converted-states state-id)))
                            covered-keys)
        max-style-color (if (seq style-metrics)
                          (apply max (map #(or (:max-color-dist %) 0.0) style-metrics))
                          0.0)
        max-style-scalar (if (seq style-metrics)
                           (apply max (map #(or (:max-scalar-diff %) 0.0) style-metrics))
                           0.0)]
    {:covered covered
     :expected expected
     :ratio ratio
     :missing-states (vec (sort (set/difference expected-keys converted-keys)))
     :extra-states (vec (sort (set/difference converted-keys expected-keys)))
     :max-style-color-dist max-style-color
     :max-style-scalar-diff max-style-scalar}))

(defn merge-thresholds
  [overrides]
  (merge-with merge default-thresholds (or overrides {})))

(defn with-threshold-and-pass
  [metric metric-name thresholds]
  (case metric-name
    :geometry
    (let [threshold (get-in thresholds [:geometry :max-deviation-px])
          pass? (and (finite-number? (:max-deviation-px metric))
                     (<= (:max-deviation-px metric) threshold))]
      (assoc metric :threshold threshold :pass? pass?))

    :style
    (let [color-threshold (get-in thresholds [:style :max-color-dist])
          scalar-threshold (get-in thresholds [:style :max-scalar-diff])
          pass? (and (finite-number? (:max-color-dist metric))
                     (finite-number? (:max-scalar-diff metric))
                     (<= (:max-color-dist metric) color-threshold)
                     (<= (:max-scalar-diff metric) scalar-threshold))]
      (assoc metric
             :threshold {:max-color-dist color-threshold
                         :max-scalar-diff scalar-threshold}
             :pass? pass?))

    :structure
    (let [threshold (get-in thresholds [:structure :similarity-ratio])
          within-node-budget? (<= (:converted-node-count metric)
                                  (:source-node-count metric))
          pass? (and (finite-number? (:similarity-ratio metric))
                     (>= (:similarity-ratio metric) threshold)
                     within-node-budget?)]
      (assoc metric
             :threshold threshold
             :within-node-budget? within-node-budget?
             :pass? pass?))

    :states
    (let [coverage-threshold (get-in thresholds [:states :coverage-ratio])
          color-threshold (get-in thresholds [:states :max-color-dist])
          scalar-threshold (get-in thresholds [:states :max-scalar-diff])
          pass? (and (finite-number? (:ratio metric))
                     (>= (:ratio metric) coverage-threshold)
                     (<= (:max-style-color-dist metric) color-threshold)
                     (<= (:max-style-scalar-diff metric) scalar-threshold))]
      (assoc metric
             :threshold {:coverage-ratio coverage-threshold
                         :max-color-dist color-threshold
                         :max-scalar-diff scalar-threshold}
             :pass? pass?))))

(defn verify-component
  "End-to-end deterministic verification.

   Inputs:
   - source-ir: canonical source Design IR node
   - converted-ir: converted Design IR node (or compiled-then-rehydrated IR)

   Optional opts:
   {:component  \"shadcn/button\"
    :extractor  :browser | :llm | :figma
    :source-path \"...\"
    :timestamp  \"ISO-8601\"
    :notes      [\"...\"]
    :thresholds {...}}

   Output report is designed to embed directly into component .edn files."
  [source-ir converted-ir & {:keys [component extractor source-path timestamp notes thresholds]}]
  (let [source-validation (design-ir/validate-design-ir source-ir)
        converted-validation (design-ir/validate-design-ir converted-ir)
        valid-inputs? (and (:valid? source-validation)
                           (:valid? converted-validation))
        merged-thresholds (merge-thresholds thresholds)
        base-notes (vec (or notes []))
        validation-notes
        (vec
          (concat
            (when-not (:valid? source-validation)
              [(str "Source Design IR is invalid (" (count (:errors source-validation)) " errors).")])
            (when-not (:valid? converted-validation)
              [(str "Converted Design IR is invalid (" (count (:errors converted-validation)) " errors).")])))
        metrics
        (if valid-inputs?
          (let [geometry (with-threshold-and-pass (geometry-diff source-ir converted-ir) :geometry merged-thresholds)
                style (with-threshold-and-pass (style-diff source-ir converted-ir) :style merged-thresholds)
                structure (with-threshold-and-pass (structure-diff source-ir converted-ir) :structure merged-thresholds)
                states (with-threshold-and-pass (state-parity-diff source-ir converted-ir) :states merged-thresholds)]
            {:geometry geometry
             :style style
             :structure structure
             :states states})
          {:geometry {:max-deviation-px nil
                      :mean-deviation-px nil
                      :threshold (get-in merged-thresholds [:geometry :max-deviation-px])
                      :pass? false}
           :style {:max-color-dist nil
                   :max-scalar-diff nil
                   :threshold (get merged-thresholds :style)
                   :pass? false}
           :structure {:similarity-ratio nil
                       :threshold (get-in merged-thresholds [:structure :similarity-ratio])
                       :pass? false}
           :states {:covered nil
                    :expected nil
                    :ratio nil
                    :threshold (get merged-thresholds :states)
                    :pass? false}})
        metric-pass? (every? true? (map :pass? (vals metrics)))
        verdict (if metric-pass? :pass :fail)
        all-notes
        (vec
          (concat
            base-notes
            validation-notes
            (when (and valid-inputs? (not (:pass? (get metrics :states))))
              ["State parity did not satisfy coverage/style thresholds."])
            (when (and valid-inputs? (not (:pass? (get metrics :structure))))
              ["Structure similarity threshold not met or converted node count exceeded source."])))]
    {:component component
     :timestamp (or timestamp (now-iso))
     :verdict verdict
     :metrics metrics
     :notes all-notes
     :source-path source-path
     :extractor extractor
     :report-schema verification-report-schema
     :thresholds merged-thresholds
     :deterministic? true
     :authoritative? true
     :llm-role :optional-assistant-only}))
