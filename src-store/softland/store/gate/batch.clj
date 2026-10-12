(ns softland.store.gate.batch
  "The pure code of one batch of the microbatch gate: the turn of a thing,
  which clock is kept for a batch,
  what an act asks of other tasks, what a task replies, which acts meet, the
  decision of an act, and the home of each row that a decision writes. Pure
  code. It needs no cluster and loads with Clojure alone.

  The gate (`softland.store.module.microbatch-gate`) reads rows, calls these
  functions and writes rows; for a batch's clock it also reads the clock and
  appends one record to a depot. None of them reads a PState, a clock or a
  random source, and none throws on what arrived. They add no check: each
  check is `softland.store.gate.decision`'s, called with the depot :microbatch
  on one act with all its rows. So the same checks serve both gates, and
  given the things of a batch in any order of arrival these functions give
  the same decisions.

  The words of a batch.

    batch          the things that one run of the gate takes from the depot
    turn           the number of a thing in its batch. The acts of a batch
                   are decided in the order of their turns, with one thing
                   that is not: a mark of a key holds from the batch after
                   (see \"The marked keys\", below)
    base           the first stamp of a batch. An act's stamp is the base
                   plus its turn
    deciding task  the task that the hash of the entity of an act's first
                   fact gives. An act that meets no other is decided there
    home task      the task on which a row lives
    the gate's task  the task that the hash of the depot word :microbatch
                   gives. The gate's last stamp is there, and the clock that
                   is kept for the newest batch
    the batch's clock  the one clock time from which a batch's base is made
                   and against which the 10-second rule is held. It is kept
                   for the batch, so it is the same on every attempt
    to meet        two acts of a batch meet when both need one row and
                   cannot be put in order on one task. They are decided on
                   the meeting task of their layer, the task that the hash
                   of the layer id gives, in the order of their turns
    question       what an act sends to the home task of a row it needs
    reply          what the home task sends back
    parcel         rows that one task sends to another to write, or an act
                   sent to the meeting task
    pass           the walk of a meeting task over the acts sent to it
    to keep        what a batch decided and the gate's depot of answers
                   does not hold yet: its answers and its strays
    keeping task   the task that keeps what one batch decided until the
                   next run of the gate appends it: the task that the hash
                   of the batch's id gives (`keeping-route`)

  A value never names a task. It names a route: an id or a text whose hash
  the gate's partitioner turns into a task. The route of an answer is the
  uuid of its act-id; of a fact, its entity; of a row of the facts by
  value, its value digest; of an act, the
  entity of its first fact; of an act that meets, its layer.

  The data between these functions and the gate, by step of the gate's walk.

    step 1  `arrive` gives messages, each [route kind key payload]. The gate
            sends each by its route and gathers the payloads by kind and
            key. The kinds: :entry, the act as step 3 needs it, under its
            turn; :answer and :place, the questions, under the key
            of the row asked for; :numbers, what the gate's task needs for
            the base. A question about a place also holds :marks-place, the
            key of its layer's marked-keys place, and :k, the fact's key.
    step 2  On the gate's task, first the batch's clock: `kept-clock` reads
            it out of the row that is kept for the newest batch;
            `clock-record` makes the record that the gate appends when none
            is kept; and `clock-row-to-keep` is the rule of the topology
            that keeps the row, first write wins. Then `base` and
            `last-stamp-of-batch`, from that clock. On each
            task `marks-places-asked` names the marked-keys places that the
            task reads from its own copy, once for a layer; `reads-of`
            gives one read for each key that was asked for, with :since for
            a place whose key is marked; `batch-end` gives the last stamp
            that the batch can hold; and `replies` gives
            [route turn [reply]] for each asker. The reply about an answer
            carries :batch-end; the reply about a place under a marked key
            carries :since.
    step 3  `items-of-task` joins a task's entries with their replies and
            marks which acts meet (`who-meets`). An item holds
            :since-by-place, the stamp of each of its places whose key is
            marked. `own-places-wanted` names the rows that the task reads
            for an act at its turn. `decide-or-send` gives the fact rows to
            write at once and the parcels, each [route kind payload].
    step 4  The gate writes the rows that a task was sent, the `:rows` of
            what it gathered, a row of the kind :by-value among them; a
            row of that kind that is nil takes its key out. No order is
            needed between them: each key of a batch has one sender, or
            all its senders are in one pass, which sends one row for a
            key, the last. `acts-that-meet` gives the acts sent
            to a meeting task, and `decide-next` decides one of them
            against the rows that earlier acts of the pass wrote.
            `rows-to-send` gives the rows of a pass.

  The marked keys. A layer marks a key by a fact whose row every task holds
  (`on-every-task?`). For a fact under a marked key the gate keeps one more
  row, in the facts by value, on the task of the value's digest; and it
  takes the row of the value before out, which needs the row before of the
  fact's place. Three things follow here.

    Where the marks are read. On the home task of a place, in step 2, from
    that task's own copy, once for each layer that the batch's questions
    name there. Nothing writes the facts in steps 1 and 2, and the batch
    before is committed on every task before this one starts; so every act
    of a batch is decided with the marks as every task held them when the
    batch began, the same on every attempt. That is why a mark holds from
    the batch after, whatever the turns of the mark and of a fact are. A
    key that is newly marked gets the last stamp of its batch
    (`batch-end`), which is not below any stamp of that batch. So one
    sentence holds: the order of a marked key holds every fact under that
    key whose stamp is later than the key's stamp, and no other.

    The row before. A place under a marked key is read when an asker does
    not have it as its own, and its row goes in the reply; an asker reads
    its own place at its turn (`own-places-wanted`).

    Two acts on one place under a marked key meet, also when each has the
    place as its own. A row of the facts by value lives on a third task,
    and what several tasks or steps send there has no order; so every key
    of that order has one writer in a batch, or all its writers are in one
    pass, which decides them in turn and sends one row for a key, the last.
    When the first of two such acts puts an entry and the second takes it
    out, the row sent is the removal: a key that was never written is taken
    out, which changes nothing.

  The keeping. What a batch decided also goes to the gate's depot of
  answers, and no attempt appends what it decided itself: an append from
  inside an attempt stays when the attempt fails. So a batch puts its
  entries into `:to-keep`, a table of the gate that Rama sets back with the
  answers' rows, and the next run of the gate, which begins only when the
  batch is committed on every task, appends them. The pure parts, by step.

    step 1  `stray-sends` gives the two messages of a stray: its entry to
            the batch's keeping task, and a word to the gate's task.
    step 2  `to-close-of` gives what the batch leaves for the next run to
            close: its id, and its clock when it made a stamp. `closes?`
            says whether this run closes a batch: whether the run before
            left one to close. For the
            batch before, on that batch's keeping task: `to-keep-range`
            names the keys to read, `records-of-closing` makes the records
            to append, `max-kept` entries to a record at most, the first
            with the batch's clock and last stamp, and `keys-kept` gives
            the keys to take out behind the appends.
    step 4  `rows-to-keep` gives what a task sends to the batch's keeping
            task, as rows of the kind :to-keep: the answers among the rows
            that it wrote and among the rows of its pass, and the strays
            that step 1 sent to it, `max-kept` to a value at most, under
            keys of `to-keep-key`.

  State and effects. None.

  Callers. `softland.store.module.microbatch-gate`; `softland.store.module`
  for `first-entity`, by which the microbatch depot partitions; and
  `softland.store.module.batch-clock` for `clock-row-to-keep` and
  `gates-depot`."
  (:require [softland.store.gate.decision :as decision]
            [softland.store.door.digest :as digest]
            [softland.store.gate.first-act :as first-act]
            [softland.store.record.rows :as record]
            [softland.store.gate.stamp :as stamp]))

(def gates-depot
  "The depot of the gate that this code serves. It is also the route of the
  gate's task: the task that the hash of this word gives."
  :microbatch)

;; ------------------------------------------------- what the depot derives

(defn- plain-map?
  "True for a map that is no record."
  [x]
  (and (map? x) (not (record? x))))

(defn first-entity
  "What the microbatch depot derives from a thing to place it. Takes
  anything. For a plain map whose `:facts` is a sequence whose first part is
  a plain map, gives that fact's `:e`. For anything else gives nil. It never
  throws, and it runs on the appending client, so no thing can stop a
  partition through it. An act on one entity then arrives on the task where
  its places are."
  [thing]
  (try
    (let [facts (when (plain-map? thing) (get thing :facts))
          fact (when (sequential? facts) (first facts))]
      (when (plain-map? fact) (get fact :e)))
    (catch Throwable _ nil)))

;; ------------------------------------------------------------ the turn

(defn position
  "Takes a task's `:turns`, {:batch :given} or nil, and the text of the
  microbatch id of the batch that is running. Gives the position of the next
  thing that the task takes from its depot partition in this batch, from 0:
  the count given so far when it belongs to this batch, else 0."
  [turns batch-text]
  (if (and turns (= batch-text (:batch turns)))
    (long (:given turns))
    0))

(defn turns-after
  "Takes the text of the running batch's microbatch id and the position that
  was just given. Gives the task's `:turns` to write: the batch, and how many
  things of it have a turn now."
  [batch-text position]
  {:batch batch-text :given (inc (long position))})

(defn turn
  "Takes the position of a thing in its depot partition's part of the batch,
  the module's number of tasks, and the number of that partition. Gives the
  thing's turn: position x tasks + partition. So the things of one partition
  keep the depot's order, and between two partitions the lower position goes
  first, then the lower partition. Nothing in a turn comes from a clock, a
  process or the order in which messages arrive, so it is the same on every
  attempt of the batch."
  [position tasks partition]
  (+ (* (long position) (long tasks)) (long partition)))

;; ------------------------------------------------------- the gathering

(defn- greater
  "The greater of two numbers, where nil is no number."
  [a b]
  (cond (nil? a) b
        (nil? b) a
        :else (max a b)))

(defn gather
  "How the gate puts two gathered values of one key together. Takes two
  values, each nil, a vector, or the map of a batch's numbers. Gives the two
  vectors as one, or for two maps the greater of each field. nil is the
  empty value. The order of a gathered vector means nothing: each function
  that reads one sorts it or looks for a turn."
  [a b]
  (cond (nil? a) b
        (nil? b) a
        (map? a) (merge-with greater a b)
        :else (into (vec a) b)))

;; --------------------------------------------- step 1: arrive and question

(def ^:private keys-on-every-task
  "The store keys whose rows every task holds in a microbatch layer."
  #{:softland.store/depot
    :softland.store/permission
    :softland.store/who-may-see
    :softland.store/builds-on
    :softland.store/marked-keys})

