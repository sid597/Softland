(ns softland.store.rebuild.reader
  "The reader of the rebuild: it takes the logs off a running module, in the
  form that `softland.store.rebuild/from-logs` takes. It requires Rama's
  client. It decides nothing and makes no row.

  What it takes and gives. `logs` takes a cluster handle of Rama's and the
  name that Rama gives the module, and gives {:tasks :acts :answers
  :batch-clocks}: the number of tasks; the logs of acts as a list, each
  {:gate :log}; a map from each gate's word to its log of answers; and the
  log of the clocks of batches. Each log is a vector of partitions in the
  order of their numbers, each a vector of the appended values in the order
  of their offsets. `:task-of-route` is not in it:
  `softland.store.rebuild.placing` gives that.

  Which depots. `depots` names them as data: a list of the depots of acts,
  each with the word of the gate that decides it; a list of the depots of
  answers, each with its gate's word; and the depot of batch clocks. No
  function here knows how many depots of acts there are, so a later one is
  one entry more.

  How it reads. A client reads a depot partition by partition: the number
  of partitions, the start and the end offset of a partition, and a range,
  which gives the values alone, with no offset and no time (run for the
  plan of the logs of answers: its stream probe, question 5). One read is
  one event on the task's thread, so each partition is read in ranges of a
  bounded number of records, which `depots` says for each depot. A range
  past the end throws for the whole read, so the end is asked for first and
  no read goes past it.

  The order of the reads: the logs of answers, then the logs of acts, then
  the log of clocks. A gate appends an entry after the thing that it
  answers is in its log of acts, and a batch's entries after its clock is
  in the log of clocks. So in this order each entry that was read has its
  thing and its clock among what was read, also on a store that is not at
  rest.

  State and effects. It reads the cluster and changes nothing. It keeps
  nothing after the call. Each log is whole in memory when it returns;
  `softland.store.rebuild` says what a large store needs instead.

  Limits. The offsets of a partition are read once, right before its
  ranges: on a store that is not at rest the partitions are cut at
  different moments. A depot that was trimmed is read from where it starts
  now. A call that throws is not caught.

  Callers. The rebuild's proof."
  (:require [com.rpl.rama :as rama]))

(def depots
  "The depots that hold the store's logs, by the names under which the
  module declares them, each with `:at-once`, the most records that one
  read of a partition asks for.

    :acts          each depot of acts, with the word of the gate that
                   decides its things. An act is small as a rule and can be
                   as large as the declaration's bound on an act lets it be
    :answers       each gate's depot of answers. A record of the streaming
                   gate is one entry, about a hundred bytes. A record of
                   the microbatch gate holds up to 256 entries and can be
                   near 30 KB, so few are read at once
    :batch-clocks  the depot of the clocks of batches, two numbers a record

  The numbers are this build's, kept, and not measured on a store with
  data: the probe read 50,000 small records in one read in 84 to 100 ms,
  and held the task's thread for that long."
  {:acts [{:gate :streaming :depot "*streaming-depot" :at-once 64}
          {:gate :microbatch :depot "*microbatch-depot" :at-once 64}]
   :answers [{:gate :streaming :depot "*streaming-answer-depot" :at-once 1024}
             {:gate :microbatch :depot "*microbatch-answer-depot" :at-once 4}]
   :batch-clocks {:depot "*batch-clock-depot" :at-once 1024}})

(defn- partition-read
  "Reads one partition of a depot from its start to its end as they stand
  when it is called. Takes the depot's handle, the number of the partition,
  and the most records for one read. Gives the appended values in the order
  of their offsets, as a vector. One call for the two offsets, then one
  read for each range."
  [depot partition at-once]
  (let [{:keys [start-offset end-offset]} (rama/foreign-depot-partition-info depot partition)]
    (loop [from start-offset
           records []]
      (if (>= from end-offset)
        records
        (let [to (min end-offset (+ from at-once))]
          (recur to (into records (rama/foreign-depot-read depot partition from to))))))))

(defn- log-read
  "Reads one depot whole. Takes a cluster handle, the module's name and one
  entry of `depots`, {:depot :at-once}. Gives the log: a vector with one
  vector of records for each partition, in the order of the partitions'
  numbers."
  [cluster module-name {:keys [depot at-once]}]
  (let [handle (rama/foreign-depot cluster module-name depot)]
    (mapv (fn [partition] (partition-read handle partition at-once))
          (range (:num-partitions (rama/foreign-object-info handle))))))

(defn logs
  "Reads every log that a rebuild takes. Takes a cluster handle of Rama's
  and the name that Rama gives the module; and, when the caller names other
  depots or other bounds, a map in the form of `depots`. Gives
  {:tasks :acts :answers :batch-clocks}, as the namespace docstring says.
  `:tasks` is the number of partitions of the depot of batch clocks: every
  depot of a module has the module's number of tasks. The logs of answers
  are read first, then the logs of acts, then the log of clocks."
  ([cluster module-name] (logs cluster module-name depots))
  ([cluster module-name {:keys [acts answers batch-clocks]}]
   (let [answers-read (into {}
                            (map (fn [described]
                                   [(:gate described) (log-read cluster module-name described)]))
                            answers)
         acts-read (mapv (fn [described]
                           {:gate (:gate described) :log (log-read cluster module-name described)})
                         acts)
         clocks-read (log-read cluster module-name batch-clocks)]
     {:tasks (count clocks-read)
      :acts acts-read
      :answers answers-read
      :batch-clocks clocks-read})))
