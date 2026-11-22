(ns softland.auth.reference-proof
  "The proof that holds the reference of Auth's contract
  (`softland.auth.reference`) to its spec,
  `src/proposal/runtime-2026-11-21/spine/auth/SPEC.md`. It needs no cluster.
  One command, from the repository root:

    clojure -M:auth -m softland.auth.reference-proof [seed]

  It checks three ways.

  - By cases: one check for each sentence of the spec that says what a call
    gives or refuses, and the clients that `open` gives over a cluster of
    the reference, two copies of the runtime at one moment among them.
  - By generated histories: thousands of random sequences of calls, from a
    seed that the run prints, with repeats of calls, inputs that do not fit
    and times that pass, some going back as a slower clock would. After
    every step, each rule of `invariants` is checked, and a check of its
    own says whether it held in every history.
  - By broken copies: for each key of `reference/breaks`, the same
    histories are run against the copy of the rules with that one rule
    broken, and must catch it.

  With no seed given, the seed is the clock's; given one, the run makes the
  same histories again. The checks of a history read the reference's state
  map, whose form its namespace gives, where no read of the contract shows
  the fact (who is linked to which account, which claim names whom).

  State. It changes nothing outside its own process."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [softland.auth.histories :refer [h text-of new-world next-call sign-ins-of sessions-of]]
            [softland.auth.reference :as r])
  (:import [java.util Random UUID]
           [java.util.concurrent CountDownLatch]))

;; ---------------------------------------------------------- saying a check

(def ^:private counts
  "The counts of this run: how many checks passed and failed."
  (atom {:passed 0 :failed 0}))

(defn- check
  "Takes a sentence that says what must hold, whether it held, and when the
  caller wants, what was seen. Prints one line, `PASSED` or `FAILED` before
  the sentence, and for a failure what was seen, cut short; counts it; and
  gives whether it held."
  ([sentence held?] (check sentence held? nil))
  ([sentence held? seen]
   (if held?
     (do (swap! counts update :passed inc)
         (println "PASSED" sentence))
     (do (swap! counts update :failed inc)
         (println "FAILED" sentence)
         (when (some? seen)
           (println "       seen:" (binding [*print-length* 12 *print-level* 6] (pr-str seen))))))
   (boolean held?)))

(defn- finish
  "Prints the counts of the run and ends the process: status 0 when no
  check failed, 1 otherwise."
  []
  (let [{:keys [passed failed]} @counts]
    (println)
    (println (str passed " passed, " failed " failed."))
    (flush)
    (shutdown-agents)
    (System/exit (if (zero? failed) 0 1))))

;; ------------------------------------------------------------- the fixtures

(defn- id
  "A UUID that is the same at every run, made from the number `n`."
  [n]
  (UUID. 0x5AF7 (long n)))

(def ^:private asha (id 1))
(def ^:private bo (id 2))
(def ^:private cy (id 3))
(def ^:private agent-1 (id 11))
(def ^:private agent-2 (id 12))
(def ^:private agent-3 (id 13))
(def ^:private sign-in-1 (id 21))
(def ^:private sign-in-2 (id 22))
(def ^:private sign-in-3 (id 23))
(def ^:private session-1 (id 31))
(def ^:private session-2 (id 32))
(def ^:private layer-1 (id 41))
(def ^:private layer-2 (id 42))
(def ^:private making-1 {:uuid (id 51) :session (id 52)})
(def ^:private making-2 {:uuid (id 53) :session (id 54)})
(def ^:private asha-google [:google "108200000000000000001"])
(def ^:private bo-google [:google "208300000000000000002"])
(def ^:private asha-code {:by :code :hash (h 1)})
(def ^:private bo-email {:by :email :email "bo@example.org"})
(def ^:private code-1 "ABCD2345")

(def ^:private auth-name
  "The name under which the proof launches Auth in a cluster of the
  reference: any string does."
  "Auth")

(defn- new-client
  "A client of a new Auth with nothing written in it."
  []
  (r/open (r/cluster auth-name) auth-name))

(defn- two-clients
  "Two clients of one new Auth, as two copies of the runtime."
  []
  (let [cl (r/cluster auth-name)]
    [(r/open cl auth-name) (r/open cl auth-name)]))

(def ^:private defaults
  "One call of each write, as Asha's first day would make it: invited at
  100, in at 110, signed in at 130 until 100000, a session at 140 until
  50000, an agent at 130, an ask at 130 that ends at 1000."
  {:invite {:person asha :claim asha-code :invited-by :system :at 100}
   :enter {:account asha-google :says {"name" "Asha"} :email nil :code-hash (h 1)
           :own-layer layer-1 :making making-1 :at 110}
   :own-layer-made {:person asha :at 120}
   :begin-sign-in {:root asha :actor asha :sign-in sign-in-1 :kind :browser :hash (h 101)
                   :until 100000 :label nil :at 130}
   :replace-secret {:root asha :sign-in sign-in-1 :old-hash (h 101) :new-hash (h 102)
                    :until 200000 :previous-until 150000 :at 140}
   :end-sign-in {:root asha :sign-in sign-in-1 :at 150}
   :make-actor {:root asha :actor agent-1 :made-by asha :label "importer" :at 130}
   :end-actor {:root asha :actor agent-1 :at 150}
   :open-session {:root asha :sign-in sign-in-1 :session session-1 :until 50000 :at 140}
   :close-session {:root asha :session session-1 :at 150}
   :ask {:code code-1 :hash (h 201) :asks "the importer on a laptop" :ends 1000 :at 130}
   :answer-ask {:code code-1 :yes? true :root asha :actor asha :new-agent nil
                :sign-in sign-in-2 :until 100000 :at 140}})

(defn- c
  "A call of the write `kind`: its default, with `parts` in their place."
  [kind & {:as parts}]
  [kind (merge (get defaults kind) parts)])

(defn- without
  "A call of the write `kind`: its default with the parts `ks` left out."
  [kind & ks]
  [kind (apply dissoc (get defaults kind) ks)])

(defn- run
  "Runs the calls in order from an empty state. Gives the last state, every
  result, and the last result."
  [calls]
  (let [[state results] (reduce (fn [[s rs] call] (let [[s' res] (r/step s call)] [s' (conj rs res)]))
                                [r/empty-state []]
                                calls)]
    {:state state :results results :last (peek results)}))

(defn- last-of
  "The result of the last of the calls, run from an empty state."
  [calls]
  (:last (run calls)))

(defn- unchanged?
  "Whether running `call` after `calls` gives `result` and leaves the state
  as `calls` left it."
  [calls call result]
  (let [before (:state (run calls))
        [after got] (r/step before call)]
    (and (= result got) (= before after))))

(defn- who
  "who-is in `state` for Asha's root: the sign-in `G`, and the session `S`
  when it is given."
  ([state G] (r/who-is state {:root asha :sign-in G :session nil}))
  ([state G S] (r/who-is state {:root asha :sign-in G :session S})))

(def ^:private unfit
  "What a call whose input does not fit gives."
  {:could-not :read-the-event})

(def ^:private asha-waits [(c :invite)])
(def ^:private asha-in [(c :invite) (c :enter)])
(def ^:private bo-in
  [(c :invite :person bo :claim bo-email :at 101)
   (c :enter :account bo-google :email "bo@example.org" :code-hash nil :own-layer layer-2
      :making making-2 :at 111)])
(def ^:private signed-in (conj asha-in (c :begin-sign-in)))
(def ^:private sitting (conj signed-in (c :open-session)))
(def ^:private with-agent (conj asha-in (c :make-actor)))
(def ^:private asked (conj asha-in (c :ask)))

;; ------------------------------------------------- the rules for every call

(def ^:private write-fns
  "The twelve writes of the client, in the order of the contract."
  [r/invite r/enter r/own-layer-made r/begin-sign-in r/replace-secret r/end-sign-in r/make-actor
   r/end-actor r/open-session r/close-session r/ask r/answer-ask])

(def ^:private contract-names
  "The eighteen functions of the contract."
  '[open close! invite enter own-layer-made begin-sign-in replace-secret end-sign-in make-actor
    end-actor open-session close-session ask answer-ask who-is ask-of under watch-root])