(defn on-every-task?
  "Takes a fact's key. Gives true for the five store keys whose rows every
  task holds in a microbatch layer: `:softland.store/depot`,
  `:softland.store/permission`, `:softland.store/who-may-see`,
  `:softland.store/builds-on` and `:softland.store/marked-keys`. Every act
  needs the permission of its `:who` on the task where it is decided, so a
  copy is on each. A gate reads a layer's marked keys on the home task of
  each place, and a reader finds who may see a layer and what it builds on
  on the task where it reads. Each is small and written rarely. What follows
  from this one function: such a fact asks no question (`questions`), its
  act goes to the meeting task (`who-meets`), and its row is sent to every
  task (`routed`)."
  [k]
  (contains? keys-on-every-task k))

(defn- place-of
  "Takes an act and one of its facts. Gives the key of the fact's place."
  [act fact]
  (record/place-key (:layer act) (:e fact) (:k fact)))

(defn- first-entity-of
  "Takes an act in its one form. Gives the entity of its first fact: the
  route of its deciding task."
  [act]
  (:e (first (:facts act))))

(defn standing-on-arrival
  "What step 1 can say of the standing of an act, from this task's own copy
  of the layer's permissions. Takes an act; the row at the place of `:who`'s
  permission; and the row at the place of the layer's depot, read only when
  the first is nil. Gives {:standing word}, with one of:

    :stands  the row holds the act's permission, and it allows acting
    :final   the act is refused for its standing, and nothing in this batch
             can change that. :word is the reason. A permission row that is
             there never changes, and a permission whose holder is not the
             act's `:who` can never come to be in `:who`'s place. A row
             that holds the act's permission, where that permission says
             `:see`, is final too, with the word
             :permission-does-not-allow-acting: the act then asks for its
             answer only, and for none of its rows
    :open    the permission's place is empty, and a give or a making act of
             this batch could still fill it. The act goes to the meeting
             task. :layer-in-record? says whether the depot row was there;
             when it was not, the record holds nothing of the layer"
  [act permission-row depot-row]
  (let [word (decision/standing gates-depot act permission-row depot-row false)]
    (cond
      (and (record/holds-value? permission-row (:permission act))
           (= :permission-does-not-allow-acting word))
      {:standing :final :word word}

      (record/holds-value? permission-row (:permission act))
      {:standing :stands}

      (or permission-row (and depot-row (= :who-not-the-permissions-holder word)))
      {:standing :final :word word}

      :else
      {:standing :open :layer-in-record? (some? depot-row)})))

