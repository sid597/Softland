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
  :microbatch. The one caller in a cluster is the walk of one thing
  (`softland.store.module.walk`), which both gates call.

  The checks, in the order in which a gate makes them. The order of the
  checks is the order of the reasons: when an act has two faults, the check
  that comes first gives the recorded word. Each check gives nil, or the
  word of its first fault. A check that examines facts takes them in the
  act's order.

    early      the declaration's own word
               (`softland.store.door.declaration/check`), which the walk
               takes as it is. Nothing of the record is read.
    standing   `standing`: the layer's depot is this gate's (c); the layer
               is made, or the act is a right making act; the act's
               permission is held by its `:who` and covers the act: the
               layer, the run that the act names as its `:session`, or
               this one act, by the uuid of its act-id and its digest
               (`softland.store.act/covers`); it stands (d), and each
               permission above it stands; and it allows acting: an act
               needs \"act\".
    places     `places`: each replaced fact is the current fact of its
               place (e); no setting key on an entity other than the layer
               (f); a permission fact writes a permission straight beneath
               the act's own, and the act's own covers the layer: a
               permission for a run or for an act gives nothing (g); who
               may write a key that takes a second
               write (`who-may-write-fault`); who may end a permission, and
               on which place a give is taken (`end-fault`,
               `give-over-fault`); and what a store key does not take in
               this build.

  `chain-needed` says which rows `standing` needs beside the first two.
  `places-needed` says which rows `places` needs, and `chains-needed` which
  more, once those are read. `time-on` gives the time on the act, which
  `writes` puts beside the stamp as a label. `writes` gives each row that
  the decision writes.

  What an accept reads after the last check, for the order by value. No
  check reads a layer's marked keys, so a refused act pays nothing for them.
  `reads-marked-keys?` says whether an accepted act needs the row at
  `marked-keys-place`; `since-by-place` gives, from that row, the places of
  the act whose key is marked, each with the key's stamp; and
  `rows-before-needed` names the rows before that are still to be read.
  `writes` then gives, beside the fact's row, the rows of the facts by
  value: the entry of the new value, and the entry of the value before
  taken out. The walk calls the four, for both gates.

  How two of the checks rest on how a row can come to be, and read less
  than their words name.

  (c) costs no read for a layer that is made here. A gate's record holds
  rows only for layers of its depot: a row is written only by an accept; an
  accept happens in a layer that is made here, or makes it; a making act is
  right only for the gate of the depot that the layer starts with; and no
  act of this build changes a depot. So a row at the permission's place, or
  at the layer's depot place, says that the layer is this gate's.

  (d) is one read for each permission of the act's chain. A permission
  fact sits on what it covers, its holder, a run or an act
  (`softland.store.gate.chain/place`), so the place that the act's
  permission names holds that permission or the act's permission does not
  stand. A permission id
  holds its whole chain, so the places of the permissions above it are read
  from the act alone (`chain-needed`), and only when the first row holds the
  act's permission. A permission can be ended: an end is a later fact at its
  place, with no value. What was given beneath it ends with it, and that is
  written nowhere: it is read here, up the chain, at each decision
  (`softland.store.gate.chain`, which says when a permission stands and
  whose choices those rules are).

  State and effects. None. No function here opens anything or reads a
  tool's value. The text of each value is in the act's one form, where the
  declaration's check put it, and `writes` puts it in the fact's row. It is
  nowhere else: the texts of an act that a check refused are dropped with
  the decision.

  Not here. Check (a), the answer under the act-id, and the test for a
  stray: the gate makes them before any of these. The bound on one value
  and the bound on an act: they are the last step of the declaration's
  check, and come to the walk as its word. The other gate's record:
  no check takes a row of it, and whether a layer is the other gate's is
  asked of the layer id alone
  (`softland.store.gate.first-act/starts-with-the-other-gates-depot?`).
  The stamp of the decision: the walk makes it of the layer's last stamp
  (`softland.store.gate.stamp/next-stamp`), and it reads nothing of the
  act. No check here reads the time on the act or a stamp that the act
  carries."
  (:require [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.door.text :as text]
            [softland.store.gate.chain :as chain]
            [softland.store.gate.first-act :as first-act]
            [softland.store.record.rows :as record]))

;; -------------------------------------------------------- the time on the act

