(ns softland.store.rebuild-pure-proof
  "The proof of the rebuild's pure core, `softland.store.rebuild`, on logs
  that are made by hand. It needs no cluster and no Rama. One command, from
  the repository root:

    clojure -M:store -m softland.store.rebuild-pure-proof

  It runs on the alias that holds Clojure alone, so a pass also holds that
  the rebuild's core loads with no Rama. It prints each thing that it
  showed on a line that starts with SHOWN, prints those lines again
  together at the end, and exits with a status that is not zero when a
  check fails.

  Approach. The logs are values here, in the form that a client's read of a
  depot gives: a vector of partitions, each a vector of records. Two tasks.
  A layer `:alice` whose gate decides each act in an event of its own, with
  every thing on partition 1; and a layer `:base` whose gate decides in
  batches. The proof writes each log, hands them to
  `softland.store.rebuild/from-logs`, and holds each table that comes back
  against rows written here as literals: the text of a value, of who wrote
  it and of the permission are written out, and a digest is taken on a
  second road, the JVM's SHA-256 over the UTF-8 of a text, not through the
  store's own digest functions. An entry of a log of answers is written
  with that second digest too, so a pass holds that the rebuild ties an
  entry to its thing by the digest that a gate takes.

  The placing of a row of a batch is the proof's own function, `task-of`:
  a table for the routes that are words, the last bit of a UUID, the first
  character of a digest. It stands in the place of the module's hash
  partitioner, which no log holds.

  State. The proof owns one atom, the lines that it showed.

  What a pass does not show. That a gate appends these records: the logs of
  answers are made by hand, in the forms of the plan of `answers-in-a-log`.
  That the tables which the gates write hold these rows: that is the
  comparison on a running module, which is another proof. Anything of
  Rama's hash, and anything of a store that is not at rest."
  (:require [clojure.test :refer [deftest is run-tests]]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.text :as text]
            [softland.store.rebuild :as rebuild])
  (:import [java.nio.charset StandardCharsets]
           [java.security MessageDigest]
           [java.util UUID]))

(defonce ^{:private true
           :doc "The lines that the proof has printed with SHOWN, in order, for the
  recap that `-main` prints at the end."}
  shown-lines (atom []))

(defn- shown
  "Prints one line of what the proof observed, and keeps it for the recap
  that `-main` prints at the end."
  [& xs]
  (let [line (apply print-str "SHOWN" xs)]
    (swap! shown-lines conj line)
    (println line)
    (flush)))

;; ------------------------------------------------------- the second road

(defn- sha
  "Takes a text. Gives the JVM's SHA-256 of its UTF-8, 32 bytes."
  [^String s]
  (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s StandardCharsets/UTF_8)))

(defn- hex
  "Takes a run of bytes. Gives its hex text, in the proof's own code."
  [^bytes b]
  (apply str (map (fn [x] (format "%02x" (bit-and 0xff (long x)))) b)))

(defn- digest-of
  "The digest of a thing on the second road: the SHA-256 of the UTF-8 of its
  one text, 32 bytes. For a thing that holds no bytes that is the digest
  that a gate takes of it."
  [thing]
  (sha (text/text thing)))

(defn- value-digest
  "Takes the text of a value as a fact's row holds it. Gives the part of a
  key of the facts by value that stands for it: the hex text of the SHA-256
  of its UTF-8."
  [value-text]
  (hex (sha value-text)))

;; ------------------------------------------------------ the logs' parts

(def ^:private tasks
  "The number of tasks of the store that the logs are of."
  2)

(defn- uuid
  "The UUID with the number `n` in its low half."
  [n]
  (UUID. 0 n))

(defn- task-of
  "The proof's placing: takes a route, gives its task. A word has the task
  that the table says; a UUID the task of its last bit; a digest task 0 when
  its first character is below 8, else task 1."
  [route]
  (cond
    (keyword? route) (get {:microbatch 1 :base 0 :thing-1 0 :thing-2 1 :thing-3 0 :thing-4 1} route)
    (uuid? route) (int (mod (.getLeastSignificantBits ^UUID route) 2))
    (string? route) (if (neg? (compare (subs route 0 1) "8")) 0 1)))

