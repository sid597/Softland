(ns softland.server.session.lately
  "The feed of the sessions that changed lately: its value, what a change
  does to it, and what one copy of the server takes from it. Pure code.

  Why there is a feed. A request proves who it is with each request: one
  read of its hash's row. A socket or a stream proves it once and then
  stays open, on one copy of the server, while her session can end through
  another copy. So each copy must learn that a row changed without asking
  for each open thing. One push of Rama's on one small value serves a whole
  copy, however many sockets it holds; a push for each socket would be
  counted against Rama's bound on the pushes of one table on one task.

  Why the value holds the last changes and not the last one. A push of
  Rama's tells the present and not each event: Rama can leave a reader out
  of a change and send the value anew
  (src-store/softland/store/read_path.clj says the same of a live read). So
  the value is state: the rows of the hashes that changed in the last
  `kept` changes, each with the number of its change.

    {:next n                         the number that the next change gets
     :changed {hash {:seq n :row row}}}

  A copy remembers the `:next` that it saw last. From a new value it takes
  each entry whose number is not below that. When the value's oldest number
  is above what the copy saw, changes left the value before the copy saw
  them: the copy is told that it missed some, and asks for the row of every
  hash that it listens for.

  Inputs and results, by call:

    start   nothing -> the value of a feed that no change has reached
    after   a value, a hash and the hash's row now -> the value after it
    news    what a copy saw last, or nil, and a value ->
            {:seen :rows :missed?}

  State and effects. None.

  Callers. `softland.server.module`, in its topology, and
  `softland.server.sessions`, in the callback of its one push.")

(def kept
  "How many changes the feed holds: by how many changes a copy can be
  behind before it reads every row that it listens for again. A number of
  the build, kept and not decided. Whether Rama ever leaves a copy that far
  behind was not seen: 400 ends from 16 threads gave a callback for each
  (a probe of the review of 20 November 2026). The whole value is written
  and pushed at each change, so a larger number costs each copy more bytes
  at each sign-in and sign-out."
  32)

(defn start
  "Gives the value of a feed that no change has reached."
  []
  {:next 0 :changed {}})

(defn after
  "Takes the feed's value, a hash and the hash's row as it is now. Gives the
  value after that change: the hash's entry holds the row under the next
  number, an entry whose number has left the last `kept` is gone, and
  `:next` is one more. A hash has one entry, its latest."
  [feed hash row]
  (let [n (:next feed)
        oldest (- (inc n) kept)]
    {:next (inc n)
     :changed (into {}
                    (remove (fn [[_ entry]] (< (:seq entry) oldest)))
                    (assoc (:changed feed) hash {:seq n :row row}))}))

(defn news
  "What one copy of the server takes from a value of the feed. Takes the
  `:next` that the copy saw last, or nil for a copy that has seen no value,
  and the value. Gives:

    :seen     what the copy has seen after this value
    :rows     [[hash row] ...] for each change that it had not seen, in
              the order of the changes
    :missed?  true when changes left the feed before the copy saw them,
              or the feed's numbers went back, as they do when the module
              is launched anew: the copy then reads the row of every hash
              that it listens for, and a listener whose row is not there is
              told that the sessions could not be reached
              (`softland.server.sessions`)

  A copy that has seen no value takes none of the entries: nothing listened
  before its first value."
  [seen feed]
  (let [next (long (:next feed 0))
        oldest (max 0 (- next kept))]
    (cond
      (nil? seen) {:seen next :rows [] :missed? false}
      (< next seen) {:seen next :rows [] :missed? true}
      :else {:seen next
             :missed? (< seen oldest)
             :rows (->> (:changed feed)
                        (filter (fn [[_ entry]] (<= seen (:seq entry))))
                        (sort-by (fn [[_ entry]] (:seq entry)))
                        (mapv (fn [[hash entry]] [hash (:row entry)])))})))
