(ns app.shared.foldable-material
  "Facet 3: revisioned policy for the two run-section headers and their
   initial fold state. Per-appearance toggles remain ephemeral client state;
   this master supplies only the shared defaults and visible header copy.

   P5 adds grammar v1: the header rows' MEANING joins their copy. Foldable
   already owned the header vocabulary, so it owns the gesture that folds
   them — the tap row lives here, not in the kernel."
  (:require [app.shared.binding-material :as binding-material]
            [app.shared.facet-material :as facet-material]))

(def master-id "fm:foldable")
(def grammar-version 0)
(def code-floor-revision-id "code-floor:fm:foldable:v0")
(def bindings-grammar-version 1)
(def bindings-code-floor-revision-id "code-floor:fm:foldable:v1")

(def default-form
  {:facet-master/id master-id
   :facet-master/grammar grammar-version
   :facet-master/facet :foldable
   :foldable/defaults {:noise? false :prose? false}
   :foldable/header-copy
   {:noise-label "thinking+tools"
    :prose-label "reply"
    :collapsed-marker "▸ "
    :expanded-marker "▾ "
    :show-suffix " — click to show"
    :hide-suffix " — click to hide"
    :line-count-prefix " (+"
    :line-count-suffix " lines)"}})

(def default-source (pr-str default-form))

(def bindings-form
  "v1 = v0 plus the fold-header gesture row, extracted verbatim from the
   pre-P5 `(when (and run? (<= row 1)) …)` branch. The row does not name the
   section: the header NODE hit is the section, so the kernel's row arithmetic
   is deleted rather than relocated."
  (assoc default-form
         :facet-master/grammar bindings-grammar-version
         :facet-master/bindings
         {:block/fold-header
          [{:binding/gesture :pointer/tap
            :binding/phase :complete
            :binding/modifiers :any
            :binding/verb {:verb/name :fold/toggle-section
                           :verb/version 0}
            :binding/priority 10}]}))

(def bindings-source (pr-str bindings-form))

(defn- exact-map?
  [x ks value-valid?]
  (and (map? x)
       (= ks (set (keys x)))
       (every? value-valid? (vals x))))

(defn- valid-defaults?
  [x]
  (exact-map? x #{:noise? :prose?} boolean?))

(defn- valid-header-copy?
  [x]
  (exact-map?
   x
   #{:noise-label
     :prose-label
     :collapsed-marker
     :expanded-marker
     :show-suffix
     :hide-suffix
     :line-count-prefix
     :line-count-suffix}
   string?))

(def ^:private v0-grammar
  {:material-keys
   #{:foldable/defaults
     :foldable/header-copy}
   :validators
   {:foldable/defaults
    {:valid? valid-defaults?
     :error-type :foldable/defaults-invalid}
    :foldable/header-copy
    {:valid? valid-header-copy?
     :error-type :foldable/header-copy-invalid}}})

(def spec
  {:facet-master/id master-id
   :facet-master/facet :foldable
   :facet-master/source-ref "softland://facet-master/foldable"
   ;; default-form stays v0: `ensure-master!` must keep minting the exact bytes
   ;; already durable on the cluster. v1 arrives as an explicit migration
   ;; (ensure-active-source!), never as a reinterpretation of v0.
   :facet-master/default-form default-form
   ;; the FLOOR carries the rows, so a malformed or absent revision still
   ;; folds headers — that is the unbreakable half of the binding fence
   :facet-master/floor-form bindings-form
   :facet-master/code-floor-revision-id bindings-code-floor-revision-id
   :facet-master/grammars
   {grammar-version v0-grammar
    bindings-grammar-version
    {:material-keys
     (conj (:material-keys v0-grammar) :facet-master/bindings)
     :validators
     (assoc (:validators v0-grammar)
            :facet-master/bindings binding-material/bindings-validator)}}})

(defn compile-form
  [form]
  (facet-material/compile-form spec form))

(defn compile-source
  [source]
  (facet-material/compile-source spec source))

(def code-floor
  (facet-material/code-floor spec))

(defn resolved-wear
  [served]
  (facet-material/resolved-wear spec served))

(defn contribution-stamp
  [wear subject site role slot]
  (facet-material/contribution-stamp wear subject site role slot))
