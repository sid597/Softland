(ns softland.auth.histories
  "The generator of the histories that the checks of Auth send: random
  sequences of calls of the fourteen writes, made to meet the state often,
  with repeats of calls that were taken, inputs that do not fit, and a
  clock that moves on and now and then goes back, as a slower copy of the
  runtime would; and now and then one write whose `:at` is far ahead, as a
  copy whose clock is wrong would send it, after which the clock is where
  it was. Two checks use it: the reference's proof
  (`softland.auth.reference-proof`), which holds each step against rules of
  its own, and the run that holds a module against the reference
  (`softland.auth.module-check`).

  A world is what the runtime of one history holds: its random source, its
  pools of ids, its clock, and the calls that were taken. The generator
  reads the reference's state, whose form `softland.auth.reference` gives,
  to choose parts that meet it; it never judges a result.

  State. None of its own: a world is a value, and its random source is the
  one thing that it moves."
  (:require [softland.auth.reference :as r])
  (:import [java.util Random UUID]))

(defn h
  "A hash made from the number `n`: 64 lower-case hex characters."
  [n]
  (format "%064x" (long n)))

(defn text-of
  "A string of `n` characters."
  [n]
  (apply str (repeat n "x")))

(def write-kinds
  "The fourteen writes."
  [:invite :enter :take-back :unlink :own-lane-made :begin-sign-in :replace-secret :end-sign-in :make-actor
   :end-actor :open-session :close-session :ask :answer-ask])

(def weighted-kinds
  "The writes that a history makes, each as many times as its weight."
  (vec (mapcat (fn [[k n]] (repeat n k))
               [[:invite 6] [:enter 8] [:take-back 3] [:unlink 3] [:own-lane-made 3] [:begin-sign-in 12] [:replace-secret 8]
                [:end-sign-in 5] [:make-actor 10] [:end-actor 6] [:open-session 14] [:close-session 4]
                [:ask 5] [:answer-ask 9]])))

(defn pick
  "One of `xs`, by `rnd`."
  [^Random rnd xs]
  (nth xs (.nextInt rnd (count xs))))

(defn chance?
  "True with the chance `p`, by `rnd`."
  [^Random rnd p]
  (< (.nextDouble rnd) p))

(defn uuid-of
  "A UUID made by `rnd`."
  [^Random rnd]
  (UUID. (.nextLong rnd) (.nextLong rnd)))

