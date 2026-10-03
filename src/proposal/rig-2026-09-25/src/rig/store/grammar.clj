;; IMPORTANT: Before modifying this file, re-read PLAN-locks-and-forgetting.md,
;; "The shapes" (Grammar) and L12, and PLAN-tools-and-grammars.md 4.1 to 4.7.
(ns rig.store.grammar
  "A key's grammar: which people its values name (ruling 8), what shape they
  have (ruling 6's shape check), whether they are opaque, and how they are
  indexed (PLAN-tools-and-grammars.md 4; PLAN-locks-and-forgetting.md, 'The
  shapes', L12).

  Since phase 6 a grammar is a fact in the layer it governs (T-FR1, T-FR7):
  `{:e <the key's id> :k :grammar :v {:shape s :subjects-at p :opaque b
  :index #{h}}}`, a control fact the layer's gate admits (`refusal` here)
  and projects into a row per key, `[L :key-rows k]` (`row`, `key-row-writes`),
  read in the same event as the values it checks (`grammars-of`, `named`,
  `hints`). A key with no grammar in a layer is permissive (T-RC2). No copy
  of any layer's grammar is kept elsewhere (EXAMINATION-copies.md).

  Both gates and their read exits use layer rows. `grammars`, the model's
  former constant, remains only for legacy pure helper arities and fixtures;
  no topology calls those arities after step 6b. Pure and total.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [clojure.edn :as edn]
            [com.rpl.rama :refer [fixed-keys-schema map-schema set-schema vector-schema]]
            [rig.store.envelope :as env]
            [rig.store.shape :as shape]))

(def grammars
  "The one grammar the model has (`fact-subjects` in model.clj): a
  `:mention`'s `:persons` collection names people. Retained for legacy pure
  fixtures through compatibility arities; both gates pass layer grammars
  explicitly. Listed for removal, not deleted in step 6b."
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

;; ------------------------------------------------ the grammar fact (phase 6)

(def grammar-parts "A grammar value's parts, exactly (T-FR1)." #{:shape :subjects-at :opaque :index})

(def index-hints "The hints a grammar may give, the read exit's own names (4.5)." #{:by-value :no-copy})

(def max-named
  "The most people one subjects position may name: phase 2's cap on an
  act's subjects (L13, `locks/max-subjects`; this namespace cannot require
  rig.store.locks, which requires it)."
  256)

(defn- subjects-path-ok?
  "Whether `at` walks required entries of `[:map]` shapes from `shape` and
  ends on a `[:set-of [:keyword] min max]` or `[:vector-of [:keyword] min
  max]` with max at most 256 (4.3): then a value that passes the shape
  always yields its subjects."
  [shape at]
  (loop [s shape ks (seq at)]
    (cond
      (not (vector? s)) false
      (empty? ks) (boolean (and (contains? #{:set-of :vector-of} (first s))
                                (= [:keyword] (second s))
                                (<= (nth s 3) max-named)))
      (not= :map (first s)) false
      :else (let [[_ entries opts] s
                  k (first ks)]
              (if (and (contains? entries k) (not (contains? (:optional opts) k)))
                (recur (get entries k) (next ks))
                false)))))

(defn refusal
  "Why grammar value `g` is not a grammar the gate admits, or nil (4.2 to
  4.5): a map with exactly `:shape`, `:subjects-at`, `:opaque`, `:index`;
  the shape in the language and its bounds; `:opaque` a boolean; `:index` a
  subset of `#{:by-value :no-copy}`; `:subjects-at` nil or a path through
  required map entries to a collection of at most 256 keywords; an opaque
  grammar with shape `[:any]`, no subjects and no `:by-value`. The gate
  answers any of these `:malformed-control`; the reason is for tests and
  notes. Total."
  [g]
  (try
    (let [at (:subjects-at g)]
      (cond
        (not (and (map? g) (= grammar-parts (set (keys g))))) :not-a-grammar
        (some? (shape/refusal (:shape g))) (shape/refusal (:shape g))
        (not (boolean? (:opaque g))) :bad-opaque
        (not (and (set? (:index g)) (every? index-hints (:index g)))) :bad-index
        (not (or (nil? at) (and (vector? at) (seq at) (every? keyword? at)))) :bad-subjects-at
        (and (some? at) (not (subjects-path-ok? (:shape g) at))) :bad-subjects-at
        (and (:opaque g) (not= [:any] (:shape g))) :opaque-with-a-shape
        (and (:opaque g) (some? at)) :opaque-with-subjects
        (and (:opaque g) (contains? (:index g) :by-value)) :opaque-by-value))
    (catch Throwable _ :not-a-grammar)))

(defn governed
  "The key a grammar fact governs: its entity (\"keys are ids\")."
  [f]
  (:e f))

(defn grammar-fact? "A fact under the store key `:grammar`." [f] (= :grammar (:k f)))

;; ---------------------------------------------------------------- the row

(defn row-schema
  "A key's row in a layer (4.7), `[L :key-rows k]`: whether the layer holds
  a fact under k, and the grammar in force, nil when none. The shape is its
  canonical EDN text, because a shape nests and a schema has no recursive
  type."
  []
  (fixed-keys-schema
   {:used Boolean
    :grammar (fixed-keys-schema
              {:fid clojure.lang.PersistentVector
               :stamp Long
               :shape String
               :subjects-at (vector-schema clojure.lang.Keyword)
               :opaque Boolean
               :index (set-schema clojure.lang.Keyword)})}))

(defn layer-fields
  "The field phase 6 adds to a one-owner layer's value in `$$layers`: one
  row per key, subindexed, since nothing bounds the keys a layer uses
  (T-RC1)."
  []
  {:key-rows (map-schema clojure.lang.Keyword (row-schema) {:subindex-options {:track-size? false}})})

(defn row
  "A row's `:grammar` part for the grammar fact `fid`, admitted at `stamp`
  with value `g`."
  [fid stamp g]
  {:fid fid
   :stamp stamp
   :shape (env/canonical (:shape g))
   :subjects-at (:subjects-at g)
   :opaque (:opaque g)
   :index (:index g)})

(defn rows-to-read
  "The keys whose rows a decision reads (4.7 step 2): every distinct key of
  the act's facts that is not a store key, and the key each grammar fact
  governs. `store-key?` is the gate's."
  [facts store-key?]
  (into [] (distinct)
        (concat (remove store-key? (map :k facts))
                (keep #(when (grammar-fact? %) (governed %)) facts))))

(defn hint-change?
  "Whether grammar value `g` changes the index hints or the opacity the row
  gives (the defaults, `#{}` and not opaque, for no grammar)."
  [row g]
  (let [old (:grammar row)]
    (or (not= (or (:index old) #{}) (:index g))
        (not= (boolean (:opaque old)) (boolean (:opaque g))))))

(defn rebuild-refusal
  "`:grammar-change-needs-rebuild` when a grammar fact of the act would
  change the hints or the opacity of a key the layer already holds a fact
  under (its row's `:used`), or that the act itself writes a fact under
  (V-F4: the act's index writes take the hints read before it). `rows` are
  the rows read, {k row}. Nil otherwise."
  [facts rows]
  (let [written (into #{} (map :k) facts)]
    (when (some (fn [f]
                  (when (grammar-fact? f)
                    (let [k (governed f)
                          row (get rows k)]
                      (and (hint-change? row (:v f))
                           (or (true? (:used row)) (contains? written k))))))
                facts)
      :grammar-change-needs-rebuild)))

(defn key-row-writes
  "The rows an admitted act changes, each whole, from the rows read in the
  same event, so a replay writes the same rows (4.7): a grammar fact sets
  its key's `:grammar`, the latest of the act winning (the gate refuses two
  for one key); the first fact under a key that is not a store key sets
  `:used`. `[[k row] ...]`, in key order of first appearance."
  [facts rows nm stamp store-key?]
  (let [changed (reduce (fn [acc [i f]]
                          (cond
                            (grammar-fact? f)
                            (let [k (governed f)
                                  r (or (get acc k) (get rows k) {:used false :grammar nil})]
                              (assoc acc k (assoc r :grammar (row [nm (long i)] stamp (:v f)))))

                            (store-key? (:k f)) acc

                            :else
                            (let [k (:k f)
                                  r (or (get acc k) (get rows k) {:used false :grammar nil})]
                              (if (true? (:used r)) acc (assoc acc k (assoc r :used true))))))
                        {}
                        (map-indexed vector facts))]
    (into [] (keep (fn [k] (when-let [r (get changed k)] [k (merge {:used false :grammar nil} r)])))
          (distinct (map (fn [f] (if (grammar-fact? f) (governed f) (:k f))) facts)))))

;; ------------------------------------------------- what the rows give

(def ^:private unreadable-shape
  "A shape nothing matches: a row whose text does not read back refuses its
  key's values rather than admitting them unchecked (fail closed)."
  [:or])

(defn grammars-of
  "The grammar map `named` and `subjects-of` take, from a layer's rows
  `{k row}`: per key with a grammar, `{:subjects-at p :opaque b :shape s}`,
  the shape read back from its text. Keys with no grammar are absent (the
  permissive default). Total."
  [rows]
  (try
    (into {}
          (keep (fn [[k r]]
                  (when-let [g (:grammar r)]
                    [k {:subjects-at (:subjects-at g)
                        :opaque (boolean (:opaque g))
                        :shape (or (try (edn/read-string (:shape g)) (catch Throwable _ nil))
                                   unreadable-shape)}])))
          rows)
    (catch Throwable _ {})))

(defn named
  "What value `v` of key `k` names under `grammars` (read-values' naming
  step; rulings 6 and 8): #{} for a key with no grammar (the permissive
  default) and for an opaque key (never interpreted: no shape check, no
  subjects read from it); else `:value-shape` when the value does not match
  the grammar's shape, or the people `subjects-of` reads at its
  `:subjects-at`. A grammar with no `:shape` (the constant `grammars`) is
  not shape-checked. Total: a failure refuses the value."
  [grammars k v]
  (try
    (if-let [g (get grammars k)]
      (cond
        (:opaque g) #{}
        (and (some? (:shape g)) (some? (shape/check (:shape g) v))) :value-shape
        :else (subjects-of grammars k v))
      #{})
    (catch Throwable _ :value-shape)))

(defn hints
  "The read exit's hint sets from a layer's rows `{k row}` (4.5):
  `:by-value` the keys indexed by value (and not opaque), `:opaque` the
  opaque keys, `:no-copy` the keys whose id-index entries carry no value.
  The store's own keys' hints are the read exit's to add (`reads/hints-of`)."
  [rows]
  (reduce (fn [h [k r]]
            (if-let [g (:grammar r)]
              (cond-> h
                (and (contains? (:index g) :by-value) (not (:opaque g))) (update :by-value conj k)
                (:opaque g) (update :opaque conj k)
                (contains? (:index g) :no-copy) (update :no-copy conj k))
              h))
          {:by-value #{} :opaque #{} :no-copy #{}}
          rows))
