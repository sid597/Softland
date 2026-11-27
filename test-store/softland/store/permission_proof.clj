(ns softland.store.permission-proof
  "Proof of the permission rules of the store's pure code, with no cluster:
  the act's form, the door's declaration, the gate's checks and the rows
  that a decision writes, and the read of an asker's rights asked once.

  Each check sends acts, as a maker hands them to the door, through the
  door's additions and the walk of one thing
  (`softland.store.gate.walk`), against a record held as a map
  (`softland.store.record.in-memory`), and reads what the gate answered and
  who may see the lane. The checks are told in the order of the lines of
  `src/proposal/permissions-2026-11-27/BUILD.md`, batches 1 to 4, and each
  sentence says what must hold.

  What it does not reach. A gate's module that drives the walk against its
  tables, and a read that is kept live: both need a cluster, and are proved
  in `softland.store.module-proof` and `softland.store.read-path-proof`. The
  statement to prove of `PERMISSIONS.md`, section 12, over generated
  histories: the judge under
  `src/proposal/runtime-2026-11-21/gate-histories/` holds it against this
  same code (`clojure -M:run fix`), and no check here says it a second
  time."
  (:require [clojure.test :refer [deftest is run-tests]]
            [softland.store.act :as act]
            [softland.store.door.additions :as additions]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.gate.chain :as chain]
            [softland.store.gate.first-act :as first-act]
            [softland.store.gate.walk :as walk]
            [softland.store.read-path.acts :as read-acts]
            [softland.store.read-path.answer :as read-answer]
            [softland.store.read-path.lanes :as read-lanes]
            [softland.store.read-path.query :as read-query]
            [softland.store.read-path.sources :as read-sources]
            [softland.store.record.in-memory :as memory]
            [softland.store.record.rows :as record])
  (:import [java.util UUID]))

