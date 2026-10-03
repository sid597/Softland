;; IMPORTANT: Before modifying this file, re-read PLAN-stream-store.md,
;; "Hybrid clock (26 September)".
(ns rig.store.clock
  "The store's stamp: a hybrid of wall-clock milliseconds and a counter,
  kept as one long so that \"as of T\" stays one number (CONCLUSION.md R2,
  a default, not a ruling). The encoding lives here and nowhere else, so it
  can change in one place.

  Tonight's encoding, first-record: stamp = ms × 65536 + counter, the
  counter 0 to 65535. Ordering stamps as longs orders them by millisecond,
  then by counter. A unit's next stamp is max(ms-now × 65536, its last
  stamp + 1, every stamp the fact stood on + 1): never backward within the
  unit, never earlier than anything the fact stood on (ruling 4), and at
  the wall's own millisecond as long as the unit gives at most 65,536
  stamps in it. Past that the counter carries into the millisecond part,
  and the stamps run ahead of the wall until it catches up.

  Pure. Longs only, with Clojure's checked arithmetic: an overflow throws,
  and the gate's guard turns that into a refusal rather than a wrapped
  stamp (R16 keeps a carried stamp far below where that could happen).")

(def counter-bits "Bits of a stamp that hold the counter." 16)

(def counter-span "Counter values per millisecond: 65536." (bit-shift-left 1 counter-bits))

(def max-counter "The largest counter a millisecond holds: 65535." (dec counter-span))

(defn pack
  "The stamp at millisecond `ms` with counter `counter` (0 to 65535)."
  [ms counter]
  (+ (* (long ms) counter-span) (long counter)))

(defn ms-of
  "A stamp's wall-clock millisecond part."
  [stamp]
  (bit-shift-right (long stamp) counter-bits))

(defn counter-of
  "A stamp's counter part, 0 to 65535."
  [stamp]
  (bit-and (long stamp) max-counter))

(defn next-stamp
  "The next stamp a unit gives, for a decision made when the wall reads
  `ms-now` milliseconds, on a unit whose last stamp is `last` (0 before its
  first), for a fact that stood on the stamps `stood-on` (any seqable of
  stamps, possibly empty): max(ms-now × 65536, last + 1, each stood-on + 1)."
  [ms-now last stood-on]
  (reduce (fn [m s] (max m (inc (long s))))
          (max (pack ms-now 0) (inc (long last)))
          stood-on))
