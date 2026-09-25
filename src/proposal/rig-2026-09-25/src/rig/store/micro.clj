;; IMPORTANT: Before modifying this file, re-read PLAN-micro-store.md (its
;; "Revision, 26 September", §A to §J, and the [PV-F n] fixes) and
;; BUILD_NOTES-micro-store.md. Adhere to all previously decided design
;; decisions; RIG.md R19 builds the permission walk, not the cascade.
(ns rig.store.micro
  "The micro store (stage 3, PLAN-micro-store.md): a microbatch gate for the
  shared layers (group, base) and for one-owner layers re-classed to by
  entity, declared into the one module by one call, `(declare! setup
  topologies)` (M1).

  One batch, block by block (each a `<<batch`, a global barrier):
  0. every task writes the frontier, the previous batch's id (M7, §D);
  1. the gather: each offer's lock work on its arrival task, where its
     lease rows are (§A, M17: unlease, open, the value checks, the
     subjects), then its name row, its layer's settings, permissions and
     stream-era heads, and its entities' clocks and heads, all as
     `[need found]` rows into one `+map-agg` on task 0, where `prepare`,
     the pure fold over skeletons, decides the batch in one order (M2, M3);
  2a. the fold's writes (records, faces, heads, settings, permissions,
     clocks, and the lease rows a yes lease act mints);
  2b. the rows, from the offers themselves: re-wrapped locks and value
     digests made on the arrival task, written on each fact's entity task
     for an act decided yes in this batch with this envelope (M10, [PV-F3]);
  2c. the leases consumed at decision, and a closed session's (M19, [PV-F4]).

  A lock never leaves the task it is read on and a plaintext value never
  leaves the task it is opened on; the leader sees digests and ids only.
  Every function here that topology code calls is total: a refusal is data,
  never an exception (SPEC 'What Rama showed' 3; a microbatch that throws
  deterministically retries for ever).

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.aggs :as aggs]
            [com.rpl.rama.ops :as ops]
            [clojure.string :as str]
            [rig.store.envelope :as env]
            [rig.store.gate :as gate]
            [rig.store.grammar :as grammar]
            [rig.store.inject :as inject]
            [rig.store.locks :as locks]
            [rig.store.permit :as permit])
  (:import [java.util HexFormat UUID]))

;; ================================================================ constants

(def control-keys
  "Fact keys the store acts on: no lock, a plaintext value (phase 2's list,
  D2, L14, and `:members`, the making fact of a group at this gate)."
  #{:forget :lock-grain :class :promote-request :crossed :permission :revoke
    :kind :owner :person :forget-person :lease :session-closed :members})

