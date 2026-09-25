(ns rig.store.lock-test
  "Pure tests of stage 2's locks (PLAN-locks-and-forgetting.md, 'Namespaces
  and tests', lock_test 1 to 6), no cluster.

  1. The model's A cases, both directions, at the lock level (R8): the
     wrap table and `wrap-closed` against formal.model's `wrap` (reading
     `:owner-required`) and `wrap-closed`, on every A case's inputs and on
     generated ones; then each case's lock record built with real locks,
     the case's forgets applied by destroying person locks, and `unwrap`
     then `open` giving the value exactly when the model says `:open`.
  2. Crypto round trips, the digest's properties, the scheme tag.
  3. The grammar (`subjects-of`).
  4. Placement and the per-act plan.
  5. No throw on garbage, truncated, swapped and wrong-length inputs.
  6. The record path's resend check.
  Also: the value checks in L27's order, the decision's lock part (the
  refusals after stage 1's, the effects), and a create-test-pstate check
  that every write a stage 2 decision computes fits `$$layers` and
  `$$persons` (a schema violation inside the topology kills the worker)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [formal.model :as fm]
            [formal.scenarios :as fs]
            [rig.store.envelope :as env]
            [rig.store.gate :as gate]
            [rig.store.grammar :as grammar]
            [rig.store.locks :as l]
            [rig.store.module :as m])
  (:import [java.util Arrays UUID]))

;; ------------------------------------------------------------------ helpers

