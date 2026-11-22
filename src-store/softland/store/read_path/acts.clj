(ns softland.store.read-path.acts
  "The read of a layer's acts by stamp: the decisions of one layer from a
  stamp on, in the order of the stamps, each with its answer and the thing
  whole, for an asker who may read it. Pure code. It needs no cluster: it
  says, from what is known so far, which sources the read wants next; it
  holds the one rule of who reads a decision whole; and it puts the answer
  together. `softland.store.read-path` fetches what it names.

  The query. The word `:acts` of a query
  (`softland.store.read-path.query`): `[:acts start most]`, with one layer,
  named by `:from-layer` or as the one layer of `:layers`. The read takes
  that layer alone, never what it builds on: two lines have no order
  between them. `most` is cut to `:max-acts-of-one-read`. `:with` and
  `:stores-own-facts` say what of a fact's row a read gives, and this read
  gives no fact's row: it does not read them.

  Where it reads: all on the layer's own task, each source with the layer
  as its routing id. In a streaming layer every row is there. In the base,
  the line, its things, its last stamp, its rows by who and a copy of its
  facts are on the base's task, where the base's gate decides, so a read of
  the base's acts is read there and not on the task of a home, whose copy
  of the base's facts can be a batch behind the base's line (the namespace
  docstring of `softland.store.read-path.sources` says where a read of
  facts reads the base, and why).

  The rounds, in the order of what must hold. Each is one call of Rama's
  client for each table, all of a round sent at once.

    1. the last stamp      the layer's last stamp, in the gate's table for
                           a task: one row
    2. the rights          the layer's own rows, her permission for the
                           layer and her run's: the same sources as a read
                           of facts reads (`softland.store.read-path.sources`)
    3. the rows above      the rows above her permission and her run's, when
                           the rows of round 2 name them
    4. for an asker who may see the layer: the window, the line rows from
       the start, `most` at most. For an asker outside it: her rows by who
       from the start, `most` at most
    5. for an asker outside: her line rows at those stamps
    6. the things          the thing of each decision given whole that fits
                           in the bound on weight

  Why in that order. The last stamp is read before the rights, and only
  decisions up to it are given. A decision at that stamp or before was
  decided before the rights were read, so the rights that judge the answer
  are not older, in the layer's line, than any decision that the answer
  gives whole on their ground: a permission that ended at stamp s, or a
  setting that changed there, is read as such by any read that gives s
  (IMPLICIT_SPEC.md R12 and R13). And since a line row is written in the
  decision's one write with the last stamp, the window holds a row for
  every stamp up to it: a row that is not there is a fault and no race. A
  decision that lands after the last stamp is read is left for the next
  read. Her rows by who are read only once the rights say that she is
  outside, and her things only then: a read by an asker who may see the
  layer reads no row by who, and a read by an asker outside it reads no row
  of the layer that is not hers, so what it costs is bounded by what is
  hers (SPEC.md, \"Her own things, without a walk through everyone's\").
  Things are read last, when what is given whole is known, so a thing that
  is withheld is never fetched (R16). A row of the line and its thing are
  written in one event and never again.

  What one read costs, counted from the code and not measured. Round 1:
  one seek. Round 2: one seek and a step for each row of the layer's own,
  and one seek for each of her two permission rows. Round 3: one seek for
  each permission above hers. Round 4: one seek and n steps for the window;
  for her rows by who, one seek and a step for each of hers. Round 5 and
  round 6: one seek for each run of neighbouring stamps that the round
  reads, and a step for each row of it (`runs`): a holder's page of n kept
  things is one seek and n steps. Rama's skill text counts the level of the
  field in the table for a task as one read more, for rounds 1 and 4.

  Who reads a decision whole: `reads-whole?`, the one place. By what the
  asker is in the layer (`asker-kind`):

    a holder   her permission for the layer, or the run that she asks in,
               stands (`softland.store.read-path.layers/either`): every
               decision whole, those from before her permission too
    a visitor  she may see the layer, which is open to everyone, and is no
               holder; signed in or not: every decision whole when the
               layer's acts setting says `:everyone`; else her own whole,
               and every other one withheld, given as its stamp alone
    outside    she may not see the layer, or it is not made: her own
               decisions whole, wherever they are, and nothing else of the
               layer, not its stamps and not its last stamp; the answer
               says that the layer is otherwise left out

  Her own: a decision whose thing names her id as its `:who`, the line
  row's `:who`. A thing's who is what its sender wrote, and the asker is
  what the caller says; neither is held here (the runtime will hold the
  asker against the caller). So in this build a caller that names an asker
  reads that asker's things, and a sender can put a thing into a layer
  that names another as its who, which she then reads as her own.

  The answer. A plain map, as every answer of the read path:

    :layers      [the layer]
    :left-out    [] or [{:layer layer :why :may-not-see or :not-made}]
    :acts        the decisions given, in the order of the stamps
    :next        the stamp from which the next read begins
    :last-stamp  the layer's last stamp when the read began; only to an
                 asker who may see the layer. nil when the layer had no
                 decision then: a read whose last stamp was read before
                 the layer was made, and its rights after

  A decision given, one of three:

    {:stamp s}                          withheld: nothing else of it
    {:stamp s :act-id id :answer a      whole
     :thing t}
    {:stamp s :act-id id :answer a      whole, with no thing: `:too-heavy`
     :no-thing why}                     or `:not-weighed`, the gate kept
                                        none (`softland.store.gate.decision`,
                                        the line row's word); or
                                        `:heavier-than-the-read`, the thing
                                        alone weighs more than the read's
                                        bound

  `:answer` is the answer of the door's read by act-id
  (`softland.store.record.rows/line-answer`). `:thing` is the thing as Rama
  gave it, the arrival that the gate decided. To an asker who may see the
  layer the stamps are consecutive from the start, none above the last
  stamp, and `:next` is the start and the count of what was given; she is at
  the end when `:next` is the last stamp and one. To an asker outside,
  `:next` is the stamp after her last decision given, or the start.

  The bounds. `most`, cut to `:max-acts-of-one-read`, and the weight of the
  things of one answer, `:max-thing-weight-of-one-read`, a number of the
  read path (`softland.store.read-path.query/default-numbers`). Things are
  given in the order of the stamps while their weights fit; the answer
  ends before the first thing that does not fit, and `:next` is its stamp.
  A thing that alone weighs more than the bound is given as
  `:heavier-than-the-read`, and the answer goes on past it. A weight is the
  gate's measure (`softland.store.door.weight`), not the bytes that Rama
  moves.

  When the line is not whole: a stamp up to the last stamp with no line
  row in the window, a stamp of her own with no line row, or a thing that
  the line says is kept and is not there. `reading` then gives `:fault`,
  with the first such stamp, and the read gives a word, never an answer
  that ends there.

  Inputs and results, by call:

    asker-kind     a layer, its own rows, whether she holds a permission
                   that stands, whether she is signed in -> `:holder`,
                   `:visitor`, `:outside` or `:not-known-yet`
    reads-whole?   a kind, the acts setting, her id's text, a line row's
                   who -> true or false
    runs           [stamp kind] pairs in the order of the stamps -> the
                   runs of neighbouring stamps that one window each reads
    reading        a query, `known`, the numbers -> {:wanted :missing} and,
                   when nothing is missing, `:answer` or `:fault`

  State and effects. None. Each function is pure, and none throws on a row.

  Callers. `softland.store.read-path`."
  (:require [softland.store.door.declaration :as declaration]
            [softland.store.gate.first-act :as first-act]
            [softland.store.read-path.layers :as layers]
            [softland.store.read-path.sources :as sources]
            [softland.store.record.rows :as rows]))

;; -------------------------------------------------------------- the sources

(defn- source-of
  "Takes a layer, a table and a path. Gives the source, read on the layer's
  own task: the layer is its routing id, in both gates."
  [layer table path]
  {:gate (first-act/depot-at-making layer) :table table :layer layer :routing-id layer :path path})

(defn last-stamp-source
  "Takes a layer. Gives the source of its last stamp, in the gate's table
  for a task."
  [layer]
  (source-of layer :task [:last-stamp (declaration/id-text layer)]))

(defn window-source
  "Takes a layer, a stamp and a count. Gives the source of the window: the
  rows of the layer's line from that stamp on, that many at most."
  [layer start n]
  (let [[from to] (rows/line-run layer start)]
    (source-of layer :line [:window from to n])))

(defn by-who-source
  "Takes a layer, an asker's id, a stamp and a count. Gives the source of
  her rows by who in the layer from that stamp on, that many at most."
  [layer asker start n]
  (let [[from to] (rows/by-who-run layer asker start)]
    (source-of layer :task [:by-who from to n])))

(defn run-source
  "Takes a layer, a table (`:line` or `:things`) and a run, [first last
  count]: neighbouring stamps from the first to the last, of which `count`
  have a row in that table. Gives the source that reads those rows and no
  other: a window from the key of the first stamp, cut after the key of the
  last. The key of the last and a space sorts after that key and before the
  key of the next stamp, whose digits are greater."
  [layer table [first-stamp last-stamp n]]
  (source-of layer table [:window (rows/line-key layer first-stamp) (str (rows/line-key layer last-stamp) " ") n]))

;; ------------------------------------------------- who reads what whole

(defn asker-kind
  "What the asker is in the layer, for this read. Takes the layer, its own
  rows (nil when not known), whether she holds a permission that stands in
  it (`softland.store.read-path.layers/either`: `:stands`, `:none` or
  `:not-known-yet`), and whether she is signed in. Gives `:holder`,
  `:visitor`, `:outside`, or `:not-known-yet`, by the table of the
  namespace docstring. Unlike a read of facts, it reads her permission in a
  layer that everyone may see too: there it decides between whole and
  withheld (IMPLICIT_SPEC.md R4)."
  [layer own-rows permission-state signed-in?]
  (cond
    (nil? own-rows) :not-known-yet
    (not (layers/made? own-rows layer)) :outside
    (= :stands permission-state) :holder
    (= :not-known-yet permission-state) :not-known-yet
    (= :everyone (layers/who-may-see own-rows layer)) :visitor
    :else :outside))

(defn reads-whole?
  "Who reads a decision whole: the one rule. Takes the asker's kind
  (`asker-kind`), the layer's acts setting (`:members` or `:everyone`,
  `softland.store.read-path.layers/who-may-read-acts`), the printed text
  of the asker's id, nil for one who is not signed in, and the line row's
  `:who`, nil for a thing that names no usable who. Gives true when she
  reads it whole: a holder reads every decision; a visitor every decision
  when the setting is `:everyone`; and any asker her own. A taken act is
  held to the same rule as a refused thing (SPEC.md, \"Who may read what\",
  a session's reading): this function is where the other reading would be
  made."
  [kind acts-setting asker-text who-text]
  (boolean (or (= :holder kind)
               (and (= :visitor kind) (= :everyone acts-setting))
               (and (some? asker-text) (= asker-text who-text)))))

;; ----------------------------------------------------------- the rows read

(defn- rows-of-run
  "Takes the source of a run (a window, or her rows by who), its value as
  a select gave it, and a function that reads the stamp of a key. Gives
  [stamp row] for each key of the run, in the order of the stamps. A key
  that is not of the run is dropped: for a run whose ends hold a letter
  from U+D800 on, the read path makes no cut at the run's end and a select
  can give keys after it (`softland.store.read-path.sources/keys-of-run`)."
  [source run stamp-of]
  (->> (sources/keys-of-run source run)
       (keep (fn [k] (when-let [s (stamp-of k)] [s (sources/row-in run k)])))
       (sort-by first)
       vec))

(defn runs
  "Takes [stamp kind] pairs in the order of the stamps, each kind one of
  `:take`, a row to read; `:bridge`, a stamp with no row in the table read,
  which a run may go across; and `:break`, a stamp whose row the read must
  not read. Gives the runs, each [first last count]: neighbouring stamps
  from a stamp to take up to one to take, with nothing between them but
  stamps to take and to bridge, and `count` the stamps taken. A gap in the
  stamps ends a run, as a `:break` does, so a window of a run reads the
  rows taken and no row of another stamp: one seek for each run and a step
  for each row."
  [pairs]
  (let [close (fn [out run] (if run (conj out [(:first run) (:last run) (:n run)]) out))]
    (loop [todo pairs run nil at nil out []]
      (if-let [[s kind] (first todo)]
        (let [s (long s)
              next? (and run (= s (inc (long at))))]
          (case kind
            :take   (if next?
                      (recur (rest todo) (assoc run :last s :n (inc (long (:n run)))) s out)
                      (recur (rest todo) {:first s :last s :n 1} s (close out run)))
            :bridge (if next?
                      (recur (rest todo) run s out)
                      (recur (rest todo) nil nil (close out run)))
            (recur (rest todo) nil nil (close out run))))
        (close out run)))))

(defn- values-of-runs
  "Takes the sources of runs and `known`, which holds each. Gives a map from
  each stamp that the runs gave a row for to that row."
  [run-sources known]
  (into {} (mapcat (fn [source]
                     (keep (fn [[s row]] (when (some? row) [s row]))
                           (rows-of-run source (get known source) rows/stamp-of-line-key))))
        run-sources))

(defn- whole-entry
  "Takes the layer, the stamp and its line row. Gives the decision whole, but
  for its thing: its stamp, its act-id and its answer."
  [layer stamp row]
  {:stamp stamp
   :act-id [layer (first-act/depot-at-making layer) (:act row)]
   :answer (rows/line-answer stamp row)})

(defn- keeps-no-thing?
  "Takes a line row. Gives true when the gate kept no thing for it: its row
  says why, or holds no weight."
  [row]
  (or (some? (:not-kept row)) (not (int? (:weight row)))))

(defn- page
  "Which things a page gives, within the bound. Takes the decisions to give,
  in the order of the stamps, each {:stamp s :whole? b :row r}, and the
  bound. Gives the decisions that the page holds, each with `:fetch?` true
  for a thing to fetch, or with `:no-thing` and its word: the page ends
  before the first thing that does not fit in what is left, and a thing
  that alone is heavier than the bound is given with no thing."
  [decisions bound]
  (loop [todo decisions left (long bound) out []]
    (if-let [{:keys [whole? row] :as d} (first todo)]
      (let [w (:weight row)]
        (cond
          (not whole?) (recur (rest todo) left (conj out d))
          (:not-kept row) (recur (rest todo) left (conj out (assoc d :no-thing (:not-kept row))))
          (not (int? w)) (recur (rest todo) left (conj out (assoc d :no-thing :not-weighed)))
          (> (long w) (long bound)) (recur (rest todo) left (conj out (assoc d :no-thing :heavier-than-the-read)))
          (> (long w) left) out
          :else (recur (rest todo) (- left (long w)) (conj out (assoc d :fetch? true)))))
      out)))

(defn- things-runs
  "Takes the decisions of a page. Gives the runs of the things table that
  fetch the things to fetch and no other thing: a decision whose thing is
  fetched is taken; one that keeps no thing is bridged; one whose thing is
  kept and not fetched, a withheld one or one heavier than the read, breaks
  a run, as a stamp that is not in the page does."
  [held]
  (runs (map (fn [{:keys [stamp row] :as d}]
               [stamp (cond (:fetch? d) :take
                            (keeps-no-thing? row) :bridge
                            :else :break)])
             held)))

;; ------------------------------------------------------------- the reading

(defn- wants
  "Takes the sources that a read wants now, and `known`. Gives the reading
  of a read that is not done: {:wanted :missing}."
  [wanted known]
  (let [wanted (into #{} (remove nil?) wanted)]
    (assoc {:wanted wanted} :missing (into #{} (remove #(contains? known %)) wanted))))

(defn- answer-of-page
  "The last round of a read: the things of a page, then the answer. Takes
  the layer, the asker's kind, the decisions to give (each {:stamp :whole?
  :row}, in the order of the stamps), the sources wanted so far, `known`,
  the numbers, the start, the last stamp and the layer's own rows. Gives
  the reading: {:wanted :missing} while a thing is missing, else with
  `:answer` or with `:fault`."
  [layer kind decisions wanted known numbers start last-stamp own-rows]
  (let [held (page decisions (:max-thing-weight-of-one-read numbers))
        fetched (filterv :fetch? held)
        thing-sources (mapv #(run-source layer :things %) (things-runs held))
        wanted (into wanted thing-sources)
        reading (wants wanted known)]
    (if (seq (:missing reading))
      reading
      (let [thing-of (values-of-runs thing-sources known)
            lost (some #(when-not (contains? thing-of (:stamp %)) (:stamp %)) fetched)
            outside? (= :outside kind)]
        (if lost
          (assoc reading :fault {:stamp lost})
          (assoc reading
                 :answer
                 (cond-> {:layers [layer]
                          :left-out (if outside?
                                      [(layers/left-out layer (if (layers/made? own-rows layer) :may-not-see :not-made))]
                                      [])
                          :acts (mapv (fn [{:keys [stamp row] :as d}]
                                        (cond
                                          (not (:whole? d)) {:stamp stamp}
                                          (:fetch? d) (assoc (whole-entry layer stamp row) :thing (get thing-of stamp))
                                          :else (assoc (whole-entry layer stamp row) :no-thing (:no-thing d))))
                                      held)
                          :next (if outside?
                                  (if (seq held) (inc (long (:stamp (peek held)))) start)
                                  (+ (long start) (count held)))}
                   (not outside?) (assoc :last-stamp last-stamp))))))))

(defn- first-gap
  "Takes the start and [stamp row] pairs in the order of the stamps. Gives
  the first stamp from the start at which the pairs leave a gap, or nil
  when they are the stamps start, start + 1, and on."
  [start pairs]
  (some (fn [[i [s]]] (when (not= (long s) (+ (long start) (long i))) (+ (long start) (long i))))
        (map-indexed vector pairs)))

(defn- inside-reading
  "Rounds 4 and 6 for an asker who may see the layer. Takes the layer, the
  asker's kind, the function that says whether she reads a line row whole,
  the sources wanted so far, `known`, the numbers, the start, the count,
  the last stamp and the layer's own rows. Gives the reading. No window is
  read when nothing is decided from the start up to the last stamp."
  [layer kind whole? wanted known numbers start n last-stamp own-rows]
  (let [expected (if (and (some? last-stamp) (<= (long start) (long last-stamp)))
                   (min (long n) (inc (- (long last-stamp) (long start))))
                   0)]
    (if (zero? expected)
      (answer-of-page layer kind [] wanted known numbers start last-stamp own-rows)
      (let [window (window-source layer start n)
            wanted (conj wanted window)]
        (if-not (contains? known window)
          (wants wanted known)
          (let [rows (->> (rows-of-run window (get known window) rows/stamp-of-line-key)
                          (filterv (fn [[s]] (<= (long s) (long last-stamp)))))
                gap (or (first-gap start rows) (when (< (count rows) expected) (+ (long start) (count rows))))]
            (if gap
              (assoc (wants wanted known) :fault {:stamp gap})
              (answer-of-page layer kind
                              (mapv (fn [[s row]] {:stamp s :whole? (whole? row) :row row}) (take expected rows))
                              wanted known numbers start last-stamp own-rows))))))))

(defn- outside-reading
  "Rounds 4, 5 and 6 for an asker outside the layer: her rows by who from
  the start, the line rows at their stamps, and her things; for an asker
  who is not signed in, nothing. Takes the layer, the query's asker and
  session parts as the reading has them, the sources wanted so far,
  `known`, the numbers, the start, the count and the layer's own rows.
  Gives the reading."
  [layer asker wanted known numbers start n own-rows]
  (if (nil? asker)
    (answer-of-page layer :outside [] wanted known numbers start nil own-rows)
    (let [by-who (by-who-source layer asker start n)
          wanted (conj wanted by-who)]
      (if-not (contains? known by-who)
        (wants wanted known)
        (let [stamps (mapv first (rows-of-run by-who (get known by-who) rows/stamp-of-by-who-key))
              line-sources (mapv #(run-source layer :line %) (runs (map (fn [s] [s :take]) stamps)))
              wanted (into wanted line-sources)]
          (if (some #(not (contains? known %)) line-sources)
            (wants wanted known)
            (let [row-of (values-of-runs line-sources known)
                  lost (some #(when-not (contains? row-of %) %) stamps)]
              (if lost
                (assoc (wants wanted known) :fault {:stamp lost})
                (answer-of-page layer :outside
                                (mapv (fn [s] {:stamp s :whole? true :row (get row-of s)}) stamps)
                                wanted known numbers start nil own-rows)))))))))

(defn reading
  "What a read of acts wants now, or its answer. Takes the query in its one
  form, with `:asks` of the word `:acts`; `known`, a map from a source to
  the value that a select gave; and the read path's numbers. Gives
  {:wanted :missing}, the sources that the read wants and those of them
  that `known` does not hold, while it needs more; else that with
  `:answer`, the answer of the namespace docstring, or with `:fault`,
  {:stamp s}, the first stamp at which the line is not whole.

  The rounds are those of the namespace docstring, each wanted only when
  what it rests on is known: at most six, for an asker outside whose
  permission's rows above must be read, and five for one who may see the
  layer. A kind that is not known when every source of the rights is known
  cannot be reached; it is read as outside, which shows the least."
  [query known numbers]
  (let [[_ start most] (:asks query)
        start (long start)
        layer (layers/home query)
        n (min (long most) (long (:max-acts-of-one-read numbers)))
        asker (:asker query)
        session (:session query)
        signed-in? (some? asker)
        last-source (last-stamp-source layer)]
    (if-not (contains? known last-source)
      (wants [last-source] known)
      (let [own (sources/own-rows-source layer layer)
            permission (when signed-in? (sources/permission-source layer layer asker))
            run-permission (when (and signed-in? session) (sources/run-permission-source layer layer session))
            rights [last-source own permission run-permission]]
        (if (some #(and % (not (contains? known %))) rights)
          (wants rights known)
          (let [above (into (if permission (sources/chain-sources layer layer (get known permission)) [])
                            (when run-permission
                              (sources/chain-sources layer layer (sources/askers-run-row (get known run-permission) asker))))
                wanted (into rights above)]
            (if (some #(not (contains? known %)) above)
              (wants wanted known)
              (let [own-rows (get known own)
                    kind (let [k (asker-kind layer own-rows (sources/permission-state query layer layer known) signed-in?)]
                           (if (= :not-known-yet k) :outside k))
                    setting (layers/who-may-read-acts own-rows layer)
                    asker-text (when signed-in? (declaration/id-text asker))
                    whole? (fn [row] (reads-whole? kind setting asker-text (:who row)))]
                (if (= :outside kind)
                  (outside-reading layer asker wanted known numbers start n own-rows)
                  (inside-reading layer kind whole? wanted known numbers start n
                                  (get known last-source) own-rows))))))))))
