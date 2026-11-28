(ns softland.store.read-path-proof
  "The proof of the read path on Rama's in-process cluster: facts go into
  both depots through the door, and come back out through
  `softland.store.read-path`, once and kept live. One command, from the
  repository root:

    clojure -M:store-module -m softland.store.read-path-proof

  It prints each thing that it showed on a line that starts with SHOWN,
  prints those lines again together at the end, and exits with a status that
  is not zero when a check fails.

  The proof is the read path's one asker: a hand-played one. No view, no
  screen, no tool and no agent asks. Each read of this proof goes through
  the read path's five calls, and each act goes in through the door, but
  for three things that the check of a lane's acts appends straight to
  the streaming depot, as a sender with no door can. So every row that
  this proof reads was written by a gate, and a gate's write
  leaves one of three things at a place: no fact, a retract, or a value. The
  fourth thing that a reader can be told, a row that holds no value of the
  form, is not reached here: the read path's pure proof holds it, on a row
  that it writes by hand.

  What it reads round the read path. For what no call shows, the proof reads
  the read path's own memory, its three maps, from the field of its type: to
  see that a push has left the map with its last reader, that no entry is
  without a reader, which fields an entry has, and that the counts of the
  pushes are the counts of the map's entries. To find the task of a lane
  it selects, with two handles of its own, the lane's last stamp in the
  table that the lane's gate keeps for a task: only the task that decides
  the lane's acts holds one. Each line that rests on that says so. It
  reads no table of facts round the read path.

  Where the proof stands in for a call of Rama's client. The read path's
  two reading calls are macros of Rama's that end in two functions,
  `compiled-foreign-select-async` and `compiled-foreign-proxy-async`.
  Clojure lets a proof put a function of its own in the place of those, and
  of `close!`, for a while. The proof does so to count the calls of a read;
  to note the table and the routing id of each call; to keep each push and
  each callback that a read opened; to make a select throw, and to make one
  never come back; to make the opening of a push throw, and to make one
  never come back. Each line that rests on that says so. It also puts a function in the place of two pure functions of the read
  path, to count the rounds of a read asked once and to call a private
  function. The other throw is real: a module name under which no module
  runs.

  How the proof shows that nothing came. A check that holds \"the asker is
  given nothing\" cannot hold it by looking at once: a change that is still
  on its way would pass as none. So after the act that must give nothing,
  the proof sends a second act that changes the same push, and waits until
  a witness has that second change: a second live read of the proof's that
  holds the same push and is told every change of it. The changes of one
  push come in order, and one callback is handled for every read that holds
  the push before the next callback begins. So when the witness has the
  second change, the first one has been handled for the asker too.

  A second cluster. One check needs a batch of the base to run a second
  time. It has a cluster of its own, after the first is closed, as the
  module's proof has one: before that cluster is launched the proof puts a
  function of its own in the place of one pure function that the walk of
  both gates calls, `softland.store.gate.decision/writes`. It notes the
  rows of facts of each decision and throws once, at the decision that the
  proof names; and the proof puts the store's own function back after. The
  store's module is loaded before the proof's function goes into the var:
  the walk calls what the var holds only when its namespace was loaded
  with the store's own function there (found in a run of this build).
  Every other check runs on the functions as the store defines them.

  State. The proof starts one in-process cluster with four tasks and two
  workers, launches the module `Store` in it, and closes it at the end; the
  cluster's files are temporary and are Rama's. It opens one door and four
  read paths on the module: the read path of most checks; one with small
  numbers, for the checks of a run's size; one whose wait is 300 ms; and one
  for the checks that count pushes, so that no other check's push is in its
  maps. The group of the read path's own bound opens two more, each with
  one small number: one that may hold 3 pushes on one table under one
  routing id, and one whose live reads may need 1 check. The group of the
  base that is read where the asker's lane lives opens one more, for its
  counts of pushes; the check of a lane's acts opens two more, each with
  small bounds of a read of acts. It owns atoms: what
  it holds of the cluster, the lines that it showed, each permission that it
  made, each thing that the door sent, and each live read that it opened.
  Near the end it starts the module again in the cluster, by Rama's module
  update with the unchanged module; Rama then prints some lines of its own
  log, marked ERROR, that are no failed checks.

  What it does not reach is in the folder's README."
  (:refer-clojure :exclude [agent])
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]
            [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [com.rpl.rama.test :as rtest]
            [softland.store.door :as door]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.gate.chain :as chain]
            [softland.store.gate.decision :as decision]
            [softland.store.gate.first-act :as first-act]
            [softland.store.module :as module]
            [softland.store.permissions :as perm :refer [person agent run]]
            [softland.store.read-path :as read-path]
            [softland.store.read-path.query :as query]
            [softland.store.read-path.sources :as sources]
            [softland.store.record.rows :as rows])
  (:import [com.rpl.rama.diffs ResyncDiff UngracefulTerminationDiff]
           [java.util UUID]
           [java.util.concurrent CompletableFuture CountDownLatch]
           [softland.store.read_path ReadPath]))

