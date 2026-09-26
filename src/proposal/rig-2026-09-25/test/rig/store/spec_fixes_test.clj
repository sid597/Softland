(ns rig.store.spec-fixes-test
  "The spec fixes, H-1 and M-1 of REVIEW-full-spec.md (PLAN-spec-fixes.md):
  pure tests of the micro gate's new steps and the maintenance reads' micro
  era, then one in-process cluster of 4 tasks where Alice's agent session
  layer is re-classed by entity after it has entries, a standing read and a
  tool run, and is then read through, closed, dropped and run over.

  M-1's race (a person forgotten between the gate's read and its write)
  cannot be timed on the cluster; its two outcomes are shown over the pure
  steps block 1a now runs, and every micro write below runs them.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [clojure.test :refer [deftest is testing]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [rig.store.client :as c]
            [rig.store.clock :as hlc]
            [rig.store.envelope :as env]
            [rig.store.grammar :as grammar]
            [rig.store.inject :as inject]
            [rig.store.locks :as locks]
            [rig.store.micro :as micro]
            [rig.store.micro-client :as mc]
            [rig.store.module :as m]
            [rig.store.read-exit :as rx]
            [rig.store.reads :as reads]
            [rig.store.recipe :as recipe]
            [rig.store.runner :as runner]
            [rig.store.shared-reads :as sr]
            [rig.store.standing :as sg]
            [rig.store.toy-grammars :as tg]))

;; ================================================================ pure helpers

(def ^:private next-offer (atom 0))

(defn- fold-name
  "Ordered fixture names (UUIDv7-shaped), so the fold's order is the test's."
  [L]
  (env/make-name L :by-entity :offer (java.util.UUID. 0x7000 (bit-or Long/MIN_VALUE (swap! next-offer inc)))))

(defn- sealed
  "A built micro offer with its value facts sealed under fresh locks of one
  lease, and the lock ids by value index."
  [spec]
  (let [o (mc/build spec)
        ids (locks/lease-ids (env/make-name (:layer o) :by-entity) (max 1 (mc/value-count o)))
        ks (into {} (map (fn [id] [id (locks/fresh-lock)])) ids)]
    [(mc/seal o (mc/assign-by-grain :per-value ids) ks) ids ks]))

