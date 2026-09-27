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
  The citation's (PLAN-citation.md section 3; class c unless marked):
    :read                 a pattern read through the one exit as the tool
                          (the runner's read function): {:rows :mark}
    :emit-all             every fact of a list, {:e :k :v} and an optional
                          :replaces, each key in :out
    :stand-on             [[fid stamp] ...] the run's act also stands on
    :material/reading     rig.material/reading-step (a file at a revision)
    :cite/find            rig.cite/find-step (a document's citations)
    :cite/bind            rig.cite/bind-step (a citation bound or not)
    :cite/check           rig.cite/check-step (the marks of a walk)
  Formulas, in the leaves (class c):
    [:lit x]              the plain value x
    [:in part & path]     a part of the matched fact (:e :k :v :fid
                          :stamp; the citation's :replaces, and :run, the
                          name the run's act will have), and a path into it
    [:got step & path]    an earlier step's result, and a path into it
    [:count f]            the element count of a collection, else nil
    [:str f ...]          the concatenated string forms
    [:map {k f ...}]      a map from literal keywords to formula values
    [:vec f ...]          a vector of formula values (the citation's)
    a plain scalar        itself (a string, number, keyword, boolean, nil)

  Total: at most 16 steps, each run once; a formula at most 8 deep and 256
  nodes; `[:got]` only to earlier steps (checked at parse); every function
  total (a path into a missing place is nil). A capability returns data or
  an error as data; a failed step stops the run and nothing is offered.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [rig.cite :as cite]
            [rig.material :as material]
            [rig.revision :as revision]
            [rig.store.envelope :as env])
  (:import [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets]
           [java.security MessageDigest]
           [java.util UUID]))

(def max-steps "Steps in one recipe (5.1)." 16)
(def max-formula-depth "How deep one formula may nest (5.2)." 8)
(def max-formula-nodes "Formula vectors in one formula (5.2)." 256)

(def tool-parts "A tool value's parts, exactly (T-FR4)." #{:matches :signature :permission :recipe})

(def parts-of-a-match
  "What `[:in part]` may name of the matched fact: its entity, key, value,
  id and stamp, and (the citation's) the fact it replaces and the name the
  run's act will have, which the runner puts in the row."
  #{:e :k :v :fid :stamp :replaces :run})

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
        :vec (sub args)
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
      :vec (mapv #(eval-formula % row results) args)
      nil))))

;; --------------------------------------------------------------- steps

(def capabilities
  "The vocabulary's steps and the argument each takes: `:formula`, or a
  literal of the named kind. The machinery count counts each entry."
  {:emit {:e :formula :k :out-key :v :formula}
   :revision/read-units {:repo :repo-id :rev :formula :path :formula :cut :cut}
   :revision/read-span {:repo :repo-id :rev :formula :path :formula :first :formula :last :formula}
   ;; the citation's (PLAN-citation.md section 3)
   :read {:pattern :formula}
   :emit-all {:facts :formula}
   :stand-on {:pairs :formula}
   :material/reading {:repo :repo-id :request :formula :file :formula :prev :formula :run :formula
                      :same :same-rule}
   :cite/find {:repo :repo-id :request :formula :doc :formula}
   :cite/bind {:found :formula :found-fid :formula :cite :formula :readings :formula :target :bind-rule}
   :cite/check {:reading :formula :dependents :formula :show :show-rule}})

(def same-rules
  "The rules `:material/reading` knows for the same form across revisions
  (P-C4): the same name in the same file."
  #{:name-in-file})

(def ^:private optional-args {:revision/read-units #{:cut}})

(defn- arg-ok? [kind x out earlier]
  (case kind
    :formula (formula-ok? x earlier)
    :out-key (contains? out x)
    :repo-id (env/readable-keyword? x)
    :cut (contains? #{:blocks :forms} x)
    :same-rule (contains? same-rules x)
    :bind-rule (contains? cite/bind-rules x)
    :show-rule (contains? cite/show-rules x)
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

(def max-emitted
  "Facts one run may emit (the citation's `:emit-all`): the door leases at
  most 256 locks at a time, and a reading of the largest file here has
  about 130 forms."
  512)

(defn- has-nil?
  "Whether a pattern holds nil in one of its own places (P-C13): a fact or
  an act the pattern would name is missing, as for a first reading's
  previous act. Only the top level: a name inside may carry a nil class
  (an act the store places), which is no gap (review finding 8)."
  [x]
  (or (nil? x) (and (sequential? x) (boolean (some nil? x)))))

(def max-stand-on
  "Pairs one run's act may stand on besides its match and its tool: the
  gate writes a lookup entry and a stood-on entry for each in an event that
  never yields (review finding 18)."
  1024)

(defn- read-step
  "A `:read` step (the citation's): the pattern read through the runner's
  read function, the one exit as the tool, so the read is recorded. A
  pattern holding nil reads nothing and returns no rows (P-C13); a refused
  read, and a partial one, fail the step."
  [pattern config]
  (cond
    (has-nil? pattern) {:rows [] :mark :complete :skipped true}
    (not (fn? (:read config))) {:error :no-read}
    :else (let [r ((:read config) pattern)]
            (cond
              (not (map? r)) {:error :read-failed}
              (contains? r :refused) {:error :read-refused :refused (:refused r)}
              (= :partial (:mark r)) {:error :partial-read}
              :else (select-keys r [:rows :mark :entry])))))

(defn- emitted-refusal
  "Why a list of facts from `:emit-all` cannot be offered, or nil: each a
  map of `:e` (a keyword the envelope takes), `:k` (in the tool's `:out`),
  `:v` (EDN data, not nil: a tool's output always asserts one) and an
  optional `:replaces` fact id."
  [fs out]
  (cond
    (not (sequential? fs)) :not-a-list
    (< max-emitted (count fs)) :too-many-facts
    :else (some (fn [f]
                  (cond
                    (not (map? f)) :not-a-fact
                    (not (every? #{:e :k :v :replaces} (keys f))) :unknown-part
                    (not (env/readable-keyword? (:e f))) :bad-entity
                    (not (contains? out (:k f))) :key-not-in-signature
                    (nil? (:v f)) :no-value
                    (not (env/edn-value? (:v f))) :bad-value
                    (not (or (nil? (:replaces f)) (env/fid? (:replaces f)))) :bad-replaces
                    :else nil))
                fs)))

(defn- pairs-refusal
  "Why a `:stand-on` list is not `[[fid stamp] ...]`, or nil."
  [ps]
  (cond
    (not (sequential? ps)) :not-a-list
    (< max-stand-on (count ps)) :too-many-pairs
    (not-every? (fn [p] (and (vector? p) (= 2 (count p)) (env/fid? (first p))
                             (int? (second p)) (<= 0 (second p) (dec env/max-carried-stamp))))
                ps)
    :bad-pair
    :else nil))

(defn- other-step
  "A step that is neither an emit nor a stand-on: the revision reader, the
  store read, and the citation's domain steps, each over its evaluated
  arguments. A result carrying `:error` fails the run."
  [s row results config]
  (let [ev #(eval-formula (get s %) row results)
        path (get-in config [:repos (:repo s)])
        no-repo {:error :unknown-repo :repo (:repo s)}]
    (case (:do s)
      (:revision/read-units :revision/read-span) (revision-step s row results config)
      :read (read-step (ev :pattern) config)
      :material/reading (if path
                          (material/reading-step path (:repo s) {:request (ev :request) :file (ev :file)
                                                                 :prev (ev :prev) :run (ev :run) :same (:same s)})
                          no-repo)
      :cite/find (if path (cite/find-step path (:repo s) {:request (ev :request) :doc (ev :doc)}) no-repo)
      :cite/bind (cite/bind-step {:found (ev :found) :found-fid (ev :found-fid) :cite (ev :cite)
                                  :readings (ev :readings) :target (:target s)})
      :cite/check (cite/check-step {:reading (ev :reading) :dependents (ev :dependents) :show (:show s)})
      {:error :unknown-step})))

(defn run
  "Run a parsed `tool`'s recipe once over one matched `row` (as the read
  exit shows it: `:e :k :value :fid :stamp`, and `:replaces`; the runner
  adds `:run`, the name the run's act will have), with the runner's
  operator `config` (`{:repos {id path} :read f}`: the repositories the
  git steps may read, and the read function a `:read` step calls; empty by
  default). Each step once, in order; each binds its result to its name.
  Returns `{:facts [...] :stood-on {fid stamp}}`: the outputs of its
  `:emit`s and `:emit-all`s in order, and the pairs its `:stand-on`s name,
  which the runner adds to what the act stands on; or `{:refused
  :step-failed :step i :error e}` when a step failed (a capability's error,
  an `:emit` whose entity is not a keyword the envelope takes or whose value
  is nil, an `:emit-all` or `:stand-on` list that is not well formed); then
  nothing is offered. Total."
  ([tool row] (run tool row {}))
  ([tool row config]
   (try
     (loop [i 0 results {} facts [] stood {}]
       (if (= i (count (:recipe tool)))
         {:facts facts :stood-on stood}
         (let [s (nth (:recipe tool) i)
               fail (fn [e] {:refused :step-failed :step i :error e})]
           (case (:do s)
             :emit
             (let [e (eval-formula (:e s) row results)
                   v (eval-formula (:v s) row results)
                   f {:e e :k (:k s) :v v}]
               (cond
                 (not (env/readable-keyword? e)) (fail :bad-entity)
                 ;; a nil value would be a retract; a tool's output always asserts one
                 (nil? v) (fail :no-value)
                 :else (recur (inc i) (assoc results (:name s) f) (conj facts f) stood)))

             :emit-all
             (let [fs (eval-formula (:facts s) row results)]
               (if-let [why (emitted-refusal fs (:out tool))]
                 (fail why)
                 (let [fs (mapv (fn [f] (cond-> (select-keys f [:e :k :v])
                                          (some? (:replaces f)) (assoc :replaces (:replaces f))))
                                fs)]
                   (if (< max-emitted (+ (count facts) (count fs)))
                     (fail :too-many-facts)
                     (recur (inc i) (assoc results (:name s) fs) (into facts fs) stood)))))

             :stand-on
             (let [ps (eval-formula (:pairs s) row results)]
               (if-let [why (or (pairs-refusal ps)
                                (when (< max-stand-on (+ (count stood) (count ps))) :too-many-pairs))]
                 (fail why)
                 (recur (inc i) (assoc results (:name s) ps) facts
                        (into stood (map (fn [[f st]] [f (long st)])) ps))))

             (let [r (other-step s row results config)]
               (if (and (map? r) (contains? r :error))
                 (fail r)
                 (recur (inc i) (assoc results (:name s) r) facts stood)))))))
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
  gate answers the later ones from its record.

  The spec fixes (H-1, T-FR5 as built, first-record): the 4-arity tags the
  name with the layer's class, `[layer class :offer id]`, so a run in a
  layer re-classed by entity goes to the micro gate; the id is the same
  function of the triple under either tag, so the runner finds a match's
  run under the tag its layer had when it ran. The 3-arity is `:by-layer`."
  ([layer tool-fid matched-fid] (run-name layer :by-layer tool-fid matched-fid))
  ([layer class tool-fid matched-fid]
   (let [md (MessageDigest/getInstance "SHA-256")
         text (env/canonical [layer tool-fid matched-fid])]
     [layer class :offer (uuid-v8 (.digest md (.getBytes ^String text StandardCharsets/UTF_8)))])))

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
