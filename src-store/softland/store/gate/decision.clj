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
  :microbatch, where its answer depends on it. The one caller in a cluster
  is the walk of one thing (`softland.store.module.walk`), which both gates
  call.

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
               permission stands for its `:who` (d), which is its holder's
               row and the row of each level above it
               (`softland.store.gate.chain/stands?`); and that permission
               allows acting: an act needs more than \"see\".
    places     `places`: the act's permission allows each fact's key; each
               replaced fact is the current fact of its place (e); no
               setting key on an entity other than the layer (f); a
               permission fact writes a permission straight beneath the
               act's own, which allows no more than the act's own (g); who
               may write a key that takes a second write
               (`who-may-write-fault`); and what a store key takes: the end
               of a right only from above it, and one write for the place
               of a permission while it holds a value.
    claims     `claims`: under a key that the layer marks as taking one
               write for a place, a claim of a place that holds a value is
               refused.

  `levels-needed` says which rows above the act's permission `standing`
  needs, `places-needed` which rows `places` needs, and `holds-a-claim?`,
  `one-write-keys-place` and `claim-places-needed` which rows `claims`
  needs. A claim is a fact with a value under a plain key that names
  nothing to replace. `time-on` gives the time on the act, which `writes`
  puts beside the stamp as a label. `writes` gives each row that the
  decision writes.

  What an accept reads after the last check, for the order by value. No
  check reads a layer's marked keys, so a refused act pays nothing for them.
  `reads-marked-keys?` says whether an accepted act needs the row at
  `marked-keys-place`; `since-by-place` gives, from that row, the places of
  the act whose key is marked, each with the key's stamp; and
  `rows-before-needed` names the rows before that are still to be read.
  `writes` then gives, beside the fact's row, the rows of the facts by
  value: the entry of the new value, and the entry of the value before
  taken out. The walk calls the four, for both gates.

  How the checks rest on how a row can come to be.

  (c) costs no read for a layer that is made here. A gate's record holds
  rows only for layers of its depot: a row is written only by an accept; an
  accept happens in a layer that is made here, or makes it; a making act is
  right only for the gate of the depot that the layer starts with; and no
  act of this build changes a depot. So a row at the permission's place, or
  at the layer's depot place, says that the layer is this gate's.

  (d) reads the row at the place of `:who`'s permission, and, when that row
  holds the act's permission, the row of each level above it. A permission
  fact sits on its holder, so the first row holds the act's permission or
  the act's permission does not stand. A right can end (Sid, 15 October
  2026: rights can be taken back; src/proposal/sign-in-2026-10-15/BUILD.md,
  decision 7 and default 11): the end is a retract at the right's place,
  and ending one ends what was given beneath it. So each level above is
  read, and the act's permission stands only when every level stands
  (`softland.store.gate.chain/stands?`), each written before the level
  beneath it. A permission of depth d costs d reads.

  The ends. Who may end a right is a default that a session told Sid on 15
  October 2026 and that he did not strike (BUILD.md, default 11): the one
  who gave it, anyone above it in its chain, and the system where the
  system is the chain's top. In the record that is one test: the act's
  permission is one of the levels above the right at the place
  (`softland.store.gate.chain/above?`). A holder does not end her own right
  this way. A place whose row is a retract holds no right, and a give onto
  it is taken: \"see\" is raised to \"see and act\" by an end and then a
  give. A right given again with the same id does not bring back what was
  given beneath it before the end (`softland.store.gate.chain`).

  What a right allows. A permission lists the keys that its holder may
  write, or allows every key (`softland.store.act/allows`); giving a right
  and changing a layer's setting are keys of the store like any other, and
  a give allows no more than the giver holds (BUILD.md, default 12). So the
  key of each fact is held to the act's permission, and a give to the
  permission above it, here, in the layer's gate.

  One write for a place under a marked key. A layer can mark plain keys so
  that a place under one takes one write while it holds a value
  (`:softland.store/one-write-keys`; BUILD.md, the store's change d): a
  claim of such a place is refused while it holds a value, a fact that
  names the current fact replaces it, and a retract frees it. The layer's
  marks are read as they were before the act, as the marked keys are. Two
  claims of one place in one layer's line: the first is taken, the second
  refused, in the order of the line.

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
  The rule of a chain: `softland.store.gate.chain`. What a permission
  allows, and whether one allows no more than another:
  `softland.store.act`. The stamp of the decision: the walk makes it of
  the layer's last stamp (`softland.store.gate.stamp/next-stamp`), and it
  reads nothing of the act. No check here reads the time on the act or a
  stamp that the act carries."
  (:require [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
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

(defn- holds-a-value?
  "Takes a fact's row, or nil. Gives true when the row holds a value: false
  for no row, and for a row whose current fact is a retract."
  [row]
  (boolean (and row (some? (:value row)))))

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

(defn levels-needed
  "Takes an act and the row at the place of its `:who`'s permission
  (`permission-place`). Gives the keys of the places of the levels above the
  act's permission (`softland.store.gate.chain/level-places`), each once, in
  the order of its chain from the level straight above, when that row holds
  the act's permission. Else none: the act's standing is then refused, or
  the act makes its layer, with no level above read. The walk reads each
  after the holder's own row and hands them to `standing`. A permission of
  depth d names d - 1, 31 at most; a first permission names none. Each
  place is in the act's layer, so no read leaves the layer's task."
  [act permission-row]
  (let [permission (:permission act)]
    (if (record/holds-value? permission-row permission)
      (let [[own & above] (chain/level-places (:layer act) permission)]
        (into [] (comp (remove #{own}) (distinct)) above))
      [])))

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
  "Takes the gate's depot, an act, the row at the place of `:who`'s
  permission, and the row at the place of the layer's depot. Gives true
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

(defn standing
  "The standing of an act: checks (c) and (d), what the act's permission
  allows, and the first act of a layer. Takes the gate's depot; an act; the
  row at the place of `:who`'s permission; the row at the place of the
  layer's depot, which is read only when the first is nil; and the rows of
  the levels above the act's permission that `levels-needed` named, as a
  map from place key to row, nil or absent for an empty place. Gives nil
  when the act's permission stands and allows acting, or when the act is a
  right making act for a layer that is not made (`makes-the-layer?` tells
  the two apart). Else a word:

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
    :permission-does-not-stand        the layer is made here, and the record
                                      does not hold the act's permission for
                                      its holder: her place is empty, holds
                                      another permission, or her right there
                                      has ended
    :permission-above-has-ended       her place holds the act's permission,
                                      and a level above it does not stand
                                      (`softland.store.gate.chain/stands?`):
                                      it was ended, or ended and given
                                      again after this right
    :permission-does-not-allow-acting the act's permission stands, and it
                                      says `:see`

  The last word costs no read: what a permission allows is a slot of the
  permission that the act names (`softland.store.act/allows`), and the row
  was just compared with it, slot and all. So nobody raises her right by
  naming it: a holder of `:see` who names her permission without the slot,
  or with `:see-and-act`, names bytes that the row does not hold, and gets
  `:permission-does-not-stand`. The word comes before every check of a
  place. A holder of `:see` cannot give either: a give is an act. To raise
  a right, one above it ends it and gives it again (`places`). A permission
  whose slot is a set of key patterns allows acting here; each fact's key
  is held to it among the places.

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
  act would hold his line as well.

  What must hold: the level rows hold the row of each place that
  `levels-needed` named for this act and this permission row."
  [gate-depot act permission-row depot-row level-rows]
  (let [permission (:permission act)
        layer (:layer act)]
    (cond
      (record/holds-value? permission-row permission)
      (cond
        (not (chain/stands? layer permission
                            (assoc level-rows (first (chain/level-places layer permission)) permission-row)))
        :permission-above-has-ended

        (= :see (act/allows permission))
        :permission-does-not-allow-acting

        :else nil)

      (or permission-row depot-row)
      (if (= (:who act) (nth permission 0))
        :permission-does-not-stand
        :who-not-the-permissions-holder)

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
  something to replace, and of each fact under `:softland.store/permission`,
  a give and an end. None for an act that makes its layer: the record holds
  nothing of a layer that is not made, and the two facts of a making act
  are not held to the checks that read a place. A fact that names nothing
  to replace needs no row for these checks, and so can never be stale; a
  claim under a one-write key needs its row for `claims`, which
  `claim-places-needed` names."
  [act makes-the-layer?]
  (if makes-the-layer?
    []
    (into []
          (comp (filter (fn [fact] (or (:replaces fact) (= :softland.store/permission (:k fact)))))
                (map (fn [fact] (place-of act fact))))
          (:facts act))))

(defn- key-fault
  "The first check of the places, for one fact. Gives `:key-not-allowed`
  when what the act's permission allows (`softland.store.act/allows`) does
  not allow the fact's key (`softland.store.act/allows-key?`). A retract is
  held to its key as a value is. A first permission, and one given without
  a list, allow every key."
  [act fact]
  (when-not (act/allows-key? (act/allows (:permission act)) (:k fact))
    :key-not-allowed))

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
  straight beneath the act's own, in the act's layer, that allows no more
  than the act's own: its layer slot is not the act's layer; its granted-in
  slot is not the act's layer; the permission above it is not exactly the
  one that the act acts under; or it allows a key that the act's own does
  not (`softland.store.act/allows-within?`). A permission with nothing
  above it earns the third word."
  [act fact]
  (let [given (:v fact)]
    (when (and (= :softland.store/permission (:k fact)) (some? given))
      (cond
        (not= (:layer act) (nth given 1)) :permission-fact-for-another-layer
        (not= (:layer act) (nth given 2)) :permission-fact-granted-in-another-layer
        (not= (:permission act) (nth given 3 nil)) :permission-fact-not-beneath-the-acts-own
        (not (act/allows-within? (act/allows given) (act/allows (:permission act))))
        :permission-fact-allows-more-than-the-givers
        :else nil))))

(defn who-may-write-fault
  "Who may write a layer's setting, the list of what it builds on, its
  marked keys and its one-write keys: the check, for one fact under one of
  `softland.store.door.declaration/second-write-keys`. Takes the act and
  the fact. Gives nil.

  The key list of a right answers it. Each of the four is a store key like
  any other (src/proposal/sign-in-2026-10-15/BUILD.md, default 12), and the
  first check of the places holds each fact's key to what the act's
  permission allows (`key-fault`). So a holder may write a setting when her
  right allows its key, and a giver can give a right that allows a tool's
  keys and none of the store's. A first permission, and one given without a
  list or with `:see-and-act`, allow every key: any such holder in a layer
  can set it to `:everyone`, and any such holder in the base can set the
  base to `:members`. Sid's word for who writes the setting is \"the
  owner\", and no layer names an owner as a fact. This function stays the
  one place for a rule that the key list cannot say, one that reads more
  than the act and the fact; a word that it gives is recorded as the
  refusal, after check (g)."
  [_act _fact]
  nil)

(defn- end-fault
  "The end of a right, for one retract under `:softland.store/permission`.
  Takes the act, the rows of the places that `places-needed` named, and
  the fact. Gives:

    :no-right-to-end      the fact's place holds no right: it is empty, its
                          row is a retract, or its value is no permission id
    :end-not-from-above   the act's permission is not one of the levels
                          above the right at the place
                          (`softland.store.gate.chain/above?`)

  and nil when the end is taken. The levels above a right are the one who
  gave it, each one above that in its chain, and the first permission of
  the layer, the system's where the system is the chain's top. The holder
  of the right is not above it. The act's own permission stood
  (`standing`), so an ender whose own right has ended ends nothing."
  [act rows fact]
  (let [row (get rows (place-of act fact))
        right (when (holds-a-value? row) (record/value-of row))]
    (cond
      (not (act/has-shape? :permission-id right)) :no-right-to-end
      (chain/above? (:permission act) right) nil
      :else :end-not-from-above)))

(defn- store-key-fault
  "What a store key takes, for one fact. A permission's place takes one
  write while it holds a value, and a retract there is the end of a right
  (`end-fault`). A layer's depot takes one write. The four keys of
  `softland.store.door.declaration/second-write-keys`, who may see a layer,
  what it builds on, its marked keys and its one-write keys, take a second
  write and any later one: such a fact earns no word here, with `:replaces`
  or without. A retract under any other store key is refused. Gives a word
  for: an end that `end-fault` refuses; a fact with no value under a store
  key but a permission's; a depot fact in a layer that is made, with the
  other depot (a change of depot is not decided) or with the layer's own
  (its place holds one); a permission fact on a place that holds a value,
  the same permission or another. A give onto a place whose row is a
  retract is taken. A fact under any other key earns none. Whether a fact
  has a value is asked of a fact under a store key alone, whose `:v` the
  act's one form keeps. What must hold: the layer is made by this gate, so
  its depot is the gate's."
  [gate-depot act rows fact]
  (let [k (:k fact)
        retract? (nil? (:v fact))]
    (cond
      (and (= :softland.store/permission k) retract?) (end-fault act rows fact)
      (and (declaration/store-key? k) retract?) :store-key-retract-not-built
      (and (= :softland.store/depot k) (not= gate-depot (:v fact))) :changing-a-depot-not-decided
      (= :softland.store/depot k) :store-key-takes-one-write
      (and (= :softland.store/permission k) (holds-a-value? (get rows (place-of act fact)))) :store-key-takes-one-write
      :else nil)))

(defn places
  "The checks of the places. Takes the gate's depot; an act; whether it makes
  its layer; and the rows of the places that `places-needed` named, as a map
  from key to row, nil or absent for an empty place. Gives nil, or the word
  of the first check that fails, in this order:

         :key-not-allowed
    (e)  :replaced-fact-not-current
    (f)  :setting-not-on-the-layer
    (g)  :permission-fact-for-another-layer,
         :permission-fact-granted-in-another-layer,
         :permission-fact-not-beneath-the-acts-own,
         :permission-fact-allows-more-than-the-givers
    then what `who-may-write-fault` gives for a fact under a key that takes
         a second write: no word in this build
    then :no-right-to-end, :end-not-from-above, :store-key-retract-not-built,
         :changing-a-depot-not-decided, :store-key-takes-one-write

  An act that makes its layer is held to the key, to (e) and to (f) as any
  act is, with no rows: its permission is a first one, which allows every
  key, and nothing is current in a layer that is not made. It is not held
  to (g) or to the last step: the permission that it writes is its own and
  not one beneath its own, and its depot fact is the layer's first.

  What must hold: `standing` gave nil for the act."
  [gate-depot act makes-the-layer? rows]
  (let [facts (:facts act)]
    (or (some #(key-fault act %) facts)
        (some #(replaced-fact-fault gate-depot act rows %) facts)
        (some #(setting-fault act %) facts)
        (when-not makes-the-layer?
          (or (some #(permission-fact-fault act %) facts)
              (some #(when (contains? declaration/second-write-keys (:k %))
                       (who-may-write-fault act %))
                    facts)
              (some #(store-key-fault gate-depot act rows %) facts))))))

;; ------------------------------------------------------------------- claims

(defn- claim?
  "Takes a fact of an act in its one form on arrival. Gives true when it is
  a claim: a fact with a value under a plain key that names nothing to
  replace. Under a plain key the one form keeps the value's text and no
  `:v`, so the value is told by `:text`."
  [fact]
  (and (some? (:text fact))
       (nil? (:replaces fact))
       (not (declaration/store-key? (:k fact)))))

(defn holds-a-claim?
  "Takes an act, or nil. Gives true when it holds a claim (`claim?`): only
  such an act reads the layer's one-write keys. A retract, a fact that
  names what it replaces, and a fact under a store key are never refused
  by `claims`, so an act of none but those reads nothing for it."
  [act]
  (boolean (some claim? (:facts act))))

(defn one-write-keys-place
  "Takes an act. Gives the key of the place of its layer's one-write keys:
  the layer as entity, under `:softland.store/one-write-keys`."
  [act]
  (record/place-key (:layer act) (:layer act) :softland.store/one-write-keys))

(defn- one-write-keys-of
  "Takes an act and the row at its layer's one-write-keys place, or nil.
  Gives the set of keys that the layer marks as taking one write: the row's
  value, or what the layer starts with when the row holds none
  (`softland.store.gate.first-act/starts-with`), the empty set. A value
  that does not read as a set gives the empty set; no gate writes one."
  [act row]
  (if (holds-a-value? row)
    (let [held (record/value-of row)]
      (if (set? held) held #{}))
    (first-act/starts-with (:layer act) :softland.store/one-write-keys)))

(defn claim-places-needed
  "Takes an act; the row at its layer's one-write-keys place, or nil; and
  the rows already read, as a map from key to row in which a key that was
  read is there also when its place is empty. Gives the keys of the places
  of the act's claims under a key that the row marks, in the act's order,
  that are still to be read. Empty when the layer marks no key of the act.
  The act is decided with the marks as they were before it: a fact of the
  act that writes the layer's one-write keys does not mark its own act."
  [act one-write-row rows-read]
  (let [marked (one-write-keys-of act one-write-row)]
    (if (empty? marked)
      []
      (into []
            (comp (filter claim?)
                  (filter #(contains? marked (:k %)))
                  (map #(place-of act %))
                  (remove #(contains? rows-read %)))
            (:facts act)))))

(defn claims
  "The check of the claims. Takes an act, and the rows read, as a map from
  key to row, nil or absent for an empty place: the row at the layer's
  one-write-keys place (`one-write-keys-place`), and the row of each place
  that `claim-places-needed` named. Gives `:place-takes-one-write` for the
  first claim, in the act's order, under a key that the layer marks as
  taking one write whose place holds a value; else nil.

  A place whose row is a retract holds no value, so it can be claimed
  again. A fact that names the current fact of its place replaces it,
  under such a key too: it is no claim, and check (e) holds it. A retract
  is taken as any retract. So a name is claimed once while it is held, and
  freed by its holder's retract or changed by a fact that names it.

  What must hold: `standing` and `places` gave nil for the act, which does
  not make its layer."
  [act rows]
  (let [marked (one-write-keys-of act (get rows (one-write-keys-place act)))]
    (when (seq marked)
      (some (fn [fact]
              (when (and (claim? fact)
                         (contains? marked (:k fact))
                         (holds-a-value? (get rows (place-of act fact))))
                :place-takes-one-write))
            (:facts act)))))

;; -------------------------------------------------- the marked keys of the layer

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
  accept writes all of them. The end of a right is a fact's row like any
  other: a retract at the right's place.

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
