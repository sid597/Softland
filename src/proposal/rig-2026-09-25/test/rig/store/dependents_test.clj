(ns rig.store.dependents-test
  "The lookup from a fact to what stood on it (PLAN-dependents.md section 9),
  on an in-process cluster of 4 tasks: one entry per fact an act stood on
  (act grain); `[:dependents x]` through the one exit shows the facts of
  every act that stood on x (a fact) or on any fact of x (an act), once an
  act, with `:on`, the facts of x it named; the moment bounds it; visibility is the
  dependent's layer's; a resend adds nothing; a refusal writes nothing; the
  field is rebuilt from the log; a forgotten dependent shows erased; the
  read entry records the lookup; standing reads and shared layers refuse
  it. The model's world (`client/seed!`) and a layer of Bob's."
  (:require [clojure.test :refer [deftest is testing]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [rig.store.client :as c]
            [rig.store.dependents :as dependents]
            [rig.store.module :as m]
            [rig.store.read-exit :as rx]
            [rig.store.reads :as reads]
            [rig.store.shared-reads :as shared-reads]
            [rig.store.standing :as standing]))

(defn- say [& xs] (apply println "OBSERVED" xs))

(defn- act [who layer facts & {:as more}]
  (c/build (merge {:who who :layer layer :class :by-layer :permission [who layer layer] :facts facts} more)))

(deftest the-lookup
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc m/Store {:tasks 4 :threads 2 :workers 1})
    (let [st (rx/connect ipc)
          ok! (fn [o] (let [a (c/offer-until-answered! st o)] (is (= :yes (:answer a)) (pr-str a)) a))
          rd (fn [who layer pattern & {:keys [as-of limit]}]
               (rx/read! st (cond-> {:reader who :reader-kind :person :working layer :permission [who layer layer]
                                     :layer layer :read [:pattern pattern]}
                              as-of (assoc :as-of as-of)
                              limit (assoc :limit limit))))
          entries (fn [layer] (into {} (foreign-select [(keypath layer :ix-dep) ALL] (:layers st))))]
      (is (every? #(= :yes (:answer %)) (c/seed! st)))
      (ok! (c/make-layer-offer :bob {:kind :personal :owner :bob}))
      (ok! (c/grant-offer st [:bob :bob :bob]))

      (let [fo (act :alice :alice [{:e :e1 :k :note :v "the fact"} {:e :e1b :k :note :v "its neighbour"}])
            fa (ok! fo)
            F [(:name fo) 0]
            F2 [(:name fo) 1]
            sF (:stamp fa)
            d (act :alice :alice [{:e :e2 :k :note :v "d one"} {:e :e3 :k :note :v "d two"}] :stood-on {F sF})
            da (ok! d)]
        (testing "1. an act standing on F: its facts, each with :on #{F}; one entry an act; nothing for an act on nothing"
          (ok! (act :alice :alice [{:e :e4 :k :note :v "stands on nothing"}]))
          (let [r (rd :alice :alice [:dependents F])]
            (say "1" (pr-str (mapv #(select-keys % [:fid :value :on]) (:rows r))))
            (is (= [["d one" #{F}] ["d two" #{F}]] (mapv (juxt :value :on) (:rows r))))
            (is (= [[(:name d) 0] [(:name d) 1]] (mapv :fid (:rows r))))
            (is (= :complete (:mark r))))
          (is (= [{:on F :on-stamp sF :act (:name d) :stamp (:stamp da) :n 2}] (vals (entries :alice)))
              "act grain: one entry for the act, ids only")
          (let [r (rd :alice :alice [:dependents F] :limit 1)]
            (is (= [1 :partial] [(count (:rows r)) (:mark r)]) "the limit counts rows: one of d's two, and partial"))
          (is (= [] (:rows (rd :alice :alice [:dependents F2]))) "nothing stood on its neighbour"))

        (testing "2. the moment: as of a stamp before the dependent, nothing"
          (is (= [] (:rows (rd :alice :alice [:dependents F] :as-of (dec (:stamp da))))))
          (is (= 2 (count (:rows (rd :alice :alice [:dependents F] :as-of (:stamp da)))))))

        (testing "3. the act-level lookup: what stood on any fact of the act, each row saying which"
          (let [h (act :alice :alice [{:e :e5 :k :note :v "on the neighbour"}] :stood-on {F2 sF})
                both (act :alice :alice [{:e :e6 :k :note :v "on both"} {:e :e7 :k :note :v "also on both"}
                                         {:e :e8 :k :note :v "and a third"}]
                          :stood-on {F sF F2 sF})]
            (ok! h)
            (let [before (count (entries :alice))]
              (ok! both)
              (is (= 2 (- (count (entries :alice)) before)) "three facts on two facts: two entries, not six"))
            (let [r (rd :alice :alice [:dependents (:name fo)])]
              (is (= #{["d one" #{F}] ["d two" #{F}] ["on the neighbour" #{F2}]
                       ["on both" #{F F2}] ["also on both" #{F F2}] ["and a third" #{F F2}]}
                     (set (map (juxt :value :on) (:rows r)))))
              (is (= 6 (count (:rows r))) "an act standing on two of them shows once"))))

        (testing "4. visibility is the dependent's layer's: Bob's act on Alice's fact stays Bob's"
          (let [b (act :bob :bob [{:e :b1 :k :note :v "bob's, private"}] :stood-on {F sF})]
            (ok! b)
            (is (not-any? #{"bob's, private"} (map :value (:rows (rd :alice :alice [:dependents F]))))
                "Alice's lookup in her layer does not show Bob's")
            (is (= {:refused :not-visible} (rd :alice :bob [:dependents F])) "Alice may not read Bob's layer")
            (is (= ["bob's, private"] (mapv :value (:rows (rd :bob :bob [:dependents F]))))
                "Bob finds his own")))

        (testing "5. a resend adds nothing; a refusal writes nothing"
          (let [before (entries :alice)]
            (is (= :yes (:answer (c/offer-until-answered! st d))) "answered from the record")
            (is (= :stale-replaces
                   (:reason (c/offer-until-answered! st (act :alice :alice [{:e :e9 :k :note :v "x" :replaces [(:name d) 7]}]
                                                             :stood-on {F sF})))))
            (is (= before (entries :alice)))))

        (testing "6. rebuilt from the log: drop the field, put pages, the same entries"
          (let [before (entries :alice)
                acts (for [[nm rec] (foreign-select [(keypath :alice :answers) ALL] (:layers st))
                           :when (= :yes (:answer rec))]
                       {:name nm :stamp (:stamp rec)
                        :rows (vec (foreign-select [(keypath :alice :log nm) ALL] (:layers st)))
                        :stood-on (into {} (foreign-select [(keypath :alice :stood-on nm) ALL] (:layers st)))})]
            (is (= before (dependents/implied acts)) "what the gate wrote is what the log implies")
            (is (= {:dropped (count before)}
                   (rx/index-op! st {:layer :alice :op :drop :field :ix-dep :entries 512})))
            (is (empty? (entries :alice)))
            (is (= {:refused :bad-op} (rx/index-op! st {:layer :alice :op :rebuild-sweep :field :ix-dep :entries 8}))
                "no sweep of this field (P-D5)")
            (rx/rebuild! st :alice :acts 3)
            (is (= before (entries :alice)))
            (is (= 5 (count (:rows (rd :alice :alice [:dependents F])))) "d's two and both's three")))

        (testing "7. a forgotten dependent shows erased, its ids still found; the forget stood on it too"
          (let [dfid [(:name d) 0]]
            (is (= :yes (:answer (c/forget-value! st :alice :alice dfid))))
            (let [rows (:rows (rd :alice :alice [:dependents F]))
                  row (first (filter #(= dfid (:fid %)) rows))]
              (is (contains? row :erased-at))
              (is (not (contains? row :value))))
            (is (= [:forget] (mapv :k (:rows (rd :alice :alice [:dependents dfid]))))
                "the forget act stood on what it forgot")))

        (testing "8. refused as data: a malformed x, a standing read, a shared layer"
          (is (= {:refused :bad-pattern} (rd :alice :alice [:dependents :not-an-id])))
          (is (= {:refused :bad-pattern} (rd :alice :alice [:dependents F :extra])))
          (is (= {:refused :not-standing}
                 (standing/subscribe! st {:reader :alice :reader-kind :person :working :alice :permission [:alice :alice :alice]
                                          :layer :alice :pattern [:dependents F]})))
          (is (= {:refused :bad-pattern} (shared-reads/micro-pp (reads/parse-pattern [:dependents F] nil nil)))
              "the micro store has no lookup (P-D3)")
          (let [q (foreign-query ipc (get-module-name m/Store) "read-dependents")
                pp (reads/parse-pattern [:dependents F] nil nil)
                m0 (:stamp da)]
            (is (= {:refused :bad-read} (foreign-invoke-query q :alice :alice {:kind :nope} m0))
                "the query, called by name, checks what it is given")
            (is (= {:refused :bad-read} (foreign-invoke-query q :alice :alice pp nil)))
            (is (= {:refused :not-visible} (foreign-invoke-query q :bob :alice pp m0))
                "and who may see the layer")))

        (testing "9. the read entry records the lookup and what it matched"
          (let [r (rd :alice :alice [:dependents F])
                entry (first (c/facts st :alice (:entry r)))]
            (is (= [:read/pattern [:dependents F] (count (:rows r))]
                   [(:k entry) (get-in entry [:v :pattern]) (get-in entry [:v :count])]))))))))
