(ns rig.store.micro-prepare-test
  "Pure tests of the micro store (PLAN-micro-store.md §F, 'The pure tests'):
  the permission walk (`rig.store.permit`), the micro gate's parse and its
  two digests, the route key, the arrival's lock work, the fold `prepare`
  over batches whose rows are gathered by the gather's own pure steps
  against an in-memory world (the same functions the topology calls, in
  the order it calls them), the rows of block 2b, and generated inputs
  that must never throw. No cluster.

  Where the model has the same history (model.clj `micro-prepare`,
  `refusal`, `stamp-for` under `baseline`), its answer is asked too.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [formal.model :as fm]
            [rig.store.clock :as hlc]
            [rig.store.envelope :as env]
            [rig.store.gate :as gate]
            [rig.store.locks :as locks]
            [rig.store.micro :as micro]
            [rig.store.micro-client :as mc]
            [rig.store.permit :as permit]))

;; ================================================================ helpers

(defn- check!
  [label n p]
  (let [r (tc/quick-check n p {:max-size 25})]
    (is (true? (:pass? r))
        (str label " failed: " (pr-str (select-keys r [:seed :num-tests :fail]))
             "\nsmallest: " (pr-str (get-in r [:shrunk :smallest]))))
    r))

(def root [:group :group :group])
(def pa [:alice :group :group root])
(def pb [:bob :group :group root])
(def ps [:s1 :group :group pa])

(defn- nm [& [layer cls]] (env/make-name (or layer :group) (if (= ::none cls) nil (or cls :by-entity))))

(defn- offer
  "A micro offer (plaintext values), built as the door builds it."
  [spec]
  (mc/build (merge {:who :alice :layer :group :session :s1 :permission ps} spec)))

(defn- lease-row [K L s & {:keys [kind owner batch under] :or {kind :group batch 1}}]
  {:under under :sealed K :layer L :session s :kind kind :owner owner :batch batch})

(defn- sealed
  "An offer sealed at the door under fresh locks of one lease, per value by
  default; returns [sealed-offer lease-rows-by-id locks-by-id]."
  [o & {:keys [grain lease-name kind owner]}]
  (let [ln (or lease-name (env/make-name (:layer o) :by-entity))
        n (mc/value-count o)
        ids (locks/lease-ids ln (max 1 (if (= :per-act grain) 1 n)))
        ks (into {} (map (fn [id] [id (locks/fresh-lock)])) ids)
        so (mc/seal o (mc/assign-by-grain (or grain :per-value) ids) ks)]
    [so (into {} (map (fn [[id K]] [id (lease-row K (:layer o) (:session o) :kind (or kind :group) :owner owner)])) ks) ks]))

(defn- task-of [e] (mod (Math/abs (long (hash e))) 4))

(defn- gather
  "The gather of block 1 for one raw record against an in-memory world, by
  the gather's own pure steps in the topology's order: the rows it would
  emit. World keys: :leases {lock-id row}, :persons {p entry}, :names {nm
  rec}, :msettings {L m}, :ssettings {L s}, :mperms {[L pid] row},
  :sperms {[L pid] row}, :sheads {[L e k r] stamp}, :tombs {[L e k r]
  tomb}, :mheads {[L e k r] head}, :clocks {t c}, :answers {e [nm ...]}."
  [raw world]
  (let [in (micro/intake raw)]
    (cond
      (nil? in) []
      (= :face (:kind in)) (:rows in)
      :else
      (let [lrows (into {} (map (fn [lid] [lid (get-in world [:leases lid])])) (micro/lease-keys in))
            ups (into {} (map (fn [p] [p (get-in world [:persons p])])) (micro/unders lrows))
            arr (micro/arrival-open in lrows ups)
            cps (micro/persons-to-check in arr)
            pr (micro/person-reason cps (into {} (map (fn [p] [p (get-in world [:persons p])])) cps))
            cnames (when-let [s (:close in)] (get-in world [:answers s]))
            sk (micro/skeleton in arr pr cnames)
            n (:name in)
            nt (task-of n)
            step (micro/name-step sk (get-in world [:names n]) nt (get-in world [:clocks nt]))
            sk2 (:sk step)]
        (case (:path step)
          :fresh
          (let [o (:offer sk2) L (:layer o)
                msettings (get-in world [:msettings L])
                ssettings (when-not msettings (get-in world [:ssettings L]))
                perms (into {} (map (fn [pid] [pid {:micro (get-in world [:mperms [L pid]])
                                                    :stream (when ssettings (get-in world [:sperms [L pid]]))}]))
                            (gate/pids-to-read o))
                hkeys (if ssettings (micro/replacing-keys o) [])
                sheads (into {} (map (fn [hk] [hk (get-in world [:sheads (into [L] hk)])])) hkeys)
                tombs (into {} (map (fn [hk] [hk (when (get sheads hk) (get-in world [:tombs (into [L] hk)]))])) hkeys)
                lstep (micro/layer-rows sk2 msettings ssettings perms sheads tombs)]
            (if (:face lstep)
              (:rows lstep)
              (let [first-rows (into (:rows step) (:rows lstep))]
                (into [] (mapcat (fn [[i e]]
                                   (let [mhk (micro/micro-head-keys sk2 e first-rows)
                                         heads (into {} (map (fn [hk] [hk (get-in world [:mheads (into [L] hk)])])) mhk)]
                                     (micro/entity-rows sk2 e (task-of e) (get-in world [:clocks (task-of e)]) heads first-rows (= 0 i)))))
                      (map-indexed vector (:entities sk2))))))
          :resend
          (let [o (:offer in)]
            (into (:rows step)
                  (for [[i f] (micro/value-facts-of sk2)
                        :let [rf (nth (:facts o) i)
                              row (get-in world [:rows [(:e f) n i]])
                              carried (get (micro/carry-rows lrows) (:lock-id rf))
                              ps (micro/resend-persons row nil carried)]]
                    [[:resend n (:fp sk2) i]
                     (micro/resend-check rf row nil carried (into {} (map (fn [p] [p (get-in world [:persons p])])) ps)
                                         (:session o) (:layer o))])))
          (:rows step))))))

