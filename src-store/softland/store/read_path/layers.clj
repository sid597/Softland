(ns softland.store.read-path.layers
  "The layers of a read: which layers a read looks through and in what
  order, whether a layer is made, whether an asker may see it, and what a
  read says of a layer that it left out. Pure code. It needs no cluster.

  Everything here is read from two kinds of row of the facts table
  (`softland.store.record.rows`, the namespace docstring):

    the layer's own rows  the rows of the layer's own entity under the
                          store's own keys: its depot, who may see it, what
                          it builds on and its marked keys. They are
                          neighbours in the table, so one read, or one push,
                          gives the four together
                          (`softland.store.record.rows/own-rows-run`). Here
                          they are a map from a row's key to the row; nil
                          says that they are not known yet
    her permission row    the row at the asker's place under
                          `:softland.store/permission`; nil says that she
                          holds none, and `:not-known-yet` that it is not
                          read. A row with no value is an end
    the rows above it     the rows at the places of the permissions above
                          the one that her row holds, as a map from the key
                          of a place to its row; `:not-known-yet` says that
                          one of them is not read. Her permission stands
                          only while each one above it stands
                          (`softland.store.gate.chain`)

  Inputs and results, by call:

    own-row       own rows, a layer, a store key -> the row, or nil
    made?         own rows, a layer -> true when the layer is made
    who-may-see   own rows, a layer -> `:members` or `:everyone`
    builds-on     own rows, a layer -> the layers that it builds on
    marked-since  own rows, a layer, a key -> the stamp of the key's mark
    permission-state
                  a layer, her permission row, the rows above it ->
                  `:stands`, `:none` or `:not-known-yet`
    run-permission-state
                  a layer, the asker, the row of the run that she asks in,
                  the rows above it -> the same three words
    either        two of those words -> one: whether she holds a
                  permission that stands, of either kind
    standing      a layer, its own rows, whether she holds a permission
                  that stands, whether she is signed in -> `:yes`,
                  `:may-not-see`, `:not-made` or `:not-known-yet`
    in-order      a query, the start layer's own rows and standing -> the
                  layers of the read, nearest first, or nil
    home          a query -> the layer that the read starts from
    left-out      a layer and why -> what an answer says of it
    meaning       a query, a layer, its own rows, whether she holds a
                  permission that stands, whether she is signed in -> what
                  the rows mean for this query, as a value that `=` compares

  \"Standing\" here is whether an asker may see a layer, as the read path
  knows it now. `softland.store.gate.decision` uses the same word for a
  permission that stands at a gate; the two are different questions.

  State and effects. None. Each function is pure, and none throws on a row.

  Callers. `softland.store.read-path.sources` and
  `softland.store.read-path.answer`.

  What the read path does not read. What a permission allows: any
  permission that stands allows seeing (src-store/README.md, decisions of
  11 and 12 October 2026, entry 1). Whether it stands is the gates' own
  rule, read from the same rows (`softland.store.gate.chain/stands?`): a
  permission that has ended, or beneath one that has, lets nobody see. A
  permission for one act is not read: it lets nobody see. So the one thing
  about a permission that is kept as built and not decided, one permission
  for the layer for each holder at a time, is no code here."
  (:require [softland.store.door.declaration :as declaration]
            [softland.store.gate.chain :as chain]
            [softland.store.gate.first-act :as first-act]
            [softland.store.record.rows :as rows]))

(defn own-row
  "Takes a layer's own rows, the layer, and one of the store's own keys that
  sit on the layer's own entity. Gives the row under that key, or nil when
  the rows hold none or are not known. What must hold: the layer is an id
  within the limit."
  [own-rows layer k]
  (get own-rows (rows/place-key layer layer k)))

(defn made?
  "Takes a layer's own rows and the layer. Gives true when the layer is
  made: its own rows hold its depot fact. The rows are those of the record
  that `softland.store.gate.first-act/depot-at-making` names for the layer,
  since that is the table that the read path reads them from."
  [own-rows layer]
  (some? (own-row own-rows layer :softland.store/depot)))

(defn who-may-see
  "Takes a layer's own rows and the layer. Gives who may see the layer:
  `:everyone` or `:members`. With no row it is what the layer starts with
  (`softland.store.gate.first-act/starts-with`). A row whose value is
  neither word reads as `:members`, the answer that shows less.

  Not decided. What a layer starts with is kept only so that the build can
  run; `starts-with` is the one place where the other answer would be
  made, and this function is its one caller in the read path."
  [own-rows layer]
  (let [row (own-row own-rows layer :softland.store/who-may-see)
        [kind v] (rows/read-value row)]
    (cond
      (nil? row) (first-act/starts-with layer :softland.store/who-may-see)
      (and (= :value kind) (= :everyone v)) :everyone
      :else :members)))

