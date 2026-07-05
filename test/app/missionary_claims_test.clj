(ns app.missionary-claims-test
  "Regression tests for VERIFIED platform-real Electric/Missionary behavioral
   claims harvested in
   `docs/current-mental-model/build/electric-skill/HACKS-LEDGER.md` and
   adjudicated in the sibling `VERDICTS.md`.

   Pure Missionary — NO DOM, NO Electric runtime. Each `deftest` is named after
   its ledger claim number and locks in the behavior VERDICTS.md records as
   PLATFORM-REAL, so a future Missionary/Electric bump that changes it is caught
   here rather than in the app.

   Only claims whose verdict is PLATFORM-REAL AND reproducible as a deterministic
   pure-Missionary probe live here (ledger claims 1, 2, 3, 6, 8, 9, 11, 12, 18).
   Everything else is adjudicated in VERDICTS.md, not asserted here:
   compile-check / primary-source claims (5, 13, 15, 16, 20), UNVERIFIED claims
   (4, 14, 19), and the falsified / agent-error / timing-only probes kept there
   as verbatim code blocks (7, 10, 17). (Claim 6 IS here — it asserts the loud
   ArityException, which is the platform truth that corrects the claim's wording.)

   NOTE the irony (per the task brief): these probes deliberately build the
   shapes CLAUDE.md bans (side effects / forks) to observe the ban's basis —
   claim-03 in particular exercises the m/latest sampling semantics that the
   'never put side effects in m/latest' rule rests on.

   Environment: Missionary b.46 (transitive via Electric
   v3-alpha-20260519.115706-45 per deps.edn), Clojure 1.12.4. Recorded
   2026-07-05."
  (:require [clojure.test :refer [deftest is testing]]
            [missionary.core :as m]))

;; ---------------------------------------------------------------------------
;; Harness. Missionary propagation on an atom mutation is synchronous on the
;; mutating thread for m/watch / m/latest / m/ap / m/?< / m/reduce (no thread
;; offload), so after driving mutations a synchronous crash has ALREADY been
;; delivered — `realized?` reflects crash-or-not with no waiting.
;; ---------------------------------------------------------------------------

(defn- collect
  "Subscribe to continuous/discrete `flow` via m/reduce with a side-effecting
   collector. Returns {:seen atom :outcome promise :cancel fn}."
  [flow]
  (let [!seen  (atom [])
        outcome (promise)
        cancel ((m/reduce (fn [_ v] (swap! !seen conj v) nil) nil flow)
                (fn [s] (deliver outcome [:done s]))
                (fn [e] (deliver outcome [:crash e])))]
    {:seen !seen :outcome outcome :cancel cancel}))

(defn- settle
  "Wait up to `ms` for the flow to crash or complete; returns as soon as the
   outcome promise is delivered. [:crash ex] / [:done s] / :running. Most crashes
   land within a few ms; m/observe-driven ones take a scheduler hop, so a small
   ceiling (returned-early on delivery) keeps crash detection robust without
   slowing the happy path."
  ([p] (settle p 1000))
  ([{:keys [outcome]} ms] (deref outcome ms :running)))

(defn- crashed
  "Assert the flow crashed; return the exception (or nil, having failed an is)."
  [p]
  (let [o (settle p)]
    (is (and (vector? o) (= :crash (first o))) (str "expected a crash, got " (pr-str o)))
    (when (vector? o) (second o))))

(defn- cancel! [{:keys [cancel]}] (try (cancel) (catch Throwable _)))

(defn- class-name [x] (.getName (class x)))

;; ---------------------------------------------------------------------------
;; Claim 1 — m/ap with multiple nested m/?< over m/watch crashes "Watch
;; cancelled" on the first input change. (The trigger is the NESTED fork, not
;; being "fed to m/latest" — see VERDICTS.md §1.)
;; ---------------------------------------------------------------------------

(deftest claim-01-nested-ap-forks-crash-watch-cancelled
  (testing "single m/ap + two nested m/?< over m/watch crashes on input change"
    (let [!a (atom 0) !b (atom 0)
          bad (m/ap (let [a (m/?< (m/watch !a))
                          b (m/?< (m/watch !b))]
                      [a b]))
          p (collect bad)]
      (reset! !a 1)                                  ; outer fork cancels inner m/watch
      (when-let [ex (crashed p)]
        (is (= "missionary.Cancelled" (class-name ex)))
        (is (= "Watch cancelled." (.getMessage ex))))
      (cancel! p)))
  (testing "m/latest over raw m/watch (the sanctioned shape) does NOT crash"
    (let [!a (atom 0) !b (atom 0)
          good (m/latest vector (m/watch !a) (m/watch !b))
          p (collect good)]
      (reset! !a 1) (reset! !b 2)
      (is (= :running (settle p 500)))               ; live, no crash
      (is (= [1 2] (last @(:seen p))))               ; latest combo observed
      (cancel! p))))

