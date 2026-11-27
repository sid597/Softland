(ns gate-histories.live-read
  "What a read tells of an asker's rights in a lane while it stays open: a
  pure model of CONTRACT.md, section 2.4, and of the pure-model invariants
  of the examination of caching (`state/loop-0d.md`, item 3; its 9 to 13),
  played against every order that an adversary can bring about. Pure code:
  no cluster, and nothing of the store.

  Four parts, each written here and nowhere else.

  The lane over time. A history's acts that the rules took
  (`gate-histories.reference`) are written one after the other; after each,
  the lane has a version. A version holds what the store's tables hold of
  the rights: the row of each give, with the stamp of the decision that
  wrote it last and what it says, or no value once it ended; and for each
  holder the run of her gives over the lane's default group, from each
  give's key to the stamp of its last grant or change, the key gone once
  the give ended (CONTRACT 2.4, the seventh table). And the lane's plain
  facts, each with its stamp.

  The query. `lane-rights` reads one version in one turn and gives
  {:standing :read-set}: the word for what the asker holds, and every row
  and every holder's run that it read, each with what it saw there. For
  `:see` it reads everyone's runs first and stops at the first give that
  stands and lets her see. It is the judge's own second writing of what the
  rules say an asker sees, read from rows: `truth` says the same from the
  rules' state, and a run holds the two to each other.

  The read. `step` is one step of an open read: it holds one answer, the
  answer of the ask that it sent last, and pushes on the sources of that
  answer's read set. Its standing is the answer's word only while every
  source of the read set is given and says what the answer saw there;
  otherwise `:not-known-yet`. It works no word out of pushed rows itself.
  While it does not know it tells nothing and owes the whole answer. It
  asks again when its answer is held against all its pushes and does not
  agree, and a rights push said something new after the last ask was sent;
  else it waits. One ask is in flight at most. An ask that failed is asked
  again, three times in a row at most; then the read ends with a word and
  never with a level.

  The world. It writes the versions, serves and advances each push on its
  own, hands each callback to the read at a later moment of its choosing,
  runs each query at a version of its choosing between the ask and the
  answer, lets a query fail, and lets the answer of a failed ask arrive
  after all. `run-schedule` plays one such order from a list of choices;
  `explore` plays every order of a small history.

  What every step of a read under test is held to:

    answered   it tells a level only when an answer that was handed to that
               very read, for its own asker, sitting and question, says
               that level and agrees with what its pushes say now, entry
               for entry (invariants 9 and 12)
    was        the level that it tells was the lane's level, by the rules,
               at a version no older than what all its pushes have shown:
               no state in between two versions, and none that its pushes
               have left behind (BUILD batch 4, line 4)
    not more   it never tells more than she had at any version that its
               pushes cover
    silent     while it does not know, it tells nothing; and what it tells
               next is the whole answer, never a change (invariant 10)
    asks       one ask in flight at most; and an ask is sent only when the
               read was opened, when its last ask failed, or when a rights
               push of it was given or changed since its last ask
               (invariant 11)
    settles    when the world stops writing and delivers everything, the
               read comes to the lane's level at the last version, unless
               it ended for a cause that the world gave it

  A read under test is a function of the shape of `client-step`. The
  reference read is held here to all of the above, and each of its broken
  copies (`gate-histories.run`) must break one. The store's own read path
  is the next: it needs `client-step` written over it.

  What the model does not hold: Rama's pushes, a push that is lost for
  good, a lane that is not made yet, two lanes, a routing id other than
  the lane, and the microbatch gate's copies."
  (:require [clojure.set :as set]
            [gate-histories.histories :as h]
            [gate-histories.reference :as reference]))

(def ^:private lane h/lane)

;; ------------------------------------------------------------ the lane over time

