(ns softland.runtime.google
  "The check of Google's ID token: the one way in for a person today
  (BUILD.md, Part 1). Pure code given its inputs: it takes the token's text,
  Google's key set, the client id and the time, and fetches nothing and
  reads no clock.

  What it checks, from Google's page \"Verify the Google ID token on your
  server side\", read 15 October 2026:

    1. the signature, RS256, against Google's published keys: the key is
       the one whose id is the token's `kid`;
    2. `aud` holds the client id;
    3. `iss` is `accounts.google.com` or `https://accounts.google.com`;
    4. `exp` has not passed, against the time handed in.

  Then the account's number: \"Only use the Google ID token's `sub` field
  as the unique identifier for the user\" (the same page). Google advises a
  JWT library and no check by hand. So the token is parsed by Nimbus
  (`com.nimbusds.jwt.SignedJWT/parse`), and its signature is verified by
  Nimbus's processor (`com.nimbusds.jwt.proc.DefaultJWTProcessor`) with a
  key selector for RS256 over the key set handed in
  (`JWSVerificationKeySelector`, `ImmutableJWKSet`). Nothing here verifies
  a signature.

  The claims are read only from what the processor gave back, so no claim
  of a token whose signature failed is read. They are compared here, each
  with its own word, and not by Nimbus's claims verifier: that verifier
  reads the JVM's clock, and gives one exception class for every fault. So
  the processor is given a claims verifier that checks nothing, and the four
  comparisons of the claims follow it in this namespace. Three tests of the
  header come before the processor, so that the words say more: it names
  RS256, it names a `kid`, and the key set holds a key with that `kid`.
  None of them is a check of the signature; the processor checks the
  algorithm and finds the key again by itself.

  No skew is allowed on `exp`: the token has passed at its `exp` second.
  Nimbus's own verifier allows 60 seconds by default, and Google's own
  libraries allow some; a caller that wants room gives a time that is
  earlier by it.

  Inputs and results, by call:

    check-id-token  a token's text, a JWKSet, the client id, the time in ms
                    -> {:ok {:sub :email :email-verified :name}} or
                       {:refuse word}

  The words, in the order of the checks: `words`.

  State and effects. None. The key set is borrowed, read and not changed.
  The fetch of Google's keys, from https://www.googleapis.com/oauth2/v3/certs
  at each sign-in, is the service's (`softland.runtime.service/fetch-google-keys`),
  and is not here.

  Callers. `softland.runtime.service`, at a sign-in and at a second account
  linked, before the steps of `softland.runtime.sign-in`, with the key set
  that its `:google-keys` gives; the pure proof and the service's proof, each
  with keys of its own."
  (:import [com.nimbusds.jose JOSEException JWSAlgorithm]
           [com.nimbusds.jose.jwk JWKSet]
           [com.nimbusds.jose.jwk.source ImmutableJWKSet]
           [com.nimbusds.jose.proc BadJOSEException JWSVerificationKeySelector]
           [com.nimbusds.jwt JWTClaimsSet SignedJWT]
           [com.nimbusds.jwt.proc DefaultJWTProcessor JWTClaimsSetVerifier]
           [java.util Date]))