;; ---------------------------------------------------------------------------
;; Claim 2 — discrete event filtering: m/eduction + @deref is safe across a
;; predicate-atom change; m/ap + m/?< over (m/watch pred) crashes on change.
;; ---------------------------------------------------------------------------

(deftest claim-02-eduction-filter-safe-ap-fork-crashes
  (testing "m/eduction + @deref passes events across a focus-atom change"
    (let [!focus (atom :editor) !push (atom nil)
          >events (m/observe (fn [!] (reset! !push !) (fn [] (reset! !push nil))))
          good (->> >events (m/eduction (filter (fn [_] (= @!focus :editor)))))
          p (collect good)]
      (when @!push (@!push :e1))
      (reset! !focus :other)                         ; focus change alone: no emission
      (reset! !focus :editor)
      (when @!push (@!push :e2))
      (is (= :running (settle p 500)))
      (is (= [:e1 :e2] @(:seen p)))
      (cancel! p)))
  (testing "m/ap + m/?< over (m/watch !focus): focus changes spuriously re-emit; a new event crashes 'Watch cancelled'"
    (let [!focus (atom :editor) !push (atom nil)
          >events (m/observe (fn [!] (reset! !push !) (fn [] (reset! !push nil))))
          bad (m/ap (let [e (m/?< >events)              ; outer fork on events
                          f (m/?< (m/watch !focus))]    ; nested inner fork on focus
                      (when (= f :editor) e)))
          p (collect bad)]
      (when @!push (@!push :e1))                        ; => emits :e1
      (reset! !focus :other)                            ; inner fork re-emits (spurious) — clean
      (reset! !focus :editor)                           ; inner fork re-emits (spurious) — clean
      (when @!push (@!push :e2))                         ; outer fork restarts => cancels nested inner m/watch
      (when-let [ex (crashed p)]                         ; => "Watch cancelled"
        (is (= "missionary.Cancelled" (class-name ex))))
      (cancel! p))))

;; ---------------------------------------------------------------------------
;; Claim 3 — m/latest re-runs its combine fn only on INPUT change, not on each
;; m/sample trigger tick. (This is the platform basis for the "no side effects
;; / RAF scheduling in m/latest" rule: scheduling inside the fn fires once then
;; never again while inputs are idle.)
;; ---------------------------------------------------------------------------

(deftest claim-03-latest-not-rerun-on-sample-trigger
  (testing "m/latest combine fn invoked once, not once per sample trigger, when inputs are idle"
    (let [!a (atom 0)
          calls (atom 0)
          <world (m/latest (fn [a] (swap! calls inc) a) (m/watch !a))
          >trigger (m/seed (range 5))              ; 5 sample ticks, !a never changes
          done (promise)]
      ((m/reduce (fn [_ _] nil) nil (m/sample vector <world >trigger))
       (fn [_] (deliver done :ok)) (fn [e] (deliver done [:err e])))
      (is (= :ok (deref done 5000 :timeout)))
      (is (= 1 @calls)
          "combine fn ran once (initial) despite 5 sample triggers"))))

;; ---------------------------------------------------------------------------
;; Claim 6 — m/latest arity mismatch is a LOUD ArityException, NOT the ledger's
;; claimed "silent corruption, no error". (VERDICTS.md §6: AGENT-ERROR on the
;; failure-mode wording; the "match arg count to arity" advice still holds.)
;; ---------------------------------------------------------------------------

(deftest claim-06-latest-arity-mismatch-throws-arityexception
  (testing "fewer flows than fn params => ArityException (loud, not silent)"
    (let [p (collect (m/latest (fn [a b c] [a b c])
                               (m/watch (atom :A)) (m/watch (atom :B))))]
      (when-let [ex (crashed p)]
        (is (= "clojure.lang.ArityException" (class-name ex))))
      (cancel! p)))
  (testing "more flows than fn params => ArityException too"
    (let [p (collect (m/latest (fn [a b] [a b])
                               (m/watch (atom :A)) (m/watch (atom :B)) (m/watch (atom :C))))]
      (when-let [ex (crashed p)]
        (is (= "clojure.lang.ArityException" (class-name ex))))
      (cancel! p))))

;; ---------------------------------------------------------------------------
;; Claim 8 — coarse invalidation: a single m/latest over N atoms re-runs the
;; WHOLE combine fn on ANY one atom's change.
;; ---------------------------------------------------------------------------

