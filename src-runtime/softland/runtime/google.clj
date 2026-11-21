(ns softland.runtime.google
  "The check of Google's ID token: the proof that it is her, made once, at
  sign-in. It requires the JDK's own signature and HTTP client, and
  `clojure.data.json`.

  Google's sign-in button gives the browser an ID token, and the browser
  posts it to the runtime. The button needs a client id and no client
  secret (Sid, 15 October 2026, ‹bb59807f›: \"Google's sign-in button needs
  none, and that's my default\" yes), so the runtime holds no secret of
  Google's. The token is three parts of base64url with dots between: a
  header, the claims, and Google's signature over the first two.

  The check, in order; the first that fails gives the word:

    :not-a-token      the text is not three parts, or a part is no base64url
                      of JSON
    :wrong-algorithm  the header's `alg` is not exactly `RS256`. A token
                      that names `none`, or a hash with a shared secret, is
                      turned away here, before any key is looked at
    :key-not-known    the header's `kid` names none of the keys given
    :signature-wrong  the signature is not that key's over the two parts
    :wrong-issuer     `iss` is not one of the issuers
    :wrong-audience   `aud` is not this runtime's client id
    :expired          `exp` has passed, or is no number
    :no-subject       `sub` is absent or empty

  Inputs and results, by call:

    check       a token, and {:client-id :issuers :keys :now-ms} ->
                {:ok claims}, or {:refuse word}
    key-name    a token -> the name of the key that its header asks for,
                or nil for a text that the check would turn away unread
    fetch-keys  a URL -> [:ok {kid public-key}], or [:threw info]
    keys-of     the text of a JWKS document -> {kid public-key}

  The claims are a map from Google's own names, as strings, to what Google
  said: `sub`, the account's number, as it is; `email`, `name` and the
  rest.

  State and effects. `check` and `keys-of` are pure. `fetch-keys` makes one
  HTTPS request, and nothing is kept of what it gave: a sign-in happens
  once a sitting, and Google changes its keys, so the keys are fetched at
  each sign-in. A copy of them kept between two sign-ins would be a cache.

  Not here. What the runtime does with the claims:
  `softland.runtime.sign-in`. A stand-in for Google, for the checks and for
  a run on one machine before Sid has made the client: it signs its own
  tokens with a key pair of its own and hands the runtime its public key,
  and this check runs the same on them."
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
  "Takes a part of base64url without padding. Gives its bytes, or nil for a
  text that is none."
  ^bytes [^String part]
  (try (.decode (Base64/getUrlDecoder) part)
       (catch Throwable _ nil)))

(defn- json-of
  "Takes a part of base64url. Gives the JSON object in it as a map with
  string keys, or nil."
  [part]
  (when-let [bytes (decoded part)]
    (try (let [got (json/read-str (String. bytes StandardCharsets/UTF_8))]
           (when (map? got) got))
         (catch Throwable _ nil))))

(defn keys-of
  "Takes the text of a JWKS document. Gives a map from each RSA key's `kid`
  to its public key. A key of another kind, or one that cannot be read, is
  left out; a text that is no such document gives an empty map."
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
  "Fetches the public keys of an issuer. Takes the URL of its JWKS
  document. Gives [:ok {kid public-key}], or [:threw info] when the request
  failed or did not answer 200. One HTTPS request, with a wait of ten
  seconds; nothing is kept."
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
  "Takes the text of an ID token. Gives the `kid` of its header when the
  text has three parts, its header is JSON, and the header names the
  algorithm `RS256` and a key; nil otherwise. A face asks this before it
  fetches the issuer's keys, so that a text which `check` would turn away
  at its first two tests costs no request to the issuer."
  [token]
  (try
    (let [parts (when (string? token) (str/split token #"\." -1))
          header (when (= 3 (count parts)) (json-of (first parts)))
          kid (get header "kid")]
      (when (and (= "RS256" (get header "alg")) (string? kid) (seq kid))
        kid))
    (catch Throwable _ nil)))

(defn- signed-by?
  "Takes a public key, the text that was signed and the signature's bytes.
  Gives true when the signature is that key's, by RSA with SHA-256."
  [^PublicKey public-key ^String signed ^bytes signature]
  (try
    (let [verifier (doto (Signature/getInstance "SHA256withRSA")
                     (.initVerify public-key)
                     (.update (.getBytes signed StandardCharsets/US_ASCII)))]
      (.verify verifier signature))
    (catch Throwable _ false)))

(defn check
  "The check of an ID token. Takes the token's text, and a map: `:client-id`,
  this runtime's client id at the issuer; `:issuers`, the set of issuer
  texts that are taken; `:keys`, a map from a key's `kid` to its public
  key; `:now-ms`, the time now. Gives {:ok claims}, or {:refuse word}, as
  the namespace docstring lists them. It never throws.

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
