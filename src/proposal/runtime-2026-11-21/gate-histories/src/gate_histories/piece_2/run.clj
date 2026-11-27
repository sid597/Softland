(ns gate-histories.piece-2.run
  "Generated histories of one lane, sent through the store's own door and
  gate code and held against the statements.

  A history is a list of things that makers send into one lane: gives,
  ends, gives after an end, plain facts, settings; a permission for a run
  and acts sent in it; a yes for one act, with an id of its own that its
  giver makes, given, ended by its giver, by the one above the giver or by
  its holder, given a second time for the same act under a new id, and
  given again at its own place; the act that a yes names, sent by its
  holder under an act-id of her own before the yes lands, while it stands,
  after it was used, as permitted, with a part changed, written another
  way, in another run, under the yes's own id, twice, under each of two
  yeses, and as the yes's own end; a refused try sent again under a new
  act-id; the same thing again under its act-id; another thing under an
  act-id that was used, by the same who and by another; and a thing sent
  under the act-id whose uuid is a yes's id, by a holder who may only see,
  by one who holds nothing, or by anyone. Some makers name a permission
  that is right, and some one that is stale, raised, another holder's, or
  another's yes.

  The histories are grown as test.check grows them, from a seed. Each is
  played to its end: a thing on which the gate and the statements differ is
  noted and taken out of the history, for both, so one history can show
  more than one kind of break. Each kind is named by the statement that
  broke and the road to it. For each kind, the first history that broke so
  is cut down by test.check's own walk of its rose tree, then further by
  taking out each step that the kind does not need (`tighter`).

  Each thing is made as a maker makes it, handed to the door's own pure
  code (`gate-histories.walk/through-the-door`), decided by the gate
  (`gate-histories.walk/decide`, with its answers and check (a)), and read
  back as the door tells its sender (`gate-histories.walk/told`); and it is
  judged by the statements (`gate-histories.piece-2.reference`). A yes's text is
  made as a maker makes it (`hex-for-a-yes`). These must hold for every
  history:

    agree      the gate takes a thing exactly when the statements take it
    told       the door tells each sender what the statements say she is
               told: \"accepted\" when they take her act (T1), \"refused\"
               when they refuse it, the first answer for the same thing
               sent again, and \"not mine\" for another thing under an
               act-id that has an answer
    once       under an act-id that has an answer nothing is decided and
               nothing is written, for the same thing and for another
    outsiders  T2: nothing that an outsider of a yes for one act sends
               writes at the yes's place or uses it up. Held at each
               decision of a thing whose who is not the yes's holder, its
               giver or one above the giver: no row at the yes's place
               among what was written, and the yes's own act, taken before
               the thing, is taken after it
    stamps     the decisions of the lane have the stamps 0, 1, 2 and on
    kept       a decision changes no row but its own: what was taken stays,
               and a refusal writes no fact
    see        after each decision, the read path lets an asker see the
               lane exactly when the statements do: each holder, one who
               was never given anything, each in no run and in each run,
               signed in and not
    unseen     an asker who keeps a read open on a lane that she may not
               see, before a decision and after it, is told nothing at that
               decision: not even an answer that says what it said, which
               would tell her that something of the lane was written
               (`softland.store.read-path.lanes/meaning`)
    screen     what such an asker was shown, after each decision, is what a
               read asked once gives (`gate-histories.live`)
    run-again  the same things, decided again from nothing, give the same
               record

  Run it from this folder:

    clojure -M:piece-2                  ; 2000 histories, a seed of its own
    clojure -M:piece-2 20000 7          ; so many histories, this seed
    clojure -M:piece-2 mutants 3000 1   ; the gate with one rule broken at a time
    clojure -M:piece-2 sketch 20000 7   ; against a sketch of a gate that takes
                                    ; the form of a yes (gate-histories.piece-2.sketch)

  It prints what the histories held, then each kind of break that it found,
  how often, the history at which it first came up, and its smallest
  history step by step; then which kinds are at a yes for one act, and the
  rows of a kind that the walk does not know, by kind. It exits 1 when a
  statement broke."
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.random :as random]
            [clojure.test.check.rose-tree :as rose]
            [clojure.walk :as walk]
            [gate-histories.live :as live]
            [gate-histories.piece-2.reference :as reference]
            [gate-histories.piece-2.sketch :as sketch]
            [gate-histories.walk :as gate]
            [softland.store.act :as act]
            [softland.store.door.digest :as digest]
            [softland.store.gate.chain :as chain]
            [softland.store.gate.decision :as decision]
            [softland.store.read-path.lanes :as lanes]
            [softland.store.record.rows :as record])
  (:import [java.util UUID]))

;; ------------------------------------------------------------------ the world

(def lane "The one lane of a history." :lab)
(def holders "Who can be given a permission. The first makes the lane." [:asha :ben :cal :dee :eve])
(def senders "Who can send: the holders, and one who is never given anything." (conj holders :zed))
(def runs "The runs that a thing can be sent in." [:run-1 :run-2 :run-3])
(def root "The holder of the lane's first permission." (first holders))
(def root-permission "The lane's first permission." [root lane lane])
(def ^:private now "The time on each thing, as the door adds it. No rule reads it." 1700000000000)

(def ^:private yes-high
  "The high half of the uuid of each yes's own id, so that a trace tells a
  yes's id from an act-id: 89, the letter Y. An act-id of a history has 0,
  1 or 2 there."
  89)

(def ^:private live-askers
  "Who keeps a read open through a history, and in which run she asks: one
  who is never given anything, and one holder."
  [[:zed :run-1] [:dee :run-2]])

(def ^:private live-asks
  "What each live read asks: one place that the histories write."
  [:place :thing-1 :colour])

(defn- comparable
  "A value with each byte array as a vector, so that two records compare."
  [x]
  (walk/postwalk (fn [v] (if (bytes? v) (vec v) v)) x))

(defn- act-id-of
  "The act-id of a history's thing with this number: the lane, its depot,
  and the number as the uuid; with `j`, the j-th act-id more of that
  number, for a step that sends more than one thing under a number."
  ([n] (act-id-of n 0))
  ([n j] [lane :streaming (UUID. (long j) (long n))]))

(defn- yes-id-of
  "The id that a giver makes for a yes, from the number of a step."
  [n]
  (UUID. yes-high (long n)))

(defn- maker-act
  "An act as its maker hands it to the door: no act-id, no depot, no time."
  [who session permission facts]
  (cond-> {:version act/version :who who :lane lane :session session :facts (vec facts)}
    permission (assoc :permission permission)))

(defn- thing
  "A thing as it arrives on the streaming depot: the maker's act through the
  door's additions, under the act-id with that number, or its j-th more."
  ([n who session permission facts] (thing n 0 who session permission facts))
  ([n j who session permission facts]
   (gate/through-the-door (maker-act who session permission facts) (act-id-of n j) now)))

(def ^:private making-maker
  "The act that makes the lane, as its maker hands it to the door."
  (maker-act root (first runs) root-permission
             [{:e lane :k :softland.store/depot :v :streaming}
              {:e root :k :softland.store/permission :v root-permission}]))

(def ^:private making-thing
  "The act that makes the lane, as it arrives."
  (gate/through-the-door making-maker (act-id-of 0) now))

(defn- from-the-end
  "The item of a vector that `n` names, counted from its last: 0 is the
  last. nil for an empty vector."
  [v n]
  (when (seq v) (nth v (- (count v) 1 (mod n (count v))))))

(defn hex-for-a-yes
  "The text by which a yes for one act names its act, as the act's maker
  or the yes's giver makes it (store-piece-2/SPEC.md, the form of 22
  November): SHA-256, as 64 hex characters, of the one text of the act's
  one form as `softland.store.act/parse` gives it, without its
  `:permission`. So it holds the act's who, its session, its facts and
  every other part of the maker's act, and no act-id, no depot and no time,
  which the maker's act does not carry. Takes the act as its maker hands it
  to the door, with a permission or without. `parse` wants a permission,
  and the one form of every other part is the same whichever permission is
  there: a first permission of the act's who in its lane is put in for the
  parse and taken out of the one form after. Throws for an act that the
  form refuses: a history makes a yes only for an act that parses."
  [maker]
  (let [parsed (act/parse (assoc maker :permission [(:who maker) (:lane maker) (:lane maker)]))]
    (if-let [one-form (:ok parsed)]
      (digest/hex-of (dissoc one-form :permission))
      (throw (ex-info "a yes is made only for an act that parses" {:act maker :parse parsed})))))

;; ------------------------------------------------------------- the generators

(def ^:private gen-holder (gen/elements holders))
(def ^:private gen-sender (gen/elements senders))
(def ^:private gen-run (gen/elements runs))

