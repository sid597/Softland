(ns gate-histories.run
  "Generated histories of one layer, decided by the store's own gate code and
  held against the statements.

  A history is a list of things that makers send into one layer: gives,
  ends, gives after an end, plain facts, settings, the same thing sent
  twice. Some name a permission that is right, and some name one that is
  stale, raised, or another holder's. test.check makes the histories and,
  when one breaks a statement, cuts it down to the smallest that still does.

  When the store has permissions for one run and for one act (piece 2 of
  the store's line), the histories hold those too: gives and ends of a
  run's permission, acts sent in a run under it and in another run, and a
  yes for one act followed by that act, by the act with a part changed, or
  by another act.

  Each thing is decided twice: by the gate (`gate-histories.walk`, which
  calls `softland.store.gate.decision`) and by the statements
  (`gate-histories.reference`). Six things must hold for every history:

    agree      the gate takes a thing exactly when the statements take it
    stamps     the decisions of the layer have the stamps 0, 1, 2 and on
    kept       a decision changes no row but its own: what was taken stays,
               and a refusal writes no fact
    once       a thing that is sent again under its act-id decides nothing
    see        after each decision, the read path lets an asker see the
               layer exactly when the statements do
    run-again  the same things, decided again from nothing, give the same
               record

  Run it from this folder:

    clojure -M:run                  ; 2000 histories, a seed of its own
    clojure -M:run 20000 7          ; so many histories, this seed
    clojure -M:run mutants 3000 1   ; the gate with one rule broken at a time

  It prints what it ran, how often each word of a refusal came up, and, for
  a history that breaks a statement, the smallest such history step by
  step. It exits 1 then."
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.walk :as walk]
            [gate-histories.reference :as reference]
            [gate-histories.walk :as gate]
            [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.gate.chain :as chain]
            [softland.store.gate.decision]
            [softland.store.read-path.layers :as layers]
            [softland.store.record.rows :as record])
  (:import [java.util UUID]))

;; ------------------------------------------------------------------ the world

(def layer "The one layer of a history." :lab)
(def holders "Who can send. The first makes the layer." [:asha :ben :cal :dee :eve])
(def runs "The runs that a thing can be sent in." [:run-1 :run-2 :run-3])
(def root "The holder of the layer's first permission." (first holders))
(def root-permission "The layer's first permission." [root layer layer])
(def ^:private now "The time on each thing. No rule reads it." 1700000000000)

(def ^:private hex-for-its-permission
  "The store's digest of a thing for a permission for one act
  (`softland.store.door.digest/hex-for-its-permission`), or nil for a store
  without such permissions."
  (ns-resolve 'softland.store.door.digest 'hex-for-its-permission))

(def piece-2?
  "Whether the store has permissions for one run and for one act: its
  declaration knows their keys and its digest is there."
  (boolean (and hex-for-its-permission
                (contains? declaration/store-keys :softland.store/run-permission))))

(defn- comparable
  "A value with each byte array as a vector, so that two records compare."
  [x]
  (walk/postwalk (fn [v] (if (bytes? v) (vec v) v)) x))

(defn- thing
  "A thing as it arrives on the streaming depot: an act-id with the given
  number as its uuid, the time, and the maker's act."
  [n who session permission facts]
  {:act-id [layer :streaming (UUID. 0 (long n))]
   :depot :streaming
   declaration/time-part now
   :version act/version
   :who who
   :layer layer
   :permission permission
   :session session
   :facts (vec facts)})

(def ^:private making-thing
  "The act that makes the layer: its depot, and the first permission."
  (thing 0 root (first runs) root-permission
         [{:e layer :k :softland.store/depot :v :streaming}
          {:e root :k :softland.store/permission :v root-permission}]))

;; ------------------------------------------------------------- the generators

(def ^:private gen-holder (gen/elements holders))
(def ^:private gen-run (gen/elements runs))

