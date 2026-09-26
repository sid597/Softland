(ns rig.store.micro-test
  "The micro store on an in-process cluster (PLAN-micro-store.md §F, and its
  'Namespaces and tests' where §F is silent): one cluster, seeded on both
  sides (stage 1's `seed!` for the one-owner layers; persons through the
  placeholder seam; the base on the stream gate and the group here, the
  base re-classed first), then the plan's cases as `testing` blocks. Where
  the model has the same history (`formal.model/run` under `baseline`,
  `formal.scenarios` for the A and D cases), its answer is asked and
  compared; a difference is reported, never hidden.

  Synchronisation is on the frontier (M15): an offer is settled when
  micro-lookup answers it, which is when every task has committed its
  batch. Crashes are injected through `rig.store.inject` (R3); tests assert
  'at least once', never an exact replay count (R4).

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.walk]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [formal.model :as fm]
            [formal.scenarios :as fs]
            [rig.store.client :as c]
            [rig.store.envelope :as env]
            [rig.store.inject :as inject]
            [rig.store.locks :as locks]
            [rig.store.micro-client :as mc]
            [rig.store.module :as m]))

;; ================================================================ helpers

(defn- say [& xs] (apply println "OBSERVED" xs) (flush))

(defn- bytes->vec
  "Data with every byte array as a vector of its bytes (a byte array
  compares by identity)."
  [x]
  (clojure.walk/postwalk #(if (bytes? %) (vec %) %) x))

(defn- wait-until
  ([f] (wait-until f 60000))
  ([f timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop []
       (let [v (try (f) (catch Exception _ nil))]
         (if (or v (> (System/currentTimeMillis) deadline))
           v
           (do (Thread/sleep 50) (recur))))))))

(def root [:group :group :group])
(defn- gp [p] [p :group :group root])
(defn- sp [s p] [s :group :group (gp p)])
(def base-root [:operator :base :base])
(defn- bp [p] [p :base :base base-root])

(defn- model-answer
  "The model's answer to the i-th offer people or the operator sent in the
  history: :yes, a reason, or nil."
  [history i]
  (let [st (fm/run fm/baseline history)
        nm (nth (distinct (:client-sent st)) i)
        a (first (fm/answers-for st nm))]
    (when a (if (= :yes (:answer a)) :yes (:reason a)))))

