(ns gate-histories.walk
  "The way of one thing through the store, in memory: the door's additions,
  the walk of the gate against a record, and the answer that the door gives
  back. Pure code: it needs no cluster and no Rama.

  It calls the store's own functions, the ones that the door and a gate
  call, and writes the rows that they give into a map. Nothing of the
  decision is written again here: the door's reading and its three
  additions are `softland.store.door.additions`'s; the test for a stray and
  the declaration's check are `softland.store.door.declaration`'s; each
  check against the record and the rows of a decision are
  `softland.store.gate.decision`'s; the stamp is `softland.store.gate.stamp`'s;
  and the answer that a sender is told is `softland.store.door.words/answer`
  of the answer's row, held against the digest that the door gave her.

  The order is the order that `softland.store.module.walk` names in its own
  description, steps 0 to 7, check (a) and the answers among them. This file
  was written from that description and from the docstrings of the pure
  functions, and checked against the Rama code of the walk: it reads the
  rows that the module reads, in the same order, and hands each pure
  function what the module hands it. So what it proves is this: given the
  rows that those docstrings say must be in hand, the gate's rules decide as
  a history's statements require. That the module hands a gate those rows
  is the module's proof, on a cluster.

  A record here is a map:

    :answers     the answer under each act-id, by `answer-key`
    :facts       the row of each place, by `place-key`
    :by-value    the rows of the facts by value
    :last-stamp  the last stamp of each lane, by lane id
    :strays      the reason word of each stray, in order
    :aside       each row of a kind that this copy does not know, as
                 [which key row], in the order written

  Two things keep the copy from breaking when the store changes under it.
  A decision may write a row of a kind that the copy does not know, as a
  later piece of the store will: the row is kept aside, under `:aside`, and
  nothing here reads it back; the run counts such rows by kind. And `writes`
  is handed every row that the walk read for the act, as the module's walk
  hands them (`softland.store.module.walk`, steps 4 to 7): the row at the
  place of the act's permission among them, so that a rule of `writes` can
  use that row without a new argument.

  The copy calls these functions of the store, with these arguments, and
  breaks when one of them changes: `decision/hex-for-its-permission` with the
  thing and the act; `decision/writes` with seven arguments; and
  `decision/permission-place`, `depot-place`, `chain-needed`, `standing`
  (six), `makes-the-lane?`, `places-needed`, `chains-needed`, `places`,
  `marked-keys-place`, `reads-marked-keys?`, `since-by-place`,
  `rows-before-needed` and `time-on`; `declaration/stray-reason` and
  `check`; `digest/digest` and `readable-hex-of`; `stamp/next-stamp`;
  `record/answer-key` and `answer-of`; `words/answer`; and the door's
  `additions/reading` and `thing-to-append`.

  `through-the-door` makes the thing that the door appends for a maker's
  act. `decide` takes a record, the depot word of the gate, and one thing as
  it arrived. It gives the record after, and what happened under
  `:outcome`. `told` reads what the door tells the thing's sender."
  (:require [softland.store.door.additions :as additions]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.door.words :as words]
            [softland.store.gate.decision :as decision]
            [softland.store.gate.stamp :as stamp]
            [softland.store.record.rows :as record]))

(def empty-record
  "A record that holds nothing: no lane is made."
  {:answers {} :facts {} :by-value {} :last-stamp {} :strays [] :aside []})

(defn through-the-door
  "The thing that the door appends for a maker's act, as `softland.store.door/send-act`
  makes it: the door's reading of the act and its three additions, the
  act-id, the depot that it names and the time
  (`softland.store.door.additions`). Takes the maker's act, the act-id that
  the maker holds, and the time. Gives the thing. Throws when the door would
  append nothing and give a word instead: a history sends only what the
  door appends."
  [maker-act act-id door-time]
  (let [reading (additions/reading maker-act act-id)
        built (when-not (:could-not reading) (additions/thing-to-append reading act-id door-time))]
    (or (:thing built)
        (throw (ex-info "the door appends nothing for this act"
                        {:act maker-act :word (if (:could-not reading) reading built)})))))

(defn- rows-at
  "Takes the record's facts and keys. Gives a map from each key to its row.
  A key whose place is empty is in the map with nil, so that a caller can
  tell a place that was read and is empty from one that was not read."
  [facts ks]
  (into {} (map (fn [k] [k (get facts k)])) ks))

