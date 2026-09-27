;; IMPORTANT: Before modifying this file, re-read PLAN-citation.md sections 5 and 6.
(ns rig.cite.run
  "The citation's driver (PLAN-citation.md, sections 5 and 6): operator
  code, as the runner is, that runs the tool over a repository on one
  in-process cluster and writes the report. It seeds a person and a layer,
  writes the four tools from `tools/citation.edn` through the door as the
  operator and grants them, writes the person's requests (a `:cite/doc` a
  document, then the `:material/read` requests of the reading plan, round
  by round), runs the runner's passes until a pass offers nothing new, and
  reads everything back through the one exit as the person for the report.

  The tool's logic is not here: it is the four tool facts and the
  vocabulary's steps. The reading plan (P-C16) is: for each resolved
  Clojure file, the distinct commits its citations were written at, in
  commit-time order, then its line of work's head; round k writes each
  file's k-th request, so a citation is bound before its file's next
  reading lands.

  `clojure -M:cite` from the rig folder runs it over this repository."
  (:refer-clojure :exclude [run!])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [com.rpl.rama.test :as rtest]
            [rig.cite :as cite]
            [rig.material :as m]
            [rig.store.client :as c]
            [rig.store.module :as module]
            [rig.store.read-exit :as rx]
            [rig.store.runner :as runner]))

;; ------------------------------------------------------------------- git

(defn- git [repo & args]
  (let [r (apply shell/sh "git" "-C" repo args)]
    (when (zero? (:exit r)) (str/trim (:out r)))))

(defn commit-of "The commit a revision names." [repo rev] (git repo "rev-parse" "--verify" (str rev "^{commit}")))

