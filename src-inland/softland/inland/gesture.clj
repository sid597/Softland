(ns softland.inland.gesture
  "A gesture, once, on the server: one record per gesture.
  Takes an event, the session's local cells and its reader part in the
  store; gives the effects the rules decided, after every read the rules
  made is recorded in ONE entry act (rig.store.read-exit `record!`).

  Inland's events already snapshot their reads once (app/Events): a
  gesture's rules are not maintained views. So the rules run here, one
  shot, by the same language `execution` runs views in: named steps, the
  leaves of `total`, layered resolution by whole record, pins, buckets,
  pattern conditions. Every read is a query through the one exit, recorded
  nothing yet and shown to nobody; the answers pass through to the rules;
  then one act records them all; only on its yes do the effects run, and
  the acts they write stand on every fact the gesture read. The test
  suite's `reference-call` interprets value/call/read steps the same way
  as a fixture; this is its production form for events.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [rig.store.read-exit :as rx]
            [softland.inland.facts :as f]
            [softland.inland.logic :as logic]
            [softland.inland.total :as total]))

(def max-depth "Named call depth, as `execution/Recipe` bounds it." 24)

(defn begin
  "A gesture's reading state: the store, the exit's reader part `who`, the
  session's thing `S` (its stored cells' entity, in layer `hand`, Inland's
  name for it), the keys of the stored cells, and the local cells the
  browser sent."
  [store {:keys [who S hand stored-cells cells]}]
  {:store store :who who :S S :hand hand :stored-cells (set stored-cells) :cells (or cells {})
   :queried (atom []) :memo (atom {})})