(defn decisions-of
  "Judges things in order by the rules. Gives each act that the rules took
  as {:sent :before :after :stamp :under}: the rules' state before and
  after it, the give that it named, and its stamp, the number of its
  decision among all that were decided; so stamps grow and have gaps.
  `named` says what each give for one exact act names."
  [named things]
  (:taken
   (reduce (fn [{:keys [ref stamp] :as state} sent]
             (let [ruled (binding [reference/*act-named* named] (reference/judge ref lane sent))
                   answered? (or (:repeat? ruled) (:not-mine? ruled))
                   stamp (if answered? stamp (inc stamp))]
               (cond-> (assoc state :ref (:state ruled) :stamp stamp)
                 (:accept? ruled) (update :taken conj {:sent sent :before ref :after (:state ruled)
                                                       :stamp stamp :under (:under ruled)}))))
           {:ref reference/start :stamp 0 :taken []}
           things)))

(defn decisions
  "The acts of a history that the rules take, as `decisions-of` gives them:
  the history is played against the rules as their own gate
  (`gate-histories.run`, `play`), the lane's making act first."
  [ops]
  (loop [state {:ref reference/start :pools h/no-pools :named {} :things [] :n 0 :stamp 0 :taken []}
         todo (cons {:op :make} ops)]
    (if (empty? todo)
      (:taken state)
      (let [{:keys [sends entries named]} (h/things-of state (first todo))
            state (cond-> state
                    (seq entries) (update-in [:pools :yeses] into entries)
                    (seq named) (update :named merge named))
            state (reduce
                   (fn [{:keys [ref stamp] :as state} sent]
                     (if (reference/unsettled ref lane sent)
                       state
                       (let [ruled (binding [reference/*act-named* (:named state)] (reference/judge ref lane sent))
                             answered? (or (:repeat? ruled) (:not-mine? ruled))
                             stamp (if answered? stamp (inc stamp))]
                         (cond-> (-> state (assoc :ref (:state ruled) :stamp stamp) (update :things conj sent))
                           (:accept? ruled) (-> (update :pools h/pools-after sent)
                                                (update :taken conj {:sent sent :before ref :after (:state ruled)
                                                                     :stamp stamp :under (:under ruled)}))))))
                   state
                   sends)]
        (recur (update state :n inc) (rest todo))))))

(def ^:dynamic *a-run-keeps-a-mark?*
  "False for the run of a holder's gives as CONTRACT 2.4 has it: from each
  key to its stamp, the key gone at an end, so that a run can say again
  what it said before. True for a run that also keeps the stamp of the last
  decision that wrote under it, an end too: such a run never says again
  what it said before."
  false)

(def ^:private no-tables {:rows {} :runs {} :facts {}})
(def ^:private no-run {:stamps (sorted-map)})

(defn- run-key
  "The key of a give in its holder's run: its sitting, or `*` for any, and
  its id."
  [says id]
  [(if-let [sitting (:session says)] (name sitting) "*") (str id)])

(defn- in-a-run? [says] (= [:default-group lane] (:covers says)))

(defn- write-run
  [tables holder stamp change]
  (cond-> (update-in tables [:runs holder] (fn [run] (update (or run no-run) :stamps change)))
    *a-run-keeps-a-mark?* (assoc-in [:runs holder :mark] stamp)))

(defn- tables-after
  "The tables after one act that the rules took: each row that it wrote
  holds its stamp; a grant and a change in place write the give's key in
  its holder's run with that stamp, and an end takes the key out."
  [tables {:keys [sent before stamp under]}]
  (let [tables
        (reduce
         (fn [tables {:keys [e k v] :as fact}]
           (case (reference/fact-kind before fact)
             (:give :change) (cond-> (assoc-in tables [:rows e] {:stamp stamp :says v})
                               (in-a-run? v) (write-run (:holder v) stamp #(assoc % (run-key v e) stamp)))
             :end (let [says (get-in tables [:rows e :says])]
                    (cond-> (assoc-in tables [:rows e] {:stamp stamp})
                      (and says (in-a-run? says)) (write-run (:holder says) stamp #(dissoc % (run-key says e)))))
             :plain (assoc-in tables [:facts [e k]] {:stamp stamp :v v})
             tables))
         tables
         (:facts sent))]
    ;; an act taken under a give for one exact act uses it up: its row
    ;; holds no value after
    (cond-> tables
      (and under (= :exact-act (first (:covers under)))) (assoc-in [:rows (:id under)] {:stamp stamp}))))

(defn versions-of
  "The lane over time, of the acts that the rules took: {:tables :refs
  :acts}, each a vector by version. Version 0 is before the lane is made;
  version n is after the n-th act that was taken."
  [taken]
  {:id (gensym "lane-")
   :tables (vec (reductions tables-after no-tables taken))
   :refs (into [reference/start] (map :after) taken)
   :acts (into [nil] taken)})

;; ---------------------------------------------------------------- the sources

(def facts-source "The source of the lane's plain facts." [:facts])

(defn source-value
  "What a source says on tables: the row of a give, {:stamp :says}, or nil
  when nothing was written there; a holder's run, {:stamps key -> stamp};
  the lane's facts."
  [tables [kind id]]
  (case kind
    :row (get-in tables [:rows id])
    :run (get-in tables [:runs id] no-run)
    :facts (:facts tables)))

(defn seen
  "What a read set holds of a source's value: the stamp of a row, nil for
  no row; the whole of a run."
  [[kind] value]
  (case kind
    :row (:stamp value)
    :run value))

;; ------------------------------------------------------------------ the query

(def scan-bound
  "How many keys of one holder's run the query reads at most. The store's
  own number is the builder's; this one is small, so that histories reach
  it."
  8)

(defn- meet
  "What a lent give allows, capped by what its lender allows."
  [mine lenders]
  (let [may-give (set/intersection (:give mine #{}) (:give lenders #{}))]
    (cond-> {}
      (and (:see mine) (:see lenders)) (assoc :see true)
      (and (:write mine) (:write lenders)) (assoc :write :all)
      (seq may-give) (assoc :give may-give)
      (and (:owner mine) (:owner lenders)) (assoc :owner true))))

(defn climb
  "Reads the row of a give and of each give above it, one at a time, each
  from what the one below says it is lent from. Gives [what each says, the
  lowest first, or nil when a row holds no value or the chain is cut at
  its bound; the read set with every row that was read]."
  [tables read-set id]
  (loop [id id values [] read-set read-set]
    (let [source [:row id]
          row (source-value tables source)
          read-set (assoc read-set source (seen source row))
          says (:says row)]
      (cond (nil? says) [nil read-set]
            (nil? (:lent-from says)) [(conj values says) read-set]
            (<= reference/max-lent-depth (inc (count values))) [nil read-set]
            :else (recur (:lent-from says) (conj values says) read-set)))))

(defn- lets-see?
  "True when a give whose rows say this stands and lets an asker see the
  lane in the sitting that she asks in."
  [values session]
  (boolean (and values
                (let [says (first values)]
                  (and (in-a-run? says)
                       (or (nil? (:session says)) (= session (:session says)))
                       (:see (reduce meet (map :allows values))))))))

(defn scan
  "Reads one holder's run in the turn, and the rows of each of its gives
  that is good in the asker's sitting, in the order of their keys, until
  one lets her see; `scan-bound` keys at most. Gives [found?, cut?, the
  read set]: `cut?` when no give let her see and keys were left unread."
  [tables read-set holder session]
  (let [source [:run holder]
        run (source-value tables source)
        read-set (assoc read-set source (seen source run))
        wanted (filter (fn [[[sitting _] _]] (or (= "*" sitting) (and session (= (name session) sitting))))
                       (:stamps run))]
    (loop [todo (take scan-bound wanted) read-set read-set]
      (if-let [[[_ id] _] (first todo)]
        (let [[values read-set] (climb tables read-set (java.util.UUID/fromString id))]
          (if (lets-see? values session)
            [true false read-set]
            (recur (rest todo) read-set)))
        [false (< scan-bound (count wanted)) read-set]))))

(defn lane-rights
  "One turn of the query of CONTRACT 2.4 on tables at one version. Takes
  the question, `:see` or `:acts`, the asker, a party or nil for one who
  is not signed in, and her sitting. Gives {:standing :read-set}.

  `:see`: `:yes` or `:may-not-see`. Everyone's runs are read first, anyone
  at all and, for a signed-in person, anyone signed in; then her own; it
  stops at the first give that lets her see.

  `:acts`: `:holder` when a give of her own lets her see, `:visitor` when
  only everyone's does, `:outside`. Her own run is read first. (The
  contract names the three words and no page says what they are of: this
  is the judge's reading.)

  At the bound of a scan it answers `:too-many-permissions`, never a level
  that the unread keys could turn."
  [tables question asker session]
  (let [own (when (and asker (not (contains? reference/everyone asker))) asker)
        shared (cond (nil? asker) [:anyone]
                     (reference/person? asker) [:anyone :anyone-signed-in]
                     :else [])
        order (case question
                :see (concat shared (when own [own]))
                :acts (concat (when own [own]) shared))]
    (loop [todo order read-set {} cut-before? false]
      (if-let [holder (first todo)]
        (let [[found? cut? read-set] (scan tables read-set holder session)]
          (cond
            (and found? (= :see question)) {:standing :yes :read-set read-set}
            (and found? (= holder own)) {:standing :holder :read-set read-set}
            ;; a visitor is one whose own run was read whole and gave nothing
            (and found? (not cut-before?)) {:standing :visitor :read-set read-set}
            (and (= :acts question) (or cut? cut-before?)) {:standing :too-many-permissions :read-set read-set}
            :else (recur (rest todo) read-set (or cut-before? cut?))))
        {:standing (cond cut-before? :too-many-permissions
                         (= :see question) :may-not-see
                         :else :outside)
         :read-set read-set}))))

(defn answer-of
  "The answer of a query that ran at version `a`: one turn on that
  version's tables. `later` is a version at or after `a`, which a query of
  one turn never reads."
  [vs a _later {:keys [question asker session]}]
  (lane-rights (nth (:tables vs) a) question asker session))

(defn truth
  "What an asker holds in the lane at a version, by the rules and from the
  rules' state alone: for `:see`, `:yes` or `:may-not-see`
  (`reference/may-see`); for `:acts`, `:holder`, `:visitor` or `:outside`."
  [ref {:keys [question asker session]}]
  (let [sees (fn [through?]
               (boolean (some (fn [[id g]]
                                (and (through? (:holder g))
                                     (reference/sees-under? ref lane asker session id)))
                              (:gives ref))))]
    (case question
      :see (if (sees (constantly true)) :yes :may-not-see)
      :acts (cond (sees #(= asker %)) :holder
                  (sees #(contains? reference/everyone %)) :visitor
                  :else :outside))))

(def ^:private rank
  "How much a level tells her: more for a higher number."
  {:yes 1 :may-not-see 0 :holder 2 :visitor 1 :outside 0 :too-many-permissions 0})

(def ^:private levels "The words of a query that are a standing." (set (keys rank)))

;; -------------------------------------------------------------------- the read

(def failed-limit "How many asks in a row may fail before the read ends." 3)
(def ask-limit "How many asks a read sends with no answer of them ever agreeing, before it ends." 24)
(def ended-word "What a read that ended tells: a word, never a level." :rights-not-known)

(defn open-read
  "A read just opened, for {:lane :routing-id :question :asker :session}.

    :flying        the number of the ask in flight, or nil
    :answer        the answer that it holds: that of the last ask answered
    :held          the rights sources that it holds pushes on: those of the
                   held answer's read set
    :at-ask        what each rights push said when the last ask was sent
    :called-back?  whether a rights push called back since then
    :need?         whether it is to ask as soon as no ask is in flight
    :failed        how many asks failed in a row
    :since-agreed  how many asks it sent since an answer last agreed
    :owed?         whether the whole answer is owed
    :told :shown   the level and the facts that it told last"
  [params]
  {:params params :asks 0 :flying nil :answer nil :held #{} :at-ask {} :called-back? false
   :need? true :failed 0 :since-agreed 0 :standing :not-known-yet :owed? true :told nil :shown nil
   :copies {} :ended? false})

(defn present-to
  "What a read takes its pushes to say at a step: what they say now, read
  from the pushes themselves at the step, and not what a callback left
  with it."
  [_read present]
  present)

(defn agrees?
  "True when the read holds an answer and every source of that answer's
  read set is given and says now what the answer saw there."
  [read present]
  (boolean (when-let [answer (:answer read)]
             (every? (fn [[source expected]]
                       (and (contains? present source)
                            (= expected (seen source (get present source)))))
                     (:read-set answer)))))

(defn held-against-all?
  "True when the read holds an answer and every source of its read set is
  given: only then can the answer be said not to agree."
  [read present]
  (boolean (when-let [answer (:answer read)]
             (every? #(contains? present %) (keys (:read-set answer))))))

(defn takes-answer?
  "An answer is taken only when it is the answer of the ask in flight."
  [read n]
  (= n (:flying read)))

(defn called-back?
  "True when a rights push of the read said something new after the last
  ask was sent: a callback came, or a push says now what it did not say
  then."
  [read present]
  (boolean (or (:called-back? read)
               (some (fn [source]
                       (and (contains? present source)
                            (not= (get present source) (get (:at-ask read) source ::none))))
                     (:held read)))))

(defn wants-to-ask?
  "True when the answer was held against all its pushes and does not
  agree, and a rights push said something new after the last ask was sent.
  Otherwise the read waits: the push that is behind will call back."
  [read present]
  (and (held-against-all? read present)
       (not (agrees? read present))
       (called-back? read present)))

(defn may-ask? "One ask is in flight at most." [read] (nil? (:flying read)))

(defn after-failure
  "The read after its ask in flight failed: it is to ask again."
  [read]
  (-> read (assoc :flying nil :need? true) (update :failed inc)))

(defn standing-of
  "What the read holds now: the word of its answer while that answer
  agrees with its pushes; `:not-known-yet` otherwise; the ended word once
  it ended."
  [read present]
  (cond (:ended? read) ended-word
        (agrees? read present) (:standing (:answer read))
        :else :not-known-yet))

(defn tells
  "What the read tells at a step, given its standing: [the read after, the
  tells]. Nothing while it does not know, and the whole answer is owed. A
  whole answer, {:anew {:standing :facts}}, when one is owed or the level
  is another; the lane's facts go with it when she may see, and it waits
  for them. A change, {:changed facts}, when she may see and the facts are
  others."
  [read standing present]
  (let [sees? (= :yes standing)
        facts (get present facts-source)]
    (cond
      (= ended-word standing)
      (if (= ended-word (:told read)) [read []] [(assoc read :told ended-word :owed? true) [{:ended ended-word}]])

      (= :not-known-yet standing) [(assoc read :owed? true) []]

      (and sees? (not (contains? present facts-source))) [read []]

      (or (:owed? read) (not= standing (:told read)))
      [(assoc read :owed? false :told standing :shown (when sees? facts))
       [{:anew (cond-> {:standing standing} sees? (assoc :facts facts))}]]

      (and sees? (not= facts (:shown read))) [(assoc read :shown facts) [{:changed facts}]]

      :else [read []])))

(defn step
  "One step of an open read. Takes the read, an event and what each of its
  pushes that is given says now. An event is `[:opened]`, `[:push source]`
  for a callback, `[:answer n snapshot]`, `[:failed n]` or `[:tick]`. Gives
  [the read after, its effects in order]: `[:leave source]`,
  `[:join source]`, `[:ask n]` and `[:tell what]`.

  Every step does the same after it took the event in: it brings its
  pushes to the read set of the answer that it holds, holds that answer
  against what the pushes say now, and only then asks, and tells."
  [read event present]
  (if (:ended? read)
    [read []]
    (let [[kind a b] event
          read (cond-> read (= :push kind) (assoc-in [:copies a] (get present a)))
          read (case kind
                 :answer (if (takes-answer? read a) (assoc read :answer b :flying nil :failed 0) read)
                 :failed (if (= a (:flying read)) (after-failure read) read)
                 :push (if (contains? (:held read) a) (assoc read :called-back? true) read)
                 read)
          wanted (set (keys (:read-set (:answer read))))
          joins (set/difference wanted (:held read))
          leaves (set/difference (:held read) wanted)
          read (-> read
                   (assoc :held wanted)
                   (update :copies #(apply dissoc % leaves))
                   (update :at-ask #(apply dissoc % leaves)))
          present (apply dissoc (present-to read present) leaves)
          read (cond-> read (agrees? read present) (assoc :since-agreed 0))
          read (cond-> read (wants-to-ask? read present) (assoc :need? true))
          free? (and (:need? read) (may-ask? read))
          over? (and free? (or (< failed-limit (:failed read)) (<= ask-limit (:since-agreed read))))
          asks? (and free? (not over?))
          read (cond over? (assoc read :ended? true :need? false)
                     asks? (-> read
                               (update :asks inc)
                               (update :since-agreed inc)
                               (assoc :flying (inc (:asks read)) :need? false :called-back? false
                                      :at-ask (select-keys present (:held read))))
                     :else read)
          standing (standing-of read present)
          [read told] (tells (assoc read :standing standing) standing present)]
      [read (-> []
                (into (map (fn [source] [:leave source])) (sort-by pr-str leaves))
                (into (map (fn [source] [:join source])) (sort-by pr-str joins))
                (cond-> (= :opened kind) (conj [:join facts-source])
                        asks? (conj [:ask (:flying read)]))
                (into (map (fn [what] [:tell what])) told))])))

(def no-client "A client that holds no read." {:reads {}})

(defn client-step
  "One step of a client that holds several open reads: the event goes to
  the read that it is of, and to no other. An answer lives in the read
  that asked for it. Takes the client, the read's id, the event and what
  that read's pushes say now; `[:opened params]` opens the read. Gives [the
  client after, the effects, each as [the read's id, the effect]]."
  [client rid event present]
  (let [read (or (get-in client [:reads rid]) (open-read (second event)))
        [read effects] (step read (if (= :opened (first event)) [:opened] event) present)]
    [(assoc-in client [:reads rid] read) (mapv (fn [effect] [rid effect]) effects)]))

(defn standing
  "What a read of the client holds now."
  [client rid]
  (get-in client [:reads rid :standing]))

;; ------------------------------------------------------------------- the world

(def ^:private change-memo (atom {}))

(defn- changes-of
  "The versions at which a source says something other than at the version
  before, in order."
  [vs source]
  (let [k [(:id vs) source]]
    (or (get @change-memo k)
        (let [tables (:tables vs)
              found (vec (filter #(not= (source-value (nth tables %) source) (source-value (nth tables (dec %)) source))
                                 (range 1 (count tables))))]
          (swap! change-memo assoc k found)
          found))))

(defn- seen-through
  "The last version up to `now` through which a push that says what the
  source said at `at` still says what the source says."
  [vs source at now]
  (let [next-change (first (filter #(< at %) (changes-of vs source)))]
    (if (and next-change (<= next-change now)) (dec next-change) now)))

(defn- behind? [vs source at now] (< (seen-through vs source at now) now))

(defn- came-back?
  "True when a run says, at this version, what it said at an earlier one
  and did not say in between."
  [vs source at]
  (let [tables (:tables vs)
        now-says (source-value (nth tables at) source)
        earlier (take-while #(<= % at) (changes-of vs source))]
    (and (<= 2 (count earlier))
         (boolean (some #(= now-says (source-value (nth tables (dec %)) source)) (butlast earlier))))))

(def ^:private truth-memo (atom {}))

(defn- level-at
  "The standing that the query gives at a version, for a read's asker,
  sitting and question."
  [vs v params]
  (let [k [(:id vs) v (dissoc params :lane :routing-id)]]
    (or (get @truth-memo k)
        (let [word (:standing (lane-rights (nth (:tables vs) v) (:question params) (:asker params) (:session params)))]
          (swap! truth-memo assoc k word)
          word))))

(defn forget! "Empties what the world remembers of versions that are done." []
  (reset! change-memo {})
  (reset! truth-memo {}))

(defn start-world
  "The world of a schedule over the lane's versions, with the reads that
  are to be opened, each {:lane :routing-id :question :asker :session}.

    :now      the version that the store is at; the lane is made at 1
    :pushes   [read source] -> {:at the version that it says, or nil while
              it is asked for and not given; :joined when it was asked for}
    :wakes    the callbacks that were made and not handed to their read yet
    :asks     [read n] -> {:asked the version when it was sent; :state
              :flying, :failed or :done}
    :handed   read -> the answers that were handed to it
    :broke    what broke, in order, each {:class :read :at}"
  [vs to-open]
  {:vs vs :now 1 :last (dec (count (:tables vs)))
   :client no-client :to-open (vec to-open) :reads {}
   :pushes {} :wakes [] :asks {} :handed {}
   :pushed-since-ask {} :failed-since-ask {} :fails-in-a-row {} :asks-since-agreed {}
   :unknown-since-tell {} :shown {}
   :broke [] :log [] :counts {}})

(defn- present-of
  "What each push of a read that is given says now."
  [{:keys [vs pushes]} rid]
  (into {}
        (keep (fn [[[r source] {:keys [at]}]]
                (when (and (= r rid) at) [source (source-value (nth (:tables vs) at) source)])))
        pushes))

(defn- broke [world rid class]
  (update world :broke conj {:class class :read rid :at (count (:log world))}))

(defn- counted
  ([world k] (counted world k 1))
  ([world k n] (update-in world [:counts k] (fnil + 0) n)))

(defn- rights-pushes
  "The pushes of a read on rights sources that are given, as [source at]."
  [world rid]
  (keep (fn [[[r source] {:keys [at]}]]
          (when (and (= r rid) at (not= facts-source source)) [source at]))
        (:pushes world)))

(defn- an-answer-agrees?
  "True when an answer that was handed to this read says this level and
  agrees with what its pushes say now, entry for entry."
  [world rid level present]
  (boolean (some (fn [{:keys [standing read-set]}]
                   (and (= level standing)
                        (every? (fn [[source expected]]
                                  (and (contains? present source)
                                       (= expected (seen source (get present source)))))
                                read-set)))
                 (get-in world [:handed rid]))))

(defn- stamp-version
  "The version whose act has this stamp."
  [vs stamp]
  (first (keep-indexed (fn [v act] (when (= stamp (:stamp act)) v)) (:acts vs))))

(defn- check
  "Holds one read to what every step is held to, after a step of it that
  had these effects."
  [world rid effects]
  (let [{:keys [vs now]} world
        params (get-in world [:reads rid :params])
        level (standing (:client world) rid)
        present (present-of world rid)
        told (keep (fn [[r [kind what]]] (when (and (= r rid) (= :tell kind)) what)) effects)
        knows? (contains? levels level)
        pushes (rights-pushes world rid)
        floor (if (seq pushes)
                (reduce min (map (fn [[source at]] (seen-through vs source at now)) pushes))
                (get-in world [:reads rid :opened]))
        window (map #(level-at vs % params) (range floor (inc now)))
        answered? (and knows? (an-answer-agrees? world rid level present))
        straddles? (and (seq pushes)
                        (let [ats (map (fn [[source at]] (seen-through vs source at now)) pushes)]
                          (< (reduce min ats) (reduce max ats))))
        world (cond-> world
                true (counted :steps-of-a-read)
                knows? (counted :steps-at-which-a-read-knew)
                (not knows?) (counted :steps-at-which-a-read-did-not-know)
                (and straddles? knows?) (counted :steps-at-which-pushes-said-different-versions-and-the-read-knew)
                (and straddles? (not knows?)) (counted :steps-at-which-pushes-said-different-versions-and-the-read-did-not-know)
                (seq told) (counted :tells (count told)))
        ;; answered
        world (cond-> world
                (and knows? (not answered?))
                (broke rid [:answered :tells-a-level-that-no-answer-of-its-own-agrees-with level]))
        world (cond-> world answered? (assoc-in [:asks-since-agreed rid] 0))
        ;; was, and not more
        world (cond
                (or (not knows?) (some #{level} window)) world

                (every? #(< (rank %) (rank level)) window)
                (broke world rid [:not-more :tells-more-than-she-had-at-any-version-its-pushes-cover level])

                (some (fn [[source at]] (and (= :run (first source)) (came-back? vs source at))) pushes)
                (broke world rid [:came-back :tells-a-level-of-an-old-version-because-a-run-says-again-what-it-said level])

                :else
                (broke world rid [:was :tells-a-level-that-the-lane-had-at-no-version-its-pushes-cover level]))
        ;; silent
        was-unknown? (get-in world [:unknown-since-tell rid] true)
        world (reduce
               (fn [world what]
                 (cond
                   (:ended what) world

                   (not knows?)
                   (broke world rid [:silent :tells-while-it-does-not-know])

                   (and (:anew what) (not= level (:standing (:anew what))))
                   (broke world rid [:silent :tells-another-level-than-it-holds])

                   (and (:changed what) was-unknown?)
                   (broke world rid [:silent :tells-a-change-where-the-whole-answer-is-owed])

                   :else world))
               world
               told)
        ;; a fact that is told was written at a version from which, up to
        ;; now, she could see at some version
        newly (fn [facts] (remove (fn [[place row]] (= row (get-in world [:shown rid place]))) facts))
        world (reduce
               (fn [world what]
                 (let [facts (or (:changed what) (:facts (:anew what)))
                       late (filter (fn [[_ row]]
                                      (let [written (stamp-version vs (:stamp row))]
                                        (not-any? #(= :yes (level-at vs % params)) (range written (inc now)))))
                                    (newly facts))]
                   (cond-> (assoc-in world [:shown rid] facts)
                     (seq late)
                     (broke rid [:fact-after :tells-a-fact-written-after-the-last-version-at-which-she-could-see]))))
               world
               (filter #(or (:changed %) (:facts (:anew %))) told))]
    (assoc-in world [:unknown-since-tell rid]
              (cond (not knows?) true
                    (seq told) false
                    :else was-unknown?))))

(defn- apply-effect
  [world [rid [kind what]]]
  (let [now (:now world)]
    (case kind
      :join (assoc-in world [:pushes [rid what]] {:at nil :joined now})
      :leave (-> world
                 (update :pushes dissoc [rid what])
                 (update :wakes (fn [wakes] (vec (remove #{[rid what]} wakes)))))
      :ask (let [flying? (some (fn [[[r _] {:keys [state]}]] (and (= r rid) (= :flying state))) (:asks world))
                 cause? (or (= 1 what)
                            (get-in world [:failed-since-ask rid])
                            (get-in world [:pushed-since-ask rid]))]
             (-> world
                 (cond-> flying? (broke rid [:asks :asks-while-an-ask-is-in-flight])
                         (not cause?) (broke rid [:asks :asks-though-no-rights-push-said-anything-new-since-its-last-ask]))
                 (assoc-in [:asks [rid what]] {:asked now :state :flying})
                 (assoc-in [:pushed-since-ask rid] false)
                 (assoc-in [:failed-since-ask rid] false)
                 (update-in [:asks-since-agreed rid] (fnil inc 0))
                 (counted :asks)))
      :tell world)))

(defn- hand
  "Hands an event to a read, applies what the read did, and holds the read
  to what every step is held to."
  [world rid event]
  (let [[client effects] (client-step (:client world) rid event (present-of world rid))
        world (reduce apply-effect (assoc world :client client) effects)]
    (check world rid effects)))

(defn- wake
  "A callback of a push was made: it waits to be handed to its read."
  [world rid source]
  (cond-> (if (some #{[rid source]} (:wakes world)) world (update world :wakes conj [rid source]))
    (not= facts-source source) (assoc-in [:pushed-since-ask rid] true)))

(defn enabled
  "What the world can do next, as [weight event] in a fixed order."
  [{:keys [vs now last to-open reads pushes wakes asks] :as world}]
  (-> []
      (cond-> (< now last) (conj [6 [:write]]))
      (into (keep (fn [[rid params]] (when-not (contains? reads rid) [4 [:open rid params]])))
            (map-indexed vector to-open))
      (into (keep (fn [[[rid source] {:keys [at]}]]
                    (cond (nil? at) [3 [:give rid source]]
                          (behind? vs source at now) [2 [:advance rid source]])))
            (sort-by (comp pr-str key) pushes))
      (into (map (fn [[rid source]] [3 [:wake rid source]])) wakes)
      (into (mapcat (fn [[[rid n] {:keys [state]}]]
                      (case state
                        :flying [[3 [:answer rid n]] [1 [:fail rid n]]]
                        :failed [[1 [:answer rid n]]]
                        nil)))
            (sort-by key asks))
      (into (map (fn [rid] [1 [:tick rid]])) (sort (keys reads)))))

(defn- pick
  "The event that a choice picks among what is enabled, by weight."
  [events choice]
  (let [total (reduce + (map first events))]
    (loop [n (mod choice total) events events]
      (let [[weight event] (first events)]
        (if (< n weight) event (recur (- n weight) (rest events)))))))

(defn perform
  "The world after one event. `how` picks the version, where the event has
  one to pick: a number, which picks one of those that it could be;
  `:latest` for the newest; or `[:at version]` for that one."
  [{:keys [vs now] :as world} [kind rid x :as event] how]
  (let [between (fn [low high]
                  (cond (= :latest how) high
                        (vector? how) (second how)
                        :else (+ low (mod how (inc (- high low))))))
        world (update world :log conj [event how])]
    (case kind
      :write (let [act (nth (:acts vs) (inc now))
                   facts (filter #(= reference/permission-key (:k %)) (:facts (:sent act)))
                   changed (keep #(when (= :change (reference/fact-kind (:before act) %)) %) facts)
                   open-reads (vals (get-in world [:client :reads]))
                   rests-on (fn [read source] (contains? (:read-set (:answer read)) source))
                   ;; a give that a read rests on above its own is changed in place
                   above (count (for [read open-reads, fact changed
                                      :when (and (rests-on read [:row (:e fact)])
                                                 (not= (:holder (:v fact)) (:asker (:params read))))]
                                  1))
                   ;; an act that changes more than one give moves what a read
                   ;; rests on, and leaves its level as it was
                   moved (count (for [[rid {:keys [params]}] (:reads world)
                                      :let [read (get-in world [:client :reads rid])]
                                      :when (and (< 1 (count facts))
                                                 (= (level-at vs now params) (level-at vs (inc now) params))
                                                 (some (fn [source] (some #{(inc now)} (changes-of vs source)))
                                                       (keys (:read-set (:answer read)))))]
                                  1))]
               (-> world
                   (assoc :now (inc now))
                   (counted :versions-written)
                   (cond-> (seq changed) (counted :changes-in-place-written (count changed))
                           (pos? above) (counted :changes-in-place-of-a-give-that-an-open-read-rested-on-above-its-own above)
                           (< 1 (count facts)) (counted :acts-written-that-change-more-than-one-give)
                           (pos? moved) (counted :such-acts-that-moved-what-an-open-read-rested-on-and-left-its-level moved))))
      :open (-> world
                (assoc-in [:reads rid] {:params x :opened now})
                (counted :reads-opened)
                (hand rid [:opened x]))
      :give (let [at (between (get-in world [:pushes [rid x] :joined]) now)]
              (-> world
                  (assoc-in [:pushes [rid x] :at] at)
                  (wake rid x)
                  (counted :pushes-given)))
      :advance (let [was (get-in world [:pushes [rid x] :at])
                     at (between (inc (seen-through vs x was now)) now)
                     crossed (count (filter #(and (< was %) (<= % at)) (changes-of vs x)))]
                 (-> world
                     (assoc-in [:pushes [rid x] :at] at)
                     (wake rid x)
                     (counted :callbacks)
                     (cond-> (< 1 crossed) (counted :callbacks-that-brought-more-than-one-change-at-once))))
      :wake (-> world
                (update :wakes (fn [wakes] (vec (remove #{[rid x]} wakes))))
                (cond-> (not= facts-source x) (assoc-in [:pushed-since-ask rid] true))
                (hand rid [:push x]))
      :answer (let [{:keys [asked state]} (get-in world [:asks [rid x]])
                    a (between asked now)
                    later (if (vector? how) (nth how 2 a) (between a now))
                    snapshot (answer-of vs a later (get-in world [:reads rid :params]))
                    newest (reduce max (keep (fn [[[r n] _]] (when (= r rid) n)) (:asks world)))]
                (-> world
                    (assoc-in [:asks [rid x] :state] :done)
                    (update-in [:handed rid] (fnil conj []) snapshot)
                    (cond-> (= :flying state) (assoc-in [:fails-in-a-row rid] 0)
                            (= :failed state) (counted :answers-that-came-after-their-ask-had-failed)
                            (< x newest) (counted :answers-that-came-after-a-later-ask-was-sent)
                            (< a now) (counted :answers-of-an-older-version-than-the-store-was-at))
                    (counted :answers)
                    (hand rid [:answer x snapshot])))
      :fail (-> world
                (assoc-in [:asks [rid x] :state] :failed)
                (assoc-in [:failed-since-ask rid] true)
                (update-in [:fails-in-a-row rid] (fnil inc 0))
                (counted :asks-that-failed)
                (hand rid [:failed x]))
      :tick (hand world rid [:tick]))))

(defn- settle
  "The world stops writing and delivers everything: each push is given, or
  brought to the last version; each callback is handed over; each ask in
  flight is answered at the last version, and none fails. Then each read
  is held to `settles`."
  [world]
  (let [quiet (loop [world world rounds 0]
                (let [next-event (first (keep (fn [[_ [kind :as event]]]
                                                (when (contains? #{:give :advance :wake :answer} kind)
                                                  (when-not (and (= :answer kind)
                                                                 (= :failed (get-in world [:asks [(nth event 1) (nth event 2)] :state])))
                                                    event)))
                                              (enabled world)))]
                  (if (or (nil? next-event) (< 2000 rounds))
                    world
                    (recur (perform world next-event :latest) (inc rounds)))))
        {:keys [vs now]} quiet]
    (reduce
     (fn [world [rid {:keys [params]}]]
       (let [level (standing (:client world) rid)
             is (level-at vs now params)]
         (cond
           (= ended-word level)
           (if (or (< failed-limit (get-in world [:fails-in-a-row rid] 0))
                   (<= ask-limit (get-in world [:asks-since-agreed rid] 0)))
             (counted world :reads-that-ended-for-a-cause)
             (broke world rid [:settles :ends-though-the-world-gave-it-no-cause]))

           (= :not-known-yet level) (broke world rid [:settles :does-not-come-to-know])
           (not= is level) (broke world rid [:settles :comes-to-another-level-than-the-lane-has level is])
           :else world)))
     quiet
     (sort-by key (:reads quiet)))))

(defn agrees-with-the-rules
  "Holds the query to the rules at one version, for one asker, sitting and
  question: the same word, or the bound's word where a run is longer than
  the bound."
  [vs v params]
  (let [says (level-at vs v params)
        is (truth (nth (:refs vs) v) params)]
    (when-not (or (= says is)
                  (and (= :too-many-permissions says)
                       (some (fn [[_ run]] (< scan-bound (count (:stamps run)))) (:runs (nth (:tables vs) v)))))
      {:class [:query :the-query-and-the-rules-differ says is] :read params :at v})))

(defn run-schedule
  "Plays one order of events over the lane's versions: the reads to open,
  and choices, each a pair of numbers, the first picking what happens next
  and the second at which version. It goes on, the choices over again,
  until every version is written, then settles. Gives the world after:
  `:broke` holds what broke, `:counts` what was walked."
  [vs to-open choices]
  (let [world (start-world vs to-open)
        choices (if (seq choices) (vec choices) [[0 0]])
        most (+ (count choices) (* 12 (count (:tables vs))))
        world (loop [world world i 0]
                (let [events (enabled world)]
                  (if (or (empty? events)
                          (<= most i)
                          (and (<= (count choices) i) (= (:now world) (:last world))))
                    world
                    (let [[a b] (nth choices (mod i (count choices)))]
                      (recur (perform world (pick events a) b) (inc i))))))
        world (settle world)
        ;; the query against the rules: for each read at each version, and
        ;; for every asker at the last
        queries (concat (for [[_ {:keys [params]}] (:reads world), v (range 1 (inc (:now world)))]
                          (agrees-with-the-rules vs v params))
                        (for [asker (cons nil h/authors), session (cons nil h/sittings), question [:see :acts]]
                          (agrees-with-the-rules vs (:now world) {:question question :asker asker :session session})))]
    (update world :broke into (remove nil?) queries)))

;; --------------------------------------------------------- every order of a few

(defn- last-change
  "The version at which a source came to say what it says at `at`."
  [vs source at]
  (or (last (filter #(<= % at) (changes-of vs source))) 0))

(defn- successors
  "Every world that one event leads to, with each version that the event
  could pick and that makes a difference: a push is given, or comes, as of
  each version at which its source says something else; a query runs at
  each version that gives another answer. A callback is handed over at
  once. `fails` bounds how many asks may fail in one order."
  [world fails]
  (let [{:keys [vs now]} world
        pushed (fn [event rid source ats]
                 (map (fn [at] (-> world (perform event [:at at]) (perform [:wake rid source] 0))) ats))]
    (mapcat
     (fn [[_ [kind rid x :as event]]]
       (case kind
         :give (let [joined (get-in world [:pushes [rid x] :joined])]
                 (pushed event rid x (cons joined (filter #(and (< joined %) (<= % now)) (changes-of vs x)))))
         :advance (let [was (get-in world [:pushes [rid x] :at])]
                    (pushed event rid x (filter #(and (< was %) (<= % now)) (changes-of vs x))))
         :answer (let [asked (get-in world [:asks [rid x] :asked])
                       params (get-in world [:reads rid :params])]
                   (->> (for [a (range asked (inc now)), later (range a (inc now))]
                          [[:at a later] (answer-of vs a later params)])
                        (reduce (fn [firsts [how snapshot]]
                                  (if (some #(= snapshot (second %)) firsts) firsts (conj firsts [how snapshot])))
                                [])
                        (map (fn [[how _]] (perform world event how)))))
         :fail (when (< (get-in world [:counts :asks-that-failed] 0) fails) [(perform world event 0)])
         (:tick :wake) nil
         [(perform world event 0)]))
     (enabled world))))

(defn- shape
  "A world without what does not decide what it can do next: the order of
  events that led to it, its counts but for how many asks failed, and the
  numbers of its asks."
  [{:keys [vs] :as world}]
  (let [reads (get-in world [:client :reads])]
    [(:now world)
     (get-in world [:counts :asks-that-failed] 0)
     (set (keys (:reads world)))
     (into {} (map (fn [[rid read]] [rid (-> read (dissoc :asks :since-agreed) (update :flying some?))])) reads)
     (into {} (map (fn [[[_ source :as k] {:keys [at joined]}]]
                     [k (if at (last-change vs source at) [:asked joined])]))
           (:pushes world))
     (set (keep (fn [[[rid n] {:keys [asked state]}]]
                  (when (not= :done state) [rid state asked (= n (get-in reads [rid :flying]))]))
                (:asks world)))
     (into {} (map (fn [[rid answers]] [rid (set answers)])) (:handed world))
     (select-keys world [:pushed-since-ask :failed-since-ask :fails-in-a-row :unknown-since-tell :shown])]))

(defn explore
  "Plays every order of events over a few versions, with one read: every
  interleaving of writes, of its pushes given and brought to each version
  at which they say something else, and of its queries answered at each
  version that gives another answer, or failed, `fails` times at most in
  one order. Each world is settled, too. Callbacks are handed over at
  once: the delay of a callback is played by the sampled orders
  (`run-schedule`). Gives {:worlds how many were reached, :broke the kinds
  of break, each with the first order of events that showed it}. With
  `enough?`, it stops as soon as that says so of the kinds found."
  ([vs reader fails] (explore vs reader fails (constantly false)))
  ([vs reader fails enough?]
  (loop [todo [(start-world vs [reader])] seen-shapes #{} found {}]
    (if (or (empty? todo) (enough? found))
      {:worlds (count seen-shapes) :broke found}
      (let [world (peek todo)
            todo (pop todo)
            note (fn [found from]
                   (reduce (fn [found {:keys [class] :as b}]
                             (if (contains? found class) found (assoc found class (assoc b :log (:log from)))))
                           found
                           (:broke from)))
            ;; what broke at the step that led here
            found (note found world)
            world (assoc world :broke [])
            k (shape world)]
        (if (contains? seen-shapes k)
          (recur todo seen-shapes found)
          (recur (into todo (successors world fails))
                 (conj seen-shapes k)
                 (note found (settle world)))))))))

;; ------------------------------------------------------------- the small histories

(defn- give
  [id holder allows by under-id lent-from]
  {:e id :k reference/permission-key
   :v (into {} (remove (comp nil? val))
            {:holder holder :allows (h/allows-of allows) :covers [:default-group lane]
             :given-by by :under under-id :lent-from lent-from})})

(defn- change [fact allows] (assoc-in fact [:v :allows] (h/allows-of allows)))
(defn- end [id] {:e id :k reference/permission-key :v nil})
(defn- note [n] {:e :thing-1 :k :note :v (str "said at " n)})

(defn- see [asker] {:lane lane :routing-id lane :question :see :asker asker :session nil})
(defn- acts [asker] {:lane lane :routing-id lane :question :acts :asker asker :session nil})

(def scenarios
  "Small histories of which every order is played, each as [what, the
  things after the making act, the reads]; with `:the-query-alone` as a
  fourth, a history that only the query is held on, at each version. Asha
  makes the lane and is its owner; her give is `first`."
  (let [first-give h/first-give
        id (fn [n] (h/give-id-of n 0))
        asha h/maker
        by-asha (fn [n facts] (h/thing n 0 asha :sit-1 first-give facts))
        ben :person.ben
        ada :agent.ada]
    [["a transfer in one act: the lane is open to everyone; one act closes it and gives Ben see"
      (let [open (give (id 1) :anyone :see asha first-give nil)]
        [(by-asha 1 [open])
         (by-asha 2 [(end (id 1)) (give (id 2) ben :see asha first-give nil)])])
      [(see ben) (see nil)]]

     ["a transfer in one act, read by one who is in before it: one act gives Ben an owner's give, gives Asha see, and ends Asha's first give"
      [(by-asha 1 [(give (id 1) ben :owner asha first-give nil)
                   (give (h/give-id-of 1 1) asha :see asha first-give nil)
                   (end first-give)])]
      [(see asha) (acts asha)]]

     ["a lane handed over in one act: Asha gives Ben an owner's give and ends her own"
      [(by-asha 1 [(give (id 1) ben :owner asha first-give nil) (end first-give)])]
      [(see asha) (acts ben)]]

     ["a lender lowered and raised in place: Ben lends an agent see; Asha takes see from Ben's give and gives it back"
      (let [bens (give (id 1) ben :member asha first-give nil)]
        [(by-asha 1 [bens])
         (h/thing 2 0 ben :sit-1 (id 1) [(give (id 2) ada :see ben (id 1) (id 1))])
         (by-asha 3 [(change bens :write)])
         (by-asha 4 [(change bens :member)])])
      [(see ada)]]

     ["an end, and a fact written after it: Ben holds see; Asha writes, ends his give, and writes again"
      [(by-asha 1 [(give (id 1) ben :see asha first-give nil)])
       (by-asha 2 [(note 2)])
       (by-asha 3 [(end (id 1))])
       (by-asha 4 [(note 4)])]
      [(see ben)]]

     ["a run that says again what it said: the lane is opened and closed; Ben is given see; one act ends his give and opens the lane"
      (let [open (give (id 1) :anyone :see asha first-give nil)]
        [(by-asha 1 [open])
         (by-asha 2 [(give (id 2) ben :see asha first-give nil)])
         (by-asha 3 [(end (id 1))])
         (by-asha 4 [(end (id 2)) (give (id 4) :anyone :see asha first-give nil)])])
      [(see ben)]]

     ["more gives than a scan reads: in one act Asha lends an agent nine gives of write and one of see"
      [(by-asha 1 (conj (mapv (fn [j] (give (h/give-id-of 1 j) ada :write asha first-give first-give)) (range 9))
                        (give (h/give-id-of 1 9) ada :see asha first-give first-give)))]
      [(see ada)]
      :the-query-alone]]))

(defn scenario-versions
  "The lane over time of a small history: the making act and its things,
  each of which the rules must take."
  [things]
  (let [all (into [h/making-thing] things)
        taken (decisions-of {} all)]
    (when (not= (count all) (count taken))
      (throw (ex-info "the rules refuse a thing of a small history" {:things all :taken (count taken)})))
    (versions-of taken)))

;; ------------------------------------------------------------------ for a trace

(defn- source-line [[kind id]]
  (case kind
    :row (str "the row of " (h/id-mark id))
    :run (str "the run of " (name id))
    :facts "the lane's facts"))

(defn- reader-line [{:keys [question asker session]}]
  (str (if asker (name asker) "one who is not signed in")
       (when session (str " in " (name session)))
       (when (= :acts question) ", of the acts")))

(defn print-order
  "Prints an order of events, one a line, over the lane's versions, with
  what each read held after each event of it. Takes the versions, the
  reads and the events with the version that each picked."
  [vs to-open events]
  (loop [world (start-world vs to-open) todo events]
    (when-let [[event how] (first todo)]
      (let [[kind rid x] event
            after (perform world event how)
            reader (fn [] (reader-line (if (= :open kind) x (get-in after [:reads rid :params]))))
            push (get-in after [:pushes [rid x]])]
        (println
         (format "    %-74s %s"
                 (case kind
                   :write (str "the store writes version " (:now after))
                   :open (str "a read is opened for " (reader))
                   :give (str (source-line x) " is given as of version " (:at push))
                   :advance (str (source-line x) " comes to version " (:at push))
                   :wake (str "the callback of " (source-line x) " is handed over")
                   :answer (str "ask " x " is answered: " (name (:standing (peek (get-in after [:handed rid])))))
                   :fail (str "ask " x " fails")
                   :tick "a step with no cause")
                 (if (contains? #{:write :give :advance} kind)
                   ""
                   (str (reader) " holds " (name (standing (:client after) rid))))))
        (recur after (rest todo))))))
