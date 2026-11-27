(ns softland.store.gate.walk
  "The walk of one thing: the decision of one thing that arrived on a gate's
  depot, as a pure function of the thing and of the rows that it asks for.
  It reads nothing and writes nothing itself.

  Anything can arrive: the door is not the only sender. A decision is made
  of the thing, the gate's word, and rows of the gate's record. The walk
  does not know beforehand which rows: a permission names the one that it
  is lent from, and a row must be read to learn it. So it is a walk in
  steps. `begin` takes the gate's word and the thing; `reads` says what the
  walk wants read now; `resume` takes what those reads gave. When `reads`
  is empty the walk is done, and `outcome` says what happened and which
  rows to write. Whoever drives it, a gate against its tables or a proof
  against a map (`softland.store.record.in-memory`), does nothing but read
  what is asked and write what is given, so the two cannot decide apart.

  A read is `[which key]`:

    [:answer key]         the row of the answers under that key
    [:fact key]           the row of the facts at that place
    [:owners lane-text]   how many owner permissions of the lane stand on
                          their own (`softland.store.gate.decision/owners-key`)
    [:last-stamp lane-text]

  `resume` takes a map from each read to what it gave, nil for nothing
  there.

  The order. A stray gets no answer. An act-id that has an answer is not
  decided again: one act-id has one answer, that of the first thing that
  the gate decided under it, whether a later thing is the same act again
  or a different one. Then the door's declaration, the digest and the
  weight in one walk of the thing, and the time on it; the standing; the
  places; the lane's marked keys, only for an act that no check refused;
  the stamp; the writes. A later stage's rows are read only when the
  earlier checks passed.

  No function that it calls throws on what arrives, so no thing can stop a
  partition: a refusal is data, an answer with a reason word. It reads no
  clock and takes no random number. So the same thing against the same
  rows gives the same rows, with the same stamp, whenever it is decided.

  What a driver must hold, and this code cannot: that it reads on the task
  that holds the lane's rows; that it decides the things of a partition in
  the depot's order, each against the rows that the things before it
  wrote; and that between the first read and the last write of one thing
  it gives its task to no other event."
  (:require [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.gate.decision :as decision]
            [softland.store.gate.stamp :as stamp]
            [softland.store.record.rows :as record]))

(defn- read-of
  "The read of a key that a check of `softland.store.gate.decision` wants."
  [lane-text k]
  (if (= decision/owners-key k)
    [:owners lane-text]
    [:fact k]))

(defn- key-of
  "The key under which the checks hold what a read gave."
  [[which k]]
  (if (= :owners which) decision/owners-key k))

(defn- done
  [state outcome]
  (assoc state :reads [] :outcome outcome))

