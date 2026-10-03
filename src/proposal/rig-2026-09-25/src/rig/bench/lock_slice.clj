;; Phase 7, the lock-growth number. A throwaway bench slice, not stage 2's
;; build: it writes lock rows in the shapes PLAN-locks-and-forgetting.md
;; draws (not yet validated), and nothing else of that plan. Nothing under
;; src/rig/store/ is changed; the envelope's name maker and canonical text
;; are used as they are.
(ns rig.bench.lock-slice
  "The thinnest slice that writes real lock rows the way the phase 2 plan
  says, for measuring the lock store's growth under a hand layer.

  Per value, in the stream topology on the layer's home task: a fresh
  32-byte lock K, the value's canonical EDN sealed under K with AES-256-GCM
  (a fresh 12-byte nonce prepended, the 16-byte tag appended), and K
  wrapped under the person locks of the value's wrap (the model's `wrap`
  under the baseline reading `:owner-required`: in a one-owner layer the
  owner's lock always, the other subjects only when the value is marked
  `:die-with-any`). The sealed value goes into the value's log row, the
  wrapped lock into a lock row under the layer's `:locks`, keyed by lock id
  `[:value fid]`, as the plan's schema has them (plan: 'The shapes',
  'PState Design', L3, L4, L5).

  What the slice leaves out, because the measurement does not need it: the
  answer record, `:by-stamp`, `:heads`, `:stood-on`, settings, the stamp,
  `$$persons` (person locks are fixed bench keys here), refusals other than
  a person with no lock, forgets and reads.

  Two PStates of one schema: `$$layers` keeps ciphertext and wrapped locks
  as base64 text in String slots (the plan's L4); `$$layers-raw` keeps the
  same bytes raw, in `byte/1` slots. A depot record picks one, and whether
  the lock row is written at all (the no-lock variant writes the same value
  rows and no lock row, so the lock store's own cost is a difference).

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [rig.store.envelope :as env])
  (:import [java.nio.charset StandardCharsets]
           [java.security MessageDigest SecureRandom]
           [java.util Base64]
           [javax.crypto Cipher]
           [javax.crypto.spec GCMParameterSpec SecretKeySpec]))

;; ---------------------------------------------------------------- crypto

(def ^:private ^SecureRandom rng (SecureRandom.))

(defn fresh-bytes
  "n random bytes from SecureRandom."
  ^bytes [n]
  (let [b (byte-array n)] (.nextBytes rng b) b))

(defn seal
  "AES-256-GCM of `plain` under `lock` with the given 12-byte nonce, as
  nonce ++ ciphertext ++ tag: 28 bytes more than the plaintext (plan L3)."
  ^bytes [^bytes lock ^bytes nonce ^bytes plain]
  (let [c (doto (Cipher/getInstance "AES/GCM/NoPadding")
            (.init Cipher/ENCRYPT_MODE (SecretKeySpec. lock "AES") (GCMParameterSpec. 128 nonce)))
        ct (.doFinal c plain)
        out (byte-array (+ 12 (alength ct)))]
    (System/arraycopy nonce 0 out 0 12)
    (System/arraycopy ct 0 out 12 (alength ct))
    out))

(defn unseal
  "The plaintext of a sealed blob under `lock`, or nil when it does not open
  (wrong lock, tampered or truncated blob). Never throws."
  [^bytes lock ^bytes sealed]
  (try
    (let [c (doto (Cipher/getInstance "AES/GCM/NoPadding")
              (.init Cipher/DECRYPT_MODE (SecretKeySpec. lock "AES")
                     (GCMParameterSpec. 128 sealed 0 12)))]
      (.doFinal c sealed 12 (- (alength sealed) 12)))
    (catch Throwable _ nil)))

(defn- sha256 ^bytes [^String s]
  (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s StandardCharsets/UTF_8)))

