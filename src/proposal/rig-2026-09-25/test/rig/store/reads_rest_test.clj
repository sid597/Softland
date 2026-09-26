(ns rig.store.reads-rest-test
  "Stage 5b on the in-process cluster (PLAN-reads-rest.md, 'Namespaces and
  tests'), one cluster of 4 tasks, the plan's tests as `testing` blocks:
  shared-layer reads through the exit at a frontier moment (RT1, RT2, RT10,
  RT12) and the re-classed base's two eras (RT11); standing reads and their
  lines (RT3, RT9); a standing read closed at session close and after a
  crash (RT4); an agent session's close keeping or dropping its read
  entries (RT5); a forget reaching every index of both stores (RT6, RT7);
  a restore, rebuild and forget replay writing the same indexes (RT8); an
  index gap and malformed records answered as data (RT13).

  Tests assert 'at least once' (R4); crash hooks (R3) come where they are
  needed. Vocabulary: \"key\" is a fact's key; \"lock\" an encryption key."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [clojure.walk :as walk]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [rig.store.client :as c]
            [rig.store.envelope :as env]
            [rig.store.inject :as inject]
            [rig.store.micro-client :as mc]
            [rig.store.module :as m]
            [rig.store.read-exit :as rx]
            [rig.store.reads :as reads]
            [rig.store.shared-reads :as sr]
            [rig.store.standing :as sg]))

;; ------------------------------------------------------------------ helpers

(defn- say [& xs] (apply println "OBSERVED" xs) (flush))

