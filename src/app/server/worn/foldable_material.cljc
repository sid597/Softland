(ns app.server.worn.foldable-material
  "The fold-state and section-header facet specification.
   Takes: served forms and binding rows for run-section headers.
   Gives: compiled fold values, header copy, interaction claims, and contribution rows.
   Holds: spec."
  (:require [app.server.worn.binding-material :as binding-material]
            [app.server.worn.facet-engine :as facet-engine]))

(def master-id "fm:foldable")
(def grammar-version 0)
(def code-floor-revision-id "code-floor:fm:foldable:v0")
(def bindings-grammar-version 1)
(def bindings-code-floor-revision-id "code-floor:fm:foldable:v1")
;; P6 · T10 — v2 adds ONE refusal to the v1 grammar (a row whose site cannot
;; feed its verb's required args). v1 keeps its own declaration below and is
;; never re-read through v2's validator.
(def strict-bindings-grammar-version 2)
(def strict-bindings-code-floor-revision-id "code-floor:fm:foldable:v2")
;; smalltalk-ui-vm P2 · W7/T7 — paste policy is born once as grammar v3.
;; Clamp fraction, threshold, and header copy are material from this point on.
(def paste-clamp-grammar-version 3)
(def paste-clamp-code-floor-revision-id "code-floor:fm:foldable:v3")

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

(def strict-bindings-form
  "v2 = v1's rows VERBATIM under the stricter grammar. The bytes differ from v1
   only in the grammar version, so this migration adds a refusal and changes no
   behavior: every row that shipped still validates."
  (assoc bindings-form
         :facet-master/grammar strict-bindings-grammar-version))

(def strict-bindings-source (pr-str strict-bindings-form))

(def paste-clamp-form
  "v3 = v2 plus the one outside-paste policy. Full pasted bytes remain durable;
   this policy decides only when and how much the collapsed projection shows."
  (assoc strict-bindings-form
         :facet-master/grammar paste-clamp-grammar-version
         :foldable/paste-clamp
         {:threshold-chars 1200
          :max-share 0.5
          :header-copy "pasted"}))

(def paste-clamp-source (pr-str paste-clamp-form))

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

(defn- valid-paste-clamp?
  [x]
  (and (map? x)
       (= #{:threshold-chars :max-share :header-copy} (set (keys x)))
       (facet-engine/integer-number? (:threshold-chars x))
       (pos? (:threshold-chars x))
       (number? (:max-share x))
       (pos? (:max-share x))
       (<= (:max-share x) 1)
       (string? (:header-copy x))
       (seq (:header-copy x))))

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
   ;; The FLOOR carries rows + paste policy, so malformed or absent revisions
   ;; retain both the interaction fence and outside-paste pressure.
   :facet-master/floor-form paste-clamp-form
   :facet-master/code-floor-revision-id
   paste-clamp-code-floor-revision-id
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
            binding-material/strict-bindings-validator)}
    paste-clamp-grammar-version
    {:material-keys
     (into (:material-keys v0-grammar)
           #{:facet-master/bindings :foldable/paste-clamp})
     :validators
     (assoc (:validators v0-grammar)
            :facet-master/bindings
            binding-material/strict-bindings-validator
            :foldable/paste-clamp
            {:valid? valid-paste-clamp?
             :error-type :foldable/paste-clamp-invalid})}}})

(defn compile-form
  [form]
  (facet-engine/compile-form spec form))

(defn compile-source
  [source]
  (facet-engine/compile-source spec source))

(def code-floor
  (facet-engine/code-floor spec))

(defn resolved-wear
  [served]
  (facet-engine/resolved-wear spec served))

(defn contribution-stamp
  [wear subject site role slot]
  (facet-engine/contribution-stamp wear subject site role slot))
