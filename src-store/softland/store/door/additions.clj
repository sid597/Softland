(ns softland.store.door.additions
  "What the door makes of one act: the tests of its own three additions, and
  the thing to append. Pure code. It needs no cluster, and it reads no
  clock: the door hands it the time.

  The door adds three things to a maker's act: an act-id, the depot that
  the act-id names, and the time of the door's clock, under `:door-time`
  (`softland.store.door.declaration/time-part` holds the name). This
  namespace makes the act-id and the depot right, or gives one
  of the door's own words (`softland.store.door.words`), and then nothing is
  appended for the act. An addition is right when the door's declaration
  (`softland.store.door.declaration`) takes it and when it wrote over
  nothing that the maker gave. The time is added as the door read it and is
  not tested here: a gate holds it to the declaration's range.

  The door checks nothing of the act's form: a gate does, and a gate's
  refusal is in the record. What is tested here is only what the door's own
  additions need:

    - the thing is a plain map whose `:layer` can be read;
    - it does not already hold `:act-id`, `:depot` or `:door-time`;
    - its `:layer` is an id that an act-id can hold, and an act-id that the
      maker gives back is an act-id of that layer. A thing that passes is no
      stray on the depot that its act-id names, so a gate answers it. A
      thing that failed would get no answer, ever, and its maker could not
      tell it from an act that is on its way.

  The door reads one part of the thing, `:layer`. It reads no fact, no who
  and no permission. Everything that the maker gave is appended as she gave
  it, and a depot keeps it for good: each value, plain; each id; `:facts`
  as the vector, the list or the other thing that it is; a part that the
  form does not know. A gate refuses such a thing where the form is wrong;
  the depot's log keeps it all the same.

  Inputs and results, by call:

    reading          a maker's thing, and the act-id that the maker gives
                     back or nil -> what the door read of it, or a word
    thing-to-append  a reading, an act-id, a time -> {:thing the thing to
                     append}, or a word
    series-word      the count of a series of act-ids -> nil, or a word

  State and effects. None. No function throws for what a maker gave: a
  thing that throws while it is read ends in a word.

  Callers. `softland.store.door`, and the pure proof.

  Not here. The making of an act-id, which needs the layer's depot, a read
  of the cluster, and a random UUID: `softland.store.door`. The read of the
  clock: `softland.store.door`. The digest of
  the thing: `softland.store.door.digest`, taken by `softland.store.door`.
  The check of the act: a gate's."
  (:require [softland.store.door.declaration :as declaration]
            [softland.store.door.words :as words]))

(defn- plain-map?
  "True for a map that is no record. The same test that the declaration and
  the act's check make of a thing and of a fact; it is private in each of
  those files, so the door writes the line once more, here. The pure proof
  holds it against `softland.store.door.declaration/stray-reason`."
  [x]
  (and (map? x) (not (record? x))))

(defn series-word
  "Takes the count of a series of act-ids. Gives nil for a fixed-width
  integer of 1 or more, or the word `:make-an-act-id` at `:count`: for a
  count below 1, and for anything else, a big integer and a number with a
  fraction among them."
  [n]
  (when-not (and (int? n) (<= 1 n))
    (words/word :make-an-act-id {:at :count})))

;; ------------------------------------------------------------- the reading

(def ^:private parts-read
  "The parts of a maker's thing that the door reads: one, `:layer`, which an
  act-id of the door's making holds and which names the depot to ask for.
  The door checks nothing of it but that an act-id can hold it."
  [:layer])

(defn- read-parts
  "Takes a thing that is a plain map. Gives a map of the parts of
  `parts-read`, each as the thing holds it, nil for a part that is absent;
  or the word `:read-the-act` at the part whose read threw, which is
  `:layer`. A sorted map whose keys cannot be compared with a keyword
  throws there."
  [thing]
  (reduce (fn [got part]
            (let [read (try [(get thing part)] (catch Throwable _ nil))]
              (if read
                (assoc got part (first read))
                (reduced (words/word :read-the-act {:at part})))))
          {}
          parts-read))

