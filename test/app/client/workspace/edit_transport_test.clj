(ns app.client.workspace.edit-transport-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.workspace.edit-transport :as transport]))

(deftest ordered-queue-and-correlation
  (let [q0 (transport/seed "" 0)
        q1 (transport/enqueue q0 {:seq 0 :request-id "r0" :text "a" :caret 1})
        q2 (transport/enqueue q1 {:seq 1 :request-id "r1" :text "ab" :caret 2})]
    (is (= {:text "ab" :caret 2 :seq 1 :request-id "r1"}
           (transport/projection q2)))
    (testing "a later acceptance heals skipped earlier decisions"
      (let [r (transport/decide q2 "r1" {:status :accepted})]
        (is (:matched? r))
        (is (:accepted? r))
        (is (= "ab" (get-in r [:queue :confirmed :text])))
        (is (transport/settled? (:queue r)))))
    (testing "unknown and prior-session decisions are strict no-ops"
      (let [r (transport/decide q2 "elsewhere:7" {:status :rejected})]
        (is (false? (:matched? r)))
        (is (= q2 (:queue r)))))
    (testing "a correlated rejection rebases the entire dependent flight"
      (let [r (transport/decide q2 "r0"
                                {:status :rejected :reason :edit/stale})]
        (is (= :edit/stale (:reason r)))
        (is (empty? (get-in r [:queue :inflight])))
        (is (= "" (get-in r [:queue :confirmed :text])))))))

(deftest caret-bound-and-truth-at-rest
  (let [q0 (transport/seed "abc" 99)
        q1 (transport/map-caret q0 -4)
        flying (transport/enqueue q1
                                  {:seq 0 :request-id "r0"
                                   :text "abcd" :caret 4})]
    (is (= 3 (get-in q0 [:confirmed :caret])))
    (is (= 0 (get-in q1 [:confirmed :caret])))
    (is (= flying (transport/adopt-truth flying "server"))
        "query truth never overwrites in-flight projection")
    (is (= "server" (get-in (transport/adopt-truth q1 "server")
                             [:confirmed :text])))))

(deftest queue-is-bounded
  (let [q (reduce (fn [queue n]
                    (transport/enqueue queue {:seq n :request-id (str "r" n)
                                              :text (str n) :caret 0}))
                  (transport/seed "" 0)
                  (range (+ transport/max-inflight 5)))]
    (is (= transport/max-inflight (count (:inflight q))))
    (is (= 5 (:seq (first (:inflight q)))))
    (is (= 68 (:seq (peek (:inflight q)))))))
