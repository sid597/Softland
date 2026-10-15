(ns softland.runtime.token
  "Tokens and invitation codes: made from ids and random bytes handed in,
  kept as a hash, read back, and checked against a kept hash. Pure code. It
  needs no cluster.

  A token says who its holder is and carries no right (BUILD.md, default 5).
  Its text is `sl1.<identity uuid>.<session uuid>.<secret>`, the secret 32
  random bytes in base64url with no padding (43 characters). An invitation's
  code is `sli.<invitation uuid>.<secret>`, made the same way. The forms are
  the plan's (PROGRESS.md, \"Tokens\"); the prefixes are a session's words.

  The store keeps the SHA-256 hex of the whole text, never the text: a
  token's hash beside its session (`:session/token-hash`), a code's beside
  its invitation (`:invitation/code-hash`). So no secret goes into the store
  (BUILD.md, default 6). A text is checked by hashing it and comparing the
  two hex texts with `java.security.MessageDigest/isEqual`, which takes the
  same time wherever two texts of one length differ.

  A token is ended by the runtime: `:session/ended` is written at its
  session, and its hash is kept (`softland.runtime.records/session-end-facts`).
  The runtime checks a token's secret against the kept hash before it reads
  whether the session ended, so only the holder of the right secret learns
  that it ended (`softland.runtime.service/actor-of`).

  Inputs and results, by call:

    make-token     an identity uuid, a session uuid, 32 bytes -> the text
    parse-token    anything -> {:identity :session :secret} or nil
    make-code      an invitation uuid, 32 bytes -> the text
    parse-code     anything -> {:invitation :secret} or nil
    text-hash      a text -> the SHA-256 hex of its UTF-8, 64 characters
    matches?       a text, and a kept hash -> true or false

  State and effects. None. Each function is pure; the random bytes come from
  the caller. Nothing here reads a clock or a random source, and no
  function throws on what it is given.

  Callers. `softland.runtime.sign-in`, for a sitting's token and an
  invitation's code; `softland.runtime.system-steps`, for an agent's or a
  program's token; `softland.runtime.service`, which reads a request's token
  back and checks it against the kept hash (`actor-of`); and the pure
  proof."
  (:require [clojure.string :as str])
  (:import [java.nio.charset StandardCharsets]
           [java.security MessageDigest]
           [java.util Base64 UUID]))

(def secret-bytes
  "How many random bytes the secret of a token or a code holds."
  32)

(def ^:private secret-length
  "How many characters the base64url text of `secret-bytes` bytes has, with
  no padding."
  43)

(defn- secret-text
  "Takes bytes. Gives their base64url text, with no padding."
  [^bytes bs]
  (.encodeToString (.withoutPadding (Base64/getUrlEncoder)) bs))

(defn- secret?
  "Takes anything. Gives true for a text of `secret-length` base64url
  characters that decodes to `secret-bytes` bytes and encodes back to
  itself, so each secret has one text."
  [x]
  (boolean
   (and (string? x)
        (= secret-length (count x))
        (re-matches #"[A-Za-z0-9_-]+" x)
        (let [bs (try (.decode (Base64/getUrlDecoder) ^String x) (catch Throwable _ nil))]
          (and bs (= secret-bytes (alength ^bytes bs)) (= x (secret-text bs)))))))

(defn- uuid-text
  "Takes a text. Gives the UUID that it is when it is the UUID's own
  printed text, in lowercase; nil for anything else. So each id in a token
  has one text, and a token has one hash."
  [s]
  (when-let [u (parse-uuid s)]
    (when (= s (str u)) u)))

(defn- secret-of
  "Takes anything. Gives the base64url text of 32 bytes, or nil when the
  thing is not 32 bytes."
  [random-bytes]
  (when (and (bytes? random-bytes) (= secret-bytes (alength ^bytes random-bytes)))
    (secret-text random-bytes)))

(defn make-token
  "Takes the id of the identity that will hold the token, the id of its
  session, both UUIDs, and 32 random bytes that the caller made. Gives the
  token's text, `sl1.<identity>.<session>.<secret>`. Gives nil when an id
  is no UUID or the bytes are not 32."
  [identity session random-bytes]
  (when-let [secret (secret-of random-bytes)]
    (when (and (uuid? identity) (uuid? session))
      (str "sl1." identity "." session "." secret))))

(defn parse-token
  "Takes anything. Gives {:identity uuid :session uuid :secret text} for a
  token's text in its one form, and nil for anything else: no string, a
  string longer than a token, another prefix, a UUID in another case, a
  secret that is not 32 bytes. It reads the text alone; whether the token
  stands is `matches?` against the hash kept at its session."
  [x]
  (when (and (string? x) (<= (count x) 200))
    (let [parts (str/split x #"\." -1)]
      (when (= 4 (count parts))
        (let [[prefix identity session secret] parts
              identity (uuid-text identity)
              session (uuid-text session)]
          (when (and (= "sl1" prefix) identity session (secret? secret))
            {:identity identity :session session :secret secret}))))))

(defn make-code
  "Takes an invitation's id, a UUID, and 32 random bytes that the caller
  made. Gives the invitation's code, `sli.<invitation>.<secret>`. Gives nil
  when the id is no UUID or the bytes are not 32."
  [invitation random-bytes]
  (when-let [secret (secret-of random-bytes)]
    (when (uuid? invitation)
      (str "sli." invitation "." secret))))

(defn parse-code
  "Takes anything. Gives {:invitation uuid :secret text} for an invitation's
  code in its one form, and nil for anything else, as `parse-token` does."
  [x]
  (when (and (string? x) (<= (count x) 200))
    (let [parts (str/split x #"\." -1)]
      (when (= 3 (count parts))
        (let [[prefix invitation secret] parts
              invitation (uuid-text invitation)]
          (when (and (= "sli" prefix) invitation (secret? secret))
            {:invitation invitation :secret secret}))))))

(defn text-hash
  "Takes a text. Gives the SHA-256 of its UTF-8 bytes as 64 lowercase hex
  digits: the hash that the store keeps of a token or a code."
  [^String text]
  (let [h (.digest (MessageDigest/getInstance "SHA-256") (.getBytes text StandardCharsets/UTF_8))]
    (apply str (map #(format "%02x" (bit-and (long %) 0xff)) h))))

(defn hash?
  "Takes anything. Gives true for 64 lowercase hex digits: the form of a
  kept hash."
  [x]
  (boolean (and (string? x) (re-matches #"[0-9a-f]{64}" x))))

(defn matches?
  "Takes a text that a caller presents, a token or a code, and the hash kept
  for it. Gives true when the text's hash is the kept hash. The two hex texts
  are compared with `MessageDigest/isEqual`, whose time does not depend on
  where they differ. Gives false for a text that is no string or a kept hash
  that is no hash, as for a token whose hash was retracted (nil). It never
  throws."
  [text kept-hash]
  (boolean
   (and (string? text) (<= (count text) 200) (hash? kept-hash)
        (MessageDigest/isEqual (.getBytes ^String (text-hash text) StandardCharsets/US_ASCII)
                               (.getBytes ^String kept-hash StandardCharsets/US_ASCII)))))
