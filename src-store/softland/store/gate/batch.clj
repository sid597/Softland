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

  A value never names a task. It names a route: an id or a text whose hash
  the gate's partitioner turns into a task. The route of an answer is the
  uuid of its act-id; of a fact, its entity; of a lock's home, its request's
  uuid; of a row of the facts by value, its value digest; of an act, the
  entity of its first fact; of an act that meets, its layer.

  The data between these functions and the gate, by step of the gate's walk.

    step 1  `arrive` gives messages, each [route kind key payload]. The gate
            sends each by its route and gathers the payloads by kind and
            key. The kinds: :entry, the act as step 3 needs it, under its
            turn; :answer, :place and :locks, the questions, under the key
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
    step 4  `rows-in-write-order` gives the rows that a task was sent, a
            row of the kind :by-value among them; a row of that kind that
            is nil takes its key out. `acts-that-meet` gives the acts sent
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

  State and effects. None.

  Callers. `softland.store.module.microbatch-gate`; `softland.store.module`
  for `first-entity`, by which the microbatch depot partitions; and
  `softland.store.module.batch-clock` for `clock-row-to-keep` and
  `gates-depot`."
  (:require [softland.store.door.declaration :as declaration]
            [softland.store.gate.decision :as decision]
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

(defn- lock-in-this-record?
  "Takes an act and a lock id. Gives true when the lock id names the act's
  layer and this gate's depot. Only such a lock can have a row in this
  record of this layer."
  [act lock-id]
  (let [[lock-layer lock-depot _] (first lock-id)]
    (and (= (:layer act) lock-layer) (= gates-depot lock-depot))))

(defn- lock-indexes-by-request
  "Takes an act. Gives, for each request whose locks the act names in this
  record, [the request's uuid, the indexes named]."
  [act]
  (->> (:facts act)
       (keep :lock-id)
       (filter #(lock-in-this-record? act %))
       (group-by (fn [[[_ _ request-uuid] _]] request-uuid))
       (mapv (fn [[request-uuid lock-ids]] [request-uuid (mapv second lock-ids)]))))

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
  :question, one of four words:

    :answer     to the task of the act-id's uuid, always: is there an answer
    :place      to the task of the entity, for each fact under a plain key
                or under :softland.store/lock-request. :wanted? says that
                the fact names something to replace, so the row is needed.
                :own? says that the entity is the act's first entity, so the
                place is on the act's own deciding task and is read there at
                its turn. :marks-place is the key of the layer's marked-keys
                place and :k the fact's key: the home task reads from them
                whether the place's key is marked
    :locks      to the task of a request's uuid, once for each request whose
                locks the act names: the range of keys that
                `softland.store.gate.decision/lock-ranges` gives, and the indexes
                named
    :new-locks  to the task of the act's own uuid, for a request of its own:
                how many locks it will make

  A fact under a key whose row every task holds (`on-every-task?`) asks
  nothing. The two kinds about locks are gathered
  under one key, the answer key of the request, so that a request and each
  use of its locks are seen together."
  [act turn to asks-rows?]
  (let [layer (:layer act)
        act-uuid (nth (:act-id act) 2)
        asker {:turn turn :to to}
        first-e (first-entity-of act)
        marks-place (decision/marked-keys-place act)]
    (into [[act-uuid :answer (record/answer-key layer act-uuid) [(assoc asker :question :answer)]]]
          (when asks-rows?
            (concat
             (for [fact (:facts act)
                   :when (not (on-every-task? (:k fact)))]
               [(:e fact) :place (place-of act fact)
                [(assoc asker :question :place
                        :wanted? (some? (:replaces fact))
                        :own? (= first-e (:e fact))
                        :marks-place marks-place
                        :k (:k fact))]])
             (for [[request-uuid indexes] (lock-indexes-by-request act)]
               [request-uuid :locks (record/answer-key layer request-uuid)
                [(assoc asker :question :locks
                        :range [(record/lock-key layer request-uuid (reduce min indexes))
                                (record/lock-key layer request-uuid (reduce max indexes))]
                        :indexes (set indexes))]])
             (for [fact (:facts act)
                   :when (and (= :softland.store/lock-request (:k fact)) (some? (:v fact)))]
               [act-uuid :locks (record/answer-key layer act-uuid)
                [(assoc asker :question :new-locks :makes (:v fact))]]))))))

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
  and no lock's bytes are in it. Gives nil when either is no integer; it
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
  "Takes the gate's last stamp as it was before this batch, and the
  `:closed-through` of the gate's task. Gives true when the tasks must write
  a new `:closed-through`: there is a last stamp, and it is not the one
  that is closed. True once after each batch that gave a stamp, and never in
  a quiet time."
  [last-stamp closed-through]
  (boolean (and last-stamp (not= last-stamp closed-through))))

;; ------------------------------------------------------ step 2: the replies

(defn marks-places-asked
  "Takes what a task gathered in step 1, a map from kind to key to
  questions. Gives the keys of the marked-keys places that the task reads
  from its own copy before it replies: the one of each layer that a question
  about a place under a plain key names, each once, in the order of the
  keys. So the marks cost one read for a task and a layer in a batch,
  however many acts the batch holds. A question about the place of a request
  for locks names none: no store key can be marked, so a batch of requests
  reads no marks."
  [asked]
  (->> (vals (:place asked))
       (apply concat)
       (remove #(declaration/store-key? (:k %)))
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
    {:about :locks  :key k :questions qs :ranges the ranges of lock keys to
                    read, each once}

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
                since (assoc :since since))))
          (for [[k qs] (:locks asked)]
            {:about :locks :key k :questions qs
             :ranges (vec (distinct (keep :range qs)))})))))

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