(def m-note {:e :e0 :k :note :replaces :none :mark #{}})
(defn- m-offer [& {:as more}] [:offer (merge {:who :alice :layer :group :facts [m-note] :stood-on nil :times 1} more)])

(defn- faces-with [st nm] (mc/faces-of st nm))

;; ============================================================ the cluster

(deftest micro-store
  (inject/reset-all!)
  (with-open [ipc (rtest/create-ipc)]
    (let [tasks (or (some-> (System/getenv "RIG_MICRO_TASKS") Long/parseLong) (rand-nth [2 4 8]))
          _ (rtest/launch-module! ipc m/Store {:tasks tasks :threads 2 :workers 1})
          st (mc/connect ipc)
          mn (:module-name st)
          pause! #(rtest/pause-microbatch-topology! ipc mn "micro")
          resume! #(rtest/resume-microbatch-topology! ipc mn "micro")
          write! (fn [spec & opts] (apply mc/write! st spec opts))
          alice (fn [s facts & {:as more}] (merge {:who :alice :layer :group :session s :permission (sp s :alice) :facts facts} more))
          rows-of (fn [e nm] (mc/open-act st e nm))
          ;; two entities on two different tasks, when there are two
          ents (vec (take 64 (for [i (range)] (keyword (str "e" i)))))
          task-of (memoize (fn [e] (mc/task-of st e)))
          [ea eb] (or (first (for [a ents b ents :when (not= (task-of a) (task-of b))] [a b])) [:e0 :e1])]
      (say "tasks" tasks "entities on two tasks" ea eb (task-of ea) (task-of eb))

      ;; ------------------------------------------------------------ the seed
      (testing "seed: the one-owner world, persons, the base on the stream gate, a stream-era base fact, then the group"
        (is (every? #(= :yes (:answer %)) (c/seed! st)))
        ;; wave 1: persons are phase 2's :people acts; the seed made Alice and Bob
        (doseq [p [:carol :dan]] (is (= :yes (:answer (mc/make-person! st p))) (str "made " p)))
        (is (some? (:lock (mc/person-entry st :alice))))
        (let [base (mc/make-base! st)]
          (is (every? #(= :yes (:answer %)) base) "made, its root granted in the making act, Alice's and Bob's beneath it")
          (is (= {:kind :base :owner :operator :class :by-layer :grain :per-value} (c/settings st :base)))
          (is (= mc/base-making-name (first (:granted (c/permission st :base base-root)))) "the base's first facts have constant ids (§C)")))
      (let [base-offer (c/build {:who :alice :layer :base :class :by-layer :permission (bp :alice)
                                 :facts [{:e :eb0 :k :note :v {:token "stream-era"}}]})
            base-answer (c/offer! st base-offer)
            base-fid [(:name base-offer) 0]
            base-row-before (foreign-select-one [(keypath :base :log (:name base-offer) 0)] (:layers st))]
        (testing "a base offer on the stream gate, before the first group, is admitted there (default R8)"
          (is (= :yes (:answer base-answer))))
        (testing "the first group re-classes the base, then is made here standing on the re-class (§C, M6)"
          (let [g (mc/make-group! st :group (:group mc/shared-world))]
            (is (= :yes (get-in g [:reclass :answer :answer])))
            (is (= :yes (get-in g [:made :answer])))
            (is (= [[(get-in g [:reclass :offer :name]) 0]] (keys (:stood-on (:offer g)))) "it stood on the re-class fact")
            (is (< (get-in g [:reclass :answer :stamp]) (get-in g [:made :stamp])) "stamped after what it stood on")
            (is (= {[(get-in g [:reclass :offer :name]) 0] (get-in g [:reclass :answer :stamp])} (mc/stood-on-of st (:name (:offer g))))))
          (is (= {:kind :group :class :by-entity :grain :per-value} (dissoc (mc/settings-of st :group) :batch :owner)))
          (is (= #{:alice :bob} (set (keys (mc/members-of st :group)))))
          (is (some? (:granted (:micro (mc/permission-of st :group root)))))
          (is (= :by-entity (:class (c/settings st :base))) "the base is by entity from its re-class on")
          (let [g2 (mc/make-group! st :guild {:members #{:bob} :root [:guild :guild :guild] :grants [[:bob :guild :guild [:guild :guild :guild]]]})]
            (is (nil? (:reclass g2)) "a later group finds the base by entity and skips the re-class")
            (is (= :yes (get-in g2 [:made :answer])))))
        (testing "after the re-class: a :by-layer base name refused :class-mismatch there; a :by-entity one decided here"
          (is (= :class-mismatch (:reason (c/offer! st (c/build {:who :alice :layer :base :class :by-layer :permission (bp :alice)
                                                                 :facts [{:e :eb1 :k :note :v 1}]})))))
          (mc/open-session! st :sb :alice [:base])
          (let [r (write! {:who :alice :layer :base :session :sb :permission [:sb :base :base (bp :alice)]
                           :facts [{:e :eb1 :k :note :v {:token "micro-era"}}]})]
            (is (= :yes (get-in r [:answer :answer])) "a stream-era permission chain, read as settled history (M5)")
            (is (= [{:token "micro-era"}] (map :value (rows-of :eb1 (get-in r [:offer :name])))))))
        (testing "a stream-era base fact replaced here gets its tombstone; the frozen row and head are untouched"
          (let [r (write! {:who :alice :layer :base :session :sb :permission [:sb :base :base (bp :alice)]
                           :facts [{:e :eb0 :k :note :v {:token "replaced here"} :replaces base-fid}]})]
            (is (= :yes (get-in r [:answer :answer])))
            (is (= {:by [(get-in r [:offer :name]) 0]} (dissoc (mc/tombstone-of st :base :eb0 :note base-fid) :batch)))
            ;; wave 1: phase 2's rows carry sealed bytes, a digest and a lock record, and a byte
            ;; array compares by identity, so the two reads compare their bytes as vectors
            (is (= (bytes->vec base-row-before) (bytes->vec (foreign-select-one [(keypath :base :log (:name base-offer) 0)] (:layers st))))
                "the stream-era row is byte-identical before and after the re-class, its lock record included")
            (is (some? (c/head st :base :eb0 :note base-fid)) "the frozen stream head is not deleted (another topology's PState)")
            (is (= :stale-replaces (get-in (write! {:who :alice :layer :base :session :sb :permission [:sb :base :base (bp :alice)]
                                                     :facts [{:e :eb0 :k :note :v {:token "again"} :replaces base-fid}]})
                                            [:answer :reason]))
                "a second replace of it is refused stale"))))

      (mc/open-session! st :s1 :alice [:group])

      ;; ------------------------------------------------------ the micro cases
      (testing "an act on two entities is admitted whole across tasks (I-G6), found by name (I-G4), its rows on both tasks"
        (let [r (write! (alice :s1 [{:e ea :k :note :v {:token "a"}} {:e eb :k :mention :v {:persons #{:bob}}}]))
              o (:offer r)
              rec (mc/lookup st (:name o) (mc/digest-of o) (mc/fp-of o))]
          (is (= :yes (get-in r [:answer :answer])))
          (is (= [:yes nil] ((juxt :answer :reason) rec)))
          (is (= #{:bob} (:subjects rec)) "the act's subject slot: the value's grammar subjects (no owner in a shared layer)")
          (is (= [{:token "a"}] (map :value (rows-of ea (:name o)))))
          (is (= [{:persons #{:bob}}] (map :value (rows-of eb (:name o)))))
          (is (= (:stamp rec) (:stamp (mc/head-of st ea :group :note [(:name o) 0]))))
          (is (= (:batch rec) (:batch (mc/head-of st eb :group :mention [(:name o) 1]))))
          (is (<= (:stamp rec) (mc/clock-of st ea)) "the touched tasks' clocks at or past the stamp")
          (is (= :yes (model-answer [(m-offer)] 0)) "the model admits the same act at its micro gate")
          (testing "a resend in a later batch: answered from the name row with the same stamp, nothing decided again (OP2)"
            (let [again (mc/offer! st o)]
              (is (= (:stamp rec) (:stamp again)))
              (is (= 1 (count (rows-of ea (:name o)))))))
          (testing "other content under the name: :name-taken on its face, the first record untouched (E1 N1)"
            (let [other (mc/seal (mc/build (assoc (alice :s1 [{:e ea :k :note :v {:token "OTHER"}} {:e eb :k :mention :v {:persons #{:bob}}}])
                                                  :name (:name o)))
                                 (fn [vi] (nth (get-in r [:lease :ids]) vi)) (:locks r))
                  a (mc/offer! st other)]
              (is (= [:no :name-taken] ((juxt :answer :reason) a)))
              (is (= :name-taken (wait-until #(get-in (faces-with st (:name o)) [(mc/fp-of other) :reason])))
                  "and the gate's own face under its fingerprint")
              (is (= (:stamp rec) (:stamp (mc/record-of st (:name o)))))
              (let [mt (assoc o :layer :base :permission (bp :alice))
                    a (mc/offer! st mt)]
                (is (= :no (:answer a)))
                (is (= :name-taken (:reason a)) "E1 N2 x tag mismatch: the lookup follows the tag to the first record and says name taken (as the model's lookup)")
                (is (= :mis-tagged (wait-until #(get-in (faces-with st (:name o)) [(mc/fp-of mt) :reason])))
                    "the gate refused it on its face, reading nothing"))
              (is (= (:stamp rec) (:stamp (mc/record-of st (:name o)))) "the record untouched")
              (is (some #(re-find #"taken by other content" %)
                        (:trace (fm/run fm/baseline [(m-offer) [:batch] [:reuse 0 {:who :alice :layer :group :facts [(assoc m-note :e :e1)]}] [:batch]]))))))))

      (testing "an act touching many entities: many tasks, still whole (OP3)"
        (let [es (take 6 ents)
              r (write! (alice :s1 (vec (for [e es] {:e e :k :wide :v {:on e}}))))
              nm (get-in r [:offer :name])]
          (is (= :yes (get-in r [:answer :answer])))
          (is (= (for [e es] [{:on e}]) (for [e es] (map :value (rows-of e nm)))) "every entity's row, one stamp")
          (is (apply = (for [[i e] (map-indexed vector es)] (:stamp (mc/head-of st e :group :wide [nm i])))))))

      (testing "a resend and its original in one batch: decided once (OP3, the same envelope)"
        (let [o (mc/build (alice :s1 [{:e ea :k :note :v nil}]))]
          (pause!) (mc/send! st o) (mc/send! st o) (resume!)
          (let [a (mc/await-answer st o)]
            (is (= :yes (:answer a)))
            (is (= 1 (count (rows-of ea (:name o))))))))

      (testing "two envelopes under one name in one batch ([PV-F3]): the earlier by [uuid7 name fp] decided, the other :name-taken on its face"
        (let [l (mc/lease! st {:who :alice :layer :group :session :s1 :permission (sp :s1 :alice) :n 2})
              ks (mc/take-locks st (:name l))
              [id0 id1] (:ids l)
              o (mc/build (alice :s1 [{:e ea :k :note :v {:token "one"}}]))
              same-ids (mc/seal (assoc-in o [:facts 0 :v] {:token "two"}) (constantly id0) ks)
              one (mc/seal o (constantly id0) ks)
              other-ids (mc/seal (assoc-in o [:facts 0 :v] {:token "three"}) (constantly id1) ks)
              mis-tagged (assoc one :layer :base :permission (bp :alice))
              envs [one same-ids other-ids mis-tagged]
              _ (do (pause!) (doseq [e envs] (mc/send! st e)) (resume!))
              answers (mapv #(mc/await-answer st %) envs)
              plain-of {(mc/fp-of one) {:token "one"} (mc/fp-of same-ids) {:token "two"} (mc/fp-of other-ids) {:token "three"}}
              decided (first (sort (keys plain-of)))
              v (:value (first (rows-of ea (:name o))))]
          (say "same-name envelopes" (mapv (juxt :answer :reason) answers))
          (is (= 1 (count (filter #(= :yes (:answer %)) answers))) "one decided")
          (is (= 2 (count (filter #(and (:face %) (= :name-taken (:reason %))) answers))) "the others hear :name-taken on their faces")
          (is (= [:no :mis-tagged true] ((juxt :answer :reason :face) (last answers))) "E1 N1 x tag mismatch: refused on its face, the original unaffected")
          (is (= (plain-of decided) v) "the rows are the decided envelope's, the lowest fingerprint's (the batch order's tiebreak)")))

      (testing "refused on the face, readable as data through micro-lookup, never the name's answer (M8, E1)"
        (let [base (mc/build (alice :s1 [{:e ea :k :note :v nil}]))
              cases [["a :by-layer name at this depot" (assoc base :name (env/make-name :group :by-layer) :class :by-layer) :wrong-gate]
                     ["the :crossing scheme" (assoc base :name [:group nil :crossing (env/uuid7)] :facts [{:e ea :k :crossed :v {}}]) :reserved-scheme]
                     ["a mis-tagged name" (assoc base :name (env/make-name :base :by-entity)) :mis-tagged]
                     ["who the store" (assoc base :who :store) :reserved-who]
                     ["a plaintext value (phase 2's L27)" (assoc base :facts [{:e ea :k :note :v {:token "plain"}}]) :not-sealed]]]
          (doseq [[what o want] cases]
            (let [a (mc/offer! st o)]
              (is (= [:no want true] ((juxt :answer :reason :face) a)) what)
              (is (nil? (mc/record-of st (:name o))) what)))))

      (testing "recorded refusals: a fact naming another layer; a :by-entity name into :alice before its re-class (from $$layers's class)"
        (let [c0 (mc/clock-of st ea)
              r (write! (alice :s1 [{:e ea :k :note :v nil :layer :base}]))
              a (:answer r)
              o (:offer r)]
          (is (= :fact-outside-the-acts-layer (:reason a)))
          (is (< c0 (:stamp a)) "E1 N0 x refused: the refusal consumed a stamp")
          (is (<= (:stamp a) (mc/clock-of st ea)))
          (is (= [:no :fact-outside-the-acts-layer (:stamp a)] ((juxt :answer :reason :stamp) (mc/offer! st o)))
              "E1 N3 x resend: the same no and stamp")
          (is (= :name-taken (:reason (mc/offer! st (assoc o :claimed-when 1)))) "E1 N3 x other content: a refused first use holds the name")
          (let [mt (assoc o :layer :base :permission (bp :alice))]
            (is (= :name-taken (:reason (mc/offer! st mt))) "E1 N3 x tag mismatch: name taken by the lookup")
            (is (= :mis-tagged (wait-until #(get-in (faces-with st (:name o)) [(mc/fp-of mt) :reason]))) "and on its face by the gate")))
        (let [r (write! (alice :s1 [{:e ea :k :note :v {:token "refused value"}}] :permission (gp :bob)) :lease-permission (sp :s1 :alice))
              o (:offer r)
              other (let [l (mc/lease! st {:who :alice :layer :group :session :s1 :permission (sp :s1 :alice) :n 1})
                          ks (mc/take-locks st (:name l))]
                      (assoc (mc/seal (mc/build (assoc (alice :s1 [{:e ea :k :note :v {:token "other value"}}] :permission (gp :bob)) :name (:name o)))
                                      (constantly (first (:ids l))) ks)
                             :claimed-when (:claimed-when o)))]
          (is (= :permission-does-not-cover-this (get-in r [:answer :reason])))
          (is (= [:no :permission-does-not-cover-this] ((juxt :answer :reason) (mc/offer! st other)))
              "other value content under a refused name hears the recorded no (§J 1: no value digest is kept for a refused act)"))
        (is (= :no-such-layer (:reason (mc/offer! st (mc/build {:who :operator :layer :nowhere :facts [{:e ea :k :note :v nil}]}))))
            "E3 L0 x offer")
        (let [o (mc/build {:who :alice :layer :alice :session nil :permission [:alice :alice :alice] :facts [{:e ea :k :note :v nil}]})]
          (is (= :class-mismatch (:reason (mc/offer! st o))))))

      (testing "chains at this gate: replace, stale, doubled, one good and one stale refused whole (OP4, E2)"
        (let [r1 (write! (alice :s1 [{:e ea :k :tag :v {:token "t1"}}]))
              f1 [(get-in r1 [:offer :name]) 0]
              r2 (write! (alice :s1 [{:e ea :k :tag :v {:token "t2"} :replaces f1}]))
              f2 [(get-in r2 [:offer :name]) 0]]
          (is (= :yes (get-in r2 [:answer :answer])))
          (is (< (get-in r1 [:answer :stamp]) (get-in r2 [:answer :stamp])) "after what it replaces")
          (is (= f2 (:replaced-by (mc/head-of st ea :group :tag f1))) "the replaced head kept, with its replacer (M7)")
          (is (= :stale-replaces (get-in (write! (alice :s1 [{:e ea :k :tag :v {:token "t3"} :replaces f1}])) [:answer :reason])) "already replaced")
          (is (= :stale-replaces (get-in (write! (alice :s1 [{:e ea :k :tag :v nil :replaces f2} {:e eb :k :tag :v nil :replaces f2}])) [:answer :reason])) "doubled")
          (is (= :stale-replaces (get-in (write! (alice :s1 [{:e eb :k :tag :v nil} {:e ea :k :tag :v nil :replaces [(env/make-name :group :by-entity) 0]}]))
                                         [:answer :reason]))
              "one good fact and one stale: refused whole")
          (is (nil? (mc/head-of st eb :group :tag [(env/make-name :group :by-entity) 0]))))
        (testing "E2 C1 x a new fact without replace: two unreplaced facts on one chain, each replaceable once; a retract replaces a head and erases nothing"
          (let [ra (write! (alice :s1 [{:e ea :k :pair :v 1}]))
                rb (write! (alice :s1 [{:e ea :k :pair :v 2}]))
                fa [(get-in ra [:offer :name]) 0]
                fb [(get-in rb [:offer :name]) 0]]
            (is (= [:yes :yes] [(get-in ra [:answer :answer]) (get-in rb [:answer :answer])]))
            (is (= :yes (get-in (write! (alice :s1 [{:e ea :k :pair :v 3 :replaces fa}])) [:answer :answer])))
            (let [rt (write! (alice :s1 [{:e ea :k :pair :v nil :replaces fb}]))]
              (is (= :yes (get-in rt [:answer :answer])) "a retract, replacing the other head")
              (is (= [2] (map :value (rows-of ea (first fb)))) "the replaced value still opens: a retract erases nothing")
              (is (= [nil] (map :value (rows-of ea (get-in rt [:offer :name]))))))
            (is (= :stale-replaces (get-in (write! (alice :s1 [{:e ea :k :pair :v 4 :replaces fb}])) [:answer :reason]))))))

      (testing "a grain switch on a shared layer is the operator's, a new settings version; a person's is refused (OP8, M14)"
        (let [before (count (mc/settings-versions st :group))
              g (mc/offer! st (mc/build {:who :operator :layer :group :facts [{:e :group :k :lock-grain :v :per-act}]}))]
          (is (= :yes (:answer g)))
          (is (= :per-act (:grain (mc/settings-of st :group))))
          (is (= (inc before) (count (mc/settings-versions st :group))) "a new version keyed by the batch, the old one kept")
          (testing "under per act, one lock for the act (L6); a per-value citation is :grain-mismatch, recorded"
            (let [r (write! (alice :s1 [{:e ea :k :note :v 1} {:e eb :k :note :v 2}]))]
              (is (= :yes (get-in r [:answer :answer])))
              (is (= 1 (count (:locks r))))
              (is (= [1 2] (map :value (concat (rows-of ea (get-in r [:offer :name])) (rows-of eb (get-in r [:offer :name])))))))
            (let [r (write! (alice :s1 [{:e ea :k :note :v 3} {:e eb :k :note :v 4}]) :grain :per-value)]
              (is (= :grain-mismatch (get-in r [:answer :reason])))
              (is (empty? (mc/lease-rows st (get-in r [:lease :name]))) "its leases consumed at the recorded refusal")))
          (is (= :control-not-allowed (:reason (mc/offer! st (mc/build (alice :s1 [{:e :group :k :lock-grain :v :per-value}]))))))
          (is (= :yes (:answer (mc/offer! st (mc/build {:who :operator :layer :group :facts [{:e :group :k :lock-grain :v :per-value}]}))))))
        (is (= :unsupported-reclass (:reason (mc/offer! st (mc/build {:who :operator :layer :group :facts [{:e :group :k :class :v :by-layer}]}))))
            "E3 L4 x re-class: not ruled, refused (O9)")
        (is (= :stale-revoke (:reason (mc/offer! st (mc/revoke-offer st [:nobody :group :group root])))) "E4 P0 x revoke"))

      ;; ------------------------------------------------------- permissions (R7)
      (testing "the permission cases at this gate"
        (testing "a permission that does not exist; one that does not cover (Bob citing Alice's); the operator exempt"
          (is (= :no-permission (get-in (write! (alice :s9 [{:e ea :k :note :v 1}]) :lease-who :operator) [:answer :reason]))
              "a session never opened (its lease the operator's, bare)")
          (is (= :permission-does-not-cover-this
                 (get-in (write! (alice :s1 [{:e ea :k :note :v 1}] :permission (gp :bob)) :lease-permission (sp :s1 :alice))
                         [:answer :reason]))
              "Bob's permission cited on Alice's write")
          (is (= :yes (get-in (write! {:who :operator :layer :group :session :s-op :facts [{:e ea :k :note :v {:token "op"}}]}) [:answer :answer]))))
        (testing "D1: a group write under Alice's session permission kept in her hand layer: refused :permission-from-another-layer; the value is missing, as in the model"
          (let [r (write! (alice :s1 [{:e :e0 :k :note :v {:token "d1"}}] :permission [:alice :group :alice-hand])
                          :lease-permission (sp :s1 :alice))
                [_ _ expect] (first fs/d-cases)
                [ok seen _] (fs/play fm/baseline (first fs/d-cases) [])]
            (is (= :permission-from-another-layer (get-in r [:answer :reason])))
            (is (empty? (rows-of :e0 (get-in r [:offer :name]))) "no value landed: :missing")
            (is ok "the model's D1 outcome is as the case says")
            (is (= {:values {[:group :note nil] :missing}} seen expect) "both :missing")))
        (testing "a chain whose ancestor lives in another layer is refused on both gates"
          (is (= :permission-from-another-layer
                 (get-in (write! (alice :s1 [{:e ea :k :note :v 1}] :permission [:s1 :group :group (bp :alice)]) :lease-permission (sp :s1 :alice))
                         [:answer :reason])))
          ;; wave 1 (phase 2's lease road, RIG.md For Sid 14): at the stream gate the door
          ;; leases before it writes, so the chain is walked, and refused, on the lease act;
          ;; the value act, citing no leased lock, is refused on its face
          (let [chain [:sx :alice :alice [:alice :alice-agent :alice-agent]]]
            (is (= :permission-from-another-layer (:reason (c/lease! st :alice :alice :sx 1 chain)))
                "the stream gate walks the same chain, on the lease act it records")
            (is (= :no-such-lock
                   (:reason (c/offer! st (c/build {:who :alice :layer :alice :class :by-layer :session :sx
                                                   :permission chain :facts [{:e :e0 :k :note :v 1}]}))))
                "and the value act that follows is refused on its face (For Sid 14)")))
        (testing "revoke cuts everything below, on the stream gate: root, session, agent (R19, default 5)"
          (let [sroot [:alice :alice :alice]
                sess [:s7 :alice :alice sroot]
                agent [:ag7 :alice :alice sess]
                _ (doseq [p [sess agent]] (is (= :yes (:answer (c/offer-until-answered! st (c/grant-offer st p))))))
                agent-write #(c/offer! st (c/build {:who :alice :layer :alice :class :by-layer :session :ag7 :permission agent
                                                    :facts [{:e :e0 :k :note :v %}]}))]
            (is (= :yes (:answer (agent-write 1))) "the agent writes beneath the session beneath the root")
            (is (= :yes (:answer (c/offer! st (c/revoke-offer st sess)))))
            (is (= :permission-revoked (:reason (agent-write 2))) "the agent's writes refused")
            (is (= :yes (:answer (c/offer! st (c/build {:who :alice :layer :alice :class :by-layer :permission sroot
                                                        :facts [{:e :e0 :k :note :v 3}]}))))
                "the root's still admitted")))
        (testing "a stream-side revoke of her session's permission in her own layer leaves her group permission live (a gate checks only its own layers')"
          (mc/open-session! st :s6 :alice [:alice :group])
          (is (= :yes (:answer (c/offer! st (c/revoke-offer st [:s6 :alice :alice [:alice :alice :alice]])))))
          (is (= :yes (get-in (write! (alice :s6 [{:e ea :k :note :v {:token "s6"}}])) [:answer :answer]))))
        (testing "the revocation race in one batch, by M2's order (OP6): a revoke named before a write refuses it, after it admits it"
          (mc/open-session! st :s4 :bob [:group])
          (mc/open-session! st :s5 :bob [:group])
          (let [bob (fn [s v] (mc/build {:who :bob :layer :group :session s :permission (sp s :bob) :facts [{:e ea :k :note :v v}]}))
                sealed-for (fn [s] (let [l (mc/lease! st {:who :bob :layer :group :session s :permission (sp s :bob) :n 1})
                                           ks (mc/take-locks st (:name l))]
                                       (fn [o] (mc/seal o (constantly (first (:ids l))) ks))))
                seal4 (sealed-for :s4)
                seal5 (sealed-for :s5)
                ;; a name's UUID7 is millisecond-grained (random within the millisecond), so
                ;; the order of two builds is the batch order only a millisecond apart (M2)
                revoke4 (mc/revoke-offer st (sp :s4 :bob))
                w4 (do (Thread/sleep 2) (seal4 (bob :s4 1)))
                w5 (do (Thread/sleep 2) (seal5 (bob :s5 2)))
                revoke5 (do (Thread/sleep 2) (mc/revoke-offer st (sp :s5 :bob)))]
            (pause!) (doseq [o [w4 revoke4 w5 revoke5]] (mc/send! st o)) (resume!)
            (is (= :yes (:answer (mc/await-answer st revoke4))))
            (is (= :permission-revoked (:reason (mc/await-answer st w4))) "revoke named first: the write refused")
            (is (= :yes (:answer (mc/await-answer st w5))) "write named first: admitted")
            (is (= :yes (:answer (mc/await-answer st revoke5))))
            (is (= :permission-revoked (model-answer [[:revoke [:alice :group :own]] (m-offer) [:batch]] 1))
                "the model's micro gate: a revoke then a write in one batch, the write refused")))
        (testing "D2, and a revoke of an ancestor cutting the session beneath it: in the same batch after it, and in the next batch"
          (mc/open-session! st :s2 :alice [:group])
          (let [l (mc/lease! st {:who :alice :layer :group :session :s2 :permission (sp :s2 :alice) :n 2})
                ks (mc/take-locks st (:name l))
                [id0 id1] (:ids l)
                revoke (mc/revoke-offer st (gp :alice))
                same-batch (do (Thread/sleep 2) (mc/seal (mc/build (alice :s2 [{:e :e0 :k :note :v {:token "d2"}}])) (constantly id0) ks))
                _ (do (pause!) (mc/send! st revoke) (mc/send! st same-batch) (resume!))
                a1 (mc/await-answer st revoke)
                a2 (mc/await-answer st same-batch)
                next-batch (mc/offer! st (mc/seal (mc/build (alice :s2 [{:e :e0 :k :note :v {:token "d2 next"}}])) (constantly id1) ks))
                [ok seen _] (fs/play fm/baseline (second fs/d-cases) [])]
            (is (= :yes (:answer a1)))
            (is (= :permission-revoked (:reason a2)) "Alice's group permission revoked earlier in the batch: her session's cut")
            (is (= :permission-revoked (:reason next-batch)) "and in the next batch")
            (is (empty? (rows-of :e0 (:name same-batch))) ":missing")
            (is ok)
            (is (= {:values {[:group :note nil] :missing}} seen) "the model's D2: :missing, as the rig")
            (is (= :permission-revoked (:reason (mc/offer! st (mc/build (alice :s1 [{:e ea :k :note :v nil}])))))
                "every session beneath her revoked permission is cut, with no write to them (R19)")
            (is (= :stale-revoke (:reason (mc/offer! st (mc/revoke-offer st (gp :alice))))) "E4 P2 x revoke again (R18)")
            (is (= :yes (:answer (mc/offer! st (mc/grant-offer (gp :alice))))) "E4 P2 x re-grant: admitted as a fact")
            (is (= :permission-revoked (:reason (mc/offer! st (mc/build (alice :s1 [{:e ea :k :note :v nil}])))))
                "and the first grant's row stands, revoked (P8)"))))

      ;; ------------------------------------------------------------ re-class
      (testing "re-class moves a layer here, and its order promise ends there (OP7, M5)"
        (let [agent-perm [:alice :alice-agent :alice-agent]
              stream-o (c/build {:who :alice :layer :alice-agent :class :by-layer :permission agent-perm
                                 :facts [{:e ea :k :note :v {:token "stream-era"}}]})
              _ (is (= :yes (:answer (c/offer! st stream-o))))
              sfid [(:name stream-o) 0]
              head-before (c/head st :alice-agent ea :note sfid)
              _ (is (= :yes (:answer (c/offer-until-answered! st (mc/reclass-offer st :alice-agent)))))
              agent (fn [facts & {:as more}] (merge {:who :alice :layer :alice-agent :session :sa :permission agent-perm :facts facts} more))]
          (is (= :class-mismatch (:reason (c/offer! st (c/build {:who :alice :layer :alice-agent :class :by-layer :permission agent-perm
                                                                 :facts [{:e ea :k :note :v 1}]}))))
              "its :by-layer names refused on the stream gate")
          (let [r (write! (agent [{:e ea :k :note :v {:token "micro-era"} :replaces sfid}]))]
            (is (= :yes (get-in r [:answer :answer])) "a :by-entity offer decided here, citing a stream-era permission (settled history)")
            (is (= #{:alice} (get-in r [:answer :subjects])) "a one-owner layer's owner is in the subject slot")
            (is (= [:alice] (:required (:lock (second (first (:rows (mc/act st ea (get-in r [:offer :name]) (get-in r [:answer :frontier]))))))))
                "and required in the wrap (owner-required), the lock in the record (an agent layer)")
            (is (= [{:token "micro-era"}] (map :value (rows-of ea (get-in r [:offer :name])))))
            (is (some? (mc/tombstone-of st :alice-agent ea :note sfid)) "the stream-era head replaced here gets its tombstone")
            (is (= head-before (c/head st :alice-agent ea :note sfid)) "the frozen head untouched")
            (is (= :stale-replaces (get-in (write! (agent [{:e ea :k :note :v 1 :replaces sfid}])) [:answer :reason])) "a second replace refused stale")
            (is (= [:no :wrong-gate true]
                   ((juxt :answer :reason :face) (mc/offer! st (mc/build {:who :operator :layer :alice-agent :facts [{:e :alice-agent :k :lock-grain :v :per-act}]}))))
                "a setting of a layer whose settings the stream gate keeps, sent here: refused on its face (P16, M25)"))
          (testing "two offers appended in one order whose UUID7s sort the other way, in one batch, are decided in UUID7 order"
            (let [lease (mc/lease! st {:who :alice :layer :alice-agent :session :sa :permission agent-perm :n 2})
                  ks (mc/take-locks st (:name lease))
                  early (mc/seal (mc/build (agent [{:e eb :k :tag :v 1}])) (constantly (first (:ids lease))) ks)
                  late (do (Thread/sleep 2) (mc/seal (mc/build (agent [{:e eb :k :tag :v 2}])) (constantly (second (:ids lease))) ks))]
              (pause!) (mc/send! st late) (mc/send! st early) (resume!)
              (let [a (mc/await-answer st early) b (mc/await-answer st late)]
                (is (= [:yes :yes] [(:answer a) (:answer b)]))
                (is (< (:stamp a) (:stamp b)) "the earlier UUID7 stamped first on the shared task, though appended second"))))
          (testing "after a micro-side revoke of the stream-era permission, an offer citing it is refused (M5's merge)"
            (is (= :yes (:answer (mc/offer! st (mc/revoke-offer st agent-perm)))))
            (is (= :permission-revoked (get-in (write! (agent [{:e eb :k :note :v 9}]) :lease-who :operator) [:answer :reason]))))))

      ;; --------------------------------------------------------- missing locks
      (mc/offer! st (mc/grant-offer (gp :carol)))
      (mc/open-session! st :s3 :carol [:group])
      (let [carol (fn [facts] {:who :carol :layer :group :session :s3 :permission (sp :s3 :carol) :facts facts})
            face-and-free (fn [what sealed]
                            (let [a (mc/offer! st sealed)]
                              (is (= [:no :no-such-lock true] ((juxt :answer :reason :face) a)) what)
                              (is (nil? (mc/record-of st (:name sealed))) (str what ": nothing recorded under the name"))
                              (let [fresh (write! (assoc (carol (:facts (update-in sealed [:facts] (fn [fs] (mapv #(-> % (dissoc :sealed :lock-id) (assoc :v 1)) fs)))))
                                                         :name (:name sealed) :claimed-when (:claimed-when sealed)))]
                                (is (= :yes (get-in fresh [:answer :answer])) (str what ": the same name under a fresh lease is admitted")))))]
        (testing "a missing lock is refused on its face (phase 2's L27): nothing recorded, nothing consumed, the name free"
          (let [o (mc/build (carol [{:e ea :k :note :v 1}]))
                never [(env/make-name :group :by-entity) 0]]
            (face-and-free "a lock id never leased" (mc/seal o (constantly never) {never (locks/fresh-lock)})))
          (let [r (write! (carol [{:e ea :k :note :v 1}]))
                lid (first (keys (:locks r)))
                o (mc/build (carol [{:e ea :k :note :v 1}]))]
            (face-and-free "a lease already consumed" (mc/seal o (constantly lid) (:locks r))))
          (let [lb (mc/lease! st {:who :operator :layer :base :session :s3 :n 1})
                ks (mc/take-locks st (:name lb))
                o (mc/build (carol [{:e ea :k :note :v 1}]))]
            (face-and-free "a lease made for another layer" (mc/seal o (constantly (first (:ids lb))) ks)))
          (let [lo (mc/lease! st {:who :operator :layer :group :session :someone-else :n 1})
                ks (mc/take-locks st (:name lo))
                o (mc/build (carol [{:e ea :k :note :v 1}]))]
            (face-and-free "a lease of another session" (mc/seal o (constantly (first (:ids lo))) ks))
            (is (= 1 (count (mc/lease-rows st (:name lo)))) "the other session's row left alone")))
        (testing "a lease sealed under a person whose lock is destroyed: :no-such-lock, the offer's bytes open under no lock the store holds"
          (mc/offer! st (mc/grant-offer (gp :dan)))
          (mc/open-session! st :s8 :dan [:group])
          (let [l (mc/lease! st {:who :dan :layer :group :session :s8 :permission (sp :s8 :dan) :n 1})
                ks (mc/take-locks st (:name l))
                _ (is (= :yes (:answer (mc/forget-person! st :dan))) "wave 1: phase 2's forget act")
                o (mc/seal (mc/build {:who :dan :layer :group :session :s8 :permission (sp :s8 :dan) :facts [{:e ea :k :note :v 1}]})
                           (constantly (first (:ids l))) ks)
                a (mc/offer! st o)]
            (is (= [:no :no-such-lock] ((juxt :answer :reason) a)))
            (is (nil? (locks/unlease (get (mc/lease-rows st (:name l)) 0) (mc/person-entry st :dan)))
                "the row no longer unleases")
            (is (= :person-forgotten (get-in (mc/lease! st {:who :dan :layer :group :session :s8 :permission (sp :s8 :dan) :n 1})
                                             [:answer :reason]))
                "and a new lease by her is refused, recorded (phase 2's L11)")))
        (testing "a lease row is consumed in the batch that decides it (M19)"
          (let [r (write! (carol [{:e ea :k :mention :v {:persons #{:bob}}}]))
                lname (get-in r [:lease :name])]
            (is (= :yes (get-in r [:answer :answer])))
            (is (empty? (mc/lease-rows st lname)) "admitted: the row is gone, the lock is the value's now")
            (is (= [{:persons #{:bob}}] (map :value (rows-of ea (get-in r [:offer :name])))) "and the value opens through its row's lock")
            (testing "a resend answered from the record: no cited row exists, nothing written"
              (let [c0 (mc/clock-of st ea)]
                (is (= (get-in r [:answer :stamp]) (:stamp (mc/offer! st (:offer r)))))
                (is (= c0 (mc/clock-of st ea)) "no stamp given"))))
          (let [r (write! (assoc (carol [{:e ea :k :note :v 7}]) :permission [:s3 :group :group (gp :nobody)]) :lease-permission (sp :s3 :carol))]
            (is (= :no-permission (get-in r [:answer :reason])))
            (is (empty? (mc/lease-rows st (get-in r [:lease :name]))) "refused: the row is gone")
            (is (empty? (rows-of ea (get-in r [:offer :name]))) "and nothing opens"))
          (testing "two offers citing one lock in one batch: the first by M2's order uses it, the second :no-such-lock"
            (let [l (mc/lease! st {:who :carol :layer :group :session :s3 :permission (sp :s3 :carol) :n 1})
                  ks (mc/take-locks st (:name l))
                  lid (first (:ids l))
                  a (mc/seal (mc/build (carol [{:e ea :k :note :v "first"}])) (constantly lid) ks)
                  b (do (Thread/sleep 2) (mc/seal (mc/build (carol [{:e eb :k :note :v "second"}])) (constantly lid) ks))]
              (pause!) (mc/send! st b) (mc/send! st a) (resume!)
              (is (= :yes (:answer (mc/await-answer st a))))
              (is (= [:no :no-such-lock true] ((juxt :answer :reason :face) (mc/await-answer st b))))))
          (testing "the lease act is answered by name; its locks readable through micro-lease only once its batch is at or below F"
            (let [l (mc/lease! st {:who :carol :layer :group :session :s3 :permission (sp :s3 :carol) :n 3})
                  b (get-in l [:answer :batch])]
              (is (= 3 (count (mc/take-locks st (:name l)))))
              (is (empty? (:locks (foreign-invoke-query (:lease-q st) (:name l) (dec b)))) "at F below its batch: nothing")
              (is (= 3 (count (:locks (foreign-invoke-query (:lease-q st) (:name l) b)))) "at its batch: the three")
              (is (= [(:name l)] (filter #{(:name l)} (:names (foreign-invoke-query (:leases-of-q st) :s3 nil))))
                  "a restarted door finds its lease again by micro-leases-of")))))

      ;; ------------------------------------- the value check before and after a forget
      (testing "the value check, before and after a forget (R1's rider, [PV-F2])"
        (mc/open-session! st :s11 :bob [:group])
        (let [spec {:who :bob :layer :group :session :s11 :permission (sp :s11 :bob) :facts [{:e ea :k :mention :v {:persons #{:bob}}}]}
              r (write! spec)
              o (:offer r)
              lid (first (keys (:locks r)))
              other-plain (fn [] (assoc (mc/seal (mc/build (assoc spec :facts [{:e ea :k :mention :v {:persons #{:bob :dan}}}]))
                                                 (constantly lid) (:locks r))
                                        :name (:name o) :claimed-when (:claimed-when o)))
              other-parts (fn [] (let [x (write! (assoc spec :facts [{:e eb :k :mention :v {:persons #{:bob}}}]))]
                                   (assoc (:offer x) :name (:name o) :claimed-when (:claimed-when o))))]
          (is (= :yes (get-in r [:answer :answer])))
          (is (= [:no :name-taken] ((juxt :answer :reason) (mc/offer! st (other-plain))))
              "the recorded lock ids with other plaintext: :name-taken before the forget")
          (let [op (other-parts)]
            (is (= [:no :name-taken] ((juxt :answer :reason) (mc/offer! st op))) "other parts under fresh locks: :name-taken by the parts digest")
            (is (= :yes (:answer (mc/forget-person! st :bob))) "wave 1: phase 2's forget act")
            (is (= :yes (:answer (mc/offer! st (other-plain))))
                "after Bob's forget the value is not checked: the recorded answer (the price of forgetting)")
            (is (= [:no :name-taken] ((juxt :answer :reason) (mc/offer! st (assoc op :claimed-when (:claimed-when o)))))
                "other parts: still :name-taken by the parts digest after the forget")
            (is (= [{:erased-at (:erased-at (mc/person-entry st :bob))}] (rows-of ea (:name o)))
                "the value itself reads as erased on the forget's date"))))

      ;; ------------------------------------------------ the door that lost its locks
      (is (= :yes (:answer (mc/make-person! st :erin))))
      (mc/offer! st (mc/grant-offer (gp :erin)))
      (mc/open-session! st :s12 :erin [:group])
      (let [spec {:who :erin :layer :group :session :s12 :permission (sp :s12 :erin) :facts [{:e ea :k :note :v {:token "kept"}}]}
            r (write! spec)
            o (:offer r)
            reseal (fn [plain] (let [l (mc/lease! st {:who :erin :layer :group :session :s12 :permission (sp :s12 :erin) :n 1})
                                     ks (mc/take-locks st (:name l))]
                                 [l (assoc (mc/seal (mc/build (assoc spec :facts [{:e ea :k :note :v plain}])) (constantly (first (:ids l))) ks)
                                           :name (:name o) :claimed-when (:claimed-when o))]))]
        (testing "a door that lost its locks resends under a fresh lease: the same plaintext is answered from the record, its new rows consumed"
          (let [[l again] (reseal {:token "kept"})]
            (is (= (get-in r [:answer :stamp]) (:stamp (mc/offer! st again))))
            (is (empty? (mc/lease-rows st (:name l))))))
        (testing "other plaintext under a fresh lease: :name-taken on its face, its rows left"
          (let [[l again] (reseal {:token "changed"})]
            (is (= [:no :name-taken true] ((juxt :answer :reason :face) (mc/offer! st again))))
            (is (= 1 (count (mc/lease-rows st (:name l)))))))
        (testing "a resend citing another session's lease ids: answered from the record, the other session's rows untouched"
          (let [lo (mc/lease! st {:who :operator :layer :group :session :s13 :n 1})
                ks (mc/take-locks st (:name lo))
                again (assoc (mc/seal (mc/build spec) (constantly (first (:ids lo))) ks) :name (:name o) :claimed-when (:claimed-when o))]
            (is (= :yes (:answer (mc/offer! st again))))
            (is (= 1 (count (mc/lease-rows st (:name lo))))))))

      (testing "a session close in the group: its unconsumed rows gone, a later offer citing one :no-such-lock ([PV-F4])"
        (let [l (mc/lease! st {:who :erin :layer :group :session :s12 :permission (sp :s12 :erin) :n 4})
              ks (mc/take-locks st (:name l))
              close (mc/offer! st (mc/build {:who :erin :layer :group :session :s12 :permission (sp :s12 :erin)
                                             :facts [{:e :s12 :k :session-closed :v {:session :s12}}]}))
              o (mc/seal (mc/build {:who :erin :layer :group :session :s12 :permission (sp :s12 :erin) :facts [{:e ea :k :note :v 1}]})
                         (constantly (first (:ids l))) ks)]
          (is (= :yes (:answer close)))
          (is (empty? (mc/lease-rows st (:name l))))
          (is (= [:no :no-such-lock] ((juxt :answer :reason) (mc/offer! st o))))))

      ;; ---------------------------------------------------- the group's A cases
      (testing "the group's six A cases, both directions (R8), through this gate: the rig's open or erased against the model's"
        (doseq [[i [what _ expect :as case]] (map-indexed vector (subvec fs/a-cases 1 7))]
          (let [a (keyword (str "alice-a" i)) b (keyword (str "bob-a" i)) s (keyword (str "sa" i))
                _ (doseq [p [a b]] (is (= :yes (:answer (mc/make-person! st p)))))
                _ (mc/offer! st (mc/grant-offer (gp b)))
                _ (mc/open-session! st s b [:group])
                ;; the case's one offer by Bob, its value's persons renamed to this case's
                [_ hist _] case
                [_ spec] (first hist)
                f (first (:facts spec))
                persons (set (map {:alice a :bob b} (:mention f)))
                v (if (:mention f) {:persons persons} {:token "note"})
                r (write! {:who b :layer :group :session s :permission (sp s b)
                           :facts [(cond-> {:e (keyword (str "ea" i)) :k (:k f) :v v} (seq (:mark f)) (assoc :mark (:mark f)))]})
                forgets (for [[op p] (drop 1 (drop-while #(not= [:batch] %) hist)) :when (= :forget-person op)] ({:alice a :bob b} p))
                _ (doseq [p forgets] (is (= :yes (:answer (mc/forget-person! st p))) "wave 1: phase 2's forget act"))
                seen (let [[row] (rows-of (keyword (str "ea" i)) (get-in r [:offer :name]))] (if (contains? row :value) :open :erased))
                [ok model-seen _] (fs/play fm/baseline case [])
                want (val (first (:values expect)))]
            (is (= :yes (get-in r [:answer :answer])) what)
            (is ok (str what ": the model as the case says"))
            (is (= want seen (val (first (:values model-seen)))) what)
            (say "A case" i what "rig" seen "model" (val (first (:values model-seen)))))))

      ;; -------------------------------------------------------------- crashes
      (testing "a batch that fails on one task leaves nothing: one-shot throws in block 1, 2a, 2b and 2c (R3, R4, RQ 5)"
        (is (= :yes (:answer (mc/make-person! st :fay))))
        (mc/offer! st (mc/grant-offer (gp :fay)))
        (mc/open-session! st :s14 :fay [:group])
        (doseq [[where point-for] [["block 1, the gather" (fn [o _] [:micro-gather (:name o)])]
                                   ["block 2a, the index writes" (fn [o _] [:micro-2a (:name o)])]
                                   ["block 2b, the rows" (fn [o _] [:micro-2b (:name o)])]
                                   ["block 2c, the leases consumed" (fn [_ l] [:micro-2c (:name l)])]]]
          (let [l (mc/lease! st {:who :fay :layer :group :session :s14 :permission (sp :s14 :fay) :n 2})
                ks (mc/take-locks st (:name l))
                o (mc/seal (mc/build {:who :fay :layer :group :session :s14 :permission (sp :s14 :fay)
                                      :facts [{:e ea :k :note :v {:w where}} {:e eb :k :note :v {:w where}}]})
                           (fn [vi] (nth (:ids l) vi)) ks)
                [point nm] (point-for o l)
                _ (inject/watch! nm)
                _ (inject/arm! point nm)
                seen (atom [])
                reader (future (loop [n 0]
                                 (when (< n 400)
                                   (let [F (try (mc/frontier st) (catch Exception _ nil))
                                         a (when F (try (foreign-invoke-query (:lookup-q st) (:name o) (mc/digest-of o) (mc/fp-of o) F) (catch Exception _ nil)))
                                         ra (when (= :yes (:answer a)) (try (mc/act st ea (:name o) F) (catch Exception _ nil)))
                                         rb (when (= :yes (:answer a)) (try (mc/act st eb (:name o) F) (catch Exception _ nil)))]
                                     (when a (swap! seen conj [(:answer a) (count (:rows ra)) (count (:rows rb))]))
                                     (if (= :yes (:answer a)) nil (do (Thread/sleep 20) (recur (inc n))))))))
                a (mc/offer! st o 120000)]
            @reader
            (is (= :yes (:answer a)) where)
            (is (<= 1 (inject/fired-count point nm)) (str where ": the crash fired"))
            (is (<= 2 (inject/count-of point nm)) (str where ": the code ran at least twice (R4)"))
            (is (= 1 (count (rows-of ea (:name o)))) (str where ": the rows once"))
            (is (= 1 (count (rows-of eb (:name o)))) where)
            (is (= (:stamp a) (:stamp (mc/record-of st (:name o)))) (str where ": one record, one stamp"))
            (is (empty? (mc/lease-rows st (:name l))) (str where ": the lease rows consumed"))
            (is (<= (:batch a) (mc/frontier st)) (str where ": the frontier at or past the batch, so every task committed it"))
            (is (every? (fn [[ans na nb]] (or (not= :yes ans) (= [1 1] [na nb]))) @seen)
                (str where ": a reader through the frontier never saw the act without its rows on both tasks")))))

      ;; ------------------------------------------------ the frontier, run (M15)
      (testing "a reader never sees half a batch through the frontier (M15): continuous acts on two tasks, readers with one F per pass"
        (let [pairs (vec (take 4 (for [a ents b ents :when (and (not= a b) (not= (task-of a) (task-of b)))] [a b])))
              pairs (if (seq pairs) pairs [[:e0 :e1]])
              written (atom [])
              stop (atom false)
              f0 (mc/frontier st)
              writer (future
                       (loop [i 0]
                         (when-not @stop
                           (let [[x y] (nth pairs (mod i (count pairs)))
                                 o (mc/build {:who :operator :layer :group :facts [{:e x :k :tick :v nil} {:e y :k :tick :v nil}]})]
                             (mc/send! st o)
                             (swap! written conj [x y (:name o)])
                             (Thread/sleep 5)
                             (recur (inc i))))))
              violations (atom 0) passes (atom 0) backward (atom 0) raw-pairs (atom 0) raw-bad (atom 0)
              f-pairs (atom 0) f-bad (atom 0)
              readers (vec (for [r (range 4)]
                             (future
                               (loop [last-F -1]
                                 (when-not @stop
                                   (let [F (mc/frontier st r)
                                         sample (take-last 20 @written)]
                                     (when (< F last-F) (swap! backward inc))
                                     (doseq [[x y nm] sample]
                                       (let [va (some? (:record (mc/act st x nm F)))
                                             vb (some? (:record (mc/act st y nm F)))]
                                         (swap! passes inc)
                                         (when (not= va vb) (swap! violations inc)))
                                       ;; the same reader bypassing the frontier
                                       (let [ra (foreign-select-one [(keypath x :answers nm)] (:micro st))
                                             rb (foreign-select-one [(keypath y :answers nm)] (:micro st))]
                                         (swap! raw-pairs inc)
                                         (when (not= (some? ra) (some? rb)) (swap! raw-bad inc)))
                                       ;; phase 0's measure: two tasks' own frontiers read one after the other
                                       (let [fa (mc/frontier st x) fb (mc/frontier st y)]
                                         (swap! f-pairs inc)
                                         (when (< fb fa) (swap! f-bad inc))))
                                     (recur (max F last-F))))))))]
          (wait-until #(<= 300 (- (mc/frontier st) f0)) 240000)
          (reset! stop true)
          @writer (doseq [r readers] @r)
          (let [batches (- (mc/frontier st) f0)]
            (say "frontier run:" batches "batches," (count @written) "acts," @passes "frontier pairs," @violations "violations,"
                 @backward "backward frontiers;" @raw-pairs "raw pairs," @raw-bad "raw inconsistent pairs;"
                 @f-pairs "raw frontier pairs," @f-bad "where the later read was older (on" tasks "tasks)")
            (is (<= 300 batches) "ran for at least 300 batches")
            (is (zero? @violations) "zero: an act visible on one entity's task is visible on the other's at the same F")
            (is (zero? @backward) "no later read used an older frontier")
            (is (pos? @passes))
            ;; the readers bypassing the frontier are reported, not asserted: whether a
            ;; half-visible batch is caught depends on Rama's commit timing and the task
            ;; count (phase 0 caught 5,822 of 410,155 on four tasks), not on this module
            (when (and (zero? @raw-bad) (zero? @f-bad))
              (say "bypassing the frontier, no half-visible batch was caught this run"))))))))
