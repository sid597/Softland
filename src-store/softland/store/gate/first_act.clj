(ns softland.store.gate.first-act
  "The first act of a layer: the depot that a layer starts with, the form of
  a making act, and what a layer starts with where no fact says it. Pure
  code. It needs no cluster.

  A layer is made by one act, its first, so that the making of a layer is in
  the acts depot. A gate accepts an act into a layer for which the record
  holds nothing only when it is a making act: it writes the layer's depot
  and one permission, the layer's first, with nothing above it, and it names
  that permission as the one it acts under. The form of a making act is the
  builder's, not a decision of Sid's.

  Inputs and results, by call:

    depot-at-making  a layer id -> the depot that the layer starts with
    starts-with-the-other-gates-depot?
                     a gate's depot, and a layer id -> true or false
    making-act?      a gate's depot, and an act in its one form on arrival
                     -> true or false
    signed-making-act  a layer id, a signing who, and a session -> the
                     signed making act of the layer for that who, as its
                     maker gives it before she signs
    starts-with      a layer id, and one of the three store keys that take a
                     second write -> what the layer reads as under that key
                     while no fact is there

  `system` and `base` are the two ids that the form knows: the system stands
  at the top of the base's chain. The system can hold other layers too (Sid,
  9 October 2026): the form takes `system` as the first holder of any layer.
  The words are Sid's (\"system\", \"the
  base\"); the keywords are a session's.

  Two editions. An unsigned making act, of version 1, is as it was: its
  `:who` is the holder or `system`, and for the base the holder is
  `system`. A signed making act, of version 2, names no `system`: its
  `:who` is the holder, for the base too. So the first holder of a layer
  that a signed act makes is the signing who that signed it, and the
  system of a store whose base a signed act made is that who: the first
  act defines it, and no id that the code knows names it. That is the
  builder's form of Sid's choice of 18 November 2026, that the first act
  itself carries its who's public half and is signed with it.

  What a gate does not check, and cannot: who the holder of a first
  permission is, a person, a session or an agent. Whether the sender is
  the `:who` that the act names is held for a signed act, by the
  declaration's check of its signature
  (`softland.store.door.declaration/check`), and not for an unsigned one.
  Any sender can make any layer that is not made: with an unsigned act for
  any holder, and with a signed act for a who whose private half she
  holds. The first right making act to arrive makes the layer, so who
  makes the base of an empty store is decided by arrival; its maker reads
  her act's answer to know that it was hers.

  One place where the holder of the permission that an act names is not the
  act's `:who`: the system makes a personal layer for its person, so that
  act's `:who` is `system` and its permission is the person's. Sid's
  decision says that the system makes the layer and the permission; under
  which permission the system then acts is the builder's reading.

  State and effects. None. Whether the record holds the layer is the
  caller's question (`softland.store.gate.decision/standing`)."
  (:require [softland.store.act :as act]))

(def system
  "The id of the system in an unsigned act: software that acts for the
  architects of the medium. It makes the base, and it makes each personal
  layer at its person's first sign-in. It can hold other layers beside the
  base (Sid, 9 October 2026). Any sender can name it. A signed making act
  does not know this id: there the system is the signing who that made the
  base."
  :system)

(def base
  "The id of the base: the one layer that everyone subscribes to."
  :base)

(defn depot-at-making
  "Takes a layer id. Gives the depot that the layer starts with: :microbatch
  for the base, :streaming for any other layer. The one place where that
  decision (F4 C) is code."
  [layer]
  (if (= base layer) :microbatch :streaming))

(defn starts-with-the-other-gates-depot?
  "Takes the depot of a gate, :streaming or :microbatch, and a layer id.
  Gives true when the layer starts with the other gate's depot: what
  `depot-at-making` gives for the layer is not the gate's depot. It reads
  the layer id and nothing else: no record, of this gate or of the other.

  `softland.store.gate.decision/standing` and `makes-the-layer?` ask it, for
  both gates, as whether the layer is the other gate's. For the streaming
  gate such a layer is the base; for the microbatch gate it is every other
  layer. So the word of an act on one depot for a layer that starts with
  the other depot comes from the act alone, and is the same whenever the
  gate decides it. That rule is a session's, 14 October 2026; `standing`
  says what forced it.

  Limit. It says where a layer starts, which in this build is where the
  layer stays: no act changes a layer's depot."
  [gate-depot layer]
  (not= gate-depot (depot-at-making layer)))

