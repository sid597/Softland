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
            [rig.store.reads :as reads]))

(defn connect
  "Handles on the store, taken once: phase 1's `client/connect` map, the two
  read queries and the `*index-ops` depot."
  [cluster]
  (let [store (c/connect cluster)
        mn (:module-name store)]
    (assoc store
           :read-point (foreign-query cluster mn "read-point")
           :read-pattern (foreign-query cluster mn "read-pattern")
           :index-ops (foreign-depot cluster mn "*index-ops"))))

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
  [{:keys [reader for reader-kind rows? working permission layer read as-of limit role]}]
  (try
    (let [kw? env/readable-keyword?]
      (when-not (and (kw? reader) (kw? for)
                     (contains? reads/reader-kinds reader-kind)
                     (or (not= :person reader-kind) (= for reader))
                     (or (nil? rows?) (boolean? rows?))
                     (kw? working) (kw? layer)
                     (or (nil? permission) (env/pid? permission))
                     (read-form-ok? read)
                     (or (nil? as-of) (and (int? as-of) (<= 0 as-of)))
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
  :by-layer`, nothing stood on, no because-of, no subjects, no session; its
  facts from `reads/entry-facts`. The built map is what is sent and resent."
  [answer {:keys [reader working permission entry-name] :as spec}]
  (c/build {:name entry-name :who reader :layer working :class :by-layer
            :permission permission :stood-on {} :because-of nil :subjects #{} :session nil
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
                  (merge (select-keys answer [:layer :moment :kind :pattern :rows :matched :mark :fingerprint :fp-secret])
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
  "Rebuild one layer's four index fields from its log (F2, RC8), driven one
  bounded page at a time: put pages from the start of the layer's answers
  until done, then sweep pages over `:ix-ek`, `:ix-ke`, `:ix-kv` and
  `:ix-of` in turn until each is done. When it returns, the fields hold
  exactly what the log implies at that point. `:acts` and `:entries` set the
  page sizes (tests use small ones). Returns the pages sent and what the
  sweeps deleted and rewrote."
  [store layer & {:keys [acts entries] :or {acts reads/max-put-acts entries reads/max-sweep-entries}}]
  (let [puts (loop [after nil n 0]
               (let [r (index-op! store {:layer layer :op :rebuild-put :after after :acts acts})]
                 (when (contains? r :refused) (throw (ex-info "rebuild put page refused" {:answer r})))
                 (if (:done? r) (inc n) (recur (:next r) (inc n)))))
        sweeps (vec (for [f [:ix-ek :ix-ke :ix-kv :ix-of]]
                      (loop [after nil n 0 deleted 0 rewritten 0]
                        (let [r (index-op! store {:layer layer :op :rebuild-sweep :field f :after after :entries entries})]
                          (when (contains? r :refused) (throw (ex-info "rebuild sweep page refused" {:answer r})))
                          (let [n (inc n) deleted (+ deleted (:deleted r 0)) rewritten (+ rewritten (:rewritten r 0))]
                            (if (:done? r)
                              {:field f :pages n :deleted deleted :rewritten rewritten}
                              (recur (:next r) n deleted rewritten)))))))]
    {:put-pages puts :sweeps sweeps
     :deleted (reduce + (map :deleted sweeps)) :rewritten (reduce + (map :rewritten sweeps))}))
