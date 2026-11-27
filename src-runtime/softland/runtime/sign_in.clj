(ns softland.runtime.sign-in
  "What sign-in does: how an account that its provider has proven becomes a
  person with a lane of her own, how a browser and a program come to stand
  for somebody, and how a deployment is born. Each call blocks, and a face
  calls it on a thread that is cheap to hold.

  What sign-in keeps lives in Auth, and the store holds none of it: what
  the provider said of her, her account's number and her email among it, is
  in Auth, with the link, and in no act. An invitation is a person id that
  waits for an account.

  Auth first, the store second. A first sign-in writes in two modules: the
  link in Auth, and the act that makes her own lane in the store. Auth
  fixes the lane's id, and the UUID and the session of the act that makes
  it, at the first `enter` of her account. The act is built from those and
  from nothing else, so every copy of the runtime sends the same thing with
  the same digest, and a gate takes it once. When the answer says accepted,
  Auth is told that the lane is made. A break anywhere between leaves a
  person whose lane is not marked as made, and the next `enter` of her
  account sends the act again. The other order would leave lanes that no
  account leads to.

  The system writes the act that makes her own lane and, at setup, the act
  that makes the base and the act that gives the first person a permission
  in it. She herself writes her name, as a fact on her own id in her own
  lane, under `name-key`."
  (:require [clojure.string :as str]
            [missionary.core :as m]
            [softland.auth.client :as auth]
            [softland.runtime.seam :as seam]
            [softland.store.gate.first-act :as first-act]
            [softland.wire.credential :as credential]
            [softland.wire.words :as words])
  (:import [java.util Locale UUID]))

(def name-key
  "The key under which a person's own id holds the name that she
  confirmed, in her own lane."
  :person/name)

(def ^:private answer-wait-ms
  "How long a step waits for the answer of an act that it sent."
  20000)

(def ask-ms
  "How long a program's sign-in that is under way waits for her answer."
  600000)

(defn- could-not
  [step got]
  {:could-not :write-her-sign-in :at step :got got})

(defn- person-party
  "The store's party ID for a person UUID (raw UUID in Loop 1; `:person.<id>` in Loop 4)."
  [id]
  id)

(defn- first-permission
  "A lane's first owner permission value for `holder`."
  [holder lane]
  [holder lane lane])

(defn- given-permission
  "A given permission value beneath `parent` for `holder` in `lane`."
  [holder lane parent allows]
  [holder lane lane parent allows])

(defn- permission-id
  "The `:permission` slot of an act for `permission`."
  [permission]
  permission)

(defn- permission-fact
  "The store fact that writes `permission`."
  [permission]
  {:e (nth permission 0) :k :softland.store/permission :v permission})

