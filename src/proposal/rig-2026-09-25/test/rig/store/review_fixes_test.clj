(ns rig.store.review-fixes-test
  "Step R (PLAN-review-fixes.md), the fixes from the review of wave 1, pure:
  no cluster. The review's own namespace, `rig.store.review-wave1-test`,
  checks the same fixes end to end on one cluster.

  - R-1: a fact under a setting key about another entity than its act's
    layer is refused `:malformed-control` by both gates' shared `refusal`
    (the stream gate's `decide`, the micro fold's `micro-decision`), and no
    door sends one: the door throws before anything is sealed, leased or
    appended.
  - R-2: a read entry's `[:kv]` line records the pattern as the module
    keyed it (`:recorded-pattern`), never the value's text; a pattern answer
    without one gives no entry.
  - F-1's last edge: the micro door never seals under no lock. `seal`
    throws `:no-lock` when it has no lock for a cited id, so a lease
    answered yes whose locks the door could not take (all or some) sends
    nothing."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [rig.store.client :as c]
            [rig.store.envelope :as env]
            [rig.store.gate :as gate]
            [rig.store.locks :as locks]
            [rig.store.micro :as micro]
            [rig.store.micro-client :as mc]
            [rig.store.reads :as reads])
  (:import [clojure.lang ExceptionInfo]
           [java.util UUID]))

;; ------------------------------------------------------------------ helpers

(def ^:private setting-keys [:kind :owner :class :lock-grain])

(def ^:private alice-settings {:kind :personal :owner :alice :class :by-layer :grain :per-value})
(def ^:private group-settings {:kind :group :owner nil :class :by-entity :grain :per-value})
(def ^:private granted {:granted [[:alice :by-layer :offer (UUID. 1 1)] 0]})

(defn- raw
  "A raw offer as the door builds it: `who` into `layer`, citing [who layer
  layer] unless the operator, under a fresh name."
  [who layer facts & {:as more}]
  (c/build (merge {:who who :layer layer :class :by-layer
                   :permission (when-not (= :operator who) [who layer layer])
                   :facts facts}
                  more)))

(defn- parsed
  "The offer as `gate` parses it (control facts only: nothing to seal)."
  [gate o]
  (let [p (env/parse o gate)]
    (or (:ok p) (throw (ex-info "the parse refused the test's offer" {:offer o :parse p})))))

(defn- thrown-data
  "The ex-data `f` throws, or :no-throw."
  [f]
  (try (f) :no-throw (catch ExceptionInfo e (ex-data e))))

;; ------------------------------------------------------------------- R-1

(deftest misplaced-settings
  (testing "a setting key about another entity than the act's layer, and only that"
    (doseq [k setting-keys]
      (is (gate/misplaced-setting? {:layer :alice} {:e :rv1 :k k :v "text"}) (str k " about another entity"))
      (is (not (gate/misplaced-setting? {:layer :alice} {:e :alice :k k :v :x})) (str k " about the layer itself")))
    (doseq [k [:note :members :permission :forget :lease :promote-request :crossed :grammar]]
      (is (not (gate/misplaced-setting? {:layer :alice} {:e :rv1 :k k :v "text"})) (str k " is not a setting key")))
    (is (not (gate/misplaced-setting? {:layer :alice} "not a fact")) "total")))

