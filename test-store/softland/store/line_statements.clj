(ns softland.store.line-statements
  "The statements of a lane's line and of the read by stamp, run over the
  store: the judge that another session wrote from piece 3's spec alone
  (`src/proposal/runtime-2026-11-21/store-piece-3/statements/`), with the
  store's door, its gates on an in-process cluster and its read path as the
  judge's target. This namespace is the target and the run; the
  statements, their model, their histories and their runner are the
  judge's, and this namespace changes none of them.

  It loads only on the classpath of the judge's folder, which holds the
  judge's own source and Rama (its alias `:store`). That alias names
  `src-store` and `test-store` one folder short of the repository root, so
  the run adds the two itself and changes nothing of the judge's. From that
  folder, behind the machine's cluster lock:

    clojure -Sdeps '{:aliases {:the-store {:extra-paths [\"../../../../../src-store\" \"../../../../../test-store\"]}}}' \\
      -M:store:the-store -m softland.store.line-statements [histories-per-seed] [seed ...]

  With no argument it runs what the judge's own run runs on its model: 80
  histories under each of the seeds 1, 2 and 3, made by the judge's
  generator in the judge's way (`piece3-statements.run/sample-histories`);
  and first the judge's probe of the bounds, on a store of its own.

  The target. The judge plays each history against a target and against
  its model side by side, through five calls (`piece3-statements.runner`):

    :act-id-for   `softland.store.door/act-id-for`
    :send-act!    `softland.store.door/send-act`; for an act of the base it
                  then waits until the microbatch gate has processed every
                  thing appended to its depot, so that the next step sees
                  the decision, as the judge's model does at once
    :append-raw!  an append with no door, to the depot named: with the ack
                  level :ack to the streaming depot, which waits for the
                  gate; with :append-ack to the microbatch depot, and then
                  the same wait. A thing that Rama's client cannot append
                  (a sorted map whose keys do not compare with `:lane`
                  throws in the depot's partitioner, on the client) is not
                  appended, and the call gives a word
    :answer-of    `softland.store.door/answer-of`
    :read-line    `softland.store.read-path/read-once` of the query
                  `{:asks [:acts start most] :from-lane lane}` with the
                  asker, or `:not-signed-in`, and the session; read by a
                  read path whose bound on the weight of one read's things
                  is the judge's `:max-bytes`. The answer is put in the
                  judge's form: `:left-out` the word or nil, `:next-stamp`
                  the answer's `:next`, and each decision given with a
                  `:status` of `:whole`, `:withheld` or `:too-large`
    :make-act-permission-fact
                  a yes for the act that the judge plans, in the form that
                  the store takes since the yes has an id of its own: the
                  judge's uuid as the yes's id, and the digest of the act as
                  its maker made it
                  (`softland.store.door.digest/hex-for-a-yes`)

  A fresh store for each history. A history begins with nothing made, and
  every history of the judge uses the lanes `:lab` and `:base`. So each is
  played on a module of its own, launched under a name of its own in one
  in-process cluster with four tasks, and destroyed after. The module is
  the store's, by `softland.store.module/add-gates-and-reads` behind its two
  depots, as `Store` declares them. Four histories are played at once, each
  on its own module.

  State. One in-process cluster, which the run creates and closes; its
  files are temporary and are Rama's. For each history a module, a door and
  the read paths of its byte bounds, dropped with the module. It writes no
  file.

  What a pass shows. For each history, every statement of the judge held at
  every step and every read: the store's answers, its line and its read
  gave what the judge's model gives. That the model says what the spec
  says is the judge's own claim, with the broken copies that its own run
  catches. A history whose play threw is a break of its own, named
  `:the-run-threw`, with what threw."
  (:require [com.rpl.rama :as rama]
            [com.rpl.rama.test :as rtest]
            [piece3-statements.run :as judge-run]
            [piece3-statements.runner :as judge]
            [softland.store.door :as door]
            [softland.store.door.digest :as digest]
            [softland.store.module :as module]
            [softland.store.permissions :as perm]
            [softland.store.read-path :as read-path])
  (:import [java.util.concurrent Callable Executors TimeUnit]))

(def ^:private tasks "The number of tasks of each module." 4)

(def ^:private at-once "How many histories are played at once, each on its own module." 4)

(defn- store-under
  "Gives the store's module under a name of its own: its two depots of acts
  and `softland.store.module/add-gates-and-reads`, as `Store` has them."
  [module-name]
  (rama/module {:module-name module-name} [setup topologies]
    (rama/declare-depot setup *streaming-depot (rama/hash-by :lane))
    (rama/declare-depot setup *microbatch-depot (rama/hash-by :lane))
    (module/add-gates-and-reads topologies)))

(defn- query-of
  "Takes a read of the judge's: {:lane :start :max-count :asker
  :not-signed-in :session}. Gives the query of the read by stamp. What the
  judge gave is passed as it is, so that a read that is wrong is turned
  away by the read path's own check."
  [{:keys [lane start max-count asker not-signed-in session]}]
  (cond-> {:asks [:acts start max-count] :from-lane lane}
    (some? asker) (assoc :asker asker)
    (some? not-signed-in) (assoc :not-signed-in not-signed-in)
    (some? session) (assoc :session session)))

