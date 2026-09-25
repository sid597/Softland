(ns rig.store.client
  "The offerer's side (PLAN-stream-store.md, 'The client side'): make a
  name, build an offer, send it, resend it under the same name after an
  error, and read the answer by name plus layer. Plain Clojure over the
  foreign API, in the same process in the rig."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [rig.store.envelope :as env]
            [rig.store.gate :as gate]
            [rig.store.module :as m]))

(defn connect
  "Handles on the store, taken once."
  [cluster]
  (let [mn (get-module-name m/Store)]
    {:module-name mn
     :depot (foreign-depot cluster mn "*offers")
     :layers (foreign-pstate cluster mn "$$layers")
     :clock (foreign-pstate cluster mn "$$clock")}))

(defn build
  "An offer ready to send: version marker, defaults, and a fresh name unless
  one is given. Pure: the map it returns is what is sent and what is resent
  (the digest covers `:claimed-when` and `:stood-on`, so a resend is this
  map, never a rebuilt one). `:claimed-when` defaults to the offerer's wall
  clock in milliseconds: the offerer's claim, not a stamp."
  [{:keys [name who layer class permission facts stood-on subjects because-of session claimed-when]}]
  (let [offer {:version env/version
               :who who
               :layer layer
               :class class
               :permission permission
               :session session
               :stood-on (or stood-on {})
               :because-of because-of
               :claimed-when (or claimed-when (System/currentTimeMillis))
               :subjects (or subjects #{})
               :facts (vec facts)}]
    (assoc offer :name (or name (env/name-for offer)))))

(defn offer!
  "Append the offer with a full ack and return the gate's answer. Blocks
  until the decision is visible. Throws when the append fails; the offer may
  still have gone in (RIG.md finding 3)."
  [store offer]
  (get (foreign-append! (:depot store) offer :ack) "gate"))

(defn record
  "The answer record kept under a name, read by name plus layer (RD1), or nil."
  [store nm]
  (foreign-select-one [(keypath (nth nm 0) :answers nm)] (:layers store)))

(defn lookup
  "The answer to an offer by its name plus layer (RD1, model.clj `lookup`):
  :no-answer when the layer's home holds none; given the asker's digest, a
  record made for other content is {:answer :no :reason :name-taken}; else
  the record itself. A nil digest returns the record as data, for a client
  that kept only the name (F13). With an offer in place of name and digest,
  the offer's own name and digest."
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
  throw the last error (I-G3, RQ 4)."
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
               (map? found) (answer-of nm found)
               (>= n tries) (throw r)
               :else (do (Thread/sleep 250) (recur (inc n)))))))))))

(defn facts
  "An admitted act's rows in index order, each value decoded (F1)."
  [store layer nm]
  (mapv #(update % :v env/decode-value)
        (foreign-select [(keypath layer :log nm) ALL] (:layers store))))

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

;; ------------------------------------------------------------------- seed

(def model-world
  "The model's one-owner side (model.clj `layers`, `permissions`): three of
  Alice's layers and the permissions that live in them. Shared layers and
  the permissions kept in them are stage 3's."
  {:layers [[:alice {:kind :personal :owner :alice}]
            [:alice-hand {:kind :hand :owner :alice}]
            [:alice-agent {:kind :agent :owner :alice}]]
   :permissions (vec (concat (for [l [:alice :alice-hand :alice-agent]] [:alice l l])
                             (for [l [:alice :alice-agent :group :base]] [:alice l :alice-hand])))})

(defn make-layer-offer
  "The operator's act that makes a one-owner layer (P10)."
  [layer {:keys [kind owner]}]
  (build {:who :operator :layer layer :class :by-layer
          :facts [{:e layer :k :kind :v kind}
                  {:e layer :k :owner :v owner}
                  {:e layer :k :class :v :by-layer}
                  {:e layer :k :lock-grain :v :per-value}]}))

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
  "The first facts: make each layer, then grant each permission that lives
  in a one-owner layer. Returns every answer."
  ([store] (seed! store model-world))
  ([store {:keys [layers permissions]}]
   (let [made (vec (for [[l spec] layers] (offer-until-answered! store (make-layer-offer l spec))))
         one-owner (set (map first layers))
         granted (vec (for [pid permissions :when (one-owner (nth pid 2))]
                        (offer-until-answered! store (grant-offer store pid))))]
     (into made granted))))
