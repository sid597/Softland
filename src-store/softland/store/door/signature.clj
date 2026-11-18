(ns softland.store.door.signature
  "The signature of a signed act: the bytes that are signed, the making of a
  signature, and the test that a signature fits its who. Pure code. It
  needs no cluster, and of the JVM it uses the hash SHA-256 and the
  signature scheme Ed25519, which every JVM from 15 on has.

  Why it is here. A signed act names a signing who, an id that holds a
  public half (`softland.store.act/public-half-hex`), and carries a
  signature. Whoever holds the private half of that pair, and nobody else,
  can make a signature that fits. So a gate can hold that the sender of an
  act is the who that the act names, from the act alone: it reads no row
  for it. The door's declaration asks `fits?` for each signed act that
  arrives (`softland.store.door.declaration/check`). Whoever sends a signed
  act makes its signature with `sign`.

  The store sees a public half and a signature, and never a private half.
  Where a private half is kept, and what it takes to use it, is outside the
  store: this namespace knows a keeper only as a function that is handed
  bytes and gives a signature back.

  Inputs and results, by call:

    signed-bytes  anything -> the bytes that a signature is made of, or nil
                  for a thing that throws while it is read
    sign          a keeper, and anything -> the text of a signature, or nil
    fits?         a who, the text of a signature, and anything -> true or
                  false
    who-of        a public half as the JVM holds one -> its signing who, or
                  nil

  What is signed. Not the thing itself: a fixed line that says what the
  signature is for, `purpose`, and the SHA-256 of the one text of the thing
  (`softland.store.door.text`), as hex. The hash keeps the signed bytes
  short whatever the size of the thing, so a keeper that signs in hardware
  is handed about a hundred bytes. The line keeps a signature of an act
  from being read as a signature of anything else that the same pair may
  one day sign, a read among them.

  Which thing. The caller chooses, and both sides must choose alike: the
  declaration's check and a sender both take the act as it arrives on its
  depot, with its act-id and its depot in it, and without its signature and
  the time on it (`softland.store.door.declaration/what-is-signed`). The
  act-id is inside the signed bytes, so a signature is good under one
  act-id in one layer, and a gate answers an act-id once.

  The text of a signature is the hex of its 64 bytes, lower case, 128
  characters: one text for one signature.

  State and effects. None of its own. `sign` calls the keeper that it is
  handed, and what that does is the keeper's. No function throws.

  Limits. It knows one kind of proof, ed25519. It does not test that a
  public half is a strong point of the curve: a who whose public half is
  one of the curve's few weak points can have signatures that fit and that
  no private half made, so such a who is one that more than one sender can
  be. Nobody gets such a who without choosing it: a pair that the JVM
  makes is never one. How long one test takes is not measured: it is one
  walk of the thing for the hash, and one verification."
  (:require [softland.store.act :as act]
            [softland.store.door.text :as text])
  (:import [java.nio.charset StandardCharsets]
           [java.security KeyFactory MessageDigest PublicKey Signature]
           [java.security.spec X509EncodedKeySpec]))

(def purpose
  "The line that the signed bytes begin with: it says that the signature is
  of a signed act of the store, of the form's version 2. A signature that
  was made of other bytes fits no act. The words are a session's."
  "softland.store signed act 2")

(defn signed-bytes
  "Takes anything: for an act, what
  `softland.store.door.declaration/what-is-signed` gives. Gives the bytes
  that a signature is made of: the UTF-8 of `purpose`, a newline, and the
  SHA-256 of the thing's one text as 64 hex characters. The same thing
  gives the same bytes in every process. Gives nil for a thing that throws
  while it is read. Cost: one walk of the thing."
  [x]
  (try
    (let [md (MessageDigest/getInstance "SHA-256")]
      (text/feed x
                 (fn [^String s] (.update md (.getBytes s StandardCharsets/UTF_8)))
                 (fn [b] (.update md ^bytes b)))
      (.getBytes (str purpose "\n" (text/hex (.digest md))) StandardCharsets/UTF_8))
    (catch Throwable _ nil)))

