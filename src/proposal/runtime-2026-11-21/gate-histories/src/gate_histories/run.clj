(ns gate-histories.run
  "Generated histories of one layer, sent through the store's own door and
  gate code and held against the statements.

  A history is a list of things that makers send into one layer: gives,
  ends, gives after an end, plain facts, settings; a permission for a run
  and acts sent in it; a yes for one act, ended, given again by its giver,
  by one above the giver or by another, and the permitted act sent before
  the yes lands, while it is ended and after it is given again, as it was
  permitted, with a part changed, written another way, or under another
  act-id; the same thing again under its act-id; another thing under an
  act-id that was used, by the same who and by another; and a thing sent
  under the uuid that a yes for one act names, by a holder who may only
  see, by one who holds nothing, or by anyone. Some makers name a
  permission that is right, and some one that is stale, raised, or another
  holder's.

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
  judged by the statements (`gate-histories.reference`). These must hold
  for every history:

    agree      the gate takes a thing exactly when the statements take it
    taken      T1: an act that the statements take is taken by the gate,
               and the door tells its sender so; a repeat of an act that
               was taken is told so too
    outsiders  T2: nothing that is sent by a who other than the holder, the
               giver and those above the giver changes whether the holder's
               permitted act is taken. Held for each act that its holder
               sends under a permission for one act: the history before it
               is played again without the things of everyone else, through
               the gate and through the statements, and the act must come
               out the same
    once       the same thing sent again under its act-id decides nothing
               and keeps its first answer
    stamps     the decisions of the layer have the stamps 0, 1, 2 and on
    kept       a decision changes no row but its own: what was taken stays,
               and a refusal writes no fact
    see        after each decision, the read path lets an asker see the
               layer exactly when the statements do: each holder, one who
               was never given anything, each in no run and in each run,
               signed in and not
    unseen     an asker who keeps a read open on a layer that she may not
               see, before a decision and after it, is told nothing at that
               decision: not even an answer that says what it said, which
               would tell her that something of the layer was written
               (`softland.store.read-path.layers/meaning`)
    screen     what such an asker was shown, after each decision, is what a
               read asked once gives (`gate-histories.live`)
    run-again  the same things, decided again from nothing, give the same
               record

  Run it from this folder:

    clojure -M:run                  ; 2000 histories, a seed of its own
    clojure -M:run 20000 7          ; so many histories, this seed
    clojure -M:run mutants 3000 1   ; the gate with one rule broken at a time

  It prints what the histories held, then each kind of break that it found,
  how often, the history at which it first came up, and its smallest
  history step by step. It exits 1 when a statement broke."
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.random :as random]
            [clojure.test.check.rose-tree :as rose]
            [clojure.walk :as walk]
            [gate-histories.live :as live]
            [gate-histories.reference :as reference]
            [gate-histories.walk :as gate]
            [softland.store.act :as act]
            [softland.store.door.digest :as digest]
            [softland.store.gate.chain :as chain]
            [softland.store.gate.decision]
            [softland.store.read-path.layers :as layers]
            [softland.store.record.rows :as record])
  (:import [java.util UUID]))

;; ------------------------------------------------------------------ the world

(def layer "The one layer of a history." :lab)
(def holders "Who can be given a permission. The first makes the layer." [:asha :ben :cal :dee :eve])
(def senders "Who can send: the holders, and one who is never given anything." (conj holders :zed))
(def runs "The runs that a thing can be sent in." [:run-1 :run-2 :run-3])
(def root "The holder of the layer's first permission." (first holders))
(def root-permission "The layer's first permission." [root layer layer])
(def ^:private now "The time on each thing, as the door adds it. No rule reads it." 1700000000000)

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
  "The act-id of a history's thing with this number: the layer, its depot,
  and the number as the uuid."
  [n]
  [layer :streaming (UUID. 0 (long n))])

(defn- maker-act
  "An act as its maker hands it to the door: no act-id, no depot, no time."
  [who session permission facts]
  (cond-> {:version act/version :who who :layer layer :session session :facts (vec facts)}
    permission (assoc :permission permission)))

(defn- thing
  "A thing as it arrives on the streaming depot: the maker's act through the
  door's additions, under the act-id with that number."
  [n who session permission facts]
  (gate/through-the-door (maker-act who session permission facts) (act-id-of n) now))

(def ^:private making-maker
  "The act that makes the layer, as its maker hands it to the door."
  (maker-act root (first runs) root-permission
             [{:e layer :k :softland.store/depot :v :streaming}
              {:e root :k :softland.store/permission :v root-permission}]))

(def ^:private making-thing
  "The act that makes the layer, as it arrives."
  (gate/through-the-door making-maker (act-id-of 0) now))