(defonce ^{:private true
           :doc "The lines that the proof has printed with SHOWN, in order, for the
  recap that `-main` prints at the end."}
  shown-lines (atom []))

(defn- shown
  [& xs]
  (let [line (apply print-str "SHOWN" xs)]
    (swap! shown-lines conj line)
    (println line)
    (flush)))

(defn- holds
  "Checks each case of a group: its function must give what the case says.
  Prints each case's sentence after PASSED or FAILED. Gives the number of
  cases."
  [cases]
  (doseq [[what f expected] cases]
    (let [got (try (f) (catch Throwable t [:threw (.getSimpleName (class t)) (.getMessage t)]))]
      (println (if (is (= expected got) what) "PASSED" "FAILED") what)))
  (count cases))

;; ---------------------------------------------------------------- the world

(def ^:private lane :lab)
(def ^:private asha :person.asha)
(def ^:private ben :person.ben)
(def ^:private cal :person.cal)
(def ^:private dee :person.dee)
(def ^:private zed :person.zed)
(def ^:private ada :agent.ada)
(def ^:private bot :agent.bot)

(defn- g "The id of the n-th permission of a check." [n] (UUID. 71 (long n)))
(def ^:private g0 "The id of the lane's first permission." (g 0))

(def ^:private see {:see true})
(def ^:private see-write {:see true :write :all})
(def ^:private member {:see true :write :all :give #{:see :write :give}})
(def ^:private publisher {:see true :write :all :give #{:see :write :give :everyone}})
(def ^:private owner {:see true :write :all :give #{:see :write :give :owner :everyone} :owner true})

(defn- says
  "What a permission says: to whom, what it allows, who gave it and under
  which permission; and of `more`, `:lent-from`, `:session` and `:covers`."
  [holder allows giver under & {:as more}]
  (cond-> (merge {:holder holder :allows allows :covers [:default-group lane] :given-by giver} more)
    under (assoc :under under)))

(defn- gives [id value] {:e id :k :softland.store/permission :v value})
(defn- ends [id] {:e id :k :softland.store/permission})
(defn- note [e text] {:e e :k :note :v text})

(defn- by
  "A maker's act: who sends it, the permission that it names or nil, and
  its facts."
  [who permission & facts]
  (cond-> {:version act/version :who who :lane lane :session :sit-1 :facts (vec facts)}
    permission (assoc :permission permission)))

(def ^:private making
  "The act that makes the lane: its depot and its first permission, an
  owner's, Asha's."
  (by asha nil
      {:e lane :k :softland.store/depot :v :streaming}
      (gives g0 (says asha owner asha nil))))

(defn- arrived
  "The thing that the door appends for a maker's act under the n-th act-id."
  [n maker]
  (let [the-lane (:lane maker)
        act-id [the-lane (first-act/depot-at-making the-lane) (UUID. 0 (long n))]]
    (:thing (additions/thing-to-append (additions/reading maker act-id) act-id 1700000000000))))

(defn- played
  "Sends each maker's act in order, each under an act-id of its own. Gives
  {:record :verdicts :outcomes}."
  [makers]
  (reduce (fn [{:keys [record verdicts outcomes]} [n maker]]
            (let [{:keys [record outcome]} (memory/decide record
                                                          (first-act/depot-at-making (:lane maker))
                                                          (arrived n maker))]
              {:record record
               :verdicts (conj verdicts (:verdict outcome))
               :outcomes (conj outcomes outcome)}))
          {:record memory/empty-record :verdicts [] :outcomes []}
          (map-indexed vector makers)))

(defn- verdicts "What the gate answered to each act, the making act's left out." [& makers]
  (subvec (:verdicts (played (cons making makers))) 1))

(defn- after "The record after the making act and these." [& makers]
  (:record (played (cons making makers))))

(defn- sees
  ([record who] (sees record who nil))
  ([record who session] (memory/may-see record lane who session)))

(defn- says-at "What the permission at an id says, from its row." [record id]
  (record/held-permission (get (:facts record) (chain/place lane id))))

;; ------------------------------------------------- batch 1: the give gets an id

(deftest a-permission-has-an-id-of-its-own
  (holds
   [["a permission is a fact on an id that its giver makes; an act names the permission that it acts under, and an end names the permission that it ends"
     (fn [] (verdicts (by asha g0 (gives (g 1) (says ben member asha g0)))
                      (by ben (g 1) (note :thing-1 "under the give"))
                      (by asha g0 (ends (g 1)))
                      (by ben (g 1) (note :thing-1 "after the end"))))
     [:accepted :accepted :accepted :permission-ended]]
    ["the old form is refused for a new act: a permission written out, a who with no kind, and the three keys that are gone"
     (fn [] (verdicts (assoc (by asha nil (note :thing-1 "x")) :permission [:person.asha lane lane])
                      (assoc (by asha g0 (note :thing-1 "x")) :who :asha)
                      (by asha g0 {:e :sit-1 :k :softland.store/run-permission :v (says ben see asha g0)})
                      (by asha g0 {:e (g 1) :k :softland.store/act-permission :v (says ben see asha g0)})
                      (by asha g0 {:e lane :k :softland.store/who-may-see :v :everyone})))
     [:permission-not-an-id :who-not-a-party :store-key-not-known :store-key-not-known :store-key-not-known]]
    ["a permission sits on a UUID, and says who holds it, what it allows and covers, and who gave it: anything else is refused by the declaration"
     (fn [] (verdicts (by asha g0 (gives :person.ben (says ben see asha g0)))
                      (by asha g0 (gives (g 1) (dissoc (says ben see asha g0) :given-by)))
                      (by asha g0 (gives (g 1) (says ben {} asha g0)))
                      (by asha g0 (gives (g 1) (says ben {:see true :give #{}} asha g0)))
                      (by asha g0 (gives (g 1) (says :ben see asha g0)))
                      (by asha g0 (gives (g 1) (assoc (says ben see asha g0) :uses 3)))))
     [:permission-fact-not-on-an-id :permission-fact-not-a-permission :permission-fact-not-a-permission
      :permission-fact-not-a-permission :permission-fact-not-a-permission :permission-fact-not-a-permission]]
    ["an act names one permission by its id, and a row holds that id and no text of a permission: who gave a permission, under what, and what it is lent from are in what it says"
     (fn [] (let [record (after (by asha g0 (gives (g 1) (says ben member asha g0)))
                                (by ben (g 1) (gives (g 2) (says ada see ben (g 1) :lent-from (g 1)))))]
              [(:permission (get (:facts record) (chain/place lane (g 2))))
               (select-keys (says-at record (g 2)) [:given-by :under :lent-from])
               (contains? (get (:facts record) (chain/place lane g0)) :permission)]))
     [(str (g 1)) {:given-by ben :under (g 1) :lent-from (g 1)} false]]
    ["a part of what a permission says that is given as nil is left out, so two values that say the same have one text"
     (fn [] (says-at (after (by asha g0 (gives (g 1) (assoc (says ben see asha g0) :lent-from nil :session nil))))
                     (g 1)))
     (says ben see asha g0)]]))

(deftest a-holder-has-any-number-of-permissions
  (holds
   [["a holder is given a second permission while her first stands, and each is used and ended by itself"
     (fn [] (verdicts (by asha g0 (gives (g 1) (says ben see asha g0)))
                      (by asha g0 (gives (g 2) (says ben member asha g0)))
                      (by ben (g 1) (note :thing-1 "under see"))
                      (by ben (g 2) (note :thing-1 "under member"))
                      (by asha g0 (ends (g 2)))
                      (by ben (g 2) (note :thing-1 "after its end"))
                      (by ben nil (ends (g 1)))))
     [:accepted :accepted :permission-does-not-allow-writing :accepted :accepted :permission-ended :accepted]]
    ["she still sees the lane under the one that stands when the other is ended, and no longer when both are"
     (fn [] (let [both [(by asha g0 (gives (g 1) (says ben see asha g0)))
                        (by asha g0 (gives (g 2) (says ben member asha g0)))]]
              [(sees (apply after both) ben)
               (sees (apply after (conj both (by asha g0 (ends (g 2))))) ben)
               (sees (apply after (conj both (by asha g0 (ends (g 2))) (by asha g0 (ends (g 1))))) ben)]))
     [:yes :yes :may-not-see]]]))

(deftest never-given-is-answered-apart-from-ended
  (holds
   [["an act under a permission that was never given, under one that ended, and under one that was used get three words"
     (fn [] (let [yes (says ben {:write :all} asha g0
                            :covers [:exact-act (digest/hex-for-a-yes (by ben nil (note :thing-1 "the one act")))])]
              (verdicts (by ben (g 9) (note :thing-1 "never given"))
                        (by asha g0 (gives (g 1) (says ben member asha g0)))
                        (by asha g0 (ends (g 1)))
                        (by ben (g 1) (note :thing-1 "ended"))
                        (by asha g0 (gives (g 2) yes))
                        (by ben (g 2) (note :thing-1 "the one act"))
                        (by ben (g 2) (note :thing-1 "the one act")))))
     [:permission-never-given :accepted :accepted :permission-ended :accepted :accepted :permission-used-up]]
    ["an end of a permission that was never given, of one that ended, and of one that was used get three words"
     (fn [] (let [yes (says ben {:write :all} asha g0
                            :covers [:exact-act (digest/hex-for-a-yes (by ben nil (note :thing-1 "the one act")))])]
              (verdicts (by asha g0 (ends (g 9)))
                        (by asha g0 (gives (g 1) (says ben member asha g0)))
                        (by asha g0 (ends (g 1)))
                        (by asha g0 (ends (g 1)))
                        (by asha g0 (gives (g 2) yes))
                        (by ben (g 2) (note :thing-1 "the one act"))
                        (by asha g0 (ends (g 2))))))
     [:no-permission-to-end :accepted :accepted :permission-already-ended :accepted :accepted :permission-already-used]]
    ["an id takes one permission for good: nothing is given at an id whose permission ended, and given again it is a new permission with a new id"
     (fn [] (verdicts (by asha g0 (gives (g 1) (says ben member asha g0)))
                      (by asha g0 (ends (g 1)))
                      (by asha g0 (gives (g 1) (says ben member asha g0)))
                      (by asha g0 (gives (g 2) (says ben member asha g0)))
                      (by ben (g 2) (note :thing-1 "under the new one"))))
     [:accepted :accepted :a-permission-id-takes-one-permission :accepted :accepted]]]))

(deftest a-holder-ends-her-own
  (holds
   [["a holder who may only see ends her own permission, in an act that names none and in an act under that permission itself"
     (fn [] (verdicts (by asha g0 (gives (g 1) (says ben see asha g0)))
                      (by asha g0 (gives (g 2) (says ben see asha g0)))
                      (by ben nil (ends (g 1)))
                      (by ben (g 2) (ends (g 2)))))
     [:accepted :accepted :accepted :accepted]]
    ["an act that names no permission is taken only when all it does is end permissions that its author holds: not with another fact beside, and not for another holder's"
     (fn [] (verdicts (by asha g0 (gives (g 1) (says ben see asha g0)))
                      (by asha g0 (gives (g 2) (says cal see asha g0)))
                      (by ben nil (ends (g 1)) (note :thing-1 "and a note"))
                      (by ben nil (ends (g 2)))
                      (by ben nil (ends (g 1)) (ends (g 2)))
                      (by ben nil (note :thing-1 "nothing named"))))
     [:accepted :accepted :no-permission :no-permission :no-permission :no-permission]]]))

(deftest what-batch-1-keeps-as-today
  (holds
   [["a permission that is good for one login sitting takes an act only in that sitting, and lets its holder see only when she asks in it"
     (fn [] (let [given (by asha g0 (gives (g 1) (says ben see-write asha g0 :session :sit-7)))
                  record (after given)]
              [(verdicts given
                         (assoc (by ben (g 1) (note :thing-1 "in it")) :session :sit-7)
                         (assoc (by ben (g 1) (note :thing-1 "in another")) :session :sit-8))
               (sees record ben :sit-7) (sees record ben :sit-8) (sees record ben)]))
     [[:accepted :accepted :permission-for-another-sitting] :yes :may-not-see :may-not-see]]
    ["a permission for one exact act takes that act and no other, part for part, its who and its sitting among them, and lets nobody see the lane"
     (fn [] (let [the-act (by ben nil (note :thing-1 "the one act"))
                  yes (by asha g0 (gives (g 1) (says ben {:see true :write :all} asha g0
                                                     :covers [:exact-act (digest/hex-for-a-yes the-act)])))]
              [(verdicts yes
                         (by ben (g 1) (note :thing-1 "another act"))
                         (assoc (by ben (g 1) (note :thing-1 "the one act")) :session :sit-2)
                         (by cal (g 1) (note :thing-1 "the one act"))
                         (by ben (g 1) (note :thing-1 "the one act")))
               (sees (after yes) ben)]))
     [[:accepted :act-not-the-one-permitted :act-not-the-one-permitted :who-not-the-permissions-holder :accepted]
      :may-not-see]]
    ["an act under an act-id that has an answer is not decided again and writes nothing, whether it is the same act or another"
     (fn [] (let [{:keys [record]} (played [making (by asha g0 (note :thing-1 "first"))])
                  again (memory/decide record :streaming (arrived 1 (by asha g0 (note :thing-1 "first"))))
                  other (memory/decide record :streaming (arrived 1 (by asha g0 (note :thing-1 "another"))))]
              [(:outcome again) (= record (:record again)) (:outcome other) (= record (:record other))]))
     [{:repeat true :writes []} true {:repeat true :writes []} true]]]))

;; ----------------------------------- batch 2: lent or standing, the right to give

(deftest lent-or-standing
  (holds
   [["a person stays when the permission of the one who let her in ends: a give to a person stands on its own"
     (fn [] (let [acts [(by asha g0 (gives (g 1) (says ben member asha g0)))
                        (by ben (g 1) (gives (g 2) (says cal member ben (g 1))))
                        (by asha g0 (ends (g 1)))]]
              [(apply verdicts (conj acts (by cal (g 2) (note :thing-1 "still here"))))
               (sees (apply after acts) cal) (sees (apply after acts) ben)]))
     [[:accepted :accepted :accepted :accepted] :yes :may-not-see]]
    ["an agent falls when its person's permission ends: what is lent falls with what it is lent from, however far up"
     (fn [] (let [acts [(by asha g0 (gives (g 1) (says ben member asha g0)))
                        (by ben (g 1) (gives (g 2) (says ada see-write ben (g 1) :lent-from (g 1))))
                        (by ada (g 2) (gives (g 3) (says bot see ada (g 2) :lent-from (g 2))))
                        (by ada (g 2) (note :thing-1 "while it stands"))
                        (by asha g0 (ends (g 1)))]]
              [(apply verdicts (conj acts (by ada (g 2) (note :thing-1 "after")) (by bot (g 3) (ends (g 3)))))
               (sees (apply after acts) ada) (sees (apply after acts) bot)]))
     [[:accepted :accepted :accepted :accepted :accepted :permission-above-ended :permission-above-ended]
      :may-not-see :may-not-see]]
    ["a holder who may only see lends what she sees to an agent, with no right to give, and the agent sees what she sees and writes nothing"
     (fn [] (let [acts [(by asha g0 (gives (g 1) (says ben see asha g0)))
                        (by ben (g 1) (gives (g 2) (says ada see ben (g 1) :lent-from (g 1))))]]
              [(apply verdicts (conj acts (by ada (g 2) (note :thing-1 "may it write"))))
               (sees (apply after acts) ada)]))
     [[:accepted :accepted :permission-does-not-allow-writing] :yes]]
    ["a give to an agent or a run is always lent: the gate tells a person from an agent or a run by the party's id"
     (fn [] (verdicts (by asha g0 (gives (g 1) (says ada see asha g0)))
                      (by asha g0 (gives (g 2) (says :run.r1 see asha g0)))
                      (by asha g0 (gives (g 3) (says ada see asha g0 :lent-from g0)))
                      (by asha g0 (gives (g 4) (says ben see asha g0)))))
     [:an-agent-or-a-run-only-borrows :an-agent-or-a-run-only-borrows :accepted :accepted]]
    ["what is made under a lent permission is lent from it; a give is lent from the permission that its act names and from no third one"
     (fn [] (verdicts (by asha g0 (gives (g 1) (says ben member asha g0 :lent-from g0)))
                      (by ben (g 1) (gives (g 2) (says cal see ben (g 1))))
                      (by ben (g 1) (gives (g 3) (says cal see ben (g 1) :lent-from g0)))
                      (by ben (g 1) (gives (g 4) (says cal see ben (g 1) :lent-from (g 1))))))
     [:accepted :made-under-a-lent-permission-is-lent :permission-not-lent-from-the-acts-own :accepted]]
    ["a give says who gave it, the act's author, and under what, the permission that the act names"
     (fn [] (verdicts (by asha g0 (gives (g 1) (says ben see cal g0)))
                      (by asha g0 (gives (g 2) (says ben see asha (g 7))))
                      (by asha g0 (gives (g 3) (says ben see asha nil)))))
     [:permission-not-given-by-the-acts-who :permission-not-under-the-acts-own :permission-not-under-the-acts-own]]]))

(deftest give-only-what-you-hold
  (holds
   [["passing to another person needs the right to give, and that right says what may be given on"
     (fn [] (verdicts (by asha g0 (gives (g 1) (says ben see-write asha g0)))
                      (by ben (g 1) (gives (g 2) (says cal see ben (g 1))))
                      (by asha g0 (gives (g 3) (says dee {:see true :write :all :give #{:see}} asha g0)))
                      (by dee (g 3) (gives (g 4) (says cal see dee (g 3))))
                      (by dee (g 3) (gives (g 5) (says cal see-write dee (g 3))))
                      (by dee (g 3) (gives (g 6) (says cal {:see true :give #{:see}} dee (g 3))))))
     [:accepted :permission-gives-more-than-its-giver-may :accepted :accepted
      :permission-gives-more-than-its-giver-may :permission-gives-more-than-its-giver-may]]
    ["nobody gives what she does not hold: a member gives no owning, and a lend to an agent holds no more than the lender holds"
     (fn [] (verdicts (by asha g0 (gives (g 1) (says ben member asha g0)))
                      (by ben (g 1) (gives (g 2) (says cal owner ben (g 1))))
                      (by asha g0 (gives (g 3) (says dee see asha g0)))
                      (by dee (g 3) (gives (g 4) (says ada see-write dee (g 3) :lent-from (g 3))))))
     [:accepted :permission-gives-more-than-its-giver-may :accepted :permission-gives-more-than-its-giver-may]]
    ["a give covers no more than the permission it is made under: not another lane, and not every sitting under a permission for one"
     (fn [] (verdicts (by asha g0 (gives (g 1) (says ben member asha g0 :covers [:default-group :elsewhere])))
                      (by asha g0 (gives (g 2) (says ben member asha g0 :session :sit-1)))
                      (by ben (g 2) (gives (g 3) (says cal see ben (g 2))))
                      (by ben (g 2) (gives (g 4) (says cal see ben (g 2) :session :sit-1)))))
     [:permission-covers-more-than-the-acts-own :accepted :permission-covers-more-than-the-acts-own :accepted]]
    ["a chain of lent permissions holds 32 at most"
     (fn [] (let [lends (for [n (range 1 33)]
                          (by (if (= 1 n) asha (if (even? n) ada bot)) (g (dec n))
                              (gives (g n) (says (if (odd? n) ada bot) see
                                                 (if (= 1 n) asha (if (even? n) ada bot))
                                                 (g (dec n)) :lent-from (g (dec n))))))
                  got (apply verdicts lends)]
              [(count (filter #{:accepted} got)) (last got)]))
     [31 :permission-lent-too-deep]]]))

(deftest the-four-who-may-end
  (let [setup [(by asha g0 (gives (g 1) (says ben member asha g0)))
               (by ben (g 1) (gives (g 2) (says cal member ben (g 1))))
               (by ben (g 1) (gives (g 3) (says ada see ben (g 1) :lent-from (g 1))))
               (by ada (g 3) (gives (g 4) (says bot see ada (g 3) :lent-from (g 3))))
               (by asha g0 (gives (g 5) (says dee member asha g0)))
               (by asha g0 (gives (g 6) (says ben see asha g0)))]
        then (fn [& more] (subvec (apply verdicts (concat setup more)) (count setup)))]
    (holds
     [["its holder ends a permission, and nobody who is none of the four does"
       (fn [] (then (by dee (g 5) (ends (g 2))) (by cal nil (ends (g 2)))))
       [:may-not-end-the-permission :accepted]]
      ["whoever gave it ends it, in an act under the permission that she gave it under, while it still lets her pass what the permission holds to its holder, and under no other of hers"
       (fn [] (then (by ben (g 6) (ends (g 2)))
                    (by ben (g 1) (ends (g 2)))))
       [:may-not-end-the-permission :accepted]]
      ["a giver who was lowered in place cannot end or lower what she can no longer pass on, while she still ends what she can still pass and what is lent from her"
       (fn [] (let [more [(by ben (g 1) (gives (g 7) (says dee see ben (g 1))))
                          (by ben (g 1) (gives (g 8) (says ada see-write ben (g 1) :lent-from (g 1))))
                          (by asha g0 (gives (g 1) (says ben {:see true :give #{:see}} asha g0)))]
                    after-lower (fn [& acts] (subvec (apply verdicts (concat setup more acts)) (+ (count setup) (count more))))]
                [(after-lower (by ben (g 1) (ends (g 2)))
                              (by ben (g 1) (gives (g 2) (says cal see ben (g 1))))
                              (by ben (g 1) (ends (g 7)))
                              (by ben (g 1) (gives (g 8) (says ada see ben (g 1) :lent-from (g 1))))
                              (by ben (g 1) (ends (g 8))))
                 (verdicts (by asha g0 (gives (g 1) (says ben owner asha g0)))
                           (by ben (g 1) (gives (g 2) (says cal owner ben (g 1))))
                           (by cal (g 2) (gives (g 1) (says ben member asha g0)))
                           (by ben (g 1) (ends (g 2)))
                           (by ben (g 1) (gives (g 2) (says cal see ben (g 1)))))]))
       [[:may-not-end-the-permission :may-not-lower-the-permission :accepted :accepted :accepted]
        [:accepted :accepted :accepted :may-not-end-the-permission :may-not-lower-the-permission]]]
      ["the holder of a permission that it is lent from ends it, directly and further up"
       (fn [] (then (by ada (g 3) (ends (g 4))) (by ben (g 1) (ends (g 3)))))
       [:accepted :accepted]]
      ["further up: the lender two above ends the lowest"
       (fn [] (then (by ben (g 1) (ends (g 4)))))
       [:accepted]]
      ["an owner ends any permission over the lane, in an act under her owner's permission"
       (fn [] (then (by asha g0 (ends (g 4))) (by asha g0 (ends (g 2)))))
       [:accepted :accepted]]
      ["a permission that fell with the one above it is ended by its holder and by an owner; one further up than the end cannot be told from a stranger, since the row of the ended permission no longer says what it was lent from"
       (fn [] (then (by ada nil (ends (g 3)))
                    (by ben (g 1) (ends (g 4)))
                    (by asha g0 (ends (g 4)))))
       [:accepted :may-not-end-the-permission :accepted]]])))

;; ------------------------- batch 3: one act as one moment, change, owners

(deftest one-act-is-one-moment
  (holds
   [["an act fails as a whole on one bad change: none of it is kept"
     (fn [] (let [acts [(by asha g0 (gives (g 1) (says ben member asha g0)))
                        (by ben (g 1)
                            (gives (g 2) (says cal see ben (g 1)))
                            (gives (g 3) (says dee owner ben (g 1))))]
                  record (apply after acts)]
              [(apply verdicts (conj acts (by cal (g 2) (ends (g 2)))))
               (contains? (:facts record) (chain/place lane (g 2)))
               (sees record cal)]))
     [[:accepted :permission-gives-more-than-its-giver-may :permission-never-given] false :may-not-see]]
    ["every change of an act is held against what its author held just before it: she ends her own permission and gives under it in the one act, in either order"
     (fn [] (verdicts (by asha g0 (gives (g 1) (says ben member asha g0)))
                      (by ben (g 1) (ends (g 1)) (gives (g 2) (says cal member ben (g 1))))
                      (by cal (g 2) (gives (g 3) (says dee see cal (g 2))) (ends (g 2)))
                      (by dee (g 3) (note :thing-1 "it stands on its own"))))
     [:accepted :accepted :accepted :permission-does-not-allow-writing]]
    ["no permission that an act gives is left lent from one that the same act ends"
     (fn [] (verdicts (by asha g0 (gives (g 1) (says ben member asha g0)))
                      (by ben (g 1) (gives (g 2) (says ada see ben (g 1) :lent-from (g 1))) (ends (g 1)))))
     [:accepted :permission-lent-from-one-the-act-ends]]
    ["no permission is changed twice in one act: an act names a permission's id in one fact at most"
     (fn [] (verdicts (by asha g0 (gives (g 1) (says ben member asha g0)))
                      (by asha g0 (gives (g 1) (says ben see asha g0)) (ends (g 1)))
                      (by asha g0 (gives (g 2) (says cal see asha g0)) (gives (g 2) (says cal member asha g0)))))
     [:accepted :place-named-twice :place-named-twice]]]))

(deftest a-change-keeps-the-id
  (let [setup [(by asha g0 (gives (g 1) (says ben member asha g0)))
               (by ben (g 1) (gives (g 2) (says ada see-write ben (g 1) :lent-from (g 1))))
               (by asha g0 (gives (g 3) (says cal member asha g0)))]
        then (fn [& more] (subvec (apply verdicts (concat setup more)) (count setup)))
        lower (by asha g0 (gives (g 1) (says ben see asha g0)))
        raise (by asha g0 (gives (g 1) (says ben member asha g0)))]
    (holds
     [["a change keeps a permission's id and replaces what it allows: its holder, who gave it, under what and what it is lent from stay"
       (fn [] [(then lower
                     (by asha g0 (gives (g 1) (says cal see asha g0)))
                     (by asha g0 (gives (g 1) (says ben {:see true :give #{:see}} asha g0 :lent-from g0)))
                     (by cal (g 3) (gives (g 2) (says ada see cal (g 3) :lent-from (g 1)))))
               (says-at (apply after (conj setup lower)) (g 1))])
       [[:accepted :a-change-keeps-holder-giver-and-lender :a-change-keeps-holder-giver-and-lender
         :a-change-keeps-holder-giver-and-lender]
        (says ben see asha g0)]]
      ["lowering needs what ending needs, and raising needs what giving needs: another member does neither, and a holder does not raise herself"
       (fn [] (then (by cal (g 3) (gives (g 1) (says ben see asha g0)))
                    lower
                    (by ben (g 1) (gives (g 1) (says ben member asha g0)))
                    (by cal (g 3) (gives (g 1) (says ben see-write asha g0)))
                    (by cal (g 3) (gives (g 1) (says ben owner asha g0)))))
       [:may-not-lower-the-permission :accepted :permission-gives-more-than-its-giver-may
        :accepted :permission-gives-more-than-its-giver-may]]
      ["a raise is held as a give of what is added: a right is added to a lent permission only under the permission that it is lent from"
       (fn [] (then (by ben (g 1) (gives (g 2) (says ada {:see true} ben (g 1) :lent-from (g 1))))
                    (by asha g0 (gives (g 2) (says ada see-write ben (g 1) :lent-from (g 1))))
                    (by cal (g 3) (gives (g 2) (says ada see-write ben (g 1) :lent-from (g 1))))
                    (by ben (g 1) (gives (g 2) (says ada see-write ben (g 1) :lent-from (g 1))))))
       [:accepted :permission-not-lent-from-the-acts-own :permission-not-lent-from-the-acts-own :accepted]]
      ["a raise under a lent permission adds nothing to a permission that stands on its own: what it added would outlive the lender"
       (fn [] (then (by asha g0 (gives (g 4) (says dee see asha g0)))
                    (by asha g0 (gives (g 5) (says bot member asha g0 :lent-from g0)))
                    (by bot (g 5) (gives (g 4) (says dee member asha g0)))
                    (by asha g0 (gives (g 6) (says dee member asha g0 :lent-from g0)))
                    (by dee (g 6) (gives (g 4) (says dee member asha g0)))))
       [:accepted :accepted :made-under-a-lent-permission-is-lent :accepted :made-under-a-lent-permission-is-lent]]
      ["a raise under a permission for one sitting adds nothing to a permission that is good in every sitting"
       (fn [] (then (by asha g0 (gives (g 4) (says dee see asha g0)))
                    (by asha g0 (gives (g 5) (says dee owner asha g0 :session :sit-7)))
                    (assoc (by dee (g 5) (gives (g 4) (says dee owner asha g0))) :session :sit-7)
                    (assoc (by dee (g 5) (gives (g 4) (says dee member asha g0))) :session :sit-7)
                    (assoc (by dee (g 4) (note :thing-1 "in another sitting")) :session :sit-9)))
       [:accepted :accepted :permission-covers-more-than-the-acts-own :permission-covers-more-than-the-acts-own
        :permission-does-not-allow-writing]]
      ["a holder lowers her own: lowering needs what ending needs, and she may end it"
       (fn [] (then (by ben (g 1) (gives (g 1) (says ben see asha g0)))))
       [:accepted]]
      ["raising or lowering someone leaves her in throughout and ends nothing she lent: what she lent is capped by what she holds at that moment, and uncapped when she is raised again"
       (fn [] (let [write (fn [text] (by ada (g 2) (note :thing-1 text)))]
                [(then (write "before") lower (write "while she is lowered") raise (write "after she is raised"))
                 (sees (apply after (conj setup lower)) ben)
                 (sees (apply after (conj setup lower)) ada)
                 (says-at (apply after (conj setup lower)) (g 2))]))
       [[:accepted :accepted :permission-does-not-allow-writing :accepted :accepted]
        :yes :yes
        (says ada see-write ben (g 1) :lent-from (g 1))]]
      ["not built, and refused: a change of what a permission covers or of its sitting; and a change that changes nothing"
       (fn [] (then (by asha g0 (gives (g 1) (says ben member asha g0 :session :sit-1)))
                    (by asha g0 (gives (g 1) (says ben member asha g0 :covers [:exact-act (apply str (repeat 64 "0"))])))
                    (by asha g0 (gives (g 1) (says ben member asha g0)))))
       [:changing-what-a-permission-covers-not-built :changing-what-a-permission-covers-not-built
        :change-changes-nothing]]])))

(deftest owners-and-the-hand-over
  (let [settings (fn [who permission]
                   (by who permission {:e lane :k :softland.store/who-may-read-acts :v :everyone}))]
    (holds
     [["owning can be given, any owner writes the lane's settings, and nobody else does"
       (fn [] (verdicts (by asha g0 (gives (g 1) (says ben owner asha g0)))
                        (by asha g0 (gives (g 2) (says cal member asha g0)))
                        (settings ben (g 1))
                        (settings asha g0)
                        (settings cal (g 2))))
       [:accepted :accepted :accepted :accepted :only-an-owner-writes-a-setting]]
      ["owning holds everything over a lane, giving to everyone among it: a permission that allows owning and less than everything is no permission, at a give, at a change and in a making act"
       (fn [] (let [without-everyone (assoc owner :give #{:see :write :give :owner})]
                [(verdicts (by asha g0 (gives (g 1) (says ben {:owner true} asha g0)))
                           (by asha g0 (gives (g 1) (says ben (dissoc owner :see) asha g0)))
                           (by asha g0 (gives (g 1) (says ben (assoc owner :give #{:see :write :give}) asha g0)))
                           (by asha g0 (gives (g 1) (says ben without-everyone asha g0)))
                           (by asha g0 (gives g0 (says asha {:owner true} asha nil))))
                 (:verdicts (played [(by asha nil
                                         {:e lane :k :softland.store/depot :v :streaming}
                                         (gives g0 (says asha without-everyone asha nil)))]))
                 (first-act/making-act? :streaming
                                        (by asha nil
                                            {:e lane :k :softland.store/depot :v :streaming}
                                            (gives g0 (says asha without-everyone asha nil))))]))
       [[:permission-fact-not-a-permission :permission-fact-not-a-permission
         :permission-fact-not-a-permission :permission-fact-not-a-permission
         :permission-fact-not-a-permission]
        [:permission-fact-not-a-permission]
        false]]
      ["an act that would leave a lane with no owner's permission that stands on its own is refused: by an end, and by a change that takes owning away"
       (fn [] (verdicts (by asha g0 (ends g0))
                        (by asha nil (ends g0))
                        (by asha g0 (gives g0 (says asha member asha nil)))))
       [:lane-would-have-no-owner :lane-would-have-no-owner :lane-would-have-no-owner]]
      ["a whole lane is handed over in one act, which gives the new owner hers and ends the old owner's; the lane's first permission ends like any other"
       (fn [] (let [hand-over (by asha g0 (gives (g 1) (says ben owner asha g0)) (ends g0))
                    record (after hand-over)]
                [(verdicts hand-over (settings ben (g 1)) (settings asha g0))
                 (sees record ben) (sees record asha)
                 (get (:owners record) (str lane))]))
       [[:accepted :accepted :permission-ended] :yes :may-not-see #{(g 1)}]]
      ["one owner ends another owner's permission while an owner remains, and the last owner's does not end"
       (fn [] (verdicts (by asha g0 (gives (g 1) (says ben owner asha g0)))
                        (by ben (g 1) (ends g0))
                        (by ben (g 1) (ends (g 1)))))
       [:accepted :accepted :lane-would-have-no-owner]]
      ["an owner's permission that is lent does not keep the lane: its lender's end would take it away"
       (fn [] (verdicts (by asha g0 (gives (g 1) (says ben owner asha g0 :lent-from g0)))
                        (by asha g0 (ends g0))
                        (by asha g0 (gives (g 2) (says ada owner asha g0 :lent-from g0)))
                        (by asha g0 (ends g0))))
       [:accepted :lane-would-have-no-owner :accepted :lane-would-have-no-owner]]
      ["a hand-over to a member leaves no owner and is refused; and the set of owners that a gate keeps follows each give, change and end"
       (fn [] (let [acts [(by asha g0 (gives (g 1) (says ben member asha g0)) (ends g0))
                          (by asha g0 (gives (g 1) (says ben owner asha g0)))
                          (by asha g0 (gives (g 1) (says ben member asha g0)))
                          (by asha g0 (gives (g 1) (says ben owner asha g0)))
                          (by ben (g 1) (ends g0))]]
                [(apply verdicts acts) (get (:owners (apply after acts)) (str lane))]))
       [[:lane-would-have-no-owner :accepted :accepted :accepted :accepted] #{(g 1)}]]])))

;; ------------------------------------ batch 4: public as a give to everyone

(deftest seen-by-everyone-is-a-permission
  (let [open (by asha g0 (gives (g 1) (says :anyone see asha g0)))
        open-to-write (by asha g0 (gives (g 2) (says :anyone-signed-in see-write asha g0)))
        everyone (fn [record] [(sees record nil) (sees record zed) (sees record ada) (sees record :system)])]
    (holds
     [["a lane is seen by everyone under a permission whose holder is anyone at all: by one who is not signed in and by any person, and by no agent, run or system on its own; ending it closes the lane"
       (fn [] [(everyone (after)) (everyone (after open)) (everyone (after open (by asha g0 (ends (g 1)))))])
       [[:may-not-see :may-not-see :may-not-see :may-not-see]
        [:yes :yes :may-not-see :may-not-see]
        [:may-not-see :may-not-see :may-not-see :may-not-see]]]
      ["anyone at all holds seeing and nothing more, at the give and at a change"
       (fn [] (verdicts (by asha g0 (gives (g 1) (says :anyone see-write asha g0)))
                        (by asha g0 (gives (g 1) (says :anyone {:see true :give #{:see}} asha g0)))
                        open
                        (by asha g0 (gives (g 1) (says :anyone see-write asha g0)))))
       [:anyone-only-sees :anyone-only-sees :accepted :anyone-only-sees]]
      ["anyone signed in writes: a person acts under that permission as one who holds it, one who is not signed in does not see by it, and an agent is not everyone"
       (fn [] [(verdicts open-to-write
                         (by zed (g 2) (note :thing-1 "a stranger writes"))
                         (by ada (g 2) (note :thing-1 "an agent on its own")))
               (sees (after open-to-write) zed) (sees (after open-to-write) nil) (sees (after open-to-write) ada)])
       [[:accepted :accepted :who-not-the-permissions-holder] :yes :may-not-see :may-not-see]]
      ["everyone holds and never asks: a read for an asker that is everyone, or that is no party, is shown nothing"
       (fn [] (let [record (after open open-to-write)]
                [(sees record :anyone-signed-in) (sees record :anyone) (sees record :asha) (sees record zed)]))
       [:may-not-see :may-not-see :may-not-see :yes]]
      ["everyone holds and never acts: an act whose who is everyone is refused for its form"
       (fn [] (verdicts open-to-write (by :anyone-signed-in (g 2) (note :thing-1 "x")) (by :anyone (g 2) (note :thing-1 "x"))))
       [:accepted :who-not-a-party :who-not-a-party]]
      ["an agent comes in through its person: a person lends everyone's permission to her agent, and to no other person"
       (fn [] (let [acts [open
                          (by zed (g 1) (gives (g 3) (says ada see zed (g 1) :lent-from (g 1))))
                          (by zed (g 1) (gives (g 4) (says dee see zed (g 1) :lent-from (g 1))))]]
                [(apply verdicts acts) (sees (apply after acts) ada)
                 (sees (apply after (conj acts (by asha g0 (ends (g 1))))) ada)]))
       [[:accepted :accepted :permission-gives-more-than-its-giver-may] :yes :may-not-see]]
      ["being covered by everyone makes nobody the holder of everyone's permission: a stranger does not end another person's agent's borrow, and its holder, its giver and an owner do"
       (fn [] (let [borrow (fn [n] (by zed (g 1) (gives (g n) (says ada see zed (g 1) :lent-from (g 1)))))]
                (verdicts open (borrow 3) (borrow 4) (borrow 5)
                          (by dee (g 1) (ends (g 3)))
                          (by ada nil (ends (g 3)))
                          (by zed (g 1) (ends (g 4)))
                          (by asha g0 (ends (g 5))))))
       [:accepted :accepted :accepted :accepted :may-not-end-the-permission :accepted :accepted :accepted]]
      ["giving or raising a permission held by everyone needs :everyone in the giver's right to give, beside the right passed; inviting a person does not, and :everyone in :give is passed on only with both :give and :everyone"
       (fn [] (verdicts (by asha g0 (gives (g 3) (says ben member asha g0)))
                        (by ben (g 3) (gives (g 4) (says cal see ben (g 3))))
                        (by ben (g 3) (gives (g 5) (says :anyone see ben (g 3))))
                        (by ben (g 3) (gives (g 5) (says :anyone-signed-in see ben (g 3))))
                        (by ben (g 3) (gives (g 5) (says dee publisher ben (g 3))))
                        (by asha g0 (gives (g 5) (says :anyone-signed-in see asha g0)))
                        (by ben (g 3) (gives (g 5) (says :anyone-signed-in see-write asha g0)))
                        (by asha g0 (gives (g 6) (says dee {:see true :give #{:everyone}} asha g0)))
                        (by dee (g 6) (gives (g 7) (says :anyone see dee (g 6))))
                        (by dee (g 6) (gives (g 7) (says cal {:see true :give #{:everyone}} dee (g 6))))
                        (by asha g0 (gives (g 8) (says ben publisher asha g0)))
                        (by ben (g 8) (gives (g 5) (says :anyone-signed-in see-write asha g0)))
                        (by ben (g 8) (gives (g 9) (says :anyone see ben (g 8))))
                        (by ben (g 8) (gives (g 10) (says cal publisher ben (g 8))))
                        (by asha g0 (gives (g 8) (says ben member asha g0)))
                        (by ben (g 8) (ends (g 9)))))
       [:accepted :accepted
        :permission-gives-more-than-its-giver-may
        :permission-gives-more-than-its-giver-may
        :permission-gives-more-than-its-giver-may
        :accepted
        :permission-gives-more-than-its-giver-may
        :accepted
        :permission-gives-more-than-its-giver-may
        :permission-gives-more-than-its-giver-may
        :accepted :accepted :accepted :accepted
        :accepted :may-not-end-the-permission]]
      ["a maker opens her lane as she makes it: the making act holds the depot, the first permission, and a permission of seeing to anyone at all under it"
       (fn [] (let [opening (fn [who value]
                              (by who nil
                                  {:e lane :k :softland.store/depot :v :streaming}
                                  (gives g0 (says asha owner who nil))
                                  (gives (g 1) value)))
                    one (fn [maker] (played [maker]))]
                [(:verdicts (one (opening asha (says :anyone see asha g0))))
                 (sees (:record (one (opening asha (says :anyone see asha g0)))) nil)
                 (:verdicts (one (opening asha (says :anyone see-write asha g0))))
                 (:verdicts (one (opening asha (says ben member asha g0))))
                 (:verdicts (one (opening :system (says :anyone see :system g0))))]))
       [[:accepted] :yes [:lane-not-made] [:lane-not-made] [:lane-not-made]]]])))

(deftest no-access-from-a-setting-or-from-code
  (let [base first-act/base
        base-act (fn [who permission & facts]
                   (assoc (apply by who permission facts) :lane base))
        base-first (first-act/first-permission-id base)
        base-says (fn [holder allows giver under]
                    (assoc (says holder allows giver under) :covers [:default-group base]))
        make-base (base-act :system nil
                            {:e base :k :softland.store/depot :v :microbatch}
                            (gives base-first (base-says :system owner :system nil)))
        open-base (base-act :system base-first
                            (gives (first-act/first-anyone-id base) (base-says :anyone see :system base-first)))
        base-sees (fn [record who] (memory/may-see record base who nil))]
    (holds
     [["the base is the system's, as today: its making act is taken only with the system as its first holder"
       (fn [] [(:verdicts (played [make-base]))
               (:verdicts (played [(base-act asha nil
                                             {:e base :k :softland.store/depot :v :microbatch}
                                             (gives base-first (base-says asha owner asha nil)))]))])
       [[:accepted] [:lane-not-made]]]
      ["what a lane starts with is in the log: the base is seen by nobody but its holder until an act gives seeing to anyone at all, and no value in code opens it"
       (fn [] (let [made (:record (played [make-base]))
                    opened (:record (played [make-base open-base]))]
                [(base-sees made nil) (base-sees made zed) (base-sees made :system)
                 (base-sees opened nil) (base-sees opened zed)
                 (first-act/starts-with base :softland.store/who-may-see)]))
       [:may-not-see :may-not-see :yes :yes :yes nil]]
      ["the ids that a lane starts with are made of the lane's id alone, the same in every process, and are UUIDs of version 8"
       (fn [] [(= (first-act/first-permission-id :lab) (first-act/first-permission-id :lab))
               (not= (first-act/first-permission-id :lab) (first-act/first-anyone-id :lab))
               (not= (first-act/first-permission-id :lab) (first-act/first-permission-id :lab2))
               (.version ^UUID (first-act/first-permission-id :lab))
               (.variant ^UUID (first-act/first-anyone-id :lab))])
       [true true true 8 2]]])))

;; ------------------------------------------------ what a decision reads and writes

(deftest what-a-decision-reads-and-writes
  (holds
   [["a give puts the permission in the order by holder with the decision's stamp, an in-place change writes the new decision's stamp there, an end takes it out, and a permission for one sitting has its sitting in the key"
     (fn [] (let [k1 (record/by-holder-key lane ben nil (g 1))
                  k2 (record/by-holder-key lane ben :sit-7 (g 2))
                  acts [(by asha g0 (gives (g 1) (says ben see asha g0)))
                        (by asha g0 (gives (g 2) (says ben see asha g0 :session :sit-7)))]
                  r-given (apply after acts)
                  r-changed (apply after (conj acts (by asha g0 (gives (g 1) (says ben member asha g0)))))
                  r-ended (apply after (conj acts
                                             (by asha g0 (gives (g 1) (says ben member asha g0)))
                                             (by ben nil (ends (g 1)))))]
              [(vec (keys (:by-holder r-given)))
               (get (:by-holder r-given) k1)
               (get (:by-holder r-changed) k1)
               (vec (keys (:by-holder r-ended)))]))
     [[(record/by-holder-key lane asha nil g0)
       (record/by-holder-key lane ben nil (g 1))
       (record/by-holder-key lane ben :sit-7 (g 2))]
      1
      3
      [(record/by-holder-key lane asha nil g0)
       (record/by-holder-key lane ben :sit-7 (g 2))]]]
    ["the use of a permission for one exact act is a row with no value that no fact wrote, where an end is a row that a fact wrote"
     (fn [] (let [the-act (by ben nil (note :thing-1 "the one act"))
                  record (after (by asha g0 (gives (g 1) (says ben {:write :all} asha g0
                                                               :covers [:exact-act (digest/hex-for-a-yes the-act)])))
                                (assoc the-act :permission (g 1))
                                (by asha g0 (gives (g 2) (says ben see asha g0)))
                                (by ben nil (ends (g 2))))
                  use (get (:facts record) (chain/place lane (g 1)))
                  end (get (:facts record) (chain/place lane (g 2)))]
              [(record/used? use) (record/ended? use) (contains? use :by-index)
               (record/ended? end) (record/used? end) (contains? end :by-index)
               (keys (:by-holder record))]))
     [true false false true false true [(record/by-holder-key lane asha nil g0)]]]
    ["the walk asks for the rows above a permission one at a time, and reads nothing above a permission that its holder or its sitting refuses"
     (fn [] (let [record (after (by asha g0 (gives (g 1) (says ben member asha g0)))
                                (by ben (g 1) (gives (g 2) (says ada see-write ben (g 1) :lent-from (g 1))))
                                (by ada (g 2) (gives (g 3) (says bot see-write ada (g 2) :lent-from (g 2)))))
                  reads-of (fn [maker]
                             (loop [state (walk/begin :streaming (arrived 99 maker))
                                    asked []]
                               (if-let [reads (seq (walk/reads state))]
                                 (recur (walk/resume state (into {} (map (fn [r] [r (memory/read-one record r)])) reads))
                                        (into asked (map (fn [[which k]] (if (= :fact which) [which (peek (record/place-of-key k))] [which])))
                                              reads))
                                 asked)))]
              [(reads-of (by bot (g 3) (ends (g 3))))
               (reads-of (by cal (g 3) (ends (g 3))))]))
     [[[:answer]
       [:fact :softland.store/permission] [:fact :softland.store/permission] [:fact :softland.store/permission]
       [:last-stamp]]
      [[:answer] [:fact :softland.store/permission] [:last-stamp]]]]
    ["the count of the lane's owners is read only for an act that takes an owner away and makes none"
     (fn [] (let [record (after (by asha g0 (gives (g 1) (says ben owner asha g0))))
                  asks-owners? (fn [maker]
                                 (loop [state (walk/begin :streaming (arrived 99 maker))
                                        asked? false]
                                   (if-let [reads (seq (walk/reads state))]
                                     (recur (walk/resume state (into {} (map (fn [r] [r (memory/read-one record r)])) reads))
                                            (or asked? (boolean (some #(= :owners (first %)) reads))))
                                     asked?)))]
              [(asks-owners? (by ben (g 1) (ends g0)))
               (asks-owners? (by ben (g 1) (ends g0) (gives (g 2) (says cal owner ben (g 1)))))
               (asks-owners? (by ben (g 1) (gives (g 2) (says cal member ben (g 1)))))]))
     [true false false]]
    ["a gate keeps a refused thing at the larger bound only for a sender whom a permission names: on a lane that is open to everyone, a large act that is refused is not kept a second time, and one that is taken is"
     (fn [] (let [large (fn [who permission]
                          (apply by who permission
                                 (for [n (range 6)] (note (keyword (str "thing-" n)) (apply str (repeat 60000 "x"))))))
                  kept (fn [& makers]
                         (let [{:keys [record outcomes]} (played (cons making makers))
                               stamp (:stamp (peek outcomes))]
                           [(:verdict (peek outcomes)) (contains? (:things record) (record/line-key lane stamp))]))]
              [(kept (by asha g0 (gives (g 1) (says :anyone see asha g0))) (large zed (g 1)))
               (kept (by asha g0 (gives (g 1) (says ben see asha g0))) (large ben (g 1)))
               (kept (by asha g0 (gives (g 1) (says :anyone-signed-in see-write asha g0))) (large zed (g 1)))
               (kept (large zed (g 9)))]))
     [[:permission-does-not-allow-writing false]
      [:permission-does-not-allow-writing true]
      [:accepted true]
      [:permission-never-given false]]]
    ["a count of owners that a reader gives as nothing reads as none, and throws nothing; and a making act that is refused is not said to make its lane"
     (fn [] (let [record (after)
                  driven (loop [state (walk/begin :streaming (arrived 99 (by asha g0 (ends g0))))]
                           (if-let [reads (seq (walk/reads state))]
                             (recur (walk/resume state (into {} (map (fn [r] [r (when-not (= :owners (first r))
                                                                                    (memory/read-one record r))]))
                                                                  reads)))
                             (walk/outcome state)))
                  refused-making (:outcome (memory/decide memory/empty-record :streaming
                                                          (arrived 0 (update-in making [:facts 0] assoc
                                                                                :replaces [[lane :streaming (UUID. 0 7)] 0]))))]
              [(:verdict driven) (select-keys refused-making [:verdict :makes?])]))
     [:lane-would-have-no-owner {:verdict :replaced-fact-not-current :makes? false}]]
    ["a party's kind is read from its id, and everyone is a holder that is no author"
     (fn [] [(mapv act/party-kind [asha ada :run.r1 :system :anyone :anyone-signed-in :asha :person. :softland/person.x "person.x"])
             (mapv act/author-party? [asha ada :system :anyone])
             (mapv act/holder-party? [asha :anyone :anyone-signed-in :asha])])
     [[:person :agent :run :system :everyone :everyone nil nil nil nil]
      [true true true false]
      [true true true false]]]
    ["lane-rights snapshots read :anyone first on :see, distinguish :holder, :visitor and :outside on :acts, return :too-many-permissions at the 32 holder-scan or chain bound, and gate live standing on entry-for-entry read-set stamp equality"
     (fn []
       (let [run-snapshot (fn [record question who session]
                            (loop [st (read-lanes/rights-begin lane question who session)]
                              (if-let [rs (seq (read-lanes/rights-reads st))]
                                (recur (read-lanes/rights-resume st (into {} (map (fn [r] [r (memory/read-one record r)])) rs)))
                                (read-lanes/rights-snapshot st))))
             open-rec (after (by asha g0 (gives (g 1) (says :anyone see asha g0))))
             see-asha (run-snapshot open-rec :see asha :sit-1)
             see-anon (run-snapshot open-rec :see nil nil)
             acts-asha (run-snapshot open-rec :acts asha :sit-1)
             acts-zed (run-snapshot open-rec :acts zed :sit-1)
             acts-ada (run-snapshot open-rec :acts ada :sit-1)
             flood-rec (apply after (for [i (range 1 34)]
                                      (by asha g0 (gives (g i) (says ada {:write :all} asha g0 :lent-from g0)))))
             flood-snap (run-snapshot flood-rec :see ada :sit-1)
             own-src (read-sources/own-rows-source lane lane)
             known-match {own-src {(record/place-key lane lane :softland.store/depot)
                                   (get (:facts open-rec) (record/place-key lane lane :softland.store/depot))}
                          (read-sources/holders-source lane lane (str lane " :anyone * ") (str lane " :anyone *!"))
                          {(record/by-holder-key lane :anyone nil (g 1)) 1}
                          (read-sources/permission-row-source lane lane (chain/place lane (g 1)))
                          (get (:facts open-rec) (chain/place lane (g 1)))}
             known-stale (assoc-in known-match
                                   [(read-sources/permission-row-source lane lane (chain/place lane (g 1))) :stamp]
                                   99)
             {:keys [ok]} (read-query/check {:asks [:place :thing-1 :note] :asker zed :lanes [lane]})]
         [(= see-asha see-anon)
          (:standing see-asha)
          (:standing acts-asha)
          (:standing acts-zed)
          (:standing acts-ada)
          (:standing flood-snap)
          (read-sources/lane-standing lane lane :live {lane see-asha} known-match)
          (read-sources/lane-standing lane lane :live {lane see-asha} known-stale)
          (:refuse (read-query/check {:asks [:place :thing-1 :note] :asker :anyone :lanes [lane]}))
          (:refuse (read-query/check {:asks [:place :thing-1 :note] :asker :asha :lanes [lane]}))
          (read-lanes/valid-rights-query? lane lane :see :anyone nil)
          (read-lanes/valid-rights-query? lane lane :see :anyone-signed-in nil)
          (read-lanes/valid-rights-query? lane lane :see :asha nil)
          (:who-may-read-acts acts-zed)
          (some? ok)]))
     [true :yes :holder :visitor :outside :too-many-permissions :yes :not-known-yet
      :asker-not-a-party :asker-not-a-party false false false :members true]]]))

(defn test-ns-hook
  "Runs the checks in the order in which the proof tells them."
  []
  (a-permission-has-an-id-of-its-own)
  (a-holder-has-any-number-of-permissions)
  (never-given-is-answered-apart-from-ended)
  (a-holder-ends-her-own)
  (what-batch-1-keeps-as-today)
  (lent-or-standing)
  (give-only-what-you-hold)
  (the-four-who-may-end)
  (one-act-is-one-moment)
  (a-change-keeps-the-id)
  (owners-and-the-hand-over)
  (seen-by-everyone-is-a-permission)
  (no-access-from-a-setting-or-from-code)
  (what-a-decision-reads-and-writes))

(defn -main
  "Runs the proof and exits: status 0 when each check holds. It takes no
  argument."
  [& _]
  (let [{:keys [fail error] :as result} (run-tests 'softland.store.permission-proof)]
    (println)
    (println (select-keys result [:test :pass :fail :error]))
    (shutdown-agents)
    (System/exit (if (zero? (+ fail error)) 0 1))))
