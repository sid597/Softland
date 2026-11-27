(ns softland.store.record.in-memory
  "The record as one map, in the memory of one process: what each table
  holds, how each read of a walk is answered from it, and how each row of a
  decision changes it. Pure code. No gate uses it: a gate's record is its
  PStates (`softland.store.record.tables`), and a gate answers the same
  reads from them and writes the same rows to them. A proof and a judge
  decide against this map, with no cluster.

  It is one lane's task: a thing is decided against whatever the map holds.

    :answers     the answer under each act-id's key
    :facts       the row of each place
    :by-value    the rows of the facts by value
    :by-holder   the permissions by holder, key to stamp, in the order of
                 the keys as Java compares Strings; that is the table's
                 own order for every key with no letter from U+D800 on
                 (`softland.store.record.rows/before?`)
    :owners      for each lane's text, the ids of its owner permissions
                 that stand on their own
    :last-stamp  the last stamp of each lane, by the lane's text
    :line :things :by-who   the lane's line, as a gate keeps it
    :strays      the digests of the strays, by word"
  (:require [softland.store.door.declaration :as declaration]
            [softland.store.gate.walk :as walk]
            [softland.store.read-path.lanes :as lanes]))

(def empty-record
  {:answers {} :facts {} :by-value {} :by-holder (sorted-map) :owners {}
   :last-stamp {} :line {} :things {} :by-who {} :strays {}})

(defn read-one
  "What one read of a walk, or of an asker's rights, gives from the record."
  [record [which k to]]
  (case which
    :answer (get (:answers record) k)
    :fact (get (:facts record) k)
    :owners (count (get (:owners record) k))
    :last-stamp (get (:last-stamp record) k)
    :holders (into {} (subseq (:by-holder record) >= k < to))))

(defn write-one
  "The record after one row of a decision. `lane-text` is the text of the
  lane of the thing that was decided: a last stamp and an owner are
  written for it."
  [record lane-text [which k row]]
  (let [put-or-take (fn [table] (if (some? row)
                                  (assoc-in record [table k] row)
                                  (update record table dissoc k)))]
    (case which
      :fact (assoc-in record [:facts k] row)
      :by-value (put-or-take :by-value)
      :holder-permission (put-or-take :by-holder)
      :owner (update-in record [:owners lane-text] (fnil (if row conj disj) #{}) k)
      :answer (assoc-in record [:answers k] row)
      :last-stamp (assoc-in record [:last-stamp lane-text] row)
      :line (assoc-in record [:line k] row)
      :thing (assoc-in record [:things k] row)
      :by-who (assoc-in record [:by-who k] row)
      :stray (update-in record [:strays k] (fnil conj #{}) row))))

(defn decide
  "Decides one thing against the record. Takes the record, the gate's depot
  word and a thing as it arrived on that gate's depot. Gives
  `{:record :outcome}`: the record after, and what
  `softland.store.gate.walk/outcome` says."
  [record gate-depot thing]
  (loop [state (walk/begin gate-depot thing)]
    (if-let [reads (seq (walk/reads state))]
      (recur (walk/resume state (into {} (map (fn [read] [read (read-one record read)])) reads)))
      (let [outcome (walk/outcome state)
            lane-text (when-not (:stray outcome) (declaration/id-text (nth (:act-id thing) 0)))]
        {:record (reduce #(write-one %1 lane-text %2) record (:writes outcome))
         :outcome outcome}))))

(defn may-see
  "Whether an asker may see a lane, read once from the record: what
  `softland.store.read-path.lanes/standing` says of her rights. `asker` is
  nil for one who is not signed in; `session` is the sitting that she asks
  in, or nil."
  [record lane asker session]
  (loop [state (lanes/rights-begin lane asker session)]
    (if-let [reads (seq (lanes/rights-reads state))]
      (recur (lanes/rights-resume state (into {} (map (fn [read] [read (read-one record read)])) reads)))
      (lanes/standing (lanes/rights state)))))
