(ns softland.store.gate.stamp
  "The stamp: the position of one decision in its lane. Pure code. It reads
  no clock.

  A gate gives each decision of a lane, accepted or refused, the lane's
  last stamp plus one. So a lane's stamps are 0, 1, 2 and on, with no gap,
  in the order of its things on the lane's depot partition. The order is
  the lane's and not the task's: two decisions in two lanes of one task
  can have one stamp. A gate that decides the same things in the same order
  gives the same stamps, however its batches are cut and whenever it runs.

  A count and not the depot's own offset: a depot partition holds the
  things of every lane that hashes to it, so its offsets are no gapless
  count of one lane; and Rama gives a stream event no offset of its record
  (a reading of the names in Rama's jar, in the build \"run again\"; not a
  run).

  Order never uses the time. The time on an act is a label beside the
  stamp. No rule reads it, so a wrong clock gives a wrong label and changes
  no stamp and no decision.

  No gate reads the stamps in an act's `:based-on`, and an act's stamp is
  not raised above them. That an act came after the facts that it is based
  on is read from its `:based-on`, by their fact ids, and not from the size
  of two stamps. A stamp of one lane is no measure against a stamp of
  another.")

(defn next-stamp
  "Wraps after 2^63 - 1 decisions in one lane."
  [last-stamp]
  (if last-stamp
    (unchecked-inc (long last-stamp))
    0))