(defn making-act?
  "Takes the depot of a gate, and an act in its one form on arrival. Gives
  true when the act has the form of a making act for that gate. All of these
  hold:

  1. the gate's depot is the depot that the act's layer starts with;
  2. the act's permission is [holder layer layer]: three slots, nothing
     above it, granted in the layer itself. A permission with four or five
     slots is a given one, so an act that names one is no making act, also
     when its fifth slot says `:see-and-act`; it is refused
     `:layer-not-made`, and the layer can still be made by a right act;
  3. for an unsigned act: the act's `:who` is the holder (a group's layer,
     made by the person who creates the group) or `system` (a personal
     layer, made for its person); for the base the holder is `system`, and
     `system` can be the holder of another layer too. For a signed act:
     the act's `:who` is the holder, for the base as for any layer, and
     `system` is no part of the rule. A layer that one who makes for
     another, as the system makes a personal layer, has no signed form in
     this build;
  4. the act has exactly two facts, in either order: the depot that the
     layer starts with, on the layer's own entity under
     `:softland.store/depot`; and the act's own permission, on its holder
     under `:softland.store/permission`.

  The form says nothing about `:replaces`: a making act that names something
  to replace is stale at the gate's check of replaced facts, because nothing
  is current in a layer that is not made.

  Limit. It reads no record. True says only that the act has the form; the
  caller must also know that the record holds nothing for the layer."
  [gate-depot act]
  (let [layer (:layer act)
        permission (:permission act)
        holder (first permission)
        starts-as (depot-at-making layer)]
    (boolean
     (and (= gate-depot starts-as)
          (= 3 (count permission))
          (= layer (nth permission 1))
          (= layer (nth permission 2))
          (if (= act/signed-version (:version act))
            (= (:who act) holder)
            (and (or (= (:who act) holder) (= (:who act) system))
                 (or (not= base layer) (= holder system))))
          (= 2 (count (:facts act)))
          (= #{{:e layer :k :softland.store/depot :v starts-as}
               {:e holder :k :softland.store/permission :v permission}}
             (into #{} (map #(select-keys % [:e :k :v])) (:facts act)))))))

(defn signed-making-act
  "Builds the signed making act of a layer, as its maker gives it before she
  signs. Takes a layer id; a signing who, the layer's first holder and the
  act's `:who`; and a session id. Gives the maker's act of version 2 with
  the two facts of a making act, the depot that the layer starts with and
  the who's first permission, and with no `:signature`: the maker adds
  that, over the act with its act-id
  (`softland.store.door.declaration/what-is-signed`). With a right
  signature it has the form that `making-act?` takes at the gate of the
  layer's depot.

  It tests nothing of what it is handed: a who that is no signing who
  gives an act that a gate refuses. For the base it is the store's first
  act."
  [layer who session]
  (let [permission [who layer layer]]
    {:version act/signed-version
     :who who
     :layer layer
     :permission permission
     :session session
     :facts [{:e layer :k :softland.store/depot :v (depot-at-making layer)}
             {:e who :k :softland.store/permission :v permission}]}))

(defn starts-with
  "What a layer starts with. Takes a layer id and a store key. Gives what the
  layer reads as under that key while its place holds no fact:

    :softland.store/who-may-see  `:everyone` for the base, `:members` for
                                 any other layer
    :softland.store/builds-on    `[]`: a layer builds on nothing until a
                                 fact says so
    :softland.store/marked-keys  `#{}`: no key is marked

  and nil for any other key. No gate calls it, and no fact is written at the
  making: a layer that was made before these keys existed holds no such
  fact, so a reader must read \"no fact\" either way, and the form of a
  making act stays two facts. A reader of the tables calls it.

  Not decided. That a new layer is \"members\" and the base \"everyone\" is
  kept only so that the build can run (BUILD-reads.md, \"Not decided\"; with
  \"everyone\" a person who is not signed in sees the base). This function
  is the one place where the other answer would be made. A later change
  here changes how every layer with no fact reads."
  [layer k]
  (case k
    :softland.store/who-may-see (if (= base layer) :everyone :members)
    :softland.store/builds-on []
    :softland.store/marked-keys #{}
    nil))
