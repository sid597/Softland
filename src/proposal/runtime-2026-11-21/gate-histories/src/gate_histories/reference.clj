(ns gate-histories.reference
  "The statements about permissions, as the plainest code that says them.
  Pure code. It shares nothing with the gate: it reads no row and no stamp,
  and it does not know how a gate tells a permission that was given again
  from the one that ended.

  It keeps, for one layer, what a person would keep on paper: who was given
  a permission by whom, and which of those gives were ended. Each give that
  is taken is a thing of its own, a grant, with a number. A grant that is
  given again after an end is another grant, though its id is the same.

  The statements, in Sid's words where he gave them
  (src/proposal/runtime-2026-11-21/PROPOSAL.md, entry 6):

    S1  An act is taken only under a permission that stands: its holder was
        given it, it was not ended since, and no permission above it was
        ended since.
    S2  \"its never revoking once for all\": after an end, the holder can be
        given a permission again.
    S3  Those beneath do not come back by themselves: what was given beneath
        an ended permission stays ended when the holder above is given a
        permission again. Each is given again, by an act of its own.
    S4  A permission is ended by its giver, by anyone above the giver, and
        by its own holder. Nobody else ends it.
    S5  The layer's first permission does not end.
    S6  A holder has one permission in a layer: a give to a holder whose
        permission stands is refused.
    S7  A permission is given only straight beneath the giver's own.
    S8  Only the holder of the first permission writes a layer's settings.
    S9  A holder who may only see cannot act: no write, no give, no end.
    S10 An act is taken whole or refused whole.

  S4's \"by its own holder\", S5 and S6 are choices of the session that
  built the gate, and Sid's to turn; they are here because the gate holds
  them.

  `judge` takes the state and one thing as its maker sent it. It gives
  whether the statements take the act, the first reason when they do not,
  and the state after.

  What it does not hold: the form of a thing (the declaration's), a fact
  that names something to replace, and anything of a read.")

(def setting-keys
  "The three keys of a layer that take a second write, by their words."
  #{:softland.store/who-may-see :softland.store/builds-on :softland.store/marked-keys})

(def start
  "The state before the layer is made.

    :made?   whether the layer is made
    :at      holder -> what sits at her place: a grant's number, or :end
    :grants  number -> {:holder :permission :parent}, each grant ever taken
    :killed  the numbers of the grants that an end named"
  {:made? false :at {} :grants {} :killed #{} :n 0})

(defn- allows
  "What a permission allows, from its id: its fifth slot, or see and act."
  [permission]
  (if (= 5 (count permission)) (nth permission 4) :see-and-act))

(defn- above
  "The permission above a permission, from its id, or nil."
  [permission]
  (nth permission 3 nil))

(defn- above?
  "True when `upper` is somewhere above `lower`, by their ids."
  [upper lower]
  (loop [p (above lower)]
    (cond (nil? p) false
          (= p upper) true
          :else (recur (above p)))))

(defn alive?
  "True when the grant with this number stands: no end named it, and the
  grant above it stands."
  [state n]
  (loop [n n]
    (let [grant (get-in state [:grants n])]
      (cond (nil? grant) false
            (contains? (:killed state) n) false
            (nil? (:parent grant)) true
            :else (recur (:parent grant))))))

(defn- grant-at
  "The grant at a holder's place, with its number, or nil for a place that
  is empty or holds an end."
  [state holder]
  (let [n (get-in state [:at holder])]
    (when (number? n)
      (assoc (get-in state [:grants n]) :n n))))

(defn standing-grant
  "The grant under which `who` may be heard when she names `permission`:
  the grant at her place, when it stands and has exactly that id. Else nil."
  [state who permission]
  (let [grant (grant-at state who)]
    (when (and grant (= permission (:permission grant)) (alive? state (:n grant)))
      grant)))

(defn- fact-reason
  "Why the statements refuse one fact of an act, or nil. `own` is the grant
  that the act is under."
  [state layer own fact]
  (let [{:keys [e k v]} fact]
    (cond
      ;; a give
      (and (= :softland.store/permission k) (some? v))
      (let [there (grant-at state e)]
        (cond
          (not= (:permission own) (above v)) :not-straight-beneath-the-givers-own   ; S7
          (and there (alive? state (:n there))) :holder-has-a-permission            ; S6
          :else nil))

      ;; an end
      (= :softland.store/permission k)
      (let [there (grant-at state e)]
        (cond
          (nil? there) :nothing-to-end
          (nil? (:parent there)) :the-first-permission-does-not-end                 ; S5
          (or (= (:permission own) (:permission there))
              (above? (:permission own) (:permission there))) nil                   ; S4
          :else :not-above-it))

      ;; a setting
      (contains? setting-keys k)
      (when (some? (:parent own)) :only-the-first-holder-writes-a-setting)         ; S8

      ;; the depot of a layer that is made
      (= :softland.store/depot k) :a-depot-is-written-once

      :else nil)))

(defn- apply-fact
  "The state after one fact of an act that the statements took."
  [state own fact]
  (let [{:keys [e k v]} fact]
    (cond
      (and (= :softland.store/permission k) (some? v))
      (let [n (:n state)]
        (-> state
            (assoc-in [:grants n] {:holder e :permission v :parent (:n own)})
            (assoc-in [:at e] n)
            (update :n inc)))

      (= :softland.store/permission k)
      (let [there (grant-at state e)]
        (-> state
            (update :killed conj (:n there))
            (assoc-in [:at e] :end)))

      :else state)))

(defn judge
  "Takes the state, the layer's id, and a thing as its maker sent it: `:who`,
  `:permission` and `:facts`, each fact with `:e`, `:k` and maybe `:v`.
  Gives {:accept? :why :state}."
  [state layer thing]
  (let [{:keys [who permission facts]} thing]
    (if-not (:made? state)
      ;; the making act: the first permission, written by its holder
      (if (and (= 3 (count permission)) (= who (first permission)))
        {:accept? true
         :state (-> state
                    (assoc :made? true)
                    (assoc-in [:grants 0] {:holder who :permission permission :parent nil})
                    (assoc-in [:at who] 0)
                    (assoc :n 1))}
        {:accept? false :why :layer-not-made :state state})
      (let [own (standing-grant state who permission)]
        (cond
          (nil? own)
          {:accept? false :why :permission-does-not-stand :state state}            ; S1

          (= :see (allows permission))
          {:accept? false :why :may-only-see :state state}                         ; S9

          :else
          (if-let [why (some #(fact-reason state layer own %) facts)]
            {:accept? false :why why :state state}                                 ; S10
            {:accept? true :state (reduce #(apply-fact %1 own %2) state facts)}))))))

(defn looks-intact?
  "True when a permission looks as if it stood to someone who reads ids
  alone: each permission of its chain is the one that sits at its holder's
  place now. A permission can look intact and not stand: one above it was
  ended and given again with the same id. That is the case that a gate must
  tell apart (S3)."
  [state permission]
  (loop [p permission]
    (cond
      (nil? p) true
      (not= p (:permission (grant-at state (first p)))) false
      :else (recur (above p)))))

(defn summary
  "For a trace: who stands in the layer now, and under what."
  [state]
  (into (sorted-map)
        (keep (fn [[holder n]]
                (cond (= :end n) [holder :ended]
                      (alive? state n) [holder (get-in state [:grants n :permission])]
                      :else [holder :ended-with-the-one-above])))
        (:at state)))
