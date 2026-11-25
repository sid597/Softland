(ns auth-concurrent.step2-probes
  "Empirical verification probes for Step 2 (`PLAN_VALIDATION.md`) on Rama 1.6.0
  `InProcessCluster` and `softland.store.act`:
    - Probe F: Auth's 32 vs Store's 32 (`softland.store.act/has-shape?` & `parse`)
    - Probe B: Multi-topology PState ownership boundary + hybrid microbatch (`invite`
               via sequential `<<batch` barriers) + stream (`local-select>` of
               microbatch-owned PState) + latency comparison
    - Probe C: `end-actor` non-yielding sizing on a shared task (3,000 live actors,
               2,000 live sign-ins, 5,000 open sessions + active `foreign-proxy`
               watches): `PLAN.md` 18,000-seek BFS vs constructed 2-scan (`ao:` +
               `$$live`), measuring sibling-root `who-is` and `ping-task` stalls
    - Probe E: `who-is` 2-seek construction vs `PLAN.md` 3-seek query topology
               across all 8 input classes, verifying identical outputs and exact
               `local-select>` counts."
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [clojure.pprint :as pprint]
            [com.rpl.rama.aggs :as aggs]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.test :as rtest]
            [softland.store.act :as act])
  (:import [clojure.lang Keyword]
           [java.util UUID]
           [java.util.concurrent CompletableFuture TimeUnit]))

;; =============================================================================
;; PROBE F: Auth's 32 vs Store's 32 (`src-store/softland/store/act.clj`)
;; =============================================================================

(defn build-permission-chain
  "Builds a nested store `:permission-id` chain starting at root `P` (`depth 0`
  in Auth, `nesting 1` in `act.clj`), followed by `makers-below-root` delegated
  agent permissions `A_1 .. A_k`."
  [P lane granted-in makers-below-root]
  (reduce (fn [parent-perm i]
            [(UUID. 0 (long (inc i))) lane granted-in parent-perm])
          [P lane granted-in]
          (range makers-below-root)))

(defn run-probe-f []
  (let [P (UUID/fromString "00000000-0000-0000-0000-000000000001")
        lane (UUID/fromString "00000000-0000-0000-0000-0000000000aa")
        granted-in (UUID/fromString "00000000-0000-0000-0000-0000000000bb")
        sess (UUID/fromString "00000000-0000-0000-0000-0000000000cc")
        perm-31 (build-permission-chain P lane granted-in 31)
        perm-32 (build-permission-chain P lane granted-in 32)
        mk-act (fn [who perm]
                 {:version 1
                  :who who
                  :lane lane
                  :permission perm
                  :session sess
                  :facts [{:e who :k :agent/status :v "ok"}]})]
    {:makers-31 {:auth-actor-depth 31
                 :store-permission-nesting 32
                 :has-shape? (act/has-shape? :permission-id perm-31)
                 :parse-verdict (first (keys (act/parse (mk-act (first perm-31) perm-31))))}
     :makers-32 {:auth-actor-depth 32
                 :auth-allows-make-actor? true
                 :store-permission-nesting 33
                 :has-shape? (act/has-shape? :permission-id perm-32)
                 :parse-result (act/parse (mk-act (first perm-32) perm-32))}}))

;; =============================================================================
;; PROBE B: Hybrid Microbatch + Stream & Single-Writer PState Ownership
;; =============================================================================

(defn pick-first-op [curr cand]
  (if (nil? curr) cand curr))

