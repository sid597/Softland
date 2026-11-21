(ns softland.wire.credential
  "A credential: the text that a browser or a program holds, and how the
  runtime finds what it stands for. It requires nothing outside the JVM.

  A credential is three parts with dots between, after a prefix:

    sl2_<root>.<sign-in>.<secret>

  `root` is the id of the person at the root of the actor, `sign-in` is the
  id of the sign-in, both UUIDs, and `secret` is 43 characters of
  base64url: 32 random bytes. The first two say where the sign-in's record
  is, so the runtime reads it with one round trip to one task and needs no
  table from a secret to its holder. They are no proof of anything: the
  proof is the secret, whose SHA-256 the record holds
  (src/proposal/runtime-2026-11-21/spine/auth/SPEC.md). The runtime keeps
  no secret.

  Whoever is signed in makes the secret. A program that asks to be signed
  in makes its own and sends the runtime its hash, so the secret crosses no
  wire before her yes; when she has said yes, the program is told the root
  and the sign-in, and puts the three parts together itself. The runtime
  makes the secret of a browser's sign-in, and sets it as a cookie.

  A code is the short text under which a program's sign-in that is under
  way is found: eight characters of an alphabet that leaves out the letters
  and digits that a person confuses, shown as `XXXX-XXXX`. The program
  shows it, and the page on which she says yes shows it again.

  Inputs and results, by call:

    make-secret   nothing -> a new secret
    secret?       anything -> whether it has the form of a secret
    hash-of       a secret -> its hash, 64 hex characters, or nil
    hash?         anything -> whether it has the form of a hash
    text          a root, a sign-in, a secret -> the credential's text
    parts         a credential's text -> {:root :sign-in :secret}, or nil
    make-code     nothing -> a new code
    code?         anything -> whether it has the form of a code
    shown         a code -> the code as a person reads it
    of-shown      what a person typed -> the code, or nil
    same-hash?    two hashes -> whether they are the same, in a time that
                  does not depend on where they differ
    of-header     the text of an Authorization header -> the credential's
                  text, or nil
    of-cookie     the text of a Cookie header -> the credential's text, or
                  nil

  State and effects. `make-secret` and `make-code` read the JVM's secure
  random numbers. Nothing else has an effect.

  Not decided, kept for now: the form of a credential, the length of a
  secret and of a code, the hash, and the name of the cookie. Each is in
  one place here."
  (:require [clojure.string :as str])
  (:import [java.nio.charset StandardCharsets]
           [java.security MessageDigest SecureRandom]
           [java.util Base64 UUID]))

(def cookie-name
  "The name of the cookie in which a browser holds its credential."
  "softland-sign-in")

(def ^:private secret-pattern
  "The form of a secret: 43 characters of base64url."
  #"[A-Za-z0-9_-]{43}")

(def ^:private credential-pattern
  "The form of a credential: the prefix, two UUIDs and a secret."
  #"sl2_([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\.([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\.([A-Za-z0-9_-]{43})")

(def ^:private code-alphabet
  "The characters of a code: digits and capital letters, without 0, O, 1, I
  and L."
  "23456789ABCDEFGHJKMNPQRSTUVWXYZ")

(def ^:private ^SecureRandom random
  "The JVM's secure random numbers, for a secret and a code."
  (SecureRandom.))

(defn make-secret
  "Gives a new secret: 32 random bytes as base64url without padding."
  []
  (let [bytes (byte-array 32)]
    (.nextBytes random bytes)
    (.encodeToString (.withoutPadding (Base64/getUrlEncoder)) bytes)))

(defn secret?
  "Takes anything. Gives true for a text that has the form of a secret."
  [x]
  (boolean (and (string? x) (re-matches secret-pattern x))))

(defn hash-of
  "Takes a secret. Gives its hash, the SHA-256 of its text as 64 lower-case
  hex characters; nil for a thing that has not the form of a secret."
  [secret]
  (when (secret? secret)
    (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                          (.getBytes ^String secret StandardCharsets/UTF_8))]
      (apply str (map (fn [b] (format "%02x" (bit-and 0xff (int b)))) digest)))))

(defn hash?
  "Takes anything. Gives true for 64 lower-case hex characters."
  [x]
  (boolean (and (string? x) (re-matches #"[0-9a-f]{64}" x))))

(defn same-hash?
  "Takes two things. Gives true when both are hashes and the same. The
  comparison reads every character whatever the first difference is, so
  the time that it takes tells nothing of a stored hash."
  [a b]
  (and (hash? a) (hash? b)
       (MessageDigest/isEqual (.getBytes ^String a StandardCharsets/US_ASCII)
                              (.getBytes ^String b StandardCharsets/US_ASCII))))

(defn text
  "Takes the id of a root, the id of a sign-in, and a secret. Gives the
  credential's text, or nil when a part has not its form."
  [root sign-in secret]
  (when (and (uuid? root) (uuid? sign-in) (secret? secret))
    (str "sl2_" root "." sign-in "." secret)))

(defn parts
  "Takes a credential's text. Gives {:root :sign-in :secret}, the two ids
  as UUIDs, or nil for a thing of another form."
  [credential]
  (when (string? credential)
    (when-let [[_ root sign-in secret] (re-matches credential-pattern credential)]
      {:root (UUID/fromString root) :sign-in (UUID/fromString sign-in) :secret secret})))

(defn make-code
  "Gives a new code: eight random characters of the code's alphabet."
  []
  (apply str (repeatedly 8 (fn [] (.charAt ^String code-alphabet (.nextInt random (count code-alphabet)))))))

(defn code?
  "Takes anything. Gives true for a text that has the form of a code."
  [x]
  (boolean (and (string? x) (= 8 (count x)) (every? (fn [ch] (str/index-of code-alphabet (str ch))) x))))

(defn shown
  "Takes a code. Gives it as a person reads it: `XXXX-XXXX`."
  [code]
  (str (subs code 0 4) "-" (subs code 4 8)))

(defn of-shown
  "Takes what a person typed or a link carried. Gives the code in it, or
  nil: blank space and the dash are dropped, and small letters are read as
  capitals."
  [typed]
  (when (string? typed)
    (let [code (-> typed (str/replace #"[\s-]" "") str/upper-case)]
      (when (code? code) code))))

(defn of-header
  "Takes the text of an Authorization header, or nil. Gives the credential
  of `Bearer <credential>`, or nil when the header is absent or holds no
  credential of the form."
  [header]
  (when (string? header)
    (let [[scheme credential] (str/split (str/trim header) #"\s+" 2)]
      (when (and scheme (= "bearer" (str/lower-case scheme)) (parts credential))
        credential))))

(defn of-cookie
  "Takes the text of a Cookie header, or nil. Gives the credential that the
  cookie `cookie-name` holds, or nil. A header that holds the cookie twice
  gives nil: which of two a browser sends first is not the runtime's to
  choose."
  [header]
  (when (string? header)
    (let [found (keep (fn [part]
                        (let [[k v] (str/split (str/trim part) #"=" 2)]
                          (when (= cookie-name k) v)))
                      (str/split header #";"))]
      (when (and (= 1 (count found)) (parts (first found)))
        (first found)))))
