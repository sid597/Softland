;; IMPORTANT: Before modifying this file, re-read PLAN-stream-store.md.
(ns rig.store.envelope
  "The envelope and the names, shared by the gate and the offerer. Pure.

  An offer is one act under one name (PLAN-stream-store.md, 'The shapes').
  `parse` is total: it never throws, whatever it is given, because an
  exception in topology code is fatal to the worker (RIG.md, phase 0).
  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [clojure.edn :as edn]
            [clojure.string :as str])
  (:import [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]
           [java.security SecureRandom]
           [java.util UUID]))

;; ------------------------------------------------------------------ parts

(def version "The only envelope version this gate knows." 1)

(def act-parts
  "The named parts the store owns, on the act, plus the version marker, the
  name and the facts (ruling: the envelope; P5)."
  #{:version :name :who :layer :class :permission :session :stood-on
    :because-of :claimed-when :subjects :facts})

(def fact-parts
  "The positional core of a fact, its mark, and an optional layer that must
  be the act's (the sharpening: layer belongs on the act)."
  #{:e :k :v :replaces :mark :layer})

(def placed-keys
  "Fact keys whose acts the store places itself (model.clj `tag-of`, P4): a
  name made for such an act carries no class."
  #{:class :lock-grain :forget :crossed})

(def setting-keys
  "Fact keys on a layer's own entity that the gate projects into settings."
  {:kind :kind, :owner :owner, :class :class, :lock-grain :grain})

(def marks #{:die-with-any :own-row})
(def classes #{:by-layer :by-entity})
(def schemes #{:offer :crossing :landing})
(def store-schemes "Schemes reserved to the store (P4)." #{:crossing :landing})
(def root-actors "Actors at the root: they cite no permission." #{:operator :store})

;; -------------------------------------------------------------- EDN values

(def ^:private max-depth 32)

(defn edn-value?
  "Whether x is EDN data the store can hold as a value: nil, booleans,
  numbers other than ratios, strings, characters, keywords, symbols, UUIDs,
  and maps, vectors, sets and lists of them, nested at most 32 deep."
  ([x] (edn-value? x 0))
  ([x depth]
   (and (< depth max-depth)
        (cond
          (nil? x) true
          (boolean? x) true
          (string? x) true
          (keyword? x) true
          (symbol? x) true
          (char? x) true
          (uuid? x) true
          (ratio? x) false
          (number? x) (not (and (float? x) (or (Double/isNaN (double x)) (Double/isInfinite (double x)))))
          (map? x) (and (not (record? x))
                        (every? (fn [[k v]] (and (edn-value? k (inc depth)) (edn-value? v (inc depth)))) x))
          (vector? x) (every? #(edn-value? % (inc depth)) x)
          (set? x) (every? #(edn-value? % (inc depth)) x)
          (list? x) (every? #(edn-value? % (inc depth)) x)
          :else false))))

(defn canonical
  "Canonical EDN text for EDN data: map entries sorted by their canonical key
  text, set elements sorted by their canonical text, recursively, so two `=`
  values print the same. Integers print as longs do."
  [x]
  (cond
    (nil? x) "nil"
    (map? x) (str "{" (str/join ", " (sort (map (fn [[k v]] (str (canonical k) " " (canonical v))) x))) "}")
    (set? x) (str "#{" (str/join " " (sort (map canonical x))) "}")
    (vector? x) (str "[" (str/join " " (map canonical x)) "]")
    (seq? x) (str "(" (str/join " " (map canonical x)) ")")
    (instance? Integer x) (str (long x))
    (instance? Short x) (str (long x))
    (instance? Byte x) (str (long x))
    :else (pr-str x)))

(defn- normalize-value
  "Integers of every width become longs, so a value reads back as it was
  offered and a digest does not depend on the width the offerer used."
  [x]
  (cond
    (or (instance? Integer x) (instance? Short x) (instance? Byte x)) (long x)
    (instance? Float x) (double x)
    (map? x) (into {} (map (fn [[k v]] [(normalize-value k) (normalize-value v)])) x)
    (vector? x) (mapv normalize-value x)
    (set? x) (into #{} (map normalize-value) x)
    (list? x) (apply list (map normalize-value x))
    :else x))

;; ------------------------------------------------------------------ digest

(def ^:private fingerprint-secret
  "The rig's fingerprint secret (P6). Every fingerprint over values is keyed
  (PROGRESS.md, rig constraints). A constant in code because the rig keeps
  no records."
  (.getBytes "rig-2026-09-25 fingerprint secret, throwaway" "UTF-8"))

(defn hmac-hex
  "HMAC-SHA256 of the text under the rig's secret, as hex."
  [^String text]
  (let [mac (Mac/getInstance "HmacSHA256")]
    (.init mac (SecretKeySpec. ^bytes fingerprint-secret "HmacSHA256"))
    (let [bs (.doFinal mac (.getBytes text "UTF-8"))
          sb (StringBuilder.)]
      (doseq [b bs] (.append sb (format "%02x" (bit-and (long b) 0xff))))
      (str sb))))

(defn digest
  "The keyed digest of what an offer says, its name aside (change E, D8, P6)."
  [offer]
  (hmac-hex (canonical (dissoc offer :name))))

;; ------------------------------------------------------------------- names

(defn store-placed?
  "An act the store places itself: every fact's key is a placed key."
  [offer]
  (let [fs (:facts offer)]
    (boolean (and (seq fs) (every? #(contains? placed-keys (:k %)) fs)))))

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
  milliseconds, then random bits. Rama 1.6.0 has no `ops/random-uuid7`."
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
  "A fresh :offer-scheme name for an offer, tagged by its layer and, unless
  the store places the act, its class."
  [offer]
  (let [[l c] (tag-of offer)] (make-name l c)))

(defn crossing-name
  "The read-out's name, derived from the request's (stage 4): the request's
  id under the reserved scheme, in the source layer, placed by the store."
  [req-name]
  [(nth req-name 0) nil :crossing (nth req-name 3)])

(defn landing-name
  "The landing's name, derived from the request's under a reserved scheme."
  [req-name target]
  [target :by-entity :landing (nth req-name 3)])

;; ------------------------------------------------------------------- parse

(defn- valid-name? [nm]
  (and (vector? nm) (= 4 (count nm))
       (keyword? (nth nm 0))
       (or (nil? (nth nm 1)) (contains? classes (nth nm 1)))
       (contains? schemes (nth nm 2))
       (uuid? (nth nm 3))))

(defn- fid? [x]
  (and (vector? x) (= 2 (count x)) (valid-name? (nth x 0)) (int? (nth x 1)) (<= 0 (nth x 1))))

(defn- pid? [x]
  (and (vector? x) (= 3 (count x)) (every? keyword? x)))

(defn- norm-name [nm] (vec nm))
(defn- norm-fid [[nm i]] [(norm-name nm) (long i)])

(defn- parse-fact
  "A fact, normalised, or a refusal reason."
  [f]
  (cond
    (not (map? f)) :malformed
    (not-every? fact-parts (keys f)) :unknown-part
    (not (keyword? (:e f))) :malformed
    (not (keyword? (:k f))) :malformed
    (not (edn-value? (:v f))) :malformed
    (not (or (nil? (:replaces f)) (fid? (:replaces f)))) :malformed
    (not (or (nil? (:mark f)) (and (set? (:mark f)) (every? marks (:mark f))))) :malformed
    (not (or (nil? (:layer f)) (keyword? (:layer f)))) :malformed
    :else (cond-> {:e (:e f) :k (:k f) :v (normalize-value (:v f))
                   :replaces (some-> (:replaces f) norm-fid)
                   :mark (set (:mark f))}
            (:layer f) (assoc :layer (:layer f)))))

(defn- parse*
  [raw gate]
  (let [facts (:facts raw)]
    (cond
      (not (map? raw)) {:refuse :malformed}
      (not-every? act-parts (keys raw)) {:refuse :unknown-part}
      (not= version (:version raw)) {:refuse :unknown-version}
      (not (valid-name? (:name raw))) {:refuse :bad-name}
      (not (keyword? (:who raw))) {:refuse :malformed}
      (not (keyword? (:layer raw))) {:refuse :malformed}
      (not (contains? classes (:class raw))) {:refuse :malformed}
      (not (or (nil? (:permission raw)) (pid? (:permission raw)))) {:refuse :malformed}
      (not (or (nil? (:session raw)) (keyword? (:session raw)))) {:refuse :malformed}
      (not (or (nil? (:stood-on raw))
               (and (map? (:stood-on raw))
                    (every? (fn [[k v]] (and (fid? k) (int? v))) (:stood-on raw)))))
      {:refuse :malformed}
      (not (or (nil? (:because-of raw)) (valid-name? (:because-of raw)))) {:refuse :malformed}
      (not (or (nil? (:claimed-when raw)) (int? (:claimed-when raw)))) {:refuse :malformed}
      (not (or (nil? (:subjects raw)) (and (set? (:subjects raw)) (every? keyword? (:subjects raw)))))
      {:refuse :malformed}
      (not (sequential? facts)) {:refuse :malformed}
      (and (contains? store-schemes (nth (:name raw) 2)) (not= :store (:who raw)))
      {:refuse :reserved-scheme}
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
                       :permission (some-> (:permission raw) vec)
                       :session (:session raw)
                       :stood-on (into {} (map (fn [[k v]] [(norm-fid k) (long v)])) (:stood-on raw))
                       :because-of (some-> (:because-of raw) norm-name)
                       :claimed-when (some-> (:claimed-when raw) long)
                       :subjects (set (:subjects raw))
                       :facts fs}]
            (cond
              (and (= :stream gate) (= :by-entity (nth (:name offer) 1))) {:refuse :wrong-gate}
              (and (= :micro gate) (= :by-layer (nth (:name offer) 1))) {:refuse :wrong-gate}
              (mis-tagged? offer) {:refuse :mis-tagged}
              :else {:ok offer})))))))

(defn parse
  "Parse a raw depot record into {:ok offer} with every part validated and
  every class normalised, or {:refuse reason} for a record refused on its
  face: malformed, an unknown part or version, a bad name, a reserved
  scheme, an empty act, a name made for the other gate, or a name made for
  another layer or class (P7). Total: never throws."
  ([raw] (parse raw :stream))
  ([raw gate]
   (try (parse* raw gate)
        (catch Throwable _ {:refuse :malformed}))))

(defn name-of
  "The raw record's name when it is a well-formed one, else nil; for the
  answer to a face refusal."
  [raw]
  (try (when (and (map? raw) (valid-name? (:name raw))) (vec (:name raw)))
       (catch Throwable _ nil)))

(defn encode-value
  "The value slot's text (P12): canonical EDN, nil for a retract."
  [v]
  (when (some? v) (canonical v)))

(defn decode-value
  "The value from its slot's text."
  [s]
  (when (some? s) (edn/read-string s)))

(defn offer-digest
  "The digest of an offer as the gate will see it: over its parsed, normalised
  form, so the client's lookup and the gate's record agree. Nil when the gate
  would refuse the record on its face."
  [raw]
  (some-> (parse raw) :ok digest))
