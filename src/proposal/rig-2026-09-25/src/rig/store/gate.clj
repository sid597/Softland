;; IMPORTANT: Before modifying this file, re-read PLAN-stream-store.md.
(ns rig.store.gate
  "The stream gate's decision, pure and total: the rig's counterpart of
  model.clj's `stream-step`, `refusal` and `stamp-for` under `baseline`.

  The topology calls `intake` on the raw record, reads what the decision
  needs on the layer's home task (the name's record first, then, only for
  an undecided name, the settings, the clock, the permission rows and the
  heads rows), calls `decide`, and writes what it returns, in one event.
  `intake` and `decide` catch every throwable: an exception in topology
  code is fatal to the worker (RIG.md, phase 0), so a failure here is the
  unrecorded face refusal :gate-error (F6)."
  (:require [rig.store.clock :as hlc]
            [rig.store.envelope :as env]
            [rig.store.permit :as permit]
            [rig.store.reads :as reads])
  (:import [com.rpl.rama.helpers TopologyUtils]))

(defn wall-now
  "The gate's wall clock in milliseconds, simulated in tests. A function
  call, not a seek. `stamp-for` turns it into a stamp (rig.store.clock)."
  []
  (TopologyUtils/currentTimeMillis))

(defn perm-entity
  "The entity a person's permission facts are about (model.clj `perm-entity`)."
  [who]
  (keyword (str "perm-" (name who))))

