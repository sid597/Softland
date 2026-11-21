(ns softland.store.gate.chain
  "The chain of a permission: the permissions above it, the places where
  their rows sit, and whether a permission stands. Pure code. It needs no
  cluster.

  A permission id holds the permission above it, whole, and that one holds
  the one above it, up to a first permission, which has nothing above it
  (`softland.store.act`, the forms of a permission id). So the chain of a
  permission is read from its id alone, and each permission of the chain
  sits on its own holder, in the same layer: a gate takes a give only
  straight beneath the giver's own permission, in the giver's layer
  (`softland.store.gate.decision`, check (g)).

  When a permission stands. All of these hold:

  1. the row at its holder's place holds it: the last fact taken at that
     place gave exactly this permission. A row with no value is an end, and
     a row with another permission is another give;
  2. it is a first permission; or the permission above it stands, and the
     row of the one above is older than its own row: the stamp of the row
     above is less than the stamp of its own row.

  The second half of 2 is how a permission that was given again is told
  from the one that ended. A permission id is made of its parts, so a second
  give of the same parts has the same id. Its row has a later stamp. A
  permission that was given beneath the first give has a row that is older
  than the row of the second give, so it does not stand: what was given
  beneath a permission ends with it, and does not come back when the holder
  is given a permission again. Each is given again, by an act of its own. A
  stamp is a gate's (`softland.store.gate.stamp`): nobody forges it, and a
  second run of the gates gives the same stamps.

  Sid decided on 20 and 21 November 2026 that a permission is ended by its
  giver or by anyone above, that what was given beneath ends with it, and
  that a holder can be given a permission again
  (src/proposal/runtime-2026-11-21/PROPOSAL.md, entry 6). That the end of
  what is beneath is read at each decision, up the chain, and written
  nowhere, and that the stamp tells a second give from the first, are a
  session's choices of 21 November 2026, and his to turn. The other way to
  end what is beneath writes an end on each place beneath at the end
  itself: it needs a table of who gave to whom, and one end then writes a
  row for each holder beneath, with no bound, in one decision of the
  layer's one line. The other way to tell a second give is one more part of
  the id; it can be added later as a slot was (`softland.store.act`).

  What it costs. To know that a permission stands takes the row of each
  permission of its chain: as many rows as the permission is deep. A first
  permission is one row, as before. The depth has a bound
  (`softland.store.act`, `nesting`).

  Inputs and results, by call:

    above         a permission -> the permission above it, or nil
    place         a layer, a permission -> the key of its holder's place
    places-above  a layer, a permission -> the keys of the places of the
                  permissions above it, nearest first
    above?        two permissions -> true when the first is in the chain
                  above the second
    stands?       a layer, a permission, rows by key -> true or false

  State and effects. None. Each function is pure, and none throws on a
  permission id in its one form.

  Callers. `softland.store.gate.decision`, for the standing of an act and
  for an end and a give at a place that holds a row. The read path's
  `softland.store.read-path.layers`, for whether an asker may see a layer
  that is open to its members."
  (:require [softland.store.record.rows :as record]))

(defn above
  "Takes a permission id in its one form. Gives the permission above it, its
  fourth slot, or nil for a first permission."
  [permission]
  (nth permission 3 nil))

(defn place
  "Takes a layer and a permission id. Gives the key of the place where the
  permission sits in that layer: on its holder, under
  `:softland.store/permission`."
  [layer permission]
  (record/place-key layer (nth permission 0) :softland.store/permission))

(defn places-above
  "Takes a layer and a permission id. Gives the keys of the places of the
  permissions above it in that layer, nearest first: the place of the one
  above, then of the one above that, up to the first permission. An empty
  vector for a first permission. One key for each permission above, also
  when one holder is in the chain twice."
  [layer permission]
  (loop [upper (above permission)
         keys []]
    (if upper
      (recur (above upper) (conj keys (place layer upper)))
      keys)))

(defn above?
  "Takes two permission ids. Gives true when the first is in the chain
  above the second: it is the one above it, or above that one, up to the
  first permission. False for the same permission twice. It compares ids
  and reads no row: whether either stands is `stands?`."
  [upper lower]
  (loop [p (above lower)]
    (cond
      (nil? p) false
      (= p upper) true
      :else (recur (above p)))))

(defn stands?
  "Takes a layer; a permission id; and rows, a map from the key of a place
  to the row at that place, nil or absent for an empty place. Gives true
  when the permission stands in the layer, by the two rules of the
  namespace docstring: its row holds it, and each permission above it stands
  and has an older row.

  What must hold: the map holds the row at `place` of the permission and at
  each of `places-above`, for each place that holds one. A place that the
  map does not hold reads as empty, so the permission then does not stand."
  [layer permission rows]
  (loop [p permission
         row (get rows (place layer permission))]
    (cond
      (not (record/holds-value? row p)) false
      (nil? (above p)) true
      :else
      (let [upper (above p)
            upper-row (get rows (place layer upper))]
        (if (and upper-row (< (long (:stamp upper-row)) (long (:stamp row))))
          (recur upper upper-row)
          false)))))