(defn- retrying [f]
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

(defn- frontiers
  "Every task's frontier."
  [st]
  (mapv #(:frontier (retrying (fn [] (foreign-invoke-query (:micro-progress st) %)))) (range 4)))

(defn- settle!
  "Until every task's frontier is at or above batch b."
  [st b]
  (wait-until #(every? (fn [f] (<= b (or f -1))) (frontiers st))))

(defn- batch-of [st nm] (:batch (mc/record-of st nm)))

(defn- fids-of [r] (mapv :fid (:rows r)))

(defn- ok! [st o] (let [a (c/offer-until-answered! st o)] (is (= :yes (:answer a)) (pr-str [(:layer o) (map :k (:facts o)) a])) a))

(def root [:group :group :group])
(defn- gp [p] [p :group :group root])
(def bot-pid [:bot :alice-agent :alice-agent [:alice :alice-agent :alice-agent]])

(def alice {:reader :alice :reader-kind :person :working :alice-hand :permission [:alice :alice-hand :alice-hand]})
(def bob {:reader :bob :reader-kind :person :working :bob :permission [:bob :bob :bob]})

(defn- rd [st who spec] (rx/read! st (merge who spec)))

(defn- gwrite!
  "`who`'s act into the group through the micro door; waits for its batch to
  settle on every task."
  [st who facts]
  (let [{:keys [answer offer]} (mc/write! st {:who who :layer :group :permission (gp who)
                                             :session (keyword (str (name who) "-g")) :facts facts})]
    (is (= :yes (:answer answer)) (pr-str [who facts answer]))
    (settle! st (batch-of st (:name offer)))
    offer))

(defn- micro-field
  "One shared index field of a layer, whole."
  [st L f]
  (into {} (retrying #(foreign-select [(keypath L f) ALL] (:micro-state st)))))

(defn- stream-field [st L f] (into {} (retrying #(foreign-select [(keypath L f) ALL] (:layers st)))))

(def stream-fields [:ix-ek :ix-ke :ix-kv :ix-s :ix-of])

(defn- all-fields
  "Every index field of the given layers of both stores, bytes as vectors."
  [st stream-layers micro-layers]
  (bytes->vec
   {:stream (into {} (for [L stream-layers f stream-fields] [[L f] (stream-field st L f)]))
    :micro (into {} (for [L micro-layers f sr/sweep-fields] [[L f] (micro-field st L f)]))}))

(defn- entries-for
  "Every id-index entry of fact `fid` among a field map."
  [fields fid]
  (for [[_ m] fields [_ e] m :when (and (map? e) (= fid (:fid e)))] e))

(defn- the-entry
  "The read entry's facts as the store holds them, opened (the store's own view)."
  [st layer nm]
  (retrying #(c/facts st layer nm)))

(defn- lines
  "The opened values of an entry entity's facts under key k in a working
  layer, found through its id index (the store's own view)."
  [st layer ent k]
  (let [fids (->> (vals (stream-field st layer :ix-ek)) (filter #(and (= ent (:e %)) (= k (:k %))))
                  (sort-by (juxt :stamp #(second (:fid %)))) (map :fid))]
    (vec (for [[nm idx] fids] (:v (nth (the-entry st layer nm) idx))))))

;; ------------------------------------------------------------------ the test

(deftest reads-rest
  (inject/reset-all!)
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc m/Store {:tasks 4 :threads 2 :workers 1})
    (let [st (merge (mc/connect ipc) (rx/connect ipc))]

      (testing "seed: the one-owner world, Bob's layer, the base with a stream-era fact, then the group (the base re-classed)"
        (is (every? #(= :yes (:answer %)) (c/seed! st)))
        (is (= :yes (:answer (mc/make-person! st :carol))))
        (ok! st (c/make-layer-offer :bob {:kind :personal :owner :bob}))
        (ok! st (c/grant-offer st [:bob :bob :bob]))
        (ok! st (c/grant-offer st bot-pid))
        (is (every? #(= :yes (:answer %)) (mc/make-base! st))))

      (let [base-alice [:alice :base :base [:operator :base :base]]
            s-title (c/build {:who :alice :layer :base :class :by-layer :permission base-alice
                              :facts [{:e :p7 :k :title :v "stream era title"}
                                      {:e :p8 :k :title :v "stream era p8"}]})
            _ (ok! st s-title)
            s-fid [(:name s-title) 0]
            held (sg/subscribe! st (merge alice {:layer :base :pattern [:k :title] :limit 50}))]
        (testing "a standing read opened on the one-owner base, before its re-class"
          (is (:handle held) (pr-str held))
          (is (= #{s-fid [(:name s-title) 1]} (set (map :fid (:rows held))))))

        (testing "the group is made; the base is re-classed on the way (default 6)"
          (let [{:keys [reclass made]} (mc/make-group! st :group (:group mc/shared-world))]
            (is (= :yes (get-in reclass [:answer :answer])))
            (is (= :yes (:answer made)))
            (is (= :by-entity (:class (c/settings st :base))))))

        (let [m-title (mc/write! st {:who :alice :layer :base :permission base-alice :session :alice-b
                                     :facts [{:e :p7 :k :title :v "micro era title" :replaces s-fid}
                                             {:e :p9 :k :title :v "micro era p9"}]})
              _ (is (= :yes (get-in m-title [:answer :answer])) (pr-str (:answer m-title)))
              m-nm (get-in m-title [:offer :name])
              _ (settle! st (batch-of st m-nm))]
          (testing "RT11, F1: the re-classed base reads both eras, merged by the entries' fields"
            (let [r (rd st alice {:layer :base :read [:pattern [:k :title]]})]
              (is (= [s-fid [m-nm 0] [(:name s-title) 1] [m-nm 1]] (fids-of r))
                  "entity, then stamp, then fact id, across both eras")
              (is (contains? (:moment r) :frontier))
              (is (= :complete (:mark r))))
            (let [r (rd st alice {:layer :base :read [:pattern [:latest :p7 :title]]})]
              (is (= [[m-nm 0]] (fids-of r)) "the micro-era replace heads the chain, not the replaced stream-era head")
              (is (= "micro era title" (:value (first (:rows r))))))
            (let [r (rd st alice {:layer :base :read [:pattern [:ek :p7 :title]]})]
              (is (= [s-fid [m-nm 0]] (fids-of r)) "the stream-era head first"))
            (let [r (rd st alice {:layer :base :read [:point [s-fid [m-nm 1]]]})]
              (is (= ["stream era title" "micro era p9"] (mapv :value (:rows r))) "a point read of both eras")))

          (testing "F18: the standing read held across the re-class is closed :reclass and reopened on a frontier moment"
            (let [h (:handle held)
                  old-ent (:ent @h)
                  d (sg/deliver! st h)]
              (is (:reopened d) (pr-str d))
              (is (= #{s-fid [(:name s-title) 1] [m-nm 0] [m-nm 1]} (set (map :fid (:rows d))))
                  "its first read shows both eras")
              (is (not= old-ent (:ent @h)))
              (is (contains? (:scan @h) :from))
              (is (= :frontier (get-in @h [:scan :kind])))
              (is (= [:reclass] (mapv :closed-by (lines st :alice-hand old-ent :read/closed)))
                  "the old entry closed :reclass")
              (is (= :nothing-new (sg/deliver! st h)) "no tick answers :moment-kind for ever")))))

      (testing "RT1, RT2: a shared read through the exit is as of a settled frontier, and its entry records it"
        (let [e1 :g-e1 e3 :g-e3
              o (gwrite! st :alice [{:e e1 :k :note :v "g one"} {:e e3 :k :note :v "g three"}])
              nm (:name o)
              b (batch-of st nm)
              r (rd st alice {:layer :group :read [:pattern [:k :note]]})
              F (get-in r [:moment :frontier])]
          (is (int? F) (pr-str r))
          (is (<= b F))
          (is (every? #(<= (:batch %) F) (:rows r)) "no row's batch is above the recorded F")
          (is (= #{[nm 0] [nm 1]} (set (fids-of r))))
          (is (= #{"g one" "g three"} (set (map :value (:rows r)))))
          (is (= :yes (:answer (mc/lookup st nm nil nil F))) "an answer and a read at one frontier never contradict")
          (is (= (sr/max-stamp (:rows r)) (:max-stamp r)))
          (let [[line] (the-entry st :alice-hand (:entry r))]
            (is (= :read/pattern (:k line)))
            (is (= {:frontier F} (get-in line [:v :moment])))
            (is (= (:max-stamp r) (get-in line [:v :max-stamp])))
            (is (< (:max-stamp r) (:entry-stamp r)) "the entry is stamped after what it names (F1 extended)"))
          (testing "as of the frontier before its batch: neither fact; at its batch: both"
            (is (= [] (fids-of (rd st alice {:layer :group :read [:pattern [:k :note]] :as-of {:frontier (dec b)}}))))
            (is (= #{[nm 0] [nm 1]} (set (fids-of (rd st alice {:layer :group :read [:pattern [:k :note]] :as-of {:frontier b}}))))))
          (testing "the clamp: asked past the frontier, the recorded F is the task's"
            (let [r2 (rd st alice {:layer :group :read [:pattern [:e e1]] :as-of {:frontier 1000000000}})
                  F2 (get-in r2 [:moment :frontier])]
              (is (< F2 1000000000))
              (is (<= F2 (reduce max (frontiers st))))))
          (testing "RT12: [:latest] at a frontier: the old head before the replace's batch, the new one at it"
            (let [o2 (gwrite! st :alice [{:e e1 :k :note :v "g one again" :replaces [nm 0]}])
                  b2 (batch-of st (:name o2))]
              (is (= [[nm 0]] (fids-of (rd st alice {:layer :group :read [:pattern [:latest e1 :note]] :as-of {:frontier (dec b2)}}))))
              (is (= [[(:name o2) 0]] (fids-of (rd st alice {:layer :group :read [:pattern [:latest e1 :note]] :as-of {:frontier b2}}))))))))

      (testing "RT10: visibility and moments on a shared layer"
        (is (= {:refused :not-visible} (rd st {:reader :carol :reader-kind :person :working :carol-none} {:layer :group :read [:pattern [:all]]}))
            "a non-member")
        (is (= {:refused :not-visible} (rd st alice {:layer :never-made :read [:pattern [:all]]})) "the same answer for no layer")
        (is (= {:refused :moment-kind} (rd st alice {:layer :group :read [:pattern [:all]] :as-of {:stamp 5}})))
        (is (= {:refused :moment-kind} (rd st alice {:layer :alice :read [:pattern [:all]] :as-of {:frontier 5}}))
            "and a frontier on a one-owner layer")
        (is (:rows (rd st bob {:layer :group :read [:pattern [:all]]})) "Bob, a member, reads the group")
        (let [o (gwrite! st :bob [{:e :g-pt :k :note :v "pt a"} {:e :g-pt :k :tag :v 1} {:e :g-pu :k :tag :v 2}])
              nm (:name o)
              ghost [(env/make-name :group :by-entity) 0]
              r (rd st bob {:layer :group :read [:point [[nm 0] ghost [nm 1] [nm 7] [nm 2]]]})]
          (is (= [false true false true false] (mapv #(boolean (:absent %)) (:rows r))) "three rows, two absent")
          (is (every? :batch (remove :absent (:rows r))))))

      (testing "RT1's race: reads while two offerers write two-fact acts never show half an act nor a batch above F"
        (let [done (atom false)
              writers (vec (for [who [:alice :bob]]
                             (future (doall (for [i (range 12)]
                                              (:name (gwrite! st who [{:e (keyword (str "rx" i (name who))) :k :race :v i}
                                                                     {:e (keyword (str "ry" i (name who))) :k :race :v i}])))))))
              reads (future (loop [n 0 bad []]
                              (if (or @done (> n 200))
                                [n bad]
                                (let [r (rd st alice {:layer :group :read [:pattern [:k :race]]})
                                      F (get-in r [:moment :frontier])
                                      by-act (group-by (comp first :fid) (:rows r))]
                                  (recur (inc n) (cond-> bad
                                                   (some #(> (:batch %) F) (:rows r)) (conj [:above F])
                                                   (some #(not= 2 (count %)) (vals by-act)) (conj [:half F])))))))]
          (run! deref writers)
          (reset! done true)
          (let [[n bad] @reads]
            (say "race reads" n "violations" (count bad))
            (is (empty? bad) (pr-str (take 5 bad)))
            (is (pos? n)))))

      (testing "RT3: a standing read: one entry, a line only for something new, a closing at unsubscribe"
        (let [tool {:reader :bot :reader-kind :tool :for :alice :working :alice-agent :permission bot-pid :session :t1}
              a0 (ok! st (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                                   :facts [{:e :e0 :k :note :v "e0 first"}]}))
              s1 (sg/subscribe! st (merge alice {:layer :alice :pattern [:e :e0]}))
              s2 (sg/subscribe! st (merge tool {:layer :group :pattern [:k :note]}))
              h1 (:handle s1)
              h2 (:handle s2)
              ent1 (:ent @h1)]
          (is (and h1 h2) (pr-str [s1 s2]))
          (is (= 1 (count (:rows s1))))
          (is (= [1 1] [(count (lines st :alice-hand ent1 :read/standing)) (count (lines st :alice-hand ent1 :read/delivery))])
              "the opening: one :read/standing fact and one delivery line, since the opening read matched")
          (is (every? #(= :nothing-new %) (repeatedly 10 #(sg/deliver! st h1))) "ten deliveries of nothing new")
          (is (= 1 (count (lines st :alice-hand ent1 :read/delivery))) "and no line for any of them")
          (let [o (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                            :facts [{:e :e0 :k :note :v "e0 second"}]})
                a (ok! st o)
                d (sg/deliver! st h1)
                ls (lines st :alice-hand ent1 :read/delivery)]
            (is (= [[(:name o) 0]] (fids-of d)))
            (is (= 2 (count ls)))
            (is (= [[[(:name o) 0] (:stamp a)]] (:exact (last ls))) "a person's line: the exact list")
            (is (= :nothing-new (do (ok! st (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                                                    :facts [{:e :e1 :k :note :v "not e0"}]}))
                                    (sg/deliver! st h1)))
                "an unmatched fact: nothing new")
            (testing "F3: each line's :so-far is the chain through it"
              (let [chain (reductions (fn [prev l] {:fp (reads/standing-link (:fp prev) (:moment l) (:fingerprint l))})
                                      {:fp nil} ls)]
                (is (= (map :fp (rest chain)) (map (comp :fp :so-far) ls)))
                (is (= [1 2] (map (comp :n :so-far) ls)))))
            (let [u (sg/unsubscribe! st h1)
                  [closed] (lines st :alice-hand ent1 :read/closed)]
              (is (= :yes (:answer u)))
              (is (= :unsubscribe (:closed-by closed)))
              (is (= 2 (:deliveries closed)))
              (is (= :complete (:mark closed)))
              (is (= (get-in (last ls) [:so-far :fp]) (:fingerprint closed)) "the closing fingerprint is the last line's running value")))
          (let [g (gwrite! st :bob [{:e :g-st :k :note :v "for the tool"}])
                d (sg/deliver! st h2)
                [l] (take-last 1 (lines st :alice-agent (:ent @h2) :read/delivery))]
            (is (= [[(:name g) 0]] (fids-of d)))
            (is (nil? (:exact l)) "a tool's line holds no exact list")
            (is (= (reads/fingerprint [[[(:name g) 0] (:stamp (first (:rows d)))]]) (:fingerprint l)))
            (is (contains? (:moment l) :frontier))
            (is (= (:stamp (first (:rows d))) (:max-stamp l))))
          (testing "a limit of 2 and three new facts: the delivery is partial, and so is the closing mark"
            (let [s3 (sg/subscribe! st (merge alice {:layer :alice :pattern [:k :lim] :limit 2}))
                  h3 (:handle s3)]
              (ok! st (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                                :facts (for [i (range 3)] {:e (keyword (str "lim" i)) :k :lim :v i})}))
              (let [d1 (sg/deliver! st h3)
                    d2 (sg/deliver! st h3)]
                (is (= 2 (count (:rows d1))))
                (is (= 1 (count (:rows d2))) "RR10: the rest on the next delivery, each fact once")
                (is (= :partial (:mark (first (lines st :alice-hand (:ent @h3) :read/delivery)))))
                (sg/unsubscribe! st h3)
                (is (= :partial (:mark (first (lines st :alice-hand (:ent @h3) :read/closed))))))))))

      (testing "RT9: deltas are exact, and F2: a match past the scan budget is still delivered"
        (let [s (sg/subscribe! st (merge alice {:layer :alice :pattern [:k :d9] :limit 3}))
              h (:handle s)
              seen (atom [])]
          (doseq [i (range 6)]
            (ok! st (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                              :facts [{:e (keyword (str "d9-" i)) :k :d9 :v i} {:e (keyword (str "n9-" i)) :k :other :v i}]}))
            (let [d (sg/deliver! st h)] (when (map? d) (swap! seen into (map (juxt :fid :stamp) (:rows d))))))
          (loop [n 0] (let [d (sg/deliver! st h)] (when (and (map? d) (< n 10)) (swap! seen into (map (juxt :fid :stamp) (:rows d))) (recur (inc n)))))
          (let [full (rd st alice {:layer :alice :read [:pattern [:k :d9]]})]
            (is (= (set (:matched full)) (set @seen)) "the union of what was delivered is the final full read")
            (is (= (count @seen) (count (set @seen))) "and nothing twice"))
          (let [s2 (sg/subscribe! st (merge alice {:layer :alice :pattern [:k :d9b] :limit 1}))
                h2 (:handle s2)]
            (ok! st (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                              :facts (vec (for [i (range 40)] {:e (keyword (str "pad" i)) :k :pad :v i}))}))
            (let [o (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                              :facts [{:e :d9b-e :k :d9b :v 1}]})
                  _ (ok! st o)
                  [ticks got] (loop [ticks []]
                                (let [d (sg/deliver! st h2)]
                                  (if (or (map? d) (< 10 (count ticks))) [ticks d] (recur (conj ticks d)))))]
              (say "F2 ticks before the match" (count ticks))
              (is (every? #{:nothing-new} ticks))
              (is (pos? (count ticks)) "the first delta was cut by the scan budget with no match")
              (is (= [[(:name o) 0]] (fids-of (if (map? got) got {:rows []}))) (pr-str got))))))

      (testing "RT4: standing reads closed at session close, and after a crash from the record"
        (let [bot (fn [s] {:reader :bot :reader-kind :model :for :alice :working :alice-agent :permission bot-pid :session s})
              live [(:handle (sg/subscribe! st (merge (bot :s-live) {:layer :alice :pattern [:k :note]})))
                    (:handle (sg/subscribe! st (merge (bot :s-live) {:layer :group :pattern [:k :note]})))]
              crashed [(:handle (sg/subscribe! st (merge (bot :s-crash) {:layer :alice :pattern [:e :e0]})))
                       (:handle (sg/subscribe! st (merge (bot :s-crash) {:layer :group :pattern [:all]})))]]
          (is (every? some? (concat live crashed)))
          (ok! st (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                            :facts [{:e :e0 :k :note :v "for the crashed door"}]}))
          (let [nm (env/make-name :alice-agent :by-layer)]
            (inject/arm! :exit-shown nm)
            (is (thrown? Exception (sg/deliver! st (first crashed) :line-name nm))
                "the door dies after the line and before the showing")
            (inject/reset-all!))
          (let [r (sg/close-session! st (bot :s-live) live)]
            (is (= {:closed 2 :crash 0} r) "the live door closes its own two"))
          (is (= [:session-close :session-close]
                 (mapv #(:closed-by (first (lines st :alice-agent (:ent @%) :read/closed))) live)))
          (let [ents (mapv #(:ent @%) crashed)
                open-before (set (retrying #(foreign-invoke-query (:standing-open st) :alice-agent :s-crash)))
                r (sg/close-session! st {:reader :operator :working :alice-agent :permission nil :session :s-crash} [])]
            (is (= (set ents) open-before) "both of the crashed door's entries are found from the record")
            (is (= {:closed 0 :crash 2} r))
            (doseq [[h ent] (map vector crashed ents)]
              (let [ls (lines st :alice-agent ent :read/delivery)
                    [c] (lines st :alice-agent ent :read/closed)]
                (is (= :crash (:closed-by c)))
                (is (= (or (get-in (last ls) [:so-far :fp]) (reads/standing-link nil nil nil)) (:fingerprint c))
                    "the closing fingerprint is the one the live handle would have produced")
                (is (= (count ls) (:deliveries c)))))
            (is (= 2 (count (lines st :alice-agent (first ents) :read/delivery)))
                "the line recorded for the delivery never shown is counted: over-recording, the safe direction")
            (is (= [] (retrying #(foreign-invoke-query (:standing-open st) :alice-agent :s-crash)))))))

      (testing "RT5: a dropped agent session's entries cannot be opened afterwards; a kept one's can"
        (let [bot (fn [s] {:reader :bot :reader-kind :model :for :alice :working :alice-agent :permission bot-pid :session s})
              ghosts (vec (for [_ (range 70)] [(env/make-name :alice :by-layer) 0]))
              p1 (rd st (bot :s1) {:layer :alice :read [:point ghosts]})
              p2 (rd st (bot :s1) {:layer :alice :read [:pattern [:k :note]]})
              s1 (sg/subscribe! st (merge (bot :s1) {:layer :alice :pattern [:e :e0]}))
              _ (sg/unsubscribe! st (:handle s1))
              k1 (rd st (bot :s2) {:layer :alice :read [:pattern [:k :note]]})
              c1 (rx/close-session! st :bot :alice-agent :s1 :reads :drop :permission bot-pid)
              c2 (rx/close-session! st :bot :alice-agent :s2 :permission bot-pid)
              entry-facts (fn [session]
                            (for [e (vals (stream-field st :alice-agent :ix-ke))
                                  :when (and (contains? reads/read-keys (:k e))
                                             (= session (:session (c/record st (first (:fid e))))))]
                              e))]
          (is (every? :entry [p1 p2 k1]) (pr-str (map :refused [p1 p2 k1])))
          (is (= [:yes :yes] (mapv :answer [c1 c2])) (pr-str [c1 c2]))
          (is (every? #(= #{:own-row} (:mark %)) (concat (entry-facts :s1) (entry-facts :s2)))
              "FRR6: every entry fact is marked :own-row")
          (is (< 64 (count (entry-facts :s1))) "more than one drop page")
          (inject/arm! :drop-page (:name c1))
          (is (thrown? Exception (rx/drop-reads! st :alice :alice-agent :s1 (:name c1) (:stamp c1)))
              "the closer dies between two pages")
          (inject/reset-all!)
          (is (pos? (get (rx/resume-drops! st :alice [:alice-agent]) :alice-agent)) "F11: the drop resumed from the record")
          (let [dropped (entry-facts :s1)
                ledger (c/ledger st :alice-agent)
                forgets (for [e (vals (stream-field st :alice-agent :ix-ke)) :when (= :forget (:k e))] e)]
            (is (seq dropped))
            (doseq [e dropped
                    :let [row (c/raw-row st :alice-agent (:fid e))
                          lid (:lock-id row)
                          o (c/opens? st :alice-agent (:fid e))]]
              (is (some? (:erased-at e)) "its index entries are tombstones")
              (is (nil? (c/lock-row st :alice-agent lid)) "no lock row")
              (is (= :row-deleted (:how (get ledger lid))) "the ledger says the row was deleted")
              (is (= (:erased-at e) (:erased-at o)) "it opens only to its date"))
            (let [e (first dropped)
                  r (rd st alice {:layer :alice-agent :read [:point [(:fid e)]]})]
              (is (= [{:erased-at (:erased-at e)}] (mapv #(select-keys % [:erased-at :value]) (:rows r)))
                  "through the exit: the entry shows as erased, with its date and no value"))
            (is (every? #(= (:name c1) (:because-of (c/record st (first (:fid %))))) forgets)
                "every forget act is because of the close act")
            (is (= (count dropped) (count forgets)) "one forget act per dropped entry fact (FRR9 as built)"))
          (doseq [e (entry-facts :s2)
                  :let [row (c/raw-row st :alice-agent (:fid e))]]
            (is (nil? (:erased-at e)))
            (is (nil? (:lock row)) "the kept entry's lock is not in the record")
            (is (some? (c/lock-row st :alice-agent (:lock-id row))) "its lock row stands")
            (is (contains? (c/opens? st :alice-agent (:fid e)) :value) "and it opens"))))

      (testing "RT6: a value forget leaves no index entry that opens or confirms the value, in both stores"
        (let [ta "rt6 alice secret" tg "rt6 group secret"
              oa (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                           :facts [{:e :f6 :k :note :v ta}]})
              _ (ok! st oa)
              fa [(:name oa) 0]
              og (gwrite! st :alice [{:e :f6g :k :note :v tg}])
              fg [(:name og) 0]
              before (all-fields st [:alice] [:group])]
          (is (str/includes? (pr-str (:stream before)) ta) "before: the one-owner value index holds the text")
          (is (not (str/includes? (pr-str (:micro before)) tg)) "the micro value index never holds it (F12)")
          (is (= [fg] (fids-of (rd st alice {:layer :group :read [:pattern [:kv :note tg]]}))))
          (let [f1 (c/forget-value! st :alice :alice fa)
                f2 (mc/forget-value! st :group fg :f6g)
                _ (settle! st (:batch f2))
                after (all-fields st [:alice] [:group])]
            (is (= [:yes :yes] [(:answer f1) (:answer f2)]))
            (is (not (str/includes? (pr-str after) ta)))
            (is (not (str/includes? (pr-str after) tg)))
            (doseq [[fid f fields] [[fa f1 (:stream after)] [fg f2 (:micro after)]]
                    :let [es (entries-for fields fid)]]
              (is (<= 3 (count es)) (str "every id index holds the fact: " fid))
              (is (every? #(= (:stamp f) (:erased-at %)) es) "each a tombstone dated by the forget, the ledger's date")
              (is (every? #(every? nil? ((juxt :v :sealed :lock :digest) %)) es) "no value field anywhere")
              (is (not-any? #(= fid (:fid %)) (concat (vals (get fields [:alice :ix-kv])) (vals (get fields [:group :ix-kv]))))
                  "no value index entry")
              (is (not (contains? (merge (get fields [:alice :ix-of]) (get fields [:group :ix-of])) fid))))
            (is (= [] (:rows (rd st alice {:layer :alice :read [:pattern [:kv :note ta]]}))))
            (is (= [] (:rows (rd st alice {:layer :group :read [:pattern [:kv :note tg]]}))))
            (is (= [{:erased-at (:stamp f2)}]
                   (->> (:rows (rd st alice {:layer :group :read [:pattern [:e :f6g]]}))
                        (filter #(= fg (:fid %)))
                        (mapv #(select-keys % [:erased-at :value]))))
                "[:e e] shows the fact with its date only (beside the forget fact, which is about the same entity)"))))

      (testing "RT7: a person forget leaves nothing that opens or confirms a value dying with the person, and keeps the rest"
        (let [own (c/build {:who :bob :layer :bob :class :by-layer :permission [:bob :bob :bob]
                            :facts [{:e :b7 :k :note :v "rt7 bob own"}]})
              _ (ok! st own)
              am (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice] :subjects #{:bob}
                           :facts [{:e :a7 :k :note :v "rt7 alice about bob"}]})
              _ (ok! st am)
              gw (fn [subjects f]
                   (let [{:keys [answer offer]} (mc/write! st {:who :alice :layer :group :permission (gp :alice) :session :alice-g
                                                               :subjects subjects :facts [f]})]
                     (is (= :yes (:answer answer)))
                     (settle! st (batch-of st (:name offer)))
                     [(:name offer) 0]))
              g-bob (gw #{:bob} {:e :g7a :k :note :v "rt7 group bob alone"})
              g-two (gw #{:bob :carol} {:e :g7b :k :note :v "rt7 group bob and carol"})
              g-mark (gw #{:bob :carol} {:e :g7c :k :note :v "rt7 group marked" :mark #{:die-with-any}})
              fb (c/forget-person! st :bob)]
          (is (= :yes (:answer fb)))
          (inject/arm! :purge-page :bob)
          (is (thrown? Exception (rx/purge-person! st :bob)) "the operator's loop dies after a page")
          (inject/reset-all!)
          (is (pos? (rx/purge-person! st :bob)) "and is run again")
          (let [fs (all-fields st [:bob :alice] [:group])]
            (doseq [fid [[(:name own) 0] g-bob g-mark]
                    :let [es (entries-for (merge (:stream fs) (:micro fs)) fid)]]
              (is (seq es))
              (is (every? #(= (:stamp fb) (:erased-at %)) es) (str "dies with Bob, dated by his forget: " fid))
              (is (not-any? #(= fid (:fid %)) (mapcat vals [(get (:stream fs) [:bob :ix-kv]) (get (:micro fs) [:group :ix-kv])])))
              (is (not (contains? (merge (get (:stream fs) [:bob :ix-of]) (get (:micro fs) [:group :ix-of])) fid))))
            (is (not (str/includes? (pr-str fs) "rt7 bob own")))
            (doseq [[fid L text] [[[(:name am) 0] :alice "rt7 alice about bob"] [g-two :group "rt7 group bob and carol"]]]
              (is (= [fid] (fids-of (rd st alice {:layer L :read [:pattern [:kv :note text]]}))) (str "7b's survivor still matches: " text))))))

      (testing "RT13, F9: an index gap in block 2d marks every read of its layer partial until a rebuild"
        (let [nm (env/make-name :group :by-entity)]
          (swap! sr/fail-index conj nm)
          (let [{:keys [answer]} (mc/write! st {:name nm :who :alice :layer :group :permission (gp :alice) :session :alice-g
                                                :facts [{:e :gap :k :note :v "gap"}]})]
            (is (= :yes (:answer answer)) "the act is admitted: its decision is block 2a's")
            (settle! st (batch-of st nm))
            (reset! sr/fail-index #{})
            (let [r (rd st alice {:layer :group :read [:pattern [:e :gap]]})]
              (is (= :partial (:mark r)) "the record never claims a completeness the index cannot back")
              (is (= [] (:rows r))))
            (rx/rebuild-micro! st :entries 64)
            (let [r (rd st alice {:layer :group :read [:pattern [:e :gap]]})]
              (is (= :complete (:mark r)))
              (is (= [[nm 0]] (fids-of r)) "the rebuild restored its entries")))))

      (testing "RT8: a restore, then rebuild, then forget replay reproduces every index of both stores exactly"
        (let [stream-ls [:alice :alice-hand :alice-agent :bob :people :base]
              micro-ls [:group :base]
              per-task (mapv #(retrying (fn [] (foreign-invoke-query (:task-layers st) %))) (range 4))
              home (into {} (for [t (range 4) L (:micro (nth per-task t))] [L t]))
              snap (all-fields st stream-ls micro-ls)
              ;; a restore adds its own acts to :people (the restore fact, and the operator's
              ;; lease it was sealed under): every entry stamped after the snapshot's last
              cut (reduce max 0 (for [[[L _] m] (:stream snap) :when (= :people L) [_ e] m :when (map? e)] (:stamp e)))
              strip (fn [fs _names]
                      (update fs :stream
                              (fn [m] (into {} (for [[[L _ :as k] v] m]
                                                 [k (if (= :people L)
                                                      (into {} (remove (fn [[_ e]] (and (map? e) (< cut (:stamp e)))) v))
                                                      v)])))))
              drop-all! (fn []
                          (doseq [L stream-ls f stream-fields]
                            (loop [] (when (pos? (:dropped (rx/index-op! st {:layer L :op :drop :field f :entries 512}) 0)) (recur))))
                          (doseq [L micro-ls f sr/sweep-fields]
                            (loop [] (when (pos? (:count (rx/micro-op! st {:op :drop :task (home L) :layer L :field f :entries 512}) 0))
                                       (recur)))))]
          (is (= #{:group :base} (set (keys home))))
          (drop-all!)
          (is (every? empty? (vals (:stream (all-fields st stream-ls micro-ls)))) "every index emptied")
          (is (every? empty? (vals (:micro (all-fields st stream-ls micro-ls)))))
          (let [r1 (rx/restore! st :acts 8 :entries 16)
                after1 (all-fields st stream-ls micro-ls)]
            (is (= :yes (:answer (:fact r1))) "the restore is a fact (FRR7)")
            (say "restore replayed" (:replayed r1) "forget facts; micro pages" (:micro-rebuilt r1))
            (is (pos? (:replayed r1)))
            (is (= snap (strip after1 #{(:name (:fact r1))})) "every field equals the snapshot, entry for entry")
            (let [r2 (rx/restore! st)]
              (is (= snap (strip (all-fields st stream-ls micro-ls) #{(:name (:fact r1)) (:name (:fact r2))}))
                  "and again after a second restore")))))

      (testing "RT13: malformed records and arguments are answered as data, with no worker restart"
        (doseq [raw [{:op :junk} {:op :rebuild-put :task 99 :req "x" :entities 1} "not a map" {:op :drop :task 0 :req 5}]]
          (foreign-append! (:micro-index-ops st) raw :append-ack))
        (is (= {:refused :bad-op} (rx/index-op! st {:layer :alice :op :person-purge :task -1 :person :bob :n 1})))
        (is (= {:refused :bad-op} (rx/index-op! st {:layer :alice :op :replay-forget :fid :nope})))
        (is (= :malformed-control (:reason (rx/close-session! st :operator :alice-agent :s9 :reads :maybe))))
        (is (= {:refused :bad-scan} (retrying #(foreign-invoke-query (:read-delta st) :alice :alice [:all] 10 {:from 5 :kind :stamp} nil))))
        (is (= {:refused :bad-read} (retrying #(foreign-invoke-query (:entry-ids st) "x" nil nil nil 5))))
        (is (= {:refused :bad-read} (retrying #(foreign-invoke-query (:read-pattern st) :group "x" [:all] nil nil))))
        (is (= {:refused :bad-pattern} (rd st alice {:layer :group :read [:pattern [:kv :note]]})))
        (is (:done? (rx/micro-op! st {:op :drop :task 0 :layer :nothing :field :ix-ek :entries 1}))
            "the micro topology still answers")
        (is (:rows (rd st alice {:layer :group :read [:pattern [:all]]})) "and the exit still reads")))))