(defn markdown-under
  "The Markdown paths under `dirs` in the commit's tree, sorted."
  [repo commit dirs]
  (->> (str/split-lines (or (apply git repo "ls-tree" "-r" "--name-only" commit "--" dirs) ""))
       (filter #(str/ends-with? % ".md"))
       sort vec))

;; ------------------------------------------------------------- the store

(defn start!
  "One in-process cluster with the store module, and the exit's handles."
  []
  (let [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc module/Store {:tasks 4 :threads 2 :workers 1})
    {:ipc ipc :st (rx/connect ipc)}))

(defn stop! [{:keys [ipc]}] (.close ^java.io.Closeable ipc))

(defn- ok!
  "Offer until answered; a no is an error the driver stops on."
  [st offer]
  (let [a (c/offer-until-answered! st offer)]
    (when-not (= :yes (:answer a))
      (throw (ex-info "an offer was refused" {:answer a :facts (take 3 (:facts offer))})))
    a))

(defn seed!
  "The person, the layer (personal, owned by the person) and the person's
  permission in it."
  [st {:keys [owner layer]}]
  (c/seed! st {:persons [owner] :layers [[layer {:kind :personal :owner owner}]]
               :permissions [[owner layer layer]]}))

(defn tool-facts
  "The tool facts of an EDN file, `:the-layer` filled in with `layer`."
  [path layer]
  (walk/postwalk #(if (= :the-layer %) layer %) (edn/read-string (slurp path))))

(defn write-tools!
  "Each tool fact through the door as the operator, in the file's order
  (their stamps are the order a pass runs them in), and each tool's grant."
  [st layer tools]
  (doseq [t tools]
    (ok! st (c/build {:who :operator :layer layer :class :by-layer :facts [t]}))
    (ok! st (c/grant-offer st [(:e t) layer layer])))
  (mapv :e tools))

(defn offer-as!
  "One act of the person's facts in the layer (P-C15)."
  [st owner layer facts]
  (when (seq facts)
    (ok! st (c/build {:who owner :layer layer :class :by-layer :permission [owner layer layer] :facts facts}))))

(defn passes!
  "Runner passes over the layer until one offers nothing new (every run
  either recorded or failed, as a run waiting for a reading does), at most
  `max-passes`. The pass reports, for the log. A tool whose match read is
  partial stops the driver: the runner reads one page of matches with no
  cursor, so matches past the limit would never run and the pass would
  look quiet (the validation's F8)."
  [st layer config & {:keys [max-passes] :or {max-passes 12}}]
  (loop [n 0 reports []]
    (let [rep (runner/run-pass! st {:layer layer :config config :limit 10000})
          _ (when-let [partial (seq (filter #(= :partial (:mark %)) (:tools rep)))]
              (throw (ex-info "a tool's match read was partial" {:tools (mapv :id partial)})))
          fresh (count (for [l (:tools rep) r (:runs l) :when (contains? r :answer)] r))
          reports (conj reports {:fresh fresh
                                 :refused (frequencies (for [l (:tools rep) r (:runs l)
                                                             :when (or (= :no (:answer r)) (:refused r))]
                                                         [(:id l) (or (:reason r) (get-in r [:error :error]) (:error r))]))
                                 :tools (mapv (fn [l] [(or (:id l) (:tool l)) (count (:runs l)) (:refused l)]) (:tools rep))})]
      (if (or (zero? fresh) (>= (inc n) max-passes))
        reports
        (recur (inc n) reports)))))

(defn read-all
  "Every fact under key `k` in the layer, read through the exit as the
  person (recorded)."
  [st owner layer k]
  (let [r (rx/read! st {:reader owner :reader-kind :person :working layer :permission [owner layer layer]
                        :layer layer :read [:pattern [:k k]] :limit 10000})]
    (when (contains? r :refused) (throw (ex-info "a read was refused" {:k k :r r})))
    (when (= :partial (:mark r)) (throw (ex-info "a read was partial" {:k k})))
    (:rows r)))

;; --------------------------------------------------------- the requests

(defn doc-requests
  "A `:cite/doc` fact a document: `docs` is `[[line commit path] ...]`,
  `heads` `{line head-commit}`."
  [repo-id docs heads]
  (vec (for [[line commit path] docs]
         {:e (cite/doc-thing repo-id line path) :k :cite/doc
          :v {:repo repo-id :line line :rev commit :path path :lines heads}})))

(defn reading-plan
  "For each resolved Clojure file of the found citations, its readings in
  order (P-C16): the distinct commits its citations were written at, by
  commit time, then its line of work's head. `{[line path] [commit ...]}`."
  [repo found heads]
  (let [times (atom {})
        t (fn [c] (or (get @times c) (let [v (cite/commit-time repo c)] (swap! times assoc c v) v)))
        by-file (reduce (fn [acc v]
                          (if (and (= :clojure (:target v)) (:resolved v))
                            (update acc [(get-in v [:resolved :line]) (get-in v [:resolved :path])]
                                    (fnil conj #{}) (get-in v [:resolved :commit]))
                            acc))
                        {} (map :value found))]
    (into (sorted-map)
          (for [[[line path] commits] by-file
                :let [head (get heads line)
                      ordered (vec (sort-by (juxt t identity) (disj commits head)))]]
            [[line path] (conj ordered head)]))))

(defn read-requests
  "The `:material/read` facts of one round: each file's k-th commit."
  [repo-id plan k]
  (vec (for [[[line path] commits] plan
             :when (< k (count commits))]
         {:e (m/file-thing repo-id line path) :k :material/read
          :v {:repo repo-id :line line :rev (nth commits k) :path path}})))

;; ------------------------------------------------------------- the report

(defn- short-sha [c] (some-> c (subs 0 8)))

(defn state-at-head
  "One citation's state at HEAD from what the store holds: not bound yet
  with its reason; the latest mark when it is against its file's latest
  reading; true when bound to the latest reading and never marked; else a
  miss of the walk."
  [{:keys [unbound bound marks]} head-reading]
  (cond
    unbound {:state :not-bound-yet :why (:reason (:value unbound)) :detail (:value unbound)}
    (nil? bound) {:state :miss :why :no-binding}
    :else
    (let [latest (last (sort-by :stamp marks))
          reading-fid (get-in bound [:value :reading])]
      (cond
        (and latest (= (get-in latest [:value :against :reading]) (:fid head-reading)))
        (select-keys (:value latest) [:state :why :now :cited-now :against])
        (and (nil? latest) (= reading-fid (:fid head-reading)))
        {:state :true :why :bound-at-head}
        :else {:state :miss :why :not-marked-against-head
               :latest-mark (some-> latest :value :against :commit short-sha)
               :head (some-> head-reading :value :commit short-sha)}))))

(defn gather
  "Everything the report needs, read back through the exit as the person."
  [st owner layer]
  (let [rd #(read-all st owner layer %)]
    {:found (rd :cite/found) :bound (rd :cite/bound) :unbound (rd :cite/unbound)
     :marks (rd :cite/mark) :readings (rd :material/file) :scanned (rd :cite/scanned)
     :walks (rd :cite/walk)}))

(defn rows-by-cite
  "The citations with their binding, marks and state at HEAD, by document
  and line."
  [{:keys [found bound unbound marks readings]}]
  (let [by-e (fn [rows] (group-by :e rows))
        b (by-e bound) u (by-e unbound) mk (by-e marks)
        head-of (reduce (fn [acc r] (let [cur (get acc (:e r))]
                                      (if (or (nil? cur) (> (:stamp r) (:stamp cur))) (assoc acc (:e r) r) acc)))
                        {} readings)]
    (->> found
         (map (fn [f]
                (let [v (:value f)
                      cite (:e f)
                      rec {:found f :bound (first (get b cite)) :unbound (first (get u cite)) :marks (get mk cite)}
                      head (get head-of (get-in v [:resolved :file]))]
                  (assoc rec :cite cite :doc (get-in v [:doc :path]) :line (get-in v [:at :line])
                         :col (get-in v [:at :col]) :state (state-at-head rec head)))))
         (sort-by (juxt :doc :line :col))
         vec)))

(defn- state-text [{:keys [state why now cited-now detail latest-mark head]}]
  (case state
    :true (if (= why :bound-at-head) "true (written at HEAD)" "true")
    :moved (str "moved: now lines " (str/join "-" cited-now) " (form at " (str/join "-" now) ")")
    :stale (case why
             :changed (str "stale: the form's text changed (now " (str/join "-" now) ")")
             :gone "stale: the form is gone"
             :file-gone "stale: the file is gone"
             :unreadable "stale: the file does not read"
             (str "stale: " (name why)))
    :not-bound-yet (str "not bound yet: " (some-> why name)
                        (when-let [k (:kind detail)] (str " (" (name k) ")"))
                        (when-let [cs (seq (:candidates detail))] (str " (" (count cs) " candidates)")))
    :miss (str "MISS: " (name why) (when latest-mark (str " (last mark at " latest-mark ", head " head ")")))))

(defn- esc [s] (str/replace (str s) "|" "\\|"))

(defn report-markdown
  "The report (PLAN-citation.md section 6): counts, the runs still refused
  when the driver stopped, then every citation by document and line."
  [rows {:keys [heads at refused]}]
  (let [states (frequencies (map (comp :state :state) rows))
        why (frequencies (for [r rows :when (= :not-bound-yet (get-in r [:state :state]))] (get-in r [:state :why])))
        stale (frequencies (for [r rows :when (= :stale (get-in r [:state :state]))] (get-in r [:state :why])))]
    (str/join
     "\n"
     (concat
      ["# Citations at HEAD"
       ""
       (str "Run " at " by `rig.cite.run` on an in-process cluster; its records are thrown away. "
            "HEAD is " (str/join ", " (for [[l c] (sort heads)] (str "`" (name l) "` " (short-sha c)))) ". "
            "A citation is bound at the commit that last changed its line (`git blame`) and marked against each later reading of its file; "
            "its state here is against the file's reading at HEAD. The placeholders behind every choice are in `PLAN-citation.md` section 9.")
       ""
       (str "**" (count rows) " citations.** "
            (str/join ", " (for [[k n] (sort-by (comp str key) states)] (str n " " (name k)))) ".")
       ""
       (str "Not bound yet, by reason: " (str/join ", " (for [[k n] (sort-by (comp - val) why)] (str (name k) " " n))) ".")
       ""
       (str "Stale, by why: " (if (seq stale) (str/join ", " (for [[k n] (sort-by (comp - val) stale)] (str (name k) " " n))) "none") ".")
       ""
       (str "Runs still refused in the last pass: "
            (if (seq refused) (str/join ", " (for [[[tool why] n] refused] (str (name tool) " " why " " n))) "none") ".")
       ""]
      (mapcat
       (fn [[doc rs]]
         (concat
          [(str "## " doc) ""
           "| line | citation | written | bound to | state at HEAD |"
           "|---|---|---|---|---|"]
          (for [r rs
                :let [v (get-in r [:found :value])
                      b (get-in r [:bound :value])
                      res (:resolved v)]]
            (str "| " (:line r)
                 " | `" (esc (:text v)) "`" (when res (str " → `" (esc (:path res)) "`"))
                 " | " (short-sha (:written v))
                 " | " (if b (str "`" (esc (get-in b [:form :name])) "` " (str/join "-" (get-in b [:form :lines]))
                                  " @" (short-sha (get-in b [:form :commit])))
                           "")
                 " | " (esc (state-text (:state r))) " |"))
          [""]))
       (group-by :doc rows))))))

;; ------------------------------------------------------------------- run

(defn run!
  "The tool over a repository: seed, tools, the documents' requests,
  passes, the reading plan's rounds, and the gathered records. `opts`:
  `:repo` the repository's path, `:repo-id` its id in the tools (`:softland`),
  `:owner`, `:layer`, `:heads` `{line head-rev}`, `:docs` `[[line path] ...]`
  read at their line's head, `:tools` the EDN path. Returns `{:rows :log
  :heads}`; `store` an open `start!` handle."
  [{:keys [st]} {:keys [repo repo-id owner layer heads docs tools]}]
  (let [heads (into {} (for [[l r] heads] [l (commit-of repo r)]))
        config {:repos {repo-id repo}}
        log (atom [])
        note! (fn [what x] (swap! log conj [what x]) (println "CITE" what (pr-str x)))]
    (seed! st {:owner owner :layer layer})
    (note! :tools (write-tools! st layer (tool-facts tools layer)))
    (offer-as! st owner layer (doc-requests repo-id (for [[l p] docs] [l (get heads l) p]) heads))
    (note! :documents (count docs))
    (note! :find-passes (passes! st layer config))
    (let [found (read-all st owner layer :cite/found)
          plan (reading-plan repo found heads)
          rounds (reduce max 0 (map count (vals plan)))]
      (note! :found (count found))
      (note! :plan {:files (count plan) :rounds rounds :readings (reduce + (map count (vals plan)))})
      (doseq [k (range rounds)]
        (offer-as! st owner layer (read-requests repo-id plan k))
        (note! [:round k] (passes! st layer config)))
      (note! :final-passes (passes! st layer config))
      (let [g (gather st owner layer)]
        {:rows (rows-by-cite g) :gathered g :log @log :heads heads :plan plan}))))

(defn this-repo-docs
  "The documents Sid named: this project's Markdown under src/proposal/ and
  docs/ on main, and RIG.md on the rig branch."
  [repo main-rev]
  (concat (for [p (markdown-under repo (commit-of repo main-rev) ["src/proposal" "docs"])] [:main p])
          [[:rig "src/proposal/rig-2026-09-25/RIG.md"]]))

(defn -main
  "Run over this repository: main's documents at `origin/main`, RIG.md at
  the rig line's head (by default `HEAD`, this branch), and write the report
  to runs/citation-report.md."
  [& [repo main-rev rig-rev]]
  (let [repo (or repo (.getCanonicalPath (io/file "../../..")))
        main-rev (or main-rev "origin/main")
        rig-rev (or rig-rev "HEAD")
        h (start!)]
    (try
      (let [out (run! h {:repo repo :repo-id :softland :owner :sid :layer :softland
                         :heads {:main main-rev :rig rig-rev}
                         :docs (this-repo-docs repo main-rev)
                         :tools "tools/citation.edn"})
            md (report-markdown (:rows out) {:heads (:heads out) :at (str (java.time.LocalDateTime/now))
                                             :refused (:refused (peek (second (last (:log out)))))})]
        (spit "runs/citation-report.md" md)
        (spit "runs/citation-run.edn" (pr-str {:heads (:heads out) :log (:log out) :plan (:plan out)}))
        (println "CITE report written:" (count (:rows out)) "citations"))
      (finally (stop! h) (shutdown-agents)))))
