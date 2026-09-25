;; IMPORTANT: Before modifying this file, re-read PLAN-stream-store.md ("The
;; client side") and PLAN-locks-and-forgetting.md ("The door", "The door and
;; the lease road").
(ns rig.store.client
  "The offerer's side, the door (PLAN-stream-store.md, 'The client side';
  PLAN-locks-and-forgetting.md, 'The door'): make a name, build an offer,
  seal its values under leased locks, send it, resend it under the same name
  after an error, and read the answer by name plus layer. Plain Clojure over
  the foreign API, in the same process in the rig; operator code, trusted
  (ruled: locks are for forgetting, not for hiding from the operator).

  The door never sends a lock or a plaintext value through the depot. For
  an act with value facts into layer L under session s it leases locks (a
  lease act into L, answered by name), takes their plaintext by the
  `lease-locks` query, seals each value under one of them (one per value
  under per-value grain, one for the act under per-act) and cites its id.
  Its held locks are an atom in the store handle, not store state; a
  restarted door (a new handle) takes its unconsumed locks again by the same
  query."
  (:require [clojure.set :as set]
            [com.rpl.rama :as rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [rig.store.envelope :as env]
            [rig.store.gate :as gate]
            [rig.store.locks :as locks]
            [rig.store.module :as m]))

(def lease-size
  "How many locks the door leases at a time when it holds too few (rig
  choice: the plan's cost model counts a lease per 64 value facts); more,
  up to 256 a lease, when one act needs more."
  64)

(defn- fresh-door
  "The door's memory: unused leased locks per [layer session], the ids it
  has handed to offers (never pooled again), the grain each layer had at
  its last `lease-locks`, the locks each unanswered offer was sealed under,
  and the person acts it made (for a person forget's stood-on)."
  []
  {:pool {} :issued {} :grain {} :held {} :made {}})

(defn connect
  "Handles on the store, taken once, and a fresh door memory. A second
  `connect` is a restarted door: it holds no locks until it takes them
  again."
  [cluster]
  (let [mn (get-module-name m/Store)]
    {:module-name mn
     :depot (foreign-depot cluster mn "*offers")
     :layers (foreign-pstate cluster mn "$$layers")
     :clock (foreign-pstate cluster mn "$$clock")
     :persons (foreign-pstate cluster mn "$$persons")
     :lease-locks-q (foreign-query cluster mn "lease-locks")
     :read-as-of-q (foreign-query cluster mn "read-as-of")
     :door (atom (fresh-door))}))

(defn forget-door!
  "Drop everything the door holds, as a door process restart would (the
  plan's lost-pool case, [V-F1])."
  [store]
  (reset! (:door store) (fresh-door)))

(defn default-session
  "The session a door writes in when an offer with value facts names none
  (rig choice): a lease needs a session, and its rows are keyed by it."
  [who]
  (keyword "door" (name who)))

(defn build
  "An offer ready to send: version marker, defaults, and a fresh name unless
  one is given. Pure: the map it returns is what is sent and what is resent
  (the digest covers `:claimed-when` and `:stood-on`, so a resend is this
  map, never a rebuilt one). `:claimed-when` defaults to the offerer's wall
  clock in milliseconds: the offerer's claim, not a stamp. An offer with a
  value fact and no session is built in the writer's `default-session`.
  Its value facts stay plaintext here; the door seals them as it sends."
  [{:keys [name who layer class permission facts stood-on subjects because-of session claimed-when]}]
  (let [offer {:version env/version
               :who who
               :layer layer
               :class class
               :permission permission
               :session (or session (when (and (keyword? who) (some env/value-fact? facts)) (default-session who)))
               :stood-on (or stood-on {})
               :because-of because-of
               :claimed-when (or claimed-when (System/currentTimeMillis))
               :subjects (or subjects #{})
               :facts (vec facts)}]
    (assoc offer :name (or name (env/name-for offer)))))

(declare offer-until-answered! lease-locks)

;; ------------------------------------------------------------ the door

(defn- value-indices [offer]
  (if (sequential? (:facts offer))
    (into [] (keep-indexed (fn [i f] (when (env/value-fact? f) i))) (:facts offer))
    []))

(defn- refresh!
  "Take the session's unconsumed locks by `lease-locks` into the pool, all
  but the ids already handed to offers, and note the grain in force; the
  ids the query no longer returns are consumed and forgotten."
  [store layer session]
  (let [{:keys [grain] leased :locks} (lease-locks store layer session)
        k [layer session]
        door (:door store)]
    (locking door
      (swap! door (fn [d]
                    (let [issued (set/intersection (get-in d [:issued k] #{}) (set (keys leased)))]
                      (-> d
                          (assoc-in [:grain layer] grain)
                          (assoc-in [:issued k] issued)
                          (update-in [:pool k] merge (into {} (remove (fn [[id _]] (contains? issued id))) leased)))))))
    grain))

(defn- take!
  "Take `n` pooled locks of [layer session], marking them handed out, or
  nil when the pool holds fewer."
  [store layer session n]
  (let [door (:door store)
        k [layer session]]
    (locking door
      (let [pool (get-in @door [:pool k])]
        (when (<= n (count pool))
          (let [picked (vec (take n (sort-by (comp str first) pool)))]
            (swap! door (fn [d] (-> d
                                    (update-in [:pool k] #(apply dissoc % (map first picked)))
                                    (update-in [:issued k] (fnil into #{}) (map first picked)))))
            picked))))))

(defn- lease-for!
  "Lease `n` locks for an offer: a lease act by its writer, citing its
  permission, in its layer and session, tagged with its class."
  [store offer n]
  (offer-until-answered!
   store
   (build {:who (:who offer) :layer (:layer offer) :class (:class offer)
           :permission (:permission offer) :session (:session offer)
           :facts [{:e (:session offer) :k :lease :v {:count n}}]})))

(defn- held-for
  "The locks an offer is sealed under: per value fact index its lock id,
  and each id's lock. Under per-act grain every value fact cites the one."
  [idx picked grain]
  (let [ids (if (= :per-act grain)
              (zipmap idx (repeat (first (first picked))))
              (zipmap idx (map first picked)))]
    {:ids ids :keys (into {} picked)}))

(defn- assign!
  "Pick the locks for an offer's value facts: from the pool, leasing more
  (and taking them by `lease-locks`) while it holds too few. A lease
  refused leaves the door no lock: it then cites the ids the refused lease
  would have minted, under locks it throws away, so the gate refuses the
  act on its face (`:no-such-lock`) and the act's answer is the gate's
  (rig choice)."
  [store offer idx]
  (let [layer (:layer offer)
        session (:session offer)
        grain (or (get-in @(:door store) [:grain layer]) (refresh! store layer session))
        n (if (= :per-act grain) 1 (count idx))]
    (loop [tries 0]
      (if-let [picked (take! store layer session n)]
        (held-for idx picked grain)
        (let [have (count (get-in @(:door store) [:pool [layer session]]))
              a (lease-for! store offer (min locks/max-lease (max lease-size (- n have))))]
          (cond
            (not= :yes (:answer a))
            (held-for idx (mapv (fn [id] [id (locks/fresh-lock)]) (locks/lease-ids (:name a) n)) grain)

            (< 100 tries) (throw (ex-info "the door could not lease enough locks" {:layer layer :n n}))

            :else (do (refresh! store layer session) (recur (inc tries)))))))))

(defn- seal-with
  "The offer with each value fact sealed under its held lock (fresh nonces
  on every sealing), citing the lock's id; control facts and retracts as
  they are."
  [offer idx held]
  (assoc offer :facts
         (reduce (fn [fs i]
                   (let [f (nth fs i)
                         id (get-in held [:ids i])
                         K (get-in held [:keys id])]
                     (assoc fs i (-> f
                                     (dissoc :v)
                                     (assoc :sealed (locks/seal K (locks/canonical-bytes (:v f)))
                                            :lock-id id)))))
                 (vec (:facts offer)) idx)))

(defn sealed
  "The offer as the door sends it: its value facts sealed under the locks
  it holds for this offer, picked (and leased) on its first sending and
  kept until it is answered, so a resend is sealed again under the same
  ids. Anything with no value fact goes as it is."
  [store offer]
  (let [idx (value-indices offer)]
    (if (empty? idx)
      offer
      (let [held (or (get-in @(:door store) [:held offer])
                     (let [h (assign! store offer idx)]
                       (swap! (:door store) assoc-in [:held offer] h)
                       h))]
        (seal-with offer idx held)))))

(defn- answered!
  "The door's memory after an answer: the offer's locks are let go (used,
  or destroyed with a refusal); a grain switch it sent makes it take the
  grain again; a session close empties that session's pool; a person it
  made is remembered for the forget's stood-on."
  [store offer a]
  (let [yes? (= :yes (:answer a))
        facts (when (and (map? offer) (sequential? (:facts offer))) (:facts offer))
        closed (some #(when (= :session-closed (:k %)) (get-in % [:v :session])) facts)
        made (some #(when (= :person (:k %)) (:e %)) facts)]
    (swap! (:door store)
           (fn [d]
             (cond-> (update d :held dissoc offer)
               (and yes? (some #(= :lock-grain (:k %)) facts)) (update :grain dissoc (:layer offer))
               (and yes? closed) (update :pool dissoc [(:layer offer) closed])
               (and yes? made (:stamp a)) (assoc-in [:made made] [[(:name offer) 0] (:stamp a)]))))))

(defn offer!
  "Seal the offer's values, append it with a full ack and return the gate's
  answer. Blocks until the decision is visible. Throws when the append
  fails; the offer may still have gone in (RIG.md finding 3), and the door
  keeps its locks for the resend."
  [store offer]
  (let [a (get (foreign-append! (:depot store) (sealed store offer) :ack) "gate")]
    (answered! store offer a)
    a))

(defn record
  "The answer record kept under a name, read by name plus layer (RD1), or nil."
  [store nm]
  (foreign-select-one [(keypath (nth nm 0) :answers nm)] (:layers store)))

(defn lookup
  "The answer to an offer by its name plus layer (RD1, model.clj `lookup`):
  :no-answer when the layer's home holds none; given the asker's parts
  digest, a record made for other content is {:answer :no :reason
  :name-taken}; else the record itself. The parts digest covers no value
  (L26), so other value content under the name looks like the record here;
  only the gate's value check tells, on a resend. A nil digest returns the
  record as data, for a client that kept only the name (F13). With an offer
  in place of name and digest, the offer's own name and digest."
  ([store offer] (lookup store (:name offer) (env/offer-digest offer)))
  ([store nm digest]
   (let [rec (record store nm)]
     (cond
       (nil? rec) :no-answer
       (and (some? digest) (not= digest (:digest rec))) {:answer :no :reason :name-taken :name nm}
       :else rec))))

(defn answer-of
  "The answer shape the ack carries, from a record found by lookup."
  [nm rec]
  (if (= :name-taken (:reason rec))
    {:answer :no :reason :name-taken :stamp nil :name nm}
    {:answer (:answer rec) :reason (:reason rec) :stamp (:stamp rec) :name nm}))

(defn offer-until-answered!
  "Send; after an error look the answer up with the offer's own digest, and
  send the same map again while there is none, up to `tries` times, then
  throw the last error (I-G3, RQ 4). Each sending is sealed again under the
  same held locks."
  ([store offer] (offer-until-answered! store offer 60))
  ([store offer tries]
   (let [nm (:name offer)
         d (env/offer-digest offer)]
     (loop [n 1]
       (let [r (try (offer! store offer) (catch Exception e e))]
         (if-not (instance? Exception r)
           r
           (let [found (try (lookup store nm d) (catch Exception _ :no-answer))]
             (cond
               (map? found) (let [a (answer-of nm found)] (answered! store offer a) a)
               (>= n tries) (throw r)
               :else (do (Thread/sleep 250) (recur (inc n)))))))))))

;; ------------------------------------------------ leases and sessions

(defn lease-locks
  "Every unconsumed lock leased to `session` in `layer`, in plaintext, with
  the grain in force: {:grain g :locks {lock-id K}} (L29)."
  [store layer session]
  (foreign-invoke-query (:lease-locks-q store) layer session))

(defn- layer-class [store layer]
  (or (:class (foreign-select-one [(keypath layer :settings)] (:layers store))) :by-layer))

(defn lease!
  "Lease `n` locks into `layer` for `session` by `who` (L20), citing
  `permission` (by default [who layer layer], none for the operator), under
  a fresh name or `nm`: the lease act, answered by name. Its answer; a yes
  carries `:lock-ids`, the same on a resend."
  ([store who layer session n]
   (lease! store who layer session n (when-not (= :operator who) [who layer layer])))
  ([store who layer session n permission]
   (lease! store who layer session n permission nil))
  ([store who layer session n permission nm]
   (let [o (build {:name nm :who who :layer layer :class (layer-class store layer)
                   :permission permission :session session
                   :facts [{:e session :k :lease :v {:count n}}]})
         a (offer-until-answered! store o)]
     (cond-> a
       (and (= :yes (:answer a)) (nil? (:lock-ids a))) (assoc :lock-ids (locks/lease-ids (:name o) n))))))

(defn close-session!
  "The `:session-closed` act (L28) by `who`, the session's writer or the
  operator: on a yes its unconsumed lease rows in `layer` are deleted."
  [store who layer session]
  (offer-until-answered!
   store
   (build {:who who :layer layer :class (layer-class store layer)
           :permission (when-not (= :operator who) [who layer layer]) :session session
           :facts [{:e session :k :session-closed :v {:session session}}]})))

;; ------------------------------------------------- persons and forgets

(def people-layer "The store layer persons are made and forgotten in (L7)." :people)

(defn make-person!
  "The operator's act that makes person `p` in `:people` (OP10, L7)."
  [store p]
  (offer-until-answered!
   store (build {:who :operator :layer people-layer :class :by-layer
                 :facts [{:e p :k :person :v {:id p}}]})))

(defn forget-person!
  "The operator's act that forgets person `p` in `:people` (OP10, L7): their
  person lock is destroyed on every task before the answer returns. It
  stands on the act that made them when this door made it."
  [store p]
  (let [[fid stamp] (get-in @(:door store) [:made p])]
    (offer-until-answered!
     store (build {:who :operator :layer people-layer :class :by-layer
                   :stood-on (if fid {fid stamp} {})
                   :facts [{:e p :k :forget-person :v {:person p}}]}))))

(defn raw-rows
  "An act's rows as stored: sealed bytes, lock ids, lock records, digests."
  [store layer nm]
  (foreign-select [(keypath layer :log nm) ALL] (:layers store)))

(defn raw-row
  "One row of an act as stored, by its fact id, or nil."
  [store layer [nm idx]]
  (nth (raw-rows store layer nm) idx nil))

(defn forget-value!
  "The forget of value `fid` in `layer` (OP9, L10) by `who`: the layer's
  owner citing [who layer layer], or the operator. It stands on the target
  with the stamp its record holds, and is placed by the store (tag class
  nil)."
  [store who layer fid]
  (let [stamp (:stamp (record store (first fid)))
        e (or (:e (raw-row store layer fid)) layer)]
    (offer-until-answered!
     store (build {:who who :layer layer :class (layer-class store layer)
                   :permission (when-not (= :operator who) [who layer layer])
                   :stood-on (if stamp {fid stamp} {})
                   :facts [{:e e :k :forget :v {:target fid}}]}))))

;; --------------------------------------------------------------- reads

(defn read-as-of
  "The layer as of stamp `T` (`read-as-of`, L18): {:as-of T :facts [...]
  :erased {lock-id {:stamp :how}}}, each fact {:id :stamp :e :k :replaces
  :mark} with `:value`, `:erased-at` or `:unreadable`."
  [store layer T]
  (foreign-invoke-query (:read-as-of-q store) layer T))

(defn opens?
  "Whether a value opens, and since when (RD7): `read-as-of` at the value's
  stamp, the fact by id: {:value v} or {:erased-at s} (or {:unreadable r})."
  [store layer fid]
  (let [stamp (:stamp (record store (first fid)))
        f (when stamp (some #(when (= fid (:id %)) %) (:facts (read-as-of store layer stamp))))]
    (select-keys f [:value :erased-at :unreadable])))

(defn facts
  "An admitted act's facts in index order, each value opened (F1): stage
  1's {:e :k :v :replaces :mark}, read through `read-as-of` at the act's
  stamp, so a value shows only as the store opens it; an erased one shows
  `:erased-at` in place of `:v`. [] for an act not admitted."
  [store layer nm]
  (let [rec (record store nm)]
    (if (not= :yes (:answer rec))
      []
      (->> (:facts (read-as-of store layer (:stamp rec)))
           (filter #(= nm (first (:id %))))
           (sort-by #(second (:id %)))
           (mapv (fn [f]
                   (cond-> {:e (:e f) :k (:k f) :v (:value f) :replaces (:replaces f) :mark (:mark f)}
                     (contains? f :erased-at) (-> (dissoc :v) (assoc :erased-at (:erased-at f)))
                     (contains? f :unreadable) (-> (dissoc :v) (assoc :unreadable (:unreadable f))))))))))

(defn stood-on
  "What an act stood on, {fid stamp} as carried (F2)."
  [store layer nm]
  (into {} (foreign-select [(keypath layer :stood-on nm) ALL] (:layers store))))

(defn settings [store layer]
  (foreign-select-one [(keypath layer :settings)] (:layers store)))

(defn permission [store layer pid]
  (foreign-select-one [(keypath layer :permissions pid)] (:layers store)))

(defn head
  "The stamp of fact `fid` while it heads its chain for (e k) in the layer,
  else nil."
  [store layer e k fid]
  (foreign-select-one [(keypath layer :heads [e k fid])] (:layers store)))

(defn clock
  "The last stamp given by the task that is the layer's home: a hybrid stamp,
  whose wall millisecond is `rig.store.clock/ms-of`."
  [store layer]
  (foreign-select-one STAY (:clock store) {:pkey layer}))

(defn lock-rows
  "A layer's lock rows, [[lock-id record] ...] (RD7s, M2)."
  [store layer]
  (foreign-select [(keypath layer :locks) ALL] (:layers store)))

(defn lock-row
  "One lock row by its lock id, or nil."
  [store layer lock-id]
  (foreign-select-one [(keypath layer :locks lock-id)] (:layers store)))

(defn lease-rows
  "A session's unconsumed lease rows in a layer, [[lock-id row] ...], raw."
  [store layer session]
  (foreign-select [(keypath layer :leases session) ALL] (:layers store)))

(defn ledger
  "A layer's erasure ledger, {lock-id {:stamp :how}}."
  [store layer]
  (into {} (foreign-select [(keypath layer :erased) ALL] (:layers store))))

(defn by-stamp
  "A layer's by-stamp index, [[stamp name] ...]."
  [store layer]
  (foreign-select [(keypath layer :by-stamp) ALL] (:layers store)))

(defn person
  "A person's entry, {:lock bytes :erased-at s}, read from the task its id
  routes to (every task holds the same entry once the person act answered)."
  [store p]
  (foreign-select-one [(keypath p)] (:persons store)))

(defn person-on-task
  "A person's entry as the task that `pkey` routes to holds it."
  [store p pkey]
  (foreign-select-one [(keypath p)] (:persons store) {:pkey pkey}))

(defn depot-records
  "Every record the depot holds, from every partition (for P6 check 1)."
  [store]
  (let [d (:depot store)
        n (:num-partitions (rama/foreign-object-info d))]
    (vec (for [p (range n)
               :let [{:keys [start-offset end-offset]} (rama/foreign-depot-partition-info d p)]
               r (rama/foreign-depot-read d p start-offset end-offset)]
           r))))

;; ------------------------------------------------------------------- seed

(def model-world
  "The model's one-owner side (model.clj `layers`, `permissions`): its two
  persons, three of Alice's layers and the permissions that live in them.
  Shared layers and the permissions kept in them are stage 3's; the base
  is `seed-base!`'s."
  {:persons [:alice :bob]
   :layers [[:alice {:kind :personal :owner :alice}]
            [:alice-hand {:kind :hand :owner :alice}]
            [:alice-agent {:kind :agent :owner :alice}]]
   :permissions (vec (concat (for [l [:alice :alice-hand :alice-agent]] [:alice l l])
                             (for [l [:alice :alice-agent :group :base]] [:alice l :alice-hand])))})

(defn make-layer-offer
  "The operator's act that makes a layer (P10); a layer with no owner (the
  store layer) gets no owner fact."
  [layer {:keys [kind owner]}]
  (build {:who :operator :layer layer :class :by-layer
          :facts (cond-> [{:e layer :k :kind :v kind}]
                   owner (conj {:e layer :k :owner :v owner})
                   true (into [{:e layer :k :class :v :by-layer}
                               {:e layer :k :lock-grain :v :per-value}]))}))

(defn grant-offer
  "The operator's act that grants a permission, in the layer it lives in."
  [store [who _ in :as pid]]
  (build {:who :operator :layer in :class (or (:class (settings store in)) :by-layer)
          :facts [{:e (gate/perm-entity who) :k :permission :v {:id pid}}]}))

(defn revoke-offer
  "The operator's act that revokes a permission, standing on its grant."
  [store [who _ in :as pid]]
  (let [{:keys [granted]} (permission store in pid)
        grant-stamp (when granted (:stamp (record store (first granted))))]
    (build {:who :operator :layer in :class (or (:class (settings store in)) :by-layer)
            :stood-on (if (and granted grant-stamp) {granted grant-stamp} {})
            :facts [{:e (gate/perm-entity who) :k :revoke :v {:permission pid}}]})))

(defn seed!
  "The first facts: the store layer `:people`, a person act per person of
  the world, then each layer, then each permission that lives in a
  one-owner layer. Persons come first: a making act naming an owner with no
  person lock is refused (L11). Returns every answer."
  ([store] (seed! store model-world))
  ([store {:keys [persons layers permissions]}]
   (let [people (offer-until-answered! store (make-layer-offer people-layer {:kind :store}))
         made-persons (vec (for [p persons] (make-person! store p)))
         made (vec (for [[l spec] layers] (offer-until-answered! store (make-layer-offer l spec))))
         one-owner (set (map first layers))
         granted (vec (for [pid permissions :when (one-owner (nth pid 2))]
                        (offer-until-answered! store (grant-offer store pid))))]
     (-> [people] (into made-persons) (into made) (into granted)))))

(defn seed-base!
  "The base on this stage's stream gate (default 6, [V-F2]): made by the
  operator with the root actor as its owner, and a permission for each
  writer kept in the base itself. Returns every answer."
  ([store] (seed-base! store [:alice :bob]))
  ([store writers]
   (into [(offer-until-answered! store (make-layer-offer :base {:kind :base :owner locks/root-actor}))]
         (for [w writers] (offer-until-answered! store (grant-offer store [w :base :base]))))))
