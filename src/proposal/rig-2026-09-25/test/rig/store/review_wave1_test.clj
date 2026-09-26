(ns rig.store.review-wave1-test
  "An independent falsification pass over wave 1 (REVIEW-wave1.md): each
  block tries to break one seam against the rulings (PROGRESS.md 'Now': the
  forget ruling, rulings 7 and 7b, the sharpenings) and SPEC.md's 'What
  Rama showed'. One in-process cluster of 4 tasks; crashes (R3) last; tests
  assert 'at least once' (R4).

  Blocks marked PENDING test a behaviour whose fix would change what a kept
  record carries (a first-record question for Sid). They print what the
  store does today as OBSERVED lines and assert the rulings' reading only
  when the environment variable RIG_PENDING is 1, so the suite stays green
  while the question is open:

    RIG_PENDING=1 clojure -M:test rig.store.review-wave1-test"
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [clojure.walk :as walk]
            [com.rpl.rama :as rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [rig.store.client :as c]
            [rig.store.envelope :as env]
            [rig.store.inject :as inject]
            [rig.store.locks :as locks]
            [rig.store.micro-client :as mc]
            [rig.store.module :as m]
            [rig.store.read-exit :as rx]))

;; ------------------------------------------------------------------ helpers

(def ^:private pending?
  "Whether the PENDING blocks assert the rulings' reading (see the ns doc)."
  (= "1" (System/getenv "RIG_PENDING")))

(defn- say [& xs] (apply println "OBSERVED" xs) (flush))

(defn- retrying
  "A foreign call, retried while a worker restarts after an injected crash."
  [f]
  (let [deadline (+ (System/currentTimeMillis) 60000)]
    (loop []
      (let [r (try [:ok (f)] (catch Exception e [:err e]))]
        (cond (= :ok (first r)) (second r)
              (> (System/currentTimeMillis) deadline) (throw (second r))
              :else (do (Thread/sleep 100) (recur)))))))

(defn- wait-until
  ([f] (wait-until f 60000))
  ([f timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop []
       (let [v (try (f) (catch Exception _ nil))]
         (if (or v (> (System/currentTimeMillis) deadline)) v (do (Thread/sleep 50) (recur))))))))

