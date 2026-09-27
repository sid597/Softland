(ns softland.inland.facts
  "Inland's records as facts the rig's store holds, and back. Pure: no store,
  no clock, no I/O.

  An Inland record is a map with a name and attributes, for example
  `{:name \"targeting\" :label \"..\" :body {:steps [] :return [:get :subject]}}`.
  The store holds facts: a thing, a key, a value. Carried from the pointer
  session's start (`Softland-pointer-2026-09-27/.../pointer/facts.clj`, 27
  September), with the note's grammar and the screen's cells added.

  Placeholders, none a ruling (the build's PROGRESS.md lists them):
  - A thing's id is made from the record's name (`entity`): `\"targeting\"` is
    `:record/targeting`, never `:targeting`, which is the `:targeting` key's
    own id; `\"file/h01\"` is `:file/h01`; `\"@base\"` is `:base`. Ingest
    identity is open (PROGRESS.md \"Now\", items 17, 59, 60).
  - One fact per attribute, the name included: a record's grain can be
    coarsened later, never refined.
  - A layer's id is the keyword of Inland's layer name.
  - A stored session cell (a selection, the context with its pins) is a fact
    on the browser session's own thing under a `:cell/` key, in the person's
    hand layer.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]))

;; ------------------------------------------------------------------ ids

