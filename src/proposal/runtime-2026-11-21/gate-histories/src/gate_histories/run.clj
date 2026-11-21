(ns gate-histories.run
  "Generated histories of one layer, decided by the store's own gate code and
  held against the statements.

  A history is a list of things that makers send into one layer: gives,
  ends, gives after an end, plain facts, settings, the same thing sent
  twice. Some name a permission that is right, and some name one that is
  stale, raised, or another holder's. test.check makes the histories and,
  when one breaks a statement, cuts it down to the smallest that still does.

  Each thing is decided twice: by the gate (`gate-histories.walk`, which
  calls `softland.store.gate.decision`) and by the statements
  (`gate-histories.reference`). Five things must hold for every history:

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

    clojure -M:run            ; 2000 histories, a seed of its own
    clojure -M:run 20000 7    ; so many histories, this seed

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
            [softland.store.record.rows :as record]))

;; ------------------------------------------------------------------ the world

(def layer "The one layer of a history." :lab)
(def holders "Who can send. The first makes the layer." [:asha :ben :cal :dee :eve])
(def root "The holder of the layer's first permission." (first holders))
(def root-permission "The layer's first permission." [root layer layer])
(def ^:private now "The time on each thing. No rule reads it." 1700000000000)

(defn- comparable
  "A value with each byte array as a vector, so that two records compare."
  [x]
  (walk/postwalk (fn [v] (if (bytes? v) (vec v) v)) x))

(defn- thing
  "A thing as it arrives on the streaming depot: the act-id of this step,
  the time, and the maker's act."
  [step who permission facts]
  {:act-id [layer :streaming (java.util.UUID. 0 (long step))]
   :depot :streaming
   declaration/time-part now
   :version act/version
   :who who
   :layer layer
   :permission permission
   :session :s1
   :facts (vec facts)})

(def ^:private making-thing
  "The act that makes the layer: its depot, and the first permission."
  (thing 0 root root-permission
         [{:e layer :k :softland.store/depot :v :streaming}
          {:e root :k :softland.store/permission :v root-permission}]))

;; ------------------------------------------------------------- the generators

(def ^:private gen-holder (gen/elements holders))

(def ^:private gen-permission-choice
  "Which permission a maker names: mostly the one she was last given; else
  an older one, her own with what it allows changed, or another holder's."
  (gen/frequency [[12 (gen/return :current)]
                  [2 (gen/fmap (fn [n] [:older n]) gen/nat)]
                  [1 (gen/return :raised)]
                  [1 (gen/fmap (fn [h] [:of h]) gen-holder)]]))

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
                            (gen/elements [:everyone :members])))]]))

(def ^:private gen-op
  "One step of a history."
  (gen/frequency
   [[30 (gen/fmap (fn [[by choice fact]] {:op :act :by by :permission choice :facts [fact]})
                  (gen/tuple gen-holder gen-permission-choice gen-fact))]
    [4 (gen/fmap (fn [[by choice f1 f2]] {:op :act :by by :permission choice :facts [f1 f2]})
                 (gen/tuple gen-holder gen-permission-choice gen-fact gen-fact))]
    [2 (gen/fmap (fn [n] {:op :again :n n}) gen/nat)]
    [1 (gen/return {:op :make-again})]]))

(def gen-history
  "A history: up to eighty steps after the making act."
  (gen/vector gen-op 0 80))

;; ------------------------------------------------- from a step to a thing sent

(defn- current-permission
  "The permission that a holder was last given, by the gives that the gate
  took; a guess beneath the first permission for a holder who was given
  none."
  [pools holder]
  (or (peek (get pools holder)) [holder layer layer root-permission]))

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
  "The permission that the maker of this step names."
  [pools by choice]
  (let [pool (get pools by)]
    (cond
      (= :current choice) (current-permission pools by)
      (= :raised choice) (raised (current-permission pools by))
      (= :older (first choice)) (if (seq pool)
                                  (nth pool (mod (second choice) (count pool)))
                                  (current-permission pools by))
      (= :of (first choice)) (current-permission pools (second choice)))))

