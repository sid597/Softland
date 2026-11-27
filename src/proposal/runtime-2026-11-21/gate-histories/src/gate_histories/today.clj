(ns gate-histories.today
  "The store as it is before the permission fix, as a gate under test for
  histories of the new form: each thing of a history is said again in
  today's form and sent through the store's own door and gate
  (`gate-histories.walk`), and the lane is read through the store's own
  read path. Pure code: no cluster. Nothing of the store is changed.

  Why. The rules (`gate-histories.reference`) are written for a store that
  is not built yet. Batch 1 of BUILD.md says that everything the batches
  do not change is as today, and that today's behaviour is a value in the
  new record: every give lent from the give above it. So on today's
  default values the rules and the store of today must answer alike,
  except where a batch changes the answer. This namespace is that check:
  what today's store answers, beside what the rules answer, with each
  difference held to a reason of
  `src/proposal/permissions-2026-11-27/ALLOWLIST.md`.

  How a thing is said in today's form:

    a party         its id without its kind: `:person.ben` is `:ben`
    a give          a permission in today's nested form, at today's place:
                    on its holder for the lane, on the sitting under
                    `:softland.store/run-permission` for one sitting, on
                    its own id under `:softland.store/act-permission` for
                    one exact act; the give above it is the one that it is
                    under, and it allows `:see` or `:see-and-act`
    a give to       the lane's setting `:softland.store/who-may-see`
    anyone at all   written `:everyone`; its end, the setting written
                    `:members`
    an end          the same fact with no value, at the give's place
    an act          names the permission that its give is today; an act
                    that names no give and ends its author's own gives
                    goes under the first of those gives itself

  What today's form cannot say is not sent, to the store or to the rules,
  and is counted by its word (`:not-today`): a give that is not lent from
  the give above it, a give that today's two words cannot say, an owner's
  give, a give to anyone signed in, an act under everyone's give, two
  facts at one of today's places, an end or a change of a give that is not
  the one at its place today, and an act that names a give which today's
  form cannot tell from one given at its place after it.

  `excuse` and `see-excuse` name the reason of the allowlist that a
  difference falls under, or nil for a difference that no reason covers.
  Each reason is held by a test of the thing and of what stood, not by the
  two answers alone: so a difference that the rules make by mistake is not
  excused because a reason of the list is near it."
  (:require [clojure.string :as str]
            [gate-histories.histories :as h]
            [gate-histories.reference :as reference]
            [gate-histories.walk :as gate]
            [softland.store.act :as act]
            [softland.store.door.digest :as digest]
            [softland.store.gate.chain :as chain]
            [softland.store.gate.decision :as decision]
            [softland.store.read-path.lanes :as lanes]
            [softland.store.record.rows :as record]))

(def ^:private now "The time on each thing, as the door adds it. No rule reads it." 1700000000000)
(def ^:private lane h/lane)

(def reasons
  "The reasons of ALLOWLIST.md that a difference between the rules and
  today's store can fall under on today's default values, each with what
  it says here. The other reasons of that list are of the form (B1 form,
  B1 place, B1 rows, B1 digest, making, S1 party, table), which the saying
  again in today's form bridges; of the live read's sources (S5 rights);
  or of values that today's form cannot say (B2 lent, B2 end, B3 owner),
  which are not sent."
  {"B1 many" "a holder can have any number of gives on one lane: today a give to a holder whose permission stands, or for a sitting that has one, is refused"
   "B1 again" "given again is a new give with a new id, and an id takes one give for good: today the same permission is given again at its place after an end"
   "B1 own end" "a holder ends her own give, a holder of see too: today she ends it only under a permission that lets her act, her own for the lane or the permission itself"
   "B2 give" "whoever holds see or write lends it to an agent or a run with no right to give: today a holder of see gives nothing, and a permission for one run or one act gives nothing"
   "B3 change" "what a give allows is changed in place: today a give to a holder whose permission stands is refused"
   "B3 one act" "the result of an act is held as a whole: today an act that ends a permission and gives one beneath it is taken, and what it gave is dead at once"
   "B4 public" "seen by everyone is a give to everyone: today it is a setting that the first holder alone writes, and it lets an agent or a run see as everyone"})

;; --------------------------------------------------- a give, in today's form

