(ns gate-histories.histories
  "The world of a history, the steps that a history is made of, and the
  things that each step sends: acts in the form of the permission fix
  (`src/proposal/permissions-2026-11-27/CONTRACT.md`, sections 2.1 to 2.3).

  A thing is an act as its maker makes it, with the act-id that she holds:

    {:act-id [lane :streaming uuid]
     :who :person.asha   :lane :lab   :session :sit-1
     :permission <the id of the give it acts under, or nil>
     :facts [{:e .. :k .. :v ..} ...]}

  A give is a fact under `:softland.store/permission` on an id that its
  giver makes; its value holds `:holder`, `:allows`, `:covers`, and where
  they are set `:session`, `:given-by`, `:under` and `:lent-from`. A value
  at an id that holds a give is a change. An end is the same fact with no
  value.

  A step says what it does abstractly: who sends, under which of her gives,
  and what the act holds. `things-of` makes it concrete against the pools:
  the gives that the gate under test took so far. So a history is the same
  list of steps whatever a gate answers, and what it sends follows what the
  gate took.

  Two profiles. `:full` walks the whole range of the rules: gives that
  stand on their own and gives that are lent, the right to give, owners
  and the hand-over of a lane, changes, several changes in one act, both
  everyones. `:today` keeps to today's default values, so that each thing
  can also be said in the form of the store before the fix
  (`gate-histories.today`): every give is lent from the give above it, and
  a give allows what today's two words allow."
  (:require [clojure.test.check.generators :as gen]
            [gate-histories.reference :as reference])
  (:import [java.util UUID]))

;; ------------------------------------------------------------------ the world

(def lane "The one lane of a history." :lab)
(def people "The people who can be given something. The first makes the lane."
  [:person.asha :person.ben :person.cal :person.dee :person.eve])
(def maker "Who makes the lane, and holds its first give." (first people))
(def stranger "A person who is never given anything." :person.zed)
(def borrowers "The agents and the run of a history." [:agent.ada :agent.bot :run.r1])
(def authors "Who can send." (into (conj people stranger) borrowers))
(def sittings "The login sittings that a thing can be sent in." [:sit-1 :sit-2 :sit-3])