(defn- fact-of
  "One fact as its maker sends it."
  [pools step permission {:keys [f to allows beneath of e k key word]}]
  (case f
    :give {:e to
           :k :softland.store/permission
           :v (cond-> [to layer layer (if beneath (current-permission pools beneath) permission)]
                allows (conj allows))}
    :end {:e of :k :softland.store/permission}
    :plain {:e e :k k :v (str "said at step " step)}
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

(defn- thing-of
  "The thing that one step sends, or nil for a step that sends none."
  [{:keys [pools things]} step {:keys [op by permission facts n]}]
  (case op
    :act (let [named (named-permission pools by permission)]
           (thing step by named (one-fact-a-place (map #(fact-of pools step named %) facts))))
    :make-again (assoc making-thing :act-id [layer :streaming (java.util.UUID. 0 (long step))])
    :again (nth things (mod n (count things)))))

;; ------------------------------------------------------------ playing a history

(def tally
  "What the histories of a run held: counted while they play."
  (atom {}))

(defn- count! [k] (swap! tally update k (fnil inc 0)))

(defn- gives-taken
  "The gives of a thing, as [holder permission]."
  [sent]
  (keep (fn [{:keys [e k v]}] (when (and (= :softland.store/permission k) (some? v)) [e v]))
        (:facts sent)))

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

(defn- see-violation
  "The statement `see` after one decision. Takes the record's facts and the
  statements' state. Gives nil, or the first asker for whom the read path
  and the statements differ."
  [facts ref]
  (first
   (for [who askers
         signed-in? [true false]
         :let [read-path (seeing facts who signed-in?)
               statements (reference/may-see ref who signed-in?)]
         :when (not= read-path statements)]
     {:statement :see
      :what (str "the read path says " (name read-path) " and the statements say " (name statements)
                 " for " (name who) (when-not signed-in? ", not signed in"))})))

(defn play
  "Plays a history. Gives {:steps :violation :record}: each step with what
  was sent and what the gate and the statements said, and the first
  statement that broke, or nil."
  [ops]
  (loop [state {:rec gate/empty-record :ref reference/start :pools {root [root-permission]}
                :things [] :steps [] :decisions 0}
         todo (cons {:op :make} ops)
         step 0]
    (if (empty? todo)
      (let [replayed (reduce (fn [rec sent] (:record (gate/decide rec :streaming sent)))
                             gate/empty-record
                             (:things state))]
        {:steps (:steps state)
         :record (:rec state)
         :violation (when (not= (comparable replayed) (comparable (:rec state)))
                      {:statement :run-again :what "the same things gave another record"})})
      (let [op (first todo)
            sent (if (= :make (:op op)) making-thing (thing-of state step op))
            again? (= :again (:op op))
            {:keys [record outcome]} (gate/decide (:rec state) :streaming sent)
            ruled (when-not again? (reference/judge (:ref state) layer sent))
            taken? (= :accepted (:verdict outcome))
            line {:step step :op (:op op) :sent sent :outcome (dissoc outcome :writes :act)
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
            (count! [:deep (count (take-while some? (iterate #(nth % 3 nil) (:permission sent))))]))
          (when (and (= (:who sent) (first (:permission sent)))
                     (reference/looks-intact? (:ref state) (:permission sent))
                     (nil? (reference/standing-grant (:ref state) (:who sent) (:permission sent))))
            (count! :looked-intact-but-ended))
          (when taken?
            (doseq [{:keys [k v e]} (:facts sent)
                    :when (= :softland.store/permission k)]
              (cond
                (nil? v) (count! :ends-taken)
                (get-in state [:ref :at e]) (count! :gives-again-taken)
                :else (count! :gives-taken)))))
        (if violation
          {:steps (conj (:steps state) (assoc line :ref-before (reference/summary (:ref state))))
           :record record
           :violation (assoc violation :step step)}
          (recur (-> state
                     (assoc :rec record)
                     (cond-> (not again?) (assoc :ref (:state ruled)))
                     (cond-> (not again?) (update :decisions inc))
                     (cond-> (not again?) (update :things conj sent))
                     (cond-> taken? (update :pools (fn [pools]
                                                     (reduce (fn [pools [holder permission]]
                                                               (update pools holder (fnil conj []) permission))
                                                             pools
                                                             (gives-taken sent)))))
                     (update :steps conj line))
                 (rest todo)
                 (inc step)))))))

(def holds
  "The property: no statement breaks in any history."
  (prop/for-all [ops gen-history]
    (nil? (:violation (play ops)))))

;; --------------------------------------------------------------------- the run

(defn- chain-line
  "A permission id as a short line: the holders of its chain, the newest
  first, with what it allows."
  [permission]
  (when permission
    (let [chain (loop [p permission out []]
                  (if p (recur (nth p 3 nil) (conj out (name (first p)))) out))]
      (str (apply str (interpose "<" chain))
           (when (= 5 (count permission)) (str " (" (name (nth permission 4)) ")"))))))

(defn- fact-line [{:keys [e k v]}]
  (cond
    (and (= :softland.store/permission k) (some? v)) (str "gives " (chain-line v))
    (= :softland.store/permission k) (str "ends " (name e) "'s")
    (= "softland.store" (namespace k)) (str "sets " (name k))
    :else (str "writes " (name e) " " (name k))))

(defn- print-trace
  "Prints a played history, one step a line."
  [{:keys [steps violation]}]
  (doseq [{:keys [step op sent outcome statements]} steps]
    (println (format "  %2d  %-5s under %-22s %-44s gate: %-34s statements: %s"
                     step
                     (name (:who sent))
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
    (println "  who stood before that step:" (:ref-before (peek steps)))))

(def ^:private mutants
  "The gate with one rule broken on purpose, each as [what, the var, the
  broken function]. A run that is worth anything finds a history that
  breaks a statement under each of them: `clojure -M:run mutants`. The
  store's code is not changed: the function is put in place for the run
  only."
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
    (fn [_own-rows _layer] :members)]])

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
    (if mutants?
      (System/exit (if (run-mutants histories seed) 0 1))
      (let [started (System/nanoTime)
            result (tc/quick-check histories holds :seed seed :max-size 80)
            seconds (/ (- (System/nanoTime) started) 1e9)
            counted @tally]
        (println (format "%d histories, seed %d, %.1f seconds" (:num-tests result) seed seconds))
        (println (format "%d decisions: %d taken, %d refused"
                         (:decisions counted 0) (:taken counted 0)
                         (- (:decisions counted 0) (:taken counted 0))))
        (println (format "taken: %d gives, %d ends, %d gives after an end"
                         (:gives-taken counted 0) (:ends-taken counted 0) (:gives-again-taken counted 0)))
        (println (format "acts under a permission that looked intact by its ids and had ended: %d"
                         (:looked-intact-but-ended counted 0)))
        (println "taken, by how deep the act's permission was:"
                 (into (sorted-map) (keep (fn [[k n]] (when (and (vector? k) (= :deep (first k))) [(second k) n]))) counted))
        (println "refused, by the gate's word:")
        (doseq [[[_ word] n] (sort-by (comp - val) (filter (fn [[k _]] (and (vector? k) (= :refused (first k)))) counted))]
          (println (format "  %8d  %s" n (name word))))
        (if (:pass? result)
          (do (println) (println "HOLDS: agree, stamps, kept, once, see, run-again") (System/exit 0))
          (let [smallest (first (get-in result [:shrunk :smallest]))]
            (println)
            (println "A STATEMENT BROKE. The smallest history that breaks it:")
            (print-trace (play smallest))
            (System/exit 1)))))))
