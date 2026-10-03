(ns rig.store.forget-test
  "Stage 2 on an in-process cluster (PLAN-locks-and-forgetting.md, 'Namespaces
  and tests', forget_test 1 to 9): locks and forgetting through the module,
  every value act through the door. One cluster, seeded with the model's
  world (the store layer, Alice and Bob, three of Alice's layers, their
  grants) and the base (default 6).

  A person forget is irreversible and global, so each A case and each case
  that forgets someone runs with persons and layers of its own (the model's
  world renamed); the A cases are compared with formal.model's answers to
  the same histories under `baseline`, A2 to A7 in the base, the stream-gated
  layer under shared-layer rules, as the model's `:group` cases ([V-F2]).
  Crashes are injected through `rig.store.inject` (R3, P14) and come last.
  Tests assert 'at least once', never an exact replay count (R4).

  Dates: every date is compared with the stamp the forget's answer carries,
  exactly; one block runs under simulated time to show a person forget's
  date is its stamp at the gate's wall (the plan asks for simulated time so
  dates are exact; the crash blocks run on the real clock, as phase 1's do)."
  (:require [clojure.test :refer [deftest is testing]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [formal.model :as fm]
            [formal.scenarios :as fs]
            [rig.store.client :as c]
            [rig.store.toy-grammars :as tg]
            [rig.store.clock :as hlc]
            [rig.store.envelope :as env]
            [rig.store.inject :as inject]
            [rig.store.locks :as l]
            [rig.store.module :as m])
  (:import [com.rpl.rama.helpers TopologyUtils]
           [java.util Arrays UUID]))

;; ------------------------------------------------------------------ helpers

(defn- say [& xs] (apply println "OBSERVED" xs))

(defn- wait-until
  ([f] (wait-until f 60000))
  ([f timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop []
       (let [v (try (f) (catch Exception _ nil))]
         (if (or v (> (System/currentTimeMillis) deadline))
           v
           (do (Thread/sleep 50) (recur))))))))

(defn- retrying
  "A foreign read, retried while a worker restarts after an injected crash."
  [f]
  (let [deadline (+ (System/currentTimeMillis) 60000)]
    (loop []
      (let [r (try [:ok (f)] (catch Exception e [:err e]))]
        (cond (= :ok (first r)) (second r)
              (> (System/currentTimeMillis) deadline) (throw (second r))
              :else (do (Thread/sleep 100) (recur)))))))

(defn- bytes= [a b] (and (bytes? a) (bytes? b) (Arrays/equals ^bytes a ^bytes b)))

