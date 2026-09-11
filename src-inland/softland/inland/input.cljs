(ns softland.inland.input
  "Native editing delivery only. Text, caret, selection and controls are
   rendered by Softland. Each mounted field owns and removes its hidden input."
  (:require [missionary.core :as m]
            [app.client.text.layout :as layout]))

(defn field [id !draft value-key]
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

(defn sync! [el value disabled?]
  (when (not= (.-value el) value) (set! (.-value el) value))
  (set! (.-disabled el) (boolean disabled?)))

(defn focus! [field-id positioned [x y]]
  (when-let [el (.getElementById js/document field-id)]
    (when-not (.-disabled el)
      (.focus el #js {:preventScroll true})
      (when positioned
        (let [hit (layout/hit-test-result positioned [x y])
              at (or (layout/source-index-offset (:index hit)) (count (.-value el)))]
          (.setSelectionRange el at at)
          (.dispatchEvent el (js/Event. "select")))))))

(defn caret [positioned offset]
  (let [{:keys [line col]} (layout/source-offset->line-col positioned offset)]
    (:rect (layout/caret-result positioned line col))))

(defn selection [positioned start end]
  (vec
    (keep (fn [line]
            (let [[a b] (layout/line-source-bounds (:source-range line))
                  lo (max start a) hi (min end b)]
              (when (< lo hi)
                [(:line/index line)
                 (:rect (layout/selection-result positioned (:line/index line) (- lo a) (- hi a)))])))
      (:lines positioned))))
