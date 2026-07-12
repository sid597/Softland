(ns app.probe.write-echo-probe
  "write-echo spike (2026-07-11) — UNCOMMITTED probe, evidence not product.

   Prices the DIRECT-to-Rama editor echo at typing speed against the REAL
   text-kernel microbatch topology. No kernel/depot/schema edits: rides the
   existing lawful request path (text_kernel.clj :artifact/ingest and
   :unit/status-set) against a single probe artifact.

   Measured legs (see the opening prompt):
     (1) client emit -> depot append :append-ack        [emitter thread]
     (2) materialization: PState reflects the event      [poller thread]
     (3) stream back through Electric into a client atom  [NOT measured here:
         characterized separately -- see build/write-echo/NOW.md. Leg 3 is a
         plain server atom mirror bumped AFTER materialization + e/watch
         transport, so it is strictly additive to (1)+(2).]

   This harness measures (1)+(2) -- the Rama half, the dominant unknown --
   with tight-poll resolution. Result is COMPOSED, NOT END-TO-END.

   IMPORTANT CAVEAT baked into every number: this runs against in-process IPC
   (create-ipc), a TEST harness. Microbatch cadence, replication, and
   cross-worker coordination differ from a production cluster. Treat these as
   a FLOOR characterization of the current topology, not a production SLA.

   Run:  clojure -J-Xss16m -M -m app.probe.write-echo-probe"
  (:require [com.rpl.rama :as r]
            [com.rpl.rama.path :refer [keypath]]
            [app.server.rama.text-kernel :as tk]
            [app.server.rama.core :as core]))

;; ── realistic single block: ~185 chars, 3 lines (prose-grade Softland block) ──
(def base-block
  (str "The map must not lie: known, proposed, derived, guessed --\n"
       "visibly distinct, always. Provenance is soul, not plumbing.\n"
       "History is terrain; the log is primary, every view a projection."))

;; Each scenario is FULLY ISOLATED: its own artifact + its own tag-scoped id
;; space (request/event/revision ids). Without this, reused request-ids dedup
;; to no-ops and reused event-ids hit the collision guard -- and a stale head
;; from a prior scenario would stamp FAKE-instant materialization. So every
;; token ends in "-<tag>-<i>" and the materialization signal is the trailing int.

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

;; ── tight materialization-signal reads (1 seek each, colocated on routing key)
(defn read-head-index
  "Max content-edit index materialized for this artifact: $$artifact-heads[art]
   is \"probe-rev-<tag>-<i>\"; returns i, or -1 (nil / seed / non-numeric)."
  [rt art]
  (trailing-int (r/foreign-select-one (keypath art) (:artifact-heads rt)
                                      {:pkey (tk/artifact-routing-key art)})))

(defn read-status-index
  "Max status-edit index materialized for one unit: the row's :event/id is
   \"probe-evt-<tag>-<i>\"; returns i, or -1."
  [rt art unit-id]
  (trailing-int (:event/id (r/foreign-select-one
                             (keypath art core/default-branch-id unit-id)
                             (:unit-statuses rt)
                             {:pkey (tk/artifact-routing-key art)}))))

;; ── stats ─────────────────────────────────────────────────────────────────
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

;; ── the scenario ────────────────────────────────────────────────────────────
(defn run-scenario
  "mode :content -> :artifact/ingest new revision per event (content-write path,
   conservative: re-derives line units each event).
   mode :status  -> :unit/status-set on a fixed unit (smallest keyed write).
   Open-loop: emits at a fixed cadence for duration-s, independent of
   materialization. Returns a summary map."
  [rt {:keys [mode rate-hz duration-s tag]}]
  (let [n (long (* rate-hz duration-s))
        period-ns (long (/ 1e9 rate-hz))
        art (str "art_write_echo_" tag)
        ;; status mode needs a pre-existing unit to judge; seed the artifact.
        unit-id (when (= mode :status)
                  (let [ev (tk/ingest-text! rt base-block
                                            {:artifact-id art
                                             :revision-id (str "probe-rev-" tag "-seed")
                                             :request-id (str "probe-req-" tag "-seed")
                                             :proposed-event-id (str "probe-evt-" tag "-seed")})]
                    (:unit/id (first (tk/unitize-lines! rt ev)))))
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
                      (tk/ingest-text-request
                        (str base-block " " i)
                        {:artifact-id art :revision-id (str "probe-rev-" tag "-" i)
                         :request-id (str "probe-req-" tag "-" i)
                         :proposed-event-id (str "probe-evt-" tag "-" i)
                         :source-type :edit :time-ms (System/currentTimeMillis)})
                      :status
                      (tk/unit-status-request
                        unit-id (if (even? i) :accepted :promoted)
                        {:artifact-id art
                         :request-id (str "probe-req-" tag "-" i)
                         :proposed-event-id (str "probe-evt-" tag "-" i)
                         :time-ms (System/currentTimeMillis)}))
                e0 (System/nanoTime)]
            (r/foreign-append! (:text-requests-depot rt) req :append-ack)
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

(defn- print-report [s]
  (println "\n════════════════════════════════════════════════════════════════")
  (println (format "  SCENARIO: mode=%s  rate=%s/s  duration=%ss"
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

(defn -main [& _]
  (println "[write-echo] launching text-kernel module (in-process IPC, tasks=4 threads=2)...")
  (let [rt (tk/start-text-runtime!)]
    (try
      (println "[write-echo] warmup...")
      ;; warmup: JIT + first-microbatch cost paid before the measured runs
      (run-scenario rt {:mode :content :rate-hz 12 :duration-s 3 :tag "warm"})
      (let [results
            [(run-scenario rt {:mode :content :rate-hz 12 :duration-s 60 :tag "c12"})
             (run-scenario rt {:mode :content :rate-hz 3  :duration-s 60 :tag "c3"})
             (run-scenario rt {:mode :status  :rate-hz 12 :duration-s 60 :tag "s12"})]]
        (doseq [s results] (print-report s))
        (spit "/tmp/claude-1000/-mnt-data-projects-Softland/781f31cb-c348-422b-975a-009b6ab56649/scratchpad/write-echo-results.edn"
              (pr-str results))
        (println "\n[write-echo] raw summary EDN -> scratchpad/write-echo-results.edn"))
      (finally
        (println "[write-echo] closing runtime.")
        (tk/close-text-runtime! rt)
        (shutdown-agents)))))