(defn- gathered
  "Block 1's rows for one offer as the fold reads them, with the arrival
  task's outcome `a` and the spec fixes' three skeleton fields given, the
  layer's settings `settings`, and permission rows `perms` {pid row}."
  [o {:keys [a lease-under lock-failed settings perms]}]
  (let [in (micro/intake o)
        sk (micro/skeleton in (merge {:status :ok :owned (:cited in) :value-reason nil :union #{}} a)
                           nil nil lease-under lock-failed)
        ns (micro/name-step sk nil 0 0)
        ls (micro/layer-rows sk settings nil (into {} (map (fn [[pid row]] [pid {:micro row :stream nil}])) perms) {} {})
        first-rows (into (:rows ns) (:rows ls))]
    (into {} (mapcat (fn [[i e]] (micro/entity-rows (:sk ns) e 0 0 {} first-rows (zero? i)))
                     (map-indexed vector (:entities sk))))))

(defn- fold [state] (:writes (micro/prepare state 1000 9)))

(defn- records [writes]
  (into {} (for [[kind nm field _ v] writes :when (and (= :name kind) (= :answer field))] [nm v])))

(defn- faces [writes]
  (into {} (for [[kind nm field _ v] writes :when (and (= :name kind) (= :face field))] [nm (:reason v)])))

(def ^:private group-settings {:kind :group :class :by-entity :grain :per-value})
(def ^:private agent-settings {:kind :agent :owner :alice :class :by-entity :grain :per-value})

(defn- alive [] {:lock (locks/fresh-lock) :erased-at nil})

;; ================================================================ pure: the micro gate

(deftest micro-close-takes-the-reads-part
  (let [close (fn [v] {:session :s1 :facts [{:e :s1 :k :session-closed :v v}]})]
    (is (micro/close-ok? (close {:session :s1})))
    (is (micro/close-ok? (close {:session :s1 :reads :drop})) "FRR5's part, as the stream gate takes it")
    (is (micro/close-ok? (close {:session :s1 :reads :keep})))
    (is (not (micro/close-ok? (close {:session :s1 :reads :maybe}))))
    (is (not (micro/close-ok? (close {:session :s1 :other 1}))))
    (is (not (micro/close-ok? (close {:reads :drop}))))
    (is (not (micro/close-ok? (close {:session :s2 :reads :drop}))) "the fact is about its own session")))

(deftest micro-lease-by-no-person-is-sealed-under-the-layer-owner
  (let [lease (fn [who] {:offer {:who who :layer :alice-agent :session :s :facts [{:e :s :k :lease :v {:count 4}}]}})
        a (alive)]
    (testing "W1-1 at this gate: whose entries block 1a reads, and whose lock the rows go under"
      (is (= [:agent-a :alice] (micro/lease-persons (lease :agent-a) agent-settings)))
      (is (= :alice (micro/lease-under-of (lease :agent-a) agent-settings {:agent-a nil :alice a}))
          "a writer who is no person: the layer's person owner")
      (is (= :bob (micro/lease-under-of (lease :bob) agent-settings {:bob (alive) :alice a})) "a person: the writer")
      (is (= :bob (micro/lease-under-of (lease :bob) agent-settings {:bob {:lock nil :erased-at 5} :alice a}))
          "a forgotten person is still a person: refused :person-forgotten, never moved to the owner")
      (is (= :agent-a (micro/lease-under-of (lease :agent-a) group-settings {:agent-a nil}))
          "no person owner either: the writer, refused :no-such-person")
      (is (= [] (micro/lease-persons (lease :operator) agent-settings)))
      (is (nil? (micro/lease-under-of (lease :operator) agent-settings {})) "the root actor's rows are bare")
      (is (nil? (micro/lease-under-of {:offer {:who :alice :layer :g :facts [{:e :x :k :note :v 1}]}} group-settings {}))
          "not a lease act"))
    (testing "the person check follows it; the 2-arity keeps the older rule for pure fixtures"
      (is (= [:alice] (micro/persons-to-check (lease :agent-a) {:status :ok} :alice)))
      (is (= [] (micro/persons-to-check (lease :operator) {:status :ok} nil)))
      (is (= [:agent-a] (micro/persons-to-check (lease :agent-a) {:status :ok})))
      (is (= :no-such-person (micro/person-reason [:agent-a] {:agent-a nil}))))
    (testing "the fold mints the lease's rows under the person the skeleton carries"
      (let [pid [:agent-a :group :group]
            o (mc/build {:who :agent-a :layer :group :session :s :permission pid :name (fold-name :group)
                         :facts [{:e :s :k :lease :v {:count 2}}]})
            ws (fold (gathered o {:a {:owned [] :union nil} :lease-under :alice :lock-failed false
                                  :settings group-settings
                                  :perms {pid {:granted [(fold-name :group) 0] :granted-batch 1}}}))
            [[_ _ _ _ meta]] (filter #(= :mint (first %)) ws)]
        (is (= :yes (:answer (get (records ws) (:name o)))) (pr-str (records ws)))
        (is (= :alice (:under meta)))))))

(deftest micro-yes-without-a-lock-is-the-face-gate-error
  (let [[o [lid]] (sealed {:who :operator :layer :group :session :fix :name (fold-name :group)
                           :facts [{:e :fx :k :note :v "a note"}]})
        state (fn [failed] (gathered o {:lock-failed failed :settings group-settings}))
        refused (fold (state true))
        admitted (fold (state false))]
    (testing "M-1: block 1a could not lock a value of an act its checks passed"
      (is (nil? (get (records refused) (:name o))) "no record: the name stays free for a resend")
      (is (= :gate-error (get (faces refused) (:name o))) "the unrecorded face, as the stream gate's fail! gives")
      (is (not-any? #(= :del-lease (first %)) refused) "nothing consumed"))
    (testing "and the same act locked: a yes, its lease consumed"
      (is (= :yes (:answer (get (records admitted) (:name o)))))
      (is (some #(= [:del-lease (locks/lease-name-of lid) nil (nth lid 1) nil] %) admitted)))))

(deftest micro-entry-is-stamped-after-its-moment
  (let [M (hlc/pack 5000 7)
        [o] (sealed {:who :operator :layer :group :session :fix :name (fold-name :group)
                     :facts [{:e :read-x :k :read/pattern :v {:moment {:stamp M} :role :shown}}]})
        stamp (fn [moments] (:stamp (get (records (fold (gathered o {:a {:entry-moments moments} :settings group-settings})))
                                         (:name o))))]
    (is (< M (stamp {0 {:moment {:stamp M}}})) "F1 at this gate: after the moment the arrival task opened")
    (is (> M (stamp {})) "without it the leader's wall (1000 ms) would stamp it before")
    (testing "the arrival task hands on only a read entry's moment parts"
      (is (= {1 {:moment {:stamp 4}}}
             (micro/entry-moment-parts [{:k :note} {:k :read/point} {:k :read/pattern}]
                                       {0 {:moment {:stamp 3}} 1 {:moment {:stamp 4} :fid [:x 0] :shown :value}})))
      (is (= {} (micro/entry-moment-parts nil {0 1}))))))

(deftest micro-lock-work-comes-from-the-reads-the-decision-used
  (let [alice (alive) bob (alive)
        persons {:alice alice :bob bob}
        lease-rows (fn [o ks under]
                     (into {} (map (fn [[id K]] [id (merge (locks/lease-row K under (get persons under) (locks/fresh-nonce))
                                                            {:layer :group :session :fix :kind :group :owner nil :batch 1})]))
                           ks))
        grammars (grammar/grammars-of (tg/rows))
        work (fn [in lrows ps]
               (let [arr (micro/arrival-open in lrows ps grammars)
                     ww (micro/row-wraps in lrows ps (:subjects arr))]
                 [arr (micro/fact-rows in ww ps (micro/fresh-nonces (micro/nonces-needed ww)))]))
        record-of (fn [[_ _ row lrow]] (or (:lock row) lrow))]
    (testing "M-1, a wrap person forgotten after the read: the value reads erased on the forget's date, never :does-not-open"
      (let [[o _ ks] (sealed {:who :alice :layer :group :session :fix :permission [:alice :group :group]
                              :facts [{:e :e1 :k :mention :v {:persons #{:bob}}}]})
            in (micro/intake o)
            [arr frows] (work in (lease-rows o ks :alice) persons)
            [[_ _ row :as frow]] frows]
        (is (= #{:bob} (:union arr)))
        (is (false? (micro/lock-failed? in arr nil frows)))
        (is (= [:bob] (:any-of (record-of frow))) "7b as written: any one of its subjects")
        (is (= {:persons #{:bob}} (:value (locks/open-with row 9 nil nil (record-of frow) persons))))
        (is (= {:erased-at 77} (locks/open-with row 9 nil nil (record-of frow) {:bob {:lock nil :erased-at 77}})))))
    (testing "A3 in the race: a group note about no one, leased by Bob, still opens after Bob's forget"
      (let [[o _ ks] (sealed {:who :bob :layer :group :session :fix :permission [:bob :group :group]
                              :facts [{:e :e0 :k :note :v "about no one"}]})
            in (micro/intake o)
            lrows (lease-rows o ks :bob)
            [arr frows] (work in lrows persons)
            [[_ _ row :as frow]] frows
            gone {:alice alice :bob {:lock nil :erased-at 77}}]
        (is (false? (micro/lock-failed? in arr nil frows)))
        (is (= {:value "about no one" :stamp 9} (locks/open-with row 9 nil nil (record-of frow) gone))
            "its lock was wrapped at the gate's read, while Bob's lease still opened")
        (testing "where block 2b's second read would have found Bob gone, the rows cannot be locked: never admitted"
          (let [ww (micro/row-wraps in lrows gone (:subjects arr))
                late (micro/fact-rows in ww gone [])]
            (is (nil? (:lock (nth (first late) 2))))
            (is (true? (micro/lock-failed? in arr nil late)))))))
    (testing "an act refused at its checks makes no rows, and is not a lock failure"
      (let [[o _ ks] (sealed {:who :alice :layer :group :session :fix :permission [:alice :group :group]
                              :facts [{:e :e1 :k :mention :v {:persons #{:bob}}}]})
            in (micro/intake o)
            arr (micro/arrival-open in (lease-rows o ks :alice) persons grammars)]
        (is (false? (micro/lock-work-due? in arr :person-forgotten)))
        (is (false? (micro/lock-failed? in arr :person-forgotten nil)))
        (is (true? (micro/lock-work-due? {:cited []} {:status :ok} nil)) "an act that cites no lock: its rows need none")))))

(deftest micro-lookup-many-takes-only-names
  (let [nm (env/make-name :g :by-entity)]
    (is (= [nm] (micro/valid-names [nm :junk [1 2]])))
    (is (= [] (micro/valid-names :junk)))
    (is (vector? (first (micro/valid-names [(subvec (into [:pad] nm) 1)]))))))

;; ================================================================ pure: names and maintenance reads

(deftest a-run-name-follows-the-class-with-the-same-id
  (let [tf [(env/make-name :alice-agent :by-layer) 0]
        mf [(env/make-name :alice-agent :by-layer) 1]
        a (recipe/run-name :alice-agent tf mf)
        b (recipe/run-name :alice-agent :by-entity tf mf)]
    (is (= :by-layer (nth a 1)))
    (is (= :by-entity (nth b 1)))
    (is (= (nth a 3) (nth b 3)) "one id under either tag")
    (is (= a (recipe/run-name :alice-agent :by-layer tf mf)))
    (is (= {:answer :yes} (runner/run-record {a {:answer :yes}} b)) "found under the other tag")
    (is (nil? (runner/run-record {} b)))))

(deftest micro-era-maintenance-reads
  (let [nm1 (env/make-name :w :by-entity) nm2 (env/make-name :w :by-entity) nm3 (env/make-name :w :by-entity)
        e (fn [k nm b & {:as more}] (merge {:k k :e :read-1 :fid [nm 0] :stamp (+ 100 b) :batch b} more))
        ents [["a" (e :read/pattern nm1 3)]
              ["b" (e :read/point nm2 5)]
              ["c" (e :read/point nm3 7)]
              ["d" (e :note nm1 3)]
              ["e" (e :read/point nm2 4 :erased-at 9)]]]
    (testing "the drop's candidates: live read entries decided at or below the close's batch"
      (is (= [[[nm1 0] 103 :read-1] [[nm2 0] 105 :read-1]] (sr/micro-id-candidates ents 5)))
      (is (= [] (sr/micro-id-candidates ents nil))))
    (testing "the page: ids of the session, in page order, whatever order the hops came back in"
      (let [rows (sr/micro-id-rows ents ents 64 nil (sr/micro-id-candidates ents 7))
            back (-> (mapv (fn [r] (if (= :cand (:kind r)) (assoc r :session (if (= nm2 (:nm r)) :s2 :s1)) r)) rows) rseq vec)]
        (is (= [[[nm1 0] 103 :read-1] [[nm3 0] 107 :read-1]] (:ids (sr/micro-ids-answer back :s1))))
        (is (true? (:done? (sr/micro-ids-answer back :s1))))
        (is (= {:refused :bad-read} (sr/micro-ids-answer [{:kind :refused :reason :bad-read}] :s1)))))
    (testing "the crash close: opens the micro era did not close, and every entity it closed"
      (let [opens [["x" {:e :r1 :fid [nm1 0]}] ["y" {:e :r2 :fid [nm2 0]}]]
            closes [["z" {:e :r2 :fid [nm3 0]}] ["w" {:e :r0 :fid [nm3 1]}]]
            rows (sr/micro-open-rows opens closes)]
        (is (= [{:kind :open :ent :r1 :nm nm1} {:kind :closed :ent :r0} {:kind :closed :ent :r2}] rows))
        (is (= {:open [:r1] :closed [:r0 :r2]} (sr/micro-open-answer (mapv #(assoc % :session :s1) rows) :s1)))
        (is (= {:open [] :closed [:r0 :r2]} (sr/micro-open-answer (mapv #(assoc % :session :s1) rows) :s9)))))
    (testing "the closing values take a kind's micro-era line over the stream era's"
      (is (= :m (sr/line-or :m :s)))
      (is (= :s (sr/line-or nil :s))))))

;; ================================================================ the cluster

(def ^:private agent-tool
  "A test tool in Alice's agent layer (tools_test's `:mention-count`, moved):
  for each mention, a note counting the people it names."
  {:e :fix-count :k :tool
   :v {:matches [:k :mention]
       :signature {:in :match :out #{:note} :rows? false}
       :permission [:fix-count :alice-agent :alice-agent]
       :recipe [{:name :count-note :do :emit :e [:in :e] :k :note
                 :v [:map {:token [:str [:count [:in :v :persons]] " named"]}]}]}})

(def ^:private bot-pid [:bot :alice-agent :alice-agent [:alice :alice-agent :alice-agent]])

(defn- bot
  "The agent reading for Alice in her agent session layer, in session `s`."
  [s]
  {:reader :bot :reader-kind :model :for :alice :working :alice-agent :permission bot-pid :session s})

(defn- wait-until
  "Poll `f` until it gives a truthy value or 60 s pass; its last value."
  [f]
  (let [deadline (+ (System/currentTimeMillis) 60000)]
    (loop []
      (let [v (try (f) (catch Exception _ nil))]
        (if (or v (> (System/currentTimeMillis) deadline)) v (do (Thread/sleep 50) (recur)))))))

(defn- opened
  "An act's rows on entity `e` as the micro store opens them."
  [st e nm]
  (mc/open-act st e nm))

(defn- closed-lines
  "The micro era's closing lines of a standing read's entry entity in
  Alice's agent layer, found through its index and opened where they
  live."
  [st ent]
  (let [es (wait-until #(seq (for [[_ e] (foreign-select [(keypath :alice-agent :ix-ek) ALL] (:micro-state st))
                                   :when (and (= ent (:e e)) (= :read/closed (:k e)))]
                               e)))]
    (vec (for [e es] (:value (first (opened st ent (first (:fid e)))))))))

(deftest a-re-classed-working-layer
  (inject/reset-all!)
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc m/Store {:tasks 4 :threads 2 :workers 1})
    (let [st (rx/connect ipc)
          ok! (fn [o] (let [a (c/offer-until-answered! st o)]
                        (is (= :yes (:answer a)) (pr-str [(:layer o) (map :k (:facts o)) a]))
                        a))
          alice-in (fn [layer facts]
                     (c/build {:who :alice :layer layer :class :by-layer :permission [:alice layer layer] :facts facts}))
          alice-reads (fn [layer read]
                        (rx/read! st {:reader :alice :reader-kind :person :working :alice-hand
                                      :permission [:alice :alice-hand :alice-hand] :layer layer :read read}))]

      (testing "seed: the model's one-owner world, the toy grammars, and in Alice's agent layer the bot's and the tool's permissions, the tool and a mention"
        (is (every? #(= :yes (:answer %)) (c/seed! st)))
        (is (every? #(= :yes (:answer %)) (tg/write! st [:alice :alice-agent])))
        (ok! (c/grant-offer st bot-pid))
        (ok! (c/build {:who :operator :layer :alice-agent :class :by-layer :facts [agent-tool]}))
        (ok! (c/grant-offer st (get-in agent-tool [:v :permission])))
        (ok! (alice-in :alice [{:e :n1 :k :note :v "first note"}]))
        (ok! (alice-in :alice-agent [{:e :m1 :k :mention :v {:persons #{:bob}}}])))

      (let [e1 (rx/read! st (merge (bot :s1) {:layer :alice :read [:pattern [:k :note]]}))
            held (sg/subscribe! st (merge (bot :s2) {:layer :alice :pattern [:k :note]}))
            pass1 (runner/run-pass! st {:layer :alice-agent})]

        (testing "the stream era: an entry, a standing read and a tool run, each named :by-layer"
          (is (= ["first note"] (mapv :value (:rows e1))) (pr-str e1))
          (is (= :by-layer (nth (:entry e1) 1)))
          (is (:handle held) (pr-str held))
          (is (= [:yes] (mapv :answer (:runs (first (:tools pass1))))) (pr-str pass1)))

        (testing "the re-class of Alice's agent session layer, the hot layer ruling 2's way out is for (OP7)"
          (is (= :yes (:answer (c/offer-until-answered! st (mc/reclass-offer st :alice-agent)))))
          (is (= :by-entity (:class (c/settings st :alice-agent)))))

        (let [r (rx/read! st (merge (bot :s1) {:layer :alice :read [:pattern [:k :note]]}))
              nfid (first (first (:matched r)))
              p (rx/read! st (merge (bot :s1) {:layer :alice :read [:point [nfid]]}))]

          (testing "H-1: the agent reads through the one exit after the re-class; its entry is a :by-entity act the micro gate decided"
            (is (= ["first note"] (mapv :value (:rows r))) (pr-str r))
            (is (= ["first note"] (mapv :value (:rows p))) (pr-str p))
            (doseq [x [r p]
                    :let [nm (:entry x)
                          rec (mc/record-of st nm)
                          ent (reads/entry-entity nm)]]
              (is (= :by-entity (nth nm 1)))
              (is (= [:yes :bot :s1 :by-entity] ((juxt :answer :who :session :class) rec)) (pr-str rec))
              (is (< (get-in x [:moment :stamp]) (:entry-stamp x)) "stamped after the moment it records (F1)")
              (is (= (:entry-stamp x) (:stamp rec)))
              (let [row (mc/row-of st ent [nm 0])]
                (is (nil? (:lock row)) "an :own-row line: its lock is not in the record")
                (is (= [:alice] (:required (mc/lock-row st ent (:lock-id row))))
                    "a lock row by its entity, wrapped under the owner the bot's lease went under (W1-1)")))
            (is (= [{:layer :alice :moment (:moment r) :role :shown :pattern [:k :note] :mark :complete :count 1
                     :fingerprint (:fingerprint r) :fp-secret (:fp-secret r) :exact (:matched r)}]
                   (mapv :value (opened st (reads/entry-entity (:entry r)) (:entry r))))
                "the line's value, as the stream gate records it, opened from the micro store"))

          (testing "a standing read opened before the re-class delivers and closes after it; its closing values count both eras"
            (ok! (alice-in :alice [{:e :n2 :k :note :v "second note"}]))
            (let [h (:handle held)
                  ent (:ent @h)
                  d (sg/deliver! st h)
                  cl (sg/unsubscribe! st h)
                  closing (:value (first (opened st ent (:name cl))))]
              (is (= ["second note"] (mapv :value (:rows d))) (pr-str d))
              (is (= :by-entity (nth (:entry d) 1)))
              (is (= :yes (:answer cl)) (pr-str cl))
              (is (= 2 (:deliveries closing)) (pr-str closing))
              (is (= :unsubscribe (:closed-by closing)))
              (is (= :alice (:layer closing)))))

          (testing "a session's close finds the standing read a crashed door left open in the micro era, and not one the micro era closed"
            (let [s3 (sg/subscribe! st (merge (bot :s3) {:layer :alice :pattern [:e :n1]}))]
              (is (:handle s3) (pr-str s3))
              (is (= {:closed 0 :crash 1} (sg/close-session! st (bot :s3))))
              (is (= [:crash] (mapv :closed-by (closed-lines st (:ent @(:handle s3))))) "closed :crash"))
            (is (= {:closed 0 :crash 0} (sg/close-session! st (bot :s2)))
                "the stream-era opening that the micro era closed is not open"))

          (testing "a session close with :reads :drop at the micro gate, and the operator's drop of both eras' entries"
            (let [c1 (rx/close-session! st :bot :alice-agent :s1 :reads :drop :permission bot-pid)]
              (is (= :yes (:answer c1)) (pr-str c1))
              (is (= :by-entity (nth (:name c1) 1)))
              (is (= 3 (rx/drop-reads! st :operator :alice-agent :s1 (:name c1) (:stamp c1)))
                  "one forget per entry fact: the stream era's e1, then r and p at the micro gate")
              (is (contains? (c/opens? st :alice-agent [(:entry e1) 0]) :erased-at) "the stream-era entry, at the stream gate (M25)")
              (doseq [x [r p]]
                (is (every? #(contains? % :erased-at) (opened st (reads/entry-entity (:entry x)) (:entry x)))
                    "each micro-era entry erased, with its date"))
              (is (some #(= [:s1 :drop (:stamp c1) (:name c1)] %)
                        (wait-until #(let [cs (rx/session-closes st :alice-agent)]
                                       (when (some (fn [x] (= (:name c1) (nth x 3))) cs) cs))))
                  "the micro-era close is found from the record")
              (is (= {:alice-agent 0} (rx/resume-drops! st :operator [:alice-agent])) "a rerun finds nothing left")))

          (testing "For Sid 41 as built: Alice's own drop cannot forget a micro-era entry (M14), so it stays"
            (let [q (rx/read! st (merge (bot :s4) {:layer :alice :read [:pattern [:k :note]]}))
                  c4 (rx/close-session! st :bot :alice-agent :s4 :reads :drop :permission bot-pid)]
              (is (= :yes (:answer c4)))
              (is (= 0 (rx/drop-reads! st :alice :alice-agent :s4 (:name c4) (:stamp c4))))
              (is (every? #(contains? % :value) (opened st (reads/entry-entity (:entry q)) (:entry q)))))))

        (testing "the runner in the re-classed layer: the match run before the re-class runs no more; a new one runs at the micro gate, once"
          (let [m2 (mc/write! st {:who :alice :layer :alice-agent :session :sa :permission [:alice :alice-agent :alice-agent]
                                  :facts [{:e :m2 :k :mention :v {:persons #{:alice :bob}}}]})
                _ (is (= :yes (get-in m2 [:answer :answer])) (pr-str (:answer m2)))
                pass2 (runner/run-pass! st {:layer :alice-agent})
                runs2 (:runs (first (:tools pass2)))
                fresh (remove :recorded runs2)
                pass3 (runner/run-pass! st {:layer :alice-agent})
                runs3 (:runs (first (:tools pass3)))]
            (is (= 2 (count runs2)) (pr-str pass2))
            (is (= 1 (count (filter :recorded runs2))) "m1's run, found under its :by-layer name")
            (is (= [:yes] (mapv :answer fresh)) (pr-str fresh))
            (is (= [:by-entity] (mapv #(nth (:name %) 1) fresh)))
            (is (= 2 (count runs3)))
            (is (every? :recorded runs3) (pr-str runs3))
            (is (= #{{:token "1 named"} {:token "2 named"}}
                   (set (map :value (:rows (alice-reads :alice-agent [:pattern [:k :note]])))))
                "one note per match, across both eras")))))))