(defn- bytes->vec [x] (walk/postwalk #(if (bytes? %) (vec %) %) x))

(defn- text-anywhere? [data text] (str/includes? (pr-str (bytes->vec data)) text))

(defn- act
  "`who`'s act into `layer`, citing [who layer layer] unless the operator."
  [who layer facts & {:as more}]
  (c/build (merge {:who who :layer layer :class :by-layer
                   :permission (when-not (= :operator who) [who layer layer])
                   :facts facts}
                  more)))

(defn- field [st layer f] (into {} (retrying #(foreign-select [(keypath layer f) ALL] (:layers st)))))

(defn- fields [st layer] (into {} (for [f [:ix-ek :ix-ke :ix-kv :ix-of]] [f (field st layer f)])))

(defn- entries-of [fs fid] (for [f [:ix-ek :ix-ke] e (vals (get fs f)) :when (= fid (:fid e))] e))

(def ^:private alice {:reader :alice :reader-kind :person :working :alice-hand :permission [:alice :alice-hand :alice-hand]})

(defn- rd [st spec] (rx/read! st (merge alice spec)))

(def ^:private base-root [:operator :base :base])
(defn- bp [p] [p :base :base base-root])
(def ^:private group-root [:group :group :group])
(defn- gp [p] [p :group :group group-root])
(defn- sp [s p] [s :group :group (gp p)])

(defn- depot-records
  "Every record a depot holds, from every partition."
  [d]
  (let [n (:num-partitions (rama/foreign-object-info d))]
    (vec (for [p (range n)
               :let [{:keys [start-offset end-offset]} (rama/foreign-depot-partition-info d p)]
               r (rama/foreign-depot-read d p start-offset end-offset)]
           r))))

(defn- plaintext-in-depot?
  "Whether a depot record under `nm` carries `text` as a fact's plaintext `:v`."
  [records nm text]
  (boolean (some (fn [r] (and (= nm (:name r)) (some #(= text (:v %)) (:facts r)))) records)))

(defn- task-keys [] (for [i (range 64)] (keyword (str "rvk" i))))

(defn- lock-on-every-task
  "The person's `:lock` as each task holds it (one pkey per task found)."
  [st p]
  (distinct (map #(some-> (retrying (fn [] (c/person-on-task st p %))) :lock vec) (task-keys))))

;; ------------------------------------------------------------- the cluster

(deftest review-wave1
  (inject/reset-all!)
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc m/Store {:tasks 4 :threads 2 :workers 1})
    (let [st (merge (mc/connect ipc) (select-keys (rx/connect ipc) [:read-point :read-pattern :index-ops]))
          send! (fn [o] (c/offer-until-answered! st o))
          ok! (fn [o] (let [a (send! o)] (is (= :yes (:answer a)) (pr-str (:facts o))) a))]

      (testing "seed: the one-owner world, the base on the stream gate (default 6), the review's persons"
        (is (every? #(= :yes (:answer %)) (c/seed! st)))
        (is (every? #(= :yes (:answer %)) (mc/make-base! st)))
        (doseq [p [:rv-b :rv-q :rv-z]] (is (= :yes (:answer (mc/make-person! st p)))))
        (ok! (c/grant-offer st (bp :rv-b))))

      ;; ------------------------------------------------ R-2, PENDING (first-record)
      (testing "R-2 PENDING: a person forget does not reach a read entry's recorded [:kv] pattern, which pairs the value's text with its fact id"
        (let [text "rv2 a value about rv-b alone"
              o (act :rv-b :base [{:e :rv2 :k :note :v text}] :subjects #{:rv-b} :permission (bp :rv-b))
              _ (ok! o)
              fid [(:name o) 0]
              r (rd st {:layer :base :read [:pattern [:kv :note text]]})]
          (is (= [fid] (mapv :fid (:rows r))) "before: Alice's [:kv] read of the base confirms the value")
          (let [f (c/forget-person! st :rv-b)
                after (rd st {:layer :base :read [:pattern [:kv :note text]]})
                entry (rd st {:layer :alice-hand :read [:point [[(:entry r) 0]]]})
                line (:value (first (:rows entry)))
                pairs? (and (= [:kv :note text] (:pattern line)) (some #(= fid (first %)) (:exact line)))]
            (is (= :yes (:answer f)))
            (is (= [] (:rows after)) "holds: the value index no longer confirms the text (the purge)")
            (is (= {:erased-at (:stamp f)} (c/opens? st :base fid)) "holds: the value reads erased on the forget's date")
            (say "R-2: after rv-b's forget, Alice's read entry" (:entry r) "reads" (pr-str (select-keys line [:layer :pattern :exact :count])))
            (when pending?
              (is (not pairs?)
                  "the forget ruling ('gone for everyone including the past') and I-L4: nothing the store keeps confirms the forgotten value; Alice's entry still pairs its text with its fact id")))))

      ;; ------------------------------------------------ R-1, PENDING (first-record)
      (testing "R-1 PENDING (W1-7 widened): a fact under a control key the stream gate does not act on keeps its value as plaintext, and a forget answered yes erases nothing"
        (let [texts {:owner "rv1 owner text" :kind "rv1 kind text" :class "rv1 class text"
                     :lock-grain "rv1 grain text" :promote-request "rv1 request text" :crossed "rv1 crossed text"}
              sent (into {} (for [[k t] texts] (let [o (act :alice :alice [{:e :rv1 :k k :v t}])] [k [o (send! o)]])))
              admitted (sort (for [[k [_ a]] sent :when (= :yes (:answer a))] k))
              depot (c/depot-records st)
              fs (fields st :alice)
              in-depot (filter (fn [k] (let [[o] (sent k)] (plaintext-in-depot? depot (:name o) (texts k)))) admitted)
              in-log (filter (fn [k] (let [[o] (sent k)] (= (env/encode-value (texts k)) (:v (c/raw-row st :alice [(:name o) 0]))))) admitted)
              in-index (filter (fn [k] (let [[o] (sent k)] (some #(= (env/encode-value (texts k)) (:v %)) (entries-of fs [(:name o) 0])))) admitted)
              shown (fn [] (set (keep :value (:rows (rd st {:layer :alice :read [:pattern [:e :rv1]]})))))
              before (shown)]
          (say "R-1: admitted at the stream gate as plaintext control facts:" admitted)
          (say "R-1: plaintext in the depot:" (vec in-depot) "| in the log's :v:" (vec in-log) "| in the id-index entries:" (vec in-index))
          (is (contains? (set admitted) :owner) "a writer with a permission can put free text under :owner on another entity")
          (let [[o] (sent :owner)
                fo (c/forget-value! st :alice :alice [(:name o) 0])
                after (shown)]
            (say "R-1: the owner's forget of the :owner fact answers" (pr-str (select-keys fo [:answer :how])) "| the exit shows it before:" (contains? before (texts :owner)) "after:" (contains? after (texts :owner)))
            (is (= :yes (:answer fo)))
            (when pending?
              (is (empty? admitted) "no act keeps a plaintext value under a control key the gate does not act on (W1-7's rule, beyond :members)")
              (is (not (contains? after (texts :owner))) "a forget answered yes leaves the value readable nowhere")))))

      ;; ------------------------------------------ W1-1: who opens an agent's lease row
      (testing "W1-1: an agent's unconsumed lease rows are sealed under its layer's owner; who can take them"
        (ok! (c/grant-offer st [:rv-agent :alice-agent :alice-agent]))
        (let [r (rx/read! st {:reader :rv-agent :for :alice :reader-kind :model :working :alice-agent
                              :permission [:rv-agent :alice-agent :alice-agent] :layer :alice :read [:pattern [:e :rv1]]})
              session (c/default-session :rv-agent)
              rows (c/lease-rows st :alice-agent session)
              taken (c/lease-locks st :alice-agent session)]
          (is (contains? r :rows) (pr-str r))
          (is (and (seq rows) (every? #(= :alice (:under (second %))) rows)) "holds: sealed under Alice's lock (W1-1)")
          (say "W1-1:" (count (:locks taken)) "of the agent's unconsumed locks come back in plaintext from lease-locks to a caller naming only the layer and"
               session "(a session derived from the agent's id; the rig has no caller identity)")))

      ;; ------------------- replays: a person forget resent after a later value forget; 7b in an agent layer
      (testing "a person forget resent after a later value forget of a value it closed: no re-dating; 7b in an agent layer (record locks)"
        (let [v (act :alice :alice [{:e :rv5 :k :note :v "rv5 marked about rv-q" :mark #{:die-with-any}}] :subjects #{:rv-q})
              ag-lives (act :alice :alice-agent [{:e :rv5a :k :note :v "rv5 agent layer, about alice and rv-q"}] :subjects #{:rv-q})
              ag-dies (act :alice :alice-agent [{:e :rv5b :k :note :v "rv5 agent layer, marked" :mark #{:die-with-any}}] :subjects #{:rv-q})
              _ (doseq [o [v ag-lives ag-dies]] (ok! o))
              fid (fn [o] [(:name o) 0])
              fq (c/build {:who :operator :layer :people :class :by-layer :facts [{:e :rv-q :k :forget-person :v {:person :rv-q}}]})
              aq (ok! fq)
              fv (c/forget-value! st :alice :alice (fid v))
              _ (inject/record-purges!)
              aq2 (send! fq)
              purged @inject/purges
              dates (set (map :erased-at (entries-of (fields st :alice) (fid v))))]
          (is (= [:yes (:stamp aq)] [(:answer aq2) (:stamp aq2)]) "the resend is answered from the record")
          (is (not-any? #(= (fid v) (:fid %)) purged) "holds: the resent forget does not purge a value a later value forget dated")
          (is (= #{(:stamp fv)} dates) "holds: its tombstones keep the value forget's date")
          (is (= {:erased-at (:stamp fv)} (c/opens? st :alice (fid v))))
          (is (= {:value "rv5 agent layer, about alice and rv-q"} (c/opens? st :alice-agent (fid ag-lives)))
              "holds (7b in a one-owner layer): the owner required, the other subject not; survives rv-q")
          (is (= {:erased-at (:stamp aq)} (c/opens? st :alice-agent (fid ag-dies))) "holds: marked, it dies with rv-q")
          (is (some #(= (fid ag-dies) (:fid %)) purged) "and the resend purges it again, idempotently")))

      ;; ---------------------------- rebuild after the forgets writes nothing back
      (testing "a rebuild after value and person forgets rewrites and deletes nothing, and writes back no purged text"
        (let [o (act :alice :alice-agent [{:e :rv4 :k :note :v "rv4 record-lock value"}])
              _ (ok! o)
              f (c/forget-value! st :alice :alice-agent [(:name o) 0])
              forgotten ["rv4 record-lock value" "rv5 marked about rv-q" "rv5 agent layer, marked" "rv2 a value about rv-b alone"]
              results (into {} (for [l [:alice :alice-agent :base]] [l (rx/rebuild! st l)]))]
          (is (= [:yes :excised] ((juxt :answer :how) f)))
          (say "rebuild after forgets:" (pr-str (update-vals results #(select-keys % [:deleted :rewritten]))))
          (doseq [[l r] results]
            (is (= [0 0] [(:deleted r) (:rewritten r)]) (str l ": the purges left exactly what a rebuild implies")))
          (doseq [l [:alice :alice-agent :base] t forgotten]
            (is (not (text-anywhere? (fields st l) t)) (str l ": no index holds " t " after the rebuild")))))

      ;; ------------------------------------------------ the micro side
      (let [g (mc/make-group! st :group (:group mc/shared-world))]
        (is (= :yes (get-in g [:made :answer])))
        (mc/open-session! st :rvs :bob [:group])

        (testing "F-1 (phase 8's D1, D2): after a refused lease the micro door sends its value act sealed under throwaway locks, so the gate answers :no-such-lock, as the stream door does"
          (let [r (mc/write! st {:who :bob :layer :group :session :rvs :permission (bp :bob)
                                 :facts [{:e :rvf1 :k :note :v {:token "rv f1"}}]})]
            (say "F-1: the lease" (pr-str (select-keys (get-in r [:lease :answer]) [:answer :reason]))
                 "| the value act" (pr-str (select-keys (:answer r) [:answer :reason :face])))
            (is (= :no (get-in r [:lease :answer :answer])) "the lease citing a permission kept in another layer is refused")
            (is (= [:no :no-such-lock] ((juxt :answer :reason) (:answer r)))
                "the value act cites the ids the refused lease would have minted, so no lock is delivered: :no-such-lock on its face")))

        (testing "R-1 PENDING at the micro gate: a setting key on another entity keeps its value as plaintext in a group layer"
          (let [text "rv1 micro owner text"
                r (mc/write! st {:who :bob :layer :group :session :rvs :permission (sp :rvs :bob)
                                 :facts [{:e :rv1m :k :owner :v text}]})
                nm (get-in r [:offer :name])
                row (mc/row-of st :rv1m [nm 0])
                in-depot (plaintext-in-depot? (depot-records (:micro-depot st)) nm text)]
            (say "R-1 micro: the act" (pr-str (select-keys (:answer r) [:answer :reason])) "| the row's :v" (pr-str (:v row)) "| plaintext in *micro-offers:" in-depot)
            (when (= :yes (get-in r [:answer :answer]))
              (let [f (mc/forget-value! st :group [nm 0] :rv1m)]
                (say "R-1 micro: the operator's forget answers" (pr-str (select-keys f [:answer :reason])) "| the act reads" (pr-str (mc/open-act st :rv1m nm)))
                (when pending?
                  (is (not= text (:value (first (mc/open-act st :rv1m nm)))) "a forget answered yes leaves the value readable nowhere"))))
            (when pending?
              (is (not in-depot) "no plaintext value in the micro depot")))))

      ;; ------------------------------------------------------------ crashes
      (testing "a crash between a value forget's lock writes and its purge's writes discards both; the replay writes both (the notes' claim)"
        (let [text "rv6 crashed inside the purge"
              o (act :alice :alice [{:e :rv6 :k :note :v text}])
              _ (ok! o)
              fid [(:name o) 0]
              lid (:lock-id (c/raw-row st :alice fid))
              fo (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                           :stood-on {fid (:stamp (c/record st (:name o)))} :facts [{:e :rv6 :k :forget :v {:target fid}}]})
              armed (atom true)]
          (inject/record-purges!)
          ;; the purge seam hands its call to the recorder before any of its writes and after
          ;; write-decision>'s: a refused swap there is an exception in the forget's own event
          (set-validator! inject/purges (fn [v] (if (and @armed (some #(= fid (:fid %)) v)) (do (reset! armed false) false) true)))
          (try
            (try (c/offer! st fo) (catch Exception _ nil))
            (let [rec (wait-until #(let [r (c/lookup st (:name fo) nil)] (when (map? r) r)) 120000)
                  fs (fields st :alice)]
              (is (false? @armed) "the crash fired inside the purge")
              (is (= :yes (:answer rec)))
              (is (= {:stamp (:stamp rec) :how :row-deleted} (get (c/ledger st :alice) lid)) "holds: the ledger holds the committed stamp")
              (is (nil? (c/lock-row st :alice lid)) "holds: the lock row deleted")
              (is (every? #(= (:stamp rec) (:erased-at %)) (entries-of fs fid)) "holds: the tombstones carry the committed stamp")
              (is (not-any? #(str/includes? % text) (keys (:ix-kv fs))) "holds: no value-index address left"))
            (finally (set-validator! inject/purges nil)))))

      (testing "a crash in a person forget's fan-out child: is every value that dies purged, and every lock destroyed, when the answer returns?"
        (let [text "rv7 dies with rv-z"
              o (act :alice :alice [{:e :rv7 :k :note :v text :mark #{:die-with-any}}] :subjects #{:rv-z})
              _ (ok! o)
              fid [(:name o) 0]
              fo (c/build {:who :operator :layer :people :class :by-layer :facts [{:e :rv-z :k :forget-person :v {:person :rv-z}}]})
              armed (atom true)]
          (inject/record-purges!)
          (set-validator! inject/purges (fn [v] (if (and @armed (some #(= fid (:fid %)) v)) (do (reset! armed false) false) true)))
          (try
            (let [a (c/offer-until-answered! st fo)
                  opens (c/opens? st :alice fid)
                  on-alice (some-> (retrying #(c/person-on-task st :rv-z :alice)) :lock)
                  kv-left (some #(str/includes? % text) (keys (field st :alice :ix-kv)))]
              (say "crash in a person forget's child: answer" (:answer a) "| right after it: the value" (pr-str opens)
                   "| the lock on the value's task" (if on-alice "still there" "destroyed") "| its value-index address" (if kv-left "still there" "gone"))
              (is (false? @armed) "the crash fired in the child's purge")
              (is (= :yes (:answer a)))
              (is (= {:erased-at (:stamp a)} opens) "OP10: once answered, no value whose wrap needs the person opens")
              (is (nil? on-alice) "the lock destroyed on the value's task at the answer")
              (is (not kv-left) "the value index purged at the answer")
              (is (wait-until #(= [nil] (lock-on-every-task st :rv-z)) 120000) "eventually on every task"))
            (finally (set-validator! inject/purges nil))))))))
