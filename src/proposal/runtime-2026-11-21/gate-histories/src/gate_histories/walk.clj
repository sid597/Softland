(ns gate-histories.walk
  "The walk of one thing against a record in memory. Pure code: it needs no
  cluster and no Rama.

  It calls the store's own functions, the ones that a gate calls, and writes
  the rows that they give into a map. Nothing of the decision is written
  again here: the test for a stray and the declaration's check are
  `softland.store.door.declaration`'s, each check against the record and the
  rows of a decision are `softland.store.gate.decision`'s, and the stamp is
  `softland.store.gate.stamp`'s.

  The order is the order that `softland.store.module.walk` names in its own
  description, steps 0 to 7. This file was written from that description and
  from the docstrings of the pure functions, not from the Rama code. So what
  it proves is this: given the rows that those docstrings say must be in
  hand, the gate's rules decide as a history's statements require. It does
  not prove that the module hands a gate those rows. That is the module's
  proof, on a cluster.

  A record here is a map:

    :answers     the answer under each act-id, by `answer-key`
    :facts       the row of each place, by `place-key`
    :by-value    the rows of the facts by value
    :last-stamp  the last stamp of each layer, by layer id
    :strays      the reason word of each stray, in order

  `decide` takes a record, the depot word of the gate, and one thing as it
  arrived. It gives the record after, and what happened under `:outcome`."
  (:require [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.gate.decision :as decision]
            [softland.store.gate.stamp :as stamp]
            [softland.store.record.rows :as record]))

(def empty-record
  "A record that holds nothing: no layer is made."
  {:answers {} :facts {} :by-value {} :last-stamp {} :strays []})

(defn- rows-at
  "Takes the record's facts and keys. Gives a map from each key to its row.
  A key whose place is empty is in the map with nil, so that a caller can
  tell a place that was read and is empty from one that was not read."
  [facts ks]
  (into {} (map (fn [k] [k (get facts k)])) ks))

(defn- checks
  "Steps 4 and 5 for an act that the declaration took. Takes the record's
  facts, the gate's depot and the act. Gives {:word :makes?}: the word of
  the first check that fails, or nil, and whether the act makes its layer."
  [facts gate-depot act]
  (let [permission-key (decision/permission-place act)
        permission-row (get facts permission-key)
        depot-key (decision/depot-place act)
        depot-row (when (nil? permission-row) (get facts depot-key))
        chain-rows (rows-at facts (decision/chain-needed act permission-row))
        standing-word (decision/standing gate-depot act permission-row depot-row chain-rows)
        makes? (decision/makes-the-layer? gate-depot act permission-row depot-row)]
    (if standing-word
      {:word standing-word :makes? false}
      (let [in-hand (cond-> (assoc chain-rows permission-key permission-row)
                      (nil? permission-row) (assoc depot-key depot-row))
            place-rows (rows-at facts (decision/places-needed act makes?))
            more-rows (rows-at facts (decision/chains-needed act makes? place-rows in-hand))
            rows (merge in-hand place-rows more-rows)]
        {:word (decision/places gate-depot act makes? rows) :makes? makes?}))))

(defn- accept-in-hand
  "Step 6 for an act that no check refused: what `decision/writes` needs of
  the layer's marked keys. Takes the record's facts, the act and its stamp."
  [facts act stamp]
  (let [marks-key (decision/marked-keys-place act)
        marks-row (when (decision/reads-marked-keys? act) (get facts marks-key))
        since (decision/since-by-place act marks-row)
        place-rows (merge {marks-key marks-row}
                          (rows-at facts (decision/rows-before-needed since {})))]
    {:since-by-place since :place-rows place-rows :level-from stamp}))

(defn- write-rows
  "Takes a record, a layer and the rows of one decision. Gives the record
  with each row written, as the walk writes them."
  [rec layer writes]
  (reduce (fn [rec [which k row]]
            (case which
              :fact (assoc-in rec [:facts k] row)
              :by-value (if row
                          (assoc-in rec [:by-value k] row)
                          (update rec :by-value dissoc k))
              :answer (assoc-in rec [:answers k] row)
              :last-stamp (assoc-in rec [:last-stamp layer] row)))
          rec
          writes))

(defn decide
  "Decides one thing. Takes a record, the gate's depot word (`:streaming` or
  `:microbatch`) and a thing as it arrived on that gate's depot. Gives
  {:record :outcome}. The outcome holds one of:

    {:stray word}                      the thing got no answer
    {:repeat true}                     its act-id has an answer; nothing is
                                       decided and nothing is written
    {:verdict :accepted :stamp n ...}  the act was taken
    {:verdict word :stamp n ...}       the act was refused, with its word

  and, for a decision, `:act`, the act in its one form (nil when the
  declaration refused the thing), `:makes?`, and `:writes`, the rows that
  the decision wrote."
  [rec gate-depot thing]
  (if-let [stray (declaration/stray-reason gate-depot thing)]
    {:record (update rec :strays conj stray) :outcome {:stray stray}}
    (let [[layer _ act-uuid :as act-id] (:act-id thing)]
      (if (get (:answers rec) (record/answer-key layer act-uuid))
        {:record rec :outcome {:repeat true}}
        (let [thing-digest (digest/digest thing)
              checked (declaration/check thing)
              act (:ok checked)
              time-millis (decision/time-on thing)
              {:keys [word makes?]} (if act
                                      (checks (:facts rec) gate-depot act)
                                      {:word (:refuse checked) :makes? false})
              stamp (stamp/next-stamp (get (:last-stamp rec) layer))
              in-hand (when-not word (accept-in-hand (:facts rec) act stamp))
              writes (decision/writes act-id act word stamp time-millis thing-digest in-hand)]
          {:record (write-rows rec layer writes)
           :outcome {:verdict (or word :accepted)
                     :stamp stamp
                     :act act
                     :makes? makes?
                     :writes (vec writes)}})))))
