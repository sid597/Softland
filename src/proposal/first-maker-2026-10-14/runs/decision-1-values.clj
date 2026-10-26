;; decision-1: can an adapter's rule be held as a fact's value? A run, not code.
;; Run from the repository root: clojure -M:store src/proposal/first-maker-2026-10-14/runs/decision-1-values.clj
(require '[softland.store.act :as act])
(def md-rule-as-record
  {:cut :lines
   :part-starts {:pattern "^\\s{0,3}(#{1,6})\\s+(.*)$" :name-from 2 :level-from 1}
   :part-name :md/section})
(def clj-rule-as-record
  {:cut :forms
   :parts-are #{"defn" "defn-" "def" "defmacro" "defprotocol"}
   :name-from 1 :docstring-at 2})
(def clj-rule-as-a-list-form '(fn [form] (= 'defn (first form))))
(def as-vectors [:fn [:form] [:= [:literal "defn"] [:first [:get :form]]]])
(doseq [[label v] [["md rule, a map of strings and keywords" md-rule-as-record]
                   ["clj rule, a map with a set of strings" clj-rule-as-record]
                   ["a Clojure fn written as a list" clj-rule-as-a-list-form]
                   ["the same fn written as vectors" as-vectors]
                   ["a compiled regex object" #"^#+"]]]
  (println (format "%-42s value? %s" label (act/value? v))))