(defmodule HybridAuthProbeMod [setup topologies]
  (declare-depot setup *invites (hash-by :claim))
  (declare-depot setup *root-events (hash-by :root))

  (let [mb (microbatch-topology topologies "mb-onboarding")
        st (stream-topology topologies "stream-roots")]
    (declare-pstate mb $$claims
                    {String (fixed-keys-schema {:person UUID :invited-at Long})})
    (declare-pstate mb $$mb-persons
                    {UUID (fixed-keys-schema {:claim String :invited-at Long})})
    (declare-pstate mb $$mb-decisions
                    {UUID (fixed-keys-schema {:kind Keyword :person UUID :reason Keyword})})
    ;; Ephemeral per-batch scratch PStates cleared at batch end:
    (declare-pstate mb $$person-cand
                    {UUID (fixed-keys-schema {:op-id UUID :claim String :at Long})})
    (declare-pstate mb $$claim-cand
                    {String (fixed-keys-schema {:op-id UUID :person UUID :at Long})})

    (declare-pstate st $$root-notes {UUID String})

    (<<sources mb
      (source> *invites :> %inv)

      ;; Batch 1 (on |hash *claim): check existing claim (repeat & :claim-in-use first!)
      (<<batch
        (%inv :> {:keys [*op-id *person *claim *at]})
        (|hash *claim)
        (local-select> [(keypath *claim)] $$claims :> *existing-c)
        (<<cond
          (case> (and> (some? *existing-c) (= (get *existing-c :person) *person)))
          (|hash *op-id)
          (local-transform> [(keypath *op-id)
                             (termval {:kind :invited :person *person :reason :none})]
                            $$mb-decisions)

          (case> (some? *existing-c))
          (|hash *op-id)
          (local-transform> [(keypath *op-id)
                             (termval {:kind :refused :person *person :reason :claim-in-use})]
                            $$mb-decisions)

          (default>)
          (|hash *person)
          (local-select> [(keypath *person)] $$mb-persons :> *existing-p)
          (<<cond
            (case> (and> (some? *existing-p) (= (get *existing-p :claim) *claim)))
            (|hash *op-id)
            (local-transform> [(keypath *op-id)
                               (termval {:kind :invited :person *person :reason :none})]
                              $$mb-decisions)

            (case> (some? *existing-p))
            (|hash *op-id)
            (local-transform> [(keypath *op-id)
                               (termval {:kind :refused :person *person :reason :person-in-use})]
                              $$mb-decisions)

            (default>)
            (local-select> [(keypath *person)] $$person-cand :> *pc)
            (<<if (nil? *pc)
              (local-transform> [(keypath *person)
                                 (termval {:op-id *op-id :claim *claim :at *at})]
                                $$person-cand)))))

      ;; Batch 2 (on |hash *person -> |hash *claim): contend for free claims
      (<<batch
        (%inv :> {:keys [*op-id *person *claim *at]})
        (|hash *op-id)
        (local-select> [(keypath *op-id)] $$mb-decisions :> *dec)
        (<<if (nil? *dec)
          (|hash *person)
          (local-select> [(keypath *person)] $$person-cand :> *pc)
          (<<if (= (get *pc :op-id) *op-id)
            (|hash *claim)
            (local-select> [(keypath *claim)] $$claim-cand :> *cc)
            (<<if (nil? *cc)
              (local-transform> [(keypath *claim)
                                 (termval {:op-id *op-id :person *person :at *at})]
                                $$claim-cand)
              (else>)
              (|hash *op-id)
              (local-transform> [(keypath *op-id)
                                 (termval {:kind :refused :person *person :reason :claim-in-use})]
                                $$mb-decisions))
            (else>)
            ;; Lost person slot to another claim of same person in this batch;
            ;; wait for Batch 3 to check if this claim was ALSO won by another person!
            (identity nil))))

      ;; Batch 3: commit winning (claim, person) pairs & resolve remaining in-batch losers
      ;; with exact SPEC refusal order (:claim-in-use before :person-in-use)!
      (<<batch
        (%inv :> {:keys [*op-id *person *claim *at]})
        (|hash *op-id)
        (local-select> [(keypath *op-id)] $$mb-decisions :> *dec)
        (<<if (nil? *dec)
          (|hash *claim)
          (local-select> [(keypath *claim)] $$claim-cand :> *cc)
          (<<cond
            (case> (= (get *cc :op-id) *op-id))
            (local-transform> [(keypath *claim)
                               (termval {:person *person :invited-at *at})]
                              $$claims)
            (|hash *person)
            (local-transform> [(keypath *person)
                               (termval {:claim *claim :invited-at *at})]
                              $$mb-persons)
            (|hash *op-id)
            (local-transform> [(keypath *op-id)
                               (termval {:kind :invited :person *person :reason :none})]
                              $$mb-decisions)

            (case> (some? *cc))
            (|hash *op-id)
            (local-transform> [(keypath *op-id)
                               (termval {:kind :refused :person *person :reason :claim-in-use})]
                              $$mb-decisions)

            (default>)
            (|hash *op-id)
            (local-transform> [(keypath *op-id)
                               (termval {:kind :refused :person *person :reason :person-in-use})]
                              $$mb-decisions))))

      ;; Batch 4: remove per-batch candidate keys touched by %inv
      (<<batch
        (%inv :> {:keys [*person *claim]})
        (|hash *person)
        (local-transform> [(keypath *person) NONE>] $$person-cand)
        (|hash *claim)
        (local-transform> [(keypath *claim) NONE>] $$claim-cand)))

    (<<sources st
      (source> *root-events :> {:keys [*root *note]})
      ;; Stream topology reading microbatch-owned PState on colocated task H(root):
      (local-select> [(keypath *root)] $$mb-persons :> *mb-p)
      (<<if (some? *mb-p)
        (local-transform> [(keypath *root) (termval *note)] $$root-notes)
        (ack-return> {:ok true :claim (get *mb-p :claim)})
        (else>)
        (ack-return> {:ok false :reason :no-such-person})))))

(defn await-mb-decision [p-dec op-id timeout-ms]
  (let [deadline (+ (System/nanoTime) (* timeout-ms 1000000))]
    (loop []
      (if-let [res (foreign-select-one [(keypath op-id)] p-dec {:pkey op-id})]
        res
        (if (< (System/nanoTime) deadline)
          (do (Thread/sleep 5) (recur))
          {:timeout true})))))

