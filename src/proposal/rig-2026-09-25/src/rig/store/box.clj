;; IMPORTANT: Before modifying this file, re-read PLAN-promotion.md ("The
;; hard question, answered whole", PR6 with [F2]).
(ns rig.store.box
  "The sealed box (PLAN-promotion.md, PR6): a 32-byte lock sealed to an
  X25519 public key, so that only the holder of the matching private key
  opens it. A promotion's landing travels sealed under a fresh lock K, and
  K travels in a box to the landing lease's public key: the depot carries
  the box and the sealed copy, and nothing the depot holds opens either.

  The construction: an ephemeral X25519 key pair per box; the shared
  secret of the ephemeral private key and the lease's public key; the
  wrapping lock HMAC-SHA256, keyed by the shared secret, over the label
  `softland/landing-box/v1`, the ephemeral public key and the lease's
  public key; AES-256-GCM of K under the wrapping lock with a fresh 12-byte
  nonce, with associated data (the caller's: a landing's lock id and name,
  so a box opens only inside the landing it was made for, [F2]). A box is
  `{:eph <44 bytes> :nonce <12> :wrapped <48>}`: 104 bytes.

  Keys are the JDK's encodings: a public key is its X.509
  SubjectPublicKeyInfo (44 bytes), a private key its PKCS#8 (48 bytes).
  Probed on this machine's JDK 21 (runs/phase4-probe-sealed-box.txt,
  runs/phase4-validate-x25519.txt): 44 random bytes do not decode; the
  small-order points u = 0 and u = 1 decode and then fail the agreement.

  Pure and total but for the draws: `keypair` and `fresh-nonce` draw from
  SecureRandom and are called before a decision, never inside one; every
  other function returns nil on any failure and never throws, because an
  exception in topology code is fatal to the worker (SPEC 'What Rama
  showed' 3). Requires nothing of the store.

  Vocabulary: \"key\" is a fact's key; a \"lock\" is an encryption key.
  X25519's own terms, public and private key, name the key pair's halves."
  (:import [java.nio.charset StandardCharsets]
           [java.security Key KeyFactory KeyPair KeyPairGenerator PrivateKey PublicKey SecureRandom]
           [java.security.spec PKCS8EncodedKeySpec X509EncodedKeySpec]
           [java.util Arrays Base64]
           [javax.crypto Cipher KeyAgreement Mac]
           [javax.crypto.spec GCMParameterSpec SecretKeySpec]))

(def public-size "An X25519 public key as X.509 SubjectPublicKeyInfo." 44)
(def private-size "An X25519 private key as PKCS#8." 48)
(def lock-size "The lock a box carries." 32)
(def nonce-size "The box's AES-GCM nonce." 12)
(def wrapped-size "The lock sealed: 32 bytes and the 16-byte tag." 48)

(def ^:private label
  "The wrapping lock's label: names this construction, first-record with
  the box (PR6 can change without touching a record: a box is read only at
  a landing's decision)."
  (.getBytes "softland/landing-box/v1" StandardCharsets/UTF_8))

(def ^:private ^SecureRandom rng (SecureRandom.))

(defn- bytes-of? [x n] (and (bytes? x) (= n (alength ^bytes x))))

;; ------------------------------------------------------------------ draws

(defn keypair
  "A fresh X25519 key pair, `{:public <44 bytes> :private <48 bytes>}`.
  Impure: drawn before a decision."
  []
  (let [^KeyPair kp (.generateKeyPair (KeyPairGenerator/getInstance "X25519"))]
    {:public (.getEncoded (.getPublic kp))
     :private (.getEncoded (.getPrivate kp))}))

(defn fresh-nonce
  "A fresh 12-byte nonce. Impure: drawn before a decision."
  []
  (let [b (byte-array nonce-size)] (.nextBytes rng b) b))

;; ------------------------------------------------------------------ keys

(defn public-key
  "The X25519 public key the bytes encode, or nil: anything that is not
  exactly 44 bytes of an X25519 SubjectPublicKeyInfo. A small-order point
  decodes; `box` then answers nil. Total."
  ^PublicKey [bs]
  (try
    (when (bytes-of? bs public-size)
      (.generatePublic (KeyFactory/getInstance "X25519") (X509EncodedKeySpec. ^bytes bs)))
    (catch Throwable _ nil)))

(defn- private-key
  ^PrivateKey [bs]
  (try
    (when (bytes-of? bs private-size)
      (.generatePrivate (KeyFactory/getInstance "X25519") (PKCS8EncodedKeySpec. ^bytes bs)))
    (catch Throwable _ nil)))

(defn encode-public
  "A public key's bytes as base64 text, the form a control value carries
  (a control value is EDN, which has no bytes). Nil for anything else."
  [bs]
  (try (when (bytes-of? bs public-size) (.encodeToString (Base64/getEncoder) ^bytes bs))
       (catch Throwable _ nil)))

(defn decode-public
  "The bytes base64 text encodes, when they are a public key's 44, else
  nil. Total."
  [s]
  (try
    (when (string? s)
      (let [bs (.decode (Base64/getDecoder) ^String s)]
        (when (bytes-of? bs public-size) bs)))
    (catch Throwable _ nil)))

;; ------------------------------------------------------------- box / unbox

(defn- shared-secret
  "The X25519 agreement of a private and a public key: 32 bytes, or nil
  when the agreement refuses (a small-order point) or gives all zeros."
  [^PrivateKey priv ^PublicKey pub]
  (try
    (let [ka (KeyAgreement/getInstance "XDH")]
      (.init ka priv)
      (.doPhase ka pub true)
      (let [s (.generateSecret ka)]
        (when (and (bytes-of? s 32) (not (Arrays/equals ^bytes s (byte-array 32)))) s)))
    (catch Throwable _ nil)))

(defn- wrapping-lock
  "HMAC-SHA256, keyed by the shared secret, over the label and both public
  keys: the AES key that seals the lock."
  [^bytes shared ^bytes eph-public ^bytes lease-public]
  (let [m (Mac/getInstance "HmacSHA256")]
    (.init m ^Key (SecretKeySpec. shared "HmacSHA256"))
    (.update m ^bytes label)
    (.update m eph-public)
    (.update m lease-public)
    (.doFinal m)))

(defn- gcm
  ^Cipher [mode ^bytes k ^bytes nonce ^bytes aad]
  (let [c (Cipher/getInstance "AES/GCM/NoPadding")]
    (.init c (int mode) ^Key (SecretKeySpec. k "AES") (GCMParameterSpec. 128 nonce))
    (.updateAAD c aad)
    c))

(defn box
  "Lock `K` sealed to `lease-public` (44 bytes), with the ephemeral key
  pair `eph` (`keypair`'s map) and `nonce` drawn before the decision, and
  `aad` the associated data it is bound to (bytes). `{:eph :nonce
  :wrapped}`, or nil on any failure: a public key that does not decode, a
  small-order point, a lock that is not 32 bytes. Pure and total."
  [lease-public K eph nonce aad]
  (try
    (let [pub (public-key lease-public)
          epriv (private-key (:private eph))
          epub (:public eph)]
      (when (and pub epriv (bytes-of? epub public-size) (bytes-of? K lock-size)
                 (bytes-of? nonce nonce-size) (bytes? aad))
        (when-let [s (shared-secret epriv pub)]
          (let [w (wrapping-lock s epub lease-public)
                wrapped (.doFinal (gcm Cipher/ENCRYPT_MODE w nonce aad) ^bytes K)]
            {:eph epub :nonce nonce :wrapped wrapped}))))
    (catch Throwable _ nil)))

(defn unbox
  "The lock a box holds, opened with the lease's private key (48 bytes)
  and public key (44 bytes) and the associated data it was made for, or
  nil: another key pair, another landing's data, a changed byte, anything
  malformed (AES-GCM fails closed). Pure and total."
  [lease-private lease-public b aad]
  (try
    (when (map? b)
      (let [{:keys [eph nonce wrapped]} b
            priv (private-key lease-private)
            epub (public-key eph)]
        (when (and priv epub (bytes-of? lease-public public-size) (bytes-of? nonce nonce-size)
                   (bytes-of? wrapped wrapped-size) (bytes? aad))
          (when-let [s (shared-secret priv epub)]
            (let [w (wrapping-lock s eph lease-public)
                  K (.doFinal (gcm Cipher/DECRYPT_MODE w nonce aad) ^bytes wrapped)]
              (when (bytes-of? K lock-size) K))))))
    (catch Throwable _ nil)))

(defn box?
  "A well-formed box as a landing carries it: exactly `:eph` (44 bytes),
  `:nonce` (12) and `:wrapped` (48). Its opening is the landing's
  decision's; this is the face check."
  [b]
  (boolean (and (map? b) (not (record? b)) (= #{:eph :nonce :wrapped} (set (keys b)))
                (bytes-of? (:eph b) public-size) (bytes-of? (:nonce b) nonce-size)
                (bytes-of? (:wrapped b) wrapped-size))))
