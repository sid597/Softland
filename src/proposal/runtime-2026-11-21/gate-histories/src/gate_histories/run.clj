(ns gate-histories.run
  "Generated histories of one lane, in the form of the permission fix, sent
  to a gate under test and held against the rules.

  A history is a list of steps (`gate-histories.histories`). Each thing
  that a step sends is decided by a gate under test and judged by the rules
  (`gate-histories.reference`), and every act that the gate took is held
  against the statement to prove (`gate-histories.statement`). A gate under
  test is one of:

    the rules themselves   the rules as their own gate: nothing can differ,
                           and what is held is that every act that the
                           rules take keeps the statement to prove
    a broken copy          the rules with one rule taken out or one function
                           replaced, for the run only. A run that is worth
                           anything shows a break under each
    today's store          the store before the fix, each thing said in
                           today's form (`gate-histories.today`). Each
                           difference must fall under a reason of the
                           allowlist
    today's store, with    the same, with one function of the store
    one rule broken        replaced for the run only. Each must show a
                           difference outside the allowlist's reasons

  The store of the permission fix is one more (`gate-histories.fix`, the
  builder's): a gate is a map of four functions (`rules-gate` shows the
  shape), and nothing else here knows which gate it plays. `gate-histories.today` is loaded only
  when it is asked for, since it calls the store before the fix: the rules
  and their broken copies run against no store at all.

  These must hold for every history:

    agree    the gate takes a thing exactly when the rules take it
    told     its sender is told what the rules say she is told: \"accepted\",
             \"refused\", the first answer for the same thing sent again,
             \"not mine\" for another thing under an act-id that has an
             answer
    once     under an act-id that has an answer nothing is decided and
             nothing is written
    apart    a give that was never given is answered apart from one that
             ended: the gate gives no word to both, for an act under such a
             give and for an end of one
    proved   every act that the gate took keeps the statement to prove, and
             nobody is shown the lane whom the statement does not let see
    see      after each decision, a read asked once lets an asker see the
             lane exactly when the rules do: each person, each agent and
             run, one who holds nothing, and one who is not signed in, in
             no sitting and in each

  A thing on which the gate and the rules differ is taken out of its
  history for both, so that the rest is still judged and one history can
  show more than one kind of break. A thing whose answer BUILD.md does not
  settle (`gate-histories.reference/unsettled`), or that today's form
  cannot say, is not sent, and is counted by its word.

  Run it from this folder:

    clojure -M:run                   ; all of the below, 2000 histories each
    clojure -M:run 5000 7            ; so many histories, this seed
    clojure -M:run rules 5000 7      ; the rules as their own gate
    clojure -M:run fix 5000 7        ; against the store of the permission
                                     ; fix (`gate-histories.fix`)
    clojure -M:run fix-mutants 2000 7  ; that store with one rule broken
    clojure -M:run today 5000 7      ; against the store before the fix
    clojure -M:run mutants 3000 7    ; the broken copies of the rules, and
                                     ; today's store with one rule broken;
                                     ; `show` after the seed prints each
                                     ; smallest history
    clojure -M:run sentences         ; the rules in plain words

  And a read that stays open is played against every order in which its
  pushes and its answers can come (`gate-histories.live-read`): the
  reference read, which must keep what every step of a read is held to,
  and its broken copies, each of which must break it.

    clojure -M:run live 2000 7       ; the read that stays open: every order
                                     ; of the small histories, and sampled
                                     ; orders of so many histories
    clojure -M:run live-mutants 2000 7  ; its broken copies

  It exits 1 when a statement broke, a difference fell outside the
  allowlist's reasons, or a broken copy was not caught."
  (:require [clojure.string :as str]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.random :as random]
            [clojure.test.check.rose-tree :as rose]
            [gate-histories.histories :as h]
            [gate-histories.live-read :as live]
            [gate-histories.reference :as reference]
            [gate-histories.statement :as statement]))

(def ^:private lane h/lane)

;; ------------------------------------------------------------ the gates under test