(defn run-probe-b []
  ;; B1: Verify Rama rejects two topologies writing the same PState
  (let [shared-write-err
        (try
          (eval '(do
                   (use 'com.rpl.rama 'com.rpl.rama.path)
                   (defmodule IllegalDualWriterMod [setup topologies]
                     (declare-depot setup *d1 :random)
                     (let [mb (microbatch-topology topologies "mb")
                           st (stream-topology topologies "st")]
                       (declare-pstate mb $$shared {Long Long})
                       (<<sources mb
                         (source> *d1 :> %mb)
                         (%mb :> *x)
                         (|hash *x)
                         (local-transform> [(keypath *x) (termval 1)] $$shared))
                       (<<sources st
                         (source> *d1 :> *x)
                         (|hash *x)
                         (local-transform> [(keypath *x) (termval 2)] $$shared))))
                   (with-open [ipc (com.rpl.rama.test/create-ipc)]
                     (com.rpl.rama.test/launch-module! ipc IllegalDualWriterMod {:tasks 2 :threads 1})
                     (let [mn (get-module-name IllegalDualWriterMod)
                           d (foreign-depot ipc mn "*d1")
                           p (foreign-pstate ipc mn "$$shared")]
                       (foreign-append! d 42 :ack)
                       (Thread/sleep 400)
                       {:launched-without-error true
                        :value-after-both (foreign-select-one [(keypath 42)] p {:pkey 42})}))))
          (catch Throwable t
            (let [root-cause (loop [e t] (if-let [c (.getCause e)] (recur c) e))]
              {:threw (class root-cause) :message (.getMessage root-cause)})))]
    ;; B2: Run HybridAuthProbeMod
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc HybridAuthProbeMod {:tasks 2 :threads 2})
      (let [mod-name (get-module-name HybridAuthProbeMod)
            d-inv (foreign-depot ipc mod-name "*invites")
            d-root (foreign-depot ipc mod-name "*root-events")
            p-dec (foreign-pstate ipc mod-name "$$mb-decisions")
            P1 (UUID/randomUUID)
            P2 (UUID/randomUUID)
            op1 (UUID/randomUUID)
            op2 (UUID/randomUUID)
            op3 (UUID/randomUUID)
            ;; Let microbatch enter idle polling mode briefly, then measure single invite latency
            _ (Thread/sleep 250)
            t0 (System/nanoTime)
            ;; Submit conflicting 3-invite triangle (Witness 9 / 3C-01):
            ;; (P2, C1), (P1, C2), and (P1, C1)
            _ (foreign-append! d-inv {:op-id op2 :person P2 :claim "alice@example.com" :at 110} :append-ack)
            _ (foreign-append! d-inv {:op-id op3 :person P1 :claim "bob@example.com" :at 120} :append-ack)
            _ (foreign-append! d-inv {:op-id op1 :person P1 :claim "alice@example.com" :at 100} :append-ack)
            r2 (await-mb-decision p-dec op2 5000)
            r3 (await-mb-decision p-dec op3 5000)
            r1 (await-mb-decision p-dec op1 5000)
            mb-ms (/ (- (System/nanoTime) t0) 1e6)
            ;; Now test stream topology reading microbatch-owned $$mb-persons!
            ts0 (System/nanoTime)
            st-ack-p1 (foreign-append! d-root {:root P1 :note "ready"} :ack)
            st-ms (/ (- (System/nanoTime) ts0) 1e6)
            ;; Also test single idle microbatch write latency after 300ms sleep:
            _ (Thread/sleep 300)
            op4 (UUID/randomUUID)
            P4 (UUID/randomUUID)
            t4 (System/nanoTime)
            _ (foreign-append! d-inv {:op-id op4 :person P4 :claim "carol@example.com" :at 200} :append-ack)
            r4 (await-mb-decision p-dec op4 5000)
            mb-idle-ms (/ (- (System/nanoTime) t4) 1e6)]
        {:dual-writer-rejection shared-write-err
         :triangle-results {:op2-P2-C1 r2 :op3-P1-C2 r3 :op1-P1-C1 r1}
         :mb-batch-latency-ms (Double/parseDouble (format "%.2f" mb-ms))
         :mb-idle-poll-latency-ms (Double/parseDouble (format "%.2f" mb-idle-ms))
         :stream-read-mb-pstate-ack st-ack-p1
         :stream-latency-ms (Double/parseDouble (format "%.2f" st-ms))}))))

;; =============================================================================
;; PROBE C: `end-actor` Non-Yielding Sizing & Shared-Task Stall
;; =============================================================================

(definterface IProbeRootDatum)
(defrecord PRoot [own-lane own-lane-made?] IProbeRootDatum)
(defrecord PActor [actor made-by depth label ended? seq] IProbeRootDatum)
(defrecord PActorOrder [actor made-by label ended? seq] IProbeRootDatum)
(defrecord PChildEdge [child] IProbeRootDatum)
(defrecord PSignIn [sign-in actor kind initial-hash hash until prev-hash prev-until ended? begun label seq] IProbeRootDatum)
(defrecord PActorSignInEdge [sign-in until] IProbeRootDatum)
(defrecord PSession [session sign-in until closed? driven-by] IProbeRootDatum)
(defrecord PSignInSessionEdge [session until] IProbeRootDatum)
(defrecord PExpiry [kind id parent-id until] IProbeRootDatum)

(definterface IProbeLive)
(defrecord PLiveSignIn [sign-in actor kind initial-hash hash until prev-hash prev-until begun label seq] IProbeLive)
(defrecord PLiveSession [session sign-in until driven-by] IProbeLive)

(defn hex16 [^long n]
  (format "%016x" n))

(defn hex-ts [^long t]
  (format "%016x" (bit-xor t Long/MIN_VALUE)))

(defn k-root [p] (str p "\u0000r"))
(defn k-actor [p a] (str p "\u0000a:" a))
(defn k-ao [p ^long s] (str p "\u0000ao:" (hex16 s)))
(defn k-ao-from [p ^long s] (str p "\u0000ao:" (hex16 s)))
(defn k-ao-hi [p] (str p "\u0000ao;"))
(defn k-ch [p b a] (str p "\u0000ch:" b ":" a))
(defn k-ch-lo [p b] (str p "\u0000ch:" b ":"))
(defn k-ch-hi [p b] (str p "\u0000ch:" b ";"))
(defn k-signin [p g] (str p "\u0000g:" g))
(defn k-ag [p a g] (str p "\u0000ag:" a ":" g))
(defn k-ag-lo [p a] (str p "\u0000ag:" a ":"))
(defn k-ag-hi [p a] (str p "\u0000ag:" a ";"))
(defn k-session [p s] (str p "\u0000s:" s))
(defn k-sg [p g s] (str p "\u0000sg:" g ":" s))
(defn k-sg-lo [p g] (str p "\u0000sg:" g ":"))
(defn k-sg-hi [p g] (str p "\u0000sg:" g ";"))
(defn k-exp [p ^long until kind-ch id] (str p "\u0000exp:" (hex-ts until) ":" kind-ch ":" id))
(defn k-live-lo [p] (str p "\u0000"))
(defn k-live-hi [p] (str p "\u0001"))

(defn map-keys-vec [m] (if (seq m) (vec (keys m)) []))
(defn map-vals-children [m] (if (seq m) (mapv :child (vals m)) []))
(defn mark-actor-ended [^PActor a] (assoc a :ended? true))
(defn make-ao-ended [^PActor a] (->PActorOrder (:actor a) (:made-by a) (:label a) true (:seq a)))
(defn mark-signin-ended [^PSignIn g] (assoc g :ended? true))
(defn mark-session-closed [^PSession s] (assoc s :closed? true))
(defn vec-pop-first [v] [(first v) (subvec v 1)])
(defn vec-append-all [v more] (into v more))
(defn most-significant-bits [^UUID u] (.getMostSignificantBits u))