(defn- batch-state
  "The +map-agg of every record's rows (a later row with the same need
  replaces an earlier one, as `merge` does)."
  [raws world]
  (reduce (fn [m [need found]] (assoc m need found)) {} (mapcat #(gather % world) raws)))

(defn- writes [raws world & {:keys [wall b] :or {wall 1000 b 7}}]
  (:writes (micro/prepare (batch-state raws world) wall b)))

(defn- answers
  "name -> the record the batch writes for it."
  [ws]
  (into {} (for [[kind route field _ v] ws :when (and (= :name kind) (= :answer field))] [route v])))

(defn- faces [ws]
  (into {} (for [[kind route field k v] ws :when (and (= :name kind) (= :face field))] [[route k] (:reason v)])))

(defn- answer-of [ws o] (get (answers ws) (:name o)))
(defn- reason-of [ws o] (:reason (answer-of ws o)))

(def group-settings {:kind :group :owner nil :class :by-entity :grain :per-value :batch 1})

(defn- world
  "The seeded group: its settings, its root and Alice's, Bob's and the
  session's permissions, and Alice's and Bob's person locks."
  [& more]
  (apply merge-with merge
         {:msettings {:group group-settings}
          :mperms {[:group root] {:granted [(nm) 4] :granted-batch 1}
                   [:group pa] {:granted [(nm) 5] :granted-batch 1}
                   [:group pb] {:granted [(nm) 6] :granted-batch 1}
                   [:group ps] {:granted [(nm) 0] :granted-batch 2}}
          :persons {:alice {:lock (locks/fresh-lock) :erased-at nil}
                    :bob {:lock (locks/fresh-lock) :erased-at nil}}}
         more))

(defn- with-leases [w rows] (update w :leases merge rows))

;; ============================================================ the permit

(deftest permit-chain-and-pid
  (testing "a chain from the id alone, the cited one first"
    (is (= [ps pa root] (permit/chain ps)))
    (is (= [root] (permit/chain root)))
    (is (= [] (permit/chain nil)))
    (is (= [] (permit/chain :junk))))
  (testing "pid? four deep, nested ids of three or four readable keywords"
    (is (env/pid? root))
    (is (env/pid? ps))
    (is (env/pid? [:tool :g :g [:agent :g :g [:s :g :g [:a :g :g]]]]) "root, session, agent, tool")
    (is (not (env/pid? [:x :g :g [:tool :g :g [:agent :g :g [:s :g :g [:a :g :g]]]]])) "five deep")
    (is (not (env/pid? [:a :g :g :h])) "a parent that is not a pid")
    (is (not (env/pid? [:a :g])))
    (is (not (env/pid? [:a "g" :g]))))
  (testing "the parser rebuilds a nested pid as PersistentVectors all the way down"
    (let [raw (assoc (offer {:facts [{:e :e0 :k :note :v nil}]}) :permission [:s1 :group :group (subvec [:x :alice :group :group root] 1)])
          p (env/parse raw :micro)]
      (is (contains? p :ok))
      (is (instance? clojure.lang.PersistentVector (nth (get-in p [:ok :permission]) 3))))))

(deftest permit-refusal-order
  (let [rows {root {:granted [:g 0]} pa {:granted [:g 1]} pb {:granted [:g 2]} ps {:granted [:g 3]}}
        o (fn [& {:as m}] (merge {:who :alice :layer :group :session :s1 :permission ps} m))]
    (testing "covered, from here, granted, live: no refusal"
      (is (nil? (permit/refusal (o) rows)))
      (is (nil? (permit/refusal (o :permission pa :session nil) rows)) "held by :who"))
    (testing ":permission-does-not-cover-this"
      (is (= :permission-does-not-cover-this (permit/refusal (o :permission nil) rows)))
      (is (= :permission-does-not-cover-this (permit/refusal (o :who :bob :session :s2) rows)) "neither who nor session")
      (is (= :permission-does-not-cover-this (permit/refusal (o :layer :base) rows)) "another layer's write"))
    (testing ":permission-from-another-layer: the cited one's in, or any ancestor's layer or in (D1)"
      (is (= :permission-from-another-layer (permit/refusal (o :permission [:alice :group :alice-hand] :session nil) rows)))
      (is (= :permission-from-another-layer
             (permit/refusal (o :permission [:s1 :group :group [:alice :group :alice-hand]]) rows))
          "an ancestor kept in another layer")
      (is (= :permission-from-another-layer
             (permit/refusal (o :permission [:s1 :group :group [:alice :base :base [:operator :base :base]]]) rows))
          "an ancestor for another layer"))
    (testing ":no-permission when any element is not granted"
      (is (= :no-permission (permit/refusal (o) (dissoc rows ps))))
      (is (= :no-permission (permit/refusal (o) (dissoc rows root))) "the root never granted"))
    (testing ":permission-revoked when any element is revoked: a revoke cuts everything below (R19)"
      (is (= :permission-revoked (permit/refusal (o) (assoc-in rows [ps :revoked] [:g 9]))))
      (is (= :permission-revoked (permit/refusal (o) (assoc-in rows [pa :revoked] [:g 9]))) "the person's, above the session's")
      (is (= :permission-revoked (permit/refusal (o) (assoc-in rows [root :revoked] [:g 9]))) "the group's root")
      (is (nil? (permit/refusal (o :permission pb :who :bob :session nil) (assoc-in rows [pa :revoked] [:g 9])))
          "a sibling's permission stays live"))
    (testing "the order is the model's: cover, then layer, then granted, then revoked"
      (is (= :permission-from-another-layer
             (permit/refusal (o :permission [:alice :group :alice-hand] :session nil) {}))
          "from another layer before not granted"))
    (testing "the model's four reasons on the same triples (model.clj `refusal`, baseline)"
      (is (= :permission-from-another-layer
             (let [st (fm/run fm/baseline [[:offer {:who :alice :layer :group :facts [{:e :e0 :k :note :replaces :none :mark #{}}]
                                                     :stood-on nil :times 1 :cite :session}] [:batch]])]
               (:reason (first (fm/answers-for st (first (:client-sent st)))))))))))

;; ================================================================ the parse

(deftest parse-and-digests
  (let [o (offer {:facts [{:e :e0 :k :note :v {:token "a"}} {:e :e1 :k :mention :v {:persons #{:bob}}}]})
        [so _ ks] (sealed o)]
    (testing "a sealed act parses; each value fact carries its bytes and lock id, no :v"
      (let [p (micro/parse-micro so)]
        (is (contains? p :ok))
        (is (every? #(and (bytes? (:sealed %)) (locks/lock-id? (:lock-id %)) (nil? (:v %))) (get-in p [:ok :facts])))))
    (testing ":not-sealed on the face (phase 2's L27)"
      (is (= {:refuse :not-sealed} (micro/parse-micro o)) "a plaintext value")
      (is (= {:refuse :not-sealed} (micro/parse-micro (assoc-in so [:facts 0 :lock-id] :junk))) "a :sealed with no well-formed lock id")
      (is (= {:refuse :not-sealed} (micro/parse-micro (assoc-in so [:facts 0 :sealed] "text"))) "sealed bytes that are not bytes")
      (is (= {:refuse :not-sealed} (micro/parse-micro (assoc-in so [:facts 0 :v] 1))) "a :sealed beside a :v")
      (is (= {:refuse :not-sealed}
             (micro/parse-micro (offer {:who :operator :permission nil
                                        :facts [{:e :group :k :lock-grain :v :per-act :sealed (byte-array 40) :lock-id [(nm) 0]}]})))
          "a control fact carrying sealed bytes")
      (is (= {:refuse :not-sealed} (micro/parse-micro (update-in so [:facts 1] dissoc :sealed))) "a lock id with no bytes"))
    (testing "stage 1's structural faces come first, on the micro side"
      (is (= :wrong-gate (:refuse (micro/parse-micro (assoc so :name (env/make-name :group :by-layer) :class :by-layer)))))
      (is (= :mis-tagged (:refuse (micro/parse-micro (assoc so :name (env/make-name :base :by-entity))))))
      (is (= :reserved-scheme (:refuse (micro/parse-micro (assoc so :name [:group nil :crossing (env/uuid7)])))))
      (is (= :reserved-who (:refuse (micro/parse-micro (assoc so :who :store)))))
      (is (= :unknown-part (:refuse (micro/parse-micro (assoc-in so [:facts 0 :colour] :red)))))
      (is (= :empty-act (:refuse (micro/parse-micro (assoc so :facts [])))))
      (is (contains? (micro/parse-micro (assoc so :name [:group :by-entity :landing (env/uuid7)])) :ok)
          "stage 4's :landing is taken as data here (M4)"))
    (testing "the parts digest holds no value and names no lock ([PV-F2]): a resend sealed again, even under other locks, digests the same"
      (let [[so2 _ _] (sealed (assoc o :name (:name so)))
            d1 (mc/digest-of so)
            d2 (mc/digest-of so2)
            d3 (mc/digest-of (mc/seal o (mc/assign-by-grain :per-value (keys ks)) ks))]
        (is (string? d1))
        (is (= d1 d2) "other nonces and other lock ids")
        (is (= d1 d3) "the same locks resealed")
        (is (not= d1 (mc/digest-of (first (sealed (assoc o :name (:name so) :claimed-when 1))))) "other parts")
        (is (not= d1 (mc/digest-of (first (sealed (assoc-in o [:facts 1 :e] :e2)))))
            "another entity is another act")
        (let [retract (mc/digest-of (assoc so :facts [(get-in so [:facts 0]) {:e :e1 :k :mention :v nil}]))]
          (is (not= d1 retract) "a value fact and a retract under the same e and k digest apart"))))
    (testing "the envelope fingerprint is per attempt: a resend sealed again has its own (M8 revised)"
      (let [[so2 _ _] (sealed (assoc o :name (:name so)))]
        (is (= (mc/fp-of so) (mc/fp-of (into {} so))) "the same map, the same fingerprint")
        (is (not= (mc/fp-of so) (mc/fp-of so2)))
        (is (= "unreadable" (micro/envelope-fp 42)))))))

(deftest route-key
  (let [o (offer {:facts [{:e :e0 :k :note :v nil} {:e :e1 :k :note :v 1}]})
        [so rows _] (sealed o)
        ln (locks/lease-name-of (first (keys rows)))]
    (is (= ln (micro/route-key so)) "a sealed act routes by the lease name of its first cited lock")
    (is (instance? clojure.lang.PersistentVector (micro/route-key (update-in so [:facts 1 :lock-id 0] #(apply list %))))
        "rebuilt as a vector, so the depot and the PState hash it alike")
    (is (= :e0 (micro/route-key (offer {:facts [{:e :e0 :k :note :v nil}]}))) "no sealed value: the first entity")
    (is (nil? (micro/route-key 42)))
    (is (nil? (micro/route-key {:facts :junk})))
    (check! "route-key is total" 300 (prop/for-all [x gen/any-printable] (do (micro/route-key x) true)))))

;; ======================================================= the arrival's work

(deftest arrival-lock-work
  (let [o (offer {:facts [{:e :e0 :k :mention :v {:persons #{:bob}}}]})
        [so rows ks] (sealed o)
        lid (first (keys rows))
        w (world)
        in (micro/intake so)]
    (testing "every cited lock delivers: the value checks pass, the union is the value's subjects"
      (let [a (micro/arrival-open in rows {})]
        (is (= :ok (:status a)))
        (is (= [lid] (:owned a)))
        (is (nil? (:value-reason a)))
        (is (= #{:bob} (:union a)))
        (is (= [:bob] (micro/persons-to-check in a)))
        (is (nil? (micro/person-reason [:bob] (:persons w))))))
    (testing "a row leased to another session, or for another layer, counts as missing and is not owned"
      (is (= :missing (:status (micro/arrival-open in (assoc-in rows [lid :session] :s2) {}))))
      (is (= [] (:owned (micro/arrival-open in (assoc-in rows [lid :session] :s2) {}))))
      (is (= :missing (:status (micro/arrival-open in (assoc-in rows [lid :layer] :base) {})))))
    (testing "a row sealed under a person whose lock is destroyed does not unlease: missing, but owned"
      (let [K (get ks lid)
            alice (get-in w [:persons :alice])
            prow (merge (locks/lease-row K :alice alice (locks/fresh-nonce)) (select-keys (get rows lid) [:layer :session :kind :owner :batch]))]
        (is (= :ok (:status (micro/arrival-open in {lid prow} {:alice alice}))))
        (is (= :missing (:status (micro/arrival-open in {lid prow} {:alice {:lock nil :erased-at 5}}))))
        (is (= [lid] (:owned (micro/arrival-open in {lid prow} {:alice {:lock nil :erased-at 5}}))))))
    (testing "the value reasons in L27's order, computed where the value is opened"
      (let [wrong (assoc-in rows [lid :sealed] (locks/fresh-lock))]
        (is (= :does-not-open (:value-reason (micro/arrival-open in wrong {})))))
      (let [bad (mc/seal (offer {:facts [{:e :e0 :k :mention :v {:persons "bob"}}]}) (constantly lid) ks)]
        (is (= :value-shape (:value-reason (micro/arrival-open (micro/intake bad) rows {})))))
      (let [many (mc/seal (offer {:facts [{:e :e0 :k :mention :v {:persons (set (map #(keyword (str "p" %)) (range 300)))}}]})
                          (constantly lid) ks)]
        (is (= :too-many-subjects (:value-reason (micro/arrival-open (micro/intake many) rows {})))))
      (let [junk (assoc-in so [:facts 0 :sealed] (locks/seal (get ks lid) (.getBytes "(not canonical" "UTF-8")))]
        (is (= :malformed-value (:value-reason (micro/arrival-open (micro/intake junk) rows {}))))))
    (testing "the person reasons, :no-such-person before :person-forgotten"
      (is (= :no-such-person (micro/person-reason [:bob :carol] (:persons w))))
      (is (= :person-forgotten (micro/person-reason [:bob] {:bob {:lock nil :erased-at 3}})))
      (is (= :no-such-person (micro/person-reason [:bob :carol] {:bob {:lock nil :erased-at 3}}))))
    (testing "the skeleton carries no value and no lock: only the marker where the bytes were"
      (let [sk (micro/skeleton in (micro/arrival-open in rows {}) nil nil)
            flat (tree-seq coll? seq sk)]
        (is (not-any? bytes? flat))
        (is (= true (get-in sk [:offer :facts 0 :sealed])))
        (is (= lid (get-in sk [:offer :facts 0 :lock-id])))))
    (testing "the record path carries only rows sealed under a person lock (M18: no bare lock crosses a task)"
      (is (= {} (micro/carry-rows rows)))
      (is (= 1 (count (micro/carry-rows (assoc-in rows [lid :under] :alice))))))))

;; ================================================================ the fold

(deftest fold-decisions
  (let [w (world)]
    (testing "an act on two entities is admitted whole: one record under the name and under each entity, heads, clocks"
      (let [o (offer {:facts [{:e :e0 :k :note :v {:token "a"}} {:e :e1 :k :note :v {:token "b"}}]})
            [so rows _] (sealed o)
            ws (writes [so] (with-leases w rows))
            rec (answer-of ws so)]
        (is (= :yes (:answer rec)))
        (is (= 7 (:batch rec)))
        (is (= #{:e0 :e1} (set (for [[kind route field k _] ws :when (and (= :entity kind) (= :answers field) (= (:name so) k))] route))))
        (is (= 2 (count (for [[kind _ field _ _] ws :when (and (= :entity kind) (= :heads field))] 1))))
        (is (every? #(= (:stamp rec) (nth % 4)) (filter #(= :task (first %)) ws)) "every touched task's clock set to the stamp")
        (is (<= (hlc/pack 1000 0) (:stamp rec)) "at or after the leader's wall")
        (is (= (set (map (fn [lid] [:del-lease (locks/lease-name-of lid) nil (nth lid 1) nil]) (keys rows)))
               (set (filter #(= :del-lease (first %)) ws)))
            "the cited lease rows consumed at decision")))
    (testing "the four permission reasons at this gate, recorded, leases consumed"
      (doseq [[what spec world* want]
              [["no such permission" {:permission [:s9 :group :group pa] :session :s9} w :no-permission]
               ["another layer (D1)" {:permission [:alice :group :alice-hand] :session nil} w :permission-from-another-layer]
               ["does not cover (Bob's)" {:who :bob :session nil :permission pa} w :permission-does-not-cover-this]
               ["revoked above (D2's)" {} (assoc-in w [:mperms [:group pa] :revoked] [(nm) 0]) :permission-revoked]]]
        (let [o (offer (merge {:facts [{:e :e0 :k :note :v {:token what}}]} spec))
              [so rows _] (sealed o)
              ws (writes [so] (with-leases world* rows))]
          (is (= [:no want] ((juxt :answer :reason) (answer-of ws so))) what)
          (is (seq (filter #(= :del-lease (first %)) ws)) (str what ": its lease consumed")))))
    (testing "the operator is exempt: cites no permission"
      (let [o (offer {:who :operator :session nil :permission nil :facts [{:e :e0 :k :note :v nil}]})]
        (is (= :yes (:answer (answer-of (writes [o] w) o))))))
    (testing "stage 1's reasons at this gate: outside the act's layer, no such layer, class mismatch"
      (let [o1 (offer {:facts [{:e :e0 :k :note :v nil :layer :base}]})
            o2 (offer {:layer :nowhere :permission [:s1 :nowhere :nowhere] :facts [{:e :e0 :k :note :v nil}]})
            o3 (mc/build {:who :alice :layer :alice :class :by-entity :session nil :permission [:alice :alice :alice]
                          :facts [{:e :e0 :k :note :v nil}]})
            ws (writes [o1 o2 o3] (assoc-in w [:ssettings :alice] {:kind :personal :owner :alice :class :by-layer :grain :per-value}))]
        (is (= :fact-outside-the-acts-layer (reason-of ws o1)))
        (is (= :no-such-layer (reason-of ws o2)))
        (is (= :class-mismatch (reason-of ws o3)) "a :by-entity name into a layer the stream gate still orders by layer")))
    (testing "control facts at this gate (M14, R13): the operator's; a person's refused; a :class outside a making act refused"
      (let [grain (offer {:who :operator :session nil :permission nil :facts [{:e :group :k :lock-grain :v :per-act}]})
            mine (offer {:facts [{:e :group :k :lock-grain :v :per-act}]})
            reclass (offer {:who :operator :session nil :permission nil :facts [{:e :group :k :class :v :by-entity}]})
            members (offer {:who :operator :session nil :permission nil :facts [{:e :group :k :members :v #{:carol}}]})
            person (offer {:who :operator :session nil :permission nil :facts [{:e :carol :k :person :v {:id :carol}}]})
            ws (writes [grain mine reclass members person] w)]
        (is (= :yes (:answer (answer-of ws grain))))
        (is (= :per-act (:grain (some (fn [[kind route field _ v]] (when (and (= :entity kind) (= :group route) (= :settings field)) v)) ws)))
            "a new settings version, keyed by the batch")
        (is (= :control-not-allowed (reason-of ws mine)) "a person's grain switch on a shared layer (no owner, D11)")
        (is (= :unsupported-reclass (reason-of ws reclass)) "O9")
        (is (= :layer-already-made (reason-of ws members)) "members only in the making act")
        (is (= :control-not-allowed (reason-of ws person)) "the :people layer's acts are the stream gate's")))
    (testing "stale and doubled replaces refuse the whole act"
      (let [f0 [(nm) 0]
            w2 (assoc-in w [:mheads [:group :e0 :note f0]] {:stamp 50 :batch 3})
            ok (offer {:facts [{:e :e0 :k :note :v nil :replaces f0}]})
            stale (offer {:facts [{:e :e0 :k :note :v nil :replaces [(nm) 1]}]})
            doubled (offer {:facts [{:e :e0 :k :note :v nil :replaces f0} {:e :e1 :k :note :v nil :replaces f0}]})
            half (offer {:facts [{:e :e1 :k :note :v nil} {:e :e0 :k :note :v nil :replaces [(nm) 2]}]})
            ws (writes [ok stale doubled half] w2)]
        (is (= :yes (:answer (answer-of ws ok))))
        (is (< 50 (:stamp (answer-of ws ok))) "after the stamp it replaces")
        (is (= :stale-replaces (reason-of ws stale)))
        (is (= :stale-replaces (reason-of ws doubled)))
        (is (= :stale-replaces (reason-of ws half)) "one good and one stale fact: refused whole")
        (is (some (fn [[kind _ field k v]] (and (= :entity kind) (= :heads field) (= [:group :note f0] k) (= (:name ok) (first (:replaced-by v))))) ws)
            "the replaced head is kept with its replacer (M7), never deleted")))
    (testing "a stale revoke; a revoke of a live grant"
      (let [dead [:carol :group :group root]
            r1 (offer {:who :operator :session nil :permission nil :facts [{:e :perm-carol :k :revoke :v {:permission dead}}]})
            r2 (offer {:who :operator :session nil :permission nil :facts [{:e :perm-bob :k :revoke :v {:permission pb}}]})
            ws (writes [r1 r2] w)]
        (is (= :stale-revoke (reason-of ws r1)))
        (is (= :yes (:answer (answer-of ws r2))))
        (is (some (fn [[kind _ field k v]] (and (= :entity kind) (= :permissions field) (= pb k) (some? (:revoked v)) (= 7 (:revoked-batch v)))) ws))))))

(deftest fold-order-and-the-race
  (let [w (world)
        [n1 n2] (sort-by #(nth % 3) (repeatedly 2 #(do (Thread/sleep 2) (nm))))]
    (testing "the revocation race by M2's order (OP6), both ways: a revoke named before a write refuses it, after it admits it"
      (let [mk (fn [n tok] (let [[so rows _] (sealed (assoc (offer {:facts [{:e :e0 :k :note :v {:token tok}}]}) :name n))] [so rows]))
            revoke (fn [n] (assoc (offer {:who :operator :session nil :permission nil :facts [{:e :perm-s1 :k :revoke :v {:permission ps}}]}) :name n))
            [wr1 rows1] (mk n2 "after")
            ws1 (writes [(revoke n1) wr1] (with-leases w rows1))
            [wr2 rows2] (mk n1 "before")
            ws2 (writes [wr2 (revoke n2)] (with-leases w rows2))]
        (is (= :permission-revoked (reason-of ws1 wr1)) "revoke first in the batch order: the write refused")
        (is (= :yes (:answer (answer-of ws2 wr2))) "write first: admitted")
        (is (= :yes (:answer (get (answers ws2) n2))) "and the revoke after it")))
    (testing "a revoke of an ancestor (Alice's group permission) cuts her session's beneath it, later in the same batch (R19)"
      (let [[wr rows _] (sealed (assoc (offer {:facts [{:e :e0 :k :note :v {:token "x"}}]}) :name n2))
            rev (assoc (offer {:who :operator :session nil :permission nil :facts [{:e :perm-alice :k :revoke :v {:permission pa}}]}) :name n1)
            ws (writes [rev wr] (with-leases w rows))]
        (is (= :permission-revoked (reason-of ws wr)))))
    (testing "offers decided in UUID7 order whatever their arrival: M2"
      (let [a (assoc (offer {:facts [{:e :e0 :k :note :v nil}]}) :name n2)
            b (assoc (offer {:facts [{:e :e0 :k :note :v nil}]}) :name n1)
            ws (writes [a b] w)]
        (is (< (:stamp (get (answers ws) n1)) (:stamp (get (answers ws) n2))) "the earlier UUID7 stamps first on the shared task")))
    (testing "stamps strictly increase per task within a batch and exceed carried stood-on stamps"
      (let [os (for [i (range 6)] (offer {:facts [{:e :e0 :k :note :v nil}] :stood-on (if (= i 3) {[(nm) 0] (hlc/pack 5000 7)} {})}))
            ws (writes os w :wall 1000)
            st (map #(:stamp (answer-of ws %)) (sort-by #(nth (:name %) 3) os))]
        (is (apply < st))
        (is (< (hlc/pack 5000 7) (:stamp (answer-of ws (nth os 3)))) "after what it stood on")))))

(deftest fold-names-and-faces
  (let [w (world)
        o (offer {:facts [{:e :e0 :k :note :v {:token "a"}}]})
        [so rows _] (sealed o)
        wl (with-leases w rows)]
    (testing "a name decided in committed state: the same content is recorded (nothing written), other content :name-taken"
      (let [rec {:answer :no :reason :no-permission :stamp 9 :digest (mc/digest-of so) :batch 3}
            ws (writes [so] (assoc-in wl [:names (:name so)] rec))]
        (is (nil? (answer-of ws so)) "no new record")
        (is (= {[(:name so) (mc/fp-of so)] :recorded} (faces ws)) "only the envelope's trace: answered from the record")
        (is (seq (filter #(= :del-lease (first %)) ws)) "a resend answered from the record consumes its owned leases ([PV-F2])"))
      (let [rec {:answer :yes :stamp 9 :digest "other" :batch 3}
            ws (writes [so] (assoc-in wl [:names (:name so)] rec))]
        (is (= {[(:name so) (mc/fp-of so)] :name-taken} (faces ws)))
        (is (empty? (filter #(= :del-lease (first %)) ws)) ":name-taken consumes nothing")))
    (testing "two envelopes under one name in one batch ([PV-F3]): the first by [uuid7 name fp] decided, the other :name-taken on its face"
      (let [[so2 rows2 _] (sealed (assoc-in o [:facts 0 :v] {:token "other"}) :lease-name (env/make-name :group :by-entity))
            so2 (assoc so2 :name (:name so))
            ws (writes [so so2] (with-leases wl rows2))
            [first-fp second-fp] (sort [(mc/fp-of so) (mc/fp-of so2)])]
        (is (= :yes (:answer (answer-of ws so))))
        (is (= {[(:name so) second-fp] :name-taken} (faces ws)))
        (is (= first-fp (some (fn [[kind _ field _ v]] (when (and (= :name kind) (= :fp field)) v)) ws))
            "the decided envelope's fingerprint beside the record, for block 2b's filter")))
    (testing "a face refusal is data under [name fp], never the name's answer (M8)"
      (let [bad (assoc so :who :store)
            ws (writes [bad] wl)]
        (is (= {[(:name so) (mc/fp-of bad)] :reserved-who} (faces ws)))
        (is (nil? (answer-of ws bad)))))
    (testing "a missing lock is a face :no-such-lock, nothing consumed, the name free"
      (let [ws (writes [so] w)]
        (is (= {[(:name so) (mc/fp-of so)] :no-such-lock} (faces ws)))
        (is (empty? (filter #(= :del-lease (first %)) ws)))
        (is (nil? (answer-of ws so)))))
    (testing "two acts citing one leased lock in one batch: the first by the order uses it, the second :no-such-lock"
      (let [[n1 n2] (sort-by #(nth % 3) (repeatedly 2 #(do (Thread/sleep 2) (nm))))
            lid (first (keys rows))
            K (get-in rows [lid :sealed])
            a (mc/seal (assoc (offer {:facts [{:e :e0 :k :note :v 1}]}) :name n1) (constantly lid) {lid K})
            b (mc/seal (assoc (offer {:facts [{:e :e1 :k :note :v 2}]}) :name n2) (constantly lid) {lid K})
            ws (writes [b a] wl)]
        (is (= :yes (:answer (get (answers ws) n1))))
        (is (= :no-such-lock (get (faces ws) [n2 (mc/fp-of b)])))))
    (testing "the grain in force decides a citation's fit (L30): a lock cited twice under per value is :grain-mismatch, recorded, leases consumed"
      (let [o2 (offer {:facts [{:e :e0 :k :note :v 1} {:e :e1 :k :note :v 2}]})
            [pa-act prows _] (sealed o2 :grain :per-act)
            ws (writes [pa-act] (with-leases w prows))]
        (is (= :grain-mismatch (reason-of ws pa-act)))
        (is (seq (filter #(= :del-lease (first %)) ws)))
        (let [ws2 (writes [pa-act] (-> (with-leases w prows) (assoc-in [:msettings :group :grain] :per-act)))]
          (is (= :yes (:answer (answer-of ws2 pa-act))) "one lock for the act under per act"))))
    (testing "a lease act mints its rows beside its name row, sealed under its :who (M16)"
      (let [l (mc/lease-offer {:who :alice :layer :group :session :s1 :permission ps :n 3})
            ws (writes [l] w)
            mint (some #(when (= :mint (first %)) %) ws)]
        (is (= :yes (:answer (answer-of ws l))))
        (is (= [:mint (:name l) nil nil {:under :alice :session :s1 :layer :group :kind :group :owner nil :count 3 :batch 7}] mint))
        (let [rows (micro/mint-rows (nth mint 4) (get-in w [:persons :alice]))]
          (is (= 3 (count rows)))
          (is (every? (fn [[_ row]] (locks/lock? (locks/unlease row (get-in w [:persons :alice])))) rows))
          (is (empty? (micro/mint-rows (nth mint 4) {:lock nil :erased-at 3})) "a person forgotten by the mint gets no rows"))
        (is (= :malformed-control (reason-of (writes [(assoc-in l [:facts 0 :v :count] 999)] w) l)) "n in 1..256")
        (is (= :no-such-person (reason-of (writes [l] (update w :persons dissoc :alice)) l)))
        (is (= :person-forgotten (reason-of (writes [l] (assoc-in w [:persons :alice :lock] nil)) l)))
        (let [op (mc/lease-offer {:who :operator :layer :group :session :s1 :n 1})]
          (is (nil? (:under (nth (some #(when (= :mint (first %)) %) (writes [op] w)) 4))) "the operator's lease is bare ([PV-F5])"))))
    (testing "a session close deletes its session's leases in the layer ([PV-F4])"
      (let [ln (env/make-name :group :by-entity)
            close (offer {:facts [{:e :s1 :k :session-closed :v {:session :s1}}]})
            ws (writes [close] (assoc-in w [:answers :s1] [ln (env/make-name :base :by-entity)]))]
        (is (= :yes (:answer (answer-of ws close))))
        (is (= [[:del-leases ln nil nil nil]] (filter #(= :del-leases (first %)) ws)) "only the layer's names")
        (is (= :control-not-allowed (reason-of (writes [(assoc close :session :s2 :permission [:s2 :group :group pa])]
                                                       (assoc-in w [:mperms [:group [:s2 :group :group pa]]] {:granted [(nm) 1]}))
                                                close))
            "another session's close")))))

(deftest fold-record-path-checks
  (let [w (world)
        o (offer {:facts [{:e :e0 :k :mention :v {:persons #{:bob}}}]})
        [so rows ks] (sealed o)
        lid (first (keys rows))
        K (get ks lid)
        plain (locks/canonical-bytes {:persons #{:bob}})
        record (locks/wrap K (locks/wrap-of nil #{:bob} false) (:persons w) [(locks/fresh-nonce)])
        row {:layer :group :k :mention :v nil :sealed (get-in so [:facts 0 :sealed]) :lock-id lid :lock record
             :digest (locks/value-digest K plain)}
        rec {:answer :yes :stamp 9 :digest (mc/digest-of so) :batch 3}
        base (-> w (assoc-in [:names (:name so)] rec) (assoc-in [:rows [:e0 (:name so) 0]] row))]
    (testing "a resend citing the recorded lock with the same plaintext: recorded"
      (let [resend (assoc (mc/seal o (constantly lid) ks) :name (:name so))
            ws (writes [resend] base)]
        (is (= {[(:name so) (mc/fp-of resend)] :recorded} (faces ws)))
        (is (nil? (answer-of ws so)))))
    (testing "the same lock ids with other plaintext: :name-taken on the face (the value check under the recorded lock)"
      (let [other (mc/seal (assoc-in o [:facts 0 :v] {:persons #{:alice}}) (constantly lid) ks)
            other (assoc other :name (:name so))]
        (is (= :name-taken (get (faces (writes [other] base)) [(:name so) (mc/fp-of other)])))))
    (testing "after the value is forgotten (its wrap closed by Bob's forget) the same resend is answered from the record"
      (let [other (assoc (mc/seal (assoc-in o [:facts 0 :v] {:persons #{:alice}}) (constantly lid) ks) :name (:name so))
            forgotten (assoc-in base [:persons :bob] {:lock nil :erased-at 11})]
        (is (= #{:recorded} (set (vals (faces (writes [other] forgotten))))))))
    (testing "a door that lost its locks resends under a fresh lease of its own person: checked, and its rows consumed"
      (let [ln2 (env/make-name :group :by-entity)
            lid2 [ln2 0]
            K2 (locks/fresh-lock)
            alice (get-in w [:persons :alice])
            row2 (merge (locks/lease-row K2 :alice alice (locks/fresh-nonce)) {:layer :group :session :s1 :kind :group :owner nil :batch 2})
            same (assoc (mc/seal o (constantly lid2) {lid2 K2}) :name (:name so))
            diff (assoc (mc/seal (assoc-in o [:facts 0 :v] {:persons #{:alice}}) (constantly lid2) {lid2 K2}) :name (:name so))
            ws1 (writes [same] (with-leases base {lid2 row2}))
            ws2 (writes [diff] (with-leases base {lid2 row2}))]
        (is (= #{:recorded} (set (vals (faces ws1)))))
        (is (= [[:del-lease ln2 nil 0 nil]] (filter #(= :del-lease (first %)) ws1)) "its new lease rows consumed")
        (is (= :name-taken (get (faces ws2) [(:name so) (mc/fp-of diff)])))
        (is (empty? (filter #(= :del-lease (first %)) ws2)) "and left alone on :name-taken")))
    (testing "a resend citing another session's lease: answered from the record, the other session's rows untouched"
      (let [ln3 (env/make-name :group :by-entity)
            lid3 [ln3 0]
            K3 (locks/fresh-lock)
            theirs (lease-row K3 :group :s2 :under nil)
            r (assoc (mc/seal o (constantly lid3) {lid3 K3}) :name (:name so))
            ws (writes [r] (with-leases base {lid3 theirs}))]
        (is (= #{:recorded} (set (vals (faces ws)))))
        (is (empty? (filter #(= :del-lease (first %)) ws)))))))

(deftest block-2b-rows
  (let [w (world)
        persons (:persons w)]
    (testing "a shared layer's value: its lock re-wrapped under its own subjects, kept in the record (ruling 7, 7b)"
      (let [o (offer {:facts [{:e :e0 :k :mention :v {:persons #{:alice :bob}}} {:e :e1 :k :note :v {:token "n"}}
                              {:e :e2 :k :mention :v {:persons #{:alice :bob}} :mark #{:die-with-any}}]})
            [so rows ks] (sealed o)
            in (micro/intake so)
            ww (micro/row-wraps in rows {})
            frows (micro/fact-rows in ww persons (micro/fresh-nonces (micro/nonces-needed ww)))
            [[_ _ r0 l0] [_ _ r1 _] [_ _ r2 _]] frows]
        (is (= {:required [] :any-of [:alice :bob]} (select-keys (:lock r0) [:required :any-of])) "any one of them (7b as written)")
        (is (nil? l0) "no lock row in a group")
        (is (= {:required [] :any-of []} (select-keys (:lock r1) [:required :any-of])) "a note about no one: an empty wrap, K bare in the record")
        (is (= {:required [:alice :bob] :any-of []} (select-keys (:lock r2) [:required :any-of])) "marked: every subject required")
        (is (every? #(nil? (:v %)) [r0 r1 r2]))
        (testing "each opens to its value through its record, and its digest is the HMAC under its lock"
          (doseq [[i r] [[0 r0] [1 r1] [2 r2]]]
            (let [K (locks/unwrap (:lock r) persons)]
              (is (locks/same-bytes? K (get ks (get-in so [:facts i :lock-id]))))
              (is (= (get-in o [:facts i :v]) (:value (locks/decode-plain (locks/open K (:sealed r))))))
              (is (locks/same-bytes? (:digest r) (locks/value-digest K (locks/canonical-bytes (get-in o [:facts i :v]))))))))
        (testing "the A cases' wraps, both directions (a mention of Bob alone dies with Bob; about both survives either alone)"
          (let [dead (fn [& ps] (reduce #(assoc %1 %2 {:lock nil :erased-at 1}) persons ps))]
            (is (some? (locks/unwrap (:lock r0) (dead :alice))))
            (is (some? (locks/unwrap (:lock r0) (dead :bob))))
            (is (nil? (locks/unwrap (:lock r0) (dead :alice :bob))))
            (is (nil? (locks/unwrap (:lock r2) (dead :bob))) "marked dies with Bob")
            (is (some? (locks/unwrap (:lock r1) (dead :alice :bob))) "about no one survives both")))))
    (testing "a personal layer re-classed here keeps a lock row on the value's task, owner required"
      (let [o (mc/build {:who :alice :layer :alice :class :by-entity :session :s1 :permission [:s1 :alice :alice [:alice :alice :alice]]
                         :facts [{:e :e0 :k :note :v 1}]})
            [so rows _] (sealed o :kind :personal :owner :alice)
            in (micro/intake so)
            ww (micro/row-wraps in rows {})
            [[_ _ r l]] (micro/fact-rows in ww persons (micro/fresh-nonces (micro/nonces-needed ww)))]
        (is (nil? (:lock r)))
        (is (= {:required [:alice] :any-of []} (select-keys l [:required :any-of])))))
    (testing "per-act grain: one lock, one wrap over the act's union, repeated in every row (L6)"
      (let [o (offer {:facts [{:e :e0 :k :mention :v {:persons #{:bob}}} {:e :e1 :k :note :v 2}]})
            [so rows _] (sealed o :grain :per-act)
            in (micro/intake so)
            ww (micro/row-wraps in rows {})
            [[_ _ r0 _] [_ _ r1 _]] (micro/fact-rows in ww persons (micro/fresh-nonces (micro/nonces-needed ww)))]
        (is (= (:lock-id r0) (:lock-id r1)))
        (is (= [:bob] (:any-of (:lock r0)) (:any-of (:lock r1))))))
    (testing "a subject forgotten since block 1 (PV-F6): the whole any-of list kept, no blob for the forgotten one"
      (let [o (offer {:facts [{:e :e0 :k :mention :v {:persons #{:alice :bob}}}]})
            [so rows _] (sealed o)
            in (micro/intake so)
            ww (micro/row-wraps in rows {})
            gone (assoc persons :bob {:lock nil :erased-at 4})
            [[_ _ r _]] (micro/fact-rows in ww gone (micro/fresh-nonces (micro/nonces-needed ww)))]
        (is (= [:alice :bob] (:any-of (:lock r))))
        (is (= #{:alice} (set (keys (:any-blobs (:lock r))))))
        (is (some? (locks/unwrap (:lock r) gone)))))
    (testing "a lease person forgotten since block 1: the row written with no lock (admitted and closed), nothing thrown"
      (let [o (offer {:facts [{:e :e0 :k :note :v 1}]})
            [so rows ks] (sealed o)
            lid (first (keys rows))
            prow (merge (locks/lease-row (get ks lid) :alice (:alice persons) (locks/fresh-nonce)) (select-keys (get rows lid) [:layer :session :kind :owner :batch]))
            in (micro/intake so)
            ww (micro/row-wraps in {lid prow} {:alice {:lock nil :erased-at 2}})
            [[_ _ r _]] (micro/fact-rows in ww persons [])]
        (is (nil? (:lock r)))
        (is (nil? (:digest r)))
        (is (bytes? (:sealed r)))))
    (testing "control facts and retracts get no lock; a control value is plaintext EDN"
      (let [o (offer {:who :operator :permission nil :session nil :facts [{:e :group :k :lock-grain :v :per-act} {:e :e0 :k :note :v nil}]})
            in (micro/intake o)
            ww (micro/row-wraps in {} {})
            [[_ _ r0 _] [_ _ r1 _]] (micro/fact-rows in ww persons [])]
        (is (= ":per-act" (:v r0)))
        (is (nil? (:v r1)))
        (is (every? #(nil? (:lock %)) [r0 r1]))))
    (testing "block 2b's filter: only this batch's yes, for this content and this envelope"
      (let [in {:digest "d" :fp "f"}]
        (is (micro/rows-written? in {:batch 5 :answer :yes :digest "d"} "f" 5))
        (is (not (micro/rows-written? in {:batch 4 :answer :yes :digest "d"} "f" 5)) "a resend in a later batch")
        (is (not (micro/rows-written? in {:batch 5 :answer :no :digest "d"} "f" 5)))
        (is (not (micro/rows-written? in {:batch 5 :answer :yes :digest "x"} "f" 5)))
        (is (not (micro/rows-written? in {:batch 5 :answer :yes :digest "d"} "g" 5)) "another envelope under the name")))))

(deftest reads-through-the-frontier
  (testing "visible-at? is the one predicate: a row is shown at F iff its batch <= F"
    (is (micro/visible-at? {:batch 3} 3))
    (is (not (micro/visible-at? {:batch 4} 3)))
    (is (not (micro/visible-at? nil 3)))
    (is (not (micro/visible-at? {:batch 1} nil))))
  (testing "micro-lookup's answer: a face first, then the record by digest, else no answer"
    (let [rec {:answer :yes :digest "d" :batch 2 :stamp 5}]
      (is (= :no-such-lock (:reason (micro/lookup-result {:reason :no-such-lock :batch 2} rec "f" "d" 3))))
      (is (= rec (dissoc (micro/lookup-result nil rec "f" "d" 3) :frontier :decided-fp)))
      (is (= "f" (:decided-fp (micro/lookup-result nil rec "f" "d" 3))) "with the fingerprint of the envelope it was decided for")
      (is (= rec (dissoc (micro/lookup-result nil rec "f" nil 3) :frontier :decided-fp)) "a nil digest: the record as data")
      (is (= :name-taken (:reason (micro/lookup-result nil rec "f" "x" 3))))
      (is (= :no-answer (:answer (micro/lookup-result nil rec "f" "d" 1))) "a record above F is not there yet")
      (is (= :no-answer (:answer (micro/lookup-result {:reason :no-such-lock :batch 4} nil nil "d" 3))))
      (is (= 3 (:frontier (micro/lookup-result nil nil nil "d" 3))))
      (is (= (assoc rec :frontier 3 :recorded true) (micro/lookup-result {:reason :recorded :batch 3} rec nil "d" 3))
          "a :recorded trace: this envelope was answered from the record"))
    (testing "the door takes only its own envelope's answer"
      (is (mc/own-answer? {:answer :yes :stamp 1 :decided-fp "f"} "f"))
      (is (not (mc/own-answer? {:answer :yes :stamp 1 :decided-fp "g"} "f")) "decided for another envelope: not yet this one's")
      (is (mc/own-answer? {:answer :yes :stamp 1 :recorded true} "f"))
      (is (mc/own-answer? {:answer :no :reason :no-such-lock :face true} "f"))
      (is (mc/own-answer? {:answer :no :reason :name-taken} "f") "other parts: the name is taken, whatever the gate's face")
      (is (not (mc/own-answer? {:answer :no-answer} "f")))))
  (testing "faces are written keep-first ([PV-F10])"
    (is (= {:reason :a :batch 1} (micro/keep-first {:reason :a :batch 9} {:reason :a :batch 1})))
    (is (= {:reason :a :batch 9} (micro/keep-first {:reason :a :batch 9} nil))))
  (testing "the frontier a read uses"
    (is (= 4 (micro/frontier-of 4 9)))
    (is (= 9 (micro/frontier-of nil 9)))
    (is (= -1 (micro/frontier-of nil nil)))))

(deftest the-fold-is-deterministic
  (let [w (world)
        os (for [i (range 8)]
             (let [[so rows _] (sealed (offer {:facts [{:e (keyword (str "e" (mod i 3))) :k :note :v {:i i}}]}))] [so rows]))
        wl (reduce (fn [w [_ rows]] (with-leases w rows)) w os)
        raws (map first os)
        strip (fn [ws] (set (map (fn [[kind route field k v]]
                                   [kind route field k (if (map? v) (dissoc v :stamp) (if (= :task kind) :clock v))])
                                 ws)))]
    (is (= (strip (writes raws wl :wall 1000)) (strip (writes (reverse raws) wl :wall 2000)))
        "the same batch in another arrival order and at another wall: the same writes but the stamps' wall term")
    (is (= (writes raws wl :wall 1000) (writes raws wl :wall 1000)) "and the same writes exactly at the same wall")))

;; ============================================================= generated input

(def gen-layer (gen/elements [:group :base :alice-agent :nowhere]))
(def gen-e (gen/elements [:e0 :e1 :e2 :group :s1]))
(def gen-k (gen/elements [:note :mention :lease :session-closed :lock-grain :members :permission :revoke :forget :class :kind]))
(def gen-v (gen/one-of [(gen/return nil) gen/small-integer gen/keyword gen/string-ascii
                        (gen/return {:count 3}) (gen/return {:session :s1}) (gen/return #{:alice})
                        (gen/return {:persons #{:bob}}) (gen/return {:id pa}) (gen/return {:permission pb})]))

(def gen-fact
  (gen/let [e gen-e k gen-k v gen-v
            sealed? gen/boolean
            replaces (gen/elements [nil [(nm) 0]])
            mark (gen/elements [#{} #{:own-row} #{:die-with-any}])
            junk (gen/elements [nil :bad-part])]
    (cond-> {:e e :k k :replaces replaces :mark mark}
      (not sealed?) (assoc :v v)
      sealed? (assoc :sealed (locks/fresh-bytes 40) :lock-id [(nm) 0])
      junk (assoc junk 1))))

(def gen-raw
  (gen/let [who (gen/elements [:alice :bob :operator :store])
            layer gen-layer
            cls (gen/elements [:by-entity :by-layer nil])
            facts (gen/vector gen-fact 0 4)
            perm (gen/elements [nil ps pa pb [:alice :group :alice-hand] :junk])
            session (gen/elements [nil :s1 :s2])
            name-junk (gen/elements [nil :junk])]
    (cond-> {:version 1 :who who :layer layer :class (or cls :by-entity) :permission perm :session session
             :stood-on {} :subjects #{} :claimed-when 1 :facts facts
             :name (env/make-name layer cls)}
      name-junk (assoc :name name-junk))))

(deftest nothing-throws
  (let [w (world)]
    (check! "intake, the gather's pure steps and prepare never throw on generated batches" 150
            (prop/for-all [raws (gen/vector gen-raw 0 6)]
              (let [ws (writes raws w)]
                (and (vector? ws) (every? vector? ws)))))
    (check! "parse-micro and parts-digest are total" 300
            (prop/for-all [x (gen/one-of [gen-raw gen/any-printable])]
              (let [p (micro/parse-micro x)]
                (or (contains? p :refuse) (string? (micro/parts-digest (:ok p)))))))
    (check! "row-wraps and fact-rows never throw" 150
            (prop/for-all [raw gen-raw]
              (let [in (micro/intake raw)]
                (or (not= :offer (:kind in))
                    (let [ww (micro/row-wraps in {} {})]
                      (vector? (micro/fact-rows in ww (:persons w) (micro/fresh-nonces (micro/nonces-needed ww)))))))))
    (testing "prepare over nil, an empty state, and junk rows"
      (is (= {:writes []} (micro/prepare nil 1 1)))
      (is (= {:writes []} (micro/prepare {} 1 1)))
      (is (vector? (:writes (micro/prepare {[:offer :junk] 1 [:face] 2 [:clock] :x [:perm :group] nil} 1 1)))))))

(deftest the-model-on-the-same-histories
  (testing "the model's micro gate: a name taken by other content in the group is refused by digest (E1)"
    (let [st (fm/run fm/baseline [[:offer {:who :alice :layer :group :facts [{:e :e0 :k :note :replaces :none :mark #{}}] :stood-on nil :times 1}]
                                  [:batch]
                                  [:reuse 0 {:who :alice :layer :group :facts [{:e :e1 :k :note :replaces :none :mark #{}}]}]
                                  [:batch]])]
      (is (some #(re-find #"taken by other content, refused by digest" %) (:trace st)))))
  (testing "the model's micro gate decides a batch in order, each offer seeing the ones before: a revoke then a write refuses it"
    (let [st (fm/run fm/baseline [[:revoke [:alice :group :own]] [:offer {:who :alice :layer :group :stood-on nil :times 1
                                                                          :facts [{:e :e0 :k :note :replaces :none :mark #{}}]}]
                                  [:batch]])
          nm (second (distinct (:client-sent st)))]
      (is (= :permission-revoked (:reason (first (fm/answers-for st nm))))))))
