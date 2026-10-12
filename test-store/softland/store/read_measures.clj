(ns softland.store.read-measures
  "The measures of the read build: a run that is no proof. It prints what it
  measured and holds nothing to a number. One command, from the repository
  root:

    clojure -M:store-module -m softland.store.read-measures

  It takes about half an hour. With the one argument `small` it runs each
  shape with a small load, in a few minutes, to see that the run works:

    clojure -M:store-module -m softland.store.read-measures small

  Each number is on a line that starts with MEASURED, with what it is a
  number of. A set of times is printed as its count, its middle, its
  slowest 1 in 100 (the 99th of 100 when the times are put in order) and
  its slowest, in milliseconds. It exits 0 unless it cannot run.

  What is run, on Rama's in-process cluster with 4 tasks, 2 threads and 2
  workers. Every act goes in through the door, and every read through the
  read path.

    M1  a gate under 100 acts a second: one maker sends acts of one fact, a
        string of 100 bytes under a plain key, each to another place of one
        layer. The streaming gate, timed to the return of `send-act`, which
        is the streaming depot's ack: once with the key not marked and once
        with it marked. And the base, timed to the moment at which the
        door's wait gives \"accepted\"
    M2  M1's first shape with 1, 100 and 1,000 live reads open on the
        layer, of one asker who holds a permission in it: each read on
        another place that is not written; and each on another run that
        holds the written places
    M3  the opening of a canvas of 5,000 facts: asked as the run across the
        5,000 entities and as what points at the canvas, once and kept
        live, through one layer and through three; with a streaming layer
        as the farthest, and with the base as the farthest
    M4  how late a push is: one live read of one place, which one maker
        changes 10 times a second; in a streaming layer and in the base;
        and, in a streaming layer, of one place of the facts by value
    M5  what is not measured here

  What a number here is. The cluster runs in the process of the measure, on
  one machine, with every row in memory and no network. So a time is a
  floor for a round trip and says little of a seek on a disk. A real
  cluster, the base on one, a network and a second node are not measured.

  State. The run starts one in-process cluster, launches the module `Store`
  in it and closes it at the end. It opens one door and two read paths, the
  second with a wait of 120 s for the openings of M3. It stands in for
  Rama's two reading calls with functions that count, for M3. It reads the
  clock and the heap."
  (:require [clojure.string :as str]
            [com.rpl.rama :as rama]
            [com.rpl.rama.test :as rtest]
            [softland.store.door :as door]
            [softland.store.module :as module]
            [softland.store.read-path :as read-path])
  (:import [java.util.concurrent ExecutorService Executors TimeUnit]))

(def ^:private loads
  "The loads of the measures, by their names: the plan's own, and a small
  one for a first try."
  {:full  {:acts 3000 :warm 500 :reads [1 100 1000] :canvas 5000 :runs 7 :dropped 2 :late-acts 500 :late-warm 20}
   :small {:acts 200 :warm 50 :reads [1 10 50] :canvas 600 :runs 3 :dropped 1 :late-acts 40 :late-warm 5}})

(defonce ^:private round (atom nil))

(defn- measured
  "Prints one line of what was measured."
  [& xs]
  (println (apply print-str "MEASURED" xs))
  (flush))

(defn- ms
  "Takes a span in nanoseconds. Gives it in milliseconds, with three
  decimals."
  [nanos]
  (/ (Math/round (/ (double nanos) 1000.0)) 1000.0))

(defn- times
  "Takes times in nanoseconds. Gives their count, their middle, their
  slowest 1 in 100 and their slowest, as text."
  [nanos]
  (if (empty? nanos)
    "count 0"
    (let [sorted (vec (sort nanos))
          n (count sorted)
          at (fn [share] (nth sorted (max 0 (dec (long (Math/ceil (* share n)))))))]
      (str "count " n " | middle " (ms (at 0.5)) " ms | slowest 1 in 100 " (ms (at 0.99))
           " ms | slowest " (ms (peek sorted)) " ms"))))

;; ------------------------------------------------------------- through the door

(defn- send!
  "Sends one act of a who into a layer through the door, under a
  permission. Gives what `send-act` gave."
  [who layer permission facts]
  (door/send-act (:door @round) {:version 1 :who who :layer layer :permission permission
                                 :session :read-measures :facts facts}))

