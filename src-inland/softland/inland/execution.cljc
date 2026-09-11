(ns softland.inland.execution
  "Tracked recipe steps and indexed applicability inside actual Electric.
   Takes a demand/context, explicit scope and accepted recipes. Gives values
   and support identities. Holds only demand-owned Electric branches."
  (:require [hyperfiddle.electric3 :as e]
            [missionary.core :as m]
            [softland.inland.total :as total]
            [softland.inland.logic :as logic]
            #?(:clj [softland.inland.store :as store])
            #?(:cljs [softland.inland.session :as session])))

(e/defn ReadStatus [owner kind path]
  (e/server
    (e/input (m/relieve (fn [_ v] v)
               (m/reductions (fn [_ v] v) {:status :pending}
                 (store/watch-path owner kind path))))))

(e/defn Read [owner kind path]
  (let [r (ReadStatus owner kind path)]
    (case (:status r)
      :value (:value r)
      :absent nil
      {:runtime/status (:status r) :reason (:reason r) :path path})))

(e/defn Local [s key]
  (e/client (e/watch (session/cell s key))))

(e/defn Index [owner workspace context bucket]
  (let [collections (e/for-by identity [layer (:layers context ["base"])]
                      (Read owner :index [workspace (str layer "/" bucket)]))
        blocked (some #(when (total/blocked? %) %) collections)]
    (or blocked (vec (sort (distinct (mapcat identity collections)))))))

(e/defn ResolveLayers [owner workspace layers name pin]
  (if (seq layers)
    (let [layer (first layers)
          row (Read owner (if pin :versions :rows)
                [workspace (if pin (total/version-key layer name pin) (total/row-key layer name))])]
      (cond (total/blocked? row) row
            (:removed row) nil
            row (assoc row :resolved-layer layer)
            :else (ResolveLayers owner workspace (vec (rest layers)) name pin)))
    nil))

(e/defn Resolve [s owner workspace context name]
  (let [pin (get (:pins context) name)]
    (ResolveLayers owner workspace (if (map? pin) [(:layer pin)] (:layers context ["base"]))
      name (if (map? pin) (:revision pin) pin))))

(e/defn Field [owner workspace layers name attr pin]
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

(e/defn Step [s owner workspace context step scope depth]
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

(e/defn Steps [s owner workspace context steps scope depth result]
  (if (seq steps)
    (e/for [step (e/diff-by (juxt :out :op) [(first steps)])]
      (let [value (Step s owner workspace context step scope depth)]
        (if (total/blocked? value) value
          (Steps s owner workspace context (vec (rest steps)) (assoc scope (:out step) value) depth result))))
    (total/evaluate result scope)))

(e/defn Recipe [s owner workspace context body scope depth]
  (cond (> depth 24) {:runtime/status :exhausted :reason "Named call depth exceeded 24; use owned total steps for repetition."}
        (total/shape-error body) {:runtime/status :failed :reason (total/shape-error body)}
        :else (Steps s owner workspace context (:steps body) scope depth (:return body))))

(e/defn Call [s owner workspace context name bindings depth]
  (let [row (Resolve s owner workspace context name)]
    (cond (total/blocked? row) row
          (nil? row) {:runtime/status :absent :reason (str "No definition resolves: " name)}
          :else (Recipe s owner workspace context (:body row) (merge {:context context} bindings) depth))))

(e/defn Conditions [s owner workspace context clauses bindings]
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

(e/defn Applicable [s owner workspace context row bindings]
  (if (or (nil? row) (:removed row)) nil
    (Conditions s owner workspace context (get-in row [:pattern :reads]) bindings)))

(e/defn Query [s owner workspace context demand bindings depth]
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
            blocked (some #(cond (total/blocked? %) %
                                 (total/blocked? (:value %)) (:value %)) supports)]
        (if blocked blocked
          (mapv (fn [[value basis]] {:value value :supports basis})
            (total/conclude (remove nil? supports)))))))))

(e/defn Dispatch [s owner workspace context event]
  (let [names (Index owner workspace context (str "event/" (name (:kind event))))]
    (if (total/blocked? names) names
      (e/for-by identity [name names]
        (let [row (Resolve s owner workspace context name)
              bindings (if (total/blocked? row) row
                         (when (= (:kind event) (get-in row [:pattern :event]))
                           (Applicable s owner workspace context row {:event event :context context})))]
          (cond (total/blocked? bindings) bindings
                (nil? bindings) nil
                :else {:support [name (:revision row)]
                       :value (Recipe s owner workspace context (:body row) bindings 0)}))))))
