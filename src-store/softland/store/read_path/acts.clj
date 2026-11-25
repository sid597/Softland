(ns softland.store.read-path.acts
  "The read of a lane's acts by stamp: the decisions of one lane from a
  stamp on, in the order of the stamps, each with its answer and the thing
  whole, for an asker who may read it. Pure code: it says, from what is
  known so far, which sources the read wants next, and
  `softland.store.read-path` fetches what it names.

  The read takes one lane alone, never what it builds on: two lines have no
  order between them. It gives no fact's row, so it does not read `:with`
  and `:stores-own-facts`.

  Where it reads: all on the lane's own task, each source with the lane
  as its routing id. The base's line, its things, its last stamp and its
  rows by who are on the base's task, where the base's gate decides. So a
  read of the base's acts is read there and not on the task of a home,
  whose copy of the base's facts can be a batch behind the base's line.

  The rounds: the last stamp; the rights, which are the lane's own rows,
  her permission for the lane and her run's; the rows above those; then
  the window of line rows for an asker who may see the lane, or her rows by
  who and after them her line rows for an asker outside; and last the
  things. Each round is one call of Rama's client for each table, all of a
  round sent at once.

  Why in that order. The last stamp is read before the rights, and only
  decisions up to it are given. A decision at that stamp or before was
  decided before the rights were read, so the rights that judge the answer
  are not older, in the lane's line, than any decision that the answer
  gives whole on their ground: a permission that ended at stamp s, or a
  setting that changed there, is read as such by any read that gives s
  (IMPLICIT_SPEC.md R12 and R13). And since a line row is written in the
  decision's one write with the last stamp, the window holds a row for
  every stamp up to it: a row that is not there is a fault and no race. A
  decision that lands after the last stamp is read is left for the next
  read. Her rows by who are read only once the rights say that she is
  outside, and her things only then: a read by an asker who may see the
  lane reads no row by who, and a read by an asker outside it reads no row
  of the lane that is not hers, so what it costs is bounded by what is
  hers (SPEC.md, \"Her own things, without a walk through everyone's\").
  Things are read last, when what is given whole is known, so a thing that
  is withheld is never fetched (R16). A row of the line and its thing are
  written in one event and never again.

  What one read costs, counted from the code and not measured. The last
  stamp: one seek. The rights: one seek and a step for each row of the
  lane's own, one seek for each of her two permission rows, and one seek
  for each permission above hers. The window: one seek and n steps; her
  rows by who: one seek and a step for each of hers. Her line rows, and the
  things: one seek for each run of neighbouring stamps that the round
  reads, and a step for each row of it (`runs`), so a holder's page of n
  kept things is one seek and n steps. Rama's skill text counts the level
  of the field in the table for a task as one read more, for the last stamp
  and for her rows by who.

  No function here throws on a row."
  (:require [softland.store.door.declaration :as declaration]
            [softland.store.gate.first-act :as first-act]
            [softland.store.read-path.lanes :as lanes]
            [softland.store.read-path.sources :as sources]
            [softland.store.record.rows :as rows]))

;; -------------------------------------------------------------- the sources

(defn- source-of
  [lane table path]
  {:gate (first-act/depot-at-making lane) :table table :lane lane :routing-id lane :path path})

(defn last-stamp-source
  [lane]
  (source-of lane :task [:last-stamp (declaration/id-text lane)]))

(defn window-source
  [lane start n]
  (let [[from to] (rows/line-run lane start)]
    (source-of lane :line [:window from to n])))

(defn by-who-source
  [lane asker start n]
  (let [[from to] (rows/by-who-run lane asker start)]
    (source-of lane :task [:by-who from to n])))

(defn run-source
  "A run is [first last count]: neighbouring stamps, of which `count` have a
  row in the table. The source reads those rows and no other: the key of
  the last and a space sorts after that key and before the key of the next
  stamp, whose digits are greater."
  [lane table [first-stamp last-stamp n]]
  (source-of lane table [:window (rows/line-key lane first-stamp) (str (rows/line-key lane last-stamp) " ") n]))

;; ------------------------------------------------- who reads what whole

(defn asker-kind
  "What the asker is in the lane, for this read. A holder: her permission
  for the lane, or the run that she asks in, stands
  (`softland.store.read-path.lanes/either`). A visitor: she may see the
  lane, which is open to everyone, and is no holder; signed in or not.
  Outside: she may not see the lane, or it is not made.

  Unlike a read of facts, it reads her permission in a lane that everyone
  may see too: there it decides between whole and withheld
  (IMPLICIT_SPEC.md R4)."
  [lane own-rows permission-state signed-in?]
  (cond
    (nil? own-rows) :not-known-yet
    (not (lanes/made? own-rows lane)) :outside
    (= :stands permission-state) :holder
    (= :not-known-yet permission-state) :not-known-yet
    (= :everyone (lanes/who-may-see own-rows lane)) :visitor
    :else :outside))

(defn reads-whole?
  "Who reads a decision whole: the one rule. A holder reads every decision,
  those from before her permission too. A visitor reads every decision
  when the lane's acts setting says `:everyone`; else her own whole, and
  every other one withheld, given as its stamp alone. An asker outside
  reads her own decisions whole, wherever they are, and nothing else of the
  lane, not its stamps and not its last stamp. A taken act is held to the
  same rule as a refused thing.

  Her own: a decision whose thing names her id as its `:who`, the line
  row's `:who`. A thing's who is what its sender wrote, and the asker is
  what the caller says; neither is held here (the runtime will hold the
  asker against the caller). So in this build a caller that names an asker
  reads that asker's things, and a sender can put a thing into a lane that
  names another as its who, which she then reads as her own.

  `asker-text` is the printed text of the asker's id, nil for one who is
  not signed in; `who-text` is nil for a thing that names no usable who."
  {:softland/whose-call
   [{:state :not-decided
     :what "A taken act is held to the same rule of who reads a decision
            whole as a refused thing. This function is where the other
            reading would be made."
     :source "SPEC.md, \"Who may read what\", a session's reading"}]}
  [kind acts-setting asker-text who-text]
  (boolean (or (= :holder kind)
               (and (= :visitor kind) (= :everyone acts-setting))
               (and (some? asker-text) (= asker-text who-text)))))

;; ----------------------------------------------------------- the rows read

(defn- rows-of-run
  "A key that is not of the run is dropped: for a run whose ends hold a
  letter from U+D800 on, the read path makes no cut at the run's end and a
  select can give keys after it
  (`softland.store.read-path.sources/keys-of-run`)."
  [source run stamp-of]
  (->> (sources/keys-of-run source run)
       (keep (fn [k] (when-let [s (stamp-of k)] [s (sources/row-in run k)])))
       (sort-by first)
       vec))

(defn runs
  "Takes [stamp kind] pairs in the order of the stamps: `:take` is a row to
  read; `:bridge` a stamp with no row in the table read, which a run may go
  across; `:break` a stamp whose row the read must not read. Gives the
  runs, each [first last count], `count` the stamps taken. A gap in the
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
  [run-sources known]
  (into {} (mapcat (fn [source]
                     (keep (fn [[s row]] (when (some? row) [s row]))
                           (rows-of-run source (get known source) rows/stamp-of-line-key))))
        run-sources))

(defn- whole-entry
  [lane stamp row]
  {:stamp stamp
   :act-id [lane (first-act/depot-at-making lane) (:act row)]
   :answer (rows/line-answer stamp row)})

(defn- keeps-no-thing?
  [row]
  (or (some? (:not-kept row)) (not (int? (:weight row)))))

(defn- page
  "Which things a page gives, within the bound on weight
  (`:max-thing-weight-of-one-read`). The page ends before the first thing
  that does not fit in what is left, and `:next` is its stamp. A thing that
  alone weighs more than the bound is given with no thing, as
  `:heavier-than-the-read`, and the page goes on past it. A weight is the
  gate's measure (`softland.store.door.weight`), not the bytes that Rama
  moves."
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
  "The runs that fetch the things to fetch and no other thing. A decision
  whose thing is kept and not fetched, a withheld one or one heavier than
  the read, breaks a run, as a stamp that is not in the page does; one that
  keeps no thing is bridged."
  [held]
  (runs (map (fn [{:keys [stamp row] :as d}]
               [stamp (cond (:fetch? d) :take
                            (keeps-no-thing? row) :bridge
                            :else :break)])
             held)))

;; ------------------------------------------------------------- the reading

(defn- wants
  [wanted known]
  (let [wanted (into #{} (remove nil?) wanted)]
    (assoc {:wanted wanted} :missing (into #{} (remove #(contains? known %)) wanted))))

(defn- answer-of-page
  "The last round of a read: the things of a page, then the answer, a plain
  map as every answer of the read path. `:fault` when a thing that the line
  says is kept is not there.

  `:thing` is the thing as Rama gave it, the arrival that the gate decided.
  A decision that is withheld is its stamp alone.

  `:last-stamp` is the lane's last stamp when the read began, given only
  to an asker who may see the lane. nil when the lane had no decision then:
  a read whose last stamp was read before the lane was made, and its rights
  after.

  To an asker who may see the lane the stamps are consecutive from the
  start, none above the last stamp, and `:next` is the start and the count
  of what was given; she is at the end when `:next` is the last stamp and
  one. To an asker outside, `:next` is the stamp after her last decision
  given, or the start, and the answer says that the lane is otherwise left
  out."
  [lane kind decisions wanted known numbers start last-stamp own-rows]
  (let [held (page decisions (:max-thing-weight-of-one-read numbers))
        fetched (filterv :fetch? held)
        thing-sources (mapv #(run-source lane :things %) (things-runs held))
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
                 (cond-> {:lanes [lane]
                          :left-out (if outside?
                                      [(lanes/left-out lane (if (lanes/made? own-rows lane) :may-not-see :not-made))]
                                      [])
                          :acts (mapv (fn [{:keys [stamp row] :as d}]
                                        (cond
                                          (not (:whole? d)) {:stamp stamp}
                                          (:fetch? d) (assoc (whole-entry lane stamp row) :thing (get thing-of stamp))
                                          :else (assoc (whole-entry lane stamp row) :no-thing (:no-thing d))))
                                      held)
                          :next (if outside?
                                  (if (seq held) (inc (long (:stamp (peek held)))) start)
                                  (+ (long start) (count held)))}
                   (not outside?) (assoc :last-stamp last-stamp))))))))

(defn- first-gap
  [start pairs]
  (some (fn [[i [s]]] (when (not= (long s) (+ (long start) (long i))) (+ (long start) (long i))))
        (map-indexed vector pairs)))

(defn- inside-reading
  [lane kind whole? wanted known numbers start n last-stamp own-rows]
  (let [expected (if (and (some? last-stamp) (<= (long start) (long last-stamp)))
                   (min (long n) (inc (- (long last-stamp) (long start))))
                   0)]
    (if (zero? expected)
      (answer-of-page lane kind [] wanted known numbers start last-stamp own-rows)
      (let [window (window-source lane start n)
            wanted (conj wanted window)]
        (if-not (contains? known window)
          (wants wanted known)
          (let [rows (->> (rows-of-run window (get known window) rows/stamp-of-line-key)
                          (filterv (fn [[s]] (<= (long s) (long last-stamp)))))
                gap (or (first-gap start rows) (when (< (count rows) expected) (+ (long start) (count rows))))]
            (if gap
              (assoc (wants wanted known) :fault {:stamp gap})
              (answer-of-page lane kind
                              (mapv (fn [[s row]] {:stamp s :whole? (whole? row) :row row}) (take expected rows))
                              wanted known numbers start last-stamp own-rows))))))))

(defn- outside-reading
  [lane asker wanted known numbers start n own-rows]
  (if (nil? asker)
    (answer-of-page lane :outside [] wanted known numbers start nil own-rows)
    (let [by-who (by-who-source lane asker start n)
          wanted (conj wanted by-who)]
      (if-not (contains? known by-who)
        (wants wanted known)
        (let [stamps (mapv first (rows-of-run by-who (get known by-who) rows/stamp-of-by-who-key))
              line-sources (mapv #(run-source lane :line %) (runs (map (fn [s] [s :take]) stamps)))
              wanted (into wanted line-sources)]
          (if (some #(not (contains? known %)) line-sources)
            (wants wanted known)
            (let [row-of (values-of-runs line-sources known)
                  lost (some #(when-not (contains? row-of %) %) stamps)]
              (if lost
                (assoc (wants wanted known) :fault {:stamp lost})
                (answer-of-page lane :outside
                                (mapv (fn [s] {:stamp s :whole? true :row (get row-of s)}) stamps)
                                wanted known numbers start nil own-rows)))))))))

(defn reading
  "What a read of acts wants now, or its answer. `known` is a map from a
  source to the value that a select gave. Gives {:wanted :missing} while
  the read needs more; else that with `:answer`, or with `:fault`.

  `:fault` is {:stamp s}, the first stamp at which the line is not whole: a
  stamp up to the last stamp with no line row in the window, a stamp of her
  own with no line row, or a thing that the line says is kept and is not
  there. The read then gives a word, never an answer that ends there.

  At most six rounds, for an asker outside whose permission's rows above
  must be read, and five for one who may see the lane. A kind that is not
  known when every source of the rights is known cannot be reached; it is
  read as outside, which shows the least."
  [query known numbers]
  (let [[_ start most] (:asks query)
        start (long start)
        lane (lanes/home query)
        n (min (long most) (long (:max-acts-of-one-read numbers)))
        asker (:asker query)
        session (:session query)
        signed-in? (some? asker)
        last-source (last-stamp-source lane)]
    (if-not (contains? known last-source)
      (wants [last-source] known)
      (let [own (sources/own-rows-source lane lane)
            permission (when signed-in? (sources/permission-source lane lane asker))
            run-permission (when (and signed-in? session) (sources/run-permission-source lane lane session))
            rights [last-source own permission run-permission]]
        (if (some #(and % (not (contains? known %))) rights)
          (wants rights known)
          (let [above (into (if permission (sources/chain-sources lane lane (get known permission)) [])
                            (when run-permission
                              (sources/chain-sources lane lane (sources/askers-run-row (get known run-permission) asker))))
                wanted (into rights above)]
            (if (some #(not (contains? known %)) above)
              (wants wanted known)
              (let [own-rows (get known own)
                    kind (let [k (asker-kind lane own-rows (sources/permission-state query lane lane known) signed-in?)]
                           (if (= :not-known-yet k) :outside k))
                    setting (lanes/who-may-read-acts own-rows lane)
                    asker-text (when signed-in? (declaration/id-text asker))
                    whole? (fn [row] (reads-whole? kind setting asker-text (:who row)))]
                (if (= :outside kind)
                  (outside-reading lane asker wanted known numbers start n own-rows)
                  (inside-reading lane kind whole? wanted known numbers start n
                                  (get known last-source) own-rows))))))))))
