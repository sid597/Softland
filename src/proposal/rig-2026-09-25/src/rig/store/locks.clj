;; IMPORTANT: Before modifying this file, re-read PLAN-locks-and-forgetting.md
;; ("Revision, 26 September", "The shapes", "The door and the lease road",
;; L1 to L30, the [V-F n] fixes) and adhere to its decisions.
(ns rig.store.locks
  "Locks and forgetting (stage 2, PLAN-locks-and-forgetting.md): real
  encryption, kept simple.

  Every value is sealed at the door under a small symmetric lock the gate
  leased (AES-256-GCM, a fresh 12-byte nonce prepended, the 16-byte tag
  appended: L3). At decision the gate takes the lock from its lease row,
  opens the value, and re-wraps the lock under the value's own subjects: a
  chain of seals under every required person's lock, in sorted order, and
  one copy of that chain per any-of person, sealed under theirs (L3). So a
  wrap's rule is what can be decrypted, not a check a reader could skip.
  The value digest is HMAC-SHA256 keyed by the value's lock over its
  plaintext (L26): it confirms a guess only to a holder of the lock, so it
  dies with the lock.

  The primitives are pure and total: every function returns nil (or a
  refusal as data) on any failure and never throws, because an exception in
  topology code kills the worker and replays (SPEC 'What Rama showed' 3).
  The only impure functions draw fresh bytes from SecureRandom
  (`fresh-bytes` and its callers); the gate binds them before its pure
  decision.

  Sections: the primitives (seal, open, digests, leases, wraps, placement,
  the value checks, the record path's check, opening a row); the
  decision's lock part (`lock-refusal`, `lock-effects`, pure); the install
  functions (`row-fields`, `layer-fields`, `declare-pstates!`,
  `declare-queries!`); the dataflow ops the gate's one event calls
  (`deliver-lock>`, `consume-locks>`, `deliver-all>`, `decision-reads>`,
  `record-path>`, `write-decision>`, `fan-out>`), the seam to the read
  exit's purge (`purge-read-indexes>`), and the reads (`open-value>`,
  `open-row>`, `read-as-of>`, `lease-locks>`). The ops reach `$$layers`
  and `$$persons` through `<<with-substitutions` and
  `this-module-pobject-task-global`, and none repartitions except the
  person fan-out.

  Shapes (plan, 'The shapes'): a lock is 32 bytes; a lock id is
  `[lease-name i]`; a lease row is `{:under p :sealed bytes}` (bare K with
  `:under nil` for the root actor); a wrap is `{:required [p ...] :any-of
  [p ...]}`, sorted vectors; a lock record is the wrap plus `:blob` (K
  sealed under the required chain, nil when any-of is not empty) and
  `:any-blobs` ({p bytes}, nil otherwise); a person entry is `{:lock bytes
  :erased-at stamp}`, `:lock` nil once destroyed.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [clojure.edn :as edn]
            [com.rpl.rama :as rama :refer :all]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.path :refer :all]
            [rig.store.envelope :as env]
            [rig.store.grammar :as grammar]
            [rig.store.inject :as inject])
  (:import [java.nio.charset StandardCharsets]
           [java.security Key SecureRandom]
           [java.security.spec AlgorithmParameterSpec]
           [java.util Arrays]
           [javax.crypto Cipher Mac]
           [javax.crypto.spec GCMParameterSpec SecretKeySpec]))

;; ------------------------------------------------------------------ sizes

(def lock-size "A lock, value or person, is 32 random bytes (L3)." 32)
(def nonce-size "Every seal takes a fresh 12-byte nonce, prepended (L3)." 12)
(def tag-size "The AES-GCM tag, appended (L3)." 16)
(def seal-overhead "A sealed blob is its plaintext plus 28 bytes." (+ nonce-size tag-size))
(def max-lease "A lease mints 1 to 256 locks (L20)." 256)
(def max-subjects "An act's subject union holds at most 256 persons (L13)." 256)

;; ------------------------------------------------------------- randomness

(def ^:private ^SecureRandom rng (SecureRandom.))

(defn fresh-bytes
  "`n` bytes from SecureRandom. Impure: the gate calls it before its pure
  decision (plan step 7), never inside it."
  [n]
  (let [b (byte-array n)] (.nextBytes rng b) b))

(defn fresh-lock "A fresh 32-byte lock." [] (fresh-bytes lock-size))
(defn fresh-nonce "A fresh 12-byte nonce." [] (fresh-bytes nonce-size))

(defn fresh
  "`n-locks` fresh locks and `n-nonces` fresh nonces, as {:locks [..]
  :nonces [..]} (the plan's `fresh`)."
  [n-locks n-nonces]
  {:locks (vec (repeatedly n-locks fresh-lock))
   :nonces (vec (repeatedly n-nonces fresh-nonce))})

;; ------------------------------------------------------------ seal / open

(defn- bytes-of? [x n] (and (bytes? x) (= n (alength ^bytes x))))

(defn lock?
  "Whether x is a lock: exactly 32 bytes."
  [x]
  (bytes-of? x lock-size))

(defn same-bytes?
  "Whether a and b are byte arrays with equal contents."
  [a b]
  (and (bytes? a) (bytes? b) (Arrays/equals ^bytes a ^bytes b)))

(defn- gcm
  "An AES/GCM cipher initialised for `mode` under lock `K`."
  [mode K spec]
  (let [c (Cipher/getInstance "AES/GCM/NoPadding")
        ^bytes kb K
        ^AlgorithmParameterSpec s spec]
    (.init c (int mode) ^Key (SecretKeySpec. kb "AES") s)
    c))

(defn seal-with
  "AES-256-GCM of `plain` under lock `K` with the given 12-byte `nonce`:
  nonce ++ ciphertext ++ tag (L3). Pure: the gate's form, its nonces drawn
  before the decision. Nil on any failure (a wrong-length lock or nonce,
  a plaintext that is not bytes)."
  [K plain nonce]
  (try
    (when (and (lock? K) (bytes? plain) (bytes-of? nonce nonce-size))
      (let [^bytes plain plain
            ^bytes nonce nonce
            ^Cipher c (gcm Cipher/ENCRYPT_MODE K (GCMParameterSpec. 128 nonce))
            ct (.doFinal c plain)
            out (byte-array (+ nonce-size (alength ct)))]
        (System/arraycopy nonce 0 out 0 nonce-size)
        (System/arraycopy ct 0 out nonce-size (alength ct))
        out))
    (catch Throwable _ nil)))

(defn seal
  "`plain` sealed under `K` with its own fresh nonce: the door's form."
  [K plain]
  (seal-with K plain (fresh-nonce)))

(defn open
  "The plaintext of `sealed` under lock `K`, or nil when it does not open: a
  wrong lock, a tampered, foreign, truncated or swapped blob (AES-GCM fails
  closed; its AEADBadTagException is caught), a lock of the wrong length,
  or anything that is not bytes. Never throws."
  [K sealed]
  (try
    (when (and (lock? K) (bytes? sealed) (<= seal-overhead (alength ^bytes sealed)))
      (let [^bytes sealed sealed
            ^Cipher c (gcm Cipher/DECRYPT_MODE K (GCMParameterSpec. 128 sealed 0 nonce-size))]
        (.doFinal c sealed nonce-size (- (alength sealed) nonce-size))))
    (catch Throwable _ nil)))

(defn value-digest
  "The value digest (L26): HMAC-SHA256 keyed by the value's lock `K` over
  its plaintext bytes (the canonical EDN text, UTF-8), 32 bytes. Nil on any
  failure. The HMAC is keyed by the lock itself, the rider's plain reading;
  a kept store may derive a separate MAC key (first-record, L26)."
  [K plain]
  (try
    (when (and (lock? K) (bytes? plain))
      (let [mac (Mac/getInstance "HmacSHA256")]
        (.init mac ^Key (SecretKeySpec. ^bytes K "HmacSHA256"))
        (.doFinal mac ^bytes plain)))
    (catch Throwable _ nil)))

;; ------------------------------------------------------- plaintext values

(defn canonical-bytes
  "The bytes a value is sealed as and digested over: the UTF-8 bytes of its
  canonical EDN text (P12, L3; first-record). The door's form of a value."
  [v]
  (.getBytes ^String (env/canonical (env/normalize-value v)) StandardCharsets/UTF_8))

