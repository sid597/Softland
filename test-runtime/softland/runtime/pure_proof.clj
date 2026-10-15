(ns softland.runtime.pure-proof
  "The proof of the runtime's pure part: who is acting. It needs no cluster
  and no network. One command, from the repository root:

    clojure -M:runtime -m softland.runtime.pure-proof

  It prints each check's sentence after PASSED or FAILED, prints what it
  saw on lines that start with SHOWN, prints those lines again together at
  the end, and exits with a status that is not zero when a check fails.

  What it checks, in order: the ids; tokens and invitation codes; the check
  of Google's ID token, with a key pair that the proof makes (Nimbus's
  `RSAKeyGenerator`) and tokens it signs the way Google does; the records;
  the binding of acts and queries, blocks and limits; the runtime's start;
  and sign-in: a first one from nothing, the same run started again from
  the state after each of its acts, two runs of one account at once, a later
  sign-in, a second account linked, and the refusals.

  The steps run against a model of the store
  (`softland.runtime.store-model`), which holds every act to the act's one
  form and every query to the query's, and keeps the rules of the gates and
  the read path that the steps lean on. It is a model: what the real gates
  and read path do with these acts is the proof on a cluster.

  State. The proof owns one atom of lines it showed, and an atom per run of
  the steps, for the model's store. It makes random ids, as a caller of the
  pure code does."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]
            [softland.runtime.binding :as binding]
            [softland.runtime.google :as google]
            [softland.runtime.ids :as ids]
            [softland.runtime.records :as records]
            [softland.runtime.sign-in :as sign-in]
            [softland.runtime.start :as start]
            [softland.runtime.steps :as steps]
            [softland.runtime.store-model :as model]
            [softland.runtime.token :as token])
  (:import [com.nimbusds.jose JOSEObjectType JWSAlgorithm JWSHeader$Builder]
           [com.nimbusds.jose.crypto MACSigner RSASSASigner]
           [com.nimbusds.jose.jwk JWKSet KeyUse RSAKey]
           [com.nimbusds.jose.jwk.gen RSAKeyGenerator]
           [com.nimbusds.jwt JWTClaimsSet$Builder SignedJWT]
           [java.security SecureRandom]
           [java.util Date UUID]))

;; ------------------------------------------------------------------ printing

(defonce ^{:private true
           :doc "The lines that the proof printed with SHOWN, for the recap."}
  shown-lines (atom []))

(defn- shown
  "Prints one line of what the proof saw, and keeps it for the recap."
  [& xs]
  (let [line (apply print-str "SHOWN" xs)]
    (swap! shown-lines conj line)
    (println line)
    (flush)))

(defn- holds
  "Checks one thing: the value must be true. Prints the sentence after
  PASSED or FAILED."
  [sentence ok?]
  (println (if (is (true? (boolean ok?)) sentence) "PASSED" "FAILED") sentence)
  (flush))

;; ------------------------------------------------------------------ random

(def ^:private random (SecureRandom.))

(defn- random-bytes
  "n random bytes."
  [n]
  (let [b (byte-array n)] (.nextBytes ^SecureRandom random b) b))

(defn- fresh
  "Fresh ids for a sign-in, as a caller makes them."
  []
  {:person (random-uuid) :record-layer (random-uuid) :own-layer (random-uuid)
   :session (random-uuid) :secret (random-bytes 32)
   :name (ids/provisional-name (random-bytes 8))})

;; ------------------------------------------------------------------- ids

