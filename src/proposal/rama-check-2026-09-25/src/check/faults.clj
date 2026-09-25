(ns check.faults
  "Side channel between the runner and topology code. Works only because the
   in-process cluster shares one JVM with the runner. Throwaway.

   A fault is armed by (id, point). Topology code calls attempt!, maybe-block!
   and maybe-fail! at named points; the runner arms failures (throw once, twice,
   ...), arms blocks (a promise the runner releases), and reads attempt counts
   and notes back out.")

(defonce state (atom {}))

(defn reset-all! [] (reset! state {}))

(defn arm-fail!
  "Throw at (id, point) the next n times it is reached. kind picks the class:
   :ex-info (default), :runtime, :intentional (Rama's own
   IntentionalFailureException)."
  ([id point n] (arm-fail! id point n :ex-info))
  ([id point n kind]
   (swap! state assoc-in [id :fail-kind point] kind)
   (swap! state assoc-in [id :fail point] n)))

(defn- make-ex [kind msg data]
  (case kind
    :runtime (RuntimeException. ^String msg)
    :intentional (let [c (Class/forName "rpl.rama.distributed.exceptions.IntentionalFailureException")]
                   (.newInstance (.getConstructor c (into-array Class [String clojure.lang.IPersistentMap]))
                                 (object-array [msg data])))
    (ex-info msg data)))

(defn arm-block!
  "Block at (id, point) until release! is called, or 30 s pass."
  [id point]
  (let [p (promise)]
    (swap! state assoc-in [id :block point] p)
    p))

(defn release! [id point]
  (when-let [p (get-in @state [id :block point])]
    (deliver p :released)))

(defn attempt!
  "Count that (id, point) was reached. Called from dataflow."
  [id point]
  (swap! state update-in [id :attempts point] (fnil inc 0))
  nil)

(defn attempts [id point]
  (get-in @state [id :attempts point] 0))

(defn maybe-block! [id point]
  (when-let [p (get-in @state [id :block point])]
    (let [r (deref p 30000 :timed-out)]
      (swap! state update-in [id :notes :block-result] (fnil conj []) [point r])))
  nil)

(defn maybe-fail! [id point]
  (let [n (get-in @state [id :fail point] 0)]
    (when (pos? n)
      (swap! state assoc-in [id :fail point] (dec n))
      (throw (make-ex (get-in @state [id :fail-kind point] :ex-info)
                      (str "injected failure " id " at " point)
                      {:id id :point point}))))
  nil)

(defn note! [id k v]
  (swap! state update-in [id :notes k] (fnil conj []) v)
  nil)

(defn notes [id k]
  (get-in @state [id :notes k]))

(defn snapshot [id]
  (-> (get @state id {})
      (update :block (fn [m] (into {} (for [[k p] m] [k (if (realized? p) @p :pending)]))))))
