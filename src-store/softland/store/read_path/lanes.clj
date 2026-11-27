(ns softland.store.read-path.lanes
  "The lanes of a read: which lanes a read looks through and in what order,
  whether a lane is made, whether an asker may see it, and what a read says
  of a lane that it left out. Pure code.

  Whether an asker may see a lane is the asker's rights in it, read as one
  thing from the rows of one moment (`rights-begin`, `rights`): the
  lane's depot; the permissions that she holds there, her own and
  everyone's, found by holder
  (`softland.store.record.rows/by-holder-key`); and the rows above each
  that is lent. She sees a lane exactly when one of those permissions
  stands, allows seeing at that moment, and covers the lane's default
  group in the sitting that she asks in. Whether a permission stands and
  what it allows are the gates' own rule, read from the same rows
  (`softland.store.gate.chain`), so a gate and a read never differ on
  either. No setting of a lane and no value in code lets anyone see.

  A permission for one exact act lets nobody see the lane.

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

(defn- holders-for
  "The holders whose permissions an asker holds: herself; anyone at all,
  when she is a person or is not signed in, which `asker` nil says; anyone
  signed in, when she is a person (`softland.store.gate.chain/held-by?`)."
  [asker]
  (cond
    (nil? asker) [:anyone]
    (= :person (act/party-kind asker)) [asker :anyone :anyone-signed-in]
    :else [asker]))

(defn- holder-reads
  "The runs of the order by holder that hold every permission which could
  let the asker see the lane: for each holder that she holds as, those good
  in every sitting, and those good in the sitting that she asks in."
  [lane asker session]
  (vec (for [holder (holders-for asker)
             sitting (if (some? session) [nil session] [nil])]
         (into [:holders] (rows/by-holder-run lane holder sitting)))))

(defn- depot-read
  [lane]
  [:fact (rows/place-key lane lane :softland.store/depot)])

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

(defn- rights-step
  "Goes through the permissions that are left, in order, until one lets the
  asker see, one needs a row that is not read, or none is left."
  [{:keys [lane asker session rows] :as state}]
  (loop [ids (:ids state)]
    (if-let [id (first ids)]
      (let [{:keys [values wants whole?]} (chain/climb lane id rows)]
        (cond
          wants (assoc state :ids ids :reads [[:fact wants]])
          (and whole? (sees-under? lane asker session values))
          (assoc state :ids [] :reads [] :rights {:made? true :sees? true})
          :else (recur (rest ids))))
      (assoc state :ids [] :reads [] :rights {:made? true :sees? false}))))

(defn rights-begin
  "The read of an asker's rights in a lane, as a walk in steps, like the
  walk of a gate (`softland.store.gate.walk`): `rights-reads` says what it
  wants read now, `rights-resume` takes what those reads gave, and when it
  wants nothing `rights` says what she holds. `asker` is nil for one who
  is not signed in; `session` is the sitting that she asks in, or nil.

  A read is `[:fact key]`, a row of the facts, or `[:holders from to]`, a
  run of the order by holder, `to` not in it, which gives a map from key
  to stamp.

  First the lane's depot and the holder runs, together. Then, in a lane
  that is made, one row at a time: the row of a permission that the runs
  name, and each row above one that is lent, until a permission lets her
  see or none is left. So every row that the answer rests on is one that
  the reader was asked for, and a reader that makes every read in one turn
  on the lane's task holds them as of one moment.

  Limit: the number of permissions that the runs name has no bound here."
  [lane asker session]
  {:lane lane :asker asker :session session :rows {}
   :stage :first
   :reads (into [(depot-read lane)] (holder-reads lane asker session))})

(defn rights-reads
  "What the read of an asker's rights wants read now. Empty when it is
  done."
  [state]
  (:reads state))

(defn rights-resume
  "Takes the state and a map from each read that `rights-reads` named to
  what it gave. Gives the state after."
  [{:keys [lane asker session] :as state} got]
  (case (:stage state)
    :first
    (if (nil? (get got (depot-read lane)))
      (assoc state :reads [] :rights {:made? false :sees? false})
      (rights-step (assoc state
                          :stage :permissions
                          :ids (into []
                                     (comp (mapcat (fn [read] (sort (keys (get got read)))))
                                           (keep rows/permission-of-by-holder-key)
                                           (distinct))
                                     (holder-reads lane asker session)))))

    :permissions
    (rights-step (update state :rows into
                         (map (fn [[_ k :as read]] [k (rows/read-as-a-permission (get got read))]))
                         (:reads state)))))

(defn rights
  "An asker's rights in a lane, `{:made? :sees?}`, for a read that is done;
  nil for one that is not."
  [state]
  (:rights state))

(defn standing
  "Is a lane made, and may this asker see it. The one function of who may
  see a lane. `rights` is `rights`'s, or nil while they are not known.
  `:system` is a party like any other: it sees what it holds."
  [rights]
  (cond
    (nil? rights) :not-known-yet
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
  "What a lane's own rows and the asker's rights in it mean for one query,
  as a value that `=` compares. The list that the lane builds on, and the
  stamp of the asked key's mark, are in it only when she may see the lane.

  A reader that is kept live compares the meaning before and after a
  change. A list or a mark that is written in a lane that she may not see
  means the same: not even an answer that says what it said is given,
  which would tell her that something of that lane was written."
  [query lane own-rows rights]
  (if (or (nil? own-rows) (nil? rights))
    :not-known-yet
    (let [seen? (= :yes (standing rights))
          [word k] (:asks query)]
      (cond-> {:made? (boolean (:made? rights))
               :sees? (boolean (:sees? rights))}
        (and seen? (= lane (:from-lane query)))
        (assoc :builds-on (builds-on own-rows lane))

        (and seen? (= :points-at word))
        (assoc :marked-since (marked-since own-rows lane k))))))
