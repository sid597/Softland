(ns softland.store.door.digest
  "The digest of a thing: SHA-256 over its one text. Pure code. It needs no
  cluster.

  One function that the door and both gates share. A gate keeps the digest
  of each thing that it decides, in the answer under the thing's act-id, and
  the read by act-id gives it back. The sender compares it with the digest
  of what it sent, and so tells its own act's answer from the answer of
  another act under the same act-id. A gate also keeps the digest of each
  stray.

  Inputs and results, by call:

    digest         anything -> 32 bytes
    hex-of         anything -> the digest as hex text, 64 characters
    is-digest-of?  a digest as hex text, and anything -> true or false

  What the digest is taken of. The thing as it arrived, with its sealed
  bytes. The one text of the thing (`softland.store.door.text`) is fed to the
  hash piece by piece as the thing is walked, so no whole text of a large
  thing is built. One difference from the text as a string, for speed: a
  run of bytes is fed as `#bytes`, its count, and the bytes themselves, not
  as hex. For a thing that holds no bytes the digest is the SHA-256 of the
  UTF-8 of `text`.

  Why the whole 32 bytes are kept. With two things of one digest, a
  different act would read the first act's answer as its own. The full
  width of a standard hash needs no argument about what short enough means.

  State and effects. None. Each function is pure, and none throws."
  (:require [softland.store.door.text :as text])
  (:import [java.nio.charset StandardCharsets]
           [java.security MessageDigest]))

(defn- sha-256
  "A new SHA-256 hash, empty. Every JVM has the algorithm."
  ^MessageDigest []
  (MessageDigest/getInstance "SHA-256"))

(defn digest
  "Takes anything. Gives its digest: 32 bytes, the same in every process for
  the same thing. It never throws: a thing that throws while it is read has
  the digest of the text #unreadable, as `softland.store.door.text/text` gives
  that thing. Cost: one walk of the thing."
  [x]
  (let [md (sha-256)]
    (try
      (text/feed x
                 (fn [^String s] (.update md (.getBytes s StandardCharsets/UTF_8)))
                 (fn [b] (.update md ^bytes b)))
      (.digest md)
      (catch Throwable _
        (.digest (sha-256) (.getBytes "#unreadable" StandardCharsets/UTF_8))))))

(defn hex-of
  "Takes anything. Gives its digest as hex text: 64 lower-case characters.
  This is the form in which a caller gets a digest, and in which a gate
  keeps the digest of a stray."
  [x]
  (text/hex (digest x)))

(defn is-digest-of?
  "The door's comparison. Takes a digest as hex text, as the read by act-id
  gives it, and the thing that the door sent. Gives true when the digest is
  the digest of that thing: the answer is then the answer of the door's own
  act. False for a digest that is nil or of another thing."
  [hex thing]
  (= hex (hex-of thing)))
