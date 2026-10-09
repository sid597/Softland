(ns softland.store.door.additions
  "What the door makes of one act: the tests of its own additions, the walk
  over the facts, the thing to append, and the request for locks. Pure code
  beside the seal's random source. It needs no cluster.

  The door adds three things to a maker's act: an act-id, the depot that the
  act-id names, and for each value fact `:sealed` bytes and a `:lock-id` in
  place of `:v`. A value fact is a fact that carries a value and whose key
  is not one of the three store keys. This namespace makes each addition
  right, or gives one of the door's own words (`softland.store.door.words`),
  and then nothing is appended for the act. An addition is right when the
  door's declaration (`softland.store.door.declaration`) takes it and when
  it wrote over nothing that the maker gave.

  The door checks nothing of the act's form: a gate does, and a gate's
  refusal is in the record. What is tested here is only what the door's own
  additions need:

    - the thing is a plain map, and it does not already hold `:act-id` or
      `:depot`;
    - its `:layer` is an id that an act-id can hold, and an act-id that the
      maker gives back is an act-id of that layer. A thing that passes is no
      stray on the depot that its act-id names, so a gate answers it. A
      thing that failed would get no answer, ever, and its maker could not
      tell it from an act that is on its way;
    - a value that the door finds is a value of the form
      (`softland.store.act/value?`). The one text of anything else does not
      read back as what the maker gave, and after the seal no gate can
      tell. A value fact does not already hold `:sealed` or `:lock-id`;
    - the act holds no more value facts than one act can hold facts, and no
      sealed value would be over the declaration's limit.

  Each of these is tested before a lock is taken, because a lock that is
  taken never comes back.

  Where the door finds a value: under `:v`, in a fact that is a map, in
  `:facts` that is a sequence. Everything else of the thing is appended as
  the maker gave it, and a depot keeps it for good: a value under a store
  key, whatever it is; what `:facts` holds when it is no sequence; a fact
  that is no map; a part that the form does not know; and each id. A gate
  refuses such a thing where the form is wrong; the depot's log keeps it all
  the same.

  Inputs and results, by call:

    reading          a maker's thing, and the act-id that the maker gives
                     back or nil -> what the door read of it, or a word
    thing-to-append  a reading, an act-id, the locks of the act -> {:thing
                     the thing to append}, or a word
    request          a reading, the act-id of a request, a count -> the
                     request for locks, as a thing to append
    series-word      the count of a series of act-ids -> nil, or a word

  `most-values-in-one-act` is the most value facts that the door seals in
  one act.

  State and effects. None of its own. `thing-to-append` seals, and each seal
  takes fresh random bytes from the JVM. No function throws for what a maker
  gave: a thing that throws while it is read ends in a word.

  Callers. `softland.store.door`, and the pure proof.

  Not here. The making of an act-id, which needs the layer's depot, a read
  of the cluster, and a random UUID: `softland.store.door`. The stock that
  the locks come from: `softland.store.door.stock`. The digest of the thing:
  `softland.store.door.digest`, taken by `softland.store.door`. The check
  of the act: a gate's."
  (:require [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.seal :as seal]
            [softland.store.door.text :as text]
            [softland.store.door.words :as words]))

(def most-values-in-one-act
  "The most value facts that the door seals in one act, and so the most
  locks that one act takes: the smaller of two limits of the declaration,
  the facts of one act and the locks of one request. Both are 256 in this
  build. A lock past the first limit cannot seal an accepted value, since a
  gate refuses an act with more facts. The second limit is here because one
  call of the door sends one request for locks at most, so one request must
  cover one act. The bound is the door's own, of its stock: if the
  declaration's limit of locks in one request were made less than its limit
  of facts, the door would stop an act that a gate would take with the
  locks of two requests."
  (min declaration/max-facts declaration/max-locks-in-request))

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
  "The parts of a maker's thing that the door reads, in the order in which
  it reads them. `:layer` and `:facts` for its own additions. `:who` to
  choose the stock: a lock is used only for an act of the who that asked
  for it. `:who`, `:permission`, `:version` and `:session` are copied into
  the door's request for locks as given. The door checks none of them."
  [:layer :facts :who :permission :version :session])

