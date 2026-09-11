(ns softland.inland.total
  "Pure authored-language leaves, structural checks and outcome records.
   Takes EDN records and explicit bindings; gives values, validation messages or
   runtime/status outcomes. Holds no source subscriptions, clock, durable store or
   effect owner. Execution supplies reads/calls; session/activity apply effects.
   Expression evaluation uses a fixed leaf table and no host eval. Size checks count
   printed characters after evaluation, not UTF-8 bytes or preemptive CPU/memory
   budgets. Structural validation is narrower than proving every authored behavior."
  (:require [app.client.engine.executor :as executor]
            [clojure.string :as str]
            [softland.inland.logic :as logic]
            #?(:clj [clojure.edn :as edn] :cljs [cljs.reader :as edn])))

(def vocabulary #{:value :read :session :call :index :query :derive})
(def leaves
  (merge (into {} (map (fn [[k [_ _ f]]] [k f]) executor/operations))
    {:str str :pr pr-str :count count :first first :rest #(vec (rest %))
     :empty? empty? :contains? #(boolean (some #{%2} %1))
     :concat #(vec (mapcat identity %&)) :distinct #(vec (distinct %))
     :conj #(conj (vec %1) %2) :assoc assoc :dissoc dissoc :merge merge
     :keys #(vec (keys %)) :vals #(vec (vals %)) :nth #(get %1 %2)
     :lookup get :join #(str/join %1 %2) :not= not= :boolean boolean
     :take #(vec (take %1 %2)) :drop #(vec (drop %1 %2))
     :parse #(edn/read-string %)}))

(defn expression
  "Formula/data and bindings → recursively evaluated value; may throw.
   Maps evaluate values, get follows binding paths, and if/or/and short-circuit.
   Literal escapes evaluation. Known vector heads call pure leaves; other vectors
   remain evaluated data vectors. Use evaluate at a runtime boundary for diagnostics."
  [x bindings]
  (cond
    (map? x) (into {} (map (fn [[k v]] [k (expression v bindings)]) x))
    (vector? x)
    (let [[op & args] x]
      (case op
        :literal (first args)
        :get (get-in bindings args)
        :if (expression (if (expression (first args) bindings) (second args) (nth args 2)) bindings)
        :or (loop [xs args] (when (seq xs) (or (expression (first xs) bindings) (recur (rest xs)))))
        :and (loop [xs args result true] (if (seq xs) (let [v (expression (first xs) bindings)] (if v (recur (rest xs) v) v)) result))
        (if-let [f (get leaves op)]
          (apply f (map #(expression % bindings) args))
          (mapv #(expression % bindings) x))))
    :else x))

(defn blocked?
  "Value → whether it carries runtime/status, including absence and failures.
   This recognizes a tagged outcome, not merely an unfinished asynchronous read."
  [x] (and (map? x) (contains? x :runtime/status)))
(defn ready?
  "Nested value → false only when it contains a pending runtime outcome.
   Failures and complete absence are ready to snapshot and handle explicitly."
  [x]
  (cond (blocked? x) (not= :pending (:runtime/status x))
        (map? x) (every? ready? (vals x))
        (sequential? x) (every? ready? x)
        :else true))
(defn evaluate
  "Formula/data and bindings → value, failed formula, or exhausted size result.
   Catches expression errors and checks the result's printed length against 65536.
   The cap is checked after construction; it is not preemptive execution metering."
  [x bindings]
  (try (let [v (expression x bindings)]
         (if (> (count (pr-str v)) 65536)
           {:runtime/status :exhausted :reason "A step exceeded its 64KB value budget."} v))
       (catch #?(:clj Throwable :cljs :default) _
         {:runtime/status :failed :reason "The formula cannot consume these inputs."})))

(defn step-error
  "Capability keyword and evaluated args → input-shape error or nil.
   Read paths accept a keyword or vector of keywords; call/query require explicit
   binding maps. Derive performs its own deeper validation in logic/derive."
  [op args]
  (if (and (not= :value op) (not (map? args))) "Capability inputs are a record."
  (case op
    :value nil
    :session (when-not (string? (:key args)) "A session read names a local cell.")
    :read (when-not (and (string? (:name args))
                         (or (not (contains? args :attr)) (keyword? (:attr args))
                             (and (vector? (:attr args)) (every? keyword? (:attr args)))))
            "An addressed read needs a name and an attribute path.")
    :call (when-not (and (string? (:name args)) (map? (:bindings args)))
            "A call needs a definition name and explicit bindings.")
    :index (when-not (string? (:key args)) "An index read needs a bucket name.")
    :query (when-not (and (keyword? (:demand args)) (map? (:bindings args)))
             "A query needs a demand and explicit bindings.")
    :derive nil
    "This capability requires an execution owner.")))

(defn first-failure
  "Nested result → first runtime/status record, or nil if none.
   Despite its name, this includes pending, complete absence and exhausted outcomes."
  [value]
  (cond (blocked? value) value
        (map? value) (some first-failure (vals value))
        (sequential? value) (some first-failure value)))

(defn collection-state
  "Expected branch count and received values → pending, nested status or nil.
   Count equality prevents a temporarily empty Electric collection from proving
   absence before all branches report. Nil values still count as completed branches."
  [expected values]
  (if (not= expected (count values))
    {:runtime/status :pending :reason "Waiting for every branch of the relevant scope."}
    (first-failure values)))

(defn effect-error
  "Event result → nil for a vector of supported effects, otherwise message.
   Checks envelope shapes, including string session keys and activity identifiers;
   nil entries are allowed here. Capability owners validate deeper request contents."
  [effects]
  (when-not (and (vector? effects)
                (every? (fn [effect]
                          (or (nil? effect)
                              (and (map? effect)
                                   (case (:effect effect)
                                     :session (and (map? (:writes effect)) (every? string? (keys (:writes effect))))
                                     :admit (map? (:request effect))
                                     :event (keyword? (get-in effect [:event :kind]))
                                     :visibility (boolean? (:value effect))
                                     :activity (every? string? (map #(get-in effect [:request %]) [:id :step :output]))
                                     :cancel (string? (:owner effect))
                                     false)))) effects))
    "An event recipe returns a vector of supported effect requests."))

(defn shape-error
  "Recipe body → structural error or nil.
   Requires return plus at most 64 steps with distinct keyword out names and known
   opcodes. It does not resolve definitions or validate evaluated leaf arguments."
  [body]
  (cond
    (not (map? body)) "A body is a total recipe record."
    (not (vector? (:steps body))) "A recipe needs a vector of named steps."
    (> (count (:steps body)) 64) "A recipe has at most 64 steps."
    (not= (count (:steps body)) (count (distinct (map :out (:steps body))))) "Step names must be unique."
    (some #(or (not (keyword? (:out %))) (not (vocabulary (:op %)))) (:steps body)) "Every step needs a name and a supported capability."
    (not (contains? body :return)) "A recipe declares its returned value."
    :else nil))

(defn parse
  "EDN source string → {:value map} or {:error message}.
   Rejects sources over 24000 characters and non-map results; never evaluates host
   code. The platform EDN reader supplies parsing semantics, including trailing input."
  [source]
  (try
    (if (> (count source) 24000)
      {:error "The record exceeds this workbench's 24KB read budget."}
      (let [v (edn/read-string source)]
        (if (map? v) {:value v} {:error "Enter an EDN record (a map)."})))
    (catch #?(:clj Throwable :cljs :default) _ {:error "The record is not valid EDN."})))

(defn row-error
  "Candidate record → first structural/admission error or nil.
   Checks stable name, printed size, body shape and bounded pattern declarations.
   This does not prove a paint result is usable; paint validates realized descriptions."
  [row]
  (or (when (not (and (string? (:name row)) (<= 1 (count (:name row)) 100))) "A row needs a stable name.")
      (when (> (count (pr-str row)) 24000) "A record exceeds the 24KB admission budget.")
      (when (:body row) (shape-error (:body row)))
      (when (and (:pattern row) (not (map? (:pattern row)))) "A pattern is a record.")
      (when (some #(and (get-in row [:pattern %]) (not (keyword? (get-in row [:pattern %])))) [:event :demand])
        "Event and demand index keys are keywords.")
      (when (and (get-in row [:pattern :reads])
                 (not (and (vector? (get-in row [:pattern :reads])) (<= (count (get-in row [:pattern :reads])) 32))))
        "A pattern has at most 32 declared reads.")
      (when (and (:pattern row) (some #(and (:not %) (:recursive %)) (:reads (:pattern row))))
        "Negation must read a completed lower dependency, never a recursive one.")))

(defn index-keys
  "Visible row → its membership buckets, or nil for missing/tombstoned rows.
   Definitions, catalogs, event/demand names and pending/running activity states each
   contribute a bucket. Completed activities remain in their ordinary catalog."
  [row]
  (when (and row (not (:removed row)))
    (cond-> #{"rows"}
      (:body row) (conj "definitions")
      (:catalog row) (conj (str "catalog/" (:catalog row)))
      (and (:activity row) (= :pending (:status row))) (conj "activities/pending")
      (and (:activity row) (= :running (:status row))) (conj "activities/running")
      (get-in row [:pattern :event]) (conj (str "event/" (name (get-in row [:pattern :event]))))
      (get-in row [:pattern :demand]) (conj (str "demand/" (name (get-in row [:pattern :demand])))))))

(defn version-key
  "Layer, record name and revision → printed tuple key for immutable versions."
  [layer name revision] (pr-str [layer name revision]))
(defn row-key
  "Layer and record name → slash-joined current-row key.
   Callers must use names/layers whose combinations do not collide under this encoding;
   this helper does not escape slashes or enforce a stronger address grammar."
  [layer name] (str layer "/" name))

(defn conclude
  "Answer records → map of each non-nil value to its distinct support ids.
   Two definitions supporting the same value survive independently. Tagged outcomes
   are ignored here; Query checks them before calling. No derived facts enter Rama."
  [supports]
  "Demand-local support union; each support remains identifiable. Two derivations
   of the same conclusion survive independently. No derived facts enter Rama."
  (reduce (fn [out {:keys [support value]}]
            (if (or (nil? value) (blocked? value)) out
              (update out value (fnil conj #{}) support))) {} supports))

(defn step-delay
  "Step result and fallback delay → milliseconds clamped to a minimum of one.
   Accepts a numeric explicit/fallback value between zero and 1000; malformed values
   use one millisecond. Outcome validation separately reports an invalid explicit wait."
  [result fallback]
  (let [ms (:wait-ms result fallback)]
    (if (and (number? ms) (<= 0 ms 1000)) (max 1 ms) 1)))

(defn step-outcome-error
  "Repeated-step result → outcome-shape/effect error or nil.
   Allows bounded waits, at most eight local/record effects and one admission.
   Child activity/event spawning is excluded. Unlike event effect-error, the step's
   narrower effect set rejects nil entries when effects are present."
  [result]
  (cond
    (not (map? result)) "A repeated step returns an outcome record."
    (and (contains? result :wait-ms) (not (and (number? (:wait-ms result)) (<= 0 (:wait-ms result) 1000))))
    "A step's explicit wait is between 0 and 1000 milliseconds."
    (:effects result)
    (or (effect-error (:effects result))
        (when (or (> (count (:effects result)) 8)
                  (> (count (filter #(= :admit (:effect %)) (:effects result))) 1)
                  (some #(not (or (= :session (:effect %))
                                  (and (= :admit (:effect %)) (#{:put :promote :remove :delete-override} (get-in % [:request :kind]))))) (:effects result)))
          "A step has at most eight local/record effects and one admission. Spawning child activities is not available inside a step."))))

(defn step-result
  "Step outcome, prior state and remaining budget → next progress record.
   Tagged outcomes propagate status; invalid outcomes fail; done completes; the last
   budgeted unfinished step exhausts. Otherwise returns running with one less step.
   It calculates progress only; the owner applies effects and adds iteration identity."
  [result state remaining]
  (cond
    (blocked? result) {:status (:runtime/status result) :state state :reason (:reason result)}
    (step-outcome-error result) {:status :failed :state state :reason (step-outcome-error result)}
    (:done result) {:status :complete :value (:result result) :state (:state result state)}
    (<= remaining 1) {:status :exhausted :state (:state result state) :reason "The step budget is exhausted; no work is running."}
    :else {:status :running :state (:state result state) :remaining (dec remaining)}))
