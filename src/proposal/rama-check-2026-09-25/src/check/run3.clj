(ns check.run3
  "Third, short run: does the class of the thrown exception decide whether the
   worker dies? Throws a RuntimeException, Rama's own IntentionalFailureException,
   and an ExceptionInfo (control), each once on partition B of a retrying
   stream event. Prints wall-clock timestamps so the log's worker deaths can be
   aligned with each experiment."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [check.module :as m]
            [check.faults :as faults]
            [check.run :as r]
            [clojure.pprint :as pp]))

(defn now [] (.format (java.text.SimpleDateFormat. "HH:mm:ss.SSS") (java.util.Date.)))

(defn -main [& kinds]
  (println (now) "run3 start")
  (let [c (Class/forName "rpl.rama.distributed.exceptions.IntentionalFailureException")]
    (println "IntentionalFailureException super:" (.getName (.getSuperclass c))
             "ctors:" (mapv #(mapv (fn [t] (.getSimpleName t)) (.getParameterTypes %)) (.getConstructors c))))
  (try
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc m/StampModule {:tasks 4 :threads 4 :workers 1})
      (rtest/launch-module! ipc m/CheckModule {:tasks 4 :threads 4 :workers 1})
      (let [mn (get-module-name m/CheckModule)
            [_k0 kA kB _k3] (rtest/gen-hashing-index-keys 4)
            s-retry (foreign-depot ipc mn "*s-retry")
            $s-a (foreign-pstate ipc mn "$$s-a")
            $s-b (foreign-pstate ipc mn "$$s-b")
            s-a (fn [] (r/read! #(foreign-select-one (keypath kA) $s-a)))
            s-b (fn [] (r/read! #(foreign-select-one (keypath kB) $s-b)))
            rec (fn [id] {:id id :ka kA :kb kB})]
        (r/try-append s-retry (rec "s0") :ack 30000)
        (println (now) "baseline done; A:" (r/ids-in (s-a)) "B:" (r/ids-in (s-b)))
        (doseq [[id kind] (if (seq kinds)
                            (for [k kinds] [(str "x-" k) (keyword k)])
                            [["x-runtime" :runtime] ["x-intentional" :intentional] ["x-exinfo" :ex-info]])]
          (println)
          (println (now) "START" id "throwing" kind "once on B")
          (let [a0 (frequencies (r/ids-in (s-a)))]
            (faults/arm-fail! id :b 1 kind)
            (let [ap (r/try-append s-retry (rec id) :append-ack 30000)
                  done (r/wait-until #(>= (faults/attempts id :b) 2) 30000 (str id " to reach B twice"))
                  quiet (r/settle! 3000)
                  a1 (frequencies (r/ids-in (s-a)))]
              (println (now) "END" id)
              (r/record! (str "stream, retry :individual, throw " (name kind) " once on B")
                         (if done :ran :error)
                         {:append ap :attempts (faults/snapshot id)
                          :a-entries (r/entries-for (s-a) id) :b-entries (r/entries-for (s-b) id)
                          :other-records-replayed (into {} (for [[k n] a1 :when (not= k id) :let [d (- n (get a0 k 0))] :when (pos? d)] [k d]))
                          :worker-connection-losses @r/conn-losses}))))))
    (catch Throwable e (println "RUN ABORTED:" (str e)))
    (finally
      (println)
      (println (now) "run3 end")
      (spit "results3.edn" (with-out-str (pp/pprint @r/results)))))
  (shutdown-agents)
  (System/exit 0))
