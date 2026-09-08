(ns app.client.view.table
  "The view's vocabulary: the path kind's table plus the two words the text
   tool needed as records. Takes named arguments; gives values. Holds nothing.
   Evidence: run_test.clj and the browser entry.

   :text/shape — keystroke records of the runs a query named, folded after
   deletes, to one glyph item per character with the box font's metrics. The
   browser's HarfBuzz shaper (text/shaper.cljs) is not bound here yet; this is
   the JVM and browser stand-in for it.
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
  "Run ids, the store's records and a box font → glyph items across those
   runs in the order the ids name. Each item carries its run's id, origin and
   asserter so one loop can lay out several runs."
  [ids store font]
  (let [by-id (into {} (map (juxt :id identity)) store)]
    (vec (for [id ids
               :let [run (get by-id id)]
               :when run
               [i ch] (map-indexed vector (fold-keys (:keys run)))
               :let [g (if (= ch "\n")
                         {:advance 0 :box [0 0 0 0] :ink 0 :break 1}
                         (assoc (or (get-in font [:glyphs ch]) (:default font)) :break 0))]]
           (assoc g :i i :ch ch :run id :at (:at run) :by (:by run))))))

(def table
  "The path kind's capabilities plus shape and collect."
  (merge construction/capabilities
         {:vocabulary vocabulary
          :text/shape {:args [:ids :store :font] :needs [[:ids] [:store] [:font]]
                       :run (fn [{:keys [ids store font]} _] (shape ids store font))}
          :collect {:args [:into :item] :needs [[:into] [:item]]
                    :run (fn [{:keys [into item]} _] (if (nil? item) into (conj into item)))}
          :value {:args [:value] :needs [[:value]]
                  :run (fn [{:keys [value]} _] value)}}))
