(ns probe.digests
  "The run of ruling 19's condition: is the digest of a thing as a client
  appended it the digest of the thing as a topology takes it off the depot?

  A module of the probe's own, on Rama 1.6.0 in-process. Two depots with the
  shapes of the store's two partitioners: one placed by the hash of the
  thing's `:layer`, read by a stream topology; one placed by the hash of the
  entity of the thing's first fact (the store's own pure function
  `softland.store.gate.batch/first-entity`), read by a microbatch topology.
  Each topology says what it sees of each thing (`probe.seen/seen-of`) and
  keeps that in a row under the uuid of the thing's act-id.

  For each case the run makes a thing as the door would append it under
  ruling 19: the maker's act with two parts added, `:act-id` and `:depot`,
  and nothing else changed. It takes `seen-of` of the thing before the
  append, appends it to each depot, reads what the topology saw, and reads
  the record back from the depot's log as a client and takes `seen-of` of
  that too. A line of the log says for each case and depot whether the three
  agree, and where a class changed on the way.

  The groups of cases:

    V  a value of each kind that the form takes, as the :v of one fact
    F  :facts given as a vector, a list, a lazy sequence and other sequences
    A  parts of the act that are no value: its own permission, its sets
    N  kinds that the form does not take. A gate refuses each; its maker
       still reads the answer by her digest

  Run, from this folder:  flock ../runs/cluster.lock clojure -M:digests
  The log is logs/digests.log. Nothing here is a form for the store."
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [com.rpl.rama.test :as rtest]
            [probe.seen :as seen]
            [softland.store.gate.batch :as batch])
  (:import [java.util UUID]
           [java.util.concurrent CompletableFuture TimeUnit]))

;; ---- the module -----------------------------------------------------------

(defmodule DigestProbe [setup topologies]
  (declare-depot setup *by-layer (hash-by :layer))
  (declare-depot setup *by-first-entity (hash-by batch/first-entity))
  (let [s (stream-topology topologies "stream")]
    (declare-pstate s $$seen-by-stream {String Object})
    (<<sources s
      (source> *by-layer {:start-from :beginning :retry-mode :all-after} :> *thing)
      (seen/id-of *thing :> *id)
      (seen/seen-of *thing :> *seen)
      (|hash *id)
      (local-transform> [(keypath *id) (termval *seen)] $$seen-by-stream)))
  (let [mb (microbatch-topology topologies "batch")]
    (declare-pstate mb $$seen-by-batch {String Object})
    (<<sources mb
      (source> *by-first-entity {:start-from :beginning} :> %things)
      (%things :> *thing)
      (seen/id-of *thing :> *id)
      (seen/seen-of *thing :> *seen)
      (|hash *id)
      (local-transform> [(keypath *id) (termval *seen)] $$seen-by-batch))))

;; ---- the log --------------------------------------------------------------

(defonce ^:private out (atom nil))

(defn- say
  "Writes one line to the run's log and to stdout behind the word PROBE."
  [& parts]
  (let [line (str/join " " parts)]
    (locking out
      (when-let [w @out] (.write ^java.io.Writer w (str line "\n")) (.flush ^java.io.Writer w))
      (println "PROBE" line))))

(defn- root-message
  "The class and message of the innermost cause of a throwable, cut."
  [^Throwable t]
  (let [r (loop [t t] (if-let [c (.getCause t)] (recur c) t))
        m (str (.getName (class r)) ": " (.getMessage r))]
    (seen/shown (if (> (count m) 260) (subs m 0 260) m))))

;; ---- the things -----------------------------------------------------------

(def ^:private who (UUID/fromString "00000000-0000-4000-8000-0000000000a1"))
(def ^:private session (UUID/fromString "00000000-0000-4000-8000-0000000000b1"))
(def ^:private granted (UUID/fromString "00000000-0000-4000-8000-0000000000c1"))

(defn- fact
  "One fact with a value, on a fresh entity."
  [v]
  {:e (UUID/randomUUID) :k :probe/v :v v})

(defn- makers-act
  "A maker's act in a layer, with the facts and the other parts given."
  [layer facts more]
  (merge {:version 1 :who who :layer layer :permission [who layer granted] :session session :facts facts}
         more))