(defn decode-plain
  "The value an opened plaintext holds: {:value v :plain bytes} when the
  bytes are the UTF-8 canonical EDN text of one value in the store's domain
  (stage 1's P5 list, nested in at most 32 collections, F6), else
  {:refuse :malformed-value}. The text must be the canonical text of the
  value it reads as (rig choice: so a value has one plaintext, and its
  digest one input). Total: a reader failure, a stack overflow on a deep
  text, anything, is `:malformed-value`."
  [plain]
  (try
    (let [text (String. ^bytes plain StandardCharsets/UTF_8)
          v (edn/read-string {:eof ::eof} text)]
      (if (and (not= ::eof v) (env/edn-value? v))
        (let [nv (env/normalize-value v)]
          (if (= text (env/canonical nv))
            {:value nv :plain plain}
            {:refuse :malformed-value}))
        {:refuse :malformed-value}))
    (catch Throwable _ {:refuse :malformed-value})))

;; ----------------------------------------------------------------- leases

(defn lease-ids
  "The lock ids a lease named `lease-name` mints, `n` of them (L21):
  [[lease-name 0] ... [lease-name n-1]]. A function of the name and the
  count alone, so a resent lease returns the same ids with nothing stored."
  [lease-name n]
  (let [nm (into [] lease-name)]
    (into [] (map (fn [i] [nm (long i)])) (range n))))

(defn lock-id?
  "A well-formed lock id: [lease-name i], the name well formed, i a
  non-negative integer."
  [x]
  (and (vector? x) (= 2 (count x))
       (env/valid-name? (nth x 0))
       (int? (nth x 1)) (<= 0 (nth x 1))))

(defn lease-name-of "The name of the lease that minted a lock." [lock-id] (nth lock-id 0))

(defn lease-row
  "The lease row for lock `K` (L22, L23): K sealed under the person lock in
  `entry` of `under`, the lease act's person, with `nonce`; bare
  (`{:under nil :sealed K}`) when `under` is nil, a lease by the root
  actor, who has no person lock. Nil when the person's lock is destroyed or
  missing."
  [K under entry nonce]
  (if (nil? under)
    (when (lock? K) {:under nil :sealed K})
    (when-let [s (seal-with (:lock entry) K nonce)]
      {:under under :sealed s})))

(defn unlease
  "The delivery's pure half (plan, 'The delivery function'): the lock a
  lease row holds, given the person entry of its `:under` person, or nil
  when the row is missing, the person's lock is destroyed, or the row does
  not open. A row with `:under nil` holds K bare. Never throws; public so a
  gate that keeps lease rows in a PState of its own can call it over a row
  it read."
  [row entry]
  (try
    (when (map? row)
      (let [s (:sealed row)
            K (if (nil? (:under row)) s (open (:lock entry) s))]
        (when (lock? K) K)))
    (catch Throwable _ nil)))

;; ------------------------------------------------------------------ wraps

(def root-actor
  "Who owns the base and acts at the root: the operator, one principal for
  tonight (builder A; first-record, a question for Sid). It has no person
  lock, contributes no owner subject and is never looked up in `$$persons`
  (default 6, [V-F2])."
  :operator)

(defn person-owner
  "The layer's owner when it is a person, else nil: nil for the base (owned
  by the root actor) and for layers with no owner ([V-F2])."
  [owner]
  (when-not (= root-actor owner) owner))

(defn wrap-of
  "model.clj `wrap` under the baseline's `:owner-required` (plan, 'The
  shapes', I-L1), as sorted vectors, fixed at write. `owner` is the layer's
  person owner (nil in the base and shared layers), `subjects` the value's
  own subjects (the owner may be among them), `marked?` a `:die-with-any`
  mark:
  - marked: every subject required, the owner among them (7b's mark);
  - unmarked, a person owner: the owner alone required;
  - unmarked, no person owner: any one of the subjects (7b as written; an
    empty wrap for a value about no one)."
  [owner subjects marked?]
  (let [subjects (cond-> (set subjects) owner (conj owner))]
    (cond
      marked? {:required (into [] (sort subjects)) :any-of []}
      owner {:required [owner] :any-of []}
      :else {:required [] :any-of (into [] (sort subjects))})))

(defn wrap-persons
  "Every person a wrap names, required first, each once."
  [w]
  (into [] (distinct) (concat (:required w) (:any-of w))))

(defn seals-needed
  "How many nonces `wrap` takes for a wrap: one per required person and one
  per any-of person."
  [w]
  (+ (count (:required w)) (count (:any-of w))))

(def wrap-scheme
  "The scheme tag every lock record carries (first-record placeholder,
  builder A from the copies examination): `:aes-gcm-1` names this
  construction, a value lock sealed with AES-256-GCM (12-byte nonce
  prepended, 16-byte tag appended) under each required person's 32-byte
  lock in sorted order, and one copy of that chain per any-of person, so a
  later person-lock scheme (public-key, say) can sit beside it."
  :aes-gcm-1)

