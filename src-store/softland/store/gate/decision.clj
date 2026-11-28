(ns softland.store.gate.decision
  "The checks of the decision of one act, after the door's declaration, as
  functions of an act and of rows; and the rows that a decision writes, as
  data. Pure code. No function reads a PState, a clock or a random source:
  a walk reads the rows, calls these, and writes what they give
  (`softland.store.gate.walk`). So the same checks serve each gate, and a
  function gives the same answer when Rama runs an event again.

  An act is an act in its one form on arrival
  (`softland.store.door.declaration/check`). `rows` is a map from the key
  of a place of the facts to its row; a key that was read is in it also
  when its place is empty, with nil. Under `owners-key` it holds a number
  when one was asked for. A gate's depot is `:streaming` or `:microbatch`.

  The checks do not know beforehand which rows they need: a permission
  names the one that it is lent from, and a row must be read to learn it.
  So each of the two steps says what it wants next of rows that `rows` does
  not hold (`standing-wanted`, `places-wanted`), a walk reads those and
  asks again, and when a step wants nothing its check gives the word
  (`standing`, `places`). A step wants a row only when the rows in hand do
  not already refuse the act.

  The order of the checks is the order of the reasons: when an act has two
  faults, the check that comes first gives the recorded word. A check that
  examines facts takes them in the act's order.

  One act is one moment. Each fact is held against the rows as they were
  before the act, and so against what its author held just before it; no
  fact is held against what an earlier fact of the same act writes. Then
  the result is held as a whole (`whole-fault`). All of an act is taken,
  or none of it is written.

  No check reads a row of the other gate's record, the time on the act, or
  a stamp that the act carries. No check reads a lane's marked keys, so a
  refused act pays nothing for them.

  The lane's depot is this gate's with no read, for a lane that is made
  here. A gate's record holds rows only for lanes of its depot: a row is
  written only by an accept; an accept happens in a lane that is made here,
  or makes it; a making act is right only for the gate of the depot that
  the lane starts with; and no act changes a depot. So a row at the place
  of the act's permission, or at the lane's depot place, says that the
  lane is this gate's."
  (:require [clojure.set :as set]
            [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
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

;; ------------------------------------------------------------ what is in hand

(def owners-key
  "Under this key `rows` holds how many owner permissions of the act's lane
  stand on their own: those that allow owning, cover the lane's default
  group in every sitting, are lent from nothing, and were not ended. A gate
  keeps them as a set for each lane and reads its size. The check of the
  whole asks for it only for an act that takes an owner away and makes
  none."
  :owners)

(defn- place-of
  [act fact]
  (record/place-key (:lane act) (:e fact) (:k fact)))

(defn permission-place
  "nil for an act that names no permission."
  [act]
  (when-let [id (:permission act)]
    (chain/place (:lane act) id)))

(defn depot-place
  [act]
  (record/place-key (:lane act) (:lane act) :softland.store/depot))

(defn- lane-is-the-other-gates?
  [gate-depot act]
  (first-act/starts-with-the-other-gates-depot? gate-depot (:lane act)))

(defn- permission-fact?
  [fact]
  (= declaration/permission-key (:k fact)))

(defn- end-fact?
  [fact]
  (and (permission-fact? fact) (nil? (:v fact))))

(defn- missing
  "Of some keys, each once, those that `rows` does not hold."
  [rows ks]
  (into [] (comp (remove nil?) (remove #(contains? rows %)) (distinct)) ks))

(defn- under
  "What the checks of an act's facts hold of the permission that the act
  names, or nil for an act that names none: `:id`; `:says`, what the
  permission says; `:allows`, what it allows at this moment
  (`softland.store.gate.chain/allows-now`); and `:depth`, how many
  permissions its chain holds, its own counted. What must hold: `standing`
  gave nil for the act."
  [act rows]
  (when-let [id (:permission act)]
    (let [{:keys [values]} (chain/climb (:lane act) id rows)]
      {:id id :says (first values) :allows (chain/allows-now values) :depth (count values)})))

;; ----------------------------------------------------------------- standing

(defn- holder-or-sitting-fault
  "What the act's permission says against the act, read from the act and
  the one row."
  [act says]
  (cond
    (not (chain/held-by? (:holder says) (:who act)))
    :who-not-the-permissions-holder

    (and (some? (:session says)) (not= (:session says) (:session act)))
    :permission-for-another-sitting))

(defn- ends-only-her-own?
  "True for an act all of whose facts end a permission whose holder is the
  act's author: the party that the permission names, and nobody who holds
  it through everyone. What must hold: the rows of the act's places are in
  `rows`."
  [act rows]
  (every? (fn [fact]
            (and (end-fact? fact)
                 (= (:who act) (:holder (record/held-permission (get rows (place-of act fact)))))))
          (:facts act)))

(defn standing-wanted
  "The keys that `standing` wants next and `rows` does not hold; empty when
  it has what it needs.

  For an act that names a permission: the row at that permission's place;
  the lane's depot, when that place is empty, to tell a lane that is not
  made from a permission that was never given; and the rows above the
  permission, one at a time, since each names the next. No row above is
  wanted when the first row already refuses the act for its holder or its
  sitting. For an act that names none: the lane's depot; and, in a lane
  that is made, the place of each fact when every fact is an end, to learn
  whether the ended permissions are its author's own."
  [act rows]
  (let [lane (:lane act)]
    (if-let [id (:permission act)]
      (let [k (chain/place lane id)]
        (cond
          (not (contains? rows k)) [k]
          (nil? (get rows k)) (missing rows [(depot-place act)])
          :else (let [says (record/held-permission (get rows k))]
                  (if (or (nil? says) (holder-or-sitting-fault act says))
                    []
                    (missing rows [(:wants (chain/climb lane id rows))])))))
      (let [depot (depot-place act)]
        (cond
          (not (contains? rows depot)) [depot]
          (and (some? (get rows depot)) (every? end-fact? (:facts act)))
          (missing rows (map #(place-of act %) (:facts act)))
          :else [])))))

(defn hex-for-its-permission
  "The digest of the act as its maker made it: in its one form, without its
  act-id, its depot, its time and its permission. A maker makes the same
  text with `softland.store.door.digest/hex-for-a-yes`, for every act that
  the declaration takes, so the two agree under any act-id that she sends
  it under. What must hold: the declaration took the thing. The thing is
  read and not the act, whose facts under a plain key hold their value as
  text alone. nil when the row of the act's permission does not say that
  it covers one exact act, so that no other act pays for the digest."
  [thing act rows]
  (when-let [k (permission-place act)]
    (when (= :exact-act (first (:covers (record/held-permission (get rows k)))))
      (digest/hex-of-the-act thing))))

(defn makes-the-lane?
  "`standing` gives nil for such an act, for an act whose permission
  stands, and for an act that ends only its author's own; this tells the
  first from the others. What must hold: the row of the lane's depot was
  read."
  [gate-depot act rows]
  (boolean (and (nil? (:permission act))
                (nil? (get rows (depot-place act)))
                (not (lane-is-the-other-gates? gate-depot act))
                (first-act/making-act? gate-depot act))))

(defn standing
  "Whether the act may be heard at all: nil, or the first word that holds.
  What must hold: `standing-wanted` gave nothing for these rows, and
  `hex-of-the-act` is what `hex-for-its-permission` gave.

  An act is heard under a permission that stood just before it, held by
  its author; or it makes its lane; or all it does is end permissions that
  its author holds, for which it names none. What the permission allows is
  not asked here: a holder who may only see is heard, and writes nothing
  (`places`).

  The words of an act that names a permission, in the order of the checks.
  `:permission-never-given`: the place of the id that the act names is
  empty, or holds what no gate wrote there. `:permission-ended`: an act
  ended that permission. `:permission-used-up`: it covered one exact act,
  and that act was taken. These three are answered apart, each by what the
  place holds: an id takes one permission for good, so an end at a place
  ended the permission of that id and no other. A place that holds an end
  or a use says nothing more of the permission, so an act under such a one
  gets that word whoever sent it. Then, for a place that holds what a
  permission says: `:who-not-the-permissions-holder`, when the act's
  author is not its holder and does not hold it through everyone
  (`softland.store.gate.chain/held-by?`);
  `:permission-for-another-sitting`, for a permission that is good for one
  sitting and an act that carries another; `:act-not-the-one-permitted`,
  for a permission that covers one exact act and an act that is not that
  act, part for part, its who and its session among them;
  `:permission-above-ended`, when a permission that it is lent from,
  however far up, holds no permission any more.

  The other gate. Whether the lane is the other gate's counts only when
  the record holds nothing of the lane, and no table of the other gate is
  read for it. So an act for a lane that starts with the other depot gets
  `:lane-belongs-to-the-other-gate` whether or not the other gate has made
  the lane, and never `:lane-not-made`. The word cannot follow the other
  gate's record: it would follow an order across two depots, which no
  second run gives again.

  A permission for one exact act covers the one act whose digest it names,
  the act as its maker made it, sent under any act-id of hers. It covers
  that act once: the accept writes its use at its place (`writes`), and
  from then it covers nothing, also the same act under another act-id. A
  try that is refused writes no use, and is sent again under a new act-id,
  since an act-id keeps its first answer. The session and the who are what
  the sending program said: a gate checks no sender.

  An act that was taken under a permission stays taken when the permission
  ends. An act that is decided after the end is refused, the holder's own
  and that of each holder who borrowed beneath her. The end has a stamp in
  the lane's one order, so which acts came before it is the same at each
  run of the gates."
  [gate-depot act rows hex-of-the-act]
  (let [lane (:lane act)
        lane-made? (some? (get rows (depot-place act)))
        not-made-word (fn [] (if (lane-is-the-other-gates? gate-depot act)
                               :lane-belongs-to-the-other-gate
                               :lane-not-made))]
    (if-let [id (:permission act)]
      (let [row (get rows (chain/place lane id))
            says (record/held-permission row)]
        (cond
          (and (nil? row) (not lane-made?)) (not-made-word)
          (nil? row) :permission-never-given
          (record/ended? row) :permission-ended
          (record/used? row) :permission-used-up
          (nil? says) :permission-never-given
          :else
          (or (holder-or-sitting-fault act says)
              (let [[kind hex] (:covers says)]
                (when (and (= :exact-act kind) (not= hex hex-of-the-act))
                  :act-not-the-one-permitted))
              (when-not (chain/stands? lane id rows)
                :permission-above-ended))))
      (cond
        (and (not lane-made?) (lane-is-the-other-gates? gate-depot act)) :lane-belongs-to-the-other-gate
        (and (not lane-made?) (first-act/making-act? gate-depot act)) nil
        (not lane-made?) :lane-not-made
        (ends-only-her-own? act rows) nil
        :else :no-permission))))

;; ------------------------------------------------- what a permission lets do

(defn- within-scope?
  "True when what one permission covers reaches no further than what
  another covers: the same cover, or one exact act where the other is a
  default group; and the other's sitting, when the other is good for one."
  [inner outer]
  (let [[inner-kind :as inner-covers] (:covers inner)
        [outer-kind :as outer-covers] (:covers outer)]
    (boolean (and inner-covers outer-covers
                  (or (= inner-covers outer-covers)
                      (and (= :default-group outer-kind) (= :exact-act inner-kind)))
                  (or (nil? (:session outer)) (= (:session inner) (:session outer)))))))

(defn- allows-within?
  "True when one set of rights allows nothing that another does not."
  [inner outer]
  (every? (fn [[right value]]
            (if (= :give right)
              (set/subset? value (:give outer #{}))
              (some? (get outer right))))
          inner))

(defn- allows-added
  "What one set of rights allows that another does not, in the same form."
  [now before]
  (into {}
        (keep (fn [[right value]]
                (if (= :give right)
                  (let [more (set/difference value (:give before #{}))]
                    (when (seq more) [:give more]))
                  (when-not (get before right) [right value]))))
        now))

(defn- may-pass?
  "Whether one who holds `held` at this moment may pass `rights` on to `to`.
  To see and to write: she holds it, and `to` is an agent or a run, which
  only borrows; or her right to give names it. Passing to everyone, anyone
  at all or anyone signed in, also needs `:everyone` in her right to give.
  The right to give: her own names the right to give, and names everything
  that the new one names, `:everyone` among them. To own: she is an owner,
  and her right to give names owning. So nobody gives what she does not
  hold, and what a holder may pass to another person or to everyone is what
  her right to give says."
  [held rights to]
  (let [may-give (:give held #{})]
    (boolean
     (and (seq rights)
          (or (not (contains? act/everyone to))
              (contains? may-give :everyone))
          (every? (fn [[right value]]
                    (case right
                      (:see :write) (and (get held right)
                                         (or (act/borrower? to) (contains? may-give right)))
                      :give (and (contains? may-give :give) (set/subset? value may-give))
                      :owner (and (:owner held) (contains? may-give :owner))
                      false))
                  rights)))))

(defn- owner-over?
  "True when the act's own permission makes its author an owner over what
  another permission covers."
  [acts-own target]
  (boolean (and acts-own
                (:owner (:allows acts-own))
                (within-scope? target (:says acts-own)))))

(defn- ends-with-no-chain?
  "Three of the four who may end a permission, each read from what the
  permission says and from the act's own: its holder, the party that it
  names, whatever the act names; whoever gave it, in an act under the
  permission that she gave it under, while it stands and still lets her
  pass what the permission holds to its holder; an owner over what it
  covers, in an act under her owner's permission."
  [act acts-own target]
  (let [who (:who act)]
    (boolean (or (= who (:holder target))
                 (and acts-own
                      (= who (:given-by target))
                      (= (:id acts-own) (:under target))
                      (may-pass? (:allows acts-own) (:allows target) (:holder target)))
                 (owner-over? acts-own target)))))

(defn- lender-above
  "The fourth: the holder of a permission that it is lent from, directly or
  further up, in an act under that permission. true, false, or
  `{:wants key}` for a row of the way up that is not read. Whoever holds
  the upper permission through everyone is not its holder. False for a
  permission that fell with an end above it, when the act's own is above
  that end: a row with no value does not say what it was lent from
  (`softland.store.gate.chain`)."
  [act acts-own target-id rows]
  (if (and acts-own (= (:who act) (:holder (:says acts-own))))
    (chain/above? (:lane act) (:id acts-own) target-id rows)
    false))

(defn- may-end?
  [act acts-own target-id target rows]
  (or (ends-with-no-chain? act acts-own target)
      (true? (lender-above act acts-own target-id rows))))

(defn- takes-away?
  "True for a change that takes something away from what a permission
  allows: it then needs what an end needs."
  [target fact]
  (not (allows-within? (:allows target) (:allows (:v fact)))))

(defn- owner-of-the-lane?
  "True for what an owner's permission that stands on its own says."
  [lane says]
  (boolean (and says
                (:owner (:allows says))
                (not (contains? act/everyone (:holder says)))
                (nil? (:lent-from says))
                (= [:default-group lane] (:covers says))
                (nil? (:session says)))))

;; ------------------------------------------------------------------- places

(defn- owners-moved
  "The ids of the owner permissions of the lane that the act takes away, by
  an end or by a change that leaves no owner's, and of those that it makes,
  by a give or by a change: {:out [id ...] :in [id ...]}. `rows` holds the
  row of each place of the act that was read; a place that it does not
  hold reads as empty, as every place of a lane that the act makes."
  [act rows]
  (let [lane (:lane act)]
    (reduce (fn [moved fact]
              (if-not (permission-fact? fact)
                moved
                (let [was? (owner-of-the-lane? lane (record/held-permission (get rows (place-of act fact))))
                      is? (owner-of-the-lane? lane (:v fact))]
                  (cond (and was? (not is?)) (update moved :out conj (:e fact))
                        (and is? (not was?)) (update moved :in conj (:e fact))
                        :else moved))))
            {:out [] :in []}
            (:facts act))))

(defn places-wanted
  "The keys that `places` wants next and `rows` does not hold; empty when
  it has what it needs. What must hold: `standing` gave nil for the act.

  None for an act that makes its lane: the record holds nothing of a lane
  that is not made. Else the place of each fact that names something to
  replace, and of each fact under the permission key. Then, for an end or
  a change that takes away, by an author who is neither the holder, nor
  the giver under the right permission, nor an owner: the rows above the
  permission that it names, one at a time, up to the act's own permission
  or to the top. Last, `owners-key`, for an act that takes an owner of the
  lane away and makes none."
  [gate-depot act rows]
  (if (makes-the-lane? gate-depot act rows)
    []
    (let [facts (:facts act)
          first-keys (missing rows (keep (fn [fact]
                                           (when (or (:replaces fact) (permission-fact? fact))
                                             (place-of act fact)))
                                         facts))]
      (if (seq first-keys)
        first-keys
        (let [acts-own (under act rows)
              chain-keys (missing rows
                                  (keep (fn [fact]
                                          (when (permission-fact? fact)
                                            (when-let [target (record/held-permission (get rows (place-of act fact)))]
                                              (when (and (or (end-fact? fact) (takes-away? target fact))
                                                         (not (ends-with-no-chain? act acts-own target)))
                                                (:wants (lender-above act acts-own (:e fact) rows))))))
                                        facts))]
          (if (seq chain-keys)
            chain-keys
            (let [{:keys [out in]} (owners-moved act rows)]
              (if (and (seq out) (empty? in))
                (missing rows [owners-key])
                []))))))))

(defn- replaced-fact-fault
  "Stale when the place is empty, another fact wrote its row, or the fact
  named is of another lane or depot."
  [gate-depot act rows fact]
  (when-let [replaces (:replaces fact)]
    (when-not (record/written-by? gate-depot (:lane act) (get rows (place-of act fact)) replaces)
      :replaced-fact-not-current)))

(defn- setting-fault
  [act fact]
  (when (and (contains? declaration/setting-keys (:k fact))
             (not= (:e fact) (:lane act)))
    :setting-not-on-the-lane))

(defn- give-fault
  "For a fact that gives a permission: a value at an id whose place is
  empty. `acts-own` is `under`'s.

  A give says who gave it, the act's author, and under what, the
  permission that the act names: so both are in what the permission says
  from the start, and a change after leaves them readable. It is lent from
  the permission that it is made under, or it stands on its own; from no
  third one. What is made under a lent permission is lent from it, and a
  give to an agent or a run is always lent: so every chain ends at a
  permission that a person, the system or everyone holds, and only a
  permission that stands on its own makes another that does. Anyone at all
  holds seeing and nothing more, and anyone signed in holds seeing and
  writing and nothing more: everyone holds no right to give and never owns.
  A give covers no more than the
  permission that it is made under, and allows no more than that one
  allows at this moment (`may-pass?`). A chain of lent permissions is no
  longer than `softland.store.gate.chain/max-lent-depth`."
  [act acts-own fact]
  (let [says (:v fact)
        lent-from (:lent-from says)]
    (cond
      (not= (:who act) (:given-by says)) :permission-not-given-by-the-acts-who
      (not= (:id acts-own) (:under says)) :permission-not-under-the-acts-own
      (and lent-from (not= lent-from (:id acts-own))) :permission-not-lent-from-the-acts-own
      (and (:lent-from (:says acts-own)) (nil? lent-from)) :made-under-a-lent-permission-is-lent
      (and (act/borrower? (:holder says)) (nil? lent-from)) :an-agent-or-a-run-only-borrows
      (and (= :anyone (:holder says)) (not= {:see true} (:allows says))) :anyone-only-sees
      (and (= :anyone-signed-in (:holder says)) (not-every? #{:see :write} (keys (:allows says)))) :anyone-signed-in-only-sees-and-writes
      (not (within-scope? says (:says acts-own))) :permission-covers-more-than-the-acts-own
      (not (may-pass? (:allows acts-own) (:allows says) (:holder says))) :permission-gives-more-than-its-giver-may
      (and lent-from (<= chain/max-lent-depth (:depth acts-own))) :permission-lent-too-deep
      :else nil)))

(defn- change-fault
  "For a fact with a value at an id that holds a permission: a change of
  what that permission allows. The id, the holder, the giver, the
  permission that it was given under and the one that it is lent from stay.
  So its holder stays in throughout, and nothing that she lent is ended:
  what she lent is capped by what she holds at each moment
  (`softland.store.gate.chain/allows-now`).

  To take something away needs what an end needs. To add something needs
  what a give of what is added, to that holder, in that permission's place,
  needs: a right is added to a lent permission only in an act under the
  permission that it is lent from, and to one that stands on its own only
  in an act under one that stands on its own; the permission covers no
  more than the act's own; and the act's own lets its author pass on what
  is added (`may-pass?`). Without the first two a holder would move what a
  permission for one sitting, or a lent one, allows her into a permission
  that is good in every sitting, or that outlives the lender. A change
  that does both needs both.

  Not built: a change of what a permission covers, or of its sitting. What
  is lent from it would have to be capped by what it covers at each
  moment, and its key by holder moved. A change that changes nothing is
  refused: it would write a row, and name a new writer of it, for one who
  was held to nothing."
  [act acts-own target fact rows]
  (let [says (:v fact)
        kept [:holder :given-by :under :lent-from]]
    (cond
      (not= (mapv #(get target %) kept) (mapv #(get says %) kept))
      :a-change-keeps-holder-giver-and-lender

      (not= [(:covers target) (:session target)] [(:covers says) (:session says)])
      :changing-what-a-permission-covers-not-built

      (= (:allows target) (:allows says))
      :change-changes-nothing

      (and (= :anyone (:holder says)) (not= {:see true} (:allows says)))
      :anyone-only-sees

      (and (= :anyone-signed-in (:holder says)) (not-every? #{:see :write} (keys (:allows says))))
      :anyone-signed-in-only-sees-and-writes

      (and (takes-away? target fact) (not (may-end? act acts-own (:e fact) target rows)))
      :may-not-lower-the-permission

      :else
      (let [added (allows-added (:allows says) (:allows target))
            lender (:lent-from target)]
        (cond
          (empty? added) nil
          (and lender (not= lender (:id acts-own))) :permission-not-lent-from-the-acts-own
          (and (nil? lender) (:lent-from (:says acts-own))) :made-under-a-lent-permission-is-lent
          (not (within-scope? target (:says acts-own))) :permission-covers-more-than-the-acts-own
          (not (may-pass? (:allows acts-own) added (:holder says))) :permission-gives-more-than-its-giver-may
          :else nil)))))

(defn- end-fault
  "For a fact with no value under the permission key: the end of the
  permission at that id. `row` is the row at the fact's place.

  `:no-permission-to-end`: the id holds no permission and never did.
  `:permission-already-ended` and `:permission-already-used`: an end is
  final, and a use leaves nothing to end. The three are answered apart.

  Four may end a permission, and nobody else: its holder, whatever it
  allows and whatever her act names; whoever gave it, in an act under the
  permission that she gave it under, while it still lets her pass what the
  permission holds to its holder; the holder of a permission that it is
  lent from, directly or further up, in an act under that permission; an
  owner over what it covers, in an act under her owner's permission. The
  act's own permission has passed its standing, so each of the last three
  still holds what she ends by. A permission that fell with one above it
  is ended by the same four, as far as the rows say who they are
  (`lender-above`).

  The fact needs no `:replaces`. With one, the check of replaced facts
  holds it to the fact that it names: an end that was sent for a
  permission as one act left it is then refused when another act has
  changed it since."
  [act acts-own row fact rows]
  (let [target (record/held-permission row)]
    (cond
      (nil? row) :no-permission-to-end
      (record/ended? row) :permission-already-ended
      (record/used? row) :permission-already-used
      (nil? target) :no-permission-to-end
      (may-end? act acts-own (:e fact) target rows) nil
      :else :may-not-end-the-permission)))

(defn- fact-fault
  "What one fact of an act in a lane that is made is refused for, or nil.

  Under a plain key: a fact is written only under a permission that allows
  writing at this moment. A setting that takes a second write: any owner of
  the lane writes it, and nobody else. A lane's depot takes one write: in a
  lane that is made, a depot fact is refused with the other depot and with
  the lane's own. A retract under a store key other than a permission's is
  not built. An id takes one permission for good: a value at an id whose
  permission was ended or used is refused, and given again a permission
  has another id.

  Whether a fact has a value is asked of a fact under a store key alone,
  whose `:v` the act's one form keeps. What must hold: the lane is made by
  this gate, so its depot is the gate's."
  [gate-depot act acts-own rows fact]
  (let [k (:k fact)
        retract? (nil? (:v fact))
        row (get rows (place-of act fact))]
    (cond
      (and (permission-fact? fact) retract?) (end-fault act acts-own row fact rows)
      (and (permission-fact? fact) (nil? acts-own)) :no-permission
      (and (permission-fact? fact) (nil? row)) (give-fault act acts-own fact)
      (permission-fact? fact) (if-let [target (record/held-permission row)]
                                (change-fault act acts-own target fact rows)
                                :a-permission-id-takes-one-permission)
      (and (declaration/store-key? k) retract?) :store-key-retract-not-built
      (and (= :softland.store/depot k) (not= gate-depot (:v fact))) :changing-a-depot-not-decided
      (= :softland.store/depot k) :store-key-takes-one-write
      (contains? declaration/second-write-settings k)
      (when-not (owner-over? acts-own {:covers [:default-group (:lane act)]})
        :only-an-owner-writes-a-setting)
      (not (:write (:allows acts-own))) :permission-does-not-allow-writing
      :else nil)))

(defn- whole-fault
  "The result of an act, held as a whole.

  No permission that the act gives is left lent from one that the same act
  ends: it would be dead as it is made.

  A lane always has an owner: an act that would leave it with no owner's
  permission that stands on its own is refused. So the lane's first
  permission ends like any other, in an act that leaves an owner, and a
  whole lane is handed over in one act, which gives the new owner hers and
  ends the old one's. `rows` holds the count of the owners only when the
  act takes one away and makes none (`places-wanted`); a count that is not
  there, or that a reader gave as nil, reads as none."
  [act rows]
  (let [ended-here (into #{} (comp (filter end-fact?) (map :e)) (:facts act))
        {:keys [out in]} (owners-moved act rows)]
    (cond
      (some (fn [fact]
              (and (permission-fact? fact)
                   (some? (:v fact))
                   (nil? (get rows (place-of act fact)))
                   (contains? ended-here (:lent-from (:v fact)))))
            (:facts act))
      :permission-lent-from-one-the-act-ends

      (and (seq out) (empty? in) (<= (long (or (get rows owners-key) 0)) (count out)))
      :lane-would-have-no-owner

      :else nil)))

(defn places
  "The checks of an act's facts and of its result as a whole: nil, or the
  first word that holds. What must hold: `standing` gave nil for the act,
  and `places-wanted` gave nothing for these rows.

  A fact that names something to replace, and a setting that is not on the
  lane, are refused in every act. An act that makes its lane is held to
  nothing more: its facts are the lane's first, and
  `softland.store.gate.first-act/making-act?` held their form."
  [gate-depot act rows]
  (let [facts (:facts act)]
    (or (some #(replaced-fact-fault gate-depot act rows %) facts)
        (some #(setting-fault act %) facts)
        (when-not (makes-the-lane? gate-depot act rows)
          (let [acts-own (under act rows)]
            (or (some #(fact-fault gate-depot act acts-own rows %) facts)
                (whole-fault act rows)))))))

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

(defn- use-of-its-permission
  "The row by which an accept uses up the permission for one exact act that
  the act was taken under: a row with no value at the permission's place,
  which no fact wrote (`softland.store.record.rows/use-row`). So the
  permission stands no more, an act under it is refused
  `:permission-used-up`, it has nothing left to end, and its id takes no
  permission again.

  No fact of the act wrote the row, so it names the act and no fact of it:
  every fact id that a row of the facts table names is the id of a fact
  that its act holds, which a reader of the act finds at that index. [] for
  an act that ends the permission itself: its own row at the place is that
  end. An act that changes it writes the change's row and then this one at
  the same place, which is the one that stays.

  The row is made of the act and its stamp alone, so a second run of the
  gates writes it again, the same, on both gates."
  [act act-uuid stamp time-millis who-text permission-text rows]
  (let [k (permission-place act)]
    (if (and k
             (= :exact-act (first (:covers (record/held-permission (get rows k)))))
             (not-any? #(and (end-fact? %) (= k (place-of act %))) (:facts act)))
      [[:fact k (record/use-row act-uuid stamp time-millis who-text permission-text)]]
      [])))

(defn- permission-writes
  "What a give, a change and an end write beside the fact's own row, in the
  act's order.

  `[:holder-permission key stamp]` puts a permission that covers a lane's
  default group in the order by holder
  (`softland.store.record.rows/by-holder-key`), with the decision's stamp
  at both its give and each change of it, and `[:holder-permission key nil]`
  takes it out at its end. So a read finds the permissions of a holder that
  were not ended with no id in hand, and a push on the holder's run sees an
  in-place change too.

  `[:owner id true]` puts a permission among the owners of the act's lane
  and `[:owner id nil]` takes it out (`owners-key`). Each is the same write
  however often it runs."
  [act stamp rows]
  (let [lane (:lane act)]
    (into []
          (mapcat (fn [fact]
                    (when (permission-fact? fact)
                      (let [row (get rows (place-of act fact))
                            was (record/held-permission row)
                            is (:v fact)
                            by-holder (fn [says] (record/by-holder-key lane (:holder says) (:session says) (:e fact)))
                            in-a-group? (fn [says] (= :default-group (first (:covers says))))
                            was-owner? (owner-of-the-lane? lane was)
                            is-owner? (owner-of-the-lane? lane is)]
                        (cond-> []
                          (and is (in-a-group? is)) (conj [:holder-permission (by-holder is) (long stamp)])
                          (and was (nil? is) (in-a-group? was)) (conj [:holder-permission (by-holder was) nil])
                          (and is-owner? (not was-owner?)) (conj [:owner (:e fact) true])
                          (and was-owner? (not is-owner?)) (conj [:owner (:e fact) nil]))))))
          (:facts act))))

(defn- accept-writes
  "In the act's order; then what its gives, changes and ends write beside
  their rows; then the use of its permission. Each fact holds the text of
  its value, from the declaration's check. `in-hand` is what `writes`
  takes."
  [act act-uuid stamp time-millis {:keys [since-by-place place-rows level-from]}]
  (let [lane (:lane act)
        who-text (declaration/id-text (:who act))
        permission-text (some-> (:permission act) declaration/id-text)]
    (-> []
        (into (comp
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
        (into (permission-writes act stamp place-rows))
        (into (use-of-its-permission act act-uuid stamp time-millis who-text permission-text place-rows)))))

(defn writes
  "Each row that one decision writes in the record, as [which key row].
  `act` is nil for a thing that the declaration refused; `refused` is nil
  for an accept; `digest` is 32 bytes; `in-hand` is read only for an
  accept:

    :since-by-place  what `since-by-place` gave
    :place-rows      every row that the checks read for the act, the row
                     at the place of its permission among them; and the
                     row before of each place of `:since-by-place`, and of
                     the marked-keys place when the act writes it
    :level-from      the stamp that a newly marked key gets: the stamp of
                     the act, in both gates. So a mark holds from the next
                     act of the lane

  A `:by-value` or `:holder-permission` row that is nil says that the key
  is to be taken out. `[:last-stamp nil n]` names no key, and `[:owner id
  true-or-nil]` names a permission: the walk writes each for the act's
  lane.

  The answer holds the time when the thing has a usable one, a refused
  thing too. Each row of a fact and of the facts by value holds the time of
  the act that wrote it, so a read gives a fact's time with no second read.

  Every row is whole, at a fixed key, and a key that is taken out is fixed
  too; so each write needs no read. An accept under a permission for one
  exact act can write two rows at that permission's place, and the walk
  writes them in the order given. No row holds anything but what the
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

(def max-kept-weight
  "32 MiB, in weight (`softland.store.door.weight`): the heaviest thing that
  a gate keeps a second time, for the read by stamp, when its sender has
  standing (`kept-bound`). Every act that passes the declaration weighs
  less, unless its decimals hold more digits than their text says. A thing
  over the bound is in its depot and in no read by stamp."
  33554432)

(def max-kept-weight-with-no-standing
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

(defn held-through-everyone?
  "True for an act under a permission whose holder is everyone: its author
  is no party that the permission names. What must hold: the row at the
  place of the act's permission is in `rows`."
  [act rows]
  (boolean (when-let [k (permission-place act)]
             (contains? act/everyone (:holder (record/held-permission (get rows k)))))))

(defn kept-bound
  "A sender has standing when the declaration took the thing and `standing`
  gave nil: the act's permission stands and is hers, a holder's who may
  only see among them, or the act makes its lane or ends only her own.

  Whoever holds a permission through everyone has that standing only for
  an act that is taken. A gate checks no sender, so on a lane that is open
  to everyone any program holds everyone's permission: were a refused act
  of such a sender kept at the larger bound, anybody could make the task
  write and hold the bytes of a large thing twice."
  [declarations-reason refused-standing refused through-everyone?]
  (if (and (nil? declarations-reason)
           (nil? refused-standing)
           (or (nil? refused) (not through-everyone?)))
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
