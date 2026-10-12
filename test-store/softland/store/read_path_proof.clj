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
  the read path's five calls, and each act goes in through the door, with
  one exception: for the bytes that are no value of the form, the proof's
  own hand asks for two locks, seals the bytes and appends each thing
  itself, since the door seals only a value.

  What it reads round the read path. For what no call shows, the proof reads
  the read path's own memory, its three maps, from the field of its type: to
  see that a push has left the map with its last reader, that no entry is
  without a reader, which fields an entry has, and that the counts of the
  pushes are the counts of the map's entries. To find the task of a
  layer it selects the layer's depot row with a handle of its own; the line
  that rests on that says so. It reads no table round the read path for a
  thing that it shows.

  Where the proof stands in for a call of Rama's client. The read path's
  two reading calls are macros of Rama's that end in two functions,
  `compiled-foreign-select-async` and `compiled-foreign-proxy-async`.
  Clojure lets a proof put a function of its own in the place of those, and
  of `close!`, for a while. The proof does so to count the calls of a read;
  to keep each push and each callback that a read opened; to make a select
  throw, and to make one never come back; to make the opening of a push
  throw, and to make one never come back. Each line that rests on that says
  so. It also puts a function in the place of two pure functions of the read
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
  module's proof has one: before that cluster is launched the proof puts
  functions of its own in the place of three pure functions of
  `softland.store.gate.batch`, which note the rows of each attempt and
  throw once, after the rows of the first attempt were made; and it puts
  the store's own three back after. Every other check runs on the functions
  as the store defines them.

  State. The proof starts one in-process cluster with four tasks and two
  workers, launches the module `Store` in it, and closes it at the end; the
  cluster's files are temporary and are Rama's. It opens one door and four
  read paths on the module: the read path of most checks; one with small
  numbers, for the checks of a run's size; one whose wait is 300 ms; and one
  for the checks that count pushes, so that no other check's push is in its
  maps. The group of the read path's own bound opens two more, each with
  one small number: one that may hold 3 pushes on one table under one
  routing id, and one whose live reads may need 1 check. It owns atoms: what
  it holds of the cluster, the lines that it showed, each permission that it
  made, each thing that the door sent, and each live read that it opened.
  Near the end it starts the module again in the cluster, by Rama's module
  update with the unchanged module; Rama then prints some lines of its own
  log, marked ERROR, that are no failed checks.

  What it does not reach is in the folder's README."
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]
            [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [com.rpl.rama.test :as rtest]
            [softland.store.door :as door]
            [softland.store.door.seal :as seal]
            [softland.store.gate.batch :as batch]
            [softland.store.module :as module]
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
  "Checks that what was observed is what must be. Gives what was observed."
  [what expected observed]
  (is (= expected observed) what)
  observed)

;; ------------------------------------------------------- the proof's own state

(def ^:private tasks "The number of tasks of the proof's cluster." 4)

(defonce ^{:private true
           :doc "What the proof holds of the cluster that it runs on, set by
  `open-round!`: the cluster, the module's name, the door, the proof's read
  paths, and the three handles of the proof's own hand."}
  round
  (atom nil))

(defonce ^{:private true
           :doc "The permission that the proof made for each who in each layer, by
  [who layer]: what `act!` sends an act under."}
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
  the three handles that the proof's own hand uses."
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
             :streaming-depot (rama/foreign-depot ipc m "*streaming-depot")
             :locks           (rama/foreign-query ipc m "locks-of-request")
             :streaming-facts (rama/foreign-pstate ipc m "$$streaming-facts")})))

(defn- the-rp "The read path of most checks." [] (:read-path @round))
(defn- the-door "The proof's door." [] (:door @round))

;; ------------------------------------------------------------- through the door

(defn- perm
  "The permission that a who holds in a layer, as the proof made it."
  [who layer]
  (get @permissions [who layer]))

(defn- act!
  "Sends one act of a who into a layer through the door, under the
  permission that the proof made for that who, and reads its answer: at
  once for a streaming act, with a bounded wait for an act of the base.
  Gives {:act-id :stamp :accepted? :answer :sent}."
  [who layer facts]
  (let [sent (door/send-act (the-door) {:version 1 :who who :layer layer :permission (perm who layer)
                                        :session :read-path-proof :facts facts})]
    (if (:could-not sent)
      {:sent sent :accepted? false}
      (let [answer (if (= :depot-holds (:ack sent))
                     (door/answer-of (the-door) (:act-id sent) (:digest sent) 30000)
                     (door/answer-of (the-door) (:act-id sent) (:digest sent)))]
        (swap! sent-things assoc (:act-id sent) sent)
        {:sent sent :answer answer :act-id (:act-id sent) :stamp (:stamp answer)
         :accepted? (= :accepted (:answer answer))}))))

(defn- put!
  "Sends one act that must be accepted, and holds that it was. Gives what
  `act!` gives."
  [who layer facts]
  (let [got (act! who layer facts)]
    (is (:accepted? got) (str "an act of " who " into " layer " is accepted: " (pr-str (or (:answer got) (:sent got)))))
    got))

(defn- make!
  "Makes a layer through the door, with a holder: a streaming layer by its
  holder, the base by the system. Keeps the holder's permission."
  [layer holder]
  (let [own [holder layer layer]]
    (swap! permissions assoc [holder layer] own)
    (put! holder layer [{:e layer :k :softland.store/depot :v (if (= :base layer) :microbatch :streaming)}
                        {:e holder :k :softland.store/permission :v own}])))

(defn- give!
  "A holder of a layer gives another who a permission in it, beneath her
  own; with a word, the permission says what it allows. Keeps it."
  ([layer giver who] (give! layer giver who nil))
  ([layer giver who allows]
   (let [given (cond-> [who layer layer (perm giver layer)] allows (conj allows))]
     (swap! permissions assoc [who layer] given)
     (put! giver layer [{:e who :k :softland.store/permission :v given}]))))

(defn- fact "A fact with a value, as a maker gives it." [e k v] {:e e :k k :v v})

(defn- setting! "Writes who may see a layer." [layer who word]
  (put! who layer [(fact layer :softland.store/who-may-see word)]))

(defn- list! "Writes what a layer builds on." [layer who listed]
  (put! who layer [(fact layer :softland.store/builds-on listed)]))

(defn- mark! "Writes the keys that a layer marks." [layer who marked]
  (put! who layer [(fact layer :softland.store/marked-keys marked)]))

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
                                            (= :no-fact (:holds place-answer)) (update screen :places dissoc place)
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

;; ----------------------------------------- standing in for a call of Rama's client

(defn- counting
  "Calls f while the proof stands in for Rama's two reading calls and its
  close with functions that count and then call Rama, and for the read
  path's own function that makes the calls of one round with one that notes
  how many it made. Gives {:result :selects :opens :closes :rounds}, the
  last a vector of the count of calls of each round of a read asked once.
  What must hold: nothing else of the proof calls Rama meanwhile."
  [f]
  (let [selects (atom 0) opens (atom 0) closes (atom 0) rounds (atom [])
        real-select rama/compiled-foreign-select-async
        real-open rama/compiled-foreign-proxy-async
        real-close rama/close!
        real-calls sources/calls-for-once]
    (with-redefs [rama/compiled-foreign-select-async (fn [& args] (swap! selects inc) (apply real-select args))
                  rama/compiled-foreign-proxy-async (fn [& args] (swap! opens inc) (apply real-open args))
                  rama/close! (fn [x] (swap! closes inc) (real-close x))
                  sources/calls-for-once (fn [missing numbers]
                                           (let [calls (real-calls missing numbers)]
                                             (when (seq calls) (swap! rounds conj (count calls)))
                                             calls))]
      (let [result (f)]
        {:result result :selects @selects :opens @opens :closes @closes :rounds @rounds}))))

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

(def ^:private stores-base
  "`softland.store.gate.batch/base` as the store defines it."
  batch/base)

(def ^:private stores-decide-or-send
  "`softland.store.gate.batch/decide-or-send` as the store defines it."
  batch/decide-or-send)

(def ^:private stores-rows-to-send
  "`softland.store.gate.batch/rows-to-send` as the store defines it."
  batch/rows-to-send)

