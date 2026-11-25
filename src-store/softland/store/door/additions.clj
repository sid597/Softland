(ns softland.store.door.additions
  "What the door makes of one act: the tests of its own three additions, and
  the thing to append. Pure code. It reads no clock: the door hands it the
  time.

  The door checks nothing of the act's form: a gate does. What is tested
  here is only what the door's own additions need, so that the thing is no
  stray on the depot that its act-id names and a gate answers it. A thing
  that failed would get no answer, ever, and its maker could not tell it
  from an act that is on its way.

  The door reads one part of the thing, `:lane`. Everything that the maker
  gave is appended as she gave it, and a depot keeps it for good. The time
  is added as the door read it and is not tested here: a gate holds it to
  the declaration's range.

  No function throws for what a maker gave: a thing that throws while it is
  read ends in a word."
  (:require [softland.store.door.declaration :as declaration]
            [softland.store.door.words :as words]))

(defn- plain-map?
  "The test that the declaration makes of a thing; it is private there, so
  the door writes the line once more. The pure proof holds it against
  `softland.store.door.declaration/stray-reason`."
  [x]
  (and (map? x) (not (record? x))))

(defn series-word
  "nil for a fixed-width integer of 1 or more; the word for anything else, a
  big integer and a number with a fraction among them."
  [n]
  (when-not (and (int? n) (<= 1 n))
    (words/word :make-an-act-id {:at :count})))

;; ------------------------------------------------------------- the reading

(def ^:private parts-read
  "The door reads `:lane` alone: an act-id of the door's making holds it,
  and it names the depot to ask for."
  [:lane])

(defn- read-parts
  "The word `:read-the-act` at the part whose read threw. A sorted map whose
  keys cannot be compared with a keyword throws there."
  [thing]
  (reduce (fn [got part]
            (let [read (try [(get thing part)] (catch Throwable _ nil))]
              (if read
                (assoc got part (first read))
                (reduced (words/word :read-the-act {:at part})))))
          {}
          parts-read))

(def ^:private parts-added
  "In the order in which `own-parts-word` tests them."
  [:act-id :depot declaration/time-part])

(defn- own-parts-word
  "A part that is there with nil counts: the door writes over nothing."
  [thing]
  (when-let [part (some (fn [part] (when (contains? thing part) part)) parts-added)]
    (words/word :add-its-parts {:at part})))

(defn- act-id-word
  "The two tests are the declaration's, the ones that a read's head makes.
  An act-id of the lane whose depot slot is a depot word and not the
  lane's depot passes: the thing then goes to the depot that the act-id
  names, and a gate refuses it there with a recorded word."
  [lane act-id]
  (cond
    (not (declaration/usable-id? lane))
    (words/word :make-an-act-id {:at :lane})

    (and (some? act-id) (not (declaration/act-id-in-lane? lane act-id)))
    (words/word :make-an-act-id {:at :act-id})))

(defn- reading*
  [thing act-id]
  (if-not (plain-map? thing)
    (words/word :read-the-act nil)
    (let [parts (read-parts thing)]
      (or (when (:could-not parts) parts)
          (own-parts-word thing)
          (act-id-word (:lane parts) act-id)
          {:thing thing
           :lane (:lane parts)}))))

(defn reading
  "Reads a maker's thing, with no round trip. Gives {:thing :lane}, or one
  of the door's own words; with a word, nothing is appended. The first test
  that fails gives the word: `:read-the-act`, then `:add-its-parts`, then
  `:make-an-act-id`.

  It reads no fact, no who and no permission, so it comes back for a thing
  that has no end in one of those. It never throws."
  [thing act-id]
  (try (reading* thing act-id)
       (catch Throwable _ (words/word :read-the-act nil))))

;; ------------------------------------------------------ the thing to append

(defn thing-to-append
  "`act-id` is one that the reading's tests passed, or that the door made
  for the reading's lane. `door-time` is in milliseconds since 1970; it is
  added as given and not tested. Each other part is the maker's own object.

  It never throws: a thing that throws while the three parts are added ends
  in the word `:read-the-act`."
  [reading act-id door-time]
  (try
    {:thing (assoc (:thing reading)
                   :act-id act-id
                   :depot (nth act-id 1)
                   declaration/time-part door-time)}
    (catch Throwable _ (words/word :read-the-act nil))))
