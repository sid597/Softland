;; IMPORTANT: Before modifying this file, re-read PLAN-stream-store.md and
;; PLAN-locks-and-forgetting.md (the refusal order L27, the gate event).
(ns rig.store.gate
  "The stream gate's decision, pure and total: the rig's counterpart of
  model.clj's `stream-step`, `refusal` and `stamp-for` under `baseline`.

  The topology calls `intake` on the raw record, reads what the decision
  needs on the layer's home task (the name's record first, then, only for
  an undecided name, the settings, the clock, the permission rows and the
  heads rows), calls `decide`, and writes what it returns, in one event.
  `intake` and `decide` catch every throwable: an exception in topology
  code is fatal to the worker (RIG.md, phase 0), so a failure here is the
  unrecorded face refusal :gate-error (F6).

  Stage 2 (PLAN-locks-and-forgetting.md): `decide` takes the lock context
  the event read (rig.store.locks `decision-reads>`), places this stage's
  recorded reasons after stage 1's (`locks/lock-refusal`, L27's order) and
  adds the lock effects (`locks/lock-effects`) under `:locks`; the lock
  control facts (forget, lease, session close, person, forget person) are
  control facts here, checked for shape and for who may write them (R13
  grown: L10, L20, L28, L7)."
  (:require [rig.store.clock :as hlc]
            [rig.store.envelope :as env]
            [rig.store.grammar :as grammar]
            [rig.store.locks :as locks]
            [rig.store.permit :as permit]
            [rig.store.promote-shape :as ps]
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
  checks (model.clj `exempt?`): the operator, and, stage 4, the store's own
  steps (a promotion's read-out, `:who :store`). No record on a depot may
  claim `:store` (env/parse refuses it on its face, F6), so only the
  store's own steps act as it."
  #{:operator :store})

;; ------------------------------------------------------------ control facts

(def layer-kinds
  "Layer kinds; stage 2 adds `:store`, the kind of the store layer
  `:people` (L7)."
  #{:personal :hand :agent :group :base :store})
(def grains #{:per-value :per-act})

(defn setting-fact?
  "A fact on the layer's own entity with a setting key (P10)."
  [offer f]
  (and (= (:layer offer) (:e f)) (contains? env/setting-keys (:k f))))

(defn misplaced-setting?
  "A fact under a setting key about an entity other than its act's layer
  (step R, REVIEW-wave1 R-1). No gate acts on one there: both project
  settings only from a fact on the layer's own entity (`setting-fact?`),
  and a control key's value is plaintext under no lock, so admitted it
  would sit in the log and the indexes beyond every forget. Both gates
  refuse it `:malformed-control` (`refusal`), and no door sends one
  (rig.store.client `refuse-misplaced!`). Pure and total."
  [offer f]
  (boolean (and (map? f) (contains? env/setting-keys (:k f)) (not= (:layer offer) (:e f)))))

(defn store-key?
  "A key the store itself owns (phase 6, V-F11): the control keys, which
  include `:grammar`, and the read entries' keys. Its grammar is compiled,
  so no grammar fact may govern it, and no tool may match or write it."
  [k]
  (or (contains? env/control-keys k)
      (= :grammar k)
      (contains? reads/read-keys k)))

(defn control-fact?
  "A fact the gate projects or acts on: a setting, a grant or a
  revocation, stage 2's lock control facts (a forget, a lease, a session
  close, a person made or forgotten), stage 4's promotion request and
  crossing fact, and phase 6's grammar facts."
  [offer f]
  (or (setting-fact? offer f)
      (contains? #{:permission :revoke :grammar} (:k f))
      (contains? locks/lock-control-keys (:k f))
      (contains? #{:promote-request :crossed} (:k f))))

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
        :revoke (and (map? v) (= #{:permission} (set (keys v))) (env/pid? (:permission v)))
        ;; stage 2 (plan, gate event step 2)
        :forget (and (map? v) (= #{:target} (set (keys v))) (env/fid? (:target v)))
        :person (and (map? v) (= #{:id} (set (keys v))) (env/readable-keyword? (:id v)) (= (:e f) (:id v)))
        :forget-person (and (map? v) (= #{:person} (set (keys v))) (env/readable-keyword? (:person v))
                            (= (:e f) (:person v)))
        ;; stage 4: a landing lease is {:count 1 :landing L*}, L* bound to its own name (PR4, F1)
        :lease (and (map? v) (or (= #{:count} (set (keys v))) (ps/landing-lease-value? offer v))
                    (int? (:count v)) (<= 1 (:count v) locks/max-lease)
                    (some? (:session offer)) (= (:e f) (:session offer)))
        ;; stage 4 (PLAN-promotion.md step 1, PR1, PR2)
        :promote-request (ps/request-value-ok? offer v)
        :crossed (ps/crossed-value-ok? v)
        ;; stage 5b (FRR5): an optional :reads part, :keep or :drop, what the session's
        ;; read entries become; absent means keep
        :session-closed (and (map? v) (contains? v :session) (every? #{:session :reads} (keys v))
                             (env/readable-keyword? (:session v)) (= (:e f) (:session v))
                             (or (not (contains? v :reads)) (contains? #{:keep :drop} (:reads v))))
        ;; phase 6: a grammar in the language, on a key that is not the store's (4.1, 4.2)
        :grammar (and (nil? (grammar/refusal v)) (not (store-key? (:e f))))))))

(defn- control-allowed?
  "Who may write a control fact (R13): the operator any; the layer's owner
  only a lock-grain switch (P10: the owner's own permission covers a grain
  switch). Grants and revocations are the operator's (P8). Stage 2: the
  owner may also forget a value (L10); anyone whose cited permission
  covers the layer may lease (L20) and close their own session, the
  operator any session (L28); person acts are the operator's, in the store
  layer only (L7), so every person act is ordered on one task (L8).
  Stage 4: a crossing fact is the store's own step's alone; anyone whose
  permission covers the layer may request a promotion (PR1). Phase 6: the
  layer's owner may write a grammar (T-RC4)."
  [offer settings f]
  (let [who (:who offer)
        k (:k f)]
    (cond
      (= :crossed k) (= :store who)
      (contains? #{:person :forget-person} k) (and (contains? exempt-actors who) (= :store (:kind settings)))
      (contains? exempt-actors who) true
      :else (or (and (setting-fact? offer f) (= :lock-grain k) (= who (:owner settings)))
                (and (= :forget k) (= who (:owner settings)))
                (and (= :grammar k) (= who (:owner settings)))
                (= :lease k)
                (= :promote-request k)
                (and (= :session-closed k) (= (:session offer) (get-in f [:v :session])))))))

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

(defn- refusal-with-rows
  "`refusal`'s body: `key-rows` nil skips the rebuild check."
  [offer settings rows heads key-rows]
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

      (or ;; step R (REVIEW-wave1 R-1): a setting key about another entity, which
          ;; no gate acts on, would keep a plaintext value under no lock
          (some #(misplaced-setting? offer %) facts)
          (some #(and (control-fact? offer %) (not (control-value-ok? offer %))) facts)
          ;; a lock control fact is the act's one fact (rig choice); stage 4: so is
          ;; a promotion request, whose read-out continues its record
          (and (some #(contains? locks/lock-control-keys (:k %)) facts) (not= 1 (count facts)))
          (and (some #(= :promote-request (:k %)) facts) (not= 1 (count facts)))
          ;; phase 6: one grammar per key per act (4.1)
          (doubled? (keep #(when (grammar/grammar-fact? %) (grammar/governed %)) facts)))
      :malformed-control

      (some #(and (control-fact? offer %) (not (control-allowed? offer settings %))) facts)
      :control-not-allowed

      ;; phase 6 (b1, V-F4): a grammar changing the hints or opacity of a key
      ;; the layer holds a fact under, or the act writes one under
      (and (some? key-rows) (grammar/rebuild-refusal facts key-rows))
      :grammar-change-needs-rebuild

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

(defn refusal
  "Why the gate refuses the act, or nil: model.clj `refusal` in its order,
  with the rig's own reasons placed as PLAN-stream-store.md says (P7, F8)
  and R13's two after the permission checks. `settings` is the layer's
  settings (nil for a layer not made), `rows` the permission rows read
  (pid -> row), `heads` the heads read ([e k r] -> stamp or nil).

  Phase 6: `key-rows`, the rows of the act's keys read in the same event
  ({k row}), for `:grammar-change-needs-rebuild`, placed right after
  `:control-not-allowed`; a malformed grammar, a grammar on a store key and
  two grammars for one key are `:malformed-control`. The 4-arity is the
  micro fold's, which reads no key rows until phase 6b (the micro gate
  refuses grammar facts meanwhile), and skips the rebuild check.

  Step R (REVIEW-wave1 R-1, W1-7 widened): a setting key about another
  entity than the layer (`misplaced-setting?`) is `:malformed-control`
  too. Both gates decide stage 1's list here (the stream gate through
  `decide`, the micro fold through `rig.store.micro/micro-decision`), so
  neither admits a control-key fact it does not act on: with W1-7
  (`stream-refusal`) and the micro gate's `foreign-control-keys`, every
  admitted control-key fact is one its gate acts on, in its place."
  ([offer settings rows heads] (refusal-with-rows offer settings rows heads nil))
  ([offer settings rows heads key-rows] (refusal-with-rows offer settings rows heads key-rows)))

(def micro-control-keys
  "Control keys only the micro gate acts on: a group's `:members`, its
  making fact there (rig.store.micro `micro-extras`). Envelope's control
  keys hold them, so the parse takes their values as plaintext; at the
  stream gate such a fact is refused `:control-not-allowed` (wave 1, the
  mirror of the micro gate's `foreign-control-keys`), so no act here keeps
  a plaintext value under a key this gate does not act on. A setting key
  in a place no gate acts on is `refusal`'s (step R)."
  #{:members})

(defn stream-refusal
  "This gate's own reason beyond `refusal` (which both gates share):
  `:control-not-allowed` for a fact under a key only the micro gate acts
  on. Checked after `refusal`, before the lock reasons."
  [offer]
  (when (some #(contains? micro-control-keys (:k %)) (:facts offer)) :control-not-allowed))

;; ------------------------------------------------------------------ stamp

(defn with-opened
  "The act's facts with each sealed value fact's `:v` set to the value the
  gate opened for it (`opened`, fact index -> value), so a pure step that
  reads a value (a read entry's moment) sees what the door sealed. Facts
  the map does not name stay as they are. Total."
  [facts opened]
  (if (empty? opened)
    facts
    (into [] (map-indexed (fn [i f] (if (contains? opened i) (assoc f :v (get opened i)) f))) facts)))

(defn plain-texts
  "The facts' value texts for the value index (reads/index-writes' optional
  argument), in the rows' order: a sealed value's canonical text from the
  value the gate opened (`opened`, fact index -> value), else the row's own
  `:v` (a control value's text; nil for a retract). The texts go only into
  `:ix-kv` addresses, which a forget purges by value id."
  [rows opened]
  (into [] (map-indexed (fn [i row] (if (contains? opened i) (env/encode-value (get opened i)) (:v row)))) rows))

(defn stamp-for
  "Ruling 4 as a hybrid clock (rig.store.clock/next-stamp): at or after the
  wall's millisecond, after this task's last stamp `clock`, after every
  stamp the act stood on (carried, P9), after every fact it replaces (read
  from the heads), and after the moment of every read entry fact it carries
  (stage 5a, F1: a read entry is stamped after what it read). `wall` is in
  milliseconds; every other argument holds stamps.

  A read entry's facts are value facts, sealed at the door (stage 2), so
  their moments are read from the values the gate opened: `opened` maps a
  fact's index to its opened value (stage 2's `read-values`), nil when the
  caller holds none (the micro gate's leader never does, M3)."
  ([offer heads clock wall] (stamp-for offer heads clock wall nil))
  ([offer heads clock wall opened]
   (hlc/next-stamp wall clock
                   (concat (vals (:stood-on offer))
                           (keep #(get heads [(:e %) (:k %) (:replaces %)]) (:facts offer))
                           (reads/entry-moments (with-opened (:facts offer) opened))))))

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

(defn intake-offer
  "The intake of a parsed offer: the offer, its digest, and the keys of the
  permission rows, heads rows and key rows the decision reads. `intake`'s
  for a depot record; stage 4's for a store-made act this gate decides (a
  landing into a layer it orders), which never passes the depot's parse.

  Phase 6's key rows are here, not in `intake` alone (wave 2's merge,
  W2-1): a landing into a layer this gate orders is decided under that
  layer's grammars, as any act is, since both go through
  `gate-event/record-or-decide>`."
  [o]
  {:offer o
   :digest (env/digest o)
   :pids (pids-to-read o)
   :heads (heads-to-read o)
   ;; phase 6: the key rows the decision reads (4.7 step 2)
   :keys (grammar/rows-to-read (:facts o) store-key?)})

(defn intake
  "The gate's first step on a raw depot record. Total. {:refuse reason} for
  a record refused on its face; else `intake-offer` over the parsed offer."
  [raw]
  (try
    (let [p (env/parse raw :stream)]
      (if (contains? p :refuse)
        p
        (intake-offer (:ok p))))
    (catch Throwable _ {:refuse :gate-error})))

;; ----------------------------------------------------------------- decide

(defn- log-rows
  "The act's rows: stage 1's {:e :k :v :replaces :mark}, `:v` the control
  value's text or nil, and for a value fact the fields the lock effects
  add (`:sealed` as offered, `:lock-id`, `:lock`, `:digest`)."
  [offer extra]
  (into [] (map-indexed (fn [i f] (merge {:e (:e f) :k (:k f) :v (env/encode-value (:v f))
                                          :replaces (:replaces f) :mark (:mark f)}
                                         (get extra i))))
        (:facts offer)))

(defn- answer-record
  "The name's answer for ever (D4): a yes and a recorded no alike keep the
  act's bounded parts; what it stood on is kept beside it, for a yes (F2).
  Its subject slot (ruling 8, for finding) is the act's union: the owner,
  what it carried, and, stage 2, what the grammar read in its values
  (`union`, when within the 256 cap, L13); the root actor is no subject."
  [offer settings reason stamp digest union]
  (let [owner (locks/person-owner (owner-in-force offer settings))]
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
     :subjects (cond-> (into #{} (:subjects offer))
                 (and union (<= (count union) locks/max-subjects)) (into union)
                 owner (conj owner))}))

(defn- first-per-pid
  "[pid fid] pairs keeping the first per pid: within one act, as across
  acts, the first grant stays the index's grant (P8)."
  [pairs]
  (second (reduce (fn [[seen out] [p _ :as pair]]
                    (if (contains? seen p) [seen out] [(conj seen p) (conj out pair)]))
                  [#{} []] pairs)))

(defn- decide*
  ([offer settings rows heads clock wall digest]
   (decide* offer settings rows heads clock wall digest (locks/empty-context offer settings)))
  ([offer settings rows heads clock wall digest lx]
   (let [nm (:name offer)
         ;; phase 6: the rows of the act's keys, read in this event (decision-reads>)
         key-rows (:key-rows lx)
         reason (or (refusal offer settings rows heads (or key-rows {}))
                    (stream-refusal offer)
                    (locks/lock-refusal offer settings lx))
         ;; the values the gate opened (stage 2's `read-values`, by fact index)
         opened (:values (:read lx))
         stamp (stamp-for offer heads clock wall opened)
         yes? (nil? reason)
         fx (locks/lock-effects offer settings lx (when yes? stamp))
         facts (:facts offer)
         indexed (map-indexed vector facts)
         setting-updates (into {} (for [f facts :when (setting-fact? offer f)]
                                    [(env/setting-keys (:k f)) (:v f)]))
         log (when yes? (log-rows offer (:rows fx)))
         ;; stage 5a: the admitted act's index entries, from the rows as written and
         ;; the texts the gate opened (a failure there is this function's throw, so
         ;; decide's :gate-error road)
         ix (if yes?
              (reads/index-writes (reads/hints-of key-rows) (:layer offer) nm log stamp (plain-texts log opened))
              reads/no-index-writes)
         _ (when (:index-error ix) (throw (ex-info "index writes failed" {:name nm})))]
     {:kind :decide
      :stamp stamp
      :record (answer-record offer settings reason stamp digest (:union (:read lx)))
      :ack (merge (ack (if yes? :yes :no) reason stamp nm) (:ack fx))
      ;; stage 2's lock writes (locks/lock-effects): for a no, the consumption only
      :locks fx
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
      ;; phase 6: the key rows the act changes, each whole (empty for a no)
      :key-rows (if yes? (grammar/key-row-writes facts (or key-rows {}) nm stamp store-key?) [])
      ;; stage 5a: the three index write lists (empty for a no)
      :index-put (:index-put ix)
      :index-of (:index-of ix)
      :index-del (:index-del ix)})))

(defn decide
  "Decide a fresh offer (no record under its name on this task): the answer
  record, the ack and every write, precomputed; the stamp is given for a yes
  and a no alike. `lx` is the lock context the event read (stage 2); the
  7-arity decides with none (stage 1's pure tests). Total: a failure inside
  is the unrecorded face refusal :gate-error, never an exception in the
  topology (F6)."
  ([offer settings rows heads clock wall digest]
   (decide offer settings rows heads clock wall digest (locks/empty-context offer settings)))
  ([offer settings rows heads clock wall digest lx]
   (try
     (decide* offer settings rows heads clock wall digest lx)
     (catch Throwable _
       {:kind :face :ack (ack :no :gate-error nil (:name offer))}))))

(defn no-such-lock-ack
  "The answer to an offer a cited lock of which was not delivered (plan,
  'The missing lock'): refused on its face, through the ack only, nothing
  recorded, consumed or written."
  [nm]
  (ack :no :no-such-lock nil nm))
