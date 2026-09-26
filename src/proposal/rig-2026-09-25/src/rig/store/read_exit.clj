;; IMPORTANT: Before modifying this file, re-read PLAN-read-exit.md, "The exit".
(ns rig.store.read-exit
  "The one exit for reads on one-owner layers (PLAN-read-exit.md, 'The
  exit'; CONCLUSION R5 as a default): query, append the read entry, then
  answer. Nothing is shown before the entry is acknowledged; a refused
  entry hides the answer. In the rig the store's clients run in the test's
  process, so the exit is plain Clojure over the foreign API beside
  `rig.store.client`; a kept store moves the same function behind the server
  (R5's read gateway), which takes the reader's kind, `:for` and working
  layer from the actor, never from the call.

  The entry goes through the ordinary client offer path,
  `client/offer-until-answered!`, as any act: decided by the gate that
  orders the working layer, answered by name, retried from the record.

  Test hooks (R3, the in-process cluster only), each naming the read by its
  entry's name: `:exit-after-query` between the query and the entry,
  `:exit-after-entry` between the entry's answer and the return,
  `:exit-shown` just before a return with rows. An armed point throws, which
  stands for the exit's process dying there."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [rig.store.client :as c]
            [rig.store.envelope :as env]
            [rig.store.inject :as inject]
            [rig.store.reads :as reads]
            [rig.store.shared-reads :as shared-reads]))

(defn connect
  "Handles on the store, taken once: phase 1's `client/connect` map, the two
  read queries and the `*index-ops` depot."
  [cluster]
  (let [store (c/connect cluster)
        mn (:module-name store)]
    (assoc store
           :read-point (foreign-query cluster mn "read-point")
           :read-pattern (foreign-query cluster mn "read-pattern")
           :index-ops (foreign-depot cluster mn "*index-ops")
           ;; stage 5b: deltas, the maintenance reads, and the micro store's index pages
           :read-delta (foreign-query cluster mn "read-delta")
           :standing-close (foreign-query cluster mn "standing-close")
           :standing-open (foreign-query cluster mn "standing-open")
           :entry-ids (foreign-query cluster mn "entry-ids")
           :micro-progress (foreign-query cluster mn "micro-index-progress")
           :task-layers (foreign-query cluster mn "task-layers")
           :micro-index-ops (foreign-depot cluster mn "*micro-index-ops")
           :micro-state (foreign-pstate cluster mn "$$micro"))))

;; ------------------------------------------------------------ the call

(defn- read-form-ok? [[kind x :as form]]
  (and (vector? form) (= 2 (count form))
       (case kind
         :point (and (vector? x) (seq x))
         :pattern (vector? x)
         false)))

