(ns app.client.view.table
  "The view's vocabulary: the path kind's table plus the two words the text
   tool needed as records. Takes named arguments; gives values. Holds nothing.
   Evidence: run_test.clj and the browser entry.

   :text/shape — one run's keystroke records, folded after deletes, to one
   glyph item per character with the box font's metrics. The browser's
   HarfBuzz shaper (text/shaper.cljs) is not bound here yet; this is the JVM
   and browser stand-in for it.
   :collect — one loop step that emits a collection, so a layout can hand its
   placements to the tools that paint, hit and caret over them.
   :value — a step that names what it is given. The leaves have no :let, so
   without it the pen expression is embedded in every place that reads it
   and the executor evaluates the whole tree at every step."
  (:require [app.client.path.construction :as construction]))

(def vocabulary
  "view 1: path kind production 2 · shape = box font per character, Backspace deletes, Enter breaks · collect = conj, nil skips · value = identity")

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

(defn shape
  "One run record and a box font → one glyph item per character that stands
   after deletes, each carrying the run's id, origin and asserter. A layout
   loops over one run: a key appended to it is then a tail append and its
   layout resumes with the new glyph alone."
  [run font]
  (vec (for [[i ch] (map-indexed vector (fold-keys (:keys run)))
             :let [g (if (= ch "\n")
                       {:advance 0 :box [0 0 0 0] :ink 0 :break 1}
                       (assoc (or (get-in font [:glyphs ch]) (:default font)) :break 0))]]
         (assoc g :i i :ch ch :run (:id run) :at (:at run) :by (:by run)))))

(def table
  "The path kind's capabilities plus shape and collect."
  (merge construction/capabilities
         {:vocabulary vocabulary
          :text/shape {:args [:run :font] :needs [[:run] [:font]]
                       :run (fn [{:keys [run font]} _] (shape run font))}
          :collect {:args [:into :item] :needs [[:into] [:item]]
                    :run (fn [{:keys [into item]} _] (if (nil? item) into (conj into item)))}
          :value {:args [:value] :needs [[:value]]
                  :run (fn [{:keys [value]} _] value)}}))
