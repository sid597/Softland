;; IMPORTANT: Before modifying this file, re-read PLAN-read-exit.md (its fixes
;; F1 to F12 are marked in place) and BUILD_NOTES-read-exit.md, and adhere to
;; their decisions.
(ns rig.store.reads
  "Stage 5a, the one-owner read exit, module side (PLAN-read-exit.md): the
  index entries the gate writes for every admitted act, the purge by value id
  and the paged rebuild from the log, the two query topologies `read-point`
  and `read-pattern`, the read entry's facts, and the keyed fingerprint.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key; the
  sorted String an index entry sits under is its address, never its key.

  Everything here that runs inside a topology is total: a pure function
  catches every throwable and returns a refusal or an empty write list,
  because an exception in topology code is fatal to the worker, in a stream
  topology (RIG.md, phase 0) and in a query topology alike (probed by this
  build, runs/phase5-read-build-probe.txt).

  Requires rig.store.envelope only among the store's namespaces: the gate
  requires this namespace, and the module requires both, so requiring
  either here would be a cycle. The open step is one seam here, `open-row>`,
  with phase 2's planned signature and returns; tonight it passes values
  through (a rig choice), and phase 2's merge makes its body a call to
  `rig.store.locks/open-row>`."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.ops :as ops]
            [rig.store.envelope :as env])
  (:import [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]))

;; ------------------------------------------------------------------ hints