(defn build-root-seed-entries
  "Generates deterministic entries for `n-actors` live agents (rooted under `A1`,
  starting with a 32-deep spine `P -> A1 -> ... -> A32` and wide fan-out across
  depths 1..32), `n-signins` live sign-ins, and `n-sessions` open sessions."
  [root own-lane n-actors n-signins n-sessions]
  (let [actors (mapv (fn [i]
                       (let [id (UUID. (most-significant-bits root) (long (inc i)))
                             made-by (cond
                                       (zero? i) root
                                       (< i 32) (UUID. (most-significant-bits root) (long i))
                                       :else (UUID. (most-significant-bits root) (long (inc (mod i 31)))))
                             depth (cond
                                     (zero? i) 1
                                     (< i 32) (inc i)
                                     :else (+ 2 (mod i 31)))]
                         {:actor id :made-by made-by :depth depth :label (str "ag-" i) :seq (long i)}))
                     (range n-actors))
        signins (mapv (fn [j]
                        (let [gid (UUID. (bit-xor (most-significant-bits root) 0x1111) (long (inc j)))
                              actor (:actor (nth actors (mod j n-actors)))
                              until (+ 100000 (long j))]
                          {:sign-in gid :actor actor :until until :seq (long j)}))
                      (range n-signins))
        sessions (mapv (fn [k]
                         (let [sid (UUID. (bit-xor (most-significant-bits root) 0x2222) (long (inc k)))
                               g (nth signins (mod k n-signins))
                               until (+ 200000 (long k))]
                           {:session sid :sign-in (:sign-in g) :until until :driven-by :person}))
                       (range n-sessions))
        rd-entries (transient [[(k-root root) (->PRoot own-lane true)]])
        live-entries (transient [])]
    (doseq [{:keys [actor made-by depth label seq]} actors]
      (conj! rd-entries [(k-actor root actor) (->PActor actor made-by depth label false seq)])
      (conj! rd-entries [(k-ao root seq) (->PActorOrder actor made-by label false seq)])
      (conj! rd-entries [(k-ch root made-by actor) (->PChildEdge actor)]))
    (doseq [{:keys [sign-in actor until seq]} signins]
      (conj! rd-entries [(k-signin root sign-in)
                         (->PSignIn sign-in actor :program "h0" "h0" until nil nil false 10 "lbl" seq)])
      (conj! rd-entries [(k-ag root actor sign-in) (->PActorSignInEdge sign-in until)])
      (conj! rd-entries [(k-exp root until "g" sign-in) (->PExpiry :sign-in sign-in actor until)])
      (conj! live-entries [(k-signin root sign-in)
                           (->PLiveSignIn sign-in actor :program "h0" "h0" until nil nil 10 "lbl" seq)]))
    (doseq [{:keys [session sign-in until driven-by]} sessions]
      (conj! rd-entries [(k-session root session) (->PSession session sign-in until false driven-by)])
      (conj! rd-entries [(k-sg root sign-in session) (->PSignInSessionEdge session until)])
      (conj! rd-entries [(k-exp root until "s" session) (->PExpiry :session session sign-in until)])
      (conj! live-entries [(k-session root session) (->PLiveSession session sign-in until driven-by)]))
    {:target-actor (:actor (first actors))
     :sample-signin (:sign-in (first signins))
     :sample-session (:session (first sessions))
     :rd-entries (persistent! rd-entries)
     :live-entries (persistent! live-entries)}))