(defn- from-the-end
  "The item of a vector that `n` names, counted from its last: 0 is the
  last. nil for an empty vector."
  [v n]
  (when (seq v) (nth v (- (count v) 1 (mod n (count v))))))

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
  layer; else an older one, her own with what it allows changed, another
  holder's, the permission of the run that she sends in, of another run, or
  of an act."
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
                                            [1 (gen/return :under-the-layers-key)]])))]
    [4 (gen/fmap (fn [run] {:f :end-run :run run}) gen-run)]
    [2 (gen/fmap (fn [n] {:f :end-act :n n}) gen/nat)]]))

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
    ;; layer, so that a giver is often not the first holder and one above
    ;; her can give the yes again under another id
    [6 (gen/fmap (fn [[[by giver] in choice for for-in fact then road]]
                   {:op :yes-for-one-act :by by :giver giver :in in :permission choice
                    :for for :for-in for-in :fact fact :then then :road road})
                 (gen/tuple (gen/tuple gen-holder (gen/elements [:as-chosen :one-who-was-given-one]))
                            gen-run
                            (gen/frequency [[6 (gen/return :current)] [1 (gen/return :run)]])
                            gen-holder gen-run gen-fact
                            (gen/elements [:nothing :send :changed :another-run :another-act
                                           :nothing :before :written-another-way :nothing])
                            (gen/frequency [[9 (gen/return :as-it-arrives)] [1 (gen/return :of-her-act)]])))]
    ;; a step on a yes that was made: its act sent, the yes ended, or given
    ;; again
    [16 (gen/fmap (fn [[n what by in]] {:op :on-an-act :n n :what what :by by :in in})
                  (gen/tuple gen-recent
                             (gen/elements [:send :send :send :end-by-giver :end-by-above :end-by-holder
                                            :again-by-giver :again-by-above :again-by-above
                                            :again-by-another :again-by-another])
                             gen-holder gen-run))]
    ;; a thing under the uuid that a yes for one act names: by a holder who
    ;; may only see, by one who holds nothing, or by anyone
    [5 (gen/fmap (fn [[n who by in fact]] {:op :under-its-act-id :n n :who who :by by :in in :fact fact})
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
  "The permission for the layer that a holder was last given, by the gives
  that the gate took; a guess beneath the first permission for a holder who
  was given none."
  [pools holder]
  (or (peek (get-in pools [:layer holder])) [holder layer layer root-permission]))

(defn- run-permission
  "The permission that a run was last given, by the gives that the gate
  took; a guess for a run that was given none, with `who` as its holder."
  [pools who run]
  (or (peek (get-in pools [:run run])) [who layer layer root-permission :see-and-act [:run run]]))

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
  (let [pool (get-in pools [:layer by])
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
           :v (cond-> [to layer layer (if beneath (current-permission pools beneath) permission)]
                allows (conj allows))}
    :give-run {:e (if (= :on-the-holder wrong) to run)
               :k (if (= :under-the-layers-key wrong) :softland.store/permission :softland.store/run-permission)
               :v [to layer layer permission allows [:run run]]}
    :end {:e of :k :softland.store/permission}
    :end-run {:e run :k :softland.store/run-permission}
    :end-act {:e (if-let [entry (from-the-end (:act pools) (:n fact))]
                   (nth (:act-id entry) 2)
                   (UUID. 9 9))
              :k :softland.store/act-permission}
    :plain {:e e :k k :v (str "said at " n)}
    :setting {:e layer
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
  "A yes for one act, made before anything is sent: the act that its holder
  will send, as she hands it to the door without its permission; its
  act-id; and the permission that the giver gives for it, which names the
  act by the text that its maker makes. Takes the pools, the number of the
  act's act-id, the giver's own permission, the holder, her run, the act's
  one fact, and how the maker makes the text:

    :as-it-arrives  of the act with its act-id and its depot, as the door is
                    about to send it, which is what
                    `softland.store.door.digest/hex-for-its-permission` asks
                    for
    :of-her-act     of the act that she hands the door, with no act-id and
                    no depot: the two things that she holds, her act and its
                    act-id, and no function of the door that makes the text
                    of the two

  Gives {:entry, for the pools: :permission :maker :act-id :road; :named,
  [[uuid text], the act as it would arrive], for the statements}."
  [pools n givers holder run fact road]
  (let [act-id (act-id-of n)
        unsigned (maker-act holder run nil [(fact-of pools n givers fact)])
        text (case road
               :as-it-arrives (digest/hex-for-its-permission (assoc unsigned :act-id act-id :depot (nth act-id 1)))
               :of-her-act (digest/hex-for-its-permission unsigned))
        pa [holder layer layer givers :see-and-act [:act (nth act-id 2) text]]]
    {:entry {:permission pa :maker unsigned :act-id act-id :road road}
     :named [[(nth act-id 2) text] (gate/through-the-door unsigned act-id now)]}))

(defn- permitted
  "The thing that the holder of a yes sends: the act that the yes was for,
  under the entry's permission, through the door under its act-id. `change`
  makes another act of it first."
  ([entry] (permitted entry identity))
  ([entry change]
   (gate/through-the-door (change (assoc (:maker entry) :permission (:permission entry))) (:act-id entry) now)))

(defn- written-another-way
  "The same act part for part, written as a maker may also write it: its
  facts as a list, and the two parts that may be left out given empty."
  [maker]
  (-> maker
      (assoc :subjects #{} :based-on {})
      (update :facts #(apply list %))))

(defn- one-who-was-given-one
  "A holder other than the first who was given a permission for the layer
  by a give that the gate took, the last of them in the order of the
  holders; `by` when there is none."
  [pools by]
  (or (last (filter #(and (not= root %) (seq (get-in pools [:layer %]))) holders)) by))

(defn- yes-for-one-act
  "The things of a yes for one act: the give of a permission for an act that
  is not sent yet, and what its holder sends with it. `n` and `n + 1` are
  the numbers of the two act-ids."
  [pools n {:keys [by giver in permission for for-in fact then road]}]
  (let [by (if (= :one-who-was-given-one giver) (one-who-was-given-one pools by) by)
        givers (named-permission pools by in permission)
        ;; a give inside the act cannot hold the act's own permission, which
        ;; names the act: so it is written beneath the giver's. A permission
        ;; for an act gives nothing, so the act is refused.
        {:keys [entry named]} (made-yes pools (inc n) givers for for-in fact road)
        the-give (thing n by in givers
                        [{:e (nth (:act-id entry) 2) :k :softland.store/act-permission :v (:permission entry)}])]
    {:entry entry
     :named named
     :sends (case then
              :nothing [the-give]
              :send [the-give (permitted entry)]
              :before [(permitted entry) the-give]
              :changed [the-give (permitted entry #(update % :facts conj {:e :thing-9 :k :note :v "added after the yes"}))]
              :another-run [the-give (permitted entry #(assoc % :session (first (remove #{for-in} runs))))]
              :another-act [the-give (gate/through-the-door (assoc (:maker entry) :permission (:permission entry))
                                                            [layer :streaming (UUID. 7 (long n))] now)]
              :written-another-way [the-give (permitted entry written-another-way)])}))

(defn- on-an-act
  "The thing of a step on a yes that was made: its act sent by its holder;
  an end of the permission at the act's place, by its giver, by the one
  above the giver, or by its holder under her permission for the layer; or
  a give of a permission for the same act again, by its giver, by the one
  above the giver, or by another holder, each under her own permission for
  the layer. A give again is another entry of the pools."
  [pools n {:keys [what by in]} entry]
  (let [pa (:permission entry)
        uuid (nth (:act-id entry) 2)
        giver (first (reference/above pa))
        above-giver (or (first (reference/above (reference/above pa))) giver)
        end-by (fn [who]
                 (let [own (current-permission pools who)]
                   (thing n who in own [{:e uuid :k :softland.store/act-permission}])))
        give-by (fn [who]
                  (let [own (current-permission pools who)
                        again (assoc pa 3 own)]
                    {:entry (assoc entry :permission again)
                     :sends [(thing n who in own [{:e uuid :k :softland.store/act-permission :v again}])]}))]
    (case what
      :send {:sends [(permitted entry)]}
      :end-by-giver {:sends [(end-by giver)]}
      :end-by-above {:sends [(end-by above-giver)]}
      :end-by-holder {:sends [(end-by (first pa))]}
      :again-by-giver (give-by giver)
      :again-by-above (give-by above-giver)
      :again-by-another (give-by by))))

(defn- pick
  "The earlier item that a step names: the one that `:target` holds, when
  the step was pinned to it and it is there (`tighter`); else the one that
  its number names from the end."
  [items step]
  (or (when-let [t (:target step)] (last (filter #(= t %) items)))
      (from-the-end items (:n step))))

(defn- things-of
  "What one step sends: {:sends the things in order}, and for a step that
  makes a yes, `:entry` for the pools and `:named` for the statements;
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
    :under-its-act-id (if-let [given (pick (or (:given-acts pools) (mapv :permission (:act pools))) step)]
                        (let [entry {:act-id [layer :streaming (second (nth given 5))] :permission given}
                              who (case (:who step)
                                    ;; a holder whose last permission for the layer says see
                                    :one-who-may-only-see
                                    (or (first (filter (fn [h] (= :see (nth (peek (get-in pools [:layer h])) 4 nil)))
                                                       (remove #{(first (:permission entry))} holders)))
                                        by)
                                    :one-who-holds-nothing :zed
                                    :anyone by)
                              own (current-permission pools who)]
                          {:picked given
                           :sends [(gate/through-the-door (maker-act who in own [(fact-of pools n own (:fact step))])
                                                          (:act-id entry) now)]})
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
  "The pools with the gives of a thing that the gate took, for the layer,
  for a run, and for one act. A yes for one act goes into the pools'
  `:act` when it is made, whether or not its give is taken; its permission
  goes into `:given-acts` when the gate takes the give."
  [pools sent]
  (reduce (fn [pools [kind permission]]
            (case kind
              :layer (update-in pools [:layer (first permission)] (fnil conj []) permission)
              :run (update-in pools [:run (second (nth permission 5))] (fnil conj []) permission)
              :act (update pools :given-acts (fnil conj []) permission)))
          pools
          (gives-of sent)))

(defn- covers-word
  "What a thing's permission covers, in a word: `:layer`, `:run` or `:act`."
  [sent]
  (first (reference/covers (:permission sent))))

(defn- permitted-act?
  "True for a thing that its holder sends under a permission for one act."
  [sent]
  (and (= :act (covers-word sent)) (= (:who sent) (first (:permission sent)))))

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
  "What the read path says of one asker and the layer, from the record: the
  layer's own rows, her permission row and the rows above it, and, for a
  read asked in a run, the row of the run's permission and the rows above
  it (`softland.store.read-path.layers`)."
  [facts who run signed-in?]
  (let [own-rows (into {}
                       (keep (fn [k] (let [place (record/place-key layer layer k)]
                                       (when-let [row (get facts place)] [place row]))))
                       [:softland.store/depot :softland.store/who-may-see
                        :softland.store/builds-on :softland.store/marked-keys])
        above-of (fn [row] (if-let [held (record/held-permission row)]
                             (rows-at facts (chain/places-above layer held))
                             {}))
        permission-row (get facts (record/place-key layer who :softland.store/permission))
        run-row (when run (get facts (record/place-key layer run :softland.store/run-permission)))]
    (layers/standing layer own-rows
                     (layers/either (layers/permission-state layer permission-row (above-of permission-row))
                                    (if run (layers/run-permission-state layer who run-row (above-of run-row)) :none))
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
  "The live reads of a history, opened when the layer is made."
  [state]
  (let [tables (live/tables (:rec state) :streaming)]
    (assoc state :live
           (mapv (fn [[who run]]
                   {:who who :run run
                    :read (live/open tables (live/check {:asks live-asks :asker who :session run :layers [layer]}))})
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
                   :what (str (name who) ", asking in " (name run) ", may not see the layer before and after, and was told "
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
  stands to the thing sent under it now: for a thing under a permission
  for one act, `:its-holder`, `:its-giver-or-above` or `:an-outsider`, by
  the permission's id; else `:the-same-who` or `:another-who`."
  [first-who sent]
  (let [pa (:permission sent)]
    (if (= :act (covers-word sent))
      (cond (= first-who (first pa)) :its-holder
            (contains? (reference/inside pa) first-who) :its-giver-or-above
            :else :an-outsider)
      (if (= first-who (:who sent)) :the-same-who :another-who))))

(defn- refused-road
  "Why a thing under a permission for one act, which the statements take,
  is refused by the gate's digest: the maker made the text of her own act,
  or the act was written another way. nil for any other thing."
  [state op sent]
  (when (= :act (covers-word sent))
    (cond
      (= :written-another-way (:then op)) :written-another-way
      (= :of-her-act (get (:roads state) (subvec (nth (:permission sent) 5) 1))) :its-text-made-of-her-act
      :else nil)))

(defn- agree-violation
  "The statement `agree` for a thing that the statements did not take as a
  repeat. Gives nil, or what broke, with its kind."
  [state op sent outcome ruled]
  (let [taken? (= :accepted (:verdict outcome))]
    (when (not= taken? (boolean (:accept? ruled)))
      (if taken?
        {:statement :agree
         :class [:gate-took-what-the-statements-refuse (covers-word sent) (:why ruled)]
         :what "the gate took what the statements refuse"}
        (let [first-under (get (:first-under state) (:act-id sent))
              act? (= :act (covers-word sent))]
          {:statement :agree
           :class (if (:repeat outcome)
                    (if act?
                      ;; under a permission for one act, the gate's word for the
                      ;; thing that took the act-id first, and whether the
                      ;; permission was given again
                      [:gate-refused-what-the-statements-take :act :act-id-answered
                       (relation (:who first-under) sent) (:verdict first-under)
                       (if (reference/given-again? (:ref state) (:permission sent)) :given-again :first-given)]
                      [:gate-refused-what-the-statements-take (covers-word sent) :act-id-answered
                       (relation (:who first-under) sent)
                       (if (= :accepted (:verdict first-under)) :after-an-accept :after-a-refusal)])
                    (cond-> [:gate-refused-what-the-statements-take (covers-word sent) (:verdict outcome)]
                      (refused-road state op sent) (conj (refused-road state op sent))))
           :what "the gate refused what the statements take"})))))

(defn- taken-in-replay
  "Whether a thing comes out taken when the things before it are sent as
  given, through the gate and through the statements: its answer, as the
  door tells it and as the statements give it, is \"accepted\". A thing
  before it on whose taking the two differ is taken out for both, as the
  play takes it out. Takes the things before it, the thing, and what each
  yes was for. Gives {:gate :statements}, each true or false."
  [before sent named]
  (binding [reference/*act-named* named]
    (let [[rec ref] (reduce (fn [[rec ref] t]
                              (let [{rec-after :record outcome :outcome} (gate/decide rec :streaming t)
                                    ruled (reference/judge ref layer t)]
                                (if (and (not (:repeat? ruled))
                                         (not= (= :accepted (:verdict outcome)) (boolean (:accept? ruled))))
                                  [rec ref]
                                  [rec-after (:state ruled)])))
                            [gate/empty-record reference/start]
                            before)
          rec (:record (gate/decide rec :streaming sent))]
      {:gate (= :accepted (:answer (gate/told rec sent)))
       :statements (= :accepted (:answer (:answer (reference/judge ref layer sent))))})))

(defn- outsiders-violation
  "T2 for an act that its holder sends under a permission for one act: the
  history before it is played again without the things of whoever is not
  inside the permission (`gate-histories.reference/inside`), and the act
  must come out as taken, or not, as it does with them. Takes the state
  before the act, the act, the record after it, and the statements'
  judgment of it. Gives nil, or what broke, with its kind: whether the gate
  alone, the statements too, or the statements alone let the others change
  it; and the road, the outsider's thing that the gate decided first under
  the act's act-id, with the gate's word for it, or a give at the act's
  place."
  [state sent record ruled]
  (let [inside (reference/inside (:permission sent))
        before (:things state)
        without (filterv #(contains? inside (:who %)) before)]
    (when (< (count without) (count before))
      (let [with-them {:gate (= :accepted (:answer (gate/told record sent)))
                       :statements (= :accepted (:answer (:answer ruled)))}
            without-them (taken-in-replay without sent (:named state))
            gate-differs? (not= (:gate with-them) (:gate without-them))
            statements-differ? (not= (:statements with-them) (:statements without-them))]
        (when (or gate-differs? statements-differ?)
          (let [uuid (nth (:act-id sent) 2)
                first-under (get (:first-under state) (:act-id sent))
                ref (:ref state)
                ;; who gave each permission that was ever taken at the act's place
                givers-there (keep (fn [n] (when (number? n)
                                             (first (reference/above (get-in ref [:grants n :permission])))))
                                   (get-in ref [:given [:act uuid]]))
                road (cond
                       (and first-under (not (contains? inside (:who first-under))))
                       [:under-its-act-id (:verdict first-under)]
                       (some #(not (contains? inside %)) givers-there)
                       [:a-give-at-its-place]
                       :else [:through-an-insiders-act])]
            {:statement :outsiders
             :differ? (not= (:gate with-them) (:statements with-them))
             :class (into [:outsiders-change-a-permitted-act
                           (cond (and gate-differs? statements-differ?) :the-gate-and-the-statements
                                 gate-differs? :the-gate-alone
                                 :else :the-statements-alone)]
                          road)
             :what (str "with the others' things the act is " (if (:gate with-them) "taken" "not taken")
                        " by the gate and " (if (:statements with-them) "taken" "not taken")
                        " by the statements; without them, " (if (:gate without-them) "taken" "not taken")
                        " and " (if (:statements without-them) "taken" "not taken"))}))))))

(defn- count-roads!
  "Counts the roads of a history that the review found unwalked, so that a
  run says how often it walked each."
  [state op sent outcome ruled]
  (let [first-under (get (:first-under state) (:act-id sent))]
    (when (:repeat? ruled) (count! [:roads :the-same-thing-again]))
    (when (and first-under (not (:repeat? ruled)))
      (count! [:roads (if (= (:who first-under) (:who sent))
                        :another-thing-under-a-used-act-id-by-the-same-who
                        :another-thing-under-a-used-act-id-by-another-who)]))
    (when (and (permitted-act? sent) (not (:repeat? ruled)))
      (let [at (reference/place-state (:ref state) (:permission sent))]
        (count! [:permitted-act-sent
                 (cond (= :nothing at) :before-its-permission-lands
                       (= :ended at) :while-it-is-ended
                       (and (= :stands at) (reference/given-again? (:ref state) (:permission sent))) :after-it-is-given-again
                       (= :stands at) :while-it-stands
                       :else at)])))
    (when (and (= :under-its-act-id (:op op)) (not (:repeat? ruled)))
      (count! [:under-an-acts-uuid-by (:verdict outcome :act-id-answered)]))
    (when (= :accepted (:verdict outcome))
      (doseq [{:keys [k v e]} (:facts sent)
              :when (= :softland.store/act-permission k)]
        (if (nil? v)
          (count! [:a-yes-for-one-act :ended])
          (let [was (remove #{:end} (get-in (:ref state) [:given [:act e]]))
                last-pa (when (seq was) (get-in (:ref state) [:grants (last was) :permission]))]
            (count! [:a-yes-for-one-act
                     (cond (nil? last-pa) :given
                           (= (reference/above v) (reference/above last-pa)) :given-again-by-its-giver
                           (contains? (reference/inside (reference/above last-pa)) (first (reference/above v)))
                           :given-again-by-one-above-the-giver
                           :else :given-again-by-another)])))))))

(defn- decide-one
  "Decides one thing by the gate and by the statements. Takes the state of a
  play, the step, the thing and whether it is an earlier thing sent again.
  Gives the state after, with each statement that broke added to
  `:violations`.

  When the gate and the statements differ on the thing itself, whether it
  is taken or what its sender is told, the thing is taken out of the
  history for both: the record, the statements' state and the live reads
  stay as they were before it, as if it had not been sent. So the rest of
  the history is still judged, and one history can show more than one kind
  of break. A break that is not of the thing itself, a read that tells too
  much or two that disagree on a third, leaves the decision in place."
  [state op sent again?]
  (let [{:keys [record outcome]} (gate/decide (:rec state) :streaming sent)
        ruled (binding [reference/*act-named* (:named state)]
                (reference/judge (:ref state) layer sent))
        told (gate/told record sent)
        taken? (= :accepted (:verdict outcome))
        decided? (contains? outcome :stamp)
        line {:step (count (:steps state)) :op (:op op) :sent sent
              :outcome (dissoc outcome :writes :act) :told told
              :statements (select-keys ruled [:accept? :why :repeat? :answer])}
        [live-after live-broke] (when-not (:stray outcome) (live-step state record (:state ruled)))
        violation
        (cond
          (:stray outcome)
          {:statement :agree :class [:a-thing-of-a-history-was-a-stray (:stray outcome)]
           :what "a thing of a history was a stray"}

          (:repeat? ruled)
          (cond
            (not (and (:repeat outcome) (= (comparable record) (comparable (:rec state)))))
            {:statement :once :class [:once :decided-again]
             :what "the same thing sent again was decided again, or wrote"}

            (and (= :accepted (:answer (:answer ruled))) (not= :accepted (:answer told)))
            {:statement :taken :class [:taken-and-not-told-so (covers-word sent) :the-same-thing-again (:answer told)]
             :what (str "the same thing sent again was taken the first time, and the door tells its sender " (pr-str told))})

          :else
          (or (when (permitted-act? sent) (outsiders-violation state sent record ruled))
              (agree-violation state op sent outcome ruled)
              (when (and (:accept? ruled) (not= :accepted (:answer told)))
                {:statement :taken :class [:taken-and-not-told-so (covers-word sent) (:answer told)]
                 :what (str "the act was taken, and the door tells its sender " (pr-str told))})
              (when (and decided? (not= (:stamp outcome) (:decisions state)))
                {:statement :stamps :class [:stamps]
                 :what "a stamp is not the count of the layer's decisions"
                 :stamp (:stamp outcome) :expected (:decisions state)})
              (when decided? (row-violation (:facts (:rec state)) (:facts record) outcome))
              (see-violation (:facts record) (:state ruled))
              live-broke))
        broke (when violation
                (assoc violation :step (:step line) :ref-before (reference/summary (:ref state))))
        out? (and broke (or (#{:agree :taken :once} (:statement broke))
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
               (= :layer (covers-word sent))
               (reference/looks-intact? (:ref state) (:permission sent))
               (nil? (reference/standing-grant (:ref state) (:who sent) (:permission sent))))
      (count! :looked-intact-but-ended))
    (when taken?
      (doseq [{:keys [k v]} (:facts sent)
              :let [kind (reference/permission-keys k)]
              :when kind]
        (count! [(if (nil? v) :ends-taken :gives-taken) kind]))
      (when (some #(reference/ends-as-holder? (:ref state) sent %) (:facts sent))
        (count! :ends-by-the-holder-under-her-layers)))
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
            (update :things conj sent)
            (cond-> taken? (update :pools with-gives sent))
            (update :steps conj line)
            (cond-> broke (update :violations conj broke)))))))

(defn play
  "Plays a history. Gives {:steps :violations :record}: each thing that was
  sent with what the gate, the door and the statements said, and each
  statement that broke, in order. A violation holds `:class`, the kind of
  break, and `:step`. The last that can break is `run-again`, of the things
  that stayed in the history."
  [ops]
  (loop [state {:rec gate/empty-record :ref reference/start
                :pools {:layer {root [root-permission]} :run {} :act []}
                :named {} :roads {} :first-under {} :live []
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
                              :what "the same things gave another record"}))})
      (let [op (first todo)
            ;; a pinned step keeps the numbers that it had (`tighter`)
            state (if-let [k (:k op)] (assoc state :n (* 2 k)) state)
            {:keys [sends entry named again? picked]} (things-of state op)
            state (update state :picked conj picked)
            state (cond-> state
                    entry (update-in [:pools :act] conj entry)
                    named (-> (assoc-in [:named (first named)] (second named))
                              (assoc-in [:roads (first named)] (:road entry))))
            state (reduce (fn [state sent] (decide-one state op sent again?)) state sends)
            state (if (= :make (:op op)) (open-live state) state)]
        ;; each step takes two numbers, so that a yes has one for its act
        (recur (update state :n + 2) (rest todo))))))

(defn- kinds-in
  "The kinds of break of a played history, as a set."
  [played]
  (into #{} (map :class) (:violations played)))

(def holds
  "The property: no statement breaks in any history."
  (prop/for-all [ops gen-history]
    (empty? (:violations (play ops)))))

;; ------------------------------------------------------------ printing a play

(defn- act-id-line
  "An act-id as a short mark: its uuid's number, with the high half when it
  has one."
  [[_ _ ^UUID u]]
  (let [hi (.getMostSignificantBits u) lo (.getLeastSignificantBits u)]
    (if (zero? hi) (str "#" lo) (str "#" hi ":" lo))))

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
             :act (str " for act #" (.getLeastSignificantBits ^UUID id))
             "")))))

(defn- fact-line [{:keys [e k v]}]
  (let [kind (reference/permission-keys k)]
    (cond
      (and kind (some? v)) (str "gives " (chain-line v))
      (= :layer kind) (str "ends " (name e) "'s")
      (= :run kind) (str "ends the one for " (name e))
      (= :act kind) (str "ends the one for act #" (.getLeastSignificantBits ^UUID e))
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
     (println (format "  %2d  %-4s in %-5s %-5s under %-30s %-42s gate: %-28s door: %-12s statements: %s%s"
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

(defn- class-line
  "A kind of break in words."
  [[what & more :as class]]
  (case what
    :gate-refused-what-the-statements-take
    (let [[covers word relation verdict again] more
          whose (case relation
                  :its-holder "the permission's holder"
                  :its-giver-or-above "its giver or one above the giver"
                  :an-outsider "one outside the permission"
                  :the-same-who "the same who"
                  :another-who "another who"
                  (pr-str relation))]
      (str "agree: the gate does not take what the statements take, under a permission for "
           (case covers :layer "the layer" :run "one run" :act "one act")
           (cond
             (and (= :act-id-answered word) (= :act covers))
             (str ": its act-id has the answer of an earlier thing of " whose
                  ", which the gate answered " (name verdict)
                  (case again :given-again "; the permission was given again after an end"
                              :first-given "; the permission is the first given there" ""))

             (= :act-id-answered word)
             (str ": its act-id has the answer of an earlier thing of " whose
                  (case verdict :after-an-accept ", which was taken" :after-a-refusal ", which was refused" ""))

             :else
             (str "; the gate's word is " (name word)
                  (case (first (drop 3 class))
                    :written-another-way ", for the act written another way"
                    :its-text-made-of-her-act ", for an act whose text its maker made of the act that she hands the door"
                    "")))))
    :outsiders-change-a-permitted-act
    (let [[who road word] more]
      (str "outsiders: a thing of one outside the permission changes whether the holder's permitted act is taken, in "
           (case who :the-gate-alone "the gate alone" :the-gate-and-the-statements "the gate and in the statements"
                     :the-statements-alone "the statements alone")
           "; "
           (case road
             :under-its-act-id (str "the outsider's thing took the act's act-id first, and the gate answered it " (name word))
             :a-give-at-its-place "the outsider gave a permission at the act's place first"
             :through-an-insiders-act "through an act of one inside the permission, which the outsider's thing made the gate take or refuse whole"
             "by another road")))
    :gate-took-what-the-statements-refuse
    (str "agree: the gate takes what the statements refuse, under a permission for " (name (first more))
         "; the statements' word is " (name (second more)))
    :taken-and-not-told-so
    (str "taken: the act is taken, and the door tells its sender " (pr-str (last more)))
    :unseen
    (str "unseen: an asker who may not see the layer, before a decision and after it, is told "
         (case (first more) :a-whole-answer "a whole answer again" "a changed place")
         " at that decision")
    :screen "screen: what a live read shows is not what a read asked once gives"
    :see (str "see: the read path says " (name (first more)) " where the statements say " (name (second more)))
    :once "once: the same thing sent again was decided again"
    :stamps "stamps: a stamp is not the count of the layer's decisions"
    :kept (str "kept: " (name (first more)))
    :run-again "run-again: the same things, decided again, give another record"
    (pr-str class)))

;; --------------------------------------------------------------------- the run

(defn- counted-under
  "The entries of the tally whose key is a vector that begins with `word`,
  as a sorted map from the rest of the key to the count."
  [counted word]
  (into (sorted-map)
        (keep (fn [[k n]] (when (and (vector? k) (= word (first k))) [(second k) n])))
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
  tree}. Exits 2, with the history, when the model throws."
  [histories seed]
  (reduce
   (fn [found [i tree]]
     (let [kinds (try (kinds-in (play (rose/root tree)))
                      (catch Throwable e
                        (println "THE MODEL THREW at history" (inc i) ":" (.getMessage e))
                        (println (pr-str (rose/root tree)))
                        (.printStackTrace e)
                        (System/exit 2)))]
       (reduce (fn [found kind]
                 (-> found
                     (update-in [kind :count] (fnil inc 0))
                     (update kind #(if (:tree %) % (assoc % :at (inc i) :tree tree)))))
               found
               kinds)))
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
  number (`:k`), from which its act-ids are made, and the earlier yes or
  thing that it named (`:target`). The pinned history plays as the history
  does; a step taken out of it moves no other step's act-ids and changes
  nothing that another step names, unless that step made it."
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
  "The gate with one rule broken on purpose, each as [what, the var, the
  broken function]. A run that is worth anything finds a history that
  breaks a statement under each of them that the real gate does not break
  there: `clojure -M:run mutants`. The store's code is not changed: the
  function is put in place for the run only."
  [["S3: a permission stands when its ids are in place, with no look at when each was given"
    #'softland.store.gate.chain/stands?
    (fn [layer permission rows]
      (loop [p permission]
        (cond (not (record/holds-value? (get rows (chain/place layer p)) p)) false
              (nil? (chain/above p)) true
              :else (recur (chain/above p)))))]
   ["S4, S5: whoever may act ends any permission, the first one too"
    #'softland.store.gate.decision/end-fault
    (fn [_act row] (when-not (record/held-permission row) :no-permission-to-end))]
   ["S6: a holder whose permission stands is given another"
    #'softland.store.gate.decision/give-over-fault
    (fn [_act _row _rows] nil)]
   ["S7: a permission is given beneath any permission"
    #'softland.store.gate.decision/permission-fact-fault
    (fn [_act _fact] nil)]
   ["S8: whoever may act writes a setting"
    #'softland.store.gate.decision/who-may-write-fault
    (fn [_act _fact] nil)]
   ["S9: a holder who may only see acts"
    #'softland.store.act/allows
    (fn [_permission] :see-and-act)]
   ["S11: a read lets a holder see after her permission ended"
    #'softland.store.read-path.layers/permission-state
    (fn [_layer permission-row _chain-rows] (if permission-row :stands :none))]
   ["S12: a read lets nobody but members see a layer that is open to everyone"
    #'softland.store.read-path.layers/who-may-see
    (fn [_own-rows _layer] :members)]
   ["R1, R9: an act is taken under a run's permission whatever run it is sent in"
    #'softland.store.gate.decision/standing
    (let [standing @#'softland.store.gate.decision/standing]
      (fn [gate-depot the-act permission-row depot-row chain-rows hex]
        (let [[kind run] (act/covers (:permission the-act))]
          (standing gate-depot
                    (if (= :run kind) (assoc the-act :session run) the-act)
                    permission-row depot-row chain-rows hex))))]
   ["R2: any act is taken under an act's permission, whatever it says"
    #'softland.store.gate.decision/hex-for-its-permission
    (fn [_thing the-act]
      (let [[kind _uuid digest] (act/covers (:permission the-act))]
        (when (= :act kind) digest)))]
   ["R8: a read lets an asker see under any run's permission, whoever holds it"
    #'softland.store.read-path.layers/run-permission-state
    (let [state @#'softland.store.read-path.layers/run-permission-state]
      (fn [layer _asker run-row chain-rows]
        (state layer (first (record/held-permission run-row)) run-row chain-rows)))]])

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
  "Runs the histories against each mutant. Gives true when every mutant
  was caught. Prints the smallest history that caught each."
  [histories seed]
  (reduce
   (fn [all-caught? [what the-var broken]]
     (let [result (tc/quick-check histories
                                  (prop/for-all [ops gen-history]
                                    (not (caught? (play ops) (with-redefs-fn {the-var broken} #(play ops)))))
                                  :seed seed :max-size 80)]
       (println)
       (println "MUTANT" what)
       (if (:pass? result)
         (do (println "  NOT CAUGHT in" histories "histories") false)
         (let [smallest (first (get-in result [:shrunk :smallest]))
               real (play smallest)
               mutated (with-redefs-fn {the-var broken} #(play smallest))]
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
  (println "a yes for one act, taken:" (counted-under counted :a-yes-for-one-act))
  (println "the permitted act sent by its holder:" (counted-under counted :permitted-act-sent))
  (println "a thing under the uuid of a yes for one act, by another, and the gate's word for it:"
           (counted-under counted :under-an-acts-uuid-by))
  (println "act-ids sent again:" (counted-under counted :roads))
  (println (format "acts under a permission that looked intact by its ids and had ended: %d; ends by a holder under her permission for the layer: %d"
                   (:looked-intact-but-ended counted 0) (:ends-by-the-holder-under-her-layers counted 0)))
  (println "taken, by how deep the act's permission was:" (counted-under counted :deep))
  (println "refused, by the gate's word:")
  (doseq [[word n] (sort-by (comp - val) (counted-under counted :refused))]
    (println (format "  %8d  %s" n (name word)))))

(defn -main
  "`clojure -M:run [histories [seed]]` runs so many histories under a seed,
  prints what they held and each kind of break with its smallest history,
  and exits 1 when a statement broke.
  `clojure -M:run mutants [histories [seed]]` runs them against the gate
  with one rule broken at a time, and exits 1 when a broken rule was not
  caught."
  [& args]
  (let [mutants? (= "mutants" (first args))
        [histories seed] (if mutants? (rest args) args)
        histories (if histories (parse-long histories) 2000)
        seed (if seed (parse-long seed) (System/currentTimeMillis))]
    (reset! tally {})
    (if mutants?
      (System/exit (if (run-mutants histories seed) 0 1))
      (let [started (System/nanoTime)
            found (kinds-of-break histories seed)
            seconds (/ (- (System/nanoTime) started) 1e9)
            counted @tally]
        (println (format "%d histories, seed %d, %.1f seconds" histories seed seconds))
        (print-counts counted)
        (println)
        (if (empty? found)
          (do (println "HOLDS: agree, taken, outsiders, once, stamps, kept, see, unseen, screen, run-again")
              (System/exit 0))
          (let [in-order (sort-by (comp - :count val) found)]
            (println (format "BROKEN: %d kinds of break. In how many of the %d histories each came up, and the first:"
                             (count found) histories))
            (doseq [[kind {:keys [count at]}] in-order]
              (println (format "  %6d  %6d  %s" count at (class-line kind))))
            (doseq [[kind {:keys [count at tree]}] in-order]
              (println)
              (println "KIND" (pr-str kind))
              (println " " (class-line kind))
              (println (format "  in %d histories; first at history %d. The smallest history that breaks this way:" count at))
              (print-trace (play (smallest-of kind tree)) kind))
            (System/exit 1)))))))
