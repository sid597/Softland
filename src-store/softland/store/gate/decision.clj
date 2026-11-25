(ns softland.store.gate.decision
  "The checks of the decision of one act, after the door's declaration, as
  functions of an act and of rows; and the rows that a decision writes, as
  data. Pure code. No function reads a PState, a clock or a random source:
  a gate reads the rows, calls these, and writes what they give. So the
  same checks serve each gate, and a function gives the same answer when
  Rama runs an event again.

  An act is an act in its one form on arrival
  (`softland.store.door.declaration/check`). A row is a row of the record
  (`softland.store.record.rows`), or nil for a key that holds none. A
  gate's depot is `:streaming` or `:microbatch`.

  Before these, the walk makes the test for a stray and check (a), the
  answer under the act-id. The order of the checks is the order of the
  reasons: when an act has two faults, the check that comes first gives the
  recorded word. A check that examines facts takes them in the act's order.

  No check reads a row of the other gate's record, the time on the act, or
  a stamp that the act carries. No check reads a lane's marked keys, so a
  refused act pays nothing for them.

  Two checks rest on how a row can come to be, and read less than their
  words name.

  (c), the lane's depot is this gate's, costs no read for a lane that is
  made here. A gate's record holds rows only for lanes of its depot: a row
  is written only by an accept; an accept happens in a lane that is made
  here, or makes it; a making act is right only for the gate of the depot
  that the lane starts with; and no act changes a depot. So a row at the
  permission's place, or at the lane's depot place, says that the lane is
  this gate's.

  (d), the act's permission stands, is one read for each permission of the
  act's chain. A permission fact sits on what it covers
  (`softland.store.gate.chain/place`), so the place that the act's
  permission names holds that permission or the permission does not stand.
  The places of the permissions above it are read from the act alone, and
  only when the first row holds the act's permission."
  (:require [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.door.text :as text]
            [softland.store.gate.chain :as chain]
            [softland.store.gate.first-act :as first-act]
            [softland.store.record.rows :as record]))

;; -------------------------------------------------------- the time on the act

(defn time-on
  "Milliseconds since 1970: what the door wrote from the clock of its
  server, or whatever else appended the thing wrote there. This code knows
  only that it is a whole number not below zero. Nil for a thing with no
  usable time, which the declaration refuses `:no-door-time`. A label
  beside the stamp: no check reads it, and no stamp holds it."
  [thing]
  (declaration/usable-time thing))

;; ----------------------------------------------------------------- standing

