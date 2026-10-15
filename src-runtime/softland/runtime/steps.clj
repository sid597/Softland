(ns softland.runtime.steps
  "Steps as data: the form in which the runtime's own work is said, so that
  it is safe to run again. Pure code. It needs no cluster.

  The runtime's start and a sign-in are each a list of steps
  (`softland.runtime.start`, `softland.runtime.sign-in`). The store gives
  no order between two layers (13 October 2026, entry 2), so the runtime
  waits for each answer before it sends the next act (BUILD.md, \"The first
  sign-in\"). A list of steps is therefore no program that runs; it is a
  pure function of what is known so far, which gives the next thing to do:

    {:do :read :reads #{query …} :step name}   reads to make, each a query
                                               of the read path, asked once
    {:do :act :act act :step name}             one act to send, as the
                                               system, and wait for
    {:do :done …}                              the work is whole
    {:do :refuse :word word :step name …}      the work stops, with why

  What is known is {:answers {query answer} :outcomes {act outcome}}: what
  each read gave (`softland.store.read-path/read-once`: an answer, or a word
  with `:could-not`), and what each act was answered
  (`softland.store.door/answer-of`: {:answer :accepted …} or
  {:answer :refused :reason word …}, or a word). A caller adds what it
  learns with `with-answers` and `with-outcome`, and asks again. `drive` is
  that loop, with the two effects handed in.

  Safe to run again. Each step reads first and does only what is missing.
  A list is worked out from its first step at every call, so a run that
  starts from nothing, in a process after a crash, finds what an earlier
  run did and goes on from there. Three rules make that hold:

    - An act's outcome drops each answer that the act could have changed
      (`touches?`), and the step reads again. So a one-write claim that
      was refused because another got there first is followed by a read
      that finds what is there, and the step goes on with it.
    - An act that was accepted counts as done (`acting`), also when a read
      after it does not show it yet: an act of the base, a microbatch
      layer, is in a read once the task that serves the read holds its
      rows (`softland.store.read-path/read-once`).
    - An act that a step would send again after it was refused, or after
      no answer came, stops the work: `:step-refused` or
      `:act-not-answered`. No act is sent twice in one run.

  Inputs and results, by call:

    place-query    an asker, a layer, an entity, a key -> a query
    entity-query   an asker, a layer, an entity, whether the store's own
                   facts are asked for -> a query
    failed?        an answer -> true for a read path's word
    left-out-why   an answer, a layer -> :not-made, :may-not-see or nil
    place-of       an answer of a place -> its place-answer
    value-at       an answer of a place -> the value, or nil
    holds-at       an answer of a place -> :value, :nothing-here, :no-fact
    record-of      an answer of an entity, the entity -> {key value}
    fact-at        an answer of an entity, a place -> the fact id there
    system-act     a context, a layer, facts -> an act of the system
    making-act     a context, a layer, its holder -> the act that makes it
    touches?       an act, a query -> whether the act can change the answer
    with-answers   known, {query answer} -> known
    with-outcome   known, an act, its outcome -> known
    reading        the step's tool: reads first, then a function of answers
    acting         the step's tool: an act, or go on when it was accepted
    run-steps      a list of steps, a context, a finish -> the next thing
    drive          a step function, read!, send! -> the last thing given

  State and effects. None, but `drive`, which calls the two functions that
  it is handed and nothing else. Each other function is pure.

  Callers. `softland.runtime.start`, `softland.runtime.sign-in` and
  `softland.runtime.system-steps`, whose steps use its tools;
  `softland.runtime.service`, which drives their step functions with the
  store's door and read path; and the pure proof, which drives them on a
  model of the store.

  Limits. An act that changes a place that a step read, made by someone
  else while the step runs, is found only when one of the step's own acts
  is refused for it, or at the next run: a read that was answered stays
  known until an act of this run touches it. A refusal that comes again
  because a read of the base lags behind a concurrent run's accepted act
  stops the work with `:step-refused`; the next run finds the act."
  (:require [softland.store.act :as act]
            [softland.store.gate.first-act :as first-act]))

;; ------------------------------------------------------------------ reads

(defn- store-key?
  "True for a key in the store's own namespace."
  [k]
  (and (keyword? k) (= act/store-namespace (namespace k))))

(defn place-query
  "Takes an asker, a layer, an entity and a key. Gives the query of that one
  place in that one layer, asked once by that asker
  (`softland.store.read-path.query`). A key of the store's own namespace
  asks for the store's own facts, as the read path needs."
  [asker layer e k]
  (cond-> {:asks [:place e k] :asker asker :layers [layer]}
    (store-key? k) (assoc :stores-own-facts true)))

(defn entity-query
  "Takes an asker, a layer, an entity, and whether the store's own facts are
  asked for. Gives the query of every place of that entity in that layer."
  [asker layer e store-facts?]
  (cond-> {:asks [:entity e] :asker asker :layers [layer]}
    store-facts? (assoc :stores-own-facts true)))

(defn failed?
  "Takes what a read gave. Gives true for a word of the read path (a map
  with `:could-not`), and for anything that is no map: such a read said
  nothing of the store."
  [answer]
  (or (not (map? answer)) (contains? answer :could-not)))

(defn left-out-why
  "Takes an answer and a layer. Gives why the read left the layer out,
  `:not-made` or `:may-not-see`, or nil when it did not
  (`softland.store.read-path.answer`, `:left-out`)."
  [answer layer]
  (some #(when (= layer (:layer %)) (:why %)) (:left-out answer)))

(defn place-of
  "Takes the answer of a place. Gives its place-answer, or {:holds :no-fact}
  when there is none."
  [answer]
  (or (:place answer) {:holds :no-fact}))

(defn holds-at
  "Takes the answer of a place. Gives what the place holds: `:value`,
  `:nothing-here` (its nearest fact is a retract: a thing that was there
  and was ended), `:no-fact`, or `:not-a-value`."
  [answer]
  (:holds (place-of answer)))

(defn value-at
  "Takes the answer of a place. Gives the value there, or nil when the place
  holds no value."
  [answer]
  (let [pa (place-of answer)]
    (when (= :value (:holds pa)) (:value pa))))

(defn record-of
  "Takes the answer of an entity and the entity. Gives a map from each key
  of the entity that holds a value to that value. A place whose nearest
  fact is a retract is not in it."
  [answer entity]
  (into {}
        (keep (fn [[[e k] pa]] (when (and (= entity e) (= :value (:holds pa))) [k (:value pa)])))
        (:places answer)))

(defn fact-at
  "Takes the answer of an entity and a place [entity key]. Gives the id of
  the fact that the place holds, which an act names in `:replaces`, or nil."
  [answer place]
  (:fact (get (:places answer) place)))

;; ------------------------------------------------------- acts of the system

(defn system-act
  "Takes a context, which holds the system's session under
  `[:in :system-session]`, a layer and facts. Gives the act of the system
  in that layer: `:who` the system, under the system's first permission in
  the layer, `[:system layer layer]`. What must hold: the system is the
  layer's top, or the gate refuses the act."
  [ctx layer facts]
  {:version act/version
   :who first-act/system
   :layer layer
   :permission [first-act/system layer layer]
   :session (get-in ctx [:in :system-session])
   :facts (vec facts)})

(defn making-act
  "Takes a context, a layer and the holder of its first permission. Gives
  the act that makes the layer (`softland.store.gate.first-act/making-act?`):
  `:who` the system, the permission `[holder layer layer]`, the layer's
  depot at its making and that permission on its holder. For the system's
  layers and a record layer the holder is the system; for a person's own
  layer it is she, and the system keeps no right there (BUILD.md, default
  18)."
  [ctx layer holder]
  (let [permission [holder layer layer]]
    {:version act/version
     :who first-act/system
     :layer layer
     :permission permission
     :session (get-in ctx [:in :system-session])
     :facts [{:e layer :k :softland.store/depot :v (first-act/depot-at-making layer)}
             {:e holder :k :softland.store/permission :v permission}]}))

;; ------------------------------------------------------------- what is known

(def nothing-known
  "What is known before a run reads anything."
  {:answers {} :outcomes {}})

(defn- query-layers
  "Takes a query. Gives the set of layers that it names."
  [query]
  (set (or (:layers query) (when (:from-layer query) [(:from-layer query)]))))

(defn touches?
  "Takes an act and a query. Gives true when the act can change what the
  query is answered: the query reads the act's layer, and the act holds a
  fact under a store key (it makes the layer, gives a right or changes a
  setting, any of which changes what a read of the layer sees), or a fact
  at the place the query asks, or on the entity it asks. True for a query
  of another word that reads the layer. It errs toward true: a dropped
  answer costs one more read."
  [the-act query]
  (boolean
   (and (contains? (query-layers query) (:layer the-act))
        (let [facts (:facts the-act)
              [word a b] (:asks query)]
          (or (some #(store-key? (:k %)) facts)
              (case word
                :place (some #(and (= a (:e %)) (= b (:k %))) facts)
                :entity (some #(= a (:e %)) facts)
                true))))))

(defn with-answers
  "Takes what is known and a map from each query read to what it gave.
  Gives what is known with those answers."
  [known answers]
  (update known :answers merge answers))

(defn with-outcome
  "Takes what is known, an act that was sent, and what it was answered.
  Gives what is known with the outcome kept under the act, and without the
  answer of each query that the act touches (`touches?`), so that the step
  reads them again."
  [known the-act outcome]
  (-> known
      (assoc-in [:outcomes the-act] outcome)
      (update :answers (fn [answers] (into {} (remove (fn [[q _]] (touches? the-act q))) answers)))))

(defn accepted?
  "True for an outcome that says the act was accepted."
  [outcome]
  (= :accepted (:answer outcome)))

(defn- refused?
  "True for an outcome that says the act was refused."
  [outcome]
  (= :refused (:answer outcome)))

;; ----------------------------------------------------------- a step's tools

(defn reading
  "A step's first tool. Takes the context, the queries that the step needs,
  and a function of their answers, in the order of the queries. Gives
  [:read the queries not yet answered] while any is missing;
  [:refuse :read-failed {…}] when one gave a word of the read path; else
  what the function gives."
  [ctx queries f]
  (let [answers (get-in ctx [:known :answers])
        missing (into #{} (remove #(contains? answers %)) queries)]
    (cond
      (seq missing) [:read missing]
      :else (if-let [q (first (filter #(failed? (get answers %)) queries))]
              [:refuse :read-failed {:query q :could-not (get answers q)}]
              (f (mapv #(get answers %) queries))))))

(defn acting
  "A step's second tool. Takes the context, the act that the step needs, and
  the context to go on with once the act holds. Gives [:act act] when the
  act was not sent; [:go context] when it was accepted, whatever a read
  shows yet; [:refuse :step-refused {:reason word}] when it was refused and
  the step, having read again, still needs it; [:refuse :act-not-answered
  {:outcome o}] when the answer was no accept and no refusal."
  [ctx the-act go-ctx]
  (let [outcomes (get-in ctx [:known :outcomes])]
    (if-not (contains? outcomes the-act)
      [:act the-act]
      (let [o (get outcomes the-act)]
        (cond
          (accepted? o) [:go go-ctx]
          (refused? o) [:refuse :step-refused {:reason (:reason o) :layer (:layer the-act)}]
          :else [:refuse :act-not-answered {:outcome o :layer (:layer the-act)}])))))

(defn run-steps
  "Takes a list of steps, each [name f], where f takes a context and gives
  [:go context], [:read queries], [:act act], [:refuse word detail] or
  [:done result]; a context, which holds what is known under `:known`; and
  a finish, a function of the last context that gives the done map. Runs
  the steps in order from the first, and gives the first thing that is not
  [:go …], as `{:do …}` with the step's name, or what the finish gives when
  every step went on."
  [steps ctx finish]
  (loop [ctx ctx
         todo steps]
    (if-let [[step-name f] (first todo)]
      (let [[what x detail] (f ctx)]
        (case what
          :go (recur x (rest todo))
          :read {:do :read :reads x :step step-name}
          :act {:do :act :act x :step step-name}
          :done (merge {:do :done} x)
          :refuse (merge {:do :refuse :word x :step step-name} detail)))
      (finish ctx))))

(def default-max-rounds
  "How many reads and acts `drive` makes before it gives up: the bound of a
  run that does not end. The pure proof shows how many a start and a
  sign-in take. A number of the build."
  200)

(defn drive
  "The loop. Takes a step function (what is known -> the next thing), a
  function that reads one query and gives what the read path gives, and a
  function that sends one act and gives its outcome, having waited for it;
  and, as options, `:known` to start from and `:max-rounds`. Asks the step
  function, makes the reads it asks (one call of `read!` each) or sends the
  act it gives, adds what came back, and asks again, until it gives done or
  a refusal. Gives that last thing, with `:rounds`, the count of reads and
  acts made, and `:known`. Gives {:do :refuse :word :steps-did-not-end} past
  the bound. It has no effect of its own: the two functions are the
  caller's."
  [step-fn read! send! & {:keys [known max-rounds] :or {known nothing-known max-rounds default-max-rounds}}]
  (loop [known known
         rounds 0]
    (let [step (step-fn known)]
      (cond
        (>= rounds max-rounds)
        {:do :refuse :word :steps-did-not-end :rounds rounds :known known}

        (= :read (:do step))
        (recur (with-answers known (into {} (map (fn [q] [q (read! q)])) (:reads step)))
               (+ rounds (count (:reads step))))

        (= :act (:do step))
        (recur (with-outcome known (:act step) (send! (:act step))) (inc rounds))

        :else (assoc step :rounds rounds :known known)))))