(def ^:private gen-recent
  "Which earlier yes or thing a step names, counted from the last: mostly
  the last or the one before it, so that a few steps in a row can work on
  one act."
  (gen/frequency [[6 (gen/return 0)] [3 (gen/return 1)] [2 gen/nat]]))

(def ^:private gen-permission-choice
  "Which permission a maker names: mostly the one she was last given for the
  lane; else an older one, her own with what it allows changed, another
  holder's, the permission of the run that she sends in, of another run, or
  a yes for one act."
  (gen/frequency
   [[12 (gen/return :current)]
    [2 (gen/fmap (fn [n] [:older n]) gen/nat)]
    [1 (gen/return :raised)]
    [1 (gen/fmap (fn [h] [:of h]) gen-holder)]
    [1 (gen/return :run)]
    [1 (gen/fmap (fn [s] [:run-of s]) gen-run)]
    [1 (gen/fmap (fn [n] [:act n]) gen/nat)]]))

(def ^:private gen-fact
  "One fact, said abstractly; `fact-of` makes it concrete at its step."
  (gen/frequency
   [[10 (gen/fmap (fn [[to allows beneath]] {:f :give :to to :allows allows :beneath beneath})
                  (gen/tuple gen-holder
                             (gen/elements [nil :see :see-and-act :see-and-act])
                             (gen/frequency [[15 (gen/return nil)] [1 gen-holder]])))]
    [5 (gen/fmap (fn [of] {:f :end :of of}) gen-holder)]
    [8 (gen/fmap (fn [[e k]] {:f :plain :e e :k k})
                 (gen/tuple (gen/elements [:thing-1 :thing-2]) (gen/elements [:colour :note])))]
    [3 (gen/fmap (fn [[k word]] {:f :setting :key k :word word})
                 (gen/tuple (gen/elements [:softland.store/who-may-see :softland.store/who-may-see
                                           :softland.store/builds-on :softland.store/marked-keys])
                            (gen/elements [:everyone :members])))]
    [8 (gen/fmap (fn [[to run allows wrong]] {:f :give-run :to to :run run :allows allows :wrong wrong})
                 (gen/tuple gen-holder gen-run
                            (gen/elements [:see :see-and-act :see-and-act])
                            (gen/frequency [[20 (gen/return nil)]
                                            [1 (gen/return :on-the-holder)]
                                            [1 (gen/return :under-the-lanes-key)]])))]
    [4 (gen/fmap (fn [run] {:f :end-run :run run}) gen-run)]
    [2 (gen/fmap (fn [n] {:f :end-act :n n}) gen/nat)]]))

(def ^:private gen-yes-fact
  "The one fact of the act that a yes is for: mostly a plain fact, which a
  yes can let be taken; else any fact, most of which no yes lets be taken
  (a give, R10; a setting, R7)."
  (gen/frequency
   [[2 (gen/fmap (fn [[e k]] {:f :plain :e e :k k})
                 (gen/tuple (gen/elements [:thing-1 :thing-2]) (gen/elements [:colour :note])))]
    [1 gen-fact]]))

(def ^:private gen-op
  "One step of a history."
  (gen/frequency
   [[30 (gen/fmap (fn [[by in choice fact]] {:op :act :by by :in in :permission choice :facts [fact]})
                  (gen/tuple gen-holder gen-run gen-permission-choice gen-fact))]
    [4 (gen/fmap (fn [[by in choice f1 f2]] {:op :act :by by :in in :permission choice :facts [f1 f2]})
                 (gen/tuple gen-holder gen-run gen-permission-choice gen-fact gen-fact))]
    ;; the two steps that make an end and a give after it common: the giver
    ;; of a holder's last permission ends it, or gives exactly that
    ;; permission once more
    [3 (gen/fmap (fn [[of in]] {:op :giver-ends :of of :in in}) (gen/tuple gen-holder gen-run))]
    [3 (gen/fmap (fn [[to in]] {:op :giver-gives-again :to to :in in}) (gen/tuple gen-holder gen-run))]
    [2 (gen/fmap (fn [n] {:op :again :n n}) gen/nat)]
    [1 (gen/return {:op :make-again})]
    [9 (gen/fmap (fn [[by in fact as]] {:op :act-in-run :by by :in in :facts [fact] :as as})
                 (gen/tuple gen-holder gen-run gen-fact
                            (gen/frequency [[9 (gen/return :its-holder)] [1 (gen/return :another)]])))]
    ;; a yes for one act, made and given, and what its holder sends. Its
    ;; giver is as chosen, or a holder who was given a permission for the
    ;; lane, so that a giver is often not the first holder and one above
    ;; her can end the yes or give a second one
    [6 (gen/fmap (fn [[[by giver] in choice for for-in fact then]]
                   {:op :yes-for-one-act :by by :giver giver :in in :permission choice
                    :for for :for-in for-in :fact fact :then then})
                 (gen/tuple (gen/tuple gen-holder (gen/elements [:as-chosen :one-who-was-given-one]))
                            gen-run
                            (gen/frequency [[6 (gen/return :current)] [1 (gen/return :run)]])
                            gen-holder gen-run gen-yes-fact
                            (gen/elements [:nothing :send :send :changed :another-run :before
                                           :written-another-way :under-its-own-id :twice
                                           :two-yeses :its-own-end :nothing])))]
    ;; a step on a yes that was made: its act sent under a new act-id, the
    ;; yes ended, a second yes for the same act under a new id, or a give
    ;; at the yes's own place again
    [16 (gen/fmap (fn [[n what by in]] {:op :on-an-act :n n :what what :by by :in in})
                  (gen/tuple gen-recent
                             (gen/elements [:send :send :send :end-by-giver :end-by-above :end-by-holder
                                            :another-by-giver :another-by-above :another-by-another
                                            :at-its-place-by-giver :at-its-place-by-another])
                             gen-holder gen-run))]
    ;; a thing under the act-id whose uuid is a yes's own id: by a holder
    ;; who may only see, by one who holds nothing, or by anyone
    [5 (gen/fmap (fn [[n who by in fact]] {:op :under-its-id :n n :who who :by by :in in :fact fact})
                 (gen/tuple gen-recent
                            (gen/elements [:one-who-may-only-see :one-who-holds-nothing :anyone])
                            gen-sender gen-run gen-fact))]
    ;; another thing under an act-id that was used
    [3 (gen/fmap (fn [[n as by in fact]] {:op :used-act-id :n n :as as :by by :in in :fact fact})
                 (gen/tuple gen-recent (gen/elements [:same :another]) gen-holder gen-run gen-fact))]]))

(def gen-history
  "A history: up to eighty steps after the making act."
  (gen/vector gen-op 0 80))

;; ------------------------------------------------ from a step to the things sent

(defn- current-permission
  "The permission for the lane that a holder was last given, by the gives
  that the gate took; a guess beneath the first permission for a holder who
  was given none."
  [pools holder]
  (or (peek (get-in pools [:lane holder])) [holder lane lane root-permission]))

(defn- run-permission
  "The permission that a run was last given, by the gives that the gate
  took; a guess for a run that was given none, with `who` as its holder."
  [pools who run]
  (or (peek (get-in pools [:run run])) [who lane lane root-permission :see-and-act [:run run]]))

(defn- raised
  "A holder's permission with what it allows changed: \"see\" becomes \"see
  and act\", and the slot is dropped or added. A first permission has none."
  [permission]
  (case (count permission)
    5 (if (= :see (nth permission 4))
        (assoc permission 4 :see-and-act)
        (subvec permission 0 4))
    4 (conj permission :see-and-act)
    permission))

(defn- named-permission
  "The permission that the maker of a step names. `in` is the run that she
  sends in."
  [pools by in choice]
  (let [pool (get-in pools [:lane by])
        kind (if (keyword? choice) choice (first choice))]
    (case kind
      :current (current-permission pools by)
      :raised (raised (current-permission pools by))
      :older (or (from-the-end pool (second choice)) (current-permission pools by))
      :of (current-permission pools (second choice))
      :run (run-permission pools by in)
      :run-of (run-permission pools by (second choice))
      :act (or (:permission (from-the-end (:act pools) (second choice)))
               (current-permission pools by)))))