(deftest the-ids
  (holds "a UUID version 5 of RFC 4122's example (the DNS namespace, www.example.com) is 2ed6657d-e927-568b-95e1-2665a8aea6a2"
         (= #uuid "2ed6657d-e927-568b-95e1-2665a8aea6a2"
            (ids/uuid-v5 #uuid "6ba7b810-9dad-11d1-80b4-00c04fd430c8" "www.example.com")))
  (let [a (ids/account-id :google "1234567890")]
    (shown "account id of google 1234567890:" a)
    (holds "one account is one id: the same kind and number give the same UUID, of version 5 and RFC 4122's variant"
           (and (= a (ids/account-id :google "1234567890"))
                (= 5 (.version ^UUID a)) (= 2 (.variant ^UUID a))))
    (holds "two numbers, or one number of two kinds, give two ids"
           (= 3 (count (set [a (ids/account-id :google "1234567891") (ids/account-id :github "1234567890")])))))
  (holds "an account id is refused for an empty number, a number that is no string, and a kind with a namespace"
         (every? nil? [(ids/account-id :google "") (ids/account-id :google 12) (ids/account-id :x/google "1")]))
  (holds "a name is 1 to 32 lowercase letters, digits, - and _, first a letter"
         (and (every? ids/name? ["s" "sid" "a-b_c9" (apply str (repeat 32 "a"))])
              (not-any? ids/name? ["" "Sid" "9a" "-a" (apply str (repeat 33 "a")) "a b" "ä" nil :sid])))
  (holds "a name's entity is (keyword \"name\" n), and nil for no name"
         (and (= :name/sid (ids/name-entity "sid")) (nil? (ids/name-entity "Sid"))))
  (let [n (ids/provisional-name (byte-array (range 8)))]
    (shown "a provisional name:" n)
    (holds "a provisional name is new- and 16 hex digits, keeps the rule of a name, and needs 8 bytes"
           (and (= "new-0001020304050607" n) (ids/name? n) (nil? (ids/provisional-name (byte-array 7)))))))

;; ------------------------------------------------------------------ tokens

(deftest tokens-and-codes
  (let [p (random-uuid) s (random-uuid)
        t (token/make-token p s (random-bytes 32))
        h (token/text-hash t)]
    (shown "a token:" (subs t 0 (- (count t) 43)) "<secret>" "length" (count t))
    (holds "a token reads back as its identity, its session and its secret"
           (= {:identity p :session s} (select-keys (token/parse-token t) [:identity :session])))
    (holds "a token's hash is 64 lowercase hex digits, and the token matches it"
           (and (token/hash? h) (token/matches? t h)))
    (holds "a token with one character changed does not match the kept hash, and nothing matches a retracted hash (nil)"
           (let [changed (str (subs t 0 (dec (count t))) (if (str/ends-with? t "A") "B" "A"))]
             (and (not (token/matches? changed h)) (not (token/matches? t nil)))))
    (holds "a token is not read from another prefix, an id in capitals, a secret of 31 bytes, a fifth part, or no string"
           (every? nil? [(token/parse-token (str/replace-first t "sl1." "sl2."))
                         (token/parse-token (str/replace-first t (str p) (str/upper-case (str p))))
                         (token/parse-token (str "sl1." p "." s "." (.encodeToString (.withoutPadding (java.util.Base64/getUrlEncoder)) (random-bytes 31))))
                         (token/parse-token (str t ".x"))
                         (token/parse-token nil)]))
    (holds "no token is made of 31 bytes or of an id that is no UUID"
           (and (nil? (token/make-token p s (random-bytes 31))) (nil? (token/make-token :p s (random-bytes 32))))))
  (let [i (random-uuid)
        c (token/make-code i (random-bytes 32))]
    (holds "an invitation's code reads back as its invitation, and matches its hash; a token does not read as a code"
           (and (= i (:invitation (token/parse-code c)))
                (token/matches? c (token/text-hash c))
                (nil? (token/parse-code (token/make-token (random-uuid) (random-uuid) (random-bytes 32))))))))

;; ------------------------------------------------------------------ google

(def ^:private client-id "proof-client.apps.googleusercontent.com")

(defn- rsa-key
  "A key pair of the proof's, with its kid."
  ^RSAKey [kid]
  (-> (RSAKeyGenerator. (int 2048)) (.keyID kid) (.keyUse KeyUse/SIGNATURE) (.algorithm JWSAlgorithm/RS256) (.generate)))

(defn- google-token
  "A token signed the way Google signs one. Takes the signer's key, the
  header's kid (nil for none), and claims to change from a good token's."
  [^RSAKey k kid now-ms changes]
  (let [c (merge {:iss "https://accounts.google.com" :aud client-id :sub "110169484474386276334"
                  :email "sid@example.com" :email_verified true :name "Sid"
                  :exp (+ now-ms 3600000) :iat now-ms}
                 changes)
        b (JWTClaimsSet$Builder.)]
    (when (:iss c) (.issuer b (:iss c)))
    (when (:aud c) (.audience b ^String (:aud c)))
    (when (:sub c) (.subject b (:sub c)))
    (when (:exp c) (.expirationTime b (Date. (long (:exp c)))))
    (.issueTime b (Date. (long (:iat c))))
    (.claim b "email" (:email c))
    (.claim b "email_verified" (:email_verified c))
    (.claim b "name" (:name c))
    (let [header (cond-> (JWSHeader$Builder. JWSAlgorithm/RS256)
                   kid (.keyID kid)
                   true (.type JOSEObjectType/JWT))
          jwt (SignedJWT. (.build header) (.build b))]
      (.sign jwt (RSASSASigner. k))
      (.serialize jwt))))

(deftest googles-id-token
  (let [k1 (rsa-key "k1")
        other (rsa-key "k1")
        key-set (JWKSet. (.toPublicJWK k1))
        now 1760500000000
        check #(google/check-id-token % key-set client-id now)
        good (check (google-token k1 "k1" now {}))]
    (shown "a good token gives:" good)
    (holds "a good token, RS256 by the key of its kid, for this client, from Google's issuer, not expired, gives its claims"
           (= {:ok {:sub "110169484474386276334" :email "sid@example.com" :email-verified true :name "Sid"}} good))
    (holds "the issuer without https is Google's too"
           (contains? (check (google-token k1 "k1" now {:iss "accounts.google.com"})) :ok))
    (holds "a token for another client is refused :id-token-wrong-audience"
           (= {:refuse :id-token-wrong-audience} (check (google-token k1 "k1" now {:aud "another-client"}))))
    (holds "a token from another issuer is refused :id-token-wrong-issuer"
           (= {:refuse :id-token-wrong-issuer} (check (google-token k1 "k1" now {:iss "https://accounts.example.com"}))))
    (holds "a token whose exp passed a second ago, or is the time given, is refused :id-token-expired"
           (and (= {:refuse :id-token-expired} (check (google-token k1 "k1" now {:exp (- now 1000)})))
                (= {:refuse :id-token-expired} (check (google-token k1 "k1" now {:exp now})))))
    (holds "a token signed by another key under the same kid is refused :id-token-signature-not-good"
           (= {:refuse :id-token-signature-not-good} (check (google-token other "k1" now {}))))
    (holds "a token whose kid is not in the key set is refused :id-token-key-not-known"
           (= {:refuse :id-token-key-not-known} (check (google-token k1 "k9" now {}))))
    (holds "a token with no kid is refused :id-token-has-no-key-id"
           (= {:refuse :id-token-has-no-key-id} (check (google-token k1 nil now {}))))
    (holds "a token signed HS256 is refused :id-token-not-rs256"
           (let [jwt (SignedJWT. (.build (.keyID (JWSHeader$Builder. JWSAlgorithm/HS256) "k1"))
                                 (.build (.subject (JWTClaimsSet$Builder.) "1")))]
             (.sign jwt (MACSigner. (byte-array 32 (byte 7))))
             (= {:refuse :id-token-not-rs256} (check (.serialize jwt)))))
    (holds "text that is no signed JWT, an unsigned JWT, and nil are refused :id-token-not-a-jwt"
           (every? #(= {:refuse :id-token-not-a-jwt} (check %))
                   ["abc" nil "eyJhbGciOiJub25lIn0.eyJzdWIiOiIxIn0." (apply str (repeat 20000 "a"))]))
    (holds "a token with no sub is refused :id-token-has-no-subject"
           (= {:refuse :id-token-has-no-subject} (check (google-token k1 "k1" now {:sub nil}))))))

;; ----------------------------------------------------------------- records

(deftest the-records
  (let [p (random-uuid) r (random-uuid) o (random-uuid)
        person {:identity/kind :person :identity/name "sid" :identity/record-layer r :identity/own-layer o}]
    (holds "a person's record is checked and becomes one fact for each part, in order, none a retract"
           (= {:ok [{:e p :k :identity/kind :v :person} {:e p :k :identity/name :v "sid"}
                    {:e p :k :identity/record-layer :v r} {:e p :k :identity/own-layer :v o}]}
              (records/facts-of :person p person)))
    (holds "a name that breaks the rule is refused :name-not-allowed at its path"
           (= {:refuse :name-not-allowed :at [:identity/name]} (records/check :person (assoc person :identity/name "Sid"))))
    (holds "a record that misses a part, or has one it does not name, is refused :record-not-in-shape at that part"
           (and (= {:refuse :record-not-in-shape :at :identity/kind} (records/check :person (dissoc person :identity/kind)))
                (= {:refuse :record-not-in-shape :at :x/y} (records/check :person (assoc person :x/y 1)))))
    (holds "an invitation's code hash must be a SHA-256 hex text, and its gives the act's forms of what a right allows"
           (and (= :not-a-hash (:refuse (records/check :invitation {:invitation/code-hash "abc" :invitation/gives [] :invitation/made-by :system})))
                (contains? (records/check :invitation {:invitation/code-hash (token/text-hash "x")
                                                       :invitation/gives [{:layer :base :allows :see-and-act}
                                                                          {:layer :base :allows #{:note/* :*}}]
                                                       :invitation/made-by :system}) :ok)
                (= :record-not-in-shape (:refuse (records/check :invitation {:invitation/code-hash (token/text-hash "x")
                                                                             :invitation/gives [{:layer :base :allows :all}]
                                                                             :invitation/made-by :system})))))
    (holds "each record of the plan has a shape: an agent with its description, a run, a session, a block, a limit"
           (every? #(contains? % :ok)
                   [(records/check :agent {:identity/kind :agent :identity/name "helper" :identity/record-layer r :identity/made-by p})
                    (records/check :agent-description {:agent/description {:instructions "Write notes." :engine {:provider "anthropic" :model "m"}}})
                    (records/check :run {:run/session (random-uuid) :run/description [[o :streaming (random-uuid)] 0]
                                         :run/engine {:provider "anthropic" :model "m" :effort :high}
                                         :run/context {:given ["a"]} :run/human-in-loop true :run/cause :asked})
                    (records/check :session {:session/of p :session/kind :conversation :session/token-hash (token/text-hash "t") :session/made-by p})
                    (records/check :block {:rule/block {:keys #{:note/*} :layers #{:base} :per-minute 5}})
                    (records/check :block {:rule/block {}})
                    (records/check :limit {:rule/acts-per-minute 60})]))
    (holds "the shape under each record's name is that record's own: no shape of a value took its name"
           (every? (fn [[record-name {:keys [parts]}]]
                     (= (mapv first parts) (mapv first (rest (get records/shapes record-name)))))
                   records/records))
    (holds "one part of a record is checked alone: a claim of :invitation/used"
           (= {:ok [{:e p :k :invitation/used :v {:person p :account r}}]}
              (records/part-facts :invitation p {:invitation/used {:person p :account r}})))
    (holds "ending a session writes when it ended and keeps its token's hash"
           (= [{:e p :k :session/ended :v 5}] (records/session-end-facts p 5))))
  (let [marks (into {} (map (fn [l] [l (records/one-write-keys l)])) ids/system-layers)]
    (shown "one-write keys by system layer:" marks)
    (holds "each system layer marks the one-write keys of the plan: all of an identity but its name, a link's person, a name, an invitation's use"
           (= {:system/identities #{:identity/kind :identity/record-layer :identity/own-layer :identity/made-by
                                    :identity/copied-from :identity/invited-by}
               :system/ways-in #{:way-in/person :way-in/invitation}
               :system/names #{:name/of}
               :system/invitations #{:invitation/used}
               :system/rules #{}
               :system/record #{}}
              marks))))

;; ----------------------------------------------------------------- binding

(deftest the-binding
  (let [p (random-uuid) s (random-uuid) l (random-uuid)
        person {:id p :kind :person :session s}
        agent-id (random-uuid) run (random-uuid)
        agent {:id agent-id :kind :agent :session s :run run}
        note (fn [who] {:version 1 :layer l :permission [who l l] :facts [{:e (random-uuid) :k :note/text :v "hi"}]})]
    (holds "a person's act is given her :who and her sitting as :session"
           (let [{:keys [ok]} (binding/bind-act person (note p))] (and (= p (:who ok)) (= s (:session ok)))))
    (holds "an agent's act is given the agent as :who and its run as :session"
           (let [{:keys [ok]} (binding/bind-act agent (note agent-id))] (and (= agent-id (:who ok)) (= run (:session ok)))))
    (holds "an act that names another :who is refused :who-not-the-actor (a client that claims another's name)"
           (= {:refuse :who-not-the-actor} (binding/bind-act person (assoc (note p) :who (random-uuid)))))
    (holds "an act that names another :session is refused :session-not-the-actors; an agent's naming its session and not its run too"
           (and (= {:refuse :session-not-the-actors} (binding/bind-act person (assoc (note p) :session (random-uuid))))
                (= {:refuse :session-not-the-actors} (binding/bind-act agent (assoc (note agent-id) :session s)))))
    (holds "an act whose permission's holder is not the actor is refused :permission-not-the-actors"
           (= {:refuse :permission-not-the-actors} (binding/bind-act person (note (random-uuid)))))
    (holds "an agent's act without a run is refused :agent-act-without-a-run"
           (= {:refuse :agent-act-without-a-run} (binding/bind-act (dissoc agent :run) (note agent-id))))
    (let [making (fn [who] {:version 1 :layer l :permission [who l l]
                            :facts [{:e l :k :softland.store/depot :v :streaming}
                                    {:e who :k :softland.store/permission :v [who l l]}]})]
      (holds "an agent's or a program's act that would make a layer is refused :may-not-make-a-layer; a person's passes"
             (and (= {:refuse :may-not-make-a-layer} (binding/bind-act agent (making agent-id)))
                  (= {:refuse :may-not-make-a-layer} (binding/bind-act {:id agent-id :kind :program :session s} (making agent-id)))
                  (contains? (binding/bind-act person (making p)) :ok))))
    (let [o (random-uuid) stranger (random-uuid)
          under (fn [allows facts] {:version 1 :layer o
                                    :permission (cond-> [agent-id o o [p o o]] (some? allows) (conj allows))
                                    :facts facts})
          give [{:e stranger :k :softland.store/permission :v [stranger o o [agent-id o o [p o o] #{:note/*}] :see]}]
          setting [{:e o :k :softland.store/who-may-see :v :everyone}]
          word #(:refuse (binding/bind-act agent %))]
      (holds "an agent's give or setting is refused :store-key-not-allowed-for-an-agent under a right with no key list, :see-and-act, or a list that does not name the key, and passes when its list names the key or :softland.store/*"
             (and (= :store-key-not-allowed-for-an-agent (word (under nil give)))
                  (= :store-key-not-allowed-for-an-agent (word (under :see-and-act setting)))
                  (= :store-key-not-allowed-for-an-agent (word (under #{:* :softland.store/permission} setting)))
                  (nil? (word (under #{:note/* :softland.store/permission} give)))
                  (nil? (word (under #{:softland.store/*} setting)))
                  (nil? (:refuse (binding/bind-act person {:version 1 :layer o :permission [p o o] :facts setting}))))))
    (holds "a thing that is no map is refused :not-an-act; no actor is refused :actor-not-known"
           (and (every? #(= {:refuse :not-an-act} (binding/bind-act person %)) [nil "act" [] 7])
                (= {:refuse :actor-not-known} (binding/bind-act {:id :system :kind :system :session s} (note p)))))
    (let [empty-act (assoc (note p) :facts [])
          no-permission (dissoc (note p) :permission)
          agents-act (assoc (note agent-id) :extra 1)]
      (shown "an act that does not parse, bound:" (binding/bind-act person empty-act))
      (holds "an act that does not parse comes back {:ok} with the actor's :who and :session on it, for the gate to log and refuse"
             (and (= {:ok (assoc empty-act :who p :session s)} (binding/bind-act person empty-act))
                  (= {:ok (assoc no-permission :who p :session s)} (binding/bind-act person no-permission))
                  (= {:ok (assoc agents-act :who agent-id :session run)} (binding/bind-act agent agents-act)))))
    (holds "an act that does not parse is still refused when it names another :who or :session"
           (and (= {:refuse :who-not-the-actor} (binding/bind-act person (assoc (note p) :facts [] :who (random-uuid))))
                (= {:refuse :session-not-the-actors} (binding/bind-act person (assoc (note p) :facts [] :session (random-uuid))))))
    (holds "an act that does not parse is still refused where its permission's holder, or an agent's depot fact, can be read"
           (and (= {:refuse :permission-not-the-actors} (binding/bind-act person (assoc (note (random-uuid)) :facts [])))
                (= {:refuse :may-not-make-a-layer}
                   (binding/bind-act agent {:version 1 :layer l :permission [agent-id l l] :extra 1
                                            :facts [{:e l :k :softland.store/depot :v :streaming}]}))))
    (let [q {:asks [:place :x :note/text] :layers [:base]}]
      (holds "a query with no one signed in says :not-signed-in true; one that names an asker then is refused"
             (and (= {:ok (assoc q :not-signed-in true)} (binding/bind-query nil q))
                  (= {:refuse :asker-not-the-actor} (binding/bind-query nil (assoc q :asker p)))))
      (holds "a signed-in actor's query asks as her; one that names another asker is refused :asker-not-the-actor"
             (and (= {:ok (assoc q :asker p)} (binding/bind-query person q))
                  (= {:ok (assoc q :asker agent-id)} (binding/bind-query agent (assoc q :asker agent-id)))
                  (= {:refuse :asker-not-the-actor} (binding/bind-query person (assoc q :asker (random-uuid))))))
      (holds "a signed-in actor may ask as not signed in, and is not given an asker"
             (= {:ok (assoc q :not-signed-in true)} (binding/bind-query person (assoc q :not-signed-in true)))))
    (let [act-in (fn [layer k] {:layer layer :facts [{:e :x :k k :v 1}]})
          read-of (fn [layer] {:asks [:place :x :note/text] :layers [layer]})
          give {:layer l :facts [{:e :x :k :softland.store/permission :v [:x l l]}]}]
      (holds "a block of layers stops every act and every read in them, and none elsewhere"
             (let [b {:layers #{l}}]
               (and (binding/block-stops-act? b (act-in l :note/text) 0) (binding/block-stops-read? b (read-of l))
                    (not (binding/block-stops-act? b (act-in :base :note/text) 0)) (not (binding/block-stops-read? b (read-of :base))))))
      (holds "a block of keys stops acts under those keys (a key, :ns/*) and no read; :* never reaches a store key"
             (and (binding/block-stops-act? {:keys #{:note/*}} (act-in l :note/text) 0)
                  (not (binding/block-stops-act? {:keys #{:note/*}} (act-in l :todo/text) 0))
                  (not (binding/block-stops-read? {:keys #{:note/*}} (read-of l)))
                  (binding/block-stops-act? {:keys #{:*}} (act-in l :todo/text) 0)
                  (not (binding/block-stops-act? {:keys #{:*}} give 0))
                  (binding/block-stops-act? {:keys #{:softland.store/*}} give 0)))
      (holds "a block of a rate stops an act at the count and over it, not under it, and stops no read; an empty block stops everything"
             (and (binding/block-stops-act? {:per-minute 3} (act-in l :note/text) 3)
                  (not (binding/block-stops-act? {:per-minute 3} (act-in l :note/text) 2))
                  (not (binding/block-stops-read? {:per-minute 3} (read-of l)))
                  (binding/block-stops-act? {} (act-in l :note/text) 0)
                  (binding/block-stops-read? {} (read-of l))))
      (holds "a count is over a limit at the limit and past it; no limit is never over"
             (and (not (binding/over-limit? 10 9)) (binding/over-limit? 10 10) (not (binding/over-limit? nil 1000)))))))

;; ------------------------------------------------------- driving the steps

(defn- drive
  "Runs a step function against a store of the model. Gives {:result the
  last thing :store the store :snapshots the store after each accepted act
  :acts the acts sent}."
  [store step-fn]
  (let [st (atom store)
        snaps (atom [])
        sent (atom [])
        result (steps/drive step-fn
                            (fn [q] (let [[s a] (model/read-query @st q)] (reset! st s) a))
                            (fn [a] (let [[s o] (model/send-act @st a)]
                                      (reset! st s)
                                      (swap! sent conj a)
                                      (when (steps/accepted? o) (swap! snaps conj s))
                                      o)))]
    {:result (dissoc result :known) :store @st :snapshots @snaps :acts @sent}))

(defn- value
  "The value at a place of the model's store, or nil."
  [store layer e k]
  (get-in store [:rows [layer e k] :value]))

(defn- persons
  "The ids of the persons in the model's store."
  [store]
  (set (for [[[l e k] r] (:rows store) :when (and (= ids/identities l) (= :identity/kind k) (= :person (:value r)))] e)))

(defn- made-layers
  "The layers made in the model's store."
  [store]
  (set (for [[[l e k] _] (:rows store) :when (and (= l e) (= :softland.store/depot k))] l)))

(defn- started
  "A store of the model on which the runtime has started, with Sid's
  invitation recorded. Gives {:store :code :session :invitation}."
  [& opts]
  (let [session (random-uuid)
        {:keys [store result]} (drive (apply model/empty-store opts) #(start/start-step {:system-session session} %))
        inv (random-uuid)
        made (sign-in/invitation {:invitation inv :secret (random-bytes 32)
                                  :gives [{:layer ids/base :allows :see-and-act}]
                                  :made-by ids/system :system-session session})
        [store outcome] (model/send-act store (get-in made [:ok :act]))]
    (assert (= :done (:do result)) (pr-str result))
    (assert (steps/accepted? outcome) (pr-str outcome))
    {:store store :code (get-in made [:ok :code]) :session session :invitation inv}))

(defn- account
  "A Google account's claims, as the check gives them, with the kind."
  [sub]
  {:kind :google :sub sub :email (str sub "@example.com") :email-verified true :name (str "Person " sub)})

(defn- sign-in
  "Drives a sign-in on a store. Gives what `drive` gives."
  [store system-session the-account code]
  (let [input {:account the-account :code code :fresh (fresh) :system-session system-session}]
    (drive store #(sign-in/sign-in-step input %))))

;; ------------------------------------------------------------------- start

(deftest the-start
  (let [session (random-uuid)
        {:keys [store result acts]} (drive (model/empty-store) #(start/start-step {:system-session session} %))]
    (shown "start from nothing:" (select-keys result [:do :session :rounds]) "acts sent:" (count acts))
    (holds "the start from nothing is done, and every act it sent was accepted and in the act's form, every read in the query's"
           (and (= {:do :done :session session} (select-keys result [:do :session]))
                (empty? (:form-faults store))))
    (holds "the base is made by the system, microbatch, and set to be seen by everyone"
           (and (= :microbatch (value store :base :base :softland.store/depot))
                (= [:system :base :base] (value store :base :system :softland.store/permission))
                (= :everyone (value store :base :base :softland.store/who-may-see))))
    (holds "each system layer is made with the system at its top, members only, marking its one-write keys"
           (every? (fn [l] (and (= [:system l l] (value store l :system :softland.store/permission))
                                (= :members (value store l l :softland.store/who-may-see))
                                (= (records/one-write-keys l) (value store l l :softland.store/one-write-keys))))
                   ids/system-layers))
    (holds "the system has its identity and its name, and this start is a session in its record layer"
           (and (= :system (value store ids/identities :system :identity/kind))
                (= :system (value store ids/names :name/system :name/of))
                (= :program-run (value store ids/system-record session :session/kind))))
    (let [again (random-uuid)
          second-run (drive store #(start/start-step {:system-session again} %))]
      (shown "start again:" (select-keys (:result second-run) [:do :rounds]) "acts sent:" (count (:acts second-run)))
      (holds "a second start finds everything made and sends one act: its own session"
             (and (= :done (get-in second-run [:result :do]))
                  (= [ids/system-record] (mapv :layer (:acts second-run))))))))

;; ----------------------------------------------------------------- sign-in

(deftest a-first-sign-in
  (let [{:keys [store code session]} (started)
        sid (account "110169484474386276334")
        a (ids/account-id :google (:sub sid))
        run (sign-in store session sid code)
        {:keys [person token] :as result} (:result run)
        after (:store run)
        r (value after ids/identities person :identity/record-layer)
        o (value after ids/identities person :identity/own-layer)
        n (value after ids/identities person :identity/name)]
    (shown "first sign-in:" (dissoc result :token) "acts:" (count (:acts run)))
    (shown "the acts of a first sign-in, by layer:" (mapv #(let [l (:layer %)] (if (uuid? l) (cond (= l r) :record-layer (= l o) :own-layer :else l) l)) (:acts run)))
    (holds "a first sign-in with an invitation is done: a person, a sitting, a token, and this run made the link"
           (and (= :done (:do result)) (uuid? person) (true? (:first-sign-in? result))
                (= {:identity person :session (:session result)} (select-keys (token/parse-token token) [:identity :session]))))
    (holds "every act it sent was accepted and in the act's form, every read in the query's"
           (empty? (:form-faults after)))
    (holds "the account links to her, by its version 5 id, keeping the number as Google gave it and the invitation"
           (and (= person (value after ids/ways-in a :way-in/person))
                (= {:kind :google :sub (:sub sid)} (value after ids/ways-in a :way-in/account))))
    (holds "the invitation is used by her and this account"
           (= {:person person :account a} (some (fn [[[l _ k] row]] (when (and (= ids/invitations l) (= :invitation/used k)) (:value row))) (:rows after))))
    (holds "her identity holds her kind, a provisional name claimed in names, her two layers and her invitation"
           (and (= :person (value after ids/identities person :identity/kind))
                (str/starts-with? n "new-")
                (= person (value after ids/names (ids/name-entity n) :name/of))
                (uuid? (value after ids/identities person :identity/invited-by))))
    (holds "her own layer is made with her first permission and no right of the system's"
           (and (= [person o o] (value after o person :softland.store/permission))
                (nil? (value after o :system :softland.store/permission))))
    (holds "her record layer is made by the system, gives her :see, and records the way in as Google gave it"
           (and (= [:system r r] (value after r :system :softland.store/permission))
                (= [person r r [:system r r] :see] (value after r person :softland.store/permission))
                (= (dissoc sid :kind) (dissoc (value after r a :way-in/account) :kind))))
    (holds "her invitation's right is given beneath the system in the base"
           (= [person :base :base [:system :base :base] :see-and-act] (value after :base person :softland.store/permission)))
    (holds "the sitting is in her record layer with the token's hash, and the token matches it"
           (and (= person (value after r (:session result) :session/of))
                (token/matches? token (value after r (:session result) :session/token-hash))))
    (holds "no token and no code is in the store, only their hashes"
           (let [all (pr-str (:rows after))]
             (and (not (str/includes? all (last (str/split token #"\."))))
                  (not (str/includes? all (last (str/split code #"\.")))))))
    (holds "one person, and two layers more than the start made"
           (and (= #{person} (persons after)) (= 9 (count (made-layers after)))))

    ;; Run again from the state after each act of the first run: a crash at
    ;; any point, and a new process with fresh ids.
    (let [snaps (into [store] (:snapshots run))
          reruns (vec (for [[i snap] (map-indexed vector snaps)]
                        (let [again (sign-in snap session sid code)]
                          {:at i :again again
                           :without-code (when (value snap ids/ways-in a :way-in/person) (sign-in snap session sid nil))})))]
      (shown "run again from" (count snaps) "states; persons after each:" (mapv #(count (persons (get-in % [:again :store]))) reruns))
      (holds "run again from the state after each act of a first sign-in, each run is done"
             (every? #(= :done (get-in % [:again :result :do])) reruns))
      (holds "each run again ends with one person, her, and nine layers: no second person and no second layer"
             (every? (fn [{:keys [at again]}]
                       (let [s (:store again)]
                         (and (= 1 (count (persons s)))
                              (or (zero? at) (= #{person} (persons s)))
                              (= 9 (count (made-layers s))))))
                     reruns))
      (holds "from each state that holds the link, a run with no code finishes too"
             (every? (fn [{:keys [without-code]}]
                       (or (nil? without-code)
                           (and (= :done (get-in without-code [:result :do]))
                                (= #{person} (persons (:store without-code)))
                                (= 9 (count (made-layers (:store without-code)))))))
                     reruns))
      (holds "no run again sent an act out of the act's form or a read out of the query's"
             (every? #(empty? (:form-faults (get-in % [:again :store]))) reruns)))

    (let [later (sign-in after session sid nil)]
      (shown "later sign-in:" (dissoc (:result later) :token) "acts:" (mapv :layer (:acts later)))
      (holds "a later sign-in needs no code, makes no link and no layer, and gives a new sitting to the same person"
             (and (= :done (get-in later [:result :do]))
                  (false? (get-in later [:result :first-sign-in?]))
                  (= person (get-in later [:result :person]))
                  (not= (:session result) (get-in later [:result :session]))
                  (= [r] (mapv :layer (:acts later)))
                  (= 9 (count (made-layers (:store later)))))))

    (let [ended (let [[s o'] (model/send-act after {:version 1 :who :system :layer :base :permission [:system :base :base]
                                                    :session session :facts [{:e person :k :softland.store/permission}]})]
                  (assert (steps/accepted? o') (pr-str o'))
                  s)
          later (sign-in ended session sid nil)]
      (holds "a right that was ended is not given again by a later sign-in"
             (and (= :done (get-in later [:result :do]))
                  (= :nothing-here (get-in (:store later) [:rows [:base person :softland.store/permission] :holds])))))))

(deftest two-at-once
  (let [{:keys [store code session]} (started)
        sid (account "200")
        in-a {:account sid :code code :fresh (fresh) :system-session session}
        in-b {:account sid :code code :fresh (fresh) :system-session session}
        ;; One step of each run in turn, on one store.
        out (loop [store store
                   ka steps/nothing-known kb steps/nothing-known
                   da nil db nil
                   turn 0]
              (if (or (and da db) (> turn 400))
                {:store store :a da :b db}
                (let [a? (and (nil? da) (or (even? turn) db))
                      [input known] (if a? [in-a ka] [in-b kb])
                      step (sign-in/sign-in-step input known)
                      [store known done]
                      (case (:do step)
                        :read (let [[s answers] (reduce (fn [[s m] q] (let [[s2 ans] (model/read-query s q)] [s2 (assoc m q ans)]))
                                                        [store {}] (:reads step))]
                                [s (steps/with-answers known answers) nil])
                        :act (let [[s o] (model/send-act store (:act step))]
                               [s (steps/with-outcome known (:act step) o) nil])
                        [store known step])]
                  (if a?
                    (recur store known kb (or da done) db (inc turn))
                    (recur store ka known da (or db done) (inc turn))))))]
    (shown "two runs of one account at once:" (:do (:a out)) (:do (:b out)) "persons:" (count (persons (:store out))))
    (holds "two first sign-ins of one account, a step of each in turn, are both done as one person with nine layers"
           (and (= :done (:do (:a out)) (:do (:b out)))
                (= (:person (:a out)) (:person (:b out)))
                (= 1 (count (persons (:store out))))
                (= 9 (count (made-layers (:store out))))))))

(deftest refusals-and-links
  (let [{:keys [store code session]} (started)
        sid (account "300")
        first-run (sign-in store session sid code)
        after (:store first-run)
        person (get-in first-run [:result :person])]
    (let [stranger (sign-in after session (account "999") nil)]
      (holds "a stranger with no invitation is refused :no-invitation, and sends no act"
             (and (= :no-invitation (get-in stranger [:result :word])) (empty? (:acts stranger)))))
    (let [wrong (sign-in after session (account "998") (str (subs code 0 (- (count code) 43)) (apply str (repeat 42 "A")) "A"))]
      (holds "a code with the right invitation and a wrong secret is refused :invitation-not-valid"
             (= :invitation-not-valid (get-in wrong [:result :word]))))
    (let [other (sign-in after session (account "301") code)]
      (holds "an invitation used by another account is refused :invitation-used, and sends no act"
             (and (= :invitation-used (get-in other [:result :word])) (empty? (:acts other)))))
    (let [second-account (account "302")
          b (ids/account-id :google "302")
          r (value after ids/identities person :identity/record-layer)
          linked (drive after #(sign-in/link-account-step {:actor {:id person :kind :person :session (random-uuid)}
                                                          :account second-account :system-session session} %))
          again (sign-in (:store linked) session second-account nil)]
      (shown "a second account linked:" (:result linked))
      (holds "a second account is linked while she is signed in, and its way in recorded in her record layer"
             (and (= {:do :done :person person :account b} (select-keys (:result linked) [:do :person :account]))
                  (= person (value (:store linked) ids/ways-in b :way-in/person))
                  (= "302@example.com" (:email (value (:store linked) r b :way-in/account)))))
      (holds "she then signs in with the second account, with no code, as the same person"
             (and (= :done (get-in again [:result :do])) (= person (get-in again [:result :person]))
                  (= 1 (count (persons (:store again)))))))
    (let [{store2 :store code2 :code session2 :session} (started)
          one (sign-in store2 session2 (account "400") code2)
          inv2 (random-uuid)
          made (sign-in/invitation {:invitation inv2 :secret (random-bytes 32) :gives []
                                    :made-by (get-in one [:result :person]) :system-session session2})
          [s2 _] (model/send-act (:store one) (get-in made [:ok :act]))
          two (sign-in s2 session2 (account "401") (get-in made [:ok :code]))
          q (get-in two [:result :person])
          taken (drive (:store two) #(sign-in/link-account-step {:actor {:id q :kind :person :session (random-uuid)}
                                                                :account (account "400") :system-session session2} %))]
      (holds "a second invitation with no rights brings in a second person"
             (and (= :done (get-in two [:result :do])) (= 2 (count (persons (:store two))))))
      (holds "linking an account that is linked to another person is refused :account-linked-to-another"
             (= :account-linked-to-another (get-in taken [:result :word])))
      (holds "an agent cannot link an account: :only-a-person-links-an-account"
             (= :only-a-person-links-an-account
                (:word (sign-in/link-account-step {:actor {:id q :kind :agent :session (random-uuid)}
                                                   :account (account "402") :system-session session2}
                                                  steps/nothing-known)))))))

(deftest a-taken-name
  (let [{:keys [store code session]} (started)
        f (fresh)
        ;; Another identity holds the name this run will take.
        [store _] (model/send-act store {:version 1 :who :system :layer ids/names :permission [:system ids/names ids/names]
                                         :session session
                                         :facts [{:e (ids/name-entity (:name f)) :k :name/of :v (random-uuid)}]})
        input {:account (account "500") :code code :fresh f :system-session session}
        first-run (drive store #(sign-in/sign-in-step input %))
        second-run (sign-in (:store first-run) session (account "500") code)
        p (get-in second-run [:result :person])
        n (value (:store second-run) ids/identities p :identity/name)]
    (shown "a taken provisional name:" (:result first-run) "then:" (:do (:result second-run)) n)
    (holds "a provisional name another holds stops the run with :provisional-name-taken"
           (= :provisional-name-taken (get-in first-run [:result :word])))
    (holds "the next run renames her to its fresh name, claims it, and is done as the same person"
           (and (= :done (get-in second-run [:result :do]))
                (not= (:name f) n)
                (= p (value (:store second-run) ids/names (ids/name-entity n) :name/of))
                (= 1 (count (persons (:store second-run))))))))

(deftest a-base-that-lags
  (let [{:keys [store code session]} (started :lag-base? true)
        run (sign-in store session (account "600") code)
        p (get-in run [:result :person])]
    (shown "with reads of the base one act behind:" (:do (:result run)) "acts in the base:" (count (filter #(= :base (:layer %)) (:acts run))))
    (holds "with reads of the base one act behind, a first sign-in is done and gives the base right once"
           (and (= :done (get-in run [:result :do]))
                (= 1 (count (filter #(= :base (:layer %)) (:acts run))))
                (= [p :base :base [:system :base :base] :see-and-act] (value (:store run) :base p :softland.store/permission))))))

(deftest inputs-not-whole
  (holds "a sign-in whose fresh ids or account are not whole is refused :input-not-whole before any read"
         (every? #(= :input-not-whole (:word (sign-in/sign-in-step % steps/nothing-known)))
                 [{:account (account "1") :fresh (dissoc (fresh) :person) :system-session (random-uuid)}
                  {:account (assoc (account "1") :sub "") :fresh (fresh) :system-session (random-uuid)}
                  {:account (account "1") :fresh (fresh)}]))
  (holds "a sign-in before the start is refused :runtime-not-started"
         (= :runtime-not-started
            (get-in (sign-in (model/empty-store) (random-uuid) (account "1") nil) [:result :word]))))

;; -------------------------------------------------------------------- main

(defn test-ns-hook
  "Runs the groups in the order of the namespace docstring."
  []
  (the-ids)
  (tokens-and-codes)
  (googles-id-token)
  (the-records)
  (the-binding)
  (the-start)
  (a-first-sign-in)
  (two-at-once)
  (refusals-and-links)
  (a-taken-name)
  (a-base-that-lags)
  (inputs-not-whole))

(defn -main
  "Runs the proof and exits: status 0 when each check holds."
  [& _]
  (let [{:keys [fail error] :as result} (run-tests 'softland.runtime.pure-proof)]
    (println)
    (println "THE PROOF, IN ONE PLACE")
    (run! println @shown-lines)
    (println (select-keys result [:test :pass :fail :error]))
    (shutdown-agents)
    (System/exit (if (zero? (+ fail error)) 0 1))))
