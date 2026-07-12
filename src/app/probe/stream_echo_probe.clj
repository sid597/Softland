(ns app.probe.stream-echo-probe
  "write-echo-2 spike (2026-07-12) — UNCOMMITTED probe, evidence not product.

   Prices the DIRECT-to-Rama editor echo at typing speed against a MINIMAL
   STREAM topology, the class D-013 pre-registered as the route to prove after
   the microbatch text-kernel FAILED the same criterion (build/write-echo/NOW.md).

   SAME pre-registered criterion, SAME method, SAME isolation discipline as
   write-echo; the ONLY variable changed is the topology CLASS under leg 2.

   Measured legs (identical decomposition to write-echo):
     (1) client emit -> depot append :append-ack        [emitter thread]
     (2) materialization: PState reflects the event      [poller thread]
     (3) stream back through Electric into a client atom  [NOT measured here:
         characterized separately in build/write-echo/NOW.md; leg 3 is a plain
         server atom mirror bumped AFTER materialization + e/watch transport,
         strictly additive to (1)+(2).]

   Legs 1+2 are the Rama half. For a STREAM topology :append-ack still returns
   on durability only (NOT processing) — so leg1 measures durability and the
   poller measures leg2 = stream processing + visibility. This is exactly
   write-echo's decomposition; direct comparability is the whole point.

   SECONDARY cross-check: a small closed-loop `:ack` round-trip (the REAL
   write-then-read-back call: foreign-append! ... :ack blocks until the stream
   event tree completes and the PState write is visible). If leg1+leg2 (polled)
   agrees with the :ack round-trip (measured in one call), the decomposition is
   trustworthy — a guard against a poll-resolution artifact.

   MINIMAL module, faithful to the text-kernel signal shape:
     - content-write path writes $$echo-revisions (the ~185-char block, a
       realistic-sized write) + $$echo-heads (the head pointer) in ONE event,
       mirroring the kernel's head+revision materialization. The ONE omission
       vs the real content path is line-unit re-derivation; write-echo bounded
       that cost as single-digit ms (microbatch content-leg2 211ms ~= status
       218ms => per-event application work is buried in ~7ms of noise).
     - status-write path writes one $$echo-statuses row (the smallest keyed
       write), mirroring $$unit-statuses.
   Per-key serialization is preserved: hash-by :routing/key = [:artifact art],
   the production text-kernel's exact routing scheme (zero partitioner hops, so
   every write for one artifact lands on that artifact's task in depot order).

   IMPORTANT CAVEAT baked into every number: this runs against in-process IPC
   (create-ipc), a TEST harness — the substrate Softland actually runs today.
   Treat these as the current real substrate's stream-class floor, not a
   production-cluster SLA.

   Run:  clojure -J-Xss16m -M -m app.probe.stream-echo-probe"
  (:use [com.rpl.rama]
        [com.rpl.rama.path]
        [com.rpl.rama.ops])
  (:require [com.rpl.rama.test :refer [create-ipc launch-module!]])
  (:import (clojure.lang Keyword)))

;; ── realistic single block: ~185 chars, 3 lines (prose-grade Softland block) ──
;; Verbatim from write-echo, so the content-write payload matches byte-for-byte.
(def base-block
  (str "The map must not lie: known, proposed, derived, guessed --\n"
       "visibly distinct, always. Provenance is soul, not plumbing.\n"
       "History is terrain; the log is primary, every view a projection."))

(def default-branch "main")

;; Routing key IS the text-kernel's: [:artifact <id>]. The depot hashes by
;; :routing/key and foreign reads pass {:pkey [:artifact art]} — same hash, same
;; task, so a write and its read-back colocate (verified against text_kernel.clj).
(defn routing-key [art] [:artifact art])

;; Topology-side materializers (plain defns run in operation position).
(defn revision-row
  "The realistic-sized content materialization: carries the full block, mirrors
   $$text-revisions. root-event-id = the revision token so the write is not a
   trivial one-field row."
  [req]
  {:revision/id (:revision/id req)
   :text/content (:text/content req)
   :content/hash (hash (:text/content req))
   :root-event-id (:revision/id req)})

