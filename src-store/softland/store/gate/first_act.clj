(ns softland.store.gate.first-act
  "The first act of a lane: the depot it starts with, the form of a making
  act, and what a lane reads as where no fact says it. Pure code.

  A gate checks no sender. So any program that can send can name `system`,
  and can make any lane that is not made. What a gate can tell is what a
  party is, since a party's id says it (`softland.store.act/party-kind`):
  a lane's first permission is held by a person, or by the system.

  The system makes a personal lane for its person: that act's `:who` is
  `system`, and the lane's first permission is the person's."
  (:require [softland.store.act :as act]
            [softland.store.door.declaration :as declaration])
  (:import [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets]
           [java.security MessageDigest]
           [java.util UUID]))

(def system
  "The id of the system: software that acts for the architects of the
  medium. It makes the base, and each personal lane at its person's first
  sign-in. It can hold other lanes too."
  :system)

(def base
  "The one lane that everyone subscribes to."
  :base)

(defn depot-at-making
  [lane]
  (if (= base lane) :microbatch :streaming))

(defn starts-with-the-other-gates-depot?
  "Reads the lane id alone. So the word of an act on one depot, for a lane
  that starts with the other, comes from the act alone and is the same
  whenever a gate decides it. No act changes a lane's depot: where a lane
  starts is where it stays."
  [gate-depot lane]
  (not= gate-depot (depot-at-making lane)))

;; ------------------------------------------- the ids that a lane starts with

(defn- id-from-text
  "A UUID of version 8 made of a text alone: the first 16 bytes of the
  SHA-256 of its UTF-8, with the version and the variant written in. The
  same text gives the same id in every process."
  [^String text]
  (let [hash (.digest (MessageDigest/getInstance "SHA-256") (.getBytes text StandardCharsets/UTF_8))
        buffer (ByteBuffer/wrap hash 0 16)
        high (.getLong buffer)
        low (.getLong buffer)]
    (UUID. (bit-or (bit-and high (bit-not 0xF000)) 0x8000)
           (bit-or (bit-and low 0x3FFFFFFFFFFFFFFF) Long/MIN_VALUE))))

(defn first-permission-id
  "The id that a maker gives a lane's first permission, made of the lane's
  id alone. So a program that makes a lane a second time, after it lost the
  answer of the first, sends the same act. No gate holds a making act to
  it: an id is its giver's."
  [lane]
  (id-from-text (str "softland/first-permission/" (declaration/id-text lane))))

(defn first-anyone-id
  "The id that a maker gives the permission that opens a lane to anyone at
  all, when she opens it as she makes it; made of the lane's id alone, as
  `first-permission-id`."
  [lane]
  (id-from-text (str "softland/first-anyone/" (declaration/id-text lane))))

;; -------------------------------------------------------- the making act

(defn- first-permission?
  "True for what a lane's first permission says: an owner's, over the
  lane's default group, good in every sitting, standing on its own, given
  by the act's author under nothing, to a person or to the system. The base
  is the system's."
  [act v]
  (let [lane (:lane act)
        holder (:holder v)]
    (boolean
     (and (contains? #{:person :system} (act/party-kind holder))
          (or (not= base lane) (= system holder))
          (or (= (:who act) holder) (= (:who act) system))
          (:owner (:allows v))
          (= [:default-group lane] (:covers v))
          (nil? (:session v))
          (= (:who act) (:given-by v))
          (nil? (:under v))
          (nil? (:lent-from v))))))

(defn- opening-permission?
  "True for what the permission says by which a maker opens her lane as she
  makes it: seeing and nothing more, to anyone at all, over the lane's
  default group, in every sitting, standing on its own, given by the
  lane's first holder under the first permission that the same act makes."
  [act first-fact v]
  (boolean
   (and (= (:who act) (:holder (:v first-fact)))
        (= :anyone (:holder v))
        (= {:see true} (:allows v))
        (= [:default-group (:lane act)] (:covers v))
        (nil? (:session v))
        (= (:who act) (:given-by v))
        (= (:e first-fact) (:under v))
        (nil? (:lent-from v)))))

(defn making-act?
  "True says only that the act has the form. It reads no record: the caller
  must also know that the record holds nothing for the lane.

  A making act names no permission. It holds the lane's depot and the
  lane's first permission (`first-permission?`), and nothing else but, when
  its maker opens the lane as she makes it, the permission to anyone at all
  (`opening-permission?`). That is the one give that is under a permission
  of its own act: every other give is under the permission that its act
  names, which stood before the act.

  The act's `:who` is the first holder, or `system`, making a personal lane
  for its person. Whoever makes a lane for another holds nothing over it
  afterwards.

  A making act that names something to replace is stale at the gate's check
  of replaced facts: nothing is current in a lane that is not made."
  [gate-depot act]
  (let [lane (:lane act)
        starts-as (depot-at-making lane)
        facts (:facts act)
        depot-facts (filter #(= :softland.store/depot (:k %)) facts)
        gives (filter #(and (= declaration/permission-key (:k %)) (some? (:v %))) facts)
        first-fact (first (filter #(nil? (:under (:v %))) gives))
        others (remove #(identical? first-fact %) gives)]
    (boolean
     (and (= gate-depot starts-as)
          (nil? (:permission act))
          (= (count facts) (+ (count depot-facts) (count gives)))
          (= 1 (count depot-facts))
          (= {:e lane :k :softland.store/depot :v starts-as}
             (select-keys (first depot-facts) [:e :k :v]))
          first-fact
          (first-permission? act (:v first-fact))
          (<= (count others) 1)
          (every? #(opening-permission? act first-fact (:v %)) others)))))

(defn starts-with
  "What a lane reads as under a store key while its place holds no fact; nil
  for any other key. No fact is written at the making: a lane made before
  these keys existed holds none, so a reader must read \"no fact\" either
  way. No gate calls it; a reader of the tables does.

  Who may see a lane is not here: a lane starts with the permissions that
  its making act gives, and no value in code opens one. The acts of a lane
  are read whole by its holders and by the who that a thing names."
  [lane k]
  (case k
    :softland.store/who-may-read-acts :members
    :softland.store/builds-on []
    :softland.store/marked-keys #{}
    nil))