(deftest the-stream-gate-refuses-a-misplaced-setting
  (doseq [k setting-keys]
    (testing (str k " about another entity, by the layer's owner and by the operator")
      (doseq [who [:alice :operator]]
        (let [v (str "rv1 " (name k) " text")
              o (parsed :stream (raw who :alice [{:e :rv1 :k k :v v}]))
              rows {[:alice :alice :alice] granted}]
          (is (= :malformed-control (gate/refusal o alice-settings rows {})) (str who))
          (let [d (gate/decide o alice-settings rows {} 0 1000 "digest")]
            (is (= :decide (:kind d)))
            (is (= [:no :malformed-control] ((juxt :answer :reason) (:record d))) "a recorded no")
            (is (nil? (:log d)) "no row")
            (is (empty? (:index-put d)) "no index entry")
            (is (nil? (:settings d)) "no setting")
            (is (not (str/includes? (pr-str (:record d)) v)) "the record keeps nothing of the value"))))))
  (testing "beside a well-placed setting fact, the act is still refused whole"
    (let [o (parsed :stream (raw :alice :alice [{:e :alice :k :lock-grain :v :per-act} {:e :rv1 :k :owner :v "rv1 mixed"}]))]
      (is (= :malformed-control (gate/refusal o alice-settings {[:alice :alice :alice] granted} {})))))
  (testing "a setting fact about the layer itself is decided as before"
    (let [rows {[:alice :alice :alice] granted}]
      (is (nil? (gate/refusal (parsed :stream (raw :alice :alice [{:e :alice :k :lock-grain :v :per-act}])) alice-settings rows {}))
          "the owner's grain switch")
      (is (= :control-not-allowed (gate/refusal (parsed :stream (raw :alice :alice [{:e :alice :k :owner :v :bob}])) alice-settings rows {})))
      (is (= :layer-already-made (gate/refusal (parsed :stream (raw :operator :alice [{:e :alice :k :owner :v :bob}])) alice-settings {} {})))))
  (testing "W1-7 is unchanged: :members at the stream gate is :control-not-allowed"
    (let [o (parsed :stream (raw :alice :alice [{:e :rv1 :k :members :v #{:alice}}]))]
      (is (nil? (gate/refusal o alice-settings {[:alice :alice :alice] granted} {})))
      (is (= :control-not-allowed (gate/stream-refusal o))))))

(deftest the-micro-gate-refuses-a-misplaced-setting
  (doseq [k setting-keys]
    (let [o (parsed :micro (raw :operator :group [{:e :rv1m :k k :v (str "rv1m " (name k) " text")}] :class :by-entity))
          d (micro/micro-decision o group-settings {} {} 0 1000)]
      (is (= :malformed-control (:reason d)) (str k ": the micro fold's stage-1 reason"))
      (is (= :malformed-control (micro/first-in-order (concat [(:reason d)] (micro/micro-extras o group-settings))))
          (str k ": first in the one order"))
      (is (nil? (:settings d)) (str k ": no settings version"))))
  (testing "a setting fact about the group itself is decided as before"
    (let [o (parsed :micro (raw :operator :group [{:e :group :k :lock-grain :v :per-act}] :class :by-entity))]
      (is (nil? (:reason (micro/micro-decision o group-settings {} {} 0 1000)))))))

(deftest no-door-sends-a-misplaced-setting
  (let [o (raw :alice :alice [{:e :rv1 :k :owner :v "rv1 owner text"}])
        mo (mc/build {:who :bob :layer :group :session :rvs :permission [:rvs :group :group]
                      :facts [{:e :rv1m :k :owner :v "rv1m owner text"}]})
        refused? (fn [d] (= {:door :refused :reason :malformed-control} (select-keys d [:door :reason])))]
    (testing "each door throws before it touches the store (the store here is nil; one try and a short timeout, so a missing check fails fast)"
      (is (refused? (thrown-data #(c/offer! nil o))) "the stream door's offer!")
      (is (refused? (thrown-data #(c/offer-until-answered! nil o 1))) "and its offer-until-answered!, before its retry loop")
      (is (refused? (thrown-data #(mc/offer! nil mo 1))) "the micro door's offer!")
      (is (refused? (thrown-data #(mc/write! nil {:who :bob :layer :group :session :rvs :permission [:rvs :group :group]
                                                  :facts [{:e :rv1m :k :owner :v "rv1m owner text"}]})))
          "and its write!, before it leases"))
    (testing "what the refusal names"
      (let [d (thrown-data #(c/offer! nil o))]
        (is (= (:name o) (:name d)))
        (is (= [[:rv1 :owner]] (:misplaced d)))))
    (testing "beside a value fact, the door still refuses, naming only the misplaced fact"
      (let [d (thrown-data #(c/offer! nil (raw :alice :alice [{:e :e0 :k :note :v "a value"} {:e :rv1 :k :kind :v "rv1 mixed"}])))]
        (is (refused? d))
        (is (= [[:rv1 :kind]] (:misplaced d)))))
    (testing "a well-placed act passes the check"
      (is (nil? (c/refuse-misplaced! (raw :alice :alice [{:e :alice :k :lock-grain :v :per-act}]))))
      (is (nil? (c/refuse-misplaced! (raw :alice :alice [{:e :e0 :k :note :v "a value"}])))))))

;; ------------------------------------------------------------------- R-2

(deftest a-kv-line-keeps-its-value-keyed
  (let [text "r2 a value that will be forgotten"
        n1 [:alice :by-layer :offer (UUID. 2 2)]
        enm [:alice-hand :by-layer :offer (UUID. 3 3)]
        spec {:entry-name enm :role :shown :reader-kind :person}
        rows [{:fid [n1 0] :stamp 10 :k :note :value text}]
        answer (reads/pattern-answer :alice 99 {:pattern [:kv :note text]} rows false)
        [line :as facts] (reads/entry-facts answer spec)]
    (testing "the module's answer carries the keyed pattern"
      (is (= (reads/recorded-pattern [:kv :note text]) (:recorded-pattern answer)))
      (is (string? (get-in answer [:recorded-pattern 2 :keyed]))))
    (testing "the line records it, and nothing of the value"
      (is (= 1 (count facts)))
      (is (= :read/pattern (:k line)))
      (is (= (:recorded-pattern answer) (get-in line [:v :pattern])))
      (is (= [[[n1 0] 10]] (get-in line [:v :exact])) "the exact list: ids and stamps")
      (is (not (str/includes? (pr-str facts) text)) "no text anywhere in the entry"))
    (testing "other forms are recorded as they are"
      (let [a (reads/pattern-answer :alice 99 {:pattern [:e :e0]} rows false)]
        (is (= [:e :e0] (get-in (first (reads/entry-facts a spec)) [:v :pattern])))))
    (testing "a pattern answer without a recorded pattern gives no entry, so no line can hold a value's text"
      (is (nil? (reads/entry-facts (dissoc answer :recorded-pattern) spec))))))

;; ---------------------------------------------------------- F-1's last edge

(deftest the-micro-door-never-seals-under-no-lock
  (let [ln [:group :by-entity :offer (UUID. 4 4)]
        spec {:who :bob :layer :group :session :rvs :permission [:rvs :group :group]
              :facts [{:e :f1a :k :note :v {:token "one"}} {:e :f1b :k :note :v {:token "two"}}]}
        run (fn [lease-answer taken]
              (let [offered (atom [])
                    r (with-redefs [mc/lease! (fn [_ {:keys [n]}]
                                                {:name ln :offer nil :answer {:answer lease-answer} :ids (locks/lease-ids ln n)})
                                    mc/take-locks (fn [& _] taken)
                                    mc/offer! (fn [_ o] (swap! offered conj o) {:answer :yes})]
                        (try (mc/write! nil spec :grain :per-value)
                             (catch ExceptionInfo e (ex-data e))))]
                {:result r :offered @offered}))
        [id0 id1] (locks/lease-ids ln 2)]
    (testing "a lease answered yes, no lock taken (take-locks' timeout gives {}): seal throws and nothing is sent"
      (let [{:keys [result offered]} (run :yes {})]
        (is (= :no-lock (:door result)))
        (is (= id0 (:lock-id result)) "the first value's id")
        (is (= ln (locks/lease-name-of (:lock-id result))) "the lock id names the lease")
        (is (= [:group :by-entity :offer] (subvec (:name result) 0 3)) "the act's own name")
        (is (empty? offered))))
    (testing "a lease answered yes, one lock of two taken: the same, at the value with no lock"
      (let [{:keys [result offered]} (run :yes {id0 (locks/fresh-lock)})]
        (is (= [:no-lock id1] ((juxt :door :lock-id) result)))
        (is (empty? offered))))
    (testing "a lease answered yes, every lock taken: sealed and sent"
      (let [{:keys [result offered]} (run :yes {id0 (locks/fresh-lock) id1 (locks/fresh-lock)})]
        (is (= {:answer :yes} (:answer result)))
        (is (= 1 (count offered)))
        (is (every? #(bytes? (:sealed %)) (:facts (first offered))))))
    (testing "a refused lease keeps F-1's fix: sealed under throwaway locks citing the ids, and sent"
      (let [{:keys [offered]} (run :no {})]
        (is (= 1 (count offered)))
        (is (every? #(bytes? (:sealed %)) (:facts (first offered))))
        (is (= [id0 id1] (mapv :lock-id (:facts (first offered)))))))
    (testing "seal itself: no lock for a cited id, or not a lock, throws; a lock seals"
      (let [o (mc/build spec)]
        (is (= [:no-lock (:name o) id0] ((juxt :door :name :lock-id) (thrown-data #(mc/seal o (constantly id0) {})))))
        (is (= :no-lock (:door (thrown-data #(mc/seal o (constantly id0) {id0 (byte-array 3)})))) "not a lock")
        (is (every? #(bytes? (:sealed %)) (:facts (mc/seal o (constantly id0) {id0 (locks/fresh-lock)}))))))))
