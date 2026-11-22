(ns cluster-probe-piece-2-yes
  "In-process 4-task Rama cluster probes against Store Piece 2 repair + yes
  (commits 27d883a3 and d3290f37, diff since 1a2e9811).
  Must be run behind cluster.lock."
  (:require [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [com.rpl.rama.test :as rtest]
            [softland.store.door :as door]
            [softland.store.door.digest :as digest]
            [softland.store.module :as module]
            [softland.store.read-path :as read-path]
            [softland.store.record.rows :as rows])
  (:import [java.util UUID]))

(defn send! [d act & [act-id]]
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

      (println "=== CLUSTER RECEIPT 1: Confidential data leaked to :may-not-see asker on live 4-task cluster ===")
      ;; Zero with-redefs! Natural Rama ProxyState delivery when :bea writes :ann's give before :bea's own end
      ;; in one atomic act while another live read on the shared ReadPath holds the secret place open.
      (let [secret :c-secret
            pub :c-pub
            s-root [:root secret secret]
            p-root [:root pub pub]
            dave-p [:dave secret secret s-root :see-and-act]
            bea-p1 [:bea secret secret dave-p :see-and-act]
            ann-p1 [:ann secret secret bea-p1 :see]
            bea-p2 [:bea secret secret s-root :see-and-act]
            ann-p2 [:ann secret secret bea-p2 :see]
            _ (send! d {:version 1 :who :root :layer secret :permission s-root :session :s0
                        :facts [{:e secret :k :softland.store/depot :v :streaming}
                                {:e :root :k :softland.store/permission :v s-root}]})
            _ (send! d {:version 1 :who :root :layer secret :permission s-root :session :s0
                        :facts [{:e :dave :k :softland.store/permission :v dave-p}]})
            _ (send! d {:version 1 :who :dave :layer secret :permission dave-p :session :s0
                        :facts [{:e :bea :k :softland.store/permission :v bea-p1}]})
            _ (send! d {:version 1 :who :bea :layer secret :permission bea-p1 :session :s0
                        :facts [{:e :ann :k :softland.store/permission :v ann-p1}]})
            ;; End :dave so :ann's chain is dead (:may-not-see):
            _ (send! d {:version 1 :who :root :layer secret :permission s-root :session :s0
                        :facts [{:e :dave :k :softland.store/permission}]})
            ;; Write confidential fact AFTER :ann lost standing (:ann NEVER had standing for this value!):
            _ (send! d {:version 1 :who :root :layer secret :permission s-root :session :s0
                        :facts [{:e :rx :k :dose :v "TOP-SECRET-99mg"}]})
            _ (send! d {:version 1 :who :root :layer pub :permission p-root :session :s0
                        :facts [{:e pub :k :softland.store/depot :v :streaming}
                                {:e :root :k :softland.store/permission :v p-root}]})
            _ (send! d {:version 1 :who :root :layer pub :permission p-root :session :s0
                        :facts [{:e pub :k :softland.store/who-may-see :v :everyone}
                                {:e :rx :k :dose :v "aspirin-81mg"}]})
            ;; :root keeps a live read on [:rx :dose] in :c-secret on the process's shared ReadPath:
            root-live (read-path/read-live rp {:asks [:place :rx :dose] :asker :root :layers [secret]} (fn [_]))
            ;; :ann opens a live read across [:c-secret :c-pub], and also an isolated ReadPath rp2 with NO shared fact push:
            rp-isolated (read-path/open ipc mod-name)
            ann-events-shared (atom [])
            ann-events-isolated (atom [])
            ann-live-shared (read-path/read-live rp {:asks [:place :rx :dose] :asker :ann :layers [secret pub]}
                                                 (fn [ev] (swap! ann-events-shared conj ev)))
            ann-live-isolated (read-path/read-live rp-isolated {:asks [:place :rx :dose] :asker :ann :layers [secret pub]}
                                                   (fn [ev] (swap! ann-events-isolated conj ev)))
            ;; :root gives :bea bea-p2 (:ann still :may-not-see!)
            _ (send! d {:version 1 :who :root :layer secret :permission s-root :session :s0
                        :facts [{:e :bea :k :softland.store/permission :v bea-p2}]})
            _ (Thread/sleep 200)
            ;; :bea sends ONE atomic act: give :ann ann-p2 (fact 0) + end :bea (fact 1)!
            atomic-res (send! d {:version 1 :who :bea :layer secret :permission bea-p2 :session :s0
                                 :facts [{:e :ann :k :softland.store/permission :v ann-p2}
                                         {:e :bea :k :softland.store/permission}]})
            _ (Thread/sleep 300)
            once-after (read-path/read-once rp {:asks [:place :rx :dose] :asker :ann :layers [secret pub]})]
        (read-path/leave rp (:leave-with ann-live-shared))
        (read-path/leave rp (:leave-with root-live))
        (read-path/leave rp-isolated (:leave-with ann-live-isolated))
        (println "  atomic end-and-give door answer:           " (:answer atomic-res))
        (println "  read-once for :ann after act (:left-out):  " (:left-out once-after) "| value:" (get-in once-after [:place :value]))
        (println "  [shared ReadPath] callbacks to :ann (exp 1):" (count @ann-events-shared))
        (doseq [[i ev] (map-indexed vector @ann-events-shared)]
          (println (format "    callback %d -> value=%-18s left-out=%s"
                           i (pr-str (get-in ev [:anew :place :value])) (pr-str (get-in ev [:anew :left-out])))))
        (println "  [isolated ReadPath] callbacks to :ann (exp 1):" (count @ann-events-isolated))
        (doseq [[i ev] (map-indexed vector @ann-events-isolated)]
          (println (format "    callback %d -> value=%-18s left-out=%s"
                           i (pr-str (get-in ev [:anew :place :value])) (pr-str (get-in ev [:anew :left-out]))))))

      (println "\n=== CLUSTER RECEIPT 2: :base (:microbatch) yes through door + 4-task replication + 256-fact 771-write yes ===")
      (let [b-root [:system :base :base]
            _ (send! d {:version 1 :who :system :layer :base :permission b-root :session :s0
                        :facts [{:e :base :k :softland.store/depot :v :microbatch}
                                {:e :system :k :softland.store/permission :v b-root}]})
            _ (send! d {:version 1 :who :system :layer :base :permission b-root :session :s0
                        :facts [{:e :base :k :softland.store/marked-keys :v #{:tag}}]})
            entities (mapv #(keyword (str "item-" %)) (range 256))
            _ (send! d {:version 1 :who :system :layer :base :permission b-root :session :s0
                        :facts (mapv (fn [e] {:e e :k :tag :v :v1}) entities)})
            yes-id (UUID/randomUUID)
            maker-256 {:version 1 :who :ben :layer :base :session :run-base
                       :facts (mapv (fn [e] {:e e :k :tag :v :v2}) entities)}
            hex-256 (digest/hex-for-a-yes maker-256)
            pa-base [:ben :base :base b-root :see-and-act [:act yes-id hex-256]]
            grant-res (send! d {:version 1 :who :system :layer :base :permission b-root :session :s0
                                :facts [{:e yes-id :k :softland.store/act-permission :v pa-base}]})
            use-res (send! d (assoc maker-256 :permission pa-base))
            retry-new-id (send! d (assoc maker-256 :permission pa-base))
            mb-facts (rama/foreign-pstate ipc mod-name "$$microbatch-facts")
            yes-key (rows/place-key :base yes-id :softland.store/act-permission)
            per-task-ended (mapv (fn [p]
                                   (rows/ended? (rama/foreign-select-one (path/keypath yes-key) mb-facts {:pkey p})))
                                 (range 4))
            own-read (read-path/read-once rp {:asks [:place yes-id :softland.store/act-permission]
                                              :asker :system :layers [:base]
                                              :stores-own-facts true :with #{:writer :permission}})]
        (println "  :base yes grant answer:                    " (:answer grant-res))
        (println "  :base 256-fact yes redeem answer (771 wr): " (:answer use-res))
        (println "  :base second send under new act-id answer: " (:answer retry-new-id))
        (println "  use-of-its-yes replicated on all 4 tasks?  " per-task-ended)
        (println "  read-once :stores-own-facts on used yes:   " (:place own-read)))

      (println "\n=== CLUSTER RECEIPT 3: Module update (restart) + standing & used yeses across restart ===")
      (let [layer :c-restart
            root [:asha layer layer]
            _ (send! d {:version 1 :who :asha :layer layer :permission root :session :s0
                        :facts [{:e layer :k :softland.store/depot :v :streaming}
                                {:e :asha :k :softland.store/permission :v root}]})
            y-stand (UUID/randomUUID)
            y-used (UUID/randomUUID)
            m-stand {:version 1 :who :ben :layer layer :session :s1 :facts [{:e :after :k :v :v "redeemed-after-update"}]}
            m-used {:version 1 :who :ben :layer layer :session :s1 :facts [{:e :before :k :v :v "redeemed-before-update"}]}
            pa-stand [:ben layer layer root :see-and-act [:act y-stand (digest/hex-for-a-yes m-stand)]]
            pa-used [:ben layer layer root :see-and-act [:act y-used (digest/hex-for-a-yes m-used)]]
            _ (send! d {:version 1 :who :asha :layer layer :permission root :session :s0
                        :facts [{:e y-stand :k :softland.store/act-permission :v pa-stand}
                                {:e y-used :k :softland.store/act-permission :v pa-used}]})
            used-before (send! d (assoc m-used :permission pa-used))
            _ (rtest/update-module! ipc module/Store)
            d2 (door/open ipc mod-name)
            rp2 (read-path/open ipc mod-name)
            used-again-after (send! d2 (assoc m-used :permission pa-used))
            stand-used-after (send! d2 (assoc m-stand :permission pa-stand))
            stand-twice-after (send! d2 (assoc m-stand :permission pa-stand))
            after-fact (read-path/read-once rp2 {:asks [:place :after :v] :asker :asha :layers [layer]})]
        (println "  y-used before update:        " (:answer used-before))
        (println "  y-used retried after update: " (:answer used-again-after))
        (println "  y-stand used after update:   " (:answer stand-used-after))
        (println "  y-stand 2nd try after update:" (:answer stand-twice-after))
        (println "  :after fact in store:        " (:place after-fact)))

      (println "\n=== CLUSTER RECEIPT 4: Forged yes on used UUID & unredeemable 257-fact yes through door ===")
      (let [layer :c-edge
            root [:asha layer layer]
            cal-p [:cal layer layer root :see-and-act]
            _ (send! d {:version 1 :who :asha :layer layer :permission root :session :s0
                        :facts [{:e layer :k :softland.store/depot :v :streaming}
                                {:e :asha :k :softland.store/permission :v root}]})
            _ (send! d {:version 1 :who :asha :layer layer :permission root :session :s0
                        :facts [{:e :cal :k :softland.store/permission :v cal-p}]})
            y1 (UUID/randomUUID)
            m1 {:version 1 :who :ben :layer layer :session :s1 :facts [{:e :x :k :v :v 1}]}
            m2 {:version 1 :who :ben :layer layer :session :s1 :facts [{:e :x :k :v :v 2}]}
            pa1 [:ben layer layer root :see-and-act [:act y1 (digest/hex-for-a-yes m1)]]
            _ (send! d {:version 1 :who :asha :layer layer :permission root :session :s0
                        :facts [{:e y1 :k :softland.store/act-permission :v pa1}]})
            _ (send! d (assoc m1 :permission pa1))
            wrong-hex-on-used (send! d (assoc m2 :permission pa1))
            pa-forged [:ben layer layer cal-p :see-and-act [:act y1 (digest/hex-for-a-yes m2)]]
            forged-on-used (send! d (assoc m1 :permission pa-forged))
            ;; 257-fact unredeemable yes:
            y-257 (UUID/randomUUID)
            m-257 {:version 1 :who :ben :layer layer :session :s1
                   :facts (mapv (fn [i] {:e (keyword (str "f" i)) :k :v :v i}) (range 257))}
            pa-257 [:ben layer layer root :see-and-act [:act y-257 (digest/hex-for-a-yes m-257)]]
            grant-257 (send! d {:version 1 :who :asha :layer layer :permission root :session :s0
                                :facts [{:e y-257 :k :softland.store/act-permission :v pa-257}]})
            redeem-257 (send! d (assoc m-257 :permission pa-257))]
        (println "  wrong-digest m2 on used pa1 (SPEC R9 says :act-not-the-one-permitted):" (:answer wrong-hex-on-used))
        (println "  never-given forged yes on used y1 (gate says :permission-ended!):     " (:answer forged-on-used))
        (println "  grant of 257-fact yes answer:                                         " (:answer grant-257))
        (println "  holder redeem of 257-fact yes answer:                                 " (:answer redeem-257)))))
  (shutdown-agents)
  (System/exit 0))
