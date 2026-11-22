(ns gate-histories.reference
  "The statements about permissions, as the plainest code that says them.
  Pure code. It shares nothing with the gate: it reads no row, no stamp and
  no digest, and it does not know how a gate tells a permission that was
  given again from the one that ended, or how it marks a yes as used.

  It keeps, for one layer, what a person would keep on paper: who was given
  a permission by whom, which of those gives were ended, which yes for one
  act was used, and the one answer of each act-id. Each give that is taken
  is a thing of its own, a grant, with a number. A grant that is given
  again after an end is another grant, though its id is the same.

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

  Piece 2 of the store's line adds a permission for one run and a yes for
  one act (src/proposal/runtime-2026-11-21/store-piece-2/SPEC.md, \"The
  statements\"). S6 becomes: a holder has at most one permission that
  covers the layer and stands, and beside it one for each of any number of
  runs, and any number of yeses for one act.

  A yes for one act has the form of 22 November, the one in the prompt that
  Sid pasted that day, which a session wrote and he can turn:
  `[who layer granted-in above :see-and-act [:act yes digest]]`. `yes` is a
  uuid that its giver makes, the yes's own id, and the yes sits on it. The
  digest names the act as its maker made it. The act goes under any act-id
  of its maker's: the act-id plays no part in a yes.

    R1  An act is taken under a run's permission only when its who is the
        holder, its `:session` is that run, and the permission stands and
        allows acting.
    R2  An act is taken under a yes for one act only when its who is the
        yes's holder, it is the act that the yes names part for part, the
        yes stands, and it is not used. The act that is taken under it uses
        it up: no act is ever taken under it again, the same act under
        another act-id neither. A try that is refused uses nothing.
    R3  A permission for a run or a yes for one act stands as any
        permission does.
    R4  The end of one of a holder's permissions ends no other of hers,
        unless that other is beneath it.
    R5  A permission for a run or a yes for one act is ended by its giver,
        by anyone above the giver, and by its holder. Its holder ends it
        under her permission for the layer, or under the permission itself
        when that permission takes the end: a permission for a run that
        allows acting takes an end of itself sent in its run; a yes takes
        no act but its one act, so it ends itself only when that act is its
        end; a permission that says see takes no act at all. Never under
        her permission for another run or another yes. A yes that is used
        has nothing left to end. After an end a run can be given a
        permission again, and an act another yes, under a new id.
    R6  A run has at most one permission that stands. An act can have any
        number of yeses that stand at once, each used once: a yes's place
        takes one give, and nothing is ever given there again, once it is
        ended or used.
    R7  A setting is written under the layer's first permission alone.
    R8  An asker may see a layer that is open to its members when her
        permission for the layer stands, or when she asks in a run whose
        permission stands and names her. A yes for one act lets nobody see.
    R9  A thing that names a permission for another run than its own, or a
        yes for another act, is refused.
    R10 A permission for a run or a yes for one act gives nothing. So
        nothing is ever beneath one. The session that wrote these statements
        found it by running them by themselves, before any of piece 2 was
        built (SPEC.md, R10): without it, a holder for one run gives herself
        a permission for the layer.

  R2's \"part for part\" is said here in plain code (`the-act`), not by a
  digest: a yes names its act by a digest, and the statements are told which
  act each yes was for (`*act-named*`). So where the gate's digest and the
  act part for part come apart, the gate and the statements differ, and a
  run shows it.

  One answer for an act-id, by Sid's decision of 7 October 2026
  (src-store/README.md, the decisions of that day, entry 6: \"a repeat is
  answered, not decided twice\"), and as SPEC.md says it since 22 November:

    once  An act-id names one act and gets one answer, whoever sent first:
          the answer of the first thing decided under it, a refusal too.
          The same thing sent again under it is not decided again and is
          told that answer; another thing under it is not decided, and its
          sender is told that it is not hers (\"not mine\"). The same thing
          is the same act part for part, under the same permission. So a
          refused try is sent again under a new act-id.

  Two more that a history is held to, which the review of piece 2 showed
  missing (src/proposal/runtime-2026-11-21/overnight-audit/REVIEW-store-piece-2.md,
  section 1). Each compares the gate with the statements, so the run holds
  them (`gate-histories.run`); this namespace gives what they need: the
  answer that the statements give each thing (`judge`), who is inside a
  permission (`inside`), and what a yes's place holds (`place-state`).

    T1  An act that the statements take is taken by the gate, and its
        sender is told so.
    T2  Nothing that an outsider of a yes for one act sends writes at the
        yes's place or uses it up. An outsider of a yes is one who is not
        its holder, its giver or one above its giver.

  `judge` takes the state and one thing as it arrived. It gives whether the
  statements take the act, the first reason when they do not, the answer
  that its sender is to be told, and the state after.

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
  "False: R10 holds, a permission for a run or a yes for one act gives
  nothing. True: the statements as they were before R10, to show what they
  let through."
  false)

(def ^:dynamic *act-named*
  "Takes [yes text], the two that a yes for one act names in its sixth slot.
  Gives the act that its giver said yes to, as its maker made it, or nil for
  a yes that names no act that anybody made. It is what the history knows
  of each yes, written down when the yes was made: a person who says yes to
  an act knows the act. The statements hold an act to it part for part
  (`the-act`), and never compute a digest."
  (constantly nil))

(def start
  "The state before the layer is made.

    :made?    whether the layer is made
    :at       place -> what sits there: a grant's number, or :end. A place
              is [:layer holder], [:run session] or [:act yes]
    :given    place -> each grant's number and each :end that was put
              there, in order
    :grants   number -> {:holder :permission :parent}, each grant ever taken
    :killed   the numbers of the grants that an end named
    :used     the numbers of the yeses for one act that an act was taken
              under
    :answers  act-id -> {:sent the first thing decided under it, as sent
              (`as-sent`), :answer its answer}
    :everyone?  whether the layer is open to everyone. A layer starts
                open to its members only"
  {:made? false :at {} :given {} :grants {} :killed #{} :used #{} :answers {} :n 0 :everyone? false})

(defn covers
  "What a permission covers, from its id: [:layer], [:run session] or
  [:act yes digest]."
  [permission]
  (if (= 6 (count permission)) (nth permission 5) [:layer]))

(defn- allows
  "What a permission allows, from its id: its fifth slot, or see and act."
  [permission]
  (if (<= 5 (count permission)) (nth permission 4) :see-and-act))

(defn above
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

(defn inside
  "Who is inside a permission, for T2: its holder, its giver, and each
  holder above the giver, read from its id. Takes a permission. Gives a set
  of whos. Whoever else sends is an outsider of it."
  [permission]
  (into #{} (map first) (take-while some? (iterate above permission))))

(defn place-of
  "The place where a permission sits, from its id: on its holder, on the
  session of its run, or on the yes's own id."
  [permission]
  (let [[kind id] (covers permission)]
    (if (= :layer kind) [:layer (first permission)] [kind id])))

(def ^:private left-out-parts
  "The parts of an act that its maker may leave out, with what a part that
  is left out says. The act's form gives each of them a default
  (`softland.store.act/shapes`); they are said here again, in plain words,
  so that an act that leaves a part out and one that gives it empty are the
  same act part for part."
  {:based-on {} :because-of nil :claimed-when nil :subjects #{}})

(defn the-act
  "An act part for part, as the statements compare two acts. Takes a thing
  as it arrived, or an act as its maker made it, or nil. Gives each of its
  parts but the four that are no part of the act that a yes names: its
  act-id, its depot, the time on it, and its permission. A part that its
  maker left out reads as that part says when it is empty; its facts come
  as a vector, each fact with its four parts, a part left out as nil. Two
  things are the same act when what this gives is equal by Clojure's `=`:
  so a list and a vector of the same facts are one act, and so are an act
  that gives `:subjects #{}` and one that leaves it out. nil for nil."
  [thing]
  (when thing
    (-> (merge left-out-parts (dissoc thing :permission :act-id :depot :door-time))
        (update :facts (fn [facts] (mapv (fn [fact] (merge {:v nil :replaces nil} fact)) facts))))))

(defn as-sent
  "A thing as the statement `once` compares two sends under one act-id: the
  act part for part (`the-act`), and the permission that it names."
  [thing]
  (assoc (the-act thing) :permission (:permission thing)))

(defn alive?
  "True when the grant with this number stands as any permission stands: no
  end named it, and the grant above it stands. A yes that was used is still
  alive by this, and is no less used (`used?`)."
  [state n]
  (loop [n n]
    (let [grant (get-in state [:grants n])]
      (cond (nil? grant) false
            (contains? (:killed state) n) false
            (nil? (:parent grant)) true
            :else (recur (:parent grant))))))

(defn used?
  "True when the grant with this number is a yes for one act under which an
  act was taken (R2)."
  [state n]
  (contains? (:used state) n))

(defn grant-at
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

(defn given-again?
  "True when the place of a permission held something before what sits
  there now: an end, or a grant before it. So what is there now was given
  again (S2, R5). Never true at a yes's place, which takes one give (R6)."
  [state permission]
  (< 1 (count (get-in state [:given (place-of permission)]))))

(defn place-state
  "What the place of a permission holds now, in a word: `:nothing`, `:ended`,
  `:used` for a yes under which an act was taken, `:stands` when the grant
  there stands and has exactly that id, `:another` when another permission
  is there, or `:ended-above` when that permission is there and one above
  it ended."
  [state permission]
  (let [at (get-in state [:at (place-of permission)])
        grant (grant-at state (place-of permission))]
    (cond (nil? at) :nothing
          (= :end at) :ended
          (not= permission (:permission grant)) :another
          (used? state (:n grant)) :used
          (alive? state (:n grant)) :stands
          :else :ended-above)))

(defn- heard-under
  "Under what a thing is heard: {:grant g} when the permission that it names
  stands for it, by S1, R1 and R2; else {:why word}."
  [state thing]
  (let [{:keys [who permission session]} thing
        [kind id digest] (covers permission)
        grant (grant-at state (place-of permission))]
    (cond
      (not= who (first permission)) {:why :who-not-the-permissions-holder}
      (and (= :run kind) (not= session id)) {:why :permission-for-another-run}         ; R1, R9
      (and (= :act kind) (let [named (*act-named* [id digest])]
                           (or (nil? named) (not= (the-act named) (the-act thing)))))
      {:why :act-not-the-one-permitted}                                                ; R2, R9
      (not (and grant (= permission (:permission grant)) (alive? state (:n grant))))
      {:why :permission-does-not-stand}                                                ; S1, R3
      (used? state (:n grant)) {:why :the-yes-is-used}                                 ; R2
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
          (and (= :act v-kind) (seq (get-in state [:given place]))) :a-yes-is-given-once ; R6
          (and there (alive? state (:n there))) :holder-has-a-permission            ; S6, R6
          :else nil))

      ;; an end
      kind
      (let [there (grant-at state [kind e])]
        (cond
          (nil? there) :nothing-to-end
          (used? state (:n there)) :nothing-to-end                                  ; R5: a used yes
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
      (let [n (:n state)
            place (place-of v)]
        (-> state
            (assoc-in [:grants n] {:holder (first v) :permission v :parent (:n own)})
            (assoc-in [:at place] n)
            (update-in [:given place] (fnil conj []) n)
            (update :n inc)))

      kind
      (let [there (grant-at state [kind e])]
        (-> state
            (update :killed conj (:n there))
            (assoc-in [:at [kind e]] :end)
            (update-in [:given [kind e]] (fnil conj []) :end)))

      (= :softland.store/who-may-see k)
      (assoc state :everyone? (= :everyone v))

      :else state)))

(defn- judge-afresh
  "The statements' judgment of a thing under an act-id that has no answer:
  S1 to S12 and R1 to R10. Gives {:accept? :why :state}, and `:under`, the
  grant that an act that was taken was under. An act taken under a yes for
  one act uses the yes up (R2): its facts are applied first, so an act that
  is the yes's own end ends it as well."
  [state thing]
  (let [{:keys [who permission facts]} thing]
    (if-not (:made? state)
      ;; the making act: the first permission, written by its holder
      (if (and (= 3 (count permission)) (= who (first permission)))
        {:accept? true
         :state (-> state
                    (assoc :made? true)
                    (assoc-in [:grants 0] {:holder who :permission permission :parent nil})
                    (assoc-in [:at [:layer who]] 0)
                    (update-in [:given [:layer who]] (fnil conj []) 0)
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
             :state (cond-> (reduce #(apply-fact %1 own %2) state facts)
                      (= :act (first (covers (:permission own)))) (update :used conj (:n own)))}))))))

(defn judge
  "Takes the state, the layer's id, and a thing as it arrived: `:act-id`,
  `:who`, `:permission`, `:session` and `:facts`, each fact with `:e`, `:k`
  and maybe `:v`. Gives {:accept? :why :answer :state}: whether the
  statements take it now, the first reason when they do not, the answer
  that its sender is to be told, {:answer :accepted}, {:answer :refused
  :reason word} or {:answer :not-mine}, and the state after; and `:under`,
  the grant that an act that was taken was under.

  Under an act-id that has an answer (`once`) nothing is decided and
  `:accept?` is false: for the same thing, `:repeat?` is true and `:answer`
  is its first answer; for another thing, `:not-mine?` is true and the
  answer is \"not mine\"."
  [state _layer thing]
  (let [act-id (:act-id thing)
        sent (as-sent thing)
        first-of (get-in state [:answers act-id])]
    (cond
      (and first-of (= sent (:sent first-of)))
      {:accept? false :repeat? true :answer (:answer first-of) :state state}

      first-of
      {:accept? false :not-mine? true :answer {:answer :not-mine} :state state}

      :else
      (let [{:keys [accept? why] :as ruled} (judge-afresh state thing)
            answer (if accept? {:answer :accepted} {:answer :refused :reason why})]
        (-> ruled
            (assoc :answer answer)
            (assoc-in [:state :answers act-id] {:sent sent :answer answer}))))))

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

(defn summary
  "For a trace: what stands in the layer now, by place."
  [state]
  (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
        (keep (fn [[place n]]
                (cond (= :end n) [place :ended]
                      (used? state n) [place :used]
                      (alive? state n) [place (get-in state [:grants n :permission])]
                      :else [place :ended-with-the-one-above])))
        (:at state)))
