(ns softland.store.rebuild
  "The rebuild's pure core: every row of every table of the store, made again
  from the logs alone, as plain data. Pure code. It needs no cluster and
  loads with Clojure alone.

  Why it is here. Sid decided on 12 October 2026 that every table can be
  made again from the logs (src-store/README.md, the decisions of that day,
  entry 1). Each gate appends what it decided to a log of answers, and this
  namespace applies those answers to the acts. It decides nothing: it runs
  no check of a gate, no test for a stray, and reads no clock. A gate whose
  rules changed after an act was decided gives that act no new answer, so
  the answer that the log holds is the one applied, also where the gate of
  today would give another. Giving a thing its one form is not deciding;
  refusing it is.

  What it takes. `from-logs` takes one map:

    :tasks          the module's number of tasks. No log holds it
    :task-of-route  a function from a route to its task: the module's hash
                    partitioner. No log holds the placing of a row of a
                    batch, so the caller hands it. It is called only for a
                    decision of a batch
    :acts           a list of logs of acts, each {:gate :log}: the word of
                    the gate that decided it, and the log. A gate can have
                    more than one
    :answers        a map from a gate's word to its log of answers
    :batch-clocks   the log of the clocks of batches

  Each log is as a client's read of a depot gives it: for each partition a
  vector of the appended values in their order, with no offset and no time;
  the partitions in the order of their numbers. A record of a log of answers
  is a map: `:answers`, entries {:act-id :stamp :digest} with `:refused` and
  the word for a refusal, the digest as 32 bytes; `:strays`, entries
  {:stray :digest :task}, the digest as hex text; and for a decision of a
  batch `:batch`, the batch's id, with `:clock` and `:last-stamp` in the
  batch's first record. A record of the log of clocks is {:batch :clock}.

  What it gives. A map:

    :tables       gate -> table -> task -> the rows of that table on that
                  task. The tables `:answers`, `:facts` and
                  `:facts-by-value` are maps from a row's key to the row;
                  `:task` is the one row that a gate keeps for a task. Every
                  task is there, with an empty map where it holds nothing
    :batch-clock  task -> gate's word -> the kept clock of a batch,
                  {:batch :clock}
    :named        a set of what the rebuild names and does not pass over in
                  silence, each a map with `:why` (`from-logs` lists the
                  words)
    :left-out     `left-out`: the parts that no log gives, by ruling

  A row is what the gate's table holds: an answer's `:digest` is the bytes
  of the entry. `bytes-as-hex` gives a value in a form that `=` compares.

  The two kinds of decision. Which kind a decision is comes from its record
  and never from a gate's word, so a later depot of acts needs no new
  branch here.

    made in an event of its own   its record holds no `:batch`. Its rows are
        on the task of the partition where its entry is: the task that
        decided. An act-id has one answer on a task. The thing is on that
        partition of a log of acts. It is decided with the marks as they
        were before it, and a key that it newly marks gets its own stamp
    of a batch   its record holds `:batch`. Its rows are placed by route, as
        `softland.store.gate.batch/routed` says, and a row under one of the
        keys that every task holds is on every task. An act-id has one
        answer in the gate. The thing is on any partition. It is decided
        with the marks as they were when its batch began, whatever the
        stamps of a mark and a fact of one batch are, and a key that it
        newly marks gets the batch's `:last-stamp`

  So in both the order of a marked key holds every fact under that key
  whose stamp is later than the key's stamp, and no other; the rows of an
  earlier marking stay.

  Approach, for one gate.

    1. One walk of its log of answers (`read-answers`): the entries, the
       strays, and what each batch keeps of itself.
    2. One entry is taken where several are equal (`taken-entries`). The
       logs hold equal copies of an entry, so the result does not depend on
       how many. Entries under one act-id that differ are named, and no row
       is made of them.
    3. One walk of its logs of acts (`tied`) ties each taken entry to its
       thing by two equalities: the act-id, and the digest of the thing. A
       digest is taken only of a thing under an act-id that has an entry.
       An entry with no thing is named, and no row is made from an answer
       alone.
    4. The decisions are applied in the order of their stamps, never of a
       depot (`applied`). The answer's row is the entry's. For an accepted
       act the thing gets its one form from
       `softland.store.door.declaration/check`, and its rows are those of
       `softland.store.gate.decision/writes`, handed what a gate hands it,
       read from the rows that this walk has made so far. So the text of a
       row is the gate's.
    5. What a gate keeps for a task (`task-rows`) and the kept clock of a
       batch (`kept-clock`) are made from the entries, the strays and the
       batches.

  State and effects. None. It reads no table: what it knows of a row before
  is what it has itself made. It owns nothing after the call.

  What it holds in memory, and what a large store needs instead. The caller
  hands each log whole, so every act is in memory at once. Beside the logs
  the core holds: every entry of a log of answers, one index from an act-id
  to its entry, one from a decided act-id to its thing, a sorted list of
  the decisions, and every row that it makes. Each log is walked once. A
  rebuild of a large store cannot do that. It would read each partition in
  ranges, keep the two indexes and the rows in a store on disk and not in a
  map, and merge the partitions' entries by stamp where this core sorts
  them; for a gate whose every decision is made in an event of its own it
  could run one task at a time, since no row of one task is read for
  another. None of that is built.

  A store that is not at rest. A thing with no entry gives no row, as the
  tables hold none for it. `:closed-through` is made as the gate's last
  stamp, which a store at rest holds and a running one reaches one run
  later. What this gives of a store that is not at rest is what falls out;
  no proof holds it equal to the tables.

  Beside it. [rebuild/](rebuild/README.md) holds what needs Rama: the
  reader that takes the logs off a running module, and what gives the task
  of a route. `routes-asked` says which routes a caller must ask for.

  Callers. The rebuild's proofs under `test-store/`. No gate, no door and no
  read calls it."
  (:require [clojure.walk :as walk]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.door.text :as text]
            [softland.store.gate.batch :as batch]
            [softland.store.gate.decision :as decision]
            [softland.store.record.rows :as rows]))

