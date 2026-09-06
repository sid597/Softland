(ns app.client.engine.executor
  "Run a construction written as data over a vocabulary of capabilities.

   Input: a construction ({:steps [...] :return ...}), a scope of named roots
   (maps with keyword keys, and earlier step outputs), and a capability
   table (op keyword → function of the resolved arguments). Output: the
   values every step produced, the resolved return, a log per step, the
   dotted paths that were read with their values, and the operations the
   table lacked. No retained state; nothing heavy happens here. The executor
   moves values between capabilities and keeps the order a construction
   declares; every heavy thing happens inside a capability.

   A step is {:out \"name\" :op :family/capability ...bindings}. A binding
   that is a string names a dotted path into the scope (\"paint.stroke.tip\",
   \"source.samples.3\"); a string whose first segment is not a root is a
   literal word. Any other value is a literal. What a construction read is
   what its result depends on: a caller keys its cache on the read values,
   so a construction that never reads the view is camera-free by
   observation, not by declaration.

   Folder map: README.md."
  (:require [clojure.string :as str]))

(defn- segment-key
  "Path segment → vector index or keyword."
  [segment]
  (if (re-matches #"[0-9]+" segment)
    #?(:clj (Long/parseLong segment) :cljs (js/parseInt segment 10))
    (keyword segment)))

(defn resolve-binding
  "Scope and binding → {:value v :read path} for a dotted path into a root,
   {:value v :literal? true} otherwise."
  [scope binding]
  (if (string? binding)
    (let [[root & segments] (str/split binding #"\.")]
      (if (contains? scope root)
        {:value (reduce (fn [value segment]
                          (let [k (segment-key segment)]
                            (if (and (number? k) (sequential? value))
                              (nth value k nil)
                              (get value k))))
                        (get scope root)
                        segments)
         :read binding}
        {:value binding :literal? true}))
    {:value binding :literal? true}))

(defn- resolve-return
  "Scope and a return spec (bindings nested in maps and vectors) → the same
   shape with bindings resolved, and the reads made."
  [scope spec]
  (cond
    (map? spec) (reduce-kv (fn [[out reads] k child]
                             (let [[value child-reads] (resolve-return scope child)]
                               [(assoc out k value) (merge reads child-reads)]))
                           [{} {}] spec)
    (vector? spec) (reduce (fn [[out reads] child]
                             (let [[value child-reads] (resolve-return scope child)]
                               [(conj out value) (merge reads child-reads)]))
                           [[] {}] spec)
    :else (let [{:keys [value read]} (resolve-binding scope spec)]
            [value (if read {read value} {})])))

(defn- now-ms []
  #?(:clj (/ (System/nanoTime) 1.0e6) :cljs (js/performance.now)))

(defn run
  "Construction, scope, capabilities → {:ok? :values :return :log :reads
   :missing :ms}.

   Steps run in order; each step's output joins the scope under its :out
   name for later steps and the return. A step whose op the table lacks
   stops the run there with :ok? false and names the op in :missing; an
   empty drawing is not counted as the tool having run. A capability that
   throws stops the run with the error in the log. :reads maps every dotted
   path read from the original roots to the value read, in the order of
   first reading."
  [construction scope capabilities]
  (let [t0 (now-ms)
        roots (set (keys scope))]
    (loop [steps (seq (:steps construction))
           scope scope
           values {}
           log []
           reads {}]
      (if-let [step (first steps)]
        (let [op (:op step)
              out (:out step)
              bindings (dissoc step :op :out)
              {:keys [args reads]} (reduce-kv (fn [acc k binding]
                                                (let [{:keys [value read]} (resolve-binding scope binding)]
                                                  (cond-> (assoc-in acc [:args k] value)
                                                    (and read (roots (first (str/split read #"\."))))
                                                    (update :reads (fn [r] (if (contains? r read) r (assoc r read value)))))))
                                              {:args {} :reads reads}
                                              bindings)
              capability (get capabilities op)]
          (if-not capability
            {:ok? false :values values :return nil
             :log (conj log {:out out :op op :error (str "no capability " op)})
             :reads reads :missing [op] :ms (- (now-ms) t0)}
            (let [step-t0 (now-ms)
                  [value error] (try [(capability args) nil]
                                     (catch #?(:clj Exception :cljs :default) e [nil e]))]
              (if error
                {:ok? false :values values :return nil
                 :log (conj log {:out out :op op :error (ex-message error) :data (ex-data error)})
                 :reads reads :missing [] :ms (- (now-ms) t0)}
                (recur (next steps)
                       (assoc scope out value)
                       (assoc values out value)
                       (conj log {:out out :op op :ms (- (now-ms) step-t0)})
                       reads)))))
        (let [[return return-reads] (resolve-return scope (:return construction))]
          {:ok? true :values values :return return :log log
           :reads (reduce-kv (fn [r k v]
                               (if (and (not (contains? r k)) (roots (first (str/split k #"\."))))
                                 (assoc r k v)
                                 r))
                             reads return-reads)
           :missing [] :ms (- (now-ms) t0)})))))

(defn reread
  "Scope and the :reads of an earlier run → the same paths read now, for a
   cache key: when this equals the earlier reads, the run's result stands."
  [scope reads]
  (reduce-kv (fn [acc path _] (assoc acc path (:value (resolve-binding scope path))))
             {} reads))

(defn missing-capabilities
  "Construction and capability table → the ops the table lacks, in step
   order, before running anything."
  [construction capabilities]
  (vec (distinct (remove #(contains? capabilities %) (map :op (:steps construction))))))