(defn plan-2scan-cascade
  "Pure Clojure planner for the constructed 2-scan `end-actor`:
  Given `target-rec` (`PActor`), `ao-map` (`ao:` range from `target.seq` upward),
  and `live-map` (`$$live` range for `root`), computes all writes in memory with
  ZERO additional PState seeks."
  [root ^PActor target-rec ao-map live-map]
  (let [ended-actors (volatile! #{(:actor target-rec)})
        rd-upserts (transient [[(k-actor root (:actor target-rec)) (assoc target-rec :ended? true)]])
        rd-deletes (transient [])
        live-deletes (transient [])
        ended-g-set (volatile! #{})
        actor-cnt (volatile! 0)
        signin-cnt (volatile! 0)
        session-cnt (volatile! 0)]
    ;; Pass 1: forward scan over ao-map (since seq(maker) < seq(child) always holds!)
    (doseq [[ao-k ^PActorOrder ao] ao-map]
      (let [aid (:actor ao)
            mb (:made-by ao)]
        (when (and (not (:ended? ao))
                   (or (= aid (:actor target-rec))
                       (contains? @ended-actors mb)))
          (vswap! ended-actors conj aid)
          (vswap! actor-cnt inc)
          (conj! rd-upserts [ao-k (assoc ao :ended? true)])
          ;; Mark a:<A> ended as well (preserving depth/seq; depth unused once ended? is true):
          (conj! rd-upserts [(k-actor root aid) (->PActor aid mb 1 (:label ao) true (:seq ao))]))))
    ;; Pass 2: scan live-map (`g:` comes before `s:` lexicographically: 'g' < 's'!)
    (doseq [[lk lv] live-map]
      (cond
        (instance? PLiveSignIn lv)
        (let [^PLiveSignIn g lv]
          (when (contains? @ended-actors (:actor g))
            (vswap! ended-g-set conj (:sign-in g))
            (vswap! signin-cnt inc)
            (conj! live-deletes lk)
            (conj! rd-deletes (k-exp root (:until g) "g" (:sign-in g)))
            (conj! rd-upserts [(k-signin root (:sign-in g))
                               (->PSignIn (:sign-in g) (:actor g) (:kind g)
                                          (:initial-hash g) (:hash g) (:until g)
                                          (:prev-hash g) (:prev-until g)
                                          true (:begun g) (:label g) (:seq g))])))

        (instance? PLiveSession lv)
        (let [^PLiveSession s lv]
          (when (contains? @ended-g-set (:sign-in s))
            (vswap! session-cnt inc)
            (conj! live-deletes lk)
            (conj! rd-deletes (k-exp root (:until s) "s" (:session s)))
            (conj! rd-upserts [(k-session root (:session s))
                               (->PSession (:session s) (:sign-in s) (:until s) true (:driven-by s))])))))
    {:rd-upserts (persistent! rd-upserts)
     :rd-deletes (persistent! rd-deletes)
     :live-deletes (persistent! live-deletes)
     :stats {:actors @actor-cnt :sign-ins @signin-cnt :sessions @session-cnt}}))

(defmodule EndActorSizingProbeMod [setup topologies]
  (declare-depot setup *d (hash-by :root))
  (let [s (stream-topology topologies "auth")]
    (declare-pstate s $$root-data {String IProbeRootDatum})
    (declare-pstate s $$live {String IProbeLive})
    (set-launch-pstate-dynamic-option! setup "$$live" "pstate.proxy.limit" 20000)

    (<<sources s
      (source> *d :> {:keys [*op *root *target *rd-entries *live-entries]})
      (<<cond
        (case> (= *op :seed-bulk))
        (loop<- [*xs *rd-entries :> *done1]
          (<<if (empty? *xs)
            (:> true)
            (else>)
            (vec-pop-first *xs :> [[*k *v] *rest])
            (local-transform> [(keypath *k) (termval *v)] $$root-data)
            (continue> *rest)))
        (loop<- [*xs *live-entries :> *done2]
          (<<if (empty? *xs)
            (:> true)
            (else>)
            (vec-pop-first *xs :> [[*k *v] *rest])
            (local-transform> [(keypath *k) (termval *v)] $$live)
            (continue> *rest)))
        (ack-return> :seeded)

        ;; PLAN.md's non-yielding BFS cascade (PLAN.md:658-668):
        ;; 3 seeks/actor (a:, ag:, ch:) + 2 seeks/sign-in (g:, sg:) + 1 seek/session (s:)
        (case> (= *op :end-actor-plan-bfs))
        (loop<- [*q [*target] *ac 0 *gc 0 *sc 0 :> *stats]
          (<<if (empty? *q)
            (:> {:actors *ac :sign-ins *gc :sessions *sc})
            (else>)
            (vec-pop-first *q :> [*x *q-rest])
            (local-select> [(keypath (k-actor *root *x))] $$root-data :> *arec)
            (<<if (or> (nil? *arec) (get *arec :ended?))
              (continue> *q-rest *ac *gc *sc)
              (else>)
              (local-transform> [(keypath (k-actor *root *x)) (termval (mark-actor-ended *arec))] $$root-data)
              (local-transform> [(keypath (k-ao *root (get *arec :seq))) (termval (make-ao-ended *arec))] $$root-data)
              (local-select> [(sorted-map-range (k-ag-lo *root *x) (k-ag-hi *root *x))] $$root-data :> *ag-map)
              (map-keys-vec *ag-map :> *ag-keys)
              (loop<- [*gk-q *ag-keys *gc-acc *gc *sc-acc *sc :> [*gc-next *sc-next]]
                (<<if (empty? *gk-q)
                  (:> [*gc-acc *sc-acc])
                  (else>)
                  (vec-pop-first *gk-q :> [*ag-k *gk-rest])
                  (get *ag-map *ag-k :> *ag-edge)
                  (get *ag-edge :sign-in :> *gid)
                  (local-select> [(keypath (k-signin *root *gid))] $$root-data :> *grec)
                  (local-transform> [(keypath (k-signin *root *gid)) (termval (mark-signin-ended *grec))] $$root-data)
                  (local-transform> [(keypath (k-signin *root *gid)) NONE>] $$live)
                  (local-transform> [(keypath (k-exp *root (get *grec :until) "g" *gid)) NONE>] $$root-data)
                  (local-transform> [(keypath *ag-k) NONE>] $$root-data)
                  (local-select> [(sorted-map-range (k-sg-lo *root *gid) (k-sg-hi *root *gid))] $$root-data :> *sg-map)
                  (map-keys-vec *sg-map :> *sg-keys)
                  (loop<- [*sk-q *sg-keys *sc2 *sc-acc :> *sc-out]
                    (<<if (empty? *sk-q)
                      (:> *sc2)
                      (else>)
                      (vec-pop-first *sk-q :> [*sg-k *sk-rest])
                      (get *sg-map *sg-k :> *sg-edge)
                      (get *sg-edge :session :> *sid)
                      (local-select> [(keypath (k-session *root *sid))] $$root-data :> *srec)
                      (local-transform> [(keypath (k-session *root *sid)) (termval (mark-session-closed *srec))] $$root-data)
                      (local-transform> [(keypath (k-session *root *sid)) NONE>] $$live)
                      (local-transform> [(keypath (k-exp *root (get *srec :until) "s" *sid)) NONE>] $$root-data)
                      (local-transform> [(keypath *sg-k) NONE>] $$root-data)
                      (continue> *sk-rest (inc *sc2))))
                  (continue> *gk-rest (inc *gc-acc) *sc-out)))
              (local-select> [(sorted-map-range (k-ch-lo *root *x) (k-ch-hi *root *x))] $$root-data :> *ch-map)
              (map-vals-children *ch-map :> *children)
              (vec-append-all *q-rest *children :> *q-next)
              (continue> *q-next (inc *ac) *gc-next *sc-next))))
        (ack-return> *stats)

        ;; Constructed 2-scan cascade (1 point seek + ao: forward scan + $$live scan):
        (case> (= *op :end-actor-2scan))
        (local-select> [(keypath (k-actor *root *target))] $$root-data :> *trec)
        (local-select> [(sorted-map-range (k-ao-from *root (get *trec :seq)) (k-ao-hi *root))] $$root-data :> *ao-map)
        (local-select> [(sorted-map-range (k-live-lo *root) (k-live-hi *root))] $$live :> *live-map)
        (plan-2scan-cascade *root *trec *ao-map *live-map :> {:keys [*rd-upserts *rd-deletes *live-deletes *stats]})
        (loop<- [*xs *rd-upserts :> *d1]
          (<<if (empty? *xs)
            (:> true)
            (else>)
            (vec-pop-first *xs :> [[*k *v] *rest])
            (local-transform> [(keypath *k) (termval *v)] $$root-data)
            (continue> *rest)))
        (loop<- [*ks *rd-deletes :> *d2]
          (<<if (empty? *ks)
            (:> true)
            (else>)
            (vec-pop-first *ks :> [*k *rest])
            (local-transform> [(keypath *k) NONE>] $$root-data)
            (continue> *rest)))
        (loop<- [*ks *live-deletes :> *d3]
          (<<if (empty? *ks)
            (:> true)
            (else>)
            (vec-pop-first *ks :> [*k *rest])
            (local-transform> [(keypath *k) NONE>] $$live)
            (continue> *rest)))
        (ack-return> *stats))))

  (<<query-topology topologies "who-is" [*root *g *s :> *res]
    (|hash *root)
    (local-select> [(keypath (k-root *root))] $$root-data :> *r)
    (local-select> [(keypath (k-signin *root *g))] $$root-data :> *sig)
    (local-select> [(keypath (k-session *root *s))] $$root-data :> *sess)
    (hash-map :own-lane (get *r :own-lane) :ended? (get *sig :ended?) :closed? (get *sess :closed?) :> *res)
    (|origin))

  (<<query-topology topologies "ping-task" [*task-id :> *tid]
    (|direct *task-id)
    (ops/current-task-id :> *tid)
    (|origin)))

(defn find-same-task-uuids [q-task-of n-needed target-tid]
  (loop [acc []]
    (if (= (count acc) n-needed)
      acc
      (let [u (UUID/randomUUID)
            tid (foreign-invoke-query q-task-of u)]
        (if (= tid target-tid)
          (recur (conj acc u))
          (recur acc))))))

(defmodule TaskFinderHelper [setup topologies]
  (declare-depot setup *d (hash-by identity))
  (<<query-topology topologies "task-of" [*k :> *tid]
    (|hash *k)
    (ops/current-task-id :> *tid)
    (|origin)))

(defn chunk-seed! [d root rd-entries live-entries]
  (doseq [chunk (partition-all 400 rd-entries)]
    (foreign-append! d {:op :seed-bulk :root root :rd-entries (vec chunk) :live-entries []} :ack))
  (doseq [chunk (partition-all 400 live-entries)]
    (foreign-append! d {:op :seed-bulk :root root :rd-entries [] :live-entries (vec chunk)} :ack)))

(defn safe-measure! [label f]
  (try
    (f)
    (catch Throwable t
      (let [rc (loop [e t] (if-let [c (.getCause e)] (recur c) e))]
        {:failed true :label label :exception (str (class rc)) :message (.getMessage rc)}))))

(defn measure-end-actor-run!
  [d p-live q-who q-ping op-kw P-heavy seed-heavy P-sib seed-sib]
  (chunk-seed! d P-heavy (:rd-entries seed-heavy) (:live-entries seed-heavy))
  (chunk-seed! d P-sib (:rd-entries seed-sib) (:live-entries seed-sib))
  (let [cb-cnt (atom 0)
        pf-h (foreign-proxy [(sorted-map-range (k-live-lo P-heavy) (k-live-hi P-heavy))] p-live
                            {:pkey P-heavy :callback-fn (fn [_ _ _] (swap! cb-cnt inc))})
        pf-s (foreign-proxy [(sorted-map-range (k-live-lo P-sib) (k-live-hi P-sib))] p-live
                            {:pkey P-sib})
        t0 (System/nanoTime)
        end-fut (foreign-append-async! d {:op op-kw :root P-heavy :target (:target-actor seed-heavy)
                                          :rd-entries [] :live-entries []} :ack)
        _ (Thread/sleep 2)
        tw0 (System/nanoTime)
        who-fut (foreign-invoke-query-async q-who P-sib (:sample-signin seed-sib) (:sample-session seed-sib))
        who-ms-fut (.thenApply who-fut (reify java.util.function.Function
                                         (apply [_ r] {:res r :ms (/ (- (System/nanoTime) tw0) 1e6)})))
        tp0 (System/nanoTime)
        ping-fut (foreign-invoke-query-async q-ping 0)
        ping-ms-fut (.thenApply ping-fut (reify java.util.function.Function
                                           (apply [_ r] {:res r :ms (/ (- (System/nanoTime) tp0) 1e6)})))
        end-ack (.get end-fut 35 TimeUnit/SECONDS)
        end-ms (/ (- (System/nanoTime) t0) 1e6)
        sib-who (.get who-ms-fut 35 TimeUnit/SECONDS)
        sib-ping (.get ping-ms-fut 35 TimeUnit/SECONDS)
        _ (Thread/sleep 30)
        post-who (foreign-invoke-query q-who P-heavy (:sample-signin seed-heavy) (:sample-session seed-heavy))]
    (close! pf-h)
    (close! pf-s)
    {:end-actor-ack end-ack
     :end-actor-ms (Double/parseDouble (format "%.2f" end-ms))
     :sibling-who-is-stall-ms (Double/parseDouble (format "%.2f" (:ms sib-who)))
     :ping-task-stall-ms (Double/parseDouble (format "%.2f" (:ms sib-ping)))
     :exceeds-3s-ping-ceiling? (> (:ms sib-ping) 3000.0)
     :exceeds-10s-client-timeout? (> end-ms 10000.0)
     :exceeds-30s-rama-sync-timeout? (> end-ms 30000.0)
     :proxy-callbacks @cb-cnt
     :post-who-is post-who}))

(defn run-probe-c []
  (let [ipc (rtest/create-ipc)]
    (try
      (rtest/launch-module! ipc EndActorSizingProbeMod {:tasks 2 :threads 2})
      (rtest/launch-module! ipc TaskFinderHelper {:tasks 2 :threads 1})
      (let [mod-name (get-module-name EndActorSizingProbeMod)
            tf-name (get-module-name TaskFinderHelper)
            d (foreign-depot ipc mod-name "*d")
            p-live (foreign-pstate ipc mod-name "$$live")
            q-who (foreign-query ipc mod-name "who-is")
            q-ping (foreign-query ipc mod-name "ping-task")
            q-tid (foreign-query ipc tf-name "task-of")
            [P1 P2 P3 P4 P5 P6] (find-same-task-uuids q-tid 6 0)
            L (UUID/randomUUID)
            sib-seed #(build-root-seed-entries % L 2 1 1)
            s2-init (sib-seed P2)
            _ (chunk-seed! d P2 (:rd-entries s2-init) (:live-entries s2-init))
            tb0 (System/nanoTime)
            _ (foreign-invoke-query q-who P2 (:sample-signin s2-init) (:sample-session s2-init))
            idle-who-ms (/ (- (System/nanoTime) tb0) 1e6)
            tp0 (System/nanoTime)
            _ (foreign-invoke-query q-ping 0)
            idle-ping-ms (/ (- (System/nanoTime) tp0) 1e6)
            ;; 1. PLAN.md BFS at medium scale (600 actors, 400 sign-ins, 1,000 sessions = 3,600 seeks)
            res-bfs-600 (safe-measure!
                         :plan-bfs-600
                         #(measure-end-actor-run!
                           d p-live q-who q-ping :end-actor-plan-bfs
                           P5 (build-root-seed-entries P5 L 600 400 1000)
                           P6 (sib-seed P6)))
            ;; 2. Constructed 2-scan at medium scale (600 actors, 400 sign-ins, 1,000 sessions = 3 seeks)
            res-2scan-600 (safe-measure!
                           :2scan-600
                           #(measure-end-actor-run!
                             d p-live q-who q-ping :end-actor-2scan
                             P3 (build-root-seed-entries P3 L 600 400 1000)
                             P4 (sib-seed P4)))
            ;; 3. PLAN.md BFS at full scale (3,000 actors, 2,000 sign-ins, 5,000 sessions = 18,000 seeks)
            res-bfs-3k (safe-measure!
                        :plan-bfs-3k
                        #(measure-end-actor-run!
                          d p-live q-who q-ping :end-actor-plan-bfs
                          P1 (build-root-seed-entries P1 L 3000 2000 5000)
                          P2 s2-init))]
        {:idle-baseline {:who-is-ms (Double/parseDouble (format "%.2f" idle-who-ms))
                         :ping-task-ms (Double/parseDouble (format "%.2f" idle-ping-ms))}
         :plan-bfs-600a-400g-1000s (assoc res-bfs-600 :seeks 3600)
         :constructed-2scan-600a-400g-1000s (assoc res-2scan-600 :seeks 3)
         :plan-bfs-3000a-2000g-5000s (assoc res-bfs-3k :seeks 18000)})
      (finally
        (try (.close ipc) (catch Throwable _ nil))))))