(defn- in-the-judges-form
  "Takes what the read path gave: an answer of a read of acts, or a word.
  Gives it in the judge's form, or the word as it is."
  [got]
  (if (:could-not got)
    got
    {:left-out (some-> got :left-out first :why)
     :last-stamp (:last-stamp got)
     :next-stamp (:next got)
     :entries (mapv (fn [entry]
                      (cond
                        (contains? entry :thing)
                        {:stamp (:stamp entry) :status :whole :act-id (:act-id entry)
                         :answer (:answer entry) :thing (:thing entry)}

                        (contains? entry :no-thing)
                        {:stamp (:stamp entry) :status :too-large :act-id (:act-id entry)
                         :answer (:answer entry)}

                        :else
                        {:stamp (:stamp entry) :status :withheld}))
                    (:acts got))}))

(defn- target-on
  "Takes the cluster and a module's name. Gives the judge's target over that
  module: the five calls of the namespace docstring, and the yes's fact."
  [ipc module-name]
  (let [the-door (door/open ipc module-name)
        read-paths (atom {})
        read-path-for (fn [max-bytes]
                        (or (get @read-paths max-bytes)
                            (let [rp (read-path/open ipc module-name
                                                     (when max-bytes {:max-thing-weight-of-one-read max-bytes}))]
                              (swap! read-paths assoc max-bytes rp)
                              rp)))
        streaming (rama/foreign-depot ipc module-name "*streaming-depot")
        microbatch (rama/foreign-depot ipc module-name "*microbatch-depot")
        appended (atom 0)
        processed! (fn []
                     (rtest/wait-for-microbatch-processed-count ipc module-name "microbatch-gate" @appended))]
    {:act-id-for
     (fn [lane] (door/act-id-for the-door lane))

     :send-act!
     (fn [maker-act act-id]
       (let [sent (door/send-act the-door maker-act act-id)]
         (when (and (vector? (:act-id sent)) (= :microbatch (nth (:act-id sent) 1)))
           (swap! appended inc)
           (processed!))
         sent))

     :append-raw!
     (fn [depot raw]
       (try
         (if (= :microbatch depot)
           (do (rama/foreign-append! microbatch raw :append-ack)
               (swap! appended inc)
               (processed!))
           (rama/foreign-append! streaming raw :ack))
         {:ok true}
         (catch Throwable t
           {:could-not :append :threw (.getName (class t))})))

     :answer-of
     (fn [act-id digest] (door/answer-of the-door act-id digest))

     :read-line
     (fn [req]
       (in-the-judges-form (read-path/read-once (read-path-for (:max-bytes req)) (query-of req))))

     :make-act-permission-fact
     (fn [{:keys [lane holder parent target-uuid planned-act]}]
       (perm/permission-fact
        (perm/act-permission holder lane parent target-uuid (digest/hex-for-a-yes planned-act))))}))

(defn- on-a-module
  "Launches a module of its own in the cluster, gives `f` the target over
  it, and destroys the module. Gives what `f` gave. Takes the cluster, the
  name that the module is declared with, and `f`. Rama puts the name of
  the namespace that declares a module in front of that name, so the
  door, the read paths and the destroying take the name that Rama gives
  (`get-module-name`)."
  [ipc declared-name f]
  (let [module (store-under declared-name)
        module-name (rama/get-module-name module)]
    (rtest/launch-module! ipc module {:tasks tasks :threads 2 :workers 1})
    (try (f (target-on ipc module-name))
         (finally (rtest/destroy-module! ipc module-name)))))

(defn -main
  "Runs the judge's probe of the bounds and its histories over the store,
  prints a line for each break and a summary, and exits: 0 when the probe
  passed and no history broke a statement."
  [& args]
  (let [per-seed (or (some-> (first args) parse-long) 80)
        seeds (or (seq (map parse-long (rest args))) [1 2 3])
        histories (vec (mapcat #(judge-run/sample-histories per-seed %) seeds))
        t0 (System/currentTimeMillis)]
    (with-open [ipc (rtest/create-ipc)]
      (let [probe (on-a-module ipc "LineStatementsProbe"
                               judge-run/check-32mib-and-raw-boundaries!)
            _ (println (if probe "PASSED" "FAILED")
                       "the judge's probe of the bounds, over the store: 1 MiB kept whole with standing, a thing over 32 MiB too large and the line goes on, a sorted map that throws is safe")
            pool (Executors/newFixedThreadPool at-once)
            futures (mapv (fn [i h]
                            (.submit pool ^Callable
                                     (fn []
                                       (try
                                         (on-a-module ipc (str "LineStatementsHistory" i)
                                                      (fn [target]
                                                        (when-let [err (judge/run-history! target h)]
                                                          (assoc err :history-index i :lane (:lane h)))))
                                         (catch Throwable t
                                           {:statement :the-run-threw :history-index i :lane (:lane h)
                                            :detail (str (.getName (class t)) ": " (.getMessage t))})))))
                          (range) histories)
            breaks (into [] (keep #(.get ^java.util.concurrent.Future %)) futures)]
        (.shutdown pool)
        (.awaitTermination pool 1 TimeUnit/MINUTES)
        (doseq [b breaks]
          (println "BROKEN" (:statement b) "at history" (:history-index b) (:lane b) "step" (:step-index b)
                   (pr-str (:step b)))
          (println "       " (subs (str (:detail b)) 0 (min 600 (count (str (:detail b)))))))
        (println (format "%s the judge's statements over the store: %d histories (seeds %s, %d each), %d broke a statement, in %d s"
                         (if (and probe (empty? breaks)) "PASSED" "FAILED")
                         (count histories) (pr-str (vec seeds)) per-seed (count breaks)
                         (quot (- (System/currentTimeMillis) t0) 1000)))
        (shutdown-agents)
        (System/exit (if (and probe (empty? breaks)) 0 1))))))
