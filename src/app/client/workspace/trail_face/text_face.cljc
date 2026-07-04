(ns app.client.workspace.trail-face.text-face
  "View-3 face: WP1 trail-text projection -> line ops (pure cljc).
   The projection is generated SERVER-side (WP1 §8 render-bundle-text);
   this face displays it VERBATIM — line-for-line, marker-colored,
   never re-wrapped (face law 2, trap 3). One marker->style table,
   keyed to the version header (face law 3, trap 12)."
  (:require [clojure.string :as str]))

(def version-header
  "The projection grammar version this marker table is keyed to."
  ";; trail-text v0")

(defn split-projection-lines
  "Split VERBATIM on newline, preserving empty and trailing lines:
   (str/join \"\\n\" (split-projection-lines t)) == t. Never re-wraps."
  [text]
  (str/split (or text "") #"\n" -1))

(defn classify-line
  "Line -> style keyword, total over ALL inputs (unknown -> :normal,
   never dropped). The marker vocabulary is WP1 §8 v0."
  [line]
  (let [trimmed (str/triml line)]
    (cond
      (str/starts-with? line ";; address:")      :address
      (str/starts-with? line ";;")               :meta
      (str/starts-with? line "==")               :heading
      (str/starts-with? line "--")               :section
      (str/starts-with? trimmed "->")            :relation-out
      (str/starts-with? trimmed "<-")            :relation-in
      (str/includes? line "(current")            :verdict
      (or (str/starts-with? trimmed "created ")
          (str/includes? line "walked unknown")) :stamps
      :else                                      :normal)))

(def marker->style
  "style-kw -> rgba. Keyed to `;; trail-text v0` (version-guard checks
   the header before this table applies). Every classify-line output has
   an entry; :normal is the total-function floor."
  {:meta         [0.55 0.55 0.60 1.0]
   :address      [0.55 0.70 0.95 1.0]
   :heading      [0.95 0.90 0.75 1.0]
   :section      [0.75 0.65 0.90 1.0]
   :relation-out [0.60 0.85 0.70 1.0]
   :relation-in  [0.60 0.75 0.90 1.0]
   :verdict      [0.95 0.70 0.55 1.0]
   :stamps       [0.65 0.65 0.65 1.0]
   :notice       [0.95 0.55 0.55 1.0]
   :normal       [0.85 0.85 0.85 1.0]})

(defn style->rgba
  "Total: unknown style keywords fall back to :normal's color."
  [style-kw]
  (get marker->style style-kw (marker->style :normal)))

(defn version-guard
  "Header check (face law 3): the first line must carry the version
   header this marker table is keyed to. Mismatch (or no header at all)
   -> the whole projection renders plain + exactly one notice line."
  [lines]
  (if (and (seq lines) (str/starts-with? (first lines) version-header))
    {:ok? true}
    {:ok? false
     :notice (str "!! trail-text version mismatch - expected \""
                  version-header "\" - rendering plain")}))

(defn projection->line-ops
  "WP1 render-bundle-text output -> {:ok? bool :ops [line-op] :notice}.
   line-op = {:line <idx> :text <verbatim line> :style <kw>}.
   ok path: one op per projection line, styles from the marker table.
   Mismatch path: ONE :notice op prepended, every projection line
   :normal — still verbatim, never dropped (honesty over prettiness)."
  [text]
  (let [lines (split-projection-lines text)
        guard (version-guard lines)]
    (if (:ok? guard)
      {:ok? true
       :ops (into []
                  (map-indexed (fn [i l] {:line i :text l :style (classify-line l)}))
                  lines)}
      {:ok? false
       :notice (:notice guard)
       :ops (into [{:line 0 :text (:notice guard) :style :notice}]
                  (map-indexed (fn [i l] {:line (inc i) :text l :style :normal}))
                  lines)})))

(defn address-header-op
  "The face's own address as ONE header text line (face law 1, gate 1).
   :text is the address printed as plain EDN — parsing it back with an
   EDN reader round-trips shape-equal (A1: pure EDN, no live resolve)."
  [address]
  {:text (pr-str address)
   :style :address
   :trail-face/address? true})