(defn entity
  "A record's name as the id of its thing. A plain name is in the `record`
  namespace, so a record never shares an id with a key: keys are ids and a
  key's grammar is a fact about its id, so a record named \"targeting\" as
  `:targeting` would be the `:targeting` key's grammar thing (found on the
  real cluster, 27 September). A name with a slash is the thing it names
  (`\"file/h01\"` is the material's `:file/h01`); a name with a leading `@`
  is a store entity (`\"@base\"` is the base layer's own `:base`, where its
  settings are). A name starting with a digit is escaped with `_`: a
  keyword's name may not start with one."
  [name]
  (cond (str/starts-with? name "@") (keyword (subs name 1))
        (str/includes? name "/") (keyword name)
        :else (keyword "record" (if (re-matches #"\d.*" name) (str "_" name) name))))

(defn record-name
  "A thing's id as the record name Inland's recipes use; `entity` reversed."
  [e]
  (cond (= "record" (namespace e)) (let [n (name e)] (if (re-matches #"_\d.*" n) (subs n 1) n))
        (namespace e) (str (namespace e) "/" (name e))
        :else (str "@" (name e))))

(defn layer-id "An Inland layer name as a store layer." [s] (keyword s))
(defn layer-name "A store layer as the name Inland's recipes use." [l] (name l))

(defn cell-key "The key a stored cell is written under: \"selection\" is `:cell/selection`." [cell] (keyword "cell" cell))

;; ------------------------------------------------------------- records

(defn record->facts
  "One record as facts on its thing: `{:e :k :v}` for every attribute that
  has a value, the name included. An attribute whose value is nil is left
  out: in the store a fact with no value is a retraction."
  [record]
  (let [e (entity (:name record))]
    (vec (for [[k v] (sort-by (comp str key) record)
               :when (some? v)]
           {:e e :k k :v v}))))

(defn namespace-of-read?
  "Whether a key is one of the store's own read-entry keys."
  [k]
  (and (keyword? k) (= "read" (namespace k))))

(defn heads
  "The current fact for each key among one thing's rows in one layer: the
  latest by stamp that no other row replaces. `rows` are the read exit's
  (`:e :k :value :fid :stamp :replaces`, or `:erased-at` in place of
  `:value`). A head with no value is a retraction, kept so a caller can tell
  \"taken away in this layer\" from \"never said here\"; a forget fact and a
  read entry's lines are not heads of anything."
  [rows]
  (let [rows (remove #(or (= :forget (:k %)) (namespace-of-read? (:k %))) rows)
        replaced (into #{} (keep :replaces) rows)]
    (->> rows
         (remove #(contains? replaced (:fid %)))
         (group-by :k)
         (into {} (map (fn [[k rs]] [k (last (sort-by (juxt :stamp #(second (:fid %))) rs))]))))))

(defn compose
  "One thing's facts across layers, nearest first, as the record Inland's
  recipes read. For each key the nearest layer that says anything wins, and
  a retraction there hides every layer beneath it. Nil when no layer says
  anything that still stands, else the record with:

    :name            the thing's name
    :revision        the largest stamp among the facts it is made of: one
                     number that names this exact version
    :resolved-layer  the nearest layer that contributed
    :basis           {key {:fid :stamp :layer}}, the facts it is made of
    :erased          {key date} for a key whose value was forgotten

  `by-layer` is `[[layer heads] ...]`, nearest first, `heads` as `heads`
  gives them."
  [name by-layer]
  (let [ks (distinct (mapcat (comp keys second) by-layer))
        chosen (into {}
                     (for [k ks
                           :let [[layer row] (some (fn [[layer hs]] (when-let [r (get hs k)] [layer r])) by-layer)]
                           :when (and row (or (contains? row :value) (contains? row :erased-at)))]
                       [k (assoc row :layer layer)]))]
    (when (seq chosen)
      (let [nearest (some (fn [[layer _]] (when (some #(= layer (:layer %)) (vals chosen)) layer)) by-layer)
            erased (into {} (keep (fn [[k r]] (when (contains? r :erased-at) [k (:erased-at r)]))) chosen)]
        (cond-> (-> (into {} (keep (fn [[k r]] (when (contains? r :value) [k (:value r)]))) chosen)
                    (assoc :name name
                           :revision (reduce max (map :stamp (vals chosen)))
                           :resolved-layer (layer-name nearest)
                           :basis (into {} (map (fn [[k r]] [k (select-keys r [:fid :stamp :layer])])) chosen)))
          (seq erased) (assoc :erased erased))))))

(def derived-parts
  "What `compose` adds to a record and Inland's admission added to a row.
  None of it is the person's to write, so an edit drops it."
  #{:revision :resolved-layer :basis :erased :asserted-by :accepted-at :accepted-request})

(defn authored
  "A record with everything the store derives taken off: what the person
  sees in the editor and what an edit sends back."
  [record]
  (apply dissoc record derived-parts))

(defn changes
  "The facts that turn the record `before` into `after` in layer `layer`,
  where `basis` is `before`'s (`compose`'s `:basis`): a fact for every
  attribute whose value differs, replacing the fact it was read from when
  that fact sits in the same layer, and a retraction for every attribute
  taken away. An attribute read from a layer beneath is not replaced, since
  a fact in one layer cannot replace a fact in another; the new fact
  shadows it. `{:facts [...] :stood-on {fid stamp}}`, the act standing on
  every fact of `before` it changes."
  [layer before after basis]
  (let [a (authored after)
        b (authored before)
        e (entity (:name a))
        ks (sort-by str (distinct (concat (keys a) (keys b))))
        facts (vec (for [k ks
                         :let [was (get b k) now (get a k) from (get basis k)]
                         :when (not= was now)]
                     (cond-> {:e e :k k}
                       (some? now) (assoc :v now)
                       (and from (= layer (:layer from))) (assoc :replaces (:fid from)))))
        stood (into {} (for [k ks
                             :let [from (get basis k)]
                             :when (and from (not= (get a k) (get b k)))]
                         [(:fid from) (:stamp from)]))]
    {:facts facts :stood-on stood}))

(defn record-in
  "One layer's record for `name` from the rows of `[:e thing]` in that layer,
  or nil when the layer says nothing that still stands."
  [name layer rows]
  (compose name [[layer (heads rows)]]))

(defn bucket-patterns
  "The patterns one layer's rows for an index bucket are read from: the
  bucket's own key, and `:removed` for a tombstone. Inland's buckets
  (`total/index-keys`) plus `key/<k>`, every thing that says key k."
  [bucket]
  (let [with-removed #(vector [:k %] [:k :removed])]
    (cond
      (or (str/starts-with? bucket "event/") (str/starts-with? bucket "demand/")) (with-removed :pattern)
      (str/starts-with? bucket "catalog/") (with-removed :catalog)
      (= bucket "definitions") (with-removed :body)
      (= bucket "rows") (with-removed :name)
      (str/starts-with? bucket "activities/") [[:k :status] [:k :activity]]
      (str/starts-with? bucket "key/") (with-removed (keyword (subs bucket 4)))
      :else nil)))

(defn bucket-names
  "The sorted names in `bucket` from one layer's rows of its patterns
  (`rows-of`, pattern -> rows), as `total/index-keys` puts a row in it; a
  removed thing is in no bucket."
  [bucket rows-of]
  (let [by-e (group-by :e (mapcat val rows-of))
        head (fn [e k] (get (heads (by-e e)) k))
        value (fn [e k] (:value (head e k)))
        member? (fn [e]
                  (and (not (value e :removed))
                       (cond
                         (str/starts-with? bucket "event/") (= (subs bucket 6) (some-> (value e :pattern) :event name))
                         (str/starts-with? bucket "demand/") (= (subs bucket 7) (some-> (value e :pattern) :demand name))
                         (str/starts-with? bucket "catalog/") (= (subs bucket 8) (value e :catalog))
                         (= bucket "definitions") (some? (value e :body))
                         (= bucket "rows") (some? (value e :name))
                         (= bucket "activities/pending") (and (value e :activity) (= :pending (value e :status)))
                         (= bucket "activities/running") (and (value e :activity) (= :running (value e :status)))
                         ;; a forgotten value still heads its key: it shows as erased, never as gone
                         (str/starts-with? bucket "key/") (let [h (head e (keyword (subs bucket 4)))]
                                                            (or (contains? h :value) (contains? h :erased-at)))
                         :else false)))]
    ;; only records and material things are in a bucket: a thing with no namespace is
    ;; the store's own (a layer's settings, a permission, a session), never a record
    (vec (sort (for [e (keys by-e) :when (and (keyword? e) (namespace e) (member? e))] (record-name e))))))

(defn parse-source
  "The editor's text as a record: `{:value record}` or `{:error message}`.
  EDN only, at most 24,000 characters, a map. Nothing is evaluated."
  [source]
  (try
    (cond
      (not (string? source)) {:error "The record is not text."}
      (> (count source) 24000) {:error "The record exceeds the 24KB read budget."}
      :else (let [v (edn/read-string source)]
              (if (map? v) {:value v} {:error "Enter an EDN record (a map)."})))
    (catch Throwable _ {:error "The record is not valid EDN."})))

;; ------------------------------------------------------------- grammars

(def step-ops
  "The step names Inland's runner knows (softland.inland.total/vocabulary),
  as data, so the store's gate can check a recipe's form."
  [:value :read :session :call :index :query :derive])

(def name-shape [:string 100])

(def body-shape
  "A recipe: named steps and a declared return; `total/shape-error` said as
  a shape the gate checks."
  [:map {:steps [:vector-of
                 [:map {:out [:keyword]
                        :op (into [:enum] step-ops)
                        :args [:any]
                        :when [:any]}
                  {:optional #{:args :when}}]
                 0 64]
         :return [:any]}])

(def pattern-shape
  [:map {:event [:keyword]
         :demand [:keyword]
         :reads [:vector-of [:any] 0 32]}
   {:optional #{:event :demand :reads}}])

(defn grammar
  "A grammar fact for key `k`: its value's shape, whose value paths name the
  people it is about (`subjects-at`), not opaque, and its index hints."
  ([k shape] (grammar k shape nil #{}))
  ([k shape subjects-at index]
   {:e k :k :grammar :v {:shape shape :subjects-at subjects-at :opaque false :index index}}))

(def record-grammars
  "The keys Inland's records use, each with the shape its values have.
  `:name` and `:catalog` are indexed by value, so a thing can be found by the
  word people call it and a catalog can list its members."
  [(grammar :name name-shape nil #{:by-value})
   (grammar :label [:string 200])
   (grammar :body body-shape)
   (grammar :pattern pattern-shape)
   (grammar :catalog name-shape nil #{:by-value})
   (grammar :targeting name-shape)
   (grammar :presentation name-shape)
   (grammar :closed-view name-shape)
   (grammar :parent name-shape)
   (grammar :neighbors [:vector-of name-shape 0 64])
   (grammar :shape [:map {:kind [:keyword]} {:open? true}])
   (grammar :session [:any])
   (grammar :machine-behavior [:enum :candidate :direct])
   (grammar :ordinary-content [:enum :candidate :direct])])

(def note-grammar
  "A note on the material, whose grammar says who it names: its `:names`, a
  set of one to four people, are the value's subjects (ruling 8's grammar
  source). In a shared layer a note about one person dies with them; in a
  person's own layer it dies with its owner and survives the people it
  names (the sharpening of rulings 7, 7b and 8)."
  (grammar :note [:map {:on [:any] :text [:string 2000] :names [:set-of [:keyword] 1 4]} {:open? true}]
           [:names] #{}))

(def cell-grammars
  "The cells the screen stores: the selection (the target a gesture chose)
  and the context (which layers are read, nearest first, and what is
  pinned)."
  [(grammar :cell/selection [:any])
   (grammar :cell/context [:map {:layers [:vector-of name-shape 1 8] :pins [:any]}])])