(defn- old-name
  "A party's id in today's form: without its kind."
  [party]
  (let [n (name party)]
    (keyword (subs n (inc (str/index-of n "."))))))

(defn- scope-kind
  "What a give covers, as today's form has it: `:lane`, `:sitting` or
  `:act`; nil for what today's form cannot say."
  [v]
  (cond (= :exact-act (first (:covers v))) (when (nil? (:session v)) :act)
        (not= [:default-group lane] (:covers v)) nil
        (some? (:session v)) :sitting
        :else :lane))

(defn- old-word
  "Today's word for what a give allows, or nil when today has no word for
  it: for the lane, see is `:see` and a member's give `:see-and-act`; for
  one sitting, see is `:see` and see and write `:see-and-act`; for one
  exact act, write is `:see-and-act`."
  [v]
  (let [allows (:allows v)]
    (case (scope-kind v)
      :lane (cond (= allows (h/allows-of :see)) :see
                  (= allows (h/allows-of :member)) :see-and-act)
      :sitting (cond (= allows (h/allows-of :see)) :see
                     (= allows (h/allows-of :see-write)) :see-and-act)
      :act (when (= allows (h/allows-of :write)) :see-and-act)
      nil)))

(defn- place-of
  "Today's place of a give: [:public] for a give to anyone at all, else
  [:lane holder], [:run sitting] or [:act id]."
  [v id]
  (if (= :anyone (:holder v))
    [:public]
    (case (scope-kind v)
      :lane [:lane (old-name (:holder v))]
      :sitting [:run (:session v)]
      :act [:act id]
      nil)))

(def ^:private place-key
  {:lane :softland.store/permission :run :softland.store/run-permission :act :softland.store/act-permission})

(defn- never-old
  "A permission in today's form that was never given and cannot stand:
  beneath the first permission of a holder that no history has."
  [who]
  [who lane lane [:nobody lane lane]])

(defn- yes-can-say?
  "True for a fact of an act that a give for one exact act names which
  reads the same in today's form whenever it is sent: a plain fact, a
  setting, a depot, or the end of that give itself."
  [yes fact]
  (if (= reference/permission-key (:k fact))
    (and (nil? (:v fact)) (= yes (:e fact)))
    true))

