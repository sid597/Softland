;; IMPORTANT: Before modifying this file, re-read PLAN-tools-and-grammars.md
;; 5 (the tool fact, tonight's vocabulary), 6.3 (the run's name) and 6.4 (the
;; loop check), and docs/decisions.md "Tools are records over a vocabulary".
(ns rig.store.recipe
  "The recipe executor (PLAN-tools-and-grammars.md 5; decisions.md: \"a
  recipe is a record: named steps over a vocabulary of capabilities,
  formulas in the leaves, one executor with several runners\"). A tool is a
  fact, `{:e <tool id> :k :tool :v {:matches :signature :permission
  :recipe}}` (T-FR4); this namespace reads its value (`parse-tool`), runs
  its recipe over one matched row (`run`), refuses the tools that would feed
  themselves (`loop-free`), and names a run so a rerun is a retry
  (`run-name`). The runner (rig.store.runner) is its first runner.

  Nothing in a record is a program: a step names a capability from the
  vocabulary below, and every argument is a formula from a closed set or a
  literal where the capability says so. No formula refers to a var, a
  namespace or a function outside the vocabulary.

  Steps, the capabilities (class c of the machinery count):
    :emit                 one output fact {:e :k :v}; :k a literal in :out
    :revision/read-units  rig.revision/read-units over an operator-named
    :revision/read-span   repository (:repo a literal id, V-F13)
  Formulas, in the leaves (class c):
    [:lit x]              the plain value x
    [:in part & path]     a part of the matched fact (:e :k :v :fid
                          :stamp), and a path into it
    [:got step & path]    an earlier step's result, and a path into it
    [:count f]            the element count of a collection, else nil
    [:str f ...]          the concatenated string forms
    [:map {k f ...}]      a map from literal keywords to formula values
    a plain scalar        itself (a string, number, keyword, boolean, nil)

  Total: at most 16 steps, each run once; a formula at most 8 deep and 256
  nodes; `[:got]` only to earlier steps (checked at parse); every function
  total (a path into a missing place is nil). A capability returns data or
  an error as data; a failed step stops the run and nothing is offered.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [rig.revision :as revision]
            [rig.store.envelope :as env])
  (:import [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets]
           [java.security MessageDigest]
           [java.util UUID]))

(def max-steps "Steps in one recipe (5.1)." 16)
(def max-formula-depth "How deep one formula may nest (5.2)." 8)
(def max-formula-nodes "Formula vectors in one formula (5.2)." 256)