(defn- bytes-of-hex
  "Takes a string. Gives the bytes that it writes as lower-case hex, two
  characters for each byte, or nil when it is not such a text of exactly
  `n` bytes."
  [^String s n]
  (when (and (= (* 2 n) (.length s)) (re-matches #"[0-9a-f]*" s))
    (let [out (byte-array n)]
      (dotimes [i n]
        (aset-byte out i (unchecked-byte (Integer/parseInt (.substring s (* 2 i) (+ 2 (* 2 i))) 16))))
      out)))

(def ^:private x509-head
  "The twelve bytes that stand before the 32 bytes of an Ed25519 public
  half in the form in which the JVM reads and writes one (X.509,
  SubjectPublicKeyInfo, RFC 8410)."
  (byte-array (map unchecked-byte [0x30 0x2a 0x30 0x05 0x06 0x03 0x2b 0x65 0x70 0x03 0x21 0x00])))

(defn- public-half
  "Takes anything. Gives the public half that a signing who holds, as the
  JVM holds one, or nil for anything that is no signing who and for bytes
  that the JVM does not take as a public half. It can throw; `fits?`
  guards it."
  ^PublicKey [who]
  (when-let [hex (act/public-half-hex who)]
    (let [half (bytes-of-hex hex 32)
          encoded (byte-array 44)]
      (System/arraycopy x509-head 0 encoded 0 12)
      (System/arraycopy half 0 encoded 12 32)
      (.generatePublic (KeyFactory/getInstance "Ed25519") (X509EncodedKeySpec. encoded)))))

(defn who-of
  "Takes a public half as the JVM holds one. Gives its signing who, the id
  `:who/ed25519.<64 hex>` (`softland.store.act/public-half-hex` reads the
  hex back out), or nil for anything that is no Ed25519 public half. It
  never throws."
  [public]
  (try
    (let [^PublicKey k public
          encoded (.getEncoded k)]
      (when (and (= 44 (alength encoded))
                 (= (vec x509-head) (vec (java.util.Arrays/copyOfRange encoded 0 12))))
        (keyword act/signing-who-namespace
                 (str "ed25519." (text/hex (java.util.Arrays/copyOfRange encoded 12 44))))))
    (catch Throwable _ nil)))

(defn sign
  "Makes the signature of a thing. Takes a keeper, a function that is
  handed the bytes to sign and gives the 64 bytes of an Ed25519 signature
  made with the private half that it keeps; and anything, the thing whose
  `signed-bytes` are signed. Gives the text of the signature, 128 hex
  characters, or nil: for a thing that throws while it is read, for a
  keeper that throws, and for a keeper that gives anything but 64 bytes.
  It never throws. The keeper alone touches a private half; this function
  sees none."
  [keeper x]
  (try
    (when-let [to-sign (signed-bytes x)]
      (let [made (keeper to-sign)]
        (when (and (bytes? made) (= 64 (alength ^bytes made)))
          (text/hex made))))
    (catch Throwable _ nil)))

(defn fits?
  "The test of a signature. Takes a who, the text of a signature, and
  anything, the thing that was signed. Gives true when the who is a
  signing who and the text is a signature of the thing's `signed-bytes`
  that fits the public half which that who holds. False for anything
  else: a who that is no signing who, a text that is not 128 lower-case
  hex characters, a signature of other bytes or of another pair, a thing
  that throws while it is read. It never throws, so a gate can call it on
  whatever arrives. It reads no record and no clock: the same three give
  the same answer whenever they are asked. Cost: one walk of the thing and
  one verification."
  [who signature x]
  (try
    (boolean
     (when-let [public (public-half who)]
       (when-let [made (and (string? signature) (bytes-of-hex signature 64))]
         (when-let [to-sign (signed-bytes x)]
           (.verify (doto (Signature/getInstance "Ed25519")
                      (.initVerify public)
                      (.update ^bytes to-sign))
                    ^bytes made)))))
    (catch Throwable _ false)))