(defn builds-on
  "Takes a layer's own rows and the layer. Gives the layers that the layer
  builds on, in the order of the fact, as a vector. With no row it is what
  the layer starts with, nothing. An id of the list comes from a row and
  not from a caller, so each is tested here
  (`softland.store.door.declaration/usable-id?`) and one that fails is left
  out: a key is made of each. A row whose value is no vector gives nothing."
  [own-rows layer]
  (let [row (own-row own-rows layer :softland.store/builds-on)
        [kind v] (rows/read-value row)]
    (cond
      (nil? row) (first-act/starts-with layer :softland.store/builds-on)
      (and (= :value kind) (vector? v)) (filterv declaration/usable-id? v)
      :else [])))

(defn marked-since
  "Takes a layer's own rows, the layer and a key. Gives the stamp since
  which the layer's order by value is level for that key
  (`softland.store.record.rows/marked-keys-of`), or nil when the layer does
  not mark the key."
  [own-rows layer k]
  (get (rows/marked-keys-of (own-row own-rows layer :softland.store/marked-keys)) k))

(defn permission-state
  "Does the asker hold a permission that stands in a layer. Takes the layer;
  her permission row in it (a row, nil for none, or `:not-known-yet`); and
  the rows above it, a map from the key of a place to its row, or
  `:not-known-yet`. Gives one of three words:

    :not-known-yet  her row is not read; or it holds a permission, and a
                    row above it is not read
    :stands         her row holds a permission, and it stands by the gates'
                    rule (`softland.store.gate.chain/stands?`): each
                    permission above it stands, with an older row
    :none           she holds no permission that stands: no row, an end, a
                    row that holds no permission, or a permission beneath
                    one that ended or was given again later

  It reads the permission from her row and not from a caller: what is
  asked is whether the permission that the record holds for her stands."
  [layer permission-row chain-rows]
  (if (= :not-known-yet permission-row)
    :not-known-yet
    (if-let [held (rows/held-permission permission-row)]
      (cond
        (= :not-known-yet chain-rows) :not-known-yet
        (chain/stands? layer held (assoc chain-rows (chain/place layer held) permission-row)) :stands
        :else :none)
      :none)))

(defn run-permission-state
  "Does the run that the asker asks in hold a permission of hers that
  stands in a layer. Takes the layer; the asker; the row at the place of
  the run's permission, the run being the query's `:session` (a row, nil
  for none, or `:not-known-yet`); and the rows above it, as
  `permission-state` takes them. Gives the three words of
  `permission-state`. `:stands` only when the row holds a permission whose
  holder is the asker, and it stands by the gates' rule: a run's permission
  is its holder's, and another who that asks in a run of the same id is
  given nothing by it. What the permission allows is not read: `:see` is
  enough for a read."
  [layer asker run-row chain-rows]
  (if (= :not-known-yet run-row)
    :not-known-yet
    (let [held (rows/held-permission run-row)]
      (cond
        (or (nil? held) (not= asker (nth held 0))) :none
        (= :not-known-yet chain-rows) :not-known-yet
        (chain/stands? layer held (assoc chain-rows (chain/place layer held) run-row)) :stands
        :else :none))))

(defn either
  "Takes what `permission-state` gave for the asker's permission for the
  layer, and what `run-permission-state` gave for the run that she asks in.
  Gives whether she holds a permission that stands, of either kind:
  `:stands` when one does; else `:not-known-yet` when one is not known;
  else `:none`. An asker may see a layer that is open to its members under
  her permission for the layer, or under the permission of the run that she
  asks in (src/proposal/runtime-2026-11-21/store-piece-2/SPEC.md, R8). A
  permission for one act lets nobody see, and is not read here."
  [for-the-layer for-the-run]
  (cond
    (or (= :stands for-the-layer) (= :stands for-the-run)) :stands
    (or (= :not-known-yet for-the-layer) (= :not-known-yet for-the-run)) :not-known-yet
    :else :none))