(def ^:private decide* @#'gate/decide*)

(defn- check!
  "Run a property; on failure report the smallest failing input."
  [label n p]
  (let [r (tc/quick-check n p {:max-size 30})]
    (is (true? (:pass? r))
        (str label " failed: " (pr-str (select-keys r [:seed :num-tests :fail]))
             "\nsmallest: " (pr-str (get-in r [:shrunk :smallest]))))
    r))

(defn- nm [layer cls] [layer cls :offer (UUID/randomUUID)])

(defn- entry [] {:lock (l/fresh-lock) :erased-at nil})

(defn- nonces [n] (repeatedly n l/fresh-nonce))

(defn- bytes= [a b] (Arrays/equals ^bytes a ^bytes b))

(defn- seal-offer
  "A plaintext offer sealed as the door seals it, with locks it makes up:
  each value fact under a fresh lock cited as [lease-name i] (per value),
  or all under one (`:per-act`). Returns [raw delivered]."
  ([raw] (seal-offer raw :per-value))
  ([raw grain]
   (let [lease [(:layer raw) :by-layer :offer (UUID/randomUUID)]
         idx (keep-indexed (fn [i f] (when (env/value-fact? f) i)) (:facts raw))
         ids (if (= :per-act grain)
               (zipmap idx (repeat [lease 0]))
               (zipmap idx (map (fn [j] [lease (long j)]) (range))))
         Ks (into {} (map (fn [id] [id (l/fresh-lock)])) (set (vals ids)))]
     [(update raw :facts (fn [fs] (vec (map-indexed (fn [i f]
                                                      (if-let [id (get ids i)]
                                                        (-> f (dissoc :v) (assoc :sealed (l/seal (get Ks id) (l/canonical-bytes (:v f))) :lock-id id))
                                                        f))
                                                    fs))))
      Ks])))

(defn- parsed-sealed
  "A parsed, sealed offer `who` into `layer` with `facts`, and its delivered locks."
  [who layer facts & {:as more}]
  (let [grain (or (:grain more) :per-value)
        raw (merge {:version 1 :who who :layer layer :class :by-layer
                    :permission (when-not (= :operator who) [who layer layer])
                    :session :s1 :facts facts}
                   (dissoc more :grain))
        raw (assoc raw :name (or (:name more) (env/name-for raw)))
        [sealed Ks] (seal-offer raw grain)
        p (env/parse sealed)]
    (when-not (:ok p) (throw (ex-info "the parser refused a sealed offer" {:raw raw :p p})))
    [(:ok p) Ks]))

(def alice-settings {:kind :personal :owner :alice :class :by-layer :grain :per-value})
(def agent-settings {:kind :agent :owner :alice :class :by-layer :grain :per-value})
(def base-settings {:kind :base :owner :operator :class :by-layer :grain :per-value})

(defn- granted
  "The permission row of [who layer layer], granted, as the gate reads it."
  [who layer]
  (if (= :operator who) {} {[who layer layer] {:granted [(nm layer :by-layer) 0]}}))

(defn- lx-for
  "The lock context the gate's event would assemble for this offer, given
  the delivered locks, the person entries and a forget's target rows and
  ledger entry."
  [offer settings delivered persons & {:keys [target-rows target-ledger]}]
  (let [rv (l/value-context offer settings delivered)]
    (l/lock-context delivered rv persons target-rows target-ledger (l/fresh-for offer settings rv))))

;; ------------------------------------------------------------ 1. A cases

(defn- case-parts
  "An A case of scenarios.clj as data: its offers' facts with their layer,
  the persons forgotten in order, and the expected state per value."
  [[what history expected]]
  {:what what
   :facts (for [[op spec] history :when (= :offer op) f (:facts spec)]
            (assoc f :layer (:layer spec)))
   :forgets (vec (for [[op p] history :when (= :forget-person op)] p))
   :expected (:values expected)})

(defn- value-of [{:keys [k mention]}]
  (if (= :mention k) {:persons mention} {:token "a note"}))

(defn- rig-wrap
  "The rig's wrap for a fact of a case: the layer's person owner, the
  grammar's subjects over the value, the mark (plan, 'The shapes')."
  [{:keys [layer k mark] :as f}]
  (l/wrap-of (l/person-owner (get-in fm/layers [layer :owner]))
             (grammar/subjects-of grammar/grammars k (value-of f))
             (contains? mark :die-with-any)))

(defn- model-wrap [{:keys [layer k mark] :as f}]
  (fm/wrap :owner-required (get-in fm/layers [layer :owner])
           (fm/fact-subjects layer {:k k :v (value-of f)})
           (contains? mark :die-with-any)))

(defn- forgotten
  "Person entries after the forgets, in order: the i-th forget dated i+1
  (the model's `:order` stands for the rig's stamp, L8)."
  [live forgets]
  (reduce (fn [ps [i p]] (assoc ps p {:lock nil :erased-at (long (inc i))}))
          live (map-indexed vector forgets)))

(defn- model-forgotten [forgets]
  (into {} (map-indexed (fn [i p] [p {:order (inc i)}])) forgets))

(deftest a-cases-at-the-lock-level
  (testing "the model's eight A cases, as scenarios.clj has them"
    (is (= 8 (count fs/a-cases))))
  (doseq [c fs/a-cases
          :let [{:keys [what facts forgets expected]} (case-parts c)
                [_ model-seen _] (fs/play fm/baseline c [])]]
    (testing what
      (is (= expected (:values model-seen)) "the model gives the case's expected answer (sanity)")
      (doseq [f facts
              :let [ref [(:layer f) (:k f) (:mention f)]
                    rw (rig-wrap f)
                    mw (model-wrap f)
                    live {:alice (entry) :bob (entry)}
                    persons (forgotten live forgets)
                    K (l/fresh-lock)
                    plain (l/canonical-bytes (value-of f))
                    sealed (l/seal K plain)
                    rec (l/wrap K rw live (nonces (l/seals-needed rw)))
                    closed (l/wrap-closed rw persons)
                    mclosed (fm/wrap-closed mw (model-forgotten forgets))
                    opened (some-> (l/unwrap rec persons) (l/open sealed))]]
        (is (= [(set (:required mw)) (set (:any-of mw))] [(set (:required rw)) (set (:any-of rw))])
            (str (pr-str ref) ": the wrap is the model's under :owner-required"))
        (is (= (:order mclosed) closed) (str (pr-str ref) ": closed by the same forget, on the same date"))
        (is (some? rec) (str (pr-str ref) ": the lock wraps under the live person locks"))
        (is (bytes= K (l/unwrap rec live)) (str (pr-str ref) ": before any forget it opens"))
        (case (get expected ref)
          :open (do (is (nil? closed) (str (pr-str ref) ": what must stay open stays open"))
                    (is (bytes= plain opened) (str (pr-str ref) ": the ciphertext opens with the locks left")))
          :erased (do (is (some? closed) (str (pr-str ref) ": what must be erased is erased"))
                      (is (nil? opened) (str (pr-str ref) ": no lock left opens it"))))))))

(def gen-person (gen/elements [:alice :bob :carol :dan]))

(deftest wrap-table-against-the-model
  (testing "on generated owners, subjects, marks and forget sequences: the same wrap, the same close and date"
    (check! "wrap and wrap-closed = the model's" 1500
            (prop/for-all [owner (gen/one-of [(gen/return nil) gen-person])
                           subjects (gen/set gen-person {:max-elements 4})
                           marked? gen/boolean
                           forgets (gen/fmap distinct (gen/vector gen-person 0 4))]
              (let [rw (l/wrap-of owner subjects marked?)
                    mw (fm/wrap :owner-required owner subjects marked?)
                    persons (forgotten {} forgets)
                    closed (l/wrap-closed rw persons)
                    mclosed (fm/wrap-closed mw (model-forgotten forgets))]
                (and (= (set (:required mw)) (set (:required rw)))
                     (= (set (:any-of mw)) (set (:any-of rw)))
                     (= (vec (sort (:required rw))) (:required rw))
                     (= (vec (sort (:any-of rw))) (:any-of rw))
                     (= (:order mclosed) closed))))))
  (testing "enforcement on generated wraps: unwrap succeeds exactly when the wrap is open"
    (check! "unwrap iff open" 500
            (prop/for-all [owner (gen/one-of [(gen/return nil) gen-person])
                           subjects (gen/set gen-person {:max-elements 4})
                           marked? gen/boolean
                           forgets (gen/fmap distinct (gen/vector gen-person 0 4))]
              (let [w (l/wrap-of owner subjects marked?)
                    live (into {} (map (fn [p] [p (entry)])) [:alice :bob :carol :dan])
                    K (l/fresh-lock)
                    rec (l/wrap K w live (nonces (l/seals-needed w)))
                    persons (forgotten live forgets)
                    out (l/unwrap rec persons)]
                (if (l/wrap-closed w persons)
                  (nil? out)
                  (bytes= K out)))))))

;; ------------------------------------------------------------- 2. crypto

(deftest crypto-round-trips
  (let [K (l/fresh-lock)
        plain (l/canonical-bytes {:token "v1" :n 3})]
    (testing "seal and open: nonce ++ ciphertext ++ tag, 28 bytes over the plaintext (L3)"
      (let [s (l/seal K plain)]
        (is (bytes= plain (l/open K s)))
        (is (= (+ 28 (alength ^bytes plain)) (alength ^bytes s)))
        (is (not (bytes= s (l/seal K plain))) "a fresh nonce each time: two seals differ")
        (is (bytes= s (l/seal-with K plain (Arrays/copyOf ^bytes s 12))) "seal-with is pure given its nonce")))
    (testing "the value digest: HMAC-SHA256 keyed by the lock over the plaintext, 32 bytes (L26)"
      (let [d (l/value-digest K plain)]
        (is (= 32 (alength ^bytes d)))
        (is (bytes= d (l/value-digest K (l/open K (l/seal K plain)))) "equal plaintext sealed again: other bytes, the same digest")
        (is (not (bytes= d (l/value-digest K (l/canonical-bytes {:token "v2" :n 3})))) "other plaintext, another digest")
        (is (not (bytes= d (l/value-digest (l/fresh-lock) plain))) "another lock, another digest")))
    (testing "the canonical plaintext: = values give = bytes; it reads back as the value"
      (is (bytes= (l/canonical-bytes {:b 1 :a [1 2]}) (l/canonical-bytes (into (sorted-map) {:a [1 2] :b 1}))))
      (is (= {:value {:a [1 2] :b 1}} (dissoc (l/decode-plain (l/canonical-bytes {:b 1 :a [1 2]})) :plain))))
    (testing "wrap and unwrap: required chains and any-of copies, the scheme tag"
      (let [ps {:alice (entry) :bob (entry) :carol (entry)}]
        (doseq [w [{:required [:alice] :any-of []}
                   {:required [:alice :bob :carol] :any-of []}
                   {:required [] :any-of [:alice :bob]}
                   {:required [:alice] :any-of [:bob :carol]}
                   {:required [] :any-of []}]]
          (let [rec (l/wrap K w ps (nonces (l/seals-needed w)))]
            (is (= :aes-gcm-1 (:scheme rec)) "every lock record names its scheme (first-record)")
            (is (= (:required w) (:required rec)))
            (is (bytes= K (l/unwrap rec ps)) (pr-str w))
            (is (= (empty? (:any-of w)) (nil? (:any-blobs rec))) "any-blobs only for an any-of wrap")
            (is (= (boolean (seq (:any-of w))) (nil? (:blob rec))) "blob nil exactly when any-of is not empty")))
        (is (bytes= K (:blob (l/wrap K {:required [] :any-of []} ps []))) "an empty wrap keeps K bare")
        (is (nil? (l/wrap K {:required [:dan] :any-of []} ps (nonces 1))) "a person with no lock: no record")
        (is (nil? (l/wrap K {:required [:alice :bob] :any-of []} ps (nonces 1))) "too few nonces: no record")
        (is (nil? (l/unwrap (assoc (l/wrap K {:required [:alice] :any-of []} ps (nonces 1)) :scheme :rsa-oaep-1) ps))
            "a record of another scheme does not open here")))
    (testing "lease rows: under the writer's lock, bare for the root actor; unlease gives the lock back (L22, L23)"
      (let [a (entry)
            row (l/lease-row K :alice a (l/fresh-nonce))
            bare (l/lease-row K nil nil nil)]
        (is (= :alice (:under row)))
        (is (not (bytes= K (:sealed row))) "sealed, not the lock itself")
        (is (bytes= K (l/unlease row a)))
        (is (nil? (l/unlease row {:lock nil :erased-at 9})) "the writer forgotten: the row no longer unleases")
        (is (nil? (l/unlease row (entry))) "another person's lock does not unlease it")
        (is (= {:under nil} (dissoc bare :sealed)))
        (is (bytes= K (l/unlease bare nil)))
        (is (nil? (l/lease-row K :alice {:lock nil} (l/fresh-nonce))) "no row under a destroyed lock")))
    (testing "lease ids are a function of the name and the count (L21)"
      (let [n [:alice :by-layer :offer (UUID. 3 3)]]
        (is (= [[n 0] [n 1] [n 2]] (l/lease-ids n 3)))
        (is (= (l/lease-ids n 3) (l/lease-ids (subvec (into [:x] n) 1) 3)) "the same ids from an equal name")
        (is (every? #(instance? clojure.lang.PersistentVector %) (l/lease-ids (subvec (into [:x] n) 1) 2)))
        (is (every? #(instance? Long (second %)) (l/lease-ids n 2)))))))

;; ------------------------------------------------------------ 3. grammar

(deftest the-grammar
  (testing "a :mention names the keywords at :persons; a key with no entry names no one (L12)"
    (is (= #{:bob} (grammar/subjects-of grammar/grammars :mention {:persons #{:bob}})))
    (is (= #{:alice :bob} (grammar/subjects-of grammar/grammars :mention {:persons [:bob :alice :bob]})))
    (is (= #{} (grammar/subjects-of grammar/grammars :mention {:persons #{}})))
    (is (= #{} (grammar/subjects-of grammar/grammars :note {:persons #{:bob}})))
    (is (= #{} (grammar/subjects-of grammar/grammars :note "anything"))))
  (testing "any other shape under :mention is refused as data"
    (doseq [v ["bob" :bob nil 7 {:persons "bob"} {:persons [:bob "x"]} {:persons {:bob 1}} {:text "no persons"} [:bob]]]
      (is (= :value-shape (grammar/subjects-of grammar/grammars :mention v)) (pr-str v))
      (is (= :value-shape (l/shape-refusal grammar/grammars :mention v)) (pr-str v))))
  (testing "the grammar is data: another map, another reading"
    (is (= #{:carol} (grammar/subjects-of {:assign {:subjects-at [:to]}} :assign {:to #{:carol}})))))

;; ----------------------------------------------------------- 4. placement

(deftest placement-and-the-per-act-plan
  (testing "a lock row for personal and hand layers, the record for agent layers and the base; :own-row overrides (ruling 7, D9)"
    (is (true? (l/row-lock? :personal #{})))
    (is (true? (l/row-lock? :hand #{})))
    (is (false? (l/row-lock? :agent #{})))
    (is (false? (l/row-lock? :base #{})))
    (is (true? (l/row-lock? :agent #{:own-row})))
    (is (true? (l/row-lock? :base #{:own-row :die-with-any}))))
  (let [facts [{:e :e0 :k :note :v "a"} {:e :e1 :k :mention :v {:persons #{:bob}} :mark #{:die-with-any}}
               {:e :e2 :k :tag :v 3 :mark #{:own-row}}]]
    (testing "per value: a lock per value fact, its own subjects and mark"
      (let [[o Ks] (parsed-sealed :alice :alice-agent facts :subjects #{:carol})
            rv (l/value-context o agent-settings Ks)
            plan (l/lock-plan o agent-settings rv)]
        (is (nil? (:reason rv)))
        (is (= 3 (count plan)))
        (is (= [[0] [1] [2]] (map :facts plan)))
        (is (= [{:required [:alice] :any-of []}
                {:required [:alice :bob :carol] :any-of []}
                {:required [:alice] :any-of []}]
               (map :wrap plan))
            "the owner alone unless marked; marked, every subject of the value: owner, grammar, carried (L19)")
        (is (= [false false true] (map :row? plan)) "the agent layer's record, but the :own-row value's row")))
    (testing "per act: one lock, one wrap over the act's union, marked if any value is, a row if any value is :own-row (L6)"
      (let [[o Ks] (parsed-sealed :alice :alice-agent facts :grain :per-act)
            s (assoc agent-settings :grain :per-act)
            rv (l/value-context o s Ks)
            plan (l/lock-plan o s rv)]
        (is (nil? (:reason rv)))
        (is (= 1 (count plan)))
        (is (= [0 1 2] (:facts (first plan))))
        (is (= {:required [:alice :bob] :any-of []} (:wrap (first plan))))
        (is (true? (:row? (first plan))))))
    (testing "the base: no person owner, so 7b as written; its locks in the record ([V-F2])"
      (let [[o Ks] (parsed-sealed :alice :base [{:e :e0 :k :note :v "about no one"}
                                                {:e :e1 :k :mention :v {:persons #{:bob}}}
                                                {:e :e2 :k :mention :v {:persons #{:alice :bob}} :mark #{:die-with-any}}])
            rv (l/value-context o base-settings Ks)
            plan (l/lock-plan o base-settings rv)]
        (is (= [{:required [] :any-of []} {:required [] :any-of [:bob]} {:required [:alice :bob] :any-of []}]
               (map :wrap plan)))
        (is (= [false false false] (map :row? plan)))
        (is (= #{:bob :alice} (:union rv)) "the root actor is no subject")))))

;; ------------------------------------------------------------ 5. no throw

(def gen-junk
  (gen/one-of [gen/any (gen/return nil) (gen/fmap byte-array (gen/vector gen/byte 0 80))
               (gen/return (byte-array 16)) (gen/return (byte-array 32)) (gen/return (byte-array 0))]))

(deftest nothing-throws
  (let [K (l/fresh-lock)
        s (l/seal K (l/canonical-bytes "v"))
        ps {:alice (entry)}
        rec (l/wrap K {:required [:alice] :any-of []} ps (nonces 1))]
    (testing "garbage, truncated, swapped blobs, wrong-length locks: nil, never a throw"
      (check! "open, unwrap, unlease, decode-plain total" 800
              (prop/for-all [a gen-junk b gen-junk]
                (and (nil? (l/open a b))
                     (or (nil? (l/unwrap a ps)) (map? a))
                     (nil? (l/unlease {:under :alice :sealed a} {:lock b}))
                     (map? (l/decode-plain a))
                     (nil? (l/value-digest a "not bytes")))))
      (is (nil? (l/open K (Arrays/copyOf ^bytes s 27))) "truncated below nonce and tag")
      (is (nil? (l/open K (Arrays/copyOf ^bytes s (dec (alength ^bytes s))))) "truncated tag")
      (is (nil? (l/open (Arrays/copyOf ^bytes K 16) s)) "a 16-byte lock is not a lock here")
      (is (nil? (l/open K (let [b (aclone ^bytes s)] (aset-byte b 14 (unchecked-byte (bit-xor (aget ^bytes b 14) 1))) b))) "tampered")
      (is (nil? (l/unwrap (assoc rec :blob (l/seal (l/fresh-lock) K)) ps)) "a swapped blob")
      (is (nil? (l/unwrap (assoc rec :required [:bob]) ps)) "a record naming someone without a lock")
      (is (nil? (l/unwrap (update rec :blob #(Arrays/copyOf ^bytes % 20)) ps)) "a truncated blob"))
    (testing "read-values, check-resend and open-with are total on anything"
      (check! "value checks total" 500
              (prop/for-all [facts (gen/vector (gen/one-of [gen/any (gen/fmap (fn [b] {:e :e0 :k :note :sealed b :lock-id [(nm :alice :by-layer) 0]})
                                                                              (gen/fmap byte-array (gen/vector gen/byte 0 60)))])
                                              0 4)
                             delivered (gen/one-of [(gen/return {}) gen/any])]
                (let [r (l/read-values facts delivered {:owner :alice :carried #{} :grain :per-value})]
                  (and (map? r)
                       (contains? #{nil :does-not-open :malformed-value :value-shape :too-many-subjects :grain-mismatch} (:reason r))
                       (contains? #{nil :name-taken} (l/check-resend facts [] {0 K} {0 K}))
                       (map? (l/open-with (first facts) 5 7 nil rec ps)))))))))

;; ------------------------------------------------------- 6. resend check

(deftest the-resend-check
  (let [K (l/fresh-lock)
        K2 (l/fresh-lock)
        n (nm :alice :by-layer)
        f (fn [v lock id] {:e :e0 :k :note :sealed (l/seal lock (l/canonical-bytes v)) :lock-id id :replaces nil :mark #{}})
        row {:e :e0 :k :note :v nil :sealed (:sealed (f "hello" K [n 0])) :lock-id [n 0]
             :digest (l/value-digest K (l/canonical-bytes "hello")) :replaces nil :mark #{}}]
    (testing "equal plaintext under the recorded lock answers; sealed again, other bytes, still answers"
      (is (nil? (l/check-resend [(f "hello" K [n 0])] [row] {0 K} {0 K}))))
    (testing "equal plaintext under a newly leased lock (a door that lost its locks, V-F1): answers"
      (is (nil? (l/check-resend [(f "hello" K2 [n 7])] [row] {0 K} {0 K2}))))
    (testing "other plaintext: the name is taken (the value digest differs)"
      (is (= :name-taken (l/check-resend [(f "other" K [n 0])] [row] {0 K} {0 K})))
      (is (= :name-taken (l/check-resend [(f "other" K2 [n 7])] [row] {0 K} {0 K2}))))
    (testing "bytes that do not open under their own lock: the name is taken"
      (is (= :name-taken (l/check-resend [(f "hello" K2 [n 0])] [row] {0 K} {0 K}))))
    (testing "a lock that no longer opens skips its value: after a forget, or a missing own lock"
      (is (nil? (l/check-resend [(f "other" K [n 0])] [row] {0 nil} {0 nil})) "the recorded lock is gone: the price of forgetting")
      (is (nil? (l/check-resend [(f "other" K2 [n 7])] [row] {0 K} {0 nil})) "the own lock did not deliver: skipped, so a replay answers the same"))
    (testing "control facts and retracts are not checked"
      (is (nil? (l/check-resend [{:e :e0 :k :note :v nil}] [{:e :e0 :k :note :v nil}] {} {}))))))

;; -------------------------------------------- the value checks, L27's order

(deftest the-value-checks
  (let [K (l/fresh-lock)
        n (nm :alice :by-layer)
        sf (fn [v i & [lock]] {:e :e0 :k :note :sealed (l/seal (or lock K) (if (bytes? v) v (l/canonical-bytes v))) :lock-id [n i]})
        mf (fn [v i] {:e :e1 :k :mention :sealed (l/seal K (l/canonical-bytes v)) :lock-id [n i]})
        dl {[n 0] K [n 1] K [n 2] K}
        ctx {:owner :alice :carried #{} :grain :per-value}]
    (testing "each check alone"
      (is (nil? (l/value-refusal [(sf "a" 0) (mf {:persons #{:bob}} 1)] dl ctx)))
      (is (= :does-not-open (l/value-refusal [(sf "a" 0 (l/fresh-lock))] dl ctx)) "a delivered lock that does not open the bytes")
      (is (= :does-not-open (l/value-refusal [(sf "a" 0)] {} ctx)) "no lock delivered for it")
      (is (= :malformed-value (l/value-refusal [(sf (.getBytes "(1 2)" "UTF-8") 0)] dl ctx)) "a list is outside the domain")
      (is (= :malformed-value (l/value-refusal [(sf (.getBytes "{:b 1, :a 2}" "UTF-8") 0)] dl ctx)) "not the canonical text")
      (is (= :malformed-value (l/value-refusal [(sf (.getBytes (str (apply str (repeat 33 "[")) "1" (apply str (repeat 33 "]"))) "UTF-8") 0)] dl ctx))
          "nested past 32 collections")
      (is (= :malformed-value (l/value-refusal [(sf (.getBytes ":a b" "UTF-8") 0)] dl ctx)) "a keyword that does not read back")
      (is (= :value-shape (l/value-refusal [(mf "bob" 0)] dl ctx)) "a :mention the grammar cannot read (L12)")
      (is (= :too-many-subjects (l/value-refusal [(mf {:persons (set (map #(keyword (str "p" %)) (range 256)))} 0)] dl ctx))
          "the owner and 256 mentioned: 257 over the cap (L13)")
      (is (nil? (l/value-refusal [(mf {:persons (set (map #(keyword (str "p" %)) (range 255)))} 0)] dl ctx)) "256 in all pass")
      (is (= :grain-mismatch (l/value-refusal [(sf "a" 0) (assoc (sf "b" 1) :lock-id [n 0])] dl ctx)) "per value, one lock cited twice (L30)")
      (is (= :grain-mismatch (l/value-refusal [(sf "a" 0) (sf "b" 1)] dl (assoc ctx :grain :per-act))) "per act, two locks")
      (is (nil? (l/value-refusal [(sf "a" 0) (assoc (sf "b" 1) :lock-id [n 0])] dl (assoc ctx :grain :per-act))) "per act, one lock for all"))
    (testing "when several fail, the earliest in L27's order is the answer"
      (is (= :does-not-open (l/value-refusal [(mf "bob" 0) (sf "a" 1 (l/fresh-lock))] dl ctx)))
      (is (= :malformed-value (l/value-refusal [(mf "bob" 0) (sf (.getBytes "(1)" "UTF-8") 1)] dl ctx)))
      (is (= :value-shape (l/value-refusal [(sf "a" 0) (assoc (mf "bob" 1) :lock-id [n 0])] dl ctx)) "shape before grain"))
    (testing "the subjects: per value owner, grammar and carried; the act's union"
      (let [r (l/read-values [(sf "a" 0) (mf {:persons #{:bob}} 1) {:e :alice :k :lock-grain :v :per-act}] dl
                             (assoc ctx :carried #{:carol}))]
        (is (= {0 #{:alice :carol} 1 #{:alice :bob :carol}} (:subjects r)))
        (is (= #{:alice :bob :carol} (:union r)))
        (is (= {0 "a" 1 {:persons #{:bob}}} (:values r)))))))

;; ------------------------------------------------ the decision's lock part

(deftest the-lock-decision
  (let [a (entry) b (entry)
        persons {:alice a :bob b}]
    (testing "an admitted value in a personal layer: a lock row under the owner, the row's sealed bytes and digest, the lease consumed"
      (let [[o Ks] (parsed-sealed :alice :alice [{:e :e0 :k :note :v "x"}])
            fx (l/lock-effects o alice-settings (lx-for o alice-settings Ks persons) 100)
            [[lid rec]] (:lock-rows fx)
            row (get-in fx [:rows 0])]
        (is (= [(:lock-id (first (:facts o)))] (:consume fx)))
        (is (= lid (:lock-id row)))
        (is (nil? (:lock row)) "a row lock: the row keeps only its id")
        (is (bytes= (:sealed (first (:facts o))) (:sealed row)) "the sealed bytes as offered")
        (is (= {:required [:alice] :any-of []} (select-keys rec [:required :any-of])))
        (is (bytes= (get Ks lid) (l/unwrap rec persons)) "re-wrapped: the same lock under the value's wrap")
        (is (bytes= (l/value-digest (get Ks lid) (l/canonical-bytes "x")) (:digest row)))
        (is (= [100 (:name o)] (:by-stamp fx)))))
    (testing "an admitted value in an agent layer: the lock in the record"
      (let [[o Ks] (parsed-sealed :alice :alice-agent [{:e :e0 :k :note :v "x"}])
            fx (l/lock-effects o agent-settings (lx-for o agent-settings Ks persons) 100)]
        (is (empty? (:lock-rows fx)))
        (is (bytes= (get Ks (:lock-id (first (:facts o)))) (l/unwrap (get-in fx [:rows 0 :lock]) persons)))))
    (testing "a refusal consumes the cited leases and writes nothing else"
      (let [[o Ks] (parsed-sealed :alice :alice [{:e :e0 :k :note :v "x"} {:e :e1 :k :note :v "y"}])]
        (is (= {:consume (l/cited-ids (:facts o))} (l/lock-effects o alice-settings (lx-for o alice-settings Ks persons) nil)))))
    (testing "a lease: n rows sealed under the writer's lock, bare for the operator; the ids in the ack"
      (let [[o _] (parsed-sealed :alice :alice [{:e :s1 :k :lease :v {:count 3}}])
            fx (l/lock-effects o alice-settings (lx-for o alice-settings {} persons) 100)
            [op _] (parsed-sealed :operator :alice [{:e :s1 :k :lease :v {:count 2}}])
            fxo (l/lock-effects op alice-settings (lx-for op alice-settings {} persons) 100)]
        (is (= (l/lease-ids (:name o) 3) (map first (:lease-rows fx))))
        (is (= {:lock-ids (l/lease-ids (:name o) 3)} (:ack fx)))
        (is (every? #(= :alice (:under (second %))) (:lease-rows fx)))
        (is (every? #(l/lock? (l/unlease (second %) a)) (:lease-rows fx)))
        (is (every? #(nil? (:under (second %))) (:lease-rows fxo)) "the operator's lease is bare")
        (is (= :no-such-person (l/lock-refusal o alice-settings (lx-for o alice-settings {} {}))) "the writer has no lock")
        (is (= :person-forgotten (l/lock-refusal o alice-settings (lx-for o alice-settings {} {:alice {:lock nil :erased-at 5}}))))))
    (testing "a value forget: the row lock deleted, a record lock excised in every row of the act, the ledger dated, the purge list"
      (let [n1 (nm :alice :by-layer)
            rows [{:e :e0 :k :note :v nil :lock-id [n1 0] :sealed (byte-array 30)}
                  {:e :e1 :k :note :v nil :lock-id [n1 0] :sealed (byte-array 30)}
                  {:e :e2 :k :tag :v "\"t\"" :lock-id nil}]
            [o _] (parsed-sealed :alice :alice [{:e :e0 :k :forget :v {:target [n1 1]}}])
            fx (l/lock-effects o alice-settings (lx-for o alice-settings {} persons :target-rows rows) 200)
            xrows (mapv #(if (:lock-id %) (assoc % :lock {:required [:alice]}) %) rows)
            fxx (l/lock-effects o agent-settings (lx-for o agent-settings {} persons :target-rows xrows) 200)]
        (is (= [[n1 0]] (:lock-deletes fx)))
        (is (= [[[n1 0] {:stamp 200 :how :row-deleted}]] (:ledger fx)))
        (is (= [[n1 0] [n1 1]] (map :fid (:purge fx))) "per act: every value sharing the lock is purged")
        (is (= {:how :row-deleted} (:ack fx)))
        (is (= [[n1 0 (assoc (xrows 0) :lock nil)] [n1 1 (assoc (xrows 1) :lock nil)]] (:row-writes fxx)))
        (is (= :excised (:how (:ack fxx))))
        (is (= {:how nil} (:ack (l/lock-effects o alice-settings (lx-for o alice-settings {} persons :target-rows rows
                                                                           :target-ledger {:stamp 150 :how :row-deleted}) 200)))
            "a lock already in the ledger: admitted, nothing changes")
        (let [[oc _] (parsed-sealed :alice :alice [{:e :e2 :k :forget :v {:target [n1 2]}}])]
          (is (= {:how nil} (:ack (l/lock-effects oc alice-settings (lx-for oc alice-settings {} persons :target-rows rows) 200)))
              "a forget of a control fact: admitted, nothing changes"))
        (let [[ou _] (parsed-sealed :alice :alice [{:e :e0 :k :forget :v {:target [n1 9]}}])
              [ob _] (parsed-sealed :alice :alice [{:e :e0 :k :forget :v {:target [n1 9999999999999]}}])]
          (is (= :no-such-value (l/lock-refusal ou alice-settings (lx-for ou alice-settings {} persons :target-rows rows))))
          (is (= :no-such-value (l/lock-refusal ob alice-settings (lx-for ob alice-settings {} persons :target-rows rows)))
              "an index past any row, even past int range: refused as data"))))
    (testing "person acts: made once, forgotten with the first date kept"
      (let [[mk _] (parsed-sealed :operator :people [{:e :carol :k :person :v {:id :carol}}])
            [fg _] (parsed-sealed :operator :people [{:e :carol :k :forget-person :v {:person :carol}}])
            ss {:kind :store :class :by-layer :grain :per-value}]
        (is (nil? (l/lock-refusal mk ss (lx-for mk ss {} {}))))
        (is (l/lock? (:lock (second (:person (l/lock-effects mk ss (lx-for mk ss {} {}) 10))))))
        (is (= :person-already-made (l/lock-refusal mk ss (lx-for mk ss {} {:carol (entry)}))))
        (is (= :no-such-person (l/lock-refusal fg ss (lx-for fg ss {} {}))))
        (is (= [:carol {:lock nil :erased-at 11}] (:person (l/lock-effects fg ss (lx-for fg ss {} {:carol (entry)}) 11))))
        (is (= {:fan-out :carol :person nil}
               (select-keys (l/lock-effects fg ss (lx-for fg ss {} {:carol {:lock nil :erased-at 11}}) 12) [:fan-out :person]))
            "forgotten again: yes, no write, the fan-out repeats")))
    (testing "a write about a person with no lock, or a forgotten one, is refused after the value checks (L11)"
      (let [[o Ks] (parsed-sealed :alice :alice [{:e :e1 :k :mention :v {:persons #{:carol}} :mark #{:die-with-any}}])]
        (is (= :no-such-person (l/lock-refusal o alice-settings (lx-for o alice-settings Ks persons))))
        (is (= :person-forgotten (l/lock-refusal o alice-settings (lx-for o alice-settings Ks (assoc persons :carol {:lock nil :erased-at 3})))))
        (is (nil? (l/lock-refusal o alice-settings (lx-for o alice-settings Ks (assoc persons :carol (entry))))))))
    (testing "the stage 2 reasons come after stage 1's list: a stage 1 refusal wins, and still consumes"
      (let [[o Ks] (parsed-sealed :alice :alice [{:e :e0 :k :note :v "x"}] :permission [:alice :alice :alice-hand])
            d (decide* o alice-settings {} {} 0 1 (env/digest o) (lx-for o alice-settings Ks persons))]
        (is (= :permission-from-another-layer (:reason (:record d))))
        (is (= {:consume (l/cited-ids (:facts o))} (:locks d)))))))

;; ------------------------------------ every stage 2 write fits the schema

(defn- apply-lock-writes!
  "Every write module.clj and write-decision> make for a decision, on test
  PStates with the module's schemas; throws where the module's writes would."
  [tp pp layer session nm d]
  (let [tx #(rtest/test-pstate-transform %1 %2)
        fx (:locks d)]
    (tx [(keypath layer :answers nm) (termval (:record d))] tp)
    (when (= :yes (:answer (:record d)))
      (tx [(keypath layer :log nm) (termval (:log d))] tp))
    (doseq [id (:consume fx)] (tx [(keypath layer :leases session id) NONE>] tp))
    (doseq [[id r] (:lock-rows fx)] (tx [(keypath layer :locks id) (termval r)] tp))
    (doseq [[id r] (:lease-rows fx)] (tx [(keypath layer :leases session id) (termval r)] tp))
    (when-let [c (:close fx)] (tx [(keypath layer :leases c) NONE>] tp))
    (doseq [id (:lock-deletes fx)] (tx [(keypath layer :locks id) NONE>] tp))
    (doseq [[n i r] (:row-writes fx)] (tx [(keypath layer :log n i) (termval r)] tp))
    (doseq [[id e] (:ledger fx)] (tx [(keypath layer :erased id) (termval e)] tp))
    (when-let [[p e] (:person fx)] (tx [(keypath p) (termval e)] pp))
    (when-let [[s n] (:by-stamp fx)] (tx [(keypath layer :by-stamp s) (termval n)] tp))))

(def gen-value
  (gen/recursive-gen (fn [inner] (gen/one-of [(gen/vector inner 0 3) (gen/set inner {:max-elements 3})
                                              (gen/map inner inner {:max-elements 3})]))
                     (gen/one-of [gen/small-integer gen/string-ascii gen/keyword gen/boolean gen/uuid])))

(def gen-stage2-act
  "An act of one of the kinds stage 2 decides: values (notes, mentions,
  marks), a lease, a session close, a person act."
  (gen/one-of
   [(gen/let [vs (gen/vector (gen/tuple (gen/elements [:note :mention :tag]) gen-value
                                        (gen/elements [#{} #{:own-row} #{:die-with-any}]))
                             1 4)
              who (gen/elements [:alice :operator])
              layer (gen/elements [:alice :alice-agent :base])]
      {:who who :layer layer
       :facts (vec (map-indexed (fn [i [k v mark]]
                                  {:e (keyword (str "e" i)) :k k
                                   :v (if (and (= :mention k) (even? i)) {:persons (set (take (inc i) [:alice :bob]))} v)
                                   :mark mark})
                                vs))})
    (gen/let [n (gen/choose 1 8) who (gen/elements [:alice :operator])]
      {:who who :layer :alice :facts [{:e :s1 :k :lease :v {:count n}}]})
    (gen/return {:who :alice :layer :alice :facts [{:e :s1 :k :session-closed :v {:session :s1}}]})
    (gen/let [p (gen/elements [:carol :dan])]
      {:who :operator :layer :people :facts [{:e p :k :person :v {:id p}}]})]))

(deftest every-lock-write-fits-the-schema
  (with-open [tp (rtest/create-test-pstate m/layers-schema)
              pp (rtest/create-test-pstate (l/persons-schema))]
    (let [persons {:alice (entry) :bob (entry)}
          written (atom #{})
          settings {:alice alice-settings :alice-agent agent-settings :base base-settings
                    :people {:kind :store :class :by-layer :grain :per-value}}]
      (testing "the decision with its lock context is total and every write it computes fits $$layers and $$persons"
        (check! "stage 2 decide + schema" 400
                (prop/for-all [spec gen-stage2-act]
                  (let [{:keys [who layer facts]} spec
                        [o Ks] (parsed-sealed who layer facts)
                        s (settings layer)
                        d (decide* o s (granted who layer) {} 0 5 (env/digest o) (lx-for o s Ks persons))]
                    (apply-lock-writes! tp pp layer :s1 (:name o) d)
                    (swap! written into (keep (fn [k] (let [v (get-in d [:locks k])]
                                                        (when (if (coll? v) (seq v) (some? v)) k)))
                                              [:lease-rows :lock-rows :rows :close :person :by-stamp]))
                    (and (= :decide (:kind d))
                         (instance? Long (:stamp d))
                         (if (= :yes (:answer (:record d)))
                           (= (count facts) (count (:log d)))
                           (nil? (:log d))))))))
        (testing "and the property decided some of each kind yes, so each kind of write was written"
          (is (= #{:lease-rows :lock-rows :rows :close :person :by-stamp} @written) (pr-str @written)))
      (testing "a forget's excision and ledger, written over rows that exist"
        (let [[o Ks] (parsed-sealed :alice :alice-agent [{:e :e0 :k :note :v "x"} {:e :e1 :k :note :v "y"}] :grain :per-act)
              s (assoc agent-settings :grain :per-act)
              d (decide* o s (granted :alice :alice-agent) {} 0 5 (env/digest o) (lx-for o s Ks persons))
              _ (apply-lock-writes! tp pp :alice-agent :s1 (:name o) d)
              rows (rtest/test-pstate-select-one [(keypath :alice-agent :log (:name o)) (subselect ALL)] tp)
              [f _] (parsed-sealed :alice :alice-agent [{:e :e0 :k :forget :v {:target [(:name o) 1]}}])
              df (decide* f agent-settings (granted :alice :alice-agent) {} 5 6 (env/digest f)
                          (lx-for f agent-settings {} persons :target-rows rows))]
          (is (= :yes (:answer (:record d))))
          (is (= :yes (:answer (:record df))))
          (is (= 2 (count (get-in df [:locks :row-writes]))) "per act: both rows excised")
          (is (= :written (do (apply-lock-writes! tp pp :alice-agent :s1 (:name f) df) :written)))
          (is (every? nil? (map :lock (rtest/test-pstate-select [(keypath :alice-agent :log (:name o)) ALL] tp))))
          (is (= {:stamp (:stamp df) :how :excised}
                 (rtest/test-pstate-select-one [(keypath :alice-agent :erased (:lock-id (first rows)))] tp))))))))