(def exempt-actors
  "Who acts at the root, cites no permission and skips the four permission
  checks (model.clj `exempt?`). The operator only in this stage: `:store`
  is refused on its face (F6)."
  #{:operator})

;; ------------------------------------------------------------ control facts

(def layer-kinds #{:personal :hand :agent :group :base})
(def grains #{:per-value :per-act})

(defn setting-fact?
  "A fact on the layer's own entity with a setting key (P10)."
  [offer f]
  (and (= (:layer offer) (:e f)) (contains? env/setting-keys (:k f))))

(defn control-fact?
  "A fact the gate projects: a setting, a grant or a revocation."
  [offer f]
  (or (setting-fact? offer f) (contains? #{:permission :revoke} (:k f))))

(defn grant-target
  "The permission a grant fact grants, when its value names one."
  [f]
  (when (= :permission (:k f))
    (let [v (:v f)] (when (map? v) (:id v)))))

(defn revoke-target
  "The permission a revoke fact revokes, when its value names one."
  [f]
  (when (= :revoke (:k f))
    (let [v (:v f)] (when (map? v) (:permission v)))))

(defn- control-value-ok?
  "Whether a control fact's value has the shape its projection needs (R13):
  a wrong shape would violate a schema inside the topology."
  [offer f]
  (let [v (:v f)]
    (if (setting-fact? offer f)
      (case (:k f)
        :kind (contains? layer-kinds v)
        :owner (env/readable-keyword? v)
        :class (contains? env/classes v)
        :lock-grain (contains? grains v))
      (case (:k f)
        :permission (and (map? v) (= #{:id} (set (keys v))) (env/pid? (:id v)))
        :revoke (and (map? v) (= #{:permission} (set (keys v))) (env/pid? (:permission v)))))))

(defn- control-allowed?
  "Who may write a control fact (R13): the operator any; the layer's owner
  only a lock-grain switch (P10: the owner's own permission covers a grain
  switch). Grants and revocations are the operator's (P8)."
  [offer settings f]
  (or (contains? exempt-actors (:who offer))
      (and (setting-fact? offer f)
           (= :lock-grain (:k f))
           (= (:who offer) (:owner settings)))))

(defn class-in-force
  "The class the offer's class is checked against: the layer's class fact,
  else the act's own class fact (P10)."
  [offer settings]
  (or (:class settings)
      (some #(when (and (setting-fact? offer %) (= :class (:k %))) (:v %)) (:facts offer))))

(defn owner-in-force
  "The layer's owner: its owner fact, else the act's own owner fact when its
  value is a keyword a subject slot can hold. The answer record is written
  for a refused act too, so an owner fact with a malformed value (refused
  :malformed-control) must not reach the record's Keyword set."
  [offer settings]
  (or (:owner settings)
      (some #(when (and (setting-fact? offer %) (= :owner (:k %)) (env/readable-keyword? (:v %))) (:v %))
            (:facts offer))))

;; ------------------------------------------------------------------ reads

(defn pids-to-read
  "The permission rows the decision reads on the home task: the cited one
  and every permission above it (`permit/chain`: a person's offer; skipped
  for the operator), and every grant's and
  revocation's target, which P8 needs (a revoke stands on an unrevoked
  grant; a second grant leaves the first as it is)."
  [offer]
  (into []
        (comp (filter env/pid?) (distinct))
        (concat (when-not (contains? exempt-actors (:who offer)) (permit/chain (:permission offer)))
                (keep grant-target (:facts offer))
                (keep revoke-target (:facts offer)))))

(defn heads-to-read
  "The heads rows the decision reads: [e k r] for each replacing fact."
  [offer]
  (into [] (comp (filter :replaces) (map (fn [f] [(:e f) (:k f) (:replaces f)])) (distinct))
        (:facts offer)))

;; --------------------------------------------------------------- refusal

(defn- doubled? [xs] (not= (count xs) (count (set xs))))

(defn refusal
  "Why the gate refuses the act, or nil: model.clj `refusal` in its order,
  with the rig's own reasons placed as PLAN-stream-store.md says (P7, F8)
  and R13's two after the permission checks. `settings` is the layer's
  settings (nil for a layer not made), `rows` the permission rows read
  (pid -> row), `heads` the heads read ([e k r] -> stamp or nil)."
  [offer settings rows heads]
  (let [facts (:facts offer)
        who (:who offer)
        exempt? (contains? exempt-actors who)
        perm (when-not exempt? (permit/refusal offer rows))
        in-force (class-in-force offer settings)
        rs (keep :replaces facts)
        revoked-pids (keep revoke-target facts)]
    (cond
      (some #(and (:layer %) (not= (:layer offer) (:layer %))) facts)
      :fact-outside-the-acts-layer

      (nil? in-force)
      :no-such-layer

      (not= (:class offer) in-force)
      :class-mismatch

      ;; the model's four permission reasons in its order, over the cited
      ;; permission's chain (PLAN-micro-store.md §B, R19: the walk)
      (some? perm)
      perm

      (some #(and (control-fact? offer %) (not (control-value-ok? offer %))) facts)
      :malformed-control

      (some #(and (control-fact? offer %) (not (control-allowed? offer settings %))) facts)
      :control-not-allowed

      (or (doubled? rs)
          (some #(and (:replaces %) (nil? (get heads [(:e %) (:k %) (:replaces %)]))) facts))
      :stale-replaces

      (or (doubled? revoked-pids)
          (some #(let [row (get rows %)] (or (nil? (:granted row)) (:revoked row))) revoked-pids))
      :stale-revoke

      (some #(and (setting-fact? offer %) (#{:kind :owner} (:k %))
                  (some? (get settings (env/setting-keys (:k %)))))
            facts)
      :layer-already-made

      (and (= :by-entity in-force)
           (some #(and (setting-fact? offer %) (= :class (:k %)) (= :by-layer (:v %))) facts))
      :unsupported-reclass)))

;; ------------------------------------------------------------------ stamp

(defn stamp-for
  "Ruling 4 as a hybrid clock (rig.store.clock/next-stamp): at or after the
  wall's millisecond, after this task's last stamp `clock`, after every
  stamp the act stood on (carried, P9), after every fact it replaces (read
  from the heads), and after the moment of every read entry fact it carries
  (stage 5a, F1: a read entry is stamped after what it read). `wall` is in
  milliseconds; every other argument holds stamps."
  [offer heads clock wall]
  (hlc/next-stamp wall clock
                  (concat (vals (:stood-on offer))
                          (keep #(get heads [(:e %) (:k %) (:replaces %)]) (:facts offer))
                          (reads/entry-moments (:facts offer)))))

;; ----------------------------------------------------------------- answer

(defn- ack [answer reason stamp nm]
  {:answer answer :reason reason :stamp stamp :name nm})

(defn answer-from-record
  "A name already decided on this task: the recorded answer when the digest
  matches, else the name is taken (nothing is written either way)."
  [rec digest nm]
  (if (= digest (:digest rec))
    {:kind :recorded :ack (ack (:answer rec) (:reason rec) (:stamp rec) nm)}
    {:kind :taken :ack (ack :no :name-taken nil nm)}))

(defn face-ack
  "The answer to a record refused on its face (P7): through the ack only."
  [refused raw]
  (ack :no (:refuse refused) nil (env/name-of raw)))

(defn intake
  "The gate's first step on a raw depot record. Total. {:refuse reason} for
  a record refused on its face; else the parsed offer, its digest, and the
  keys of the permission rows and heads rows the decision reads."
  [raw]
  (try
    (let [p (env/parse raw :stream)]
      (if (contains? p :refuse)
        p
        (let [o (:ok p)]
          {:offer o
           :digest (env/digest o)
           :pids (pids-to-read o)
           :heads (heads-to-read o)})))
    (catch Throwable _ {:refuse :gate-error})))

;; ----------------------------------------------------------------- decide

(defn- log-rows [offer]
  (mapv (fn [f] {:e (:e f) :k (:k f) :v (env/encode-value (:v f))
                 :replaces (:replaces f) :mark (:mark f)})
        (:facts offer)))

(defn- answer-record
  "The name's answer for ever (D4): a yes and a recorded no alike keep the
  act's bounded parts; what it stood on is kept beside it, for a yes (F2)."
  [offer settings reason stamp digest]
  (let [owner (owner-in-force offer settings)]
    {:answer (if reason :no :yes)
     :reason reason
     :stamp stamp
     :digest digest
     :who (:who offer)
     :class (:class offer)
     :permission (:permission offer)
     :session (:session offer)
     :because-of (:because-of offer)
     :claimed-when (:claimed-when offer)
     :subjects (cond-> (into #{} (:subjects offer)) owner (conj owner))}))

(defn- first-per-pid
  "[pid fid] pairs keeping the first per pid: within one act, as across
  acts, the first grant stays the index's grant (P8)."
  [pairs]
  (second (reduce (fn [[seen out] [p _ :as pair]]
                    (if (contains? seen p) [seen out] [(conj seen p) (conj out pair)]))
                  [#{} []] pairs)))

(defn- decide*
  [offer settings rows heads clock wall digest]
  (let [nm (:name offer)
        reason (refusal offer settings rows heads)
        stamp (stamp-for offer heads clock wall)
        yes? (nil? reason)
        facts (:facts offer)
        indexed (map-indexed vector facts)
        setting-updates (into {} (for [f facts :when (setting-fact? offer f)]
                                   [(env/setting-keys (:k f)) (:v f)]))
        log (when yes? (log-rows offer))
        ;; stage 5a: the admitted act's index entries, from the rows as written (a
        ;; failure there is this function's throw, so decide's :gate-error road)
        ix (if yes? (reads/index-writes (reads/current-hints) (:layer offer) nm log stamp) reads/no-index-writes)
        _ (when (:index-error ix) (throw (ex-info "index writes failed" {:name nm})))]
    {:kind :decide
     :stamp stamp
     :record (answer-record offer settings reason stamp digest)
     :ack (ack (if yes? :yes :no) reason stamp nm)
     ;; the rest is written only for a yes
     :log log
     :stood-on (if yes? (:stood-on offer) {})
     :heads-del (if yes? (into [] (distinct) (for [f facts :when (:replaces f)] [(:e f) (:k f) (:replaces f)])) [])
     :heads-put (if yes? (vec (for [[i f] indexed] [[(:e f) (:k f) [nm (long i)]] stamp])) [])
     ;; the settings as they stand after the act, written whole (they were read)
     :settings (when (and yes? (seq setting-updates)) (merge settings setting-updates))
     ;; permission rows as they stand after the act, written whole (they were read)
     :permissions (if yes?
                    (let [grants (first-per-pid
                                  (for [[i f] indexed
                                        :let [p (grant-target f)]
                                        :when (and p (nil? (:granted (get rows p))))]
                                    [p [nm (long i)]]))
                          revokes (for [[i f] indexed
                                        :let [p (revoke-target f)]
                                        :when p]
                                    [p [nm (long i)]])]
                      (into []
                            (concat (for [[p g] grants] [p {:granted g}])
                                    (for [[p r] revokes] [p (assoc (get rows p) :revoked r)]))))
                    [])
     ;; stage 5a: the three index write lists (empty for a no)
     :index-put (:index-put ix)
     :index-of (:index-of ix)
     :index-del (:index-del ix)}))

(defn decide
  "Decide a fresh offer (no record under its name on this task): the answer
  record, the ack and every write, precomputed; the stamp is given for a yes
  and a no alike. Total: a failure inside is the unrecorded face refusal
  :gate-error, never an exception in the topology (F6)."
  [offer settings rows heads clock wall digest]
  (try
    (decide* offer settings rows heads clock wall digest)
    (catch Throwable _
      {:kind :face :ack (ack :no :gate-error nil (:name offer))})))
