(ns softland.runtime.service-proof
  "The proof of the runtime as a service, on Rama's in-process cluster with
  the store's module `Store`. One command, from the repository root, under
  the lock file that the store's cluster proofs share:

    flock src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock \\
      clojure -M:runtime-module -m softland.runtime.service-proof

  It prints each check's sentence after PASSED or FAILED, prints what it saw
  on lines that start with SHOWN, prints those lines again together at the
  end, and exits with a status that is not zero when a check fails.

  A stand-in for Google. The proof makes a key pair of its own (Nimbus's
  `RSAKeyGenerator`, with a kid), signs ID tokens with it the way Google
  signs them (RS256; `iss`, `aud`, `sub`, `exp`, the email and the name),
  and opens the service with `:google-keys` giving its public key set. No
  network is called.

  What it checks, in order, each on the store's real gates and read path
  through the service's calls: the start, and the start again, with the
  rules' defaults; an invitation, and a stranger without one refused; a
  first sign-in and a later one, and a token with another secret refused; a
  second account linked, and one linked by Sid; a client that claims
  another's name refused; an act from someone without a right sent to the
  door and refused by the gate; a name claimed and changed, a taken name
  refused, a freed name waiting and then taken once its wait has passed
  (the proof opens with a wait of two seconds); a read by someone not
  signed in, once and kept live; a layer set to anyone signed in; an agent
  made, its token made and used, its run recorded, its act in its person's
  layer named as itself in its run, a key its right does not allow
  refused; a right ended and the agent's next act refused; the agent's
  token ended, its next use refused at the runtime, and a wrong secret
  told nothing of the end; a token asked for and approved, with the
  approval recorded beside its session; a flood of requests for one agent,
  which drops its oldest; an agent's give and setting refused under a
  right with no key list and given when its list names the key, and a
  right given to an agent with `:allows` left out; a copy of an agent; a
  blocked sender stopped (its acts, a read, a request on its behalf, and a
  sign-in) and counted; a block of some keys in one layer, which stops
  those keys there and nothing else; a person's block reaching the program
  she made, the program's acts counting toward her limit, and a limit of
  bytes; a program held to a limit on its own id; a sitting past its end,
  by a clock that the proof sets a week ahead and back; a sign-out; a first
  sign-in cut after each of its acts and finished by the next sign-in, with
  no second person; and two first sign-ins of one account at the same
  moment, on two threads released together by a latch, each with its own
  fresh ids, ten times with a new account each time: one person, one own
  layer, one record layer, both ending with that person. For the race it
  shows each run's refused acts with their words, so that a pass shows
  that the two runs met.

  State. The proof starts one in-process cluster with four tasks and two
  workers, launches the module `Store` in it, opens one service, and closes
  the cluster at the end; the cluster's files are temporary and are Rama's.
  It owns three atoms: the lines it showed; `world`, what later checks
  build on; and how far the service's clock is set ahead. It reads the JVM's clock, through the service, and for the
  expiry of its ID tokens."
  (:require [clojure.test :refer [is run-tests]]
            [com.rpl.rama :as rama]
            [com.rpl.rama.test :as rtest]
            [softland.runtime.ids :as ids]
            [softland.runtime.service :as service]
            [softland.runtime.sign-in :as sign-in]
            [softland.runtime.steps :as steps]
            [softland.runtime.token :as token]
            [softland.store.act :as act]
            [softland.store.module :as module])
  (:import [com.nimbusds.jose JOSEObjectType JWSAlgorithm JWSHeader$Builder]
           [java.util.concurrent CountDownLatch]
           [com.nimbusds.jose.crypto RSASSASigner]
           [com.nimbusds.jose.jwk JWKSet KeyUse RSAKey]
           [com.nimbusds.jose.jwk.gen RSAKeyGenerator]
           [com.nimbusds.jwt JWTClaimsSet$Builder SignedJWT]
           [java.util Date]))

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

;; ------------------------------------------------------ the stand-in for Google

(def ^:private client-id "proof-client.apps.googleusercontent.com")

(def ^:private ^RSAKey google-key
  "The proof's key pair, standing in for Google's, with its kid."
  (-> (RSAKeyGenerator. (int 2048)) (.keyID "proof-kid") (.keyUse KeyUse/SIGNATURE)
      (.algorithm JWSAlgorithm/RS256) (.generate)))

(def ^:private google-keys
  "The public key set that the service is given in place of Google's."
  (JWKSet. (.toPublicJWK google-key)))

(defn- id-token
  "An ID token for an account's number, signed as Google signs one, good
  for an hour from now."
  [sub]
  (let [now (System/currentTimeMillis)
        b (doto (JWTClaimsSet$Builder.)
            (.issuer "https://accounts.google.com")
            (.audience ^String client-id)
            (.subject sub)
            (.expirationTime (Date. (+ now 3600000)))
            (.issueTime (Date. now))
            (.claim "email" (str sub "@example.com"))
            (.claim "email_verified" true)
            (.claim "name" (str "Person " sub)))
        jwt (SignedJWT. (.build (doto (JWSHeader$Builder. JWSAlgorithm/RS256)
                                  (.keyID "proof-kid")
                                  (.type JOSEObjectType/JWT)))
                        (.build b))]
    (.sign jwt (RSASSASigner. google-key))
    (.serialize jwt)))

;; ------------------------------------------------------------------ the world

