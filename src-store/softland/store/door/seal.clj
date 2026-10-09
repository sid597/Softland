(ns softland.store.door.seal
  "Locks and the seal: making a lock's bytes, sealing a value to its place,
  and opening it. Pure code beside the JVM's random source and its cipher.
  It needs no cluster.

  \"Lock\" is an encryption key. \"Key\" is a fact's key. Never one word for
  both.

  Each value gets its own lock. Sid decided it on 9 October 2026
  (src-store/README.md, H1 A). The door seals a value with a lock that the
  store made; a gate opens each sealed value once, with its lock, to check
  that the lock opens it, and keeps nothing of the plain value. A lock that
  is later destroyed is that value gone, although the sealed bytes stay in
  the depot for ever.

  A lock, in this namespace, is what its row in the record gives:
  {:bytes the lock's bytes, :way the number of its way of sealing}.

  The way of sealing is said by the lock, not by a mark in each value. A
  sealed value carries no version byte. `ways` declares each way that the
  code knows, a number beside its word; the number is what a lock's row
  holds, and the word lives here only. A lock whose way the code does not
  know seals nothing and opens nothing. So the way can change later without
  a stored value being touched: a new way is new locks with a new number.

  This build knows one way, 1, whose word is :aes-256-gcm:

    the lock      32 random bytes, the length that AES-256 takes, from the
                  JVM's SecureRandom
    sealing       AES-GCM under the lock, with a fresh random part of 12
                  bytes for each seal and a tag of 16
    sealed bytes  the random part, then the cipher's output with its tag:
                  28 bytes more than the plain bytes
    the place     bound in as the cipher's associated data: the UTF-8 of the
                  one text of [layer entity key]. Sealed bytes that are
                  copied to another entity, key or layer do not open

  Inputs and results, by call:

    new-lock    nothing -> a lock with fresh random bytes, of `way-of-new-locks`
    known-way?  the number of a way -> true or false
    seal        a lock, a place, plain bytes -> sealed bytes, or nil
    open        a lock, a place, sealed bytes -> the plain bytes, or nil
    opens?      a lock, a place, sealed bytes -> true or false

  State and effects. One random source for each process, made at its first
  use; it holds nothing of the record. `new-lock` and `seal` take random
  bytes from it. Nothing here writes a lock anywhere: a gate writes the
  lock's row, and a lock's bytes are never appended to a depot.

  Not here. Which plain bytes stand for a value: the door's. The proof's
  hand seals the UTF-8 of the one text of the value. A gate does not read
  what a lock opens. Wrapping a lock under the locks of the people a value
  is about, and the grains of one lock for an act or for a session and
  layer, are not built."
  (:require [softland.store.door.text :as text])
  (:import [java.security SecureRandom]
           [javax.crypto Cipher]
           [javax.crypto.spec GCMParameterSpec SecretKeySpec]))

(def ways
  "Each way of sealing that the code knows: its number, which a lock's row
  holds, beside its word, which lives here only."
  {1 :aes-256-gcm})

(def way-of-new-locks
  "The number of the way that each new lock is made for."
  1)

(def lock-length
  "How many bytes a lock of way 1 has: the length that AES-256 takes."
  32)

(def ^:private random-part-length
  "How many bytes the random part of a seal has, in way 1."
  12)

(def ^:private tag-bits
  "How many bits the tag of a seal has, in way 1."
  128)

(def ^:private random-source
  "The random source of this process, made at its first use. It cannot be
  guessed, and it holds nothing of the record."
  (delay (SecureRandom.)))

(defn- random-bytes
  "Takes a count. Gives that many fresh random bytes."
  [n]
  (let [b (byte-array n)]
    (.nextBytes ^SecureRandom @random-source b)
    b))

(defn known-way?
  "Takes the number of a way of sealing, as a lock's row holds it. Gives true
  when the code knows the way."
  [way]
  (contains? ways way))

(defn new-lock
  "Gives a new lock: {:bytes 32 fresh random bytes, :way the number of the
  way that new locks are made for}. Each call gives other bytes, so a caller
  that must give the same lock twice keeps what it got."
  []
  {:bytes (random-bytes lock-length) :way way-of-new-locks})

(defn- cipher
  "Takes a cipher mode, a lock's bytes, a random part and a place. Gives the
  AES-GCM cipher of way 1, ready, with the place as its associated data. It
  can throw, for bytes that are no lock."
  ^Cipher [mode lock-bytes random-part place]
  (doto (Cipher/getInstance "AES/GCM/NoPadding")
    (.init (int mode)
           (SecretKeySpec. ^bytes lock-bytes "AES")
           (GCMParameterSpec. (int tag-bits) ^bytes random-part))
    (.updateAAD ^bytes (text/utf8 place))))

(defn seal
  "Seals a value to its place. Takes a lock; the place, as
  [layer entity key]; and the plain bytes. Gives the sealed bytes: a fresh
  random part, then the cipher's output with its tag. Gives nil when the
  lock's way is not known, or its bytes are no lock of that way. It never
  throws. Each call takes a fresh random part, so sealing the same value
  twice gives two different runs of bytes."
  [lock place plain]
  (when (known-way? (:way lock))
    (try
      (let [random-part (random-bytes random-part-length)
            sealed-part (.doFinal (cipher Cipher/ENCRYPT_MODE (:bytes lock) random-part place)
                                  ^bytes plain)
            out (byte-array (+ random-part-length (alength sealed-part)))]
        (System/arraycopy random-part 0 out 0 random-part-length)
        (System/arraycopy sealed-part 0 out random-part-length (alength sealed-part))
        out)
      (catch Throwable _ nil))))

(defn open
  "Opens a sealed value. Takes a lock; the place, as [layer entity key]; and
  the sealed bytes. Gives the plain bytes, or nil when the lock's way is not
  known, the tag does not hold (other bytes, another lock, another place),
  or the bytes are too few to be a seal. It never throws."
  [lock place sealed]
  (when (known-way? (:way lock))
    (try
      (let [^bytes sealed sealed
            random-part (java.util.Arrays/copyOfRange sealed 0 (int random-part-length))]
        (.doFinal (cipher Cipher/DECRYPT_MODE (:bytes lock) random-part place)
                  sealed (int random-part-length) (int (- (alength sealed) random-part-length))))
      (catch Throwable _ nil))))

(defn opens?
  "The gate's test. Takes a lock, a place and sealed bytes. Gives true when
  the lock opens the bytes at that place. It keeps nothing of the plain
  bytes."
  [lock place sealed]
  (some? (open lock place sealed)))
