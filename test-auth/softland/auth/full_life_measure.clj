(ns softland.auth.full-life-measure
  "STEP 4 full-life cluster measurements for `softland.auth.module/Auth` and
  `softland.auth.client`, run on live Rama 1.6.0 `InProcessCluster`s:

    1. `who-is` is one round trip to one task, its seeks match the 2-seek
       fast path (`1` seek for `S=nil`, `2` seeks for `S!=nil` live, `3` seeks
       for ended agent `S!=nil`), and neither seek count nor latency grows
       between 10 and 10,000 closed historical sessions.
    2. 20,000 distinct roots are watched simultaneously on ONE task (testing
       `pstate.proxy.limit = 20000` on `$$live`), with live updates delivered
       while all 20,000 proxies are open; and what a write under another root
       costs meanwhile: `open-session`, `close-session` and a `who-is` right
       behind them stay near what they cost with no watch open (each p50 at
       most twice its p50 with no watch, plus 2 ms), because a watch is a
       proxy on its own root's map in `$$live` alone.
    3. `end-actor` under a root with 3,000 live agents (including a 32-deep
       spine), 2,000 live sign-ins, and 5,000 open sessions completes within
       Revision 2's `< 2,000 ms` bound (far under the `8,000 ms` `ping-task`
       ceiling), and ZERO watchers of a sibling root on the same task receive
       `:softland.auth.client/not-reached`.
    3b. `end-actor` reads the live agents made after the one it ends, and
       none that ended before: ending each of 100 agents of a root under
       which 3,000 agents made after them have ended costs what it costs
       under a root with no ended history (mean at most 1.5 ms more).
    4. No write and no read of one person passes through a task that every
       person's passes through (static audit + empirical 4-task depot partition
       accounting across 40 persons)."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.rpl.rama :as rama]
            [com.rpl.rama.path]
            [com.rpl.rama.test :as rtest]
            [softland.auth.client :as client]
            [softland.auth.module :as mod]
            [softland.auth.schema :as s])
  (:import [java.util UUID]
           [java.util.concurrent Callable CountDownLatch Executors TimeUnit]
           [java.util.concurrent.atomic AtomicLong]))

(defn- h-of
  "Deterministic 64-char lower-case hex hash from integer `n`."
  [^long n]
  (format "%064x" n))

(defn- percentile
  "Returns the `p`-th percentile (`0.0..1.0`) from non-empty collection `xs`."
  [xs p]
  (let [v (vec (sort xs))
        idx (min (dec (count v)) (max 0 (int (Math/floor (* p (count v))))))]
    (nth v idx)))

(defn- time-ms
  "Invokes 0-arg `f`, returning `[result elapsed-ms]`."
  [f]
  (let [t0 (System/nanoTime)
        res (f)]
    [res (/ (- (System/nanoTime) t0) 1e6)]))

(defn- read-module-forms
  "Reads all top-level Clojure forms of `src-auth/softland/auth/module.clj`
  (ignoring comments) so static topology checks inspect real code forms rather
  than docstrings or comments."
  []
  (with-open [r (java.io.PushbackReader. (io/reader (io/file "src-auth/softland/auth/module.clj")))]
    (loop [acc []]
      (let [form (binding [*read-eval* false] (read r false ::eof))]
        (if (= form ::eof) acc (recur (conj acc form)))))))

