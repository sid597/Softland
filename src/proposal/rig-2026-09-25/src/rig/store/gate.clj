;; IMPORTANT: Before modifying this file, re-read PLAN-stream-store.md.
(ns rig.store.gate
  "The stream gate's decision, pure and total: the rig's counterpart of
  model.clj's `stream-step`, `refusal` and `stamp-for` under `baseline`.

  The topology reads what the decision needs on the layer's home task,
  calls `decide`, and writes what it returns, in one event. Nothing here
  throws on parsed input; `decide` is wrapped as a last line anyway."
  (:require [rig.store.envelope :as env])
  (:import [com.rpl.rama.helpers TopologyUtils]))

(defn wall-now
  "The gate's wall clock, simulated in tests."
  []
  (TopologyUtils/currentTimeMillis))

(defn perm-entity
  "The entity a person's permission facts are about (model.clj `perm-entity`)."
  [who]
  (keyword (str "perm-" (name who))))

;; ------------------------------------------------------------ control facts

(def layer-kinds #{:personal :hand :agent :group :base})
(def grains #{:per-value :per-act})

(defn- pid? [x] (and (vector? x) (= 3 (count x)) (every? keyword? x)))

(defn setting-fact?
  "A fact on the layer's own entity with a setting key (P10)."
  [offer f]
  (and (= (:layer offer) (:e f)) (contains? env/setting-keys (:k f))))

(defn control-fact?
  "A fact the gate projects: a setting, a grant or a revocation."
  [offer f]
  (or (setting-fact? offer f) (contains? #{:permission :revoke} (:k f))))

(defn- control-value-ok?
  "Whether a control fact's value has the shape its projection needs; a
  wrong shape would violate a schema inside the topology."
  [offer f]
  (let [v (:v f)]
    (if (setting-fact? offer f)
      (case (:k f)
        :kind (contains? layer-kinds v)
        :owner (keyword? v)
        :class (contains? env/classes v)
        :lock-grain (contains? grains v))
      (case (:k f)
        :permission (and (map? v) (pid? (:id v)) (= #{:id} (set (keys v))))
        :revoke (and (map? v) (pid? (:permission v)) (= #{:permission} (set (keys v))))))))

(defn- control-allowed?
  "Who may write a control fact (rig choice): the operator and the store any;
  the layer's owner only a lock-grain switch (P10: the owner's own permission
  covers a grain switch). Grants and revocations are the operator's (P8)."
  [offer settings f]
  (or (contains? env/root-actors (:who offer))
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
  "The layer's owner: its owner fact, else the act's own owner fact."
  [offer settings]
  (or (:owner settings)
      (some #(when (and (setting-fact? offer %) (= :owner (:k %))) (:v %)) (:facts offer))))

;; ------------------------------------------------------------------ reads

(defn pids-to-read
  "The permission rows the decision reads on the home task: the cited one
  (a person's offer), and every grant's and revocation's target."
  [offer]
  (vec (distinct
        (concat (when-not (contains? env/root-actors (:who offer))
                  (when (pid? (:permission offer)) [(:permission offer)]))
                (for [f (:facts offer)
                      :let [p (case (:k f)
                                :permission (get-in f [:v :id])
                                :revoke (get-in f [:v :permission])
                                nil)]
                      :when (pid? p)]
                  p)))))

(defn heads-to-read
  "The heads rows the decision reads: [e k r] for each replacing fact."
  [offer]
  (vec (distinct (for [f (:facts offer) :when (:replaces f)] [(:e f) (:k f) (:replaces f)]))))

;; --------------------------------------------------------------- refusal

(defn refusal
  "Why the gate refuses the act, or nil: model.clj `refusal` in its order,
  with the rig's own reasons placed as PLAN-stream-store.md P7 says.
  `settings` is the layer's settings, `rows` the permission rows read
  (pid -> row), `heads` the heads read ([e k r] -> stamp or nil)."
  [offer settings rows heads]
  (let [facts (:facts offer)
        who (:who offer)
        root? (contains? env/root-actors who)
        [pw pl pin :as pid] (:permission offer)
        rs (keep :replaces facts)]
    (cond
      (some #(and (:layer %) (not= (:layer offer) (:layer %))) facts)
      :fact-outside-the-acts-layer

      (nil? (class-in-force offer settings))
      :no-such-layer

      (not= (:class offer) (class-in-force offer settings))
      :class-mismatch

      (and (not root?) (or (nil? pid) (not= pw who) (not= pl (:layer offer))))
      :permission-does-not-cover-this

      (and (not root?) (not= pin (:layer offer)))
      :permission-from-another-layer

      (and (not root?) (nil? (:granted (get rows pid))))
      :no-permission

      (and (not root?) (:revoked (get rows pid)))
      :permission-revoked

      (some #(and (control-fact? offer %) (not (control-value-ok? offer %))) facts)
      :malformed-control

      (some #(and (control-fact? offer %) (not (control-allowed? offer settings %))) facts)
      :control-not-allowed

      (or (not= (count rs) (count (set rs)))
          (some #(and (:replaces %) (nil? (get heads [(:e %) (:k %) (:replaces %)]))) facts))
      :stale-replaces

      (some #(and (= :revoke (:k %))
                  (let [row (get rows (get-in % [:v :permission]))]
                    (or (nil? (:granted row)) (:revoked row))))
            facts)
      :stale-revoke)))

;; ------------------------------------------------------------------ stamp

(defn stamp-for
  "Ruling 4 as the model's hybrid clock: at or after the wall, after this
  task's last stamp, after every stamp the act stood on (carried, P9) and
  after every fact it replaces (read from the heads)."
  [offer heads clock wall]
  (apply max wall (inc clock)
         (concat (map inc (vals (:stood-on offer)))
                 (keep #(some-> (get heads [(:e %) (:k %) (:replaces %)]) inc) (:facts offer)))))

;; ----------------------------------------------------------------- decide

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
  [parsed raw]
  (ack :no (:refuse parsed) nil (env/name-of raw)))

(defn- log-rows [offer]
  (mapv (fn [f] {:e (:e f) :k (:k f) :v (env/encode-value (:v f))
                 :replaces (:replaces f) :mark (:mark f)})
        (:facts offer)))

(defn- answer-record [offer settings reason stamp digest]
  {:answer (if reason :no :yes)
   :reason reason
   :stamp stamp
   :digest digest
   :who (:who offer)
   :class (:class offer)
   :permission (:permission offer)
   :session (:session offer)
   :stood-on (:stood-on offer)
   :because-of (:because-of offer)
   :claimed-when (:claimed-when offer)
   :subjects (cond-> (set (:subjects offer))
               (owner-in-force offer settings) (conj (owner-in-force offer settings)))})

(defn- decide*
  [offer settings rows heads clock wall digest]
  (let [nm (:name offer)
        reason (refusal offer settings rows heads)
        stamp (stamp-for offer heads clock wall)
        yes? (nil? reason)
        facts (:facts offer)
        indexed (map-indexed vector facts)]
    {:kind :decide
     :stamp stamp
     :record (answer-record offer settings reason stamp digest)
     :ack (ack (if yes? :yes :no) reason stamp nm)
     :log (when yes? (log-rows offer))
     :heads-del (if yes? (vec (for [f facts :when (:replaces f)] [(:e f) (:k f) (:replaces f)])) [])
     :heads-put (if yes? (vec (for [[i f] indexed] [[(:e f) (:k f) [nm (long i)]] stamp])) [])
     :settings (if yes?
                 (into {} (for [f facts :when (setting-fact? offer f)] [(env/setting-keys (:k f)) (:v f)]))
                 {})
     :grants (if yes?
               (vec (for [[i f] indexed
                          :when (= :permission (:k f))
                          :let [p (get-in f [:v :id])]
                          :when (nil? (:granted (get rows p)))]
                      [p [nm (long i)]]))
               [])
     :revokes (if yes?
                (vec (for [[i f] indexed :when (= :revoke (:k f))]
                       [(get-in f [:v :permission]) [nm (long i)]]))
                [])}))

(defn decide
  "Decide a fresh offer (no record under its name on this task): the answer
  record, the ack, and every write, precomputed. Total: a failure inside is
  a refusal, never an exception in the topology."
  [offer settings rows heads clock wall digest]
  (try
    (decide* offer settings rows heads clock wall digest)
    (catch Throwable _
      {:kind :face :ack (ack :no :malformed nil (:name offer))})))