;; =============================================================================
;; PROBE E: `who-is` 2-Seek Construction vs `PLAN.md` 3-Seek Query Topology
;; =============================================================================

(def seek-counter (atom 0))
(defn bump-seek! [x] (swap! seek-counter inc) x)

(defrecord ERoot [state own-lane own-lane-made?])
(defrecord ESignIn [actor kind hash until prev-hash prev-until ended? own-lane own-lane-made-at-begin?])
(defrecord ESession [sign-in until closed? driven-by])
(defrecord EActor [ended?])

(defn assemble-who-is [root r sig sess actor-ended?]
  (let [entered? (and (some? r) (= :entered (:state r)))
        own-lane (cond
                    (some? sig) (:own-lane sig)
                    entered? (:own-lane r)
                    :else nil)
        made? (cond
                (and (some? sig) (:own-lane-made-at-begin? sig)) true
                entered? (boolean (:own-lane-made? r))
                :else false)]
    {:sign-in (when (some? sig)
                {:actor (:actor sig)
                 :kind (:kind sig)
                 :hash (:hash sig)
                 :until (:until sig)
                 :previous (when (:prev-hash sig) {:hash (:prev-hash sig) :until (:prev-until sig)})
                 :ended? (boolean (:ended? sig))
                 :actor-ended? (boolean actor-ended?)})
     :session (when (and (some? sess) (or (some? sig) entered?))
                {:sign-in (:sign-in sess)
                 :until (:until sess)
                 :closed? (boolean (:closed? sess))
                 :driven-by (:driven-by sess)})
     :own-lane own-lane
     :own-lane-made? made?}))

