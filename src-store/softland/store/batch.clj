(ns softland.store.batch
  "The pure code of one batch of the microbatch gate: the turn of a thing,
  what an act asks of other tasks, what a task replies, which acts meet, the
  decision of an act, and the home of each row that a decision writes. Pure
  code. It needs no cluster and loads with Clojure alone.

  The gate (`softland.store.microbatch-gate`) only reads rows, calls these
  functions and writes rows. None of them reads a PState, a clock or a
  random source, and none throws on what arrived. They add no check: each
  check is `softland.store.decision`'s, called with the class :microbatch
  on one act with all its rows. So the same checks serve both gates, and
  given the things of a batch in any order of arrival these functions give
  the same decisions.

  The words of a batch.

    batch          the things that one run of the gate takes from the depot
    turn           the number of a thing in its batch. The acts of a batch
                   are decided in the order of their turns
    base           the first stamp of a batch. An act's stamp is the base
                   plus its turn
    deciding task  the task that the hash of the entity of an act's first
                   fact gives. An act that meets no other is decided there
    home task      the task on which a row lives
    the gate's task  the task that the hash of the class word :microbatch
                   gives. The gate's last stamp is there
    to meet        two acts of a batch meet when both need one row and
                   cannot be put in order on one task. They are decided on
                   the meeting task of their layer, the task that the hash
                   of the layer id gives, in the order of their turns
    question       what an act sends to the home task of a row it needs
    reply          what the home task sends back
    parcel         rows that one task sends to another to write, or an act
                   sent to the meeting task
    pass           the walk of a meeting task over the acts sent to it

  A value never names a task. It names a route: an id whose hash the gate's
  partitioner turns into a task. The route of an answer is the uuid of its
  act-id; of a fact, its entity; of a lock's home, its request's uuid; of an
  act, the entity of its first fact; of an act that meets, its layer.

  The data between these functions and the gate, by step of the gate's walk.

    step 1  `arrive` gives messages, each [route kind key payload]. The gate
            sends each by its route and gathers the payloads by kind and
            key. The kinds: :entry, the act as step 3 needs it, under its
            turn; :answer, :place and :locks, the questions, under the key
            of the row asked for; :numbers, what the gate's task needs for
            the base.
    step 2  `base` and `last-stamp-of-batch` on the gate's task. On each
            task `reads-of` gives one read for each key that was asked for,
            and `replies` gives [route turn [reply]] for each asker.
    step 3  `items-of-task` joins a task's entries with their replies and
            marks which acts meet (`who-meets`). `own-places-wanted` names
            the rows that the task reads for an act at its turn.
            `decide-or-send` gives the fact rows to write at once and the
            parcels, each [route kind payload].
    step 4  `rows-in-write-order` gives the rows that a task was sent.
            `acts-that-meet` gives the acts sent to a meeting task, and
            `decide-next` decides one of them against the rows that earlier
            acts of the pass wrote. `rows-to-send` gives the rows of a pass.

  State and effects. None.

  Callers. `softland.store.microbatch-gate`, and `softland.store.module`
  for `first-entity`, by which the microbatch depot partitions."
  (:require [softland.store.decision :as decision]
            [softland.store.digest :as digest]
            [softland.store.first-act :as first-act]
            [softland.store.record :as record]
            [softland.store.stamp :as stamp]))

(def gates-class
  "The class of the gate that this code serves. It is also the route of the
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

(defn on-every-task?
  "Takes a fact's key. Gives true for the two store keys whose rows every
  task holds in a microbatch layer: the class and a permission. Every act
  needs the permission of its `:who` on the task where it is decided, so a
  copy is on each."
  [k]
  (or (= :class k) (= :permission k)))

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
  layer and this gate's class. Only such a lock can have a row in this
  record of this layer."
  [act lock-id]
  (let [[lock-layer lock-class _] (first lock-id)]
    (and (= (:layer act) lock-layer) (= gates-class lock-class))))

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
  permission; and the row at the place of the layer's class, read only when
  the first is nil. Gives {:standing word}, with one of:

    :stands  the row holds the act's permission
    :final   the act is refused for its standing, and nothing in this batch
             can change that. :word is the reason. A permission row that is
             there never changes, and a permission whose holder is not the
             act's `:who` can never come to be in `:who`'s place
    :open    the permission's place is empty, and a give or a making act of
             this batch could still fill it. The act goes to the meeting
             task. :layer-in-record? says whether the class row was there;
             when it was not, the record holds nothing of the layer"
  [act permission-row class-row]
  (let [word (decision/standing gates-class act permission-row class-row false)]
    (cond
      (record/holds-plain-value? permission-row (:permission act))
      {:standing :stands}

      (or permission-row (and class-row (= :who-not-the-permissions-holder word)))
      {:standing :final :word word}

      :else
      {:standing :open :layer-in-record? (some? class-row)})))

