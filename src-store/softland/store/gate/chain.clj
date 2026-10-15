(ns softland.store.gate.chain
  "The chain of a permission: the levels above it, the places where they
  sit, and whether the whole chain still stands. Pure code. It needs no
  cluster. The one rule of a chain, for the gates' check of an act's
  standing and for the read path's check of who may see a layer.

  A permission id holds its whole chain (`softland.store.act`, the shape
  `:permission-id`): `[holder layer granted-in]` is a first permission,
  and `[holder layer granted-in parent]` or `[holder layer granted-in
  parent allows]` a given one, whose fourth slot is the permission above
  it. So the levels of a permission are itself, its parent, the parent's
  parent, and so on to the first permission of the layer, which has
  nothing above it. Each level sits on its own holder, at the place
  `<layer> <holder> :softland.store/permission`.

  A right can end (Sid, 15 October 2026: rights can be taken back). The
  end is a later fact at the right's place, a retract, and ending one ends
  what was given beneath it. So a permission stands when each of its
  levels stands, and a level stands when:

    1. the row at its holder's place holds exactly that permission; and
    2. for a level above the first, its row was written before the row of
       the level beneath it: its stamp is lower.

  The second is what makes an end reach down for good. A right that is
  ended and then given again, with the same id, has a row of a later
  stamp than every right that was given beneath it before the end, so
  those stay ended; a right given beneath it after the new give stands.
  In a chain that no end has touched the order holds by itself: a give is
  decided under the giver's permission, which stood, so its row is later
  than the giver's. The rule reads stamps of one layer only, which are the
  layer's count of decisions (`softland.store.gate.stamp`), and no time.

  Inputs and results, by call:

    levels        a permission id -> the levels, itself first, the first
                  permission of the chain last
    parent-of     a permission id -> the permission above it, or nil
    level-places  a layer and a permission id -> the key of each level's
                  place, in the order of `levels`
    stands?       a layer, a permission id, and rows by place key -> true
                  when every level stands
    above?        a permission id, and another -> true when the first is
                  one of the levels above the second

  What must hold. A permission id here is one in the act's one form
  (`softland.store.act/parse`): its depth is bounded, so `levels` ends. A
  row is a row of the record (`softland.store.record.rows`), or nil.

  State and effects. None. Each function is pure and never throws on a
  row.

  Callers. `softland.store.gate.decision`: `levels-needed` asks
  `level-places`, `standing` asks `stands?`, and the check of an end asks
  `above?`; the walk of one thing reads the rows that `levels-needed` names
  (`softland.store.module.walk`). The read path's
  layers (`softland.store.read-path.layers`), for whether a holder of a
  permission may see a members layer.

  Not here. What a permission allows (`softland.store.act/allows`), and
  who may end a right: the gate's check of the end
  (`softland.store.gate.decision`)."
  (:require [softland.store.record.rows :as record]))

(defn parent-of
  "Takes a permission id. Gives the permission above it, its fourth slot, or
  nil for a first permission."
  [permission]
  (nth permission 3 nil))

(defn levels
  "Takes a permission id. Gives its levels as a vector: the permission
  itself first, then each permission above it, the first permission of the
  chain last. Bounded by the depth of a permission id."
  [permission]
  (into [] (take-while some?) (iterate parent-of permission)))

(defn level-places
  "Takes a layer id and a permission id. Gives the key of the place of each
  level, in the order of `levels`: `<layer> <holder> :softland.store/permission`
  for the holder of each level. Every place is in the layer given, which is
  the act's layer: a level whose own layer slot is another layer does not
  stand (`stands?`), and its place here is in the layer given all the same,
  so no read leaves the layer's task."
  [layer permission]
  (mapv (fn [level] (record/place-key layer (nth level 0) :softland.store/permission))
        (levels permission)))

(defn stands?
  "Takes a layer id, a permission id, and the rows of the places of its
  levels, as a map from place key to row (nil or absent for an empty
  place). Gives true when every level stands: its own layer slot is the
  layer, the row at its place holds exactly that permission, and, for each
  level above the first, that row's stamp is lower than the stamp of the
  row of the level beneath it. A retract at a level's place holds no value,
  so a right that was ended does not stand, and neither does any right
  beneath it. A row with no whole-number stamp, which no gate writes, does
  not stand either, so the function never throws on a row. What must hold: the map holds the row of each place that
  `level-places` names, or the place is empty."
  [layer permission rows]
  (let [ls (levels permission)
        places (level-places layer permission)]
    (loop [i 0
           below-stamp nil]
      (if (= i (count ls))
        true
        (let [level (nth ls i)
              row (get rows (nth places i))]
          (if (and (= layer (nth level 1))
                   (record/holds-value? row level)
                   (int? (:stamp row))
                   (or (nil? below-stamp)
                       (< (long (:stamp row)) (long below-stamp))))
            (recur (inc i) (:stamp row))
            false))))))

(defn above?
  "Takes two permission ids. Gives true when the first is one of the levels
  above the second: its parent, or the parent's parent, and so on. False
  for the second itself, so no holder ends her own right by this test."
  [upper permission]
  (boolean (some #(= upper %) (rest (levels permission)))))
