(ns softland.inland.execution
  "Tracked interpretation of bounded authored recipes inside Electric.
   Takes explicit workspace/context, session cells, definitions and bindings; gives
   values, applicability bindings, support sets or tagged read/execution outcomes.
   Holds only demand-owned Electric branches and their source subscriptions; borrows
   Rama authority through store and browser-local cells through session. Named calls
   resolve accepted records; all external reads are explicit steps or pattern reads.
   Field resolution shadows whole records by layer; it does not merge attributes.
   Pure relational closure is a bounded leaf computation over explicit inputs, not
   an incremental solver internal to Electric. See logic for that separate limit."
  (:require [hyperfiddle.electric3 :as e]
            [missionary.core :as m]
            [softland.inland.total :as total]
            [softland.inland.logic :as logic]
            #?(:clj [softland.inland.store :as store])
            #?(:cljs [softland.inland.session :as session])))

(e/defn ReadStatus
  "Owner plus PState kind/path → continuous tagged source state on the server.
   Emits pending before ProxyState acquisition; subsequent values/absence/failure
   come from the owned store flow. Cancelling the branch releases that subscription."
  [owner kind path]
  (e/server
    (e/input (m/relieve (fn [_ v] v)
               (m/reductions (fn [_ v] v) {:status :pending}
                 (store/watch-path owner kind path))))))

(e/defn Read
  "Address → accepted value, nil for complete absence, or runtime/status record.
   Pending and failed source reads stay tagged; neither is treated as absence."
  [owner kind path]
  (let [r (ReadStatus owner kind path)]
    (case (:status r)
      :value (:value r)
      :absent nil
      {:runtime/status (:status r) :reason (:reason r) :path path})))

(e/defn Local
  "Session and string key → tracked browser-local cell value.
   A missing cell is allocated by session/cell; this never reads accepted Rama state."
  [s key]
  (e/client (e/watch (session/cell s key))))

(e/defn Index
  "Context layers and bucket → sorted distinct names across the relevant layers.
   Waits for one result per distinct layer before declaring the collection complete.
   Each bucket is a stored vector: this is scoped lookup, not paged index traversal."
  [owner workspace context bucket]
  (let [layers (vec (distinct (:layers context ["base"])))
        collections (e/for-by identity [layer layers]
                      (Read owner :index [workspace (str layer "/" bucket)]))
        blocked (total/collection-state (count layers) collections)]
    (or blocked (vec (sort (distinct (mapcat identity collections)))))))

(e/defn ResolveLayers
  "Ordered layers, name and optional revision → first visible row or nil.
   A tagged read blocks fallback; a tombstone ends resolution. Pins read retained
   versions instead of current rows; module documents the delete/recreate limit. Returned rows carry their resolved-layer."
  [owner workspace layers name pin]
  (if (seq layers)
    (let [layer (first layers)
          row (Read owner (if pin :versions :rows)
                [workspace (if pin (total/version-key layer name pin) (total/row-key layer name))])]
      (cond (total/blocked? row) row
            (:removed row) nil
            row (assoc row :resolved-layer layer)
            :else (ResolveLayers owner workspace (vec (rest layers)) name pin)))
    nil))

(e/defn Resolve
  "Session/context and name → contextual current row or pinned version.
   A map pin fixes both layer and revision; a scalar revision uses the layer order.
   The session argument is passed through the calling convention, not read here."
  [s owner workspace context name]
  (let [pin (get (:pins context) name)]
    (ResolveLayers owner workspace (if (map? pin) [(:layer pin)] (:layers context ["base"]))
      name (if (map? pin) (:revision pin) pin))))

(e/defn Field
  "Layered address and keyword/path → narrowly subscribed attribute value.
   Reads row presence, tombstone and requested attribute separately. Once a row is
   present, its nil attribute is complete absence; lower layers are not consulted.
   A map pin fixes the layer/revision, and a tagged read preserves uncertainty."
  [owner workspace layers name attr pin]
  (if (map? pin)
    (Field owner workspace [(:layer pin)] name attr (:revision pin))
    (if (seq layers)
    (let [layer (first layers)
          key (if pin (total/version-key layer name pin) (total/row-key layer name))
          kind (if pin :versions :rows)
          present (Read owner kind [workspace key :name])
          removed (Read owner kind [workspace key :removed])
          value (Read owner kind (into [workspace key] (if (vector? attr) attr [attr])))]
      (cond (total/blocked? present) present
            (total/blocked? removed) removed
            removed nil
            (total/blocked? value) value
            (some? present) value
            :else (Field owner workspace (vec (rest layers)) name attr pin)))
    nil)))

(e/declare Recipe Call Query)

(e/defn Step
  "One named step and existing bindings → its value or tagged outcome.
   Evaluates args and optional when, validates capability inputs, then opens tracked
   read/session/index/call/query dependencies or invokes pure finite derivation.
   An explicit layer replaces the context for that read/index and drops its pins."
  [s owner workspace context step scope depth]
  (let [args (total/evaluate (:args step) scope)
        enabled (if (contains? step :when) (total/evaluate (:when step) scope) true)]
    (cond
      (total/blocked? args) args
      (total/blocked? enabled) enabled
      (not enabled) nil
      (total/step-error (:op step) args) {:runtime/status :failed :reason (total/step-error (:op step) args)}
      :else
      (case (:op step)
        :value args
        :session (Local s (:key args))
        :read (let [read-context (if (:layer args) {:layers [(:layer args)]} context)]
                (if (contains? args :attr)
                  (Field owner workspace (:layers read-context ["base"]) (:name args) (:attr args) (get (:pins read-context) (:name args)))
                  (Resolve s owner workspace read-context (:name args))))
        :index (Index owner workspace (if (:layer args) {:layers [(:layer args)]} context) (:key args))
        :call (Call s owner workspace context (:name args) (:bindings args) (inc depth))
        :query (Query s owner workspace context (:demand args) (:bindings args) (inc depth))
        :derive (logic/derive args)
        {:runtime/status :failed :reason "This capability requires an execution owner."}))))

(e/defn Steps
  "Ordered steps, bindings and return expression → a composed recipe value.
   Each out/op pair owns its Electric branch; completed values extend the bindings.
   A tagged step outcome stops later steps. Replacing an opcode releases old work
   without feeding differently shaped arguments into the previous branch."
  [s owner workspace context steps scope depth result]
  (if (seq steps)
    (e/for [step (e/diff-by (juxt :out :op) [(first steps)])]
      (let [value (Step s owner workspace context step scope depth)]
        (if (total/blocked? value) value
          (Steps s owner workspace context (vec (rest steps)) (assoc scope (:out step) value) depth result))))
    (total/evaluate result scope)))

(e/defn Recipe
  "Recipe body, bindings and call depth → returned value or failure/exhaustion.
   Validates named-step shape and rejects depth above 24. Repetition belongs to an
   activity owner; recursion here is bounded by the call-depth check."
  [s owner workspace context body scope depth]
  (cond (> depth 24) {:runtime/status :exhausted :reason "Named call depth exceeded 24; use owned total steps for repetition."}
        (total/shape-error body) {:runtime/status :failed :reason (total/shape-error body)}
        :else (Steps s owner workspace context (:steps body) scope depth (:return body))))

(e/defn Call
  "Definition name and explicit bindings → live invocation of its resolved body.
   Missing definitions return tagged absence, not a host exception. The runtime
   context governs address resolution; bindings also receive a context value."
  [s owner workspace context name bindings depth]
  (let [row (Resolve s owner workspace context name)]
    (cond (total/blocked? row) row
          (nil? row) {:runtime/status :absent :reason (str "No definition resolves: " name)}
          :else (Recipe s owner workspace context (:body row) (merge {:context context} bindings) depth))))

(e/defn Conditions
  "Ordered pattern reads and bindings → extended bindings, nil mismatch or status.
   Reads a session cell, context path or addressed field. Equals compares values;
   not matches only completed nil; a bare clause requires a non-nil value. Each
   successful bind is available to later clauses. Unknown reads never satisfy not."
  [s owner workspace context clauses bindings]
  (if (seq clauses)
    (let [clause (first clauses)
          value (cond (:session clause) (Local s (:session clause))
                      (:context clause) (get-in context (:context clause))
                      :else (let [name (total/evaluate (:name clause) bindings)]
                              (Field owner workspace (:layers context ["base"]) name (:attr clause) (get (:pins context) name))))
          expected (when (contains? clause :equals) (total/evaluate (:equals clause) bindings))
          matches (cond (total/blocked? value) value
                        (:not clause) (nil? value)
                        (contains? clause :equals) (= expected value)
                        :else (some? value))]
      (cond (total/blocked? matches) matches
            (not matches) nil
            :else (Conditions s owner workspace context (vec (rest clauses))
                    (cond-> bindings (:bind clause) (assoc (:bind clause) value)))))
    bindings))

(e/defn Applicable
  "Resolved row and initial bindings → the pattern's matching bindings.
   Missing/removed rows do not apply. Event/demand identity is checked by the caller;
   this function only evaluates the row's declared read conditions."
  [s owner workspace context row bindings]
  (if (or (nil? row) (:removed row)) nil
    (Conditions s owner workspace context (get-in row [:pattern :reads]) bindings)))

(e/defn Query
  "Demand keyword and bindings → maintained conclusions with alternative supports.
   Uses the scoped demand index, resolves/matches each row, and waits for every
   branch before unioning equal values. Support identity is [name revision] for the
   answering definition; it is not a stored transitive provenance graph. Query/call
   depth is bounded; positive recursive closure is provided separately by derive."
  [s owner workspace context demand bindings depth]
  (if (> depth 24) {:runtime/status :exhausted :reason "Recursive demand needs a finite relational recipe or an owned step."}
    (let [names (Index owner workspace context (str "demand/" (name demand)))]
    (if (total/blocked? names) names
      (let [supports (e/for-by identity [name names]
                       (let [row (Resolve s owner workspace context name)
                             matched (if (total/blocked? row) row
                                       (when (= demand (get-in row [:pattern :demand]))
                                         (Applicable s owner workspace context row bindings)))]
                         (cond (total/blocked? matched) matched
                               (nil? matched) nil
                               :else {:support [name (:revision row)]
                                      :value (Recipe s owner workspace context (:body row) matched depth)})))
            blocked (total/collection-state (count names) supports)]
        (if blocked blocked
          (mapv (fn [[value basis]] {:value value :supports basis})
            (total/conclude (remove nil? supports)))))))))

(e/defn Dispatch
  "Event kind and context → applicable rule results with definition/revision ids.
   Only that event's indexed candidates are evaluated. An incomplete collection
   stays pending. This function is reactive; app/Events owns the one-shot snapshot
   and effect application, so dispatch itself performs no durable or local writes."
  [s owner workspace context event]
  (let [names (Index owner workspace context (str "event/" (name (:kind event))))]
    (if (total/blocked? names) names
      (let [results (e/for-by identity [name names]
        (let [row (Resolve s owner workspace context name)
              bindings (if (total/blocked? row) row
                         (when (= (:kind event) (get-in row [:pattern :event]))
                           (Applicable s owner workspace context row {:event event :context context})))]
          (cond (total/blocked? bindings) bindings
                (nil? bindings) nil
                :else {:support [name (:revision row)]
                       :value (Recipe s owner workspace context (:body row) bindings 0)})))]
        (or (total/collection-state (count names) results) results)))))
