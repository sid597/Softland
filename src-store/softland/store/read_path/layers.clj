(ns softland.store.read-path.layers
  "The layers of a read: which layers a read looks through and in what
  order, whether a layer is made, whether an asker may see it, and what a
  read says of a layer that it left out. Pure code. It needs no cluster.

  Everything here is read from three kinds of row of the facts table
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
                          `:softland.store/permission`; nil says that no
                          fact was ever accepted there, and `:not-known-yet`
                          that it is not read
    the rows above her    the row at the place of each level above the
                          right that her row holds: the giver of her right,
                          the giver's giver, and so on to the first
                          permission of the layer
                          (`softland.store.gate.chain/level-places`). Their
                          places are known only once her row is read: her
                          right's id holds its whole chain. Here they are a
                          map from a place's key to its row, holding only
                          the places that are read; nil under a key says
                          that the place is read and empty

  Who may see a layer. A layer's setting says it, in one of three words
  (`softland.store.door.declaration/shapes`, `:who-may-see-word`):
  `:everyone`, a person who is not signed in too; `:signed-in`, anyone who
  is signed in; `:members`, whoever holds a right in the layer that stands.
  An asker is signed in here when her query names her (`:asker`), and not
  when it says `:not-signed-in`; who asks is what the caller says
  (`softland.store.read-path.query`).

  Her right stands as a gate reads the standing of an act
  (`softland.store.gate.chain/stands?`, the one rule of a chain for the
  gates and the read path): her row holds a permission whose holder is she
  and whose layer is this layer, and each level of its chain stands. A
  retract at her place holds no permission, so a right that was ended lets
  her see nothing; and a right whose level above was ended does not stand,
  also after that level is given again.

  Inputs and results, by call:

    own-row          own rows, a layer, a store key -> the row, or nil
    made?            own rows, a layer -> true when the layer is made
    who-may-see      own rows, a layer -> `:members`, `:signed-in` or
                     `:everyone`
    builds-on        own rows, a layer -> the layers that it builds on
    marked-since     own rows, a layer, a key -> the stamp of the key's mark
    held-permission  a layer, the asker, her permission row -> the right
                     that her row holds, or nil
    above-places     a layer, the asker, her permission row -> the keys of
                     the places of the levels above her right, or nil
    right            a layer, the asker, her permission row, the rows above
                     her -> `:stands`, `:none` or `:not-known-yet`
    levels-above     what `right` takes -> for each level above her right,
                     whether it stands beneath her, or nil
    standing         a layer, its own rows, her right, whether she is
                     signed in -> `:yes`, `:may-not-see`, `:not-made` or
                     `:not-known-yet`
    in-order         a query, the start layer's own rows and standing -> the
                     layers of the read, nearest first, or nil
    home             a query -> the layer that the read starts from
    left-out         a layer and why -> what an answer says of it
    meaning          a query, a layer, its own rows, her right, the marks
                     of the levels above it, whether she is signed in ->
                     what the rows mean for this query, as a value that `=`
                     compares

  \"Standing\" here is whether an asker may see a layer, as the read path
  knows it now. `softland.store.gate.decision` uses the same word for a
  permission that stands at a gate; the two are different questions, and
  whether her right stands is the one rule of both.

  State and effects. None. Each function is pure, and none throws on a row.

  Callers. `softland.store.read-path.sources` and
  `softland.store.read-path.answer`.

  What the read path does not read. What a permission allows: any right
  that stands allows seeing, whatever its fifth slot says
  (src-store/README.md, decisions of 11 and 12 October 2026, entry 1). So
  the things about a permission that are not decided (one permission for
  each holder; who may write a setting, a list or a mark) are no code
  here."
  (:require [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
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
  `:everyone`, `:signed-in` or `:members`. With no row it is what the layer
  starts with (`softland.store.gate.first-act/starts-with`). A row whose
  value is none of the three words reads as `:members`, the answer that
  shows least.

  Not decided. What a layer starts with is kept only so that the build can
  run; `starts-with` is the one place where the other answer would be
  made, and this function is its one caller in the read path."
  [own-rows layer]
  (let [row (own-row own-rows layer :softland.store/who-may-see)
        [kind v] (rows/read-value row)]
    (cond
      (nil? row) (first-act/starts-with layer :softland.store/who-may-see)
      (and (= :value kind) (contains? #{:everyone :signed-in} v)) v
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

(defn held-permission
  "Takes a layer, the asker's id, and her permission row in the layer (a
  row, nil, or `:not-known-yet`). Gives the right that her row holds, as a
  permission id in its one form, when it is hers in this layer: the row
  holds a value (`softland.store.record.rows/read-value`) that has the
  shape `:permission-id` (`softland.store.act/has-shape?`), whose holder is
  the asker and whose layer slot is the layer, and the holder of each of
  its levels is an id of which a key can be made
  (`softland.store.door.declaration/usable-id?`). Else nil: no row, a
  retract, a row not read yet, or a value that is no right of hers here. A
  gate writes a permission fact only on its holder, so a row that a gate
  wrote at her place and that holds a value is hers; the test is for a row
  that came by another hand. What it allows is not read."
  [layer asker permission-row]
  (when (map? permission-row)
    (let [[kind v] (rows/read-value permission-row)]
      (when (and (= :value kind)
                 (act/has-shape? :permission-id v)
                 (= asker (nth v 0))
                 (= layer (nth v 1))
                 (every? #(declaration/usable-id? (nth % 0)) (chain/levels v)))
        v))))

(defn above-places
  "Takes a layer, the asker's id and her permission row. Gives the keys of
  the places of the levels above the right that her row holds, nearest
  first, as a vector (`softland.store.gate.chain/level-places`, without
  her own place): empty for a first permission, which has nothing above
  it; nil when her row holds no right of hers (`held-permission`). Every
  place is in the layer given. Bounded by the depth of a permission id."
  [layer asker permission-row]
  (when-let [held (held-permission layer asker permission-row)]
    (subvec (chain/level-places layer held) 1)))

(defn right
  "Whether the asker's right in a layer stands. Takes the layer, her id,
  her permission row (a row, nil, or `:not-known-yet`), and the rows above
  her: a map from the key of each place of `above-places` that is read to
  its row, nil for an empty place. Gives one of three words:

    :not-known-yet  her row is not read yet, or a place above her right is
                    not in the map
    :none           her row holds no right of hers (`held-permission`):
                    no row, a retract, or another value; or her right's
                    chain does not stand
    :stands         her row holds a right of hers, and each level of its
                    chain stands (`softland.store.gate.chain/stands?`)

  So a right that was ended at her place, or above her, gives `:none`, and
  so does a right beneath a level that was ended and then given again: the
  rule of the chain reads the stamps of the rows, and the new row above is
  later than hers. It reads no time. It never throws on a row: a row that
  holds a value and no stamp, which no gate writes, makes the chain not
  stand."
  [layer asker permission-row above-rows]
  (if (= :not-known-yet permission-row)
    :not-known-yet
    (if-let [held (held-permission layer asker permission-row)]
      (let [places (chain/level-places layer held)]
        (if (every? #(contains? above-rows %) (rest places))
          (if (try (chain/stands? layer held (assoc above-rows (first places) permission-row))
                   (catch Throwable _ false))
            :stands
            :none)
          :not-known-yet))
      :none)))

(defn levels-above
  "For each level above the asker's right, nearest first, whether it stands
  beneath her: true when the level's row holds exactly that level and its
  stamp is lower than the stamp of her own row; false when not; and
  `:not-known-yet` for a place that the rows above her do not hold. Takes
  what `right` takes. Gives a vector, empty for a first permission; nil
  when her row holds no right of hers, or is not read.

  Each mark is read from one row above and her own row, and from no other
  row. That is what a live read's meaning needs (`meaning`). A live read
  keeps no value between two changes: at a change of one push it compares
  what the rows mean with that push's value before and after, beside what
  the other pushes hold now. Two levels above that end at once can both
  hold their new values before either push is handled; her right is then
  `:none` before and after in each step, and the word alone would tell her
  nothing. Each mark changes in its own push's step. Every row that is
  written at a level's place after her own row has a later stamp, the
  layer's count of decisions (`softland.store.gate.stamp`): so a mark is
  true while her right stands, and the first change of that level's row,
  an end or a give again, turns it false for good, for as long as her row
  holds the same right. It never throws on a row."
  [layer asker permission-row above-rows]
  (when-let [held (held-permission layer asker permission-row)]
    (let [her-stamp (:stamp permission-row)]
      (mapv (fn [level place]
              (if (contains? above-rows place)
                (let [row (get above-rows place)
                      stamp (:stamp row)]
                  (boolean (and (rows/holds-value? row level)
                                (integer? stamp)
                                (integer? her-stamp)
                                (< (long stamp) (long her-stamp)))))
                :not-known-yet))
            (rest (chain/levels held))
            (rest (chain/level-places layer held))))))

(defn standing
  "Is a layer made, and may this asker see it. Takes the layer; its own
  rows, or nil when they are not known; her right in the layer, as `right`
  gives it; and whether she is signed in. Gives one of four words:

    the layer's own rows   its setting   she, and her right     it gives
    not known yet                                               :not-known-yet
    no depot row                                                :not-made
    a depot row            :everyone     anyone                 :yes
    a depot row            :signed-in    she is not signed in   :may-not-see
    a depot row            :signed-in    she is signed in       :yes
    a depot row            :members      she is not signed in   :may-not-see
    a depot row            :members      not known yet          :not-known-yet
    a depot row            :members      :stands                :yes
    a depot row            :members      :none                  :may-not-see

  Any right that stands allows seeing; what it allows beyond that is not
  read. Under `:everyone` and `:signed-in` her right is not read. `:system`
  is a who like any other. The one function of who may see a layer
  (src-store/README.md, entry 1 of 11 and 12 October 2026, and the
  decisions of sign-in of 15 October 2026).

  Not decided. What a layer starts with (`who-may-see`), and that
  \"members\" is whoever holds a right in the layer that stands, are kept
  only so that the build can run."
  [layer own-rows her-right signed-in?]
  (let [setting (when (some? own-rows) (who-may-see own-rows layer))]
    (cond
      (nil? own-rows) :not-known-yet
      (not (made? own-rows layer)) :not-made
      (= :everyone setting) :yes
      (not signed-in?) :may-not-see
      (= :signed-in setting) :yes
      (= :not-known-yet her-right) :not-known-yet
      (= :stands her-right) :yes
      :else :may-not-see)))

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
  "What a layer's own rows and the asker's right mean for one query, as a
  value that `=` compares. Takes the query in its one form, the layer, its
  own rows (nil when not known), her right (`right`: `:stands`, `:none` or
  `:not-known-yet`), the marks of the levels above her right
  (`levels-above`), and whether she is signed in. Gives a map: whether the
  layer is made; who may see it; her right; the marks of the levels above
  it; and, only when her standing with these rows is `:yes`, the list that
  the layer builds on when it is the query's start layer, and the stamp of
  the asked key's mark when the query asks what points at a thing. For
  rows that are not known it gives `:not-known-yet`.

  A live read compares the meaning of the rows before and after a change. A
  setting that is written again with the same word means the same, so the
  asker is given nothing. A list or a mark that is written in a layer that
  she may not see means the same too: not even an answer that says what it
  said is given, which would tell her that something of that layer was
  written. The setting itself is in the meaning whatever her standing: a
  setting that goes to \"members\" for an asker whose right stands leaves
  her standing as it was, and a change of a fact that was passed over
  while her right was read must still be told.

  Her right is in the meaning as its word and as one mark for each level
  above it, each mark read from one row (`levels-above` says why the word
  alone is not enough). So a level above that is given again, after an end
  that already took her right, means the same; and the first change of a
  level above whose mark was still true means another thing, also when her
  right had ended already by another level: she is then given an answer
  anew that says what it said. That is at most once for each level above
  her right, while her row holds the same right, and it says that a row of
  her own right's chain was written."
  [query layer own-rows her-right levels signed-in?]
  (if (nil? own-rows)
    :not-known-yet
    (let [seen? (= :yes (standing layer own-rows her-right signed-in?))
          [word k] (:asks query)]
      (cond-> {:made?       (made? own-rows layer)
               :who-may-see (who-may-see own-rows layer)
               :right       her-right
               :levels      levels}
        (and seen? (= layer (:from-layer query)))
        (assoc :builds-on (builds-on own-rows layer))

        (and seen? (= :points-at word))
        (assoc :marked-since (marked-since own-rows layer k))))))