(defn new-world
  "What the runtime of one history holds: its random source, a few ids of
  each kind, so that they meet often, its clock, the count of hashes made,
  the calls that were taken, and the links that it saw stand, so that an
  unlink can name one that stands no more. An email is its hash: the
  runtime makes it, and Auth is given nothing else."
  [seed]
  (let [rnd (Random. (long seed))]
    {:rnd rnd
     :persons (vec (repeatedly 4 #(uuid-of rnd)))
     :accounts [[:google "1"] [:google "2"] [:github "1"] [:google "3"]]
     :email-hashes [(h 2001) (h 2002) (h 2003)]
     :code-hashes [(h 1001) (h 1002) (h 1003)]
     :agents (vec (repeatedly 6 #(uuid-of rnd)))
     :sign-ins (vec (repeatedly 6 #(uuid-of rnd)))
     :sessions (vec (repeatedly 6 #(uuid-of rnd)))
     :codes ["CODE0001" "CODE0002" "CODE0003"]
     :now 1000
     :fresh 0
     :taken []
     :links-seen []}))

(defn tick
  "Moves the world's clock: mostly on, by steps of ten that often land on
  an end; now and then back, as a copy whose clock is behind."
  [w]
  (let [rnd (:rnd w)]
    (if (chance? rnd 0.06)
      (update w :now - (pick rnd [10 20]))
      (update w :now + (pick rnd [0 0 10 10 10 20 20 30 50 100 300])))))

(defn end-after
  "An end near the world's now: before it, at it, just after it, or far
  enough after it that the thing lives through many steps."
  [w]
  (+ (:now w) (pick (:rnd w) [-20 -10 0 10 20 30 50 100 200 300 500 1000 2000 5000 10000])))

(defn fresh-hash
  "A hash that the history has not made before, and the world that counts it.
  A world that holds a `:hash-base` makes its hashes from there, so that no
  other history's hashes are the same."
  [w]
  [(update w :fresh inc) (h (+ (get w :hash-base 5000000) (:fresh w)))])

(defn entered-persons
  "The persons of the state who have entered."
  [state]
  (vec (sort (keys (:persons state)))))

(defn claims-of
  "Every claim of the world's pools."
  [w]
  (into [] (concat (map (fn [x] {:by :email :hash x}) (:email-hashes w))
                   (map (fn [x] {:by :code :hash x}) (:code-hashes w)))))

(defn invitation-end
  "An end for an invitation: mostly one that lives through many steps, now
  and then one that is over soon, at once or from its making."
  [w]
  (+ (:now w) (pick (:rnd w) [-10 0 10 30 100 300 1000 5000 100000 100000])))

(defn agents-of
  "The agents of the root `P` in the state, in the order in which they were made."
  [state P]
  (get-in state [:roots P :agent-order] []))

(defn sign-ins-of
  "The sign-ins of the root `P` in the state, ended ones too, in the order in which they began."
  [state P]
  (get-in state [:roots P :sign-in-order] []))

(defn sessions-of
  "The sessions of the root `P` in the state, closed ones too."
  [state P]
  (vec (sort (keys (get-in state [:roots P :sessions])))))

(defn live-sign-ins-of
  "The sign-ins of the root `P` that nobody ended and whose time is after
  `now`, so that a call at `now` finds them live, watched or not."
  [state P now]
  (vec (sort (keep (fn [[G g]] (when (and (not (:ended? g)) (< now (:until g))) G))
                   (get-in state [:roots P :sign-ins])))))

(defn a-root
  "A root for a call: mostly a person who has entered."
  [w state]
  (let [rnd (:rnd w)
        in (entered-persons state)]
    (if (and (seq in) (chance? rnd 0.88)) (pick rnd in) (pick rnd (:persons w)))))

(defn one-of
  "One of `xs` with the chance `p` when there are any, else one of `pool`."
  [w xs p pool]
  (if (and (seq xs) (chance? (:rnd w) p)) (pick (:rnd w) xs) (pick (:rnd w) pool)))

(defn a-signed-in-root
  "A root for a call that wants a live sign-in: mostly one that has one."
  [w state]
  (let [roots (vec (sort (filter #(seq (live-sign-ins-of state % (:now w))) (keys (:roots state)))))]
    (if (and (seq roots) (chance? (:rnd w) 0.75)) (pick (:rnd w) roots) (a-root w state))))

(defn a-sign-in
  "A sign-in id for a call under `P`: mostly a live one, else one of hers, else any."
  [w state P]
  (let [live (live-sign-ins-of state P (:now w))]
    (if (and (seq live) (chance? (:rnd w) 0.8))
      (pick (:rnd w) live)
      (one-of w (sign-ins-of state P) 0.6 (:sign-ins w)))))

(defn make-call
  "A call of the write `kind` that fits, with parts chosen to meet the state
  often. Gives the world, which may have counted a fresh hash, and the call."
  [w state kind]
  (let [rnd (:rnd w)
        now (:now w)]
    (case kind
      :invite (let [claims (claims-of w)
                    fresh-persons (vec (remove #(contains? (:persons state) %) (:persons w)))
                    fresh-claims (vec (remove #(contains? (:claims state) %) claims))
                    fresh? (and (seq fresh-persons) (seq fresh-claims) (chance? rnd 0.5))
                    ;; Now and then a person who has entered, under a claim that
                    ;; names her already, as she adds an account.
                    own (vec (sort-by str (keep (fn [[claim c]] (when (contains? (:persons state) (:person c)) [(:person c) claim]))
                                                (:claims state))))
                    [P claim] (if (and (seq own) (chance? rnd 0.2))
                                (pick rnd own)
                                [(pick rnd (if fresh? fresh-persons (:persons w))) (pick rnd (if fresh? fresh-claims claims))])]
                [w [:invite {:person P
                             :claim claim
                             :invited-by (if (chance? rnd 0.5) :system (uuid-of rnd))
                             :at now
                             :ends (invitation-end w)
                             :op-id (uuid-of rnd)}]])
      :enter (let [waiting (vec (sort-by str (keep (fn [[claim c]] (when (:waiting c) claim)) (:claims state))))
                   proving (when (and (seq waiting) (chance? rnd 0.5)) (pick rnd waiting))
                   ;; Mostly an account that has no link when one proves a claim.
                   free (vec (remove #(contains? (:links state) %) (:accounts w)))
                   account (if (and proving (seq free) (chance? rnd 0.7)) (pick rnd free) (pick rnd (:accounts w)))]
               [w [:enter {:account account
                           :says (pick rnd [{} {"name" "Asha"} {"name" "Bo" "picture" "https://p/1"}])
                           :email-hash (cond (= :email (:by proving)) (:hash proving)
                                             (chance? rnd 0.6) (pick rnd (:email-hashes w)))
                           :code-hash (cond (= :code (:by proving)) (:hash proving)
                                            (chance? rnd 0.6) (pick rnd (:code-hashes w)))
                           :own-lane (uuid-of rnd)
                           :making {:uuid (uuid-of rnd) :session (uuid-of rnd)}
                           :at now}]])
      :take-back (let [waiting (vec (sort-by str (keep (fn [[claim c]] (when (:waiting c) [claim c])) (:claims state))))
                       ops (vec (keep (fn [{[k x] :call}] (when (= k :invite) x)) (:taken w)))
                       [claim I P] (cond
                                     (and (seq waiting) (chance? rnd 0.7))
                                     (let [[claim c] (pick rnd waiting)]
                                       [claim (get-in c [:waiting :invite]) (:person c)])
                                     (and (seq ops) (chance? rnd 0.8))
                                     (let [x (pick rnd ops)] [(:claim x) (:op-id x) (:person x)])
                                     :else [(pick rnd (claims-of w)) (uuid-of rnd) (pick rnd (:persons w))])]
                   [w [:take-back {:person P :claim claim :invite I :at now}]])
      :unlink (let [links (vec (sort-by str (:links state)))
                    w (update w :links-seen (fn [seen] (into seen (comp (map (comp :link second)) (remove (set seen))) links)))
                    [account L P] (cond
                                    (and (seq links) (chance? rnd 0.75))
                                    (let [[account l] (pick rnd links)]
                                      [account (:link l) (if (chance? rnd 0.9) (:person l) (pick rnd (:persons w)))])
                                    (and (seq (:links-seen w)) (chance? rnd 0.7))
                                    [(pick rnd (:accounts w)) (pick rnd (:links-seen w)) (a-root w state)]
                                    :else [(pick rnd (:accounts w)) (uuid-of rnd) (a-root w state)])]
                [w [:unlink {:account account :link L :person P :at now}]])
      :own-lane-made [w [:own-lane-made {:person (a-root w state) :at now}]]
      :begin-sign-in (let [P (a-root w state)
                           [w H] (fresh-hash w)]
                       [w [:begin-sign-in {:root P :actor (one-of w (conj (agents-of state P) P) 0.9 (:agents w))
                                           :sign-in (pick rnd (:sign-ins w)) :kind (pick rnd [:browser :program])
                                           :hash H :until (end-after w) :label (pick rnd [nil "laptop" "phone"])
                                           :at now}]])
      :replace-secret (let [P (a-signed-in-root w state)
                            G (a-sign-in w state P)
                            g (get-in state [:roots P :sign-ins G])
                            ;; Now and then the hash before the present one comes back.
                            [w H'] (if (and (:previous g) (chance? rnd 0.2))
                                     [w (get-in g [:previous :hash])]
                                     (fresh-hash w))
                            old (cond (and g (chance? rnd 0.8)) (:hash g)
                                      (and (:previous g) (chance? rnd 0.5)) (get-in g [:previous :hash])
                                      :else (h (pick rnd [7 8 9])))]
                        [w [:replace-secret {:root P :sign-in G :old-hash old :new-hash H' :until (end-after w)
                                             :previous-until (end-after w) :at now}]])
      :end-sign-in (let [P (a-root w state)]
                     [w [:end-sign-in {:root P :sign-in (a-sign-in w state P) :at now}]])
      :make-actor (let [P (a-root w state)
                        agents (agents-of state P)
                        maker (if (and (seq agents) (chance? rnd 0.4))
                                (peek agents)
                                (one-of w (conj agents P) 0.92 (:agents w)))]
                    [w [:make-actor {:root P :actor (pick rnd (:agents w)) :made-by maker
                                     :label (pick rnd ["importer" "runner"]) :at now}]])
      :end-actor (let [P (a-root w state)
                       agents (agents-of state P)
                       signed-in (vec (sort (disj (into #{} (keep (fn [g] (when-not (:ended? g) (:actor g))))
                                                         (vals (get-in state [:roots P :sign-ins])))
                                                   P)))]
                   [w [:end-actor {:root P
                                   :actor (cond (chance? rnd 0.08) P
                                                (and (seq signed-in) (chance? rnd 0.5)) (pick rnd signed-in)
                                                (and (seq agents) (chance? rnd 0.5)) (pick rnd (subvec agents 0 (quot (inc (count agents)) 2)))
                                                :else (one-of w agents 0.85 (:agents w)))
                                   :at now}]])
      :open-session (let [P (a-signed-in-root w state)
                          input {:root P :sign-in (a-sign-in w state P) :session (pick rnd (:sessions w))
                                 :until (end-after w) :at now}]
                      [w [:open-session (case (.nextInt rnd 4)
                                          0 input
                                          1 (assoc input :driven-by nil)
                                          2 (assoc input :driven-by {:kind :person})
                                          (assoc input :driven-by {:kind :model :model (pick rnd ["claude-opus-5-5" "a-small-model"])}))]])
      :close-session (let [P (a-root w state)
                           open (vec (sort (keep (fn [[S s]] (when-not (:closed? s) S))
                                                 (get-in state [:roots P :sessions]))))]
                       [w [:close-session {:root P
                                           :session (if (and (seq open) (chance? rnd 0.7))
                                                      (pick rnd open)
                                                      (one-of w (sessions-of state P) 0.85 (:sessions w)))
                                           :at now}]])
      :ask (let [U (pick rnd (:codes w))
                 k (get-in state [:asks U])
                 ;; Now and then the code's own hash with another asking hash,
                 ;; which is another ask.
                 [w H] (if (and k (chance? rnd 0.3)) [w (:hash k)] (fresh-hash w))
                 [w H2] (fresh-hash w)]
             [w [:ask {:code U :hash H :asking-hash H2 :asks (pick rnd ["an importer" "a runner"])
                       :ends (end-after w) :at now}]])
      :answer-ask (let [P (a-root w state)
                        U (pick rnd (:codes w))
                        yes? (chance? rnd 0.8)
                        how (.nextInt rnd 3)
                        actor (case how
                                0 P
                                1 (pick rnd (:agents w))
                                (one-of w (agents-of state P) 0.85 (:agents w)))
                        ends (get-in state [:asks U :ends])]
                    [w [:answer-ask {:code U :yes? yes? :root P :actor actor
                                     :new-agent (when (and yes? (= how 1)) {:label "from an ask"})
                                     :sign-in (pick rnd (:sign-ins w)) :until (end-after w)
                                     ;; Now and then at the ask's very end, as a copy whose clock is there.
                                     :at (if (and ends (chance? rnd 0.15)) ends now)}]]))))

(defn vary-dropped
  "The input of a repeat with the parts that the spec drops from a repeat
  made other."
  [w kind input]
  (let [rnd (:rnd w)]
    (case kind
      :invite (assoc input :invited-by (uuid-of rnd) :ends (invitation-end w))
      :enter (assoc input :says {"name" "Another"} :own-lane (uuid-of rnd)
                    :making {:uuid (uuid-of rnd) :session (uuid-of rnd)}
                    :email-hash (pick rnd (:email-hashes w)) :code-hash (pick rnd (:code-hashes w)))
      :begin-sign-in (assoc input :until (end-after w) :label "another")
      :replace-secret (assoc input :until (end-after w) :previous-until (end-after w))
      :make-actor (assoc input :label "another")
      :open-session (assoc input :until (end-after w) :driven-by {:kind :model :model "another"})
      :ask (assoc input :asks "another" :ends (end-after w))
      :answer-ask (cond-> (assoc input :until (end-after w))
                    (:new-agent input) (assoc :new-agent {:label "another"}))
      input)))

(defn older-replacements
  "The indexes of the taken replacements of the world after which the same
  sign-in was replaced again: sent again, each is a replacement that
  arrives after a newer one."
  [w]
  (let [taken (:taken w)
        replaced (keep-indexed (fn [i {[kind input] :call}]
                                 (when (= kind :replace-secret) [i [(:root input) (:sign-in input)]]))
                               taken)]
    (vec (for [[i where] replaced
               :when (some (fn [[k other]] (and (> k i) (= other where))) replaced)]
           i))))

(defn a-repeat
  "A call that was taken, sent again: now and then a replacement after which
  its sign-in was replaced again; else one of the last few, or one of a
  write picked evenly among the writes that were taken, so that the many
  invites and asks of a history's start do not crowd out the rest. Mostly
  at the world's now and not its own time, and half the time with the
  parts that a repeat drops made other. Gives the world, the call, and the
  index of the call that was taken first."
  [w]
  (let [rnd (:rnd w)
        taken (:taken w)
        n (count taken)
        older (when (chance? rnd 0.2) (older-replacements w))
        j (cond (seq older) (pick rnd older)
                (chance? rnd 0.4) (- n 1 (.nextInt rnd (min 6 n)))
                :else (let [by-kind (group-by (fn [i] (first (:call (nth taken i)))) (range n))]
                        (pick rnd (get by-kind (pick rnd (vec (sort (keys by-kind))))))))
        [kind input] (:call (nth (:taken w) j))
        input (cond-> input (chance? rnd 0.7) (assoc :at (:now w)))
        input (if (chance? rnd 0.5) (vary-dropped w kind input) input)]
    [w [kind input] j]))

(defn unfit-parts
  "For each write, parts that make its input not fit. Among them, strings
  that UTF-8 cannot carry: each holds a surrogate character without its
  pair (`lone`, a high one, and `lone-low`)."
  [w]
  (let [rnd (:rnd w)
        long-text (text-of 257)
        lone (str (char 0xD800))
        lone-low (str (char 0xDC00))]
    {:invite [{:person "a person"} {:claim {:by :email :email "a@x.org"}} {:claim {:by :phone :phone "1"}}
              {:claim {:by :code :hash "abc"}} {:claim {:by :email :hash (h 1) :more 1}} {:invited-by "someone"}
              {:claim {:by :email :hash (apply str (repeat 64 "A"))}} {:ends "later"} {:ends nil}
              {:op-id (str (uuid-of rnd))} {:op-id nil}]
     :take-back [{:invite "an invite"} {:invite nil} {:claim {:by :email :email "a@x.org"}} {:person nil}]
     :unlink [{:link "a link"} {:link nil} {:account [:google]} {:person "a person"}
              {:account [:google (str lone "1")]}]
     :enter [{:account [:google]} {:account (list :google "1")} {:account ["google" "1"]}
             {:account [:google long-text]} {:says {"k" (text-of 2049)}}
             {:says (into {} (map (fn [i] [(str i) "v"])) (range 33))} {:says {:k "v"}} {:says nil}
             {:email "a@x.org"} {:email-hash 7} {:email-hash "a@x.org"} {:code-hash (apply str (repeat 64 "A"))}
             {:making {:uuid (uuid-of rnd)}} {:account [(keyword long-text) "1"]} {:own-lane "a lane"}
             {:own-lane nil} {:account [:google (str lone "1")]} {:account [:google (str "1" lone-low)]}
             {:account [(keyword (str "goo" lone "gle")) "1"]} {:says {"name" (str "A" lone "sha")}}
             {:says {(str "na" lone-low "me") "Asha"}}]
     :own-lane-made [{:person "a person"}]
     :begin-sign-in [{:kind :robot} {:hash (apply str (repeat 64 "A"))} {:hash (apply str (repeat 63 "a"))}
                     {:label long-text} {:until "5"} {:root "a root"} {:label (str "lap" lone "top")}]
     :replace-secret [{:old-hash nil} {:new-hash "x"} {:previous-until 1.5}]
     :end-sign-in [{:sign-in "a sign-in"}]
     :make-actor [{:label nil} {:label long-text} {:made-by :system} {:label (str lone-low "runner")}]
     :end-actor [{:actor "an actor"}]
     :open-session [{:driven-by {:kind :model}} {:driven-by {:kind :model :model long-text}}
                    {:driven-by {:kind :person :model "x"}} {:driven-by "a person"} {:session 5}
                    {:driven-by {:kind :model :model (str "model" lone)}}]
     :close-session [{:session "a session"}]
     :ask [{:code "CODE001"} {:code "CODE00011"} {:asks long-text} {:ends nil} {:asks (str "asks" lone)}
           {:code (str "CODE00" lone "1")}]
     :answer-ask [{:yes? "yes"} {:new-agent {:label nil}} {:new-agent {:label "x" :more 1}} {:code 7} {:yes? nil}
                  {:new-agent {:label (str "x" lone)}}
                  {:yes? false :actor "an actor"} {:yes? false :sign-in nil} {:yes? false :until "later"}]}))

(defn unfit-call
  "A call whose input does not fit: a call that fits, with a part made
  wrong, left out or added, or a call of no form at all."
  [w state]
  (let [rnd (:rnd w)
        kind (pick rnd write-kinds)
        [w [_ input]] (make-call w state kind)
        special (get (unfit-parts w) kind)
        call (if (chance? rnd 0.6)
               [kind (merge input (pick rnd special))]
               (case (.nextInt rnd 4)
                 0 [kind (dissoc input (pick rnd (vec (sort (remove #{:driven-by} (keys input))))))]
                 1 [kind (assoc input :and-more 1)]
                 2 [kind (assoc input :at (pick rnd [(str (:at input)) (double (:at input)) (bigint (:at input)) nil]))]
                 (pick rnd [[kind] [kind input :more] (list kind input) [:fly input] [kind [input]] {kind input} nil "a write"])))]
    [w call]))

(defn unfit-input
  "An input that does not fit, for a write that it picks, as a client of the
  contract is given one: an input that fits, with a part made wrong, left
  out or added, or a time of another kind; or a thing that is no map. Gives
  the world, the write and the input."
  [w state]
  (let [rnd (:rnd w)
        kind (pick rnd write-kinds)
        [w [_ input]] (make-call w state kind)
        special (get (unfit-parts w) kind)
        input (if (chance? rnd 0.6)
                (merge input (pick rnd special))
                (case (.nextInt rnd 4)
                  0 (dissoc input (pick rnd (vec (sort (remove #{:driven-by} (keys input))))))
                  1 (assoc input :and-more 1)
                  2 (assoc input :at (pick rnd [(str (:at input)) (double (:at input)) (bigint (:at input)) nil]))
                  (pick rnd [nil 7 "a write" [input] (seq input) (dissoc (into (sorted-map) input) :at)])))]
    [w kind input]))

(def far-ahead
  "How far ahead the clock of a copy that is wrong is: past every end that
  a history gives a thing."
  1000000)

(defn from-a-clock-far-ahead
  "The call, now and then with its `:at` far ahead of the world's now."
  [w [kind input :as call]]
  (if (and (map? input) (contains? input :at) (chance? (:rnd w) 0.04))
    [kind (assoc input :at (+ (:now w) far-ahead))]
    call))

(defn next-call
  "The next call of a history: moves the clock, then makes a repeat of a
  taken call, a call that does not fit, or a call that fits, now and then
  from a clock far ahead. Gives the world, the call, and what the history
  knows of it: whether it fits, and of which taken call it is a repeat."
  [w state]
  (let [w (tick w)
        rnd (:rnd w)
        roll (.nextDouble ^Random rnd)]
    (cond
      (and (< roll 0.13) (seq (:taken w))) (let [[w call j] (a-repeat w)] [w call {:fits? true :repeat-of j}])
      (< roll 0.19) (let [[w call] (unfit-call w state)] [w call {:fits? false}])
      :else (let [kind (if (and (empty? (entered-persons state)) (chance? rnd 0.5))
                         ;; Until someone has entered, most writes are refused: get there sooner.
                         (pick rnd [:invite :enter])
                         (pick rnd weighted-kinds))
                  [w call] (make-call w state kind)]
              [w (from-a-clock-far-ahead w call) {:fits? true}]))))
