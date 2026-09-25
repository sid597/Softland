(ns rig.store.stream-gate-test
  "The stream store on an in-process cluster (PLAN-stream-store.md,
  'Namespaces and tests'): the stream gate's branches as rig choice R7 lists
  them, the plan's test list with F13's additions, and the implicit spec's
  stream-side rows (E1 to E4). One cluster, seeded with the model's
  one-owner world; where the model can express a branch, its answer to the
  same history is asked of `formal.model/run` under `baseline` and
  compared. Crashes are injected through `rig.store.inject` (R3, P14).
  Tests assert 'at least once', never an exact replay count (R4).

  Stamps are hybrid stamps (rig.store.clock: ms × 65536 + counter). The
  model's stamps are one integer in which a wall tick and a counter step
  are the same size; only the model's answers are compared, never its
  stamp values, and the relations asserted here (strictly increasing,
  after what was stood on) hold in both units.

  `layers-schema-probe` repeats, as assertions, the build's first check
  (F14): the Rama behaviours of the exact `$$layers` schema the module
  rests on. It needs no module.

  Stage 2 (PLAN-locks-and-forgetting.md, [V-F4]): every value act goes
  through the door, which leases and seals, so where an expected answer
  changed because of it the test says so at the place: a writer whose
  lease is refused cites no lock, and its value act is refused
  `:no-such-lock` on its face while the lease act carries the recorded
  refusal (a difference from the model's answer, which phase 8 reports);
  a making act needs its owner to be a person (L11); a log row holds its
  value sealed; the subject slot holds the grammar's subjects; the parts
  digest covers no value. Where a test predicts stamps, the door's pool is
  stocked first so no lease act takes a stamp between."
  (:require [clojure.test :refer [deftest is testing]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [formal.model :as fm]
            [rig.store.client :as c]
            [rig.store.clock :as hlc]
            [rig.store.envelope :as env]
            [rig.store.inject :as inject]
            [rig.store.module :as m])
  (:import [com.rpl.rama.helpers TopologyUtils]))

;; ------------------------------------------------------------------ helpers

(defn- say [& xs] (apply println "OBSERVED" xs))

(defn- wait-until
  "Poll f (exceptions count as not yet) until it is truthy or timeout-ms
  passes; its last value."
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

(defn- nest [n] (loop [v 1 i 0] (if (< i n) (recur [v] (inc i)) v)))
(defn- subv [v] (subvec (into [::pad] v) 1))
(defn- note-fact [e v] {:e e :k :note :v v})

(defn- act
  "`who`'s act into `layer`, citing [who layer layer] unless given."
  [who layer facts & {:as more}]
  (c/build (merge {:who who :layer layer :class :by-layer
                   :permission (when-not (= :operator who) [who layer layer])
                   :facts facts}
                  more)))

(defn- raw-answer-row
  "One row of an act, by its fact id, as stored (the value slot as text)."
  [layers-ps layer nm idx]
  (foreign-select-one [(keypath layer :log nm idx)] layers-ps))

(defn- operator-act [layer facts & {:as more}]
  (c/build (merge {:who :operator :layer layer :class :by-layer :facts facts} more)))

;; the model's side: its offer specs and its answers under `baseline`

(def m-note {:e :e0 :k :note :replaces :none :mark #{}})

(defn- m-offer [& {:as more}]
  [:offer (merge {:who :alice :layer :alice :facts [m-note]} more)])

(defn- model-answer
  "The model's answer to the i-th offer people or the operator sent in the
  history: :yes, a reason, or nil."
  [history i]
  (let [st (fm/run fm/baseline history)
        nm (nth (distinct (:client-sent st)) i)
        a (first (fm/answers-for st nm))]
    (when a (if (= :yes (:answer a)) :yes (:reason a)))))

(defn- model-says? [history re]
  (boolean (some #(re-find re %) (:trace (fm/run fm/baseline history)))))

;; --------------------------------------------------------- the schema probe

(deftest layers-schema-probe
  (with-open [tp (rtest/create-test-pstate m/layers-schema)]
    (let [tx #(rtest/test-pstate-transform % tp)
          sel1 #(rtest/test-pstate-select-one % tp)
          sel #(rtest/test-pstate-select % tp)
          n1 [:alice :by-layer :offer (env/uuid7)]
          rec {:answer :yes :reason nil :stamp 5 :digest "ab" :who :alice :class :by-layer
               :permission [:alice :alice :alice] :session nil :because-of nil :claimed-when 7
               :subjects #{:alice}}
          rows [{:e :e0 :k :note :v "\"a\"" :replaces nil :mark #{}}
                {:e :e1 :k :note :v "\"b\"" :replaces [n1 0] :mark #{:own-row}}
                {:e :e2 :k :note :v nil :replaces nil :mark #{}}]]
      (testing "a record under a vector name key, created on an empty PState with its nested maps"
        (tx [(keypath :alice :answers n1) (termval rec)])
        (is (= rec (sel1 [(keypath :alice :answers n1)])))
        (is (nil? (sel1 [(keypath :nobody :answers n1)])))
        (is (nil? (sel1 [(keypath :nobody :settings)]))))
      (testing "a heads row under [e k [name idx]]: written, read, deleted with NONE>"
        (tx [(keypath :alice :heads [:e0 :note [n1 0]]) (termval 5)])
        (is (= 5 (sel1 [(keypath :alice :heads [:e0 :note [n1 0]])])))
        (tx [(keypath :alice :heads [:e0 :note [n1 0]]) NONE>])
        (is (nil? (sel1 [(keypath :alice :heads [:e0 :note [n1 0]])]))))
      (testing "F1: one whole-vector termval into the subindexed row vector; ALL reads the rows in order; one row by index"
        (tx [(keypath :alice :log n1) (termval rows)])
        (is (= rows (sel [(keypath :alice :log n1) ALL])))
        (is (= (rows 1) (sel1 [(keypath :alice :log n1 1)])))
        (is (thrown? Exception (tx [(keypath :alice :log [:alice :by-layer :offer (env/uuid7)] 0) (termval (rows 0))]))
            "a per-index write on an absent vector is refused, which is why the module writes the whole vector"))
      (testing "Rama 1.6.0's vector-schema takes :subindex? only, so the row vector tracks its size"
        (is (thrown? Exception (vector-schema String {:subindex-options {:track-size? false}}))))
      (testing "F2: the stood-on map by name, one termval per entry"
        (tx [(keypath :alice :stood-on n1 [n1 0]) (termval 3)])
        (is (= [[[n1 0] 3]] (sel [(keypath :alice :stood-on n1) ALL]))))
      (testing "answers iterate in the byte order of their keys: UUID7 names of one layer and class in time order"
        (let [names (vec (for [_ (range 4)] (do (Thread/sleep 2) [:bob :by-layer :offer (env/uuid7)])))]
          (doseq [n (shuffle names)] (tx [(keypath :bob :answers n) (termval rec)]))
          (is (= names (mapv first (sel [(keypath :bob :answers) ALL]))))))
      (testing "heads keys iterate with prefix contiguity, which stage 5's range over [e k] wants"
        (doseq [[k s] [[[:e0 :note [n1 0]] 1] [[:e0 :notes [n1 1]] 2] [[:e1 :note [n1 2]] 3] [[:e0 :note [n1 3]] 4]]]
          (tx [(keypath :carol :heads k) (termval s)]))
        (is (= [[:e0 :note] [:e0 :note] [:e0 :notes] [:e1 :note]]
               (mapv #(subvec (first %) 0 2) (sel [(keypath :carol :heads) ALL])))))
      (testing "the writes the parser and R13 exist to prevent throw: a subvec key, an Integer stamp, a string setting"
        (is (thrown? Exception (tx [(keypath :alice :heads (subv [:e0 :note [n1 0]])) (termval 1)])))
        (is (thrown? Exception (tx [(keypath :alice :heads [:e9 :note [n1 0]]) (termval (int 1))])))
        (is (thrown? Exception (tx [(keypath :carol :settings :kind) (termval "personal")])))))))

;; ------------------------------------------------------- the cluster, once

(deftest stream-gate
  (inject/reset-all!)
  (with-open [ipc (rtest/create-ipc)]
    (let [tasks (rand-nth [2 4 8])
          _ (rtest/launch-module! ipc m/Store {:tasks tasks :threads 2 :workers 1})
          st (c/connect ipc)
          layers-ps (:layers st)
          send! (fn [o] (c/offer! st o))
          rec (fn [nm] (retrying #(c/record st nm)))
          rows (fn [layer nm] (retrying #(c/facts st layer nm)))
          clock (fn [layer] (retrying #(c/clock st layer)))
          head (fn [layer e k fid] (retrying #(c/head st layer e k fid)))
          raw-answer (fn [layer nm] (retrying #(foreign-select-one [(keypath layer :answers nm)] layers-ps)))]
      (say "tasks" tasks)

      (testing "seed: the model's one-owner world, every making and granting act admitted"
        (let [answers (c/seed! st)]
          ;; stage 2: the store layer :people and a person act per person come first
          (is (= 13 (count answers)))
          (is (every? #(= :yes (:answer %)) answers))
          (is (= :yes (:answer (c/make-person! st :carol))) "Carol is a person: E1 names her, and she writes below")
          (is (= {:kind :personal :owner :alice :class :by-layer :grain :per-value} (c/settings st :alice)))
          (is (= {:kind :hand :owner :alice :class :by-layer :grain :per-value} (c/settings st :alice-hand)))
          (is (= {:kind :agent :owner :alice :class :by-layer :grain :per-value} (c/settings st :alice-agent)))
          (doseq [pid (:permissions c/model-world)]
            (is (some? (:granted (c/permission st (nth pid 2) pid))) (pr-str pid)))
          (is (nil? (c/permission st :alice [:alice :group :alice-hand]))
              "a session permission lives in the hand session, not in the layer it names")))

      (testing "E1: an act admitted whole, then resent, reused with other content, offered under another tag, looked up"
        (let [grant-name (first (:granted (c/permission st :alice [:alice :alice :alice])))
              grant-stamp (:stamp (rec grant-name))
              c0 (clock :alice)
              because (env/make-name :alice-hand :by-layer)
              facts [(note-fact :e0 {:token "v1"})
                     {:e :e1 :k :mention :v {:persons #{:bob}} :mark #{:die-with-any}}
                     {:e :e2 :k :note :v 7 :layer :alice}]
              o (act :alice :alice facts :subjects #{:carol} :stood-on {[grant-name 0] grant-stamp}
                     :because-of because :session :alice-hand :claimed-when 12345)
              nm (:name o)
              a (send! o)
              s (:stamp a)
              r (rec nm)]
          (testing "admitted on the layer's home: the answer, the record, the rows, the heads, what it stood on, the clock (R7, I-G6)"
            (is (= {:answer :yes :reason nil :stamp s :name nm} a))
            (is (< c0 s) "after the task's last stamp")
            (is (< grant-stamp s) "after what it stood on")
            (is (= {:answer :yes :reason nil :stamp s :digest (env/offer-digest o) :who :alice :class :by-layer
                    :permission [:alice :alice :alice] :session :alice-hand :because-of because
                    :claimed-when 12345 :subjects #{:alice :bob :carol}}
                   r)
                "the act's parts once; its subjects the owner, what it carried (P15), and (stage 2) the grammar's Bob")
            (is (= [{:e :e0 :k :note :v {:token "v1"} :replaces nil :mark #{}}
                    {:e :e1 :k :mention :v {:persons #{:bob}} :replaces nil :mark #{:die-with-any}}
                    {:e :e2 :k :note :v 7 :replaces nil :mark #{}}]
                   (rows :alice nm))
                "every fact, in the act's order, its value as offered")
            (let [raw-row (raw-answer-row layers-ps :alice nm 1)]
              (is (= {:e :e1 :k :mention :v nil :replaces nil :mark #{:die-with-any}}
                     (select-keys raw-row [:e :k :v :replaces :mark]))
                  "one row by its fact id [name idx]; stage 2: the value slot holds no plaintext")
              (is (and (bytes? (:sealed raw-row)) (bytes? (:digest raw-row)) (vector? (:lock-id raw-row)))
                  "the value sealed, its lock id and its value digest instead"))
            (is (= [s s s] (for [[i f] (map-indexed vector facts)] (head :alice (:e f) (:k f) [nm i])))
                "every fact heads its chain at the act's one stamp")
            (is (= {[grant-name 0] grant-stamp} (retrying #(c/stood-on st :alice nm))) "what it stood on, kept beside the record (F2)")
            (is (= s (clock :alice)) "the {:pkey layer} read reaches the task the gate wrote (F14)")
            (is (= :yes (model-answer [(m-offer)] 0)) "the model admits the same act"))
          (testing "a value about no one in a one-owner layer is about the owner"
            (let [o2 (act :alice :alice [(note-fact :e3 "nobody")])]
              (is (= :yes (:answer (send! o2))))
              (is (= #{:alice} (:subjects (rec (:name o2)))))))
          (testing "a resend of the same map: the recorded answer and stamp, nothing decided again (OP2, E1 N2 × resend)"
            (let [c1 (clock :alice)]
              (is (= a (send! o)))
              (is (= r (rec nm)))
              (is (= 3 (count (rows :alice nm))))
              (is (= c1 (clock :alice)) "no stamp given")
              (is (model-says? [(m-offer) [:work 0] [:retry 0]] #"already decided here"))
              (is (= :yes (model-answer [(m-offer) [:work 0] [:retry 0]] 0)))))
          (testing "other content under the name: name taken, recorded nowhere, the first record untouched (I-G7, E1 N2 × other content)"
            (let [c1 (clock :alice)]
              (is (= {:answer :no :reason :name-taken :stamp nil :name nm} (send! (assoc o :facts [(note-fact :e0 "other")]))))
              (is (= {:answer :no :reason :name-taken :stamp nil :name nm} (send! (assoc o :claimed-when 12346)))
                  "a map rebuilt with a new claimed-when is other content (P6)")
              (is (= r (rec nm)))
              (is (= 3 (count (rows :alice nm))))
              (is (= c1 (clock :alice)))
              (is (model-says? [(m-offer) [:work 0] [:reuse 0 {:who :alice :layer :alice :facts [(assoc m-note :e :e1)]}]]
                               #"taken by other content"))))
          (testing "the same name under another layer or class: refused on its face, reading nothing (E1 N2 × tag mismatch)"
            ;; stage 2: the door holds locks for :alice-hand first, so no lease act takes a stamp here
            (c/stock! st :alice :alice-hand :alice-hand 8)
            (let [c1 (clock :alice)
                  c1h (clock :alice-hand)]
              (is (= {:answer :no :reason :mis-tagged :stamp nil :name nm}
                     (send! (assoc o :layer :alice-hand :permission [:alice :alice-hand :alice-hand]))))
              (is (= {:answer :no :reason :mis-tagged :stamp nil :name nm}
                     (send! (assoc o :facts [{:e :alice :k :lock-grain :v :per-act}]))))
              (is (= r (rec nm)))
              (is (nil? (raw-answer :alice-hand nm)))
              (is (= [c1 c1h] [(clock :alice) (clock :alice-hand)]))
              (is (model-says? [(m-offer) [:work 0] [:reuse 0 {:who :alice :layer :alice-hand :facts [m-note]}]]
                               #"refused on its face"))))
          (testing "lookup by name plus layer (RD1, F13)"
            (is (= r (c/lookup st nm (env/offer-digest o))))
            (is (= r (c/lookup st nm nil)) "a nil digest returns the record as data, for a client that kept only the name")
            (is (= {:answer :no :reason :name-taken :name nm}
                   (c/lookup st nm (env/offer-digest (assoc o :facts [(note-fact :e0 "other")])))))
            (is (= :no-answer (c/lookup st (env/make-name :alice :by-layer) nil)) "no answer yet is not a refusal")
            (is (= :no-answer (c/lookup st (assoc nm 0 :alice-hand) nil)))
            (is (= r (c/lookup st nm (env/offer-digest (assoc-in o [:facts 0 :v] {:token "other"}))))
                "stage 2 ([V-F4]): the parts digest covers no value, so value-only other content looks like the record here; only the gate's value check tells")
            (is (= {:answer :no :reason :name-taken :stamp nil :name nm} (send! (assoc-in o [:facts 0 :v] {:token "other"})))
                "and the gate does, on the resend"))))

      (testing "refused on its face, reading and recording nothing (P7, O4)"
        (c/stock! st :alice :alice :door/alice 16)
        (let [c0 (clock :alice)
              base (act :alice :alice [(note-fact :e3 "x")])]
          (doseq [[what o want]
                  [["a name made with no class, for ordinary facts" (assoc base :name (env/make-name :alice nil)) :mis-tagged]
                   ["a name made for another layer" (assoc base :name (env/make-name :alice-agent :by-layer)) :mis-tagged]
                   ["a name made for the micro gate" (assoc base :name (env/make-name :alice :by-entity) :class :by-entity) :wrong-gate]
                   ["the :crossing scheme" (assoc base :name (env/make-name :alice nil :crossing (env/uuid7))
                                                  :facts [{:e :e3 :k :crossed :v {}}]) :reserved-scheme]
                   ["the :landing scheme" (assoc base :name (env/make-name :alice :by-layer :landing (env/uuid7))) :reserved-scheme]
                   ["the operator under the :landing scheme"
                    (assoc base :who :operator :permission nil :name (env/make-name :alice :by-layer :landing (env/uuid7)))
                    :reserved-scheme]]]
            (is (= {:answer :no :reason want :stamp nil :name (:name o)} (send! o)) what)
            (is (nil? (raw-answer :alice (:name o))) what)
            (is (nil? (raw-answer (nth (:name o) 0) (:name o))) what))
          (is (= c0 (clock :alice)) "no stamp given")))

      (testing "refusals recorded under the name with a stamp, nothing admitted (R7, I-G5, E1 N0 × refused)"
        (let [check-no (fn [o want what]
                         (let [layer (:layer o)
                               c0 (clock layer)
                               a (send! o)
                               r (rec (:name o))]
                           (is (= {:answer :no :reason want :stamp (:stamp a) :name (:name o)} a) what)
                           (is (and (number? (:stamp a)) (< c0 (:stamp a))) (str what ": the refusal consumed a stamp"))
                           (is (= (:stamp a) (clock layer)) what)
                           (is (= [:no want (:stamp a)] [(:answer r) (:reason r) (:stamp r)]) what)
                           (is (= [] (rows layer (:name o))) (str what ": nothing admitted"))
                           (is (every? nil? (for [[i f] (map-indexed vector (:facts o))]
                                              (head layer (:e f) (:k f) [(:name o) i])))
                               what)
                           a))
              ;; stage 2 ([V-F4], trace 6 item 7): a writer whose cause of refusal holds at the
              ;; lease cannot lease, so its value act cites no lock: refused on its face, recorded
              ;; nowhere; the lease act, an offer like any other, carries the recorded refusal
              check-lease-refused (fn [o want what]
                                    (let [layer (:layer o)
                                          a (send! o)
                                          lease (c/lease! st (:who o) layer (:session o) 1 (:permission o))]
                                      (is (= {:answer :no :reason :no-such-lock :stamp nil :name (:name o)} a)
                                          (str what ": the value act cites no lock"))
                                      (is (nil? (raw-answer layer (:name o))) (str what ": recorded nowhere"))
                                      (is (= [] (rows layer (:name o))) (str what ": nothing admitted"))
                                      (is (= [:no want] ((juxt :answer :reason) lease)) (str what ": the lease act carries the recorded refusal"))
                                      (is (= [:no want (:stamp lease)] ((juxt :answer :reason :stamp) (rec (:name lease))))
                                          (str what ": recorded under the lease's name, with a stamp"))
                                      a))]
          (check-no (act :alice :alice [(note-fact :e4 "a") {:e :e4 :k :tag :v 1 :layer :alice-hand}])
                    :fact-outside-the-acts-layer "a fact naming another layer")
          (is (= :fact-outside-the-acts-layer
                 (model-answer [(m-offer :facts [(assoc m-note :other-layer :alice-hand)])] 0)))
          (check-no (act :alice :alice [{:e :alice :k :lock-grain :v :per-act}] :class :by-entity)
                    :class-mismatch "a class other than the layer's class fact, before any re-class")
          (check-lease-refused (act :bob :alice [(note-fact :e4 "b")] :permission [:alice :alice :alice])
                               :permission-does-not-cover-this "Bob citing Alice's permission")
          (check-lease-refused (act :alice :alice [(note-fact :e4 "c")] :permission nil)
                               :permission-does-not-cover-this "a person citing no permission")
          (check-lease-refused (act :alice :alice [(note-fact :e4 "d")] :permission [:alice :alice-hand :alice-hand])
                               :permission-does-not-cover-this "a permission for another layer")
          (check-lease-refused (act :alice :alice [(note-fact :e4 "e")] :permission [:alice :alice :alice-hand])
                               :permission-from-another-layer "a permission kept in the hand session, cited on her own layer")
          (is (= :permission-from-another-layer (model-answer [(m-offer :cite :session)] 0))
              "the model records this reason for the value act itself; the rig records it for the lease (a difference, [V-F4])")
          (check-lease-refused (act :alice :alice-agent [(note-fact :e4 "f")] :permission [:alice :alice-agent :alice-hand])
                               :permission-from-another-layer "[:alice :alice-agent :alice-hand] cited on :alice-agent")
          (let [bob-no (act :bob :alice [(note-fact :e4 "g")])
                a (check-lease-refused bob-no :no-permission "a permission that does not exist (E4 P0 × write)")]
            (is (= :no-permission (model-answer [(m-offer :who :bob)] 0)))
            (testing "granted since: the resend, now able to lease, is decided fresh ([V-F4]; phase 1 kept the refused name refused)"
              (is (= :yes (:answer (send! (c/grant-offer st [:bob :alice :alice])))))
              (let [y (send! bob-no)]
                (is (= :yes (:answer y)) "a face refusal is recorded nowhere, so the name was still free")
                (is (= :yes (:answer (send! (act :bob :alice [(note-fact :e4 "h")])))))
                (testing "now admitted, the name holds (E1 N2 × other content, × tag mismatch)"
                  (is (= :name-taken (:reason (send! (assoc bob-no :facts [(note-fact :e4 "other")]))))
                      "other value content under it: the gate's value check")
                  (is (= :yes (:answer (c/lookup st (:name bob-no) (env/offer-digest (assoc bob-no :facts [(note-fact :e4 "other")])))))
                      "the client's lookup compares the parts digest only (L26)")
                  (is (= :mis-tagged (:reason (send! (assoc bob-no :layer :alice-hand)))))
                  (is (= y (send! bob-no)) "the record is untouched"))))
            (testing "a refusal whose cause arises between the lease and the offer stays recorded, and the refused name stays refused"
              (c/stock! st :bob :alice :door/bob 4)
              (let [late (act :bob :alice [(note-fact :e4 "after the revoke")])]
                (is (= :yes (:answer (c/offer-until-answered! st (c/revoke-offer st [:bob :alice :alice])))))
                (let [n1 (check-no late :permission-revoked "Bob's permission revoked after his lease")]
                  (is (= n1 (send! late)) "the same no and stamp on a resend")))))
          (check-lease-refused (act :alice :yan [(note-fact :e4 "i")]) :no-such-layer "a layer with no class fact (E3 L0 × offer)")
          (is (nil? (c/settings st :yan)))))

      (testing "making a layer (OP5, E3 L0 × make), making it again (F8), control facts (R13), grain switches (OP8)"
        ;; stage 2 (L11): a making act naming an owner with no person lock is refused, so Xia is made a person first
        (is (= :no-such-person (:reason (send! (c/make-layer-offer :xia-early {:kind :hand :owner :xia})))))
        (is (= :yes (:answer (c/make-person! st :xia))))
        (is (= :yes (:answer (send! (c/make-layer-offer :xia {:kind :hand :owner :xia})))))
        (is (= {:kind :hand :owner :xia :class :by-layer :grain :per-value} (c/settings st :xia)))
        (is (= :yes (:answer (send! (c/grant-offer st [:xia :xia :xia])))))
        (is (= :yes (:answer (send! (act :xia :xia [(note-fact :e0 "first")])))) "its root permission now decides offers")
        (is (= [:no :layer-already-made]
               ((juxt :answer :reason) (send! (c/make-layer-offer :xia {:kind :personal :owner :bob})))))
        (is (= :layer-already-made (:reason (send! (operator-act :xia [{:e :xia :k :owner :v :bob}])))))
        (is (= {:kind :hand :owner :xia :class :by-layer :grain :per-value} (c/settings st :xia)) "the owner is not rewritten")
        (testing "only the operator writes control facts; the owner may switch her grain (R13)"
          (is (= :control-not-allowed (:reason (send! (act :xia :xia [{:e :perm-xia :k :permission :v {:id [:xia :xia :xia]}}]))))
              "a person granting")
          (is (= :control-not-allowed (:reason (send! (act :xia :xia [{:e :perm-xia :k :revoke :v {:permission [:xia :xia :xia]}}]))))
              "a person revoking")
          (is (= :control-not-allowed (:reason (send! (act :xia :xia [{:e :xia :k :class :v :by-entity}])))) "a person re-classing")
          (is (= :control-not-allowed (:reason (send! (act :xia :xia [{:e :xia :k :owner :v :bob}])))) "a person rewriting the owner")
          (is (= :malformed-control (:reason (send! (operator-act :xia [{:e :xia :k :lock-grain :v :hourly}]))))))
        (testing "the owner switches her grain, and back in an act that also writes a value"
          (is (= :yes (:answer (send! (act :xia :xia [{:e :xia :k :lock-grain :v :per-act}])))) "E3 L1 × grain switch")
          (is (= :per-act (:grain (c/settings st :xia))))
          (let [mixed (act :xia :xia [{:e :xia :k :lock-grain :v :per-value} (note-fact :e1 "with it")])]
            (is (= [:xia :by-layer] (subvec (:name mixed) 0 2)) "a setting mixed with ordinary facts: the name carries the class")
            (is (= :yes (:answer (send! mixed))) "E3 L2 × switch back")
            (is (= :per-value (:grain (c/settings st :xia))))
            (is (= 2 (count (rows :xia (:name mixed)))))))
        (testing "a making act whose owner is not a keyword is refused as data and the worker stays up (the phase 4 finding)"
          (let [bad (operator-act :wren [{:e :wren :k :kind :v :personal} {:e :wren :k :owner :v "wren"}
                                         {:e :wren :k :class :v :by-layer}])
                a (send! bad)]
            (is (= [:no :malformed-control] ((juxt :answer :reason) a)))
            (is (= #{} (:subjects (rec (:name bad)))))
            (is (nil? (c/settings st :wren)))
            (is (= :yes (:answer (send! (act :xia :xia [(note-fact :e2 "still up")]))))))))

      (testing "re-class (OP7, E3 L1 × re-class, L3 × offer), P16, and re-class back (F8)"
        (is (= :yes (:answer (c/make-person! st :zed))) "stage 2 (L11): the owner is a person")
        (is (= :yes (:answer (send! (c/make-layer-offer :zed {:kind :personal :owner :zed})))))
        (is (= :yes (:answer (send! (c/grant-offer st [:zed :zed :zed])))))
        (let [before (act :zed :zed [(note-fact :e0 "before")])
              a-before (send! before)
              reclass (operator-act :zed [{:e :zed :k :class :v :by-entity}])]
          (is (= :yes (:answer a-before)))
          (is (= [:zed nil] (subvec (:name reclass) 0 2)) "the store places a re-class: its name carries no class")
          (is (= :yes (:answer (send! reclass))))
          (is (= :by-entity (:class (c/settings st :zed))))
          (is (= :class-mismatch (:reason (send! (act :zed :zed [(note-fact :e1 "after")]))))
              "an offer tagged by layer, decided after the re-class")
          (is (= :class-mismatch (model-answer [[:reclass :alice] (m-offer) [:work 0]] 1)))
          (let [by-entity (act :zed :zed [(note-fact :e1 "after")] :class :by-entity)]
            (is (= :by-entity (nth (:name by-entity) 1)))
            (is (= {:answer :no :reason :wrong-gate :stamp nil :name (:name by-entity)} (send! by-entity))
                "a name made for the micro gate reaches one gate only"))
          (is (= a-before (send! before)) "a name decided before the re-class is answered from its record (OP2)")
          (let [grain (act :zed :zed [{:e :zed :k :lock-grain :v :per-act}] :class :by-entity)]
            (is (= [:zed nil] (subvec (:name grain) 0 2)))
            (is (= :yes (:answer (send! grain))) "P16: a store-placed act of a re-classed one-owner layer stays with the stream gate")
            (is (= :per-act (:grain (c/settings st :zed)))))
          (is (= :unsupported-reclass
                 (:reason (send! (operator-act :zed [{:e :zed :k :class :v :by-layer}] :class :by-entity))))
              "E3 L3 × re-class back")
          (is (= :by-entity (:class (c/settings st :zed))))))

      (testing "grant and revoke (OP6, E4, P8)"
        (let [pid [:carol :alice-agent :alice-agent]
              g (send! (c/grant-offer st pid))
              row (c/permission st :alice-agent pid)
              carol-1 (act :carol :alice-agent [(note-fact :e0 "carol")])
              a1 (send! carol-1)]
          (is (= :yes (:answer g)))
          (is (= :yes (:answer a1)) "P1 × a write citing it in its own layer")
          (let [into-alice (act :carol :alice [(note-fact :e0 "x")] :permission pid)]
            (is (= :no-such-lock (:reason (send! into-alice)))
                "P1 × a write into another layer citing it: stage 2, the lease cannot be made, so the act cites no lock ([V-F4])")
            (is (= :permission-does-not-cover-this (:reason (c/lease! st :carol :alice (:session into-alice) 1 pid)))
                "the lease act carries the refusal"))
          (let [rv (c/revoke-offer st pid)
                ra (send! rv)]
            (is (= :yes (:answer ra)))
            (is (= {(:granted row) (:stamp g)} (:stood-on rv)) "the revoke stands on the grant")
            (is (< (:stamp g) (:stamp ra)))
            (is (= {:granted (:granted row) :revoked [(:name rv) 0]} (c/permission st :alice-agent pid)))
            (is (= :permission-revoked (:reason (send! (act :carol :alice-agent [(note-fact :e0 "later")])))) "P2 × write")
            (is (= a1 (send! carol-1)) "a decision before the revocation stands")
            (is (= :stale-revoke (:reason (send! (c/revoke-offer st pid)))) "P2 × revoke again")
            (is (= :yes (:answer (send! (c/grant-offer st pid)))) "P2 × re-grant: admitted as a fact")
            (is (= {:granted (:granted row) :revoked [(:name rv) 0]} (c/permission st :alice-agent pid))
                "the index keeps the first grant and its revocation (P8)"))
          (is (= :stale-revoke (:reason (send! (c/revoke-offer st [:dan :alice-agent :alice-agent])))) "P0 × revoke")
          (is (= :stale-revoke
                 (:reason (send! (operator-act :alice [{:e :perm-alice :k :revoke :v {:permission [:alice :alice :alice-hand]}}]))))
              "a revoke placed in a layer other than the permission's")
          (let [pid2 [:erin :alice-agent :alice-agent]]
            (is (= :yes (:answer (send! (c/grant-offer st pid2)))))
            (is (= :stale-revoke
                   (:reason (send! (operator-act :alice-agent [{:e :perm-erin :k :revoke :v {:permission pid2}}
                                                               {:e :perm-erin :k :revoke :v {:permission pid2}}]))))
                "two revokes of one permission in one act")
            (is (nil? (:revoked (c/permission st :alice-agent pid2)))))
          (is (= :yes (:answer (send! (act :alice :alice-agent [(note-fact :e0 "alice still")]))))
              "revoking one permission leaves the others in the layer live")
          (is (= :permission-revoked (model-answer [[:revoke [:alice :alice :own]] (m-offer)] 1)))))

      (testing "chains (OP4, E2): replace, stale and doubled replaces, one good and one stale refused whole, retract and undo"
        (let [ok (fn [o] (let [a (send! o)] (is (= :yes (:answer a)) (pr-str (:facts o))) a))
              no (fn [o why] (is (= :stale-replaces (:reason (send! o))) why))
              a1 (act :alice :alice [(note-fact :e5 "one")])
              s1 (:stamp (ok a1))
              f1 [(:name a1) 0]
              a2 (act :alice :alice [(assoc (note-fact :e5 "two") :replaces f1)])
              s2 (:stamp (ok a2))
              f2 [(:name a2) 0]]
          (is (< s1 s2) "a replace is stamped after the fact it replaces")
          (is (nil? (head :alice :e5 :note f1)) "C1 × replace of the head: the old fact no longer heads its chain")
          (is (= s2 (head :alice :e5 :note f2)))
          (no (act :alice :alice [(assoc (note-fact :e5 "three") :replaces f1)]) "C3: a replace of a replaced fact")
          (is (= :stale-replaces
                 (model-answer [(m-offer) [:work 0] (m-offer :facts [(assoc m-note :replaces :head)]) [:work 0]
                                (m-offer :facts [(assoc m-note :replaces :stale)])]
                               2)))
          (no (act :alice :alice [(assoc (note-fact :e5 "x") :replaces f2) (assoc (note-fact :e5 "y") :replaces f2)])
              "two facts of one act replacing the head")
          (is (= :stale-replaces
                 (model-answer [(m-offer) [:work 0] (m-offer :facts [(assoc m-note :replaces :head) (assoc m-note :replaces :head)])]
                               1)))
          (let [mixed (act :alice :alice [(assoc (note-fact :e5 "good") :replaces f2) (assoc (note-fact :e5 "stale") :replaces f1)])]
            (no mixed "one good and one stale replace")
            (is (= s2 (head :alice :e5 :note f2)) "refused whole: the good replace did not happen")
            (is (= [] (rows :alice (:name mixed)))))
          (let [h1 (act :alice :alice-hand [(note-fact :e5 "in the hand")])]
            (ok h1)
            (no (act :alice :alice [(assoc (note-fact :e5 "z") :replaces [(:name h1) 0])]) "a replace naming a fact of another layer"))
          (no (act :alice :alice [(assoc (note-fact :e5 "z") :replaces [(env/make-name :alice :by-layer) 0])])
              "C0: a replace naming no fact")
          (no (act :alice :alice [{:e :e5 :k :tag :v "z" :replaces f2}]) "a replace on another key")
          (let [retract (act :alice :alice [{:e :e5 :k :note :v nil :replaces f2}])
                sr (:stamp (ok retract))
                fr [(:name retract) 0]]
            (is (= [{:e :e5 :k :note :v nil :replaces f2 :mark #{}}] (rows :alice (:name retract)))
                "C1 × retract: a fact with no value replacing the head (P13)")
            (is (nil? (head :alice :e5 :note f2)))
            (is (= sr (head :alice :e5 :note fr)) "the retract heads the chain")
            (let [undo (act :alice :alice [(assoc (note-fact :e5 "back") :replaces fr)])
                  su (:stamp (ok undo))
                  fu [(:name undo) 0]]
              (is (= "back" (:v (first (rows :alice (:name undo))))) "its undo carries the value again")
              (is (nil? (head :alice :e5 :note fr)))
              (is (= su (head :alice :e5 :note fu)))
              (let [beside (act :alice :alice [(note-fact :e5 "beside")])
                    sb (:stamp (ok beside))]
                (is (= su (head :alice :e5 :note fu)) "C1 × a new fact without replace: both stay heads (C4)")
                (is (= sb (head :alice :e5 :note [(:name beside) 0]))))))))

      (testing "stamps keep the clock promises (I-O2, P9), as hybrid stamps: ms × 65536 + counter (rig.store.clock)"
        ;; stage 2: Alice's pool stocked first; Bob's refused offers are his lease acts, since a
        ;; value act of his cites no lock and is refused on its face with no stamp ([V-F4])
        (c/stock! st :alice :alice-agent :door/alice 16)
        (let [layer :alice-agent
              answers (vec (for [i (range 6)]
                             (if (even? i)
                               (send! (act :alice layer [(note-fact :e6 i)]))
                               (c/lease! st :bob layer :door/bob 1))))
              stamps (map :stamp answers)]
          (is (= [:yes :no :yes :no :yes :no] (map :answer answers)))
          (is (apply < stamps) "never backward on a task: one stamp per decided offer, yes or no, strictly increasing")
          (is (= (last stamps) (clock layer))))
        ;; before any act carries a stamp ahead of the wall or time is simulated:
        ;; both leave a task's clock ahead of the wall for the rest of the run
        (testing "stamps stay at the wall at a high rate: 3,200 acts on one task from 32 offerers at once"
          (let [layer :alice-agent
                _ (is (<= (hlc/ms-of (clock layer)) (System/currentTimeMillis)) "before the burst the task's clock is not ahead of the wall")
                t0 (System/nanoTime)
                runs (doall (for [j (range 32)]
                              (future
                                (vec (for [i (range 100)]
                                       (let [sent (System/currentTimeMillis)
                                             a (send! (act :alice layer [(note-fact :e15 [j i])]))]
                                         [sent (:stamp a) (System/currentTimeMillis) (:answer a)]))))))
                acts (vec (mapcat deref runs))
                secs (/ (- (System/nanoTime) t0) 1e9)
                stamps (map second acts)
                in-order (sort stamps)
                walls (map hlc/ms-of in-order)
                ms-unit (reductions (fn [p w] (max w (inc p))) (first walls) (rest walls))
                lead (apply max (map (fn [[_ s acked]] (- (hlc/ms-of s) acked)) acts))
                lag (apply max (map (fn [[sent s]] (- sent (hlc/ms-of s))) acts))]
            (say "burst:" (count acts) "acts in" (format "%.2f" secs) "s," (Math/round (/ (count acts) secs)) "a second;"
                 "largest lead of a stamp's millisecond over the wall at its ack" lead "ms;"
                 "largest counter" (apply max (map hlc/counter-of stamps)) ";"
                 "the millisecond unit over the same decisions would have ended" (- (last ms-unit) (last walls)) "ms ahead")
            (is (every? #(= :yes (nth % 3)) acts))
            (is (= (count acts) (count (set stamps))) "one stamp per decision, none shared")
            (is (<= lead 0) "no stamp's millisecond is past the wall when its ack came back")
            (is (<= lag 0) "none is before the wall when it was sent")
            (is (some #(pos? (hlc/counter-of %)) stamps) "some millisecond held more than one decision, told apart by the counter")
            (is (= (last in-order) (clock layer)))))
        (testing "a stood-on stamp ahead of the wall clock is honoured, and the task keeps the lead: never backward"
          (c/stock! st :alice :alice-agent :door/alice 4)
          (let [layer :alice-agent
                ahead (hlc/pack (+ (System/currentTimeMillis) 60000) 7)
                fid [(env/make-name :alice-hand :by-layer) 0]
                a (send! (act :alice layer [(note-fact :e6 "stood")] :stood-on {fid ahead}))
                b (send! (act :alice layer [(note-fact :e6 "after it")]))]
            (is (= (inc ahead) (:stamp a)) "never earlier than anything it stood on (the carried stamp)")
            (is (< (System/currentTimeMillis) (hlc/ms-of (:stamp a))) "its millisecond is ahead of the wall")
            (is (= (inc (:stamp a)) (:stamp b)) "the next act: one past the last, though the wall is behind")
            (is (= [(hlc/ms-of ahead) 9] ((juxt hlc/ms-of hlc/counter-of) (:stamp b))))
            (is (= (:stamp b) (clock layer)))))
        (c/stock! st :alice :alice-hand :door/alice 16)
        (with-open [_ (TopologyUtils/startSimTime)]
          (let [layer :alice-hand
                wall (+ (hlc/ms-of (clock layer)) 5000000)
                _ (TopologyUtils/advanceSimTime wall)
                oa (act :alice layer [(note-fact :e6 "at the wall")])
                a (send! oa)
                b (send! (act :alice layer [(note-fact :e6 "same wall")]))]
            (is (= (hlc/pack wall 0) (:stamp a)) "at the gate's wall clock: the simulated millisecond, counter 0")
            (is (= (hlc/pack wall 1) (:stamp b)) "the wall standing still: one past the task's last stamp, the counter counting")
            (testing "the counter rolling past 65,535 within one millisecond"
              (let [fid [(env/make-name :alice-agent :by-layer) 0]
                    top (send! (act :alice layer [(note-fact :e6 "to the top")] :stood-on {fid (hlc/pack wall 65534)}))
                    over (send! (act :alice layer [(note-fact :e6 "past the top")]))]
                (is (= (hlc/pack wall 65535) (:stamp top)) "the wall's millisecond, its last counter")
                (is (= (hlc/pack (inc wall) 0) (:stamp over)) "carried into the next millisecond, which the wall has not reached")
                (TopologyUtils/advanceSimTime 1)
                (is (= (hlc/pack (inc wall) 1) (:stamp (send! (act :alice layer [(note-fact :e6 "the wall at it")]))))
                    "the wall reaching the carried millisecond: the counter counts on")
                (TopologyUtils/advanceSimTime 1)
                (is (= (hlc/pack (+ wall 2) 0) (:stamp (send! (act :alice layer [(note-fact :e6 "the wall past it")]))))
                    "the wall past the task's clock: its millisecond, counter 0")))
            (testing "a resend after the wall moved on is answered with its recorded stamp, not a new one"
              (let [c1 (clock layer)]
                (is (= a (send! oa)))
                (is (= [wall 0] ((juxt hlc/ms-of hlc/counter-of) (:stamp (rec (:name oa))))))
                (is (= c1 (clock layer)) "no stamp given"))))))

      (testing "an act of 600 facts is admitted whole, at one stamp, in one decision"
        (let [facts (vec (for [i (range 600)] (note-fact (keyword (str "big" i)) i)))
              o (act :alice :alice-agent facts)
              a (send! o)
              rs (rows :alice-agent (:name o))]
          (is (= :yes (:answer a)))
          (is (= 600 (count rs)))
          (is (= (range 600) (map :v rs)))
          (let [raw-row (raw-answer-row layers-ps :alice-agent (:name o) 599)]
            (is (= {:e :big599 :k :note :v nil :replaces nil :mark #{}} (select-keys raw-row [:e :k :v :replaces :mark]))
                "stage 2: the value sealed in the row, no plaintext")
            (is (= [:alice] (:required (:lock raw-row))) "an agent layer: the lock in the record"))
          (is (= (:stamp a) (head :alice-agent :big0 :note [(:name o) 0]) (head :alice-agent :big599 :note [(:name o) 599])))))

      (testing "records a client should never send are refused as data and the gate never throws (I-G1, F6, F13)"
        (let [ok (act :alice :alice [(note-fact :e7 "fine")])
              bad [[42 :malformed] ["offer" :malformed] [[1 2 3] :malformed] [{:junk 1} :unknown-part]
                   [(assoc ok :extra 1) :unknown-part]
                   [(assoc-in ok [:facts 0 :colour] :red) :unknown-part]
                   [(assoc ok :version 2) :unknown-version]
                   [(assoc ok :name (str (:name ok))) :bad-name]
                   [(assoc ok :facts []) :empty-act]
                   [(assoc ok :who :store :permission nil) :reserved-who]
                   [(assoc ok :subjects (set (map #(keyword (str "p" %)) (range 257)))) :malformed]
                   [(assoc ok :stood-on {[(:name ok) 0] Long/MAX_VALUE}) :malformed]]]
          (doseq [[x want] bad]
            (let [r (try (send! x) (catch Exception e {:threw (str e)}))]
              (is (= [:no want] [(:answer r) (:reason r)])
                  (str "answered through the ack, the append did not fail: " (pr-str (if (map? x) (dissoc x :facts :subjects) x))))))
          (is (nil? (raw-answer :alice (:name ok))) "none of them was recorded under the name")
          (testing "stage 2 ([V-F4]): a value outside the domain is sealed, so the gate sees it only when it opens it: recorded :malformed-value, with a stamp"
            (doseq [v [(nest 40) '(1 2) 1/3 (keyword "a b")]]
              (let [o (act :alice :alice [(note-fact :e7 v)])
                    a (send! o)]
                (is (= [:no :malformed-value] [(:answer a) (:reason a)]) (pr-str v))
                (is (number? (:stamp a)) "recorded, with a stamp")
                (is (= :malformed-value (:reason (rec (:name o)))))))
            (is (= :not-sealed (:reason (get (foreign-append! (:depot st) (assoc ok :name (env/make-name :alice :by-layer)) :ack) "gate")))
                "the same value unsealed, past the door: refused on its face"))
          (let [r (try (send! (update ok :name subv)) (catch Exception e {:client-threw (.getName (class e))}))]
            (say "a subvec name through the depot:" (select-keys r [:answer :reason :client-threw]))
            (is (or (= :yes (:answer r)) (contains? r :client-threw))
                "a subvec name arrives a PersistentVector and is decided, or the client will not send it: the gate never sees a subvec"))
          (is (= :yes (:answer (send! (act :alice :alice [(note-fact :e7 "after")])))) "the gate keeps deciding")
          (is (empty? @inject/fired) "no crash was injected")))

      (testing "offers at once: one name twice (E1 N1), a mis-tagged copy, two replaces of one head, an offer racing its layer's making"
        (let [o (act :alice :alice [(note-fact :e8 "twice")])
              [a b] (map deref (doall [(future (send! o)) (future (send! o))]))]
          (is (= :yes (:answer a)))
          (is (= a b) "the same content twice in flight: one decision, one answer, one stamp")
          (is (= 1 (count (rows :alice (:name o))))))
        (let [nm (env/make-name :alice :by-layer)
              oa (act :alice :alice [(note-fact :e8 "a")] :name nm)
              ob (act :alice :alice [(note-fact :e8 "b")] :name nm)
              [a b] (map deref (doall [(future (send! oa)) (future (send! ob))]))
              r (rec nm)
              ;; stage 2: the two differ in value only, so their parts digests are equal; the winner is the one admitted
              winner (if (= :yes (:answer a)) oa ob)
              outcome #(if (= :yes (:answer %)) :yes (:reason %))]
          (is (= #{:yes :name-taken} (set [(outcome a) (outcome b)]))
              "other content under one name in flight: whichever is decided first holds the name")
          (is (contains? #{(env/offer-digest oa) (env/offer-digest ob)} (:digest r)))
          (is (= [(:v (first (:facts winner)))] (map :v (rows :alice nm)))))
        (let [o (act :alice :alice [(note-fact :e13 "original")])
              mis (assoc o :layer :alice-hand :permission [:alice :alice-hand :alice-hand])
              [a b] (map deref (doall [(future (send! o)) (future (send! mis))]))]
          (is (= :yes (:answer a)) "E1 N1 × tag mismatch in flight: the original is unaffected")
          (is (= :mis-tagged (:reason b)))
          (is (= (:stamp a) (:stamp (rec (:name o))))))
        (let [base (act :alice :alice [(note-fact :e12 "head")])
              _ (send! base)
              f [(:name base) 0]
              r1 (act :alice :alice [(assoc (note-fact :e12 "one") :replaces f)])
              r2 (act :alice :alice [(assoc (note-fact :e12 "two") :replaces f)])
              [a b] (map deref (doall [(future (send! r1)) (future (send! r2))]))
              outcome #(if (= :yes (:answer %)) :yes (:reason %))]
          (is (= #{:yes :stale-replaces} (set [(outcome a) (outcome b)]))
              "two replaces of one head at once: the first decided is admitted, the other refused stale (OP1)"))
        (let [_ (is (= :yes (:answer (c/make-person! st :vic))) "stage 2 (L11): the owner is a person")
              make (c/make-layer-offer :vic {:kind :personal :owner :vic})
              early (act :vic :vic [(note-fact :e0 "early")])
              [a b] (map deref (doall [(future (send! make)) (future (send! early))]))]
          (is (= :yes (:answer a)))
          (is (= :no-such-lock (:reason b))
              "an offer racing its layer's making is refused as data (OP5); stage 2: its lease is refused (no layer yet, or no permission), so it cites no lock ([V-F4])")))

      ;; the crashes come last: each restarts the worker and replays records

      (testing "a crash before the writes: the record replays from its start and is decided once (R7, RQ 2, RQ 4)"
        (let [o (act :alice :alice [(note-fact :e9 "crash-before") (note-fact :e10 "second fact")])
              nm (:name o)
              d (env/offer-digest o)]
          (inject/watch! nm)
          (inject/arm! :before-writes nm)
          (let [first-try (try (send! o) (catch Exception _ :append-threw))
                answered (wait-until #(let [r (c/lookup st nm d)] (when (map? r) r)) 120000)
                again (c/offer-until-answered! st o)
                r (rec nm)]
            (say "crash before the writes: the append" (if (= :append-threw first-try) "threw" (str "returned " first-try))
                 "| the gate saw the offer" (inject/count-of :seen nm) "times, reached its writes"
                 (inject/count-of :before-writes nm) "times")
            (is (= 1 (inject/fired-count :before-writes nm)) "the crash fired")
            (is (map? answered) "answered with no resend: the record replayed after the worker restarted")
            (is (<= 2 (inject/count-of :seen nm)) "processed at least twice (R4)")
            (is (= :yes (:answer r)))
            (is (= {:answer :yes :reason nil :stamp (:stamp r) :name nm} again) "a resend gets the recorded answer")
            (is (= 2 (count (rows :alice nm))) "admitted once")
            (is (= [(:stamp r) (:stamp r)] [(head :alice :e9 :note [nm 0]) (head :alice :e10 :note [nm 1])]) "one stamp")
            (is (<= (:stamp r) (clock :alice)))
            (is (= :yes (model-answer [(m-offer) [:step 0] [:failover :stream 0]] 0))
                "the model gives the same answer across its failover"))))

      (testing "a crash after the writes, inside the event: the writes are discarded and the replay decides once"
        (let [o (act :alice :alice-hand [(note-fact :e9 "crash-after")])
              nm (:name o)
              d (env/offer-digest o)]
          (inject/watch! nm)
          (inject/arm! :after-writes nm)
          (let [first-try (try (send! o) (catch Exception _ :append-threw))
                answered (wait-until #(let [r (c/lookup st nm d)] (when (map? r) r)) 120000)
                r (rec nm)]
            (say "crash after the writes: the append" (if (= :append-threw first-try) "threw" (str "returned " first-try))
                 "| decision code ran" (inject/count-of :before-writes nm) "times")
            (is (= 1 (inject/fired-count :after-writes nm)) "the crash fired")
            (is (map? answered))
            (is (<= 2 (inject/count-of :before-writes nm)) "the decision ran at least twice: the first attempt's writes were discarded")
            (is (= :yes (:answer r)))
            (is (= (:stamp answered) (:stamp r)))
            (is (= 1 (count (rows :alice-hand nm))) "admitted once")
            (is (= (:stamp r) (head :alice-hand :e9 :note [nm 0])) "one stamp"))))

      (testing "a completed offer replayed by a later crash on the same layer: answer, stamp and rows unchanged (F13, RQ 2, D4, E1 N2 × crash)"
        (let [attempt (fn []
                        (let [a (act :alice :alice-agent [(note-fact :e11 "completed")])
                              ;; stage 2: a refusal recorded after the delivery (Bob's no-permission is now a face refusal, [V-F4])
                              refused (act :alice :alice-agent [(note-fact :e11 "refused") {:e :e11 :k :tag :v 1 :layer :alice-hand}])
                              b (act :alice :alice-agent [(note-fact :e11 "crashes")])
                              an (:name a)
                              rn (:name refused)
                              bn (:name b)]
                          (inject/watch! an rn bn)
                          (let [aa (send! a)
                                r-before (rec an)
                                ra (send! refused)
                                rr-before (rec rn)]
                            (inject/arm! :before-writes bn)
                            (try (send! b) (catch Exception _ nil))
                            (wait-until #(map? (c/lookup st bn (env/offer-digest b))) 120000)
                            (wait-until #(<= 2 (inject/count-of :seen bn)) 30000)
                            {:aa aa :resent (c/offer-until-answered! st a) :clock-after (clock :alice-agent)
                             :r-before r-before :r-after (rec an) :rows (rows :alice-agent an)
                             :seen (inject/count-of :seen an) :recorded (inject/count-of :recorded an)
                             :decided (inject/count-of :before-writes an)
                             :ra ra :rr-before rr-before :rr-after (rec rn)
                             :r-seen (inject/count-of :seen rn) :r-recorded (inject/count-of :recorded rn)
                             :r-decided (inject/count-of :before-writes rn)})))
              results (loop [i 0 acc []]
                        (let [acc (conj acc (attempt))]
                          (if (or (<= 2 (:seen (peek acc))) (>= i 4)) acc (recur (inc i) acc))))]
          (say "completed-record replay: attempts" (count results)
               "| times the completed offer was seen" (mapv :seen results)
               "| answered from its record" (mapv :recorded results))
          (doseq [{:keys [aa resent clock-after r-before r-after rows decided ra rr-before rr-after r-decided]} results]
            (is (= :yes (:answer aa)))
            (is (= r-before r-after) "the same answer and stamp")
            (is (= aa resent) "a resend after the replay: the recorded answer and stamp")
            (is (< (:stamp aa) clock-after) "though the task's clock has moved past it")
            (is (= 1 (count rows)) "no second admission")
            (is (= 1 decided) "the replay decided nothing and gave no stamp")
            (is (= :fact-outside-the-acts-layer (:reason ra)))
            (is (= rr-before rr-after) "E1 N3 × crash: the same no and stamp")
            (is (= 1 r-decided)))
          (is (some #(and (<= 2 (:seen %)) (<= 1 (:recorded %))) results)
              "in some attempt the completed offer was processed again after the crash, from its record (R4: at least once)")
          (is (some #(and (<= 2 (:r-seen %)) (<= 1 (:r-recorded %))) results)
              "and the completed refusal likewise")))

      (testing "E4 P1 (a session root) × revoke: offers citing it refused, the permissions kept under it stay live (no cascade, P8)"
        ;; stage 2: Alice's pool in :alice-hand stocked before the revoke, so her act cites a lock and its refusal is recorded
        (retrying #(c/stock! st :alice :alice-hand :door/alice 4))
        (let [root [:alice :alice-hand :alice-hand]
              rv (retrying #(c/revoke-offer st root))]
          (is (= :yes (:answer (c/offer-until-answered! st rv))))
          (is (= :permission-revoked
                 (:reason (c/offer-until-answered! st (act :alice :alice-hand [(note-fact :e14 "after the root")])))))
          (doseq [pid [[:alice :alice :alice-hand] [:alice :alice-agent :alice-hand]
                       [:alice :group :alice-hand] [:alice :base :alice-hand]]]
            (is (nil? (:revoked (retrying #(c/permission st :alice-hand pid)))) (str "still live: " (pr-str pid))))
          (is (= :yes (:answer (c/offer-until-answered! st (act :alice :alice-agent [(note-fact :e14 "her agent layer")]))))))))))
