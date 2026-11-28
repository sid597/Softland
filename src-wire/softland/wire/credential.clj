(ns softland.wire.credential
  "A credential: the text that a browser or a program holds, and how the
  runtime finds what it stands for.

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
  in makes its own and sends the runtime its hash. It makes a second
  secret only for asking, and sends that one's hash too. While it waits for
  her yes it sends the secret for asking with the code: that is how the
  runtime knows that the program that waits is the one that asked. That
  secret is on the wire, and is as safe as what carries it; it becomes no
  credential. The secret of the credential crosses no wire before her yes:
  when she has said yes, the program is told the root and the sign-in, puts
  the three parts together itself, and sends the secret for the first time
  as its credential. The runtime makes the secret of a browser's sign-in,
  and sets it as a cookie.

  A code is the short text under which a program's sign-in that is under
  way is found: eight characters of an alphabet that leaves out the letters
  and digits that a person confuses, shown as `XXXX-XXXX`. The program
  shows it, and the page on which she says yes shows it again."
  (:require [clojure.string :as str])
  (:import [java.nio.charset StandardCharsets]
           [java.security MessageDigest SecureRandom]
           [java.util Base64 UUID]))

(def cookie-name
  "softland-sign-in")

(def ^:private secret-pattern
  #"[A-Za-z0-9_-]{43}")

(def ^:private credential-pattern
  #"sl2_([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\.([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\.([A-Za-z0-9_-]{43})")

(def ^:private code-alphabet
  "23456789ABCDEFGHJKMNPQRSTUVWXYZ")

(def ^:private ^SecureRandom random
  (SecureRandom.))

(defn make-secret
  []
  (let [bytes (byte-array 32)]
    (.nextBytes random bytes)
    (.encodeToString (.withoutPadding (Base64/getUrlEncoder)) bytes)))

(defn secret?
  [x]
  (boolean (and (string? x) (re-matches secret-pattern x))))

(defn hash-of
  [secret]
  (when (secret? secret)
    (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                          (.getBytes ^String secret StandardCharsets/UTF_8))]
      (apply str (map (fn [b] (format "%02x" (bit-and 0xff (int b)))) digest)))))

(defn hash?
  [x]
  (boolean (and (string? x) (re-matches #"[0-9a-f]{64}" x))))

(defn same-hash?
  "The comparison reads every character whatever the first difference is,
  so the time that it takes tells nothing of a stored hash."
  [a b]
  (and (hash? a) (hash? b)
       (MessageDigest/isEqual (.getBytes ^String a StandardCharsets/US_ASCII)
                              (.getBytes ^String b StandardCharsets/US_ASCII))))

(defn text
  [root sign-in secret]
  (when (and (uuid? root) (uuid? sign-in) (secret? secret))
    (str "sl2_" root "." sign-in "." secret)))

(defn parts
  [credential]
  (when (string? credential)
    (when-let [[_ root sign-in secret] (re-matches credential-pattern credential)]
      {:root (UUID/fromString root) :sign-in (UUID/fromString sign-in) :secret secret})))

(defn make-code
  []
  (apply str (repeatedly 8 (fn [] (.charAt ^String code-alphabet (.nextInt random (count code-alphabet)))))))

(defn code?
  [x]
  (boolean (and (string? x) (= 8 (count x)) (every? (fn [ch] (str/index-of code-alphabet (str ch))) x))))

(defn shown
  "`code` must have the form of a code."
  [code]
  (str (subs code 0 4) "-" (subs code 4 8)))

(defn of-shown
  [typed]
  (when (string? typed)
    (let [code (-> typed (str/replace #"[\s-]" "") str/upper-case)]
      (when (code? code) code))))

(defn of-header
  [header]
  (when (string? header)
    (let [[scheme credential] (str/split (str/trim header) #"\s+" 2)]
      (when (and scheme (= "bearer" (str/lower-case scheme)) (parts credential))
        credential))))

(defn of-cookie
  "A header that holds the cookie twice gives nil: which of two a browser
  sends first is not the runtime's to choose."
  [header]
  (when (string? header)
    (let [found (keep (fn [part]
                        (let [[k v] (str/split (str/trim part) #"=" 2)]
                          (when (= cookie-name k) v)))
                      (str/split header #";"))]
      (when (and (= 1 (count found)) (parts (first found)))
        (first found)))))