(def ^:private parts-added
  "The parts of the act that the door adds, in the order in which
  `own-parts-word` tests them: the act-id, the depot that it names, and the
  time of the door's clock. The name of the third is the declaration's
  (`softland.store.door.declaration/time-part`)."
  [:act-id :depot declaration/time-part])

(defn- own-parts-word
  "Takes a thing that is a plain map. Gives nil when it holds none of
  `parts-added`, the three parts of the act that the door adds: `:act-id`,
  `:depot` and `:door-time`; or the word `:add-its-parts` at the first of
  them that it holds. A part that is there with nil counts: the door writes
  over nothing."
  [thing]
  (when-let [part (some (fn [part] (when (contains? thing part) part)) parts-added)]
    (words/word :add-its-parts {:at part})))

(defn- act-id-word
  "Takes the thing's `:layer`, and the act-id that the maker gives back, or
  nil for none. Gives nil when an act-id of the layer can be made, and the
  one given is one; or the word `:make-an-act-id`, at `:layer` or at
  `:act-id`. The two tests are the declaration's, the ones that a read's
  head makes. An act-id of the layer whose depot slot is a depot word and
  not the layer's depot passes: the thing then goes to the depot that the
  act-id names, and a gate refuses it there with a recorded word."
  [layer act-id]
  (cond
    (not (declaration/usable-id? layer))
    (words/word :make-an-act-id {:at :layer})

    (and (some? act-id) (not (declaration/act-id-in-layer? layer act-id)))
    (words/word :make-an-act-id {:at :act-id})))

(defn- reading*
  "The body of `reading`, with no guard around it. It can throw, for a
  thing that throws when it is asked whether it holds a part; `reading`
  turns the throw into a word."
  [thing act-id]
  (if-not (plain-map? thing)
    (words/word :read-the-act nil)
    (let [parts (read-parts thing)]
      (or (when (:could-not parts) parts)
          (own-parts-word thing)
          (act-id-word (:layer parts) act-id)
          {:thing thing
           :layer (:layer parts)}))))

(defn reading
  "Reads a maker's thing, with no round trip. Takes the thing, which can be
  anything, and the act-id that the maker gives back with it, or nil for
  none. Gives a reading, or one of the door's own words; with a word,
  nothing is appended.

  A reading is a map: `:thing`, the maker's own map; and `:layer`, as the
  thing holds it.

  The tests, in their order; the first that fails gives the word:

    1. the thing is a plain map, and its `:layer` can be
       read                                               :read-the-act
    2. it holds no `:act-id`, no `:depot` and no
       `:door-time`                                       :add-its-parts
    3. its `:layer` is an id within the limit, and the
       act-id given is an act-id of that layer            :make-an-act-id

  It reads no fact, no who and no permission, so it comes back for a thing
  that has no end in one of those. It never throws."
  [thing act-id]
  (try (reading* thing act-id)
       (catch Throwable _ (words/word :read-the-act nil))))

;; ------------------------------------------------------ the thing to append

(defn thing-to-append
  "Builds the thing that the door appends for an act. Takes a reading; the
  act-id, [layer depot uuid], which the reading's tests passed or the door
  made for the reading's layer; and the time that the door read from its
  clock, in milliseconds since 1970. Gives {:thing the thing}.

  The thing is the maker's map with three parts added: `:act-id`; `:depot`,
  the second slot of the act-id; and `:door-time`, the time given, under
  the name that `softland.store.door.declaration/time-part` holds. Each
  other part is the maker's own object: the door fills no missing part,
  reorders nothing, makes no part a vector, gives no value a normal form,
  names no permission, and carries `:who`, `:session`, `:permission` and
  `:facts` as given.

  It reads no clock and does not test the time: it adds what it is given,
  and a gate refuses a thing whose time is outside the declaration's range.

  It never throws: a thing that throws while the three parts are added ends
  in the word `:read-the-act`, and that is the one word that it can give."
  [reading act-id door-time]
  (try
    {:thing (assoc (:thing reading)
                   :act-id act-id
                   :depot (nth act-id 1)
                   declaration/time-part door-time)}
    (catch Throwable _ (words/word :read-the-act nil))))
