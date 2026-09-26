;; IMPORTANT: Before modifying this file, re-read PLAN-locks-and-forgetting.md,
;; "The shapes" (Grammar) and L12.
(ns rig.store.grammar
  "Which people a value names, by its key's grammar (ruling 8;
  PLAN-locks-and-forgetting.md, 'The shapes', L12). The grammar is data: a
  map from fact key to an entry, a constant for this stage. Stage 6 reads
  the same map from `:grammar` facts, and `subjects-of` does not change.
  Pure and total.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key.")

(def grammars
  "The one grammar the model has (`fact-subjects` in model.clj): a
  `:mention`'s `:persons` collection names people."
  {:mention {:subjects-at [:persons]}})

(defn subjects-of
  "The person ids value `v` of key `k` names under `grammars`:
  - a key with no entry names no one: #{};
  - a key with an entry: the keywords at the entry's `:subjects-at` in `v`,
    when `v` is a map and that position holds a vector or a set of
    keywords, as a set;
  - otherwise the keyword `:value-shape`, the refusal as data (L12): the
    gate must read this one shape to wrap the value, and a value it cannot
    read cannot be admitted under a wrong wrap.
  Total: never throws."
  [grammars k v]
  (try
    (if-let [at (get-in grammars [k :subjects-at])]
      (let [ps (if (map? v) (get-in v at ::absent) ::absent)]
        (if (and (or (set? ps) (vector? ps)) (every? keyword? ps))
          (set ps)
          :value-shape))
      #{})
    (catch Throwable _ :value-shape)))