(defn standing
  "Is a layer made, and may this asker see it. Takes the layer; its own
  rows, or nil when they are not known; whether she holds a permission that
  stands in it, as `either` gives it (`:stands`, `:none` or
  `:not-known-yet`); and whether she is signed in. Gives one of four
  words:

    the layer's own rows   its setting   her permission        it gives
    not known yet                                              :not-known-yet
    no depot row                                               :not-made
    a depot row            :everyone     any, or not read      :yes
    a depot row            :members      she is not signed in  :may-not-see
    a depot row            :members      not known yet         :not-known-yet
    a depot row            :members      one stands            :yes
    a depot row            :members      none that stands      :may-not-see

  Any permission that stands allows seeing; what it allows beyond that is
  not read. `:system` is a who like any other. The one function of who may
  see a layer (src-store/README.md, entry 1 of 11 and 12 October 2026).

  Not decided. What a layer starts with (`who-may-see`), and that
  \"members\" is whoever holds a permission in the layer, are kept only so
  that the build can run."
  [layer own-rows permission-state signed-in?]
  (cond
    (nil? own-rows) :not-known-yet
    (not (made? own-rows layer)) :not-made
    (= :everyone (who-may-see own-rows layer)) :yes
    (not signed-in?) :may-not-see
    :else (case permission-state
            :not-known-yet :not-known-yet
            :stands :yes
            :may-not-see)))

(defn in-order
  "Which layers a read looks through, nearest first. Takes a query in its
  one form; and, for a query that names `:from-layer`, the start layer's own
  rows (nil when they are not known) and the asker's standing in it. Gives
  a vector of layers, each once, at its first place; or nil while the list
  cannot be said yet.

    :layers [..]     the list as given
    :from-layer s    s, then the layers of s's builds-on fact, in the fact's
                     order; s itself never a second time. No further: the
                     lists of those layers are not read. With no fact the
                     list is s alone. While s's own rows or her standing in
                     s are not known: nil. When she may not see s, or s is
                     not made: s alone, since the list is a fact of a layer
                     that she may not see, and the read does not use it

  Not decided. How far a read walks from a layer is a working answer, for
  Sid (the one list, and no further), and so is what a read does when the
  asker may not see the start layer. This function is the one place where
  the walk would be made; a walk would also make the own rows of each
  walked layer a thing that `softland.store.read-path.sources/reading`
  wants before the list is known."
  [query start-own-rows start-standing]
  (if-let [given (:layers query)]
    (vec (distinct given))
    (let [start (:from-layer query)]
      (case start-standing
        :yes (vec (distinct (cons start (builds-on start-own-rows start))))
        (:may-not-see :not-made) [start]
        nil))))

(defn home
  "Takes a query in its one form. Gives the home of the read: the layer that
  the read starts from, which is `:from-layer`, or the first layer of
  `:layers`. It is read from the query alone, so it is the same in every
  round of a read and for as long as a read is kept live, whatever the
  tables hold: a home that is not made, or that the asker may not see, is
  the home all the same.

  The home says where a microbatch layer is read, and nothing else
  (`softland.store.read-path.sources`). Which layers a read goes through,
  and in what order, is `in-order`'s."
  [query]
  (or (:from-layer query) (first (:layers query))))

(defn left-out
  "What an answer says of a layer that gave nothing. Takes the layer and
  why, `:may-not-see` or `:not-made`
  (`softland.store.read-path.words/left-out-words`). Gives
  {:layer layer :why why}. It says nothing of the layer's facts.

  Not decided. That a read says which layer it left out, and tells a layer
  that she may not see from one that is not made, is a working answer, for
  Sid. This function is the one place where a read would say less."
  [layer why]
  {:layer layer :why why})

(defn meaning
  "What a layer's own rows and the asker's permission row mean for one
  query, as a value that `=` compares. Takes the query in its one form, the
  layer, its own rows (nil when not known), whether she holds a permission
  that stands in it (`either`), and whether she is signed in. Gives a map:
  whether the layer is made; who may see it; whether she holds a permission
  that stands;
  and, only when her standing with these rows is `:yes`, the list that the
  layer builds on when it is the query's start layer, and the stamp of the
  asked key's mark when the query asks what points at a thing. For rows
  that are not known it gives `:not-known-yet`.

  A live read compares the meaning of the rows before and after a change. A
  setting that is written again with the same word means the same, so the
  asker is given nothing. A list or a mark that is written in a layer that
  she may not see means the same too: not even an answer that says what it
  said is given, which would tell her that something of that layer was
  written. The setting itself is in the meaning whatever her standing: a
  setting that goes to \"members\" for an asker who holds a permission
  leaves her standing as it was, and a change of a fact that was passed
  over while her permission was read must still be told."
  [query layer own-rows permission-state signed-in?]
  (if (nil? own-rows)
    :not-known-yet
    (let [seen? (= :yes (standing layer own-rows permission-state signed-in?))
          [word k] (:asks query)]
      (cond-> {:made?       (made? own-rows layer)
               :who-may-see (who-may-see own-rows layer)
               :permission  permission-state}
        (and seen? (= layer (:from-layer query)))
        (assoc :builds-on (builds-on own-rows layer))

        (and seen? (= :points-at word))
        (assoc :marked-since (marked-since own-rows layer k))))))
