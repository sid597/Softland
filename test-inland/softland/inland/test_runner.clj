(ns softland.inland.test-runner
  "Focused admission, source ownership and finite-recipe tests.
   Takes a fresh two-task Rama IPC; asserts transitions, scoped proxy disposal and
   pure recipe results. Owns the IPC and test handles; no production cluster or real
   provider is used. The small reference interpreter tests seed semantics only;
   actual Electric scheduling and rendered interaction require the browser suite."
  (:require [clojure.test :refer :all]
            [com.rpl.rama :as r] [com.rpl.rama.path :as p] [com.rpl.rama.test :as rt]
            [missionary.core :as m]
            [softland.inland.module :as module]
            [softland.inland.store :as store]
            [softland.inland.total :as total]
            [softland.inland.resident :as resident]
            [softland.inland.logic :as logic]))

(defonce handles (atom nil))
(defn read-one
  "Test workspace, PState kind and key path → synchronous IPC value.
   Requires handles installed by -main."
  [workspace kind & path]
  (r/foreign-select-one [(apply p/keypath workspace path)] (kind @handles)))
(defn submit
  "Test operation fields and optional overrides → retained admission decision.
   Defaults to a fresh id, base layer and sid actor; appends to the real IPC depot."
  [workspace kind name & [extra]]
  (let [op (merge {:workspace workspace :request-id (str (random-uuid)) :kind kind
                   :name name :layer "base" :actor "sid"} extra)]
    (or (get (r/foreign-append! (:depot @handles) op :ack) "accept")
        (:decision (read-one workspace :decisions (:request-id op))))))
(defn seed!
  "Workspace → seed-v1 admission in the fresh IPC. Repeating it reuses its id."
  [workspace] (submit workspace :seed "world" {:request-id "seed-v1"}))
(defn wait-until
  "Predicate → true when satisfied within about two seconds, otherwise false.
   Sleeps 20 ms between observations; used only for asynchronous proxy receipts."
  [pred]
  (loop [n 100] (if (pred) true (if (zero? n) false (do (Thread/sleep 20) (recur (dec n)))))))