(defn questions
  "The questions of one act. Takes the act; its turn; its route, to which
  the replies go; and whether it asks for rows beside its answer. Gives
  messages [route kind key [question]]. A question holds :turn, :to and
  :question, one of two words:

    :answer     to the task of the act-id's uuid, always: is there an answer
    :place      to the task of the entity, for each fact under a plain key.
                :wanted? says that
                the fact names something to replace, so the row is needed.
                :own? says that the entity is the act's first entity, so the
                place is on the act's own deciding task and is read there at
                its turn. :marks-place is the key of the layer's marked-keys
                place and :k the fact's key: the home task reads from them
                whether the place's key is marked

  A fact under a key whose row every task holds (`on-every-task?`) asks
  nothing; those are the five store keys, so no fact under a store key asks
  about a place."
  [act turn to asks-rows?]
  (let [layer (:layer act)
        act-uuid (nth (:act-id act) 2)
        asker {:turn turn :to to}
        first-e (first-entity-of act)
        marks-place (decision/marked-keys-place act)]
    (into [[act-uuid :answer (record/answer-key layer act-uuid) [(assoc asker :question :answer)]]]
          (when asks-rows?
            (for [fact (:facts act)
                  :when (not (on-every-task? (:k fact)))]
              [(:e fact) :place (place-of act fact)
               [(assoc asker :question :place
                       :wanted? (some? (:replaces fact))
                       :own? (= first-e (:e fact))
                       :marks-place marks-place
                       :k (:k fact))]])))))

(defn- carried-of
  "Takes an act, or nil. Gives the greatest stamp that it carries, or nil.
  It never throws."
  [act]
  (when act
    (try (stamp/greatest-carried act)
         (catch Throwable _ nil))))

(defn arrive
  "Step 1 for one thing that is no stray. Takes the thing; what
  `softland.store.door.declaration/check` gave for it, as the act or nil and the
  reason word or nil; the thing's turn and its position; and, for an act,
  the two rows of `standing-on-arrival`. Gives the messages that the gate
  sends, each [route kind key payload]:

    the entry    what step 3 needs of the thing, under its turn. For an act:
                 the thing, the act, its standing and the two rows, sent to
                 the task of its first entity. For a thing that the
                 declaration refused: its word and its digest, and no facts,
                 sent to the task of its act-id's uuid, where its one row,
                 the answer, will be
    the questions  `questions`. An act refused for good, and an act into a
                 layer of which the record holds nothing, ask for their
                 answer only
    the numbers  the greatest stamp that the thing carries and its position,
                 to the gate's task

  What must hold: `stray-reason` gave nil for the thing, so its act-id is
  [layer :microbatch uuid] with a layer within the limit."
  [thing act declarations-reason turn position permission-row depot-row]
  (let [act-id (get thing :act-id)
        [layer _ act-uuid] act-id
        numbers [gates-depot :numbers :batch {:carried (carried-of act) :position (long position)}]]
    (if declarations-reason
      [[act-uuid :entry turn [{:turn turn :act-id act-id :to act-uuid
                               :refused declarations-reason :digest (digest/digest thing)}]]
       [act-uuid :answer (record/answer-key layer act-uuid) [{:turn turn :to act-uuid :question :answer}]]
       numbers]
      (let [to (first-entity-of act)
            {:keys [standing word layer-in-record?]} (standing-on-arrival act permission-row depot-row)
            asks-rows? (boolean (or (= :stands standing) (and (= :open standing) layer-in-record?)))
            entry (cond-> {:turn turn :act-id act-id :to to :thing thing :act act
                           :standing standing :asks-rows? asks-rows?
                           :permission-row permission-row :depot-row depot-row}
                    word (assoc :final-word word))]
        (-> [[to :entry turn [entry]]]
            (into (questions act turn to asks-rows?))
            (conj numbers))))))

;; ------------------------------------------------ step 2: the batch's clock

(defn- as-long
  "Takes anything. Gives it as a long when it is an integer that a long
  holds, else nil. It never throws."
  [x]
  (when (and (integer? x) (<= Long/MIN_VALUE x Long/MAX_VALUE))
    (long x)))

(defn clock-record
  "The record that the gate appends to the depot of batch clocks, to keep a
  batch's clock. Takes the microbatch id of the run, which Rama gives as a
  Long, and a reading of the clock in milliseconds. Gives
  {:batch :clock}, both as longs: the two words say what the numbers are, so
  that a reader of that log needs nothing else to read it. Nothing of an act
  is in it. Gives nil when either is no integer; it
  never throws. The topology that keeps the clock writes nothing for nil
  (`clock-row-to-keep`), so the gate then finds no clock kept and fails the
  attempt, and makes no stamp."
  [batch-id clock-millis]
  (let [batch (as-long batch-id)
        clock (as-long clock-millis)]
    (when (and batch clock)
      {:batch batch :clock clock})))

(defn kept-clock
  "The clock that is kept for a batch. Takes the row that the table of the
  kept clock holds, {:batch :clock} or nil, and the microbatch id of the
  run. Gives the row's `:clock` when the row's `:batch` is that id, else
  nil: for no row, for a row of another batch, and for a thing that is no
  such row. It never throws. The gate takes a batch's clock out of this
  function and from nowhere else, so every attempt of a batch that makes a
  stamp makes it from the one clock that was kept for the batch's id."
  [row batch-id]
  (try
    (let [batch (as-long (get row :batch))
          clock (as-long (get row :clock))
          id (as-long batch-id)]
      (when (and batch clock id (= batch id))
        clock))
    (catch Throwable _ nil)))

(defn clock-row-to-keep
  "The rule of the topology that keeps a batch's clock: first write wins.
  Takes the row that the table holds, {:batch :clock} or nil, and a record
  of the depot of batch clocks. Gives the row to write, {:batch :clock} as
  longs, when the table holds no row or a row of an older batch. Gives nil,
  and so nothing is written:

    for a record of the row's own batch   the clock that was kept first for
        a batch stays, however often Rama hands the topology a record, and
        when two attempts of one batch each appended a reading
    for a record of an older batch   a record of a closed batch that Rama
        hands over late never puts the row back
    for a thing that is no map with two integers under :batch and :clock

  It never throws: a record that throws would be tried again for ever. The
  rule rests on the id of a batch going up and never coming again; it is
  the same id on each attempt of a batch. What it writes for one record is
  the same however often it runs."
  [held record]
  (try
    (let [batch (as-long (get record :batch))
          clock (as-long (get record :clock))
          held-batch (as-long (get held :batch))]
      (when (and (map? record) batch clock (or (nil? held-batch) (< held-batch batch)))
        {:batch batch :clock clock}))
    (catch Throwable _ nil)))

