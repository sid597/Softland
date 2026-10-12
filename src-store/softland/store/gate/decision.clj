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
  takes the depot of the gate that owns the record, :streaming or
  :microbatch.

  The checks, in the order in which a gate makes them. The order of the
  checks is the order of the reasons: when an act has two faults, the check
  that comes first gives the recorded word. Each check gives nil, or the
  word of its first fault. A check that examines facts takes them in the
  act's order.

    early      `refused-early`: the declaration's own word, or the 10-second
               rule on a carried stamp. Nothing of the record is read.
    standing   `standing`: the layer's depot is this gate's (c); the layer
               is made, or the act is a right making act; the act's
               permission stands for its `:who` (d); and that permission
               allows acting: an act needs \"act\".
    places     `places`: each replaced fact is the current fact of its
               place (e); no setting key on an entity other than the layer
               (f); a permission fact writes a permission straight beneath
               the act's own (g); who may write a key that takes a second
               write (`who-may-write-fault`); and what a store key does not
               take in this build.
    seals      `opened`: each sealed value's lock is one that the store made
               for this layer, seals nothing yet, is of a known way, and
               opens the value at its place. It gives the plain bytes that
               each lock opened.

  `places-needed` and `lock-ranges` say which rows the last two need.
  `stamp-of` gives the stamp of the decision. `writes` gives each row that
  the decision writes.

  What an accept reads after the last check, for the order by value. No
  check reads a layer's marked keys, so a refused act pays nothing for them.
  `reads-marked-keys?` says whether an accepted act needs the row at
  `marked-keys-place`; `since-by-place` gives, from that row, the places of
  the act whose key is marked, each with the key's stamp; and
  `rows-before-needed` names the rows before that are still to be read.
  `writes` then gives, beside the fact's row, the rows of the facts by
  value: the entry of the new value, and the entry of the value before
  taken out. The streaming gate calls the four; the microbatch gate reads
  the marks on a place's home task and hands `writes` the same things
  (`softland.store.gate.batch`).

  How two of the checks rest on how a row can come to be, and read less
  than their words name.

  (c) costs no read for a layer that is made here. A gate's record holds
  rows only for layers of its depot: a row is written only by an accept; an
  accept happens in a layer that is made here, or makes it; a making act is
  right only for the gate of the depot that the layer starts with; and no
  act of this build changes a depot. So a row at the permission's place, or
  at the layer's depot place, says that the layer is this gate's.

  (d) is one read. A permission fact sits on its holder, so the place of
  `:who`'s permission holds the act's permission or the act's permission
  does not stand. \"Each permission above it stands\" is not read: a
  permission row is written only by a right making act, which has nothing
  above it, or by a give that passed (d) and (g), whose own permission stood
  and is exactly the one above; a permission id holds its whole chain; and
  nothing in this build ends a permission. The room for a permission that
  has ended is the place itself: an end would be a later fact on it.

  State and effects. None. `opened` opens each sealed value once and gives
  the plain bytes to `writes`, which puts them in the fact's row. They are
  nowhere else: the bytes of a value that was opened before a later fact of
  its act failed are dropped with the decision.

  Not here. Check (a), the answer under the act-id, and the test for a
  stray: the gate makes them before any of these. Whether the other gate's
  record holds a layer: the gate reads it and gives the answer to
  `standing`."
  (:require [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.gate.first-act :as first-act]
            [softland.store.record.rows :as record]
            [softland.store.door.seal :as seal]
            [softland.store.gate.stamp :as stamp]))

;; -------------------------------------------------------------------- early

(defn refused-early
  "The checks that read nothing of the record. Takes the gate's clock, in
  milliseconds: the streaming gate's reading for the decision, or the
  microbatch gate's clock of the batch, which is the same on every attempt
  of the batch; and what `softland.store.door.declaration/check` gave for the
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
  standing of an act. One place for a holder in a layer, so one permission:
  that is kept as built and not decided, and this key is one of the two
  places where the other answer would be made
  (`softland.store.door.declaration/store-keys`)."
  [act]
  (record/place-key (:layer act) (:who act) :softland.store/permission))

(defn depot-place
  "Takes an act. Gives the key of the place of its layer's depot: the layer
  as entity, under `:softland.store/depot`. A gate reads it only when the
  place of the permission is empty."
  [act]
  (record/place-key (:layer act) (:layer act) :softland.store/depot))