(defonce ^{:private true
           :doc "The lines that the proof has printed with SHOWN, in order, for the
  recap that `-main` prints at the end."}
  shown-lines (atom []))

(defn- shown
  "Prints one line of what the proof observed, and keeps it for the recap
  that `-main` prints at the end."
  [& xs]
  (let [line (apply print-str "SHOWN" xs)]
    (swap! shown-lines conj line)
    (println line)
    (flush)))

(defn- check
  "Checks that what was observed is what must be, and prints the check's
  sentence after PASSED or FAILED, so that a run lists what it checked. Gives
  what was observed."
  [what expected observed]
  (println (if (is (= expected observed) what) "PASSED" "FAILED") what)
  observed)

;; ------------------------------------------------------- the proof's own state

(def ^:private tasks "The number of tasks of the proof's cluster." 4)

(defonce ^{:private true
           :doc "What the proof holds of the cluster that it runs on, set by
  `open-round!`: the cluster, the module's name, the door, the proof's read
  paths, and the two handles of the proof's own, of the table that each
  gate keeps for a task."}
  round
  (atom nil))

(defonce ^{:private true
           :doc "The permission that the proof made for each who in each lane, by
  [who lane]: what `act!` sends an act under."}
  permissions (atom {}))

(defonce ^{:private true
           :doc "What the door gave for each act that the proof sent, by act-id: the
  digest with which the door's answer is read again."}
  sent-things (atom {}))

(defonce ^{:private true
           :doc "Every live read that the proof opened with `live`, in order, each
  with the notes of its function's calls: what the check of the first thing
  that a function is given goes through."}
  live-reads (atom []))

(defn- open-round!
  "Takes the cluster. Opens the door and the read paths of the proof, and
  the two handles that the proof itself selects with."
  [ipc]
  (let [m (rama/get-module-name module/Store)]
    (reset! round
            {:ipc             ipc
             :module          m
             :door            (door/open ipc m)
             :read-path       (read-path/open ipc m)
             :small           (read-path/open ipc m {:run-start-places 4 :max-places-of-one-run 10
                                                     :max-checks-of-one-read 6})
             :short           (read-path/open ipc m {:wait-ms 300})
             :counted         (read-path/open ipc m)
             :streaming-task  (rama/foreign-pstate ipc m "$$streaming-task")
             :microbatch-task (rama/foreign-pstate ipc m "$$microbatch-task")})))

(defn- the-rp "The read path of most checks." [] (:read-path @round))
(defn- the-door "The proof's door." [] (:door @round))

;; ------------------------------------------------------------- through the door

(defn- perm
  "The permission that a who holds in a lane, as the proof made it."
  [who lane]
  (get @permissions [who lane]))

(defn- act!
  "Sends one act through the door (or builds one of a who into a lane under
  the permission that the proof made for that who) and reads its answer: at
  once for a streaming act, with a bounded wait for an act of the base.
  Gives {:act-id :stamp :accepted? :answer :sent}."
  ([an-act]
   (let [sent (door/send-act (the-door) an-act)]
     (if (:could-not sent)
       {:sent sent :accepted? false}
       (let [answer (if (= :depot-holds (:ack sent))
                      (door/answer-of (the-door) (:act-id sent) (:digest sent) 30000)
                      (door/answer-of (the-door) (:act-id sent) (:digest sent)))]
         (swap! sent-things assoc (:act-id sent) sent)
         {:sent sent :answer answer :act-id (:act-id sent) :stamp (:stamp answer)
          :accepted? (= :accepted (:answer answer))}))))
  ([who lane facts]
   (act! {:version 1 :who who :lane lane :permission (perm/permission-id (perm who lane))
          :session :read-path-proof :facts facts})))

(defn- put!
  "Sends one act that must be accepted, and holds that it was. Gives what
  `act!` gives."
  ([an-act]
   (let [got (act! an-act)]
     (is (:accepted? got) (str "an act of " (:who an-act) " into " (:lane an-act) " is accepted: " (pr-str (or (:answer got) (:sent got)))))
     got))
  ([who lane facts]
   (put! {:version 1 :who who :lane lane :permission (perm/permission-id (perm who lane))
          :session :read-path-proof :facts facts})))

(defonce ^{:private true
           :doc "A count for minting a fresh permission id each time a helper
  gives a permission again."}
  give-salt (atom 0))

(defn- make!
  "Makes a lane through the door, with a holder: a streaming lane by its
  holder, the base by the system (opened at its making to anyone at all).
  Keeps the holder's permission, and the open permission when opened."
  ([lane holder] (make! lane holder {:open? (= :base lane)}))
  ([lane holder {:keys [open?] :as opts}]
   (let [own (perm/first-permission holder lane)]
     (swap! permissions assoc [holder lane] own)
     (when open?
       (swap! permissions assoc [:anyone lane] (perm/open-permission lane holder own)))
     (put! (perm/making-act lane holder holder :read-path-proof opts)))))

(defn- give!
  "A holder of a lane gives another who a permission in it, lent from her
  own; with a word or map, the permission says what it allows. Keeps it."
  ([lane giver who] (give! lane giver who nil))
  ([lane giver who allows]
   (let [opts (if (and (map? allows) (not-any? #(contains? allows %) [:see :write :give :owner]))
                (assoc allows :salt (swap! give-salt inc))
                {:allows allows :salt (swap! give-salt inc)})
         given (perm/given-permission who lane (perm giver lane) opts)]
     (swap! permissions assoc [who lane] given)
     (put! giver lane [(perm/permission-fact given)]))))

(defn- fact "A fact with a value, as a maker gives it." [e k v] {:e e :k k :v v})

(defn- setting!
  "Opens a lane to everyone by a permission to `:anyone`, or closes it to
  its members by ending that permission."
  [lane who word]
  (case word
    :everyone
    (let [open-p (perm/given-permission :anyone lane (perm who lane)
                                        {:allows :see :lent-from nil :salt (swap! give-salt inc)})]
      (swap! permissions assoc [:anyone lane] open-p)
      (put! who lane [(perm/permission-fact open-p)]))
    :members
    (let [open-p (perm :anyone lane)]
      (swap! permissions dissoc [:anyone lane])
      (put! who lane [(perm/end-fact open-p)]))))

(defn- list! "Writes what a lane builds on." [lane who listed]
  (put! who lane [(fact lane :softland.store/builds-on listed)]))

(defn- mark! "Writes the keys that a lane marks." [lane who marked]
  (put! who lane [(fact lane :softland.store/marked-keys marked)]))

;; ---------------------------------------------------------- through the read path

(defn- ask
  "Reads once through the read path of most checks."
  ([query] (read-path/read-once (the-rp) query))
  ([rp query] (read-path/read-once rp query)))

(defn- live
  "Opens a live read with a function of the proof's that notes each call: the
  change, the thread, and whether another call for the same read was
  running. Gives what `read-live` gave, with `:calls`, the atom of the
  notes, and `:read-path`."
  ([query] (live (the-rp) query))
  ([rp query]
   (let [calls (atom [])
         inside (atom 0)
         f (fn [change]
             (let [n (swap! inside inc)]
               (swap! calls conj {:change change :thread (Thread/currentThread) :overlap? (< 1 n)})
               (swap! inside dec)))
         caller (Thread/currentThread)
         got (read-path/read-live rp query f)
         read (assoc got :calls calls :read-path rp :caller caller)]
     (swap! live-reads conj read)
     read)))

(defn- changes "Every map that a live read's function was given, in order." [read]
  (mapv :change @(:calls read)))

(defn- later "Every map that a live read's function was given after its first call." [read]
  (vec (rest (changes read))))

(defn- screen
  "The screen of a live read: made only from what its function was given.
  Each `{:anew}` in the place of all, each `{:changed}` put in."
  [read]
  (reduce (fn [screen change]
            (cond
              (:anew change) (:anew change)
              (:changed change) (reduce (fn [screen [place place-answer]]
                                          (cond
                                            (contains? screen :place) (assoc screen :place place-answer)
                                            (= :no-fact-given (:holds place-answer)) (update screen :places dissoc place)
                                            :else (assoc-in screen [:places place] place-answer)))
                                        screen
                                        (:changed change))
              :else screen))
          nil
          (changes read)))

(defn- leave! "Leaves a live read." [read]
  (read-path/leave (:read-path read) (:leave-with read)))

(defn- wait-until
  "Waits until a function of no argument gives a true thing, or a bound in
  milliseconds passes (15 s when none is given). Gives what it gave last."
  ([f] (wait-until 15000 f))
  ([ms f]
   (let [deadline (+ (System/currentTimeMillis) ms)]
     (loop []
       (or (f)
           (when (< (System/currentTimeMillis) deadline)
             (Thread/sleep 10)
             (recur)))))))

(defn- settled
  "Waits a short while, for a thing that must not come."
  []
  (Thread/sleep 250))

(defn- held
  "The read path's own three maps, read round it from the field of its type:
  {:pushes :counts :reads}. No call of the read path shows them."
  [rp]
  @(.-held ^ReadPath rp))

(defn- counts-of-pushes
  "How many entries the read path's map of pushes holds under each gate,
  table and routing id, counted by the proof from the map's keys: what the
  read path's own map of counts must hold."
  [rp]
  (frequencies (map (juxt :gate :table :routing-id) (keys (:pushes (held rp))))))

(defn- no-entry-without-a-reader?
  "True when each entry of the read path's map of pushes has a reader."
  [rp]
  (every? (comp seq :readers) (vals (:pushes (held rp)))))

(defn- task-of
  "Takes a lane of which a gate has decided an act. Gives the number of the
  task that decides the lane's acts, found by selects of the proof's own,
  round the read path: each task is asked, under a routing id that Rama's
  test namespace gives for it, for the lane's last stamp in the table that
  the lane's gate keeps for a task. Only the deciding task holds one, in a
  streaming lane and in the base. nil when not exactly one task holds it."
  [lane]
  (let [table (get @round (if (= :base lane) :microbatch-task :streaming-task))
        holding (keep-indexed (fn [i k]
                                (when (some? (rama/foreign-select-one
                                              (path/keypath :last-stamp (declaration/id-text lane)) table {:pkey k}))
                                  i))
                              (rtest/gen-hashing-index-keys tasks))]
    (when (= 1 (count holding))
      (first holding))))

;; ----------------------------------------- standing in for a call of Rama's client

(defn- counting
  "Calls f while the proof stands in for Rama's three reading calls and its
  close with functions that count and then call Rama, and for the read
  path's own function that makes the calls of one round with one that notes
  how many it made. Gives {:result :selects :queries :opens :closes :rounds},
  the last a vector of the count of select calls of each round of a read
  asked once. What must hold: nothing else of the proof calls Rama meanwhile."
  [f]
  (let [selects (atom 0) queries (atom 0) opens (atom 0) closes (atom 0) rounds (atom [])
        real-select rama/compiled-foreign-select-async
        real-query rama/foreign-invoke-query-async
        real-open rama/compiled-foreign-proxy-async
        real-close rama/close!
        real-calls sources/calls-for-once]
    (with-redefs [rama/compiled-foreign-select-async (fn [& args] (swap! selects inc) (apply real-select args))
                  rama/foreign-invoke-query-async (fn [& args] (swap! queries inc) (apply real-query args))
                  rama/compiled-foreign-proxy-async (fn [& args] (swap! opens inc) (apply real-open args))
                  rama/close! (fn [x] (swap! closes inc) (real-close x))
                  sources/calls-for-once (fn [missing numbers]
                                           (let [calls (real-calls missing numbers)]
                                             (when (seq calls) (swap! rounds conj (count calls)))
                                             calls))]
      (let [result (f)]
        {:result result :selects @selects :queries @queries :opens @opens :closes @closes :rounds @rounds}))))

(defn- noting-routing-ids
  "Calls f while the proof stands in for Rama's three reading calls with
  functions that note the table or query and the routing id of each call,
  and then call Rama. Takes the read path whose calls are noted: a table or
  query is known by its handle, which is a field of the read path's type.
  Gives {:result :selects :queries :opens}, the last three each a vector of
  [gate table-or-query routing-id] in the order of the calls; a call on a
  handle that is not that read path's, as the door's are, is noted as
  [:another nil routing-id]."
  [^ReadPath rp f]
  (let [selects (atom []) queries (atom []) opens (atom [])
        real-select rama/compiled-foreign-select-async
        real-query rama/foreign-invoke-query-async
        real-open rama/compiled-foreign-proxy-async
        table-of (fn [pstate]
                   (condp identical? pstate
                     (.-streaming-facts rp)                  [:streaming :facts]
                     (.-microbatch-facts rp)                 [:microbatch :facts]
                     (.-streaming-facts-by-value rp)         [:streaming :facts-by-value]
                     (.-microbatch-facts-by-value rp)        [:microbatch :facts-by-value]
                     (.-streaming-permissions-by-holder rp)  [:streaming :permissions-by-holder]
                     (.-microbatch-permissions-by-holder rp) [:microbatch :permissions-by-holder]
                     (.-streaming-line rp)                   [:streaming :line]
                     (.-microbatch-line rp)                  [:microbatch :line]
                     (.-streaming-things rp)                 [:streaming :things]
                     (.-microbatch-things rp)                [:microbatch :things]
                     (.-streaming-task rp)                   [:streaming :task]
                     (.-microbatch-task rp)                  [:microbatch :task]
                     [:another nil]))
        note! (fn [calls pstate options] (swap! calls conj (conj (table-of pstate) (:pkey options))))]
    (with-redefs [rama/compiled-foreign-select-async (fn [the-path pstate & more]
                                                       (note! selects pstate (first more))
                                                       (apply real-select the-path pstate more))
                  rama/foreign-invoke-query-async (fn [q routing-id lane & more]
                                                    (swap! queries conj
                                                           (if (identical? q (.-lane-rights-query rp))
                                                             [(first-act/depot-at-making lane) :lane-rights routing-id]
                                                             [:another nil routing-id]))
                                                    (apply real-query q routing-id lane more))
                  rama/compiled-foreign-proxy-async (fn [the-path pstate & more]
                                                      (note! opens pstate (first more))
                                                      (apply real-open the-path pstate more))]
      (let [result (f)]
        {:result result :selects @selects :queries @queries :opens @opens}))))

(defn- of-the-base
  "Takes calls that `noting-routing-ids` noted. Gives those on the two
  tables of the microbatch gate, in their order."
  [calls]
  (filterv #(= :microbatch (first %)) calls))

(defn- keeping-pushes
  "Calls f while the proof stands in for the opening of a push with a
  function that keeps the options of each opening, and so its callback, and
  the future of its push, and then calls Rama. Gives {:result :opened}, the
  last a vector of {:options :push}, where `:push` is the future that Rama
  gave."
  [f]
  (let [opened (atom [])
        real-open rama/compiled-foreign-proxy-async]
    (with-redefs [rama/compiled-foreign-proxy-async
                  (fn [the-path pstate options]
                    (let [push (real-open the-path pstate options)]
                      (swap! opened conj {:options options :push push})
                      push))]
      (let [result (f)]
        {:result result :opened @opened}))))

;; ------------------------------------------------ a batch that runs a second time

(def ^:private stores-writes
  "`softland.store.gate.decision/writes` as the store defines it."
  decision/writes)

(defonce ^{:private true
           :doc "The rows of facts of each decision that wrote one on the proof's
  second cluster, in the order of the decisions, a second attempt's among
  them: for each a vector of [key row]."}
  attempts (atom []))

(defonce ^{:private true
           :doc "The key of a fact's place at whose decision the proof asks for one
  throw; nil when it asks for none."}
  throw-at (atom nil))

(defonce ^{:private true :doc "How many throws the proof made."} throws-made (atom 0))

(defn noting-writes
  "Stands in for `softland.store.gate.decision/writes` on the proof's second
  cluster. Takes what it takes and gives what it gives, and notes the rows
  of facts that it gave, each [key row]. Then, when the proof asked for a
  throw at a place and this call gave a row for that place, it throws, this
  once: the decision fails after its rows were made, the attempt of its
  batch fails with it, and Rama runs the batch again. The walk of both
  gates calls it, once in each decision. It is public and held by a var,
  and it closes over nothing."
  [& args]
  (let [out (apply stores-writes args)
        fact-rows (into [] (keep (fn [[which k row]] (when (= :fact which) [k row]))) out)
        at @throw-at]
    (when (seq fact-rows)
      (swap! attempts conj fact-rows))
    (when (and at (some #(= at (first %)) fact-rows) (compare-and-set! throw-at at nil))
      (swap! throws-made inc)
      (throw (ex-info "the read path's proof: a forced failure of this attempt of a batch, after the rows of a decision were made" {})))
    out))

(defn- put-the-proofs-function!
  "Puts the proof's function into the var of the store's `writes`. What
  must hold: the store's module is loaded, which this namespace's own
  requires see to, and no cluster of this JVM is launched."
  []
  (alter-var-root #'decision/writes (constantly noting-writes)))

(defn- put-the-stores-function-back!
  "Puts the store's own `writes` back into its var."
  []
  (alter-var-root #'decision/writes (constantly stores-writes)))

;; ---------------------------------------------------------------- the checks

(deftest the-read-path-is-opened
  (let [wrong (read-path/open (:ipc @round) "no.such/Module")
        rp (the-rp)
        text (str "#<read-path " (:module @round) ">")]
    (check "a read path opened on a name under which no module runs: the word, by a real throw of Rama's client, and no read path"
           [:find-the-module "no.such/Module" true]
           [(:could-not wrong) (:module wrong) (string? (:threw wrong))])
    (check "the right name gives a read path and no word; so does the same name with numbers of the read path's given"
           [nil nil nil] [(:could-not rp) (:could-not (:small @round)) (:could-not (:short @round))])
    (check "a read path prints as the words read-path and the module's name, whatever prints it: pr, str, println, a pretty-printer"
           (vec (repeat 4 text))
           [(pr-str rp) (str rp) (str/trim (with-out-str (println rp))) (str/trim (with-out-str (pprint/pprint rp)))])
    (check "a read path that was just opened holds no live read and no push" {:live-reads 0 :pushes 0} (read-path/open-count rp))
    (shown "a read path opened on a wrong module name:" (pr-str (select-keys wrong [:could-not :module :threw]))
           "| a real throw of Rama's client")
    (shown "an opened read path prints as" (pr-str rp) "and holds" (pr-str (read-path/open-count rp)))))

(deftest a-thing-that-is-no-query
  (let [good {:asks [:place :note-41 :text] :asker (person :alice) :lanes [:main]}
        faults [[5 :not-a-query]
                [(assoc good :live true) :part-not-known]
                [(assoc good :asks [:find "text"]) :asks-no-word-known]
                [(assoc good :asks [:place :note-41]) :place-not-named]
                [(dissoc good :asker) :no-one-asker]
                [(assoc good :lanes []) :lanes-not-named-one-way]
                [(assoc good :lanes (mapv #(keyword (str "l" %)) (range 66))) :too-many-lanes]
                [(assoc good :asks [:range [:b] [:a]]) :range-ends-out-of-order]
                [(assoc good :asks [:place :main :softland.store/depot]) :store-key-not-asked-for]
                [(assoc good :asker (perm/long-person 300)) :id-over-the-limit]]
        counted (counting (fn [] (mapv (fn [[thing]]
                                        [(read-path/read-once (the-rp) thing)
                                         (read-path/read-live (the-rp) thing (fn [_] nil))])
                                      faults)))]
    (check "read-once and read-live each give the word :read-the-query for a thing that is no query, with the check's word under :why"
           (mapv (fn [[_ why]] [[:read-the-query why] [:read-the-query why]]) faults)
           (mapv (fn [pair] (mapv (juxt :could-not :why) pair)) (:result counted)))
    (check "no call was made to Rama for any of them, and no live read is open" [0 0 {:live-reads 0 :pushes 0}]
           [(:selects counted) (:opens counted) (read-path/open-count (the-rp))])
    (shown "a thing that is no query:" (count faults) "faults, each handed to read-once and to read-live, give :read-the-query with the check's word,"
           (pr-str (mapv second faults)) "| calls of Rama's client, counted by a function of the proof's in their place: 0")))

(deftest each-word-once-for-each-gate
  (let [_ (make! :main (person :alice))
        _ (make! :base :system)
        main-marked (mark! :main (person :alice) #{:parent})
        base-marked (mark! :base :system #{:parent})
        facts (vec (concat [(fact :note-41 :text "a note")]
                           (for [i (range 1 6)] (fact :doc (keyword (str "k" i)) i))
                           (for [i (range 1 13)] (fact (keyword (format "e-%02d" i)) :size i))
                           (for [i (range 1 4)] (fact (keyword (str "block-" i)) :parent :canvas-1))))
        observed (for [[who lane marked] [[(person :alice) :main main-marked] [:system :base base-marked]]]
                   (let [wrote (put! who lane facts)
                         id (:act-id wrote)
                         base {:asker who :lanes [lane]}
                         place (ask (assoc base :asks [:place :note-41 :text]))
                         entity (ask (assoc base :asks [:entity :doc]))
                         run (ask (assoc base :asks [:range [:e-01] [:e-12]]))
                         points (ask (assoc base :asks [:points-at :parent :canvas-1]))]
                     (check (str "a place of " lane ": the value that the accepted act wrote, the lane, the fact id that the door's act-id and the fact's index make, and the stamp of the door's answer")
                            {:lanes [lane] :left-out []
                             :place {:holds :value :value "a note" :lane lane :fact [id 0] :stamp (:stamp wrote)}}
                            place)
                     (check (str "an entity of " lane ": its five places, each with its value and its fact")
                            (into {} (for [i (range 1 6)]
                                       [[:doc (keyword (str "k" i))]
                                        {:holds :value :value i :lane lane :fact [id i] :stamp (:stamp wrote)}]))
                            (:places entity))
                     (check (str "a range of " lane " across twelve entities: its twelve places")
                            (into {} (for [i (range 1 13)]
                                       [[(keyword (format "e-%02d" i)) :size]
                                        {:holds :value :value i :lane lane :fact [id (+ 5 i)] :stamp (:stamp wrote)}]))
                            (:places run))
                     (check (str "what points at a thing in " lane ": the three places under the marked key")
                            (into {} (for [i (range 1 4)]
                                       [[(keyword (str "block-" i)) :parent]
                                        {:holds :value :value :canvas-1 :lane lane :fact [id (+ 17 i)] :stamp (:stamp wrote)}]))
                            (:places points))
                     (check (str "since which stamp the order of " lane " is level: the stamp of the act that marked the key, in a streaming lane and in the base; and earlier than the facts that are in the order")
                            [[lane] (:stamp marked) true]
                            (let [since (:level-since (first (:orders points)))]
                              [(mapv :lane (:orders points)) since (< since (:stamp wrote))]))
                     [lane (count (:places entity)) (count (:places run)) (count (:places points)) (:stamp wrote)]))]
    (doseq [[lane entity run points stamp] observed]
      (shown "each word of a query, asked once in" lane
             (if (= :base lane) "(the microbatch gate):" "(the streaming gate):")
             "a place gives the value that an accepted act wrote, with its lane, its fact id and the stamp" stamp
             "of the door's answer; an entity gives its" entity "places; a range across entities its" run
             "; what points at a thing its" points "places and the stamp since which the order is level"))))

(deftest the-three-things-that-a-gates-write-leaves-at-a-place
  (let [uuid (UUID/randomUUID)
        long-text (apply str (repeat 60000 "x"))
        values {:spaces "a string with  two spaces" :map {:a 1 :b #{:x "y"} :c [1.5M nil]} :uuid uuid :long long-text}
        _ (put! (person :alice) :main (vec (for [[e v] values] (fact e :text v))))
        _ (put! (person :alice) :main [(fact :gone :text "soon gone")])
        retract (put! (person :alice) :main [{:e :gone :k :text}])
        place (fn [e] (:place (ask {:asks [:place e :text] :asker (person :alice) :lanes [:main]})))
        value-live (live {:asks [:place :map :text] :asker (person :alice) :lanes [:main]})
        value-now (:place (:now value-live))
        _ (leave! value-live)]
    (check "a place with no fact" {:holds :no-fact-given} (place :nobody))
    (check "a place whose current fact is a retract: nothing here, with the lane, the fact and the stamp of the retract"
           {:holds :nothing-here :lane :main :fact [(:act-id retract) 0] :stamp (:stamp retract)} (place :gone))
    (check "a value reads back as its maker gave it: a string with spaces, a map, a UUID, a string of 60,000 characters"
           values (into {} (for [e (keys values)] [e (:value (place e))])))
    (check "each of those places says that it holds a value, asked once; and one of them, kept live, says the same as it does asked once"
           [#{:value} (place :map)]
           [(set (map (comp :holds place) (keys values))) value-now])
    (shown "the three things that a gate's write leaves at a place are told apart: no fact" (pr-str (place :nobody))
           "| a retract" (pr-str (:holds (place :gone))) "| a value as its maker gave it, of four kinds, the longest of 60,000 characters"
           "| not reached here: a row that holds no value of the form, which no gate's write makes; the read path's pure proof holds what a reader is told of one")))

(deftest a-part-of-a-row-is-given-only-when-asked
  (let [place (fn [with] (:place (ask {:asks [:place :note-41 :text] :asker (person :alice) :lanes [:main] :with with})))
        _ (swap! permissions assoc [(person :dora) :dora] (perm/first-permission (person :dora) :dora {:given-by :system}))
        made (let [sent (door/send-act (the-door) (perm/making-act :dora :system (person :dora) :read-path-proof))]
               (door/answer-of (the-door) (:act-id sent) (:digest sent)))
        depot (:place (ask {:asks [:place :dora :softland.store/depot] :asker (person :dora) :lanes [:dora]
                            :stores-own-facts true :with #{:writer :permission}}))]
    (check "the system's making of a personal lane for its person is accepted" :accepted (:answer made))
    (check "a place asked with no part of its row: the place-answer holds neither who wrote it nor the permission"
           #{:holds :value :lane :fact :stamp} (set (keys (place #{}))))
    (check "asked with the writer: who wrote it, and no permission" [(person :alice) false]
           [(:writer (place #{:writer})) (contains? (place #{:writer}) :permission)])
    (check "asked with both: they are the act's who and the id of the permission that it named"
           [(person :alice) (perm/permission-id (perm (person :alice) :main))] ((juxt :writer :permission) (place #{:writer :permission})))
    (check "for the system's making of a personal lane: the system wrote the fact, and the making act named no permission"
           [:system nil] ((juxt :writer :permission) depot))
    (shown "a part of a row is handed over only when the query asks: with no :with the place-answer's parts are" (pr-str (sort (keys (place #{}))))
           "| with :writer it holds" (pr-str (:writer (place #{:writer}))) "| with both, the act's who and its permission"
           "| the depot fact of a personal lane that the system made:" (pr-str (select-keys depot [:writer :permission])))))

(deftest the-stores-own-facts
  (let [places (fn [asks own?] (:places (ask (cond-> {:asks asks :asker (person :alice) :lanes [:main]} own? (assoc :stores-own-facts true)))))
        alice-perm-id (perm/permission-id (perm (person :alice) :main))
        store-key? (fn [[_ k]] (= "softland.store" (namespace k)))
        refused (counting #(ask {:asks [:place :main :softland.store/depot] :asker (person :alice) :lanes [:main]}))
        all (places [:range :first :last] false)
        all-own (places [:range :first :last] true)]
    (check "the entity of a permission's id, asked without the store's own facts: nothing; with them: the permission, and nothing on its holder's entity"
           [{} #{:softland.store/permission} {}]
           [(places [:entity alice-perm-id] false)
            (set (map second (keys (places [:entity alice-perm-id] true))))
            (places [:entity (person :alice)] true)])
    (check "the entity of the lane itself: nothing; with the store's own facts: its depot and its marked keys"
           [{} #{:softland.store/depot :softland.store/marked-keys}]
           [(places [:entity :main] false) (set (map second (keys (places [:entity :main] true))))])
    (check "a range across the whole lane, after values were written: no permission, setting or mark in the answer that did not ask; with the store's own facts the lane's depot, its holder's permission and its marked keys are there, and no fact under any other key of the store"
           [true true #{:softland.store/depot :softland.store/permission :softland.store/marked-keys}]
           [(pos? (count all)) (not-any? store-key? (keys all)) (set (map second (filter store-key? (keys all-own))))])
    (check "a place under one of the store's own keys, asked without them, is turned away with a word, and no call is made"
           [[:read-the-query :store-key-not-asked-for] 0]
           [((juxt :could-not :why) (:result refused)) (:selects refused)])
    (shown "the store's own facts are in no answer that did not ask for them: the whole lane gives" (count all) "places and none under a key of the store;"
           "asked with :stores-own-facts it gives" (count all-own) "| a place under a store key, asked without, gives"
           (pr-str (select-keys (:result refused) [:could-not :why])) "with no call to Rama")))

(deftest through-lanes
  (let [_ (doseq [lane [:n7 :m7 :f7]] (make! lane (person :alice)))
        through (fn [asks lanes] (ask {:asks asks :asker (person :alice) :lanes lanes}))
        from (fn [asks] (ask {:asks asks :asker (person :alice) :from-lane :n7}))
        winner (fn [answer] ((juxt :holds :lane :value) (:place answer)))
        far (put! (person :alice) :f7 [(fact :p7 :text "from the farthest") (fact :q7 :text "f") (fact :r7-b :n "hidden") (fact :r7-c :n "c")])
        only-far (through [:place :p7 :text] [:n7 :m7 :f7])
        _ (put! (person :alice) :m7 [(fact :p7 :text "from the middle") (fact :q7 :text "m") (fact :r7-b :n "b")])
        with-middle (through [:place :p7 :text] [:n7 :m7 :f7])
        retract (do (put! (person :alice) :n7 [(fact :p7 :text "soon gone") (fact :r7-a :n "a")])
                    (put! (person :alice) :n7 [{:e :p7 :k :text}]))
        with-retract (through [:place :p7 :text] [:n7 :m7 :f7])
        run (through [:range [:r7-a] [:r7-c]] [:n7 :m7 :f7])
        _ (list! :n7 (person :alice) [:m7 :f7])
        first-list (from [:place :q7 :text])
        _ (list! :n7 (person :alice) [:f7])
        second-list (from [:place :q7 :text])
        twice (through [:place :q7 :text] [:n7 :m7 :n7 :f7 :m7])]
    (check "a place that only the farthest lane holds comes from there, and the answer says which lane won"
           [[:value :f7 "from the farthest"] [(:act-id far) 0]] [(winner only-far) (:fact (:place only-far))])
    (check "when a nearer lane holds a fact there too, the nearer wins" [:value :m7 "from the middle"] (winner with-middle))
    (check "when the nearest holds a retract, the nearest wins with nothing here"
           [[:nothing-here :n7 nil] [(:act-id retract) 0]] [(winner with-retract) (:fact (:place with-retract))])
    (check "a run that three lanes hold in parts: each place from the nearest lane that holds it"
           {[:r7-a :n] [:n7 "a"] [:r7-b :n] [:m7 "b"] [:r7-c :n] [:f7 "c"]}
           (into {} (map (fn [[p a]] [p [(:lane a) (:value a)]])) (:places run)))
    (check "from a lane: the lane, then the lanes of its own fact, in order; the read uses the newest list"
           [[[:n7 :m7 :f7] [:value :m7 "m"]] [[:n7 :f7] [:value :f7 "f"]]]
           [[(:lanes first-list) (winner first-list)] [(:lanes second-list) (winner second-list)]])
    (check "a list that names a lane twice is read with each lane once, at its first place"
           [[:n7 :m7 :f7] [:value :m7 "m"]] [(:lanes twice) (winner twice)])
    (shown "through three lanes the nearest lane that holds a fact wins: only the farthest holds it" (pr-str (winner only-far))
           "| a nearer one too" (pr-str (winner with-middle)) "| the nearest holds a retract" (pr-str (winner with-retract))
           "| a run in parts gives each place from its nearest lane")
    (shown "from a lane the read takes the lane's own list, and the newest one:" (pr-str (:lanes first-list)) "and, after the list was written again,"
           (pr-str (:lanes second-list)) "| a list that names a lane twice is read as" (pr-str (:lanes twice)))))

(deftest a-lane-that-is-left-out
  (let [_ (give! :n7 (person :alice) (person :bob))
        _ (give! :f7 (person :alice) (person :bob))
        bob (fn [m] (ask (merge {:asker (person :bob)} m)))
        with (bob {:asks [:range [:r7-a] [:r7-c]] :lanes [:n7 :m7 :f7]})
        without (bob {:asks [:range [:r7-a] [:r7-c]] :lanes [:n7 :f7]})
        not-made (bob {:asks [:place :q7 :text] :lanes [:n7 :nowhere :f7]})
        start (bob {:asks [:place :q7 :text] :from-lane :m7})]
    (check "an asker with no permission in the middle lane: the answer names the lane and why"
           [[:n7 :m7 :f7] [{:lane :m7 :why :may-not-see}]] [(:lanes with) (:left-out with)])
    (check "every other part of the answer equals the answer of the same query without that lane; nothing of the lane is given: the place that the middle lane held comes from the farthest"
           [true [:f7 "hidden"]] [(= (:places with) (:places without)) ((juxt :lane :value) (get (:places with) [:r7-b :n]))])
    (check "a lane that is not made is named with another word"
           [[{:lane :nowhere :why :not-made}] [:value :f7 "f"]]
           [(:left-out not-made) ((juxt :holds :lane :value) (:place not-made))])
    (check "a start lane that she may not see: that one lane is left out, its list is not used, and nothing else is given"
           {:lanes [:m7] :left-out [{:lane :m7 :why :may-not-see}] :place {:holds :no-fact-given}} start)
    (shown "a lane that the asker may not see is left out, and the read says so:" (pr-str (:left-out with))
           "| a lane that is not made:" (pr-str (:left-out not-made)) "| a start lane that she may not see gives" (pr-str start))))

(deftest who-asks
  (let [not-signed-in (ask {:asks [:place :note-41 :text] :not-signed-in true :lanes [:main :base]})
        system (ask {:asks [:place :note-41 :text] :asker :system :lanes [:main :base]})
        turned-away (mapv (fn [thing] ((juxt :could-not :why) (ask thing)))
                          [{:asks [:place :note-41 :text] :lanes [:main]}
                           {:asks [:place :note-41 :text] :asker (person :alice) :not-signed-in true :lanes [:main]}
                           {:asks [:place :note-41 :text] :asker (perm/long-person 300) :lanes [:main]}])
        ;; the store's own facts, asked for by a person who is not signed in
        own-in-base (ask {:asks [:place (perm/permission-id (perm :system :base)) :softland.store/permission] :not-signed-in true :lanes [:base]
                          :stores-own-facts true})
        own-in-main (ask {:asks [:place (perm/permission-id (perm (person :alice) :main)) :softland.store/permission] :not-signed-in true :lanes [:main]
                          :stores-own-facts true})]
    (check "a person who is not signed in sees the base, which is everyone, and nothing of a members lane"
           [[{:lane :main :why :may-not-see}] [:value :base "a note"]]
           [(:left-out not-signed-in) ((juxt :holds :lane :value) (:place not-signed-in))])
    (check "what the working answer for the store's own facts gives a person who is not signed in, when she asks for them: in the base, which is everyone, a holder's permission; in a members lane nothing, and the lane is left out"
           [[:value :base (perm/permission-value (perm/first-permission :system :base))] [:no-fact-given [{:lane :main :why :may-not-see}]]]
           [((juxt :holds :lane :value) (:place own-in-base))
            [(:holds (:place own-in-main)) (:left-out own-in-main)]])
    (check "the system is a who like any other: it holds no permission in a person's lane, and sees the base"
           [[{:lane :main :why :may-not-see}] :base] [(:left-out system) (:lane (:place system))])
    (check "a query with no asker, with both, and with an asker of 300 characters is turned away with its word"
           [[:read-the-query :no-one-asker] [:read-the-query :no-one-asker] [:read-the-query :id-over-the-limit]] turned-away)
    (shown "who asks is what the caller says: a person who is not signed in sees the base and no members lane," (pr-str (:left-out not-signed-in))
           "| :system is a who like any other | no asker, both, and an asker of 300 characters:" (pr-str (mapv second turned-away))
           "| asked for the store's own facts, a person who is not signed in is given the system's permission in the base," (pr-str (:value (:place own-in-base)))
           ", and nothing of a permission in a members lane")))

(deftest what-points-at-a-thing-through-lanes
  (let [_ (doseq [lane [:near10 :plain10 :late10 :far10]] (make! lane (person :alice)))
        points (fn [thing lanes] (ask {:asks [:points-at :parent thing] :asker (person :alice) :lanes lanes}))
        at (fn [answer e] (when-let [a (get (:places answer) [e :parent])] ((juxt :lane :stamp) a)))
        far-mark (mark! :far10 (person :alice) #{:parent})
        near-mark (mark! :near10 (person :alice) #{:parent})
        ;; a farther lane points, a nearer one moves, takes away, moves back
        far-7 (put! (person :alice) :far10 [(fact :block-7 :parent :canvas-1)])
        from-far (points :canvas-1 [:near10 :far10])
        moved (put! (person :alice) :near10 [(fact :block-7 :parent :canvas-2)])
        after-move [(points :canvas-1 [:near10 :far10]) (points :canvas-2 [:near10 :far10])]
        _ (put! (person :alice) :near10 [{:e :block-7 :k :parent}])
        after-retract (points :canvas-1 [:near10 :far10])
        back (put! (person :alice) :near10 [(fact :block-7 :parent :canvas-1)])
        after-back (points :canvas-1 [:near10 :far10])
        ;; a nearer lane that does not mark the key
        _ (put! (person :alice) :far10 [(fact :block-8 :parent :canvas-1)])
        _ (put! (person :alice) :plain10 [(fact :block-8 :parent :canvas-2)])
        plain-other (points :canvas-1 [:plain10 :far10])
        plain-same-act (put! (person :alice) :plain10 [(fact :block-8 :parent :canvas-1)])
        plain-same (points :canvas-1 [:plain10 :far10])
        ;; a nearer fact that says the same thing and was written before its lane's mark
        early (put! (person :alice) :late10 [(fact :block-9 :parent :canvas-1)])
        late-mark (mark! :late10 (person :alice) #{:parent})
        _ (put! (person :alice) :far10 [(fact :block-9 :parent :canvas-1)])
        before-mark (points :canvas-1 [:late10 :far10])
        _ (put! (person :alice) :far10 [(fact :block-9 :parent :canvas-3)])
        far-moved-away (points :canvas-1 [:late10 :far10])
        alone (points :canvas-1 [:late10])
        ;; the mark off and on
        _ (mark! :late10 (person :alice) #{})
        off (points :canvas-1 [:late10])
        again-mark (mark! :late10 (person :alice) #{:parent})
        in-new-order (put! (person :alice) :late10 [(fact :block-10 :parent :canvas-1)])
        on-again (points :canvas-1 [:late10])]
    (check "a farther lane points at the thing: the place comes from the farther lane's order"
           [[:far10 (:stamp far-7)] [{:lane :near10 :level-since (:stamp near-mark)} {:lane :far10 :level-since (:stamp far-mark)}]]
           [(at from-far :block-7) (:orders from-far)])
    (check "the nearer lane moves the block: it is hidden from what points at the first thing, and shown, from the nearer lane, for the second"
           [nil [:near10 (:stamp moved)]] [(at (first after-move) :block-7) (at (second after-move) :block-7)])
    (check "the nearer lane takes the block away, with a retract: it stays hidden" nil (at after-retract :block-7))
    (check "the nearer lane moves it back: it is shown from the nearer lane's own order" [:near10 (:stamp back)] (at after-back :block-7))
    (check "a nearer lane that does not mark the key and writes another thing hides the place"
           [nil {:lane :plain10 :not-marked true}] [(at plain-other :block-8) (first (:orders plain-other))])
    (check "when it writes the same thing, the place is shown from the nearer lane's row, with its lane, its fact and its stamp, under not-marked"
           [[:plain10 (:stamp plain-same-act)] [(:act-id plain-same-act) 0] {:lane :plain10 :not-marked true}]
           [(at plain-same :block-8) (:fact (get (:places plain-same) [:block-8 :parent])) (first (:orders plain-same))])
    (check "a nearer fact that says the same thing and was written before its lane's mark: the place comes from the nearer lane's row, whose stamp is not later than the stamp since which that lane's order is level"
           [[:late10 (:stamp early)] true {:lane :late10 :level-since (:stamp late-mark)}]
           [(at before-mark :block-9) (<= (:stamp early) (:stamp late-mark)) (first (:orders before-mark))])
    (check "then the farther lane moves the place away: no order holds it, and it is not in the answer, though the nearer fact still says the thing"
           [nil nil] [(at far-moved-away :block-9) (at alone :block-9)])
    (check "facts that were written before the mark are not given through that lane's own order: level-since is the stamp of the act that marked the key, and the answer holds no place"
           [{} [{:lane :late10 :level-since (:stamp late-mark)}]] [(:places alone) (:orders alone)])
    (check "the mark taken off: the lane says not-marked. Put on again: another stamp, and a fact that is written after it is in the order"
           [[{:lane :late10 :not-marked true}] [{:lane :late10 :level-since (:stamp again-mark)}] true
            {[:block-10 :parent] [(:act-id in-new-order) 0]}]
           [(:orders off) (:orders on-again) (< (:stamp late-mark) (:stamp again-mark))
            (into {} (map (fn [[place a]] [place (:fact a)])) (:places on-again))])
    (shown "what points at a thing through two lanes, asked once: the farther lane's place" (pr-str (at from-far :block-7))
           "is hidden when a nearer lane moves the block or takes it away, and shown from the nearer lane when it moves it back," (pr-str (at after-back :block-7)))
    (shown "a nearer lane that does not mark the key hides the place with another thing, and with the same thing gives the place from its own row,"
           (pr-str (at plain-same :block-8)) "under" (pr-str (first (:orders plain-same)))
           "| a nearer fact from before its lane's mark gives the place with a stamp" (:stamp early) "that is not later than level-since" (:stamp late-mark)
           "| once the farther lane moves away, the place is not in the answer")
    (shown "the mark of a key: level-since is the stamp of the act that marked it," (:stamp late-mark) "; taken off, the lane says :not-marked; put on again it is"
           (:stamp again-mark) "and only facts written after it are in the order")))

(deftest the-calls-of-a-read-asked-once
  (let [_ (list! :n7 (person :alice) [:m7 :f7])
        _ (give! :base :system (person :alice))
        counted (fn [query] (let [c (counting #(ask query))] [(:selects c) (:rounds c) (:queries c) (some? (:could-not (:result c)))]))
        place-3 (counted {:asks [:place :q7 :text] :asker (person :alice) :lanes [:n7 :m7 :f7]})
        from (counted {:asks [:place :q7 :text] :asker (person :alice) :from-lane :n7})
        entity (counted {:asks [:entity :doc] :asker (person :alice) :lanes [:main]})
        base-run (counted {:asks [:range [:e-01] [:e-12]] :asker (person :alice) :lanes [:base]})
        base-run-through (counted {:asks [:range [:e-01] [:e-12]] :asker (person :alice) :lanes [:main :base]})
        points (counted {:asks [:points-at :parent :canvas-1] :asker (person :alice) :lanes [:near10 :plain10 :far10]})
        ;; 300 places, each checked in a nearer lane, in one call of 300 paths
        _ (doseq [lane [:o11n :o11f]] (make! lane (person :alice)))
        _ (mark! :o11f (person :alice) #{:parent})
        blocks (mapv #(keyword (format "w-%03d" %)) (range 300))
        _ (doseq [part (partition-all 150 blocks)]
            (put! (person :alice) :o11f (mapv #(fact % :parent :canvas-9) part)))
        nearer (into {} (mapcat (fn [part]
                                  (let [wrote (put! (person :alice) :o11n (mapv #(fact % :parent :canvas-9) part))]
                                    (map-indexed (fn [i block] [[block :parent] [(:act-id wrote) i]]) part)))
                                (partition-all 150 blocks)))
        many (counting #(ask {:asks [:points-at :parent :canvas-9] :asker (person :alice) :lanes [:o11n :o11f]}))
        ;; the same place asked once while a live read holds a push open on each of its sources
        watcher (live {:asks [:place :q7 :text] :asker (person :alice) :lanes [:n7 :m7 :f7]})
        watcher-holds (read-path/open-count (the-rp))
        beside-a-live-read (counted {:asks [:place :q7 :text] :asker (person :alice) :lanes [:n7 :m7 :f7]})
        same-answer? (= (dissoc (:now watcher) :leave-with) (ask {:asks [:place :q7 :text] :asker (person :alice) :lanes [:n7 :m7 :f7]}))
        _ (leave! watcher)]
    (check "a place through three lanes: three calls of the facts in one round, and three queries of the rights"
           [3 [3] 3 false] place-3)
    (check "the same place asked once while a live read holds a push open on each of its sources: the same three calls in one round and three queries of the rights, and the same answer. A read asked once reads the tables and takes no value of a push"
           [[3 [3] 3 false] 1 true true]
           [beside-a-live-read (:live-reads watcher-holds) (<= 3 (:pushes watcher-holds)) same-answer?])
    (check "that live read has left, and none is open" {:live-reads 0 :pushes 0} (read-path/open-count (the-rp)))
    (check "the same from a lane: three calls of the facts in two rounds, the start lane's call first, and three queries of the rights"
           [3 [1 2] 3 false] from)
    (check "an entity of five keys: one call of the facts, and one query of the rights"
           [1 [1] 1 false] entity)
    (check "a range across entities of the base, from the base itself: one call of the facts in one round, and one query of the rights"
           [1 [1] 1 false] base-run)
    (check "the same run through her own lane and the base: two calls of the facts in one round and two queries of the rights, one for each lane"
           [2 [2] 2 false] base-run-through)
    (check "what points at a thing through three lanes: the own rows, the orders of the two lanes that mark the key, the checks in the two nearer lanes, and the queries of the rights after each"
           [7 [3 2 2] 7 false] points)
    (check "300 places of a farther order, each checked in a nearer lane by one call of 300 paths: each place is given from the nearer lane's own fact, so the values came in the paths' order"
           [300 true [2 1 1]]
           [(count (:places (:result many)))
            (every? (fn [[place fact-id]] (= fact-id (:fact (get (:places (:result many)) place)))) nearer)
            (:rounds many)])
    (shown "the calls of a read asked once, counted by functions of the proof's in the place of Rama's select and of the read path's rounds:"
           "a place through three lanes" (pr-str (second place-3)) "| the same while a live read held" (:pushes watcher-holds) "pushes open on its sources" (pr-str (second beside-a-live-read))
           "| from a lane" (pr-str (second from)) "| an entity" (pr-str (second entity))
           "| a range across entities of the base" (pr-str (second base-run)) "| the same through her own lane" (pr-str (second base-run-through))
           "| what points at a thing through three lanes" (pr-str (second points)))
    (shown "a call of 300 paths gives its values in the paths' order: each of 300 places came with the fact of its own nearer row")))

(deftest a-very-large-run
  (let [small (:small @round)
        _ (make! :sized (person :alice))
        _ (doseq [[e n] [[:r03 3] [:r08 8] [:r12 12]]]
            (put! (person :alice) :sized (mapv #(fact e (keyword (format "k%02d" %)) %) (range n))))
        counted (fn [query] (let [c (counting #(ask small query))]
                              [(:selects c) (:queries c) (count (:places (:result c))) (select-keys (:result c) [:could-not :what :most])]))
        run-3 (counted {:asks [:entity :r03] :asker (person :alice) :lanes [:sized]})
        run-8 (counted {:asks [:entity :r08] :asker (person :alice) :lanes [:sized]})
        run-12 (counted {:asks [:entity :r12] :asker (person :alice) :lanes [:sized]})
        unseen (counting #(ask small {:asks [:entity :r12] :asker (person :bob) :lanes [:sized]}))
        whole-lane (counted {:asks [:range :first :last] :asker (person :alice) :lanes [:sized]})
        _ (doseq [lane [:c12n :c12f]] (make! lane (person :alice)))
        _ (mark! :c12f (person :alice) #{:parent})
        _ (put! (person :alice) :c12f (mapv #(fact (keyword (str "c-" %)) :parent :canvas-1) (range 7)))
        seven (counted {:asks [:points-at :parent :canvas-1] :asker (person :alice) :lanes [:c12n :c12f]})
        live-large (read-path/read-live small {:asks [:entity :r12] :asker (person :alice) :lanes [:sized]} (fn [_] nil))
        ;; a run of 8 places opens, and then grows to 11 under the open read
        grows (live small {:asks [:entity :r08] :asker (person :alice) :lanes [:sized]})
        opened-with (count (:places (:now grows)))
        _ (put! (person :alice) :sized (mapv #(fact :r08 (keyword (format "g%02d" %)) %) (range 3)))
        grew (wait-until #(some :ended (changes grows)))
        after-it-grew [(read-path/open-count small) (leave! grows)]
        unknown {:gate :streaming :table :facts :lane :sized :routing-id :sized :path [:whole-table]}
        no-path (counting (fn [] [(#'read-path/path-of unknown (query/numbers nil) :whole)
                                  (#'read-path/select-async small unknown (#'read-path/path-of unknown (query/numbers nil) :whole))
                                  (#'read-path/path-of {:path [:run ":sized " ":sized!"]} (query/numbers nil) :no-such-step)]))]
    (check "with a first read of 4 places and at most 10 places for one run: a run of 3 places is one select and one query of the rights"
           [1 1 3 {}] run-3)
    (check "a run of 8 places is three selects and two queries of the rights: the first read, the rights, the size by a bounded walk, the run whole, and the rights after"
           [3 2 8 {}] run-8)
    (check "a run of 12 places is two selects and one query of the rights, and then the word, with no place"
           [2 1 0 {:could-not :serve-a-read-that-large :what :places-of-one-run :most 10}] run-12)
    (check "a run of 12 places in a lane that the asker may not see: one select and one query of the rights, no size, no word; the lane is left out"
           [1 1 nil [{:lane :sized :why :may-not-see}]]
           [(:selects unseen) (:queries unseen) (:could-not (:result unseen)) (:left-out (:result unseen))])
    (check "the widest thing that a query can name, a whole lane, ends in the word within two selects and one query of the rights"
           [2 1 0 {:could-not :serve-a-read-that-large :what :places-of-one-run :most 10}] whole-lane)
    (check "a read that needs 7 checks of a nearer lane, where one read may have 6, ends in the word"
           {:could-not :serve-a-read-that-large :what :checks-of-one-read :most 6} (nth seven 3))
    (check "kept live, the same run of 12 places ends in the word and holds no push"
           [{:could-not :serve-a-read-that-large :what :places-of-one-run :most 10} {:live-reads 0 :pushes 0}]
           [(select-keys live-large [:could-not :what :most]) (read-path/open-count small)])
    (check "a run of 8 places is kept live, and then an act takes it to 11, one more than one read may hold: the read ends with the word and why, and holds nothing"
           [8 {:ended {:could-not :keep-the-read-live :because :a-run-grew-too-large :what :places-of-one-run :most 10}}
            [{:live-reads 0 :pushes 0} :not-open]]
           [opened-with (when grew (last (changes grows))) after-it-grew])
    (check "a source of a kind that the read path builds no path for, and a step that it does not know: no path, and no call is made"
           [[nil [:no-path] nil] 0] [(:result no-path) (:selects no-path)])
    (shown "a run is bounded before it is read whole, by the calls of Rama's select: a run of 3 places" (first run-3) "select; of 8 places" (first run-8)
           "selects; of 12 places" (first run-12) "selects and" (pr-str (nth run-12 3)) "| in a lane that she may not see" (:selects unseen) "select and no word")
    (shown "the widest thing that a query can name, a whole lane, ends in a word within" (first whole-lane) "selects; a read that needs 7 checks, where 6 are allowed, gives"
           (pr-str (nth seven 3)) "| a source of a kind that the read path knows no path for makes no call")))

(deftest a-run-across-entities-of-the-base
  (let [query {:asks [:range [:e-01] [:e-12]] :asker (person :alice) :lanes [:base]}
        once (noting-routing-ids (the-rp) #(ask query))
        with-own (ask {:asks [:range [:b] [:base]] :asker (person :alice) :lanes [:base] :stores-own-facts true})
        without-own (ask {:asks [:range [:b] [:base]] :asker (person :alice) :lanes [:base]})
        read (live query)
        counts (read-path/open-count (the-rp))
        held-under (set (map (juxt :gate :table :routing-id) (keys (:pushes (held (the-rp))))))
        wrote (put! :system :base [(fact :e-07 :size 700)])
        got (wait-until 30000 #(seq (later read)))
        _ (settled)
        after (later read)
        _ (leave! read)]
    (check "asked once, from the base itself: the twelve places come whole from one task, in one select of the facts and one query of the rights, both under the base's routing id"
           [12 [[:microbatch :facts :base]] [[:microbatch :lane-rights :base]]]
           [(count (:places (:result once))) (:selects once) (:queries once)])
    (check "kept live: the same answer now, and four pushes, the run, the base's own rows, everyone's permission and everyone's holder run, all under the base's routing id"
           [(:places (:result once)) {:live-reads 1 :pushes 4} #{[:microbatch :facts :base] [:microbatch :permissions-by-holder :base]}]
           [(:places (:now read)) counts held-under])
    (check "one write in the run gives one change, with the place that was written"
           [true [{:changed {[:e-07 :size] {:holds :value :value 700 :lane :base :fact [(:act-id wrote) 0] :stamp (:stamp wrote)}}}]]
           [(some? got) after])
    (check "a run that reaches the base's own entity, asked with the store's own facts: the base's depot row is in the answer; asked without, no row of the store's is"
           [:microbatch true]
           [(:value (get (:places with-own) [:base :softland.store/depot]))
            (not-any? (fn [[_ k]] (= "softland.store" (namespace k))) (keys (:places without-own)))])
    (shown "a range across entities of the base comes whole from one task: asked once from the base itself it gives" (count (:places (:result once)))
           "places by the calls" (pr-str (:selects once)) "; kept live it holds" (:pushes counts) "pushes, the run and the base's own rows, under"
           (pr-str held-under) "; one write in the run gave" (count after) "change | the calls are noted by functions of the proof's in the place of Rama's")))

(deftest the-base-is-read-where-the-askers-lane-lives
  (let [rp (the-rp)
        base-task (task-of :base)
        ;; a lane of alice's on each task of the cluster
        homes (loop [i 0 found (sorted-map)]
                (if (or (= tasks (count found)) (<= 60 i))
                  found
                  (let [lane (keyword (str "home-" i))
                        _ (make! lane (person :alice))
                        task (task-of lane)]
                    (recur (inc i) (if (and task (not (contains? found task))) (assoc found task lane) found)))))
        home-a (some (fn [[task lane]] (when (not= base-task task) lane)) homes)
        ;; a lane of bob's own, on a task that is neither the base's nor that of alice's lane
        home-b (loop [i 0]
                 (let [lane (keyword (str "bobs-" i))]
                   (make! lane (person :bob))
                   (cond (not (contains? #{base-task (task-of home-a)} (task-of lane))) lane
                         (< i 60) (recur (inc i)))))
        the-three-tasks [(task-of home-a) (task-of home-b) base-task]
        through (fn [who home asks] {:asks asks :asker who :lanes [home :base]})
        note-asks [:place :note-41 :text]
        run-asks [:range [:e-01] [:e-12]]
        points-asks [:points-at :parent :canvas-1]
        from-the-base (fn [asks] (ask {:asks asks :asker :system :lanes [:base]}))
        ;; what the base's own task gives, before this group writes into the run or the order
        base-note (:place (from-the-base note-asks))
        base-run (:places (from-the-base run-asks))
        base-order (:places (from-the-base points-asks))
        ;; (i) asked once: the base through a lane on each task
        on-each-task (into (sorted-map)
                           (map (fn [[task home]] [task (noting-routing-ids rp #(ask (through (person :alice) home note-asks)))]))
                           homes)
        bobs-note (noting-routing-ids rp #(ask (through (person :bob) home-b note-asks)))
        ;; (iii) asked once: a range across entities, and the order by value, each whole from one task
        wholes (vec (for [[who home] [[(person :alice) home-a] [(person :bob) home-b]]
                          asks [run-asks points-asks]]
                      (let [noted (noting-routing-ids rp #(ask (through who home asks)))]
                        {:home home :asks asks :places (:places (:result noted))
                         :calls (of-the-base (:selects noted))
                         :queries (of-the-base (:queries noted))})))
        ;; a place of the base that the proof writes under open reads
        watched [:place :watched-33 :text]
        value-through (fn [who home] (get-in (ask (through who home watched)) [:place :value]))
        everywhere? (fn [v] (and (= v (get-in (from-the-base watched) [:place :value]))
                                 (= v (value-through (person :bob) home-b))
                                 (every? #(= v (value-through (person :alice) %)) (vals homes))))
        _ (put! :system :base [(fact :watched-33 :text "v0")])
        v0-everywhere (wait-until 30000 #(everywhere? "v0"))
        ;; (ii) kept live: two askers whose lanes live on two tasks, and one who reads from the base itself
        readers [(live (through (person :alice) home-a watched)) (live (through (person :bob) home-b watched))
                 (live {:asks watched :asker :system :lanes [:base]})]
        nows (mapv #(get-in % [:now :place :value]) readers)
        live-counts (read-path/open-count rp)
        pushes-on-the-row (set (keep (fn [source]
                                       (when (= [:row (rows/place-key :base :watched-33 :text)] (:path source))
                                         (:routing-id source)))
                                     (keys (:pushes (held rp)))))
        seen? (fn [v] (every? #(= v (get-in (screen %) [:place :value])) readers))
        wrote-1 (put! :system :base [(fact :watched-33 :text "v1")])
        got-1 (wait-until 30000 #(seen? "v1"))
        wrote-2 (put! :system :base [(fact :watched-33 :text "v2")])
        got-2 (wait-until 30000 #(seen? "v2"))
        _ (settled)
        told (mapv later readers)
        _ (run! leave! readers)
        ;; (iii) kept live: a range across entities and the order by value, through two homes
        _ (wait-until 30000 #(everywhere? "v2"))
        runs [(live (through (person :alice) home-a run-asks)) (live (through (person :bob) home-b run-asks))]
        orders [(live (through (person :alice) home-a points-asks)) (live (through (person :bob) home-b points-asks))]
        opened-whole [(mapv #(:places (:now %)) runs) (mapv #(:places (:now %)) orders)]
        wrote-both (put! :system :base [(fact :e-05 :size 500) (fact :block-4 :parent :canvas-1)])
        got-both (wait-until 30000 #(every? (comp seq later) (concat runs orders)))
        _ (settled)
        told-runs (mapv later runs)
        told-orders (mapv later orders)
        _ (run! leave! (concat runs orders))
        after-all (read-path/open-count rp)
        ;; straight after the maker's answer: a read asked once through a lane on another task
        straight-after (vec (for [i (range 20)]
                              (let [v (str "straight-" i)]
                                (put! :system :base [(fact :watched-33 :text v)])
                                [(= v (value-through (person :alice) home-a))
                                 (some? (wait-until 30000 #(= v (value-through (person :alice) home-a))))])))
        last-value (str "straight-" 19)
        _ (wait-until 30000 #(everywhere? last-value))
        ;; (iv) the pushes: two askers with one home, and one with another
        _ (give! home-a (person :alice) (person :bob))
        counted (read-path/open (:ipc @round) (:module @round))
        shared (noting-routing-ids
                counted
                (fn []
                  (let [alice-a (live counted (through (person :alice) home-a watched))
                        one (:counts (held counted))
                        bob-a (live counted (through (person :bob) home-a watched))
                        two (:counts (held counted))
                        bob-b (live counted (through (person :bob) home-b watched))
                        three (:counts (held counted))
                        on-the-row (frequencies (keep (fn [source]
                                                        (when (= [:row (rows/place-key :base :watched-33 :text)] (:path source))
                                                          (:routing-id source)))
                                                      (keys (:pushes (held counted)))))
                        wrote (put! :system :base [(fact :watched-33 :text "shared")])
                        got (wait-until 30000 (fn [] (every? #(= "shared" (get-in (screen %) [:place :value])) [alice-a bob-a bob-b])))
                        _ (settled)
                        calls (mapv later [alice-a bob-a bob-b])
                        _ (leave! alice-a)
                        after-alice (:counts (held counted))
                        _ (leave! bob-a)
                        after-bob (:counts (held counted))
                        _ (leave! bob-b)]
                    {:one one :two two :three three :on-the-row on-the-row :wrote wrote :got got :calls calls
                     :after-alice after-alice :after-bob after-bob :at-the-end (held counted)})))
        change-of (fn [place value wrote index]
                    {:changed {place {:holds :value :value value :lane :base :fact [(:act-id wrote) index] :stamp (:stamp wrote)}}})]
    (check "the proof found a lane of alice's on each of the four tasks; and a lane of alice's, a lane of bob's and the base on three different tasks, each by selects of its own"
           [tasks true true 3]
           [(count homes) (some? home-a) (some? home-b) (count (set (remove nil? the-three-tasks)))])
    (check "asked once through a lane on each task, a place of the base is given as it is from the base itself: its value, its lane, its fact and its stamp"
           [true (vec (repeat tasks base-note))]
           [(= [:value "a note" :base] ((juxt :holds :value :lane) base-note)) (mapv (comp :place :result) (vals on-each-task))])
    (check "each of those reads made one call on the base's facts and one query of its rights, both under the routing id of the lane that it starts from, so on that lane's task; none was made under the base's routing id"
           (mapv (fn [home] [[[:microbatch :facts home]] [[:microbatch :lane-rights home]]]) (vals homes))
           (mapv (juxt (comp of-the-base :selects) (comp of-the-base :queries)) (vals on-each-task)))
    (check "bob, through his own lane, is given the same place, by one call on the base's facts and one query of its rights under his lane's routing id"
           [base-note [[:microbatch :facts home-b]] [[:microbatch :lane-rights home-b]]]
           [(:place (:result bobs-note)) (of-the-base (:selects bobs-note)) (of-the-base (:queries bobs-note))])
    (check "a range across entities of the base comes back whole from the one task of each asker's lane: the places that the base's own task gives, by one call on the base's facts and one query of its rights under her lane's routing id"
           (vec (for [home [home-a home-b]] [base-run [[:microbatch :facts home]] [[:microbatch :lane-rights home]]]))
           (mapv (juxt :places :calls :queries) (filter #(= run-asks (:asks %)) wholes)))
    (check "what points at a thing in the base, its order by value, comes back whole from that one task too: the places that the base's own task gives, by one call on the base's facts for its marks, one on its facts by value, and the queries of its rights, all under her lane's routing id"
           (vec (for [home [home-a home-b]]
                  [base-order [[:microbatch :facts home] [:microbatch :facts-by-value home]]
                   [[:microbatch :lane-rights home] [:microbatch :lane-rights home]]]))
           (mapv (juxt :places :calls :queries) (filter #(= points-asks (:asks %)) wholes)))
    (check "those two answers hold places: the twelve of the run and the three under the marked key"
           [12 3] [(count base-run) (count base-order)])
    (check "a fact of the base is given through each lane and from the base itself, within the proof's wait after its maker's answer"
           true (some? v0-everywhere))
    (check "kept live by two askers whose lanes are on two tasks, and by one who reads from the base itself: each has the value now; the three hold 24 pushes, and the base's row has three of them, one under each read's home"
           [["v0" "v0" "v0"] {:live-reads 3 :pushes 24} #{home-a home-b :base}]
           [nows live-counts pushes-on-the-row])
    (check "a new fact of the base is told to each of the three on the task of her read's home: one change for each of two acts, with the fact and the stamp of its act"
           [true true (vec (repeat 3 [(change-of [:watched-33 :text] "v1" wrote-1 0) (change-of [:watched-33 :text] "v2" wrote-2 0)]))]
           [(some? got-1) (some? got-2) told])
    (check "a range across entities of the base and what points at a thing in it, kept live through two lanes on two tasks: each opens with the whole that the base's own task gives"
           [(vec (repeat 2 base-run)) (vec (repeat 2 base-order))]
           [(first opened-whole) (second opened-whole)])
    (check "one act of the base with a fact in the run and a fact under the marked key: each asker of the run is told the one place of the run, and each asker of what points at the thing the one new place, once"
           [true
            (vec (repeat 2 [(change-of [:e-05 :size] 500 wrote-both 0)]))
            (vec (repeat 2 [(change-of [:block-4 :parent] :canvas-1 wrote-both 1)]))]
           [(some? got-both) told-runs told-orders])
    (check "when each of them has left, the read path holds no live read and no push" {:live-reads 0 :pushes 0} after-all)
    (check "20 acts of the base, each followed at once by a read asked once through a lane on another task: each was given within the proof's wait; how many were given at once is shown and not held"
           (vec (repeat 20 true)) (mapv second straight-after))
    (check "one asker through her lane holds 3 pushes on that lane's facts, 3 on its permissions by holder, 3 on the base's facts and 1 on the base's permissions by holder, all counted under her lane's routing id; a second asker with the same home adds his lent permission in that lane and his holder run, sharing hers above it and all four pushes on the base"
           [{[:streaming :facts home-a] 3 [:streaming :permissions-by-holder home-a] 3
             [:microbatch :facts home-a] 3 [:microbatch :permissions-by-holder home-a] 1}
            {[:streaming :facts home-a] 4 [:streaming :permissions-by-holder home-a] 4
             [:microbatch :facts home-a] 3 [:microbatch :permissions-by-holder home-a] 1}]
           [(:one (:result shared)) (:two (:result shared))])
    (check "an asker with another home holds pushes of his own on the base, under his lane's routing id: the base's row has two pushes, one for each home, where three askers look at it"
           [{[:streaming :facts home-a] 4 [:streaming :permissions-by-holder home-a] 4
             [:microbatch :facts home-a] 3 [:microbatch :permissions-by-holder home-a] 1
             [:streaming :facts home-b] 3 [:streaming :permissions-by-holder home-b] 3
             [:microbatch :facts home-b] 3 [:microbatch :permissions-by-holder home-b] 1}
            {home-a 1 home-b 1}
            {[:microbatch :facts home-a] 3 [:microbatch :permissions-by-holder home-a] 1
             [:microbatch :facts home-b] 3 [:microbatch :permissions-by-holder home-b] 1}]
           [(:three (:result shared)) (:on-the-row (:result shared)) (frequencies (of-the-base (:opens shared)))])
    (check "one write to the base's row calls each of the three functions once, with the fact of that act"
           [true (vec (repeat 3 [(change-of [:watched-33 :text] "shared" (:wrote (:result shared)) 0)]))]
           [(some? (:got (:result shared))) (:calls (:result shared))])
    (check "the first asker of a home leaves: the pushes on the base that the other asker of that home holds stay, and so do his pushes in the home, his permission and hers that his is lent from, while her holder run is closed; he leaves: they are gone, and the other home's stay; the last leaves: nothing is held"
           [{[:streaming :facts home-a] 4 [:streaming :permissions-by-holder home-a] 3
             [:microbatch :facts home-a] 3 [:microbatch :permissions-by-holder home-a] 1
             [:streaming :facts home-b] 3 [:streaming :permissions-by-holder home-b] 3
             [:microbatch :facts home-b] 3 [:microbatch :permissions-by-holder home-b] 1}
            {[:streaming :facts home-b] 3 [:streaming :permissions-by-holder home-b] 3
             [:microbatch :facts home-b] 3 [:microbatch :permissions-by-holder home-b] 1}
            {:pushes {} :counts {} :reads {}}]
           [(:after-alice (:result shared)) (:after-bob (:result shared)) (:at-the-end (:result shared))])
    (shown "the base is read where the asker's lane lives: the base's acts are decided on task" base-task
           "; through a lane of alice's on each of the tasks" (pr-str (vec (keys homes))) "a place of the base was given as from the base itself, each by the calls"
           (pr-str (mapv (comp of-the-base :selects) (vals on-each-task))) "on the base's tables | the tasks are found by selects of the proof's own, the calls noted by functions of the proof's in the place of Rama's")
    (shown "a range across entities of the base and what points at a thing in it come back whole from one task: through alice's lane" home-a "on task" (task-of home-a)
           "and bob's lane" home-b "on task" (task-of home-b) "each gave the" (count base-run) "places of the run and the"
           (count base-order) "places of the order, by the calls" (pr-str (mapv :calls wholes)))
    (shown "kept live, a new fact of the base is told on each asker's task: three reads of one row of the base, with the homes" (pr-str pushes-on-the-row)
           ", held" (:pushes live-counts) "pushes, three of them on that row; each of two acts gave each reader one change; one act with a fact in a run and one under a marked key gave each of four readers one change")
    (shown "straight after the maker's answer of an act of the base, a read asked once through a lane on another task gave the new value at once"
           (count (filter first straight-after)) "times of 20, and each time within the proof's wait | Rama's pages say that two tasks show a batch at two instants; this run saw a read that was ahead of the copy"
           (count (remove first straight-after)) "times")
    (shown "the count of pushes on the base is by the read's home: two askers with one home share its pushes," (pr-str (:two (:result shared)))
           "; an asker with another home holds his own," (pr-str (:on-the-row (:result shared))) "on the one row; the openings on the base's tables were"
           (pr-str (frequencies (of-the-base (:opens shared)))))))

(deftest each-word-kept-live
  (let [_ (doseq [lane [:l14 :n14]] (make! lane (person :alice)))
        _ (mark! :l14 (person :alice) #{:parent})
        observed
        (vec (for [[who lane wait] [[(person :alice) :l14 15000] [:system :base 30000]]
                   [word asks inside outside] [[:place [:place :live-1 :text] (fact :live-1 :text "inside") (fact :live-2 :text "outside")]
                                               [:entity [:entity :live-e] (fact :live-e :a "inside") (fact :live-f :a "outside")]
                                               [:range [:range [:live-r1] [:live-r3]] (fact :live-r2 :a "inside") (fact :live-r4 :a "outside")]
                                               [:points-at [:points-at :parent :canvas-14] (fact :live-b :parent :canvas-14) (fact :live-c :parent :canvas-15)]]]
               (let [read (live {:asks asks :asker who :lanes [lane]})
                     now (:now read)
                     wrote (put! who lane [inside])
                     got (wait-until wait #(seq (later read)))
                     _ (put! who lane [outside])
                     again (put! who lane [(assoc inside :v (if (= :points-at word) :canvas-14 "inside again"))])
                     got-again (wait-until wait #(< 1 (count (later read))))
                     _ (settled)
                     all (later read)
                     place [(:e inside) (:k inside)]
                     _ (leave! read)]
                 (check (str lane ", " word ", kept live: the answer now is what a read asked once gives; the first call of the function is that answer")
                        [true true] [(map? now) (= {:anew now} (first (changes read)))])
                 (check (str lane ", " word ": an act inside gives one change, with the place and the fact of that act; an act outside gives none; a second act inside gives the second change")
                        [true true 2 [(:act-id wrote) 0] [(:act-id again) 0]]
                        [(some? got) (some? got-again) (count all)
                         (:fact (get (:changed (first all)) place)) (:fact (get (:changed (second all)) place))])
                 [lane word (count all)])))
        ;; through lanes: an act in a farther lane at a place that a nearer lane holds
        _ (put! (person :alice) :n14 [(fact :under :text "the nearer holds this")])
        _ (put! (person :alice) :l14 [(fact :under :text "f0")])
        asker (live {:asks [:place :under :text] :asker (person :alice) :lanes [:n14 :l14]})
        witness (live {:asks [:place :under :text] :asker (person :alice) :lanes [:l14]})
        _ (put! (person :alice) :l14 [(fact :under :text "f1")])
        _ (put! (person :alice) :l14 [(fact :under :text "f2")])
        seen (wait-until #(= "f2" (get-in (screen witness) [:place :value])))
        under (later asker)
        ;; twenty acts on one place in quick turn
        quick (live {:asks [:place :quick :text] :asker (person :alice) :lanes [:n14 :l14]})
        _ (dotimes [i 20] (put! (person :alice) (if (even? i) :l14 :n14) [(fact :quick :text i)]))
        ended (wait-until #(= 19 (get-in (screen quick) [:place :value])))
        once (ask {:asks [:place :quick :text] :asker (person :alice) :lanes [:n14 :l14]})
        quick-calls (count (later quick))
        _ (run! leave! [asker witness quick])]
    (check "an act in a farther lane at a place that a nearer lane holds gives the asker nothing: the witness, who holds the same push, has the second of two such acts, and the asker's function was not called"
           [true []] [(some? seen) under])
    (check "twenty acts on one place in quick turn, in two lanes by turns: the asker's screen ends at what a read asked once gives; no count of changes is asserted"
           [true (:place once)] [(some? ended) (:place (screen quick))])
    (check "no live read is left open" {:live-reads 0 :pushes 0} (read-path/open-count (the-rp)))
    (shown "each word kept live, for each gate: an act inside gives one change with that act's fact, an act outside gives none, a second act inside the second change:"
           (pr-str (mapv (fn [[lane word n]] [lane word n]) observed)))
    (shown "an act in a farther lane under a nearer lane's fact gave the asker" (count under) "calls, while a witness on the same push got both acts"
           "| twenty acts in quick turn on one place in two lanes: the screen ends at the last value, after" quick-calls "calls")))

(deftest what-shapes-a-read-changes-under-it
  (let [_ (doseq [lane [:m15 :t15 :home15 :x15]] (make! lane (person :alice)))
        _ (give! :t15 (person :alice) (person :bob))
        _ (give! :t15 (person :alice) (person :carol))
        _ (put! (person :alice) :m15 [(fact :note :text "from m15")])
        _ (put! (person :alice) :t15 [(fact :note :text "from t15")])
        place-of (fn [read] ((juxt :lane :value) (:place (screen read))))
        ;; (a) a permission given to an asker whose read left the lane out
        bob (live {:asks [:place :note :text] :asker (person :bob) :lanes [:m15 :t15]})
        bob-now (:now bob)
        _ (put! (person :alice) :m15 [(fact :note :text "bob cannot see this")])
        _ (put! (person :alice) :t15 [(fact :note :text "from t15, again")])
        _ (wait-until #(= "from t15, again" (second (place-of bob))))
        bob-before (changes bob)
        _ (give! :m15 (person :alice) (person :bob) :see)
        _ (wait-until #(= :m15 (first (place-of bob))))
        bob-joined (last (changes bob))
        ;; (b) a members lane goes to everyone, for an asker with no permission
        carol (live {:asks [:place :note :text] :asker (person :carol) :lanes [:m15 :t15]})
        carol-now (:now carol)
        _ (setting! :m15 (person :alice) :everyone)
        _ (wait-until #(= :m15 (first (place-of carol))))
        carol-joined (last (changes carol))
        ;; ... and back to members: the lane is no longer rightful for her
        _ (setting! :m15 (person :alice) :members)
        _ (wait-until #(seq (:left-out (screen carol))))
        carol-left (last (changes carol))
        carol-count (count (changes carol))
        _ (put! (person :alice) :m15 [(fact :note :text "after carol left")])
        _ (wait-until #(= "after carol left" (second (place-of bob))))
        _ (settled)
        carol-after (count (changes carol))
        ;; (c) the start lane's list written again
        _ (list! :home15 (person :alice) [:t15])
        home (live {:asks [:place :note :text] :asker (person :alice) :from-lane :home15})
        _ (list! :home15 (person :alice) [:m15 :t15])
        _ (wait-until #(= [:home15 :m15 :t15] (:lanes (screen home))))
        home-wider (last (changes home))
        _ (list! :home15 (person :alice) [])
        _ (wait-until #(= [:home15] (:lanes (screen home))))
        home-alone (last (changes home))
        ;; (d) a key marked, a fact under it, the mark off, the mark on
        _ (put! (person :alice) :x15 [(fact :comment-1 :on :paper-9)])
        marks (live {:asks [:points-at :on :paper-9] :asker (person :alice) :lanes [:x15]})
        marks-now (:now marks)
        marked (mark! :x15 (person :alice) #{:on})
        _ (wait-until #(:level-since (first (:orders (screen marks)))))
        after-mark (last (changes marks))
        comment (put! (person :alice) :x15 [(fact :comment-3 :on :paper-9)])
        _ (wait-until #(get (:places (screen marks)) [:comment-3 :on]))
        after-comment (last (changes marks))
        _ (mark! :x15 (person :alice) #{})
        _ (wait-until #(:not-marked (first (:orders (screen marks)))))
        after-off (last (changes marks))
        marked-again (mark! :x15 (person :alice) #{:on})
        _ (wait-until #(= (:stamp marked-again) (:level-since (first (:orders (screen marks))))))
        after-on (last (changes marks))
        ;; (e) for an asker who may not see a lane of her list: its list written again, and the asked key marked in it
        dave (live {:asks [:points-at :on :paper-9] :asker (person :dave) :lanes [:x15 :t15]})
        _ (setting! :t15 (person :alice) :everyone)
        _ (wait-until #(= [{:lane :x15 :why :may-not-see}] (:left-out (screen dave))))
        dave-count (count (changes dave))
        _ (list! :x15 (person :alice) [:t15])
        _ (mark! :x15 (person :alice) #{})
        last-mark (mark! :x15 (person :alice) #{:on})
        _ (wait-until #(= (:stamp last-mark) (:level-since (first (:orders (screen marks))))))
        _ (settled)
        dave-after (count (changes dave))
        _ (run! leave! [bob carol home marks dave])
        ;; (f) through three lanes, a permission given in the nearest while the farther orders hold places
        _ (doseq [lane [:j15n :j15m :j15f]] (make! lane (person :alice)))
        _ (doseq [lane [:j15n :j15m :j15f]] (mark! lane (person :alice) #{:parent}))
        _ (give! :j15m (person :alice) (person :erin))
        _ (give! :j15f (person :alice) (person :erin))
        _ (put! (person :alice) :j15f [(fact :f-1 :parent :canvas-1) (fact :f-2 :parent :canvas-1)])
        _ (put! (person :alice) :j15m [(fact :m-1 :parent :canvas-1)])
        _ (put! (person :alice) :j15n [(fact :n-1 :parent :canvas-1) (fact :f-2 :parent :canvas-2)])
        erin (live {:asks [:points-at :parent :canvas-1] :asker (person :erin) :lanes [:j15n :j15m :j15f]})
        erin-now (:now erin)
        _ (give! :j15n (person :alice) (person :erin))
        _ (wait-until #(get (:places (screen erin)) [:n-1 :parent]))
        _ (settled)
        erin-later (later erin)
        erin-once (ask {:asks [:points-at :parent :canvas-1] :asker (person :erin) :lanes [:j15n :j15m :j15f]})
        _ (leave! erin)
        ;; (g) the base: its setting to members and back
        _ (put! :system :base [(fact :note-15 :text "in the base")])
        frank (live {:asks [:place :note-15 :text] :asker (person :frank) :lanes [:base]})
        alice-in-base (live {:asks [:place :note-15 :text] :asker (person :alice) :lanes [:base]})
        _ (setting! :base :system :members)
        _ (wait-until 30000 #(seq (:left-out (screen frank))))
        frank-out (last (changes frank))
        _ (wait-until 30000 #(seq (later alice-in-base)))
        alice-same (later alice-in-base)
        frank-count (count (changes frank))
        base-wrote (put! :system :base [(fact :note-15 :text "frank cannot see this")])
        _ (wait-until 30000 #(= "frank cannot see this" (get-in (screen alice-in-base) [:place :value])))
        _ (settled)
        frank-after (count (changes frank))
        _ (setting! :base :system :everyone)
        _ (wait-until 30000 #(= "frank cannot see this" (get-in (screen frank) [:place :value])))
        frank-back (last (changes frank))
        _ (run! leave! [frank alice-in-base])]
    (check "an asker with no permission in the nearer lane: the lane is left out, a write in it gives her nothing, and the place comes from the farther lane"
           [[{:lane :m15 :why :may-not-see}] [:t15 "from t15"] true]
           [(:left-out bob-now) ((juxt :lane :value) (:place bob-now))
            (every? #(not= :m15 (:lane (first (vals (:changed %))))) (rest bob-before))])
    (check "a permission that says see is given to her under her open read: the whole answer comes anew, the lane joins, and the place comes from it"
           [[:m15 :t15] [] [:m15 "bob cannot see this"]]
           [(:lanes (:anew bob-joined)) (:left-out (:anew bob-joined)) ((juxt :lane :value) (:place (:anew bob-joined)))])
    (check "a members lane goes to everyone under the open read of an asker with no permission: the lane joins, and the whole answer comes anew"
           [[{:lane :m15 :why :may-not-see}] [] :m15]
           [(:left-out carol-now) (:left-out (:anew carol-joined)) (:lane (:place (:anew carol-joined)))])
    (check "the lane goes back to members: she is told a whole answer that says that the lane is left out, with what now wins the place, and nothing of the lane's facts"
           {:lanes [:m15 :t15] :left-out [{:lane :m15 :why :may-not-see}]
            :place (:place (ask {:asks [:place :note :text] :asker (person :carol) :lanes [:t15]}))}
           (:anew carol-left))
    (check "after the lane left, a write in it gives that asker nothing, and the other asker of the same push her change"
           [carol-count "after carol left"] [carol-after (second (place-of bob))])
    (check "the start lane's list is written again under an open read: lanes join and leave by the new list, each time with the whole answer anew"
           [[[:home15 :m15 :t15] :m15] [[:home15] {:holds :no-fact-given}]]
           [[(:lanes (:anew home-wider)) (:lane (:place (:anew home-wider)))]
            [(:lanes (:anew home-alone)) (:place (:anew home-alone))]])
    (check "a key is marked under an open read of what points at a thing: before it the lane says not-marked; after it the whole answer comes anew with the stamp of the mark and no place, since the fact from before the mark is not in the order"
           [[{:lane :x15 :not-marked true}] {:lanes [:x15] :left-out [] :places {} :orders [{:lane :x15 :level-since (:stamp marked)}]}]
           [(:orders marks-now) (:anew after-mark)])
    (check "a fact under the marked key gives one change; the mark taken off gives the answer anew with not-marked; put on again, another stamp, and the fact of before is not in the new order"
           [{[:comment-3 :on] {:holds :value :value :paper-9 :lane :x15 :fact [(:act-id comment) 0] :stamp (:stamp comment)}}
            [[{:lane :x15 :not-marked true}] {}]
            [[{:lane :x15 :level-since (:stamp marked-again)}] {}]]
           [(:changed after-comment)
            [(:orders (:anew after-off)) (:places (:anew after-off))]
            [(:orders (:anew after-on)) (:places (:anew after-on))]])
    (check "for an asker who may not see a lane of her list, that lane's list written again, the asked key's mark taken off and the key marked again give her nothing: the other asker, who holds the same push, was told the last of them"
           dave-count dave-after)
    (check "through three lanes, a permission is given in the nearest while the farther orders hold places: the lane that joins gives a whole answer anew with its own place in it, and the place that it moved is hidden; the screen is what a read asked once gives"
           [#{[:f-1 :parent] [:f-2 :parent] [:m-1 :parent]} true (:places erin-once) #{[:f-1 :parent] [:m-1 :parent] [:n-1 :parent]}]
           [(set (keys (:places erin-now))) (every? :anew erin-later) (:places (screen erin)) (set (keys (:places erin-once)))])
    (check "the base is closed to its members under the open read of an asker who holds no permission there: the base is left out, and she is given no fact"
           {:lanes [:base] :left-out [{:lane :base :why :may-not-see}] :place {:holds :no-fact-given}} (:anew frank-out))
    (check "an asker who holds a permission in the base is given an answer anew that says what it said"
           [true true] [(every? :anew alice-same) (= (:now alice-in-base) (:anew (last alice-same)))])
    (check "while the base is members, a write in it gives the asker who may not see it nothing; back to everyone, the whole answer comes anew with that fact"
           [frank-count [:value "frank cannot see this" [(:act-id base-wrote) 0]]]
           [frank-after ((juxt :holds :value :fact) (:place (:anew frank-back)))])
    (check "no live read is left open" {:live-reads 0 :pushes 0} (read-path/open-count (the-rp)))
    (shown "what shapes a read changes under it, each as a whole answer anew: a permission given" (pr-str (select-keys (:anew bob-joined) [:lanes :left-out]))
           "| a members lane to everyone, and back:" (pr-str (:left-out (:anew carol-left))) "| after the lane left, a write in it gave that asker"
           (- carol-after carol-count) "calls")
    (shown "the start lane's list written again:" (pr-str (:lanes (:anew home-wider))) "then" (pr-str (:lanes (:anew home-alone)))
           "| a key marked:" (pr-str (:orders (:anew after-mark))) "| a fact under it: one change | the mark off:" (pr-str (:orders (:anew after-off)))
           "| on again:" (pr-str (:orders (:anew after-on))))
    (shown "a list and a mark that are written in a lane that she may not see gave her" (- dave-after dave-count)
           "calls | a lane that joins over farther orders gave" (count erin-later) "whole answers anew, the last with its own place and what a read asked once gives"
           "| the base to members and back:" (pr-str (:left-out (:anew frank-out))) "then the fact again")))

(deftest a-permission-ends-under-a-live-read
  (let [_ (make! :m-ends (person :ada))
        _ (make! :t-ends (person :ada))
        _ (setting! :t-ends (person :ada) :everyone)
        _ (give! :m-ends (person :ada) (person :bea))
        _ (give! :m-ends (person :bea) (person :cal))
        _ (give! :m-ends (person :ada) (person :dov))
        _ (put! (person :ada) :t-ends [(fact :note :text "from t16")])
        _ (put! (person :ada) :m-ends [(fact :note :text "from m16")])
        query (fn [who] {:asks [:place :note :text] :asker who :lanes [:m-ends :t-ends]})
        place-of (fn [read] ((juxt :lane :value) (:place (screen read))))
        end! (fn [ender holder] (put! ender :m-ends [(perm/end-fact (perm holder :m-ends))]))
        from-t16 (:place (ask {:asks [:place :note :text] :asker (person :cal) :lanes [:t-ends]}))
        ;; Cal's permission is beneath Bea's, and Bea's beneath the first holder's. Dov's is beside Bea's.
        cal (live (query (person :cal)))
        dov (live (query (person :dov)))
        cal-now (:now cal)
        ;; the permission above Cal's ends
        _ (end! (person :ada) (person :bea))
        _ (wait-until #(seq (:left-out (screen cal))))
        cal-left (last (changes cal))
        cal-count (count (changes cal))
        once-after-the-end (ask (query (person :cal)))
        _ (put! (person :ada) :m-ends [(fact :note :text "after the end")])
        _ (wait-until #(= "after the end" (second (place-of dov))))
        _ (settled)
        dov-after-a-write (place-of dov)
        cal-after-a-write (count (changes cal))
        ;; Bea is given hers again: Cal's does not come back with it
        _ (give! :m-ends (person :ada) (person :bea))
        _ (put! (person :ada) :m-ends [(fact :note :text "after bea's second give")])
        _ (wait-until #(= "after bea's second give" (second (place-of dov))))
        _ (settled)
        cal-after-beas-give (count (changes cal))
        once-after-beas-give (ask (query (person :cal)))
        ;; Cal is given hers again, by an act of its own
        _ (give! :m-ends (person :bea) (person :cal))
        _ (wait-until #(= :m-ends (first (place-of cal))))
        cal-back (last (changes cal))
        ;; her own permission ends
        _ (end! (person :bea) (person :cal))
        _ (wait-until #(seq (:left-out (screen cal))))
        cal-out-again (last (changes cal))
        dov-at-the-end (place-of dov)
        _ (run! leave! [cal dov])
        left-out [{:lane :m-ends :why :may-not-see}]]
    (check "an asker whose permission is two beneath the lane's first opens a live read of a place through that lane, which is open to its members: the lane is in, and its fact wins"
           [[] [:m-ends "from m16"]] [(:left-out cal-now) ((juxt :lane :value) (:place cal-now))])
    (check "the permission that hers is lent from ends under her open read: she is told a whole answer that says that the lane is left out, with what now wins the place and nothing of the lane's facts; a read asked once gives the same"
           [{:lanes [:m-ends :t-ends] :left-out left-out :place from-t16} [left-out from-t16]]
           [(:anew cal-left) ((juxt :left-out :place) once-after-the-end)])
    (check "after her read left the lane, a write in it gives her nothing; an asker whose permission is beside the one that ended, and still stands, is told the write"
           [cal-count [:m-ends "after the end"]] [cal-after-a-write dov-after-a-write])
    (check "the holder above her is given a permission again, on a new id: hers does not come back with it. Her read is given nothing, and a read asked once still leaves the lane out"
           [cal-count left-out] [cal-after-beas-give (:left-out once-after-beas-give)])
    (check "she is given hers again by an act of its own: the lane joins her open read, and the whole answer comes anew with the lane's fact"
           [[] [:m-ends "after bea's second give"]]
           [(:left-out (:anew cal-back)) ((juxt :lane :value) (:place (:anew cal-back)))])
    (check "her own permission ends under her open read: she is told that the lane is left out; the other asker's read gives the lane's fact still"
           [left-out [:m-ends "after bea's second give"]] [(:left-out (:anew cal-out-again)) dov-at-the-end])
    (shown "a permission ends under a live read: the end of the one above the asker's gave her" (pr-str (:left-out (:anew cal-left)))
           "| a write in the lane after it gave her" (- cal-after-a-write cal-count) "calls | the second give of the one above gave her"
           (- cal-after-beas-give cal-count) "calls | her own second give gave her the lane's fact again:"
           (pr-str ((juxt :lane :value) (:place (:anew cal-back)))))))

(deftest a-read-asked-in-a-run
  (let [_ (make! :m-run (person :ada))
        _ (make! :t-run (person :ada))
        _ (setting! :t-run (person :ada) :everyone)
        _ (put! (person :ada) :t-run [(fact :note :text "from t-run")])
        _ (put! (person :ada) :m-run [(fact :note :text "from m-run")])
        for-run (fn [who session]
                  (perm/run-permission who :m-run (perm (person :ada) :m-run) session :see {:salt (swap! give-salt inc)}))
        give-run! (fn [p] (put! (person :ada) :m-run [(perm/permission-fact p)]))
        run-9 (for-run (person :cal) :run-9)
        _ (give-run! run-9)
        query (fn [who session] (cond-> {:asks [:place :note :text] :asker who :lanes [:m-run :t-run]}
                                  session (assoc :session session)))
        seen (fn [answer] [(:left-out answer) ((juxt :lane :value) (:place answer))])
        left-out [{:lane :m-run :why :may-not-see}]
        once (mapv (fn [[who session]] (seen (ask (query who session))))
                   [[(person :cal) :run-9] [(person :cal) nil] [(person :cal) :run-8] [(person :dov) :run-9]])
        place-of (fn [read] ((juxt :lane :value) (:place (screen read))))
        cal (live (query (person :cal) :run-9))
        cal-now (:now cal)
        _ (put! (person :ada) :m-run [(perm/end-fact run-9)])
        _ (wait-until #(seq (:left-out (screen cal))))
        cal-left (last (changes cal))
        _ (give-run! (for-run (person :cal) :run-9))
        _ (wait-until #(= :m-run (first (place-of cal))))
        cal-back (last (changes cal))
        _ (leave! cal)]
    (check "a read asked once in a run, through a lane open to its members: the asker sees the lane under the permission of the run that she asks in; not when she names no run, or another run; and another who that asks in that run is given nothing by it"
           [[[] [:m-run "from m-run"]] [left-out [:t-run "from t-run"]] [left-out [:t-run "from t-run"]] [left-out [:t-run "from t-run"]]]
           once)
    (check "a live read asked in a run: the lane is in; the run's permission ends, and she is told a whole answer that leaves the lane out; the run is given one again, and the lane joins with its fact"
           [[[] [:m-run "from m-run"]] [left-out [:t-run "from t-run"]] [[] [:m-run "from m-run"]]]
           [(seen cal-now) (seen (:anew cal-left)) (seen (:anew cal-back))])
    (shown "a read asked in a run: once, in the run, in no run, in another run, and by another who in that run:" (pr-str (mapv first once))
           "| live, at the end of the run's permission:" (pr-str (:left-out (:anew cal-left))))))

(deftest the-reviews-cluster-probe-written-again
  (let [perm-key declaration/permission-key
        send! (fn [an-act act-id]
                (let [sent (if act-id (door/send-act (the-door) an-act act-id) (door/send-act (the-door) an-act))]
                  {:sent sent
                   :answer (when-not (:could-not sent) (door/answer-of (the-door) (:act-id sent) (:digest sent)))}))
        word (fn [{:keys [answer]}] (if (= :refused (:answer answer)) (:reason answer) (:answer answer)))
        ;; receipt 1: a yes ended, a try while it is ended, and a yes given again
        _ (make! :c-lab (person :asha))
        root (perm (person :asha) :c-lab)
        x {:version 1 :who (person :ben) :lane :c-lab :session :run-1 :facts [(fact :exp-1 :dose "5 mg")]}
        hex-x (digest/hex-for-a-yes x)
        [u1 u2] (repeatedly 2 #(UUID/randomUUID))
        pa1 (perm/act-permission (person :ben) :c-lab root u1 hex-x)
        _ (put! (person :asha) :c-lab [(perm/permission-fact pa1)])
        _ (put! (person :asha) :c-lab [(perm/end-fact pa1)])
        attempt-1 (send! (assoc x :permission (perm/permission-id pa1)) nil)
        _ (give! :c-lab (person :asha) (person :cal) :see-and-act)
        cal-perm (perm (person :cal) :c-lab)
        cals (fn [facts] {:version 1 :who (person :cal) :lane :c-lab :permission (perm/permission-id cal-perm) :session :s0 :facts facts})
        again-at-u1 (send! (cals [(perm/permission-fact (perm/act-permission (person :ben) :c-lab cal-perm u1 hex-x))]) nil)
        pa2 (perm/act-permission (person :ben) :c-lab cal-perm u2 hex-x)
        re-grant (send! (cals [(perm/permission-fact pa2)]) nil)
        attempt-2 (send! (assoc x :permission (perm/permission-id pa2)) nil)
        exp-read (ask {:asks [:place :exp-1 :dose] :asker (person :asha) :lanes [:c-lab]})
        ;; receipt 2: a holder who may only see finds the yes's id, and sends under an act-id that holds it
        _ (make! :c-studio (person :ida))
        ida (perm (person :ida) :c-studio)
        _ (give! :c-studio (person :ida) (person :cal) :see)
        cal-see (perm (person :cal) :c-studio)
        joes {:version 1 :who (person :joe) :lane :c-studio :session :run-1 :facts [(fact :secret :dose "5 mg")]}
        u (UUID/randomUUID)
        pa (perm/act-permission (person :joe) :c-studio ida u (digest/hex-for-a-yes joes))
        _ (put! (person :ida) :c-studio [(perm/permission-fact pa)])
        cal-saw (ask {:asks [:range :first :last] :asker (person :cal) :lanes [:c-studio] :stores-own-facts true})
        found (first (keep (fn [[[e k] a]]
                             (when (and (= perm-key k) (= :exact-act (first (:covers (:value a))))) e))
                           (:places cal-saw)))
        cal-under-it (send! {:version 1 :who (person :cal) :lane :c-studio :permission (perm/permission-id cal-see) :session :s-cal
                             :facts [(fact :x :y 1)]}
                            [:c-studio found])
        joe-res (send! (assoc joes :permission (perm/permission-id pa)) nil)
        secret (ask {:asks [:place :secret :dose] :asker (person :ida) :lanes [:c-studio]})
        ;; receipt 3: a live read in a sitting, of a lane that she may not see, while another holder's
        ;; permission for that sitting is given and ended; a witness holds that permission's place
        _ (make! :c-near (person :root))
        _ (give! :c-near (person :root) (person :bea))
        _ (put! (person :root) :c-near [(fact :p :text "secret")])
        _ (make! :c-far (person :root))
        _ (setting! :c-far (person :root) :everyone)
        _ (put! (person :root) :c-far [(fact :p :text "public")])
        ann (live {:asks [:place :p :text] :asker (person :ann) :session :run-7 :lanes [:c-near :c-far]})
        dovs (perm/run-permission (person :dov) :c-near (perm (person :bea) :c-near) :run-7 :see {:salt (swap! give-salt inc)})
        witness (live {:asks [:place (perm/permission-id dovs) perm-key] :asker (person :root) :lanes [:c-near] :stores-own-facts true})
        _ (put! (person :bea) :c-near [(perm/permission-fact dovs)])
        _ (wait-until #(= :value (:holds (:place (screen witness)))))
        bea-or-dov-held? (let [pushes (:pushes (held (the-rp)))
                               bea-row (sources/permission-row-source
                                        :c-near :c-near
                                        (rows/place-key :c-near (perm/permission-id (perm (person :bea) :c-near)) perm-key))
                               [d-from d-to] (rows/by-holder-run :c-near (person :dov) :run-7)
                               dov-holders (sources/holders-source :c-near :c-near d-from d-to)]
                           (or (contains? pushes bea-row) (contains? pushes dov-holders)))
        _ (put! (person :bea) :c-near [(perm/end-fact dovs)])
        _ (wait-until #(= :nothing-here (:holds (:place (screen witness)))))
        _ (settled)
        ann-changes (changes ann)
        _ (leave! ann)
        _ (leave! witness)
        ;; receipt 4: the longest live read, with one write while it opens
        runs-of (fn [lane]
                  (perm/permission-fact
                   (perm/run-permission (person :cal) lane (perm (person :root) lane) :run-7 :see {:salt (swap! give-salt inc)})))
        _ (make! :c-start (person :root))
        _ (put! (person :root) :c-start [(fact :c-start :softland.store/builds-on [:c-mid :c-far2]) (runs-of :c-start)])
        _ (make! :c-mid (person :root))
        _ (put! (person :root) :c-mid [(runs-of :c-mid)])
        _ (make! :c-far2 (person :root))
        _ (put! (person :root) :c-far2 [(fact :c-far2 :softland.store/marked-keys #{:parent}) (runs-of :c-far2)])
        _ (put! (person :root) :c-far2 [(fact :item-1 :parent :canvas-1)])
        open-push @#'softland.store.read-path/open-push
        wrote? (atom false)
        opened (with-redefs [softland.store.read-path/open-push
                             (fn [rp source called]
                               (let [fut (open-push rp source called)]
                                 (when (and (= :c-start (:lane source))
                                            (= [:row (rows/place-key :c-start :item-1 :parent)] (:path source))
                                            (compare-and-set! wrote? false true))
                                   (put! (person :root) :c-far2 [(fact :item-2 :parent :canvas-1)])
                                   (Thread/sleep 100))
                                 fut))]
                 (read-path/read-live (the-rp) {:asks [:points-at :parent :canvas-1] :asker (person :cal) :session :run-7
                                                :from-lane :c-start}
                                      (fn [_])))
        _ (when (:leave-with opened) (read-path/leave (the-rp) (:leave-with opened)))]
    (check "the review's receipt 1, written again for a yes on an id of its own: the try under the ended yes is refused; Cal's give at the ended yes's id is refused, a yes's id taking one give; his yes on a new id is taken, and Ben's act under it, whose fact is in the store"
           [:permission-ended :a-permission-id-takes-one-permission :accepted :accepted [:value "5 mg"]]
           [(word attempt-1) (word again-at-u1) (word re-grant) (word attempt-2) ((juxt :holds :value) (:place exp-read))])
    (check "the review's receipt 2, written again: a holder who may only see reads the yes's id among the store's own facts and sends under an act-id that holds it, and is refused; the holder's act under the yes, under an act-id of the door's, is taken and its fact is in the store"
           [u :permission-does-not-allow-writing :accepted [:value "5 mg"]]
           [found (word cal-under-it) (word joe-res) ((juxt :holds :value) (:place secret))])
    (check "the review's receipt 3, written again: a live read in a sitting, of a lane that she may not see, is handed nothing while another holder's permission for that sitting is given and ended, as a witness of that permission's place was told both; and the read path holds no push of that holder's run or of the permission above it"
           [1 false]
           [(count ann-changes) bea-or-dov-held?])
    (check "the review's receipt 4: the longest live read, from a start lane through two more in a sitting, with one write while it opens, opens with an answer, within the rounds that it is given"
           [true true]
           [(some? (:now opened)) @wrote?])
    (shown "the review's cluster probe, written again: receipt 1" (pr-str (mapv word [attempt-1 again-at-u1 re-grant attempt-2]))
           "| receipt 2" (pr-str [(= u found) (word cal-under-it) (word joe-res)])
           "| receipt 3, the calls of her function:" (count ann-changes)
           "| receipt 4:" (if (:now opened) "opened" (pr-str opened)))))

(deftest the-second-reviews-cluster-probe-written-again
  (let [perm-key declaration/permission-key
        word (fn [{:keys [answer]}] (if (= :refused (:answer answer)) (:reason answer) (:answer answer)))
        other-rp (read-path/open (:ipc @round) (:module @round))
        ;; the review's P1, on a lane open to its members: Ann's lent chain through Bea ended above,
        ;; a fact written after that, Bea given a new permission on a new id, and one act of Bea's
        ;; that tries to lend Ann (or Ann's sitting) a permission beneath her new one and end her own
        stale-chain!
        (fn [lane run?]
          (let [_ (make! lane (person :root))
                _ (give! lane (person :root) (person :dave))
                _ (give! lane (person :dave) (person :bea))
                given-perm (fn [] (if run?
                                    (perm/run-permission (person :ann) lane (perm (person :bea) lane) :run-7 :see
                                                         {:salt (swap! give-salt inc)})
                                    (perm/given-permission (person :ann) lane (perm (person :bea) lane)
                                                           {:allows :see :salt (swap! give-salt inc)})))
                _ (put! (person :bea) lane [(perm/permission-fact (given-perm))])
                _ (put! (person :root) lane [(perm/end-fact (perm (person :dave) lane))])
                _ (put! (person :root) lane [(fact :rx :dose "the secret")])
                query (cond-> {:asks [:place :rx :dose] :asker (person :ann) :lanes [lane :c-open]} run? (assoc :session :run-7))
                root-read (live {:asks [:place :rx :dose] :asker (person :root) :lanes [lane]})
                ann (live query)
                ann-alone (live other-rp query)
                _ (give! lane (person :root) (person :bea))
                second-perm (given-perm)
                second-fact (perm/permission-fact second-perm)
                the-act (act! (person :bea) lane [second-fact (perm/end-fact (perm (person :bea) lane))])
                _ (put! (person :root) lane [(fact :rx :dose "the secret, still")])
                _ (wait-until #(= "the secret, still" (get-in (screen root-read) [:place :value])))
                _ (settled)
                told [(changes ann) (changes ann-alone)]
                once (ask query)]
            (run! leave! [root-read ann ann-alone])
            {:word (word the-act)
             :calls (mapv count told)
             :secret? (boolean (some #(contains? #{"the secret" "the secret, still"} (get-in % [:anew :place :value]))
                                     (apply concat told)))
             :once ((juxt :left-out (comp :value :place)) once)}))
        _ (make! :c-open (person :root))
        _ (setting! :c-open (person :root) :everyone)
        _ (put! (person :root) :c-open [(fact :rx :dose "public")])
        of-hers (stale-chain! :c-stale false)
        of-her-run (stale-chain! :c-stale-run true)
        ;; the review's P2 and its P3 of the yes's use, through the door
        _ (make! :c-yes (person :asha))
        root (perm (person :asha) :c-yes)
        m1 {:version 1 :who (person :ben) :lane :c-yes :session :s1 :facts [(fact :x :v 1)]}
        m2 {:version 1 :who (person :ben) :lane :c-yes :session :s1 :facts [(fact :x :v 2)]}
        [y1 y2 y-never] (repeatedly 3 #(UUID/randomUUID))
        pa1 (perm/act-permission (person :ben) :c-yes root y1 (digest/hex-for-a-yes m1))
        pa2 (perm/act-permission (person :ben) :c-yes root y2 (digest/hex-for-a-yes m1))
        run-p (perm/run-permission (person :ben) :c-yes root :run-1 :see-and-act {:salt (swap! give-salt inc)})
        _ (put! (person :asha) :c-yes [(perm/permission-fact pa1) (perm/permission-fact pa2) (perm/permission-fact run-p)])
        send! (fn [an-act] (let [sent (door/send-act (the-door) an-act)]
                             {:answer (door/answer-of (the-door) (:act-id sent) (:digest sent))}))
        wrong-act (send! (assoc m2 :permission (perm/permission-id pa2)))
        wrong-run (send! (assoc m1 :permission (perm/permission-id run-p) :session :run-2))
        taken (send! (assoc m1 :permission (perm/permission-id pa1)))
        _ (put! (person :asha) :c-yes [(perm/end-fact run-p)])
        words [(word wrong-act)
               (word (send! (assoc m2 :permission (perm/permission-id pa1))))
               (word (send! (assoc m1 :permission y-never)))
               (word wrong-run)
               (word (send! (assoc m1 :permission (perm/permission-id run-p) :session :run-2)))]
        use-read (:place (ask {:asks [:place y1 perm-key] :asker (person :asha) :lanes [:c-yes] :stores-own-facts true
                               :with #{:writer}}))]
    (check "the review's P1 on the cluster: an asker who holds a live read on a lane that she may not see, through a lent chain whose permission above ended, is handed nothing when the giver beneath it is given a new permission and then, in one act, tries to lend the asker one beneath it and end her own, which the gate refuses; another asker holds the lane's fact open on the same read path, and a read path of its own is played too; a read asked once leaves the lane out"
           {:word :permission-lent-from-one-the-act-ends
            :calls [1 1] :secret? false :once [[{:lane :c-stale :why :may-not-see}] "public"]}
           of-hers)
    (check "the same for the asker's sitting: the giver tries to lend her sitting a permission beneath hers and end hers in one act, which the gate refuses, and the asker who asks in that sitting is handed nothing"
           {:word :permission-lent-from-one-the-act-ends
            :calls [1 1] :secret? false :once [[{:lane :c-stale-run :why :may-not-see}] "public"]}
           of-her-run)
    (check "the review's P2 through the door, written again: an act whose digest is not a standing yes's is refused as not the act permitted; under the used yes, as used up; under an id never given, as never given; in another sitting under a standing sitting permission, as for another sitting; once ended, as ended"
           [:accepted [:act-not-the-one-permitted :permission-used-up :permission-never-given
                       :permission-for-another-sitting :permission-ended]]
           [(word taken) words])
    (check "the review's P3 of the yes's use, read back: the used yes's place says nothing here, with the stamp of the act that used it and who sent it, and names no fact"
           [:nothing-here (person :ben) false]
           [(:holds use-read) (:writer use-read) (contains? use-read :fact)])
    (shown "the second review's cluster probe, written again: the asker's calls, on the shared read path and on one of its own:"
           (pr-str (:calls of-hers)) "and in her run" (pr-str (:calls of-her-run))
           "| the words through the door:" (pr-str words) "| the used yes's place:" (pr-str (dissoc use-read :writer)))))

(deftest a-step-that-does-not-settle-ends-its-read
  (let [query (:ok (query/check {:asks [:place :p :v] :asker (person :ann) :from-lane :l0}))
        own (sources/own-rows-source :l0 :l0)
        place (sources/place-source :l0 :l0 :p :v)
        open-p (perm/open-permission :l0 :system)
        open-id (perm/permission-id open-p)
        [a-from a-to] (rows/by-holder-run :l0 :anyone nil)
        anyone-holders (sources/holders-source :l0 :l0 a-from a-to)
        anyone-holders-val {(rows/by-holder-key :l0 :anyone nil open-id) 1}
        anyone-row-key (rows/place-key :l0 open-id declaration/permission-key)
        anyone-row-src (sources/permission-row-source :l0 :l0 anyone-row-key)
        anyone-row-val (rows/fact-row {:e open-id :k declaration/permission-key :text (pr-str (perm/permission-value open-p))}
                                      (UUID. 0 1) 1 1 1000 ":system" nil)
        depot-key (rows/place-key :l0 :l0 :softland.store/depot)
        row (fn [k text index stamp] (rows/fact-row {:e :l0 :k k :text text} (UUID. 0 stamp) index stamp 1000 ":system" nil))
        own-rows (fn [listed] {depot-key (row :softland.store/depot ":streaming" 0 1)
                               (rows/place-key :l0 :l0 :softland.store/builds-on) (row :softland.store/builds-on (pr-str listed) 0 2)})
        snap {:standing :yes
              :read-set {[:fact depot-key] 1
                         [:holders a-from a-to] anyone-holders-val
                         [:fact anyone-row-key] 1}}
        calls (atom [])
        opened (fn [] (delay (CompletableFuture/completedFuture {:proxy :none})))
        entry (fn [] {:readers #{1} :called (java.util.concurrent.atomic.AtomicBoolean. true) :opening (opened)})
        initial-sources #{own anyone-holders anyone-row-src place}
        rp ((var-get #'softland.store.read-path/->ReadPath)
            "Store" nil nil nil nil nil nil nil nil nil nil nil nil nil (query/numbers nil)
            (atom {:pushes {own (entry) anyone-holders (entry) anyone-row-src (entry) place (entry)}
                   :counts {[:streaming :facts :l0] 3 [:streaming :permissions-by-holder :l0] 1}
                   :reads {1 {:query query :tell #(swap! calls conj %) :turn (Object.) :sources initial-sources
                              :rights {:l0 {:snapshot snap :push-seq 0 :asked-seq 0 :in-flight? false :same-snap-retries 0}}
                              :opened? true :owed? {:told {:l0 :yes}}}}})
            (java.util.concurrent.atomic.AtomicLong. 1))
        step (java.util.concurrent.atomic.AtomicInteger. 0)]
    ;; Each time the step asks what it holds, the lane's list names a lane that it has not
    ;; joined yet, and no push that it joins is given: the step never settles.
    (with-redefs [softland.store.read-path/known-of
                  (fn [_ read] (let [n (.getAndIncrement step)]
                                 (into {} (keep (fn [s] (cond (= s own) [s (own-rows [(keyword (str "l-" n))])]
                                                              (= s anyone-holders) [s anyone-holders-val]
                                                              (= s anyone-row-src) [s anyone-row-val]
                                                              (= s place) [s nil])))
                                       (:sources read))))
                  softland.store.read-path/open-push (fn [_ _ _] (CompletableFuture.))]
      (#'softland.store.read-path/on-change rp 1 own [[:put (rows/place-key :l0 :l0 :softland.store/builds-on)]]
                                           (own-rows []) (own-rows [:l-0])))
    (check "a step of an open read that still wants to join a source after the rounds that a live read is given ends the read: its function is told that it ended, and why, the read leaves the read path, and so does every push that it held"
           [[{:ended {:could-not :keep-the-read-live :because :a-step-did-not-settle}}] nil {}]
           [@calls (get-in (held rp) [:reads 1]) (:pushes (held rp))])
    (shown "a step that does not settle ends its read, after" (.get step) "readings of what it holds")))

(deftest one-push-for-one-thing
  (let [rp (:counted @round)
        _ (make! :t16 (person :alice))
        _ (give! :t16 (person :alice) (person :bob))
        _ (put! (person :alice) :t16 [(fact :note-41 :text "v0")])
        query (fn [who] {:asks [:place :note-41 :text] :asker who :lanes [:t16]})
        kept (keeping-pushes
              (fn []
                (let [alice (live rp (assoc (query (person :alice)) :with #{:writer}))
                      one (read-path/open-count rp)
                      entries (held rp)
                      bob (live rp (query (person :bob)))
                      two (read-path/open-count rp)
                      wrote (put! (person :alice) :t16 [(fact :note-41 :text "v1")])
                      _ (wait-until #(and (seq (later alice)) (seq (later bob))))
                      _ (settled)
                      told [(later alice) (later bob)]
                      left-alice (leave! alice)
                      after-alice (read-path/open-count rp)
                      left-bob (leave! bob)
                      after-bob (read-path/open-count rp)]
                  {:one one :two two :wrote wrote :told told :after-alice after-alice :after-bob after-bob
                   :left [left-alice left-bob (leave! bob)] :entries entries})))
        {:keys [one two wrote told after-alice after-bob left entries]} (:result kept)
        pushes (mapv (fn [{:keys [push]}] (.get ^CompletableFuture push)) (:opened kept))
        statuses (wait-until (fn [] (let [s (mapv rama/proxy-status pushes)] (when (every? #{:terminated} s) s))))
        third (counting (fn [] (let [read (live rp (query (person :alice)))] [(:value (:place (:now read))) (leave! read)])))
        ;; two open the same thing at once, 50 times
        together (counting
                  (fn []
                    (vec (for [_ (range 50)]
                           (let [start (CountDownLatch. 1)
                                 opened (mapv (fn [who] (future (.await start) (live rp (query who)))) [(person :alice) (person :bob)])
                                 _ (.countDown start)
                                 reads (mapv deref opened)
                                 counts (read-path/open-count rp)
                                 whole? (no-entry-without-a-reader? rp)]
                             (run! leave! reads)
                             [counts whole? (mapv (comp :value :place :now) reads)])))))
        ;; one joins while the last leaves, and a write follows, 50 times
        crossing (counting
                  (fn []
                    (vec (for [i (range 50)]
                           (let [first-read (live rp (query (person :alice)))
                                 start (CountDownLatch. 1)
                                 leaving (future (.await start) (leave! first-read))
                                 joining (future (.await start) (live rp (query (person :bob))))
                                 _ (.countDown start)
                                 left @leaving
                                 joined @joining
                                 v (str "crossing " i)
                                 _ (put! (person :alice) :t16 [(fact :note-41 :text v)])
                                 got (wait-until #(= v (get-in (screen joined) [:place :value])))
                                 whole? (no-entry-without-a-reader? rp)]
                             (leave! joined)
                             [left (some? got) whole?])))))
        closed (wait-until #(= (+ (:opens together) (:opens crossing)) (+ (:closes together) (:closes crossing))))]
    (check "one asker of a place in a members lane holds six pushes: the lane's own rows, her permission, the place, and the three holder runs"
           {:live-reads 1 :pushes 6} one)
    (check "a second asker of the same place joins the five pushes that are open, her permission above his lent one among them, and opens his own permission and his holder run"
           [{:live-reads 2 :pushes 8} 8] [two (count (:opened kept))])
    (check "a write to the place: each function is called once; the asker who asked for the writer is handed it, the other is not"
           [[1 1] [(person :alice) nil] ["v1" "v1"] [[(:act-id wrote) 0] [(:act-id wrote) 0]]]
           [(mapv count told)
            (mapv (fn [changes] (:writer (first (vals (:changed (first changes)))))) told)
            (mapv (fn [changes] (:value (first (vals (:changed (first changes)))))) told)
            (mapv (fn [changes] (:fact (first (vals (:changed (first changes)))))) told)])
    (check "the first leaves: the shared pushes stay, her permission that his is lent from among them, while her holder run is closed; the last leaves: nothing is held; a read that has left is not open"
           [{:live-reads 1 :pushes 7} {:live-reads 0 :pushes 0} [:left :left :not-open]] [after-alice after-bob left])
    (check "each push that the read path opened is closed when its last reader has left: Rama says terminated of all eight"
           (vec (repeat 8 :terminated)) statuses)
    (check "a third asker after the last one left opens new pushes, six, and gets a fresh value" [6 ["v1" :left]] [(:opens third) (:result third)])
    (check "what the read path holds for a live read and for a push: who looks at what, what each asked for, the pushes, the rights snapshots, and one mark; no field for a plain fact's row or value"
           [#{:query :tell :turn :sources :rights :opened? :owed?} #{:opening :readers :called}]
           [(set (mapcat keys (vals (:reads entries)))) (set (mapcat keys (vals (:pushes entries))))])
    (check "50 times, two threads open the same thing at once: one opening for each thing, eight pushes for the two, and each is closed when both have left"
           [(repeat 50 [{:live-reads 2 :pushes 8} true]) 400]
           [(map (fn [[counts whole?]] [counts whole?]) (:result together)) (:opens together)])
    (check "50 times, one joins while the last one leaves, and a write follows: the one who joined gets the write's change each time, so nobody was handed a push that is closed; the map of pushes holds no entry without a reader"
           (repeat 50 [:left true true]) (:result crossing))
    (check "Rama's opening of a push was called as often as its close, and nothing is held at the end"
           [true {:live-reads 0 :pushes 0}] [(some? closed) (read-path/open-count rp)])
    (shown "those who look at the same thing share one push: one asker of a place holds" (:pushes one) "pushes, two askers" (:pushes two)
           "; one write called each function once, and only the asker who asked for the writer was handed it; when the first left" (:pushes after-alice)
           "pushes stayed, when the last left none, and Rama says" (pr-str (distinct statuses)) "of each push")
    (shown "what the read path holds for a live read:" (pr-str (sort (set (mapcat keys (vals (:reads entries))))))
           "and for a push:" (pr-str (sort (set (mapcat keys (vals (:pushes entries)))))) "| read from the fields of its type")
    (shown "50 times two threads opened the same thing at once:" (:opens together) "openings of a push in all, eight each time, and as many closes"
           "| 50 times one joined while the last one left, with a write after: the joiner got the write's change each time; openings"
           (:opens crossing) "and closes" (:closes crossing) "| counted by functions of the proof's in the place of Rama's")))

(deftest the-read-paths-own-bound-on-its-pushes
  (let [ipc (:ipc @round)
        m (:module @round)
        ;; a read path that may hold three pushes on one table under one routing id
        rp (read-path/open ipc m {:max-pushes-under-one-routing-id 3})
        level? (fn [] (= (:counts (held rp)) (counts-of-pushes rp)))
        _ (doseq [lane [:p87 :q87]] (make! lane (person :alice)))
        _ (put! (person :alice) :p87 (into [(fact :a :text "a") (fact :b :text "b")]
                                  (map #(fact (keyword (str "t" %)) :text %)) (range 8)))
        _ (put! (person :alice) :q87 [(fact :a :text "a in q")])
        place (fn [lane e] {:asks [:place e :text] :asker (person :alice) :lanes [lane]})
        ;; one read fills the count of its lane: the lane's own rows, her permission, the place
        first-read (live rp (place :p87 :a))
        full [(read-path/open-count rp) (:counts (held rp)) (level?)]
        turned-away (counting #(read-path/read-live rp (place :p87 :b) (fn [_] nil)))
        after-turned-away [(read-path/open-count rp) (:counts (held rp)) (level?)]
        once (ask rp (place :p87 :b))
        same-thing (live rp (place :p87 :a))
        shared [(get-in same-thing [:now :place :value]) (read-path/open-count rp) (:counts (held rp)) (level?)]
        other-lane (live rp (place :q87 :a))
        apart [(get-in other-lane [:now :place :value]) (:counts (held rp)) (level?)]
        _ (leave! same-thing)
        _ (leave! first-read)
        after-left [(read-path/open-count rp) (:counts (held rp)) (level?)]
        now-room (live rp (place :p87 :b))
        with-room [(get-in now-room [:now :place :value]) (:counts (held rp)) (level?)]
        _ (leave! now-room)
        _ (leave! other-lane)
        emptied [(read-path/open-count rp) (held rp)]
        ;; eight threads, each a read of another place of the one lane, at once: 2 shared pushes and 8 places, where 3 may be held
        races (vec (for [_ (range 20)]
                     (let [start (promise)
                           reads (mapv (fn [i] (future @start (live rp (place :p87 (keyword (str "t" i)))))) (range 8))
                           _ (deliver start true)
                           got (mapv deref reads)
                           opened (filterv :leave-with got)
                           while-open [(count opened) (frequencies (map :could-not (remove :leave-with got)))
                                       (:counts (held rp)) (level?)]]
                       (run! leave! opened)
                       [while-open (held rp)])))
        ;; the facts by value are another table; and an open read that comes to need one push more
        _ (doseq [lane [:n87 :f87]] (make! lane (person :alice)))
        _ (mark! :f87 (person :alice) #{:parent})
        _ (put! (person :alice) :f87 [(fact :block-1 :parent :canvas-87)])
        points {:asks [:points-at :parent :canvas-87] :asker (person :alice) :lanes [:n87 :f87]}
        grows (live rp points)
        by-table [(count (:places (:now grows))) (:counts (held rp)) (level?)]
        _ (put! (person :alice) :f87 [(fact :block-2 :parent :canvas-87)])
        ended (wait-until #(some :ended (changes grows)))
        after-ended [(read-path/open-count rp) (held rp) (leave! grows)]
        ;; the base, from the base itself: its own rows, everyone's permission, the place, and everyone's holder run, under the base
        in-base (live rp {:asks [:place :e-07 :size] :asker (person :alice) :lanes [:base]})
        base-counts [(some? (:now in-base)) (:counts (held rp)) (level?)]
        _ (leave! in-base)
        ;; the base through a home: the count is of one home's pushes on the base. The homes here are
        ;; lanes that are not made, so a read holds one push on the home's own rows and no other there;
        ;; on the base's facts one home holds own-rows, everyone's permission, and up to two places under 4
        rp-home (read-path/open ipc m {:max-pushes-under-one-routing-id 4})
        level-home? (fn [] (= (:counts (held rp-home)) (counts-of-pushes rp-home)))
        via (fn [home e] {:asks [:place e :size] :asker (person :alice) :lanes [home :base]})
        first-of-home (live rp-home (via :unmade-87a :e-01))
        second-of-home (live rp-home (via :unmade-87a :e-02))
        home-full [(mapv #(get-in % [:now :place :value]) [first-of-home second-of-home]) (:counts (held rp-home)) (level-home?)]
        third-of-home (counting #(read-path/read-live rp-home (via :unmade-87a :e-03) (fn [_] nil)))
        other-home (live rp-home (via :unmade-87b :e-03))
        from-base (live rp-home {:asks [:place :e-03 :size] :asker (person :alice) :lanes [:base]})
        homes-apart [(mapv #(get-in % [:now :place :value]) [other-home from-base]) (:counts (held rp-home)) (level-home?)]
        _ (run! leave! [first-of-home second-of-home other-home from-base])
        after-homes (held rp-home)
        ;; the checks of one read that is kept live
        one-check (read-path/open ipc m {:max-checks-of-one-live-read 1})
        asked-once (ask one-check points)
        kept-live (counting #(read-path/read-live one-check points (fn [_] nil)))
        ;; an open read whose checks come to be more than one live read may need
        _ (doseq [lane [:n88 :f88]] (make! lane (person :alice)))
        _ (mark! :f88 (person :alice) #{:parent})
        _ (put! (person :alice) :f88 [(fact :block-1 :parent :canvas-88)])
        outgrows (live one-check {:asks [:points-at :parent :canvas-88] :asker (person :alice) :lanes [:n88 :f88]})
        opened-with (count (:places (:now outgrows)))
        _ (put! (person :alice) :f88 [(fact :block-2 :parent :canvas-88)])
        outgrown (wait-until #(some :ended (changes outgrows)))
        after-outgrown [(read-path/open-count one-check) (held one-check) (leave! outgrows)]]
    (check "a read path that may hold 3 pushes on one table under one routing id: one live read of a place in a members lane holds 3 on its facts, the lane's own rows, her permission and the place, and 3 on its permissions by holder; the count is the count of the map's entries"
           [{:live-reads 1 :pushes 6}
            {[:streaming :facts :p87] 3 [:streaming :permissions-by-holder :p87] 3} true]
           full)
    (check "a live read of another place of that lane needs one push more: it is turned away at once with the read path's own word, which carries what was too many and the bound; no push was opened for it and none closed"
           [{:could-not :keep-more-reads-live :what :pushes-under-one-routing-id :most 3} 0 0]
           [(:result turned-away) (:opens turned-away) (:closes turned-away)])
    (check "after it was turned away, the read path holds what it held: the first read and its 6 pushes, 3 on each table"
           [{:live-reads 1 :pushes 6}
            {[:streaming :facts :p87] 3 [:streaming :permissions-by-holder :p87] 3} true]
           after-turned-away)
    (check "a read asked once is not held to that bound: it reads the same place" "b" (get-in once [:place :value]))
    (check "a second live read of the same thing opens: it shares the 6 pushes, and a push that is open already is not counted again"
           ["a" {:live-reads 2 :pushes 6}
            {[:streaming :facts :p87] 3 [:streaming :permissions-by-holder :p87] 3} true]
           shared)
    (check "the pushes of another lane are under another routing id and are counted apart: a read through it opens while the first lane is full"
           ["a in q"
            {[:streaming :facts :p87] 3 [:streaming :permissions-by-holder :p87] 3
             [:streaming :facts :q87] 3 [:streaming :permissions-by-holder :q87] 3} true]
           apart)
    (check "when the readers of a lane have left, its count is gone, and the read that was turned away opens"
           [[{:live-reads 1 :pushes 6}
             {[:streaming :facts :q87] 3 [:streaming :permissions-by-holder :q87] 3} true]
            ["b"
             {[:streaming :facts :p87] 3 [:streaming :permissions-by-holder :p87] 3
              [:streaming :facts :q87] 3 [:streaming :permissions-by-holder :q87] 3} true]]
           [after-left with-room])
    (check "when every reader has left, nothing is held and no count is left" [{:live-reads 0 :pushes 0} {:pushes {} :counts {} :reads {}}] emptied)
    (check "20 times, eight threads each open a read of another place of one lane at once, where 3 pushes may be held on one table: one opens and seven are turned away with the word, the count stands at the bound, and nothing is held when the one has left"
           (repeat 20 [[1 {:keep-more-reads-live 7}
                        {[:streaming :facts :p87] 3 [:streaming :permissions-by-holder :p87] 3} true]
                       {:pushes {} :counts {} :reads {}}])
           races)
    (check "the three tables of one lane are counted apart: what points at a thing through two lanes holds 3 pushes on the nearer lane's facts, its own rows, her permission and the one check, and 3 on its permissions by holder; 2 on the farther lane's facts, 3 on its permissions by holder, and 1 on its facts by value, the order"
           [1 {[:streaming :facts :n87] 3 [:streaming :permissions-by-holder :n87] 3
               [:streaming :facts :f87] 2 [:streaming :permissions-by-holder :f87] 3
               [:streaming :facts-by-value :f87] 1} true]
           by-table)
    (check "a second place comes into the farther order under that open read, so it comes to need a second check, one push more than the read path may hold on the nearer lane's facts: the read ends with the word and why, and holds nothing"
           [{:ended {:could-not :keep-the-read-live :because :no-room-for-a-push-it-came-to-need
                     :what :pushes-under-one-routing-id :most 3}}
            [{:live-reads 0 :pushes 0} {:pushes {} :counts {} :reads {}} :not-open]]
           [(when ended (last (changes grows))) after-ended])
    (check "a read that starts from the base holds the base's own rows, everyone's permission and a place on its facts, and everyone's holder run on its permissions by holder, all counted under the base's routing id"
           [true {[:microbatch :facts :base] 3 [:microbatch :permissions-by-holder :base] 1} true] base-counts)
    (check "in the base the count is of the pushes of one home: where 4 pushes may be held on one table, two reads of two places through one home that is not made hold 4 pushes on the base's facts under that home, its own rows, everyone's permission and the two places, 1 on the base's permissions by holder, and 1 on the home's own rows"
           [[1 2] {[:streaming :facts :unmade-87a] 1
                   [:microbatch :facts :unmade-87a] 4
                   [:microbatch :permissions-by-holder :unmade-87a] 1} true]
           home-full)
    (check "a third place of the base through that home needs one push more than the read path may hold there: it is turned away with the word, and no push was opened or closed for it"
           [{:could-not :keep-more-reads-live :what :pushes-under-one-routing-id :most 4} 0 0]
           [(:result third-of-home) (:opens third-of-home) (:closes third-of-home)])
    (check "the same place opens through another home, and from the base itself: each home has a count of its own on the base, and the full home's count stands"
           [[3 3] {[:streaming :facts :unmade-87a] 1 [:microbatch :facts :unmade-87a] 4 [:microbatch :permissions-by-holder :unmade-87a] 1
                   [:streaming :facts :unmade-87b] 1 [:microbatch :facts :unmade-87b] 3 [:microbatch :permissions-by-holder :unmade-87b] 1
                   [:microbatch :facts :base] 3 [:microbatch :permissions-by-holder :base] 1} true]
           homes-apart)
    (check "when those readers have left, nothing is held and no count is left" {:pushes {} :counts {} :reads {}} after-homes)
    (check "the checks of one read that is kept live have a number of their own: where one such read may need 1 check, what points at a thing with two places in the farther order is given whole when asked once, and kept live it is turned away with the word; 11 pushes were opened, for the rights of the two lanes and the one order, none for a check, and each was closed"
           [2 {:could-not :serve-a-read-that-large :what :checks-of-one-read :most 1} 11 11 {:live-reads 0 :pushes 0}]
           [(count (:places asked-once)) (:result kept-live) (:opens kept-live) (:closes kept-live) (read-path/open-count one-check)])
    (check "where one read kept live may need 1 check: a read of what points at a thing opens with one place in the farther order, so with one check; a second place comes into that order, so it comes to need two: the read ends with the word and why, and holds nothing"
           [1 {:ended {:could-not :keep-the-read-live :because :a-run-grew-too-large :what :checks-of-one-read :most 1}}
            [{:live-reads 0 :pushes 0} {:pushes {} :counts {} :reads {}} :not-open]]
           [opened-with (when outgrown (last (changes outgrows))) after-outgrown])
    (shown "the read path's own bound on its pushes, with 3 for one table under one routing id: a read that needs one more is turned away with"
           (pr-str (:result turned-away)) "after" (:opens turned-away) "openings of a push; a read asked once of the same place still answers; a second reader of an open thing is not counted again"
           "| eight threads at once, 20 times: each time one opened and seven were turned away"
           "| an open read that came to need one push more ended with" (pr-str (:because (:ended (last (changes grows))))))
    (shown "the counts, read from the read path's own memory: what points at a thing through two lanes" (pr-str (nth by-table 1))
           "| a place of the base, from the base itself" (pr-str (nth base-counts 1)) "| at each moment the counts were the counts of the map's entries")
    (shown "on the base the bound holds the pushes of one home: with 3 for one table under one routing id, two places of the base through one home filled its count,"
           (pr-str (nth home-full 1)) "; a third place through that home was turned away with" (pr-str (:could-not (:result third-of-home)))
           "after" (:opens third-of-home) "openings, and opened through another home and from the base itself:" (pr-str (nth homes-apart 1)))
    (shown "kept live, a read that needs more checks than one live read may is turned away before a check is opened:" (pr-str (:result kept-live))
           "after" (:opens kept-live) "openings; asked once the same read gave" (count (:places asked-once)) "places")))

(deftest nothing-is-shown-that-is-not-in-the-tables
  (let [rp (:counted @round)
        _ (give! :t16 (person :alice) (person :vera) :see)
        asker (live rp {:asks [:place :note-41 :text] :asker (person :bob) :lanes [:t16]})
        refused (act! (person :vera) :t16 [{:e :note-41 :k :text}])
        accepted (put! (person :alice) :t16 [(fact :note-41 :text "after the refused act")])
        got (wait-until #(= "after the refused act" (get-in (screen asker) [:place :value])))
        all (later asker)
        facts (keep (fn [change] (:fact (or (:place (:anew change)) (first (vals (:changed change)))))) (changes asker))
        answers (mapv (fn [[act-id]] (:answer (door/answer-of (the-door) act-id (:digest (get @sent-things act-id))))) facts)
        _ (leave! asker)]
    (check "an act that the gate refuses at a place gives a live asker of that place nothing: she has the change of the accepted act that followed, and no other"
           [[:refused :permission-does-not-allow-writing] true 1 [(:act-id accepted) 0]]
           [((juxt :answer :reason) (:answer refused)) (some? got) (count all) (:fact (first (vals (:changed (first all)))))])
    (check "every value that she was given is of an act that a gate accepted, by the door's answer for the act of its fact"
           [true true] [(pos? (count answers)) (every? #{:accepted} answers)])
    (check "when every asker has left, the read path holds no reader, no push and no count of a push" {:pushes {} :counts {} :reads {}} (held rp))
    (shown "nothing is shown before it is in the tables: a refused act at a watched place gave the asker nothing; each of" (count answers)
           "values that she was given is of an accepted act; after every asker left the read path's three maps are" (pr-str (held rp)))))

(deftest reads-that-fail
  (let [_ (make! :l19 (person :alice))
        _ (put! (person :alice) :l19 [(fact :note :text "v0")])
        query {:asks [:place :note :text] :asker (person :alice) :lanes [:l19]}
        never (fn [& _] (CompletableFuture.))
        threw (with-redefs [rama/compiled-foreign-select-async (fn [& _] (throw (ex-info "the proof made this select throw" {})))]
                (ask query))
        late (with-redefs [rama/compiled-foreign-select-async never]
               (let [began (System/currentTimeMillis)
                     got (ask (:short @round) query)]
                 [got (- (System/currentTimeMillis) began)]))
        ;; an opening that throws, and one that never comes back, under an open read
        _ (list! :l19 (person :alice) [])
        failing (fn [rp stand-in]
                  (let [read (live rp {:asks [:place :note :text] :asker (person :alice) :from-lane :l19})]
                    (with-redefs [rama/compiled-foreign-proxy-async stand-in]
                      (list! :l19 (person :alice) [:main])
                      (let [ended (wait-until #(some :ended (changes read)))]
                        [ended (read-path/open-count rp) (leave! read) (.getName ^Thread (:thread (last @(:calls read))))]))))
        open-threw (failing (the-rp) (fn [& _] (throw (ex-info "the proof made this opening throw" {}))))
        _ (list! :l19 (person :alice) [])
        open-late (failing (:short @round) never)
        ;; Rama ends a push
        kept (keeping-pushes #(live {:asks [:place :ended-by-rama :text] :asker (person :alice) :lanes [:l19]}))
        ended-read (:result kept)
        place-push (first (filter (fn [{:keys [options]}] (= :l19 (:pkey options))) (reverse (:opened kept))))
        _ ((:callback-fn (:options (last (:opened kept)))) nil (UngracefulTerminationDiff. (ex-info "the proof made this diff" {})) nil)
        store-ended (wait-until #(some :ended (changes ended-read)))
        after-store-ended [(read-path/open-count (the-rp)) (leave! ended-read)]
        ;; a value that Rama sends anew, after the first callback of a push: the proof makes Rama's own
        ;; diff for it and hands it to the callback of a run's push, once with the rows as they are and
        ;; once with one row that differs
        _ (put! (person :alice) :l19 [(fact :sheet :a 1) (fact :sheet :b 2)])
        kept-anew (keeping-pushes #(live {:asks [:entity :sheet] :asker (person :alice) :lanes [:l19]}))
        anew-read (:result kept-anew)
        key-a (rows/place-key :l19 :sheet :a)
        run-push (first (filter (fn [{:keys [push]}]
                                  (let [value @(.get ^CompletableFuture push)]
                                    (and (map? value) (contains? value key-a))))
                                (:opened kept-anew)))
        run-value (when run-push @(.get ^CompletableFuture (:push run-push)))
        sent-anew (when run-push
                    (let [callback (:callback-fn (:options run-push))
                          before (count (later anew-read))
                          _ (callback run-value (ResyncDiff.) run-value)
                          after-the-same (count (later anew-read))
                          differs (update run-value key-a update :stamp inc)
                          _ (callback differs (ResyncDiff.) run-value)
                          told (subvec (later anew-read) after-the-same)]
                      {:before before :after-the-same after-the-same :told told}))
        _ (leave! anew-read)
        ;; Rama ends a push of a read while the read opens, 40 times. The proof hands the ending diff to the
        ;; callback of the read's first push on a thread of its own, some moments after that push was opened,
        ;; as Rama would: never from inside a call of the read path.
        crossings
        (vec (for [turn (range 40)]
               (let [calls (atom [])
                     first-callback (promise)
                     real-open rama/compiled-foreign-proxy-async
                     got (with-redefs [rama/compiled-foreign-proxy-async
                                       (fn [the-path pstate options]
                                         (when-not (realized? first-callback)
                                           (deliver first-callback (:callback-fn options))
                                           (future (Thread/sleep (long (quot turn 4)))
                                                   ((:callback-fn options) nil
                                                    (UngracefulTerminationDiff. (ex-info "the proof made this diff" {})) nil)))
                                         (real-open the-path pstate options))]
                           (read-path/read-live (the-rp) {:asks [:place (keyword (str "crossing-" turn)) :text]
                                                         :asker (person :alice) :lanes [:l19]}
                                                (fn [change] (swap! calls conj change))))
                     opened? (contains? got :now)
                     _ (wait-until 5000 #(if opened? (some :ended @calls) true))
                     _ (wait-until 5000 #(= {:live-reads 0 :pushes 0} (read-path/open-count (the-rp))))]
                 {:opened? opened?
                  :word (:could-not got)
                  :calls (mapv (fn [change] (cond (:anew change) :anew (:changed change) :changed :else :ended)) @calls)
                  :first-is-the-answer? (or (empty? @calls) (= {:anew (:now got)} (first @calls)))
                  :because (:because (some :ended @calls))
                  :held (read-path/open-count (the-rp))})))
        ;; a function that throws, and throws again when it is given {:ended}
        calls (atom [])
        thrower (read-path/read-live (the-rp) query
                                     (fn [change]
                                       (swap! calls conj change)
                                       (when-not (:anew change) (throw (ex-info "the proof's function throws" {})))))
        _ (put! (person :alice) :l19 [(fact :note :text "v1")])
        thrown (wait-until #(some :ended @calls))
        after-thrown [(read-path/open-count (the-rp)) (read-path/leave (the-rp) (:leave-with thrower))]
        first-call-throws (read-path/read-live (the-rp) query (fn [_] (throw (ex-info "the proof's function throws at once" {}))))
        after-first [(some? (:now first-call-throws)) (read-path/leave (the-rp) (:leave-with first-call-throws))
                     (read-path/open-count (the-rp))]
        ;; a thing that is no function, where a function is taken; and a thing that is no handle of this read path
        no-function (mapv (fn [thing]
                            (let [got (read-path/read-live (the-rp) query thing)]
                              [(= (ask query) (:now got)) (read-path/leave (the-rp) (:leave-with got)) (read-path/open-count (the-rp))]))
                          [nil 5 "a function"])
        elsewhere (live (:short @round) query)
        no-handle (mapv #(read-path/leave (the-rp) %) [nil 5 "a handle" {:leave-with 1} (:leave-with elsewhere)])
        still-open-elsewhere (read-path/open-count (:short @round))
        left-elsewhere (leave! elsewhere)]
    (check "a select that throws: the word :reach-the-store, with what Rama's client threw; the call does not throw"
           [:reach-the-store "clojure.lang.ExceptionInfo"] ((juxt :could-not :threw) threw))
    (check "a select that does not come back within a wait of 300 ms: the word, and the call returns after about that long"
           [{:could-not :get-an-answer-within-its-wait :waited-ms 300} true]
           [(first late) (< 250 (second late) 3000)])
    (check "the opening of a push that an open read came to need throws: the read ends with its word and why, and holds no push; the map of pushes holds no entry of the failed opening"
           [[:keep-the-read-live :a-read-it-came-to-need-failed "clojure.lang.ExceptionInfo"] {:live-reads 0 :pushes 0} :not-open]
           [((juxt :could-not :because :threw) (first open-threw)) (second open-threw) (nth open-threw 2)])
    (check "the opening of a push that an open read came to need never comes back: after the wait the read ends, on a thread that is not the caller's"
           [[:keep-the-read-live :a-read-it-came-to-need-failed 300] {:live-reads 0 :pushes 0} :not-open true]
           [((juxt :could-not :because :waited-ms) (first open-late)) (second open-late) (nth open-late 2)
            (not= (.getName (Thread/currentThread)) (nth open-late 3))])
    (check "Rama ends a push, by the diff that its pages name, handed to the push's callback by the proof: each read on it ends with the word and why"
           [[:keep-the-read-live :the-store-ended-a-push] [{:live-reads 0 :pushes 0} :not-open] true]
           [((juxt :could-not :because) store-ended) after-store-ended (some? place-push)])
    (check "a value that Rama sends anew after the first callback of a push, by Rama's own diff for it, made by the proof and handed to the callback of a run's push that holds two places: with the rows as they are, the asker is told nothing; with one row that differs, she is told that one place, with what the read gives there, and not the other, and no whole answer"
           [true [0 0] [[[:sheet :a]]] [1]]
           [(some? run-push)
            [(:before sent-anew) (:after-the-same sent-anew)]
            (mapv (comp vec keys :changed) (:told sent-anew))
            (mapv (fn [change] (get-in change [:changed [:sheet :a] :value])) (:told sent-anew))])
    (check "40 times, Rama's ending diff is handed to the callback of a read's first push while the read opens, on another thread. Each time one of two things holds, and no other: the read was not opened yet, read-live gives a word, and the function was not called at all; or the read was opened, the function was given the whole answer first and then {:ended}. Never {:ended} before the answer, and never {:ended} alone. Nothing is held afterwards"
           (repeat 40 true)
           (map (fn [{:keys [opened? word calls first-is-the-answer? because held]}]
                  (and (= {:live-reads 0 :pushes 0} held)
                       (if opened?
                         (and (= [:anew :ended] calls) first-is-the-answer? (= :the-store-ended-a-push because))
                         (and (= :get-an-answer-within-its-wait word) (= [] calls)))))
                crossings))
    (check "a function that throws when it is given a change, and throws again when it is given {:ended}: the read ends, nothing is held, and no call throws"
           [[:keep-the-read-live :its-function-threw] [{:live-reads 0 :pushes 0} :not-open]]
           [((juxt :could-not :because) thrown) after-thrown])
    (check "a function that throws at its first call: read-live still gives the answer, the read is ended, and its handle is not open"
           [true :not-open {:live-reads 0 :pushes 0}] after-first)
    (check "a thing that is no function, where read-live takes a function, is as a function that throws at its first call, for nil, a number and a string: read-live gives the answer, the handle is not open, nothing is held, and no call throws"
           (repeat 3 [true :not-open {:live-reads 0 :pushes 0}]) no-function)
    (check "leave of a thing that is no handle of this read path gives :not-open and does not throw: nil, a number, a string, a map, and the handle of a read of another read path, which stays open there until it is left there"
           [(repeat 5 :not-open) 1 :left] [no-handle (:live-reads still-open-elsewhere) left-elsewhere])
    (shown "reads that fail, each with a function of the proof's in the place of a call of Rama's client: a select that throws gives" (pr-str (:could-not threw))
           "| one that never comes back gives" (pr-str (first late)) "after" (second late) "ms")
    (shown "under an open read: an opening that throws ends the read with" (pr-str (select-keys (first open-threw) [:could-not :because]))
           "| one that never comes back ends it after its wait, on the thread" (nth open-late 3)
           "| an UngracefulTerminationDiff of Rama's class, made by the proof and handed to the callback, ends it with" (pr-str (:because store-ended))
           "| a ResyncDiff of Rama's class, made by the proof and handed to the callback of a run's push: with the same rows the asker was told"
           (- (:after-the-same sent-anew) (:before sent-anew)) "things, with one row that differs" (count (:told sent-anew)) "change, of the places" (pr-str (mapv (comp vec keys :changed) (:told sent-anew)))
           "| the same diff while a read opened, 40 times:" (count (remove :opened? crossings)) "times the read was not opened yet, read-live gave a word and the function was not called;"
           (count (filter :opened? crossings)) "times it was opened, and the function was given the answer and then {:ended}"
           "| a function that throws ends it with" (pr-str (:because thrown)) "| no call threw, and after each the read path held no push")))

(deftest the-function-and-its-threads
  (let [_ (make! :l20 (person :alice))
        read (live {:asks [:entity :busy] :asker (person :alice) :lanes [:l20]})
        writers (mapv (fn [t] (future (vec (for [i (range 50)]
                                             (act! (person :alice) :l20 [(fact :busy (keyword (str "k" t "-" i)) i)])))))
                      (range 4))
        sent (vec (mapcat deref writers))
        not-accepted (vec (remove :accepted? sent))
        ;; straight after the acks: through the read path that holds the open push, and through one that holds none
        beside-the-push (count (:places (ask {:asks [:entity :busy] :asker (person :alice) :lanes [:l20]})))
        by-a-select (count (:places (ask (:counted @round) {:asks [:entity :busy] :asker (person :alice) :lanes [:l20]})))
        _ (wait-until 30000 #(= 200 (count (:places (screen read)))))
        once (ask {:asks [:entity :busy] :asker (person :alice) :lanes [:l20]})
        ended (wait-until 30000 #(= (:places once) (:places (screen read))))
        calls @(:calls read)
        threads (distinct (map #(.getName ^Thread (:thread %)) (rest calls)))
        _ (leave! read)]
    (check "200 acts from 4 threads on one entity, each of which the door sent and the gate accepted: the asker's screen ends at what a read asked once gives, a place for each act"
           [true 200 [] 200]
           [(some? ended) (count sent) (mapv (fn [got] (or (:answer got) (select-keys (:sent got) [:could-not]))) not-accepted)
            (count (:places once))])
    (check "the first call of the function is on the caller's own thread; every later call is on another thread, a thread of Rama's client"
           [true true] [(= (:caller read) (:thread (first calls))) (every? #(not= (:caller read) (:thread %)) (rest calls))])
    (check "the calls for one read come one at a time: no call began while another was running" false (boolean (some :overlap? calls)))
    (check "straight after the 200 acks, a read asked once gives every place, through the read path that holds an open push on that very run and through a read path that holds none: each reads the table, and neither is given the value of a push"
           [200 200] [beside-the-push by-a-select])
    (shown "a read asked once reads the table, also where a push is open on its source: straight after the acks of 200 acts it gave" beside-the-push
           "of 200 places through the read path that holds the push, and" by-a-select "through a read path that holds none")
    (shown "the function of a live read, under 200 acts from 4 threads:" (count calls) "calls, the first on the caller's thread" (.getName ^Thread (:caller read))
           ", the others on" (pr-str threads) "| no two calls for the read overlapped")))

(deftest one-reader-in-the-source
  (let [files (filter #(str/ends-with? (.getName ^java.io.File %) ".clj") (file-seq (io/file "src-store")))
        holds (fn [word] (set (keep (fn [^java.io.File f] (when (str/includes? (slurp f) word) (.getName f))) files)))
        own (slurp (io/file "src-store/softland/store/read_path.clj"))]
    (check "the four reading calls of Rama's client are named in the read path's one file and in no other file of the store's source"
           [#{"read_path.clj"} #{"read_path.clj"} #{"read_path.clj"} #{"read_path.clj"}]
           (mapv holds ["foreign-pstate" "foreign-select" "foreign-proxy" "foreign-invoke-query-async"]))
    (check "the read path's file names no append and no depot" [false false]
           [(str/includes? own "foreign-append!") (str/includes? own "foreign-depot")])
    (shown "one reader of facts in the store's source, by a search of" (count files) "files as text: foreign-pstate, foreign-select, foreign-proxy and foreign-invoke-query-async are in read_path.clj and in no other; it holds no foreign-append! and no foreign-depot"
           "| the proofs' own handles are the stated exception")))

(deftest the-order-of-the-tables-keys
  (let [_ (make! :o23 (person :alice))
        wide (String. (Character/toChars 0x1D49C))
        ids (mapv #(keyword (str "x" %)) ["z" "\u00E4" "\u044F" "\u6C34"])
        far-ids (mapv #(keyword (str "x" %)) ["\uFB00" wide])
        _ (put! (person :alice) :o23 (mapv #(fact % :n (str %)) (concat ids far-ids)))
        _ (put! (person :alice) :o23 [(fact :a :n 1) (fact :a! :n 2) (fact :ab :n 3)])
        run (fn [from to] (set (map first (keys (:places (ask {:asks [:range [from] [to]] :asker (person :alice) :lanes [:o23]}))))))
        entity (set (keys (:places (ask {:asks [:entity :a] :asker (person :alice) :lanes [:o23]}))))
        far-entities (mapv (fn [e] (set (keys (:places (ask {:asks [:entity e] :asker (person :alice) :lanes [:o23]}))))) far-ids)
        far-run ((juxt :could-not :why) (ask {:asks [:range [(nth ids 0)] [(nth far-ids 1)]] :asker (person :alice) :lanes [:o23]}))
        whole-lane (set (map first (keys (:places (ask {:asks [:range :first :last] :asker (person :alice) :lanes [:o23]})))))
        ;; a lane whose text is the start of another's, on one task
        _ (make! :pfx (person :alice))
        neighbour (loop [i 0]
                    (let [lane (keyword (str "pfx!" i))]
                      (make! lane (person :alice))
                      (cond (and (some? (task-of lane)) (= (task-of :pfx) (task-of lane))) lane
                            (< i 40) (recur (inc i)))))
        _ (put! (person :alice) :pfx [(fact :mine :n 1)])
        _ (put! (person :alice) neighbour [(fact :not-mine :n 2)])
        whole (set (keys (:places (ask {:asks [:range :first :last] :asker (person :alice) :lanes [:pfx]}))))]
    (check "entities whose ids hold letters outside ASCII, of Latin, Cyrillic and Han: a run between two of them gives the places that the table's order holds between its ends, which is the order by code point"
           [(set (subvec ids 1)) (set (subvec ids 2)) (set ids) (set (subvec ids 0 3))]
           [(run (nth ids 1) (nth ids 3)) (run (nth ids 2) (nth ids 3)) (run (nth ids 0) (nth ids 3)) (run (nth ids 0) (nth ids 2))])
    (check "an entity whose id holds a letter from U+E000 on, and one whose id holds a letter outside the first plane: each is read as an entity, and gives its own place and no other"
           [#{[(nth far-ids 0) :n]} #{[(nth far-ids 1) :n]}] far-entities)
    (check "a run between two entities that a caller names is turned away when an end holds such a letter: a range of Rama's between such ends was seen to give fewer places than the table holds, and the read path reads none"
           [:read-the-query :range-end-not-ordered] far-run)
    (check "the whole of the lane holds every entity, those with such letters too"
           true (every? whole-lane (concat ids far-ids [:a :a! :ab])))
    (check "Java's own order of Strings would put the letter outside the first plane before the one at U+FB00; the read path's order, and the table's, do not"
           [true false] [(neg? (compare wide "\uFB00")) (rows/before? wide "\uFB00")])
    (check "the run of an entity holds no place of an entity whose text begins with its text" #{[:a :n]} entity)
    (check "a lane whose text is the start of another's, on the same task: the whole of the first lane holds no place of the second"
           [true #{[:mine :n]}] [(some? neighbour) whole])
    (shown "the order of the store's own table is by code point: a run between entities with letters outside ASCII gives" (pr-str (sort (map str (run (nth ids 1) (nth ids 3)))))
           "| an entity whose id holds a letter from U+E000 on or outside the first plane is read as an entity and gives its own place; a run between two ends that a caller names, one of which holds such a letter, gives" (pr-str far-run)
           "| the entity :a holds no place of :a! or :ab | the whole of the lane :pfx holds no place of" neighbour
           ", which the proof found on the same task by a select of its own")))

(deftest a-function-that-leaves-or-asks-inside-its-call
  (let [_ (make! :l24 (person :alice))
        _ (put! (person :alice) :l24 [(fact :note :text "v0")])
        rp (the-rp)
        query {:asks [:place :note :text] :asker (person :alice) :lanes [:l24]}
        ;; a function that leaves its own read inside its call
        own-handle (promise)
        own-calls (atom [])
        own (read-path/read-live rp query (fn [change]
                                            (swap! own-calls conj change)
                                            (when (:changed change)
                                              (swap! own-calls conj (read-path/leave rp @own-handle)))))
        _ (deliver own-handle (:leave-with own))
        _ (put! (person :alice) :l24 [(fact :note :text "v1")])
        _ (wait-until #(some #{:left} @own-calls))
        _ (put! (person :alice) :l24 [(fact :note :text "v2")])
        _ (settled)
        own-after [(count @own-calls) (last @own-calls) (read-path/open-count rp)]
        ;; a function that leaves another read
        other (live query)
        leaver (read-path/read-live rp query (fn [change] (when (:changed change) (leave! other))))
        _ (put! (person :alice) :l24 [(fact :note :text "v3")])
        _ (wait-until #(= 1 (:live-reads (read-path/open-count rp))))
        other-left (leave! other)
        _ (read-path/leave rp (:leave-with leaver))
        ;; two reads on one push, each leaving the other inside its function, 50 times
        crossed (vec (for [i (range 50)]
                       (let [a (promise) b (promise)
                             read-a (read-path/read-live rp query (fn [change] (when (:changed change) (read-path/leave rp @b))))
                             read-b (read-path/read-live rp query (fn [change] (when (:changed change) (read-path/leave rp @a))))]
                         (deliver a (:leave-with read-a))
                         (deliver b (:leave-with read-b))
                         (put! (person :alice) :l24 [(fact :note :text (str "crossed " i))])
                         (wait-until #(<= (:live-reads (read-path/open-count rp)) 1))
                         (read-path/leave rp (:leave-with read-a))
                         (read-path/leave rp (:leave-with read-b))
                         (read-path/open-count rp))))
        ;; a function that asks inside its call
        asked (atom [])
        inside-calls (atom [])
        asking (counting
                (fn []
                  (let [read (read-path/read-live rp query
                                                  (fn [change]
                                                    (let [c (counting
                                                             (fn []
                                                               [(:could-not (read-path/read-once rp query))
                                                                (:could-not (read-path/read-live rp query (fn [_] nil)))]))]
                                                      (swap! inside-calls conj [(:selects c) (:queries c) (:opens c)])
                                                      (swap! asked conj
                                                             (into [(cond (:anew change) :anew (:changed change) :changed :else :ended)]
                                                                   (:result c))))))]
                    (put! (person :alice) :l24 [(fact :note :text "asked")])
                    (wait-until #(= 2 (count @asked)))
                    (put! (person :alice) :l24 [(fact :note :text "asked again")])
                    (wait-until #(= 3 (count @asked)))
                    [(read-path/open-count rp) (read-path/leave rp (:leave-with read))])))]
    (check "a function that calls leave on its own read, inside its call: leave gives :left and returns, the read gets no call after it, and nothing is held"
           [3 :left {:live-reads 0 :pushes 0}] own-after)
    (check "a function that leaves another read: that read is not open afterwards" :not-open other-left)
    (check "50 times, two reads on one push, each leaving the other inside its function: nothing stands still, and nothing is held afterwards"
           (repeat 50 {:live-reads 0 :pushes 0}) crossed)
    (check "a function that calls read-once and read-live inside its call, at its first call on the caller's thread and at each change on Rama's: both are turned away with the word of their own; the read whose function asked goes on to its next change"
           [[:anew :read-inside-an-askers-function :read-inside-an-askers-function]
            [:changed :read-inside-an-askers-function :read-inside-an-askers-function]
            [:changed :read-inside-an-askers-function :read-inside-an-askers-function]]
           @asked)
    (check "no call was made to Rama for a read that was turned away: inside the function the selects, queries and openings are none, and in all they are the three holder-size selects, one rights query and six pushes of the one read that opened"
           [[[0 0 0] [0 0 0] [0 0 0]] 3 1 6 [{:live-reads 1 :pushes 6} :left]]
           [@inside-calls (:selects asking) (:queries asking) (:opens asking) (:result asking)])
    (shown "a function may leave inside its own call: its own read, " (pr-str (take-last 2 @own-calls)) ", and another read; 50 times two reads on one push each left the other and nothing stood still")
    (shown "a read asked from inside an asker's function is turned away at once:" (pr-str (distinct (map rest @asked)))
           "| selects made meanwhile:" (:selects asking) "| the read whose function asked went on and was told" (count @asked) "times")))

(deftest the-opening
  (let [_ (make! :l25 (person :alice))
        _ (give! :l25 (person :alice) (person :bob))
        rp (the-rp)
        turns (vec (for [i (range 50)]
                     (let [e (keyword (str "opening-" i))
                           maker (future (dotimes [n 20] (act! (person :alice) :l25 [(fact e :text n)])))
                           _ (Thread/sleep (long (rand-int 25)))
                           read (live {:asks [:place e :text] :asker (person :bob) :lanes [:l25]})
                           _ @maker
                           ended (wait-until #(= 19 (get-in (screen read) [:place :value])))
                           calls (changes read)]
                       (leave! read)
                       [(some? ended) (:anew (first calls)) (:now read) (get-in (:now read) [:place :value]) (count calls)])))
        ;; a function that stays 2 s, and a read of another asker meanwhile
        _ (put! (person :alice) :l25 [(fact :slow :text "v0")])
        staying (atom nil)
        slow (read-path/read-live rp {:asks [:place :slow :text] :asker (person :alice) :lanes [:l25]}
                                  (fn [change] (when (:changed change) (reset! staying true) (Thread/sleep 2000) (reset! staying false))))
        _ (put! (person :alice) :l25 [(fact :slow :text "v1")])
        _ (wait-until #(true? @staying))
        began (System/currentTimeMillis)
        meanwhile (try (ask {:asks [:place :slow :text] :asker (person :bob) :lanes [:l25]}) (catch Throwable t [:threw (str t)]))
        took (- (System/currentTimeMillis) began)
        _ (wait-until #(false? @staying))
        _ (read-path/leave rp (:leave-with slow))]
    (check "50 times, a maker writes one place 20 times in quick turn while read-live opens on it: each time the screen, made only from what the function was given, ends at the value of the last act"
           (repeat 50 true) (map first turns))
    (check "each time the first thing that the function is given is the whole answer, the same that :now holds: no change is lost between the opening of the pushes and that answer, and none is told before it"
           (repeat 50 true) (map (fn [[_ anew now]] (and (some? anew) (= anew now))) turns))
    (check "a read of another asker, asked while one asker's function stays for 2 s, gives an answer or a word and does not throw"
           true (and (map? meanwhile) (or (contains? meanwhile :place) (contains? meanwhile :could-not))))
    (shown "the opening: 50 times a maker wrote one place 20 times in quick turn while a live read opened on it; the first call of the function was the whole answer each time, and the screen ended at the last value; the value in the first answer was"
           (pr-str (frequencies (map #(if (nil? (nth % 3)) :no-fact-yet (if (= 19 (nth % 3)) :the-last :one-between)) turns))))
    (shown "while one asker's function stayed for 2 s, a read of another asker gave"
           (if (:could-not meanwhile) (pr-str (select-keys meanwhile [:could-not])) "an answer") "after" took "ms; held to no number")))

(deftest the-first-thing-a-function-is-given
  (let [opened (filter :leave-with @live-reads)
        firsts (map (fn [read] (let [c (first @(:calls read))] [(= {:anew (:now read)} (:change c)) (= (:caller read) (:thread c))])) opened)]
    (check "of every live read that this proof opened with its noting function: the first call of the function was the whole answer that read-live gave under :now, on the caller's own thread"
           [true (pos? (count opened))] [(every? #(= [true true] %) firsts) true])
    (check "no call of any of them overlapped another call for the same read" false (boolean (some :overlap? (mapcat (comp deref :calls) opened))))
    (shown "the first thing that a function is given is the whole answer: of" (count opened) "live reads that the proof opened, each first call was {:anew answer}, equal to :now, on the caller's thread, before any change")))

(deftest the-module-is-started-again
  (let [_ (make! :l21 (person :alice))
        _ (put! (person :alice) :l21 [(fact :note :text "before")])
        read (live {:asks [:place :note :text] :asker (person :alice) :lanes [:l21]})
        _ (rtest/update-module! (:ipc @round) module/Store)
        once (try (ask {:asks [:place :note :text] :asker (person :alice) :lanes [:l21]}) (catch Throwable t [:threw (str t)]))
        wrote (act! (person :alice) :l21 [(fact :note :text "after")])
        outcome (wait-until 60000 #(or (some :ended (changes read))
                                       (when (= "after" (get-in (screen read) [:place :value])) :got-the-change)))
        once-after (try (ask {:asks [:place :note :text] :asker (person :alice) :lanes [:l21]}) (catch Throwable t [:threw (str t)]))
        _ (leave! read)]
    (check "a read asked once straight after the module was started again gives an answer or a word, and does not throw"
           true (and (map? once) (or (contains? once :place) (contains? once :could-not))))
    (check "an open read gets the change of an act that was sent after it, or ends with a word; it does not throw"
           true (or (= :got-the-change outcome) (= :keep-the-read-live (:could-not (:ended outcome)))))
    (check "a read asked once after that gives the newest value, or a word"
           true (or (and (:accepted? wrote) (= "after" (get-in once-after [:place :value]))) (contains? once-after :could-not)
                    (not (:accepted? wrote))))
    (shown "the module was started again under an open read, by Rama's module update with the unchanged module: a read asked once straight after gave"
           (if (:could-not once) (pr-str (select-keys once [:could-not])) "an answer")
           "| the open read" (if (= :got-the-change outcome) "got the change of an act that was sent after it" (str "ended with " (pr-str (:ended outcome))))
           "| a read asked once after that gave" (if (:could-not once-after) (pr-str (select-keys once-after [:could-not])) (pr-str (get-in once-after [:place :value]))))))

(deftest a-batch-that-runs-a-second-time
  (let [system-own (perm/first-permission :system :base)
        _ (make! :base :system)
        ;; a lane of alice's on another task than the base's, to read the base through
        base-task (task-of :base)
        elsewhere (loop [i 0]
                    (let [lane (keyword (str "elsewhere-" i))]
                      (make! lane (person :alice))
                      (cond (let [task (task-of lane)] (and (some? task) (not= base-task task))) lane
                            (< i 40) (recur (inc i)))))
        before (put! :system :base [(fact :twice :text "before")])
        through-elsewhere {:asks [:place :twice :text] :asker (person :alice) :lanes [elsewhere :base]}
        _ (wait-until 30000 #(= "before" (get-in (ask through-elsewhere) [:place :value])))
        read (live {:asks [:place :twice :text] :asker :system :lanes [:base]})
        read-elsewhere (live through-elsewhere)
        ipc (:ipc @round)
        m (:module @round)
        place-key (rows/place-key :base :twice :text)
        send (fn [facts] (door/send-act (the-door) {:version 1 :who :system :lane :base :permission (perm/permission-id system-own)
                                                    :session :read-path-proof :facts facts}))
        answer (fn [sent] (wait-until 90000 #(let [a (door/answer-of (the-door) (:act-id sent) (:digest sent))]
                                               (when (= :accepted (:answer a)) a))))
        notes-before (count @attempts)
        ;; one batch takes both acts, in the depot's order; the decision of the second throws once, after
        ;; the first act's rows were written on the base's task and sent to every task
        _ (rtest/pause-microbatch-topology! ipc m "microbatch-gate")
        twice (send [(fact :twice :text "written by a batch that ran twice")])
        other (send [(fact :twice-other :text "the act whose decision throws once")])
        _ (reset! throw-at (rows/place-key :base :twice-other :text))
        _ (rtest/resume-microbatch-topology! ipc m "microbatch-gate")
        twice-answer (answer twice)
        other-answer (answer other)
        got (wait-until 60000 #(and (seq (later read)) (seq (later read-elsewhere))))
        ;; a second act at the same place, so that a second call for the first could not be on its way
        after (send [(fact :twice :text "after")])
        after-answer (answer after)
        _ (wait-until 60000 #(and (= "after" (get-in (screen read) [:place :value]))
                                  (= "after" (get-in (screen read-elsewhere) [:place :value]))))
        _ (settled)
        all (later read)
        all-elsewhere (later read-elsewhere)
        notes (subvec @attempts notes-before)
        rows-at-the-place (fn [note] (mapv second (filter (fn [[k]] (= place-key k)) note)))
        with-a-row (filterv (comp seq rows-at-the-place) notes)
        first-row (last (rows-at-the-place (first with-a-row)))
        second-row (last (rows-at-the-place (second with-a-row)))
        of-the-batch (filterv (fn [note] (some #(rows/same-row? first-row %) (rows-at-the-place note))) with-a-row)
        told [{:changed {[:twice :text] {:holds :value :value "written by a batch that ran twice" :lane :base
                                         :fact [(:act-id twice) 0] :stamp (:stamp first-row)}}}
              {:changed {[:twice :text] {:holds :value :value "after" :lane :base
                                         :fact [(:act-id after) 0] :stamp (:stamp after-answer)}}}]
        _ (run! leave! [read read-elsewhere])]
    (check "the proof found a lane of alice's on another task than the base's, by selects of its own" [true true] [(some? base-task) (some? elsewhere)])
    (check "the proof made one throw, in the decision of the act that came after in the batch; the act that wrote the place was decided twice, each time with a row for the place; the third decision with a row for the place is that of the act that was sent after"
           [1 2 3 true] [@throws-made (count of-the-batch) (count with-a-row) (some? other-answer)])
    (check "the row that the second attempt made for the place is the first attempt's row, in every field, the stamp too"
           [true true] [(some? first-row) (rows/same-row? first-row second-row)])
    (check "under the open read from the base itself the function was called once for that place, with the first attempt's row: the stamp of the row that the first attempt made, and the fact of the act; the next call is of the act that was sent after"
           [true told]
           [(and (some? got) (some? twice-answer) (= (:stamp twice-answer) (:stamp first-row)) (< (:stamp before) (:stamp first-row)))
            all])
    (check "under the open read through a lane on another task, where the first attempt's copy of that row was sent: the same two calls and no other"
           told all-elsewhere)
    (shown "a batch of the base ran a second time under two open reads of a place that it writes, on a cluster of its own in which the proof replaced one pure function of the gates' walk and threw once, in the decision of a later act of the batch:"
           (count of-the-batch) "decisions made a row for the place, the same row in every field; the function of the read from the base itself was called" (count all)
           "times in all, and that of the read through the lane" elsewhere "on task" (task-of elsewhere) "(the base is on task" (str base-task ")") (count all-elsewhere)
           "times: once for that act with the stamp" (:stamp first-row) "of the first attempt's row, and once for the act after it")))

;; ------------------------------------------------- a lane's acts by stamp

(defn- raw!
  "Appends a thing straight to the streaming depot, with no door, as any
  sender that can reach Rama's ports can, and waits for the streaming gate
  (the ack level :ack). Takes the thing."
  [thing]
  (rama/foreign-append! (rama/foreign-depot (:ipc @round) (:module @round) "*streaming-depot") thing :ack))

(defn- acts-read
  "Reads a lane's acts once: from a stamp, at most a count, by an asker, an
  id or nil for one who is not signed in, and with a session when one is
  given; through the read path of most checks, or through the one given."
  ([lane start most asker] (acts-read (the-rp) lane start most asker nil))
  ([rp lane start most asker session]
   (ask rp (cond-> {:asks [:acts start most] :from-lane lane}
             asker (assoc :asker asker)
             (nil? asker) (assoc :not-signed-in true)
             session (assoc :session session)))))

(defn- given-of
  "Takes an answer of a read of acts. Gives each decision given as [stamp
  what]: `:withheld`, `:whole`, or the word of `:no-thing`."
  [answer]
  (mapv (fn [d] [(:stamp d) (cond (contains? d :thing) :whole
                                  (contains? d :no-thing) (:no-thing d)
                                  :else :withheld)])
        (:acts answer)))

(defn- thing-of-act
  "Takes what `act!` gave for an act that the door sent. Gives the thing as
  it arrived on the depot: the act as the proof made it, with the door's
  two additions."
  [got who lane facts]
  (assoc {:version 1 :who who :lane lane :permission (perm/permission-id (perm who lane)) :session :read-path-proof :facts facts}
         :act-id (:act-id got) :door-time (:time (:answer got))))

(defn- every-page
  "Reads a lane's acts from stamp 0 in pages of a count, each from where
  the last ended, until a page begins past the last stamp. Gives the pages."
  [rp lane most asker]
  (loop [start 0 pages []]
    (let [a (acts-read rp lane start most asker nil)]
      (if (or (:could-not a) (> start (long (:last-stamp a))) (< 40 (count pages)))
        (conj pages a)
        (recur (:next a) (conj pages a))))))

(deftest a-lanes-acts-by-stamp
  (let [lane :acts-lab
        _ (make! lane (person :ann))
        _ (give! lane (person :ann) (person :bo))
        note [(fact :note :n 1)]
        taken (put! (person :ann) lane note)
        cy-sent (door/send-act (the-door) {:version 1 :who (person :cy) :lane lane
                                           :permission (perm/permission-id (perm/first-permission (person :cy) lane))
                                           :session :read-path-proof :facts [(fact :note :n 2)]})
        cy-answer (door/answer-of (the-door) (:act-id cy-sent) (:digest cy-sent))
        no-time {:act-id [lane (UUID. 51 1)] :version 1 :who (person :cy) :lane lane
                 :facts [(fact :x :n 3)]}
        _ (raw! no-time)
        no-who {:act-id [lane (UUID. 51 2)] :door-time 1 :lane lane}
        _ (raw! no-who)
        heavy {:act-id [lane (UUID. 51 3)] :version 1 :who (person :cy) :lane lane
               :blob (apply str (repeat 300000 "h"))}
        _ (raw! heavy)
        bo-facts [(fact :note :n 4)]
        bo-act (put! (person :bo) lane bo-facts)
        ann (acts-read lane 0 100 (person :ann))
        bo (acts-read lane 0 100 (person :bo))
        cy (acts-read lane 0 100 (person :cy))
        nobody (acts-read lane 0 100 nil)
        all-whole [[0 :whole] [1 :whole] [2 :whole] [3 :whole] [4 :whole] [5 :whole] [6 :too-heavy] [7 :whole]]
        entry (fn [answer stamp] (first (filter #(= stamp (:stamp %)) (:acts answer))))]
    (check "a holder of a lane, its first or one given beneath her, reads each of its decisions whole: taken acts, an act refused for its standing, things that the declaration refused, a thing that names no who, and a thing too heavy to keep, with its word; and the last stamp"
           [all-whole 7 8 [] all-whole]
           [(given-of ann) (:last-stamp ann) (:next ann) (:left-out ann) (given-of bo)])
    (check "a decision given whole holds the door's answer and the thing as it arrived on the depot: a taken act, and an act refused for its standing, each with its act-id"
           [[(:act-id taken) {:verdict :accepted :reason nil :stamp 2 :time (:time (:answer taken)) :digest (:digest (:sent taken))}
             (thing-of-act taken (person :ann) lane note)]
            [(:act-id cy-sent) :refused (:reason cy-answer) 3]]
           [((juxt :act-id :answer :thing) (entry ann 2))
            (let [e (entry ann 3)] [(:act-id e) (:verdict (:answer e)) (:reason (:answer e)) (:stamp (:answer e))])])
    (check "a thing that the declaration refused is given as it arrived, its who read from the raw thing; one that names no who is the holders' alone"
           [no-time :refused no-who]
           [(:thing (entry ann 4)) (:verdict (:answer (entry ann 4))) (:thing (entry ann 5))])
    (check "an asker who may not see a lane is given her own decisions there, taken or refused, and nothing else: no stamp of another's, no last stamp; the lane is left out. One who is not signed in is given nothing"
           [[[3 :whole] [4 :whole] [6 :too-heavy]] false [{:lane lane :why :may-not-see}] 7
            [] [{:lane lane :why :may-not-see}]]
           [(given-of cy) (contains? cy :last-stamp) (:left-out cy) (:next cy) (given-of nobody) (:left-out nobody)])
    (let [_ (setting! lane (person :ann) :everyone)
          cy-open (acts-read lane 0 100 (person :cy))
          nobody-open (acts-read lane 0 100 nil)
          _ (put! (person :ann) lane [(fact lane :softland.store/who-may-read-acts :everyone)])
          cy-read (acts-read lane 0 100 (person :cy))
          nobody-read (acts-read lane 0 100 nil)
          _ (put! (person :ann) lane [(fact lane :softland.store/who-may-read-acts :members)])
          cy-again (acts-read lane 0 100 (person :cy))
          _ (put! (person :ann) lane [(perm/end-fact (perm (person :bo) lane))])
          bo-visitor (acts-read lane 0 100 (person :bo))
          _ (setting! lane (person :ann) :members)
          bo-outside (acts-read lane 0 100 (person :bo))]
      (check "in a lane that everyone may see, while its acts setting is members, a visitor reads her own decisions whole and every other one as its stamp alone, with the last stamp; one who is not signed in reads each as its stamp alone"
             [[[0 :withheld] [1 :withheld] [2 :withheld] [3 :whole] [4 :whole] [5 :withheld] [6 :too-heavy] [7 :withheld] [8 :withheld]]
              8 #{#{:stamp}} (vec (for [s (range 9)] [s :withheld]))]
             [(given-of cy-open) (:last-stamp cy-open)
              (set (map (comp set keys) (remove #(contains? % :act-id) (:acts cy-open))))
              (given-of nobody-open)])
      (check "the lane's first holder turns its acts setting to everyone: whoever may see the lane reads its acts whole, one who is not signed in too; she turns it back, and a visitor's read is withheld again"
             [(conj all-whole [8 :whole] [9 :whole]) (conj all-whole [8 :whole] [9 :whole])
              [[0 :withheld] [3 :whole] [4 :whole] [6 :too-heavy] [10 :withheld]]]
             [(given-of cy-read) (given-of nobody-read)
              (filterv (fn [[s]] (#{0 3 4 6 10} s)) (given-of cy-again))])
      (check "a holder whose permission ended reads the lane's acts as anyone who is no holder: in a lane that everyone may see, his own whole and the rest withheld; once it is open to its members, his own alone, and the lane left out"
             [[[0 :withheld] [7 :whole]] [[[7 :whole]] [{:lane lane :why :may-not-see}]]]
             [(filterv (fn [[s]] (#{0 7} s)) (given-of bo-visitor))
              [(given-of bo-outside) (:left-out bo-outside)]]))
    (let [counted (counting #(acts-read lane 0 100 (person :ann)))
          bounded (read-path/open (:ipc @round) (:module @round) {:max-acts-of-one-read 3 :max-thing-weight-of-one-read 1})
          pages (every-page bounded lane 100 (person :ann))
          small (read-path/open (:ipc @round) (:module @round)
                                {:max-thing-weight-of-one-read (+ (:weight (digest/weighed (:thing (entry ann 0)) 100000))
                                                                  (:weight (digest/weighed (:thing (entry ann 1)) 100000)))})
          two (acts-read small lane 0 100 (person :ann) nil)]
      (check "a read of a holder's acts is four select rounds of one call each, and one query of the rights: the last stamp, the own rows and rights, the line, the things"
             [[1 1 1 1] 1] [(:rounds counted) (:queries counted)])
      (check "a read gives `most` decisions at most, cut to the read path's number, and the pages joined from stamp 0 give each decision once, in order; with a bound on weight that no thing fits in, each kept thing is given as heavier than the read, and the line goes on past it"
             [true (vec (range 13)) #{:heavier-than-the-read :too-heavy}]
             [(every? #(<= (count (:acts %)) 3) pages)
              (vec (mapcat #(map :stamp (:acts %)) pages))
              (set (map :no-thing (mapcat :acts pages)))])
      (check "the things of one answer weigh no more than the read's bound: with the weight of the first two, a read gives those two and ends before the third, where the next one begins"
             [[[0 :whole] [1 :whole]] 2]
             [(given-of two) (:next two)]))
    (check "a read of acts is asked once: kept live it is turned away, with no call to Rama; a read of acts of no count is a query that is turned away"
           [:keep-a-read-of-acts-live [:read-the-query :acts-not-named]]
           [(:could-not (read-path/read-live (the-rp) {:asks [:acts 0 5] :asker (person :ann) :from-lane lane} (fn [_])))
            ((juxt :could-not :why) (ask {:asks [:acts 0 0] :asker (person :ann) :from-lane lane}))])
    (let [_ (dotimes [i 3] (put! :system :base [(fact :acts-of-the-base :n i)]))
          first-read (acts-read :base 0 1 :system)
          last-stamp (:last-stamp first-read)
          from (max 0 (- (long last-stamp) 2))
          noted (noting-routing-ids (the-rp) #(acts-read :base from 10 :system))
          cy-base (acts-read :base from 10 (person :cy))]
      (check "the base's acts: its holder reads its last decisions whole, and each call of the read is made on the base's own task, in the tables of the microbatch gate"
             [(vec (for [s (range from (inc (long last-stamp)))] [s :whole])) #{[:microbatch :base]}]
             [(given-of (:result noted)) (set (map (juxt first last) (:selects noted)))])
      (check "a visitor of the base, which everyone may see, reads its acts as its stamps alone while the base's acts setting is members, as every lane's starts"
             (vec (for [s (range from (inc (long last-stamp)))] [s :withheld]))
             (given-of cy-base)))
    (let [wide (String. (Character/toChars 0x1D49C))
          far (keyword (str "acts" wide))
          far-who (person (keyword (str "cy" "ﬀ")))
          _ (make! far (person :ann))
          _ (put! (person :ann) far [(fact :note :n 1)])
          sent (door/send-act (the-door) {:version 1 :who far-who :lane far
                                          :permission (perm/permission-id (perm/first-permission far-who far))
                                          :session :read-path-proof :facts [(fact :note :n 2)]})
          _ (door/answer-of (the-door) (:act-id sent) (:digest sent))
          _ (put! (person :ann) far [(fact :note :n 3)])
          holder-pages (every-page (the-rp) far 1 (person :ann))
          own (acts-read far 0 100 far-who)
          prefix (acts-read :pfx 0 100 (person :alice))]
      (check "a lane whose id holds a letter outside the first plane, and a who whose id holds one from U+E000 on: a holder's pages of one give each decision once, and the who reads her own decision there and nothing else"
             [[0 1 2 3] [[2 :whole]] (:act-id sent)]
             [(vec (mapcat #(map :stamp (:acts %)) holder-pages)) (given-of own) (:act-id (first (:acts own)))])
      (check "a lane whose text is the start of another's on the same task: the read of its line gives its own decisions, each stamp once from 0 to its last, and no thing of the other's"
             [true #{:pfx}]
             [(= (mapv :stamp (:acts prefix)) (vec (range (inc (long (:last-stamp prefix))))))
              (set (map (comp :lane :thing) (:acts prefix)))]))
    (shown "a lane's acts by stamp, read by each kind of asker: a holder" (pr-str (given-of ann))
           "| an asker who may not see the lane" (pr-str (given-of cy)) "| one who is not signed in" (pr-str (given-of nobody)))))

;; -------------------------------------- the permission fix: batches 1 to 4

(deftest the-pushes-a-live-viewer-holds
  (let [rp (read-path/open (:ipc @round) (:module @round))
        sources-count (fn [] (count (:sources (first (vals (:reads (held rp)))))))
        ;; (i) a private lane under one standing-on-its-own member permission
        _ (make! :b1-private-member (person :alice))
        _ (put! (person :alice) :b1-private-member [(fact :doc :title "private")])
        p-bob (perm/given-permission (person :bob) :b1-private-member (perm (person :alice) :b1-private-member)
                                     {:allows :see-and-act :lent-from nil :salt (swap! give-salt inc)})
        _ (put! (person :alice) :b1-private-member [(perm/permission-fact p-bob)])
        v-member (live rp {:asks [:place :doc :title] :asker (person :bob) :lanes [:b1-private-member]})
        member-open [(read-path/open-count rp) (sources-count) (:counts (held rp))]
        member-left [(leave! v-member) (read-path/open-count rp) (held rp)]
        ;; (ii) a private lane where an agent holds a lent permission
        _ (make! :b1-private-agent (person :alice))
        _ (put! (person :alice) :b1-private-agent [(fact :doc :title "for-agent")])
        p-bot (perm/given-permission (agent :bot) :b1-private-agent (perm (person :alice) :b1-private-agent)
                                     {:allows :see :salt (swap! give-salt inc)})
        _ (put! (person :alice) :b1-private-agent [(perm/permission-fact p-bot)])
        v-agent (live rp {:asks [:place :doc :title] :asker (agent :bot) :lanes [:b1-private-agent]})
        agent-open [(read-path/open-count rp) (sources-count) (:counts (held rp))]
        agent-left [(leave! v-agent) (read-path/open-count rp) (held rp)]
        ;; (iii) an open lane where :anyone holds :see
        _ (make! :b1-open (person :alice) {:open? true})
        _ (put! (person :alice) :b1-open [(fact :doc :title "open")])
        v-open-1 (live rp {:asks [:place :doc :title] :asker (person :carol) :lanes [:b1-open]})
        open-one [(read-path/open-count rp) (sources-count) (:counts (held rp))]
        v-open-2 (live rp {:asks [:place :doc :title] :asker (person :dave) :lanes [:b1-open]})
        open-two (read-path/open-count rp)
        open-left [(leave! v-open-1) (leave! v-open-2) (read-path/open-count rp) (held rp)]]
    (check "one live viewer on a private lane under one member permission holds 6 pushes: 3 on the lane's facts (own rows, her permission, the place) and 3 on its permissions by holder (:anyone, :anyone-signed-in, her own); leaving empties all"
           [[{:live-reads 1 :pushes 6} 6
             {[:streaming :facts :b1-private-member] 3 [:streaming :permissions-by-holder :b1-private-member] 3}]
            [:left {:live-reads 0 :pushes 0} {:pushes {} :counts {} :reads {}}]]
           [member-open member-left])
    (check "one live agent viewer on a private lane under a lent permission holds 5 pushes: 4 on the lane's facts (own rows, the agent's permission, its lender's permission, the place) and 1 on its permissions by holder (the agent's own); leaving empties all"
           [[{:live-reads 1 :pushes 5} 5
             {[:streaming :facts :b1-private-agent] 4 [:streaming :permissions-by-holder :b1-private-agent] 1}]
            [:left {:live-reads 0 :pushes 0} {:pushes {} :counts {} :reads {}}]]
           [agent-open agent-left])
    (check "one live viewer on an open lane (:anyone holds :see) holds 4 pushes: 3 on the lane's facts (own rows, everyone's permission, the place) and 1 on its permissions by holder (:anyone); a second viewer shares all 4; leaving empties all"
           [[{:live-reads 1 :pushes 4} 4
             {[:streaming :facts :b1-open] 3 [:streaming :permissions-by-holder :b1-open] 1}]
            {:live-reads 2 :pushes 4}
            [:left :left {:live-reads 0 :pushes 0} {:pushes {} :counts {} :reads {}}]]
           [open-one open-two open-left])
    (shown "the pushes held by one live viewer, measured on a read path of its own: private member"
           (pr-str (first member-open)) "by-table" (pr-str (nth member-open 2))
           "| private lent agent" (pr-str (first agent-open)) "by-table" (pr-str (nth agent-open 2))
           "| open lane (:anyone)" (pr-str (first open-one)) "by-table" (pr-str (nth open-one 2))
           "(two viewers:" (pr-str open-two) ")"
           "| after leave:" (pr-str (second member-left)))))

(deftest a-transfer-in-one-act-never-shows-a-state-in-between
  (let [rp (the-rp)
        perm-key declaration/permission-key
        watches-perm? (fn [home lane perm-obj]
                        (let [src (sources/permission-row-source
                                   home lane (rows/place-key lane (perm/permission-id perm-obj) perm-key))]
                          (contains? (:pushes (held rp)) src)))
        no-flicker? (fn [changes-vec place expected-values]
                      (and (seq changes-vec)
                           (every? (fn [c]
                                     (and (nil? (:ended c))
                                          (cond
                                            (:anew c)
                                            (let [a (:anew c)]
                                              (and (empty? (:left-out a))
                                                   (= :value (:holds (:place a)))
                                                   (contains? expected-values (:value (:place a)))))

                                            (:changed c)
                                            (let [pa (get (:changed c) place)]
                                              (and (some? pa)
                                                   (= :value (:holds pa))
                                                   (contains? expected-values (:value pa))))

                                            :else false)))
                                   changes-vec)))
        ;; 1. Streaming lane: 1-act agent borrow transfers, 1-act person permission transfers, and 1-act owner hand-over
        _ (make! :xfer-stream (person :alice))
        alice-own (perm (person :alice) :xfer-stream)
        _ (put! (person :alice) :xfer-stream [(fact :doc :text "s0")])
        p-vera-1 (perm/given-permission (person :vera) :xfer-stream alice-own
                                        {:allows :see-and-act :lent-from nil :salt (swap! give-salt inc)})
        p-bot-1 (perm/given-permission (agent :vera-bot) :xfer-stream p-vera-1
                                       {:allows :see :salt (swap! give-salt inc)})
        _ (put! (person :alice) :xfer-stream [(perm/permission-fact p-vera-1)])
        _ (put! {:version 1 :who (person :vera) :lane :xfer-stream :permission (perm/permission-id p-vera-1)
                 :session :xfer :facts [(perm/permission-fact p-bot-1)]})
        vera-stream (live {:asks [:place :doc :text] :asker (person :vera) :lanes [:xfer-stream]})
        bot-stream (live {:asks [:place :doc :text] :asker (agent :vera-bot) :lanes [:xfer-stream]})
        ;; 5 back-to-back 1-act borrow transfers for the agent viewer under p-vera-1
        p-bot-final (reduce (fn [prev idx]
                              (let [nxt (perm/given-permission (agent :vera-bot) :xfer-stream p-vera-1
                                                               {:allows (if (even? idx) :see-and-act :see)
                                                                :salt (swap! give-salt inc)})]
                                (put! {:version 1 :who (person :vera) :lane :xfer-stream
                                       :permission (perm/permission-id p-vera-1)
                                       :session :xfer
                                       :facts [(perm/end-fact prev) (perm/permission-fact nxt)]})
                                (when (zero? idx)
                                  (wait-until #(watches-perm? :xfer-stream :xfer-stream nxt)))
                                nxt))
                            p-bot-1
                            (range 5))
        _ (wait-until #(watches-perm? :xfer-stream :xfer-stream p-bot-final))
        _ (put! (person :alice) :xfer-stream [(fact :doc :text "s1")])
        _ (wait-until #(and (= "s1" (get-in (screen vera-stream) [:place :value]))
                            (= "s1" (get-in (screen bot-stream) [:place :value]))))
        bot-stream-changes (changes bot-stream)
        _ (leave! bot-stream)
        ;; 5 1-act permission transfers for the person viewer (first waited, then 4 rapid back-to-back)
        p-vera-final (reduce (fn [prev idx]
                               (let [nxt (perm/given-permission (person :vera) :xfer-stream alice-own
                                                                {:allows (if (odd? idx) :see-and-act :see)
                                                                 :lent-from nil
                                                                 :salt (swap! give-salt inc)})]
                                 (put! (person :alice) :xfer-stream [(perm/end-fact prev) (perm/permission-fact nxt)])
                                 (when (zero? idx)
                                   (wait-until #(watches-perm? :xfer-stream :xfer-stream nxt)))
                                 nxt))
                             p-vera-1
                             (range 5))
        _ (wait-until #(watches-perm? :xfer-stream :xfer-stream p-vera-final))
        _ (put! (person :alice) :xfer-stream [(fact :doc :text "s2")])
        _ (wait-until #(= "s2" (get-in (screen vera-stream) [:place :value])))
        _ (settled)
        vera-stream-changes (changes vera-stream)
        _ (leave! vera-stream)
        ;; 1-act whole-lane owner hand-over while the new owner holds a live view
        _ (make! :xfer-handover (person :alice))
        handover-first (perm (person :alice) :xfer-handover)
        _ (put! (person :alice) :xfer-handover [(fact :doc :text "h0")])
        p-bob-see (perm/given-permission (person :bob) :xfer-handover handover-first
                                         {:allows :see :lent-from nil :salt (swap! give-salt inc)})
        _ (put! (person :alice) :xfer-handover [(perm/permission-fact p-bob-see)])
        bob-handover (live {:asks [:place :doc :text] :asker (person :bob) :lanes [:xfer-handover]})
        p-bob-owner (perm/given-permission (person :bob) :xfer-handover handover-first
                                           {:allows :owner :lent-from nil :salt (swap! give-salt inc)})
        _ (put! (person :alice) :xfer-handover [(perm/permission-fact p-bob-owner)
                                                (perm/end-fact p-bob-see)
                                                (perm/end-fact handover-first)])
        _ (wait-until #(watches-perm? :xfer-handover :xfer-handover p-bob-owner))
        alice-after-handover (act! (person :alice) :xfer-handover [(fact :doc :text "refused")])
        _ (put! {:version 1 :who (person :bob) :lane :xfer-handover :permission (perm/permission-id p-bob-owner)
                 :session :xfer :facts [(fact :doc :text "h1")]})
        _ (wait-until #(= "h1" (get-in (screen bob-handover) [:place :value])))
        _ (settled)
        handover-changes (changes bob-handover)
        _ (leave! bob-handover)
        ;; 2. Microbatch (:base): read directly [:base] AND via [:viewer-home :base] on another Rama task
        base-task (task-of :base)
        viewer-home (loop [i 0]
                      (let [lane (keyword (str "viewer-home-" i))]
                        (make! lane (person :vera))
                        (cond (let [t (task-of lane)] (and (some? t) (not= base-task t))) lane
                              (< i 60) (recur (inc i)))))
        home-task (task-of viewer-home)
        p-home-bot (perm/given-permission (agent :vera-bot) viewer-home (perm (person :vera) viewer-home)
                                          {:allows :see :salt (swap! give-salt inc)})
        _ (put! (person :vera) viewer-home [(perm/permission-fact p-home-bot)])
        _ (put! :system :base [(fact :xfer-base-doc :text "b0")])
        _ (wait-until 30000 #(= "b0" (get-in (ask {:asks [:place :xfer-base-doc :text]
                                                   :asker (person :vera) :lanes [viewer-home :base]})
                                             [:place :value])))
        base-outcome
        (try
          (let [sys-own (perm :system :base)
                p-vera-b1 (perm/given-permission (person :vera) :base sys-own
                                                 {:allows :see-and-act :lent-from nil :salt (swap! give-salt inc)})
                p-bot-b1 (perm/given-permission (agent :vera-bot) :base p-vera-b1
                                                {:allows :see :salt (swap! give-salt inc)})
                _ (put! :system :base [(perm/permission-fact p-vera-b1)])
                _ (put! {:version 1 :who (person :vera) :lane :base :permission (perm/permission-id p-vera-b1)
                         :session :xfer :facts [(perm/permission-fact p-bot-b1)]})
                ;; Close :base to :anyone so standing genuinely comes from p-vera-b* / p-bot-b*
                _ (setting! :base :system :members)
                _ (wait-until 30000
                              #(and (= [{:lane :base :why :may-not-see}]
                                       (:left-out (ask {:asks [:place :xfer-base-doc :text]
                                                        :asker (person :outsider-xfer) :lanes [:base]})))
                                    (= [{:lane viewer-home :why :may-not-see} {:lane :base :why :may-not-see}]
                                       (:left-out (ask {:asks [:place :xfer-base-doc :text]
                                                        :asker (person :outsider-xfer) :lanes [viewer-home :base]})))))
                direct-vera (live {:asks [:place :xfer-base-doc :text] :asker (person :vera) :lanes [:base]})
                home-vera (live {:asks [:place :xfer-base-doc :text] :asker (person :vera) :lanes [viewer-home :base]})
                home-bot (live {:asks [:place :xfer-base-doc :text] :asker (agent :vera-bot) :lanes [viewer-home :base]})
                ;; 1-act borrow swap in :base for the cross-task agent viewer
                p-bot-b2 (perm/given-permission (agent :vera-bot) :base p-vera-b1
                                                {:allows :see-and-act :salt (swap! give-salt inc)})
                _ (put! {:version 1 :who (person :vera) :lane :base :permission (perm/permission-id p-vera-b1)
                         :session :xfer :facts [(perm/end-fact p-bot-b1) (perm/permission-fact p-bot-b2)]})
                _ (wait-until 30000 #(watches-perm? viewer-home :base p-bot-b2))
                _ (put! :system :base [(fact :xfer-base-doc :text "b1")])
                _ (wait-until 30000 #(every? (fn [v] (= "b1" (get-in (screen v) [:place :value])))
                                             [direct-vera home-vera home-bot]))
                home-bot-changes (changes home-bot)
                _ (leave! home-bot)
                ;; 1-act permission swaps in :base for both direct [:base] and cross-task [viewer-home :base]
                p-vera-b2 (perm/given-permission (person :vera) :base sys-own
                                                 {:allows :see :lent-from nil :salt (swap! give-salt inc)})
                _ (put! :system :base [(perm/end-fact p-vera-b1) (perm/permission-fact p-vera-b2)])
                _ (wait-until 30000 #(and (watches-perm? :base :base p-vera-b2)
                                          (watches-perm? viewer-home :base p-vera-b2)))
                p-vera-b3 (perm/given-permission (person :vera) :base sys-own
                                                 {:allows :see-and-act :lent-from nil :salt (swap! give-salt inc)})
                p-vera-b4 (perm/given-permission (person :vera) :base sys-own
                                                 {:allows :see :lent-from nil :salt (swap! give-salt inc)})
                _ (put! :system :base [(perm/end-fact p-vera-b2) (perm/permission-fact p-vera-b3)])
                _ (put! :system :base [(perm/end-fact p-vera-b3) (perm/permission-fact p-vera-b4)])
                _ (wait-until 30000 #(and (watches-perm? :base :base p-vera-b4)
                                          (watches-perm? viewer-home :base p-vera-b4)))
                outsider-still-out (:left-out (ask {:asks [:place :xfer-base-doc :text]
                                                    :asker (person :outsider-xfer) :lanes [:base]}))
                _ (put! :system :base [(fact :xfer-base-doc :text "b2")])
                _ (wait-until 30000 #(and (= "b2" (get-in (screen direct-vera) [:place :value]))
                                          (= "b2" (get-in (screen home-vera) [:place :value]))))
                _ (settled)
                direct-changes (changes direct-vera)
                home-changes (changes home-vera)]
            (run! leave! [direct-vera home-vera])
            {:outsider outsider-still-out
             :direct direct-changes
             :home home-changes
             :bot home-bot-changes
             :direct-final (get-in (screen direct-vera) [:place :value])
             :home-final (get-in (screen home-vera) [:place :value])
             :bot-final (get-in (screen home-bot) [:place :value])})
          (finally
            (when-not (perm :anyone :base)
              (setting! :base :system :everyone))
            (wait-until 30000 #(= "b2" (get-in (ask {:asks [:place :xfer-base-doc :text]
                                                     :not-signed-in true :lanes [:base]})
                                               [:place :value])))))]
    (check "on a streaming lane, 1-act permission transfers for a person and for a lent agent never show :may-not-see or an intermediate state, and a write after the transfers arrives cleanly on the same open view"
           [true "s2" true "s1"]
           [(no-flicker? vera-stream-changes [:doc :text] #{"s0" "s1" "s2"})
            (get-in (last vera-stream-changes) [:changed [:doc :text] :value])
            (no-flicker? bot-stream-changes [:doc :text] #{"s0" "s1"})
            (get-in (last bot-stream-changes) [:changed [:doc :text] :value])])
    (check "a 1-act owner hand-over that grants the new owner, ends his old see-only permission and ends the first owner's permission never shows him a state in between, refuses the former owner afterwards, and delivers the new owner's write"
           [true :permission-ended "h1"]
           [(no-flicker? handover-changes [:doc :text] #{"h0" "h1"})
            (:reason (:answer alice-after-handover))
            (get-in (last handover-changes) [:changed [:doc :text] :value])])
    (check "on the base, closed to :anyone and read both directly on the base's task and through a home lane on another task, 1-act transfers for a person and for an agent never show :may-not-see or an intermediate state, and subsequent writes arrive cleanly"
           [true [{:lane :base :why :may-not-see}]
            true "b2"
            true "b2"
            true "b1"]
           [(and (some? base-task) (some? home-task) (not= base-task home-task))
            (:outsider base-outcome)
            (no-flicker? (:direct base-outcome) [:xfer-base-doc :text] #{"b0" "b1" "b2"})
            (:direct-final base-outcome)
            (no-flicker? (:home base-outcome) [:xfer-base-doc :text] #{"b0" "b1" "b2"})
            (:home-final base-outcome)
            (no-flicker? (:bot base-outcome) [:xfer-base-doc :text] #{"b0" "b1"})
            (:bot-final base-outcome)])
    (shown "a 1-act transfer never shows a live viewer a state in between: streaming person calls" (count vera-stream-changes)
           "agent calls" (count bot-stream-changes) "owner hand-over calls" (count handover-changes)
           "| base (task" base-task "and via" viewer-home "on task" (str home-task ")")
           "direct calls" (count (:direct base-outcome)) "cross-task calls" (count (:home base-outcome))
           "cross-task agent calls" (count (:bot base-outcome)))))

(deftest the-statement-to-prove-with-its-named-exceptions
  (let [lane :the-statement
        perm-key declaration/permission-key
        send-act! (fn [an-act]
                    (let [sent (door/send-act (the-door) an-act)]
                      {:sent sent :answer (door/answer-of (the-door) (:act-id sent) (:digest sent))}))
        reason-of (fn [got] (:reason (:answer got)))
        ;; Build a multi-party history exercising making, gives, lends, sitting scope, exact-act yes,
        ;; public :anyone-signed-in, in-place lowering/capping, own-end with nil :permission,
        ;; owner hand-over, and refused acts across failure modes.
        _ (make! lane (person :alice))
        p-alice (perm (person :alice) lane)
        _ (mark! lane (person :alice) #{:tag})
        _ (put! (person :alice) lane [(fact :doc-1 :title "by-alice") (fact :doc-1 :tag :featured)])
        p-bob (perm/given-permission (person :bob) lane p-alice
                                     {:allows :publisher :lent-from nil :salt (swap! give-salt inc)})
        p-carol (perm/given-permission (person :carol) lane p-alice
                                       {:allows :see :lent-from nil :salt (swap! give-salt inc)})
        p-wren (perm/given-permission (person :wren) lane p-alice
                                      {:allows {:write :all} :lent-from nil :salt (swap! give-salt inc)})
        p-sit (perm/run-permission (person :sid-sit) lane p-alice :sit-1 :see-and-act {:lent-from nil :salt (swap! give-salt inc)})
        yan-act {:version 1 :who (person :yan) :lane lane :session :s-yan :facts [(fact :doc-yan :title "by-yes")]}
        y-id (UUID/randomUUID)
        p-yes (perm/act-permission (person :yan) lane p-alice y-id (digest/hex-for-a-yes yan-act) {:lent-from nil})
        p-eve (perm/given-permission (person :eve) lane p-alice
                                     {:allows :see :lent-from nil :salt (swap! give-salt inc)})
        _ (put! (person :alice) lane (mapv perm/permission-fact [p-bob p-carol p-wren p-sit p-yes p-eve]))
        ;; Lends to agents and runs (from publisher Bob and from see-only Carol)
        p-bob-bot (perm/given-permission (agent :bob-bot) lane p-bob
                                         {:allows {:see true :write :all} :salt (swap! give-salt inc)})
        p-bob-run (perm/given-permission (run :bob-job) lane p-bob
                                         {:allows :see :salt (swap! give-salt inc)})
        _ (put! {:version 1 :who (person :bob) :lane lane :permission (perm/permission-id p-bob)
                 :session :s-bob :facts [(perm/permission-fact p-bob-bot) (perm/permission-fact p-bob-run)]})
        p-carol-bot (perm/given-permission (agent :carol-bot) lane p-carol
                                           {:allows :see :salt (swap! give-salt inc)})
        _ (put! {:version 1 :who (person :carol) :lane lane :permission (perm/permission-id p-carol)
                 :session :s-carol :facts [(perm/permission-fact p-carol-bot)]})
        ;; Accepted writes under lent agent, write-only holder, sitting permission, and exact-act yes
        _ (put! {:version 1 :who (agent :bob-bot) :lane lane :permission (perm/permission-id p-bob-bot)
                 :session :s-bot :facts [(fact :doc-bot :title "by-bob-bot")]})
        wren-wrote (put! {:version 1 :who (person :wren) :lane lane :permission (perm/permission-id p-wren)
                          :session :s-wren :facts [(fact :doc-wren :title "by-wren")]})
        _ (put! {:version 1 :who (person :sid-sit) :lane lane :permission (perm/permission-id p-sit)
                 :session :sit-1 :facts [(fact :doc-sit :title "by-sit")]})
        yan-taken (put! (assoc yan-act :permission (perm/permission-id p-yes)))
        ;; Public writing via :anyone-signed-in: person acts directly, agent is refused directly and enters via person's lend
        p-asi (perm/given-permission :anyone-signed-in lane p-bob
                                     {:allows {:write :all} :lent-from nil :salt (swap! give-salt inc)})
        _ (put! {:version 1 :who (person :bob) :lane lane :permission (perm/permission-id p-bob)
                 :session :s-bob :facts [(perm/permission-fact p-asi)]})
        zoe-wrote (put! {:version 1 :who (person :zoe) :lane lane :permission (perm/permission-id p-asi)
                         :session :s-zoe :facts [(fact :doc-zoe :title "by-zoe-via-everyone")]})
        zoe-bot-direct (send-act! {:version 1 :who (agent :zoe-bot) :lane lane :permission (perm/permission-id p-asi)
                                   :session :s-zbot :facts [(fact :doc-zbot :title "no-direct-everyone")]})
        p-zoe-bot (perm/given-permission (agent :zoe-bot) lane p-asi
                                         {:allows {:write :all} :given-by (person :zoe) :salt (swap! give-salt inc)})
        _ (put! {:version 1 :who (person :zoe) :lane lane :permission (perm/permission-id p-asi)
                 :session :s-zoe :facts [(perm/permission-fact p-zoe-bot)]})
        zoe-bot-wrote (put! {:version 1 :who (agent :zoe-bot) :lane lane :permission (perm/permission-id p-zoe-bot)
                             :session :s-zbot :facts [(fact :doc-zbot :title "by-zoe-bot-lent")]})
        _ (put! {:version 1 :who (person :bob) :lane lane :permission (perm/permission-id p-bob)
                 :session :s-bob :facts [(perm/end-fact p-asi)]})
        ;; Lower Bob in place to {:write :all}: caps p-bob-bot and p-bob-run out of :see without ending them
        p-bob-lowered (assoc-in p-bob [:value :allows] {:write :all})
        _ (put! (person :alice) lane [(perm/permission-fact p-bob-lowered)])
        ;; Clause 1c: Eve (see-only) ends her own give with :permission nil
        eve-own-end (put! {:version 1 :who (person :eve) :lane lane :session :s-eve
                           :facts [(perm/end-fact p-eve)]})
        ;; Refused acts across failure modes (recorded on the lane's line)
        ref-never (send-act! {:version 1 :who (person :outsider) :lane lane :permission (UUID/randomUUID)
                              :session :s-out :facts [(fact :bad :n 1)]})
        ref-ended (send-act! {:version 1 :who (person :eve) :lane lane :permission (perm/permission-id p-eve)
                              :session :s-eve :facts [(fact :bad :n 2)]})
        ref-used (send-act! (assoc yan-act :permission (perm/permission-id p-yes)))
        ref-sitting (send-act! {:version 1 :who (person :sid-sit) :lane lane :permission (perm/permission-id p-sit)
                                :session :sit-2 :facts [(fact :bad :n 3)]})
        ref-see-write (send-act! {:version 1 :who (person :carol) :lane lane :permission (perm/permission-id p-carol)
                                  :session :s-carol :facts [(fact :bad :n 4)]})
        ref-nil-perm (send-act! {:version 1 :who (person :outsider) :lane lane :session :s-out
                                 :facts [(perm/end-fact p-carol)]})
        ref-above (send-act! {:version 1 :who (agent :zoe-bot) :lane lane :permission (perm/permission-id p-zoe-bot)
                              :session :s-zbot :facts [(fact :bad :n 5)]})
        ref-no-owner (send-act! {:version 1 :who (person :alice) :lane lane :permission (perm/permission-id p-alice)
                                 :session :s-alice :facts [(perm/end-fact p-alice)]})
        ;; Hand over ownership from Alice to Dana in one act (ending first-permission p-alice)
        p-dana (perm/given-permission (person :dana) lane p-alice
                                      {:allows :owner :lent-from nil :salt (swap! give-salt inc)})
        _ (put! (person :alice) lane [(perm/permission-fact p-dana) (perm/end-fact p-alice)])
        ;; Read the complete log of decisions as owner Dana, and replay Clauses 1 & 3 mechanically
        full-log (acts-read lane 0 1000 (person :dana))
        audit
        (let [first-id (first-act/first-permission-id lane)
              row-map-entry (fn [id v]
                              [(chain/place lane id)
                               (when v
                                 (rows/fact-row {:e id :k perm-key :text (pr-str v)} (UUID. 0 1) 0 1 0 ":x" nil))])
              stands-and-allows (fn [rows-now perm-id]
                                  (let [{:keys [values whole?]} (chain/climb lane perm-id rows-now)]
                                    (when whole?
                                      {:says (first values) :allows (chain/allows-now values)})))]
          (reduce
           (fn [{:keys [rows-now created clause1-ok? clause3-ok?] :as st} {:keys [stamp answer thing]}]
             (if (= :refused (:verdict answer))
               st
               (let [parsed (:ok (declaration/check thing))
                     who (:who parsed)
                     pid (:permission parsed)
                     facts (:facts parsed)
                     making? (and (zero? stamp) (nil? pid) (first-act/making-act? :streaming parsed))
                     own-end? (and (nil? pid)
                                   (every? (fn [f]
                                             (and (= perm-key (:k f))
                                                  (nil? (:v f))
                                                  (= who (:holder (rows/held-permission (get rows-now (chain/place lane (:e f))))))))
                                           facts))
                     under-info (when pid (stands-and-allows rows-now pid))
                     held-before? (boolean
                                   (and under-info
                                        (chain/held-by? (:holder (:says under-info)) who)
                                        (or (nil? (:session (:says under-info)))
                                            (= (:session parsed) (:session (:says under-info))))
                                        (every? (fn [f]
                                                  (cond
                                                    (= perm-key (:k f)) true
                                                    (contains? declaration/second-write-settings (:k f))
                                                    (:owner (:allows under-info))
                                                    :else (:write (:allows under-info))))
                                                facts)))
                     c1-this? (or making? own-end? held-before?)
                     ;; Check Clause 3 on every new permission grant in this accepted act
                     new-gives (filter (fn [f]
                                         (and (= perm-key (:k f))
                                              (some? (:v f))
                                              (nil? (get rows-now (chain/place lane (:e f))))))
                                       facts)
                     c3-this? (every? (fn [f]
                                        (let [v (:v f)
                                              id (:e f)]
                                          (if (and making? (= id first-id))
                                            (and (= who (:given-by v)) (nil? (:under v)))
                                            (and (= who (:given-by v))
                                                 (= pid (:under v))
                                                 (contains? created (:under v))
                                                 (some? under-info)))))
                                      new-gives)
                     ;; Advance permission rows for next stamp
                     rows-after (let [r1 (reduce (fn [m f]
                                                   (if (= perm-key (:k f))
                                                     (let [[k row] (row-map-entry (:e f) (:v f))]
                                                       (assoc m k row))
                                                     m))
                                                 rows-now
                                                 facts)]
                                  (if (and pid (= :exact-act (first (:covers (:says under-info)))))
                                    (assoc r1 (chain/place lane pid) nil)
                                    r1))
                     created-after (reduce (fn [m f] (assoc m (:e f) (:v f))) created new-gives)]
                 {:rows-now rows-after
                  :created created-after
                  :clause1-ok? (and clause1-ok? c1-this?)
                  :clause3-ok? (and clause3-ok? c3-this?)})))
           {:rows-now {} :created {} :clause1-ok? true :clause3-ok? true}
           (:acts full-log)))
        traces-to-making?
        (let [created (:created audit)
              first-id (first-act/first-permission-id lane)]
          (every? (fn [[id _]]
                    (loop [cur id hops 0]
                      (cond
                        (= cur first-id) true
                        (> hops 64) false
                        :else (when-let [u (:under (get created cur))]
                                (recur u (inc hops))))))
                  created))
        ;; Clause 2 + Named Exceptions 1 & 2 on the private lane (:who-may-read-acts defaults to :members)
        plain-read (fn [who session]
                     (ask (cond-> {:asks [:range :first :last] :lanes [lane]}
                            who (assoc :asker who)
                            (nil? who) (assoc :not-signed-in true)
                            session (assoc :session session))))
        own-facts-read (fn [who]
                         (ask (cond-> {:asks [:range :first :last] :lanes [lane] :stores-own-facts true}
                                who (assoc :asker who)
                                (nil? who) (assoc :not-signed-in true))))
        sees-plain? (fn [who session]
                      (let [r (plain-read who session)]
                        [(empty? (:left-out r)) (pos? (count (:places r)))]))
        all-stamps (mapv :stamp (:acts full-log))
        refused-stamps (mapv :stamp (filter #(= :refused (:verdict (:answer %))) (:acts full-log)))
        own-stamps-of (fn [who]
                        (mapv :stamp (filter #(= who (:who (:thing %))) (:acts full-log))))
        whole-stamps-of (fn [acts-ans]
                          (mapv :stamp (filter #(contains? % :thing) (:acts acts-ans))))
        ;; Uncap Bob by raising p-bob back to publisher, then open lane & test :who-may-read-acts :everyone
        private-sees {:dana (sees-plain? (person :dana) nil)
                      :carol (sees-plain? (person :carol) nil)
                      :carol-bot (sees-plain? (agent :carol-bot) nil)
                      :sid-in-sit1 (sees-plain? (person :sid-sit) :sit-1)
                      :sid-no-sit (sees-plain? (person :sid-sit) nil)
                      :sid-sit2 (sees-plain? (person :sid-sit) :sit-2)
                      :wren-write-only (sees-plain? (person :wren) nil)
                      :bob-lowered (sees-plain? (person :bob) nil)
                      :bob-bot-capped (sees-plain? (agent :bob-bot) nil)
                      :bob-run-capped (sees-plain? (run :bob-job) nil)
                      :zoe-after-end (sees-plain? (person :zoe) nil)
                      :alice-handed-over (sees-plain? (person :alice) nil)
                      :eve-ended (sees-plain? (person :eve) nil)
                      :outsider (sees-plain? (person :outsider) nil)
                      :not-signed-in (sees-plain? nil nil)}
        private-acts {:dana (whole-stamps-of (acts-read lane 0 1000 (person :dana)))
                      :carol-see-only (whole-stamps-of (acts-read lane 0 1000 (person :carol)))
                      :carol-bot (whole-stamps-of (acts-read lane 0 1000 (agent :carol-bot)))
                      :wren-write-only (whole-stamps-of (acts-read lane 0 1000 (person :wren)))
                      :bob-lowered (whole-stamps-of (acts-read lane 0 1000 (person :bob)))
                      :zoe (whole-stamps-of (acts-read lane 0 1000 (person :zoe)))
                      :zoe-bot (whole-stamps-of (acts-read lane 0 1000 (agent :zoe-bot)))
                      :alice (whole-stamps-of (acts-read lane 0 1000 (person :alice)))
                      :outsider (whole-stamps-of (acts-read lane 0 1000 (person :outsider)))
                      :not-signed-in (whole-stamps-of (acts-read lane 0 1000 nil))}
        store-key-set (fn [ans]
                        (set (keep (fn [[[_ k] _]] (when (= "softland.store" (namespace k)) k))
                                   (:places ans))))
        private-own-facts {:carol-see-only (store-key-set (own-facts-read (person :carol)))
                           :carol-bot (store-key-set (own-facts-read (agent :carol-bot)))
                           :wren-write-only (store-key-set (own-facts-read (person :wren)))
                           :not-signed-in (store-key-set (own-facts-read nil))}
        ;; Raise Bob back: uncaps Bob, Bob's agent, and Bob's run without touching their rows
        _ (put! {:version 1 :who (person :dana) :lane lane :permission (perm/permission-id p-dana)
                 :session :s-dana :facts [(perm/permission-fact p-bob)]})
        uncapped-sees [(sees-plain? (person :bob) nil)
                       (sees-plain? (agent :bob-bot) nil)
                       (sees-plain? (run :bob-job) nil)]
        ;; Open lane to :anyone and flip :who-may-read-acts from :members to :everyone
        p-open (perm/given-permission :anyone lane p-dana
                                      {:allows :see :lent-from nil :salt (swap! give-salt inc)})
        _ (put! {:version 1 :who (person :dana) :lane lane :permission (perm/permission-id p-dana)
                 :session :s-dana :facts [(perm/permission-fact p-open)]})
        open-members-visitor (acts-read lane 0 1000 (person :outsider))
        open-members-anon (acts-read lane 0 1000 nil)
        _ (put! {:version 1 :who (person :dana) :lane lane :permission (perm/permission-id p-dana)
                 :session :s-dana :facts [(fact lane :softland.store/who-may-read-acts :everyone)]})
        open-anon-own-facts (store-key-set (own-facts-read nil))
        open-everyone-visitor (acts-read lane 0 1000 (person :outsider))
        open-everyone-anon (acts-read lane 0 1000 nil)
        all-stamps-open (mapv :stamp (:acts open-everyone-visitor))
        ;; Also verify Clause 3 lineage on :base (:microbatch)
        base-log (acts-read :base 0 1000 :system)
        base-lineage-ok?
        (let [base-first (first-act/first-permission-id :base)
              base-created (reduce (fn [m {:keys [answer thing]}]
                                     (if (= :refused (:verdict answer))
                                       m
                                       (reduce (fn [m2 f]
                                                 (if (and (= perm-key (:k f)) (some? (:v f)) (not (contains? m2 (:e f))))
                                                   (assoc m2 (:e f) (:v f))
                                                   m2))
                                               m
                                               (:facts (:ok (declaration/check thing))))))
                                   {}
                                   (:acts base-log))]
          (and (pos? (count base-created))
               (every? (fn [[id _]]
                         (loop [cur id hops 0]
                           (cond
                             (= cur base-first) true
                             (> hops 64) false
                             :else (when-let [u (:under (get base-created cur))]
                                     (recur u (inc hops))))))
                       base-created)))]
    (check "clause 1 of the statement to prove: across a multi-party history, every accepted act named a standing uncapped give held by its author (directly or via :anyone-signed-in for a person) allowing its writes, or was the lane's making act, or named nil and ended only gives its author held; every violation was refused"
           [true true true true
            [:who-not-the-permissions-holder :permission-never-given :permission-ended :permission-used-up
             :permission-for-another-sitting :permission-does-not-allow-writing :no-permission
             :permission-above-ended :lane-would-have-no-owner]]
           [(:clause1-ok? audit) (:accepted? wren-wrote) (:accepted? zoe-wrote) (:accepted? eve-own-end)
            (mapv reason-of [zoe-bot-direct ref-never ref-ended ref-used ref-sitting
                             ref-see-write ref-nil-perm ref-above ref-no-owner])])
    (check "clause 3 of the statement to prove: every accepted give in the streaming lane and in the base traces back, give by give through :under, to the making act's first permission, and each giver held what she gave when she gave it"
           [true true true]
           [(:clause3-ok? audit) traces-to-making? base-lineage-ok?])
    (check "clause 2 of the statement to prove: plain facts are shown if and only if the asker currently holds a standing, uncapped give allowing :see over the lane's default group in the sitting asked; raising a lowered lender uncaps its borrowers at once"
           [{:dana [true true]
             :carol [true true]
             :carol-bot [true true]
             :sid-in-sit1 [true true]
             :sid-no-sit [false false]
             :sid-sit2 [false false]
             :wren-write-only [false false]
             :bob-lowered [false false]
             :bob-bot-capped [false false]
             :bob-run-capped [false false]
             :zoe-after-end [false false]
             :alice-handed-over [false false]
             :eve-ended [false false]
             :outsider [false false]
             :not-signed-in [false false]}
            [[true true] [true true] [true true]]]
           [private-sees uncapped-sees])
    (check "clause 2 and named exception 1 (who-may-read-acts): outside askers (write-only, capped, ended, handed-over, stranger) are shown only the acts they themselves wrote; standing :see holders read all acts including refused acts under :members; under :everyone on an open lane, visitors and unauthenticated askers read all accepted and refused acts whole too"
           [true
            {:dana all-stamps
             :carol-see-only all-stamps
             :carol-bot all-stamps
             :wren-write-only (own-stamps-of (person :wren))
             :bob-lowered (own-stamps-of (person :bob))
             :zoe (own-stamps-of (person :zoe))
             :zoe-bot (own-stamps-of (agent :zoe-bot))
             :alice (own-stamps-of (person :alice))
             :outsider (own-stamps-of (person :outsider))
             :not-signed-in []}
            [(own-stamps-of (person :outsider)) []]
            [all-stamps-open all-stamps-open]]
           [(and (seq refused-stamps) (every? (set all-stamps) refused-stamps))
            private-acts
            [(whole-stamps-of open-members-visitor) (whole-stamps-of open-members-anon)]
            [(whole-stamps-of open-everyone-visitor) (whole-stamps-of open-everyone-anon)]])
    (check "named exception 2 (the store's own facts): permission grants, changes and ends, :depot, :marked-keys and :who-may-read-acts are readable with :stores-own-facts under the lane's :see rule, by see-only holders, lent agents and unauthenticated visitors of an open lane, and hidden from write-only or outside askers"
           [{:carol-see-only #{:softland.store/depot :softland.store/permission :softland.store/marked-keys}
             :carol-bot #{:softland.store/depot :softland.store/permission :softland.store/marked-keys}
             :wren-write-only #{}
             :not-signed-in #{}}
            #{:softland.store/depot :softland.store/permission :softland.store/marked-keys :softland.store/who-may-read-acts}]
           [private-own-facts open-anon-own-facts])
    (shown "the statement to prove, with its two named exceptions, checked across" (count (:acts full-log))
           "decisions (" (count refused-stamps) "refused) and" (count (:created audit))
           "gives in" lane "and" (count (:acts base-log)) "decisions in :base")))

(deftest a-read-of-acts-reads-standing-and-setting-at-one-moment
  (let [lane :acts-one-moment
        alice (person :alice)
        _ (put! (perm/making-act lane alice alice :s-acts))
        p-owner (perm/first-permission alice lane)
        _ (put! {:version 1 :who alice :lane lane :permission (perm/permission-id p-owner)
                 :session :s-acts
                 :facts [{:e lane :k :softland.store/who-may-read-acts :v :everyone}
                         {:e :vault :k :secret :v "private-era-thing"}]})
        lr-query (rama/foreign-query (:ipc @round) (:module @round) "lane-rights")
        bad-askers (mapv #(rama/foreign-invoke-query lr-query lane lane :see % nil)
                         [:anyone :anyone-signed-in :asha (UUID. 0 99)])
        p-open (perm/given-permission :anyone lane p-owner
                                      {:allows :see :lent-from nil :salt (swap! give-salt inc)})
        real-invoke rama/foreign-invoke-query-async
        torn-ans
        (with-redefs [rama/foreign-invoke-query-async
                      (fn [q & args]
                        (CompletableFuture/supplyAsync
                         (reify java.util.function.Supplier
                           (get [_]
                             (Thread/sleep 40)
                             (put! {:version 1 :who alice :lane lane :permission (perm/permission-id p-owner)
                                    :session :s-acts
                                    :facts [{:e lane :k :softland.store/who-may-read-acts :v :members}
                                            (perm/permission-fact p-open)]})
                             (.get ^CompletableFuture (apply real-invoke q args))))))]
          (read-path/read-once (the-rp) {:asks [:acts 1 10] :not-signed-in true :lanes [lane]}))]
    (check "lane-rights rejects collective holders and non-party ids in *asker with nil"
           [nil nil nil nil]
           bad-askers)
    (check "a read of acts reads standing and who-may-read-acts in the same lane-rights turn, so an atomic flip from private/:everyone to open/:members never leaks private-era things"
           []
           (filterv #(contains? % :thing) (:acts torn-ans)))))

(defn- on-a-cluster
  "Starts an in-process cluster, launches the module `Store` in it, opens
  the proof's door and read paths, calls f, and closes the cluster."
  [f]
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc module/Store {:tasks tasks :threads 2 :workers 2})
    (open-round! ipc)
    (f)))

(deftest a-key-that-is-a-uuid
  (let [k (UUID/fromString "0b0b0b0b-0000-4000-8000-00000000000b")
        k-late (UUID/fromString "f0f0f0f0-0000-4000-8000-00000000000f")
        alice (person :alice)
        _ (make! :keyed alice)
        marked (mark! :keyed alice #{k})
        q (fn [asks] (ask {:asks asks :asker alice :lanes [:keyed]}))
        wrote (put! alice :keyed [(fact :chair-7 k :oak) (fact :chair-7 :colour :green) (fact :chair-7 k-late 3)])
        id (:act-id wrote)
        answer-of-fact (fn [index v] {:holds :value :value v :lane :keyed :fact [id index] :stamp (:stamp wrote)})
        at-place (:place (q [:place :chair-7 k]))
        in-entity (:places (q [:entity :chair-7]))
        to-the-keyword (:places (q [:range [:chair-7 k] [:chair-7 :colour]]))
        to-the-late-key (:places (q [:range [:chair-7 k] [:chair-7 k-late]]))
        points (q [:points-at k :oak])
        replaced (put! alice :keyed [(assoc (fact :chair-7 k :elm) :replaces [id 0])])
        stale (act! alice :keyed [(assoc (fact :chair-7 k :ash) :replaces [id 0])])
        after-replace (:place (q [:place :chair-7 k]))
        points-after [(:places (q [:points-at k :oak])) (set (keys (:places (q [:points-at k :elm]))))]
        retracted (put! alice :keyed [{:e :chair-7 :k k}])
        after-retract (:place (q [:place :chair-7 k]))
        in-the-base (put! :system :base [(fact :chair-7 k "in the base")])
        base-place (:place (ask {:asks [:place :chair-7 k] :asker :system :lanes [:base]}))]
    (check "a fact under a key that is a UUID is accepted and read back by its place: its value, its lane, the fact id that the door's act-id and the fact's index make, and the stamp of the door's answer"
           (answer-of-fact 0 :oak) at-place)
    (check "the entity gives its three places: one under a keyword key and two under keys that are UUIDs"
           {[:chair-7 k] (answer-of-fact 0 :oak) [:chair-7 :colour] (answer-of-fact 1 :green) [:chair-7 k-late] (answer-of-fact 2 3)}
           in-entity)
    (check "a range goes by the printed text of a key: from the UUID key that begins with a digit to the keyword key it holds those two places, and to the UUID key that begins with a letter, which comes after every keyword, all three"
           [#{[:chair-7 k] [:chair-7 :colour]} #{[:chair-7 k] [:chair-7 :colour] [:chair-7 k-late]}]
           [(set (keys to-the-keyword)) (set (keys to-the-late-key))])
    (check "a lane marks a key that is a UUID: what points at a thing under it gives the place, and the order is level since the stamp of the act that marked the key"
           [{[:chair-7 k] (answer-of-fact 0 :oak)} (:stamp marked)]
           [(:places points) (:level-since (first (:orders points)))])
    (check "the fact is replaced by one that names it, and a second fact that names the same one is refused as stale; the place then holds the new value, and the order by value holds the place under the new value and no longer under the old"
           [true :replaced-fact-not-current
            {:holds :value :value :elm :lane :keyed :fact [(:act-id replaced) 0] :stamp (:stamp replaced)}
            [{} #{[:chair-7 k]}]]
           [(:accepted? replaced) (:reason (:answer stale)) after-replace points-after])
    (check "the fact is retracted: the place says nothing here, with the fact and the stamp of the retract"
           {:holds :nothing-here :lane :keyed :fact [(:act-id retracted) 0] :stamp (:stamp retracted)} after-retract)
    (check "in the base, through the microbatch gate, a fact under a key that is a UUID is accepted and read back by its place"
           {:holds :value :value "in the base" :lane :base :fact [(:act-id in-the-base) 0] :stamp (:stamp in-the-base)} base-place)
    (shown "a fact's key that is a UUID, through the door, each gate and the read path: written and read back by place, in its entity and in a range; marked, and found by what points at a thing; replaced, a stale replace refused"
           (pr-str (:reason (:answer stale))) "; retracted | a range goes by the printed text, so of one entity the keys that are UUIDs stand on both sides of the keyword keys")))

(defn- the-checks-of-the-first-cluster
  "Runs the checks in the order in which the proof tells them. Later checks
  build on the lanes that earlier ones made. The checks that count the
  calls of a read asked once come before any live read is opened; the
  module is started again last."
  []
  (the-read-path-is-opened)
  (a-thing-that-is-no-query)
  (each-word-once-for-each-gate)
  (the-three-things-that-a-gates-write-leaves-at-a-place)
  (a-part-of-a-row-is-given-only-when-asked)
  (the-stores-own-facts)
  (through-lanes)
  (a-lane-that-is-left-out)
  (who-asks)
  (what-points-at-a-thing-through-lanes)
  (the-calls-of-a-read-asked-once)
  (a-very-large-run)
  (a-run-across-entities-of-the-base)
  (the-base-is-read-where-the-askers-lane-lives)
  (each-word-kept-live)
  (what-shapes-a-read-changes-under-it)
  (a-permission-ends-under-a-live-read)
  (a-read-asked-in-a-run)
  (the-reviews-cluster-probe-written-again)
  (the-second-reviews-cluster-probe-written-again)
  (a-step-that-does-not-settle-ends-its-read)
  (one-push-for-one-thing)
  (the-read-paths-own-bound-on-its-pushes)
  (nothing-is-shown-that-is-not-in-the-tables)
  (reads-that-fail)
  (the-function-and-its-threads)
  (one-reader-in-the-source)
  (the-order-of-the-tables-keys)
  (a-lanes-acts-by-stamp)
  (a-function-that-leaves-or-asks-inside-its-call)
  (the-opening)
  (the-pushes-a-live-viewer-holds)
  (a-transfer-in-one-act-never-shows-a-state-in-between)
  (the-statement-to-prove-with-its-named-exceptions)
  (a-read-of-acts-reads-standing-and-setting-at-one-moment)
  (a-key-that-is-a-uuid)
  (the-first-thing-a-function-is-given)
  (the-module-is-started-again))

(defn test-ns-hook
  "Runs the proof on its two clusters, one after the other: every check but
  one on the module as the store defines it; then, with one function of
  the gates' walk replaced, the check of a batch that runs a second time.
  The store's own function is put back whatever that check does. What the
  proof keeps of the first cluster means nothing on the second, so it is
  emptied between them."
  []
  (on-a-cluster the-checks-of-the-first-cluster)
  (reset! permissions {})
  (reset! give-salt 0)
  (reset! sent-things {})
  (reset! live-reads [])
  (put-the-proofs-function!)
  (try
    (on-a-cluster a-batch-that-runs-a-second-time)
    (finally
      (put-the-stores-function-back!))))

(defn -main
  "Runs the proof, which starts and closes its own clusters, and exits:
  status 0 when each check holds. It takes no argument."
  [& _]
  (let [result (run-tests 'softland.store.read-path-proof)
        {:keys [fail error]} result]
    (println)
    (println "THE PROOF, IN ONE PLACE")
    (run! println @shown-lines)
    (println (select-keys result [:test :pass :fail :error]))
    (shutdown-agents)
    (System/exit (if (zero? (+ fail error)) 0 1))))
