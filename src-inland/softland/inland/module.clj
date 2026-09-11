(ns softland.inland.module
  "Rama authority for accepted material, revision rows, indexes and decisions.
   Takes workspace-routed proposals; gives admission decisions and addressable facts.
   Owns five PStates in one stream. Row/index/decision writes precede the separate
   global workspace registration event. External calls never execute in topology.
   Revisions increase from the current row; deletion of an override removes that
   counter. Version identity across delete/recreate is therefore not guaranteed."
  (:use [com.rpl.rama] [com.rpl.rama.path])
  (:require [com.rpl.rama.ops :as ops]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [softland.inland.total :as total]))

(defn seed-rows
  "Classpath seed resource → parsed genesis rows. Reads packaged EDN; it is an
   input to admission, not the current accepted world."
  [] (edn/read-string (slurp (io/resource "inland/seed.edn"))))
(defn decision
  "Operation, status, reason and revision → public admission receipt.
   Does not write state or imply an external effect completed."
  [op status reason revision]
  {:request-id (:request-id op) :name (:name op) :layer (:layer op)
   :operation (:kind op) :status status :reason reason :revision revision})

(defn outcome*
  "Operation plus current/source/policy rows → decision and proposed mutation.
   Checks actor labels, revision expectations, row shape and activity transitions.
   These labels identify the local build's actors; they are not an authentication
   system. Reads the seed resource only for genesis; otherwise derives without I/O.
   A running cancellation becomes unconfirmed. The same executor may later provide
   a confirmed reply. Candidate promotion checks source revision and the supplied
   base expectation; it does not independently validate transitive provenance."
  [op current source policy]
  (let [revision (:revision current 0)
        reject #(hash-map :decision (decision op :rejected % revision))
        kind (:kind op)
        parsed (when (:source op) (total/parse (:source op)))
        row (or (:value parsed) (:row op))
        save (fn [value]
               {:row (assoc value :name (:name op) :revision (inc revision)
                                  :asserted-by (:actor op) :accepted-at (:arrival-time op)
                                  :accepted-request (:request-id op))
                :decision (decision op :accepted nil (inc revision))})]
    (cond
      (not (and (string? (:workspace op)) (string? (:request-id op))
                (string? (:name op)) (#{"sid" "resident" "executor"} (:actor op))))
      (reject "Malformed request or unauthenticated actor.")
      (= kind :seed) {:seed (when-not current (seed-rows)) :decision (decision op :accepted nil revision)}
      (and (#{:put :promote :remove :delete-override} kind) (not= revision (:expected-revision op)))
      (reject "The accepted revision changed. Load it before applying this draft.")
      (and (#{:promote :remove :delete-override} kind) (or (:activity current) (:activity source)))
      (reject "Activity records retain their owned lifecycle and cannot be promoted or removed as definitions.")
      (= kind :put)
      (cond (:error parsed) (reject (:error parsed))
            (or (:activity current) (:activity row)) (reject "Activity state changes through its owned lifecycle, not a record edit.")
            (total/row-error (assoc row :name (:name op))) (reject (total/row-error (assoc row :name (:name op))))
            (and (= "resident" (:actor op)) (:body row) (= "base" (:layer op))
                 (not= :direct (:machine-behavior policy))) (reject "Shared behavior uses a candidate layer under the accepted policy.")
            :else (save (cond-> (dissoc row :resolved-layer)
                          (:basis op) (assoc :basis (:basis op)))))
      (= kind :promote)
      (if (and source (not (:removed source)) (= (:source-revision op) (:revision source)))
        (save (dissoc source :revision :asserted-by :accepted-request))
        (reject "The candidate basis is missing or changed."))
      (= kind :remove) (save {:removed true})
      (= kind :delete-override)
      (if (= "base" (:layer op)) (reject "Base removal uses a tombstone.")
          {:delete true :decision (decision op :accepted nil (inc revision))})
      (= kind :start)
      (cond current (reject "An activity with this identity already exists.")
            (not (and (= :claude (get-in op [:intent :runner]))
                      (string? (get-in op [:intent :input]))
                      (<= 1 (count (get-in op [:intent :input])) 2000)
                      (<= 1 (get-in op [:intent :max-output] 0) 400)))
            (reject "The resident needs a 1–2000 character request and a maximum of 400 output tokens.")
            :else (save {:activity (assoc (:intent op) :definition-basis (:invocation op))
                         :status :pending :catalog "activities"}))
      (= kind :claim)
      (if (and (:activity current) (= "executor" (:actor op)) (= :pending (:status current)))
        (save (assoc current :status :running :execution-owner (:execution-owner op)))
        (reject "This activity is already owned or terminal."))
      (= kind :observe)
      (cond
        (not (:activity current)) (reject "No accepted activity exists at this address.")
        (not= "executor" (:actor op)) (reject "Only the execution owner reports an outcome.")
        (not= (:execution-owner current) (:execution-owner op)) (reject "A different executor owns this activity.")
        (#{:complete :failed :cancelled} (:status current)) (reject "The terminal outcome is already retained.")
        (not (#{:complete :failed :unconfirmed :cancelled} (:status op))) (reject "Unknown external outcome.")
        :else (save (cond-> (assoc current :status (:status op) :reason (:reason op))
                      (:provider-result op) (assoc :provider-result (:provider-result op))
                      (:reply op) (assoc :reply (:reply op) :provenance :machine)
                      (and (= :unconfirmed (:status current)) (:reply op)) (assoc :late-result true))))
      (= kind :cancel)
      (if (#{:pending :running} (:status current))
        (save (assoc current :status (if (= :pending (:status current)) :cancelled :unconfirmed)
                            :reason "Cancellation requested; a possibly performed external call is not retried."))
        (reject "This activity is already terminal."))
      :else (reject "No admission capability answers this request."))))

(defn outcome
  "Admission inputs → outcome, converting malformed-input exceptions to rejection.
   The wrapper keeps invalid proposals from failing the stream evaluation."
  [op current source policy]
  (try (outcome* op current source policy)
       (catch Throwable _
         {:decision (decision op :rejected "The request does not satisfy the capability's input contract." (:revision current))})))

(defn index-change
  "Bucket members, name and membership flag → sorted unique member vector.
   Updates the whole scoped bucket; no paging or per-member PState is provided."
  [members name add?]
  (vec (sort (if add? (conj (set members) name) (disj (set members) name)))))

(defn mutates?
  "Outcome → truthy row/delete marker when current material must change.
   Seed installation is handled separately by the topology."
  [outcome] (or (:row outcome) (:delete outcome)))
(defn same-request?
  "Two admission envelopes → equality excluding server arrival time.
   Used to replay a retained decision or reject reuse of an id for different work."
  [a b] (= (dissoc a :arrival-time) (dissoc b :arrival-time)))
(defn register-workspace?
  "Operation and outcome → whether accepted seeding should register the workspace.
   Registration lets a later resident process discover workspaces without a browser."
  [op outcome]
  (and (= :seed (:kind op)) (= :accepted (get-in outcome [:decision :status]))))

(defmodule material [setup topologies]
  (declare-depot setup *operations (hash-by :workspace))
  (let [stream (stream-topology topologies "accept")]
    (declare-pstate stream $$rows {String (map-schema String clojure.lang.IPersistentMap {:subindex? true})})
    (declare-pstate stream $$versions {String (map-schema String clojure.lang.IPersistentMap {:subindex? true})})
    (declare-pstate stream $$index {String (map-schema String clojure.lang.IPersistentVector {:subindex? true})})
    (declare-pstate stream $$decisions {String (map-schema String clojure.lang.IPersistentMap {:subindex? true})})
    (declare-pstate stream $$workspaces {String Boolean} {:global? true})
    (<<sources stream
      (source> *operations {:retry-mode :all-after} :> *op)
      (get *op :workspace :> *workspace)
      (get *op :request-id :> *rid)
      (get *op :name :> *name)
      (get *op :layer "base" :> *layer)
      (total/row-key *layer *name :> *key)
      (local-select> [(keypath *workspace *rid)] $$decisions :> *prior)
      (<<if (some? *prior)
        (<<if (same-request? *op (get *prior :request))
          (ack-return> (get *prior :decision))
          (else>)
          (decision *op :rejected "Request identity already belongs to a different envelope." nil :> *conflict)
          (ack-return> *conflict))
        (else>)
        (local-select> [(keypath *workspace *key)] $$rows :> *current)
        (total/row-key (get *op :from-layer "base") (get *op :from-name "") :> *source-key)
        (local-select> [(keypath *workspace *source-key)] $$rows :> *source)
        (local-select> [(keypath *workspace "base/admission-policy")] $$rows :> *policy)
        (outcome *op *current *source *policy :> *outcome)
        (<<if (get *outcome :seed)
          (ops/explode (get *outcome :seed) :> *seed)
          (assoc *seed :revision 1 :asserted-by "sid" :accepted-request *rid :> *seed-row)
          (total/row-key "base" (get *seed-row :name) :> *seed-key)
          (total/version-key "base" (get *seed-row :name) 1 :> *seed-version)
          (local-transform> [(keypath *workspace *seed-key) (termval *seed-row)] $$rows)
          (local-transform> [(keypath *workspace *seed-version) (termval *seed-row)] $$versions)
          (<<atomic
            (ops/explode (total/index-keys *seed-row) :> *bucket)
            (str "base/" *bucket :> *index-key)
            (local-select> [(keypath *workspace *index-key)] $$index :> *members)
            (index-change *members (get *seed-row :name) true :> *next-members)
            (local-transform> [(keypath *workspace *index-key) (termval *next-members)] $$index)))
        (<<if (get *outcome :row)
          (get *outcome :row :> *row)
          (local-transform> [(keypath *workspace *key) (termval *row)] $$rows)
          (total/version-key *layer *name (get *row :revision) :> *vkey)
          (local-transform> [(keypath *workspace *vkey) (termval *row)] $$versions))
        (<<if (get *outcome :delete)
          (local-transform> [(keypath *workspace *key) NONE>] $$rows))
        (<<if (mutates? *outcome)
          (total/index-keys *current :> *before)
          (total/index-keys (get *outcome :row) :> *after)
          (<<atomic
            (ops/explode (set/union (set *before) (set *after)) :> *bucket)
            (str *layer "/" *bucket :> *index-key)
            (contains? (set *after) *bucket :> *add?)
            (local-select> [(keypath *workspace *index-key)] $$index :> *members)
            (index-change *members *name *add? :> *next-members)
            (local-transform> [(keypath *workspace *index-key) (termval *next-members)] $$index)))
        (get *outcome :decision :> *decision)
        (hash-map :request *op :decision *decision :> *receipt)
        (local-transform> [(keypath *workspace *rid) (termval *receipt)] $$decisions)
        ;; Registration is a separate metadata event. All accepted material,
        ;; indexes and its decision were written together before this boundary.
        (<<if (register-workspace? *op *outcome)
          (|global)
          (local-transform> [(keypath *workspace) (termval true)] $$workspaces))
        (ack-return> *decision)))))