(def foreign-control-keys
  "Control keys that belong to another gate's layers (the `:people` layer's
  person acts, stage 4's promotion): refused `:control-not-allowed` here."
  #{:person :forget-person :promote-request :crossed})

(def max-lease "A lease mints 1 to 256 locks (M24, phase 2's L20)." 256)

(def reason-order
  "Every recorded reason, in the one order both gates place them: stage 1's
  list (I-G5 with P7, [F8] and R13's placed as the stream plan says), then
  phase 2's L27 value and person reasons. The micro gate reuses
  `gate/decide` for stage 1's list and places its own inputs' reasons
  (the value checks it cannot run on the leader, this gate's control facts)
  by this order, so the gates cannot drift ([PV-F13])."
  [:fact-outside-the-acts-layer :no-such-layer :class-mismatch
   :permission-does-not-cover-this :permission-from-another-layer
   :no-permission :permission-revoked
   :malformed-control :control-not-allowed :stale-replaces :stale-revoke
   :layer-already-made :unsupported-reclass
   :does-not-open :malformed-value :value-shape :too-many-subjects
   :grain-mismatch :no-such-person :person-forgotten])

(def ^:private reason-rank (zipmap reason-order (range)))

(defn first-in-order
  "The first of `reasons` (nils ignored) in `reason-order`."
  [reasons]
  (first (sort-by #(get reason-rank % Long/MAX_VALUE) (remove nil? reasons))))

;; ================================================================== schemas

(def lock-record-schema
  "Phase 2's lock record, shared by both stores (M12)."
  (fixed-keys-schema {:required  clojure.lang.PersistentVector
                      :any-of    clojure.lang.PersistentVector
                      :blob      byte/1
                      :any-blobs (map-schema clojure.lang.Keyword byte/1)}))

(def answer-record-schema
  "The stream plan's answer record plus `:batch`, the microbatch that
  decided it (M7)."
  (fixed-keys-schema {:answer       clojure.lang.Keyword
                      :reason       clojure.lang.Keyword
                      :stamp        Long
                      :digest       String
                      :who          clojure.lang.Keyword
                      :class        clojure.lang.Keyword
                      :permission   clojure.lang.PersistentVector
                      :session      clojure.lang.Keyword
                      :because-of   clojure.lang.PersistentVector
                      :claimed-when Long
                      :subjects     (set-schema clojure.lang.Keyword)
                      :batch        Long}))

(def ^:private sub {:subindex-options {:track-size? false}})

(def micro-schema
  "`$$micro`: everything keyed by an entity, on the entity's task; a layer
  id doubles as the entity its projections live under (PLAN, `$$micro`)."
  {clojure.lang.Keyword
   (fixed-keys-schema
    {:log         (map-schema clojure.lang.PersistentVector                 ; name
                              (map-schema Long                               ; idx -> row
                                          (fixed-keys-schema
                                           {:layer    clojure.lang.Keyword
                                            :k        clojure.lang.Keyword
                                            :v        String                 ; canonical EDN of a control fact; nil for a value and a retract
                                            :sealed   byte/1                 ; phase 2's: the sealed bytes as offered
                                            :replaces clojure.lang.PersistentVector
                                            :mark     (set-schema clojure.lang.Keyword)
                                            :lock-id  clojure.lang.PersistentVector ; [lease-name i]
                                            :lock     lock-record-schema     ; the wrapped lock, kept in the record (ruling 7)
                                            :digest   byte/1})               ; the value digest (R1, phase 2's L26)
                                          sub)
                              sub)
     :heads       (map-schema clojure.lang.PersistentVector                 ; [layer k fid]
                              (fixed-keys-schema {:stamp          Long
                                                  :batch          Long
                                                  :replaced-by    clojure.lang.PersistentVector
                                                  :replaced-batch Long})
                              sub)
     :answers     (map-schema clojure.lang.PersistentVector answer-record-schema sub)
     :locks       (map-schema clojure.lang.PersistentVector lock-record-schema sub) ; lock id -> lock row
     :erased      (map-schema clojure.lang.PersistentVector                 ; lock id -> the ledger entry (phase 2's)
                              (fixed-keys-schema {:stamp Long :how clojure.lang.Keyword})
                              sub)
     :settings    (map-schema Long                                          ; batch -> settings in force from it
                              (fixed-keys-schema {:kind  clojure.lang.Keyword
                                                  :owner clojure.lang.Keyword
                                                  :class clojure.lang.Keyword
                                                  :grain clojure.lang.Keyword
                                                  :batch Long})
                              sub)
     :members     (map-schema clojure.lang.Keyword Long sub)                ; person -> the batch that named them
     :permissions (map-schema clojure.lang.PersistentVector                 ; pid
                              (fixed-keys-schema {:granted       clojure.lang.PersistentVector
                                                  :granted-batch Long
                                                  :revoked       clojure.lang.PersistentVector
                                                  :revoked-batch Long})
                              sub)
     :replaced    (map-schema clojure.lang.PersistentVector                 ; [e k fid] of a stream-era head
                              (fixed-keys-schema {:by clojure.lang.PersistentVector :batch Long})
                              sub)})})

(def names-schema
  "`$$micro-names`: everything keyed by a name, on the name's task."
  {clojure.lang.PersistentVector
   (fixed-keys-schema
    {:answer   answer-record-schema                                         ; the name row (I-G4)
     :fp       String                                                       ; [PV-F3] the decided envelope's fingerprint
     :faces    (map-schema String                                           ; envelope fp -> a face refusal (M8)
                           (fixed-keys-schema {:reason clojure.lang.Keyword :batch Long})
                           sub)
     :stood-on (map-schema clojure.lang.PersistentVector Long sub)          ; fid -> stamp as carried ([F2])
     :leases   (map-schema Long                                             ; i -> a lease row [PV-F1], M16
                           (fixed-keys-schema {:under   clojure.lang.Keyword ; the lease act's :who; nil for the operator
                                               :sealed  byte/1               ; K sealed under :under's person lock; K bare when :under is nil
                                               :layer   clojure.lang.Keyword
                                               :session clojure.lang.Keyword
                                               :kind    clojure.lang.Keyword ; the layer's kind at the lease (rig choice)
                                               :owner   clojure.lang.Keyword ; the layer's person owner at the lease (rig choice)
                                               :batch   Long})
                           sub)})})

(def task-schema
  "`$$micro-task`: the task's clock (a hybrid stamp) and the frontier."
  (fixed-keys-schema {:clock Long :frontier Long}))

(def persons-schema
  "Phase 2's `$$persons` schema: every person's lock, on every task."
  {clojure.lang.Keyword (fixed-keys-schema {:lock byte/1 :erased-at Long})})

;; ============================================================ small helpers

(def ^:private ^HexFormat hexf (HexFormat/of))

(defn hex "Bytes as lower-case hex." [^bytes b] (.formatHex hexf b))

(defn wall
  "The leader's wall clock in milliseconds, read once per batch (§E; the
  gate's own simulated clock in tests)."
  []
  (gate/wall-now))

(defn frontier-of
  "The frontier a read uses: the caller's F when given, else the reading
  task's own (-1 before the first batch)."
  [f own]
  (if (some? f) f (or own -1)))

(defn visible-at?
  "The one reader's rule (§D): a row written by batch b is shown at
  frontier F iff b <= F."
  [row F]
  (boolean (and (some? row) (some? (:batch row)) (some? F) (<= (:batch row) F))))

(defn keep-first
  "A faces entry is written keep-first ([PV-F10]): the entry already there
  stays, so its batch never moves above a reader's F."
  [new old]
  (if (some? old) old new))

(defn- uuid-of [nm] (nth nm 3))

;; ======================================================== the sealed envelope

(defn- fp-form
  "The envelope as canonical data for its fingerprint: byte arrays as their
  hex, tagged so no EDN value collides with them."
  [x]
  (cond
    (bytes? x) [::bytes (hex x)]
    (map? x) (into {} (map (fn [[k v]] [(fp-form k) (fp-form v)])) x)
    (vector? x) (mapv fp-form x)
    (set? x) (into #{} (map fp-form) x)
    (seq? x) (apply list (map fp-form x))
    :else x))

(defn envelope-fp
  "The envelope fingerprint (M8 revised): P6's keyed HMAC, the rig secret,
  over the canonical sealed envelope as sent, minus its name. Per attempt:
  a resend sealed again has its own. The door and the gate compute it from
  the same map. Total."
  [raw]
  (try (env/hmac-hex (env/canonical (fp-form (dissoc raw :name))))
       (catch Throwable _ "unreadable")))

(defn- norm-lock-id [lid] [(into [] (nth lid 0)) (long (nth lid 1))])

(defn- not-sealed?
  "Phase 2's L27 face: a non-control fact with a plaintext `:v`; a `:sealed`
  that is not bytes, or has no well-formed `:lock-id`, or sits beside a
  `:v`; a `:lock-id` with no `:sealed`; a control fact carrying either."
  [raw-fact parsed-fact]
  (let [sealed? (contains? raw-fact :sealed)
        lid? (contains? raw-fact :lock-id)]
    (cond
      (contains? control-keys (:k parsed-fact)) (or sealed? lid?)
      (or sealed? lid?) (or (not (bytes? (:sealed raw-fact)))
                            (not (locks/lock-id? (:lock-id raw-fact)))
                            (some? (:v raw-fact)))
      :else (some? (:v parsed-fact)))))

(defn parse-micro
  "The micro gate's parse (the adapter the plan names, 'What this stage
  takes'): stage 1's total `env/parse` on the `:micro` side over the
  envelope with each fact's sealed parts set aside, then phase 2's
  `:not-sealed` face, then the sealed parts put back, normalised: a value
  fact is `{:e :k :v nil :replaces :mark :sealed bytes :lock-id [n i]}`.
  {:ok offer} or {:refuse reason}. Total."
  [raw]
  (try
    (let [facts (when (map? raw) (:facts raw))
          split? (and (sequential? facts) (every? map? facts))
          p (env/parse (cond-> raw split? (assoc :facts (mapv #(dissoc % :sealed :lock-id) facts))) :micro)]
      (if (contains? p :refuse)
        p
        (let [o (:ok p)
              raws (vec facts)]
          (if (some (fn [[rf pf]] (not-sealed? rf pf)) (map vector raws (:facts o)))
            {:refuse :not-sealed}
            {:ok (assoc o :facts (mapv (fn [pf rf]
                                         (if (contains? rf :sealed)
                                           (assoc pf :sealed (:sealed rf) :lock-id (norm-lock-id (:lock-id rf)))
                                           pf))
                                       (:facts o) raws))}))))
    (catch Throwable _ {:refuse :malformed})))

(defn parts-digest
  "Phase 2's parts digest (L26, [PV-F2]): `env/digest` over the offer minus
  its name with every `:sealed` replaced by `true` and every `:lock-id`
  removed, so it holds no value and names no lock, and a resend sealed again
  under newly leased locks digests the same."
  [offer]
  (env/digest (update offer :facts (fn [fs] (mapv #(cond-> (dissoc % :lock-id) (contains? % :sealed) (assoc :sealed true)) fs)))))

(defn route-key
  "The micro depot's partitioner (M4 revised, §A): the lease name of the
  first lock any fact cites, else the first fact's entity, else nil (which
  hashes somewhere, where block 1 refuses the record). A top-level defn, so
  `hash-by` can name it; total."
  [raw]
  (try
    (let [facts (when (map? raw) (:facts raw))
          fs (when (sequential? facts) facts)
          lid (some #(when (and (map? %) (contains? % :sealed)) (:lock-id %)) fs)]
      (if (and (vector? lid) (= 2 (count lid)) (sequential? (nth lid 0)))
        (into [] (nth lid 0))
        (let [f (first fs)] (when (map? f) (:e f)))))
    (catch Throwable _ nil)))

;; ================================================================= intake

(defn value-fact? "A sealed value fact." [f] (locks/sealed? f))

(defn lease-act?
  "A lease act's one control fact (M24, phase 2's L20): `{:e s :k :lease
  :v {:count n}}`."
  [offer]
  (let [fs (:facts offer)] (and (= 1 (count fs)) (= :lease (:k (first fs))))))

(defn close-act?
  "A session close's one control fact ([PV-F4], phase 2's L28)."
  [offer]
  (let [fs (:facts offer)] (and (= 1 (count fs)) (= :session-closed (:k (first fs))))))

(defn- lease-count [f] (let [v (:v f)] (when (map? v) (:count v))))

(defn lease-ok?
  "A well-formed lease act: its one fact on the offer's own `:session`,
  `{:count n}` with n a long in 1..256."
  [offer]
  (let [f (first (:facts offer)) n (lease-count f)]
    (boolean (and (lease-act? offer) (some? (:session offer)) (= (:session offer) (:e f))
                  (map? (:v f)) (= #{:count} (set (keys (:v f)))) (int? n) (<= 1 n max-lease)))))

(defn close-ok?
  "A well-formed session close: `{:e s :k :session-closed :v {:session s}}`."
  [offer]
  (let [f (first (:facts offer)) v (:v f)]
    (boolean (and (close-act? offer) (map? v) (= #{:session} (set (keys v)))
                  (env/readable-keyword? (:session v)) (= (:session v) (:e f))))))

(defn skeleton-offer
  "The offer as it may travel to the leader: every sealed value's bytes
  replaced by the marker `true` (its lock id kept); control values stay,
  they are ids and settings, not the store's values (D2)."
  [offer]
  (update offer :facts (fn [fs] (mapv #(cond-> % (contains? % :sealed) (assoc :sealed true)) fs))))

(defn intake
  "Block 1's first step on a raw depot record, pure and total. nil for a
  record with no readable name (nobody can ask for it); a face
  `{:kind :face :name :fp :reason :rows}`; else `{:kind :offer ...}`: the
  parsed offer (sealed bytes still on its facts, for the arrival task), the
  parts digest, the envelope fingerprint, the cited lock ids and their
  lease name (the route key), the entities, and a session close's
  session."
  [raw]
  (try
    (when-let [nm (env/name-of raw)]
      (let [fp (envelope-fp raw)
            p (parse-micro raw)]
        (if-let [r (:refuse p)]
          {:kind :face :name nm :fp fp :reason r :rows [[[:face nm fp] r]]}
          (let [o (:ok p)
                cited (locks/cited-ids (:facts o))]
            {:kind :offer
             :name nm
             :fp fp
             :offer o
             :digest (parts-digest o)
             :cited cited
             :lease-name (some-> (first cited) locks/lease-name-of)
             :entities (into [] (comp (map :e) (distinct)) (:facts o))
             :close (when (close-ok? o) (:e (first (:facts o))))}))))
    (catch Throwable _
      (when-let [nm (env/name-of raw)]
        (let [fp (envelope-fp raw)]
          {:kind :face :name nm :fp fp :reason :gate-error :rows [[[:face nm fp] :gate-error]]})))))

;; ===================================================== block 1, the arrival

(defn lease-keys
  "The lease rows block 1 may read on the arrival task: every cited id under
  the route's lease name (M24: all the locks an act cites come from one
  lease; an id of another lease is on another task and counts as missing)."
  [in]
  (let [ln (:lease-name in)]
    (into [] (filter #(= ln (locks/lease-name-of %))) (:cited in))))

(defn unders
  "The persons whose locks seal the lease rows read (none for a bare row)."
  [lrows]
  (into [] (comp (keep :under) (distinct)) (vals lrows)))

(defn- present-rows [in lrows] (into {} (filter (comp some? val)) lrows))

(defn owned-ids
  "The cited ids whose rows are present and leased to this offer's session
  in its layer: the only rows it may consume ([PV-F2], M19)."
  [in lrows]
  (let [o (:offer in) s (:session o) L (:layer o)]
    (into [] (filter (fn [lid] (let [row (get lrows lid)]
                                 (and row (= s (:session row)) (= L (:layer row))))))
          (:cited in))))

(defn delivered-locks
  "{lock-id K} for each owned id whose row unleases (phase 2's pure half of
  the delivery, `locks/unlease`)."
  [in lrows persons]
  (into {} (keep (fn [lid] (let [row (get lrows lid)
                                 K (locks/unlease row (get persons (:under row)))]
                             (when K [lid K]))))
        (owned-ids in lrows)))

(defn lease-meta
  "The layer's kind and person owner, as the act's lease recorded them (a
  rig choice: block 1 and 2b need them on the arrival task, before the
  layer task is reached)."
  [lrows]
  (some-> (first (keep val lrows)) (select-keys [:kind :owner])))

(defn arrival-open
  "Block 1's lock work on the arrival task (§A, M17), pure: which cited ids
  are owned, whether every cited lock delivers, and, when they do, the
  value checks in L27's order over the opened values (phase 2's
  `locks/read-values`; the grain is the fold's, since the grain in force is
  on the layer's task) and the act's subject union. Nothing it returns
  opens a value: the plaintext and the locks stay here."
  [in lrows persons]
  (let [o (:offer in)
        cited (:cited in)
        owned (owned-ids in lrows)
        delivered (delivered-locks in lrows persons)
        missing (into [] (remove #(contains? delivered %)) cited)
        meta (lease-meta lrows)]
    (if (seq missing)
      {:status :missing :owned owned :missing missing}
      (let [rv (when (seq cited)
                 (locks/read-values (:facts o) delivered
                                    {:owner (:owner meta) :carried (:subjects o) :grain :per-value}))
            r (:reason rv)]
        {:status :ok
         :owned owned
         :value-reason (when-not (= :grain-mismatch r) r)
         :union (when (seq cited) (:union rv))
         :kind (:kind meta)
         :owner (:owner meta)}))))

(defn persons-to-check
  "Whose person locks the decision needs alive (phase 2's L11): every wrap
  person of a value act (its subject union, which holds every value's own
  subjects), a lease act's `:who`. None for the operator."
  [in a]
  (let [o (:offer in)]
    (cond
      (lease-act? o) (if (contains? gate/exempt-actors (:who o)) [] [(:who o)])
      (and (= :ok (:status a)) (nil? (:value-reason a)) (seq (:cited in))) (vec (sort (:union a)))
      :else [])))

(defn person-reason
  "`:no-such-person` when a needed person has no entry, else
  `:person-forgotten` when one's lock is destroyed (L27's order)."
  [ps persons]
  (cond
    (some #(nil? (get persons %)) ps) :no-such-person
    (some #(nil? (:lock (get persons %))) ps) :person-forgotten))

(defn skeleton
  "What travels from the arrival task to the leader: never a value or a
  lock (M3, §A). The offer with its sealed bytes replaced by a marker, the
  parts digest, the fingerprint, the cited and owned ids, the lease status,
  the first value reason, the subject union, the person reason, and a
  session close's lease names."
  [in a preason close-names]
  (let [o (:offer in)]
    {:name (:name in)
     :fp (:fp in)
     :digest (:digest in)
     :offer (skeleton-offer o)
     :cited (:cited in)
     :owned (:owned a)
     :status (:status a)
     :value-reason (:value-reason a)
     :union (:union a)
     :person-reason preason
     :entities (:entities in)
     :close-names (when (:close in)
                    (into [] (filter #(= (:layer o) (nth % 0))) close-names))}))

;; ==================================================== block 1, the name task

(defn value-facts-of [sk] (into [] (keep-indexed (fn [i f] (when (value-fact? f) [i f]))) (:facts (:offer sk))))

(defn name-step
  "Block 1 on the name's task, pure: a decided name answers from its record
  whatever its leases (phase 2's order); else a missing lock is a face
  `:no-such-lock`; else the gather goes on to the layer's task. The paths:
  `:record` (the fold answers from the record), `:resend` (a yes record
  with values: each value is checked on its entity task first), `:face`,
  `:fresh`."
  [sk rec nt nclock]
  (let [nm (:name sk) fp (:fp sk)
        sk (assoc sk :name-task nt)]
    (cond
      (some? rec)
      (let [resend? (and (= (:digest sk) (:digest rec)) (= :yes (:answer rec)) (seq (value-facts-of sk)))]
        {:path (if resend? :resend :record)
         :sk sk
         :rows [[[:name nm] rec] [[:offer nm fp] (assoc sk :path :record)]]})

      (= :missing (:status sk))
      {:path :face :sk sk :rows [[[:face nm fp] :no-such-lock]]}

      :else
      {:path :fresh :sk (assoc sk :path :fresh) :rows [[[:clock nt] (or nclock 0)]]})))

;; =================================================== block 1, the layer task

(defn setting-fact-keys "Setting facts of the act (P10)." [o]
  (into [] (filter #(gate/setting-fact? o %)) (:facts o)))

(defn replacing-keys
  "[e k r] for each replacing fact of the act."
  [o]
  (into [] (comp (filter :replaces) (map (fn [f] [(:e f) (:k f) (:replaces f)])) (distinct)) (:facts o)))

(defn layer-rows
  "Block 1 on the layer's task, pure over what it read: the settings (micro
  first, `$$layers` on a miss), the permission rows of the chain and of
  the act's grant and revoke targets from both stores (M5), and a
  re-classed layer's stream-era heads and their tombstones. A setting fact
  for a layer whose settings the stream gate keeps is refused on its face
  `:wrong-gate` (P16, M25: its settings stay with the stream gate)."
  [sk msettings ssettings perms sheads tombs]
  (let [o (:offer sk) L (:layer o) nm (:name sk) fp (:fp sk)]
    (if (and (some? ssettings) (seq (setting-fact-keys o)))
      {:face true :rows [[[:face nm fp] :wrong-gate]]}
      {:face false
       :rows (-> [[[:settings L] {:micro msettings :stream ssettings}]]
                 (into (map (fn [[pid rows]] [[:perm L pid] rows])) perms)
                 (into (keep (fn [[hk v]] (when (some? v) [(into [:shead L] hk) v]))) sheads)
                 (into (keep (fn [[hk v]] (when (some? v) [(into [:tomb L] hk) v]))) tombs))})))

(defn micro-head-keys
  "The replacing facts on entity `e` whose head the entity task reads: those
  not found as a stream-era head on the layer's task."
  [sk e lrows]
  (let [o (:offer sk) L (:layer o)
        found (into #{} (keep (fn [[need _]] (when (= :shead (first need)) (subvec need 2)))) lrows)]
    (into [] (filter (fn [[fe _ _ :as hk]] (and (= e fe) (not (contains? found hk))))) (replacing-keys o))))

(defn entity-rows
  "Block 1 on an entity's task: its task id, its clock and the micro heads
  read; the first entity also carries the name's and layer's rows and the
  offer's own row, so each envelope's rows are emitted once."
  [sk e t clock heads first-rows first?]
  (let [o (:offer sk) L (:layer o) nm (:name sk) fp (:fp sk)
        mine (-> [[[:task-of e] t] [[:clock t] (or clock 0)]]
                 (into (keep (fn [[[he hk hr] v]] (when (some? v) [[:head L he hk hr] v]))) heads))]
    (if first?
      (-> mine (into first-rows) (conj [[:offer nm fp] sk]))
      mine)))

;; =========================================== block 1, the record path's check

(defn resend-check
  "The record path's value check for one value of a resend with a yes record
  ([PV-F2], M18), on the value's entity task: R, the recorded lock,
  unwrapped from the first act's row (nil when forgotten or closed by a
  person forget); O, the resend's own lock: R when it cites the row's lock
  id, else the carried lease row unleased here (nil when missing, not the
  offer's session or layer, or when it does not unlease). Both present: the
  resend's bytes are opened with O and the value digest recomputed under R
  (phase 2's `locks/check-resend`). `:recorded` or `:name-taken`."
  [f row lock-row carried-row persons session layer]
  (try
    (let [record (or (:lock row) lock-row)
          R (when record (locks/unwrap record persons))
          O (cond
              (= (:lock-id f) (:lock-id row)) R
              (and (some? carried-row) (= session (:session carried-row)) (= layer (:layer carried-row)))
              (locks/unlease carried-row (get persons (:under carried-row)))
              :else nil)]
      (if (= :name-taken (locks/check-resend [f] [row] {0 R} {0 O}))
        :name-taken
        :recorded))
    (catch Throwable _ :name-taken)))

(defn resend-persons
  "The persons a record path check reads on the entity task: the recorded
  lock's wrap persons and the carried lease row's `:under`."
  [row lock-row carried-row]
  (let [record (or (:lock row) lock-row)]
    (into [] (comp (remove nil?) (distinct))
          (concat (some-> record locks/wrap-persons) [(:under carried-row)]))))

;; ================================================================ the fold

(defn- envelopes
  "Every envelope of the batch, [name fp], in the batch order M2 with
  [PV-F3]'s tiebreak: [uuid7 name fp]."
  [state]
  (->> (keys state)
       (keep (fn [need] (when (#{:face :offer} (first need)) [(nth need 1) (nth need 2)])))
       (distinct)
       (sort-by (fn [[nm fp]] [(uuid-of nm) nm fp]))))

(defn- init-w
  "The fold's working state W, from the gathered rows (committed state)."
  [state]
  (reduce-kv
   (fn [w need found]
     (case (first need)
       :name (assoc-in w [:names (nth need 1)] {:record found :committed true})
       :settings (assoc-in w [:settings (nth need 1)] found)
       :perm (assoc-in w [:perms [(nth need 1) (nth need 2)]] found)
       :shead (assoc-in w [:sheads (subvec need 1)] found)
       :tomb (assoc-in w [:tombs (subvec need 1)] found)
       :head (assoc-in w [:heads (subvec need 1)] found)
       :clock (assoc-in w [:clocks (nth need 1)] found)
       :task-of (assoc-in w [:task-of (nth need 1)] found)
       :resend (assoc-in w [:resend [(nth need 1) (nth need 2)] (nth need 3)] found)
       :face (assoc-in w [:faces [(nth need 1) (nth need 2)]] found)
       :offer (assoc-in w [:offers [(nth need 1) (nth need 2)]] found)
       w))
   {:names {} :settings {} :perms {} :sheads {} :tombs {} :heads {} :clocks {} :task-of {}
    :resend {} :faces {} :offers {} :cited #{} :given {} :out {} :dels [] :mints []}
   state))

(defn- settings-in-force [w L]
  (let [s (get-in w [:settings L])] (or (:micro s) (:stream s))))

(defn- stream-held? [w L] (some? (:stream (get-in w [:settings L]))))

(defn- merged-perm
  "A permission's row as the check sees it (M5): granted in either store,
  revoked in either."
  [w L pid]
  (let [{:keys [micro stream]} (get-in w [:perms [L pid]])]
    {:granted (or (:granted micro) (:granted stream))
     :revoked (or (:revoked micro) (:revoked stream))}))

(defn- head-stamp
  "The stamp of fact r while it heads its chain for (e k) in layer L, as W
  holds it, else nil: a micro head unreplaced, or a stream-era head present
  and not tombstoned here."
  [w L e k r]
  (if-let [mh (get-in w [:heads [L e k r]])]
    (when (nil? (:replaced-by mh)) (:stamp mh))
    (let [sh (get-in w [:sheads [L e k r]])]
      (when (and (some? sh) (nil? (get-in w [:tombs [L e k r]]))) sh))))

(defn- touched-tasks [w sk]
  (into [] (distinct) (concat (keep #(get-in w [:task-of %]) (:entities sk)) [(:name-task sk)])))

(defn- clock-of [w t] (or (get-in w [:given t]) (get-in w [:clocks t]) 0))

(defn- put [w loc write] (assoc-in w [:out loc] write))

(defn- face [w nm fp reason b]
  (let [loc [:name nm :face fp]]
    (if (get-in w [:out loc])
      w
      (put w loc [:name nm :face fp {:reason reason :batch b}]))))

(defn- consume [w lids]
  (-> w
      (update :dels into (map (fn [lid] [:del-lease (locks/lease-name-of lid) nil (nth lid 1) nil])) lids)
      (update :cited into lids)))

(defn micro-extras
  "This gate's control facts, each reason to be placed by `reason-order`
  (M14, R13, phase 2's L20 and L28): a group's `:members` (the operator's,
  in the making act only); a lease act's shape; a session close's shape and
  author (the session itself or the operator); a forget's (the operator's
  here); another gate's control keys; and a `:class` fact outside a making
  act (O9: a shared layer's re-class is not ruled)."
  [o settings]
  (let [op? (contains? gate/exempt-actors (:who o))
        L (:layer o)
        made? (some? settings)
        facts (:facts o)]
    (concat
     (for [f facts :when (= :members (:k f))]
       (cond (not (and (= L (:e f)) (set? (:v f)) (every? env/readable-keyword? (:v f)))) :malformed-control
             (not op?) :control-not-allowed
             made? :layer-already-made))
     (for [f facts :when (= :lease (:k f))]
       (when-not (lease-ok? o) :malformed-control))
     (for [f facts :when (= :session-closed (:k f))]
       (cond (not (close-ok? o)) :malformed-control
             (not (or op? (= (:session o) (:e f)))) :control-not-allowed))
     (for [f facts :when (= :forget (:k f))]
       (let [v (:v f)]
         (cond (not (and (map? v) (= #{:target} (set (keys v)))
                         (vector? (:target v)) (= 2 (count (:target v)))
                         (env/valid-name? (nth (:target v) 0)) (int? (nth (:target v) 1))))
               :malformed-control
               (not op?) :control-not-allowed)))
     (for [f facts :when (contains? foreign-control-keys (:k f))] :control-not-allowed)
     (when (and made? (some #(and (gate/setting-fact? o %) (= :class (:k %))) facts))
       [:unsupported-reclass]))))

(defn record-subjects
  "The act's subject slot (ruling 8, phase 2's shapes): its facts' own
  subjects' union, with the layer's person owner (never the root actor)
  and what the offer carried. Over the cap it is refused, and the record
  keeps only the carried subjects, which the parser bounds (F3)."
  [sk settings reason]
  (let [o (:offer sk)
        owner (locks/person-owner (:owner settings))
        base (cond-> (into #{} (:subjects o)) owner (conj owner))]
    (if (= :too-many-subjects reason)
      (into #{} (:subjects o))
      (into base (:union sk)))))

(defn- decide-envelope
  "Rule 4 of the fold: the decision, by `gate/decide` over W's inputs, with
  this gate's reasons placed in `reason-order`; then W and the writes."
  [w sk wall b]
  (let [o (:offer sk) nm (:name sk) fp (:fp sk) L (:layer o)
        settings (settings-in-force w L)
        rows (into {} (map (fn [pid] [pid (merged-perm w L pid)])) (gate/pids-to-read o))
        heads (into {} (map (fn [[e k r]] [[e k r] (head-stamp w L e k r)])) (replacing-keys o))
        tasks (touched-tasks w sk)
        clock (reduce max 0 (map #(clock-of w %) tasks))
        d (gate/decide o settings rows heads clock wall (:digest sk))]
    (if (not= :decide (:kind d))
      (face w nm fp :gate-error b)
      (let [grain (or (:grain settings) :per-value)
            reason (first-in-order (concat [(get-in d [:record :reason])]
                                           (micro-extras o settings)
                                           [(:value-reason sk)
                                            (locks/grain-refusal grain (:facts o))
                                            (:person-reason sk)]))
            yes? (nil? reason)
            stamp (:stamp d)
            rec (-> (:record d)
                    (assoc :answer (if yes? :yes :no) :reason reason :batch b
                           :subjects (record-subjects sk settings reason)))
            entities (:entities sk)
            w (-> w
                  (assoc-in [:names nm] {:record rec :fp fp})
                  (put [:name nm :answer] [:name nm :answer nil rec])
                  (put [:name nm :fp] [:name nm :fp nil fp])
                  (as-> w (reduce (fn [w e] (put w [:entity e :answers nm] [:entity e :answers nm rec])) w entities))
                  (as-> w (reduce (fn [w t] (update-in w [:given t] (fnil max 0) stamp)) w tasks))
                  (consume (:owned sk)))]
        (if-not yes?
          w
          (let [indexed (map-indexed vector (:facts o))
                ;; heads: every fact heads its chain; a replaced head is kept with its replacer (M7)
                w (reduce (fn [w [i f]]
                            (let [fid [nm (long i)] e (:e f) k (:k f) head {:stamp stamp :batch b}
                                  w (-> w
                                        (assoc-in [:heads [L e k fid]] head)
                                        (put [:entity e :heads [L k fid]] [:entity e :heads [L k fid] head]))]
                              (if-let [r (:replaces f)]
                                (if-let [mh (get-in w [:heads [L e k r]])]
                                  (let [mh2 (assoc mh :replaced-by fid :replaced-batch b)]
                                    (-> w
                                        (assoc-in [:heads [L e k r]] mh2)
                                        (put [:entity e :heads [L k r]] [:entity e :heads [L k r] mh2])))
                                  (let [tomb {:by fid :batch b}]
                                    (-> w
                                        (assoc-in [:tombs [L e k r]] tomb)
                                        (put [:entity L :replaced [e k r]] [:entity L :replaced [e k r] tomb]))))
                                w)))
                          w indexed)
                ;; settings: a new version keyed by this batch (never an overwrite of an earlier one)
                w (if-let [s (:settings d)]
                    (let [v (assoc (select-keys s [:kind :owner :class :grain]) :batch b)]
                      (-> w
                          (assoc-in [:settings L :micro] v)
                          (put [:entity L :settings b] [:entity L :settings b v])))
                    w)
                ;; a group's members
                w (reduce (fn [w p] (put w [:entity L :members p] [:entity L :members p b]))
                          w (for [f (:facts o) :when (= :members (:k f)) p (sort (:v f))] p))
                ;; permissions: this store's delta rows (M5), batch-stamped
                w (reduce (fn [w [pid row]]
                            (let [mine (get-in w [:perms [L pid] :micro])
                                  mrow (if (and (:revoked row) (nil? (:revoked mine)))
                                         (assoc mine :revoked (:revoked row) :revoked-batch b)
                                         (assoc mine :granted (:granted row) :granted-batch b))]
                              (-> w
                                  (assoc-in [:perms [L pid] :micro] mrow)
                                  (put [:entity L :permissions pid] [:entity L :permissions pid mrow]))))
                          w (:permissions d))
                ;; a lease act mints its rows beside its name row (§A, M16)
                w (if (lease-act? o)
                    (update w :mints conj
                            [:mint nm nil nil {:under (when-not (contains? gate/exempt-actors (:who o)) (:who o))
                                               :session (:session o)
                                               :layer L
                                               :kind (:kind settings)
                                               :owner (locks/person-owner (:owner settings))
                                               :count (long (lease-count (first (:facts o))))
                                               :batch b}])
                    w)
                ;; a session close deletes its unconsumed lease rows in the layer ([PV-F4])
                w (if (close-act? o)
                    (update w :dels into (map (fn [ln] [:del-leases ln nil nil nil])) (:close-names sk))
                    w)]
            w))))))

(defn- fold-envelope
  "One envelope of the batch, in order (§A 'The fold', [PV-F3]):
  1. a face row: copied; 2. the name decided (committed, or earlier in this
  fold): the record path, or `:name-taken`; 3. a lock an earlier envelope
  under another name consumed: `:no-such-lock`; 4. the decision."
  [w [nm fp] wall b]
  (if-let [r (get-in w [:faces [nm fp]])]
    (face w nm fp r b)
    (let [sk (get-in w [:offers [nm fp]])
          prior (get-in w [:names nm])]
      (cond
        (nil? sk) w

        (and prior (:committed prior))
        (let [rec (:record prior)]
          (cond
            (not= (:digest sk) (:digest rec)) (face w nm fp :name-taken b)
            (and (= :yes (:answer rec))
                 (some #{:name-taken} (vals (get-in w [:resend [nm fp]]))))
            (face w nm fp :name-taken b)
            ;; recorded: answered from the record; its owned leases are consumed
            :else (consume w (:owned sk))))

        prior
        ;; decided earlier in this fold: the fold holds no value and cannot check content
        (face w nm fp :name-taken b)

        (some #(contains? (:cited w) %) (:cited sk))
        (face w nm fp :no-such-lock b)

        :else
        (decide-envelope w sk wall b)))))

(defn prepare
  "The fold on task 0 (M3), pure and total: the batch's envelopes in M2's
  order, each seeing the ones before it through W, with a guard per
  envelope that turns a failure into the unrecorded face `:gate-error`
  (the stream plan's [F6]), never a guard around the whole fold.
  {:writes [[kind route field k v] ...]}: the index writes (block 2a), the
  lease rows to mint (2a), and the lease rows to delete (2c)."
  [state wall b]
  (let [state (or state {})
        w0 (try (init-w state) (catch Throwable _ nil))]
    (if (nil? w0)
      {:writes []}
      (let [w (reduce (fn [w env]
                        (try (fold-envelope w env wall b)
                             (catch Throwable _ (face w (nth env 0) (nth env 1) :gate-error b))))
                      w0 (envelopes state))
            clocks (for [[t s] (:given w)] [:task t :clock nil s])]
        {:writes (-> []
                     (into (vals (:out w)))
                     (into clocks)
                     (into (:mints w))
                     (into (distinct) (:dels w)))}))))

;; =============================================================== block 2a

(defn mint-rows
  "The lease rows a yes lease act mints (§A, M16), with fresh locks and
  nonces drawn here, outside any decision (phase 2's `fresh`): each lock
  sealed under the lease act's person lock, or bare for the operator. A
  person whose lock is gone by now gets no rows (PV-F6: nothing throws; a
  later offer citing them is refused `:no-such-lock` on its face). A
  retried batch mints other bytes; only the committed attempt's exist."
  [meta entry]
  (try
    (let [n (:count meta)
          {:keys [locks nonces]} (locks/fresh n n)]
      (into [] (keep (fn [i]
                       (when-let [row (locks/lease-row (nth locks i) (:under meta) entry (nth nonces i))]
                         [(long i) (merge row (select-keys meta [:layer :session :kind :owner :batch]))])))
            (range n)))
    (catch Throwable _ [])))

;; =============================================================== block 2b

(defn row-wraps
  "Block 2b on the arrival task (§A): for a sealed act, open each value
  with its delivered lock, then its wrap by phase 2's table (per value:
  the value's own subjects; per act, one lock cited by several values:
  one wrap over the act's union, marked when any value is, phase 2's L6).
  {:lock-of {lid K} :plain {i bytes} :wraps {lid wrap} :marked {lid bool}
  :row? {lid bool}}. Pure, total; a lock that does not deliver now (its
  person forgotten since block 1, PV-F6) is left out, and its value's row
  is written with no lock."
  [in lrows persons]
  (try
    (let [o (:offer in)
          facts (:facts o)
          delivered (delivered-locks in lrows persons)
          meta (lease-meta lrows)
          owner (:owner meta)
          rv (locks/read-values facts delivered {:owner owner :carried (:subjects o) :grain :per-value})
          subjects (or (:subjects rv) {})
          by-lock (group-by (fn [[_ f]] (:lock-id f)) (keep-indexed (fn [i f] (when (value-fact? f) [i f])) facts))
          wraps (into {} (map (fn [[lid ifs]]
                                (let [marked? (boolean (some (fn [[_ f]] (contains? (:mark f) :die-with-any)) ifs))
                                      subs (if (< 1 (count ifs))
                                             (reduce into #{} (map (fn [[i _]] (get subjects i)) ifs))
                                             (get subjects (first (first ifs))))]
                                  [lid (locks/wrap-of owner subs marked?)])))
                    by-lock)]
      {:lock-of delivered
       :plain (or (:plain rv) {})
       :wraps wraps
       :row? (into {} (map (fn [[lid ifs]]
                             [lid (locks/row-lock? (:kind meta) (reduce into #{} (map (fn [[_ f]] (:mark f)) ifs)))]))
                   by-lock)})
    (catch Throwable _ {:lock-of {} :plain {} :wraps {} :row? {}})))

(defn wrap-persons-of [ww] (into [] (comp (mapcat locks/wrap-persons) (distinct)) (vals (:wraps ww))))

(defn nonces-needed [ww] (reduce + 0 (map locks/seals-needed (vals (:wraps ww)))))

(defn fresh-nonces "`n` fresh nonces, drawn outside any decision." [n]
  (try (:nonces (locks/fresh 0 n)) (catch Throwable _ [])))

(defn- wrap-live
  "The lock record of K under wrap w. A subject forgotten since block 1
  (PV-F6) cannot get an any-of blob: the record keeps the whole any-of
  list (the subject list, never the live list) with blobs for the live
  ones only, so the value is closed for the forgotten one; a required
  person forgotten, or every any-of one, leaves no record (closed)."
  [K w persons nonces]
  (let [live (into [] (filter #(locks/lock? (:lock (get persons %)))) (:any-of w))]
    (if (and (seq (:any-of w)) (seq live) (not= live (:any-of w)))
      (some-> (locks/wrap K (assoc w :any-of live) persons nonces) (assoc :any-of (:any-of w)))
      (locks/wrap K w persons nonces))))

(defn fact-rows
  "Block 2b's rows, one per fact, `[e i row lock-row]`, computed on the
  arrival task before any hop (so no plaintext and no bare lock leaves it,
  except the empty wrap of a value about no one, K bare in `:blob` by
  phase 2's lock record, [PV-F2]): `:v` for a control fact, `:sealed` as
  offered, `:lock-id`, the lock record in `:lock` or as the value's lock
  row (a personal or hand layer's, or an `:own-row` mark), and the value
  digest. Pure, total."
  [in ww persons nonces]
  (let [o (:offer in)
        L (:layer o)
        ns (vec nonces)
        ;; each lock's record, the nonces taken in the wraps' order
        [records _] (reduce (fn [[acc off] [lid w]]
                              (let [k (locks/seals-needed w)
                                    K (get (:lock-of ww) lid)]
                                [(assoc acc lid (when K (wrap-live K w persons (subvec ns (min off (count ns)) (min (+ off k) (count ns))))))
                                 (+ off k)]))
                            [{} 0] (sort-by (comp str key) (:wraps ww)))]
    (into []
          (map-indexed
           (fn [i f]
             (if (value-fact? f)
               (let [lid (:lock-id f)
                     K (get (:lock-of ww) lid)
                     record (get records lid)
                     row? (get (:row? ww) lid)
                     plain (get (:plain ww) i)]
                 [(:e f) (long i)
                  {:layer L :k (:k f) :v nil :sealed (:sealed f) :replaces (:replaces f) :mark (:mark f)
                   :lock-id lid
                   :lock (when-not row? record)
                   :digest (when (and K plain) (locks/value-digest K plain))}
                  (when row? record)])
               [(:e f) (long i)
                {:layer L :k (:k f) :v (env/encode-value (:v f)) :replaces (:replaces f) :mark (:mark f)}
                nil])))
          (:facts o))))

(defn rows-written?
  "Block 2b's filter on the name's task (M10, [PV-F3]): only an act decided
  yes in this batch, for this content, as this envelope, writes rows."
  [in rec dfp b]
  (boolean (and (some? rec) (= b (:batch rec)) (= :yes (:answer rec))
                (= (:digest in) (:digest rec)) (= (:fp in) dfp))))

;; ================================================================= queries

(defn lookup-result
  "micro-lookup's answer (§D, RD1): a faces entry for this envelope at or
  below F gives its reason; else a record at or below F, the record when
  its parts digest matches (or no digest is given: the record as data,
  [F13]) and `:name-taken` when it differs; else `:no-answer`."
  [face rec digest F]
  (cond
    (visible-at? face F) {:frontier F :answer :no :reason (:reason face) :face true :batch (:batch face)}
    (and (visible-at? rec F) (or (nil? digest) (= digest (:digest rec)))) (assoc rec :frontier F)
    (visible-at? rec F) {:frontier F :answer :no :reason :name-taken}
    :else {:frontier F :answer :no-answer}))

(defn lease-result
  "micro-lease's answer (§A): the plaintext locks of the rows at or below F,
  each unleased over its `:under` person's entry ([PV-F1]); a row that does
  not unlease is left out. The depot never sees this path."
  [entries persons F]
  {:frontier F
   :locks (into {} (keep (fn [[i row]]
                           (when (visible-at? row F)
                             (when-let [K (locks/unlease row (get persons (:under row)))]
                               [i K]))))
                entries)})

(defn lease-unders [entries] (into [] (comp (keep (fn [[_ row]] (:under row))) (distinct)) entries))

(defn act-result
  "micro-act's answer: the act's record under the entity and its rows on
  that entity, in index order, when its batch is at or below F; else only F."
  [rec rows F]
  (if (visible-at? rec F)
    {:frontier F :record rec :rows (vec rows)}
    {:frontier F :record nil :rows []}))

(defn leases-of-result
  "micro-leases-of's answer (§A): the names of the yes acts that touched the
  session as an entity (its lease acts), visible at F, for a restarted door
  to take its leases again by `micro-lease`."
  [answers F]
  {:frontier F
   :names (into [] (keep (fn [[nm rec]] (when (and (visible-at? rec F) (= :yes (:answer rec))) nm))) answers)})

(defn micro-head-key
  "The key a micro head sits under on its entity: [layer k fid]."
  [L hk]
  [L (nth hk 1) (nth hk 2)])

;; ============================================================ the topology

(def replace-tick-depot?
  "Tests may replace the tick depot with a global depot they append to
  (testing.md 'Testing Tick Depots'); M15 keeps it live and synchronises on
  the frontier."
  false)

(def persons-placeholder?
  "Phase 2's `$$persons` is declared by its stream gate, which this branch
  does not hold: only phase 2's pure lock namespace was merged
  (rig-build-locks 1febfa3d). Until that gate merges, this module declares
  the same PState, with phase 2's schema, on a placeholder stream topology
  fed by a test depot (`*persons-placeholder`), so the micro gate's reads of
  person locks run. The one seam: set false (or delete the call) when phase
  2's `gate` declares `$$persons`, and the micro gate reads that one."
  true)

(defn- declare-persons-placeholder!
  [setup topologies]
  (declare-depot setup *persons-placeholder (hash-by :person))
  (let [ps (stream-topology topologies "persons-placeholder")]
    (declare-pstate ps $$persons persons-schema)
    (<<sources ps
      (source> *persons-placeholder :> {:keys [*person *entry]})
      (|all)
      (local-transform> [(keypath *person) (termval *entry)] $$persons))))

(defn declare!
  "Declares the micro store into the one module (M1): its depot, its tick
  depot, the `micro` microbatch topology with its three PStates, and its
  query topologies. Called once from `rig.store.module/Store`'s body,
  after the stream gate's declarations (it reads `$$layers`)."
  [setup topologies]
  (when persons-placeholder? (declare-persons-placeholder! setup topologies))
  (declare-depot setup *micro-offers (hash-by route-key))
  (if replace-tick-depot?
    (declare-depot setup *micro-tick :random {:global? true})
    (declare-tick-depot setup *micro-tick 250))
  (let [mb (microbatch-topology topologies "micro")]
    (declare-pstate mb $$micro micro-schema)
    (declare-pstate mb $$micro-names names-schema)
    (declare-pstate mb $$micro-task task-schema)
    (<<sources mb
      ;; the tick forces a batch every 250 ms, so the frontier passes the last data batch (M7)
      (source> *micro-tick :> %tick)
      (%tick)

      (source> *micro-offers :> %mb)

      ;; ---- block 0: the frontier, on every task, every batch (M7, §D)
      (<<batch
        (|all)
        (ops/current-microbatch-id :> *b0)
        (local-transform> [(keypath :frontier) (termval (dec *b0))] $$micro-task))

      ;; ---- block 1: the gather, then the fold on task 0 (M3)
      (<<batch
        (%mb :> *raw)
        (intake *raw :> *in)
        (filter> (some? *in))
        (get *in :name :> *name)
        (inject/point! :micro-gather *name)
        (<<if (= :face (get *in :kind))
          (get *in :rows :> *rows)
         (else>)
          ;; the arrival task: the lease rows, their persons, the lock work (§A)
          (get *in :lease-name :> *lname)
          (lease-keys *in :> *lkeys)
          (loop<- [*l1-todo *lkeys *l1-acc {} :> *lrows]
            (<<if (empty? *l1-todo)
              (:> *l1-acc)
             (else>)
              (first *l1-todo :> *l1-lid)
              (nth *l1-lid 1 :> *l1-i)
              (local-select> [(keypath *lname :leases *l1-i)] $$micro-names :> *l1-row)
              (continue> (rest *l1-todo) (assoc *l1-acc *l1-lid *l1-row))))
          (unders *lrows :> *ups)
          (loop<- [*l2-todo *ups *l2-acc {} :> *upersons]
            (<<if (empty? *l2-todo)
              (:> *l2-acc)
             (else>)
              (first *l2-todo :> *l2-p)
              (local-select> [(keypath *l2-p)] $$persons :> *l2-entry)
              (continue> (rest *l2-todo) (assoc *l2-acc *l2-p *l2-entry))))
          (arrival-open *in *lrows *upersons :> *arr)
          (persons-to-check *in *arr :> *cps)
          (loop<- [*l3-todo *cps *l3-acc {} :> *cpersons]
            (<<if (empty? *l3-todo)
              (:> *l3-acc)
             (else>)
              (first *l3-todo :> *l3-p)
              (local-select> [(keypath *l3-p)] $$persons :> *l3-entry)
              (continue> (rest *l3-todo) (assoc *l3-acc *l3-p *l3-entry))))
          (person-reason *cps *cpersons :> *preason)
          ;; a session close reads its session's acts on this task, its first entity's (§A, [PV-F4])
          (get *in :close :> *close)
          (<<if (some? *close)
            (local-select> [(keypath *close :answers) (subselect MAP-KEYS)] $$micro {:allow-yield? true} :> *cnames)
           (else>)
            (identity nil :> *cnames))
          (skeleton *in *arr *preason *cnames :> *sk)
          ;; the name's task: its record first
          (|hash *name)
          (local-select> [(keypath *name :answer)] $$micro-names :> *rec)
          (ops/current-task-id :> *nt)
          (local-select> [(keypath :clock)] $$micro-task :> *nclock)
          (name-step *sk *rec *nt *nclock :> *nstep)
          (get *nstep :path :> *path)
          (get *nstep :sk :> *sk2)
          (<<cond
            (case> (= :fresh *path))
            ;; the layer's task: settings, the chain's permission rows, stream-era heads (M5)
            (get-in *sk2 [:offer :layer] :> *layer)
            (|hash *layer)
            (local-select> [(keypath *layer :settings) (sorted-map-range-to-end 1) (subselect MAP-VALS)] $$micro :> *msv)
            (first *msv :> *msettings)
            (<<if (nil? *msettings)
              (local-select> [(keypath *layer :settings)] $$layers :> *ssettings)
             (else>)
              (identity nil :> *ssettings))
            (gate/pids-to-read (get *sk2 :offer) :> *pids)
            (loop<- [*l4-todo *pids *l4-acc {} :> *perms]
              (<<if (empty? *l4-todo)
                (:> *l4-acc)
               (else>)
                (first *l4-todo :> *l4-pid)
                (local-select> [(keypath *layer :permissions *l4-pid)] $$micro :> *l4-mrow)
                (<<if (some? *ssettings)
                  (local-select> [(keypath *layer :permissions *l4-pid)] $$layers :> *l4-srow)
                 (else>)
                  (identity nil :> *l4-srow))
                (continue> (rest *l4-todo) (assoc *l4-acc *l4-pid {:micro *l4-mrow :stream *l4-srow}))))
            (<<if (some? *ssettings)
              (replacing-keys (get *sk2 :offer) :> *hkeys)
             (else>)
              (identity [] :> *hkeys))
            (loop<- [*l5-todo *hkeys *l5-sh {} *l5-tb {} :> *sheads *tombs]
              (<<if (empty? *l5-todo)
                (:> *l5-sh *l5-tb)
               (else>)
                (first *l5-todo :> *l5-hk)
                (local-select> [(keypath *layer :heads *l5-hk)] $$layers :> *l5-s)
                (<<if (some? *l5-s)
                  (local-select> [(keypath *layer :replaced *l5-hk)] $$micro :> *l5-t)
                 (else>)
                  (identity nil :> *l5-t))
                (continue> (rest *l5-todo) (assoc *l5-sh *l5-hk *l5-s) (assoc *l5-tb *l5-hk *l5-t))))
            (layer-rows *sk2 *msettings *ssettings *perms *sheads *tombs :> *lstep)
            (<<if (get *lstep :face)
              (get *lstep :rows :> *rows)
             (else>)
              (into (get *nstep :rows) (get *lstep :rows) :> *first-rows)
              ;; each entity's task: its task id, clock and micro heads
              (ops/explode-indexed (get *sk2 :entities) :> *ei *e)
              (|hash *e)
              (ops/current-task-id :> *t)
              (local-select> [(keypath :clock)] $$micro-task :> *eclock)
              (micro-head-keys *sk2 *e *first-rows :> *mhkeys)
              (loop<- [*l6-todo *mhkeys *l6-acc {} :> *mheads]
                (<<if (empty? *l6-todo)
                  (:> *l6-acc)
                 (else>)
                  (first *l6-todo :> *l6-hk)
                  (micro-head-key *layer *l6-hk :> *l6-key)
                  (local-select> [(keypath *e :heads *l6-key)] $$micro :> *l6-h)
                  (continue> (rest *l6-todo) (assoc *l6-acc *l6-hk *l6-h))))
              (entity-rows *sk2 *e *t *eclock *mheads *first-rows (= 0 *ei) :> *rows))

            (case> (= :resend *path))
            ;; the record path: each value checked on its entity task under the recorded lock (M18)
            (value-facts-of *sk2 :> *vfacts)
            (get *in :offer :> *roffer)
            (ops/explode *vfacts :> [*vi *vf0])
            (nth (get *roffer :facts) *vi :> *vf)
            (get *lrows (get *vf :lock-id) :> *carried)
            (get *vf :e :> *ve)
            (|hash *ve)
            (local-select> [(keypath *ve :log *name *vi)] $$micro :> *vrow)
            (get *vrow :lock-id :> *vrlid)
            (<<if (and> (some? *vrow) (nil? (get *vrow :lock)) (some? *vrlid))
              (local-select> [(keypath *ve :locks *vrlid)] $$micro :> *vlockrow)
             (else>)
              (identity nil :> *vlockrow))
            (resend-persons *vrow *vlockrow *carried :> *rps)
            (loop<- [*l7-todo *rps *l7-acc {} :> *rpersons]
              (<<if (empty? *l7-todo)
                (:> *l7-acc)
               (else>)
                (first *l7-todo :> *l7-p)
                (local-select> [(keypath *l7-p)] $$persons :> *l7-entry)
                (continue> (rest *l7-todo) (assoc *l7-acc *l7-p *l7-entry))))
            (resend-check *vf *vrow *vlockrow *carried *rpersons (get *roffer :session) (get *roffer :layer) :> *check)
            (<<if (= *vi (first (first *vfacts)))
              (conj (get *nstep :rows) [[:resend *name (get *sk2 :fp) *vi] *check] :> *rows)
             (else>)
              (identity [[[:resend *name (get *sk2 :fp) *vi] *check]] :> *rows))

            (default>)
            (get *nstep :rows :> *rows)))
        (ops/explode *rows :> [*need *found])
        (|global)
        (aggs/+map-agg *need *found :> *state)
        (wall :> *wall)
        (ops/current-microbatch-id :> *b1)
        (prepare *state *wall *b1 :> *out)
        (ops/explode (get *out :writes) :> *w)
        (materialize> *w :> $$micro-writes))

      ;; ---- block 2a: the fold's index writes, and the lease rows a yes lease act mints
      (<<batch
        ($$micro-writes :> *w2)
        (nth *w2 0 :> *kind2)
        (nth *w2 1 :> *route2)
        (nth *w2 2 :> *field2)
        (nth *w2 3 :> *k2)
        (nth *w2 4 :> *v2)
        (<<cond
          (case> (= :entity *kind2))
          (|hash *route2)
          (local-transform> [(keypath *route2 *field2 *k2) (termval *v2)] $$micro)

          (case> (= :name *kind2))
          (|hash *route2)
          (inject/point! :micro-2a *route2)
          (<<cond
            (case> (= :face *field2))
            (partial keep-first *v2 :> *keep)
            (local-transform> [(keypath *route2 :faces *k2) (term *keep)] $$micro-names)
            (default>)
            (local-transform> [(keypath *route2 *field2) (termval *v2)] $$micro-names))

          (case> (= :task *kind2))
          (|direct *route2)
          (local-transform> [(keypath *field2) (termval *v2)] $$micro-task)

          (case> (= :mint *kind2))
          (|hash *route2)
          (get *v2 :under :> *under2)
          (<<if (some? *under2)
            (local-select> [(keypath *under2)] $$persons :> *pentry)
           (else>)
            (identity nil :> *pentry))
          (mint-rows *v2 *pentry :> *mrows)
          (ops/explode *mrows :> [*mi *mrow])
          (local-transform> [(keypath *route2 :leases *mi) (termval *mrow)] $$micro-names)

          (default>)
          (filter> false)))

      ;; ---- block 2b: the rows, from the offers themselves (M10, §A)
      (<<batch
        (%mb :> *raw3)
        (intake *raw3 :> *in3)
        (filter> (= :offer (get *in3 :kind)))
        (get *in3 :lease-name :> *lname3)
        (lease-keys *in3 :> *lkeys3)
        (loop<- [*m1-todo *lkeys3 *m1-acc {} :> *lrows3]
          (<<if (empty? *m1-todo)
            (:> *m1-acc)
           (else>)
            (first *m1-todo :> *m1-lid)
            (nth *m1-lid 1 :> *m1-i)
            (local-select> [(keypath *lname3 :leases *m1-i)] $$micro-names :> *m1-row)
            (continue> (rest *m1-todo) (assoc *m1-acc *m1-lid *m1-row))))
        (unders *lrows3 :> *ups3)
        (loop<- [*m2-todo *ups3 *m2-acc {} :> *upersons3]
          (<<if (empty? *m2-todo)
            (:> *m2-acc)
           (else>)
            (first *m2-todo :> *m2-p)
            (local-select> [(keypath *m2-p)] $$persons :> *m2-entry)
            (continue> (rest *m2-todo) (assoc *m2-acc *m2-p *m2-entry))))
        (row-wraps *in3 *lrows3 *upersons3 :> *ww)
        (wrap-persons-of *ww :> *wps)
        (loop<- [*m3-todo *wps *m3-acc {} :> *wpersons]
          (<<if (empty? *m3-todo)
            (:> *m3-acc)
           (else>)
            (first *m3-todo :> *m3-p)
            (local-select> [(keypath *m3-p)] $$persons :> *m3-entry)
            (continue> (rest *m3-todo) (assoc *m3-acc *m3-p *m3-entry))))
        (fresh-nonces (nonces-needed *ww) :> *nonces)
        (fact-rows *in3 *ww *wpersons *nonces :> *frows)
        (get *in3 :name :> *name3)
        (|hash *name3)
        (local-select> [(keypath *name3 :answer)] $$micro-names :> *rec3)
        (local-select> [(keypath *name3 :fp)] $$micro-names :> *dfp3)
        (ops/current-microbatch-id :> *b3)
        (filter> (rows-written? *in3 *rec3 *dfp3 *b3))
        (inject/point! :micro-2b *name3)
        (<<atomic
          (ops/explode-map (get-in *in3 [:offer :stood-on]) :> *sf *ss)
          (local-transform> [(keypath *name3 :stood-on *sf) (termval *ss)] $$micro-names))
        (ops/explode *frows :> [*e3 *i3 *row3 *lockrow3])
        (|hash *e3)
        (local-transform> [(keypath *e3 :log *name3 *i3) (termval *row3)] $$micro)
        (get *row3 :lock-id :> *lid3)
        (<<if (some? *lockrow3)
          (local-transform> [(keypath *e3 :locks *lid3) (termval *lockrow3)] $$micro)))

      ;; ---- block 2c: consume the leases decided in this batch, and a closed session's (M19)
      (<<batch
        ($$micro-writes :> *w4)
        (nth *w4 0 :> *kind4)
        (filter> (contains? #{:del-lease :del-leases} *kind4))
        (nth *w4 1 :> *lname4)
        (|hash *lname4)
        (inject/point! :micro-2c *lname4)
        (nth *w4 3 :> *i4)
        (<<if (= :del-lease *kind4)
          (local-transform> [(keypath *lname4 :leases *i4) NONE>] $$micro-names)
         (else>)
          (local-transform> [(keypath *lname4 :leases) NONE>] $$micro-names)))))

  ;; ---------------------------------------------------------------- queries
  ;; Every reader-facing read takes F explicitly (§D): nil means the reading
  ;; task's own frontier; the result carries the F it used.

  (<<query-topology topologies "micro-lookup" [*name *digest *fp *f :> *result]
    (|hash *name)
    (local-select> [(keypath :frontier)] $$micro-task :> *own)
    (frontier-of *f *own :> *F)
    (<<if (some? *fp)
      (local-select> [(keypath *name :faces *fp)] $$micro-names :> *face)
     (else>)
      (identity nil :> *face))
    (<<if (visible-at? *face *F)
      (identity nil :> *rec)
     (else>)
      (local-select> [(keypath *name :answer)] $$micro-names :> *rec))
    (lookup-result *face *rec *digest *F :> *result)
    (|origin))

  (<<query-topology topologies "micro-act" [*e *name *f :> *result]
    (|hash *e)
    (local-select> [(keypath :frontier)] $$micro-task :> *own)
    (frontier-of *f *own :> *F)
    (local-select> [(keypath *e :answers *name)] $$micro :> *rec)
    (<<if (visible-at? *rec *F)
      (local-select> [(keypath *e :log *name) (subselect ALL)] $$micro :> *rows)
     (else>)
      (identity nil :> *rows))
    (act-result *rec *rows *F :> *result)
    (|origin))

  (<<query-topology topologies "micro-lease" [*lease-name *f :> *result]
    (|hash *lease-name)
    (local-select> [(keypath :frontier)] $$micro-task :> *own)
    (frontier-of *f *own :> *F)
    (local-select> [(keypath *lease-name :leases) (subselect ALL)] $$micro-names {:allow-yield? true} :> *entries)
    (lease-unders *entries :> *lups)
    (loop<- [*q-todo *lups *q-acc {} :> *lpersons]
      (<<if (empty? *q-todo)
        (:> *q-acc)
       (else>)
        (first *q-todo :> *q-p)
        (local-select> [(keypath *q-p)] $$persons :> *q-entry)
        (continue> (rest *q-todo) (assoc *q-acc *q-p *q-entry))))
    (lease-result *entries *lpersons *F :> *result)
    (|origin))

  (<<query-topology topologies "micro-leases-of" [*session *f :> *result]
    (|hash *session)
    (local-select> [(keypath :frontier)] $$micro-task :> *own)
    (frontier-of *f *own :> *F)
    (local-select> [(keypath *session :answers) (subselect ALL)] $$micro {:allow-yield? true} :> *answers)
    (leases-of-result *answers *F :> *result)
    (|origin)))