(def allows-of
  "What a give allows, by the word that a step uses. `:see`, `:see-write`,
  `:write`, `:member` and `:owner` are the presets of CONTRACT.md, section
  2.3; the last two are finer rights to give, which its shape lets be said."
  {:see {:see true}
   :see-write {:see true :write :all}
   :write {:write :all}
   :member {:see true :write :all :give #{:see :write :give}}
   :owner {:see true :write :all :give #{:see :write :give :owner} :owner true}
   :see-gives-see {:see true :give #{:see}}
   :member-gives-see {:see true :write :all :give #{:see}}})

(def ^:private give-high
  "The high half of the uuid of each give's id, so that a trace tells a
  give from an act-id: 71, the letter G."
  71)

(defn act-id-of
  "The act-id of a step's thing: the lane, its depot, and the step's number
  as the uuid; with `j`, the j-th act-id more of that step."
  ([n] (act-id-of n 0))
  ([n j] [lane :streaming (UUID. (long j) (long n))]))

(defn give-id-of
  "The id that a giver makes for the j-th give of the step with number n."
  [n j]
  (UUID. give-high (long (+ (* 64 n) j))))

(defn- never-id
  "An id at which no step gives."
  [n]
  (UUID. give-high (long (- (inc n)))))

(def first-give "The id of the lane's first give." (give-id-of 0 0))

(defn- value
  "A give's value, with each part that is not set left out."
  [m]
  (into {} (remove (comp nil? val)) m))

(defn thing
  "A thing: an act as its maker makes it, under the act-id of the step with
  number n, or its j-th more."
  [n j who session permission facts]
  {:act-id (act-id-of n j) :who who :lane lane :session session
   :permission permission :facts (vec facts)})

(def making-thing
  "The act that makes the lane: it names no give, and holds the lane's depot
  and its first give, an owner's, which stands on its own."
  (thing 0 0 maker (first sittings) nil
         [{:e lane :k reference/depot-key :v :streaming}
          {:e first-give :k reference/permission-key
           :v (value {:holder maker :allows (allows-of :owner)
                      :covers [:default-group lane] :given-by maker})}]))

;; ------------------------------------------------------------------- the pools

(def no-pools
  "The pools before the lane is made.

    :gives  each give that the gate took, in order, as it says now, with
            its `:id`
    :ended  the ids that an act that the gate took ended
    :yeses  each give for one exact act that a step made, taken or not:
            {:id :value :maker :text :giver :under}"
  {:gives [] :ended #{} :yeses []})

(defn pools-after
  "The pools after a thing that the gate took."
  [pools sent]
  (reduce (fn [pools {:keys [e k v]}]
            (cond (not= reference/permission-key k) pools
                  (nil? v) (update pools :ended conj e)
                  (some #(= e (:id %)) (:gives pools))
                  (update pools :gives (fn [gs] (mapv #(if (= e (:id %)) (assoc v :id e) %) gs)))
                  :else (update pools :gives conj (assoc v :id e))))
          pools
          (:facts sent)))

(defn- from-the-end
  "The item of a vector that `n` names, counted from its last: 0 is the
  last. nil for an empty vector."
  [v n]
  (when (seq v) (nth v (- (count v) 1 (mod n (count v))))))

(defn- live "Of some gives, those that no act ended." [pools gs]
  (filterv #(not (contains? (:ended pools) (:id %))) gs))

(defn- held "The gives that name a party as their holder, in order." [pools who]
  (filterv #(= who (:holder %)) (:gives pools)))

(defn- by-id [pools id] (first (filter #(= id (:id %)) (:gives pools))))

(defn- public
  "The last give to everyone that no act ended, or nil."
  [pools]
  (peek (live pools (filterv #(contains? reference/everyone (:holder %)) (:gives pools)))))

(defn- own "The last give of a party's own that no act ended, or nil." [pools who]
  (peek (live pools (held pools who))))

(defn- current
  "The give that a party acts under when she names her current one: her own
  last; for a person who has none, everyone's."
  [pools who]
  (or (own pools who) (when (reference/person? who) (public pools))))

(defn- an-owner
  "The holder of the last owner's give that no act ended; the maker when
  there is none."
  [pools]
  (or (:holder (peek (live pools (filterv #(and (:owner (:allows %)) (reference/person? (:holder %)))
                                         (:gives pools)))))
      maker))

(defn- word-of [choice] (if (keyword? choice) choice (first choice)))

(defn- sender
  "Who sends a step's act: the author that the step chose; or one who holds
  something, the holder of one of the gives that no act ended, counted from
  the last; or an owner. So most acts are sent by one who may do something."
  [pools by choice]
  (case (word-of choice)
    :as-chosen by
    :one-who-holds (or (:holder (from-the-end
                                 (live pools (filterv #(not (contains? reference/everyone (:holder %))) (:gives pools)))
                                 (second choice)))
                       by)
    :an-owner (an-owner pools)
    by))

(defn- beneath?
  "True when a give is lent from the give with this id, directly or further
  up, by what the pools hold."
  [pools id g]
  (loop [lender (:lent-from g) hops 0]
    (cond (nil? lender) false
          (= id lender) true
          (< 64 hops) false
          :else (recur (:lent-from (by-id pools lender)) (inc hops)))))

(defn- named
  "The id of the give that the maker of a step names, or nil for none."
  [pools n by choice]
  (case (word-of choice)
    :current (or (:id (current pools by)) (never-id n))
    :older (or (:id (from-the-end (held pools by) (second choice))) (:id (current pools by)) (never-id n))
    :of (or (:id (current pools (second choice))) (never-id n))
    :public (or (:id (public pools)) (:id (current pools by)) (never-id n))
    :never (never-id n)
    :none nil))

(defn- target-of
  "The id of the give that an end or a change of a step names."
  [pools n by under-id choice]
  (let [k (when (vector? choice) (second choice))]
    (or (case (word-of choice)
          :own (:id (own pools by))
          :gave (:id (from-the-end (live pools (filterv #(= by (:given-by %)) (:gives pools))) k))
          :lent (:id (from-the-end (live pools (filterv #(and under-id (= under-id (:lent-from %))) (:gives pools))) k))
          :beneath (:id (from-the-end (live pools (filterv #(and under-id (beneath? pools under-id %)) (:gives pools))) k))
          :live (:id (from-the-end (live pools (:gives pools)) k))
          :any (:id (from-the-end (:gives pools) k))
          :ended (from-the-end (vec (sort (:ended pools))) k)
          :first first-give
          :public (:id (public pools))
          :of (:id (own pools k))
          :named under-id
          :never nil)
        (never-id n))))

;; ------------------------------------------------------ from a fact said abstractly

(defn- give-value
  "The value of a give of a step: to whom, what it allows, what it covers,
  whether it is lent, and for a step that says something wrong on purpose,
  the wrong part."
  [pools n by under-id {:keys [to allows scope lend wrong]}]
  (let [above (by-id pools under-id)
        [covers session] (case (word-of scope)
                           :as-above [(or (:covers above) [:default-group lane]) (:session above)]
                           :lane [[:default-group lane] nil]
                           :sitting [[:default-group lane] (second scope)]
                           :elsewhere [[:default-group :elsewhere] nil])
        under (if (= :under-another wrong)
                (or (:id (first (remove #(= under-id (:id %)) (:gives pools)))) (never-id n))
                under-id)
        lent-from (case (word-of lend)
                    :default (when (or (reference/borrower? to) (:lent-from above)) under-id)
                    :lent under-id
                    :as-under under
                    :standing nil
                    :from (or (:id (from-the-end (:gives pools) (second lend))) under-id))]
    (value {:holder to :allows (allows-of allows) :covers covers :session session
            :given-by (if (= :given-by-another wrong) (first (remove #{by} people)) by)
            :under under
            :lent-from lent-from})))

(defn- fact-of
  "One fact as its maker sends it, or nil for a change that finds nothing
  to change. `n` is the step's number and `j` the fact's place in it: they
  make a give's id, and mark the value of a plain fact."
  [pools n j by under-id {:keys [f] :as fact}]
  (case f
    :plain {:e (:e fact) :k (:k fact) :v (str "said at " n)}
    :setting {:e lane :k (:key fact)
              :v (case (:key fact)
                   :softland.store/who-may-read-acts (:word fact)
                   :softland.store/builds-on []
                   :softland.store/marked-keys #{:colour})}
    :depot {:e lane :k reference/depot-key :v :streaming}
    :give {:e (give-id-of n j) :k reference/permission-key :v (give-value pools n by under-id fact)}
    :end {:e (target-of pools n by under-id (:target fact)) :k reference/permission-key :v nil}
    :change (let [id (target-of pools n by under-id (:target fact))
                  was (by-id pools id)
                  wrong (:wrong fact)]
              (when was
                {:e id :k reference/permission-key
                 :v (value (cond-> (assoc (select-keys was [:holder :covers :session :lent-from :given-by :under])
                                          :allows (if (= :same (:allows fact)) (:allows was) (allows-of (:allows fact))))
                             (= :another-holder wrong)
                             (assoc :holder (first (remove #{(:holder was)} people)))
                             (= :another-lender wrong)
                             (assoc :lent-from (when-not (:lent-from was) under-id))
                             (= :given-by-another wrong)
                             (assoc :given-by (first (remove #{(:given-by was)} people)))
                             (= :under-another wrong)
                             (assoc :under (never-id n))))}))))

(defn- facts-of
  "The facts of a step's act, one for each abstract fact that gives one,
  with `j0` as the place of the first; a plain fact when none gives one,
  since an act holds a fact."
  ([pools n by under-id facts] (facts-of pools n 0 by under-id facts))
  ([pools n j0 by under-id facts]
   (or (not-empty (vec (keep-indexed (fn [j fact] (fact-of pools n (+ j0 j) by under-id fact)) facts)))
       [{:e :thing-1 :k :note :v (str "said at " n)}])))

;; --------------------------------------------------------------- the generators

(def ^:private gen-person (gen/elements people))
(def ^:private gen-sitting (gen/elements sittings))

(def ^:private gen-author
  "Who sends: mostly a person who can be given something."
  (gen/frequency [[12 gen-person] [2 (gen/return stranger)] [5 (gen/elements borrowers)]]))

(def ^:private gen-sender
  "Who sends a step's act (`sender`)."
  (gen/frequency [[3 (gen/return :as-chosen)]
                  [6 (gen/fmap (fn [n] [:one-who-holds n]) gen/nat)]
                  [2 (gen/return :an-owner)]]))

(def ^:private gen-recent
  "Which earlier give, yes or thing a step names, counted from the last:
  mostly the last or the one before it."
  (gen/frequency [[6 (gen/return 0)] [3 (gen/return 1)] [2 gen/nat]]))

(defn- gen-under
  "Which give a maker names: mostly her current one; else an older one of
  hers, another's, everyone's, one that was never given, or none."
  [profile]
  (gen/frequency
   (cond-> [[14 (gen/return :current)]
            [2 (gen/fmap (fn [n] [:older n]) gen/nat)]
            [1 (gen/fmap (fn [who] [:of who]) gen-author)]
            [1 (gen/return :never)]
            [1 (gen/return :none)]]
     (= :full profile) (conj [2 (gen/return :public)]))))

(defn- gen-to [profile]
  (gen/frequency
   (cond-> [[11 gen-person] [6 (gen/elements borrowers)] [2 (gen/return :anyone)]]
     (= :full profile) (conj [1 (gen/return :anyone-signed-in)]))))

(def ^:private gen-allows
  (gen/frequency [[5 (gen/return :see)] [3 (gen/return :see-write)] [1 (gen/return :write)]
                  [6 (gen/return :member)] [2 (gen/return :owner)]
                  [1 (gen/return :see-gives-see)] [1 (gen/return :member-gives-see)]]))

(def ^:private gen-target
  "Which give an end or a change names."
  (gen/frequency [[4 (gen/return :own)]
                  [4 (gen/fmap (fn [n] [:gave n]) gen-recent)]
                  [2 (gen/fmap (fn [n] [:lent n]) gen-recent)]
                  [2 (gen/fmap (fn [n] [:beneath n]) gen/nat)]
                  [3 (gen/fmap (fn [n] [:live n]) gen/nat)]
                  [1 (gen/fmap (fn [n] [:any n]) gen-recent)]
                  [2 (gen/fmap (fn [n] [:ended n]) gen-recent)]
                  [1 (gen/return :first)]
                  [1 (gen/return :public)]
                  [2 (gen/fmap (fn [who] [:of who]) gen-author)]
                  [1 (gen/return :named)]
                  [1 (gen/return :never)]]))

(defn- gen-give
  "A give, said abstractly. Under `:today` it keeps to today's default
  values: lent from the give above, and one of the pairs of what it covers
  and allows that today's form can say."
  [profile]
  (if (= :today profile)
    (gen/fmap (fn [[to [scope allows] wrong]]
                (if (= :anyone to)
                  {:f :give :to to :allows :see :scope :lane :lend :as-under :wrong wrong}
                  {:f :give :to to :allows allows :scope scope :lend :as-under :wrong wrong}))
              (gen/tuple (gen-to profile)
                         (gen/frequency [[8 (gen/return [:lane :see])]
                                         [10 (gen/return [:lane :member])]
                                         [2 (gen/fmap (fn [s] [[:sitting s] :see]) gen-sitting)]
                                         [3 (gen/fmap (fn [s] [[:sitting s] :see-write]) gen-sitting)]])
                         (gen/frequency [[30 (gen/return nil)] [1 (gen/return :under-another)]])))
    (gen/fmap (fn [[to allows scope lend wrong]]
                {:f :give :to to :allows allows :scope scope :lend lend :wrong wrong})
              (gen/tuple (gen-to profile)
                         gen-allows
                         (gen/frequency [[14 (gen/return :as-above)]
                                         [3 (gen/fmap (fn [s] [:sitting s]) gen-sitting)]
                                         [1 (gen/return :lane)]
                                         [1 (gen/return :elsewhere)]])
                         (gen/frequency [[10 (gen/return :default)] [3 (gen/return :lent)]
                                         [3 (gen/return :standing)]
                                         [1 (gen/fmap (fn [n] [:from n]) gen/nat)]])
                         (gen/frequency [[30 (gen/return nil)] [1 (gen/return :given-by-another)]
                                         [1 (gen/return :under-another)]])))))

(defn- gen-change [profile]
  (gen/fmap (fn [[target allows wrong]] {:f :change :target target :allows allows :wrong wrong})
            (gen/tuple gen-target
                       (gen/frequency [[8 (if (= :today profile) (gen/elements [:see :member]) gen-allows)]
                                       [2 (gen/return :same)]])
                       (if (= :today profile)
                         (gen/return nil)
                         (gen/frequency [[20 (gen/return nil)] [1 (gen/return :another-holder)]
                                         [1 (gen/return :another-lender)] [1 (gen/return :given-by-another)]
                                         [1 (gen/return :under-another)]])))))

(def ^:private gen-plain
  (gen/fmap (fn [[e k]] {:f :plain :e e :k k})
            (gen/tuple (gen/elements [:thing-1 :thing-2]) (gen/elements [:colour :note]))))

(defn- gen-fact
  "One fact, said abstractly; `fact-of` makes it concrete at its step."
  [profile]
  (gen/frequency
   [[12 (gen-give profile)]
    [7 (gen/fmap (fn [target] {:f :end :target target}) gen-target)]
    [(if (= :today profile) 3 5) (gen-change profile)]
    [8 gen-plain]
    [2 (gen/fmap (fn [[k word]] {:f :setting :key k :word word})
                 (gen/tuple (gen/elements [:softland.store/who-may-read-acts :softland.store/builds-on
                                           :softland.store/marked-keys])
                            (gen/elements [:everyone :members])))]
    [1 (gen/return {:f :depot})]]))

(defn- gen-op
  "One step of a history."
  [profile]
  (let [fact (gen-fact profile)
        act (fn [facts]
              (gen/fmap (fn [[by sender sitting under facts stray?]]
                          {:op :act :by by :sender sender :sitting sitting :under under :facts facts
                           :other-sitting? stray?})
                        (gen/tuple gen-author gen-sender gen-sitting (gen-under profile) facts
                                   (gen/frequency [[11 (gen/return false)] [1 (gen/return true)]]))))]
    (gen/frequency
     (cond-> [[34 (act (gen/vector fact 1))]
              [6 (act (gen/vector fact 2))]
              [2 (act (gen/vector fact 3))]
              [2 (gen/fmap (fn [n] {:op :again :n n}) gen/nat)]
              [1 (gen/return {:op :make-again})]
              ;; a give for one exact act, made and given, and what its
              ;; holder sends
              [6 (gen/fmap (fn [[by sender for for-sitting sitting under fact then lend allows]]
                             {:op :yes :by by :sender sender :for for :for-sitting for-sitting :sitting sitting
                              :under under :fact fact :then then :lend lend :allows allows})
                           (gen/tuple gen-person gen-sender gen-author gen-sitting gen-sitting
                                      (gen/frequency [[8 (gen/return :current)] [1 (gen/fmap (fn [n] [:older n]) gen/nat)]])
                                      (if (= :today profile) gen-plain (gen/frequency [[2 gen-plain] [1 fact]]))
                                      (gen/elements [:nothing :send :send :send :before :changed
                                                     :another-sitting :twice :its-own-end :by-another])
                                      (if (= :today profile)
                                        (gen/return :as-under)
                                        (gen/elements [:default :default :lent :standing]))
                                      ;; a yes allows write; under `:full`, now and then see too
                                      (if (= :today profile)
                                        (gen/return :write)
                                        (gen/elements [:write :write :write :see-write]))))]
              ;; a step on a give for one exact act that was made
              [9 (gen/fmap (fn [[n what by sitting]] {:op :on-a-yes :n n :what what :by by :sitting sitting})
                           (gen/tuple gen-recent
                                      (gen/elements [:send :send :send :end-by-giver :end-by-holder
                                                     :end-by-owner :end-by-another :at-its-place])
                                      gen-author gen-sitting))]
              ;; another thing under an act-id that was used
              [3 (gen/fmap (fn [[n as by sitting fact]]
                             {:op :used-act-id :n n :as as :by by :sitting sitting :fact fact})
                           (gen/tuple gen-recent (gen/elements [:same :another]) gen-author gen-sitting fact))]]
       true
       (conj ;; the giver of a give does something to it, under the give
             ;; that she gave it under
             [5 (gen/fmap (fn [[n how sitting]] {:op :take-back :n n :how how :sitting sitting})
                          (gen/tuple gen/nat
                                     (if (= :today profile)
                                       (gen/elements [:end :lower :raise])
                                       (gen/elements [:end :end :lower :lower :raise :raise :twice
                                                      :wrong-giver :wrong-under]))
                                     gen-sitting))])
       true
       (conj ;; a give that an act ended is given once more by its giver:
             ;; the same holder and the same terms, under a new id
             [3 (gen/fmap (fn [[n sitting]] {:op :give-again :n n :sitting sitting})
                          (gen/tuple gen-recent gen-sitting))])
       (= :full profile)
       (into [;; a lender is lowered and raised again, and her borrower
              ;; writes after each
              [3 (gen/fmap (fn [[n sitting]] {:op :lower-a-lender :n n :sitting sitting})
                           (gen/tuple gen/nat gen-sitting))]
              ;; everyone's give: opened, and what a person, a stranger and
              ;; an agent do under it
              [6 (gen/fmap (fn [[by what to sitting]]
                             {:op :through-everyone :by by :what what :to to :sitting sitting})
                           (gen/tuple (gen/frequency [[3 gen-person] [2 (gen/return stranger)]])
                                      (gen/elements [:open :open-signed-in :write :lend :lend
                                                     :lend-to-a-person :end-a-borrow :end-a-borrow :as-an-agent])
                                      (gen/elements borrowers)
                                      gen-sitting))]
              ;; a lane handed over, or not quite
              [5 (gen/fmap (fn [[to how sitting]] {:op :hand-over :to to :how how :sitting sitting})
                           (gen/tuple gen-person
                                      (gen/elements [:proper :proper :end-first :lent :to-a-member :alone :and-more])
                                      gen-sitting))]
              ;; a chain of lent gives: mostly a few, now and then to its
              ;; bound and past it
              [2 (gen/fmap (fn [[by sender depth sitting]]
                             {:op :deepen :by by :sender sender :depth depth :sitting sitting})
                           (gen/tuple gen-person gen-sender
                                      (gen/frequency [[4 (gen/choose 2 5)] [1 (gen/choose 29 34)]])
                                      gen-sitting))]])))))

(defn gen-history
  "A history of a profile: up to eighty steps after the making act."
  [profile]
  (gen/vector (gen-op profile) 0 80))

;; ------------------------------------------------ from a step to the things sent

(defn- yes-text
  "The text by which the give for one exact act of the step with number n
  names its act: 64 hex characters. The rules read no digest; a gate that
  does is handed the act itself beside it (`gate-histories.today`)."
  [n]
  (format "%064x" (long n)))

(defn- sitting-for
  "The sitting that an act is sent in: the sitting of the give that it
  names, when that give is good for one; else the sitting chosen."
  [pools under-id chosen stray?]
  (or (when-not stray? (:session (by-id pools under-id))) chosen))

(defn- yes-step
  "The things of a give for one exact act: the give, with an id of its own,
  for an act that is not sent yet, and what its holder sends with it, each
  under an act-id of its own.

    :nothing          the give alone
    :send             the give, then the act
    :before           the act, refused, as the give is not there yet; the
                      give; the act again under a new act-id
    :changed          the give, then the act with a fact added
    :another-sitting  the give, then the act in another sitting
    :by-another       the give, then the act sent by another
    :twice            the give, the act, and the act again under a new
                      act-id: the second finds the give used
    :its-own-end      the act is the end of the give itself"
  [pools n {:keys [by for for-sitting sitting under fact then lend allows] :as step}]
  (let [by (sender pools by (:sender step))
        under-id (named pools n by under)
        yes (give-id-of n 0)
        text (yes-text n)
        maker-act {:who for :lane lane :session for-sitting
                   :facts (if (= :its-own-end then)
                            [{:e yes :k reference/permission-key :v nil}]
                            (facts-of pools n 1 for yes [fact]))}
        v (give-value pools n by under-id {:to for :allows (or allows :write) :scope :lane :lend lend})
        v (assoc v :covers [:exact-act text])
        give (thing n 0 by (sitting-for pools under-id sitting false) under-id
                    [{:e yes :k reference/permission-key :v v}])
        act (fn [j change]
              (let [made (change maker-act)]
                (assoc made :act-id (act-id-of n j) :permission yes)))]
    {:entries [{:id yes :value v :maker maker-act :text text :giver by :under under-id}]
     :named {text maker-act}
     :sends (case then
              :nothing [give]
              (:send :its-own-end) [give (act 1 identity)]
              :before [(act 1 identity) give (act 2 identity)]
              :changed [give (act 1 #(update % :facts conj {:e :thing-9 :k :note :v "added after the yes"}))]
              :another-sitting [give (act 1 #(assoc % :session (first (remove #{for-sitting} sittings))))]
              :by-another [give (act 1 #(assoc % :who (first (remove #{for} people))))]
              :twice [give (act 1 identity) (act 2 identity)])}))

(defn- on-a-yes
  "The thing of a step on a give for one exact act that was made: its act
  sent by its holder under a new act-id; its end, by its giver under the
  give that she gave it under, by its holder under no give, by an owner, or
  by another; or a give at its id again."
  [pools n {:keys [what by sitting]} entry]
  (let [yes (:id entry)
        end [{:e yes :k reference/permission-key :v nil}]
        owner (an-owner pools)]
    (case what
      :send (assoc (:maker entry) :act-id (act-id-of n) :permission yes)
      :end-by-giver (thing n 0 (:giver entry) sitting (:under entry) end)
      :end-by-holder (thing n 0 (:holder (:value entry)) sitting nil end)
      :end-by-owner (thing n 0 owner sitting (:id (own pools owner)) end)
      :end-by-another (thing n 0 by sitting (:id (current pools by)) end)
      :at-its-place (thing n 0 (:giver entry) sitting (:under entry)
                           [{:e yes :k reference/permission-key :v (:value entry)}]))))

(defn- hand-over
  "The act of a hand-over step, by an owner under her owner's give: a give
  to another person and the end of her own, in one act.

    :proper       an owner's give that stands on its own, then her end
    :end-first    her end, then the give
    :lent         the new owner's give is lent from hers: left lent from a
                  give that the act ends
    :to-a-member  the new give is a member's: no owner would be left,
                  unless another owner stands
    :alone        her end alone
    :and-more     the give, her end, and a plain fact"
  [pools n {:keys [to how sitting]}]
  (let [by (an-owner pools)
        under-id (or (:id (own pools by)) (never-id n))
        give (fn [allows lend]
               {:e (give-id-of n 0) :k reference/permission-key
                :v (give-value pools n by under-id {:to to :allows allows :scope :lane :lend lend})})
        end {:e under-id :k reference/permission-key :v nil}]
    (thing n 0 by sitting under-id
           (case how
             :proper [(give :owner :standing) end]
             :end-first [end (give :owner :standing)]
             :lent [(give :owner :lent) end]
             :to-a-member [(give :member :standing) end]
             :alone [end]
             :and-more [(give :owner :standing) end {:e :thing-1 :k :note :v (str "handed over at " n)}]))))

(defn- deepen
  "The things of a chain of lent gives: a person lends see to an agent, the
  agent lends it on, and on, each under the give before it, `depth` times."
  [pools n {:keys [by depth sitting] :as step}]
  (let [chosen (sender pools by (:sender step))
        by (if (reference/person? chosen) chosen (an-owner pools))
        start (or (:id (own pools by)) (never-id n))
        holder-of (fn [j] (nth borrowers (mod j 2)))]
    (mapv (fn [j]
            (let [under-id (if (zero? j) start (give-id-of n (dec j)))
                  who (if (zero? j) by (holder-of (dec j)))]
              (thing n j who sitting under-id
                     [{:e (give-id-of n j) :k reference/permission-key
                       :v (value {:holder (holder-of j) :allows (allows-of :see)
                                  :covers [:default-group lane]
                                  :given-by who :under under-id :lent-from under-id})}])))
          (range depth))))

(defn- take-back
  "The thing of a step in which the giver of a give that no act ended does
  something to it, in an act under the give that she gave it under: ends
  it, lowers it to see, raises it to a member's, lowers and ends it in one
  act, or lowers it and says that another did, or under another give."
  [pools n {:keys [how sitting]} g]
  (let [by (:given-by g)
        under-id (:under g)
        end {:e (:id g) :k reference/permission-key :v nil}
        to (fn [allows]
             {:e (:id g) :k reference/permission-key
              :v (value (assoc (select-keys g [:holder :covers :session :lent-from])
                               :allows (allows-of allows) :given-by by :under under-id))})]
    (thing n 0 by (sitting-for pools under-id sitting false) under-id
           (case how
             :end [end]
             :lower [(to :see)]
             :raise [(to :member)]
             :twice [(to :see) end]
             :wrong-giver [(assoc-in (to :see) [:v :given-by] (first (remove #{by} people)))]
             :wrong-under [(assoc-in (to :see) [:v :under] (never-id n))]))))

(defn- lower-a-lender
  "The things of a step on a give that another is lent from: its giver
  lowers it to see; the borrower writes; its giver raises it to what it
  was; the borrower writes again. `lent` is the borrower's give and `above`
  the give that it is lent from."
  [pools n {:keys [sitting]} lent above]
  (let [by (:given-by above)
        under-id (:under above)
        to (fn [allows]
             {:e (:id above) :k reference/permission-key
              :v (value (assoc (select-keys above [:holder :covers :session :lent-from])
                               :allows allows :given-by by :under under-id))})
        change (fn [j allows] (thing n j by (sitting-for pools under-id sitting false) under-id [(to allows)]))
        write (fn [j] (thing n j (:holder lent) (sitting-for pools (:id lent) sitting false) (:id lent)
                             [{:e :thing-2 :k :note :v (str "written at " n "." j)}]))]
    [(change 0 (allows-of :see)) (write 1) (change 2 (:allows above)) (write 3)]))

(defn- through-everyone
  "The thing of a step on everyone's give: an owner opens the lane to
  anyone at all, or to anyone signed in; or, under the last give to
  everyone, a person writes, lends it to an agent or a run, lends it to
  another person, or ends what another lent from it; or an agent acts
  under it on its own."
  [pools n {:keys [by what to sitting]}]
  (let [owner (an-owner pools)
        owners (or (:id (own pools owner)) (never-id n))
        everyones (or (:id (public pools)) (never-id n))
        give (fn [under-id to allows lend]
               {:e (give-id-of n 0) :k reference/permission-key
                :v (give-value pools n (if (= under-id owners) owner by) under-id
                               {:to to :allows allows :scope :lane :lend lend})})]
    (case what
      :open (thing n 0 owner sitting owners [(give owners :anyone :see :standing)])
      :open-signed-in (thing n 0 owner sitting owners [(give owners :anyone-signed-in :see-write :standing)])
      :write (thing n 0 by sitting everyones [{:e :thing-2 :k :colour :v (str "said at " n)}])
      :lend (thing n 0 by sitting everyones [(give everyones to :see :lent)])
      :lend-to-a-person (thing n 0 by sitting everyones [(give everyones (first (remove #{by} people)) :see :lent)])
      :end-a-borrow (thing n 0 by sitting everyones
                           [{:e (target-of pools n by everyones [:lent n]) :k reference/permission-key :v nil}])
      :as-an-agent (thing n 0 to sitting everyones [{:e :thing-2 :k :colour :v (str "said at " n)}]))))

(defn things-of
  "What one step sends: {:sends the things in order}, and for a step that
  makes a give for one exact act, `:entries` for the pools and `:named`,
  from its text to the act, for the rules; `:again?` for a step that sends
  an earlier thing once more. Takes the pools, the things sent so far, the
  step's number and the step."
  [{:keys [pools things n]} {:keys [op by sitting under facts] :as step}]
  (case op
    :make {:sends [making-thing]}
    :act (let [by (sender pools by (:sender step))
               under-id (named pools n by under)]
           {:sends [(thing n 0 by (sitting-for pools under-id sitting (:other-sitting? step)) under-id
                           (facts-of pools n by under-id facts))]})
    :yes (yes-step pools n step)
    :on-a-yes (if-let [entry (from-the-end (:yeses pools) (:n step))]
                {:sends [(on-a-yes pools n step entry)]}
                {:sends []})
    :hand-over {:sends [(hand-over pools n step)]}
    :take-back (if-let [g (from-the-end (live pools (filterv #(and (:under %) (:given-by %)) (:gives pools)))
                                        (:n step))]
                 {:sends [(take-back pools n step g)]}
                 {:sends []})
    :give-again (if-let [g (from-the-end (filterv #(and (:under %) (contains? (:ended pools) (:id %))) (:gives pools))
                                         (:n step))]
                  (let [by (:given-by g) under-id (:under g)]
                    {:sends [(thing n 0 by (sitting-for pools under-id sitting false) under-id
                                    [{:e (give-id-of n 0) :k reference/permission-key :v (dissoc g :id)}])]})
                  {:sends []})
    :lower-a-lender (let [lent (from-the-end (live pools (filterv #(:under (by-id pools (:lent-from %))) (:gives pools)))
                                             (:n step))]
                      (if lent
                        {:sends (lower-a-lender pools n step lent (by-id pools (:lent-from lent)))}
                        {:sends []}))
    :through-everyone {:sends [(through-everyone pools n step)]}
    :deepen {:sends (deepen pools n step)}
    :used-act-id (if-let [earlier (from-the-end things (:n step))]
                   (let [who (if (= :same (:as step)) (:who earlier) by)
                         under-id (or (:id (current pools who)) (never-id n))]
                     {:sends [(assoc (thing n 0 who sitting under-id (facts-of pools n who under-id [(:fact step)]))
                                     :act-id (:act-id earlier))]})
                   {:sends []})
    :make-again {:sends [(assoc making-thing :act-id (act-id-of n))]}
    :again (if-let [earlier (from-the-end things (:n step))]
             {:sends [earlier] :again? true}
             {:sends []})))

;; ------------------------------------------------------------------ for a trace

(defn id-mark
  "An id of a give as a short mark: g and its number, the step's number and
  the give's place in the step; g? and a number for an id at which no step
  gives."
  [^UUID u]
  (let [lo (.getLeastSignificantBits u)]
    (cond (not= give-high (.getMostSignificantBits u)) (str u)
          (neg? lo) (str "g?" (- lo))
          (zero? (mod lo 64)) (str "g" (quot lo 64))
          :else (str "g" (quot lo 64) "." (mod lo 64)))))

(defn act-id-mark
  "An act-id as a short mark: its step's number, with the j-th more after a
  dot."
  [[_ _ ^UUID u]]
  (let [hi (.getMostSignificantBits u) lo (.getLeastSignificantBits u)]
    (if (zero? hi) (str "#" lo) (str "#" lo "." hi))))
