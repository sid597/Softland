(ns app.client.engine.executor-pending-test
  (:require [app.client.engine.executor :as executor]
            [app.client.engine.value-bytes :as vb]
            [clojure.test :refer [deftest is testing]]))

(def read-table
  {:vocabulary "pending test 1"
   :read {:args [:point :layer] :needs [[:point] [:layer]]
          :snapshot (fn [args _] args)
          :run (fn [{:keys [point layer]} {:keys [budget]}]
                 (cond
                   (= :unordered layer) {:status :needs-policy :candidates [:a :b]}
                   (= :unsupported layer) {:status :unsupported :reason :filter}
                   (= 0 budget) {:status :pending :known [:a] :missing [:b] :partial [0.5 0 0 0.5]}
                   :else {:status :resolved :color [point 0 0 1] :contributors [:a :b]}))}
   :identity {:args [:value] :needs [[:value]] :run (fn [args _] (:value args))}})

(def brush
  {:roots {:events [{:x 1} {:x 2}] :layer :ordered}
   :program {:each {:items [:get :events] :item :event :fields [:x]
                    :state {:color [0 0 0 0]}
                    :steps [{:out :picked :op :read :args {:point [:get :event :x] :layer [:get :layer]}}
                            {:out :painted :op :identity :args {:value [:get :picked :color]}}]
                    :next {:color [:get :painted]}}
             :return {:color [:get :state :color]}}})

(deftest a-loop-subject-includes-the-read-that-initialized-state
  (let [record (-> brush
                   (assoc-in [:program :steps]
                             [{:out :initial :op :read :args {:point 3 :layer [:get :layer]}}
                              {:out :unused :op :read :args {:point 9 :layer [:get :layer]}}])
                   (assoc-in [:program :each :state] {:color [:get :initial :color]})
                   (assoc-in [:program :each :steps] [])
                   (assoc-in [:program :each :next] {:color [:get :state :color]}))
        eager (executor/run record {} read-table {:budget 1})
        c (:continuation (executor/run record {} read-table {:budget 1 :until 1}))
        resumed (executor/resume (executor/decode (executor/encode c) read-table) read-table {:budget 1})]
    (is (= :complete (:status eager)))
    (is (= [[3 0 0 1]] (mapv :color (get-in eager [:subjects :color :reads]))))
    (is (vb/equal? (:subjects eager) (:subjects resumed)))))

(deftest pending-is-a-transaction-barrier-and-replay-is-a-value
  (let [calls (atom 0)
        table (assoc-in read-table [:identity :run] (fn [args _] (swap! calls inc) (:value args)))
        held (executor/run brush {} table {:budget 0})
        c (:continuation held)]
    (is (= [:suspended :pending 0] ((juxt :status :reason :at) held)))
    (is (= {:color [0 0 0 0]} (:state c)))
    (is (= [] (:history c)))
    (is (= [:b] (:missing held)))
    (is (zero? @calls))
    (is (vb/equal? c (:continuation (executor/run brush {} table {:budget 0}))))
    (let [eager (executor/run brush {} table {:budget 1})
          delayed (executor/resume c table {:budget 1})
          bytes (executor/encode c)
          cold (executor/resume (executor/decode bytes table) table {:budget 1})]
      (doseq [r [eager delayed cold]] (is (= :complete (:status r)) (pr-str r)))
      (doseq [k [:state :history :results :subjects]]
        (is (vb/equal? (k eager) (k delayed))))
      (is (vb/equal? delayed cold)))))