(defn permission-place
  {:softland/whose-call
   [{:state :not-decided
     :what "One place for a holder's permission for the lane is kept as built
            and not decided. This key is one of the two places where the
            other answer would be made
            (`softland.store.door.declaration/store-keys`)."}]}
  [act]
  (chain/place (:lane act) (:permission act)))

(defn hex-for-its-permission
  "The digest of the act as its maker made it: in its one form, without its
  act-id, its depot, its time and its permission. A maker makes the same
  text with `softland.store.door.digest/hex-for-a-yes`, for every act that
  the declaration takes, so the two agree under any act-id that she sends
  it under. What must hold: the declaration took the thing. The thing is
  read and not the act, whose facts under a plain key hold their value as
  text alone. nil when the act's permission is no yes for one act, so that
  no other act pays for the digest."
  [thing act]
  (when (= :act (first (act/covers (:permission act))))
    (digest/hex-of-the-act thing)))

(defn used-up?
  "A use is an end that names the yes itself as the permission of its act:
  an act taken under that yes wrote it (`use-of-its-yes`), also when that
  act is the yes's own end. An end by the yes's giver, one above the giver,
  or its holder under her permission for the lane names another permission:
  an end and no use."
  [row permission]
  (boolean (and (record/ended? row)
                (= (:permission row) (text/text permission)))))

(defn- use-of-another-yes?
  "True for an end that an act wrote under another yes on the same id. A
  yes's place takes one give, so the yes that was used there is the one
  yes ever given there, and the one named was never given: its act is
  refused `:permission-does-not-stand`, and not told that a permission of
  its was ended. False for an end that the yes's giver, one above the giver
  or its holder wrote, which names a permission for the lane: that end does
  not say which yes it ended."
  [row permission]
  (let [[kind yes] (act/covers permission)]
    (boolean (and (= :act kind)
                  (record/ended? row)
                  (not (used-up? row permission))
                  (let [[named-kind named-yes] (act/covers (record/permission-of row))]
                    (and (= :act named-kind) (= yes named-yes)))))))

(defn depot-place
  [act]
  (record/place-key (:lane act) (:lane act) :softland.store/depot))

(defn- lane-is-the-other-gates?
  [gate-depot act]
  (first-act/starts-with-the-other-gates-depot? gate-depot (:lane act)))

(defn makes-the-lane?
  "`standing` gives nil for such an act and for an act whose permission
  stands; this tells the two apart. What must hold: when the permission's
  row is nil, the depot's row was read."
  [gate-depot act permission-row depot-row]
  (boolean (and (nil? permission-row)
                (nil? depot-row)
                (not (lane-is-the-other-gates? gate-depot act))
                (first-act/making-act? gate-depot act))))

(defn chain-needed
  "Empty when the row does not hold the act's permission: the act is then
  refused with no read more."
  [act permission-row]
  (if (record/holds-value? permission-row (:permission act))
    (chain/places-above (:lane act) (:permission act))
    []))

(defn standing
  "Checks (c) and (d), what the act's permission allows and covers, and the
  first act of a lane. Gives nil, or the first word that holds.

  What must hold: `depot-row` was read when `permission-row` is nil;
  `chain-rows` is a map from key to row for the places that `chain-needed`
  named; `hex-of-the-act` is what `hex-for-its-permission` gave.

  The other gate. Whether the lane is the other gate's counts only when
  both rows are nil, and no table of the other gate is read for it. So an
  act for a lane that starts with the other depot, of which this record
  holds nothing, gets `:lane-belongs-to-the-other-gate` whether or not the
  other gate has made the lane, and never `:lane-not-made`. The word cannot
  follow the other gate's record: it would follow an order across two
  depots, which no second run gives again.

  The words of another run and of another act come before what the place
  holds: such an act is refused with that word whatever the place of the
  permission holds, ended, used or empty
  (src/proposal/runtime-2026-11-21/store-piece-2/SPEC.md, R9, \"whatever
  else is right in it\"), as the statements of the generated histories
  order them. Each of the two reads the act alone, and the digest that the
  walk made of it, so neither needs a row.

  `:permission-above-ended` says that a permission above the act's own does
  not stand, or that one was given again after the act's own was given
  (`softland.store.gate.chain/stands?`). `:permission-does-not-stand` says
  that the record does not hold the act's permission at its place: the
  place is empty, holds another permission, or holds the use of another
  yes, so the one named was never given there.

  `:permission-does-not-allow-acting` costs no read: what a permission
  allows is a slot of the permission that the act names, and the row was
  just compared with it, slot and all. So nobody raises her right by naming
  it: a holder of `:see` who names her permission without the slot, or with
  `:see-and-act`, names bytes that the row does not hold, and gets
  `:permission-does-not-stand`. A holder of `:see` cannot give either: a
  give is an act, and so is an end, her own too. A permission that stands
  is not written over, so \"see\" becomes \"see and act\" by an end and a
  give after it (`give-over-fault`).

  What a permission covers. One for a run covers the acts that carry that
  run as their `:session`, of the permission's holder alone. A yes for one
  act covers the one act whose digest it names, the act as its maker made
  it, sent under any act-id of hers: so the act that is taken is the act
  that was said yes to, part for part, its who and its session among them.
  It covers that act once: the accept writes the yes's use at its place
  (`writes`), and from then the yes covers nothing, also the same act under
  another act-id. A try that is refused writes no use, and is sent again
  under a new act-id, since an act-id keeps its first answer. Two yeses for
  one act can stand at once, each on its own id, and each is used once. The
  session and the who are what the sending program said: a gate checks no
  sender.

  An act that was taken under a permission stays taken when the permission
  ends. An act that is decided after the end is refused, the holder's own
  and that of each holder beneath her. The end has a stamp in the lane's
  one order, so which acts came before it is the same at each run of the
  gates."
  {:softland/whose-call
   [{:state :not-decided
     :what "The word of an act for a lane of the other gate comes from the
            act alone: a session's rule, and no decision of Sid's. One half
            of the rule is forced by his line that the store comes out the
            same when it is run again: the word cannot follow the other
            gate's record, since a word that followed what the other gate had
            made by then would follow an order across two depots, which no
            second run gives again. The other half is the session's choice:
            which of the two words is then given,
            `:lane-belongs-to-the-other-gate`. `:lane-not-made` for every
            such act would hold his line as well."
     :source "a session, 14 October 2026"}]}
  [gate-depot act permission-row depot-row chain-rows hex-of-the-act]
  (let [permission (:permission act)
        [kind what hex] (act/covers permission)]
    (cond
      (not (or permission-row depot-row))
      (cond
        (lane-is-the-other-gates? gate-depot act) :lane-belongs-to-the-other-gate
        (first-act/making-act? gate-depot act) nil
        :else :lane-not-made)

      (not= (:who act) (nth permission 0))
      :who-not-the-permissions-holder

      (and (= :run kind) (not= what (:session act)))
      :permission-for-another-run

      (and (= :act kind) (not= hex hex-of-the-act))
      :act-not-the-one-permitted

      (record/holds-value? permission-row permission)
      (cond
        (not (chain/stands? (:lane act) permission
                            (assoc chain-rows (chain/place (:lane act) permission) permission-row)))
        :permission-above-ended

        (= :see (act/allows permission))
        :permission-does-not-allow-acting)

      (and (= :act kind) (used-up? permission-row permission))
      :permission-used-up

      (and (record/ended? permission-row) (not (use-of-another-yes? permission-row permission)))
      :permission-ended

      :else
      :permission-does-not-stand)))

;; ------------------------------------------------------------------- places

(defn- place-of
  [act fact]
  (record/place-key (:lane act) (:e fact) (:k fact)))

(defn places-needed
  "None for an act that makes its lane: the record holds nothing of a lane
  that is not made, and the two facts of a making act are not held to the
  checks that read a place. A fact that names nothing to replace needs no
  row, and so can never be stale."
  [act makes-the-lane?]
  (if makes-the-lane?
    []
    (into []
          (comp (filter (fn [fact] (or (:replaces fact) (contains? declaration/permission-keys (:k fact)))))
                (map (fn [fact] (place-of act fact))))
          (:facts act))))

(defn- replaced-fact-fault
  "Check (e). Stale when the place is empty, another fact wrote its row, or
  the fact named is of another lane or depot."
  [gate-depot act rows fact]
  (when-let [replaces (:replaces fact)]
    (when-not (record/written-by? gate-depot (:lane act) (get rows (place-of act fact)) replaces)
      :replaced-fact-not-current)))

(defn- setting-fault
  "Check (f)."
  [act fact]
  (when (and (contains? declaration/setting-keys (:k fact))
             (not= (:e fact) (:lane act)))
    :setting-not-on-the-lane))

(defn- permission-fact-fault
  "Check (g), for a fact that gives a permission.

  A permission for a run or for an act gives nothing. What is given beneath
  a permission must cover no more than it does: else the holder of a
  permission for one run gives herself one for the lane beneath it, and a
  yes for one run is a yes for every run. Neither a lane nor a second run
  fits inside one run."
  {:softland/whose-call
   [{:state :not-decided
     :what "A permission for a run or for an act gives nothing. The other
            way, a run that may give, would be one more word in the sixth
            slot."
     :source "a session, 21 November 2026;
              src/proposal/runtime-2026-11-21/store-piece-2/SPEC.md, R10"}]}
  [act fact]
  (let [given (:v fact)]
    (when (and (contains? declaration/permission-keys (:k fact)) (some? given))
      (cond
        (some? (act/covers (:permission act))) :only-a-permission-for-the-lane-gives
        (not= (:lane act) (nth given 1)) :permission-fact-for-another-lane
        (not= (:lane act) (nth given 2)) :permission-fact-granted-in-another-lane
        (not= (:permission act) (nth given 3 nil)) :permission-fact-not-beneath-the-acts-own
        :else nil))))

(defn who-may-write-fault
  "For a fact under one of
  `softland.store.door.declaration/second-write-keys`. The first holder is
  the holder of the lane's first permission, which the lane's making act
  wrote: the person of a personal lane, whoever made a group's lane, the
  system for the base. The check reads no row: the act's permission has
  passed its standing, and a permission with three slots is a first
  permission (`softland.store.act`). A lane has one, and it does not end
  (`end-fault`).

  Not built, and not asked: a lane that is handed to another holder."
  {:softland/whose-call
   [{:state :decided
     :what "Only the lane's root holder writes its settings."
     :source "Sid, 20 November 2026;
              src/proposal/runtime-2026-11-21/PROPOSAL.md, entry 6"}
    {:state :not-decided
     :what "Sid's earlier word for the root holder is \"the owner\". No lane
            names an owner as a fact; that the first holder, the holder of
            the lane's first permission, is the one is a session's reading."}]}
  [act _fact]
  (when-not (= 3 (count (:permission act)))
    :only-the-first-holder-writes-a-setting))

(defn chains-needed
  "`place-rows` and `in-hand` are maps from key to row, in which a key that
  was read is there also when its place is empty. `give-over-fault` needs
  the rows of these places to say whether the permission at a place stands.
  A give at a place that is empty or holds an end needs none, and neither
  does an end, nor a give of a yes for one act: a yes's place takes one
  give, so whatever it holds is not read further (`store-key-fault`)."
  [act makes-the-lane? place-rows in-hand]
  (if makes-the-lane?
    []
    (into []
          (comp (filter (fn [fact] (and (contains? declaration/permission-keys (:k fact))
                                        (not= :softland.store/act-permission (:k fact))
                                        (some? (:v fact)))))
                (keep (fn [fact] (record/held-permission (get place-rows (place-of act fact)))))
                (mapcat (fn [held] (chain/places-above (:lane act) held)))
                (remove (fn [k] (or (contains? in-hand k) (contains? place-rows k))))
                (distinct))
          (:facts act))))

(defn- end-fault
  "The end of a permission: a fact with no value under one of the three keys
  of a permission. `row` is the row at the fact's place.

  `:no-permission-to-end` is also the word for a yes for one act that was
  used up: its use is an end, and nothing is left of it to end. The first
  permission does not end: nobody is above it, so nobody could give it
  again, and no act could be taken in the lane or write its settings ever
  after.

  A permission is ended by its giver, by anyone above the giver up to the
  first holder, and by its own holder. Its holder ends it by an act under
  that permission itself, or under her permission for the lane: never
  under her permission for another run or act, so a run ends nothing of
  hers but its own. A yes for one act takes no act but its one act
  (`standing`), so it ends itself only when that act is its end. The act's
  own permission has passed its standing, so whoever ends stands, and may
  act: a holder of `:see` cannot end, her own neither. A permission at the
  place that no longer stands, because one above it ended, can be ended
  too, by the same holders.

  The fact needs no `:replaces`. With one, check (e) holds it to the give
  that it names: an end that was sent for one give is then refused when
  another give has taken its place."
  {:softland/whose-call
   [{:state :decided
     :what "The giver of a permission, or anyone above, ends it."
     :source "Sid's decision of 20 November 2026;
              src/proposal/runtime-2026-11-21/PROPOSAL.md, entry 6"}
    {:state :not-decided
     :what "A holder ends her own permission, and the first permission does
            not end. His to turn."
     :source "a session, 21 November 2026"}]}
  [act row]
  (let [held (record/held-permission row)
        own (:permission act)]
    (cond
      (nil? held) :no-permission-to-end
      (= 3 (count held)) :first-permission-does-not-end
      (or (= held own) (chain/above? own held)) nil
      (and (nil? (act/covers own)) (= (:who act) (nth held 0))) nil
      :else :not-above-the-permission)))

(defn- give-over-fault
  "A give at a place that holds a row, for a permission for the lane or for
  a run; a yes's place takes one give, and never comes here
  (`store-key-fault`). The give is taken when the place holds an end, or a
  permission that does not stand. A holder has one permission for the
  lane, and a run one: one that stands is ended before another is given
  there. So a holder is given a permission again after an end, and each
  holder beneath an end is given one again by an act of its own.

  What must hold: `rows` holds the row of each place of `chains-needed`,
  and check (g) has held the new permission straight beneath the act's own."
  {:softland/whose-call
   [{:state :decided
     :what "A holder is given a permission again after an end, and each
            holder beneath an end is given one again by an act of its own."
     :source "Sid's decision of 21 November 2026;
              src/proposal/runtime-2026-11-21/PROPOSAL.md, entry 6"}]}
  [act row rows]
  (let [held (record/held-permission row)
        lane (:lane act)]
    (when (and held
               (chain/stands? lane held (assoc rows (chain/place lane held) row)))
      :holder-has-a-permission)))

(defn- store-key-fault
  "What a store key takes, for one fact. `rows` is the rows in hand, a map
  from key to row.

  A yes's place takes one give, whatever its row holds: its id names one
  yes for good, and a yes that was ended or used is never given again.
  Another yes for the same act is given under an id of its own. A lane's
  depot takes one write: in a lane that is made, a depot fact is refused
  with the other depot and with the lane's own. The three keys of
  `softland.store.door.declaration/second-write-keys` take a second write
  and any later one, with `:replaces` or without. A retract under a store
  key other than a permission's is not built.

  Whether a fact has a value is asked of a fact under a store key alone,
  whose `:v` the act's one form keeps. What must hold: the lane is made by
  this gate, so its depot is the gate's."
  {:softland/whose-call
   [{:state :not-decided
     :what "A change of a lane's depot is not decided: a depot fact with the
            other depot, in a lane that is made, is refused
            `:changing-a-depot-not-decided`."}]}
  [gate-depot act rows fact]
  (let [k (:k fact)
        retract? (nil? (:v fact))
        row (get rows (place-of act fact))]
    (cond
      (and (contains? declaration/permission-keys k) retract?) (end-fault act row)
      (and (declaration/store-key? k) retract?) :store-key-retract-not-built
      (and (= :softland.store/depot k) (not= gate-depot (:v fact))) :changing-a-depot-not-decided
      (= :softland.store/depot k) :store-key-takes-one-write
      (and (= :softland.store/act-permission k) row) :a-yes-is-given-once
      (and (contains? declaration/permission-keys k) row) (give-over-fault act row rows)
      :else nil)))

(defn places
  "Checks (e), (f) and (g), then who may write a key that takes a second
  write, then what a store key takes.

  An act that makes its lane is held to (e) and (f) as any act is, with no
  rows: nothing is current in a lane that is not made. It is not held to
  (g) or to the last step: the permission that it writes is its own and not
  one beneath its own, and its depot fact is the lane's first.

  What must hold: `standing` gave nil for the act, and `rows` holds the
  rows of the places that `places-needed` and `chains-needed` named, nil or
  absent for an empty place."
  [gate-depot act makes-the-lane? rows]
  (let [facts (:facts act)]
    (or (some #(replaced-fact-fault gate-depot act rows %) facts)
        (some #(setting-fault act %) facts)
        (when-not makes-the-lane?
          (or (some #(permission-fact-fault act %) facts)
              (some #(when (contains? declaration/second-write-keys (:k %))
                       (who-may-write-fault act %))
                    facts)
              (some #(store-key-fault gate-depot act rows %) facts))))))

;; ---------------------------------------------- the marked keys of the lane

(defn marked-keys-place
  "Its row says which keys the lane marks, and since which stamp each
  (`softland.store.record.rows/marked-keys-of`)."
  [act]
  (record/place-key (:lane act) (:lane act) :softland.store/marked-keys))

(defn reads-marked-keys?
  "A fact under a plain key may be under a marked one. A fact under
  `:softland.store/marked-keys` needs the row before, which gives the
  stamps that its keys keep."
  [act]
  (boolean (some (fn [fact]
                   (or (= :softland.store/marked-keys (:k fact))
                       (not (declaration/store-key? (:k fact)))))
                 (:facts act))))

(defn since-by-place
  "From the key of each place of the act whose key the lane marks to that
  key's stamp: the stamp since which the order of the key is level. A
  retract is in it too: it takes the entry of the value before out. The act
  is decided with the marks as they were before it, so a fact of the act
  that writes the mark of its own key is not in it."
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
  "`place-rows` holds a key that was read also when its place is empty. The
  row before of a place under a marked key gives the entry to take out of
  the order by value: that entry's key holds the digest of the value
  before, so it cannot be found without the row."
  [since-by-place place-rows]
  (into [] (remove #(contains? place-rows %)) (sort (keys since-by-place))))

;; --------------------------------------------------------------- the writes

(defn- level-since-after
  "A key that stays marked keeps its stamp, and so its order. A key whose
  mark is taken off and that is marked again later gets another stamp, and
  with it other keys in the order by value."
  [row-before new-keys level-from]
  (let [before (record/marked-keys-of row-before)]
    (into {} (map (fn [k] [k (get before k level-from)])) new-keys)))

(defn- by-value-writes
  "`value-bytes` is nil for a retract; `since` is the stamp since which the
  fact's key is marked.

  The entry of the value before goes out only when the row before has a
  value, its stamp is later than the key's stamp, and its digest is not the
  digest of the new value. A row before whose stamp is not later was
  written before the mark, or with it, and has no entry. A fact that is
  written again with the same value has one digest: its entry is written
  over, and nothing goes out.

  Every value is kept, whatever it is, by the digest of its bytes: a gate
  does not judge what an id is. Cost: one SHA-256 over the new value's
  bytes and one over the bytes of the value before."
  {:softland/whose-call
   [{:state :not-decided
     :what "What a marked key may hold is not decided. Every value is kept,
            whatever it is, by the digest of its bytes. This function is the
            one place where \"kept in no run\" would be made."
     :source "the queue's K5"}]}
  [act fact value-bytes row-before since entry]
  (let [digest-now (when value-bytes (record/value-digest value-bytes))
        before (record/plain-bytes-of row-before)
        digest-before (when (and before (< (long since) (long (:stamp row-before))))
                        (record/value-digest before))
        key-of (fn [digest] (record/by-value-key (:lane act) (:k fact) since digest (:e fact)))]
    (cond-> []
      (and digest-before (not= digest-before digest-now))
      (conj [:by-value (key-of digest-before) nil])

      digest-now
      (conj [:by-value (key-of digest-now) entry]))))

(defn- use-of-its-yes
  "The row by which an accept uses up the yes for one act that the act was
  taken under: an end at the yes's place, which names the yes itself as
  the permission that the act named. So the yes stands no more, an act
  under it is refused `:permission-used-up` (`used-up?`), it has nothing
  left to end, and its place takes no give again.

  No fact of the act wrote the row, so it names the act and no fact of it
  (`softland.store.record.rows/use-row`): every fact id that a row of the
  facts table names is the id of a fact that its act holds, which a reader
  of the act finds at that index. [] for an act that is the yes's own end:
  its own row at the place is that end and that use.

  The row is made of the act and its stamp alone, so a second run of the
  gates writes it again, the same, on both gates. The base's gate copies
  it to every task with the act's own rows
  (`softland.store.module.walk/copies-of`)."
  {:softland/whose-call
   [{:state :not-decided
     :what "A yes for one act is used once: the form of 22 November 2026, a
            session's, in the prompt that Sid pasted that day as his go for
            the build. His to turn."
     :source "src/proposal/runtime-2026-11-21/store-piece-2/SPEC.md"}
    {:state :not-decided
     :what "The use of a yes is kept as an end at the yes's place, written by
            the accept in the one write of the decision: a session's choice.
            His to turn."}]}
  [act act-uuid stamp time-millis who-text permission-text]
  (if (= :act (first (act/covers (:permission act))))
    (let [k (permission-place act)]
      (if (some #(= k (place-of act %)) (:facts act))
        []
        [[:fact k (record/use-row act-uuid stamp time-millis who-text permission-text)]]))
    []))

(defn- accept-writes
  "In the act's order, then the yes's use. Each fact holds the text of its
  value, from the declaration's check. `in-hand` is what `writes` takes."
  [act act-uuid stamp time-millis {:keys [since-by-place place-rows level-from]}]
  (let [lane (:lane act)
        who-text (text/text (:who act))
        permission-text (text/text (:permission act))]
    (into (into []
                (comp
                 (map-indexed
                  (fn [index fact]
                    (let [place (record/place-key lane (:e fact) (:k fact))
                          row (cond-> (record/fact-row fact act-uuid index stamp time-millis who-text permission-text)
                                (and (= :softland.store/marked-keys (:k fact)) (some? (:v fact)))
                                (record/with-level-since
                                  (level-since-after (get place-rows place) (:v fact) level-from)))
                          since (get since-by-place place)]
                      (concat
                       [[:fact place row]]
                       (when since
                         (by-value-writes act fact (record/plain-bytes-of row) (get place-rows place) since
                                          (record/by-value-row who-text permission-text act-uuid index stamp time-millis)))))))
                 cat)
                (:facts act))
          (use-of-its-yes act act-uuid stamp time-millis who-text permission-text))))

(defn writes
  "Each row that one decision writes in the record, as [which key row].
  `act` is nil for a thing that the declaration refused; `refused` is nil
  for an accept; `digest` is 32 bytes; `in-hand` is read only for an
  accept:

    :since-by-place  what `since-by-place` gave
    :place-rows      the rows of the act's places that were read: the row
                     before of each place of `:since-by-place`, and of the
                     marked-keys place when the act writes it
    :level-from      the stamp that a newly marked key gets: the stamp of
                     the act, in both gates. So a mark holds from the next
                     act of the lane

  A `:by-value` row that is nil says that the key is to be taken out.
  `[:last-stamp nil n]` names no key: the walk writes it for the act's
  lane.

  The answer holds the time when the thing has a usable one, a refused
  thing too. Each row of a fact and of the facts by value holds the time of
  the act that wrote it, so a read gives a fact's time with no second read.

  Every row is whole, at a fixed key, and a key that is taken out is fixed
  too; so each write needs no read. No row holds anything but what the
  thing, the rows read and the stamp give: a second run of the same
  decision with the same stamp writes the same rows at the same keys.

  What must hold: the thing is no stray; for an accept, each check gave nil
  for the act, each fact of the act holds its `:text` as the declaration's
  check gave it, `:place-rows` holds what the list above says, and the
  time is there."
  [act-id act refused stamp time-millis digest in-hand]
  (let [[lane _ act-uuid] act-id]
    (conj (if refused [] (accept-writes act act-uuid stamp time-millis in-hand))
          [:answer (record/answer-key lane act-uuid) (record/answer-row refused stamp time-millis digest)]
          [:last-stamp nil (long stamp)])))

;; ----------------------------------------------------------------- the line

(def ^{:softland/whose-call
       [{:state :not-decided
         :what "33,554,432, 32 MiB in weight, the heaviest thing that a gate
                keeps a second time when its sender has standing: Sid's call
                of 22 November 2026, the default of the plan's check. A
                number of the build, kept and not decided."
         :source "Sid's call of 22 November 2026"}]}
  max-kept-weight
  "32 MiB, in weight (`softland.store.door.weight`): the heaviest thing that
  a gate keeps a second time, for the read by stamp, when its sender has
  standing (`kept-bound`). Every act that passes the declaration weighs
  less, unless its decimals hold more digits than their text says. A thing
  over the bound is in its depot and in no read by stamp."
  33554432)

(def ^{:softland/whose-call
       [{:state :not-decided
         :what "262,144, 256 KiB, the heaviest thing that a gate keeps a
                second time when its sender has no standing in the lane, or
                the declaration refused it: Sid's call of 22 November 2026. A
                number of the build, kept and not decided."
         :source "Sid's call of 22 November 2026"}]}
  max-kept-weight-with-no-standing
  "256 KiB: the heaviest thing that a gate keeps a second time when its
  sender has no standing in the lane, or the declaration refused it. So a
  sender with no standing cannot make the task write and hold the bytes of
  a large thing twice."
  262144)

(defn weighing-bound
  "The smaller bound for a thing that the declaration refused, since no
  standing is read for it; else the larger, the most that the standing can
  give. The weighing stops at the bound and one, so a thing that is too
  heavy keeps no exact weight."
  [declarations-reason]
  (if declarations-reason max-kept-weight-with-no-standing max-kept-weight))

(defn kept-bound
  "A sender has standing when the declaration took the thing and the act's
  permission stands and is hers: `standing` gave nil, or
  `:permission-does-not-allow-acting`, for a holder who may only see."
  [declarations-reason refused-standing]
  (if (and (nil? declarations-reason)
           (or (nil? refused-standing) (= :permission-does-not-allow-acting refused-standing)))
    max-kept-weight
    max-kept-weight-with-no-standing))

(defn line-writes
  "The rows that one decision writes into the lane's line, as [which key
  row]. `weight` is what `softland.store.door.digest/weighed` gave: a long,
  `:too-heavy` or `:not-weighed`. `bound` is what `kept-bound` gave. A thing
  that is not kept has no weight in its row, only the word.

  Each row is made of the thing, the stamp and the digest, and of nothing
  else: no task, no position in a depot, no clock. So a second run of the
  gates writes the same rows at the same keys, on a store of any number of
  tasks.

  What must hold: the thing is no stray, and no answer was under its
  act-id."
  [act-id thing refused stamp time-millis digest weight bound]
  (let [[lane _ act-uuid] act-id
        who (declaration/usable-who thing)
        kept? (and (int? weight) (<= (long weight) (long bound)))
        not-kept (cond kept? nil (= :not-weighed weight) :not-weighed :else :too-heavy)
        k (record/line-key lane stamp)]
    (cond-> [[:line k (record/line-row act-uuid refused time-millis digest who (when kept? weight) not-kept)]]
      kept? (conj [:thing k thing])
      (some? who) (conj [:by-who (record/by-who-key lane who stamp) (long stamp)]))))
