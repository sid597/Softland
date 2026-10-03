(ns rig.store.screen-test
  "The pointer screen's pieces of the store, on an in-process cluster of 2
  tasks: the bells a standing reader's proxies are pushed by (a one-owner
  layer's last admitted stamp, the settled frontier, a person's entry on
  each task); one record per gesture (every read a gesture queried, in one
  entry act); one act for the lines of many standing reads; and a made
  group accepting a new member.

  Tests assert 'at least once' (R4). Vocabulary: \"key\" is a fact's key;
  \"lock\" an encryption key."
  (:require [clojure.test :refer [deftest is testing]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [rig.store.bell :as bell]
            [rig.store.client :as c]
            [rig.store.inject :as inject]
            [rig.store.micro-client :as mc]
            [rig.store.module :as m]
            [rig.store.read-exit :as rx]
            [rig.store.standing :as sg]
            [rig.store.toy-grammars :as tg]))

(defn- say [& xs] (apply println "OBSERVED" xs) (flush))

(defn- retrying [f]
  (let [deadline (+ (System/currentTimeMillis) 60000)]
    (loop []
      (let [r (try [:ok (f)] (catch Exception e [:err e]))]
        (cond (= :ok (first r)) (second r)
              (> (System/currentTimeMillis) deadline) (throw (second r))
              :else (do (Thread/sleep 100) (recur)))))))

(defn- wait-until
  ([f] (wait-until f 30000))
  ([f timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop []
       (let [v (try (f) (catch Exception _ nil))]
         (if (or v (> (System/currentTimeMillis) deadline)) v (do (Thread/sleep 25) (recur))))))))

(defn- ok! [st o]
  (let [a (c/offer-until-answered! st o)]
    (is (= :yes (:answer a)) (pr-str [(:layer o) (map :k (:facts o)) a]))
    a))

