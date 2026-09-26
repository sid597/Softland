;; IMPORTANT: Before modifying this file, re-read PLAN-promotion.md ("The
;; protocol, act by act", "Names", PR1 to PR6, and the [F1], [F2], [F3]
;; fixes).
(ns rig.store.promote-shape
  "The shapes promotion adds to both gates (stage 4, PLAN-promotion.md),
  pure and total: the four names of one promotion, which share the
  request's uuid u; the request's control value check ([F1], [F2]); the
  landing lease's value check ([F1]); the landing lease's row (PR4); the
  landing body of the delivery, which opens a landing's box with its lease
  row's private key (PR6); and a landing envelope's face rules ([F3]).

  The four names, for a request in layer L into target T of class C:
  the landing lease `[T C :offer u]` (its one lock id `[[T C :offer u]
  0]`), the request `[L :by-layer :offer u]`, the crossing `[L nil
  :crossing u]` (`env/crossing-name`), the landing `[T C :landing u]`
  (`env/landing-name`). Each is unique by its gate's name rule, and u
  first appears in the requester's own lease act, so no one can take a
  name before it ([F1]).

  It requires only the envelope and the box, so the lock road, the stream
  gate and the micro gate can each call it.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [rig.store.box :as box]
            [rig.store.envelope :as env])
  (:import [java.nio.charset StandardCharsets]))

;; ------------------------------------------------------------------ names

(defn request-name
  "The request's name in the owner's layer: `[L :by-layer :offer u]`."
  [L u]
  [L :by-layer :offer u])

(defn lease-name
  "The landing lease's name in the target: `[T C :offer u]`, u the
  request's own uuid ([F1])."
  [T C u]
  [T C :offer u])

(defn bound-lease-id
  "The one lease id a landing name binds ([F1]): `[[T C :offer u] 0]` for
  the landing `[T C :landing u]`."
  [landing-name]
  [[(nth landing-name 0) (nth landing-name 1) :offer (nth landing-name 3)] 0])

(defn landing?
  "Whether `nm` is a name under the store's `:landing` scheme."
  [nm]
  (boolean (and (vector? nm) (= 4 (count nm)) (= :landing (nth nm 2)))))

(defn aad
  "The associated data a landing's box is bound to (PR6, [F2]): the
  canonical text, UTF-8, of `[lock-id landing-name]`, so a box opens only
  inside the landing it was made for."
  [lock-id landing-name]
  (.getBytes ^String (env/canonical [lock-id landing-name]) StandardCharsets/UTF_8))

;; --------------------------------------------------------- the request

(def request-keys
  "The request's control value, exactly these keys (PR1)."
  #{:source :target :class :lease :public :permission :replaces :subjects})

(defn- readable-fid?
  "A fact id a read can take: well formed, its index in a row vector's
  range (as rig.store.locks `readable-fid?`)."
  [x]
  (boolean (and (env/fid? x) (< (nth x 1) Integer/MAX_VALUE))))

(defn request-value-ok?
  "Whether a `:promote-request` fact's value has its shape (PLAN step 1,
  [F1], [F2]): exactly the request's keys; the source a readable fact id
  in the act's own layer, on which the act stands (its stamp carried); the
  target another layer; the class a class; the lease exactly `[[T C
  :offer u] 0]`, u the request's own uuid; the public key base64 text of
  44 bytes that decode as an X25519 public key; the permission a
  permission id for the target; replaces nil or a fact id in the target;
  the subjects a set of at most 256 readable keywords. The act has a
  session, the landing's (the landing lease is the session's). Everything
  else about the source is the read-out's to check. Pure, total."
  [offer v]
  (try
    (let [L (:layer offer)
          u (nth (:name offer) 3)
          {:keys [source target class lease public permission replaces subjects]} v]
      (boolean
       (and (map? v) (not (record? v)) (= request-keys (set (keys v)))
            (some? (:session offer))
            (readable-fid? source) (= L (nth (nth source 0) 0))
            (contains? (:stood-on offer) source)
            (env/readable-keyword? target) (not= L target)
            (contains? env/classes class)
            (= lease [[target class :offer u] 0])
            (some? (box/public-key (box/decode-public public)))
            (env/pid? permission) (= target (nth permission 1))
            (or (nil? replaces) (and (env/fid? replaces) (= target (nth (nth replaces 0) 0))))
            (set? subjects) (<= (count subjects) env/max-subjects) (every? env/readable-keyword? subjects))))
    (catch Throwable _ false)))

