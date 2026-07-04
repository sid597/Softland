(ns app.client.workspace.trail-face.sanitize
  "Codepoint-level op sanitizer (V3-5 client half).
   Pure data-in/data-out: the coverage set is PASSED IN (loaded by
   wiring.cljs / tests); no atoms, no asset IO here (S2).
   Law (face law 9 / INV-9): no content past the shaper the atlas cannot
   draw — uncovered codepoints render the fallback glyph, never vanish;
   advance counting is codepoint-correct (surrogate pair = one advance)."
  (:require [clojure.string :as str]))

(def fallback-cp
  "U+FFFD REPLACEMENT CHARACTER — the honest-tofu substitute."
  0xFFFD)

(defn codepoint-segments
  "THE one platform shim (S2 carve-out, named): walk s by Unicode
   codepoint, surrogate-pair-safe. Returns a vector of
   {:cp <int> :seg <string>} — :seg is the original char sequence for
   that codepoint (1 or 2 UTF-16 units)."
  [s]
  #?(:clj  (let [s ^String s
                 n (.length s)]
             (loop [i 0 acc []]
               (if (< i n)
                 (let [cp (.codePointAt s i)
                       j  (+ i (Character/charCount cp))]
                   (recur j (conj acc {:cp cp :seg (subs s i j)})))
                 acc)))
     :cljs (let [n (.-length s)]
             (loop [i 0 acc []]
               (if (< i n)
                 (let [cp (.codePointAt s i)
                       j  (+ i (if (> cp 0xFFFF) 2 1))]
                   (recur j (conj acc {:cp cp :seg (subs s i j)})))
                 acc)))))

(defn codepoints
  "Seq of ints, one per Unicode codepoint (NOT UTF-16 code unit)."
  [s]
  (mapv :cp (codepoint-segments s)))

(defn codepoint-count
  "Codepoint count of s == rendered advance count (surrogate pair = 1)."
  [s]
  (count (codepoint-segments s)))

(defn coverage-set
  "Atlas JSON (parsed) -> #{codepoint}. Reads glyphs[].unicode; tolerant
   of string or keyword keys. The LOADING of the JSON happens outside
   this ns (wiring.cljs / test)."
  [atlas-json]
  (let [glyphs (or (get atlas-json "glyphs") (get atlas-json :glyphs))]
    (into #{}
          (keep (fn [g] (or (get g "unicode") (get g :unicode))))
          glyphs)))

(defn sanitize-text
  "Codepoint-wise: covered codepoints pass through verbatim; uncovered
   are substituted with fallback-cp. Codepoint COUNT and positions are
   preserved — never dropped, never zero-width.
   Control whitespace (< U+0020: \\n \\r \\t) passes through UNTOUCHED —
   the renderer owns line breaks; tofu-ing a newline turns paragraph
   breaks into fallback glyphs (first-light finding F-L1)."
  [coverage fallback-cp s]
  (let [fb (str (char fallback-cp))]
    (->> (codepoint-segments s)
         (map (fn [{:keys [cp seg]}]
                (cond
                  (< cp 0x20)             seg
                  (contains? coverage cp) seg
                  :else                   fb)))
         (str/join))))

(defn sanitize-op
  "Sanitize a text op's :text against the coverage set (OP-2). Updates
   :to when present (UTF-16 bookkeeping may shrink when an astral pair
   collapses to U+FFFD; codepoint count is invariant)."
  [coverage fallback-cp op]
  (if-let [t (:text op)]
    (let [t' (sanitize-text coverage fallback-cp t)]
      (cond-> (assoc op :text t')
        (contains? op :to) (assoc :to (count t'))))
    op))

(defn op-advance-count
  "Rendered advance count of an op == codepoint count of its text (OP-3).
   Downstream clip/wrap/hit math must use THIS, not UTF-16 `count`."
  [op]
  (codepoint-count (:text op "")))
