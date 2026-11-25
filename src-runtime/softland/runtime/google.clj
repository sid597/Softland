(ns softland.runtime.google
  "The check of Google's ID token: the proof that it is her, made once, at
  sign-in. `check` and `keys-of` are pure.

  Google's sign-in button gives the browser an ID token, and the browser
  posts it to the runtime. The button needs a client id and no client
  secret, so the runtime holds no secret of Google's. The token is three
  parts of base64url with dots between: a header, the claims, and Google's
  signature over the first two. The claims are a map from Google's own
  names, as strings, to what Google said: `sub`, the account's number, as
  it is; `email`, `name` and the rest.

  Nothing is kept of the keys that `fetch-keys` gave: a sign-in happens
  once a sitting, and Google changes its keys, so the keys are fetched at
  each sign-in. A copy of them kept between two sign-ins would be a cache."
  {:softland/whose-call
   [{:state :decided
     :what "Google's sign-in button needs a client id and no client secret
            (\"Google's sign-in button needs none, and that's my default\"
            yes), so the runtime holds no secret of Google's."
     :source "Sid, 15 October 2026, ‹bb59807f›"}]}
  (:require [clojure.data.json :as json]
            [clojure.string :as str])
  (:import [java.math BigInteger]
           [java.net URI]
           [java.net.http HttpClient HttpRequest HttpResponse$BodyHandlers]
           [java.nio.charset StandardCharsets]
           [java.security KeyFactory PublicKey Signature]
           [java.security.spec RSAPublicKeySpec]
           [java.time Duration]
           [java.util Base64]))

(def issuers
  "The two texts that Google writes as the issuer of its ID tokens."
  #{"accounts.google.com" "https://accounts.google.com"})

(def keys-url
  "Where Google publishes the public keys of its ID tokens."
  "https://www.googleapis.com/oauth2/v3/certs")

(defn- decoded
  ^bytes [^String part]
  (try (.decode (Base64/getUrlDecoder) part)
       (catch Throwable _ nil)))

(defn- json-of
  [part]
  (when-let [bytes (decoded part)]
    (try (let [got (json/read-str (String. bytes StandardCharsets/UTF_8))]
           (when (map? got) got))
         (catch Throwable _ nil))))

(defn keys-of
  "A key of another kind than RSA, or one that cannot be read, is left out;
  a text that is no JWKS document gives an empty map."
  [text]
  (try
    (into {}
          (keep (fn [k]
                  (when (and (= "RSA" (get k "kty")) (string? (get k "kid")))
                    (when-let [n (decoded (get k "n"))]
                      (when-let [e (decoded (get k "e"))]
                        [(get k "kid")
                         (.generatePublic (KeyFactory/getInstance "RSA")
                                          (RSAPublicKeySpec. (BigInteger. 1 n) (BigInteger. 1 e)))])))))
          (get (json/read-str text) "keys"))
    (catch Throwable _ {})))

(def ^:private shared-http-client
  "Shared JDK HttpClient for fetching JWKS keys so each sign-in does not leak a
  new SelectorManager thread."
  (delay (-> (HttpClient/newBuilder) (.connectTimeout (Duration/ofSeconds 10)) (.build))))

(defn fetch-keys
  [url]
  (try
    (let [^HttpClient client @shared-http-client
          request (-> (HttpRequest/newBuilder (URI/create url)) (.timeout (Duration/ofSeconds 10)) (.GET) (.build))
          response (.send client request (HttpResponse$BodyHandlers/ofString))]
      (if (= 200 (.statusCode response))
        [:ok (keys-of (.body response))]
        [:threw {:said (str "status " (.statusCode response))}]))
    (catch Throwable t
      [:threw {:threw (.getName (class t)) :said (.getMessage t)}])))

(defn key-name
  "The `kid` of the token's header, or nil for a text that `check` would
  turn away at its first two tests. A face asks this before it fetches the
  issuer's keys, so that such a text costs no request to the issuer."
  [token]
  (try
    (let [parts (when (string? token) (str/split token #"\." -1))
          header (when (= 3 (count parts)) (json-of (first parts)))
          kid (get header "kid")]
      (when (and (= "RS256" (get header "alg")) (string? kid) (seq kid))
        kid))
    (catch Throwable _ nil)))

(defn- signed-by?
  [^PublicKey public-key ^String signed ^bytes signature]
  (try
    (let [verifier (doto (Signature/getInstance "SHA256withRSA")
                     (.initVerify public-key)
                     (.update (.getBytes signed StandardCharsets/US_ASCII)))]
      (.verify verifier signature))
    (catch Throwable _ false)))

(defn check
  "`:keys` is a map from a key's `kid` to its public key, and `:now-ms` the
  time now. The first test that fails gives the word. It never throws.

  A header whose `alg` is not exactly `RS256` is turned away before any key
  is looked at: so a token that names `none`, or a hash with a shared
  secret.

  `aud` is taken as a text, or as a list that holds exactly this client
  id: a token for several audiences is not this runtime's alone."
  [token {:keys [client-id issuers keys now-ms]}]
  (try
    (let [parts (when (string? token) (str/split token #"\." -1))
          [header-part claims-part signature-part] parts
          header (when (= 3 (count parts)) (json-of header-part))
          claims (when header (json-of claims-part))
          signature (when claims (decoded signature-part))]
      (cond
        (not (and header claims signature)) {:refuse :not-a-token}
        (not= "RS256" (get header "alg")) {:refuse :wrong-algorithm}
        :else
        (let [public-key (get keys (get header "kid"))
              aud (get claims "aud")
              exp (get claims "exp")
              sub (get claims "sub")]
          (cond
            (nil? public-key) {:refuse :key-not-known}
            (not (signed-by? public-key (str header-part "." claims-part) signature)) {:refuse :signature-wrong}
            (not (contains? issuers (get claims "iss"))) {:refuse :wrong-issuer}
            (not (or (= client-id aud) (= [client-id] aud))) {:refuse :wrong-audience}
            (not (and (number? exp) (< now-ms (* 1000 (long exp))))) {:refuse :expired}
            (not (and (string? sub) (seq sub))) {:refuse :no-subject}
            :else {:ok claims}))))
    (catch Throwable _ {:refuse :not-a-token})))