(def issuers
  "The two texts of `iss` that Google's page accepts."
  #{"accounts.google.com" "https://accounts.google.com"})

(def max-token-length
  "How many characters a token's text can have before it is read. Google's
  ID tokens are about 1,000 to 1,500. A number of the build."
  16384)

(def words
  "The words of a refusal, in the order of the checks:

    :id-token-not-a-jwt            no string, longer than `max-token-length`,
                                   or no signed JWT that Nimbus parses
    :id-token-not-rs256            the header names another algorithm
    :id-token-has-no-key-id        the header names no `kid`
    :id-token-key-not-known        the key set holds no key with that `kid`
    :id-token-signature-not-good   Nimbus's processor refused the token:
                                   the signature does not verify with that
                                   key, or the key does not fit RS256
    :id-token-wrong-issuer         `iss` is neither of `issuers`
    :id-token-wrong-audience       `aud` does not hold the client id
    :id-token-expired              `exp` is absent, or not later than the
                                   time handed in
    :id-token-has-no-subject       `sub` is absent, empty, or longer than
                                   255 characters

  The words are a session's, Sid's to change."
  [:id-token-not-a-jwt :id-token-not-rs256 :id-token-has-no-key-id
   :id-token-key-not-known :id-token-signature-not-good :id-token-wrong-issuer
   :id-token-wrong-audience :id-token-expired :id-token-has-no-subject])

(def ^:private checks-nothing
  "A claims verifier that checks nothing. The processor is given it in place
  of Nimbus's default, which reads the JVM's clock; the claims are checked
  after the processor, in `claims-word`, against the time handed in."
  (reify JWTClaimsSetVerifier
    (verify [_ _ _] nil)))

(defn- processor
  "Takes a key set. Gives a Nimbus processor that accepts RS256 alone, takes
  its key from the key set by the token's `kid`, and checks no claim
  (`checks-nothing`). A new one for each check: it is cheap, and holds the
  key set that was handed in."
  [^JWKSet key-set]
  (doto (DefaultJWTProcessor.)
    (.setJWSKeySelector (JWSVerificationKeySelector. JWSAlgorithm/RS256 (ImmutableJWKSet. key-set)))
    (.setJWTClaimsSetVerifier checks-nothing)))

(defn- parsed
  "Takes anything. Gives the signed JWT that Nimbus parses from it, or nil:
  for no string, one over `max-token-length`, and any text that Nimbus does
  not parse as a signed JWT, an unsigned one among them."
  [text]
  (when (and (string? text) (<= (count text) max-token-length))
    (try (SignedJWT/parse ^String text)
         (catch Throwable _ nil))))

(defn- claims-word
  "Takes the claims that the processor gave back, the client id and the time
  in milliseconds. Gives the word of the first claim that fails, or nil:
  the issuer, the audience, the expiry, the subject."
  [^JWTClaimsSet claims client-id now-ms]
  (let [^Date exp (.getExpirationTime claims)
        sub (.getSubject claims)]
    (cond
      (not (contains? issuers (.getIssuer claims))) :id-token-wrong-issuer
      (not (some #(= client-id %) (.getAudience claims))) :id-token-wrong-audience
      (or (nil? exp) (<= (.getTime exp) (long now-ms))) :id-token-expired
      (not (and (string? sub) (<= 1 (count sub) 255))) :id-token-has-no-subject
      :else nil)))

(defn- claim-string
  "Takes the claims and a claim's name. Gives the claim when it is a
  string, else nil."
  [^JWTClaimsSet claims claim]
  (let [v (.getClaim claims ^String claim)]
    (when (string? v) v)))

(defn check-id-token
  "The check of a Google ID token. Takes the token's text as the browser
  sent it; Google's key set, a `com.nimbusds.jose.jwk.JWKSet` that the
  caller fetched; the client id, a string; and the time, in milliseconds
  since 1970, that the caller read. Gives {:ok claims} or {:refuse word},
  with one of `words`.

  The claims are {:sub :email :email-verified :name}: `sub` always, the
  account's number; the other three when the token holds them, each nil
  when it does not. The email and the name are Google's, as it gave them.

  In order: the text is parsed; the header names RS256 and a `kid` that the
  key set holds; Nimbus's processor verifies the signature with that key;
  then the issuer, the audience, the expiry against the time given, and the
  subject. It never throws: any throw of the processor is a refusal of the
  signature.

  What must hold: the key set is Google's, fetched over TLS by the caller.
  A key set of anyone else's makes their tokens pass; the pure proof uses
  that, with a key pair of its own."
  [text ^JWKSet key-set client-id now-ms]
  (let [jwt ^SignedJWT (parsed text)
        header (when jwt (.getHeader jwt))
        kid (when header (.getKeyID header))]
    (cond
      (nil? jwt) {:refuse :id-token-not-a-jwt}
      (not= JWSAlgorithm/RS256 (.getAlgorithm header)) {:refuse :id-token-not-rs256}
      (nil? kid) {:refuse :id-token-has-no-key-id}
      (nil? (and key-set (.getKeyByKeyId key-set kid))) {:refuse :id-token-key-not-known}
      :else
      (let [claims (try (.process ^DefaultJWTProcessor (processor key-set) jwt nil)
                        (catch BadJOSEException _ nil)
                        (catch JOSEException _ nil)
                        (catch Throwable _ nil))]
        (if (nil? claims)
          {:refuse :id-token-signature-not-good}
          (if-let [word (claims-word claims client-id now-ms)]
            {:refuse word}
            {:ok {:sub (.getSubject ^JWTClaimsSet claims)
                  :email (claim-string claims "email")
                  :email-verified (let [v (.getClaim ^JWTClaimsSet claims "email_verified")]
                                    (when (boolean? v) v))
                  :name (claim-string claims "name")}}))))))
