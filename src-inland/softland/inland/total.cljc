(ns softland.inland.total
  "Total recipe validation and pure leaves. Takes records and explicit bindings.
   Gives values or bounded diagnostics. Holds no sources, clock, cache or effects."
  (:require [app.client.engine.executor :as executor]
            [clojure.string :as str]
            [softland.inland.logic :as logic]
            #?(:clj [clojure.edn :as edn] :cljs [cljs.reader :as edn])))

(def vocabulary #{:value :read :session :call :index :activity :query :derive})
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

(defn expression [x bindings]
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

(defn blocked? [x] (and (map? x) (contains? x :runtime/status)))
(defn ready? [x]
  (cond (blocked? x) (not= :pending (:runtime/status x))
        (map? x) (every? ready? (vals x))
        (sequential? x) (every? ready? x)
        :else true))
(defn evaluate [x bindings]
  (try (let [v (expression x bindings)]
         (if (> (count (pr-str v)) 65536)
           {:runtime/status :exhausted :reason "A step exceeded its 64KB value budget."} v))
       (catch #?(:clj Throwable :cljs :default) _
         {:runtime/status :failed :reason "The formula cannot consume these inputs."})))

(defn step-error [op args]
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

(defn first-failure [value]
  (cond (blocked? value) value
        (map? value) (some first-failure (vals value))
        (sequential? value) (some first-failure value)))

(defn collection-state [expected values]
  (if (not= expected (count values))
    {:runtime/status :pending :reason "Waiting for every branch of the relevant scope."}
    (first-failure values)))

(defn effect-error [effects]
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

(defn shape-error [body]
  (cond
    (not (map? body)) "A body is a total recipe record."
    (not (vector? (:steps body))) "A recipe needs a vector of named steps."
    (> (count (:steps body)) 64) "A recipe has at most 64 steps."
    (not= (count (:steps body)) (count (distinct (map :out (:steps body))))) "Step names must be unique."
    (some #(or (not (keyword? (:out %))) (not (vocabulary (:op %)))) (:steps body)) "Every step needs a name and a supported capability."
    (not (contains? body :return)) "A recipe declares its returned value."
    :else nil))

(defn parse [source]
  (try
    (if (> (count source) 24000)
      {:error "The record exceeds this workbench's 24KB read budget."}
      (let [v (edn/read-string source)]
        (if (map? v) {:value v} {:error "Enter an EDN record (a map)."})))
    (catch #?(:clj Throwable :cljs :default) _ {:error "The record is not valid EDN."})))

(defn row-error [row]
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

(defn index-keys [row]
  (when (and row (not (:removed row)))
    (cond-> #{"rows"}
      (:body row) (conj "definitions")
      (:catalog row) (conj (str "catalog/" (:catalog row)))
      (and (:activity row) (= :pending (:status row))) (conj "activities/pending")
      (and (:activity row) (= :running (:status row))) (conj "activities/running")
      (get-in row [:pattern :event]) (conj (str "event/" (name (get-in row [:pattern :event]))))
      (get-in row [:pattern :demand]) (conj (str "demand/" (name (get-in row [:pattern :demand])))))))

(defn version-key [layer name revision] (pr-str [layer name revision]))
(defn row-key [layer name] (str layer "/" name))

(defn conclude [supports]
  "Demand-local support union; each support remains identifiable. Two derivations
   of the same conclusion survive independently. No derived facts enter Rama."
  (reduce (fn [out {:keys [support value]}]
            (if (or (nil? value) (blocked? value)) out
              (update out value (fnil conj #{}) support))) {} supports))

(defn step-result [result state remaining]
  (cond
    (blocked? result) {:status (:runtime/status result) :state state :reason (:reason result)}
    (:done result) {:status :complete :value (:result result) :state (:state result state)}
    (<= remaining 1) {:status :exhausted :state (:state result state) :reason "The step budget is exhausted; no work is running."}
    :else {:status :running :state (:state result state) :remaining (dec remaining)}))
