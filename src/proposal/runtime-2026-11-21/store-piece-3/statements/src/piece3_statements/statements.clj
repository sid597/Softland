(ns piece3-statements.statements
  "Plain-code statements of what must hold of a lane's line and of the read
  by stamp, derived from `store-piece-3/SPEC.md` (which governs) and
  `IMPLICIT_SPEC.md` alone, together with Sid's call of 22 November 2026:

    A refused thing is kept whole up to 32 MiB from a sender with standing,
    and up to 256 KiB from a sender with none or refused at the declaration.

  This namespace imports nothing of `src-store` and designs no PState and no
  topology. It states what any implementation that gives the store's door and
  the new read must satisfy:

    L1  :strays-excluded
        A stray gets no stamp, no answer, and never appears in the read.
    L2  :other-gate-excluded
        A thing for L on the other gate's depot is never in L's line or L's
        last-stamp.
    L3  :used-act-id-once
        A second arrival under a used act-id gets no stamp and changes nothing
        of the first decision (not its answer, time, who, or kept thing).
    L4  :consecutive-stamps-from-zero
        First non-stray arrivals on L's own depot take stamps 0, 1, 2, ...
        with no gaps, refusals and pre-making refusals included.
    L5  :door-answer-agreement
        Every decision's answer {:verdict :reason :stamp :time :digest} agrees
        with the door's answer by act-id and with the decided thing's usable
        time and digest.
    L6  :exact-decided-thing
        A decision given :whole carries the exact thing decided at that stamp,
        with all of its parts (:session, :based-on, :because-of, :subjects,
        :claimed-when, :door-time, or raw unparsed keys).

    K1  :accepted-kept-whole
        Every accepted act is kept whole.
    K2  :refused-with-standing-32mib
        A refused thing from a sender with standing (passed declaration and
        standing, refused at places) is kept whole up to 32 MiB (33,554,432
        bytes) and not kept whole above 32 MiB.
    K3  :refused-no-standing-or-declaration-256kib
        A refused thing from a sender with no standing or refused at the
        declaration is kept whole up to 256 KiB (262,144 bytes) and not kept
        whole above 256 KiB.

    S1  :default-members-including-base
        With no fact under :softland.store/who-may-read-acts, every lane —
        the base included — reads as :members.
    S2  :only-first-holder-writes-acts-setting
        Only an act under the lane's first permission writes
        :softland.store/who-may-read-acts; a given permission is refused
        :only-the-first-holder-writes-a-setting.
    S3  :acts-setting-shape-and-entity
        :softland.store/who-may-read-acts accepts only :members or :everyone
        on the lane's own entity, cannot be retracted, and cannot be marked.
    S4  :acts-setting-retroactive
        Changing :softland.store/who-may-read-acts governs the entire line
        from stamp 0 onward.

    R1  :holder-reads-all
        A holder of a standing lane permission (:see or :see-and-act) or of
        a standing run-permission for the query's :session gets L's
        last-stamp and every decision whole (or :too-large when over byte
        bounds); an act-permission grants no reading.
    R2  :ended-or-above-ended-loses-holder
        Once a permission or any permission above it in its chain no longer
        stands, the asker gets nothing whole on a holder's ground.
    R3  :open-lane-visitor-members-setting
        In an :everyone lane whose acts setting is :members, a visitor gets
        last-stamp and consecutive stamps: a signed-in visitor gets her own
        usable-:who decisions whole/:too-large and every other :withheld
        (:stamp alone — no act-id, no answer, no thing, no :too-large); an
        unsigned-in visitor gets every decision :withheld.
    R4  :open-lane-visitor-everyone-setting
        In an :everyone lane whose acts setting is :everyone, any visitor
        (signed in or :not-signed-in) gets last-stamp and every decision
        whole/:too-large.
    R5  :closed-lane-own-things-and-left-out
        In a made :members lane where the asker has no standing permission,
        L is left out (:may-not-see) with no last-stamp; a signed-in asker
        gets her own usable-:who decisions whole/:too-large (with gaps where
        others' stamps sit) and nothing of any other stamp; an unsigned-in
        asker gets no decisions.
    R6  :unmade-lane-own-refusals-and-not-made
        In a lane that is not made, L is left out (:not-made) with no
        last-stamp; a signed-in asker gets her own usable-:who refused
        decisions whole/:too-large, and nobody else gets anything.
    R7  :raw-who-safety
        Reading :who from a declaration-refused raw thing never throws and
        matches an asker iff :who is a usable id equal to the asker.
    R8  :read-byte-bound-and-too-large
        A decision that the asker may read whose thing was not kept whole or
        exceeds the read's byte bound does not stop the line: it gives
        {:stamp :status :too-large :act-id :answer} with no :thing, distinct
        from :withheld, and next-stamp advances past it.
    R9  :consecutive-and-pagination
        For an asker who may see L, each page's stamps are consecutive from
        start with none above last-stamp; for every asker, paging from 0 via
        next-stamp terminates and joins with no duplicate and no omission.
    R10 :bad-query-refused-cleanly
        Malformed read inputs never throw and return {:could-not ...}."
  (:import [clojure.lang BigInt]
           [java.math BigDecimal BigInteger]
           [java.nio.charset StandardCharsets]
           [java.security MessageDigest]
           [java.util Arrays]))

;; --------------------------------------------------------- Sid's byte bounds

(def max-no-standing-or-declaration-bytes
  "256 KiB: the largest refused thing kept whole when refused at the
  declaration or from a sender with no standing (Sid's call, 22 Nov 2026)."
  (* 256 1024))

(def max-with-standing-bytes
  "32 MiB: the largest refused thing kept whole from a sender with standing
  (Sid's call, 22 Nov 2026)."
  (* 32 1024 1024))

(def max-id-text 256)
(def acts-setting-key :softland.store/who-may-read-acts)
(def who-may-see-key :softland.store/who-may-see)

;; --------------------------------------------- Plain-code ids, text, and size

(def ^:private token-re
  #"[\p{L}*+!_?$%&=<>.\-][\p{L}\p{N}*+!_?$%&=<>.\-'#]*")

(defn readable-keyword? [x]
  (and (keyword? x)
       (boolean (re-matches token-re (name x)))
       (if-let [ns (namespace x)]
         (boolean (re-matches token-re ns))
         true)))

(defn usable-id? [x]
  (boolean
   (and (or (readable-keyword? x) (uuid? x))
        (<= (count (str x)) max-id-text))))

(defn plain-map? [x]
  (and (map? x) (not (record? x))))

(defn usable-time [thing]
  (let [t (try (when (plain-map? thing) (get thing :door-time))
               (catch Throwable _ nil))]
    (when (and (int? t) (<= 0 (long t)))
      (long t))))

(defn raw-usable-who
  "Reads `:who` from any raw arrived thing without throwing. Gives the id
  when it passes `usable-id?`, else nil."
  [thing]
  (let [w (try (when (plain-map? thing) (get thing :who))
               (catch Throwable _ nil))]
    (when (usable-id? w) w)))

(defn depot-at-making [lane]
  (if (= :base lane) :microbatch :streaming))

(defn utf8-len ^long [^String s]
  (let [n (.length s)]
    (loop [i 0 total 0]
      (if (>= i n)
        total
        (let [c (.charAt s i)
              code (int c)]
          (cond
            (< code 0x80) (recur (inc i) (inc total))
            (< code 0x800) (recur (inc i) (+ total 2))
            (Character/isHighSurrogate c)
            (if (and (< (inc i) n) (Character/isLowSurrogate (.charAt s (inc i))))
              (recur (+ i 2) (+ total 4))
              (recur (inc i) (inc total)))
            (Character/isLowSurrogate c) (recur (inc i) (inc total))
            :else (recur (inc i) (+ total 3))))))))

(def ^:private hex-chars "0123456789abcdef")

(defn hex ^String [^bytes b]
  (let [sb (StringBuilder. (* 2 (alength b)))]
    (dotimes [i (alength b)]
      (let [v (bit-and 0xff (aget b i))]
        (.append sb (.charAt hex-chars (bit-shift-right v 4)))
        (.append sb (.charAt hex-chars (bit-and v 0xf)))))
    (.toString sb)))

(defn- quoted ^String [^String s]
  (let [sb (StringBuilder. (+ 2 (.length s)))]
    (.append sb \")
    (dotimes [i (.length s)]
      (let [c (.charAt s i)]
        (if-let [esc (char-escape-string c)]
          (.append sb ^String esc)
          (.append sb c))))
    (.append sb \")
    (.toString sb)))

(declare feed-one*)

(defn- one-text* ^String [x depth]
  (let [sb (StringBuilder.)]
    (feed-one* x depth
               (fn [^String s] (.append sb s))
               (fn [^bytes b] (.append sb (quoted (hex b)))))
    (.toString sb)))

(defn- scalar-one-text ^String [x]
  (cond
    (nil? x) "nil"
    (true? x) "true"
    (false? x) "false"
    (string? x) (quoted x)
    (keyword? x) (if (readable-keyword? x)
                   (str x)
                   (str "#keyword " (if-let [n (namespace x)] (quoted n) "nil") " " (quoted (name x))))
    (uuid? x) (str "#uuid \"" x "\"")
    (int? x) (Long/toString (long x))
    (instance? BigInt x) (str (.toBigInteger ^BigInt x) "N")
    (instance? BigInteger x) (str x "N")
    (instance? Double x) (let [d (double x)]
                           (cond
                             (Double/isNaN d) "#double \"NaN\""
                             (Double/isInfinite d) (if (pos? d) "#double \"Infinity\"" "#double \"-Infinity\"")
                             (zero? d) "0.0"
                             :else (Double/toString d)))
    (decimal? x) (str (.stripTrailingZeros ^BigDecimal x) "M")
    :else (str "#other " (.getName (class x)))))

(defn- feed-seq [open xs close depth take-text take-bytes]
  (take-text open)
  (loop [xs (seq xs) first? true]
    (when xs
      (when-not first? (take-text " "))
      (feed-one* (first xs) (inc depth) take-text take-bytes)
      (recur (next xs) false)))
  (take-text close))

(defn- feed-one* [x depth take-text take-bytes]
  (cond
    (> depth 64) (take-text "#too-deep")
    (bytes? x) (do (take-text (str "#bytes " (alength ^bytes x) " "))
                   (take-bytes x))
    (vector? x) (feed-seq "[" x "]" depth take-text take-bytes)
    (record? x) (take-text (scalar-one-text x))
    (map? x) (let [keyed (map (fn [[k v]] [(one-text* k (inc depth)) v]) x)
                   tied? (and (seq keyed) (not (apply distinct? (map first keyed))))
                   ordered (if tied?
                             (sort-by (fn [[kt v]] [kt (one-text* v (inc depth))]) keyed)
                             (sort-by first keyed))]
               (take-text "{")
               (loop [es (seq ordered) first? true]
                 (when es
                   (let [[kt v] (first es)]
                     (when-not first? (take-text " "))
                     (take-text kt)
                     (take-text " ")
                     (feed-one* v (inc depth) take-text take-bytes))
                   (recur (next es) false)))
               (take-text "}"))
    (set? x) (do (take-text "#{")
                 (take-text (apply str (interpose " " (sort (map #(one-text* % (inc depth)) x)))))
                 (take-text "}"))
    (sequential? x) (feed-seq "(" x ")" depth take-text take-bytes)
    :else (take-text (scalar-one-text x))))

(defn thing-utf8-bytes
  "Counts the UTF-8 bytes of `thing`'s canonical text without building a
  single huge string when `thing` holds large strings or byte arrays."
  ^long [thing]
  (let [total (volatile! 0)]
    (try
      (feed-one* thing 0
                 (fn [^String s] (vswap! total + (utf8-len s)))
                 (fn [^bytes b] (vswap! total + (+ 2 (* 2 (alength b))))))
      @total
      (catch Throwable _
        (utf8-len "#unreadable")))))

(defn digest-hex
  "SHA-256 hex digest of `thing` without top-level `:door-time`, matching
  `softland.store.door.digest/hex-of`."
  ^String [thing]
  (let [md (MessageDigest/getInstance "SHA-256")
        bare (if (plain-map? thing)
               (try (dissoc thing :door-time) (catch Throwable _ thing))
               thing)]
    (try
      (feed-one* bare 0
                 (fn [^String s] (.update md (.getBytes s StandardCharsets/UTF_8)))
                 (fn [^bytes b] (.update md b)))
      (hex (.digest md))
      (catch Throwable _
        (hex (.digest (MessageDigest/getInstance "SHA-256")
                      (.getBytes "#unreadable" StandardCharsets/UTF_8)))))))

(defn same-thing?
  "Deep structural equality that also compares byte arrays by content and
  never throws on strange maps."
  [a b]
  (try
    (cond
      (and (bytes? a) (bytes? b)) (Arrays/equals ^bytes a ^bytes b)
      (and (vector? a) (vector? b)) (and (= (count a) (count b))
                                         (every? true? (map same-thing? a b)))
      (and (sequential? a) (sequential? b) (not (vector? a)) (not (vector? b)))
      (and (= (count a) (count b))
           (every? true? (map same-thing? a b)))
      (and (plain-map? a) (plain-map? b))
      (and (= (count a) (count b))
           (every? (fn [[k va]] (and (contains? b k) (same-thing? va (get b k)))) a))
      :else (= a b))
    (catch Throwable _
      (= (digest-hex a) (digest-hex b)))))

;; ------------------------------------- Plain-code derivation of rights & view

(defn kept-whole-by-gate?
  "Sid's call of 22 November 2026 + K1..K3:
   - Accepted acts: always kept whole.
   - Refused from a sender with standing (`:refused-stage == :places`): kept
     whole iff `thing-bytes <= 32 MiB`.
   - Refused from a sender with no standing (`:refused-stage == :standing`) or
     refused at the declaration (`:refused-stage == :declaration`): kept whole
     iff `thing-bytes <= 256 KiB`."
  [{:keys [verdict refused-stage thing-bytes]}]
  (case verdict
    :accepted true
    :refused (case refused-stage
               :places (<= (long thing-bytes) max-with-standing-bytes)
               (:standing :declaration) (<= (long thing-bytes) max-no-standing-or-declaration-bytes))))

(defn permission-stands?
  "Plain-code statement of Piece 1's `stands?` on a lane's current permission
  places (`{place-id {:perm p :stamp s}}`, nil/absent when ended or empty)."
  [perm-places perm]
  (let [covers (when (= 6 (count perm)) (nth perm 5))
        place-id (case (first covers)
                   :run [:run (second covers)]
                   :act [:act (second covers)]
                   [:holder (nth perm 0)])
        cur (get perm-places place-id)]
    (and (some? cur)
         (= perm (:perm cur))
         (let [parent (when (>= (count perm) 4) (nth perm 3))]
           (if (nil? parent)
             true
             (let [parent-covers (when (= 6 (count parent)) (nth parent 5))
                   parent-place (case (first parent-covers)
                                  :run [:run (second parent-covers)]
                                  :act [:act (second parent-covers)]
                                  [:holder (nth parent 0)])
                   parent-cur (get perm-places parent-place)]
               (and (some? parent-cur)
                    (< (long (:stamp parent-cur)) (long (:stamp cur)))
                    (permission-stands? perm-places parent))))))))

(defn holder-for-read?
  "SPEC.md ('After piece 2'): an asker holds a permission that stands in a
  made lane iff her lane permission stands OR she asks in `:session` and
  that run's permission stands and names her as its holder. An act-permission
  grants no reading."
  [{:keys [made? perm-places]} asker session]
  (and made?
       (some? asker)
       (or (if-let [cur (get perm-places [:holder asker])]
             (permission-stands? perm-places (:perm cur))
             false)
           (if (and (some? session) (contains? perm-places [:run session]))
             (let [cur (get perm-places [:run session])]
               (and (= asker (nth (:perm cur) 0))
                    (permission-stands? perm-places (:perm cur))))
             false))))

(defn asker-role
  "Classifies how `(asker, not-signed-in, session)` meets `lane-state` under
  SPEC.md (which governs over IMPLICIT_SPEC.md on closed and unmade lanes):
    `:not-made`      — lane not made: left-out `:not-made`, no last-stamp,
                       signed-in asker still gets her own usable-:who refusals
    `:may-not-see`   — made `:members` lane, asker is no holder: left-out
                       `:may-not-see`, no last-stamp, signed-in asker still
                       gets her own usable-:who decisions
    `:all-whole`     — holder, OR open lane (`who-may-see == :everyone`) with
                       `who-may-read-acts == :everyone`
    `:visitor-own`   — open lane with `who-may-read-acts == :members`:
                       consecutive stamps, own usable-:who whole/:too-large,
                       all others `:withheld`"
  [{:keys [made? who-may-see who-may-read-acts] :as lane-state}
   {:keys [asker session]}]
  (cond
    (not made?) :not-made
    (holder-for-read? lane-state asker session) :all-whole
    (= :everyone who-may-see)
    (if (= :everyone who-may-read-acts) :all-whole :visitor-own)
    :else :may-not-see))

(defn expected-entry-for
  "What a single decision `d` (`{:stamp :act-id :answer :thing :thing-bytes
  :raw-who :kept-whole?}`) must look like for `role` and `asker` under a
  per-entry byte limit `max-bytes` (`nil` when `d` must be omitted completely,
  as for another sender's decision in `:not-made` or `:may-not-see`)."
  [role asker max-bytes {:keys [stamp act-id answer thing thing-bytes raw-who kept-whole?]}]
  (let [own? (and (some? asker) (some? raw-who) (= asker raw-who))
        give-authorized (fn []
                          (if (and kept-whole? (<= (long thing-bytes) (long max-bytes)))
                            {:stamp stamp
                             :status :whole
                             :act-id act-id
                             :answer answer
                             :thing thing}
                            {:stamp stamp
                             :status :too-large
                             :act-id act-id
                             :answer answer}))]
    (case role
      :all-whole (give-authorized)
      :visitor-own (if own?
                     (give-authorized)
                     {:stamp stamp :status :withheld})
      (:may-not-see :not-made) (when own? (give-authorized)))))

;; --------------------------------------------- Checking a read response

(defn valid-read-request?
  [{:keys [lane start max-count max-bytes asker not-signed-in session]}]
  (and (usable-id? lane)
       (int? start) (<= 0 (long start))
       (int? max-count) (pos? (long max-count))
       (or (nil? max-bytes) (and (int? max-bytes) (pos? (long max-bytes))))
       (not= (some? asker) (true? not-signed-in))
       (or (nil? asker) (usable-id? asker))
       (or (nil? session) (and (some? asker) (usable-id? session)))))

(defn check-entry-against-expected
  "Checks one returned entry against its expected shape and returns nil or a
  break map `{:statement kw :detail str}`."
  [role expected actual decided-map]
  (cond
    (not= (:stamp expected) (:stamp actual))
    {:statement :R9/consecutive-and-pagination
     :detail (str "Expected stamp " (:stamp expected) " but got " (pr-str actual))}

    (= :withheld (:status expected))
    (cond
      (not= :withheld (:status actual))
      {:statement (if (= :accepted (get-in decided-map [:answer :verdict]))
                    :R3/visitor-reads-taken-act-whole
                    :R3/open-lane-visitor-members-setting)
       :detail (str "Decision at stamp " (:stamp expected) " must be :withheld, got " (pr-str actual))}

      (not= #{:stamp :status} (set (keys actual)))
      {:statement :R3/withheld-leaks-fields
       :detail (str "Withheld entry must hold stamp alone, got keys " (keys actual) ": " (pr-str actual))}

      :else nil)

    ;; Expected is `:whole` or `:too-large`
    (= :withheld (:status actual))
    {:statement (if (= :all-whole role) :R1/holder-or-open-acts-withheld :R3/own-thing-withheld)
     :detail (str "Authorized decision at stamp " (:stamp expected) " was wrongly :withheld: " (pr-str actual))}

    (not= (:act-id expected) (:act-id actual))
    {:statement :L5/door-answer-agreement
     :detail (str "Wrong :act-id at stamp " (:stamp expected) ": expected " (:act-id expected) ", got " (:act-id actual))}

    (not= (:answer expected) (:answer actual))
    {:statement :L5/door-answer-agreement
     :detail (str "Answer mismatch at stamp " (:stamp expected) ": expected " (:answer expected) ", got " (:answer actual))}

    (not= (:status expected) (:status actual))
    {:statement (cond
                  (not (:kept-whole? decided-map))
                  (if (= :places (:refused-stage decided-map))
                    :K2/refused-with-standing-32mib
                    :K3/refused-no-standing-or-declaration-256kib)

                  (and (= :whole (:status expected)) (= :too-large (:status actual))
                       (= :places (:refused-stage decided-map)))
                  :K2/refused-with-standing-32mib

                  :else :R8/read-byte-bound-and-too-large)
     :detail (str "At stamp " (:stamp expected) " (stage=" (:refused-stage decided-map)
                  ", bytes=" (:thing-bytes decided-map) ") expected status "
                  (:status expected) " but got " (:status actual))}

    (and (= :too-large (:status expected)) (contains? actual :thing))
    {:statement :R8/read-byte-bound-and-too-large
     :detail (str ":too-large entry at stamp " (:stamp expected) " still carried :thing")}

    (and (= :whole (:status expected))
         (not (same-thing? (:thing expected) (:thing actual))))
    {:statement :L6/exact-decided-thing
     :detail (str "Decided :thing mismatch at stamp " (:stamp expected))}

    :else nil))

(defn check-single-page
  "Holds one read response `resp` against `lane-state` and `req` (`{:lane
  :start :max-count :max-bytes :asker :not-signed-in :session}`). Returns nil
  when every statement holds, or `{:statement kw :detail str}`."
  [lane-state {:keys [start max-count max-bytes asker] :as req} resp]
  (if-not (valid-read-request? req)
    (when-not (and (map? resp) (:could-not resp))
      {:statement :R10/bad-query-refused-cleanly
       :detail (str "Invalid read request " (pr-str req) " was not refused cleanly: " (pr-str resp))})
    (if (or (not (map? resp)) (:could-not resp))
      {:statement :R10/bad-query-refused-cleanly
       :detail (str "Valid read request " (pr-str req) " failed: " (pr-str resp))}
      (let [role (asker-role lane-state req)
            decisions (:decisions lane-state)
            true-last (when (seq decisions) (dec (count decisions)))
            eff-max-bytes (or max-bytes Long/MAX_VALUE)
            expected-all (->> decisions
                              (drop start)
                              (keep #(expected-entry-for role asker eff-max-bytes %))
                              vec)
            entries (:entries resp)]
        (cond
          ;; 1. Left-out & last-stamp rules (R1..R6, L4)
          (contains? #{:not-made :may-not-see} role)
          (cond
            (not= role (:left-out resp))
            {:statement (if (= :not-made role)
                          :R6/unmade-lane-own-refusals-and-not-made
                          :R5/closed-lane-own-things-and-left-out)
             :detail (str "Expected :left-out " role ", got " (pr-str (:left-out resp)))}

            (some? (:last-stamp resp))
            {:statement :R5/closed-lane-leaks-last-stamp
             :detail (str "Left-out lane (" role ") leaked :last-stamp " (:last-stamp resp))}

            :else nil)

          (some? (:left-out resp))
          {:statement :R1/holder-reads-all
           :detail (str "Asker with role " role " saw lane left out: " (pr-str resp))}

          (not= true-last (:last-stamp resp))
          {:statement :L4/consecutive-stamps-from-zero
           :detail (str "Expected :last-stamp " true-last ", got " (:last-stamp resp))}

          :else nil)))))

(defn check-page-entries-and-bounds
  "Checks the entries, count bound, byte bound, consecutiveness, and
  `next-stamp` of a valid single-page response `resp`."
  [lane-state {:keys [start max-count max-bytes asker] :as req} resp]
  (or (check-single-page lane-state req resp)
      (when (valid-read-request? req)
        (let [role (asker-role lane-state req)
              decisions (:decisions lane-state)
              eff-max-bytes (or max-bytes Long/MAX_VALUE)
              expected-all (->> decisions
                                (drop start)
                                (keep #(expected-entry-for role asker eff-max-bytes %))
                                vec)
              entries (vec (:entries resp))
              n (count entries)
              bytes-returned (reduce + 0 (for [e entries :when (= :whole (:status e))]
                                           (:thing-bytes (nth decisions (:stamp e)))))]
          (cond
            (> n max-count)
            {:statement :R8/count-bound-exceeded
             :detail (str "Returned " n " entries > max-count " max-count)}

            ;; If more than 1 entry is returned, total whole bytes must not exceed max-bytes;
            ;; a single whole entry must also be <= max-bytes (otherwise it must be :too-large).
            (> bytes-returned eff-max-bytes)
            {:statement :R8/read-byte-bound-and-too-large
             :detail (str "Returned whole bytes " bytes-returned " > max-bytes " eff-max-bytes)}

            ;; When expected-all is non-empty, a page must make progress!
            ;; Either it returns >= 1 entry, or (for a closed/unmade lane that pages in chunks)
            ;; advances `next-stamp > start`. For an asker who sees the lane (`:all-whole` or
            ;; `:visitor-own`), if `expected-all` is non-empty, `entries` MUST be non-empty!
            (and (seq expected-all)
                 (empty? entries)
                 (contains? #{:all-whole :visitor-own} role))
            {:statement :R8/too-large-stops-line
             :detail (str "Empty entries returned when " (count expected-all)
                          " decisions remain from start=" start "; resp=" (pr-str resp))}

            (and (seq expected-all)
                 (empty? entries)
                 (not (and (int? (:next-stamp resp)) (> (long (:next-stamp resp)) (long start)))))
            {:statement :R9/consecutive-and-pagination
             :detail (str "No progress made from start=" start ": " (pr-str resp))}

            ;; Returned entries must match the exact prefix of `expected-all`!
            (> n (count expected-all))
            {:statement (if (contains? #{:not-made :may-not-see} role)
                          :R5/closed-lane-leaks-others
                          :R9/consecutive-and-pagination)
             :detail (str "Returned " n " entries when only " (count expected-all)
                          " are visible: " (pr-str entries))}

            :else
            (or (some (fn [[exp act]]
                        (check-entry-against-expected
                         role exp act (nth decisions (:stamp exp))))
                      (map vector (take n expected-all) entries))
                ;; Consecutive stamps for askers who see the lane
                (when (and (contains? #{:all-whole :visitor-own} role) (seq entries))
                  (let [stamps (mapv :stamp entries)
                        want (vec (range start (+ start n)))]
                    (when (not= want stamps)
                      {:statement :R9/consecutive-and-pagination
                       :detail (str "Stamps not consecutive from " start ": got " stamps)})))
                ;; next-stamp check when seeing the lane
                (when (contains? #{:all-whole :visitor-own} role)
                  (let [next-s (:next-stamp resp)]
                    (cond
                      (nil? next-s) nil
                      (and (empty? expected-all) (< (long next-s) (long start)))
                      {:statement :R9/consecutive-and-pagination
                       :detail (str "Start past end gave next-stamp " next-s " < start " start)}
                      (and (seq entries) (not= (inc (:stamp (last entries))) (long next-s)))
                      {:statement :R9/consecutive-and-pagination
                       :detail (str "Expected next-stamp " (inc (:stamp (last entries)))
                                    ", got " next-s)}
                      :else nil)))))))))

(defn check-full-pagination
  "Pages forward from `start = 0` using `read-line` with `page-size` and
  `max-bytes` until the end of the line, verifying every page and verifying
  that all pages concatenated equal the complete expected sequence for `req`."
  [read-line lane-state {:keys [asker max-bytes] :as base-req} page-size]
  (let [role (asker-role lane-state base-req)
        decisions (:decisions lane-state)
        eff-max-bytes (or max-bytes Long/MAX_VALUE)
        expected-all (->> decisions
                          (keep #(expected-entry-for role asker eff-max-bytes %))
                          vec)]
    (loop [cursor 0
           acc []
           guards 0]
      (if (> guards (+ (count decisions) 10))
        {:statement :R9/consecutive-and-pagination
         :detail (str "Pagination failed to terminate after " guards " pages")}
        (let [req (assoc base-req :start cursor :max-count page-size)
              resp (read-line req)]
          (if-let [err (check-page-entries-and-bounds lane-state req resp)]
            err
            (let [entries (vec (:entries resp))
                  acc' (into acc entries)
                  next-s (:next-stamp resp)
                  done? (cond
                          (contains? #{:all-whole :visitor-own} role)
                          (let [ls (:last-stamp resp)]
                            (or (nil? ls)
                                (nil? next-s)
                                (> (long next-s) (long ls))
                                (and (empty? entries) (> cursor (long ls)))))

                          :else
                          (or (nil? next-s)
                              (and (empty? entries) (<= (long next-s) cursor))
                              (> (long next-s) (count decisions))))]
              (if done?
                (when (not= expected-all acc')
                  {:statement (cond
                                (= :not-made role) :R6/unmade-lane-own-refusals-and-not-made
                                (= :may-not-see role) :R5/closed-lane-own-things-and-left-out
                                (= :all-whole role) :R1/holder-reads-all
                                :else :R9/consecutive-and-pagination)
                   :detail (str "Joined pages (" (count acc') " entries) != expected ("
                                (count expected-all) " entries) for role=" role
                                " req=" (pr-str base-req))})
                (recur (long next-s) acc' (inc guards))))))))))