(defn- old-text
  "The text by which today's form names the act of a give for one exact
  act: the store's own digest of the act in today's form, as a maker asks
  it (`softland.store.door.digest/hex-for-a-yes`). nil when today's form
  cannot say the act."
  [named yes v]
  (when-let [maker (named (second (:covers v)))]
    (when (and (keyword? (:who maker)) (every? #(yes-can-say? yes %) (:facts maker)))
      (digest/hex-for-a-yes
       {:version act/version :who (old-name (:who maker)) :lane lane :session (:session maker)
        :facts (mapv (fn [fact]
                       (if (= reference/permission-key (:k fact))
                         {:e yes :k :softland.store/act-permission}
                         fact))
                     (:facts maker))}))))

(defn- old-give
  "A give, or a value at an id that holds one, in today's form. Gives {:fact
  :place :old-id}, or {:not-today word}. `named-id` is the id of the give
  that its act names. Today's form has one slot for the give above: a new
  give is said with the give that it is under there, which must be the one
  that it is lent from; a value at an id that holds a give keeps the give
  above of the give there. A give to anyone at all is a setting, which has
  no such slot: it can be said only under the give that its act names."
  [gs ref named who named-id id v]
  (let [was (reference/give ref id)
        public-now (get-in gs [:latest [:public]])]
    (cond
      (or (= :anyone-signed-in (:holder v)) (= :anyone-signed-in (:holder was)))
      {:not-today :a-give-to-anyone-signed-in}

      (and (nil? was) (not= who (:given-by v)))
      {:not-today :a-give-that-says-another-gave-it}

      (and (nil? was) (not= (:lent-from v) (:under v)))
      {:not-today :a-give-not-lent-from-the-give-above}

      (and was (not= (select-keys was [:lent-from :given-by :under]) (select-keys v [:lent-from :given-by :under])))
      {:not-today :a-change-that-says-another-giver-or-give-above}

      (and (= :anyone (:holder v)) (not= (:under v) named-id))
      {:not-today :a-give-to-anyone-under-another-give}

      (and was (or (not= (place-of was id) (place-of v id))
                   (= [:public] (place-of v id))
                   (not= id (get-in gs [:latest (place-of was id)]))))
      {:not-today :a-change-of-a-give-that-is-not-the-one-at-its-place}

      (= :anyone (:holder v))
      (cond (not (and (= {:see true} (:allows v)) (= :lane (scope-kind v))))
            {:not-today :a-give-to-anyone-of-more-than-seeing-the-lane}
            (and public-now (reference/stands? ref public-now))
            {:not-today :a-second-give-to-anyone}
            :else
            {:fact {:e lane :k :softland.store/who-may-see :v :everyone} :place [:public]})

      (nil? (old-word v))
      {:not-today :a-give-that-todays-two-words-cannot-say}

      :else
      (let [holder (old-name (:holder v))
            above (or (get-in gs [:old-id (if was (:lent-from v) (:under v))]) (never-old :nobody))
            [kind e :as place] (place-of v id)
            old-id (case kind
                     :lane [holder lane lane above (old-word v)]
                     :run [holder lane lane above (old-word v) [:run e]]
                     :act (when-let [text (old-text named id v)]
                            [holder lane lane above :see-and-act [:act id text]]))]
        (if old-id
          {:fact {:e e :k (place-key kind) :v old-id} :place place :old-id old-id}
          {:not-today :a-give-for-an-act-that-today-cannot-say})))))

(defn- old-end
  "An end in today's form: the fact with no value at the give's place. Gives
  {:fact :place}, or {:not-today word}."
  [gs ref id]
  (let [was (reference/give ref id)]
    (cond
      (nil? was)
      {:fact {:e id :k :softland.store/act-permission} :place [:act id]}

      (= :anyone-signed-in (:holder was))
      {:not-today :a-give-to-anyone-signed-in}

      (= :anyone (:holder was))
      (if (and (= id (get-in gs [:latest [:public]])) (reference/stands? ref id))
        {:fact {:e lane :k :softland.store/who-may-see :v :members} :place [:public]}
        {:not-today :an-end-of-a-give-to-anyone-that-does-not-stand})

      (not= id (get-in gs [:latest (place-of was id)]))
      {:not-today :an-end-of-a-give-that-is-not-the-one-at-its-place}

      :else
      (let [[kind e :as place] (place-of was id)]
        {:fact {:e e :k (place-key kind)} :place place}))))

(defn- old-fact
  "One fact in today's form: {:fact :place}, and `:old-id` and `:id` for a
  give; or {:not-today word}."
  [gs ref named who named-id fact]
  (let [{:keys [e k v]} fact]
    (cond (not= reference/permission-key k) {:fact fact :place [:fact e k]}
          (nil? v) (old-end gs ref e)
          :else (assoc (old-give gs ref named who named-id e v) :id e))))

(defn- old-permission
  "The permission that an act names, in today's form: {:permission p}, or
  {:not-today word}. An act that names no give and ends its author's own
  gives goes under the first of those gives itself. An act under a give
  that was never given goes under a permission that was never given."
  [gs ref who sent]
  (let [named-id (:permission sent)
        own-ends? (and (seq (:facts sent))
                       (every? (fn [{:keys [e k v]}]
                                 (and (= reference/permission-key k) (nil? v)
                                      (= who (:holder (reference/give ref e)))))
                               (:facts sent)))
        id (or named-id (when own-ends? (:e (first (:facts sent)))))
        was (reference/give ref id)]
    (cond
      (and (nil? named-id) (not own-ends?)) {:not-today :an-act-that-names-no-give}
      (nil? was) {:permission (never-old (old-name who))}
      (contains? reference/everyone (:holder was)) {:not-today :an-act-under-everyones-give}
      :else
      (let [mine (get-in gs [:old-id id])
            there (get-in gs [:latest (place-of was id)])]
        (if (or (= id there) (not= mine (get-in gs [:old-id there])))
          {:permission mine}
          {:not-today :an-act-under-a-give-that-today-cannot-tell-from-the-one-after-it})))))

(defn- first-give-of
  "The fact of a making act that gives the lane's first give, or nil for a
  thing that is no making act: it names no give, and holds two facts, a
  depot and an owner's give to a person, under nothing and lent from
  nothing."
  [sent]
  (when (and (nil? (:permission sent))
             (= 2 (count (:facts sent)))
             (some #(= reference/depot-key (:k %)) (:facts sent)))
    (first (filter (fn [{:keys [k v]}]
                     (and (= reference/permission-key k) (some? v)
                          (nil? (:under v)) (nil? (:lent-from v))
                          (reference/person? (:holder v))
                          (= (h/allows-of :owner) (:allows v))))
                   (:facts sent)))))

(defn- said-today
  "A thing in today's form: {:maker the act as its maker hands it to
  today's door, :lands [place id old-id] for each give of it}, or
  {:not-today word}. A making act is today's making act: it goes under the
  first permission that it gives, on its holder."
  [gs ref named sent]
  (let [who (:who sent)]
    (if-let [first-give (first-give-of sent)]
      (let [holder (old-name (:holder (:v first-give)))
            first-old [holder lane lane]]
        {:maker {:version act/version :who (old-name who) :lane lane :session (:session sent)
                 :permission first-old
                 :facts (mapv (fn [fact]
                                (if (identical? fact first-give)
                                  {:e holder :k :softland.store/permission :v first-old}
                                  fact))
                              (:facts sent))}
         :lands [[[:lane holder] (:e first-give) first-old]]})
      (let [permission (old-permission gs ref who sent)
            facts (mapv #(old-fact gs ref named who (:permission sent) %) (:facts sent))
            places (map :place facts)]
        (or (when (:not-today permission) permission)
            (first (filter :not-today facts))
            (when-not (apply distinct? places) {:not-today :two-facts-at-one-of-todays-places})
            {:maker {:version act/version :who (old-name who) :lane lane :session (:session sent)
                     :permission (:permission permission) :facts (mapv :fact facts)}
             :lands (vec (keep (fn [{:keys [id place old-id fact]}]
                                 (when (and id (some? (:v fact))) [place id old-id]))
                               facts))})))))

;; ---------------------------------------------------------------- today's read

(defn- rows-at [facts ks] (into {} (map (fn [k] [k (get facts k)])) ks))

(defn- seeing
  "What today's read path says of one asker and the lane, from the record:
  the lane's own rows, her permission row and the rows above it, and, for
  a read asked in a sitting, the row of the sitting's permission and the
  rows above it (`softland.store.read-path.lanes`)."
  [facts who session signed-in?]
  (let [own-rows (into {}
                       (keep (fn [k] (let [place (record/place-key lane lane k)]
                                       (when-let [row (get facts place)] [place row]))))
                       [:softland.store/depot :softland.store/who-may-see
                        :softland.store/builds-on :softland.store/marked-keys])
        above-of (fn [row] (if-let [held (record/held-permission row)]
                             (rows-at facts (chain/places-above lane held))
                             {}))
        permission-row (get facts (record/place-key lane who :softland.store/permission))
        run-row (when session (get facts (record/place-key lane session :softland.store/run-permission)))]
    (lanes/standing lane own-rows
                    (lanes/either (lanes/permission-state lane permission-row (above-of permission-row))
                                  (if session
                                    (lanes/run-permission-state lane who run-row (above-of run-row))
                                    :none))
                    signed-in?)))

;; ---------------------------------------------------------------- the excuses

(defn excuse
  "The reason of the allowlist that a difference on a thing falls under, or
  nil. Takes the gate's state and the rules' state before the thing, the
  thing, the rules' judgment and the store's outcome."
  [gs ref sent ruled outcome]
  (let [word (:verdict outcome)
        took? (= :accepted word)
        who (:who sent)
        facts (:facts sent)
        kind #(reference/fact-kind ref %)
        under (reference/give ref (:permission sent))
        holds (when under (reference/holds-now ref (:id under)))
        own-end? (fn [fact] (and (= :end (kind fact))
                                 (= who (:holder (reference/give ref (:e fact))))))
        lends? (fn [fact] (and (= :give (kind fact)) (reference/borrower? (:holder (:v fact)))))]
    (cond
      (and took? (not (:accept? ruled)))
      (case (:why ruled)
        :a-place-takes-one-give "B1 again"
        :left-lent-from-a-give-the-act-ends "B3 one act"
        ;; a holder was given a give again after an end, and ends what hangs
        ;; beneath the ended one: today's form gives both the same id, so
        ;; she is above it again; by the rules the new give is another give
        :not-one-who-may-end-it
        (when (some (fn [fact]
                      (and (= :end (kind fact))
                           (some (fn [above]
                                   (and (not= above (:permission sent))
                                        (reference/ended? ref above)
                                        (some? (get-in gs [:old-id above]))
                                        (= (get-in gs [:old-id above]) (get-in gs [:old-id (:permission sent)]))))
                                 (reference/lent-chain ref (:e fact)))))
                    facts)
          "B1 again")
        nil)

      (and (:accept? ruled) (not took?))
      (cond
        (and (= :holder-has-a-permission word)
             (some (fn [fact]
                     (and (= :give (kind fact))
                          (let [there (get-in gs [:latest (place-of (:v fact) (:e fact))])]
                            (and there (reference/stands? ref there)))))
                   facts))
        "B1 many"

        ;; today has no change: whatever its word, it refuses a value at
        ;; a place whose permission stands
        (some #(= :change (kind %)) facts)
        "B3 change"

        (and (= :only-the-first-holder-writes-a-setting word)
             (some #(and (= :give (kind %)) (= :anyone (:holder (:v %)))) facts))
        "B4 public"

        ;; her give has no right to give, and she lends under it: today's
        ;; word is of one who may only see, or of a permission for one
        ;; sitting or one act, which gives nothing
        (and (contains? #{:permission-does-not-allow-acting :only-a-permission-for-the-lane-gives} word)
             (empty? (:give (:allows holds)))
             (some lends? facts))
        "B2 give"

        (and (some own-end? facts)
             (every? #(or (own-end? %) (not= :end (kind %))) facts)
             (or (nil? (:permission sent))
                 (and (= :permission-does-not-allow-acting word) (not (:write (:allows holds))))
                 (and (= :not-above-the-permission word) (not= :lane (scope-kind under)))))
        "B1 own end"))))

(defn see-excuse
  "The reason of the allowlist that a difference on what an asker may see
  falls under, or nil: today a lane that is open to everyone is seen by an
  agent or a run as by anyone; by the rules an agent or a run is never
  everyone on its own."
  [gs ref asker _session gate-word rules-word]
  (when (and (= :yes gate-word) (= :may-not-see rules-word)
             (reference/borrower? asker)
             (let [public-now (get-in gs [:latest [:public]])]
               (and public-now (reference/stands? ref public-now))))
    "B4 public"))

;; -------------------------------------------------------------------- the gate

(def gate
  "Today's store as a gate under test (`gate-histories.run`).

  Its state: `:rec`, the record of the walk; `:latest`, from each of
  today's places to the id of the give that was last taken there;
  `:old-id`, from the id of each give that was taken to its permission in
  today's form; `:sent`, from each act-id that has an answer to the first
  thing decided under it, in the new form and in today's."
  {:name "the store as it is before the permission fix, each thing said in today's form"
   :profile :today
   :words? false
   :start {:rec gate/empty-record :latest {} :old-id {} :sent {}}
   :send (fn [gs ref named sent]
           (let [act-id (:act-id sent)
                 first-sent (get-in gs [:sent act-id])
                 same? (and first-sent (= (reference/as-sent sent) (:new first-sent)))
                 said (if same? {:again (:old first-sent)} (said-today gs ref named sent))]
             (if-let [word (:not-today said)]
               {:skip word}
               (let [old (or (:again said) (gate/through-the-door (:maker said) act-id now))]
                 (if (and first-sent (not same?) (= old (:old first-sent)))
                   ;; two things that the new form tells apart and today's does not
                   {:skip :another-thing-that-is-the-same-thing-today}
                   (let [{:keys [record outcome]} (gate/decide (:rec gs) :streaming old)
                         took? (= :accepted (:verdict outcome))
                         decided? (contains? outcome :stamp)]
                     {:verdict (cond (:repeat outcome) :act-id-answered
                                     (:stray outcome) [:stray (:stray outcome)]
                                     :else (:verdict outcome))
                      :decided? decided?
                      :wrote? (not (identical? (:facts record) (:facts (:rec gs))))
                      :told (gate/told record old)
                      :gs (cond-> (assoc gs :rec record)
                            decided? (assoc-in [:sent act-id] {:new (reference/as-sent sent) :old old})
                            took? (as-> gs (reduce (fn [gs [place id old-id]]
                                                     (cond-> (assoc-in gs [:latest place] id)
                                                       old-id (assoc-in [:old-id id] old-id)))
                                                   gs
                                                   (:lands said))))}))))))
   :sees (fn [gs pairs]
           (mapv (fn [[asker session]]
                   (seeing (:facts (:rec gs)) (if asker (old-name asker) :nobody) session (some? asker)))
                 pairs))
   :excuse excuse
   :see-excuse see-excuse})

;; ------------------------------------------------- today's store, one rule broken

(def broken-stores
  "Today's store with one rule broken on purpose, each as [what, the var, a
  function that takes the function in place and gives the broken one]: the
  list of the judge of the store's piece 2
  (`gate-histories.piece-2.run`, `mutants`), which that judge catches by
  its own statements. Here each must be caught through the saying again in
  today's form, by the rules of the permission fix: a history must show a
  difference outside the allowlist's reasons under it that the store
  without it does not show. So the rules, on today's default values, hold
  today's store to what today's store holds itself to. The store's files
  are not changed: the function is put in place for the run only."
  [["S3: a permission stands when its ids are in place, with no look at when each was given"
    #'softland.store.gate.chain/stands?
    (fn [_]
      (fn [lane permission rows]
        (loop [p permission]
          (cond (not (record/holds-value? (get rows (chain/place lane p)) p)) false
                (nil? (chain/above p)) true
                :else (recur (chain/above p))))))]
   ["S4, S5: whoever may act ends any permission, the first one too"
    #'softland.store.gate.decision/end-fault
    (fn [_] (fn [_act row] (when-not (record/held-permission row) :no-permission-to-end)))]
   ["S6: a holder whose permission stands is given another, which takes its place"
    #'softland.store.gate.decision/give-over-fault
    (fn [_] (fn [_act _row _rows] nil))]
   ["S7: a permission is given beneath any permission"
    #'softland.store.gate.decision/permission-fact-fault
    (fn [_] (fn [_act _fact] nil))]
   ["S8: whoever may act writes a setting"
    #'softland.store.gate.decision/who-may-write-fault
    (fn [_] (fn [_act _fact] nil))]
   ["S9: a holder who may only see acts"
    #'softland.store.act/allows
    (fn [_] (fn [_permission] :see-and-act))]
   ["S11: a read lets a holder see after her permission ended"
    #'softland.store.read-path.lanes/permission-state
    (fn [_] (fn [_lane permission-row _chain-rows] (if permission-row :stands :none)))]
   ["S12: a read lets nobody but members see a lane that is open to everyone"
    #'softland.store.read-path.lanes/who-may-see
    (fn [_] (fn [_own-rows _lane] :members))]
   ["R1, R9: an act is taken under a run's permission whatever run it is sent in"
    #'softland.store.gate.decision/standing
    (fn [standing]
      (fn [gate-depot the-act permission-row depot-row chain-rows hex]
        (let [[kind run] (act/covers (:permission the-act))]
          (standing gate-depot
                    (if (= :run kind) (assoc the-act :session run) the-act)
                    permission-row depot-row chain-rows hex))))]
   ["R8: a read lets an asker see under any run's permission, whoever holds it"
    #'softland.store.read-path.lanes/run-permission-state
    (fn [state]
      (fn [lane _asker run-row chain-rows]
        (state lane (first (record/held-permission run-row)) run-row chain-rows)))]
   ["R2: any act is taken under a yes for one act, whatever it says"
    #'softland.store.gate.decision/hex-for-its-permission
    (fn [_]
      (fn [_thing the-act]
        (let [[kind _yes text] (act/covers (:permission the-act))]
          (when (= :act kind) text))))]
   ["R2: a yes for one act is used more than once: what an accept under it writes at its place is dropped"
    #'softland.store.gate.decision/writes
    (fn [writes]
      (fn [act-id the-act refused stamp time-millis thing-digest in-hand]
        (let [rows (writes act-id the-act refused stamp time-millis thing-digest in-hand)]
          (if (and the-act (nil? refused) (= :act (first (act/covers (:permission the-act)))))
            (let [place (decision/permission-place the-act)]
              (into [] (remove (fn [[which k]] (and (= :fact which) (= k place)))) rows))
            rows))))]])