(def person-locks
  "Fixed bench person locks (32 bytes each), standing in for `$$persons`."
  {:alice (sha256 "rig-2026-09-25 bench person lock alice")
   :bob   (sha256 "rig-2026-09-25 bench person lock bob")
   :carol (sha256 "rig-2026-09-25 bench person lock carol")})

(defn encode
  "The slot form of sealed bytes: base64 text (the plan's L4) or the raw bytes."
  [encoding ^bytes b]
  (if (= :raw encoding) b (.encodeToString (Base64/getEncoder) b)))

(defn decode
  "Sealed bytes back from their slot form."
  ^bytes [encoding x]
  (if (= :raw encoding) x (.decode (Base64/getDecoder) ^String x)))

;; ------------------------------------------------------------- the wrap

(def grammars
  "The one grammar the model has (plan: 'Grammar'): a :mention's :persons
  names people."
  {:mention {:subjects-at [:persons]}})

(defn subjects-of
  "The persons a value names by its key's grammar."
  [k v]
  (if-let [at (get-in grammars [k :subjects-at])]
    (let [ps (when (map? v) (get-in v at))]
      (if (and (coll? ps) (every? keyword? ps)) (set ps) #{}))
    #{}))

(defn wrap
  "model.clj `wrap` under `:owner-required`, as sorted vectors (plan: 'The
  shapes'): marked `:die-with-any` -> every own subject required; else, with
  an owner, the owner alone required; else (shared layers) any one of the
  subjects."
  [owner subjects marked?]
  (let [subjects (cond-> (set subjects) owner (conj owner))]
    (cond
      marked? {:required (vec (sort subjects)) :any-of []}
      owner   {:required [owner] :any-of []}
      :else   {:required [] :any-of (vec (sort subjects))})))

(defn wrap-lock
  "The lock record for value lock K (plan: 'Lock record', L3): K sealed
  under each required person's lock in sorted order (the chain) into
  `:blob`; when `:any-of` is not empty, the chain's output sealed under each
  any-of person's lock into `:any-blobs`, and `:blob` nil. The four fields
  are written as the plan draws them, nil fields included."
  [encoding ^bytes K {:keys [required any-of]} nonces]
  (let [chain (reduce (fn [b [p n]] (seal (person-locks p) n b)) K
                      (map vector required nonces))]
    (if (seq any-of)
      {:required required :any-of any-of :blob nil
       :any-blobs (into {} (map (fn [p n] [p (encode encoding (seal (person-locks p) n chain))])
                                any-of (drop (count required) nonces)))}
      {:required required :any-of any-of :blob (encode encoding chain) :any-blobs nil})))

(defn unwrap-lock
  "K from a lock record given the live person locks, or nil. For checks."
  [encoding {:keys [required any-of blob any-blobs]} locks]
  (let [peel (fn [b] (reduce (fn [b p] (when b (some-> (locks p) (unseal b))))
                             b (reverse required)))]
    (if (seq any-of)
      (some (fn [p] (some->> (get any-blobs p) (decode encoding) (unseal (locks p)) peel)) any-of)
      (peel (decode encoding blob)))))

;; ------------------------------------------------------------ the write

(defn seal-value
  "Everything the topology writes for one value record, computed before any
  write: the log row (the plan's stage 1 row plus `:v` sealed, `:lock-id`,
  `:lock` nil for a row lock) and the lock record. Fresh randomness is drawn
  here, in one place, before the pure seal and wrap. Total: a wrap naming a
  person with no lock gives {:refuse :no-such-person}, never a throw."
  [{:keys [layer name e k v mark owner encoding]}]
  (try
    (let [w (wrap owner (subjects-of k v) (contains? mark :die-with-any))
          persons (concat (:required w) (:any-of w))]
      (if-not (every? person-locks persons)
        {:refuse :no-such-person}
        (let [K (fresh-bytes 32)
              nonces (repeatedly (count persons) #(fresh-bytes 12))
              plain (.getBytes ^String (env/encode-value v) StandardCharsets/UTF_8)
              lock-id [:value [name 0]]]
          {:layer layer
           :name name
           :lock-id lock-id
           :row {:e e :k k :v (encode encoding (seal K (fresh-bytes 12) plain))
                 :replaces nil :mark mark :lock-id lock-id :lock nil}
           :lock (wrap-lock encoding K w nonces)})))
    (catch Throwable _ {:refuse :slice-error})))

;; ----------------------------------------------------------- the module

(defn layers-schema
  "The plan's `$$layers` (PState Design, '+2' fields), cut to what the
  slice writes: the log rows and the lock rows. `blob` is the slot class of
  ciphertext and wrapped locks: String (L4) or byte/1."
  [blob]
  {clojure.lang.Keyword
   (fixed-keys-schema
    {:log   (map-schema clojure.lang.PersistentVector              ; name -> the act's rows
                        (vector-schema
                         (fixed-keys-schema {:e        clojure.lang.Keyword
                                             :k        clojure.lang.Keyword
                                             :v        blob         ; the value sealed under its lock
                                             :replaces clojure.lang.PersistentVector
                                             :mark     (set-schema clojure.lang.Keyword)
                                             :lock-id  clojure.lang.PersistentVector
                                             :lock     (fixed-keys-schema
                                                        {:required  clojure.lang.PersistentVector
                                                         :any-of    clojure.lang.PersistentVector
                                                         :blob      blob
                                                         :any-blobs (map-schema clojure.lang.Keyword blob)})})
                         {:subindex? true})
                        {:subindex-options {:track-size? false}})
     :locks (map-schema clojure.lang.PersistentVector              ; lock id -> the lock record
                        (fixed-keys-schema {:required  clojure.lang.PersistentVector
                                            :any-of    clojure.lang.PersistentVector
                                            :blob      blob
                                            :any-blobs (map-schema clojure.lang.Keyword blob)})
                        {:subindex-options {:track-size? false}})})})

;; One event per value record, on the layer's home task (the depot is placed
;; by layer, as the store's is), with no partitioner: the row and the lock
;; row commit in one group. Every write is a set keyed by name or lock id.
(defmodule LockSlice {:module-name "rig.bench.LockSlice"}
  [setup topologies]
  (declare-depot setup *values (hash-by :layer))
  (let [s (stream-topology topologies "slice")]
    (declare-pstate s $$layers (layers-schema String))
    (declare-pstate s $$layers-raw (layers-schema byte/1))
    (<<sources s
      (source> *values :> *rec)
      (seal-value *rec :> *out)
      (<<if (contains? *out :refuse)
        (ack-return> (get *out :refuse))
       (else>)
        (get *out :layer :> *layer)
        (get *out :name :> *name)
        (get *out :lock-id :> *lock-id)
        (get *out :row :> *row)
        (get *out :lock :> *lock)
        (get *rec :row-lock :> *row-lock)
        (<<if (= :raw (get *rec :encoding))
          (local-transform> [(keypath *layer :log *name) (termval [*row])] $$layers-raw)
          (<<if *row-lock
            (local-transform> [(keypath *layer :locks *lock-id) (termval *lock)] $$layers-raw))
         (else>)
          (local-transform> [(keypath *layer :log *name) (termval [*row])] $$layers)
          (<<if *row-lock
            (local-transform> [(keypath *layer :locks *lock-id) (termval *lock)] $$layers)))
        (ack-return> :written)))))

(defn value-record
  "A depot record for one value: one act of one value fact in `layer`
  (owned by `owner`), under a fresh name made as the offerer makes one."
  [{:keys [layer owner e k v mark encoding row-lock]}]
  {:layer layer
   :name (env/make-name layer :by-layer)
   :e e :k k :v v :mark (or mark #{})
   :owner owner
   :encoding encoding
   :row-lock (boolean row-lock)})