(def seed-hints
  "The index hints tonight (RC7, F7), a constant standing in for phase 6's
  grammar facts: `:by-value` keys get a value index; `:opaque` keys never do
  (ruling 6; a key in both is opaque); `:no-copy` keys get id-index entries
  without their value fields (the store's own read lines: written on every
  read, read rarely). Changing hints for an existing layer needs a rebuild
  of its indexes."
  {:by-value #{:note} :opaque #{} :no-copy #{:read/point :read/pattern}})

(defn current-hints
  "The hints the module decides with. A function, so the topology code calls
  it rather than embedding the constant."
  []
  seed-hints)

(defn opaque? [hints k] (contains? (:opaque hints) k))

(defn by-value?
  "Whether the key's values are indexed by value: hinted so and not opaque."
  [hints k]
  (and (contains? (:by-value hints) k) (not (opaque? hints k))))

(defn copy?
  "Whether an id-index entry of this key carries the row's value fields."
  [hints k]
  (not (contains? (:no-copy hints) k)))

(def value-fields
  "The row fields that hold a value or anything derived from it: dropped from
  a `:no-copy` id-index entry and from every tombstone. `:v` tonight; phase
  2's sealed value, its record lock and its value digest when they land. A
  lock id stays: it is an id, as the fact id is."
  #{:v :sealed :lock :digest})

(def read-keys "The store-owned fact keys of read entries (FR6), first-record." #{:read/point :read/pattern})

(def roles "The seed roles (ruling 3), placeholders: FR10." #{:stood-on :shown :matched :passed-through})

(def reader-kinds #{:person :model :tool})

;; -------------------------------------------------------------- addresses

(def sep "The part separator of an address, U+0000 (RC4)." "\u0000")

(defn- kw-text
  "A readable keyword's printed text without the colon; it holds no U+0000."
  [k]
  (subs (str k) 1))

(defn hex16
  "A stamp as 16 lowercase hex digits, so String order is stamp order."
  [stamp]
  (format "%016x" (long stamp)))

(defn- fid-text
  "A fact id's part of an address: its name's canonical text, U+0000, and its
  index as 8 hex digits, so the facts of one act sort in the act's order (a
  rig choice, see BUILD_NOTES-read-exit.md: the canonical text of the whole
  id would sort index 10 before index 9)."
  [[nm idx]]
  (str (env/canonical nm) sep (format "%08x" (long idx))))

(defn address
  "The address of a fact's entry in one index field (RC4): parts joined by
  U+0000. `:ix-ek` is e, k, stamp, fact id; `:ix-ke` is k, e, stamp, fact id;
  `:ix-kv` is k, the value text's length in chars as 8 hex digits, the value
  text, stamp, fact id. Unique per fact: the fact id is the last part."
  [field {:keys [e k vtext stamp fid]}]
  (case field
    :ix-ek (str (kw-text e) sep (kw-text k) sep (hex16 stamp) sep (fid-text fid))
    :ix-ke (str (kw-text k) sep (kw-text e) sep (hex16 stamp) sep (fid-text fid))
    :ix-kv (str (kw-text k) sep (format "%08x" (count vtext)) sep vtext sep (hex16 stamp) sep (fid-text fid))))

(defn- kv-prefix [k vtext]
  (str (kw-text k) sep (format "%08x" (count vtext)) sep vtext sep))

;; ------------------------------------------------------- schema (install)

(defn index-entry
  "An index entry's schema: the log row's fields, whatever they are (F6;
  phase 1's :e :k :v :replaces :mark, and phase 2's fields when they land),
  plus the fact id, its act's stamp, a purge's date, and the no-copy flag
  (F7). module.clj passes its row field map in."
  [row-fields]
  (fixed-keys-schema
   (merge row-fields
          {:fid       clojure.lang.PersistentVector
           :stamp     Long
           :erased-at Long
           :copy      Boolean})))

(defn layer-fields
  "The four fields this stage adds to a layer's value in `$$layers`: three
  String-addressed indexes whose entries carry the row, and the reverse map
  from a fact id to its `:ix-kv` addresses, so a purge needs no value."
  [row-fields]
  (let [entry (index-entry row-fields)]
    {:ix-ek (map-schema String entry {:subindex-options {:track-size? false}})
     :ix-ke (map-schema String entry {:subindex-options {:track-size? false}})
     :ix-kv (map-schema String entry {:subindex-options {:track-size? false}})
     :ix-of (map-schema clojure.lang.PersistentVector (set-schema String)
                        {:subindex-options {:track-size? false}})}))

(def index-fields #{:ix-ek :ix-ke :ix-kv})
(def all-fields #{:ix-ek :ix-ke :ix-kv :ix-of})

;; --------------------------------------------------------- index entries

(def no-index-writes {:index-put [] :index-of [] :index-del []})

(defn tombstone
  "A purged fact's id-index entry: the row without its value fields, with the
  fact id, its stamp and the forget's date. No hints: a tombstone is the same
  whoever wrote it (purge or rebuild)."
  [fid stamp row erased-at]
  (-> (apply dissoc row value-fields)
      (assoc :fid fid :stamp stamp :erased-at erased-at)))

(defn- fact-writes
  "One fact's index writes. `plain` is the value's canonical text (nil for a
  retract or a value that did not open); `erased-at` a date when the value is
  erased, which gives tombstones and no value entry."
  [hints fid stamp row plain erased-at]
  (let [e (:e row)
        k (:k row)
        ek (address :ix-ek {:e e :k k :stamp stamp :fid fid})
        ke (address :ix-ke {:e e :k k :stamp stamp :fid fid})]
    (if (some? erased-at)
      (let [t (tombstone fid stamp row erased-at)]
        {:index-put [[:ix-ek ek t] [:ix-ke ke t]] :index-of []})
      (let [full (assoc row :fid fid :stamp stamp)
            id-entry (if (copy? hints k) full (assoc (apply dissoc full value-fields) :copy false))
            kv (when (and (by-value? hints k) (string? plain))
                 (address :ix-kv {:k k :vtext plain :stamp stamp :fid fid}))]
        {:index-put (cond-> [[:ix-ek ek id-entry] [:ix-ke ke id-entry]]
                      kv (conj [:ix-kv kv full]))
         :index-of (if kv [[fid #{kv}]] [])}))))

(defn merge-writes
  "Concatenate write lists, in order, for the three write blocks. Phase 2's
  forget merges one `purge-writes` per value into its own decision's lists
  with this."
  [& ws]
  {:index-put (into [] (mapcat :index-put) ws)
   :index-of (into [] (mapcat :index-of) ws)
   :index-del (into [] (mapcat :index-del) ws)})

(defn index-writes
  "The index writes of an admitted act (W2), for the gate's decision event:
  an `:ix-ek` and an `:ix-ke` entry per fact (the row whole, or without its
  value fields for a `:no-copy` key), and for a fact whose key is indexed by
  value and whose value is not nil, an `:ix-kv` entry and its `:ix-of` set.
  Every write a set at a computed address, so a replay writes the same.

  `rows` are the log rows as the gate writes them; `plain`, optional, the
  facts' plaintext value texts in the same order, default each row's `:v`
  (phase 2, sealing values at the door, passes the texts it opened at
  decision). Pure and total: a throwable gives empty lists and
  `:index-error`, which the gate turns into its unrecorded :gate-error."
  ([hints layer nm rows stamp] (index-writes hints layer nm rows stamp nil))
  ([hints _layer nm rows stamp plain]
   (try
     (let [plain (or plain (mapv :v rows))]
       (apply merge-writes no-index-writes
              (for [[i row] (map-indexed vector rows)]
                (fact-writes hints [nm (long i)] stamp row (nth plain i nil) nil))))
     (catch Throwable _ (assoc no-index-writes :index-error true)))))

(defn purge-writes
  "Purge one value from the indexes by its value id (W3): the call phase 2's
  forget makes in its own decision event, once per value, for a value forget
  and for every value a person forget kills. Its inputs are what that event
  reads anyway, the fact's row and its act's stamp, plus one seek,
  `[(keypath layer :ix-of fid)]`, for `kv-addresses` (nil when none).
  Returns the id-index entries as tombstones dated `forget-stamp` (no value
  needed: the addresses come from the row's entity and key, the stamp and
  the fact id) and the deletes of every `:ix-kv` address and of the fact's
  `:ix-of` entry. After it no index holds the value or anything derived from
  it; the id, entity, key and stamp stay, so a read as of any moment shows the
  fact with its erasure date. Pure and total (empty lists on a throwable)."
  [fid row fact-stamp kv-addresses forget-stamp]
  (try
    (let [fid [(into [] (nth fid 0)) (long (nth fid 1))]
          ek (address :ix-ek {:e (:e row) :k (:k row) :stamp fact-stamp :fid fid})
          ke (address :ix-ke {:e (:e row) :k (:k row) :stamp fact-stamp :fid fid})
          t (tombstone fid (long fact-stamp) row (long forget-stamp))]
      {:index-put [[:ix-ek ek t] [:ix-ke ke t]]
       :index-of []
       :index-del (conj (into [] (map (fn [a] [:ix-kv a])) (sort (filter string? kv-addresses)))
                        [:ix-of fid])})
    (catch Throwable _ no-index-writes)))

(defn- open-plain
  "The value text an open result gives for a value index, or nil."
  [o]
  (when (and (map? o) (contains? o :value) (some? (:value o)))
    (env/encode-value (:value o))))

(defn- row-writes
  "One row's writes during a rebuild, from its open result: tombstones for a
  value `open-row>` says is erased, the ordinary entries otherwise (a value
  entry only when it opened to a value)."
  [hints fid stamp row o]
  (fact-writes hints fid stamp row (open-plain o) (when (map? o) (:erased-at o))))

(defn put-page-writes
  "A rebuild put page's writes (F2): every entry and `:ix-of` set that one
  page of yes acts implies. `acts` is `[{:name nm :stamp s :rows [row ...]
  :opens [open-result ...]} ...]`, each row's `open-row>` result aligned
  with the rows. Puts only; a page never deletes. Pure and total."
  [hints _layer acts]
  (try
    (apply merge-writes no-index-writes
           (for [{:keys [name stamp rows opens]} acts
                 [i row] (map-indexed vector rows)]
             (row-writes hints [name (long i)] stamp row (nth opens i nil))))
    (catch Throwable _ no-index-writes)))

(defn implied
  "What the log implies for a layer's four fields: `{field {address entry}}`,
  and `{:ix-of {fid #{address}}}`, for `acts` shaped as `put-page-writes`
  takes them (the whole log's yes acts). What a finished rebuild leaves."
  [hints layer acts]
  (let [{:keys [index-put index-of]} (put-page-writes hints layer acts)]
    (reduce (fn [m [f a e]] (assoc-in m [f a] e))
            {:ix-ek {} :ix-ke {} :ix-kv {} :ix-of (into {} index-of)}
            index-put)))

(defn- fid-ok?
  "A well-formed fact id [name idx] (envelope's own check is private)."
  [x]
  (and (vector? x) (= 2 (count x)) (env/valid-name? (nth x 0))
       (int? (nth x 1)) (<= 0 (nth x 1))))

(defn- norm-fid [x] [(into [] (nth x 0)) (long (nth x 1))])

(defn- fact-implied
  "The entries one fact implies now, `{[field address] entry}`, with its
  `:ix-of` set under `:ix-of`; empty when its act is not a yes or it has no
  row."
  [hints fid rec row o]
  (if (and (fid-ok? fid) (= :yes (:answer rec)) (some? row) (int? (:stamp rec)))
    (let [{:keys [index-put index-of]} (row-writes hints (norm-fid fid) (:stamp rec) row o)]
      (assoc (into {} (map (fn [[f a e]] [[f a] e])) index-put)
             :ix-of (second (first index-of))))
    {}))

(defn sweep-page-writes
  "A rebuild sweep page's writes (F2). `entries` is one page of field
  `field`, `[[address value] ...]` (for `:ix-of` the address is the fact id
  and the value its set); `found` aligns with it, `{:rec :row :open :ix-of}`
  for each entry's fact (`:ix-of` the fact's set, read for `:ix-kv` pages).
  An entry the log does not imply is deleted, and for an `:ix-kv` address
  its removal from the fact's `:ix-of` set; an entry at an address the log
  implies but with other content is rewritten to the implied one (so the
  loop ends with exactly what the log implies; a divergence from the plan's
  delete, BUILD_NOTES-read-exit.md); an `:ix-of` set that differs from the
  implied one is rewritten, or deleted when none is implied. Pure and total."
  [hints _layer field entries found]
  (try
    (let [out (atom no-index-writes)
          add! (fn [k x] (swap! out update k conj x))
          kv-removed (atom {})]
      (doseq [[[a v] f] (map vector entries found)]
        (if (= :ix-of field)
          (let [fid a
                want (:ix-of (fact-implied hints fid (:rec f) (:row f) (:open f)))]
            (cond
              (empty? want) (add! :index-del [:ix-of fid])
              (not= want v) (add! :index-of [fid want])))
          (let [fid (:fid v)
                imp (fact-implied hints fid (:rec f) (:row f) (:open f))
                want (get imp [field a])]
            (cond
              (and (some? want) (= want v)) nil
              (some? want) (add! :index-put [field a want])
              :else (do (add! :index-del [field a])
                        (when (and (= :ix-kv field) (fid-ok? fid) (set? (:ix-of f)))
                          (swap! kv-removed update (norm-fid fid)
                                 (fn [[s removed]] [(or s (:ix-of f)) (conj (or removed #{}) a)]))))))))
      (doseq [[fid [s removed]] @kv-removed
              :let [left (reduce disj s removed)]]
        (cond
          (empty? left) (add! :index-del [:ix-of fid])
          (not= left s) (add! :index-of [fid left])))
      (assoc @out :deleted (count (:index-del @out)) :rewritten (+ (count (:index-put @out)) (count (:index-of @out)))))
    (catch Throwable _ (assoc no-index-writes :deleted 0 :rewritten 0))))

;; ------------------------------------------------------------------ open

(defonce ^{:doc "Test only (R3; the in-process cluster runs every task in this JVM):
  a function (layer fid row stamp T) -> open result, or nil to fall through to
  the pass-through. Stands in for phase 2's erasure in tests (T19). A kept
  store has no such hook; phase 2's merge drops it with the pass-through."}
  open-double
  (atom nil))

(defn open-row
  "Tonight's open step, pure (a rig choice: values stay unsealed and the open
  step passes them through): one of `{:value v :stamp s}` (v nil for a
  retract), `{:erased-at s}`, `{:unreadable reason}` with `:no-such-fact`,
  `:after-moment` (the fact's stamp is after T; T nil means now) or
  `:does-not-open`, phase 2's return shapes. Never throws."
  [layer fid row stamp T]
  (try
    (or (when-let [f @open-double] (f layer fid row stamp T))
        (cond
          (or (not (map? row)) (not (int? stamp))) {:unreadable :no-such-fact}
          (and (int? T) (> stamp T)) {:unreadable :after-moment}
          :else {:value (env/decode-value (:v row)) :stamp stamp}))
    (catch Throwable _ {:unreadable :does-not-open})))

(deframafn open-row>
  "The one function every shown value passes through (phase 2's `open-row>`,
  F6: the twin of `open-value>` given a row and its act's stamp already read,
  so nothing is read twice). An index entry is the row plus its own fields,
  so it passes as the row. Called on the layer's home task, in a query
  topology or a gate event; it must not repartition. Phase 2's merge makes
  its body a call to `rig.store.locks/open-row>`, whose contract wins."
  [*layer *fid *row *stamp *T]
  (:> (open-row *layer *fid *row *stamp *T)))

;; ----------------------------------------------------------- the moment

(defn moment
  "The stamp a read is as of (FR2, first-record): min(asked, clock), where
  clock is the home task's last stamp read in the same query; nil asked
  means the clock. Every fact admitted on the task after the read is stamped
  above the clock, so the read is final."
  [as-of clock]
  (let [clock (long (or clock 0))]
    (if (int? as-of) (min (long as-of) clock) clock)))

(defn- as-of-ok? [x] (or (nil? x) (and (int? x) (<= 0 x))))

;; ------------------------------------------------------------ visibility

(defn visible?
  "Ruling 9's default as a constant (RC6), decided for the person the read
  is for (F12), never for an agent's or a tool's own actor id: personal,
  hand and agent layers to their owner; the base to any actor. A layer not
  made answers the same as a private one (F4)."
  [settings for]
  (boolean
   (and (map? settings) (env/readable-keyword? for)
        (case (:kind settings)
          (:personal :hand :agent) (= for (:owner settings))
          :base true
          false))))

(defn placed-by-layer?
  "Whether the layer's facts are this store's to read: a layer re-classed by
  entity is read through the micro store after phase 3 (refused
  `:re-classed` meanwhile, a rig choice)."
  [settings]
  (= :by-layer (:class settings)))

;; --------------------------------------------------------------- parsers

(def max-point-fids "Fact ids per point read (RC2)." 1000)
(def default-limit "Rows per pattern read by default (RC2)." 1000)
(def max-limit 10000)
(def pattern-value-depth
  "The collection depth at which a `[:kv k v]` value is checked, so v nests
  in at most 28 collections and the recorded line holding the pattern stays
  within the envelope's 32."
  4)

(defn parse-point
  "A point read's inputs, total: `{:fids [fid ...] :as-of s-or-nil}`, or
  `{:refused :bad-read}` for a list that is not a non-empty vector of at
  most 1,000 well-formed fact ids, or a moment that is not nil or a
  non-negative integer. (An empty list is refused: a read of nothing has no
  entry to record, since an act of no facts is refused on its face.)"
  [_for fids as-of]
  (try
    (if (and (vector? fids) (seq fids) (<= (count fids) max-point-fids)
             (every? fid-ok? fids) (as-of-ok? as-of))
      {:fids (mapv norm-fid fids) :as-of (some-> as-of long)}
      {:refused :bad-read})
    (catch Throwable _ {:refused :bad-read})))

(defn parse-pattern
  "A pattern read's inputs, total (the pattern language tonight, RC1):
  `[:all]`, `[:e e]`, `[:ek e k]`, `[:latest e k]`, `[:k k]`, `[:kv k v]`.
  Returns `{:kind :pattern :ix :prefix :limit :as-of :v :vtext}` or
  `{:refused r}`: `:bad-pattern` for anything outside the language, a limit
  outside 1 to 10,000, a malformed moment, or a `[:kv]` value that is nil,
  not EDN data the store holds, or nested past 28 collections; `:opaque` for
  a `[:kv]` read of an opaque key, `:not-indexed` for a key not indexed by
  value. A `[:kv]` value is normalised as the gate normalises it, so its
  address text matches the admitted one."
  ([pattern limit as-of] (parse-pattern pattern limit as-of (current-hints)))
  ([pattern limit as-of hints]
   (try
     (let [kw? env/readable-keyword?
           lim (if (nil? limit) default-limit limit)
           bad {:refused :bad-pattern}]
       (cond
         (not (and (int? lim) (<= 1 lim max-limit))) bad
         (not (as-of-ok? as-of)) bad
         (not (vector? pattern)) bad
         :else
         (let [[kind a b] pattern
               n (count pattern)
               base {:limit (long lim) :as-of (some-> as-of long)}]
           (cond
             (and (= kind :all) (= n 1))
             (assoc base :kind :all :pattern [:all] :ix :ix-ek :prefix "")

             (and (= kind :e) (= n 2) (kw? a))
             (assoc base :kind :e :pattern [:e a] :ix :ix-ek :prefix (str (kw-text a) sep))

             (and (= kind :ek) (= n 3) (kw? a) (kw? b))
             (assoc base :kind :ek :pattern [:ek a b] :ix :ix-ek :prefix (str (kw-text a) sep (kw-text b) sep))

             (and (= kind :latest) (= n 3) (kw? a) (kw? b))
             (assoc base :kind :latest :pattern [:latest a b] :ix :ix-ek :prefix (str (kw-text a) sep (kw-text b) sep))

             (and (= kind :k) (= n 2) (kw? a))
             (assoc base :kind :k :pattern [:k a] :ix :ix-ke :prefix (str (kw-text a) sep))

             (and (= kind :kv) (= n 3) (kw? a))
             (cond
               (opaque? hints a) {:refused :opaque}
               (not (by-value? hints a)) {:refused :not-indexed}
               (or (nil? b) (not (env/edn-value? b pattern-value-depth))) bad
               :else (let [v (env/normalize-value b)
                           vtext (env/encode-value v)]
                       (assoc base :kind :kv :pattern [:kv a v] :ix :ix-kv :v v
                              :prefix (kv-prefix a vtext))))

             :else bad))))
     (catch Throwable _ {:refused :bad-pattern}))))

(defn- prefix-end
  "The least String above every String that starts with `prefix`, when
  `prefix` ends in U+0000: the prefix with that last char raised to U+0001."
  [prefix]
  (str (subs prefix 0 (dec (count prefix))) "\u0001"))

(defn bounds
  "Where a pattern read's range starts and ends as of moment m: `[:ek]` and
  `[:kv]` fold the moment into the end (prefix + hex(m + 1)), `[:latest]`
  into its tail bound; `[:all]` runs to the map's end; `[:e]` and `[:k]` end
  with their prefix and filter by stamp. [from end], end nil for none."
  [pp m]
  (let [p (:prefix pp)]
    (case (:kind pp)
      :all ["" nil]
      (:e :k) [p (prefix-end p)]
      (:ek :kv :latest) [p (str p (hex16 (inc (long m))))])))

;; ----------------------------------------------------- the page loop, pure

(def first-page "The first page of a pattern read's range (RC3)." 16)
(def budget-factor "Entries scanned per row allowed, past which a read is partial (F8)." 16)

(defn page-init
  "The page loop's state before its first page: pages of min(16, limit + 1)
  entries, doubling (RC3), capped by what is left of the scan budget of
  16 × (limit + 1) entries (F8)."
  [pp m]
  (let [[from end] (bounds pp m)
        limit (:limit pp)
        budget (* budget-factor (inc limit))]
    {:ix (:ix pp) :kind (:kind pp) :v (:v pp) :from from :end end :m (long m)
     :limit limit :budget budget :page (min first-page (inc limit) budget)
     :scanned 0 :matched 0 :kept [] :done? false :more? false}))

(defn page-entries
  "A page's entries as `[[address entry] ...]`, in address order."
  [sub]
  (try (into [] (map (fn [e] [(key e) (val e)])) sub) (catch Throwable _ [])))

(defn- in-range? [st a] (let [end (:end st)] (or (nil? end) (neg? (compare a end)))))

(defn- stamp-ok? [st e] (let [s (:stamp e)] (and (int? s) (<= s (:m st)))))

(defn kv-candidates
  "For a `[:kv]` page, the entries to open, in order: in range, stamped at or
  before the moment, not tombstones. Empty for other kinds (F5: a value
  index entry is a candidate until its value opens to the pattern's)."
  [st entries]
  (try
    (if (= :kv (:kind st))
      (into [] (comp (take-while (fn [[a _]] (in-range? st a)))
                     (filter (fn [[_ e]] (and (map? e) (stamp-ok? st e) (nil? (:erased-at e))))))
            entries)
      [])
    (catch Throwable _ [])))

(defn kv-need
  "How many more `[:kv]` matches the read looks for: limit + 1 - matched."
  [st]
  (- (inc (:limit st)) (:matched st)))

(defn kv-hit?
  "Whether an opened `[:kv]` candidate matches: it opened to a value equal
  to the pattern's (F5)."
  [st o]
  (boolean (and (map? o) (contains? o :value) (some? (:value o)) (= (:value o) (:v st)))))

(defn need-after [need hit?] (if hit? (dec need) need))

(defn page-step
  "The page loop's pure step over one page of entries (read from `:from`,
  at most `:page` of them). Keeps entries below the end whose stamp is at or
  before the moment (for `[:kv]`, only those `opened` says match; `opened`
  maps an address to its open result); counts what it scanned and what
  matched; stops at the range's end or the map's, at the limit + 1st match
  (which it neither keeps nor returns, F3: it only decides the mark), or
  when the scan budget is spent (F8, partial); else says where the next page
  starts (the last address followed by U+0000, the least String above it)
  and how big it is (twice the last, capped by the budget left)."
  [st entries opened]
  (try
    (loop [es entries scanned (:scanned st) kept (:kept st) matched (:matched st)]
      (let [done (fn [more?] (assoc st :scanned scanned :kept kept :matched matched :done? true :more? more?))]
        (if (empty? es)
          (let [left (- (:budget st) scanned)]
            (cond
              (< (count entries) (:page st)) (done false)
              (<= left 0) (done true)
              :else (assoc st :scanned scanned :kept kept :matched matched
                           :from (str (first (peek entries)) sep)
                           :page (min (* 2 (:page st)) left))))
          (let [[a e] (first es)]
            (if-not (in-range? st a)
              (done false)
              (let [scanned (inc scanned)
                    o (get opened a)
                    hit? (and (map? e) (stamp-ok? st e)
                              (if (= :kv (:kind st)) (and (nil? (:erased-at e)) (kv-hit? st o)) true))]
                (cond
                  (not hit?) (recur (rest es) scanned kept matched)
                  (= matched (:limit st)) (assoc (done true) :scanned scanned)
                  :else (recur (rest es) scanned (conj kept {:address a :entry e :opened o}) (inc matched)))))))))
    (catch Throwable _ (assoc st :done? true :more? true :failed? true))))

(defn tail-state
  "A `[:latest e k]` read's result from its tail read (the one entry just
  below the bound): kept only when its address has the pattern's prefix,
  so it is about e with key k; never partial."
  [pp sub m]
  (try
    (let [[[a e]] (page-entries sub)]
      {:kept (if (and a (map? e) (.startsWith ^String a ^String (:prefix pp))
                      (int? (:stamp e)) (<= (:stamp e) m))
               [{:address a :entry e :opened nil}]
               [])
       :more? false})
    (catch Throwable _ {:kept [] :more? false})))

;; ---------------------------------------------------------- answer rows

(defn- row-base [fid stamp row]
  {:fid fid :stamp stamp :e (:e row) :k (:k row) :replaces (:replaces row) :mark (:mark row)})

(defn- with-open-result [r o]
  (cond
    (and (map? o) (contains? o :value)) (assoc r :value (:value o))
    (and (map? o) (contains? o :erased-at)) (assoc r :erased-at (:erased-at o))
    (and (map? o) (contains? o :unreadable)) (assoc r :unreadable (:unreadable o))
    :else (assoc r :unreadable :does-not-open)))

(defn yes-by?
  "Whether a name's record is an admitted act stamped at or before m."
  [rec m]
  (boolean (and (map? rec) (= :yes (:answer rec)) (int? (:stamp rec)) (<= (:stamp rec) m))))

(defn absent-row [fid] {:fid fid :absent true})

(defn point-row
  "A point read's row for a fact that is there: its parts and the open."
  [fid stamp row o]
  (try (with-open-result (row-base fid stamp row) o)
       (catch Throwable _ {:fid fid :unreadable :does-not-open})))

(defn show-how
  "How step 6 of a pattern read shows a kept entry: `:tomb` (a purged fact,
  its date only, nothing opened), `:opened` (a `[:kv]` match opened in the
  page loop), `:read-row` (a `:no-copy` entry: its row is read, F7), or
  `:open` (the entry is the row)."
  [kept]
  (let [e (:entry kept)]
    (cond
      (some? (:erased-at e)) :tomb
      (some? (:opened kept)) :opened
      (false? (:copy e)) :read-row
      :else :open)))

(defn shown-row
  "A kept entry's answer row, from the entry and its open result (nil for a
  tombstone)."
  [kept o]
  (try
    (let [e (:entry kept)
          r (row-base (:fid e) (:stamp e) e)]
      (if (some? (:erased-at e))
        (assoc r :erased-at (:erased-at e))
        (with-open-result r (or o (:opened kept)))))
    (catch Throwable _ {:fid (get-in kept [:entry :fid]) :unreadable :does-not-open})))

(defn kept-name-idx
  "A kept entry's act name and row index, for reading its row."
  [kept]
  (let [[nm idx] (get-in kept [:entry :fid])] [nm idx]))

;; ----------------------------------------------------------- fingerprint

(def fp-secret-id
  "The fingerprint secret's id, named in every pattern line (FR12,
  first-record). The plan's `:read-fp/1` is not EDN (a keyword's name cannot
  start with a digit), so a line holding it would be refused as malformed;
  `:read-fp/v1` is the simplest readable placeholder. A rotation is a new id,
  and old lines stay checkable under the old one."
  :read-fp/v1)

(def ^:private root-secret
  "The store's root secret. A constant in code tonight, as throwaway as phase
  1's digest constant (the rig keeps no records); where a kept store keeps it
  is edition one's (phase 2's lock store or the operator's secret store)."
  (.getBytes "rig-2026-09-25 root secret, throwaway" "UTF-8"))

(defn- hmac [k msg]
  (let [^bytes k k
        ^bytes msg msg
        mac (Mac/getInstance "HmacSHA256")]
    (.init mac (SecretKeySpec. k "HmacSHA256"))
    (.doFinal mac msg)))

(defn- hex [bs]
  (let [^bytes bs bs
        sb (StringBuilder.)]
    (dotimes [i (alength bs)]
      (.append sb (format "%02x" (bit-and (long (aget bs i)) 0xff))))
    (.toString sb)))

(def ^:private fp-secret
  "The fingerprint secret, derived and never stored (FR12, first-record):
  HMAC-SHA256(root, \"softland/read-fingerprint/1\"), 32 bytes. Used only
  inside the module; never leaves `fingerprint`."
  (hmac root-secret (.getBytes "softland/read-fingerprint/1" "UTF-8")))

(defn- pair-ok? [p]
  (and (vector? p) (= 2 (count p)) (fid-ok? (nth p 0)) (int? (nth p 1))))

(defn fingerprint-bytes
  "The bytes the fingerprint hashes (FR11, first-record): the UTF-8 of
  \"softland.read-fp/1\\n\" and the canonical text of the SET of [fid stamp]
  pairs, so equal sets give equal bytes whatever index served them. Ids and
  stamps only; nil unless every element is such a pair."
  [pairs]
  (try
    (when (and (coll? pairs) (every? pair-ok? pairs))
      (.getBytes (str "softland.read-fp/1\n"
                      (env/canonical (into #{} (map (fn [[f s]] [(norm-fid f) (long s)])) pairs)))
                 "UTF-8"))
    (catch Throwable _ nil)))

(defn fingerprint
  "The keyed fingerprint of what a pattern read matched (FR11, FR12): HMAC-
  SHA256 under the fingerprint secret over `fingerprint-bytes`, as lowercase
  hex. It takes [fid stamp] pairs only, never a value. One swappable
  function. Nil for anything that is not a collection of such pairs."
  [pairs]
  (when-let [bs (fingerprint-bytes pairs)]
    (hex (hmac fp-secret bs))))

;; --------------------------------------------------------------- answers

(defn point-answer
  "A point read's answer: the rows in the fact ids' order; `:matched` the
  [fid stamp] of the rows that were there."
  [layer m rows]
  {:layer layer :moment {:stamp m} :kind :point :pattern nil
   :rows rows
   :matched (into [] (comp (remove :absent) (map (fn [r] [(:fid r) (:stamp r)]))) rows)})

(defn pattern-answer
  "A pattern read's answer (step 7): the rows in address order, `:matched`
  their [fid stamp] pairs, the mark, and the fingerprint over the set of
  matched pairs (F3: nothing of an entry past the limit is here)."
  [layer m pp rows more?]
  (try
    (let [matched (mapv (fn [r] [(:fid r) (:stamp r)]) rows)]
      {:layer layer :moment {:stamp m} :kind :pattern :pattern (:pattern pp)
       :rows rows
       :matched matched
       :mark (if more? :partial :complete)
       :fingerprint (fingerprint matched)
       :fp-secret fp-secret-id})
    (catch Throwable _ {:refused :read-error})))

;; ------------------------------------------------------------ read entry

(defn entry-entity
  "The read entry's one entity, fresh per entry, from its act's name (FR5)."
  [nm]
  (keyword (str "read-" (nth nm 3))))

(defn- shown-kind [r]
  (cond (:absent r) :absent
        (contains? r :erased-at) :erased
        (contains? r :unreadable) :unreadable
        :else :value))

(defn exact?
  "Whether a reader's pattern line carries the exact list (ruling 3): a
  person's or a model's always; a tool's only when it asks for rows."
  [reader-kind rows?]
  (or (contains? #{:person :model} reader-kind) (true? rows?)))

(defn entry-facts
  "The read entry's facts (FR5 to FR8, first-record), for the answer a query
  gave and the exit's call `spec` (with `:entry-name`, the entry act's
  name): a point read gives one `:read/point` fact per fact id read, naming
  how it came out and never the value; a pattern read gives one
  `:read/pattern` line with pattern, moment, role, mark, count, fingerprint
  and the secret's id, and the exact list of [fid stamp] for a person or a
  model, or a tool that asks for rows. Empty pattern reads give their line
  too. Total: nil for anything else."
  [answer {:keys [entry-name role reader-kind rows?]}]
  (try
    (let [ent (entry-entity entry-name)
          base {:layer (:layer answer) :moment (:moment answer) :role (or role :shown)}]
      (case (:kind answer)
        :point (vec (for [r (:rows answer)]
                      {:e ent :k :read/point
                       :v (assoc base :fid (:fid r) :stamp (:stamp r) :shown (shown-kind r))}))
        :pattern [{:e ent :k :read/pattern
                   :v (cond-> (assoc base :pattern (:pattern answer) :mark (:mark answer)
                                     :count (count (:matched answer))
                                     :fingerprint (:fingerprint answer) :fp-secret (:fp-secret answer))
                        (exact? reader-kind rows?) (assoc :exact (vec (:matched answer))))}]))
    (catch Throwable _ nil)))

(defn entry-moments
  "The moments of an act's read entry facts (F1, FR14), as stamps the act
  stood on: `[:v :moment :stamp]` of each `:read/point` and `:read/pattern`
  fact when it is an integer in [0, max-carried-stamp), else nothing. The
  gate's stamp rule counts them, so an entry is stamped after the moment it
  records, and so after everything it names. A hand-written line pushes a
  task's clock no further than a carried stood-on stamp can (R16). Pure and
  total. (With phase 2's sealing at the door, it reads the value the gate
  opens at decision.)"
  [facts]
  (try
    (into [] (keep (fn [f]
                     (when (and (map? f) (contains? read-keys (:k f)) (map? (:v f)))
                       (let [s (get-in f [:v :moment :stamp])]
                         (when (and (int? s) (<= 0 s) (< s env/max-carried-stamp)) (long s))))))
          facts)
    (catch Throwable _ [])))

;; ------------------------------------------------------ index operations

(def max-put-acts 256)
(def max-put-rows "A put page stops once its acts hold this many rows." 4096)
(def max-sweep-entries 512)

(defn- bounded? [n lo hi] (and (int? n) (<= lo n hi)))

(defn index-op
  "An `*index-ops` record, total (F2, RC8): a rebuild put page `{:layer L
  :op :rebuild-put :after nm-or-nil :acts n}` (n 1 to 256), a sweep page
  `{:layer L :op :rebuild-sweep :field f :after a-or-nil :entries n}` (f one
  of the four fields, n 1 to 512), and three test-only ops: `{:layer L :op
  :purge :fid fid :forget-stamp s}` (runs `purge-writes`, standing in for
  phase 2's forget), `{:layer L :op :drop :field f :entries n}` (deletes the
  first n entries of a field) and `{:layer L :op :put :field f :address a
  :fid fid :stamp s :e e :k k}` (writes one stale id entry). Anything else is
  `{:refuse :bad-op}`."
  [raw]
  (try
    (let [{:keys [layer op after field]} (when (map? raw) raw)
          kw? env/readable-keyword?]
      (if-not (and (map? raw) (not (record? raw)) (kw? layer))
        {:refuse :bad-op}
        (case op
          :rebuild-put
          (if (and (bounded? (:acts raw) 1 max-put-acts) (or (nil? after) (env/valid-name? after)))
            {:layer layer :op op :after (some->> after (into [])) :acts (long (:acts raw))}
            {:refuse :bad-op})

          :rebuild-sweep
          (if (and (contains? all-fields field) (bounded? (:entries raw) 1 max-sweep-entries)
                   (or (nil? after)
                       (if (= :ix-of field) (fid-ok? after) (string? after))))
            {:layer layer :op op :field field :after (if (= :ix-of field) (some-> after norm-fid) after)
             :entries (long (:entries raw))}
            {:refuse :bad-op})

          :purge
          (if (and (fid-ok? (:fid raw)) (int? (:forget-stamp raw))
                   (<= 0 (:forget-stamp raw)) (< (:forget-stamp raw) env/max-carried-stamp))
            {:layer layer :op op :fid (norm-fid (:fid raw)) :forget-stamp (long (:forget-stamp raw))}
            {:refuse :bad-op})

          :drop
          (if (and (contains? all-fields field) (bounded? (:entries raw) 1 max-sweep-entries))
            {:layer layer :op op :field field :entries (long (:entries raw))}
            {:refuse :bad-op})

          :put
          (if (and (contains? index-fields field) (string? (:address raw)) (fid-ok? (:fid raw))
                   (int? (:stamp raw)) (<= 0 (:stamp raw)) (kw? (:e raw)) (kw? (:k raw)))
            {:layer layer :op op :field field :address (:address raw) :fid (norm-fid (:fid raw))
             :stamp (long (:stamp raw)) :e (:e raw) :k (:k raw)}
            {:refuse :bad-op})

          {:refuse :bad-op})))
    (catch Throwable _ {:refuse :bad-op})))

(defn op-refused [op] {:refused (:refuse op)})

(defn after-opts
  "The range options for a page after an exclusive cursor."
  [n]
  {:max-amt n :inclusive? false})

(defn put-todo [recs] (try (into [] (map (fn [e] [(key e) (val e)])) recs) (catch Throwable _ [])))

(defn put-rows-full? [nrows] (>= nrows max-put-rows))

(defn fid-of [nm i] [nm (long i)])

(defn put-act [nm stamp rows opens] {:name nm :stamp stamp :rows rows :opens opens})

(defn put-out [acts last-nm left] {:acts acts :last last-nm :left (count left)})

(defn put-ack
  "A put page's answer: where the next page starts and whether the log is
  done (the records ran out and every one read was processed)."
  [recs n out]
  {:next (:last out) :acts (count (:acts out))
   :done? (and (< (count recs) n) (zero? (:left out)))})

(defn sweep-todo
  "A sweep page's entries as `[[address value fid] ...]` (for `:ix-of` the
  address is the fact id)."
  [field ents]
  (try
    (into [] (map (fn [e] (let [a (key e) v (val e)]
                            [a v (if (= :ix-of field) a (when (map? v) (:fid v)))])))
          ents)
    (catch Throwable _ [])))

(defn sweep-entries [todo] (mapv (fn [[a v _]] [a v]) todo))

(defn name-idx
  "A fact id's name and index when it is well-formed, else [nil nil]."
  [fid]
  (if (fid-ok? fid) (norm-fid fid) [nil nil]))

(defn found-of [a rec row o ix-of] {:address a :rec rec :row row :open o :ix-of ix-of})

(defn sweep-ack
  [todo n d]
  {:next (first (peek todo)) :done? (< (count todo) n)
   :deleted (:deleted d 0) :rewritten (:rewritten d 0)})

(defn purge-op-writes
  "The test-only `:purge` op: `purge-writes` for a fact that is there (a yes
  act's row), nothing otherwise."
  [fid rec row kv-addresses forget-stamp]
  (if (and (= :yes (:answer rec)) (map? row) (int? (:stamp rec)))
    (purge-writes fid row (:stamp rec) kv-addresses forget-stamp)
    no-index-writes))

(defn purge-ack [rec row] {:purged (boolean (and (= :yes (:answer rec)) (map? row)))})

(defn drop-writes
  "The test-only `:drop` op: delete the entries of one page of a field."
  [field ents]
  (try (assoc no-index-writes :index-del (into [] (map (fn [e] [field (key e)])) ents))
       (catch Throwable _ no-index-writes)))

(defn drop-ack [d] {:dropped (count (:index-del d))})

(defn put-one-writes
  "The test-only `:put` op: one stale id entry, built from checked parts so
  it fits the entry schema."
  [op]
  (try
    (assoc no-index-writes
           :index-put [[(:field op) (:address op)
                        {:e (:e op) :k (:k op) :v nil :replaces nil :mark #{}
                         :fid (:fid op) :stamp (:stamp op)}]])
    (catch Throwable _ no-index-writes)))

;; ------------------------------------------------------- install: depot

(defn declare-depots!
  "The operator depot for index maintenance (W4): placed by layer like the
  layer's offers, so a page runs on the layer's home task. Not an act and
  not in the log (a rebuild changes no meaning; re-encoding is allowed)."
  [setup]
  (declare-depot setup *index-ops (hash-by :layer)))

;; ------------------------------------------------- install: the ops source

(defn declare-index-ops-source!
  "The gate topology's second source (F2), on `*index-ops`: one bounded
  event per record, on the layer's home. A put page reads up to n answer
  records after its cursor and each yes act's rows (stopping once 4,096 rows
  are gathered), opens every row, and puts every entry they imply; a sweep
  page reads up to n entries of one field after its cursor, each entry's
  record, row and open (and for `:ix-kv` its `:ix-of` set), and deletes or
  rewrites what the log does not imply; the test-only ops purge, drop and put.
  Writes through the same three blocks as the gate's decision. Idempotent:
  every write is a set or a delete at a computed address. Total: a record it
  does not know is answered `{:refused :bad-op}` and nothing else happens.

  A page never yields (no `yield-if-overtime`, no `:allow-yield?`): it reads
  the log and writes what that read implies in one atomic event, so no
  forget can land between its reads and its writes and have a purged value
  written back. Its size bounds how long it holds the task (a divergence
  from the plan's yielding sweep loop, BUILD_NOTES-read-exit.md)."
  [s]
  (<<sources s
    (source> *index-ops {:retry-mode :all-after} :> *raw)
    (index-op *raw :> *op)
    (<<if (contains? *op :refuse)
      (ack-return> (op-refused *op))
     (else>)
      (get *op :layer :> *layer)
      (get *op :op :> *kind)
      (current-hints :> *hints)
      (<<cond
        (case> (= *kind :rebuild-put))
        ;; the task's clock is "now" for the open of every row
        (local-select> STAY $$clock :> *clock)
        (get *op :after :> *after)
        (get *op :acts :> *n)
        (after-opts *n :> *opts)
        (<<if (nil? *after)
          (local-select> [(keypath *layer :answers) (sorted-map-range-from-start *n)] $$layers :> *recs)
         (else>)
          (local-select> [(keypath *layer :answers) (sorted-map-range-from *after *opts)] $$layers :> *recs))
        (put-todo *recs :> *todo0)
        (loop<- [*pt *todo0 *acts [] *nrows 0 *last nil :> *pout]
          (<<if (or> (empty? *pt) (put-rows-full? *nrows))
            (:> (put-out *acts *last *pt))
           (else>)
            (first *pt :> [*pnm *prec])
            (<<if (= :yes (get *prec :answer))
              (local-select> [(keypath *layer :log *pnm) (subselect ALL)] $$layers :> *prows)
              (get *prec :stamp :> *pstamp)
              (loop<- [*ri 0 *ropens [] :> *opens]
                (<<if (>= *ri (count *prows))
                  (:> *ropens)
                 (else>)
                  (nth *prows *ri :> *rrow)
                  (fid-of *pnm *ri :> *rfid)
                  (open-row> *layer *rfid *rrow *pstamp *clock :> *ro)
                  (continue> (inc *ri) (conj *ropens *ro))))
              (put-act *pnm *pstamp *prows *opens :> *pact)
              (continue> (rest *pt) (conj *acts *pact) (+ *nrows (count *prows)) *pnm)
             (else>)
              (continue> (rest *pt) *acts *nrows *pnm))))
        (get *pout :acts :> *pacts)
        (put-page-writes *hints *layer *pacts :> *d)
        (put-ack *recs *n *pout :> *ack)

        (case> (= *kind :rebuild-sweep))
        (local-select> STAY $$clock :> *sclock)
        (get *op :field :> *field)
        (get *op :after :> *safter)
        (get *op :entries :> *sn)
        (after-opts *sn :> *sopts)
        (<<if (nil? *safter)
          (local-select> [(keypath *layer *field) (sorted-map-range-from-start *sn)] $$layers :> *ents)
         (else>)
          (local-select> [(keypath *layer *field) (sorted-map-range-from *safter *sopts)] $$layers :> *ents))
        (sweep-todo *field *ents :> *stodo)
        (loop<- [*sw *stodo *found [] :> *founds]
          (<<if (empty? *sw)
            (:> *found)
           (else>)
            (first *sw :> [*sa *sv *sfid])
            (name-idx *sfid :> [*snm *sidx])
            (<<if (nil? *snm)
              (continue> (rest *sw) (conj *found (found-of *sa nil nil nil nil)))
             (else>)
              (local-select> [(keypath *layer :answers *snm)] $$layers :> *srec)
              (local-select> [(keypath *layer :log *snm *sidx)] $$layers :> *srow)
              (open-row> *layer *sfid *srow (get *srec :stamp) *sclock :> *so)
              (<<if (= *field :ix-kv)
                (local-select> [(keypath *layer :ix-of *sfid)] $$layers :> *sof)
               (else>)
                (identity nil :> *sof))
              (continue> (rest *sw) (conj *found (found-of *sa *srec *srow *so *sof))))))
        (sweep-entries *stodo :> *sents)
        (sweep-page-writes *hints *layer *field *sents *founds :> *d)
        (sweep-ack *stodo *sn *d :> *ack)

        (case> (= *kind :purge))
        (get *op :fid :> *ufid)
        (first *ufid :> *unm)
        (second *ufid :> *uidx)
        (local-select> [(keypath *layer :answers *unm)] $$layers :> *urec)
        (local-select> [(keypath *layer :log *unm *uidx)] $$layers :> *urow)
        (local-select> [(keypath *layer :ix-of *ufid)] $$layers :> *ukv)
        (get *op :forget-stamp :> *uforget)
        (purge-op-writes *ufid *urec *urow *ukv *uforget :> *d)
        (purge-ack *urec *urow :> *ack)

        (case> (= *kind :drop))
        (get *op :field :> *dfield)
        (get *op :entries :> *dn)
        (local-select> [(keypath *layer *dfield) (sorted-map-range-from-start *dn)] $$layers :> *dents)
        (drop-writes *dfield *dents :> *d)
        (drop-ack *d :> *ack)

        (default>)
        (put-one-writes *op :> *d)
        (identity {:put true} :> *ack))
      ;; the same three write blocks as the gate's decision (module.clj)
      (<<atomic
        (ops/explode (get *d :index-put) :> [*ix *ia *ie])
        (local-transform> [(keypath *layer *ix *ia) (termval *ie)] $$layers))
      (<<atomic
        (ops/explode (get *d :index-of) :> [*ofid *ias])
        (local-transform> [(keypath *layer :ix-of *ofid) (termval *ias)] $$layers))
      (<<atomic
        (ops/explode (get *d :index-del) :> [*dx *da])
        (local-transform> [(keypath *layer *dx *da) NONE>] $$layers))
      (ack-return> *ack))))

;; ------------------------------------------------------ install: queries

(defn declare-queries!
  "The two read queries (RE1, RE2), each on the layer's home task only: the
  leading `(|hash *layer)` is evaluated client-side, so the query goes
  straight to the task that holds the layer; one emit, no aggregator. Every
  step is total: a query topology exception is fatal to the worker."
  [topologies]
  (<<query-topology topologies "read-point" [*layer *for *fids *as-of :> *answer]
    (|hash *layer)
    (parse-point *for *fids *as-of :> *p)
    (<<if (contains? *p :refused)
      (identity *p :> *answer)
     (else>)
      (local-select> [(keypath *layer :settings)] $$layers :> *settings)
      (<<cond
        (case> (not (visible? *settings *for)))
        (identity {:refused :not-visible} :> *answer)

        (case> (not (placed-by-layer? *settings)))
        (identity {:refused :re-classed} :> *answer)

        (default>)
        (local-select> STAY $$clock :> *clock)
        (get *p :as-of :> *pas-of)
        (moment *pas-of *clock :> *m)
        (get *p :fids :> *pfids)
        (loop<- [*todo *pfids *acc [] :> *rows]
          (yield-if-overtime)
          (<<if (empty? *todo)
            (:> *acc)
           (else>)
            (first *todo :> *fid)
            (first *fid :> *nm)
            (second *fid :> *idx)
            (local-select> [(keypath *layer :answers *nm)] $$layers :> *rec)
            (<<if (yes-by? *rec *m)
              (local-select> [(keypath *layer :log *nm *idx)] $$layers :> *row)
              (get *rec :stamp :> *s)
              (<<if (nil? *row)
                (absent-row *fid :> *r)
               (else>)
                (open-row> *layer *fid *row *s *m :> *o)
                (point-row *fid *s *row *o :> *r))
             (else>)
              (absent-row *fid :> *r))
            (continue> (rest *todo) (conj *acc *r))))
        (point-answer *layer *m *rows :> *answer)))
    (|origin))

  (<<query-topology topologies "read-pattern" [*layer *for *pattern *as-of *limit :> *answer]
    (|hash *layer)
    (parse-pattern *pattern *limit *as-of :> *pp)
    (<<if (contains? *pp :refused)
      (identity *pp :> *answer)
     (else>)
      (local-select> [(keypath *layer :settings)] $$layers :> *settings)
      (<<cond
        (case> (not (visible? *settings *for)))
        (identity {:refused :not-visible} :> *answer)

        (case> (not (placed-by-layer? *settings)))
        (identity {:refused :re-classed} :> *answer)

        (default>)
        (local-select> STAY $$clock :> *clock)
        (get *pp :as-of :> *pas-of)
        (moment *pas-of *clock :> *m)
        (get *pp :ix :> *pix)
        (<<if (= :latest (get *pp :kind))
          ;; one tail read below the bound: the latest fact of (e, k) at or before m
          (bounds *pp *m :> [*tfrom *tend])
          (local-select> [(keypath *layer :ix-ek) (sorted-map-range-to *tend {:max-amt 1})] $$layers :> *tsub)
          (tail-state *pp *tsub *m :> *pfinal)
         (else>)
          ;; doubling pages over one index, each read yielding, until the range's
          ;; end, limit + 1 matches, or the scan budget
          (page-init *pp *m :> *st0)
          (loop<- [*pst *st0 :> *pfinal]
            (yield-if-overtime)
            (get *pst :from :> *pfrom)
            (get *pst :page :> *ppage)
            (local-select> [(keypath *layer *pix) (sorted-map-range-from *pfrom *ppage)] $$layers
                           {:allow-yield? true} :> *psub)
            (page-entries *psub :> *pents)
            (kv-candidates *pst *pents :> *pcands)
            (kv-need *pst :> *pneed)
            ;; [F5] a [:kv] candidate counts only when its value opens to the pattern's
            (loop<- [*kt *pcands *kn *pneed *kg {} :> *popened]
              (yield-if-overtime)
              (<<if (or> (empty? *kt) (<= *kn 0))
                (:> *kg)
               (else>)
                (first *kt :> [*ka *ke])
                (get *ke :fid :> *kfid)
                (get *ke :stamp :> *kstamp)
                (open-row> *layer *kfid *ke *kstamp *m :> *ko)
                (kv-hit? *pst *ko :> *khit)
                (need-after *kn *khit :> *kn2)
                (continue> (rest *kt) *kn2 (assoc *kg *ka *ko))))
            (page-step *pst *pents *popened :> *pst2)
            (<<if (get *pst2 :done?)
              (:> *pst2)
             (else>)
              (continue> *pst2))))
        (get *pfinal :kept :> *kept)
        (get *pfinal :more? :> *more?)
        ;; step 6: show each kept entry, opening it through the one seam
        (loop<- [*vt *kept *vacc [] :> *vrows]
          (yield-if-overtime)
          (<<if (empty? *vt)
            (:> *vacc)
           (else>)
            (first *vt :> *vk)
            (show-how *vk :> *how)
            (get *vk :entry :> *ve)
            (get *ve :fid :> *vfid)
            (get *ve :stamp :> *vstamp)
            (<<cond
              (case> (= *how :read-row))
              (kept-name-idx *vk :> [*vnm *vidx])
              (local-select> [(keypath *layer :log *vnm *vidx)] $$layers :> *vrow)
              (open-row> *layer *vfid *vrow *vstamp *m :> *vo)
              (shown-row *vk *vo :> *vr)

              (case> (= *how :open))
              (open-row> *layer *vfid *ve *vstamp *m :> *vo2)
              (shown-row *vk *vo2 :> *vr)

              (default>)
              (shown-row *vk nil :> *vr))
            (continue> (rest *vt) (conj *vacc *vr))))
        (pattern-answer *layer *m *pp *vrows *more? :> *answer)))
    (|origin)))
