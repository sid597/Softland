(ns softland.auth.module-check
  "The run that holds a module of Auth against the reference of its contract,
  through the contract alone. The contract is Auth's spec,
  `src/proposal/runtime-2026-11-21/spine/auth/SPEC.md`; the reference,
  `softland.auth.reference`, is that contract written a second time. One
  command, from the repository root:

    clojure -M:auth -m softland.auth.module-check [seed [histories [module]]]

  `module` is the full name of a function of no argument that launches the
  module and gives what the run holds against the reference, as
  `implementation` makes it. Without it the run holds the reference itself
  three ways: as it is; with its watches called late, on threads of their
  own (`late-watches`); and with the answers of some calls lost, made or not
  (`lossy`). Then it holds each broken copy of the reference
  (`softland.auth.reference/breaks`), and says for each whether the run
  caught it.

  What it does. It opens two clients of the module through the contract's
  `open`, as two copies of the runtime, and sends generated histories
  (`softland.auth.histories`) through them and through the reference: each
  write through one client or the other; reads of `who-is`, `ask-of`,
  `under`, `invitation-of` and `accounts-of`, of their own and, after each
  write, of every root, claim and person that the write could change; and
  watches, registered with `watch-root` and stopped. After
  each step it compares the module's result with the reference's, each read
  with the reference's read, and what each watch came to with what is
  watched under its root in the reference. It reads nothing of the module but what
  the twenty-two functions give.

  What a real module needs of it:
  - A watch's calls come later, on another thread. After a write that
    changed what is watched under a watched root, the run waits, a bound at
    most, until the watch has come to the reference's value. It compares
    what the watch came to, not the values that it passed through.
  - A `{:could-not ...}` from a write, but `:read-the-event`, says nothing of
    whether the write was taken: the run sends the same write again, a bound
    of tries at most, and compares what the last try gave. The contract makes
    a write that arrives twice the same as once, so the reference is sent it
    once. A read is tried again so too. A `:could-not` is compared by its word
    alone, since the contract lets it hold more.
  - The histories of a run go through one Auth, since a launch for each would
    take hours: each history's ids, accounts, emails, hashes and codes are its
    own, and its reference starts empty. A run needs a module that nothing
    else has written to.
  - A watch that says `:softland.auth.client/not-reached`, or is called after
    it, is a difference: the contract lets a process say so when it can no
    longer say what is watched, and in a run on one machine nothing is lost.
  - A quarter of the histories hold a deep segment: 32 agents in a line under
    one root, one more under the deepest, which is `:too-deep`, and a sign-in
    for the deepest. The histories go on with those agents among the rest.
  - A link's id is made by the module, the op of the enter that made it,
    and by the reference otherwise: the run pairs the two by the account
    and the time of each link that `accounts-of` gives, holds the pairing
    to one id for one, and sends an unlink the module's id of the link that
    the reference names. Every enter that links is followed by a read of
    its person's accounts, so each link is paired before an unlink names it.
  - Two rules of a session's defaults of 22 November, Sid's to turn, are met in every run, not only
    now and then: after an `own-lane-made`, a copy of the person's first
    enter is sent again, now and then; and after a replacement whose new hash
    is the one before the present one, the replacement before it
    (`follow-ups`).

  A history stops at its first difference, since what follows would mostly
  repeat it, and the run prints the first differences that it found.

  State. It writes to the module that it is given and closes its clients at
  the end; nothing else."
  (:require [softland.auth.histories :as hs]
            [softland.auth.reference :as r])
  (:import [java.util Random UUID]))

;; ---------------------------------------------------------- saying a check

(def ^:private counts
  "The counts of this run: how many checks passed and failed."
  (atom {:passed 0 :failed 0}))

(defn- shown
  "`x` as a line short enough to read."
  [x]
  (binding [*print-length* 10 *print-level* 5] (pr-str x)))

(defn- check
  "Prints one line, `PASSED` or `FAILED` before the sentence, and for a
  failure what was seen; counts it; and gives whether it held."
  ([sentence held?] (check sentence held? nil))
  ([sentence held? seen]
   (if held?
     (do (swap! counts update :passed inc)
         (println "PASSED" sentence))
     (do (swap! counts update :failed inc)
         (println "FAILED" sentence)
         (doseq [s (if (sequential? seen) seen [seen]) :when (some? s)]
           (println "       seen:" (shown s)))))
   (boolean held?)))

;; -------------------------------------------------------- what is checked

(def contract-names
  "The twenty-two functions of the contract, by their names in
  `softland.auth.client`."
  '[open close! invite enter take-back unlink own-lane-made begin-sign-in replace-secret end-sign-in
    make-actor end-actor open-session close-session ask answer-ask who-is ask-of under invitation-of
    accounts-of watch-root])

(defn calls-of
  "The twenty-two functions of the contract as the namespace `ns-sym` gives them,
  the namespace loaded first: a map from each name of the contract to its
  function, or nil when one is missing."
  [ns-sym]
  (require ns-sym)
  (let [m (into {} (map (fn [s] [s (some-> (ns-resolve ns-sym s) deref)])) contract-names)]
    (when (every? ifn? (vals m)) m)))