(deftest claim-08-latest-coarse-invalidation
  (testing "changing any one input re-runs the entire combine fn"
    (let [!a (atom 0) !b (atom 0) !c (atom 0)
          runs (atom 0)
          <w (m/latest (fn [a b c] (swap! runs inc) [a b c])
                       (m/watch !a) (m/watch !b) (m/watch !c))
          p (collect <w)]
      (reset! !a 1)                                  ; !b never touched
      (reset! !c 1)
      (is (= 3 @runs) "initial + change-a + change-c = 3 full re-runs")
      (cancel! p))))

;; ---------------------------------------------------------------------------
;; Claim 9 — an atom read via @ inside a combine fn, but NOT declared as a flow
;; input, is an untracked (hidden) edge: changing it does not re-run the fn, so
;; the read goes stale until a declared input changes.
;; ---------------------------------------------------------------------------

(deftest claim-09-hidden-edge-deref-not-declared-input
  (testing "@-read of an atom that is not a flow input is untracked / stale"
    (let [!a (atom 0) !c (atom :x)
          runs (atom 0) seen (atom nil)
          <d (m/latest (fn [a] (swap! runs inc) (reset! seen [a @!c]) a) (m/watch !a))
          p (collect <d)]
      (is (= 1 @runs))
      (is (= [0 :x] @seen))
      (reset! !c :y)                                 ; !c is not a flow input
      (is (= 1 @runs) "changing an @-read atom does NOT re-run the fn")
      (is (= [0 :x] @seen) "the @-read stays STALE :x")
      (reset! !a 1)                                  ; a DECLARED input changes
      (is (= 2 @runs))
      (is (= [1 :y] @seen) "only a declared-input change re-runs the fn (and picks up :y)")
      (cancel! p))))

;; ---------------------------------------------------------------------------
;; Claim 11 — m/sample requires initially-ready sampled inputs, trigger last.
;; ---------------------------------------------------------------------------

(deftest claim-11-sample-requires-initially-ready-sampled-inputs
  (testing "correct order (continuous sampled first, discrete trigger last) works"
    (let [!a (atom :v)
          done (promise) seen (atom [])]
      ((m/reduce (fn [_ v] (swap! seen conj v) nil) nil
                 (m/sample vector (m/watch !a) (m/seed (range 3))))
       (fn [_] (deliver done :ok)) (fn [e] (deliver done [:err e])))
      (is (= :ok (deref done 5000 :timeout)))
      (is (= [[:v 0] [:v 1] [:v 2]] @seen))))
  (testing "a NOT-initially-ready flow in a sampled slot throws 'Undefined continuous flow'"
    (let [>not-ready (m/observe (fn [!] (fn [] nil)))   ; never emits => never ready
          >trigger (m/seed (range 3))
          out (promise)]
      ((m/reduce (fn [_ _] nil) nil (m/sample vector >not-ready >trigger))
       (fn [_] (deliver out :ok)) (fn [e] (deliver out [:crash (.getMessage e)])))
      (let [r (deref out 5000 :timeout)]
        (is (= :crash (first r)))
        (is (= "Undefined continuous flow." (second r)))))))

;; ---------------------------------------------------------------------------
;; Claim 12 — m/join: when one branch fails, siblings are cancelled and join
;; fails with that error (upstream-documented; missionary-reference.txt:783).
;; ---------------------------------------------------------------------------

(deftest claim-12-join-cancels-siblings-on-failure
  (testing "a failing branch cancels the sibling and fails join with its error"
    (let [other-cancelled (atom false)
          blocker (fn [_s f] (fn [] (reset! other-cancelled true) (f (ex-info "cancelled" {}))))
          boomer  (fn [_s f] (f (ex-info "boom" {})) (fn [] nil))
          out (promise)]
      ((m/join vector blocker boomer)
       (fn [s] (deliver out [:done s])) (fn [e] (deliver out [:fail (.getMessage e)])))
      (is (= [:fail "boom"] (deref out 5000 :timeout)))
      (is (true? @other-cancelled)))))

;; ---------------------------------------------------------------------------
;; Claim 18 — a Missionary flow is not a reference type; @flow does not deref.
;; ---------------------------------------------------------------------------

(deftest claim-18-flows-not-derefable-with-deref
  (testing "@(m/watch atom) throws (a flow is not IDeref)"
    (is (thrown? ClassCastException (deref (m/watch (atom 0))))))
  (testing "an atom IS derefable"
    (is (= 0 (deref (atom 0))))))
