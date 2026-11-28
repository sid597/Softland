(ns softland.server.sign-in
  "What sign-in writes into the store: the acts by which the server finds a
  person from her Google account, or registers her. It requires Missionary
  and the seam, and it is the one caller of the seam's `system`.

  Not decided: all of it. Which acts sign-in writes, into which lanes and
  by which who, is Sid's to say, on a page that he has not yet seen:
  src/proposal/server-2026-11-20/SIGN-IN-WRITES.md holds this build's
  answer for his redline. It is the least form that these lines of his
  leave:

    - \"sign-in gives us a bunch of data and we just store that data in
      rama\" (15 October 2026, ‹3165b020›);
    - the server finds her id from Google's account id, or registers her,
      and writes that id as the who of each act that she makes (his go of
      19 November 2026, ‹afbff6f5›);
    - a person's id is a UUID that says nothing, and her sign-in accounts
      are joined to it by links (src-store/README.md, 9 October 2026,
      entry 1);
    - Google's number is kept as it is (15 October 2026, ‹5325b353›,
      \"1. A\");
    - a signed-in user has a personal lane and a permission in it,
      without asking, made by the system at her first sign-in
      (src-store/README.md, 8 October 2026, entry 2).

  What is written. One lane of the system's, `people`, which only the
  system may see. In it, on the entity of each person, an id that the
  server makes at random:

    account-key    Google's number of her account, as Google gave it. The
                   lane marks this key, so the read path finds the person
                   who holds a number by \"what points at a thing\"
    own-lane-key  the id of her own lane, a random UUID
    says-key       what Google's token said at her latest sign-in: each
                   claim under Google's own name. Every sign-in writes it
                   again, so the lane's line of acts holds what each
                   sign-in gave

  And her own lane, made by its first act: the system is the act's who,
  and the lane's first permission is hers.

  Each act that is about a person names her id in a fact of its own: the
  making act of her own lane as the holder of its first permission, and an
  act in `people` as the entity of its facts.

  What is not written: a name, an invitation, a kind of actor, a limit, a
  block, an agent, a record of sessions. A session is the server's own
  (`softland.server.module`).

  The steps of a sign-in are one task, and each step holds when the task is
  run a second time or by two copies of the server at once: it reads first
  and writes what is missing.

    1. Who holds this number? One read, as the system.
    2. Nobody: one act writes the three facts on a new id. Somebody: one
       act writes what Google said this time.
    3. Who holds it now? When two first sign-ins of one account ran at
       once, two ids hold the number. The one whose fact has the lower
       stamp is the person, for every copy and for good; the other id is
       never read again.
    4. Her own lane's id, from her entity. Is that lane made? When it is
       not, its making act.

  Who may become a person. Anybody whom Google proves: nothing stands
  between a Google account and a person here. On a store that is thrown
  away, with Sid alone, nothing has to. Not decided, and due before a
  stranger can reach the server.

  Inputs and results, by call:

    setup       the seam -> a task of `:ready`, or of a word
    own-lane-of
                the seam and a person's id -> a task of her own lane's id
    person-for  the seam, and the claims of a checked token -> a task of
                {:who id :own-lane id}, or of a word

  State and effects. It owns no state. Each call writes acts through the
  seam, as the system, and reads through it."
  (:require [missionary.core :as m]
            [softland.server.seam :as seam]
            [softland.store.door.declaration :as declaration]
            [softland.store.gate.first-act :as first-act])
  (:import [java.util UUID]))

;; ------------------------------------------------------- the words it writes

(def people
  "The id of the system's lane that holds each person. A session's word."
  :people)

(def account-key
  "The key under which a person's entity holds Google's number of her
  account. A session's word. Another kind of account is another key."
  :account/google)

(def says-key
  "The key under which a person's entity holds what Google's token said at
  her latest sign-in. A session's word."
  :account/google-says)

(def own-lane-key
  "The key under which a person's entity holds the id of her own lane. A
  session's word."
  :person/own-lane)

(def ^:private answer-wait-ms
  "How long a step waits for the answer of an act that it sent."
  10000)

(def ^:private rounds
  "How many times a step reads and writes before it gives up: twice is
  enough when one copy of the server runs, and the third is for a second
  copy that wrote between a read and a write."
  3)

;; ------------------------------------------------------------- the small steps

(defn- could-not
  "Takes what the step was doing and what it got. Gives a word of this
  namespace's own, `:write-her-sign-in`, with the step and the thing."
  [step got]
  {:could-not :write-her-sign-in :at step :got got})

(defn- done
  "Sends one act as the system and reads its answer. Takes the seam, the
  system's actor and an act without `:who` and `:session`. Gives a task of
  the door's answer, or of the door's word."
  [the-seam system act]
  (m/sp
   (let [receipt (m/? (seam/send the-seam system act nil))]
     (if (:could-not receipt)
       receipt
       (m/? (seam/answer the-seam system (:act-id receipt) (:digest receipt) answer-wait-ms))))))

(defn- person-party
  "The store's party ID for a person UUID (`CONTRACT.md` §2.1: `:person.<id>`)."
  [id]
  (keyword (str "person." id)))

(defn first-permission
  "A lane's first owner permission for `holder` (`CONTRACT.md` §2.1, §2.3)."
  ([holder lane] (first-permission holder lane first-act/system))
  ([holder lane given-by]
   {:id (first-act/first-permission-id lane)
    :value {:holder holder
            :allows declaration/owner-allows
            :covers [:default-group lane]
            :given-by given-by}}))

(defn permission-id
  "The `:permission` slot of an act for `permission`: its `UUID`."
  [permission]
  (:id permission))

