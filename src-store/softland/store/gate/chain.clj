(ns softland.store.gate.chain
  "The chain of a permission: the permissions above it, the places where
  their rows sit, and whether a permission stands. Pure code.

  A permission id holds the permission above it, whole, up to a first
  permission, which has nothing above it (`softland.store.act`). So the
  chain is read from the id alone. Each function takes a permission id in
  its one form.

  A permission stands when the row at its place holds it, and it is a first
  permission or the one above it stands and has a row with a lesser stamp.
  A row with no value is an end, or for a yes for one act its use; a row
  with another permission is another give.

  The stamp tells a permission that is given again from the one that ended.
  A permission id is made of its parts, so a second give of the same parts
  has the same id, and its row has a later stamp. What was given beneath
  the first give has a row older than the second give's, so it does not
  stand: what was given beneath a permission ends with it, and does not
  come back when the holder is given a permission again. Each is given
  again, by an act of its own. A stamp is a gate's
  (`softland.store.gate.stamp`): nobody forges it, and a second run of the
  gates gives the same stamps.

  The end of what is beneath is read at each decision, up the chain, and
  written nowhere. To write an end on each place beneath needs a table of
  who gave to whom, and one end then writes a row for each holder beneath,
  with no bound, in one decision of the lane's one line.

  Cost: to know that a permission stands takes one row for each permission
  of its chain. The depth has a bound (`softland.store.act`, `nesting`)."
  (:require [softland.store.act :as act]
            [softland.store.record.rows :as record]))

(defn above
  [permission]
  (nth permission 3 nil))

(defn place
  "A yes for one act sits on the yes's own id, the uuid that its giver made.
  So a holder's permission for the lane, her permissions for runs and her
  yeses for acts stand or end each by itself, and two yeses for one act sit
  on two places."
  [lane permission]
  (let [[kind what] (act/covers permission)]
    (case kind
      :run (record/place-key lane what :softland.store/run-permission)
      :act (record/place-key lane what :softland.store/act-permission)
      (record/place-key lane (nth permission 0) :softland.store/permission))))

(defn places-above
  "Nearest first."
  [lane permission]
  (loop [upper (above permission)
         keys []]
    (if upper
      (recur (above upper) (conj keys (place lane upper)))
      keys)))

(defn above?
  "Compares ids and reads no row: true does not say that either stands."
  [upper lower]
  (loop [p (above lower)]
    (cond
      (nil? p) false
      (= p upper) true
      :else (recur (above p)))))

(defn stands?
  "`rows` is a map from the key of a place to the row at that place. It must
  hold the row at `place` of the permission and at each of `places-above`,
  for each place that holds one: a place that the map does not hold reads
  as empty, so the permission then does not stand."
  [lane permission rows]
  (loop [p permission
         row (get rows (place lane permission))]
    (cond
      (not (record/holds-value? row p)) false
      (nil? (above p)) true
      :else
      (let [upper (above p)
            upper-row (get rows (place lane upper))]
        (if (and upper-row (< (long (:stamp upper-row)) (long (:stamp row))))
          (recur upper upper-row)
          false)))))
