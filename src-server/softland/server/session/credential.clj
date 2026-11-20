(ns softland.server.session.credential
  "A credential: the token that a system holds, the hash under which the
  server knows it, and the short code that a person compares. It requires
  nothing outside the JVM.

  A token is `sl1_` and 43 characters of base64url: 32 random bytes. The
  server keeps no token. It keeps the SHA-256 of the token's text, as 64 hex
  characters (`softland.server.session.record`), and finds a session's row
  by that hash. A hash gives nobody the token, so it can be shown: the link
  that a person opens to say yes to a command-line program carries the
  hash, and the program holds the token.

  Either side can make a token. The server makes one for a browser at
  sign-in. A program that asks to be signed in makes its own, so that the
  token crosses no wire before her yes, and nothing is written for a sign-in
  that is only under way.

  The short code is the first eight characters of the hash, as `xxxx-xxxx`.
  The program shows it beside the link, and the page on which she says yes
  shows it again: she says yes only when the two are the same. It is a
  check by eye against a link that somebody else sent her, and no secret.

  Inputs and results, by call:

    make         nothing -> a new token
    token?       anything -> whether it has the form of a token
    hash-of      a token -> its hash, or nil for a thing of another form
    code-of      a hash -> the short code
    of-header    the text of an Authorization header -> the token, or nil
    of-cookie    the text of a Cookie header -> the token, or nil

  State and effects. `make` reads the JVM's secure random numbers. Nothing
  else has an effect.

  Not decided, kept for now: the form of a token, its length, the hash, and
  the name of the cookie. Each is in one place here."
  (:require [clojure.string :as str])
  (:import [java.nio.charset StandardCharsets]
           [java.security MessageDigest SecureRandom]
           [java.util Base64]))

(def cookie-name
  "The name of the cookie in which a browser holds its token."
  "softland")

(def ^:private token-pattern
  "The form of a token: the prefix and 43 characters of base64url."
  #"sl1_[A-Za-z0-9_-]{43}")

(def ^:private ^SecureRandom random
  "The JVM's secure random numbers, for the bytes of a token."
  (SecureRandom.))

(defn make
  "Gives a new token: `sl1_` and 32 random bytes as base64url without
  padding."
  []
  (let [bytes (byte-array 32)]
    (.nextBytes random bytes)
    (str "sl1_" (.encodeToString (.withoutPadding (Base64/getUrlEncoder)) bytes))))

(defn token?
  "Takes anything. Gives true for a text that has the form of a token."
  [x]
  (boolean (and (string? x) (re-matches token-pattern x))))

(defn hash-of
  "Takes a token. Gives its hash, the SHA-256 of its text as 64 lower-case
  hex characters; nil for a thing that has not the form of a token, so that
  no row is asked for under the hash of an arbitrary text."
  [token]
  (when (token? token)
    (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                          (.getBytes ^String token StandardCharsets/UTF_8))]
      (apply str (map (fn [b] (format "%02x" (bit-and 0xff (int b)))) digest)))))

(defn code-of
  "Takes a hash. Gives the short code that a person compares: its first
  eight characters as `xxxx-xxxx`."
  [hash]
  (str (subs hash 0 4) "-" (subs hash 4 8)))

(defn of-header
  "Takes the text of an Authorization header, or nil. Gives the token of
  `Bearer <token>`, or nil when the header is absent or holds no token of
  the form."
  [header]
  (when (string? header)
    (let [[scheme token] (str/split (str/trim header) #"\s+" 2)]
      (when (and scheme (= "bearer" (str/lower-case scheme)) (token? token))
        token))))

(defn of-cookie
  "Takes the text of a Cookie header, or nil. Gives the token that the
  cookie `cookie-name` holds, or nil. A header that holds the cookie twice
  gives nil: which of two a browser sends first is not the server's to
  choose."
  [header]
  (when (string? header)
    (let [found (keep (fn [part]
                        (let [[k v] (str/split (str/trim part) #"=" 2)]
                          (when (= cookie-name k) v)))
                      (str/split header #";"))]
      (when (and (= 1 (count found)) (token? (first found)))
        (first found)))))