(def tool-parts "A tool value's parts, exactly (T-FR4)." #{:matches :signature :permission :recipe})

(def parts-of-a-match "What `[:in part]` may name of the matched fact." #{:e :k :v :fid :stamp})

(def pattern-kinds
  "The read exit's six pattern forms (PLAN-read-exit.md), by head and
  length. The exit's own parser is the authority; this is only what the
  loop check needs to know which key a pattern matches."
  {:all 1 :e 2 :ek 3 :latest 3 :k 2 :kv 3})

;; ------------------------------------------------------------- formulas

(declare formula-ok?)

(defn- path-ok? [path] (every? env/edn-value? path))

(defn- formula-measure
  "[nodes depth] of a well-formed formula given the step names `earlier`
  it may refer to, or nil."
  [f earlier]
  (cond
    ;; a plain scalar in a formula's place stands for itself (5.1's `" named"`)
    (and (not (coll? f)) (env/edn-value? f)) [1 1]
    (not (and (vector? f) (keyword? (first f)))) nil
    :else
    (let [[head & args] f
          sub (fn [fs] (let [ms (mapv #(formula-measure % earlier) fs)]
                         (when (every? some? ms)
                           [(reduce + 1 (map first ms)) (inc (reduce max 0 (map second ms)))])))]
      (case head
        :lit (when (and (= 1 (count args)) (env/edn-value? (first args))) [1 1])
        :in (when (and (<= 1 (count args)) (contains? parts-of-a-match (first args)) (path-ok? (rest args))) [1 1])
        :got (when (and (<= 1 (count args)) (contains? earlier (first args)) (path-ok? (rest args))) [1 1])
        :count (when (= 1 (count args)) (sub args))
        :str (when (<= 1 (count args)) (sub args))
        :map (when (and (= 1 (count args)) (map? (first args)) (every? env/readable-keyword? (keys (first args))))
               (sub (vals (first args))))
        nil))))

(defn formula-ok?
  "Whether `f` is a formula of the closed set, within its bounds, whose
  `[:got]`s name only steps in `earlier`."
  [f earlier]
  (boolean (when-let [[nodes depth] (formula-measure f earlier)]
             (and (<= nodes max-formula-nodes) (<= depth max-formula-depth)))))

(defn eval-formula
  "A formula's value over the matched `row` ({:e :k :value :fid :stamp},
  as the read exit shows it) and the earlier steps' results. Total for a
  parsed formula: a path into a missing place, or into something that is
  not a collection, is nil."
  [f row results]
  (if-not (vector? f)
    f
    (let [[head & args] f]
    (case head
      :lit (first args)
      :in (let [[part & path] args
                x (if (= :v part) (:value row) (get row part))]
            (get-in x path))
      :got (let [[step & path] args] (get-in (get results step) path))
      :count (let [x (eval-formula (first args) row results)] (when (coll? x) (count x)))
      :str (apply str (map #(eval-formula % row results) args))
      :map (into {} (map (fn [[k g]] [k (eval-formula g row results)])) (first args))
      nil))))

;; --------------------------------------------------------------- steps

(def capabilities
  "The vocabulary's steps and the argument each takes: `:formula`, or a
  literal of the named kind. The machinery count counts each entry."
  {:emit {:e :formula :k :out-key :v :formula}
   :revision/read-units {:repo :repo-id :rev :formula :path :formula :cut :cut}
   :revision/read-span {:repo :repo-id :rev :formula :path :formula :first :formula :last :formula}})

(def ^:private optional-args {:revision/read-units #{:cut}})

(defn- arg-ok? [kind x out earlier]
  (case kind
    :formula (formula-ok? x earlier)
    :out-key (contains? out x)
    :repo-id (env/readable-keyword? x)
    :cut (contains? #{:blocks :forms} x)
    false))

(defn- step-refusal
  "Why step `s` (at index `i`) is not a step of the vocabulary, or nil."
  [s out earlier]
  (let [spec (get capabilities (:do s))]
    (cond
      (not (map? s)) :not-a-step
      (not (env/readable-keyword? (:name s))) :step-without-a-name
      (contains? earlier (:name s)) :step-name-twice
      (nil? spec) :unknown-step
      (not= (set (keys s)) (into #{:name :do} (remove (fn [k] (and (contains? (get optional-args (:do s)) k)
                                                                   (not (contains? s k))))
                                                      (keys spec))))
      :bad-arguments
      (some (fn [[k kind]] (and (contains? s k) (not (arg-ok? kind (get s k) out earlier)))) spec)
      :bad-arguments)))

;; ------------------------------------------------------------ the tool

(defn- pattern-ok? [p]
  (and (vector? p)
       (= (get pattern-kinds (first p)) (count p))
       (case (first p)
         :all true
         :e (env/readable-keyword? (nth p 1))
         (:ek :latest) (and (env/readable-keyword? (nth p 1)) (env/readable-keyword? (nth p 2)))
         :k (env/readable-keyword? (nth p 1))
         :kv (env/readable-keyword? (nth p 1)))))

(defn parse-tool
  "A tool fact's value read as a tool, or why not (5.1, V-F7): `{:ok tool}`
  or `{:refused :malformed-tool :why reason}`. `store-key?` is the gate's:
  an `:out` holding a store key or `:tool` is refused, so no tool writes
  control facts, read entries or tools tonight. The pattern is checked for
  its form only; the read exit's parser decides the rest when the runner
  reads it. Total."
  [v store-key?]
  (try
    (let [{:keys [matches signature permission recipe]} v
          out (:out signature)
          bad (fn [why] {:refused :malformed-tool :why why})]
      (cond
        (not (and (map? v) (= tool-parts (set (keys v))))) (bad :not-a-tool)
        (not (pattern-ok? matches)) (bad :bad-pattern)
        (not (and (map? signature) (= #{:in :out :rows?} (set (keys signature))))) (bad :bad-signature)
        (not= :match (:in signature)) (bad :bad-signature)
        (not (and (set? out) (seq out) (every? env/readable-keyword? out))) (bad :bad-signature)
        (some #(or (store-key? %) (= :tool %)) out) (bad :writes-a-store-key)
        (not (boolean? (:rows? signature))) (bad :bad-signature)
        (not (env/pid? permission)) (bad :bad-permission)
        (not (and (vector? recipe) (<= 1 (count recipe) max-steps))) (bad :bad-recipe)
        :else
        (let [r (reduce (fn [earlier s]
                          (if-let [why (step-refusal s out earlier)]
                            (reduced why)
                            (conj earlier (:name s))))
                        #{} recipe)]
          (if (keyword? r)
            (bad r)
            {:ok {:matches matches :out out :rows? (:rows? signature)
                  :permission permission :recipe recipe}}))))
    (catch Throwable _ {:refused :malformed-tool :why :not-a-tool})))

;; ------------------------------------------------------------- one run

(defn- revision-step
  "A `:revision/*` step: the reader over the repository the operator's
  configuration names for the step's literal id (V-F13), or a failure as
  data when the runner was given no such id. The reader never throws."
  [s row results config]
  (let [path (get-in config [:repos (:repo s)])
        ev #(eval-formula (get s %) row results)]
    (if (nil? path)
      {:error :unknown-repo :repo (:repo s)}
      (case (:do s)
        :revision/read-units (revision/read-units path (ev :rev) (ev :path)
                                                  (if (contains? s :cut) {:cut (:cut s)} {}))
        :revision/read-span (revision/read-span path (ev :rev) (ev :path) (ev :first) (ev :last))))))

(defn run
  "Run a parsed `tool`'s recipe once over one matched `row` (as the read
  exit shows it: `:e :k :value :fid :stamp`), with the runner's operator
  `config` (`{:repos {id path}}`, empty by default). Each step once, in
  order; each binds its result to its name. Returns `{:facts [{:e :k :v}
  ...]}`, the outputs of its `:emit`s in order, or `{:refused :step-failed
  :step i :error e}` when a step failed (a capability's error, or an
  `:emit` whose entity is not a keyword the envelope takes or whose value is
  nil); then nothing is offered. Total."
  ([tool row] (run tool row {}))
  ([tool row config]
   (try
     (loop [i 0 results {} facts []]
       (if (= i (count (:recipe tool)))
         {:facts facts}
         (let [s (nth (:recipe tool) i)]
           (if (= :emit (:do s))
             (let [e (eval-formula (:e s) row results)
                   v (eval-formula (:v s) row results)
                   f {:e e :k (:k s) :v v}]
               (cond
                 (not (env/readable-keyword? e)) {:refused :step-failed :step i :error :bad-entity}
                 ;; a nil value would be a retract; a tool's output always asserts one
                 (nil? v) {:refused :step-failed :step i :error :no-value}
                 :else (recur (inc i) (assoc results (:name s) f) (conj facts f))))
             (let [r (revision-step s row results config)]
               (if (and (map? r) (contains? r :error))
                 {:refused :step-failed :step i :error r}
                 (recur (inc i) (assoc results (:name s) r) facts)))))))
     (catch Throwable _ {:refused :step-failed :step nil :error :internal}))))

;; ------------------------------------------------------- the run's name

(defn- uuid-v8
  "A version 8 UUID (RFC 9562) from the first 128 bits of `bytes`, with
  its version and variant bits set."
  [^bytes bytes]
  (let [bb (ByteBuffer/wrap bytes)
        msb (.getLong bb)
        lsb (.getLong bb)]
    (UUID. (bit-or (bit-and msb (bit-not 0xF000)) 0x8000)
           (bit-or (bit-and lsb 0x3FFFFFFFFFFFFFFF) Long/MIN_VALUE))))

(defn run-name
  "The name of the run of the tool fact `tool-fid` on the matched fact
  `matched-fid` in layer `layer` (6.3, T-RC6, first-record):
  `[layer :by-layer :offer id]`, id a version 8 UUID from the first 128
  bits of SHA-256 over the canonical text of `[layer tool-fid
  matched-fid]`. The same triple always gives the same name, so a second
  pass, a restarted runner or two runners at once offer one name, and the
  gate answers the later ones from its record."
  [layer tool-fid matched-fid]
  (let [md (MessageDigest/getInstance "SHA-256")
        text (env/canonical [layer tool-fid matched-fid])]
    [layer :by-layer :offer (uuid-v8 (.digest md (.getBytes ^String text StandardCharsets/UTF_8)))]))

;; ------------------------------------------------------------ the loop

(defn matched-key
  "The key a pattern matches, or nil for a pattern that matches every key
  (`[:all]`, `[:e e]`)."
  [p]
  (case (first p)
    (:ek :latest) (nth p 2)
    (:k :kv) (nth p 1)
    nil))

(defn- reaches?
  "Whether key `from` reaches key `to` through the edges `graph` ({k #{k}})."
  [graph from to]
  (loop [todo [from] seen #{}]
    (if-let [[k & more] (seq todo)]
      (cond
        (= k to) true
        (contains? seen k) (recur more seen)
        :else (recur (into (vec more) (get graph k)) (conj seen k)))
      false)))

(defn loop-free
  "The tools of one layer that may run, in the order given (their facts'
  stamps), and the ones refused `:tool-loop` (6.4, T-RC10, V-F7): a tool
  whose pattern matches every key, whose pattern's key is one of its own
  outputs, or whose pattern names a store key (the runner writes read
  entries and leases into the layer on every pass), and a tool whose edges,
  from its matched key to its output keys, would close a cycle with the
  tools before it. A refused tool never stops one that came before it.
  `tools` are `[[fid tool] ...]`; returns `{:run [[fid tool] ...] :refused
  {fid :tool-loop}}`. Pure and total."
  [tools store-key?]
  (-> (reduce (fn [{:keys [graph] :as acc} [fid tool]]
            (let [k (matched-key (:matches tool))
                  out (:out tool)]
              (if (or (nil? k)
                      (store-key? k)
                      (contains? out k)
                      (some #(reaches? graph % k) out))
                (assoc-in acc [:refused fid] :tool-loop)
                (-> acc
                    (update :run conj [fid tool])
                    (update-in [:graph k] (fnil into #{}) out)))))
          {:run [] :refused {} :graph {}}
          tools)
      (dissoc :graph)))