(defn- note [e v] {:e e :k :note :v v})
(defn- mention [e ps & [mark]] (cond-> {:e e :k :mention :v {:persons (set ps)}} mark (assoc :mark #{mark})))

(defn- act
  "`who`'s act into `layer`, citing [who layer layer] unless the operator."
  [who layer facts & {:as more}]
  (c/build (merge {:who who :layer layer :class :by-layer
                   :permission (when-not (= :operator who) [who layer layer])
                   :facts facts}
                  more)))

(defn- raw-send!
  "Append a record as it is, past the door's sealing: the gate's answer."
  [st raw]
  (get (foreign-append! (:depot st) raw :ack) "gate"))

(defn- fid [a i] [(:name a) (long i)])

(defn- as-of [st layer T] (retrying #(c/read-as-of st layer T)))
(defn- now [st layer] (as-of st layer Long/MAX_VALUE))
(defn- fact-in [r id] (some #(when (= id (:id %)) %) (:facts r)))
(defn- state-of [st layer id]
  (let [f (fact-in (now st layer) id)]
    (cond (contains? f :value) :open (contains? f :erased-at) :erased :else [:missing f])))

(defn- held-of
  "The locks and ids the door holds for an offer it has sealed and not yet
  had answered."
  [st o]
  (get-in @(:door st) [:held o]))

(defn- sealed-under
  "The offer with its value facts sealed as given: `ks` maps a fact index
  to [lock-id K], so a test can resend under the recorded lock, or cite a
  lock the door would not."
  [o ks]
  (assoc o :facts (vec (map-indexed (fn [i f]
                                      (if-let [[id K] (get ks i)]
                                        (-> f (dissoc :v) (assoc :sealed (l/seal K (l/canonical-bytes (:v f))) :lock-id id))
                                        f))
                                    (:facts o)))))

(defn- person! [st p]
  (let [a (c/make-person! st p)]
    (is (= :yes (:answer a)) (str "made " p))
    a))

(defn- layer!
  "Make a one-owner layer owned by `owner` and grant the owner's own
  permission in it."
  [st layer kind owner]
  (is (= :yes (:answer (c/offer-until-answered! st (c/make-layer-offer layer {:kind kind :owner owner})))) (str "made " layer))
  (is (= :yes (:answer (c/offer-until-answered! st (c/grant-offer st [owner layer layer])))) (str "granted " owner " in " layer))
  ;; phase 6: the toy grammars as facts in the layer (D-P4)
  (is (every? #(= :yes (:answer %)) (tg/write! st [layer])) (str "grammars in " layer)))

(defn- grant! [st pid]
  (is (= :yes (:answer (c/offer-until-answered! st (c/grant-offer st pid)))) (str "granted " (pr-str pid))))

(defn- on-every-task
  "A person's entry as every task holds it."
  [st p keys]
  (mapv #(retrying (fn [] (c/person-on-task st p %))) keys))

(defn- lease-ids-of [st layer session] (set (map first (c/lease-rows st layer session))))

;; P6 widened: every lock the store still holds, and whether any opens a value

(defn- live-persons [st ps]
  (into {} (for [p ps :let [e (c/person st p)] :when e] [p e])))

(defn- held-locks
  "Every lock the store still holds that could open a value (plan, 'P6
  widened'): each lease row, lock row and record lock of `layers`,
  unwrapped with every live person lock, and the live person locks
  themselves."
  [st layers ps]
  (let [persons (live-persons st ps)
        lps (:layers st)
        person-locks (keep :lock (vals persons))
        lease-ks (for [layer layers
                       s (foreign-select [(keypath layer :leases) MAP-KEYS] lps)
                       [_ row] (c/lease-rows st layer s)
                       :let [K (l/unlease row (get persons (:under row)))]
                       :when K]
                   K)
        row-ks (for [layer layers [_ rec] (c/lock-rows st layer) :let [K (l/unwrap rec persons)] :when K] K)
        record-ks (for [layer layers
                        n (foreign-select [(keypath layer :log) MAP-KEYS] lps)
                        r (c/raw-rows st layer n)
                        :let [K (some-> (:lock r) (l/unwrap persons))]
                        :when K]
                    K)]
    (vec (concat person-locks lease-ks row-ks record-ks))))

(defn- depot-copies
  "The sealed bytes of fact `idx` in every depot record under name `nm`."
  [st nm idx]
  (for [r (c/depot-records st)
        :when (and (map? r) (= nm (:name r)))
        :let [f (nth (:facts r) idx nil)]]
    f))

(defn- p6-check!
  "P6 widened, the three places (plan, 'P6 widened'), for a forgotten value
  `id` in `layer` with plaintext `v`: its depot copies (the records under
  its name, or only those citing `carried`, the lock ids that carried this
  value) and its log bytes open under no lock the store holds; no held lock
  reproduces its value digest; the depot records carry no plaintext value."
  [st layer id v layers ps what & [carried]]
  (let [[nm idx] id
        held (held-locks st layers ps)
        copies (cond->> (depot-copies st nm idx) carried (filter #(contains? carried (:lock-id %))))
        row (c/raw-row st layer id)
        plain (l/canonical-bytes v)]
    (say what ": locks the store holds" (count held) ", depot copies of the value" (count copies))
    (is (seq copies) (str what ": the value went through the depot"))
    (is (every? #(and (bytes? (:sealed %)) (not (contains? % :v))) copies)
        (str what ": 1. the depot holds sealed bytes and no plaintext value"))
    (is (not-any? (fn [cp] (some #(l/open % (:sealed cp)) held)) copies)
        (str what ": 1. no held lock opens the depot's bytes"))
    (is (not-any? #(l/open % (:sealed row)) held) (str what ": 2 and 3. no held lock opens the log's bytes"))
    (is (not-any? #(bytes= (:digest row) (l/value-digest % plain)) held)
        (str what ": 3. no held lock reproduces the value digest from the plaintext"))
    (is (not (bytes= (:sealed row) plain)) (str what ": the log row's bytes are not the value"))))

;; the model's A cases, as data (see rig.store.lock-test)

(defn- case-offers [[_ history _]] (for [[op spec] history :when (= :offer op)] spec))
(defn- case-forgets [[_ history _]] (vec (for [[op p] history :when (= :forget-person op)] p)))
(defn- model-values [c] (:values (second (fs/play fm/baseline c []))))

(defn- model-closer
  "Which of a case's forgets closes a fact's wrap in the model: its index
  in the case's forgets, or nil."
  [layer f forgets]
  (let [w (fm/wrap :owner-required (get-in fm/layers [layer :owner])
                   (fm/fact-subjects layer {:k (:k f) :v {:persons (:mention f)}})
                   (contains? (:mark f) :die-with-any))]
    (some-> (fm/wrap-closed w (into {} (map-indexed (fn [i p] [p {:order (inc i)}])) forgets)) :order dec)))

;; ---------------------------------------------------------------- the test

(deftest locks-and-forgetting
  (inject/reset-all!)
  (with-open [ipc (rtest/create-ipc)]
    (let [tasks (rand-nth [2 4 8])
          _ (rtest/launch-module! ipc m/Store {:tasks tasks :threads 2 :workers 1})
          st (c/connect ipc)
          send! (fn [o] (c/offer! st o))
          rec (fn [nm] (retrying #(c/record st nm)))
          clock (fn [layer] (retrying #(c/clock st layer)))
          task-keys (rtest/gen-hashing-index-keys tasks)
          all-persons (atom #{:alice :bob})
          person+ (fn [p] (swap! all-persons conj p) (person! st p))]
      (say "tasks" tasks)

      (testing "seed: the store layer, the persons, the model's layers and grants; the base with its writers' grants"
        (let [answers (c/seed! st)
              base (c/seed-base! st)]
          (is (= 13 (count answers)))
          (is (every? #(= :yes (:answer %)) (concat answers base)))
          ;; phase 6: the toy grammars as facts in the seeded layers and the base (D-P4)
          (is (every? #(= :yes (:answer %)) (tg/write! st [:alice :alice-hand :alice-agent :base])))
          (is (= {:kind :store :class :by-layer :grain :per-value} (c/settings st :people)) "the store layer has no owner")
          (is (= {:kind :base :owner :operator :class :by-layer :grain :per-value} (c/settings st :base))
              "the base is made with the root actor as its owner, not refused for want of a person lock")
          (doseq [p [:alice :bob]]
            (is (every? #(and (l/lock? (:lock %)) (nil? (:erased-at %))) (on-every-task st p task-keys))
                (str p "'s lock on every task")))
          (person+ :carol)))

      (testing "the door seals: ciphertext in the depot and the log, the value back only through its lock (8, the ciphertext claim)"
        (let [o (act :alice :alice [(note :e0 {:token "v1"}) (mention :e1 [:bob])] :subjects #{:carol})
              a (send! o)
              rows (c/raw-rows st :alice (:name o))
              depot (filter #(= (:name o) (:name %)) (c/depot-records st))]
          (is (= :yes (:answer a)))
          (is (every? #(and (nil? (:v %)) (bytes? (:sealed %)) (bytes? (:digest %)) (l/lock-id? (:lock-id %))) rows)
              "each value row: no plaintext slot, the sealed bytes, the lock id, the value digest")
          (is (not (bytes= (:sealed (first rows)) (l/canonical-bytes {:token "v1"}))) "the row's bytes are not the value's EDN")
          (is (= 1 (count depot)))
          (is (every? #(and (contains? % :sealed) (not (contains? % :v))) (:facts (first depot))) "the depot record: sealed facts, no :v")
          (is (bytes= (:sealed (first rows)) (:sealed (first (:facts (first depot))))) "the log keeps the sealed bytes as offered")
          (is (= [{:e :e0 :k :note :v {:token "v1"} :replaces nil :mark #{}}
                  {:e :e1 :k :mention :v {:persons #{:bob}} :replaces nil :mark #{}}]
                 (c/facts st :alice (:name o)))
              "opened by the store through each value's lock")
          (is (= {:value {:token "v1"}} (c/opens? st :alice (fid a 0))))
          (is (= #{:alice :bob :carol} (:subjects (rec (:name o))))
              "the act's subject slot: the owner, the grammar's (Bob), what it carried (Carol) (ruling 8, L19)")
          (is (every? #(some? (c/lock-row st :alice (:lock-id %))) rows) "a lock row per value in a personal layer")))

      (testing "the missing lock, then the resend (2)"
        (let [o (act :alice :alice-hand [(note :e0 "hand note")])
              sess (:session o)
              _ (c/stock! st :alice :alice-hand sess 8)
              leased-before (lease-ids-of st :alice-hand sess)
              fake [[:alice-hand :by-layer :offer (UUID/randomUUID)] 0]
              a1 (send! (sealed-under o {0 [fake (l/fresh-lock)]}))]
          (is (= {:answer :no :reason :no-such-lock :stamp nil :name (:name o)} a1) "an id never leased: refused on its face")
          (is (nil? (rec (:name o))) "no answer record under the name")
          (is (= [] (c/raw-rows st :alice-hand (:name o))) "no row")
          (is (= leased-before (lease-ids-of st :alice-hand sess)) "no lease row touched")
          (let [a2 (send! o)]
            (is (= :yes (:answer a2)) "the same name resent, sealed under a leased lock: decided fresh")
            (is (= :yes (:answer (rec (:name o))))))
          (testing "an id leased to another session: the same face refusal, and that session's row stays"
            (let [other :s-other
                  _ (c/stock! st :alice :alice-hand other 2)
                  [[oid oK]] (vec (:locks (c/lease-locks st :alice-hand other)))
                  o2 (act :alice :alice-hand [(note :e1 "under another session")])]
              (is (= :no-such-lock (:reason (send! (sealed-under o2 {0 [oid oK]})))))
              (is (contains? (lease-ids-of st :alice-hand other) oid) "the other session's row is still there")))
          (testing "an id consumed by an earlier offer: the same"
            (let [used (:lock-id (first (c/raw-rows st :alice-hand (:name o))))
                  o3 (act :alice :alice-hand [(note :e2 "a consumed lock")])]
              (is (= :no-such-lock (:reason (send! (sealed-under o3 {0 [used (l/fresh-lock)]})))))))))

      (testing "lease consumption, three ways (3)"
        (testing "admitted: the lease row is gone; the lock is the value's lock row (personal) or in the row's record (agent)"
          (let [o (act :alice :alice [(note :e3 "consumed")])
                sess (:session o)
                s (c/sealed st o)
                id (:lock-id (first (:facts s)))
                _ (is (contains? (lease-ids-of st :alice sess) id) "the lease row stands before the decision")
                a (send! o)]
            (is (= :yes (:answer a)))
            (is (not (contains? (lease-ids-of st :alice sess) id)) "consumed at the decision")
            (is (some? (c/lock-row st :alice id)) "the value's lock row, under the same id")
            (let [oa (act :alice :alice-agent [(note :e3 "consumed in the agent layer")])
                  sa (c/sealed st oa)
                  ida (:lock-id (first (:facts sa)))
                  _ (send! oa)
                  row (first (c/raw-rows st :alice-agent (:name oa)))]
              (is (not (contains? (lease-ids-of st :alice-agent (:session oa)) ida)))
              (is (nil? (c/lock-row st :alice-agent ida)) "no lock row in an agent layer")
              (is (= ida (:lock-id row)))
              (is (= [:alice] (:required (:lock row))) "the lock in the record, wrapped under the owner"))))
        (testing "a recorded refusal (the permission revoked between the lease and the offer): the lease row is gone, no lock kept"
          (let [pid [:carol :alice-agent :alice-agent]
                _ (grant! st pid)
                o (act :carol :alice-agent [(note :e4 "after the revoke")])
                sess (:session o)
                _ (c/stock! st :carol :alice-agent sess 4)
                s (c/sealed st o)
                id (:lock-id (first (:facts s)))
                _ (is (= :yes (:answer (c/offer-until-answered! st (c/revoke-offer st pid)))))
                a (send! o)]
            (is (= :permission-revoked (:reason a)))
            (is (= :permission-revoked (:reason (rec (:name o)))) "recorded under the name")
            (is (not (contains? (lease-ids-of st :alice-agent sess) id)) "a recorded no destroys its lease")
            (is (= [] (c/raw-rows st :alice-agent (:name o))))
            (is (nil? (c/lock-row st :alice-agent id)))))
        (testing "answered from the record: the recorded answer, the lock row unchanged, no stamp given; the resend's own lease consumed"
          (let [o (act :alice :alice [(note :e5 "resent")])
                sess (:session o)
                _ (c/sealed st o)
                [[id K]] (vec (:keys (held-of st o)))
                a (send! o)
                lr (c/lock-row st :alice id)
                c1 (clock :alice)]
            (is (= a (send! (sealed-under o {0 [id K]}))) "resent under the recorded lock: the recorded answer")
            (is (= c1 (clock :alice)) "no stamp given")
            (is (= (select-keys lr [:required :any-of]) (select-keys (c/lock-row st :alice id) [:required :any-of])))
            (is (bytes= (:blob lr) (:blob (c/lock-row st :alice id))) "the lock row unchanged")
            (let [s2 (c/sealed st o)
                  id2 (:lock-id (first (:facts s2)))]
              (is (not= id id2) "a resend after the answer is sealed under a lock the door still holds")
              (is (= a (send! o)))
              (is (= c1 (clock :alice)))
              (is (not (contains? (lease-ids-of st :alice sess) id2)) "the record path consumed the resend's own lease ([V-F1])")))))

      (testing "a lease resent (6): the same lock ids, one set of lease rows, the same lock bytes"
        (let [lease (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                              :session :s-lease :facts [{:e :s-lease :k :lease :v {:count 4}}]})
              a1 (send! lease)
              k1 (:locks (c/lease-locks st :alice :s-lease))
              a2 (send! lease)
              k2 (:locks (c/lease-locks st :alice :s-lease))]
          (is (= :yes (:answer a1)))
          (is (= (l/lease-ids (:name lease) 4) (:lock-ids a1)))
          (is (= a1 a2) "the resend: the same answer, stamp and lock ids")
          (is (= 4 (count (c/lease-rows st :alice :s-lease))))
          (is (= (set (keys k1)) (set (keys k2)) (set (:lock-ids a1))))
          (is (every? (fn [[id K]] (bytes= K (get k2 id))) k1) "lease-locks returns the same bytes both times")
          (is (= :name-taken (:reason (send! (assoc-in lease [:facts 0 :v :count] 8)))) "another count under the name: taken")))

      (testing "a resend with other content, before and after a forget (5)"
        (let [o (act :alice :alice-hand [(note :e6 "the first content")])
              _ (c/sealed st o)
              [[id K]] (vec (:keys (held-of st o)))
              a (send! o)
              other (assoc-in o [:facts 0 :v] "other content")
              c1 (clock :alice-hand)]
          (is (= :yes (:answer a)))
          (is (= {:answer :no :reason :name-taken :stamp nil :name (:name o)} (send! (sealed-under other {0 [id K]})))
              "other plaintext under the same lock: the value digest differs")
          (is (= a (send! (sealed-under o {0 [id K]}))) "the same plaintext sealed again, new bytes: the recorded yes")
          (is (= c1 (clock :alice-hand)) "nothing decided again")
          (let [f (c/forget-value! st :alice :alice-hand (fid a 0))]
            (is (= [:yes :row-deleted] [(:answer f) (:how f)]))
            (is (= a (send! (sealed-under other {0 [id K]}))) "after the forget, other content answers as a retry: the price of forgetting")
            (is (= a (send! (sealed-under o {0 [id K]}))) "and the same content too"))))

      (testing "a resend from a door that lost its locks ([V-F1])"
        (let [o (act :alice :alice [(note :e7 "lost pool")] :session :s-vf1)
              sess (:session o)
              _ (c/sealed st o)
              [[id _]] (vec (:keys (held-of st o)))
              a (send! o)
              st2 (c/connect ipc)
              _ (is (not (contains? (:locks (c/lease-locks st2 :alice sess)) id)) "lease-locks never returns a consumed lock")
              s2 (c/sealed st2 o)
              id2 (:lock-id (first (:facts s2)))
              a2 (c/offer! st2 o)]
          (is (not= id id2) "the restarted door seals under another lock")
          (is (= a a2) "the recorded answer and stamp")
          (is (not (contains? (lease-ids-of st :alice sess) id2)) "the new lease row deleted")
          (let [st3 (c/connect ipc)
                other (assoc-in o [:facts 0 :v] "lost pool, other content")
                s3 (c/sealed st3 other)
                id3 (:lock-id (first (:facts s3)))]
            (is (= :name-taken (:reason (c/offer! st3 other))) "the same resend with other content: taken")
            (is (contains? (lease-ids-of st :alice sess) id3) "a name taken consumes nothing: the new row is still there"))
          (let [f (c/forget-value! st :alice :alice (fid a 0))
                st4 (c/connect ipc)
                s4 (c/sealed st4 o)
                id4 (:lock-id (first (:facts s4)))]
            (is (= :yes (:answer f)))
            (is (= a (c/offer! st4 o)) "after the forget: the recorded answer")
            (is (not (contains? (lease-ids-of st :alice sess) id4)) "the resend's lease row deleted")
            (p6-check! st :alice (fid a 0) "lost pool" [:alice :alice-hand :alice-agent :base] @all-persons
                       "a forgotten value resent under new locks" #{id id2 id4})
            (is (<= 3 (count (filter #(contains? #{id id2 id4} (:lock-id %)) (depot-copies st (:name o) 0))))
                "its copies: the first send and two resends (a restarted door may take the lock the other-content resend left unconsumed, whose copy then dies with it)"))
          (testing "the record answer replayed after a later offer's crash: the same answer"
            (let [attempt (fn []
                            (let [x (act :alice :alice [(note :e8 "replayed record")])
                                  ax (send! x)
                                  y (act :alice :alice [(note :e8 "crashes after its writes")])]
                              (inject/watch! (:name x))
                              (is (= ax (send! x)) "answered from the record")
                              (inject/arm! :after-writes (:name y))
                              (try (send! y) (catch Exception _ nil))
                              (wait-until #(map? (c/lookup st (:name y) (env/offer-digest y))) 120000)
                              {:ax ax :again (c/offer-until-answered! st x) :recorded (inject/count-of :recorded (:name x))}))
                  results (loop [i 0 acc []]
                            (let [acc (conj acc (attempt))]
                              (if (or (<= 3 (:recorded (peek acc))) (>= i 4)) acc (recur (inc i) acc))))]
              (say "record path replay: times the resent offer was answered from its record" (mapv :recorded results))
              (doseq [{:keys [ax again]} results]
                (is (= ax again) "the same answer after the crash"))
              (is (some #(<= 3 (:recorded %)) results)
                  "in some attempt the record answer was replayed by the crash, and answered the same (R4: at least once)")))
          (c/forget-door! st2)))

      (testing "value forgets, their ledger and their reach (8, the 25 September cases, and P6 in three places)"
        (testing "in a personal layer: the row deleted, the ledger dated, erased at every moment after its stamp, the rest open"
          (let [o1 (act :alice :alice [(note :e10 "forget me")])
                o2 (act :alice :alice [(note :e11 "keep me")])
                a1 (send! o1)
                a2 (send! o2)
                id (fid a1 0)
                lid (:lock-id (c/raw-row st :alice id))
                head-before (c/head st :alice :e10 :note id)
                _ (inject/record-purges!)
                f (c/forget-value! st :alice :alice id)
                purged @inject/purges]
            (is (= [:yes :row-deleted] [(:answer f) (:how f)]))
            (is (nil? (c/lock-row st :alice lid)) "the lock row deleted")
            (is (= {:stamp (:stamp f) :how :row-deleted} (get (c/ledger st :alice) lid)) "the ledger's date is the forget's stamp")
            (is (= {:erased-at (:stamp f)} (c/opens? st :alice id)) "erased as of its own stamp, before the forget's moment")
            (is (= {:erased-at (:stamp f)} (select-keys (fact-in (now st :alice) id) [:erased-at :value])))
            (is (= {:value "keep me"} (c/opens? st :alice (fid a2 0))) "another value of the layer opens")
            (is (= head-before (c/head st :alice :e10 :note id)) "heads untouched: the fact keeps its place (I-L8)")
            (is (= [{:task (:task (first purged)) :layer :alice :fid id :forget-stamp (:stamp f)}] purged)
                "the purge seam got the value, from the forget's own event")
            (is (= :yes (:answer (send! (act :alice :alice [(assoc (note :e10 "replaced after its forget") :replaces id)]))))
                "a forgotten value can still be replaced")
            (p6-check! st :alice id "forget me" [:alice :alice-hand :alice-agent :base] @all-persons "value forget, personal")
            (testing "a second forget of the same value: yes, nothing changes, the first date stays"
              (let [f2 (c/forget-value! st :alice :alice id)]
                (is (= [:yes nil] [(:answer f2) (:how f2)]))
                (is (= {:stamp (:stamp f) :how :row-deleted} (get (c/ledger st :alice) lid)))))
            (testing "a forget resent answers the same, its :how included (read back from the ledger)"
              (let [a3 (send! (act :alice :alice [(note :e10 "forget me too")]))
                    id3 (fid a3 0)
                    fo (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                                 :stood-on {id3 (:stamp a3)} :facts [{:e :e10 :k :forget :v {:target id3}}]})
                    f3 (send! fo)]
                (is (= :row-deleted (:how f3)))
                (is (= f3 (send! fo)) "the recorded answer, with the :how of the erasure it made")))))
        (testing "in an agent layer: the record's lock excised, the ledger :excised, no lock row before or after"
          (let [o (act :alice :alice-agent [(note :e12 "agent value")])
                a (send! o)
                id (fid a 0)
                lid (:lock-id (c/raw-row st :alice-agent id))
                _ (is (nil? (c/lock-row st :alice-agent lid)))
                f (c/forget-value! st :alice :alice-agent id)]
            (is (= [:yes :excised] [(:answer f) (:how f)]))
            (is (nil? (:lock (c/raw-row st :alice-agent id))) "the row's :lock excised")
            (is (= {:stamp (:stamp f) :how :excised} (get (c/ledger st :alice-agent) lid)))
            (is (nil? (c/lock-row st :alice-agent lid)))
            (is (= {:erased-at (:stamp f)} (c/opens? st :alice-agent id)))
            (p6-check! st :alice-agent id "agent value" [:alice :alice-hand :alice-agent :base] @all-persons "value forget, agent")
            (let [f2 (c/forget-value! st :alice :alice-agent id)]
              (is (= [:yes nil] [(:answer f2) (:how f2)]) "an excised value forgotten again: yes, nothing changes (E5 K4)")
              (is (= {:stamp (:stamp f) :how :excised} (get (c/ledger st :alice-agent) lid)) "the first date stays"))))
        (testing "in a hand layer: a lock row, deleted"
          (let [a (send! (act :alice :alice-hand [(note :e13 "hand value")]))
                id (fid a 0)
                lid (:lock-id (c/raw-row st :alice-hand id))]
            (is (some? (c/lock-row st :alice-hand lid)))
            (is (= :row-deleted (:how (c/forget-value! st :alice :alice-hand id))))
            (is (nil? (c/lock-row st :alice-hand lid)))))
        (testing "an :own-row value in an agent layer gets a lock row (D9)"
          (let [a (send! (act :alice :alice-agent [(assoc (note :e14 "own row") :mark #{:own-row})]))
                lid (:lock-id (c/raw-row st :alice-agent (fid a 0)))]
            (is (some? (c/lock-row st :alice-agent lid)))
            (is (nil? (:lock (c/raw-row st :alice-agent (fid a 0)))))
            (is (= :row-deleted (:how (c/forget-value! st :alice :alice-agent (fid a 0)))))))
        (testing "refused forgets and forgets that change nothing"
          (let [a (send! (act :alice :alice [(note :e15 "Bob cannot forget this")]))
                id (fid a 0)
                bob-forget (c/build {:who :bob :layer :alice :class :by-layer :permission [:alice :alice :alice]
                                     :stood-on {id (:stamp a)} :facts [{:e :e15 :k :forget :v {:target id}}]})
                hand (send! (act :alice :alice-hand [(note :e15 "in another layer")]))]
            (is (= :permission-does-not-cover-this (:reason (send! bob-forget))) "a forget by Bob of Alice's value")
            (is (= {:value "Bob cannot forget this"} (c/opens? st :alice id)))
            (is (= :no-such-value (:reason (c/forget-value! st :alice :alice [(env/make-name :alice :by-layer) 0])))
                "an unknown target")
            (is (= :no-such-value (:reason (c/forget-value! st :alice :alice (fid hand 0)))) "a target in another layer")
            (let [grant-name (first (:granted (c/permission st :alice [:alice :alice :alice])))
                  f (c/forget-value! st :operator :alice [grant-name 0])]
              (is (= [:yes nil] [(:answer f) (:how f)]) "a forget of a control fact: yes, nothing changes")
              (is (= :yes (:answer (send! (act :alice :alice [(note :e15 "the grant still stands")]))))))))
        (testing "a forget by a person whose permission is revoked is refused :permission-revoked, as the model does (L17, O13)"
          (person+ :jo)
          (layer! st :jo :personal :jo)
          (let [a (send! (act :jo :jo [(note :e0 "jo's note")]))
                _ (is (= :yes (:answer (c/offer-until-answered! st (c/revoke-offer st [:jo :jo :jo])))))
                f (c/forget-value! st :jo :jo (fid a 0))]
            (is (= :permission-revoked (:reason f)))
            (is (= :permission-revoked (:reason (rec (:name f)))) "recorded")
            (is (= {:value "jo's note"} (c/opens? st :jo (fid a 0))) "the value still opens")
            (is (= :yes (:answer (c/forget-value! st :operator :jo (fid a 0)))) "the operator can still forget it")))
        (testing "per-act grain: one lock for the act, a forget erases it whole, earlier values keep theirs"
          (person+ :pia)
          (layer! st :pia :personal :pia)
          (let [before (send! (act :pia :pia [(note :e0 "before the switch")]))
                sw (send! (act :pia :pia [{:e :pia :k :lock-grain :v :per-act}]))
                three (act :pia :pia [(note :e1 "one") (note :e2 "two") (mention :e3 [:bob])])
                s (c/sealed st three)
                ids (set (map :lock-id (:facts s)))
                a3 (send! three)
                rows (c/raw-rows st :pia (:name three))
                lid (:lock-id (first rows))]
            (is (= [:yes :yes :yes] (map :answer [before sw a3])))
            (is (= 1 (count ids)) "the door sealed the act under one lock")
            (is (= #{lid} (set (map :lock-id rows))) "every row cites the act's lock")
            (is (some? (c/lock-row st :pia lid)) "one lock row for the act")
            (is (= 2 (count (c/lock-rows st :pia))) "the act's one row and the earlier value's")
            (let [f (c/forget-value! st :pia :pia (fid a3 1))]
              (is (= :row-deleted (:how f)))
              (is (= [:erased :erased :erased] (map #(state-of st :pia (fid a3 %)) [0 1 2])) "the act erased whole")
              (is (= :open (state-of st :pia (fid before 0))) "a value written before the switch keeps its own lock"))
            (testing "an act citing three locks after the switch: :grain-mismatch, recorded, its leases gone"
              (let [o (act :pia :pia [(note :e4 "a") (note :e5 "b") (note :e6 "c")])
                    _ (c/stock! st :pia :pia (:session o) 4)
                    pooled (vec (take 3 (:locks (c/lease-locks st :pia (:session o)))))
                    a (send! (sealed-under o (zipmap [0 1 2] pooled)))]
                (is (= :grain-mismatch (:reason a)))
                (is (= :grain-mismatch (:reason (rec (:name o)))) "recorded")
                (is (not-any? (lease-ids-of st :pia (:session o)) (map first pooled)) "its three leases destroyed")))))
        (testing "a marked mention dies with the one it mentions; an unmarked one does not (7b, A8's direction)"
          (person+ :fay) (person+ :gus)
          (layer! st :fay :personal :fay)
          (let [marked (send! (act :fay :fay [(mention :e0 [:gus] :die-with-any)]))
                plain (send! (act :fay :fay [(mention :e1 [:gus])]))
                fg (c/forget-person! st :gus)]
            (is (= :yes (:answer fg)))
            (is (= {:erased-at (:stamp fg)} (c/opens? st :fay (fid marked 0))) "marked: dies with Gus, dated by his forget")
            (is (= {:value {:persons #{:gus}}} (c/opens? st :fay (fid plain 0))) "unmarked: survives Gus")
            (is (= :person-forgotten (:reason (send! (act :fay :fay [(mention :e2 [:gus] :die-with-any)]))))
                "a new value that must die with Gus cannot be wrapped under his destroyed lock")
            (let [later (send! (act :fay :fay [(mention :e3 [:gus])]))]
              (is (= :yes (:answer later)) "an unmarked mention of Gus after his forget: admitted, wrapped under Fay alone (E6 Q1, L11)")
              (is (= [:fay] (:required (c/lock-row st :fay (:lock-id (c/raw-row st :fay (fid later 0)))))))
              (is (contains? (:subjects (rec (:name later))) :gus) "and still found by him in the subject slot (ruling 8)"))))
        (testing "a write about a person with no lock is refused :no-such-person (L11)"
          (is (= :no-such-person (:reason (send! (act :alice :alice [(mention :e16 [:nobody] :die-with-any)]))))))
        (testing "a value forget after a person forget: the ledger's date shows (L16)"
          (person+ :erin)
          (layer! st :erin :personal :erin)
          (let [a (send! (act :erin :erin [(note :e0 "erin's note")]))
                fp (c/forget-person! st :erin)
                _ (is (= {:erased-at (:stamp fp)} (c/opens? st :erin (fid a 0))))
                fv (c/forget-value! st :operator :erin (fid a 0))]
            (is (= [:yes :row-deleted] [(:answer fv) (:how fv)]) "the operator's forget of a value already closed: admitted, it erases the row")
            (is (= {:erased-at (:stamp fv)} (c/opens? st :erin (fid a 0))) "the value forget's date shows")))
        (testing "after a person is forgotten: a lease is refused, an old lease's lock does not deliver, their grain switch stands"
          (person+ :hal)
          (layer! st :hal :personal :hal)
          (let [o (act :hal :hal [(note :e0 "hal's note")])
                _ (c/stock! st :hal :hal (:session o) 4)
                _ (c/sealed st o)
                fp (c/forget-person! st :hal)]
            (is (= :yes (:answer fp)))
            (is (= :person-forgotten (:reason (c/lease! st :hal :hal :door/hal 4))) "a lease into his layer")
            (is (= :no-such-lock (:reason (send! o))) "an offer citing a lease from before his forget: refused on its face")
            (is (nil? (rec (:name o))))
            (is (= :yes (:answer (send! (act :hal :hal [{:e :hal :k :lock-grain :v :per-act}])))) "his grain switch is still admitted")))
        (testing "making a person twice is refused; forgetting twice is yes and keeps the first date"
          (person+ :ike)
          (is (= :person-already-made (:reason (c/make-person! st :ike))))
          (let [f1 (c/forget-person! st :ike)
                e1 (c/person st :ike)
                f2 (c/forget-person! st :ike)]
            (is (= [:yes :yes] [(:answer f1) (:answer f2)]))
            (is (= {:lock nil :erased-at (:stamp f1)} e1))
            (is (= e1 (c/person st :ike)) "the first date stays")
            (is (every? #(= e1 %) (on-every-task st :ike task-keys)))))
        (testing "time travel: a read as of a moment shows nothing admitted after it, and an erasure only as its date (I-L7)"
          (person+ :ivy)
          (layer! st :ivy :personal :ivy)
          (let [made-stamp (first (first (c/by-stamp st :ivy)))
                a1 (send! (act :ivy :ivy [(note :e0 "first")]))
                a2 (send! (act :ivy :ivy [(note :e1 "second")]))
                ids (fn [r] (set (map :id (:facts r))))
                at1 (as-of st :ivy (:stamp a1))]
            (is (contains? (ids at1) (fid a1 0)))
            (is (not (contains? (ids at1) (fid a2 0))) "the second act is after the moment")
            (is (= "first" (:value (fact-in at1 (fid a1 0)))))
            (let [f (c/forget-value! st :ivy :ivy (fid a1 0))
                  again (as-of st :ivy (:stamp a1))]
              (is (< (:stamp a2) (:stamp f)))
              (is (= (:stamp f) (:erased-at (fact-in again (fid a1 0)))) "erased on the forget's date, though the forget is after the moment")
              (is (not (contains? (fact-in again (fid a1 0)) :value)))
              (is (not (contains? (ids again) (fid a2 0))) "still nothing from after the moment")
              (is (not-any? #(= :forget (:k %)) (:facts again)) "the forget fact itself is after the moment"))
            (is (empty? (:facts (as-of st :ivy (dec made-stamp)))) "a read before the layer's first stamp is empty")))
        (testing "the lock store's count: personal and hand grow by one per value, agent by none"
          (let [count-of (fn [l] (count (c/lock-rows st l)))
                before (mapv count-of [:alice :alice-hand :alice-agent])]
            (send! (act :alice :alice [(note :e17 "one") (note :e18 "two")]))
            (send! (act :alice :alice-hand [(note :e17 "one")]))
            (send! (act :alice :alice-agent [(note :e17 "one") (note :e18 "two")]))
            (is (= (mapv + before [2 1 0]) (mapv count-of [:alice :alice-hand :alice-agent]))))))

      (testing "the model's A cases through the module, both directions (1, R8)"
        (inject/record-purges!)
        (doseq [[i c] (map-indexed vector fs/a-cases)
                :let [[what _ _] c
                      alice (keyword (str "a" (inc i) "-alice"))
                      bob (keyword (str "a" (inc i) "-bob"))
                      rename {:alice alice :bob bob}
                      offers (case-offers c)
                      forgets (case-forgets c)
                      one-owner? (= :alice (:layer (first offers)))
                      layer (if one-owner? alice :base)
                      expected (model-values c)]]
          (testing what
            (person+ alice) (person+ bob)
            (if one-owner?
              (layer! st alice :personal alice)
              (do (grant! st [alice :base :base]) (grant! st [bob :base :base])))
            (let [facts (vec (for [spec offers
                                   :let [who (rename (:who spec))
                                         fs (vec (map (fn [f] (cond-> {:e (:e f) :k (:k f)
                                                                       :v (if (= :mention (:k f))
                                                                            {:persons (set (map rename (:mention f)))}
                                                                            {:token (str what)})}
                                                                (seq (:mark f)) (assoc :mark (:mark f))))
                                                      (:facts spec)))
                                         a (send! (act who layer fs))]
                                   [j f] (map-indexed vector (:facts spec))]
                               {:ref [(:layer spec) (:k f) (:mention f)] :id (fid a j) :answer a :spec f :layer (:layer spec)}))
                  _ (is (every? #(= :yes (:answer (:answer %))) facts) "the case's offers admitted")
                  _ (is (every? #(= :open (state-of st layer (:id %))) facts) "open before any forget")
                  fstamps (vec (for [p forgets] (:stamp (c/forget-person! st (rename p)))))
                  seen (into {} (for [{:keys [ref id]} facts] [ref (state-of st layer id)]))]
              (is (= expected seen) "the module's answer is the model's for the same history")
              (doseq [{:keys [id spec] model-layer :layer} facts
                      :let [closer (model-closer model-layer spec forgets)]]
                (if closer
                  (is (= (nth fstamps closer) (:erased-at (fact-in (now st layer) id)))
                      "erased on the date of the forget the model says closed it")
                  (is (contains? (fact-in (now st layer) id) :value) "what must stay open stays open")))
              (doseq [p forgets]
                (is (every? #(and (nil? (:lock %)) (some? (:erased-at %))) (on-every-task st (rename p) task-keys))
                    (str "after the forget's answer, " (rename p) "'s lock is gone on every task")))
              (let [purged (set (map :fid (filter #(= layer (:layer %)) @inject/purges)))]
                (doseq [{:keys [ref id]} facts]
                  (if (= :erased (get expected ref))
                    (is (contains? purged id) "a value that died with the person went to the purge seam")
                    (is (not (contains? purged id)) "a value that stays open is not purged"))))
              (when (= 0 i)
                (let [lrows (c/lock-rows st layer)]
                  (is (= 2 (count lrows)) "after a person forget the lock rows stay (E5, RD7s): closed, not deleted")
                  (is (every? #(nil? (l/unwrap (second %) (live-persons st @all-persons))) lrows)
                      "and none unwraps with a live person lock"))
                (p6-check! st layer (:id (first facts)) {:token (str what)} [:alice :alice-hand :alice-agent :base layer]
                           @all-persons "A1's person forget"))))))

      (testing "the base's re-wrap: a note about no one by Alice survives her forget; her unconsumed leases there no longer unlease ([V-F2])"
        (person+ :rw-alice)
        (grant! st [:rw-alice :base :base])
        (let [o (act :rw-alice :base [(note :e0 "about no one")])
              _ (c/stock! st :rw-alice :base (:session o) 4)
              a (send! o)
              _ (is (= :yes (:answer a)))
              _ (is (= {:required [] :any-of []} (select-keys (:lock (c/raw-row st :base (fid a 0))) [:required :any-of]))
                    "an empty wrap, K in the record (R8), not under the writer's lock")
              unconsumed (count (c/lease-rows st :base (:session o)))
              f (c/forget-person! st :rw-alice)]
          (is (= :yes (:answer f)))
          (is (= {:value "about no one"} (c/opens? st :base (fid a 0))) "the note survives its writer")
          (is (= unconsumed (count (c/lease-rows st :base (:session o)))) "her lease rows stay, unconsumed")
          (is (empty? (:locks (c/lease-locks st :base (:session o)))) "and none unleases any more")))

      (testing "session close (9)"
        (let [s :s-close
              o (act :alice :alice-hand [(note :e20 "x")] :session s)
              _ (c/stock! st :alice :alice-hand s 8)
              _ (c/stock! st :alice :alice-hand :s-stays 2)
              used (vec (for [i (range 3)] (send! (act :alice :alice-hand [(note :e20 i)] :session s))))
              spare (first (:locks (c/lease-locks st :alice-hand s)))
              left (count (c/lease-rows st :alice-hand s))
              closed (c/close-session! st :alice :alice-hand s)]
          (is (every? #(= :yes (:answer %)) used))
          (is (= 5 left) "8 leased, 3 used")
          (is (= :yes (:answer closed)))
          (is (empty? (c/lease-rows st :alice-hand s)) "its unconsumed rows are gone")
          (is (= 2 (count (c/lease-rows st :alice-hand :s-stays))) "another session's rows stay")
          (is (= :no-such-lock (:reason (send! (sealed-under o {0 spare})))) "an offer citing a closed lease")
          (let [again (c/close-session! st :alice :alice-hand s)]
            (is (= :yes (:answer again)) "a second close: yes")
            (is (empty? (c/lease-rows st :alice-hand s)) "and changes nothing"))
          (is (= :control-not-allowed
                 (:reason (c/offer-until-answered!
                           st (c/build {:who :alice :layer :alice-hand :class :by-layer :permission [:alice :alice-hand :alice-hand]
                                        :session :s-mine :facts [{:e :s-stays :k :session-closed :v {:session :s-stays}}]}))))
              "an act from one session cannot close another (L28)")
          (is (= 2 (count (c/lease-rows st :alice-hand :s-stays))))))

      (testing "a person forget's date is its stamp at the gate's wall, under simulated time"
        (person+ :sim)
        (layer! st :sim :personal :sim)
        (let [a (send! (act :sim :sim [(note :e0 "simulated")]))]
          (with-open [_ (TopologyUtils/startSimTime)]
            (let [wall (+ 5000000 (apply max (map #(hlc/ms-of (clock %)) [:people :sim])))
                  _ (TopologyUtils/advanceSimTime wall)
                  f (c/forget-person! st :sim)]
              (is (= wall (hlc/ms-of (:stamp f))) "stamped at the simulated wall's millisecond")
              (is (= {:erased-at (:stamp f)} (c/opens? st :sim (fid a 0))) "the value shows that date")
              (is (= (:stamp f) (:erased-at (c/person st :sim))))))))

      ;; the crashes come last: each restarts the worker and replays records

      (testing "a crash between the lease and the offer: the lease is answered from its record, the offer decided once (7)"
        (let [s :s-crash1
              o (act :alice :alice-hand [(note :e30 "after the lease")] :session s)
              _ (c/stock! st :alice :alice-hand s 4)
              before (:locks (c/lease-locks st :alice-hand s))
              sealed (c/sealed st o)
              used (:lock-id (first (:facts sealed)))]
          (inject/watch! (:name o))
          (inject/arm! :seen (:name o))
          (let [first-try (try (send! o) (catch Exception _ :append-threw))
                answered (wait-until #(let [r (c/lookup st (:name o) (env/offer-digest o))] (when (map? r) r)) 120000)
                again (c/offer-until-answered! st o)
                after (:locks (retrying #(c/lease-locks st :alice-hand s)))]
            (say "crash at :seen: the append" (if (= :append-threw first-try) "threw" "returned"))
            (is (= 1 (inject/fired-count :seen (:name o))) "the crash fired")
            (is (map? answered))
            (is (= :yes (:answer again)))
            (is (= (set (keys (dissoc before used))) (set (keys after))) "the lease's other rows stand under the same ids")
            (is (every? (fn [[id K]] (bytes= K (get before id))) after) "with the same bytes: no new bytes")
            (is (= {:value "after the lease"} (c/opens? st :alice-hand (fid again 0))) "the value opens under the lock the door sealed with"))))

      (testing "a crash during the gate event, before and after its writes: the writes discarded, the leases still there, the replay admits (7)"
        (doseq [point [:before-writes :after-writes]]
          (let [s (keyword (str "s-crash-" (name point)))
                o (act :alice :alice [(note :e31 (str "crash " (name point)))] :session s)
                _ (c/stock! st :alice :alice s 2)
                sealed (c/sealed st o)
                used (:lock-id (first (:facts sealed)))]
            (inject/watch! (:name o))
            (inject/arm! point (:name o))
            (let [first-try (try (send! o) (catch Exception _ :append-threw))
                  answered (wait-until #(let [r (c/lookup st (:name o) (env/offer-digest o))] (when (map? r) r)) 120000)
                  again (c/offer-until-answered! st o)]
              (say "crash at" point ": the append" (if (= :append-threw first-try) "threw" "returned")
                   "| decisions" (inject/count-of :before-writes (:name o)))
              (is (= 1 (inject/fired-count point (:name o))))
              (is (map? answered))
              (is (= :yes (:answer again)))
              (is (<= 2 (inject/count-of :before-writes (:name o))) "decided again after the crash (R4)")
              (is (= 1 (count (c/raw-rows st :alice (:name o)))) "admitted once")
              (is (not (contains? (lease-ids-of st :alice s) used)) "its lease consumed once, by the decision that stood")
              (is (= {:value (str "crash " (name point))} (c/opens? st :alice (fid again 0))))))))

      (testing "a crash in a person forget's fan-out child: every task ends with the lock destroyed (8)"
        (person+ :kit)
        (layer! st :kit :personal :kit)
        (let [a (send! (act :kit :kit [(note :e0 "kit's note")]))
              fo (c/build {:who :operator :layer :people :class :by-layer :facts [{:e :kit :k :forget-person :v {:person :kit}}]})]
          (inject/arm! :fan-out (:name fo))
          (let [first-try (try (send! fo) (catch Exception _ :append-threw))
                answer (c/offer-until-answered! st fo)]
            (say "crash in the fan-out: the append" (if (= :append-threw first-try) "threw" "returned"))
            (is (= 1 (inject/fired-count :fan-out (:name fo))))
            (is (= :yes (:answer answer)))
            (is (wait-until #(every? (fn [e] (and (nil? (:lock e)) (= (:stamp answer) (:erased-at e))))
                                     (on-every-task st :kit task-keys))
                            120000)
                "at least once on every task (R4)")
            (is (= {:erased-at (:stamp answer)} (c/opens? st :kit (fid a 0)))))))

      (testing "a crash in a value forget before its writes: no half forget; the replay decides the same (8)"
        (let [a (send! (act :alice :alice [(note :e32 "forgotten through a crash")]))
              id (fid a 0)
              lid (:lock-id (c/raw-row st :alice id))
              fo (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                           :stood-on {id (:stamp a)} :facts [{:e :e32 :k :forget :v {:target id}}]})]
          (inject/watch! (:name fo))
          (inject/arm! :before-writes (:name fo))
          (let [first-try (try (send! fo) (catch Exception _ :append-threw))
                answer (c/offer-until-answered! st fo)
                ;; an answer found by lookup (the append failed while the worker restarted) has no :how;
                ;; the resend's ack carries it, from the ledger
                again (send! fo)]
            (say "crash in a value forget: the append" (if (= :append-threw first-try) "threw" "returned"))
            (is (= 1 (inject/fired-count :before-writes (:name fo))))
            (is (<= 2 (inject/count-of :before-writes (:name fo))) "decided again after the crash")
            (is (= :yes (:answer answer)))
            (is (= [:yes :row-deleted (:stamp answer)] [(:answer again) (:how again) (:stamp again)]))
            (is (= {:stamp (:stamp answer) :how :row-deleted} (get (c/ledger st :alice) lid)) "one ledger entry, the stamp that stood")
            (is (nil? (c/lock-row st :alice lid)))
            (is (= {:erased-at (:stamp answer)} (c/opens? st :alice id)))))))))