(defn- accepted!
  "Sends one act and waits until the door's wait gives its answer. Gives
  the answer. It stops the run when the act is not accepted: the measures
  cannot run on a layer that is not as they made it."
  [who layer permission facts]
  (let [sent (send! who layer permission facts)
        answer (when-not (:could-not sent)
                 (door/answer-of (:door @round) (:act-id sent) (:digest sent) 60000))]
    (when-not (= :accepted (:answer answer))
      (throw (ex-info "the measures: an act of the set-up was not accepted" {:sent sent :answer answer})))
    answer))

(defn- make!
  "Makes a streaming layer whose holder is `:maker`. Gives the maker's
  permission in it."
  [layer]
  (let [own [:maker layer layer]]
    (accepted! :maker layer own [{:e layer :k :softland.store/depot :v :streaming}
                                 {:e :maker :k :softland.store/permission :v own}])
    own))

(def ^:private system-own "The system's permission in the base." [:system :base :base])

(defn- text-of-100-bytes
  "A string of 100 bytes that says which act it is of."
  [i]
  (let [head (str "act " i " ")]
    (str head (apply str (repeat (- 100 (count head)) "x")))))

(defn- paced
  "Calls f once for each i below n, at start + i x every-ms and no sooner,
  with i and the moment at which the call was due, in nanoseconds. One
  thread: a call that is late makes the next ones late."
  [n every-ms f]
  (let [start (System/nanoTime)]
    (dotimes [i n]
      (let [due (+ start (* (long i) (long every-ms) 1000000))
            wait (- due (System/nanoTime))]
        (when (pos? wait)
          (Thread/sleep (quot wait 1000000) (int (rem wait 1000000))))
        (f i due)))))

;; ------------------------------------------------------------------------ M1

(defn- streaming-gate-times
  "One maker sends acts of one fact into a streaming layer, 100 a second,
  after acts that are not counted. Takes the layer, the maker's permission,
  how many acts, how many are not counted, and a function from the act's
  number to its fact. Gives {:from-due :from-call}, each the times to the
  return of `send-act`, in nanoseconds."
  [layer permission n warm fact-of]
  (dotimes [i warm]
    (send! :maker layer permission [(fact-of (- i warm))]))
  (let [from-due (atom []) from-call (atom [])]
    (paced n 10
           (fn [i due]
             (let [called (System/nanoTime)
                   sent (send! :maker layer permission [(fact-of i)])
                   returned (System/nanoTime)]
               (when-not (:could-not sent)
                 (swap! from-due conj (- returned due))
                 (swap! from-call conj (- returned called))))))
    {:from-due @from-due :from-call @from-call}))

(defn- another-place
  "The fact of an act of M1: a string of 100 bytes at a place of its own."
  [i]
  {:e (keyword (str "p" (+ 100000 i))) :k :text :v (text-of-100-bytes i)})

