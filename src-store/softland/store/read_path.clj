;; Before changing this file, read the plan of this part again:
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-read-path.md
;; and src/proposal/permissions-2026-11-27/CONTRACT.md §2.4
;; and src/proposal/permissions-2026-11-27/state/loop-0d.md

(ns softland.store.read-path
  "The read path: the one piece of code by which a fact comes back out of
  the store to a caller in the runtime's process, once or kept live. Of
  Rama's client it uses `foreign-pstate`, `foreign-query`,
  `foreign-select-async`, `foreign-invoke-query-async`,
  `foreign-proxy-async` and `close!`. Facts are read straight from the
  gates' PStates; an asker's rights in a lane are read as one atomic unit
  on the task by the module's `lane-rights` query topology, and a live read
  trusts that query's standing only while its open Rama pushes match the
  query's `:read-set` stamps entry for entry (`softland.store.read-path.sources`).

  No call throws for what a caller gave or for what Rama did, and a read
  that failed has never said \"no fact\". What must hold: the first thing
  that a call is given is a read path that `open` gave. Who asks is what the
  caller says, and `:system` is a who like any other. Every wait for Rama
  has the read path's own bound, `:wait-ms`. The read path tries nothing a
  second time by itself: the caller asks again.

  State. The read path owns no durable state, and it writes nothing: it
  appends to no depot and keeps no record of who read what. It borrows
  thirteen handles of Rama's client; the opener owns the cluster handle,
  and nothing closes the read path. Its one memory is `held`, an atom of
  three maps, in the memory of one process:

    :pushes  a source -> {:opening a delay around the one call that opens
             its push, :readers the ids of the live reads that hold it,
             :called a mark that its first callback came}
    :counts  a gate, a table and a routing id -> how many entries of
             `:pushes` are under them (`counted-under`)
    :reads   a read's id -> its query, its function, its turn, the set of
             the sources that it holds, whether it is opened, its mark
             (`softland.store.read-path.answer/mark-of`), and `:rights`,
             each lane's per-read `lane-rights` snapshot and single-flight
             state (never shared across reads or `read-once`)"
  (:require [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [softland.store.read-path.acts :as acts]
            [softland.store.read-path.answer :as answer]
            [softland.store.read-path.lanes :as lanes]
            [softland.store.read-path.query :as query]
            [softland.store.read-path.sources :as sources]
            [softland.store.read-path.words :as words])
  (:import [com.rpl.rama.diffs DestroyedDiff Diff Diff$Processor KeyDiff KeyDiff$Processor
            KeyRemoveDiff KeyRemoveDiff$Processor ResyncDiff UngracefulTerminationDiff]
           [java.io Writer]
           [java.util ArrayList]
           [java.util.concurrent CompletableFuture ExecutionException TimeUnit TimeoutException]
           [java.util.concurrent.atomic AtomicBoolean AtomicLong]
           [java.util.function BiConsumer Consumer]))

;; ------------------------------------------------- the read path as a value

(defn- printed
  [module]
  (str "#<read-path " module ">"))

(deftype ReadPath [module
                   streaming-facts
                   microbatch-facts
                   streaming-facts-by-value
                   microbatch-facts-by-value
                   streaming-permissions-by-holder
                   microbatch-permissions-by-holder
                   streaming-line
                   microbatch-line
                   streaming-things
                   microbatch-things
                   streaming-task
                   microbatch-task
                   lane-rights-query
                   numbers
                   held
                   next-id]
  Object
  (toString [_] (printed module)))

(defmethod print-method ReadPath [^ReadPath read-path ^Writer w]
  (.write w (.toString read-path)))

(defmethod print-dup ReadPath [^ReadPath read-path ^Writer w]
  (.write w (.toString read-path)))

(alter-meta! #'->ReadPath assoc
             :private true
             :doc "An opened read path. Its thirteen handles of Rama's client are
  borrowed; `next-id` is the count from which the ids of its live reads are
  made.")

(deftype LiveRead [held id]
  Object
  (toString [_] (str "#<live-read " id ">")))

(defmethod print-method LiveRead [^LiveRead live-read ^Writer w]
  (.write w (.toString live-read)))

(defmethod print-dup LiveRead [^LiveRead live-read ^Writer w]
  (.write w (.toString live-read)))

(alter-meta! #'->LiveRead assoc
             :private true
             :doc "The handle of a live read. It holds the `held` atom of the read
  path that opened the read, so that a handle of another read path is told
  apart.")

(def ^:private max-rounds
  "How many rounds a read is given, by how it is asked, before it stops with
  `:get-an-answer-within-its-wait`."
  {:once 6 :live 24})

(defn- now-ms
  "The JVM's steady clock in milliseconds, to bound a wait."
  []
  (quot (System/nanoTime) 1000000))

;; ------------------------------------------------ the calls of Rama's client

(defn- what-threw
  [^Throwable t]
  (when (instance? InterruptedException t)
    (.interrupt (Thread/currentThread)))
  {:threw (.getName (class t)) :said (.getMessage t)})

(defn- attempt
  [f]
  (try [:ok (f)]
       (catch Throwable t [:threw (what-threw t)])))

(defn- within
  [^CompletableFuture future ms]
  (try [:ok (.get future (max 0 (long ms)) TimeUnit/MILLISECONDS)]
       (catch TimeoutException _ [:late])
       (catch ExecutionException e [:threw (what-threw (or (.getCause e) e))])
       (catch Throwable t [:threw (what-threw t)])))

(defn- pstate-of
  [^ReadPath read-path source]
  (case [(:gate source) (:table source)]
    [:streaming :facts]                  (.-streaming-facts read-path)
    [:microbatch :facts]                 (.-microbatch-facts read-path)
    [:streaming :facts-by-value]         (.-streaming-facts-by-value read-path)
    [:microbatch :facts-by-value]        (.-microbatch-facts-by-value read-path)
    [:streaming :permissions-by-holder]  (.-streaming-permissions-by-holder read-path)
    [:microbatch :permissions-by-holder] (.-microbatch-permissions-by-holder read-path)
    [:streaming :line]                   (.-streaming-line read-path)
    [:microbatch :line]                  (.-microbatch-line read-path)
    [:streaming :things]                 (.-streaming-things read-path)
    [:microbatch :things]                (.-microbatch-things read-path)
    [:streaming :task]                   (.-streaming-task read-path)
    [:microbatch :task]                  (.-microbatch-task read-path)
    nil))

(defn- window
  [from to n]
  (if (or (sources/outside-the-agreed-range? from) (sources/outside-the-agreed-range? to))
    (path/sorted-map-range-from from {:max-amt n})
    [(path/sorted-map-range-from from {:max-amt n}) (path/sorted-map-range from to)]))

(defn- path-of
  "The one function that makes a path of Rama's. Nil for a source or a step
  of any other kind; the read path then makes no call."
  [source numbers step]
  (let [[kind a b n] (:path source)
        window? (contains? #{:window :by-who} kind)
        ok? (and (string? a)
                 (or (contains? #{:row :last-stamp} kind) (string? b))
                 (or (not window?) (and (int? n) (pos? (long n)))))]
    (when ok?
      (case [kind step]
        [:row :whole]        (path/keypath a)
        [:own-rows :whole]   (path/sorted-map-range a b)
        [:holders :whole]    (path/sorted-map-range a b)
        [:holders :size]     (let [limit (inc lanes/max-holder-permissions)]
                               (if (lanes/holder-range-agreed? a b)
                                 [(path/sorted-map-range-from a {:max-amt limit})
                                  (path/sorted-map-range a b)]
                                 (path/sorted-map-range-from a {:max-amt limit})))
        [:run :whole]        (path/sorted-map-range a b)
        [:run :first]        [(path/sorted-map-range-from a {:max-amt (inc (:run-start-places numbers))})
                              (path/sorted-map-range a b)]
        [:run :count]        [(path/sorted-map-range-from a {:max-amt (inc (:run-start-places numbers))})
                              (path/sorted-map-range a b)
                              (path/view count)]
        [:run :size]         [(path/sorted-map-range-from a {:max-amt (inc (:max-places-of-one-run numbers))})
                              (path/multi-path (path/view count) [path/LAST path/FIRST])]
        [:window :whole]     (window a b n)
        [:last-stamp :whole] (path/keypath :last-stamp a)
        [:by-who :whole]     [(path/keypath :by-who) (window a b n)]
        nil))))

(defn- select-async
  [read-path source the-path]
  (let [pstate (pstate-of read-path source)]
    (if (or (nil? pstate) (nil? the-path))
      [:no-path]
      (attempt (fn [] (rama/foreign-select-async the-path pstate {:pkey (:routing-id source)}))))))

(defn- invoke-rights-raw
  [^ReadPath read-path routing-id lane question asker session]
  (attempt (fn []
             (rama/foreign-invoke-query-async
              (.-lane-rights-query read-path)
              routing-id lane question asker session))))

(defn- close-push!
  [push]
  (try (rama/close! push)
       (catch Throwable _ nil)))

;; ------------------------------------------------------ a read asked once

(defn- late
  [^ReadPath read-path]
  (words/word :get-an-answer-within-its-wait {:waited-ms (:wait-ms (.-numbers read-path))}))

(defn- too-large
  [^ReadPath read-path]
  (words/word :serve-a-read-that-large
              {:what :places-of-one-run :most (:max-places-of-one-run (.-numbers read-path))}))

(defn- too-many-perms
  [lane]
  (words/word :too-many-permissions {:lane lane :most lanes/max-holder-permissions}))

(defn- no-room
  [^ReadPath read-path]
  {:what :pushes-under-one-routing-id
   :most (:max-pushes-under-one-routing-id (.-numbers read-path))})

(defn- gather
  "Waits for async futures that were all sent already, up to `deadline`."
  [read-path sent deadline]
  (loop [todo sent values []]
    (if-let [[outcome got] (first todo)]
      (case outcome
        :ok (let [[outcome got] (within got (- deadline (now-ms)))]
              (case outcome
                :ok    (recur (rest todo) (conj values got))
                :late  [:word (late read-path)]
                :threw [:word (words/word :reach-the-store got)]))
        :threw [:word (words/word :reach-the-store got)]
        [:word (words/word :reach-the-store
                           {:said "a source of a kind for which the read path makes no path; no call was made"})])
      [:ok values])))

(defn- query-rights-now
  "Invokes `lane-rights` in parallel for `pairs` (`[[routing-id lane] ...]`)
  and waits up to `deadline`."
  [^ReadPath read-path pairs question asker session deadline]
  (if (empty? pairs)
    [:ok {}]
    (let [sent (mapv (fn [[routing-id lane]]
                       (invoke-rights-raw read-path routing-id lane question asker session))
                     pairs)
          [outcome got] (gather read-path sent deadline)]
      (cond
        (= :word outcome)
        [:word got]

        (some nil? got)
        [:word (words/word :reach-the-store {:said "lane-rights gave nothing"})]

        :else
        [:ok (zipmap (map second pairs) got)]))))

(defn- select-now
  "Reads the sources of the calls of one round
  (`softland.store.read-path.sources/calls-for-once`), each call one round
  trip to one task, all sent before any is waited for."
  [^ReadPath read-path calls deadline]
  (let [numbers (.-numbers read-path)
        step-of (fn [source] (if (= :run (first (:path source))) :first :whole))
        reads-of (fn [call] (vec (distinct (map sources/read-of (:sources call)))))
        sent (mapv (fn [call]
                     (let [reads (reads-of call)
                           paths (mapv #(path-of % numbers (step-of %)) reads)]
                       (if (some nil? paths)
                         [:no-path]
                         (select-async read-path (first reads) (apply path/multi-path paths)))))
                   calls)
        [outcome got] (gather read-path sent deadline)]
    (if (= :word outcome)
      [:word got]
      (if (every? true? (map (fn [call values] (= (count (reads-of call)) (count values))) calls got))
        [:ok (into {} (mapcat (fn [call values]
                                (let [by-read (zipmap (reads-of call) values)]
                                  (map (fn [source] [source (get by-read (sources/read-of source))])
                                       (:sources call))))
                              calls got))]
        [:word (words/word :reach-the-store
                           {:said "a call gave another count of values than it had paths"})]))))

(defn- run-now
  "Reads whole the runs whose first read was not the whole run, each after a
  bounded walk on its task has given its size. What must hold: the asker's
  standing in each source's lane is `:yes`."
  [^ReadPath read-path run-sources deadline]
  (let [numbers (.-numbers read-path)
        ask (fn [step] (mapv #(select-async read-path % (path-of % numbers step)) run-sources))
        [outcome sizes] (gather read-path (ask :size) deadline)]
    (cond
      (= :word outcome)
      [:word sizes]

      (some true? (map (fn [source size] (sources/over-the-limit? size (nth (:path source) 2) numbers))
                       run-sources sizes))
      [:word (too-large read-path)]

      :else
      (let [[outcome runs] (gather read-path (ask :whole) deadline)]
        (cond
          (= :word outcome) [:word runs]
          (some #(< (:max-places-of-one-run numbers) (count (first %))) runs) [:word (too-large read-path)]
          :else [:ok (zipmap run-sources (map first runs))])))))

(defn- read-acts-once
  "The body of `read-once` for a query of `:acts`."
  [^ReadPath read-path query]
  (let [numbers (.-numbers read-path)
        deadline (+ (now-ms) (:wait-ms numbers))
        lane (lanes/home query)]
    (loop [round 0 known {} snapshot nil]
      (let [{:keys [answer fault missing needs-rights too-many-permissions]}
            (acts/reading query known numbers snapshot)]
        (cond
          answer answer
          too-many-permissions (words/word :too-many-permissions too-many-permissions)
          fault (words/word :find-the-line-whole fault)
          (and (empty? missing) (not needs-rights))
          (words/word :reach-the-store {:said "a read of acts wanted nothing and gave no answer"})
          (<= (:once max-rounds) round) (late read-path)
          :else
          (let [rights-fut (when needs-rights
                             (invoke-rights-raw read-path lane lane :acts (:asker query) (:session query)))
                [sel-out sel-got] (if (seq missing)
                                    (select-now read-path (sources/calls-for-once missing numbers) deadline)
                                    [:ok {}])
                [r-out r-vals] (if (or (= :word sel-out) (nil? rights-fut))
                                 [:ok nil]
                                 (gather read-path [rights-fut] deadline))]
            (cond
              (= :word sel-out) sel-got
              (= :word r-out) r-vals
              (and rights-fut (nil? (first r-vals)))
              (words/word :reach-the-store {:said "lane-rights gave nothing"})
              :else
              (recur (inc round)
                     (merge known sel-got)
                     (if rights-fut (first r-vals) snapshot)))))))))

;; ------------------------------------------------- the pushes, and their sharing

(declare on-callback)

(defn- open-push
  "The body of the delay of an entry of `:pushes`: the one call that opens
  the push of a source. Gives a future that ends, and never with a throw,
  in `{:push push}` or in `{:word word}`."
  [^ReadPath read-path source ^AtomicBoolean called]
  (let [numbers (.-numbers read-path)
        out (CompletableFuture.)
        give! (fn [x] (.complete out x))
        threw! (fn [info] (give! {:word (words/word :reach-the-store info)}))
        large! (fn [] (give! {:word (too-large read-path)}))
        many-perms! (fn [] (give! {:word (too-many-perms (:lane source))}))
        kind (first (:path source))
        run? (= :run kind)
        holders? (= :holders kind)
        after (fn [[outcome got] then]
                (case outcome
                  :ok (.whenComplete ^CompletableFuture got
                                     (reify BiConsumer
                                       (accept [_ value thrown]
                                         (try (if thrown (threw! (what-threw thrown)) (then value))
                                              (catch Throwable t (threw! (what-threw t)))))))
                  :threw (threw! got)
                  (threw! {:said "a source of a kind for which the read path makes no path; no call was made"})))
        open! (fn []
                (let [pstate (pstate-of read-path source)
                      the-path (path-of source numbers :whole)]
                  (after (if (or (nil? pstate) (nil? the-path))
                           [:no-path]
                           (attempt (fn []
                                      (rama/foreign-proxy-async
                                       the-path pstate
                                       {:pkey (:routing-id source)
                                        :callback-fn (fn [new diff old]
                                                       (on-callback read-path source called new diff old))}))))
                         (fn [push]
                           (cond
                             (and run? (< (:max-places-of-one-run numbers) (count @push)))
                             (do (close-push! push) (large!))

                             (and holders?
                                  (< lanes/max-holder-permissions
                                     (count (sources/keys-of-run source @push))))
                             (do (close-push! push) (many-perms!))

                             :else
                             (when-not (give! {:push push})
                               (close-push! push)))))))]
    (try
      (let [size-then-open! (fn []
                              (after (select-async read-path source (path-of source numbers :size))
                                     (fn [size]
                                       (if (sources/over-the-limit? size (nth (:path source) 2) numbers)
                                         (large!)
                                         (open!)))))]
        (cond
          holders?
          (let [[_ a b] (:path source)]
            (after (select-async read-path source (path-of source numbers :size))
                   (fn [got]
                     (if (< lanes/max-holder-permissions
                            (count (lanes/clean-holder-run a b (first got))))
                       (many-perms!)
                       (open!)))))

          (not run?)
          (open!)

          (sources/first-read-holds? source)
          (after (select-async read-path source (path-of source numbers :count))
                 (fn [[n]] (if (sources/run-is-whole? n numbers) (open!) (size-then-open!))))

          :else
          (size-then-open!)))
      (catch Throwable t (threw! (what-threw t))))
    (.completeOnTimeout out {:word (late read-path)} (long (:wait-ms numbers)) TimeUnit/MILLISECONDS)
    out))

(defn- held-now
  [^ReadPath read-path]
  @(.-held read-path))

(defn- read-of
  [read-path id]
  (get-in (held-now read-path) [:reads id]))

(defn- snapshots-of
  [read]
  (into {}
        (keep (fn [[lane st]]
                (when-let [s (:snapshot st)]
                  [lane s])))
        (:rights read)))

(defn- given
  [entry]
  (let [opening (:opening entry)]
    (when (realized? opening)
      (let [^CompletableFuture future @opening]
        (when (.isDone future)
          (.getNow future nil))))))

(defn- counted-under
  [source]
  [(:gate source) (:table source) (:routing-id source)])

(defn- one-fewer
  [counts source]
  (let [under (counted-under source)
        n (dec (long (get counts under 1)))]
    (if (pos? n) (assoc counts under n) (dissoc counts under))))

(defn- join!
  [^ReadPath read-path id sources]
  (when (seq sources)
    (let [most (:max-pushes-under-one-routing-id (.-numbers read-path))
          [_ after]
          (swap-vals! (.-held read-path)
                      (fn [held]
                        (if-not (contains? (:reads held) id)
                          held
                          (let [to-open (remove #(contains? (:pushes held) %) sources)
                                counts (reduce (fn [counts source]
                                                 (update counts (counted-under source) (fnil inc 0)))
                                               (:counts held)
                                               to-open)]
                            (if (some #(< most (get counts (counted-under %))) to-open)
                              held
                              (reduce (fn [held source]
                                        (let [pushes (update (:pushes held) source
                                                             (fn [entry]
                                                               (if entry
                                                                 (update entry :readers conj id)
                                                                 (let [called (AtomicBoolean. false)]
                                                                   {:opening (delay (open-push read-path source called))
                                                                    :readers #{id}
                                                                    :called called}))))]
                                          (-> held
                                              (assoc :pushes pushes)
                                              (update-in [:reads id :sources] conj (key (find pushes source))))))
                                      (assoc held :counts counts)
                                      sources))))))]
      (cond
        (not (contains? (:reads after) id))
        nil

        (not-every? #(contains? (get-in after [:reads id :sources]) %) sources)
        :past-the-bound

        :else
        (do (doseq [source sources]
              (when-let [entry (get-in after [:pushes source])]
                (force (:opening entry))))
            :joined)))))

(defn- close-when-given!
  [entry]
  (try
    (let [^CompletableFuture opening (force (:opening entry))]
      (.thenAccept opening (reify Consumer
                             (accept [_ got]
                               (when-let [push (:push got)]
                                 (close-push! push))))))
    (catch Throwable _ nil)))

(defn- leave-sources!
  [^ReadPath read-path id sources whole?]
  (let [[before after]
        (swap-vals! (.-held read-path)
                    (fn [held]
                      (if-let [read (get-in held [:reads id])]
                        (let [leaving (if (= :all sources) (:sources read) (filter (:sources read) sources))
                              [pushes counts] (reduce (fn [[pushes counts] source]
                                                        (if-let [entry (get pushes source)]
                                                          (let [readers (disj (:readers entry) id)]
                                                            (if (empty? readers)
                                                              [(dissoc pushes source) (one-fewer counts source)]
                                                              [(assoc pushes source (assoc entry :readers readers)) counts]))
                                                          [pushes counts]))
                                                      [(:pushes held) (:counts held)]
                                                      leaving)
                              held (assoc held :pushes pushes :counts counts)]
                          (if whole?
                            (update held :reads dissoc id)
                            (update-in held [:reads id :sources] #(reduce disj % leaving))))
                        held)))
        read (get-in before [:reads id])]
    (when read
      (doseq [source (if (= :all sources) (:sources read) sources)
              :let [entry (get-in before [:pushes source])]
              :when (and entry
                         (contains? (:readers entry) id)
                         (not (contains? (:pushes after) source)))]
        (close-when-given! entry)))
    (some? read)))

(defn- known-of
  [read-path read]
  (try
    (let [pushes (:pushes (held-now read-path))]
      (persistent!
       (reduce (fn [known source]
                 (if-let [push (:push (some-> (get pushes source) given))]
                   (assoc! known source @push)
                   known))
               (transient {})
               (:sources read))))
    (catch Throwable _ nil)))

;; ----------------------------------------------------- the steps of a live read

(def ^:private ^ThreadLocal telling
  (ThreadLocal.))

(defn- telling?
  []
  (some? (.get telling)))

(defn- call-marked
  [f change]
  (let [before (.get telling)]
    (.set telling Boolean/TRUE)
    (try (f change)
         :returned
         (catch Throwable _ :threw)
         (finally (if before (.set telling before) (.remove telling))))))

(defn- end-read!
  [read-path id because carries]
  (when-let [{:keys [turn]} (read-of read-path id)]
    (locking turn
      (let [{:keys [tell opened?]} (read-of read-path id)]
        (when (and (leave-sources! read-path id :all true) opened?)
          (call-marked tell
                       {:ended (words/word :keep-the-read-live (assoc carries :because because))}))))))

(defn- tell!
  [read-path id change]
  (when-let [read (read-of read-path id)]
    (when (= :threw (call-marked (:tell read) change))
      (end-read! read-path id :its-function-threw nil))))

(defn- settle!
  "Recomputes `known` and `reading` (re-checking `:read-set` equality on
  every step), joins newly wanted pushes, and leaves unwanted pushes until
  moves stabilize."
  [^ReadPath read-path id]
  (loop [round 0 joined #{} left #{}]
    (when-let [read (read-of read-path id)]
      (when-let [known (known-of read-path read)]
        (let [snapshots (snapshots-of read)
              reading (sources/reading (:query read) known :live (.-numbers read-path) (:sources read) snapshots)
              {:keys [join leave]} (sources/moves reading (:sources read))]
          (cond
            (:too-many-permissions reading)
            (do (end-read! read-path id :too-many-permissions (:too-many-permissions reading)) nil)

            (:too-large reading)
            (do (end-read! read-path id :a-run-grew-too-large (:too-large reading)) nil)

            (and (empty? join) (empty? leave))
            (do
              (swap! (.-held read-path)
                     (fn [held]
                       (if-not (contains? (:reads held) id)
                         held
                         (reduce (fn [h [lane st]]
                                   (if (contains? #{:yes :may-not-see} st)
                                     (assoc-in h [:reads id :rights lane :same-snap-retries] 0)
                                     h))
                                 held
                                 (:standing reading)))))
              {:reading reading :known known :snapshots snapshots :joined joined :left left})

            (<= (:live max-rounds) round)
            (do (end-read! read-path id :a-step-did-not-settle nil) nil)

            :else
            (do (leave-sources! read-path id leave false)
                (if (= :past-the-bound (join! read-path id join))
                  (do (end-read! read-path id :no-room-for-a-push-it-came-to-need (no-room read-path)) nil)
                  (recur (inc round) (into joined join) (into left leave))))))))))

(declare on-arrival on-rights)

(defn- when-given!
  [read-path id sources]
  (let [pushes (:pushes (held-now read-path))]
    (doseq [source sources]
      (when-let [entry (get pushes source)]
        (when (contains? (:readers entry) id)
          (.whenComplete ^CompletableFuture (force (:opening entry))
                         (reify BiConsumer
                           (accept [_ got _thrown]
                             (try (on-arrival read-path id source got)
                                  (catch Throwable _ nil))))))))))

(defn- launch-rights-async!
  "Fires one non-blocking `foreign-invoke-query-async` for `(id, lane)`,
  bounded by `:wait-ms`, delivering its outcome to `on-rights`."
  [^ReadPath read-path id routing-id lane query]
  (let [numbers (.-numbers read-path)
        out (CompletableFuture.)
        [outcome fut] (invoke-rights-raw read-path routing-id lane :see (:asker query) (:session query))]
    (case outcome
      :ok (.whenComplete ^CompletableFuture fut
                         (reify BiConsumer
                           (accept [_ value thrown]
                             (if thrown
                               (.complete out {:word (words/word :reach-the-store (what-threw thrown))})
                               (.complete out {:snapshot value})))))
      :threw (.complete out {:word (words/word :reach-the-store fut)}))
    (.completeOnTimeout out {:word (late read-path)} (long (:wait-ms numbers)) TimeUnit/MILLISECONDS)
    (.whenComplete out
                   (reify BiConsumer
                     (accept [_ got _thrown]
                       (try (on-rights read-path id lane got)
                            (catch Throwable _ nil)))))))

(defn- ensure-rights-queries!
  "Single-flight `lane-rights` launcher per `(read, lane)` when no held
  snapshot matches `known` and a rights push arrived (or the lane first
  became wanted) after the last ask. What must hold: the thread holds the
  read's turn."
  [^ReadPath read-path id reading]
  (when-let [read (read-of read-path id)]
    (when (:opened? read)
      (doseq [[routing-id lane] (:needs-rights reading)]
        (when-let [now-read (read-of read-path id)]
          (let [st (get-in now-read [:rights lane])
                push-seq (long (:push-seq st 0))
                asked-seq (long (:asked-seq st -1))
                in-flight? (boolean (:in-flight? st))
                retries (long (:same-snap-retries st 0))]
            (when (and (not in-flight?) (> push-seq asked-seq))
              (if (<= (:live max-rounds) retries)
                (end-read! read-path id :a-step-did-not-settle nil)
                (do
                  (swap! (.-held read-path)
                         (fn [held]
                           (if (contains? (:reads held) id)
                             (update-in held [:reads id :rights lane]
                                        (fn [cur]
                                          (assoc (or cur {})
                                                 :push-seq push-seq
                                                 :asked-seq push-seq
                                                 :in-flight? true)))
                             held)))
                  (launch-rights-async! read-path id routing-id lane (:query now-read)))))))))))

(defn- after-step!
  [^ReadPath read-path id {:keys [tell owed?]} joined]
  (swap! (.-held read-path)
         (fn [held] (if (contains? (:reads held) id) (assoc-in held [:reads id :owed?] owed?) held)))
  (when tell
    (tell! read-path id tell))
  (when-given! read-path id joined))

(defn- on-rights
  "Callback when an async `lane-rights` query completes for an opened live
  read. Never blocks or throws onto Rama's thread."
  [read-path id lane {:keys [snapshot word]}]
  (when-let [{:keys [turn]} (read-of read-path id)]
    (locking turn
      (when-let [read (read-of read-path id)]
        (when (:opened? read)
          (swap! (.-held read-path)
                 (fn [held]
                   (if (contains? (:reads held) id)
                     (update-in held [:reads id :rights lane]
                                (fn [st]
                                  (let [prev (:snapshot st)
                                        same-rs? (and snapshot prev
                                                      (= (:read-set snapshot) (:read-set prev)))]
                                    (cond-> (assoc (or st {}) :in-flight? false)
                                      snapshot (assoc :snapshot snapshot
                                                      :same-snap-retries
                                                      (if same-rs?
                                                        (inc (long (:same-snap-retries st 0)))
                                                        0))))))
                     held)))
          (cond
            word
            (end-read! read-path id :a-read-it-came-to-need-failed (dissoc word :could-not))

            (nil? snapshot)
            (end-read! read-path id :a-read-it-came-to-need-failed
                       {:said "lane-rights gave nothing"})

            (= :too-many-permissions (:standing snapshot))
            (end-read! read-path id :too-many-permissions
                       {:lane lane :most lanes/max-holder-permissions})

            :else
            (when-let [{:keys [reading known joined left]} (settle! read-path id)]
              (after-step! read-path id
                           (answer/after-rights (:query read) reading known lane joined left
                                                (:owed? (read-of read-path id)))
                           joined)
              (ensure-rights-queries! read-path id reading))))))))

(defn- on-arrival
  "A push that an opened read came to need is given."
  [read-path id source got]
  (when-let [{:keys [turn]} (read-of read-path id)]
    (locking turn
      (let [read (read-of read-path id)]
        (when (and read (:opened? read) (contains? (:sources read) source))
          (if-let [word (:word got)]
            (case (:could-not word)
              :serve-a-read-that-large
              (end-read! read-path id :a-run-grew-too-large (select-keys word [:what :most]))

              :too-many-permissions
              (end-read! read-path id :too-many-permissions (select-keys word [:lane :most]))

              (end-read! read-path id :a-read-it-came-to-need-failed (dissoc word :could-not)))
            (do
              (when (sources/rights-kind (:query read) source)
                (let [lane (:lane source)
                      home (lanes/home (:query read))
                      snap (get-in read [:rights lane :snapshot])
                      known-now (known-of read-path read)]
                  (when (and snap known-now
                             (not (sources/source-matches-snapshot? home lane snap source known-now)))
                    (swap! (.-held read-path)
                           (fn [held]
                             (if (contains? (:reads held) id)
                               (update-in held [:reads id :rights lane]
                                          (fn [st]
                                            (update (or st {}) :push-seq (fnil inc 0))))
                               held))))))
              (when-let [{:keys [reading known joined left]} (settle! read-path id)]
                (after-step! read-path id
                             (answer/after-arrival (:query read) reading known source joined left
                                                   (:owed? (read-of read-path id)))
                             joined)
                (ensure-rights-queries! read-path id reading)))))))))

(defn- on-change
  "A push of an opened read changed."
  [read-path id source changed old new]
  (when-let [{:keys [turn]} (read-of read-path id)]
    (locking turn
      (let [read (read-of read-path id)]
        (when (and read (:opened? read) (contains? (:sources read) source))
          (when (sources/rights-kind (:query read) source)
            (let [lane (:lane source)]
              (swap! (.-held read-path)
                     (fn [held]
                       (if (contains? (:reads held) id)
                         (update-in held [:reads id :rights lane]
                                    (fn [st]
                                      (update (or st {}) :push-seq (fnil inc 0))))
                         held)))))
          (when-let [{:keys [reading known snapshots joined left]} (settle! read-path id)]
            (let [query (:query read)
                  [meant-before meant-after] (answer/meanings query snapshots known source old new)]
              (after-step! read-path id
                           (answer/after-change query reading known source
                                                (sources/places-of query source changed)
                                                meant-before meant-after joined left
                                                (:owed? (read-of read-path id)))
                           joined)
              (ensure-rights-queries! read-path id reading))))))))

(defn- keys-of-diff
  [diff]
  (try
    (let [named (ArrayList.)
          unhandled? (AtomicBoolean. false)
          processor (reify
                      Diff$Processor
                      (unhandled [_] (.set unhandled? true))
                      KeyDiff$Processor
                      (processKeyDiff [_ d] (.add named [:put (.getKey ^KeyDiff d)]))
                      KeyRemoveDiff$Processor
                      (processKeyRemoveDiff [_ d] (.add named [:out (.getKey ^KeyRemoveDiff d)])))]
      (.process ^Diff diff processor)
      (if (.get unhandled?) :all (vec named)))
    (catch Throwable _ :all)))

(defn- diff-kind
  [diff]
  (cond
    (instance? DestroyedDiff diff)             :own-close
    (instance? UngracefulTerminationDiff diff) :ended-by-the-store
    (instance? ResyncDiff diff)                :anew
    :else                                      :change))

(defn- on-callback
  [^ReadPath read-path source ^AtomicBoolean called new diff old]
  (try
    (let [entry (get-in (held-now read-path) [:pushes source])
          kind (diff-kind diff)
          first? (and (= :anew kind) (.compareAndSet called false true))]
      (when (and entry (identical? called (:called entry)) (not= :own-close kind) (not first?))
        (.set called true)
        (if (= :ended-by-the-store kind)
          (doseq [id (:readers entry)]
            (end-read! read-path id :the-store-ended-a-push nil))
          (let [changed (sources/named source (if (= :anew kind) :all (keys-of-diff diff)) old new)
                most (:max-places-of-one-run (.-numbers read-path))
                path-kind (first (:path source))]
            (when (seq changed)
              (cond
                (and (= :run path-kind) (< most (count new)))
                (doseq [id (:readers entry)]
                  (end-read! read-path id :a-run-grew-too-large {:what :places-of-one-run :most most}))

                (and (= :holders path-kind)
                     (< lanes/max-holder-permissions (count (sources/keys-of-run source new))))
                (doseq [id (:readers entry)]
                  (end-read! read-path id :too-many-permissions
                             {:lane (:lane source) :most lanes/max-holder-permissions}))

                :else
                (doseq [id (:readers entry)]
                  (on-change read-path id source changed old new))))))))
    (catch Throwable _ nil)))

;; ------------------------------------------------------------------ the calls

(defn open
  "Gives a read path, or the word `:find-the-module` when the cluster gave
  one of the thirteen handles not under that name; no read path is then
  opened."
  ([cluster module-name] (open cluster module-name nil))
  ([cluster module-name numbers]
   (try
     (->ReadPath module-name
                 (rama/foreign-pstate cluster module-name "$$streaming-facts")
                 (rama/foreign-pstate cluster module-name "$$microbatch-facts")
                 (rama/foreign-pstate cluster module-name "$$streaming-facts-by-value")
                 (rama/foreign-pstate cluster module-name "$$microbatch-facts-by-value")
                 (rama/foreign-pstate cluster module-name "$$streaming-permissions-by-holder")
                 (rama/foreign-pstate cluster module-name "$$microbatch-permissions-by-holder")
                 (rama/foreign-pstate cluster module-name "$$streaming-line")
                 (rama/foreign-pstate cluster module-name "$$microbatch-line")
                 (rama/foreign-pstate cluster module-name "$$streaming-things")
                 (rama/foreign-pstate cluster module-name "$$microbatch-things")
                 (rama/foreign-pstate cluster module-name "$$streaming-task")
                 (rama/foreign-pstate cluster module-name "$$microbatch-task")
                 (rama/foreign-query cluster module-name "lane-rights")
                 (query/numbers numbers)
                 (atom {:pushes {} :counts {} :reads {}})
                 (AtomicLong. 0))
     (catch Throwable t
       (words/word :find-the-module (assoc (what-threw t) :module module-name))))))

(defn- checked
  [thing]
  (if (telling?)
    [:word (words/word :read-inside-an-askers-function nil)]
    (let [{query :ok why :refuse at :at} (query/check thing)]
      (if why
        [:word (words/word :read-the-query {:why why :at at})]
        [:ok query]))))

(defn- bump-epochs
  [epochs sources]
  (reduce (fn [m lane] (update m lane (fnil inc 0)))
          epochs
          (distinct (map :lane sources))))

(defn- read-once*
  "Reads `query` once. It reads `held` nowhere. Each made lane's
  `lane-rights` snapshot is accepted only at that lane's current
  `fact-epoch`, so `lane-rights` is always asked after the lane's last fact
  select returns (`CONTRACT.md` §2.4, `loop-0d.md`)."
  [^ReadPath read-path query]
  (let [numbers (.-numbers read-path)
        deadline (+ (now-ms) (:wait-ms numbers))
        max-sel (:once max-rounds)
        max-ask (* 2 max-sel)]
    (loop [sel-round 0
           ask-round 0
           known {}
           first-reads #{}
           fact-epochs {}
           rights-at {}]
      (let [valid-snapshots (into {}
                                  (keep (fn [[lane {:keys [epoch snapshot]}]]
                                          (when (or (= :too-many-permissions (:standing snapshot))
                                                    (= epoch (get fact-epochs lane 0)))
                                            [lane snapshot])))
                                  rights-at)
            reading (sources/reading query known :once numbers nil valid-snapshots)
            to-read-whole (filterv #(= :yes (get (:standing reading) (:lane %))) first-reads)]
        (cond
          (:too-many-permissions reading)
          (words/word :too-many-permissions (:too-many-permissions reading))

          (:too-large reading)
          (words/word :serve-a-read-that-large (:too-large reading))

          (seq to-read-whole)
          (let [[outcome got] (run-now read-path to-read-whole deadline)]
            (if (= :word outcome)
              got
              (recur sel-round
                     ask-round
                     (merge known got)
                     (reduce disj first-reads to-read-whole)
                     (bump-epochs fact-epochs to-read-whole)
                     rights-at)))

          (seq (:missing reading))
          (if (<= max-sel sel-round)
            (late read-path)
            (let [unread (:missing reading)
                  no-first-read? (fn [source] (and (= :run (first (:path source)))
                                                   (not (sources/first-read-holds? source))))
                  [outcome got] (select-now read-path
                                            (sources/calls-for-once (remove no-first-read? unread) numbers)
                                            deadline)
                  [outcome-whole got-whole] (if (= :word outcome)
                                              [outcome got]
                                              (let [runs (filterv no-first-read? unread)]
                                                (if (seq runs) (run-now read-path runs deadline) [:ok {}])))]
              (cond
                (= :word outcome) got
                (= :word outcome-whole) got-whole
                :else
                (recur (inc sel-round)
                       ask-round
                       (merge known got got-whole)
                       (into first-reads
                             (filter (fn [source]
                                       (and (= :run (first (:path source)))
                                            (not (sources/run-is-whole? (get got source) numbers)))))
                             (keys got))
                       (bump-epochs fact-epochs unread)
                       rights-at))))

          (seq (:needs-rights reading))
          (if (<= max-ask ask-round)
            (late read-path)
            (let [pairs (:needs-rights reading)
                  [outcome snaps] (query-rights-now read-path pairs :see (:asker query) (:session query) deadline)]
              (if (= :word outcome)
                snaps
                (recur sel-round
                       (inc ask-round)
                       known
                       first-reads
                       fact-epochs
                       (reduce (fn [m [lane snap]]
                                 (assoc m lane {:epoch (get fact-epochs lane 0) :snapshot snap}))
                               rights-at
                               snaps)))))

          (sources/settled? reading)
          (answer/whole query reading known)

          :else
          (late read-path))))))

(defn read-once
  "Gives an answer (`softland.store.read-path.answer`; of a lane's acts,
  `softland.store.read-path.acts`), or one of the read path's words. It
  never throws, and it holds nothing when it returns."
  [^ReadPath read-path query]
  (try
    (let [[outcome query] (checked query)]
      (cond
        (= :word outcome) query
        (= :acts (first (:asks query))) (read-acts-once read-path query)
        :else (read-once* read-path query)))
    (catch Throwable t
      (words/word :reach-the-store (what-threw t)))))

(defn- wait-for-held!
  [read-path id deadline]
  (let [pushes (:pushes (held-now read-path))]
    (loop [todo (seq (:sources (read-of read-path id)))]
      (if-let [source (first todo)]
        (if-let [entry (get pushes source)]
          (let [[outcome got] (within (force (:opening entry)) (- deadline (now-ms)))]
            (case outcome
              :ok    (if-let [word (:word got)] [:word word] (recur (next todo)))
              :late  [:word (late read-path)]
              :threw [:word (words/word :reach-the-store got)]))
          (recur (next todo)))
        [:ok]))))

(defn- read-live*
  [^ReadPath read-path query f]
  (let [numbers (.-numbers read-path)
        held (.-held read-path)
        id (.incrementAndGet ^AtomicLong (.-next-id read-path))
        turn (Object.)
        deadline (+ (now-ms) (:wait-ms numbers))
        give-up (fn [word] (leave-sources! read-path id :all true) word)
        wanted-now (fn []
                     (when-let [read (read-of read-path id)]
                       (when-let [known (known-of read-path read)]
                         (let [snapshots (snapshots-of read)
                               reading (sources/reading query known :live numbers (:sources read) snapshots)]
                           (assoc (sources/moves reading (:sources read))
                                  :reading reading :known known)))))]
    (swap! held assoc-in [:reads id]
           {:query query :tell f :turn turn :sources #{} :rights {} :opened? false :owed? false})
    (try
      (loop [round 0]
        (let [{:keys [reading join leave]} (wanted-now)]
          (cond
            (< deadline (now-ms))
            (give-up (late read-path))

            (nil? reading)
            (give-up (late read-path))

            (:too-many-permissions reading)
            (give-up (words/word :too-many-permissions (:too-many-permissions reading)))

            (:too-large reading)
            (give-up (words/word :serve-a-read-that-large (:too-large reading)))

            (and (or (seq join) (seq leave) (seq (:missing reading)) (seq (:needs-rights reading)))
                 (<= (:live max-rounds) round))
            (give-up (late read-path))

            (or (seq join) (seq leave) (seq (:missing reading)))
            (do (leave-sources! read-path id leave false)
                (if (= :past-the-bound (join! read-path id join))
                  (give-up (words/word :keep-more-reads-live (no-room read-path)))
                  (let [[outcome word] (wait-for-held! read-path id deadline)]
                    (if (= :word outcome)
                      (give-up word)
                      (recur (inc round))))))

            (seq (:needs-rights reading))
            (let [prev-snaps (some-> (read-of read-path id) snapshots-of)
                  [outcome snaps] (query-rights-now read-path (:needs-rights reading)
                                                    :see (:asker query) (:session query) deadline)]
              (if (= :word outcome)
                (give-up snaps)
                (let [same-all? (and (seq snaps)
                                     (every? (fn [[lane snap]]
                                               (= (:read-set snap)
                                                  (:read-set (get prev-snaps lane))))
                                             snaps))]
                  (swap! held
                         (fn [now]
                           (if-not (contains? (:reads now) id)
                             now
                             (reduce (fn [h [lane snap]]
                                       (assoc-in h [:reads id :rights lane]
                                                 {:snapshot snap
                                                  :push-seq 0
                                                  :asked-seq 0
                                                  :in-flight? false
                                                  :same-snap-retries 0}))
                                     now
                                     snaps))))
                  (if same-all?
                    (let [left-ms (- deadline (now-ms))]
                      (if (<= left-ms 0)
                        (give-up (late read-path))
                        (if (try (Thread/sleep (min 5 left-ms)) true
                                 (catch InterruptedException _
                                   (.interrupt (Thread/currentThread))
                                   false))
                          (recur round)
                          (give-up (late read-path)))))
                    (recur (inc round))))))

            :else
            (let [opened (locking turn
                           (let [{:keys [reading known join leave]} (wanted-now)]
                             (when (and (sources/settled? reading)
                                        (empty? join)
                                        (empty? leave))
                               (let [answer (answer/whole query reading known)]
                                 (swap! held (fn [now]
                                               (if (contains? (:reads now) id)
                                                 (-> now
                                                     (assoc-in [:reads id :opened?] true)
                                                     (assoc-in [:reads id :owed?] (answer/mark-of answer)))
                                                 now)))
                                 (tell! read-path id {:anew answer})
                                 {:now answer :leave-with (->LiveRead held id)}))))]
              (cond
                opened opened
                (nil? (read-of read-path id)) (give-up (late read-path))
                :else (recur (inc round)))))))
      (catch Throwable t
        (give-up (words/word :reach-the-store (what-threw t)))))))

(defn read-live
  "Gives `{:now answer, :leave-with handle}`, or one of the read path's
  words. A query of a lane's acts is not kept live: it gives
  `:keep-a-read-of-acts-live` at once, with no call to Rama. It never
  throws."
  [^ReadPath read-path query f]
  (try
    (let [[outcome query] (checked query)]
      (cond
        (= :word outcome) query
        (= :acts (first (:asks query))) (words/word :keep-a-read-of-acts-live nil)
        :else (read-live* read-path query f)))
    (catch Throwable t
      (words/word :reach-the-store (what-threw t)))))

(defn leave
  "Gives `:left`; or `:not-open` for a read that was left or ended before,
  and for a thing that is no handle of this read path. It never throws."
  [^ReadPath read-path handle]
  (try
    (if (and (instance? LiveRead handle)
             (identical? (.-held read-path) (.-held ^LiveRead handle))
             (leave-sources! read-path (.-id ^LiveRead handle) :all true))
      :left
      :not-open)
    (catch Throwable _ :not-open)))

(defn open-count
  "A caller that never leaves shows here. It makes no call to Rama."
  [^ReadPath read-path]
  (let [held (held-now read-path)]
    {:live-reads (count (:reads held))
     :pushes     (count (:pushes held))}))
