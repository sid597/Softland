(ns gate-histories.reference
  "The rules about permissions of the permission fix, as the plainest code
  that says them. Pure code. It shares nothing with a gate: it reads no row,
  no stamp and no digest.

  Written from `src/proposal/permissions-2026-11-27/BUILD.md`, batches 1 to
  4 and the clarifications that Sid confirmed at its end; from
  `PERMISSIONS.md` beside it, sections 6 and 12; and, for the shapes alone,
  from `CONTRACT.md`. Where those say nothing, the rule is today's: batch 1
  says \"everything else is as today\", and the statements of the store's
  piece 2 (`gate-histories.piece-2.reference`) say what today is.

  It keeps, for one lane, what a person would keep on paper: each give that
  was taken, by its id, with what it says now; which gives an act ended;
  which give for one exact act was used; and the one answer of each act-id.

  The words. A give is a permission with an id of its own. Its holder is a
  person, an agent, a run, or everyone: `:anyone`, which is anyone at all,
  and `:anyone-signed-in`. It allows some of see, write, give (with what
  may be given on) and owner. It covers the lane's default group or one
  exact act, for any sitting or for one. It is lent from another give, or
  stands on its own. It says who gave it and under which give.

  Each rule of the gate is one entry of `rules`: a word, the sentence in
  plain words, where the sentence comes from, and the test that an act
  must not meet. A gate under test can be this code with one entry taken
  out or one function replaced (`gate-histories.run`, the broken copies);
  so every sentence here is a function or an entry of its own, and none is
  folded into another.

  `judge` takes the state and one thing as it arrived, and gives whether
  the rules take the act, the first reason when they do not, the answer
  that its sender is to be told, and the state after. `may-see` says
  whether an asker may see the lane, read once. `unsettled` names a thing
  on which BUILD.md does not settle the answer: a run does not send it.
  `sentences` gives every sentence, for a person to read.

  What it does not hold: the form of a thing (the declaration's), a fact
  that names something to replace, who may read a lane's acts whole
  (`:softland.store/who-may-read-acts`, which this build leaves as it is),
  who may make a lane for another holder, and what a live read tells while
  it stays open."
  (:require [clojure.set :as set]
            [clojure.string :as str]))

;; ------------------------------------------------------------------ the words

(def permission-key "The key that a give sits under, on its own id." :softland.store/permission)
(def depot-key "The key of a lane's depot, written by its making act." :softland.store/depot)

(def setting-keys
  "The keys of a lane's settings."
  #{:softland.store/who-may-read-acts :softland.store/builds-on :softland.store/marked-keys})

(def max-lent-depth
  "How many gives a chain of lent gives holds at most, the lowest and the one
  that stands on its own among them (CONTRACT.md, section 2.3)."
  32)

(def everyone "The two holders that are everyone." #{:anyone :anyone-signed-in})

(defn kind-of
  "What a party is, from its id: `:person`, `:agent`, `:run`, `:everyone`
  or `:system`; nil for anything else."
  [party]
  (cond (contains? everyone party) :everyone
        (= :system party) :system
        (keyword? party) (let [n (name party)]
                           (cond (str/starts-with? n "person.") :person
                                 (str/starts-with? n "agent.") :agent
                                 (str/starts-with? n "run.") :run))))

(defn person? "True for a person." [party] (= :person (kind-of party)))
(defn borrower? "True for an agent or a run: one who only ever borrows." [party]
  (contains? #{:agent :run} (kind-of party)))

(def ^:dynamic *act-named*
  "Takes the text by which a give for one exact act names its act. Gives the
  act that its giver said yes to, as its maker made it, or nil for a text
  that names no act that anybody made. It is what a history knows of each
  such give, written down when it was made: a person who says yes to an act
  knows the act. The rules hold an act to it part for part (`the-act`), and
  never compute a digest."
  (constantly nil))

(def start
  "The state before the lane is made.

    :made?    whether the lane is made
    :gives    id -> the give there, as it says now: `:holder`, `:allows`,
              `:covers`, `:session`, `:given-by`, `:under`, `:lent-from`;
              and `:id`, and `:n`, its number in the order of the gives
    :ended    the ids of the gives that an act ended
    :used     the ids of the gives for one exact act under which an act was
              taken
    :answers  act-id -> {:sent the first thing decided under it, as sent
              (`as-sent`), :answer its answer}"
  {:made? false :gives {} :ended #{} :used #{} :answers {} :n 0})

;; --------------------------------------------------- what stands, and what it holds

(defn give "The give at an id as it says now, or nil." [state id] (get-in state [:gives id]))
(defn ended? "True when an act ended the give at this id." [state id] (contains? (:ended state) id))
(defn used? "True when an act was taken under this give for one exact act." [state id]
  (contains? (:used state) id))

(defn lent-chain
  "The ids of the gives above a give: the one that it is lent from, the one
  that that one is lent from, and on up, the nearest first. Empty for a
  give that stands on its own."
  [state id]
  (loop [id id above []]
    (let [lender (:lent-from (give state id))]
      (if (and lender (< (count above) (* 2 max-lent-depth)))
        (recur lender (conj above lender))
        above))))

(defn stands?
  "True when the give at this id stands: it was given, no act ended it, and
  no give above it, however far up, was ended. A give that stands on its
  own has nothing above it, so nothing that happens to the one who gave it
  makes it fall."
  [state id]
  (and (some? (give state id))
       (every? #(not (ended? state %)) (cons id (lent-chain state id)))))

(defn- meet-allows
  "What a lent give allows at this moment: what it says, and no more than
  its lender allows. Takes what the give allows and what its lender allows."
  [mine lenders]
  (let [may-give (set/intersection (:give mine #{}) (:give lenders #{}))]
    (cond-> {}
      (and (:see mine) (:see lenders)) (assoc :see true)
      (and (:write mine) (:write lenders)) (assoc :write :all)
      (seq may-give) (assoc :give may-give)
      (and (:owner mine) (:owner lenders)) (assoc :owner true))))

(defn holds-now
  "What a give allows and covers at this moment: {:allows :covers :session},
  or nil for a give that does not stand. What it allows is what it says,
  capped by each give above it as that one says now: lower a lender and
  what she lent is capped, raise her again and it is uncapped."
  [state id]
  (when (stands? state id)
    (let [own (give state id)]
      {:allows (reduce (fn [allows lender] (meet-allows allows (:allows (give state lender))))
                       (:allows own)
                       (lent-chain state id))
       :covers (:covers own)
       :session (:session own)})))

(defn covers-party?
  "True when a give with this holder is one that a party holds: she is its
  holder; or its holder is anyone at all and she is a person, or is not
  signed in (`party` nil); or its holder is anyone signed in and she is a
  person. An agent or a run is never everyone on its own."
  [holder party]
  (boolean (or (and (some? party) (= holder party))
               (and (= :anyone holder) (or (nil? party) (person? party)))
               (and (= :anyone-signed-in holder) (person? party)))))

(defn- within-covers?
  "True when one cover reaches no further than another: the same, or one
  exact act where the other is the default group."
  [inner outer]
  (boolean (and inner outer
                (or (= inner outer)
                    (and (= :default-group (first outer)) (= :exact-act (first inner)))))))

(defn within-scope?
  "True when what one give covers reaches no further than what another
  covers: its cover within the other's, and its sitting the other's when
  the other is good for one sitting. Takes two maps with `:covers` and
  `:session`."
  [inner outer]
  (and (within-covers? (:covers inner) (:covers outer))
       (or (nil? (:session outer)) (= (:session inner) (:session outer)))))

(defn allows-within?
  "True when one give allows nothing that another does not."
  [inner outer]
  (every? (fn [[right value]]
            (if (= :give right)
              (set/subset? value (:give outer #{}))
              (some? (get outer right))))
          inner))

(defn allows-added
  "What one give allows that another does not, as a map of the same form."
  [new old]
  (into {}
        (keep (fn [[right value]]
                (if (= :give right)
                  (let [more (set/difference value (:give old #{}))]
                    (when (seq more) [:give more]))
                  (when-not (get old right) [right value]))))
        new))

(defn may-pass?
  "Whether one who holds `held` at this moment may pass `allows` on to `to`.
  See and write: she holds it, and `to` is an agent or a run, which only
  borrows; or her right to give names it. The right to give: her own right
  to give names the right to give, and names everything that the new right
  names. Owner: she is an owner and her right to give names owning."
  [held allows to]
  (let [may-give (:give held #{})]
    (boolean
     (and (seq allows)
          (every? (fn [[right value]]
                    (case right
                      (:see :write) (and (get held right) (or (borrower? to) (contains? may-give right)))
                      :give (and (contains? may-give :give) (set/subset? value may-give))
                      :owner (and (:owner held) (contains? may-give :owner))
                      false))
                  allows)))))

;; ------------------------------------------------------------- who may end a give

(defn ends-as-holder?
  "Its holder: the party that the give names as its holder. A member of
  everyone is not the holder of everyone's give."
  [_state who _under target]
  (= who (:holder target)))

(defn ends-as-giver?
  "Whoever gave it, while she still holds what she gave it under: her act
  names that give, which stands."
  [_state who under target]
  (boolean (and under (= who (:given-by target)) (= (:id under) (:under target)))))

(defn ends-as-lender?
  "Whoever is the holder of a give that it is lent from, directly or
  further up: her act names that give. Being covered by everyone does not
  make her the holder of everyone's give."
  [state who under target]
  (boolean (and under
                (= who (:holder under))
                (some #{(:id under)} (lent-chain state (:id target))))))

(defn ends-as-owner?
  "Whoever holds everything over what it covers: her act names her owner's
  give."
  [state _who under target]
  (boolean (and under
                (let [held (holds-now state (:id under))]
                  (and (:owner (:allows held)) (within-scope? target held))))))

(defn may-end?
  "Whether `who`, in an act that names the give `under` (nil for none), may
  end the give `target`: the four of item 18, and nobody else."
  [state who under target]
  (or (ends-as-holder? state who under target)
      (ends-as-giver? state who under target)
      (ends-as-lender? state who under target)
      (ends-as-owner? state who under target)))

(defn has-owner?
  "True when the lane has an owner's give that stands on its own: one that
  allows owning, covers the lane's default group for any sitting, is lent
  from nothing, and stands."
  [state lane]
  (boolean (some (fn [[id g]]
                   (and (:owner (:allows g))
                        (nil? (:lent-from g))
                        (= [:default-group lane] (:covers g))
                        (nil? (:session g))
                        (stands? state id)))
                 (:gives state))))

;; ----------------------------------------------------------------- an act's parts

(def ^:private left-out-parts
  "The parts of an act that its maker may leave out, with what a part that
  is left out says."
  {:based-on {} :because-of nil :claimed-when nil :subjects #{}})

(defn the-act
  "An act part for part, as the rules compare two acts. Takes a thing as it
  arrived, or an act as its maker made it, or nil. Gives each of its parts
  but the four that are no part of the act that a give for one exact act
  names: its act-id, its depot, the time on it, and its permission. A part
  that its maker left out reads as that part says when it is empty; its
  facts come as a vector, each fact with its four parts, a part left out as
  nil. nil for nil."
  [thing]
  (when thing
    (-> (merge left-out-parts (dissoc thing :permission :act-id :depot :door-time))
        (update :facts (fn [facts] (mapv (fn [fact] (merge {:v nil :replaces nil} fact)) facts))))))

(defn as-sent
  "A thing as the rule `once` compares two sends under one act-id: the act
  part for part (`the-act`), and the give that it names."
  [thing]
  (assoc (the-act thing) :permission (:permission thing)))

(defn fact-kind
  "What one fact of an act is, in the state just before the act: `:give`, a
  value at an id that holds no give; `:change`, a value at an id that holds
  one; `:end`; `:setting`; `:depot`; or `:plain`."
  [state fact]
  (let [{:keys [e k v]} fact]
    (cond (= permission-key k) (cond (nil? v) :end
                                     (give state e) :change
                                     :else :give)
          (contains? setting-keys k) :setting
          (= depot-key k) :depot
          :else :plain)))

(defn- own-end?
  "True for a fact that ends a give whose holder is `who`."
  [state who fact]
  (and (= permission-key (:k fact))
       (nil? (:v fact))
       (some? (give state (:e fact)))
       (= who (:holder (give state (:e fact))))))

(defn- names-this-act?
  "True when the thing is the act that a give for one exact act names, part
  for part."
  [under thing]
  (let [named (*act-named* (second (:covers under)))]
    (and (some? named) (= (the-act named) (the-act thing)))))

(defn- exact-act? [g] (= :exact-act (first (:covers g))))

(defn- lowers? "True when a change takes away something that the give allows."
  [target v] (not (allows-within? (:allows target) (:allows v))))

(defn- raises? "True when a change adds something that the give does not allow."
  [target v] (not (allows-within? (:allows v) (:allows target))))

;; ------------------------------------------------------------------- the rules

(def rules
  "Each rule of the gate. `:of` says what it is asked of: `:act`, the act as
  a whole before its facts; a kind of fact (`fact-kind`); or `:whole`, the
  result of the act as a whole. `:refuses?` takes what is in hand:

    :state :lane :thing :who   the state just before the act, and the act
    :under                     the give that the act names, or nil
    :holds                     what that give allows and covers at this
                               moment (`holds-now`), or nil
    :fact :id :v :target       for a fact: itself, its `:e`, its `:v`, and
                               the give at that id before the act
    :after                     for `:whole`: the state after the act

  and gives true when the act must be refused for it. `:from` says where
  the sentence comes from. `:alone?` false marks a rule whose refusals
  every other rule would also make; it is here for its word."
  [{:of :act :word :names-no-give :alone? false
    :from "BUILD batch 1, line 1; P section 12, point 5"
    :says "An act names the give it acts under. An act that names none is taken only when it makes its lane, or when all it does is end gives that its author holds."
    :refuses? (fn [{:keys [state thing who]}]
                (and (nil? (:permission thing))
                     (not (and (seq (:facts thing)) (every? #(own-end? state who %) (:facts thing))))))}
   {:of :act :word :give-never-given :alone? false
    :from "BUILD batch 1, line 3 (bug 3)"
    :says "An act under a give that was never given is refused, and is answered apart from an act under a give that ended."
    :refuses? (fn [{:keys [thing under]}] (and (some? (:permission thing)) (nil? under)))}
   {:of :act :word :not-its-holder
    :from "P section 12, the statement to prove; BUILD batch 4, line 1 (item 20)"
    :says "An act is taken only under a give that its author holds: she is its holder, or she is a person and its holder is everyone. An agent or a run is never everyone on its own: it comes in through its person."
    :refuses? (fn [{:keys [who under]}] (and under (not (covers-party? (:holder under) who))))}
   {:of :act :word :another-sitting
    :from "BUILD batch 1, line 5: a per-run permission is a give good for one login sitting"
    :says "A give good for one sitting takes an act only in that sitting."
    :refuses? (fn [{:keys [thing under]}]
                (and under (some? (:session under)) (not= (:session under) (:session thing))))}
   {:of :act :word :not-the-act-named
    :from "P section 12, point 10; as today"
    :says "A give that covers one exact act takes that act and no other, part for part."
    :refuses? (fn [{:keys [thing under]}] (and under (exact-act? under) (not (names-this-act? under thing))))}
   {:of :act :word :give-does-not-stand :alone? false
    :from "P section 12, the statement to prove; BUILD batch 2, line 1 (item 5)"
    :says "An act is taken only under a give that stood just before it: no act ended it, and when it is lent, no give above it, however far up, was ended."
    :refuses? (fn [{:keys [state under]}] (and under (not (stands? state (:id under)))))}
   {:of :act :word :give-used
    :from "P section 12, point 10; as today"
    :says "A give that covers one exact act is used up by the act taken under it: no second act is taken under it."
    :refuses? (fn [{:keys [state under]}] (and under (used? state (:id under))))}

   {:of :plain :word :may-not-write
    :from "P section 6, item 2: the two atoms, see and write"
    :says "A fact is written only under a give that allows write at that moment. Whoever may only see writes nothing."
    :refuses? (fn [{:keys [holds]}] (not (:write (:allows holds))))}
   {:of :setting :word :only-an-owner-writes-a-setting
    :from "BUILD batch 3, line 4 (items 6 and 21); the clarification on who-may-read-acts"
    :says "A lane's setting is written only under an owner's give, and any owner writes it."
    :refuses? (fn [{:keys [holds lane]}]
                (not (and (:owner (:allows holds))
                          (= [:default-group lane] (:covers holds))
                          (nil? (:session holds)))))}
   {:of :depot :word :a-depot-is-written-once
    :from "as today"
    :says "A lane's depot is written once, by its making act."
    :refuses? (constantly true)}

   {:of :give :word :a-give-says-who-gave-it
    :from "P section 12, the record: given by, the author of the give act"
    :says "A give says who gave it, and that is the author of the act that makes it."
    :refuses? (fn [{:keys [who v]}] (not= who (:given-by v)))}
   {:of :give :word :a-give-is-under-the-give-its-act-names
    :from "P section 12, the record: under, the give that act named; today's \"straight beneath the giver's own\""
    :says "A give is made under the give that its act names, and says so."
    :refuses? (fn [{:keys [under v]}] (not= (:id under) (:under v)))}
   {:of :give :word :lent-from-the-give-it-is-made-under
    :from "BUILD batch 2, lines 1 and 2; a session's reading of \"unless its giver lends it\""
    :says "A give is lent from the give it is made under, or it stands on its own."
    :refuses? (fn [{:keys [under v]}] (and (some? (:lent-from v)) (not= (:lent-from v) (:id under))))}
   {:of :give :word :made-under-a-lent-give-is-lent
    :from "BUILD batch 2, line 3"
    :says "What is made under a lent give is lent from it. Only a give that stands on its own makes another that stands on its own."
    :refuses? (fn [{:keys [under v]}] (and (some? (:lent-from under)) (nil? (:lent-from v))))}
   {:of :give :word :an-agent-or-a-run-only-borrows
    :from "BUILD batch 2, line 2 (item 5)"
    :says "A give to an agent or a run is always lent, so its chain ends at a give that a person or everyone holds."
    :refuses? (fn [{:keys [v]}] (and (borrower? (:holder v)) (nil? (:lent-from v))))}
   {:of :give :word :anyone-only-sees
    :from "BUILD batch 4, line 1 (item 20): anyone at all, for reading"
    :says "A give to anyone at all allows seeing and nothing more. Writing by everyone is a give to anyone signed in, because an act needs an author."
    :refuses? (fn [{:keys [v]}] (and (= :anyone (:holder v)) (not= {:see true} (:allows v))))}
   {:of :give :word :covers-more-than-the-give-above
    :from "BUILD batch 2, line 4"
    :says "A give never covers more than the give it is made under: the same lane; the same sitting, when that give is good for one; the same exact act, when that give covers one."
    :refuses? (fn [{:keys [holds v]}] (not (within-scope? v holds)))}
   {:of :give :word :gives-more-than-she-may
    :from "BUILD batch 2, lines 4 and 5 (items 2 and 19)"
    :says "A give never allows more than the give it is made under allows at that moment. Passing see or write to a person, or to everyone, needs the right to give it; lending see or write to an agent or a run needs only holding it. The right to give says what may be given on: the right to give itself, and owning, are passed on only by one whose own right to give names them."
    :refuses? (fn [{:keys [holds v]}] (not (may-pass? (:allows holds) (:allows v) (:holder v))))}
   {:of :give :word :lent-too-deep
    :from "CONTRACT section 2.3: max-lent-depth"
    :says "A chain of lent gives holds 32 gives at most."
    :refuses? (fn [{:keys [state under v]}]
                (and (some? (:lent-from v))
                     (<= max-lent-depth (inc (count (lent-chain state (:id under)))))))}

   {:of :change :word :a-place-takes-one-give
    :from "BUILD batch 1, line 3 (bug 3)"
    :says "An id takes one give for good. Nothing is given at an id whose give was ended or used: given again, it is a new give with a new id."
    :refuses? (fn [{:keys [state id]}] (or (ended? state id) (used? state id)))}
   {:of :change :word :a-change-keeps-holder-lender-and-giver
    :from "BUILD batch 3, line 2; \"what is settled before you start\"; P section 12, point 2"
    :says "A change keeps a give's id and replaces what it allows, never its holder, what it is lent from, who gave it or what it was given under: those stay readable after a change."
    :refuses? (fn [{:keys [target v]}]
                (not= (select-keys target [:holder :lent-from :given-by :under])
                      (select-keys v [:holder :lent-from :given-by :under])))}
   {:of :change :word :lowering-needs-what-ending-needs
    :from "BUILD batch 3, line 2"
    :says "Lowering a give needs what ending it needs: only one who may end it takes anything away from it."
    :refuses? (fn [{:keys [state who under target v]}]
                (and (lowers? target v) (not (may-end? state who under target))))}
   {:of :change :word :raising-needs-what-giving-needs
    :from "BUILD batch 3, line 2"
    :says "Raising a give needs what giving needs: only one who may give its holder what is added adds it."
    :refuses? (fn [{:keys [holds target v]}]
                (and (raises? target v)
                     (not (may-pass? (:allows holds) (allows-added (:allows v) (:allows target)) (:holder v)))))}

   {:of :end :word :never-given
    :from "BUILD batch 1, lines 1 and 3 (bug 3)"
    :says "An end names the give it ends. An end of a give that was never given is refused, and is answered apart from an end of one that had ended."
    :refuses? (fn [{:keys [target]}] (nil? target))}
   {:of :end :word :already-ended
    :from "P section 12, point 2: an end is final"
    :says "A give is ended once: an end is final."
    :refuses? (fn [{:keys [state id]}] (ended? state id))}
   {:of :end :word :already-used
    :from "as today"
    :says "A give for one exact act that was used has nothing left to end."
    :refuses? (fn [{:keys [state id]}] (used? state id))}
   {:of :end :word :not-one-who-may-end-it
    :from "BUILD batch 2, line 6 (item 18); batch 1, line 4 (bug 4); the clarification on \"holds\""
    :says "These may end a give, and nobody else: its holder, whatever it allows; whoever gave it, in an act under the give she gave it under, while that give stands; the holder of a give it is lent from, directly or further up, in an act under that give; an owner of the lane, in an act under her owner's give. Being covered by everyone makes nobody the holder of everyone's give."
    :refuses? (fn [{:keys [state who under target]}]
                (and (some? target) (not (may-end? state who under target))))}

   {:of :whole :word :a-give-is-changed-once-in-an-act
    :from "BUILD batch 3, line 1"
    :says "No give is changed twice in one act: an act names an id of a give in one fact at most."
    :refuses? (fn [{:keys [thing]}]
                (let [ids (keep #(when (= permission-key (:k %)) (:e %)) (:facts thing))]
                  (boolean (and (seq ids) (not (apply distinct? ids))))))}
   {:of :whole :word :left-lent-from-a-give-the-act-ends
    :from "BUILD batch 3, line 1"
    :says "No give that an act makes is left lent from a give that the same act ends."
    :refuses? (fn [{:keys [state thing]}]
                (let [ended-here (into #{} (keep #(when (= :end (fact-kind state %)) (:e %))) (:facts thing))]
                  (boolean (some #(and (= :give (fact-kind state %))
                                       (contains? ended-here (:lent-from (:v %))))
                                 (:facts thing)))))}
   {:of :whole :word :a-lane-keeps-an-owner
    :from "BUILD batch 3, line 5 (item 21)"
    :says "A lane always has an owner: an act that would leave it with no owner's give that stands on its own is refused. The lane's first give ends like any other in an act that leaves an owner, so a whole lane is handed over in one act."
    :refuses? (fn [{:keys [after lane]}] (not (has-owner? after lane)))}])

(defn- reason-of
  "The word of the first rule of a kind that refuses what is in hand, or
  nil."
  [of in-hand]
  (some (fn [rule] (when (and (= of (:of rule)) ((:refuses? rule) in-hand)) (:word rule)))
        rules))

;; --------------------------------------------------------------------- judging

(defn- apply-fact
  "The state after one fact of an act that the rules took. A give is put at
  its id. A change replaces what the give allows and covers. An end ends."
  [state thing fact]
  (let [{:keys [e v]} fact]
    (case (fact-kind state fact)
      :give (-> state
                (assoc-in [:gives e] (assoc v :id e :n (:n state)))
                (update :n inc))
      :change (update-in state [:gives e]
                         #(merge (dissoc % :allows :covers :session)
                                 (select-keys v [:allows :covers :session])))
      :end (update state :ended conj e)
      state)))

(defn- after-the-act
  "The state after an act that the rules took: each of its facts, and the
  use of the give for one exact act that it was taken under."
  [state thing under]
  (cond-> (reduce #(apply-fact %1 thing %2) state (:facts thing))
    (and under (exact-act? under)) (update :used conj (:id under))))

(defn- makes-the-lane?
  "True for a lane's making act: it names no give, and holds the lane's
  depot and the lane's first give, an owner's give that its author gives,
  under nothing, lent from nothing, over the lane's default group."
  [lane thing]
  (let [{:keys [who permission facts]} thing
        depot (first (filter #(= depot-key (:k %)) facts))
        v (:v (first (filter #(= permission-key (:k %)) facts)))]
    (boolean (and (nil? permission)
                  (= 2 (count facts))
                  depot (= lane (:e depot))
                  v
                  (= who (:given-by v))
                  (nil? (:under v))
                  (nil? (:lent-from v))
                  (person? (:holder v))
                  (:owner (:allows v))
                  (= [:default-group lane] (:covers v))
                  (nil? (:session v))))))

(defn- fact-reason
  "Why the rules refuse one fact of an act, or nil. Each fact is held
  against the state just before the act: against what its author held
  then."
  [in-hand fact]
  (let [state (:state in-hand)
        kind (fact-kind state fact)]
    (reason-of kind (assoc in-hand
                           :fact fact :id (:e fact) :v (:v fact)
                           :target (when (= permission-key (:k fact)) (give state (:e fact)))))))

(defn- judge-afresh
  "The rules' judgment of a thing under an act-id that has no answer. Gives
  {:accept? :why :state}, and `:under`, the give that an act that was taken
  named. Every fact is held against the state just before the act; then the
  result is held as a whole; all of it is taken, or none."
  [state lane thing]
  (if-not (:made? state)
    (if (makes-the-lane? lane thing)
      {:accept? true :state (assoc (after-the-act state thing nil) :made? true)}
      {:accept? false :why :lane-not-made :state state})
    (let [under (when-let [id (:permission thing)] (give state id))
          in-hand {:state state :lane lane :thing thing :who (:who thing)
                   :under under
                   :holds (when under (holds-now state (:id under)))}
          after (after-the-act state thing under)
          why (or (reason-of :act in-hand)
                  (some #(fact-reason in-hand %) (:facts thing))
                  (reason-of :whole (assoc in-hand :after after)))]
      (if why
        {:accept? false :why why :state state}
        {:accept? true :under under :state after}))))

(defn judge
  "Takes the state, the lane's id, and a thing as it arrived: `:act-id`,
  `:who`, `:permission`, `:session` and `:facts`, each fact with `:e`, `:k`
  and `:v`. Gives {:accept? :why :answer :state}: whether the rules take
  it now, the first reason when they do not, the answer that its sender is
  to be told, {:answer :accepted}, {:answer :refused :reason word} or
  {:answer :not-mine}, and the state after.

  One answer for an act-id, by Sid's decision of 7 October 2026 and as
  today: under an act-id that has an answer nothing is decided and
  `:accept?` is false. For the same thing, `:repeat?` is true and `:answer`
  is its first answer; for another thing, `:not-mine?` is true and the
  answer is \"not mine\"."
  [state lane thing]
  (let [act-id (:act-id thing)
        sent (as-sent thing)
        first-of (get-in state [:answers act-id])]
    (cond
      (and first-of (= sent (:sent first-of)))
      {:accept? false :repeat? true :answer (:answer first-of) :state state}

      first-of
      {:accept? false :not-mine? true :answer {:answer :not-mine} :state state}

      :else
      (let [{:keys [accept? why] :as ruled} (judge-afresh state lane thing)
            answer (if accept? {:answer :accepted} {:answer :refused :reason why})]
        (-> ruled
            (assoc :answer answer)
            (assoc-in [:state :answers act-id] {:sent sent :answer answer}))))))

;; ------------------------------------------------------------------- the read

(defn sees-under?
  "True when this give lets an asker see the lane, read once: it stands, it
  allows see at this moment, it covers the lane's default group, she holds
  it (`covers-party?`), and when it is good for one sitting she asks in
  that sitting. `asker` nil is one who is not signed in."
  [state lane asker session id]
  (boolean (and (covers-party? (:holder (give state id)) asker)
                (let [held (holds-now state id)]
                  (and held
                       (:see (:allows held))
                       (= [:default-group lane] (:covers held))
                       (or (nil? (:session held)) (= session (:session held))))))))

(defn may-see
  "Whether an asker may see the lane, read once: `:not-made` before the
  lane is made, else `:yes` or `:may-not-see`. `asker` is a party, or nil
  for one who is not signed in; `session` is the sitting that she asks in,
  or nil."
  [state lane asker session]
  (cond (not (:made? state)) :not-made
        (some #(sees-under? state lane asker session %) (keys (:gives state))) :yes
        :else :may-not-see))

;; ------------------------------------------------------- what BUILD.md leaves open

(defn unsettled
  "A word for a thing whose answer BUILD.md does not settle, or nil. A run
  does not send such a thing, so no gate is held to the judge's guess on
  it. Most are of a change of a give that was not ended or used:

    :a-change-of-what-a-give-covers
        BUILD says a change replaces what a give allows or covers. What a
        give lent from it then covers is not said, and no history walks it
    :a-change-that-changes-nothing
        a value at an id that says what the give there says already
    :a-change-of-a-give-that-fell
        a change of a lent give after a give above it was ended
    :a-raise-by-one-who-may-give-the-raise-and-not-the-whole
        raising needs what giving needs: of what is added, or of the whole
        give after it; here the two differ
    :an-act-that-ends-a-give-and-changes-one-lent-from-it
        whether \"no give left lent from one the same act ends\" reaches a
        give that the act only changes

  and one is of a give:

    :a-give-under-or-lent-from-a-give-that-the-same-act-makes
        BUILD says that changes may name what the same act makes. The
        rules hold a give to the give that its act names, which stood
        before the act; whether a give may be under or lent from one that
        its own act makes, as a making act that also opens its lane might
        say it, is the builder's"
  [state lane thing]
  (when (:made? state)
    (let [under (when-let [id (:permission thing)] (give state id))
          holds (when under (holds-now state (:id under)))
          ended-here (into #{} (keep #(when (= :end (fact-kind state %)) (:e %))) (:facts thing))
          made-here (into #{} (keep #(when (= :give (fact-kind state %)) (:e %))) (:facts thing))]
      (some (fn [fact]
              (if (and (= :give (fact-kind state fact))
                       (or (contains? made-here (:under (:v fact)))
                           (contains? made-here (:lent-from (:v fact)))))
                :a-give-under-or-lent-from-a-give-that-the-same-act-makes
              (when (and (= :change (fact-kind state fact))
                         (not (ended? state (:e fact)))
                         (not (used? state (:e fact))))
                (let [target (give state (:e fact))
                      v (:v fact)]
                  (cond
                    (or (not= (:covers target) (:covers v)) (not= (:session target) (:session v)))
                    :a-change-of-what-a-give-covers

                    (= (:allows target) (:allows v))
                    :a-change-that-changes-nothing

                    (not (stands? state (:e fact)))
                    :a-change-of-a-give-that-fell

                    (and (raises? target v)
                         (not= (may-pass? (:allows holds) (allows-added (:allows v) (:allows target)) (:holder v))
                               (may-pass? (:allows holds) (:allows v) (:holder v))))
                    :a-raise-by-one-who-may-give-the-raise-and-not-the-whole

                    (contains? ended-here (:lent-from target))
                    :an-act-that-ends-a-give-and-changes-one-lent-from-it)))))
            (:facts thing)))))

;; ------------------------------------------------------------- for a person to read

(def definitions
  "The sentences that are no single test of an act: what the words mean,
  and what holds of every act. Each is a function of this namespace, named
  under `:in`."
  [{:in "stands?" :from "BUILD batch 2, line 1 (item 5); batch 1, line 6"
    :says "A give stands from the act that makes it until an act ends it. A lent give stands only while every give above it stands: it falls with the give it is lent from. A give that stands on its own does not fall when the one who gave it loses hers."}
   {:in "holds-now" :from "BUILD batch 2, line 1; batch 3, line 3 (bug 2)"
    :says "A lent give never allows more than the give it is lent from allows at that moment. Lowering or raising someone leaves her in throughout and ends nothing she lent: what she lent is capped by what she holds at that moment, and uncapped when she is raised again."}
   {:in "has-owner?, the owner preset" :from "BUILD batch 3, line 4 (items 6 and 21)"
    :says "Owning is a give that allows everything over a lane: seeing, writing, its settings, giving all of it on, ending any give over it. It can be given. Several owners each act alone, and one owner may end another owner's give while an owner remains."}
   {:in "judge-afresh" :from "BUILD batch 3, line 1 (item 7)"
    :says "One act is one moment. Every change in it is checked against what its author held just before the act; then the result is checked as a whole. All of it is taken, or none of it is kept."}
   {:in "makes-the-lane?" :from "CONTRACT section 2, the making act; P section 12, point 15"
    :says "A lane's making act names no give. It writes the lane's depot and the lane's first give: an owner's give that stands on its own."}
   {:in "judge" :from "Sid, 7 October 2026; as today"
    :says "An act-id names one act and gets one answer, whoever sent first. The same thing sent again is told that answer and is not decided again; another thing under it is told that it is not hers."}
   {:in "may-see, sees-under?" :from "P section 12, the statement to prove; BUILD batch 4, lines 1 and 2 (item 20)"
    :says "An asker sees a lane exactly when a give that stands and allows see at that moment covers the lane's default group for her: she is its holder; or its holder is anyone at all, and she is a person or is not signed in; or its holder is anyone signed in, and she is a signed-in person. A give good for one sitting lets her see only when she asks in that sitting. A give that covers one exact act lets nobody see the lane. An agent or a run sees only under a give of its own. Seen by everyone is such a give, written in the lane's order; ending it closes the lane. No setting of the lane and no value in code lets anyone see."}])

(defn sentences
  "Every sentence of the rules, in order, each as {:says :from :at}: `:at`
  is the rule's word, or the function that says it."
  []
  (into (mapv (fn [d] {:says (:says d) :from (:from d) :at (:in d)}) definitions)
        (map (fn [r] {:says (:says r) :from (:from r) :at (name (:word r))}))
        rules))

;; ------------------------------------------------------------------ for a trace

(defn standing-word
  "What an id holds now, in a word: `:never-given`, `:ended`, `:used`,
  `:fell` for a lent give under an ended one, or `:stands`."
  [state id]
  (cond (nil? (give state id)) :never-given
        (ended? state id) :ended
        (used? state id) :used
        (stands? state id) :stands
        :else :fell))
