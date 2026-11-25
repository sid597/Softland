(ns softland.runtime.google-stand-in
  "A stand-in for Google: a key pair of its own with which it signs ID
  tokens as Google does. The key pair lives in the memory of the process
  and nowhere else.

  It is for two things. The checks of the runtime use it, so that a sign-in
  runs from the token to the session with no call to Google. And a run on
  one machine can use it before Sid has made the client id: the program is
  started with it only when its opener says so, and it then says on every
  page that anybody can sign in as anybody.

  The runtime's check of a token (`softland.runtime.google/check`) is the
  same for a stand-in's token and for Google's: the stand-in gives the
  server its issuer, its client id and its public key, and nothing in the
  check knows which of the two it is looking at."
  (:require [clojure.data.json :as json])
  (:import [java.nio.charset StandardCharsets]
           [java.security KeyPairGenerator PrivateKey Signature]
           [java.util Base64]))

(defn make
  "The issuer and the client id are texts that no real issuer uses. `:keys`
  is as `softland.runtime.google/check` takes it."
  []
  (let [pair (.generateKeyPair (doto (KeyPairGenerator/getInstance "RSA") (.initialize 2048)))
        kid "softland-stand-in-1"]
    {:issuer "https://google-stand-in.softland.invalid"
     :client-id "softland-stand-in"
     :kid kid
     :keys {kid (.getPublic pair)}
     :private-key (.getPrivate pair)}))

(defn- part
  [^bytes bytes]
  (.encodeToString (.withoutPadding (Base64/getUrlEncoder)) bytes))

(defn token
  "`claims` is a map from a claim's name to its value, with `sub` at least;
  `now-ms` is milliseconds since 1970. The stand-in's issuer and client id
  are written as `iss` and `aud`, and the token lasts an hour, unless the
  claims given say otherwise: a check can so make a token that is wrong in
  one part."
  [stand-in claims now-ms]
  (let [header (part (.getBytes (json/write-str {"alg" "RS256" "kid" (:kid stand-in) "typ" "JWT"})
                                StandardCharsets/UTF_8))
        seconds (quot (long now-ms) 1000)
        claims (merge {"iss" (:issuer stand-in)
                       "aud" (:client-id stand-in)
                       "iat" seconds
                       "exp" (+ seconds 3600)}
                      claims)
        payload (part (.getBytes (json/write-str claims) StandardCharsets/UTF_8))
        signed (str header "." payload)
        signature (.sign (doto (Signature/getInstance "SHA256withRSA")
                           (.initSign ^PrivateKey (:private-key stand-in))
                           (.update (.getBytes signed StandardCharsets/US_ASCII))))]
    (str signed "." (part signature))))