(defonce ^{:private true
           :doc "The rows that each attempt of a batch with things made on the
  proof's second cluster, in the order in which the attempts began: for
  each attempt a vector of [key row]."}
  attempts (atom []))

(defonce ^{:private true :doc "True while the proof asks for one throw."} throw-asked (atom false))
(defonce ^{:private true :doc "How many throws the proof made."} throws-made (atom 0))

(defn- note-rows!
  "Adds rows, each [key row], to the note of the newest attempt."
  [rows]
  (when (seq rows)
    (swap! attempts (fn [notes] (if (seq notes) (update notes (dec (count notes)) into rows) notes)))))

(defn noting-base
  "Stands in for `softland.store.gate.batch/base` on the proof's second
  cluster. Takes what it takes and gives what it gives. The gate calls it
  once in each attempt; for a batch with things it begins the note of an
  attempt. It is public and held by a var, and it closes over nothing: Rama
  takes the function that the store's var holds at the launch."
  [& args]
  (when (nth args 2 nil)
    (swap! attempts conj []))
  (apply stores-base args))

(defn noting-decide-or-send
  "Stands in for `softland.store.gate.batch/decide-or-send` on the proof's
  second cluster. Takes what it takes and gives what it gives, and notes
  each fact row that it gave: those that the task writes at once, and those
  that it sends to another task."
  [& args]
  (let [out (apply stores-decide-or-send args)]
    (note-rows! (concat (:now out)
                        (mapcat (fn [[_ kind payload]]
                                  (when (= :rows kind) (map (fn [[_ k row]] [k row]) payload)))
                                (:parcels out))))
    out))

(defn throwing-rows-to-send
  "Stands in for `softland.store.gate.batch/rows-to-send` on the proof's
  second cluster. Takes what it takes and gives what it gives, and notes
  each row. Then, when the proof asked for a throw and this call gave rows,
  it throws, this once: the attempt fails after its rows were made, and
  Rama runs the batch again."
  [& args]
  (let [out (apply stores-rows-to-send args)]
    (note-rows! (map (juxt :key :row) out))
    (when (and (seq out) (compare-and-set! throw-asked true false))
      (swap! throws-made inc)
      (throw (ex-info "the read path's proof: a forced failure of this attempt of a batch, after its rows were made" {})))
    out))

(defn- put-the-proofs-functions!
  "Puts the three functions of the proof into the vars of the store's
  three. What must hold: no cluster of this JVM is launched; a launch takes
  what the vars hold then."
  []
  (alter-var-root #'batch/base (constantly noting-base))
  (alter-var-root #'batch/decide-or-send (constantly noting-decide-or-send))
  (alter-var-root #'batch/rows-to-send (constantly throwing-rows-to-send)))

(defn- put-the-stores-functions-back!
  "Puts the store's own three functions back into their vars."
  []
  (alter-var-root #'batch/base (constantly stores-base))
  (alter-var-root #'batch/decide-or-send (constantly stores-decide-or-send))
  (alter-var-root #'batch/rows-to-send (constantly stores-rows-to-send)))

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
  (let [good {:asks [:place :note-41 :text] :asker :alice :layers [:main]}
        faults [[5 :not-a-query]
                [(assoc good :live true) :part-not-known]
                [(assoc good :asks [:find "text"]) :asks-no-word-known]
                [(assoc good :asks [:place :note-41]) :place-not-named]
                [(dissoc good :asker) :no-one-asker]
                [(assoc good :layers []) :layers-not-named-one-way]
                [(assoc good :layers (mapv #(keyword (str "l" %)) (range 66))) :too-many-layers]
                [(assoc good :asks [:run [:b] [:a]]) :run-ends-out-of-order]
                [(assoc good :asks [:place :main :softland.store/depot]) :store-key-not-asked-for]
                [(assoc good :asker (keyword (apply str (repeat 300 "a")))) :id-over-the-limit]]
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
  (let [_ (make! :main :alice)
        _ (make! :base :system)
        main-marked (mark! :main :alice #{:parent})
        base-marked (mark! :base :system #{:parent})
        facts (vec (concat [(fact :note-41 :text "a note")]
                           (for [i (range 1 6)] (fact :doc (keyword (str "k" i)) i))
                           (for [i (range 1 13)] (fact (keyword (format "e-%02d" i)) :size i))
                           (for [i (range 1 4)] (fact (keyword (str "block-" i)) :parent :canvas-1))))
        observed (for [[who layer marked] [[:alice :main main-marked] [:system :base base-marked]]]
                   (let [wrote (put! who layer facts)
                         id (:act-id wrote)
                         base {:asker who :layers [layer]}
                         place (ask (assoc base :asks [:place :note-41 :text]))
                         entity (ask (assoc base :asks [:entity :doc]))
                         run (ask (assoc base :asks [:run [:e-01] [:e-12]]))
                         points (ask (assoc base :asks [:points-at :parent :canvas-1]))]
                     (check (str "a place of " layer ": the value that the accepted act wrote, the layer, the fact id that the door's act-id and the fact's index make, and the stamp of the door's answer")
                            {:layers [layer] :left-out []
                             :place {:holds :value :value "a note" :layer layer :fact [id 0] :stamp (:stamp wrote)}}
                            place)
                     (check (str "an entity of " layer ": its five places, each with its value and its fact")
                            (into {} (for [i (range 1 6)]
                                       [[:doc (keyword (str "k" i))]
                                        {:holds :value :value i :layer layer :fact [id i] :stamp (:stamp wrote)}]))
                            (:places entity))
                     (check (str "a run of " layer " across twelve entities: its twelve places")
                            (into {} (for [i (range 1 13)]
                                       [[(keyword (format "e-%02d" i)) :size]
                                        {:holds :value :value i :layer layer :fact [id (+ 5 i)] :stamp (:stamp wrote)}]))
                            (:places run))
                     (check (str "what points at a thing in " layer ": the three places under the marked key")
                            (into {} (for [i (range 1 4)]
                                       [[(keyword (str "block-" i)) :parent]
                                        {:holds :value :value :canvas-1 :layer layer :fact [id (+ 17 i)] :stamp (:stamp wrote)}]))
                            (:places points))
                     (check (str "since which stamp the order of " layer " is level: in a streaming layer the stamp of the act that marked the key; in the base a stamp of that act's batch, not earlier than the act's; and earlier than the facts that are in the order")
                            [[layer] true true]
                            (let [since (:level-since (first (:orders points)))]
                              [(mapv :layer (:orders points))
                               (if (= :base layer) (<= (:stamp marked) since) (= (:stamp marked) since))
                               (< since (:stamp wrote))]))
                     [layer (count (:places entity)) (count (:places run)) (count (:places points)) (:stamp wrote)]))]
    (doseq [[layer entity run points stamp] observed]
      (shown "each word of a query, asked once in" layer
             (if (= :base layer) "(the microbatch gate):" "(the streaming gate):")
             "a place gives the value that an accepted act wrote, with its layer, its fact id and the stamp" stamp
             "of the door's answer; an entity gives its" entity "places; a run across entities its" run
             "; what points at a thing its" points "places and the stamp since which the order is level"))))

(deftest the-four-things-that-a-place-can-hold
  (let [uuid (UUID/randomUUID)
        long-text (apply str (repeat 60000 "x"))
        values {:spaces "a string with  two spaces" :map {:a 1 :b #{:x "y"} :c [1.5M nil]} :uuid uuid :long long-text}
        _ (put! :alice :main (vec (for [[e v] values] (fact e :text v))))
        _ (put! :alice :main [(fact :gone :text "soon gone")])
        retract (put! :alice :main [{:e :gone :k :text}])
        ;; by the proof's own hand: bytes that are no value, under a right seal
        request-id [:main :streaming (UUID/randomUUID)]
        hand {:version 1 :who :alice :layer :main :permission (perm :alice :main) :session :read-path-proof}
        _ (rama/foreign-append! (:streaming-depot @round)
                                (merge hand {:act-id request-id :depot :streaming
                                             :facts [{:e (nth request-id 2) :k :softland.store/lock-request :v 2}]})
                                :ack)
        locks (rama/foreign-invoke-query (:locks @round) (rows/routing-id request-id) request-id 0 1)
        lock (get locks 0)
        raw-id [:main :streaming (UUID/randomUUID)]
        _ (rama/foreign-append! (:streaming-depot @round)
                                (merge hand {:act-id raw-id :depot :streaming
                                             :facts [{:e :raw :k :text
                                                      :sealed (seal/seal lock [:main :raw :text] (.getBytes "(no value of the form" "UTF-8"))
                                                      :lock-id [request-id 0]}]})
                                :ack)
        ;; and bytes that are no text at all: a gate keeps them as U+0000 and a character for each byte
        no-text (byte-array [(byte -1) (byte -2) (byte 0) (byte 65)])
        no-text-id [:main :streaming (UUID/randomUUID)]
        _ (rama/foreign-append! (:streaming-depot @round)
                                (merge hand {:act-id no-text-id :depot :streaming
                                             :facts [{:e :raw-bytes :k :text
                                                      :sealed (seal/seal (get locks 1) [:main :raw-bytes :text] no-text)
                                                      :lock-id [request-id 1]}]})
                                :ack)
        place (fn [e] (:place (ask {:asks [:place e :text] :asker :alice :layers [:main]})))
        no-text-live (live {:asks [:place :raw-bytes :text] :asker :alice :layers [:main]})
        no-text-now (:place (:now no-text-live))
        _ (leave! no-text-live)]
    (check "a place with no fact" {:holds :no-fact} (place :nobody))
    (check "a place whose current fact is a retract: nothing here, with the layer, the fact and the stamp of the retract"
           {:holds :nothing-here :layer :main :fact [(:act-id retract) 0] :stamp (:stamp retract)} (place :gone))
    (check "a value reads back as its maker gave it: a string with spaces, a map, a UUID, a string of 60,000 characters"
           values (into {} (for [e (keys values)] [e (:value (place e))])))
    (check "bytes that are no value of the form, which the proof's own hand sealed and appended: the place says so, and the read does not throw"
           [:not-a-value :main [raw-id 0]] ((juxt :holds :layer :fact) (place :raw)))
    (check "bytes that are no text at all, sealed and appended by the same hand, which the gate keeps as U+0000 and a character for each byte: asked once and kept live the place says that they are no value, with its layer and its fact, and holds no :value; no read throws"
           [[:not-a-value :main [no-text-id 0] false] [:not-a-value :main [no-text-id 0] false]]
           (mapv (fn [a] [(:holds a) (:layer a) (:fact a) (contains? a :value)]) [(place :raw-bytes) no-text-now]))
    (shown "the four things that a place can hold are told apart: no fact" (pr-str (place :nobody))
           "| a retract" (pr-str (:holds (place :gone))) "| a value as its maker gave it, of four kinds, the longest of 60,000 characters"
           "| bytes that are no value" (pr-str (:holds (place :raw))) "(appended by the proof's own hand; the door seals only a value)")))

(deftest a-part-of-a-row-is-given-only-when-asked
  (let [place (fn [with] (:place (ask {:asks [:place :note-41 :text] :asker :alice :layers [:main] :with with})))
        _ (swap! permissions assoc [:dora :dora] [:dora :dora :dora])
        made (let [sent (door/send-act (the-door) {:version 1 :who :system :layer :dora :permission [:dora :dora :dora]
                                                   :session :read-path-proof
                                                   :facts [{:e :dora :k :softland.store/depot :v :streaming}
                                                           {:e :dora :k :softland.store/permission :v [:dora :dora :dora]}]})]
               (door/answer-of (the-door) (:act-id sent) (:digest sent)))
        depot (:place (ask {:asks [:place :dora :softland.store/depot] :asker :dora :layers [:dora]
                            :stores-own-facts true :with #{:writer :permission}}))]
    (check "the system's making of a personal layer for its person is accepted" :accepted (:answer made))
    (check "a place asked with no part of its row: the place-answer holds neither who wrote it nor the permission"
           #{:holds :value :layer :fact :stamp} (set (keys (place #{}))))
    (check "asked with the writer: who wrote it, and no permission" [:alice false]
           [(:writer (place #{:writer})) (contains? (place #{:writer}) :permission)])
    (check "asked with both: they are the act's who and the permission that it named"
           [:alice (perm :alice :main)] ((juxt :writer :permission) (place #{:writer :permission})))
    (check "for the system's making of a personal layer the two differ: the system wrote the fact, under the person's permission"
           [:system [:dora :dora :dora]] ((juxt :writer :permission) depot))
    (shown "a part of a row is handed over only when the query asks: with no :with the place-answer's parts are" (pr-str (sort (keys (place #{}))))
           "| with :writer it holds" (pr-str (:writer (place #{:writer}))) "| with both, the act's who and its permission"
           "| the depot fact of a personal layer that the system made:" (pr-str (select-keys depot [:writer :permission])))))

(deftest the-stores-own-facts
  (let [places (fn [asks own?] (:places (ask (cond-> {:asks asks :asker :alice :layers [:main]} own? (assoc :stores-own-facts true)))))
        store-key? (fn [[_ k]] (= "softland.store" (namespace k)))
        refused (counting #(ask {:asks [:place :main :softland.store/depot] :asker :alice :layers [:main]}))
        all (places [:run :first :last] false)
        all-own (places [:run :first :last] true)]
    (check "the entity of a holder, asked without the store's own facts: nothing; with them: her permission"
           [{} #{:softland.store/permission}]
           [(places [:entity :alice] false) (set (map second (keys (places [:entity :alice] true))))])
    (check "the entity of the layer itself: nothing; with the store's own facts: its depot and its marked keys"
           [{} #{:softland.store/depot :softland.store/marked-keys}]
           [(places [:entity :main] false) (set (map second (keys (places [:entity :main] true))))])
    (check "a run across the whole layer, after values were written: no permission, setting, mark or request for locks in the answer that did not ask; with the store's own facts each kind is there"
           [true true #{:softland.store/depot :softland.store/permission :softland.store/marked-keys :softland.store/lock-request}]
           [(pos? (count all)) (not-any? store-key? (keys all)) (set (map second (filter store-key? (keys all-own))))])
    (check "a place under one of the store's own keys, asked without them, is turned away with a word, and no call is made"
           [[:read-the-query :store-key-not-asked-for] 0]
           [((juxt :could-not :why) (:result refused)) (:selects refused)])
    (shown "the store's own facts are in no answer that did not ask for them: the whole layer gives" (count all) "places and none under a key of the store;"
           "asked with :stores-own-facts it gives" (count all-own) "| a place under a store key, asked without, gives"
           (pr-str (select-keys (:result refused) [:could-not :why])) "with no call to Rama")))

(deftest through-layers
  (let [_ (doseq [layer [:n7 :m7 :f7]] (make! layer :alice))
        through (fn [asks layers] (ask {:asks asks :asker :alice :layers layers}))
        from (fn [asks] (ask {:asks asks :asker :alice :from-layer :n7}))
        winner (fn [answer] ((juxt :holds :layer :value) (:place answer)))
        far (put! :alice :f7 [(fact :p7 :text "from the farthest") (fact :q7 :text "f") (fact :r7-b :n "hidden") (fact :r7-c :n "c")])
        only-far (through [:place :p7 :text] [:n7 :m7 :f7])
        _ (put! :alice :m7 [(fact :p7 :text "from the middle") (fact :q7 :text "m") (fact :r7-b :n "b")])
        with-middle (through [:place :p7 :text] [:n7 :m7 :f7])
        retract (do (put! :alice :n7 [(fact :p7 :text "soon gone") (fact :r7-a :n "a")])
                    (put! :alice :n7 [{:e :p7 :k :text}]))
        with-retract (through [:place :p7 :text] [:n7 :m7 :f7])
        run (through [:run [:r7-a] [:r7-c]] [:n7 :m7 :f7])
        _ (list! :n7 :alice [:m7 :f7])
        first-list (from [:place :q7 :text])
        _ (list! :n7 :alice [:f7])
        second-list (from [:place :q7 :text])
        twice (through [:place :q7 :text] [:n7 :m7 :n7 :f7 :m7])]
    (check "a place that only the farthest layer holds comes from there, and the answer says which layer won"
           [[:value :f7 "from the farthest"] [(:act-id far) 0]] [(winner only-far) (:fact (:place only-far))])
    (check "when a nearer layer holds a fact there too, the nearer wins" [:value :m7 "from the middle"] (winner with-middle))
    (check "when the nearest holds a retract, the nearest wins with nothing here"
           [[:nothing-here :n7 nil] [(:act-id retract) 0]] [(winner with-retract) (:fact (:place with-retract))])
    (check "a run that three layers hold in parts: each place from the nearest layer that holds it"
           {[:r7-a :n] [:n7 "a"] [:r7-b :n] [:m7 "b"] [:r7-c :n] [:f7 "c"]}
           (into {} (map (fn [[p a]] [p [(:layer a) (:value a)]])) (:places run)))
    (check "from a layer: the layer, then the layers of its own fact, in order; the read uses the newest list"
           [[[:n7 :m7 :f7] [:value :m7 "m"]] [[:n7 :f7] [:value :f7 "f"]]]
           [[(:layers first-list) (winner first-list)] [(:layers second-list) (winner second-list)]])
    (check "a list that names a layer twice is read with each layer once, at its first place"
           [[:n7 :m7 :f7] [:value :m7 "m"]] [(:layers twice) (winner twice)])
    (shown "through three layers the nearest layer that holds a fact wins: only the farthest holds it" (pr-str (winner only-far))
           "| a nearer one too" (pr-str (winner with-middle)) "| the nearest holds a retract" (pr-str (winner with-retract))
           "| a run in parts gives each place from its nearest layer")
    (shown "from a layer the read takes the layer's own list, and the newest one:" (pr-str (:layers first-list)) "and, after the list was written again,"
           (pr-str (:layers second-list)) "| a list that names a layer twice is read as" (pr-str (:layers twice)))))

(deftest a-layer-that-is-left-out
  (let [_ (give! :n7 :alice :bob)
        _ (give! :f7 :alice :bob)
        bob (fn [m] (ask (merge {:asker :bob} m)))
        with (bob {:asks [:run [:r7-a] [:r7-c]] :layers [:n7 :m7 :f7]})
        without (bob {:asks [:run [:r7-a] [:r7-c]] :layers [:n7 :f7]})
        not-made (bob {:asks [:place :q7 :text] :layers [:n7 :nowhere :f7]})
        start (bob {:asks [:place :q7 :text] :from-layer :m7})]
    (check "an asker with no permission in the middle layer: the answer names the layer and why"
           [[:n7 :m7 :f7] [{:layer :m7 :why :may-not-see}]] [(:layers with) (:left-out with)])
    (check "every other part of the answer equals the answer of the same query without that layer; nothing of the layer is given: the place that the middle layer held comes from the farthest"
           [true [:f7 "hidden"]] [(= (:places with) (:places without)) ((juxt :layer :value) (get (:places with) [:r7-b :n]))])
    (check "a layer that is not made is named with another word"
           [[{:layer :nowhere :why :not-made}] [:value :f7 "f"]]
           [(:left-out not-made) ((juxt :holds :layer :value) (:place not-made))])
    (check "a start layer that she may not see: that one layer is left out, its list is not used, and nothing else is given"
           {:layers [:m7] :left-out [{:layer :m7 :why :may-not-see}] :place {:holds :no-fact}} start)
    (shown "a layer that the asker may not see is left out, and the read says so:" (pr-str (:left-out with))
           "| a layer that is not made:" (pr-str (:left-out not-made)) "| a start layer that she may not see gives" (pr-str start))))

(deftest who-asks
  (let [not-signed-in (ask {:asks [:place :note-41 :text] :not-signed-in true :layers [:main :base]})
        system (ask {:asks [:place :note-41 :text] :asker :system :layers [:main :base]})
        turned-away (mapv (fn [thing] ((juxt :could-not :why) (ask thing)))
                          [{:asks [:place :note-41 :text] :layers [:main]}
                           {:asks [:place :note-41 :text] :asker :alice :not-signed-in true :layers [:main]}
                           {:asks [:place :note-41 :text] :asker (keyword (apply str (repeat 300 "a"))) :layers [:main]}])]
    (check "a person who is not signed in sees the base, which is everyone, and nothing of a members layer"
           [[{:layer :main :why :may-not-see}] [:value :base "a note"]]
           [(:left-out not-signed-in) ((juxt :holds :layer :value) (:place not-signed-in))])
    (check "the system is a who like any other: it holds no permission in a person's layer, and sees the base"
           [[{:layer :main :why :may-not-see}] :base] [(:left-out system) (:layer (:place system))])
    (check "a query with no asker, with both, and with an asker of 300 characters is turned away with its word"
           [[:read-the-query :no-one-asker] [:read-the-query :no-one-asker] [:read-the-query :id-over-the-limit]] turned-away)
    (shown "who asks is what the caller says: a person who is not signed in sees the base and no members layer," (pr-str (:left-out not-signed-in))
           "| :system is a who like any other | no asker, both, and an asker of 300 characters:" (pr-str (mapv second turned-away)))))

(deftest what-points-at-a-thing-through-layers
  (let [_ (doseq [layer [:near10 :plain10 :late10 :far10]] (make! layer :alice))
        points (fn [thing layers] (ask {:asks [:points-at :parent thing] :asker :alice :layers layers}))
        at (fn [answer e] (when-let [a (get (:places answer) [e :parent])] ((juxt :layer :stamp) a)))
        far-mark (mark! :far10 :alice #{:parent})
        near-mark (mark! :near10 :alice #{:parent})
        ;; a farther layer points, a nearer one moves, takes away, moves back
        far-7 (put! :alice :far10 [(fact :block-7 :parent :canvas-1)])
        from-far (points :canvas-1 [:near10 :far10])
        moved (put! :alice :near10 [(fact :block-7 :parent :canvas-2)])
        after-move [(points :canvas-1 [:near10 :far10]) (points :canvas-2 [:near10 :far10])]
        _ (put! :alice :near10 [{:e :block-7 :k :parent}])
        after-retract (points :canvas-1 [:near10 :far10])
        back (put! :alice :near10 [(fact :block-7 :parent :canvas-1)])
        after-back (points :canvas-1 [:near10 :far10])
        ;; a nearer layer that does not mark the key
        _ (put! :alice :far10 [(fact :block-8 :parent :canvas-1)])
        _ (put! :alice :plain10 [(fact :block-8 :parent :canvas-2)])
        plain-other (points :canvas-1 [:plain10 :far10])
        plain-same-act (put! :alice :plain10 [(fact :block-8 :parent :canvas-1)])
        plain-same (points :canvas-1 [:plain10 :far10])
        ;; a nearer fact that says the same thing and was written before its layer's mark
        early (put! :alice :late10 [(fact :block-9 :parent :canvas-1)])
        late-mark (mark! :late10 :alice #{:parent})
        _ (put! :alice :far10 [(fact :block-9 :parent :canvas-1)])
        before-mark (points :canvas-1 [:late10 :far10])
        _ (put! :alice :far10 [(fact :block-9 :parent :canvas-3)])
        far-moved-away (points :canvas-1 [:late10 :far10])
        alone (points :canvas-1 [:late10])
        ;; the mark off and on
        _ (mark! :late10 :alice #{})
        off (points :canvas-1 [:late10])
        again-mark (mark! :late10 :alice #{:parent})
        in-new-order (put! :alice :late10 [(fact :block-10 :parent :canvas-1)])
        on-again (points :canvas-1 [:late10])]
    (check "a farther layer points at the thing: the place comes from the farther layer's order"
           [[:far10 (:stamp far-7)] [{:layer :near10 :level-since (:stamp near-mark)} {:layer :far10 :level-since (:stamp far-mark)}]]
           [(at from-far :block-7) (:orders from-far)])
    (check "the nearer layer moves the block: it is hidden from what points at the first thing, and shown, from the nearer layer, for the second"
           [nil [:near10 (:stamp moved)]] [(at (first after-move) :block-7) (at (second after-move) :block-7)])
    (check "the nearer layer takes the block away, with a retract: it stays hidden" nil (at after-retract :block-7))
    (check "the nearer layer moves it back: it is shown from the nearer layer's own order" [:near10 (:stamp back)] (at after-back :block-7))
    (check "a nearer layer that does not mark the key and writes another thing hides the place"
           [nil {:layer :plain10 :not-marked true}] [(at plain-other :block-8) (first (:orders plain-other))])
    (check "when it writes the same thing, the place is shown from the nearer layer's row, with its layer, its fact and its stamp, under not-marked"
           [[:plain10 (:stamp plain-same-act)] [(:act-id plain-same-act) 0] {:layer :plain10 :not-marked true}]
           [(at plain-same :block-8) (:fact (get (:places plain-same) [:block-8 :parent])) (first (:orders plain-same))])
    (check "a nearer fact that says the same thing and was written before its layer's mark: the place comes from the nearer layer's row, whose stamp is not later than the stamp since which that layer's order is level"
           [[:late10 (:stamp early)] true {:layer :late10 :level-since (:stamp late-mark)}]
           [(at before-mark :block-9) (<= (:stamp early) (:stamp late-mark)) (first (:orders before-mark))])
    (check "then the farther layer moves the place away: no order holds it, and it is not in the answer, though the nearer fact still says the thing"
           [nil nil] [(at far-moved-away :block-9) (at alone :block-9)])
    (check "facts that were written before the mark are not given through that layer's own order: level-since is the stamp of the act that marked the key, and the answer holds no place"
           [{} [{:layer :late10 :level-since (:stamp late-mark)}]] [(:places alone) (:orders alone)])
    (check "the mark taken off: the layer says not-marked. Put on again: another stamp, and a fact that is written after it is in the order"
           [[{:layer :late10 :not-marked true}] [{:layer :late10 :level-since (:stamp again-mark)}] true
            {[:block-10 :parent] [(:act-id in-new-order) 0]}]
           [(:orders off) (:orders on-again) (< (:stamp late-mark) (:stamp again-mark))
            (into {} (map (fn [[place a]] [place (:fact a)])) (:places on-again))])
    (shown "what points at a thing through two layers, asked once: the farther layer's place" (pr-str (at from-far :block-7))
           "is hidden when a nearer layer moves the block or takes it away, and shown from the nearer layer when it moves it back," (pr-str (at after-back :block-7)))
    (shown "a nearer layer that does not mark the key hides the place with another thing, and with the same thing gives the place from its own row,"
           (pr-str (at plain-same :block-8)) "under" (pr-str (first (:orders plain-same)))
           "| a nearer fact from before its layer's mark gives the place with a stamp" (:stamp early) "that is not later than level-since" (:stamp late-mark)
           "| once the farther layer moves away, the place is not in the answer")
    (shown "the mark of a key: level-since is the stamp of the act that marked it," (:stamp late-mark) "; taken off, the layer says :not-marked; put on again it is"
           (:stamp again-mark) "and only facts written after it are in the order")))

(deftest the-calls-of-a-read-asked-once
  (let [_ (list! :n7 :alice [:m7 :f7])
        _ (give! :base :system :alice)
        counted (fn [query] (let [c (counting #(ask query))] [(:selects c) (:rounds c) (some? (:could-not (:result c)))]))
        place-3 (counted {:asks [:place :q7 :text] :asker :alice :layers [:n7 :m7 :f7]})
        from (counted {:asks [:place :q7 :text] :asker :alice :from-layer :n7})
        entity (counted {:asks [:entity :doc] :asker :alice :layers [:main]})
        base-run (counted {:asks [:run [:e-01] [:e-12]] :asker :alice :layers [:base]})
        points (counted {:asks [:points-at :parent :canvas-1] :asker :alice :layers [:near10 :plain10 :far10]})
        ;; 300 places, each checked in a nearer layer, in one call of 300 paths
        _ (doseq [layer [:o11n :o11f]] (make! layer :alice))
        _ (mark! :o11f :alice #{:parent})
        blocks (mapv #(keyword (format "w-%03d" %)) (range 300))
        _ (doseq [part (partition-all 150 blocks)]
            (put! :alice :o11f (mapv #(fact % :parent :canvas-9) part)))
        nearer (into {} (mapcat (fn [part]
                                  (let [wrote (put! :alice :o11n (mapv #(fact % :parent :canvas-9) part))]
                                    (map-indexed (fn [i block] [[block :parent] [(:act-id wrote) i]]) part)))
                                (partition-all 150 blocks)))
        many (counting #(ask {:asks [:points-at :parent :canvas-9] :asker :alice :layers [:o11n :o11f]}))
        ;; the same place asked once while a live read holds a push open on each of its sources
        watcher (live {:asks [:place :q7 :text] :asker :alice :layers [:n7 :m7 :f7]})
        watcher-holds (read-path/open-count (the-rp))
        beside-a-live-read (counted {:asks [:place :q7 :text] :asker :alice :layers [:n7 :m7 :f7]})
        same-answer? (= (dissoc (:now watcher) :leave-with) (ask {:asks [:place :q7 :text] :asker :alice :layers [:n7 :m7 :f7]}))
        _ (leave! watcher)]
    (check "a place through three layers: three calls, in one round" [3 [3] false] place-3)
    (check "the same place asked once while a live read holds a push open on each of its sources: the same three calls in one round, and the same answer. A read asked once reads the tables and takes no value of a push"
           [[3 [3] false] 1 true true]
           [beside-a-live-read (:live-reads watcher-holds) (<= 3 (:pushes watcher-holds)) same-answer?])
    (check "that live read has left, and none is open" {:live-reads 0 :pushes 0} (read-path/open-count (the-rp)))
    (check "the same from a layer: three calls in two rounds, the start layer's call first" [3 [1 2] false] from)
    (check "an entity of five keys: one call" [1 [1] false] entity)
    (check "a run across entities of the base: one call for each task, in one round, the rights riding in one of them" [tasks [tasks] false] base-run)
    (check "what points at a thing through three layers: the rights, the orders of the two layers that mark the key, the checks in the two nearer layers"
           [7 [3 2 2] false] points)
    (check "300 places of a farther order, each checked in a nearer layer by one call of 300 paths: each place is given from the nearer layer's own fact, so the values came in the paths' order"
           [300 true [2 1 1]]
           [(count (:places (:result many)))
            (every? (fn [[place fact-id]] (= fact-id (:fact (get (:places (:result many)) place)))) nearer)
            (:rounds many)])
    (shown "the calls of a read asked once, counted by functions of the proof's in the place of Rama's select and of the read path's rounds:"
           "a place through three layers" (pr-str (second place-3)) "| the same while a live read held" (:pushes watcher-holds) "pushes open on its sources" (pr-str (second beside-a-live-read))
           "| from a layer" (pr-str (second from)) "| an entity" (pr-str (second entity))
           "| a run across entities of the base" (pr-str (second base-run)) "| what points at a thing through three layers" (pr-str (second points)))
    (shown "a call of 300 paths gives its values in the paths' order: each of 300 places came with the fact of its own nearer row")))

(deftest a-very-large-run
  (let [small (:small @round)
        _ (make! :sized :alice)
        _ (doseq [[e n] [[:r03 3] [:r08 8] [:r12 12]]]
            (put! :alice :sized (mapv #(fact e (keyword (format "k%02d" %)) %) (range n))))
        counted (fn [query] (let [c (counting #(ask small query))]
                              [(:selects c) (count (:places (:result c))) (select-keys (:result c) [:could-not :what :most])]))
        run-3 (counted {:asks [:entity :r03] :asker :alice :layers [:sized]})
        run-8 (counted {:asks [:entity :r08] :asker :alice :layers [:sized]})
        run-12 (counted {:asks [:entity :r12] :asker :alice :layers [:sized]})
        unseen (counting #(ask small {:asks [:entity :r12] :asker :bob :layers [:sized]}))
        whole-layer (counted {:asks [:run :first :last] :asker :alice :layers [:sized]})
        _ (doseq [layer [:c12n :c12f]] (make! layer :alice))
        _ (mark! :c12f :alice #{:parent})
        _ (put! :alice :c12f (mapv #(fact (keyword (str "c-" %)) :parent :canvas-1) (range 7)))
        seven (counted {:asks [:points-at :parent :canvas-1] :asker :alice :layers [:c12n :c12f]})
        live-large (read-path/read-live small {:asks [:entity :r12] :asker :alice :layers [:sized]} (fn [_] nil))
        ;; a run of 8 places opens, and then grows to 11 under the open read
        grows (live small {:asks [:entity :r08] :asker :alice :layers [:sized]})
        opened-with (count (:places (:now grows)))
        _ (put! :alice :sized (mapv #(fact :r08 (keyword (format "g%02d" %)) %) (range 3)))
        grew (wait-until #(some :ended (changes grows)))
        after-it-grew [(read-path/open-count small) (leave! grows)]
        unknown {:gate :streaming :table :facts :layer :sized :routing-id :sized :path [:whole-table]}
        no-path (counting (fn [] [(#'read-path/path-of unknown (query/numbers nil) :whole)
                                  (#'read-path/select-async small unknown (#'read-path/path-of unknown (query/numbers nil) :whole))
                                  (#'read-path/path-of {:path [:run ":sized " ":sized!"]} (query/numbers nil) :no-such-step)]))]
    (check "with a first read of 4 places and at most 10 places for one run: a run of 3 places is one call"
           [1 3 {}] run-3)
    (check "a run of 8 places is three calls: the first read, the size by a bounded walk, the run whole" [3 8 {}] run-8)
    (check "a run of 12 places is two calls, and then the word, with no place"
           [2 0 {:could-not :serve-a-read-that-large :what :places-of-one-run :most 10}] run-12)
    (check "a run of 12 places in a layer that the asker may not see: one call, no size, no word; the layer is left out"
           [1 nil [{:layer :sized :why :may-not-see}]]
           [(:selects unseen) (:could-not (:result unseen)) (:left-out (:result unseen))])
    (check "the widest thing that a query can name, a whole layer, ends in the word within two calls"
           [2 0 {:could-not :serve-a-read-that-large :what :places-of-one-run :most 10}] whole-layer)
    (check "a read that needs 7 checks of a nearer layer, where one read may have 6, ends in the word"
           {:could-not :serve-a-read-that-large :what :checks-of-one-read :most 6} (nth seven 2))
    (check "kept live, the same run of 12 places ends in the word and holds no push"
           [{:could-not :serve-a-read-that-large :what :places-of-one-run :most 10} {:live-reads 0 :pushes 0}]
           [(select-keys live-large [:could-not :what :most]) (read-path/open-count small)])
    (check "a run of 8 places is kept live, and then an act takes it to 11, one more than one read may hold: the read ends with the word and why, and holds nothing"
           [8 {:ended {:could-not :keep-the-read-live :because :a-run-grew-too-large :what :places-of-one-run :most 10}}
            [{:live-reads 0 :pushes 0} :not-open]]
           [opened-with (when grew (last (changes grows))) after-it-grew])
    (check "a source of a kind that the read path builds no path for, and a step that it does not know: no path, and no call is made"
           [[nil [:no-path] nil] 0] [(:result no-path) (:selects no-path)])
    (shown "a run is bounded before it is read whole, by the calls of Rama's select: a run of 3 places" (first run-3) "call; of 8 places" (first run-8)
           "calls; of 12 places" (first run-12) "calls and" (pr-str (nth run-12 2)) "| in a layer that she may not see" (:selects unseen) "call and no word")
    (shown "the widest thing that a query can name, a whole layer, ends in a word within" (first whole-layer) "calls; a read that needs 7 checks, where 6 are allowed, gives"
           (pr-str (nth seven 2)) "| a source of a kind that the read path knows no path for makes no call")))

(deftest a-run-across-entities-of-the-base
  (let [query {:asks [:run [:e-01] [:e-12]] :asker :alice :layers [:base]}
        once (ask query)
        with-own (ask {:asks [:run [:b] [:base]] :asker :alice :layers [:base] :stores-own-facts true})
        without-own (ask {:asks [:run [:b] [:base]] :asker :alice :layers [:base]})
        read (live query)
        counts (read-path/open-count (the-rp))
        wrote (put! :system :base [(fact :e-07 :size 700)])
        got (wait-until 30000 #(seq (later read)))
        _ (settled)
        after (later read)
        _ (leave! read)]
    (check "asked once: the places of every task are in the answer" 12 (count (:places once)))
    (check "kept live: the same answer now, and one push for each task beside the push of the base's own rows"
           [(:places once) {:live-reads 1 :pushes (inc tasks)}] [(:places (:now read)) counts])
    (check "one write on one task gives one change, with the place that was written"
           [true [{:changed {[:e-07 :size] {:holds :value :value 700 :layer :base :fact [(:act-id wrote) 0] :stamp (:stamp wrote)}}}]]
           [(some? got) after])
    (check "a run that reaches the base's own entity, asked with the store's own facts: the base's depot row, which every task holds, is in the answer once; asked without, no row of the store's is"
           [:microbatch true]
           [(:value (get (:places with-own) [:base :softland.store/depot]))
            (not-any? (fn [[_ k]] (= "softland.store" (namespace k))) (keys (:places without-own)))])
    (shown "a run across entities of the base has one part on each task: asked once it gives" (count (:places once)) "places; kept live it holds"
           (:pushes counts) "pushes, one for each of" tasks "tasks and one for the base's own rows; one write on one task gave" (count after) "change"
           "| the base's depot row, which every task holds, is given once")))

(deftest each-word-kept-live
  (let [_ (doseq [layer [:l14 :n14]] (make! layer :alice))
        _ (mark! :l14 :alice #{:parent})
        observed
        (vec (for [[who layer wait] [[:alice :l14 15000] [:system :base 30000]]
                   [word asks inside outside] [[:place [:place :live-1 :text] (fact :live-1 :text "inside") (fact :live-2 :text "outside")]
                                               [:entity [:entity :live-e] (fact :live-e :a "inside") (fact :live-f :a "outside")]
                                               [:run [:run [:live-r1] [:live-r3]] (fact :live-r2 :a "inside") (fact :live-r4 :a "outside")]
                                               [:points-at [:points-at :parent :canvas-14] (fact :live-b :parent :canvas-14) (fact :live-c :parent :canvas-15)]]]
               (let [read (live {:asks asks :asker who :layers [layer]})
                     now (:now read)
                     wrote (put! who layer [inside])
                     got (wait-until wait #(seq (later read)))
                     _ (put! who layer [outside])
                     again (put! who layer [(assoc inside :v (if (= :points-at word) :canvas-14 "inside again"))])
                     got-again (wait-until wait #(< 1 (count (later read))))
                     _ (settled)
                     all (later read)
                     place [(:e inside) (:k inside)]
                     _ (leave! read)]
                 (check (str layer ", " word ", kept live: the answer now is what a read asked once gives; the first call of the function is that answer")
                        [true true] [(map? now) (= {:anew now} (first (changes read)))])
                 (check (str layer ", " word ": an act inside gives one change, with the place and the fact of that act; an act outside gives none; a second act inside gives the second change")
                        [true true 2 [(:act-id wrote) 0] [(:act-id again) 0]]
                        [(some? got) (some? got-again) (count all)
                         (:fact (get (:changed (first all)) place)) (:fact (get (:changed (second all)) place))])
                 [layer word (count all)])))
        ;; through layers: an act in a farther layer at a place that a nearer layer holds
        _ (put! :alice :n14 [(fact :under :text "the nearer holds this")])
        _ (put! :alice :l14 [(fact :under :text "f0")])
        asker (live {:asks [:place :under :text] :asker :alice :layers [:n14 :l14]})
        witness (live {:asks [:place :under :text] :asker :alice :layers [:l14]})
        _ (put! :alice :l14 [(fact :under :text "f1")])
        _ (put! :alice :l14 [(fact :under :text "f2")])
        seen (wait-until #(= "f2" (get-in (screen witness) [:place :value])))
        under (later asker)
        ;; twenty acts on one place in quick turn
        quick (live {:asks [:place :quick :text] :asker :alice :layers [:n14 :l14]})
        _ (dotimes [i 20] (put! :alice (if (even? i) :l14 :n14) [(fact :quick :text i)]))
        ended (wait-until #(= 19 (get-in (screen quick) [:place :value])))
        once (ask {:asks [:place :quick :text] :asker :alice :layers [:n14 :l14]})
        quick-calls (count (later quick))
        _ (run! leave! [asker witness quick])]
    (check "an act in a farther layer at a place that a nearer layer holds gives the asker nothing: the witness, who holds the same push, has the second of two such acts, and the asker's function was not called"
           [true []] [(some? seen) under])
    (check "twenty acts on one place in quick turn, in two layers by turns: the asker's screen ends at what a read asked once gives; no count of changes is asserted"
           [true (:place once)] [(some? ended) (:place (screen quick))])
    (check "no live read is left open" {:live-reads 0 :pushes 0} (read-path/open-count (the-rp)))
    (shown "each word kept live, for each gate: an act inside gives one change with that act's fact, an act outside gives none, a second act inside the second change:"
           (pr-str (mapv (fn [[layer word n]] [layer word n]) observed)))
    (shown "an act in a farther layer under a nearer layer's fact gave the asker" (count under) "calls, while a witness on the same push got both acts"
           "| twenty acts in quick turn on one place in two layers: the screen ends at the last value, after" quick-calls "calls")))

(deftest what-shapes-a-read-changes-under-it
  (let [_ (doseq [layer [:m15 :t15 :home15 :x15]] (make! layer :alice))
        _ (give! :t15 :alice :bob)
        _ (give! :t15 :alice :carol)
        _ (put! :alice :m15 [(fact :note :text "from m15")])
        _ (put! :alice :t15 [(fact :note :text "from t15")])
        place-of (fn [read] ((juxt :layer :value) (:place (screen read))))
        ;; (a) a permission given to an asker whose read left the layer out
        bob (live {:asks [:place :note :text] :asker :bob :layers [:m15 :t15]})
        bob-now (:now bob)
        _ (put! :alice :m15 [(fact :note :text "bob cannot see this")])
        _ (put! :alice :t15 [(fact :note :text "from t15, again")])
        _ (wait-until #(= "from t15, again" (second (place-of bob))))
        bob-before (changes bob)
        _ (give! :m15 :alice :bob :see)
        _ (wait-until #(= :m15 (first (place-of bob))))
        bob-joined (last (changes bob))
        ;; (b) a members layer goes to everyone, for an asker with no permission
        carol (live {:asks [:place :note :text] :asker :carol :layers [:m15 :t15]})
        carol-now (:now carol)
        _ (setting! :m15 :alice :everyone)
        _ (wait-until #(= :m15 (first (place-of carol))))
        carol-joined (last (changes carol))
        ;; ... and back to members: the layer is no longer rightful for her
        _ (setting! :m15 :alice :members)
        _ (wait-until #(seq (:left-out (screen carol))))
        carol-left (last (changes carol))
        carol-count (count (changes carol))
        _ (put! :alice :m15 [(fact :note :text "after carol left")])
        _ (wait-until #(= "after carol left" (second (place-of bob))))
        _ (settled)
        carol-after (count (changes carol))
        ;; (c) the start layer's list written again
        _ (list! :home15 :alice [:t15])
        home (live {:asks [:place :note :text] :asker :alice :from-layer :home15})
        _ (list! :home15 :alice [:m15 :t15])
        _ (wait-until #(= [:home15 :m15 :t15] (:layers (screen home))))
        home-wider (last (changes home))
        _ (list! :home15 :alice [])
        _ (wait-until #(= [:home15] (:layers (screen home))))
        home-alone (last (changes home))
        ;; (d) a key marked, a fact under it, the mark off, the mark on
        _ (put! :alice :x15 [(fact :comment-1 :on :paper-9)])
        marks (live {:asks [:points-at :on :paper-9] :asker :alice :layers [:x15]})
        marks-now (:now marks)
        marked (mark! :x15 :alice #{:on})
        _ (wait-until #(:level-since (first (:orders (screen marks)))))
        after-mark (last (changes marks))
        comment (put! :alice :x15 [(fact :comment-3 :on :paper-9)])
        _ (wait-until #(get (:places (screen marks)) [:comment-3 :on]))
        after-comment (last (changes marks))
        _ (mark! :x15 :alice #{})
        _ (wait-until #(:not-marked (first (:orders (screen marks)))))
        after-off (last (changes marks))
        marked-again (mark! :x15 :alice #{:on})
        _ (wait-until #(= (:stamp marked-again) (:level-since (first (:orders (screen marks))))))
        after-on (last (changes marks))
        ;; (e) for an asker who may not see a layer of her list: its list written again, and the asked key marked in it
        dave (live {:asks [:points-at :on :paper-9] :asker :dave :layers [:x15 :t15]})
        _ (setting! :t15 :alice :everyone)
        _ (wait-until #(= [{:layer :x15 :why :may-not-see}] (:left-out (screen dave))))
        dave-count (count (changes dave))
        _ (list! :x15 :alice [:t15])
        _ (mark! :x15 :alice #{})
        last-mark (mark! :x15 :alice #{:on})
        _ (wait-until #(= (:stamp last-mark) (:level-since (first (:orders (screen marks))))))
        _ (settled)
        dave-after (count (changes dave))
        _ (run! leave! [bob carol home marks dave])
        ;; (f) through three layers, a permission given in the nearest while the farther orders hold places
        _ (doseq [layer [:j15n :j15m :j15f]] (make! layer :alice))
        _ (doseq [layer [:j15n :j15m :j15f]] (mark! layer :alice #{:parent}))
        _ (give! :j15m :alice :erin)
        _ (give! :j15f :alice :erin)
        _ (put! :alice :j15f [(fact :f-1 :parent :canvas-1) (fact :f-2 :parent :canvas-1)])
        _ (put! :alice :j15m [(fact :m-1 :parent :canvas-1)])
        _ (put! :alice :j15n [(fact :n-1 :parent :canvas-1) (fact :f-2 :parent :canvas-2)])
        erin (live {:asks [:points-at :parent :canvas-1] :asker :erin :layers [:j15n :j15m :j15f]})
        erin-now (:now erin)
        _ (give! :j15n :alice :erin)
        _ (wait-until #(get (:places (screen erin)) [:n-1 :parent]))
        _ (settled)
        erin-later (later erin)
        erin-once (ask {:asks [:points-at :parent :canvas-1] :asker :erin :layers [:j15n :j15m :j15f]})
        _ (leave! erin)
        ;; (g) the base: its setting to members and back
        _ (put! :system :base [(fact :note-15 :text "in the base")])
        frank (live {:asks [:place :note-15 :text] :asker :frank :layers [:base]})
        alice-in-base (live {:asks [:place :note-15 :text] :asker :alice :layers [:base]})
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
    (check "an asker with no permission in the nearer layer: the layer is left out, a write in it gives her nothing, and the place comes from the farther layer"
           [[{:layer :m15 :why :may-not-see}] [:t15 "from t15"] true]
           [(:left-out bob-now) ((juxt :layer :value) (:place bob-now))
            (every? #(not= :m15 (:layer (first (vals (:changed %))))) (rest bob-before))])
    (check "a permission that says see is given to her under her open read: the whole answer comes anew, the layer joins, and the place comes from it"
           [[:m15 :t15] [] [:m15 "bob cannot see this"]]
           [(:layers (:anew bob-joined)) (:left-out (:anew bob-joined)) ((juxt :layer :value) (:place (:anew bob-joined)))])
    (check "a members layer goes to everyone under the open read of an asker with no permission: the layer joins, and the whole answer comes anew"
           [[{:layer :m15 :why :may-not-see}] [] :m15]
           [(:left-out carol-now) (:left-out (:anew carol-joined)) (:layer (:place (:anew carol-joined)))])
    (check "the layer goes back to members: she is told a whole answer that says that the layer is left out, with what now wins the place, and nothing of the layer's facts"
           {:layers [:m15 :t15] :left-out [{:layer :m15 :why :may-not-see}]
            :place (:place (ask {:asks [:place :note :text] :asker :carol :layers [:t15]}))}
           (:anew carol-left))
    (check "after the layer left, a write in it gives that asker nothing, and the other asker of the same push her change"
           [carol-count "after carol left"] [carol-after (second (place-of bob))])
    (check "the start layer's list is written again under an open read: layers join and leave by the new list, each time with the whole answer anew"
           [[[:home15 :m15 :t15] :m15] [[:home15] {:holds :no-fact}]]
           [[(:layers (:anew home-wider)) (:layer (:place (:anew home-wider)))]
            [(:layers (:anew home-alone)) (:place (:anew home-alone))]])
    (check "a key is marked under an open read of what points at a thing: before it the layer says not-marked; after it the whole answer comes anew with the stamp of the mark and no place, since the fact from before the mark is not in the order"
           [[{:layer :x15 :not-marked true}] {:layers [:x15] :left-out [] :places {} :orders [{:layer :x15 :level-since (:stamp marked)}]}]
           [(:orders marks-now) (:anew after-mark)])
    (check "a fact under the marked key gives one change; the mark taken off gives the answer anew with not-marked; put on again, another stamp, and the fact of before is not in the new order"
           [{[:comment-3 :on] {:holds :value :value :paper-9 :layer :x15 :fact [(:act-id comment) 0] :stamp (:stamp comment)}}
            [[{:layer :x15 :not-marked true}] {}]
            [[{:layer :x15 :level-since (:stamp marked-again)}] {}]]
           [(:changed after-comment)
            [(:orders (:anew after-off)) (:places (:anew after-off))]
            [(:orders (:anew after-on)) (:places (:anew after-on))]])
    (check "for an asker who may not see a layer of her list, that layer's list written again, the asked key's mark taken off and the key marked again give her nothing: the other asker, who holds the same push, was told the last of them"
           dave-count dave-after)
    (check "through three layers, a permission is given in the nearest while the farther orders hold places: the layer that joins gives a whole answer anew with its own place in it, and the place that it moved is hidden; the screen is what a read asked once gives"
           [#{[:f-1 :parent] [:f-2 :parent] [:m-1 :parent]} true (:places erin-once) #{[:f-1 :parent] [:m-1 :parent] [:n-1 :parent]}]
           [(set (keys (:places erin-now))) (every? :anew erin-later) (:places (screen erin)) (set (keys (:places erin-once)))])
    (check "the base's setting goes to members under the open read of an asker who holds no permission there: the base is left out, and she is given no fact"
           {:layers [:base] :left-out [{:layer :base :why :may-not-see}] :place {:holds :no-fact}} (:anew frank-out))
    (check "an asker who holds a permission in the base is given an answer anew that says what it said"
           [true true] [(every? :anew alice-same) (= (:now alice-in-base) (:anew (last alice-same)))])
    (check "while the base is members, a write in it gives the asker who may not see it nothing; back to everyone, the whole answer comes anew with that fact"
           [frank-count [:value "frank cannot see this" [(:act-id base-wrote) 0]]]
           [frank-after ((juxt :holds :value :fact) (:place (:anew frank-back)))])
    (check "no live read is left open" {:live-reads 0 :pushes 0} (read-path/open-count (the-rp)))
    (shown "what shapes a read changes under it, each as a whole answer anew: a permission given" (pr-str (select-keys (:anew bob-joined) [:layers :left-out]))
           "| a members layer to everyone, and back:" (pr-str (:left-out (:anew carol-left))) "| after the layer left, a write in it gave that asker"
           (- carol-after carol-count) "calls")
    (shown "the start layer's list written again:" (pr-str (:layers (:anew home-wider))) "then" (pr-str (:layers (:anew home-alone)))
           "| a key marked:" (pr-str (:orders (:anew after-mark))) "| a fact under it: one change | the mark off:" (pr-str (:orders (:anew after-off)))
           "| on again:" (pr-str (:orders (:anew after-on))))
    (shown "a list and a mark that are written in a layer that she may not see gave her" (- dave-after dave-count)
           "calls | a layer that joins over farther orders gave" (count erin-later) "whole answers anew, the last with its own place and what a read asked once gives"
           "| the base to members and back:" (pr-str (:left-out (:anew frank-out))) "then the fact again")))

(deftest one-push-for-one-thing
  (let [rp (:counted @round)
        _ (make! :t16 :alice)
        _ (give! :t16 :alice :bob)
        _ (put! :alice :t16 [(fact :note-41 :text "v0")])
        query (fn [who] {:asks [:place :note-41 :text] :asker who :layers [:t16]})
        kept (keeping-pushes
              (fn []
                (let [alice (live rp (assoc (query :alice) :with #{:writer}))
                      one (read-path/open-count rp)
                      entries (held rp)
                      bob (live rp (query :bob))
                      two (read-path/open-count rp)
                      wrote (put! :alice :t16 [(fact :note-41 :text "v1")])
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
        third (counting (fn [] (let [read (live rp (query :alice))] [(:value (:place (:now read))) (leave! read)])))
        ;; two open the same thing at once, 50 times
        together (counting
                  (fn []
                    (vec (for [_ (range 50)]
                           (let [start (CountDownLatch. 1)
                                 opened (mapv (fn [who] (future (.await start) (live rp (query who)))) [:alice :bob])
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
                           (let [first-read (live rp (query :alice))
                                 start (CountDownLatch. 1)
                                 leaving (future (.await start) (leave! first-read))
                                 joining (future (.await start) (live rp (query :bob)))
                                 _ (.countDown start)
                                 left @leaving
                                 joined @joining
                                 v (str "crossing " i)
                                 _ (put! :alice :t16 [(fact :note-41 :text v)])
                                 got (wait-until #(= v (get-in (screen joined) [:place :value])))
                                 whole? (no-entry-without-a-reader? rp)]
                             (leave! joined)
                             [left (some? got) whole?])))))
        closed (wait-until #(= (+ (:opens together) (:opens crossing)) (+ (:closes together) (:closes crossing))))]
    (check "one asker of a place in a members layer holds three pushes: the layer's own rows, her permission, the place"
           {:live-reads 1 :pushes 3} one)
    (check "a second asker of the same place joins the two pushes that are open and opens only his own permission"
           [{:live-reads 2 :pushes 4} 4] [two (count (:opened kept))])
    (check "a write to the place: each function is called once; the asker who asked for the writer is handed it, the other is not"
           [[1 1] [:alice nil] ["v1" "v1"] [[(:act-id wrote) 0] [(:act-id wrote) 0]]]
           [(mapv count told)
            (mapv (fn [changes] (:writer (first (vals (:changed (first changes)))))) told)
            (mapv (fn [changes] (:value (first (vals (:changed (first changes)))))) told)
            (mapv (fn [changes] (:fact (first (vals (:changed (first changes)))))) told)])
    (check "the first leaves: her permission's push is closed, and the two shared pushes stay; the last leaves: nothing is held; a read that has left is not open"
           [{:live-reads 1 :pushes 3} {:live-reads 0 :pushes 0} [:left :left :not-open]] [after-alice after-bob left])
    (check "each push that the read path opened is closed when its last reader has left: Rama says terminated of all four"
           [:terminated :terminated :terminated :terminated] statuses)
    (check "a third asker after the last one left opens new pushes, three, and gets a fresh value" [3 ["v1" :left]] [(:opens third) (:result third)])
    (check "what the read path holds for a live read and for a push: who looks at what, what each asked for, the pushes, and one mark; no field for a row, a value or an answer"
           [#{:query :tell :turn :sources :opened? :owed? :task-routing-ids} #{:opening :readers :called}]
           [(set (mapcat keys (vals (:reads entries)))) (set (mapcat keys (vals (:pushes entries))))])
    (check "50 times, two threads open the same thing at once: one opening for each thing, four pushes for the two, and each is closed when both have left"
           [(repeat 50 [{:live-reads 2 :pushes 4} true]) 200]
           [(map (fn [[counts whole?]] [counts whole?]) (:result together)) (:opens together)])
    (check "50 times, one joins while the last one leaves, and a write follows: the one who joined gets the write's change each time, so nobody was handed a push that is closed; the map of pushes holds no entry without a reader"
           (repeat 50 [:left true true]) (:result crossing))
    (check "Rama's opening of a push was called as often as its close, and nothing is held at the end"
           [true {:live-reads 0 :pushes 0}] [(some? closed) (read-path/open-count rp)])
    (shown "those who look at the same thing share one push: one asker of a place holds" (:pushes one) "pushes, two askers" (:pushes two)
           "; one write called each function once, and only the asker who asked for the writer was handed it; when the first left" (:pushes after-alice)
           "pushes stayed, when the last left none, and Rama says" (pr-str (distinct statuses)) "of each push")
    (shown "what the read path holds for a live read:" (pr-str (sort (set (mapcat keys (vals (:reads entries))))))
           "and for a push:" (pr-str (sort (set (mapcat keys (vals (:pushes entries)))))) "| read from the fields of its type; no field holds a row, a value or an answer")
    (shown "50 times two threads opened the same thing at once:" (:opens together) "openings of a push in all, four each time, and as many closes"
           "| 50 times one joined while the last one left, with a write after: the joiner got the write's change each time; openings"
           (:opens crossing) "and closes" (:closes crossing) "| counted by functions of the proof's in the place of Rama's")))

(deftest the-read-paths-own-bound-on-its-pushes
  (let [ipc (:ipc @round)
        m (:module @round)
        ;; a read path that may hold three pushes on one table under one routing id
        rp (read-path/open ipc m {:max-pushes-under-one-routing-id 3})
        level? (fn [] (= (:counts (held rp)) (counts-of-pushes rp)))
        _ (doseq [layer [:p87 :q87]] (make! layer :alice))
        _ (put! :alice :p87 (into [(fact :a :text "a") (fact :b :text "b")]
                                  (map #(fact (keyword (str "t" %)) :text %)) (range 8)))
        _ (put! :alice :q87 [(fact :a :text "a in q")])
        place (fn [layer e] {:asks [:place e :text] :asker :alice :layers [layer]})
        ;; one read fills the count of its layer: the layer's own rows, her permission, the place
        first-read (live rp (place :p87 :a))
        full [(read-path/open-count rp) (:counts (held rp)) (level?)]
        turned-away (counting #(read-path/read-live rp (place :p87 :b) (fn [_] nil)))
        after-turned-away [(read-path/open-count rp) (:counts (held rp)) (level?)]
        once (ask rp (place :p87 :b))
        same-thing (live rp (place :p87 :a))
        shared [(get-in same-thing [:now :place :value]) (read-path/open-count rp) (:counts (held rp)) (level?)]
        other-layer (live rp (place :q87 :a))
        apart [(get-in other-layer [:now :place :value]) (:counts (held rp)) (level?)]
        _ (leave! same-thing)
        _ (leave! first-read)
        after-left [(read-path/open-count rp) (:counts (held rp)) (level?)]
        now-room (live rp (place :p87 :b))
        with-room [(get-in now-room [:now :place :value]) (:counts (held rp)) (level?)]
        _ (leave! now-room)
        _ (leave! other-layer)
        emptied [(read-path/open-count rp) (held rp)]
        ;; eight threads, each a read of another place of the one layer, at once: 2 shared pushes and 8 places, where 3 may be held
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
        _ (doseq [layer [:n87 :f87]] (make! layer :alice))
        _ (mark! :f87 :alice #{:parent})
        _ (put! :alice :f87 [(fact :block-1 :parent :canvas-87)])
        points {:asks [:points-at :parent :canvas-87] :asker :alice :layers [:n87 :f87]}
        grows (live rp points)
        by-table [(count (:places (:now grows))) (:counts (held rp)) (level?)]
        _ (put! :alice :f87 [(fact :block-2 :parent :canvas-87)])
        ended (wait-until #(some :ended (changes grows)))
        after-ended [(read-path/open-count rp) (held rp) (leave! grows)]
        ;; the base: its own rows under the layer, a place under its entity
        in-base (live rp {:asks [:place :e-07 :size] :asker :alice :layers [:base]})
        base-counts [(some? (:now in-base)) (:counts (held rp)) (level?)]
        _ (leave! in-base)
        ;; the checks of one read that is kept live
        one-check (read-path/open ipc m {:max-checks-of-one-live-read 1})
        asked-once (ask one-check points)
        kept-live (counting #(read-path/read-live one-check points (fn [_] nil)))
        ;; an open read whose checks come to be more than one live read may need
        _ (doseq [layer [:n88 :f88]] (make! layer :alice))
        _ (mark! :f88 :alice #{:parent})
        _ (put! :alice :f88 [(fact :block-1 :parent :canvas-88)])
        outgrows (live one-check {:asks [:points-at :parent :canvas-88] :asker :alice :layers [:n88 :f88]})
        opened-with (count (:places (:now outgrows)))
        _ (put! :alice :f88 [(fact :block-2 :parent :canvas-88)])
        outgrown (wait-until #(some :ended (changes outgrows)))
        after-outgrown [(read-path/open-count one-check) (held one-check) (leave! outgrows)]]
    (check "a read path that may hold 3 pushes on one table under one routing id: one live read of a place in a members layer holds 3 there, the layer's own rows, her permission and the place; the count is the count of the map's entries"
           [{:live-reads 1 :pushes 3} {[:streaming :facts :p87] 3} true] full)
    (check "a live read of another place of that layer needs one push more: it is turned away at once with the read path's own word, which carries what was too many and the bound; no push was opened for it and none closed"
           [{:could-not :keep-more-reads-live :what :pushes-under-one-routing-id :most 3} 0 0]
           [(:result turned-away) (:opens turned-away) (:closes turned-away)])
    (check "after it was turned away, the read path holds what it held: the first read and its 3 pushes"
           [{:live-reads 1 :pushes 3} {[:streaming :facts :p87] 3} true] after-turned-away)
    (check "a read asked once is not held to that bound: it reads the same place" "b" (get-in once [:place :value]))
    (check "a second live read of the same thing opens: it shares the 3 pushes, and a push that is open already is not counted again"
           ["a" {:live-reads 2 :pushes 3} {[:streaming :facts :p87] 3} true] shared)
    (check "the pushes of another layer are under another routing id and are counted apart: a read through it opens while the first layer is full"
           ["a in q" {[:streaming :facts :p87] 3 [:streaming :facts :q87] 3} true] apart)
    (check "when the readers of a layer have left, its count is gone, and the read that was turned away opens"
           [[{:live-reads 1 :pushes 3} {[:streaming :facts :q87] 3} true]
            ["b" {[:streaming :facts :p87] 3 [:streaming :facts :q87] 3} true]]
           [after-left with-room])
    (check "when every reader has left, nothing is held and no count is left" [{:live-reads 0 :pushes 0} {:pushes {} :counts {} :reads {}}] emptied)
    (check "20 times, eight threads each open a read of another place of one layer at once, where 3 pushes may be held: one opens and seven are turned away with the word, the count stands at the bound, and nothing is held when the one has left"
           (repeat 20 [[1 {:keep-more-reads-live 7} {[:streaming :facts :p87] 3} true] {:pushes {} :counts {} :reads {}}])
           races)
    (check "the two tables of one layer are counted apart: what points at a thing through two layers holds 3 pushes on the nearer layer's facts, its own rows, her permission and the one check; 2 on the farther layer's facts; and 1 on its facts by value, the order"
           [1 {[:streaming :facts :n87] 3 [:streaming :facts :f87] 2 [:streaming :facts-by-value :f87] 1} true] by-table)
    (check "a second place comes into the farther order under that open read, so it comes to need a second check, one push more than the read path may hold on the nearer layer's facts: the read ends with the word and why, and holds nothing"
           [{:ended {:could-not :keep-the-read-live :because :no-room-for-a-push-it-came-to-need
                     :what :pushes-under-one-routing-id :most 3}}
            [{:live-reads 0 :pushes 0} {:pushes {} :counts {} :reads {}} :not-open]]
           [(when ended (last (changes grows))) after-ended])
    (check "in the base a layer's own rows are counted under the layer and a place under its entity, so two entities of the base are two counts, on one task or not"
           [true {[:microbatch :facts :base] 1 [:microbatch :facts :e-07] 1} true] base-counts)
    (check "the checks of one read that is kept live have a number of their own: where one such read may need 1 check, what points at a thing with two places in the farther order is given whole when asked once, and kept live it is turned away with the word; 5 pushes were opened, for the rights of the two layers and the one order, none for a check, and each was closed"
           [2 {:could-not :serve-a-read-that-large :what :checks-of-one-read :most 1} 5 5 {:live-reads 0 :pushes 0}]
           [(count (:places asked-once)) (:result kept-live) (:opens kept-live) (:closes kept-live) (read-path/open-count one-check)])
    (check "where one read kept live may need 1 check: a read of what points at a thing opens with one place in the farther order, so with one check; a second place comes into that order, so it comes to need two: the read ends with the word and why, and holds nothing"
           [1 {:ended {:could-not :keep-the-read-live :because :a-run-grew-too-large :what :checks-of-one-read :most 1}}
            [{:live-reads 0 :pushes 0} {:pushes {} :counts {} :reads {}} :not-open]]
           [opened-with (when outgrown (last (changes outgrows))) after-outgrown])
    (shown "the read path's own bound on its pushes, with 3 for one table under one routing id: a read that needs one more is turned away with"
           (pr-str (:result turned-away)) "after" (:opens turned-away) "openings of a push; a read asked once of the same place still answers; a second reader of an open thing is not counted again"
           "| eight threads at once, 20 times: each time one opened and seven were turned away"
           "| an open read that came to need one push more ended with" (pr-str (:because (:ended (last (changes grows))))))
    (shown "the counts, read from the read path's own memory: what points at a thing through two layers" (pr-str (nth by-table 1))
           "| a place of the base" (pr-str (nth base-counts 1)) "| at each moment the counts were the counts of the map's entries")
    (shown "kept live, a read that needs more checks than one live read may is turned away before a check is opened:" (pr-str (:result kept-live))
           "after" (:opens kept-live) "openings; asked once the same read gave" (count (:places asked-once)) "places")))

(deftest nothing-is-shown-that-is-not-in-the-tables
  (let [rp (:counted @round)
        _ (give! :t16 :alice :vera :see)
        asker (live rp {:asks [:place :note-41 :text] :asker :bob :layers [:t16]})
        refused (act! :vera :t16 [{:e :note-41 :k :text}])
        accepted (put! :alice :t16 [(fact :note-41 :text "after the refused act")])
        got (wait-until #(= "after the refused act" (get-in (screen asker) [:place :value])))
        all (later asker)
        facts (keep (fn [change] (:fact (or (:place (:anew change)) (first (vals (:changed change)))))) (changes asker))
        answers (mapv (fn [[act-id]] (:answer (door/answer-of (the-door) act-id (:digest (get @sent-things act-id))))) facts)
        _ (leave! asker)]
    (check "an act that the gate refuses at a place gives a live asker of that place nothing: she has the change of the accepted act that followed, and no other"
           [[:refused :permission-does-not-allow-acting] true 1 [(:act-id accepted) 0]]
           [((juxt :answer :reason) (:answer refused)) (some? got) (count all) (:fact (first (vals (:changed (first all)))))])
    (check "every value that she was given is of an act that a gate accepted, by the door's answer for the act of its fact"
           [true true] [(pos? (count answers)) (every? #{:accepted} answers)])
    (check "when every asker has left, the read path holds no reader, no push and no count of a push" {:pushes {} :counts {} :reads {}} (held rp))
    (shown "nothing is shown before it is in the tables: a refused act at a watched place gave the asker nothing; each of" (count answers)
           "values that she was given is of an accepted act; after every asker left the read path's three maps are" (pr-str (held rp)))))

(deftest reads-that-fail
  (let [_ (make! :l19 :alice)
        _ (put! :alice :l19 [(fact :note :text "v0")])
        query {:asks [:place :note :text] :asker :alice :layers [:l19]}
        never (fn [& _] (CompletableFuture.))
        threw (with-redefs [rama/compiled-foreign-select-async (fn [& _] (throw (ex-info "the proof made this select throw" {})))]
                (ask query))
        late (with-redefs [rama/compiled-foreign-select-async never]
               (let [began (System/currentTimeMillis)
                     got (ask (:short @round) query)]
                 [got (- (System/currentTimeMillis) began)]))
        ;; an opening that throws, and one that never comes back, under an open read
        _ (list! :l19 :alice [])
        failing (fn [rp stand-in]
                  (let [read (live rp {:asks [:place :note :text] :asker :alice :from-layer :l19})]
                    (with-redefs [rama/compiled-foreign-proxy-async stand-in]
                      (list! :l19 :alice [:main])
                      (let [ended (wait-until #(some :ended (changes read)))]
                        [ended (read-path/open-count rp) (leave! read) (.getName ^Thread (:thread (last @(:calls read))))]))))
        open-threw (failing (the-rp) (fn [& _] (throw (ex-info "the proof made this opening throw" {}))))
        _ (list! :l19 :alice [])
        open-late (failing (:short @round) never)
        ;; Rama ends a push
        kept (keeping-pushes #(live {:asks [:place :ended-by-rama :text] :asker :alice :layers [:l19]}))
        ended-read (:result kept)
        place-push (first (filter (fn [{:keys [options]}] (= :l19 (:pkey options))) (reverse (:opened kept))))
        _ ((:callback-fn (:options (last (:opened kept)))) nil (UngracefulTerminationDiff. (ex-info "the proof made this diff" {})) nil)
        store-ended (wait-until #(some :ended (changes ended-read)))
        after-store-ended [(read-path/open-count (the-rp)) (leave! ended-read)]
        ;; a value that Rama sends anew, after the first callback of a push: the proof makes Rama's own
        ;; diff for it and hands it to the callback of a run's push, once with the rows as they are and
        ;; once with one row that differs
        _ (put! :alice :l19 [(fact :sheet :a 1) (fact :sheet :b 2)])
        kept-anew (keeping-pushes #(live {:asks [:entity :sheet] :asker :alice :layers [:l19]}))
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
                                                         :asker :alice :layers [:l19]}
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
        _ (put! :alice :l19 [(fact :note :text "v1")])
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
  (let [_ (make! :l20 :alice)
        read (live {:asks [:entity :busy] :asker :alice :layers [:l20]})
        writers (mapv (fn [t] (future (vec (for [i (range 50)]
                                             (act! :alice :l20 [(fact :busy (keyword (str "k" t "-" i)) i)])))))
                      (range 4))
        sent (vec (mapcat deref writers))
        not-accepted (vec (remove :accepted? sent))
        ;; straight after the acks: through the read path that holds the open push, and through one that holds none
        beside-the-push (count (:places (ask {:asks [:entity :busy] :asker :alice :layers [:l20]})))
        by-a-select (count (:places (ask (:counted @round) {:asks [:entity :busy] :asker :alice :layers [:l20]})))
        _ (wait-until 30000 #(= 200 (count (:places (screen read)))))
        once (ask {:asks [:entity :busy] :asker :alice :layers [:l20]})
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
    (check "the three reading calls of Rama's client are named in the read path's one file and in no other file of the store's source"
           [#{"read_path.clj"} #{"read_path.clj"} #{"read_path.clj"}]
           (mapv holds ["foreign-pstate" "foreign-select" "foreign-proxy"]))
    (check "the read path's file names no append and no depot" [false false]
           [(str/includes? own "foreign-append!") (str/includes? own "foreign-depot")])
    (shown "one reader of facts in the store's source, by a search of" (count files) "files as text: foreign-pstate, foreign-select and foreign-proxy are in read_path.clj and in no other; it holds no foreign-append! and no foreign-depot"
           "| the proofs' own handles are the stated exception")))

(deftest the-order-of-the-tables-keys
  (let [_ (make! :o23 :alice)
        wide (String. (Character/toChars 0x1D49C))
        ids (mapv #(keyword (str "x" %)) ["z" "\u00E4" "\u044F" "\u6C34"])
        far-ids (mapv #(keyword (str "x" %)) ["\uFB00" wide])
        _ (put! :alice :o23 (mapv #(fact % :n (str %)) (concat ids far-ids)))
        _ (put! :alice :o23 [(fact :a :n 1) (fact :a! :n 2) (fact :ab :n 3)])
        run (fn [from to] (set (map first (keys (:places (ask {:asks [:run [from] [to]] :asker :alice :layers [:o23]}))))))
        entity (set (keys (:places (ask {:asks [:entity :a] :asker :alice :layers [:o23]}))))
        far-entities (mapv (fn [e] (set (keys (:places (ask {:asks [:entity e] :asker :alice :layers [:o23]}))))) far-ids)
        far-run ((juxt :could-not :why) (ask {:asks [:run [(nth ids 0)] [(nth far-ids 1)]] :asker :alice :layers [:o23]}))
        whole-layer (set (map first (keys (:places (ask {:asks [:run :first :last] :asker :alice :layers [:o23]})))))
        ;; a layer whose text is the start of another's, on one task
        task-of (fn [layer]
                  (first (keep-indexed (fn [i k]
                                         (when (rama/foreign-select-one (path/keypath (rows/place-key layer layer :softland.store/depot))
                                                                        (:streaming-facts @round) {:pkey k})
                                           i))
                                       (rtest/gen-hashing-index-keys tasks))))
        _ (make! :pfx :alice)
        neighbour (loop [i 0]
                    (let [layer (keyword (str "pfx!" i))]
                      (make! layer :alice)
                      (cond (= (task-of :pfx) (task-of layer)) layer
                            (< i 40) (recur (inc i)))))
        _ (put! :alice :pfx [(fact :mine :n 1)])
        _ (put! :alice neighbour [(fact :not-mine :n 2)])
        whole (set (keys (:places (ask {:asks [:run :first :last] :asker :alice :layers [:pfx]}))))]
    (check "entities whose ids hold letters outside ASCII, of Latin, Cyrillic and Han: a run between two of them gives the places that the table's order holds between its ends, which is the order by code point"
           [(set (subvec ids 1)) (set (subvec ids 2)) (set ids) (set (subvec ids 0 3))]
           [(run (nth ids 1) (nth ids 3)) (run (nth ids 2) (nth ids 3)) (run (nth ids 0) (nth ids 3)) (run (nth ids 0) (nth ids 2))])
    (check "an entity whose id holds a letter from U+E000 on, and one whose id holds a letter outside the first plane: each is read as an entity, and gives its own place and no other"
           [#{[(nth far-ids 0) :n]} #{[(nth far-ids 1) :n]}] far-entities)
    (check "a run between two entities that a caller names is turned away when an end holds such a letter: a range of Rama's between such ends was seen to give fewer places than the table holds, and the read path reads none"
           [:read-the-query :run-end-not-ordered] far-run)
    (check "the whole of the layer holds every entity, those with such letters too"
           true (every? whole-layer (concat ids far-ids [:a :a! :ab])))
    (check "Java's own order of Strings would put the letter outside the first plane before the one at U+FB00; the read path's order, and the table's, do not"
           [true false] [(neg? (compare wide "\uFB00")) (rows/before? wide "\uFB00")])
    (check "the run of an entity holds no place of an entity whose text begins with its text" #{[:a :n]} entity)
    (check "a layer whose text is the start of another's, on the same task: the whole of the first layer holds no place of the second"
           [true #{[:mine :n]}] [(some? neighbour) whole])
    (shown "the order of the store's own table is by code point: a run between entities with letters outside ASCII gives" (pr-str (sort (map str (run (nth ids 1) (nth ids 3)))))
           "| an entity whose id holds a letter from U+E000 on or outside the first plane is read as an entity and gives its own place; a run between two ends that a caller names, one of which holds such a letter, gives" (pr-str far-run)
           "| the entity :a holds no place of :a! or :ab | the whole of the layer :pfx holds no place of" neighbour
           ", which the proof found on the same task by a select of its own")))

(deftest a-function-that-leaves-or-asks-inside-its-call
  (let [_ (make! :l24 :alice)
        _ (put! :alice :l24 [(fact :note :text "v0")])
        rp (the-rp)
        query {:asks [:place :note :text] :asker :alice :layers [:l24]}
        ;; a function that leaves its own read inside its call
        own-handle (promise)
        own-calls (atom [])
        own (read-path/read-live rp query (fn [change]
                                            (swap! own-calls conj change)
                                            (when (:changed change)
                                              (swap! own-calls conj (read-path/leave rp @own-handle)))))
        _ (deliver own-handle (:leave-with own))
        _ (put! :alice :l24 [(fact :note :text "v1")])
        _ (wait-until #(some #{:left} @own-calls))
        _ (put! :alice :l24 [(fact :note :text "v2")])
        _ (settled)
        own-after [(count @own-calls) (last @own-calls) (read-path/open-count rp)]
        ;; a function that leaves another read
        other (live query)
        leaver (read-path/read-live rp query (fn [change] (when (:changed change) (leave! other))))
        _ (put! :alice :l24 [(fact :note :text "v3")])
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
                         (put! :alice :l24 [(fact :note :text (str "crossed " i))])
                         (wait-until #(<= (:live-reads (read-path/open-count rp)) 1))
                         (read-path/leave rp (:leave-with read-a))
                         (read-path/leave rp (:leave-with read-b))
                         (read-path/open-count rp))))
        ;; a function that asks inside its call
        asked (atom [])
        asking (counting
                (fn []
                  (let [read (read-path/read-live rp query
                                                  (fn [change]
                                                    (swap! asked conj
                                                           [(cond (:anew change) :anew (:changed change) :changed :else :ended)
                                                            (:could-not (read-path/read-once rp query))
                                                            (:could-not (read-path/read-live rp query (fn [_] nil)))])))]
                    (put! :alice :l24 [(fact :note :text "asked")])
                    (wait-until #(= 2 (count @asked)))
                    (put! :alice :l24 [(fact :note :text "asked again")])
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
    (check "no call was made to Rama for a read that was turned away: the selects are none, and the pushes are the three of the one read that opened"
           [0 3 [{:live-reads 1 :pushes 3} :left]] [(:selects asking) (:opens asking) (:result asking)])
    (shown "a function may leave inside its own call: its own read, " (pr-str (take-last 2 @own-calls)) ", and another read; 50 times two reads on one push each left the other and nothing stood still")
    (shown "a read asked from inside an asker's function is turned away at once:" (pr-str (distinct (map rest @asked)))
           "| selects made meanwhile:" (:selects asking) "| the read whose function asked went on and was told" (count @asked) "times")))

(deftest the-opening
  (let [_ (make! :l25 :alice)
        _ (give! :l25 :alice :bob)
        rp (the-rp)
        turns (vec (for [i (range 50)]
                     (let [e (keyword (str "opening-" i))
                           maker (future (dotimes [n 20] (act! :alice :l25 [(fact e :text n)])))
                           _ (Thread/sleep (long (rand-int 25)))
                           read (live {:asks [:place e :text] :asker :bob :layers [:l25]})
                           _ @maker
                           ended (wait-until #(= 19 (get-in (screen read) [:place :value])))
                           calls (changes read)]
                       (leave! read)
                       [(some? ended) (:anew (first calls)) (:now read) (get-in (:now read) [:place :value]) (count calls)])))
        ;; a function that stays 2 s, and a read of another asker meanwhile
        _ (put! :alice :l25 [(fact :slow :text "v0")])
        staying (atom nil)
        slow (read-path/read-live rp {:asks [:place :slow :text] :asker :alice :layers [:l25]}
                                  (fn [change] (when (:changed change) (reset! staying true) (Thread/sleep 2000) (reset! staying false))))
        _ (put! :alice :l25 [(fact :slow :text "v1")])
        _ (wait-until #(true? @staying))
        began (System/currentTimeMillis)
        meanwhile (try (ask {:asks [:place :slow :text] :asker :bob :layers [:l25]}) (catch Throwable t [:threw (str t)]))
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
  (let [_ (make! :l21 :alice)
        _ (put! :alice :l21 [(fact :note :text "before")])
        read (live {:asks [:place :note :text] :asker :alice :layers [:l21]})
        _ (rtest/update-module! (:ipc @round) module/Store)
        once (try (ask {:asks [:place :note :text] :asker :alice :layers [:l21]}) (catch Throwable t [:threw (str t)]))
        wrote (act! :alice :l21 [(fact :note :text "after")])
        outcome (wait-until 60000 #(or (some :ended (changes read))
                                       (when (= "after" (get-in (screen read) [:place :value])) :got-the-change)))
        once-after (try (ask {:asks [:place :note :text] :asker :alice :layers [:l21]}) (catch Throwable t [:threw (str t)]))
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
  (let [system-own [:system :base :base]
        _ (make! :base :system)
        _ (put! :system :base [(fact :stock :text "so that the system's stock holds locks")])
        before (put! :system :base [(fact :twice :text "before")])
        read (live {:asks [:place :twice :text] :asker :system :layers [:base]})
        ipc (:ipc @round)
        m (:module @round)
        place-key (rows/place-key :base :twice :text)
        send (fn [facts] (door/send-act (the-door) {:version 1 :who :system :layer :base :permission system-own
                                                    :session :read-path-proof :facts facts}))
        answer (fn [sent] (wait-until 90000 #(let [a (door/answer-of (the-door) (:act-id sent) (:digest sent))]
                                               (when (= :accepted (:answer a)) a))))
        notes-before (count @attempts)
        ;; one batch takes both acts; its first attempt fails after its rows were made
        _ (rtest/pause-microbatch-topology! ipc m "microbatch-gate")
        twice (send [(fact :twice :text "written by a batch that ran twice")])
        _ (send [(fact :base :softland.store/who-may-see :everyone)])
        _ (reset! throw-asked true)
        _ (rtest/resume-microbatch-topology! ipc m "microbatch-gate")
        twice-answer (answer twice)
        got (wait-until 60000 #(seq (later read)))
        ;; a second act at the same place, so that a second call for the first could not be on its way
        after (send [(fact :twice :text "after")])
        after-answer (answer after)
        _ (wait-until 60000 #(= "after" (get-in (screen read) [:place :value])))
        all (later read)
        notes (subvec @attempts notes-before)
        rows-at-the-place (fn [note] (mapv second (filter (fn [[k]] (= place-key k)) note)))
        with-a-row (filterv (comp seq rows-at-the-place) notes)
        first-row (last (rows-at-the-place (first with-a-row)))
        second-row (last (rows-at-the-place (second with-a-row)))
        of-the-batch (filterv (fn [note] (some #(rows/same-row? first-row %) (rows-at-the-place note))) with-a-row)
        _ (leave! read)]
    (check "the proof made one throw, and the batch that wrote the place had two attempts, each of which made a row for the place; the third note with a row for the place is the batch of the act that was sent after"
           [1 2 3] [@throws-made (count of-the-batch) (count with-a-row)])
    (check "the row that the second attempt made for the place is the first attempt's row, in every field, the stamp too"
           [true true] [(some? first-row) (rows/same-row? first-row second-row)])
    (check "under the open read the function was called once for that place, with the first attempt's row: the stamp of the row that the first attempt made, and the fact of the act; the next call is of the act that was sent after"
           [true
            [{:changed {[:twice :text] {:holds :value :value "written by a batch that ran twice" :layer :base
                                        :fact [(:act-id twice) 0] :stamp (:stamp first-row)}}}
             {:changed {[:twice :text] {:holds :value :value "after" :layer :base
                                        :fact [(:act-id after) 0] :stamp (:stamp after-answer)}}}]]
           [(and (some? got) (some? twice-answer) (= (:stamp twice-answer) (:stamp first-row)) (< (:stamp before) (:stamp first-row)))
            all])
    (shown "a batch of the base ran a second time under an open read of a place that it writes, on a cluster of its own in which the proof replaced three pure functions of the gate's batch and threw once:"
           (count of-the-batch) "attempts made a row for the place, the same row in every field; the asker's function was called" (count all)
           "times in all, once for that act with the stamp" (:stamp first-row) "of the first attempt's row, and once for the act after it")))

(defn- on-a-cluster
  "Starts an in-process cluster, launches the module `Store` in it, opens
  the proof's door and read paths, calls f, and closes the cluster."
  [f]
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc module/Store {:tasks tasks :threads 2 :workers 2})
    (open-round! ipc)
    (f)))

(defn- the-checks-of-the-first-cluster
  "Runs the checks in the order in which the proof tells them. Later checks
  build on the layers that earlier ones made. The checks that count the
  calls of a read asked once come before any live read is opened; the
  module is started again last."
  []
  (the-read-path-is-opened)
  (a-thing-that-is-no-query)
  (each-word-once-for-each-gate)
  (the-four-things-that-a-place-can-hold)
  (a-part-of-a-row-is-given-only-when-asked)
  (the-stores-own-facts)
  (through-layers)
  (a-layer-that-is-left-out)
  (who-asks)
  (what-points-at-a-thing-through-layers)
  (the-calls-of-a-read-asked-once)
  (a-very-large-run)
  (a-run-across-entities-of-the-base)
  (each-word-kept-live)
  (what-shapes-a-read-changes-under-it)
  (one-push-for-one-thing)
  (the-read-paths-own-bound-on-its-pushes)
  (nothing-is-shown-that-is-not-in-the-tables)
  (reads-that-fail)
  (the-function-and-its-threads)
  (one-reader-in-the-source)
  (the-order-of-the-tables-keys)
  (a-function-that-leaves-or-asks-inside-its-call)
  (the-opening)
  (the-first-thing-a-function-is-given)
  (the-module-is-started-again))

(defn test-ns-hook
  "Runs the proof on its two clusters, one after the other: every check but
  one on the module as the store defines it; then, with three functions of
  the gate's batch replaced, the check of a batch that runs a second time.
  The store's own functions are put back whatever that check does. What the
  proof keeps of the first cluster means nothing on the second, so it is
  emptied between them."
  []
  (on-a-cluster the-checks-of-the-first-cluster)
  (reset! permissions {})
  (reset! sent-things {})
  (reset! live-reads [])
  (put-the-proofs-functions!)
  (try
    (on-a-cluster a-batch-that-runs-a-second-time)
    (finally
      (put-the-stores-functions-back!))))

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
