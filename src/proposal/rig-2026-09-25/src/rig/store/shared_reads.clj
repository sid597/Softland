;; IMPORTANT: Before modifying this file, re-read PLAN-reads-rest.md (its fixes
;; F1 to F18 are marked in place), PLAN-read-exit.md and
;; BUILD_NOTES-reads-rest.md, and adhere to their decisions.
;; Step 6b: PLAN-grammar-micro.md supplies effective rows and pre-batch hints.
(ns rig.store.shared-reads
  "Stage 5b, the rest of phase 5, module side (PLAN-reads-rest.md): the read
  of a shared layer through the one exit as of a settled frontier; the
  micro store's indexes (placed on the layer's task, written in the batch
  that decides the act, purged by value id in the forget's batch, rebuilt
  from the log in pages); a standing read's delta and its running closing
  fingerprint; the maintenance queries a close and a drop use.

  The read exit's two queries send a layer of the shared kind here with one
  `invoke-query` on the same task (reads.clj requires nothing of this
  namespace, which requires it). The micro topology calls this
  namespace's ops for its block 2d, its forget purges and its
  `*micro-index-ops` pages; only that topology writes `$$micro`.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key; the
  sorted String an index entry sits under is its address.

  Everything that runs in a topology is total: a refusal is data, never an
  exception, which is fatal to the worker (SPEC 'What Rama showed' 3; the
  read exit's probe for query topologies)."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.aggs :as aggs]
            [com.rpl.rama.ops :as ops]
            [rig.store.envelope :as env]
            [rig.store.inject :as inject]
            [rig.store.locks :as locks]
            [rig.store.reads :as reads]))

;; ============================================================ addresses

(def sep reads/sep)

(defn time-text
  "An entry's time part in a shared address: its batch, then its stamp, each
  as 16 hex digits (RR3: batch first, so 'as of F' is an end bound)."
  [batch stamp]
  (str (reads/hex16 batch) sep (reads/hex16 stamp)))

(defn address
  "A shared index entry's address (PLAN-reads-rest.md, 'The fields'), parts
  joined by U+0000: `:ix-ek` e, k, batch, stamp, fact id; `:ix-ke` k, e,
  batch, stamp, fact id; `:ix-kv` k, the value's keyed digest (F12), batch,
  stamp, fact id; `:ix-s` batch, stamp, fact id."
  [field {:keys [e k digest batch stamp fid]}]
  (let [t (str (time-text batch stamp) sep (reads/fid-text fid))]
    (case field
      :ix-ek (str (reads/kw-text e) sep (reads/kw-text k) sep t)
      :ix-ke (str (reads/kw-text k) sep (reads/kw-text e) sep t)
      :ix-kv (str (reads/kw-text k) sep digest sep t)
      :ix-s t)))

(defn kv-prefix
  "The shared `[:kv k v]` prefix: k, then the keyed digest of v's canonical
  text (F12)."
  [k v]
  (when-let [d (reads/kv-digest (env/encode-value v))]
    (str (reads/kw-text k) sep d sep)))

;; ============================================================ the fields

(defn index-entry
  "A shared index entry's schema: the micro row whole (F6's rule) plus its
  entity, fact id, stamp, the batch that decided it, a purge's date and the
  no-copy flag."
  [row-fields]
  (fixed-keys-schema
   (merge row-fields
          {:e         clojure.lang.Keyword
           :fid       clojure.lang.PersistentVector
           :stamp     Long
           :batch     Long
           :erased-at Long
           :copy      Boolean})))

(defn layer-fields
  "The fields this stage adds to the value `$$micro` keeps under a layer id
  (PLAN-reads-rest.md, 'The fields'), every map subindexed with size
  tracking off. `:ix-place` is Option D's bucket count (F5), maintenance
  state, never a fact: absent means 1, Option B, the only placement built
  tonight (RR5), so nothing writes it yet and a read takes its absence as 1."
  [row-fields]
  (let [entry (index-entry row-fields)
        sub {:subindex-options {:track-size? false}}]
    {:ix-ek    (map-schema String entry sub)
     :ix-ke    (map-schema String entry sub)
     :ix-kv    (map-schema String entry sub)
     :ix-s     (map-schema String entry sub)
     :ix-of    (map-schema clojure.lang.PersistentVector (set-schema String) sub)
     :ix-id    (map-schema clojure.lang.PersistentVector String sub)
     :ix-error Long
     :ix-place Long}))

(def task-fields
  "The fields this stage adds to `$$micro-task`: the shared layers whose
  home is this task (block 2d adds each layer it writes for), and the last
  index page's progress, which the operator's loop reads (a microbatch
  cannot answer through an ack)."
  {:layers  (set-schema clojure.lang.Keyword {:subindex? true})
   :rebuild (fixed-keys-schema {:req    String
                                :op     clojure.lang.Keyword
                                :cursor clojure.lang.PersistentVector
                                :done?  Boolean
                                :batch  Long
                                :count  Long})})

(def id-fields "The shared id indexes, whose entries a purge tombstones." [:ix-ek :ix-ke :ix-s])
(def sweep-fields "The fields a shared rebuild sweeps, in order." [:ix-ek :ix-ke :ix-kv :ix-s :ix-of :ix-id])

(deframaop key-rows-of>
  "Effective grammar rows on hash(L): micro first, stream on a miss.
  A present row with no grammar is a hit. Reads only the distinct keys
  requested; the caller owns the layer partition before calling."
  [*L *ks]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")
                         $$layers (this-module-pobject-task-global "$$layers")]
    (loop<- [*todo (distinct *ks) *acc {} :> *out]
      (yield-if-overtime)
      (<<if (empty? *todo)
        (:> *acc)
       (else>)
        (first *todo :> *k)
        (local-select> [(keypath *L :key-rows *k)] $$micro :> *mr)
        (<<if (some? *mr)
          (identity *mr :> *row)
         (else>)
          (local-select> [(keypath *L :key-rows *k)] $$layers :> *row))
        (continue> (rest *todo) (assoc *acc *k *row))))
    (:> *out)))

;; ============================================================ entries and writes

(def no-writes {:index-put [] :index-of [] :index-del [] :index-id []})

(defn merge-writes [& ws]
  {:index-put (into [] (mapcat :index-put) ws)
   :index-of (into [] (mapcat :index-of) ws)
   :index-del (into [] (mapcat :index-del) ws)
   :index-id (into [] (mapcat :index-id) ws)})

(defn entry-of
  "A shared index entry: the micro row, its entity, fact id, stamp and batch,
  and its lock record wherever the row keeps it (the row's `:lock`, else a
  copy of its lock row, `lock-row`), so the entry opens on the layer's task
  alone."
  [row e fid stamp batch lock-row]
  (cond-> (assoc row :e e :fid fid :stamp (long stamp) :batch (long batch))
    (and (nil? (:lock row)) (map? lock-row)) (assoc :lock lock-row)))

(defn tombstone
  "A purged fact's shared id-index entry: the entry without its value
  fields and no-copy flag, with the erasure's date. The same whoever writes
  it, purge or rebuild."
  [entry date]
  (-> (apply dissoc entry (conj reads/value-fields :copy))
      (assoc :erased-at (long date))))

(defn- no-copy [entry] (assoc (apply dissoc entry reads/value-fields) :copy false))

