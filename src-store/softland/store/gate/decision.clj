(ns softland.store.gate.decision
  "The checks of the decision of one act, after the door's declaration, as
  functions of an act and of rows; and the rows that a decision writes, as
  data. Pure code. It needs no cluster. None of these functions reads a
  PState, a clock or a random source: a gate reads the rows, calls these,
  and writes what they give. So the same checks serve each gate, a function
  gives the same answer when Rama runs an event again, and each can be
  proven without a cluster.

  An act here is an act in its one form on arrival
  (`softland.store.door.declaration/check`). A row is a row of the record
  (`softland.store.record.rows`), or nil for a key that holds none. Each check
  takes the class of the gate that owns the record, :streaming or
  :microbatch.

  The checks, in the order in which a gate makes them. The order of the
  checks is the order of the reasons: when an act has two faults, the check
  that comes first gives the recorded word. Each check gives nil, or the
  word of its first fault. A check that examines facts takes them in the
  act's order.

    early      `refused-early`: the declaration's own word, or the 10-second
               rule on a carried stamp. Nothing of the record is read.
    standing   `standing`: the layer's class is this gate's (c); the layer
               is made, or the act is a right making act; the act's
               permission stands for its `:who` (d).
    places     `places`: each replaced fact is the current fact of its
               place (e); no setting key on an entity other than the layer
               (f); a permission fact writes a permission straight beneath
               the act's own (g); and what a store key does not take in
               this build.
    seals      `seals`: each sealed value's lock is one that the store made
               for this layer, seals nothing yet, is of a known way, and
               opens the value at its place.

  `places-needed` and `lock-ranges` say which rows the last two need.
  `stamp-of` gives the stamp of the decision. `writes` gives each row that
  the decision writes.

  How two of the checks rest on how a row can come to be, and read less
  than their words name.

  (c) costs no read for a layer that is made here. A gate's record holds
  rows only for layers of its class: a row is written only by an accept; an
  accept happens in a layer that is made here, or makes it; a making act is
  right only for the gate of the class that the layer starts with; and no
  act of this build changes a class. So a row at the permission's place, or
  at the layer's class place, says that the layer is this gate's.

  (d) is one read. A permission fact sits on its holder, so the place of
  `:who`'s permission holds the act's permission or the act's permission
  does not stand. \"Each permission above it stands\" is not read: a
  permission row is written only by a right making act, which has nothing
  above it, or by a give that passed (d) and (g), whose own permission stood
  and is exactly the one above; a permission id holds its whole chain; and
  nothing in this build ends a permission. The room for a permission that
  has ended is the place itself: an end would be a later fact on it.

  State and effects. None. `seals` opens each sealed value and keeps nothing
  of the plain bytes.

  Not here. Check (a), the answer under the act-id, and the test for a
  stray: the gate makes them before any of these. Whether the other gate's
  record holds a layer: the gate reads it and gives the answer to
  `standing`."
  (:require [softland.store.door.declaration :as declaration]
            [softland.store.gate.first-act :as first-act]
            [softland.store.record.rows :as record]
            [softland.store.door.seal :as seal]
            [softland.store.gate.stamp :as stamp]))

;; -------------------------------------------------------------------- early