(deftest admitted-definition-is-the-only-live-basis
  (let [w "admission"]
    (is (= :accepted (:status (seed! w))))
    (is (some #{w} (store/registered-workspaces)))
    (is (= "Pointer" (:label (read-one w :rows "base/pointer"))))
    (is (>= (count (read-one w :index "base/demand/view")) 8))
    (let [before (read-one w :rows "base/targeting")
          bad (submit w :put "targeting" {:expected-revision 1 :row {:name "targeting" :body {:steps [{:out :bad :op :eval}] :return nil}}})]
      (is (= :rejected (:status bad)))
      (is (= before (read-one w :rows "base/targeting"))))
    (is (= :rejected (:status (submit w :put "bad-index" {:expected-revision 0 :row {:pattern {:event {}}}}))))
    (is (nil? (read-one w :rows "base/bad-index")))
    (let [row {:name "targeting" :body {:steps [{:out :parent :op :read :args {:name [:get :subject] :attr :parent}}]
                                            :return [:or [:get :parent] [:get :subject]]}}
          extra {:expected-revision 1 :row row :request-id "stable-id"}
          accepted (submit w :put "targeting" extra)]
      (is (= :accepted (:status accepted)))
      (is (= accepted (submit w :put "targeting" extra)))
      (is (= :rejected (:status (submit w :put "targeting" (assoc extra :row (assoc row :label "different envelope"))))))
      (is (= 2 (:revision (read-one w :rows "base/targeting"))))
      (is (= :rejected (:status (submit w :put "targeting" {:expected-revision 1 :row row})))))))

(deftest candidates-pins-promotion-and-removal
  (let [w "contexts" _ (seed! w)
        original (read-one w :rows "base/targeting")
        candidate (assoc original :label "Candidate")]
    (is (= :accepted (:status (submit w :put "targeting" {:layer "candidate" :expected-revision 0 :row candidate :basis {:base-revision 1}}))))
    (is (= original (read-one w :rows "base/targeting")))
    (is (= :accepted (:status (submit w :promote "targeting" {:expected-revision 1 :from-layer "candidate" :from-name "targeting" :source-revision 1}))))
    (is (= "Candidate" (:label (read-one w :rows "base/targeting"))))
    (is (= original (read-one w :versions (total/version-key "base" "targeting" 1))))
    (is (= :rejected (:status (submit w :promote "targeting" {:expected-revision 1 :from-layer "candidate" :from-name "targeting" :source-revision 1}))))
    (is (= :accepted (:status (submit w :delete-override "targeting" {:layer "candidate" :expected-revision 1}))))
    (is (nil? (read-one w :rows "candidate/targeting")))
    (is (some? (read-one w :versions (total/version-key "candidate" "targeting" 1))))
    (is (= :accepted (:status (submit w :remove "targeting" {:expected-revision 2}))))
    (is (true? (:removed (read-one w :rows "base/targeting"))))))

(deftest narrow-proxies-and-independent-disposal
  (let [w "proxies" _ (seed! w) !a (atom []) !b (atom []) !shape (atom [])
        subscribe (fn [owner path target]
                    ((m/reduce (fn [_ v] (swap! target conj v)) nil (store/watch-path owner :rows (into [w] path)))
                     (fn [_]) #(swap! target conj {:failure (str %)})))
        a (subscribe "A" ["base/targeting" :body] !a)
        b (subscribe "B" ["base/targeting" :body] !b)
        shape (subscribe "S" ["base/orb" :shape] !shape)]
    (try
      (is (wait-until #(and (seq @!a) (seq @!b) (seq @!shape))))
      (let [before (count @!shape)
            original (read-one w :rows "base/targeting")]
        (is (= :accepted (:status (submit w :put "targeting" {:expected-revision 1 :row (assoc original :body {:steps [] :return "changed"})}))))
        (is (wait-until #(= "changed" (get-in (last @!b) [:value :return]))))
        (is (= before (count @!shape)))
        (a)
        (let [count-a (count @!a)]
          (submit w :put "targeting" {:expected-revision 2 :row original})
          (is (wait-until #(= (:body original) (:value (last @!b)))))
          (is (= count-a (count @!a)))
          (is (= 1 (get-in @store/metrics ["A" [:closed [:rows [w "base/targeting" :body]]]])))))
      (finally (a) (b) (shape))))
  (let [seen (atom [])]
    (with-redefs [store/request-proxy (fn [& _] (java.util.concurrent.CompletableFuture/failedFuture (ex-info "Unavailable" {})))]
      (let [cancel ((m/reduce (fn [_ v] (swap! seen conj v)) nil (store/watch-path "failed-reader" :rows ["missing"])) (fn [_]) (fn [_]))]
        (try
          (is (wait-until #(seq @seen)))
          (is (= :failed (:status (first @seen))))
          (is (not (:complete? (first @seen))))
          (finally (cancel)))))))

(defn reference-call
  "Seed rows, definition name, bindings and read-log atom → pure recipe value.
   Supports value/call/read only and logs addressed reads; not the Electric runtime.
   Caller supplies finite known definitions; no independent recursion guard here."
  [rows name bindings reads]
  (let [body (:body (get rows name))]
    (loop [steps (:steps body) scope bindings]
      (if-let [step (first steps)]
        (let [enabled (if (contains? step :when) (total/evaluate (:when step) scope) true)
              args (total/evaluate (:args step) scope)
              value (when enabled
                      (case (:op step)
                        :value args
                        :call (reference-call rows (:name args) (:bindings args) reads)
                        :read (do (swap! reads conj [(:name args) (:attr args)]) (get-in rows [(:name args) (:attr args)]))))]
          (recur (rest steps) (assoc scope (:out step) value)))
        (total/evaluate (:return body) scope)))))

(deftest authored-step-and-maintained-supports
  (let [rows (into {} (map (juxt :name identity) (module/seed-rows))) reads (atom [])
        execute (fn [budget]
                  (loop [state {:frontier ["assembly"] :visited []} remaining budget]
                    (let [result (reference-call rows "walk-from-scene" {:state state} reads)
                          progress (total/step-result result state remaining)]
                      (if (= :running (:status progress)) (recur (:state progress) (:remaining progress)) progress))))]
    (is (= ["assembly" "orb" "ring"] (:value (execute 12))))
    (is (= :exhausted (:status (execute 1))))
    (is (= #{["assembly" :neighbors] ["orb" :neighbors] ["ring" :neighbors]} (set @reads)))
    (is (= #{:a :b} (get (total/conclude [{:support :a :value "same"} {:support :b :value "same"}]) "same")))
    (is (= #{:b} (get (total/conclude [{:support :b :value "same"}]) "same"))))
  (is (false? (total/ready? {:result {:runtime/status :pending}})))
  (is (= :pending (:runtime/status (total/collection-state 1 []))))
  (is (nil? (total/collection-state 1 [nil])))
  (is (= :failed (:runtime/status (total/first-failure [{:value {:runtime/status :failed}}]))))
  (is (some? (total/step-error :query {:demand nil :bindings {}})))
  (is (= 20 (total/step-delay {:wait-ms 20} 100)))
  (is (= :failed (:status (total/step-result {:effects [{:effect :activity :request {:id "child" :step "x" :output "x"}}]} {} 10))))
  (is (= :exhausted (:status (total/step-result {:wait-ms 20 :effects [{:effect :session :writes {"mark" "seen"}}] :state {}} {} 1))))
  (let [facts [{:id :a :tuple [:edge "a" "b"]} {:id :b :tuple [:edge "b" "a"]}]
        rules [{:id :direct :head '[:reach ?a ?b] :body '[[:edge ?a ?b]]}
               {:id :transitive :head '[:reach ?a ?c] :body '[[:reach ?a ?b] [:edge ?b ?c]]}]
        result (logic/derive {:facts facts :rules rules})]
    (is (:complete? result))
    (is (contains? (:conclusions result) [:reach "a" "a"]))
    (is (not (contains? (:conclusions (logic/derive {:facts (vec (rest facts)) :rules rules})) [:reach "a" "a"])))
    (is (= :exhausted (:runtime/status (logic/derive {:facts facts :rules rules :budget 1}))))
    (is (= :failed (:runtime/status (logic/derive {:facts facts :rules [{:id :negative :head '[:edge ?a ?b] :body '[[:edge ?a ?b]] :not '[[:edge ?b ?a]]}]}))))))

(deftest external-intent-ownership-and-uncertainty
  (let [decoded (resident/decode-result "{\"type\":\"system\",\"subtype\":\"init\"}\n{\"type\":\"result\",\"subtype\":\"success\",\"result\":\"Terminal fixture\"}" 0)]
    (is (= :complete (:status decoded)))
    (is (= "Terminal fixture" (:reply decoded)))
    (is (= :stream (get-in decoded [:provider-result :format]))))
  (is (= :complete (:status (resident/decode-result "[{\"type\":\"result\",\"subtype\":\"success\",\"result\":\"Protocol fixture\"}]" 0))))
  (is (= :unconfirmed (:status (resident/decode-result "{\"type\":\"assistant\",\"content\":\"No terminal result\"}" 0))))
  (let [w "external" _ (seed! w)
        intent {:runner :claude :owner "world" :input "A small request" :basis {:definition "ask-rule" :revision 1}
                :model "haiku" :max-output 100 :timeout-seconds 30}]
    (is (= :accepted (:status (submit w :start "ask" {:intent intent}))))
    (is (= :pending (:status (read-one w :rows "base/ask"))))
    (is (= :accepted (:status (submit w :claim "ask" {:actor "executor" :execution-owner "one"}))))
    (is (= :rejected (:status (submit w :claim "ask" {:actor "executor" :execution-owner "two"}))))
    (is (= :rejected (:status (submit w :observe "ask" {:actor "executor" :execution-owner "two" :status :complete :reply "foreign"}))))
    (is (= :accepted (:status (submit w :observe "ask" {:actor "executor" :execution-owner "one" :status :unconfirmed}))))
    (is (= :rejected (:status (submit w :claim "ask" {:actor "executor" :execution-owner "two"}))))
    (is (= :accepted (:status (submit w :observe "ask" {:actor "executor" :execution-owner "one" :status :complete :reply "late"}))))
    (is (true? (:late-result (read-one w :rows "base/ask")) ))
    (is (= :machine (:provenance (read-one w :rows "base/ask"))))
    (is (= :rejected (:status (submit w :observe "ask" {:actor "executor" :execution-owner "one" :status :failed}))))
    (is (= :rejected (:status (submit w :put "ask" {:expected-revision 4 :row {:name "ask" :activity intent :status :pending}}))))
    (is (= :rejected (:status (submit w :observe "unknown" {:actor "executor" :status :complete :reply "No intent"}))))
    (is (= :rejected (:status (submit w :start "malformed" {:intent (assoc intent :max-output "wrong type")}))))
    (is (= :rejected (:status (submit w :put "machine-rule" {:actor "resident" :expected-revision 0 :row {:name "machine-rule" :body {:steps [] :return nil}}}))))))

(defn -main
  "Launch → fresh two-task IPC, focused tests, then IPC disposal.
   Installs test store handles, shuts down futures and exits nonzero on failures.
   This entry never invokes the real provider or the disk-backed product cluster."
  [& _]
  (with-open [ipc (rt/create-ipc)]
    (rt/launch-module! ipc module/material {:tasks 2 :threads 2})
    (let [name (r/get-module-name module/material)
          h (into {:depot (r/foreign-depot ipc name "*operations")}
              (for [kind [:rows :versions :index :decisions :workspaces]] [kind (r/foreign-pstate ipc name (str "$$" (clojure.core/name kind)))]))]
      (reset! handles h) (reset! store/connection h)
      (let [result (run-tests 'softland.inland.test-runner)]
        (shutdown-agents)
        (when (pos? (+ (:fail result) (:error result))) (System/exit 1))))))
