(ns softland.store.gate.first-act
  "The first act of a layer: the class that a layer starts with, and the form
  of a making act. Pure code. It needs no cluster.

  A layer is made by one act, its first, so that the making of a layer is in
  the acts depot. A gate accepts an act into a layer for which the record
  holds nothing only when it is a making act: it writes the layer's class
  and one permission, the layer's first, with nothing above it, and it names
  that permission as the one it acts under. The form of a making act is the
  builder's, not a decision of Sid's.

  Inputs and results, by call:

    class-at-making  a layer id -> the class that the layer starts with
    making-act?      a gate's class, and an act in its one form on arrival
                     -> true or false

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

(defn class-at-making
  "Takes a layer id. Gives the class that the layer starts with: :microbatch
  for the base, :streaming for any other layer. The one place where that
  decision (F4 C) is code."
  [layer]
  (if (= base layer) :microbatch :streaming))

(defn making-act?
  "Takes the class of a gate, and an act in its one form on arrival. Gives
  true when the act has the form of a making act for that gate. All of these
  hold:

  1. the gate's class is the class that the act's layer starts with;
  2. the act's permission is [holder layer layer]: three slots, nothing
     above it, granted in the layer itself;
  3. the act's `:who` is the holder (a group's layer, made by the person who
     creates the group) or `system` (a personal layer, made for its person);
     for the base the holder is `system`, and `system` can be the holder of
     another layer too;
  4. the act has exactly two facts, in either order: the class that the
     layer starts with, on the layer's own entity under `:class`; and the
     act's own permission, on its holder under `:permission`.

  The form says nothing about `:replaces`: a making act that names something
  to replace is stale at the gate's check of replaced facts, because nothing
  is current in a layer that is not made.

  Limit. It reads no record. True says only that the act has the form; the
  caller must also know that the record holds nothing for the layer."
  [gate-class act]
  (let [layer (:layer act)
        permission (:permission act)
        holder (first permission)
        starts-as (class-at-making layer)]
    (boolean
     (and (= gate-class starts-as)
          (= 3 (count permission))
          (= layer (nth permission 1))
          (= layer (nth permission 2))
          (or (= (:who act) holder) (= (:who act) system))
          (or (not= base layer) (= holder system))
          (= 2 (count (:facts act)))
          (= #{{:e layer :k :class :v starts-as}
               {:e holder :k :permission :v permission}}
             (into #{} (map #(select-keys % [:e :k :v])) (:facts act)))))))
