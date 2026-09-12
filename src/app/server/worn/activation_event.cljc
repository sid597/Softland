(ns app.server.worn.activation-event
  "Pure data codec for the content of facet-master active-pointer revisions.
   Events name the material revision being worn, a declared operation, scope,
   actor, time and grounds. Constructors/serialization do not validate; explicit
   validators and parse return diagnostics. Legacy bare revision-id sources read
   as activations with unknown grounds, without rewriting stored bytes.

   Owns immutable vocabularies only. No depot append, clock read, authorization
   or revision lookup occurs here. The pointer revision's identity and parent
   chain live in object-container; facet-master uses them to order history."
  (:require [clojure.string :as str]
            #?(:clj [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])))

;; ===========================================================================
;; The closed vocabulary
;; ===========================================================================

(def kinds
  "Declared operation kinds: activate, rollback, pin and unpin. The codec checks
   vocabulary, not whether a requested revision is newer or previously worn."
  #{:activate :rollback :pin :unpin})

(def scope-kinds
  "The two supported scope descriptors. Shared all-unpinned applies by reference;
   subject scope describes an instance operation. This vocabulary does not
   enforce which pointer a caller edits."
  #{:scope/all-unpinned :scope/subject})

(def ground-relations
  #{:grounded-in :responds-to})

(def ground-kinds
  "The declared things an activation may point at. Nothing else is nameable,
   so a case report can only repeat what the event already declared."
  #{:experience :deviation :conflict})

(def event-keys
  #{:activation/revision-id
    :activation/kind
    :activation/scope
    :activation/actor
    :activation/time-ms
    :activation/grounds})

(def ground-keys
  #{:ground/relation :ground/kind :ground/id})

;; ===========================================================================
;; Construction
;; ===========================================================================

(defn all-unpinned-scope
  "Construct the shared-master scope descriptor; performs no wearer lookup."
  []
  [:scope/all-unpinned])

(defn subject-scope
  "Construct a scope for the stringified subject; validation is separate."
  [subject-uid]
  [:scope/subject (str subject-uid)])

(defn ground
  "Construct a ground reference without validating its vocabulary or resolving its id."
  [relation kind id]
  {:ground/relation relation
   :ground/kind kind
   :ground/id (str id)})

(defn event
  "Construct the six event fields, filling scope/actor/grounds defaults and
   coercing time to long (default 0). revision-id names material, not the pointer
   revision. Does not read the clock or call valid-event?; live request callers
   supply the act's time, while bootstrap paths may explicitly use time 0."
  [{:keys [revision-id kind scope actor time-ms grounds]}]
  {:activation/revision-id (str revision-id)
   :activation/kind kind
   :activation/scope (or scope (all-unpinned-scope))
   :activation/actor {:actor/id (str (or (:actor/id actor) "system"))
                      :actor/type (or (:actor/type actor) :system)}
   :activation/time-ms (long (or time-ms 0))
   :activation/grounds (vec (or grounds []))})

;; ===========================================================================
;; Validation — total, never throws
;; ===========================================================================

(defn valid-scope?
  "Accept the exact vector arity for all-unpinned or a nonempty subject string."
  [scope]
  (and (vector? scope)
       (contains? scope-kinds (first scope))
       (case (first scope)
         :scope/all-unpinned (= 1 (count scope))
         :scope/subject (and (= 2 (count scope))
                             (string? (second scope))
                             (seq (second scope)))
         false)))

(defn valid-ground?
  "Check exact ground keys, declared relation/kind and a nonempty id; no lookup."
  [g]
  (and (map? g)
       (= ground-keys (set (keys g)))
       (contains? ground-relations (:ground/relation g))
       (contains? ground-kinds (:ground/kind g))
       (string? (:ground/id g))
       (seq (:ground/id g))))

(defn valid-actor?
  "Require a nonempty actor id and keyword type; not the object-container allowlist."
  [a]
  (and (map? a)
       (string? (:actor/id a))
       (seq (:actor/id a))
       (keyword? (:actor/type a))))

(defn event-errors
  "Every reason this form is not a legal activation event, deterministically
   ordered. Empty means valid. Total: a non-map, a truncated map, and a map
   with hostile values all return errors rather than throwing."
  [e]
  (cond-> []
    (not (map? e))
    (conj {:type :activation/not-a-map})

    (and (map? e) (not= event-keys (set (keys e))))
    (conj {:type :activation/keys-invalid
           :expected (vec (sort-by str event-keys))
           :actual (vec (sort-by str (keys e)))})

    (and (map? e) (not (and (string? (:activation/revision-id e))
                            (seq (:activation/revision-id e)))))
    (conj {:type :activation/revision-id-invalid
           :actual (:activation/revision-id e)})

    (and (map? e) (not (contains? kinds (:activation/kind e))))
    (conj {:type :activation/kind-invalid
           :expected (vec (sort-by str kinds))
           :actual (:activation/kind e)})

    (and (map? e) (not (valid-scope? (:activation/scope e))))
    (conj {:type :activation/scope-invalid
           :actual (:activation/scope e)})

    (and (map? e) (not (valid-actor? (:activation/actor e))))
    (conj {:type :activation/actor-invalid
           :actual (:activation/actor e)})

    (and (map? e) (not (integer? (:activation/time-ms e))))
    (conj {:type :activation/time-ms-invalid
           :actual (:activation/time-ms e)})

    (and (map? e)
         (not (and (vector? (:activation/grounds e))
                   (every? valid-ground? (:activation/grounds e)))))
    (conj {:type :activation/grounds-invalid
           :actual (:activation/grounds e)})))

(defn valid-event?
  "True when event-errors returns no violations of the closed event shape."
  [e]
  (empty? (event-errors e)))

(defn source-for
  "Serialize an event without validation. `*print-namespace-maps*` is pinned off, and
   that is load-bearing, not tidiness: every key here lives in the `:activation`
   namespace, so `pr-str` emits `#:activation{…}` when the flag is true (the
   REPL default) and `{:activation/…}` when it is false (the plain-program
   default). Two byte strings for one value — and object-container import
   identity is CONTENT-HASH keyed, so the same activation issued from a REPL
   and from the server would mint two different durable revisions. Pin it."
  [e]
  (binding [*print-namespace-maps* false]
    (pr-str e)))

;; ===========================================================================
;; Reading — v0 bare strings and P6 forms through ONE door
;; ===========================================================================

(def v0-actor
  "What a v0 bare-string pointer source can honestly say about its actor:
   nothing. The row exists, the actor was not recorded, and the reader says
   so rather than back-filling `system`."
  {:actor/id "unknown" :actor/type :unknown})

(defn parse
  "Read pointer content as a declared event or a legacy/degraded activation.
   A valid map retains its fields with :activation/v0? false and known grounds.
   Nonempty non-map-looking source is a bare material revision id. Invalid or
   unreadable map-looking source returns validation/parse errors, unknown actor
   and grounds, and :activation/v0? true; that flag also marks degraded events.

   Recover the material revision id from an invalid map when possible, otherwise
   use revision-id-fallback (nil by default). Corrupt source need not identify a
   worn revision. Never rewrites source or performs a revision lookup."
  ([source] (parse source nil))
  ([source revision-id-fallback]
   (let [s (str/trim (str source))
         ;; `#:activation{…}` is the namespace-map spelling of the same value.
         ;; `source-for` never writes it, but a form written by any other
         ;; printer must still read back as an event rather than degrade to a
         ;; v0 bare string that happens to start with `#`.
         form? (or (str/starts-with? s "{")
                   (str/starts-with? s "#:"))
         parsed (when form?
                  (try (edn/read-string s)
                       (catch #?(:clj Throwable :cljs :default) _ ::unreadable)))
         errors (when (and form? (not= ::unreadable parsed))
                  (event-errors parsed))]
     (cond
       (and form? (not= ::unreadable parsed) (empty? errors))
       (assoc parsed
              :activation/v0? false
              :activation/grounds-known? true)

       :else
       (let [rev (or (when (and (map? parsed)
                                (string? (:activation/revision-id parsed))
                                (seq (:activation/revision-id parsed)))
                       (:activation/revision-id parsed))
                     (when-not form? (when (seq s) s))
                     revision-id-fallback)]
         (cond-> {:activation/revision-id rev
                  :activation/kind :activate
                  :activation/scope (all-unpinned-scope)
                  :activation/actor v0-actor
                  :activation/time-ms nil
                  :activation/grounds []
                  :activation/v0? true
                  :activation/grounds-known? false}
           form?
           (assoc :activation/errors
                  (if (= ::unreadable parsed)
                    [{:type :activation/parse-error}]
                    (vec errors)))))))))

(defn worn-revision-id
  "The revision a pointer source names, v0 or P6. This is the ONE reader every
   serve path uses, so a v0 pointer and a P6 event resolve identically."
  ([source] (worn-revision-id source nil))
  ([source fallback] (:activation/revision-id (parse source fallback))))

;; ===========================================================================
;; Reading the trail — CAUSAL, never clock-ordered (R3 / T2)
;; ===========================================================================

(defn grounds-label
  "What a reader may SAY about an event's grounds. Three distinct answers —
   collapsing `unknown` into `ungrounded` is the lie this function exists to
   prevent."
  [e]
  (cond
    (false? (:activation/grounds-known? e)) :grounds/unknown
    (empty? (:activation/grounds e)) :grounds/ungrounded
    :else :grounds/declared))

(defn clock-regression?
  "Does this causally-ordered pair run backwards in wall-clock terms? Not an
   error — the honest consequence of grandfathered deploy-time stamps (P1 v0=0,
   P3 v1=1, P5=2) sitting under live wall-clock activations. Readers EXPOSE it
   (`:ambiguous-activation-history`, P4's pattern); they never sort it away."
  [earlier later]
  (let [a (:activation/time-ms earlier)
        b (:activation/time-ms later)]
    (boolean (and (integer? a) (integer? b) (< b a)))))

(defn trail-regressions
  "Every causally-adjacent pair whose stamps run backwards, over a chain that
   is ALREADY in causal (parent-chain) order. Passing a clock-sorted chain here
   would defeat the purpose, so callers name their ordering at the call site."
  [causal-chain]
  (vec
   (keep-indexed
    (fn [i later]
      (when (pos? i)
        (let [earlier (nth causal-chain (dec i))]
          (when (clock-regression? earlier later)
            {:type :ambiguous-activation-history
             :earlier/revision-id (:activation/revision-id earlier)
             :earlier/time-ms (:activation/time-ms earlier)
             :later/revision-id (:activation/revision-id later)
             :later/time-ms (:activation/time-ms later)}))))
    causal-chain)))
