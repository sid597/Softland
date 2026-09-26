;; IMPORTANT: Before modifying this file, re-read PLAN-promotion.md (steps
;; 0, 1, 5 and 6, PR1, PR4, PR9, open question 2).
(ns rig.store.promote-client
  "The door's side of promotion (stage 4, PLAN-promotion.md): the landing
  lease in the target, the request in the owner's layer, the resend, and
  the status read with its statement. Plain Clojure over the foreign API,
  beside rig.store.client (the stream door) and rig.store.micro-client
  (the micro door), whose handles it extends.

  The door rules the store cannot check: one session for the landing lease
  and the request (the landing cites the lease under the request's
  session, open question 2); no request without a landing lease; and the
  session kept open in the target until the promotion is done or refused
  (PR9: a landing lease dies with its session).

  The four names of one promotion share the request's uuid u, made first
  (or given), so each is known before anything is sent: the landing lease
  `[T C :offer u]`, the request `[L :by-layer :offer u]`, the crossing
  `(env/crossing-name req)`, the landing `(env/landing-name req T C)`.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [com.rpl.rama :refer :all]
            [rig.store.box :as box]
            [rig.store.client :as c]
            [rig.store.envelope :as env]
            [rig.store.micro-client :as mc]
            [rig.store.promote-shape :as ps]))

(defn connect
  "The micro door's handles (`micro-client/connect`, which holds the
  stream door's) with the `promotion-status` query."
  [cluster]
  (let [st (mc/connect cluster)]
    (assoc st :status-q (foreign-query cluster (:module-name st) "promotion-status"))))

;; ------------------------------------------------------------ the status

(defn promotion-status
  "A promotion's status as of stamp `as-of` (nil: now), by the request's
  name in its layer: `{:status s ...}` with `s` one of `:none`,
  `:pending`, `:crossed`, `:done`, `:refused`, the stamps of the steps
  found, a refusal's `:reason` and `:at` (`:read-out` or `:landing`), and
  the `:statement` for the state found (PLAN step 6)."
  [store layer req as-of]
  (foreign-invoke-query (:status-q store) layer req as-of))

(defn status-of
  "Just the status keyword (phase 8's form)."
  [store layer req as-of]
  (:status (promotion-status store layer req as-of)))

;; ------------------------------------------------------- the landing lease

(defn landing-lease-offer
  "The landing lease act (PLAN step 0, PR4): into the target T, named
  `[T C :offer u]` from the request's own uuid u ([F1]), by the requester
  citing her permission in T, in the session the request will carry, with
  one control fact `{:e session :k :lease :v {:count 1 :landing [T C
  :landing u]}}`."
  [{:keys [who target class session permission uuid]}]
  (c/build {:name (ps/lease-name target class uuid) :who who :layer target :class class
            :permission permission :session session
            :facts [{:e session :k :lease :v {:count 1 :landing [target class :landing uuid]}}]}))

(defn- take-public
  "The landing lease's public key and the landing it is for, by the lease
  query of the target's gate (`micro-lease` once its batch is settled, or
  `lease-locks`), which gives a landing row's public key only. Nil on a
  timeout."
  [store T C session lease-name timeout-ms]
  (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
    (loop []
      (let [r (try (if (= :by-entity C)
                     (get-in (foreign-invoke-query (:lease-q store) lease-name nil) [:landings 0])
                     (get-in (c/lease-locks store T session) [:landings [lease-name 0]]))
                   (catch Exception _ nil))]
        (if (or r (> (System/currentTimeMillis) deadline))
          r
          (do (Thread/sleep 50) (recur)))))))

(defn lease-landing!
  "Take the landing lease (PLAN step 0) and its public key. `spec`: `:who`,
  `:target` (T), `:class` (T's class, which names the gate), `:session`,
  `:permission` (the requester's in T), `:uuid` (the request's). Returns
  `{:name :offer :lock-id :answer :public :for}`; `:public` nil when the
  lease was refused."
  [store {:keys [target class session] :as spec}]
  (let [o (landing-lease-offer spec)
        a (if (= :by-entity class) (mc/offer! store o) (c/offer-until-answered! store o))
        pub (when (= :yes (:answer a)) (take-public store target class session (:name o) 30000))]
    {:name (:name o) :offer o :lock-id [(:name o) 0] :answer a :public (:public pub) :for (:for pub)}))

;; ------------------------------------------------------------- the request

(defn request-offer
  "The request (PLAN step 1, PR1): an act by the owner in L named `[L
  :by-layer :offer u]`, in the session the landing lease was taken in,
  citing her permission in L, standing on the source at its stamp, with
  one `:promote-request` fact on the source's entity."
  [{:keys [who layer permission session source source-stamp source-e target class
           public landing-permission replaces subjects uuid claimed-when]}]
  (c/build {:name (ps/request-name layer uuid) :who who :layer layer :class :by-layer
            :permission permission :session session :claimed-when claimed-when
            :stood-on {source source-stamp}
            :facts [{:e source-e :k :promote-request
                     :v {:source source :target target :class class
                         :lease [(ps/lease-name target class uuid) 0]
                         :public (box/encode-public public)
                         :permission landing-permission
                         :replaces replaces
                         :subjects (or subjects #{})}}]}))

(defn resend!
  "The door's resend of a request under its name (a retry, or after a
  held step is released): answered from its record, which continues the
  promotion where it stopped. Its answer."
  [store p]
  (c/offer-until-answered! store (:offer p)))

(defn promote!
  "A promotion from the door, end to end (PLAN steps 0, 1 and 6). The
  request's uuid first (`:uuid`, or made here), so every name is known
  before anything is sent; the landing lease in the target; the request
  in the owner's layer. Its answer carries the crossing's when the
  read-out ran in the request's event. `spec`: `:who`, `:layer` (L),
  `:permission` (hers in L; default `[who L L]`), `:session` (default the
  writer's door session), `:source` (a fact id in L), `:target` (T),
  `:class` (T's class; default its settings'), `:landing-permission`
  (hers in T), `:replaces` (the head in T the copy replaces), `:subjects`
  (the copy's carried subjects), `:uuid`. Returns `{:name :uuid :offer
  :lease :request :crossing :landing :status :statement}`; with no
  landing lease, no request is sent (`:request` nil, `:status :none`)."
  [store spec]
  (let [u (or (:uuid spec) (env/uuid7))
        {:keys [who source]} spec
        L (:layer spec)
        T (:target spec)
        C (or (:class spec) (:class (mc/settings-of store T)) :by-layer)
        session (or (:session spec) (c/default-session who))
        lease (lease-landing! store {:who who :target T :class C :session session
                                     :permission (:landing-permission spec) :uuid u})]
    (if (nil? (:public lease))
      {:name (ps/request-name L u) :uuid u :lease lease :request nil :status :none}
      (let [o (request-offer {:who who :layer L :permission (or (:permission spec) [who L L])
                              :session session :source source
                              :source-stamp (or (:source-stamp spec) (:stamp (c/record store (first source))))
                              :source-e (or (:source-e spec) (:e (c/raw-row store L source)))
                              :target T :class C :public (:public lease)
                              :landing-permission (:landing-permission spec)
                              :replaces (:replaces spec) :subjects (:subjects spec) :uuid u})
            a (c/offer-until-answered! store o)
            st (promotion-status store L (:name o) nil)]
        {:name (:name o) :uuid u :offer o :lease lease
         :request (dissoc a :crossing :landing) :crossing (:crossing a) :landing (:landing a)
         :status (:status st) :statement (:statement st)}))))