;; ===========================================================================
;; The module — ONE depot, ONE stream topology, head/revision/status PStates.
;; ===========================================================================

(defmodule stream-echo-module [setup topologies]
  ;; hash-by :routing/key: every write for one artifact lands on that
  ;; artifact's task, so same-key events serialize in depot order and a
  ;; write is read-back-visible on its own task (rama-pitfalls §1, §10).
  (declare-depot setup *echo-requests-depot (hash-by :routing/key))
  (let [s (stream-topology topologies "stream-echo-topology")]
    (declare-pstate s $$echo-heads {String String})
    (declare-pstate s $$echo-revisions {String {String (map-schema Keyword Object)}})
    ;; artifact -> branch -> unit -> status row (mirrors $$unit-statuses shape).
    (declare-pstate s $$echo-statuses {String {String {String (map-schema Keyword Object)}}})
    (<<sources s
      (source> *echo-requests-depot :> *req)
      (get *req :op :> *op)
      (get *req :artifact/id :> *art)
      (<<cond
        (case> (= :head *op))
        ;; content-write: revision (realistic size) + head pointer, ONE event,
        ;; same task => atomic together; timing the head times the revision.
        (get *req :revision/id :> *rev)
        (revision-row *req :> *rev-row)
        (local-transform> [(keypath *art *rev) (termval *rev-row)] $$echo-revisions)
        (local-transform> [(keypath *art) (termval *rev)] $$echo-heads)
        (ack-return> *rev)

        (case> (= :status *op))
        ;; status-write: smallest keyed write — one status row.
        (get *req :branch/id :> *branch)
        (get *req :unit/id :> *unit)
        (get *req :status/row :> *row)
        (local-transform> [(keypath *art *branch *unit) (termval *row)] $$echo-statuses)
        (ack-return> *unit)

        ;; every harness record is :head or :status; this only guards against a
        ;; hang if an :ack appender ever sent something else.
        (default>)
        (ack-return> :ignored)))))

;; ===========================================================================
;; Runtime (mirrors tk/start-text-runtime! — same launch opts, own IPC).
;; ===========================================================================

(defn start-stream-echo-runtime!
  []
  (let [ipc (create-ipc)
        module-name (get-module-name stream-echo-module)
        launch-opts {:tasks 4 :threads 2}]
    (launch-module! ipc stream-echo-module launch-opts)
    {:ipc ipc
     :module-name module-name
     :echo-requests-depot (foreign-depot ipc module-name "*echo-requests-depot")
     :echo-heads (foreign-pstate ipc module-name "$$echo-heads")
     :echo-revisions (foreign-pstate ipc module-name "$$echo-revisions")
     :echo-statuses (foreign-pstate ipc module-name "$$echo-statuses")}))

(defn close-stream-echo-runtime!
  [runtime]
  (when-let [ipc (:ipc runtime)]
    (try (.close ^java.lang.AutoCloseable ipc)
         (catch Exception _ nil))))

;; ── request envelopes (client-minted; deterministic ids, tag-scoped) ─────────
(defn content-request
  [art rev-token content]
  {:op :head
   :routing/key (routing-key art)
   :artifact/id art
   :revision/id rev-token
   :text/content content})

(defn status-request
  [art unit-id status evt-token]
  {:op :status
   :routing/key (routing-key art)
   :artifact/id art
   :branch/id default-branch
   :unit/id unit-id
   :status/row {:event/id evt-token
                :unit/id unit-id
                :status status
                :branch/id default-branch}})

;; ===========================================================================
;; Isolation discipline (verbatim from write-echo): each scenario has its OWN
;; artifact + its own tag-scoped id space. The materialization signal is the
;; trailing int of the token; a fresh artifact reads nil (index -1) first, so
;; no stale head from a prior scenario can stamp a FAKE-instant materialization.
;; ===========================================================================