(deftest answers-route-past-an-earlier-read-and-compare-every-input
  (let [record (update-in brush [:program :each :steps]
                          #(into [{:out :preview :op :read :args {:point 0 :layer [:get :layer]}}] %))
        calls (atom [])
        table (assoc-in read-table [:read :run]
                        (fn [args ctx] (swap! calls conj (:step ctx))
                          ((get-in read-table [:read :run]) args ctx)))
        demand (executor/run record {} table {:stop-at :picked :budget 1})
        answer (select-keys demand [:request :value])]
    (is (= :answered (:status demand)))
    (is (= [] (:history demand)))
    (reset! calls [])
    (let [result (executor/run record {} table {:answer answer :budget 1})]
      (is (= :complete (:status result)) (pr-str result))
      (is (= [:preview :preview :picked] @calls) "the supplied picked read alone is skipped"))
    (doseq [[path value reason] [[[:recipe :roots :layer] :changed :recipe]
                                 [[:consumed] [{:x -1}] :consumed]
                                 [[:item :x] 9 :item]
                                 [[:state :color] [9 0 0 1] :state]
                                 [[:snapshot :point] 9 :snapshot]]]
      (let [r (executor/run record {} table {:answer (update answer :request assoc-in path value) :budget 1})]
        (is (= [:stale reason] ((juxt :status :reason) r)))
        (is (= [] (:history r)))))
    (let [done (:continuation (executor/run (update-in record [:roots :events] conj {:x 3}) {} table {:until 2 :budget 1}))
          r (executor/resume done table {:record record :answer answer :budget 1})]
      (is (= :answer-not-consumed (:reason r)))
      (is (= 2 (count (:history r)))))
    (is (= :item (:reason (executor/run (assoc-in record [:roots :events 0 :x] 3) {} table {:answer answer :budget 1}))))))

(deftest before-loop-read-suspends-before-initializing-state
  (let [record (-> brush
                   (assoc-in [:program :steps] [{:out :initial :op :read :args {:point 3 :layer [:get :layer]}}])
                   (assoc-in [:program :each :state :color] [:get :initial :color]))
        held (executor/run record {} read-table {:budget 0})
        c (:continuation held)
        eager (executor/run record {} read-table {:budget 1})
        resumed (executor/resume (executor/decode (executor/encode c) read-table) read-table {:budget 1})]
    (is (= :before-loop (:phase c)))
    (is (nil? (:state c)))
    (is (vb/equal? (:history eager) (:history resumed)))
    (is (vb/equal? (:subjects eager) (:subjects resumed)))))

(deftest read-position-includes-the-phase
  (let [record (assoc-in brush [:program :steps]
                         [{:out :picked :op :read :args {:point 9 :layer [:get :layer]}}])
        demand (executor/run record {} read-table {:stop-at :picked :budget 1})
        answer (select-keys demand [:request :value])
        c (:continuation (executor/run record {} read-table {:until 0 :budget 1}))
        calls (atom [])
        table (assoc-in read-table [:read :run] (fn [args ctx] (swap! calls conj (:point args))
                                                ((get-in read-table [:read :run]) args ctx)))
        r (executor/resume c table {:answer answer :budget 1})]
    (is (= :before-loop (get-in answer [:request :phase])))
    (is (= :complete (:status r)) (pr-str r))
    (is (= [1 2] @calls) "only the pre-loop read consumed that answer")))

(deftest replayed-pre-loop-pending-keeps-the-committed-prefix
  (let [record (assoc-in brush [:program :steps]
                         [{:out :initial :op :read :args {:point 9 :layer [:get :layer]}}])
        c (:continuation (executor/run record {} read-table {:until 1 :budget 1}))
        held (executor/resume c read-table {:budget 0})
        again (:continuation held)
        resumed (executor/resume (executor/decode (executor/encode again) read-table) read-table {:budget 1})
        eager (executor/run record {} read-table {:budget 1})]
    (is (= :before-loop (:phase again)))
    (is (:loop-entered? again))
    (is (= (:state c) (:state again)))
    (is (= (:history c) (:history again)))
    (is (= :complete (:status resumed)) (pr-str resumed))
    (is (vb/equal? (:history eager) (:history resumed)))))

(deftest policy-unsupported-and-provisional-are-distinct
  (let [r (executor/run (assoc-in brush [:roots :layer] :unordered) {} read-table {})]
    (is (= [:suspended :needs-policy] ((juxt :status :reason) r)))
    (is (= [:a :b] (:missing r)))
    (is (= [] (get-in r [:continuation :history]))))
  (let [r (executor/run (assoc-in brush [:roots :layer] :unsupported) {} read-table {})]
    (is (= :error (:status r)))
    (is (= :filter (get-in r [:data :reason]))))
  (let [record (assoc-in brush [:program :each :steps 0 :pending] :provisional)
        r (executor/run record {} read-table {:budget 0})]
    (is (= :complete (:status r)) (pr-str r))
    (is (= [0.5 0 0 0.5] (get-in r [:results :color])))
    (is (every? #(get-in % [:steps 0 :provisional]) (:history r)))
    (is (every? :provisional (get-in r [:subjects :color :reads]))))
  (let [record {:roots {} :program {:steps [{:out :picked :op :read :pending :provisional :args {:point 1 :layer :ordered}}]
                                    :return {:color [:get :picked :color]}}}
        a (executor/run record {} read-table {:budget 0})
        b (executor/run record {} read-table {:budget 1})]
    (is (= :complete (:status a)))
    (is (not= (:subjects a) (:subjects b)) "a non-loop provisional output still has its read in the subject")))

(deftest retained-inputs-check-the-producer-and-output-before-work
  (let [producer {:roots {:radius 150} :program {:steps [{:out :built :op :identity :args {:value {:radius [:get :radius]}}}]
                                               :return {:region [:get :built]}}}
        made (executor/run producer {} read-table)
        value (assoc (get-in made [:results :region]) :subject (get-in made [:subjects :region]))
        consumer {:roots {:inputs {:clip {:from {:record "reach" :output :region}}}}
                  :program {:steps [{:out :used :op :identity :args {:value [:get :inputs :clip]}}]
                            :return {:clip [:get :used]}}}
        run #(executor/run consumer {:inputs {:clip %}} read-table {:records {"reach" %2}})]
    (is (= :complete (:status (run value producer))))
    (is (= :subject (:reason (run (dissoc value :subject) producer))))
    (is (= :subject (:reason (run value (assoc-in producer [:roots :radius] 140)))))
    (is (= :subject (:reason (run (assoc-in value [:subject :recipe :roots] {}) producer))))))
