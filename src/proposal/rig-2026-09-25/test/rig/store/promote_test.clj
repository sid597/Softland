(ns rig.store.promote-test
  "Promotion on an in-process cluster (PLAN-promotion.md, 'Namespaces and
  tests', T1 to T14, and the hold hook's own test). One cluster serves the
  cases in order: the one-owner world and the base on the stream gate
  first (T10 a and c, T6 on the stream gate, before any group), then the
  group, made with the base's re-class, and every case into it.

  The model's B cases (T1 to T4) are played as scenarios.clj plays them and
  their `:shown` sequences and `:values` compared with the model's. A
  person forget is irreversible, so B2 and B4 are played with a fresh
  person in Alice's part (`:ann`, `:ava`), and T13 with `:abe` and `:bea`,
  so one cluster serves every case.

  Holds (PR15, R3) stop the stream gate's continuation at a named point
  without blocking a task thread; the micro gate is held by pausing its
  topology. Tests assert 'at least once' for replays, never an exact
  count (R4). Every run waits on the cluster lock.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [clojure.test :refer [deftest is testing]]
            [com.rpl.rama :as rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [formal.scenarios :as fs]
            [rig.store.box :as box]
            [rig.store.client :as c]
            [rig.store.envelope :as env]
            [rig.store.inject :as inject]
            [rig.store.locks :as locks]
            [rig.store.micro-client :as mc]
            [rig.store.module :as m]
            [rig.store.promote-client :as pc]
            [rig.store.promote-shape :as ps]
            [rig.store.read-exit :as rx]
            [rig.store.toy-grammars :as tg]))

;; ================================================================ helpers

(defn- say [& xs] (apply println "OBSERVED" xs) (flush))

(defn- wait-until
  ([f] (wait-until f 60000))
  ([f ms]
   (let [deadline (+ (System/currentTimeMillis) ms)]
     (loop []
       (let [v (try (f) (catch Exception _ nil))]
         (if (or v (> (System/currentTimeMillis) deadline))
           v
           (do (Thread/sleep 50) (recur))))))))

(def ^:private root [:group :group :group])
(defn- gp [p] [p :group :group root])
(def ^:private base-root [:operator :base :base])
(defn- bp [p] [p :base :base base-root])

(defn- note!
  "`who`'s value act into their one-owner layer: one fact on `e` (key
  `:note` unless `:k`); its fact id, answer and offer."
  [st who layer e v & {:as more}]
  (let [o (c/build (merge {:who who :layer layer :class :by-layer :permission [who layer layer]
                           :facts [{:e e :k (or (:k more) :note) :v v}]}
                          (dissoc more :k)))
        a (c/offer-until-answered! st o)]
    {:fid [(:name o) 0] :answer a :offer o :stamp (:stamp a)}))

(defn- spec-into
  "A promotion spec: `who`'s value `src` in their own layer, into `target`."
  [who src target class s & {:as more}]
  (merge {:who who :layer who :source src :target target :class class :session s
          :landing-permission (if (= :group target) (gp who) (bp who))}
         more))

(defn- status [st p] (pc/promotion-status st (get-in p [:offer :layer]) (:name p) nil))
(defn- status-kw [st p] (:status (status st p)))
(defn- status-as-of [st p T] (:status (pc/promotion-status st (get-in p [:offer :layer]) (:name p) T)))