(defn- thing
  "An act of `:alice` as it arrives on a depot: takes the depot's word, the
  layer, the number of its act-id's UUID, and its facts."
  [depot layer n & facts]
  {:act-id [layer depot (uuid n)] :depot depot
   :version 1 :who :alice :layer layer :permission [:alice layer :alice] :session :alice-s1
   :facts (vec facts)})

(defn- entry
  "An answer's entry for a thing: its act-id, the stamp, the digest of the
  thing on the second road, and the word of a refusal when one is given."
  ([thing stamp] (entry thing stamp nil))
  ([thing stamp refused]
   (cond-> {:act-id (:act-id thing) :stamp stamp :digest (digest-of thing)}
     refused (assoc :refused refused))))

(defn- answer
  "The answer's row that the tables must hold for a thing, with its digest
  as hex text."
  ([thing stamp] (answer thing stamp nil))
  ([thing stamp refused]
   (cond-> {:stamp stamp :digest (hex (digest-of thing))}
     refused (assoc :refused refused))))

(defn- answer-key
  "The key of the answer under an act-id of a layer, written out."
  [layer n]
  (str layer " " (uuid n)))

(defn- row
  "A fact's row as the tables must hold it, written out: the first fact of
  the act with UUID number `n`, at a stamp, by `:alice` under her first
  permission in the layer, with the text of its value when it has one."
  ([layer n stamp] (row layer n stamp nil))
  ([layer n stamp value-text]
   (cond-> {:by-act (uuid n) :by-index 0 :stamp stamp
            :who ":alice" :permission (str "[:alice " layer " :alice]")}
     value-text (assoc :value value-text))))

;; -------------------------------------- the layer whose acts are decided alone