(defn questions
  "The questions of one act. Takes the act; its turn; its route, to which
  the replies go; and whether it asks for rows beside its answer. Gives
  messages [route kind key [question]]. A question holds :turn, :to and
  :question, one of four words:

    :answer     to the task of the act-id's uuid, always: is there an answer
    :place      to the task of the entity, for each fact under a plain key
                or under :lock-request. :wanted? says that the fact names
                something to replace, so the row is needed. :own? says that
                the entity is the act's first entity, so the place is on the
                act's own deciding task and is read there at its turn
    :locks      to the task of a request's uuid, once for each request whose
                locks the act names: the range of keys that
                `softland.store.decision/lock-ranges` gives, and the indexes
                named
    :new-locks  to the task of the act's own uuid, for a request of its own:
                how many locks it will make

  A fact under :class or :permission asks nothing: every task holds its
  row. The two kinds about locks are gathered under one key, the answer key
  of the request, so that a request and each use of its locks are seen
  together."
  [act turn to asks-rows?]
  (let [layer (:layer act)
        act-uuid (nth (:act-id act) 2)
        asker {:turn turn :to to}
        first-e (first-entity-of act)]
    (into [[act-uuid :answer (record/answer-key layer act-uuid) [(assoc asker :question :answer)]]]
          (when asks-rows?
            (concat
             (for [fact (:facts act)
                   :when (not (on-every-task? (:k fact)))]
               [(:e fact) :place (place-of act fact)
                [(assoc asker :question :place
                        :wanted? (some? (:replaces fact))
                        :own? (= first-e (:e fact)))]])
             (for [[request-uuid indexes] (lock-indexes-by-request act)]
               [request-uuid :locks (record/answer-key layer request-uuid)
                [(assoc asker :question :locks
                        :range [(record/lock-key layer request-uuid (reduce min indexes))
                                (record/lock-key layer request-uuid (reduce max indexes))]
                        :indexes (set indexes))]])
             (for [fact (:facts act)
                   :when (and (= :lock-request (:k fact)) (some? (:v fact)))]
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
  `softland.store.declaration/check` gave for it, as the act or nil and the
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
  [thing act declarations-reason turn position permission-row class-row]
  (let [act-id (get thing :act-id)
        [layer _ act-uuid] act-id
        numbers [gates-class :numbers :batch {:carried (carried-of act) :position (long position)}]]
    (if declarations-reason
      [[act-uuid :entry turn [{:turn turn :act-id act-id :to act-uuid
                               :refused declarations-reason :digest (digest/digest thing)}]]
       [act-uuid :answer (record/answer-key layer act-uuid) [{:turn turn :to act-uuid :question :answer}]]
       numbers]
      (let [to (first-entity-of act)
            {:keys [standing word layer-in-record?]} (standing-on-arrival act permission-row class-row)
            asks-rows? (boolean (or (= :stands standing) (and (= :open standing) layer-in-record?)))
            entry (cond-> {:turn turn :act-id act-id :to to :thing thing :act act
                           :standing standing :asks-rows? asks-rows?
                           :permission-row permission-row :class-row class-row}
                    word (assoc :final-word word))]
        (-> [[to :entry turn [entry]]]
            (into (questions act turn to asks-rows?))
            (conj numbers))))))

;; ------------------------------------------------------- step 2: the stamp

(def ^:private greatest-counter
  "The greatest counter that a stamp holds."
  (dec (bit-shift-left 1 stamp/counter-bits)))

(defn base
  "The base of a batch: its first stamp. Takes the gate's clock now, in
  milliseconds, read once for the batch; the gate's last stamp, or nil; and
  the batch's numbers, {:carried :position}, or nil for a batch that took no
  thing or only strays. Gives nil for no numbers: such a batch gives no
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

(defn reads-of
  "Takes what a task gathered in step 1, a map from kind to key to
  questions. Gives one read for each key that was asked for, as a map:

    {:about :answer :key k :questions qs}
    {:about :place  :key k :questions qs :read? whether the row is read: an
                    asker that does not have the place as its own wants it}
    {:about :locks  :key k :questions qs :ranges the ranges of lock keys to
                    read, each once}"
  [asked]
  (vec (concat
        (for [[k qs] (:answer asked)]
          {:about :answer :key k :questions qs})
        (for [[k qs] (:place asked)]
          {:about :place :key k :questions qs
           :read? (boolean (some #(and (:wanted? %) (not (:own? %))) qs))})
        (for [[k qs] (:locks asked)]
          {:about :locks :key k :questions qs
           :ranges (vec (distinct (keep :range qs)))}))))

(defn- answer-replies
  "The replies about an act-id. Takes the answer's row or nil, the
  questions, the batch's clock and its base. A row there: each asker is told
  :used. No row: the asker with the least turn is told :yours, each other
  :taken-in-this-batch. This is check (a): the first thing under an act-id
  uses it, whether it is then accepted or refused. The reply carries the
  clock and the base to the task that decides."
  [row qs now-millis base]
  (let [first-turn (reduce min (map :turn qs))]
    (mapv (fn [q]
            [(:to q) (:turn q)
             [{:about :answer
               :says (cond row :used
                           (= first-turn (:turn q)) :yours
                           :else :taken-in-this-batch)
               :clock now-millis
               :base base}]])
          qs)))

(defn- place-replies
  "The replies about a place. Takes the place's key, its row or nil, and the
  questions. The place is shared when more than one act of the batch names
  it, unless each of them has it as its own. The row goes to each asker
  that wants it and does not have the place as its own."
  [k row qs]
  (let [shared? (boolean (and (< 1 (count (distinct (map :turn qs))))
                              (not (every? :own? qs))))]
    (mapv (fn [q]
            [(:to q) (:turn q)
             [(cond-> {:about :place :key k :shared? shared?}
                (and (:wanted? q) (not (:own? q))) (assoc :row row))]])
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
  rows as a map, nil when nothing was read; the batch's clock; and its base.
  Gives one [route turn [reply]] for each asker. A reply holds :about, and:

    about an answer     :says, one of :yours, :used, :taken-in-this-batch;
                        :clock and :base
    about a place       :key, :shared?, and :row when the asker wanted it
    about locks         :shared?, and :rows, a map from key to row
    about new locks     :shared?

  The asker of the reply is found by the route, the task that decides the
  act, and by the turn."
  [read found now-millis base]
  (let [qs (:questions read)]
    (case (:about read)
      :answer (answer-replies found qs now-millis base)
      :place (place-replies (:key read) found qs)
      :locks (lock-replies found qs)
      [])))

;; ------------------------------------------------------- step 3: who meets

(defn- with-replies
  "Takes an entry and the replies gathered for its turn. Gives the entry
  with what the replies say: :says, :clock and :base from the reply about
  its answer; :shared?, true when any reply says so; :place-rows, the rows
  of the places it asked for; :lock-rows, the rows of the locks it names."
  [entry replies]
  (let [answer (first (filter #(= :answer (:about %)) replies))]
    (assoc entry
           :says (:says answer)
           :clock (:clock answer)
           :base (:base answer)
           :shared? (boolean (some :shared? replies))
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

    1. it holds a fact under :class or :permission: it writes a row that
       every task holds;
    2. its standing is :open: the place of its permission is empty, and an
       earlier act of this batch may fill it;
    3. a reply says :shared: a place or a lock that it names is named by an
       act that cannot be put in order with it on this task;
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
  `softland.store.decision/writes` gave. Gives a vector of rows, each
  {:which :key :row :home :every-task?}:

    :fact       home: the fact's entity. :every-task? is true for a fact
                under :class or :permission
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
               :new-lock [{:which :new-lock :key k :row nil :home act-uuid :every-task? false}]
               :answer [{:which :answer :key k :row row :home act-uuid :every-task? false}]
               [])))
          writes)))

(defn- decided
  "The decision of one act, with every row that it needs in hand. Takes what
  is known of the act, {:act-id :act :digest :clock :base :turn}; the row at
  the place of `:who`'s permission and the row at the layer's class place,
  each as it is at the act's turn; whether the other gate's record holds the
  layer; the rows of its places; and its lock rows. It calls the checks of
  `softland.store.decision` in their order: the 10-second rule, the
  standing, the places, the seals. Gives the routed rows that the decision
  writes: the answer alone for a refusal."
  [{:keys [act-id act digest clock base turn]} permission-row class-row other-holds? place-rows lock-rows]
  (let [refused (or (decision/refused-early clock act nil)
                    (decision/standing gates-class act permission-row class-row other-holds?)
                    (decision/places gates-class act
                                     (decision/makes-the-layer? gates-class act permission-row class-row other-holds?)
                                     place-rows)
                    (decision/seals gates-class act lock-rows))]
    (routed act-id act (decision/writes act-id act refused (stamp-of-turn base turn) digest lock-rows))))

;; ---------------------------------------------------------- step 3: decide

(defn own-places-wanted
  "Takes an item. Gives the keys of the rows that the deciding task reads
  for it at its turn: its own places whose fact names something to replace.
  None for an item that is not decided, for an act that asked for no rows,
  and for an act that the 10-second rule refuses."
  [item]
  (let [act (:act item)]
    (if (and (to-decide? item)
             (:asks-rows? item)
             (nil? (decision/refused-early (:clock item) act nil)))
      (let [first-e (first-entity-of act)]
        (into []
              (comp (filter (fn [fact] (and (:replaces fact)
                                            (= first-e (:e fact))
                                            (not (on-every-task? (:k fact))))))
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
                 the act, its digest, the clock and the base, its two
                 standing rows, and the rows of its places and locks
    any other act   decided here. Each row but the facts of its own places
                 is a parcel of the kind :rows, to the row's home; also both
                 rows of a lock that it uses, so that step 4 writes them in
                 its order"
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
                   :clock clock :base base}
            place-rows (merge (:place-rows item) own-rows)]
        (if (:meets? item)
          {:now []
           :parcels [[(:layer act) :meet
                      [(assoc known
                              :permission-row (:permission-row item)
                              :class-row (:class-row item)
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
  class-row] as they are at the act's turn: what an earlier act of the pass
  wrote, else what the act carried. The class row is nil when the
  permission's row is there, as a gate reads it."
  [pass sent]
  (let [act (:act sent)
        written (:facts pass)
        permission-row (or (get written (decision/permission-place act)) (:permission-row sent))
        class-row (when (nil? permission-row)
                    (or (get written (decision/class-place act)) (:class-row sent)))]
    [permission-row class-row]))

(defn reads-other-record?
  "Takes a pass and an act sent to the meeting task. Gives true when the
  gate must ask the other gate's record for the act's layer: neither this
  record nor the pass holds anything of the layer, and the layer starts with
  the other gate's class. Only then can the other record hold it."
  [pass sent]
  (let [[permission-row class-row] (standing-rows-at-turn pass sent)]
    (boolean (and (nil? permission-row)
                  (nil? class-row)
                  (not= gates-class (first-act/class-at-making (:layer (:act sent))))))))

(defn class-place-of
  "Takes an act sent to the meeting task. Gives the key of its layer's class
  place, which the gate reads in the other gate's record."
  [sent]
  (decision/class-place (:act sent)))

(defn meeting-reads
  "Takes a pass and an act sent to the meeting task. Gives the keys that the
  meeting task reads from its own copy of the rows that every task holds:
  the place of each fact of the act under :permission, and of each fact
  under :class that names something to replace. None when neither the record
  nor the pass holds anything of the layer, and none that the pass wrote."
  [pass sent]
  (let [act (:act sent)
        [permission-row class-row] (standing-rows-at-turn pass sent)
        written (:facts pass)]
    (if (and (nil? permission-row) (nil? class-row))
      []
      (into []
            (comp (filter (fn [fact] (or (= :permission (:k fact))
                                         (and (= :class (:k fact)) (:replaces fact)))))
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
  (`meeting-reads`), a map from key to row; the row at the layer's class
  place in the other gate's record, or nil when it was not read or is not
  there; and the row of a lock with bytes drawn for this call alone. Gives
  the pass after the act.

  Each row is taken as it is at the act's turn: what an earlier act of the
  pass wrote, else what the meeting task read, else what the act carried. A
  lock that a request of this pass makes has no bytes yet, so the check of
  the seal is given the drawn row for it: no sender can have sealed with
  bytes that are not made, and the use is refused as a value that the lock
  does not open."
  [pass sent local-rows other-class-row drawn-lock-row]
  (let [act (:act sent)
        [permission-row class-row] (standing-rows-at-turn pass sent)
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
    (lay-over pass (decided sent permission-row class-row (some? other-class-row) place-rows lock-rows))))

(defn rows-to-send
  "Takes a pass that is done. Gives the rows that the meeting task sends, as
  `routed` gives them: one row for each kind and key, the last that the pass
  wrote, so that of two acts that write one place the later turn's row is
  the one written; and every home's row of a lock before any other row."
  [pass]
  (let [out (:out pass)
        id (juxt :which :key)
        last-at (into {} (map-indexed (fn [i row] [(id row) i])) out)
        kept (keep-indexed (fn [i row] (when (= i (get last-at (id row))) row)) out)
        home? #(= :lock-home (:which %))]
    (into (filterv home? kept) (remove home?) kept)))
