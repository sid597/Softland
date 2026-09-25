;; IMPORTANT: Before modifying this file, re-read PLAN-micro-store.md (§A
;; "The door", §B, §C, §D, M13, M15, M25) and BUILD_NOTES-micro-store.md.
(ns rig.store.micro-client
  "The offerer's side of the micro store (PLAN-micro-store.md, M13): the
  door that leases, takes its locks by query, seals each value at the door
  and offers the sealed act; the reads through the settled frontier; the
  seeding of the shared layers and the base (M11, §C); opening a session
  (§B); and the dispatch of an offer to the gate its name is tagged for
  (M13, M25).

  It is its own namespace because `rig.store.module` requires
  `rig.store.micro` for its one line, and the door needs the module's
  name and the stream side's client (BUILD_NOTES, 'Build-level choices').
  Plain Clojure over the foreign API, in the same process in the rig.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [rig.store.client :as c]
            [rig.store.envelope :as env]
            [rig.store.gate :as gate]
            [rig.store.locks :as locks]
            [rig.store.micro :as micro]))

;; ================================================================= handles

(defn connect
  "The stream side's handles (`rig.store.client/connect`) with the micro
  store's, taken once."
  [cluster]
  (let [st (c/connect cluster)
        mn (:module-name st)]
    (assoc st
           :micro-depot (foreign-depot cluster mn "*micro-offers")
           :persons-depot (when micro/persons-placeholder? (foreign-depot cluster mn "*persons-placeholder"))
           :micro (foreign-pstate cluster mn "$$micro")
           :names (foreign-pstate cluster mn "$$micro-names")
           :task (foreign-pstate cluster mn "$$micro-task")
           :persons (foreign-pstate cluster mn "$$persons")
           :lookup-q (foreign-query cluster mn "micro-lookup")
           :act-q (foreign-query cluster mn "micro-act")
           :lease-q (foreign-query cluster mn "micro-lease")
           :leases-of-q (foreign-query cluster mn "micro-leases-of"))))

(defn- retrying
  "A foreign call, retried while a worker restarts after a crash (a read
  that throws is 'not yet', §G)."
  ([f] (retrying f 60000))
  ([f timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop []
       (let [r (try [:ok (f)] (catch Exception e [:err e]))]
         (cond (= :ok (first r)) (second r)
               (> (System/currentTimeMillis) deadline) (throw (second r))
               :else (do (Thread/sleep 100) (recur))))))))

;; ================================================================== reads