;; ------------------------------------------------------- step 2: the stamp

(def ^:private greatest-counter
  "The greatest counter that a stamp holds."
  (dec (bit-shift-left 1 stamp/counter-bits)))

(defn base
  "The base of a batch: its first stamp. Takes the batch's clock, in
  milliseconds: the one clock that is kept for the batch, the same on every
  attempt, and nil for a batch with no numbers; the gate's last stamp, or
  nil; and
  the batch's numbers, {:carried :position}, or nil for a batch that took no
  thing or only strays. Gives nil for no numbers, before it looks at the
  clock: such a batch gives no
  stamp. Else the next stamp after the clock, the last stamp, and the
  greatest carried stamp of the batch, the last held to the greatest stamp
  that the 10-second rule lets pass. So the base is later than each stamp
  that the gate gave before, on any task, and later than each stamp of each
  act of the batch that can be accepted; and a forged stamp puts the stamps
  of one batch 10 seconds ahead at most."
  [now-millis last-stamp numbers]
  (when numbers
    (let [carried (:carried numbers)
          bound (stamp/pack (+ (long now-millis) stamp/max-ahead-millis) greatest-counter)]
      (stamp/next-stamp now-millis last-stamp (when carried (min (long carried) bound))))))

(defn stamp-of-turn
  "Takes the base of a batch and a turn. Gives the stamp of the thing with
  that turn: the base plus the turn. One stamp for an act, on its answer and
  on each of its facts. Two things never share a turn, so two acts never
  share a stamp, and the stamps of a batch go up in the order of the turns
  on every task."
  [base turn]
  (+ (long base) (long turn)))

(defn last-stamp-of-batch
  "Takes the base of a batch, its numbers and the module's number of tasks.
  Gives the gate's new last stamp: the base plus the greatest turn that the
  batch can hold, so it is not below any stamp of the batch."
  [base numbers tasks]
  (+ (long base) (* (inc (long (:position numbers))) (long tasks)) -1))

(defn closes?
  "The test of a closing. Takes the `:to-close` that the head of step 2 read
  on the gate's task: what the run before left to close, {:batch} with
  `:clock` when that batch made a stamp, or nil. Gives true when this run
  closes a batch: the thing holds a batch's id. True once after each batch
  that left something to keep, a batch of strays alone among them, and
  never in a quiet time: a batch with nothing to keep takes `:to-close`
  out (`to-close-of`). The gate then appends what the batch left to keep,
  and the tasks write `:closed-through` when the batch made a stamp. It
  never throws."
  [to-close]
  (some? (as-long (get to-close :batch))))

;; ------------------------------------------------------ step 2: the replies

(defn marks-places-asked
  "Takes what a task gathered in step 1, a map from kind to key to
  questions. Gives the keys of the marked-keys places that the task reads
  from its own copy before it replies: the one of each layer that a question
  about a place names, each once, in the order of the
  keys. So the marks cost one read for a task and a layer in a batch,
  however many acts the batch holds. Every question about a place is of a
  fact under a plain key, which may be a marked one: a fact under a store
  key asks about no place (`questions`), and no store key can be marked."
  [asked]
  (->> (vals (:place asked))
       (apply concat)
       (keep :marks-place)
       (distinct)
       (sort)
       (vec)))