(defn- m1-streaming
  "M1, the streaming gate: once with the key not marked, once with it
  marked."
  [{:keys [acts warm]}]
  (let [layer :m1
        own (make! layer)
        plain (streaming-gate-times layer own acts warm another-place)
        _ (accepted! :maker layer own [{:e layer :k :softland.store/marked-keys :v #{:text}}])
        marked (streaming-gate-times layer own acts warm (fn [i] (another-place (+ 50000 i))))]
    (doseq [[what got] [["the key not marked" plain] ["the key marked, so each act also writes a row of the facts by value" marked]]]
      (measured "M1 the streaming gate, 100 acts a second, one fact of 100 bytes each to another place," what
                "| from the act's due moment to the return of send-act:" (times (:from-due got)))
      (measured "M1 the streaming gate, the same," what "| from the call of send-act to its return:" (times (:from-call got))))))

(defn- m1-base
  "M1, the base: the same acts into the base, each timed to the moment at
  which the door's wait gives \"accepted\". The sender sends at its pace;
  each act's answer is waited for on a thread of its own, by the door's
  wait, which reads the answer every 200 ms: a time can be up to that much
  later than the answer's row."
  [{:keys [acts warm]}]
  (let [^ExecutorService waiters (Executors/newCachedThreadPool)
        from-due (atom []) from-call (atom []) not-accepted (atom 0)
        send-one (fn [i due count?]
                   (let [called (System/nanoTime)
                         sent (send! :system :base system-own [(another-place i)])]
                     (if (:could-not sent)
                       (swap! not-accepted inc)
                       (.submit waiters
                                ^Runnable
                                (fn []
                                  (let [answer (door/answer-of (:door @round) (:act-id sent) (:digest sent) 120000)
                                        now (System/nanoTime)]
                                    (if (= :accepted (:answer answer))
                                      (when count?
                                        (swap! from-due conj (- now due))
                                        (swap! from-call conj (- now called)))
                                      (swap! not-accepted inc))))))))]
    (paced warm 10 (fn [i due] (send-one (- i warm) due false)))
    (paced acts 10 (fn [i due] (send-one i due true)))
    (.shutdown waiters)
    (.awaitTermination waiters 180 TimeUnit/SECONDS)
    (measured "M1 the base, 100 acts a second, the same acts | from the act's due moment to the moment at which the door's wait gives accepted:"
              (times @from-due))
    (measured "M1 the base, the same | from the call of send-act to that moment:" (times @from-call)
              "| acts that got no accepted answer:" @not-accepted)
    (measured "M1 the base | how many acts a batch held: not told: no function gives a stamp's batch")))

;; ------------------------------------------------------------------------ M2

(defn- m2
  "M2: M1's first shape with live reads open on the layer, in two shapes
  and three counts. The asker holds a permission in the layer, whose
  setting is \"members\", so n reads hold n + 2 pushes: the n fact sources,
  the layer's own rows and her permission.

  In the shape of runs the acts go round 256 places, and not each to a
  place of its own: each run holds every written place, so with places
  that only grow, 1,000 runs would come to hold 3,500 rows each in the
  memory of this one process."
  [{:keys [acts warm reads]}]
  (doseq [shape [:places :runs]
          n reads]
    (let [layer (keyword (str "m2-" (name shape) "-" n))
          own (make! layer)
          asker-permission [:asker layer layer own]
          _ (accepted! :maker layer own [{:e :asker :k :softland.store/permission :v asker-permission}])
          told (atom 0)
          rp (:read-path @round)
          opened (vec (for [j (range n)]
                        (read-path/read-live
                         rp
                         {:asks (if (= :places shape)
                                  [:place (keyword (str "unwritten-" j)) :text]
                                  [:run [:p] [(keyword (str "q" (+ 100000 j)))]])
                          :asker :asker :layers [layer]}
                         (fn [change] (when (:changed change) (swap! told inc))))))
          words (count (filter :could-not opened))
          counts (read-path/open-count rp)
          fact-of (if (= :places shape)
                    another-place
                    (fn [i] {:e (keyword (str "p" (+ 100000 (mod i 256)))) :k :text :v (text-of-100-bytes i)}))
          got (streaming-gate-times layer own acts warm fact-of)
          began (System/nanoTime)
          _ (run! #(read-path/leave rp (:leave-with %)) (remove :could-not opened))
          left (- (System/nanoTime) began)]
      (measured "M2 the streaming gate, 100 acts a second, with" n "live reads open on the layer,"
                (if (= :places shape)
                  "each on another place that is not written"
                  "each on another run that holds the written places, which go round 256 places")
                "|" (pr-str counts) "| reads that did not open:" words
                "| from the act's due moment to the return of send-act:" (times (:from-due got)))
      (measured "M2 the same," n "live reads," (name shape) "| from the call of send-act to its return:" (times (:from-call got))
                "| changes that the askers were told:" @told "| leaving them all took" (ms left) "ms"))))

;; ------------------------------------------------------------------------ M3

(defn- counting
  "Calls f while the measure stands in for Rama's two reading calls with
  functions that count and then call Rama. Gives {:result :selects
  :opens}."
  [f]
  (let [selects (atom 0) opens (atom 0)
        real-select rama/compiled-foreign-select-async
        real-open rama/compiled-foreign-proxy-async]
    (with-redefs [rama/compiled-foreign-select-async (fn [& args] (swap! selects inc) (apply real-select args))
                  rama/compiled-foreign-proxy-async (fn [& args] (swap! opens inc) (apply real-open args))]
      (let [result (f)]
        {:result result :selects @selects :opens @opens}))))

(defn- heap-in-use
  "Gives the heap in use after a collection, in megabytes."
  []
  (System/gc)
  (Thread/sleep 200)
  (let [runtime (Runtime/getRuntime)]
    (quot (- (.totalMemory runtime) (.freeMemory runtime)) (* 1024 1024))))

(defn- block
  "The id of the i-th block of a canvas."
  [i]
  (keyword (format "blk-%05d" i)))

(defn- canvas!
  "Writes the canvas of M3 with one farthest layer: `canvas` blocks with ids
  that are neighbours, each with one fact under the marked key `:parent`
  that points at the canvas; and two nearer layers, which mark the key too
  and each hold 50 facts at places of the canvas, 25 that say another thing
  and 25 that say the canvas. Takes the farthest layer, who writes into it
  and under which permission, the names of the two nearer layers, and the
  count of blocks. Gives the three layers, nearest first."
  [far far-who far-permission [near mid] canvas]
  (accepted! far-who far far-permission [{:e far :k :softland.store/marked-keys :v #{:parent}}])
  (doseq [part (partition-all 250 (range canvas))]
    (accepted! far-who far far-permission (mapv (fn [i] {:e (block i) :k :parent :v :canvas-1}) part)))
  (doseq [[layer offset] [[near 0] [mid 100]]]
    (let [own (make! layer)]
      (accepted! :maker layer own [{:e layer :k :softland.store/marked-keys :v #{:parent}}])
      (accepted! :maker layer own
                 (vec (for [i (range 50)]
                        {:e (block (+ offset (* 7 i))) :k :parent :v (if (even? i) :canvas-2 :canvas-1)})))))
  [near mid far])

(defn- m3-shapes
  "The eight shapes of M3 for one canvas: asked as the run across the
  entities and as what points at the canvas; once and kept live; through
  the farthest layer alone and through the three. Each shape is run `runs`
  times, of which the first `dropped` are not counted."
  [what [_ _ far :as three] asker {:keys [canvas runs dropped]}]
  (let [rp (:patient @round)
        last-block (block (dec canvas))]
    (doseq [[asked asks] [["the run across the entities" [:run [(block 0)] [last-block]]]
                          ["what points at the canvas" [:points-at :parent :canvas-1]]]
            [through layers] [["one layer" [far]] ["three layers" three]]
            live? [false true]]
      (let [query {:asks asks :asker asker :layers layers}
            before (heap-in-use)
            turns (vec (for [turn (range runs)]
                         (let [began (System/nanoTime)
                               counted (counting #(if live?
                                                    (read-path/read-live rp query (fn [_] nil))
                                                    (read-path/read-once rp query)))
                               took (- (System/nanoTime) began)
                               got (:result counted)
                               answer (if live? (:now got) got)
                               held (read-path/open-count rp)
                               heap (when (= turn (dec runs)) (heap-in-use))
                               left-began (System/nanoTime)
                               _ (when (:leave-with got) (read-path/leave rp (:leave-with got)))
                               left (- (System/nanoTime) left-began)]
                           {:took took :left left :selects (:selects counted) :opens (:opens counted)
                            :pushes (:pushes held) :heap heap
                            :places (count (:places answer)) :word (:could-not got)})))
            counted (subvec turns dropped)
            last-turn (peek turns)]
        (measured "M3" what "|" asked (if live? "kept live" "asked once") "through" through
                  "| from the call to its return:" (times (map :took counted))
                  "| each time:" (pr-str (mapv (comp ms :took) counted))
                  (if (some :word turns) (str "| a word came in the place of an answer: " (pr-str (distinct (keep :word turns)))) ""))
        (measured "M3 the same |" (:places last-turn) "places in the answer | calls of Rama's select:" (:selects last-turn)
                  "| pushes that it opened:" (:opens last-turn) "| pushes held:" (:pushes last-turn)
                  "| heap in use before" before "MB and after the last opening" (:heap last-turn) "MB"
                  (if live? (str "| leave took " (pr-str (mapv (comp ms :left) counted)) " ms") ""))))))

(defn- m3
  "M3: the opening of a canvas, with a streaming layer as the farthest and
  with the base as the farthest."
  [load]
  (let [far-own (make! :c-far)
        streaming (canvas! :c-far :maker far-own [:c-near :c-mid] (:canvas load))
        base (canvas! :base :system system-own [:b-near :b-mid] (:canvas load))]
    (m3-shapes "a canvas in a streaming layer" streaming :maker load)
    (m3-shapes "a canvas in the base" base :maker load)))

;; ------------------------------------------------------------------------ M4

(defn- m4-one
  "M4 for one depot and one kind of table: one live read, and one maker who
  changes what it reads 10 times a second. Each act is timed from the call
  of `send-act`, and from its return, to the first call of the asker's
  function that tells that act or a later one: two acts of one batch of
  the base at one place give one change, with the later one, so an act can
  have no call of its own.

  `table` is `:facts` or `:facts-by-value`. For `:facts` the read is of one
  place, and each act writes another value there: the push is on a row of
  the facts table, which holds the value's bytes. For `:facts-by-value` the
  read is of what points at a thing under a marked key, and each act writes
  the same thing again at one place: the push is on a run of the facts by
  value, whose rows hold no bytes, and each act changes the one row of it.
  An act is known in a change by its act-id, which the place-answer's fact
  holds. What must hold for `:facts-by-value`: the layer marks `:late-on`."
  [what table who layer permission {:keys [late-acts late-warm]}]
  (let [rp (:read-path @round)
        told (atom [])
        number-of (atom {})
        by-value? (= :facts-by-value table)
        read (read-path/read-live rp
                                  {:asks (if by-value? [:points-at :late-on :late-thing] [:place :late :n])
                                   :asker who :layers [layer]}
                                  (fn [change]
                                    (when-let [[act-id] (:fact (first (vals (:changed change))))]
                                      (swap! told conj [act-id (System/nanoTime)]))))
        sent (atom [])]
    (paced (+ late-warm late-acts) 100
           (fn [i _]
             (let [called (System/nanoTime)
                   got (send! who layer permission
                              [(if by-value? {:e :late :k :late-on :v :late-thing} {:e :late :k :n :v i})])]
               (when-not (:could-not got)
                 (swap! number-of assoc (:act-id got) i)
                 (swap! sent conj [i called (System/nanoTime)])))))
    (let [deadline (+ (System/currentTimeMillis) 60000)
          last-number (first (peek @sent))]
      (while (and (< (System/currentTimeMillis) deadline)
                  (not (some (fn [[act-id]] (= last-number (get @number-of act-id))) @told)))
        (Thread/sleep 50)))
    (read-path/leave rp (:leave-with read))
    (let [told (keep (fn [[act-id at]] (when-let [i (get @number-of act-id)] [i at])) @told)
          timed (for [[i called returned] @sent
                      :when (<= late-warm i)
                      :let [[v at] (first (filter (fn [[v]] (<= i v)) told))]
                      :when at]
                  {:from-call (- at called) :from-return (- at returned) :own? (= v i)})]
      (measured "M4 how late a push is," what ", one live read, 10 acts a second | from the call of send-act to the first call of the asker's function that tells that act or a later one:"
                (times (map :from-call timed)))
      (measured "M4 the same," what "| from the return of send-act, which is"
                (if (= :base layer) "the microbatch depot's append-ack" "the streaming depot's ack") ", to that call:"
                (times (map :from-return timed))
                "| a time below zero says that the function was called before send-act returned"
                "| acts told by a call of their own:" (count (filter :own? timed)) "| only by a later act's:" (count (remove :own? timed))
                "| acts that were sent and never told:" (- (count (filter (fn [[i]] (<= late-warm i)) @sent)) (count timed))))))

(defn- m4
  "M4: a place of the facts table, in a streaming layer and in the base; and
  a place of the facts by value in a streaming layer, so that a push on rows
  that hold a value's bytes and a push on rows that hold none are measured
  under the same load."
  [load]
  (let [own (make! :m4)]
    (accepted! :maker :m4 own [{:e :m4 :k :softland.store/marked-keys :v #{:late-on}}])
    (m4-one "of a place of the facts table, in a streaming layer" :facts :maker :m4 own load)
    (m4-one "of a place of the facts table, in the base" :facts :system :base system-own load)
    (m4-one "of a place of the facts by value, in a streaming layer" :facts-by-value :maker :m4 own load)))

;; ----------------------------------------------------------------------- the run

(defn -main
  "Starts the cluster, launches the module, runs the measures, closes the
  cluster and exits. With the argument `small` each shape has a small
  load."
  [& args]
  (let [load-name (if (= "small" (first args)) :small :full)
        load (get loads load-name)]
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc module/Store {:tasks 4 :threads 2 :workers 2})
      (let [m (rama/get-module-name module/Store)]
        (reset! round {:door (door/open ipc m)
                       :read-path (read-path/open ipc m)
                       :patient (read-path/open ipc m {:wait-ms 120000})}))
      (measured "the measures of the read build, with the load" (pr-str load-name) (pr-str load)
                "| Rama's in-process cluster, 4 tasks, 2 threads, 2 workers, one machine, every row in memory, no network | JVM heap limit"
                (quot (.maxMemory (Runtime/getRuntime)) (* 1024 1024)) "MB")
      (accepted! :system :base system-own [{:e :base :k :softland.store/depot :v :microbatch}
                                           {:e :system :k :softland.store/permission :v system-own}])
      (accepted! :system :base system-own [{:e :maker :k :softland.store/permission :v [:maker :base :base system-own]}])
      (m1-streaming load)
      (m1-base load)
      (m2 load)
      (m3 load)
      (m4 load)
      (measured "M5 not measured here: a real cluster, the base on one, a network, a second node"))
    (println (str/join " " ["the measures ran to their end;" "they prove nothing"]))
    (shutdown-agents)
    (System/exit 0)))