(defn crossed-value-ok?
  "Whether a `:crossed` fact's value has its shape (PR2): exactly
  `{:request <name> :source <fact id>}`. Pure, total."
  [v]
  (boolean (and (map? v) (not (record? v)) (= #{:request :source} (set (keys v)))
                (env/valid-name? (:request v)) (env/fid? (:source v)))))

;; ----------------------------------------------------- the landing lease

(defn landing-lease-value?
  "Whether a lease fact's value is a landing lease's (PR4, [F1]): exactly
  `{:count 1 :landing L*}`, L* the landing name the lease act's own name
  binds, `[T C :landing u]` for the act `[T C :offer u]`. A lease `:for`
  another name, or of another count, is `:malformed-control`. Pure, total."
  [offer v]
  (let [nm (:name offer)]
    (boolean (and (map? v) (not (record? v)) (= #{:count :landing} (set (keys v))) (= 1 (:count v))
                  (vector? nm) (= 4 (count nm)) (= :offer (nth nm 2))
                  (= (:landing v) [(nth nm 0) (nth nm 1) :landing (nth nm 3)])))))

(defn landing-of
  "The landing name a lease fact is for, when it is a landing lease's
  value, else nil."
  [f]
  (let [v (:v f)] (when (map? v) (:landing v))))

(defn landing-row
  "A landing lease's row (PR4, PR9): bare whoever leases it (`:under nil`,
  so it survives the leaser's forget, B case 4), the key pair's private
  key in `:sealed`, its public key in `:public`, and `:for` the one
  landing it may open."
  [pair landing-name]
  {:under nil :sealed (:private pair) :public (:public pair) :for landing-name})

(defn landing-row?
  "Whether a lease row is a landing lease's: it carries a public key."
  [row]
  (boolean (and (map? row) (some? (:public row)))))

(defn landing-public
  "What a lease query shows of a landing row: its public key and the
  landing it is for, never the private key."
  [row]
  {:public (:public row) :for (:for row)})

;; ------------------------------------------------------- the landing body

(defn open-landing
  "The landing body of the delivery (PR6, the plan's step 0 item 5): the
  lock a landing's box holds, opened with its lease row's private key,
  when the row is a landing row made for this very landing; else nil (a
  row of an ordinary lease, a row `:for` another name, a box that does not
  open). Pure, total."
  [row landing-name lock-id b]
  (try
    (when (and (landing-row? row) (= landing-name (:for row)))
      (box/unbox (:sealed row) (:public row) b (aad lock-id landing-name)))
    (catch Throwable _ nil)))

(defn landing-face
  "Why a landing envelope is refused on its face ([F3]), or nil: its facts
  must be exactly one sealed value fact carrying a well-formed `:box`
  (`:malformed` otherwise), citing exactly the lease id its name binds
  (`:no-such-lock` otherwise). `raw-facts` are the envelope's facts as
  sent. Pure, total."
  [nm raw-facts]
  (try
    (let [f (when (and (sequential? raw-facts) (= 1 (count raw-facts))) (first raw-facts))]
      (cond
        (not (and (map? f) (contains? f :sealed) (bytes? (:sealed f)) (box/box? (:box f)))) :malformed
        (not= (bound-lease-id nm) (:lock-id f)) :no-such-lock
        :else nil))
    (catch Throwable _ :malformed)))