(defmodule WhoIs2SeekProbeMod [setup topologies]
  (declare-depot setup *d (hash-by :root))
  (let [s (stream-topology topologies "s")]
    (declare-pstate s $$rd {String Object})
    (<<sources s
      (source> *d :> {:keys [*op *root *k *v]})
      (<<cond
        (case> (= *op :put))
        (local-transform> [(keypath *k) (termval *v)] $$rd)
        (ack-return> :ok))))

  ;; PLAN.md's query topology (reads r -> g -> s -> a; 3 seeks on dominant HTTP request)
  (<<query-topology topologies "who-is-plan" [*root *g *s :> *out]
    (|hash *root)
    (local-select> [(keypath (k-root *root))] $$rd :> *r0)
    (bump-seek! *r0 :> *r)
    (<<if (and> (some? *r) (= (get *r :state) :entered))
      (local-select> [(keypath (k-signin *root *g))] $$rd :> *sig0)
      (bump-seek! *sig0 :> *sig)
      (<<if (some? *s)
        (local-select> [(keypath (k-session *root *s))] $$rd :> *sess0)
        (bump-seek! *sess0 :> *sess)
        (else>)
        (identity nil :> *sess))
      (<<if (and> (some? *sig) (get *sig :ended?) (not= (get *sig :actor) *root))
        (local-select> [(keypath (k-actor *root (get *sig :actor)))] $$rd :> *a0)
        (bump-seek! *a0 :> *arec)
        (get *arec :ended? :> *ae)
        (else>)
        (identity false :> *ae))
      (assemble-who-is *root *r *sig *sess *ae :> *out)
      (else>)
      (assemble-who-is *root nil nil nil false :> *out))
    (|origin))

  ;; Constructed 2-seek query topology:
  ;; Reads g:<G> FIRST; skips reading "<P>\0r" whenever g:<G> exists AND had
  ;; own-lane-made?=true at begin-sign-in (99.99%+ of requests)!
  (<<query-topology topologies "who-is-2seek" [*root *g *s :> *out]
    (|hash *root)
    (local-select> [(keypath (k-signin *root *g))] $$rd :> *sig0)
    (bump-seek! *sig0 :> *sig)
    (<<if (and> (some? *sig) (get *sig :own-lane-made-at-begin?))
      (identity nil :> *r)
      (identity true :> *known-entered?)
      (else>)
      (local-select> [(keypath (k-root *root))] $$rd :> *r0)
      (bump-seek! *r0 :> *r)
      (= (get *r :state) :entered :> *known-entered?))
    (<<if *known-entered?
      (<<if (some? *s)
        (local-select> [(keypath (k-session *root *s))] $$rd :> *sess0)
        (bump-seek! *sess0 :> *sess)
        (else>)
        (identity nil :> *sess))
      (<<if (and> (some? *sig) (get *sig :ended?) (not= (get *sig :actor) *root))
        (local-select> [(keypath (k-actor *root (get *sig :actor)))] $$rd :> *a0)
        (bump-seek! *a0 :> *arec)
        (get *arec :ended? :> *ae)
        (else>)
        (identity false :> *ae))
      (assemble-who-is *root *r *sig *sess *ae :> *out)
      (else>)
      (assemble-who-is *root nil nil nil false :> *out))
    (|origin)))

