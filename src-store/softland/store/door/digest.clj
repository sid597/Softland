(ns softland.store.door.digest
  "The digest of a thing: SHA-256 over its one text, with the time on the
  act left out. Pure code. It needs no cluster.

  One function that the door and both gates share. A gate keeps the digest
  of each thing that it decides, in the answer under the thing's act-id, and
  the read by act-id gives it back. The door gives its maker the digest of
  what it sent, as hex text, and compares that text with the answer's: it
  keeps no thing. So a maker tells its own act's answer from the answer of
  another act under the same act-id. A gate also keeps the digest of each
  stray.

  Inputs and results, by call:

    digest         anything -> 32 bytes
    hex-of         anything -> the digest as hex text, 64 characters
    is-digest-of?  a digest as hex text, and anything -> true or false
    readable-hex-of  anything -> the digest as hex text, or nil for a thing
                     that throws while it is read

  What the digest is taken of. The thing as it arrived, each value plain,
  without the time on the act: when the thing is a plain map, a map that is
  no record, its top-level `:door-time` is taken out first
  (`softland.store.door.declaration/time-part` holds the name). Anything
  else is taken as it is, and a `:door-time` deeper in a thing stays. Why
  the time is left out: a maker's second send of one act under one act-id
  carries another time, and both sends must read the one answer as their
  own; a gate keeps the digest of the first thing to arrive. So two things
  that differ only in that part have one digest, and so have a thing with
  the part and the same thing without it.

  The one text of the thing (`softland.store.door.text`) is fed to the hash
  piece by piece as the thing is walked, so no whole text of a large thing
  is built. One difference from the text as a string, for speed: a
  run of bytes is fed as `#bytes`, its count, and the bytes themselves, not
  as hex. For a thing that holds no bytes and no top-level `:door-time` the
  digest is the SHA-256 of the UTF-8 of `text`.

  Why the whole 32 bytes are kept. With two things of one digest, a
  different act would read the first act's answer as its own. The full
  width of a standard hash needs no argument about what short enough means.

  State and effects. None. Each function is pure, and none throws."
  (:require [softland.store.door.declaration :as declaration]
            [softland.store.door.text :as text])
  (:import [java.nio.charset StandardCharsets]
           [java.security MessageDigest]))

(defn- sha-256
  "A new SHA-256 hash, empty. Every JVM has the algorithm."
  ^MessageDigest []
  (MessageDigest/getInstance "SHA-256"))

(defn- without-the-time
  "Takes anything. Gives what a digest is taken of: a plain map, a map that
  is no record, without its top-level `:door-time`
  (`softland.store.door.declaration/time-part`); anything else as it is. A
  plain map that does not hold the part comes back equal to itself.

  It never throws. A map that throws when a part is taken out of it comes
  back as it is: a sorted map whose keys cannot be compared with a keyword
  is one, and such a map cannot hold the part. So each such thing keeps a
  digest of its own.

  Not decided: kept for now. That a digest leaves the time out is one of
  the three places of the working answer on a second send
  (`softland.store.door/send-act` names the three); what a second send of
  the same act carries as its time is Sid's to say. What follows from it
  beside the second send: a thing with no time and the same thing with a
  time have one digest. So a thing that was appended without the door and
  refused `:no-door-time` would be read as her own answer by a maker who
  later sends the same act through the door under the same act-id. That
  is derived from this function and the walk's check of the answer under
  the act-id; no run made it."
  [x]
  (if (and (map? x) (not (record? x)))
    (try (dissoc x declaration/time-part)
         (catch Throwable _ x))
    x))

(defn digest
  "Takes anything. Gives its digest: 32 bytes, the same in every process for
  the same thing, taken of the thing without the time on the act
  (`without-the-time`). It never throws: a thing that throws while it is
  read has the digest of the text #unreadable, as
  `softland.store.door.text/text` gives that thing. Cost: one walk of the
  thing."
  [x]
  (let [md (sha-256)]
    (try
      (text/feed (without-the-time x)
                 (fn [^String s] (.update md (.getBytes s StandardCharsets/UTF_8)))
                 (fn [b] (.update md ^bytes b)))
      (.digest md)
      (catch Throwable _
        (.digest (sha-256) (.getBytes "#unreadable" StandardCharsets/UTF_8))))))

(defn hex-of
  "Takes anything. Gives its digest as hex text: 64 lower-case characters,
  the hex of what `digest` gives, so the time on the act is left out here
  too. This is the form in which a caller gets a digest, and in which a
  gate keeps the digest of a stray."
  [x]
  (text/hex (digest x)))

(defn is-digest-of?
  "The comparison of a sender that still holds its thing, as the hand of the
  module's proof does. Takes a digest as hex text, as the read by act-id
  gives it, and the thing that was sent, with the time on the act or
  without it. Gives true when the digest is the digest of that thing: the
  answer is then the answer of the sender's own act. False for a digest
  that is nil or of another thing. The door does not call it: it holds no
  thing after a send, and compares two hex texts
  (`softland.store.door.words/answer`)."
  [hex thing]
  (= hex (hex-of thing)))

(defn readable-hex-of
  "The digest that tells a throw. Takes anything. Gives its digest as hex
  text, the 64 characters that `hex-of` gives; or nil for a thing that
  throws while it is read, where `hex-of` gives the digest of
  #unreadable. The door takes the digest of
  the thing that it is about to append with this, so that it appends no
  thing that it could not read to its end. It never throws. Cost: one walk
  of the thing, the walk of `digest`. Limit: a thing with no end does not
  come back from it."
  [x]
  (let [md (sha-256)]
    (try
      (text/feed (without-the-time x)
                 (fn [^String s] (.update md (.getBytes s StandardCharsets/UTF_8)))
                 (fn [b] (.update md ^bytes b)))
      (text/hex (.digest md))
      (catch Throwable _ nil))))
