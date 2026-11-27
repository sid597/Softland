(ns softland.store.read-path.lanes
  "The lanes of a read: which lanes a read looks through and in what order,
  whether a lane is made, whether an asker may see it, and what a read says
  of a lane that it left out. Pure code.

  Whether an asker may see a lane, or what she is in a read of its acts, is
  her rights in it, read in one turn on the task from the rows of one moment
  (`rights-begin`, `rights-snapshot`): the lane's depot; the permissions by
  holder, bounded at `max-holder-permissions`; and the rows above each that
  is lent, bounded at `softland.store.gate.chain/max-lent-depth`. For `:see`
  it reads `:anyone` first and stops at the first permission that stands,
  allows seeing now, and covers the lane's default group in the sitting
  asked, so public viewers share pushes. For `:acts` it reads the asker's
  own runs first (`:holder`), and then everyone's (`:visitor`), else
  `:outside`. Every row and holder run read in the turn is returned in
  `:read-set` with its stamp or stamps; a cut at either bound gives
  `:too-many-permissions` and never a false `:may-not-see`.

  Whether a permission stands and what it allows are the gates' own rule,
  read from the same rows (`softland.store.gate.chain`), so a gate and a
  read never differ on either. No setting of a lane and no value in code
  lets anyone see. A permission for one exact act lets nobody see the lane.

  The other functions take a lane's own rows, a map from a row's key to the
  row, nil while they are not known: they are neighbours in the table, so
  one read gives them together
  (`softland.store.record.rows/own-rows-run`).

  No function here throws on a row."
  (:require [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.gate.chain :as chain]
            [softland.store.gate.first-act :as first-act]
            [softland.store.record.rows :as rows]))

(def max-holder-permissions
  "The most permissions in one holder run, or distinct candidate permissions
  across an asker's holder runs in one turn, that `lane-rights` examines
  before giving `:too-many-permissions`."
  32)

(defn outside-the-agreed-range?
  "True for a text that holds a UTF-16 unit from U+D800 on: a letter outside
  the first plane, or one from U+E000 on. Only for such a text can Java's
  order of Strings and the order by code point give two answers."
  [^String k]
  (let [n (.length k)]
    (loop [i 0]
      (cond (>= i n) false
            (>= (int (.charAt k i)) 0xD800) true
            :else (recur (inc i))))))

(defn holder-range-agreed?
  "True when both ends of a holder run lie below U+D800, so Rama's two-ended
  `sorted-map-range` cut agrees with the table's code-point order."
  [from to]
  (and (string? from)
       (string? to)
       (not (outside-the-agreed-range? from))
       (not (outside-the-agreed-range? to))))

(defn clean-holder-run
  "A plain `{String Long}` map of the entries of `raw-run` whose keys lie in
  `[from, to)` under the table's code-point order. Iterates `(seq raw-run)`
  directly so a client submap with characters from U+D800 on never misses a
  key by UTF-16 lookup."
  [from to raw-run]
  (if (empty? raw-run)
    {}
    (let [check-bounds? (not (holder-range-agreed? from to))]
      (persistent!
       (reduce (fn [acc [k v]]
                 (if (and (string? k)
                          (int? v)
                          (or (not check-bounds?)
                              (and (not (rows/before? k from))
                                   (rows/before? k to))))
                   (assoc! acc k (long v))
                   acc))
               (transient {})
               raw-run)))))

