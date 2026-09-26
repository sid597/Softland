;; IMPORTANT: Before modifying this file, re-read PLAN-promotion.md whole
;; (its [F1] to [F9] fixes) and PLAN_VALIDATION-promotion.md.
(ns rig.store.promote
  "Promotion (stage 4, PLAN-promotion.md), its pure core: the request as
  the gate admits it, the store-made crossing and landing, the read-out's
  verdict over what its event read, the stored forward and its schema,
  the status of a promotion over the three answers the gates record, and
  what is said at the point of promotion.

  The protocol, chosen once for every promotion (a uniformity rule): the
  door leases a landing lease in the target T under the request's uuid u;
  its request, an act in the owner's layer L, is decided by the stream
  gate; the read-out, the next event of the request's own record on L's
  home, opens the source through its lock, seals the copy under a fresh
  lock K, boxes K to the landing lease's public key and writes the
  crossing fact; the forward sends the stored landing to T's gate, which
  decides it as any sealed act into T, K coming out of the box. The
  dataflow is rig.store.promote-flow's; this namespace decides nothing
  the gates do not decide themselves.

  Every function is pure and total but `fresh-read-out`, the one draw,
  made before the read-out's decision.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [com.rpl.rama :refer :all]
            [rig.store.box :as box]
            [rig.store.clock :as hlc]
            [rig.store.envelope :as env]
            [rig.store.gate :as gate]
            [rig.store.locks :as locks]
            [rig.store.promote-shape :as ps])
  (:import [java.time Instant]))

;; ---------------------------------------------------------------- the request

(defn request-fact
  "A promotion request's one fact, `:promote-request` (PR1), or nil for any
  other act."
  [offer]
  (let [fs (:facts offer)]
    (when (and (= 1 (count fs)) (= :promote-request (:k (first fs))))
      (first fs))))

(defn continues?
  "Whether a record's event goes on to a promotion's read-out: the act is a
  request and its answer, fresh or from the record, is a yes."
  [offer ack]
  (boolean (and (request-fact offer) (= :yes (:answer ack)))))

(defn source-of
  "The request's source fact id."
  [offer]
  (get-in (request-fact offer) [:v :source]))

;; ---------------------------------------------------------------- the crossing

