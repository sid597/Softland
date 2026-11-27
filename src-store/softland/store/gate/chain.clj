(ns softland.store.gate.chain
  "The chain of a permission: the permissions that it is lent from, one above
  another, whether it stands, and what it allows at this moment. Pure code.

  A permission is a fact on an id of its own
  (`softland.store.door.declaration`, `:permission-value`). It is lent from
  another permission of its lane, which its value names by id, or it stands
  on its own. So the chain is found one row at a time: the row at a
  permission's place names the one above it.

  A permission stands when the row at its place holds what it says, and it
  stands on its own or the one that it is lent from stands. A row with no
  value is an end, or the use of a permission for one exact act. An id
  takes one permission for good: given again, a permission has another id.
  So no stamp is compared, and a row goes from what a permission says to
  its end and never back.

  What a lent permission allows is what it says and no more than each
  permission above it allows as that one's row says now (`allows-now`). So
  a holder who is lowered stays in, what she lent is capped at once, and
  it is uncapped when she is raised again; nothing is written on the
  permissions beneath for either.

  The end of what is beneath is read at each decision, up the chain, and
  written nowhere. To write an end on each permission beneath needs a table
  of who lent to whom, and one end then writes a row for each holder
  beneath, with no bound, in one decision of the lane's one line.

  A limit. A row with no value does not say what the permission that it
  ended was lent from. So above an end the chain cannot be read: a
  permission that fell with the one above it is known to have fallen, and
  not who stands further up.

  Cost: to know that a permission stands takes one row for each permission
  of its chain, `max-lent-depth` at most."
  (:require [clojure.set :as set]
            [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.record.rows :as record]))

(def max-lent-depth
  "How many permissions a chain holds at most, the lowest and the one that
  stands on its own among them. A gate refuses the give that would make a
  chain longer, so a walk up a chain reads this many rows at most; and a
  walk stops there whatever the rows say, so rows that came to the table by
  another hand cannot make it go round."
  32)

(defn place
  [lane id]
  (record/place-key lane id declaration/permission-key))

(defn held-by?
  "True when a party holds a permission whose holder is `holder`: she is
  that holder; or the holder is anyone at all, and she is a person or is
  not signed in, which `party` nil says; or the holder is anyone signed in,
  and she is a person. An agent or a run is never everyone on its own: it
  comes in through its person, under a permission that is lent to it. The
  system is no person."
  [holder party]
  (let [person? (= :person (act/party-kind party))]
    (boolean (or (and (some? party) (= holder party))
                 (and (= :anyone holder) (or (nil? party) person?))
                 (and (= :anyone-signed-in holder) person?)))))

(defn climb
  "From the permission at an id up, as far as `rows` goes. `rows` is a map
  from the key of a place to its row; a key that was read is in it also
  when its place is empty. Gives a map:

    :values  what each permission says, the one at `id` first and then each
             that the one before is lent from
    :ids     the id of each, in the same order
    :wants   the key of the next place above, when `rows` does not hold it
    :whole?  true when the last of `:values` stands on its own, so that the
             chain was read to its top and every row of it held a value

  Neither `:wants` nor `:whole?`: a place of the chain holds no permission,
  being empty, an end or a use; or the chain is over the depth. Then the
  permission at `id` does not stand."
  [lane id rows]
  (loop [id id
         values []
         ids []]
    (let [k (place lane id)]
      (if-not (contains? rows k)
        {:values values :ids ids :wants k}
        (if-let [v (record/held-permission (get rows k))]
          (let [values (conj values v)
                ids (conj ids id)
                lender (:lent-from v)]
            (cond
              (nil? lender) {:values values :ids ids :whole? true}
              (<= max-lent-depth (count values)) {:values values :ids ids}
              :else (recur lender values ids)))
          {:values values :ids ids})))))

(defn stands?
  "What must hold: `rows` holds the row of each place that `climb` wanted."
  [lane id rows]
  (boolean (:whole? (climb lane id rows))))

(defn- meet
  "What two sets of rights both allow."
  [mine lenders]
  (let [may-give (set/intersection (:give mine #{}) (:give lenders #{}))]
    (cond-> {}
      (and (:see mine) (:see lenders)) (assoc :see true)
      (and (:write mine) (:write lenders)) (assoc :write :all)
      (seq may-give) (assoc :give may-give)
      (and (:owner mine) (:owner lenders)) (assoc :owner true))))

(defn allows-now
  "What a permission allows at this moment: what it says, and no more than
  each permission above it says. `values` is `climb`'s, of a chain that is
  whole."
  [values]
  (reduce meet (:allows (first values)) (map :allows (rest values))))

(defn above?
  "Whether the permission at `upper` is one that the permission at `lower`
  is lent from, directly or further up, as the rows say: true, false, or
  `{:wants key}` when a row that would say it is not in `rows`. False as
  soon as a place of the way up holds no permission: above an end the
  chain is not known."
  [lane upper lower rows]
  (loop [id lower
         n 1]
    (let [k (place lane id)]
      (if-not (contains? rows k)
        {:wants k}
        (let [lender (:lent-from (record/held-permission (get rows k)))]
          (cond
            (nil? lender) false
            (= upper lender) true
            (<= max-lent-depth n) false
            :else (recur lender (inc n))))))))