(defn- fact-of
  "One fact as its maker sends it. `n` marks the value of a plain fact."
  [pools n permission {:keys [f to allows beneath of e k key word run wrong] :as fact}]
  (case f
    :give {:e to
           :k :softland.store/permission
           :v (cond-> [to lane lane (if beneath (current-permission pools beneath) permission)]
                allows (conj allows))}
    :give-run {:e (if (= :on-the-holder wrong) to run)
               :k (if (= :under-the-lanes-key wrong) :softland.store/permission :softland.store/run-permission)
               :v [to lane lane permission allows [:run run]]}
    :end {:e of :k :softland.store/permission}
    :end-run {:e run :k :softland.store/run-permission}
    :end-act {:e (if-let [entry (from-the-end (:act pools) (:n fact))]
                   (:yes entry)
                   (UUID. yes-high -1))
              :k :softland.store/act-permission}
    :plain {:e e :k k :v (str "said at " n)}
    :setting {:e lane
              :k key
              :v (case key
                   :softland.store/who-may-see word
                   :softland.store/builds-on []
                   :softland.store/marked-keys #{:colour})}))

(defn- one-fact-a-place
  "Facts with one for each place, the first kept: an act of a history writes
  a place once."
  [facts]
  (vals (reduce (fn [seen fact]
                  (let [place [(:e fact) (:k fact)]]
                    (if (contains? seen place) seen (assoc seen place fact))))
                (array-map)
                facts)))

(defn- made-yes
  "A yes for one act, made by its giver before the act is sent: its own id,
  and the permission that names the act by the text that a maker makes of
  it (`hex-for-a-yes`). Takes the yes's id, the act as its maker will hand
  it to the door, without its permission, and the giver's own permission.
  Gives {:entry, for the pools: :permission :maker :yes; :named, [[yes
  text] the act], for the statements}."
  [yes maker givers]
  (let [text (hex-for-a-yes maker)
        pa [(:who maker) lane lane givers :see-and-act [:act yes text]]]
    {:entry {:permission pa :maker maker :yes yes}
     :named [[yes text] maker]}))

(defn- give-of
  "The fact that gives a yes: the yes's permission, on the yes's own id,
  under the key of a yes for one act."
  [entry]
  {:e (:yes entry) :k :softland.store/act-permission :v (:permission entry)})

(defn- permitted
  "The thing that the holder of a yes sends: the act that the yes was for,
  under the yes, through the door under the act-id given, which is any
  act-id of hers. `change` makes another act of it first."
  ([entry act-id] (permitted entry act-id identity))
  ([entry act-id change]
   (gate/through-the-door (change (assoc (:maker entry) :permission (:permission entry))) act-id now)))

(defn- written-another-way
  "The same act part for part, written as a maker may also write it: its
  facts as a list, and the two parts that may be left out given empty."
  [maker]
  (-> maker
      (assoc :subjects #{} :based-on {})
      (update :facts #(apply list %))))

(defn- one-who-was-given-one
  "A holder other than the first who was given a permission for the lane
  by a give that the gate took, the last of them in the order of the
  holders; `by` when there is none."
  [pools by]
  (or (last (filter #(and (not= root %) (seq (get-in pools [:lane %]))) holders)) by))

(defn- yes-for-one-act
  "The things of a yes for one act: the give of a yes, with an id of its
  own, for an act that is not sent yet, and what its holder sends with it,
  each under an act-id of hers. `n` and `n + 1` are the step's two numbers:
  the yes's id and the give's act-id are of `n`, the act's act-ids of
  `n + 1`; a second yes, for `:two-yeses`, has the id of `n + 1`.

    :nothing              the give alone
    :send                 the give, then the act
    :before               the act, refused, as the yes is not there yet;
                          the give; the act again under a new act-id
    :changed              the give, then the act with a fact added
    :another-run          the give, then the act in another run
    :written-another-way  the give, then the act part for part, written
                          another way
    :under-its-own-id     the give, then the act under the act-id whose uuid
                          is the yes's own id
    :twice                the give, the act, and the act again under a new
                          act-id: the second finds the yes used
    :two-yeses            two yeses for the act, each with its own id, and
                          the act under each, under two act-ids
    :its-own-end          the act is the end of the yes itself: the give,
                          then the act"
  [pools n {:keys [by giver in permission for for-in fact then]}]
  (let [by (if (= :one-who-was-given-one giver) (one-who-was-given-one pools by) by)
        givers (named-permission pools by in permission)
        yes (yes-id-of n)
        maker (maker-act for for-in nil
                         [(if (= :its-own-end then)
                            {:e yes :k :softland.store/act-permission}
                            (fact-of pools n givers fact))])
        first-yes (made-yes yes maker givers)
        second-yes (when (= :two-yeses then) (made-yes (yes-id-of (inc n)) maker givers))
        entry (:entry first-yes)
        give (fn [made j] (thing n j by in givers [(give-of (:entry made))]))
        act-at (fn [j] (act-id-of (inc n) j))]
    {:entries (cond-> [entry] second-yes (conj (:entry second-yes)))
     :named (cond-> [(:named first-yes)] second-yes (conj (:named second-yes)))
     :sends (case then
              :nothing [(give first-yes 0)]
              (:send :its-own-end) [(give first-yes 0) (permitted entry (act-at 0))]
              :before [(permitted entry (act-at 0)) (give first-yes 0) (permitted entry (act-at 1))]
              :changed [(give first-yes 0)
                        (permitted entry (act-at 0) #(update % :facts conj {:e :thing-9 :k :note :v "added after the yes"}))]
              :another-run [(give first-yes 0)
                            (permitted entry (act-at 0) #(assoc % :session (first (remove #{for-in} runs))))]
              :written-another-way [(give first-yes 0) (permitted entry (act-at 0) written-another-way)]
              :under-its-own-id [(give first-yes 0) (permitted entry [lane :streaming yes])]
              :twice [(give first-yes 0) (permitted entry (act-at 0)) (permitted entry (act-at 1))]
              :two-yeses [(give first-yes 0) (give second-yes 1)
                          (permitted entry (act-at 0)) (permitted (:entry second-yes) (act-at 1))])}))

(defn- on-an-act
  "The thing of a step on a yes that was made: its act sent by its holder
  under a new act-id; an end of the yes, by its giver, by the one above the
  giver, or by its holder under her permission for the lane; a second yes
  for the same act and holder, with a new id, by its giver, by the one
  above the giver, or by another holder, each beneath her own permission
  for the lane; or a give at this yes's own place again, of the same yes
  beneath the giver's own permission or beneath another's. A second yes is
  another entry of the pools."
  [pools n {:keys [what by in]} entry]
  (let [pa (:permission entry)
        yes (:yes entry)
        giver (first (reference/above pa))
        above-giver (or (first (reference/above (reference/above pa))) giver)
        end-by (fn [who]
                 {:sends [(thing n who in (current-permission pools who)
                                 [{:e yes :k :softland.store/act-permission}])]})
        another-by (fn [who]
                     (let [own (current-permission pools who)
                           made (made-yes (yes-id-of n) (:maker entry) own)]
                       {:entries [(:entry made)] :named [(:named made)]
                        :sends [(thing n who in own [(give-of (:entry made))])]}))
        at-its-place-by (fn [who]
                          (let [own (current-permission pools who)]
                            {:sends [(thing n who in own [(give-of (assoc entry :permission (assoc pa 3 own)))])]}))]
    (case what
      :send {:sends [(permitted entry (act-id-of n))]}
      :end-by-giver (end-by giver)
      :end-by-above (end-by above-giver)
      :end-by-holder (end-by (first pa))
      :another-by-giver (another-by giver)
      :another-by-above (another-by above-giver)
      :another-by-another (another-by by)
      :at-its-place-by-giver (at-its-place-by giver)
      :at-its-place-by-another (at-its-place-by by))))

(defn- pick
  "The earlier item that a step names: the one that `:target` holds, when
  the step was pinned to it and it is there (`tighter`); else the one that
  its number names from the end."
  [items step]
  (or (when-let [t (:target step)] (last (filter #(= t %) items)))
      (from-the-end items (:n step))))

(defn- things-of
  "What one step sends: {:sends the things in order}, and for a step that
  makes a yes, `:entries` for the pools and `:named` for the statements;
  `:again?` for a step that sends an earlier thing once more; and
  `:picked`, the earlier yes or thing that the step named."
  [{:keys [pools things n]} {:keys [op by in permission facts] :as step}]
  (case op
    :make {:sends [making-thing]}
    :act (let [named (named-permission pools by in permission)]
           {:sends [(thing n by in named (one-fact-a-place (map #(fact-of pools n named %) facts)))]})
    :act-in-run (let [named (run-permission pools by in)
                      ;; mostly sent by the holder of the run's permission
                      who (if (= :its-holder (:as step)) (first named) by)]
                  {:sends [(thing n who in named (one-fact-a-place (map #(fact-of pools n named %) facts)))]})
    :yes-for-one-act (yes-for-one-act pools n step)
    :on-an-act (if-let [entry (pick (:act pools) step)]
                 (assoc (on-an-act pools n step entry) :picked entry)
                 {:sends []})
    :under-its-id (if-let [given (pick (or (:given-acts pools) (mapv :permission (:act pools))) step)]
                    (let [yes (second (nth given 5))
                          who (case (:who step)
                                ;; a holder whose last permission for the lane says see
                                :one-who-may-only-see
                                (or (first (filter (fn [h] (= :see (nth (peek (get-in pools [:lane h])) 4 nil)))
                                                   (remove #{(first given)} holders)))
                                    by)
                                :one-who-holds-nothing :zed
                                :anyone by)
                          own (current-permission pools who)]
                      {:picked given
                       :sends [(gate/through-the-door (maker-act who in own [(fact-of pools n own (:fact step))])
                                                      [lane :streaming yes] now)]})
                    {:sends []})
    :used-act-id (let [earlier (pick things step)
                       who (if (= :same (:as step)) (:who earlier) by)
                       own (current-permission pools who)]
                   {:picked earlier
                    :sends [(gate/through-the-door (maker-act who in own [(fact-of pools n own (:fact step))])
                                                   (:act-id earlier) now)]})
    (:giver-ends :giver-gives-again)
    (let [holder (or (:of step) (:to step))
          hers (current-permission pools holder)
          above (nth hers 3 nil)
          fact (if (= :giver-ends op)
                 {:e holder :k :softland.store/permission}
                 {:e holder :k :softland.store/permission :v hers})]
      ;; the first holder has no giver: she ends, or gives, her own
      {:sends [(if (nil? above)
                 (thing n holder in hers [fact])
                 (thing n (first above) in above [fact]))]})
    :make-again {:sends [(gate/through-the-door making-maker (act-id-of n) now)]}
    :again (let [earlier (pick things step)] {:sends [earlier] :again? true :picked earlier})))

;; ------------------------------------------------------------ playing a history

(def tally
  "What the histories of a run held: counted while they play."
  (atom {}))

(defn- count! [k] (swap! tally update k (fnil inc 0)))

(defn- gives-of
  "The gives of a thing, as [kind permission]."
  [sent]
  (keep (fn [{:keys [k v]}]
          (when (and (reference/permission-keys k) (some? v) (vector? v))
            [(reference/permission-keys k) v]))
        (:facts sent)))

(defn- with-gives
  "The pools with the gives of a thing that the gate took, for the lane,
  for a run, and for one act. A yes for one act goes into the pools' `:act`
  when it is made, whether or not its give is taken; its permission goes
  into `:given-acts` when the gate takes the give."
  [pools sent]
  (reduce (fn [pools [kind permission]]
            (case kind
              :lane (update-in pools [:lane (first permission)] (fnil conj []) permission)
              :run (update-in pools [:run (second (nth permission 5))] (fnil conj []) permission)
              :act (update pools :given-acts (fnil conj []) permission)))
          pools
          (gives-of sent)))

(defn- covers-word
  "What a thing's permission covers, in a word: `:lane`, `:run` or `:act`."
  [sent]
  (first (reference/covers (:permission sent))))

(defn- permitted-act?
  "True for a thing that its holder sends under a yes for one act."
  [sent]
  (and (= :act (covers-word sent)) (= (:who sent) (first (:permission sent)))))

(defn- yes-of
  "The id of the yes that a permission for one act is, or nil."
  [permission]
  (let [[kind yes] (reference/covers permission)]
    (when (= :act kind) yes)))

(defn- yes-place-key
  "The key of the place of a yes in the record: its own id, under the key of
  a yes for one act."
  [yes]
  (record/place-key lane yes :softland.store/act-permission))

(defn- yeses-of
  "Each yes for one act that the statements took, as [grant number,
  permission], in the order given."
  [ref]
  (for [[n grant] (sort-by key (:grants ref))
        :when (yes-of (:permission grant))]
    [n (:permission grant)]))

(defn- at-a-yes?
  "True when a break is at a yes for one act: the thing names a yes as its
  permission, gives or ends a yes, or goes under the act-id whose uuid is a
  yes's own id; or what broke is T2."
  [sent statement]
  (boolean (or (= :outsiders statement)
               (and sent (= :act (covers-word sent)))
               (some #(= :softland.store/act-permission (:k %)) (:facts sent))
               (when-let [^UUID u (nth (:act-id sent) 2 nil)]
                 (= yes-high (.getMostSignificantBits u))))))

(defn- row-violation
  "The statement `kept` for one decision. Takes the facts before and after,
  and the outcome. Gives nil, or what broke."
  [before after outcome]
  (let [written (into #{} (keep (fn [[which k]] (when (= :fact which) k))) (:writes outcome))
        refused? (not= :accepted (:verdict outcome))]
    (cond
      (and refused? (seq written))
      {:statement :kept :class [:kept :a-refusal-wrote-a-fact] :what "a refusal wrote a fact" :keys written}

      (not= (comparable (apply dissoc before written)) (comparable (apply dissoc after written)))
      {:statement :kept :class [:kept :a-row-that-it-did-not-write]
       :what "a decision changed a row that it did not write"})))

(defn- rows-at [facts ks] (into {} (map (fn [k] [k (get facts k)])) ks))

(defn- seeing
  "What the read path says of one asker and the lane, from the record: the
  lane's own rows, her permission row and the rows above it, and, for a
  read asked in a run, the row of the run's permission and the rows above
  it (`softland.store.read-path.lanes`)."
  [facts who run signed-in?]
  (let [own-rows (into {}
                       (keep (fn [k] (let [place (record/place-key lane lane k)]
                                       (when-let [row (get facts place)] [place row]))))
                       [:softland.store/depot :softland.store/who-may-see
                        :softland.store/builds-on :softland.store/marked-keys])
        above-of (fn [row] (if-let [held (record/held-permission row)]
                             (rows-at facts (chain/places-above lane held))
                             {}))
        permission-row (get facts (record/place-key lane who :softland.store/permission))
        run-row (when run (get facts (record/place-key lane run :softland.store/run-permission)))]
    (lanes/standing lane own-rows
                     (lanes/either (lanes/permission-state lane permission-row (above-of permission-row))
                                    (if run (lanes/run-permission-state lane who run-row (above-of run-row)) :none))
                     signed-in?)))

(defn- see-violation
  "The statement `see` after one decision. Takes the record's facts and the
  statements' state. Gives nil, or the first asker for whom the read path
  and the statements differ."
  [facts ref]
  (first
   (for [who senders
         run (cons nil runs)
         signed-in? [true false]
         :when (or signed-in? (and (= who root) (nil? run)))
         :let [read-path (seeing facts who run signed-in?)
               statements (reference/may-see ref who run signed-in?)]
         :when (not= read-path statements)]
     {:statement :see
      :class [:see read-path statements]
      :what (str "the read path says " (name read-path) " and the statements say " (name statements)
                 " for " (name who) (when run (str " in " (name run))) (when-not signed-in? ", not signed in"))})))

(defn- open-live
  "The live reads of a history, opened when the lane is made."
  [state]
  (let [tables (live/tables (:rec state) :streaming)]
    (assoc state :live
           (mapv (fn [[who run]]
                   {:who who :run run
                    :read (live/open tables (live/check {:asks live-asks :asker who :session run :lanes [lane]}))})
                 live-askers))))

(defn- live-step
  "The statements `unseen` and `screen` at one decision. Takes the state
  before, the record and the statements' state after. Gives [the live reads
  after, nil or what broke]."
  [state rec ref]
  (if (or (empty? (:live state))
          (and (identical? (:facts rec) (:facts (:rec state)))
               (identical? (:by-value rec) (:by-value (:rec state)))))
    [(:live state) nil]
    (let [tables (live/tables rec :streaming)]
      (reduce
       (fn [[reads broke] {:keys [who run read] :as one}]
         (let [[read tells] (live/after-writes read tables)
               unseen? (and (= :may-not-see (reference/may-see (:ref state) who run true))
                            (= :may-not-see (reference/may-see ref who run true)))
               truth (live/once tables (:query read))]
           [(conj reads (assoc one :read read))
            (or broke
                (when (and unseen? (seq tells))
                  {:statement :unseen
                   :class [:unseen (if (:anew (first tells)) :a-whole-answer :a-changed-place)]
                   :what (str (name who) ", asking in " (name run) ", may not see the lane before and after, and was told "
                              (count tells) " time" (when (< 1 (count tells)) "s") ": " (pr-str (first tells)))})
                (when (not= truth (:screen read))
                  {:statement :screen
                   :class [:screen]
                   :what (str (name who) " in " (name run) " was shown " (pr-str (:screen read))
                              "; a read asked once gives " (pr-str truth))}))]))
       [[] nil]
       (:live state)))))

(defn- relation
  "How the who of the first thing that the gate decided under an act-id
  stands to the thing sent under it now: for a thing under a yes for one
  act, `:its-holder`, `:its-giver-or-above` or `:an-outsider`, by the yes's
  id; else `:the-same-who` or `:another-who`."
  [first-who sent]
  (let [pa (:permission sent)]
    (if (= :act (covers-word sent))
      (cond (= first-who (first pa)) :its-holder
            (contains? (reference/inside pa) first-who) :its-giver-or-above
            :else :an-outsider)
      (if (= first-who (:who sent)) :the-same-who :another-who))))

(defn- road-of
  "The road of a step, for the kinds of break of the act that a yes names:
  the word of the step that sent it."
  [op]
  (case (:op op)
    :yes-for-one-act (:then op)
    :on-an-act (:what op)
    (:op op)))

(defn- agree-violation
  "The statement `agree` for a thing that the statements decided. Gives nil,
  or what broke, with its kind: for a thing under a yes that the gate
  refused, the gate's word and the road of the step that sent it."
  [state op sent outcome ruled]
  (let [taken? (= :accepted (:verdict outcome))]
    (when (not= taken? (boolean (:accept? ruled)))
      (if taken?
        {:statement :agree
         :class [:gate-took-what-the-statements-refuse (covers-word sent) (:why ruled)]
         :what "the gate took what the statements refuse"}
        (let [first-under (get (:first-under state) (:act-id sent))]
          {:statement :agree
           :class (if (:repeat outcome)
                    [:gate-refused-what-the-statements-take (covers-word sent) :act-id-answered
                     (relation (:who first-under) sent)
                     (if (= :accepted (:verdict first-under)) :after-an-accept :after-a-refusal)]
                    (cond-> [:gate-refused-what-the-statements-take (covers-word sent) (:verdict outcome)]
                      (= :act (covers-word sent)) (conj (road-of op))))
           :what "the gate refused what the statements take"})))))

(def ^:private probe-act-id
  "The act-id under which T2 asks whether a yes's own act would be taken:
  one that no history uses."
  [lane :streaming (UUID. -1 -1)])

(defn- probe-of
  "The act that a yes names, sent by its holder under the yes, through the
  door under `probe-act-id`; nil for a yes that names no act that the
  history made. What T2 asks the gate and the statements about, and keeps
  nothing of."
  [named pa]
  (let [[_ yes text] (reference/covers pa)]
    (when-let [maker (get named [yes text])]
      (gate/through-the-door (assoc maker :permission pa) probe-act-id now))))

(defn- gate-takes?
  "True when the gate, on this record, takes the thing."
  [rec t]
  (= :accepted (:verdict (:outcome (gate/decide rec :streaming t)))))

(defn- statements-take?
  "True when the statements, in this state, take the thing."
  [ref named t]
  (binding [reference/*act-named* named]
    (boolean (:accept? (reference/judge ref lane t)))))

(defn- outsiders-violation
  "T2 for one decision: nothing that an outsider of a yes for one act sends
  writes at the yes's place or uses it up. Takes the state before the
  decision, the thing, the gate's outcome and its record after, and the
  statements' judgment. Only a thing that was taken can write, so for a
  thing that the gate or the statements took, and for each yes that the
  statements took before it whose outsider sent it (not its holder, its
  giver, or one above the giver):

    wrote at its place  the gate wrote a row at the yes's place, or the
                        statements took a fact on the yes's id under the
                        key of a yes
    used it up          the yes stood unused, its own act, sent by its
                        holder under a fresh act-id, was taken before the
                        thing and is not after it: by the gate, or by the
                        statements

  Gives nil, or what broke, with its kind: in the gate alone, in the
  statements alone, or in both; the road; and what the yes's place held
  before, in the statements' word (`gate-histories.piece-2.reference/place-state`)."
  [state sent outcome record ruled]
  (let [who (:who sent)
        ref (:ref state)
        named (:named state)
        gate-took? (= :accepted (:verdict outcome))
        statements-took? (boolean (:accept? ruled))]
    (when (or gate-took? statements-took?)
      (first
       (for [[_ pa] (yeses-of ref)
             :when (not (contains? (reference/inside pa) who))
             :let [yes (yes-of pa)
                   held (reference/place-state ref pa)
                   gate-wrote? (and gate-took?
                                    (boolean (some (fn [[which k]] (and (= :fact which) (= k (yes-place-key yes))))
                                                   (:writes outcome))))
                   statements-wrote? (and statements-took?
                                          (boolean (some (fn [{:keys [e k]}] (and (= e yes) (= k :softland.store/act-permission)))
                                                         (:facts sent))))
                   t (when (and (= :stands held) (not gate-wrote?) (not statements-wrote?))
                       (probe-of named pa))
                   takeable? (boolean (and t (statements-take? ref named t)))
                   gate-used? (boolean (and takeable? gate-took?
                                            (not (gate-takes? record t))
                                            (gate-takes? (:rec state) t)))
                   statements-used? (boolean (and takeable? statements-took?
                                                  (not (statements-take? (:state ruled) named t))))
                   gate-broke? (or gate-wrote? gate-used?)
                   statements-broke? (or statements-wrote? statements-used?)]
             :when (or gate-broke? statements-broke?)]
         {:statement :outsiders
          :differ? (not= gate-took? statements-took?)
          :class [:outsiders
                  (cond (and gate-broke? statements-broke?) :the-gate-and-the-statements
                        gate-broke? :the-gate-alone
                        :else :the-statements-alone)
                  (if (or gate-wrote? statements-wrote?) :wrote-at-its-place :used-it-up)
                  held]
          :what (str (name who) " is an outsider of the yes y" (.getLeastSignificantBits ^UUID yes)
                     ", whose place held " (name held) ", and "
                     (cond (or gate-wrote? statements-wrote?)
                           (str "her thing wrote at its place, in " (if gate-wrote? "the gate" "")
                                (when (and gate-wrote? statements-wrote?) " and ")
                                (if statements-wrote? "the statements" ""))
                           :else
                           (str "after her thing the yes's own act is no longer taken, by "
                                (if gate-used? "the gate" "") (when (and gate-used? statements-used?) " and ")
                                (if statements-used? "the statements" ""))))})))))

(defn- yes-place-word
  "What the place of a yes holds now in the statements, in a word, by the
  yes's id: `:nothing`, `:ended`, `:used`, `:stands`, or `:ended-above`."
  [ref yes]
  (let [at (get-in ref [:at [:act yes]])]
    (cond (nil? at) :nothing
          (= :end at) :ended
          (reference/used? ref at) :used
          (reference/alive? ref at) :stands
          :else :ended-above)))

(defn- count-roads!
  "Counts the roads of a history that come from the form of a yes and from
  one answer for an act-id, so that a run says how often it walked each."
  [state op sent outcome ruled]
  (let [first-under (get (:first-under state) (:act-id sent))
        ref (:ref state)
        decided? (not (or (:repeat? ruled) (:not-mine? ruled)))]
    (when (:repeat? ruled) (count! [:act-ids :the-same-thing-again]))
    (when (:not-mine? ruled)
      (count! [:act-ids (if (= (:who first-under) (:who sent))
                          :another-thing-by-the-same-who
                          :another-thing-by-another-who)]))
    (when (and decided? (permitted-act? sent))
      (count! [:the-act-under-its-yes (reference/place-state ref (:permission sent)) (:verdict outcome)])
      (when (some #(not= :accepted %) (get-in state [:tries (yes-of (:permission sent))]))
        (count! [:sent-again-under-a-new-act-id-after-a-refused-try (:verdict outcome)])))
    (when (= :under-its-id (:op op))
      (count! [:a-thing-under-a-yes-id (if decided? (:verdict outcome) :act-id-answered)]))
    (when (and (= :on-an-act (:op op)) (#{:at-its-place-by-giver :at-its-place-by-another} (:what op)) decided?)
      (count! [:a-give-at-a-yes-place-again (:what op)
               (yes-place-word ref (:e (first (:facts sent)))) (:verdict outcome)]))
    (when (= :accepted (:verdict outcome))
      (doseq [{:keys [k v]} (:facts sent)
              :when (= :softland.store/act-permission k)]
        (if (nil? v)
          (count! [:a-yes :ended])
          (count! [:a-yes (if (some (fn [[_ pa]] (= (nth (nth pa 5) 2) (nth (nth v 5) 2))) (yeses-of ref))
                            :given-for-an-act-that-was-given-one
                            :given)])))
      (when (= :act (covers-word sent))
        (let [text (nth (nth (:permission sent) 5) 2)]
          (count! [:a-yes (if (some (fn [[n pa]] (and (not= pa (:permission sent))
                                                      (= text (nth (nth pa 5) 2))
                                                      (reference/used? ref n)))
                                    (yeses-of ref))
                            :used-after-another-for-the-same-act-was-used
                            :used)]))))))

(defn- decide-one
  "Decides one thing by the gate and by the statements. Takes the state of a
  play, the step, the thing and whether it is an earlier thing sent again.
  Gives the state after, with each statement that broke added to
  `:violations`.

  When the gate and the statements differ on the thing itself, whether it
  is taken, what its sender is told, or whether it was decided at all, the
  thing is taken out of the history for both: the record, the statements'
  state and the live reads stay as they were before it, as if it had not
  been sent. So the rest of the history is still judged, and one history
  can show more than one kind of break. A break that is not of the thing
  itself, a read that tells too much or two that disagree on a third,
  leaves the decision in place."
  [state op sent again?]
  (let [{:keys [record outcome]} (gate/decide (:rec state) :streaming sent)
        ruled (binding [reference/*act-named* (:named state)]
                (reference/judge (:ref state) lane sent))
        told (gate/told record sent)
        taken? (= :accepted (:verdict outcome))
        decided? (contains? outcome :stamp)
        answered? (or (:repeat? ruled) (:not-mine? ruled))
        line {:step (count (:steps state)) :op (:op op) :sent sent
              :outcome (dissoc outcome :writes :act) :told told
              :statements (select-keys ruled [:accept? :why :repeat? :not-mine? :answer])}
        [live-after live-broke] (when-not (:stray outcome) (live-step state record (:state ruled)))
        violation
        (cond
          (:stray outcome)
          {:statement :agree :class [:a-thing-of-a-history-was-a-stray (:stray outcome)]
           :what "a thing of a history was a stray"}

          answered?
          (let [which (if (:repeat? ruled) :the-same-thing-again :another-thing)]
            (cond
              (not (and (:repeat outcome) (= (comparable record) (comparable (:rec state)))))
              {:statement :once :class [:once which (covers-word sent)]
               :what (str (if (:repeat? ruled) "the same thing" "another thing")
                          " under an act-id that has an answer was decided, or wrote")}

              (not= (:answer (:answer ruled)) (:answer told))
              {:statement :told :class [:told which (covers-word sent) (:answer (:answer ruled)) (:answer told)]
               :what (str "the statements tell its sender " (pr-str (:answer ruled)) ", and the door tells her " (pr-str told))}))

          :else
          (or (outsiders-violation state sent outcome record ruled)
              (agree-violation state op sent outcome ruled)
              (when (not= (:answer (:answer ruled)) (:answer told))
                {:statement :told :class [:told :decided (covers-word sent) (:answer (:answer ruled)) (:answer told)]
                 :what (str "the statements tell its sender " (pr-str (:answer ruled)) ", and the door tells her " (pr-str told))})
              (when (and decided? (not= (:stamp outcome) (:decisions state)))
                {:statement :stamps :class [:stamps]
                 :what "a stamp is not the count of the lane's decisions"
                 :stamp (:stamp outcome) :expected (:decisions state)})
              (when decided? (row-violation (:facts (:rec state)) (:facts record) outcome))
              (see-violation (:facts record) (:state ruled))
              live-broke))
        broke (when violation
                (assoc violation
                       :step (:step line)
                       :at-a-yes? (at-a-yes? sent (:statement violation))
                       :ref-before (reference/summary (:ref state))))
        out? (and broke (or (#{:agree :told :once} (:statement broke))
                            (and (= :outsiders (:statement broke)) (:differ? broke))))]
    (count! :things)
    (when out? (count! :taken-out))
    (when decided?
      (count! :decisions)
      (if taken? (count! :taken) (count! [:refused (:verdict outcome)])))
    (when again? (count! :sent-again))
    (count-roads! state op sent outcome ruled)
    (when (and taken? (not= :make (:op op)))
      (count! [:deep (count (take-while some? (iterate #(nth % 3 nil) (:permission sent))))])
      (count! [:under (covers-word sent)]))
    (when (and (= (:who sent) (first (:permission sent)))
               (= :lane (covers-word sent))
               (reference/looks-intact? (:ref state) (:permission sent))
               (nil? (reference/standing-grant (:ref state) (:who sent) (:permission sent))))
      (count! :looked-intact-but-ended))
    (when taken?
      (doseq [{:keys [k v]} (:facts sent)
              :let [kind (reference/permission-keys k)]
              :when kind]
        (count! [(if (nil? v) :ends-taken :gives-taken) kind]))
      (when (some #(reference/ends-as-holder? (:ref state) sent %) (:facts sent))
        (count! :ends-by-the-holder-under-her-lanes)))
    (let [line (cond-> line broke (assoc :broke broke))]
      (if out?
        (-> state
            (update :steps conj (assoc line :taken-out? true))
            (update :violations conj broke))
        (-> state
            (assoc :rec record :ref (:state ruled) :live live-after)
            (cond-> decided? (update :decisions inc))
            (cond-> (and decided? (not (contains? (:first-under state) (:act-id sent))))
              (assoc-in [:first-under (:act-id sent)] {:who (:who sent) :verdict (:verdict outcome)}))
            (cond-> (and (not answered?) (permitted-act? sent))
              (update-in [:tries (yes-of (:permission sent))] (fnil conj []) (:verdict outcome)))
            (update :things conj sent)
            (cond-> taken? (update :pools with-gives sent))
            (update :steps conj line)
            (cond-> broke (update :violations conj broke)))))))

(defn play
  "Plays a history. Gives {:steps :violations :record}: each thing that was
  sent with what the gate, the door and the statements said, and each
  statement that broke, in order. A violation holds `:class`, the kind of
  break, `:step`, and `:at-a-yes?`. The last that can break is `run-again`,
  of the things that stayed in the history."
  [ops]
  (loop [state {:rec gate/empty-record :ref reference/start
                :pools {:lane {root [root-permission]} :run {} :act []}
                :named {} :first-under {} :tries {} :live []
                :things [] :steps [] :violations [] :picked [] :decisions 0 :n 0}
         todo (cons {:op :make} ops)]
    (if (empty? todo)
      (let [replayed (reduce (fn [rec sent] (:record (gate/decide rec :streaming sent)))
                             gate/empty-record
                             (:things state))]
        {:steps (:steps state)
         :record (:rec state)
         :picked (:picked state)
         :violations (cond-> (:violations state)
                       (not= (comparable replayed) (comparable (:rec state)))
                       (conj {:statement :run-again :class [:run-again] :step (count (:steps state))
                              :at-a-yes? false :what "the same things gave another record"}))})
      (let [op (first todo)
            ;; a pinned step keeps the numbers that it had (`tighter`)
            state (if-let [k (:k op)] (assoc state :n (* 2 k)) state)
            {:keys [sends entries named again? picked]} (things-of state op)
            state (update state :picked conj picked)
            state (cond-> state
                    (seq entries) (update-in [:pools :act] into entries)
                    (seq named) (update :named into named))
            state (reduce (fn [state sent] (decide-one state op sent again?)) state sends)
            state (if (= :make (:op op)) (open-live state) state)]
        ;; each step takes two numbers, so that a yes's act has one of its own
        (recur (update state :n + 2) (rest todo))))))

(defn- kinds-in
  "The kinds of break of a played history: a map from each kind to whether
  each of its breaks was at a yes for one act."
  [played]
  (reduce (fn [kinds v] (update kinds (:class v) #(and (if (nil? %) true %) (:at-a-yes? v))))
          {}
          (:violations played)))

(defn- aside-of
  "The rows of a kind that the walk does not know, in a played history's
  record, counted by kind."
  [played]
  (frequencies (map first (:aside (:record played)))))

(def holds
  "The property: no statement breaks in any history."
  (prop/for-all [ops gen-history]
    (empty? (:violations (play ops)))))

;; ------------------------------------------------------------ printing a play

(defn- yes-line
  "A yes's own id as a short mark: y and its number."
  [^UUID u]
  (str "y" (.getLeastSignificantBits u)))

(defn- act-id-line
  "An act-id as a short mark: its uuid's number, with the j-th more after a
  dot; #y and the number for the act-id whose uuid is a yes's id."
  [[_ _ ^UUID u]]
  (let [hi (.getMostSignificantBits u) lo (.getLeastSignificantBits u)]
    (cond (= yes-high hi) (str "#" (yes-line u))
          (zero? hi) (str "#" lo)
          (neg? hi) "#probe"
          :else (str "#" lo "." hi))))

(defn- chain-line
  "A permission id as a short line: the holders of its chain, the newest
  first, with what it allows and covers."
  [permission]
  (when permission
    (let [holders-of (loop [p permission out []]
                       (if p (recur (nth p 3 nil) (conj out (name (first p)))) out))
          [kind id] (reference/covers permission)]
      (str (apply str (interpose "<" holders-of))
           (when (<= 5 (count permission)) (str " (" (name (nth permission 4)) ")"))
           (case kind
             :run (str " for " (name id))
             :act (str " yes " (yes-line id))
             "")))))

(defn- fact-line [{:keys [e k v]}]
  (let [kind (reference/permission-keys k)]
    (cond
      (and kind (some? v)) (str "gives " (chain-line v))
      (= :lane kind) (str "ends " (name e) "'s")
      (= :run kind) (str "ends the one for " (name e))
      (= :act kind) (str "ends the yes " (yes-line e))
      (= "softland.store" (namespace k)) (str "sets " (name k))
      :else (str "writes " (name e) " " (name k)))))

(defn- print-trace
  "Prints a played history, one thing a line: the step, who sent it, in
  which run, under which act-id and permission, what it says; what the gate
  did, what the door tells its sender, and what the statements say. A step
  at which a statement broke is marked; a thing that was taken out of the
  history for it says so. Then, for the kind given, the first step at which
  it broke, what broke, and what stood before that step; for no kind, the
  first break."
  ([played] (print-trace played nil))
  ([{:keys [steps violations]} kind]
   (doseq [{:keys [step sent outcome told statements broke taken-out?]} steps]
     (println (format "  %2d  %-4s in %-5s %-6s under %-30s %-42s gate: %-28s door: %-12s statements: %s%s"
                      step
                      (name (:who sent))
                      (name (:session sent))
                      (act-id-line (:act-id sent))
                      (chain-line (:permission sent))
                      (str (apply str (interpose "; " (map fact-line (:facts sent))))
                           (when (seq? (:facts sent)) " (written another way)"))
                      (cond (:repeat outcome) "act-id answered: not decided"
                            (:stray outcome) (str "stray " (:stray outcome))
                            :else (name (:verdict outcome)))
                      (name (:answer told))
                      (cond (:repeat? statements) (str "the same thing again: " (name (:answer (:answer statements))))
                            (:not-mine? statements) "another thing under a used act-id: not mine"
                            (:accept? statements) "taken"
                            :else (name (:why statements)))
                      (cond (and broke taken-out?) (str "   <- " (name (:statement broke)) " broke; taken out of the history")
                            broke (str "   <- " (name (:statement broke)) " broke")
                            :else ""))))
   (when-let [v (if kind (first (filter #(= kind (:class %)) violations)) (first violations))]
     (println)
     (println "  BROKEN at step" (:step v) "-" (name (:statement v)) ":" (:what v))
     (when-let [before (:ref-before v)]
       (println "  what stood before that step:" before)))))

(defn- covers-phrase
  "What a permission covers, as words."
  [covers]
  (case covers :lane "a permission for the lane" :run "a permission for one run" :act "a yes for one act"
        (pr-str covers)))

(defn- class-line
  "A kind of break in words."
  [[what & more :as class]]
  (case what
    :gate-refused-what-the-statements-take
    (let [[covers word relation verdict] more
          whose (case relation
                  :its-holder "the yes's holder"
                  :its-giver-or-above "its giver or one above the giver"
                  :an-outsider "an outsider of the yes"
                  :the-same-who "the same who"
                  :another-who "another who"
                  (pr-str relation))]
      (str "agree: the gate does not take what the statements take, under " (covers-phrase covers)
           (if (= :act-id-answered word)
             (str ": its act-id has the answer of an earlier thing of " whose
                  (case verdict :after-an-accept ", which was taken" :after-a-refusal ", which was refused" ""))
             (str "; the gate's word is " (name word)
                  (when (= :act covers) (str "; the step: " (name (nth class 3 :none))))))))
    :outsiders
    (let [[where road held] more]
      (str "outsiders: a thing of an outsider of a yes "
           (case road :wrote-at-its-place "writes at the yes's place" :used-it-up "uses the yes up" (pr-str road))
           ", in " (case where :the-gate-alone "the gate alone" :the-gate-and-the-statements "the gate and in the statements"
                         :the-statements-alone "the statements alone" (pr-str where))
           "; the place held " (name held)))
    :gate-took-what-the-statements-refuse
    (str "agree: the gate takes what the statements refuse, under " (covers-phrase (first more))
         "; the statements' word is " (name (second more)))
    :told
    (let [[which covers says door] more]
      (str "told: " (case which :the-same-thing-again "the same thing sent again" :another-thing
                          "another thing under an act-id that has an answer" :decided "a thing that was decided")
           ", under " (covers-phrase covers) ": the statements tell its sender " (name says)
           " and the door tells her " (name door)))
    :once (str "once: under an act-id that has an answer, "
               (case (first more) :the-same-thing-again "the same thing sent again" "another thing")
               " was decided, or wrote, under " (covers-phrase (second more)))
    :unseen
    (str "unseen: an asker who may not see the lane, before a decision and after it, is told "
         (case (first more) :a-whole-answer "a whole answer again" "a changed place")
         " at that decision")
    :screen "screen: what a live read shows is not what a read asked once gives"
    :see (str "see: the read path says " (name (first more)) " where the statements say " (name (second more)))
    :stamps "stamps: a stamp is not the count of the lane's decisions"
    :kept (str "kept: " (name (first more)))
    :run-again "run-again: the same things, decided again, give another record"
    (pr-str class)))

;; --------------------------------------------------------------------- the run

(defn- counted-under
  "The entries of the tally whose key is a vector that begins with `word`,
  as a sorted map from the rest of the key to the count: one word, or a
  vector of the words after it."
  [counted word]
  (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
        (keep (fn [[k n]] (when (and (vector? k) (= word (first k)))
                            [(if (= 2 (count k)) (second k) (subvec k 1)) n])))
        counted))

(defn- sampled
  "The histories of a run, as rose trees: grown as test.check's quick-check
  grows them, from the seed, at the sizes 0 to 79 again and again, one
  split of the random number for each. Lazy."
  [histories seed]
  (letfn [(more [i sizes rnd]
            (lazy-seq
             (when (< i histories)
               (let [[r1 r2] (random/split rnd)]
                 (cons (gen/call-gen gen-history r1 (first sizes))
                       (more (inc i) (rest sizes) r2))))))]
    (more 0 (gen/make-size-range-seq 80) (random/make-random seed))))

(defn- kinds-of-break
  "Plays the histories of a run. Gives, for each kind of break that came
  up, {:count in how many histories, :at the first of them, :tree its rose
  tree, :at-a-yes? whether each of its breaks was at a yes for one act}.
  Counts the rows of a kind that the walk does not know, by kind, in the
  tally. Exits 2, with the history, when the model throws."
  [histories seed]
  (reduce
   (fn [found [i tree]]
     (let [played (try (play (rose/root tree))
                       (catch Throwable e
                         (println "THE MODEL THREW at history" (inc i) ":" (.getMessage e))
                         (println (pr-str (rose/root tree)))
                         (.printStackTrace e)
                         (System/exit 2)))]
       (doseq [[which n] (aside-of played)]
         (swap! tally update [:aside which] (fnil + 0) n))
       (reduce (fn [found [kind at-yes?]]
                 (-> found
                     (update-in [kind :count] (fnil inc 0))
                     (update-in [kind :at-a-yes?] #(if (nil? %) at-yes? (and % at-yes?)))
                     (update kind #(if (:tree %) % (assoc % :at (inc i) :tree tree)))))
               found
               (kinds-in played))))
   {}
   (map-indexed vector (sampled histories seed))))

(defn- shrunk
  "The smallest history under a rose tree that still breaks in one way, by
  test.check's own walk of the tree (`clojure.test.check/shrink-loop`): a
  smaller history that breaks so is searched further down; one that does
  not is passed over for the next beside it."
  [tree kind]
  (let [breaks? (fn [ops] (contains? (kinds-in (play ops)) kind))]
    (loop [nodes (rose/children tree) smallest (rose/root tree)]
      (if (empty? nodes)
        smallest
        (let [head (first nodes)]
          (if (breaks? (rose/root head))
            (if-let [children (seq (rose/children head))]
              (recur children (rose/root head))
              (recur (rest nodes) (rose/root head)))
            (recur (rest nodes) smallest)))))))

(defn- pinned
  "The steps of a history, each pinned to what it had in that history: its
  number (`:k`), from which its act-ids and yes ids are made, and the
  earlier yes or thing that it named (`:target`). The pinned history plays
  as the history does; a step taken out of it moves no other step's
  numbers and changes nothing that another step names, unless that step
  made it."
  [ops]
  (let [picked (:picked (play ops))]
    (vec (map-indexed (fn [i op]
                        (let [target (get picked (inc i))]
                          (cond-> (assoc op :k (inc i)) target (assoc :target target))))
                      ops))))

(defn- tighter
  "A history cut down further than test.check cuts it. A step names an
  earlier yes or thing by where it stands, and its act-ids are made of
  where it stands, so taking a step out moves the others, and test.check's
  cuts stop early. This pins each step (`pinned`), then takes out each step
  that the kind does not need, one at a time, until none can be taken out."
  [ops kind]
  (loop [ops (pinned ops)]
    (if-let [smaller (first (for [i (range (count ops))
                                  :let [without (into (subvec ops 0 i) (subvec ops (inc i)))]
                                  :when (contains? (kinds-in (play without)) kind)]
                              without))]
      (recur smaller)
      ops)))

(defn- smallest-of
  "The smallest history that breaks in one way: the first history of the
  run that broke so, cut down by test.check's walk of its rose tree
  (`shrunk`) and then by `tighter`. Takes the kind and the rose tree."
  [kind tree]
  (tighter (shrunk tree kind) kind))

(def ^:private mutants
  "The gate with one rule broken on purpose, each as [what, the var, a
  function that takes the function in place and gives the broken one]. A
  run that is worth anything finds a history that breaks a statement under
  each of them that the gate without it does not break there: `clojure
  -M:run mutants`. The store's code is not changed: the function is put in
  place for the run only, around the store's own function or the sketch's
  (`gate-histories.piece-2.sketch`), whichever is in place. The last two touch a
  yes for one act, and can be caught only by a gate that takes an act under
  a yes. The last assumes that a gate writes the use of a yes at the yes's
  place, as the row there is handed to `writes` for."
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
   ["S6: a holder whose permission stands is given another"
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

(defn- caught?
  "True when a history breaks a statement under a mutant that the real gate
  does not break: a break, by its step and its kind, that the play under the
  real gate does not have."
  [real mutated]
  (let [breaks (fn [played] (into #{} (map (juxt :step :class)) (:violations played)))]
    (boolean (seq (remove (breaks real) (breaks mutated))))))

(defn- first-new-break
  "The kind of the first break of a play under a mutant that the play under
  the real gate does not have."
  [real mutated]
  (let [known (into #{} (map (juxt :step :class)) (:violations real))]
    (:class (first (remove #(known [(:step %) (:class %)]) (:violations mutated))))))

(defn- run-mutants
  "Runs the histories against each mutant. Takes the count, the seed, and
  the functions in place under every play, a map for `with-redefs-fn`:
  empty for the store's own gate, the sketch's for `sketch`. Gives true
  when every mutant was caught. Prints the smallest history that caught
  each."
  [histories seed base]
  (reduce
   (fn [all-caught? [what the-var make-broken]]
     (let [in-place (get base the-var @the-var)
           broken (merge base {the-var (make-broken in-place)})
           real-play (fn [ops] (with-redefs-fn base #(play ops)))
           mutated-play (fn [ops] (with-redefs-fn broken #(play ops)))
           result (tc/quick-check histories
                                  (prop/for-all [ops gen-history]
                                    (not (caught? (real-play ops) (mutated-play ops))))
                                  :seed seed :max-size 80)]
       (println)
       (println "MUTANT" what)
       (if (:pass? result)
         (do (println "  NOT CAUGHT in" histories "histories") false)
         (let [smallest (first (get-in result [:shrunk :smallest]))
               real (real-play smallest)
               mutated (mutated-play smallest)]
           (println (format "  caught at history %d; the smallest history that catches it, under the mutant:"
                            (:num-tests result)))
           (print-trace mutated (first-new-break real mutated))
           all-caught?))))
   true
   mutants))

(defn- print-counts
  "Prints what the histories of a run held, from the tally."
  [counted]
  (println (format "%d things sent, %d of them an earlier thing again, %d taken out of their history for a break; %d decisions of the gate: %d taken, %d refused (every count below counts a thing that was taken out too)"
                   (:things counted 0) (:sent-again counted 0) (:taken-out counted 0)
                   (:decisions counted 0) (:taken counted 0)
                   (- (:decisions counted 0) (:taken counted 0))))
  (println "taken, by what the act's permission covered:" (counted-under counted :under))
  (println "gives taken, by what they cover:" (counted-under counted :gives-taken)
           " ends taken:" (counted-under counted :ends-taken))
  (println "a yes for one act, by the gate:" (counted-under counted :a-yes))
  (println "the act of a yes, sent by its holder under an act-id of hers, by what the yes's place held and the gate's word:")
  (doseq [[k n] (counted-under counted :the-act-under-its-yes)]
    (println (format "  %8d  %s" n (pr-str k))))
  (println "the act of a yes sent again under a new act-id after a refused try, by the gate's word:"
           (counted-under counted :sent-again-under-a-new-act-id-after-a-refused-try))
  (println "a thing under the act-id whose uuid is a yes's id, by the step that sends one, and the gate's word:"
           (counted-under counted :a-thing-under-a-yes-id))
  (println "a give at a yes's place again, by whom, what the place held, and the gate's word:")
  (doseq [[k n] (counted-under counted :a-give-at-a-yes-place-again)]
    (println (format "  %8d  %s" n (pr-str k))))
  (println "under an act-id that has an answer:" (counted-under counted :act-ids))
  (println (format "acts under a permission that looked intact by its ids and had ended: %d; ends by a holder under her permission for the lane: %d"
                   (:looked-intact-but-ended counted 0) (:ends-by-the-holder-under-her-lanes counted 0)))
  (println "taken, by how deep the act's permission was:" (counted-under counted :deep))
  (println "refused, by the gate's word:")
  (doseq [[word n] (sort-by (comp - val) (counted-under counted :refused))]
    (println (format "  %8d  %s" n (name word)))))

(defn- print-last-lines
  "The run's last lines: which kinds of break are at a yes for one act, and
  the rows of a kind that the walk does not know, kept aside, by kind."
  [found counted]
  (let [elsewhere (remove (comp :at-a-yes? val) found)]
    (println)
    (println (format "AT A YES FOR ONE ACT: %d of the %d kinds of break. Elsewhere: %d."
                     (- (count found) (count elsewhere)) (count found) (count elsewhere)))
    (doseq [[kind] (sort-by (comp pr-str key) elsewhere)]
      (println "  elsewhere:" (pr-str kind))))
  (let [aside (counted-under counted :aside)]
    (println "ROWS OF A KIND THAT THE WALK DOES NOT KNOW, kept aside, by kind:"
             (if (empty? aside) "none" aside))))

(defn -main
  "`clojure -M:piece-2 [histories [seed]]` runs so many histories under a seed,
  prints what they held and each kind of break with its smallest history,
  and exits 1 when a statement broke.
  `clojure -M:piece-2 mutants [histories [seed]]` runs them against the gate
  with one rule broken at a time, and exits 1 when a broken rule was not
  caught.
  `sketch` before either runs it against the sketch of a gate that takes
  the form of a yes for one act (`gate-histories.piece-2.sketch`), not against the
  store's own gate."
  [& args]
  (let [sketch? (= "sketch" (first args))
        args (if sketch? (rest args) args)
        mutants? (= "mutants" (first args))
        [histories seed] (if mutants? (rest args) args)
        histories (if histories (parse-long histories) 2000)
        seed (if seed (parse-long seed) (System/currentTimeMillis))
        base (if sketch? sketch/redefs {})]
    (reset! tally {})
    (when sketch?
      (println "UNDER THE SKETCH of a gate that takes the form of a yes for one act (gate-histories.piece-2.sketch), not the store's own gate"))
    (if mutants?
      (System/exit (if (run-mutants histories seed base) 0 1))
      (with-redefs-fn base
        (fn []
          (let [started (System/nanoTime)
                found (kinds-of-break histories seed)
                seconds (/ (- (System/nanoTime) started) 1e9)
                counted @tally]
            (println (format "%d histories, seed %d, %.1f seconds" histories seed seconds))
            (print-counts counted)
            (println)
            (if (empty? found)
              (do (println "HOLDS: agree, told, once, outsiders, stamps, kept, see, unseen, screen, run-again")
                  (print-last-lines found counted)
                  (System/exit 0))
              (let [in-order (sort-by (comp - :count val) found)]
                (println (format "BROKEN: %d kinds of break. In how many of the %d histories each came up, and the first:"
                                 (count found) histories))
                (doseq [[kind {:keys [count at at-a-yes?]}] in-order]
                  (println (format "  %6d  %6d  %s  %s" count at (if at-a-yes? "yes " "    ") (class-line kind))))
                (doseq [[kind {:keys [count at tree]}] in-order]
                  (println)
                  (println "KIND" (pr-str kind))
                  (println " " (class-line kind))
                  (println (format "  in %d histories; first at history %d. The smallest history that breaks this way:" count at))
                  (print-trace (play (smallest-of kind tree)) kind))
                (print-last-lines found counted)
                (System/exit 1)))))))))