(defn- checks
  "Steps 4 and 5 for an act that the declaration took. Takes the record's
  facts, the gate's depot, the thing as it arrived and the act. Gives
  {:word :makes? :rows}: the word of the first check that fails, or nil;
  whether the act makes its lane; and, when no check of the standing
  failed, every row that the two steps read, as a map from key to row: the
  row at the place of the act's permission and the rows of its chain, and
  the rows of the places of its facts and of the chains that those need.
  That is the map that the module's walk carries on (`*place-rows` there),
  and the depot's row is not in it."
  [facts gate-depot thing act]
  (let [permission-key (decision/permission-place act)
        permission-row (get facts permission-key)
        depot-key (decision/depot-place act)
        depot-row (when (nil? permission-row) (get facts depot-key))
        chain-rows (rows-at facts (decision/chain-needed act permission-row))
        standing-word (decision/standing gate-depot act permission-row depot-row chain-rows
                                         (decision/hex-for-its-permission thing act))
        makes? (decision/makes-the-lane? gate-depot act permission-row depot-row)]
    (if standing-word
      {:word standing-word :makes? false :rows {}}
      (let [standing-rows (assoc chain-rows permission-key permission-row)
            place-rows (rows-at facts (decision/places-needed act makes?))
            more-rows (rows-at facts (decision/chains-needed act makes? place-rows standing-rows))
            rows (merge standing-rows more-rows place-rows)]
        {:word (decision/places gate-depot act makes? rows) :makes? makes? :rows rows}))))

(defn- accept-in-hand
  "Steps 6 and 7 for an act that no check refused: what `decision/writes`
  has in hand, as the module's walk gives it. Takes the record's facts, the
  act, the rows that `checks` read, and the act's stamp. The rows handed on
  under `:place-rows` are all of those, and for an act that reads the
  lane's marked keys the row at their place and the row before of each
  place under a marked key, each read once."
  [facts act rows stamp]
  (if (decision/reads-marked-keys? act)
    (let [marks-key (decision/marked-keys-place act)
          marks-row (if (contains? rows marks-key) (get rows marks-key) (get facts marks-key))
          since (decision/since-by-place act marks-row)
          before-rows (rows-at facts (decision/rows-before-needed since rows))]
      {:since-by-place since
       :place-rows (assoc (merge rows before-rows) marks-key marks-row)
       :level-from stamp})
    {:since-by-place {} :place-rows rows :level-from stamp}))

(defn- write-rows
  "Takes a record, a lane and the rows of one decision. Gives the record
  with each row written, as the walk writes them: a fact, an entry of the
  facts by value, the answer and the lane's last stamp. A row of any other
  kind is kept aside, under `:aside`, in the order written; nothing here
  reads it back, so a rule of the store that rests on such a row is not
  held by this copy until the copy writes that kind."
  [rec lane writes]
  (reduce (fn [rec [which k row :as write]]
            (case which
              :fact (assoc-in rec [:facts k] row)
              :by-value (if row
                          (assoc-in rec [:by-value k] row)
                          (update rec :by-value dissoc k))
              :answer (assoc-in rec [:answers k] row)
              :last-stamp (assoc-in rec [:last-stamp lane] row)
              (update rec :aside (fnil conj []) write)))
          rec
          writes))

(defn decide
  "Decides one thing. Takes a record, the gate's depot word (`:streaming` or
  `:microbatch`) and a thing as it arrived on that gate's depot. Gives
  {:record :outcome}. The outcome holds one of:

    {:stray word}                      the thing got no answer
    {:repeat true}                     its act-id has an answer; nothing is
                                       decided and nothing is written
                                       (check (a)), whether the thing is the
                                       one that the answer is of or another
    {:verdict :accepted :stamp n ...}  the act was taken
    {:verdict word :stamp n ...}       the act was refused, with its word

  and, for a decision, `:act`, the act in its one form (nil when the
  declaration refused the thing), `:makes?`, and `:writes`, the rows that
  the decision wrote."
  [rec gate-depot thing]
  (if-let [stray (declaration/stray-reason gate-depot thing)]
    {:record (update rec :strays conj stray) :outcome {:stray stray}}
    (let [[lane _ act-uuid :as act-id] (:act-id thing)]
      (if (get (:answers rec) (record/answer-key lane act-uuid))
        {:record rec :outcome {:repeat true}}
        (let [thing-digest (digest/digest thing)
              checked (declaration/check thing)
              act (:ok checked)
              time-millis (decision/time-on thing)
              {:keys [word makes? rows]} (if act
                                           (checks (:facts rec) gate-depot thing act)
                                           {:word (:refuse checked) :makes? false :rows {}})
              stamp (stamp/next-stamp (get (:last-stamp rec) lane))
              in-hand (if word {} (accept-in-hand (:facts rec) act rows stamp))
              writes (decision/writes act-id act word stamp time-millis thing-digest in-hand)]
          {:record (write-rows rec lane writes)
           :outcome {:verdict (or word :accepted)
                     :stamp stamp
                     :act act
                     :makes? makes?
                     :writes (vec writes)}})))))

(defn told
  "What the door tells the sender of a thing when she reads her answer
  (`softland.store.door/answer-of`): the answer under the thing's act-id,
  held against the digest that the door gave her when it sent the thing
  (`softland.store.door.digest/readable-hex-of`). Takes a record and the
  thing as it arrived. Gives one of the door's four answers:
  `{:answer :accepted ...}`, `{:answer :refused :reason word ...}`,
  `{:answer :nothing-yet}`, or `{:answer :not-mine}` when the act-id's
  answer is of another thing."
  [rec thing]
  (let [[lane _ act-uuid] (:act-id thing)]
    (words/answer (record/answer-of (get (:answers rec) (record/answer-key lane act-uuid)))
                  (digest/readable-hex-of thing))))
