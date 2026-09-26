(ns rig.probe.tools-lease-probe
  "Probe for BUILD_NOTES-tools-and-grammars.md (part 2, step 1; the plan's
  13.2): (a) the read exit with the operator as reader and `:permission
  nil`; (b) a tool with only a grant in `:alice` reading through the exit
  and offering an output under its pid, the door leasing as the tool (D-P2:
  wave 1's W1-1 seals its lease under Alice); and a smoke of the gate's key
  rows: a grammar fact admitted, its row written, a value refused by it."
  (:require [clojure.test :refer [deftest is]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [rig.store.client :as c]
            [rig.store.module :as m]
            [rig.store.read-exit :as rx]))

(defn- say [& xs] (apply println "PROBE" xs))

(deftest tools-lease
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc m/Store {:tasks 4 :threads 2 :workers 1})
    (let [st (rx/connect ipc)
          seeded (c/seed! st)
          op (fn [facts] (c/offer-until-answered! st (c/build {:who :operator :layer :alice :class :by-layer :facts facts})))
          alice (fn [facts] (c/offer-until-answered! st (c/build {:who :alice :layer :alice :class :by-layer
                                                                  :permission [:alice :alice :alice] :facts facts})))]
      (say "seed" (frequencies (map :answer seeded)))
      ;; the gate's key rows: a grammar fact, its row, a value refused by it
      (let [g (op [{:e :mention :k :grammar :v {:shape [:map {:persons [:set-of [:keyword] 1 2]} {:open? true}]
                                                 :subjects-at [:persons] :opaque false :index #{}}}])
            row (foreign-select-one [(keypath :alice :key-rows :mention)] (:layers st))
            bad (alice [{:e :e1 :k :mention :v {:persons #{:bob :carol :dan}}}])
            good (alice [{:e :e2 :k :mention :v {:persons #{:bob}}}])
            used (foreign-select-one [(keypath :alice :key-rows :mention)] (:layers st))]
        (say "grammar" (select-keys g [:answer :reason]) "row" row)
        (say "three people" (select-keys bad [:answer :reason]) "one person" (select-keys good [:answer :reason])
             "subjects" (:subjects (c/record st (:name good))))
        (say "row after use" used)
        (is (= :yes (:answer g)))
        (is (= :value-shape (:reason bad)))
        (is (= :yes (:answer good))))
      ;; (a) the operator reads through the exit with no permission
      (let [r (rx/read! st {:reader :operator :reader-kind :tool :rows? false :role :stood-on :for :alice
                            :working :alice :permission nil :layer :alice :read [:pattern [:k :mention]]})]
        (say "operator read" (select-keys r [:refused :mark :matched :entry-stamp]))
        (is (not (contains? r :refused))))
      ;; (b) a tool with only a grant reads and writes, the door leasing as the tool
      (let [g (c/offer-until-answered! st (c/grant-offer st [:probe-tool :alice :alice]))
            r (rx/read! st {:reader :probe-tool :reader-kind :tool :rows? false :role :matched :for :alice
                            :working :alice :permission [:probe-tool :alice :alice] :layer :alice
                            :read [:pattern [:k :mention]]})
            o (c/offer-until-answered! st (c/build {:who :probe-tool :layer :alice :class :by-layer
                                                     :permission [:probe-tool :alice :alice]
                                                     :session (c/default-session :probe-tool)
                                                     :facts [{:e :e9 :k :note :v {:token "p"}}]}))
            leases (c/lease-rows st :alice (c/default-session :probe-tool))]
        (say "grant" (select-keys g [:answer :reason]))
        (say "tool read" (select-keys r [:refused :mark :matched]))
        (say "tool output" (select-keys o [:answer :reason]))
        (say "tool's unconsumed lease rows" (count leases) "under" (set (map (comp :under second) leases)))
        (is (not (contains? r :refused)))
        (is (= :yes (:answer o)))))))
