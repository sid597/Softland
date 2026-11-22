(ns softland.auth.fit
  "Input-form checks for `softland.auth.client`, matching Auth's contract
  (`src/proposal/runtime-2026-11-21/spine/auth/SPEC.md` and Sid's calls of
  22 November).

  Every write and read checks its arguments here before touching Rama: an
  argument that does not fit returns nil here and becomes
  `{:could-not :read-the-event}` at the client edge without appending any
  depot record. Every fixed-width integer time (`Byte`, `Short`, `Integer`,
  `Long`; never `BigInt` or `BigInteger`) is coerced to an exact `Long` so
  Rama's exact-class record schemas never see boxed `Integer` where `Long`
  is declared.

  All checks are wrapped in `try/catch Throwable` so exotic maps (such as a
  sorted map whose comparator throws on keyword keys) count as unfit rather
  than throwing.

  State and effects: none. Pure functions only."
  (:import [java.util Locale]))

;; ------------------------------------------------------------------- bounds

(def ^:private longest-text
  "Maximum JVM `char` length of a text value or provider keyword name."
  256)

(def ^:private longest-said
  "Maximum JVM `char` length of a value inside a `:says` map."
  2048)

(def ^:private most-said
  "Maximum number of entries in a `:says` map."
  32)

;; ---------------------------------------------------------- value predicates

(defn time-val?
  "Whether `x` is a fixed-width integer timestamp (`Byte`, `Short`, `Integer`,
  or `Long`, rejecting `BigInt`, `BigInteger`, floats, and nil)."
  [x]
  (int? x))

(defn id?
  "Whether `x` is a `java.util.UUID`."
  [x]
  (uuid? x))

