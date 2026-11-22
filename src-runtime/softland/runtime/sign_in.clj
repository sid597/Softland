(ns softland.runtime.sign-in
  "What sign-in does: how an account that its provider has proven becomes a
  person with a layer of her own, how a browser and a program come to stand
  for somebody, and how a deployment is born. It requires Missionary, the
  seam, Auth's client and the store's two ids. Each call blocks, and a face calls it on a
  thread that is cheap to hold.

  Sid's decisions that it stands on (src/proposal/runtime-2026-11-21/
  PROPOSAL.md, entries 2, 5 and 12; and his go of 21 November 2026 to the
  sheet for sign-in):

    - what sign-in keeps lives in Auth, a module of its own; the store
      holds none of it;
    - sign-up is by invitation or a list, and an invitation is a person id
      that waits for an account;
    - a signed-in person has a layer of her own and a permission in it,
      without asking, and the system makes both at her first sign-in
      (src-store/README.md, 8 October 2026, entry 2);
    - she is called what she confirms: her provider's name for her is
      shown, and she confirms or changes it;
    - a deployment is born by one setup command, which makes the base and
      the first person.

  Auth first, the store second. A first sign-in writes in two modules: the
  link in Auth, and the act that makes her own layer in the store. The
  runtime writes Auth first. Auth fixes the layer's id, and the UUID and
  the session of the act that makes it, at the first `enter` of her
  account. The act is built from those and from nothing else, so every
  copy of the runtime sends the same thing with the same digest, and a
  gate takes it once. When the answer says accepted, Auth is told that the
  layer is made. A break anywhere between leaves a person whose layer is
  not marked as made, and the next `enter` of her account sends the act
  again. The other order would leave layers that no account leads to.

  What is written into the store, and by whom:

    the system    the act that makes her own layer: the layer's depot, and
                  her first permission in it. It names her as its subject.
                  At setup: the act that makes the base, and the act that
                  gives the first person a permission in it
    she herself   her name, as a fact on her own id in her own layer, under
                  `name-key`

  What the provider said of her, her account's number and her email among
  it, is in Auth, with the link, and in no act.

  Inputs and results, by call:

    enter!          the seam and a proven account -> {:person :own-layer},
                    or a word
    begin-browser!  the seam and a person -> {:credential :until}, or a word
    name-of         the seam and a caller -> her name, or nil
    name!           the seam, a caller, a name and a UUID -> the door's
                    receipt with its answer, or a word
    ask!            the seam and what a program asks -> {:code :ends}, or a
                    word
    asked           the seam, a code and a secret -> what a program that
                    waits is told
    answer!         the seam, a caller, a code and her answer -> what Auth
                    gave, or a word
    born?           the seam -> whether the base is made
    setup!          the seam and the first person -> what was done, or a
                    word

  State and effects. It owns no state. Each call reads and writes Auth
  through its client, and sends acts through the seam."
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
  confirmed, in her own layer. A session's word, and Sid's to change."
  :person/name)

(def ^:private answer-wait-ms
  "How long a step waits for the answer of an act that it sent."
  20000)

(def ask-ms
  "How long a program's sign-in that is under way waits for her answer: ten
  minutes. A number of the build, kept and not decided."
  600000)

(defn- could-not
  "Takes what a step was doing and what it got. Gives a word of this
  namespace's own, `:write-her-sign-in`, with the step and the thing."
  [step got]
  {:could-not :write-her-sign-in :at step :got got})