(defn time-on
  "Takes a thing as it arrived on a depot. Gives the time on it, in
  milliseconds since 1970, when it holds a usable one
  (`softland.store.door.declaration/usable-time`): the time which the door
  writes from the clock of its server, or whatever else appended the thing
  wrote there. This code knows only that the part holds a whole number not
  below zero. Nil for a thing with no usable time, which the declaration
  refuses `:no-door-time`. A gate gives what this gives to `writes`, which
  puts it beside the stamp as a label: in the answer's row, and for an
  accept in each row of a fact and of the facts by value. No check reads
  it, and no stamp holds it. A gate reads no clock for it."
  [thing]
  (declaration/usable-time thing))

;; ----------------------------------------------------------------- standing

(defn permission-place
  "Takes an act. Gives the key of the place where the permission that the
  act names sits in the act's layer, read from the permission's own form
  (`softland.store.gate.chain/place`): on its holder for one that covers the
  layer, on the act's run or on the act itself for one that covers a run or
  an act. The first row that a gate reads for the standing of an act. One
  place for a holder's permission for the layer: that is kept as built and
  not decided, and this key is one of the two places where the other answer
  would be made (`softland.store.door.declaration/store-keys`)."
  [act]
  (chain/place (:layer act) (:permission act)))

(defn hex-for-its-permission
  "Takes a thing as it arrived on a depot, and the act that the declaration's
  check made of it. Gives the text that `standing` holds against a
  permission for one act: the digest of the thing without the time on it
  and without its `:permission`
  (`softland.store.door.digest/hex-for-its-permission`). nil when the
  permission that the act names is not for an act, so that no other act
  pays for the hash."
  [thing act]
  (when (= :act (first (act/covers (:permission act))))
    (digest/hex-for-its-permission thing)))

(defn depot-place
  "Takes an act. Gives the key of the place of its layer's depot: the layer
  as entity, under `:softland.store/depot`. A gate reads it only when the
  place of the permission is empty."
  [act]
  (record/place-key (:layer act) (:layer act) :softland.store/depot))

(defn- layer-is-the-other-gates?
  "Takes the gate's depot and an act. Gives true when the act's layer is the
  other gate's: it starts with the other gate's depot
  (`softland.store.gate.first-act/starts-with-the-other-gates-depot?`). It
  reads the layer id of the act and nothing else, so it gives the same
  whether or not the other gate has made the layer. For the streaming gate
  such a layer is the base; for the microbatch gate it is every other
  layer."
  [gate-depot act]
  (first-act/starts-with-the-other-gates-depot? gate-depot (:layer act)))

(defn makes-the-layer?
  "Takes the gate's depot, an act, the row at the place of the permission
  that the act names (`permission-place`), and the row at the place of the
  layer's depot. Gives true
  when the act is the first act of its layer: both rows are nil, the layer
  is not the other gate's (`layer-is-the-other-gates?`), and the act has
  the form of a making act for this gate. `standing` gives nil for such an
  act and for an act whose permission stands; this tells the two apart.
  What must hold: when the permission's row is nil, the depot's row was
  read."
  [gate-depot act permission-row depot-row]
  (boolean (and (nil? permission-row)
                (nil? depot-row)
                (not (layer-is-the-other-gates? gate-depot act))
                (first-act/making-act? gate-depot act))))

(defn chain-needed
  "Takes an act, and the row at the place of the permission that the act
  names (`permission-place`). Gives the
  keys of the places whose rows `standing` needs beside that row: the place
  of each permission above the act's own, nearest first
  (`softland.store.gate.chain/places-above`). An empty vector when the row
  does not hold the act's permission, since the act is then refused with no
  read more, and for an act under a first permission, which has nothing
  above it."
  [act permission-row]
  (if (record/holds-value? permission-row (:permission act))
    (chain/places-above (:layer act) (:permission act))
    []))