(defn valid-rights-query?
  "The test at the head of the `lane-rights` query topology: nil/false when
  any argument is outside the declared domain or routes a streaming lane to
  another task. It never throws."
  [routing-id lane question asker session]
  (try
    (boolean
     (and (declaration/usable-id? routing-id)
          (declaration/usable-id? lane)
          (or (= :microbatch (first-act/depot-at-making lane))
              (= routing-id lane))
          (contains? #{:see :acts} question)
          (or (nil? asker) (and (act/author-party? asker) (declaration/id-within-the-limit? asker)))
          (or (nil? session) (declaration/usable-id? session))))
    (catch Throwable _ false)))

(defn own-row
  [own-rows lane k]
  (get own-rows (rows/place-key lane lane k)))

(defn made?
  "A lane is made when its own rows hold its depot fact. What must hold: the
  rows are of the record that
  `softland.store.gate.first-act/depot-at-making` names for the lane."
  [own-rows lane]
  (some? (own-row own-rows lane :softland.store/depot)))

(defn who-may-read-acts
  "Who may read the lane's acts whole. A row whose value is neither word
  reads as `:members`, the answer that shows less. Only a read of a lane's
  acts asks it (`softland.store.read-path.acts`): a read of facts reads a
  lane's facts whatever the setting says."
  [own-rows lane]
  (let [row (own-row own-rows lane :softland.store/who-may-read-acts)
        [kind v] (rows/read-value row)]
    (cond
      (nil? row) (first-act/starts-with lane :softland.store/who-may-read-acts)
      (and (= :value kind) (= :everyone v)) :everyone
      :else :members)))

(defn builds-on
  "An id of the list comes from a row and not from a caller, so each is
  tested here (`softland.store.door.declaration/usable-id?`) and one that
  fails is left out: a key is made of each."
  [own-rows lane]
  (let [row (own-row own-rows lane :softland.store/builds-on)
        [kind v] (rows/read-value row)]
    (cond
      (nil? row) (first-act/starts-with lane :softland.store/builds-on)
      (and (= :value kind) (vector? v)) (filterv declaration/usable-id? v)
      :else [])))

(defn marked-since
  [own-rows lane k]
  (get (rows/marked-keys-of (own-row own-rows lane :softland.store/marked-keys)) k))

;; ------------------------------------------------------ an asker's rights

(defn- holder-plan
  "Ordered `[holder sitting match-standing]` triples for one turn of
  `lane-rights`. On `:see`, `:anyone` comes first (with `nil` sitting before
  `session`) so all viewers of an open lane short-circuit on the same shared
  runs; `:anyone-signed-in` comes next for a person, then `asker`. On
  `:acts`, `asker` comes first (`:holder`), then everyone (`:visitor`), so
  a member of an open lane is never mistaken for a visitor."
  [question asker session]
  (let [sittings (if (and (some? asker) (some? session)) [nil session] [nil])
        runs-of (fn [holders match]
                  (for [h holders
                        s (if (and (= :anyone h) (nil? asker)) [nil] sittings)]
                    [h s match]))]
    (case question
      :see
      (cond
        (nil? asker) (vec (runs-of [:anyone] :yes))
        (= :person (act/party-kind asker)) (vec (runs-of [:anyone :anyone-signed-in asker] :yes))
        (act/author-party? asker) (vec (runs-of [asker] :yes))
        :else [])

      :acts
      (cond
        (nil? asker) (vec (runs-of [:anyone] :visitor))
        (= :person (act/party-kind asker)) (into (vec (runs-of [asker] :holder))
                                                 (runs-of [:anyone :anyone-signed-in] :visitor))
        (act/author-party? asker) (vec (runs-of [asker] :holder))
        :else [])

      [])))

(defn- depot-read
  [lane]
  [:fact (rows/place-key lane lane :softland.store/depot)])

(defn- acts-setting-read
  [lane]
  [:fact (rows/place-key lane lane :softland.store/who-may-read-acts)])

(defn- sees-under?
  "True when the permission whose chain these values are lets the asker see
  the lane, asked in this sitting. `values` is
  `softland.store.gate.chain/climb`'s, of a chain that is whole."
  [lane asker session values]
  (let [says (first values)]
    (boolean (and (chain/held-by? (:holder says) asker)
                  (= [:default-group lane] (:covers says))
                  (or (nil? (:session says)) (= session (:session says)))
                  (:see (chain/allows-now values))))))

(defn- finish-rights
  [state made? sees? standing-word]
  (assoc state
         :ids []
         :plan []
         :reads []
         :rights {:made? made? :sees? sees? :standing standing-word}
         :snapshot (cond-> {:standing standing-word :read-set (:read-set state)}
                     (contains? state :who-may-read-acts)
                     (assoc :who-may-read-acts (:who-may-read-acts state)))))

(defn- advance-rights
  "Walks candidate permissions and remaining holder runs until a standing
  permission answers the question, a row or holder run must be read next, a
  bound is hit, or nothing is left."
  [state]
  (loop [{:keys [lane question asker session ids plan seen-ids cut-chain? rows] :as st} state]
    (if-let [id (first ids)]
      (cond
        (and (not (contains? seen-ids id))
             (<= max-holder-permissions (count seen-ids)))
        (finish-rights st true false :too-many-permissions)

        :else
        (let [seen-ids (conj seen-ids id)
              st (assoc st :seen-ids seen-ids)
              {:keys [values wants whole?]} (chain/climb lane id rows)]
          (cond
            wants
            (assoc st :ids ids :reads [[:fact wants]])

            (and whole? (sees-under? lane asker session values))
            (finish-rights st true true (:match-standing st))

            :else
            (let [chain-cut? (and (not whole?) (<= chain/max-lent-depth (count values)))]
              (recur (assoc st
                            :ids (subvec ids 1)
                            :cut-chain? (or cut-chain? chain-cut?)))))))
      (if-let [[holder sitting match-standing] (first plan)]
        (let [[from to] (rows/by-holder-run lane holder sitting)]
          (assoc st
                 :match-standing match-standing
                 :plan (subvec plan 1)
                 :reads [[:holders from to]]))
        (if cut-chain?
          (finish-rights st true false :too-many-permissions)
          (finish-rights st true false (if (= :acts question) :outside :may-not-see)))))))

(defn rights-begin
  "The read of an asker's rights in a lane, as a walk in steps, like the
  walk of a gate (`softland.store.gate.walk`): `rights-reads` says what it
  wants read now, `rights-resume` takes what those reads gave, and when it
  wants nothing `rights` and `rights-snapshot` say what she holds and which
  rows and holder runs the answer rests on.

  `question` is `:see` (default in the 3-argument form) or `:acts`. `asker`
  is nil for one who is not signed in; `session` is the sitting that she
  asks in, or nil.

  A read is `[:fact key]`, a row of the facts, or `[:holders from to]`, a
  run of the order by holder, `to` not in it, which gives `{key stamp}`."
  ([lane asker session]
   (rights-begin lane :see asker session))
  ([lane question asker session]
   (let [effective-session (when (some? asker) session)]
     {:lane lane
      :question question
      :asker asker
      :session effective-session
      :stage :depot
      :reads (if (= :acts question)
               [(depot-read lane) (acts-setting-read lane)]
               [(depot-read lane)])
      :plan (holder-plan question asker effective-session)
      :ids []
      :match-standing nil
      :seen-ids #{}
      :cut-chain? false
      :rows {}
      :read-set {}})))

(defn rights-reads
  "What the read of an asker's rights wants read now. Empty when it is
  done."
  [state]
  (:reads state))

(defn rights-resume
  "Takes the state and a map from each read that `rights-reads` named to
  what it gave. Gives the state after."
  [{:keys [lane question] :as state} got]
  (case (:stage state)
    :depot
    (let [read (depot-read lane)
          row (get got read)
          state (assoc state :read-set (assoc (:read-set state) read (:stamp row)))]
      (if (nil? row)
        (finish-rights state false false (if (= :acts question) :outside :may-not-see))
        (let [state (if (= :acts question)
                      (let [s-read (acts-setting-read lane)
                            s-row (get got s-read)]
                        (-> state
                            (update :read-set assoc s-read (:stamp s-row))
                            (assoc :who-may-read-acts (who-may-read-acts {(second s-read) s-row} lane))))
                      state)]
          (advance-rights (assoc state :stage :walk)))))

    :walk
    (let [[which a b :as read] (first (:reads state))]
      (case which
        :holders
        (let [run-map (clean-holder-run a b (get got read))
              state (assoc state :read-set (assoc (:read-set state) read run-map))]
          (if (< max-holder-permissions (count run-map))
            (finish-rights state true false :too-many-permissions)
            (let [new-ids (into []
                                (comp (keep rows/permission-of-by-holder-key)
                                      (remove (:seen-ids state))
                                      (distinct))
                                (sort (keys run-map)))]
              (advance-rights (assoc state :ids new-ids)))))

        :fact
        (let [raw-row (get got read)
              state (-> state
                        (update :rows assoc a (rows/read-as-a-permission raw-row))
                        (update :read-set assoc read (:stamp raw-row)))]
          (advance-rights state))))))

(defn rights
  "An asker's rights in a lane, `{:made? :sees? :standing}`, for a read that
  is done; nil for one that is not."
  [state]
  (:rights state))

(defn rights-snapshot
  "`{:standing ... :read-set ...}` as returned by the `lane-rights` query
  topology (`CONTRACT.md` §2.4), for a read that is done; nil for one that
  is not."
  [state]
  (:snapshot state))

(defn standing
  "Is a lane made, and may this asker see it. The one function of who may
  see a lane. `rights` is `rights`'s, or nil while they are not known.
  `:system` is a party like any other: it sees what it holds."
  [rights]
  (cond
    (nil? rights) :not-known-yet
    (= :too-many-permissions (:standing rights)) :too-many-permissions
    (not (:made? rights)) :not-made
    (:sees? rights) :yes
    :else :may-not-see))

(defn meaning-standing
  "The standing that a meaning of `meaning` holds, as `standing` gives it;
  nil for nil, a source that is no rights source."
  [meant]
  (cond
    (nil? meant) nil
    (= :not-known-yet meant) :not-known-yet
    (not (:made? meant)) :not-made
    (:sees? meant) :yes
    :else :may-not-see))

(defn in-order
  "The lanes that a read looks through, nearest first, each once; nil while
  the list cannot be said yet.

  For `:from-lane` s: s, then the lanes of s's builds-on fact, and no
  further: the lists of those lanes are not read. When she may not see s,
  or s is not made: s alone, since the list is a fact of a lane that she
  may not see, and the read does not use it."
  [query start-own-rows start-standing]
  (if-let [given (:lanes query)]
    (vec (distinct given))
    (let [start (:from-lane query)]
      (case start-standing
        :yes (vec (distinct (cons start (builds-on start-own-rows start))))
        (:may-not-see :not-made) [start]
        nil))))

(defn home
  "The lane that a read starts from. It is read from the query alone, so it
  is the same in every round of a read and for as long as a read is kept
  live, whatever the tables hold: a home that is not made, or that the
  asker may not see, is the home all the same.

  The home says where a microbatch lane is read, and nothing else
  (`softland.store.read-path.sources`)."
  [query]
  (or (:from-lane query) (first (:lanes query))))

(defn left-out
  "What an answer says of a lane that gave nothing. `why` is `:may-not-see`
  or `:not-made` (`softland.store.read-path.words/left-out-words`)."
  [lane why]
  {:lane lane :why why})

(defn meaning
  "What a lane's own rows and the asker's standing in it mean for one query,
  as a value that `=` compares. `rights-or-standing` is `rights`'s map or a
  standing word (`:yes`, `:may-not-see`, `:not-made`, `:not-known-yet`). The
  list that the lane builds on, and the stamp of the asked key's mark, are
  in it only when she may see the lane.

  A reader that is kept live compares the meaning before and after a
  change. A list or a mark that is written in a lane that she may not see
  means the same: not even an answer that says what it said is given,
  which would tell her that something of that lane was written."
  [query lane own-rows rights-or-standing]
  (let [st (if (keyword? rights-or-standing)
             rights-or-standing
             (standing rights-or-standing))]
    (if (or (nil? own-rows) (nil? st) (= :not-known-yet st) (= :too-many-permissions st))
      :not-known-yet
      (let [made? (not= :not-made st)
            seen? (= :yes st)
            [word k] (:asks query)]
        (cond-> {:made? made?
                 :sees? seen?}
          (and seen? (= lane (:from-lane query)))
          (assoc :builds-on (builds-on own-rows lane))

          (and seen? (= :points-at word))
          (assoc :marked-since (marked-since own-rows lane k)))))))
