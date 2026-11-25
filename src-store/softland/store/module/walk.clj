;; Before changing this file, read the papers of this part again:
;; src/proposal/backend-on-rama-2026-10-02/run-again/PLAN-the-bases-gate.md
;; src/proposal/backend-on-rama-2026-10-02/run-again/PART-A.md
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-streaming-gate-and-record.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md

(ns softland.store.module.walk
  "The walk of one thing: the decision of one thing that arrived on a gate's
  depot, as one Rama operation, `decide`, for both gates. It requires Rama.

  Anything can arrive: the door is not the only sender. The two gates
  differ in the names of their tables and in their word; the walk is handed
  both, so one decision is written once.

  A decision is made of the thing, the gate's word, and rows of the tables
  on the task of the call. The walk reads no clock and no table of the
  other gate, takes no random number, and knows nothing of a batch. So the
  same thing against the same rows gives the same rows, with the same
  stamp, whenever it is decided. No function that it calls throws, so no
  thing can stop a partition: a refusal is data, an answer with a reason
  word.

  One act-id has one answer: that of the first thing that the gate decided
  under it, whether a later thing is the same act again or a different
  one.

  What one call costs. A read is counted as Rama's skill text counts a read
  on disk, one for each level of a path: a row of the answers or of the
  facts is 1, and a lane's last stamp is 2
  (`softland.store.record.tables` says why it is kept so). A write is 1
  for each row: each is a whole value at a fixed key, which reads nothing.
  Counted from the dataflow; no run here measured a read on disk. A later
  stage's rows are read only when the earlier checks passed.

    - A stray: no row read, and one add to a set. The set tracks its size,
      and Rama's docs say that size tracking adds a read on disk to each
      write.
    - A thing under an act-id that has an answer: 1 read, no write.
    - A thing that the declaration refuses: 3 reads, 2 writes.
    - An act whose permission the record does not hold, an act into a lane
      of the other gate among them: 5 reads at most, 2 writes.
    - A making act: 5 reads, 4 writes.
    - A give or an end: 5 reads, 3 writes. A give at a place that holds a
      permission for the lane or for a run reads the row of each permission
      above that one too, unless step 4 read it. A give of a yes at a place
      that holds a row reads nothing more.
    - One accepted act with one value under a plain key: 5 reads, 3 writes.
      A fact that names something to replace costs the read of its place
      more. The read of the marked keys is paid by every accepted act that
      holds a fact under a plain key, also in a lane that marks no key.
      When the fact's key is marked: 6 reads, and 4 writes, or 5 when the
      entry of a value before goes out.
    - Each count above is for an act under a first permission. An act under
      a given permission, for the lane, a run or one act, reads one row
      more for each permission above its own: 31 more at most
      (`softland.store.act`, `nesting`).
    - An act under a yes for one act costs one reading of the act's parts
      and one SHA-256 over its one form more, and its accept writes one row
      more, the yes's use. No other act pays for either.
    - Each count above leaves the lane's line out. Every decision writes
      its line row; one row more when the gate keeps the thing again, the
      thing's own bytes a second time; and one more when the thing names a
      usable who. No read. A thing that the declaration refused and whose
      text does not write out a part of it pays, beside the digest, a walk
      of that part up to the bound of a thing with no standing and one
      (`softland.store.door.weight`).

  The greatest call. An act holds 256 facts at most
  (`softland.store.door.declaration/max-facts`), and each place is read
  once. Under a first permission and with no give at a place that holds a
  permission: 261 reads at most (the 256 places, the answer, the
  permission, the marked keys and the two of the last stamp), and 774
  writes at most (three for each fact, two more, the use of a yes for one
  act, and the three of the lane's line). The chains add reads and no
  writes: 31 at most for the act's own permission, and 31 at most for each
  give at a place that holds a permission. So one call reads
  261 + 31 + 256 x 31 = 8,228 rows at most; the bound comes down with the
  depth of a permission (`softland.store.act`, `nesting`). Each of the five
  loops is over the act's own rows, or over the chains that they name, and
  ends.

  It never gives its task back. Between the first read and the last write
  stands no partitioner, no yield, no read that may yield and no append,
  and no change to this file may add one. A gate decides the things of a
  lane in the order of the lane's depot partition, each against the rows
  that the things before it wrote. Rama runs other events of a task while
  one event waits at a partitioner or a yield. A walk that waited there
  would let the next thing of the partition be decided first, against a
  record that lacks this thing's rows: an act that replaces the fact of the
  act before it would be refused, and the stamps of a lane would leave the
  depot's order. A stand-in gate whose walk did yield decided things out of
  the depot's order and refused them (PLAN-the-bases-gate.md, claim 4).
  The cost: the walk holds the task's thread for the whole of one act, and
  a read of the task waits that long.

  `decide` is a `deframafn`: Rama's form of an operation that emits exactly
  once. The writes of a decision are therefore a loop, where a gate's own
  dataflow could emit once for each write.

  When a thing is run again. Each write of a decision is behind check (a),
  and the answer is written with the rest.

    - A stream topology can process a record again, also after its writes
      were committed. A second run after a commit finds the answer at check
      (a), writes no row and gives an empty vector. So a lane's last stamp
      is not raised twice for one thing, and no entry of the facts by value
      that a later act put back is taken out again. A second run after a
      discard is the first that counts.
    - A microbatch topology sets its tables back when an attempt of a batch
      fails, on every task, and processes the batch's records again. The
      second attempt reads the rows that the first read, so it writes the
      same rows with the same stamps and gives the same copies.

  The stray's write is not behind check (a); it is an add to a set, the
  same write however often it runs.

  A limit for a gate that copies. A thing whose act-id has an answer gives
  no copies. So the copies of a decision and its answer must be committed
  together or not at all: were the answer committed and a copy lost, the
  next run would find the answer and copy nothing. A microbatch topology
  commits a whole batch on every task or on none. A stream topology
  commits each task's writes apart, so a streaming gate that copied to
  other tasks would not hold this.

  What it rests on and does not test.

    - The caller calls it on the task that holds the lane's rows, which is
      the task of the thing's depot partition, with tables that the calling
      topology owns, of the schemas of `softland.store.record.tables`. On
      another task it decides against that task's record.
    - The caller calls it for the things of a partition in the depot's
      order, and starts the next call when this one has given its result.
    - The gate's word is the depot word of the calling gate. With the other
      word every thing of the depot is a stray.
    - No function that it calls throws on what arrives. A throw in the walk
      fails the event or the attempt of the batch that called it."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.gate.decision :as decision]
            [softland.store.gate.stamp :as stamp]
            [softland.store.record.rows :as record]))