(defn- making-act
  "The act that makes a lane, in the form of
  `softland.store.gate.first-act/making-act?`. When the holder is a person,
  the act names her as its subject, so that what the log holds of her can
  be found by her id."
  [lane holder session]
  (let [permission (first-permission holder lane)]
    (cond-> {:version 1
             :who first-act/system
             :session session
             :lane lane
             :permission (permission-id permission)
             :facts [{:e lane :k :softland.store/depot :v (first-act/depot-at-making lane)}
                     (permission-fact permission)]}
      (not= first-act/system holder) (assoc :subjects #{holder}))))

(defn- accepted?
  [sent]
  (= :accepted (get-in sent [:answer :answer])))

;; ------------------------------------------------------------------ a sign-in

(defn enter!
  "An account that its provider has proven comes in. `:account` is
  [provider subject]; `:says`, what the provider said, a map of texts;
  `:email`, the address that the provider vouches for, or nil;
  `:code-hash`, the hash of an invitation's code that came with the
  sign-in, or nil. An account that has no person and proves no claim gets
  `:take-that` with `:why :not-invited`. A person whose lane is marked as
  made costs one write of Auth and nothing else."
  [the-seam {:keys [account says email code-hash]}]
  (let [client (seam/auth-of the-seam)
        entered (auth/enter client {:account account
                                    :says says
                                    :email (some-> ^String email (.toLowerCase Locale/ROOT))
                                    :code-hash code-hash
                                    :own-lane (UUID/randomUUID)
                                    :making {:uuid (UUID/randomUUID) :session (UUID/randomUUID)}
                                    :at (seam/now-ms)})]
    (cond
      (:could-not entered) (words/could-not :reach-auth {:got entered})
      (:refused entered) (words/could-not :take-that {:why (:refused entered)})
      (:own-lane-made? entered) (select-keys entered [:person :own-lane])
      :else
      (let [{:keys [person own-lane making]} entered
            sent (seam/send-as-system the-seam
                                      (making-act own-lane (person-party person) (:session making))
                                      (:uuid making)
                                      answer-wait-ms)]
        (if-not (accepted? sent)
          (could-not :make-her-own-lane sent)
          (let [marked (auth/own-lane-made client {:person person :at (seam/now-ms)})]
            (if (or (:could-not marked) (:refused marked))
              (words/could-not :reach-auth {:got marked})
              {:person person :own-lane own-lane})))))))

(defn begin-browser!
  "`:credential` is the text for the cookie. The runtime makes the secret
  here and keeps none of it: Auth holds its hash."
  [the-seam person]
  (let [secret (credential/make-secret)
        sign-in (UUID/randomUUID)
        now (seam/now-ms)
        until (+ now (long (:secret-ms (seam/numbers-of the-seam))))
        begun (auth/begin-sign-in (seam/auth-of the-seam)
                                  {:root person :actor person :sign-in sign-in :kind :browser
                                   :hash (credential/hash-of secret) :until until
                                   :label "a browser" :at now})]
    (cond
      (:could-not begun) (words/could-not :reach-auth {:got begun})
      (:refused begun) (words/could-not :take-that {:why (:refused begun)})
      :else {:credential (credential/text person sign-in secret) :until until})))

;; -------------------------------------------------------------------- her name

(defn name-of
  "Nil when she has confirmed none, and when the read failed. One read of
  her own lane, as herself."
  [the-seam caller]
  (when-let [lane (seam/own-lane-of caller)]
    (let [who (seam/root-of caller)
          answer (m/? (seam/call the-seam caller
                                 {:word :read
                                  :query {:asks [:place who name-key] :asker (seam/who-of caller) :lanes [lane]}}))
          value (get-in answer [:place :value])]
      (when (string? value) value))))

(defn name!
  "The caller must be the person herself, in a session. `uuid` is the
  act's, which the page made, so that a form that is posted twice is one
  act."
  [the-seam caller name uuid]
  (let [who (seam/who-of caller)
        lane (seam/own-lane-of caller)]
    (m/? (seam/call the-seam caller
                    {:word :send
                     :act {:version 1
                           :who who
                           :session (seam/session-of caller)
                           :lane lane
                           :permission (permission-id (first-permission who lane))
                           :subjects #{who}
                           :facts [{:e who :k name-key :v name}]}
                     :act-id uuid
                     :wait-ms answer-wait-ms}))))

;; -------------------------------------------------- a program's sign-in

(defn ask!
  "`:hash` is the hash of the secret that the program made and keeps;
  `:asks`, what the program says of itself. A code is made here at random;
  one that is in use is made again, five times at most."
  [the-seam {:keys [hash asks]}]
  (let [client (seam/auth-of the-seam)]
    (loop [tries 5]
      (let [code (credential/make-code)
            now (seam/now-ms)
            ends (+ now ask-ms)
            got (auth/ask client {:code code :hash hash :asks asks :ends ends :at now})]
        (cond
          (:could-not got) (words/could-not :reach-auth {:got got})
          (:asked got) {:code code :ends ends}
          (pos? tries) (recur (dec tries))
          :else (words/could-not :take-that {:why (:refused got)}))))))

(defn asked
  "What a program that waits is told. With `:state :yes` come the three
  parts from which the program makes its credential. A code that names no
  ask and a secret that is not the ask's give the same word,
  `:find-the-ask`: whoever holds a wrong secret learns nothing of an ask."
  [the-seam code secret]
  (let [got (auth/ask-of (seam/auth-of the-seam) code)]
    (cond
      (and (map? got) (:could-not got)) (words/could-not :reach-auth {:got got})
      (nil? got) (words/could-not :find-the-ask nil)
      (not (credential/same-hash? (credential/hash-of secret) (:hash got))) (words/could-not :find-the-ask nil)
      (= :yes (:state got)) (assoc (select-keys (:answer got) [:root :actor :sign-in]) :state :yes)
      (= :no (:state got)) {:state :no}
      (<= (long (:ends got)) (seam/now-ms)) {:state :over}
      :else {:state :waiting :ends (:ends got)})))

(defn ask-shown
  "What the page on which she answers shows of an ask."
  [the-seam code]
  (let [got (auth/ask-of (seam/auth-of the-seam) code)]
    (when (and (map? got) (not (:could-not got)))
      (select-keys got [:asks :state :ends]))))

(defn answer!
  "The caller is the person herself, signed in at a browser. For a yes,
  `:label` is the name of a new agent, or nil to sign the program in as
  herself."
  [the-seam caller code {:keys [yes? label]}]
  (if-not (seam/person? caller)
    (words/could-not :do-that-as-an-agent nil)
    (let [root (seam/root-of caller)
          now (seam/now-ms)
          got (auth/answer-ask (seam/auth-of the-seam)
                               {:code code
                                :yes? (boolean yes?)
                                :root root
                                :actor (if (and yes? label) (UUID/randomUUID) root)
                                :new-agent (when (and yes? label) {:label label})
                                :sign-in (UUID/randomUUID)
                                :until (+ now (long (:secret-ms (seam/numbers-of the-seam))))
                                :at now})]
      (cond
        (:could-not got) (words/could-not :reach-auth {:got got})
        (:refused got) (words/could-not :take-that {:why (:refused got)})
        :else got))))

;; ------------------------------------------------------- a deployment's birth

(defn- not-made?
  [answer]
  (boolean (some (fn [left] (= :not-made (:why left))) (:left-out answer))))

(defn born?
  "Whether this deployment is born: the base is made. Gives true, false, or
  a word of the read path when the read failed."
  [the-seam]
  (let [answer (seam/read-as-system the-seam {:asks [:place first-act/base :softland.store/depot]
                                              :asker first-act/system
                                              :lanes [first-act/base]
                                              :stores-own-facts true})]
    (cond
      (:could-not answer) answer
      (not-made? answer) false
      :else true)))

(defn setup!
  "A deployment's birth, and the first person. `:email` is the address of
  the first person's account, as her provider vouches for it; `:person` is
  the id that an earlier run of this call printed, or nil for a new one.

  Auth first: the first person waits under her email; then the base is
  made, when it is not; then the system gives her a permission in the base,
  beneath its own, to see and to act. Each step holds when it is done
  twice. A run that was cut off after the first step is run again with the
  same `:person`: a new id would be refused, since the claim names the
  first id for good. Her own lane is made at her first sign-in, as
  everybody's is."
  [the-seam {:keys [email person]}]
  (let [client (seam/auth-of the-seam)
        person (or person (UUID/randomUUID))
        invited (auth/invite client {:person person :claim {:by :email :email (.toLowerCase ^String email Locale/ROOT)}
                                     :invited-by :system :at (seam/now-ms)})]
    (cond
      (:could-not invited) (words/could-not :reach-auth {:got invited})
      (:refused invited) (words/could-not :take-that {:why (:refused invited) :person person})
      :else
      (let [base first-act/base
            system first-act/system
            session (UUID/randomUUID)
            born (born? the-seam)
            made (cond
                   (true? born) :was-made
                   (false? born) (seam/send-as-system the-seam (making-act base system session)
                                                      (UUID/randomUUID) answer-wait-ms)
                   :else born)]
        (cond
          (and (map? made) (:could-not made)) made
          (and (map? made) (not (accepted? made)) (not (true? (born? the-seam)))) (could-not :make-the-base made)
          :else
          (let [above (first-permission system base)
                her-party (person-party person)
                given (seam/send-as-system
                       the-seam
                       {:version 1
                        :who system
                        :session session
                        :lane base
                        :permission (permission-id above)
                        :subjects #{her-party}
                        :facts [(permission-fact (given-permission her-party base above :see-and-act))]}
                       (UUID/randomUUID)
                       answer-wait-ms)]
            (if (:could-not given)
              given
              {:person person
               :base (if (= :was-made made) :was-made :made)
               :gave (:answer given)})))))))
