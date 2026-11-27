(ns gate-histories.piece-2.cluster
  "The smallest histories of a run, sent through the store's own module on
  an in-process cluster: the door's `send-act` and `answer-of`, the gate's
  walk in Rama (`softland.store.module.walk/decide`), and the read path's
  `read-live`. It requires Rama.

  Why. `gate-histories.walk` is the store's walk said again in memory, from
  the module's own description and the pure functions that it calls. This
  holds the two against each other on the histories that a run found: for
  each kind of break, its smallest history is played in memory and then
  sent, thing by thing, through the door of a cluster that runs the module,
  and the door's answer to each sender is held against the answer that the
  walk in memory gave. For the live reads of a history, each asker's
  function is counted on the cluster as well.

  What it takes and does. `clojure -M:piece-2-cluster [histories [seed]]` plays the
  run of `gate-histories.piece-2.run` with that count and seed, takes the smallest
  history of each kind of break, and for each one launches the module
  `Store` on a fresh in-process cluster, sends the history's things up to
  the first step at which the kind broke, and prints, for each thing, the
  door's answer on the cluster beside the answer in memory. A history in
  which a thing before that step was taken out of the history for another
  break cannot be sent as it was played, and is said so and passed over.

  The time on each thing is the door's clock on the cluster, not the
  history's: no rule reads it, and the digest leaves it out.

  One in-process cluster at a time runs on this machine: run it behind the
  machine's cluster lock,

    flock ../../backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock clojure -M:piece-2-cluster 3000 1

  What it does not reach: the base and the microbatch gate (every history is
  one streaming lane), and every order of the pushes of a live read, which
  Rama gives as it gives them."
  (:require [com.rpl.rama :as rama]
            [com.rpl.rama.test :as rtest]
            [gate-histories.piece-2.run :as run]
            [softland.store.door :as door]
            [softland.store.module :as module]
            [softland.store.read-path :as read-path]))

(def ^:private live-askers @#'gate-histories.piece-2.run/live-askers)
(def ^:private live-asks @#'gate-histories.piece-2.run/live-asks)
(def ^:private the-lane run/lane)

(defn- answer-word
  "An answer of the door as a short word: the answer, with the reason of a
  refusal."
  [answer]
  (if (= :refused (:answer answer))
    (str "refused " (name (:reason answer)))
    (name (or (:answer answer) (:could-not answer) :none))))

(defn- sent-on-the-cluster
  "Sends one thing of a history through the door of the cluster: the maker's
  act, without the three parts that the door adds, under the thing's own
  act-id. Gives the door's answer to its sender, read with a wait, or the
  door's word."
  [d thing]
  (let [maker (dissoc thing :act-id :depot :door-time)
        sent (door/send-act d maker (:act-id thing))]
    (if (:could-not sent)
      sent
      (door/answer-of d (:act-id sent) (:digest sent) 5000))))

(defn- replay
  "Sends a history's things through the module on a fresh cluster, up to
  the first step at which `kind` broke. Takes the kind and the history.
  Gives {:lines [[step who in-memory on-the-cluster same?]], :calls {asker
  [how many calls of her function after each step]}} or {:passed-over why}."
  [kind ops]
  (let [played (run/play ops)
        steps (:steps played)
        at (some (fn [{:keys [step broke]}] (when (= kind (:class broke)) step)) steps)
        out-before (some (fn [{:keys [step taken-out? broke]}]
                           (when (and taken-out? (< step at)) [step (:class broke)]))
                         steps)]
    (if out-before
      {:passed-over (str "step " (first out-before) " was taken out of the history for " (pr-str (second out-before)))}
      (with-open [ipc (rtest/create-ipc)]
        (rtest/launch-module! ipc module/Store {:tasks 4 :threads 2})
        (let [module-name (rama/get-module-name module/Store)
              d (door/open ipc module-name)
              rp (read-path/open ipc module-name)
              calls (atom {})
              opened (atom nil)]
          (try
            (let [lines
                  (vec
                   (for [{:keys [step sent told]} (take (inc at) steps)]
                     (let [on-cluster (sent-on-the-cluster d sent)]
                       ;; the live reads open after the making act, as in memory
                       (when (and (zero? step) (nil? @opened))
                         (reset! opened
                                 (vec (for [[who run] live-askers]
                                        (let [events (atom [])]
                                          (swap! calls assoc who [])
                                          {:who who :events events
                                           :live (read-path/read-live rp {:asks live-asks :asker who :session run
                                                                          :lanes [the-lane]}
                                                                      (fn [ev] (swap! events conj ev)))})))))
                       ;; a push reaches a live read a little after the gate ran
                       (Thread/sleep 150)
                       (doseq [{:keys [who events]} @opened]
                         (swap! calls update who conj (count @events)))
                       [step (:who sent) (answer-word told) (answer-word on-cluster)
                        (= (answer-word told) (answer-word on-cluster))])))]
              {:lines lines :calls @calls :at at})
            (finally
              (doseq [{:keys [live]} @opened]
                (when-let [h (:leave-with live)] (read-path/leave rp h))))))))))

(defn -main
  "`clojure -M:piece-2-cluster [histories [seed]]`: the smallest history of each
  kind of break of that run, sent through the module on a cluster. Exits 1
  when an answer on the cluster is not the answer in memory."
  [& args]
  (let [[histories seed] args
        histories (if histories (parse-long histories) 2000)
        seed (if seed (parse-long seed) 1)
        found (@#'gate-histories.piece-2.run/kinds-of-break histories seed)
        smallest-of @#'gate-histories.piece-2.run/smallest-of
        differs (atom 0)]
    (println (format "%d histories, seed %d: %d kinds of break, each one's smallest history on a cluster"
                     histories seed (count found)))
    (doseq [[kind {:keys [tree]}] (sort-by (comp - :count val) found)]
      (let [ops (smallest-of kind tree)
            result (replay kind ops)]
        (println)
        (println "KIND" (pr-str kind))
        (if-let [why (:passed-over result)]
          (println "  passed over:" why)
          (do
            (doseq [[step who in-memory on-cluster same?] (:lines result)]
              (when-not same? (swap! differs inc))
              (println (format "  %2d  %-4s  in memory: %-40s on the cluster: %-40s %s"
                               step (name who) in-memory on-cluster (if same? "same" "DIFFERENT"))))
            (doseq [[who counts] (:calls result)]
              (println (format "  %s's live read: calls of her function after each step, counted from the first: %s"
                               (name who) (pr-str counts))))))))
    (println)
    (println (if (zero? @differs)
               "Every answer on the cluster was the answer in memory."
               (str @differs " answers on the cluster were not the answers in memory.")))
    (shutdown-agents)
    (System/exit (if (zero? @differs) 0 1))))
