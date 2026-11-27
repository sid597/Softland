(ns gate-histories.statement
  "The statement to prove of `PERMISSIONS.md`, section 12, held over the
  acts that a gate took. Pure code.

  The statement, in that page's words:

    An act is taken only if it names a give that stood just before it, held
    by its author and allowing that kind of act where it lands; or it is a
    lane's making act; or it ends a give its author holds. Nobody is shown
    an act unless she holds a give that stands and allows see over the
    group it was born into, or she wrote it. Every give leads back, give by
    give, to the making act of its lane. Each giver held what she gave when
    she gave it, and what is lent also needs every give above it to stand
    still.

  This is not the rules (`gate-histories.reference`): it says only what
  must be true of an act that was taken, and nothing of which acts must be
  taken or who may end what. It is written a second time on purpose, as a
  fold of its own over the acts that were taken, and calls no function of
  the rules, so that the rules are held to it as a gate is: a play holds
  every act that the rules take against it, and says of an act that a gate
  took against the rules whether the statement forbids it too.

  What it reads of each act that was taken, in order: its who, the give it
  names, its sitting and its facts. From those alone it keeps which gives
  were made, under what and lent from what, what each allows now, and
  which were ended.

  Its exceptions, named:

    the door       it speaks of what a store answers for acts it has taken,
                   given that the door vouches for each act's author
    everyone       \"held by its author\" includes holding through everyone:
                   a person holds what anyone at all or anyone signed in
                   holds; one who is not signed in holds what anyone at all
                   holds
    she wrote it   a sender is told the answer of her own act; that is the
                   door's, and is held by the run's `told`
    one exact act  that an act under a give for one exact act is that act,
                   and that the give is used once, is the rules' and is
                   held by the run's `agree`
    what a change  a change is held to it for what it adds, not for what it
    adds           leaves
    not here       who may read a lane's acts whole
                   (`:softland.store/who-may-read-acts`), refused acts read
                   by a lane's members, and the open read of the store's
                   own facts: P section 12 leaves them outside the statement

  `taken` takes the fold, the lane and a thing that was taken, and gives
  the fold after and each clause that the thing broke. `may-be-shown?`
  says whether the statement lets an asker be shown the lane."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [gate-histories.reference :as reference]))

(def start
  "The fold before any act: no give, nothing ended."
  {:made? false :gives {} :ended #{}})

(defn- starts-with? [party prefix] (and (keyword? party) (str/starts-with? (name party) prefix)))
(defn- a-person? [party] (starts-with? party "person."))
(defn- an-agent-or-a-run? [party] (or (starts-with? party "agent.") (starts-with? party "run.")))

(defn- hers?
  "True when a party holds a give with this holder, through everyone too.
  `party` nil is one who is not signed in."
  [holder party]
  (or (and (some? party) (= holder party))
      (and (= :anyone holder) (or (nil? party) (a-person? party)))
      (and (= :anyone-signed-in holder) (a-person? party))))

(defn- above
  "The gives above a give, by what each is lent from, the nearest first."
  [fold id]
  (loop [id id out [] hops 0]
    (let [lender (get-in fold [:gives id :lent-from])]
      (if (and lender (< hops 64))
        (recur lender (conj out lender) (inc hops))
        out))))

(defn- stood?
  "True when a give was made, was not ended, and no give above it was
  ended."
  [fold id]
  (and (contains? (:gives fold) id)
       (not-any? #(contains? (:ended fold) %) (cons id (above fold id)))))

(defn- allowed-then
  "What a give allowed at this moment: each right that it and every give
  above it allows."
  [fold id]
  (let [all (map #(get-in fold [:gives % :allows]) (cons id (above fold id)))]
    {:see (every? :see all)
     :write (every? :write all)
     :owner (every? :owner all)
     :give (reduce set/intersection (map #(:give % #{}) all))}))

(defn- held-within?
  "True when every right of `allows` is one that `then` holds."
  [allows then]
  (every? (fn [[right value]]
            (case right
              :see (:see then)
              :write (:write then)
              :owner (:owner then)
              :give (set/subset? value (:give then))
              false))
          allows))

(defn- covers-within?
  [inner outer]
  (and (or (= (:covers inner) (:covers outer))
           (and (= :default-group (first (:covers outer))) (= :exact-act (first (:covers inner)))))
       (or (nil? (:session outer)) (= (:session inner) (:session outer)))))

(defn- kind
  "What a fact is, to the fold: a give, a change, an end, a setting, a
  depot or a plain fact."
  [fold {:keys [e k v]}]
  (cond (= reference/permission-key k) (cond (nil? v) :end
                                             (contains? (:gives fold) e) :change
                                             :else :give)
        (contains? reference/setting-keys k) :setting
        (= reference/depot-key k) :depot
        :else :plain))

(defn- broken-by
  "Each clause of the statement that a thing that was taken broke, as words.
  `fold` is the fold just before it."
  [fold lane thing]
  (let [{:keys [who permission session facts]} thing
        under (get-in fold [:gives permission])
        then (when under (allowed-then fold permission))
        kinds (map #(kind fold %) facts)
        own-end? (fn [fact] (and (= :end (kind fold fact))
                                 (= who (get-in fold [:gives (:e fact) :holder]))))]
    (cond-> []
      ;; an act names a give that stood just before it, held by its author;
      ;; or all it does is end gives that its author holds
      (and (nil? permission) (not (every? own-end? facts)))
      (conj :taken-with-no-give-and-not-only-her-own-ends)

      (and (some? permission) (not (stood? fold permission)))
      (conj :taken-under-a-give-that-did-not-stand)

      (and under (not (hers? (:holder under) who)))
      (conj :taken-under-a-give-she-does-not-hold)

      (and under (some? (:session under)) (not= session (:session under)))
      (conj :taken-outside-its-gives-sitting)

      ;; allowing that kind of act where it lands
      (and under (some #{:plain} kinds) (not (:write then)))
      (conj :wrote-with-no-write)

      (and under (some #{:setting} kinds)
           (not (and (:owner then) (= [:default-group lane] (:covers under)) (nil? (:session under)))))
      (conj :wrote-a-setting-with-no-owning)

      ;; each giver held what she gave when she gave it
      (and under
           (some (fn [fact]
                   (case (kind fold fact)
                     :give (not (and (held-within? (:allows (:v fact)) then)
                                     (covers-within? (:v fact) under)))
                     :change (let [was (get-in fold [:gives (:e fact) :allows])
                                   added (into {}
                                               (keep (fn [[right value]]
                                                       (if (= :give right)
                                                         (let [more (set/difference value (:give was #{}))]
                                                           (when (seq more) [:give more]))
                                                         (when-not (get was right) [right value]))))
                                               (:allows (:v fact)))]
                               (not (held-within? added then)))
                     false))
                 facts))
      (conj :gave-more-than-she-held)

      ;; a give to an agent or a run is lent, so its chain ends at a person
      (some (fn [fact] (and (= :give (kind fold fact))
                            (an-agent-or-a-run? (:holder (:v fact)))
                            (nil? (:lent-from (:v fact)))))
            facts)
      (conj :an-agent-or-a-run-stands-on-its-own)

      ;; what is lent is lent from a give that was made
      (some (fn [fact] (and (= :give (kind fold fact))
                            (some? (:lent-from (:v fact)))
                            (not (contains? (:gives fold) (:lent-from (:v fact))))))
            facts)
      (conj :lent-from-a-give-that-was-never-made))))

(defn taken
  "Takes the fold, the lane and a thing that a gate took. Gives {:fold the
  fold after, :broke the clauses of the statement that the thing broke, as
  words}. The first thing taken is the lane's making act: it breaks nothing
  when it names no give, and the gives that it makes lead back to it."
  [fold lane thing]
  (let [making? (not (:made? fold))
        broke (if making?
                (cond-> [] (some? (:permission thing)) (conj :a-making-act-that-names-a-give))
                (broken-by fold lane thing))
        ;; every give leads back, give by give, to the making act: a give is
        ;; made under a give that was made before it, or by the making act
        leads-nowhere? (and (not making?)
                            (some (fn [fact] (= :give (kind fold fact))) (:facts thing))
                            (not (contains? (:gives fold) (:permission thing))))
        after (reduce (fn [f fact]
                        (case (kind fold fact)
                          :give (assoc-in f [:gives (:e fact)]
                                          (assoc (select-keys (:v fact) [:holder :allows :covers :session :lent-from])
                                                 :made-under (:permission thing)))
                          :change (update-in f [:gives (:e fact)]
                                             #(merge (dissoc % :allows :covers :session)
                                                     (select-keys (:v fact) [:allows :covers :session])))
                          :end (update f :ended conj (:e fact))
                          f))
                      (assoc fold :made? true)
                      (:facts thing))]
    {:fold after
     :broke (cond-> broke leads-nowhere? (conj :a-give-that-leads-nowhere))}))

(defn may-be-shown?
  "Whether the statement lets an asker be shown the lane: she holds a give
  that stands and allows see over the lane's default group, in the sitting
  that she asks in when the give is good for one. `asker` nil is one who is
  not signed in."
  [fold lane asker session]
  (boolean (some (fn [[id g]]
                   (and (hers? (:holder g) asker)
                        (stood? fold id)
                        (:see (allowed-then fold id))
                        (= [:default-group lane] (:covers g))
                        (or (nil? (:session g)) (= session (:session g)))))
                 (:gives fold))))
