(ns rig.smoke-test
  (:require [clojure.test :refer [deftest is]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [rig.store.client :as c]
            [rig.store.module :as m]))

(deftest smoke
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc m/Store {:tasks 4 :threads 2})
    (let [st (c/connect ipc)
          seeded (c/seed! st)
          _ (println "SEED" (mapv (juxt :answer :reason) seeded))
          o (c/build {:who :alice :layer :alice :class :by-layer :permission [:alice :alice :alice]
                      :facts [{:e :e0 :k :note :v {:token "v1"}} {:e :e1 :k :mention :v {:persons #{:bob}}}]})
          a (c/offer! st o)
          a2 (c/offer! st o)
          o2 (assoc o :facts [{:e :e0 :k :note :v "other"}])
          a3 (c/offer! st o2)]
      (println "ANSWER" a)
      (println "RESEND" a2)
      (println "REUSE" a3)
      (println "FACTS" (c/facts st :alice (:name o)))
      (println "SETTINGS" (c/settings st :alice))
      (println "PERM" (c/permission st :alice [:alice :alice :alice]))
      (println "HEAD" (c/head st :alice :e0 :note [(:name o) 0]))
      (println "CLOCK" (c/clock st :alice))
      (println "LOOKUP" (c/lookup st o) (c/lookup st o2))
      (println "FACE" (c/offer! st {:junk 1}) (c/offer! st 42))
      (is (= :yes (:answer a))))))
