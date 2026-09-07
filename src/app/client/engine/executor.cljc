(ns app.client.engine.executor
  "One expression language for recipe arguments and width rules.

   Takes EDN expressions, explicit bindings and a capability table. Gives
   values, results with subjects, and portable continuations. Holds no state or clock.
   Capabilities own their effects. No read report is a dependency key.
   Evidence: test/app/client/engine/executor_test.clj."
  (:require [app.client.engine.schema :as schema]
            [app.client.engine.surface :as surface]
            [app.client.engine.value-bytes :as vb]))

(def ^:dynamic *read-observer* nil)

(def operations
  "Operator → [minimum arity, maximum arity or nil, implementation]."
  {:+ [0 nil +] :- [1 nil -] :* [0 nil *] :/ [1 nil (fn [& xs] (apply / (map double xs)))]
   :min [1 nil min] :max [1 nil max] :abs [1 1 #(Math/abs (double %))]
   :sqrt [1 1 #(Math/sqrt (double %))] :pow [2 2 #(Math/pow (double %1) (double %2))]
   :sin [1 1 #(Math/sin (double %))] :cos [1 1 #(Math/cos (double %))]
   :exp [1 1 #(Math/exp (double %))] :floor [1 1 #(Math/floor (double %))]
   :clamp [3 3 (fn [x lo hi] (max lo (min hi x)))]
   :step [2 2 (fn [edge x] (if (< x edge) 0.0 1.0))]
   :smoothstep [3 3 (fn [lo hi x] (let [t (max 0.0 (min 1.0 (/ (- x lo) (- hi lo))))] (* t t (- 3.0 (* 2.0 t)))))]
   :mix [3 3 (fn [a b t] (+ a (* (- b a) t)))]
   :< [2 2 <] :<= [2 2 <=] := [2 2 =] :not [1 1 not]})

(defn- arity! [op args lo hi]
  (when-not (and (<= lo (count args)) (or (nil? hi) (<= (count args) hi)))
    (throw (ex-info "Wrong expression arity" {:error-type :executor/arity :operator op :arguments args}))))

(defn compile-expression
  "Expression → a pure function of bindings. :get is required, :literal
   quotes data, :if is lazy; maps and ordinary vectors resolve children.
   Compilation happens once for a width rule, before sampling its points."
  [expression]
  (cond
    (map? expression)
    (let [entries (mapv (fn [[k v]] [k (compile-expression v)]) expression)]
      (fn [bindings] (into {} (map (fn [[k f]] [k (f bindings)])) entries)))

    (vector? expression)
    (let [[op & args] expression]
      (case op
        :literal (do (arity! op args 1 1) (constantly (first args)))
        :get (do (arity! op args 1 nil)
                 (fn [bindings]
                   (let [missing #?(:clj (Object.) :cljs (js-obj))
                         value (get-in bindings args missing)]
                     (when (identical? missing value)
                       (throw (ex-info "Missing construction binding"
                                       {:error-type :executor/missing-binding :path (vec args)})))
                     (when *read-observer* (*read-observer* (vec args) value))
                     value)))
        :if (do (arity! op args 3 3)
                (let [[pred yes no] (mapv compile-expression args)]
                  (fn [bindings] ((if (pred bindings) yes no) bindings))))
        (if-let [[lo hi f] (get operations op)]
          (do (arity! op args lo hi)
              (let [fs (mapv compile-expression args)]
                (fn [bindings]
                  (let [result (apply f (map #(% bindings) fs))]
                    (when (and (number? result) (not (schema/finite-number? result)))
                      (throw (ex-info "Expression produced a non-finite number"
                                      {:error-type :executor/non-finite :operator op})))
                    result))))
          (if (keyword? op)
            (throw (ex-info "Unknown expression operator; quote literal data"
                            {:error-type :executor/operator :operator op}))
            (let [fs (mapv compile-expression expression)]
              (fn [bindings] (mapv #(% bindings) fs)))))))
    :else (constantly expression)))

(defn evaluate
  "Expression and bindings → value, using the same compiler as width rules."
  [expression bindings]
  ((compile-expression expression) bindings))

(defn references
  "Expression → all declared :get paths, including both :if branches.
   Diagnostic syntax information only; never observations or cache keys."
  [expression]
  (cond
    (map? expression) (into #{} (mapcat references) (vals expression))
    (vector? expression) (case (first expression)
                          :literal #{}
                          :get #{(vec (rest expression))}
                          (into #{} (mapcat references) expression))
    :else #{}))

(def continuation-schema "softland/executor-continuation/1")

(defn- refuse! [reason detail]
  (throw (ex-info (name reason) {:status :refused :reason reason :detail detail})))

(defn- admission! [{:keys [program]} roots capabilities]
  (let [{:keys [steps each]} program
        all-steps (concat steps (:steps each))
        root-names (conj (set (keys roots)) :state)
        expressions (concat (map :args all-steps) [(:state each) (:next each) (:return program)])]
    (when (or (not (map? program)) (contains? program :bindings)
              (some #(some (partial contains? %) [:each :steps :call :bind]) all-steps)
              (and each (not (map? each))))
      (refuse! :one-loop "One top-level loop; steps use :out, :op and named :args"))
    (when (and each (or (not (keyword? (:item each))) (not (vector? (:fields each)))))
      (refuse! :item-fields "A loop requires an explicit :item and vector :fields"))
    (when (and each (or (not (map? (:state each))) (not (map? (:next each)))))
      (refuse! :state "A loop requires initial :state and whole replacement :next maps"))
    (when (and each (contains? root-names (:item each)))
      (refuse! :shadows-root (:item each)))
    (when (some #(= (:item each) (:out %)) all-steps)
      (refuse! :shadows-item (:item each)))
    (let [missing (vec (distinct (keep #(when-not (contains? capabilities (:op %)) (:op %)) all-steps)))]
      (when (seq missing) (refuse! :missing-capability missing)))
    (doseq [{:keys [out op args]} all-steps]
      (when-not (and (keyword? out) (keyword? op) (map? args))
        (refuse! :step "Every step requires :out, :op and an :args map"))
      (when (contains? root-names out) (refuse! :shadows-root out))
      (let [cap (get capabilities op) supplied (set (keys args))]
        (doseq [k supplied :when (not (some #{k} (:args cap)))] (refuse! :unconsumed-argument {:op op :argument k}))
        (doseq [group (:needs cap) :when (not-any? supplied group)] (refuse! :missing-argument {:op op :group group}))))
    ;; Names are single-assignment within each phase; the loop's output names
    ;; are rebound on each item, never carried implicitly into the next one.
    (doseq [phase [steps (:steps each)]]
      (when-not (= (count phase) (count (set (map :out phase)))) (refuse! :duplicate-output (mapv :out phase))))
    (when each
      (doseq [path (mapcat references expressions)
              :when (and (= (:item each) (first path)) (> (count path) 1)
                         (not (some #{(second path)} (:fields each))))]
        (refuse! :unknown-field (second path))))
    ;; Compile every expression before any capability executes; syntax typos
    ;; in an untaken branch are still syntax errors.
    (doseq [expr (concat expressions [(:items each)])] (compile-expression expr))))

(defn- dependencies
  "Static producer graph. A local output reaches its preceding producers;
   roots stay whole values. Never uses the diagnostic read report."
  [program roots]
  (let [root-names (set (keys roots))
        reach (fn reach [expr producers]
                (reduce (fn [acc path]
                          (let [k (first path)]
                            (cond
                              (contains? producers k) (into acc (get producers k))
                              (contains? root-names k) (conj acc k)
                              :else acc))) #{} (references expr)))
        before (reduce (fn [p {:keys [out args]}] (assoc p out (reach args p))) {} (:steps program))
        each (:each program)
        loop-producers (reduce (fn [p {:keys [out args]}] (assoc p out (reach args p))) before (:steps each))
        transition (if each
                     (reduce into (reach (:state each) before)
                             (concat (map #(reach (:args %) loop-producers) (:steps each))
                                     [(reach (:next each) loop-producers)])) #{})]
    {:transition transition :before before :reach reach}))

(defn recipe
  "Record and caller roots → full program plus whole roots reached by a
   transition. :items and :return have their own dependency readings."
  [{:keys [program] :as record} scope]
  (let [roots (merge (:roots record) scope)]
    {:program program :roots (select-keys roots (:transition (dependencies program roots)))}))

(defn- project-item [each item at]
  (doseq [k (:fields each)]
    (when-not (and (map? item) (contains? item k))
      (throw (ex-info "Missing declared item field" {:error-type :executor/missing-field :field k :at at}))))
  (select-keys item (:fields each)))

(defn- execute-steps [steps scope capabilities ctx reads read-outputs]
  (reduce
   (fn [{:keys [scope rows producers]} {:keys [out op args]}]
     (let [ctx (assoc ctx :step out)
           args-value (try (evaluate args scope)
                           (catch #?(:clj Exception :cljs :default) e
                             (throw (ex-info #?(:clj (.getMessage e) :cljs (.-message e))
                                             (assoc (ex-data e) :step out) e))))
           _ (when (some #(and (map? %) (:status %) (not= :resolved (:status %))) (vals args-value))
               (throw (ex-info "An unresolved read cannot be consumed" {:step out :error-type :executor/unresolved-read})))
           value (try ((:run (get capabilities op)) args-value ctx)
                      (catch #?(:clj Exception :cljs :default) e
                        (throw (ex-info #?(:clj (.getMessage e) :cljs (.-message e))
                                        (assoc (ex-data e) :step out) e))))
           read? (and (map? value) (contains? value :status))
           _ (when (and read? (not= :resolved (:status value)))
               (throw (ex-info "Only resolved reads are landed in slice A"
                               {:step out :error-type :executor/unresolved-read :read value})))
           dependencies (reduce into #{} (map #(get producers (first %) #{}) (references args)))
           read-value (when read? (dissoc value :snapshot))
           dependencies (cond-> dependencies read? (conj out))
           row (cond-> {:out out :op op :status (if read? (:status value) :complete)}
                 read? (merge value)
                 (and (map? value) (contains? value :changed)) (assoc :changed (:changed value)))]
       (when read? (swap! reads conj read-value) (swap! read-outputs assoc out read-value))
       {:scope (assoc scope out value) :rows (conj rows row) :producers (assoc producers out dependencies)}))
   {:scope scope :rows [] :producers {}} steps))

(defn- subjects [program roots results consumed history before-producers before-reads]
  (let [{:keys [reach before transition]} (dependencies program roots)
        loop-reads (vec (for [row history step (:steps row) :when (= :resolved (:status step))]
                          (dissoc step :out :op :snapshot)))
        per-output (fn [[out expr]]
                     (let [refs (references expr) state? (some #(= :state (first %)) refs)
                           root-set (cond-> (reach expr before) state? (into transition))
                           read-names (reduce into #{} (map #(get before-producers (first %) #{}) refs))]
                       [out {:recipe {:program program :roots (select-keys roots root-set)}
                             :consumed (if state? consumed [])
                             :reads (into (mapv before-reads (sort-by str read-names)) (when state? loop-reads))
                             :out out}]))]
    (if (map? (:return program)) (into {} (map per-output) (:return program))
        ;; A bare path is an admitted L0 result. Each returned field came from
        ;; the same return expression, so each has that expression's subject.
        (if (map? results) (into {} (map #(per-output [% (:return program)])) (keys results)) {}))))

(defn- result-error [e at]
  (let [d (ex-data e)]
    (if (= :refused (:status d)) d
        {:status :error :at (or (:at d) at) :step (:step d)
         :error #?(:clj (.getMessage e) :cljs (.-message e)) :data d})))

(defn- run* [{:keys [program] :as record} caller capabilities opts continuation]
  (let [roots (merge (:roots record) caller)
        diagnostic (atom {}) read-order (atom []) current-at (atom 0)
        observer (fn [path value]
                   (when (contains? roots (first path))
                     (when-not (contains? @diagnostic path) (swap! read-order conj path))
                     (swap! diagnostic assoc path value)))
        before-reads (atom []) before-outputs (atom {})]
    (try
      (admission! record roots capabilities)
      (binding [*read-observer* observer]
        (let [before (execute-steps (:steps program) roots capabilities {:at 0 :record record :budget (:budget opts)} before-reads before-outputs)
              scope (:scope before) each (:each program)
              items (when each (evaluate (:items each) scope))
              _ (when (and each (not (sequential? items)))
                  (throw (ex-info "Items must be a finite sequence value" {:error-type :executor/each-value})))
              start (if continuation (:at continuation) 0)
              _ (when continuation
                  (when (or (not (integer? start)) (neg? start) (> start (count items))
                            (not= start (count (:consumed continuation))))
                    (refuse! :consumed-items-differ {:at start}))
                  (doseq [i (range start)]
                    (when-not (vb/equal? (nth (:consumed continuation) i) (project-item each (nth items i) i))
                      (refuse! :consumed-items-differ {:at i}))))
              initial (when each (if continuation (try (surface/validate-value! (:state continuation))
                                                      (catch #?(:clj Exception :cljs :default) e
                                                        (refuse! :load (ex-data e))))
                                    (evaluate (:state each) scope)))]
          (loop [at start state initial consumed (vec (:consumed continuation)) history (vec (:history continuation))
                 log (vec (:rows before))]
            (reset! current-at at)
            (cond
              (and each (< at (count items)) (= at (:until opts)))
              {:status :suspended :reason :until :at at
               :continuation {:schema continuation-schema :vocabulary (:vocabulary capabilities)
                              :record record :scope caller :phase :in-loop :at at :consumed consumed
                              :state state :history history :request nil :missing nil}}

              (or (nil? each) (= at (count items)))
              (let [final-scope (cond-> scope each (assoc :state state)) results (evaluate (:return program) final-scope)]
                {:status :complete :results results
                 :subjects (subjects program roots results consumed history (:producers before) @before-outputs)
                 :state state :history history :log log :reads @diagnostic :read-order @read-order
                 :unread (vec (remove (set (map first (keys @diagnostic))) (keys (:roots record))))})

              :else
              (let [item (project-item each (nth items at) at)
                    iteration (execute-steps (:steps each) (assoc scope :state state (:item each) item) capabilities
                                             {:at at :record record :budget (:budget opts)} (atom []) (atom {}))
                    next-state (evaluate (:next each) (:scope iteration))
                    row {:at at :item item :state-after next-state :steps (:rows iteration)}]
                (recur (inc at) next-state (conj consumed item) (conj history row) (into log (:rows iteration))))))))
      (catch #?(:clj Exception :cljs :default) e (result-error e @current-at)))))

(defn run
  "Record {:program :roots}, caller scope, capability table and options →
   complete/error/refused, or a continuation before item :until. Every run
   is independent. Timing belongs to the caller, not this pure result."
  ([record scope capabilities] (run record scope capabilities {}))
  ([record scope capabilities opts] (run* record scope capabilities opts nil)))

(defn encode
  "Continuation → portable UTF-8 EDN bytes; float arrays carry LE payloads."
  [continuation] (surface/encode continuation))

(defn decode
  "Bytes and capability table → continuation, or {:status :refused :reason
   :load}. Schema, vocabulary and surface payload lengths are checked."
  [bytes capabilities]
  (try
    (let [c (surface/decode bytes)]
      (when-not (= continuation-schema (:schema c)) (refuse! :schema (:schema c)))
      (when-not (= (:vocabulary capabilities) (:vocabulary c)) (refuse! :vocabulary (:vocabulary c))) c)
    (catch #?(:clj Exception :cljs :default) e
      {:status :refused :reason :load :detail (ex-data e)})))

(defn resume
  "Continuation, table and options (optional replacement :record/:scope) →
   resumed run. Check vocabulary, full recipe and consumed projections;
   replay pre-loop work, restore state, then execute only the remaining items."
  [continuation capabilities opts]
  (try
    (let [record (get opts :record (:record continuation)) scope (get opts :scope (:scope continuation))
          old (recipe (:record continuation) (:scope continuation)) fresh (recipe record scope)]
      (when-not (= continuation-schema (:schema continuation)) (refuse! :load :schema))
      (when-not (= (:vocabulary capabilities) (:vocabulary continuation)) (refuse! :vocabulary (:vocabulary continuation)))
      (when-not (vb/equal? old fresh)
        (refuse! :recipe-differs
                 (if-not (= (:program old) (:program fresh)) :program
                     (first (filter #(not (vb/equal? (get-in old [:roots %]) (get-in fresh [:roots %])))
                                    (sort-by str (into (set (keys (:roots old))) (keys (:roots fresh)))))))))
      (run* record scope capabilities opts continuation))
    (catch #?(:clj Exception :cljs :default) e (result-error e (:at continuation)))))