(defn- making-act
  "Takes a layer, the holder of its first permission, and the session that
  the act names. Gives the act that makes the layer, whole: the system is
  its who; its facts are the layer's depot on the layer and the first
  permission on its holder (`softland.store.gate.first-act/making-act?`).
  When the holder is a person, the act names her as its subject, so that
  what the log holds of her can be found by her id."
  [layer holder session]
  (let [permission [holder layer layer]]
    (cond-> {:version 1
             :who first-act/system
             :session session
             :layer layer
             :permission permission
             :facts [{:e layer :k :softland.store/depot :v (first-act/depot-at-making layer)}
                     {:e holder :k :softland.store/permission :v permission}]}
      (not= first-act/system holder) (assoc :subjects #{holder}))))

(defn- accepted?
  "Takes what `softland.runtime.seam/send-as-system` gave. Gives true when
  a gate accepted the act."
  [sent]
  (= :accepted (get-in sent [:answer :answer])))

;; ------------------------------------------------------------------ a sign-in

(defn enter!
  "An account that its provider has proven comes in. Takes the seam and a
  map: `:account`, [provider subject]; `:says`, what the provider said, a
  map of texts; `:email`, the address that the provider vouches for, or
  nil; `:code-hash`, the hash of an invitation's code that came with the
  sign-in, or nil. Gives {:person her id :own-layer her layer's id}, or a
  word: `:take-that` with `:why :not-invited` for an account that has no
  person and proves no claim; `:reach-auth`; or `:write-her-sign-in` when
  the act that makes her layer was not accepted.

  The steps are in the namespace docstring, \"Auth first, the store
  second\". A person whose layer is marked as made costs one write of Auth
  and nothing else."
  [the-seam {:keys [account says email code-hash]}]
  (let [client (seam/auth-of the-seam)
        entered (auth/enter client {:account account
                                    :says says
                                    :email (some-> ^String email (.toLowerCase Locale/ROOT))
                                    :code-hash code-hash
                                    :own-layer (UUID/randomUUID)
                                    :making {:uuid (UUID/randomUUID) :session (UUID/randomUUID)}
                                    :at (seam/now-ms)})]
    (cond
      (:could-not entered) (words/could-not :reach-auth {:got entered})
      (:refused entered) (words/could-not :take-that {:why (:refused entered)})
      (:own-layer-made? entered) (select-keys entered [:person :own-layer])
      :else
      (let [{:keys [person own-layer making]} entered
            sent (seam/send-as-system the-seam
                                      (making-act own-layer person (:session making))
                                      (:uuid making)
                                      answer-wait-ms)]
        (if-not (accepted? sent)
          (could-not :make-her-own-layer sent)
          (let [marked (auth/own-layer-made client {:person person :at (seam/now-ms)})]
            (if (or (:could-not marked) (:refused marked))
              (words/could-not :reach-auth {:got marked})
              {:person person :own-layer own-layer})))))))

(defn begin-browser!
  "Begins a browser's sign-in for a person. Takes the seam and her id.
  Gives {:credential the text for the cookie, :until when its secret's time
  passes}, or a word. The runtime makes the secret here and keeps none of
  it: Auth holds its hash."
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
  "Reads the name that a person confirmed. Takes the seam and her caller.
  Gives the name, or nil when she has confirmed none or the read failed.
  One read of her own layer, as herself."
  [the-seam caller]
  (when-let [layer (seam/own-layer-of caller)]
    (let [who (seam/root-of caller)
          answer (m/? (seam/call the-seam caller
                                 {:word :read
                                  :query {:asks [:place who name-key] :asker (seam/who-of caller) :layers [layer]}}))
          value (get-in answer [:place :value])]
      (when (string? value) value))))

(defn name!
  "Writes the name that a person confirms: her own act, in her own layer.
  Takes the seam; her caller, who is the person herself and is in a
  session; the name, a text; and the act's UUID, which the page made, so
  that a form that is posted twice is one act. Gives what the word `:send`
  gives."
  [the-seam caller name uuid]
  (let [who (seam/who-of caller)
        layer (seam/own-layer-of caller)]
    (m/? (seam/call the-seam caller
                    {:word :send
                     :act {:version 1
                           :who who
                           :session (seam/session-of caller)
                           :layer layer
                           :permission [who layer layer]
                           :subjects #{who}
                           :facts [{:e who :k name-key :v name}]}
                     :act-id uuid
                     :wait-ms answer-wait-ms}))))

;; -------------------------------------------------- a program's sign-in

(defn ask!
  "A program asks to be signed in. Takes the seam and a map: `:hash`, the
  hash of the secret that the program made and keeps; `:asks`, what the
  program says of itself, a text. Gives {:code the code under which the
  ask is found, :ends when it is over}, or a word. A code is made here at
  random; one that is in use is made again, five times at most."
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
  "What a program that waits is told. Takes the seam, the code and the
  secret that the program holds. Gives one of:

    {:state :waiting :ends t}       nobody has answered yet
    {:state :yes :root :actor :sign-in}
                                    she said yes: the three parts from
                                    which the program makes its credential
    {:state :no}                    she said no
    {:state :over}                  the ask's time passed with no answer

  or the word `:find-the-ask` for a code that names no ask, and for a
  secret that is not the ask's: whoever holds a wrong secret learns nothing
  of an ask. One read of Auth."
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
  "What the page on which she answers shows of an ask. Takes the seam and a
  code. Gives {:asks text :state :ends}, or nil for a code that names no
  ask."
  [the-seam code]
  (let [got (auth/ask-of (seam/auth-of the-seam) code)]
    (when (and (map? got) (not (:could-not got)))
      (select-keys got [:asks :state :ends]))))

(defn answer!
  "A person answers a program's ask. Takes the seam; her caller, who is the
  person herself, signed in at a browser; the code; and a map: `:yes?`;
  and, for a yes, `:label`, the name of a new agent, or nil to sign the
  program in as herself. Gives what Auth gave, {:yes {...}} or {:no code},
  or a word: `:do-that-as-an-agent` for a caller that is no person;
  `:take-that` with Auth's reason; `:reach-auth`."
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
  "Takes an answer of a read through one layer. Gives true when the answer
  says that the layer is not made."
  [answer]
  (boolean (some (fn [left] (= :not-made (:why left))) (:left-out answer))))

(defn born?
  "Whether this deployment is born: the base is made. Takes the seam. Gives
  true, false, or a word of the read path when the read failed. One read,
  as the system."
  [the-seam]
  (let [answer (seam/read-as-system the-seam {:asks [:place first-act/base :softland.store/depot]
                                              :asker first-act/system
                                              :layers [first-act/base]
                                              :stores-own-facts true})]
    (cond
      (:could-not answer) answer
      (not-made? answer) false
      :else true)))

(defn setup!
  "A deployment's birth, and the first person. Takes the seam and a map:
  `:email`, the address of the first person's account, as her provider
  vouches for it; and `:person`, the id that an earlier run of this call
  printed, or nil for a new one. Gives a map of what was done, or a word.

  The steps, Auth first:

    1. The first person waits under her email (`softland.auth.client/
       invite`, by the system). A claim that is in use under another id is
       told: this email was set up before.
    2. The base is made, by the system, when it is not.
    3. The system gives the first person a permission in the base, beneath
       its own, to see and to act.

  Each step holds when it is done twice. A run that was cut off after step
  1 is run again with the same `:person`, which the first run printed; a
  new id would be refused at step 1, since the claim names the first id
  for good. Her own layer is made at her first sign-in, as everybody's is."
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
          (let [above [system base base]
                given (seam/send-as-system
                       the-seam
                       {:version 1
                        :who system
                        :session session
                        :layer base
                        :permission above
                        :subjects #{person}
                        :facts [{:e person :k :softland.store/permission
                                 :v [person base base above :see-and-act]}]}
                       (UUID/randomUUID)
                       answer-wait-ms)]
            (if (:could-not given)
              given
              {:person person
               :base (if (= :was-made made) :was-made :made)
               :gave (:answer given)})))))))