(def ^:private module-defaults
  "How long the run waits and how often it tries, unless the module says:
  the bound on a watch's coming to a value, the pause before the watches are
  compared at the end of a history, the tries of a call, and the pause
  between tries, which grows with each."
  {:watch-bound-ms 10000 :settle-ms 200 :tries 8 :retry-pause-ms 100})

(defn implementation
  "What the run holds against the reference: `calls`, the twenty-two functions
  as `calls-of` gives them; `cluster`, what stands for the cluster that the
  module runs in, as `open` takes it; and `module-name`. Options: `:name`,
  `:watch-bound-ms`, `:settle-ms`, `:tries`, `:retry-pause-ms`, and `:close`,
  a function of no argument that the run calls last."
  [calls cluster module-name & {:as opts}]
  (merge module-defaults {:name module-name} opts
         {:calls calls :cluster cluster :module-name module-name}))

(defn the-reference
  "The reference itself, as a module for the run: its twenty-two functions, over
  a cluster of its own. With `broken`, a key of `r/breaks`, the copy of the
  rules in which that one rule is broken. Its watches are called before a
  write returns, so the run waits for nothing."
  ([] (the-reference nil))
  ([broken]
   (implementation (calls-of 'softland.auth.reference)
                   (if broken (r/broken-cluster broken "Auth") (r/cluster "Auth"))
                   "Auth"
                   :name (if broken (str "the reference, with the rule " (name broken) " broken") "the reference itself")
                   :watch-bound-ms 1000 :settle-ms 0 :tries 3 :retry-pause-ms 0)))

(defn late-watches
  "The module `impl` with late watches, as a real client's are: each call
  that a watch is owed is handed to a thread of the watch's own, which makes
  it after a pause of up to `most-ms`, the calls of one watch in their order.
  For the run's own proof that it waits for a watch and compares what the
  watch came to."
  [impl most-ms seed]
  (let [watch-root (get-in impl [:calls 'watch-root])
        rnd (Random. (long seed))]
    (-> impl
        (assoc :name (str (:name impl) ", with its watches called up to " most-ms " ms late, on threads of their own"))
        (assoc :settle-ms (max (:settle-ms impl) (* 4 most-ms)))
        (assoc-in [:calls 'watch-root]
                  (fn [c P f]
                    (let [pool (java.util.concurrent.Executors/newSingleThreadExecutor)
                          late (fn [v]
                                 (let [pause (locking rnd (.nextInt rnd (inc most-ms)))]
                                   (.execute pool (fn [] (Thread/sleep (long pause)) (f v))))
                                 nil)
                          given (watch-root c P late)]
                      (if (fn? given)
                        (fn [] (given) (.shutdown pool) nil)
                        (do (.shutdown pool) given))))))))

(def ^:private read-calls
  "The names in the contract of the reads but `watch-root`."
  '[who-is ask-of under invitation-of accounts-of])

(def ^:private write-calls
  "The names in the contract of the fourteen writes."
  (mapv (fn [k] (symbol (name k))) hs/write-kinds))

(defn lossy
  "The module `impl`, made to give `{:could-not :reach-the-module}` for one
  write or read in `one-in`, at random: half of those before the call, which
  is then not made, and half after it, when the call was made and its answer
  lost on the way. For the run's own proof that it sends such a call again,
  and that the contract's rule for a write that arrives twice makes that
  safe."
  [impl one-in seed]
  (let [rnd (Random. (long seed))
        roll (fn [] (locking rnd (let [x (.nextInt rnd (* 2 one-in))] (case x 0 :before 1 :after nil))))
        lose (fn [f] (fn [c & args]
                       (case (roll)
                         :before {:could-not :reach-the-module :threw "lost before the call"}
                         :after (do (apply f c args) {:could-not :reach-the-module :threw "lost after the call"})
                         (apply f c args))))]
    (-> impl
        (assoc :name (str (:name impl) ", which loses the answer of one call in " one-in ", made or not"))
        (assoc :tries 12)
        (update :calls (fn [calls] (reduce (fn [m s] (update m s lose)) calls (concat write-calls read-calls)))))))

(def ^:private broken-clients
  "Clients over the reference, each wrong in one way that only time or a
  watch shows, never a rule of the state: for the run's own proof that what
  it compares of watches and of reads after a write is not empty. Each is a
  sentence and a function that breaks a module."
  [["never calls its watches"
    (fn [impl] (let [wr (get-in impl [:calls 'watch-root])]
                 (assoc-in impl [:calls 'watch-root] (fn [c P _] (wr c P (fn [_] nil))))))]
   ["calls its watches for every second change alone"
    (fn [impl] (let [wr (get-in impl [:calls 'watch-root])]
                 (assoc-in impl [:calls 'watch-root]
                           (fn [c P f] (let [n (atom 0)] (wr c P (fn [v] (when (odd? (swap! n inc)) (f v)))))))))]
   ["says :softland.auth.client/not-reached at a watch's first change"
    (fn [impl] (let [wr (get-in impl [:calls 'watch-root])]
                 (assoc-in impl [:calls 'watch-root]
                           (fn [c P f] (let [lost? (atom false)]
                                         (wr c P (fn [v] (when (compare-and-set! lost? false true)
                                                           (f :softland.auth.client/not-reached)))))))))]
   ["returns from a write before its effect can be read: its reads are one write behind"
    (fn [impl] (let [behind (r/open (r/cluster "Auth") "Auth")
                     owed (atom nil)
                     calls (:calls impl)
                     late (fn [f] (fn [c input]
                                    (let [result (f c input)]
                                      (when-let [[g x] @owed] (g behind x))
                                      (reset! owed [f input])
                                      result)))]
                 (-> impl
                     (update :calls (fn [m] (reduce (fn [m s] (update m s late)) m write-calls)))
                     (update :calls (fn [m] (reduce (fn [m s] (assoc m s (fn [_ arg] ((get calls s) behind arg))))
                                                    m read-calls))))))]])

;; -------------------------------------------------------------- the calls

(defn- could-not-word
  "The word of a `:could-not`, or nil for anything else."
  [x]
  (when (and (map? x) (contains? x :could-not)) (get x :could-not)))

(defn- says-nothing?
  "Whether `x` is a `:could-not` that says nothing of whether the call was
  taken: every word but `:read-the-event`, which says that the input does not
  fit and that nothing was written."
  [x]
  (let [word (could-not-word x)]
    (and (some? word) (not= :read-the-event word))))

(defn- comparable
  "A result as the contract lets it be compared: a `:could-not` by its word
  alone."
  [x]
  (if-let [word (could-not-word x)] {:could-not word} x))

(defn- send!
  "Calls `f` with `args`, and again while it gives a `:could-not` that says
  nothing, `:tries` of the module at most, with a pause that grows. A throw
  is caught and given as `{:threw ...}`, which no result of the reference
  is. Gives the last result and how many times the call was sent again."
  [impl f & args]
  (loop [k 1]
    (let [x (try (apply f args) (catch Throwable t {:threw (str t)}))]
      (if (and (says-nothing? x) (< k (:tries impl)))
        (do (when (pos? (:retry-pause-ms impl)) (Thread/sleep (long (* k (:retry-pause-ms impl)))))
            (recur (inc k)))
        [x (dec k)]))))

(defn- read-of
  "The reference's read `kind` of `arg` in the state."
  [state kind arg]
  (case kind
    :who-is (r/who-is state arg)
    :ask-of (r/ask-of state arg)
    :under (r/under state arg)
    :invitation-of (r/invitation-of state arg)
    :accounts-of (r/accounts-of state arg)))

(defn- call-name
  "The name in the contract of the write or read `kind`."
  [kind]
  (symbol (name kind)))

;; -------------------------------------------------------------- the watches

(defn- watch!
  "Registers a watch on the root `P` through the client `c` of the module.
  Gives what `watch-root` gave, a function that stops the watch or what came
  in its place, and the record that the watch's function keeps: the last
  value, how many calls, whether `:softland.auth.client/not-reached` came,
  and how many calls came after it. The function returns at once."
  [impl c P]
  (let [rec (atom {:last ::none :calls 0 :lost? false :after-lost 0})
        f (fn [v]
            (swap! rec (fn [x] (cond (:lost? x) (update x :after-lost inc)
                                     (= v :softland.auth.client/not-reached) (assoc x :lost? true)
                                     :else (-> x (assoc :last v) (update :calls inc)))))
            nil)
        [given _] (send! impl (get (:calls impl) 'watch-root) c P f)]
    [given rec]))

(defn- stops?
  "Whether `x` is what `watch-root` gives for a watch that it took: a function
  of no argument, not a map, a keyword or another thing that can be called."
  [x]
  (and (ifn? x) (not (coll? x)) (not (keyword? x)) (not (symbol? x))))

(defn- wait-for
  "Waits, `bound-ms` at most, until the watch's record holds `want` as its
  last value, or says not-reached. Gives the record as it then is."
  [rec want bound-ms]
  (let [deadline (+ (System/nanoTime) (* 1000000 (long bound-ms)))]
    (loop []
      (let [x @rec]
        (if (or (= want (:last x)) (:lost? x) (>= (System/nanoTime) deadline))
          x
          (do (Thread/sleep 1) (recur)))))))

(defn- watch-difference
  "Compares what the watches came to with what the reference holds live
  under each one's root. A watch whose root changed since it was registered
  must come to the reference's value, within the bound when `wait?`; one
  whose root did not may hold no value, or the value at its registration. A
  watch that said not-reached, or was called after it, differs: in a run on
  one machine nothing is lost. Gives the first difference, or nil."
  [impl watches wait?]
  (first
   (for [[id {:keys [root rec want base]}] (sort-by key watches)
         :let [x (if (and wait? (not= ::none want)) (wait-for rec want (:watch-bound-ms impl)) @rec)
               ok? (and (not (:lost? x))
                        (zero? (:after-lost x))
                        (if (= ::none want)
                          (or (= ::none (:last x)) (= base (:last x)))
                          (= want (:last x))))]
         :when (not ok?)]
     {:what (cond (:lost? x) :a-watch-said-not-reached
                  (pos? (:after-lost x)) :a-watch-was-called-after-not-reached
                  :else :a-watch-came-to-another-value)
      :watch id :root root :reference (if (= ::none want) base want) :module (:last x)})))

(defn- stop-all!
  "Stops every watch, through the function that the module gave for it."
  [watches]
  (doseq [[_ W] watches]
    (try ((:stop W)) (catch Throwable _ nil))))

;; ------------------------------------------------------------ the histories

(defn- world
  "What the runtime of history `i` of the run of `seed` holds: the
  generator's world, with pools that no other history of the run shares, so
  that every history of one run can go through one Auth: its accounts, the
  hashes of its emails and of its codes, the codes of its asks, and the
  hashes that it makes; and the pairing of the reference's links with the
  module's (`:links*`)."
  [seed i]
  (let [n (+ seed i)
        k (mod n 0x40000000)
        base (* k 1000000)]
    (assoc (hs/new-world n)
           :accounts [[:google (str "g" n ".1")] [:google (str "g" n ".2")] [:github (str "h" n ".1")]
                      [:google (str "g" n ".3")]]
           :email-hashes [(hs/h (+ base 999101)) (hs/h (+ base 999102)) (hs/h (+ base 999103))]
           :links* (atom {})
           :code-hashes [(hs/h (+ base 999001)) (hs/h (+ base 999002)) (hs/h (+ base 999003))]
           :codes (mapv #(format "%08X" (+ (* 4 k) %)) [0 1 2])
           :hash-base base
           :pending [])))

(defn- deep-steps
  "The deep segment under the root `P`: 32 agents in a line, each made by the
  one before it and the first by `P`; one more, made by the deepest, which is
  32 makers below its root and so `:too-deep`; and a sign-in for the
  deepest. Their ids are new to the history."
  [w P]
  (let [rnd (:rnd w)
        ids (vec (repeatedly 33 #(hs/uuid-of rnd)))
        makers (into [P] ids)
        at (:now w)
        [w H] (hs/fresh-hash w)]
    [w (conj (mapv (fn [j] {:op :write
                            :call [:make-actor {:root P :actor (ids j) :made-by (makers j)
                                                :label (str "deep " (inc j)) :at at}]})
                   (range 33))
             {:op :write
              :call [:begin-sign-in {:root P :actor (ids 31) :sign-in (hs/pick rnd (:sign-ins w)) :kind :program
                                     :hash H :until (+ at 5000) :label nil :at at}]})]))

(defn- a-read
  "A read for a step of its own: `who-is`, `ask-of`, `under`,
  `invitation-of` or `accounts-of`, with parts chosen as the writes' are,
  and now and then one whose input does not fit."
  [w state]
  (let [rnd (:rnd w)
        P (hs/a-root w state)
        unfit? (hs/chance? rnd 0.12)]
    (case (.nextInt ^Random rnd 5)
      3 [:invitation-of (if unfit?
                          (hs/pick rnd [{:by :email :email "a@x.org"} {:by :code} nil (str P)])
                          (hs/pick rnd (hs/claims-of w)))]
      4 [:accounts-of (if unfit? (hs/pick rnd [(str P) nil 7]) P)]
      0 (let [q {:root P :sign-in (hs/a-sign-in w state P)
                 :session (when (hs/chance? rnd 0.7) (hs/pick rnd (:sessions w)))}]
          [:who-is (if unfit?
                     (hs/pick rnd [(dissoc q :session) (assoc q :more 1) (assoc q :root (str P)) (assoc q :sign-in nil) nil])
                     q)])
      1 [:ask-of (if unfit? (hs/pick rnd ["CODE001" "CODE00011" 12345678 nil]) (hs/pick rnd (:codes w)))]
      [:under (if unfit? (hs/pick rnd [(str P) nil 7]) P)])))

(defn- next-step
  "The next step of a history: moves the clock, then takes a step of the
  deep segment when one is under way; else a repeat of a write that was
  taken, a write whose input does not fit, a read, a watch registered or
  stopped, or a write that fits."
  [w state watches]
  (let [w (hs/tick w)
        rnd ^Random (:rnd w)]
    (if-let [step (first (:pending w))]
      [(update w :pending (comp vec rest)) step]
      (let [roll (.nextDouble rnd)]
        (cond
          (and (< roll 0.12) (seq (:taken w)))
          (let [[w call j] (hs/a-repeat w)] [w {:op :write :call call :repeat-of j}])
          (< roll 0.17)
          (let [[w kind input] (hs/unfit-input w state)] [w {:op :write :call [kind input] :unfit? true}])
          (< roll 0.25)
          [w {:op :read :read (a-read w state)}]
          (and (< roll 0.29) (< (count watches) 4))
          [w {:op :watch :root (if (hs/chance? rnd 0.08) (str (hs/a-root w state)) (hs/a-root w state))}]
          (and (< roll 0.31) (seq watches))
          [w {:op :stop :watch (hs/pick rnd (vec (sort (keys watches))))}]
          :else
          (let [kind (if (and (empty? (hs/entered-persons state)) (hs/chance? rnd 0.5))
                       (hs/pick rnd [:invite :enter])
                       (hs/pick rnd hs/weighted-kinds))
                [w call] (hs/make-call w state kind)]
            [w {:op :write :call (hs/from-a-clock-far-ahead w call)}]))))))

(defn- follow-ups
  "Steps that a write calls for next, as the runtime sends them, so that two rules of a session's defaults of 22 November, Sid's to turn, are met in every run and not only now
  and then: after an `own-lane-made` that was taken, half the time a copy of
  the person's first enter, which must say the flag as it stands; after a
  replacement whose new hash is the hash before the present one, half the
  time the replacement before it sent again, which is taken, since its old
  hash is the hash now. Gives the world, with the steps in its queue."
  [w state [kind input] want]
  (let [rnd (:rnd w)
        taken (:taken w)
        index-of (fn [pred] (first (keep-indexed (fn [j {:keys [call result]}] (when (pred call result) j)) taken)))]
    (case kind
      :own-lane-made
      (if-let [j (when (and (= {:made (:person input)} want) (hs/chance? rnd 0.5))
                   (index-of (fn [[k _] result] (and (= k :enter) (= (:person input) (:person result))))))]
        (update w :pending conj {:op :write :call (:call (nth taken j)) :repeat-of j})
        w)
      :replace-secret
      (let [g (get-in state [:roots (:root input) :sign-ins (:sign-in input)])
            came-back? (and g (= (:new-hash input) (get-in g [:previous :hash])) (contains? want :replaced))
            j (when (and came-back? (hs/chance? rnd 0.5))
                (index-of (fn [[k x] _] (and (= k :replace-secret) (= (:root x) (:root input))
                                             (= (:sign-in x) (:sign-in input))
                                             (= (:old-hash x) (:new-hash input)) (= (:new-hash x) (:old-hash input))))))]
        (if j
          (let [[k x] (:call (nth taken j))]
            (update w :pending conj {:op :write :call [k (assoc x :at (:now w))] :repeat-of j}))
          w))
      w)))

(defn- sweep
  "The reads after a write: for each root that the write could change, its
  `under`, and `who-is` of each pair of the history's sign-ins and sessions
  and of one with no session; after an ask or an answer, `ask-of` of each of
  the history's codes; after an invite, an enter, a take-back or an unlink,
  `invitation-of` of each of the history's claims and `accounts-of` of each
  of its persons and of the person that the write gave."
  [w kind input result]
  (let [root (when (map? input) (some #(when (uuid? %) %) [(get input :root) (get input :person)]))
        person (when (map? result) (let [p (get result :person)] (when (uuid? p) p)))]
    (concat
     (for [P (distinct (remove nil? [root person]))
           q (concat [[:under P] [:who-is {:root P :sign-in (first (:sign-ins w)) :session nil}]]
                     (map (fn [G S] [:who-is {:root P :sign-in G :session S}]) (:sign-ins w) (:sessions w)))]
       q)
     (when (#{:ask :answer-ask} kind)
       (for [U (:codes w)] [:ask-of U]))
     (when (#{:invite :enter :take-back :unlink} kind)
       (concat (for [C (hs/claims-of w)] [:invitation-of C])
               (for [P (distinct (remove nil? (conj (:persons w) person)))] [:accounts-of P]))))))

(defn- paired
  "The module's `accounts-of` read `got` with each link's id the reference's
  id that it is paired with in `links*`, pairing the links of the same place
  in the reference's read `want` when they have the same account and time.
  One id is paired with one id for good. Gives `got` as it is when the two
  do not pair."
  [links* want got]
  (let [ws (:accounts want)
        gs (:accounts got)]
    (if (and (vector? ws) (vector? gs) (= (count ws) (count gs))
             (every? true? (map (fn [a b] (= (dissoc a :link) (dissoc b :link))) ws gs)))
      (let [ok? (every? (fn [[a b]]
                          (let [m @links*
                                ref-of (some (fn [[r x]] (when (= x (:link b)) r)) m)]
                            (cond
                              (= (get m (:link a)) (:link b)) true
                              (or (contains? m (:link a)) ref-of) false
                              :else (do (swap! links* assoc (:link a) (:link b)) true))))
                        (map vector ws gs))]
        (if ok? (assoc got :accounts (mapv (fn [a b] (assoc b :link (:link a))) ws gs)) got))
      got)))

(defn- first-read-difference
  "Makes each read through a client picked at random, and gives the first
  whose result is not the reference's, with the times it was sent again; or
  nil, with the times."
  [impl clients ^Random rnd w state reads]
  (loop [reads reads, retries 0]
    (if-let [[kind arg] (first reads)]
      (let [want (read-of state kind arg)
            [got n] (send! impl (get (:calls impl) (call-name kind)) (clients (.nextInt rnd 2)) arg)
            got (if (= kind :accounts-of) (paired (:links* w) want got) got)]
        (if (= (comparable want) (comparable got))
          (recur (rest reads) (+ retries n))
          [{:what :a-read-is-not-the-reference's :read [kind arg] :reference want :module got} (+ retries n)]))
      [nil retries])))

(def ^:private empty-tally
  "What a history counts."
  {:histories 0 :steps 0 :writes 0 :repeats 0 :unfit 0 :reads 0 :watches 0 :watch-changes 0
   :retries 0 :deep 0 :too-deep 0 :linked 0 :unlinked 0 :taken-back 0})

(defn- step!
  "Takes one step of a history through the module's clients and the
  reference. Gives the world, the reference's state, the watches, what the
  step counted, and its difference, if any."
  [impl clients w state watches next-id step]
  (let [rnd ^Random (:rnd w)
        c (clients (.nextInt rnd 2))
        calls (:calls impl)]
    (case (:op step)
      :write
      (let [[kind input :as call] (:call step)
            [state' want] (r/step state call)
            sent (if (and (= kind :unlink) (map? input))
                   (update input :link #(get @(:links* w) % %))
                   input)
            [got tries] (send! impl (get calls (call-name kind)) c sent)
            changed (into {} (keep (fn [[id W]] (let [after (r/live-under state' (:root W))]
                                                   (when (not= after (r/live-under state (:root W)))
                                                     [id (assoc W :want after)]))))
                          watches)
            watches (merge watches changed)
            result-difference (when (not= (comparable want) (comparable got))
                                {:what :a-result-is-not-the-reference's :call call :reference want :module got})
            watch-difference (when-not result-difference (watch-difference impl watches true))
            reads (sweep w kind input want)
            [read-difference read-tries] (if (or result-difference watch-difference)
                                           [nil 0]
                                           (first-read-difference impl clients rnd w state' reads))
            taken? (not (or (contains? want :refused) (contains? want :could-not)))]
        {:w (follow-ups (if (and taken? (not (:repeat-of step))) (update w :taken conj {:call call :result want}) w)
                        state call want)
         :state state'
         :watches watches
         :tally {:writes 1 :repeats (if (:repeat-of step) 1 0) :unfit (if (:unfit? step) 1 0) :reads (count reads)
                 :watch-changes (count changed) :retries (+ tries read-tries)
                 :too-deep (if (= {:refused :too-deep} want) 1 0)
                 :linked (if (and (= kind :enter) (map? input)
                                  (not= (get-in state [:links (:account input) :link]) (get-in state' [:links (:account input) :link]))
                                  (get-in state' [:links (:account input)]))
                           1 0)
                 :unlinked (if (and (= kind :unlink) (not= state state')) 1 0)
                 :taken-back (if (and (= kind :take-back) (not= state state')) 1 0)}
         :difference (or result-difference watch-difference read-difference)})

      :read
      (let [[difference tries] (first-read-difference impl clients rnd w state [(:read step)])]
        {:w w :state state :watches watches :tally {:reads 1 :retries tries} :difference difference})

      :watch
      (let [P (:root step)
            [given rec] (watch! impl c P)
            fits? (uuid? P)]
        (cond
          (and fits? (stops? given))
          {:w w :state state
           :watches (assoc watches next-id {:root P :rec rec :stop given :want ::none :base (r/live-under state P)})
           :tally {:watches 1}}
          (and (not fits?) (= {:could-not :read-the-event} (comparable given)))
          {:w w :state state :watches watches :tally {:watches 1}}
          :else
          {:w w :state state :watches watches :tally {:watches 1}
           :difference {:what :watch-root-is-not-the-reference's :root P
                        :reference (if fits? "a function that stops the watch" {:could-not :read-the-event})
                        :module given}}))

      :stop
      (let [W (get watches (:watch step))]
        (try ((:stop W)) (catch Throwable _ nil))
        {:w w :state state :watches (dissoc watches (:watch step)) :tally {}}))))

(defn- run-history
  "Runs history `i` of the run of `seed` through the module's two clients and
  through the reference, from an empty state. Gives what it counted and its
  first difference, if any, with where it came."
  [impl clients seed i]
  (let [rnd0 (Random. (bit-xor (long (+ seed i)) 0x5DEECE66D))
        steps (+ 40 (.nextInt rnd0 81))
        deep-at (when (< (.nextDouble rnd0) 0.25) (+ 8 (.nextInt rnd0 32)))]
    (loop [k 0
           w (world seed i)
           state r/empty-state
           watches {}
           next-id 0
           deep? false
           tally (assoc empty-tally :histories 1)]
      (if (and (>= k steps) (empty? (:pending w)))
        (do (when (pos? (:settle-ms impl)) (Thread/sleep (long (:settle-ms impl))))
            (let [difference (watch-difference impl watches false)]
              (stop-all! watches)
              {:tally tally
               :difference (when difference (assoc difference :history i :seed (+ seed i) :step k))}))
        (let [start-deep? (and deep-at (not deep?) (>= k deep-at) (seq (hs/entered-persons state)))
              w (if start-deep?
                  (let [[w segment] (deep-steps w (hs/pick (:rnd w) (hs/entered-persons state)))]
                    (update w :pending into segment))
                  w)
              [w step] (next-step w state watches)
              out (step! impl clients w state watches next-id step)
              tally (merge-with + tally (:tally out) {:steps 1 :deep (if start-deep? 1 0)})]
          (if-let [difference (:difference out)]
            (do (stop-all! (:watches out))
                {:tally tally :difference (assoc difference :history i :seed (+ seed i) :step k :step-was step)})
            (recur (inc k) (:w out) (:state out) (:watches out)
                   (if (= :watch (:op step)) (inc next-id) next-id)
                   (or deep? start-deep?) tally)))))))

(defn- attempt
  "What `f` gives with `args`, or `{:threw ...}` when it throws."
  [f & args]
  (try (apply f args) (catch Throwable t {:threw (str t)})))

(defn- open-clients
  "Two clients of the module, opened through the contract, as two copies of
  the runtime; or what `open` gave in place of one."
  [impl]
  (let [open (get (:calls impl) 'open)]
    [(attempt open (:cluster impl) (:module-name impl)) (attempt open (:cluster impl) (:module-name impl))]))

(defn- run-histories
  "Runs histories `0` to `n` of `seed` through the module, and gives the sum
  of what they counted and their first differences, ten at most kept."
  [impl clients seed n]
  (reduce (fn [acc i]
            (let [{:keys [tally difference]} (run-history impl clients seed i)]
              (cond-> (update acc :tally #(merge-with + % tally))
                difference (update :differences (fn [ds] (if (< (count ds) 10) (conj ds difference) ds)))
                difference (update :histories-that-differ inc))))
          {:tally empty-tally :differences [] :histories-that-differ 0}
          (range n)))

;; ------------------------------------------------------------ open, close!

(defn- fixture
  "One call of each write and read for a person of its own, made from
  `seed`, so that no history's ids meet it: her invitation, her enter, a
  sign-in, a session, an agent, an ask."
  [seed]
  (let [rnd (Random. (bit-xor (long seed) 0xC105ED))
        id #(UUID. (.nextLong rnd) (.nextLong rnd))
        P (id) G (id) S (id) A (id)
        n (mod seed 0x40000000)
        code-hash (hs/h (+ (* n 1000000) 999999))]
    {:person P :sign-in G
     :invite {:person P :claim {:by :code :hash code-hash} :invited-by :system :at 100 :ends 100000 :op-id (id)}
     :enter {:account [:google (str "close." seed)] :says {} :email-hash nil :code-hash code-hash
             :own-lane (id) :making {:uuid (id) :session (id)} :at 110}
     :take-back {:person P :claim {:by :code :hash code-hash} :invite (id) :at 120}
     :unlink {:account [:google (str "close." seed)] :link (id) :person P :at 120}
     :own-lane-made {:person P :at 120}
     :begin-sign-in {:root P :actor P :sign-in G :kind :browser :hash (hs/h 77) :until 100000 :label nil :at 130}
     :replace-secret {:root P :sign-in G :old-hash (hs/h 77) :new-hash (hs/h 78) :until 200000
                      :previous-until 150000 :at 140}
     :end-sign-in {:root P :sign-in G :at 150}
     :make-actor {:root P :actor A :made-by P :label "an agent" :at 130}
     :end-actor {:root P :actor A :at 150}
     :open-session {:root P :sign-in G :session S :until 50000 :at 140}
     :close-session {:root P :session S :at 150}
     :ask {:code (format "%08X" (bit-xor (* 4 n) 3)) :hash (hs/h 79) :asking-hash (hs/h 80) :asks "a program"
           :ends 1000 :at 130}
     :answer-ask {:code (format "%08X" (bit-xor (* 4 n) 3)) :yes? false :root P :actor P :new-agent nil
                  :sign-in (id) :until 100000 :at 140}
     :who-is {:root P :sign-in G :session nil}}))

(defn- open-and-close-checks
  "The checks of `open` and `close!`, through the contract, on a person of
  their own."
  [impl seed]
  (let [{:keys [calls cluster module-name]} impl
        open (get calls 'open)
        close! (get calls 'close!)
        fx (fixture seed)]
    (check "open of a name that the cluster does not hold, and of a thing that is no cluster, gives {:could-not :find-the-module}."
           (= [{:could-not :find-the-module} {:could-not :find-the-module}]
              (map comparable [(attempt open cluster (str module-name ".not-there")) (attempt open :not-a-cluster module-name)])))
    (let [a (attempt open cluster module-name)
          b (attempt open cluster module-name)
          [_ _] (send! impl (get calls 'invite) a (:invite fx))
          [entered _] (send! impl (get calls 'enter) a (:enter fx))
          [stop rec] (watch! impl b (:person fx))
          closed (attempt close! b)
          on-closed (vec (concat (for [kind hs/write-kinds] (comparable ((get calls (call-name kind)) b (get fx kind))))
                                 [(comparable ((get calls 'who-is) b (:who-is fx)))
                                  (comparable ((get calls 'ask-of) b (get-in fx [:ask :code])))
                                  (comparable ((get calls 'under) b (:person fx)))
                                  (comparable ((get calls 'invitation-of) b (get-in fx [:invite :claim])))
                                  (comparable ((get calls 'accounts-of) b (:person fx)))
                                  (comparable ((get calls 'watch-root) b (:person fx) (fn [_] nil)))]))
          again (attempt close! b)
          [later _] (send! impl (get calls 'begin-sign-in) a (:begin-sign-in fx))
          _ (Thread/sleep (long (max 200 (:settle-ms impl))))
          seen @rec]
      (check "After close!, each of the twenty calls on that client gives {:could-not :closed}, watch-root's in place of the function; close! gives nil, also again; no function that it watched with is called again; and the other client goes on."
             (and (= (:person fx) (:person entered))
                  (stops? stop)
                  (nil? closed) (nil? again)
                  (= 20 (count on-closed))
                  (every? #(= {:could-not :closed} %) on-closed)
                  (= {:begun (:sign-in fx)} (comparable later))
                  (= ::none (:last seen)) (zero? (:calls seen)))
             [on-closed later seen])
      (attempt close! a))))

;; ------------------------------------------------------------------ the run

(def ^:private unseen
  "The broken copies that no call of the contract can show, each with why."
  {:says-of-every-enter
   "no result and no read of the contract gives :says, so a link's :says can be held against the spec only by reading the module's tables"})

(defn- history-check
  "Runs `n` histories of `seed` through the module and says whether it gave
  what the reference gives in all of them."
  [impl seed n]
  (let [clients (open-clients impl)]
    (if-not (every? #(nil? (could-not-word %)) clients)
      (check "open gives two clients of the module." false clients)
      (let [{:keys [tally differences histories-that-differ]} (run-histories impl clients seed n)
            {:keys [steps writes repeats unfit reads watches watch-changes retries deep too-deep linked unlinked taken-back]} tally]
        (doseq [c clients] (attempt (get (:calls impl) 'close!) c))
        (check (str "The module, " (:name impl) ", gave what the reference gives, through the contract alone, in " n
                    " generated histories of " steps " steps: every result of " writes " writes (" repeats
                    " of them repeats, " unfit " whose input does not fit), every one of " reads
                    " reads of who-is, ask-of, under, invitation-of and accounts-of, and what each of " watches
                    " watches came to after each of the " watch-changes " changes under its root; with " deep
                    " deep segments of 32 agents in a line, " too-deep " times :too-deep, " linked " links made, " unlinked
                    " removed and " taken-back " invitations taken back, and " retries
                    " calls sent again after a :could-not that said nothing.")
               (zero? histories-that-differ)
               (when (pos? histories-that-differ)
                 (cons (str histories-that-differ " histories differ; the first of each, ten at most:") differences)))))))

(defn- broken-checks
  "Runs, for each broken copy of the reference, the histories of `seed`
  through it until one differs from the reference, `n` at most, and says
  whether one did. A copy that no call of the contract can show is said so,
  and counted neither way."
  [seed n]
  (doseq [[broken how] (sort-by key r/breaks)]
    (let [impl (the-reference broken)
          clients (open-clients impl)
          found (some (fn [i] (:difference (run-history impl clients seed i))) (range n))
          where (when found (str "history " (:history found) ", step " (:step found) ", " (name (:what found))))]
      (if-let [why (get unseen broken)]
        (println (if found "CAUGHT, though no call should show it:" "NOT CAUGHT, as no run through the contract can:")
                 (str "the copy in which " how "; " why "."))
        (check (str "Through the contract alone, the run catches the copy of the rules in which " how
                    (when where (str ": " where)) ".")
               (some? found)
               (when-not found (str "not caught in " n " histories")))))))

(defn- broken-client-checks
  "Runs, for each broken client, the histories of `seed` through it, over a
  reference of its own, until one differs from the reference, `n` at most,
  and says whether one did."
  [seed n]
  (doseq [[how break] broken-clients]
    (let [impl (-> (the-reference) break (assoc :watch-bound-ms 50))
          clients (open-clients impl)
          found (some (fn [i] (:difference (run-history impl clients seed i))) (range n))]
      (check (str "Through the contract alone, the run catches a client over the reference that " how
                  (when found (str ": history " (:history found) ", step " (:step found) ", " (name (:what found)))) ".")
             (some? found)
             (when-not found (str "not caught in " n " histories"))))))

(defn- timed
  "Runs `f` and prints how long it took, under `what`."
  [what f]
  (let [started (System/nanoTime)]
    (f)
    (println (format "(%s took %.1f s.)" what (/ (- (System/nanoTime) started) 1e9)))))

(defn -main
  "Runs the checks, prints the counts, and ends the process with a status:
  0 when no check failed. Arguments: the seed, which the clock gives when
  it is left out; how many histories, 1,000 when left out; and the full name
  of a function of no argument that gives the module. When the module is
  left out, the run holds the reference itself, as it is, with late watches
  and with lost answers, and then each broken copy of it."
  [& args]
  (let [seed (or (some-> (first args) parse-long) (System/currentTimeMillis))
        n (or (some-> (second args) parse-long) 1000)
        module-fn (some-> (nth args 2 nil) symbol)]
    (println "Seed of the histories:" seed)
    (if module-fn
      (let [impl ((requiring-resolve module-fn))]
        (println "The module:" (:name impl))
        (timed "The module's histories" #(history-check impl seed n))
        (open-and-close-checks impl seed)
        (when-let [close (:close impl)] (close)))
      (do
        (println "The module: the reference itself, three ways, and then each broken copy of it.")
        (timed "The histories" #(history-check (the-reference) seed n))
        (open-and-close-checks (the-reference) seed)
        (timed "The histories with late watches" #(history-check (late-watches (the-reference) 3 seed) seed n))
        (timed "The histories with lost answers" #(history-check (lossy (the-reference) 20 seed) seed n))
        (println)
        (println "Each broken copy of the reference, as the module:")
        (timed "The broken copies" #(broken-checks seed n))
        (println)
        (println "Each broken client over the reference, as the module:")
        (timed "The broken clients" #(broken-client-checks seed n))))
    (let [{:keys [passed failed]} @counts]
      (println)
      (println (str passed " passed, " failed " failed."))
      (flush)
      (shutdown-agents)
      (System/exit (if (zero? failed) 0 1)))))
