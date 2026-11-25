(ns softland.store.read-path.lanes
  "The lanes of a read: which lanes a read looks through and in what order,
  whether a lane is made, whether an asker may see it, and what a read says
  of a lane that it left out. Pure code.

  What the functions take, read from the facts table:

    a lane's own rows   a map from a row's key to the row; nil says that
                        they are not known yet. They are neighbours in the
                        table, so one read, or one push, gives them together
                        (`softland.store.record.rows/own-rows-run`)
    her permission row  nil says that she holds none, and `:not-known-yet`
                        that it is not read. A row with no value is an end
    her run's row       the row of the run that a query is asked in; hers
                        only when its permission names her as its holder
    the rows above      a map from the key of a place to its row;
                        `:not-known-yet` says that one of them is not read.
                        A permission stands only while each one above it
                        stands (`softland.store.gate.chain`)

  \"Standing\" here is whether an asker may see a lane, as the read path
  knows it. `softland.store.gate.decision` uses the same word for a
  permission that stands at a gate; the two are different questions.

  What the read path does not read. What a permission allows: any
  permission that stands allows seeing. Whether it stands is the gates' own
  rule, read from the same rows (`softland.store.gate.chain/stands?`): a
  permission that has ended, or beneath one that has, lets nobody see. A
  permission for one act is not read: it lets nobody see."
  (:require [softland.store.door.declaration :as declaration]
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

(defn who-may-see
  "With no row it is what the lane starts with
  (`softland.store.gate.first-act/starts-with`). A row whose value is
  neither word reads as `:members`, the answer that shows less."
  [own-rows lane]
  (let [row (own-row own-rows lane :softland.store/who-may-see)
        [kind v] (rows/read-value row)]
    (cond
      (nil? row) (first-act/starts-with lane :softland.store/who-may-see)
      (and (= :value kind) (= :everyone v)) :everyone
      :else :members)))

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

(defn permission-state
  "Whether the asker holds a permission that stands in a lane, by the
  gates' rule (`softland.store.gate.chain/stands?`). `:none` also for an
  end, for a row that holds no permission, and for a permission beneath one
  that ended or was given again later.

  It reads the permission from her row and not from a caller: what is
  asked is whether the permission that the record holds for her stands."
  [lane permission-row chain-rows]
  (if (= :not-known-yet permission-row)
    :not-known-yet
    (if-let [held (rows/held-permission permission-row)]
      (cond
        (= :not-known-yet chain-rows) :not-known-yet
        (chain/stands? lane held (assoc chain-rows (chain/place lane held) permission-row)) :stands
        :else :none)
      :none)))

(defn run-permission-state
  "As `permission-state`, for the row of the run that she asks in, the
  query's `:session`. `:stands` only when the row's permission names the
  asker as its holder: a run's permission is its holder's, and another who
  that asks in a run of the same id is given nothing by it. What the
  permission allows is not read: `:see` is enough for a read."
  [lane asker run-row chain-rows]
  (if (= :not-known-yet run-row)
    :not-known-yet
    (let [held (rows/held-permission run-row)]
      (cond
        (or (nil? held) (not= asker (nth held 0))) :none
        (= :not-known-yet chain-rows) :not-known-yet
        (chain/stands? lane held (assoc chain-rows (chain/place lane held) run-row)) :stands
        :else :none))))

(defn either
  "An asker may see a lane that is open to its members under her permission
  for the lane, or under the permission of the run that she asks in
  (src/proposal/runtime-2026-11-21/store-piece-2/SPEC.md, R8). A permission
  for one act lets nobody see, and is not read here."
  [for-the-lane for-the-run]
  (cond
    (or (= :stands for-the-lane) (= :stands for-the-run)) :stands
    (or (= :not-known-yet for-the-lane) (= :not-known-yet for-the-run)) :not-known-yet
    :else :none))

(defn standing
  "Is a lane made, and may this asker see it. The one function of who may
  see a lane. `permission-state` is what `either` gives. For a lane that is
  open to everyone her permission is not read.

  Any permission that stands allows seeing; what it allows beyond that is
  not read. `:system` is a who like any other."
  [lane own-rows permission-state signed-in?]
  (cond
    (nil? own-rows) :not-known-yet
    (not (made? own-rows lane)) :not-made
    (= :everyone (who-may-see own-rows lane)) :yes
    (not signed-in?) :may-not-see
    :else (case permission-state
            :not-known-yet :not-known-yet
            :stands :yes
            :may-not-see)))

(defn meaning-standing
  "The standing that a meaning of `meaning` holds, as `standing` gives it;
  nil for nil, a source that is no rights source. What must hold: the
  meaning holds what `standing` reads. An asker who is not signed in holds
  no permission that stands, so the meaning of her rights is `:none`."
  [meant]
  (cond
    (nil? meant) nil
    (= :not-known-yet meant) :not-known-yet
    (not (:made? meant)) :not-made
    (= :everyone (:who-may-see meant)) :yes
    (= :stands (:permission meant)) :yes
    (= :not-known-yet (:permission meant)) :not-known-yet
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
  stamp of the asked key's mark, are in it only when her standing with
  these rows is `:yes`.

  A live read compares the meaning of the rows before and after a change. A
  setting that is written again with the same word means the same, so the
  asker is given nothing. A list or a mark that is written in a lane that
  she may not see means the same too: not even an answer that says what it
  said is given, which would tell her that something of that lane was
  written. The setting itself is in the meaning whatever her standing: a
  setting that goes to \"members\" for an asker who holds a permission
  leaves her standing as it was, and a change of a fact that was passed
  over while her permission was read must still be told."
  [query lane own-rows permission-state signed-in?]
  (if (nil? own-rows)
    :not-known-yet
    (let [seen? (= :yes (standing lane own-rows permission-state signed-in?))
          [word k] (:asks query)]
      (cond-> {:made?       (made? own-rows lane)
               :who-may-see (who-may-see own-rows lane)
               :permission  permission-state}
        (and seen? (= lane (:from-lane query)))
        (assoc :builds-on (builds-on own-rows lane))

        (and seen? (= :points-at word))
        (assoc :marked-since (marked-since own-rows lane k))))))
