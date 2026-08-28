(ns app.server.worn.activation-event
  "Declared activation, rollback, pin, and unpin event forms.
   Takes: pointer revision ids, kinds, scopes, actors, times, and grounds.
   Gives: validated event maps, parsed pointer-source bytes, and compatibility reads for prior strings.
   Holds nothing."
  (:require [clojure.string :as str]
            #?(:clj [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])))

;; ===========================================================================
;; The closed vocabulary
;; ===========================================================================

(def kinds
  "What an activation DID. `:activate` moves the worn revision forward,
   `:rollback` re-wears a prior one (including the inherited state — T11: the
   removal of a deviation is a rollback to the parent, never a tombstone and
   never a deletion), `:pin`/`:unpin` move an instance master's pin."
  #{:activate :rollback :pin :unpin})

(def scope-kinds
  "P6 opens exactly two. `:scope/all-unpinned` reaches every unpinned wearer
   INCLUDING wearers that do not exist yet — by reference, never by fan-out
   writes (T1). `:scope/subject` is one subject's instance master. Richer
   scopes wait for a need that names itself."
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

(defn all-unpinned-scope [] [:scope/all-unpinned])

(defn subject-scope [subject-uid] [:scope/subject (str subject-uid)])

(defn ground
  [relation kind id]
  {:ground/relation relation
   :ground/kind kind
   :ground/id (str id)})

(defn event
  "Build one closed activation event. `time-ms` MUST be the honest wall clock
   of the requesting act (R3); callers that pass a deterministic constant are
   test callers passing it explicitly, and G11 greps production paths for it."
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
  [g]
  (and (map? g)
       (= ground-keys (set (keys g)))
       (contains? ground-relations (:ground/relation g))
       (contains? ground-kinds (:ground/kind g))
       (string? (:ground/id g))
       (seq (:ground/id g))))

(defn valid-actor?
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
  [e]
  (empty? (event-errors e)))

(defn source-for
  "The DURABLE bytes of one event. `*print-namespace-maps*` is pinned off, and
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
  "One pointer source → one activation event, ALWAYS. Three outcomes, each
   labeled so a reader never has to guess which it got:

   - a P6 edn form that validates  → the event, `:activation/v0? false`,
                                     `:activation/grounds-known? true`
   - a v0 bare revision-id string  → `{:activation/kind :activate}` over that
                                     revision, `:activation/v0? true`,
                                     `:activation/grounds-known? false`
   - a form that parses as a map   → the same v0-shaped answer PLUS
     but fails validation            `:activation/errors`, so a corrupt event
                                     degrades to `we know what is worn, we do
                                     not know why` instead of to nothing.

   Never throws. `revision-id-fallback` is the pointer's own naming of the
   worn revision when the source itself cannot supply one."
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
