;; Phase 4 plan probe (26 Sept 2026): can the JDK seal a 32-byte lock to an
;; X25519 public key and open it with the private key, with no new
;; dependency, and how long do keygen and seal/open take? No cluster.
(import '[java.security KeyPairGenerator KeyFactory SecureRandom]
        '[java.security.spec PKCS8EncodedKeySpec X509EncodedKeySpec]
        '[javax.crypto KeyAgreement Cipher Mac]
        '[javax.crypto.spec SecretKeySpec GCMParameterSpec])
(def rng (SecureRandom.))
(defn rand-bytes [n] (let [b (byte-array n)] (.nextBytes rng b) b))
(defn keypair [] (let [g (KeyPairGenerator/getInstance "X25519")] (.generateKeyPair g)))
(defn pub-bytes [kp] (.getEncoded (.getPublic kp)))
(defn priv-bytes [kp] (.getEncoded (.getPrivate kp)))
(defn pub-of [bs] (.generatePublic (KeyFactory/getInstance "X25519") (X509EncodedKeySpec. bs)))
(defn priv-of [bs] (.generatePrivate (KeyFactory/getInstance "X25519") (PKCS8EncodedKeySpec. bs)))
(defn shared [priv pub] (let [ka (KeyAgreement/getInstance "XDH")] (.init ka priv) (.doPhase ka pub true) (.generateSecret ka)))
(defn hmac [k & parts] (let [m (Mac/getInstance "HmacSHA256")] (.init m (SecretKeySpec. k "HmacSHA256")) (doseq [p parts] (.update m ^bytes p)) (.doFinal m)))
(defn gcm [mode k nonce data] (let [c (Cipher/getInstance "AES/GCM/NoPadding")] (.init c (int mode) (SecretKeySpec. k "AES") (GCMParameterSpec. 128 nonce)) (.doFinal c data)))
(def info (.getBytes "softland/landing-box/v1" "UTF-8"))
(defn box [lease-pub-bs lock eph nonce]
  (let [s (shared (.getPrivate eph) (pub-of lease-pub-bs))
        w (hmac s info (pub-bytes eph) lease-pub-bs)]
    {:eph (pub-bytes eph) :nonce nonce :wrapped (gcm Cipher/ENCRYPT_MODE w nonce lock)}))
(defn unbox [lease-priv-bs lease-pub-bs {:keys [eph nonce wrapped]}]
  (try (let [s (shared (priv-of lease-priv-bs) (pub-of eph))
             w (hmac s info eph lease-pub-bs)]
         (gcm Cipher/DECRYPT_MODE w nonce wrapped))
       (catch Exception _ nil)))
(let [lease (keypair) lpub (pub-bytes lease) lpriv (priv-bytes lease)
      k-land (rand-bytes 32)
      b (box lpub k-land (keypair) (rand-bytes 12))
      other (keypair)
      ok (java.util.Arrays/equals ^bytes k-land ^bytes (unbox lpriv lpub b))
      wrong (unbox (priv-bytes other) (pub-bytes other) b)
      n 2000
      t0 (System/nanoTime) _ (dotimes [_ n] (keypair)) t1 (System/nanoTime)
      _ (dotimes [_ n] (box lpub k-land (keypair) (rand-bytes 12))) t2 (System/nanoTime)
      _ (dotimes [_ n] (unbox lpriv lpub b)) t3 (System/nanoTime)]
  (println "java" (System/getProperty "java.version"))
  (println "sizes: public" (count lpub) "bytes, private" (count lpriv) "bytes, box eph" (count (:eph b)) "+ nonce 12 + wrapped" (count (:wrapped b)))
  (println "opens with the lease's private key:" ok)
  (println "opens with another private key:" (some? wrong))
  (println (format "per op over %d: keygen %.3f ms, box (with its ephemeral keygen) %.3f ms, unbox %.3f ms"
                   n (/ (- t1 t0) 1e6 n) (/ (- t2 t1) 1e6 n) (/ (- t3 t2) 1e6 n))))