(defn- read!
  "One read of the gesture, through the exit's first two steps, once per
  (layer, pattern, moment): its rows, or a tagged outcome when refused."
  [g layer read as-of]
  (let [k [layer read as-of]]
    (or (get @(:memo g) k)
        (let [[spec answer] (rx/checked-query (:store g) (:who g)
                                              ;; the acts the gesture writes stand on what it read (ruling 3's role)
                                              (cond-> {:layer layer :read read :role :stood-on} as-of (assoc :as-of as-of)))
              v (if (contains? answer :refused)
                  {:runtime/status :failed :reason (str "The store refused the read: " (name (:refused answer)))}
                  (vec (:rows answer)))]
          (swap! (:queried g) conj [spec answer])
          (swap! (:memo g) assoc k v)
          v))))

(defn- rows-of [g layer e as-of]
  (read! g (f/layer-id layer) [:pattern [:e e]] (when as-of {:stamp as-of})))

(defn- record-in
  "A layer's record for `name`, as of revision `as-of` when pinned."
  [g layer name as-of]
  (let [rows (rows-of g layer (f/entity name) as-of)]
    (if (total/blocked? rows) rows (f/record-in name layer rows))))

(defn resolve-record
  "`execution/ResolveLayers`: the first layer's record, whole; a tombstone
  ends resolution; a refused read blocks fallback."
  [g layers name pin]
  (loop [layers (seq layers)]
    (when layers
      (let [layer (first layers)
            row (record-in g layer name pin)]
        (cond (total/blocked? row) row
              (:removed row) nil
              row (assoc row :resolved-layer layer)
              :else (recur (next layers)))))))

(defn- resolve-in-context [g context name]
  (let [pin (get (:pins context) name)]
    (resolve-record g (if (map? pin) [(:layer pin)] (:layers context ["base"])) name (if (map? pin) (:revision pin) pin))))

(defn field
  "`execution/Field`: the first layer whose record is present answers,
  nil attribute included."
  [g layers name attr pin]
  (if (map? pin)
    (field g [(:layer pin)] name attr (:revision pin))
    (loop [layers (seq layers)]
      (when layers
        (let [row (record-in g (first layers) name pin)]
          (cond (total/blocked? row) row
                (:removed row) nil
                (some? (:name row)) (get-in row (if (vector? attr) attr [attr]))
                :else (recur (next layers))))))))

(defn index
  "`execution/Index`: the sorted distinct names in a bucket across layers."
  [g layers bucket]
  (let [per (for [layer (distinct layers)]
              (let [rows-of (into {} (for [p (f/bucket-patterns bucket)] [p (read! g (f/layer-id layer) [:pattern p] nil)]))]
                (or (some #(when (total/blocked? %) %) (vals rows-of))
                    (f/bucket-names bucket rows-of))))]
    (or (some #(when (total/blocked? %) %) per)
        (vec (sort (distinct (mapcat identity per)))))))

(defn cell
  "A session cell: a stored one is read from the session's thing in the hand
  layer (so the gesture stands on it); a local one is what the browser sent."
  [g key]
  (if (contains? (:stored-cells g) key)
    (let [rows (read! g (f/layer-id (:hand g)) [:pattern [:e (:S g)]] nil)]
      (if (total/blocked? rows) rows (:value (get (f/heads rows) (f/cell-key key)))))
    (get (:cells g) key)))

(declare recipe call query)

(defn- step [g context st scope depth]
  (let [args (total/evaluate (:args st) scope)
        enabled (if (contains? st :when) (total/evaluate (:when st) scope) true)]
    (cond
      (total/blocked? args) args
      (total/blocked? enabled) enabled
      (not enabled) nil
      (total/step-error (:op st) args) {:runtime/status :failed :reason (total/step-error (:op st) args)}
      :else
      (case (:op st)
        :value args
        :session (cell g (:key args))
        :read (let [ctx (if (:layer args) {:layers [(:layer args)]} context)]
                (if (contains? args :attr)
                  (field g (:layers ctx ["base"]) (:name args) (:attr args) (get (:pins ctx) (:name args)))
                  (resolve-in-context g ctx (:name args))))
        :index (index g (:layers (if (:layer args) {:layers [(:layer args)]} context) ["base"]) (:key args))
        :call (call g context (:name args) (:bindings args) (inc depth))
        :query (query g context (:demand args) (:bindings args) (inc depth))
        :derive (logic/derive args)
        {:runtime/status :failed :reason "This capability requires an execution owner."}))))

(defn recipe
  "`execution/Recipe`: named steps then the return, depth-bounded."
  [g context body scope depth]
  (cond (> depth max-depth) {:runtime/status :exhausted :reason "Named call depth exceeded 24; use owned total steps for repetition."}
        (total/shape-error body) {:runtime/status :failed :reason (total/shape-error body)}
        :else (loop [steps (seq (:steps body)) scope scope]
                (if steps
                  (let [st (first steps)
                        v (step g context st scope depth)]
                    (if (total/blocked? v) v (recur (next steps) (assoc scope (:out st) v))))
                  (total/evaluate (:return body) scope)))))

(defn call
  "`execution/Call`: a definition's body with explicit bindings."
  [g context name bindings depth]
  (let [row (resolve-in-context g context name)]
    (cond (total/blocked? row) row
          (nil? row) {:runtime/status :absent :reason (str "No definition resolves: " name)}
          :else (recipe g context (:body row) (merge {:context context} bindings) depth))))

(defn- conditions
  "`execution/Conditions`: pattern reads extend the bindings, or nil."
  [g context clauses bindings]
  (loop [clauses (seq clauses) bindings bindings]
    (if-not clauses
      bindings
      (let [clause (first clauses)
            value (cond (:session clause) (cell g (:session clause))
                        (:context clause) (get-in context (:context clause))
                        :else (let [name (total/evaluate (:name clause) bindings)]
                                (field g (:layers context ["base"]) name (:attr clause) (get (:pins context) name))))
            expected (when (contains? clause :equals) (total/evaluate (:equals clause) bindings))
            matches (cond (total/blocked? value) value
                          (:not clause) (nil? value)
                          (contains? clause :equals) (= expected value)
                          :else (some? value))]
        (cond (total/blocked? matches) matches
              (not matches) nil
              :else (recur (next clauses) (cond-> bindings (:bind clause) (assoc (:bind clause) value))))))))

(defn- applicable [g context row bindings]
  (when-not (or (nil? row) (:removed row))
    (conditions g context (get-in row [:pattern :reads]) bindings)))

(defn query
  "`execution/Query`: a demand's answers with their supports."
  [g context demand bindings depth]
  (if (> depth max-depth)
    {:runtime/status :exhausted :reason "Recursive demand needs a finite relational recipe or an owned step."}
    (let [names (index g (:layers context ["base"]) (str "demand/" (name demand)))]
      (if (total/blocked? names) names
        (let [supports (for [n names]
                         (let [row (resolve-in-context g context n)
                               matched (if (total/blocked? row) row
                                         (when (= demand (get-in row [:pattern :demand]))
                                           (applicable g context row bindings)))]
                           (cond (total/blocked? matched) matched
                                 (nil? matched) nil
                                 :else {:support [n (:revision row)] :value (recipe g context (:body row) matched depth)})))]
          (or (total/first-failure supports)
              (mapv (fn [[value basis]] {:value value :supports basis})
                    (total/conclude (remove nil? supports)))))))))

(defn dispatch
  "`execution/Dispatch`: every rule the event's kind indexes in the context
  layers that applies, with its effects and its support `[name revision]`."
  [g context event]
  (let [names (index g (:layers context ["base"]) (str "event/" (name (:kind event))))]
    (if (total/blocked? names) names
      (vec (keep (fn [n]
                   (let [row (resolve-in-context g context n)
                         bindings (if (total/blocked? row) row
                                    (when (= (:kind event) (get-in row [:pattern :event]))
                                      (applicable g context row {:event event :context context})))]
                     (cond (total/blocked? bindings) bindings
                           (nil? bindings) nil
                           :else {:support [n (:revision row)]
                                  :value (recipe g context (:body row) bindings 0)})))
                 names)))))

;; ------------------------------------------------------------ the record

(defn stood-on
  "Every fact the gesture read, `{fid stamp}`: what its acts stand on. The
  store's own read lines are not facts anyone built on."
  [g]
  (into {} (for [[_ answer] @(:queried g)
                 :when (not (contains? answer :refused))
                 row (:rows answer)
                 :when (and (:fid row) (int? (:stamp row)) (not (f/namespace-of-read? (:k row))))]
             [(:fid row) (:stamp row)])))

(defn record!
  "One entry act for every read the gesture made. The act's answer with
  `:entry`, or `{:answer :none}` when it read nothing that records."
  [g]
  (rx/record! (:store g) (:who g) @(:queried g)))
