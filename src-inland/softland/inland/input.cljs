(ns softland.inland.input
  "Browser-native editing delivery for Softland-painted text fields.
   Takes session draft cells and positioned text; gives edit/selection updates and
   caret/selection geometry. Each field owns a hidden textarea and its listeners.
   Borrows Softland text layout for addressing; no DOM text or controls provide the
   visible product. Browser selection offsets follow textarea source offsets."
  (:require [missionary.core :as m]
            [app.client.text.layout :as layout]))

(defn field
  "Id, draft atom and value key → flow retaining one hidden textarea.
   Input/select/focus events update the draft value, range and focus flag. Cancelling
   the flow removes every installed listener and the element."
  [id !draft value-key]
  (m/observe
    (fn [emit]
      (let [el (.createElement js/document "textarea")
            update (fn [_]
                     (swap! !draft assoc value-key (.-value el)
                       :selection [(.-selectionStart el) (.-selectionEnd el)]
                       :focused (= el (.-activeElement js/document))))]
        (set! (.-id el) id)
        (set! (.-className el) "native-input")
        (set! (.-spellcheck el) false)
        (.setAttribute el "aria-label" (if (= value-key :source) "Executable rule" "New tool name"))
        (doseq [event ["input" "select" "keyup" "focus" "blur"]] (.addEventListener el event update))
        (.appendChild (.-body js/document) el)
        (emit el)
        (fn []
          (doseq [event ["input" "select" "keyup" "focus" "blur"]] (.removeEventListener el event update))
          (.remove el))))))

(defn sync!
  "Textarea, current value and disabled flag → updated native input properties.
   Skips equal value assignment to preserve native selection; does not emit an edit."
  [el value disabled?]
  (when (not= (.-value el) value) (set! (.-value el) value))
  (set! (.-disabled el) (boolean disabled?)))

(defn focus!
  "Field id, positioned text and point → focus and collapsed native selection.
   Ignores missing/disabled fields; layout hit maps to source offset, falling back
   to end of input. Dispatches select so the session draft observes the new range."
  [field-id positioned [x y]]
  (when-let [el (.getElementById js/document field-id)]
    (when-not (.-disabled el)
      (.focus el #js {:preventScroll true})
      (when positioned
        (let [hit (layout/hit-test-result positioned [x y])
              at (or (layout/source-index-offset (:index hit)) (count (.-value el)))]
          (.setSelectionRange el at at)
          (.dispatchEvent el (js/Event. "select")))))))

(defn caret
  "Positioned layout and source offset → caret rectangle from Softland layout."
  [positioned offset]
  (let [{:keys [line col]} (layout/source-offset->line-col positioned offset)]
    (:rect (layout/caret-result positioned line col))))

(defn selection
  "Positioned layout and source range → [line-index rectangle] pairs.
   Includes only nonempty intersections with each line; returns no geometry for a
   collapsed range. Clipping to a field is the paint caller's responsibility."
  [positioned start end]
  (vec
    (keep (fn [line]
            (let [[a b] (layout/line-source-bounds (:source-range line))
                  lo (max start a) hi (min end b)]
              (when (< lo hi)
                [(:line/index line)
                 (:rect (layout/selection-result positioned (:line/index line) (- lo a) (- hi a)))])))
      (:lines positioned))))
