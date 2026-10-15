(ns softland.runtime.store-model
  "A model of the store, for the runtime's pure proof: the rules of the
  store's gates and read path that the runtime's steps lean on, over a
  plain map, with no cluster. It is a model and no store: the real gates
  and read path are proven by the store's own proofs, and the steps on them
  by the runtime's proof on a cluster.

  Every act that it is sent is first held to the act's one form
  (`softland.store.act/parse`), and every query to the query's
  (`softland.store.read-path.query/check`); a thing that fails is kept in
  `:form-faults`, so the proof can say that the steps made none.

  The rules it keeps, each from the store's code or the plan of this build:

    - a layer is made by a making act alone
      (`softland.store.gate.first-act/making-act?`), and a second making of
      it is refused;
    - an act's permission must be the row at its `:who`'s place, its holder
      the act's `:who`; a permission that allows `:see` does not act; each
      key of the act must be allowed (`softland.store.act/allows-key?`);
    - a fact that names a fact to replace must name the current one;
    - a setting sits on the layer's own entity;
    - a give is straight beneath the act's permission, in its layer, and
      allows no more (`softland.store.act/allows-within?`); its place takes
      one write while it holds a value;
    - an end of a right is a retract at its place, from a permission above
      it (`softland.store.gate.chain/above?`);
    - a key that the layer marks in `:softland.store/one-write-keys` takes
      one write while its place holds a value, unless the fact names the
      fact it replaces;
    - a read sees a layer that is made and set to `:everyone`, or one where
      the asker holds a permission with a value; a row under a store key
      counts only when the query asks for the store's own facts.

  With `:lag-base? true`, reads of the base see the rows as they were
  before the last accepted act: a read of a microbatch layer that is behind
  its gate.

  Inputs and results, by call:

    empty-store  options -> a store
    read-query   a store, a query -> [store answer]
    send-act     a store, an act -> [store outcome]

  State. None: a store is a value, and each call gives the next one."
  (:require [softland.store.act :as act]
            [softland.store.gate.chain :as chain]
            [softland.store.gate.first-act :as first-act]
            [softland.store.read-path.query :as query])
  (:import [java.util UUID]))

(defn empty-store
  "Gives a store that holds nothing. Option `:lag-base?`."
  [& {:keys [lag-base?]}]
  {:rows {} :base-view {} :stamps {} :acts 0 :lag-base? (boolean lag-base?) :form-faults []})

(defn- store-key?
  "True for a key in the store's namespace."
  [k]
  (= act/store-namespace (namespace k)))

(def ^:private setting-keys
  "The keys that sit on a layer's own entity."
  #{:softland.store/depot :softland.store/who-may-see :softland.store/builds-on
    :softland.store/marked-keys :softland.store/one-write-keys})

(defn- row
  "The row at a place, in the rows given."
  [rows layer e k]
  (get rows [layer e k]))

(defn- holds-value?
  "True for a row that holds a value."
  [r]
  (= :value (:holds r)))

(defn- made?
  "True when the rows hold the layer's depot."
  [rows layer]
  (some? (row rows layer layer :softland.store/depot)))

(defn- fault
  "The first fault of one fact of an act in a made layer, or nil."
  [rows the-act {:keys [e k v replaces] :as _fact}]
  (let [layer (:layer the-act)
        permission (:permission the-act)
        here (row rows layer e k)
        marks (let [r (row rows layer layer :softland.store/one-write-keys)] (when (holds-value? r) (:value r)))]
    (cond
      (not (act/allows-key? (act/allows permission) k)) :key-not-allowed
      (and replaces (not= replaces (:fact here))) :replaced-fact-not-current
      (and (contains? setting-keys k) (not= e layer)) :setting-not-on-the-layer
      (= :softland.store/depot k) :store-key-takes-one-write

      (and (= :softland.store/permission k) (nil? v))
      (cond (not (holds-value? here)) :no-right-to-end
            (not (chain/above? permission (:value here))) :end-not-from-above
            :else nil)

      (= :softland.store/permission k)
      (cond (or (not= layer (nth v 1)) (not= layer (nth v 2)) (not= permission (nth v 3 nil)))
            :permission-fact-not-beneath-the-acts-own
            (not (act/allows-within? (act/allows v) (act/allows permission)))
            :permission-fact-allows-more-than-the-givers
            (holds-value? here) :store-key-takes-one-write
            :else nil)

      (and (some? v) (nil? replaces) (contains? marks k) (holds-value? here)) :place-takes-one-write
      :else nil)))

