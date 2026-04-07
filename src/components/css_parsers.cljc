(ns components.css-parsers
  "Parse CSS property strings into Clojure data structures.
   All colors return [r g b a] with 0-1 floats.
   All lengths return numbers (px assumed)."
  (:require [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- safe-parse-double [s]
  (when (and s (string? s) (not (str/blank? s)))
    (try
      #?(:clj  (Double/parseDouble (str/trim s))
         :cljs (let [n (js/parseFloat (str/trim s))]
                 (when-not (js/isNaN n) n)))
      #?(:clj  (catch Exception _ nil)
         :cljs (catch :default _ nil)))))

(defn- hex-byte
  "Parse a 2-char hex string to 0-1 float."
  [s]
  (when (and s (= 2 (count s)))
    (let [n #?(:clj  (Integer/parseInt s 16)
               :cljs (js/parseInt s 16))]
      (/ n 255.0))))

(defn- clamp01 [x] (max 0.0 (min 1.0 (double x))))

;; ---------------------------------------------------------------------------
;; Named colors (minimal set — covers what getComputedStyle returns + common)
;; ---------------------------------------------------------------------------

(def ^:private named-colors
  {"transparent" [0 0 0 0]
   "black"       [0 0 0 1]
   "white"       [1 1 1 1]
   "red"         [1 0 0 1]
   "green"       [0 0.502 0 1]
   "blue"        [0 0 1 1]
   "yellow"      [1 1 0 1]
   "cyan"        [0 1 1 1]
   "magenta"     [1 0 1 1]
   "gray"        [0.502 0.502 0.502 1]
   "grey"        [0.502 0.502 0.502 1]
   "orange"      [1 0.647 0 1]
   "purple"      [0.502 0 0.502 1]
   "pink"        [1 0.753 0.796 1]
   "none"        [0 0 0 0]
   "currentcolor" nil})

;; ---------------------------------------------------------------------------
;; HSL → RGB conversion
;; ---------------------------------------------------------------------------

(defn- hue->rgb [p q t]
  (let [t (cond (< t 0) (+ t 1) (> t 1) (- t 1) :else t)]
    (cond
      (< t (/ 1.0 6)) (+ p (* (- q p) 6.0 t))
      (< t 0.5)       q
      (< t (/ 2.0 3)) (+ p (* (- q p) (- (/ 2.0 3) t) 6.0))
      :else            p)))

(defn- hsl->rgb [h s l]
  (let [h (/ (mod h 360) 360.0)
        s (clamp01 (/ s 100.0))
        l (clamp01 (/ l 100.0))]
    (if (zero? s)
      [l l l]
      (let [q (if (< l 0.5) (* l (+ 1.0 s)) (+ l s (- (* l s))))
            p (- (* 2.0 l) q)]
        [(clamp01 (hue->rgb p q (+ h (/ 1.0 3))))
         (clamp01 (hue->rgb p q h))
         (clamp01 (hue->rgb p q (- h (/ 1.0 3))))]))))

;; ---------------------------------------------------------------------------
;; parse-color  — THE main color parser
;; ---------------------------------------------------------------------------