(defn standing
  "The standing of an act: checks (c) and (d), what the act's permission
  allows and covers, and the first act of a layer. Takes the gate's depot;
  an act; the row at the place of the permission that the act names
  (`permission-place`); the row at the place of the layer's depot, which is
  read only when the first is nil; the rows of the places that
  `chain-needed` named, as a map from key to row, nil or absent for an empty
  place; and what `hex-for-its-permission` gave for the thing. Gives nil
  when the act's permission stands, covers the act and allows acting, or
  when the act is a right making act for a layer that is not made
  (`makes-the-layer?` tells the two apart). Else a word:

    :layer-belongs-to-the-other-gate  the record holds nothing for the
                                      layer, and the layer is the other
                                      gate's
    :layer-not-made                   the record holds nothing for the
                                      layer, the layer is not the other
                                      gate's, and the act is no right
                                      making act
    :who-not-the-permissions-holder   the layer is made here, and the holder
                                      that the act's permission names is not
                                      the act's `:who`
    :permission-ended                 the layer is made here, and the place
                                      of the act's permission holds an end:
                                      the permission of that holder, run or
                                      act was ended, and none was given
                                      there again
    :permission-for-another-run       the record holds the act's permission,
                                      which covers one run, and the act's
                                      `:session` is not that run
    :permission-for-another-act       the record holds the act's permission,
                                      which covers one act, and the uuid of
                                      the act's act-id is not the one that
                                      it names
    :act-not-the-one-permitted        the record holds the act's permission,
                                      which covers this act-id, and the
                                      thing that arrived is not the act
                                      that was permitted: its digest,
                                      without its permission and the time
                                      on it, is not the one that the
                                      permission names
    :permission-does-not-stand        the layer is made here, and the record
                                      does not hold the act's permission for
                                      its holder: the place is empty, or
                                      holds another permission
    :permission-above-ended           the record holds the act's permission,
                                      and a permission above it does not
                                      stand, or was given again after the
                                      act's own was given
                                      (`softland.store.gate.chain/stands?`)
    :permission-does-not-allow-acting the act's permission stands, and it
                                      says `:see`

  An act that was taken under a permission stays taken when the permission
  ends: the rows of its facts stay, and the log holds the act. An act that
  is decided after the end is refused, the holder's own and that of each
  holder beneath her. The end has a stamp in the layer's one order, so
  which acts came before it is the same at each run of the gates.

  The last word costs no read: what a permission allows is a slot of the
  permission that the act names (`softland.store.act/allows`), and the row
  was just compared with it, slot and all. So nobody raises her right by
  naming it: a holder of `:see` who names her permission without the slot,
  or with `:see-and-act`, names bytes that the row does not hold, and gets
  `:permission-does-not-stand`. The word comes before every check of a
  place. A holder of `:see` cannot give either: a give is an act, and so
  is an end, her own too. A permission that stands is not written over, so
  \"see\" becomes \"see and act\" by an end and a give after it
  (`give-over-fault`).

  What a permission covers is held here, before the chain is read
  (`softland.store.act/covers`). A permission for a run covers the acts
  that carry that run as their `:session`, of the permission's holder alone:
  ten thousand runs of one who each have a permission of their own, or none.
  A permission for an act covers the one thing whose act-id holds its uuid
  and whose digest it names, so the act that is taken is the act that was
  permitted, part for part; an act-id has one answer, so no second act is
  ever taken under it. The session and the who are what the sending program
  said: a gate checks no sender.

  Whether the layer is the other gate's is worked out here, from the gate's
  depot and the act's layer (`layer-is-the-other-gates?`), and counts only
  when both rows are nil. No table of the other gate is read for it, in
  either gate. So an act on one depot for a layer that starts with the
  other depot, of which this gate's record holds nothing, gets
  `:layer-belongs-to-the-other-gate` whether or not the other gate has made
  the layer, and never `:layer-not-made`. The word of an act for a layer of
  the other gate comes from the act alone: a session's rule, 14 October
  2026, and no decision of Sid's. One half of the rule is forced by his
  line that the store comes out the same when it is run again: the word
  cannot follow the other gate's record, since a word that followed what
  the other gate had made by then would follow an order across two depots,
  which no second run gives again. The other half is the session's choice:
  which of the two words is then given. `:layer-not-made` for every such
  act would hold his line as well."
  [gate-depot act permission-row depot-row chain-rows hex-of-the-act]
  (let [permission (:permission act)
        [kind what hex] (act/covers permission)]
    (cond
      (record/holds-value? permission-row permission)
      (cond
        (not= (:who act) (nth permission 0))
        :who-not-the-permissions-holder

        (and (= :run kind) (not= what (:session act)))
        :permission-for-another-run

        (and (= :act kind) (not= what (nth (:act-id act) 2 nil)))
        :permission-for-another-act

        (and (= :act kind) (not= hex hex-of-the-act))
        :act-not-the-one-permitted

        (not (chain/stands? (:layer act) permission
                            (assoc chain-rows (chain/place (:layer act) permission) permission-row)))
        :permission-above-ended

        (= :see (act/allows permission))
        :permission-does-not-allow-acting)

      (or permission-row depot-row)
      (cond
        (not= (:who act) (nth permission 0)) :who-not-the-permissions-holder
        (record/ended? permission-row) :permission-ended
        :else :permission-does-not-stand)

      (layer-is-the-other-gates? gate-depot act)
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
  something to replace, and of each fact under one of the three keys of a
  permission (`softland.store.door.declaration/permission-keys`).
  None for an act that makes its layer: the record holds nothing of a layer
  that is not made, and the two facts of a making act are not held to the
  checks that read a place. A fact that names nothing to replace needs no
  row, and so can never be stale."
  [act makes-the-layer?]
  (if makes-the-layer?
    []
    (into []
          (comp (filter (fn [fact] (or (:replaces fact) (contains? declaration/permission-keys (:k fact)))))
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
  "Check (g) for one fact. Gives a word when the fact is under one of the
  three keys of a permission, has a value, and is no give that the act's own
  permission can make: the act's own permission covers a run or an act, and
  such a permission gives nothing; the given permission's layer slot is not
  the act's layer; its granted-in slot is not the act's layer; or the
  permission above it is not exactly the one that the act acts under. A
  permission with nothing above it earns the last word.

  Why a permission for a run or for an act gives nothing. What is given
  beneath a permission must cover no more than it does: else the holder of a
  permission for one run gives herself one for the layer beneath it, and a
  yes for one run is a yes for every run. Neither a layer nor a second run
  fits inside one run. A session's rule of 21 November 2026
  (src/proposal/runtime-2026-11-21/store-piece-2/SPEC.md, R10); the other
  way, a run that may give, would be one more word in the sixth slot."
  [act fact]
  (let [given (:v fact)]
    (when (and (contains? declaration/permission-keys (:k fact)) (some? given))
      (cond
        (some? (act/covers (:permission act))) :only-a-permission-for-the-layer-gives
        (not= (:layer act) (nth given 1)) :permission-fact-for-another-layer
        (not= (:layer act) (nth given 2)) :permission-fact-granted-in-another-layer
        (not= (:permission act) (nth given 3 nil)) :permission-fact-not-beneath-the-acts-own
        :else nil))))

(defn who-may-write-fault
  "Who may write a layer's setting, the list of what it builds on, and its
  marked keys: the check, for one fact under one of
  `softland.store.door.declaration/second-write-keys`. Takes the act and the
  fact. Gives nil when the act is under the layer's first permission, and
  `:only-the-first-holder-writes-a-setting` under any other: a given
  permission, whatever it allows.

  The first holder is the holder of the layer's first permission, the one
  with nothing above it that the layer's making act wrote
  (`softland.store.gate.first-act/making-act?`): the person of a personal
  layer, whoever made a group's layer, the system for the base. The check
  reads no row: an act's permission has passed its standing, and a
  permission with three slots is a first permission
  (`softland.store.act`). A layer has one, and it does not end
  (`end-fault`).

  Sid decided on 20 November 2026 that only the layer's root holder writes
  its settings (src/proposal/runtime-2026-11-21/PROPOSAL.md, entry 6). His
  earlier word for her is \"the owner\"; no layer names an owner as a
  fact, and that the first holder is the one is a session's reading. Not
  built, and not asked: a layer that is handed to another holder."
  [act _fact]
  (when-not (= 3 (count (:permission act)))
    :only-the-first-holder-writes-a-setting))

(defn chains-needed
  "Takes an act; whether it makes its layer; the rows of the places that
  `places-needed` named, as a map from key to row; and the rows that are in
  hand already, as a map in which a key that was read is there also when
  its place is empty. Gives the keys of the places that `places` needs
  beside those, each once, in the order of the keys: for each fact of the
  act that gives a permission at a place which holds a permission, the
  places of the permissions above the one that the place holds
  (`softland.store.gate.chain/places-above`), without the keys that are in
  hand. `give-over-fault` needs them to say whether the permission at the
  place stands. An empty vector for an act with no such fact, and for an
  act that makes its layer. A give at a place that is empty or holds an end
  needs none, and neither does an end."
  [act makes-the-layer? place-rows in-hand]
  (if makes-the-layer?
    []
    (into []
          (comp (filter (fn [fact] (and (contains? declaration/permission-keys (:k fact)) (some? (:v fact)))))
                (keep (fn [fact] (record/held-permission (get place-rows (place-of act fact)))))
                (mapcat (fn [held] (chain/places-above (:layer act) held)))
                (remove (fn [k] (or (contains? in-hand k) (contains? place-rows k))))
                (distinct))
          (:facts act))))

(defn- end-fault
  "The end of a permission, for one fact with no value under one of the three
  keys of a permission: the end of a holder's permission for the layer, of
  a run's, or of an act's, each on its own place. Takes the act and the row at the fact's place. Gives nil
  when the end is taken, or a word:

    :no-permission-to-end            the place holds no permission: it is
                                     empty, or holds an end
    :first-permission-does-not-end   the place holds the layer's first
                                     permission. Nobody is above it, so
                                     nobody could give it again, and no act
                                     could be taken in the layer or write
                                     its settings ever after
    :not-above-the-permission        the act's permission is not the one at
                                     the place, is not in the chain above
                                     it (`softland.store.gate.chain/above?`),
                                     and is not the permission for the
                                     layer of the holder of the one at the
                                     place

  So a permission is ended by its giver, by anyone above the giver up to
  the first holder, and by its own holder. Its holder ends it by an act
  under that permission itself, or under her permission for the layer:
  never under her permission for another run or act, so a run ends nothing
  of hers but its own. The act's own permission has
  passed its standing, so whoever ends stands, and may act: a holder of
  `:see` cannot end, her own neither, since an end is an act. A permission
  at the place that no longer stands, because one above it ended, can be
  ended too, by the same holders.

  That the giver or anyone above ends a permission is Sid's decision of 20
  November 2026 (src/proposal/runtime-2026-11-21/PROPOSAL.md, entry 6).
  That a holder ends her own, and that the first permission does not end,
  are a session's choices of 21 November 2026, and his to turn.

  The fact needs no `:replaces`. With one, check (e) holds it to the give
  that it names: an end that was sent for one give is then refused when
  another give has taken its place."
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
  "A give at a place that holds a row, for one fact with a value under one of
  the three keys of a permission. Takes the act, the row at the
  fact's place, and the rows in hand as a map from key to row. Gives nil
  when the place holds an end, or a permission that does not stand
  (`softland.store.gate.chain/stands?`): the give is taken and its row
  takes the place. Gives `:holder-has-a-permission` when the permission at
  the place stands: a holder has one permission for the layer, and a run or
  an act one, and one that stands is ended before another is given there.

  So a holder is given a permission again after an end, and each holder
  beneath an end is given one again by an act of its own (Sid's decision of
  21 November 2026, src/proposal/runtime-2026-11-21/PROPOSAL.md, entry 6).
  Check (g) has held the new permission straight beneath the act's own.

  What must hold: `rows` holds the row of each place of `chains-needed`."
  [act row rows]
  (let [held (record/held-permission row)
        layer (:layer act)]
    (when (and held
               (chain/stands? layer held (assoc rows (chain/place layer held) row)))
      :holder-has-a-permission)))

(defn- store-key-fault
  "What a store key takes, for one fact. Takes the gate's depot, the act,
  the rows in hand as a map from key to row, and the fact. A fact under
  one of the three keys of a permission with no value is an end, and
  `end-fault` has the word; one with a value at a place that holds a row is a give over
  what is there, and `give-over-fault` has the word. A layer's depot takes
  one write. The three keys of
  `softland.store.door.declaration/second-write-keys`, who may see a layer,
  what it builds on and its marked keys, take a second write and any later
  one: such a fact earns no word here, with `:replaces` or without. A
  retract under a store key other than the permission is refused. Gives a
  word for: an end or a give that its own check refuses; a fact with no
  value under any other store key; a depot fact in a layer that is made,
  with the other depot (a change of depot is not decided) or with the
  layer's own (its place holds one). A fact under any other key earns none.
  Whether a fact has a value is asked of a fact under a store key alone,
  whose `:v` the act's one form keeps. What must hold: the layer is made by
  this gate, so its depot is the gate's."
  [gate-depot act rows fact]
  (let [k (:k fact)
        retract? (nil? (:v fact))
        row (get rows (place-of act fact))]
    (cond
      (and (contains? declaration/permission-keys k) retract?) (end-fault act row)
      (and (declaration/store-key? k) retract?) :store-key-retract-not-built
      (and (= :softland.store/depot k) (not= gate-depot (:v fact))) :changing-a-depot-not-decided
      (= :softland.store/depot k) :store-key-takes-one-write
      (and (contains? declaration/permission-keys k) row) (give-over-fault act row rows)
      :else nil)))

(defn places
  "The checks of the places. Takes the gate's depot; an act; whether it makes
  its layer; and the rows in hand, as a map from key to row, nil or absent
  for an empty place: the rows of the places that `places-needed` and
  `chains-needed` named. Gives nil, or the word of the first check that
  fails, in this order:

    (e)  :replaced-fact-not-current
    (f)  :setting-not-on-the-layer
    (g)  :only-a-permission-for-the-layer-gives,
         :permission-fact-for-another-layer,
         :permission-fact-granted-in-another-layer,
         :permission-fact-not-beneath-the-acts-own
    then :only-the-first-holder-writes-a-setting, for a fact under a key
         that takes a second write (`who-may-write-fault`)
    then, for the first fact that earns one: :no-permission-to-end,
         :first-permission-does-not-end, :not-above-the-permission
         (`end-fault`); :store-key-retract-not-built,
         :changing-a-depot-not-decided, :store-key-takes-one-write;
         :holder-has-a-permission (`give-over-fault`)

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
  whose row before gives the stamps that its keys keep. False for a give, a
  making act, a setting and a list: none of them reads the marked keys."
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

;; --------------------------------------------------------------- the writes

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
  the act's order, for each fact: its row, with the text of its value, who
  wrote it, the permission that the act named and the time on the act, and
  for the fact under `:softland.store/marked-keys` the stamps of
  `level-since-after`; and for a fact under a marked key, the rows of
  `by-value-writes`. The one text of
  the act's `:who` and the one text of its permission are made here, once
  for the act, and handed to each row of a fact and of the facts by value.
  The text of a value is not made here: each fact holds its own, from the
  declaration's check. `in-hand` is what `writes` takes."
  [act act-uuid stamp time-millis {:keys [since-by-place place-rows level-from]}]
  (let [layer (:layer act)
        who-text (text/text (:who act))
        permission-text (text/text (:permission act))]
    (into []
          (comp
           (map-indexed
            (fn [index fact]
              (let [place (record/place-key layer (:e fact) (:k fact))
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
          (:facts act))))

(defn writes
  "Each row that one decision writes, as data, for the walk to write in one
  call. Takes the act-id under which the thing arrived; the act, or nil for
  a thing that the declaration refused; the word of the refusal, or nil for
  an accept; the stamp of the decision; the time on the act (`time-on`), or
  nil for a thing with no usable time; the digest of the thing, 32 bytes;
  and what the walk has in hand for an accept, a map that is not read for a
  refusal:

    :since-by-place  for each place of the act whose key the layer marks,
                     the key's stamp (`since-by-place`); empty or absent
                     when none is
    :place-rows      the rows of the act's places that were read, a map
                     from key to row: the row before of each place of
                     `:since-by-place`, and of the marked-keys place when
                     the act writes it
    :level-from      the stamp that a newly marked key gets: the stamp of
                     the act, in both gates. So a mark holds from the next
                     act of the layer

  Gives a vector of [which key row]:

    [:fact key row]        the row of a fact's place
    [:by-value key row]    a row of the facts by value, for a fact under a
                           marked key. A row that is nil says that the key
                           is to be taken out
    [:answer key row]      the answer under the act-id
    [:last-stamp nil n]    the last stamp to keep, which is the stamp of
                           the decision. It names no key: the walk writes
                           it for the act's layer, in the table that the
                           gate keeps for a task

  A refusal writes the answer and the last stamp, and nothing else. An
  accept writes all of them.

  The time is a label beside the stamp, and no rule reads it. The answer
  holds it when the thing has a usable one, a refused thing too: a refusal
  for a fault other than the time keeps the time that the thing carried.
  Each row of a fact and of the facts by value holds the time of the act
  that wrote it, so a read gives a fact's time with no second read.

  Every row is whole, at a fixed key, and a key that is taken out is fixed
  too; so each write needs no read. No row holds anything but what the
  thing, the rows read and the stamp give: a second run of the same
  decision with the same stamp writes the same rows at the same keys. The
  time is the thing's own.

  What must hold: the thing is no stray; for an accept, each check gave nil
  for the act, each fact of the act holds its `:text` as the declaration's
  check gave it, `:place-rows` holds what the list above says, and the
  time is there: the declaration refuses a thing with none."
  [act-id act refused stamp time-millis digest in-hand]
  (let [[layer _ act-uuid] act-id]
    (conj (if refused [] (accept-writes act act-uuid stamp time-millis in-hand))
          [:answer (record/answer-key layer act-uuid) (record/answer-row refused stamp time-millis digest)]
          [:last-stamp nil (long stamp)])))