(defn rules-gate
  "The rules as a gate under test, with some of their functions replaced for
  the run: none for the rules as their own gate, one for a broken copy.
  `redefs` is a map from a var of `gate-histories.reference` to what stands
  in its place while this gate decides or reads; the rules that judge are
  not touched.

  The shape of a gate under test:

    :name     what it is, in words
    :profile  the profile of the histories that it is played
    :words?   whether its words of refusal are held apart (`apart`)
    :start    its state before the lane is made
    :send     takes its state, the rules' state, what each give for one
              exact act names, and a thing. Gives {:skip word} for a thing
              that it cannot be sent; else {:verdict, `:accepted` or its
              word; :decided?, false for a thing under an act-id that has
              an answer; :wrote?, whether more than an answer was written;
              :told, the answer that its sender reads; :gs, its state after}
    :sees     takes its state and pairs of asker and sitting; gives for
              each `:not-made`, `:yes` or `:may-not-see`"
  [what redefs]
  {:name what
   :profile :full
   :words? true
   :start reference/start
   :send (fn [gs _ref named sent]
           (let [ruled (with-redefs-fn redefs
                         #(binding [reference/*act-named* named]
                            (reference/judge gs lane sent)))
                 answered? (or (:repeat? ruled) (:not-mine? ruled))]
             {:verdict (cond (:accept? ruled) :accepted
                             answered? :act-id-answered
                             :else (:why ruled))
              :decided? (not answered?)
              :wrote? (not= (dissoc gs :answers) (dissoc (:state ruled) :answers))
              :told (:answer ruled)
              :gs (:state ruled)}))
   :sees (fn [gs pairs]
           (with-redefs-fn redefs
             #(mapv (fn [[asker session]] (reference/may-see gs lane asker session)) pairs)))})

(def ^:private askers
  "Each asker of a read with the sitting that she asks in: one who is not
  signed in; and each who can send, in no sitting and in each."
  (into [[nil nil]]
        (for [who h/authors, session (cons nil h/sittings)] [who session])))

;; ------------------------------------------------------------ playing a history

(def tally "What the histories of a run walked: counted while they play." (atom {}))

(defn- count!
  ([k] (count! k 1))
  ([k n] (swap! tally update k (fnil + 0) n)))

(defn- ends-as
  "Which of the four that may end a give the author of a taken end was, by
  the rules: the first that fits."
  [ref who under target]
  (cond (reference/ends-as-holder? ref who under target) :its-holder
        (reference/ends-as-giver? ref who under target) :its-giver
        (reference/ends-as-lender? ref who under target) :one-it-is-lent-from
        (reference/ends-as-owner? ref who under target) :an-owner
        :else :none))

(defn- count-roads!
  "Counts what a thing that the rules took walked, so that a run says how
  often its histories reached each part of the rules."
  [ref ref-after sent]
  (let [who (:who sent)
        under (reference/give ref (:permission sent))]
    (count! [:under (cond (nil? under) :no-give
                          (contains? reference/everyone (:holder under)) :everyones-give
                          (= :exact-act (first (:covers under))) :a-give-for-one-exact-act
                          (:session under) :a-give-for-one-sitting
                          (:lent-from under) :a-lent-give
                          :else :a-give-that-stands-on-its-own)])
    (when (and under (some #(and (= :give (reference/fact-kind ref %)) (:owner (:allows (:v %)))) (:facts sent))
               (some #(and (= :end (reference/fact-kind ref %)) (= (:e %) (:id under))) (:facts sent)))
      (count! :lanes-handed-over-in-one-act))
    (when (< 1 (count (filter #(= reference/permission-key (:k %)) (:facts sent))))
      (count! :acts-with-several-permission-changes))
    (doseq [fact (:facts sent)
            :let [kind (reference/fact-kind ref fact)
                  v (:v fact)
                  target (reference/give ref (:e fact))]]
      (case kind
        :give (do (count! [:gives (reference/kind-of (:holder v)) (if (:lent-from v) :lent :standing)])
                  (count! [:lent-chain (inc (count (reference/lent-chain ref-after (:e fact))))]))
        :change (count! [:changes (cond (and (reference/allows-within? (:allows v) (:allows target))) :lowered
                                        (reference/allows-within? (:allows target) (:allows v)) :raised
                                        :else :lowered-and-raised)])
        :end (count! [:ends (ends-as ref who under target)
                      (if (reference/stands? ref (:e fact)) :it-stood :it-had-fallen)])
        (count! [:facts kind])))))

(defn- apart-violation
  "The statement `apart` at one decision. A gate must answer a give that
  was never given apart from one that ended: on each of two roads, an act
  under such a give and an end of one, no word of the gate's is given to
  both. Takes the words seen so far, the rules' state before, the thing,
  the rules' judgment and the gate's word. Gives [the words after, nil or
  what broke]."
  [words ref sent ruled word]
  (let [why (:why ruled)
        [road class] (cond
                       (= :give-never-given why) [:an-act-under-it :never-given]
                       (and (= :give-does-not-stand why) (reference/ended? ref (:permission sent)))
                       [:an-act-under-it :ended]
                       (and (= 1 (count (:facts sent))) (= :never-given why)) [:an-end-of-it :never-given]
                       (and (= 1 (count (:facts sent))) (= :already-ended why)) [:an-end-of-it :ended])
        other (case class :never-given :ended :ended :never-given nil)]
    (if (and road (not= :accepted word) (not= :act-id-answered word))
      [(update-in words [road class] (fnil conj #{}) word)
       (when (contains? (get-in words [road other] #{}) word)
         {:statement :apart :class [:apart road word]
          :what (str "the gate answers " (pr-str word) " for " (name road)
                     " when the give was never given and when it had ended")})]
      [words nil])))

(defn- see-violation
  "The statements `see` and `proved` for the read, after one decision. Takes
  the gate, its state and the rules' state after, and the statement's fold.
  Gives {:broke nil or what broke, :excused the reasons under which a
  difference fell}."
  [gate gs ref fold]
  (let [gate-words ((:sees gate) gs askers)]
    (reduce
     (fn [found [[asker session] gate-word]]
       (let [rules-word (reference/may-see ref lane asker session)
             who (if asker (name (reference/kind-of asker)) "one who is not signed in")]
         (cond
           (:broke found) found

           (not= gate-word rules-word)
           (if-let [reason (when-let [f (:see-excuse gate)] (f gs ref asker session gate-word rules-word))]
             (update found :excused conj reason)
             (assoc found :broke
                    {:statement :see :class [:see gate-word rules-word (keyword who)]
                     :what (str "a read lets " (if asker (name asker) "one who is not signed in")
                                (when session (str ", asking in " (name session)))
                                " " (name gate-word) " where the rules say " (name rules-word))}))

           (and (= :yes gate-word) (not (statement/may-be-shown? fold lane asker session)))
           (assoc found :broke
                  {:statement :proved :class [:proved :shown-with-no-give (keyword who)]
                   :what (str (if asker (name asker) "one who is not signed in")
                              " is shown the lane and holds no give that stands and allows see")})

           :else found)))
     {:broke nil :excused #{}}
     (map vector askers gate-words))))

(defn- decide-one
  "Decides one thing by the gate under test and by the rules. Takes the
  gate, the state of a play, the step, the thing and whether it is an
  earlier thing sent again. Gives the state after, with each statement that
  broke added to `:violations`.

  A thing that is not sent changes nothing. When the gate and the rules
  differ on the thing itself, whether it is taken, what its sender is told,
  or whether it was decided at all, the thing is taken out of the history
  for both, whether the difference broke a statement or fell under a reason
  of the allowlist. A break that is not of the thing itself leaves the
  decision in place."
  [gate state op sent again?]
  (let [ref (:ref state)
        named (:named state)
        unsettled (reference/unsettled ref lane sent)
        outcome (when-not unsettled ((:send gate) (:gs state) ref named sent))]
    (cond
      unsettled (do (count! [:not-sent unsettled]) state)
      (:skip outcome) (do (count! [:not-today (:skip outcome)]) state)
      :else
      (let [ruled (binding [reference/*act-named* named] (reference/judge ref lane sent))
            word (:verdict outcome)
            taken? (= :accepted word)
            answered? (boolean (or (:repeat? ruled) (:not-mine? ruled)))
            differ? (and (not answered?) (not= taken? (boolean (:accept? ruled))))
            reason (when-let [f (and differ? (:excuse gate))] (f (:gs state) ref sent ruled outcome))
            proved (when (and taken? (not answered?)) (statement/taken (:fold state) lane sent))
            forbidden (first (:broke proved))
            told-differs? (not= (:answer (:answer ruled)) (:answer (:told outcome)))
            told-broke (when told-differs?
                         {:statement :told
                          :class [:told (cond (:repeat? ruled) :the-same-thing-again
                                              (:not-mine? ruled) :another-thing
                                              :else :decided)
                                  (:answer (:answer ruled)) (:answer (:told outcome))]
                          :what (str "the rules tell its sender " (pr-str (:answer ruled))
                                     ", and the gate tells her " (pr-str (:told outcome)))})
            thing-broke
            (cond
              reason nil

              answered?
              (if (or (:decided? outcome) (:wrote? outcome))
                {:statement :once
                 :class [:once (if (:repeat? ruled) :the-same-thing-again :another-thing)]
                 :what (str (if (:repeat? ruled) "the same thing" "another thing")
                            " under an act-id that has an answer was decided, or wrote")}
                told-broke)

              differ?
              (if taken?
                {:statement :agree
                 :class [:the-gate-took (:why ruled) (or forbidden :not-forbidden-by-the-statement)]
                 :what (str "the gate took what the rules refuse (" (name (:why ruled)) ")"
                            (when forbidden (str "; the statement to prove forbids it: " (name forbidden))))}
                {:statement :agree
                 :class [:the-gate-refused word]
                 :what (str "the gate refused what the rules take; its word is " (pr-str word))})

              :else told-broke)
            out? (boolean (or reason thing-broke))
            line {:step (:n state) :op (:op op) :sent sent
                  :kinds (mapv #(reference/fact-kind ref %) (:facts sent))
                  :verdict word :told (:told outcome)
                  :rules (select-keys ruled [:accept? :why :repeat? :not-mine? :answer])}]
        (count! :things)
        (when again? (count! :sent-again))
        (when (:decided? outcome) (count! :decisions))
        (when-not answered?
          (if (:accept? ruled) (count! :taken-by-the-rules) (count! [:refused (:why ruled)])))
        (when answered? (count! [:act-ids (if (:repeat? ruled) :the-same-thing-again :another-thing)]))
        (if out?
          (do (when reason (count! [:excused reason]))
              (when thing-broke (count! :taken-out))
              (cond-> (update state :steps conj (cond-> (assoc line :taken-out? true)
                                                  reason (assoc :excused reason)
                                                  thing-broke (assoc :broke thing-broke)))
                thing-broke (update :violations conj (assoc thing-broke :step (:n state)))))
          (let [ref-after (:state ruled)
                gs-after (:gs outcome)
                fold-after (if proved (:fold proved) (:fold state))
                [words apart-broke] (if (and (:words? gate) (not answered?))
                                      (apart-violation (:words state) ref sent ruled word)
                                      [(:words state) nil])
                seen (when (or taken? (:wrote? outcome)) (see-violation gate gs-after ref-after fold-after))
                broke (or (when forbidden
                            {:statement :proved :class [:proved forbidden]
                             :what (str "the rules and the gate took an act that the statement to prove forbids: "
                                        (name forbidden))})
                          apart-broke
                          (:broke seen))]
            (when (and (:accept? ruled) (not answered?)) (count-roads! ref ref-after sent))
            (doseq [reason (:excused seen)] (count! [:excused-in-a-read reason]))
            (-> state
                (assoc :ref ref-after :gs gs-after :fold fold-after :words words)
                (cond-> taken? (update :pools h/pools-after sent))
                (update :things conj sent)
                (update :steps conj (cond-> line broke (assoc :broke broke)))
                (cond-> broke (update :violations conj (assoc broke :step (:n state)))))))))))

(defn play
  "Plays a history against a gate under test. Gives {:steps :violations}:
  each thing that was sent with what the gate and the rules said, and each
  statement that broke, in order. A violation holds `:class`, the kind of
  break, and `:step`."
  [gate ops]
  (loop [state {:ref reference/start :gs (:start gate) :fold statement/start
                :pools h/no-pools :named {} :words {}
                :things [] :steps [] :violations [] :n 0}
         todo (cons {:op :make} ops)]
    (if (empty? todo)
      (select-keys state [:steps :violations])
      (let [op (first todo)
            ;; a pinned step keeps the number that it had (`tighter`)
            state (if-let [k (:k op)] (assoc state :n k) state)
            {:keys [sends entries named again?]} (h/things-of state op)
            state (cond-> state
                    (seq entries) (update-in [:pools :yeses] into entries)
                    (seq named) (update :named merge named))
            state (reduce (fn [state sent] (decide-one gate state op sent again?)) state sends)]
        (recur (update state :n inc) (rest todo))))))

(defn- kinds-in
  "The kinds of break of a played history."
  [played]
  (into #{} (map :class) (:violations played)))

;; ------------------------------------------------------------ printing a play

(defn- party-line [party]
  (cond (nil? party) "-"
        (reference/person? party) (subs (name party) 7)
        :else (name party)))

(defn- allows-line
  "What a give allows, as a word: the preset's when it is one."
  [allows]
  (or (some (fn [[word a]] (when (= a allows) (name word))) h/allows-of)
      (pr-str allows)))

(defn- value-line [v]
  (str (allows-line (:allows v))
       (when (= :exact-act (first (:covers v))) " for one exact act")
       (when (not= lane (second (:covers v)))
         (when (= :default-group (first (:covers v))) (str " over " (name (second (:covers v))))))
       (when (:session v) (str " in " (name (:session v))))
       (if (:lent-from v) (str ", lent from " (h/id-mark (:lent-from v))) ", standing on its own")))

(defn- fact-line [kind {:keys [e k v]}]
  (case kind
    :give (str "gives " (h/id-mark e) " to " (party-line (:holder v)) ": " (value-line v))
    :change (str "changes " (h/id-mark e) " (" (party-line (:holder v)) ") to " (value-line v))
    :end (str "ends " (h/id-mark e))
    :setting (str "sets " (name k))
    :depot "writes the depot"
    (str "writes " (name e) " " (name k))))

(defn- print-trace
  "Prints a played history, one thing a line: the step, who sent it, in
  which sitting, under which act-id and give, what it says; what the gate
  did, what its sender is told, and what the rules say. A step at which a
  statement broke is marked; a thing that was taken out of the history says
  so. Then, for the kind given, the first step at which it broke and what
  broke; for no kind, the first break."
  ([played] (print-trace played nil))
  ([{:keys [steps violations]} kind]
   (doseq [{:keys [step sent kinds verdict told rules broke taken-out? excused]} steps]
     (println (format "  %3d  %-9s %-5s %-6s under %-5s %s"
                      step (party-line (:who sent)) (name (:session sent))
                      (h/act-id-mark (:act-id sent))
                      (if-let [p (:permission sent)] (h/id-mark p) "-")
                      (str/join "; " (map fact-line kinds (:facts sent)))))
     (println (format "         gate: %-40s told: %-9s rules: %s%s"
                      (if (keyword? verdict) (name verdict) (pr-str verdict))
                      (name (:answer told))
                      (cond (:repeat? rules) (str "the same thing again: " (name (:answer (:answer rules))))
                            (:not-mine? rules) "another thing under a used act-id: not mine"
                            (:accept? rules) "taken"
                            :else (name (:why rules)))
                      (cond excused (str "   <- differs, under " excused "; taken out of the history")
                            (and broke taken-out?) (str "   <- " (name (:statement broke)) " broke; taken out of the history")
                            broke (str "   <- " (name (:statement broke)) " broke")
                            :else ""))))
   (when-let [v (if kind (first (filter #(= kind (:class %)) violations)) (first violations))]
     (println)
     (println "  BROKEN at step" (:step v) "-" (name (:statement v)) ":" (:what v)))))

(defn- class-line
  "A kind of break in words."
  [[what & more :as class]]
  (case what
    :the-gate-took (str "agree: the gate takes what the rules refuse; the rules' word is " (name (first more))
                        (if (= :not-forbidden-by-the-statement (second more))
                          ""
                          (str "; the statement to prove forbids it too: " (name (second more)))))
    :the-gate-refused (str "agree: the gate refuses what the rules take; the gate's word is " (pr-str (first more)))
    :told (let [[which says gate] more]
            (str "told: " (case which
                            :the-same-thing-again "the same thing sent again"
                            :another-thing "another thing under an act-id that has an answer"
                            :decided "a thing that was decided")
                 ": the rules tell its sender " (name says) " and the gate tells her " (name gate)))
    :once (str "once: under an act-id that has an answer, "
               (case (first more) :the-same-thing-again "the same thing sent again" "another thing")
               " was decided, or wrote")
    :apart (str "apart: the gate answers " (pr-str (second more)) " for " (name (first more))
                " both when the give was never given and when it had ended")
    :proved (str "proved: the statement to prove is broken: " (name (first more))
                 (when (second more) (str ", for " (name (second more)))))
    :see (let [[gate rules who] more]
           (str "see: a read says " (name gate) " where the rules say " (name rules) ", for " (name who)))
    (pr-str class)))

;; --------------------------------------------------------- growing and cutting

(defn- sampled
  "The histories of a run, as rose trees: grown as test.check's quick-check
  grows them, from the seed, at the sizes 0 to 79 again and again, one
  split of the random number for each. Lazy."
  [profile histories seed]
  (let [history (h/gen-history profile)]
    (letfn [(more [i sizes rnd]
              (lazy-seq
               (when (< i histories)
                 (let [[r1 r2] (random/split rnd)]
                   (cons (gen/call-gen history r1 (first sizes))
                         (more (inc i) (rest sizes) r2))))))]
      (more 0 (gen/make-size-range-seq 80) (random/make-random seed)))))

(defn- kinds-of-break
  "Plays the histories of a run against a gate. Gives, for each kind of
  break that came up, {:count in how many histories, :at the first of them,
  :tree its rose tree}. Exits 2, with the history, when the judge throws."
  [gate histories seed]
  (reduce
   (fn [found [i tree]]
     (let [played (try (play gate (rose/root tree))
                       (catch Throwable e
                         (println "THE JUDGE THREW at history" (inc i) ":" (.getMessage e))
                         (println (pr-str (rose/root tree)))
                         (.printStackTrace e)
                         (System/exit 2)))]
       (reduce (fn [found kind]
                 (-> found
                     (update-in [kind :count] (fnil inc 0))
                     (update kind #(if (:tree %) % (assoc % :at (inc i) :tree tree)))))
               found
               (kinds-in played))))
   {}
   (map-indexed vector (sampled (:profile gate) histories seed))))

(defn- shrunk
  "The smallest history under a rose tree that still breaks in one way, by
  test.check's own walk of the tree."
  [gate tree kind]
  (let [breaks? (fn [ops] (contains? (kinds-in (play gate ops)) kind))]
    (loop [nodes (rose/children tree) smallest (rose/root tree)]
      (if (empty? nodes)
        smallest
        (let [head (first nodes)]
          (if (breaks? (rose/root head))
            (if-let [children (seq (rose/children head))]
              (recur children (rose/root head))
              (recur (rest nodes) (rose/root head)))
            (recur (rest nodes) smallest)))))))

(defn- tighter
  "A history cut down further than test.check cuts it. A step's act-ids and
  the ids of its gives are made of where it stands, so taking a step out
  moves the others. This pins each step to its number, then takes out each
  step that the break does not need, one at a time, until none can be
  taken out. `breaks?` takes a history and says whether it still breaks so."
  [ops breaks?]
  (loop [ops (vec (map-indexed (fn [i op] (assoc op :k (inc i))) ops))]
    (if-let [smaller (first (for [i (range (count ops))
                                  :let [without (into (subvec ops 0 i) (subvec ops (inc i)))]
                                  :when (breaks? without)]
                              without))]
      (recur smaller)
      ops)))

(defn- smallest-of
  "The smallest history that breaks in one way under a gate."
  [gate kind tree]
  (tighter (shrunk gate tree kind) #(contains? (kinds-in (play gate %)) kind)))

;; ----------------------------------------------------------- the broken copies

(defn- without-rule
  "The rules with one taken out."
  [word]
  (fn [rules] (vec (remove #(= word (:word %)) rules))))

(defn- not-of-a-raise
  "The rules with one that is asked of a give and of a raise asked of a give
  alone."
  [word]
  (fn [rules] (mapv #(if (= word (:word %)) (assoc % :of :give) %) rules)))

(def ^:private dropped
  "A broken copy for each rule that stands alone: the rules without it."
  (vec (for [rule reference/rules
             :when (not (false? (:alone? rule)))]
         [(str "without the rule " (name (:word rule)) ": " (:says rule))
          {#'reference/rules (without-rule (:word rule))}])))

(def ^:private replaced
  "The broken copies that replace a function of the rules, each as [what,
  {a var, a function that takes what is in place and gives the broken
  one}]."
  [["BUILD 2.1: a lent give still stands after the give it is lent from was ended"
    {#'reference/stands?
     (fn [_] (fn [state id] (and (some? (reference/give state id)) (not (reference/ended? state id)))))}]
   ["BUILD 2.1: a give that stands on its own falls when the give it was made under is ended (batch 1's value kept: every give lent from the give above)"
    {#'reference/stands?
     (fn [real]
       (fn [state id]
         (and (real state id)
              (every? #(not (reference/ended? state %))
                      (take 64 (take-while some? (iterate #(:under (reference/give state %))
                                                          (:under (reference/give state id)))))))))}]
   ["BUILD 3.1: an end ends nothing: an act under a give that was ended is taken"
    {#'reference/ended? (fn [_] (fn [_state _id] false))}]
   ["BUILD 2.1, 3.3 (bug 2): what is lent keeps what it allows when its lender is lowered"
    {#'reference/meet-allows (fn [_] (fn [mine _lenders] mine))}]
   ["BUILD 3.3 (bug 2): lowering someone ends what she lent"
    {#'reference/apply-fact
     (fn [real]
       (fn [state thing fact]
         (let [after (real state thing fact)
               was (reference/give state (:e fact))]
           (if (and (= :change (reference/fact-kind state fact))
                    (not (reference/allows-within? (:allows was) (:allows (:v fact)))))
             (update after :ended into (keep (fn [[id g]] (when (= (:e fact) (:lent-from g)) id)) (:gives state)))
             after))))}]
   ["BUILD 2.4: passing see or write to another person needs no right to give"
    {#'reference/may-pass? (fn [real] (fn [held allows _to] (real held allows :agent.any)))}]
   ["BUILD 2.5 (item 19): lending see or write to an agent or a run needs the right to give"
    {#'reference/may-pass? (fn [real] (fn [held allows _to] (real held allows :person.any)))}]
   ["BUILD 2.4: the right to give does not say what may be given on: whoever may give anything gives everything she holds"
    {#'reference/may-pass?
     (fn [real]
       (fn [held allows to]
         (real (cond-> held (seq (:give held)) (assoc :give #{:see :write :give :owner :everyone})) allows to)))}]
   ["BUILD 2.4 (item 2): a give or a raise passes on what its author does not hold"
    {#'reference/may-pass? (fn [_] (fn [_held allows _to] (boolean (seq allows))))}]
   ["item 26 (Option B): the right to give to everyone is passed on by one whose own right to give does not name it"
    {#'reference/may-pass?
     (fn [real]
       (fn [held allows to]
         (let [named (disj (:give allows #{}) :everyone)]
           (real held (cond (not (contains? (:give allows #{}) :everyone)) allows
                            (seq named) (assoc allows :give named)
                            :else (dissoc allows :give))
                 to))))}]
   ["item 26 (Option B): whoever may give see or write to a named person gives it to everyone too (Option A)"
    {#'reference/may-give-to? (fn [_] (fn [_held _to] true))}]
   ["item 26 (Option B): inviting a named person needs the right to give to everyone"
    {#'reference/may-give-to?
     (fn [_] (fn [held to] (or (reference/borrower? to) (contains? (:give held #{}) :everyone))))}]
   ["item 26: a giver who was lowered in place still ends, and lowers, what she gave (the store before the fix of decision.clj:348)"
    {#'reference/ends-as-giver?
     (fn [_]
       (fn [_state who under target]
         (boolean (and under (= who (:given-by target)) (= (:id under) (:under target))))))}]
   ["item 26: whoever gave a give ends it though she could not give it to everyone any more: her give is held to the rights and not to the holder"
    {#'reference/gives-it-still?
     (fn [_]
       (fn [state under target what]
         (or (empty? what)
             (reference/may-pass? (:allows (reference/holds-now state (:id under))) what (:holder target)))))}]
   ["reading 4, as it was: one further up a chain of lent gives ends a give past an ended give, which no row says she is above"
    {#'reference/ends-as-lender?
     (fn [_]
       (fn [state who under target]
         (boolean (and under
                       (= who (:holder under))
                       (some #{(:id under)} (reference/lent-chain state (:id target)))))))}]
   ["reading 5, as it was: a raise is held to what its author may pass on and to nothing else that a give is held to"
    {#'reference/rules
     (fn [rules]
       (mapv #(if (set? (:of %)) (update % :of disj :raise) %) rules))}]
   ["reading 5: a lent give is raised under another give than the one it is lent from"
    {#'reference/rules (not-of-a-raise :lent-from-the-give-it-is-made-under)}]
   ["reading 5: a give that stands on its own is raised under a lent give, and what is added outlives the lender"
    {#'reference/rules (not-of-a-raise :made-under-a-lent-give-is-lent)}]
   ["reading 5: a give good in every sitting is raised under a give good for one"
    {#'reference/rules (not-of-a-raise :covers-more-than-the-give-above)}]
   ["item 26 (Option B): everyone's give is raised by one whose right to give does not name everyone"
    {#'reference/rules (not-of-a-raise :opens-to-everyone-without-the-right)}]
   ["reading 6, as it was: anyone at all is held to seeing where the give is made and not at a change"
    {#'reference/rules
     (fn [rules]
       (mapv #(if (= :anyone-only-sees (:word %)) (assoc % :of :give) %) rules))}]
   ["BUILD 1.4 (bug 4): a holder who may only see cannot end her own give"
    {#'reference/ends-as-holder?
     (fn [real]
       (fn [state who under target]
         (and (real state who under target)
              (boolean (:write (:allows (when under (reference/holds-now state (:id under)))))))))}]
   ["BUILD 2.6 (item 18): whoever gave a give cannot end it"
    {#'reference/ends-as-giver? (fn [_] (fn [_ _ _ _] false))}]
   ["BUILD 2.6 (item 18): the holder of a give that another is lent from cannot end the other"
    {#'reference/ends-as-lender? (fn [_] (fn [_ _ _ _] false))}]
   ["BUILD 2.6 (item 18): an owner cannot end a give that she neither gave nor lent"
    {#'reference/ends-as-owner? (fn [_] (fn [_ _ _ _] false))}]
   ["the clarification on \"holds\": whoever is covered by everyone ends what is lent from everyone's give"
    {#'reference/ends-as-lender?
     (fn [_]
       (fn [state who under target]
         (boolean (and under
                       (reference/covers-party? (:holder under) who)
                       (some #{(:id under)} (reference/lent-chain state (:id target)))))))}]
   ["BUILD 3.5 (item 21): the lane's first give never ends, though another owner remains (batch 1's guard kept)"
    {#'reference/may-end?
     (fn [real] (fn [state who under target] (and (not= 0 (:n target)) (real state who under target))))}]
   ["BUILD 3.5 (item 21): a lent owner's give counts as the lane's owner"
    {#'reference/has-owner?
     (fn [_]
       (fn [state lane]
         (boolean (some (fn [[id g]]
                          (and (:owner (:allows g)) (= [:default-group lane] (:covers g))
                               (nil? (:session g)) (reference/stands? state id)))
                        (:gives state)))))}]
   ["BUILD 3.1 (item 7): an act that is refused keeps the changes of its that were good"
    {#'reference/judge-afresh
     (fn [real]
       (fn [state lane thing]
         (let [ruled (real state lane thing)]
           (if (or (:accept? ruled) (not (:made? state)))
             ruled
             (assoc ruled :state
                    (reduce (fn [kept fact]
                              (if (:accept? (real state lane (assoc thing :facts [fact])))
                                (@#'reference/apply-fact kept thing fact)
                                kept))
                            state
                            (:facts thing)))))))}]
   ["BUILD 3.1: each change of an act is checked against what the act's earlier facts left, not against what its author held just before the act"
    {#'reference/judge-afresh
     (fn [real]
       (fn [state lane thing]
         (if (or (not (:made? state)) (< (count (:facts thing)) 2))
           (real state lane thing)
           (loop [now state facts (:facts thing)]
             (if (empty? facts)
               {:accept? true :state now}
               (let [ruled (real now lane (assoc thing :facts [(first facts)]))]
                 (if (:accept? ruled)
                   (recur (:state ruled) (rest facts))
                   (assoc ruled :state state))))))))}]
   ["BUILD 4.1 (item 20): an agent or a run acts and sees as everyone on its own"
    {#'reference/covers-party?
     (fn [_]
       (fn [holder party]
         (boolean (or (and (some? party) (= holder party))
                      (= :anyone holder)
                      (and (= :anyone-signed-in holder) (some? party))))))}]
   ["BUILD 4.1 (item 20): one who is not signed in holds what anyone signed in holds"
    {#'reference/covers-party?
     (fn [real] (fn [holder party] (or (real holder party) (and (nil? party) (= :anyone-signed-in holder)))))}]
   ["the read: a holder still sees after her give, or a give above it, was ended or lowered"
    {#'reference/sees-under?
     (fn [_]
       (fn [state lane asker session id]
         (let [g (reference/give state id)]
           (boolean (and g
                         (reference/covers-party? (:holder g) asker)
                         (:see (:allows g))
                         (= [:default-group lane] (:covers g))
                         (or (nil? (:session g)) (= session (:session g))))))))}]
   ["the read: a give good for one sitting lets its holder see in any sitting"
    {#'reference/sees-under?
     (fn [real]
       (fn [state lane asker session id]
         (real state lane asker (or (:session (reference/give state id)) session) id)))}]
   ["the read: a give that allows write and not see lets its holder see"
    {#'reference/sees-under?
     (fn [_]
       (fn [state lane asker session id]
         (let [held (reference/holds-now state id)]
           (boolean (and held
                         (reference/covers-party? (:holder (reference/give state id)) asker)
                         (seq (:allows held))
                         (= [:default-group lane] (:covers held))
                         (or (nil? (:session held)) (= session (:session held))))))))}]
   ["the read: a give that covers one exact act lets its holder see the lane"
    {#'reference/sees-under?
     (fn [_]
       (fn [state _lane asker session id]
         (let [held (reference/holds-now state id)]
           (boolean (and held
                         (reference/covers-party? (:holder (reference/give state id)) asker)
                         (:see (:allows held))
                         (or (nil? (:session held)) (= session (:session held))))))))}]
   ["BUILD 1.3 (bug 3): a give that was never given is answered as one that ended"
    {#'reference/rules
     (fn [rules]
       (mapv #(case (:word %)
                :give-never-given (assoc % :word :give-does-not-stand)
                :never-given (assoc % :word :already-ended)
                %)
             rules))}]
   ["BUILD 1.2 (bug 1): a holder has one give on a lane: a give to a holder whose give stands is refused"
    {#'reference/rules
     (fn [rules]
       (conj rules
             {:of :give :word :holder-has-a-give
              :refuses? (fn [{:keys [state v]}]
                          (boolean (some (fn [[id g]]
                                           (and (= (:holder g) (:holder v))
                                                (= (:covers g) (:covers v))
                                                (= (:session g) (:session v))
                                                (reference/stands? state id)))
                                         (:gives state))))}))}]
   ["one answer for an act-id: another thing under an act-id that has an answer is decided"
    {#'reference/judge
     (fn [real]
       (fn [state lane thing]
         (let [first-of (get-in state [:answers (:act-id thing)])]
           (if (and first-of (not= (reference/as-sent thing) (:sent first-of)))
             (real (update state :answers dissoc (:act-id thing)) lane thing)
             (real state lane thing)))))}]])

(def ^:private mutants "Every broken copy." (into dropped replaced))

(defn- run-mutants
  "Plays the histories against each broken copy. Gives true when every one
  was caught. Prints, for each, the history at which a statement first
  broke and the kind of break; with `show?`, its smallest history too."
  [histories seed show?]
  (println (format "THE BROKEN COPIES: %d, each played up to %d histories, seed %d" (count mutants) histories seed))
  (let [missed
        (reduce
         (fn [missed [what replaced]]
           (let [redefs (into {} (map (fn [[the-var make]] [the-var (make @the-var)])) replaced)
                 gate (rules-gate what redefs)
                 result (tc/quick-check histories
                                        (prop/for-all [ops (h/gen-history :full)]
                                          (empty? (:violations (play gate ops))))
                                        :seed seed :max-size 80)]
             (if (:pass? result)
               (do (println (format "  NOT CAUGHT   %s" what)) (conj missed what))
               (let [smallest (first (get-in result [:shrunk :smallest]))
                     smallest (tighter smallest #(seq (:violations (play gate %))))
                     played (play gate smallest)
                     kind (:class (first (:violations played)))]
                 (println (format "  caught at %5d  %s" (:num-tests result) what))
                 (println (format "                   by %s" (class-line kind)))
                 (when show?
                   (print-trace played kind)
                   (println))
                 missed))))
         []
         mutants)]
    (println (format "%d of %d broken copies caught" (- (count mutants) (count missed)) (count mutants)))
    (empty? missed)))

;; --------------------------------------------------------------------- the runs

(defn- counted-under
  "The entries of the tally whose key is a vector that begins with `word`,
  as a sorted map from the rest of the key to the count."
  [counted word]
  (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
        (keep (fn [[k n]] (when (and (vector? k) (= word (first k)))
                            [(if (= 2 (count k)) (second k) (subvec k 1)) n])))
        counted))

(defn- print-counts
  "Prints what the histories of a run walked, from the tally."
  [counted]
  (println (format "%d things sent, %d of them an earlier thing again; %d decisions of the gate; the rules took %d and refused %d"
                   (:things counted 0) (:sent-again counted 0) (:decisions counted 0)
                   (:taken-by-the-rules counted 0)
                   (reduce + (vals (counted-under counted :refused)))))
  (println "taken by the rules, by the give that the act named:" (counted-under counted :under))
  (println "gives taken, by holder and whether lent:" (counted-under counted :gives))
  (println "gives taken, by how many gives their chain holds:"
           (let [chains (counted-under counted :lent-chain)]
             (into (sorted-map) (map (fn [[k n]] [k n])) chains)))
  (println "changes taken:" (counted-under counted :changes))
  (println "ends taken, by which of the four ended and whether the give stood:" (counted-under counted :ends))
  (println (format "acts taken with several permission changes: %d; lanes handed over in one act: %d"
                   (:acts-with-several-permission-changes counted 0) (:lanes-handed-over-in-one-act counted 0)))
  (println "other facts taken:" (counted-under counted :facts))
  (println "under an act-id that has an answer:" (counted-under counted :act-ids))
  (println "refused by the rules, by the rule's word:")
  (doseq [[word n] (sort-by (comp - val) (counted-under counted :refused))]
    (println (format "  %8d  %s" n (name word))))
  (let [never (remove (set (keys (counted-under counted :refused))) (map :word reference/rules))]
    (println "rules that refused nothing in this run:" (if (empty? never) "none" (str/join ", " (map name never)))))
  (println "not sent, because BUILD.md does not settle the answer:"
           (let [m (counted-under counted :not-sent)] (if (empty? m) "none" m))))

(defn- print-breaks
  "Prints each kind of break that a run found, with its smallest history."
  [gate found histories]
  (let [in-order (sort-by (comp - :count val) found)]
    (println (format "BROKEN: %d kinds of break. In how many of the %d histories each came up, and the first:"
                     (count found) histories))
    (doseq [[kind {:keys [count at]}] in-order]
      (println (format "  %6d  %6d  %s" count at (class-line kind))))
    (doseq [[kind {:keys [count at tree]}] in-order]
      (println)
      (println "KIND" (pr-str kind))
      (println " " (class-line kind))
      (println (format "  in %d histories; first at history %d. The smallest history that breaks this way:" count at))
      (print-trace (play gate (smallest-of gate kind tree)) kind))))

(defn- run-rules
  "The rules as their own gate. Gives true when no statement broke."
  [histories seed]
  (reset! tally {})
  (let [gate (rules-gate "the rules themselves" {})
        started (System/nanoTime)
        found (kinds-of-break gate histories seed)
        counted @tally]
    (println (format "THE RULES AS THEIR OWN GATE: %d histories, seed %d, %.1f seconds"
                     histories seed (/ (- (System/nanoTime) started) 1e9)))
    (print-counts counted)
    (if (empty? found)
      (do (println "HOLDS: every act that the rules take keeps the statement to prove, and nobody is shown the lane whom it does not let see (proved); agree, told, once, apart and see hold of the rules against themselves")
          true)
      (do (print-breaks gate found histories) false))))

(defn- todays
  "A var of `gate-histories.today`, which is loaded only when it is asked
  for: it calls the store before the fix, and does not load against a
  store of another form. Gives nil, and says why, when it does not load."
  [sym]
  (try @(requiring-resolve (symbol "gate-histories.today" (name sym)))
       (catch Throwable e
         (println "NOT RUN against the store before the fix: gate-histories.today does not load against the store on the path."
                  "It needs the store as it was before the permission fix (the README says how to put an export of it on the path).")
         (println "  " (.getMessage e))
         nil)))

(defn- run-broken-stores
  "Today's store with one of its own rules broken, through the saying again
  in today's form. Gives true when every one showed a difference outside
  the allowlist's reasons that the store without it does not show."
  [histories seed show?]
  (let [gate (todays 'gate)
        broken-stores (todays 'broken-stores)
        breaks (fn [played] (into #{} (map (juxt :step :class)) (:violations played)))]
    (println (format "THE STORE BEFORE THE FIX WITH ONE RULE BROKEN: %d, each played up to %d histories, seed %d"
                     (count broken-stores) histories seed))
    (let [missed
          (reduce
           (fn [missed [what the-var make-broken]]
             (let [redefs {the-var (make-broken @the-var)}
                   new-breaks (fn [ops]
                                (let [real (play gate ops)
                                      broken (with-redefs-fn redefs #(play gate ops))]
                                  (remove (breaks real) (breaks broken))))
                   result (tc/quick-check histories
                                          (prop/for-all [ops (h/gen-history :today)] (empty? (new-breaks ops)))
                                          :seed seed :max-size 80)]
               (if (:pass? result)
                 (do (println (format "  NOT CAUGHT   %s" what)) (conj missed what))
                 (let [smallest (tighter (first (get-in result [:shrunk :smallest])) #(seq (new-breaks %)))
                       kind (second (first (new-breaks smallest)))]
                   (println (format "  caught at %5d  %s" (:num-tests result) what))
                   (println (format "                   by %s" (class-line kind)))
                   (when show?
                     (print-trace (with-redefs-fn redefs #(play gate smallest)) kind)
                     (println))
                   missed))))
           []
           broken-stores)]
      (println (format "%d of %d caught" (- (count broken-stores) (count missed)) (count broken-stores)))
      (empty? missed))))

(defn- run-today
  "The store before the fix, on today's default values. Gives true when
  every difference fell under a reason of the allowlist."
  [histories seed]
  (reset! tally {})
  (let [gate (todays 'gate)
        reasons (todays 'reasons)
        started (System/nanoTime)
        found (kinds-of-break gate histories seed)
        counted @tally
        excused (counted-under counted :excused)
        excused-reads (counted-under counted :excused-in-a-read)]
    (println (format "AGAINST THE STORE BEFORE THE FIX, on today's default values: %d histories, seed %d, %.1f seconds"
                     histories seed (/ (- (System/nanoTime) started) 1e9)))
    (print-counts counted)
    (println "not sent, because today's form cannot say it:")
    (doseq [[word n] (sort-by (comp - val) (counted-under counted :not-today))]
      (println (format "  %8d  %s" n (name word))))
    (println)
    (println (format "COMPARED: %d things, each decided by today's store and judged by the rules; and after each that wrote, a read asked once for each of %d askers"
                     (:things counted 0) (count askers)))
    (println "DIFFERENCES INSIDE THE ALLOWLIST'S REASONS, by reason:")
    (doseq [[reason says] (sort-by key reasons)]
      (println (format "  %8d  %-10s  %s" (get excused reason 0) reason says)))
    (println (format "  decisions after which a read differed under B4 public (an agent or a run seeing as everyone): %d"
                     (get excused-reads "B4 public" 0)))
    (if (empty? found)
      (do (println "DIFFERENCES OUTSIDE THE ALLOWLIST'S REASONS: none. agree, told, once and see hold everywhere else; every act that today's store took keeps the statement to prove")
          true)
      (do (println (format "DIFFERENCES OUTSIDE THE ALLOWLIST'S REASONS: %d kinds" (count found)))
          (print-breaks gate found histories)
          false))))

(defn- the-fix
  "A var of `gate-histories.fix`, which is loaded only when it is asked
  for: it calls the store of the permission fix, and does not load against
  the store before it. Gives nil, and says why, when it does not load."
  [sym]
  (try @(requiring-resolve (symbol "gate-histories.fix" (name sym)))
       (catch Throwable e
         (println "NOT RUN against the store of the permission fix: gate-histories.fix does not load against the store on the path.")
         (println "  " (.getMessage e))
         nil)))

(defn- run-fix
  "The store of the permission fix as a gate under test. Gives true when no
  statement broke. A difference that falls under one of the judge's
  readings on which the store differs is counted and printed, as a
  question."
  [histories seed]
  (reset! tally {})
  (let [gate (the-fix 'gate)
        questions (the-fix 'questions)
        started (System/nanoTime)
        found (kinds-of-break gate histories seed)
        counted @tally
        asked (counted-under counted :excused)]
    (println (format "AGAINST THE STORE OF THE PERMISSION FIX: %d histories, seed %d, %.1f seconds"
                     histories seed (/ (- (System/nanoTime) started) 1e9)))
    (print-counts counted)
    (println "not sent, because the store's declaration refuses its form, which the rules do not hold:"
             (let [m (counted-under counted :not-today)] (if (empty? m) "none" m)))
    (println)
    (println (format "COMPARED: %d things, each decided by the store's own walk and judged by the rules; and after each that wrote, a read asked once for each of %d askers"
                     (:things counted 0) (count askers)))
    (println "QUESTIONS: things on which the store differs under a reading of the judge's, taken out of their histories, by reading:")
    (doseq [[reading says] (sort-by key questions)]
      (println (format "  %8d  %-10s  %s" (get asked reading 0) reading says)))
    (doseq [[reading n] (counted-under counted :excused-in-a-read)]
      (println (format "  decisions after which a read differed under %s: %d" reading n)))
    (if (empty? found)
      (do (println "HOLDS everywhere else: agree, told, once, apart, proved and see")
          true)
      (do (print-breaks gate found histories) false))))

(defn- run-broken-fixes
  "The store of the permission fix with one of its own rules broken. Gives
  true when every one broke a statement that the store without it keeps."
  [histories seed show?]
  (let [gate (the-fix 'gate)
        broken-stores (the-fix 'broken-stores)
        breaks (fn [played] (into #{} (map (juxt :step :class)) (:violations played)))
        plays (fn [gate ops] (try (play gate ops)
                                  (catch Throwable e
                                    {:steps [] :violations [{:step 0 :statement :threw :class [:threw (.getSimpleName (class e))]
                                                             :what (str "the store threw: " (.getMessage e))}]})))]
    (println (format "THE STORE OF THE PERMISSION FIX WITH ONE RULE BROKEN: %d, each played up to %d histories, seed %d"
                     (count broken-stores) histories seed))
    (let [missed
          (reduce
           (fn [missed [what the-var make-broken]]
             (let [redefs {the-var (make-broken @the-var)}
                   new-breaks (fn [ops]
                                (let [real (plays gate ops)
                                      broken (with-redefs-fn redefs #(plays gate ops))]
                                  (remove (breaks real) (breaks broken))))
                   result (tc/quick-check histories
                                          (prop/for-all [ops (h/gen-history :full)] (empty? (new-breaks ops)))
                                          :seed seed :max-size 80)]
               (if (:pass? result)
                 (do (println (format "  NOT CAUGHT   %s" what)) (conj missed what))
                 (let [smallest (tighter (first (get-in result [:shrunk :smallest])) #(seq (new-breaks %)))
                       kind (second (first (new-breaks smallest)))]
                   (println (format "  caught at %5d  %s" (:num-tests result) what))
                   (println (format "                   by %s" (class-line kind)))
                   (when show?
                     (print-trace (with-redefs-fn redefs #(plays gate smallest)) kind)
                     (println))
                   missed))))
           []
           broken-stores)]
      (println (format "%d of %d caught" (- (count broken-stores) (count missed)) (count broken-stores)))
      (empty? missed))))

;; --------------------------------------------------------------- the live read

(def ^:private questions-of-a-read
  "The kinds of break of a read that are a question and not a break of the
  read under test: each follows from what the pages give, and no read can
  mend it. A run counts them and prints the first order that shows each."
  {:came-back "a run of gives says again what it said before (CONTRACT 2.4: the key is gone at an end), so an old answer agrees again with pushes that say different versions, and the read tells a level of that old version"
   :fact-after "the push of the lane's facts runs ahead of the rights pushes, and the read tells a fact that was written after the last version at which she could see"})

(defn- red-of
  "Of what broke, what is a break of the read under test."
  [broke]
  (remove #(contains? questions-of-a-read (first (:class %))) broke))

(def ^:private gen-reader
  "One read to open: an asker, or one who is not signed in; in no sitting
  or in one; of what she may see, or of the lane's acts."
  (gen/fmap (fn [[asker session question]]
              {:lane lane :routing-id lane :question question :asker asker :session session})
            (gen/tuple (gen/frequency [[1 (gen/return nil)] [8 (gen/elements h/authors)]])
                       (gen/frequency [[2 (gen/return nil)] [1 (gen/elements h/sittings)]])
                       (gen/frequency [[3 (gen/return :see)] [1 (gen/return :acts)]]))))

(def ^:private gen-order
  "A history of up to forty steps, one to four reads, and the choices of
  an order of events."
  (gen/tuple (h/gen-history :full 40)
             (gen/vector gen-reader 1 4)
             (gen/vector (gen/tuple gen/nat gen/nat) 0 300)))

(defn- play-order
  "Plays one sampled order. Gives the world after."
  [[ops readers choices]]
  (live/forget!)
  (live/run-schedule (live/versions-of (live/decisions ops)) readers choices))

(defn- every-order
  "Every order of each small history, for each of its reads. Gives a line
  for each: {:what :reader :worlds :broke}; `:broke` holds each kind of
  break with the first order that showed it, the query against the rules
  among them. With `enough?`, a history is left as soon as that says so of
  the kinds that it showed."
  ([fails] (every-order fails (constantly false)))
  ([fails enough?]
   (for [[what things readers only] live/scenarios
         reader readers]
     (do (live/forget!)
         (let [vs (live/scenario-versions things)
               query (first (keep #(live/agrees-with-the-rules vs % reader) (range 1 (count (:tables vs)))))
               explored (if (or query (= :the-query-alone only))
                          {:worlds 0 :broke {}}
                          (live/explore vs reader fails enough?))]
           {:what what :reader reader :vs vs :worlds (:worlds explored)
            :broke (cond-> (:broke explored) query (assoc (:class query) query))})))))

(defn- reader-word [{:keys [question asker session]}]
  (str (if asker (name asker) "one who is not signed in")
       (when session (str " in " (name session)))
       (if (= :acts question) ", asking of the acts" "")))

(defn- class-of-a-read
  "A kind of break of a read, in words."
  [[held what & more]]
  (str (name held) ": " (str/replace (name what) "-" " ")
       (when (seq more) (str " (" (str/join ", " (map name more)) ")"))))

(defn- print-sampled-break
  "Prints the smallest sampled order that breaks in one way."
  [[ops readers choices] class]
  (let [vs (live/versions-of (live/decisions ops))
        world (live/run-schedule vs readers choices)
        at (:at (first (filter #(= class (:class %)) (:broke world))))]
    (println (format "    the reads: %s" (str/join "; " (map reader-word readers))))
    (live/print-order vs readers (take (or at 0) (:log world)))))

(defn- sample-orders
  "Plays the sampled orders of a run. Gives {:counts what was walked, summed;
  :kinds for each kind of break, in how many histories and the first}."
  [histories seed]
  (let [{:keys [counts kinds]}
        (reduce
         (fn [found [i tree]]
           (let [world (play-order (rose/root tree))]
             (reduce (fn [found class]
                       (-> found
                           (update-in [:kinds class :count] (fnil inc 0))
                           (update-in [:kinds class] #(if (:at %) % (assoc % :at (inc i) :tree tree)))))
                     (update found :counts #(merge-with + % (:counts world)))
                     (distinct (map :class (:broke world))))))
         {:counts {} :kinds {}}
         (map-indexed vector
                      (letfn [(more [i sizes rnd]
                                (lazy-seq
                                 (when (< i histories)
                                   (let [[r1 r2] (random/split rnd)]
                                     (cons (gen/call-gen gen-order r1 (first sizes))
                                           (more (inc i) (rest sizes) r2))))))]
                        (more 0 (gen/make-size-range-seq 80) (random/make-random seed)))))]
    {:counts counts :kinds kinds}))

(defn- print-live-counts [counts]
  (doseq [k [:versions-written :reads-opened :steps-of-a-read :steps-at-which-a-read-knew
             :steps-at-which-a-read-did-not-know :asks :answers
             :answers-of-an-older-version-than-the-store-was-at :asks-that-failed
             :answers-that-came-after-their-ask-had-failed :answers-that-came-after-a-later-ask-was-sent
             :reads-that-ended-for-a-cause :pushes-given :callbacks
             :callbacks-that-brought-more-than-one-change-at-once
             :steps-at-which-pushes-said-different-versions-and-the-read-knew
             :steps-at-which-pushes-said-different-versions-and-the-read-did-not-know
             :changes-in-place-written :changes-in-place-of-a-give-that-an-open-read-rested-on-above-its-own
             :acts-written-that-change-more-than-one-give
             :such-acts-that-moved-what-an-open-read-rested-on-and-left-its-level :tells]]
    (println (format "  %9d  %s" (get counts k 0) (str/replace (name k) "-" " ")))))

(defn- run-live
  "The reference read, held to what every step of a read is held to: in
  every order of the small histories, and in sampled orders of generated
  histories; with the runs as CONTRACT 2.4 has them, and with runs that
  keep a mark. Gives true when nothing broke but the two questions."
  [histories seed]
  (let [started (System/nanoTime)
        report-every (fn [lines]
                       (doseq [{:keys [what reader worlds broke]} lines]
                         (println (format "  %7d worlds  %-22s %s" worlds (reader-word reader) what))
                         (doseq [[class _] broke]
                           (println (format "                  %s %s"
                                            (if (contains? questions-of-a-read (first class)) "QUESTION" "BROKEN  ")
                                            (class-of-a-read class))))))
        red-lines (fn [lines] (mapcat (fn [line] (red-of (vals (:broke line)))) lines))
        asked (fn [lines word] (count (filter #(some (fn [class] (= word (first class))) (keys (:broke %))) lines)))
        every (vec (every-order 1))
        every-marked (binding [live/*a-run-keeps-a-mark?* true] (vec (every-order 1)))
        sampled (sample-orders histories seed)
        sampled-marked (binding [live/*a-run-keeps-a-mark?* true] (sample-orders histories seed))
        red-kinds (fn [found] (remove #(contains? questions-of-a-read (first (key %))) (:kinds found)))
        in-how-many (fn [found word] (reduce + (keep (fn [[class {:keys [count]}]] (when (= word (first class)) count)) (:kinds found))))
        green? (and (empty? (red-lines every)) (empty? (red-lines every-marked))
                    (empty? (red-kinds sampled)) (empty? (red-kinds sampled-marked))
                    (zero? (asked every-marked :came-back)) (zero? (in-how-many sampled-marked :came-back)))]
    (println "THE LIVE READ: the reference read, held to answered, was, not more, silent, asks and settles")
    (println)
    (println "EVERY ORDER of each small history, one read at a time, one ask failing at most; the runs as CONTRACT 2.4 has them:")
    (report-every every)
    (println "EVERY ORDER of the same, with runs that keep the stamp of the last decision that wrote under them:")
    (report-every every-marked)
    (println)
    (println (format "SAMPLED ORDERS: %d histories of up to 40 steps, one to four reads each, seed %d; the runs as CONTRACT 2.4 has them:"
                     histories seed))
    (print-live-counts (:counts sampled))
    (doseq [[word says] questions-of-a-read]
      (println (format "QUESTION %s, in %d of the %d histories, and in %d with runs that keep a mark: %s"
                       (name word) (in-how-many sampled word) histories (in-how-many sampled-marked word) says)))
    (doseq [[what found] [["the runs as CONTRACT 2.4 has them" sampled] ["runs that keep a mark" sampled-marked]]
            [class {:keys [count at tree]}] (red-kinds found)]
      (println)
      (println (format "BROKEN, %s: %s; in %d histories, first at history %d. The smallest order:"
                       what (class-of-a-read class) count at))
      (let [smallest (first (get-in (tc/quick-check 1 (prop/for-all [order (gen/return (rose/root tree))]
                                                         (not-any? #(= class (:class %)) (:broke (play-order order)))))
                                    [:shrunk :smallest]))]
        (print-sampled-break (or smallest (rose/root tree)) class)))
    (doseq [word (keys questions-of-a-read)
            :let [line (first (filter #(some (fn [class] (= word (first class))) (keys (:broke %))) every))]
            :when line
            ;; the order is looked for again with no ask failing: it is shorter
            :let [found (:broke (live/explore (:vs line) (:reader line) 0
                                              (fn [found] (some #(= word (first %)) (keys found)))))
                  [_ b] (first (filter #(= word (first (key %))) found))]]
      (println)
      (println (format "THE QUESTION %s, in the first order found of \"%s\", read by %s:"
                       (name word) (:what line) (reader-word (:reader line))))
      (live/print-order (:vs line) [(:reader line)] (:log b)))
    (println)
    (println (format "%.1f seconds" (/ (- (System/nanoTime) started) 1e9)))
    (if green?
      (println "HOLDS: in every order of the small histories and in every sampled order, the reference read keeps answered, not more, silent, asks and settles; it keeps was but for the question came-back, and keeps it whole with runs that keep a mark; the query and the rules give the same word at every version")
      (println "BROKEN: see above"))
    green?))

(def ^:private broken-reads
  "The broken copies of the live read and of its query, each as [what, {a
  var of `gate-histories.live-read`, a function that takes what is in
  place and gives the broken one}, how many asks may fail in an order of a
  small history]. Each must break something that a read is held to."
  [["invariant 9: an answer is told whatever its pushes say: a remembered answer"
    {#'live/agrees? (fn [_] (fn [read _present] (some? (:answer read))))} 1]
   ["invariant 9, the gap between the query and the push: a source of the read set that is not given yet counts as agreeing"
    {#'live/agrees?
     (fn [_]
       (fn [read present]
         (boolean (when-let [answer (:answer read)]
                    (every? (fn [[source expected]]
                              (or (not (contains? present source))
                                  (= expected (live/seen source (get present source)))))
                            (:read-set answer))))))} 1]
   ["invariant 9: an answer is held to its rows and not to its runs"
    {#'live/agrees?
     (fn [real]
       (fn [read present]
         (and (some? (:answer read))
              (real (update-in read [:answer :read-set] #(into {} (remove (fn [[[kind] _]] (= :run kind))) %)) present))))} 1]
   ["invariant 9: where no answer agrees, the read works a level out of what its pushes say"
    {#'live/standing-of
     (fn [real]
       (fn [read present]
         (let [word (real read present)
               {:keys [question asker session]} (:params read)]
           (if (and (= :not-known-yet word) (live/held-against-all? read present))
             (:standing (live/lane-rights
                         {:rows (into {} (keep (fn [[[kind id] v]] (when (= :row kind) [id v]))) present)
                          :runs (into {} (keep (fn [[[kind id] v]] (when (= :run kind) [id v]))) present)}
                         question asker session))
             word))))} 1]
   ["invariant 9: the read keeps what each push said at its callback, and does not read its pushes at each step"
    {#'live/present-to (fn [_] (fn [read _present] (:copies read)))} 1]
   ["invariant 10: while it does not know, the read goes on telling the lane's facts as they change"
    {#'live/tells
     (fn [real]
       (fn [read standing present]
         (let [facts (get present live/facts-source)]
           (if (and (= :not-known-yet standing) (= :yes (:told read)) (some? facts) (not= facts (:shown read)))
             [(assoc read :shown facts) [{:changed facts}]]
             (real read standing present)))))} 1]
   ["invariant 10: after not knowing, the read tells a change and not the whole answer: nothing is owed"
    {#'live/tells
     (fn [real]
       (fn [read standing present]
         (let [[after told] (real read standing present)]
           [(if (= :not-known-yet standing) (assoc after :owed? (:owed? read)) after) told])))} 1]
   ["invariant 11: the read asks whenever no answer agrees, though no rights push said anything new since its last ask"
    {#'live/called-back? (fn [_] (fn [_read _present] true))} 1]
   ["invariant 11: the read never asks again once it holds an answer"
    {#'live/wants-to-ask? (fn [_] (fn [_read _present] false))} 1]
   ["single flight: the read asks though an ask of it is in flight"
    {#'live/may-ask? (fn [_] (fn [_read] true))} 1]
   ["invariant 12: an answer is taken by every read of the lane, whoever asked and whatever was asked"
    {#'live/client-step
     (fn [real]
       (fn [client rid event present]
         (let [[client effects] (real client rid event present)]
           [(if (= :answer (first event))
              (update client :reads
                      (fn [reads] (into {} (map (fn [[r read]] [r (if (= r rid) read (assoc read :answer (nth event 2)))])) reads)))
              client)
            effects])))} 1]
   ["answers out of order: the answer of an earlier ask is taken when it comes late, in place of the one held"
    {#'live/takes-answer? (fn [_] (fn [_read _n] true))} 1]
   ["a failed ask: it reads as \"she may not see\""
    {#'live/after-failure
     (fn [_] (fn [read] (assoc read :flying nil :answer {:standing :may-not-see :read-set {}})))} 1]
   ["a failed ask: it is not asked again, and the read does not end"
    {#'live/after-failure (fn [_] (fn [read] (assoc read :flying nil)))} 1]
   ["a read that ended: it goes on telling the last level that it knew"
    {#'live/standing-of
     (fn [real]
       (fn [read present]
         (if (:ended? read) (or (:told read) live/ended-word) (real read present))))} 4]
   ["CONTRACT 2.4, the read set: it leaves out the rows above a lent give"
    {#'live/climb
     (fn [real]
       (fn [tables read-set id]
         (let [[values after] (real tables read-set id)
               source [:row id]]
           [values (assoc read-set source (get after source))])))} 1]
   ["CONTRACT 2.4, the read set: it leaves out a holder's run, and holds the rows that were read"
    {#'live/scan
     (fn [real]
       (fn [tables read-set holder session]
         (let [[found? cut? after] (real tables read-set holder session)]
           [found? cut? (dissoc after [:run holder])])))} 1]
   ["invariant 5: the query reads the runs in one turn and the rows in a later one"
    {#'live/answer-of
     (fn [_]
       (fn [vs a later {:keys [question asker session]}]
         (live/lane-rights (assoc (nth (:tables vs) later) :runs (:runs (nth (:tables vs) a)))
                           question asker session)))} 1]
   ["invariant 6: at the bound of a scan the query answers a level, \"she may not see\", and not a word"
    {#'live/lane-rights
     (fn [real]
       (fn [tables question asker session]
         (let [answer (real tables question asker session)]
           (if (= :too-many-permissions (:standing answer))
             (assoc answer :standing (if (= :see question) :may-not-see :outside))
             answer))))} 1]])

(defn- run-live-mutants
  "Plays every order of the small histories, and then sampled orders,
  against each broken copy of the read and of its query. Gives true when
  each broke something that a read is held to, beyond the two questions."
  [histories seed show?]
  (println (format "THE BROKEN COPIES OF THE LIVE READ: %d, each played every order of the small histories, then up to %d sampled orders, seed %d"
                   (count broken-reads) histories seed))
  (let [missed
        (reduce
         (fn [missed [what replaced fails]]
           (let [redefs (into {} (map (fn [[the-var make]] [the-var (make @the-var)])) replaced)
                 red? (fn [found] (boolean (seq (red-of (vals found)))))
                 in-every (with-redefs-fn redefs
                            #(first (for [line (every-order fails red?)
                                          b (red-of (vals (:broke line)))]
                                      (assoc b :line line))))]
             (if in-every
               (do (println (format "  caught in every order  %s" what))
                   (println (format "                   by %s; in \"%s\", read by %s"
                                    (class-of-a-read (:class in-every)) (:what (:line in-every))
                                    (reader-word (:reader (:line in-every)))))
                   (when (and show? (:log in-every))
                     (with-redefs-fn redefs
                       #(live/print-order (:vs (:line in-every)) [(:reader (:line in-every))] (:log in-every)))
                     (println))
                   missed)
               (let [result (with-redefs-fn redefs
                              #(tc/quick-check histories
                                               (prop/for-all [order gen-order]
                                                 (empty? (red-of (:broke (play-order order)))))
                                               :seed seed :max-size 80))]
                 (if (:pass? result)
                   (do (println (format "  NOT CAUGHT   %s" what)) (conj missed what))
                   (let [smallest (first (get-in result [:shrunk :smallest]))
                         class (with-redefs-fn redefs #(:class (first (red-of (:broke (play-order smallest))))))]
                     (println (format "  caught at %5d  %s" (:num-tests result) what))
                     (println (format "                   by %s" (class-of-a-read class)))
                     (when show?
                       (with-redefs-fn redefs #(print-sampled-break smallest class))
                       (println))
                     missed))))))
         []
         broken-reads)]
    (println (format "%d of %d broken copies of the live read caught" (- (count broken-reads) (count missed)) (count broken-reads)))
    (empty? missed)))

(defn- print-sentences
  "The rules in plain words, each with where it comes from and where in the
  code it is said."
  []
  (println "THE RULES, IN PLAIN WORDS")
  (doseq [[i {:keys [says from at]}] (map-indexed vector (reference/sentences))]
    (println)
    (println (format "%2d. %s" (inc i) says))
    (println (format "    [%s; %s]" from at))))

(defn -main
  "`clojure -M:run [histories [seed]]` runs the rules as their own gate, the
  store of the permission fix, the store before the fix, and the broken
  copies, and the live read and its broken copies, each when it loads
  against the store on the path; `rules`, `fix`, `fix-mutants`, `today`,
  `mutants`, `live` or `live-mutants` before the count runs one of them;
  `sentences` prints the rules in plain words. Exits 1 when anything was
  red."
  [& args]
  (let [mode (when (contains? #{"rules" "fix" "fix-mutants" "today" "mutants" "live" "live-mutants" "sentences"} (first args))
               (first args))
        args (if mode (rest args) args)
        histories (if-let [n (first args)] (parse-long n) 2000)
        seed (if-let [s (second args)] (parse-long s) (System/currentTimeMillis))
        show? (= "show" (nth args 2 nil))]
    (if (= "sentences" mode)
      (do (print-sentences) (System/exit 0))
      (let [run? (fn [m] (or (nil? mode) (= m mode)))
            lens? (some? (when (or (run? "today") (run? "mutants")) (todays 'gate)))
            fix? (some? (when (or (run? "fix") (run? "fix-mutants")) (the-fix 'gate)))
            results (filterv some?
                             [(when (run? "rules")
                                (let [green? (run-rules histories seed)] (println) ["the rules as their own gate" green?]))
                              (when (and fix? (run? "fix"))
                                (let [green? (run-fix histories seed)] (println) ["against the store of the permission fix" green?]))
                              (when (and fix? (run? "fix-mutants"))
                                (let [green? (run-broken-fixes histories seed show?)]
                                  (println)
                                  ["the store of the permission fix with one rule broken" green?]))
                              (when (and lens? (run? "today"))
                                (let [green? (run-today histories seed)] (println) ["against the store before the fix" green?]))
                              (when (run? "mutants")
                                (let [green? (run-mutants histories seed show?)] (println) ["the broken copies of the rules" green?]))
                              (when (and lens? (run? "mutants"))
                                (let [green? (run-broken-stores histories seed show?)]
                                  (println)
                                  ["the store before the fix with one rule broken" green?]))
                              (when (run? "live")
                                (let [green? (run-live histories seed)] (println) ["the live read" green?]))
                              (when (run? "live-mutants")
                                (let [green? (run-live-mutants histories seed show?)]
                                  (println)
                                  ["the broken copies of the live read" green?]))])]
        (doseq [[what green?] results]
          (println (if green? "GREEN" "RED  ") what))
        (when (and (not lens?) (or (run? "today") (run? "mutants")))
          (println "NOT RUN against the store before the fix, and the store before the fix with one rule broken"))
        (System/exit (if (every? second results) 0 1))))))