(defn run-who-case [q-plan q-2seek label root g s]
  (reset! seek-counter 0)
  (let [r-plan (foreign-invoke-query q-plan root g s)
        s-plan @seek-counter
        _ (reset! seek-counter 0)
        r-2seek (foreign-invoke-query q-2seek root g s)
        s-2seek @seek-counter]
    {:case label
     :equal? (= r-plan r-2seek)
     :plan-seeks s-plan
     :two-seek-seeks s-2seek
     :result r-2seek}))

(defn run-probe-e []
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc WhoIs2SeekProbeMod {:tasks 2 :threads 1})
    (let [mod-name (get-module-name WhoIs2SeekProbeMod)
          d (foreign-depot ipc mod-name "*d")
          q-plan (foreign-query ipc mod-name "who-is-plan")
          q-2seek (foreign-query ipc mod-name "who-is-2seek")
          P-unk (UUID/randomUUID)
          P-wait (UUID/randomUUID)
          P-unmade (UUID/randomUUID)
          P-made (UUID/randomUUID)
          L1 (UUID/randomUUID)
          L2 (UUID/randomUUID)
          A-ended (UUID/randomUUID)
          G-early (UUID/randomUUID)
          G-live (UUID/randomUUID)
          G-ended-ag (UUID/randomUUID)
          G-miss (UUID/randomUUID)
          S1 (UUID/randomUUID)
          put! (fn [r k v] (foreign-append! d {:op :put :root r :k k :v v} :ack))]
      (put! P-wait (k-root P-wait) (->ERoot :invited nil false))
      (put! P-unmade (k-root P-unmade) (->ERoot :entered L1 false))
      (put! P-unmade (k-signin P-unmade G-early) (->ESignIn P-unmade :browser "h1" 500 nil nil false L1 false))
      (put! P-made (k-root P-made) (->ERoot :entered L2 true))
      (put! P-made (k-actor P-made A-ended) (->EActor true))
      (put! P-made (k-signin P-made G-live) (->ESignIn P-made :browser "h2" 900 nil nil false L2 true))
      (put! P-made (k-signin P-made G-ended-ag) (->ESignIn A-ended :program "h3" 900 nil nil true L2 true))
      (put! P-made (k-session P-made S1) (->ESession G-live 1200 false {:kind :person}))
      (let [c1 (run-who-case q-plan q-2seek "1. unknown root (S=S1)" P-unk G-miss S1)
            c2 (run-who-case q-plan q-2seek "2. waiting root (S=S1)" P-wait G-miss S1)
            c3 (run-who-case q-plan q-2seek "3. entered (made?=false), missing G" P-unmade G-miss nil)
            c4 (run-who-case q-plan q-2seek "4. entered (made?=false), G begun before made" P-unmade G-early nil)
            ;; Now flip P-unmade's own-lane-made? to true on r ONLY (0 updates to G-early!):
            _ (put! P-unmade (k-root P-unmade) (->ERoot :entered L1 true))
            c5 (run-who-case q-plan q-2seek "5. G begun before made, AFTER own-lane-made" P-unmade G-early nil)
            c6 (run-who-case q-plan q-2seek "6. dominant S=nil (entered made?=true, live G)" P-made G-live nil)
            c7 (run-who-case q-plan q-2seek "7. dominant HTTP S=S1 (entered made?=true, live G)" P-made G-live S1)
            c8 (run-who-case q-plan q-2seek "8. ended agent G (A!=P), S=S1" P-made G-ended-ag S1)
            cases [c1 c2 c3 c4 c5 c6 c7 c8]]
        {:all-equal? (every? :equal? cases)
         :cases (mapv #(dissoc % :result) cases)}))))

(defn -main [& _]
  (println "=== PROBE F: Auth's 32 vs Store's 32 (softland.store.act) ===")
  (let [rf (run-probe-f)]
    (pprint/pprint rf))
  (println "\n=== PROBE B: Hybrid Microbatch + Stream & PState Ownership ===")
  (let [rb (run-probe-b)]
    (pprint/pprint rb))
  (println "\n=== PROBE E: who-is 2-Seek Construction vs PLAN.md 3-Seek ===")
  (let [re (run-probe-e)]
    (pprint/pprint re))
  (println "\n=== PROBE C: end-actor Non-Yielding Sizing & Shared-Task Stall ===")
  (let [rc (run-probe-c)]
    (pprint/pprint rc))
  (shutdown-agents)
  (System/exit 0))