(defn- rules-checks
  "The checks of \"Rules for every call\" and of the forms."
  []
  (let [cl (new-client)
        odd [nil 7 "x" [] {} (range) (sorted-map-by (fn [_ _] (throw (ex-info "no order" {}))) :a 1)
             {:root "x" :at "y"} [1 2 3] (Object.)]
        could-not? (fn [x] (and (map? x) (contains? x :could-not)))
        outcomes (concat
                  (for [f write-fns v odd] (could-not? (f cl v)))
                  (for [f write-fns v odd] (could-not? (f v (second (c :invite)))))
                  (for [f [r/who-is r/ask-of r/under r/live-under] v odd] (could-not? (f cl v)))
                  (for [f [r/who-is r/ask-of r/under r/live-under] v odd] (could-not? (f v asha)))
                  (for [v odd] (could-not? (r/watch-root cl v v)))
                  (for [v odd] (could-not? (r/watch-root v asha (fn [_] nil))))
                  (for [v odd] (= {:could-not :find-the-module} (r/open v auth-name)))
                  (for [v odd] (= {:could-not :find-the-module} (r/open (r/cluster auth-name) v)))
                  (for [v odd] (nil? (r/close! v)))
                  (for [v odd] (= unfit (second (r/step r/empty-state v))))
                  (for [v odd] (= unfit (second (r/step r/empty-state [:invite v])))))]
    (check "No call throws for what a caller gives: each of the eighteen functions, given things of no form (an infinite sequence and a map whose order throws among them), gives a map that holds :could-not, close! gives nil, and step gives :read-the-event."
           (every? true? outcomes)))

  (check "Auth checks an id only within its root: an id of a sign-in, an agent and a session that another root holds is taken under this one."
         (= [{:begun sign-in-1} {:begun sign-in-1} {:made agent-1} {:made agent-1} {:open session-1} {:open session-1}]
            (take-last 6 (:results (run (concat asha-in bo-in
                                                [(c :begin-sign-in) (c :begin-sign-in :root bo :actor bo :hash (h 102))
                                                 (c :make-actor) (c :make-actor :root bo :made-by bo)
                                                 (c :open-session) (c :open-session :root bo)]))))))

  (let [cases [[[] (c :invite)]
               [asha-waits (c :enter)]
               [asha-in (c :own-layer-made)]
               [asha-in (c :begin-sign-in)]
               [signed-in (c :replace-secret)]
               [signed-in (c :end-sign-in)]
               [asha-in (c :make-actor)]
               [with-agent (c :end-actor)]
               [signed-in (c :open-session)]
               [sitting (c :close-session)]
               [[] (c :ask)]
               [asked (c :answer-ask)]]
        failing (remove (fn [[before w]]
                          (let [once (run (conj before w))
                                twice (run (conj before w w))]
                            (and (not (contains? (:last once) :refused))
                                 (= (:state once) (:state twice))
                                 (= (:last once) (:last twice)))))
                        cases)]
    (check "A write that arrives twice leaves the same state as once, and the second call gives the result of the first: so for each of the twelve writes."
           (and (= 12 (count cases)) (empty? failing)) (map (comp first second) failing)))

  (let [{:keys [state last]} (run [(c :invite) (c :invite :invited-by agent-1 :at 999)])]
    (check "Two invites are the same write when the person and the claim are: a repeat with another :invited-by and :at gives {:invited P} and keeps the first ones."
           (and (= {:invited asha} last)
                (= [:system 100] [(get-in state [:persons asha :invited-by]) (get-in state [:persons asha :invited-at])]))))

  (check "Two begin-sign-ins are the same write when the root, the actor, the kind and the hash are: a repeat with another :until, :label and :at gives {:begun G} and changes nothing."
         (unchanged? signed-in (c :begin-sign-in :until 7 :label "another" :at 135) {:begun sign-in-1}))

  (check "A sign-in id is bound once: with another actor, another kind or another hash it is refused with :in-use."
         (every? #(= {:refused :in-use} (last-of (concat with-agent [(c :begin-sign-in) %])))
                 [(c :begin-sign-in :actor agent-1) (c :begin-sign-in :kind :program) (c :begin-sign-in :hash (h 109))]))

  (let [{:keys [state results]} (run (conj with-agent (c :make-actor :label "another" :at 135)
                                           (c :make-actor :made-by agent-1 :at 136)))]
    (check "Two make-actors are the same write when the root and the maker are: a repeat with another :label gives {:made A} and keeps the first label; with another maker it is :in-use."
           (and (= [{:made agent-1} {:refused :in-use}] (take-last 2 results))
                (= "importer" (:label (first (:actors (r/under state asha))))))))

  (let [later (:state (run (conj sitting (c :open-session :until 60000 :at 141))))
        earlier (:state (run (conj sitting (c :open-session :until 40000 :at 141))))]
    (check "Two open-sessions are the same write when the root and the sign-in are: a repeat moves the session's time on to its :until when that is later, and keeps it when that is earlier."
           (= [60000 50000] [(:until (:session (who later sign-in-1 session-1)))
                             (:until (:session (who earlier sign-in-1 session-1)))])))

  (check "Two asks are the same write when the hash is: a repeat with another :asks, :ends and :at gives {:asked U} and changes nothing."
         (unchanged? [(c :ask)] (c :ask :asks "another" :ends 5000 :at 131) {:asked code-1}))

  (check "A refusal binds nothing: a begin-sign-in refused with :no-such-actor is taken when it comes again after the agent is made."
         (= [{:refused :no-such-actor} {:made agent-1} {:begun sign-in-1}]
            (take-last 3 (:results (run (conj asha-in (c :begin-sign-in :actor agent-1 :at 125) (c :make-actor)
                                              (c :begin-sign-in :actor agent-1)))))))

  (check "No write reads a clock: the reference's source names none of the JVM's clocks, so every time that it holds against is the :at of a call."
         (let [source (slurp (io/resource "softland/auth/reference.clj"))]
           (not-any? #(str/includes? source %)
                     ["currentTimeMillis" "nanoTime" "java.time" "Instant" "java.util.Date" "(Date." "Clock"])))

  (check "Times are fixed-width integers: an :at that is a BigInt, a BigInteger, a double, a string or nil does not fit, and one that is an int does."
         (and (every? #(= unfit (last-of [(c :invite :at %)])) [(bigint 100) (biginteger 100) 100.0 "100" nil])
              (= {:invited asha} (last-of [(c :invite :at (int 100))]))))

  (check "Ids of persons, actors, sign-ins and sessions are UUIDs: an id given as its text does not fit."
         (every? #(= unfit (last-of (conj signed-in %)))
                 [(c :invite :person (str bo)) (c :begin-sign-in :actor (str asha) :sign-in sign-in-2)
                  (c :end-sign-in :sign-in (str sign-in-1)) (c :open-session :session (str session-1))]))

  (check "A hash is 64 lower-case hex characters: one of 63 characters, one in capitals, and one with a letter that is no hex digit do not fit."
         (every? #(= unfit (last-of (conj asha-in (c :begin-sign-in :hash %))))
                 [(apply str (repeat 63 "a")) (apply str (repeat 64 "A")) (str (apply str (repeat 63 "a")) "g")]))

  (let [state (:state (run [(c :ask)]))]
    (check "A code is a string of 8 characters, compared exactly: one of 7 or 9 characters does not fit, and the same letters in lower case are another code."
           (and (every? #(= unfit (last-of [(c :ask :code %)])) ["ABCD234" "ABCD23456" 12345678])
                (some? (r/ask-of state code-1))
                (nil? (r/ask-of state "abcd2345")))))

  (let [pair (String. (Character/toChars 0x1F600))]
    (check "A text is a string of at most 256 characters, a character being one char of the JVM: a label of 256 fits and one of 257 does not; 128 characters outside the BMP, which are 256 chars, fit, and 129 do not."
           (and (= {:made agent-1} (last-of (conj asha-in (c :make-actor :label (text-of 256)))))
                (= unfit (last-of (conj asha-in (c :make-actor :label (text-of 257)))))
                (= {:made agent-1} (last-of (conj asha-in (c :make-actor :label (apply str (repeat 128 pair))))))
                (= unfit (last-of (conj asha-in (c :make-actor :label (apply str (repeat 129 pair))))))
                (= unfit (last-of [(c :enter :account [:google (text-of 257)])]))
                (= unfit (last-of [(c :ask :asks (text-of 257))])))))

  (check "A provider is a keyword: an account whose provider is a string does not fit, nor an account that is no vector of two."
         (every? #(= unfit (last-of (conj asha-waits (c :enter :account %))))
                 [["google" "1082"] [:google] [:google "1082" "more"] (list :google "1082")]))

  (check "A provider is a keyword whose name is at most 256 characters, and :own-layer is a UUID: forms, which Auth checks (Sid's call of 22 November). An enter whose provider's name has 257 characters, its namespace counted, or whose :own-layer is no UUID or nil, does not fit; a provider's name of 256 characters fits."
         (and (every? #(= unfit (last-of (conj asha-waits %)))
                      [(c :enter :account [(keyword (text-of 257)) "1"])
                       (c :enter :account [(keyword "g" (text-of 255)) "1"])
                       (c :enter :own-layer "a layer") (c :enter :own-layer (str layer-1)) (c :enter :own-layer nil)])
              (= asha (:person (last-of (conj asha-waits (c :enter :account [(keyword (text-of 256)) "1"])))))))

  (check ":making holds two UUIDs, its act's and its session's (QUESTIONS.md, 4, which Sid's call of 22 November let stand): a :making without its session, or whose session is no UUID, does not fit."
         (every? #(= unfit (last-of (conj asha-waits %)))
                 [(c :enter :making {:uuid (id 51)}) (c :enter :making {:uuid (id 51) :session "s"})]))

  (check "A :driven-by, optional on open-session, is nil or either {:kind :person} or {:kind :model :model text}: each fits, and a model of 257 characters, a model without its text, a person with a model, another kind and a string do not."
         (and (every? #(= {:open session-1} (last-of (conj signed-in %)))
                      [(c :open-session) (c :open-session :driven-by nil) (c :open-session :driven-by {:kind :person})
                       (c :open-session :driven-by {:kind :model :model "claude-opus-5-5"})
                       (c :open-session :driven-by {:kind :model :model (text-of 256)})])
              (every? #(= unfit (last-of (conj signed-in (c :open-session :driven-by %))))
                      [{:kind :model :model (text-of 257)} {:kind :model} {:kind :person :model "m"} {:kind :machine}
                       "person" {:kind :model :model :m}])))

  (let [says-of (fn [n] (into {} (map (fn [i] [(str "k" i) "v"])) (range n)))]
    (check "A :says map holds at most 32 entries, each from a string of at most 256 characters to a string of at most 2,048: the bounds fit, and one entry more, a longer key, a longer value, a key that is no string and a :says that is no map do not."
           (and (every? #(= asha (:person (last-of (conj asha-waits (c :enter :says %)))))
                        [{} (says-of 32) {(text-of 256) (text-of 2048)}])
                (every? #(= unfit (last-of (conj asha-waits (c :enter :says %))))
                        [(says-of 33) {(text-of 257) "v"} {"k" (text-of 2049)} {:name "Asha"} nil ["name" "Asha"]]))))

  (let [before (:state (run sitting))
        [after got] (r/step before (c :open-session :until "later" :at 99999999))]
    (check "A write whose input does not fit gives {:could-not :read-the-event} and writes nothing, also when its time has passed what is live under its root."
           (and (= unfit got) (= before after) (= 1 (count (:sessions (r/live-under after asha)))))))

  (let [state (:state (run sitting))]
    (check "A read whose input does not fit gives {:could-not :read-the-event}: who-is with a part left out, one part more or a root that is no UUID; ask-of with a code of 7 characters; under and live-under of a text."
           (every? #(= unfit %)
                   [(r/who-is state {:root asha :sign-in sign-in-1})
                    (r/who-is state {:root asha :sign-in sign-in-1 :session nil :more 1})
                    (r/who-is state {:root (str asha) :sign-in sign-in-1 :session nil})
                    (r/ask-of state "ABCD234") (r/under state (str asha)) (r/live-under state (str asha))])))

  (check "Every part that the contract names must be there, an -or-nil one as nil, and no other part may be (Sid's call of 22 November): an enter without :email or :code-hash, a begin-sign-in without :label, an answer without :new-agent and an invite with one part more do not fit; :driven-by alone may be left out."
         (and (every? #(= unfit (last-of (conj asked %)))
                      [(without :enter :email) (without :enter :code-hash) (without :begin-sign-in :label)
                       (without :answer-ask :new-agent) (c :invite :more 1) (c :open-session :at 140 :more 1)])
              (= {:open session-1} (last-of (conj signed-in (without :open-session :driven-by))))))

  (check "Two emails are the same when their texts are the same: an enter whose email is another text than the invitation's, by one character, does not prove it."
         (and (= bo (:person (last-of (conj (vec (take 1 bo-in)) (c :enter :account bo-google :code-hash nil :email "bo@example.org")))))
              (every? #(= {:refused :not-invited}
                          (last-of (conj (vec (take 1 bo-in)) (c :enter :account bo-google :code-hash nil :email %))))
                      ["bo@example.or" "b0@example.org" "bo@example.org."])))

  (check "The runtime gives every email in lower case, and Auth holds that as form (QUESTIONS.md, 3, which Sid's call of 22 November let stand): an email with a capital letter does not fit, in a claim or in an enter, also an enter of an account that has a link."
         (and (= unfit (last-of [(c :invite :claim {:by :email :email "Bo@example.org"})]))
              (= unfit (last-of (conj (vec (take 1 bo-in)) (c :enter :account bo-google :code-hash nil :email "Bo@example.org"))))
              (= unfit (last-of (conj asha-in (c :enter :email "Asha@example.org" :at 120))))))

  (let [cl (r/cluster auth-name "Another")
        a (r/open cl auth-name)
        b (r/open cl auth-name)
        other (r/open cl "Another")]
    (check "open takes what stands for the cluster and a module's name, and gives a client that a program can write through and read through: clients opened under one name share one Auth, and another name is another Auth. A name that the cluster does not hold, a cluster that holds none, and a thing that stands for no cluster, a client among them, give {:could-not :find-the-module}."
           (and (= {:invited asha} (r/invite a (second (c :invite))))
                (= layer-1 (:own-layer (r/enter b (second (c :enter)))))
                (= {:refused :not-invited} (r/enter other (second (c :enter))))
                (= {:could-not :find-the-module} (r/open cl "Auth2"))
                (= {:could-not :find-the-module} (r/open (r/cluster) auth-name))
                (= {:could-not :find-the-module} (r/open a auth-name))
                (= {:could-not :find-the-module} (r/open :a-cluster auth-name))
                (= {:could-not :find-the-module} (r/open nil auth-name)))))

  (let [cl (new-client)]
    (check "close! gives nil, also a second time."
           (and (nil? (r/close! cl)) (nil? (r/close! cl))))))

;; ------------------------------------------------------------------- invite

(defn- invite-checks
  "The checks of invite."
  []
  (check "invite gives {:invited P} and makes the person wait under the claim: the first account that proves the claim becomes hers."
         (and (= {:invited asha} (last-of [(c :invite)]))
              (= asha (:person (last-of asha-in)))))

  (check "invite is refused with :claim-in-use when the claim names another person."
         (= {:refused :claim-in-use} (last-of [(c :invite) (c :invite :person bo)])))

  (check "invite is refused with :person-in-use when the person has another claim."
         (= {:refused :person-in-use} (last-of [(c :invite) (c :invite :claim bo-email)])))

  (check "A claim names one person for good: once an account has proven it, it is used up; inviting another person under it is :claim-in-use, and another account that proves it is not let in."
         (= [{:refused :claim-in-use} {:refused :not-invited}]
            (take-last 2 (:results (run (conj asha-in (c :invite :person bo) (c :enter :account bo-google)))))))

  (check "The same person under the same claim again gives {:invited P} and changes nothing, also after she has entered."
         (and (unchanged? asha-waits (c :invite :at 105) {:invited asha})
              (unchanged? asha-in (c :invite :at 200) {:invited asha})))

  (let [{:keys [state results]} (run (conj asha-waits (c :begin-sign-in) (c :make-actor)))]
    (check "A person who waits has no sign-in and is no actor yet: a sign-in for her is :no-such-actor, an agent made by her is :no-such-maker, and who-is gives her no own layer."
           (and (= [{:refused :no-such-actor} {:refused :no-such-maker}] (take-last 2 results))
                (= {:sign-in nil :session nil :own-layer nil :own-layer-made? false} (who state sign-in-1)))))

  (let [{:keys [state last]} (run [(c :invite :invited-by agent-3)])]
    (check ":invited-by is kept as given and not checked: an invite by an id that is no actor's is taken, and the state keeps that id."
           (and (= {:invited asha} last) (= agent-3 (get-in state [:persons asha :invited-by])))))

  (check "An invitation has no time: an enter ten years after the invite is let in."
         (= asha (:person (last-of [(c :invite) (c :enter :at (+ 100 (* 10 365 24 3600 1000)))]))))

  (check "Where both refusals of invite apply, :claim-in-use comes first, as the contract lists them: Asha, who has a claim, under the claim of Bo is :claim-in-use."
         (= {:refused :claim-in-use} (last-of [(c :invite) (c :invite :person bo :claim bo-email) (c :invite :claim bo-email)]))))

;; -------------------------------------------------------------------- enter

(defn- enter-checks
  "The checks of enter and own-layer-made."
  []
  (check "enter of an account that proves an invitation gives {:person P :own-layer L :making M :own-layer-made? false}, with the own layer and the :making that it offered."
         (= {:person asha :own-layer layer-1 :making making-1 :own-layer-made? false} (last-of asha-in)))

  (check "enter of an account that has no link and proves no claim is refused with :not-invited, and nothing of it is kept."
         (unchanged? asha-waits (c :enter :code-hash (h 9) :email "nobody@example.org") {:refused :not-invited}))

  (let [{:keys [results]} (run (conj asha-in (c :invite :person bo :claim bo-email :at 112)
                                     (c :enter :email "bo@example.org" :code-hash nil :at 120)
                                     (c :enter :account bo-google :email "bo@example.org" :code-hash nil
                                        :own-layer layer-2 :making making-2 :at 130)))]
    (check "An account that has a link gives its person, whatever claim came with it: an enter of Asha's account with the email of Bo, who waits, gives Asha, and Bo's claim still waits for Bo."
           (and (= asha (:person (nth results 3))) (= bo (:person (nth results 4))))))

  (let [both (conj asha-waits (c :invite :person bo :claim bo-email :at 101))]
    (check "An account that has none takes the person of the code's invitation when :code-hash names one that waits, before the email's."
           (= asha (:person (last-of (conj both (c :enter :account [:github "9"] :email "bo@example.org")))))))

  (check "An account that has none takes the email's person when :code-hash names no invitation that waits: none at all, or one that is used up."
         (and (= bo (:person (last-of (conj (vec (take 1 bo-in)) (c :enter :account bo-google :code-hash (h 9) :email "bo@example.org")))))
              (= bo (:person (last-of (concat asha-in (take 1 bo-in)
                                              [(c :enter :account bo-google :code-hash (h 1) :email "bo@example.org" :at 120)]))))))

  (let [{:keys [results]} (run (conj asha-in (c :enter :at 115) (c :enter :account bo-google :at 116)
                                     (c :begin-sign-in) (c :make-actor)))]
    (check "The link is written, the claim is used up, and the person becomes an actor, her own root: the account gives her again, another account with her code is not let in, and she begins a sign-in and makes an agent."
           (and (= asha (:person (nth results 2))) (= {:refused :not-invited} (nth results 3))
                (= [{:begun sign-in-1} {:made agent-1}] (take-last 2 results)))))

  (check "The first enter that makes her an actor fixes :own-layer and :making; a later enter gives the fixed ones, and its own offer is dropped."
         (= {:person asha :own-layer layer-1 :making making-1 :own-layer-made? false}
            (last-of (conj asha-in (c :enter :own-layer layer-2 :making making-2 :at 120)))))

  (let [{:keys [state]} (run (conj asha-in (c :enter :says {"name" "Asha K"} :at 300)
                                   (c :enter :says {"name" "Asha, from a clock behind"} :at 250)
                                   (c :enter :says {"name" "Asha, at the same time"} :at 300)))]
    (check ":says is kept from the enter with the latest :at, with that :at; an enter whose :at is not later, earlier or the same, leaves it (Sid's call of 22 November)."
           (= {:person asha :says {"name" "Asha K"} :said-at 300}
              (select-keys (get-in state [:links asha-google]) [:person :says :said-at]))))

  (check "A copy of an enter, sent again after a later enter of the account, gives its person, own layer and :making, and leaves :says as the later enter left it: nothing changes (Sid's call of 22 November)."
         (unchanged? (conj asha-in (c :enter :says {"name" "Asha K"} :at 300)) (c :enter)
                     {:person asha :own-layer layer-1 :making making-1 :own-layer-made? false}))

  (let [{:keys [state]} (run (into asha-in (map (fn [i] (c :enter :says {"n" (str i)} :at (+ 200 i)))) (range 100)))]
    (check "The link keeps no memory of each enter: after a hundred enters of one account, its link holds the person, one :says and one :at, and nothing more (Sid's call of 22 November)."
           (= {:person asha :says {"n" "99"} :said-at 299} (get-in state [:links asha-google]))))

  (let [{:keys [results]} (run (conj asha-in (c :enter :at 115)))]
    (check "An enter that is run again after a failure gives the person, the own layer and the :making that it gave, and :own-layer-made? as it stands."
           (= (nth results 1) (nth results 2))))

  (let [{:keys [results]} (run (conj asha-in (c :enter :at 115) (c :own-layer-made) (c :enter :at 125)))]
    (check "Until own-layer-made, every enter says :own-layer-made? false; after it, an enter says true (Sid's call of 22 November)."
           (= [false false true] (map :own-layer-made? [(nth results 1) (nth results 2) (nth results 4)]))))

  (let [{:keys [results]} (run (conj asha-in (c :own-layer-made) (c :enter)))]
    (check "A copy of the first enter, sent again after own-layer-made, gives the person, the own layer and the :making that the first gave, and :own-layer-made? true: an enter says the flag as it stands at that enter (Sid's call of 22 November)."
           (and (= {:person asha :own-layer layer-1 :making making-1 :own-layer-made? false} (nth results 1))
                (= {:person asha :own-layer layer-1 :making making-1 :own-layer-made? true} (nth results 3)))))

  (let [{:keys [state last]} (run (conj asha-in (c :own-layer-made)))]
    (check "own-layer-made of a person who has entered gives {:made P}, and who-is then says :own-layer-made? true, with her own layer."
           (and (= {:made asha} last)
                (= {:own-layer layer-1 :own-layer-made? true} (select-keys (who state sign-in-1) [:own-layer :own-layer-made?])))))

  (check "own-layer-made of a person who waits, or of an id that is no person, is refused with :no-such-person."
         (= [{:refused :no-such-person} {:refused :no-such-person}]
            [(last-of (conj asha-waits (c :own-layer-made))) (last-of (conj asha-in (c :own-layer-made :person cy)))]))

  (check "own-layer-made again gives {:made P} and changes nothing."
         (unchanged? (conj asha-in (c :own-layer-made)) (c :own-layer-made :at 130) {:made asha})))

;; ------------------------------------------------------------ begin-sign-in

(defn- begin-sign-in-checks
  "The checks of begin-sign-in."
  []
  (let [{:keys [state last]} (run signed-in)]
    (check "begin-sign-in gives {:begun G}: from then the secret whose hash is H stands for the actor A until t1, and who-is says so, with no secret before it."
           (and (= {:begun sign-in-1} last)
                (= {:actor asha :kind :browser :hash (h 101) :until 100000 :previous nil :ended? false :actor-ended? false}
                   (:sign-in (who state sign-in-1))))))

  (check "begin-sign-in for an actor that is not of the root is refused with :no-such-actor: an id that is nobody's, and an agent of another root."
         (= [{:refused :no-such-actor} {:refused :no-such-actor}]
            (take-last 2 (:results (run (concat asha-in bo-in [(c :make-actor :root bo :made-by bo :actor agent-2)
                                                               (c :begin-sign-in :actor agent-3)
                                                               (c :begin-sign-in :actor agent-2)]))))))

  (check "begin-sign-in for an actor that has ended, or whose maker has ended, is refused with :ended."
         (= [{:refused :ended} {:refused :ended}]
            (take-last 2 (:results (run (conj with-agent (c :make-actor :actor agent-2 :made-by agent-1 :at 131)
                                              (c :end-actor :at 140)
                                              (c :begin-sign-in :actor agent-1 :at 141)
                                              (c :begin-sign-in :actor agent-2 :at 142)))))))

  (check "begin-sign-in with an id that is another sign-in already is refused with :in-use."
         (= {:refused :in-use} (last-of (conj signed-in (c :begin-sign-in :hash (h 109) :at 131)))))

  (check "Where several refusals of begin-sign-in apply, they come in the order of the contract: an actor that is not there with an id in use is :no-such-actor, and an ended actor with an id in use is :ended."
         (= [{:refused :no-such-actor} {:refused :ended}]
            (take-last 2 (:results (run (conj with-agent (c :begin-sign-in) (c :end-actor :at 140)
                                              (c :begin-sign-in :actor agent-3 :at 141)
                                              (c :begin-sign-in :actor agent-1 :at 142)))))))

  (check "Auth refuses nothing for a person whose layer is not marked as made: her sign-in begins before own-layer-made."
         (= {:begun sign-in-1} (last-of signed-in)))

  (check "A repeat of begin-sign-in is known by the hash with which the sign-in began, also after its secret was replaced (QUESTIONS.md, 8)."
         (unchanged? (conj signed-in (c :replace-secret)) (c :begin-sign-in :at 145) {:begun sign-in-1})))

;; ----------------------------------------------------------- replace-secret

(defn- replace-secret-checks
  "The checks of replace-secret."
  []
  (let [{:keys [state last]} (run (conj signed-in (c :replace-secret)))]
    (check "replace-secret gives {:replaced G}: the secret becomes the one whose hash is H', with the new until, and the old one stands until t2 as the secret before it."
           (and (= {:replaced sign-in-1} last)
                (= {:hash (h 102) :until 200000 :previous {:hash (h 101) :until 150000}}
                   (select-keys (:sign-in (who state sign-in-1)) [:hash :until :previous])))))

  (check "replace-secret of a sign-in that is not live is refused with :not-live: one that has ended, one that the root does not have, one whose time is at or before the write's :at, and one whose actor has ended."
         (= (repeat 4 {:refused :not-live})
            [(last-of (conj signed-in (c :end-sign-in :at 135) (c :replace-secret)))
             (last-of (conj signed-in (c :replace-secret :sign-in sign-in-3)))
             (last-of (conj signed-in (c :replace-secret :at 100000)))
             (last-of (conj with-agent (c :begin-sign-in :actor agent-1) (c :end-actor :at 135) (c :replace-secret)))]))

  (check "replace-secret whose old hash is not the sign-in's hash now is refused with :wrong-secret."
         (= {:refused :wrong-secret} (last-of (conj signed-in (c :replace-secret :old-hash (h 109))))))

  (check "The same replacement again, when H' is the hash now and H the one before it, gives {:replaced G} and changes nothing, whatever its other parts."
         (unchanged? (conj signed-in (c :replace-secret)) (c :replace-secret :until 7 :previous-until 8 :at 145)
                     {:replaced sign-in-1}))

  (check "A replacement that arrives after a newer one is no repeat, and is refused with :wrong-secret when its old hash is no longer the hash now (Sid's call of 22 November)."
         (= {:refused :wrong-secret}
            (last-of (conj signed-in (c :replace-secret) (c :replace-secret :old-hash (h 102) :new-hash (h 103) :at 145)
                           (c :replace-secret :at 146)))))

  (let [before (:state (run sitting))
        after (:state (run (conj sitting (c :replace-secret :at 141))))]
    (check "No session changes when the secret is replaced with a new time after the replacement's :at; for one whose new time is not, see QUESTIONS.md."
           (= (:session (who before sign-in-1 session-1)) (:session (who after sign-in-1 session-1)))))

  (check "A replacement's :previous-until is kept as given, also when it is later than the new :until (QUESTIONS.md, 13)."
         (= {:until 200000 :previous {:hash (h 101) :until 900000}}
            (select-keys (:sign-in (who (:state (run (conj signed-in (c :replace-secret :previous-until 900000)))) sign-in-1))
                         [:until :previous])))

  (check "One secret before the present one stands: a later replacing puts its own old secret and its t2 in the place of the one before."
         (= {:hash (h 102) :until 160000}
            (:previous (:sign-in (who (:state (run (conj signed-in (c :replace-secret)
                                                         (c :replace-secret :old-hash (h 102) :new-hash (h 103)
                                                            :previous-until 160000 :at 145))))
                                      sign-in-1)))))

  (let [cycled (conj signed-in (c :replace-secret) (c :replace-secret :old-hash (h 102) :new-hash (h 101) :at 141))
        {:keys [state last]} (run (conj cycled (c :replace-secret :at 142)))]
    (check "A replacement is judged by the present hash and the one before it alone, and a sign-in keeps no history of its replacements (Sid's call of 22 November): after H1 to H2 and H2 to H1, H1 to H2 again is taken, since H1 is the hash now, and the secret is then H2 with H1 before it; a replacement whose old and new hash are one is taken."
           (and (= {:replaced sign-in-1} last)
                (= {:hash (h 102) :until 200000 :previous {:hash (h 101) :until 150000}}
                   (select-keys (:sign-in (who state sign-in-1)) [:hash :until :previous]))
                (= #{:actor :kind :hash :first-hash :until :previous :label :begun :ended?}
                   (set (keys (get-in state [:roots asha :sign-ins sign-in-1]))))
                (= {:replaced sign-in-1} (last-of (conj signed-in (c :replace-secret :new-hash (h 101))))))))

  (check "Where both refusals of replace-secret apply, :not-live comes first: an ended sign-in with a wrong old hash is :not-live."
         (= {:refused :not-live} (last-of (conj signed-in (c :end-sign-in :at 135) (c :replace-secret :old-hash (h 109))))))

  (let [{:keys [state last]} (run (conj sitting (c :replace-secret :until 140 :at 145)))]
    (check "A replacement whose new until is at or before its own :at is taken, and takes the sign-in out at once, with its sessions (QUESTIONS.md, 10, which Sid's call of 22 November let stand)."
           (and (= {:replaced sign-in-1} last)
                (:ended? (:sign-in (who state sign-in-1)))
                (:closed? (:session (who state sign-in-1 session-1)))
                (= {:sign-ins {} :sessions {}} (r/live-under state asha))))))

;; -------------------------------------------------------------- end-sign-in

(defn- end-sign-in-checks
  "The checks of end-sign-in."
  []
  (let [{:keys [state last]} (run (conj sitting (c :open-session :session session-2 :at 141) (c :end-sign-in)))]
    (check "end-sign-in gives {:ended G}: the sign-in ends, every session under it is closed, and none of them is in what is live."
           (and (= {:ended sign-in-1} last)
                (:ended? (:sign-in (who state sign-in-1)))
                (every? #(:closed? (:session (who state sign-in-1 %))) [session-1 session-2])
                (= {:sign-ins {} :sessions {}} (r/live-under state asha)))))

  (check "end-sign-in of a sign-in that the root does not have is refused with :no-such-sign-in."
         (= {:refused :no-such-sign-in} (last-of (conj signed-in (c :end-sign-in :sign-in sign-in-2)))))

  (check "Ending a sign-in again gives {:ended G} and changes nothing."
         (unchanged? (conj sitting (c :end-sign-in)) (c :end-sign-in :at 160) {:ended sign-in-1})))

;; --------------------------------------------------------------- make-actor

(defn- chain
  "The calls that make `n` agents in a line under Asha, each made by the one
  before it, the first by Asha; with the ids that they make."
  [n]
  (let [ids (mapv #(id (+ 1000 %)) (range n))]
    [(vec (map-indexed (fn [i A] (c :make-actor :actor A :made-by (if (zero? i) asha (ids (dec i))) :at (+ 130 i))) ids))
     ids]))

(defn- make-actor-checks
  "The checks of make-actor."
  []
  (let [{:keys [state last]} (run with-agent)]
    (check "make-actor gives {:made A}: the agent is under the root, made by the live actor B, with its label."
           (and (= {:made agent-1} last)
                (= [{:actor agent-1 :made-by asha :label "importer" :ended? false}] (:actors (r/under state asha))))))

  (check "make-actor whose maker is not an actor of the root is refused with :no-such-maker: an id that is nobody's, an agent of another root, and a person who waits."
         (= (repeat 3 {:refused :no-such-maker})
            [(last-of (conj asha-in (c :make-actor :made-by agent-3)))
             (last-of (concat asha-in bo-in [(c :make-actor :root bo :made-by bo :actor agent-2)
                                             (c :make-actor :made-by agent-2)]))
             (last-of (conj asha-waits (c :make-actor)))]))

  (check "make-actor whose maker has ended is refused with :ended."
         (= {:refused :ended} (last-of (conj with-agent (c :end-actor :at 140) (c :make-actor :actor agent-2 :made-by agent-1 :at 141)))))

  (check "make-actor whose id is an actor already is refused with :in-use: an agent made by another maker, and the person's own id."
         (= [{:refused :in-use} {:refused :in-use}]
            [(last-of (conj with-agent (c :make-actor :actor agent-2 :made-by agent-1 :at 131)
                            (c :make-actor :actor agent-1 :made-by agent-2 :at 132)))
             (last-of (conj with-agent (c :make-actor :actor asha :made-by agent-1 :at 131)))]))

  (let [[calls ids] (chain 32)]
    (check "Who made whom goes 32 deep at most: 32 agents in a line are made, and the next, whose maker is 32 makers below its root, is refused with :too-deep."
           (let [{:keys [results]} (run (concat asha-in calls [(c :make-actor :actor agent-1 :made-by (peek ids) :at 200)]))]
             (and (every? #(contains? % :made) (butlast (drop 2 results)))
                  (= {:refused :too-deep} (peek results))))))

  (let [[calls ids] (chain 32)]
    (check "Where several refusals of make-actor apply, they come in the order of the contract: an ended maker with an id in use is :ended, and an id in use under a maker that is too deep is :in-use."
           (and (= {:refused :ended}
                   (last-of (conj with-agent (c :make-actor :actor agent-2 :made-by asha :at 131) (c :end-actor :at 140)
                                  (c :make-actor :actor agent-2 :made-by agent-1 :at 141))))
                (= {:refused :in-use}
                   (last-of (concat asha-in calls [(c :make-actor :actor (first ids) :made-by (peek ids) :at 200)])))))))

;; ---------------------------------------------------------------- end-actor

(defn- end-actor-checks
  "The checks of end-actor."
  []
  (let [calls (conj with-agent
                    (c :make-actor :actor agent-2 :made-by agent-1 :at 131)
                    (c :make-actor :actor agent-3 :made-by agent-2 :at 132)
                    (c :begin-sign-in :actor agent-3 :sign-in sign-in-3 :hash (h 103) :at 133)
                    (c :open-session :sign-in sign-in-3 :session session-2 :at 134)
                    (c :begin-sign-in)
                    (c :open-session)
                    (c :end-actor))
        {:keys [state last]} (run calls)]
    (check "end-actor gives {:ended A}: the agent ends with every actor that it made, in any depth, and every sign-in and session of each; the person's own go on."
           (and (= {:ended agent-1} last)
                (every? :ended? (:actors (r/under state asha)))
                (:ended? (:sign-in (who state sign-in-3)))
                (:actor-ended? (:sign-in (who state sign-in-3)))
                (:closed? (:session (who state sign-in-3 session-2)))
                (= [sign-in-1] (keys (:sign-ins (r/live-under state asha))))
                (= [session-1] (keys (:sessions (r/live-under state asha)))))))

  (check "end-actor of an actor that the root does not have is refused with :no-such-actor."
         (= {:refused :no-such-actor} (last-of (conj with-agent (c :end-actor :actor agent-3)))))

  (let [{:keys [state results]} (run (conj signed-in (c :end-actor :actor asha :at 140) (c :begin-sign-in :sign-in sign-in-2 :at 141)))]
    (check "end-actor of the person is refused with :is-a-person, and she is not ended: her sign-in goes on and she begins another."
           (and (= [{:refused :is-a-person} {:begun sign-in-2}] (take-last 2 results))
                (= #{sign-in-1 sign-in-2} (set (keys (:sign-ins (r/live-under state asha))))))))

  (check "end-actor again gives {:ended A} and changes nothing; so does end-actor of an agent that ended with one above it (QUESTIONS.md, 16)."
         (and (unchanged? (conj with-agent (c :end-actor)) (c :end-actor :at 160) {:ended agent-1})
              (unchanged? (conj with-agent (c :make-actor :actor agent-2 :made-by agent-1 :at 131) (c :end-actor))
                          (c :end-actor :actor agent-2 :at 160) {:ended agent-2})))

  (check "Where both refusals of end-actor apply, :no-such-actor comes first: end-actor of a person who waits is :no-such-actor."
         (= {:refused :no-such-actor} (last-of (conj asha-waits (c :end-actor :actor asha))))))

;; ----------------------------------------------------------------- sessions

(defn- session-checks
  "The checks of open-session and close-session."
  []
  (let [{:keys [state last]} (run sitting)]
    (check "open-session under a live sign-in gives {:open S}: who-is gives the session with its sign-in and its until, open, with no :driven-by said."
           (and (= {:open session-1} last)
                (= {:sign-in sign-in-1 :until 50000 :closed? false :driven-by nil} (:session (who state sign-in-1 session-1))))))

  (check "open-session under a sign-in that is not live is refused with :not-live: one that has ended, one that the root does not have, and one whose time is at or before the write's :at."
         (= (repeat 3 {:refused :not-live})
            [(last-of (conj signed-in (c :end-sign-in :at 135) (c :open-session)))
             (last-of (conj signed-in (c :open-session :sign-in sign-in-2)))
             (last-of (conj signed-in (c :open-session :at 100000 :until 200000)))]))

  (check "A session's id that is another sign-in's, open or not, is :in-use."
         (= [{:refused :in-use} {:refused :in-use}]
            [(last-of (conj sitting (c :begin-sign-in :sign-in sign-in-2 :hash (h 102) :at 141)
                            (c :open-session :sign-in sign-in-2 :at 142)))
             (last-of (conj sitting (c :close-session) (c :begin-sign-in :sign-in sign-in-2 :hash (h 102) :at 151)
                            (c :open-session :sign-in sign-in-2 :at 152)))]))

  (let [closed (run (conj sitting (c :close-session) (c :open-session :until 90000 :at 151)))
        passed (run (conj sitting (c :end-sign-in :sign-in sign-in-2 :at 50000) (c :open-session :until 90000 :at 50001)))]
    (check "A session that is closed, or was taken out because its time passed, is not opened again: open-session of it gives {:open S}, the result of the first, and it stays closed (QUESTIONS.md, 17, which Sid's call of 22 November let stand)."
           (and (= {:open session-1} (:last closed) (:last passed))
                (= {:closed? true :until 50000}
                   (select-keys (:session (who (:state closed) sign-in-1 session-1)) [:closed? :until])
                   (select-keys (:session (who (:state passed) sign-in-1 session-1)) [:closed? :until])))))

  (let [model {:kind :model :model "claude-opus-5-5"}
        state (:state (run (conj signed-in (c :open-session :driven-by model) (c :close-session))))]
    (check ":driven-by is kept with the session and is read through who-is, also after the session closes."
           (= {:closed? true :driven-by model} (select-keys (:session (who state sign-in-1 session-1)) [:closed? :driven-by]))))

  (let [state (:state (run (conj signed-in (c :open-session :driven-by {:kind :model :model "m"})
                                 (c :open-session :driven-by {:kind :person} :until 60000 :at 141))))]
    (check "A repeat of open-session does not change its :driven-by, while it moves the session's time on."
           (= {:until 60000 :driven-by {:kind :model :model "m"}}
              (select-keys (:session (who state sign-in-1 session-1)) [:until :driven-by]))))

  (check "A :driven-by that is absent or nil means not said: who-is gives :driven-by nil."
         (= [nil nil] (map #(:driven-by (:session (who (:state (run (conj signed-in %))) sign-in-1 session-1)))
                           [(c :open-session) (c :open-session :driven-by nil)])))

  (check "Where both refusals of open-session apply, :not-live comes first: a sign-in that has ended with a session of another sign-in is :not-live."
         (= {:refused :not-live}
            (last-of (conj sitting (c :begin-sign-in :sign-in sign-in-2 :hash (h 102) :at 141) (c :end-sign-in :sign-in sign-in-2 :at 142)
                           (c :open-session :sign-in sign-in-2 :at 143)))))

  (let [{:keys [state last]} (run (conj sitting (c :close-session)))]
    (check "close-session gives {:closed S}: who-is says the session is closed, and it is not in what is live."
           (and (= {:closed session-1} last)
                (:closed? (:session (who state sign-in-1 session-1)))
                (= {} (:sessions (r/live-under state asha))))))

  (check "close-session of a session that the root does not have is refused with :no-such-session."
         (= {:refused :no-such-session} (last-of (conj sitting (c :close-session :session session-2)))))

  (check "A session is closed once: closing it again gives {:closed S} and changes nothing."
         (unchanged? (conj sitting (c :close-session)) (c :close-session :at 160) {:closed session-1})))

;; ---------------------------------------------------------------------- ask

(defn- ask-checks
  "The checks of ask, answer-ask and ask-of."
  []
  (let [{:keys [state last]} (run [(c :ask)])]
    (check "ask gives {:asked U}, and ask-of gives its hash, its :asks, its :ends, :waiting and no answer."
           (and (= {:asked code-1} last)
                (= {:hash (h 201) :asks "the importer on a laptop" :ends 1000 :state :waiting :answer nil} (r/ask-of state code-1)))))

  (check "ask under a code that is bound to another hash is refused with :in-use, also when the ask is over: a code is bound for good."
         (= [{:refused :in-use} {:refused :in-use}]
            [(last-of [(c :ask) (c :ask :hash (h 202) :at 131)])
             (last-of [(c :ask) (c :ask :hash (h 202) :at 5000 :ends 9000)])]))

  (let [{:keys [state last]} (run (conj asked (c :answer-ask)))]
    (check "yes, as herself: {:yes {:root P :actor P :sign-in G}}; a sign-in of kind :program begins for her with the ask's hash, and ask-of says :yes with the answer."
           (and (= {:yes {:root asha :actor asha :sign-in sign-in-2}} last)
                (= {:actor asha :kind :program :hash (h 201) :until 100000 :ended? false}
                   (select-keys (:sign-in (who state sign-in-2)) [:actor :kind :hash :until :ended?]))
                (= {:state :yes :answer {:root asha :actor asha :sign-in sign-in-2}}
                   (select-keys (r/ask-of state code-1) [:state :answer])))))

  (let [{:keys [state last]} (run (conj asked (c :answer-ask :actor agent-1 :new-agent {:label "the importer"})))]
    (check "yes, as a new agent: the agent is made under her, by her, with the label, and the sign-in begins for it."
           (and (= {:yes {:root asha :actor agent-1 :sign-in sign-in-2}} last)
                (= [{:actor agent-1 :made-by asha :label "the importer" :ended? false}] (:actors (r/under state asha)))
                (= agent-1 (:actor (:sign-in (who state sign-in-2)))))))

  (let [{:keys [state last]} (run (conj with-agent (c :ask) (c :answer-ask :actor agent-1)))]
    (check "yes, as a live agent of hers that exists: the sign-in begins for that agent."
           (and (= {:yes {:root asha :actor agent-1 :sign-in sign-in-2}} last)
                (= agent-1 (:actor (:sign-in (who state sign-in-2)))))))

  (let [{:keys [state results]} (run (conj asked (c :answer-ask :yes? false) (c :answer-ask :at 141)))]
    (check "no: {:no U}, ask-of says :no, and the ask is refused for good: a later yes gives {:no U} and begins nothing."
           (and (= [{:no code-1} {:no code-1}] (take-last 2 results))
                (= {:state :no :answer nil} (select-keys (r/ask-of state code-1) [:state :answer]))
                (nil? (:sign-in (who state sign-in-2))))))

  (check "answer-ask under a code that has no ask is refused with :no-such-ask."
         (= {:refused :no-such-ask} (last-of (conj asha-in (c :answer-ask)))))

  (check "An answer to an ask that waits, whose :at is at or after the ask's :ends, is refused with :over; one just before is taken."
         (and (= {:refused :over} (last-of (conj asked (c :answer-ask :at 1000))))
              (= {:refused :over} (last-of (conj asked (c :answer-ask :at 1001))))
              (contains? (last-of (conj asked (c :answer-ask :at 999))) :yes)))

  (check "answer-ask is refused with :no-such-actor when its actor is not of the root: an id that is nobody's, an agent of another root, and a root who waits."
         (= (repeat 3 {:refused :no-such-actor})
            [(last-of (conj asked (c :answer-ask :actor agent-3)))
             (last-of (concat asked bo-in [(c :make-actor :root bo :made-by bo :actor agent-2) (c :answer-ask :actor agent-2)]))
             (last-of [(c :invite) (c :ask) (c :answer-ask)])]))

  (check "answer-ask for an agent that has ended is refused with :ended."
         (= {:refused :ended} (last-of (conj with-agent (c :ask) (c :end-actor :at 135) (c :answer-ask :actor agent-1)))))

  (check "answer-ask is refused with :in-use when the new agent's id or the sign-in's id is another's."
         (= [{:refused :in-use} {:refused :in-use} {:refused :in-use}]
            [(last-of (conj with-agent (c :ask) (c :answer-ask :actor agent-1 :new-agent {:label "x"})))
             (last-of (conj asked (c :answer-ask :actor asha :new-agent {:label "x"})))
             (last-of (conj signed-in (c :ask) (c :answer-ask :sign-in sign-in-1)))]))

  (let [once (conj asked (c :answer-ask))]
    (check "The first answer stands: the same answer again gives the same result and changes nothing, and another answer, a no or a yes of another sign-in, gives what stands and begins nothing."
           (and (unchanged? once (c :answer-ask :until 7 :at 141) {:yes {:root asha :actor asha :sign-in sign-in-2}})
                (unchanged? once (c :answer-ask :yes? false :at 142) {:yes {:root asha :actor asha :sign-in sign-in-2}})
                (unchanged? once (c :answer-ask :sign-in sign-in-3 :at 143) {:yes {:root asha :actor asha :sign-in sign-in-2}}))))

  (let [{:keys [state]} (run (conj asked (c :answer-ask)))]
    (check "The sign-in that a yes begins has the ask's :asks as its label."
           (= "the importer on a laptop" (:label (first (:sign-ins (r/under state asha)))))))

  (let [once (conj asha-in (c :invite :person bo :claim bo-email :at 101) (c :begin-sign-in) (c :ask) (c :answer-ask))]
    (check "Once an ask has an answer, every answer that fits gives what stands, before any refusal, and changes nothing: a yes that asks for a new agent of her own id, a yes as an actor that is nobody's, a yes whose sign-in id is in use, a no from a root who waits, and a yes from a root that is nobody's (Sid's call of 22 November)."
           (every? #(unchanged? once % {:yes {:root asha :actor asha :sign-in sign-in-2}})
                   [(c :answer-ask :new-agent {:label "x"} :at 141) (c :answer-ask :actor agent-3 :at 142)
                    (c :answer-ask :sign-in sign-in-1 :at 143) (c :answer-ask :yes? false :root bo :at 144)
                    (c :answer-ask :root cy :at 145)])))

  (check "A no to an ask that waits, at or after its :ends, is refused with :over, as every answer to an ask that waits is (QUESTIONS.md, 21)."
         (= {:refused :over} (last-of (conj asked (c :answer-ask :yes? false :at 1000)))))

  (check "A live agent of hers is any live agent of her root: a yes as an agent that her agent made is taken (QUESTIONS.md, 24, which Sid's call of 22 November let stand)."
         (= {:yes {:root asha :actor agent-2 :sign-in sign-in-2}}
            (last-of (conj with-agent (c :make-actor :actor agent-2 :made-by agent-1 :at 131) (c :ask) (c :answer-ask :actor agent-2)))))

  (check "A yes whose sign-in id is a sign-in of the root already is :in-use, also when that sign-in has the very parts that the yes would bind (QUESTIONS.md, 25)."
         (= {:refused :in-use}
            (last-of (conj asked (c :begin-sign-in :sign-in sign-in-2 :kind :program :hash (h 201) :at 135) (c :answer-ask)))))

  (check "A no needs a person who has entered, and judges no other part: a no with an actor and a sign-in that are nobody's is taken, and a no from a root who waits is :no-such-actor (QUESTIONS.md, 22)."
         (and (= {:no code-1} (last-of (conj asked (c :answer-ask :yes? false :actor agent-3 :sign-in sign-in-1))))
              (= {:refused :no-such-actor} (last-of [(c :invite) (c :ask) (c :answer-ask :yes? false)]))))

  (check "The parts of a no that it does not judge must still fit (Sid's call of 22 November): a no whose actor or sign-in is no UUID, or whose :until is no time, does not fit."
         (every? (fn [[k v]] (= unfit (last-of (conj asked (c :answer-ask :yes? false k v)))))
                 [[:actor "an actor"] [:sign-in "a sign-in"] [:until "later"]]))

  (check "ask-of judges no time: it says :waiting for an ask that nobody answered, after its :ends; and gives nil for a code that has no ask."
         (let [state (:state (run (conj asked (c :make-actor :at 5000))))]
           (and (= :waiting (:state (r/ask-of state code-1))) (nil? (r/ask-of state "ZZZZ9999")))))

  (check "Where several refusals of answer-ask apply, they come in the order of the contract: a late answer to an ask that waits, with an actor that is nobody's, is :over, and an answer under a code with no ask is :no-such-ask."
         (and (= {:refused :over} (last-of (conj asked (c :answer-ask :actor agent-3 :at 1000))))
              (= {:refused :no-such-ask} (last-of (conj asha-in (c :answer-ask :code "ZZZZ9999" :actor agent-3 :at 1000))))))

  (check "After the ask's end, every answer to an ask that has an answer gives what stands and changes nothing: the same answer again, and another answer, a no (Sid's call of 22 November)."
         (let [once (conj asked (c :answer-ask))]
           (and (unchanged? once (c :answer-ask :at 2000) {:yes {:root asha :actor asha :sign-in sign-in-2}})
                (unchanged? once (c :answer-ask :yes? false :at 2000) {:yes {:root asha :actor asha :sign-in sign-in-2}}))))

  (check "An answer that was told what stands is told it again when it comes again, before the ask's end or after: its result comes from what stands (Sid's call of 22 November)."
         (let [once (conj asked (c :answer-ask) (c :answer-ask :yes? false :at 141))]
           (and (= {:yes {:root asha :actor asha :sign-in sign-in-2}} (last-of once))
                (= {:yes {:root asha :actor asha :sign-in sign-in-2}} (last-of (conj once (c :answer-ask :yes? false :at 2000))))))))

;; -------------------------------------------------------------------- reads

(defn- read-checks
  "The checks of the reads."
  []
  (check "who-is of a root that has nothing gives no sign-in, no session, no own layer, and :own-layer-made? false."
         (= {:sign-in nil :session nil :own-layer nil :own-layer-made? false}
            (r/who-is r/empty-state {:root cy :sign-in sign-in-1 :session session-1})))

  (let [state (:state (run asha-in))]
    (check "under and live-under of a root that is no person give no agents, no sign-ins and no sessions."
           (and (= {:actors [] :sign-ins []} (r/under state cy))
                (= {:sign-ins {} :sessions {}} (r/live-under state cy)))))

  (let [state (:state (run (conj sitting (c :begin-sign-in :sign-in sign-in-2 :hash (h 102) :at 141))))]
    (check "who-is gives the session asked for with the sign-in that it is under, which can be another than the sign-in asked for."
           (= sign-in-1 (:sign-in (:session (who state sign-in-2 session-1))))))

  (let [state (:state (run (conj asha-in (c :make-actor :actor agent-2 :at 130) (c :make-actor :actor agent-1 :at 131)
                                 (c :begin-sign-in :sign-in sign-in-2 :hash (h 102) :at 132)
                                 (c :begin-sign-in :sign-in sign-in-1 :at 133)
                                 (c :begin-sign-in :sign-in sign-in-3 :hash (h 103) :at 134)
                                 (c :end-actor :actor agent-2 :at 135)
                                 (c :end-sign-in :sign-in sign-in-1 :at 136))))
        {:keys [actors sign-ins]} (r/under state asha)]
    (check "under gives the agents of the root, ended ones too, in the order in which they were made, and its live sign-ins, in the order in which they began."
           (and (= [[agent-2 true] [agent-1 false]] (map (juxt :actor :ended?) actors))
                (= [sign-in-2 sign-in-3] (map :sign-in sign-ins))
                (= {:sign-in sign-in-2 :actor asha :kind :browser :label nil :begun 132 :until 100000} (first sign-ins)))))

  (let [state (:state (run (conj sitting (c :make-actor) (c :end-actor :at 141) (c :close-session :at 142) (c :end-sign-in :at 143)
                                 (c :ask) (c :answer-ask :yes? false :at 144))))]
    (check "Nothing is deleted: an ended sign-in, a closed session, an ended actor and an answered ask can still be read."
           (and (:ended? (:sign-in (who state sign-in-1)))
                (:closed? (:session (who state sign-in-1 session-1)))
                (:ended? (first (:actors (r/under state asha))))
                (= :no (:state (r/ask-of state code-1)))))))

;; ---------------------------------------------------------------- take-out

(defn- take-out-checks
  "The checks of \"What takes a thing out of what is live\"."
  []
  (let [calls (conj sitting (c :begin-sign-in :sign-in sign-in-2 :hash (h 102) :until 200000 :at 141))
        late 100000]
    (check "A write that names a root takes out of what is live under it every sign-in and session whose time is at or before its :at, also when the write is refused."
           (let [{:keys [state last]} (run (conj calls (c :end-sign-in :sign-in sign-in-3 :at late)))]
             (and (= {:refused :no-such-sign-in} last)
                  (= [sign-in-2] (keys (:sign-ins (r/live-under state asha))))
                  (= {} (:sessions (r/live-under state asha))))))
    (check "The take-out comes also when the write is a repeat: what a repeat takes out follows its time."
           (let [{:keys [state last]} (run (conj calls (c :begin-sign-in :sign-in sign-in-2 :hash (h 102) :at late)))]
             (and (= {:begun sign-in-2} last)
                  (= [sign-in-2] (keys (:sign-ins (r/live-under state asha))))
                  (:ended? (:sign-in (who state sign-in-1)))))))

  (check "The take-out comes before the write is judged: open-session under a sign-in at the very time of its until is :not-live, and replace-secret of it is :not-live."
         (and (= {:refused :not-live} (last-of (conj signed-in (c :open-session :session session-2 :at 100000 :until 200000))))
              (= {:refused :not-live} (last-of (conj signed-in (c :replace-secret :at 100000))))))

  (let [state (:state (run (conj sitting (c :open-session :session session-2 :until 300 :at 141) (c :make-actor :at 100000))))]
    (check "A sign-in that is taken out so is ended, as by end-sign-in, with its sessions; a session alone is closed, as by close-session."
           (and (:ended? (:sign-in (who state sign-in-1)))
                (every? #(:closed? (:session (who state sign-in-1 %))) [session-1 session-2])))
    (let [state' (:state (run (conj sitting (c :open-session :session session-2 :until 300 :at 141) (c :make-actor :at 300))))]
      (check "A session whose time has passed is closed alone: its sign-in and the other sessions under it stay live."
             (and (not (:ended? (:sign-in (who state' sign-in-1))))
                  (:closed? (:session (who state' sign-in-1 session-2)))
                  (= [session-1] (keys (:sessions (r/live-under state' asha)))))))
    (check "Neither is taken back: a later write whose :at is earlier, from a copy whose clock is behind, leaves them out."
           (let [later (:state (run (conj sitting (c :make-actor :at 100000) (c :make-actor :actor agent-2 :at 150))))]
             (and (:ended? (:sign-in (who later sign-in-1)))
                  (= {:sign-ins {} :sessions {}} (r/live-under later asha))))))

  (check "invite, enter, own-layer-made and ask name no root and take nothing out."
         (every? (fn [call] (let [state (:state (run (conj sitting call)))]
                              (= [sign-in-1] (keys (:sign-ins (r/live-under state asha))))))
                 [(c :invite :person bo :claim bo-email :at 999999) (c :enter :at 999999) (c :own-layer-made :at 999999)
                  (c :ask :at 999999 :ends 9999999)]))

  (let [state (:state (run (conj signed-in (c :replace-secret :until 500 :previous-until 900) (c :make-actor :at 600))))]
    (check "A sign-in's time is its present secret's :until alone: a secret before it that stands longer does not keep it live."
           (:ended? (:sign-in (who state sign-in-1)))))

  (let [state (:state (run (conj signed-in (c :replace-secret :previous-until 145) (c :make-actor :at 5000))))]
    (check "The secret before the present one stays with the sign-in until the next replacing: who-is gives it after its t2 has passed."
           (= {:hash (h 101) :until 145} (:previous (:sign-in (who state sign-in-1))))))

  (let [state (:state (run (conj asha-in (c :begin-sign-in :until 500))))]
    (check "What watch-root and under give is bounded by what was live at the root's latest event: a sign-in whose time has passed with no event since is still given."
           (and (= [sign-in-1] (map :sign-in (:sign-ins (r/under state asha))))
                (= [sign-in-1] (keys (:sign-ins (r/live-under state asha)))))))

  (let [{:keys [state results]} (run (conj asha-in (c :begin-sign-in :until 130) (c :begin-sign-in :sign-in sign-in-2 :hash (h 102) :at 131)
                                           (c :open-session :sign-in sign-in-2 :until 132 :at 132) (c :ask :ends 120)
                                           (c :answer-ask :sign-in sign-in-3 :at 100)))]
    (check "A sign-in, a session or an ask whose end is at or before the :at of the event that makes it is taken and is never live: the sign-in begins ended, the session opens closed, and the ask is over, also to an answer from a slower clock (QUESTIONS.md, 9 and 19)."
           (and (= [{:begun sign-in-1} {:begun sign-in-2} {:open session-1} {:asked code-1} {:refused :over}] (drop 2 results))
                (:ended? (:sign-in (who state sign-in-1)))
                (:closed? (:session (who state sign-in-2 session-1)))
                (= [sign-in-2] (keys (:sign-ins (r/live-under state asha))))
                (= :waiting (:state (r/ask-of state code-1))))))

  (check "A thing is live while the time that it is held against is before its end: a sign-in until 1000 is live at a write at 999 and is taken out at a write at 1000."
         (= [[sign-in-1] []]
            (map #(vec (keys (:sign-ins (r/live-under (:state (run (conj asha-in (c :begin-sign-in :until 1000) (c :make-actor :at %)))) asha))))
                 [999 1000]))))

;; ------------------------------------------------------------------ the client

(defn- at-once
  "Runs the functions `fs` on threads of their own, let go at one moment.
  Gives what each gave, in order."
  [fs]
  (let [latch (CountDownLatch. 1)
        futures (mapv (fn [f] (future (.await latch) (f))) fs)]
    (.countDown latch)
    (mapv deref futures)))

(def ^:private client-writes
  "The client's function of each write."
  {:invite r/invite :enter r/enter :own-layer-made r/own-layer-made :begin-sign-in r/begin-sign-in
   :replace-secret r/replace-secret :end-sign-in r/end-sign-in :make-actor r/make-actor
   :end-actor r/end-actor :open-session r/open-session :close-session r/close-session :ask r/ask
   :answer-ask r/answer-ask})

(defn- through!
  "Makes the calls, in order, through the client. Gives what the last gave."
  [client calls]
  (reduce (fn [_ [kind input]] ((get client-writes kind) client input)) nil calls))

(defn- client-checks
  "The checks of the clients that `open` gives over a cluster of the
  reference: the functions, two copies of the runtime, watch-root, and
  close!."
  []
  (check "The reference has the eighteen functions of the contract under their own names."
         (and (= 18 (count contract-names))
              (every? #(fn? (some-> (ns-resolve 'softland.auth.reference %) deref)) contract-names)))

  (let [[a b] (two-clients)]
    (r/invite a (second (c :invite)))
    (check "A write returns when its effect can be read by any copy of the runtime: what one client wrote, another over the same atom reads at once."
           (and (= asha (:person (r/enter b (second (c :enter)))))
                (= layer-1 (:own-layer (r/who-is a {:root asha :sign-in sign-in-1 :session nil}))))))

  (let [runs (vec (for [_ (range 100)]
                    (let [[a b] (two-clients)]
                      (r/invite a (second (c :invite)))
                      (at-once [#(r/enter a (second (c :enter)))
                                #(r/enter b (second (c :enter :own-layer layer-2 :making making-2)))]))))]
    (check "Two enters of one new account through two copies of the runtime at one moment give one person, one own layer and one :making: so in 100 tries."
           (every? (fn [[x y]] (and (= x y) (= asha (:person x)))) runs)))

  (let [runs (vec (for [_ (range 100)]
                    (let [[a b] (two-clients)]
                      (r/invite a (second (c :invite)))
                      (at-once [#(r/enter a (second (c :enter))) #(r/enter b (second (c :enter :account bo-google)))]))))]
    (check "Two accounts that prove one claim through two copies at one moment: one becomes the person and the other is refused with :not-invited; so in 100 tries."
           (every? (fn [results] (= #{asha :not-invited} (set (map #(or (:person %) (:refused %)) results)))) runs)))

  (let [runs (vec (for [_ (range 100)]
                    (let [[a b] (two-clients)]
                      (through! a (concat asha-in bo-in [(c :ask)]))
                      [(at-once [#(r/answer-ask a (second (c :answer-ask)))
                                 #(r/answer-ask b (second (c :answer-ask :root bo :actor bo :sign-in sign-in-3)))])
                       (r/ask-of a code-1)])))]
    (check "Two persons who answer one ask through two copies at one moment: one answer stands, and both are told which; so in 100 tries."
           (every? (fn [[[x y] k]] (and (= x y) (some? (:yes x)) (= (:yes x) (:answer k)))) runs)))

  (let [[a b] (two-clients)
        seen (atom [])]
    (through! a asha-in)
    (let [stop (r/watch-root a asha #(swap! seen conj %))
          at-registration @seen
          _ (r/make-actor a (second (c :make-actor)))
          after-nothing-live @seen
          _ (r/begin-sign-in b (second (c :begin-sign-in)))
          _ (r/open-session a (second (c :open-session :driven-by {:kind :person})))
          two @seen
          _ (stop)
          _ (r/close-session a (second (c :close-session)))]
      (check "No call is owed at the registration of watch-root."
             (= [] at-registration))
      (check "watch-root calls f with what is live under the root each time it changed, and not for a write that changed nothing live."
             (and (= [] after-nothing-live) (= 2 (count two))))
      (check "watch-root's function is called through whichever copy of the runtime the change came: a sign-in begun through another client is seen."
             (= [sign-in-1] (keys (:sign-ins (first two)))))
      (check "watch-root's value is {:sign-ins {G {:actor :kind :hash :until :previous}} :sessions {S {:sign-in :until}}}, and does not carry :driven-by."
             (= {:sign-ins {sign-in-1 {:actor asha :kind :browser :hash (h 101) :until 100000 :previous nil}}
                 :sessions {session-1 {:sign-in sign-in-1 :until 50000}}}
                (second two)))
      (check "watch-root gives a function of no argument that stops the watching: a change after it calls nothing."
             (= two @seen))))

  (let [[a b] (two-clients)
        seen (atom 0)]
    (through! a asha-in)
    (r/watch-root a asha (fn [_] (swap! seen inc)))
    (r/close! a)
    (let [on-closed (vec (concat (for [f write-fns] (f a (second (c :invite))))
                                 [(r/who-is a {:root asha :sign-in sign-in-1 :session nil}) (r/ask-of a code-1)
                                  (r/under a asha) (r/live-under a asha) (r/watch-root a asha (fn [_] nil))]))
          later (r/begin-sign-in b (second (c :begin-sign-in)))]
      (check "After close!, every call on the client gives {:could-not :closed}, watch-root's in place of the function; close! calls no function that it watched with again; and another client over the same atom goes on."
             (and (every? #(= {:could-not :closed} %) on-closed)
                  (zero? @seen)
                  (= {:begun sign-in-1} later)))))

  (let [a (new-client)
        calls (atom 0)]
    (through! a asha-in)
    (r/watch-root a asha (fn [_] (swap! calls inc) (throw (ex-info "a watch that throws" {}))))
    (let [first-write (r/begin-sign-in a (second (c :begin-sign-in)))
          second-write (r/open-session a (second (c :open-session)))]
      (check "A watching function that throws makes no call throw, and is called again at the next change (QUESTIONS.md, 27)."
             (and (= {:begun sign-in-1} first-write) (= {:open session-1} second-write) (= 2 @calls)))))

  (let [a (new-client)]
    (check "watch-root takes a function: a keyword, a map or a var in its place gives {:could-not :read-the-event} (QUESTIONS.md, 27)."
           (every? #(= unfit (r/watch-root a asha %)) [:a-keyword {} #'clojure.core/inc])))

  (let [a (new-client)
        calls (atom [])
        stop-second (promise)]
    (through! a asha-in)
    (r/watch-root a asha (fn [_] (swap! calls conj :first) (@stop-second)))
    (deliver stop-second (r/watch-root a asha (fn [_] (swap! calls conj :second))))
    (r/begin-sign-in a (second (c :begin-sign-in)))
    (r/open-session a (second (c :open-session)))
    (let [stopped @calls
          [a2 b2] (two-clients)
          calls2 (atom [])]
      (through! a2 asha-in)
      (r/watch-root a2 asha (fn [_] (swap! calls2 conj :first) (r/close! b2)))
      (r/watch-root b2 asha (fn [_] (swap! calls2 conj :second)))
      (r/begin-sign-in a2 (second (c :begin-sign-in)))
      (r/open-session a2 (second (c :open-session)))
      (check "A watch that an earlier watch's function stops, or whose client it closes, is not called for that change nor for any later one."
             (and (= [:first :first] stopped) (= [:first :first] @calls2)))))

  (let [a (new-client)
        seen-first (atom [])
        seen-second (atom [])
        wrote? (atom false)]
    (through! a asha-in)
    (r/watch-root a asha (fn [v]
                           (swap! seen-first conj v)
                           (when (compare-and-set! wrote? false true)
                             (r/begin-sign-in a (second (c :begin-sign-in :sign-in sign-in-2 :hash (h 102) :at 131))))))
    (r/watch-root a asha (fn [v] (swap! seen-second conj v)))
    (r/begin-sign-in a (second (c :begin-sign-in)))
    (let [live (r/live-under a asha)]
      (check "A write that a watching function makes reaches every watch after the change that came before it: each watch is handed the changes in their order, and the last value that it holds is what is live."
             (and (= [#{sign-in-1} #{sign-in-1 sign-in-2}]
                     (map (comp set keys :sign-ins) @seen-first)
                     (map (comp set keys :sign-ins) @seen-second))
                  (= live (peek @seen-first) (peek @seen-second)))))))

;; --------------------------------------------------------- generated histories

(def ^:private invariants
  "The rules that hold after every step of a generated history, each with
  the words that its check prints."
  [[:no-throw "no step throws"]
   [:one-person "an account names one person at most, and the same one for good; a claim names one person for good; a person has one claim"]
   [:repeat "a write that arrives twice gives the result of the first, by the parts that bind, an enter with :own-layer-made? as it stands, and leaves the state as once, but for what its own time takes out, the time that a repeat of open-session moves on, and the :says of an enter whose :at is later than the kept one's; a replacement that arrives after a newer one is no repeat: it is refused when its sign-in is not live or its old hash is not the hash now, and taken when that hash has come back"]
   [:refusal "a write that is refused changes nothing but what its time takes out"]
   [:no-revival "an end is never taken back: no later call makes an ended sign-in, a closed session or an ended actor live again"]
   [:time "what is live under a root holds no sign-in and no session whose time is at or before the :at of that root's latest write"]
   [:standing "what is live under a root holds no open session whose sign-in is not live in it"]
   [:end-actor "end-actor ends every actor beneath it, in any depth, with every sign-in and session of each, and an actor that an ended one made has ended"]
   [:answer "the first answer to an ask stands, and the ask keeps its hash, :asks and :ends; every answer to an ask that has an answer gives what stands; and an answer to an ask that waits, at or after its :ends or to an ask that was over when it was made, is refused with :over"]
   [:driven-by "who-is of a session gives the :driven-by that it was opened with"]
   [:fit "an input that does not fit gives {:could-not :read-the-event} and changes nothing, and an input that fits is judged"]])

(def ^:private empty-tracker
  "What the checks of a history know before its first step."
  {:account->person {} :claim->person {} :person->claim {}
   :ended {:sign-ins #{} :sessions #{} :agents #{}}
   :latest-at {} :asks {} :over-when-made #{} :standing {} :driven-by {}})

(defn- taken?
  "Whether a result says that the write was taken: no refusal, no :could-not."
  [result]
  (not (or (contains? result :refused) (contains? result :could-not))))

(defn- swept
  "The state before a call, with what the call's time takes out under its
  root taken out, by the reference's own `take-out`."
  [state kind input]
  (if (contains? r/names-a-root kind) (r/take-out state (:root input) (:at input)) state))

(defn- first-seen
  "`m` with each pair of `kvs` whose key it does not hold yet."
  [m kvs]
  (reduce (fn [m [k v]] (if (contains? m k) m (assoc m k v))) m kvs))

(defn- one-person-violations
  "Checks that an account names one person, for good; that a claim names
  one person, for good; that a person has one claim; and that the links,
  claims and persons of the state agree."
  [tracker {:keys [links persons claims]}]
  (concat
   (for [[account P] (:account->person tracker) :when (not= P (get-in links [account :person]))] [:account-moved account])
   (for [[claim P] (:claim->person tracker) :when (not= P (get claims claim))] [:claim-moved claim])
   (for [[P claim] (:person->claim tracker) :when (not= claim (get-in persons [P :claim]))] [:claim-of-person-changed P])
   (for [[claim P] claims :when (not= claim (get-in persons [P :claim]))] [:claim-not-the-persons claim P])
   (for [[account {:keys [person]}] links :when (not= account (get-in persons [person :account]))] [:link-not-the-persons account])
   (let [accounts (keep :account (vals persons))]
     (when (not= (count accounts) (count (distinct accounts))) [[:one-account-two-persons]]))))

(defn- expected-after-repeat
  "The state that a repeat of `kind` with `input` must leave after
  `before`: what its time takes out; then for open-session the session's
  time moved on while it is open; and for an enter whose :at is later than
  the :at that its link keeps, the link's :says and its :at set from it."
  [kind input before]
  (let [s0 (swept before kind input)]
    (case kind
      :open-session (let [path [:roots (:root input) :sessions (:session input)]
                          s (get-in s0 path)]
                      (if (and s (= (:sign-in input) (:sign-in s)) (not (:closed? s)))
                        (assoc-in s0 (conj path :until) (max (:until s) (:until input)))
                        s0))
      :enter (let [link (get-in s0 [:links (:account input)])]
               (if (and link (> (:at input) (:said-at link)))
                 (update-in s0 [:links (:account input)] assoc :says (:says input) :said-at (:at input))
                 s0))
      s0)))

(defn- bound-parts
  "The parts of a result that a repeat must give again: all of it, but for
  enter, whose :own-layer-made? says how the flag stands at the repeat."
  [kind result]
  (if (= kind :enter) (select-keys result [:person :own-layer :making]) result))

(defn- repeat-violations
  "Checks a repeat of the taken call `j` of the world: it gives the first
  result by the parts that bind, an enter with :own-layer-made? as it
  stands, and leaves the state as once. A replacement that arrives after a
  newer one is no repeat: it is judged by the present hash and the one
  before it alone, and so refused with :not-live when its sign-in is no
  longer live, with :wrong-secret when its old hash is not the hash now,
  and taken, as a new replacement, when that hash has come back."
  [w before [kind input :as call] result after j]
  (let [first-result (:result (nth (:taken w) j))
        s0 (swept before kind input)
        g (when (= kind :replace-secret)
            (:sign-in (r/who-is s0 {:root (:root input) :sign-in (:sign-in input) :session nil})))
        late? (and (= kind :replace-secret)
                   (not (and g (= (:new-hash input) (:hash g)) (= (:old-hash input) (get-in g [:previous :hash])))))]
    (if late?
      (let [live? (and g (not (:ended? g)) (not (:actor-ended? g)) (< (:at input) (:until g)))
            want (cond (not live?) {:refused :not-live}
                       (not= (:old-hash input) (:hash g)) {:refused :wrong-secret}
                       :else {:replaced (:sign-in input)})
            g' (:sign-in (r/who-is after {:root (:root input) :sign-in (:sign-in input) :session nil}))]
        (cond-> []
          (not= want result) (conj [:late-replacement call want result])
          (and (contains? want :refused) (not= s0 after)) (conj [:late-replacement-changed call])
          (and (contains? want :replaced)
               (not= [(:new-hash input) {:hash (:old-hash input) :until (:previous-until input)}]
                     [(:hash g') (:previous g')]))
          (conj [:hash-that-came-back-not-taken call])))
      (cond-> []
        (not= (bound-parts kind first-result) (bound-parts kind result)) (conj [:other-result call first-result result])
        (and (= kind :enter)
             (not= (:own-layer-made? result)
                   (:own-layer-made? (r/who-is before {:root (:person result) :sign-in (:person result) :session nil}))))
        (conj [:flag-not-as-it-stands call result])
        (not= (expected-after-repeat kind input before) after) (conj [:state-changed call])))))

(defn- ended-in
  "The sign-ins that have ended, the sessions that are closed, and the
  agents that have ended, in a state, each as `[root id]`."
  [state]
  {:sign-ins (set (for [[P rt] (:roots state) [G g] (:sign-ins rt) :when (:ended? g)] [P G]))
   :sessions (set (for [[P rt] (:roots state) [S s] (:sessions rt) :when (:closed? s)] [P S]))
   :agents (set (for [[P rt] (:roots state) [A a] (:agents rt) :when (:ended? a)] [P A]))})

(defn- views
  "For each root of the state and each root that the tracker knows, what is
  live under it and what `under` gives."
  [tracker state]
  (into {} (map (fn [P] [P {:live (r/live-under state P) :under (r/under state P)}]))
        (into (set (keys (:roots state))) (keys (:latest-at tracker)))))

(defn- revival-violations
  "Checks that each thing that had ended is still ended by who-is and under,
  and is not in what is live."
  [tracker after vs]
  (let [{:keys [sign-ins sessions agents]} (:ended tracker)]
    (concat
     (for [[P G] sign-ins
           :let [g (:sign-in (r/who-is after {:root P :sign-in G :session nil}))]
           :when (or (not (:ended? g)) (contains? (get-in vs [P :live :sign-ins]) G))]
       [:sign-in-live-again P G])
     (for [[P S] sessions
           :let [s (:session (r/who-is after {:root P :sign-in P :session S}))]
           :when (or (not (:closed? s)) (contains? (get-in vs [P :live :sessions]) S))]
       [:session-open-again P S])
     (for [[P A] agents
           :let [a (some #(when (= A (:actor %)) %) (get-in vs [P :under :actors]))]
           :when (not (:ended? a))]
       [:agent-live-again P A]))))

(defn- time-violations
  "Checks that what is live under each root holds nothing whose time is at
  or before the :at of the root's latest write."
  [tracker vs]
  (for [[P at] (:latest-at tracker)
        :let [{:keys [sign-ins sessions]} (get-in vs [P :live])]
        [what thing until] (concat (for [[G g] sign-ins] [:sign-in G (:until g)])
                                   (for [[S s] sessions] [:session S (:until s)]))
        :when (<= until at)]
    [what P thing until at]))

(defn- standing-violations
  "Checks that each open session in what is live has its sign-in live in it."
  [vs]
  (for [[P {:keys [live]}] vs
        [S s] (:sessions live)
        :when (not (contains? (:sign-ins live) (:sign-in s)))]
    [:session-of-no-live-sign-in P S]))

(defn- below
  "The agent `A` and every agent beneath it, from the pairs of `made-by`."
  [made-by A]
  (loop [found #{A}]
    (let [more (into found (keep (fn [[B m]] (when (contains? found m) B))) made-by)]
      (if (= more found) found (recur more)))))

(defn- end-actor-violations
  "Checks that after a taken end-actor every agent beneath it has ended, with
  its sign-ins and their sessions; and that at every step an agent that an
  ended agent made has ended."
  [[kind input] result after vs]
  (concat
   (when (and (= kind :end-actor) (contains? result :ended))
     (let [P (:root input)
           actors (get-in vs [P :under :actors])
           ended (into #{} (keep #(when (:ended? %) (:actor %))) actors)
           all (below (map (juxt :actor :made-by) actors) (:actor input))
           sign-ins (for [G (sign-ins-of after P)
                          :let [g (:sign-in (r/who-is after {:root P :sign-in G :session nil}))]
                          :when (contains? all (:actor g))]
                      [G g])
           Gs (set (map first sign-ins))]
       (concat
        (for [A all :when (not (contains? ended A))] [:agent-not-ended P A])
        (for [[G g] sign-ins :when (not (:ended? g))] [:sign-in-not-ended P G])
        (for [S (sessions-of after P)
              :let [s (:session (r/who-is after {:root P :sign-in P :session S}))]
              :when (and (contains? Gs (:sign-in s)) (not (:closed? s)))]
          [:session-not-closed P S]))))
   (for [[P {:keys [under]}] vs
         :let [ended (into #{} (keep #(when (:ended? %) (:actor %))) (:actors under))]
         a (:actors under)
         :when (and (contains? ended (:made-by a)) (not (:ended? a)))]
     [:made-by-an-ended-agent P (:actor a)])))

(defn- answer-violations
  "Checks that each ask keeps its hash, :asks and :ends; that a standing
  answer stays; that every answer to an ask that has an answer gives what
  stands; and that an answer to an ask that waits, at or after its :ends
  or to an ask that was over when made, is refused with :over."
  [tracker before [kind input] result after]
  (concat
   (for [[U k] (:asks tracker) :when (not= k (select-keys (r/ask-of after U) [:hash :asks :ends]))] [:ask-changed U])
   (for [[U read] (:standing tracker) :when (not= read (select-keys (r/ask-of after U) [:state :answer]))]
     [:answer-changed U])
   (when (= kind :answer-ask)
     (let [U (:code input)
           k (r/ask-of before U)]
       (cond
         (nil? k) nil
         (not= :waiting (:state k))
         (let [stands (if (= :yes (:state k)) {:yes (:answer k)} {:no U})]
           (when (not= stands result) [[:not-what-stands input stands result]]))
         (and (or (>= (:at input) (:ends k)) (contains? (:over-when-made tracker) U))
              (not= {:refused :over} result))
         [[:not-over input result]])))))

(defn- driven-by-violations
  "Checks that who-is of each session that the history opened gives the
  :driven-by of the open-session that made it."
  [tracker after]
  (for [[[P S] {:keys [sign-in driven-by]}] (:driven-by tracker)
        :let [s (:session (r/who-is after {:root P :sign-in sign-in :session S}))]
        :when (not= driven-by (:driven-by s))]
    [:driven-by-changed P S]))

(defn- fit-violations
  "Checks that an input that does not fit gives :read-the-event and changes
  nothing, and that one that fits is judged."
  [before result after fits?]
  (if fits?
    (when (= unfit result) [[:fitting-input-not-judged]])
    (cond-> []
      (not= unfit result) (conj [:unfit-input-judged result])
      (not= before after) (conj [:unfit-input-changed-the-state]))))

(defn- learn
  "What the checks of the history know after a step."
  [tracker before call result after fits?]
  (let [[kind input] (when (vector? call) call)
        new-ask?(and fits? (= kind :ask) (nil? (r/ask-of before (:code input))) (some? (r/ask-of after (:code input))))
        answered? (and fits? (= kind :answer-ask)
                       (= :waiting (:state (r/ask-of before (:code input))))
                       (#{:yes :no} (:state (r/ask-of after (:code input)))))
        opened? (and fits? (= kind :open-session) (= {:open (:session input)} result)
                     (nil? (:session (r/who-is before {:root (:root input) :sign-in (:sign-in input) :session (:session input)}))))]
    (cond-> (-> tracker
                (update :account->person first-seen (map (fn [[a l]] [a (:person l)]) (:links after)))
                (update :claim->person first-seen (:claims after))
                (update :person->claim first-seen (keep (fn [[P p]] (when (:claim p) [P (:claim p)])) (:persons after)))
                (assoc :ended (ended-in after))
                (update :asks first-seen (map (fn [[U _]] [U (select-keys (r/ask-of after U) [:hash :asks :ends])]) (:asks after))))
      (and new-ask? (<= (:ends input) (:at input))) (update :over-when-made conj (:code input))
      answered? (assoc-in [:standing (:code input)] (select-keys (r/ask-of after (:code input)) [:state :answer]))
      opened? (assoc-in [:driven-by [(:root input) (:session input)]]
                        {:sign-in (:sign-in input) :driven-by (:driven-by input)}))))

(defn- check-step
  "Checks every rule of `invariants` after one step of a history, and
  learns from the step. Gives the tracker and the violations, each as
  `[rule what]`."
  [tracker w before call result after {:keys [fits? repeat-of]}]
  (let [[kind input] (when (vector? call) call)
        tracker (if (and fits? (contains? r/names-a-root kind))
                  (assoc-in tracker [:latest-at (:root input)] (:at input))
                  tracker)
        vs (views tracker after)
        tagged (fn [rule found] (map (fn [x] [rule x]) found))
        violations (concat
                    (tagged :fit (fit-violations before result after fits?))
                    (when fits?
                      (concat
                       (when repeat-of (tagged :repeat (repeat-violations w before call result after repeat-of)))
                       (when (contains? result :refused)
                         (when (not= (swept before kind input) after) [[:refusal [call result]]]))
                       (tagged :answer (answer-violations tracker before call result after))
                       (tagged :end-actor (end-actor-violations call result after vs))))
                    (tagged :one-person (one-person-violations tracker after))
                    (tagged :no-revival (revival-violations tracker after vs))
                    (tagged :time (time-violations tracker vs))
                    (tagged :standing (standing-violations vs))
                    (tagged :driven-by (driven-by-violations tracker after)))]
    [(learn tracker before call result after fits?) (vec violations)]))

(defn- steps-of
  "How many steps the history of `seed` has: 40 to 120."
  [seed]
  (+ 40 (.nextInt (Random. (bit-xor (long seed) 0x5DEECE66D)) 81)))

(defn- run-history
  "Runs the history of `seed` against the copy `broken` of the rules, nil
  for the rules as they are, checking every rule after every step. Gives
  the violations that it found, at most `most`, each with the seed, the
  step, the call and the result."
  [seed broken most]
  (let [steps (steps-of seed)]
    (loop [k 0
           w (new-world seed)
           state r/empty-state
           tracker empty-tracker
           found []]
      (if (or (= k steps) (>= (count found) most))
        found
        (let [[w call meta] (next-call w state)
              stepped (try (r/step state call broken) (catch Throwable t t))]
          (if (instance? Throwable stepped)
            (conj found {:rule :no-throw :seed seed :step k :call call :threw (str stepped)})
            (let [[after result] stepped
                  [tracker violations] (check-step tracker w state call result after meta)]
              (recur (inc k)
                     ;; An answer that was told what stands is taken too: told it again, it
                     ;; must hear the same, since its result comes from what stands.
                     (if (and (taken? result) (not (:repeat-of meta)))
                       (update w :taken conj {:call call :result result})
                       w)
                     after
                     tracker
                     (into found (map (fn [[rule what]] {:rule rule :seed seed :step k :call call :result result :what what}))
                           violations)))))))))

(defn- history-checks
  "Runs `n` histories from `seed` against the rules as they are, and says
  for each rule of `invariants` whether it held after every step."
  [seed n]
  (let [found (into [] (mapcat #(run-history (+ seed %) nil 3)) (range n))
        by-rule (group-by :rule found)
        steps (reduce + (map #(steps-of (+ seed %)) (range n)))]
    (doseq [[rule words] invariants]
      (check (str "In " n " generated histories, " steps " steps in all, after every step " words ".")
             (empty? (get by-rule rule))
             (first (get by-rule rule))))))

(defn- broken-checks
  "Runs, for each broken copy of the rules, the histories of `seed` until
  one of them catches it, `n` at most, and says whether one did."
  [seed n]
  (doseq [[broken how] (sort-by key r/breaks)]
    (let [caught (some (fn [i] (first (run-history (+ seed i) broken 1))) (range n))]
      (check (str "The generated histories catch the copy of the rules in which " how ".")
             (and caught (not= :no-throw (:rule caught)))
             (or caught (str "not caught in " n " histories"))))))

(def ^:private histories
  "How many histories the run makes against the rules as they are, and at
  most against each broken copy."
  5000)

(defn -main
  "Runs every check, prints the counts, and ends the process with a status.
  The first argument, when given, is the seed of the histories."
  [& args]
  (let [seed (or (some-> (first args) parse-long) (System/currentTimeMillis))]
    (println "Seed of the histories:" seed)
    (rules-checks)
    (invite-checks)
    (enter-checks)
    (begin-sign-in-checks)
    (replace-secret-checks)
    (end-sign-in-checks)
    (make-actor-checks)
    (end-actor-checks)
    (session-checks)
    (ask-checks)
    (read-checks)
    (take-out-checks)
    (client-checks)
    (history-checks seed histories)
    (broken-checks seed histories)
    (finish)))