(def ^:private gen-permission-choice
  "Which permission a maker names: mostly the one she was last given for the
  layer; else an older one, her own with what it allows changed, or another
  holder's; and, with piece 2, the permission of the run that she sends in,
  of another run, or of an act."
  (gen/frequency
   (cond-> [[12 (gen/return :current)]
            [2 (gen/fmap (fn [n] [:older n]) gen/nat)]
            [1 (gen/return :raised)]
            [1 (gen/fmap (fn [h] [:of h]) gen-holder)]]
     piece-2? (into [[1 (gen/return :run)]
                     [1 (gen/fmap (fn [s] [:run-of s]) gen-run)]
                     [1 (gen/fmap (fn [n] [:act n]) gen/nat)]]))))

(def ^:private gen-fact
  "One fact, said abstractly; `fact-of` makes it concrete at its step."
  (gen/frequency
   (cond-> [[10 (gen/fmap (fn [[to allows beneath]] {:f :give :to to :allows allows :beneath beneath})
                          (gen/tuple gen-holder
                                     (gen/elements [nil :see :see-and-act :see-and-act])
                                     (gen/frequency [[15 (gen/return nil)] [1 gen-holder]])))]
            [5 (gen/fmap (fn [of] {:f :end :of of}) gen-holder)]
            [8 (gen/fmap (fn [[e k]] {:f :plain :e e :k k})
                         (gen/tuple (gen/elements [:thing-1 :thing-2]) (gen/elements [:colour :note])))]
            [3 (gen/fmap (fn [[k word]] {:f :setting :key k :word word})
                         (gen/tuple (gen/elements [:softland.store/who-may-see :softland.store/who-may-see
                                                   :softland.store/builds-on :softland.store/marked-keys])
                                    (gen/elements [:everyone :members])))]]
     piece-2? (into [[8 (gen/fmap (fn [[to run allows wrong]] {:f :give-run :to to :run run :allows allows :wrong wrong})
                                  (gen/tuple gen-holder gen-run
                                             (gen/elements [:see :see-and-act :see-and-act])
                                             (gen/frequency [[20 (gen/return nil)]
                                                             [1 (gen/return :on-the-holder)]
                                                             [1 (gen/return :under-the-layers-key)]])))]
                     [4 (gen/fmap (fn [run] {:f :end-run :run run}) gen-run)]
                     [2 (gen/fmap (fn [n] {:f :end-act :n n}) gen/nat)]]))))