(defn- frontiers [st n]
  (mapv #(:frontier (retrying (fn [] (foreign-invoke-query (:micro-progress st) %)))) (range n)))

(defn- settle! [st n b]
  (wait-until #(every? (fn [f] (<= b (or f -1))) (frontiers st n))))

(def alice {:reader :alice :reader-kind :person :working :alice-hand :permission [:alice :alice-hand :alice-hand]})
(def bob {:reader :bob :reader-kind :person :working :bob :permission [:bob :bob :bob]})

(defn- act-facts
  "An admitted act's facts, opened (the store's own view)."
  [st layer nm]
  (retrying #(c/facts st layer nm)))

(deftest pointer-screen-store
  (inject/reset-all!)
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc m/Store {:tasks 2 :threads 2 :workers 1})
    (let [st (merge (mc/connect ipc) (rx/connect ipc))
          tasks 2]

      (testing "seed: the model's one-owner world, Bob's layer, the base, the toy grammars"
        (is (every? #(= :yes (:answer %)) (c/seed! st)))
        (ok! st (c/make-layer-offer :bob {:kind :personal :owner :bob}))
        (ok! st (c/grant-offer st [:bob :bob :bob]))
        (is (every? #(= :yes (:answer %)) (mc/make-base! st)))
        (is (every? #(= :yes (:answer %)) (tg/write! st [:alice :alice-hand :bob :base]))))

      (testing "the layer's bell: its proxy is pushed each admitted act's stamp, and nothing for a refusal"
        (let [rings (atom [])
              p (bell/layer-bell st :alice #(swap! rings conj %))
              a (ok! st (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                                  :facts [{:e :s1 :k :note :v "first"}]}))]
          (try
            (is (wait-until #(some #{(:stamp a)} @rings)) (pr-str [@rings (:stamp a)]))
            (let [before (count @rings)
                  no (c/offer-until-answered! st (c/build {:who :bob :layer :alice :class :by-layer
                                                           :permission [:bob :alice :alice]
                                                           :facts [{:e :s1 :k :note :v "not bob's"}]}))]
              (is (= :no (:answer no)) (pr-str no))
              (Thread/sleep 300)
              (is (= before (count @rings)) "a refused act rings nothing"))
            (let [b (ok! st (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                                      :facts [{:e :s2 :k :note :v "second"}]}))]
              (is (wait-until #(some #{(:stamp b)} @rings)))
              (is (< (:stamp a) (:stamp b)))
              (say "bell rings" @rings))
            (finally (close! p)))))

      (testing "one record per gesture: every read the gesture queried, one entry act, an entity per read"
        (let [_ (ok! st (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                                  :facts [{:e :g1 :k :note :v "pointed at"}]}))
              q1 (rx/checked-query st alice {:layer :alice :read [:pattern [:k :note]]})
              q2 (rx/checked-query st alice {:layer :base :read [:pattern [:k :title]]})
              q3 (rx/checked-query st alice {:layer :bob :read [:pattern [:k :note]]})
              r (rx/record! st alice [q1 q2 q3])]
          (is (= :yes (:answer r)) (pr-str r))
          (is (contains? (second q3) :refused) "Bob's own layer is not Alice's to read")
          (let [facts (act-facts st :alice-hand (:entry r))]
            (is (= 2 (count facts)) "two lines: the refused read records nothing, as read! records none")
            (is (= #{:read/pattern} (set (map :k facts))))
            (is (= 2 (count (distinct (map :e facts)))) "each read on an entity of its own")
            (is (every? #(= :shown (get-in % [:v :role])) facts))
            (is (every? #(vector? (get-in % [:v :exact])) facts) "a person's pattern lines carry the exact list")
            (say "gesture record" (:entry r) (mapv (juxt :e #(get-in % [:v :pattern]) #(get-in % [:v :count])) facts)))
          (is (= {:answer :none} (rx/record! st alice [q3])) "nothing to record, no act")))

      (let [spec-a (merge alice {:layer :alice :pattern [:k :note] :limit 50})
            spec-b (merge alice {:layer :base :pattern [:k :title] :limit 50})
            ea (sg/fresh-entry st :alice-hand)
            eb (sg/fresh-entry st :alice-hand)
            oa (sg/open-lines st spec-a ea)
            ob (sg/open-lines st spec-b eb)
            !states (atom {ea (:state oa) eb (:state ob)})]
        (testing "one act opens two standing reads"
          (is (every? :state [oa ob]) (pr-str [oa ob]))
          (let [r (sg/offer-lines! st alice (into (:facts oa) (:facts ob)))]
            (is (= :yes (:answer r)) (pr-str r))
            (let [facts (act-facts st :alice-hand (:entry r))]
              (is (= #{ea eb} (set (map :e facts))))
              (is (= 2 (count (filter #(= :read/standing (:k %)) facts))))
              (is (every? #(contains? (:mark %) :own-row) facts)))))

        (testing "one act delivers both: something new in each layer"
          (ok! st (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                            :facts [{:e :s3 :k :note :v "third"}]}))
          (ok! st (c/build {:who :alice :layer :base :class :by-layer
                            :permission [:alice :base :base [:operator :base :base]]
                            :facts [{:e :t1 :k :title :v "a title"}]}))
          (let [da (sg/delivery-lines st (get @!states ea))
                db (sg/delivery-lines st (get @!states eb))]
            (is (and (:facts da) (:facts db)) (pr-str [da db]))
            (is (= ["third"] (mapv :value (:rows da))))
            (is (= ["a title"] (mapv :value (:rows db))))
            (let [r (sg/offer-lines! st alice (into (:facts da) (:facts db)))]
              (is (= :yes (:answer r)))
              (swap! !states assoc ea (:state da) eb (:state db)))
            (is (:nothing-new (sg/delivery-lines st (get @!states ea))) "nothing new after the delivery")))

        (testing "one act closes both, and the session holds no open entry after"
          (let [facts (into (sg/close-lines st alice ea :unsubscribe) (sg/close-lines st alice eb :unsubscribe))
                r (sg/offer-lines! st alice facts)]
            (is (= :yes (:answer r)))
            (let [open (foreign-invoke-query (:standing-open st) :alice-hand (c/default-session :alice))]
              (is (not-any? #{ea eb} (if (sequential? open) open [])) (pr-str open))))))

      (testing "a made group accepts a new member; the member reads it from then on"
        (let [pair {:members #{:alice} :root [:pair :pair :pair] :grants [[:alice :pair :pair [:pair :pair :pair]]]}
              {:keys [reclass made]} (mc/make-group! st :pair pair)
              rings (atom [])
              p (bell/frontier-bell st #(swap! rings conj %))]
          (try
            (is (= :yes (get-in reclass [:answer :answer])) "the first group re-classes the base")
            (is (= :yes (:answer made)) (pr-str made))
            (is (every? #(= :yes (:answer %)) (tg/write-micro! st [:pair])))
            (let [w (mc/write! st {:who :alice :layer :pair :permission [:alice :pair :pair [:pair :pair :pair]]
                                   :session :alice-p :facts [{:e :n1 :k :note :v "in the pair"}]})
                  b (:batch (mc/record-of st (get-in w [:offer :name])))]
              (is (= :yes (get-in w [:answer :answer])) (pr-str (:answer w)))
              (settle! st tasks b)
              (is (wait-until #(seq @rings)) "the frontier's bell rang for the batch")
              (is (= {:refused :not-visible} (select-keys (rx/read! st (merge bob {:layer :pair :read [:pattern [:k :note]]})) [:refused]))
                  "Bob is not a member yet")
              (let [added (mc/add-members! st :pair [:bob])]
                (is (= :yes (:answer added)) (pr-str added))
                (is (contains? (mc/members-of st :pair) :bob) (pr-str (mc/members-of st :pair)))
                (wait-until #(let [r (rx/read! st (merge bob {:layer :pair :read [:pattern [:k :note]]}))]
                               (not (contains? r :refused))))
                (let [r (rx/read! st (merge bob {:layer :pair :read [:pattern [:k :note]]}))]
                  (is (= ["in the pair"] (mapv :value (:rows r))) (pr-str r))
                  (say "bob reads the pair" (:moment r) (mapv :value (:rows r))))
                (let [w2 (mc/write! st {:who :bob :layer :pair :permission [:bob :pair :pair [:pair :pair :pair]]
                                        :session :bob-p :facts [{:e :n2 :k :note :v "bob in the pair"}]})]
                  (is (= :yes (get-in w2 [:answer :answer])) (str "Bob writes under his new permission " (pr-str (:answer w2)))))
                (let [late (mc/offer! st (mc/build {:who :operator :layer :base :facts [{:e :base :k :members :v #{:bob}}]}))]
                  (is (= :no (:answer late)))
                  (is (= :layer-already-made (:reason late)) "a later :members is a group's only"))))
            (finally (close! p)))))

      (testing "a person's bell rings on every task once their forget has erased there"
        (is (= :yes (:answer (mc/make-person! st :gus))))
        (let [ks (bell/task-keys st)
              seen (atom {})
              ps (mapv (fn [k] (bell/person-bell st :gus k #(swap! seen assoc k %))) ks)]
          (try
            (is (= tasks (count (distinct ks))) (pr-str ks))
            (is (= :yes (:answer (mc/forget-person! st :gus))))
            (is (wait-until #(= (set ks) (set (keep (fn [[k e]] (when (:erased-at e) k)) @seen)))))
            (is (every? #(nil? (:lock %)) (vals @seen)) "the lock is gone on every task")
            (finally (run! close! ps))))))))