(defn check-call
  "The exit's step 1, total: nil when the call is well formed, else the
  reason `:bad-read`. A person reads for itself; a model or a tool for the
  person named in `:for` (tonight a parameter, F12)."
  [spec]
  (try
    (let [{:keys [reader for reader-kind rows? working permission layer read as-of limit role session]} spec
          kw? env/readable-keyword?
          moment-ok? (fn [x] (and (map? x) (= 1 (count x))
                                  (let [[k v] (first x)] (and (#{:stamp :frontier} k) (int? v) (<= 0 v)))))]
      (when-not (and (kw? reader) (kw? for)
                     (contains? reads/reader-kinds reader-kind)
                     (or (not= :person reader-kind) (= for reader))
                     (or (nil? rows?) (boolean? rows?))
                     (kw? working) (kw? layer)
                     (or (nil? permission) (env/pid? permission))
                     (read-form-ok? read)
                     (or (nil? as-of) (and (int? as-of) (<= 0 as-of)) (moment-ok? as-of))
                     (or (nil? session) (kw? session))
                     (or (nil? limit) (int? limit))
                     (contains? reads/roles role))
        :bad-read))
    (catch Throwable _ :bad-read)))

(defn query
  "The exit's step 2: the read on the read layer's home, through the query
  topology its form names."
  [store {:keys [for layer read as-of limit]}]
  (let [[kind x] read]
    (case kind
      :point (foreign-invoke-query (:read-point store) layer for x as-of)
      :pattern (foreign-invoke-query (:read-pattern store) layer for x as-of limit))))

(defn entry-offer
  "The read entry as an ordinary offer into the working layer (FR3, FR4,
  first-record): `:who` the reader, its permission there, `:class
  :by-layer`, nothing stood on, no because-of, no subjects; its facts from
  `reads/entry-facts`. Stage 5b (FRR10, first-record): the entry names the
  session the read was taken in (`:session`, the reader's session in the
  working layer; the door's own when none is given), so its session's close
  keeps or drops it, and it acts under the reader's permission there. The
  built map is what is sent and resent."
  [answer {:keys [reader working permission entry-name session] :as spec}]
  (c/build {:name entry-name :who reader :layer working :class :by-layer
            :permission permission :stood-on {} :because-of nil :subjects #{} :session session
            :facts (reads/entry-facts answer spec)}))

(defn read!
  "Read through the one exit. `spec`: `:reader` (the person, or the agent's
  actor id), `:for` (the person the read is for; a person's own id by
  default), `:reader-kind` (`:person`, `:model`, `:tool`), `:rows?` (a tool
  asking for exact rows), `:working` and `:permission` (the reader's working
  layer and its permission there, where the entry lands), `:layer` (the
  layer read), `:read` (`[:point [fid ...]]` or `[:pattern p]`), `:as-of`,
  `:limit`, `:role` (default `:shown`), and `:entry-name` (tests arm hooks
  on it; else made fresh here, before anything else).

  Returns, only after the gate acknowledged the entry:
  `{:rows [...] :moment {:stamp m} :matched [[fid stamp] ...] :mark ..
  :fingerprint .. :entry name :entry-stamp s ...}`; or `{:refused r}` when
  the call or the query refused (nothing read, nothing recorded); or
  `{:refused r :entry name}` when the entry was refused (nothing shown).
  Throws when the entry's answer cannot be had; nothing was shown then."
  [store spec]
  (let [spec (cond-> (merge {:role :shown} spec)
               (and (= :person (:reader-kind spec)) (nil? (:for spec))) (assoc :for (:reader spec)))
        nm (or (:entry-name spec) (env/make-name (:working spec) :by-layer))
        spec (assoc spec :entry-name nm)]
    (if-let [bad (check-call spec)]
      {:refused bad}
      (let [answer (query store spec)]
        (if (contains? answer :refused)
          {:refused (:refused answer)}
          (do
            (inject/point! :exit-after-query nm)
            (let [entry (entry-offer answer spec)
                  a (c/offer-until-answered! store entry)]
              (inject/point! :exit-after-entry nm)
              (if (= :yes (:answer a))
                (do
                  (inject/point! :exit-shown nm)
                  (merge (select-keys answer [:layer :moment :kind :pattern :rows :matched :mark :fingerprint :fp-secret
                                              :max-stamp])
                         {:entry nm :entry-stamp (:stamp a)}))
                {:refused (:reason a) :entry nm}))))))))

;; ------------------------------------------------------------ rebuild

(defn index-op!
  "Append one `*index-ops` record with a full ack and return the gate's
  answer to it. Resends the same record after an append error (a page is
  idempotent; phase 0 finding 4), up to `tries` times."
  ([store op] (index-op! store op 60))
  ([store op tries]
   (loop [n 1]
     (let [r (try (get (foreign-append! (:index-ops store) op :ack) "gate")
                  (catch Exception e e))]
       (cond
         (not (instance? Exception r)) r
         (>= n tries) (throw r)
         :else (do (Thread/sleep 250) (recur (inc n))))))))

(defn rebuild!
  "Rebuild one layer's five index fields from its log (F2, RC8), driven one
  bounded page at a time: put pages from the start of the layer's answers
  until done, then sweep pages over `:ix-ek`, `:ix-ke`, `:ix-kv`, `:ix-s`
  (stage 5b) and `:ix-of` in turn until each is done. When it returns, the fields hold
  exactly what the log implies at that point. `:acts` and `:entries` set the
  page sizes (tests use small ones). Returns the pages sent and what the
  sweeps deleted and rewrote."
  [store layer & {:keys [acts entries] :or {acts reads/max-put-acts entries reads/max-sweep-entries}}]
  (let [puts (loop [after nil n 0]
               (let [r (index-op! store {:layer layer :op :rebuild-put :after after :acts acts})]
                 (when (contains? r :refused) (throw (ex-info "rebuild put page refused" {:answer r})))
                 (if (:done? r) (inc n) (recur (:next r) (inc n)))))
        sweeps (vec (for [f [:ix-ek :ix-ke :ix-kv :ix-s :ix-of]]
                      (loop [after nil n 0 deleted 0 rewritten 0]
                        (let [r (index-op! store {:layer layer :op :rebuild-sweep :field f :after after :entries entries})]
                          (when (contains? r :refused) (throw (ex-info "rebuild sweep page refused" {:answer r})))
                          (let [n (inc n) deleted (+ deleted (:deleted r 0)) rewritten (+ rewritten (:rewritten r 0))]
                            (if (:done? r)
                              {:field f :pages n :deleted deleted :rewritten rewritten}
                              (recur (:next r) n deleted rewritten)))))))]
    {:put-pages puts :sweeps sweeps
     :deleted (reduce + (map :deleted sweeps)) :rewritten (reduce + (map :rewritten sweeps))}))

;; ================================================== stage 5b: the close, the drop, the purges, the restore
;; Operator and door code beside the exit (PLAN-reads-rest.md, "The close act",
;; "Indexes and forgets, whole"): plain Clojure over the foreign API. Test hooks
;; (R3): `:drop-page` after each drop page, `:purge-page` after each person
;; purge page, each naming its close act or its person.

(def drop-page "Read-entry ids a drop takes per page (F4)." 64)

(defn close-session!
  "A session's close act (phase 2's L28) with stage 5b's part `:reads :keep
  | :drop` (FRR5, first-record; absent means keep), by `who`, the session's
  writer (citing `permission`, by default [who layer layer]) or the
  operator. Its answer. The drop is not run here: the closer runs
  `drop-reads!` after the yes, as the layer's owner or the operator (an
  agent cannot forget in its layer; `resume-drops!` finds a pending drop
  from the record)."
  [store who layer session & {:keys [reads permission]}]
  (c/offer-until-answered!
   store
   (c/build {:who who :layer layer :class :by-layer
             :permission (when-not (= :operator who) (or permission [who layer layer]))
             :session session
             :facts [{:e session :k :session-closed :v (cond-> {:session session} reads (assoc :reads reads))}]})))

(defn entry-ids
  "RS6: one page of a working layer's live read-entry facts of `session`
  admitted at or before `before`, `{:ids [[fid stamp e] ...] :next :done?}`."
  [store layer session before after n]
  (foreign-invoke-query (:entry-ids store) layer session before after n))

(defn forget-entry-offer
  "One dropped entry's forget (FRR9 as built, first-record): phase 2's
  ordinary value forget, by the layer's owner or the operator, standing on
  its target, because of the close act."
  [who layer fid stamp e because]
  (c/build {:who who :layer layer :class :by-layer
            :permission (when-not (= :operator who) [who layer layer])
            :stood-on {fid stamp} :because-of because
            :facts [{:e e :k :forget :v {:target fid}}]}))

(defn drop-reads!
  "The drop at a session's close (PLAN-reads-rest.md, 'Dropped'): the
  session's read entries admitted at or before the close act's stamp,
  forgotten one ordinary forget act per entry fact (phase 2's rule: a lock
  control fact is its act's one fact), each named before it is offered,
  answered by name and retried from the record, `:because-of` the close
  act. Pages of 64 ids; a rerun finds only what is not yet erased. Returns
  the number of forget acts answered yes."
  [store who layer session close-name close-stamp]
  (loop [after nil n 0]
    (let [{:keys [ids next done?]} (entry-ids store layer session close-stamp after drop-page)
          yes (count (filter #(= :yes (:answer %))
                             (mapv (fn [[fid stamp e]]
                                     (c/offer-until-answered! store (forget-entry-offer who layer fid stamp e close-name)))
                                   ids)))]
      (inject/point! :drop-page close-name)
      (if done? (+ n yes) (recur next (+ n yes))))))

(defn session-closes
  "A working layer's session close facts, from the `:ix-ke` range of
  `:session-closed` (control facts, plaintext, indexed like any fact):
  `[[session reads stamp name] ...]`, reads `:keep` when the act said
  nothing."
  [store layer]
  (let [from (str "session-closed" reads/sep)
        to (reads/prefix-end from)]
    (vec (for [e (foreign-select [(keypath layer :ix-ke) (sorted-map-range from to) MAP-VALS] (:layers store))
               :let [v (try (env/decode-value (:v e)) (catch Exception _ nil))]
               :when (map? v)]
           [(:session v) (or (:reads v) :keep) (:stamp e) (first (:fid e))]))))

(defn resume-drops!
  "F11: the drops a crashed closer left, found from the record: every
  session close with `:reads :drop` in each layer, its drop run again (a
  drop that finished finds nothing). Run by the owner's next door for its
  agent layers or by the operator's start-up, as `who`. {layer forgets}."
  [store who layers]
  (into {} (for [L layers]
             [L (reduce + 0 (for [[s r stamp nm] (session-closes store L) :when (= :drop r)]
                              (drop-reads! store who L s nm stamp)))])))

(defn task-count
  "The module's task count, from the micro ops depot's partitions."
  [store]
  (:num-partitions (foreign-object-info (:micro-index-ops store))))

(defn- wait-frontier!
  "Until the settled frontier reaches batch `b`: every task has committed it."
  [store b]
  (loop [n 0]
    (let [f (try (:frontier (foreign-invoke-query (:micro-progress store) 0)) (catch Exception _ nil))]
      (when (< (or f -1) b)
        (when (> n 2400) (throw (ex-info "the frontier did not pass the page's batch" {:batch b})))
        (Thread/sleep 50)
        (recur (inc n))))))

(defn micro-op!
  "Append one `*micro-index-ops` record (maintenance, RR8) and wait for its
  page's progress row on its task, then for the frontier to pass that
  page's batch, so its writes are committed everywhere. Resends after an
  append error (a page is idempotent). The progress row."
  [store op]
  (let [req (str (random-uuid))
        op (assoc op :req req)]
    (loop [tries 0]
      (let [r (try (foreign-append! (:micro-index-ops store) op :append-ack) nil (catch Exception e e))]
        (when r
          (if (>= tries 60) (throw r) (do (Thread/sleep 250) (recur (inc tries)))))))
    (loop [n 0]
      (let [p (try (:progress (foreign-invoke-query (:micro-progress store) (:task op))) (catch Exception _ nil))]
        (if (= req (:req p))
          (do (wait-frontier! store (:batch p)) p)
          (do (when (> n 2400) (throw (ex-info "the micro index page was not seen" {:op op})))
              (Thread/sleep 50)
              (recur (inc n))))))))

(defn rebuild-micro!
  "The micro store's indexes rebuilt from its log (PLAN-reads-rest.md,
  'Rebuild from the log, shared'), one bounded page at a time on every
  task: put pages over the task's entities until done, then sweep pages
  over each field of the shared layers homed there. The first page's batch
  is the rebuild's start: an index gap flagged before it is cleared (F9).
  Returns the pages sent."
  [store & {:keys [entities entries] :or {entities shared-reads/max-put-entities entries shared-reads/max-sweep-entries}}]
  (let [N (task-count store)
        since (volatile! nil)
        pages (volatile! 0)]
    (doseq [t (range N)]
      (loop [after nil]
        (let [p (micro-op! store {:op :rebuild-put :task t :after after :entities entities})]
          (vswap! pages inc)
          (when (nil? @since) (vreset! since (:batch p)))
          (when-not (:done? p) (recur (:cursor p))))))
    (doseq [f shared-reads/sweep-fields t (range N)]
      (loop [after nil]
        (let [p (micro-op! store {:op :rebuild-sweep :task t :field f :after after :entries entries :since @since})]
          (vswap! pages inc)
          (when-not (:done? p) (recur (:cursor p))))))
    {:pages @pages :since @since}))

(defn purge-person!
  "Path 3's purge after a person forget (PLAN-reads-rest.md, F17), driven
  by the operator in bounded pages on every task: the one-owner pages
  (`*index-ops`, the read exit's source; phase 2's fan-out child already
  purged them in the forget's own event, so here they find the same
  writes, and after a restore they are the replay), then the micro store's
  pages. Resumable: every page is idempotent. Returns the pages sent."
  [store p & {:keys [n] :or {n reads/max-person-page}}]
  (let [N (task-count store)
        pages (volatile! 0)]
    (doseq [t (range N)]
      (loop [after nil]
        (let [a (index-op! store {:layer p :op :person-purge :task t :person p :after after :n n})]
          (vswap! pages inc)
          (inject/point! :purge-page p)
          (when (and (map? a) (not (:done? a)) (not (contains? a :refused))) (recur (:next a))))))
    (doseq [t (range N)]
      (loop [after nil]
        (let [pr (micro-op! store {:op :person-purge :task t :person p :after after
                                   :entries (min n shared-reads/max-person-entries)})]
          (vswap! pages inc)
          (inject/point! :purge-page p)
          (when-not (:done? pr) (recur (:cursor pr))))))
    @pages))

(defn forget-person!
  "The person forget and its purge (F17's wrapper): phase 2's
  `client/forget-person!`, then `purge-person!`, phase 2 untouched. A crash
  between the two is repaired by running `purge-person!` again. The
  forget's answer."
  [store p]
  (let [a (c/forget-person! store p)]
    (when (= :yes (:answer a)) (purge-person! store p))
    a))

(defn- range-vals [pstate layer k]
  (let [from (str (reads/kw-text k) reads/sep)]
    (foreign-select [(keypath layer :ix-ke) (sorted-map-range from (reads/prefix-end from)) MAP-VALS] pstate)))

(defn forget-facts
  "Every admitted forget fact a restore replays, in stamp order across both
  stores (default 2: stamps compare across them): `[stamp kind layer
  target store]`, kind `:value` (target a fact id) or `:person`, found
  through the rebuilt `:ix-ke` ranges (control facts are plaintext and
  indexed like any fact)."
  [store stream-layers micro-layers]
  (let [value (fn [st pstate L]
                (for [e (range-vals pstate L :forget)
                      :let [v (try (env/decode-value (:v e)) (catch Exception _ nil))]
                      :when (and (map? v) (:target v))]
                  [(:stamp e) :value L (:target v) st]))]
    (->> (concat (mapcat #(value :stream (:layers store) %) stream-layers)
                 (mapcat #(value :micro (:micro-state store) %) micro-layers)
                 (for [e (range-vals (:layers store) c/people-layer :forget-person)
                       :let [v (try (env/decode-value (:v e)) (catch Exception _ nil))]
                       :when (and (map? v) (:person v))]
                   [(:stamp e) :person nil (:person v) :stream]))
         (sort-by first)
         vec)))

(defn restore!
  "A restore (PLAN-reads-rest.md path 5), the operator's: (1) the restore is
  a fact, an operator act in the store layer `:people` (FRR7,
  first-record); (2) every index rebuilt from the log in pages, every
  one-owner layer by `rebuild!` and the micro store by `rebuild-micro!`;
  (3) then the forget facts of both stores replayed after it in stamp order
  (the rig constraint): a value forget as `:replay-forget` on its store's
  ops depot, a person forget as `purge-person!`. With the purge invariant
  the indexes after the replay are those of a store that never restored.
  Returns what it did."
  [store & {:keys [acts entries] :or {acts reads/max-put-acts entries reads/max-sweep-entries}}]
  (let [fact (c/offer-until-answered!
              store (c/build {:who :operator :layer c/people-layer :class :by-layer
                              :facts [{:e :store :k :restore :v {:indexes :rebuilt}}]}))
        N (task-count store)
        per-task (mapv #(foreign-invoke-query (:task-layers store) %) (range N))
        stream (vec (distinct (mapcat :stream per-task)))
        micro (vec (distinct (mapcat :micro per-task)))
        home (into {} (for [t (range N) L (:micro (nth per-task t))] [L t]))
        rebuilt (into {} (for [L stream] [L (rebuild! store L :acts acts :entries entries)]))
        micro-rebuilt (rebuild-micro! store :entries entries)
        forgets (forget-facts store stream micro)]
    (doseq [[_ kind L target st] forgets]
      (case kind
        :value (if (= :micro st)
                 (micro-op! store {:op :replay-forget :task (home L) :layer L :fid target})
                 (index-op! store {:layer L :op :replay-forget :fid target}))
        :person (purge-person! store target)))
    {:fact fact :stream stream :micro micro :rebuilt rebuilt :micro-rebuilt micro-rebuilt :replayed (count forgets)}))