(defn- as-appended
  "What the door appends under ruling 19: the maker's act with `:act-id` and
  `:depot` added and nothing else changed."
  [act depot-word]
  (assoc act :act-id [(:layer act) depot-word (UUID/randomUUID)] :depot depot-word))

(defn- nested
  "x inside n vectors."
  [x n]
  (nth (iterate vector x) n))

(defn- permission-of-depth
  "A permission id that holds d permission ids, itself counted."
  [layer d]
  (reduce (fn [parent i] [(UUID/randomUUID) layer (UUID/randomUUID) parent])
          [(UUID/randomUUID) layer (UUID/randomUUID)]
          (range (dec d))))

(defrecord ARecord [a])

(def ^:private two-hundred-thousand (apply str (repeat 200000 "a")))

(defn- value-cases
  "Group V: [label value] for a value of each kind that the form takes."
  []
  [["nil, the :v of a retract" nil]
   ["true" true]
   ["false" false]
   ["an empty string" ""]
   ["a plain string" "a plain string"]
   ["a string with a quote, a backslash, a newline and a tab" "a \" b \\ c \n d \t e"]
   ["a string with U+0000" (str "a" (char 0) "b")]
   ["a string that is U+0000 alone" (str (char 0))]
   ["a string with characters outside the first plane (U+1D11E, U+1F600, U+10FFFF)"
    (str "x" (String. (Character/toChars 0x1D11E)) (String. (Character/toChars 0x1F600)) (String. (Character/toChars 0x10FFFF)) "y")]
   ["a string with both: U+0000 and U+1F600" (str (char 0) (String. (Character/toChars 0x1F600)) (char 0))]
   ["a string with U+FFFF, U+FEFF, U+2028, U+00E9, U+0080, U+007F, U+001F"
    (str (char 0xFFFF) (char 0xFEFF) (char 0x2028) (char 0xE9) (char 0x80) (char 0x7F) (char 0x1F))]
   ["a string of 65,536 a" (apply str (repeat 65536 "a"))]
   ["a string of 200,000 a" two-hundred-thousand]
   ["a string of 30,000 times U+1F600 (120,000 UTF-8 bytes)" (apply str (repeat 30000 (String. (Character/toChars 0x1F600))))]
   ["a keyword" :word]
   ["a keyword with a namespace" :softland.probe/word]
   ["a keyword with the marks that read back" :a.b-c_d?/e*+!<>=$%&'#]
   ["a UUID" (UUID/randomUUID)]
   ["the UUID of all zeros" (UUID. 0 0)]
   ["the UUID of all ones" (UUID. -1 -1)]
   ["0" 0]
   ["-1" -1]
   ["Long/MAX_VALUE" Long/MAX_VALUE]
   ["Long/MIN_VALUE" Long/MIN_VALUE]
   ["an Integer, 7" (int 7)]
   ["Integer/MIN_VALUE as an Integer" Integer/MIN_VALUE]
   ["a Short, 7" (short 7)]
   ["a Byte, -7" (byte -7)]
   ["a big integer that fits a long, 5N" 5N]
   ["a big integer one past the long range" (inc' Long/MAX_VALUE)]
   ["a big integer one below the long range" (dec' Long/MIN_VALUE)]
   ["a big integer, 2 to the 200" (.pow (biginteger 2) 200)]
   ["a java.math.BigInteger that fits a long, 5" (BigInteger. "5")]
   ["a java.math.BigInteger of 30 digits" (BigInteger. "123456789012345678901234567890")]
   ["a java.math.BigInteger, minus 2 to the 200" (.negate (.pow (biginteger 2) 200))]
   ["0.0" 0.0]
   ["-0.0" -0.0]
   ["1.5" 1.5]
   ["0.1" 0.1]
   ["Double/MIN_VALUE" Double/MIN_VALUE]
   ["Double/MAX_VALUE" Double/MAX_VALUE]
   ["minus Double/MAX_VALUE" (- Double/MAX_VALUE)]
   ["Double/MIN_NORMAL" Double/MIN_NORMAL]
   ["1.0E21" 1.0E21]
   ["1.0E-5" 1.0E-5]
   ["9007199254740993.0, past 2 to the 53" 9007199254740993.0]
   ["0M" 0M]
   ["1M" 1M]
   ["1.0M" 1.0M]
   ["1.00M, with trailing zeros" 1.00M]
   ["a decimal 1E+3" (BigDecimal. "1E+3")]
   ["a decimal 1000" (BigDecimal. "1000")]
   ["a decimal zero with a scale, 0E-10" (BigDecimal. "0E-10")]
   ["-0.5M" -0.5M]
   ["a decimal of 60 digits" (BigDecimal. "3.14159265358979323846264338327950288419716939937510582097494")]
   ["a decimal 1E-400" (BigDecimal. "1E-400")]
   ["a decimal 1E+400" (BigDecimal. "1E+400")]
   ["an empty vector" []]
   ["an empty map" {}]
   ["an empty set" #{}]
   ["a vector" [1 2 3]]
   ["a subvector" (subvec [0 1 2 3 4] 1 4)]
   ["a vector with nil inside" [nil 1 nil]]
   ["a vector of 40 longs (past one chunk)" (vec (range 40))]
   ["a vector of mixed kinds" [1 "1" :1 1.0 1M 1N true nil (UUID. 0 1)]]
   ["a map" {:a 1}]
   ["a map with a nil value" {:a nil}]
   ["a map with a nil key" {nil 1}]
   ["a map with nil for key and value" {nil nil}]
   ["a map with keys of several kinds" {1 :long "1" :string :1 :keyword 1.5 :double [1] :vector #{1} :set {1 1} :map (UUID. 0 1) :uuid true :true}]
   ["a sorted map" (sorted-map :b 2 :a 1 :c 3)]
   ["a map of 100 entries" (into {} (map (fn [i] [(keyword (str "k" i)) i]) (range 100)))]
   ["a map of 9 entries built as an array map" (apply array-map (mapcat (fn [i] [(keyword (str "k" i)) i]) (range 9)))]
   ["a set" #{1 2 3}]
   ["a set with nil inside" #{nil 1}]
   ["a sorted set" (sorted-set 3 1 2)]
   ["a set of 100 strings" (into #{} (map #(str "s" %) (range 100)))]
   ["a set of sets and vectors" #{#{1 2} [1 2] #{} []}]
   ["nested: a map of a vector of a map of a set of a vector with nil" {:a [1 {:b #{2 [3 nil]}}] :c nil}]
   ["nested: a vector that holds a subvector, a sorted map, a decimal with trailing zeros and an Integer"
    [(subvec [1 2 3] 1) (sorted-map :z 1 :y 2) 2.50M (int 3) -0.0 5N]]
   ["a 1 inside 32 vectors, the deepest value" (nested 1 32)]
   ["a nil inside 32 vectors" (nested nil 32)]
   ["a map inside 31 vectors, with nil inside" (nested {:k nil} 31)]])

(defn- facts-cases
  "Group F: [label facts more] for `:facts` in each form."
  []
  (let [f1 (fact "one") f2 (fact "two") f0 (fact "zero")]
    [["a vector of two facts" [f1 f2] nil]
     ["a list of two facts" (list f1 f2) nil]
     ["a lazy sequence of two facts, not yet walked" (map identity [f1 f2]) nil]
     ["a lazy sequence of two facts, walked before" (doall (map identity [f1 f2])) nil]
     ["a lazy sequence made by concat" (concat [f1] [f2]) nil]
     ["a lazy sequence made by lazy-seq and cons" (lazy-seq (cons f1 (lazy-seq (cons f2 nil)))) nil]
     ["(seq vector), a chunked sequence" (seq [f1 f2]) nil]
     ["(rest vector)" (rest [f0 f1 f2]) nil]
     ["(cons fact list), a Cons" (cons f1 (list f2)) nil]
     ["a subvector of facts" (subvec [f0 f1 f2] 1) nil]
     ["a lazy sequence of 40 facts" (map (fn [i] (fact i)) (range 40)) nil]
     ["a list of one fact" (list f1) nil]
     ["an empty list" () nil]
     ["an empty lazy sequence" (lazy-seq nil) nil]
     ["an empty vector" [] nil]
     ["a vector whose fact is a sorted map" [(into (sorted-map) f1)] nil]
     ["a vector whose fact has no :v at all, a retract" [{:e (UUID/randomUUID) :k :probe/v}] nil]
     ["a vector whose fact has :v nil and :replaces nil" [{:e (UUID/randomUUID) :k :probe/v :v nil :replaces nil}] nil]]))

(defn- act-cases
  "Group A: [label facts more] for parts of the act that are no value."
  []
  (let [layer :probe-layer-a]
    [["the whole act a sorted map" [(fact 1)] :sorted]
     [":subjects a sorted set, :based-on a sorted map" [(fact 1)]
      {:subjects (sorted-set (UUID. 0 5) (UUID. 0 4))
       :based-on (sorted-map [[layer :streaming (UUID. 0 9)] 0] 7
                             [[layer :streaming (UUID. 0 8)] 1] (int 9))}]
     [":claimed-when an Integer, :version an Integer" [(fact 1)] {:claimed-when (int 5) :version (int 1)}]
     ["an own permission of depth 4" [(fact 1)] {:permission (permission-of-depth layer 4)}]
     ["an own permission of depth 5" [(fact 1)] {:permission (permission-of-depth layer 5)}]
     ["an own permission of depth 32" [(fact 1)] {:permission (permission-of-depth layer 32)}]
     ["an own permission of depth 33" [(fact 1)] {:permission (permission-of-depth layer 33)}]
     ["an own permission of depth 63" [(fact 1)] {:permission (permission-of-depth layer 63)}]
     ["an own permission of depth 64" [(fact 1)] {:permission (permission-of-depth layer 64)}]
     ["an own permission of depth 65" [(fact 1)] {:permission (permission-of-depth layer 65)}]
     ["an own permission of depth 200" [(fact 1)] {:permission (permission-of-depth layer 200)}]
     ["a given permission of depth 32, as the :v of a fact" [(fact (permission-of-depth layer 32))] nil]
     ["a given permission of depth 33, as the :v of a fact" [(fact (permission-of-depth layer 33))] nil]
     ["a given permission of depth 5 with the fifth slot" [(fact (conj (permission-of-depth layer 5) :see))] nil]]))

(defn- not-taken-cases
  "Group N: [label value] for kinds that the form does not take."
  []
  [["a list as a value" (list 1 2)]
   ["a lazy sequence as a value" (map inc [1 2])]
   ["a ratio" 1/2]
   ["a Java float" (float 1.5)]
   ["a character" \a]
   ["a symbol" 'sym]
   ["NaN" Double/NaN]
   ["Infinity" Double/POSITIVE_INFINITY]
   ["a keyword that does not read back" (keyword "a b")]
   ["a string with a lone high surrogate, U+D83D" (str "a" (char 0xD83D) "b")]
   ["a string with a lone low surrogate, U+DE00" (str "a" (char 0xDE00) "b")]
   ["a 1 inside 33 vectors" (nested 1 33)]
   ["a 1 inside 70 vectors, past the text's 64" (nested 1 70)]
   ["a run of bytes" (byte-array [1 2 3])]
   ["a java.util.Date" (java.util.Date. 0)]
   ["a java.util.ArrayList" (java.util.ArrayList. [1 2])]
   ["a java.util.HashMap" (java.util.HashMap. {"a" 1})]
   ["an array of longs" (long-array [1 2])]
   ["a record" (->ARecord 1)]
   ["a plain java.lang.Object" (Object.)]])

(defn- not-taken-facts-cases
  "Group N, for `:facts` that is no sequence."
  []
  (let [f1 (fact "one")]
    [[":facts a java.util.ArrayList of one fact" (java.util.ArrayList. [f1]) nil]
     [":facts a set of one fact" #{f1} nil]
     [":facts one fact and no sequence" f1 nil]
     [":facts nil" nil nil]]))

;; ---- the run --------------------------------------------------------------

(defn- send!
  "Appends a thing to a depot and waits at most 30 seconds. Gives :ok, or
  [:threw message]."
  [depot thing ack]
  (try
    (let [^CompletableFuture f (foreign-append-async! depot thing ack)]
      (.get f 30 TimeUnit/SECONDS)
      :ok)
    (catch Throwable t [:threw (root-message t)])))

(defn- read-log
  "Reads every record of every partition of a depot. Gives a map from the
  uuid text of a record's act-id to the record."
  [depot]
  (let [n (:num-partitions (foreign-object-info depot))]
    (into {}
          (for [p (range n)
                :let [{:keys [start-offset end-offset]} (foreign-depot-partition-info depot p)]
                from (range start-offset end-offset 50)
                r (foreign-depot-read depot p from (min end-offset (+ from 50)))]
            [(seen/id-of r) r]))))

(defn- wait-for-row
  "Reads a row until it is there or 30 seconds have passed. Gives the row or
  nil."
  [pstate id]
  (let [end (+ (System/currentTimeMillis) 30000)]
    (loop []
      (let [row (foreign-select-one (keypath id) pstate)]
        (cond row row
              (> (System/currentTimeMillis) end) nil
              :else (do (Thread/sleep 100) (recur)))))))

(def ^:private compared [:digest :text :facts-class :v-class :v-classes :parse :row-text :row-bytes :row-digest])

(defn- differences
  "The keys of `compared` at which two maps of `seen-of` differ."
  [a b]
  (vec (filter #(not= (get a %) (get b %)) compared)))

(defn- run-case!
  "One case through one depot. Says its lines. Gives a map of what it found."
  [ctx group label depot-word act]
  (let [{:keys [depots rows]} ctx
        thing (as-appended act depot-word)
        id (seen/id-of thing)
        door (seen/seen-of thing)
        ack (if (= :microbatch depot-word) :append-ack :ack)
        sent (send! (get depots depot-word) thing ack)]
    (if (not= :ok sent)
      (do (say "CASE" group "|" label "|" depot-word "| NOT APPENDED:" (second sent))
          {:group group :label label :depot depot-word :appended false})
      (let [gate (wait-for-row (get rows depot-word) id)]
        {:group group :label label :depot depot-word :appended true :id id :thing thing :door door :gate gate}))))

(defn- say-case!
  "Says the lines of one case that was appended, with what a client read
  back from the log."
  [{:keys [group label depot thing door gate]} back]
  (let [log-seen (when back (seen/seen-of back))
        d-gate (if gate (differences door gate) [:no-row])
        d-log (if back (differences door log-seen) [:not-in-the-log])
        digests? (and gate (= (:digest door) (:digest gate)))
        log-digests? (and back (= (:digest door) (:digest log-seen)))]
    (say "CASE" group "|" label "|" depot
         "| door digest = gate digest:" (boolean digests?)
         "| door digest = digest of the record read back by a client:" (boolean log-digests?)
         "| (= thing, record read back):" (if back (try (= thing back) (catch Throwable _ :threw)) :none)
         "| :facts" (:facts-class door) "->" (:facts-class gate) "->" (:facts-class log-seen)
         "| :v" (:v-class door) "->" (:v-class gate) "->" (:v-class log-seen)
         "| parse" (:parse door) "->" (:parse gate)
         "| row bytes" (:row-bytes door) "->" (:row-bytes gate))
    (when (seq d-gate)
      (say "  DIFF door/gate at" (pr-str d-gate))
      (doseq [k d-gate :when (keyword? k)]
        (say "    " k "door:" (pr-str (get door k)))
        (say "    " k "gate:" (pr-str (get gate k)))))
    (when (and (seq d-log) (not= d-log d-gate))
      (say "  DIFF door/log-read at" (pr-str d-log))
      (doseq [k d-log :when (keyword? k)]
        (say "    " k "door:    " (pr-str (get door k)))
        (say "    " k "log-read:" (pr-str (get log-seen k)))))
    {:group group :label label :depot depot
     :digests? (boolean digests?) :log-digests? (boolean log-digests?)
     :class-changed? (boolean (or (not= (:facts-class door) (:facts-class gate))
                                  (not= (:v-classes door) (:v-classes gate))))
     :row-same? (and gate (= (:row-digest door) (:row-digest gate)))}))

(defn- run-group!
  "Runs the cases of one group through both depots, then reads both logs
  back and says each case. Gives the found maps."
  [ctx group cases]
  (say "RUN     ---- group" group "·" (count cases) "cases, each through both depots ----")
  (let [sent (vec (for [[label act] cases
                        depot-word [:streaming :microbatch]]
                    (run-case! ctx group label depot-word act)))
        logs {:streaming (read-log (get-in ctx [:depots :streaming]))
              :microbatch (read-log (get-in ctx [:depots :microbatch]))}]
    (vec (for [c sent]
           (if (:appended c)
             (say-case! c (get-in logs [(:depot c) (:id c)]))
             c)))))

(defn- say-summary!
  [found]
  (doseq [[group cs] (group-by :group found)
          [depot ds] (group-by :depot cs)]
    (let [appended (filter #(not (false? (:appended %))) ds)]
      (say "SUM     group" group "·" depot "· cases" (count ds)
           "· appended" (count appended)
           "· door digest = gate digest:" (count (filter :digests? appended))
           "· door digest = log-read digest:" (count (filter :log-digests? appended))
           "· a class changed on the way:" (count (filter :class-changed? appended))
           "· not appended:" (count (filter #(false? (:appended %)) ds)))
      (doseq [c appended :when (not (:digests? c))]
        (say "SUM       DIFFERS at the gate:" (:label c)))
      (doseq [c appended :when (and (:digests? c) (not (:log-digests? c)))]
        (say "SUM       DIFFERS at the client's read of the log:" (:label c)))
      (doseq [c ds :when (false? (:appended c))]
        (say "SUM       NOT APPENDED:" (:label c))))))

(defn -main [& _]
  (io/make-parents "logs/digests.log")
  (reset! out (io/writer "logs/digests.log"))
  (try
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc DigestProbe {:tasks 4 :threads 2})
      (let [m (get-module-name DigestProbe)
            ctx {:depots {:streaming (foreign-depot ipc m "*by-layer")
                          :microbatch (foreign-depot ipc m "*by-first-entity")}
                 :rows {:streaming (foreign-pstate ipc m "$$seen-by-stream")
                        :microbatch (foreign-pstate ipc m "$$seen-by-batch")}}
            layer-of (fn [i] (keyword (str "probe-layer-" (mod i 7))))
            v (map-indexed (fn [i [label value]] [label (makers-act (layer-of i) [(fact value)] nil)]) (value-cases))
            f (map-indexed (fn [i [label facts more]] [label (makers-act (layer-of i) facts more)]) (facts-cases))
            a (map (fn [[label facts more]]
                     [label (if (= :sorted more)
                              (into (sorted-map) (makers-act :probe-layer-a facts nil))
                              (makers-act :probe-layer-a facts more))])
                   (act-cases))
            n (concat
               (map-indexed (fn [i [label value]] [label (makers-act (layer-of i) [(fact value)] nil)]) (not-taken-cases))
               (map-indexed (fn [i [label facts more]] [label (makers-act (layer-of i) facts more)]) (not-taken-facts-cases)))]
        (say "RUN     Rama 1.6.0, in-process, 4 tasks, 2 threads · Clojure" (clojure-version)
             "· Java" (System/getProperty "java.version")
             "· the depots:" (pr-str (foreign-object-info (get-in ctx [:depots :streaming])))
             (pr-str (foreign-object-info (get-in ctx [:depots :microbatch]))))
        (let [found (vec (concat (run-group! ctx "V" v)
                                 (run-group! ctx "F" f)
                                 (run-group! ctx "A" a)
                                 (run-group! ctx "N" n)))]
          (say "RUN     ---- the short of it ----")
          (say-summary! found))
        (say "RUN     END")))
    (catch Throwable t
      (say "RUN     THREW" (root-message t))
      (.printStackTrace t)))
  (shutdown-agents)
  (System/exit 0))