(defn wrap
  "The lock record of value lock `K` under wrap `w` (L3): K sealed under
  each required person's lock in the wrap's (sorted) order, the chain;
  when any-of is not empty, the chain sealed under each any-of person's
  lock into `:any-blobs` and `:blob` nil; else the chain in `:blob` (K bare
  for an empty wrap). `persons` maps person id to entry; `nonces` holds at
  least `seals-needed` fresh nonces, used in order. Nil when a named person
  has no live lock, or on any failure."
  [K w persons nonces]
  (try
    (let [required (into [] (:required w))
          any-of (into [] (:any-of w))
          lock-of (fn [p] (:lock (get persons p)))
          ns (into [] nonces)]
      (when (and (lock? K)
                 (every? #(lock? (lock-of %)) (concat required any-of))
                 (<= (seals-needed w) (count ns)))
        (let [chain (reduce (fn [b [p n]] (when b (seal-with (lock-of p) b n)))
                            K (map vector required ns))]
          (when chain
            (if (seq any-of)
              (let [blobs (into {} (map (fn [p n] [p (seal-with (lock-of p) chain n)])
                                        any-of (drop (count required) ns)))]
                (when (every? some? (vals blobs))
                  {:scheme wrap-scheme :required required :any-of any-of :blob nil :any-blobs blobs}))
              {:scheme wrap-scheme :required required :any-of any-of :blob chain :any-blobs nil})))))
    (catch Throwable _ nil)))

(defn unwrap
  "The value lock a lock record holds, given the person entries, or nil
  when it does not open: a required person's lock destroyed, every any-of
  person's lock destroyed, a record tampered with, or a record of another
  scheme (an untagged record is read as this scheme's). Never throws."
  [record persons]
  (try
    (when (and (map? record) (contains? #{nil wrap-scheme} (:scheme record)))
      (let [{:keys [required any-of blob any-blobs]} record
            lock-of (fn [p] (:lock (get persons p)))
            peel (fn [b] (reduce (fn [b p] (when b (open (lock-of p) b))) b (reverse required)))
            K (if (seq any-of)
                (some (fn [p] (when-let [c (open (lock-of p) (get any-blobs p))] (peel c))) any-of)
                (peel blob))]
        (when (lock? K) K)))
    (catch Throwable _ nil)))

(defn wrap-closed
  "model.clj `wrap-closed` with stamps for `:order` (L8): nil while the
  wrap opens by the person entries' dates, else the date it closed, the
  earliest of: each destroyed required person's `:erased-at`, and, only
  when every any-of person is destroyed, the latest any-of `:erased-at`.
  It gives the date only; whether a value opens is the ciphertext's to
  say (`unwrap`)."
  [w persons]
  (let [dead (fn [p] (:erased-at (get persons p)))
        dead-req (keep dead (:required w))
        any-of (:any-of w)
        dead-any (keep dead any-of)
        cands (cond-> (vec dead-req)
                (and (seq any-of) (= (count dead-any) (count any-of))) (conj (apply max dead-any)))]
    (when (seq cands) (apply min cands))))

(defn erasure
  "Why a value no longer opens, or nil while it does (model.clj `erasure`):
  the ledger's entry for its lock first, the value forget's date (O14,
  L16), as {:stamp s :how :row-deleted|:excised}; else the wrap of its lock
  `record` closed by person forgets, as {:stamp s :how :wrap-closed}."
  [ledger-entry record persons]
  (cond
    (some? ledger-entry) {:stamp (:stamp ledger-entry) :how (:how ledger-entry)}
    (map? record) (when-let [s (wrap-closed record persons)] {:stamp s :how :wrap-closed})
    :else nil))

;; -------------------------------------------------------------- placement

(def row-kinds "Layer kinds whose value locks sit in lock rows (ruling 7)." #{:personal :hand})

(defn row-lock?
  "Whether a lock goes to a lock row in the layer's `:locks` (personal and
  hand layers, or any value marked `:own-row`: D9, the mark overrides in
  every layer), else into the record (the row's `:lock`)."
  [kind marks]
  (boolean (or (contains? row-kinds kind) (contains? (set marks) :own-row))))

;; ---------------------------------------------------- facts and citations

(defn sealed?
  "A sealed fact: the door sealed its value under a leased lock (L24)."
  [f]
  (some? (:sealed f)))

(defn cited-ids
  "The distinct lock ids an act's facts cite, in the order they first
  appear: what the gate delivers and what it consumes."
  [facts]
  (into [] (comp (filter sealed?) (map :lock-id) (distinct)) facts))

;; ------------------------------------------------ the value checks (pure)
;; Each takes plain data and returns a refusal or nil, so a gate that never
;; holds a value in its decision (phase 3's leader, PV-F13) can compute them
;; where the value is opened and carry the reason to its decision.

(defn does-not-open
  ":does-not-open when a delivered lock does not open the sealed bytes."
  [K sealed]
  (when (nil? (open K sealed)) :does-not-open))

(defn shape-refusal
  ":value-shape when the key's grammar cannot read the value (L12)."
  [grammars k v]
  (when (= :value-shape (grammar/subjects-of grammars k v)) :value-shape))

(defn subjects-refusal
  ":too-many-subjects when an act's subject union is over the cap (L13)."
  [union]
  (when (< max-subjects (count union)) :too-many-subjects))

(defn grain-refusal
  ":grain-mismatch when an act's citations do not fit the grain in force
  (L30): under per-value a distinct lock id per value fact, under per-act
  one lock id for all of them."
  [grain facts]
  (let [ids (keep #(when (sealed? %) (:lock-id %)) facts)
        distinct-ids (count (set ids))]
    (when (seq ids)
      (if (= :per-act grain)
        (when (< 1 distinct-ids) :grain-mismatch)
        (when (not= (count ids) distinct-ids) :grain-mismatch)))))

(defn read-values
  "Open and check an act's value facts (plan, gate event step 5), pure and
  total. `facts` are the parsed offer's facts (a sealed fact carries
  `:sealed` and `:lock-id`); `delivered` maps each cited lock id to its
  lock; `owner` is the layer's person owner (nil in the base and shared
  layers); `carried` the offer's `:subjects`; `grain` the grain in force.

  Returns {:reason r :values {i v} :plain {i bytes} :subjects {i #{p}}
  :union #{p}}: per value fact (by index) its value, its canonical
  plaintext and its own subjects (owner, grammar, carried: the three
  sources, L19), and the act's subject union (owner, carried and every
  value's grammar subjects, ruling 8). `:reason` is nil when every check
  passes, else the first failing check in L27's order, each over every
  value fact: `:does-not-open`, `:malformed-value`, `:value-shape`,
  `:too-many-subjects`, `:grain-mismatch`. The later fields are present
  only as far as the checks got."
  ([facts delivered ctx] (read-values facts delivered ctx grammar/grammars))
  ([facts delivered {:keys [owner carried grain]} grammars]
   (try
     (let [idx (into [] (keep-indexed (fn [i f] (when (sealed? f) [i f]))) facts)
           base (cond-> (set carried) owner (conj owner))
           opened (into {} (map (fn [[i f]] [i (open (get delivered (:lock-id f)) (:sealed f))])) idx)]
       (if (some nil? (vals opened))
         {:reason :does-not-open}
         (let [decoded (into {} (map (fn [[i p]] [i (decode-plain p)])) opened)]
           (if (some #(contains? % :refuse) (vals decoded))
             {:reason :malformed-value}
             (let [values (update-vals decoded :value)
                   plain (update-vals decoded :plain)
                   named (into {} (map (fn [[i f]] [i (grammar/subjects-of grammars (:k f) (get values i))])) idx)]
               (if (some #{:value-shape} (vals named))
                 {:reason :value-shape :values values :plain plain}
                 (let [subjects (update-vals named #(into base %))
                       union (reduce into base (vals named))
                       out {:values values :plain plain :subjects subjects :union union}]
                   (assoc out :reason (or (subjects-refusal union) (grain-refusal grain facts))))))))))
     (catch Throwable _ {:reason :malformed-value}))))

(defn value-refusal
  "The first value check that fails for an act, in L27's order, or nil: the
  reason `read-values` gives, alone."
  [facts delivered ctx]
  (:reason (read-values facts delivered ctx)))

;; ------------------------------------------------- the record path's check

(defn check-resend
  "The record path's value check for a recorded yes ([V-F1]): the resend's
  value facts correspond to the recorded rows by position (the parts digest
  matched). `R` maps a value fact's index to its recorded lock (nil when
  forgotten or its wrap closed), `O` to the resend's own lock (nil when it
  does not deliver). Where both are present the resend's bytes are opened
  with O and the value digest recomputed under R over the plaintext, and
  compared with the row's `:digest`; bytes that do not open, or a digest
  that differs, give `:name-taken`. Where either is nil the value is not
  checked (the price of forgetting; a missing own lock skips so a replayed
  answer stays the same). Nil when every value is checked equal or skipped.
  Total: an unexpected failure is `:name-taken`, a face refusal that writes
  nothing."
  [facts rows R O]
  (try
    (let [bad? (fn [i f]
                 (let [r (get R i) o (get O i)]
                   (when (and r o)
                     (let [d (some-> (open o (:sealed f)) decode-plain :plain)]
                       (or (nil? d)
                           (not (same-bytes? (value-digest r d) (:digest (nth rows i nil)))))))))]
      (when (some true? (keep-indexed (fn [i f] (when (sealed? f) (boolean (bad? i f)))) facts))
        :name-taken))
    (catch Throwable _ :name-taken)))

;; ---------------------------------------------------------- opening a row

(defn open-with
  "The pure half of `open-value>` and `open-row>`: what a read shows of one
  row, given its act's `stamp`, the moment `T` (nil: now), the ledger entry
  of its lock id, its lock record (the row's `:lock`, else its lock row) and
  the person entries of the record's wrap. Exactly one of:
  - {:value v :stamp s} — a control fact's value, a retract's nil, or a
    sealed value opened through its lock;
  - {:erased-at date} — the ledger's date, else the date the wrap closed;
    shown whatever the date's relation to T (I-L7);
  - {:unreadable reason} — `:no-such-fact` (no row, or no stamp for it),
    `:after-moment` (stamped after T: nothing admitted after T is shown),
    `:does-not-open` (a live lock that fails on the bytes, which no write
    of this plan produces).
  Never throws."
  [row stamp T ledger-entry record persons]
  (try
    (cond
      (or (nil? row) (nil? stamp)) {:unreadable :no-such-fact}
      (and (some? T) (< T stamp)) {:unreadable :after-moment}
      (not (sealed? row)) {:value (env/decode-value (:v row)) :stamp stamp}
      :else
      (if-let [e (erasure ledger-entry record persons)]
        {:erased-at (:stamp e)}
        (let [d (some-> (unwrap record persons) (open (:sealed row)) decode-plain)]
          (if (contains? d :value)
            {:value (:value d) :stamp stamp}
            {:unreadable :does-not-open}))))
    (catch Throwable _ {:unreadable :does-not-open})))

;; ======================================================================
;; The decision's lock part (pure; gate/decide calls it)

(defn row-at
  "Row `i` of an act's rows, or nil: total for any index an offer carries
  (a fact id's index may be any non-negative long, and `nth` past int
  range throws)."
  [rows i]
  (when (and (int? i) (<= 0 i) (< i (count rows)))
    (nth rows i)))

(def lock-control-keys
  "The control keys this stage adds whose facts the gate acts on (L7, L10,
  L20, L28). An act carrying one carries that fact alone (rig choice: one
  lock act, one fact; the gate refuses otherwise `:malformed-control`)."
  #{:forget :lease :session-closed :person :forget-person})

(defn fact-of "The first fact of `offer` under key `k`, or nil." [offer k]
  (some #(when (= k (:k %)) %) (:facts offer)))

(defn lease-count "A lease act's count, or nil." [offer]
  (some-> (fact-of offer :lease) :v :count))

(defn forget-target
  "A value forget's target fact id, or nil; nil too when the target is not
  a well-formed fact id (refused `:malformed-control` by the decision), so
  the reads made before the decision never take a malformed key."
  [offer]
  (let [t (some-> (fact-of offer :forget) :v :target)]
    (when (env/fid? t) t)))

(defn person-target
  "The person a person act makes or forgets, or nil; nil too for an id
  that is not a readable keyword (refused `:malformed-control`)."
  [offer]
  (let [p (or (some-> (fact-of offer :person) :v :id)
              (some-> (fact-of offer :forget-person) :v :person))]
    (when (env/readable-keyword? p) p)))

(defn close-session "The session a session close closes, or nil." [offer]
  (some-> (fact-of offer :session-closed) :v :session))

(defn making-owner
  "The person a making act names as the new layer's owner (its owner fact
  on the layer's own entity, while the layer has no settings), or nil; nil
  for the root actor, who owns the base and is not a person ([V-F2])."
  [offer settings]
  (when (nil? settings)
    (some #(when (and (= (:layer offer) (:e %)) (= :owner (:k %)) (env/readable-keyword? (:v %)))
             (person-owner (:v %)))
          (:facts offer))))

(defn up-front-persons
  "The persons the gate reads before the delivery (plan, gate event step
  4): the layer's person owner, for an act that cites a lock, and the
  offer's writer when a person, for an act that cites a lock or is a lease
  (the rows a delivery opens are sealed under the lease act's writer, L23,
  who for an honest door is the offer's). None for any other act."
  [offer settings]
  (let [cites? (boolean (seq (cited-ids (:facts offer))))
        lease? (some? (fact-of offer :lease))]
    (into [] (comp (keep identity) (distinct))
          [(when cites? (person-owner (:owner settings)))
           (when (or cites? lease?) (person-owner (:who offer)))])))

(defn deliverable?
  "Whether a lock id can be delivered to an offer in `layer` under
  `session` at all: the session a keyword, the id well formed, and its
  lease made for this layer (a lock is only used in the layer it was
  leased in)."
  [layer session lock-id]
  (boolean (and (keyword? session) (lock-id? lock-id) (= layer (nth (lease-name-of lock-id) 0)))))

(defn delivery
  "What the delivery step hands on: the person entries read, each cited
  id's lock (nil when missing), and whether any is missing."
  [persons delivered]
  {:persons persons :delivered delivered :missing? (boolean (some nil? (vals delivered)))})

(defn value-context
  "`read-values` over the offer's facts with the layer's person owner, the
  offer's carried subjects and the grain in force."
  [offer settings delivered]
  (read-values (:facts offer) delivered
               {:owner (person-owner (:owner settings))
                :carried (:subjects offer)
                :grain (:grain settings)}))

(defn lock-plan
  "What a yes locks (plan, 'Re-wrap under the value's subjects'; L6): one
  entry per lock the act's value facts cite, in citation order, each
  {:lock-id :wrap :row? :facts [i ...]}. Per-value grain: a lock per value
  fact, wrapped over its own subjects with its own mark, a row by the
  layer's kind or its `:own-row` mark. Per-act grain: the act's one lock,
  wrapped over the act's union, marked when any value fact is, a row when
  the kind is personal or hand or any value fact is marked `:own-row`.
  `rv` is `read-values`' result with no reason."
  [offer settings rv]
  (let [owner (person-owner (:owner settings))
        kind (:kind settings)
        idx (into [] (keep-indexed (fn [i f] (when (sealed? f) [i f]))) (:facts offer))]
    (if (= :per-act (:grain settings))
      (when (seq idx)
        (let [marks (into #{} (mapcat (comp :mark second)) idx)]
          [{:lock-id (:lock-id (second (first idx)))
            :wrap (wrap-of owner (:union rv) (contains? marks :die-with-any))
            :row? (row-lock? kind marks)
            :facts (mapv first idx)}]))
      (mapv (fn [[i f]]
              {:lock-id (:lock-id f)
               :wrap (wrap-of owner (get-in rv [:subjects i]) (contains? (:mark f) :die-with-any))
               :row? (row-lock? kind (:mark f))
               :facts [i]})
            idx))))

(defn- plan-persons [offer settings rv]
  (when (and rv (nil? (:reason rv)))
    (into [] (comp (mapcat (comp wrap-persons :wrap)) (distinct)) (lock-plan offer settings rv))))

(defn persons-needed
  "The persons whose `$$persons` entries the decision reads after the
  delivery (plan step 6): the wraps' persons (only when the value checks
  passed), a making act's owner, a person act's target."
  [offer settings rv]
  (into [] (comp (keep identity) (distinct))
        (concat (plan-persons offer settings rv)
                [(making-owner offer settings) (person-target offer)])))

(defn fresh-for
  "The fresh randomness a decision may write (plan step 7), drawn before
  the pure decision: a lease's locks and, when the lease's writer is a
  person, one nonce each for their rows' seals; the re-wraps' nonces in
  `lock-plan` order; a new person's lock. Impure; total (a draw that fails
  is empty, and a decision that needs the bytes then fails closed)."
  [offer settings rv]
  (try
    (let [n (lease-count offer)
          n (if (and (int? n) (<= 1 n max-lease)) n 0)
          wraps (reduce + 0 (map (comp seals-needed :wrap)
                                 (when (and rv (nil? (:reason rv))) (lock-plan offer settings rv))))]
      {:locks (vec (repeatedly n fresh-lock))
       :lease-nonces (vec (repeatedly (if (person-owner (:who offer)) n 0) fresh-nonce))
       :nonces (vec (repeatedly wraps fresh-nonce))
       :person-lock (when (fact-of offer :person) (fresh-lock))})
    (catch Throwable _ {})))

(defn lock-context
  "The lock part of a decision's input (`lx`): the delivered locks, the
  value checks' result, every person entry read, a forget's target act
  rows and the ledger entry of its lock, and the fresh draw."
  [delivered rv persons target-rows target-ledger fresh]
  {:delivered delivered :read rv :persons persons
   :target-rows target-rows :target-ledger target-ledger :fresh fresh})

(defn empty-context
  "A context for a decision made with no lock reads (stage 1's pure tests):
  nothing delivered, no persons, and a fresh draw. A sealed value then
  does not open."
  [offer settings]
  (let [rv (value-context offer settings {})]
    (lock-context {} rv {} nil nil (fresh-for offer settings rv))))

(defn- persons-refusal [offer settings lx]
  (let [persons (:persons lx)
        wrap-ps (plan-persons offer settings (:read lx))
        lease-who (when (fact-of offer :lease) (person-owner (:who offer)))
        maker (making-owner offer settings)
        forgotten (when (fact-of offer :forget-person) (person-target offer))
        missing? (fn [p] (and (some? p) (nil? (get persons p))))
        dead? (fn [p] (and (some? p) (nil? (:lock (get persons p)))))]
    (cond
      (or (some missing? wrap-ps) (missing? lease-who) (missing? maker) (missing? forgotten))
      :no-such-person
      (or (some dead? wrap-ps) (dead? lease-who) (dead? maker))
      :person-forgotten)))

(defn lock-refusal
  "This stage's recorded reasons, after stage 1's list, in L27's order: the
  value checks' reason (`:does-not-open`, `:malformed-value`,
  `:value-shape`, `:too-many-subjects`, `:grain-mismatch`), then
  `:no-such-person` (a wrap person, a lease's writer, a making act's owner
  or a forget-person target with no entry), `:person-forgotten` (a wrap
  person, a lease's writer or a making act's owner whose lock is
  destroyed, L11), `:person-already-made`, `:no-such-value` (a forget whose
  target is no row of this layer's log, L10). Nil for a yes."
  [offer settings lx]
  (or (:reason (:read lx))
      (persons-refusal offer settings lx)
      (when (and (fact-of offer :person) (some? (get (:persons lx) (person-target offer))))
        :person-already-made)
      (when-let [t (forget-target offer)]
        (when (nil? (row-at (:target-rows lx) (nth t 1))) :no-such-value))))

(defn- fail!
  "An impossible state inside the decision: thrown, and turned by
  gate/decide's guard into the unrecorded face refusal :gate-error, so a
  value is never admitted half locked."
  [what]
  (throw (ex-info (str "lock decision: " what) {})))

(defn- value-writes [offer settings lx]
  (let [rv (:read lx)
        persons (:persons lx)
        delivered (:delivered lx)
        facts (:facts offer)]
    (loop [plan (seq (lock-plan offer settings rv))
           ns (:nonces (:fresh lx))
           rows {}
           lock-rows []]
      (if-let [{:keys [lock-id row?] w :wrap covered :facts} (first plan)]
        (let [k (seals-needed w)
              K (get delivered lock-id)
              rec (or (wrap K w persons (take k ns)) (fail! "a wrap did not seal"))
              rows (reduce (fn [m i]
                             (assoc m i (cond-> {:sealed (:sealed (nth facts i))
                                                 :lock-id lock-id
                                                 :digest (or (value-digest K (get-in rv [:plain i]))
                                                             (fail! "no value digest"))}
                                          (not row?) (assoc :lock rec))))
                           rows covered)]
          (recur (next plan) (drop k ns) rows (cond-> lock-rows row? (conj [lock-id rec]))))
        {:rows rows :lock-rows lock-rows}))))

(defn- lease-writes [offer lx]
  (when-let [n (lease-count offer)]
    (let [ids (lease-ids (:name offer) n)
          under (person-owner (:who offer))
          entry (get (:persons lx) under)
          Ks (:locks (:fresh lx))
          ns (if under (:lease-nonces (:fresh lx)) (repeat n nil))]
      (when-not (= n (count Ks) (count (take n ns))) (fail! "a lease without its fresh locks"))
      {:lease-rows (mapv (fn [id K nonce] [id (or (lease-row K under entry nonce) (fail! "a lease row did not seal"))])
                         ids Ks ns)
       :ack {:lock-ids ids}})))

(defn- forget-writes [offer lx stamp]
  (when-let [[tname tidx] (forget-target offer)]
    (let [trows (:target-rows lx)
          row (row-at trows tidx)
          lid (:lock-id row)]
      (if (or (nil? lid) (some? (:target-ledger lx)))
        {:ack {:how nil}}
        (let [shared (into [] (keep-indexed (fn [i r] (when (= lid (:lock-id r)) [(long i) r]))) trows)
              excise? (some? (:lock row))
              how (if excise? :excised :row-deleted)]
          {:lock-deletes (if excise? [] [lid])
           :row-writes (if excise? (mapv (fn [[i r]] [tname i (assoc r :lock nil)]) shared) [])
           :ledger [[lid {:stamp stamp :how how}]]
           :purge (mapv (fn [[i r]] {:fid [tname i] :row r}) shared)
           :ack {:how how}})))))

(defn- person-writes [offer lx stamp]
  (let [p (person-target offer)]
    (cond
      (fact-of offer :person)
      (let [K (:person-lock (:fresh lx))]
        (when-not (lock? K) (fail! "a person without a fresh lock"))
        {:person [p {:lock K :erased-at nil}] :fan-out p})

      (fact-of offer :forget-person)
      (if (some? (:erased-at (get (:persons lx) p)))
        {:fan-out p}
        {:person [p {:lock nil :erased-at stamp}] :fan-out p}))))

(defn lock-effects
  "What a decision writes for locks (plan, 'Writes'), precomputed, every
  entry a set or a delete keyed by lock id, session, fact id, stamp or
  person. For a refusal (`stamp` nil) only `:consume`, the cited lease rows
  (a recorded no destroys its leases). For a yes also: `:rows`, per value
  fact index the log row's added fields (`:sealed` as offered, `:lock-id`,
  `:lock` for a record lock, `:digest`); `:lock-rows`; a lease's
  `:lease-rows`; a session's `:close`; a forget's `:lock-deletes` or
  `:row-writes` (the excision), `:ledger` and `:purge` (the erased values,
  for the read exit's purge); a person act's `:person` (the home's entry)
  and `:fan-out`; `:by-stamp`; and `:ack`, what the answer adds (a lease's
  `:lock-ids`, a forget's `:how`)."
  [offer settings lx stamp]
  (let [consume (cited-ids (:facts offer))]
    (if (nil? stamp)
      {:consume consume}
      (let [v (value-writes offer settings lx)
            l (lease-writes offer lx)
            f (forget-writes offer lx stamp)
            p (person-writes offer lx stamp)]
        {:consume consume
         :rows (:rows v)
         :lock-rows (:lock-rows v)
         :lease-rows (:lease-rows l)
         :close (close-session offer)
         :lock-deletes (:lock-deletes f)
         :row-writes (:row-writes f)
         :ledger (:ledger f)
         :purge (:purge f)
         :person (:person p)
         :fan-out (:fan-out p)
         :by-stamp [stamp (:name offer)]
         :ack (merge (:ack l) (:ack f))}))))

;; ------------------------------------------ the record path's parts (pure)

(defn resend-check?
  "Whether a name's record calls for the value check (V-F1): a recorded yes
  with the parts digest matched, for an act with sealed facts."
  [offer rec d0]
  (boolean (and (= :recorded (:kind d0)) (= :yes (:answer rec)) (some sealed? (:facts offer)))))

(defn forget-how?
  "Whether a recorded yes is a value forget, whose answer carries `:how`."
  [offer rec d0]
  (boolean (and (= :recorded (:kind d0)) (= :yes (:answer rec)) (forget-target offer))))

(defn records-persons
  "Every person the lock records name, each once."
  [records]
  (into [] (comp (filter map?) (mapcat wrap-persons) (distinct)) (vals records)))

(defn own-lock-pairs
  "[index cited-id] for each sealed fact of a resend whose cited lock is not
  its recorded row's (a door that lost its locks and leased again)."
  [offer rows]
  (into [] (keep-indexed (fn [i f] (when (and (sealed? f) (not= (:lock-id f) (:lock-id (nth rows i nil))))
                                     [(long i) (:lock-id f)])))
        (:facts offer)))

(defn resend-verdict
  "The record path's check over what it read: per value fact the recorded
  lock R (its record unwrapped with the persons read) and its own lock O
  (R when it cites the recorded id, else the delivered one), then
  `check-resend`."
  [offer rows records persons own]
  (let [facts (:facts offer)
        R (into {} (keep-indexed (fn [i f] (when (sealed? f)
                                             [i (unwrap (get records (:lock-id (nth rows i nil))) persons)])))
                facts)
        O (into {} (keep-indexed (fn [i f] (when (sealed? f)
                                             [i (if (= (:lock-id f) (:lock-id (nth rows i nil))) (get R i) (get own i))])))
                facts)]
    (check-resend facts rows R O)))

(defn record-answer
  "The record path's outcome: {:ack :consume :fan-out}. A name taken (by
  the parts digest or the value check) writes nothing. The recorded
  answer, yes or no, consumes every cited lease id under the offer's
  session ([V-F1, V-F3]); a lease's yes carries its `:lock-ids`, a
  forget's yes its `:how` from the ledger (the forget's own erasure when
  the ledger's date is its stamp, else nil: it changed nothing), and a
  person act's yes fans out again (L9)."
  [offer rec d0 verdict target-ledger]
  (cond
    (not= :recorded (:kind d0)) {:ack (:ack d0)}
    (= :name-taken verdict) {:ack {:answer :no :reason :name-taken :stamp nil :name (:name offer)}}
    :else
    (let [yes? (= :yes (:answer rec))
          n (lease-count offer)]
      {:ack (cond-> (:ack d0)
              (and yes? (int? n)) (assoc :lock-ids (lease-ids (:name offer) n))
              (and yes? (forget-target offer))
              (assoc :how (when (and target-ledger (= (:stamp target-ledger) (:stamp rec))) (:how target-ledger))))
       :consume (cited-ids (:facts offer))
       :fan-out (when (and yes? (or (fact-of offer :person) (fact-of offer :forget-person)))
                  (person-target offer))})))

;; ------------------------------------------- reads and enumeration (pure)

(defn readable-fid?
  "A fact id a point read can take: well formed, its index within the range
  a row vector can hold."
  [fid]
  (boolean (and (env/fid? fid) (< (nth fid 1) Integer/MAX_VALUE))))

(defn target-lock-id "The lock id of a forget's target row, or nil." [rows target]
  (:lock-id (row-at rows (nth target 1))))

(defn rows-under "The persons lease rows are sealed under, each once." [rows]
  (into [] (comp (keep (comp :under second)) (distinct)) rows))

(defn leased-locks
  "`lease-locks`' answer: the grain in force and every unconsumed lock of
  the rows read, unleased; a row that does not unlease is left out."
  [settings rows persons]
  {:grain (:grain settings)
   :locks (into {} (keep (fn [[id row]] (when-let [K (unlease row (get persons (:under row)))] [id K]))) rows)})

(defn wrap-persons-of "A lock record's persons, or none." [record]
  (if (map? record) (wrap-persons record) []))

(defn names-person?
  "Whether an answer record is a yes whose subject slot holds `p`: the acts
  whose values may be wrapped under p (a value's wrap persons are among its
  own subjects, which are among the act's)."
  [rec p]
  (boolean (and (= :yes (:answer rec)) (contains? (:subjects rec) p))))

(defn locked-rows "[idx row] for each row of an act that has a lock id." [rows]
  (into [] (keep-indexed (fn [i r] (when (:lock-id r) [(long i) r]))) rows))

(defn closes-with?
  "Whether a lock record names `p` and its wrap is closed by the person
  entries: a value that died with p's forget."
  [record persons p]
  (boolean (and (map? record) (some #{p} (wrap-persons record)) (some? (wrap-closed record persons)))))

(defn erased-item "One erased value, as the purge seam takes it." [nm idx row]
  [{:fid [nm idx] :row row}])

(defn fact-entry
  "One fact of `read-as-of`'s answer: its id, stamp, e, k, replaces and
  mark, and what `open-row>` showed (`:value`, `:erased-at`, or
  `:unreadable`)."
  [nm idx row stamp r]
  (merge {:id [nm idx] :stamp stamp :e (:e row) :k (:k row) :replaces (:replaces row) :mark (:mark row)}
         (dissoc r :stamp)))

(defn as-of-result
  "`read-as-of`'s answer: the moment, every fact of every yes act stamped
  at or before it, by stamp then index, and the erasure ledger whole."
  [T facts erased]
  {:as-of T
   :facts (into [] (sort-by (juxt :stamp #(nth (:id %) 1)) facts))
   :erased (into {} erased)})

(defn indexed "[i x] pairs of a collection, i a long." [xs]
  (into [] (map-indexed (fn [i x] [(long i) x])) xs))

(defn opens-at?
  "Whether a sealed row stamped `stamp` is to be opened for a read as of
  `T` (nil: now). Total: a moment that is not a stamp opens nothing."
  [row stamp T]
  (boolean (and (sealed? row) (int? stamp) (or (nil? T) (and (int? T) (<= stamp T))))))

;; ======================================================================
;; Install functions: the schema fields, $$persons, the query topologies

(defn- lock-record-schema []
  (fixed-keys-schema {:scheme clojure.lang.Keyword
                      :required clojure.lang.PersistentVector
                      :any-of clojure.lang.PersistentVector
                      :blob byte/1
                      :any-blobs (map-schema clojure.lang.Keyword byte/1)}))

(defn row-fields
  "The fields this stage adds to a log row (plan, 'PState Design'; L24):
  the offer's sealed bytes as they came, the cited lock id, the lock
  record when kept in the record (nil for a row lock, nil once excised),
  and the value digest. Every byte slot raw bytes (L4)."
  []
  {:sealed byte/1
   :lock-id clojure.lang.PersistentVector
   :lock (lock-record-schema)
   :digest byte/1})

(defn layer-fields
  "The fields this stage adds to a layer's value in `$$layers` (plan,
  'PState Design', L5, L22): the lock rows, the lease rows (session, then
  lock id), the erasure ledger and the by-stamp index, all subindexed with
  size tracking off (L15)."
  []
  {:locks (map-schema clojure.lang.PersistentVector (lock-record-schema)
                      {:subindex-options {:track-size? false}})
   :leases (map-schema clojure.lang.Keyword
                       (map-schema clojure.lang.PersistentVector
                                   (fixed-keys-schema {:under clojure.lang.Keyword :sealed byte/1})
                                   {:subindex-options {:track-size? false}})
                       {:subindex-options {:track-size? false}})
   :erased (map-schema clojure.lang.PersistentVector
                       (fixed-keys-schema {:stamp Long :how clojure.lang.Keyword})
                       {:subindex-options {:track-size? false}})
   :by-stamp (map-schema Long clojure.lang.PersistentVector
                         {:subindex-options {:track-size? false}})})

(defn persons-schema
  "`$$persons`: every person's lock, on every task (L1): 32 random bytes,
  nil once destroyed, and the forget-person fact's stamp."
  []
  {clojure.lang.Keyword (fixed-keys-schema {:lock byte/1 :erased-at Long})})

(defn declare-pstates!
  "Declares `$$persons` on the gate topology `s`, which owns it."
  [s]
  ;; the Rama kondo hook reads declare-pstate inside defmodule only
  #_:clj-kondo/ignore
  (declare-pstate s $$persons (persons-schema)))

;; ======================================================================
;; The dataflow ops: call sites inside the gate's one event (plan,
;; 'Interfaces'), and the reads. Local to the task they run on.

(deframafn read-persons>
  "The `$$persons` entries of the persons in `*ps` not already in
  `*persons`, read on this task (every task holds every person's entry,
  L1) and merged in: one local seek each; a person with no entry maps to
  nil."
  [*ps *persons]
  (<<with-substitutions [$$persons (rama/this-module-pobject-task-global "$$persons")]
    (loop<- [*todo (seq *ps) *acc *persons :> *out]
      (<<if (empty? *todo)
        (:> *acc)
       (else>)
        (first *todo :> *p)
        (<<if (contains? *acc *p)
          (continue> (rest *todo) *acc)
         (else>)
          (local-select> (keypath *p) $$persons :> *entry)
          (continue> (rest *todo) (assoc *acc *p *entry)))))
    (:> *out)))

(deframafn deliver-lock>
  "The delivery function (plan, 'The delivery function'; L25), the lease
  body: the lock the lease row `[layer :leases session lock-id]` holds,
  unleased with its `:under` person's entry (from `*persons`, read from
  `$$persons` only when absent), or nil: never leased, leased to another
  session or for another layer, already consumed, destroyed at a session
  close, or unopenable because its person was forgotten. One seek.
  Read-only under every body, so a face refusal after a partial delivery
  consumes nothing ([V-F3]). Total. The holder road would replace this
  body alone."
  [*layer *session *lock-id *persons]
  (<<with-substitutions [$$layers (rama/this-module-pobject-task-global "$$layers")
                         $$persons (rama/this-module-pobject-task-global "$$persons")]
    (<<if (deliverable? *layer *session *lock-id)
      (local-select> (keypath *layer :leases *session *lock-id) $$layers :> *row)
      (get *row :under :> *under)
      (<<if (or> (nil? *under) (contains? *persons *under))
        (:> (unlease *row (get *persons *under)))
       (else>)
        (local-select> (keypath *under) $$persons :> *entry)
        (:> (unlease *row *entry)))
     (else>)
      (:> nil))))

(deframaop consume-locks>
  "The one place a lock leaves the delivery's store ([V-F3]): a no-read
  `NONE>` on each cited lease row under the offer's session, idempotent.
  Called in the decision's group for a yes and a recorded no, and on the
  record path for the recorded answer; never on a face refusal."
  [*layer *session *lock-ids]
  (<<with-substitutions [$$layers (rama/this-module-pobject-task-global "$$layers")]
    (<<if (and> (keyword? *session) (seq *lock-ids))
      (<<atomic
        (ops/explode *lock-ids :> *id)
        (local-transform> [(keypath *layer :leases *session *id) NONE>] $$layers)))
    (:>)))

(deframafn deliver-all>
  "The gate's delivery step (plan, gate event step 4): the up-front person
  entries (the layer's person owner, the offer's writer), then
  `deliver-lock>` per distinct cited lock id. Returns `delivery`'s map;
  `:missing?` true means the offer is refused `:no-such-lock` on its face,
  with nothing recorded, consumed or written."
  [*layer *offer *settings]
  (up-front-persons *offer *settings :> *ps)
  (read-persons> *ps {} :> *persons)
  (cited-ids (get *offer :facts) :> *ids)
  (get *offer :session :> *session)
  (loop<- [*todo (seq *ids) *acc {} :> *delivered]
    (<<if (empty? *todo)
      (:> *acc)
     (else>)
      (first *todo :> *id)
      (deliver-lock> *layer *session *id *persons :> *k)
      (continue> (rest *todo) (assoc *acc *id *k))))
  (:> (delivery *persons *delivered)))

(deframafn decision-reads>
  "The reads and draws a fresh decision needs beyond stage 1's (plan, gate
  event steps 5 to 7): the value checks over the delivered locks (pure),
  the person entries they and the act need, a forget's target act rows and
  the ledger entry of its lock, and the fresh randomness. Returns the lock
  context `lx` for gate/decide."
  [*layer *offer *settings *lk]
  (<<with-substitutions [$$layers (rama/this-module-pobject-task-global "$$layers")]
    (get *lk :delivered :> *delivered)
    (value-context *offer *settings *delivered :> *rv)
    (persons-needed *offer *settings *rv :> *ps)
    (read-persons> *ps (get *lk :persons) :> *persons)
    (forget-target *offer :> *target)
    (<<if (some? *target)
      (first *target :> *tname)
      (local-select> [(keypath *layer :log *tname) (subselect ALL)] $$layers :> *trows)
      (target-lock-id *trows *target :> *tlid)
      (<<if (some? *tlid)
        (local-select> (keypath *layer :erased *tlid) $$layers :> *tledger)
       (else>)
        (identity nil :> *tledger))
     (else>)
      (identity nil :> *trows)
      (identity nil :> *tledger))
    (fresh-for *offer *settings *rv :> *fresh)
    (:> (lock-context *delivered *rv *persons *trows *tledger *fresh))))

(deframafn row-records>
  "{lock-id record} for every value row of an act: the row's own `:lock` (a
  record lock), else its lock row, read once per lock id (nil once
  deleted)."
  [*layer *rows]
  (<<with-substitutions [$$layers (rama/this-module-pobject-task-global "$$layers")]
    (loop<- [*todo (seq *rows) *acc {} :> *out]
      (<<if (empty? *todo)
        (:> *acc)
       (else>)
        (first *todo :> *row)
        (get *row :lock-id :> *lid)
        (<<cond
          (case> (or> (nil? *lid) (contains? *acc *lid)))
          (continue> (rest *todo) *acc)

          (case> (some? (get *row :lock)))
          (continue> (rest *todo) (assoc *acc *lid (get *row :lock)))

          (default>)
          (local-select> (keypath *layer :locks *lid) $$layers :> *lrec)
          (continue> (rest *todo) (assoc *acc *lid *lrec)))))
    (:> *out)))

(deframafn own-locks>
  "{index lock} for each [index cited-id] pair: `deliver-lock>` of the
  cited id under the offer's own session (the record path, [V-F1])."
  [*layer *session *pairs *persons]
  (loop<- [*todo (seq *pairs) *acc {} :> *out]
    (<<if (empty? *todo)
      (:> *acc)
     (else>)
      (first *todo :> [*i *id])
      (deliver-lock> *layer *session *id *persons :> *k)
      (continue> (rest *todo) (assoc *acc *i *k))))
  (:> *out))

(deframafn record-path>
  "The record path for a name decided on this task (plan, 'The digest and
  the resend check', [V-F1]), given stage 1's verdict `*d0` over the parts
  digest: for a recorded yes of an act with sealed facts, the act's rows,
  their lock records and persons, the resend's own locks where it cites
  new ones, and the value check; for a recorded yes forget, its target's
  ledger entry (its `:how`). Returns `record-answer`'s {:ack :consume
  :fan-out}; the caller does the one write, the consumption."
  [*layer *offer *rec *d0]
  (<<with-substitutions [$$layers (rama/this-module-pobject-task-global "$$layers")]
    (get *offer :name :> *name)
    (<<if (resend-check? *offer *rec *d0)
      (local-select> [(keypath *layer :log *name) (subselect ALL)] $$layers :> *rows)
      (row-records> *layer *rows :> *records)
      (read-persons> (records-persons *records) {} :> *persons)
      (own-lock-pairs *offer *rows :> *pairs)
      (own-locks> *layer (get *offer :session) *pairs *persons :> *own)
      (resend-verdict *offer *rows *records *persons *own :> *verdict)
     (else>)
      (identity nil :> *verdict))
    (<<if (forget-how? *offer *rec *d0)
      (forget-target *offer :> *target)
      (first *target :> *tname)
      (local-select> [(keypath *layer :log *tname) (subselect ALL)] $$layers :> *trows)
      (target-lock-id *trows *target :> *tlid)
      (<<if (some? *tlid)
        (local-select> (keypath *layer :erased *tlid) $$layers :> *tledger)
       (else>)
        (identity nil :> *tledger))
     (else>)
      (identity nil :> *tledger))
    (:> (record-answer *offer *rec *d0 *verdict *tledger))))

(deframaop purge-read-indexes>
  "THE SEAM to the read exit's purge by value id (PLAN-read-exit.md,
  'Purge and rebuild'). A forget calls it in its own event on the values'
  task: a value forget once, after the ledger write, with every value its
  lock erased; a person forget once per value that dies with the person,
  on every task, in the fan-out child. `*erased` is a vector of {:fid
  [name idx] :row row} (the row as it stood before the forget),
  `*forget-stamp` the forget fact's stamp. A no-op tonight: the read
  exit's indexes are not on this branch. The merge replaces this body with
  the purge (its RE4 read of each value's `:ix-kv` addresses and the act's
  stamp, `reads/purge-writes`, and the three write blocks), so the purge
  commits with the forget. Tonight it only hands the call to the test
  recorder (rig.store.inject `purged!`, off unless a test turns it on)."
  [*layer *erased *forget-stamp]
  (inject/purged! (ops/current-task-id) *layer *erased *forget-stamp)
  (:>))

(deframaop write-decision>
  "The lock writes of a fresh decision (plan, 'Writes'), in the decision's
  one atomic group after stage 1's writes: the consumption of the cited
  lease rows (yes or no), the lock rows, a lease's rows, a session close, a
  forget's deletion or excision, its ledger entry and the purge seam, a
  person act's entry on the home, and the by-stamp entry. Every write a
  `termval` or a `NONE>` keyed by id."
  [*layer *offer *d]
  (<<with-substitutions [$$layers (rama/this-module-pobject-task-global "$$layers")
                         $$persons (rama/this-module-pobject-task-global "$$persons")]
    (get *d :locks :> *fx)
    (get *offer :session :> *session)
    (consume-locks> *layer *session (get *fx :consume))
    (<<atomic
      (ops/explode (get *fx :lock-rows) :> [*lid *lrec])
      (local-transform> [(keypath *layer :locks *lid) (termval *lrec)] $$layers))
    (<<atomic
      (ops/explode (get *fx :lease-rows) :> [*lid *lrow])
      (local-transform> [(keypath *layer :leases *session *lid) (termval *lrow)] $$layers))
    (get *fx :close :> *close)
    (<<if (some? *close)
      (local-transform> [(keypath *layer :leases *close) NONE>] $$layers))
    (<<atomic
      (ops/explode (get *fx :lock-deletes) :> *lid)
      (local-transform> [(keypath *layer :locks *lid) NONE>] $$layers))
    (<<atomic
      (ops/explode (get *fx :row-writes) :> [*rname *ridx *rrow])
      (local-transform> [(keypath *layer :log *rname *ridx) (termval *rrow)] $$layers))
    (<<atomic
      (ops/explode (get *fx :ledger) :> [*lid *entry])
      (local-transform> [(keypath *layer :erased *lid) (termval *entry)] $$layers))
    (get *fx :purge :> *purge)
    (<<if (seq *purge)
      (purge-read-indexes> *layer *purge (get *d :stamp)))
    (get *fx :person :> *person)
    (<<if (some? *person)
      (first *person :> *p)
      (second *person :> *pentry)
      (local-transform> [(keypath *p) (termval *pentry)] $$persons))
    (get *fx :by-stamp :> *bs)
    (<<if (some? *bs)
      (first *bs :> *bstamp)
      (second *bs :> *bname)
      (local-transform> [(keypath *layer :by-stamp *bstamp) (termval *bname)] $$layers))
    (:>)))

(deframaop purge-dying>
  "On this task, after a person forget's fan-out child destroyed `*p`'s
  lock here (builder A's addition): every value whose wrap closes with the
  forget, passed to the purge seam one by one. It scans every layer homed
  on this task, and in each the acts whose subject slot names the person,
  then those acts' rows, their lock records and persons, yielding as it
  goes. Its cost is O(acts on the task) iterations plus a few seeks per
  value that dies: over any per-event budget at scale (BUILD_NOTES)."
  [*p *entry]
  (<<with-substitutions [$$layers (rama/this-module-pobject-task-global "$$layers")]
    (local-select> MAP-KEYS $$layers {:allow-yield? true} :> *layer)
    (local-select> [(keypath *layer :answers) ALL] $$layers {:allow-yield? true} :> [*aname *arec])
    (<<if (names-person? *arec *p)
      (local-select> [(keypath *layer :log *aname) (subselect ALL)] $$layers {:allow-yield? true} :> *arows)
      (ops/explode (locked-rows *arows) :> [*ridx *rrow])
      (get *rrow :lock-id :> *rlid)
      (local-select> (keypath *layer :erased *rlid) $$layers :> *rledger)
      (<<if (nil? *rledger)
        (get *rrow :lock :> *rlock)
        (<<if (some? *rlock)
          (identity *rlock :> *record)
         (else>)
          (local-select> (keypath *layer :locks *rlid) $$layers :> *record))
        (read-persons> (wrap-persons-of *record) (hash-map *p *entry) :> *wpersons)
        (<<if (closes-with? *record *wpersons *p)
          (purge-read-indexes> *layer (erased-item *aname *ridx *rrow) (get *entry :erased-at)))))))

(deframaop fan-out>
  "The person fan-out (plan, gate event step 11; L9): for a person act
  answered yes, fresh or recorded, the home's `$$persons` entry as it
  stands, then `(|all)` (the commit boundary for the home's writes), then
  an unconditional `termval` of that entry on every task; for a person
  forget, each task then passes the values that died with the person to
  the purge seam. The crash hook's `:fan-out` point is in the child."
  [*name *p]
  (<<with-substitutions [$$persons (rama/this-module-pobject-task-global "$$persons")]
    (<<if (some? *p)
      (local-select> (keypath *p) $$persons :> *entry)
      (|all)
      (inject/point! :fan-out *name)
      (local-transform> [(keypath *p) (termval *entry)] $$persons)
      (<<if (some? (get *entry :erased-at))
        (purge-dying> *p *entry)))))

;; ------------------------------------------------------------- the reads

;; *fid is the read exit's interface (it passes the row's id); the body needs the row
#_{:clj-kondo/ignore [:unused-binding]}
(deframafn open-row>
  "What a read shows of one row it already holds, with its act's stamp
  (the read exit's interface): the ledger entry of its lock, its lock
  record (its `:lock`, else the lock row) and the wrap's person entries,
  all local, then `open-with`. Exactly one of {:value v :stamp s},
  {:erased-at date}, {:unreadable reason}. Reads nothing for a control
  fact, a retract, a missing row or a row after `*T`. `*fid` is the
  interface's (the read exit passes it); the row already names its value."
  [*layer *fid *row *stamp *T]
  (<<with-substitutions [$$layers (rama/this-module-pobject-task-global "$$layers")]
    (<<if (opens-at? *row *stamp *T)
      (get *row :lock-id :> *lid)
      (local-select> (keypath *layer :erased *lid) $$layers :> *ledger)
      (<<cond
        (case> (some? *ledger))
        (:> (open-with *row *stamp *T *ledger nil {}))

        (case> (some? (get *row :lock)))
        (get *row :lock :> *record)
        (read-persons> (wrap-persons-of *record) {} :> *persons)
        (:> (open-with *row *stamp *T nil *record *persons))

        (default>)
        (local-select> (keypath *layer :locks *lid) $$layers :> *lrecord)
        (read-persons> (wrap-persons-of *lrecord) {} :> *lpersons)
        (:> (open-with *row *stamp *T nil *lrecord *lpersons)))
     (else>)
      (:> (open-with *row *stamp *T nil nil {})))))

(deframafn open-value>
  "Whether one value opens, and as of `*T` (RD7; the read exit's
  interface): the act's answer (its stamp), the row, then `open-row>`, all
  on the layer's home. `*fid` is [name idx]; `*T` a stamp or nil (now).
  Exactly one of {:value v :stamp s} (v nil for a retract), {:erased-at
  date} (the ledger's date, else the person-forget date the wrap's close
  gives), or {:unreadable reason}: `:no-such-fact`, `:after-moment` (the
  fact's stamp is after T), `:does-not-open` (a live lock that fails on
  the bytes, which no write of this plan produces). Never throws."
  [*layer *fid *T]
  (<<with-substitutions [$$layers (rama/this-module-pobject-task-global "$$layers")]
    (<<if (readable-fid? *fid)
      (first *fid :> *name)
      (second *fid :> *idx)
      (local-select> (keypath *layer :answers *name) $$layers :> *rec)
      (<<if (= :yes (get *rec :answer))
        ;; one seek; past the end of the row vector it navigates to nil
        (local-select> (keypath *layer :log *name *idx) $$layers :> *row)
        (get *rec :stamp :> *stamp)
       (else>)
        (identity nil :> *row)
        (identity nil :> *stamp))
     (else>)
      (identity nil :> *row)
      (identity nil :> *stamp))
    (open-row> *layer *fid *row *stamp *T :> *r)
    (:> *r)))

(deframaop read-as-of>
  "The read as of a moment (RD4's erasure part, I-L7; L18), on the layer's
  home: every yes act stamped at or before `*T` (the by-stamp range), each
  of its rows through `open-row>`, and the erasure ledger whole. Yields as
  it goes. Stage 5's read exit replaces it at the merge."
  [*layer *T]
  (<<with-substitutions [$$layers (rama/this-module-pobject-task-global "$$layers")]
    (<<if (int? *T)
      (local-select> [(keypath *layer :by-stamp) (sorted-map-range-to *T {:inclusive? true})]
                     $$layers {:allow-yield? true} :> *acts)
     (else>)
      (identity {} :> *acts))
    (loop<- [*todo (seq *acts) *acc [] :> *facts]
      (yield-if-overtime)
      (<<if (empty? *todo)
        (:> *acc)
       (else>)
        (first *todo :> [*stamp *name])
        (local-select> [(keypath *layer :log *name) (subselect ALL)] $$layers {:allow-yield? true} :> *rows)
        (loop<- [*rtodo (seq (indexed *rows)) *racc *acc :> *acc2]
          (<<if (empty? *rtodo)
            (:> *racc)
           (else>)
            (first *rtodo :> [*idx *row])
            (open-row> *layer (vector *name *idx) *row *stamp *T :> *r)
            (continue> (rest *rtodo) (conj *racc (fact-entry *name *idx *row *stamp *r)))))
        (continue> (rest *todo) *acc2)))
    (local-select> [(keypath *layer :erased) (subselect ALL)] $$layers {:allow-yield? true} :> *erased)
    (:> (as-of-result *T *facts *erased))))

(deframaop lease-locks>
  "The door's one path to a leased lock's plaintext (plan, '`lease-locks`';
  L29), on the layer's home: the settings (the grain in force), the
  session's lease rows in one range read, the entries of the rows' `:under`
  persons (one for a session), and each row unleased. The person lock
  never leaves the module."
  [*layer *session]
  (<<with-substitutions [$$layers (rama/this-module-pobject-task-global "$$layers")]
    (local-select> (keypath *layer :settings) $$layers :> *settings)
    (<<if (keyword? *session)
      (local-select> [(keypath *layer :leases *session) (subselect ALL)] $$layers {:allow-yield? true} :> *rows)
     (else>)
      (identity [] :> *rows))
    (read-persons> (rows-under *rows) {} :> *persons)
    (:> (leased-locks *settings *rows *persons))))

(defn declare-queries!
  "Declares this stage's two query topologies on the module's
  `topologies`: `lease-locks [layer session]` and `read-as-of [layer T]`,
  each routed by `(|hash layer)` to the layer's home, as the depot's
  `hash-by :layer` and `$$layers`' key partitioner place it (P2)."
  [topologies]
  ;; the Rama kondo hook reads <<query-topology inside defmodule only
  #_:clj-kondo/ignore
  (<<query-topology topologies "lease-locks" [*layer *session :> *result]
    (|hash *layer)
    (lease-locks> *layer *session :> *out)
    (|origin)
    (identity *out :> *result))
  #_:clj-kondo/ignore
  (<<query-topology topologies "read-as-of" [*layer *T :> *result]
    (|hash *layer)
    (read-as-of> *layer *T :> *out)
    (|origin)
    (identity *out :> *result)))
