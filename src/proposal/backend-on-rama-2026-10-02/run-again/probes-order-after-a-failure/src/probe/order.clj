(ns probe.order
  "The runs of the probe of 13 October 2026. One question: can a failure in a
  layer that a stream topology decides make acts that were in flight come out
  in another order when the store is run again?

  Each run is one scenario, repeated on a fresh in-process cluster each time:

    module First decides the acts as they arrive, with the failure put in;
    then module Again mirrors First's depot and runs the same gate over it
    from its beginning, with no failure; then the tables of the two modules
    are held against each other, over every act that the depot holds.

  `clojure -M:order <scenario> [key value ...]`, or `clojure -M:order suite
  <name>` for the scenarios of suites/<name>.edn one after another in one
  JVM. The scenarios:

    look   one act, no failure: what a stream event has on its thread (K1)
    none   no failure: four makers, 100 acts each
    s1     A throws once on its first attempt
             :mode      :together  A, B and C are called while an earlier event holds the thread for 400 ms
                        :during    A sleeps 300 ms and then throws; B and C are called while it sleeps
                        :behind    A, B and C are sent one behind the other, no sleep
             :throw-at  :end (after the writes) or :start (before the reads)
    s2     A sleeps on the task's thread on its first attempt only
             :sleep-ms  how long
             :sleep-at  :start (before the reads) or :end (after the writes)
             :window    :before          A and B are called while an earlier event holds the thread for 400 ms
                        :while           B is called 1 s after A began to sleep
                        :after-deadline  B is called 1 s after the stream timeout has passed, A still asleep
                        :after-wake      B is called when A has woken
    s4     A's first attempt writes an answer that Rama cannot write; B and C as in s1
             :mode      as in s1
    hooks  one act, no failure: which of Rama's own test hooks its code calls (see `probe.hooks`)
    s4slow the write of the batch that holds A's first attempt into the task's log is made slow, by such a hook
             :hook      the hook's name · :hook-ms how long its next call sleeps · :window :while or :before
    load   makers send acts at `:rate` a second for `:secs` seconds, four of
           them on each key; one second in, one thing happens:
             :event     :none    nothing
                        :stall   A is sent and sleeps `:sleep-ms`, B behind it
                        :throw   A is sent and throws at its end, B behind it
                        :bad     A is sent and its write cannot be written, B behind it
                        :update  the module First is updated with itself
    every scenario also takes
             :timeout     seconds for `topology.stream.timeout.seconds` (left out: Rama's own value)
             :retry-mode  :all-after (the default), :individual, :none
             :launch      what both modules are launched with (default {:tasks 4 :threads 4})
             :reps        how many times, each on a fresh cluster (default 3)
             :log         the name of the log file under logs/ (default: the scenario)

  The acts. A and B are of one layer and one key and both say `:replaces
  nil`, so the one decided first is accepted and the other refused. C is of
  another layer on the same task. E is of A's layer on another key, decided
  before A is sent. D is of A's layer and key, sent when all is quiet.

  State. Each module has its three tables. The JVM keeps the notes of the
  attempts (`probe.gate/events`). The run keeps nothing else but its log.

  A probe, not a design."
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.rpl.rama.test :as rtest]
            [probe.gate :as g]
            [probe.hooks :as h])
  (:import [java.time LocalTime]
           [java.time.format DateTimeFormatter]
           [java.util.concurrent CompletableFuture]
           [java.util.concurrent.locks LockSupport]))

;; ---- the log ---------------------------------------------------------------

(defonce ^{:doc "The open writer of the run's log file, or nil."} log-writer (atom nil))

(def ^:private clock-format (DateTimeFormatter/ofPattern "HH:mm:ss.SSS"))

(defn say
  "Takes parts of a line. Writes them as one line, with the wall clock and the
  milliseconds since the cluster's clock began in front, to the run's log and
  to stdout behind the word PROBE."
  [& parts]
  (let [line (str (.format (LocalTime/now) clock-format) (format " %9.1f  " (g/ms)) (str/join " " parts))]
    (locking log-writer
      (println (str "PROBE " line))
      (when-let [^java.io.Writer w @log-writer]
        (.write w line) (.write w "\n") (.flush w)))))

