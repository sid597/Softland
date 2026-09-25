(ns rig.store.envelope-test
  "Pure tests of the envelope and the stream gate's decision
  (PLAN-stream-store.md, 'Namespaces and tests'): property tests with
  test.check, a table of the reason order, and comparisons with the formal
  model's own functions. No cluster. One test PState (create-test-pstate,
  module-free) checks that every write `decide` computes fits the
  `$$layers` schema, because a schema violation inside the topology is fatal
  to the worker (RIG.md, phase 0; the F14 probe showed which writes throw).

  The unguarded `parse*` and `decide*` are driven directly: their Throwable
  guards are the last line, and a property that only exercised the guarded
  functions could not fail."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [formal.model :as fm]
            [formal.scenarios :as fs]
            [rig.store.envelope :as env]
            [rig.store.gate :as gate]
            [rig.store.module :as m])
  (:import [clojure.lang PersistentVector]
           [java.math BigDecimal]
           [java.security MessageDigest]
           [java.util UUID]))

;; ------------------------------------------------------------------ helpers

(def ^:private parse* @#'env/parse*)
(def ^:private decide* @#'gate/decide*)

(defn- check!
  "Run a property; on failure report the smallest failing input."
  [label n p]
  (let [r (tc/quick-check n p {:max-size 30})]
    (is (true? (:pass? r))
        (str label " failed: "
             (pr-str (select-keys r [:seed :num-tests :fail]))
             "\nsmallest: " (pr-str (get-in r [:shrunk :smallest]))))
    r))

(defn- nest
  "A value nested in n vectors."
  [n]
  (loop [v 1 i 0] (if (< i n) (recur [v] (inc i)) v)))

(defn- subv
  "The same elements as a subvec, which is a vector but not a PersistentVector."
  [v]
  (subvec (into [::pad] v) 1))

(def face-reasons
  "Every reason the gate gives on a record's face (P7, F6)."
  #{:malformed :unknown-part :unknown-version :bad-name :reserved-who
    :reserved-scheme :empty-act :wrong-gate :mis-tagged})

(def recorded-reasons
  "Every reason the gate records under a name, in its order (plan step 4,
  F8, R13)."
  [:fact-outside-the-acts-layer :no-such-layer :class-mismatch
   :permission-does-not-cover-this :permission-from-another-layer
   :no-permission :permission-revoked :malformed-control :control-not-allowed
   :stale-replaces :stale-revoke :layer-already-made :unsupported-reclass])

(defn- uuid [] (UUID/randomUUID))
(defn- nm [layer cls] [layer cls :offer (uuid)])

;; --------------------------------------------------------------- generators

(def layers [:alice :alice-hand :alice-agent])

(def gen-name
  (gen/let [l (gen/elements layers) c (gen/elements [:by-layer nil]) u gen/uuid] [l c :offer u]))

(def gen-fid (gen/tuple gen-name (gen/choose 0 5)))