(defn- trailing-int
  "Integer after the last '-' in a probe token (\"probe-rev-c12-7\" -> 7), or
   -1 for nil / seed / any non-numeric suffix."
  [s]
  (if (string? s)
    (let [i (.lastIndexOf ^String s "-")]
      (if (neg? i)
        -1
        (try (Long/parseLong (subs s (inc i))) (catch Exception _ -1))))
    -1))

(defn read-head-index
  "Max content-edit index materialized for this artifact: $$echo-heads[art] is
   \"probe-rev-<tag>-<i>\"; returns i, or -1 (nil / non-numeric)."
  [rt art]
  (trailing-int (foreign-select-one (keypath art) (:echo-heads rt)
                                    {:pkey (routing-key art)})))

(defn read-status-index
  "Max status-edit index materialized for one unit: the row's :event/id is
   \"probe-evt-<tag>-<i>\"; returns i, or -1."
  [rt art unit-id]
  (trailing-int (:event/id (foreign-select-one
                             (keypath art default-branch unit-id)
                             (:echo-statuses rt)
                             {:pkey (routing-key art)}))))

;; ── stats (verbatim from write-echo, so the tables are directly comparable) ──
(defn- ->ms [ns] (/ (double ns) 1e6))

(defn- pctl
  "Nearest-rank percentile over a sorted long vector of nanos, in ms."
  [sorted p]
  (if (empty? sorted)
    nil
    (let [n (count sorted)
          rank (long (Math/ceil (* (/ p 100.0) n)))
          idx (max 0 (min (dec n) (dec rank)))]
      (->ms (nth sorted idx)))))

(defn- summarize [label nanos-vec]
  (let [s (vec (sort nanos-vec))
        n (count s)]
    {:label label
     :n n
     :p50 (pctl s 50) :p95 (pctl s 95) :p99 (pctl s 99)
     :max (when (seq s) (->ms (last s)))
     :min (when (seq s) (->ms (first s)))}))