(defn- lock-replies
  "The replies about the locks of one request. Takes the lock rows that were
  read, a map from key to row, and the questions. A lock is shared when an
  act of another turn names it too, or when a request of another turn in
  this batch will make it. An asker that names a shared lock is told so, and
  so is a request whose new lock another act names. Each asker gets the rows
  of its own range."
  [rows qs]
  (let [uses (filter #(= :locks (:question %)) qs)
        makings (filter #(= :new-locks (:question %)) qs)
        named-by-another? (fn [turn index]
                            (some #(and (not= turn (:turn %)) (contains? (:indexes %) index)) uses))
        made-by-another? (fn [turn index]
                           (some #(and (not= turn (:turn %)) (< index (:makes %))) makings))]
    (mapv (fn [q]
            (let [turn (:turn q)]
              [(:to q) turn
               [(if (= :new-locks (:question q))
                  {:about :new-locks
                   :shared? (boolean (some (fn [use]
                                             (and (not= turn (:turn use))
                                                  (some #(< % (:makes q)) (:indexes use))))
                                           uses))}
                  (let [[first-key last-key] (:range q)]
                    {:about :locks
                     :shared? (boolean (some #(or (named-by-another? turn %) (made-by-another? turn %))
                                             (:indexes q)))
                     :rows (into {}
                                 (filter (fn [[k _]] (and (<= (compare first-key k) 0)
                                                          (<= (compare k last-key) 0))))
                                 rows)}))]]))
          qs)))

(defn replies
  "The replies of a home task about one key. Takes one read of `reads-of`;
  what the gate read for it: the answer's row, the place's row, or the lock
  rows as a map, nil when nothing was read; the batch's clock; its base; and
  its end (`batch-end`). Gives one [route turn [reply]] for each asker. A
  reply holds :about, and:

    about an answer     :says, one of :yours, :used, :taken-in-this-batch;
                        :clock, :base and :batch-end
    about a place       :key, :shared?, and :row when the asker wanted it.
                        Under a marked key also :since, the key's stamp,
                        and the row for each asker that does not have the
                        place as its own
    about locks         :shared?, and :rows, a map from key to row
    about new locks     :shared?

  The asker of the reply is found by the route, the task that decides the
  act, and by the turn."
  [read found now-millis base batch-end]
  (let [qs (:questions read)]
    (case (:about read)
      :answer (answer-replies found qs now-millis base batch-end)
      :place (place-replies (:key read) found qs (:since read))
      :locks (lock-replies found qs)
      [])))

;; ------------------------------------------------------- step 3: who meets

(defn- with-replies
  "Takes an entry and the replies gathered for its turn. Gives the entry
  with what the replies say: :says, :clock, :base and :batch-end from the
  reply about its answer; :shared?, true when any reply says so;
  :place-rows, the rows of the places it asked for; :since-by-place, a map
  from the key of each of its places whose key is marked to that key's
  stamp; :lock-rows, the rows of the locks it names."
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
                             replies)
           :lock-rows (into {} (mapcat :rows) (filter #(= :locks (:about %)) replies)))))

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
    3. a reply says :shared: a place or a lock that it names is named by an
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
    :lock       the whole row of a lock that the act uses, with its bytes.
                Home: the entity of the value that it seals
    :lock-home  the home's row of that lock, with no bytes. Home: the uuid
                of the lock's request
    :new-lock   a key at which a new lock is made. Home: the act's uuid
    :answer     home: the uuid of the act-id

  So a lock that comes to seal a value gives two rows, and its bytes are in
  one. The home's row comes before the whole row. The task's last stamp is
  left out: this gate has one for a batch."
  [act-id act writes]
  (let [[layer _ act-uuid] act-id
        facts (:facts act)
        by-place (into {} (map (fn [fact] [(record/place-key layer (:e fact) (:k fact)) fact])) facts)
        by-lock (into {}
                      (keep (fn [fact]
                              (when-let [[[_ _ request-uuid] index] (:lock-id fact)]
                                [(record/lock-key layer request-uuid index) fact])))
                      facts)]
    (into []
          (mapcat
           (fn [[which k row]]
             (case which
               :fact (let [fact (get by-place k)]
                       [{:which :fact :key k :row row
                         :home (:e fact) :every-task? (on-every-task? (:k fact))}])
               :lock (let [fact (get by-lock k)
                           request-uuid (nth (first (:lock-id fact)) 2)]
                       [{:which :lock-home :key k :row (record/lock-row-without-bytes row)
                         :home request-uuid :every-task? false}
                        {:which :lock :key k :row row
                         :home (:e fact) :every-task? false}])
               :by-value [{:which :by-value :key k :row row
                           :home (record/by-value-route k) :every-task? false}]
               :new-lock [{:which :new-lock :key k :row nil :home act-uuid :every-task? false}]
               :answer [{:which :answer :key k :row row :home act-uuid :every-task? false}]
               [])))
          writes)))

(defn- decided
  "The decision of one act, with every row that it needs in hand. Takes what
  is known of the act,
  {:act-id :act :digest :clock :base :turn :since-by-place :batch-end}; the
  row at the place of `:who`'s permission and the row at the layer's depot
  place, each as it is at the act's turn; whether the other gate's record
  holds the layer; the rows of its places; and its lock rows. It calls the
  checks of `softland.store.gate.decision` in their order: the 10-second
  rule, the standing, the places, the seals. Each sealed value is opened
  once, only when no earlier check refused the act. Gives the routed rows
  that the decision writes: the answer alone for a refusal.

  `:since-by-place` is what step 2's replies said of the act's places: the
  marks as the batch began. `:batch-end` is the stamp that a key gets which
  the act newly marks. What must hold for an accept: the place rows hold
  the row before of each place of `:since-by-place`, and of the marked-keys
  place when the act writes it."
  [{:keys [act-id act digest clock base turn since-by-place batch-end]}
   permission-row depot-row other-holds? place-rows lock-rows]
  (let [refused-before (or (decision/refused-early clock act nil)
                           (decision/standing gates-depot act permission-row depot-row other-holds?)
                           (decision/places gates-depot act
                                            (decision/makes-the-layer? gates-depot act permission-row depot-row other-holds?)
                                            place-rows))
        opened (when-not refused-before (decision/opened gates-depot act lock-rows))
        refused (or refused-before (:refused opened))
        in-hand (if refused
                  {}
                  {:lock-rows lock-rows
                   :plain (:plain opened)
                   :since-by-place since-by-place
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
                 end, its two standing rows, the rows of its places and
                 locks, and the stamp of each of its places whose key is
                 marked
    any other act   decided here. Each row but the facts of its own places
                 is a parcel of the kind :rows, to the row's home; also both
                 rows of a lock that it uses, so that step 4 writes them in
                 its order, and each row of the facts by value, a row that
                 is nil among them"
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
                              :place-rows place-rows
                              :lock-rows (:lock-rows item))]]]}
          (let [rows (decided known (:permission-row item) nil false place-rows (:lock-rows item))
                first-e (:to item)
                own? (fn [row] (and (= :fact (:which row)) (= first-e (:home row))))]
            {:now (mapv (juxt :key :row) (filter own? rows))
             :parcels (mapv as-parcel (remove own? rows))}))))))

;; -------------------------------------------------- step 4: write and meet

(defn rows-in-write-order
  "Takes what a task gathered in step 3. Gives the rows that it was sent, as
  [which key row], with every home's row of a lock before any other row. So
  on a task that is both the home of a lock and the task of its value, where
  the two rows have one key, the whole row is written last and stays."
  [parcels]
  (let [rows (:rows parcels)
        home? #(= :lock-home (first %))]
    (into (filterv home? rows) (remove home?) rows)))

(defn acts-that-meet
  "Takes what a task gathered in step 3. Gives the acts that were sent to it
  as their layer's meeting task, in the order of their turns."
  [parcels]
  (vec (sort-by :turn (:meet parcels))))

(defn new-pass
  "Gives an empty pass: no fact row and no lock row written, nothing to
  send."
  []
  {:facts {} :locks {} :out []})

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
  rows laid over what it holds and added to what it sends. A new lock is
  kept as the word :new: its bytes are made at the write."
  [pass rows]
  (reduce (fn [pass {:keys [which key row]}]
            (case which
              :fact (assoc-in pass [:facts key] row)
              :lock (assoc-in pass [:locks key] row)
              :new-lock (assoc-in pass [:locks key] :new)
              pass))
          (update pass :out into rows)
          rows))

(defn decide-next
  "The decision of the next act of a pass. Takes the pass; the act as it was
  sent; the rows that the meeting task read for it from its own copy
  (`meeting-reads`), a map from key to row; the row at the layer's depot
  place in the other gate's record, or nil when it was not read or is not
  there; and the row of a lock with bytes drawn for this call alone. Gives
  the pass after the act.

  Each row is taken as it is at the act's turn: what an earlier act of the
  pass wrote, else what the meeting task read, else what the act carried.
  So of two acts of a pass that write one place under a marked key, the
  second takes out the entry that the first put; and of two that write the
  marks, the second finds the first one's stamps, so a key that both hold
  keeps one stamp. Which keys are marked is not taken from the pass: each
  act carries the stamps that step 2 gave it, the marks as the batch began.
  A lock that a request of this pass makes has no bytes yet, so the check of
  the seal is given the drawn row for it: no sender can have sealed with
  bytes that are not made, and the use is refused as a value that the lock
  does not open."
  [pass sent local-rows other-depot-row drawn-lock-row]
  (let [act (:act sent)
        [permission-row depot-row] (standing-rows-at-turn pass sent)
        written (:facts pass)
        carried (merge (:place-rows sent) local-rows)
        place-rows (into {}
                         (map (fn [fact]
                                (let [k (place-of act fact)]
                                  [k (if (contains? written k) (get written k) (get carried k))])))
                         (:facts act))
        locks (:locks pass)
        lock-rows (into {}
                        (keep (fn [fact]
                                (when-let [lock-id (:lock-id fact)]
                                  (when (lock-in-this-record? act lock-id)
                                    (let [[[_ _ request-uuid] index] lock-id
                                          k (record/lock-key (:layer act) request-uuid index)
                                          in-pass (get locks k)
                                          row (cond (= :new in-pass) drawn-lock-row
                                                    in-pass in-pass
                                                    :else (get (:lock-rows sent) k))]
                                      (when row [k row]))))))
                        (:facts act))]
    (lay-over pass (decided sent permission-row depot-row (some? other-depot-row) place-rows lock-rows))))

(defn rows-to-send
  "Takes a pass that is done. Gives the rows that the meeting task sends, as
  `routed` gives them: one row for each kind and key, the last that the pass
  wrote, so that of two acts that write one place the later turn's row is
  the one written; and every home's row of a lock before any other row. For
  a key of the facts by value the last row can be nil, a removal: when an
  earlier act of the pass put the entry and a later one took it out, the
  key is taken out at its home, where it was never written."
  [pass]
  (let [out (:out pass)
        id (juxt :which :key)
        last-at (into {} (map-indexed (fn [i row] [(id row) i])) out)
        kept (keep-indexed (fn [i row] (when (= i (get last-at (id row))) row)) out)
        home? #(= :lock-home (:which %))]
    (into (filterv home? kept) (remove home?) kept)))
