(ns app.server.rama.probe-harness
  "Reusable depot-adversary probes for kernel modules on IPC.

   The depot adversary matrix (skill rama-retro, step R5): for every depot of a
   kernel, deliver the hostile inputs an at-least-once world will eventually
   produce — appends before their target exists, duplicate ids with same and
   different payloads, missing/wrong claim tokens, writes after terminal state,
   collections pushed past their claimed bound — and assert committed truth
   survives untouched.

   The harness is module-agnostic: probes take closures over a module's own
   runtime helpers (append!/read-row/await fns like the ones each kernel module
   already exposes for its tests) and return result maps with the before/after
   truth snapshots, so per-module tests can assert SEMANTIC payloads, not just
   row existence. Per-module probe suites come in the per-kernel fix sessions;
   this namespace only provides the shared machinery.

   Typical wiring with runtime helpers supplied by a module test:

     (let [runtime (start-runtime!)]
       (probe/probe-duplicate-id-same-payload!
         {:read-state #(read-run runtime run-id)
          :append!    #(append-run-command! runtime request)}))"
  (:require [clojure.test :refer [do-report]]))

;; ── Polling ─────────────────────────────────────────────────────────────────

(defn await-materialized
  "Poll read-f until pred is truthy or timeout-ms elapses; returns the last
   value read either way. Use to settle a positive expectation (a row/decision
   appearing) before running a probe whose pass condition is negative."
  ([read-f pred] (await-materialized read-f pred 2000))
  ([read-f pred timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop [value (read-f)]
       (cond
         (pred value) value
         (>= (System/currentTimeMillis) deadline) value
         :else (do (Thread/sleep 25)
                   (recur (read-f))))))))

;; ── Classifiers (pure) ──────────────────────────────────────────────────────
;;
;; A classifier takes the before/after truth snapshots and returns
;; {:pass? <bool> :expected <kw> ...}. Probes whose pass condition is negative
;; ("truth must NOT change") cannot poll for it, so run-probe! settles for a
;; fixed window (or a caller-provided fence) before the after-snapshot.

(defn unchanged-truth
  "Pass when committed truth is identical before and after the adversarial act."
  [before after]
  {:pass? (= before after)
   :expected :unchanged
   :changed? (not= before after)})

(defn absent-truth
  "Pass when no truth row exists after the adversarial act (e.g. an observation
   for an unknown id must not create a phantom row)."
  [_before after]
  {:pass? (nil? after)
   :expected :absent})

(defn within-bound
  "Pass when a collection's size honors its claimed bound. Snapshots are counts."
  [bound]
  (fn [_before after]
    {:pass? (and (number? after) (<= after bound))
     :expected :within-bound
     :bound bound
     :count after}))

;; ── Probe core ──────────────────────────────────────────────────────────────

(defn run-probe!
  "Generic adversary probe: snapshot truth, perform the adversarial act, settle,
   snapshot again, classify.

   opts:
     :probe       keyword naming the probe (carried into the result)
     :read-state  (fn []) reads the truth under attack (row, count, …)
     :act!        (fn []) performs the adversarial act (append the hostile record)
     :classify    (fn [before after]) → {:pass? … :expected …}
     :settle!     optional (fn []) that blocks until the topology has consumed
                  past the hostile record — e.g. append a subsequent VALID
                  record and await its effect (a fence). Strictly stronger than
                  sleeping; prefer it when the module makes it possible.
     :settle-ms   fixed settle window when no :settle! is given (default 400)
     :read-signal optional (fn []) reading a side signal (dead-letter row,
                  decision, conflict marker); snapshotted before and after so
                  tests can assert the rejection was RECORDED, not just that
                  truth survived.

   Returns {:probe … :before … :after … (:signal-before :signal-after) …}
   merged with the classifier's verdict."
  [{:keys [probe read-state act! classify settle! settle-ms read-signal]
    :or {settle-ms 400}}]
  (let [before (read-state)
        signal-before (when read-signal (read-signal))]
    (act!)
    (if settle!
      (settle!)
      (Thread/sleep settle-ms))
    (let [after (read-state)
          signal-after (when read-signal (read-signal))]
      (cond-> (merge {:probe probe :before before :after after}
                     (classify before after))
        read-signal (assoc :signal-before signal-before
                           :signal-after signal-after)))))

;; ── The adversary matrix ────────────────────────────────────────────────────

(defn probe-append-before-request!
  "Adversary: deliver an observation/claim/control for an id whose request was
   never appended. Pass when no phantom truth row appears for that id.

     (probe-append-before-request!
       {:read-state  #(compute/read-run runtime \"run_orphan\")
        :append-obs! #(compute/append-observation! runtime orphan-obs)})"
  [{:keys [read-state append-obs! settle! settle-ms read-signal]}]
  (run-probe! {:probe :append-before-request
               :read-state read-state
               :act! append-obs!
               :classify absent-truth
               :settle! settle!
               :settle-ms (or settle-ms 400)
               :read-signal read-signal}))

(defn probe-duplicate-id-same-payload!
  "Adversary: re-deliver an already-committed record verbatim (Rama retry /
   client replay). Pass when committed truth is byte-identical afterwards.
   Run only after the first delivery has settled to a stable state — await the
   row/decision first, or the comparison races the normal lifecycle.

     (compute/await-decision runtime run-id)
     (probe-duplicate-id-same-payload!
       {:read-state #(select-keys (compute/read-run runtime run-id)
                                  [:status :argv :claimed-by])
        :append!    #(compute/append-run-command! runtime request)})"
  [{:keys [read-state append! settle! settle-ms read-signal]}]
  (run-probe! {:probe :duplicate-id-same-payload
               :read-state read-state
               :act! append!
               :classify unchanged-truth
               :settle! settle!
               :settle-ms (or settle-ms 400)
               :read-signal read-signal}))

(defn probe-duplicate-id-different-payload!
  "Adversary: deliver a record reusing a committed id with DIFFERENT content.
   Pass when committed truth still reflects the original. Pair with
   :read-signal to also assert the conflict was recorded (conflict decision /
   dead-letter), not silently swallowed.

     (probe-duplicate-id-different-payload!
       {:read-state  #(compute/read-run runtime run-id)
        :append!     #(compute/append-run-command! runtime conflicting-request)
        :read-signal #(compute/read-decision runtime run-id)})"
  [{:keys [read-state append! settle! settle-ms read-signal]}]
  (run-probe! {:probe :duplicate-id-different-payload
               :read-state read-state
               :act! append!
               :classify unchanged-truth
               :settle! settle!
               :settle-ms (or settle-ms 400)
               :read-signal read-signal}))

(defn probe-unauthorized-observation!
  "Adversary: deliver an observation with no claim token, a wrong token, or a
   stale executor identity (the caller builds the hostile record; run once per
   variant). Pass when the truth row is unchanged.

     (probe-unauthorized-observation!
       {:read-state #(compute/read-run runtime run-id)
        :append-obs! #(compute/append-observation!
                        runtime (assoc obs :claim/token \"wrong\"))})"
  [{:keys [read-state append-obs! settle! settle-ms read-signal]}]
  (run-probe! {:probe :unauthorized-observation
               :read-state read-state
               :act! append-obs!
               :classify unchanged-truth
               :settle! settle!
               :settle-ms (or settle-ms 400)
               :read-signal read-signal}))

(defn probe-post-terminal-write!
  "Adversary: drive the row to a terminal state FIRST (caller's job — await it
   with await-materialized), then deliver a late mutation. Pass when the row
   stays exactly as it was at terminal: sticky terminals never regress.

     (compute/await-view runtime run-id terminal-view? 5000)
     (probe-post-terminal-write!
       {:read-state #(compute/read-run runtime run-id)
        :append-late! #(compute/append-observation! runtime late-obs)})"
  [{:keys [read-state append-late! settle! settle-ms read-signal]}]
  (run-probe! {:probe :post-terminal-write
               :read-state read-state
               :act! append-late!
               :classify unchanged-truth
               :settle! settle!
               :settle-ms (or settle-ms 400)
               :read-signal read-signal}))

(defn probe-collection-bound!
  "Adversary: push a bounded collection past its claimed bound. :read-count
   reads the collection's current size; :push-n! appends n records that each
   grow the collection. Pass when the size still honors the bound after the
   topology settles.

     (probe-collection-bound!
       {:read-count #(count (:stdout-tail (compute/read-view runtime run-id)))
        :push-n!    (fn [n] (dotimes [i n] (append-line! i)))
        :bound      200
        :overshoot  50})"
  [{:keys [read-count push-n! bound overshoot settle! settle-ms read-signal]
    :or {overshoot 25}}]
  (let [n (+ (long bound) (long overshoot))]
    (run-probe! {:probe :collection-past-bound
                 :read-state read-count
                 :act! #(push-n! n)
                 :classify (within-bound bound)
                 :settle! settle!
                 :settle-ms (or settle-ms 800)
                 :read-signal read-signal})))

;; ── Matrix runner ───────────────────────────────────────────────────────────

(defn run-adversary-matrix!
  "Run a sequence of probe thunks, report each failure through clojure.test
   (with the full result map as the actual value, so failures show semantic
   payloads), and return all results. Use inside a deftest:

     (deftest compute-adversary-matrix-test
       (run-adversary-matrix!
         [#(probe-duplicate-id-same-payload! {...})
          #(probe-post-terminal-write! {...})]))"
  [probe-thunks]
  (let [results (mapv (fn [thunk] (thunk)) probe-thunks)]
    (doseq [result results]
      (do-report (if (:pass? result)
                   {:type :pass}
                   {:type :fail
                    :message (str "adversary probe failed: " (:probe result))
                    :expected (:expected result)
                    :actual result})))
    results))

(defn failed-probes
  "Filter a result seq down to the failures."
  [results]
  (vec (remove :pass? results)))