(defn- decide
  "The model's decision of an act in its one form: nil to accept, or the
  word."
  [rows the-act]
  (let [layer (:layer the-act)]
    (if-not (made? rows layer)
      (when-not (first-act/making-act? (first-act/depot-at-making layer) the-act) :layer-not-made)
      (let [who-row (row rows layer (:who the-act) :softland.store/permission)]
        (cond
          (not= (:who the-act) (first (:permission the-act))) :who-not-the-permissions-holder
          (not (and (holds-value? who-row) (= (:permission the-act) (:value who-row)))) :permission-does-not-stand
          (= :see (act/allows (:permission the-act))) :permission-does-not-allow-acting
          :else (some #(fault rows the-act %) (:facts the-act)))))))

(defn- accept
  "Writes each fact of an accepted act as a row. Gives the rows."
  [rows the-act act-uuid stamp]
  (let [layer (:layer the-act)
        act-id [layer (first-act/depot-at-making layer) act-uuid]]
    (reduce (fn [rows [i {:keys [e k v]}]]
              (assoc rows [layer e k]
                     (cond-> {:holds (if (nil? v) :nothing-here :value) :fact [act-id i] :stamp stamp}
                       (some? v) (assoc :value v))))
            rows
            (map-indexed vector (:facts the-act)))))

(defn send-act
  "Takes a store and an act. Gives [store outcome], the outcome as the door
  gives one: {:answer :accepted :stamp s} or {:answer :refused :reason w}."
  [store the-act]
  (let [parsed (act/parse the-act)]
    (if-let [reason (:refuse parsed)]
      [(update store :form-faults conj {:act the-act :reason reason}) {:answer :refused :reason reason}]
      (let [a (:ok parsed)
            rows (:rows store)
            word (decide rows a)
            n (inc (:acts store))]
        (if word
          [(assoc store :acts n) {:answer :refused :reason word}]
          (let [stamp (inc (get-in store [:stamps (:layer a)] 0))
                new-rows (accept rows a (UUID. 0 n) stamp)]
            [(assoc store
                    :acts n
                    :rows new-rows
                    :base-view (if (:lag-base? store) rows new-rows)
                    :stamps (assoc (:stamps store) (:layer a) stamp))
             {:answer :accepted :stamp stamp}]))))))

(defn- rows-for
  "The rows that a read of a layer sees."
  [store layer]
  (if (= first-act/base layer) (:base-view store) (:rows store)))

(defn- standing
  "Whether the asker of a query may see a layer: :yes, :may-not-see or
  :not-made."
  [rows q layer]
  (let [setting (row rows layer layer :softland.store/who-may-see)
        who (if (holds-value? setting) (:value setting) (if (= first-act/base layer) :everyone :members))]
    (cond
      (not (made? rows layer)) :not-made
      (= :everyone who) :yes
      (nil? (:asker q)) :may-not-see
      (holds-value? (row rows layer (:asker q) :softland.store/permission)) :yes
      :else :may-not-see)))

(defn- place-answer
  "The place-answer of a row in a layer."
  [layer r]
  (if (nil? r)
    {:holds :no-fact}
    (cond-> {:holds (:holds r) :layer layer :fact (:fact r) :stamp (:stamp r)}
      (holds-value? r) (assoc :value (:value r)))))

(defn read-query
  "Takes a store and a query. Gives [store answer], the answer as the read
  path gives one for `:place` and `:entity` through the layers named; a
  query not in its form gives the read path's word and is kept as a fault."
  [store q]
  (let [checked (query/check q)]
    (if-let [word (:refuse checked)]
      [(update store :form-faults conj {:query q :reason word}) {:could-not :read-the-query :why word}]
      (let [q (:ok checked)
            [word a b] (:asks q)
            layers (:layers q)
            counts? (fn [k] (or (:stores-own-facts q) (not (store-key? k))))
            seen (filter #(= :yes (standing (rows-for store %) q %)) layers)
            left-out (keep #(let [s (standing (rows-for store %) q %)]
                              (when (not= :yes s) {:layer % :why s}))
                           layers)
            answer {:layers layers :left-out (vec left-out)}]
        [store
         (case word
           :place (assoc answer :place
                         (or (some (fn [l] (when-let [r (and (counts? b) (row (rows-for store l) l a b))]
                                             (place-answer l r)))
                                   seen)
                             {:holds :no-fact}))
           :entity (assoc answer :places
                          (into {}
                                (for [l (reverse seen)
                                      [[rl e k] r] (rows-for store l)
                                      :when (and (= rl l) (= e a) (counts? k))]
                                  [[e k] (place-answer l r)])))
           (assoc answer :could-not :not-in-the-model))]))))
