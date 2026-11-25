(ns softland.store.door.wait
  "The numbers of a maker's wait for an answer
  (`softland.store.door/answer-of`): how long the wait sleeps between two
  reads, and the longest wait that the door counts. Pure code.")

(def longest-wait-ms
  "In milliseconds: about 73 years. A wait is counted in nanoseconds of a
  clock that never goes back, and a longer one would leave the range of a
  long there."
  (quot Long/MAX_VALUE 4000000))

(def default-numbers
  "`:read-every-ms` is how long a maker's wait in `answer-of` sleeps between
  two reads of an answer. Each read of a wait but the last finds nothing,
  and each is a round trip and a read on a task whose one thread a gate
  also needs. At 200 ms an answer that is there after 0.9 s costs 6 reads
  and is learnt 100 ms late at the mean; at 20 ms it costs 46. A microbatch
  lane is not for what a person does directly, so a tenth of a second there
  is a small price."
  {:read-every-ms 200})

(defn numbers
  "The opener's `:read-every-ms` is taken when it is a fixed-width integer
  of 1 or more, cut to `longest-wait-ms`. Any other name in what the opener
  gave is not read. It never throws: a thing that is no map, and a map that
  throws when it is read, give the default."
  [given]
  (let [x (try (get given :read-every-ms) (catch Throwable _ nil))]
    (if (and (int? x) (<= 1 x))
      (assoc default-numbers :read-every-ms (min (long x) longest-wait-ms))
      default-numbers)))