(defn root-message
  "Takes a throwable. Gives the class and the first line of the message of its innermost cause, cut to 200 characters."
  [^Throwable t]
  (let [^Throwable r (last (take-while some? (iterate #(.getCause ^Throwable %) t)))
        m (-> (or (.getMessage r) "") (str/split #"\n") first)]
    (str (.getSimpleName (class r)) ": " (subs m 0 (min 200 (count m))))))

(defn wait-until
  "Takes a function of no arguments and a limit in ms. Calls it every 2 ms until it gives a true value or the limit has passed. Gives that value or nil."
  [f limit-ms]
  (let [end (+ (System/nanoTime) (* (long limit-ms) 1000000))]
    (loop []
      (or (try (f) (catch Throwable _ nil))
          (when (< (System/nanoTime) end)
            (LockSupport/parkNanos 2000000)
            (recur))))))

(defn within
  "Takes a limit in ms and a function of no arguments. Runs it on another thread. Gives its value, or :timed-out, or [:threw message]."
  [limit-ms f]
  (let [fut (future (try (f) (catch Throwable t [:threw (root-message t)])))]
    (deref fut limit-ms :timed-out)))

(defn- cut-list
  "Takes a collection. Gives it as text, cut to its first twelve items when it has more."
  [v]
  (if (> (count v) 12)
    (str (pr-str (vec (take 12 v))) " and " (- (count v) 12) " more")
    (pr-str (vec v))))

;; ---- the clients -----------------------------------------------------------

(defn- tables
  "Takes the cluster and a module's name. Gives clients of its three tables."
  [ipc module]
  {:rows (foreign-pstate ipc module "$$rows")
   :answers (foreign-pstate ipc module "$$answers")
   :last (foreign-pstate ipc module "$$last")})

(defn- answer [tables act]
  (within 8000 #(foreign-select-one (keypath (:id act)) (:answers tables) {:pkey (:layer act)})))

(defn- row [tables layer k]
  (within 8000 #(foreign-select-one (keypath [layer k]) (:rows tables) {:pkey layer})))

(defn- last-number [tables layer]
  (within 8000 #(foreign-select-one (keypath layer) (:last tables) {:pkey layer})))

(defn- act
  "Takes a layer, an id and a key. Gives an act that says `:replaces nil`, with no time yet."
  [layer id k]
  {:layer layer :id id :key k :val (str "val-of-" id) :replaces nil})

(defn append!
  "Takes the context and an act. Writes the time on the act and appends it
  with :ack on a thread of its own. Keeps what the call gave and when, in the
  context's :appends. Gives a promise of what the call gave, as text."
  [ctx a]
  (let [a (assoc a :time (System/currentTimeMillis))
        p (promise)
        called (g/ms)]
    (doto (Thread.
           (fn []
             (let [gave (try (pr-str (foreign-append! (:depot ctx) a :ack))
                             (catch Throwable t (root-message t)))]
               (swap! (:appends ctx) conj {:id (:id a) :called called :returned (g/ms) :gave gave})
               (deliver p gave))))
      (.setDaemon true)
      (.setName (str "maker of " (:id a)))
      (.start))
    p))

(defn append-now!
  "Takes the context and an act. Appends it with :ack and waits for the call, at most 40 s. Gives what the call gave, as text."
  [ctx a]
  (deref (append! ctx a) 40000 ":the call did not come back within 40 s"))

(defn append-async!
  "Takes the context and an act. Writes the time on the act and appends it
  with :ack by the client's call that does not wait. Keeps what the call gave
  and when, in the context's :appends."
  [ctx a]
  (let [a (assoc a :time (System/currentTimeMillis))
        called (g/ms)
        keep! (fn [gave] (swap! (:appends ctx) conj {:id (:id a) :called called :returned (g/ms) :gave gave}))]
    (try
      (.whenComplete ^CompletableFuture (foreign-append-async! (:depot ctx) a :ack)
                     (reify java.util.function.BiConsumer
                       (accept [_ v t] (keep! (if t (root-message t) (pr-str v))))))
      (catch Throwable t (keep! (str "the call itself threw " (root-message t)))))
    nil))

;; ---- what the run reads off the JVM's notes --------------------------------

(defn- note-of
  "Takes a module word, an id, an attempt and a word for what happened. Gives the first such note or nil."
  [word id attempt what]
  (first (filter #(and (= word (:word %)) (= id (:id %)) (= attempt (:attempt %)) (= what (:what %))) (g/notes))))

(defn- quiet!
  "Waits until no note was made for `still-ms`, at most `limit-ms`."
  [still-ms limit-ms]
  (let [end (+ (g/ms) limit-ms)]
    (loop [n (count (g/notes)) since (g/ms)]
      (Thread/sleep 100)
      (let [n2 (count (g/notes)) now (g/ms)]
        (cond
          (> now end) nil
          (not= n n2) (recur n2 now)
          (>= (- now since) still-ms) true
          :else (recur n since))))))

(defn- say-notes!
  "Says the notes of a module that carry one of the ids, in the order in which they were made, and each time a task global was made ready again."
  [word ids]
  (doseq [{:keys [t id attempt what] :as n} (g/notes)
          :when (and (= word (:word n)) (or (contains? ids id) (= :task-global-ready-again what)))]
    (say (format "    %9.1f  %s" t
                 (case what
                   :task-global-ready-again (str "the task global of task " (:task n) " is made ready again (time " (:time-n n) "): the worker was started again")
                   :begins (str id " attempt " attempt " begins on task " (:task n))
                   :sleeps (str id " attempt " attempt " sleeps " (:for-ms n) " ms on the task's thread, at the " (name (:at n)) " of the event")
                   :wakes (str id " attempt " attempt " wakes")
                   :throws (str id " attempt " attempt " THROWS on purpose at the " (name (:at n)) " of the event")
                   :decides (str id " attempt " attempt " decides: " (name (:verdict n)) ", number " (:n n))
                   :finds-its-answer (str id " attempt " attempt " finds its answer there and does nothing")
                   :ends (str id " attempt " attempt " ends")
                   (str id " attempt " attempt " " what))))))

;; ---- the tables of the two modules, held against each other ----------------

(defn- number-text
  "Takes a number of an act and the wall ms at which the cluster's clock began. Gives it as `<ms since then>:<the low 16 bits>`."
  [n base]
  (if (number? n) (str (- (quot (long n) 65536) (long base)) ":" (rem (long n) 65536)) (pr-str n)))

(defn- partition-records
  "Takes the depot client and a partition. Gives its records in order, each with :pos, its offset, and :partition."
  [depot p]
  (let [{:keys [start-offset end-offset]} (foreign-depot-partition-info depot p)]
    (loop [at start-offset out []]
      (if (>= at end-offset)
        out
        (let [upto (min end-offset (+ at 500))
              recs (foreign-depot-read depot p at upto)]
          (recur upto (into out (map-indexed (fn [i r] (assoc r :pos (+ at i) :partition p)) recs))))))))

(defn- depot-records
  "Takes the depot client. Gives the records of its four partitions, each partition in order."
  [depot]
  (vec (mapcat #(partition-records depot %) (range 4))))

(defn- first-places
  "Takes the records of a depot. Gives one record for each id: the first that the depot holds."
  [records]
  (vals (reduce (fn [m r] (if (contains? m (:id r)) m (assoc m (:id r) r))) {} records)))

(defn named-by-the-rule
  "The naming rule, pure. Takes the records of a depot, each with :partition,
  :pos, :layer and :id, each partition in order, and a function from an id to
  its number in the first module or nil. Gives {:out-of-line ids, :no-number
  ids}. An act is out of its line when an act of the same layer stands
  before it in its partition with a higher number, or stands behind it with
  a lower number. Of an id that the depot holds more than once, the first
  place counts."
  [records number-of]
  (let [with-n (map #(assoc % :n (number-of (:id %))) (first-places records))
        lines (vals (group-by (juxt :partition :layer) (filter :n with-n)))]
    {:no-number (set (map :id (remove :n with-n)))
     :out-of-line (set (mapcat
                        (fn [line]
                          (let [line (vec (sort-by :pos line))
                                nums (mapv :n line)
                                ;; for each place: the highest number before it, the lowest behind it
                                max-before (vec (butlast (reductions max Long/MIN_VALUE nums)))
                                min-behind (vec (rest (reverse (reductions min Long/MAX_VALUE (reverse nums)))))]
                            (keep-indexed (fn [i x] (when (or (> (max-before i) (:n x)) (< (min-behind i) (:n x))) (:id x)))
                                          line)))
                        lines))}))

(defn fold-gate
  "The gate's rule folded over records in the order given, pure: what a gate
  holds that decides them one after another in that order, with no failure.
  Gives {:answers id → {:verdict :n}, :rows, :last}."
  [records]
  (reduce (fn [st r]
            (if (contains? (:answers st) (:id r))
              st
              (let [rk [(:layer r) (:key r)]
                    {:keys [verdict n new-row]} (g/decide (get (:rows st) rk) (get (:last st) (:layer r)) r)]
                (cond-> (-> st
                            (assoc-in [:answers (:id r)] {:verdict verdict :n n})
                            (assoc-in [:last (:layer r)] n))
                  (= :accepted verdict) (assoc-in [:rows rk] new-row)))))
          {:answers {} :rows {} :last {}}
          records))

(defn- what-if!
  "Computes, with the gate's pure rule and no cluster, two things over the
  records of A's partition, and says them. First: the rule folded over the
  partition in its order, held against what module Again holds. Second: the
  rule folded over the same records with B moved in front of A, which is the
  order that the probe of 12 October saw after a stalled batch; then what
  would differ from Again, and what the naming rule would name. Takes the
  context, the answers of Again by id, and the base of the numbers."
  [ctx again-answers base]
  (let [records (:records ctx)
        a (first (filter #(= "A" (:id %)) records))
        b (first (filter #(= "B" (:id %)) records))]
    (when (and a b (= (:partition a) (:partition b)))
      (let [line (vec (filter #(= (:partition a) (:partition %)) records))
            in-order (:answers (fold-gate line))
            again (into {} (for [r line] [(:id r) (select-keys (again-answers (:id r)) [:verdict :n])]))
            moved (vec (mapcat #(cond (= "A" (:id %)) [b %] (= "B" (:id %)) [] :else [%]) line))
            if-answers (:answers (fold-gate moved))
            differing (set (for [r line :when (not= (if-answers (:id r)) (again (:id r)))] (:id r)))
            named (:out-of-line (named-by-the-rule line #(:n (if-answers %))))
            text (fn [id] (str id " " (name (:verdict (if-answers id))) " number " (number-text (:n (if-answers id)) base)))]
        (say "  PURE   the gate's rule folded over the partition in its order gives exactly the answers that Again holds:" (= in-order again))
        (say "  IF     B had been decided before A in First (computed with the gate's rule, not run):" (text "A") "·" (text "B")
             "· answers that would differ from Again:" (cut-list (sort differing))
             "· the rule would name:" (cut-list (sort named))
             "· exactly those:" (= named differing))))))

(defn- hold-against!
  "Reads both modules and says what each holds for the acts to show, then one
  line with SAME or DIFFERENT over every act that the depot holds. Takes the
  context, the label of the run, and the acts to show in order. Gives a map
  with the outcome."
  [ctx label shown]
  (let [{:keys [first-tables again-tables records base]} ctx
        acts (first-places records)
        ans (fn [tables] (into {} (for [a acts] [(:id a) (answer tables a)])))
        fa (ans first-tables)
        aa (ans again-tables)
        same-answer? (fn [id] (= (select-keys (fa id) [:verdict :n]) (select-keys (aa id) [:verdict :n])))
        differing (set (remove same-answer? (map :id acts)))
        rows-differing (vec (for [[l k] (distinct (map (juxt :layer :key) acts))
                                  :let [rf (row first-tables l k) ra (row again-tables l k)]
                                  :when (not= rf ra)]
                              {:row [l k] :first (:id rf) :again (:id ra)}))
        last-differing (vec (for [l (distinct (map :layer acts))
                                  :let [lf (last-number first-tables l) lg (last-number again-tables l)]
                                  :when (not= lf lg)]
                              {:layer l :first lf :again lg}))
        order-in (fn [answers layer]
                   (->> acts (filter #(= layer (:layer %))) (map :id)
                        (filter #(number? (:n (answers %))))
                        (sort-by #(:n (answers %))) vec))
        named (named-by-the-rule records #(let [n (:n (fa %))] (when (number? n) n)))
        of-row (or (first (filter #(= "A" (:id %)) shown)) (first shown))
        la (:layer of-row)
        part (:partition (first (filter #(= la (:layer %)) records)))
        skip #(vec (remove (fn [id] (str/starts-with? (str id) "find-")) %))]
    (doseq [a shown :let [id (:id a) f (fa id) g2 (aa id)]]
      (say (format "  %-4s First: %-9s number %-10s attempt %-3s | Again: %-9s number %-10s | %s"
                   id (some-> (:verdict f) name) (number-text (:n f) base) (:attempt f)
                   (some-> (:verdict g2) name) (number-text (:n g2) base)
                   (if (same-answer? id) "same" "DIFFERENT"))))
    (say "  the row at" (pr-str [la (:key of-row)]) "· First holds the act" (pr-str (:id (row first-tables la (:key of-row))))
         "· Again holds the act" (pr-str (:id (row again-tables la (:key of-row)))))
    (say "  the order of the decisions in layer" la "by number · First:" (cut-list (skip (order-in fa la)))
         "· Again:" (cut-list (skip (order-in aa la))))
    (say "  the partition of layer" la "in order, [position id]:"
         (cut-list (remove #(str/starts-with? (str (second %)) "find-")
                           (map (juxt :pos :id) (filter #(= part (:partition %)) records)))))
    (say "  the depot holds" (count records) "records of" (count acts) "acts · answers that differ:" (count differing) (cut-list (sort differing))
         "· rows that differ:" (count rows-differing) (cut-list rows-differing)
         "· last numbers that differ:" (count last-differing)
         "· acts with no answer in First:" (count (:no-number named)) (cut-list (sort (:no-number named))))
    (let [breaks (into (:no-number named) (:out-of-line named))
          ;; in each layer: the acts that stand behind the first act that the rule names
          behind (set (mapcat (fn [line]
                                (let [[_ from] (split-with #(not (breaks (:id %))) (sort-by :pos line))]
                                  (map :id (rest from))))
                              (vals (group-by (juxt :partition :layer) acts))))
          fold-answers (apply merge {} (for [[_ recs] (group-by :partition records)] (:answers (fold-gate recs))))
          fold-differing (set (for [a acts :when (not= (select-keys (fa (:id a)) [:verdict :n]) (fold-answers (:id a)))] (:id a)))]
      (say "  RULE   names:" (count breaks) (cut-list (sort breaks))
           "(with no number:" (count (:no-number named)) "· with a number out of its line:" (str (count (:out-of-line named)) ")")
           "· acts that stand behind the first of them in their layer:" (count behind)
           "· the acts whose answer differs:" (count differing)
           "· the rule names exactly those:" (= breaks differing)
           "· every one of those is named or stands behind a named one:" (every? #(or (breaks %) (behind %)) differing))
      (say "  FOLD   acts whose answer in First is not what the gate's rule gives when folded over their partition in order:"
           (count fold-differing) (cut-list (sort fold-differing))
           "· exactly the acts whose answer differs from Again:" (= fold-differing differing))
      (what-if! ctx aa base)
      (let [same? (and (empty? differing) (empty? rows-differing) (empty? last-differing))]
        (say "VERDICT" label "·" (if same? "SAME" "DIFFERENT")
             "· First decided" (cut-list (skip (order-in fa la))) "· Again decided" (cut-list (skip (order-in aa la)))
             "· workers of First started again:" (if (pos? (g/restarts "first")) "yes" "no"))
        {:same? same? :differing differing :named breaks :fold-exact? (= fold-differing differing)}))))

;; ---- one cluster -----------------------------------------------------------

(defn- open!
  "Launches First on an open cluster. Sends one act for each of twelve
  layers to learn each layer's task. Gives the context: the clients, the
  layers chosen (:la, :lc on the same task, :lx on another), and an atom for
  what the appends gave."
  [ipc]
  (rtest/launch-module! ipc g/First (:launch @g/settings))
  (let [first-name (get-module-name g/First)
        ctx {:ipc ipc
             :depot (foreign-depot ipc first-name "*acts")
             :first-tables (tables ipc first-name)
             :base (- (System/currentTimeMillis) (long (g/ms)))
             :appends (atom [])}
        layers (mapv #(format "L%02d" %) (range 12))]
    (doseq [l layers]
      (append-now! ctx (act l (str "find-" l) "find")))
    (let [task (fn [l] (get @g/task-of ["first" l]))
          la (first layers)
          lc (first (filter #(and (not= % la) (= (task %) (task la))) layers))
          lx (first (filter #(not= (task %) (task la)) layers))]
      (when-not (and lc lx) (throw (ex-info "no two layers on one task" {:tasks @g/task-of})))
      (say "RUN     layers" la "and" lc "are on task" (task la) "· layer" lx "is on task" (task lx)
           "· a read of the answer of" (str "find-" la) "from outside gives" (pr-str (answer (:first-tables ctx) {:id (str "find-" la) :layer la})))
      (assoc ctx :la la :lc lc :lx lx))))

(defn- run-again!
  "Reads the depot, launches Again on the cluster and waits until it holds an
  answer for every act that the depot holds, at most 120 s. Gives the context
  with the depot's records and Again's tables."
  [ctx]
  (let [again-name (get-module-name g/Again)
        records (depot-records (:depot ctx))
        acts (first-places records)
        t (g/ms)]
    (rtest/launch-module! (:ipc ctx) g/Again (:launch @g/settings))
    (let [ctx (assoc ctx :again-tables (tables (:ipc ctx) again-name) :records records)
          all? (fn [] (every? #(map? (answer (:again-tables ctx) %)) acts))
          done (wait-until all? 120000)]
      (Thread/sleep 1000)
      (say "RUN     module Again launched and run over the depot from its beginning ·" (count records) "records"
           "· it holds an answer for every act:" (boolean done) "· after" (format "%.0f" (- (g/ms) t)) "ms")
      ctx)))

(defn- say-appends!
  "Says what each :ack append of the ids gave and how long the call took."
  [ctx ids]
  (doseq [{:keys [id called returned gave]} (sort-by :called @(:appends ctx)) :when (contains? ids id)]
    (say (format "  the :ack append of %-3s called at %9.1f, came back after %8.1f ms with %s" id called (- returned called) gave))))

(defn- settle!
  "Waits until every one of the acts has an answer in First, at most `limit-ms`, then until no note was made for 6 s."
  [ctx acts limit-ms]
  (wait-until (fn [] (every? #(map? (answer (:first-tables ctx) %)) acts)) limit-ms)
  (quiet! 6000 60000))

;; ---- the scenarios ---------------------------------------------------------

(defn- finish!
  "The end of a scenario with A and B: sends D when all is quiet, says the
  notes and the appends, runs Again, and holds the two against each other."
  [ctx label {:keys [a b c e h]}]
  (settle! ctx (remove nil? [a b c]) 40000)
  (let [d (act (:la ctx) "D" "k")
        shown (remove nil? [e a b c d])
        ids (set (map :id (remove nil? [e h a b c d])))]
    (say "  the :ack append of D, sent when all was quiet, gave" (append-now! ctx d))
    (Thread/sleep 500)
    (say "  NOTES of module First, in the order in which they were made:")
    (say-notes! "first" ids)
    (say-appends! ctx ids)
    (let [ctx (run-again! ctx)]
      (say "  NOTES of module Again:")
      (say-notes! "again" (set (map :id (remove nil? [a b]))))
      (hold-against! ctx label shown))))

(defn- send-three!
  "Sends A, B and C in one of the three modes of s1. Takes the context, the mode, the acts and the holder."
  [ctx mode a b c h]
  (case mode
    :together (do (swap! g/plan assoc "H" {:sleep-ms 400 :sleep-at :start})
                  (append! ctx h)
                  (wait-until #(note-of "first" "H" 1 :sleeps) 20000)
                  (append! ctx a) (Thread/sleep 60)
                  (append! ctx b) (Thread/sleep 60)
                  (append! ctx c))
    :during (do (swap! g/plan update "A" merge {:sleep-ms 300 :sleep-at :start})
                (append! ctx a)
                (wait-until #(note-of "first" "A" 1 :sleeps) 20000)
                (append! ctx b) (Thread/sleep 60)
                (append! ctx c))
    :behind (do (append-async! ctx a)
                (append-async! ctx b)
                (append-async! ctx c))))

(defn- s1!
  "S1: A throws once on its first attempt."
  [ctx label {:keys [mode throw-at] :or {mode :together throw-at :end}}]
  (let [{:keys [la lc]} ctx
        a (act la "A" "k") b (act la "B" "k") c (act lc "C" "k")
        e (act la "E" "other-key")
        h (act lc "H" "h")]
    (say "RUN     ----" label "· S1, A throws at the" (name throw-at) "of its event on its first attempt · mode" mode "----")
    (say "  the :ack append of E, before all, gave" (append-now! ctx e))
    (swap! g/plan assoc "A" {:throw-at throw-at})
    (send-three! ctx mode a b c h)
    (finish! ctx label {:a a :b b :c c :e e :h (when (= mode :together) h)})))

(defn- s4!
  "S4: A's first attempt writes an answer that Rama cannot write."
  [ctx label {:keys [mode] :or {mode :together}}]
  (let [{:keys [la lc]} ctx
        a (act la "A" "k") b (act la "B" "k") c (act lc "C" "k")
        e (act la "E" "other-key")
        h (act lc "H" "h")]
    (say "RUN     ----" label "· S4, the first attempt of A writes an answer that Rama cannot write · mode" mode "----")
    (say "  the :ack append of E, before all, gave" (append-now! ctx e))
    (swap! g/plan assoc "A" {:bad-write true})
    (send-three! ctx mode a b c h)
    (finish! ctx label {:a a :b b :c c :e e :h (when (= mode :together) h)})))

(defn- s2!
  "S2 and S5: A sleeps on the task's thread on its first attempt only."
  [ctx label {:keys [sleep-ms sleep-at window timeout] :or {sleep-ms 5000 sleep-at :start window :while}}]
  (let [{:keys [la lc]} ctx
        a (act la "A" "k") b (act la "B" "k") c (act lc "C" "k")
        e (act la "E" "other-key")
        h (act lc "H" "h")
        limit-ms (* 1000 (long (or timeout 5)))]
    (say "RUN     ----" label "· S2, A sleeps" sleep-ms "ms on the task's thread at the" (name sleep-at)
         "of its event, first attempt only · window" window "· the stream timeout is" (or timeout "Rama's own") "s ----")
    (say "  the :ack append of E, before all, gave" (append-now! ctx e))
    (swap! g/plan assoc "A" {:sleep-ms sleep-ms :sleep-at sleep-at})
    (case window
      :before (do (swap! g/plan assoc "H" {:sleep-ms 400 :sleep-at :start})
                  (append! ctx h)
                  (wait-until #(note-of "first" "H" 1 :sleeps) 20000)
                  (append! ctx a) (Thread/sleep 60)
                  (append! ctx b) (Thread/sleep 60)
                  (append! ctx c))
      :while (do (append! ctx a)
                 (wait-until #(note-of "first" "A" 1 :sleeps) 20000)
                 (Thread/sleep 1000)
                 (append! ctx b) (Thread/sleep 60)
                 (append! ctx c))
      :after-deadline (do (append! ctx a)
                          (wait-until #(note-of "first" "A" 1 :sleeps) 20000)
                          (Thread/sleep (+ limit-ms 1000))
                          (append! ctx b) (Thread/sleep 60)
                          (append! ctx c))
      :after-wake (do (append! ctx a)
                      (wait-until #(note-of "first" "A" 1 :wakes) (+ sleep-ms 20000))
                      (append! ctx b) (Thread/sleep 60)
                      (append! ctx c)))
    (wait-until #(note-of "first" "A" 1 :wakes) (+ sleep-ms 20000))
    (finish! ctx label {:a a :b b :c c :e e :h (when (= window :before) h)})))

(defn- none!
  "No failure: four makers send 100 acts each over three layers and four keys, every one saying `:replaces nil`."
  [ctx label _]
  (let [{:keys [la lc lx]} ctx
        layers [la lc lx]
        makers (mapv (fn [m]
                       (future
                         (dotimes [i 100]
                           (let [l (layers (mod (+ i m) 3))]
                             (deref (append! ctx (act l (format "n-%d-%03d" m i) (str "k" (mod i 4)))) 40000 nil)))))
                     (range 4))]
    (say "RUN     ----" label "· no failure: four makers, 100 acts each, three layers, four keys ----")
    (run! deref makers)
    (quiet! 2000 30000)
    (let [gave (frequencies (map :gave @(:appends ctx)))
          ctx (run-again! ctx)]
      (say "  what the :ack appends gave:" (pr-str gave))
      (hold-against! ctx label [(first (filter #(= la (:layer %)) (remove #(str/starts-with? (:id %) "find-") (:records ctx))))]))))

(defn- load!
  "Makers send acts at a rate; one second in, one thing happens."
  [ctx label {:keys [rate secs event sleep-ms] :or {rate 200 secs 8 event :stall sleep-ms 5000}}]
  (let [{:keys [la lc ipc]} ctx
        a (act la "A" "kA") b (act la "B" "kA")
        gap (long (/ 4e9 rate))
        t0 (System/nanoTime)
        sent (atom 0)
        makers (mapv (fn [m]
                       (future
                         (loop [i 0]
                           (when (< (- (System/nanoTime) t0) (* secs 1e9))
                             (append-async! ctx (act (if (even? i) la lc) (format "m%d-%05d" m i) (str "k" i)))
                             (swap! sent inc)
                             (let [next-at (+ t0 (* (inc i) gap))]
                               (LockSupport/parkNanos (max 0 (- next-at (System/nanoTime)))))
                             (recur (inc i))))))
                     (range 4))]
    (say "RUN     ----" label "· load:" rate "acts a second for" secs "s, four makers, four acts on each key · one second in:" event "----")
    (Thread/sleep 1000)
    (case event
      :none nil
      :stall (do (swap! g/plan assoc "A" {:sleep-ms sleep-ms :sleep-at :start})
                 (append! ctx a) (Thread/sleep 300) (append! ctx b))
      :throw (do (swap! g/plan assoc "A" {:throw-at :end})
                 (append! ctx a) (Thread/sleep 3) (append! ctx b))
      :bad (do (swap! g/plan assoc "A" {:bad-write true})
               (append! ctx a) (Thread/sleep 3) (append! ctx b))
      :update (let [t (g/ms)
                    r (within 120000 #(rtest/update-module! ipc g/First))]
                (say "  the update of module First gave" (pr-str r) "after" (format "%.0f" (- (g/ms) t)) "ms")))
    (run! deref makers)
    (say "  the makers have sent" @sent "acts")
    (quiet! 6000 120000)
    (Thread/sleep 1000)
    (let [gave (frequencies (map :gave @(:appends ctx)))
          took (vec (sort (map #(- (:returned %) (:called %)) @(:appends ctx))))]
      (say "  what the :ack appends gave:" (pr-str gave))
      (when (seq took)
        (say (format "  the :ack calls took: middle %.1f ms · 99 of 100 under %.1f ms · most %.1f ms"
                     (double (nth took (quot (count took) 2))) (double (nth took (int (* 0.99 (count took))))) (double (last took)))))
      (say "  attempts begun in First:" (reduce + (for [[[w _] n] @g/attempts :when (= w "first")] n))
           "· acts with more than one attempt:" (count (for [[[w _] n] @g/attempts :when (and (= w "first") (> n 1))] n))
           "· workers of First started again:" (g/restarts "first") "task globals made ready again")
      (when (#{:stall :throw :bad} event)
        (say "  NOTES of module First for A and B:")
        (say-notes! "first" #{"A" "B"})
        (say-appends! ctx #{"A" "B"}))
      (let [ctx (run-again! ctx)]
        (hold-against! ctx label (if (#{:stall :throw :bad} event)
                                   [a b]
                                   [(first (remove #(str/starts-with? (:id %) "find-") (:records ctx)))]))))))

(defn- hooks!
  "S4, a look: which of Rama's test hooks its code calls around one act, in what order, and on which threads."
  [ctx label _]
  (say "RUN     ----" label "· Rama's test hooks around one act, no failure ----")
  (doseq [[k v] (sort-by key @h/installed)] (say "  hook" k "·" v))
  (say "  calls before the act, by hook and kind of thread:" (pr-str (sort-by key @h/calls)))
  (reset! h/trace [])
  (reset! h/tracing? true)
  (let [t0 (System/nanoTime)]
    (say "  the :ack append of one act gave" (append-now! ctx (act (:la ctx) "A" "k")))
    (Thread/sleep 300)
    (reset! h/tracing? false)
    (say "  calls of the hooks from the append on," (count @h/trace) "in all; the first 30, [ms since the call · hook · thread]:")
    (doseq [[t hook thread] (take 30 @h/trace)]
      (say (format "    +%8.2f ms  %-34s %s" (/ (- t t0) 1e6) hook thread))))
  (say "  NOTES of module First for the act:")
  (say-notes! "first" #{"A"})
  {:same? true})

(defn- s4slow!
  "S4 with a hook: the end of the batch that holds A's first attempt is made
  slow. When A's event has run its code, the next call of the hook `:hook`
  (from a thread whose name holds `:slow-thread`, when given) sleeps
  `:hook-ms` on its own thread. A may also sleep in its event (`:sleep-ms`).
  B and C are called one second after A's event ended its code, or with
  window :before while an earlier event holds the thread."
  [ctx label {:keys [hook hook-ms slow-thread sleep-ms window] :or {hook-ms 4500 sleep-ms 0 window :while}}]
  (let [{:keys [la lc]} ctx
        a (act la "A" "k") b (act la "B" "k") c (act lc "C" "k")
        e (act la "E" "other-key")
        hh (act lc "H" "h")]
    (say "RUN     ----" label "· S4 with a hook: after A's event has run, the next call of" hook "sleeps" hook-ms "ms on its own thread"
         (if slow-thread (str "(threads named *" slow-thread "* only)") "") "· A sleeps" sleep-ms "ms in its event · window" window "----")
    (say "  the :ack append of E, before all, gave" (append-now! ctx e))
    (reset! h/slow-thread slow-thread)
    (swap! g/plan assoc "A" (cond-> {:end-fn (fn [] (reset! h/slow {hook [hook-ms]}))}
                              (pos? sleep-ms) (assoc :sleep-ms sleep-ms :sleep-at :start)))
    (reset! h/trace [])
    (reset! h/tracing? true)
    (case window
      :before (do (swap! g/plan assoc "H" {:sleep-ms 400 :sleep-at :start})
                  (append! ctx hh)
                  (wait-until #(note-of "first" "H" 1 :sleeps) 20000)
                  (append! ctx a) (Thread/sleep 60)
                  (append! ctx b) (Thread/sleep 60)
                  (append! ctx c))
      :while (do (append! ctx a)
                 (wait-until #(note-of "first" "A" 1 :ends) (+ sleep-ms 20000))
                 (Thread/sleep 1000)
                 (append! ctx b) (Thread/sleep 60)
                 (append! ctx c)))
    (wait-until #(note-of "first" "A" 1 :ends) (+ sleep-ms 20000))
    (Thread/sleep (+ hook-ms 500))
    (reset! h/tracing? false)
    (say "  the thread of A's event:" (:thread (note-of "first" "A" 1 :begins)))
    (let [start @g/clock-start]
      (say "  calls of the hooks while it ran," (count @h/trace) "in all; the first 40, [ms on the run's clock · hook · thread]:")
      (doseq [[t hook thread] (take 40 @h/trace)]
        (say (format "    %9.1f  %-44s %s" (/ (- t (long start)) 1e6) hook thread))))
    (finish! ctx label {:a a :b b :c c :e e :h (when (= window :before) hh)})))

(defn- look!
  "K1: what a stream event has on its thread."
  [ctx label _]
  (let [a (assoc (act (:la ctx) "A" "k") :look true)]
    (say "RUN     ----" label "· one act, no failure: the dynamic vars bound on the thread of its event ----")
    (say "  the :ack append gave" (append-now! ctx a))
    (let [seen @g/looked
          words #"(?i)offset|source|depot|ack|stream|record|partition|batch|retry|attempt"]
      (say "  dynamic vars bound on the event's thread:" (count seen))
      (spit "logs/look-bindings.txt" (str/join "\n" (map #(str/join "  " %) seen)))
      (doseq [[n v] seen :when (re-find words n)]
        (say "    " n "=" v))
      (say "  the whole list is in logs/look-bindings.txt"))
    {:same? true}))

;; ---- the launch ------------------------------------------------------------

(defn- run-scenario!
  "Runs one scenario `:reps` times, each on a fresh cluster, into its own log. Takes the scenario's word and its options."
  [scenario opts]
  (let [reps (:reps opts 3)
        log (str "logs/" (:log opts scenario) ".log")
        run-one (case scenario "s1" s1! "s2" s2! "s4" s4! "none" none! "load" load! "look" look!
                      "hooks" hooks! "s4slow" s4slow!)
        outcomes (atom [])]
    (when (#{"hooks" "s4slow"} scenario) (h/install!))
    (io/make-parents log)
    (reset! log-writer (io/writer log))
    (reset! g/settings (merge {:timeout nil :retry-mode :all-after :launch {:tasks 4 :threads 4}}
                              (select-keys opts [:timeout :retry-mode :launch])))
    (println (str "PROBE SUITE begin " (:log opts scenario)))
    (say "RUN     Rama 1.6.0, in-process · scenario" scenario (pr-str opts)
         "· settings of First:" (pr-str @g/settings))
    (dotimes [i reps]
      (g/reset-state!)
      (h/reset-counts!)
      (let [label (str (:log opts scenario) " run " (inc i) " of " reps)]
        (try
          (with-open [ipc (rtest/create-ipc)]
            (let [ctx (open! ipc)]
              (swap! outcomes conj (run-one ctx label opts))))
          (catch Throwable t
            (say "RUN    " label "DID NOT REACH ITS END:" (root-message t))
            (.printStackTrace t)
            (swap! outcomes conj {:threw true})))))
    (say "RUN     END ·" reps "runs · SAME:" (count (filter #(true? (:same? %)) @outcomes))
         "· DIFFERENT:" (count (filter #(false? (:same? %)) @outcomes))
         "· did not reach the end:" (count (filter :threw @outcomes))
         "· the rule named exactly the differing acts in:" (count (filter #(and (contains? % :named) (= (:named %) (:differing %))) @outcomes))
         "· the fold named exactly the differing acts in:" (count (filter :fold-exact? @outcomes)))
    (println (str "PROBE SUITE end " (:log opts scenario)))
    (when-let [^java.io.Writer w @log-writer] (.close w))
    (reset! log-writer nil)))

(defn -main
  "Runs one scenario, or with the word `suite` several one after another in
  this one JVM, and ends the JVM. A suite is a file under suites/ that holds
  a vector of maps: each map the options of one scenario, with `:scenario`."
  [& args]
  (if (= "suite" (first args))
    (doseq [opts (read-string (slurp (str "suites/" (second args) ".edn")))]
      (run-scenario! (:scenario opts) (dissoc opts :scenario)))
    (run-scenario! (first args)
                   (into {} (map (fn [[k v]] [(read-string k) (read-string v)]) (partition 2 (rest args))))))
  (shutdown-agents)
  (System/exit 0))
