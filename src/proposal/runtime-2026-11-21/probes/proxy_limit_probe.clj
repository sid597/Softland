(ns proxy-limit-probe
  "Black-box public-API probe on Rama 1.6.0 InProcessCluster measuring what
   `pstate.proxy.limit` bounds (src-store/OPEN-QUESTIONS-rama.md, section 4;
   src/proposal/runtime-2026-11-21/PROPOSAL.md, 'Not checked').

   Run from repository root:
     clojure -Sdeps '{:paths [\"src/proposal/runtime-2026-11-21/probes\"]}' -M:store-module -m proxy-limit-probe"
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest]))

(defmodule ModA [setup topologies]
  (declare-depot setup *d :random)
  (let [s (stream-topology topologies "s")]
    (declare-pstate s $$p1 {Object Object})
    (declare-pstate s $$p2 {Object Object})
    (set-launch-pstate-dynamic-option! setup "$$p1" "pstate.proxy.limit" 4)
    (set-launch-pstate-dynamic-option! setup "$$p2" "pstate.proxy.limit" 6)
    (<<sources s
      (source> *d :> [*k *v])
      (|hash *k)
      (local-transform> [(keypath *k) (termval *v)] $$p1)
      (local-transform> [(keypath *k) (termval *v)] $$p2))))

(defmodule ModB [setup topologies]
  (declare-depot setup *d :random)
  (let [s (stream-topology topologies "s")]
    (declare-pstate s $$p3 {Object Object})
    (set-launch-pstate-dynamic-option! setup "$$p3" "pstate.proxy.limit" 4)
    (<<sources s
      (source> *d :> [*k *v])
      (|hash *k)
      (local-transform> [(keypath *k) (termval *v)] $$p3))))

(defn try-open-n [pstate k n]
  (loop [i 0 opened []]
    (if (= i n)
      {:opened (count opened) :proxies opened :failed-at nil}
      (let [res (try
                  [:ok (foreign-proxy [(keypath k)] pstate)]
                  (catch Throwable t
                    [:err (.getMessage t)]))]
        (if (= :ok (first res))
          (recur (inc i) (conj opened (second res)))
          {:opened (count opened) :proxies opened :failed-at i :err (second res)})))))

(defn close-all! [proxies]
  (doseq [p proxies] (.close ^java.io.Closeable p)))

(defn -main [& _]
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc ModA {:tasks 2 :threads 2})
    (rtest/launch-module! ipc ModB {:tasks 2 :threads 2})
    (let [mod-a (get-module-name ModA)
          mod-b (get-module-name ModB)
          [k0 k1] (rtest/gen-hashing-index-keys 2)
          da (foreign-depot ipc mod-a "*d")
          db (foreign-depot ipc mod-b "*d")
          _ (foreign-append! da [k0 "v0"])
          _ (foreign-append! da [k1 "v1"])
          _ (foreign-append! db [k0 "v0"])
          p1-a (foreign-pstate ipc mod-a "$$p1")
          p1-b (foreign-pstate ipc mod-a "$$p1")
          p2   (foreign-pstate ipc mod-a "$$p2")
          p3   (foreign-pstate ipc mod-b "$$p3")
          ;; 1. Fill $$p1 task 0 (limit=4), try 5
          r-p1-t0 (try-open-n p1-a k0 5)
          ;; 2. While $$p1 task 0 is full, try $$p1 task 1 (limit=4)
          r-p1-t1 (try-open-n p1-a k1 5)
          ;; 3. While $$p1 task 0 is full, try $$p2 task 0 (same task 0, limit=6)
          r-p2-t0 (try-open-n p2 k0 7)
          ;; 4. While ModA $$p1 is full, try ModB $$p3 task 0 (limit=4)
          r-p3-t0 (try-open-n p3 k0 5)]
      (close-all! (:proxies r-p1-t0))
      (close-all! (:proxies r-p1-t1))
      (close-all! (:proxies r-p2-t0))
      (close-all! (:proxies r-p3-t0))
      ;; 5. After closing all on $$p1 task 0, open 2 on handle A and try 3 on handle B
      (let [r-c1 (try-open-n p1-a k0 2)
            r-c2 (try-open-n p1-b k0 3)]
        (close-all! (:proxies r-c1))
        (close-all! (:proxies r-c2))
        (prn {:p1-task0-limit4               (dissoc r-p1-t0 :proxies)
              :p1-task1-while-t0-full        (dissoc r-p1-t1 :proxies)
              :p2-task0-while-p1-t0-full     (dissoc r-p2-t0 :proxies)
              :modB-p3-task0-while-modA-full (dissoc r-p3-t0 :proxies)
              :two-handles-2-then-3-on-limit4 {:c1 (dissoc r-c1 :proxies)
                                               :c2 (dissoc r-c2 :proxies)}})))
    (shutdown-agents)))