;; ── the scenario (open-loop; verbatim structure from write-echo) ─────────────
(defn run-scenario
  "mode :content -> content-request (revision + head write, realistic payload).
   mode :status  -> status-request on a fixed unit (smallest keyed write).
   Open-loop: emits at a fixed cadence for duration-s, independent of
   materialization. Returns a summary map."
  [rt {:keys [mode rate-hz duration-s tag]}]
  (let [n (long (* rate-hz duration-s))
        period-ns (long (/ 1e9 rate-hz))
        art (str "art_stream_echo_" tag)
        unit-id (str art "/rev-seed/line/1")
        emit-ns (long-array n -1)
        ack-ns  (long-array n -1)
        mat-ns  (long-array n -1)
        poll-count (atom 0)
        poll-nanos (atom 0)
        emitter-done? (atom false)
        read-idx (case mode
                   :content #(read-head-index rt art)
                   :status  #(read-status-index rt art unit-id))
        ;; poller: tight loop, timestamps each index transition (monotonic).
        grace-ns (long 5e9)
        poller (Thread.
                 (fn []
                   (let [deadline-atom (atom (+ (System/nanoTime) (long (* period-ns n)) grace-ns))]
                     (loop [next-i 0]
                       (let [t0 (System/nanoTime)
                             k (read-idx)
                             t1 (System/nanoTime)]
                         (swap! poll-count inc)
                         (swap! poll-nanos + (- t1 t0))
                         (let [now (System/nanoTime)
                               ;; fill all newly-visible indices with this observation time
                               ;; (batch-jumps: N events visible together share one time)
                               next-i' (loop [j next-i]
                                         (if (and (< j n) (<= j k))
                                           (do (aset mat-ns j now) (recur (inc j)))
                                           j))]
                           (cond
                             (>= next-i' n) nil ;; all materialized, done
                             (and @emitter-done? (>= now @deadline-atom)) nil ;; grace elapsed
                             :else (recur next-i'))))))))]
    (.start poller)
    ;; emitter (this thread): fixed-cadence open-loop.
    (let [t-start (System/nanoTime)]
      (dotimes [i n]
        (let [target (+ t-start (* i period-ns))]
          ;; precise-ish cadence: park until the slot
          (let [wait (- target (System/nanoTime))]
            (when (> wait 0) (java.util.concurrent.locks.LockSupport/parkNanos wait)))
          (let [req (case mode
                      :content
                      (content-request art (str "probe-rev-" tag "-" i)
                                       (str base-block " " i))
                      :status
                      (status-request art unit-id
                                      (if (even? i) :accepted :promoted)
                                      (str "probe-evt-" tag "-" i)))
                e0 (System/nanoTime)]
            (foreign-append! (:echo-requests-depot rt) req :append-ack)
            (let [a0 (System/nanoTime)]
              (aset emit-ns i e0)
              (aset ack-ns i a0)))))
      (reset! emitter-done? true)
      (.join poller (long 12000))
      ;; ── derive per-event legs ──
      (let [leg1 (vec (for [i (range n)] (- (aget ack-ns i) (aget emit-ns i))))
            materialized-idx (filterv #(pos? (aget mat-ns %)) (range n))
            unmaterialized (- n (count materialized-idx))
            leg2 (vec (for [i materialized-idx] (- (aget mat-ns i) (aget ack-ns i))))
            echo12 (vec (for [i materialized-idx] (- (aget mat-ns i) (aget emit-ns i))))
            ;; stall = legs1+2 echo > 100ms; unmaterialized also counts as a stall
            stalls (+ unmaterialized (count (filter #(> % (long 100e6)) echo12)))
            emit-span-ns (- (aget emit-ns (dec n)) (aget emit-ns 0))
            achieved-rate (if (pos? emit-span-ns) (/ (double (dec n)) (->ms emit-span-ns) 0.001) 0.0)
            first-echo (when (pos? (aget mat-ns 0)) (->ms (- (aget mat-ns 0) (aget emit-ns 0))))]
        {:mode mode :rate-hz rate-hz :duration-s duration-s
         :requested-events n
         :materialized (count materialized-idx)
         :unmaterialized unmaterialized
         :achieved-emit-rate-hz (Double/parseDouble (format "%.2f" achieved-rate))
         :stalls-over-100ms stalls
         :first-event-echo-ms first-echo
         :poll-resolution-ms (Double/parseDouble
                               (format "%.4f" (if (pos? @poll-count)
                                                (->ms (/ @poll-nanos @poll-count)) 0.0)))
         :poll-samples @poll-count
         :leg1-emit->ack       (summarize "leg1 emit->append-ack" leg1)
         :leg2-ack->materialize (summarize "leg2 append-ack->materialized" leg2)
         :echo-legs1+2-emit->materialize (summarize "echo (legs1+2) emit->materialized" echo12)}))))

;; ── SECONDARY cross-check: closed-loop :ack round-trip ───────────────────────
;; The REAL write-then-read-back call. foreign-append! ... :ack blocks until the
;; stream event tree completes AND the PState write is visible. Single writer,
;; one append at a time (closed-loop by nature — :ack blocks). If this p95
;; agrees with the open-loop echo p95, the polled leg-decomposition is trusted.
(defn run-ack-roundtrip
  [rt {:keys [n tag]}]
  (let [art (str "art_stream_ack_" tag)
        rt-ns (long-array n -1)]
    (dotimes [i n]
      (let [req (content-request art (str "ack-rev-" tag "-" i) (str base-block " " i))
            t0 (System/nanoTime)]
        (foreign-append! (:echo-requests-depot rt) req :ack)
        ;; read-back on return: :ack guarantees the write is visible now.
        (let [_ (foreign-select-one (keypath art) (:echo-heads rt)
                                    {:pkey (routing-key art)})
              t1 (System/nanoTime)]
          (aset rt-ns i (- t1 t0)))))
    (assoc (summarize "ack round-trip (append :ack -> visible + read-back)" (vec rt-ns))
           :n n :tag tag)))

;; ── reporting ────────────────────────────────────────────────────────────────
(defn- print-report [s]
  (println "\n════════════════════════════════════════════════════════════════")
  (println (format "  SCENARIO: mode=%s  rate=%s/s  duration=%ss  [STREAM topology]"
                    (name (:mode s)) (:rate-hz s) (:duration-s s)))
  (println "════════════════════════════════════════════════════════════════")
  (println (format "  requested events      : %d" (:requested-events s)))
  (println (format "  materialized          : %d" (:materialized s)))
  (println (format "  UNMATERIALIZED        : %d   (never visible within grace)" (:unmaterialized s)))
  (println (format "  achieved emit rate    : %.2f /s" (:achieved-emit-rate-hz s)))
  (println (format "  stalls (>100ms)       : %d /min" (:stalls-over-100ms s)))
  (println (format "  first-event echo      : %s ms  (cold: first keystroke)" (:first-event-echo-ms s)))
  (println (format "  poll resolution       : %.4f ms/read  (%d reads)"
                   (:poll-resolution-ms s) (:poll-samples s)))
  (let [fmt (fn [x] (if x (format "%.2f" x) "n/a"))]
    (doseq [k [:leg1-emit->ack :leg2-ack->materialize :echo-legs1+2-emit->materialize]]
      (let [d (get s k)]
        (println (format "  %-34s p50=%-7s p95=%-7s p99=%-7s max=%s"
                         (:label d) (fmt (:p50 d)) (fmt (:p95 d)) (fmt (:p99 d)) (fmt (:max d)))))))
  (let [echo (:echo-legs1+2-emit->materialize s)
        p95 (:p95 echo)
        pass? (and p95 (<= p95 50.0) (<= (:stalls-over-100ms s) 1))]
    (println (format "  PRE-REGISTERED (legs1+2): p95=%.2fms (<=50) AND stalls=%d (<=1)  => %s"
                     (or p95 -1.0) (:stalls-over-100ms s) (if pass? "STANDS" "FAILS")))))

(defn- print-ack-report [s]
  (println "\n────────────────────────────────────────────────────────────────")
  (println (format "  CROSS-CHECK: closed-loop :ack round-trip  (n=%d)" (:n s)))
  (println "  (foreign-append! ... :ack  ->  PState visible + read-back, in ONE call)")
  (let [fmt (fn [x] (if x (format "%.2f" x) "n/a"))]
    (println (format "  %-34s p50=%-7s p95=%-7s p99=%-7s max=%s"
                     "ack round-trip" (fmt (:p50 s)) (fmt (:p95 s)) (fmt (:p99 s)) (fmt (:max s))))))

(defn -main [& _]
  (println "[write-echo-2] launching stream-echo module (in-process IPC, tasks=4 threads=2)...")
  (let [rt (start-stream-echo-runtime!)]
    (try
      (println "[write-echo-2] warmup (JIT + first-batch cost paid before measured runs)...")
      (run-scenario rt {:mode :content :rate-hz 12 :duration-s 3 :tag "warm"})
      (let [results
            [(run-scenario rt {:mode :content :rate-hz 12 :duration-s 60 :tag "c12"})
             (run-scenario rt {:mode :content :rate-hz 3  :duration-s 60 :tag "c3"})
             (run-scenario rt {:mode :status  :rate-hz 12 :duration-s 60 :tag "s12"})]
            ack (run-ack-roundtrip rt {:n 300 :tag "ack1"})]
        (doseq [s results] (print-report s))
        (print-ack-report ack)
        (spit "/tmp/claude-1000/-mnt-data-projects-Softland/6dd8a20e-7826-4c0d-95e0-dbbd31410859/scratchpad/write-echo-2-results.edn"
              (pr-str {:scenarios results :ack-roundtrip ack}))
        (println "\n[write-echo-2] raw summary EDN -> scratchpad/write-echo-2-results.edn"))
      (finally
        (println "[write-echo-2] closing runtime.")
        (close-stream-echo-runtime! rt)
        (shutdown-agents)))))
