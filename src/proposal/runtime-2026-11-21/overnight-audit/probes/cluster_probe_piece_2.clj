(ns cluster-probe-piece-2
  "In-process Rama cluster verification of the store piece 2 findings.
  Must be run behind cluster.lock."
  (:require [com.rpl.rama :as rama]
            [com.rpl.rama.test :as rtest]
            [softland.store.door :as door]
            [softland.store.door.digest :as digest]
            [softland.store.module :as module]
            [softland.store.read-path :as read-path])
  (:import [java.util UUID]))

(defn send-and-answer! [d act act-id]
  (let [sent (if act-id (door/send-act d act act-id) (door/send-act d act))]
    {:sent sent
     :answer (when-not (:could-not sent)
               (door/answer-of d (:act-id sent) (:digest sent) 5000))}))

(defn -main [& _]
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc module/Store {:tasks 4 :threads 2})
    (let [mod-name (rama/get-module-name module/Store)
          d (door/open ipc mod-name)
          rp (read-path/open ipc mod-name)]

      (println "=== CLUSTER RECEIPT 1: Re-granting :act-permission after end + attempt (R5 vs Check a) ===")
      (let [layer :c-lab
            root [:asha layer layer]
            _ (send-and-answer! d {:version 1 :who :asha :layer layer :permission root :session :s0
                                   :facts [{:e layer :k :softland.store/depot :v :streaming}
                                           {:e :asha :k :softland.store/permission :v root}]} nil)
            act-id (door/act-id-for d layer)
            u (nth act-id 2)
            maker-x {:version 1 :who :ben :layer layer :session :run-1
                     :facts [{:e :exp-1 :k :dose :v "5 mg"}]}
            ;; Note: maker must synthesize :act-id and :depot manually just to compute hex!
            hex-x (digest/hex-for-its-permission (assoc maker-x :act-id act-id :depot :streaming))
            pa1 [:ben layer layer root :see-and-act [:act u hex-x]]
            _ (send-and-answer! d {:version 1 :who :asha :layer layer :permission root :session :s0
                                   :facts [{:e u :k :softland.store/act-permission :v pa1}]} nil)
            _ (send-and-answer! d {:version 1 :who :asha :layer layer :permission root :session :s0
                                   :facts [{:e u :k :softland.store/act-permission}]} nil)
            attempt-1 (send-and-answer! d (assoc maker-x :permission pa1) act-id)
            ;; Cal gets layer permission and grants pa2 for the exact same act u (R5)
            cal-perm [:cal layer layer root :see-and-act]
            _ (send-and-answer! d {:version 1 :who :asha :layer layer :permission root :session :s0
                                   :facts [{:e :cal :k :softland.store/permission :v cal-perm}]} nil)
            pa2 [:ben layer layer cal-perm :see-and-act [:act u hex-x]]
            re-grant (send-and-answer! d {:version 1 :who :cal :layer layer :permission cal-perm :session :s0
                                          :facts [{:e u :k :softland.store/act-permission :v pa2}]} nil)
            attempt-2 (send-and-answer! d (assoc maker-x :permission pa2) act-id)
            exp-read (read-path/read-once rp {:asks [:place :exp-1 :dose] :asker :asha :layers [layer]})]
        (println "  attempt-1 (after end):      " (:answer attempt-1))
        (println "  re-grant pa2 (R5):          " (:answer re-grant))
        (println "  attempt-2 (under live pa2): " (:answer attempt-2))
        (println "  exp-1 place in store:       " (:place exp-read)))

      (println "\n=== CLUSTER RECEIPT 2: :see-only reader reads :act-permission via read-path & burns act-id ===")
      (let [layer :c-studio
            ida [:ida layer layer]
            cal-see [:cal layer layer ida :see]
            _ (send-and-answer! d {:version 1 :who :ida :layer layer :permission ida :session :s0
                                   :facts [{:e layer :k :softland.store/depot :v :streaming}
                                           {:e :ida :k :softland.store/permission :v ida}]} nil)
            _ (send-and-answer! d {:version 1 :who :ida :layer layer :permission ida :session :s0
                                   :facts [{:e :cal :k :softland.store/permission :v cal-see}]} nil)
            joe-act-id (door/act-id-for d layer)
            u (nth joe-act-id 2)
            maker-joe {:version 1 :who :joe :layer layer :session :run-1
                       :facts [{:e :secret :k :dose :v "5 mg"}]}
            pa [:joe layer layer ida :see-and-act
                [:act u (digest/hex-for-its-permission (assoc maker-joe :act-id joe-act-id :depot :streaming))]]
            _ (send-and-answer! d {:version 1 :who :ida :layer layer :permission ida :session :s0
                                   :facts [{:e u :k :softland.store/act-permission :v pa}]} nil)
            ;; Cal (:see only!) reads store's own facts:
            cal-saw (read-path/read-once rp {:asks [:run :first :last] :asker :cal :layers [layer] :stores-own-facts true})
            leaked-u (first (keep (fn [[[e k] _]] (when (= :softland.store/act-permission k) e)) (:places cal-saw)))
            ;; Cal sends a dummy act under [layer :streaming leaked-u]:
            cal-burn (send-and-answer! d {:version 1 :who :cal :layer layer :permission cal-see :session :s-cal
                                          :facts [{:e :x :k :y :v 1}]}
                                       [layer :streaming leaked-u])
            ;; Joe sends his valid permitted act:
            joe-res (send-and-answer! d (assoc maker-joe :permission pa) joe-act-id)
            secret-read (read-path/read-once rp {:asks [:place :secret :dose] :asker :ida :layers [layer]})]
        (println "  Cal (:see) discovered UUID? " (= u leaked-u))
        (println "  Cal's dummy send answer:    " (:answer cal-burn))
        (println "  Joe's permitted act answer: " (:answer joe-res))
        (println "  Joe's fact in store:        " (:place secret-read)))

      (println "\n=== CLUSTER RECEIPT 3: Live read in :session leaks :dov's grant/end in :may-not-see layer ===")
      (let [near :c-near
            far :c-far
            near-root [:root near near]
            bea-perm [:bea near near near-root]
            far-root [:root far far]
            _ (send-and-answer! d {:version 1 :who :root :layer near :permission near-root :session :s0
                                   :facts [{:e near :k :softland.store/depot :v :streaming}
                                           {:e :root :k :softland.store/permission :v near-root}]} nil)
            _ (send-and-answer! d {:version 1 :who :root :layer near :permission near-root :session :s0
                                   :facts [{:e :bea :k :softland.store/permission :v bea-perm}
                                           {:e :p :k :text :v "secret"}]} nil)
            _ (send-and-answer! d {:version 1 :who :root :layer far :permission far-root :session :s0
                                   :facts [{:e far :k :softland.store/depot :v :streaming}
                                           {:e :root :k :softland.store/permission :v far-root}]} nil)
            _ (send-and-answer! d {:version 1 :who :root :layer far :permission far-root :session :s0
                                   :facts [{:e far :k :softland.store/who-may-see :v :everyone}
                                           {:e :p :k :text :v "public"}]} nil)
            events (atom [])
            live (read-path/read-live rp {:asks [:place :p :text] :asker :ann :session :run-7 :layers [near far]}
                                      (fn [ev] (swap! events conj ev)))
            pushes-before (:pushes (read-path/open-count rp))
            ;; Grant :dov on :run-7 in :near
            _ (send-and-answer! d {:version 1 :who :bea :layer near :permission bea-perm :session :s0
                                   :facts [{:e :run-7 :k :softland.store/run-permission
                                            :v [:dov near near bea-perm :see [:run :run-7]]}]} nil)
            _ (Thread/sleep 250)
            pushes-during (:pushes (read-path/open-count rp))
            ;; End :dov on :run-7 in :near
            _ (send-and-answer! d {:version 1 :who :bea :layer near :permission bea-perm :session :s0
                                   :facts [{:e :run-7 :k :softland.store/run-permission}]} nil)
            _ (Thread/sleep 250)
            pushes-after (:pushes (read-path/open-count rp))
            _ (read-path/leave rp (:leave-with live))]
        (println "  pushes before / during :dov's grant / after :dov's end:" [pushes-before pushes-during pushes-after])
        (println "  total callbacks delivered to :ann (expected 1 initial :anew):" (count @events))
        (println "  all callbacks identical {:anew ...}?"
                 (and (every? #(contains? % :anew) @events) (apply = @events))))

      (println "\n=== CLUSTER RECEIPT 4: 8-round static live read fails with fake :get-an-answer-within-its-wait on 1 concurrent fact write ===")
      (let [mk! (fn [l more-facts]
                  (let [r [:root l l]]
                    (send-and-answer! d {:version 1 :who :root :layer l :permission r :session :s0
                                         :facts [{:e l :k :softland.store/depot :v :streaming}
                                                 {:e :root :k :softland.store/permission :v r}]} nil)
                    (send-and-answer! d {:version 1 :who :root :layer l :permission r :session :s0
                                         :facts more-facts} nil)))]
        (mk! :c-start [{:e :c-start :k :softland.store/builds-on :v [:c-mid :c-far]}
                       {:e :run-7 :k :softland.store/run-permission
                        :v [:cal :c-start :c-start [:root :c-start :c-start] :see [:run :run-7]]}])
        (mk! :c-mid [{:e :run-7 :k :softland.store/run-permission
                      :v [:cal :c-mid :c-mid [:root :c-mid :c-mid] :see [:run :run-7]]}])
        (mk! :c-far [{:e :c-far :k :softland.store/marked-keys :v #{:parent}}
                     {:e :run-7 :k :softland.store/run-permission
                      :v [:cal :c-far :c-far [:root :c-far :c-far] :see [:run :run-7]]}])
        (send-and-answer! d {:version 1 :who :root :layer :c-far :permission [:root :c-far :c-far] :session :s0
                             :facts [{:e :item-1 :k :parent :v :canvas-1}]} nil)
        ;; Inject exactly ONE ordinary fact write on :c-far when open-push opens the round-7 check on :c-start/:item-1:
        (let [orig-open-push @#'softland.store.read-path/open-push
              wrote? (atom false)
              t0 (System/nanoTime)
              res (with-redefs [softland.store.read-path/open-push
                                (fn [rp* source called]
                                  (let [fut (orig-open-push rp* source called)]
                                    (when (and (= :c-start (:layer source))
                                               (= [:row ":c-start :item-1 :parent"] (:path source))
                                               (compare-and-set! wrote? false true))
                                      (send-and-answer! d {:version 1 :who :root :layer :c-far
                                                           :permission [:root :c-far :c-far] :session :s0
                                                           :facts [{:e :item-2 :k :parent :v :canvas-1}]} nil)
                                      (Thread/sleep 100))
                                    fut))]
                    (read-path/read-live rp {:asks [:points-at :parent :canvas-1]
                                             :asker :cal :session :run-7 :from-layer :c-start}
                                         (fn [_])))
              elapsed-ms (quot (- (System/nanoTime) t0) 1000000)]
          (println "  read-live result:" res)
          (println "  actual elapsed ms (despite :waited-ms 10000):" elapsed-ms)))))
  (System/exit 0))
