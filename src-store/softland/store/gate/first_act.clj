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
    starts-with      a layer id, and one of the four store keys that take a
                     second write -> what the layer reads as under that key
                     while no fact is there

  `system` and `base` are the two ids that the form knows: the system stands
  at the top of the base's chain. The system can hold other layers too (Sid,
  9 October 2026): the form takes `system` as the first holder of any layer.
  The words are Sid's (\"system\", \"the
  base\"); the keywords are a session's.

  What a gate does not check, and cannot: who the holder of a first
  permission is, a person, a session or an agent; and whether the sender is
  the `:who` that the act names. Until sign-in is built, any sender can make
  any layer that is not made, for any holder.

  One place where the holder of the permission that an act names is not the
  act's `:who`: the system makes a personal layer for its person, so that
  act's `:who` is `system` and its permission is the person's. Sid's
  decision says that the system makes the layer and the permission; under
  which permission the system then acts is the builder's reading.

  State and effects. None. Whether the record holds the layer is the
  caller's question (`softland.store.gate.decision/standing`).")

(def system
  "The id of the system: software that acts for the architects of the
  medium. It makes the base, and it makes each personal layer at its
  person's first sign-in. It can hold other layers beside the base (Sid,
  9 October 2026)."
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
  3. the act's `:who` is the holder (a group's layer, made by the person who
     creates the group) or `system` (a personal layer, made for its person);
     for the base the holder is `system`, and `system` can be the holder of
     another layer too;
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
          (or (= (:who act) holder) (= (:who act) system))
          (or (not= base layer) (= holder system))
          (= 2 (count (:facts act)))
          (= #{{:e layer :k :softland.store/depot :v starts-as}
               {:e holder :k :softland.store/permission :v permission}}
             (into #{} (map #(select-keys % [:e :k :v])) (:facts act)))))))

(defn starts-with
  "What a layer starts with. Takes a layer id and a store key. Gives what the
  layer reads as under that key while its place holds no fact:

    :softland.store/who-may-see  `:everyone` for the base, `:members` for
                                 any other layer
    :softland.store/builds-on    `[]`: a layer builds on nothing until a
                                 fact says so
    :softland.store/marked-keys  `#{}`: no key is marked
    :softland.store/one-write-keys
                                 `#{}`: every place takes a second write

  and nil for any other key. No fact is written at the making: a layer that
  was made before these keys existed holds no such fact, so a reader must
  read \"no fact\" either way, and the form of a making act stays two
  facts. A reader of the tables calls it, and so does the gate's check of
  the claims (`softland.store.gate.decision/claims`), for the one-write
  keys of a layer whose place holds no fact.

  Not decided. That a new layer is \"members\" and the base \"everyone\" is
  kept only so that the build can run (BUILD-reads.md, \"Not decided\"; with
  \"everyone\" a person who is not signed in sees the base). That a layer
  starts with no one-write key is a session's, of the sign-in build. This
  function is the one place where the other answer would be made. A later
  change here changes how every layer with no fact reads."
  [layer k]
  (case k
    :softland.store/who-may-see (if (= base layer) :everyone :members)
    :softland.store/builds-on []
    :softland.store/marked-keys #{}
    :softland.store/one-write-keys #{}
    nil))