(defn crossing-offer
  "The read-out's act (PR2): named `(env/crossing-name req)`, `:who
  :store` with no permission (the store's own steps act at the root, the
  model's `exempt?`), standing on the source at the stamp the request
  carried and on the request at its stamp, with one `:crossed` fact
  `{:request req :source src}` on the source's entity (the request's
  when the source has no row). Placed by the store: nil class in its
  name, `:by-layer` on the act, checked against the layer's class ([F8]).
  Built on the read-out's fresh path only: a crossing's name is the
  store's alone, so its record answers by name (`recorded-ack`)."
  [req-offer req-stamp src-row]
  (let [f (request-fact req-offer)
        req (:name req-offer)
        src (get-in f [:v :source])]
    {:version env/version
     :name (env/crossing-name req)
     :who :store
     :layer (:layer req-offer)
     :class :by-layer
     :permission nil
     :session nil
     :stood-on {src (get (:stood-on req-offer) src) [req 0] req-stamp}
     :because-of req
     :claimed-when nil
     :subjects #{}
     :facts [{:e (or (:e src-row) (:e f)) :k :crossed :v {:request req :source src} :replaces nil :mark #{}}]}))

(defn recorded-ack
  "A crossing's answer from its record. Its name is derived from the
  request's under the store's reserved scheme, which no depot record may
  carry (env/parse), so only the store's read-out ever wrote under it: the
  record answers by name, with no digest to compare."
  [crec cname]
  {:answer (:answer crec) :reason (:reason crec) :stamp (:stamp crec) :name cname})

(defn fresh-read-out
  "The read-out's one draw, made before its decision (as phase 2 draws its
  locks and nonces): the copy's lock K, the copy's nonce, the box's
  ephemeral key pair and nonce. Impure; {} if a draw fails."
  []
  (try
    {:lock (locks/fresh-lock) :nonce (locks/fresh-nonce) :eph (box/keypair) :box-nonce (box/fresh-nonce)}
    (catch Throwable _ {})))

(defn fresh-ok?
  "Whether a draw holds everything the read-out seals with."
  [fresh]
  (boolean (and (locks/lock? (:lock fresh)) (bytes? (:nonce fresh)) (map? (:eph fresh))
                (bytes? (:public (:eph fresh))) (bytes? (:private (:eph fresh))) (bytes? (:box-nonce fresh)))))

(defn read-out-verdict
  "The read-out's own verdict (PLAN step 2), pure over what its event read
  and drew: `src-row` the source's row (nil when its act is not admitted
  or has no such row), `opened` what `locks/open-row>` showed of it, and
  the draw. The read-out's reasons, beside the model's two ([F2]):
  - `:source-erased`: the value is forgotten (a value forget, or the
    owner's person forget closing its wrap); a live lock failing on its
    bytes, which no write produces, reads the same;
  - `:source-has-no-value`: no such fact, a control fact, a retract;
  - `:malformed-control`: no box can be made to the request's public key
    (a small-order point, [F2]).
  Else `{:reason nil :sealed copy :box b}`: the copy sealed under the
  fresh lock K, K boxed to the landing lease's public key, bound to the
  lease id and the landing name. The layer's class, `gate/decide`'s, is
  placed before these ([F8]). Total."
  [req-offer src-row opened fresh]
  (try
    (let [f (request-fact req-offer)
          v (:v f)
          lname (env/landing-name (:name req-offer) (:target v) (:class v))]
      (cond
        (contains? opened :erased-at) {:reason :source-erased}
        (= :does-not-open (:unreadable opened)) {:reason :source-erased}
        (not (and (contains? opened :value) (locks/sealed? src-row))) {:reason :source-has-no-value}
        :else
        (let [K (:lock fresh)
              copy (locks/seal-with K (locks/canonical-bytes (:value opened)) (:nonce fresh))
              b (box/box (box/decode-public (:public v)) K (:eph fresh) (:box-nonce fresh)
                         (ps/aad (:lease v) lname))]
          (if (and copy b)
            {:reason nil :sealed copy :box b}
            {:reason :malformed-control}))))
    (catch Throwable _ {:reason :malformed-control})))

(defn crossing-context
  "The lock context the crossing's decision takes (`gate/decide`'s `lx`):
  nothing delivered, no persons, no draw, and the read-out's own reason as
  its value check's, which `locks/lock-refusal` places first among the
  lock reasons, after stage 1's list."
  [ro]
  (locks/lock-context {} {:reason (:reason ro)} {} nil nil {}))

;; ------------------------------------------------------------- the landing

(defn landing-offer
  "The landing (PR3), as it is sent: named `[T C :landing u]` from the
  request under the store's scheme, `:who` the requester, its session and
  the landing permission the request named, because of the request,
  standing on the source and on the crossing with their stamps, carrying
  the request's subjects (never the source act's subject slot), with one
  sealed value fact on the source's entity and key, replacing the head the
  request named, citing the landing lease's one lock and carrying the box.
  No nil part: the stored envelope reads back as it was sent."
  [req-offer crossing-stamp src-row sealed b]
  (let [f (request-fact req-offer)
        v (:v f)
        req (:name req-offer)
        src (:source v)]
    {:version env/version
     :name (env/landing-name req (:target v) (:class v))
     :who (:who req-offer)
     :layer (:target v)
     :class (:class v)
     :permission (:permission v)
     :session (:session req-offer)
     :because-of req
     :stood-on {src (get (:stood-on req-offer) src) [(env/crossing-name req) 0] crossing-stamp}
     :subjects (:subjects v)
     :facts [(cond-> {:e (:e src-row) :k (:k src-row) :sealed sealed :lock-id (:lease v) :box b}
               (:replaces v) (assoc :replaces (:replaces v)))]}))

(defn forward
  "The stored forward of a crossing decided yes (PR8, [F4]): the landing
  exactly as it is sent, and its route, the landing lease's name (the
  micro depot's hash key: the landing arrives where its lease row is) for
  a target on the micro gate; none for a target this gate orders, whose
  landing goes by a hop. Nil for anything but a yes."
  [req-offer d ro src-row]
  (when (and (= :decide (:kind d)) (= :yes (get-in d [:record :answer])) (nil? (:reason ro)))
    (let [landing (landing-offer req-offer (get-in d [:record :stamp]) src-row (:sealed ro) (:box ro))]
      (cond-> {:landing landing}
        (= :by-entity (:class landing))
        (assoc :route (ps/lease-name (:layer landing) (:class landing) (nth (:name req-offer) 3)))))))

(defn landing-parsed
  "A landing as the gate's decision takes an offer (the parsed form,
  `env/parse`'s): every act part present, each fact with its `:replaces`
  and `:mark`. A landing never passes the stream parse (its scheme is the
  store's), so a landing into a layer this gate orders is normalised
  here."
  [landing]
  (-> landing
      (update :stood-on #(or % {}))
      (merge {:claimed-when nil})
      (update :facts (fn [fs] (mapv #(merge {:replaces nil :mark #{}} %) fs)))))

(defn forward-schema
  "One stored forward's schema: the route, and the landing envelope as it
  is sent, typed part by part."
  []
  (fixed-keys-schema
   {:route clojure.lang.PersistentVector
    :landing (fixed-keys-schema
              {:version Long
               :name clojure.lang.PersistentVector
               :who clojure.lang.Keyword
               :layer clojure.lang.Keyword
               :class clojure.lang.Keyword
               :permission clojure.lang.PersistentVector
               :session clojure.lang.Keyword
               :because-of clojure.lang.PersistentVector
               :stood-on (map-schema clojure.lang.PersistentVector Long)
               :subjects (set-schema clojure.lang.Keyword)
               :facts (vector-schema
                       (fixed-keys-schema
                        {:e clojure.lang.Keyword
                         :k clojure.lang.Keyword
                         :replaces clojure.lang.PersistentVector
                         :sealed byte/1
                         :lock-id clojure.lang.PersistentVector
                         :box (fixed-keys-schema {:eph byte/1 :nonce byte/1 :wrapped byte/1})}))})}))

(defn layer-fields
  "The field this stage adds to a one-owner layer's value in `$$layers`:
  `:forwards`, request name to its stored forward, subindexed (a layer
  promotes without bound), written once in the crossing's yes and read on
  the crossing's record path and by the status read."
  []
  {:forwards (map-schema clojure.lang.PersistentVector (forward-schema)
                         {:subindex-options {:track-size? false}})})

;; ------------------------------------------------------------- the status

(defn- at-or-before? [stamp T] (and (int? stamp) (or (nil? T) (<= stamp T))))

(defn- settled?
  "A landing's answer is settled when it is visible on every task: a micro
  answer in a batch at or below the frontier F of the task its name row is
  on (R5); a stream answer once committed (no batch)."
  [rec F]
  (or (nil? (:batch rec)) (and (int? F) (<= (:batch rec) F))))

(defn status
  "The model's `promotion-status` as of `T` (nil: now), without optimism
  (PLAN step 5, [F5]), over the three answers the gates recorded: the
  request's and the crossing's on the owner layer's home, the landing's
  with the frontier F of its name row's task (nil for a stream target).
  - `:none`: no request admitted at or before T (a read as of a moment
    shows nothing admitted after it, [F5]); a refused request is no
    promotion (`:request-refused`);
  - `:pending`: no crossing answer at or before T: not yet read out,
    still forgettable;
  - `:refused`: the crossing, or a settled landing at or before T, said no;
  - `:done`: a settled landing at or before T said yes;
  - `:crossed`: otherwise: read out, not landed.
  The model's precedence (done, refused, crossed, pending) holds because
  the steps are one chain. With the stamps of each step found."
  [req-rec crossing-rec landing-rec F T]
  (cond
    (not (and req-rec (at-or-before? (:stamp req-rec) T)))
    {:status :none}

    (= :no (:answer req-rec))
    {:status :none :request-refused (:reason req-rec) :request-stamp (:stamp req-rec)}

    (not (and crossing-rec (at-or-before? (:stamp crossing-rec) T)))
    {:status :pending :request-stamp (:stamp req-rec)}

    (= :no (:answer crossing-rec))
    {:status :refused :at :read-out :reason (:reason crossing-rec)
     :request-stamp (:stamp req-rec) :crossing-stamp (:stamp crossing-rec)}

    (and landing-rec (settled? landing-rec F) (at-or-before? (:stamp landing-rec) T))
    (merge {:request-stamp (:stamp req-rec) :crossing-stamp (:stamp crossing-rec)
            :landing-stamp (:stamp landing-rec)}
           (if (= :yes (:answer landing-rec))
             {:status :done}
             {:status :refused :at :landing :reason (:reason landing-rec)}))

    :else
    {:status :crossed :request-stamp (:stamp req-rec) :crossing-stamp (:stamp crossing-rec)}))

;; ------------------------------------------------------ what is said

(def reason-sentences
  "Why a promotion was refused, in the person's words."
  {:source-erased "the value was forgotten before it was read out"
   :source-has-no-value "the request names no value to promote"
   :malformed-control "the request is malformed"
   :class-mismatch "the layer moved to the other gate before this step"
   :landing-lock-gone "the landing's lease is gone, with the session it was taken in"
   :permission-revoked "your permission in the target was revoked"
   :no-permission "you hold no permission in the target"
   :stale-replaces "the value it replaces in the target changed first"
   :person-forgotten "someone the copy is about was forgotten"
   :no-such-person "someone the copy is about is not a person the store knows"})

(defn- date-of [stamp]
  (when (int? stamp) (str (Instant/ofEpochMilli (hlc/ms-of stamp)))))

(defn statement
  "What is said at the point of promotion (the sharpening: 'Say all of
  this at the point of promotion'), for the state a status found, as data:
  `{:key k :text sentence}`, nil for `:none`. `target` names the layer the
  copy goes to."
  [st target]
  (case (:status st)
    :pending {:key :pending
              :text "Not yet read out. Forgetting the value now refuses this promotion."}
    :crossed {:key :crossed
              :text (str "Read out at " (date-of (:crossing-stamp st)) ". Forgetting the value now does not "
                         "recall the copy; the read-out is a read that happened. The copy is about whoever "
                         (name target) "'s grammar and tool name, not you. Crossed does not promise done: "
                         "the landing can still be refused.")}
    :done {:key :done :text (str "Landed in " (name target) " at " (date-of (:landing-stamp st)) ".")}
    :refused {:key :refused
              :text (str "Refused: " (get reason-sentences (:reason st) (some-> (:reason st) name)) ".")}
    nil))

(defn with-statement
  "A status with its statement."
  [st target]
  (if-let [s (statement st target)] (assoc st :statement s) st))

(defn status-target
  "The target a request names, from its forward or its request record's
  act (for the statement)."
  [fwd]
  (get-in fwd [:landing :layer]))

(defn gate-intake
  "The intake of a store-made act the stream gate decides (a landing into
  a layer it orders): `gate/intake-offer` over its parsed form."
  [landing]
  (gate/intake-offer (landing-parsed landing)))

(defn gate-error-ack
  "The crossing's answer when its event could not decide (a draw that
  failed): the gate's unrecorded `:gate-error`, so a resend retries."
  [cname]
  {:answer :no :reason :gate-error :stamp nil :name cname})

(defn admitted-by?
  "Whether a request's record is a yes at or before T (nil: now): the
  status read then looks at the crossing."
  [rec T]
  (boolean (and (= :yes (:answer rec)) (at-or-before? (:stamp rec) T))))

(defn crossed-by?
  "Whether a crossing's record is a yes at or before T: the status read
  then looks at the landing."
  [rec T]
  (boolean (and (= :yes (:answer rec)) (at-or-before? (:stamp rec) T))))

(defn status-of
  "`promotion-status`' answer from what its two parts read: the home's
  records and forward, the landing's record and its task's frontier."
  [home landing-rec F T]
  (with-statement (status (:req home) (:crossing home) landing-rec F T)
                  (status-target (:forward home))))
