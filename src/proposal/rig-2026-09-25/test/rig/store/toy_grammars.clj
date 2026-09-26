(ns rig.store.toy-grammars
  "The formal model's two toy grammars as facts, for the earlier stages'
  suites (BUILD_NOTES-tools-and-grammars.md, D-P4). Before phase 6 the store
  knew them as compiled constants (phase 2's `grammar/grammars`, the read
  exit's `seed-hints`); since phase 6 the stream gate and the one-owner exit
  read a key's grammar only from facts in the layer, so a suite that relies
  on a `:mention` naming its people, or on `[:kv :note ..]` reads, writes
  these first, into each layer before that layer's first `:note` or
  `:mention` (a grammar changing a used key's hints is refused).

  The `:mention` grammar is the model's (README 46: a value that names one
  or two people); the `:note` grammar is a plain value indexed by value, as
  `seed-hints` had it. Test data, not store code: nothing under `src/`
  knows either key."
  (:require [rig.store.client :as c]
            [rig.store.grammar :as grammar]))

(def mention
  "The `:mention` grammar fact: an open map whose `:persons`, a set of one or
  two people, are the value's subjects."
  {:e :mention :k :grammar
   :v {:shape [:map {:persons [:set-of [:keyword] 1 2]} {:open? true}]
       :subjects-at [:persons]
       :opaque false
       :index #{}}})

(def note
  "The `:note` grammar fact: any value, indexed by value."
  {:e :note :k :grammar
   :v {:shape [:any] :subjects-at nil :opaque false :index #{:by-value}}})

(def facts "Both, in one act." [mention note])

(defn write!
  "One operator act per layer writing both toy grammars into it; the
  answers, in the layers' order."
  [store layers]
  (vec (for [l layers]
         (c/offer-until-answered!
          store (c/build {:who :operator :layer l :class (or (:class (c/settings store l)) :by-layer)
                          :facts facts})))))

(def ^:private grammar-act
  "A name standing for the act that wrote the toy grammars, for pure tests."
  [:alice :by-layer :offer #uuid "00000000-0000-8000-8000-000000000001"])

(defn rows
  "The key rows the toy grammars give a layer, `{k row}`, as the gate would
  have projected them (for pure tests that decide without a cluster)."
  []
  {:mention {:used false :grammar (grammar/row [grammar-act 0] 1 (:v mention))}
   :note {:used false :grammar (grammar/row [grammar-act 1] 1 (:v note))}})