(defn hash-str?
  "Whether `x` is a 64-character lower-case hexadecimal string."
  [x]
  (and (string? x)
       (= 64 (.length ^String x))
       (some? (re-matches #"[0-9a-f]{64}" x))))

(defn carried?
  "Whether UTF-8 can carry the string `s` as it is: it holds no surrogate
  `char` without its pair. Rama keeps a string as UTF-8, so a string that
  holds one would be kept changed (the unpaired surrogate becomes `?`), and
  two accounts whose subjects differ only there would be one. Such a string
  does not fit wherever the contract takes a string (the build's default of
  23 November, for Sid to turn; SPEC.md)."
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
  "Whether `x` is an 8-character ask code string that UTF-8 can carry."
  [x]
  (and (string? x) (= 8 (.length ^String x)) (carried? x)))

(defn text?
  "Whether `x` is a string of at most 256 JVM `char`s that UTF-8 can carry
  (`carried?`)."
  [x]
  (and (string? x) (<= (.length ^String x) longest-text) (carried? x)))

(defn email?
  "Whether `x` is a lower-case email text (`Locale/ROOT`)."
  [x]
  (and (text? x) (= x (.toLowerCase ^String x Locale/ROOT))))

(defn provider?
  "Whether `x` is a keyword whose full name (including namespace and `/` if
  qualified) is at most 256 JVM `char`s and can be carried by UTF-8."
  [x]
  (and (keyword? x)
       (let [full (subs (str x) 1)]
         (and (<= (.length full) longest-text) (carried? full)))))

(defn account?
  "Whether `x` is `[provider subject]` with a valid provider keyword and text
  subject."
  [x]
  (and (vector? x)
       (= 2 (count x))
       (provider? (nth x 0))
       (text? (nth x 1))))

(defn says?
  "Whether `x` is a `:says` map: at most 32 entries, each from a text key to
  a string value of at most 2,048 JVM `char`s that UTF-8 can carry."
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
  "Returns a predicate accepting `nil` or any value accepted by `pred`."
  [pred]
  (fn [x] (or (nil? x) (pred x))))

(defn- is-kw
  "Returns a predicate accepting only the exact keyword `k`."
  [k]
  (fn [x] (= k x)))

(defn- shaped?
  "Whether `m` is a map containing every key of `required` (passing its
  predicate), optionally any key of `optional` (passing its predicate if
  present), and no other keys."
  [m required optional]
  (and (map? m)
       (every? (fn [[k ok?]] (and (contains? m k) (ok? (get m k)))) required)
       (every? (fn [k] (or (contains? required k) (contains? optional k))) (keys m))
       (every? (fn [[k ok?]] (or (not (contains? m k)) (ok? (get m k)))) optional)))

(defn claim?
  "Whether `x` is `{:by :email :email lower-text}` or `{:by :code :hash h}`."
  [x]
  (or (shaped? x {:by (is-kw :email) :email email?} {})
      (shaped? x {:by (is-kw :code) :hash hash-str?} {})))

(defn kind?
  "Whether `x` is a sign-in kind (`:browser` or `:program`)."
  [x]
  (or (= :browser x) (= :program x)))

(defn making?
  "Whether `x` is `{:uuid UUID :session UUID}` with no extra keys."
  [x]
  (shaped? x {:uuid id? :session id?} {}))

(defn new-agent?
  "Whether `x` is `nil` or `{:label text}` with no extra keys."
  [x]
  (or (nil? x) (shaped? x {:label text?} {})))

(defn driven-by?
  "Whether `x` is `nil`, `{:kind :person}`, or `{:kind :model :model text}`."
  [x]
  (or (nil? x)
      (shaped? x {:kind (is-kw :person)} {})
      (shaped? x {:kind (is-kw :model) :model text?} {})))

(defn invited-by?
  "Whether `x` is a UUID actor id or `:system`."
  [x]
  (or (id? x) (= :system x)))

;; ------------------------------------------------------- write & read forms

(def ^:private write-forms
  "Required parts and predicates for each of the 12 write operations."
  {:invite         {:person id? :claim claim? :invited-by invited-by? :at time-val?}
   :enter          {:account account? :says says? :email (or-nil email?) :code-hash (or-nil hash-str?)
                    :own-layer id? :making making? :at time-val?}
   :own-layer-made {:person id? :at time-val?}
   :begin-sign-in  {:root id? :actor id? :sign-in id? :kind kind? :hash hash-str? :until time-val?
                    :label (or-nil text?) :at time-val?}
   :replace-secret {:root id? :sign-in id? :old-hash hash-str? :new-hash hash-str? :until time-val?
                    :previous-until time-val? :at time-val?}
   :end-sign-in    {:root id? :sign-in id? :at time-val?}
   :make-actor     {:root id? :actor id? :made-by id? :label text? :at time-val?}
   :end-actor      {:root id? :actor id? :at time-val?}
   :open-session   {:root id? :sign-in id? :session id? :until time-val? :at time-val?}
   :close-session  {:root id? :session id? :at time-val?}
   :ask            {:code code-str? :hash hash-str? :asks text? :ends time-val? :at time-val?}
   :answer-ask     {:code code-str? :yes? boolean? :root id? :actor id? :new-agent new-agent?
                    :sign-in id? :until time-val? :at time-val?}})

(def ^:private write-optionals
  "Optional parts for write operations (`:driven-by` on `open-session` alone)."
  {:open-session {:driven-by driven-by?}})

(def ^:private time-keys
  "Keys whose fixed-width integer values must be coerced to `Long`."
  [:at :until :previous-until :ends])

(defn- coerce-times
  "Coerces any present fixed-width integer time key in `m` to an exact `Long`."
  [m]
  (reduce (fn [acc k]
            (if (contains? acc k)
              (assoc acc k (long (get acc k)))
              acc))
          m
          time-keys))

(defn fit-write
  "Validates `input` against the form for write `kind`. Returns a plain Clojure
  map with all time fields coerced to `Long` (and `:says` copied into a plain
  persistent hash map when present), or `nil` when `input` does not fit."
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
  "Validates `query` for `who-is`: exact keys `#{:root :sign-in :session}` with
  UUID `:root` and `:sign-in`, and UUID-or-nil `:session`. Returns a plain map
  or `nil`."
  [query]
  (try
    (when (shaped? query {:root id? :sign-in id? :session (or-nil id?)} {})
      {:root (:root query)
       :sign-in (:sign-in query)
       :session (:session query)})
    (catch Throwable _ nil)))

(defn fit-ask-of
  "Validates `code` for `ask-of` (an 8-char string). Returns `code` or `nil`."
  [code]
  (try
    (when (code-str? code) code)
    (catch Throwable _ nil)))

(defn fit-under
  "Validates `root` for `under` (a UUID). Returns `root` or `nil`."
  [root]
  (try
    (when (id? root) root)
    (catch Throwable _ nil)))

(defn fit-watch-root
  "Validates `[root f]` for `watch-root`: `root` must be a UUID and `f` must
  satisfy `fn?` (rejecting keywords, maps, sets, symbols, and vars). Returns
  `[root f]` or `nil`."
  [root f]
  (try
    (when (and (id? root) (fn? f))
      [root f])
    (catch Throwable _ nil)))
