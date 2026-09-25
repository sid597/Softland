;; IMPORTANT: Before modifying this file, re-read PLAN-stream-store.md and
;; PLAN-locks-and-forgetting.md (sealed facts, the parts digest, L24 to L27).
(ns rig.store.envelope
  "The envelope and the names, shared by the gate and the offerer. Pure.

  An offer is one act under one name (PLAN-stream-store.md, 'The shapes').
  `parse` is total: it never throws, whatever it is given, because an
  exception in topology code is fatal to the worker (RIG.md, phase 0), and
  every class it hands on is the exact class the `$$layers` schema names,
  because a schema violation in a write is such an exception (F6).
  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [clojure.edn :as edn]
            [clojure.string :as str])
  (:import [clojure.lang BigInt]
           [java.math BigDecimal BigInteger]
           [java.security SecureRandom]
           [java.util UUID]
           [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]))

;; ------------------------------------------------------------------ parts

(def version "The only envelope version this gate knows." 1)

(def act-parts
  "The named parts the store owns, on the act, plus the version marker, the
  name and the facts (ruling: the envelope; P5). Any other part is refused."
  #{:version :name :who :layer :class :permission :session :stood-on
    :because-of :claimed-when :subjects :facts})

(def fact-parts
  "The positional core of a fact, its mark, and an optional layer that must
  be the act's (the sharpening: layer belongs on the act). Stage 2: a value
  fact carries `:sealed` and `:lock-id` in place of `:v` (L24)."
  #{:e :k :v :replaces :mark :layer :sealed :lock-id})

(def placed-keys
  "Fact keys whose acts the store places itself (model.clj `tag-of`, P4): a
  name made for such an act carries no class. Stage 2 adds the person acts
  (L7)."
  #{:class :lock-grain :forget :crossed :person :forget-person})

(def control-keys
  "Fact keys the store itself acts on (D2): they get no lock and their value
  slot stays plaintext canonical EDN (P12). The model's seven, stage 1's
  `:kind` and `:owner`, and stage 2's person, forget-person, lease and
  session-close keys. Every other fact with a value is a value fact, sealed
  at the door (L24, L27)."
  #{:forget :lock-grain :class :promote-request :crossed :permission :revoke
    :kind :owner :person :forget-person :lease :session-closed})

(def setting-keys
  "Fact keys on a layer's own entity that the gate projects into settings,
  and the settings field each goes to (P10)."
  {:kind :kind, :owner :owner, :class :class, :lock-grain :grain})

(def marks #{:die-with-any :own-row})
(def classes #{:by-layer :by-entity})
(def schemes #{:offer :crossing :landing})
(def store-schemes "Schemes reserved to the store (P4)." #{:crossing :landing})

(def reserved-who
  "Who no record on the client-appended depot may claim to be (F6): the
  store's own steps enter in stage 4, which decides how."
  #{:store})

(def max-depth
  "How many collections a value may nest (F6): a value inside more is
  refused as malformed, so no walk over it recurses deep."
  32)

(def max-subjects "Carried subjects an offer may name (F3)." 256)

(def max-carried-stamp
  "A carried stood-on stamp must be below this (R16): the stamp is at least
  a carried stamp + 1, and a task's clock follows it, so a carried
  Long/MAX_VALUE would overflow and leave every later offer on that task a
  gate error. In the hybrid encoding (rig.store.clock) 2^62 is millisecond
  2^46, some two thousand years from now, so no honest stamp reaches it."
  (bit-shift-left 1 62))

;; -------------------------------------------------------------- EDN values

(def ^:private token-rx
  "Name and namespace of a keyword whose printed form reads back as itself."
  #"[\p{L}*+!_?$%&=<>.\-][\p{L}\p{N}*+!_?$%&=<>.\-'#]*")

(defn readable-keyword?
  "A keyword whose printed form reads back as the same keyword, so that the
  canonical text of anything holding it is unambiguous."
  [x]
  (boolean (and (keyword? x)
                (re-matches token-rx (name x))
                (let [ns (namespace x)] (or (nil? ns) (re-matches token-rx ns))))))

(defn- integral? [x]
  (or (instance? Long x) (instance? Integer x) (instance? Short x) (instance? Byte x)))

(defn- fits-long? [x] (<= Long/MIN_VALUE x Long/MAX_VALUE))

(defn edn-value?
  "Whether x is EDN data the store can hold as a value (P5): nil, booleans,
  strings, readable keywords, UUIDs, numbers (integers of any width, finite
  doubles, big integers and decimals; not ratios, and not Java floats, which
  EDN does not have and Clojure hashes apart from the equal double, so a set
  holding one is not = to the same set read back), and maps, vectors and
  sets of them, nested in at most `max-depth` collections."
  ([x] (edn-value? x 0))
  ([x depth]
   (cond
     (nil? x) true
     (boolean? x) true
     (string? x) true
     (keyword? x) (readable-keyword? x)
     (uuid? x) true
     (integral? x) true
     (instance? BigInt x) true
     (instance? BigInteger x) true
     (instance? Double x) (not (or (Double/isNaN x) (Double/isInfinite x)))
     (decimal? x) true
     (>= depth max-depth) false
     (map? x) (and (not (record? x))
                   (every? (fn [[k v]] (and (edn-value? k (inc depth)) (edn-value? v (inc depth)))) x))
     (vector? x) (every? #(edn-value? % (inc depth)) x)
     (set? x) (every? #(edn-value? % (inc depth)) x)
     :else false)))

(defn normalize-value
  "One form per `=` class, so a value reads back as it was offered and its
  digest does not depend on how the offerer built it: integers as longs
  (big ones only past the long range), -0.0 as 0.0, decimals without
  trailing zeros, every vector a PersistentVector (never a subvec), plain
  maps and sets. Each form is = to the value offered and hashes as it does."
  [x]
  (cond
    (integral? x) (long x)
    (instance? BigInt x) (if (fits-long? x) (long x) x)
    (instance? BigInteger x) (let [b (bigint x)] (if (fits-long? b) (long b) b))
    (instance? Double x) (if (zero? (double x)) 0.0 x)
    (decimal? x) (.stripTrailingZeros ^BigDecimal x)
    (map? x) (into {} (map (fn [[k v]] [(normalize-value k) (normalize-value v)])) x)
    (vector? x) (into [] (map normalize-value) x)
    (set? x) (into #{} (map normalize-value) x)
    :else x))

(defn- canonical*
  [x]
  (cond
    (nil? x) "nil"
    (map? x) (str "{" (str/join ", " (sort (map (fn [[k v]] (str (canonical* k) " " (canonical* v))) x))) "}")
    (set? x) (str "#{" (str/join " " (sort (map canonical* x))) "}")
    (vector? x) (str "[" (str/join " " (map canonical* x)) "]")
    (seq? x) (str "(" (str/join " " (map canonical* x)) ")")
    (instance? Integer x) (str (long x))
    (instance? Short x) (str (long x))
    (instance? Byte x) (str (long x))
    :else (pr-str x)))

(defn canonical
  "Canonical EDN text for normalised EDN data: map entries sorted by their
  canonical text, set elements likewise, recursively, so two `=` values
  print the same and two different ones never do."
  [x]
  (binding [*print-readably* true *print-length* nil *print-level* nil
            *print-meta* false *print-dup* false]
    (canonical* x)))

;; ------------------------------------------------------------------ digest

(def ^:private fingerprint-secret
  "The rig's fingerprint secret (P6). Every fingerprint over values is keyed
  (PROGRESS.md, rig constraints). A constant in code because the rig keeps
  no records; the client's copy is a convenience for `lookup` (F11)."
  (.getBytes "rig-2026-09-25 fingerprint secret, throwaway" "UTF-8"))

(def ^:private hex-chars (.toCharArray "0123456789abcdef"))

(defn- hex [bs]
  (let [^bytes bs bs
        ^chars hc hex-chars
        n (alength bs)
        out (char-array (* 2 n))]
    (dotimes [i n]
      (let [b (bit-and (long (aget bs i)) 0xff)]
        (aset out (* 2 i) (aget hc (bit-shift-right b 4)))
        (aset out (inc (* 2 i)) (aget hc (bit-and b 0xf)))))
    (String. out)))

(defn hmac-hex
  "HMAC-SHA256 of the text under the rig's secret, as hex."
  [^String text]
  (let [mac (Mac/getInstance "HmacSHA256")]
    (.init mac (SecretKeySpec. ^bytes fingerprint-secret "HmacSHA256"))
    (hex (.doFinal mac (.getBytes text "UTF-8")))))

(defn- parts-fact
  "A fact as the parts digest sees it (L26, [V-F1]): a sealed fact's bytes
  replaced by `true` and its lock id removed, so a resend sealed again, or
  sealed under a newly leased lock, digests the same, while a value fact, a
  retract and a control fact under the same e and k still digest apart."
  [f]
  (if (contains? f :sealed)
    (-> f (dissoc :lock-id) (assoc :sealed true))
    f))

(defn digest
  "The parts digest (change E, D8, P6; L26): the keyed digest of what a
  parsed offer says, its name aside, with every value fact's sealed bytes
  replaced by `true` and every lock id removed. It covers who, layer,
  class, permission, session, stood-on, because-of, claimed-when, subjects
  and per fact e, k, replaces, mark and any control value; nothing in it is
  a value and nothing names a lock. Derived by the gate when it decides and
  by the client when it looks up; the offer does not carry it."
  [offer]
  (hmac-hex (canonical (update (dissoc offer :name) :facts #(mapv parts-fact %)))))

;; ------------------------------------------------------------------- names

(defn store-placed-facts?
  "An act the store places itself: every fact's key is a placed key."
  [facts]
  (boolean (and (seq facts) (every? #(contains? placed-keys (:k %)) facts))))

(defn store-placed? [offer] (store-placed-facts? (:facts offer)))

(defn tag-of
  "The tag a name made for this offer carries: its layer, and the class that
  places it, or nil for an act the store places itself (model.clj `tag-of`)."
  [offer]
  [(:layer offer) (when-not (store-placed? offer) (:class offer))])

(defn name-tag [nm] [(nth nm 0) (nth nm 1)])

(defn mis-tagged?
  "Whether the offer claims another layer or class than its name was made
  for; refused on its face, reading nothing."
  [offer]
  (not= (tag-of offer) (name-tag (:name offer))))

(def ^:private ^SecureRandom rng (SecureRandom.))

(defn uuid7
  "A time-ordered random UUID (RFC 9562, version 7): 48 bits of Unix
  milliseconds, then random bits. Rama 1.6.0 has no `ops/random-uuid7` (R12)."
  []
  (let [ms (System/currentTimeMillis)
        rand-a (bit-and (.nextLong rng) 0xFFF)
        msb (bit-or (bit-shift-left (bit-and ms 0xFFFFFFFFFFFF) 16) (bit-shift-left 7 12) rand-a)
        lsb (bit-or (bit-and (.nextLong rng) 0x3FFFFFFFFFFFFFFF) Long/MIN_VALUE)]
    (UUID. msb lsb)))

(defn make-name
  "A fresh name, made by the offerer before the gate: [layer class scheme id]."
  ([layer class] (make-name layer class :offer (uuid7)))
  ([layer class scheme id] [layer class scheme id]))

(defn name-for
  "A fresh :offer-scheme name tagged by the layer and, unless the store
  places the act (every fact key a placed key), the class."
  ([offer] (name-for (:layer offer) (:class offer) (:facts offer)))
  ([layer class facts]
   (make-name layer (when-not (store-placed-facts? facts) class))))

(defn crossing-name
  "The read-out's name, derived from the request's (stage 4): the request's
  id under the reserved scheme, in the source layer, placed by the store."
  [req-name]
  [(nth req-name 0) nil :crossing (nth req-name 3)])

(defn landing-name
  "The landing's name, derived from the request's under a reserved scheme,
  tagged for the target layer's micro gate (stage 4)."
  [req-name target]
  [target :by-entity :landing (nth req-name 3)])

;; ------------------------------------------------------------------- parse

(defn valid-name?
  "A well-formed name: [layer class scheme uuid], class nil or known."
  [nm]
  (and (vector? nm) (= 4 (count nm))
       (readable-keyword? (nth nm 0))
       (or (nil? (nth nm 1)) (contains? classes (nth nm 1)))
       (contains? schemes (nth nm 2))
       (uuid? (nth nm 3))))

(defn fid?
  "A fact id [name idx], idx a non-negative integer; a lock id [lease-name
  i] has the same shape (L21)."
  [x]
  (and (vector? x) (= 2 (count x)) (valid-name? (nth x 0))
       (int? (nth x 1)) (<= 0 (nth x 1))))

(defn pid?
  "A permission id [who layer in] (P8)."
  [x]
  (and (vector? x) (= 3 (count x)) (every? readable-keyword? x)))

(defn- carried-stamp? [x] (and (int? x) (< x max-carried-stamp)))

;; Every vector that reaches a write is rebuilt with (into [] ...): `vec`
;; hands a subvec back unchanged, and a subvec is refused as a
;; PersistentVector key (F6; the F14 probe showed the refusal).
(defn- norm-name [nm] (into [] nm))
(defn- norm-fid [x] [(norm-name (nth x 0)) (long (nth x 1))])

(defn- value-slot
  "The value slot of a fact, by its kind (L24, L27): a control fact keeps a
  plaintext EDN `:v` and carries no seal; a value fact carries `:sealed`
  bytes and a well-formed `:lock-id` and no `:v`; a retract is `:v` nil
  with neither. The normalised slot fields, or the reason the fact is
  refused on its face: `:not-sealed` for a value that is not sealed as the
  door seals it, `:malformed` for a control value outside the domain."
  [f]
  (cond
    (contains? control-keys (:k f))
    (cond
      (or (contains? f :sealed) (contains? f :lock-id)) :not-sealed
      (not (edn-value? (:v f))) :malformed
      :else {:v (normalize-value (:v f))})

    (contains? f :sealed)
    (if (and (not (contains? f :v)) (bytes? (:sealed f)) (fid? (:lock-id f)))
      {:sealed (:sealed f) :lock-id (norm-fid (:lock-id f))}
      :not-sealed)

    (or (contains? f :lock-id) (some? (:v f))) :not-sealed
    :else {:v nil}))

(defn- parse-fact
  "A fact, normalised, or the reason it is refused."
  [f]
  (let [slot (when (and (map? f) (not (record? f))) (value-slot f))]
    (cond
      (not (map? f)) :malformed
      (record? f) :malformed
      (not-every? fact-parts (keys f)) :unknown-part
      (not (readable-keyword? (:e f))) :malformed
      (not (readable-keyword? (:k f))) :malformed
      (keyword? slot) slot
      (not (or (nil? (:replaces f)) (fid? (:replaces f)))) :malformed
      (not (or (nil? (:mark f)) (and (set? (:mark f)) (every? marks (:mark f))))) :malformed
      (not (or (nil? (:layer f)) (readable-keyword? (:layer f)))) :malformed
      :else (cond-> (merge {:e (:e f) :k (:k f)}
                           slot
                           {:replaces (some-> (:replaces f) norm-fid)
                            :mark (into #{} (:mark f))})
              (some? (:layer f)) (assoc :layer (:layer f))))))

(defn- parse*
  [raw gate]
  (let [facts (when (map? raw) (:facts raw))
        so (when (map? raw) (:stood-on raw))
        subjects (when (map? raw) (:subjects raw))]
    (cond
      (not (map? raw)) {:refuse :malformed}
      (record? raw) {:refuse :malformed}
      (not-every? act-parts (keys raw)) {:refuse :unknown-part}
      (not= version (:version raw)) {:refuse :unknown-version}
      (not (valid-name? (:name raw))) {:refuse :bad-name}
      (not (readable-keyword? (:who raw))) {:refuse :malformed}
      (contains? reserved-who (:who raw)) {:refuse :reserved-who}
      (not (readable-keyword? (:layer raw))) {:refuse :malformed}
      (not (contains? classes (:class raw))) {:refuse :malformed}
      (not (or (nil? (:permission raw)) (pid? (:permission raw)))) {:refuse :malformed}
      (not (or (nil? (:session raw)) (readable-keyword? (:session raw)))) {:refuse :malformed}
      (not (or (nil? so)
               (and (map? so) (not (record? so))
                    (every? (fn [[k v]] (and (fid? k) (carried-stamp? v))) so))))
      {:refuse :malformed}
      (not (or (nil? (:because-of raw)) (valid-name? (:because-of raw)))) {:refuse :malformed}
      (not (or (nil? (:claimed-when raw)) (int? (:claimed-when raw)))) {:refuse :malformed}
      (not (or (nil? subjects) (and (set? subjects) (every? readable-keyword? subjects))))
      {:refuse :malformed}
      (< max-subjects (count subjects)) {:refuse :malformed}
      (not (sequential? facts)) {:refuse :malformed}
      (contains? store-schemes (nth (:name raw) 2)) {:refuse :reserved-scheme}
      (empty? facts) {:refuse :empty-act}
      :else
      (let [fs (mapv parse-fact facts)]
        (if-let [bad (first (filter keyword? fs))]
          {:refuse bad}
          (let [offer {:version version
                       :name (norm-name (:name raw))
                       :who (:who raw)
                       :layer (:layer raw)
                       :class (:class raw)
                       :permission (some-> (:permission raw) norm-name)
                       :session (:session raw)
                       :stood-on (into {} (map (fn [[k v]] [(norm-fid k) (long v)])) so)
                       :because-of (some-> (:because-of raw) norm-name)
                       :claimed-when (some-> (:claimed-when raw) long)
                       :subjects (into #{} subjects)
                       :facts fs}]
            (cond
              (and (= :stream gate) (= :by-entity (nth (:name offer) 1))) {:refuse :wrong-gate}
              (and (= :micro gate) (= :by-layer (nth (:name offer) 1))) {:refuse :wrong-gate}
              (mis-tagged? offer) {:refuse :mis-tagged}
              :else {:ok offer})))))))

(defn parse
  "Parse a raw depot record into {:ok offer}, every part validated and every
  class normalised, or {:refuse reason} for a record refused on its face,
  answered through the ack and recorded nowhere (P7): malformed (including
  a control value nested past `max-depth`, more than `max-subjects` carried
  subjects, a carried stamp past `max-carried-stamp`), an unknown part or
  version, a bad name, `:who :store` (:reserved-who), a reserved scheme, an
  empty act, a value fact not sealed as the door seals it (:not-sealed,
  L27: a plaintext `:v` on a value fact, `:sealed` that is not bytes or has
  no well-formed `:lock-id`, a `:v` beside `:sealed`, a seal on a control
  fact), a name made for the other gate (:wrong-gate), or a name made for
  another layer or class (:mis-tagged). A sealed value is not read here:
  its domain checks run where the gate opens it (rig.store.locks
  `read-values`). Total: never throws."
  ([raw] (parse raw :stream))
  ([raw gate]
   (try (parse* raw gate)
        (catch Throwable _ {:refuse :malformed}))))

(defn name-of
  "The raw record's name when it is a well-formed one, else nil; for the
  answer to a face refusal."
  [raw]
  (try (when (and (map? raw) (valid-name? (:name raw))) (norm-name (:name raw)))
       (catch Throwable _ nil)))

(defn encode-value
  "The value slot's text (P12): canonical EDN, nil for a retract."
  [v]
  (when (some? v) (canonical v)))

(defn decode-value
  "The value from its slot's text."
  [s]
  (when (some? s) (edn/read-string s)))

(defn value-fact?
  "A fact the door seals: a key that is not a control key, with a value
  (a retract has none, L14)."
  [f]
  (and (map? f) (not (contains? control-keys (:k f))) (some? (:v f))))

(defn sealed-view
  "An offer as the gate will parse it once the door has sealed it: every
  plaintext value fact given placeholder sealed bytes and a placeholder
  lock id in place of its `:v`. The parts digest leaves both out, so this
  view digests as the sealed offer will, whatever locks the door uses.
  Anything that is not an offer with a vector of facts is returned as it
  is."
  [raw]
  (if (and (map? raw) (sequential? (:facts raw)))
    (let [placeholder [(:name raw) 0]]
      (assoc raw :facts (mapv (fn [f]
                                (if (value-fact? f)
                                  (-> f (dissoc :v) (assoc :sealed (byte-array 0) :lock-id placeholder))
                                  f))
                              (:facts raw))))
    raw))

(defn offer-digest
  "The parts digest of an offer as the gate will see it: over its parsed,
  normalised form, so the client's lookup and the gate's record agree. A
  plaintext offer is digested as the door will seal it (`sealed-view`).
  Nil when the gate would refuse the record on its face."
  [raw]
  (some-> (parse (sealed-view raw)) :ok digest))