(defn fact-writes
  "One fact's shared index writes. `digest` is its value's keyed digest when
  its key is indexed by value and the value opened (nil otherwise);
  `erased-at` a date when it no longer opens, which gives tombstones and no
  value entry."
  [hints entry digest erased-at]
  (let [{:keys [e k fid]} entry
        parts (select-keys entry [:e :k :batch :stamp :fid])
        ek (address :ix-ek parts)
        ke (address :ix-ke parts)
        ss (address :ix-s parts)]
    (if (some? erased-at)
      (let [t (tombstone entry erased-at)]
        {:index-put [[:ix-ek ek t] [:ix-ke ke t] [:ix-s ss t]] :index-of [] :index-del []
         :index-id [[fid ek]]})
      (let [id-entry (if (reads/copy? hints k) entry (no-copy entry))
            kv (when (and (string? digest) (reads/by-value? hints k))
                 (address :ix-kv (assoc parts :digest digest)))]
        {:index-put (cond-> [[:ix-ek ek id-entry] [:ix-ke ke id-entry] [:ix-s ss id-entry]]
                      kv (conj [:ix-kv kv entry]))
         :index-of (if kv [[fid #{kv}]] [])
         :index-del []
         :index-id [[fid ek]]}))))

(defn kv-digests
  "Block 2b's keyed digests on the arrival task (F12), pure: for each fact
  whose key is indexed by value, the digest of its value's canonical text,
  from the plaintext the arrival task opened (`plain`, fact index ->
  bytes) or, for a control fact, its own value. {index hex}. The text never
  leaves this task; the digest travels with the sealed bytes. Total."
  ([offer plain] (kv-digests (reads/current-hints) offer plain))
  ([hints offer plain]
  (try
    (into {}
          (keep-indexed
           (fn [i f]
             (when (reads/by-value? hints (:k f))
               (let [text (if (contains? f :sealed)
                            (some-> (get plain i) (String. "UTF-8"))
                            (env/encode-value (:v f)))]
                 (when-let [d (reads/kv-digest text)] [(long i) d])))))
          (:facts offer))
    (catch Throwable _ {}))))

(defonce ^{:doc "Test only (R3): a set of act names whose block 2d index writes
  fail as if the pure step had thrown (RT13, F9). A kept store has no such
  hook."}
  fail-index
  (atom #{}))

(defn index-writes
  "Block 2d's writes for an act decided yes in this batch (WS3), pure and
  total: per fact of `frows` (block 2b's `[e i row lock-row]`), its entries
  at their three id addresses, and for a value-indexed fact its `:ix-kv`
  entry and `:ix-of` set, and its `:ix-id` address. Every write a set at a
  computed address, so a retried batch writes the same. A throwable gives
  no writes and `:index-error`, which the batch records (RR6, F9), never a
  throw."
  [hints nm frows kvd stamp batch]
  (try
    (when (contains? @fail-index nm) (throw (ex-info "forced index error" {})))
    (apply merge-writes no-writes
           (for [[e i row lock-row] frows]
             (fact-writes hints (entry-of row e [(into [] nm) (long i)] stamp batch lock-row)
                          (get kvd (long i)) nil)))
    (catch Throwable _ (assoc no-writes :index-error true))))

(defn purge-writes
  "Purge one fact from a shared layer's indexes by its value id (WS4), the
  micro twin of `reads/purge-writes`: tombstones at its three id addresses,
  computed from the entry (no value needed), dated `date`; the deletes of
  every `:ix-kv` address in its `:ix-of` set and of the set; `:ix-id` kept
  (ids only). Pure and total."
  [entry kv-addresses date]
  (try
    (if (and (map? entry) (int? date))
      (let [parts (select-keys entry [:e :k :batch :stamp :fid])
            t (tombstone entry date)]
        {:index-put (mapv (fn [f] [f (address f parts) t]) id-fields)
         :index-of []
         :index-del (conj (mapv (fn [a] [:ix-kv a]) (sort (filter string? kv-addresses)))
                          [:ix-of (:fid entry)])
         :index-id []})
      no-writes)
    (catch Throwable _ no-writes)))

(defn implied-writes
  "What a shared rebuild writes for one row, from its open result (the
  invariant: a purge writes exactly what a rebuild would): tombstones for a
  row that no longer opens, dated as the open gives it (the ledger's date,
  else the wrap's close), else the entries, with the value entry only when
  the value opened (its digest taken here, where the plaintext is)."
  [hints entry o]
  (try
    (let [erased (when (map? o) (:erased-at o))
          text (when (and (map? o) (contains? o :value) (some? (:value o)))
                 (env/encode-value (:value o)))
          digest (when (and text (reads/by-value? hints (:k entry))) (reads/kv-digest text))]
      (fact-writes hints entry digest erased))
    (catch Throwable _ no-writes)))

(defn implied-map
  "`implied-writes` as `{[field address] entry}` plus `:ix-of` (the set) and
  `:ix-id` (the address), for a sweep's judgement of one fact."
  [w]
  (assoc (into {} (map (fn [[f a e]] [[f a] e])) (:index-put w))
         :ix-of (second (first (:index-of w)))
         :ix-id (second (first (:index-id w)))))

(defn sweep-writes
  "One swept entry's writes on the layer's task, given what the log implies
  for its fact now (`imp`, `implied-map`'s shape, empty when the log implies
  nothing): for an id or value field, nothing when the stored entry is the
  implied one, a rewrite when the address is implied with other content,
  else a delete (and for `:ix-kv` the fact's `:ix-of` set rewritten to the
  implied one); for `:ix-of` and `:ix-id` (addressed by fact id) a rewrite
  to the implied value or a delete. Pure and total."
  [field a v imp]
  (try
    (case field
      :ix-of (let [want (:ix-of imp)]
               (cond (empty? want) (assoc no-writes :index-del [[:ix-of a]])
                     (reads/same-entry? want v) no-writes
                     :else (assoc no-writes :index-of [[a want]])))
      :ix-id (let [want (:ix-id imp)]
               (cond (nil? want) (assoc no-writes :index-del [[:ix-id a]])
                     (= want v) no-writes
                     :else (assoc no-writes :index-id [[a want]])))
      (let [want (get imp [field a])]
        (cond
          (and (some? want) (reads/same-entry? want v)) no-writes
          (some? want) (assoc no-writes :index-put [[field a want]])
          (= :ix-kv field) (cond-> (assoc no-writes :index-del [[:ix-kv a]])
                             (some? (:fid v)) (assoc :index-of (if (seq (:ix-of imp)) [[(:fid v) (:ix-of imp)]] [])
                                                     :index-del (cond-> [[:ix-kv a]]
                                                                  (empty? (:ix-of imp)) (conj [:ix-of (:fid v)]))))
          :else (assoc no-writes :index-del [[field a]]))))
    (catch Throwable _ no-writes)))

;; ============================================================ the moment and visibility

(defn moment
  "A shared read's moment (FRR8, first-record): `{:F f}` with f =
  min(asked, the frontier on the task the read runs on), the task's
  frontier when nothing is asked; `{:refused :moment-kind}` for a stamp
  moment (RR1: for a shared layer 'as of T' is F). Total."
  [as-of Ft]
  (try
    (let [Ft (long (or Ft -1))]
      (cond
        (nil? as-of) {:F Ft}
        (reads/frontier-moment? as-of) {:F (min (long (:frontier as-of)) Ft)}
        :else {:refused :moment-kind}))
    (catch Throwable _ {:refused :moment-kind})))

(defn visible?
  "Ruling 9's default for a shared read, decided for the person the read is
  for (F12): a re-classed layer by its stream settings (the read exit's
  rule: the base to any actor, a person's layers to their owner); a group
  by membership as of F (the batch that named the member is at or below F;
  RR2's reading as of F, which `:members`' batch makes possible); a layer
  with neither settings answers as a private one (F4). Total."
  [ssettings msettings member-batch F for]
  (try
    (boolean
     (and (env/readable-keyword? for)
          (cond
            (map? ssettings) (reads/visible? ssettings for)
            (map? msettings) (case (:kind msettings)
                               :group (and (int? member-batch) (<= member-batch F))
                               :base true
                               (reads/visible? msettings for))
            :else false)))
    (catch Throwable _ false)))

;; ============================================================ patterns on the micro store

(defn micro-pp
  "A parsed pattern (reads/parse-pattern's) as the micro store reads it:
  visibility by `:batch`, and the `[:kv]` prefix by the value's keyed
  digest (F12). The other prefixes are the read exit's (a shared address
  shares their leading parts). `{:refused r}` when a digest cannot be
  taken."
  [pp]
  (try
    (if (= :kv (:kind pp))
      (if-let [p (kv-prefix (second (:pattern pp)) (:v pp))]
        (assoc pp :prefix p :by :batch)
        {:refused :bad-pattern})
      (assoc pp :by :batch))
    (catch Throwable _ {:refused :bad-pattern})))

(defn merge-key
  "The order both eras of a re-classed layer share (F1): by the pattern's
  parts, then stamp, then fact id, from the entries' fields, never their
  addresses (a stream-era address has a stamp where a micro-era one has a
  batch)."
  [kind entry]
  (let [e (reads/kw-text (:e entry)) k (reads/kw-text (:k entry))
        s (:stamp entry) f (reads/fid-text (:fid entry))]
    (case kind
      :e [k s f]
      :k [e s f]
      :all [e k s f]
      [s f])))

(defn merge-eras
  "A re-classed layer's two eras merged (step 6, F1): each list already in
  the pattern's order and cut at the limit; the union's first `limit` in
  that order, partial when either era was or the union is longer. `[:kv]`
  lists the stream era, then the micro era (FRR8); `[:latest]` keeps the
  head with the larger stamp (P13's head rule across both eras). Pure."
  [kind limit stream micro]
  (try
    (case kind
      :latest (let [all (concat (:kept stream) (:kept micro))]
                {:kept (if (seq all) [(apply max-key #(get-in % [:entry :stamp]) all)] [])
                 :more? false})
      :kv (let [all (vec (concat (:kept stream) (:kept micro)))]
            {:kept (vec (take limit all))
             :more? (boolean (or (:more? stream) (:more? micro) (< limit (count all))))})
      (let [all (sort-by #(merge-key kind (:entry %)) compare (concat (:kept stream) (:kept micro)))]
        {:kept (vec (take limit all))
         :more? (boolean (or (:more? stream) (:more? micro) (< limit (count all))))}))
    (catch Throwable _ {:kept [] :more? true})))

(defn with-batch
  "A shown row of a shared layer carries its entry's batch (FRR8)."
  [row entry]
  (if (and (map? row) (int? (:batch entry))) (assoc row :batch (:batch entry)) row))

(defn max-stamp
  "The largest stamp among an answer's rows (0 when none): what the entry
  must stand on (F1 extended, FRR8)."
  [rows]
  (reduce max 0 (keep #(let [s (:stamp %)] (when (int? s) s)) rows)))

(defn shared-answer
  "A shared read's answer: the read exit's answer with the frontier moment,
  `:max-stamp`, and, while the layer's index has a gap (F9), the mark
  `:partial`."
  [answer F ix-error]
  (try
    (cond-> (assoc answer :moment {:frontier F} :max-stamp (max-stamp (:rows answer)))
      (and (some? ix-error) (= :pattern (:kind answer))) (assoc :mark :partial))
    (catch Throwable _ {:refused :read-error})))

;; ============================================================ deltas (standing reads)

(defn delta-index
  "Which index a standing read's delta reads (PLAN-reads-rest.md, 'The
  delta'): `[:ek]`, `[:kv]` and `[:latest]` their own (time-ordered inside
  their prefix); `[:all]`, `[:e]`, `[:k]` the time-ordered `:ix-s`, filtered
  by the pattern."
  [pp]
  (if (#{:all :e :k} (:kind pp)) :ix-s (:ix pp)))

(defn delta-prefix [pp] (if (#{:all :e :k} (:kind pp)) "" (:prefix pp)))

(defn after-bound
  "Where the next delta starts once a delta at moment m is delivered whole:
  just above m in its index (a stamp, or a batch for a shared layer)."
  [pp m]
  (str (delta-prefix pp) (reads/hex16 (inc (long m)))))

(defn delta-state
  "The page loop's state for a delta: the read exit's `page-init` over the
  delta's index, from the handle's cursor (`from`, an address in that
  index) to the moment's bound, with the pattern as its filter over
  `:ix-s`."
  [pp m from]
  (let [st (reads/page-init pp m)]
    (if (#{:all :e :k} (:kind pp))
      (assoc st :ix :ix-s :from from :end (reads/hex16 (inc (long m))) :match (:pattern pp))
      (assoc st :from from))))

(defn next-scan
  "The handle's next cursor after a delta (RR10, F2), from the delta's final
  loop state: past the moment when it was not cut; after the last shown
  row's address when the limit cut it (the limit + 1st match comes next);
  when the scan budget cut it, past the last address it scanned, sealed
  (`reads/seal-cursor`), so the client holds nothing of an unshown fact.
  The plan's time part (F2) could not move past one act larger than the
  budget (every fact of an act shares its stamp): found by RT9 at the
  build. Pure but for the seal's nonce."
  [pp m st]
  (try
    (case (:cut st)
      :limit {:from (str (:address (peek (:kept st))) sep)}
      :budget (if-let [t (some-> (:last st) (str sep) reads/seal-cursor)]
                {:token t}
                {:from (after-bound pp m)})
      {:from (after-bound pp m)})
    (catch Throwable _ {:from (after-bound pp m)})))

(defn scan-from
  "Where a delta starts: the cursor's address, or the address its sealed
  token holds; nil when it holds none."
  [scan]
  (when (map? scan) (or (:from scan) (reads/open-cursor (:token scan)))))

(defn latest-new?
  "Whether a `[:latest]` delta's head is new: its address at or above the
  handle's cursor (its time is past the last delivered moment)."
  [kept from]
  (boolean (and (seq kept) (string? from) (<= 0 (compare (:address (first kept)) from)))))

(defn so-far
  "A delivery line's running closing value (F3): the previous line's, with
  this line's moment and fingerprint chained in, the count raised, the
  mark carried. Unchanged for a delivery of nothing."
  [prev moment fp mark delivered?]
  (if-not delivered?
    prev
    {:fp (reads/standing-link (:fp prev) moment fp)
     :n (inc (long (or (:n prev) 0)))
     :partial? (boolean (or (:partial? prev) (= :partial mark)))}))

(defn delta-answer
  "A delta's answer (RS3): the read exit's pattern answer over what it shows,
  or `:nothing-new`; the running closing value through this delivery; the
  handle's next cursor. For a shared layer the moment is `{:frontier F}`
  with `:max-stamp`."
  [layer m-kind m pp rows more? prev nxt]
  (try
    (let [a (reads/pattern-answer layer m pp rows more?)
          moment (if (= :frontier m-kind) {:frontier m} {:stamp m})
          a (cond-> (assoc a :moment moment) (= :frontier m-kind) (assoc :max-stamp (max-stamp rows)))
          delivered? (seq rows)]
      (assoc a
             :nothing-new (not delivered?)
             :so-far (so-far prev moment (:fingerprint a) (:mark a) delivered?)
             :next-scan nxt))
    (catch Throwable _ {:refused :read-error})))

(defn closing
  "The closing values a standing read's entry records (FRR3, F3): from its
  last delivery line's `:so-far`, or the seed's for an entry with none."
  [last-line opening]
  (if-let [sf (:so-far last-line)]
    {:fingerprint (:fp sf) :deliveries (:n sf) :mark (if (:partial? sf) :partial :complete)
     :moment (:moment last-line) :layer (or (:layer last-line) (:layer opening))}
    {:fingerprint (reads/standing-link nil nil nil) :deliveries 0 :mark :complete
     :moment (:moment opening) :layer (:layer opening)}))

;; ============================================================ the micro index ops (records)

(def max-put-entities 64)
(def max-put-rows
  "A micro put page stops once it has taken this many rows (RR7, halved as
  wave 1 halved the stream put page's, since every row is opened)."
  2048)
(def max-sweep-entries 512)
(def max-person-entries 256)

(defn micro-op
  "An `*micro-index-ops` record, total (PLAN-reads-rest.md, 'Rebuild from
  the log, shared', F14, F15): a put page `{:op :rebuild-put :task t :after
  [e name-or-nil]-or-nil :entities n}`, a sweep page `{:op :rebuild-sweep
  :task t :field f :after [L a]-or-nil :entries n :since b}`, a person purge
  page `{:op :person-purge :task t :person p :after [L a]-or-nil :entries
  n}`, a replayed forget `{:op :replay-forget :task t :layer L :fid fid}`,
  and a test-only drop `{:op :drop :task t :layer L :field f :entries n}`.
  Every record names a request id `:req` (a string) its progress row
  carries. Anything else is `{:refuse :bad-op}`."
  [raw]
  (try
    (let [{:keys [op task req after]} (when (map? raw) raw)
          kw? env/readable-keyword?
          n-ok? (fn [n hi] (and (int? n) (<= 1 n hi)))
          base (when (and (map? raw) (int? task) (<= 0 task) (string? req))
                 {:op op :task (long task) :req req})]
      (if (nil? base)
        {:refuse :bad-op}
        (case op
          :rebuild-put
          (if (and (n-ok? (:entities raw) max-put-entities)
                   (or (nil? after)
                       (and (vector? after) (= 2 (count after)) (kw? (nth after 0))
                            (or (nil? (nth after 1)) (env/valid-name? (nth after 1))))))
            (assoc base :entities (long (:entities raw))
                   :after (when after [(nth after 0) (some->> (nth after 1) (into []))]))
            {:refuse :bad-op})

          (:rebuild-sweep :person-purge)
          (let [field (:field raw)
                cursor-ok? (or (nil? after)
                               (and (vector? after) (= 2 (count after)) (kw? (nth after 0))
                                    (let [a (nth after 1)]
                                      (or (nil? a) (string? a) (reads/fid-ok? a)))))]
            (cond
              (and (= op :rebuild-sweep) (some #{field} sweep-fields) (n-ok? (:entries raw) max-sweep-entries)
                   cursor-ok? (or (nil? (:since raw)) (int? (:since raw))))
              (assoc base :field field :entries (long (:entries raw)) :after after :since (some-> (:since raw) long))

              (and (= op :person-purge) (kw? (:person raw)) (n-ok? (:entries raw) max-person-entries) cursor-ok?)
              (assoc base :person (:person raw) :entries (long (:entries raw)) :after after)

              :else {:refuse :bad-op}))

          :replay-forget
          (if (and (kw? (:layer raw)) (reads/fid-ok? (:fid raw)))
            (assoc base :layer (:layer raw) :fid (reads/norm-fid (:fid raw)))
            {:refuse :bad-op})

          :drop
          (if (and (kw? (:layer raw)) (some #{(:field raw)} sweep-fields) (n-ok? (:entries raw) max-sweep-entries))
            (assoc base :layer (:layer raw) :field (:field raw) :entries (long (:entries raw)))
            {:refuse :bad-op})

          {:refuse :bad-op})))
    (catch Throwable _ {:refuse :bad-op})))

(defn progress
  "The progress row a page writes on its task."
  [op cursor done? batch count]
  {:req (:req op) :op (:op op) :cursor cursor :done? (boolean done?) :batch (long batch) :count (long (or count 0))})

(defn entries-todo
  "A range read's entries as `[[address value] ...]`, total."
  [sub]
  (reads/page-entries sub))

(defn fid-parts
  "A fact id's name and index, or [nil nil]."
  [fid]
  (if (reads/fid-ok? fid) (reads/norm-fid fid) [nil nil]))

(defn names-person-lock?
  "Whether an entry's lock record names `p` among its wrap's persons."
  [entry p]
  (boolean (and (map? entry) (map? (:lock entry)) (some #{p} (locks/wrap-persons (:lock entry))))))

(defn rows-of
  "An act's rows on one entity as `[[idx row] ...]`, from a subselect over
  its idx -> row map, total."
  [x]
  (try (into [] (map (fn [[i r]] [(long i) r])) x) (catch Throwable _ [])))

(defn put-row-writes
  "A put page's writes for one act's rows on one entity (`opened`: per row
  `[idx row lock-row open-result]`), per fact and grouped by layer:
  `{layer [[fid writes] ...]}`."
  [hints e nm rec opened]
  (try
    (reduce (fn [acc [i row lock-row o]]
              (let [L (:layer row)
                    fid [(into [] nm) (long i)]
                    entry (entry-of row e fid (:stamp rec) (:batch rec) lock-row)]
                (update acc L (fnil conj []) [fid (implied-writes hints entry o)])))
            {} opened)
    (catch Throwable _ {})))

(defn merge-by-layer [a b] (merge-with into a b))

(defn rebuild-facts
  "Open results reduced on the entity task to sealed entries, keyed digests
  and erased dates. No opened sealed-value plaintext crosses to the layer;
  control values remain clear, as in the existing index and gather paths.
  Grouped by layer, as a put page's existing bounded accumulator is."
  [e nm rec opened]
  (reduce (fn [acc [i row lock-row o]]
            (let [L (:layer row)
                  fid [(into [] nm) (long i)]
                  text (when (some? (:value o)) (env/encode-value (:value o)))
                  summary {:entry (entry-of row e fid (:stamp rec) (:batch rec) lock-row)
                           :digest (when text (reads/kv-digest text))
                           :erased-at (:erased-at o)}]
              (update acc L (fnil conj []) [fid summary])))
          {} opened))

(defn rebuild-keys
  "Distinct fact keys in a layer's safe rebuild summaries."
  [facts]
  (into [] (comp (map (fn [[_ s]] (get-in s [:entry :k]))) (distinct)) facts))

(defn rebuild-writes
  "The layer's current grammar applied to a safe summary, after the open."
  [hints summary]
  (if summary
    (fact-writes hints (:entry summary) (:digest summary) (:erased-at summary))
    no-writes))

(defn fact-imp
  "What the log implies for the one fact of a sweep's `put-row-writes` in
  layer L, as `implied-map`, or {} when nothing."
  [byL L]
  (if-let [[_ w] (first (get byL L))] (implied-map w) {}))

(defn live-ek
  "A fact's writes' `:ix-ek` put when it is live: [address entry], else nil."
  [w]
  (some (fn [[f a e]] (when (and (= :ix-ek f) (map? e) (nil? (:erased-at e))) [a e])) (:index-put w)))

(defn imp-live-ek
  "An implied map's live `:ix-ek` entry, or nil."
  [imp]
  (some (fn [[k v]] (when (and (vector? k) (= :ix-ek (first k)) (map? v) (nil? (:erased-at v))) v)) imp))

(defn tomb-imp
  "What a sweep judges a fact by when its stored `:ix-ek` entry is a
  tombstone while the log it read implies the fact live (a forget of this
  batch its read did not see, the sections of a batch running side by
  side): the tombstone's reading, so a page never brings an erased value
  back (PLAN-reads-rest.md's guard, 'Rebuild from the log, shared')."
  [hints imp cur]
  (if-let [e (imp-live-ek imp)]
    (implied-map (fact-writes hints e nil (:erased-at cur)))
    imp))

(defn from-opts "Range options from an inclusive cursor." [n] {:max-amt n :inclusive? true})

(defn tag-kept
  "A loop's kept entries tagged with the store they sit in, so a merged list
  of both eras shows each through its own open step."
  [final store]
  (update final :kept (fn [ks] (mapv #(assoc % :store store) ks))))

(defn kept-store [kept store] (or (:store kept) store))

(defn settings-at
  "The last micro settings version at or below F, from a tail read."
  [sub]
  (first (vals sub)))

(defn member-batch "A member row's batch, or nil." [x] (when (int? x) x))

(defn refusal? [x] (boolean (and (map? x) (contains? x :refused))))

(defn or-refusal "The first argument when it is a refusal, else the second." [a b] (if (refusal? a) a b))

(defn reader-args
  "A read's layer and reader checked before any PState read keyed by them
  (every `$$micro` and `$$layers` key is a Keyword; a read refuses as data,
  never by throwing): nil when readable, else the refusal."
  [layer for]
  (when-not (and (env/readable-keyword? layer) (env/readable-keyword? for)) {:refused :bad-read}))

(defn scan-ok?
  "A standing read's cursor as the delta takes it: an address in its index,
  or a sealed one that opens, and the moment kind it was made in."
  [scan]
  (boolean (and (map? scan) (string? (scan-from scan)) (#{:stamp :frontier} (:kind scan)))))

(defn delta-args
  "A delta's arguments checked before any read keyed by them: nil when
  readable, else the refusal (a cursor that is not one, `:bad-scan`)."
  [layer for scan]
  (or (reader-args layer for)
      (when (and (some? scan) (not (scan-ok? scan))) {:refused :bad-scan})))

(defn upkeep-args
  "The maintenance reads' arguments, checked before any read keyed by them:
  a readable layer (and entity), an optional session keyword, an optional
  stamp bound, an optional String cursor, a page of 1 to 512."
  [layer ent session before after n]
  (when-not (and (env/readable-keyword? layer)
                 (or (nil? ent) (env/readable-keyword? ent))
                 (or (nil? session) (env/readable-keyword? session))
                 (or (nil? before) (int? before))
                 (or (nil? after) (string? after))
                 (or (nil? n) (and (int? n) (<= 1 n 512))))
    {:refused :bad-read}))


(defn record-of
  "A row's lock record: its own `:lock` (in the record), else its lock row."
  [row lock-row]
  (or (:lock row) lock-row))

(defn next-after
  "A page's next cursor over one layer's field: the last address read when
  the page was full, else the next layer's start (nil when none: done)."
  [layer ents n next-layer]
  (if (>= (count ents) n)
    {:cursor [layer (first (peek ents))] :done? false}
    {:cursor (when next-layer [next-layer nil]) :done? (nil? next-layer)}))

(defn entry-fid-e
  "A swept entry's fact id and entity, for the id and value fields (the
  entry names both); nil for anything else."
  [field v]
  (when (and (some #{field} [:ix-ek :ix-ke :ix-kv :ix-s]) (map? v))
    [(:fid v) (:e v)]))

;; ============================================================ dataflow: opening and reading

(deframafn open-entry>
  "`open-row>`'s shared twin (PLAN-reads-rest.md, '`open-entry>`'): what a
  read shows of one shared index entry, on the layer's task alone, from the
  entry (a copy of its row and of its lock record) and `$$persons`, which
  every task holds. A tombstone gives its date and opens nothing; a
  `:no-copy` entry gives `{:unreadable :no-copy}` rather than a hop to its
  entity's task (RR4); else phase 2's pure `open-with` (a wrap a person
  forget closed gives that date). `*persons` are the entries this read has
  read, handed back extended (wave 1's `open-row-with>` rule). Never
  throws."
  [*entry *persons]
  (<<cond
    (case> (some? (get *entry :erased-at)))
    (:> {:erased-at (get *entry :erased-at)} *persons)

    (case> (false? (get *entry :copy)))
    (:> {:unreadable :no-copy} *persons)

    (default>)
    (get *entry :lock :> *record)
    (locks/read-persons> (locks/wrap-persons-of *record) *persons :> *rp)
    (:> (locks/open-with *entry (get *entry :stamp) nil nil *record *rp) *rp)))

(deframafn open-one>
  "One entry opened by the store it sits in: the micro store's by
  `open-entry>`, the stream store's (a re-classed layer's stream era, or a
  one-owner layer's delta) by the read exit's `open-row-with>` as of `*m`."
  [*store *layer *entry *m *persons]
  (<<if (= :micro *store)
    (open-entry> *entry *persons :> *o *p2)
    (:> *o *p2)
   (else>)
    (reads/open-row-with> *layer (get *entry :fid) *entry (get *entry :stamp) *m *persons :> *o2 *p3)
    (:> *o2 *p3)))

(deframafn index-read>
  "One page of an index, `*n` entries of field `*ix` of `*layer` from
  `*from`, in the store named: `[[address entry] ...]`."
  [*store *layer *ix *from *n]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")
                         $$layers (this-module-pobject-task-global "$$layers")]
    (<<if (= :micro *store)
      (local-select> [(keypath *layer *ix) (sorted-map-range-from *from *n)] $$micro {:allow-yield? true} :> *sub)
     (else>)
      (local-select> [(keypath *layer *ix) (sorted-map-range-from *from *n)] $$layers {:allow-yield? true} :> *sub))
    (:> (reads/page-entries *sub))))

(deframafn tail-read>
  "The one entry just below `*end` in field `*ix` (the read exit's tail read,
  [probed]), in the store named."
  [*store *layer *ix *end]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")
                         $$layers (this-module-pobject-task-global "$$layers")]
    (<<if (= :micro *store)
      (local-select> [(keypath *layer *ix) (sorted-map-range-to *end {:max-amt 1})] $$micro :> *sub)
     (else>)
      (local-select> [(keypath *layer *ix) (sorted-map-range-to *end {:max-amt 1})] $$layers :> *sub))
    (:> *sub)))

(deframaop loop-pages>
  "The read exit's doubling page loop (RC3, F8) over either store, from a
  prepared state: pages until the range's end, limit + 1 matches or the scan
  budget; a `[:kv]` candidate counts only when it opens to the pattern's
  value (F5). Emits the final state and the person entries read."
  [*store *layer *st0 *m *persons]
  (loop<- [*pst *st0 *kpc *persons :> *pfinal *pcfinal]
    (yield-if-overtime)
    (index-read> *store *layer (get *pst :ix) (get *pst :from) (get *pst :page) :> *pents)
    (reads/kv-candidates *pst *pents :> *pcands)
    (reads/kv-need *pst :> *pneed)
    (loop<- [*kt *pcands *kn *pneed *kg {} *kpc2 *kpc :> *popened *kpc3]
      (yield-if-overtime)
      (<<if (or> (empty? *kt) (<= *kn 0))
        (:> *kg *kpc2)
       (else>)
        (first *kt :> [*ka *ke])
        (open-one> *store *layer *ke *m *kpc2 :> *ko *kpc4)
        (reads/kv-hit? *pst *ko :> *khit)
        (reads/need-after *kn *khit :> *kn2)
        (continue> (rest *kt) *kn2 (assoc *kg *ka *ko) *kpc4)))
    (reads/page-step *pst *pents *popened :> *pst2)
    (<<if (get *pst2 :done?)
      (:> *pst2 *kpc3)
     (else>)
      (continue> *pst2 *kpc3)))
  (:> *pfinal *pcfinal))

(deframaop pattern-loop>
  "A pattern read's matching step over one store (step 4): the tail read for
  `[:latest]`, else the page loop from the pattern's own bounds."
  [*store *layer *pp *m *persons]
  (<<if (= :latest (get *pp :kind))
    (reads/bounds *pp *m :> [*tfrom *tend])
    (tail-read> *store *layer :ix-ek *tend :> *tsub)
    (reads/tail-state *pp *tsub *m :> *final)
    (:> *final *persons)
   (else>)
    (reads/page-init *pp *m :> *st0)
    (loop-pages> *store *layer *st0 *m *persons :> *final2 *p2)
    (:> *final2 *p2)))

(deframaop show-kept>
  "Step 5 and 6: each kept entry shown, opened through its store's open step
  (a stream-era `:no-copy` entry by its row; a micro one answers
  `{:unreadable :no-copy}`, RR4), a shared row carrying its batch."
  [*store *layer *kept *m *persons]
  (<<with-substitutions [$$layers (this-module-pobject-task-global "$$layers")]
    (loop<- [*vt *kept *vacc [] *vpc *persons :> *vrows *vpcf]
      (yield-if-overtime)
      (<<if (empty? *vt)
        (:> *vacc *vpc)
       (else>)
        (first *vt :> *vk)
        (kept-store *vk *store :> *vs)
        (reads/show-how *vk :> *how)
        (get *vk :entry :> *ve)
        (<<cond
          (case> (= *how :read-row))
          (<<if (= :micro *vs)
            (identity {:unreadable :no-copy} :> *vo)
            (identity *vpc :> *vpc2)
           (else>)
            (reads/kept-name-idx *vk :> [*vnm *vidx])
            (local-select> [(keypath *layer :log *vnm *vidx)] $$layers :> *vrow)
            (reads/open-row-with> *layer (get *ve :fid) *vrow (get *ve :stamp) *m *vpc :> *vo *vpc2))
          (reads/shown-row *vk *vo :> *vr0)

          (case> (= *how :open))
          (open-one> *vs *layer *ve *m *vpc :> *vo2 *vpc2)
          (reads/shown-row *vk *vo2 :> *vr0)

          (default>)
          (reads/shown-row *vk nil :> *vr0)
          (identity *vpc :> *vpc2))
        (with-batch *vr0 *ve :> *vr)
        (continue> (rest *vt) (conj *vacc *vr) *vpc2)))
    (:> *vrows *vpcf)))

;; ============================================================ dataflow: the micro index's writes

(deframaop write-index>
  "A layer's shared index writes, on its task, as `termval`s and `NONE>`s at
  computed addresses (idempotent under a retried batch): puts, `:ix-of`
  sets, `:ix-id` addresses, then deletes."
  [*layer *w]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")]
    (<<atomic
      (ops/explode (get *w :index-put) :> [*f *a *e])
      (local-transform> [(keypath *layer *f *a) (termval *e)] $$micro))
    (<<atomic
      (ops/explode (get *w :index-of) :> [*ofid *oset])
      (local-transform> [(keypath *layer :ix-of *ofid) (termval *oset)] $$micro))
    (<<atomic
      (ops/explode (get *w :index-id) :> [*ifid *ia])
      (local-transform> [(keypath *layer :ix-id *ifid) (termval *ia)] $$micro))
    (<<atomic
      (ops/explode (get *w :index-del) :> [*df *da])
      (local-transform> [(keypath *layer *df *da) NONE>] $$micro))
    (:>)))

(deframaop index-block>
  "Block 2d (PLAN-reads-rest.md, 'Written in which batch'), a branch of the
  micro topology's block 2b after its name-task check: the act's entries,
  computed from the rows as block 2b writes them, the keyed digests taken
  on the arrival task, and the act's stamp and batch, written on the
  layer's task in the batch that decided the act (atomic with the rows
  across tasks). One hop per admitted act, no seek. The layer joins its
  task's shared layers. An index error writes nothing for the act and
  marks the layer (RR6, F9)."
  [*layer *nm *frows *kvd *rec *b *hints]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")
                         $$micro-task (this-module-pobject-task-global "$$micro-task")]
    (index-writes *hints *nm *frows *kvd (get *rec :stamp) *b :> *w)
    (|hash *layer)
    (local-transform> [(keypath :layers) NONE-ELEM (termval *layer)] $$micro-task)
    (<<if (get *w :index-error)
      (local-transform> [(keypath *layer :ix-error) (termval *b)] $$micro)
     (else>)
      (write-index> *layer *w))
    (:>)))

(deframaop purge-fact>
  "A value forget's purge in a shared layer (path 2), on the layer's task in
  the forget's own batch: the fact's entry through `:ix-id`, its `:ix-of`
  set, then `purge-writes` dated by the forget's stamp, the ledger's date
  (three seeks). A fact never indexed: nothing."
  [*layer *fid *date]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")]
    (local-select> [(keypath *layer :ix-id *fid)] $$micro :> *a)
    (<<if (some? *a)
      (local-select> [(keypath *layer :ix-ek *a)] $$micro :> *entry)
      (local-select> [(keypath *layer :ix-of *fid)] $$micro :> *kv)
      (purge-writes *entry *kv *date :> *w)
      (write-index> *layer *w))
    (:>)))

;; ============================================================ dataflow: the micro index pages

(defn put-offset "The row offset a put page starts an entity from." [after e]
  (when (and (vector? after) (= e (nth after 0))) (nth after 1)))

(defn page-end-cursor "A put page's cursor past its last entity." [ents]
  (when (seq ents) [(peek ents) nil]))

(defn put-done? [full? ents n] (boolean (and (not full?) (< (count ents) n))))

(defn one-row [idx row] [[idx row]])

(defn writes-or-none [w] (or w no-writes))

(defn live-for?
  "Whether a person purge takes this entry: not a tombstone, and its lock
  record names the person."
  [entry p]
  (boolean (and (map? entry) (nil? (:erased-at entry)) (names-person-lock? entry p))))

(defn erasure-date
  "The date the open step gives for a value that no longer opens (the
  ledger's, else the wrap's close), or nil."
  [ledger record persons]
  (some-> (locks/erasure ledger record persons) :stamp))

(defn gap-cleared? [err since] (boolean (and (int? err) (int? since) (< err since))))

(defn judged? [rec b] (not (and (map? rec) (int? (:batch rec)) (>= (:batch rec) b))))

(deframaop open-rows>
  "An act's rows on one entity opened as a reader would (`[[idx row
  lock-row open-result] ...]`), on the entity's task: the ledger entry of
  each row's lock, its lock row when not in the record, the wrap's persons,
  then phase 2's `open-with`. The plaintext stays here: a caller takes only
  a keyed digest of it."
  [*e *rows *stamp]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")]
    (loop<- [*rt *rows *out [] *pc {} :> *opened]
      (<<if (empty? *rt)
        (:> *out)
       (else>)
        (first *rt :> [*ri *rr])
        (get *rr :lock-id :> *rlid)
        (<<if (some? *rlid)
          (local-select> [(keypath *e :erased *rlid)] $$micro :> *ledger)
          (<<if (nil? (get *rr :lock))
            (local-select> [(keypath *e :locks *rlid)] $$micro :> *lrow)
           (else>)
            (identity nil :> *lrow))
         (else>)
          (identity nil :> *ledger)
          (identity nil :> *lrow))
        (record-of *rr *lrow :> *record)
        (locks/read-persons> (locks/wrap-persons-of *record) *pc :> *pc2)
        (locks/open-with *rr *stamp nil *ledger *record *pc2 :> *o)
        (continue> (rest *rt) (conj *out [*ri *rr *lrow *o]) *pc2)))
    (:> *opened)))

(deframaop guarded-write>
  "A put page's writes for one fact, on the layer's task: live entries are
  not written over a tombstone (one seek, the fact's `:ix-ek` address): a
  forget of this batch may have purged it after the page read the log."
  [*L *w]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")]
    (live-ek *w :> *le)
    (<<if (some? *le)
      (local-select> [(keypath *L :ix-ek (first *le))] $$micro :> *cur)
      (<<if (nil? (get *cur :erased-at))
        (write-index> *L *w))
     (else>)
      (write-index> *L *w))
    (:>)))

(deframafn guard-imp>
  "A sweep's implied map, on the layer's task, read against the fact's
  stored `:ix-ek` entry (`tomb-imp`)."
  [*L *imp *hints]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")]
    (imp-live-ek *imp :> *le)
    (<<if (nil? *le)
      (:> *imp)
     (else>)
      (get *imp :ix-id :> *addr)
      (local-select> [(keypath *L :ix-ek *addr)] $$micro :> *cur)
      (<<if (some? (get *cur :erased-at))
        (:> (tomb-imp *hints *imp *cur))
       (else>)
        (:> *imp)))))

(deframaop put-page>
  "A micro rebuild's put page on its task (F13, RR7): the next entities of
  `$$micro` on this task after the cursor, each entity's answer records
  from its row offset (bounded, never read whole), every row of a yes act
  opened on the entity's task and turned into the entries the log implies
  (tombstones for a row that no longer opens), grouped by layer and written
  on each layer's task. At most 64 entities and 2,048 rows; the rest waits
  for the next page. Puts only. The progress row on this task says where
  the next page starts."
  [*op *b]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")
                         $$micro-task (this-module-pobject-task-global "$$micro-task")]
    (get *op :after :> *after)
    (get *op :entities :> *n)
    (<<cond
      (case> (nil? *after))
      (local-select> (subselect (sorted-map-range-from-start *n) MAP-KEYS) $$micro :> *ents)

      (case> (some? (second *after)))
      (local-select> (subselect (sorted-map-range-from (first *after) (from-opts *n)) MAP-KEYS)
                     $$micro :> *ents)

      (default>)
      (local-select> (subselect (sorted-map-range-from (first *after) (reads/after-opts *n)) MAP-KEYS)
                     $$micro :> *ents))
    (loop<- [*et *ents *acc {} *taken 0 :> *byL *cursor *full?]
      (<<if (empty? *et)
        (:> *acc (page-end-cursor *ents) false)
       (else>)
        (first *et :> *e)
        (put-offset *after *e :> *off)
        (- max-put-rows *taken :> *room)
        (<<if (nil? *off)
          (local-select> [(keypath *e :answers) (sorted-map-range-from-start *room)] $$micro {:allow-yield? true} :> *recs)
         (else>)
          (local-select> [(keypath *e :answers) (sorted-map-range-from *off (reads/after-opts *room))]
                         $$micro {:allow-yield? true} :> *recs))
        (reads/put-todo *recs :> *todo)
        (loop<- [*at *todo *acc2 *acc *taken2 *taken *lastnm *off :> *acc3 *taken3 *lastnm2]
          (<<if (or> (empty? *at) (>= *taken2 max-put-rows))
            (:> *acc2 *taken2 *lastnm)
           (else>)
            (first *at :> [*anm *arec])
            (<<if (= :yes (get *arec :answer))
              (local-select> [(keypath *e :log *anm) (subselect ALL)] $$micro {:allow-yield? true} :> *arows0)
              (rows-of *arows0 :> *arows)
              (open-rows> *e *arows (get *arec :stamp) :> *opened)
              (rebuild-facts *e *anm *arec *opened :> *w)
              (continue> (rest *at) (merge-by-layer *acc2 *w) (+ *taken2 (count *arows)) *anm)
             (else>)
              (continue> (rest *at) *acc2 *taken2 *anm))))
        (<<if (or> (>= *taken3 max-put-rows) (>= (count *todo) *room))
          (:> *acc3 [*e *lastnm2] true)
         (else>)
          (continue> (rest *et) *acc3 *taken3))))
    (local-transform> [(keypath :rebuild) (termval (progress *op *cursor (put-done? *full? *ents *n) *b (count *byL)))]
                      $$micro-task)
    (ops/explode-map *byL :> *L *facts)
    (|hash *L)
    (local-transform> [(keypath :layers) NONE-ELEM (termval *L)] $$micro-task)
    (key-rows-of> *L (rebuild-keys *facts) :> *keyrows)
    (reads/hints-of *keyrows :> *hints)
    (ops/explode *facts :> [*fid *summary])
    (rebuild-writes *hints *summary :> *w)
    (guarded-write> *L *w)))

(deframafn sweep-start>
  "Where a sweep or person purge page starts on its task: the cursor's layer
  and address, or the first shared layer homed here."
  [*after]
  (<<with-substitutions [$$micro-task (this-module-pobject-task-global "$$micro-task")]
    (<<if (nil? *after)
      (local-select> [(keypath :layers) (sorted-set-range-from-start 1)] $$micro-task :> *firsts)
      (:> (first *firsts) nil)
     (else>)
      (:> (first *after) (second *after)))))

(deframafn next-layer>
  "The shared layer homed on this task after `*L`, or nil."
  [*L]
  (<<with-substitutions [$$micro-task (this-module-pobject-task-global "$$micro-task")]
    (local-select> [(keypath :layers) (sorted-set-range-from *L (reads/after-opts 1))] $$micro-task :> *nexts)
    (:> (first *nexts))))

(deframafn layer-page>
  "One page of a layer's field on this task, after an exclusive cursor."
  [*L *field *a *n]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")]
    (<<if (nil? *a)
      (local-select> [(keypath *L *field) (sorted-map-range-from-start *n)] $$micro {:allow-yield? true} :> *sub)
     (else>)
      (local-select> [(keypath *L *field) (sorted-map-range-from *a (reads/after-opts *n))] $$micro {:allow-yield? true} :> *sub))
    (:> (entries-todo *sub))))

(deframaop sweep-one>
  "One swept entry judged against the log (F2's rule for the micro store,
  'Rebuild from the log, shared'): its fact found from the entry (or, for
  the fields addressed by fact id, through `:ix-id`), its act's record and
  row read and opened on the entity's task, what the log implies for it
  taken back to the layer's task, and the entry kept, rewritten or deleted.
  An entry whose act was decided in this batch is not judged. Two hops."
  [*L *field *a *v *b]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")]
    (<<cond
      (case> (some? (entry-fid-e *field *v)))
      (entry-fid-e *field *v :> [*fid *e])

      (case> (= :ix-id *field))
      (identity *a :> *fid)
      (local-select> [(keypath *L :ix-ek *v)] $$micro :> *ie)
      (get *ie :e :> *e)

      (default>)
      (identity *a :> *fid)
      (local-select> [(keypath *L :ix-id *a)] $$micro :> *oa)
      (<<if (some? *oa)
        (local-select> [(keypath *L :ix-ek *oa)] $$micro :> *oe)
       (else>)
        (identity nil :> *oe))
      (get *oe :e :> *e))
    (fid-parts *fid :> [*nm *idx])
    (<<if (or> (nil? *nm) (nil? *e))
      (sweep-writes *field *a *v {} :> *w0)
      (write-index> *L *w0)
     (else>)
      (|hash *e)
      (local-select> [(keypath *e :answers *nm)] $$micro :> *rec)
      (<<if (judged? *rec *b)
        (<<if (= :yes (get *rec :answer))
          (local-select> [(keypath *e :log *nm *idx)] $$micro :> *row)
         (else>)
          (identity nil :> *row))
        (<<if (some? *row)
          (open-rows> *e (one-row *idx *row) (get *rec :stamp) :> *opened)
          (rebuild-facts *e *nm *rec *opened :> *byL)
          (get *byL *L :> *facts)
         (else>)
          (identity [] :> *facts))
        (|hash *L)
        (key-rows-of> *L (rebuild-keys *facts) :> *keyrows)
        (reads/hints-of *keyrows :> *hints)
        (second (first *facts) :> *summary)
        (rebuild-writes *hints *summary :> *iw)
        (implied-map *iw :> *imp0)
        (guard-imp> *L *imp0 *hints :> *imp)
        (sweep-writes *field *a *v *imp :> *w)
        (write-index> *L *w)))))

(deframaop sweep-page>
  "A micro rebuild's sweep page on its task (RR7): the next entries of one
  field of the shared layers homed here, after the cursor `[L a]`, each
  judged against the log (`sweep-one>`). When the last field's sweep
  finishes a layer, its index gap flag (F9) is cleared if the gap is older
  than the rebuild (`:since`, the rebuild's first batch)."
  [*op *b]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")
                         $$micro-task (this-module-pobject-task-global "$$micro-task")]
    (get *op :field :> *field)
    (get *op :entries :> *n)
    (sweep-start> (get *op :after) :> *L *a)
    (<<if (nil? *L)
      (local-transform> [(keypath :rebuild) (termval (progress *op nil true *b 0))] $$micro-task)
     (else>)
      (layer-page> *L *field *a *n :> *ents)
      (<<if (< (count *ents) *n)
        (next-layer> *L :> *nextL)
        (<<if (= :ix-id *field)
          (local-select> [(keypath *L :ix-error)] $$micro :> *err)
          (<<if (gap-cleared? *err (get *op :since))
            (local-transform> [(keypath *L :ix-error) (termval nil)] $$micro)))
       (else>)
        (identity nil :> *nextL))
      (next-after *L *ents *n *nextL :> *nx)
      (local-transform> [(keypath :rebuild) (termval (progress *op (get *nx :cursor) (get *nx :done?) *b (count *ents)))]
                        $$micro-task)
      (ops/explode *ents :> [*ea *ev])
      (sweep-one> *L *field *ea *ev *b))))

(deframaop person-purge-page>
  "A person purge page for the micro store on its task (path 3, the shared
  half, RR17): the next `:ix-s` entries of the shared layers homed here
  after the cursor; each live entry whose lock names the person and whose
  wrap is now closed (person entries read here, where every task holds
  them) is purged by value id, dated by the wrap's close: the date the open
  step gives, so the purge writes what a rebuild would. All on this task."
  [*op *b]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")
                         $$micro-task (this-module-pobject-task-global "$$micro-task")]
    (get *op :person :> *p)
    (get *op :entries :> *n)
    (sweep-start> (get *op :after) :> *L *a)
    (<<if (nil? *L)
      (local-transform> [(keypath :rebuild) (termval (progress *op nil true *b 0))] $$micro-task)
     (else>)
      (layer-page> *L :ix-s *a *n :> *ents)
      (<<if (< (count *ents) *n)
        (next-layer> *L :> *nextL)
       (else>)
        (identity nil :> *nextL))
      (next-after *L *ents *n *nextL :> *nx)
      (local-transform> [(keypath :rebuild) (termval (progress *op (get *nx :cursor) (get *nx :done?) *b (count *ents)))]
                        $$micro-task)
      (ops/explode *ents :> [*ea *ev])
      (filter> (live-for? *ev *p))
      (get *ev :lock :> *record)
      (locks/read-persons> (locks/wrap-persons-of *record) {} :> *wp)
      (locks/wrap-closed *record *wp :> *date)
      (filter> (some? *date))
      (local-select> [(keypath *L :ix-of (get *ev :fid))] $$micro :> *kv)
      (purge-writes *ev *kv *date :> *w)
      (write-index> *L *w))))

(deframaop replay-forget>
  "A forget replayed after a restore, in a shared layer (F15), on the
  layer's task: the target's entry through `:ix-id`; on its entity's task
  the date the open step gives (the ledger's, else the wrap's close: in the
  rig no lock is restored, L17, so the forget's own effect changes nothing
  and is not rerun); back on the layer's task the purge at that date.
  Idempotent, and independent of its order among other replays."
  [*op *b]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")
                         $$micro-task (this-module-pobject-task-global "$$micro-task")]
    (get *op :layer :> *L)
    (get *op :fid :> *fid)
    (local-transform> [(keypath :rebuild) (termval (progress *op nil true *b 1))] $$micro-task)
    (local-select> [(keypath *L :ix-id *fid)] $$micro :> *a)
    (filter> (some? *a))
    (local-select> [(keypath *L :ix-ek *a)] $$micro :> *entry)
    (get *entry :e :> *e)
    (fid-parts *fid :> [*nm *idx])
    (filter> (some? *e))
    (|hash *e)
    (local-select> [(keypath *e :log *nm *idx)] $$micro :> *row)
    (get *row :lock-id :> *lid)
    (<<if (some? *lid)
      (local-select> [(keypath *e :erased *lid)] $$micro :> *ledger)
      (<<if (nil? (get *row :lock))
        (local-select> [(keypath *e :locks *lid)] $$micro :> *lrow)
       (else>)
        (identity nil :> *lrow))
     (else>)
      (identity nil :> *ledger)
      (identity nil :> *lrow))
    (record-of *row *lrow :> *record)
    (locks/read-persons> (locks/wrap-persons-of *record) {} :> *rp)
    (erasure-date *ledger *record *rp :> *date)
    (filter> (some? *date))
    (|hash *L)
    (local-select> [(keypath *L :ix-of *fid)] $$micro :> *kv)
    (purge-writes *entry *kv *date :> *w)
    (write-index> *L *w)))

(deframaop drop-page>
  "Test only (RT8): delete the first n entries of one shared field."
  [*op *b]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")
                         $$micro-task (this-module-pobject-task-global "$$micro-task")]
    (get *op :layer :> *L)
    (get *op :field :> *f)
    (layer-page> *L *f nil (get *op :entries) :> *ents)
    (local-transform> [(keypath :rebuild) (termval (progress *op nil true *b (count *ents)))] $$micro-task)
    (ops/explode *ents :> [*a *v])
    (local-transform> [(keypath *L *f *a) NONE>] $$micro)))

(deframaop micro-ops>
  "The `*micro-index-ops` block of the micro topology (maintenance, RR8: no
  act, nothing in the record): each record parsed totally, dropped as data
  when malformed or naming a task outside [0, N), then routed to its task
  with `|direct` and run as one bounded page. A page's progress row on its
  task is written in its batch."
  [*raw]
  (micro-op *raw :> *op)
  (filter> (not (contains? *op :refuse)))
  (ops/module-instance-info :> *info)
  (filter> (reads/task-ok? *info (get *op :task)))
  (|direct (get *op :task))
  (ops/current-microbatch-id :> *b)
  (get *op :op :> *kind)
  (<<cond
    (case> (= *kind :rebuild-put))
    (put-page> *op *b)

    (case> (= *kind :rebuild-sweep))
    (sweep-page> *op *b)

    (case> (= *kind :person-purge))
    (person-purge-page> *op *b)

    (case> (= *kind :replay-forget))
    (replay-forget> *op *b)

    (default>)
    (drop-page> *op *b)))

;; ============================================================ the stream side's grammar (wave 2)
;; Phase 6 moved the stream store's index hints and opaque marks to the layer's
;; grammar facts (its key rows in `$$layers`); the micro store uses
;; effective rows in step 6b. Where a read meets the stream store (a
;; one-owner layer's delta, a re-classed layer's stream era), it takes the rows,
;; as the read exit's own queries do (wave 2's merge, W2-3).

(defn stream-hints
  "The hints a pattern is parsed with: for a one-owner layer the read exit's
  `parse-hints`, so the pattern's form is checked whatever the grammar and
  its key's grammar is applied after the visibility check
  (`reads/kv-refusal`), as `read-pattern` does since phase 6; for a shared
  layer likewise form-only hints, with effective rows checked after visibility."
  [shared? pattern]
  (reads/parse-hints pattern))

(defn stream-era-kv-refusal
  "Why a `[:kv]` read of a re-classed layer is refused by its stream era's
  grammar, or nil. `row` is the key's row in `$$layers`, the stream era's
  (nil for none). When the stream era holds a fact under the key (the
  row's `:used`) and its grammar there gives the key no value index, or
  makes it opaque, that era's value index cannot answer, so the read is
  refused as the read exit refuses a one-owner layer's
  (`reads/kv-refusal`); a key the stream era never used leaves the read to
  the micro era."
  [pp row]
  (when (true? (:used row)) (reads/kv-refusal pp row)))

(defn stream-row?
  "Whether a shown row is the stream store's: its fact's name is tagged by
  layer (a re-classed layer's stream era)."
  [row]
  (= :by-layer (try (nth (nth (:fid row) 0) 1) (catch Throwable _ nil))))

(defn stream-shown-keys
  "The keys whose rows mark a read's rows: every row's for a one-owner
  layer (`all?`), else only the stream-era rows'."
  [rows all?]
  (reads/shown-keys (if all? rows (filterv stream-row? rows))))

(defn mark-stream-rows
  "`reads/mark-opaque` over the stream store's rows: all of them for a
  one-owner layer (`all?`), else a re-classed layer's stream-era rows only,
  the micro era's left to its own grammar (phase 6b). `key-rows` are
  `$$layers`' rows of the keys shown. Total."
  [rows key-rows all?]
  (try
    (let [marked (reads/mark-opaque rows key-rows)]
      (if all?
        marked
        (mapv (fn [r m] (if (stream-row? r) m r)) rows marked)))
    (catch Throwable _ rows)))

(deframaop stream-marks>
  "Phase 6's opaque marks on the rows a read shows from the stream store:
  the rows in `$$layers` of the keys those rows show, read on this task,
  one seek a distinct key, in a loop that yields (a read may show 10,000
  rows), as the read exit's two queries read them; then
  `mark-stream-rows`. Emits the rows once."
  [*layer *rows *all?]
  (<<with-substitutions [$$layers (this-module-pobject-task-global "$$layers")]
    (stream-shown-keys *rows *all? :> *ks)
    (loop<- [*t *ks *acc {} :> *krows]
      (yield-if-overtime)
      (<<if (empty? *t)
        (:> *acc)
       (else>)
        (first *t :> *k)
        (local-select> [(keypath *layer :key-rows *k)] $$layers :> *row)
        (continue> (rest *t) (assoc *acc *k *row))))
    (:> (mark-stream-rows *rows *krows *all?))))

;; ============================================================ dataflow: the shared reads

(defn micro-shown-keys [rows]
  (reads/shown-keys (filterv (complement stream-row?) rows)))

(defn mark-micro-rows [rows key-rows]
  (mapv (fn [r m] (if (stream-row? r) r m)) rows (reads/mark-opaque rows key-rows)))

(deframaop micro-marks>
  "Micro-era shown values use effective grammar rows on the layer's task.
  Stream-era rows retain W2-3's own marks."
  [*layer *rows]
  (key-rows-of> *layer (micro-shown-keys *rows) :> *keyrows)
  (:> (mark-micro-rows *rows *keyrows)))

(deframafn micro-visibility>
  "Step 2 of a shared read: the layer's micro settings as of F (one tail
  read, for a group; a re-classed layer's are its stream settings, already
  read) and, for a group, the reader's membership (one seek); then
  `visible?`."
  [*layer *for *ss *F]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")]
    (<<if (some? *ss)
      (:> (visible? *ss nil nil *F *for))
     (else>)
      (inc *F :> *F1)
      (local-select> [(keypath *layer :settings) (sorted-map-range-to *F1 {:max-amt 1})] $$micro :> *ssub)
      (local-select> [(keypath *layer :members *for)] $$micro :> *mb)
      (:> (visible? nil (settings-at *ssub) (member-batch *mb) *F *for)))))

(deframaop shared-pattern>
  "RS1, a pattern read of a shared layer through the one exit
  (PLAN-reads-rest.md, 'The shared pattern read, step by step'), on the
  layer's task: the moment (F = min(asked, this task's frontier); a stamp
  moment refused `:moment-kind`), visibility as of F, the pattern (the read
  exit's parser), the index gap flag (F9), the page loop over `$$micro [L]`
  with visibility by batch, and for a re-classed layer its stream era by
  the read exit's own loop, merged by the entries' fields (F1); each kept
  entry shown through its store's open step. The answer is the read exit's
  with `{:frontier F}`, `:max-stamp` and each row's batch.

  Wave 2's merge (W2-3): the stream era of a re-classed layer takes its own
  grammar facts, as the read exit does: a `[:kv]` read of a key that era
  used but never indexed by value (or made opaque) is refused
  (`stream-era-kv-refusal`), and its rows get their opaque marks
  (`stream-marks>`); the micro era uses effective micro-first rows."
  [*layer *for *pattern *as-of *limit *ss]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")
                         $$micro-task (this-module-pobject-task-global "$$micro-task")
                         $$layers (this-module-pobject-task-global "$$layers")
                         $$clock (this-module-pobject-task-global "$$clock")]
    (local-select> [(keypath :frontier)] $$micro-task :> *Ft)
    (or-refusal (reader-args *layer *for) (moment *as-of *Ft) :> *mo)
    (<<if (refusal? *mo)
      (:> *mo)
     (else>)
      (get *mo :F :> *F)
      (micro-visibility> *layer *for *ss *F :> *vis)
      (<<if (not *vis)
        (:> {:refused :not-visible})
       (else>)
        (reads/parse-pattern *pattern *limit *as-of (reads/parse-hints *pattern) :> *pp0)
        (reads/check-layer *pp0 *layer :bad-pattern :> *pp)
        (micro-pp *pp :> *mpp)
        ;; the stream era's own grammar for a [:kv] read's key (one seek, a re-classed layer only)
        (reads/kv-key *pp :> *kvk)
        (<<if (some? *kvk)
          (key-rows-of> *layer [*kvk] :> *mkrows)
         (else>)
          (identity {} :> *mkrows))
        (reads/kv-refusal *pp (get *mkrows *kvk) :> *mkvr)
        (<<if (and> (some? *ss) (some? *kvk))
          (local-select> [(keypath *layer :key-rows *kvk)] $$layers :> *kvrow)
         (else>)
          (identity nil :> *kvrow))
        (stream-era-kv-refusal *pp *kvrow :> *skvr)
        (<<cond
          (case> (refusal? *pp))
          (:> *pp)

          (case> (refusal? *mpp))
          (:> *mpp)

          (case> (some? *mkvr))
          (:> {:refused *mkvr})

          (case> (some? *skvr))
          (:> {:refused *skvr})

          (default>)
          (local-select> [(keypath *layer :ix-error)] $$micro :> *err)
          (pattern-loop> :micro *layer *mpp *F {} :> *mfinal *pc1)
          (<<if (some? *ss)
            ;; step 6: the stream era of a re-classed layer, as of the home's clock (F10)
            (local-select> STAY $$clock :> *clock)
            (pattern-loop> :stream *layer *pp *clock *pc1 :> *sfinal *pc2)
            (merge-eras (get *pp :kind) (get *pp :limit) (tag-kept *sfinal :stream) (tag-kept *mfinal :micro) :> *final)
           (else>)
            (identity nil :> *clock)
            (identity *pc1 :> *pc2)
            (identity (tag-kept *mfinal :micro) :> *final))
          (show-kept> :micro *layer (get *final :kept) *clock *pc2 :> *rows0 *pc3)
          (<<if (some? *ss)
            (stream-marks> *layer *rows0 false :> *rows1)
           (else>)
            (identity *rows0 :> *rows1))
          (micro-marks> *layer *rows1 :> *rows)
          (reads/pattern-answer *layer *F *pp *rows (get *final :more?) :> *a0)
          (:> (shared-answer *a0 *F *err)))))))

(defn fid-class "A fact id's name's class tag, or nil." [fid] (try (nth (nth fid 0) 1) (catch Throwable _ nil)))

(defn visible-entry?
  "Whether a shared entry is shown at F: its batch at or below F (the one
  reader's rule, §D); a tombstone keeps its act's batch, so an erasure shows
  with only its date whenever its fact would (erasures cross the frontier)."
  [entry F]
  (boolean (and (map? entry) (int? (:batch entry)) (<= (:batch entry) F))))

(deframaop shared-point>
  "RS2, a point read of a shared layer (PLAN-reads-rest.md, 'The shared
  point read'), on the layer's task: the moment and visibility as RS1;
  per fact id, a stream-era id (a re-classed layer's) by the read exit's
  own steps as of the home's clock, a micro-era id through `:ix-id` then
  `:ix-ek` (two seeks), shown when its batch is at or below F; else
  `{:fid :absent true}` (the three causes not told apart). Wave 2's merge
  (W2-3): the stream-era rows get their opaque marks from that era's
  grammar facts (`stream-marks>`)."
  [*layer *for *fids *as-of *ss]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")
                         $$micro-task (this-module-pobject-task-global "$$micro-task")
                         $$layers (this-module-pobject-task-global "$$layers")
                         $$clock (this-module-pobject-task-global "$$clock")]
    (local-select> [(keypath :frontier)] $$micro-task :> *Ft)
    (or-refusal (reader-args *layer *for) (moment *as-of *Ft) :> *mo)
    (<<if (refusal? *mo)
      (:> *mo)
     (else>)
      (get *mo :F :> *F)
      (micro-visibility> *layer *for *ss *F :> *vis)
      (<<if (not *vis)
        (:> {:refused :not-visible})
       (else>)
        (reads/parse-point *for *fids *as-of :> *p0)
        (reads/check-layer *p0 *layer :bad-read :> *p)
        (<<if (refusal? *p)
          (:> *p)
         (else>)
          (local-select> STAY $$clock :> *clock)
          (loop<- [*todo (get *p :fids) *acc [] *qpc {} :> *rows0]
            (yield-if-overtime)
            (<<if (empty? *todo)
              (:> *acc)
             (else>)
              (first *todo :> *fid)
              (first *fid :> *nm)
              (second *fid :> *idx)
              (<<cond
                (case> (= :by-layer (fid-class *fid)))
                (local-select> [(keypath *layer :answers *nm)] $$layers :> *rec)
                (<<if (reads/yes-by? *rec *clock)
                  (local-select> [(keypath *layer :log *nm *idx)] $$layers :> *row)
                  (<<if (nil? *row)
                    (reads/absent-row *fid :> *r)
                    (identity *qpc :> *qpc2)
                   (else>)
                    (reads/open-row-with> *layer *fid *row (get *rec :stamp) *clock *qpc :> *o *qpc2)
                    (reads/point-row *fid (get *rec :stamp) *row *o :> *r))
                 (else>)
                  (reads/absent-row *fid :> *r)
                  (identity *qpc :> *qpc2))

                (default>)
                (local-select> [(keypath *layer :ix-id *fid)] $$micro :> *a)
                (<<if (some? *a)
                  (local-select> [(keypath *layer :ix-ek *a)] $$micro :> *entry)
                 (else>)
                  (identity nil :> *entry))
                (<<if (visible-entry? *entry *F)
                  (open-entry> *entry *qpc :> *eo *qpc2)
                  (reads/point-row *fid (get *entry :stamp) *entry *eo :> *r0)
                  (with-batch *r0 *entry :> *r)
                 (else>)
                  (reads/absent-row *fid :> *r)
                  (identity *qpc :> *qpc2)))
              (continue> (rest *todo) (conj *acc *r) *qpc2)))
          (<<if (some? *ss)
            (stream-marks> *layer *rows0 false :> *rows1)
           (else>)
            (identity *rows0 :> *rows1))
          (micro-marks> *layer *rows1 :> *rows)
          (reads/point-answer *layer *F *rows :> *a0)
          (:> (shared-answer *a0 *F nil)))))))

;; ============================================================ dataflow: deltas

(defn scan-kind "A cursor's moment kind, `:stamp` or `:frontier`." [scan] (:kind scan))

(defn opening-answer
  "A standing read's opening read (FRR1): the one exit's pattern answer as
  `read-pattern` gave it, with the running closing value through its line
  (when it matched anything) and the handle's first cursor: just past its
  moment in the delta's index (an opening cut by the limit is not caught up
  later; its line and the closing mark say partial, a rig choice). The
  pattern is parsed with `stream-hints` (wave 2's merge, W2-3), so a
  one-owner layer's `[:kv]` read of a key its grammar indexes gets its
  cursor, whatever the constant says."
  [a pattern limit prev]
  (try
    (if (refusal? a)
      a
      (let [moment (:moment a)
            shared? (contains? moment :frontier)
            pp (reads/parse-pattern pattern limit nil (stream-hints shared? pattern))
            pp (if shared? (micro-pp pp) pp)
            m (if shared? (:frontier moment) (:stamp moment))
            delivered? (seq (:rows a))]
        (assoc a
               :nothing-new (not delivered?)
               :so-far (so-far prev moment (:fingerprint a) (:mark a) delivered?)
               :next-scan {:from (after-bound pp m) :kind (if shared? :frontier :stamp)})))
    (catch Throwable _ {:refused :read-error})))

(defn latest-final
  "A `[:latest]` delta's result: its head when new since the cursor, else
  nothing."
  [t0 from]
  (if (latest-new? (:kept t0) from) t0 (assoc t0 :kept [] :more? false)))

(defn frontier-or [x] (long (or x -1)))

(deframaop delta-read>
  "The delta's reading once the store, the moment and the cursor are known.
  A one-owner layer's rows get their opaque marks from its grammar facts,
  as `read-pattern`'s do (wave 2's merge, W2-3)."
  [*store *layer *pp *m *from *prev *mkind]
  (<<if (= :latest (get *pp :kind))
    (reads/bounds *pp *m :> [*tfrom *tend])
    (tail-read> *store *layer :ix-ek *tend :> *tsub)
    (reads/tail-state *pp *tsub *m :> *t0)
    (latest-final *t0 *from :> *final)
    (identity {} :> *pc)
   (else>)
    (delta-state *pp *m *from :> *st0)
    (loop-pages> *store *layer *st0 *m {} :> *final *pc))
  (show-kept> *store *layer (get *final :kept) *m *pc :> *rows0 *pc2)
  (<<if (= :stream *store)
    (stream-marks> *layer *rows0 true :> *rows)
   (else>)
    (micro-marks> *layer *rows0 :> *rows))
  (next-scan *pp *m *final :> *nxt0)
  (assoc *nxt0 :kind *mkind :> *nxt)
  (:> (delta-answer *layer *mkind *m *pp *rows (get *final :more?) *prev *nxt)))

(deframaop delta>
  "RS3, a standing read's delta (PLAN-reads-rest.md, 'The delta'), on the
  read layer's task: every match that became visible after the handle's
  cursor and at or before the read's moment (the home's clock, or this
  task's frontier), over the pattern's own time-ordered index or `:ix-s`,
  with the read exit's limit and scan budget; each shown through its open
  step. A stamp cursor meeting a layer now of the shared kind answers
  `{:refused :reclassed}` (F18); a frontier cursor on a one-owner layer
  `:moment-kind`. Only the micro era of a re-classed layer is read (RR12).

  Wave 2's merge (W2-3): a one-owner layer's delta parses with
  `stream-hints` and applies its key's grammar after the visibility check,
  as `read-pattern` does (a `[:kv]` read of a key the layer does not index
  by value is refused `:not-indexed`, of an opaque one `:opaque`); a shared
  layer's uses effective micro-first rows after visibility as well."
  [*layer *for *pattern *limit *scan *prev]
  (<<with-substitutions [$$layers (this-module-pobject-task-global "$$layers")
                         $$micro (this-module-pobject-task-global "$$micro")
                         $$micro-task (this-module-pobject-task-global "$$micro-task")
                         $$clock (this-module-pobject-task-global "$$clock")]
    (local-select> [(keypath *layer :settings)] $$layers :> *ss)
    (reads/parse-pattern *pattern *limit nil (stream-hints (reads/shared-layer? *ss) *pattern) :> *pp0)
    (reads/check-layer *pp0 *layer :bad-pattern :> *pp)
    (<<cond
      (case> (refusal? *pp))
      (:> *pp)

      (case> (reads/shared-layer? *ss))
      (local-select> [(keypath :frontier)] $$micro-task :> *Ft)
      (frontier-or *Ft :> *F)
      (micro-pp *pp :> *mpp)
      (micro-visibility> *layer *for *ss *F :> *vis)
      (<<cond
        (case> (= :stamp (scan-kind *scan)))
        (:> {:refused :reclassed})

        (case> (not *vis))
        (:> {:refused :not-visible})

        (case> (refusal? *mpp))
        (:> *mpp)

        (default>)
        (reads/kv-key *pp :> *mkvk)
        (<<if (some? *mkvk)
          (key-rows-of> *layer [*mkvk] :> *mkrows)
         (else>)
          (identity {} :> *mkrows))
        (reads/kv-refusal *pp (get *mkrows *mkvk) :> *mkvr)
        (<<if (some? *mkvr)
          (:> {:refused *mkvr})
         (else>)
          (delta-read> :micro *layer *mpp *F (scan-from *scan) *prev :frontier :> *a)
          (:> *a)))

      (default>)
      (<<cond
        (case> (not (reads/visible? *ss *for)))
        (:> {:refused :not-visible})

        (case> (= :frontier (scan-kind *scan)))
        (:> {:refused :moment-kind})

        (default>)
        ;; a [:kv] delta's key row: its grammar in the layer says whether it is indexed by value
        (reads/kv-key *pp :> *kvk)
        (<<if (some? *kvk)
          (local-select> [(keypath *layer :key-rows *kvk)] $$layers :> *kvrow)
         (else>)
          (identity nil :> *kvrow))
        (reads/kv-refusal *pp *kvrow :> *kvr)
        (<<if (some? *kvr)
          (:> {:refused *kvr})
         (else>)
          (local-select> STAY $$clock :> *clock)
          (delta-read> :stream *layer *pp *clock (scan-from *scan) *prev :stamp :> *a2)
          (:> *a2))))))


;; ============================================================ dataflow: the maintenance reads

(defn key-prefix "An `:ix-ke` prefix for one key." [k] (str (reads/kw-text k) sep))

(defn line-prefix "An `:ix-ek` prefix for one entity and key." [ent k] (str (reads/kw-text ent) sep (reads/kw-text k) sep))

(defn under-prefix
  "The entry of a tail read when its address starts with `prefix`."
  [sub prefix]
  (let [[[a e]] (reads/page-entries sub)]
    (when (and (string? a) (.startsWith ^String a ^String prefix)) e)))

(defn below [end ents] (filterv (fn [[a _]] (neg? (compare a end))) ents))

(defn open-entities
  "A standing read's open entries: the opening facts `[[address entry]
  ...]` whose entity has no closing fact, `[[ent fid] ...]`."
  [opens closes]
  (let [closed (into #{} (map (comp :e second)) closes)]
    (into [] (comp (map second) (remove #(contains? closed (:e %))) (map (fn [e] [(:e e) (:fid e)]))) opens)))

(defn id-candidates
  "The read-entry facts of a page (`reads/read-keys`, never another
  `:read/...` key written by hand), live (not a tombstone), admitted at or
  before `before`: `[[fid stamp e] ...]`."
  [ents before]
  (into [] (keep (fn [[_ e]]
                   (when (and (map? e) (contains? reads/read-keys (:k e)) (nil? (:erased-at e))
                              (int? (:stamp e)) (or (nil? before) (<= (:stamp e) before)))
                     [(:fid e) (:stamp e) (:e e)])))
        ents))

(defn ids-page
  "`entry-ids`' answer: the ids, the cursor past the last entry scanned, and
  whether the `read/` range ran out."
  [ids ents0 ents n after]
  {:ids ids
   :next (or (first (peek ents)) after)
   :done? (boolean (or (< (count ents0) n) (< (count ents) (count ents0))))})

(deframafn line-value>
  "The value of an entry's last line under key `*k` (a tail read of
  `:ix-ek` below its prefix, its row, its open): nil when there is none."
  [*layer *ent *k]
  (<<with-substitutions [$$layers (this-module-pobject-task-global "$$layers")]
    (line-prefix *ent *k :> *pfx)
    (reads/prefix-end *pfx :> *end)
    (local-select> [(keypath *layer :ix-ek) (sorted-map-range-to *end {:max-amt 1})] $$layers :> *sub)
    (under-prefix *sub *pfx :> *entry)
    (<<if (nil? *entry)
      (:> nil)
     (else>)
      (get *entry :fid :> *fid)
      (fid-parts *fid :> [*nm *idx])
      (local-select> [(keypath *layer :log *nm *idx)] $$layers :> *row)
      (reads/open-row> *layer *fid *row (get *entry :stamp) nil :> *o)
      (:> (get *o :value)))))

(deframafn prefix-entries>
  "Every `:ix-ke` entry of a layer under a key's prefix, in doubling pages
  (a maintenance read: ids and stamps are what its callers use)."
  [*layer *prefix]
  (<<with-substitutions [$$layers (this-module-pobject-task-global "$$layers")]
    (reads/prefix-end *prefix :> *end)
    (loop<- [*from *prefix *page 16 *acc [] :> *all]
      (yield-if-overtime)
      (local-select> [(keypath *layer :ix-ke) (sorted-map-range-from *from *page)] $$layers {:allow-yield? true} :> *sub)
      (reads/page-entries *sub :> *ents)
      (below *end *ents :> *ins)
      (<<if (or> (< (count *ents) *page) (< (count *ins) (count *ents)))
        (:> (into *acc *ins))
       (else>)
        (continue> (str (first (peek *ents)) sep) (min 4096 (* 2 *page)) (into *acc *ins))))
    (:> *all)))

(deframaop standing-open>
  "RS5: the standing reads of a working layer left open (an opening fact and
  no closing fact about the same entry entity), those of `*session` when it
  is given (the opening act's record names its session). Entities only."
  [*layer *session]
  (<<with-substitutions [$$layers (this-module-pobject-task-global "$$layers")]
    (prefix-entries> *layer (key-prefix :read/standing) :> *opens)
    (<<if (empty? *opens)
      (:> [])
     (else>)
      (prefix-entries> *layer (key-prefix :read/closed) :> *closes)
      (open-entities *opens *closes :> *cands)
      (loop<- [*t *cands *acc [] :> *out]
        (<<if (empty? *t)
          (:> *acc)
         (else>)
          (first *t :> [*ent *fid])
          (<<if (nil? *session)
            (continue> (rest *t) (conj *acc *ent))
           (else>)
            (local-select> [(keypath *layer :answers (first *fid) :session)] $$layers :> *s)
            (<<if (= *s *session)
              (continue> (rest *t) (conj *acc *ent))
             (else>)
              (continue> (rest *t) *acc)))))
      (:> *out))))

(deframaop entry-ids>
  "RS6, one page of a working layer's read-entry facts for a drop: the ids,
  stamps and entities of the live `:read/*` facts (the five store keys)
  admitted at or before `*before` whose act names `*session` (its record's
  session), from one `:ix-ke` page of `*n` entries of the `read/` range
  after the cursor. Nothing is opened (a maintenance read, RR11)."
  [*layer *session *before *after *n]
  (<<with-substitutions [$$layers (this-module-pobject-task-global "$$layers")]
    (<<if (nil? *after)
      (local-select> [(keypath *layer :ix-ke) (sorted-map-range-from "read/" *n)] $$layers {:allow-yield? true} :> *sub)
     (else>)
      (local-select> [(keypath *layer :ix-ke) (sorted-map-range-from *after (reads/after-opts *n))] $$layers
                     {:allow-yield? true} :> *sub))
    (reads/page-entries *sub :> *ents0)
    (below "read0" *ents0 :> *ents)
    (id-candidates *ents *before :> *cands)
    (loop<- [*ct *cands *ids [] *cc {} :> *out]
      (<<if (empty? *ct)
        (:> *ids)
       (else>)
        (first *ct :> [*cfid *cstamp *ce])
        (first *cfid :> *cnm)
        (<<cond
          (case> (nil? *session))
          (identity nil :> *cs)
          (identity *cc :> *cc2)

          (case> (contains? *cc *cnm))
          (get *cc *cnm :> *cs)
          (identity *cc :> *cc2)

          (default>)
          (local-select> [(keypath *layer :answers *cnm :session)] $$layers :> *cs)
          (assoc *cc *cnm *cs :> *cc2))
        (<<if (= *cs *session)
          (continue> (rest *ct) (conj *ids [*cfid *cstamp *ce]) *cc2)
         (else>)
          (continue> (rest *ct) *ids *cc2))))
    (:> (ids-page *out *ents0 *ents *n *after))))

;; ------------------------------------------------ the maintenance reads' micro era
;; The spec fixes (H-1). A working layer re-classed by entity keeps its later read
;; entries, standing-read lines and session closes in the micro store: their index
;; entries on the layer's task (`$$micro [W :ix-*]`, written by block 2d), their rows
;; and lock rows on each entry entity's task, their records on their name's task. The
;; reads below cover that era beside the stream era's above, settled: only entries
;; decided in a batch at or below the frontier on the layer's task, which every task
;; has committed (SPEC 'What Rama showed' 5 and 7).

(defn settled-at?
  "Whether a micro index entry was decided in a batch at or below `F`."
  [entry F]
  (boolean (and (map? entry) (int? (:batch entry)) (int? F) (<= (:batch entry) F))))

(defn line-or
  "A line's value from the micro era when it has one, else the stream era's."
  [micro stream]
  (if (some? micro) micro stream))

(defn last-settled-under
  "The last entry of a tail page whose address starts with `prefix` and
  whose batch is settled at `F`, or nil: a line decided in a batch not yet
  committed on every task is not read."
  [sub prefix F]
  (->> (reads/page-entries sub)
       (filter (fn [[a e]] (and (string? a) (.startsWith ^String a ^String prefix) (settled-at? e F))))
       last
       second))

(deframafn micro-line-entry>
  "The micro era's last settled line of entry `*ent` under key `*k`, on the
  layer's task: its `$$micro [W :ix-ek]` entry (fact id, stamp, batch; a
  read line is `:no-copy`, so no value), or nil. One tail read: a standing
  read's lines are offered one after another, so at most one is unsettled."
  [*layer *ent *k *F]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")]
    (line-prefix *ent *k :> *pfx)
    (reads/prefix-end *pfx :> *end)
    (local-select> [(keypath *layer :ix-ek) (sorted-map-range-to *end {:max-amt 2})] $$micro :> *sub)
    (:> (last-settled-under *sub *pfx *F))))

(deframafn micro-line-value>
  "The value of a micro-era line on its entity's task, where its row is:
  the row, its lock (the row's record, else its lock row: a line is
  `:own-row`), the ledger entry of that lock, the persons its wrap names,
  then phase 2's `locks/open-with`, as `open-row>` opens a stream-era line.
  Nil for no entry, or a line that no longer opens."
  [*ent *entry]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")]
    (<<if (nil? *entry)
      (:> nil)
     (else>)
      (fid-parts (get *entry :fid) :> [*nm *idx])
      (local-select> [(keypath *ent :log *nm *idx)] $$micro :> *row)
      (get *row :lock-id :> *lid)
      (<<if (some? *lid)
        (local-select> [(keypath *ent :erased *lid)] $$micro :> *ledger)
        (<<if (nil? (get *row :lock))
          (local-select> [(keypath *ent :locks *lid)] $$micro :> *lrow)
         (else>)
          (identity nil :> *lrow))
       (else>)
        (identity nil :> *ledger)
        (identity nil :> *lrow))
      (record-of *row *lrow :> *record)
      (locks/read-persons> (locks/wrap-persons-of *record) {} :> *rp)
      (:> (get (locks/open-with *row (get *entry :stamp) nil *ledger *record *rp) :value)))))

(deframaop standing-close>
  "RS4, a standing read's closing values (F3): its last delivery line's
  running value (one tail read, its row, its lock row and open: about four
  seeks whatever the number of lines), else the seed's; with its opening
  line's layer. Computed in the module, where the fingerprint secret is; not
  itself recorded (RR11): its output is written into the closing line.

  The spec fixes (H-1): when the working layer is re-classed by entity
  (its stream settings say so; one more seek), the micro era's last
  delivery and opening lines are read too (two tail reads on this task,
  then, when either is there, one hop to the entry entity's task to open
  them), and each kind's micro-era line, which is always the later, is
  taken over the stream era's."
  [*layer *ent]
  (<<with-substitutions [$$layers (this-module-pobject-task-global "$$layers")
                         $$micro-task (this-module-pobject-task-global "$$micro-task")]
    (line-value> *layer *ent :read/delivery :> *d)
    (line-value> *layer *ent :read/standing :> *o)
    (local-select> [(keypath *layer :settings :class)] $$layers :> *class)
    (<<if (not= :by-entity *class)
      (:> (closing *d *o))
     (else>)
      (local-select> [(keypath :frontier)] $$micro-task :> *F)
      (micro-line-entry> *layer *ent :read/delivery *F :> *mde)
      (micro-line-entry> *layer *ent :read/standing *F :> *moe)
      (<<if (and> (nil? *mde) (nil? *moe))
        (:> (closing *d *o))
       (else>)
        (|hash *ent)
        (micro-line-value> *ent *mde :> *md)
        (micro-line-value> *ent *moe :> *mo)
        (:> (closing (line-or *md *d) (line-or *mo *o)))))))

(defn micro-id-candidates
  "The micro era's read-entry facts of a page (`reads/read-keys`), live,
  decided in a batch at or below `before` (a close's batch) and settled at
  `F`: `[[fid stamp e] ...]`."
  [ents before F]
  (into [] (keep (fn [[_ e]]
                   (when (and (map? e) (contains? reads/read-keys (:k e)) (nil? (:erased-at e))
                              (int? (:stamp e)) (int? before) (settled-at? e F) (<= (:batch e) before))
                     [(:fid e) (:stamp e) (:e e)])))
        ents))

(defn micro-ids-args
  "`micro-entry-ids`' arguments checked before any read keyed by them: the
  maintenance reads' own check, with the session and the batch bound
  required."
  [layer session before after n]
  (or (upkeep-args layer nil session before after n)
      (when-not (and (env/readable-keyword? session) (int? before) (int? n)) {:refused :bad-read})))

(defn micro-id-rows
  "`micro-entry-ids`' rows before its session check: the page first, then
  each candidate with its place in the page and its act's name."
  [ents0 ents n after cands]
  (into [{:kind :page :page (ids-page [] ents0 ents n after)}]
        (map-indexed (fn [i [fid :as c]] {:kind :cand :cand c :i i :nm (into [] (nth fid 0))}))
        cands))

(defn micro-ids-answer
  "`micro-entry-ids`' answer from its gathered rows: a refusal, or the page
  (`:next`, `:done?`) with the ids whose act names `session`, in page
  order."
  [rows session]
  (let [rows (vec rows)]
    (if-let [r (some #(when (= :refused (:kind %)) %) rows)]
      {:refused (:reason r)}
      (let [page (or (some #(when (= :page (:kind %)) (:page %)) rows) {:next nil :done? true})]
        (assoc page :ids (->> rows
                              (filter #(and (= :cand (:kind %)) (some? session) (= session (:session %))))
                              (sort-by :i)
                              (mapv :cand)))))))

(deframaop micro-id-rows>
  "One page of a working layer's micro-era read entries, on the layer's
  task: `entry-ids>`' page over `$$micro [W :ix-ke]`'s `read/` range, the
  candidates for its query to check each one's session where its record
  is. `[{:kind :refused ...}]` for bad arguments."
  [*layer *session *before *after *n]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")
                         $$micro-task (this-module-pobject-task-global "$$micro-task")]
    (<<if (some? (micro-ids-args *layer *session *before *after *n))
      (:> [{:kind :refused :reason :bad-read}])
     (else>)
      (local-select> [(keypath :frontier)] $$micro-task :> *F)
      (<<if (nil? *after)
        (local-select> [(keypath *layer :ix-ke) (sorted-map-range-from "read/" *n)] $$micro {:allow-yield? true} :> *sub)
       (else>)
        (local-select> [(keypath *layer :ix-ke) (sorted-map-range-from *after (reads/after-opts *n))] $$micro
                       {:allow-yield? true} :> *sub))
      (reads/page-entries *sub :> *ents0)
      (below "read0" *ents0 :> *ents)
      (micro-id-candidates *ents *before *F :> *cands)
      (:> (micro-id-rows *ents0 *ents *n *after *cands)))))

(deframafn micro-prefix-entries>
  "Every `$$micro [W :ix-ke]` entry under a key's prefix, in doubling pages,
  as `prefix-entries>` reads the stream era's."
  [*layer *prefix]
  (<<with-substitutions [$$micro (this-module-pobject-task-global "$$micro")]
    (reads/prefix-end *prefix :> *end)
    (loop<- [*from *prefix *page 16 *acc [] :> *all]
      (yield-if-overtime)
      (local-select> [(keypath *layer :ix-ke) (sorted-map-range-from *from *page)] $$micro {:allow-yield? true} :> *sub)
      (reads/page-entries *sub :> *ents)
      (below *end *ents :> *ins)
      (<<if (or> (< (count *ents) *page) (< (count *ins) (count *ents)))
        (:> (into *acc *ins))
       (else>)
        (continue> (str (first (peek *ents)) sep) (min 4096 (* 2 *page)) (into *acc *ins))))
    (:> *all)))

(defn micro-open-rows
  "`micro-standing-open`'s rows before its session check: each settled
  opening line the micro era did not close, `{:kind :open :ent e :nm
  name}`, then each entity the micro era closed, `{:kind :closed :ent e}`
  (a close there may end an entry the stream era opened)."
  [opens closes F]
  (let [settled (fn [xs] (filter (fn [[_ e]] (settled-at? e F)) xs))
        closed (into #{} (map (comp :e second)) (settled closes))]
    (-> []
        (into (comp (map second)
                    (remove #(contains? closed (:e %)))
                    (map (fn [e] {:kind :open :ent (:e e) :nm (into [] (nth (:fid e) 0))})))
              (settled opens))
        (into (map (fn [ent] {:kind :closed :ent ent})) (sort-by str closed)))))

(defn micro-open-answer
  "`micro-standing-open`'s answer: `{:open [ent ...] :closed [ent ...]}`,
  the open ones those whose opening act names `session` (every one when
  nil), or a refusal."
  [rows session]
  (let [rows (vec rows)]
    (if-let [r (some #(when (= :refused (:kind %)) %) rows)]
      {:refused (:reason r)}
      {:open (into [] (comp (filter #(and (= :open (:kind %)) (or (nil? session) (= session (:session %)))))
                            (map :ent) (distinct))
                   rows)
       :closed (into [] (comp (filter #(= :closed (:kind %))) (map :ent) (distinct)) rows)})))

(deframaop micro-open-rows>
  "The micro era's opening and closing lines of a working layer, on the
  layer's task, as `micro-open-rows`. `[{:kind :refused ...}]` for bad
  arguments."
  [*layer *session]
  (<<with-substitutions [$$micro-task (this-module-pobject-task-global "$$micro-task")]
    (<<if (some? (upkeep-args *layer nil *session nil nil 1))
      (:> [{:kind :refused :reason :bad-read}])
     (else>)
      (local-select> [(keypath :frontier)] $$micro-task :> *F)
      (micro-prefix-entries> *layer (key-prefix :read/standing) :> *opens)
      (micro-prefix-entries> *layer (key-prefix :read/closed) :> *closes)
      (:> (micro-open-rows *opens *closes *F)))))

;; ============================================================ install

(defn declare-queries!
  "This stage's query topologies (PLAN-reads-rest.md, 'Query Topologies'),
  each on one task: the shared reads the read exit's two queries hand a
  layer of the shared kind to, `shared-read-pattern` and
  `shared-read-point` (with the stream settings the caller read); a
  standing read's `read-delta` (its opening read through `read-pattern`,
  then deltas); the maintenance reads `standing-close`, `standing-open` and
  `entry-ids` on the working layer's home; and the operator's
  `micro-index-progress` and `task-layers` by task."
  [topologies]
  #_:clj-kondo/ignore
  (<<query-topology topologies "shared-read-pattern" [*layer *for *pattern *as-of *limit *ss :> *answer]
    (|hash *layer)
    (shared-pattern> *layer *for *pattern *as-of *limit *ss :> *answer)
    (|origin))
  #_:clj-kondo/ignore
  (<<query-topology topologies "shared-read-point" [*layer *for *fids *as-of *ss :> *answer]
    (|hash *layer)
    (shared-point> *layer *for *fids *as-of *ss :> *answer)
    (|origin))
  #_:clj-kondo/ignore
  (<<query-topology topologies "read-delta" [*layer *for *pattern *limit *scan *prev :> *answer]
    (|hash *layer)
    (<<cond
      (case> (some? (delta-args *layer *for *scan)))
      (delta-args *layer *for *scan :> *answer)

      (case> (nil? *scan))
      (invoke-query "read-pattern" *layer *for *pattern nil *limit :> *a)
      (opening-answer *a *pattern *limit *prev :> *answer)

      (default>)
      (delta> *layer *for *pattern *limit *scan *prev :> *answer))
    (|origin))
  #_:clj-kondo/ignore
  (<<query-topology topologies "standing-close" [*layer *ent :> *answer]
    (|hash *layer)
    (<<if (some? (upkeep-args *layer *ent nil nil nil 1))
      (identity {:refused :bad-read} :> *answer)
     (else>)
      (standing-close> *layer *ent :> *answer))
    (|origin))
  #_:clj-kondo/ignore
  (<<query-topology topologies "standing-open" [*layer *session :> *answer]
    (|hash *layer)
    (<<if (some? (upkeep-args *layer nil *session nil nil 1))
      (identity {:refused :bad-read} :> *answer)
     (else>)
      (standing-open> *layer *session :> *answer))
    (|origin))
  #_:clj-kondo/ignore
  (<<query-topology topologies "entry-ids" [*layer *session *before *after *n :> *answer]
    (|hash *layer)
    (<<if (or> (nil? *n) (some? (upkeep-args *layer nil *session *before *after *n)))
      (identity {:refused :bad-read} :> *answer)
     (else>)
      (entry-ids> *layer *session *before *after *n :> *answer))
    (|origin))
  ;; the spec fixes (H-1): the maintenance reads' micro era. Each reads the layer's
  ;; index on its task, then checks each candidate's act session where its record is
  ;; (a hop per candidate: a micro index entry carries no session), then gathers.
  #_:clj-kondo/ignore
  (<<query-topology topologies "micro-entry-ids" [*layer *session *before *after *n :> *answer]
    (|hash *layer)
    (micro-id-rows> *layer *session *before *after *n :> *rows)
    (ops/explode *rows :> *row0)
    (<<if (= :cand (get *row0 :kind))
      (|hash (get *row0 :nm))
      (local-select> [(keypath (get *row0 :nm) :answer :session)] $$micro-names :> *s)
      (assoc *row0 :session *s :> *row)
     (else>)
      (identity *row0 :> *row))
    (|origin)
    (aggs/+vec-agg *row :> *all)
    (micro-ids-answer *all *session :> *answer))
  #_:clj-kondo/ignore
  (<<query-topology topologies "micro-standing-open" [*layer *session :> *answer]
    (|hash *layer)
    (micro-open-rows> *layer *session :> *rows)
    (ops/explode *rows :> *row0)
    (<<if (and> (= :open (get *row0 :kind)) (some? *session))
      (|hash (get *row0 :nm))
      (local-select> [(keypath (get *row0 :nm) :answer :session)] $$micro-names :> *s)
      (assoc *row0 :session *s :> *row)
     (else>)
      (identity *row0 :> *row))
    (|origin)
    (aggs/+vec-agg *row :> *all)
    (micro-open-answer *all *session :> *answer))
  #_:clj-kondo/ignore
  (<<query-topology topologies "micro-index-progress" [*task :> *answer]
    (|direct *task)
    (local-select> [(keypath :rebuild)] $$micro-task :> *r)
    (local-select> [(keypath :frontier)] $$micro-task :> *f)
    (hash-map :progress *r :frontier *f :> *answer)
    (|origin))
  #_:clj-kondo/ignore
  (<<query-topology topologies "task-layers" [*task :> *answer]
    (|direct *task)
    (local-select> (subselect MAP-KEYS) $$layers {:allow-yield? true} :> *sl)
    (local-select> [(keypath :layers) (subselect ALL)] $$micro-task {:allow-yield? true} :> *ml)
    (hash-map :stream *sl :micro *ml :> *answer)
    (|origin)))

(defn declare-depots!
  "The operator depot for the micro store's index maintenance (RR8): every
  record names its task and is routed there, so `:random`. Not an act and
  not in the log."
  [setup]
  (declare-depot setup *micro-index-ops :random))
