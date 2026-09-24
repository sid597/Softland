(ns rig.store.client
  "The offerer's side (PLAN-stream-store.md, 'The client side'): make a
  name, build an offer, send it, resend it under the same name after an
  error, and read the answer by name plus layer. Plain Clojure over the
  foreign API."
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
  one is given. Pure: the map it returns is what is sent and what is resent."
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
  "Append the offer with a full ack and return the gate's answer. Throws when
  the append fails; the offer may still have gone in (RIG.md finding 3)."
  [store offer]
  (get (foreign-append! (:depot store) offer :ack) "gate"))

(defn lookup
  "The answer to an offer by its name plus layer (RD1): :no-answer when the
  home holds none, name taken when it holds one for other content."
  [store offer]
  (let [nm (:name offer)
        rec (foreign-select-one [(keypath (nth nm 0) :answers nm)] (:layers store))]
    (cond
      (nil? rec) :no-answer
      (not= (:digest rec) (env/offer-digest offer)) {:answer :no :reason :name-taken :name nm}
      :else {:answer (:answer rec) :reason (:reason rec) :stamp (:stamp rec) :name nm})))

(defn offer-until-answered!
  "Send; after an error look the answer up, and send the same map again
  while there is none, up to `tries` times (I-G3)."
  ([store offer] (offer-until-answered! store offer 40))
  ([store offer tries]
   (loop [n 1]
     (let [r (try (offer! store offer) (catch Exception e e))]
       (if-not (instance? Exception r)
         r
         (let [found (try (lookup store offer) (catch Exception _ :no-answer))]
           (cond
             (map? found) found
             (>= n tries) (throw r)
             :else (do (Thread/sleep 250) (recur (inc n))))))))))

(defn record [store nm]
  (foreign-select-one [(keypath (nth nm 0) :answers nm)] (:layers store)))

(defn facts
  "An admitted act's rows, each value decoded."
  [store layer nm]
  (some->> (foreign-select-one [(keypath layer :log nm)] (:layers store))
           (mapv #(update % :v env/decode-value))))

(defn settings [store layer]
  (foreign-select-one [(keypath layer :settings)] (:layers store)))

(defn permission [store layer pid]
  (foreign-select-one [(keypath layer :permissions pid)] (:layers store)))

(defn head
  "The stamp of fact `fid` while it heads its chain for (e k), else nil."
  [store layer e k fid]
  (foreign-select-one [(keypath layer :heads [e k fid])] (:layers store)))

(defn clock
  "The last stamp given by the task that is the layer's home."
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
        grant-stamp (:stamp (record store (first granted)))]
    (build {:who :operator :layer in :class (or (:class (settings store in)) :by-layer)
            :stood-on (if granted {granted grant-stamp} {})
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