(def left-out
  "The parts of what the microbatch gate keeps for a task that `from-logs`
  does not make, each with its reason. A comparison of what a rebuild made
  with the tables leaves these parts out."
  [{:table :task :part :turns
    :why "The working state of one run of the gate. No log holds it, and nothing reads it once its batch is committed."}
   {:table :task :part :to-keep
    :why "The entries that a batch has still to append. On a store at rest it holds no key."}
   {:table :task :part :to-close
    :why "What the next run has to close. On a store at rest it is absent."}])

(defn bytes-as-hex
  "Takes plain data. Gives it with each run of bytes as its hex text, so
  that `=` compares two of them: two byte arrays are equal to `=` only when
  they are one object. A proof calls it on what `from-logs` gave and on
  what it read from the tables."
  [x]
  (walk/postwalk (fn [part] (if (bytes? part) (text/hex part) part)) x))

;; ------------------------------------------------- the log of answers, read

(defn- read-answers
  "One walk of one gate's log of answers. Takes the log, a vector of
  partitions, each a vector of records. Gives:

    :entries  each answer's entry, in the order of the walk, with `:hex`,
              its digest as hex text; `:partition`, the number of the
              partition where its record is; and `:batch`, the id that its
              record holds, or nil
    :strays   each stray's entry, as it is
    :batches  batch id -> {:clocks :last-stamps}: the set of each that the
              records of that batch hold

  What must hold: each record has the form that a gate appends."
  [log]
  (reduce
   (fn [read [partition record]]
     (let [batch (:batch record)]
       (cond-> (-> read
                   (update :entries into
                           (map (fn [entry]
                                  {:act-id (:act-id entry) :refused (:refused entry)
                                   :stamp (:stamp entry) :digest (:digest entry)
                                   :hex (text/hex (:digest entry))
                                   :partition partition :batch batch}))
                           (:answers record))
                   (update :strays into (:strays record)))
         (and (some? batch) (some? (:clock record)))
         (update-in [:batches batch :clocks] (fnil conj #{}) (:clock record))

         (and (some? batch) (some? (:last-stamp record)))
         (update-in [:batches batch :last-stamps] (fnil conj #{}) (:last-stamp record)))))
   {:entries [] :strays [] :batches {}}
   (for [[partition records] (map-indexed vector log)
         record records]
     [partition record])))

(defn- batch-facts
  "What each batch keeps of itself. Takes a gate's word and `:batches` of
  `read-answers`. Gives {:batches :named}: batch id -> {:clock :last-stamp},
  each only when a record of the batch holds it; and, for a batch whose
  records hold two clocks or two last stamps, a named thing in its place.
  Every record of one batch that holds the two holds the same two, so two
  that differ are a fault of the store and neither is picked."
  [gate batches]
  (reduce-kv
   (fn [got id {:keys [clocks last-stamps]}]
     (if (or (< 1 (count clocks)) (< 1 (count last-stamps)))
       (update got :named conj {:why :records-of-a-batch-differ :gate gate :batch id
                                :clocks clocks :last-stamps last-stamps})
       (assoc-in got [:batches id]
                 (cond-> {}
                   (seq clocks) (assoc :clock (first clocks))
                   (seq last-stamps) (assoc :last-stamp (first last-stamps))))))
   {:batches {} :named []}
   batches))

(defn- newest-with
  "Takes the batches of `batch-facts` and one of `:clock` and `:last-stamp`.
  Gives [id value] of the batch with the greatest id that holds that part,
  or nil when none does."
  [batches part]
  (when-let [ids (seq (keep (fn [[id kept]] (when (some? (get kept part)) id)) batches))]
    (let [id (reduce max ids)]
      [id (get-in batches [id part])])))

;; ---------------------------------------------------- one entry for an act

(defn- seat
  "Takes an entry of `read-answers`. Gives the part of the answers in which
  its act-id has one answer: the number of its partition for a decision
  made in an event of its own, whose answer is on the task that decided;
  the word `:gate` for a decision of a batch, whose answer has one home in
  the whole gate."
  [entry]
  (if (nil? (:batch entry)) (:partition entry) :gate))

(defn- named-entry
  "Takes a word, a gate's word and an entry. Gives the named thing for that
  entry: the word under `:why`, the gate, the act-id, the stamp, the digest
  as hex text, and the word of the refusal when there is one."
  [why gate entry]
  (cond-> {:why why :gate gate :act-id (:act-id entry) :stamp (:stamp entry) :digest (:hex entry)}
    (:refused entry) (assoc :refused (:refused entry))))

(defn- taken-entries
  "The rule for copies. Takes a gate's word and the entries of
  `read-answers`. Gives {:taken :named}. `:taken` is a map from
  [seat act-id] to one entry, for each act-id of a seat whose entries all
  hold one word, one stamp and one digest: the logs hold equal copies of an
  entry, and one is as good as another. Where the copies name more than one
  batch the least id is kept. `:named` holds one thing for each act-id of a
  seat whose entries differ, with the answers that differ as a set: a gate
  cannot make that, so it is named and none is picked."
  [gate entries]
  (reduce-kv
   (fn [got [_ act-id :as at] copies]
     (let [answers (into #{} (map (juxt :refused :stamp :hex)) copies)]
       (if (= 1 (count answers))
         (assoc-in got [:taken at]
                   (let [batches (keep :batch copies)]
                     (cond-> (first copies)
                       (seq batches) (assoc :batch (reduce min batches)))))
         (update got :named conj
                 {:why :entries-differ :gate gate :act-id act-id
                  :entries (into #{}
                                 (map (fn [[refused stamp hex]]
                                        (cond-> {:stamp stamp :digest hex}
                                          refused (assoc :refused refused))))
                                 answers)}))))
   {:taken {} :named []}
   (group-by (juxt seat :act-id) entries)))

(defn- tied
  "One walk of one gate's logs of acts. Takes the logs, each a vector of
  partitions, and `:taken` of `taken-entries`. Gives a map from
  [seat act-id] to the thing that the entry there is the answer of: a thing
  under that act-id whose digest is the entry's. For a decision made in an
  event of its own the thing is looked for on the partition of the entry
  alone; for a decision of a batch on every partition. Under one act-id a
  log can hold several things: the one with the entry's digest was decided,
  and each other is a repeat or another thing that got no answer, and gives
  nothing. Cost: one digest for each thing under an act-id that has an
  entry, until its entry is tied."
  [logs taken]
  (reduce
   (fn [things [partition thing]]
     (let [act-id (when (map? thing) (get thing :act-id))
           hex (delay (digest/hex-of thing))]
       (if (nil? act-id)
         things
         (reduce (fn [things at]
                   (let [entry (get taken at)]
                     (if (and entry (not (contains? things at)) (= (:hex entry) @hex))
                       (assoc things at thing)
                       things)))
                 things
                 [[partition act-id] [:gate act-id]]))))
   {}
   (for [log logs
         [partition things] (map-indexed vector log)
         thing things]
     [partition thing])))

;; ------------------------------------------------- the rows of one decision

(defn- writes-of
  "The rows of one decision, as data. Takes a decision, an entry with its
  `:thing`; the fact rows that the walk holds now where this decision looks
  for a row before, a map from key to row; the same as they were when the
  decision's batch began, or the same map for a decision made in an event
  of its own; and the stamp that a newly marked key gets, or nil when the
  logs do not give it. Gives {:writes :act :named}.

  `:writes` is a vector of [which key row]. The answer is always in it,
  made from the entry with `softland.store.record.rows/answer-key` and
  `answer-row`: the word, the stamp and the digest as the log holds them. A
  refusal writes nothing else. For an accept the thing gets its one form
  from `softland.store.door.declaration/check`, under `:act`, and the fact
  rows and the rows of the facts by value are those of
  `softland.store.gate.decision/writes`, called as a gate calls it: the
  marked keys of the act's places from the marks row as the batch began,
  the row before of each such place and of the marked-keys place as they
  are now.

  `:named` is there, with the answer alone in `:writes`, when no fact row
  can be made: the check of today refuses the thing that the log says was
  accepted, with the word under `:word`; or the act is of a batch whose
  records hold no last stamp."
  [decision rows-now rows-as-batch-began level-from]
  (let [[layer _ act-uuid] (:act-id decision)
        answer [:answer (rows/answer-key layer act-uuid)
                (rows/answer-row (:refused decision) (:stamp decision) (:digest decision))]]
    (if (:refused decision)
      {:writes [answer]}
      (let [{act :ok word :refuse} (declaration/check (:thing decision))]
        (cond
          (nil? act)
          {:writes [answer] :named {:why :form-cannot-be-given :word word}}

          (nil? level-from)
          {:writes [answer] :named {:why :no-last-stamp-of-its-batch :batch (:batch decision)}}

          :else
          (let [marks-place (decision/marked-keys-place act)
                since-by-place (decision/since-by-place act (get rows-as-batch-began marks-place))
                place-rows (into {}
                                 (map (fn [place] [place (get rows-now place)]))
                                 (conj (vec (keys since-by-place)) marks-place))
                gates-writes (decision/writes (:act-id decision) act nil (:stamp decision)
                                              (:digest decision)
                                              {:since-by-place since-by-place
                                               :place-rows place-rows
                                               :level-from level-from})]
            {:act act
             :writes (conj (filterv (fn [[which]] (contains? #{:fact :by-value} which)) gates-writes)
                           answer)}))))))

(def ^:private table-of
  "The table of each kind of row that a decision writes."
  {:answer :answers :fact :facts :by-value :facts-by-value})

(defn- placed
  "Where each row of one decision is. Takes the decision; its act in its one
  form, or nil; its `:writes`; the number of tasks; and the function from a
  route to its task. Gives a vector of {:table :tasks :key :row}. A decision
  made in an event of its own has every row on the task of its entry's
  partition. A decision of a batch has each row where
  `softland.store.gate.batch/routed` says: on the task of its home's route,
  or on every task. A row that is nil says that its key is taken out."
  [decision act writes tasks task-of-route]
  (if (nil? (:batch decision))
    (mapv (fn [[which k row]]
            {:table (table-of which) :tasks [(:partition decision)] :key k :row row})
          writes)
    (mapv (fn [{:keys [which key row home every-task?]}]
            {:table (table-of which)
             :tasks (if every-task? (range tasks) [(task-of-route home)])
             :key key :row row})
          (batch/routed (:act-id decision) act writes))))

(defn- put-row
  "Takes the tables made so far, table -> task -> key -> row, and one row
  of `placed`. Gives the tables with the row written on each of its tasks,
  or with its key taken out there when the row is nil."
  [made {:keys [table tasks key row]}]
  (reduce (fn [made task]
            (if (nil? row)
              (update-in made [table task] dissoc key)
              (assoc-in made [table task key] row)))
          made
          tasks))

(defn- applied
  "One step of the walk. Takes the state of the walk, a gate's word, the
  next decision in the order of the stamps, the batches of `batch-facts`,
  the number of tasks and the function from a route to its task. Gives the
  state after the decision. The state:

    :made            table -> task -> key -> row, the tables so far
    :held            key -> fact row, for the decisions of batches: a place
                     of a batch has one home, so one map serves the gate
    :batch           the id of the batch that is being applied
    :as-batch-began  `:held` as it was when that batch began
    :named           what could not be made, so far

  A decision made in an event of its own looks for a row before on its own
  task, in `:made`. A decision of a batch looks in `:held`, and for the
  marks in `:as-batch-began`. What must hold: the decisions of one batch
  come one after another, which their stamps give."
  [state gate decision batches tasks task-of-route]
  (let [batch (:batch decision)
        own-event? (nil? batch)
        state (if (or own-event? (= batch (:batch state)))
                state
                (assoc state :batch batch :as-batch-began (:held state)))
        rows-now (if own-event?
                   (get-in state [:made :facts (:partition decision)])
                   (:held state))
        rows-as-batch-began (if own-event? rows-now (:as-batch-began state))
        level-from (if own-event?
                     (:stamp decision)
                     (get-in batches [batch :last-stamp]))
        {:keys [act writes named]} (writes-of decision rows-now rows-as-batch-began level-from)]
    (cond-> (reduce (fn [state row]
                      (cond-> (update state :made put-row row)
                        (and (not own-event?) (= :facts (:table row)))
                        (assoc-in [:held (:key row)] (:row row))))
                    state
                    (placed decision act writes tasks task-of-route))
      named (update :named conj (merge (named-entry (:why named) gate decision) named)))))

;; ------------------------------------- what a gate keeps for a task, a clock

(defn- task-rows
  "What a gate keeps for each task. Takes the gate's word, what
  `read-answers` gave, the batches of `batch-facts`, the number of tasks and
  the function from a route to its task. Gives task -> row, a row for every
  task:

    :strays          word -> the set of digests, from each stray's entry
                     with that word and that `:task`. A stray that arrived
                     twice has two entries and is one member
    :last-stamp      for the decisions made in events of their own: on each
                     task the greatest stamp of the answers' entries on its
                     partition, every entry counted. For the decisions of
                     batches: on the gate's task alone, the route of which
                     is the gate's word, the `:last-stamp` of the batch with
                     the greatest id that holds one
    :closed-through  on every task, that same last stamp of the newest
                     batch: what a store at rest holds

  `left-out` names the parts that are not made."
  [gate {:keys [entries strays]} batches tasks task-of-route]
  (let [with-strays (reduce (fn [rows {:keys [stray digest task]}]
                              (update-in rows [task :strays stray] (fnil conj #{}) digest))
                            (zipmap (range tasks) (repeat {}))
                            strays)
        with-own (reduce (fn [rows {:keys [partition stamp]}]
                           (update-in rows [partition :last-stamp] (fnil max stamp) stamp))
                         with-strays
                         (filter (fn [entry] (nil? (:batch entry))) entries))
        [_ last-of-batches] (newest-with batches :last-stamp)]
    (if (nil? last-of-batches)
      with-own
      (reduce (fn [rows task] (assoc-in rows [task :closed-through] last-of-batches))
              (assoc-in with-own [(task-of-route gate) :last-stamp] last-of-batches)
              (range tasks)))))

(defn- kept-clock
  "The kept clock of a batch, made again. Takes the gate's word, the batches
  of `batch-facts`, the log of the clocks of batches and the function from a
  route to its task. Gives nil when no batch of the gate holds a clock. Else
  {:task :row}: the row {:batch :clock} of the batch with the greatest id
  that holds a clock, on the gate's task. The gate's own log says which
  clock was kept; the log of clocks is the check, and when no record of it
  holds that batch with that clock the row is given all the same with
  `:named`, the thing to name. Cost: one walk of the log of clocks."
  [gate batches batch-clocks task-of-route]
  (when-let [[id clock] (newest-with batches :clock)]
    (let [in-the-log? (some (fn [record]
                              (and (map? record) (= id (:batch record)) (= clock (:clock record))))
                            (apply concat batch-clocks))]
      (cond-> {:task (task-of-route gate) :row {:batch (long id) :clock (long clock)}}
        (not in-the-log?)
        (assoc :named {:why :clock-not-in-the-log-of-clocks :gate gate :batch id :clock clock})))))

;; ------------------------------------------------------------ the rebuild

(defn- gate-from-logs
  "The rebuild of one gate. Takes the gate's word, its logs of acts, its log
  of answers, the log of the clocks of batches, the number of tasks and the
  function from a route to its task. Gives {:tables :clock :named}: the
  four tables, each task -> rows; what `kept-clock` gave; and the named
  things. It throws when the log holds a batch and no function was given:
  a row of a batch then has no task."
  [gate acts-logs answers-log batch-clocks tasks task-of-route]
  (let [read (read-answers answers-log)
        _ (when (and (seq (:batches read)) (nil? task-of-route))
            (throw (ex-info "A log of answers holds a batch, and no :task-of-route was given."
                            {:gate gate})))
        {:keys [batches] batches-named :named} (batch-facts gate (:batches read))
        {:keys [taken] entries-named :named} (taken-entries gate (:entries read))
        things (tied acts-logs taken)
        with-no-act (for [[at entry] taken :when (not (contains? things at))]
                      (named-entry :entry-with-no-act gate entry))
        decisions (sort-by (juxt :stamp :partition)
                           (for [[at entry] taken :when (contains? things at)]
                             (assoc entry :thing (get things at))))
        each-task (zipmap (range tasks) (repeat {}))
        walked (reduce (fn [state decision]
                         (applied state gate decision batches tasks task-of-route))
                       {:made {:answers each-task :facts each-task :facts-by-value each-task}
                        :held {} :batch nil :as-batch-began {} :named []}
                       decisions)
        clock (kept-clock gate batches batch-clocks task-of-route)]
    {:tables (assoc (:made walked) :task (task-rows gate read batches tasks task-of-route))
     :clock clock
     :named (into #{} cat [batches-named entries-named with-no-act (:named walked)
                           (when (:named clock) [(:named clock)])])}))

(defn from-logs
  "Makes every row of every table again from the logs. Takes the map that
  the namespace docstring describes: `:tasks`, `:task-of-route`, `:acts`,
  `:answers` and `:batch-clocks`. Gives {:tables :batch-clock :named
  :left-out}, as described there. It reads no table and decides nothing.

  The words of `:named`, each with what was made:

    :entry-with-no-act   no thing of the gate's logs of acts has the
                         entry's act-id and digest. No row
    :entries-differ      two entries under one act-id differ in the word,
                         the stamp or the digest. No row for that act-id
    :form-cannot-be-given  the log says accepted, and the check of today
                         refuses the thing with `:word`. The answer's row,
                         accepted as logged, and no fact row
    :no-last-stamp-of-its-batch  an accepted act of a batch whose records
                         hold no last stamp. The answer's row, and no fact
                         row
    :records-of-a-batch-differ  the records of one batch hold two clocks or
                         two last stamps. The batch then gives neither
    :clock-not-in-the-log-of-clocks  the kept clock that was made is in no
                         record of the log of clocks. The row is made

  What must hold: each record has the form that a gate appends; the
  partitions of each log are in the order of their numbers; and
  `:task-of-route` is given when a log of answers holds a batch, and is the
  placing that decided the acts. Limits: the namespace docstring says what
  is held in memory, and what this gives of a store that is not at rest."
  [{:keys [tasks task-of-route acts answers batch-clocks]}]
  (let [gates (distinct (concat (map :gate acts) (keys answers)))
        by-gate (into {}
                      (map (fn [gate]
                             [gate (gate-from-logs gate
                                                   (keep (fn [log] (when (= gate (:gate log)) (:log log))) acts)
                                                   (get answers gate)
                                                   batch-clocks tasks task-of-route)]))
                      gates)]
    {:tables (into {} (map (fn [[gate made]] [gate (:tables made)])) by-gate)
     :batch-clock (reduce (fn [rows [gate {:keys [clock]}]]
                            (if clock
                              (assoc-in rows [(:task clock) gate] (:row clock))
                              rows))
                          (zipmap (range tasks) (repeat {}))
                          by-gate)
     :named (into #{} (mapcat :named) (vals by-gate))
     :left-out left-out}))

(defn routes-asked
  "The routes whose task `from-logs` asks for. Takes the map that `from-logs`
  takes; a `:task-of-route` in it is not called. Gives the set of the routes
  that `from-logs` hands `:task-of-route` for these logs: the home of each
  row of a batch, and the word of each gate that holds a batch with a clock
  or a last stamp. A caller that gets the task of a route from a cluster
  asks for all of them in few calls, and then hands `from-logs` the map
  that came back.

  It walks the logs as `from-logs` does, with a placing that puts every row
  on task 0 and keeps each route in an atom of this call. The routes do not
  depend on what the placing gives: a decision of a batch finds a row
  before by its key, wherever the row was put. Cost: one walk of the logs
  more, in memory, and no read of a depot."
  [logs]
  (let [asked (atom #{})]
    (from-logs (assoc logs :task-of-route (fn [route] (swap! asked conj route) 0)))
    @asked))
