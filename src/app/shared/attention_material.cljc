(ns app.shared.attention-material
  "Facet 2: revisioned policy for the block's hit box and attention-only
   interaction border. Hover/focus remain ephemeral mechanism; these values
   are the shared material policy they reveal.

   P5 adds grammar v1: attention already owned the hit box, so it owns what
   landing attention on that box MEANS. Its rows are filed under two sites —
   a user block and a machine block are different hit areas, and the kernel
   decides which one a rendered block claims at build time, so the dispatch
   function carries no machine?/user? branch at all."
  (:require [app.shared.binding-material :as binding-material]
            [app.shared.facet-material :as facet-material]))

(def master-id "fm:attention")
(def grammar-version 0)
(def code-floor-revision-id "code-floor:fm:attention:v0")
(def bindings-grammar-version 1)
(def bindings-code-floor-revision-id "code-floor:fm:attention:v1")
;; P6 · T10 — v2 adds ONE refusal to the v1 grammar (a row whose site cannot
;; feed its verb's required args). v1 keeps its own declaration below and is
;; never re-read through v2's validator.
(def strict-bindings-grammar-version 2)
(def strict-bindings-code-floor-revision-id "code-floor:fm:attention:v2")

(def default-form
  {:facet-master/id master-id
   :facet-master/grammar grammar-version
   :facet-master/facet :attention
   :facet-master/merge :append
   :facet-master/priority 20
   :attention/hit-padding 8.0
   :attention/border-width 1.0
   :attention/border-color [0.45 0.52 0.66 0.55]
   :attention/background [0.0 0.0 0.0 0.0]})

(def default-source (pr-str default-form))

(def bindings-form
  "v1 = v0 plus the block hit-area gesture rows, each extracted verbatim from a
   pre-P5 branch:

   - user, press/:begin, shift  ← pointer-down!'s `(when (and text? (not= uid
     focus)) …)` (Task 11: shift focuses in the same gesture)
   - user, press/:threshold, shift  ← pointer-move! :pending `(:text? p)`
   - user, tap  ← pointer-up! :pending non-machine branch
   - machine, press/:threshold, shift  ← pointer-move! :pending `(:mtext? p)`
   - machine, tap  ← pointer-up! :pending machine branch with no fold row

   A machine block carries NO press/:begin row, which is exactly the pre-P5
   silence: shift-pressing a machine block never focused it."
  (assoc default-form
         :facet-master/grammar bindings-grammar-version
         :facet-master/bindings
         {:block/user-hit-area
          [{:binding/gesture :pointer/press
            :binding/phase :begin
            :binding/modifiers #{:shift}
            :binding/verb {:verb/name :focus/enter-block :verb/version 0}
            :binding/priority 10}
           {:binding/gesture :pointer/press
            :binding/phase :threshold
            :binding/modifiers #{:shift}
            :binding/verb {:verb/name :selection/text-begin :verb/version 0}
            :binding/priority 10}
           {:binding/gesture :pointer/tap
            :binding/phase :complete
            :binding/modifiers :any
            :binding/verb {:verb/name :focus/place-caret :verb/version 0}
            :binding/priority 10}]
          :block/machine-hit-area
          [{:binding/gesture :pointer/press
            :binding/phase :threshold
            :binding/modifiers #{:shift}
            :binding/verb {:verb/name :selection/machine-begin
                           :verb/version 0}
            :binding/priority 10}
           {:binding/gesture :pointer/tap
            :binding/phase :complete
            :binding/modifiers :any
            :binding/verb {:verb/name :focus/release :verb/version 0}
            :binding/priority 10}]}))

(def bindings-source (pr-str bindings-form))

(def strict-bindings-form
  "v2 = v1's rows VERBATIM under the stricter grammar. The bytes differ from v1
   only in the grammar version, so this migration adds a refusal and changes no
   behavior: every row that shipped still validates."
  (assoc bindings-form
         :facet-master/grammar strict-bindings-grammar-version))

(def strict-bindings-source (pr-str strict-bindings-form))

(def ^:private v0-grammar
  {:material-keys
   #{:facet-master/merge
     :facet-master/priority
     :attention/hit-padding
     :attention/border-width
     :attention/border-color
     :attention/background}
   :validators
   {:facet-master/merge
    {:valid? #{:append}
     :error-type :facet-master/merge-invalid}
    :facet-master/priority
    {:valid? facet-material/integer-number?
     :error-type :facet-master/priority-invalid}
    :attention/hit-padding
    {:valid? facet-material/non-negative-number?
     :error-type :attention/hit-padding-invalid}
    :attention/border-width
    {:valid? facet-material/non-negative-number?
     :error-type :attention/border-width-invalid}
    :attention/border-color
    {:valid? facet-material/valid-rgba?
     :error-type :attention/border-color-invalid}
    :attention/background
    {:valid? facet-material/valid-rgba?
     :error-type :attention/background-invalid}}})

(def spec
  {:facet-master/id master-id
   :facet-master/facet :attention
   :facet-master/source-ref "softland://facet-master/attention"
   :facet-master/default-form default-form
   :facet-master/floor-form strict-bindings-form
   :facet-master/code-floor-revision-id
   strict-bindings-code-floor-revision-id
   :facet-master/grammars
   {grammar-version v0-grammar
    bindings-grammar-version
    {:material-keys
     (conj (:material-keys v0-grammar) :facet-master/bindings)
     :validators
     (assoc (:validators v0-grammar)
            :facet-master/bindings binding-material/bindings-validator)}
    ;; v1 above stays EXACTLY as it shipped. A durable v1 revision is compiled
    ;; under v1's declaration forever; v2 is an additional entry, never a
    ;; rewrite of an existing one (P3's per-version grammar law).
    strict-bindings-grammar-version
    {:material-keys
     (conj (:material-keys v0-grammar) :facet-master/bindings)
     :validators
     (assoc (:validators v0-grammar)
            :facet-master/bindings
            binding-material/strict-bindings-validator)}}})

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