(defn parse-color
  "Parse a CSS color string → [r g b a] (0-1 floats) or nil."
  [s]
  (when (and s (string? s) (not (str/blank? s)))
    (let [s (str/trim (str/lower-case s))]
      (cond
        ;; Named colors
        (contains? named-colors s)
        (get named-colors s)

        ;; Hex: #RGB, #RGBA, #RRGGBB, #RRGGBBAA
        (str/starts-with? s "#")
        (let [hex (subs s 1)]
          (case (count hex)
            3 [(hex-byte (str (nth hex 0) (nth hex 0)))
               (hex-byte (str (nth hex 1) (nth hex 1)))
               (hex-byte (str (nth hex 2) (nth hex 2)))
               1.0]
            4 [(hex-byte (str (nth hex 0) (nth hex 0)))
               (hex-byte (str (nth hex 1) (nth hex 1)))
               (hex-byte (str (nth hex 2) (nth hex 2)))
               (hex-byte (str (nth hex 3) (nth hex 3)))]
            6 [(hex-byte (subs hex 0 2))
               (hex-byte (subs hex 2 4))
               (hex-byte (subs hex 4 6))
               1.0]
            8 [(hex-byte (subs hex 0 2))
               (hex-byte (subs hex 2 4))
               (hex-byte (subs hex 4 6))
               (hex-byte (subs hex 6 8))]
            nil))

        ;; CSS Color Level 4: rgb(R G B / A) or rgba(R G B / A) or rgb(R, G, B, A)
        (or (str/starts-with? s "rgb(") (str/starts-with? s "rgba("))
        (let [inner (-> s
                        (str/replace #"^rgba?\(" "")
                        (str/replace #"\)$" "")
                        str/trim)]
          (if (str/includes? inner "/")
            ;; Space-separated with slash alpha: rgb(23 23 25 / 0.5)
            (let [[color-part alpha-part] (str/split inner #"/")
                  parts (str/split (str/trim color-part) #"\s+")
                  alpha (str/trim alpha-part)]
              (when (= 3 (count parts))
                (let [parse-comp (fn [v]
                                   (if (str/ends-with? v "%")
                                     (/ (safe-parse-double (subs v 0 (dec (count v)))) 100.0)
                                     (/ (or (safe-parse-double v) 0) 255.0)))
                      a (if (str/ends-with? alpha "%")
                          (/ (safe-parse-double (subs alpha 0 (dec (count alpha)))) 100.0)
                          (safe-parse-double alpha))]
                  [(clamp01 (parse-comp (nth parts 0)))
                   (clamp01 (parse-comp (nth parts 1)))
                   (clamp01 (parse-comp (nth parts 2)))
                   (clamp01 (or a 1.0))])))
            ;; Comma-separated: rgb(255, 128, 0) or rgba(255, 128, 0, 0.5)
            ;; Also handles space-separated without slash: rgb(255 128 0)
            (let [parts (if (str/includes? inner ",")
                          (mapv str/trim (str/split inner #","))
                          (str/split (str/trim inner) #"\s+"))
                  parse-comp (fn [v]
                               (if (str/ends-with? v "%")
                                 (/ (safe-parse-double (subs v 0 (dec (count v)))) 100.0)
                                 (/ (or (safe-parse-double v) 0) 255.0)))]
              (case (count parts)
                3 [(clamp01 (parse-comp (nth parts 0)))
                   (clamp01 (parse-comp (nth parts 1)))
                   (clamp01 (parse-comp (nth parts 2)))
                   1.0]
                4 (let [a-str (nth parts 3)
                        a (if (str/ends-with? a-str "%")
                            (/ (safe-parse-double (subs a-str 0 (dec (count a-str)))) 100.0)
                            (safe-parse-double a-str))]
                    [(clamp01 (parse-comp (nth parts 0)))
                     (clamp01 (parse-comp (nth parts 1)))
                     (clamp01 (parse-comp (nth parts 2)))
                     (clamp01 (or a 1.0))])
                nil))))

        ;; HSL/HSLA
        (or (str/starts-with? s "hsl(") (str/starts-with? s "hsla("))
        (let [inner (-> s
                        (str/replace #"^hsla?\(" "")
                        (str/replace #"\)$" "")
                        str/trim)
              ;; Handle both comma and space/slash separators
              [color-part alpha-part] (if (str/includes? inner "/")
                                        (str/split inner #"/")
                                        [inner nil])
              parts (if (str/includes? (str/trim color-part) ",")
                      (mapv str/trim (str/split (str/trim color-part) #","))
                      (str/split (str/trim color-part) #"\s+"))
              parse-num (fn [v]
                          (safe-parse-double (str/replace v #"[°%deg]" "")))]
          (when (>= (count parts) 3)
            (let [h (or (parse-num (nth parts 0)) 0)
                  s-val (or (parse-num (nth parts 1)) 0)
                  l (or (parse-num (nth parts 2)) 0)
                  [r g b] (hsl->rgb h s-val l)
                  a (cond
                      alpha-part
                      (let [at (str/trim alpha-part)]
                        (if (str/ends-with? at "%")
                          (/ (safe-parse-double (subs at 0 (dec (count at)))) 100.0)
                          (safe-parse-double at)))
                      (= 4 (count parts))
                      (let [at (nth parts 3)]
                        (if (str/ends-with? at "%")
                          (/ (safe-parse-double (subs at 0 (dec (count at)))) 100.0)
                          (safe-parse-double at)))
                      :else 1.0)]
              [r g b (clamp01 (or a 1.0))])))

        :else nil))))

;; ---------------------------------------------------------------------------
;; Length / numeric parsers
;; ---------------------------------------------------------------------------

(defn parse-px
  "Parse a CSS length string → number (px). Handles px, rem (×16), em (×16), bare numbers."
  [s]
  (when (and s (string? s) (not (str/blank? s)))
    (let [s (str/trim (str/lower-case s))]
      (cond
        (= s "0")                          0
        (str/ends-with? s "px")            (safe-parse-double (subs s 0 (- (count s) 2)))
        (str/ends-with? s "rem")           (when-let [n (safe-parse-double (subs s 0 (- (count s) 3)))]
                                             (* n 16))
        (str/ends-with? s "em")            (when-let [n (safe-parse-double (subs s 0 (- (count s) 2)))]
                                             (* n 16))
        :else                              (safe-parse-double s)))))

(defn parse-font-weight
  "Parse CSS font-weight → number. 'normal'→400, 'bold'→700, numeric string."
  [s]
  (when (and s (string? s) (not (str/blank? s)))
    (let [s (str/trim (str/lower-case s))]
      (case s
        "normal"  400
        "bold"    700
        "lighter" 300
        "bolder"  700
        (safe-parse-double s)))))

;; ---------------------------------------------------------------------------
;; Box model parsers
;; ---------------------------------------------------------------------------

(defn parse-padding
  "Parse 4 CSS padding strings (top, right, bottom, left) → [t r b l] numbers."
  [top right bottom left]
  [(or (parse-px top) 0)
   (or (parse-px right) 0)
   (or (parse-px bottom) 0)
   (or (parse-px left) 0)])

(defn parse-border-widths
  "Parse 4 CSS border-width strings → [t r b l] numbers."
  [top right bottom left]
  [(or (parse-px top) 0)
   (or (parse-px right) 0)
   (or (parse-px bottom) 0)
   (or (parse-px left) 0)])

(defn parse-border-color
  "Parse 4 CSS border-color strings → first non-nil parsed [r g b a]."
  [top right bottom left]
  (some parse-color [top right bottom left]))

(defn parse-border-radius
  "Parse 4 CSS border-radius corner strings → {:uniform N} or {:corners [tl tr br bl]}."
  [tl tr br bl]
  (let [vals [(or (parse-px tl) 0) (or (parse-px tr) 0) (or (parse-px br) 0) (or (parse-px bl) 0)]]
    (if (apply = vals)
      {:uniform (first vals)}
      {:corners vals})))

;; ---------------------------------------------------------------------------
;; Box shadow
;; ---------------------------------------------------------------------------

(defn- split-outside-parens
  "Split string on commas that are NOT inside parentheses."
  [s]
  (loop [i 0 depth 0 start 0 result []]
    (if (>= i (count s))
      (conj result (subs s start))
      (let [c (nth s i)]
        (cond
          (= c \() (recur (inc i) (inc depth) start result)
          (= c \)) (recur (inc i) (max 0 (dec depth)) start result)
          (and (= c \,) (zero? depth))
          (recur (inc i) depth (inc i) (conj result (subs s start i)))
          :else (recur (inc i) depth start result))))))

(defn- parse-single-shadow
  "Parse a single CSS box-shadow value → shadow map or nil (for inset)."
  [s]
  (let [s (str/trim s)]
    (when-not (str/blank? s)
      ;; Skip inset shadows
      (when-not (str/includes? s "inset")
        ;; Strategy: extract color tokens (rgb/rgba/hsl/hsla/hex/named), rest are numbers
        (let [;; Extract function-call colors first (e.g., rgba(...))
              color-fn-re #"(?:rgba?|hsla?)\([^)]+\)"
              color-fn-match (re-find color-fn-re s)
              remaining (if color-fn-match
                          (str/replace s color-fn-re "")
                          s)
              ;; Tokenize remaining
              tokens (filterv #(not (str/blank? %)) (str/split (str/trim remaining) #"\s+"))
              ;; Separate color tokens from number tokens
              color-token (when-not color-fn-match
                            (first (filter #(or (str/starts-with? % "#")
                                                (contains? named-colors (str/lower-case %)))
                                           tokens)))
              num-tokens (filterv #(and (not (str/starts-with? % "#"))
                                        (not (contains? named-colors (str/lower-case %))))
                                  tokens)
              ;; Parse numeric values: offset-x offset-y [blur [spread]]
              nums (mapv #(or (parse-px %) 0) num-tokens)
              color-str (or color-fn-match color-token)]
          (when (>= (count nums) 2)
            (cond-> {:offset [(nth nums 0) (nth nums 1)]
                     :blur   (or (get nums 2) 0)
                     :spread (or (get nums 3) 0)}
              color-str (assoc :color color-str))))))))

(defn parse-box-shadow
  "Parse CSS box-shadow string → vector of shadow maps matching IR format.
   Each shadow: {:offset [x y] :blur N :spread N :color \"css-string\"}
   Returns nil for 'none' or empty."
  [s]
  (when (and s (string? s) (not (str/blank? s)))
    (let [s (str/trim s)]
      (when (and (not= s "none") (not= s ""))
        (let [shadows (->> (split-outside-parens s)
                           (keep parse-single-shadow)
                           vec)]
          (when (seq shadows)
            shadows))))))

;; ---------------------------------------------------------------------------
;; Gradient
;; ---------------------------------------------------------------------------

(def ^:private direction-angles
  {"to top"          (* Math/PI 0)        ;; 0deg
   "to right"        (* Math/PI 0.5)      ;; 90deg
   "to bottom"       (* Math/PI 1)        ;; 180deg
   "to left"         (* Math/PI 1.5)      ;; 270deg
   "to top right"    (* Math/PI 0.25)
   "to bottom right" (* Math/PI 0.75)
   "to bottom left"  (* Math/PI 1.25)
   "to top left"     (* Math/PI 1.75)})

(defn parse-gradient
  "Parse CSS linear-gradient(...) → {:type :linear :angle N :stops [{:at N :value \"color\"}]}
   Angle in radians. Returns nil for non-linear or unparseable."
  [s]
  (when (and s (string? s))
    (let [s (str/trim s)]
      (when (str/starts-with? (str/lower-case s) "linear-gradient(")
        (let [inner (-> s
                        (str/replace #"^[Ll]inear-gradient\(" "")
                        (str/replace #"\)$" "")
                        str/trim)
              parts (split-outside-parens inner)
              first-part (str/trim (first parts))
              ;; Check if first part is angle/direction
              angle-deg (cond
                          (str/ends-with? first-part "deg")
                          (safe-parse-double (subs first-part 0 (- (count first-part) 3)))

                          (str/ends-with? first-part "rad")
                          nil ;; handled separately below

                          (contains? direction-angles (str/lower-case first-part))
                          nil ;; handled separately below

                          :else nil)
              angle-rad (cond
                          angle-deg
                          (* angle-deg (/ Math/PI 180.0))

                          (str/ends-with? first-part "rad")
                          (safe-parse-double (subs first-part 0 (- (count first-part) 3)))

                          (contains? direction-angles (str/lower-case first-part))
                          (get direction-angles (str/lower-case first-part))

                          :else nil)
              ;; If we consumed an angle, stops start at index 1; otherwise index 0
              has-angle? (some? angle-rad)
              stop-parts (if has-angle? (rest parts) parts)
              ;; Parse stops: "color position?" pairs
              stops (vec (keep-indexed
                           (fn [i part]
                             (let [part (str/trim part)
                                   ;; Try to extract a trailing percentage/px position
                                   pos-match (re-find #"(\d+(?:\.\d+)?)\s*%\s*$" part)
                                   at (if pos-match
                                        (/ (safe-parse-double (second pos-match)) 100.0)
                                        ;; Auto-distribute
                                        (if (> (count stop-parts) 1)
                                          (/ (double i) (dec (count stop-parts)))
                                          0.0))
                                   color-str (if pos-match
                                               (str/trim (subs part 0 (- (count part) (count (first pos-match)))))
                                               part)]
                               {:at (clamp01 at) :value color-str}))
                           stop-parts))]
          {:type  :linear
           :angle (or angle-rad (* Math/PI 1)) ;; default 180deg = top-to-bottom
           :stops stops})))))

;; ---------------------------------------------------------------------------
;; Layout property parsers
;; ---------------------------------------------------------------------------

(defn parse-direction
  "CSS flex-direction → :row or :column."
  [s]
  (when (and s (string? s))
    (case (str/trim (str/lower-case s))
      "row"            :row
      "row-reverse"    :row
      "column"         :column
      "column-reverse" :column
      nil)))

(defn parse-align
  "CSS align-items → :start, :center, or :end."
  [s]
  (when (and s (string? s))
    (case (str/trim (str/lower-case s))
      "flex-start" :start
      "start"      :start
      "center"     :center
      "flex-end"   :end
      "end"        :end
      "stretch"    :start
      "baseline"   :start
      nil)))

(defn parse-display
  "CSS display → :flex, :block, :inline, or :none."
  [s]
  (when (and s (string? s))
    (let [s (str/trim (str/lower-case s))]
      (cond
        (str/includes? s "flex")   :flex
        (str/includes? s "grid")   :flex  ;; treat grid as flex for our layout engine
        (= s "none")               :none
        (str/includes? s "inline") :inline
        :else                      :block))))
