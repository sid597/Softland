(ns softland.store.door.wait
  "The numbers of a maker's wait for an answer. Pure code. It needs no
  cluster, and it requires nothing outside Clojure and the JVM.

  A maker reads the answer of an act by its act-id, and may wait for it
  (`softland.store.door/answer-of`). The wait is one read, and then a sleep
  and another read until an answer is there or the maker's bound has
  passed. Two numbers shape it: how long the wait sleeps between two reads,
  which the opener of a door may give, and the longest wait that the door
  counts, which is a constant.

  Inputs and results, by call:

    numbers  what the opener of a door gave, a map or nil -> the door's
             numbers, a map

  `longest-wait-ms` and `default-numbers` are declared here too.

  State and effects. None. `numbers` is pure.

  Callers. `softland.store.door`: `open` asks `numbers`, and `answer-of`
  cuts a maker's bound to `longest-wait-ms`.

  Not here. The clock, the sleep and the reads of a wait:
  `softland.store.door`.")

(def longest-wait-ms
  "The longest wait that the door counts, in milliseconds: about 73 years. A
  wait is counted in nanoseconds of a clock that never goes back, and a
  longer one would leave the range of a long there. A number of the build."
  (quot Long/MAX_VALUE 4000000))

(def default-numbers
  "The door's numbers, with the default of each. There is one. The opener of
  the door may give another for it (`numbers`); it is not measured, and it
  is no decision of Sid's.

    :read-every-ms 200   how long a maker's wait in `answer-of` sleeps
        between two reads of an answer. Each read of a wait but the last
        finds nothing, and each is a round trip and a read on a task whose
        one thread a gate also needs. At 200 ms an answer that is there
        after 0.9 s costs 6 reads and is learnt 100 ms late at the mean; at
        20 ms it costs 46. A microbatch layer is not for what a person does
        directly, so a tenth of a second there is a small price."
  {:read-every-ms 200})

(defn numbers
  "Takes what the opener of a door gave for the door's numbers: a map, or
  nil for nothing. Gives the door's numbers as a map: `default-numbers`,
  with the opener's `:read-every-ms` in the place of the default when it is
  a fixed-width integer of 1 or more, cut to `longest-wait-ms`. Any other
  name in what the opener gave is not read. It never throws: a thing that
  is no map, and a map that throws when it is read, give the default."
  [given]
  (let [x (try (get given :read-every-ms) (catch Throwable _ nil))]
    (if (and (int? x) (<= 1 x))
      (assoc default-numbers :read-every-ms (min (long x) longest-wait-ms))
      default-numbers)))
