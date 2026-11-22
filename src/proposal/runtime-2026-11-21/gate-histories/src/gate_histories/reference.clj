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
    S11 Whoever holds a permission that stands may see the layer, whatever
        the permission allows. A holder whose permission ended, or beneath
        one that ended, may not see a layer that is open to its members.
    S12 A layer that its first holder opened to everyone is seen by
        everyone, signed in or not; closed again, by its members only.

  S4's \"by its own holder\", S5 and S6 are choices of the session that
  built the gate, and Sid's to turn; they are here because the gate holds
  them.

  Piece 2 of the store's line adds a permission for one run and for one act
  (src/proposal/runtime-2026-11-21/store-piece-2/SPEC.md, \"The statements\").
  S6 becomes: a holder has at most one permission that covers the layer and
  stands, and beside it one for each of any number of runs and of acts.

    R1  An act is taken under a run's permission only when its who is the
        holder, its `:session` is that run, and the permission stands and
        allows acting.
    R2  An act is taken under an act's permission only when its who is the
        holder, the uuid of its act-id is the one named, and it is that act
        part for part. No other act is ever taken under it.
    R3  A permission for a run or for an act stands as any permission does.
    R4  The end of one of a holder's permissions ends no other of hers,
        unless that other is beneath it.
    R5  A permission for a run or for an act is ended by its giver, by
        anyone above the giver, and by its holder: under that permission
        itself, or under her permission for the layer, and never under her
        permission for another run or act. After an end it can be given
        again.
    R6  A run or an act has at most one permission that stands.
    R7  A setting is written under the layer's first permission alone.
    R8  An asker may see a layer that is open to its members when her
        permission for the layer stands, or when she asks in a run whose
        permission stands and names her. An act's permission lets nobody
        see.
    R9  A thing that names a permission for another run than its own, or
        for another act, is refused.
    R10 A permission for a run or for an act gives nothing. So nothing is
        ever beneath one. This statement came from running the statements
        by themselves (`gate-histories.design`): without it, a holder for
        one run gives herself a permission for the layer.

  `judge` takes the state and one thing as its maker sent it. It gives
  whether the statements take the act, the first reason when they do not,
  and the state after.

  `may-see` says whether an asker may see the layer.

  What it does not hold: the form of a thing (the declaration's), a fact
  that names something to replace, and what a read gives beyond whether the
  asker may see the layer.")

(def setting-keys
  "The three keys of a layer that take a second write, by their words."
  #{:softland.store/who-may-see :softland.store/builds-on :softland.store/marked-keys})

(def permission-keys
  "The key that a permission fact is under, for each kind of thing that a
  permission covers."
  {:softland.store/permission :layer
   :softland.store/run-permission :run
   :softland.store/act-permission :act})

(def ^:dynamic *short-gives?*
  "False: R10 holds, a permission for a run or for an act gives nothing.
  True: the statements as they were before R10, for `gate-histories.design`
  to show what they let through."
  false)

(def ^:dynamic *digest-of*
  "Takes a thing as it arrives. Gives what an act's permission holds of it:
  the act part for part, without its time and its permission. Here the
  plainest thing that says so; a run against the gate puts the store's own
  digest in its place."
  (fn [thing] (pr-str (into (sorted-map) (dissoc thing :permission :door-time)))))

(def start
  "The state before the layer is made.

    :made?   whether the layer is made
    :at      place -> what sits there: a grant's number, or :end. A place
             is [:layer holder], [:run session] or [:act uuid]
    :grants  number -> {:holder :permission :parent}, each grant ever taken
    :killed  the numbers of the grants that an end named
    :everyone?  whether the layer is open to everyone. A layer starts
                open to its members only"
  {:made? false :at {} :grants {} :killed #{} :n 0 :everyone? false})

(defn covers
  "What a permission covers, from its id: [:layer], [:run session] or
  [:act uuid digest]."
  [permission]
  (if (= 6 (count permission)) (nth permission 5) [:layer]))

(defn- allows
  "What a permission allows, from its id: its fifth slot, or see and act."
  [permission]
  (if (<= 5 (count permission)) (nth permission 4) :see-and-act))

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

(defn- place-of
  "The place where a permission sits, from its id: on its holder, on the
  session of its run, or on the uuid of its act."
  [permission]
  (let [[kind id] (covers permission)]
    (if (= :layer kind) [:layer (first permission)] [kind id])))

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
  "The grant at a place, with its number, or nil for a place that is empty
  or holds an end."
  [state place]
  (let [n (get-in state [:at place])]
    (when (number? n)
      (assoc (get-in state [:grants n]) :n n))))

(defn standing-grant
  "The grant under which `who` may be heard when she names a permission for
  the layer: the grant at her place, when it stands and has exactly that id.
  Else nil."
  [state who permission]
  (let [grant (grant-at state [:layer who])]
    (when (and grant (= permission (:permission grant)) (alive? state (:n grant)))
      grant)))

(defn- heard-under
  "Under what a thing is heard: {:grant g} when the permission that it names
  stands for it, by S1, R1 and R2; else {:why word}."
  [state thing]
  (let [{:keys [who permission session act-id]} thing
        [kind id digest] (covers permission)
        grant (grant-at state (place-of permission))]
    (cond
      (not= who (first permission)) {:why :who-not-the-permissions-holder}
      (and (= :run kind) (not= session id)) {:why :permission-for-another-run}          ; R1, R9
      (and (= :act kind) (not= id (nth act-id 2 nil))) {:why :permission-for-another-act} ; R2, R9
      (and (= :act kind) (not= digest (*digest-of* thing))) {:why :act-not-the-one-permitted} ; R2
      (not (and grant (= permission (:permission grant)) (alive? state (:n grant))))
      {:why :permission-does-not-stand}                                                ; S1, R3
      :else {:grant grant})))

(defn- fact-reason
  "Why the statements refuse one fact of an act, or nil. `own` is the grant
  that the act is under, and `who` the act's who."
  [state who own fact]
  (let [{:keys [e k v]} fact
        kind (permission-keys k)
        own-kind (first (covers (:permission own)))]
    (cond
      ;; a give
      (and kind (some? v))
      (let [[v-kind] (covers v)
            place (place-of v)
            there (grant-at state place)]
        (cond
          (and (not *short-gives?*) (not= :layer own-kind)) :only-a-permission-for-the-layer-gives ; R10
          (not= kind v-kind) :permission-fact-under-another-key
          (not= e (second place)) :permission-fact-not-on-what-it-covers
          (and (= :act v-kind) (not= :see-and-act (allows v))) :an-acts-permission-says-see-and-act
          (not= (:permission own) (above v)) :not-straight-beneath-the-givers-own   ; S7
          (and there (alive? state (:n there))) :holder-has-a-permission            ; S6, R6
          :else nil))

      ;; an end
      kind
      (let [there (grant-at state [kind e])]
        (cond
          (nil? there) :nothing-to-end
          (nil? (:parent there)) :the-first-permission-does-not-end                 ; S5
          (or (= (:permission own) (:permission there))
              (above? (:permission own) (:permission there))) nil                   ; S4, R5
          (and (= :layer own-kind) (= who (:holder there))) nil                     ; R5, the holder
          :else :not-above-it))

      ;; a setting
      (contains? setting-keys k)
      (when (some? (:parent own)) :only-the-first-holder-writes-a-setting)         ; S8, R7

      ;; the depot of a layer that is made
      (= :softland.store/depot k) :a-depot-is-written-once

      :else nil)))

(defn- apply-fact
  "The state after one fact of an act that the statements took."
  [state own fact]
  (let [{:keys [e k v]} fact
        kind (permission-keys k)]
    (cond
      (and kind (some? v))
      (let [n (:n state)]
        (-> state
            (assoc-in [:grants n] {:holder (first v) :permission v :parent (:n own)})
            (assoc-in [:at (place-of v)] n)
            (update :n inc)))

      kind
      (let [there (grant-at state [kind e])]
        (-> state
            (update :killed conj (:n there))
            (assoc-in [:at [kind e]] :end)))

      (= :softland.store/who-may-see k)
      (assoc state :everyone? (= :everyone v))

      :else state)))

(defn judge
  "Takes the state, the layer's id, and a thing as its maker sent it: `:who`,
  `:permission` and `:facts`, each fact with `:e`, `:k` and maybe `:v`; and,
  for a thing under a permission for a run or for an act, its `:session` and
  its `:act-id`. Gives {:accept? :why :state}, and `:under`, the grant that
  an act that was taken was under."
  [state _layer thing]
  (let [{:keys [who permission facts]} thing]
    (if-not (:made? state)
      ;; the making act: the first permission, written by its holder
      (if (and (= 3 (count permission)) (= who (first permission)))
        {:accept? true
         :state (-> state
                    (assoc :made? true)
                    (assoc-in [:grants 0] {:holder who :permission permission :parent nil})
                    (assoc-in [:at [:layer who]] 0)
                    (assoc :n 1))}
        {:accept? false :why :layer-not-made :state state})
      (let [{own :grant why :why} (heard-under state thing)]
        (cond
          (nil? own)
          {:accept? false :why why :state state}                                   ; S1

          (= :see (allows permission))
          {:accept? false :why :may-only-see :state state}                         ; S9

          :else
          (if-let [why (some #(fact-reason state who own %) facts)]
            {:accept? false :why why :state state}                                 ; S10
            {:accept? true :under own
             :state (reduce #(apply-fact %1 own %2) state facts)}))))))

(defn may-see
  "Whether an asker may see the layer, by S11, S12 and R8: `:not-made` before
  the layer is made, else `:yes` or `:may-not-see`. `run` is the run that
  she asks in, or nil."
  ([state who signed-in?] (may-see state who nil signed-in?))
  ([state who run signed-in?]
   (cond
     (not (:made? state)) :not-made
     (:everyone? state) :yes
     (not signed-in?) :may-not-see
     :else (let [hers (grant-at state [:layer who])
                 runs (when run (grant-at state [:run run]))]
             (if (or (and hers (alive? state (:n hers)))
                     (and runs (= who (:holder runs)) (alive? state (:n runs))))
               :yes
               :may-not-see)))))

(defn ends-as-holder?
  "True when the statements take this end only because the act's who is the
  holder of the permission that it ends, and her act is under her
  permission for the layer (R5): she is not its giver and not above it."
  [state thing fact]
  (let [kind (permission-keys (:k fact))
        there (when (and kind (nil? (:v fact))) (grant-at state [kind (:e fact)]))
        own (:permission thing)]
    (boolean (and there
                  (= (:who thing) (:holder there))
                  (not= own (:permission there))
                  (not (above? own (:permission there)))))))

(defn looks-intact?
  "True when a permission looks as if it stood to someone who reads ids
  alone: each permission of its chain is the one that sits at its place
  now. A permission can look intact and not stand: one above it was ended
  and given again with the same id. That is the case that a gate must tell
  apart (S3)."
  [state permission]
  (loop [p permission]
    (cond
      (nil? p) true
      (not= p (:permission (grant-at state (place-of p)))) false
      :else (recur (above p)))))

(defn chain
  "The grants of a grant's chain, itself first, then each one above."
  [state grant]
  (take-while some? (iterate (fn [g] (when-let [n (:parent g)] (assoc (get-in state [:grants n]) :n n))) grant)))

(defn summary
  "For a trace: what stands in the layer now, by place."
  [state]
  (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
        (keep (fn [[place n]]
                (cond (= :end n) [place :ended]
                      (alive? state n) [place (get-in state [:grants n :permission])]
                      :else [place :ended-with-the-one-above])))
        (:at state)))