(defn frontier
  "The frontier as one task holds it (§D): any task will do."
  ([store] (frontier store 0))
  ([store pkey] (retrying #(foreign-select-one [(keypath :frontier)] (:task store) {:pkey pkey}))))

(defn clock-of
  "The micro clock of the task entity `e` hashes to (a hybrid stamp)."
  [store e]
  (retrying #(or (foreign-select-one [(keypath :clock)] (:task store) {:pkey e}) 0)))

(defn lookup
  "micro-lookup (RD1 through the frontier, §D): by name, the parts digest
  (nil: the record as data), the envelope fingerprint and F (nil: the
  task's own)."
  ([store nm digest fp] (lookup store nm digest fp nil))
  ([store nm digest fp F] (retrying #(foreign-invoke-query (:lookup-q store) nm digest fp F))))

(defn act
  "micro-act: an act's record and rows on one entity, through F."
  ([store e nm] (act store e nm nil))
  ([store e nm F] (retrying #(foreign-invoke-query (:act-q store) e nm F))))

(defn record-of
  "The name row as stored, bypassing the frontier (tests only)."
  [store nm]
  (retrying #(foreign-select-one [(keypath nm :answer)] (:names store))))

(defn faces-of
  "Every faces entry under a name, bypassing the frontier (tests only)."
  [store nm]
  (retrying #(into {} (foreign-select [(keypath nm :faces) ALL] (:names store)))))

(defn lease-rows
  "A lease's rows as stored, bypassing the frontier (tests only)."
  [store lease-name]
  (retrying #(into {} (foreign-select [(keypath lease-name :leases) ALL] (:names store)))))

(defn settings-of
  "A layer's settings in force: this store's latest version, else the
  stream store's (M5)."
  [store L]
  (retrying #(or (last (foreign-select [(keypath L :settings) (sorted-map-range-to-end 1) MAP-VALS] (:micro store)))
                 (c/settings store L))))

(defn settings-versions
  "Every settings version this store keeps for a layer, by batch."
  [store L]
  (retrying #(into (sorted-map) (foreign-select [(keypath L :settings) ALL] (:micro store)))))

(defn permission-of
  "A permission's rows in both stores (M5)."
  [store L pid]
  (retrying #(hash-map :micro (foreign-select-one [(keypath L :permissions pid)] (:micro store))
                       :stream (c/permission store L pid))))

(defn head-of
  "A micro head of fact `fid` for (e k) in layer L, with its replacer."
  [store e L k fid]
  (retrying #(foreign-select-one [(keypath e :heads [L k fid])] (:micro store))))

(defn tombstone-of
  "The tombstone this store keeps for a stream-era head it replaced (M5)."
  [store L e k fid]
  (retrying #(foreign-select-one [(keypath L :replaced [e k fid])] (:micro store))))

(defn members-of [store L]
  (retrying #(into {} (foreign-select [(keypath L :members) ALL] (:micro store)))))

(defn stood-on-of [store nm]
  (retrying #(into {} (foreign-select [(keypath nm :stood-on) ALL] (:names store)))))

(defn person-entry
  "A person's entry in `$$persons` (tests and the reader here only: a
  person lock never leaves the module in a kept store)."
  [store p]
  (retrying #(foreign-select-one [(keypath p)] (:persons store) {:pkey p})))

(defn lock-row
  "A value's lock row on its entity's task, when its lock is a row."
  [store e lid]
  (retrying #(foreign-select-one [(keypath e :locks lid)] (:micro store))))

;; ======================================================= persons (the seam)

(defn- put-person! [store p entry]
  (if-let [d (:persons-depot store)]
    (foreign-append! d {:person p :entry entry} :ack)
    (throw (ex-info "persons are phase 2's once its gate merges: make them by its :people acts" {:person p}))))

(defn make-person!
  "A person with a fresh lock (phase 2's person act's effect), through the
  placeholder seam (`rig.store.micro/persons-placeholder?`)."
  [store p]
  (put-person! store p {:lock (locks/fresh-lock) :erased-at nil}))

(defn forget-person!
  "A person forget's effect (phase 2's): the lock destroyed, the date kept,
  on every task, through the placeholder seam."
  [store p stamp]
  (put-person! store p {:lock nil :erased-at (long stamp)}))

;; ============================================================ the envelope

(defn build
  "A micro offer ready to seal and send: `rig.store.client/build` with the
  class by entity unless given (its name `[layer class scheme uuid7]`, the
  class nil for an act the store places itself, P4)."
  [spec]
  (c/build (merge {:class :by-entity} spec)))

(defn value-fact?
  "A fact the door seals: not a control key, and a value (a retract, `:v
  nil`, has nothing to seal, L14)."
  [f]
  (and (not (contains? micro/control-keys (:k f))) (some? (:v f))))

(defn value-count [offer] (count (filter value-fact? (:facts offer))))

(defn seal
  "The door's sealing (phase 2's): each value fact's `:v` sealed under the
  lock `assign` gives its index (a lock id), citing that id; control facts
  and retracts as they are. `locks` maps lock id to the lock."
  [offer assign locks]
  (update offer :facts
          (fn [fs]
            (let [vi (volatile! -1)]
              (mapv (fn [f]
                      (if (value-fact? f)
                        (let [lid (assign (vswap! vi inc))]
                          (-> f
                              (dissoc :v)
                              (assoc :sealed (locks/seal (get locks lid) (locks/canonical-bytes (:v f)))
                                     :lock-id lid)))
                        f))
                    fs)))))

(defn digest-of
  "The parts digest the gate will compute for this envelope (phase 2's)."
  [offer]
  (some-> (micro/parse-micro offer) :ok micro/parts-digest))

(defn fp-of "The envelope fingerprint (M8 revised)." [offer] (micro/envelope-fp offer))

;; ================================================================ offering

(defn send!
  "Append one sealed envelope to the micro depot, durable before it returns
  (`:append-ack`; a microbatch depot has no processing ack)."
  [store offer]
  (foreign-append! (:micro-depot store) offer :append-ack))

(defn await-answer
  "Poll micro-lookup for this envelope at 50 ms (I-G3, M15) until it answers
  or `timeout-ms` passes; a read that throws is 'not yet' (§G). The last
  result, `{:answer :no-answer ...}` on a timeout."
  ([store offer] (await-answer store offer 60000))
  ([store offer timeout-ms]
   (let [nm (:name offer) d (digest-of offer) fp (fp-of offer)
         deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop []
       (let [r (try (foreign-invoke-query (:lookup-q store) nm d fp nil) (catch Exception _ nil))]
         (if (or (and r (not= :no-answer (:answer r))) (> (System/currentTimeMillis) deadline))
           (or r {:answer :no-answer})
           (do (Thread/sleep 50) (recur))))))))

(defn offer!
  "Send an envelope and wait for its answer; after a send error, look its
  answer up and resend the same map while there is none (P6's road, I-G3)."
  ([store offer] (offer! store offer 60000))
  ([store offer timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop []
       (let [sent (try (send! store offer) true (catch Exception _ false))
             r (await-answer store offer (if sent timeout-ms 2000))]
         (if (or (not= :no-answer (:answer r)) (> (System/currentTimeMillis) deadline))
           r
           (recur)))))))

;; ================================================================== leases

(defn lease-offer
  "A lease act (phase 2's L20, §A): an ordinary act into the layer by
  `who` in `session`, citing a permission there, with one control fact
  `{:e session :k :lease :v {:count n}}`."
  [{:keys [who layer session permission n class]}]
  (build {:who who :layer layer :class (or class :by-entity)
          :permission (when-not (contains? gate/exempt-actors who) permission)
          :session session
          :facts [{:e session :k :lease :v {:count n}}]}))

(defn lease!
  "Offer a lease act and wait for its answer. {:name :answer :ids}: the ids
  are a function of the name and the count (M16), so the door needs no ack."
  [store spec]
  (let [o (lease-offer spec)
        a (offer! store o)]
    {:name (:name o) :offer o :answer a :ids (locks/lease-ids (:name o) (:n spec))}))

(defn take-locks
  "The plaintext locks of a lease, by micro-lease once its batch is at or
  below the frontier (§A): {lock-id K}. Empty on a timeout, or when the
  lease was refused or its rows consumed."
  ([store lease-name] (take-locks store lease-name 30000))
  ([store lease-name timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop []
       (let [r (try (foreign-invoke-query (:lease-q store) lease-name nil) (catch Exception _ nil))
             ks (:locks r)]
         (if (or (seq ks) (> (System/currentTimeMillis) deadline))
           (into {} (map (fn [[i K]] [[(into [] lease-name) (long i)] K])) ks)
           (do (Thread/sleep 50) (recur))))))))

(defn assign-by-grain
  "Which lease id each value fact cites: its own under per value, the
  first under per act (L30)."
  [grain ids]
  (fn [vi] (if (= :per-act grain) (first ids) (nth ids vi))))

(defn write!
  "The door end to end for one act (§A 'The door'): lease as many locks as
  the grain in force needs (by `:lease-who`, default the act's `:who`,
  under the act's own permission and session), take them, seal each value,
  offer the sealed act, and wait. An act with no value to seal is offered
  as it is. {:answer :offer :lease :locks}."
  [store spec & {:keys [lease-who lease-permission grain]}]
  (let [o (build spec)
        n (value-count o)]
    (if (zero? n)
      {:answer (offer! store o) :offer o}
      (let [g (or grain (:grain (settings-of store (:layer o))) :per-value)
            lw (or lease-who (:who o))
            l (lease! store {:who lw :layer (:layer o) :session (:session o) :class (:class o)
                             :permission (or lease-permission (:permission o))
                             :n (if (= :per-act g) 1 n)})
            ks (if (= :yes (get-in l [:answer :answer])) (take-locks store (:name l)) {})
            sealed (seal o (assign-by-grain g (:ids l)) ks)]
        {:answer (offer! store sealed) :offer sealed :lease l :locks ks}))))

;; ========================================================= opening values

(defn open-row
  "What a reader holding the person locks sees of one row (phase 2's
  `locks/open-with`, the pure half of `open-value>`): {:value v}, {:erased-at
  d} or {:unreadable reason}. The reader here is the test's (it reads
  `$$persons`), standing in for the reads stage."
  [store e row stamp]
  (let [record (or (:lock row) (when (:lock-id row) (lock-row store e (:lock-id row))))
        persons (into {} (map (fn [p] [p (person-entry store p)])) (some-> record locks/wrap-persons))
        ledger (when (:lock-id row) (retrying #(foreign-select-one [(keypath e :erased (:lock-id row))] (:micro store))))]
    (locks/open-with row stamp nil ledger record persons)))

(defn open-act
  "Every row of an act on one entity, opened as `open-row` does, in index
  order, through F."
  ([store e nm] (open-act store e nm nil))
  ([store e nm F]
   (let [{:keys [record rows]} (act store e nm F)]
     (mapv (fn [[_ row]] (open-row store e row (:stamp record))) rows))))

;; ============================================================ control acts

(defn grant-offer
  "The operator's grant of `pid` in the layer it lives in (its `in`),
  through the micro gate."
  [[who _ in :as pid]]
  (build {:who :operator :layer in :class :by-entity
          :facts [{:e (gate/perm-entity who) :k :permission :v {:id pid}}]}))

(defn revoke-offer
  "The operator's revoke of `pid` in its layer, standing on its grant
  (P8): the grant's fact id and stamp, from either store (M5)."
  [store [who _ in :as pid]]
  (let [{:keys [micro stream]} (permission-of store in pid)
        gfid (or (:granted micro) (:granted stream))
        gname (first gfid)
        gstamp (when gname (or (:stamp (record-of store gname)) (:stamp (c/record store gname))))]
    (build {:who :operator :layer in :class :by-entity
            :stood-on (if (and gfid gstamp) {gfid gstamp} {})
            :facts [{:e (gate/perm-entity who) :k :revoke :v {:permission pid}}]})))

;; ========================================================= the dispatch (M13)

(defn gate-for
  "Which gate an offer goes to (M13, M25): its name's tag class, `:by-layer`
  to the stream gate and `:by-entity` to this one; a nil-tagged act (the
  store places it) by its layer: this gate for a layer this store orders
  (a shared one: its settings are this store's), and for a `:forget` whose
  target was admitted here (M25); else the stream gate (P16)."
  [store offer]
  (let [[L cls] (env/name-tag (:name offer))]
    (case cls
      :by-layer :stream
      :by-entity :micro
      (let [micro-layer? (seq (settings-versions store L))
            target (some (fn [f] (when (= :forget (:k f)) (get-in f [:v :target]))) (:facts offer))]
        (cond
          micro-layer? :micro
          (and target (= :by-entity (nth (nth target 0) 1))) :micro
          :else :stream)))))

(defn send-any!
  "Offer to the gate the offer's name is tagged for (M13): the stream
  gate's answer through its ack, or this gate's through micro-lookup."
  [store offer]
  (if (= :micro (gate-for store offer))
    (offer! store offer)
    (c/offer-until-answered! store offer)))

;; ================================================================= seeding

(def shared-world
  "The shared side of the model's world (model.clj `layers`, `permissions`;
  M11 revised, §B, §C): the group, its root held by the group itself and
  Alice's and Bob's beneath it; the base, made on the stream gate as a
  one-owner layer of the root actor, its root held by the operator and
  Alice's and Bob's beneath it."
  {:group {:members #{:alice :bob}
           :root [:group :group :group]
           :grants [[:alice :group :group [:group :group :group]]
                    [:bob :group :group [:group :group :group]]]}
   :base {:root [locks/root-actor :base :base]
          :grants [[:alice :base :base [locks/root-actor :base :base]]
                   [:bob :base :base [locks/root-actor :base :base]]]}})

(defn make-base!
  "The base (§C, default R8): the operator's making act on the stream gate,
  one-owner, `:kind :base`, owned by the root actor, with its root
  permission granted in the making act (P10); then Alice's and Bob's
  beneath it. The stream gate's answers."
  ([store] (make-base! store (:base shared-world)))
  ([store {:keys [root grants]}]
   (let [made (c/offer-until-answered!
               store (c/build {:who :operator :layer :base :class :by-layer
                               :facts [{:e :base :k :kind :v :base}
                                       {:e :base :k :owner :v locks/root-actor}
                                       {:e :base :k :class :v :by-layer}
                                       {:e :base :k :lock-grain :v :per-value}
                                       {:e (gate/perm-entity (nth root 0)) :k :permission :v {:id root}}]}))
         granted (mapv #(c/offer-until-answered! store (c/grant-offer store %)) grants)]
     (into [made] granted))))

(defn reclass-offer
  "The re-class of a one-owner layer to by entity (OP7, P16): the
  operator's act on the stream gate, placed by the store (its name has no
  class)."
  [store L]
  (c/build {:who :operator :layer L :class (or (:class (c/settings store L)) :by-layer)
            :facts [{:e L :k :class :v :by-entity}]}))

(defn make-group!
  "A group (§C, M22): while the base is placed by layer, first its re-class
  on the stream gate, waited for; then the group's making act on this
  gate, standing on the re-class fact with its stamp carried (M6), with
  its kind, members, class, grain, its root `[L L L]` held by the group,
  and the members' permissions beneath it (P10's grants in the making
  act). {:reclass stream-answer-or-nil :made micro-answer :offer offer}."
  [store L {:keys [members root grants]}]
  (let [reclass (when (= :by-layer (:class (c/settings store :base)))
                  (let [o (reclass-offer store :base)
                        a (c/offer-until-answered! store o)]
                    {:offer o :answer a}))
        stood (if-let [{:keys [offer answer]} reclass]
                (if (= :yes (:answer answer)) {[(:name offer) 0] (:stamp answer)} {})
                {})
        o (build {:who :operator :layer L :stood-on stood
                  :facts (into [{:e L :k :kind :v :group}
                                {:e L :k :members :v (set members)}
                                {:e L :k :class :v :by-entity}
                                {:e L :k :lock-grain :v :per-value}
                                {:e (gate/perm-entity (nth root 0)) :k :permission :v {:id root}}]
                               (for [pid grants] {:e (gate/perm-entity (nth pid 0)) :k :permission :v {:id pid}}))})]
    {:reclass reclass :made (offer! store o) :offer o}))

(defn seed-shared!
  "The shared side of the world, before any history (M11 revised): the
  base on the stream gate, then the group here (re-classing the base
  first). {:base [answers] :group make-group!'s result}."
  [store]
  (let [base (make-base! store)
        group (make-group! store :group (:group shared-world))]
    {:base base :group group}))

(defn person-permission
  "The person's own permission in a layer: `[p L L]` where p owns L, else
  `[p L L root]` beneath the layer's root."
  [p L]
  (case L
    :group [p :group :group [:group :group :group]]
    :base [p :base :base [locks/root-actor :base :base]]
    [p L L]))

(defn open-session!
  "Opening a session (§B, default R7): through each layer's own gate, a
  grant `[S L L p]` beneath the person's permission p in L, by the
  operator (R13). {L answer}."
  [store S p layers]
  (into {}
        (for [L layers
              :let [pid [S L L (person-permission p L)]
                    micro? (= :by-entity (:class (settings-of store L)))]]
          [L (if micro?
               (offer! store (grant-offer pid))
               (c/offer-until-answered! store (c/grant-offer store pid)))])))