(def gen-leaf
  "EDN leaves of every width and kind the gate accepts."
  (gen/one-of [(gen/return nil) gen/boolean gen/small-integer gen/large-integer
               (gen/fmap int gen/small-integer)
               (gen/fmap short (gen/choose -100 100))
               (gen/fmap byte (gen/choose -100 100))
               (gen/double* {:infinite? false :NaN? false})
               (gen/fmap float (gen/choose -1000 1000))
               (gen/elements [0.0 -0.0 1.5 -2.25])
               (gen/fmap bigint gen/large-integer)
               (gen/fmap biginteger gen/large-integer)
               (gen/fmap #(bigint (* 1000000000000 (bigint %))) gen/large-integer)
               (gen/fmap #(BigDecimal. (str % ".500")) gen/small-integer)
               gen/string-ascii gen/keyword gen/keyword-ns gen/uuid]))

(def gen-value
  (gen/recursive-gen
   (fn [inner] (gen/one-of [(gen/vector inner 0 4)
                            (gen/set inner {:max-elements 4})
                            (gen/map inner inner {:max-elements 4})]))
   gen-leaf))

(defn- mostly
  "Mostly a good value, sometimes a bad one."
  [good bad]
  (gen/frequency [[3 (gen/elements good)] [1 (gen/elements bad)]]))

(def gen-control-fact
  "A fact the gate projects, with a value mostly good for its projection."
  (gen/let [layer (gen/elements layers)
            [k v] (gen/one-of
                   [(gen/tuple (gen/return :kind) (mostly [:personal :hand :agent] [:bogus "personal" 3]))
                    (gen/tuple (gen/return :owner) (mostly [:alice :bob] ["alice" 7 nil]))
                    (gen/tuple (gen/return :class) (mostly [:by-layer :by-entity] [:sideways "x"]))
                    (gen/tuple (gen/return :lock-grain) (mostly [:per-act :per-value] [:hourly "per-act"]))
                    (gen/tuple (gen/return :permission)
                               (mostly [{:id [:alice :alice :alice]} {:id [:bob :alice :alice]}
                                        {:id [:carol :alice-agent :alice-agent]}]
                                       [{:id "x"} {:id [:a :b]} {:id [:alice :alice :alice] :x 1} "grant"]))
                    (gen/tuple (gen/return :revoke)
                               (mostly [{:permission [:alice :alice :alice]} {:permission [:carol :alice :alice]}
                                        {:permission [:alice :alice-hand :alice-hand]}]
                                       [{:permission 5} {} [:alice :alice :alice]]))])]
    {:e layer :k k :v v :layer-hint layer}))

(def gen-fact
  (gen/let [e (gen/elements [:e0 :e1 :e2]) k (gen/elements [:note :mention :tag])
            v gen-value
            replaces (gen/one-of [(gen/return nil) gen-fid])
            mark (gen/elements [nil #{} #{:own-row} #{:die-with-any} #{:own-row :die-with-any}])
            layer-tag (gen/frequency [[6 (gen/return nil)] [1 (gen/elements layers)]])]
    (cond-> {:e e :k k :v v}
      replaces (assoc :replaces replaces)
      mark (assoc :mark mark)
      layer-tag (assoc :layer layer-tag))))

(defn- tag-name
  "A fresh :offer name for the offer as the client would make it."
  [o]
  (env/name-for o))

(def gen-offer
  "A well-formed offer, as a client builds it: sometimes with control facts
  on the layer's own entity, sometimes carrying stood-on stamps."
  (gen/let [layer (gen/elements layers)
            who (gen/frequency [[2 (gen/return :alice)] [1 (gen/return :bob)] [2 (gen/return :operator)]])
            ordinary (gen/vector gen-fact 0 4)
            controls (gen/vector gen-control-fact 0 2)
            subjects (gen/set (gen/elements [:bob :carol :dan]) {:max-elements 3})
            stood (gen/map gen-fid (gen/choose 0 2000000000) {:max-elements 3})
            cw (gen/one-of [(gen/return nil) gen/large-integer])
            because (gen/one-of [(gen/return nil) gen-name])
            session (gen/elements [nil :alice-hand])
            cls (gen/frequency [[5 (gen/return :by-layer)] [1 (gen/return :by-entity)]])
            cite (gen/frequency [[6 (gen/return :own)] [1 (gen/elements [:other-layer :other-who :nil])]])]
    (let [facts (into ordinary (map #(-> % (assoc :e layer) (dissoc :layer-hint)) controls))
          facts (if (empty? facts) [{:e :e0 :k :note :v "x"}] facts)
          o {:version 1 :who who :layer layer :class cls
             :permission (when-not (= :operator who)
                           (case cite
                             :own [who layer layer]
                             :other-layer [who layer :alice-hand]
                             :other-who [:alice layer layer]
                             :nil nil))
             :session session :stood-on stood :because-of because :claimed-when cw
             :subjects subjects :facts facts}]
      (assoc o :name (tag-name o)))))

(def mutations
  "Ways a record goes wrong on its way to the gate, and two that keep it
  well-formed but hand the gate a subvec."
  [(fn [o _] (dissoc o :who))
   (fn [o g] (assoc o :junk g))
   (fn [o _] (assoc o :version 2))
   (fn [o g] (assoc o :name g))
   (fn [o _] (assoc o :name (str (:name o))))
   (fn [o _] (update o :name subv))
   (fn [o _] (assoc o :who :store))
   (fn [o _] (assoc-in o [:name 2] :crossing))
   (fn [o _] (assoc-in o [:name 2] :landing))
   (fn [o _] (assoc o :facts []))
   (fn [o g] (assoc o :facts g))
   (fn [o g] (assoc-in o [:facts 0 :v] g))
   (fn [o g] (assoc-in o [:facts 0 :zz] g))
   (fn [o g] (assoc-in o [:facts 0 :replaces] g))
   (fn [o _] (assoc-in o [:facts 0 :v] (nest 40)))
   (fn [o _] (assoc-in o [:facts 0 :v] (nest 3000)))
   (fn [o _] (assoc-in o [:facts 0 :v] '(1 2 3)))
   (fn [o _] (assoc-in o [:facts 0 :v] 1/3))
   (fn [o _] (assoc-in o [:facts 0 :v] (keyword "a b")))
   (fn [o _] (assoc-in o [:facts 0 :v] (java.util.concurrent.atomic.AtomicLong. 1)))
   (fn [o _] (assoc o :subjects (set (map #(keyword (str "p" %)) (range 257)))))
   (fn [o _] (assoc o :stood-on {[(:name o) 0] Long/MAX_VALUE}))
   (fn [o g] (assoc o :stood-on g))
   (fn [o _] (update o :permission #(when % (subv %))))
   (fn [o _] (update o :because-of #(when % (subv %))))
   (fn [o _] (assoc-in o [:facts 0 :replaces] [(subv (:name o)) 0]))
   (fn [o g] (assoc o :layer g))
   (fn [o _] (assoc o :layer :alice-hand))
   (fn [o _] (assoc o :class :by-entity))
   (fn [o _] (assoc-in o [:name 1] :by-entity))
   (fn [o _] (assoc-in o [:name 1] nil))
   (fn [o g] (assoc o :claimed-when g))
   (fn [o g] (assoc o :subjects g))
   (fn [o _] (assoc-in o [:facts 0 :layer] :alice-agent))])

(def gen-record
  "Anything a client could append: garbage, or a well-formed offer with up
  to three things done to it."
  (gen/one-of
   [gen/any
    (gen/let [o gen-offer
              ms (gen/vector (gen/choose 0 (dec (count mutations))) 0 3)
              g gen/any]
      ;; a mutation that does not apply to an already mutated record (say,
      ;; assoc-in into facts that are now a number) leaves it as it is
      (reduce (fn [o i] (try ((mutations i) o g) (catch Throwable _ o))) o ms))]))

;; ------------------------------------------------------------ parse: total

(deftest parse-is-total
  (testing "the unguarded parser returns {:ok offer} or a face reason for anything a client could append"
    (check! "parse* total" 1500
            (prop/for-all [x gen-record]
              (let [r (parse* x :stream)]
                (or (and (contains? r :ok) (map? (:ok r)))
                    (contains? face-reasons (:refuse r)))))))
  (testing "intake never reports a gate error: digest and read keys are total on every parsed offer"
    (check! "intake total" 1000
            (prop/for-all [x gen-record]
              (let [r (gate/intake x)]
                (not= :gate-error (:refuse r)))))))

(defn- exact-vector? [x] (identical? PersistentVector (class x)))

(defn- classes-ok?
  "Every vector the parser hands on is a PersistentVector, every Long
  position a Long, every keyword position a keyword: the classes the
  `$$layers` schema names (F6)."
  [o]
  (and (exact-vector? (:name o))
       (or (nil? (:permission o)) (exact-vector? (:permission o)))
       (or (nil? (:because-of o)) (exact-vector? (:because-of o)))
       (or (nil? (:claimed-when o)) (instance? Long (:claimed-when o)))
       (every? (fn [[k v]] (and (exact-vector? k) (exact-vector? (first k)) (instance? Long (second k))
                                (instance? Long v)))
               (:stood-on o))
       (every? keyword? (:subjects o))
       (<= (count (:subjects o)) env/max-subjects)
       (exact-vector? (:facts o))
       (every? (fn [f] (and (keyword? (:e f)) (keyword? (:k f)) (set? (:mark f))
                            (or (nil? (:replaces f))
                                (and (exact-vector? (:replaces f)) (exact-vector? (first (:replaces f)))
                                     (instance? Long (second (:replaces f)))))
                            (= (:v f) (env/normalize-value (:v f)))))
               (:facts o))))

(deftest parse-hands-on-schema-classes
  (testing "for every accepted record, including a subvec in a vector position (F6, F13)"
    (check! "parsed classes" 1500
            (prop/for-all [x gen-record]
              (let [r (parse* x :stream)]
                (or (contains? r :refuse) (classes-ok? (:ok r)))))))
  (testing "a subvec in each vector position comes out a PersistentVector"
    (let [n (nm :alice :by-layer)
          o {:version 1 :name (subv n) :who :alice :layer :alice :class :by-layer
             :permission (subv [:alice :alice :alice])
             :because-of (subv (nm :alice :by-layer))
             :stood-on {[(subv (nm :alice-hand :by-layer)) 0] 5}
             :facts [{:e :e0 :k :note :v (subv [1 2 (subv [3])]) :replaces [(subv n) (int 1)]}]}
          r (env/parse o)]
      (is (contains? r :ok))
      (is (classes-ok? (:ok r)))
      (is (= [1 2 [3]] (-> r :ok :facts first :v)))
      (is (exact-vector? (-> r :ok :facts first :v)))
      (is (exact-vector? (nth (-> r :ok :facts first :v) 2))))))

;; ------------------------------------------------------------ parse: bounds

(deftest parse-bounds
  (let [base (fn [v] {:version 1 :name (nm :alice :by-layer) :who :alice :layer :alice
                      :class :by-layer :permission [:alice :alice :alice]
                      :facts [{:e :e0 :k :note :v v}]})]
    (testing "a value may nest in 32 collections; 33 is malformed; 3000 is malformed without a deep walk (F6)"
      (is (true? (env/edn-value? (nest 32))))
      (is (false? (env/edn-value? (nest 33))))
      (is (contains? (env/parse (base (nest 32))) :ok))
      (is (= {:refuse :malformed} (env/parse (base (nest 33)))))
      (is (= {:refuse :malformed} (parse* (base (nest 3000)) :stream)) "unguarded: the walk stops at the bound")
      (is (= {:refuse :malformed} (parse* (base (nest 200000)) :stream)))
      (is (string? (env/canonical (nest 32)))))
    (testing "nested maps and sets count the same way"
      (let [nest-map (fn [n] (loop [v 1 i 0] (if (< i n) (recur {:a v} (inc i)) v)))
            nest-set (fn [n] (loop [v 1 i 0] (if (< i n) (recur #{v} (inc i)) v)))]
        (is (true? (env/edn-value? (nest-map 32))))
        (is (false? (env/edn-value? (nest-map 33))))
        (is (false? (env/edn-value? (nest-set 33))))))
    (testing "256 carried subjects pass; 257 are malformed (F3)"
      (let [subjects (fn [n] (set (map #(keyword (str "p" %)) (range n))))]
        (is (contains? (env/parse (assoc (base 1) :subjects (subjects 256))) :ok))
        (is (= {:refuse :malformed} (env/parse (assoc (base 1) :subjects (subjects 257)))))))
    (testing "a carried stamp must be below 2^62 (proposed rig choice); negative and Integer stamps pass"
      (let [f [(nm :alice-hand :by-layer) 0]]
        (is (contains? (env/parse (assoc (base 1) :stood-on {f (dec env/max-carried-stamp)})) :ok))
        (is (= {:refuse :malformed} (env/parse (assoc (base 1) :stood-on {f env/max-carried-stamp}))))
        (is (= {:refuse :malformed} (env/parse (assoc (base 1) :stood-on {f Long/MAX_VALUE}))))
        (is (= {:refuse :malformed} (env/parse (assoc (base 1) :stood-on {f 1.5}))))
        (is (contains? (env/parse (assoc (base 1) :stood-on {f -5})) :ok))
        (is (= 5 (get-in (env/parse (assoc (base 1) :stood-on {f (int 5)})) [:ok :stood-on f])))))
    (testing "values outside P5's list are malformed: lists, symbols, characters, ratios, records, NaN, Java floats, unreadable keywords"
      (doseq [v ['(1 2) 'sym \c 1/3 (java.util.Date.) Double/NaN Double/POSITIVE_INFINITY (float 1.5) #{(float 1.5)}
                 (keyword "a b") (keyword "") (keyword "x/y" "z") {:a '(1)} #{(keyword "a b")}
                 (java.util.concurrent.atomic.AtomicLong. 1) (java.util.HashMap.)]]
        (is (= {:refuse :malformed} (env/parse (base v))) (pr-str v))))
    (testing "values inside it pass, normalised to one form per = class"
      (doseq [[v want] [[(int 7) 7] [(short 7) 7] [(byte 7) 7] [7N 7] [(biginteger 7) 7]
                        [-0.0 0.0] [#{-0.0} #{0.0}] [1.500M 1.5M] [100M 1E+2M]
                        [(sorted-map :b 2 :a 1) {:a 1 :b 2}] [(sorted-set 3 1) #{1 3}]
                        [(bigint 1e30) (bigint 1e30)]]]
        (let [got (-> (env/parse (base v)) :ok :facts first :v)]
          (is (= want got) (pr-str v))
          (is (= (class want) (class got)) (pr-str v)))))))

(deftest face-refusals-by-reason
  (let [n (nm :alice :by-layer)
        ok {:version 1 :name n :who :alice :layer :alice :class :by-layer
            :permission [:alice :alice :alice] :facts [{:e :e0 :k :note :v "x"}]}]
    (is (contains? (env/parse ok) :ok))
    (doseq [[what x reason]
            [["a number" 42 :malformed]
             ["a string" "offer" :malformed]
             ["a vector" [1 2] :malformed]
             ["a record type" (java.util.Date.) :malformed]
             ["an unknown act part" (assoc ok :signature "x") :unknown-part]
             ["an unknown fact part" (assoc-in ok [:facts 0 :colour] :red) :unknown-part]
             ["version 2" (assoc ok :version 2) :unknown-version]
             ["no version" (dissoc ok :version) :unknown-version]
             ["a name that is a string" (assoc ok :name (str n)) :bad-name]
             ["a name of three parts" (assoc ok :name (subvec n 0 3)) :bad-name]
             ["a name with an unknown scheme" (assoc-in ok [:name 2] :mine) :bad-name]
             ["a name whose id is not a uuid" (assoc-in ok [:name 3] "id") :bad-name]
             ["who is missing" (dissoc ok :who) :malformed]
             [":who :store (F6)" (assoc ok :who :store) :reserved-who]
             ["a class outside the two" (assoc ok :class :sideways) :malformed]
             ["a permission of two parts" (assoc ok :permission [:alice :alice]) :malformed]
             ["the :crossing scheme" (assoc-in ok [:name 2] :crossing) :reserved-scheme]
             ["the :landing scheme" (assoc-in ok [:name 2] :landing) :reserved-scheme]
             ["the operator under the :crossing scheme" (assoc ok :who :operator :permission nil :name (assoc n 2 :crossing)) :reserved-scheme]
             ["an empty act" (assoc ok :facts []) :empty-act]
             ["facts that are a map" (assoc ok :facts {:e :e0}) :malformed]
             ["a name made for the micro gate" (assoc ok :name (nm :alice :by-entity) :class :by-entity) :wrong-gate]
             ["a name made for another layer" (assoc ok :layer :alice-hand) :mis-tagged]
             ["a name made with no class for ordinary facts" (assoc-in ok [:name 1] nil) :mis-tagged]
             ["a name made for a store-placed act, with a class" (assoc ok :facts [{:e :alice :k :lock-grain :v :per-act}]) :mis-tagged]]]
      (is (= {:refuse reason} (env/parse x)) what)
      (is (= reason (:refuse (gate/intake x))) what))))

;; ------------------------------------------------------------------ values

(deftest values-read-back-as-offered
  (testing "every accepted value is = to its normalised form, and its slot text reads back = (P12)"
    (check! "value round trip" 1500
            (prop/for-all [v gen-value]
              (or (not (env/edn-value? v))
                  (let [nv (env/normalize-value v)]
                    (and (= v nv)
                         (= nv (env/normalize-value nv))
                         (= nv (env/decode-value (env/encode-value nv)))))))))
  (testing "a keyword the parser takes reads back as itself"
    (check! "readable keywords" 1500
            (prop/for-all [k (gen/one-of [gen/keyword gen/keyword-ns
                                          (gen/fmap keyword gen/string)
                                          (gen/fmap (fn [[a b]] (keyword a b)) (gen/tuple gen/string-alphanumeric gen/string))])]
              (or (not (env/readable-keyword? k))
                  (= k (edn/read-string (pr-str k))))))))

;; ------------------------------------------------------------------ digest

(defn- rebuild-value
  "An = value built differently: maps in reverse insertion order, vectors
  as subvecs, sets rebuilt, integers as ints or big integers, a decimal
  with trailing zeros."
  [x]
  (cond
    (map? x) (into {} (reverse (map (fn [[k v]] [(rebuild-value k) (rebuild-value v)]) x)))
    (vector? x) (subv (mapv rebuild-value x))
    (set? x) (into #{} (reverse (map rebuild-value x)))
    (and (instance? Long x) (<= Integer/MIN_VALUE x Integer/MAX_VALUE)) (int x)
    (instance? Long x) (bigint x)
    (decimal? x) (.setScale ^BigDecimal x (+ 2 (max 0 (.scale ^BigDecimal x))))
    :else x))

(defn- rebuild
  "An = offer built differently: every map in reverse insertion order,
  every vector a subvec, integers in the envelope as ints where they fit,
  and each fact's value by `rebuild-value`."
  [x]
  (cond
    (and (map? x) (contains? x :k) (contains? x :v))
    (into {} (reverse (map (fn [[k v]] [k (if (= :v k) (rebuild-value v) (rebuild v))]) x)))
    (map? x) (into {} (reverse (map (fn [[k v]] [(rebuild k) (rebuild v)]) x)))
    (vector? x) (subv (mapv rebuild x))
    (set? x) (into #{} (reverse (map rebuild x)))
    (and (instance? Long x) (<= Integer/MIN_VALUE x Integer/MAX_VALUE)) (int x)
    :else x))

(defn- digest-of [raw] (env/offer-digest raw))

(deftest digest-properties
  (testing "= offers built in other orders and widths have one digest (P6)"
    (check! "digest respects =" 800
            (prop/for-all [o gen-offer]
              (let [o2 (rebuild o)
                    d1 (digest-of o)]
                (and (= o o2) (= d1 (digest-of o2)))))))
  (testing "offers whose parsed content differs have different digests"
    (check! "digest separates" 800
            (prop/for-all [a gen-offer b gen-offer]
              (let [pa (:ok (env/parse a)) pb (:ok (env/parse b))]
                (or (nil? pa) (nil? pb)
                    (= (dissoc pa :name) (dissoc pb :name))
                    (not= (env/digest pa) (env/digest pb)))))))
  (let [o {:version 1 :name (nm :alice :by-layer) :who :alice :layer :alice :class :by-layer
           :permission [:alice :alice :alice] :claimed-when 5 :subjects #{:bob}
           :stood-on {[(nm :alice-hand :by-layer) 0] 9}
           :facts [{:e :e0 :k :note :v "1"} {:e :e1 :k :mention :v {:persons #{:bob}}}
                   {:e :e2 :k :tag :v "one"}]}
        d (digest-of o)]
    (testing "every part is covered: one change anywhere changes the digest"
      (doseq [[what o2] [["the value's type" (assoc-in o [:facts 0 :v] 1)]
                         ["a keyword for a string" (assoc-in o [:facts 2 :v] :one)]
                         ["a vector for a set" (assoc-in o [:facts 1 :v :persons] [:bob])]
                         ["a mark" (assoc-in o [:facts 0 :mark] #{:own-row})]
                         ["what it replaces" (assoc-in o [:facts 0 :replaces] [(nm :alice :by-layer) 0])]
                         ["the fact order" (update o :facts (comp vec reverse))]
                         ["claimed-when (a rebuilt map is other content)" (assoc o :claimed-when 6)]
                         ["a carried stamp" (assoc o :stood-on {(first (keys (:stood-on o))) 10})]
                         ["the subjects" (assoc o :subjects #{:bob :carol})]
                         ["who" (assoc o :who :bob :permission [:bob :alice :alice])]
                         ["the permission" (assoc o :permission [:alice :alice :alice-hand])]
                         ["the session" (assoc o :session :alice-hand)]
                         ["because-of" (assoc o :because-of (nm :alice :by-layer))]]]
        (is (some? (digest-of o2)) what)
        (is (not= d (digest-of o2)) what)))
    (testing "the name is not covered"
      (is (= d (digest-of (assoc o :name (nm :alice :by-layer))))))
    (testing "it is keyed: not the plain SHA-256 of the same text (D8)"
      (let [text (env/canonical (dissoc (:ok (env/parse o)) :name))
            sha (let [md (MessageDigest/getInstance "SHA-256")]
                  (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest md (.getBytes ^String text "UTF-8")))))]
        (is (= 64 (count d)))
        (is (not= sha d))))))

;; --------------------------------------------------------------- names, tags

(deftest names-and-tags
  (testing "a name carries the layer and, unless the store places the act, the class (P4)"
    (is (= [:alice :by-layer :offer] (subvec (env/name-for :alice :by-layer [{:k :note}]) 0 3)))
    (is (= [:alice nil :offer] (subvec (env/name-for :alice :by-layer [{:k :lock-grain}]) 0 3)))
    (is (= [:alice nil :offer] (subvec (env/name-for :alice :by-layer [{:k :class} {:k :forget}]) 0 3)))
    (is (= [:alice :by-layer :offer] (subvec (env/name-for :alice :by-layer [{:k :lock-grain} {:k :note}]) 0 3))))
  (testing "the derived names of stage 4 keep the request's id under the reserved schemes"
    (let [req (nm :alice :by-layer)]
      (is (= [:alice nil :crossing (nth req 3)] (env/crossing-name req)))
      (is (= [:group :by-entity :landing (nth req 3)] (env/landing-name req :group)))))
  (testing "uuid7: version 7, RFC variant, the current millisecond in its top 48 bits (R12)"
    (let [t0 (System/currentTimeMillis)
          us (repeatedly 50 env/uuid7)
          t1 (System/currentTimeMillis)]
      (is (every? #(= 7 (.version ^UUID %)) us))
      (is (every? #(= 2 (.variant ^UUID %)) us))
      (is (every? #(<= t0 (bit-shift-right (.getMostSignificantBits ^UUID %) 16) t1) us))
      (is (= 50 (count (set us))))))
  (testing "tag-of agrees with the model's tag-of on every offer the model sends in its fixed histories"
    (let [histories (concat (map second fs/a-cases) (map second fs/b-cases) (map second fs/d-cases)
                            [[[:set-grain :alice] [:work 0] [:reclass :alice-agent] [:work 2]
                              [:forget-value 0]]
                             [[:offer {:who :alice :layer :alice :facts [{:e :e0 :k :note :replaces :none :mark #{}}]}]
                              [:work 0] [:forget-value 0] [:revoke [:alice :alice :own]] [:work 0]]])
          offers (for [h histories
                       o (vals (:offers (fm/run fm/baseline h)))]
                   o)
          translate (fn [o] {:layer (:layer o) :class (:class o)
                             :facts (mapv #(select-keys % [:e :k :v :replaces]) (:facts o))})]
      (is (< 20 (count offers)) "the histories send offers of every kind")
      (is (some #(every? :at (:facts %)) offers) "store-placed offers among them")
      (doseq [o offers]
        (is (= (fm/tag-of o) (env/tag-of (translate o))) (pr-str (select-keys o [:name :layer :class]))))))
  (testing "mis-tagged? is the tag check"
    (let [o {:layer :alice :class :by-layer :facts [{:e :e0 :k :note}] :name (nm :alice :by-layer)}]
      (is (false? (env/mis-tagged? o)))
      (is (true? (env/mis-tagged? (assoc o :layer :alice-hand))))
      (is (true? (env/mis-tagged? (assoc o :name (nm :alice nil))))))))

;; ------------------------------------------------------ the decision, pure

(def alice-settings {:kind :personal :owner :alice :class :by-layer :grain :per-value})

(defn- parsed [raw] (:ok (env/parse raw)))

(defn- offer*
  "A parsed offer: `who` into `layer` with `facts`, under a fresh name."
  [who layer facts & {:as more}]
  (let [raw (merge {:version 1 :who who :layer layer :class :by-layer
                    :permission (when-not (= :operator who) [who layer layer])
                    :facts facts}
                   more)
        raw (assoc raw :name (or (:name more) (env/name-for raw)))]
    (or (parsed raw) (throw (ex-info "offer* built a record the parser refuses" {:raw raw :r (env/parse raw)})))))

(def granted-row {:granted [[:alice :by-layer :offer (UUID. 1 1)] 0]})

(deftest refusal-order
  (let [n1 [:alice :by-layer :offer (UUID. 7 7)]
        h1 [:e0 :note [n1 0]]
        rows {[:alice :alice :alice] granted-row
              [:alice :alice :alice-hand] granted-row
              [:bob :alice :alice] (assoc granted-row :revoked [n1 1])}
        heads {h1 5}
        note {:e :e0 :k :note :v "x"}
        outside (assoc note :layer :alice-hand)
        replace-stale {:e :e0 :k :note :v "y" :replaces [(nm :alice :by-layer) 0]}
        decide (fn [o & {:keys [settings rows heads] :or {settings alice-settings rows rows heads {}}}]
                 (gate/refusal o settings rows heads))]
    (testing "one reason at a time, as the plan's step 4 orders them"
      (is (nil? (decide (offer* :alice :alice [note]))))
      (is (= :fact-outside-the-acts-layer (decide (offer* :alice :alice [outside]))))
      (is (= :no-such-layer (decide (offer* :alice :yan [note] :permission [:alice :yan :yan]) :settings nil)))
      (is (= :class-mismatch (decide (offer* :alice :alice [{:e :alice :k :lock-grain :v :per-act}] :class :by-entity))))
      (is (= :permission-does-not-cover-this (decide (offer* :bob :alice [note] :permission [:alice :alice :alice]))))
      (is (= :permission-does-not-cover-this (decide (offer* :alice :alice [note] :permission nil))))
      (is (= :permission-does-not-cover-this (decide (offer* :alice :alice [note] :permission [:alice :alice-hand :alice-hand]))))
      (is (= :permission-from-another-layer (decide (offer* :alice :alice [note] :permission [:alice :alice :alice-hand]))))
      (is (= :no-permission (decide (offer* :carol :alice [note] :permission [:carol :alice :alice]))))
      (is (= :permission-revoked (decide (offer* :bob :alice [note] :permission [:bob :alice :alice]))))
      (is (= :malformed-control (decide (offer* :operator :alice [{:e :alice :k :lock-grain :v :hourly}]))))
      (is (= :malformed-control (decide (offer* :operator :alice [{:e :perm-bob :k :permission :v {:id "x"}}]))))
      (is (= :control-not-allowed (decide (offer* :alice :alice [{:e :perm-alice :k :permission :v {:id [:alice :alice :alice]}}]))))
      (is (= :control-not-allowed (decide (offer* :alice :alice [{:e :alice :k :owner :v :bob}]))))
      (is (= :control-not-allowed (decide (offer* :alice :alice [{:e :alice :k :class :v :by-entity}]))))
      (is (= :control-not-allowed (decide (offer* :bob :alice [{:e :alice :k :lock-grain :v :per-act}] :permission [:bob :alice :alice])
                                          :rows (assoc rows [:bob :alice :alice] granted-row))))
      (is (nil? (decide (offer* :alice :alice [{:e :alice :k :lock-grain :v :per-act}]))) "the owner switches her grain")
      (is (= :stale-replaces (decide (offer* :alice :alice [replace-stale]))))
      (is (nil? (decide (offer* :alice :alice [{:e :e0 :k :note :v "y" :replaces [n1 0]}]) :heads heads)))
      (is (= :stale-replaces (decide (offer* :alice :alice [{:e :e0 :k :note :v "y" :replaces [n1 0]}
                                                            {:e :e0 :k :note :v "z" :replaces [n1 0]}])
                                     :heads heads))
          "two facts of one act replacing one fact")
      (is (= :stale-replaces (decide (offer* :alice :alice [{:e :e0 :k :tag :v "y" :replaces [n1 0]}]) :heads heads))
          "a replace on another key finds no head")
      (is (= :stale-revoke (decide (offer* :operator :alice [{:e :perm-carol :k :revoke :v {:permission [:carol :alice :alice]}}]))))
      (is (= :stale-revoke (decide (offer* :operator :alice [{:e :perm-bob :k :revoke :v {:permission [:bob :alice :alice]}}])))
          "already revoked")
      (is (= :stale-revoke (decide (offer* :operator :alice [{:e :perm-alice :k :revoke :v {:permission [:alice :alice :alice]}}
                                                             {:e :perm-alice :k :revoke :v {:permission [:alice :alice :alice]}}])))
          "two revokes of one permission in one act")
      (is (nil? (decide (offer* :operator :alice [{:e :perm-alice :k :revoke :v {:permission [:alice :alice :alice]}}]))))
      (is (= :layer-already-made (decide (offer* :operator :alice [{:e :alice :k :owner :v :bob}]))))
      (is (= :layer-already-made (decide (offer* :operator :alice [{:e :alice :k :kind :v :hand}]))))
      (is (nil? (decide (offer* :operator :new [{:e :new :k :kind :v :hand} {:e :new :k :owner :v :carol}
                                                {:e :new :k :class :v :by-layer} {:e :new :k :lock-grain :v :per-value}])
                        :settings nil))
          "a making act on an unmade layer: the class in force is its own class fact (P10)")
      (is (= :unsupported-reclass (decide (offer* :operator :alice [{:e :alice :k :class :v :by-layer}] :class :by-entity)
                                          :settings (assoc alice-settings :class :by-entity))))
      (is (nil? (decide (offer* :operator :alice [{:e :alice :k :class :v :by-entity}]))) "the re-class itself"))
    (testing "when several reasons hold, the earliest in the order is the answer"
      ;; :layer-already-made before :unsupported-reclass cannot be built at the
      ;; stream gate: a :kind fact makes the act's tag carry its class, and the
      ;; class check passes only with :by-entity, which is :wrong-gate here.
      (let [cases [[:fact-outside-the-acts-layer (offer* :alice :alice [{:e :alice :k :lock-grain :v :per-act :layer :alice-hand}]
                                                         :class :by-entity)]
                   [:class-mismatch (offer* :bob :alice [{:e :alice :k :lock-grain :v :per-act}] :class :by-entity
                                            :permission [:alice :alice :alice])]
                   [:permission-does-not-cover-this (offer* :bob :alice [note] :permission [:alice :alice :alice-hand])]
                   [:permission-from-another-layer (offer* :carol :alice [note] :permission [:carol :alice :alice-hand])]
                   [:permission-revoked (offer* :bob :alice [replace-stale {:e :alice :k :lock-grain :v :hourly}]
                                                :permission [:bob :alice :alice])]
                   [:malformed-control (offer* :alice :alice [replace-stale {:e :alice :k :owner :v "bob"}])]
                   [:control-not-allowed (offer* :alice :alice [replace-stale {:e :perm-alice :k :revoke :v {:permission [:carol :alice :alice]}}])]
                   [:stale-replaces (offer* :operator :alice [replace-stale {:e :perm-carol :k :revoke :v {:permission [:carol :alice :alice]}}])]
                   [:stale-revoke (offer* :operator :alice [{:e :alice :k :owner :v :bob} {:e :perm-carol :k :revoke :v {:permission [:carol :alice :alice]}}])]]]
        (doseq [[want o] cases]
          (is (= want (gate/refusal o alice-settings rows {})) (str want)))))
    (testing "every recorded reason the gate can give is one of the plan's"
      (check! "reasons known" 800
              (prop/for-all [raw gen-offer
                             settings (gen/elements [nil alice-settings (assoc alice-settings :class :by-entity)])]
                (let [o (parsed raw)]
                  (or (nil? o)
                      (let [r (gate/refusal o settings rows heads)]
                        (or (nil? r) (some #{r} recorded-reasons))))))))))

(deftest stamps
  (testing "the stamp is max(wall, clock + 1, every carried stood-on + 1, every replaced head + 1) (P9)"
    (check! "stamp-for" 1000
            (prop/for-all [clock (gen/choose 0 5000000)
                           wall (gen/choose 0 5000000)
                           stood (gen/map gen-fid (gen/choose 0 6000000) {:max-elements 3})
                           head-stamps (gen/vector (gen/one-of [(gen/return nil) (gen/choose 0 5000000)]) 0 3)]
              (let [facts (vec (for [i (range (count head-stamps))]
                                 {:e :e0 :k :note :v i :replaces [[:alice :by-layer :offer (UUID. i i)] 0]}))
                    o (offer* :alice :alice (if (seq facts) facts [{:e :e0 :k :note :v 1}]) :stood-on stood)
                    heads (into {} (for [[f h] (map vector (:facts o) head-stamps)]
                                     [[(:e f) (:k f) (:replaces f)] h]))
                    s (gate/stamp-for o heads clock wall)
                    want (apply max wall (inc clock) (concat (map inc (vals (:stood-on o))) (map inc (keep identity head-stamps))))]
                (and (= want s) (instance? Long s) (< clock s) (<= wall s)
                     (every? #(< % s) (vals (:stood-on o)))
                     (every? #(< % s) (keep identity head-stamps))))))))

;; ------------------------------------------- decide: total, and fits the schema

(defn- apply-decision!
  "The writes module.clj makes for a decision, on a test PState with the same
  schema; throws where the module's writes would."
  [tp clock-tp layer nm d]
  (rtest/test-pstate-transform [(keypath layer :answers nm) (termval (:record d))] tp)
  (when (= :yes (:answer (:record d)))
    (rtest/test-pstate-transform [(keypath layer :log nm) (termval (:log d))] tp)
    (doseq [[f s] (:stood-on d)] (rtest/test-pstate-transform [(keypath layer :stood-on nm f) (termval s)] tp))
    (doseq [k (:heads-del d)] (rtest/test-pstate-transform [(keypath layer :heads k) NONE>] tp))
    (doseq [[k s] (:heads-put d)] (rtest/test-pstate-transform [(keypath layer :heads k) (termval s)] tp))
    (when-let [s (:settings d)] (rtest/test-pstate-transform [(keypath layer :settings) (termval s)] tp))
    (doseq [[p row] (:permissions d)] (rtest/test-pstate-transform [(keypath layer :permissions p) (termval row)] tp)))
  (rtest/test-pstate-transform [(termval (:stamp d))] clock-tp))

(def gen-decision-input
  (gen/let [raw gen-offer
            settings (gen/frequency [[6 (gen/return alice-settings)]
                                     [1 (gen/elements [nil (assoc alice-settings :owner :bob)
                                                       (assoc alice-settings :class :by-entity) {:class :by-layer}])]])
            row-states (gen/vector (gen/frequency [[6 (gen/return :granted)] [1 (gen/return nil)] [1 (gen/return :revoked)]]) 8)
            head-states (gen/vector (gen/frequency [[3 (gen/choose 0 1000)] [1 (gen/return nil)]]) 8)
            clock (gen/choose 0 2000000000)
            wall (gen/choose 0 2000000000)]
    (let [o (parsed raw)]
      (when o
        (let [pids (gate/pids-to-read o)
              hks (gate/heads-to-read o)
              fid [(nm :alice :by-layer) 0]]
          {:offer o :settings settings :clock clock :wall wall
           :rows (into {} (map (fn [p st] [p (case st nil nil :granted {:granted fid} :revoked {:granted fid :revoked fid})])
                               pids (cycle row-states)))
           :heads (into {} (map vector hks (cycle head-states)))})))))

(deftest decide-is-total-and-fits-the-schema
  (with-open [tp (rtest/create-test-pstate m/layers-schema)
              clock-tp (rtest/create-test-pstate Long)]
    (testing "the unguarded decision returns a decision for every parsed offer, and its writes fit $$layers (I-G1)"
      (check! "decide total + schema" 700
              (prop/for-all [in gen-decision-input]
                (or (nil? in)
                    (let [{:keys [offer settings rows heads clock wall]} in
                          d (decide* offer settings rows heads clock wall (env/digest offer))
                          yes? (= :yes (:answer (:record d)))]
                      (apply-decision! tp clock-tp (:layer offer) (:name offer) d)
                      (and (= :decide (:kind d))
                           (instance? Long (:stamp d))
                           (= (:stamp d) (:stamp (:record d)) (:stamp (:ack d)))
                           (= yes? (nil? (:reason (:record d))))
                           (= (:answer (:record d)) (:answer (:ack d)))
                           (if yes?
                             (and (= (count (:facts offer)) (count (:log d)) (count (:heads-put d)))
                                  (every? #(= (:stamp d) (second %)) (:heads-put d))
                                  (= (:stood-on offer) (:stood-on d)))
                             (and (nil? (:log d)) (empty? (:heads-put d)) (empty? (:heads-del d))
                                  (nil? (:settings d)) (empty? (:permissions d)) (empty? (:stood-on d))))))))))
    (testing "the guarded decision turns a failure into the unrecorded :gate-error (F6)"
      (let [o (offer* :alice :alice [{:e :e0 :k :note :v 1}])]
        (is (= {:kind :face :ack {:answer :no :reason :gate-error :stamp nil :name (:name o)}}
               (gate/decide o alice-settings {[:alice :alice :alice] granted-row} {} "not a clock" 0 "d")))))
    (testing "a refused making act whose owner fact is a string still writes a record that fits (the phase 4 finding)"
      (let [o (offer* :operator :wren [{:e :wren :k :kind :v :personal} {:e :wren :k :owner :v "wren"}
                                       {:e :wren :k :class :v :by-layer}])
            d (gate/decide o nil {} {} 10 20 (env/digest o))]
        (is (= :malformed-control (:reason (:record d))))
        (is (= #{} (:subjects (:record d))))
        (is (= :written (do (apply-decision! tp clock-tp :wren (:name o) d) :written)))))))
