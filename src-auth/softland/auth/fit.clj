(ns softland.auth.fit
  "Input-form checks for `softland.auth.client`, matching Auth's contract
  (`src/proposal/runtime-2026-11-21/spine/auth/SPEC.md`). Pure code.

  An argument that does not fit gives nil here, and the client answers
  `{:could-not :read-the-event}` without appending any depot record. Every
  time is coerced to an exact `Long`: Rama's record schemas match the exact
  class, and a boxed `Integer` does not fit where `Long` is declared.

  Every check catches `Throwable`, so that an exotic map (such as a sorted
  map whose comparator throws on keyword keys) counts as unfit.")

;; ------------------------------------------------------------------- bounds

(def ^:private longest-text
  "In JVM `char`s; also for a provider keyword's full name."
  256)

(def ^:private longest-said
  "In JVM `char`s, for one value of a `:says` map."
  2048)

(def ^:private most-said
  "Entries of a `:says` map."
  32)

;; ---------------------------------------------------------- value predicates

(defn time-val?
  "`int?` takes `Byte`, `Short`, `Integer` and `Long`, never `BigInt` or
  `BigInteger`."
  [x]
  (int? x))

(defn id?
  [x]
  (uuid? x))

(defn hash-str?
  [x]
  (and (string? x)
       (= 64 (.length ^String x))
       (some? (re-matches #"[0-9a-f]{64}" x))))

(defn carried?
  "True when `s` holds no surrogate `char` without its pair. Rama keeps a
  string as UTF-8, so a string that holds one would be kept changed (the
  unpaired surrogate becomes `?`), and two accounts whose subjects differ
  only there would be one."
  [^String s]
  (let [n (.length s)]
    (loop [i 0]
      (if (>= i n)
        true
        (let [c (.charAt s i)]
          (cond
            (Character/isHighSurrogate c)
            (if (and (< (inc i) n) (Character/isLowSurrogate (.charAt s (inc i))))
              (recur (+ i 2))
              false)

            (Character/isLowSurrogate c)
            false

            :else
            (recur (inc i))))))))

(defn code-str?
  [x]
  (and (string? x) (= 8 (.length ^String x)) (carried? x)))

(defn text?
  [x]
  (and (string? x) (<= (.length ^String x) longest-text) (carried? x)))

(defn provider?
  [x]
  (and (keyword? x)
       (let [full (subs (str x) 1)]
         (and (<= (.length full) longest-text) (carried? full)))))

(defn account?
  [x]
  (and (vector? x)
       (= 2 (count x))
       (provider? (nth x 0))
       (text? (nth x 1))))

(defn says?
  [x]
  (and (map? x)
       (<= (count x) most-said)
       (every? (fn [[k v]]
                 (and (text? k)
                      (string? v)
                      (<= (.length ^String v) longest-said)
                      (carried? v)))
               x)))

(defn- or-nil
  [pred]
  (fn [x] (or (nil? x) (pred x))))

(defn- is-kw
  [k]
  (fn [x] (= k x)))

(defn- shaped?
  [m required optional]
  (and (map? m)
       (every? (fn [[k ok?]] (and (contains? m k) (ok? (get m k)))) required)
       (every? (fn [k] (or (contains? required k) (contains? optional k))) (keys m))
       (every? (fn [[k ok?]] (or (not (contains? m k)) (ok? (get m k)))) optional)))

(defn claim?
  "Two kinds of one form: Auth is given the hash of an email, which the
  runtime makes, and never the address."
  [x]
  (or (shaped? x {:by (is-kw :email) :hash hash-str?} {})
      (shaped? x {:by (is-kw :code) :hash hash-str?} {})))

(defn kind?
  [x]
  (or (= :browser x) (= :program x)))

(defn making?
  [x]
  (shaped? x {:uuid id? :session id?} {}))

(defn new-agent?
  [x]
  (or (nil? x) (shaped? x {:label text?} {})))

(defn driven-by?
  [x]
  (or (nil? x)
      (shaped? x {:kind (is-kw :person)} {})
      (shaped? x {:kind (is-kw :model) :model text?} {})))

(defn invited-by?
  [x]
  (or (id? x) (= :system x)))

;; ------------------------------------------------------- write & read forms

(def ^:private write-forms
  {:invite         {:person id? :claim claim? :invited-by invited-by? :at time-val? :ends time-val?
                    :op-id id?}
   :enter          {:account account? :says says? :email-hash (or-nil hash-str?) :code-hash (or-nil hash-str?)
                    :own-lane id? :making making? :at time-val?}
   :take-back      {:person id? :claim claim? :invite id? :at time-val?}
   :unlink         {:account account? :link id? :person id? :at time-val?}
   :own-lane-made {:person id? :at time-val?}
   :begin-sign-in  {:root id? :actor id? :sign-in id? :kind kind? :hash hash-str? :until time-val?
                    :label (or-nil text?) :at time-val?}
   :replace-secret {:root id? :sign-in id? :old-hash hash-str? :new-hash hash-str? :until time-val?
                    :previous-until time-val? :at time-val?}
   :end-sign-in    {:root id? :sign-in id? :at time-val?}
   :make-actor     {:root id? :actor id? :made-by id? :label text? :at time-val?}
   :end-actor      {:root id? :actor id? :at time-val?}
   :open-session   {:root id? :sign-in id? :session id? :until time-val? :at time-val?}
   :close-session  {:root id? :session id? :at time-val?}
   :ask            {:code code-str? :hash hash-str? :asking-hash hash-str? :asks text? :ends time-val?
                    :at time-val?}
   :answer-ask     {:code code-str? :yes? boolean? :root id? :actor id? :new-agent new-agent?
                    :sign-in id? :until time-val? :at time-val?}})

(def ^:private write-optionals
  {:open-session {:driven-by driven-by?}})

(def ^:private time-keys
  [:at :until :previous-until :ends])

(defn- coerce-times
  [m]
  (reduce (fn [acc k]
            (if (contains? acc k)
              (assoc acc k (long (get acc k)))
              acc))
          m
          time-keys))

(defn fit-write
  [kind input]
  (try
    (when-let [req (get write-forms kind)]
      (let [opt (get write-optionals kind {})]
        (when (shaped? input req opt)
          (let [plain (into {} input)
                coerced (coerce-times plain)]
            (if (contains? coerced :says)
              (assoc coerced :says (into {} (:says coerced)))
              coerced)))))
    (catch Throwable _ nil)))

(defn fit-who-is
  [query]
  (try
    (when (shaped? query {:root id? :sign-in id? :session (or-nil id?)} {})
      {:root (:root query)
       :sign-in (:sign-in query)
       :session (:session query)})
    (catch Throwable _ nil)))

(defn fit-ask-of
  [code]
  (try
    (when (code-str? code) code)
    (catch Throwable _ nil)))

(defn fit-invitation-of
  [claim]
  (try
    (when (claim? claim) claim)
    (catch Throwable _ nil)))

(defn fit-accounts-of
  [person]
  (try
    (when (id? person) person)
    (catch Throwable _ nil)))

(defn fit-under
  [root]
  (try
    (when (id? root) root)
    (catch Throwable _ nil)))

(defn fit-watch-root
  "`fn?`, not `ifn?`: a keyword, map, set, symbol or var is no callback."
  [root f]
  (try
    (when (and (id? root) (fn? f))
      [root f])
    (catch Throwable _ nil)))
