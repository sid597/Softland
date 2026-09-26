;; IMPORTANT: Before modifying this file, re-read PLAN-tools-and-grammars.md
;; 4.2 (the value shape) and T-RC5 (its bounds and budget).
(ns rig.store.shape
  "The value shapes a key's grammar states (PLAN-tools-and-grammars.md 4.2;
  ruling 6's shape check). A shape is plain data, never code: a vector whose
  head names its form.

    [:any]                     any value the floor admits
    [:nil] [:boolean] [:int] [:uuid]
    [:keyword]                 a readable keyword
    [:string] [:string max]    a string of at most max characters
    [:enum v ...]              one of up to 64 plain values
    [:vector-of s min max]     a vector of min to max elements, each s
    [:set-of s min max]        a set likewise
    [:map {k s ...} opts]      entries named by keywords, each matching its
                               shape; opts {:optional #{k} :open? bool},
                               optional; a closed map admits no other entry
    [:or s ...]                the first of up to 8 shapes that matches

  Total by construction: a shape is finite data with no names and no
  references, and checking is structural recursion over the shape (at most
  8 deep once admitted) and the value (bounded by the envelope). Checking
  counts the (shape, value) pairs it visits and stops at 65,536, which
  refuses the value like any mismatch (`[:or]` can multiply work). The
  shape checks meaning, never bytes: the floor's one encoding is the same
  for every key (CORNERS C4.4).

  Pure and total: `refusal` and `check` catch every throwable, because the
  gate calls them inside its decision and an exception in topology code is
  fatal to the worker.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [rig.store.envelope :as env]))

(def max-depth "How deep an admitted shape may nest (T-RC5)." 8)
(def max-nodes "How many shape vectors an admitted shape may hold (T-RC5)." 256)
(def max-branches "Branches of one `[:or]` (T-RC5)." 8)
(def max-enum "Values of one `[:enum]` (T-RC5)." 64)
(def max-string "The largest `[:string max]`." 1048576)
(def max-elements "The largest `max` of `[:vector-of]` and `[:set-of]`." 65536)
(def budget "(shape, value) pairs one check may visit before it refuses (T-RC5)." 65536)

(defn- nat? [x] (and (int? x) (<= 0 x)))

(declare measure)

(defn- measure-all
  "[nodes depth] of a node holding the shapes `ss` below it, or nil when any
  of them is outside the language."
  [ss]
  (let [ms (mapv measure ss)]
    (when (every? some? ms)
      [(reduce + 1 (map first ms)) (inc (reduce max 0 (map second ms)))])))

(defn- map-opts-ok? [entries opts]
  (and (map? opts)
       (every? #{:optional :open?} (keys opts))
       (let [o (:optional opts)]
         (or (nil? o) (and (set? o) (every? #(contains? entries %) o))))
       (let [x (:open? opts)]
         (or (nil? x) (boolean? x)))))

(defn measure
  "[nodes depth] of a shape in the language, or nil for anything outside it
  (bounds aside: `refusal` applies them). Recursion follows the shape's own
  nesting, which the envelope bounds at 32 for a value."
  [s]
  (when (and (vector? s) (keyword? (first s)))
    (let [[head & args] s
          n (count args)]
      (case head
        (:any :nil :boolean :int :uuid :keyword)
        (when (zero? n) [1 1])

        :string
        (when (or (zero? n) (and (= 1 n) (nat? (first args)) (<= (first args) max-string)))
          [1 1])

        :enum
        (when (and (<= 1 n) (every? env/edn-value? args)) [1 1])

        (:vector-of :set-of)
        (when (= 3 n)
          (let [[e lo hi] args]
            (when (and (nat? lo) (nat? hi) (<= lo hi max-elements))
              (measure-all [e]))))

        :map
        (when (<= 1 n 2)
          (let [[entries opts] args
                opts (if (= 2 n) opts {})]
            (when (and (map? entries)
                       (every? env/readable-keyword? (keys entries))
                       (map-opts-ok? entries opts))
              (measure-all (vals entries)))))

        :or
        (when (<= 1 n max-branches) (measure-all args))

        nil))))

(defn- nodes-of
  "Every shape vector of a shape `measure` accepted, itself first."
  [s]
  (let [[head & args] s]
    (cons s (case head
              (:vector-of :set-of) (nodes-of (first args))
              :map (mapcat nodes-of (vals (first args)))
              :or (mapcat nodes-of args)
              nil))))

(defn refusal
  "Why `s` is not a shape of the language within its bounds, or nil:
  `:not-a-shape`, `:too-many-nodes` (over 256), `:too-deep` (over 8),
  `:too-many-values` (an `[:enum]` over 64). Total."
  [s]
  (try
    (if-let [[nodes depth] (measure s)]
      (cond
        (< max-nodes nodes) :too-many-nodes
        (< max-depth depth) :too-deep
        (some #(and (= :enum (first %)) (< max-enum (dec (count %)))) (nodes-of s)) :too-many-values)
      :not-a-shape)
    (catch Throwable _ :not-a-shape)))

;; ------------------------------------------------------------------ check

(defn- over-budget [] (ex-info "shape check budget" {::over-budget true}))

(defn check
  "nil when value `v` matches shape `s`; else the path into `v` of the
  first mismatch found (a vector of map entry names, vector indices and set
  elements; [] for the value itself), or `:budget` when the check visited
  more than 65,536 (shape, value) pairs before deciding. For an admitted
  shape only (`refusal` nil); anything else is a mismatch at []. Whether a
  value is refused never depends on the order a set or a map is walked in:
  a value that matches visits every pair whatever the order. Total."
  [s v]
  (let [visits (volatile! 0)]
    (letfn [(step [s v path]
              (when (< budget (vswap! visits inc)) (throw (over-budget)))
              (let [[head & args] s]
                (case head
                  :any nil
                  :nil (when-not (nil? v) path)
                  :boolean (when-not (boolean? v) path)
                  :int (when-not (int? v) path)
                  :uuid (when-not (uuid? v) path)
                  :keyword (when-not (env/readable-keyword? v) path)
                  :string (when-not (and (string? v) (or (empty? args) (<= (count v) (first args)))) path)
                  :enum (when-not (some #(= % v) args) path)
                  :vector-of (let [[e lo hi] args]
                               (if (and (vector? v) (<= lo (count v) hi))
                                 (reduce (fn [_ [i x]] (when-let [p (step e x (conj path i))] (reduced p)))
                                         nil (map-indexed vector v))
                                 path))
                  :set-of (let [[e lo hi] args]
                            (if (and (set? v) (<= lo (count v) hi))
                              (reduce (fn [_ x] (when-let [p (step e x (conj path x))] (reduced p)))
                                      nil v)
                              path))
                  :map (let [[entries opts] args
                             optional (or (:optional opts) #{})
                             open? (true? (:open? opts))]
                         (cond
                           (not (map? v)) path
                           (and (not open?) (some #(not (contains? entries %)) (keys v)))
                           (conj path (first (remove #(contains? entries %) (keys v))))
                           :else
                           (reduce (fn [_ [k es]]
                                     (if (contains? v k)
                                       (when-let [p (step es (get v k) (conj path k))] (reduced p))
                                       (when-not (contains? optional k) (reduced (conj path k)))))
                                   nil entries)))
                  :or (when-not (some #(nil? (step % v path)) args) path)
                  path)))]
      (try
        (step s v [])
        (catch clojure.lang.ExceptionInfo e
          (if (::over-budget (ex-data e)) :budget []))
        (catch Throwable _ [])))))