(defn makes-the-layer?
  "Takes the gate's depot, an act, the row at the place of `:who`'s
  permission, the row at the place of the layer's depot, and whether the
  other gate's record holds the layer. Gives true when the act is the first
  act of its layer: both rows are nil, the other gate's record does not hold
  the layer, and the act has the form of a making act for this gate. What
  must hold: when the permission's row is nil, the depot's row was read."
  [gate-depot act permission-row depot-row other-gate-holds-layer?]
  (boolean (and (nil? permission-row)
                (nil? depot-row)
                (not other-gate-holds-layer?)
                (first-act/making-act? gate-depot act))))

(defn standing
  "The standing of an act: checks (c) and (d), what the act's permission
  allows, and the first act of a layer. Takes the gate's depot; an act; the
  row at the place of `:who`'s permission; the row at the place of the
  layer's depot, which is read only when the first is nil; and whether the
  other gate's record holds the layer, which is asked only when both are
  nil. Gives nil when the act's permission stands and allows acting, or
  when the act is a right making act for a layer that is not made
  (`makes-the-layer?` tells the two apart). Else a word:

    :layer-belongs-to-the-other-gate  the layer is made, by the other gate
    :layer-not-made                   the record holds nothing for the
                                      layer, and the act is no right making
                                      act
    :who-not-the-permissions-holder   the layer is made here, and the holder
                                      that the act's permission names is not
                                      the act's `:who`
    :permission-does-not-stand        the layer is made here, and the record
                                      does not hold the act's permission for
                                      its holder
    :permission-does-not-allow-acting the record holds the act's permission,
                                      and it says `:see`

  The last word costs no read: what a permission allows is a slot of the
  permission that the act names (`softland.store.act/allows`), and the row
  was just compared with it, slot and all. So nobody raises her right by
  naming it: a holder of `:see` who names her permission without the slot,
  or with `:see-and-act`, names bytes that the row does not hold, and gets
  `:permission-does-not-stand`. The word comes before every check of a
  place. A holder of `:see` cannot give either: a give is an act. A
  permission's place takes one write and nothing ends a permission, so
  \"see\" stays \"see\"; that is kept, and not decided
  (`store-key-fault`)."
  [gate-depot act permission-row depot-row other-gate-holds-layer?]
  (let [permission (:permission act)]
    (cond
      (record/holds-value? permission-row permission)
      (when (= :see (act/allows permission))
        :permission-does-not-allow-acting)

      (or permission-row depot-row)
      (if (= (:who act) (nth permission 0))
        :permission-does-not-stand
        :who-not-the-permissions-holder)

      other-gate-holds-layer?
      :layer-belongs-to-the-other-gate

      (first-act/making-act? gate-depot act)
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
  something to replace, and of each fact under `:softland.store/permission`.
  None for an act that makes its layer: the record holds nothing of a layer
  that is not made, and the two facts of a making act are not held to the
  checks that read a place. A fact that names nothing to replace needs no
  row, and so can never be stale."
  [act makes-the-layer?]
  (if makes-the-layer?
    []
    (into []
          (comp (filter (fn [fact] (or (:replaces fact) (= :softland.store/permission (:k fact)))))
                (map (fn [fact] (place-of act fact))))
          (:facts act))))

(defn- replaced-fact-fault
  "Check (e) for one fact. Gives the word when the fact names something to
  replace and that is not the current fact of its place: the place is empty,
  another fact wrote its row, or the fact named is of another layer or
  depot."
  [gate-depot act rows fact]
  (when-let [replaces (:replaces fact)]
    (when-not (record/written-by? gate-depot (:layer act) (get rows (place-of act fact)) replaces)
      :replaced-fact-not-current)))

(defn- setting-fault
  "Check (f) for one fact. Gives the word when the fact is under a setting
  key on an entity that is not the act's layer."
  [act fact]
  (when (and (contains? declaration/setting-keys (:k fact))
             (not= (:e fact) (:layer act)))
    :setting-not-on-the-layer))

(defn- permission-fact-fault
  "Check (g) for one fact. Gives a word when the fact is under
  `:softland.store/permission`, has a value, and that permission is not one
  straight beneath the act's own, in the act's layer: its layer slot is not
  the act's layer; its granted-in slot is not the act's layer; or the
  permission above it is not exactly the one that the act acts under. A
  permission with nothing above it earns the third word."
  [act fact]
  (let [given (:v fact)]
    (when (and (= :softland.store/permission (:k fact)) (some? given))
      (cond
        (not= (:layer act) (nth given 1)) :permission-fact-for-another-layer
        (not= (:layer act) (nth given 2)) :permission-fact-granted-in-another-layer
        (not= (:permission act) (nth given 3 nil)) :permission-fact-not-beneath-the-acts-own
        :else nil))))

(defn who-may-write-fault
  "Who may write a layer's setting, the list of what it builds on, and its
  marked keys: the check, for one fact under one of
  `softland.store.door.declaration/second-write-keys`. Takes the act and the
  fact. Gives nil: whoever may act in the layer may write each of the three,
  and the act has passed its standing.

  Not decided. Sid's word for who writes a layer's setting is \"the owner\",
  and no layer names an owner as a fact. The answer here is kept only so
  that the build can run (BUILD-reads.md, \"Not decided\"), as for a layer's
  depot. What it lets happen: any holder of \"see and act\" in a layer can
  set that layer to `:everyone`, any such holder in the base can set the
  base to `:members`, and any of them can write the list and the marks. The
  other answer is the body of this function: it has the act, with its
  `:who` and its permission, and the fact; a word that it gives is recorded
  as the refusal, after check (g)."
  [_act _fact]
  nil)

(defn- store-key-fault
  "What a store key does not take in this build, for one fact. A permission's
  place and a layer's depot take one write. The three keys of
  `softland.store.door.declaration/second-write-keys`, who may see a layer,
  what it builds on and its marked keys, take a second write and any
  later one: such a fact earns no word here, with `:replaces` or without. A
  retract under any store key is refused. Gives a word for: a permission
  fact with no value, since who may end a permission is not decided; a fact
  with no value under any other store key; a depot fact in a layer that is
  made, with the other depot (a change of depot is not decided) or with the
  layer's own (its place holds one); a permission fact on a place that holds
  a row, the same permission or another. A request fact with a value earns
  none: its place is new by its form. A fact under any other key earns none.
  What must hold: the layer is made by this gate, so its depot is the
  gate's.

  The line that gives `:store-key-takes-one-write` for a permission is why a
  permission that says `:see` stays `:see`: to raise one, or to end one, is
  not built and not decided."
  [gate-depot act rows fact]
  (let [k (:k fact)
        retract? (nil? (:v fact))]
    (cond
      (and (= :softland.store/permission k) retract?) :ending-a-permission-not-decided
      (and (declaration/store-key? k) retract?) :store-key-retract-not-built
      (and (= :softland.store/depot k) (not= gate-depot (:v fact))) :changing-a-depot-not-decided
      (= :softland.store/depot k) :store-key-takes-one-write
      (and (= :softland.store/permission k) (get rows (place-of act fact))) :store-key-takes-one-write
      :else nil)))

(defn places
  "The checks of the places. Takes the gate's depot; an act; whether it makes
  its layer; and the rows of the places that `places-needed` named, as a map
  from key to row, nil or absent for an empty place. Gives nil, or the word
  of the first check that fails, in this order:

    (e)  :replaced-fact-not-current
    (f)  :setting-not-on-the-layer
    (g)  :permission-fact-for-another-layer,
         :permission-fact-granted-in-another-layer,
         :permission-fact-not-beneath-the-acts-own
    then what `who-may-write-fault` gives for a fact under a key that takes
         a second write: no word in this build
    then :ending-a-permission-not-decided, :store-key-retract-not-built,
         :changing-a-depot-not-decided, :store-key-takes-one-write

  An act that makes its layer is held to (e) and (f) as any act is, with no
  rows: nothing is current in a layer that is not made. It is not held to
  (g) or to the last step: the permission that it writes is its own and not
  one beneath its own, and its depot fact is the layer's first.

  What must hold: `standing` gave nil for the act."
  [gate-depot act makes-the-layer? rows]
  (let [facts (:facts act)]
    (or (some #(replaced-fact-fault gate-depot act rows %) facts)
        (some #(setting-fault act %) facts)
        (when-not makes-the-layer?
          (or (some #(permission-fact-fault act %) facts)
              (some #(when (contains? declaration/second-write-keys (:k %))
                       (who-may-write-fault act %))
                    facts)
              (some #(store-key-fault gate-depot act rows %) facts))))))

;; ---------------------------------------------- the marked keys of the layer

(defn marked-keys-place
  "Takes an act. Gives the key of the place of its layer's marked keys: the
  layer as entity, under `:softland.store/marked-keys`. Its row says which
  keys the layer marks, and since which stamp each
  (`softland.store.record.rows/marked-keys-of`)."
  [act]
  (record/place-key (:layer act) (:layer act) :softland.store/marked-keys))

(defn reads-marked-keys?
  "Takes an act, or nil. Gives true when a gate that accepts the act needs
  the row at `marked-keys-place`: the act holds a fact under a plain key,
  which may be a marked one, or a fact under `:softland.store/marked-keys`,
  whose row before gives the stamps that its keys keep. False for a request
  for locks, a give, a making act, a setting and a list: none of them reads
  the marked keys."
  [act]
  (boolean (some (fn [fact]
                   (or (= :softland.store/marked-keys (:k fact))
                       (not (declaration/store-key? (:k fact)))))
                 (:facts act))))

(defn since-by-place
  "Takes an act and the row at its layer's marked-keys place, or nil. Gives a
  map from the key of each place of the act whose key the layer marks to
  that key's stamp: the stamp since which the order of the key is level.
  Empty when the layer marks no key of the act. A retract is in it too: it
  takes the entry of the value before out. The act is decided with the
  marks as they were before it, so a fact of the act that writes the mark
  of its own key is not in it."
  [act marks-row]
  (let [marked (record/marked-keys-of marks-row)]
    (if (empty? marked)
      {}
      (into {}
            (keep (fn [fact]
                    (when-let [since (get marked (:k fact))]
                      [(place-of act fact) since])))
            (:facts act)))))

(defn rows-before-needed
  "Takes what `since-by-place` gave, and the rows of the places that were
  read already, as a map from key to row in which a key that was read is
  there also when its place is empty. Gives the keys of the places under a
  marked key whose row is still to be read, in the order of the keys. The
  row before of such a place gives the entry to take out of the order by
  value: that entry's key holds the digest of the value before, so it
  cannot be found without the row."
  [since-by-place place-rows]
  (into [] (remove #(contains? place-rows %)) (sort (keys since-by-place))))

;; -------------------------------------------------------------------- seals

(defn- lock-in-this-record?
  "Takes the gate's depot, an act and a lock id. Gives true when the lock id
  names this layer and this depot. Only such a lock can have a row in this
  gate's record of this layer, so only then is a row read."
  [gate-depot act lock-id]
  (let [[lock-layer lock-depot _] (first lock-id)]
    (and (= (:layer act) lock-layer) (= gate-depot lock-depot))))

(defn lock-ranges
  "Takes the gate's depot and an act. Gives the ranges of lock rows that
  `opened` needs, each as [first key, last key], both in the range: one range
  for each request whose locks the act names, from the least to the greatest
  index that the act uses. A lock id of another layer or of the other depot
  gives no range, and so costs no read. Empty for an act with no sealed
  value."
  [gate-depot act]
  (let [layer (:layer act)]
    (->> (:facts act)
         (keep :lock-id)
         (filter #(lock-in-this-record? gate-depot act %))
         (group-by (fn [[[_ _ request-uuid] _]] request-uuid))
         (mapv (fn [[request-uuid lock-ids]]
                 (let [indexes (map second lock-ids)]
                   [(record/lock-key layer request-uuid (reduce min indexes))
                    (record/lock-key layer request-uuid (reduce max indexes))]))))))

(defn- opened-fact
  "The check of the seal for one fact. Gives {:plain bytes}, the plain bytes
  that the fact's lock opened, or {:plain nil} for a fact that is not
  sealed; or {:refused word} when the fact is sealed and: its lock is not
  one that the store made for this layer; the lock already seals an accepted
  value; the lock's way is not one that the code knows; or the lock does not
  open the sealed bytes at this fact's place. The four tests are made in
  that order, and the value is opened once."
  [gate-depot act lock-rows fact]
  (if-let [lock-id (:lock-id fact)]
    (let [layer (:layer act)
          [[_ _ request-uuid] index] lock-id
          row (when (lock-in-this-record? gate-depot act lock-id)
                (get lock-rows (record/lock-key layer request-uuid index)))]
      (cond
        (nil? row) {:refused :lock-not-made-for-this-layer}
        (record/seals-a-fact? row) {:refused :lock-already-seals-a-value}
        (not (seal/known-way? (:way row))) {:refused :lock-way-not-known}
        :else (if-let [plain (seal/open (record/lock-of row) [layer (:e fact) (:k fact)] (:sealed fact))]
                {:plain plain}
                {:refused :lock-does-not-open-the-value})))
    {:plain nil}))

(defn opened
  "The check of the seals, the last and the most costly, and the opening of
  each sealed value. Takes the gate's depot; an act; and the lock rows that
  `lock-ranges` named, as a map from a lock's key to its row. Gives
  {:refused word} for the first sealed fact that fails, in the act's order:

    :lock-not-made-for-this-layer  the lock id names another layer or the
                                   other depot, or the record holds no such
                                   lock
    :lock-already-seals-a-value    the lock's row names a fact
    :lock-way-not-known            the row's way is not one that the code
                                   knows. No lock of this build can earn it
    :lock-does-not-open-the-value  the lock does not open the bytes at this
                                   fact's place

  or {:plain [...]}, with one entry for each fact in the act's order: the
  plain bytes of a sealed fact, nil for any other. `writes` hands entry i to
  the row of fact i, so a gate keeps the plain bytes of each value that it
  accepts, and each value is opened once for one decision. The bytes of a
  value that was opened before a later fact failed are dropped here.

  It does not check that what a lock opens is a value of the form: a gate
  keeps the bytes whatever they are, and
  `softland.store.record.rows/read-value` tells a reader. What must hold:
  the act passed the declaration, so no two facts are under one lock."
  [gate-depot act lock-rows]
  (reduce (fn [{:keys [plain]} fact]
            (let [got (opened-fact gate-depot act lock-rows fact)]
              (if (:refused got)
                (reduced got)
                {:plain (conj plain (:plain got))})))
          {:plain []}
          (:facts act)))

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

(defn- level-since-after
  "The stamps of a layer's marked keys after a write of the marks. Takes the
  row at the marked-keys place as it was before the write, or nil; the set
  of keys that the new fact marks; and the stamp that a newly marked key
  gets. Gives a map from each key of the new set to its stamp: the stamp
  that the row before gave it, for a key that the row before held; the new
  stamp for any other. So a key that stays marked keeps its order, a key
  whose mark is taken off is not in the map, and a key that is marked again
  later gets another stamp and with it other keys in the order by value."
  [row-before new-keys level-from]
  (let [before (record/marked-keys-of row-before)]
    (into {} (map (fn [k] [k (get before k level-from)])) new-keys)))

(defn- by-value-writes
  "The rows of the facts by value for one fact under a marked key. Takes the
  act; the fact; the plain bytes of the value that the fact's row will hold
  (`softland.store.record.rows/plain-bytes-of`), or nil for a
  retract; the row before at the fact's place, or nil; the stamp since which
  the fact's key is marked; and the entry to put for the fact
  (`softland.store.record.rows/by-value-row`). Gives zero, one or two
  [:by-value key row]:

    out  [:by-value key nil], the entry of the value before, only when the
         row before has a value, its stamp is later than the key's stamp,
         and its digest is not the digest of the new value. A row before
         whose stamp is not later was written before the mark, or with it,
         and has no entry
    in   [:by-value key entry], the entry of the new value. None for a
         retract. A fact that is written again with the same value has one
         digest: its entry is written over, and nothing goes out

  Every value is kept, whatever it is, by the digest of its bytes: a gate
  does not judge what an id is. What a marked key may hold is not decided
  (the queue's K5); this function is the one place where \"kept in no run\"
  would be made. Cost: one SHA-256 over the new value's bytes and one over
  the bytes of the value before."
  [act fact value-bytes row-before since entry]
  (let [digest-now (when value-bytes (record/value-digest value-bytes))
        before (record/plain-bytes-of row-before)
        digest-before (when (and before (< (long since) (long (:stamp row-before))))
                        (record/value-digest before))
        key-of (fn [digest] (record/by-value-key (:layer act) (:k fact) since digest (:e fact)))]
    (cond-> []
      (and digest-before (not= digest-before digest-now))
      (conj [:by-value (key-of digest-before) nil])

      digest-now
      (conj [:by-value (key-of digest-now) entry]))))

(defn- accept-writes
  "The rows that an accept writes, beside the answer and the last stamp. In
  the act's order, for each fact: its row, with the plain bytes that its
  lock opened, who wrote it and the permission that the act named, and for
  the fact under `:softland.store/marked-keys` the stamps of
  `level-since-after`; for a fact under a marked key, the rows of
  `by-value-writes`; for a sealed fact, its lock's row again, with the
  bytes and the way as read and the fact that it now seals; for a request
  for n locks, n keys at which a new lock is to be made. `in-hand` is what
  `writes` takes."
  [act act-uuid stamp {:keys [lock-rows plain since-by-place place-rows level-from]}]
  (let [layer (:layer act)]
    (into []
          (comp
           (map-indexed
            (fn [index fact]
              (let [[[_ _ request-uuid] lock-index] (:lock-id fact)
                    place (record/place-key layer (:e fact) (:k fact))
                    row (cond-> (record/fact-row act fact (nth plain index nil) act-uuid index stamp)
                          (and (= :softland.store/marked-keys (:k fact)) (some? (:v fact)))
                          (record/with-level-since
                            (level-since-after (get place-rows place) (:v fact) level-from)))
                    since (get since-by-place place)]
                (concat
                 [[:fact place row]]
                 (when since
                   (by-value-writes act fact (record/plain-bytes-of row) (get place-rows place) since
                                    (record/by-value-row act act-uuid index stamp)))
                 (when (:lock-id fact)
                   (let [lock-key (record/lock-key layer request-uuid lock-index)]
                     [[:lock lock-key (record/lock-row-sealing (get lock-rows lock-key) act-uuid index)]]))
                 (when (and (= :softland.store/lock-request (:k fact)) (some? (:v fact)))
                   (map (fn [i] [:new-lock (record/lock-key layer act-uuid i) nil])
                        (range (:v fact))))))))
           cat)
          (:facts act))))

(defn writes
  "Each row that one decision writes, as data, for a gate to write in one
  event. Takes the act-id under which the thing arrived; the act, or nil for
  a thing that the declaration refused; the word of the refusal, or nil for
  an accept; the stamp of the decision; the digest of the thing, 32 bytes;
  and what the gate has in hand for an accept, a map that is not read for a
  refusal:

    :lock-rows       the lock rows that were read
    :plain           what `opened` gave with those rows: for each fact in
                     the act's order, the plain bytes of its value, or nil
    :since-by-place  for each place of the act whose key the layer marks,
                     the key's stamp (`since-by-place`); empty or absent
                     when none is
    :place-rows      the rows of the act's places that were read, a map
                     from key to row: the row before of each place of
                     `:since-by-place`, and of the marked-keys place when
                     the act writes it
    :level-from      the stamp that a newly marked key gets: the stamp of
                     the act in the streaming gate; the last stamp of the
                     batch in the microbatch gate, where a mark holds from
                     the batch after

  Gives a vector of [which key row]:

    [:fact key row]        the row of a fact's place
    [:by-value key row]    a row of the facts by value, for a fact under a
                           marked key. A row that is nil says that the key
                           is to be taken out
    [:lock key row]        the row of a lock that the act uses, written
                           again whole, now naming the fact that it seals
    [:new-lock key nil]    a key at which the gate makes a new lock. The
                           bytes are made by the gate at the write, never
                           here and never on a depot
    [:answer key row]      the answer under the act-id
    [:last-stamp nil n]    the task's last stamp

  A refusal writes the answer and the last stamp, and nothing else: a lock
  that a refused act named stays as it was, free. An accept writes all of
  them. Every row is whole, at a fixed key, and a key that is taken out is
  fixed too; so each write needs no read, and a second run of the same
  decision would write the same keys.

  What must hold: the thing is no stray; for an accept, `opened` gave
  `:plain` with these lock rows, and `:place-rows` holds what the list
  above says."
  [act-id act refused stamp digest in-hand]
  (let [[layer _ act-uuid] act-id]
    (conj (if refused [] (accept-writes act act-uuid stamp in-hand))
          [:answer (record/answer-key layer act-uuid) (record/answer-row refused stamp digest)]
          [:last-stamp nil (long stamp)])))