(defn- settled!
  "Wait for a promotion to end, done or refused; its status."
  [st p]
  (wait-until #(let [s (status st p)] (when (#{:done :refused} (:status s)) s))))

(defn- req-value [p] (get-in p [:offer :facts 0 :v]))
(defn- lname-of [p] (let [v (req-value p)] (env/landing-name (:name p) (:target v) (:class v))))
(defn- cname-of [p] (env/crossing-name (:name p)))
(defn- lease-name-of [p] (first (:lease (req-value p))))
(defn- forward-of [st p] (foreign-select-one [(keypath (get-in p [:offer :layer]) :forwards (:name p))] (:layers st)))

(defn- promote-held!
  "Hold the continuation at `point` for a request whose name is made first,
  then promote."
  [st spec point]
  (let [u (env/uuid7)]
    (inject/hold! point (ps/request-name (:layer spec) u))
    (pc/promote! st (assoc spec :uuid u))))

(defn- promote-with!
  "The door's two steps, with `between` run after the landing lease and
  before the request: pausing the micro gate there holds the landing in
  its depot once it is forwarded (phase 3's batch hold)."
  [st {:keys [who source] :as spec} between]
  (let [u (env/uuid7)
        L (:layer spec) T (:target spec) C (:class spec) s (:session spec)
        lease (pc/lease-landing! st {:who who :target T :class C :session s
                                     :permission (:landing-permission spec) :uuid u})
        _ (between)
        o (pc/request-offer {:who who :layer L :permission [who L L] :session s :source source
                             :source-stamp (:stamp (c/record st (first source)))
                             :source-e (:e (c/raw-row st L source))
                             :target T :class C :public (:public lease)
                             :landing-permission (:landing-permission spec)
                             :replaces (:replaces spec) :subjects (:subjects spec) :uuid u})
        a (c/offer-until-answered! st o)]
    {:name (:name o) :uuid u :offer o :lease lease
     :request (dissoc a :crossing :landing) :crossing (:crossing a)}))

(defn- depot-records
  "Every record of a depot, from every partition, read raw."
  [d]
  (let [n (:num-partitions (rama/foreign-object-info d))]
    (vec (for [p (range n)
               :let [{:keys [start-offset end-offset]} (rama/foreign-depot-partition-info d p)]
               r (rama/foreign-depot-read d p start-offset end-offset)]
           r))))

(defn- micro-records-named [st nm] (filterv #(= nm (:name %)) (depot-records (:micro-depot st))))
(defn- offers-records-named [st nm] (filterv #(= nm (:name %)) (depot-records (:depot st))))

(defn- micro-copy
  "The copy a landing wrote in a shared layer, read through the frontier
  (phase 3's reader): its one row's open result."
  [st e lname]
  (first (mc/open-act st e lname)))

(defn- spki-of-u
  "A well-formed 44-byte X25519 public key whose u-coordinate is the byte
  `u` (little-endian): u = 0 and u = 1 are small-order points."
  [u]
  (let [out (byte-array 44)]
    (System/arraycopy (byte-array (map unchecked-byte [0x30 0x2a 0x30 0x05 0x06 0x03 0x2b 0x65 0x6e 0x03 0x21 0x00])) 0 out 0 12)
    (aset-byte out 12 (unchecked-byte u))
    out))

(defn- make-promoter!
  "A fresh person with a personal layer, her permission there, and her
  permission in the group (through the micro gate)."
  [st p]
  (c/make-person! st p)
  (c/offer-until-answered! st (c/make-layer-offer p {:kind :personal :owner p}))
  (c/offer-until-answered! st (c/grant-offer st [p p p]))
  ;; phase 6: the toy grammars as facts in her layer (D-P4; wave 2's merge, W2-2),
  ;; so T13's :mention names Bea in the source as the constant did
  (tg/write! st [p])
  (mc/offer! st (mc/grant-offer (gp p))))

(defn- b-case
  "The model's B case by its label's start: its expected `:values` and
  `:shown`."
  [prefix]
  (some (fn [[label _ expected]] (when (.startsWith ^String label prefix) expected)) fs/b-cases))

;; ============================================================ the cluster

(deftest promotion
  (inject/reset-all!)
  (with-open [ipc (rtest/create-ipc)]
    (let [tasks (or (some-> (System/getenv "RIG_PROMOTE_TASKS") Long/parseLong) (rand-nth [2 4 8]))
          _ (rtest/launch-module! ipc m/Store {:tasks tasks :threads 2 :workers 1})
          st (merge (rx/connect ipc) (pc/connect ipc))
          mn (:module-name st)
          pause! #(rtest/pause-microbatch-topology! ipc mn "micro")
          resume! #(rtest/resume-microbatch-topology! ipc mn "micro")
          alice-reader {:reader :alice :reader-kind :person :working :alice-hand
                        :permission [:alice :alice-hand :alice-hand]}
          exit-open (fn [fid] (first (:rows (rx/read! st (merge alice-reader {:layer :alice :read [:point [fid]]})))))]
      (say "tasks" tasks)

      (testing "seed: the one-owner world, then the base on the stream gate, no group yet (default 6)"
        (is (every? #(= :yes (:answer %)) (c/seed! st)))
        (is (every? #(= :yes (:answer %)) (mc/make-base! st)))
        ;; phase 6: the toy grammars as facts in the seeded layers and the base (D-P4; W2-2)
        (is (every? #(= :yes (:answer %)) (tg/write! st [:alice :alice-hand :alice-agent :base])))
        (is (= :by-layer (:class (c/settings st :base)))))

      ;; ------------------------------------------ the base on the stream gate
      (let [n10 (note! st :alice :alice :t10a "into the base, before its re-class")
            p10 (pc/promote! st (spec-into :alice (:fid n10) :base :by-layer :s/t10a))]
        (testing "T10 a: into the base while one-owner: the stream gate's hop, no depot on the road"
          (is (= :yes (:answer (:request p10))))
          (is (= :yes (get-in p10 [:crossing :answer])))
          (is (= :yes (get-in p10 [:landing :answer])) "the landing's answer rides the request's ack")
          (is (= :done (:status p10)) "decided in the request's own event tree")
          (is (= [:base :by-layer :landing (:uuid p10)] (lname-of p10)))
          (is (= :yes (:answer (c/record st (lname-of p10)))))
          (is (empty? (micro-records-named st (lname-of p10))) "no record in the micro depot")
          (is (empty? (offers-records-named st (lname-of p10))) "nor in *offers: the store's steps never pass a depot there")
          (is (= {:value "into the base, before its re-class"} (c/opens? st :base [(lname-of p10) 0]))
              "the copy opens in the base")
          (is (empty? (c/lease-rows st :base :s/t10a)) "the landing lease consumed")
          (is (nil? (:route (forward-of st p10))) "its forward has no route: a stream target")
          (is (= :done (:key (:statement p10)))))

        (testing "T8, in the base: a forget of the source after the landing leaves the copy; the copy's own forget reaches it"
          (c/forget-value! st :alice :alice (:fid n10))
          (is (contains? (c/opens? st :alice (:fid n10)) :erased-at) "the source reads erased")
          (is (= {:value "into the base, before its re-class"} (c/opens? st :base [(lname-of p10) 0])) "the copy stays")
          (let [a (c/forget-value! st :operator :base [(lname-of p10) 0])]
            (is (= :yes (:answer a)))
            (is (= :excised (:how a)) "the base keeps locks in the record: the operator's excision")
            (is (contains? (c/opens? st :base [(lname-of p10) 0]) :erased-at) "then nothing opens the copy")
            (is (nil? (:lock (c/raw-row st :base [(lname-of p10) 0]))) "its record lock is gone"))))

      (testing "T6 on the stream gate: a retried request lands once"
        (let [n (note! st :alice :alice :t6b "retried into the base")
              u (env/uuid7)
              req (ps/request-name :alice u)
              _ (inject/watch! req (env/crossing-name req) [:base :by-layer :landing u])
              _ (inject/hold! :before-read-out req)
              p (pc/promote! st (assoc (spec-into :alice (:fid n) :base :by-layer :s/t6b) :uuid u))
              r1 (pc/resend! st p)
              r2 (pc/resend! st p)
              _ (inject/release! :before-read-out req)
              _ (inject/hold! :before-forward req)
              r3 (pc/resend! st p)
              _ (inject/release! :before-forward req)
              r4 (pc/resend! st p)
              r5 (pc/resend! st p)
              lname (lname-of p)]
          (is (= :pending (:status p)))
          (is (= 1 (count (distinct (map #(select-keys % [:answer :reason :stamp]) [(:request p) r1 r2 r3 r4 r5]))))
              "every resend's ack carries the same request answer")
          (is (every? nil? (map :crossing [r1 r2])) "held before the read-out: no crossing")
          (is (= 1 (count (distinct (map :crossing [r3 r4 r5])))) "the same crossing answer on every later resend")
          (is (= :yes (get-in r4 [:landing :answer])))
          (is (= (get-in r4 [:landing :stamp]) (get-in r5 [:landing :stamp])) "the landing answered from its record")
          (is (= 1 (inject/count-of :before-writes (env/crossing-name req))) "the crossing decided once")
          (is (= 1 (inject/count-of :before-writes lname)) "the landing decided once")
          (is (= {:value "retried into the base"} (c/opens? st :base [lname 0])))
          (is (empty? (c/lease-rows st :base :s/t6b)))
          (is (= :done (status-kw st p)))))

      (testing "D6: on the stream gate, a landing whose lease was taken in another session is recorded :landing-lock-gone"
        (let [n (note! st :alice :alice :d6 "leased in one session, requested in another")
              u (env/uuid7)
              lease (pc/lease-landing! st {:who :alice :target :base :class :by-layer :session :s/d6a
                                           :permission (bp :alice) :uuid u})
              o (pc/request-offer {:who :alice :layer :alice :permission [:alice :alice :alice] :session :s/d6b
                                   :source (:fid n) :source-stamp (:stamp n) :source-e :d6
                                   :target :base :class :by-layer :public (:public lease)
                                   :landing-permission (bp :alice) :uuid u})
              a (c/offer-until-answered! st o)]
          (is (= :yes (get-in a [:crossing :answer])))
          (is (= [:no :landing-lock-gone] ((juxt :answer :reason) (:landing a)))
              "recorded, so the promotion ends refused (no forger reaches this gate)")
          (is (= :refused (:status (pc/promotion-status st :alice (:name o) nil))))
          (is (= 1 (count (c/lease-rows st :base :s/d6a)))
              "the other session's row stays until that session closes (open question 2)")))

      (testing "the hold hook: a hold ends the record's processing at its point and never blocks the task"
        (let [n (note! st :alice :alice :h1 "held")
              u (env/uuid7)
              req (ps/request-name :alice u)
              _ (inject/hold! :before-read-out req)
              p (pc/promote! st (assoc (spec-into :alice (:fid n) :base :by-layer :s/h1) :uuid u))
              t0 (System/currentTimeMillis)
              other (note! st :alice :alice :h2 "decided while the hold stands")
              t1 (System/currentTimeMillis)]
          (testing "held before the read-out: the request answered, no crossing, and the task never blocked"
            (is (inject/held? :before-read-out req))
            (is (= :yes (get-in p [:request :answer])))
            (is (nil? (:crossing p)))
            (is (nil? (c/record st (env/crossing-name req))))
            (is (= :yes (get-in other [:answer :answer])) "another act on the same layer's home task is decided while held")
            (is (< (- t1 t0) 15000) "promptly: nothing waits on the hold")
            (is (nil? (:crossing (pc/resend! st p))) "a resend while held stops there too")
            (is (= :pending (status-kw st p))))
          (testing "released, and held before the forward: the resend reads out, and stops before the landing"
            (inject/release! :before-read-out req)
            (is (not (inject/held? :before-read-out req)))
            (inject/hold! :before-forward req)
            (let [r (pc/resend! st p)]
              (is (= :yes (get-in r [:crossing :answer])))
              (is (nil? (:landing r)))
              (is (nil? (c/record st (lname-of p))))
              (is (= :crossed (status-kw st p)))))
          (testing "released: the next resend sends the stored forward, and it lands"
            (inject/release! :before-forward req)
            (let [r (pc/resend! st p)]
              (is (= :yes (get-in r [:landing :answer])))
              (is (= :done (status-kw st p)))
              (is (= {:value "held"} (c/opens? st :base [(lname-of p) 0])))))
          (testing "reset-all! clears every hold"
            (inject/hold! :before-read-out req)
            (inject/reset-all!)
            (is (not (inject/held? :before-read-out req))))))

      ;; the base's T10 c: the request before the re-class, the landing after it
      (let [n (note! st :alice :alice :t10c "crossed before the base's re-class")
            p (promote-held! st (spec-into :alice (:fid n) :base :by-layer :s/t10c) :before-forward)]
        (testing "T10 c (1): crossed, the forward held"
          (is (= :crossed (:status p)))
          (is (seq (c/lease-rows st :base :s/t10c)) "its landing lease waits on the stream gate"))

        (testing "seed: the group, made with the base's re-class (default 6)"
          (let [g (mc/make-group! st :group (:group mc/shared-world))]
            (is (= :yes (get-in g [:reclass :answer :answer])))
            (is (= :yes (get-in g [:made :answer])))
            (is (= :by-entity (:class (c/settings st :base))))))

        (testing "T10 c (2): released, the landing named for the stream gate reaches it, which no longer orders the base"
          (inject/release! :before-forward (:name p))
          (pc/resend! st p)
          (let [s (settled! st p)]
            (is (= :refused (:status s)))
            (is (= :landing (:at s)))
            (is (= :class-mismatch (:reason s)) "recorded, so the promotion ends")
            (is (= :class-mismatch (:reason (c/record st (lname-of p))))))
          (is (empty? (c/lease-rows st :base :s/t10c)) "the lease consumed with the refusal")
          (is (= {:value "crossed before the base's re-class"} (c/opens? st :alice (:fid n)))
              "the source untouched: a promotion is not a forget")))

      ;; ------------------------------------------------------ into the group
      (testing "T10 b: into the base after its re-class: through *micro-offers"
        (let [n (note! st :alice :alice :t10b "into the base, after its re-class")
              p (pc/promote! st (spec-into :alice (:fid n) :base :by-entity :s/t10b))
              s (settled! st p)]
          (is (= :done (:status s)))
          (is (= [:base :by-entity :landing (:uuid p)] (lname-of p)))
          (is (seq (micro-records-named st (lname-of p))) "a landing record in the micro depot")
          (is (= "into the base, after its re-class" (:value (micro-copy st :t10b (lname-of p)))))))

      (make-promoter! st :ann)
      (make-promoter! st :ava)

      (let [;; B1, Alice: a value forget queued before the read-out
            n1 (note! st :alice :alice :t1 "B1: forgotten before the read-out")
            p1 (promote-held! st (spec-into :alice (:fid n1) :group :by-entity :s/t1) :before-read-out)
            shown1a (status-kw st p1)
            f1 (c/forget-value! st :alice :alice (:fid n1))
            shown1b (status-kw st p1)
            _ (inject/release! :before-read-out (:name p1))
            r1 (pc/resend! st p1)
            s1 (status st p1)
            b1 (b-case "B: a value forget queued before the read-out")
            ;; B2, Ann in Alice's part: her person forget before the read-out
            n2 (note! st :ann :ann :t2 "B2: Ann forgotten before the read-out")
            p2 (promote-held! st (spec-into :ann (:fid n2) :group :by-entity :s/t2) :before-read-out)
            shown2a (status-kw st p2)
            fp2 (c/forget-person! st :ann)
            _ (inject/release! :before-read-out (:name p2))
            r2 (pc/resend! st p2)
            s2 (status st p2)
            b2 (b-case "B: Alice forgotten before the read-out")]
        (testing "T1 (B1): a value forget queued before the read-out: pending, then refused"
          (is (= :pending (:status p1)))
          (is (= :pending (:key (:statement p1))) "said at the point of promotion (T14)")
          (is (= :yes (:answer f1)))
          (is (= (:shown b1) [shown1a shown1b (:status s1)]) "the model's :shown")
          (is (= [:refused :read-out :source-erased] ((juxt :status :at :reason) s1)))
          (is (= :source-erased (get-in r1 [:crossing :reason])) "the resend's ack carries the read-out's no")
          (is (= {[:alice :note nil] :erased} (:values b1)))
          (is (contains? (exit-open (:fid n1)) :erased-at) "the note reads erased through the read exit")
          (is (empty? (micro-records-named st (lname-of p1))) "no landing was ever appended")
          (is (nil? (forward-of st p1)) "no forward stored"))

        (testing "T2 (B2): the owner forgotten before the read-out: pending, then refused"
          (is (= :pending (:status p2)))
          (is (= :yes (:answer fp2)))
          (is (= (:shown b2) [shown2a (:status s2)]) "the model's :shown")
          (is (= [:refused :read-out :source-erased] ((juxt :status :at :reason) s2)))
          (is (= :source-erased (get-in r2 [:crossing :reason])))
          (is (contains? (c/opens? st :ann (:fid n2)) :erased-at) "her note reads erased")
          (is (empty? (micro-records-named st (lname-of p2)))))

        (testing "T11: after a forget before the read-out, nothing any depot holds opens the source"
          (let [plains #{"B1: forgotten before the read-out" "B2: Ann forgotten before the read-out"}
                sealed-of (fn [fid] (get-in (first (offers-records-named st (first fid))) [:facts 0 :sealed]))
                sources (keep sealed-of [(:fid n1) (:fid n2)])
                persons (into {} (for [q [:alice :bob :ann :ava]] [q (c/person st q)]))
                ;; the stream store's lease rows and lock rows in the layers these acts touched
                stream-rows (for [L [:alice :ann :base]
                                  sess (foreign-select [(keypath L :leases) MAP-KEYS] (:layers st))
                                  [_ row] (c/lease-rows st L sess)]
                              row)
                lease-locks (keep #(locks/unlease % (get persons (:under %))) stream-rows)
                row-locks (for [L [:alice :ann] [_ rec] (c/lock-rows st L)
                                :let [K (locks/unwrap rec persons)] :when K] K)
                ;; the micro store's landing rows of these promotions, and every box in its depot
                landing-rows (concat (vals (mc/lease-rows st (lease-name-of p1))) (vals (mc/lease-rows st (lease-name-of p2))))
                micro-recs (depot-records (:micro-depot st))
                boxed (for [r micro-recs f (:facts r) :when (:box f)
                            row landing-rows
                            :let [K (ps/open-landing row (:name r) (:lock-id f) (:box f))] :when K] K)
                candidates (concat lease-locks row-locks boxed (keep :lock (vals persons)))]
            (is (= 2 (count sources)) "both sources' sealed bytes are in *offers")
            (is (= 2 (count landing-rows)) "both unused landing leases still wait, bare")
            (is (every? (fn [s] (not-any? (fn [K] (some-> (locks/open K s) locks/decode-plain :value plains)) candidates)) sources)
                "no lock the store holds opens either source")
            (is (empty? boxed) "the unused landing leases' private keys open no box in the depot")
            (is (not-any? #(= (lname-of p1) (:name %)) micro-recs))
            (say "T11 candidates tried" (count candidates) "micro records" (count micro-recs)))
          (testing "then Alice's session in the group closes, and her unused landing lease goes with it"
            (let [cl (mc/offer! st (mc/build {:who :alice :layer :group :session :s/t1 :permission (gp :alice)
                                             :facts [{:e :s/t1 :k :session-closed :v {:session :s/t1}}]}))]
              (is (= :yes (:answer cl)))
              (is (empty? (mc/lease-rows st (lease-name-of p1)))))))

        (let [;; B3, Alice: a value forget after the read-out
              n3 (note! st :alice :alice :t3 "B3: forgotten after the read-out")
              p3 (promote-with! st (spec-into :alice (:fid n3) :group :by-entity :s/t3) pause!)
              shown3a (status-kw st p3)
              f3 (c/forget-value! st :alice :alice (:fid n3))
              shown3b (status-kw st p3)
              _ (resume!)
              s3 (settled! st p3)
              shown3d (status-kw st p3)
              b3 (b-case "B: a value forget after the read-out")
              ;; B4, Ava in Alice's part: her person forget after the read-out
              n4 (note! st :ava :ava :t4 "B4: Ava forgotten after the read-out")
              p4 (promote-with! st (spec-into :ava (:fid n4) :group :by-entity :s/t4) pause!)
              shown4a (status-kw st p4)
              fp4 (c/forget-person! st :ava)
              rows4-before (mc/lease-rows st (lease-name-of p4))
              _ (resume!)
              s4 (settled! st p4)
              shown4c (status-kw st p4)
              rows4-after (mc/lease-rows st (lease-name-of p4))
              b4 (b-case "B: Alice forgotten after the read-out")]
          (testing "T3 (B3): a value forget after the read-out: crossed, then done; the copy stays"
            (is (= :yes (get-in p3 [:crossing :answer])))
            (is (= (:shown b3) [shown3a shown3b (:status s3) shown3d]) "the model's :shown")
            (is (< (get-in p3 [:crossing :stamp]) (:stamp f3)) "the forget falls after the read-out's line")
            (is (= {[:alice :note nil] :erased [:group :note nil] :open} (:values b3)))
            (is (contains? (exit-open (:fid n3)) :erased-at) "the note reads erased through the read exit")
            (is (= "B3: forgotten after the read-out" (:value (micro-copy st :t3 (lname-of p3)))) "the copy reads open"))

          (testing "T4 (B4): the owner forgotten after the read-out: crossed, then done; the copy stays"
            (is (= :yes (:answer fp4)))
            (is (= (:shown b4) [shown4a (:status s4) shown4c]) "the model's :shown")
            (is (= 1 (count rows4-before)) "[F9] her landing lease row outlives her forget")
            (is (nil? (:under (val (first rows4-before)))) "it is bare")
            (is (empty? rows4-after) "the landing consumed it")
            (is (= "B4: Ava forgotten after the read-out" (:value (micro-copy st :t4 (lname-of p4)))) "the copy reads open")
            (is (contains? (c/opens? st :ava (:fid n4)) :erased-at) "her note reads erased"))

          (testing "T5: both sides of the read-out line, by stamp"
            (let [rs (get-in p3 [:request :stamp]) cs (get-in p3 [:crossing :stamp]) ls (:landing-stamp s3)]
              (is (< (:stamp n3) rs cs ls) "the source, the request, the crossing, the landing: in stamp order, across both stores")
              (is (= :none (status-as-of st p3 (dec rs))) "nothing admitted after the moment is shown ([F5])")
              (is (= :pending (status-as-of st p3 rs)))
              (is (= :pending (status-as-of st p3 (dec cs))))
              (is (= :crossed (status-as-of st p3 cs)))
              (is (= :crossed (status-as-of st p3 (dec ls))))
              (is (= :done (status-as-of st p3 ls))))
            (let [cs1 (get-in r1 [:crossing :stamp])]
              (is (< (:stamp f1) cs1) "B1's forget falls before the line")
              (is (= :pending (status-as-of st p1 (dec cs1))))
              (is (= :refused (status-as-of st p1 cs1))))))

        (testing "T6: a retried request lands once (into the group)"
          (let [n (note! st :alice :alice :t6 "retried into the group")
                u (env/uuid7)
                req (ps/request-name :alice u)
                _ (inject/hold! :before-read-out req)
                p (pc/promote! st (assoc (spec-into :alice (:fid n) :group :by-entity :s/t6) :uuid u))
                r1 (pc/resend! st p)
                r2 (pc/resend! st p)
                _ (inject/release! :before-read-out req)
                _ (pause!)
                r3 (pc/resend! st p)
                r4 (pc/resend! st p)
                _ (resume!)
                s (settled! st p)
                lname (lname-of p)
                rec-before (mc/record-of st lname)
                r5 (pc/resend! st p)
                r6 (pc/resend! st p)
                fwd (forward-of st p)
                _ (mc/await-answer st (:landing fwd))
                rec-after (mc/record-of st lname)]
            (is (= 1 (count (distinct (map #(select-keys % [:answer :reason :stamp]) [(:request p) r1 r2 r3 r4 r5 r6]))))
                "every resend's ack carries the same request answer")
            (is (every? nil? (map :crossing [r1 r2])))
            (is (= 1 (count (distinct (map :crossing [r3 r4 r5 r6])))) "the same crossing answer after the read-out")
            (is (>= (count (micro-records-named st lname)) 2) "the forward was sent again (at least once more)")
            (is (= :done (:status s)))
            (is (= (select-keys rec-before [:answer :stamp :batch]) (select-keys rec-after [:answer :stamp :batch]))
                "one landing answer: later sends answered from it")
            (is (= 1 (count (:rows (mc/act st :t6 lname)))) "one copy row")
            (is (= "retried into the group" (:value (micro-copy st :t6 lname))))
            (is (empty? (mc/lease-rows st (lease-name-of p))) "the lease consumed by the first landing")))

        (testing "T7: a failover between the forward and the landing"
          (testing "(a) a crash just after the append: the record replays, the stored forward is sent again, one copy"
            (let [n (note! st :alice :alice :t7a "crash after the forward")
                  u (env/uuid7)
                  req (ps/request-name :alice u)
                  _ (inject/arm! :after-forward req 1)
                  p (pc/promote! st (assoc (spec-into :alice (:fid n) :group :by-entity :s/t7a) :uuid u))
                  s (settled! st p)
                  lname (lname-of p)]
              (is (= 1 (inject/fired-count :after-forward req)) "the crash fired")
              (is (= :done (:status s)))
              (is (wait-until #(<= 2 (count (micro-records-named st lname))) 30000)
                  "the replay appended the stored forward again (it and the landing's batch are ordered by nothing)")
              (is (= 1 (count (:rows (mc/act st :t7a lname)))) "decided once: one copy row")
              (is (= "crash after the forward" (:value (micro-copy st :t7a lname))))
              (is (empty? (mc/lease-rows st (lease-name-of p))))))
          (testing "(b) a crash after the crossing's commit, before the append: the replay sends the stored forward"
            (let [n (note! st :alice :alice :t7b "crash before the forward")
                  u (env/uuid7)
                  req (ps/request-name :alice u)
                  _ (inject/arm! :before-forward req 1)
                  p (pc/promote! st (assoc (spec-into :alice (:fid n) :group :by-entity :s/t7b) :uuid u))
                  s (settled! st p)]
              (is (= 1 (inject/fired-count :before-forward req)))
              (is (= :done (:status s)))
              (is (= "crash before the forward" (:value (micro-copy st :t7b (lname-of p)))))))
          (testing "(b, held) the source forgotten between the crossing and the forward: the stored forward still lands"
            (let [n (note! st :alice :alice :t7h "forgotten between the crossing and the forward")
                  p (promote-held! st (spec-into :alice (:fid n) :group :by-entity :s/t7h) :before-forward)
                  _ (c/forget-value! st :alice :alice (:fid n))
                  _ (inject/release! :before-forward (:name p))
                  _ (pc/resend! st p)
                  s (settled! st p)]
              (is (= :crossed (:status p)))
              (is (= :done (:status s)) "the model's forward would not re-send once the source is erased; here the first send cannot be lost")
              (is (contains? (exit-open (:fid n)) :erased-at))
              (is (= "forgotten between the crossing and the forward" (:value (micro-copy st :t7h (lname-of p)))))))
          (testing "(c) the landing's batch fails once on the micro gate: retried whole, one copy, the lease consumed once"
            (let [n (note! st :alice :alice :t7c "the landing's batch fails once")
                  u (env/uuid7)
                  lname [:group :by-entity :landing u]
                  _ (inject/arm! :micro-gather lname 1)
                  p (pc/promote! st (assoc (spec-into :alice (:fid n) :group :by-entity :s/t7c) :uuid u))
                  s (settled! st p)]
              (is (= 1 (inject/fired-count :micro-gather lname)))
              (is (= :done (:status s)))
              (is (= 1 (count (:rows (mc/act st :t7c lname)))))
              (is (= "the landing's batch fails once" (:value (micro-copy st :t7c lname))))
              (is (empty? (mc/lease-rows st (lease-name-of p))))))))

      (testing "T8: a forget of the source after the landing: the copy stays, and opens only under its own record lock"
        (let [n (note! st :alice :alice :t8 "landed, then its source forgotten")
              p (pc/promote! st (spec-into :alice (:fid n) :group :by-entity :s/t8))
              s (settled! st p)
              lname (lname-of p)
              fwd (forward-of st p)
              _ (c/forget-value! st :alice :alice (:fid n))
              row (mc/row-of st :t8 [lname 0])
              K (locks/unwrap (:lock row) (into {} (for [q (locks/wrap-persons (:lock row))] [q (c/person st q)])))
              opened-by-K (fn [f] (some-> (locks/open K (:sealed f)) locks/decode-plain :value))]
          (is (= :done (:status s)))
          (is (contains? (exit-open (:fid n)) :erased-at) "the source reads erased")
          (is (= "landed, then its source forgotten" (:value (micro-copy st :t8 lname))) "the copy reads open")
          (is (empty? (mc/lease-rows st (lease-name-of p))) "the box's one opener, the landing lease, is gone")
          (is (= "landed, then its source forgotten" (opened-by-K (get-in fwd [:landing :facts 0])))
              "the stored forward's bytes open under the copy's record lock")
          (is (every? #(= "landed, then its source forgotten" (opened-by-K (first (:facts %)))) (micro-records-named st lname))
              "and so do the micro depot's")
          (let [a (mc/forget-value! st :group [lname 0] :t8)]
            (is (= :yes (:answer a)) "the operator's excision of the copy in the group")
            (is (nil? (:lock (mc/row-of st :t8 [lname 0]))) "its record lock is gone")
            (is (contains? (micro-copy st :t8 lname) :erased-at) "nothing the store holds opens the copy now"))))

      (testing "T9: a landing refused by the target; crossed does not promise done"
        (testing "(a) its permission revoked in the group between the crossing and the landing"
          (mc/open-session! st :s/t9a :alice [:group])
          (let [spid [:s/t9a :group :group (gp :alice)]
                n (note! st :alice :alice :t9a "refused: its permission revoked")
                p (promote-held! st (spec-into :alice (:fid n) :group :by-entity :s/t9a :landing-permission spid) :before-forward)
                rv (mc/offer! st (mc/revoke-offer st spid))
                _ (inject/release! :before-forward (:name p))
                _ (pc/resend! st p)
                s (settled! st p)]
            (is (= :crossed (:status p)))
            (is (= :yes (:answer rv)) "the revoke, in the group where the permission lives")
            (is (= [:refused :landing :permission-revoked] ((juxt :status :at :reason) s)))
            (is (empty? (mc/lease-rows st (lease-name-of p))) "the lease consumed with the refusal: the box opens under nothing now")
            (is (= "refused: its permission revoked" (:value (c/opens? st :alice (:fid n)))) "the source still open")))
        (testing "(b) Bob replaces the head the copy would replace, first"
          (let [g (mc/write! st {:who :bob :layer :group :session :s/bob9 :permission (gp :bob)
                                 :facts [{:e :t9b :k :note :v "Bob's first"}]})
                gfid [(:name (:offer g)) 0]
                n (note! st :alice :alice :t9b "refused: stale")
                p (promote-held! st (spec-into :alice (:fid n) :group :by-entity :s/t9b :replaces gfid) :before-forward)
                g2 (mc/write! st {:who :bob :layer :group :session :s/bob9 :permission (gp :bob)
                                  :facts [{:e :t9b :k :note :v "Bob's second" :replaces gfid}]})
                _ (inject/release! :before-forward (:name p))
                _ (pc/resend! st p)
                s (settled! st p)]
            (is (= :yes (get-in g [:answer :answer])))
            (is (= :yes (get-in g2 [:answer :answer])))
            (is (= [:refused :landing :stale-replaces] ((juxt :status :at :reason) s)))
            (is (= "refused: stale" (:value (c/opens? st :alice (:fid n)))))))
        (testing "(c) the session its landing lease was taken in closes first: :landing-lock-gone, recorded"
          (let [n (note! st :alice :alice :t9c "refused: its lease gone")
                p (promote-held! st (spec-into :alice (:fid n) :group :by-entity :s/t9c) :before-forward)
                cl (mc/offer! st (mc/build {:who :alice :layer :group :session :s/t9c :permission (gp :alice)
                                           :facts [{:e :s/t9c :k :session-closed :v {:session :s/t9c}}]}))
                _ (inject/release! :before-forward (:name p))
                _ (pc/resend! st p)
                s (settled! st p)]
            (is (= :yes (:answer cl)))
            (is (= [:refused :landing :landing-lock-gone] ((juxt :status :at :reason) s)) "a promotion ends, never crossed for ever")
            (is (= :landing-lock-gone (:reason (mc/record-of st (lname-of p)))))
            (is (= "refused: its lease gone" (:value (c/opens? st :alice (:fid n))))))))

      (testing "T12: the reservation, and a third writer who tries to capture a promotion"
        (let [s12 :s/t12]
          (testing "a door's store-scheme names, and landing leases of another shape"
            (let [u (env/uuid7)]
              (is (= :reserved-scheme (:reason (c/offer! st (c/build {:name [:alice :by-layer :landing u] :who :alice :layer :alice :class :by-layer
                                                                      :permission [:alice :alice :alice] :facts [{:e :x :k :promote-request :v {}}]}))))
                  "a :landing name on *offers")
              (is (= :reserved-scheme (:reason (c/offer! st (c/build {:name [:alice nil :crossing u] :who :alice :layer :alice :class :by-layer
                                                                      :permission [:alice :alice :alice] :facts [{:e :x :k :crossed :v {}}]}))))
                  "a :crossing name on *offers")
              (is (= :reserved-scheme (:reason (mc/offer! st (mc/build {:name [:group nil :crossing u] :who :alice :layer :group
                                                                        :permission (gp :alice) :facts [{:e :x :k :crossed :v {}}]}))))
                  "a :crossing name on *micro-offers")
              (is (= :malformed-control
                     (:reason (mc/offer! st (mc/build {:name [:group :by-entity :offer u] :who :alice :layer :group :session s12 :permission (gp :alice)
                                                       :facts [{:e s12 :k :lease :v {:count 2 :landing [:group :by-entity :landing u]}}]}))))
                  "a landing lease of count 2")))
          (let [n (note! st :alice :alice :t12 "Alice's content")
                p (promote-held! st (spec-into :alice (:fid n) :group :by-entity s12) :before-forward)
                lname (lname-of p)
                lid (ps/bound-lease-id lname)
                pub (:public (:lease p))
                forged (fn [content]
                         (let [K (locks/fresh-lock)]
                           {:version 1 :name lname :who :bob :layer :group :class :by-entity :permission (gp :bob)
                            :session :s/bob12 :stood-on {} :subjects #{}
                            :facts [{:e :t12 :k :note :sealed (locks/seal K (locks/canonical-bytes content)) :lock-id lid
                                     :box (box/box pub K (box/keypair) (box/fresh-nonce) (ps/aad lid lname))}]}))]
            (testing "an :offer citing a landing lease gets no lock from it (PR10)"
              (let [K (locks/fresh-lock)
                    o (mc/build {:who :alice :layer :group :session s12 :permission (gp :alice)
                                 :facts [{:e :t12x :k :note :sealed (locks/seal K (locks/canonical-bytes "x")) :lock-id lid}]})]
                (is (= :no-such-lock (:reason (mc/offer! st o))))
                (is (= 1 (count (mc/lease-rows st (lease-name-of p)))) "her row untouched")))
            (testing "[F1] (a) Bob's own lease act for Alice's landing name"
              (is (= :malformed-control
                     (:reason (mc/offer! st (mc/build {:who :bob :layer :group :session :s/bob12 :permission (gp :bob)
                                                       :facts [{:e :s/bob12 :k :lease :v {:count 1 :landing lname}}]}))))))
            (testing "[F1] (b) Bob's lease act under Alice's lease name: taken"
              (is (= :name-taken
                     (:reason (mc/offer! st (mc/build {:name (lease-name-of p) :who :bob :layer :group :session :s/bob12 :permission (gp :bob)
                                                       :facts [{:e :s/bob12 :k :lease :v {:count 1 :landing lname}}]}))))))
            (testing "[F3] (c) Bob's landing under Alice's landing name, citing her lease under his session: refused on its face, nothing recorded, her row left alone"
              (let [a (mc/offer! st (forged "Bob's content"))]
                (is (= :no-such-lock (:reason a)))
                (is (:face a))
                (is (nil? (mc/record-of st lname)))
                (is (= 1 (count (mc/lease-rows st (lease-name-of p)))))))
            (testing "[F3] (d) Bob's landing in the same batch as Alice's: hers is decided, his refused on its face"
              (pause!)
              (let [bob (forged "Bob's content, again")]
                (mc/send! st bob)
                (inject/release! :before-forward (:name p))
                (pc/resend! st p)
                (resume!)
                (let [s (settled! st p)]
                  (is (= :done (:status s)))
                  (is (= "Alice's content" (:value (micro-copy st :t12 lname))) "the landing is Alice's, with her content")
                  (is (:face (mc/await-answer st bob)) "his: a face refusal")))))))

      (testing "T13: whose the copy is: about whom the target's grammar and the tool name, not its former owner"
        (make-promoter! st :abe)
        (c/make-person! st :bea)
        (let [m (note! st :abe :abe :t13m {:persons #{:bea}} :k :mention)
              pl (note! st :abe :abe :t13n "a note about no one")
              pm (pc/promote! st (spec-into :abe (:fid m) :group :by-entity :s/t13m))
              pn (pc/promote! st (spec-into :abe (:fid pl) :group :by-entity :s/t13n))
              _ (settled! st pm)
              _ (settled! st pn)
              mcopy #(micro-copy st :t13m (lname-of pm))
              ncopy #(micro-copy st :t13n (lname-of pn))]
          (is (= {:persons #{:bea}} (:value (mcopy))))
          (is (= [:bea] (:any-of (:lock (mc/row-of st :t13m [(lname-of pm) 0])))) "wrapped under its own subject, not its former owner")
          (c/forget-person! st :abe)
          (is (= {:persons #{:bea}} (:value (mcopy))) "the former owner's forget leaves the copy")
          (is (contains? (c/opens? st :abe (:fid m)) :erased-at) "her own value dies with her")
          (c/forget-person! st :bea)
          (is (contains? (mcopy) :erased-at) "about one person in a shared layer, it dies with her (7b as written)")
          (is (= "a note about no one" (:value (ncopy))) "a note about no one survives both")))

      (testing "T15: the implicit spec's other cases (OP13 to OP15)"
        (testing "a hand session's and an agent session's value promote as a personal one's; the agent source's excision leaves the copy"
          (let [nh (note! st :alice :alice-hand :t15h "from the hand session")
                na (note! st :alice :alice-agent :t15a "from the agent session")
                ph (pc/promote! st (spec-into :alice (:fid nh) :group :by-entity :s/t15h :layer :alice-hand))
                pa (pc/promote! st (spec-into :alice (:fid na) :group :by-entity :s/t15a :layer :alice-agent))]
            (is (= :done (:status (settled! st ph))))
            (is (= :done (:status (settled! st pa))))
            (is (= "from the hand session" (:value (micro-copy st :t15h (lname-of ph)))))
            (is (= :excised (:how (c/forget-value! st :alice :alice-agent (:fid na)))) "an agent layer keeps its locks in the record")
            (is (contains? (c/opens? st :alice-agent (:fid na)) :erased-at))
            (is (= "from the agent session" (:value (micro-copy st :t15a (lname-of pa)))) "the copy stays")))
        (testing "two requests for one value are two promotions: both land, unless the first moved the head the second names"
          (let [g (mc/write! st {:who :bob :layer :group :session :s/bob15 :permission (gp :bob)
                                 :facts [{:e :t15d :k :note :v "the head"}]})
                gfid [(:name (:offer g)) 0]
                n (note! st :alice :alice :t15d "promoted twice")
                pa (pc/promote! st (spec-into :alice (:fid n) :group :by-entity :s/t15d1 :replaces gfid))
                sa (settled! st pa)
                pb (pc/promote! st (spec-into :alice (:fid n) :group :by-entity :s/t15d2 :replaces gfid))
                sb (settled! st pb)
                pc3 (pc/promote! st (spec-into :alice (:fid n) :group :by-entity :s/t15d3))
                pd (pc/promote! st (spec-into :alice (:fid n) :group :by-entity :s/t15d4))]
            (is (= :done (:status sa)))
            (is (= [:refused :landing :stale-replaces] ((juxt :status :at :reason) sb)))
            (is (= [:done :done] [(:status (settled! st pc3)) (:status (settled! st pd))]) "replacing nothing, both land")))
        (testing "a refused request is no promotion: no read-out, and the status says so"
          (let [n (note! st :alice :alice :t15r "a request refused")
                u (env/uuid7)
                o (pc/request-offer {:who :alice :layer :alice :permission [:alice :alice :alice] :session :s/t15r
                                     :source (:fid n) :source-stamp (:stamp n) :source-e :t15r
                                     :target :group :class :by-entity :public (locks/fresh-bytes 44)
                                     :landing-permission (gp :alice) :uuid u})
                a (c/offer-until-answered! st o)
                s (pc/promotion-status st :alice (:name o) nil)]
            (is (= [:no :malformed-control] ((juxt :answer :reason) a)) "[F2] a public key that is no X25519 key")
            (is (nil? (:crossing a)))
            (is (nil? (c/record st (env/crossing-name (:name o)))))
            (is (= {:status :none :request-refused :malformed-control} (select-keys s [:status :request-refused])))))
        (testing "a request for a value already erased is admitted; its read-out refuses"
          (let [n (note! st :alice :alice :t15e "erased before its request")
                _ (c/forget-value! st :alice :alice (:fid n))
                p (pc/promote! st (spec-into :alice (:fid n) :group :by-entity :s/t15e))]
            (is (= :yes (get-in p [:request :answer])) "the request checks nothing about its source")
            (is (= [:refused :source-erased] ((juxt :status (comp :reason :crossing)) p)))))
        (testing "a request for a fact with no value: no such fact, a retract"
          (let [n (note! st :alice :alice :t15f "a value")
                none [(first (:fid n)) 3]
                p1 (pc/promote! st (spec-into :alice none :group :by-entity :s/t15f1 :source-stamp (:stamp n) :source-e :t15f))
                r (c/offer-until-answered! st (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                                                        :facts [{:e :t15f :k :note :v nil :replaces (:fid n)}]}))
                rfid [(:name r) 0]
                p2 (pc/promote! st (spec-into :alice rfid :group :by-entity :s/t15f2 :source-stamp (:stamp r) :source-e :t15f))]
            (is (= [:refused :source-has-no-value] ((juxt :status (comp :reason :crossing)) p1)))
            (is (= :yes (:answer r)))
            (is (= [:refused :source-has-no-value] ((juxt :status (comp :reason :crossing)) p2)))))
        (make-promoter! st :amy)
        (testing "a source under per-act grain: the act's one lock is opened"
          (let [g (c/offer-until-answered! st (c/build {:who :amy :layer :amy :class :by-layer :permission [:amy :amy :amy]
                                                        :facts [{:e :amy :k :lock-grain :v :per-act}]}))
                o (c/build {:who :amy :layer :amy :class :by-layer :permission [:amy :amy :amy]
                            :facts [{:e :t15g1 :k :note :v "first of the act"} {:e :t15g2 :k :note :v "second of the act"}]})
                a (c/offer-until-answered! st o)
                p (pc/promote! st (spec-into :amy [(:name o) 1] :group :by-entity :s/t15g))]
            (is (= [:yes :yes] [(:answer g) (:answer a)]))
            (is (= 1 (count (distinct (map :lock-id (c/raw-rows st :amy (:name o)))))) "one lock for the act")
            (is (= :done (:status (settled! st p))))
            (is (= "second of the act" (:value (micro-copy st :t15g2 (lname-of p)))))))
        (testing "the source layer re-classed between the request and the read-out: read out where the lock is (V-2)"
          (let [n (note! st :amy :amy :t15c "read out after its layer's re-class")
                p (promote-held! st (spec-into :amy (:fid n) :group :by-entity :s/t15c) :before-read-out)
                rc (c/offer-until-answered! st (mc/reclass-offer st :amy))
                _ (inject/release! :before-read-out (:name p))
                r (pc/resend! st p)
                s (settled! st p)]
            (is (= :yes (:answer rc)))
            (is (= :by-entity (:class (c/settings st :amy))))
            (is (= :yes (get-in r [:crossing :answer])) "the read-out claims no class, as the model's does (the plan's F8 refused it)")
            (is (= :done (:status s)))
            (is (= "read out after its layer's re-class" (:value (micro-copy st :t15c (lname-of p)))))))
        (testing "a landing citing a permission that was never granted: refused :no-permission"
          (let [n (note! st :alice :alice :t15p "its landing cites no permission")
                u (env/uuid7)
                missing [:s/t15p :group :group (gp :alice)]
                lease (pc/lease-landing! st {:who :alice :target :group :class :by-entity :session :s/t15p
                                             :permission (gp :alice) :uuid u})
                o (pc/request-offer {:who :alice :layer :alice :permission [:alice :alice :alice] :session :s/t15p
                                     :source (:fid n) :source-stamp (:stamp n) :source-e :t15p
                                     :target :group :class :by-entity :public (:public lease)
                                     :landing-permission missing :uuid u})
                a (c/offer-until-answered! st o)
                s (settled! st {:name (:name o) :offer o})]
            (is (= :yes (get-in a [:crossing :answer])))
            (is (= [:refused :landing :no-permission] ((juxt :status :at :reason) s)))))
        (testing "[F2] a small-order public key passes the request's check; the read-out records :malformed-control, never a throw"
          (let [n (note! st :alice :alice :t15s "boxed to a small-order point")
                o (pc/request-offer {:who :alice :layer :alice :permission [:alice :alice :alice] :session :s/t15s
                                     :source (:fid n) :source-stamp (:stamp n) :source-e :t15s
                                     :target :group :class :by-entity :public (spki-of-u 1)
                                     :landing-permission (gp :alice) :uuid (env/uuid7)})
                a (c/offer-until-answered! st o)]
            (is (= :yes (:answer a)))
            (is (= [:no :malformed-control] ((juxt :answer :reason) (:crossing a))))
            (is (= [:refused :read-out :malformed-control]
                   ((juxt :status :at :reason) (pc/promotion-status st :alice (:name o) nil))))))
        (testing "the copy carries no marks: a mark is set at write, and the landing is a new write in the target"
          (let [o (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                            :facts [{:e :t15m :k :note :v "a marked source" :mark #{:die-with-any}}]})
                _ (c/offer-until-answered! st o)
                p (pc/promote! st (spec-into :alice [(:name o) 0] :group :by-entity :s/t15m))]
            (is (= :done (:status (settled! st p))))
            (is (= #{:die-with-any} (:mark (c/raw-row st :alice [(:name o) 0]))))
            (is (= #{} (:mark (mc/row-of st :t15m [(lname-of p) 0]))))))
        (testing "the head the request names, erased since: the replace is still allowed (OP4)"
          (let [g (mc/write! st {:who :bob :layer :group :session :s/bob15x :permission (gp :bob)
                                 :facts [{:e :t15x :k :note :v "a head, then erased"}]})
                gfid [(:name (:offer g)) 0]
                fg (mc/forget-value! st :group gfid :t15x)
                n (note! st :alice :alice :t15x "replaces an erased head")
                p (pc/promote! st (spec-into :alice (:fid n) :group :by-entity :s/t15x :replaces gfid))]
            (is (= :yes (:answer fg)))
            (is (= :done (:status (settled! st p))))
            (is (= "replaces an erased head" (:value (micro-copy st :t15x (lname-of p))))))))

      (testing "T14: what is said at the point of promotion"
        (let [n (note! st :alice :alice :t14 "said at the point of promotion")
              pp (promote-held! st (spec-into :alice (:fid n) :group :by-entity :s/t14p) :before-read-out)
              pc2 (promote-held! st (spec-into :alice (:fid n) :group :by-entity :s/t14c) :before-forward)]
          (is (= [:pending :pending] [(:status pp) (:key (:statement pp))]))
          (is (re-find #"Forgetting the value now refuses this promotion" (get-in pp [:statement :text])))
          (is (= [:crossed :crossed] [(:status pc2) (:key (:statement pc2))]))
          (is (re-find #"does not recall the copy" (get-in pc2 [:statement :text])))
          (is (re-find #"Crossed does not promise done" (get-in pc2 [:statement :text])))
          (inject/release! :before-forward (:name pc2))
          (pc/resend! st pc2)
          (let [s (settled! st pc2)]
            (is (= [:done :done] [(:status s) (get-in s [:statement :key])]))
            (is (re-find #"Landed in group" (get-in s [:statement :text]))))
          (let [s (status st pp)]
            (is (= :pending (get-in s [:statement :key])) "the read gives the statement of the state it found"))))
      (inject/reset-all!))))