(def ^:private a-marks
  "The act that marks the key `:text` in `:alice`."
  (thing :streaming :alice 1 {:e :alice :k :softland.store/marked-keys :v #{:text}}))

(def ^:private a-first
  "The first fact of `:note-1` under the marked key. It is in the log twice."
  (thing :streaming :alice 2 {:e :note-1 :k :text :v "a text"}))

(def ^:private a-other
  "A second and different thing under the act-id of `a-first`."
  (thing :streaming :alice 2 {:e :note-9 :k :text :v "another thing"}))

(def ^:private a-refused
  "An act whose entry holds a refusal."
  (thing :streaming :alice 3 {:e :note-2 :k :text :v "refused"}))

(def ^:private a-replaced
  "The act that writes `:note-1` a second time."
  (thing :streaming :alice 4 {:e :note-1 :k :text :v "a new text"}))

(def ^:private a-to-go
  "A fact under the marked key that `a-retract` takes back."
  (thing :streaming :alice 5 {:e :note-3 :k :text :v "to go"}))

(def ^:private a-retract
  "The retract of the fact of `a-to-go`: a fact with no value."
  (thing :streaming :alice 6 {:e :note-3 :k :text}))

(def ^:private a-mark-off
  "The act that takes the mark off `:text`."
  (thing :streaming :alice 7 {:e :alice :k :softland.store/marked-keys :v #{}}))

(def ^:private a-mark-again
  "The act that marks `:text` again."
  (thing :streaming :alice 8 {:e :alice :k :softland.store/marked-keys :v #{:text}}))

(def ^:private a-third
  "The act that writes `:note-1` under the second marking."
  (thing :streaming :alice 9 {:e :note-1 :k :text :v "a third text"}))

(def ^:private a-not-in-log
  "An act that no log of acts holds. The log of answers holds an entry of it."
  (thing :streaming :alice 20 {:e :note-7 :k :text :v "in no log"}))

(def ^:private a-two
  "An act whose act-id has two entries that differ in the stamp."
  (thing :streaming :alice 21 {:e :note-4 :k :text :v "two answers"}))

(def ^:private a-no-session
  "A thing that the check of today refuses, for it names no session. Its
  entry says accepted."
  (dissoc (thing :streaming :alice 22 {:e :note-5 :k :text :v "no session"}) :session))

(def ^:private a-fine
  "An act that the check of today takes. Its entry says refused."
  (thing :streaming :alice 23 {:e :note-6 :k :text :v "fine"}))

(def ^:private a-not-yet
  "An act on the place of `:note-1` that has no entry: it has no answer yet."
  (thing :streaming :alice 24 {:e :note-1 :k :text :v "no answer yet"}))

(def ^:private a-stray
  "A stray: a map with no act-id."
  {:hello "there"})

(def ^:private alone-acts
  "The log of acts of the layer `:alice`. Partition 0 holds a stray, twice.
  Partition 1 holds the acts, not in the order of their stamps: the act
  that replaces a fact is before the act that wrote it, the repeat of an
  act is there a second time, and a second and different thing stands
  under its act-id, before it. The last act has no entry in the log of
  answers: it has no answer yet."
  [[a-stray a-stray]
   [a-replaced a-other a-marks a-first a-refused a-first a-to-go a-retract
    a-third a-mark-again a-mark-off a-two a-no-session a-fine a-not-yet]])

(def ^:private alone-answers
  "The log of answers of the layer `:alice`. On partition 0 the stray's
  entry, twice. On partition 1 the entries, not in the order of their
  stamps, with three copies of the entry of the repeated act; an entry of
  an act that no log holds; two entries under one act-id that differ; an
  accept of a thing that the check of today refuses; and a refusal of a
  thing that the check of today takes."
  (let [stray {:stray :no-act-id :digest (hex (digest-of a-stray)) :task 0}]
    [[{:strays [stray]} {:strays [stray]}]
     [{:answers [(entry a-third 180)]}
      {:answers [(entry a-marks 100)]}
      {:answers [(entry a-first 110)]}
      {:answers [(entry a-refused 120 :permission-does-not-stand)]}
      {:answers [(entry a-first 110)]}
      {:answers [(entry a-replaced 130)]}
      {:answers [(entry a-to-go 140) (entry a-first 110)]}
      {:answers [(entry a-retract 150)]}
      {:answers [(entry a-mark-off 160)]}
      {:answers [(entry a-mark-again 170)]}
      {:answers [(entry a-not-in-log 190)]}
      {:answers [(entry a-two 200)]}
      {:answers [(entry a-two 201)]}
      {:answers [(entry a-no-session 210)]}
      {:answers [(entry a-fine 220 :a-word-of-another-day)]}]]))

;; ---------------------------------------------- the layer of the batches

(def ^:private b-marks
  "The act that marks the key `:kind` in `:base`, in batch 7."
  (thing :microbatch :base 31 {:e :base :k :softland.store/marked-keys :v #{:kind}}))

(def ^:private b-in-batch
  "A fact under `:kind` in the batch that marks it."
  (thing :microbatch :base 32 {:e :thing-1 :k :kind :v "x"}))

(def ^:private b-first
  "The first of two acts of batch 7 on one place, under a key with no mark."
  (thing :microbatch :base 33 {:e :thing-2 :k :name :v "first"}))

(def ^:private b-second
  "The second of two acts of batch 7 on one place."
  (thing :microbatch :base 34 {:e :thing-2 :k :name :v "second"}))

(def ^:private b-later
  "A fact under `:kind` in batch 8, the batch after the mark."
  (thing :microbatch :base 35 {:e :thing-1 :k :kind :v "y"}))

(def ^:private b-p
  "The first of two acts of batch 8 on one place under the marked key."
  (thing :microbatch :base 36 {:e :thing-3 :k :kind :v "p"}))

(def ^:private b-q
  "The second of two acts of batch 8 on one place under the marked key."
  (thing :microbatch :base 37 {:e :thing-3 :k :kind :v "q"}))

(def ^:private b-mark-off
  "The act that takes the mark off `:kind`, in batch 9."
  (thing :microbatch :base 38 {:e :base :k :softland.store/marked-keys :v #{}}))

(def ^:private b-z
  "A fact under `:kind` in the batch that takes the mark off, with a later
  stamp than the act that takes it off."
  (thing :microbatch :base 39 {:e :thing-4 :k :kind :v "z"}))

(def ^:private b-stray
  "A stray of the base: an act whose act-id names the other gate's depot."
  (thing :streaming :alice 40 {:e :note-1 :k :text :v "on the wrong depot"}))

(def ^:private b-not-yet
  "An act of the base on the place of `b-second` that has no entry: it has
  no answer yet."
  (thing :microbatch :base 41 {:e :thing-2 :k :name :v "no answer yet"}))

(def ^:private batch-acts
  "The log of acts of the layer `:base`, on two partitions, in no order of
  the stamps. One act, `b-not-yet`, has no entry in the log of answers: it
  has no answer yet."
  [[b-q b-in-batch b-second b-marks b-stray]
   [b-z b-first b-later b-p b-mark-off b-not-yet]])

(def ^:private batch-answers
  "The log of answers of the gate that decides in batches. Three batches.
  Batch 7: a mark of `:kind`, a fact under `:kind`, and two acts on one
  place. Batch 8: a fact under `:kind`, and two acts on one place under
  `:kind`. Batch 9: the mark taken off, and after it a fact under `:kind`.
  The first record of a batch holds its clock and its last stamp. The first
  record of batch 7 is there twice, an equal copy, and a stray has an
  entry."
  (let [first-of-7 {:batch 7 :clock 5000 :last-stamp 1007
                    :answers [(entry b-marks 1000) (entry b-in-batch 1001)]}]
    [[first-of-7
      {:batch 8 :clock 6000 :last-stamp 2005
       :answers [(entry b-q 2002) (entry b-later 2000)]
       :strays [{:stray :act-id-of-the-other-gate :digest (hex (digest-of b-stray)) :task 0}]}
      {:batch 9 :answers [(entry b-z 3001)]}]
     [{:batch 7 :answers [(entry b-second 1003) (entry b-first 1002)]}
      first-of-7
      {:batch 8 :answers [(entry b-p 2001)]}
      {:batch 9 :clock 7000 :last-stamp 3003 :answers [(entry b-mark-off 3000)]}]]))

(def ^:private clocks
  "The log of the clocks of batches. Batch 8 has two records, as a batch has
  whose second attempt found no clock kept."
  [[]
   [{:batch 7 :clock 5000} {:batch 8 :clock 6000} {:batch 8 :clock 6001} {:batch 9 :clock 7000}]])

(def ^:private logs
  "What the proof hands the rebuild."
  {:tasks tasks
   :task-of-route task-of
   :acts [{:gate :streaming :log alone-acts}
          {:gate :microbatch :log batch-acts}]
   :answers {:streaming alone-answers :microbatch batch-answers}
   :batch-clocks clocks})

(defn- made
  "What the rebuild makes of some logs, with each run of bytes as hex text."
  [logs]
  (rebuild/bytes-as-hex (rebuild/from-logs logs)))

;; ------------------------------------------------------------- the checks

(deftest an-act-of-a-gate-that-decides-alone
  (let [result (made logs)
        tables (get-in result [:tables :streaming])]
    (is (= {0 {}
            1 {(answer-key :alice 1) (answer a-marks 100)
               (answer-key :alice 2) (answer a-first 110)
               (answer-key :alice 3) (answer a-refused 120 :permission-does-not-stand)
               (answer-key :alice 4) (answer a-replaced 130)
               (answer-key :alice 5) (answer a-to-go 140)
               (answer-key :alice 6) (answer a-retract 150)
               (answer-key :alice 7) (answer a-mark-off 160)
               (answer-key :alice 8) (answer a-mark-again 170)
               (answer-key :alice 9) (answer a-third 180)
               (answer-key :alice 22) (answer a-no-session 210)
               (answer-key :alice 23) (answer a-fine 220 :a-word-of-another-day)}}
           (:answers tables)))
    (shown "the answers of a gate that decides alone: an accepted act and a refused one each have the row of their entry, on the task of the entry's partition:"
           (pr-str (select-keys (get-in tables [:answers 1]) [(answer-key :alice 2) (answer-key :alice 3)])))
    (is (= {0 {}
            1 {":alice :alice :softland.store/marked-keys"
               (assoc (row :alice 8 170 "#{:text}") :level-since "{:text 170}")
               ":alice :note-1 :text" (row :alice 9 180 "\"a third text\"")
               ":alice :note-3 :text" (row :alice 6 150)}}
           (:facts tables)))
    (shown "the facts: the acts were applied in the order of their stamps and not of the log, where the replacing act stands before the act that it replaces:"
           (pr-str (get-in tables [:facts 1 ":alice :note-1 :text"])))
    (shown "a refused act has no fact:" (pr-str (get-in tables [:facts 1 ":alice :note-2 :text"]))
           "| a retract is a row with no value:" (pr-str (get-in tables [:facts 1 ":alice :note-3 :text"])))
    (shown "a repeat with three copies of its entry is one answer, and the second and different thing under its act-id gives no row:"
           (pr-str (get-in tables [:facts 1 ":alice :note-9 :text"])))
    (is (= {0 {}
            1 {(str ":alice :text 100 " (value-digest "\"a new text\"") " :note-1") (row :alice 4 130)
               (str ":alice :text 170 " (value-digest "\"a third text\"") " :note-1") (row :alice 9 180)}}
           (:facts-by-value tables)))
    (shown "the facts by value: the replaced value's row went out, the retracted value's row went out, and after the mark was taken off at 160 and put on again at 170 the row of the earlier marking stays beside the new one:"
           (pr-str (sort (keys (get-in tables [:facts-by-value 1])))))
    (is (= {0 {:strays {:no-act-id #{(hex (digest-of a-stray))}}}
            1 {:last-stamp 220}}
           (:task tables)))
    (shown "what the gate keeps for a task: a stray that arrived twice is one member of its word's set on its task, and the last stamp is the greatest stamp of the partition's entries:"
           (pr-str (:task tables)))))

(deftest an-act-of-a-gate-that-decides-in-batches
  (let [result (made logs)
        tables (get-in result [:tables :microbatch])
        marks-row (assoc (row :base 38 3000 "#{}") :level-since "{}")
        by-value (fn [since value-text entity] (str ":base :kind " since " " (value-digest value-text) " " entity))
        on-its-task (fn [rows] (reduce (fn [placed [k r]]
                                         (assoc-in placed [(task-of (nth (.split ^String k " ") 3)) k] r))
                                       {0 {} 1 {}}
                                       rows))]
    (is (= {0 {(answer-key :base 32) (answer b-in-batch 1001)
               (answer-key :base 34) (answer b-second 1003)
               (answer-key :base 36) (answer b-p 2001)
               (answer-key :base 38) (answer b-mark-off 3000)}
            1 {(answer-key :base 31) (answer b-marks 1000)
               (answer-key :base 33) (answer b-first 1002)
               (answer-key :base 35) (answer b-later 2000)
               (answer-key :base 37) (answer b-q 2002)
               (answer-key :base 39) (answer b-z 3001)}}
           (:answers tables)))
    (shown "the answers of a gate that decides in batches are on the task of each act-id's UUID, whatever the partition of the record:"
           (pr-str (into {} (map (fn [[task rows]] [task (count rows)])) (:answers tables))))
    (is (= {0 {":base :base :softland.store/marked-keys" marks-row
               ":base :thing-1 :kind" (row :base 35 2000 "\"y\"")
               ":base :thing-3 :kind" (row :base 37 2002 "\"q\"")}
            1 {":base :base :softland.store/marked-keys" marks-row
               ":base :thing-2 :name" (row :base 34 1003 "\"second\"")
               ":base :thing-4 :kind" (row :base 39 3001 "\"z\"")}}
           (:facts tables)))
    (shown "the facts: the marks row is on every task; of two acts of one batch on one place the row is the later stamp's:"
           (pr-str (get-in tables [:facts 1 ":base :thing-2 :name"])))
    (is (= (on-its-task {(by-value 1007 "\"y\"" :thing-1) (row :base 35 2000)
                         (by-value 1007 "\"q\"" :thing-3) (row :base 37 2002)
                         (by-value 1007 "\"z\"" :thing-4) (row :base 39 3001)})
           (:facts-by-value tables)))
    (shown "the facts by value: the key's stamp is its batch's last stamp, 1007; the fact of the mark's own batch (\"x\", stamp 1001) has no row; of two acts of one batch on one place under the key the first value (\"p\") has none; a fact after a mark was taken off in its own batch (\"z\") is decided with the marks as the batch began:"
           (pr-str (sort (map (fn [k] (vec (take 3 (.split ^String k " ")))) (mapcat keys (vals (:facts-by-value tables)))))))
    (is (not (contains? (into #{} (mapcat keys) (vals (:facts-by-value tables)))
                        (by-value 1007 "\"x\"" :thing-1))))
    (is (= {0 {:closed-through 3003
               :strays {:act-id-of-the-other-gate #{(hex (digest-of b-stray))}}}
            1 {:closed-through 3003 :last-stamp 3003}}
           (:task tables)))
    (shown "what the gate keeps for a task: the last stamp of the newest batch on the gate's task, the same number as :closed-through on every task, and a stray on the task of its entry:"
           (pr-str (:task tables)))
    (is (= {0 {} 1 {:microbatch {:batch 9 :clock 7000}}} (:batch-clock result)))
    (shown "the kept clock of a batch, from the gate's own log, on the gate's task:" (pr-str (:batch-clock result)))
    (is (= [[:task :turns] [:task :to-keep] [:task :to-close]] (mapv (juxt :table :part) (:left-out result))))
    (is (every? (fn [part] (seq (:why part))) (:left-out result)))
    (is (not-any? (fn [row] (some #{:turns :to-keep :to-close} (keys row))) (vals (:task tables))))
    (shown "left out, each named with its reason, and in no row that was made:"
           (pr-str (mapv (juxt :part :why) (:left-out result))))))

(deftest what-the-rebuild-names
  (let [result (made logs)
        named (:named result)]
    (is (= #{{:why :entry-with-no-act :gate :streaming :act-id [:alice :streaming (uuid 20)]
              :stamp 190 :digest (hex (digest-of a-not-in-log))}
             {:why :entries-differ :gate :streaming :act-id [:alice :streaming (uuid 21)]
              :entries #{{:stamp 200 :digest (hex (digest-of a-two))}
                         {:stamp 201 :digest (hex (digest-of a-two))}}}
             {:why :form-cannot-be-given :word :no-session :gate :streaming
              :act-id [:alice :streaming (uuid 22)] :stamp 210 :digest (hex (digest-of a-no-session))}}
           named))
    (shown "named, and not passed over: an entry with no act, two entries under one act-id that differ, an accepted act whose form the check of today cannot give:"
           (pr-str (sort (map (juxt :why (comp last :act-id)) named))))
    (is (nil? (get-in result [:tables :streaming :answers 1 (answer-key :alice 20)])))
    (is (nil? (get-in result [:tables :streaming :answers 1 (answer-key :alice 21)])))
    (is (nil? (get-in result [:tables :streaming :facts 1 ":alice :note-4 :text"])))
    (is (nil? (get-in result [:tables :streaming :facts 1 ":alice :note-7 :text"])))
    (shown "no row is made from an answer alone, and none from entries that differ")
    (is (nil? (get-in result [:tables :streaming :answers 1 (answer-key :alice 24)])))
    (is (= "\"a third text\"" (get-in result [:tables :streaming :facts 1 ":alice :note-1 :text" :value])))
    (is (nil? (get-in result [:tables :microbatch :answers (task-of (uuid 41)) (answer-key :base 41)])))
    (is (= "\"second\"" (get-in result [:tables :microbatch :facts 1 ":base :thing-2 :name" :value])))
    (is (not-any? (fn [a-named] (contains? #{(uuid 24) (uuid 41)} (last (:act-id a-named)))) named))
    (shown "an act that is in a log of acts and has no entry, in each layer, has no answer and wrote no fact: its place holds the value of the last act that has an entry; and it is not named:"
           (pr-str [(get-in result [:tables :streaming :facts 1 ":alice :note-1 :text" :value])
                    (get-in result [:tables :microbatch :facts 1 ":base :thing-2 :name" :value])]))))

(deftest what-the-rebuild-names-of-a-batch
  (let [clock-in-no-log (made (assoc logs :batch-clocks [[] []]))
        two-clocks (made (update-in logs [:answers :microbatch 0] conj {:batch 9 :clock 7001 :last-stamp 3003}))
        tables (get-in two-clocks [:tables :microbatch])
        no-last-stamp (into #{} (filter (fn [a-named] (= :no-last-stamp-of-its-batch (:why a-named)))) (:named two-clocks))]
    (is (contains? (:named clock-in-no-log) {:why :clock-not-in-the-log-of-clocks :gate :microbatch :batch 9 :clock 7000}))
    (is (= {0 {} 1 {:microbatch {:batch 9 :clock 7000}}} (:batch-clock clock-in-no-log)))
    (shown "a kept clock that no record of the log of clocks holds is made from the gate's own log all the same, and named:"
           (pr-str (:batch-clock clock-in-no-log)))
    (is (contains? (:named two-clocks) {:why :records-of-a-batch-differ :gate :microbatch :batch 9
                                        :clocks #{7000 7001} :last-stamps #{3003}}))
    (is (= #{{:why :no-last-stamp-of-its-batch :gate :microbatch :batch 9 :act-id (:act-id b-mark-off)
              :stamp 3000 :digest (hex (digest-of b-mark-off))}
             {:why :no-last-stamp-of-its-batch :gate :microbatch :batch 9 :act-id (:act-id b-z)
              :stamp 3001 :digest (hex (digest-of b-z))}}
           no-last-stamp))
    (is (= (answer b-z 3001) (get-in tables [:answers 1 (answer-key :base 39)])))
    (is (nil? (get-in tables [:facts 1 ":base :thing-4 :kind"])))
    (shown "a batch whose records hold two clocks is named and gives neither its clock nor its last stamp; each accepted act of it is named, has its answer's row as the log holds it, and no fact row:"
           (pr-str (sort (map (juxt :why (comp last :act-id)) no-last-stamp))))))

(deftest the-answer-of-the-log-is-applied-and-not-the-gate-of-today
  (let [result (made logs)
        tables (get-in result [:tables :streaming])]
    (is (= {:refuse :no-session} (declaration/check a-no-session)))
    (is (= (answer a-no-session 210) (get-in tables [:answers 1 (answer-key :alice 22)])))
    (is (nil? (get-in tables [:facts 1 ":alice :note-5 :text"])))
    (shown "the check of today refuses a thing with" (pr-str (declaration/check a-no-session))
           "| the log says accepted | the answer made again is accepted:"
           (pr-str (dissoc (get-in tables [:answers 1 (answer-key :alice 22)]) :digest))
           "| no fact row, and the act is named")
    (is (contains? (declaration/check a-fine) :ok))
    (is (= (answer a-fine 220 :a-word-of-another-day) (get-in tables [:answers 1 (answer-key :alice 23)])))
    (is (nil? (get-in tables [:facts 1 ":alice :note-6 :text"])))
    (shown "the check of today takes a thing | the log says refused with a word that no check of today gives | the answer made again:"
           (pr-str (dissoc (get-in tables [:answers 1 (answer-key :alice 23)]) :digest))
           "| its fact:" (pr-str (get-in tables [:facts 1 ":alice :note-6 :text"])))))

(deftest copies-and-the-order-of-a-log-change-nothing
  (let [once (made logs)
        each-record (fn [f log] (mapv (fn [partition] (vec (f partition))) log))
        thrice (-> logs
                   (update-in [:answers :streaming] (partial each-record (fn [records] (mapcat (fn [r] [r r r]) records))))
                   (update-in [:answers :microbatch] (partial each-record (fn [records] (mapcat (fn [r] [r r r]) records)))))
        turned (-> logs
                   (update-in [:answers :streaming] (partial each-record reverse))
                   (update-in [:answers :microbatch] (partial each-record reverse))
                   (update :acts (fn [acts] (mapv (fn [log] (update log :log (partial each-record reverse))) acts)))
                   (update :batch-clocks (partial each-record reverse)))]
    (is (= once (made thrice)))
    (shown "with three copies of every record of both logs of answers the rebuild gives the same tables and names the same things:"
           (= once (made thrice)))
    (is (= once (made turned)))
    (shown "with every partition of every log turned back to front it gives the same:" (= once (made turned)))))

(defn test-ns-hook
  "Runs the checks in the order in which the proof tells them."
  []
  (an-act-of-a-gate-that-decides-alone)
  (an-act-of-a-gate-that-decides-in-batches)
  (what-the-rebuild-names)
  (what-the-rebuild-names-of-a-batch)
  (the-answer-of-the-log-is-applied-and-not-the-gate-of-today)
  (copies-and-the-order-of-a-log-change-nothing))

(defn -main
  "Runs the proof and exits: status 0 when each check holds. It takes no
  argument."
  [& _]
  (let [{:keys [fail error] :as result} (run-tests 'softland.store.rebuild-pure-proof)]
    (println)
    (println "THE PROOF, IN ONE PLACE")
    (run! println @shown-lines)
    (println (select-keys result [:test :pass :fail :error]))
    (shutdown-agents)
    (System/exit (if (zero? (+ fail error)) 0 1))))