(defn copies-of
  "The writes that a gate may copy to other tasks, in the order given. The
  answer, the last stamp and the rows of the lane's line are left out: they
  stay on the task of the decision. A row that is nil says that the key is
  taken out."
  [writes]
  (into [] (filter (fn [[which]] (or (= :fact which) (= :by-value which)))) writes))

(deframafn decide
  "Emits exactly once: what `copies-of` gives for an accept, in the order in
  which the rows were written, and an empty vector for a refusal, for a
  thing under an act-id that has an answer, and for a stray. The walk
  copies nothing itself.

  It holds no partitioner, no yield, no read that may yield and no append,
  and none may be added. The namespace docstring says why, what the caller
  must hold, and what a call costs: it holds the task's thread for 8,228
  reads and 774 writes at most."
  [$$answers $$facts $$facts-by-value $$task $$line $$things *gate *thing]

  ;; 0. A stray gets no answer.
  (declaration/stray-reason *gate *thing :> *stray)
  (<<if *stray
    (digest/hex-of *thing :> *stray-digest)
    (local-transform> [(keypath :strays *stray) NONE-ELEM (termval *stray-digest)] $$task)
    (identity [] :> *copies)

   (else>)
    ;; 1. Check (a): an act-id that has an answer is not decided again.
    (get *thing :act-id :> *act-id)
    (nth *act-id 0 :> *lane)
    (nth *act-id 2 :> *act-uuid)
    (record/answer-key *lane *act-uuid :> *answer-key)
    (local-select> (keypath *answer-key) $$answers :> *answered)
    (<<if (some? *answered)
      (identity [] :> *copies)

     (else>)
      ;; 2 and 3. Check (b), the door's declaration; the digest and the
      ;; weight, in one walk; and the time on the act. No clock is read:
      ;; the time is the thing's own.
      (declaration/check *thing :> {*act :ok *declarations-reason :refuse})
      (decision/weighing-bound *declarations-reason :> *weighing-bound)
      (digest/weighed *thing *weighing-bound :> {*digest :digest *weight :weight})
      (decision/time-on *thing :> *act-time)

      ;; 4. Standing: checks (c) and (d), and the first act of a lane. No
      ;; table of the other gate is read.
      (<<if *declarations-reason
        (identity *declarations-reason :> *refused-standing)
        (identity false :> *makes-the-lane)
        (identity {} :> *standing-rows)
       (else>)
        (decision/permission-place *act :> *permission-key)
        (local-select> (keypath *permission-key) $$facts :> *permission-row)
        (<<if (nil? *permission-row)
          (decision/depot-place *act :> *depot-key)
          (local-select> (keypath *depot-key) $$facts :> *depot-row)
         (else>)
          (identity nil :> *depot-row))
        (decision/chain-needed *act *permission-row :> *chain-keys)
        (loop<- [*chain-left *chain-keys
                 *chain-read {}
                 :> *chain-rows]
          (<<if (empty? *chain-left)
            (:> *chain-read)
           (else>)
            (first *chain-left :> *chain-key)
            (local-select> (keypath *chain-key) $$facts :> *chain-row)
            (continue> (rest *chain-left) (assoc *chain-read *chain-key *chain-row))))
        (decision/hex-for-its-permission *thing *act :> *hex-of-the-act)
        (decision/standing *gate *act *permission-row *depot-row *chain-rows *hex-of-the-act
                           :> *refused-standing)
        (decision/makes-the-lane? *gate *act *permission-row *depot-row :> *makes-the-lane)
        ;; What step 4 read of the permissions, for step 5.
        (assoc *chain-rows *permission-key *permission-row :> *standing-rows))

      ;; 5. Places: checks (e), (f), (g) and what a store key takes. The
      ;; last check.
      (<<if *refused-standing
        (identity *refused-standing :> *refused)
        (identity {} :> *place-rows)
       (else>)
        (decision/places-needed *act *makes-the-lane :> *place-keys)
        (loop<- [*keys-left *place-keys
                 *rows-read {}
                 :> *first-place-rows]
          (<<if (empty? *keys-left)
            (:> *rows-read)
           (else>)
            (first *keys-left :> *place-key)
            (local-select> (keypath *place-key) $$facts :> *place-row)
            (continue> (rest *keys-left) (assoc *rows-read *place-key *place-row))))
        ;; A give at a place that holds a permission: the rows above the
        ;; one that is there, unless step 4 read them.
        (decision/chains-needed *act *makes-the-lane *first-place-rows *standing-rows :> *more-keys)
        (loop<- [*more-left *more-keys
                 *more-read {}
                 :> *more-rows]
          (<<if (empty? *more-left)
            (:> *more-read)
           (else>)
            (first *more-left :> *more-key)
            (local-select> (keypath *more-key) $$facts :> *more-row)
            (continue> (rest *more-left) (assoc *more-read *more-key *more-row))))
        (merge *standing-rows *more-rows *first-place-rows :> *place-rows)
        (decision/places *gate *act *makes-the-lane *place-rows :> *refused))

      ;; 6. The marked keys, only for an act that no check refused. Each
      ;; row is read once: step 5 may have read it.
      (<<if (and> (nil? *refused) (decision/reads-marked-keys? *act))
        (decision/marked-keys-place *act :> *marks-key)
        (<<if (contains? *place-rows *marks-key)
          (get *place-rows *marks-key :> *marks-row)
         (else>)
          (local-select> (keypath *marks-key) $$facts :> *marks-row))
        (decision/since-by-place *act *marks-row :> *since-by-place)
        (decision/rows-before-needed *since-by-place *place-rows :> *before-keys)
        (loop<- [*before-left *before-keys
                 *before-read {}
                 :> *before-rows]
          (<<if (empty? *before-left)
            (:> *before-read)
           (else>)
            (first *before-left :> *before-key)
            (local-select> (keypath *before-key) $$facts :> *before-row)
            (continue> (rest *before-left) (assoc *before-read *before-key *before-row))))
        (assoc (merge *place-rows *before-rows) *marks-key *marks-row :> *rows-in-hand)
       (else>)
        (identity {} :> *since-by-place)
        (identity *place-rows :> *rows-in-hand))

      ;; 7. The stamp, and the writes.
      (declaration/id-text *lane :> *lane-text)
      (local-select> (keypath :last-stamp *lane-text) $$task :> *last-stamp)
      (stamp/next-stamp *last-stamp :> *stamp)
      ;; A key that this act newly marks gets the act's own stamp.
      (<<if *refused
        (identity {} :> *in-hand)
       (else>)
        (hash-map :since-by-place *since-by-place
                  :place-rows *rows-in-hand
                  :level-from *stamp
                  :> *in-hand))
      (decision/writes *act-id *act *refused *stamp *act-time *digest *in-hand :> *record-writes)
      ;; The lane's line is written first.
      (decision/kept-bound *declarations-reason *refused-standing :> *kept-bound)
      (decision/line-writes *act-id *thing *refused *stamp *act-time *digest *weight *kept-bound
                            :> *line-writes)
      (into *line-writes *record-writes :> *writes)
      ;; The writes, in the order given, with no yield between two. Nothing
      ;; stands behind the last write: the walk then gives its result.
      (loop<- [*writes-left *writes
               :> *written]
        (<<if (empty? *writes-left)
          (:> true)
         (else>)
          (first *writes-left :> [*which *key *row])
          (<<cond
            (case> (= *which :fact))
            (local-transform> [(keypath *key) (termval *row)] $$facts)

            (case> (and> (= *which :by-value) (nil? *row)))
            (local-transform> [(keypath *key) NONE>] $$facts-by-value)

            (case> (= *which :by-value))
            (local-transform> [(keypath *key) (termval *row)] $$facts-by-value)

            (case> (= *which :answer))
            (local-transform> [(keypath *key) (termval *row)] $$answers)

            (case> (= *which :last-stamp))
            (local-transform> [(keypath :last-stamp *lane-text) (termval *row)] $$task)

            (case> (= *which :line))
            (local-transform> [(keypath *key) (termval *row)] $$line)

            (case> (= *which :thing))
            (local-transform> [(keypath *key) (termval *row)] $$things)

            (case> (= *which :by-who))
            (local-transform> [(keypath :by-who *key) (termval *row)] $$task)

            (default>)
            (identity nil :> *no-row))
          (continue> (rest *writes-left))))
      (copies-of *writes :> *copies)))
  (:> *copies))