(defn- read-parts
  "Takes a thing that is a plain map. Gives a map of the parts of
  `parts-read`, each as the thing holds it, nil for a part that is absent;
  or the word `:read-the-act` at the part whose read threw. A sorted map
  whose keys cannot be compared with a keyword throws at the first."
  [thing]
  (reduce (fn [got part]
            (let [read (try [(get thing part)] (catch Throwable _ nil))]
              (if read
                (assoc got part (first read))
                (reduced (words/word :read-the-act {:at part})))))
          {}
          parts-read))

(defn- own-parts-word
  "Takes a thing that is a plain map. Gives nil when it holds neither
  `:act-id` nor `:depot`, the two parts of the act that the door adds; or
  the word `:add-its-parts` at the part that it holds. A part that is there
  with nil counts: the door writes over nothing."
  [thing]
  (when-let [part (some (fn [part] (when (contains? thing part) part)) [:act-id :depot])]
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

(defn- entry-of
  "One fact of the walk. Takes the act's layer, the fact's position, the
  fact, and how many value facts the walk has counted before it. Gives
  {:fact the fact} for a fact that goes as the maker gave it; for a value
  fact {:fact the fact, :plain the plain bytes of its value, :place
  [layer entity key]}; or a word. In order:

    no map: a string, a number, a vector     as given
    a map that is no plain map, a record,
      with a value under :v                  :seal-a-value at [:facts i]
    such a map with no value                 as given
    a plain map with no :v, or a nil one:
      a retract, or a fact that already
      carries :sealed or :lock-id            as given; a gate judges it
    a plain map with a value, under one of
      the three store keys                   as given, the value plain, also
                                             when it holds :sealed or
                                             :lock-id: the door adds no part
                                             to it, so it writes over
                                             nothing
    a plain map with a value, under any
      other key, that holds :sealed or
      :lock-id                               :add-its-parts at that part
    a plain map with a value, under any
      other key: a value fact                counted and made ready

  A key in the store's namespace that is no store key is \"any other key\":
  its value is sealed, and a gate refuses the fact either way. The door
  tells a store key by its being one of the three, as the gates do.

  For a value fact, in order: the word `:seal-that-many-values` when the
  walk has counted `most-values-in-one-act` already; `:seal-a-value` at
  [:facts i :v] when the value is no value of the form; and the same word,
  with `:sealed-bytes` and `:most`, when its sealed bytes would be more than
  the declaration's limit. The length is asked of
  `softland.store.door.seal/sealed-length`, for the way that new locks are
  made for. The plain bytes of a value are the UTF-8 of the one text of its
  normal form. It can throw, for a fact that throws when it is read; `walk`
  turns the throw into a word."
  [layer i fact counted]
  (cond
    (not (map? fact))
    {:fact fact}

    (not (plain-map? fact))
    (if (some? (get fact :v))
      (words/word :seal-a-value {:at [:facts i]})
      {:fact fact})

    :else
    (let [v (get fact :v)]
      (cond
        (nil? v)
        {:fact fact}

        (declaration/store-key? (get fact :k))
        {:fact fact}

        (contains? fact :sealed)
        (words/word :add-its-parts {:at [:facts i :sealed]})

        (contains? fact :lock-id)
        (words/word :add-its-parts {:at [:facts i :lock-id]})

        (<= most-values-in-one-act counted)
        (words/word :seal-that-many-values {:most most-values-in-one-act})

        (not (act/value? v))
        (words/word :seal-a-value {:at [:facts i :v]})

        :else
        (let [^bytes plain (text/utf8 (act/normal-value v))
              sealed-bytes (seal/sealed-length seal/way-of-new-locks (alength plain))]
          (if (and sealed-bytes (< declaration/max-sealed-bytes sealed-bytes))
            (words/word :seal-a-value {:at [:facts i :v]
                                       :sealed-bytes sealed-bytes
                                       :most declaration/max-sealed-bytes})
            {:fact fact
             :plain plain
             :place [layer (get fact :e) (get fact :k)]}))))))

(defn- walk
  "The walk of the facts. Takes the act's layer and what the thing holds
  under `:facts`. Gives {:entries :values}, or a word.

  When `:facts` is no sequence (absent, nil, one fact map, a set, a string)
  the door finds no fact: `:entries` is nil and `:values` is 0. The thing
  then goes as given, a gate refuses it, and a value inside goes plain into
  the depot. When it is a sequence, `:entries` is a vector with one entry
  for each fact at its position (`entry-of`), and `:values` is the count of
  the value facts.

  The walk takes one fact at a time and stops at the first word. So it ends
  for facts with no end in which value facts keep coming: at the one past
  `most-values-in-one-act`. It does not end for a sequence with no end that
  holds fewer value facts than that. A fact that throws while it is read
  gives `:read-the-act` at its position."
  [layer facts]
  (if-not (sequential? facts)
    {:entries nil :values 0}
    (loop [i 0
           left (try (seq facts) (catch Throwable _ ::threw))
           entries []
           counted 0]
      (cond
        (= ::threw left)
        (words/word :read-the-act {:at [:facts i]})

        (nil? left)
        {:entries entries :values counted}

        :else
        (let [entry (try (entry-of layer i (first left) counted)
                         (catch Throwable _ (words/word :read-the-act {:at [:facts i]})))]
          (if (:could-not entry)
            entry
            (recur (inc i)
                   (try (next left) (catch Throwable _ ::threw))
                   (conj entries entry)
                   (if (:plain entry) (inc counted) counted))))))))

(def ^:private parts-carried
  "The parts of a maker's thing that the door's request for locks carries,
  each as given: a gate holds a request to the version, the who, the
  permission and the session of an act as it holds any act."
  [:version :who :permission :session])

(defn- carried-of
  "Takes a thing that is a plain map, and its parts as `read-parts` gave
  them. Gives a map of the parts of `parts-carried` that the thing holds,
  each as given: a part that is there with nil is in it with nil, and a
  part that the thing does not hold is not in it. It can throw, for a thing
  that throws when it is asked whether it holds a part; `reading` turns the
  throw into a word."
  [thing parts]
  (into {}
        (keep (fn [part] (when (contains? thing part) [part (get parts part)])))
        parts-carried))

(defn- reading*
  "The body of `reading`, with no guard around it."
  [thing act-id]
  (if-not (plain-map? thing)
    (words/word :read-the-act nil)
    (let [parts (read-parts thing)]
      (or (when (:could-not parts) parts)
          (own-parts-word thing)
          (act-id-word (:layer parts) act-id)
          (let [walked (walk (:layer parts) (:facts parts))]
            (if (:could-not walked)
              walked
              {:thing   thing
               :layer   (:layer parts)
               :who     (:who parts)
               :carried (carried-of thing parts)
               :entries (:entries walked)
               :values  (:values walked)}))))))

(defn reading
  "Reads a maker's thing, with no round trip and before any lock is taken.
  Takes the thing, which can be anything, and the act-id that the maker
  gives back with it, or nil for none. Gives a reading, or one of the door's
  own words; with a word, nothing is appended.

  A reading is a map: `:thing`, the maker's own map; `:layer` and `:who` as
  the thing holds them; `:carried`, the parts that a request for locks
  carries, `:version`, `:who`, `:permission` and `:session`, each as given
  and none that the thing does not hold; `:entries`, one for each fact at
  its position, or nil when `:facts` is no sequence; `:values`, how many
  value facts the act holds, which is how many locks it needs.

  The tests, in their order; the first that fails gives the word:

    1. the thing is a plain map, and each part that the door reads can be
       read                                               :read-the-act
    2. it holds no `:act-id` and no `:depot`              :add-its-parts
    3. its `:layer` is an id within the limit, and the
       act-id given is an act-id of that layer            :make-an-act-id
    4. the walk of the facts (`walk`)                     :add-its-parts,
                                                          :seal-a-value,
                                                          :seal-that-many-values,
                                                          :read-the-act

  It never throws. Limit: facts with no end that hold fewer value facts than
  `most-values-in-one-act` do not come back from it, as they do not from the
  form's own check."
  [thing act-id]
  (try (reading* thing act-id)
       (catch Throwable _ (words/word :read-the-act nil))))

;; ------------------------------------------------------ the thing to append

(defn- facts-to-append
  "Takes the entries of a reading and the locks of the act, in the order of
  the value facts. Gives the facts as a vector, each at the position that
  the maker gave it, or a word. A value fact is the maker's fact without
  `:v` and with `:sealed` and `:lock-id`; each other part of it stays. Each
  other fact is the maker's own object.

  A value is sealed to its place, [layer entity key], with the next lock.
  The word is `:seal-a-value` at [:facts i :v] when no lock is left for the
  value, when the seal gives nothing, and, with `:sealed-bytes` and `:most`,
  when the sealed bytes are more than the declaration's limit. The limit is
  held here on the real bytes, whatever way the lock is of."
  [entries locks]
  (loop [i 0
         entries (seq entries)
         locks (seq locks)
         facts []]
    (if-not entries
      facts
      (let [{:keys [fact plain place]} (first entries)]
        (if-not plain
          (recur (inc i) (next entries) locks (conj facts fact))
          (let [{:keys [lock-id lock]} (first locks)
                ^bytes sealed (when lock (seal/seal lock place plain))]
            (cond
              (nil? sealed)
              (words/word :seal-a-value {:at [:facts i :v]})

              (< declaration/max-sealed-bytes (alength sealed))
              (words/word :seal-a-value {:at [:facts i :v]
                                         :sealed-bytes (alength sealed)
                                         :most declaration/max-sealed-bytes})

              :else
              (recur (inc i)
                     (next entries)
                     (next locks)
                     (conj facts (-> fact (dissoc :v) (assoc :sealed sealed :lock-id lock-id)))))))))))

(defn thing-to-append
  "Builds the thing that the door appends for an act. Takes a reading; the
  act-id, [layer depot uuid], which the reading's tests passed or the door
  made for the reading's layer; and the locks of the act, a vector of
  {:lock-id :lock} as the stock gives them, one for each value fact in the
  order of the facts. Gives {:thing the thing}, or a word.

  The thing is the maker's map with `:act-id` and `:depot` added, the depot
  being the second slot of the act-id, and `:facts` as a vector, each fact
  at its position, each value sealed (`facts-to-append`). When `:facts` was
  no sequence it stays as it was. Each other part is the maker's own object:
  the door fills no missing part, names no permission, and carries `:who`,
  `:session` and `:permission` as given.

  It never throws: a thing that throws while it is built ends in the word
  `:read-the-act`. A lock that was taken for a value is not given back when
  a word comes out here."
  [reading act-id locks]
  (try
    (let [{:keys [thing entries]} reading
          facts (when entries (facts-to-append entries locks))]
      (if (:could-not facts)
        facts
        {:thing (cond-> (assoc thing :act-id act-id :depot (nth act-id 1))
                  entries (assoc :facts facts))}))
    (catch Throwable _ (words/word :read-the-act nil))))

;; ------------------------------------------------------ the request for locks

(defn request
  "The door's request for locks, as a thing to append. Takes the reading of
  the maker's act; the act-id of the request, which is of the act's layer
  and carries the depot slot of the act's own act-id; and how many locks to
  ask for. Gives an act of the door's own making: the `:version`, `:who`,
  `:permission` and `:session` of the maker's act, each as given and none
  when the maker gave none; the act's `:layer`; the request's `:act-id` and
  its `:depot`; and one fact, on the uuid of the request's own act-id under
  `:softland.store/lock-request`, that holds the count.

  So a request is asked for under the maker's who and permission, and a
  gate that refuses it refuses it for what the maker's own act carries. The
  door makes no session and no id of its own for it. It carries a count and
  no lock's bytes: a gate makes the bytes when it accepts the request.

  It reads nothing of the maker's thing again: the four parts are the
  reading's `:carried`. So it cannot throw, and the door calls it while it
  sits in a stock's seat.

  What must hold: the count is 1 to the declaration's ceiling of one
  request, which `softland.store.door.stock/ask-for` gives."
  [reading request-act-id n]
  (assoc (:carried reading)
         :layer (:layer reading)
         :act-id request-act-id
         :depot (nth request-act-id 1)
         :facts [{:e (nth request-act-id 2) :k :softland.store/lock-request :v n}]))