(defonce ^{:private true
           :doc "What later checks build on: the service, and the actors, layers
  and ids that earlier checks made."}
  world (atom {}))

(defn- svc [] (:svc @world))

(defn- w [k] (get @world k))

(defn- remember! [& kvs] (apply swap! world assoc kvs))

(def ^:private proof-rules
  "The rules' defaults that the proof opens the service with, over the
  service's own: a freed name waits two seconds, so that it is seen taken
  again; two blocked attempts are kept whole."
  {:name-wait-ms 2000 :keep-first 2})

(defonce ^{:private true
           :doc "How far the service's clock is set ahead of the JVM's, in
  milliseconds: moved only to show a sitting past its end, and set back."}
  clock-offset (atom 0))

;; ------------------------------------------------------------------ helpers

(defn- sys-read
  "A read as the system. Gives the answer, or the refusal."
  [query]
  (let [r (service/read-as-system (svc) query)]
    (or (:ok r) r)))

(defn- sys-place
  "The place-answer of one place in one layer, read as the system."
  [layer e k]
  (:place (sys-read (steps/place-query ids/system layer e k))))

(defn- sys-value
  "The value at one place in one layer, read as the system, or nil."
  [layer e k]
  (let [pa (sys-place layer e k)] (when (= :value (:holds pa)) (:value pa))))

(defn- identity-record
  "An identity's record in `identities`, read as the system."
  [id]
  (steps/record-of (sys-read (steps/entity-query ids/system ids/identities id false)) id))

(defn- note
  "An act of one note: in a layer, under a permission, on an entity."
  [layer permission e text]
  {:version act/version :layer layer :permission permission
   :facts [{:e e :k :note/text :v text}]})

(defn- answer-word
  "The answer of `act!`'s result: :accepted, [:refused reason], or the
  refusal of the call."
  [r]
  (if-let [a (get-in r [:ok :answer])]
    (if (= :refused (:answer a)) [:refused (:reason a)] (:answer a))
    [:runtime (:refuse r)]))

(defn- signed-in
  "Signs in with an account's number and a code; gives what `sign-in!`
  gave, and its actor when it gave {:ok …}."
  [sub code]
  (let [r (service/sign-in! (svc) (id-token sub) code)
        actor (when-let [t (get-in r [:ok :token])] (:ok (service/actor-of (svc) t)))]
    (assoc r :actor actor)))

;; ------------------------------------------------------------------ the checks

(defn- made?
  "Whether a layer is made, read as the system: a layer that is not made is
  left out as `:not-made` for any asker; a made layer is read, or left out
  as `:may-not-see` when the system holds no right there (a person's own
  layer)."
  [layer]
  (let [a (sys-read (steps/place-query ids/system layer ids/system :softland.store/permission))]
    (not= :not-made (steps/left-out-why a layer))))

(defn the-start []
  (let [s (svc)
        first-start (service/start! s)
        again (service/start! s)]
    (shown "start:" first-start "then again:" again)
    (shown "base seen by:" (sys-value ids/base ids/base :softland.store/who-may-see)
           "| limits:" (mapv #(sys-value ids/rules % :rule/acts-per-minute) [:person :agent :program])
           "| name wait ms:" (sys-value ids/rules :names :rule/name-wait-ms)
           "| keep first:" (sys-value ids/rules :blocked :rule/keep-first))
    (holds "the start makes the base and the system's layers, and runs again with nothing missing"
           (and (:ok first-start) (:ok again)
                (= :everyone (sys-value ids/base ids/base :softland.store/who-may-see))
                (every? #(= [ids/system % %] (sys-value % ids/system :softland.store/permission)) ids/system-layers)))
    (shown "bytes a minute:" (mapv #(sys-value ids/rules % :rule/bytes-per-minute) [:person :agent :program])
           "| a sitting, ms:" (sys-value ids/rules :person :rule/sitting-ms))
    (holds "the start writes the rules' defaults: a limit of acts and of bytes for each kind of actor, a program's the same as a person's, the name wait, how many attempts are kept, how long a sitting lasts"
           (and (= [600 600 600] (mapv #(sys-value ids/rules % :rule/acts-per-minute) [:person :agent :program]))
                (= (vec (repeat 3 (* 64 1024 1024))) (mapv #(sys-value ids/rules % :rule/bytes-per-minute) [:person :agent :program]))
                (= (* 7 24 60 60 1000) (sys-value ids/rules :person :rule/sitting-ms))
                (= 2000 (sys-value ids/rules :names :rule/name-wait-ms))
                (= 2 (sys-value ids/rules :blocked :rule/keep-first))))))

(defn an-invitation-and-a-stranger []
  (let [inv (service/invite! (svc) {:gives [{:layer ids/base :allows :see-and-act}]})
        stranger (signed-in "stranger-1" nil)
        bad-code (signed-in "stranger-2" "sli.not-a-code")
        other-code (service/invite! (svc) {:gives []})]
    (remember! :sid-code (get-in inv [:ok :code]) :ana-code (get-in other-code [:ok :code]))
    (shown "invitation:" (some? (get-in inv [:ok :code])) "| stranger without a code:" (:refuse stranger)
           "| stranger with a code of no form:" (:refuse bad-code))
    (holds "an invitation is made, and a stranger without one is refused"
           (and (string? (get-in inv [:ok :code])) (string? (get-in other-code [:ok :code]))
                (= :no-invitation (:refuse stranger)) (= :invitation-not-valid (:refuse bad-code))))))

(defn a-first-sign-in-and-a-later-one []
  (let [first-in (signed-in "sid-1" (w :sid-code))
        sid (:actor first-in)
        later (signed-in "sid-1" nil)
        rec (identity-record (:id sid))
        the-token (get-in first-in [:ok :token])
        forged (str (subs the-token 0 (- (count the-token) 4)) (if (= "AAAA" (subs the-token (- (count the-token) 4))) "BBBB" "AAAA"))
        forged-actor (service/actor-of (svc) forged)
        no-form (service/actor-of (svc) "sl1.nothing")
        own (service/own-identity (svc) sid)]
    (remember! :sid sid :sid-later (:actor later) :sid-token (get-in first-in [:ok :token])
               :sid-later-token (get-in later [:ok :token])
               :sid-own (:identity/own-layer rec) :sid-record (:identity/record-layer rec))
    (shown "first sign-in:" (dissoc (:ok first-in) :token) "| later:" (dissoc (:ok later) :token))
    (shown "Sid's identity:" rec "| his right in the base:" (sys-value ids/base (:id sid) :softland.store/permission))
    (holds "a first sign-in makes a person, her layers and her sitting; a later one finds her and opens another sitting"
           (and (true? (get-in first-in [:ok :first-sign-in?])) (false? (get-in later [:ok :first-sign-in?]))
                (= (:id sid) (get-in later [:ok :person])) (not= (:session sid) (:session (:actor later)))
                (= :person (:kind sid)) (= :person (:identity/kind rec))
                (some? (:actor later))))
    (shown "a token with another secret:" (:refuse forged-actor) "| a token of no form:" (:refuse no-form))
    (holds "a sitting's token names its person, and a token with another secret, or of no form, names no one"
           (and (= :token-not-valid (:refuse forged-actor)) (= :token-not-valid (:refuse no-form))
                (= rec (:ok own))))
    (holds "the invitation's right is given beneath the system in the base"
           (= [(:id sid) ids/base ids/base [ids/system ids/base ids/base] :see-and-act]
              (sys-value ids/base (:id sid) :softland.store/permission)))))

(defn a-second-account-linked []
  (let [sid (w :sid)
        linked (service/link-account! (svc) sid (id-token "sid-2"))
        by-second (signed-in "sid-2" nil)
        ana-in (signed-in "ana-1" (w :ana-code))
        ana (:actor ana-in)
        to-ana (service/link-account! (svc) ana (id-token "sid-2"))
        by-admin (service/link-by-admin! (svc) {:kind :google :sub "ana-new"} (:id ana))
        by-new (signed-in "ana-new" nil)
        ana-rec (identity-record (:id ana))]
    (remember! :ana ana :ana-own (:identity/own-layer ana-rec) :ana-record (:identity/record-layer ana-rec))
    (shown "linked:" linked "| sign-in by the second account:" (dissoc (:ok by-second) :token))
    (shown "the same account linked to Ana:" (:refuse to-ana) "| linked by Sid's command line:" by-admin
           "| sign-in by that account:" (get-in by-new [:ok :person]))
    (holds "a second account is linked while signed in, and signs in as the same person"
           (and (= (:id sid) (get-in linked [:ok :person])) (= (:id sid) (get-in by-second [:ok :person]))
                (false? (get-in by-second [:ok :first-sign-in?]))))
    (holds "an account linked to one person is refused for another"
           (and (some? ana) (= :account-linked-to-another (:refuse to-ana))))
    (holds "Sid's command line links an account to a person, who then signs in by it"
           (and (:ok by-admin) (= (:id ana) (get-in by-new [:ok :person]))))))

(defn a-client-that-claims-another-name []
  (let [{:keys [sid ana]} @world
        e (random-uuid)
        who (service/act! (svc) ana (assoc (note ids/base [(:id ana) ids/base ids/base] e "x") :who (:id sid)))
        perm (service/act! (svc) ana (note ids/base [(:id sid) ids/base ids/base [ids/system ids/base ids/base] :see-and-act] e "x"))
        session (service/act! (svc) ana (assoc (note ids/base [(:id ana) ids/base ids/base] e "x") :session (:session sid)))
        asker (service/read! (svc) ana {:asks [:place e :note/text] :asker (:id sid) :layers [ids/base]})]
    (shown "Ana as Sid:" (:refuse who) "| under his permission:" (:refuse perm) "| in his sitting:" (:refuse session)
           "| asking as him:" (:refuse asker))
    (holds "a client that claims another's name, sitting or right is refused at the runtime"
           (and (= :who-not-the-actor (:refuse who)) (= :permission-not-the-actors (:refuse perm))
                (= :session-not-the-actors (:refuse session)) (= :asker-not-the-actor (:refuse asker))))))

(defn an-act-without-a-right []
  (let [ana (w :ana)
        e (random-uuid)
        r (service/act! (svc) ana (note ids/base [(:id ana) ids/base ids/base] e "mine"))]
    (shown "Ana's act in the base, where she has no right:" (answer-word r) "| act-id:" (get-in r [:ok :act-id]))
    (holds "an act from someone without a right goes to the door, and the gate logs and refuses it"
           (and (some? (get-in r [:ok :act-id])) (= :refused (get-in r [:ok :answer :answer]))
                (nil? (sys-value ids/base e :note/text))))))

(defn names []
  (let [{:keys [sid ana]} @world
        s (service/rename! (svc) sid "sid")
        s2 (service/rename! (svc) sid "siddharth")
        taken (service/rename! (svc) ana "siddharth")
        waits (service/rename! (svc) ana "sid")
        ana-named (service/rename! (svc) ana "ana")
        bad (service/rename! (svc) ana "Ana!")]
    (shown "Sid takes sid:" s "then siddharth:" s2)
    (shown "names now: sid ->" (sys-place ids/names :name/sid :name/of)
           "| siddharth ->" (sys-value ids/names :name/siddharth :name/of)
           "| Sid's identity name:" (:identity/name (identity-record (:id sid))))
    (shown "Ana takes siddharth:" (:refuse taken) "| takes the freed sid:" waits "| takes ana:" ana-named
           "| a name of no rule:" (:refuse bad))
    (holds "a name is claimed and changed: the new one points at her, the old one is freed, her identity says the new"
           (and (:ok s) (:ok s2)
                (= (:id sid) (sys-value ids/names :name/siddharth :name/of))
                (= :nothing-here (:holds (sys-place ids/names :name/sid :name/of)))
                (= "siddharth" (:identity/name (identity-record (:id sid))))))
    (holds "a second claim of a taken name is refused, and a freed name waits before another takes it"
           (and (= :name-taken (:refuse taken)) (= :name-waits (:refuse waits)) (some? (:until waits))
                (:ok ana-named) (= :name-not-allowed (:refuse bad))))))

(defn a-freed-name-taken-again []
  (let [{:keys [sid ana]} @world
        freed (service/rename! (svc) sid "sid-again")
        at-once (service/rename! (svc) ana "siddharth")
        _ (Thread/sleep (max 0 (+ 200 (- (or (:until at-once) 0) (System/currentTimeMillis)))))
        after-wait (service/rename! (svc) ana "siddharth")]
    (shown "Sid moves to sid-again:" (:ok freed) "| Ana takes siddharth at once:" (select-keys at-once [:refuse :until])
           "| after the wait:" after-wait "| siddharth ->" (sys-value ids/names :name/siddharth :name/of))
    (holds "a freed name is refused to another identity at once, and taken by her once its wait has passed"
           (and (:ok freed) (= :name-waits (:refuse at-once)) (:ok after-wait)
                (= (:id ana) (sys-value ids/names :name/siddharth :name/of))
                (= "siddharth" (:identity/name (identity-record (:id ana))))))))

(defn reads-not-signed-in []
  (let [{:keys [sid ana sid-own ana-own]} @world
        e (random-uuid)
        in-base (service/act! (svc) sid (note ids/base [(:id sid) ids/base ids/base [ids/system ids/base ids/base] :see-and-act] e "in the base"))
        in-own (service/act! (svc) sid (note sid-own [(:id sid) sid-own sid-own] e "in his own layer"))
        in-ana (service/act! (svc) ana (note ana-own [(:id ana) ana-own ana-own] e "in hers"))
        q {:asks [:place e :note/text] :layers [sid-own ana-own ids/base]}
        nobody (service/read! (svc) nil q)
        as-sid (service/read! (svc) sid q)
        told (atom [])
        live (service/read-live! (svc) nil q #(swap! told conj %))
        left (service/leave! (svc) (get-in live [:ok :leave-with]))
        left-again (service/leave! (svc) (get-in live [:ok :leave-with]))]
    (remember! :note e)
    (shown "kept live, not signed in:" (get-in live [:ok :now :place :value]) "| told first:" (count @told)
           "| left:" left "| again:" left-again)
    (holds "a read kept live by someone not signed in is bound the same way, and is left once"
           (and (= (get-in nobody [:ok :place]) (get-in live [:ok :now :place]))
                (= [{:anew (get-in live [:ok :now])}] (take 1 @told))
                (= {:ok :left} left) (= :not-open (:refuse left-again))))
    (shown "acts:" (answer-word in-base) (answer-word in-own) (answer-word in-ana))
    (shown "not signed in:" (get-in nobody [:ok :place :value]) "| left out:" (get-in nobody [:ok :left-out])
           "| Sid:" (get-in as-sid [:ok :place :value]))
    (holds "a read by someone not signed in sees the layers set to everyone, the base among them, and no members layer"
           (and (= :accepted (answer-word in-base)) (= :accepted (answer-word in-own)) (= :accepted (answer-word in-ana))
                (= "in the base" (get-in nobody [:ok :place :value]))
                (= #{{:layer sid-own :why :may-not-see} {:layer ana-own :why :may-not-see}}
                   (set (get-in nobody [:ok :left-out])))
                (= "in his own layer" (get-in as-sid [:ok :place :value]))))))

(defn a-signed-in-layer []
  (let [{:keys [sid ana sid-own ana-own note]} @world
        set-it (service/act! (svc) sid {:version act/version :layer sid-own :permission [(:id sid) sid-own sid-own]
                                        :facts [{:e sid-own :k :softland.store/who-may-see :v :signed-in}]})
        q {:asks [:place note :note/text] :layers [sid-own]}
        nobody (service/read! (svc) nil q)
        as-ana (service/read! (svc) ana q)
        sid-in-ana (service/read! (svc) sid {:asks [:place note :note/text] :layers [ana-own]})]
    (shown "Sid sets his layer to anyone signed in:" (answer-word set-it))
    (shown "not signed in:" (get-in nobody [:ok :left-out]) "| Ana:" (get-in as-ana [:ok :place :value])
           "| Sid in Ana's members layer:" (get-in sid-in-ana [:ok :left-out]))
    (holds "a layer set to anyone signed in is seen by a signed-in person and not by one who is not"
           (and (= :accepted (answer-word set-it))
                (= [{:layer sid-own :why :may-not-see}] (get-in nobody [:ok :left-out]))
                (= "in his own layer" (get-in as-ana [:ok :place :value]))
                (= [{:layer ana-own :why :may-not-see}] (get-in sid-in-ana [:ok :left-out]))))))

(defn an-agent []
  (let [{:keys [sid sid-own]} @world
        made (service/make-agent! (svc) sid {:name "scribe"
                                             :description {:instructions "Write notes for Sid."
                                                           :engine {:provider "anthropic" :model "a-model" :effort :high}}})
        agent-id (get-in made [:ok :id])
        rec (identity-record agent-id)
        token (service/make-token! (svc) sid agent-id)
        the-token (get-in token [:ok :token])
        agent (:ok (service/actor-of (svc) the-token))
        run (service/start-run! (svc) agent {:engine {:provider "anthropic" :model "a-model" :effort :high}
                                             :context {:given ["the note to write"]} :human-in-loop true
                                             :cause :sid-asked})
        run-id (get-in run [:ok :run])
        in-run (:ok (service/actor-of (svc) the-token run-id))
        no-run (service/act! (svc) agent (note sid-own [agent-id sid-own sid-own] (random-uuid) "x"))
        right (service/give-right! (svc) sid {:layer sid-own :to agent-id :allows #{:note/*}})
        p (get-in right [:ok :permission])
        e (random-uuid)
        wrote (service/act! (svc) in-run (note sid-own p e "written by the scribe"))
        back (service/read! (svc) in-run {:asks [:place e :note/text] :layers [sid-own] :with #{:writer :permission}})
        not-allowed (service/act! (svc) in-run {:version act/version :layer sid-own :permission p
                                                :facts [{:e e :k :other/thing :v 1}]})
        a-layer (service/act! (svc) in-run {:version act/version :layer (random-uuid) :permission [agent-id (random-uuid) (random-uuid)]
                                            :facts [{:e (random-uuid) :k :softland.store/depot :v :streaming}]})
        renamed (service/rename! (svc) in-run "scribe-2")
        run-record (steps/record-of (sys-read (steps/entity-query ids/system (:identity/record-layer rec) run-id false)) run-id)
        sid-sees-record (service/read! (svc) sid {:asks [:entity run-id] :layers [(:identity/record-layer rec)]})]
    (remember! :agent agent :agent-token the-token :agent-in-run in-run :agent-permission p :agent-id agent-id)
    (shown "agent made:" (:ok made) "| identity:" rec)
    (shown "token's actor:" agent "| run:" (:ok run) "| without a run:" (:refuse no-run))
    (shown "right given:" p "| act:" (answer-word wrote) "in session" (get-in wrote [:ok :session]))
    (shown "read back:" (select-keys (get-in back [:ok :place]) [:value :writer :permission]))
    (shown "a key not allowed:" (answer-word not-allowed) "| a layer made:" (:refuse a-layer) "| an agent renames:" (:refuse renamed))
    (shown "the run's record:" run-record)
    (holds "an agent is made with its name, its record layer and its description; its token is made and names it"
           (and (= :agent (:identity/kind rec)) (= (:id sid) (:identity/made-by rec)) (= "scribe" (:identity/name rec))
                (= agent-id (sys-value ids/names :name/scribe :name/of))
                (= :accepted (answer-word (get-in made [:ok :description])))
                (= {:id agent-id :kind :agent :session (get-in token [:ok :session])} agent)))
    (holds "the agent's run is recorded, with its description's version, engine, context and cause"
           (and (= (:session agent) (:run/session run-record)) (some? (:run/description run-record))
                (= {:provider "anthropic" :model "a-model" :effort :high} (:run/engine run-record))
                (true? (:run/human-in-loop run-record)) (= :sid-asked (:run/cause run-record))
                (= run-id (:run in-run)) (seq (get-in sid-sees-record [:ok :places]))
                (= :agent-act-without-a-run (:refuse no-run))))
    (holds "the agent's act in its person's layer is accepted, in its run, and reads back named as the agent beneath its person"
           (and (= :accepted (answer-word wrote)) (= run-id (get-in wrote [:ok :session]))
                (= agent-id (get-in back [:ok :place :writer]))
                (= [agent-id sid-own sid-own [(:id sid) sid-own sid-own] #{:note/*}] (get-in back [:ok :place :permission]))))
    (holds "a key that the agent's right does not allow is refused, and an agent makes no layer and changes no name"
           (and (= [:refused :key-not-allowed] (answer-word not-allowed))
                (= :may-not-make-a-layer (:refuse a-layer)) (= :only-a-person (:refuse renamed))))))

(defn a-right-ended []
  (let [{:keys [sid sid-own agent-in-run agent-permission agent-id]} @world
        ended (service/end-right! (svc) sid {:layer sid-own :of agent-id})
        after (service/act! (svc) agent-in-run (note sid-own agent-permission (random-uuid) "after the end"))]
    (shown "Sid ends the agent's right:" (answer-word ended) "| the agent's next act:" (answer-word after))
    (holds "a right is ended by its giver, and the agent's next act under it is refused by the gate"
           (and (= :accepted (answer-word ended)) (= :refused (first (answer-word after)))))))

(defn a-token-ended []
  (let [{:keys [sid agent agent-token agent-id]} @world
        ended (service/end-token! (svc) sid (:session agent) agent-id)
        again (service/actor-of (svc) agent-token)
        tail (subs agent-token (- (count agent-token) 4))
        wrong (str (subs agent-token 0 (- (count agent-token) 4)) (if (= "AAAA" tail) "BBBB" "AAAA"))
        wrong-secret (service/actor-of (svc) wrong)
        ana-ends (service/end-token! (svc) (w :ana) (:session agent) agent-id)]
    (shown "Sid ends the agent's token:" ended "| the token afterwards:" again "| with a wrong secret:" (:refuse wrong-secret)
           "| Ana ends it:" (:refuse ana-ends))
    (holds "an agent's token is ended by its person, and its next act is refused at the runtime"
           (and (false? (get-in ended [:ok :ended-before?])) (= :session-ended (:refuse again))
                (= :may-not-end-this-session (:refuse ana-ends))))
    (holds "an ended token with a wrong secret is answered :token-not-valid, so only its holder learns that it ended"
           (= :token-not-valid (:refuse wrong-secret)))))

(defn a-token-asked-for []
  (let [{:keys [sid ana agent-id]} @world
        asked (service/ask-token! (svc) agent-id)
        request (get-in asked [:ok :request])
        pending (service/pending-requests (svc) sid)
        ana-pending (service/pending-requests (svc) ana)
        too-soon (service/collect-token! (svc) request)
        ana-approves (service/approve-token! (svc) ana request)
        approved (service/approve-token! (svc) sid request)
        collected (service/collect-token! (svc) request)
        _ (remember! :scribe-token (get-in collected [:ok :token]))
        actor (service/actor-of (svc) (get-in collected [:ok :token]))
        again (service/collect-token! (svc) request)
        asked-fact (sys-value (:identity/record-layer (identity-record agent-id)) (get-in collected [:ok :session]) :session/asked)]
    (shown "asked:" (dissoc (:ok asked) :request) "| Sid sees:" (mapv #(select-keys % [:name :code]) (:ok pending))
           "| Ana sees:" (:ok ana-pending))
    (shown "collected too soon:" (:refuse too-soon) "| Ana approves:" (:refuse ana-approves) "| Sid approves:" (some? (:ok approved))
           "| collected:" (some? (get-in collected [:ok :token])) "| its actor:" (:ok actor) "| again:" (:refuse again))
    (shown "the approval beside its session:" (dissoc asked-fact :request))
    (holds "a token is asked for by an agent, approved by its person, collected once, and names the agent"
           (and (some? request) (= [(get-in asked [:ok :code])] (mapv :code (:ok pending))) (= [] (:ok ana-pending))
                (= :not-approved-yet (:refuse too-soon)) (= :not-the-maker (:refuse ana-approves))
                (:ok approved) (= agent-id (:id (:ok actor))) (= :request-not-known (:refuse again))))
    (holds "an approval is recorded beside the session it made: the request, its code and when it was asked"
           (and (= request (:request asked-fact)) (= (get-in asked [:ok :code]) (:code asked-fact))
                (int? (:asked-at asked-fact))))))

(defn token-requests-in-a-flood []
  (let [{:keys [sid agent-id]} @world
        asks (mapv (fn [_] (service/ask-token! (svc) agent-id)) (range 9))
        first-one (get-in (first asks) [:ok :request])
        pending (filter #(= agent-id (:identity %)) (:ok (service/pending-requests (svc) sid)))
        first-collect (service/collect-token! (svc) first-one)
        not-an-id (service/approve-token! (svc) sid "not-a-request")
        unknown (service/approve-token! (svc) sid (random-uuid))]
    (shown "nine asks:" (mapv #(some? (:ok %)) asks) "| pending for the agent:" (count pending)
           "| the first one:" (:refuse first-collect) "| approving no id:" (:refuse not-an-id) "| an unknown id:" (:refuse unknown))
    (holds "a ninth request for one agent drops its oldest instead of being refused, and an approval of an id that is not held is refused"
           (and (every? :ok asks) (= 8 (count pending))
                (not (some #(= first-one (:request %)) pending))
                (= :request-not-known (:refuse first-collect))
                (= :request-not-known (:refuse not-an-id)) (= :request-not-known (:refuse unknown))))))

(defn an-agents-store-keys []
  (let [{:keys [sid sid-own agent-id scribe-token]} @world
        bare (:ok (service/actor-of (svc) scribe-token))
        run (service/start-run! (svc) bare {:engine {:provider "anthropic" :model "a-model"} :context {:given []}
                                            :human-in-loop false :cause :proof})
        agent (assoc bare :run (get-in run [:ok :run]))
        stranger (random-uuid)
        give (fn [p] (service/act! (svc) agent {:version act/version :layer sid-own :permission p
                                                :facts [{:e stranger :k :softland.store/permission
                                                         :v [stranger sid-own sid-own p :see]}]}))
        setting (fn [p] (service/act! (svc) agent {:version act/version :layer sid-own :permission p
                                                   :facts [{:e sid-own :k :softland.store/who-may-see :v :everyone}]}))
        wide (service/give-right! (svc) sid {:layer sid-own :to agent-id :allows :see-and-act})
        p1 (get-in wide [:ok :permission])
        give-wide (give p1)
        set-wide (setting p1)
        _ (service/end-right! (svc) sid {:layer sid-own :of agent-id})
        named (service/give-right! (svc) sid {:layer sid-own :to agent-id :allows #{:note/* :softland.store/permission}})
        p2 (get-in named [:ok :permission])
        give-named (give p2)
        set-named (setting p2)
        _ (service/end-right! (svc) sid {:layer sid-own :of agent-id})
        left-out (service/give-right! (svc) sid {:layer sid-own :to agent-id})
        seen-by (sys-value sid-own sid-own :softland.store/who-may-see)]
    (shown "under :see-and-act, the agent gives:" (:refuse give-wide) "| sets a layer:" (:refuse set-wide))
    (shown "under a list that names :softland.store/permission, it gives:" (answer-word give-named)
           "| sets a layer:" (:refuse set-named) "| Sid's layer is still seen by:" seen-by)
    (shown "a right given to the agent with :allows left out:" (last (get-in left-out [:ok :permission])))
    (holds "an agent gives no right and changes no setting under a right with no key list, and gives one when its list names the key"
           (and (:ok run) (:ok wide) (= :store-key-not-allowed-for-an-agent (:refuse give-wide))
                (= :store-key-not-allowed-for-an-agent (:refuse set-wide))
                (= :accepted (answer-word give-named))
                (= :store-key-not-allowed-for-an-agent (:refuse set-named))
                (= :signed-in seen-by)))
    (holds "a right given to an agent with :allows left out allows every plain key and no store key"
           (= #{:*} (last (get-in left-out [:ok :permission]))))))

(defn a-copy []
  (let [{:keys [ana agent-id]} @world
        copied (service/copy-agent! (svc) ana agent-id {:name "scribe-copy"})
        rec (identity-record (get-in copied [:ok :id]))]
    (shown "Ana copies the scribe:" (dissoc (:ok copied) :description) "| description:" (answer-word (get-in copied [:ok :description]))
           "| identity:" rec)
    (holds "a copy of an agent is a new agent of its copier, with a link to the one it was made from"
           (and (= :agent (:identity/kind rec)) (= agent-id (:identity/copied-from rec)) (= (:id ana) (:identity/made-by rec))
                (= :accepted (answer-word (get-in copied [:ok :description])))))))

(defn a-blocked-sender []
  (let [{:keys [ana ana-own ana-record]} @world
        block (service/block! (svc) (:id ana) {})
        es (repeatedly 5 random-uuid)
        tries (mapv #(service/act! (svc) ana (note ana-own [(:id ana) ana-own ana-own] % "while blocked")) es)
        a-read (service/read! (svc) ana {:asks [:place (first es) :note/text] :layers [ana-own]})
        a-rename (service/rename! (svc) ana "ana-blocked")
        a-sign-in (signed-in "ana-1" nil)
        counted (sys-value ana-record (:id ana) :blocked/count)
        kept (->> (:places (sys-read {:asks [:run :first :last] :layers [ana-record]}))
                  (keep (fn [[[_ k] pa]] (when (= :blocked/attempt k) (:value pa)))))
        landed (keep #(sys-value ana-own % :note/text) es)
        unblocked (service/unblock! (svc) (:id ana))
        after (service/act! (svc) ana (note ana-own [(:id ana) ana-own ana-own] (random-uuid) "after"))]
    (shown "block:" block "| tries:" (mapv :refuse tries) "| a read:" (:refuse a-read) "| a rename:" (:refuse a-rename)
           "| a sign-in:" (:refuse a-sign-in))
    (shown "the record's count:" counted "| attempts kept:" (count kept) (mapv #(select-keys % [:why :at]) kept)
           "| facts that landed:" (count landed))
    (shown "unblocked:" unblocked "| her next act:" (answer-word after))
    (holds "a blocked sender is stopped at the runtime, its acts, its reads and what it asks the runtime to do for it, and nothing it sent lands"
           (and (:ok block) (every? #(= :blocked (:refuse %)) tries) (= :blocked (:refuse a-read))
                (= :blocked (:refuse a-rename)) (empty? landed)))
    (holds "a blocked person's sign-in is refused :blocked, and makes no sitting"
           (and (= :blocked (:refuse a-sign-in)) (nil? (:actor a-sign-in))))
    (holds "the blocked attempts are counted in the sender's record layer, at 1, 2, 4, 8, and the first few kept whole"
           (and (= 8 counted) (= 2 (count kept)) (every? #(= :blocked (:why %)) kept)
                (some? (get-in (first kept) [:thing :facts]))))
    (holds "a block lifted lets the sender's acts through again"
           (and (:ok unblocked) (= :accepted (answer-word after))))))

(defn a-block-of-keys []
  (let [{:keys [sid ana ana-own ana-record sid-own]} @world
        right (service/give-right! (svc) sid {:layer sid-own :to (:id ana) :allows #{:tool/* :note/*}})
        p (get-in right [:ok :permission])
        before (sys-value ana-record (:id ana) :blocked/count)
        block (service/block! (svc) (:id ana) {:keys #{:tool/*} :layers #{ana-own}})
        tools-here (mapv (fn [i] (service/act! (svc) ana {:version act/version :layer ana-own
                                                          :permission [(:id ana) ana-own ana-own]
                                                          :facts [{:e (random-uuid) :k :tool/x :v i}]}))
                         (range 8))
        tool-here (first tools-here)
        note-here (service/act! (svc) ana (note ana-own [(:id ana) ana-own ana-own] (random-uuid) "a note"))
        tool-there (service/act! (svc) ana {:version act/version :layer sid-own :permission p
                                            :facts [{:e (random-uuid) :k :tool/x :v 2}]})
        a-read (service/read! (svc) ana {:asks [:place (:id ana) :softland.store/permission] :layers [ana-own]
                                         :stores-own-facts true})
        after (sys-value ana-record (:id ana) :blocked/count)
        unblocked (service/unblock! (svc) (:id ana))]
    (shown "Ana blocked from :tool/* in her own layer:" (:ok block) "| :tool/x there, eight times:" (mapv :refuse tools-here)
           "| :note/text there:" (answer-word note-here) "| :tool/x in Sid's layer:" (answer-word tool-there)
           "| a read there:" (some? (:ok a-read)))
    (shown "her record's count before:" before "| after:" after "| unblocked:" (:ok unblocked))
    (holds "a block of some keys in one layer stops an act under those keys there and counts it, and lets other keys there, and those keys elsewhere, through"
           (and (:ok block) (every? #(= :blocked (:refuse %)) tools-here) (= :accepted (answer-word note-here))
                (= :accepted (answer-word tool-there)) (:ok a-read)
                (= 8 before) (= 16 after) (:ok unblocked)))))

(defn a-persons-program []
  (let [code (get-in (service/invite! (svc) {:gives []}) [:ok :code])
        bo (:actor (signed-in "bo-1" code))
        bo-own (:identity/own-layer (identity-record (:id bo)))
        made (service/make-agent! (svc) bo {:kind :program :name "bo-tool"})
        prog-id (get-in made [:ok :id])
        right (service/give-right! (svc) bo {:layer bo-own :to prog-id :allows #{:note/* :tool/*}})
        p (get-in right [:ok :permission])
        prog (:ok (service/actor-of (svc) (get-in (service/make-token! (svc) bo prog-id) [:ok :token])))
        act-of (fn [k v] (service/act! (svc) prog {:version act/version :layer bo-own :permission p
                                                   :facts [{:e (random-uuid) :k k :v v}]}))
        sent (fn [id] (:ok (service/sent-in-last-minute (svc) id)))
        block (service/block! (svc) (:id bo) {:keys #{:note/*}})
        note-blocked (act-of :note/text "while her block holds")
        prog-record (:identity/record-layer (identity-record prog-id))
        counted (sys-value prog-record prog-id :blocked/count)
        tool-ok (act-of :tool/x 1)
        _ (service/unblock! (svc) (:id bo))
        note-ok (act-of :note/text "after")
        bytes-now (:bytes (sent prog-id))
        byte-limit (service/set-limits! (svc) prog-id {:bytes-per-minute (+ bytes-now 3000)})
        small (act-of :note/text (apply str (repeat 1000 "a")))
        large (act-of :note/text (apply str (repeat 5000 "b")))
        _ (service/set-limits! (svc) prog-id {:bytes-per-minute (* 64 1024 1024)})
        bo-before (:acts (sent (:id bo)))
        prog-before (:acts (sent prog-id))
        limit (service/set-limits! (svc) (:id bo) {:acts-per-minute (+ bo-before 2)})
        three (mapv #(act-of :tool/x %) [1 2 3])
        bo-after (:acts (sent (:id bo)))
        prog-after (:acts (sent prog-id))]
    (shown "Bo's program:" prog "| Bo blocked from :note/*:" (:ok block) "| its :note/text:" (:refuse note-blocked)
           "| its :tool/x:" (answer-word tool-ok) "| after the block is lifted:" (answer-word note-ok)
           "| its record's count:" counted)
    (shown "its bytes a minute:" byte-limit "| a 1000-byte value:" (answer-word small) "| a 5000-byte value:" (select-keys large [:refuse :bound]))
    (shown "Bo's acts of the last minute:" bo-before "| his limit set to" (+ bo-before 2) ":" (:ok limit)
           "| the program's three acts:" (mapv #(if (:refuse %) (select-keys % [:refuse :bound]) (answer-word %)) three)
           "| Bo's count after:" bo-after "| the program's own:" prog-before "->" prog-after)
    (holds "a block of a person reaches the program she made: its act under the blocked keys is stopped and counted, and its other acts go through"
           (and (:ok block) (= :blocked (:refuse note-blocked)) (= :accepted (answer-word tool-ok))
                (= :accepted (answer-word note-ok)) (= 1 counted)))
    (holds "a program's acts count toward its person's limit, which stops it while its own is far from reached"
           (and (:ok limit) (= [:accepted :accepted] (mapv answer-word (take 2 three)))
                (= {:refuse :over-limit :bound :acts} (select-keys (nth three 2) [:refuse :bound]))
                (= (+ bo-before 2) bo-after) (= (+ prog-before 2) prog-after)))
    (holds "a limit of bytes a minute stops an act that would pass it"
           (and (:ok byte-limit) (= :accepted (answer-word small))
                (= {:refuse :over-limit :bound :bytes} (select-keys large [:refuse :bound]))))))

(defn a-sitting-past-its-end []
  (let [code (get-in (service/invite! (svc) {:gives []}) [:ok :code])
        before (System/currentTimeMillis)
        cleo-in (signed-in "cleo-1" code)
        the-token (get-in cleo-in [:ok :token])
        cleo (:actor cleo-in)
        record (:identity/record-layer (identity-record (:id cleo)))
        ends-at (sys-value record (:session cleo) :session/ends-at)
        week (* 7 24 60 60 1000)
        _ (reset! clock-offset (+ week 1000))
        later (service/actor-of (svc) the-token)
        _ (reset! clock-offset 0)
        now-again (service/actor-of (svc) the-token)]
    (shown "Cleo's sitting ends at:" ends-at "| a week and a second on:" (:refuse later) "| back to now:" (some? (:ok now-again)))
    (holds "a sitting ends seven days after its sign-in, by a fact in her record layer, and actor-of refuses it after"
           (and (some? cleo) (<= (+ before week) ends-at (+ (System/currentTimeMillis) week))
                (= :session-ended (:refuse later)) (some? (:ok now-again))))))

(defn a-limit []
  (let [{:keys [sid sid-own]} @world
        made (service/make-agent! (svc) sid {:kind :program :name "importer"})
        program-id (get-in made [:ok :id])
        token (service/make-token! (svc) sid program-id)
        program (:ok (service/actor-of (svc) (get-in token [:ok :token])))
        right (service/give-right! (svc) sid {:layer sid-own :to program-id :allows #{:import/*}})
        p (get-in right [:ok :permission])
        own-limit (service/set-limits! (svc) program-id {:acts-per-minute 3})
        acts (mapv (fn [i] (service/act! (svc) program {:version act/version :layer sid-own :permission p
                                                        :facts [{:e (random-uuid) :k :import/row :v i}]}))
                   (range 5))
        record (:identity/record-layer (identity-record program-id))]
    (shown "program:" program "| its own limit:" own-limit "| its five acts:" (mapv #(or (:refuse %) (answer-word %)) acts)
           "| the record's count:" (sys-value record program-id :blocked/count))
    (holds "a program held to a limit on its own id, read before its kind's, is stopped at the runtime and counted"
           (and (:ok own-limit) (= :program (:kind program)) (= [:accepted :accepted :accepted] (mapv answer-word (take 3 acts)))
                (= [:over-limit :over-limit] (mapv :refuse (drop 3 acts)))
                (= 2 (sys-value record program-id :blocked/count))))))

(defn a-sign-out []
  (let [{:keys [sid sid-token sid-later-token]} @world
        out (service/sign-out! (svc) sid)
        again (service/sign-out! (svc) sid)
        first-token (service/actor-of (svc) sid-token)
        later-token (service/actor-of (svc) sid-later-token)]
    (shown "sign-out:" out "| again:" again "| the first sitting's token:" (:refuse first-token)
           "| the later one:" (some? (:ok later-token)))
    (holds "a sign-out ends that sitting alone, and a second sign-out finds it ended"
           (and (false? (get-in out [:ok :ended-before?])) (true? (get-in again [:ok :ended-before?]))
                (= :session-ended (:refuse first-token)) (:ok later-token)))))

(defn- cut-after
  "A send that sends n acts and then throws, as a crash would stop a run."
  [send! n]
  (let [sent (atom 0)]
    (fn [the-act]
      (if (< @sent n)
        (do (swap! sent inc) (send! the-act))
        (throw (ex-info "cut" {::cut n}))))))

(defn a-crash-halfway []
  (let [s (svc)
        {:keys [read! send!]} (service/step-io s)
        results
        (vec
         (for [n (range 1 10)]
           (let [code (get-in (service/invite! s {:gives [{:layer ids/base :allows :see}]}) [:ok :code])
                 sub (str "cut-" n "-" (random-uuid))
                 claims {:sub sub :email (str sub "@example.com") :email-verified true :name sub}
                 input (service/sign-in-input s claims code)
                 cut (try (steps/drive #(sign-in/sign-in-step input %) read! (cut-after send! n))
                          (catch clojure.lang.ExceptionInfo e (if (::cut (ex-data e)) :cut (throw e))))
                 again (signed-in sub code)
                 person (get-in again [:ok :person])
                 cut-person (get-in input [:fresh :person])
                 link (sys-value ids/ways-in (ids/account-id :google sub) :way-in/person)]
             {:n n :cut (if (= :cut cut) :cut (:do cut)) :person person :again-first? (get-in again [:ok :first-sign-in?])
              :same-person? (= person link)
              :cut-person-or-none? (or (= cut-person person) (empty? (identity-record cut-person)))
              :right (sys-value ids/base person :softland.store/permission)
              :actor? (some? (:actor again))})))]
    (doseq [r results] (shown "cut after" (:n r) "acts:" (dissoc r :n :right) "| right:" (some? (:right r))))
    (holds "a first sign-in cut after each of its acts is finished by the next sign-in, and makes no second person"
           (every? #(and (:same-person? %) (:cut-person-or-none? %) (some? (:right %)) (:actor? %)) results))))

(defn- race-once
  "Two first sign-ins of one new account, by one new invitation, each with
  its own fresh ids, released together by a latch on two threads. Gives
  what the proof looks at afterwards."
  [n]
  (let [s (svc)
        {:keys [read! send!]} (service/step-io s)
        code (get-in (service/invite! s {:gives [{:layer ids/base :allows :see}]}) [:ok :code])
        sub (str "race-" n "-" (random-uuid))
        claims {:sub sub :email (str sub "@example.com") :email-verified true :name sub}
        inputs [(service/sign-in-input s claims code) (service/sign-in-input s claims code)]
        latch (CountDownLatch. 1)
        refused (mapv (fn [_] (atom [])) inputs)
        runs (mapv (fn [input seen]
                     (future (.await latch)
                             (dissoc (steps/drive #(sign-in/sign-in-step input %) read!
                                                  (fn [a] (let [o (send! a)]
                                                            (when-not (steps/accepted? o)
                                                              (swap! seen conj [(if (uuid? (:layer a)) :a-fresh-layer (:layer a))
                                                                                (or (:reason o) (:answer o))]))
                                                            o)))
                                     :known)))
                   inputs refused)
        _ (.countDown latch)
        results (mapv deref runs)
        link (sys-value ids/ways-in (ids/account-id :google sub) :way-in/person)
        rec (identity-record link)
        fresh (map :fresh inputs)
        own-layers (map :own-layer fresh)
        record-layers (map :record-layer fresh)
        made (fn [layers] (vec (filter made? layers)))]
    {:n n
     :results (mapv #(select-keys % [:do :person :first-sign-in? :word :step :reason :rounds]) results)
     :refused (mapv deref refused)
     :link link
     :both-that-person? (every? #(and (= :done (:do %)) (= link (:person %))) results)
     :persons-with-identity (count (filter #(seq (identity-record %)) (map :person fresh)))
     :person-is-a-fresh-one? (contains? (set (map :person fresh)) link)
     :identity-kind (:identity/kind rec)
     :own-layers-made (count (made own-layers))
     :record-layers-made (count (made record-layers))
     :identity-layers-made? (and (= [(:identity/own-layer rec)] (made own-layers))
                                 (= [(:identity/record-layer rec)] (made record-layers)))
     :invitation-used-by (:person (sys-value ids/invitations (:invitation (token/parse-code code)) :invitation/used))
     :tokens-name-her? (every? #(= link (get-in (service/actor-of s (:token %)) [:ok :id])) results)
     :right? (some? (sys-value ids/base link :softland.store/permission))}))

(defn two-first-sign-ins-at-once []
  (let [runs (mapv race-once (range 1 11))]
    (doseq [r runs]
      (shown "race" (:n r) ":" (:results r))
      (shown "race" (:n r) "acts refused, by run:" (:refused r))
      (shown "race" (:n r) "after:" (dissoc r :n :results :link :refused)))
    (holds "two first sign-ins of one account at the same moment make one person, one own layer and one record layer, and both end with that person"
           (every? #(and (:both-that-person? %) (= 1 (:persons-with-identity %)) (:person-is-a-fresh-one? %)
                         (= :person (:identity-kind %)) (= 1 (:own-layers-made %)) (= 1 (:record-layers-made %))
                         (:identity-layers-made? %) (= (:link %) (:invitation-used-by %))
                         (:tokens-name-her? %) (:right? %))
                   runs))))

(defn test-ns-hook
  "Runs the checks in their order: later checks build on what earlier ones
  made."
  []
  (the-start)
  (an-invitation-and-a-stranger)
  (a-first-sign-in-and-a-later-one)
  (a-second-account-linked)
  (a-client-that-claims-another-name)
  (an-act-without-a-right)
  (names)
  (a-freed-name-taken-again)
  (reads-not-signed-in)
  (a-signed-in-layer)
  (an-agent)
  (a-right-ended)
  (a-token-ended)
  (a-token-asked-for)
  (token-requests-in-a-flood)
  (an-agents-store-keys)
  (a-copy)
  (a-blocked-sender)
  (a-block-of-keys)
  (a-persons-program)
  (a-limit)
  (a-sitting-past-its-end)
  (a-sign-out)
  (a-crash-halfway)
  (two-first-sign-ins-at-once))

(defn -main
  "Starts the cluster, launches the module, opens the service with the
  stand-in for Google, runs the checks, closes the cluster and exits:
  status 0 when each check holds. It takes no argument."
  [& _]
  (let [result (with-open [ipc (rtest/create-ipc)]
                 (rtest/launch-module! ipc module/Store {:tasks 4 :threads 2 :workers 2})
                 (let [s (service/open ipc (rama/get-module-name module/Store)
                                       {:google-client-id client-id
                                        :google-keys (constantly google-keys)
                                        :clock #(+ (System/currentTimeMillis) @clock-offset)
                                        :wait-ms 20000
                                        :rules proof-rules})]
                   (reset! world {:svc s})
                   (run-tests 'softland.runtime.service-proof)))
        {:keys [fail error]} result]
    (println)
    (println "THE PROOF, IN ONE PLACE")
    (run! println @shown-lines)
    (println (select-keys result [:test :pass :fail :error]))
    (shutdown-agents)
    (System/exit (if (zero? (+ fail error)) 0 1))))
