(ns softland.store.gate.stamp
  "The stamp: the position of one decision in its layer. Pure code. It
  needs no cluster, and it reads no clock.

  A gate gives one stamp to each decision, accepted or refused: the layer's
  last stamp plus one, or 0 for the layer's first decision. So the stamps
  of a layer are 0, 1, 2 and on, with no gap, in the order in which the
  gate decided the layer's things, which is the order of those things on
  the layer's depot partition. The order is the layer's and not the
  task's: a task decides many layers, and two decisions in two layers of
  one task can have one stamp. A gate that decides the same things in the
  same order gives the same stamps, however its batches are cut and
  whenever it runs.

  Why a count and not the depot's own offset. A depot partition holds the
  things of every layer that hashes to it, so its offsets are no gapless
  count of one layer; and Rama gives a stream event no offset of its
  record (a reading of the names in Rama's jar, in the build \"run again\";
  not a run).

  What a stamp does not hold. Order and time are kept apart, and order
  never uses the time (Sid's decision of 14 October 2026).

    - No time. The time on an act, which the door writes, is a label beside
      the stamp, in the answer's row and in each fact's row
      (`softland.store.gate.decision/writes`). No rule reads it, so a
      wrong clock gives a wrong label and changes no stamp and no
      decision.
    - Nothing of what the act carries. The stamps in an act's `:based-on`
      are what its maker saw, and no gate reads them: an act's stamp is not
      raised above them (Sid's decision of 14 October 2026). That an act
      came after the facts that it is based on is read from its
      `:based-on`, by their fact ids, and not from the size of two stamps.
      A stamp of one layer is no measure against a stamp of another.

  Limit. \"Last plus one\" has no bound of its own: it wraps after 2^63 - 1
  decisions in one layer.

  Inputs and results:

    next-stamp   the layer's last stamp, or nil -> the stamp

  State and effects. None. Where a last stamp is kept is the gate's: the
  walk of one thing (`softland.store.module.walk`) reads the layer's from
  the gate's record at each decision and writes the new one.")

(defn next-stamp
  "The stamp of one decision, an accept or a refusal. Takes the last stamp
  that the gate gave in the thing's layer, or nil before the layer's first
  decision. Gives that stamp plus one, or 0 for nil. It never throws.
  Limit: it wraps after 2^63 - 1 decisions in one layer."
  [last-stamp]
  (if last-stamp
    (unchecked-inc (long last-stamp))
    0))
