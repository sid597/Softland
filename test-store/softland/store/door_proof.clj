(ns softland.store.door-proof
  "The proof of the door, on Rama's in-process cluster with the rig's module
  as it is. One command, from the repository root:

    clojure -M:store -m softland.store.door-proof

  Each act here goes through the door, the seed also. The proof builds the
  operator's acts itself, because they are converters and not the door. It
  reads the two acts depots itself, with Rama's own calls, because the door
  has no read of a depot.

  It prints each thing that it showed on a line that starts with SHOWN,
  prints those lines again together at the end, and exits with a status
  that is not zero when a check fails.

  State. It owns one fresh in-process cluster and its handles for one run.
  It also changes state outside the door: it pauses the microbatch topology,
  arms the rig's fault hooks (rig.store.inject, atoms that the whole JVM
  shares), and rebinds two of the door's bounds for single calls. A pass
  shows the door on the in-process cluster in one JVM, not on a deployed
  cluster. See README.md.

  What it shows, in order:

    0. the sealed view of an act goes through the microbatch gate's parse
       as the sealed act does (no cluster);
    1. the seed: stream layers and a microbatch layer, made through the
       door, and each answer read back by name;
    2. one act by a person, with one value, into that person's own layer;
    3. the same into a microbatch layer, through the same entry;
    4. one act that a gate refuses in its decision; one act sent two times
       under one name; a name that the caller gives; a series named whole
       before its first act goes;
    5. acts that the door refuses, and no depot holds one of them;
    6. the locks: a lease for more than 64, one lock for the act in both
       classes, and a session that closes;
    7. a change of class between two acts;
    8. the microbatch gate held: a record that the gate decided for another
       sealed form is not the answer to this sending;
    9. sendings to the stream gate that fail: the same sealed bytes go
       again, and the answer is the gate's. These restart the worker;
   10. a door that starts again takes its unused locks by query."
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [com.rpl.rama :as r]
            [com.rpl.rama.test :as rtest]
            [rig.store.envelope :as act]
            [rig.store.inject :as inject]
            [rig.store.locks :as locks]
            [rig.store.micro :as micro]
            [rig.store.module :as module]
            [softland.store.door :as door])
  (:import [clojure.lang ExceptionInfo]
           [java.nio.charset StandardCharsets]
           [java.util Arrays]))

(defonce ^:private shown-lines (atom []))

(defn- shown
  "Print one line of what the proof observed, and keep it for the recap that
  `-main` prints at the end."
  [& xs]
  (let [line (apply print-str "SHOWN" xs)]
    (swap! shown-lines conj line)
    (println line)
    (flush)))

(defn- brief
  "An answer without its name, for a line that already says which act it is."
  [answer]
  (pr-str (if (map? answer) (dissoc answer :name) answer)))

(defn- depot-records
  "Each record that a depot holds, from each partition, as the sender sent it."
  [depot]
  (vec (for [part (range (:num-partitions (r/foreign-object-info depot)))
             :let [{:keys [start-offset end-offset]} (r/foreign-depot-partition-info depot part)]
             record (r/foreign-depot-read depot part start-offset end-offset)]
         record)))

