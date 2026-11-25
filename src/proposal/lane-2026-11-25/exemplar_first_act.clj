(ns softland.store.gate.first-act
  "The first act of a lane: the depot it starts with, the form of a making
  act, and what a lane reads as where no fact says it. Pure code.

  A gate checks no sender, and cannot tell what the holder of a first
  permission is, a person, a session or an agent. So any program that can
  send can name `system`, and can make any lane that is not made.

  The system makes a personal lane for its person: that act's `:who` is
  `system`, and its permission is the person's.")

(def system
  "The id of the system. It makes the base, and each personal lane at its
  person's first sign-in. It can hold other lanes too."
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

(defn making-act?
  "True says only that the act has the form. It reads no record: the caller
  must also know that the record holds nothing for the lane.

  The act's `:who` is the holder (a person making a group's lane) or
  `system` (making a personal lane for its person).

  An act that names a given permission, four or five slots, is no making
  act, also when its fifth slot says `:see-and-act`. It is refused
  `:lane-not-made`, and the lane can still be made by a right act.

  A making act that names something to replace is stale at the gate's check
  of replaced facts: nothing is current in a lane that is not made."
  [gate-depot act]
  (let [lane (:lane act)
        permission (:permission act)
        holder (first permission)
        starts-as (depot-at-making lane)]
    (boolean
     (and (= gate-depot starts-as)
          (= 3 (count permission))
          (= lane (nth permission 1))
          (= lane (nth permission 2))
          (or (= (:who act) holder) (= (:who act) system))
          (or (not= base lane) (= holder system))
          (= 2 (count (:facts act)))
          (= #{{:e lane :k :softland.store/depot :v starts-as}
               {:e holder :k :softland.store/permission :v permission}}
             (into #{} (map #(select-keys % [:e :k :v])) (:facts act)))))))

(defn starts-with
  "What a lane reads as under a store key while its place holds no fact; nil
  for any other key. No fact is written at the making: a lane made before
  these keys existed holds none, so a reader must read \"no fact\" either
  way. No gate calls it; a reader of the tables does.

  The base is open to everyone and its acts are not: they are read whole by
  its holders and by the who that a thing names."
  [lane k]
  (case k
    :softland.store/who-may-see (if (= base lane) :everyone :members)
    :softland.store/who-may-read-acts :members
    :softland.store/builds-on []
    :softland.store/marked-keys #{}
    nil))
