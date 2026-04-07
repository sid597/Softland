(ns components.token-matcher
  "Map literal CSS values → nearest design tokens (dt) from loop.cljs.
   Produces token refs where matches are within threshold, preserving
   literal values otherwise."
  (:require [clojure.string :as str]
            [components.css-parsers :as css]))

;; ---------------------------------------------------------------------------
;; Distance functions
;; ---------------------------------------------------------------------------

(defn color-distance
  "Euclidean distance between two [r g b a] vectors. Alpha weighted 2x.
   Returns 1.0 if only one is nil, 0.0 if both nil."
  [a b]
  (cond
    (and (nil? a) (nil? b)) 0.0
    (or (nil? a) (nil? b))  1.0
    :else
    (let [dr (- (nth a 0) (nth b 0))
          dg (- (nth a 1) (nth b 1))
          db (- (nth a 2) (nth b 2))
          da (* 2.0 (- (nth a 3 1.0) (nth b 3 1.0)))]
      (Math/sqrt (+ (* dr dr) (* dg dg) (* db db) (* da da))))))

(defn- numeric-distance [a b] (Math/abs (double (- a b))))

;; ---------------------------------------------------------------------------
;; Match functions — each returns {:type :token :ref [...]} or {:type :solid :value v}
;; ---------------------------------------------------------------------------

(defn match-color
  "Find the nearest dt color token for an [r g b a] vector.
   dt-colors: flat map of keyword→[r g b a] (e.g., (:colors dt)).
   Returns {:type :token :ref [:colors <key>]} if within threshold,
   otherwise {:type :solid :value rgba-vec}."
  [rgba dt-colors threshold]
  (if (nil? rgba)
    nil
    (let [best (reduce-kv
                 (fn [acc k v]
                   (let [d (color-distance rgba v)]
                     (if (< d (:dist acc))
                       {:dist d :key k}
                       acc)))
                 {:dist Double/MAX_VALUE :key nil}
                 dt-colors)]
      (if (and (:key best) (<= (:dist best) threshold))
        {:type :token :ref [:colors (:key best)] :token-distance (:dist best)}
        {:type :solid :value rgba}))))

(defn match-radius
  "Find nearest dt radius token for a number.
   dt-radii: map of keyword→number (e.g., (:radii dt)).
   Returns token keyword or literal number."
  [n dt-radii]
  (if (nil? n)
    nil
    (let [best (reduce-kv
                 (fn [acc k v]
                   (let [d (numeric-distance n v)]
                     (if (< d (:dist acc))
                       {:dist d :key k}
                       acc)))
                 {:dist Double/MAX_VALUE :key nil}
                 dt-radii)]
      (if (and (:key best) (<= (:dist best) 2.0))
        (:key best)
        n))))

(defn match-shadow
  "Find nearest dt shadow token for a shadow map.
   Compares blur, offset-x, offset-y, and color distance.
   Returns token keyword or literal shadow map."
  [shadow-map dt-shadows]
  (if (nil? shadow-map)
    nil
    (let [s-blur   (or (:blur shadow-map) 0)
          s-ox     (or (get-in shadow-map [:offset 0])
                       (:offset-x shadow-map) 0)
          s-oy     (or (get-in shadow-map [:offset 1])
                       (:offset-y shadow-map) 0)
          s-color  (let [c (:color shadow-map)]
                     (cond
                       (vector? c) c
                       (string? c) (css/parse-color c)
                       :else nil))
          best (reduce-kv
                 (fn [acc k v]
                   (let [t-blur  (or (:blur v) 0)
                         t-ox    (or (:offset-x v) 0)
                         t-oy    (or (:offset-y v) 0)
                         t-color (:color v)
                         d (+ (numeric-distance s-blur t-blur)
                              (numeric-distance s-ox t-ox)
                              (numeric-distance s-oy t-oy)
                              (* 10.0 (color-distance s-color t-color)))]
                     (if (< d (:dist acc))
                       {:dist d :key k}
                       acc)))
                 {:dist Double/MAX_VALUE :key nil}
                 dt-shadows)]
      (if (and (:key best) (<= (:dist best) 5.0))
        (:key best)
        shadow-map))))