(defn- permission-fact
  "The store fact that writes `permission`."
  [permission]
  {:e (:id permission) :k declaration/permission-key :v (:value permission)})

(defn- making-act
  "Takes a lane and the holder of its first permission. Gives the act
  that makes the lane, without `:who` and `:session`: the lane's depot on
  the lane, and the first permission on its own id
  (`softland.store.gate.first-act/making-act?`). Whom the lane is for is
  what its first permission says, its holder: a making act holds nothing
  else of her."
  [lane holder]
  (let [permission (first-permission holder lane first-act/system)]
    {:version 1
     :lane lane
     :facts [{:e lane :k :softland.store/depot :v (first-act/depot-at-making lane)}
             (permission-fact permission)]}))

(defn- in-people
  "Takes facts. Gives the act that writes them into `people` under the
  system's first permission there, without `:who` and `:session`. A person
  that the facts are about is their entity."
  [facts]
  {:version 1
   :lane people
   :permission (permission-id (first-permission first-act/system people))
   :facts facts})

(defn- not-made?
  "Takes an answer of a read through one lane. Gives true when the answer
  says that the lane is not made."
  [answer]
  (boolean (some (fn [left] (= :not-made (:why left))) (:left-out answer))))

;; --------------------------------------------------------------------- setup

(defn setup
  "Makes the ground that a sign-in stands on, when it is not there: the
  lane `people`, made by the system, with `account-key` among its marked
  keys. Takes the seam. Gives a task of `:ready`, or of a word. It runs at
  the server's start, reads first, and writes what is missing; two copies
  that start together both end with `:ready`."
  [the-seam]
  (m/sp
   (let [system (seam/system the-seam)]
     (loop [round 0]
       (let [marks (m/? (seam/read the-seam system {:asks [:place people :softland.store/marked-keys]
                                                    :lanes [people]
                                                    :stores-own-facts true}))]
         (cond
           (:could-not marks) (could-not :read-the-marks marks)
           (contains? (:value (:place marks)) account-key) :ready
           (<= rounds round) (could-not :setup-did-not-settle marks)
           (not-made? marks) (do (m/? (done the-seam system (making-act people first-act/system)))
                                 (recur (inc round)))
           :else (do (m/? (done the-seam system
                                (in-people [{:e people
                                             :k :softland.store/marked-keys
                                             :v (conj (or (:value (:place marks)) #{}) account-key)}])))
                     (recur (inc round)))))))))

(defn own-lane-of
  "Tells a person where her own lane is. Takes the seam and her id. Gives a
  task of her own lane's id, or of nil when her entity holds none or the
  read failed. One read of `people`, as the system: she may not see that
  lane, which holds what Google said of everybody, and this one fact about
  herself is what a view needs to know where her acts go. A face hands it
  to the system that holds her credential, which keeps it; it is read at a
  `:who-am-i` and not on the way of an act."
  [the-seam who]
  (m/sp
   (let [own (m/? (seam/read the-seam (seam/system the-seam) {:asks [:place who own-lane-key]
                                                              :lanes [people]}))
         lane (:value (:place own))]
     (when (uuid? lane) lane))))

;; ------------------------------------------------------------------ a sign-in

(defn- holder-of
  "Takes an answer of \"what points at this number\". Gives the id of the
  person who holds the number: the entity of the place whose fact has the
  lowest stamp, or nil when no place holds it."
  [answer]
  (when-let [[[entity _] _] (first (sort-by (fn [[_ place-answer]] (:stamp place-answer)) (:places answer)))]
    entity))

(defn person-for
  "Finds the person of a Google account, or registers her. Takes the seam
  and the claims of an ID token that `softland.server.google/check` passed:
  a map from Google's names to what Google said, with `sub`. Gives a task
  of {:who her id :own-lane her lane's id}, or of a word. The steps are
  in the namespace docstring."
  [the-seam claims]
  (m/sp
   (let [system (seam/system the-seam)
         number (get claims "sub")
         who-holds (fn [] (seam/read the-seam system {:asks [:points-at account-key number]
                                                      :lanes [people]}))
         before (m/? (who-holds))]
     (if (:could-not before)
       (could-not :find-the-account before)
       (let [known (holder-of before)
             subject (or known (person-party (UUID/randomUUID)))
             written (m/? (done the-seam system
                                (in-people
                                 (if known
                                   [{:e subject :k says-key :v claims}]
                                   [{:e subject :k account-key :v number}
                                    {:e subject :k own-lane-key :v (UUID/randomUUID)}
                                    {:e subject :k says-key :v claims}]))))
             after (if known before (m/? (who-holds)))
             person (holder-of after)]
         (cond
           (not= :accepted (:answer written)) (could-not :write-what-google-said written)
           (:could-not after) (could-not :find-the-account after)
           (nil? person) (could-not :find-the-account after)
           :else
           (let [own (m/? (seam/read the-seam system {:asks [:place person own-lane-key]
                                                      :lanes [people]}))
                 lane (:value (:place own))]
             (if-not (uuid? lane)
               (could-not :find-her-own-lane own)
               (let [made (m/? (seam/read the-seam system {:asks [:place lane :softland.store/depot]
                                                           :lanes [lane]
                                                           :stores-own-facts true}))]
                 (cond
                   (:could-not made) (could-not :find-her-own-lane made)
                   (not-made? made)
                   (let [making (m/? (done the-seam system (making-act lane person)))]
                     ;; A second copy can have made it between the read and
                     ;; the act; its act was then the one that counted.
                     (if (:could-not making)
                       (could-not :make-her-own-lane making)
                       {:who person :own-lane lane}))
                   :else {:who person :own-lane lane}))))))))))
