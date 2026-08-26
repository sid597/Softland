(ns app.client.substrate.frame-scheduler-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.string :as str]
            [app.client.substrate.frame-scheduler :as scheduler]))

(use-fixtures :each
  (fn [test-fn]
    (scheduler/clear-deadlines!)
    (scheduler/reset-clock-source!)
    (try (test-fn)
         (finally
           (scheduler/clear-deadlines!)
           (scheduler/reset-clock-source!)))))

(deftest clean-sleeps-and-causes-coalesce-to-one-encode
  (let [clean (scheduler/decide nil 0 #{} {})
        world (scheduler/decide (:state clean) 16 #{:world :interaction} {})]
    (is (false? (:encode? clean)))
    (is (true? (:encode? world)))
    (is (= #{:world :interaction} (:causes world)))
    (is (= 1 (get-in world [:state :encodes])))
    (is (= 1 (get-in world [:state :skips])))
    (is (= #{:viewport}
           (scheduler/derive-causes {:camera-moved? true}))))
  (is (thrown? clojure.lang.ExceptionInfo
               (scheduler/decide nil 0 #{:invented} {}))))

(deftest clocked-consumer-wakes-at-cadence-and-retires-on-stop
  (let [!stopped? (atom false)
        deadline {:next-deadline 0.0 :cadence (/ 1000.0 30.0)
                  :stop-predicate #(deref !stopped?)}
        first-step (scheduler/decide nil 0 #{} {:pulse deadline})
        early (scheduler/decide (:state first-step) 10 #{}
                                (:deadlines first-step))
        due (scheduler/decide (:state early) 34 #{} (:deadlines early))]
    (is (= #{:clock} (:causes first-step)))
    (is (false? (:encode? early)))
    (is (true? (:encode? due)))
    (reset! !stopped? true)
    (let [stopped (scheduler/decide (:state due) 68 #{} (:deadlines due))]
      (is (false? (:encode? stopped)))
      (is (= [:pulse] (:retired-deadlines stopped)))
      (is (empty? (:deadlines stopped))))))

(deftest replay-and-clock-rebinding-are-deterministic
  (let [rows [{:time 0 :causes [:world] :plan-hash :a}
              {:time 16 :causes [] :plan-hash :a}
              {:time 34 :causes [:clock] :plan-hash :a}]
        replay-a (scheduler/replay rows {})
        replay-b (scheduler/replay rows {})]
    (is (= replay-a replay-b))
    (is (= [true false true]
           (mapv :encode? (:decisions replay-a))))
    (scheduler/set-clock-source! (constantly 123.0))
    (is (= 123.0 (scheduler/clock-time 999.0)))))

(deftest pure-frame-namespaces-have-no-wall-clock-or-loop-source
  (doseq [path ["src/app/client/substrate/frame_graph.cljc"
                "src/app/client/substrate/frame_effects.cljc"
                "src/app/client/substrate/frame_scheduler.cljc"]
          token ["js/Date" "performance.now" "requestAnimationFrame"
                 "setInterval"]]
    (is (not (str/includes? (slurp path) token))
        (str path " contains forbidden token " token))))