(defn match-font-size
  "Find nearest dt font-size token. Returns keyword or number."
  [n dt-font-sizes]
  (if (nil? n)
    nil
    (let [best (reduce-kv
                 (fn [acc k v]
                   (let [d (numeric-distance n v)]
                     (if (< d (:dist acc))
                       {:dist d :key k}
                       acc)))
                 {:dist Double/MAX_VALUE :key nil}
                 dt-font-sizes)]
      (if (and (:key best) (<= (:dist best) 1.0))
        (:key best)
        n))))

(defn match-spacing
  "Find nearest dt spacing token. Returns keyword or number."
  [n dt-spacing]
  (if (nil? n)
    nil
    (let [best (reduce-kv
                 (fn [acc k v]
                   (let [d (numeric-distance n v)]
                     (if (< d (:dist acc))
                       {:dist d :key k}
                       acc)))
                 {:dist Double/MAX_VALUE :key nil}
                 dt-spacing)]
      (if (and (:key best) (<= (:dist best) 2.0))
        (:key best)
        n))))

;; ---------------------------------------------------------------------------
;; Recursive tokenizer — walks IR tree, upgrades literals → token refs
;; ---------------------------------------------------------------------------

(defn- resolve-color-val
  "If color-val is a string, parse it. If vector, keep. Returns [r g b a] or nil."
  [v]
  (cond
    (vector? v) v
    (string? v) (css/parse-color v)
    :else nil))

(defn- tokenize-fill [fill dt-colors threshold]
  (if (and fill (= :solid (:type fill)))
    (let [rgba (resolve-color-val (:value fill))
          matched (when rgba (match-color rgba dt-colors threshold))]
      (or matched fill))
    fill))

(defn- tokenize-visual [visual dt threshold]
  (if (nil? visual)
    nil
    (let [dt-colors (:colors dt)]
      (cond-> visual
        (:fill visual)
        (update :fill tokenize-fill dt-colors threshold)

        (get-in visual [:border :color])
        (update-in [:border :color]
                   (fn [c]
                     (let [rgba (resolve-color-val c)
                           matched (when rgba (match-color rgba dt-colors threshold))]
                       (if (and matched (= :token (:type matched)))
                         matched
                         c))))

        (get-in visual [:shadow :color])
        (update-in [:shadow :color]
                   (fn [c]
                     (let [rgba (resolve-color-val c)
                           matched (when rgba (match-color rgba dt-colors threshold))]
                       (if (and matched (= :token (:type matched)))
                         matched
                         c))))))))

(defn- tokenize-typography [typo dt threshold]
  (if (nil? typo)
    nil
    (let [dt-colors (:colors dt)]
      (cond-> typo
        (:color typo)
        (update :color
                (fn [c]
                  (let [rgba (resolve-color-val c)
                        matched (when rgba (match-color rgba dt-colors threshold))]
                    (if (and matched (= :token (:type matched)))
                      matched
                      c))))))))

(defn tokenize-ir
  "Recursively walk an IR node tree, replacing literal values with token refs
   where they match dt tokens within threshold.
   dt: the design tokens map from loop.cljs.
   opts: {:color-threshold 0.08} (optional overrides)."
  ([node dt] (tokenize-ir node dt {}))
  ([node dt opts]
   (let [threshold (or (:color-threshold opts) 0.08)]
     (cond-> node
       (:visual node)
       (update :visual tokenize-visual dt threshold)

       (:typography node)
       (update :typography tokenize-typography dt threshold)

       (:children node)
       (update :children (fn [cs] (mapv #(tokenize-ir % dt opts) cs)))

       (:states node)
       (update :states
               (fn [states]
                 (reduce-kv
                   (fn [acc k v]
                     (assoc acc k
                            (cond-> v
                              (:visual v) (update :visual tokenize-visual dt threshold)
                              (:typography v) (update :typography tokenize-typography dt threshold))))
                   {}
                   states)))))))
