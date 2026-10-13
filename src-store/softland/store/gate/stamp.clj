(ns softland.store.gate.stamp
  "The stamp: one integer that holds a time and a counter. Pure code. It
  needs no cluster, and it reads no clock: a gate gives each number.

  A gate gives one stamp to each decision, accepted or refused. In the
  streaming gate a stamp is strictly increasing in its layer: it is later
  than the last stamp that the gate gave in that layer. It is later than
  each stamp that the act carries, and below 2^62. The order is the
  layer's and not the task's: two decisions in two layers of one task can
  have one stamp.

  Where the time comes from. The streaming gate reads no clock. It gives
  the time on the act, which the door wrote from the clock of the server
  that runs the door (`softland.store.door/send-act`), and the last stamp
  of the act's layer. For a thing with no usable time it gives nil, and the
  stamp is the layer's last stamp plus one. Sid's decisions of 13 October
  2026 say this: \"The door writes the time on the act. No gate reads a
  clock\", and \"A number counted per layer\". The microbatch gate is not
  built so: it gives the clock of its batch, which it reads once for a
  batch and keeps, and the one last stamp of the whole gate
  (`softland.store.gate.batch`).

  The packing. stamp = milliseconds since 1970 x 65,536 + counter: the low
  16 bits are the counter, the bits above them the time. One millisecond
  holds 65,536 stamps for whatever the last stamp is kept for, a layer in
  the streaming gate, which is far more than a gate decides there in a
  millisecond; so the time in a stamp does not run ahead of the time that
  the gate gave because of load. That is what the finding asks: plain
  milliseconds ran ahead under load. The packing reaches 2^62 when the time
  reaches 2^46 milliseconds, in the year 4199; the declaration holds the
  time on an act below that (`softland.store.door.declaration/max-time`).

  The time in a stamp is not always the time on its act. A stamp is the
  greatest of three numbers (`next-stamp`), so an act whose time is not
  later than the time in its layer's last stamp gets that stamp plus one,
  and an act that carries a later stamp gets that one plus one.

  Inputs and results, by call:

    next-stamp        the time on the act or nil, the layer's last stamp or
                      nil, the greatest carried stamp or nil -> the stamp
    too-far-ahead?    the time on the act or nil, a carried stamp -> true
                      or false
    greatest-carried  an act in its one form -> its greatest carried stamp,
                      or nil
    pack, time-of, counter-of   the two halves of a stamp

  State and effects. None. Where a last stamp is kept is the gate's: the
  streaming gate reads the layer's from the record at each decision.
  `next-stamp` takes it as a number.")

(def counter-bits
  "How many low bits of a stamp are the counter."
  16)

(def max-ahead-millis
  "How far ahead of the time on its act a carried stamp can be: 10 seconds.
  A number of the build, kept and not decided. Without it a carried stamp
  alone could push a layer's stamps to the end of their range. It holds the
  carried stamps and not the time on the act itself
  (`softland.store.door.declaration/max-time` says what holds that). The
  microbatch gate holds a carried stamp to the same span past the clock of
  its batch."
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

  The microbatch gate calls it with the clock of its batch as the time and
  with the last stamp of the whole gate.

  It never throws. What must hold for a stamp below 2^62: the time is below
  2^46, as the declaration holds the time on an act; and the last and the
  carried stamp are below 2^62 less one. A carried stamp is below 2^62 by
  the form, and the gate refuses an act that carries one more than
  `max-ahead-millis` ahead of its time before it makes a stamp. Limit: in a
  layer whose last stamp holds a time at the end of that range, \"last plus
  one\" passes 2^62 when the counter is full, and an act cannot carry such
  a stamp."
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
