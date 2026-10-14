(ns softland.store.gate.stamp
  "The stamp: one integer that holds a time and a counter. Pure code. It
  needs no cluster, and it reads no clock: a gate gives each number.

  A gate gives one stamp to each decision, accepted or refused. In both
  gates a stamp is strictly increasing in its layer: it is later than the
  last stamp that the gate gave in that layer. The order is the layer's
  and not the task's: two decisions in two layers of one task can have one
  stamp.

  Two more things hold of a stamp, each with its exception.

    - The stamp of an accepted act is later than each stamp that the act
      carries. The stamp of a refusal is not: it is the layer's last stamp
      plus one, and nothing of what the refused thing carries is in it
      (`softland.store.gate.decision/stamp-of`).
    - A stamp is below 2^62, the bound of a stamp that an act can carry,
      until its layer has had about 2^61 decisions. \"Last plus one\" has
      no bound of its own; `softland.store.door.declaration/max-time` holds
      the arithmetic.

  Where the time comes from. No gate reads a clock. A gate gives the time
  on the act, which the door writes from the clock of the server that runs
  the door (`softland.store.door/send-act`), or whatever else appended the
  thing wrote there: this code knows only that the part holds a number in
  the range. And it gives the last stamp of the act's layer. For a
  refusal, a thing with no usable time among them, it gives no time, and
  the stamp is the layer's last stamp plus one. Sid's decisions of 13 October 2026
  say this: \"The door writes the time on the act. No gate reads a
  clock\", and \"A number counted per layer\". Nothing of a batch is in a
  stamp, so a gate that decides the same things in the same order gives
  the same stamps however its batches are cut.

  The packing. stamp = milliseconds since 1970 x 65,536 + counter: the low
  16 bits are the counter, the bits above them the time. One millisecond
  holds 65,536 stamps for a layer, which is far more than a gate decides
  in one layer in a millisecond; so the time in a stamp does not run ahead
  of the time that the gate gave because of load. That is what the finding
  asks: plain milliseconds ran ahead under load. The packing reaches 2^62
  when the time reaches 2^46 milliseconds, in the year 4199; the
  declaration holds the time on an act below half of that, 2^45
  milliseconds, in the year 3084
  (`softland.store.door.declaration/max-time` says why half).

  The time in a stamp is not always the time on its act. The stamp of an
  accept is the greatest of three numbers (`next-stamp`), so an act whose
  time is not later than the time in its layer's last stamp gets that
  stamp plus one, and an act that carries a later stamp gets that one plus
  one. The stamp of a refusal never holds the time on its thing.

  Inputs and results, by call:

    next-stamp        the time on the act or nil, the layer's last stamp or
                      nil, the greatest carried stamp or nil -> the stamp
    too-far-ahead?    the time on the act or nil, a carried stamp -> true
                      or false
    greatest-carried  an act in its one form -> its greatest carried stamp,
                      or nil
    pack, time-of, counter-of   the two halves of a stamp

  State and effects. None. Where a last stamp is kept is the gate's: the
  walk of one thing (`softland.store.module.walk`) reads the layer's from
  the gate's record at each decision. `next-stamp` takes it as a number.")

(def counter-bits
  "How many low bits of a stamp are the counter."
  16)

(def max-ahead-millis
  "How far ahead of the time on its act a carried stamp can be: 10 seconds.
  A number of the build, kept and not decided. Without it a carried stamp
  alone could push a layer's stamps to the end of their range. It holds the
  carried stamps and not the time on the act itself
  (`softland.store.door.declaration/max-time` says what holds that). Both
  gates hold a carried stamp to it."
  10000)

(defn pack
  "Takes a time in milliseconds and a counter of 0 to 65,535. Gives the
  stamp that holds both. It never throws; what must hold for a stamp below
  2^62: the time is not below zero and is below 2^46."
  [millis counter]
  (bit-or (bit-shift-left (long millis) counter-bits) (long counter)))

(defn time-of
  "Takes a stamp that is not below zero. Gives the time that it holds, in
  milliseconds."
  [stamp]
  (bit-shift-right (long stamp) counter-bits))

(defn counter-of
  "Takes a stamp. Gives the counter that it holds, 0 to 65,535."
  [stamp]
  (bit-and (long stamp) (dec (bit-shift-left 1 counter-bits))))

(defn next-stamp
  "The stamp of one decision. Takes the time on the act in milliseconds, or
  nil for a thing with no usable time; the last stamp that the gate gave in
  the act's layer, or nil before the layer's first decision; and the
  greatest stamp that the act carries, or nil. Gives the greatest of: the
  time with a counter of zero, when there is a time; the last stamp plus
  one, when there is one; the greatest carried stamp plus one, when there
  is one. With none of the three it gives 0.

  So it is later than the layer's last stamp also when the time on the act
  is not later than the time in that stamp: when the clock of the act's
  door is behind the clock that wrote an earlier act's time, or went back;
  when a thing has no time; and when one millisecond had more decisions
  than the counter holds. \"Last plus one\" then goes on from the last
  stamp, into the time bits when the counter is full, and the time in the
  stamp is ahead of the time on the act until the time on a later act
  passes it.

  It never throws. What must hold for a stamp below 2^62: the time is below
  2^46, and the last and the carried stamp are below 2^62 less one. The
  callers hold more than that. The declaration holds the time on an act
  below 2^45. A carried stamp is below 2^62 by the form, and the gate
  refuses an act that carries one more than `max-ahead-millis` ahead of its
  time before it makes a stamp. So the time and the carried stamp plus one
  give 655,360,000 above 2^61 at most
  (`softland.store.door.declaration/max-time`). Limit: \"last plus one\"
  has no bound here. It passes 2^62 after about 2^61 decisions in one
  layer, and an act cannot carry such a stamp."
  [time-millis last-stamp greatest-carried]
  (let [of-the-time (when time-millis (pack time-millis 0))
        after-the-last (when last-stamp (unchecked-inc (long last-stamp)))
        after-the-carried (when greatest-carried (unchecked-inc (long greatest-carried)))
        given (remove nil? [of-the-time after-the-last after-the-carried])]
    (if (seq given)
      (long (reduce max given))
      0)))

(defn too-far-ahead?
  "The 10-second rule. Takes the time on the act in milliseconds, or nil for
  a thing with no usable time, and a carried stamp. Gives true when the
  time in the stamp is more than `max-ahead-millis` past the time on the
  act. False for nil: with no time there is nothing to hold the stamp
  against."
  [time-millis carried-stamp]
  (boolean (and time-millis
                (> (time-of carried-stamp) (+ (long time-millis) max-ahead-millis)))))

(defn greatest-carried
  "Takes an act in its one form. Gives the greatest stamp that it carries,
  or nil when it carries none. The stamps that an act carries are the values
  of its `:based-on`. `:claimed-when` is the maker's claim and is no carried
  stamp."
  [act]
  (when-let [stamps (seq (vals (:based-on act)))]
    (reduce max stamps)))