(defn- asking
  "The state that waits for the reads of some keys of the checks."
  [state ks]
  (assoc state :reads (mapv #(read-of (:lane-text state) %) ks)))

(declare step)

(defn- to-stage
  [state stage]
  (step (assoc state :stage stage)))

(defn- finish
  "The stamp and the writes: the last stage. The lane's line is written
  first."
  [{:keys [gate thing act-id act lane rows refused refused-standing declarations-reason
           digest weight time last-stamp since-by-place makes?] :as state}]
  (let [stamp (stamp/next-stamp last-stamp)
        in-hand (if refused
                  {}
                  {:since-by-place (or since-by-place {})
                   :place-rows rows
                   :level-from stamp})
        record-writes (decision/writes act-id act refused stamp time digest in-hand)
        line-writes (decision/line-writes act-id thing refused stamp time digest weight
                                          (decision/kept-bound declarations-reason refused-standing refused
                                                               (boolean (and act (decision/held-through-everyone? act rows)))))]
    (done state {:verdict (or refused :accepted)
                 :stamp stamp
                 :act act
                 :makes? (boolean (and makes? (nil? refused)))
                 :writes (into (vec line-writes) record-writes)})))

(defn- step
  "Goes on from the state's stage until the walk wants a read or is done."
  [{:keys [gate thing act rows] :as state}]
  (case (:stage state)
    :declaration
    (let [{act :ok declarations-reason :refuse} (declaration/check thing)
          {:keys [digest weight]} (digest/weighed thing (decision/weighing-bound declarations-reason))
          state (assoc state
                       :act act
                       :declarations-reason declarations-reason
                       :digest digest
                       :weight weight
                       :time (decision/time-on thing))]
      (if declarations-reason
        (to-stage (assoc state :refused declarations-reason :refused-standing declarations-reason) :stamp)
        (to-stage state :standing)))

    :standing
    (let [wanted (decision/standing-wanted act rows)]
      (if (seq wanted)
        (asking state wanted)
        (let [word (decision/standing gate act rows (decision/hex-for-its-permission thing act rows))]
          (if word
            (to-stage (assoc state :refused word :refused-standing word) :stamp)
            (to-stage (assoc state :makes? (decision/makes-the-lane? gate act rows)) :places)))))

    :places
    (let [wanted (decision/places-wanted gate act rows)]
      (if (seq wanted)
        (asking state wanted)
        (let [word (decision/places gate act rows)]
          (cond
            word (to-stage (assoc state :refused word) :stamp)
            (decision/reads-marked-keys? act) (to-stage state :marks)
            :else (to-stage state :stamp)))))

    ;; The lane's marked keys, and the row before of each place under one:
    ;; each row is read once, since an earlier stage may have read it.
    :marks
    (let [marks-key (decision/marked-keys-place act)]
      (if-not (contains? rows marks-key)
        (asking state [marks-key])
        (let [since (decision/since-by-place act (get rows marks-key))
              before (decision/rows-before-needed since rows)]
          (if (seq before)
            (asking state before)
            (to-stage (assoc state :since-by-place since) :stamp)))))

    :stamp
    (if (contains? state :last-stamp)
      (finish state)
      (assoc state :reads [[:last-stamp (:lane-text state)]]))))

(defn begin
  "Takes the gate's depot word, `:streaming` or `:microbatch`, and a thing
  as it arrived on that gate's depot. Gives the walk's state. With the
  other gate's word every thing of the depot is a stray."
  [gate-depot thing]
  (if-let [stray (declaration/stray-reason gate-depot thing)]
    (done {:gate gate-depot :thing thing}
          {:stray stray :writes [[:stray stray (digest/hex-of thing)]]})
    (let [[lane _ act-uuid :as act-id] (:act-id thing)]
      {:gate gate-depot
       :thing thing
       :act-id act-id
       :lane lane
       :lane-text (declaration/id-text lane)
       :rows {}
       :stage :answer
       :reads [[:answer (record/answer-key lane act-uuid)]]})))

(defn reads
  "What the walk wants read now, each as `[which key]`. Empty when the walk
  is done."
  [state]
  (:reads state))

(defn resume
  "Takes the state and a map from each read that `reads` named to what it
  gave. Gives the state after: one that wants more reads, or one that is
  done."
  [state got]
  (case (:stage state)
    :answer
    (if (some? (get got (first (:reads state))))
      (done state {:repeat true :writes []})
      (to-stage state :declaration))

    :stamp
    (step (assoc state :last-stamp (get got (first (:reads state)))))

    (step (update state :rows into
                  (map (fn [[_ k :as read]]
                         [(key-of read)
                          (cond-> (get got read)
                            (record/permission-place? k) record/read-as-a-permission)]))
                  (:reads state)))))

(defn outcome
  "What happened, for a walk that is done; nil for one that is not. One of:

    {:stray word :writes [[:stray word hex]]}
        the thing got no answer; the one write keeps its digest under its
        word, and is the same write however often it runs
    {:repeat true :writes []}
        its act-id has an answer; nothing is decided and nothing is
        written, whether the thing is the one that the answer is of or
        another
    {:verdict :accepted :stamp n :act act :makes? b :writes [...]}
    {:verdict word :stamp n :act act-or-nil :makes? false :writes [...]}
        the act was taken, or refused with its word; `:act` is nil when
        the declaration refused the thing

  `:writes` is each row to write, as `[which key row]`, in the order given
  (`softland.store.gate.decision`, `writes` and `line-writes`): nothing
  stands behind the last write."
  [state]
  (:outcome state))