(defn refused-early
  "The checks that read nothing of the record. Takes the gate's clock now, in
  milliseconds; and what `softland.store.door.declaration/check` gave for the
  thing, as its two parts: the act, or nil, and the declaration's reason
  word, or nil. Gives the declaration's word when there is one; or
  :carried-stamp-too-far-ahead when a stamp that the act carries is more
  than 10 seconds ahead of the clock; or nil."
  [now-millis act declarations-reason]
  (cond
    declarations-reason declarations-reason
    (some #(stamp/too-far-ahead? now-millis %) (vals (:based-on act))) :carried-stamp-too-far-ahead
    :else nil))

;; ----------------------------------------------------------------- standing

(defn permission-place
  "Takes an act. Gives the key of the place where the permission of the act's
  `:who` sits in the act's layer. The first row that a gate reads for the
  standing of an act."
  [act]
  (record/place-key (:layer act) (:who act) :permission))

(defn class-place
  "Takes an act. Gives the key of the place of its layer's class: the layer
  as entity, under `:class`. A gate reads it only when the place of the
  permission is empty."
  [act]
  (record/place-key (:layer act) (:layer act) :class))

(defn makes-the-layer?
  "Takes the gate's class, an act, the row at the place of `:who`'s
  permission, the row at the place of the layer's class, and whether the
  other gate's record holds the layer. Gives true when the act is the first
  act of its layer: both rows are nil, the other gate's record does not hold
  the layer, and the act has the form of a making act for this gate. What
  must hold: when the permission's row is nil, the class's row was read."
  [gate-class act permission-row class-row other-gate-holds-layer?]
  (boolean (and (nil? permission-row)
                (nil? class-row)
                (not other-gate-holds-layer?)
                (first-act/making-act? gate-class act))))

(defn standing
  "The standing of an act: checks (c) and (d), and the first act of a layer.
  Takes the gate's class; an act; the row at the place of `:who`'s
  permission; the row at the place of the layer's class, which is read only
  when the first is nil; and whether the other gate's record holds the
  layer, which is asked only when both are nil. Gives nil when the act's
  permission stands, or when the act is a right making act for a layer that
  is not made (`makes-the-layer?` tells the two apart). Else a word:

    :layer-belongs-to-the-other-gate  the layer is made, by the other gate
    :layer-not-made                   the record holds nothing for the
                                      layer, and the act is no right making
                                      act
    :who-not-the-permissions-holder   the layer is made here, and the holder
                                      that the act's permission names is not
                                      the act's `:who`
    :permission-does-not-stand        the layer is made here, and the record
                                      does not hold the act's permission for
                                      its holder"
  [gate-class act permission-row class-row other-gate-holds-layer?]
  (let [permission (:permission act)]
    (cond
      (record/holds-plain-value? permission-row permission)
      nil

      (or permission-row class-row)
      (if (= (:who act) (nth permission 0))
        :permission-does-not-stand
        :who-not-the-permissions-holder)

      other-gate-holds-layer?
      :layer-belongs-to-the-other-gate

      (first-act/making-act? gate-class act)
      nil

      :else
      :layer-not-made)))

;; ------------------------------------------------------------------- places

(defn- place-of
  "Takes an act and one of its facts. Gives the key of the fact's place."
  [act fact]
  (record/place-key (:layer act) (:e fact) (:k fact)))

(defn places-needed
  "Takes an act, and whether it makes its layer. Gives the keys of the places
  whose rows `places` needs, each once: the place of each fact that names
  something to replace, and of each fact under `:permission`. None for an
  act that makes its layer: the record holds nothing of a layer that is not
  made, and the two facts of a making act are not held to the checks that
  read a place. A fact that names nothing to replace needs no row, and so
  can never be stale."
  [act makes-the-layer?]
  (if makes-the-layer?
    []
    (into []
          (comp (filter (fn [fact] (or (:replaces fact) (= :permission (:k fact)))))
                (map (fn [fact] (place-of act fact))))
          (:facts act))))

(defn- replaced-fact-fault
  "Check (e) for one fact. Gives the word when the fact names something to
  replace and that is not the current fact of its place: the place is empty,
  another fact wrote its row, or the fact named is of another layer or
  class."
  [gate-class act rows fact]
  (when-let [replaces (:replaces fact)]
    (when-not (record/written-by? gate-class (:layer act) (get rows (place-of act fact)) replaces)
      :replaced-fact-not-current)))

(defn- setting-fault
  "Check (f) for one fact. Gives the word when the fact is under a setting
  key on an entity that is not the act's layer."
  [act fact]
  (when (and (contains? declaration/setting-keys (:k fact))
             (not= (:e fact) (:layer act)))
    :setting-not-on-the-layer))

(defn- permission-fact-fault
  "Check (g) for one fact. Gives a word when the fact is under `:permission`,
  has a value, and that permission is not one straight beneath the act's
  own, in the act's layer: its layer slot is not the act's layer; its
  granted-in slot is not the act's layer; or the permission above it is not
  exactly the one that the act acts under. A permission with nothing above
  it earns the third word."
  [act fact]
  (let [given (:v fact)]
    (when (and (= :permission (:k fact)) (some? given))
      (cond
        (not= (:layer act) (nth given 1)) :permission-fact-for-another-layer
        (not= (:layer act) (nth given 2)) :permission-fact-granted-in-another-layer
        (not= (:permission act) (nth given 3 nil)) :permission-fact-not-beneath-the-acts-own
        :else nil))))

(defn- store-key-fault
  "What a store key does not take in this build, for one fact. In this build
  a store key takes one write for a place. Gives a word for: a `:permission`
  fact with no value, since who may end a permission is not decided; a
  `:class` or `:lock-request` fact with no value; a `:class` fact in a layer
  that is made, with the other class (a change of class is not decided) or
  with the layer's own (its place holds one); a `:permission` fact on a
  place that holds a row, the same permission or another. A `:lock-request`
  fact with a value earns none: its place is new by its form. What must
  hold: the layer is made by this gate, so its class is the gate's."
  [gate-class act rows fact]
  (let [k (:k fact)
        retract? (nil? (:v fact))]
    (cond
      (and (= :permission k) retract?) :ending-a-permission-not-decided
      (and (declaration/store-key? k) retract?) :store-key-retract-not-built
      (and (= :class k) (not= gate-class (:v fact))) :changing-a-class-not-decided
      (= :class k) :store-key-takes-one-write
      (and (= :permission k) (get rows (place-of act fact))) :store-key-takes-one-write
      :else nil)))

(defn places
  "The checks of the places. Takes the gate's class; an act; whether it makes
  its layer; and the rows of the places that `places-needed` named, as a map
  from key to row, nil or absent for an empty place. Gives nil, or the word
  of the first check that fails, in this order:

    (e)  :replaced-fact-not-current
    (f)  :setting-not-on-the-layer
    (g)  :permission-fact-for-another-layer,
         :permission-fact-granted-in-another-layer,
         :permission-fact-not-beneath-the-acts-own
    then :ending-a-permission-not-decided, :store-key-retract-not-built,
         :changing-a-class-not-decided, :store-key-takes-one-write

  An act that makes its layer is held to (e) and (f) as any act is, with no
  rows: nothing is current in a layer that is not made. It is not held to
  (g) or to the last step: the permission that it writes is its own and not
  one beneath its own, and its class fact is the layer's first.

  What must hold: `standing` gave nil for the act."
  [gate-class act makes-the-layer? rows]
  (let [facts (:facts act)]
    (or (some #(replaced-fact-fault gate-class act rows %) facts)
        (some #(setting-fault act %) facts)
        (when-not makes-the-layer?
          (or (some #(permission-fact-fault act %) facts)
              (some #(store-key-fault gate-class act rows %) facts))))))

;; -------------------------------------------------------------------- seals

(defn- lock-in-this-record?
  "Takes the gate's class, an act and a lock id. Gives true when the lock id
  names this layer and this class. Only such a lock can have a row in this
  gate's record of this layer, so only then is a row read."
  [gate-class act lock-id]
  (let [[lock-layer lock-class _] (first lock-id)]
    (and (= (:layer act) lock-layer) (= gate-class lock-class))))

(defn lock-ranges
  "Takes the gate's class and an act. Gives the ranges of lock rows that
  `seals` needs, each as [first key, last key], both in the range: one range
  for each request whose locks the act names, from the least to the greatest
  index that the act uses. A lock id of another layer or of the other class
  gives no range, and so costs no read. Empty for an act with no sealed
  value."
  [gate-class act]
  (let [layer (:layer act)]
    (->> (:facts act)
         (keep :lock-id)
         (filter #(lock-in-this-record? gate-class act %))
         (group-by (fn [[[_ _ request-uuid] _]] request-uuid))
         (mapv (fn [[request-uuid lock-ids]]
                 (let [indexes (map second lock-ids)]
                   [(record/lock-key layer request-uuid (reduce min indexes))
                    (record/lock-key layer request-uuid (reduce max indexes))]))))))

(defn- seal-fault
  "The check of the seal for one fact. Gives a word when the fact is sealed
  and: its lock is not one that the store made for this layer; the lock
  already seals an accepted value; the lock's way is not one that the code
  knows; or the lock does not open the sealed bytes at this fact's place.
  The plain bytes that the lock opens are dropped at once."
  [gate-class act lock-rows fact]
  (when-let [lock-id (:lock-id fact)]
    (let [layer (:layer act)
          [[_ _ request-uuid] index] lock-id
          row (when (lock-in-this-record? gate-class act lock-id)
                (get lock-rows (record/lock-key layer request-uuid index)))]
      (cond
        (nil? row) :lock-not-made-for-this-layer
        (record/seals-a-fact? row) :lock-already-seals-a-value
        (not (seal/known-way? (:way row))) :lock-way-not-known
        (not (seal/opens? (record/lock-of row) [layer (:e fact) (:k fact)] (:sealed fact))) :lock-does-not-open-the-value
        :else nil))))

(defn seals
  "The check of the seals, the last and the most costly. Takes the gate's
  class; an act; and the lock rows that `lock-ranges` named, as a map from a
  lock's key to its row. Gives nil, or for the first sealed fact that fails,
  in the act's order, its word:

    :lock-not-made-for-this-layer  the lock id names another layer or the
                                   other class, or the record holds no such
                                   lock
    :lock-already-seals-a-value    the lock's row names a fact
    :lock-way-not-known            the row's way is not one that the code
                                   knows. No lock of this build can earn it
    :lock-does-not-open-the-value  the lock does not open the bytes at this
                                   fact's place

  It opens each sealed value once and keeps nothing of the plain value. It
  does not check that what a lock opens is a value of the form. What must
  hold: the act passed the declaration, so no two facts are under one lock."
  [gate-class act lock-rows]
  (some #(seal-fault gate-class act lock-rows %) (:facts act)))

;; ------------------------------------------------------ the stamp, the writes

(defn stamp-of
  "The stamp of one decision. Takes the gate's clock now, in milliseconds;
  the task's last stamp, or nil; the act, or nil for a thing that the
  declaration refused; and the word of the refusal, or nil. Gives the stamp:
  later than the task's last stamp, and later than each stamp that the act
  carries. The one exception is what the 10-second rule is for: an act
  refused :carried-stamp-too-far-ahead gets an ordinary stamp, which is not
  later than the stamp that made it be refused."
  [now-millis last-stamp act refused]
  (stamp/next-stamp now-millis
                    last-stamp
                    (when (and act (not= :carried-stamp-too-far-ahead refused))
                      (stamp/greatest-carried act))))

(defn- accept-writes
  "The rows that an accept writes, beside the answer and the last stamp. In
  the act's order: the row of each fact; for each sealed fact, its lock's
  row again, with the bytes and the way as read and the fact that it now
  seals; for a request for n locks, n keys at which a new lock is to be
  made."
  [act act-uuid stamp lock-rows]
  (let [layer (:layer act)]
    (into []
          (comp
           (map-indexed
            (fn [index fact]
              (let [[[_ _ request-uuid] lock-index] (:lock-id fact)]
                (concat
                 [[:fact (record/place-key layer (:e fact) (:k fact)) (record/fact-row fact act-uuid index stamp)]]
                 (when (:lock-id fact)
                   (let [lock-key (record/lock-key layer request-uuid lock-index)]
                     [[:lock lock-key (record/lock-row-sealing (get lock-rows lock-key) act-uuid index)]]))
                 (when (and (= :lock-request (:k fact)) (some? (:v fact)))
                   (map (fn [i] [:new-lock (record/lock-key layer act-uuid i) nil])
                        (range (:v fact))))))))
           cat)
          (:facts act))))

(defn writes
  "Each row that one decision writes, as data, for a gate to write in one
  event. Takes the act-id under which the thing arrived; the act, or nil for
  a thing that the declaration refused; the word of the refusal, or nil for
  an accept; the stamp of the decision; the digest of the thing, 32 bytes;
  and the lock rows that were read. Gives a vector of [which key row]:

    [:fact key row]        the row of a fact's place
    [:lock key row]        the row of a lock that the act uses, written
                           again whole, now naming the fact that it seals
    [:new-lock key nil]    a key at which the gate makes a new lock. The
                           bytes are made by the gate at the write, never
                           here and never on a depot
    [:answer key row]      the answer under the act-id
    [:last-stamp nil n]    the task's last stamp

  A refusal writes the answer and the last stamp, and nothing else: a lock
  that a refused act named stays as it was, free. An accept writes all of
  them. Every row is whole, at a fixed key, so each write needs no read and
  a second run of the same decision would write the same keys.

  What must hold: the thing is no stray; for an accept, `seals` gave nil
  with these lock rows."
  [act-id act refused stamp digest lock-rows]
  (let [[layer _ act-uuid] act-id]
    (conj (if refused [] (accept-writes act act-uuid stamp lock-rows))
          [:answer (record/answer-key layer act-uuid) (record/answer-row refused stamp digest)]
          [:last-stamp nil (long stamp)])))