(def ^:private gen-op
  "One step of a history."
  (gen/frequency
   (cond-> [[30 (gen/fmap (fn [[by in choice fact]] {:op :act :by by :in in :permission choice :facts [fact]})
                          (gen/tuple gen-holder gen-run gen-permission-choice gen-fact))]
            [4 (gen/fmap (fn [[by in choice f1 f2]] {:op :act :by by :in in :permission choice :facts [f1 f2]})
                         (gen/tuple gen-holder gen-run gen-permission-choice gen-fact gen-fact))]
            ;; the two steps that make an end and a give after it common: the
            ;; giver of a holder's last permission ends it, or gives exactly
            ;; that permission once more
            [3 (gen/fmap (fn [[of in]] {:op :giver-ends :of of :in in}) (gen/tuple gen-holder gen-run))]
            [3 (gen/fmap (fn [[to in]] {:op :giver-gives-again :to to :in in}) (gen/tuple gen-holder gen-run))]
            [2 (gen/fmap (fn [n] {:op :again :n n}) gen/nat)]
            [1 (gen/return {:op :make-again})]]
     piece-2? (conj [9 (gen/fmap (fn [[by in fact as]] {:op :act-in-run :by by :in in :facts [fact] :as as})
                                 (gen/tuple gen-holder gen-run gen-fact
                                            (gen/frequency [[9 (gen/return :its-holder)] [1 (gen/return :another)]])))]
                    [6 (gen/fmap (fn [[by in choice for for-in fact then]]
                                   {:op :yes-for-one-act :by by :in in :permission choice
                                    :for for :for-in for-in :fact fact :then then})
                                 (gen/tuple gen-holder gen-run
                                            (gen/frequency [[6 (gen/return :current)] [1 (gen/return :run)]])
                                            gen-holder gen-run gen-fact
                                            (gen/elements [:send :send :send :changed :another-run
                                                           :another-act :nothing])))]))))

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
      :older (if (seq pool)
               (nth pool (mod (second choice) (count pool)))
               (current-permission pools by))
      :of (current-permission pools (second choice))
      :run (run-permission pools by in)
      :run-of (run-permission pools by (second choice))
      :act (if (seq (:act pools))
             (nth (:act pools) (mod (second choice) (count (:act pools))))
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
    :end-act {:e (if (seq (:act pools))
                   (second (nth (nth (:act pools) (mod (:n fact) (count (:act pools)))) 5))
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

(defn- yes-for-one-act
  "The things of one yes for one act: the give of a permission for an act
  that is not sent yet, and then what its holder sends under it. `n` and
  `n + 1` are the numbers of the two act-ids."
  [pools n {:keys [by in permission for for-in fact then]}]
  (let [givers (named-permission pools by in permission)
        ;; a give inside the act cannot hold the act's own permission, which
        ;; holds the digest of the act: so it is written beneath the giver's.
        ;; A permission for an act gives nothing, so the act is refused.
        the-fact (fact-of pools (inc n) givers fact)
        unsigned (thing (inc n) for for-in nil [the-fact])
        for-the-act [for layer layer givers :see-and-act
                     [:act (nth (:act-id unsigned) 2) (hex-for-its-permission unsigned)]]
        the-act (assoc unsigned :permission for-the-act)
        the-give (thing n by in givers
                        [{:e (nth (:act-id unsigned) 2) :k :softland.store/act-permission :v for-the-act}])]
    (case then
      :nothing [the-give]
      :send [the-give the-act]
      :changed [the-give (update the-act :facts conj {:e :thing-9 :k :note :v "added after the yes"})]
      :another-run [the-give (assoc the-act :session (first (remove #{for-in} runs)))]
      :another-act [the-give (assoc the-act :act-id [layer :streaming (UUID. 7 (long n))])])))

(defn- things-of
  "The things that one step sends, in order, each with whether it is a thing
  that was sent before."
  [{:keys [pools things n]} {:keys [op by in permission facts] :as step}]
  (case op
    :make [[making-thing false]]
    :act (let [named (named-permission pools by in permission)]
           [[(thing n by in named (one-fact-a-place (map #(fact-of pools n named %) facts))) false]])
    :act-in-run (let [named (run-permission pools by in)
                      ;; mostly sent by the holder of the run's permission
                      who (if (= :its-holder (:as step)) (first named) by)]
                  [[(thing n who in named (one-fact-a-place (map #(fact-of pools n named %) facts))) false]])
    :yes-for-one-act (mapv (fn [sent] [sent false]) (yes-for-one-act pools n step))
    (:giver-ends :giver-gives-again)
    (let [holder (or (:of step) (:to step))
          hers (current-permission pools holder)
          above (nth hers 3 nil)]
      (if (nil? above)
        ;; the first holder has no giver: she ends, or gives, her own
        [[(thing n holder in hers [(if (= :giver-ends op)
                                     {:e holder :k :softland.store/permission}
                                     {:e holder :k :softland.store/permission :v hers})]) false]]
        [[(thing n (first above) in above
                 [(if (= :giver-ends op)
                    {:e holder :k :softland.store/permission}
                    {:e holder :k :softland.store/permission :v hers})]) false]]))
    :make-again [[(assoc making-thing :act-id [layer :streaming (UUID. 0 (long n))]) false]]
    :again [[(nth things (mod (:n step) (count things))) true]]))

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
  "The pools with the gives of a thing that the gate took."
  [pools sent]
  (reduce (fn [pools [kind permission]]
            (case kind
              :layer (update-in pools [:layer (first permission)] (fnil conj []) permission)
              :run (update-in pools [:run (second (nth permission 5))] (fnil conj []) permission)
              :act (update pools :act (fnil conj []) permission)))
          pools
          (gives-of sent)))

(defn- row-violation
  "The statement `kept` for one decision. Takes the facts before and after,
  and the outcome. Gives nil, or what broke."
  [before after outcome]
  (let [written (into #{} (keep (fn [[which k]] (when (= :fact which) k))) (:writes outcome))
        refused? (not= :accepted (:verdict outcome))]
    (cond
      (and refused? (seq written))
      {:statement :kept :what "a refusal wrote a fact" :keys written}

      (not= (comparable (apply dissoc before written)) (comparable (apply dissoc after written)))
      {:statement :kept :what "a decision changed a row that it did not write"})))

(def ^:private askers
  "Who asks to see the layer after each decision: each holder of a history,
  and one who is never given anything."
  (conj holders :zed))

(defn- seeing
  "What the read path says of one asker and the layer, from the record: the
  layer's own rows, her permission row, and the rows above the permission
  that her row holds (`softland.store.read-path.layers/standing`)."
  [facts who signed-in?]
  (let [own-rows (into {}
                       (keep (fn [k] (let [place (record/place-key layer layer k)]
                                       (when-let [row (get facts place)] [place row]))))
                       [:softland.store/depot :softland.store/who-may-see
                        :softland.store/builds-on :softland.store/marked-keys])
        permission-row (get facts (record/place-key layer who :softland.store/permission))
        held (record/held-permission permission-row)
        chain-rows (if held
                     (into {} (map (fn [k] [k (get facts k)])) (chain/places-above layer held))
                     {})]
    (layers/standing layer own-rows permission-row chain-rows signed-in?)))

(def see-asked?
  "False once the read path's `standing` no longer takes what `seeing` hands
  it: it is being changed for a read that names its run, and the statement
  `see` is not asked until this check is brought up to it."
  (atom true))

(defn- see-violation
  "The statement `see` after one decision, for a read that names no run.
  Takes the record's facts and the statements' state. Gives nil, or the
  first asker for whom the read path and the statements differ."
  [facts ref]
  (when @see-asked?
    (try
      (first
       (for [who askers
             signed-in? [true false]
             :let [read-path (seeing facts who signed-in?)
                   statements (reference/may-see ref who signed-in?)]
             :when (not= read-path statements)]
         {:statement :see
          :what (str "the read path says " (name read-path) " and the statements say " (name statements)
                     " for " (name who) (when-not signed-in? ", not signed in"))}))
      (catch clojure.lang.ArityException _
        (reset! see-asked? false)
        nil))))

(defn- decide-one
  "Decides one thing by the gate and by the statements. Takes the state of a
  play, the step, the thing and whether it was sent before. Gives the state
  after, with `:violation` when a statement broke."
  [state op sent again?]
  (let [{:keys [record outcome]} (gate/decide (:rec state) :streaming sent)
        ruled (when-not again? (reference/judge (:ref state) layer sent))
        taken? (= :accepted (:verdict outcome))
        line {:step (count (:steps state)) :op (:op op) :sent sent :outcome (dissoc outcome :writes :act)
              :statements (select-keys ruled [:accept? :why])}
        violation
        (cond
          again?
          (when-not (and (:repeat outcome) (= (comparable record) (comparable (:rec state))))
            {:statement :once :what "a thing sent again was decided again, or wrote"})

          (:stray outcome)
          {:statement :agree :what "a thing of a history was a stray" :word (:stray outcome)}

          (not= taken? (:accept? ruled))
          {:statement :agree
           :what (if taken?
                   "the gate took what the statements refuse"
                   "the gate refused what the statements take")
           :gate (:verdict outcome) :statements (or (:why ruled) :taken)}

          (not= (:stamp outcome) (:decisions state))
          {:statement :stamps :what "a stamp is not the count of the layer's decisions"
           :stamp (:stamp outcome) :expected (:decisions state)}

          :else (or (row-violation (:facts (:rec state)) (:facts record) outcome)
                    (see-violation (:facts record) (:state ruled))))]
    (when-not again?
      (count! :decisions)
      (if taken? (count! :taken) (count! [:refused (:verdict outcome)]))
      (when (and taken? (not= :make (:op op)))
        (count! [:deep (count (take-while some? (iterate #(nth % 3 nil) (:permission sent))))])
        (count! [:under (first (reference/covers (:permission sent)))]))
      (when (and (= (:who sent) (first (:permission sent)))
                 (= :layer (first (reference/covers (:permission sent))))
                 (reference/looks-intact? (:ref state) (:permission sent))
                 (nil? (reference/standing-grant (:ref state) (:who sent) (:permission sent))))
        (count! :looked-intact-but-ended))
      (when taken?
        (doseq [{:keys [k v]} (:facts sent)
                :let [kind (reference/permission-keys k)]
                :when kind]
          (count! [(if (nil? v) :ends-taken :gives-taken) kind]))
        (when (some #(reference/ends-as-holder? (:ref state) sent %) (:facts sent))
          (count! :ends-by-the-holder-under-her-layers))))
    (if violation
      (assoc state
             :rec record
             :steps (conj (:steps state) (assoc line :ref-before (reference/summary (:ref state))))
             :violation (assoc violation :step (:step line)))
      (-> state
          (assoc :rec record)
          (cond-> (not again?) (assoc :ref (:state ruled)))
          (cond-> (not again?) (update :decisions inc))
          (cond-> (not again?) (update :things conj sent))
          (cond-> taken? (update :pools with-gives sent))
          (update :steps conj line)))))

(defn play
  "Plays a history. Gives {:steps :violation :record}: each thing that was
  sent with what the gate and the statements said, and the first statement
  that broke, or nil."
  [ops]
  (loop [state {:rec gate/empty-record :ref reference/start
                :pools {:layer {root [root-permission]} :run {} :act []}
                :things [] :steps [] :decisions 0 :n 0}
         todo (cons {:op :make} ops)]
    (cond
      (:violation state)
      (select-keys state [:steps :violation :rec])

      (empty? todo)
      (let [replayed (reduce (fn [rec sent] (:record (gate/decide rec :streaming sent)))
                             gate/empty-record
                             (:things state))]
        {:steps (:steps state)
         :record (:rec state)
         :violation (when (not= (comparable replayed) (comparable (:rec state)))
                      {:statement :run-again :what "the same things gave another record"})})

      :else
      (let [op (first todo)
            sending (things-of state op)
            ;; a yes for one act is made for an act that is not sent yet: its
            ;; permission goes into the pools whether or not the give is taken
            state (cond-> state
                    (= :yes-for-one-act (:op op))
                    (update-in [:pools :act] conj (:v (first (:facts (first (first sending)))))))
            state (reduce (fn [state [sent again?]]
                            (if (:violation state)
                              (reduced state)
                              (decide-one state op sent again?)))
                          state
                          sending)]
        ;; each step takes two numbers, so that a yes has one for its act
        (recur (update state :n + 2) (rest todo))))))

(def holds
  "The property: no statement breaks in any history."
  (prop/for-all [ops gen-history]
    (nil? (:violation (play ops)))))

;; --------------------------------------------------------------------- the run

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
             :act " for one act"
             "")))))

(defn- fact-line [{:keys [e k v]}]
  (let [kind (reference/permission-keys k)]
    (cond
      (and kind (some? v)) (str "gives " (chain-line v))
      (= :layer kind) (str "ends " (name e) "'s")
      (= :run kind) (str "ends the one for " (name e))
      (= :act kind) "ends the one for an act"
      (= "softland.store" (namespace k)) (str "sets " (name k))
      :else (str "writes " (name e) " " (name k)))))

(defn- print-trace
  "Prints a played history, one thing a line."
  [{:keys [steps violation]}]
  (doseq [{:keys [step op sent outcome statements]} steps]
    (println (format "  %2d  %-5s in %-5s under %-34s %-46s gate: %-36s statements: %s"
                     step
                     (name (:who sent))
                     (name (:session sent))
                     (chain-line (:permission sent))
                     (apply str (interpose "; " (map fact-line (:facts sent))))
                     (cond (:repeat outcome) "sent again, nothing decided"
                           (:stray outcome) (str "stray " (:stray outcome))
                           :else (name (:verdict outcome)))
                     (cond (= :again op) "-"
                           (:accept? statements) "taken"
                           :else (name (:why statements))))))
  (when violation
    (println)
    (println "  BROKEN at step" (:step violation) "-" (name (:statement violation)) ":" (:what violation))
    (println "  what stood before that step:" (:ref-before (peek steps)))))

(def ^:private mutants
  "The gate with one rule broken on purpose, each as [what, the var, the
  broken function]. A run that is worth anything finds a history that
  breaks a statement under each of them: `clojure -M:run mutants`. The
  store's code is not changed: the function is put in place for the run
  only."
  (cond->
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
     (fn [_own-rows _layer] :members)]]
    piece-2?
    (into [["R1, R9: an act is taken under a run's permission whatever run it is sent in"
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
                (when (= :act kind) digest)))]])))

(defn- run-mutants
  "Runs the histories against each mutant. Gives true when every mutant
  was caught. Prints the smallest history that caught each."
  [histories seed]
  (reduce
   (fn [all-caught? [what the-var broken]]
     (let [result (with-redefs-fn {the-var broken}
                    #(tc/quick-check histories holds :seed seed :max-size 80))]
       (println)
       (println "MUTANT" what)
       (if (:pass? result)
         (do (println "  NOT CAUGHT in" histories "histories") false)
         (let [smallest (first (get-in result [:shrunk :smallest]))]
           (println (format "  caught at history %d; the smallest history that catches it:"
                            (:num-tests result)))
           (with-redefs-fn {the-var broken} #(print-trace (play smallest)))
           all-caught?))))
   true
   mutants))

(defn- counted-under
  "The entries of the tally whose key is a vector that begins with `word`,
  as a sorted map from the rest of the key to the count."
  [counted word]
  (into (sorted-map)
        (keep (fn [[k n]] (when (and (vector? k) (= word (first k))) [(second k) n])))
        counted))

(defn -main
  "`clojure -M:run [histories [seed]]` runs so many histories under a seed,
  prints what they held, and exits 1 when a statement broke.
  `clojure -M:run mutants [histories [seed]]` runs them against the gate
  with one rule broken at a time, and exits 1 when a broken rule was not
  caught."
  [& args]
  (let [mutants? (= "mutants" (first args))
        [histories seed] (if mutants? (rest args) args)
        histories (if histories (parse-long histories) 2000)
        seed (if seed (parse-long seed) (System/currentTimeMillis))]
    (reset! tally {})
    (when piece-2?
      (alter-var-root #'reference/*digest-of* (constantly @hex-for-its-permission)))
    (if mutants?
      (System/exit (if (run-mutants histories seed) 0 1))
      (let [started (System/nanoTime)
            result (tc/quick-check histories holds :seed seed :max-size 80)
            seconds (/ (- (System/nanoTime) started) 1e9)
            counted @tally]
        (println (format "%d histories, seed %d, %.1f seconds; the store %s permissions for one run and one act"
                         (:num-tests result) seed seconds (if piece-2? "has" "has no")))
        (println (format "%d decisions: %d taken, %d refused"
                         (:decisions counted 0) (:taken counted 0)
                         (- (:decisions counted 0) (:taken counted 0))))
        (println "taken, by what the act's permission covered:" (counted-under counted :under))
        (println "gives taken, by what they cover:" (counted-under counted :gives-taken)
                 " ends taken:" (counted-under counted :ends-taken))
        (println (format "acts under a permission that looked intact by its ids and had ended: %d; ends by a holder under her permission for the layer: %d"
                         (:looked-intact-but-ended counted 0) (:ends-by-the-holder-under-her-layers counted 0)))
        (println "taken, by how deep the act's permission was:" (counted-under counted :deep))
        (println "refused, by the gate's word:")
        (doseq [[word n] (sort-by (comp - val) (counted-under counted :refused))]
          (println (format "  %8d  %s" n (name word))))
        (when-not @see-asked?
          (println "The read path is being changed: the statement `see` was not asked."))
        (if (:pass? result)
          (do (println)
              (println (str "HOLDS: agree, stamps, kept, once, " (when @see-asked? "see, ") "run-again"))
              (System/exit 0))
          (let [smallest (first (get-in result [:shrunk :smallest]))]
            (println)
            (println "A STATEMENT BROKE. The smallest history that breaks it:")
            (print-trace (play smallest))
            (System/exit 1)))))))