(defn- copies
  "The records under a name in a depot."
  [depot nm]
  (filterv #(= nm (:name %)) (depot-records depot)))

(defn- lease-acts
  "The lease acts by `who` into `layer` that a depot holds, in `session` if one is given."
  ([depot who layer]
   (filterv #(and (= who (:who %)) (= layer (:layer %)) (= :lease (:k (first (:facts %))))) (depot-records depot)))
  ([depot who layer session]
   (filterv #(= session (:session %)) (lease-acts depot who layer))))

(defn- lease-counts
  "How many locks each of some lease acts asks for."
  [leases]
  (mapv #(:count (:v (first (:facts %)))) leases))

(defn- sealed-bytes
  "The sealed bytes of the first fact of a depot's copy, as a value that can be compared."
  [record]
  (vec (:sealed (first (:facts record)))))

(defn- sealed-as-sent?
  "Whether a fact of a depot's copy holds sealed bytes and a lock id and no
  plain value: no :v, sealed bytes as long as the value's canonical text
  and the seal's overhead, and the text itself not in them."
  [fact value]
  (let [plain (locks/canonical-bytes value)
        sealed (:sealed fact)]
    (and (not (contains? fact :v))
         (bytes? sealed)
         (act/fid? (:lock-id fact))
         (= (alength ^bytes sealed) (+ (alength ^bytes plain) locks/seal-overhead))
         (not (.contains (String. ^bytes sealed StandardCharsets/ISO_8859_1)
                         (String. ^bytes plain StandardCharsets/ISO_8859_1))))))

(defn- as-sent
  "A fact of a depot's copy, with the length of its sealed bytes in their place."
  [fact]
  (pr-str (update fact :sealed #(str (alength ^bytes %) " sealed bytes"))))

(defn- thrown-by
  "The data of the door's \"no answer\" throw, when a call threw it; else what the call gave."
  [f]
  (try (f)
       (catch ExceptionInfo ex
         (if (= :no-answer (:door (ex-data ex))) (ex-data ex) (throw ex)))))

(defn- operator
  "An act of the operator into a layer: the parts that the door takes."
  [layer & facts]
  {:who :operator :layer layer :facts (vec facts)})

(def alice-own "Alice's permission in her own layer." [:alice :alice :alice])
(def alice-hand "Alice's permission in her hand layer." [:alice :alice-hand :alice-hand])
(def group-root "The group's root permission, held by the group." [:group :group :group])
(def alice-in-group "Alice's permission in the group, beneath its root." [:alice :group :group group-root])
(def bob-in-group "Bob's permission in the group, beneath its root." [:bob :group :group group-root])

(defn- sealed-view-check
  "Scene 0. The door examines an act before it seals it, so it gives the
  gate's parse the sealed view of the act. This shows that the microbatch
  gate's parse takes that view as it takes the sealed act."
  []
  (testing "the sealed view of an act through the microbatch gate's parse"
    (let [made (fn [facts & {:as more}]
                 (let [a (merge {:version act/version :who :alice :layer :group :class :by-entity
                                 :permission alice-in-group :session :s :stood-on {} :because-of nil
                                 :claimed-when 1 :subjects #{} :facts (vec facts)}
                                more)]
                   (assoc a :name (act/name-for a))))
          really-sealed (fn [a]
                          (let [lease (act/make-name (:layer a) :by-entity)]
                            (update a :facts
                                    (fn [fs]
                                      (vec (map-indexed
                                            (fn [i f]
                                              (if (act/value-fact? f)
                                                (-> f (dissoc :v)
                                                    (assoc :sealed (locks/seal (locks/fresh-lock) (locks/canonical-bytes (:v f)))
                                                           :lock-id [lease (long i)]))
                                                f))
                                            fs))))))
          shape (fn [parsed]
                  (if-let [o (:ok parsed)]
                    {:ok (update o :facts (fn [fs] (mapv #(cond-> % (contains? % :sealed) (assoc :sealed :bytes :lock-id :an-id)) fs)))}
                    parsed))
          good (made [{:e :n1 :k :note :v {:text "a value"}}
                      {:e :n2 :k :note :v 7 :mark #{:own-row}}
                      {:e :n3 :k :note :v nil :replaces [(act/make-name :group :by-entity) 0]}
                      {:e :s :k :lease :v {:count 1}}])
          view (micro/parse-micro (act/sealed-view good))
          sent (micro/parse-micro (really-sealed good))
          bad-fact (made [{:e :n1 :k :note :v "x" :colour :red}])
          bad-act (made [{:e :n1 :k :note :v "x"}] :extra 1)]
      (is (contains? view :ok) "the sealed view of a good act parses")
      (is (= (shape view) (shape sent)) "it parses to the same act as the sealed act, bytes and lock ids apart")
      (is (= (micro/parts-digest (:ok view)) (micro/parts-digest (:ok sent))) "and to the same digest")
      (is (= {:refuse :unknown-part} (micro/parse-micro (act/sealed-view bad-fact)) (micro/parse-micro (really-sealed bad-fact)))
          "a fact with a part too many: the same refusal for the view and for the sealed act")
      (is (= {:refuse :unknown-part} (micro/parse-micro (act/sealed-view bad-act)) (micro/parse-micro (really-sealed bad-act)))
          "an act with a part too many: the same refusal for both")
      (is (= {:refuse :not-sealed} (micro/parse-micro good)) "the plain act, not viewed, is refused")
      (is (= act/control-keys micro/control-keys) "the act's and the microbatch gate's control keys are one set")
      (shown "0. the sealed view parses as the sealed act does:" (= (shape view) (shape sent))
             "| same digest:" (= (micro/parts-digest (:ok view)) (micro/parts-digest (:ok sent)))))))

(deftest the-door
  (sealed-view-check)
  (inject/reset-all!)
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc module/Store {:tasks 4 :threads 2 :workers 1})
    (let [module-name (r/get-module-name module/Store)
          d (door/connect ipc module-name)
          ;; a second door: new handles and no memory. It only reads answers.
          second-door (door/connect ipc module-name)
          stream-depot (r/foreign-depot ipc module-name "*offers")
          microbatch-depot (r/foreign-depot ipc module-name "*micro-offers")
          in-a-depot (fn [nm] (into (copies stream-depot nm) (copies microbatch-depot nm)))
          alice (fn [layer permission & facts]
                  {:who :alice :layer layer :permission permission :session :alice-s1 :facts (vec facts)})]
      (shown "the module is" module-name "on an in-process cluster of 4 tasks")

      ;; --------------------------------------------------------- 1. the seed
      (testing "the seed: each act of the operator goes through the door"
        (doseq [[what parts class gate]
                [["the store layer :people"
                  (operator :people {:e :people :k :kind :v :store} {:e :people :k :class :v :by-layer}
                            {:e :people :k :lock-grain :v :per-value})
                  :by-layer :stream]
                 ["the person :alice" (operator :people {:e :alice :k :person :v {:id :alice}}) nil :stream]
                 ["the person :bob" (operator :people {:e :bob :k :person :v {:id :bob}}) nil :stream]
                 ["alice's own layer, a stream layer"
                  (operator :alice {:e :alice :k :kind :v :personal} {:e :alice :k :owner :v :alice}
                            {:e :alice :k :class :v :by-layer} {:e :alice :k :lock-grain :v :per-value})
                  :by-layer :stream]
                 ["alice's permission in her layer"
                  (operator :alice {:e :perm-alice :k :permission :v {:id alice-own}})
                  :by-layer :stream]
                 ["alice's hand layer, a stream layer"
                  (operator :alice-hand {:e :alice-hand :k :kind :v :hand} {:e :alice-hand :k :owner :v :alice}
                            {:e :alice-hand :k :class :v :by-layer} {:e :alice-hand :k :lock-grain :v :per-value})
                  :by-layer :stream]
                 ["alice's permission in her hand layer"
                  (operator :alice-hand {:e :perm-alice :k :permission :v {:id alice-hand}})
                  :by-layer :stream]
                 ["the group, a microbatch layer"
                  (operator :group {:e :group :k :kind :v :group} {:e :group :k :members :v #{:alice :bob}}
                            {:e :group :k :class :v :by-entity} {:e :group :k :lock-grain :v :per-value}
                            {:e :perm-group :k :permission :v {:id group-root}}
                            {:e :perm-alice :k :permission :v {:id alice-in-group}}
                            {:e :perm-bob :k :permission :v {:id bob-in-group}})
                  :by-entity :microbatch]]]
          (let [a (door/make d parts)
                answer (door/send d a)
                depot (if (= :microbatch gate) microbatch-depot stream-depot)]
            (shown "1. seed:" what "| name" (:name a) "| answer" (brief answer))
            (is (= class (nth (:name a) 1)) (str what ": the class in the name"))
            (is (= :yes (:answer answer)) what)
            (is (= 1 (count (copies depot (:name a)))) (str what ": in the acts depot of its gate"))
            (is (= answer (door/answer second-door (:name a))) (str what ": the same answer by its name")))))

      ;; ------------------------------------- 2. one act into a stream layer
      (let [text "the first act through the door"
            a (door/make d (alice :alice alice-own {:e :note-1 :k :note :v text}))
            nm (:name a)
            answer (door/send d a)
            sent (copies stream-depot nm)
            fact (first (:facts (first sent)))
            leases (lease-acts stream-depot :alice :alice)
            back (door/answer second-door nm)]
        (testing "one act by a person, with one value, into that person's own layer"
          (is (= :by-layer (:class a)) "the door read the class")
          (is (= [:alice :by-layer :offer] (subvec nm 0 3)) "and made the name for that layer and class")
          (is (uuid? (nth nm 3)))
          (is (= text (:v (first (:facts a)))) "the act as said holds the plain value")
          (is (= #{:name :answer :reason :stamp} (set (keys answer))) "the answer has its four parts")
          (is (= [nm :yes nil] ((juxt :name :answer :reason) answer)) "the stream gate answers yes")
          (is (int? (:stamp answer)) "with a stamp")
          (is (= 1 (count sent)) "the stream acts depot holds one copy")
          (is (sealed-as-sent? fact text) "its fact holds sealed bytes and a lock id and no plain value")
          (is (empty? (copies microbatch-depot nm)) "the other acts depot holds none")
          (is (= [64] (lease-counts leases)) "the door made one lease act, for 64 locks")
          (is (= (:name (first leases)) (first (:lock-id fact))) "the lock id is the lease's name and a number")
          (is (= answer back) "a second door, from the name alone, reads the same answer and stamp")
          (shown "2. the act as said:" (pr-str (select-keys a [:name :class :session :facts])))
          (shown "2. the answer:" (pr-str answer))
          (shown "2. the depot's copy of its fact:" (as-sent fact))
          (shown "2. the door's lease act, also in the depot:" (pr-str (select-keys (first leases) [:name :who :session :facts])))
          (shown "2. a second door, by the name alone:" (pr-str back)))

        ;; ---------------------------------- 3. one act into a microbatch layer
        (let [text "one act into a microbatch layer"
              b (door/make d (alice :group alice-in-group {:e :note-2 :k :note :v text}))
              bn (:name b)
              b-answer (door/send d b)
              b-sent (copies microbatch-depot bn)
              b-fact (first (:facts (first b-sent)))
              b-leases (lease-acts microbatch-depot :alice :group)
              b-back (door/answer second-door bn)]
          (testing "the same for one act into a microbatch layer, through the same entry"
            (is (= :by-entity (:class b)) "the door read the class")
            (is (= [:group :by-entity :offer] (subvec bn 0 3)))
            (is (= #{:name :answer :reason :stamp :batch} (set (keys b-answer))) "a microbatch answer has the batch also")
            (is (= [bn :yes nil] ((juxt :name :answer :reason) b-answer)) "the microbatch gate answers yes")
            (is (int? (:stamp b-answer)))
            (is (int? (:batch b-answer)))
            (is (= 1 (count b-sent)) "the microbatch acts depot holds one copy")
            (is (sealed-as-sent? b-fact text) "sealed, with a lock id and no plain value")
            (is (empty? (copies stream-depot bn)) "the other acts depot holds none")
            (is (= [1] (lease-counts b-leases)) "the door made one lease act for this act, for one lock")
            (is (= [(:name (first b-leases)) 0] (:lock-id b-fact)) "the lock id is the lease's name and 0")
            (is (= b-answer b-back) "a second door, from the name alone, reads the same answer and stamp")
            (shown "3. the act as said:" (pr-str (select-keys b [:name :class :session :facts])))
            (shown "3. the answer:" (pr-str b-answer))
            (shown "3. the depot's copy of its fact:" (as-sent b-fact))
            (shown "3. a second door, by the name alone:" (pr-str b-back))))

        ;; ------------------- 4. a gate's refusal; two times; a given name; a series
        (let [first-fact [nm 0]
              c (door/make d (assoc (alice :alice alice-own {:e :note-1 :k :note :v "a second text" :replaces first-fact})
                                    :stood-on {first-fact (:stamp answer)}))
              c-answer (door/send d c)
              e (door/make d (alice :alice alice-own {:e :note-1 :k :note :v "a third text" :replaces first-fact}))
              e-answer (door/send d e)
              e-back (door/answer second-door (:name e))]
          (testing "one act that a gate refuses in its decision: it replaces a fact that another act already replaced"
            (is (= :yes (:answer c-answer)) "the first act that replaces the fact is accepted")
            (is (< (:stamp answer) (:stamp c-answer)) "and its stamp is after the stamp that it stood on")
            (is (= [:no :stale-replaces] ((juxt :answer :reason) e-answer)) "the second is a no, with its reason")
            (is (int? (:stamp e-answer)) "and a stamp")
            (is (= e-answer e-back) "that no reads back by name")
            (shown "4. the gate's refusal:" (pr-str e-answer))
            (shown "4. read back by name:" (pr-str e-back))))

        (let [again (door/send d a)
              sent (copies stream-depot nm)
              [one two] (map #(first (:facts %)) sent)]
          (testing "one act sent two times under one name"
            (is (= answer again) "the same answer and stamp both times")
            (is (= 2 (count sent)) "the depot holds the two sendings")
            (is (and (sealed-as-sent? one text) (sealed-as-sent? two text)))
            (is (not (Arrays/equals ^bytes (:sealed one) ^bytes (:sealed two)))
                "the door sealed again for the second, because the first had its answer")
            (is (= answer (door/answer second-door nm)) "one decision: the record holds that same answer")
            (shown "4. sent two times:" (brief answer) "then" (brief again) "|" (count sent) "copies in the depot")))

        (let [other (door/make d (assoc (alice :alice alice-own {:e :note-1 :k :note :v "other content under a used name"}) :name nm))
              other-answer (door/send d other)
              elsewhere (door/make d (assoc (alice :group alice-in-group {:e :note-7 :k :note :v "a name made for another layer"})
                                            :name nm))
              sendings (count (in-a-depot nm))
              elsewhere-answer (door/send d elsewhere)]
          (testing "a name that the caller gives"
            (is (= [nm nm] [(:name other) (:name elsewhere)]) "make keeps the name")
            (is (= {:name nm :answer :no :reason :name-taken :stamp nil} other-answer)
                "other content under a used name: the gate refuses it by its digest")
            (is (= {:name nm :answer :no :reason :mis-tagged :stamp nil} elsewhere-answer)
                "a name made for another layer and class: the door refuses it")
            (is (= sendings (count (in-a-depot nm))) "and sends nothing")
            (is (= answer (door/answer second-door nm)) "the record under the name is as it was")
            (shown "4. other content under a used name:" (brief other-answer)
                   "| a name made for another layer:" (brief elsewhere-answer))))

        (let [first-of-two (door/make d (alice :alice alice-own {:e :note-14 :k :note :v "the first of a series"}))
              ;; the second is made, with its name, before the first goes; it cites the first by name
              second-of-two (door/make d (assoc (alice :alice alice-own {:e :note-14 :k :note :v nil :layer :alice
                                                                         :replaces [(:name first-of-two) 0]})
                                                :because-of (:name first-of-two) :subjects #{:bob} :claimed-when 12345))
              one (door/send d first-of-two)
              two (door/send d second-of-two)]
          (testing "a series named whole before its first act goes, with the parts that only a caller knows"
            (is (= [(:name first-of-two) #{:bob} 12345] ((juxt :because-of :subjects :claimed-when) second-of-two))
                "the door keeps the cause, the subjects and the claimed time that the caller gives")
            (is (= [:yes :yes] [(:answer one) (:answer two)]) "the gate accepts both; the second retracts the fact of the first")
            (is (< (:stamp one) (:stamp two)))
            (shown "4. a series of two, both named before the first went:" (brief one) "then" (brief two)))))

      ;; --------------------------------------- 5. the door's own refusals
      (let [too-many (door/make d (alice :alice alice-own {:e :note-3 :k :note :v "a fact with a part too many" :colour :red}))
            no-session (door/make d (dissoc (alice :alice alice-own {:e :note-4 :k :note :v "a value and no session"}) :session))
            no-permission (door/make d {:who :bob :layer :alice :permission [:bob :alice :alice] :session :bob-s1
                                        :facts [{:e :note-5 :k :note :v "a value by a writer with no permission"}]})
            records (fn [] (+ (count (depot-records stream-depot)) (count (depot-records microbatch-depot))))
            before (records)
            r1 (door/send d too-many)
            r2 (door/send d no-session)
            between (records)
            r3 (door/send d no-permission)
            after (records)
            lease (first (lease-acts stream-depot :bob :alice))]
        (testing "three acts that the door refuses"
          (is (= {:name (:name too-many) :answer :no :reason :unknown-part :stamp nil} r1)
              "a fact with a part that the act does not know")
          (is (= {:name (:name no-session) :answer :no :reason :no-session :stamp nil} r2)
              "a value fact and no session")
          (is (= {:name (:name no-permission) :answer :no :reason :no-permission :stamp nil} r3)
              "a writer with no permission: the lease's reason, under the act's name")
          (is (= before between) "the first two put nothing in a depot")
          (is (= (inc between) after) "the third put one record in a depot: the lease act")
          (is (some? lease))
          (doseq [a [too-many no-session no-permission]]
            (is (empty? (in-a-depot (:name a))) "no depot holds a record under its name")
            (is (= :no-answer (door/answer second-door (:name a))) "and no answer is recorded under its name"))
          (is (= :no-answer (door/answer second-door [:alice :by-layer :offer "not an id"]) (door/answer second-door nil))
              "something that is not a well-formed name has no answer")
          (let [recorded (door/answer second-door (:name lease))]
            (is (= [:no :no-permission] ((juxt :answer :reason) recorded)) "the gate recorded its no to the lease")
            (is (int? (:stamp recorded)))
            (shown "5. a part too many:" (pr-str r1))
            (shown "5. no session:" (pr-str r2))
            (shown "5. no permission:" (pr-str r3))
            (shown "5. the lease that the gate refused, by its own name:" (pr-str recorded))
            (shown "5. records in the two depots before, after the first two, after the third:" before between after)))

        (testing "the same checks, where the three above did not go"
          (doseq [[what parts reason]
                  [["a setting key about an entity that is not the act's layer"
                    (operator :alice {:e :somewhere-else :k :kind :v :personal})
                    :malformed-control]
                   ["into a microbatch layer: a fact with a part too many"
                    (alice :group alice-in-group {:e :note-9 :k :note :v "a part too many" :colour :red})
                    :unknown-part]
                   ["into a microbatch layer: a permission that nobody granted, so the lease is refused"
                    {:who :bob :layer :group :permission [:bob :group :group] :session :bob-s1
                     :facts [{:e :note-10 :k :note :v "a value under a permission that nobody granted"}]}
                    :no-permission]
                   ["into a layer that nobody made: the lease is refused"
                    (alice :nowhere [:alice :nowhere :nowhere] {:e :note-11 :k :note :v "a value into no layer"})
                    :no-such-layer]]]
            (let [a (door/make d parts)
                  refusal (door/send d a)]
              (is (= {:name (:name a) :answer :no :reason reason :stamp nil} refusal) what)
              (is (empty? (in-a-depot (:name a))) (str what ": no depot holds a record under its name"))
              (is (= :no-answer (door/answer second-door (:name a))) (str what ": no answer under its name"))
              (shown "5." (str what ":") (brief refusal))))))

      ;; ---------------------------------------------------- 6. the locks
      (testing "a lease for more than 64 locks"
        (let [a (door/make d {:who :alice :layer :alice :permission alice-own :session :alice-s2
                              :facts (mapv (fn [i] {:e (keyword (str "many-" i)) :k :note :v i}) (range 70))})
              answer (door/send d a)
              sent (first (copies stream-depot (:name a)))]
          (is (= :yes (:answer answer)))
          (is (= [70] (lease-counts (lease-acts stream-depot :alice :alice :alice-s2))) "one lease act, for the 70 locks that the act needs")
          (is (= 70 (count (distinct (map :lock-id (:facts sent))))) "one lock for each value")
          (shown "6. an act of 70 values:" (brief answer) "| its lease asked for"
                 (lease-counts (lease-acts stream-depot :alice :alice :alice-s2)) "locks")))

      (testing "one lock for the act, when the grain of the layer says so: a stream layer"
        (let [grain (door/make d (operator :alice {:e :alice :k :lock-grain :v :per-act}))
              grain-answer (door/send d grain)
              a (door/make d (alice :alice alice-own {:e :pair-1 :k :note :v "one"} {:e :pair-2 :k :note :v "two"}))
              answer (door/send d a)
              ids (map :lock-id (:facts (first (copies stream-depot (:name a)))))]
          (is (nil? (nth (:name grain) 1)) "an act that the store places: its name has no class")
          (is (= [:yes :yes] [(:answer grain-answer) (:answer answer)]))
          (is (= [2 1] [(count ids) (count (distinct ids))]) "the door took the grain again: two values under one lock")
          (shown "6. a stream layer, after its grain changed: two values under" (count (distinct ids)) "lock |" (brief answer))))

      (testing "the same in a microbatch layer; the change of grain has a name with no class"
        (let [grain (door/make d (operator :group {:e :group :k :lock-grain :v :per-act}))
              grain-answer (door/send d grain)
              a (door/make d (alice :group alice-in-group {:e :pair-1 :k :note :v "one"} {:e :pair-2 :k :note :v "two"}))
              answer (door/send d a)
              ids (map :lock-id (:facts (first (copies microbatch-depot (:name a)))))]
          (is (nil? (nth (:name grain) 1)))
          (is (= [1 0] [(count (copies microbatch-depot (:name grain))) (count (copies stream-depot (:name grain)))])
              "a name with no class, for a layer that the microbatch gate made, goes to the microbatch acts depot")
          (is (= [:yes :yes] [(:answer grain-answer) (:answer answer)]))
          (is (= grain-answer (door/answer second-door (:name grain)))
              "its answer by name: not in the stream record, then found in the microbatch record")
          (is (= [2 1] [(count ids) (count (distinct ids))]) "two values under one lock")
          (shown "6. a name with no class in a microbatch layer:" (:name grain) (brief grain-answer)
                 "| two values under" (count (distinct ids)) "lock")))

      (testing "a session that closes: the door drops its pooled locks"
        (let [in-s3 (fn [& facts] {:who :alice :layer :alice :permission alice-own :session :alice-s3 :facts (vec facts)})
              one (door/make-and-send d (in-s3 {:e :note-12 :k :note :v "before the session closes"}))
              before (lease-counts (lease-acts stream-depot :alice :alice :alice-s3))
              closed (door/make-and-send d (in-s3 {:e :alice-s3 :k :session-closed :v {:session :alice-s3}}))
              two (door/make-and-send d (in-s3 {:e :note-13 :k :note :v "after the session closed"}))
              after (lease-counts (lease-acts stream-depot :alice :alice :alice-s3))]
          (is (= [:yes :yes :yes] (mapv :answer [one closed two])))
          (is (= [[64] [64 64]] [before after])
              "the next act in that session did not use a pooled lock, which the gate deleted: it leased again")
          (shown "6. a session closed:" (brief closed) "| lease acts in it before" before "and after one more act" after)))

      ;; ------------------------------------------- 7. a change of class
      (testing "a change of class between two acts: the door reads the class for each act"
        (let [before (door/make d (alice :alice-hand alice-hand {:e :note-15 :k :note :v "before the change of class"}))
              before-answer (door/send d before)
              early (door/make d (alice :alice-hand alice-hand {:e :note-16 :k :note :v "made before the change, sent after it"}))
              change (door/make d (operator :alice-hand {:e :alice-hand :k :class :v :by-entity}))
              change-answer (door/send d change)
              early-answer (door/send d early)
              again (door/make d (alice :alice-hand alice-hand {:e :note-16 :k :note :v "made before the change, sent after it"}))
              again-answer (door/send d again)]
          (is (= [:by-layer :yes] [(:class before) (:answer before-answer)]))
          (is (= :yes (:answer change-answer)) "the operator changes the class of the layer")
          (is (= [:by-layer :no :class-mismatch] [(:class early) (:answer early-answer) (:reason early-answer)])
              "an act made for the class that the layer had: the gate's no, an answer like each other")
          (is (int? (:stamp early-answer)))
          (is (= early-answer (door/answer second-door (:name early))))
          (is (not= (:name early) (:name again)) "the caller makes the act again; the door made no second name for the first")
          (is (= [:by-entity :yes] [(:class again) (:answer again-answer)]) "the door read the new class")
          (is (int? (:batch again-answer)) "and the microbatch gate decided it")
          (is (= [1 0] [(count (copies microbatch-depot (:name again))) (count (copies stream-depot (:name again)))]))
          (is (= before-answer (door/answer second-door (:name before))) "the answer under the earlier name is where it was")
          (shown "7. before the change: class" (:class before) (brief before-answer))
          (shown "7. made before the change, sent after it: class" (:class early) (brief early-answer))
          (shown "7. made again after the change: class" (:class again) (brief again-answer))))

      ;; ---------------------------------- 8. the microbatch gate held
      (testing "the microbatch gate held: only this sending's own answer is its answer"
        (let [a (door/make d (alice :group alice-in-group {:e :note-17 :k :note :v "an act whose answer is in the record"}))
              nm (:name a)
              first-answer (door/send d a)
              ;; Seal the act again now, while the gate runs, because step 6 needs a lease. This is
              ;; the one place where the proof calls a step of the door itself. It gives the door a
              ;; sealed form that is not the one that the gate decided.
              _ (#'door/sealed-form d a :microbatch)
              _ (rtest/pause-microbatch-topology! ipc module-name "micro")
              waited (with-redefs [door/microbatch-wait-ms 3000] (thrown-by #(door/send d a)))
              fresh (door/make d (alice :group alice-in-group {:e :note-18 :k :note :v "an act whose lease gets no answer"}))
              lease-waited (with-redefs [door/microbatch-wait-ms 3000] (thrown-by #(door/send d fresh)))
              _ (rtest/resume-microbatch-topology! ipc module-name "micro")
              after (door/send d a)
              sent (copies microbatch-depot nm)]
          (is (= :yes (:answer first-answer)))
          (is (= {:door :no-answer :name nm} waited)
              "the record under the name, which the gate decided for another sealed form, is not this sending's answer: the door throws")
          (is (= [:no-answer (:name fresh)] [(:door lease-waited) (:name lease-waited)])
              "an act whose lease gets no answer: the throw carries the act's name")
          (is (and (act/valid-name? (:lease lease-waited)) (not= (:name fresh) (:lease lease-waited))) "with the lease's name beside it")
          (is (empty? (copies microbatch-depot (:name fresh))) "and the act did not go")
          (is (= first-answer after) "when the gate runs again, the same act gets the gate's answer")
          (is (= [1 2] (sort (vals (frequencies (map sealed-bytes sent)))))
              "the sealed form that had no answer went two times, the same bytes")
          (shown "8. gate held, a record for another sealed form: the door threw" (pr-str waited))
          (shown "8. gate held, a lease with no answer: the door threw" (pr-str lease-waited))
          (shown "8. gate running again:" (brief after) "| sendings of each sealed form:"
                 (pr-str (sort (vals (frequencies (map sealed-bytes sent))))))))

      ;; --------------------------- 9. sendings to the stream gate that fail
      (let [text "an act whose first sending fails"
            a (door/make d (alice :alice alice-own {:e :note-6 :k :note :v text}))
            nm (:name a)
            ;; the gate fails after its writes, inside its event, one time; the worker starts again
            _ (inject/arm! :after-writes nm)
            answer (door/send d a)
            sent (copies stream-depot nm)
            back (door/answer second-door nm)]
        (testing "a sending that fails: the door sends the same sealed bytes again, and the answer is the gate's"
          (is (= 1 (inject/fired-count :after-writes nm)) "the gate's first event for the act failed")
          (is (<= 2 (count sent)) "the door sent the act more than one time")
          (is (every? #(sealed-as-sent? (first (:facts %)) text) sent))
          (is (apply = (map sealed-bytes sent)) "each sending is the same sealed form, byte for byte")
          (is (apply = (map #(:lock-id (first (:facts %))) sent)) "under the same lock")
          (is (= :yes (:answer answer)))
          (is (= answer back) "the answer that the door gave is the one in the record")
          (shown "9. the gate's event failed" (inject/fired-count :after-writes nm) "time; the door sent the same bytes"
                 (count sent) "times; its answer:" (brief answer) "| in the record:" (brief back))))

      (let [text "an act that gets no answer within the bound"
            a (door/make d (alice :alice alice-own {:e :note-19 :k :note :v text}))
            nm (:name a)
            _ (inject/arm! :after-writes nm)
            ;; a bound of one sending, so that the failed one is the last
            thrown (with-redefs [door/stream-tries 1] (thrown-by #(door/send d a)))
            answer (door/send d a)
            sent (copies stream-depot nm)]
        (testing "no answer within the bound: the door throws with the name, and the caller gives the act again"
          (is (= {:door :no-answer :name nm} thrown) "the throw carries the name")
          (is (= :yes (:answer answer)) "the caller gives the same act again")
          (is (<= 2 (count sent)))
          (is (apply = (map sealed-bytes sent)) "and the same sealed bytes go: the door kept the sealed form")
          (is (= answer (door/answer second-door nm)))
          (shown "9. a bound of one sending: the door threw" (pr-str thrown) "| given again:" (brief answer)
                 "|" (count sent) "sendings, the same bytes")))

      ;; ----------------------------------- 10. a door that starts again
      (let [third-door (door/connect ipc module-name)
            leases (fn [] (lease-acts stream-depot :alice :alice :alice-s1))
            before (leases)
            text "an act through a door that started again"
            ;; the one call that makes and then sends
            answer (door/make-and-send third-door (alice :alice alice-own {:e :note-8 :k :note :v text}))
            fact (first (:facts (first (copies stream-depot (:name answer)))))]
        (testing "a door that starts again takes its unused locks by query"
          (is (= :yes (:answer answer)))
          (is (sealed-as-sent? fact text))
          (is (= 1 (count before) (count (leases))) "it made no lease act")
          (is (= (:name (first before)) (first (:lock-id fact))) "it sealed under a lock of the lease that the first door made")
          (shown "10. a door that started again:" (brief answer) "| lease acts in the session:" (count (leases))
                 "| its lock:" (pr-str (:lock-id fact))))))))

(defn -main
  "Run the proof and exit: status 0 when each check holds."
  [& _]
  (let [{:keys [fail error] :as result} (run-tests 'softland.store.door-proof)]
    (println)
    (println "THE PROOF, IN ONE PLACE")
    (run! println @shown-lines)
    (println (select-keys result [:test :pass :fail :error]))
    (shutdown-agents)
    (System/exit (if (zero? (+ fail error)) 0 1))))
