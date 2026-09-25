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

  This part is pure and total: every function returns nil (or a refusal as
  data) on any failure and never throws, because an exception in topology
  code kills the worker and replays (SPEC 'What Rama showed' 3). The only
  impure functions draw fresh bytes from SecureRandom (`fresh-bytes` and
  its callers); the gate binds them before its pure decision.

  Shapes (plan, 'The shapes'): a lock is 32 bytes; a lock id is
  `[lease-name i]`; a lease row is `{:under p :sealed bytes}` (bare K with
  `:under nil` for the root actor); a wrap is `{:required [p ...] :any-of
  [p ...]}`, sorted vectors; a lock record is the wrap plus `:blob` (K
  sealed under the required chain, nil when any-of is not empty) and
  `:any-blobs` ({p bytes}, nil otherwise); a person entry is `{:lock bytes
  :erased-at stamp}`, `:lock` nil once destroyed.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [clojure.edn :as edn]
            [rig.store.envelope :as env]
            [rig.store.grammar :as grammar])
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
                  {:required required :any-of any-of :blob nil :any-blobs blobs}))
              {:required required :any-of any-of :blob chain :any-blobs nil})))))
    (catch Throwable _ nil)))

(defn unwrap
  "The value lock a lock record holds, given the person entries, or nil
  when it does not open: a required person's lock destroyed, every any-of
  person's lock destroyed, a record tampered with. Never throws."
  [record persons]
  (try
    (when (map? record)
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
