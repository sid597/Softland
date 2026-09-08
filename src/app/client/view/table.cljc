(ns app.client.view.table
  "The view's vocabulary: the path kind's table plus the words the text tool
   needed as records. Takes named arguments; gives values. Holds nothing but
   the fonts a table was built with. Evidence: run_test.clj and the browser
   entry.

   :text/shape — one run's keystroke records, folded after deletes, to one
   glyph item per character: advance, bounding box and outline in the font's
   units, and whether it inks. A font record with inline :glyphs (the box
   font) answers from itself; a font record with a :source answers from the
   TrueType file the table was built with (text/truetype.cljc). The
   browser's HarfBuzz shaper is not bound here: no kerning, no ligatures,
   no bidi.
   :path/place — an outline in font units, y up, placed at a point and scaled
   into local units, y down.
   :collect — one loop step that emits a collection, so a layout can hand its
   placements to the tools that paint, hit and caret over them.
   :value — a step that names what it is given. The leaves have no :let, so
   without it the pen expression is embedded in every place that reads it
   and the executor evaluates the whole tree at every step."
  (:require [app.client.path.construction :as construction]
            [app.client.path.value :as value]
            [app.client.text.truetype :as truetype]))

(def vocabulary
  "view 2: path kind production 2 · shape = box glyphs or TrueType outlines per character, Backspace deletes, Enter breaks · place = translate and scale, y flipped · collect = conj, nil skips · value = identity")

(defn fold-keys
  "Keystroke records in order → the characters that stand after deletes.
   Enter stands as a newline character; anything else without :ch is skipped."
  [keys]
  (reduce (fn [chars {:keys [ch key]}]
            (cond (= key "Backspace") (if (seq chars) (pop chars) chars)
                  (= key "Enter") (conj chars "\n")
                  (string? ch) (conj chars ch)
                  :else chars))
          [] keys))

(def break-glyph {:advance 0 :bbox [0 0 0 0] :outline {:subpaths []} :ink 0 :break 1})

(defn glyph-of
  "A font record, the fonts a table holds and one character → the glyph:
   advance, bounding box, outline and ink, in the font's units."
  [font fonts ch]
  (cond
    (:glyphs font) (assoc (or (get-in font [:glyphs ch]) (:default font)) :break 0)
    (:source font) (let [parsed (or (get fonts (:id font))
                                    (throw (ex-info "The font's file is not loaded" {:error-type :font/unloaded :font (:id font)})))
                         {:keys [advance bbox outline]} (truetype/glyph-for-char parsed ch)]
                     {:advance advance :bbox bbox :outline outline :ink (if (seq (:subpaths outline)) 1 0) :break 0})
    :else (throw (ex-info "A font record needs :glyphs or a :source" {:error-type :font/shape :font (:id font)}))))

(defn- words
  "Glyph items in order → the same items with :word-start (1 at the first
   glyph after whitespace or a break, else 0) and :word-advance (the sum of
   advances from this glyph to the next whitespace or break, in font
   units), so a layout can keep a word whole where it fits."
  [items]
  (let [gap? (fn [g] (or (= 0 (:ink g)) (= 1 (:break g))))
        advances (loop [k (dec (count items)) run 0 acc (list)]
                   (if (neg? k) (vec acc)
                       (let [g (nth items k) run (if (gap? g) 0 (+ run (:advance g)))]
                         (recur (dec k) run (conj acc run)))))]
    (vec (map-indexed (fn [k g]
                        (assoc g :word-start (if (and (not (gap? g)) (or (zero? k) (gap? (nth items (dec k))))) 1 0)
                                 :word-advance (nth advances k)))
                      items))))

(defn shape
  "One run record, its font record and the fonts a table holds → one glyph
   item per character that stands after deletes, each carrying the run's id,
   origin and asserter, and its word's start and advance. A layout loops
   over one run: a key appended to it is then a tail append and its layout
   resumes with the new glyph alone (a key that changes the last word's
   advance changes that word's earlier items, and the layout reruns)."
  [run font fonts]
  (words (vec (for [[i ch] (map-indexed vector (fold-keys (:keys run)))
                    :let [g (if (= ch "\n") break-glyph (glyph-of font fonts ch))]]
                (assoc g :i i :ch ch :run (:id run) :at (:at run) :by (:by run))))))

(defn place
  "A path in font units (y up), a local point and a scale → the path in
   local units (y down) with its origin at the point."
  [path [ax ay] scale]
  (value/map-points path (fn [[x y]] [(+ ax (* scale x)) (- ay (* scale y))])))

(defn with-fonts
  "The fonts loaded from their files, by font record id → the capability
   table the view's tools run over."
  [fonts]
  (merge construction/capabilities
         {:vocabulary vocabulary
          :text/shape {:args [:run :font] :needs [[:run] [:font]]
                       :run (fn [{:keys [run font]} _] (shape run font fonts))}
          :path/place {:args [:path :at :scale] :needs [[:path] [:at] [:scale]]
                       :run (fn [{:keys [path at scale]} _] (place path at scale))}
          :collect {:args [:into :item] :needs [[:into] [:item]]
                    :run (fn [{:keys [into item]} _] (if (nil? item) into (conj into item)))}
          :value {:args [:value] :needs [[:value]]
                  :run (fn [{:keys [value]} _] value)}}))

(def table
  "The table with no font files loaded: the box font still shapes."
  (with-fonts {}))
