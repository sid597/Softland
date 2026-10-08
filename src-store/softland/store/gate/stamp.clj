(ns softland.store.gate.stamp
  "The stamp: one integer that holds the clock time and a counter. Pure code.
  It needs no cluster, and it reads no clock: the gate reads the clock once
  for a decision and gives the number.

  A gate gives one stamp to each decision, accepted or refused. A stamp is
  strictly increasing on its task, later than each stamp that the act
  carries, and below 2^62.

  The packing. stamp = milliseconds since 1970 x 65,536 + counter: the low
  16 bits are the counter, the bits above them the clock time. One clock
  tick, a millisecond, holds 65,536 stamps for a task, which is far more
  than a task decides in a millisecond; so the time in a stamp does not run
  ahead of the clock because of load. That is what the finding asks: plain
  milliseconds ran ahead under load. The packing reaches 2^62 when the clock
  reaches 2^46 milliseconds, in the year 4199.

  Inputs and results, by call:

    next-stamp        the clock now, the task's last stamp or nil, the
                      greatest carried stamp or nil -> the stamp
    too-far-ahead?    the clock now, a carried stamp -> true or false
    greatest-carried  an act in its one form -> its greatest carried stamp,
                      or nil
    pack, time-of, counter-of   the two halves of a stamp

  State and effects. None. Where the last stamp of a task is kept is the
  gate's: it is read from the record at each decision. `next-stamp` takes it
  as a number, so the greatest of several tasks' last stamps is the same
  call.")

(def counter-bits
  "How many low bits of a stamp are the counter."
  16)

(def max-ahead-millis
  "How far ahead of the gate's clock a carried stamp can be: 10 seconds. A
  number of the build, kept and not decided. Without it a forged stamp could
  push a task's stamps to the end of their range."
  10000)

(defn pack
  "Takes a clock time in milliseconds and a counter of 0 to 65,535. Gives the
  stamp that holds both. It never throws; what must hold for a stamp below
  2^62: the clock time is below 2^46."
  [millis counter]
  (bit-or (bit-shift-left (long millis) counter-bits) (long counter)))

(defn time-of
  "Takes a stamp that is not below zero. Gives the clock time that it holds,
  in milliseconds."
  [stamp]
  (bit-shift-right (long stamp) counter-bits))

(defn counter-of
  "Takes a stamp. Gives the counter that it holds, 0 to 65,535."
  [stamp]
  (bit-and (long stamp) (dec (bit-shift-left 1 counter-bits))))

(defn next-stamp
  "The stamp of one decision. Takes the clock now in milliseconds; the last
  stamp that the gate gave on the task, or nil before its first decision;
  and the greatest stamp that the act carries, or nil. Gives the greatest
  of: the clock now with a counter of zero; the last stamp plus one; the
  greatest carried stamp plus one.

  So it is later than the last stamp when the clock went back, when the
  task's leader moved to a node whose clock is behind, and when one tick had
  more decisions than the counter holds: \"last plus one\" then carries into
  the time bits, and the time in the stamp is ahead until the clock passes
  it.

  It never throws. What must hold for a stamp below 2^62: the clock is
  before the year 4199, and the last and the carried stamp are below 2^62.
  A carried stamp is, by the form; and the gate refuses an act that carries
  one more than `max-ahead-millis` ahead before it makes a stamp."
  [now-millis last-stamp greatest-carried]
  (cond-> (pack now-millis 0)
    last-stamp (max (unchecked-inc (long last-stamp)))
    greatest-carried (max (unchecked-inc (long greatest-carried)))))

(defn too-far-ahead?
  "The 10-second rule. Takes the clock now in milliseconds and a carried
  stamp. Gives true when the clock time in the stamp is more than
  `max-ahead-millis` past the clock now."
  [now-millis carried-stamp]
  (> (time-of carried-stamp) (+ (long now-millis) max-ahead-millis)))

(defn greatest-carried
  "Takes an act in its one form. Gives the greatest stamp that it carries,
  or nil when it carries none. The stamps that an act carries are the values
  of its `:based-on`. `:claimed-when` is the maker's claim and is no carried
  stamp."
  [act]
  (when-let [stamps (seq (vals (:based-on act)))]
    (reduce max stamps)))