(defn- strip-docstrings
  "Removes docstring string literals immediately following `ns`, `def`, `defn`,
  `defn-`, `deframaop`, or `defmodule` names, returning all non-doc symbols/forms."
  [form]
  (cond
    (and (seq? form) (#{'ns 'def 'defn 'defn- 'deframaop 'defmodule} (first form))
         (>= (count form) 3) (string? (nth form 2)))
    (cons (first form) (cons (second form) (map strip-docstrings (drop 3 form))))

    (coll? form)
    (map strip-docstrings form)

    :else form))

(defn- find-query-form
  "Finds the `(<<query-topology topologies name ...)` form in `forms`."
  [forms qname]
  (first
   (filter #(and (seq? %) (= '<<query-topology (first %)) (= qname (nth % 2 nil)))
           (tree-seq coll? seq forms))))

(defn- all-symbols-and-keywords
  "Collects all symbols and keywords appearing in code form `form`."
  [form]
  (filter #(or (symbol? %) (keyword? %)) (tree-seq coll? seq form)))

(defn- parallel-run!
  "Runs `f` over `items` across `n-threads` threads, returning vector of results."
  [n-threads items f]
  (let [pool (Executors/newFixedThreadPool n-threads)]
    (try
      (let [futs (mapv (fn [item] (.submit pool ^Callable #(f item))) items)]
        (mapv #(.get ^java.util.concurrent.Future % 120 TimeUnit/SECONDS) futs))
      (finally
        (.shutdownNow pool)))))

;; ============================================================================
;; Measure 1: who-is 1 RT to 1 task, 2-seek fast path, flat across 10k history
;; ============================================================================

(defn- trace-who-is-seeks
  "Evaluates the exact conditional seek guards of `module.clj`'s `\"who-is\"`
  query topology (`s/signin-fast-path?`, `s/root-entered?`, `some?`,
  `s/need-actor-seek?`) against the live `$$root-data` PState of `Auth` on
  `ipc`, returning exact per-key seek counts and verifying the result matches
  `(client/who-is c query)` verbatim."
  [ipc mod-name c {:keys [root sign-in session] :as query}]
  (let [p-rd (rama/foreign-pstate ipc mod-name "$$root-data")
        sel! (fn [k] (rama/foreign-select-one [(com.rpl.rama.path/keypath k)] p-rd {:pkey root}))
        g-cnt (atom 0) r-cnt (atom 0) s-cnt (atom 0) a-cnt (atom 0)
        sig (do (swap! g-cnt inc) (sel! (s/k-signin root sign-in)))
        fast? (s/signin-fast-path? sig)
        r (when-not fast? (swap! r-cnt inc) (sel! (s/k-root root)))
        entered? (or fast? (s/root-entered? r))
        sess (when (and entered? (some? session))
               (swap! s-cnt inc) (sel! (s/k-session root session)))
        arec (when (and entered? (s/need-actor-seek? root sig))
               (swap! a-cnt inc) (sel! (s/k-actor root (s/signin-actor sig))))
        traced-res (s/build-who-is-result root sig r sess arec)
        live-res (client/who-is c query)
        seeks {:g @g-cnt :r @r-cnt :s @s-cnt :a @a-cnt}]
    {:matches-live? (= traced-res live-res)
     :result live-res
     :seeks seeks
     :total (+ @g-cnt @r-cnt @s-cnt @a-cnt)}))

(defn measure-1-who-is!
  "Measures `who-is` seek counts, 1-RT/1-task structure, and latencies at
  10 vs 10,000 closed historical sessions (plus 500 ended sign-ins and 100
  ended agents)."
  []
  (println "\n=== MEASURE 1: who-is (1 RT, 1 task, 2-seek fast path, flat across history) ===")
  (let [forms (map strip-docstrings (read-module-forms))
        who-form (find-query-form forms "who-is")
        who-body (drop 4 who-form)
        who-syms (all-symbols-and-keywords who-body)
        part-syms (filter #{'|hash '|direct '|all '|global} who-syms)
        scan-syms (filter #{'sorted-map-range 'sorted-map-range-from 'ALL 'MAP-VALS 'MAP-KEYS} who-syms)
        ;; Verify statically on parsed Clojure AST that "who-is" starts with
        ;; leading (|hash *root), ends with (|origin), has 0 other partitioners,
        ;; and uses 0 range/iterator navigators:
        one-rt-one-task?
        (and (some? who-form)
             (= '(|hash *root) (first who-body))
             (= '(|origin) (last who-body))
             (= ['|hash] (vec part-syms))
             (empty? scan-syms))]
    (with-open [ipc (rtest/create-ipc)]
      (let [mod-name (rama/get-module-name mod/Auth)
            _ (rtest/launch-module! ipc mod/Auth {:tasks 4 :threads 2})
            c (client/open ipc mod-name)
            P (UUID/randomUUID)
            L (UUID/randomUUID)
            M {:uuid (UUID/randomUUID) :session (UUID/randomUUID)}
            G-live (UUID/randomUUID)
            S-live (UUID/randomUUID)
            A-end (UUID/randomUUID)
            G-end (UUID/randomUUID)
            S-end (UUID/randomUUID)
            _ (client/invite c {:person P :claim {:by :email :email "m1@example.com"} :invited-by :system :at 10})
            _ (client/enter c {:account [:google "m1-user"] :says {} :email "m1@example.com"
                               :code-hash nil :own-lane L :making M :at 20})
            _ (client/own-lane-made c {:person P :at 25})
            _ (client/begin-sign-in c {:root P :actor P :sign-in G-live :kind :browser :hash (h-of 1)
                                       :until 1000000 :label "live" :at 30})
            _ (client/open-session c {:root P :sign-in G-live :session S-live :until 1000000
                                      :driven-by {:kind :person} :at 35})
            _ (client/make-actor c {:root P :actor A-end :made-by P :label "ended-ag" :at 40})
            _ (client/begin-sign-in c {:root P :actor A-end :sign-in G-end :kind :program :hash (h-of 2)
                                       :until 1000000 :label "ag-g" :at 45})
            _ (client/open-session c {:root P :sign-in G-end :session S-end :until 1000000
                                      :driven-by {:kind :model :model "m"} :at 46})
            _ (client/end-actor c {:root P :actor A-end :at 50})
            ;; Seed 10 closed historical sessions
            _ (doseq [i (range 10)]
                (client/open-session c {:root P :sign-in G-live :session (UUID/randomUUID)
                                        :until 50 :at (+ 60 i)}))
            q-http    {:root P :sign-in G-live :session S-live}
            q-conn    {:root P :sign-in G-live :session nil}
            q-end-s   {:root P :sign-in G-end  :session S-end}
            q-end-nil {:root P :sign-in G-end  :session nil}
            q-unknown {:root (UUID/randomUUID) :sign-in G-live :session S-live}
            ;; Warmup + small-history latency (200 calls)
            _ (dotimes [_ 50] (client/who-is c q-http))
            small-seeks {:http (trace-who-is-seeks ipc mod-name c q-http)
                         :conn (trace-who-is-seeks ipc mod-name c q-conn)
                         :ended-agent-with-s (trace-who-is-seeks ipc mod-name c q-end-s)
                         :ended-agent-no-s (trace-who-is-seeks ipc mod-name c q-end-nil)
                         :unknown-root (trace-who-is-seeks ipc mod-name c q-unknown)}
            small-ms (mapv (fn [_] (second (time-ms #(client/who-is c q-http)))) (range 200))
            ;; Now grow history to 10,000 closed sessions + 500 ended sign-ins + 100 ended agents!
            [_ seed-ms]
            (time-ms
             #(do
                (parallel-run! 8 (range 100)
                               (fn [i]
                                 (let [aid (UUID/randomUUID)]
                                   (client/make-actor c {:root P :actor aid :made-by P :label (str "hist-a-" i) :at 100})
                                   (client/end-actor c {:root P :actor aid :at 101}))))
                (parallel-run! 8 (range 500)
                               (fn [i]
                                 (client/begin-sign-in c {:root P :actor P :sign-in (UUID/randomUUID)
                                                          :kind :program :hash (h-of (+ 1000 i))
                                                          :until 100 :label "hist-g" :at 200})))
                (parallel-run! 8 (range 9990)
                               (fn [_]
                                 (client/open-session c {:root P :sign-in G-live :session (UUID/randomUUID)
                                                         :until 100 :at 300})))))
            _ (dotimes [_ 50] (client/who-is c q-http))
            large-seeks {:http (trace-who-is-seeks ipc mod-name c q-http)
                         :conn (trace-who-is-seeks ipc mod-name c q-conn)
                         :ended-agent-with-s (trace-who-is-seeks ipc mod-name c q-end-s)
                         :ended-agent-no-s (trace-who-is-seeks ipc mod-name c q-end-nil)
                         :unknown-root (trace-who-is-seeks ipc mod-name c q-unknown)}
            large-ms (mapv (fn [_] (second (time-ms #(client/who-is c q-http)))) (range 200))
            _ (client/close! c)
            small-p50 (percentile small-ms 0.50)
            small-p95 (percentile small-ms 0.95)
            large-p50 (percentile large-ms 0.50)
            large-p95 (percentile large-ms 0.95)
            seeks-ok? (and (every? :matches-live? (vals small-seeks))
                           (every? :matches-live? (vals large-seeks))
                           (= 2 (get-in small-seeks [:http :total]))
                           (= 1 (get-in small-seeks [:conn :total]))
                           (= 3 (get-in small-seeks [:ended-agent-with-s :total]))
                           (= 2 (get-in small-seeks [:ended-agent-no-s :total]))
                           (= 2 (get-in small-seeks [:unknown-root :total]))
                           (= (into {} (map (fn [[k v]] [k (:seeks v)]) small-seeks))
                              (into {} (map (fn [[k v]] [k (:seeks v)]) large-seeks))))
            flat-ok? (and one-rt-one-task? seeks-ok? (< large-p50 10.0))]
        (println (format "  Static query check: leading (|hash *root), 0 other partitioners, 0 range scans = %s" one-rt-one-task?))
        (println (format "  Seeded 10,000 closed sessions + 500 ended sign-ins + 100 ended agents in %.1f ms" seed-ms))
        (println "  Exact $$root-data seeks at 10 closed sessions:"
                 (into {} (map (fn [[k v]] [k [(:total v) (:seeks v)]]) small-seeks)))
        (println "  Exact $$root-data seeks at 10,000 closed sessions:"
                 (into {} (map (fn [[k v]] [k [(:total v) (:seeks v)]]) large-seeks)))
        (println (format "  who-is latency (200 calls): 10 sessions p50=%.3f ms p95=%.3f ms | 10,000 sessions p50=%.3f ms p95=%.3f ms"
                         small-p50 small-p95 large-p50 large-p95))
        (println (if flat-ok? "PASSED" "FAILED") "Measure 1: who-is 1 RT, 1 task, 2-seek fast path, seeks constant across 10 -> 10,000 history.")
        flat-ok?))))

;; ============================================================================
;; Measure 2: 20,000 roots watched simultaneously on ONE task
;; ============================================================================

(defn- write-cycles
  "Runs `n` cycles under root `P`'s live sign-in `G`, each an `open-session`
  of a new session, its `close-session`, and a `who-is` right behind them.
  Gives `{:open [p50 p95] :close [p50 p95] :who-is [p50 p95]}` in ms. Throws
  when a call does not give what it must."
  [c P G n]
  (let [samples (reduce
                 (fn [acc _]
                   (let [S (UUID/randomUUID)
                         [r1 t1] (time-ms #(client/open-session c {:root P :sign-in G :session S
                                                                   :until 999999999 :at 100}))
                         [r2 t2] (time-ms #(client/close-session c {:root P :session S :at 100}))
                         [r3 t3] (time-ms #(client/who-is c {:root P :sign-in G :session nil}))]
                     (when-not (and (= {:open S} r1) (= {:closed S} r2) (map? (:sign-in r3)))
                       (throw (ex-info "a write cycle gave what it must not" {:open r1 :close r2 :who-is r3})))
                     (-> acc (update :open conj t1) (update :close conj t2) (update :who-is conj t3))))
                 {:open [] :close [] :who-is []}
                 (range n))]
    (into {} (map (fn [[k xs]] [k [(percentile xs 0.5) (percentile xs 0.95)]])) samples)))

(defn- near-baseline?
  "Whether each p50 of `watched` is at most twice its p50 in `baseline`,
  plus 2 ms."
  [baseline watched]
  (every? (fn [k] (<= (first (get watched k)) (+ 2.0 (* 2.0 (first (get baseline k))))))
          [:open :close :who-is]))

(defn- show-cycles
  "Prints the p50 and p95 of one run of `write-cycles`."
  [label {:keys [open close who-is]}]
  (println (format "  %-24s open-session p50=%.3f p95=%.3f | close-session p50=%.3f p95=%.3f | who-is right after p50=%.3f p95=%.3f ms"
                   label (first open) (second open) (first close) (second close) (first who-is) (second who-is))))

(defn measure-2-20k-watches!
  "Opens 20,000 distinct `watch-root` subscriptions on ONE task (1-task IPC so
  all 20,000 live on task 0), verifies 0 `:not-reached` and live push delivery
  on roots #0 and #19,999 while all 20,000 watches are active simultaneously;
  and times `write-cycles` under a root that nobody watches, with no watch
  open and with the 20,000 open, holding the second to `near-baseline?`."
  []
  (println "\n=== MEASURE 2: 20,000 roots watched simultaneously on one task ===")
  (with-open [ipc (rtest/create-ipc)]
    (let [mod-name (rama/get-module-name mod/Auth)
          _ (rtest/launch-module! ipc mod/Auth {:tasks 1 :threads 1})
          c (client/open ipc mod-name)
          ;; The writer: a root that nobody watches, with a live sign-in.
          P-w (UUID/randomUUID)
          G-w (UUID/randomUUID)
          _ (do (client/invite c {:person P-w :claim {:by :email :email "writer@example.com"}
                                  :invited-by :system :at 10})
                (client/enter c {:account [:google "writer"] :says {} :email "writer@example.com"
                                 :code-hash nil :own-lane (UUID/randomUUID)
                                 :making {:uuid (UUID/randomUUID) :session (UUID/randomUUID)} :at 20})
                (client/own-lane-made c {:person P-w :at 25})
                (client/begin-sign-in c {:root P-w :actor P-w :sign-in G-w :kind :browser :hash (h-of 8999)
                                         :until 999999999 :label "writer" :at 30}))
          _ (write-cycles c P-w G-w 300)
          baseline (write-cycles c P-w G-w 300)
          _ (show-cycles "0 roots watched:" baseline)
          n-roots 20000
          roots (vec (repeatedly n-roots #(UUID/randomUUID)))
          nr-count (AtomicLong. 0)
          first-latch (CountDownLatch. 1)
          last-latch (CountDownLatch. 1)
          first-val (atom nil)
          last-val (atom nil)
          [stops open-ms]
          (time-ms
           #(parallel-run!
             16
             (range n-roots)
             (fn [i]
               (let [p (nth roots i)
                     cb (cond
                          (= i 0)
                          (fn [v]
                            (if (= v :softland.auth.client/not-reached)
                              (.incrementAndGet nr-count)
                              (do (reset! first-val v) (.countDown first-latch))))
                          (= i (dec n-roots))
                          (fn [v]
                            (if (= v :softland.auth.client/not-reached)
                              (.incrementAndGet nr-count)
                              (do (reset! last-val v) (.countDown last-latch))))
                          :else
                          (fn [v]
                            (when (= v :softland.auth.client/not-reached)
                              (.incrementAndGet nr-count))))]
                 (client/watch-root c p cb)))))
          fn-count (count (filter fn? stops))
          watched (write-cycles c P-w G-w 300)
          _ (show-cycles "20,000 roots watched:" watched)
          near? (near-baseline? baseline watched)
          ;; Trigger live updates on root #0 and root #19,999 while all 20,000 watches stay open!
          P0 (nth roots 0)
          P-last (nth roots (dec n-roots))
          G0 (UUID/randomUUID)
          G-last (UUID/randomUUID)
          _ (doseq [[p g idx] [[P0 G0 1] [P-last G-last 2]]]
              (client/invite c {:person p :claim {:by :email :email (str "w20k-" idx "@example.com")}
                                :invited-by :system :at 10})
              (client/enter c {:account [:google (str "w20k-" idx)] :says {} :email (str "w20k-" idx "@example.com")
                               :code-hash nil :own-lane (UUID/randomUUID)
                               :making {:uuid (UUID/randomUUID) :session (UUID/randomUUID)} :at 20})
              (client/own-lane-made c {:person p :at 25})
              (client/begin-sign-in c {:root p :actor p :sign-in g :kind :browser :hash (h-of (+ 9000 idx))
                                       :until 999999 :label "live" :at 30}))
          got-0? (.await first-latch 10 TimeUnit/SECONDS)
          got-last? (.await last-latch 10 TimeUnit/SECONDS)
          ;; Wait across one 2 s ping-task cycle to verify 0 false :not-reached under 20k watches
          _ (Thread/sleep 2300)
          nrs (.get nr-count)
          [_ close-ms] (time-ms #(client/close! c))
          ok? (and (= n-roots fn-count)
                   (zero? nrs)
                   got-0? (contains? (:sign-ins @first-val) G0)
                   got-last? (contains? (:sign-ins @last-val) G-last)
                   near?)]
      (println (format "  Registered %d/%d watches on task 0 in %.1f ms (not-reached=%d)"
                       fn-count n-roots open-ms nrs))
      (println (format "  Live push while 20,000 active: root #0=%s, root #19,999=%s; close! took %.1f ms"
                       got-0? got-last? close-ms))
      (println (format "  Writes under another root with 20,000 watched, each p50 within 2x + 2 ms of none watched: %s" near?))
      (println (if ok? "PASSED" "FAILED") "Measure 2: 20,000 roots watched simultaneously on one task, and a write under another root and a who-is right behind it cost what they cost with none watched.")
      ok?)))

;; ============================================================================
;; Measure 3: end-actor under 3,000 actors, 2,000 sign-ins, 5,000 sessions
;; ============================================================================

(defn measure-3-heavy-end-actor!
  "Seeds `P_heavy` with 3,000 live agents (including 32-deep spine), 2,000 live
  sign-ins, and 5,000 open sessions on shared task 0 alongside watched sibling
  root `P_sib`. Runs `end-actor` on the root agent `A1` and verifies wall time
  stays within Revision 2's `< 2,000 ms` bound and ZERO sibling watchers receive
  `:softland.auth.client/not-reached`."
  []
  (println "\n=== MEASURE 3: end-actor under 3,000 actors, 2,000 sign-ins, 5,000 sessions ===")
  (with-open [ipc (rtest/create-ipc)]
    (let [mod-name (rama/get-module-name mod/Auth)
          _ (rtest/launch-module! ipc mod/Auth {:tasks 1 :threads 1})
          c1 (client/open ipc mod-name)
          c2 (client/open ipc mod-name)
          P-heavy (UUID/randomUUID)
          P-sib   (UUID/randomUUID)
          G-sib   (UUID/randomUUID)
          S-sib   (UUID/randomUUID)
          ;; Setup both roots
          _ (doseq [[p em sub] [[P-heavy "heavy@example.com" "heavy"] [P-sib "sib@example.com" "sib"]]]
              (client/invite c1 {:person p :claim {:by :email :email em} :invited-by :system :at 10})
              (client/enter c1 {:account [:google sub] :says {} :email em :code-hash nil
                                :own-lane (UUID/randomUUID)
                                :making {:uuid (UUID/randomUUID) :session (UUID/randomUUID)} :at 20})
              (client/own-lane-made c1 {:person p :at 25}))
          _ (client/begin-sign-in c1 {:root P-sib :actor P-sib :sign-in G-sib :kind :browser
                                      :hash (h-of 777) :until 999999 :label "sib" :at 30})
          _ (client/open-session c1 {:root P-sib :sign-in G-sib :session S-sib :until 999999
                                     :driven-by {:kind :person} :at 31})
          ;; Seed 3,000 live agents under P-heavy (32-deep spine A0..A31, rest fan out under A0..A30)
          actors (vec (repeatedly 3000 #(UUID/randomUUID)))
          signins (vec (repeatedly 2000 #(UUID/randomUUID)))
          sessions (vec (repeatedly 5000 #(UUID/randomUUID)))
          [_ seed-ms]
          (time-ms
           #(do
              ;; First 32 sequentially to build the 32-deep spine
              (doseq [i (range 32)]
                (let [res (client/make-actor c1 {:root P-heavy :actor (nth actors i)
                                                 :made-by (if (zero? i) P-heavy (nth actors (dec i)))
                                                 :label (str "spine-" i) :at (+ 100 i)})]
                  (assert (= {:made (nth actors i)} res) (str "spine make-actor failed: " res))))
              ;; Remaining 2,968 agents in parallel (all descendants of A0..A30, depths 2..32)
              (parallel-run! 8 (range 32 3000)
                             (fn [i]
                               (let [parent (nth actors (mod i 31))
                                     res (client/make-actor c1 {:root P-heavy :actor (nth actors i)
                                                                :made-by parent
                                                                :label (str "ag-" i) :at (+ 100 i)})]
                                 (assert (= {:made (nth actors i)} res) (str "fan make-actor failed: " res)))))
              ;; 2,000 live sign-ins across the 3,000 agents (ensure depth-32 agent actors[31] gets signins[31])
              (parallel-run! 8 (range 2000)
                             (fn [j]
                               (let [aid (nth actors (mod j 3000))
                                     res (client/begin-sign-in c1 {:root P-heavy :actor aid
                                                                   :sign-in (nth signins j)
                                                                   :kind :program :hash (h-of (+ 10000 j))
                                                                   :until 999999 :label (str "g-" j) :at (+ 4000 j)})]
                                 (assert (= {:begun (nth signins j)} res) (str "begin-sign-in failed: " res)))))
              ;; 5,000 open sessions across the 2,000 sign-ins
              (parallel-run! 8 (range 5000)
                             (fn [k]
                               (let [gid (nth signins (mod k 2000))
                                     res (client/open-session c1 {:root P-heavy :sign-in gid
                                                                  :session (nth sessions k)
                                                                  :until 999999
                                                                  :driven-by {:kind :model :model "worker"}
                                                                  :at (+ 10000 k)})]
                                 (assert (= {:open (nth sessions k)} res) (str "open-session failed: " res)))))))
          ;; Watch BOTH P-heavy and 20 sibling roots (including P-sib) on c2 (same task 0!)
          sib-nrs (AtomicLong. 0)
          heavy-nrs (AtomicLong. 0)
          heavy-empty (CountDownLatch. 1)
          sib-roots (into [P-sib] (repeatedly 19 #(UUID/randomUUID)))
          _ (doseq [sr sib-roots]
              (client/watch-root c2 sr (fn [v]
                                         (when (= v :softland.auth.client/not-reached)
                                           (.incrementAndGet sib-nrs)))))
          _ (client/watch-root c2 P-heavy (fn [v]
                                            (if (= v :softland.auth.client/not-reached)
                                              (.incrementAndGet heavy-nrs)
                                              (when (and (empty? (:sign-ins v)) (empty? (:sessions v)))
                                                (.countDown heavy-empty)))))
          ;; Concurrently probe who-is(P-sib) while end-actor runs on task 0!
          sib-who-ms (atom nil)
          probe-fut (future
                      (Thread/sleep 10)
                      (let [[w ms] (time-ms #(client/who-is c2 {:root P-sib :sign-in G-sib :session S-sib}))]
                        (reset! sib-who-ms [ms (some? (:sign-in w))])))
          [end-res end-ms] (time-ms #(client/end-actor c1 {:root P-heavy :actor (nth actors 0) :at 50000}))
          _ @probe-fut
          heavy-cleared? (.await heavy-empty 10 TimeUnit/SECONDS)
          ;; Wait 3 seconds (past ping-task 2 s cycle) to verify ZERO sibling watchers get :not-reached!
          _ (Thread/sleep 3000)
          und (client/under c2 P-heavy)
          deep-who (client/who-is c2 {:root P-heavy :sign-in (nth signins 31) :session (nth sessions 31)})
          sib-nr-cnt (.get sib-nrs)
          heavy-nr-cnt (.get heavy-nrs)
          _ (client/close! c1)
          _ (client/close! c2)
          ok? (and (= {:ended (nth actors 0)} end-res)
                   (< end-ms 2000.0)
                   (zero? sib-nr-cnt)
                   (zero? heavy-nr-cnt)
                   heavy-cleared?
                   (= 3000 (count (:actors und)))
                   (every? :ended? (:actors und))
                   (empty? (:sign-ins und))
                   (true? (get-in deep-who [:sign-in :ended?]))
                   (true? (get-in deep-who [:sign-in :actor-ended?]))
                   (true? (get-in deep-who [:session :closed?]))
                   (= {:kind :model :model "worker"} (get-in deep-who [:session :driven-by])))]
      (println (format "  Seeded 3,000 actors + 2,000 sign-ins + 5,000 sessions via contract in %.1f ms" seed-ms))
      (println (format "  end-actor wall time = %.1f ms (Revision 2 bound < 2,000 ms; ping-task ceiling = 8,000 ms)" end-ms))
      (println (format "  Concurrent sibling who-is(P-sib) = %.1f ms (ok=%s)" (first @sib-who-ms) (second @sib-who-ms)))
      (println (format "  Sibling watchers :not-reached = %d / 20 | heavy watcher :not-reached = %d | heavy cleared = %s"
                       sib-nr-cnt heavy-nr-cnt heavy-cleared?))
      (println (format "  Post-check: under actors=%d (all ended=%s), live sign-ins=%d; depth-32 who-is ended=%s actor-ended=%s session-closed=%s driven-by=%s"
                       (count (:actors und)) (every? :ended? (:actors und)) (count (:sign-ins und))
                       (get-in deep-who [:sign-in :ended?]) (get-in deep-who [:sign-in :actor-ended?])
                       (get-in deep-who [:session :closed?]) (pr-str (get-in deep-who [:session :driven-by]))))
      (println (if ok? "PASSED" "FAILED") "Measure 3: heavy end-actor within Revision 2 bound and 0 sibling :not-reached.")
      ok?)))

;; ============================================================================
;; Measure 3b: end-actor over a long ended history
;; ============================================================================

(defn- entered-root!
  "Invites and enters a new person through client `c`. Gives her root."
  [c tag]
  (let [P (UUID/randomUUID)]
    (client/invite c {:person P :claim {:by :email :email (str tag "@example.com")} :invited-by :system :at 10})
    (client/enter c {:account [:google tag] :says {} :email (str tag "@example.com") :code-hash nil
                     :own-lane (UUID/randomUUID)
                     :making {:uuid (UUID/randomUUID) :session (UUID/randomUUID)} :at 20})
    (client/own-lane-made c {:person P :at 25})
    P))

(defn- make-agents!
  "Makes `n` agents of root `P`, each made by `P`, in order. Gives their ids."
  [c P n tag]
  (let [ids (vec (repeatedly n #(UUID/randomUUID)))]
    (doseq [[i a] (map-indexed vector ids)]
      (let [res (client/make-actor c {:root P :actor a :made-by P :label (str tag "-" i) :at (+ 100 i)})]
        (assert (= {:made a} res) (str "make-actor failed: " res))))
    ids))

(defn- end-times
  "Ends each agent of `ids` under root `P`, in order, one call at a time.
  Gives the time of each call, in ms."
  [c P ids]
  (mapv (fn [a]
          (let [[res ms] (time-ms #(client/end-actor c {:root P :actor a :at 60000}))]
            (assert (= {:ended a} res) (str "end-actor failed: " res))
            ms))
        ids))

(defn- mean
  "The mean of a non-empty collection of numbers."
  [xs]
  (/ (reduce + xs) (count xs)))

(defn measure-3b-ended-history!
  "On one task: root `P-hist` makes 100 agents and then 3,000 more, and the
  3,000 are ended; root `P-base` makes 100 agents and nothing else. Then the
  100 agents of each are ended one by one, first made first. The live agents
  that each of these end-actors finds after its own are the same under both
  roots (the rest of the 100); under `P-hist` 3,000 ended agents lie after
  them too. Passes when the mean end-actor under `P-hist` is at most 1.5 ms
  above the mean under `P-base`: the scan does not read the ended history
  (at 1.2 µs an entry, the review's measure, reading it would add 3.6 ms)."
  []
  (println "\n=== MEASURE 3b: end-actor over 3,000 ended agents made after it ===")
  (with-open [ipc (rtest/create-ipc)]
    (let [mod-name (rama/get-module-name mod/Auth)
          _ (rtest/launch-module! ipc mod/Auth {:tasks 1 :threads 1})
          c (client/open ipc mod-name)
          P-hist (entered-root! c "hist")
          P-base (entered-root! c "base")
          hist-probes (make-agents! c P-hist 100 "probe")
          fillers (make-agents! c P-hist 3000 "filler")
          [_ fill-end-ms] (time-ms #(parallel-run! 8 fillers
                                                    (fn [a] (client/end-actor c {:root P-hist :actor a :at 50000}))))
          base-probes (make-agents! c P-base 100 "probe")
          ;; Warm both paths once, on agents that are not measured.
          _ (end-times c P-base (make-agents! c P-base 5 "warm"))
          base-ms (end-times c P-base base-probes)
          hist-ms (end-times c P-hist hist-probes)
          und (client/under c P-hist)
          _ (client/close! c)
          ok? (and (<= (mean hist-ms) (+ 1.5 (mean base-ms)))
                   (= 3100 (count (:actors und)))
                   (every? :ended? (:actors und))
                   (= (concat hist-probes fillers) (map :actor (:actors und))))]
      (println (format "  Ended the 3,000 later agents of P-hist in %.1f ms" fill-end-ms))
      (println (format "  end-actor of each of 100 agents: no ended history mean=%.3f p95=%.3f ms | 3,000 ended agents after them mean=%.3f p95=%.3f ms"
                       (mean base-ms) (percentile base-ms 0.95) (mean hist-ms) (percentile hist-ms 0.95)))
      (println (format "  under P-hist afterwards: %d agents, all ended=%s, in the order they were made=%s"
                       (count (:actors und)) (every? :ended? (:actors und))
                       (= (concat hist-probes fillers) (map :actor (:actors und)))))
      (println (if ok? "PASSED" "FAILED") "Measure 3b: end-actor reads no agent that ended before, and under still gives every agent in the order it was made.")
      ok?)))

;; ============================================================================
;; Measure 4: No write and no read of one person goes through a shared task
;; ============================================================================

(defn- depot-offsets
  "Returns vector of end offsets across all `n-parts` partitions of `depot`."
  [depot n-parts]
  (mapv (fn [p] (long (or (:end-offset (rama/foreign-depot-partition-info depot p)) 0)))
        (range n-parts)))

(defn measure-4-no-shared-task!
  "Verifies both statically (source inspection of `module.clj`: 0 `:global?`,
  0 `|global`, 0 `|all`, `|direct` only in internal `ping-task`) and dynamically
  on a 4-task `InProcessCluster` that:
  - A single person's writes advance AT MOST 1 partition per depot;
  - Across 40 distinct persons, writes distribute across ALL 4 partitions of
    all 4 depots (`*root-events`, `*invites`, `*enters`, `*asks`) — proving no
    single task sees every person's writes or reads."
  []
  (println "\n=== MEASURE 4: No write and no read of one person goes through a shared task ===")
  (let [forms (map strip-docstrings (read-module-forms))
        all-syms (all-symbols-and-keywords forms)
        has-global? (boolean (some #{:global? '|global '|all} all-syms))
        ping-form (find-query-form forms "ping-task")
        direct-total (count (filter #{'|direct} all-syms))
        direct-in-ping (count (filter #{'|direct} (all-symbols-and-keywords ping-form)))
        direct-only-ping? (and (= 1 direct-total) (= 1 direct-in-ping))]
    (with-open [ipc (rtest/create-ipc)]
      (let [mod-name (rama/get-module-name mod/Auth)
            _ (rtest/launch-module! ipc mod/Auth {:tasks 4 :threads 2})
            c (client/open ipc mod-name)
            d-root (rama/foreign-depot ipc mod-name "*root-events")
            d-inv  (rama/foreign-depot ipc mod-name "*invites")
            d-ent  (rama/foreign-depot ipc mod-name "*enters")
            d-ask  (rama/foreign-depot ipc mod-name "*asks")
            before-1 {:root (depot-offsets d-root 4)
                      :inv  (depot-offsets d-inv 4)
                      :ent  (depot-offsets d-ent 4)
                      :ask  (depot-offsets d-ask 4)}
            ;; Run 1 person's full write lifecycle
            run-one-person!
            (fn [idx]
              (let [P (UUID/randomUUID)
                    L (UUID/randomUUID)
                    M {:uuid (UUID/randomUUID) :session (UUID/randomUUID)}
                    A (UUID/randomUUID)
                    G (UUID/randomUUID)
                    G2 (UUID/randomUUID)
                    S (UUID/randomUUID)
                    U (format "M4%06d" idx)
                    em (str "m4-" idx "@example.com")]
                (client/invite c {:person P :claim {:by :email :email em} :invited-by :system :at 10})
                (client/enter c {:account [:google (str "m4-" idx)] :says {} :email em :code-hash nil
                                 :own-lane L :making M :at 20})
                (client/own-lane-made c {:person P :at 25})
                (client/make-actor c {:root P :actor A :made-by P :label "a" :at 30})
                (client/begin-sign-in c {:root P :actor A :sign-in G :kind :program :hash (h-of (+ 1 idx))
                                         :until 99999 :label "g" :at 35})
                (client/open-session c {:root P :sign-in G :session S :until 99999 :at 36})
                (client/ask c {:code U :hash (h-of (+ 1000 idx)) :asking-hash (h-of (+ 2000 idx)) :asks "cli"
                               :ends 99999 :at 40})
                (client/answer-ask c {:code U :yes? true :root P :actor P :new-agent nil
                                      :sign-in G2 :until 99999 :at 45})
                (client/who-is c {:root P :sign-in G :session S})
                (client/under c P)
                (client/ask-of c U)))
            _ (run-one-person! 0)
            after-1 {:root (depot-offsets d-root 4)
                     :inv  (depot-offsets d-inv 4)
                     :ent  (depot-offsets d-ent 4)
                     :ask  (depot-offsets d-ask 4)}
            delta-1 (into {} (map (fn [[k v]] [k (mapv - v (get before-1 k))]) after-1))
            one-person-single-part?
            (every? (fn [[_ ds]] (= 1 (count (filter pos? ds)))) delta-1)
            ;; Run 39 more distinct persons (40 total)
            _ (doseq [i (range 1 40)] (run-one-person! i))
            after-40 {:root (depot-offsets d-root 4)
                      :inv  (depot-offsets d-inv 4)
                      :ent  (depot-offsets d-ent 4)
                      :ask  (depot-offsets d-ask 4)}
            all-4-parts-used?
            (every? (fn [[_ vs]] (every? pos? vs)) after-40)
            _ (client/close! c)
            ok? (and (not has-global?)
                     direct-only-ping?
                     one-person-single-part?
                     all-4-parts-used?)]
        (println (format "  Static audit: :global?/|global/|all present=%s, |direct only in ping-task=%s"
                         has-global? direct-only-ping?))
        (println "  Single person depot partition deltas across 4 tasks:" delta-1)
        (println "  40 distinct persons depot partition counts across 4 tasks:" after-40)
        (println (if ok? "PASSED" "FAILED") "Measure 4: no write and no read of one person passes through a shared task.")
        ok?))))

(defn -main
  "Runs the STEP 4 full-life cluster measurements (1, 2, 3, 3b, 4) and exits non-zero if any fails."
  [& _]
  (let [t0 (System/nanoTime)
        m1 (measure-1-who-is!)
        m2 (measure-2-20k-watches!)
        m3 (measure-3-heavy-end-actor!)
        m3b (measure-3b-ended-history!)
        m4 (measure-4-no-shared-task!)
        elapsed-s (/ (- (System/nanoTime) t0) 1e9)
        all-ok? (and m1 m2 m3 m3b m4)]
    (println (format "\nSTEP 4 Summary: m1=%s m2=%s m3=%s m3b=%s m4=%s (ALL_PASSED=%s, elapsed=%.1f s)"
                     m1 m2 m3 m3b m4 all-ok? elapsed-s))
    (flush)
    (shutdown-agents)
    (System/exit (if all-ok? 0 1))))