(defn reads-of
  "Takes what a task gathered in step 1, a map from kind to key to
  questions; and the rows of `marks-places-asked` as the task read them, a
  map from key to row. Gives one read for each key that was asked for, as a
  map:

    {:about :answer :key k :questions qs}
    {:about :place  :key k :questions qs
                    :since the stamp of the place's key, only when the
                    layer marks that key
                    :read? whether the row is read: an asker that does not
                    have the place as its own wants it, because its fact
                    names something to replace or because the key is
                    marked}

  The questions about one place name one layer and one key, so the first
  tells both."
  [asked marks-rows]
  (let [marked-at (into {} (map (fn [[place row]] [place (record/marked-keys-of row)])) marks-rows)]
    (vec (concat
          (for [[k qs] (:answer asked)]
            {:about :answer :key k :questions qs})
          (for [[k qs] (:place asked)]
            (let [asked-first (first qs)
                  since (get (get marked-at (:marks-place asked-first)) (:k asked-first))]
              (cond-> {:about :place :key k :questions qs
                       :read? (boolean (some #(and (or (:wanted? %) since) (not (:own? %))) qs))}
                since (assoc :since since))))))))

(defn batch-end
  "Takes the base of a batch, or nil; its numbers; and the module's number of
  tasks. Gives nil for a batch with no base, else the last stamp that the
  batch can hold (`last-stamp-of-batch`): the stamp that a key gets when an
  act of this batch marks it. It reads no clock; a task computes it from
  what the gate's task handed on, and every task gets the same number."
  [base numbers tasks]
  (when base
    (last-stamp-of-batch base numbers tasks)))

(defn- answer-replies
  "The replies about an act-id. Takes the answer's row or nil, the
  questions, the batch's clock, its base and its end. A row there: each
  asker is told :used. No row: the asker with the least turn is told :yours,
  each other :taken-in-this-batch. This is check (a): the first thing under
  an act-id uses it, whether it is then accepted or refused. The reply
  carries the clock, the base and the batch's end to the task that decides."
  [row qs now-millis base batch-end]
  (let [first-turn (reduce min (map :turn qs))]
    (mapv (fn [q]
            [(:to q) (:turn q)
             [{:about :answer
               :says (cond row :used
                           (= first-turn (:turn q)) :yours
                           :else :taken-in-this-batch)
               :clock now-millis
               :base base
               :batch-end batch-end}]])
          qs)))

(defn- place-replies
  "The replies about a place. Takes the place's key; its row or nil; the
  questions; and the stamp of the place's key when the layer marks it, else
  nil. Under a key that is not marked the place is shared when more than one
  act of the batch names it, unless each of them has it as its own; and the
  row goes to each asker that wants it and does not have the place as its
  own. Under a marked key the place is shared whenever two acts of the batch
  name it, so that both go to the meeting task; each asker is told :since,
  the key's stamp; and the row goes to each asker that does not have the
  place as its own, whether its fact names something to replace or not."
  [k row qs since]
  (let [shared? (boolean (and (< 1 (count (distinct (map :turn qs))))
                              (or since (not (every? :own? qs)))))]
    (mapv (fn [q]
            [(:to q) (:turn q)
             [(cond-> {:about :place :key k :shared? shared?}
                since (assoc :since since)
                (and (or (:wanted? q) since) (not (:own? q))) (assoc :row row))]])
          qs)))

(defn replies
  "The replies of a home task about one key. Takes one read of `reads-of`;
  what the gate read for it: the answer's row or the place's row, nil when
  nothing was read or nothing is there; the batch's clock; its base; and
  its end (`batch-end`). Gives one [route turn [reply]] for each asker. A
  reply holds :about, and:

    about an answer     :says, one of :yours, :used, :taken-in-this-batch;
                        :clock, :base and :batch-end
    about a place       :key, :shared?, and :row when the asker wanted it.
                        Under a marked key also :since, the key's stamp,
                        and the row for each asker that does not have the
                        place as its own

  The asker of the reply is found by the route, the task that decides the
  act, and by the turn."
  [read found now-millis base batch-end]
  (let [qs (:questions read)]
    (case (:about read)
      :answer (answer-replies found qs now-millis base batch-end)
      :place (place-replies (:key read) found qs (:since read))
      [])))

;; ------------------------------------------------------- step 3: who meets

(defn- with-replies
  "Takes an entry and the replies gathered for its turn. Gives the entry
  with what the replies say: :says, :clock, :base and :batch-end from the
  reply about its answer; :shared?, true when any reply says so;
  :place-rows, the rows of the places it asked for; :since-by-place, a map
  from the key of each of its places whose key is marked to that key's
  stamp."
  [entry replies]
  (let [answer (first (filter #(= :answer (:about %)) replies))]
    (assoc entry
           :says (:says answer)
           :clock (:clock answer)
           :base (:base answer)
           :batch-end (:batch-end answer)
           :shared? (boolean (some :shared? replies))
           :since-by-place (into {}
                                 (keep (fn [reply]
                                         (when (and (= :place (:about reply)) (:since reply))
                                           [(:key reply) (:since reply)])))
                                 replies)
           :place-rows (into {}
                             (keep (fn [reply]
                                     (when (and (= :place (:about reply)) (contains? reply :row))
                                       [(:key reply) (:row reply)])))
                             replies))))

(defn- to-decide?
  "Takes an item. Gives true when the item is an act whose checks against
  the record are still to be made: its act-id is its own to use, the
  declaration passed it, and step 1 did not refuse it for good."
  [item]
  (and (= :yours (:says item))
       (nil? (:refused item))
       (not= :final (:standing item))))

(defn- own-places
  "Takes an act. Gives the keys of its own places: the place of each fact on
  the act's first entity, under a key whose row is not on every task. Their
  home is the act's deciding task."
  [act]
  (let [first-e (first-entity-of act)]
    (into []
          (comp (filter (fn [fact] (and (= first-e (:e fact)) (not (on-every-task? (:k fact))))))
                (map (fn [fact] (place-of act fact))))
          (:facts act))))

(defn who-meets
  "Which acts of a task go to the meeting task. Takes the task's items.
  Gives the set of their turns. An act goes when:

    1. it holds a fact under a key whose row every task holds
       (`on-every-task?`): a layer's depot, a permission, who may see the
       layer, what it builds on, its marked keys;
    2. its standing is :open: the place of its permission is empty, and an
       earlier act of this batch may fill it;
    3. a reply says :shared: a place that it names is named by an
       act that cannot be put in order with it on this task, or a place
       under a marked key that it names is named by another act at all;
    4. an act of this task with an earlier turn that goes to the meeting
       task names one of its own places.

  Every other act is decided where it arrived. An act that is not decided at
  all, a repeat or a refusal of step 1, is in neither group. The walk is in
  the order of the turns, so rule 4 carries on from act to act."
  [items]
  (:meeting
   (reduce (fn [{:keys [meeting named] :as state} item]
             (if-not (to-decide? item)
               state
               (let [act (:act item)
                     own (own-places act)]
                 (if (or (some #(on-every-task? (:k %)) (:facts act))
                         (= :open (:standing item))
                         (:shared? item)
                         (some named own))
                   {:meeting (conj meeting (:turn item)) :named (into named own)}
                   state))))
           {:meeting #{} :named #{}}
           (sort-by :turn items))))

(defn items-of-task
  "Takes what a task holds for step 3: its entries, a map from turn to a
  vector with the entry, and its replies, a map from turn to the replies.
  Gives the task's items in the order of their turns: each entry with what
  its replies say, and :meets?, whether the act goes to the meeting task."
  [entries replies]
  (let [items (->> (vals entries)
                   (apply concat)
                   (sort-by :turn)
                   (mapv (fn [entry] (with-replies entry (get replies (:turn entry))))))
        meeting (who-meets items)]
    (mapv #(assoc % :meets? (contains? meeting (:turn %))) items)))

;; -------------------------------------------------------- the decision

(defn routed
  "The home of each row that a decision writes. Takes the act-id; the act,
  or nil for a thing that the declaration refused; and what
  `softland.store.gate.decision/writes` gave. Gives a vector of rows, each
  {:which :key :row :home :every-task?}:

    :fact       home: the fact's entity. :every-task? is true for a fact
                under a key whose row every task holds (`on-every-task?`)
    :by-value   a row of the facts by value, or nil for a key to take out.
                Home: the value digest that the key holds
                (`softland.store.record.rows/by-value-route`), so the run of
                one value is on one task
    :answer     home: the uuid of the act-id

  One row for each of those three kinds that `writes` gave. The task's last
  stamp is left out: this gate has one for a batch."
  [act-id act writes]
  (let [[layer _ act-uuid] act-id
        facts (:facts act)
        by-place (into {} (map (fn [fact] [(record/place-key layer (:e fact) (:k fact)) fact])) facts)]
    (into []
          (mapcat
           (fn [[which k row]]
             (case which
               :fact (let [fact (get by-place k)]
                       [{:which :fact :key k :row row
                         :home (:e fact) :every-task? (on-every-task? (:k fact))}])
               :by-value [{:which :by-value :key k :row row
                           :home (record/by-value-route k) :every-task? false}]
               :answer [{:which :answer :key k :row row :home act-uuid :every-task? false}]
               [])))
          writes)))

(defn- decided
  "The decision of one act, with every row that it needs in hand. Takes what
  is known of the act,
  {:act-id :act :digest :clock :base :turn :since-by-place :batch-end}; the
  row at the place of `:who`'s permission and the row at the layer's depot
  place, each as it is at the act's turn; whether the other gate's record
  holds the layer; and the rows of its places. It calls the
  checks of `softland.store.gate.decision` in their order: the 10-second
  rule, the standing, the places. Gives the routed rows
  that the decision writes: the answer alone for a refusal.

  `:since-by-place` is what step 2's replies said of the act's places: the
  marks as the batch began. `:batch-end` is the stamp that a key gets which
  the act newly marks. What must hold for an accept: the place rows hold
  the row before of each place of `:since-by-place`, and of the marked-keys
  place when the act writes it."
  [{:keys [act-id act digest clock base turn since-by-place batch-end]}
   permission-row depot-row other-holds? place-rows]
  (let [refused (or (decision/refused-early clock act nil)
                    (decision/standing gates-depot act permission-row depot-row other-holds?)
                    (decision/places gates-depot act
                                     (decision/makes-the-layer? gates-depot act permission-row depot-row other-holds?)
                                     place-rows))
        in-hand (if refused
                  {}
                  {:since-by-place since-by-place
                   :place-rows place-rows
                   :level-from batch-end})]
    (routed act-id act (decision/writes act-id act refused (stamp-of-turn base turn) digest in-hand))))

;; ---------------------------------------------------------- step 3: decide

(defn own-places-wanted
  "Takes an item. Gives the keys of the rows that the deciding task reads
  for it at its turn: its own places whose fact names something to replace,
  and its own places whose key is marked (`:since-by-place`), whose row
  before gives the entry to take out of the facts by value. None for an item
  that is not decided, for an act that asked for no rows, and for an act
  that the 10-second rule refuses."
  [item]
  (let [act (:act item)]
    (if (and (to-decide? item)
             (:asks-rows? item)
             (nil? (decision/refused-early (:clock item) act nil)))
      (let [first-e (first-entity-of act)
            marked (:since-by-place item)]
        (into []
              (comp (filter (fn [fact] (and (= first-e (:e fact))
                                            (not (on-every-task? (:k fact)))
                                            (or (:replaces fact)
                                                (contains? marked (place-of act fact))))))
                    (map (fn [fact] (place-of act fact))))
              (:facts act)))
      [])))

(defn- as-parcel
  "Takes a routed row. Gives it as a parcel: [route :rows [[which key row]]]."
  [row]
  [(:home row) :rows [[(:which row) (:key row) (:row row)]]])

(defn decide-or-send
  "Step 3 for one item, at its turn. Takes the item, and the rows of
  `own-places-wanted` as the task holds them now, a map from key to row.
  Gives {:now :parcels}. :now holds [key row] for each fact row that the
  task writes at once, so that a later act of the pass reads it: the facts
  of an act decided here, on its own places. :parcels holds
  [route kind payload] for everything else:

    told :used or :taken-in-this-batch   nothing. A repeat is answered from
                 the record; a different thing under a used act-id gets no
                 answer of its own
    refused by the declaration   its answer
    refused for good in step 1   its answer. The 10-second rule is held
                 first, with the batch's clock, and gives the word when it
                 fails: it comes before the standing in the order of the
                 checks
    an act that meets   one parcel of the kind :meet, to its layer's task:
                 the act, its digest, the clock, the base and the batch's
                 end, its two standing rows, the rows of its places, and
                 the stamp of each of its places whose key is marked
    any other act   decided here. Each row but the facts of its own places
                 is a parcel of the kind :rows, to the row's home: its
                 answer, a fact on another entity, and each row of the
                 facts by value, a row that is nil among them"
  [item own-rows]
  (let [{:keys [says act-id act turn clock base refused standing]} item]
    (cond
      (not= :yours says)
      {:now [] :parcels []}

      refused
      {:now []
       :parcels (mapv as-parcel
                      (routed act-id nil
                              (decision/writes act-id nil refused (stamp-of-turn base turn) (:digest item) {})))}

      (= :final standing)
      (let [word (or (decision/refused-early clock act nil) (:final-word item))]
        {:now []
         :parcels (mapv as-parcel
                        (routed act-id act
                                (decision/writes act-id act word (stamp-of-turn base turn)
                                                 (digest/digest (:thing item)) {})))})

      :else
      (let [known {:turn turn :act-id act-id :act act :digest (digest/digest (:thing item))
                   :clock clock :base base
                   :since-by-place (:since-by-place item) :batch-end (:batch-end item)}
            place-rows (merge (:place-rows item) own-rows)]
        (if (:meets? item)
          {:now []
           :parcels [[(:layer act) :meet
                      [(assoc known
                              :permission-row (:permission-row item)
                              :depot-row (:depot-row item)
                              :place-rows place-rows)]]]}
          (let [rows (decided known (:permission-row item) nil false place-rows)
                first-e (:to item)
                own? (fn [row] (and (= :fact (:which row)) (= first-e (:home row))))]
            {:now (mapv (juxt :key :row) (filter own? rows))
             :parcels (mapv as-parcel (remove own? rows))}))))))

;; -------------------------------------------------- step 4: write and meet

(defn acts-that-meet
  "Takes what a task gathered in step 3. Gives the acts that were sent to it
  as their layer's meeting task, in the order of their turns."
  [parcels]
  (vec (sort-by :turn (:meet parcels))))

(defn new-pass
  "Gives an empty pass: no fact row written, nothing to send. `:facts` is a
  map from a place's key to the row that the pass wrote there last; `:out`
  holds the routed rows of each decision of the pass, in order."
  []
  {:facts {} :out []})

(defn- standing-rows-at-turn
  "Takes a pass and an act sent to the meeting task. Gives [permission-row
  depot-row] as they are at the act's turn: what an earlier act of the pass
  wrote, else what the act carried. The depot row is nil when the
  permission's row is there, as a gate reads it."
  [pass sent]
  (let [act (:act sent)
        written (:facts pass)
        permission-row (or (get written (decision/permission-place act)) (:permission-row sent))
        depot-row (when (nil? permission-row)
                    (or (get written (decision/depot-place act)) (:depot-row sent)))]
    [permission-row depot-row]))

(defn reads-other-record?
  "Takes a pass and an act sent to the meeting task. Gives true when the
  gate must ask the other gate's record for the act's layer: neither this
  record nor the pass holds anything of the layer, and the layer starts with
  the other gate's depot. Only then can the other record hold it."
  [pass sent]
  (let [[permission-row depot-row] (standing-rows-at-turn pass sent)]
    (boolean (and (nil? permission-row)
                  (nil? depot-row)
                  (not= gates-depot (first-act/depot-at-making (:layer (:act sent))))))))

(defn depot-place-of
  "Takes an act sent to the meeting task. Gives the key of its layer's depot
  place, which the gate reads in the other gate's record."
  [sent]
  (decision/depot-place (:act sent)))

(defn meeting-reads
  "Takes a pass and an act sent to the meeting task. Gives the keys that the
  meeting task reads from its own copy of the rows that every task holds:
  the place of each fact of the act under :softland.store/permission; of
  each fact under :softland.store/depot, :softland.store/who-may-see or
  :softland.store/builds-on that names something to replace; and of each
  fact under :softland.store/marked-keys, whether it names something to
  replace or not, since its row before gives the stamps that its keys keep.
  An act that holds no fact under :softland.store/marked-keys does not read
  the marks here: it has the stamp of each of its places from step 2. None
  when neither the record nor the pass holds anything of the layer, and none
  that the pass wrote."
  [pass sent]
  (let [act (:act sent)
        [permission-row depot-row] (standing-rows-at-turn pass sent)
        written (:facts pass)]
    (if (and (nil? permission-row) (nil? depot-row))
      []
      (into []
            (comp (filter (fn [fact]
                            (let [k (:k fact)]
                              (or (= :softland.store/permission k)
                                  (= :softland.store/marked-keys k)
                                  (and (on-every-task? k) (:replaces fact))))))
                  (map (fn [fact] (place-of act fact)))
                  (remove #(contains? written %)))
            (:facts act)))))

(defn- lay-over
  "Takes a pass and the routed rows of one decision. Gives the pass with the
  fact rows laid over the fact rows that it holds, so that a later act of
  the pass finds them, and with every row added to what it sends."
  [pass rows]
  (reduce (fn [pass {:keys [which key row]}]
            (if (= :fact which)
              (assoc-in pass [:facts key] row)
              pass))
          (update pass :out into rows)
          rows))

(defn decide-next
  "The decision of the next act of a pass. Takes the pass; the act as it was
  sent; the rows that the meeting task read for it from its own copy
  (`meeting-reads`), a map from key to row; and the row at the layer's depot
  place in the other gate's record, or nil when it was not read or is not
  there. Gives the pass after the act.

  Each row is taken as it is at the act's turn: what an earlier act of the
  pass wrote, else what the meeting task read, else what the act carried.
  So of two acts of a pass that write one place under a marked key, the
  second takes out the entry that the first put; and of two that write the
  marks, the second finds the first one's stamps, so a key that both hold
  keeps one stamp. Which keys are marked is not taken from the pass: each
  act carries the stamps that step 2 gave it, the marks as the batch began."
  [pass sent local-rows other-depot-row]
  (let [act (:act sent)
        [permission-row depot-row] (standing-rows-at-turn pass sent)
        written (:facts pass)
        carried (merge (:place-rows sent) local-rows)
        place-rows (into {}
                         (map (fn [fact]
                                (let [k (place-of act fact)]
                                  [k (if (contains? written k) (get written k) (get carried k))])))
                         (:facts act))]
    (lay-over pass (decided sent permission-row depot-row (some? other-depot-row) place-rows))))

(defn rows-to-send
  "Takes a pass that is done. Gives the rows that the meeting task sends, as
  `routed` gives them: one row for each kind and key, the last that the pass
  wrote, so that of two acts that write one place the later turn's row is
  the one written. The rows keep the order in which the pass wrote them,
  and nothing rests on that order: one row is sent for a key. For
  a key of the facts by value the last row can be nil, a removal: when an
  earlier act of the pass put the entry and a later one took it out, the
  key is taken out at its home, where it was never written."
  [pass]
  (let [out (:out pass)
        id (juxt :which :key)
        last-at (into {} (map-indexed (fn [i row] [(id row) i])) out)]
    (into [] (keep-indexed (fn [i row] (when (= i (get last-at (id row))) row))) out)))

;; ------------------------------------ the keeping of what a batch decided

(def max-kept
  "The most entries, answers and strays together, that one value of
  `:to-keep` holds, and that one record of a closing holds. A number of the
  build. It bounds the two vectors of a value, which are therefore not
  subindexed, and the size of a record of the gate's depot of answers."
  256)

(defn keeping-route
  "Takes the microbatch id of a batch. Gives the route of the batch's
  keeping task: the id as a long, whose hash the gate's partitioner turns
  into a task. One task keeps what one batch decided until the next run of
  the gate appends it, so the closing reads on one task; another batch has
  another route, so neither the reads nor the records of the depot gather
  on one task. Nothing is read from the task that a record sits on."
  [batch-id]
  (long batch-id))

(defn stray-sends
  "Step 1 for a stray. Takes the microbatch id of the batch; the stray's
  word; the digest of the thing as hex text; and the number of the task on
  which it arrived, where the set of its word is. Gives the two messages
  that the gate sends, each [route kind key payload]:

    the stray's entry  `softland.store.record.rows/stray-record`, to the
                       batch's keeping task, under the kind :stray
    a word             to the gate's task, under the kind :stray-sent: the
                       batch has something to keep. Step 2 would not know
                       it otherwise: a stray has no numbers, and a batch of
                       strays alone makes no stamp. The words of a batch
                       gather into one small map

  Each arrival of a stray sends both, a second arrival of one thing too."
  [batch-id word digest-hex task]
  [[(keeping-route batch-id) :stray :batch [(record/stray-record word digest-hex task)]]
   [gates-depot :stray-sent :batch {:sent 1}]])

(defn to-close-of
  "What a batch leaves for the next run of the gate to close. Takes the
  microbatch id of the batch; the batch's clock, or nil for a batch that
  made no stamp; and what the gate's task gathered under :stray-sent, or
  nil when no stray arrived. Gives the `:to-close` that the gate's task
  writes: {:batch id :clock clock} for a batch that made a stamp,
  {:batch id} for a batch of strays alone, and nil for a batch with nothing
  to keep. A batch that made a stamp leaves it also when each of its things
  was a repeat and no answer was written: its clock and its last stamp go
  to the log all the same."
  [batch-id batch-clock stray-sent]
  (cond
    batch-clock {:batch (long batch-id) :clock (long batch-clock)}
    stray-sent  {:batch (long batch-id)}
    :else       nil))

(defn to-keep-key
  "Takes the microbatch id of a batch; the number of the task that sends a
  value to keep; and the number of the value among those that the task
  sends in this batch, from 0. Gives the key of the value in `:to-keep`, a
  text: the id in 19 digits, the task in 5 and the part in 5, with a space
  between. So the keys of one batch are neighbours and one range read gives
  them (`to-keep-range`), and two tasks never write one key. A text and no
  packed number: the id of a batch goes up with every run for as long as
  the module lives, and 19 digits hold every long. A task or a part of more
  than five digits prints longer and still names one key."
  [batch-id task part]
  (format "%019d %05d %05d" (long batch-id) (long task) (long part)))

(defn to-keep-range
  "Takes the microbatch id of a batch. Gives [from to], the two ends of the
  batch's keys in `:to-keep`, `to` not in the range: the id in 19 digits
  and a space, and the same text with `!`, the character after a space, in
  the place of the space."
  [batch-id]
  (let [id (format "%019d" (long batch-id))]
    [(str id " ") (str id "!")]))

(defn- kept-answer
  "Takes an answer's key and its row. Gives the answer as a value of
  `:to-keep` holds it: {:layer :uuid :stamp :digest}, and `:refused` when
  the row holds a word. The act-id is held as the text of its layer and its
  uuid, so that each position has one class. nil for a key that is no
  answer's key."
  [k row]
  (when-let [[layer-text act-uuid] (record/answer-key-parts k)]
    (cond-> {:layer layer-text :uuid act-uuid :stamp (:stamp row) :digest (:digest row)}
      (:refused row) (assoc :refused (:refused row)))))

(defn- in-parts
  "Takes answers and strays, each a sequence. Gives them in parts of
  `max-kept` entries at most, the answers first: a vector of
  {:answers :strays}, each a vector. None for no entry."
  [answers strays]
  (->> (concat (map (fn [answer] [:answers answer]) answers)
               (map (fn [stray] [:strays stray]) strays))
       (partition-all max-kept)
       (mapv (fn [part]
               (reduce (fn [held [which entry]] (update held which conj entry))
                       {:answers [] :strays []}
                       part)))))

(defn rows-to-keep
  "What a task sends to keep in step 4. Takes the microbatch id of the
  batch; the number of the task; the rows that the task wrote from its
  parcels, each [which key row]; the rows of its pass, as `rows-to-send`
  gives them; and the strays' entries that step 1 sent to this task, or
  nil. Gives rows as `routed` gives them, of the kind :to-keep, each
  {:which :to-keep :key :row :home :every-task?}. The home is the batch's
  keeping route, the key is `to-keep-key`'s, and the row is a value of
  `:to-keep`:

    {:batch    the id of the batch
     :answers  each answer among the rows, as {:layer :uuid :stamp :digest}
               with `:refused` for a refusal
     :strays   the strays' entries, as they came}

  A value holds `max-kept` entries at most, answers and strays together,
  and a part with nothing in it is absent. None for a task with no answer
  and no stray. Each answer of a batch is in one value: an answer's row is
  written from one parcel or sent by one pass, and the answers of a pass
  are kept by the task of the pass, because their home writes them in the
  send that carries these rows. The answers are in the order of their keys
  and the strays in the order of their parts, so a second attempt of the
  batch puts the same values under the same keys."
  [batch-id task rows-written rows-of-pass strays]
  (let [answers (->> (concat (keep (fn [[which k row]] (when (= :answer which) [k row])) rows-written)
                             (keep (fn [{:keys [which key row]}] (when (= :answer which) [key row])) rows-of-pass))
                     (sort-by first)
                     (keep (fn [[k row]] (kept-answer k row))))
        strays (sort-by (juxt :task :stray :digest) strays)
        id (long batch-id)]
    (into []
          (map-indexed
           (fn [part {:keys [answers strays]}]
             {:which :to-keep
              :key (to-keep-key id task part)
              :row (cond-> {:batch id}
                     (seq answers) (assoc :answers answers)
                     (seq strays) (assoc :strays strays))
              :home (keeping-route id)
              :every-task? false}))
          (in-parts answers strays))))

(defn records-of-closing
  "The records that the run which closes a batch appends to the gate's
  depot of answers. Takes the `:to-close` that the head of step 2 read,
  {:batch :clock} or {:batch}; the gate's last stamp as the head read it,
  which is the last stamp that the batch can hold; and what the keeping
  task read under the batch's keys of `:to-keep`, a sequence of [key value]
  in the order of the keys. Gives a vector of records. Each holds `:batch`,
  the id, and the `:answers` and `:strays` of
  `softland.store.record.rows/log-record`: every entry that was kept,
  `max-kept` to a record at most, the answers first. An answer's entry is
  `softland.store.record.rows/answer-record`'s, with the layer read back
  from its text. The first record also holds `:clock` and `:last-stamp`,
  both only when `:to-close` held a clock: what a batch that made a stamp
  keeps of itself. A batch that made a stamp and wrote no answer gives that
  one record alone. A batch with no clock and nothing kept, and a thing
  that is no `:to-close`, give none. No record holds a value of a fact."
  [to-close last-stamp kept]
  (if-let [batch (as-long (get to-close :batch))]
    (let [clock (as-long (get to-close :clock))
          of-itself (when (and clock (as-long last-stamp))
                      {:clock clock :last-stamp (as-long last-stamp)})
          values (map second kept)
          answers (for [value values
                        held (:answers value)]
                    (record/answer-record gates-depot (record/id-of-text (:layer held)) (:uuid held) held))
          strays (mapcat :strays values)
          parts (in-parts answers strays)
          parts (if (and (empty? parts) of-itself) [{:answers [] :strays []}] parts)]
      (into []
            (map-indexed
             (fn [i {:keys [answers strays]}]
               (merge {:batch batch}
                      (when (zero? i) of-itself)
                      (record/log-record answers strays))))
            parts))
    []))

(defn keys-kept
  "Takes what the keeping task read under a batch's keys of `:to-keep`, a
  sequence of [key value]. Gives the keys, which the closing takes out
  behind its appends."
  [kept]
  (mapv first kept))
